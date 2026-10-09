# The evaluator, phase 1: the core, loading arbace.core

B1a step 5 ([B1-PLAN.md](B1-PLAN.md)), first phase (branch `eval-core`, 2026-10-09): the
evaluator over the translated `Compiler`'s `Expr` tree ([EVAL-PLAN.md](EVAL-PLAN.md), the user's
decisions Q1-Q7), loading `arbace.core` and the namespaces `arbace.main` needs from sources
embedded in the executable, and the checks: the program from `bin/c2g --program` starts, loads
`arbace.core`, and runs `arbace.main` (`-e`, scripts, `-` for stdin, the REPL) on linux/amd64
and, under `qemu-aarch64`, linux/arm64.

```
$ bin/arbace-go -e '(+ 1 2)'
3
$ bin/arbace-go script.clj      # deftype, defrecord, protocols, reify, letfn, case, try ...
$ echo '(defn sq [x] (* x x)) (sq 7)' | bin/arbace-go
Clojure 1.13.0-master-SNAPSHOT
user=> #'user/sq
user=> 49
```

Summary of the results (details below): the oracle's forms corpus passes 9,364 of 9,652 cases (97.0%); its class
scripts and regex corpus run through the evaluator too (8,940 of 8,943 and 1,204 of 1,233; identical on arm64); `bin/c2g-check` with
`arbace.core` loaded passes all 9,002 steps, the 47 that needed `arbace.core` among them;
`bin/arbace-go --smoke` passes on both architectures. `arbace.core` loads in about 1.9 s
(amd64), the whole start of `arbace.main` takes about 4.2 s; the executable is about 37 MB.

## Structure

| where | what |
|---|---|
| `arbace/lang/go/Compiler.clj` | the evaluator (below): `Compiler$Frame`, `EvalFn`, `EvalMethod`, `EvalState`, `Dyn` (natives), `Evaluator`; an `evalIn(Frame)` method for every node kind that can hold locals, in the variants of the `Expr` classes; `ObjExpr.compile` defines the classes of `deftype`/`reify`; `NewInstanceExpr.compileStub`; `FnMethod.primInterface` (nil, Q1); `loadFile` without `java.io.File` |
| `arbace/lang/go/RT.clj` | `RT.load` over `resourceAsStream`: the program's embedded sources (jrt's host `Resource`), then the directories of `ARBACE_PATH`; RT's initialization loads `arbace.core` when the program has its sources; `doInit` without the socket server; `ARBACE_LOAD_TIMES` |
| `arbace/lang/go/ns/` (new) | the Go build's namespace variants (below): `genclass.clj` (`gen-interface` at run time), `core_proxy.clj` (`proxy` throws when evaluated), `instant.clj` (decision 7, over `java.util.Date`), `main.after.clj`, `main.subst.clj`, `java/io.subst.clj` |
| `arbace/c2g/embed.clj` (new) | `bin/c2g --program`: the embedded sources, the namespace variants applied, and the classes the namespaces name outside the world |
| `arbace/c2g/main.clj` | `--program`: the REPL's world (every public member of every class with class forms, Q2), the embedded sources (`go:embed` of `res/`, the host's `Resources`), the cut classes the namespaces name; registrations of both packages before the cut classes; the `:after` hook last |
| `arbace/c2g/out.clj` | member tables: the public methods and constructors that do not exist in Go listed, throwing (`--program`); cut classes' tables from the JVM's reflection, throwing; overloads in the JVM's order; setters of volatile fields; out-of-world superinterfaces replaced by theirs; `reflect_tables_c2g.go` (jrt members c2g writes: `String.format`, the regex methods, `System.in/out/err`, `CASE_INSENSITIVE_ORDER`, the context class loader and resources) |
| `arbace/c2g/dyn.clj` | `Dyn`'s natives `setStaticMethod`, `defineCtor`, `defineInterface`, `addInterfaceMethod`, `defineFnClass`, `hideField`; HotSpot's `AbstractMethodError` message |
| `arbace/c2g/decls.clj`, `code.clj`, `world.clj` | `getClass` of a class with a field `c2g$class`; a `this(...)`/`super(...)` to a constructor outside the world is a missing operation; catch clauses of classes outside the world dropped; `identical?` of two interface types through `any`; variants' parameter keys skip `:throws` |
| `arbace/g2c/build.clj` | a directory holding `.g2c-data` is data, not forms (the embedded sources) |
| `go/arbace/jrt/` | the evaluator's natives (`natives.clj`: trace hook, monitors, checkcast, `RT.hostResource`, `UUID`'s random bytes); evaluated frames in stack traces (`throwable.clj`); `Box` maps a typed nil to null; `System`'s `Class` (`system.clj`) and member table; `WeakReference`'s constructors for subclasses; tables regenerated (`test/jrt/tables.clj`: overloads in the JVM's order) |
| `overlay/jdk/variants/` | `UUID.randomUUID` from the host's random bytes; `ByteArray` and `ByteArrayLittleEndian` without `VarHandle`s (`UUID.toString`) |
| `bin/arbace-go` (new) | build, run, smoke-test the executable (below) |
| `test/c2g/check.clj` | `bin/c2g-check -- --program`: the check programs load `arbace.core` |
| `test/c2g/eval/` | the proof grows by the other node kinds |

## The evaluator

### Nodes

EVAL-PLAN §2 describes one walk, `Evaluator.eval(Expr, Frame)`, a `cond` over the node classes.
As built, **each node class evaluates itself**: the variant adds a default method `evalIn(Frame)`
to the interface `Expr` (calling the node's own `eval`, right for the nodes that hold no locals:
constants, vars, keywords, `the-var`, static fields, `QualifiedMethodExpr`) and overrides it in
the node kinds that can hold locals: `LocalBindingExpr`, `BodyExpr`, `IfExpr`, `LetExpr` (let and
loop), `RecurExpr`, `LetFnExpr`, `TryExpr`, `ThrowExpr`, `InvokeExpr`, `KeywordInvokeExpr`,
`StaticMethodExpr`, `InstanceMethodExpr`, `InstanceFieldExpr`, `StaticFieldExpr` (assignment),
`AssignExpr`, `DefExpr`, `NewExpr`, `MetaExpr`, `VectorExpr`, `ListExpr`, `MapExpr`, `SetExpr`,
`EmptyExpr`, `ConstantExpr`, `ImportExpr`, `InstanceOfExpr`, `StrConcatExpr`, `CaseExpr`,
`MonitorEnterExpr`, `MonitorExitExpr`, and `ObjExpr` (`fn*`, `reify`). The reason: a Go
interface call per node instead of a chain of up to 30 type assertions, and each case next to the
class it evaluates. `Evaluator.eval(e, f)` remains, as `e.evalIn(f)`. Top-level forms keep
`Compiler.eval`'s path (a non-`def` collection is wrapped in a `fn*` and invoked, so it reaches
`evalIn`); `Expr.eval` of the frameless nodes is unchanged.

**Frames.** A `Frame` holds `Object[maxLocal + 2]` slots indexed by `LocalBinding.idx`, the
running `EvalFn` (its closed-over values) or the deftype method's object (whose fields are the
closed-over values), the method, the `ObjExpr`, the line being evaluated, the source file, and
the calling frame. **Where a local is** is decided once per `LocalBindingExpr` and cached on it
(`evalWhere`: the slot, or the index among the `ObjExpr`'s closes): by identity among the closes,
not through `ObjMethod.locals`, because a direct fn's parameters are renumbered after they enter
that map and `LocalBinding`'s hash depends on the number (the lookup failed on `defn` itself).
The closes in order (`evalCloses`) and where the creating frame holds each (`evalWhere` of the
`ObjExpr`) are cached on the `FnExpr`, so making a closure copies values only.

**Functions** (Q3): one `EvalFn extends RestFn` of required arity 0. Its `doInvoke` receives
RestFn's seq and `Evaluator.invokeFn` binds from it, realizing only the fixed parameters: the rest
parameter is the remaining seq, unrealized (`(apply f (range))` on a variadic fn, `mapcat`'s
chunking as on the JVM). The method is chosen as the JVM's classes choose (a fixed arity, then
the variadic one; `ArityException` with the fn's class name, which `Compiler.macroexpand1`
recognizes for macros). Each `FnExpr` has a **run-time class** (`Dyn.defineFnClass`), a subclass
of `EvalFn` named as the JVM names the fn's class (`arbace.core$map`, `user$eval12$fn__13`),
answered by `getClass` (c2g's rule for a field `c2g$class`, proposed amendment V2): messages,
`class`, printing and stack traces show the JVM's names.

**Primitive fns** (Q1): `FnMethod.primInterface` returns nil, so the analyzer makes no fn
primitive; `invokeFn` converts `^long`/`^double` parameters on entry and the result on exit.

**The bytecode's semantics** are mirrored where `Expr.eval` (the JVM's eval path for top-level
forms) differs from what the compiled code does, which is what the JVM runs inside every fn:

- exceptions are not wrapped in `CompilerException` inside fns (`Expr.eval` wraps them);
- resolved methods and constructors take their arguments as `emitTypedArgs` passes them: a
  primitive parameter's argument cast to `Number` (`Boolean`, `Character`) and converted by
  RT's checked casts, a reference parameter's cast to its class (`ClassCastException`,
  `NullPointerException`); a `char` where the bytecode widens it; an instance call casts its
  target to the method's class first (not `Method.invoke`'s `IllegalArgumentException`);
- casts to a class known at run time throw jrt's `ClassCast`, the JVM's checkcast message;
- a method's primitive return is converted as `ObjMethod.emitBody` does: a primitive body by
  RT's checked casts, any other by `Number.intValue` and the like (a deftype `hashCode`
  truncates as on the JVM);
- constants are rebuilt as the class's static initializer builds them (`emitValue`: a quoted
  seq is a `PersistentList`, maps by `RT.map`, metadata kept), once per node; empty literals are
  the types' `EMPTY`; `import*` evaluates to the class;
- a resolved instance field is read as `getfield` reads it; a deftype's mutable fields are
  private to reflection.

**Control.** `recur` assigns the loop's slots (converted to the loop locals' primitive types)
and returns a sentinel the loop or the method restarts on. `try` is a class forms `try` catching
`Throwable` and choosing the clause by `Class.isInstance`, else rethrowing; `finally` as Java's.
`letfn*` makes the fns, then fills each one's closed-over letfn locals. `case*` is
`CaseExpr.doEmit`'s switch (ints with the expression's primitive type, hashes with `skipCheck`,
identity), its key looked up in `tests`.

**Depth** (Q4): a per-thread `EvalState` (a `ThreadLocal`) holds the innermost frame and the
depth; `invokeFn` and `invokeMethod` push and pop, and beyond 10,000 frames throw
`StackOverflowError`.

**Stack traces** (Q6): when an exception is made, jrt asks the evaluator (a `Supplier` it set,
`jrt.EvalTrace`) for this thread's frames as `StackTraceElement`s (the fn's class name, `invoke`
or the method's name, the source file the fn was analyzed in, the line of the call being
evaluated). jrt's frame mapping puts each in place of the Go frame of the evaluator's call body
(`Evaluator.invokeFn`'s try literal) and leaves out the evaluator's own frames, the `Dyn`
dispatch, member tables' invokers, and the reflective call frames under an evaluated host call,
which compiled code calls directly. So `arbace.main`'s error triage names the Clojure frame:

```
$ bin/arbace-go -e '(defn f [] (/ 1 0)) (f)'
#'user/f
Execution error (ArithmeticException) at user/f (REPL:1).
Divide by zero
```

### Types made at run time

Enough of EVAL-PLAN §2.5 to load `arbace.core` (`gvec`'s `deftype`s, `definterface`s and
`reify`, core's `Eduction`, `defprotocol`'s interfaces), with defrecord working:

- `NewInstanceExpr.compileStub` makes the stub (`compile__stub.NAME`) with `Dyn.defineClass`:
  the interfaces and the fields (public), and a deftype's constructors.
- `ObjExpr.compile` (at analysis, as the JVM loads the class) makes the class: its methods as
  `EvalMethod`s set per descriptor, with every covariant return; constructors (a record's two
  shorter ones too); `getBasis` and a record's `create` as static methods; a reify's `meta` and
  `withMeta` over a last hidden field. Its fields are the `NewInstanceExpr`'s closes: a deftype's
  fields, a reify's closed-over locals.
- In a method's frame slot 0 is the object; fields are read and set (`set!` of a mutable field)
  through `Dyn.getField`/`setField`. Code naming the stub (`(Foo. ...)`, `instance?`, a hinted
  field) is mapped to the class (`Evaluator.destub`).
- `gen-interface` (the Go build's `genclass.clj`) makes an interface with `Dyn.defineInterface`
  and `addInterfaceMethod` (Q5): its methods, called by reflection on an object of a run-time
  class, call that class's method of the same descriptor. `instance?` holds by `DynImplements`.

Left for phase 2: deftypes implementing closed-world interfaces with inline protocol methods
called through Go's interface dispatch are covered by `Dyn`, but neither checked against the
suite nor timed; `extend` on run-time classes works through `Class` objects as on the JVM.

## Loading

**The sources** are embedded in the executable: `bin/c2g --program` writes them under the main
package's forms (`go/arbace/cmd/arbace/res/arbace/...`, 41 files, 825 KB), lists them as the
package's `:embed-files`, declares `//go:embed res`, and `main` sets the host's `Resources` to
`fs.Sub(resources, "res")` before `Main.main`. The embedded tree is `arbace/**/*.clj` less the
tools (`classes`, `j2c`, `g2c`, `c2g`), the runtime's class forms (`arbace/lang`, compiled into
the program), ASM, and the namespaces decision 6 leaves out (`core.server`, `repl.deps`,
`java.basis`, `tools.deps.interop`, `java.process`, `java.shell`, `java.browse`, `java.javadoc`)
and D6's Swing and SAX (`inspector`, `xml`). Alternatives considered: reading the checkout at run
time (not standalone), pre-read forms (step 6's start-up work).

`RT.load` (variant) finds `NAME.clj` or `.cljc` among the embedded sources, then in the
directories of `ARBACE_PATH` (the JVM's class path; Clojure's test suite will need it), and
evaluates it with `Compiler.load`; there are no class files. RT's initialization loads
`arbace.core` when the program has its sources (the check programs without them, and the
evaluator's proof, run as before). `RT.doInit` has no socket server. `Compiler.loadFile` works
without `java.io.File`.

**Namespace variants** (`arbace/lang/go/ns/`, proposed amendment V1): the Go build's differences
in the namespaces, applied when the sources are embedded: `P.clj` replaces `arbace/P.clj`,
`P.after.clj` is appended to it, `P.subst.clj` (a vector of strings, old and new) replaces text
found exactly once. Used for:

| variant | why |
|---|---|
| `genclass.clj` | `gen-interface` at run time (Q5); `gen-class` throws (D6); ASM's imports out |
| `core_proxy.clj` | `proxy` expands to a throw (D6: core_print's `PrintWriter-on` and pprint's writers load, and fail when used) |
| `instant.clj` | decision 7: `#inst` read and printed over `java.util.Date`'s milliseconds, with GregorianCalendar's arithmetic (Julian before 1582-10-15, years of era); Calendar and Timestamp instants throw |
| `main.after.clj` | the REPL's requires trimmed (decision 6: no javadoc, no repl.deps; pprint needs proxy) |
| `main.subst.clj` | no `DynamicClassLoader` for the REPL's thread; the error report printed with `prn` into `java.io.tmpdir` through `FileOutputStream` |
| `java/io.subst.clj` | no reflection warnings at every start for the members D6 cuts (`java.nio` channels, `File`) |

**Cut classes.** The namespaces name classes outside the world (`(:import (java.nio.file
Files))`, `^java.util.stream.BaseStream`, `arbace.asm.Type/getDescriptor` in core's annotation
support). `bin/c2g --program` reads the embedded sources, collects the class names their forms
mention (qualified symbols, a qualified symbol's namespace, import lists, metadata), keeps those
the JVM has from the JDK or `arbace.lang`/`arbace.asm` and the world lacks, adds the JDK classes
their public members name (one level: `Files/createTempFile` returns a `Path`), and registers
them as cut classes with **member tables from the JVM's reflection whose members throw**
(`UnsupportedOperationException: ... is not in the Go build`); likewise the public members of
translated classes that do not exist in Go (`RT/toUrl`, `FileInputStream(File)`). Code naming
them analyzes, as on the JVM, and fails when it runs (D6's "names exist and throw"). 162 cut
classes in the REPL's program. `java.sql.Timestamp` is always cut: core's `when-class` guard
loads `instant` on it.

**jrt's reflection** grew by what core and the REPL ask at load: `System`'s `Class` and table,
the context class loader with resources (`getResourceAsStream` from the host, `getResources`
empty: no `data_readers.clj`), `String.format` and the regex methods, `System.out`,
`CASE_INSENSITIVE_ORDER`; a nil pointer result is null (`Box`); and overloads are listed in the
JVM's order, which `Reflector`'s choice among applicable overloads follows (`(Math/floorDiv c
1461)` with `c` an `Object` chose `(int, int)` before).

## Deviations from EVAL-PLAN

1. Each node evaluates itself (`evalIn`), not one `cond` in `Evaluator.eval` (§2): speed and
   locality; the entry point stays.
2. `EvalFn` binds from RestFn's seq (§2.2: "the arguments as a seq" packed into an array):
   realizing a lazy argument list changed the meaning of `apply` and `mapcat`.
3. A local's place is decided by identity among the closes (§2.1: the method's locals), see
   above.
4. Each fn has a run-time class named as the JVM's (§2.2 had one class): needs c2g's
   `c2g$class` rule (V2).
5. Exceptions are not wrapped in `CompilerException` (§2.8 said "with phase :execution around
   invokes and interop", which phase 2B did): compiled code does not wrap, and `catch` of the
   original class must work.
6. The evaluator mirrors the bytecode's conversions and casts where `Expr.eval` does not (list
   above); `*unchecked-math*`'s unchecked casts are not distinguished at run time (the node does
   not record it): checked casts always.
7. Loading needs the namespace variants and cut classes (§2.7 did not foresee them).

## Measurements

On this machine (64 cores, shared), go1.27.1, `GOGC` default:

| | amd64 | arm64 (qemu-aarch64) |
|---|---:|---:|
| executable (static, not stripped) | 37.4 MB | 35.5 MB |
| start to `-e nil` done (`arbace.core`, `spec.alpha`, `arbace.main`) | 4.2 s, 110 MB resident | 43.6 s, 190 MB resident |
| `arbace.core` alone (`ARBACE_LOAD_TIMES`) | about 1.9 s | 18.8 s |
| `bin/c2g --program` | 40-45 s wall | |
| `bin/g2c build --line-file`, warm | about 60 s | about 60 s |

Load times per namespace on amd64 (`ARBACE_LOAD_TIMES=1`, nested loads included in their parent):

| source | ms | source | ms |
|---|---:|---|---:|
| `core.clj` (all below to `core_classes`) | 1,929 | `main.clj` (with spec) | 2,297 |
| `core_print.clj` | 113 | `spec/alpha.clj` | 658 |
| `core_deftype.clj` | 144 | `core/specs/alpha.clj` | 213 |
| `core/protocols.clj` | 102 | `spec/gen/alpha.clj` | 84 |
| `gvec.clj` | 169 | `walk.clj` | 30 |
| `instant.clj` | 108 | `java/io.clj` | 197 |
| `string.clj` | 66 | `uuid.clj`, `genclass.clj`, `core_proxy.clj`, `core_classes.clj` | 22, 13, 6, 33 |


The JVM's `bin/arbace` starts in 0.2 s (AOT-compiled namespaces in a CDS cache). The evaluator
loads 8,436 lines of core and its helpers in 1.9 s; most of the rest is `spec.alpha` (its specs
of core's macros). The garbage collector is a large part: with `GOGC=400` the start takes 3.6 s
(user time 5.9 s against 14.9 s), with `GOMAXPROCS=1` 7.0 s. EVAL-PLAN §6 and step 6/7 are the
remedies (pre-read or pre-analyzed core, closure compilation, D7's `GOGC`).

## Checks

**`bin/arbace-go --smoke`** (new): `-e`, a script (protocols, records, deftypes, `ex-info`,
`loop`), the REPL on stdin, an error's exit status and message, `-` for stdin, on each of
`ARBACE_GO_ARCHES` (default amd64, arm64 under qemu): all pass on both.

**The oracle's forms corpus** (`bin/oracle check 'target/arbace-go/amd64/arbace -'`):

| file | pass | file | pass |
|---|---:|---|---:|
| `collections.clj` | 811 / 814 | `harvest/protocols.clj` | 7 / 7 |
| `destructuring_macros.clj` | 453 / 453 | `harvest/reader.clj` | 136 / 136 |
| `format.clj` | 324 / 326 | `harvest/reducers.clj` | 1 / 5 |
| `harvest/array_symbols.clj` | 44 / 45 | `harvest/sequences.clj` | 891 / 891 |
| `harvest/clojure_set.clj` | 108 / 108 | `harvest/special.clj` | 3 / 3 |
| `harvest/clojure_walk.clj` | 8 / 8 | `harvest/string.clj` | 94 / 107 |
| `harvest/control.clj` | 115 / 115 | `harvest/transducers.clj` | 102 / 102 |
| `harvest/data_structures.clj` | 616 / 616 | `harvest/transients.clj` | 7 / 7 |
| `harvest/def.clj` | 2 / 2 | `harvest/vars.clj` | 12 / 12 |
| `harvest/evaluation.clj` | 14 / 14 | `harvest/vectors.clj` | 58 / 66 |
| `harvest/for.clj` | 15 / 15 | `hashing.clj` | 266 / 268 |
| `harvest/keywords.clj` | 7 / 7 | `inst.clj` | 190 / 231 |
| `harvest/logic.clj` | 116 / 116 | `numbers.clj` | 814 / 822 |
| `harvest/macros.clj` | 24 / 24 | `polymorphism.clj` | 528 / 533 |
| `harvest/math.clj` | 4 / 149 | `printing.clj` | 366 / 396 |
| `harvest/multimethods.clj` | 8 / 8 | `reader.clj` | 466 / 466 |
| `harvest/numbers.clj` | 308 / 308 | `seqs.clj` | 554 / 555 |
| `harvest/other_functions.clj` | 224 / 224 | `sorting.clj` | 343 / 343 |
| `harvest/parse.clj` | 40 / 40 | `state_errors.clj` | 789 / 804 |
| `harvest/predicates.clj` | 15 / 17 | `strings.clj` | 454 / 462 |
| `harvest/printer.clj` | 27 / 27 |  | |
| **forms** | **9364 / 9652** | | |

The whole oracle (`bin/oracle check` without a selection) passes 19,508 of 19,828 cases: the
class scripts, run through the evaluator's reflection, 8,940 of 8,943 (the 3 others: two helpful
`NullPointerException` messages, `(Util/equiv ##NaN ##NaN)` true where the JVM's two boxed NaNs
differ), the regex corpus 1,204 of 1,233 (the 29 cases of phase 2C that need the JDK's resource
data). **linux/arm64 under `qemu-aarch64` gives the same results file for file** (each file about
10x slower to run: the start under qemu is about 44 s).

**`bin/c2g-check -- --program`**: the check programs embed the sources and load `arbace.core`,
binding `*print-namespace-maps*` as `arbace.main` does: 9,002 of 9,002 steps pass on linux/amd64 and linux/arm64 alike, the 47 that needed `arbace.core` among them (a check program is 38 MB; each builds in about 14 s once the packages are cached). Without `--program` the
check is as before (`test/c2g/needs-core.edn` still applies to it): 8,955 pass, 47 need core, 0 fail (amd64; no regression from the c2g changes).

**`bin/c2g-evalproof`**: the proof grows by `try`/`catch`/`finally`, `letfn*`, `case*`,
collection literals with locals, keyword invokes, monitors, `instance?` and `StackOverflowError`:
31 forms, the recorded output on amd64 and arm64.

**`bin/jrt test`** (jrt alone, amd64 and arm64): passes.

## Failure causes (the forms corpus)

Of the 288 forms that differ (amd64 and arm64 alike):

| cases | cause | where it belongs |
|---:|---|---|
| 145 | `arbace.math` does not load: jrt's `Math` lacks the trigonometric, exponential and hyperbolic functions (its table lists what the runtime needed) | jrt (phase 2, part 2) |
| 52 | HotSpot's helpful `NullPointerException` messages (`Cannot invoke "Object.getClass()" because ...`): jrt's are null (V11) | jrt or the oracle's acceptance |
| 37 | `java.util.Calendar`, `GregorianCalendar`, `java.sql.Timestamp`, `java.time.Instant` (decision 7 left them out; `#inst` over `Date` passes) | jrt (part 2) |
| 30 | `proxy` (pprint, `cl-format`, `PrintWriter-on`) | types at run time (part 1), D6 |
| 5 | the JDK surface cut or missing: streams (`.lines`, `.codePoints`, `Collectors`), JDK 21's sequenced collections (`LinkedHashMap`'s views), `UTF-16BE` | jrt (part 2) |
| 4 | `arbace.core.reducers` (`ForkJoinPool`, cut by D6) | D6 |
| 4 | `supers`/`bases`/`parents` of `Long` lack `java.lang.constant`'s `Constable` and `ConstantDesc` (cut) | jrt (part 2) |
| 3 | `:arglists` of core's vars are the macro's seqs, where the JVM's AOT-compiled core holds rebuilt lists (top-level `def` evaluates its metadata, as the JVM's eval does) | step 6 (pre-analyzed core) |
| 2 | message formats: jrt's array cast (`Cannot cast [I to [J`), `#inst [2020]`'s cast | jrt |
| 2 | identity of boxed NaN (`(let [x ##NaN] (identical? x x))`), identity hash of a `StringBuilder` (recorded by the JVM) | jrt |
| 2 | `String/join` with an `Iterable` (jrt's `String` table lacks the overload), `.formatted` | jrt (part 2) |
| 1 | `(deftype Foo/2 [a])`'s error comes from `import`'s macro, not `deftype*`'s analysis | types at run time (part 1) |
| 1 | `UUID/nameUUIDFromBytes` (`MessageDigest`, not in the world) | jrt |

None is the evaluator's own: what the evaluator got wrong while the corpus ran was fixed (above:
lazy arguments, the bytecode's conversions, constants, error locations).

## A split for phase 2

Parallel parts, each with its own check (the agents should work on separate files where they
can; the shared files are `arbace/lang/go/Compiler.clj`, `arbace/c2g/out.clj` and jrt's tables):

1. **Types made at run time** (EVAL-PLAN §2.5, Q5): `deftype`/`reify`/`defrecord`/protocols
   checked against the oracle's `polymorphism` and the suite's `protocols`, `data_structures`
   and `reflect` tests; `proxy` over `Dyn` for interfaces and abstract classes with a Go-side
   superclass (pprint's writers, `PrintWriter-on`; D6 left `proxy` out at first: the user's
   call); `gen-class` stays out. Files: `Compiler.clj` (`NewInstanceExpr`, `Dyn`), `dyn.clj`,
   `ns/core_proxy.clj`.
2. **jrt's surface for the REPL** (Q2 for the hand-written classes): the hand-written classes'
   tables list only what the manifest declared for the runtime: `Math`'s trigonometric and
   exponential functions (all of `arbace.math`, 145 cases), `String.join(CharSequence,
   Iterable)`, `Thread`, `ClassLoader`; the JDK 21 sequenced collections (`LinkedHashMap`'s
   views need `SequencedSet`); more charsets (`UTF-16BE`); `java.time.Instant`, `Calendar`,
   `Timestamp` (decision 7's rest); the helpful `NullPointerException` messages or their
   acceptance in the oracle as `bin/c2g-check` accepts them (V11); the REPL's `(source f)`
   (a `NullPointerException`: `source-fn` reads the resource through the base loader). Files: `go/arbace/jrt`,
   `test/jrt`, `bin/jrt-convert`'s sources.
3. **Clojure's test suite on the Go build**: a runner (`bin/clojure-tests` with the Go executable,
   the suite through `ARBACE_PATH`), recorded per namespace as `test/arbace-results.edn` is; then
   the failures by namespace. Files: `bin/`, `test/`.
4. **Start-up and speed** (steps 6, 7): pre-read or pre-analyzed namespaces, `GOGC`, the
   evaluator's per-arity `invoke` (Q3) and closure compilation; locals clearing (the bytecode
   clears a local after its last use: the evaluator keeps it, so a head held in a frame slot
   is not collected).

## Proposed amendments (for the user's review)

- **V1 (C2G-SPEC §10.3) Namespace variants.** The Go build's differences in the namespaces live
  in `arbace/lang/go/ns/`: `P.clj` replaces `arbace/P.clj` in the executable, `P.after.clj` is
  appended, `P.subst.clj` replaces strings found exactly once; applied by `bin/c2g --program`
  when it embeds the sources.
- **V2 (§5.11, §10.2) A run-time class per fn.** A class with an instance field `c2g$class`
  answers `getClass` with it when set; the evaluator gives each `FnExpr` a class made at run
  time, a subclass of `EvalFn` named as the JVM names the fn's class.
- **V3 (§4.1, §10.6) The REPL's cut classes and absent members.** `--program` registers the
  classes the embedded namespaces name outside the world as cut classes, with member tables from
  the JVM's reflection whose members throw, and lists the public members of translated classes
  that do not exist in Go in their tables, throwing; the code naming them analyzes.
- **V4 (§5.11) Overload order.** Member tables (c2g's and jrt's) list a name's overloads in the
  order the JVM's reflection gives them; `Reflector` chooses by that order.
- **V5 (§10.3, §9.4) Embedded sources.** `--program` embeds the namespaces' sources in the main
  package (`//go:embed res`, the host's `Resources`); `RT.load` reads them, then `ARBACE_PATH`; a
  forms directory holding `.g2c-data` is data to `bin/g2c` (amendment to SPEC §4: g2c's program
  layout).
- **V6 (§10.7) Evaluated frames.** jrt's `EvalTrace` hook and the frame mapping above.
- **V7 (§9.1) JDK variants for VarHandle and SecureRandom**: `ByteArray`,
  `ByteArrayLittleEndian` and `UUID.randomUUID` (overlay/jdk/variants).
- **V8 (C2G-SPEC §7) c2g fixes** found with the whole world: a `this(...)`/`super(...)` call of a
  constructor outside the world is a missing operation; catch clauses of classes outside the
  world are dropped; `identical?` of two interface types compares as `any`; volatile fields have
  setters in member tables; a superinterface outside the world stands for its superinterfaces.

## Sources

Nothing vendored. Studied: upstream Clojure's `Compiler.java` as Arbace's `arbace/lang/Compiler.clj`
holds it (the emit methods each evaluation mirrors), `clojure.main`'s error triage
(`arbace/main.clj`); openjdk/jdk26u `src/java.base/share/classes/java/util/GregorianCalendar.java`
(the default cutover) and HotSpot's `AbstractMethodError` and checkcast messages (from the JVM);
H. Hinnant, "chrono-Compatible Low-Level Date Algorithms" (civil-from-days, days-from-civil).
