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

All accepted 2026-10-09 and folded, renamed M1-M8 so as not to collide with C2G-SPEC's
deviations V1-V13 (C2G-SPEC §16 has the scheme: phase 1 M, phase 2A X, phase 2B S).

- **V1 (C2G-SPEC §10.3) Namespace variants.** The Go build's differences in the namespaces live
  in `arbace/lang/go/ns/`: `P.clj` replaces `arbace/P.clj` in the executable, `P.after.clj` is
  appended, `P.subst.clj` replaces strings found exactly once; applied by `bin/c2g --program`
  when it embeds the sources. *Accepted 2026-10-09, folded into C2G-SPEC §10.3 as M1.*
- **V2 (§5.11, §10.2) A run-time class per fn.** A class with an instance field `c2g$class`
  answers `getClass` with it when set; the evaluator gives each `FnExpr` a class made at run
  time, a subclass of `EvalFn` named as the JVM names the fn's class. *Accepted 2026-10-09,
  folded into C2G-SPEC §5.11 and §10.2 as M2 (and EVAL-PLAN §2.2).*
- **V3 (§4.1, §10.6) The REPL's cut classes and absent members.** `--program` registers the
  classes the embedded namespaces name outside the world as cut classes, with member tables from
  the JVM's reflection whose members throw, and lists the public members of translated classes
  that do not exist in Go in their tables, throwing; the code naming them analyzes. *Accepted
  2026-10-09, folded into C2G-SPEC §4.1 as M3.*
- **V4 (§5.11) Overload order.** Member tables (c2g's and jrt's) list a name's overloads in the
  order the JVM's reflection gives them; `Reflector` chooses by that order. *Accepted
  2026-10-09, folded into C2G-SPEC §5.11 as M4.*
- **V5 (§10.3, §9.4) Embedded sources.** `--program` embeds the namespaces' sources in the main
  package (`//go:embed res`, the host's `Resources`); `RT.load` reads them, then `ARBACE_PATH`; a
  forms directory holding `.g2c-data` is data to `bin/g2c` (amendment to SPEC §4: g2c's program
  layout). *Accepted 2026-10-09, folded into C2G-SPEC §10.3 as M5; the `.g2c-data` rule into
  BUILD.md, "The program's layout" (g2c's program layout is BUILD.md's, not SPEC §4's).*
- **V6 (§10.7) Evaluated frames.** jrt's `EvalTrace` hook and the frame mapping above.
  *Accepted 2026-10-09, folded into C2G-SPEC §10.7 (and §11) as M6.*
- **V7 (§9.1) JDK variants for VarHandle and SecureRandom**: `ByteArray`,
  `ByteArrayLittleEndian` and `UUID.randomUUID` (overlay/jdk/variants). *Accepted 2026-10-09,
  folded into C2G-SPEC §4.6, where the JDK's variants are (and §11, `UUID`'s natives), as M7.*
- **V8 (C2G-SPEC §7) c2g fixes** found with the whole world: a `this(...)`/`super(...)` call of a
  constructor outside the world is a missing operation; catch clauses of classes outside the
  world are dropped; `identical?` of two interface types compares as `any`; volatile fields have
  setters in member tables; a superinterface outside the world stands for its superinterfaces.
  *Accepted 2026-10-09, folded into C2G-SPEC §4.1 and §5.11 as M8 (the identity rule was
  already §5.8's).*

## Phase 2A: types made at run time

Branch `eval-2a` (2026-10-09), part 1 of the phase-2 split: `proxy` over `Dyn` (the user's
decision of 2026-10-09, reversing D6 for `proxy`; `gen-class` stays out), and `deftype`,
`defrecord`, `reify`, `definterface`, protocols checked against the JVM with a new oracle file.

### proxy

**The Go side** (`arbace/c2g/dyn.clj`). A proxy of `Object` is a `Dyn`. A proxy of a class `C`
must be a Go value of `C_I` (translated code calls it as a `C`: `(PrintWriter. proxy-writer)`,
`*out*` bound to a proxy), so for each class of `proxy-supers` c2g writes a Go type
**`DynSub_C`** in `c2g_dyn.go`: a struct embedding `C`'s struct (so `Self_C`, `C`'s fields and its
`Impl_` methods are promoted), with `Dyn`'s `D` (the run-time class) and `F` (the fields), and a
method for every method of every interface of the world (as `Dyn`) and every virtual method of
`C`. Each method calls its slot's fn; when the slot is unset or the fn answers the marker
`dynSuper` (`Compiler$Dyn.superMarker`), it runs `C`'s implementation (`(.Impl_m t t ...)`, the
super call, or the interface's default, or `AbstractMethodError` with HotSpot's text for an
abstract class's method). `Object`'s `toString`, `hashCode` and `equals` fall back to `C`'s or
the header's; `Dyn`'s own three methods now honour the marker too. The constructors are `C`'s
non-private ones: allocate the `DynSub_C`, run `C`'s initialization and constructor body
(`(.Ctor_... (.-C t) t args)`), as a translated subclass's constructor does.

`proxy-supers` (the classes a proxy may extend besides `Object`): `java.io.Writer` (pprint's
writers, `PrintWriter-on`, cl-format's case writers), `Reader`, `PushbackReader` (`arbace.repl`'s
`source-fn`), `InputStream`, `OutputStream`, `arbace.lang.APersistentMap` (`bean`), and jrt's
hand-written `ThreadLocal` (`clojure.test.check.random`; its constructor body and `Impl_`
methods follow the shapes of translated non-leaf classes). A class must be translated or
hand-written, not final and not a leaf (a leaf's values are `*C`, §5.3: `BufferedWriter` or
`BitSet` cannot be extended without making them non-leaf, which changes every use; a proxy of
one, or of a class outside the list, expands to a throw of `UnsupportedOperationException`
"proxy of C is not in the Go build", so code naming it loads). Each type has about 650 methods;
the seven add about 4 MB to the executable (amd64 37.4 to 41.6 MB, arm64 35.5 to 39.3 MB). Their
roots (`dyn/proxy-roots`: `C`'s constructors and instance methods, up its superclass chain) keep
the implementations translated.

`Compiler$Dyn` gains two natives: `defineProxyClass(name, super, interfaces)` (a `Dyn` class
with one private field, the fn map, and a no-argument constructor for `Object`; else a class
over `DynSub_C`, its superclass `C`, its interfaces with `C`'s, its constructors `C`'s) and
`superMarker()`. `DynClass` holds its slot table (`SlotMap`: `Dyn`'s or the `DynSub_C`'s) and
whether it is a proxy; `setMethod` on a proxy's method that has a slot sets only the slot (the
method is reached by reflection through `C`'s or the interface's member, which calls the Go
method, so `proxy-super`'s reflective call runs `C`'s implementation); a method without a slot
(of an interface made at run time) gets a member as for `deftype`. The natives taking an object
work on both kinds through the Go interface `dynObject` (`DynClassOf`, `DynFields`). A third
native, `fillProxySlots(c, factory)`, gives the superclass's methods that reflection does not
list (the protected ones of jrt's classes: `ThreadLocal.initialValue`) the fn `(factory name)`,
so a proxy can override them as on the JVM. jrt's
stack traces leave out `DynSub_C` dispatch frames as they do `Dyn`'s.

**The Clojure side** (`arbace/lang/go/ns/core_proxy.clj`, replacing the variant that expanded
`proxy` to a throw): `proxy-name`, `get-proxy-class`, `construct-proxy`, `init-proxy`,
`update-proxy`, `proxy-mappings`, `proxy`, `proxy-call-with-super`, `proxy-super` are upstream's;
`generate-proxy` is replaced by `define-proxy`, which chooses the same methods as
`generate-proxy` (the superclass's non-final public and protected instance methods, the
interfaces' and the abstract ones, the bridges of covariant groups) and sets each to a fn that
looks the method's name up in the fn map (field 0): the mapped fn is applied to the proxy and the
arguments; without one a superclass method answers the marker and an interface or abstract
method throws `UnsupportedOperationException` with the name, as the generated bytecode does. Of
`Object`'s methods only the three a `Dyn` can override are set. The IProxy methods read and write
field 0. The class is named and cached as on the JVM (`user.proxy$java.lang.Object$IDeref$...`);
`bean` is upstream's over the properties found by reflection as `java.beans.Introspector` finds
the readable ones (`getX()`, `isX()` returning boolean, `Introspector.decapitalize`), since
`java.beans` is not in the Go build.

Not done: proxies of leaf classes and of classes outside `proxy-supers`; serialization of
proxies (`NotSerializableException`: `ObjectOutputStream` is not in the Go build); methods with
more than 18 parameters (the JVM calls super for them; here the fn is called).

**pprint at the REPL.** pprint loads now (`(require 'arbace.pprint)`, `cl-format`,
`with-out-str` of `pprint`), but loading it takes about 7.9 s on the evaluator (`cl_format.clj`
4.1 s: its directive tables and the formatters compiled at load), so the REPL still does not
require it at start and the error report stays `prn` (`main.after.clj`, `main.subst.clj`):
both go back to the JVM's once start-up work (pre-analyzed namespaces, steps 6-7) makes the
load cheap. Loading `core_proxy` itself takes about 0.1 s more than the throwing variant (the
proxy classes of `bean` and, in `core_print`, `PrintWriter-on` are made at load, as the JVM
loads them).

### Fixes found by the checks

- A `deftype` or `reify` defining a method twice (`(reify java.util.List (size [_] 10)
  java.util.Collection (size [_] 20))`) is the JVM's `ClassFormatError` ("Duplicate method name
  "size" with signature "()I" in class file ..."), thrown by `Evaluator.defineType` when the
  class is made, so the analyzer wraps it in `CompilerException` as on the JVM; jrt gains the
  stand-in `ClassFormatError` (`test/jrt/standins.clj`).
- `(set! (.x this) v)` and `(set! (.-x this) v)` in a deftype's method (the field resolved on the
  stub class) set the object's field, as `putfield` does, instead of going through reflection,
  which does not see mutable fields (the suite's `java_interop` `test-set!`).
- `ArityException` of an evaluated fn applied to a seq that is not `Counted` (`apply`, a proxy's
  method fn) counts the arguments up to 21, as `AFn.applyToHelper` does, instead of stopping at
  the fn's largest arity.

### The oracle's new file

`test/oracle/forms/types.clj` (425 forms, recorded on the JVM): `definterface` (primitive and
`String` hints, hinted and reflective calls, `AbstractMethodError` of an unimplemented method),
protocols (inline, `extend-type` on `String`, `nil`, `Long`, `List`, `Object`, `long[]`,
`extend-protocol`, `extend` on a record, `satisfies?`, `extends?`, `extenders`, docstrings and
`:sigs`, multi-arity methods, `recur` in a method, redefining an extension,
`:extend-via-metadata`), `instance?`, `cast`, `class`, `supers`, `getInterfaces`, `getFields` and
`getBasis` of run-time classes; records as maps (lookup, `assoc`/`dissoc` of basis and extra
keys and the resulting class, equality and hashes against maps and records, `map->R`, `R/create`,
the four-argument constructor, metadata, `into`, `merge`, `conj`, `reduce-kv`, sets and map keys,
printing with `*print-namespace-maps*`, `*print-length*`, `*print-meta*`, primitive fields and
their conversions, `empty`'s exception); deftypes with `^:unsynchronized-mutable` and
`^:volatile-mutable` fields (set through `set!` of the local and of `(.x this)`, private to
reflection), primitive fields of every kind, `toString`/`equals`/`hashCode` overrides in sets,
maps, `distinct`, `format`; `IHashEq`; `IFn` implementations (arities, `apply`, `map`, missing
arities); `ILookup`, `IPersistentVector` (covariant `cons`: the class through each interface),
`ISeq`, `Counted`, `Seqable`, `Iterable`, `CollReduce`, `Comparable`, `IDeref`/`IPending`,
`IObj`/`IMeta`, `Named` by deftype; reify (closures, identity of its class, metadata,
`Comparator`, `Iterator`); proxy (of `Object` with `toString`/`equals`/`hashCode`, of interfaces,
an unmapped interface method, of `Writer` used directly, through `binding [*out*]` and through
`PrintWriter`, of `PushbackReader` with `proxy-super`, of `InputStream` and `OutputStream`,
`update-proxy` to a fn and to nil, `init-proxy`, `proxy-mappings`, `get-proxy-class` and
`construct-proxy`, a method defined twice, an unresolvable class, a protocol's interface);
`bean`; pprint and `cl-format`'s case conversion writers; the duplicate-method errors.

Left out as JVM-only: a record's `getFields` lists the compiled class's `const__N` statics (the
form filters them); `AbstractMethodError` of a record called with an arity it lacks says "Method
user/R.invoke(...) is abstract" on HotSpot (the record's class has HotSpot's abstract overpass)
where the Go build gives the "does not define or inherit" text of a deftype; neither is about
the evaluator's meaning.

### Results

| | phase 1 | phase 2C (main) | with 2A |
|---|---:|---:|---:|
| oracle forms (existing files) | 9,364 / 9,652 | 9,416 / 9,652 | 9,446 / 9,652 |
| `printing.clj` | 366 / 396 | 366 / 396 | 396 / 396 |
| `types.clj` (new) | | | 425 / 425 |
| oracle forms, all | | | 9,871 / 10,077 |
| whole oracle (forms, classes, regex), amd64 | 19,508 / 19,828 | 19,562 / 19,828 | 20,017 / 20,253 |

(2C's numbers count V11's helpful-NPE rule; 2A alone, before merging main: forms 9,393 of 9,652
and 9,818 of 10,077, the whole oracle 19,962 of 20,253.)

linux/arm64 under `qemu-aarch64` gives the same forms results file for file.

Clojure's suite on the Go build (`CLOJURE_TESTS_GO=... bin/clojure-tests`, phase 2C's runner,
after merging main): 1,874 assertions pass (1,852 in 2C's reference), `printer` 74 of 74 (13
errors before: pprint's writers), `protocols` 196 of 196 (a proxy error and the
method-defined-twice failure before), `proxy.examples` loads; `data-structures-interop`,
`parse`, `sequences` and `transducers` get past `clojure.test.check.random`'s proxy of
`ThreadLocal` and now stop at jrt's `Math/exp` (part B). The reference
`test/arbace-go-results.edn` is updated accordingly. The one new
mismatch in the existing files, `polymorphism.clj:176` (`class-ambig`), names the two ambiguous
interfaces in the other order: the order is that of a hash map keyed by `Class` objects, whose
hashes are identity hashes, which the classes made while `core_proxy` loads shift (it passed by
the same luck before). Fixed afterwards (2026-10-09, the user's decision): two classes are
named in the order of their names (VENDOR-NOTES.md, hand change 14), on the JVM as on Go, and
the case re-recorded. Clojure's suite on the Go build (`ARBACE_PATH` the renamed suite):
`protocols` 196 of 196 (195 before the duplicate-method fix), `printer` 74 of 74 (`bean`),
`transients` 35 of 35, and `java_interop`'s proxy tests (`test-proxy-chain`, `test-bases`,
`test-supers`, `test-proxy-abstract-super`, `test-iterable-bean`, `test-set!`) pass;
`test-proxy-method-order` reads the class with ASM's `ClassReader` (not in the Go build) and
`test-proxy-super` extends `java.util.BitSet` (not in the world); the namespaces that do not load
for other reasons (`java_interop`: `arbace.inspector`, the suite's Java classes;
`data_structures`: `clojure.test.generative`; `reflect`: the suite's Java classes; `pprint`:
`java.util.concurrent.Semaphore`; `vectors`: `Collectors`) are part 2's and 3's.
`bin/arbace-go --smoke` (amd64), `bin/c2g-evalproof` (amd64), `bin/jrt test` (amd64) and
`bin/c2g-check -- --program` (amd64: 9,002 of 9,002 steps) pass.

### Proposed amendments (for the user's review)

All accepted 2026-10-09 and folded, renamed X1-X4 (C2G-SPEC §16).

- **A1 (C2G-SPEC §5.12, §5.3) Proxies of a class: `DynSub_C`.** For each class of a configured
  list (`arbace.c2g.dyn/proxy-supers`: translated, not final, not a leaf), c2g writes a Go type
  embedding the class's struct, with `Dyn`'s methods and the class's virtual methods, each
  calling its slot's fn and else the class's implementation; `Compiler$Dyn.defineProxyClass` and
  `superMarker`; a slot fn answering the marker runs the superclass's implementation. §5.3's
  "nothing can subclass a class at run time" then reads: nothing but a proxy of a listed class.
  *Accepted 2026-10-09, folded into C2G-SPEC §5.12 and §5.3 (and V9) as X1. The list's classes
  may be hand-written too (jrt's `ThreadLocal`), as above, not only translated.*
- **A2 (C2G-SPEC §10.4; B1-PLAN D6) `proxy` over `Dyn`**, the namespace variant
  `core_proxy.clj` above; `bean` by reflection; `gen-class` stays out. *Accepted 2026-10-09,
  folded into C2G-SPEC §10.4 and §12 as X2, B1-PLAN D6 (partly reversed) and EVAL-PLAN §2.5.*
- **A3 (C2G-SPEC §5.12) A duplicate method is `ClassFormatError`** when a class is made at run
  time, with the JVM's message; jrt's stand-in `ClassFormatError`. *Accepted 2026-10-09,
  folded into C2G-SPEC §5.12 as X3.*
- **A4 (ORACLE.md) The oracle covers proxy** (`types.clj`), no longer excluded. *Accepted
  2026-10-09, folded into ORACLE.md ("Exclusions") as X4; the harvest still leaves out the
  suite's `proxy` namespace and forms naming `proxy` (`harvest.clj`).*

## Phase 2C: Clojure's test suite on the Go build, and the oracle's NPE rule

Branch `eval-2c` (2026-10-09), part 3 of the split above plus V11's acceptance in the oracle.

### The oracle's rule for helpful NullPointerException messages (V11)

By the user's decision (2026-10-09) a `NullPointerException` whose JVM message is one of
HotSpot's helpful ones (`Cannot invoke "Object.getClass()" because "<parameter1>" is null`) is
compared by class only. `test/oracle/runner.clj` marks such chain entries when it records (and
when it reads what the implementation printed): `[class message :helpful-npe]`, helpful meaning
that the whole message matches the grammar of HotSpot's `print_NPE_failed_action` and
`print_NPE_cause` (`helpful-npe-re`); `check` then compares a marked entry by class only, and
counts the cases that match only so (`N by V11` per file and in the summary). Explicit
`NullPointerException`s (a message the code chose, or none) are compared as before.
[ORACLE.md](ORACLE.md), "Helpful NullPointerException messages", has the details. Re-recorded:
12 expected files, 54 entries marked (52 forms, 2 class script steps), nothing else changed. (The
rule is the user's decision, so no amendment: ORACLE.md describes it, and C2G-SPEC §7.9.5 and
V11 refer to it. The gate proposal below is still open.)

Results: `bin/oracle check jvm` 19,828 of 19,828 (none by V11). On the Go build (amd64) the 52
forms all pass now: the forms corpus 9,416 of 9,652 (9,364 before), the whole oracle 19,562 of
19,828 (19,508 before; 54 by V11): class scripts 8,942 of 8,943 (the boxed NaN's identity
remains), regex 1,204 of 1,233 (unchanged).

### The runner

`bin/clojure-tests` runs the suite on the Go build with `CLOJURE_TESTS_GO`, the command running
it:

    CLOJURE_TESTS_GO=$PWD/target/arbace-go/amd64/arbace bin/clojure-tests -j 16

It implies rename mode: the suite and the test libraries renamed as for the JVM (the same
`arbace.j2c.rename`, on the JVM), into `.tmp/clojure-tests/go/` (`CLOJURE_TESTS_RUN`, default
`go`). Listing the namespaces and the report run on the JVM's `target/stage2` (`CLOJURE_SRC`);
each test namespace runs in its own Go process (`TMPDIR` the run's `tmp/`, `ARBACE_PATH` the
renamed libraries then the suite's `test/`), which, as upstream's JVM does, requires every test
namespace first. The fixtures step is skipped: the Go build loads no class files, so the AOT
fixtures load from source and the Java fixtures do not exist. The reference is
`test/arbace-go-results.edn` (the default `CLOJURE_TESTS_EXPECTED` in this mode), in the shape
of `test/arbace-results.edn`, with a comment per namespace that fails saying why. Exit status as
for the JVM: 0 when nothing differs from the reference (the counts of tests are compared
exactly, so an improvement is reported too, and the reference is then rewritten from
`.tmp/clojure-tests/go/results.edn`).

`test/run_clojure_tests.clj` reads and writes its files through `FileInputStream` and
`FileOutputStream` (the Go build has no `java.io.File`; `slurp` of a path string goes through
`io/as-url`, cut); its `generative` mode takes the namespaces from a file when given one
(`tools.namespace` searches directories with `File`).

**Exclusions** (`:skipped`, 19 besides upstream's two fixtures), the namespaces that cannot load
in the Go build by design:

| namespace | why |
|---|---|
| `compilation` | Java fixtures `compilation.TestDispatch`, `JDK8InterfaceMethods`, `ClassWithFailingStaticInitialiser`; `compile` writes class files |
| `generated-all-fi-adapters-in-let`, `generated-functional-adapters-in-def`, `generated-functional-adapters-in-def-requiring-reflection` | Java fixture `arbace.test.AdapterExerciser` |
| `java-interop` | Java fixtures `FIConstructor`, `FIStatic`, `FunctionalTester`, `AdapterExerciser`; `arbace.inspector` |
| `param-tags` | Java fixtures `SwissArmy`, `ConcreteClass` |
| `reflect` | Java fixture `reflector.IBar` |
| `try-catch` | Java fixture `ReflectorTryCatchFixture` |
| `genclass`, `genclass.examples` | `gen-class` (stays out) and its AOT-compiled examples |
| `java.javadoc`, `java.process`, `java.shell`, `repl.deps`, `server`, `clojure-xml` | the namespace under test is not in the Go build (D6) |
| `metadata` | lists the public vars of `arbace.inspector` (D6, Swing) |
| `serialization` | Java serialization (`ObjectOutputStream`, D6) |
| `reducers` | `arbace.core.reducers` needs `ForkJoinPool` (D6) |

On the JVM these hold 138 tests and 1,148 assertions. The other namespaces run, and their load
errors are recorded, so that the work of parts 1 and 2 shows in the reference.

**test.generative** does not run on the Go build (the runner's `--generative` forces it): its
runner requires `clojure.data.generators` (`java.util.Random`, below) and
`clojure.tools.namespace.find`, which imports `java.util.jar.JarFile` (cut, D6).

### Results

`bin/clojure-tests` with `CLOJURE_TESTS_GO` on linux/amd64 (not run under qemu: a namespace's
process takes about a minute on amd64, about ten under `qemu-aarch64`):

| | JVM (`test/arbace-results.edn`) | Go build |
|---|---:|---:|
| namespaces run | 83 | 64 (21 skipped) |
| namespaces loading | 83 | 46 |
| tests | 809 | 289 |
| assertions | 20,750 | 1,906 |
| passing | 20,750 | 1,852 (20 fail, 34 errors) |
| namespaces passing as on the JVM | 83 | 40 (1,557 assertions) |

Per namespace (tests and assertions on the JVM, then on the Go build):

| namespace | JVM | Go | cause (part) |
|---|---|---|---|
| `agents`, `array-symbols`, `clojure-set`, `clojure-walk`, `control`, `data`, `def`, `errors`, `evaluation`, `fn`, `for`, `keywords`, `logic`, `macros`, `main`, `multimethods`, `ns-libs`, `other-functions`, `parallel`, `protocols.hash-collisions`, `rt`, `run-single-test`, `special`, `string`, `tap`, `test`, `test-fixtures`, `transients`, `vars`, `volatiles` (and 10 namespaces without tests) | 234 / 1,557 | 234 / 1,557, all pass | |
| `clearing` | 3 / 31 | 12 pass, 19 fail | part 4: an evaluated fn has no fields for its closed-over locals (the test reads them by reflection), and `^:once` fns do not clear them |
| `method-thunks` | 4 / 20 | 17 pass, 3 errors | B: `java.io.File`'s constructor |
| `printer` | 13 / 74 | 61 pass, 13 errors | A: pprint's writers are proxies |
| `protocols` | 23 / 196 | 23 / 189: 187 pass, 1 fail, 1 error | A: `proxy`; a `reify` defining a method twice is not an error |
| `repl` | 7 / 22 | 18 pass, 4 errors | B: `source-fn`'s `NullPointerException` |
| `streams` | 5 / 30 | 5 / 13, 13 errors | B: `java.util.stream` (cut) |
| `api`, `data-structures`, `edn`, `generators`, `numbers`, `reader` | 130 / 12,786 | load errors | B: `clojure.data.generators` needs `java.util.Random`, whose class initialization fails (`Unsafe.objectFieldOffset`, which jrt lacks) |
| `data-structures-interop`, `parse`, `sequences`, `transducers` | 107 / 1,319 | load errors | A: `clojure.test.check.random` makes a `proxy` (of `ThreadLocal`) |
| `vectors` | 17 / 1,583 | load error | B: `java.util.stream.Collectors` (cut) |
| `predicates` | 4 / 1,085 | load error | B: `java.net.URI`'s constructor (cut) |
| `pprint` | 58 / 474 | load error | B: `java.util.concurrent.Semaphore`; A: pprint's writers |
| `math` | 41 / 262 | load error | B: jrt's `Math` (`sin` and the rest of `arbace.math`) |
| `java.io` | 15 / 115 | load error | D6: `java.net.ServerSocket`; B: `java.io.File` |
| `atoms` | 5 / 23 | load error | B: `java.util.function.IntSupplier` is not in the world |
| `delays` | 5 / 25 | load error | B: `java.util.concurrent.CyclicBarrier` is not in the world |
| `proxy.examples` | 0 / 0 | load error | A: `proxy` |

So by part: **B** (jrt's surface) blocks the most: `java.util.Random` alone holds back 12,786
assertions; then `Collectors`, `URI`, `Math`, `File`, `IntSupplier`, `CyclicBarrier`,
`Semaphore`, streams, `source-fn`. **A** (proxy) blocks `test.check` (4 namespaces, 1,319
assertions), pprint and two tests of `protocols`, and `reify`'s duplicate method check.
**Part 4** (the evaluator's fns and locals clearing): `clearing`.

**Fixed here, the evaluator's own** (`arbace/lang/go/Compiler.clj`, `arbace/lang/go/RT.clj`):

1. *Method values in the namespace they were analyzed in.* A `Class/method` value is a fn that
   `QualifiedMethodExpr` builds and analyzes (`buildThunk`): the JVM does it when it emits the
   enclosing code, in the namespace being compiled; the evaluator did it each time the value was
   evaluated, in the current namespace, where the class's short name may not resolve
   (`method-thunks`: `Tuple/create` evaluated during `run-tests` in `user`). The variant records
   the namespace at analysis (its constructor) and builds the thunk once, there.
2. *`def` of an existing dynamic var.* `DefExpr.eval` sets the var's dynamic flag to the def's,
   `DefExpr.emit` only sets it when the def says `^:dynamic`. The JVM runs `arbace.core`
   AOT-compiled, so `core_print`'s `(def print-initialized true)` leaves core's dynamic var
   dynamic there, and not in the evaluator (`rt`: binding it threw, and the unbalanced
   `pop-thread-bindings` ended the process). Now a top-level def in a source embedded in the
   program (`RT.load` binds `Evaluator.EMBEDDED_LOAD` while it loads one) keeps the flag, as the
   emitted code does, and a def inside a fn (`evalIn`) runs as `emit` compiles it (dynamic flag
   only when set; meta, then the root). Top-level defs elsewhere keep `eval`'s semantics, as the
   JVM's REPL and `load` have them.
3. *`case`'s performance warning.* "case has int tests, but tested expression is not primitive"
   is printed by `CaseExpr.emitExprForInts`, which the Go build never runs: the variant's
   constructor (a copy of `CaseExpr`'s) prints it at analysis (`control`).
4. *`*unchecked-math* :warn-on-boxed`.* `StaticMethodExpr.isBoxedMath` reads `Numbers`' methods'
   `WarnBoxedMath` annotations, which jrt's reflection does not have (`test.check` sets the
   option, and every boxed call failed to analyze): the variant knows the 24 annotated names
   (each name's overloads are all annotated `false`), and warns as the JVM does.

### Times and resources

On this machine (64 cores, shared with two other agents' work): the suite takes **4 min 36 s**
with `-j 16` (102 CPU-minutes); the JVM's, in the gate, 3 min 15 s with `-j 24` (renaming and fixtures included). A namespace's process takes 58 to 102 s,
almost all of it requiring the test namespaces (and their libraries) through the evaluator.
`GOGC` matters here: one process with the default `GOGC=100` takes 63 s and 203 s of CPU (289
MB resident), with `GOGC=400` 56 s and 89 s of CPU (650 MB), with `GOMAXPROCS=4` 88 s (339 MB).
The runner sets `GOGC=400` for its processes unless `GOGC` is set: 16 processes need about 10
GB. Start-up work (part 4: pre-read or pre-analyzed namespaces) would shorten every process.

### For the gate (done, 2026-10-09; `bin/gate` unchanged)

The user's decision (2026-10-09): `bin/gate --full` runs one more chain (each step its own log in
`.tmp/gate/` and line in the summary), concurrently with g2c's round trips once the three suites
(stages 1 and 2, j2c's) are done:

1. `jrt-convert`: `bin/jrt-convert`, then `go-build`: `ARBACE_GO_ARCHES=amd64 bin/arbace-go
   --build` (c2g and the Go build, the Go cache warm);
2. then concurrently: `go-smoke` (`ARBACE_GO_ARCHES=amd64 bin/arbace-go --smoke`); `suite-go`
   (`CLOJURE_TESTS_GO=target/arbace-go/amd64/arbace bin/clojure-tests -j 12`, against
   `test/arbace-go-results.edn`); and `oracle-go` (`bin/oracle check 'target/arbace-go/amd64/arbace
   -' --timeout 900 --expected test/oracle/known-go-amd64.edn`).

The oracle's new `--expected FILE` (ORACLE.md) passes when the mismatching cases are exactly
those of the reference, `test/oracle/known-go-amd64.edn`: the 32 of main at `4284edc`, in five
groups with their reasons (`deftype Foo/2`'s error source, the `StringBuilder` identity hash, the
JVM's recorded `dcmpg` bug, 5 `\N{name}` and 24 `CANON_EQ` regex cases needing the JDK's
resource data); `--write-expected FILE` rewrites it. The timeout is 900 s, not 300: the reducers
file takes about 285 s on the Go build. Measured (64 cores; the host's memory read 46 GB in the morning, 62-64 GB in the afternoon):

- The proposal's layout, everything concurrent from the start, does not fit: the three suites
  (24 test JVMs each) use 40-59 GB at their peak by themselves, and the OOM killer took g2c's
  converter in both runs that started converters beside them (once with jrt-convert's too);
  `suite-stage1` crashed in the first. So g2c and the Go chain wait for the three suites
  (`converters` in `bin/gate`), and g2c no longer runs beside them either.
- With that: `bin/gate --full` passes in **21m24s** (the bootstrap 1m31s; the suites on stages
  2 and 1 3m37s and 3m36s, j2c 5m12s; then g2c 1m27s, `jrt-convert` 1m32s, `go-build` 2m58s,
  `go-smoke` 31 s, `suite-go` 10m08s, the longest (`reducers` 345 s and `parse` 306 s of its
  namespaces), `oracle-go` 5m22s). Memory used (`free`, sampled each second): at most 59 GB
  in the suites' phase, as before; at most 26 GB once the converters and the Go checks run.
  Before: about 7 minutes.

arm64 (qemu) stays out of the gate (the smoke test there takes minutes, the suite hours). The
essential gate stays as it is: the Go build depends on nothing it checks, and a c2g or jrt
change is what the Go checks guard.

## Phase 2B: jrt's surface for the REPL

Part B of the split (branch `eval-2b`, 2026-10-09; three branches in all: `eval-2b-math`,
`eval-2b-time` merged into it): the jrt and JDK gaps the oracle's forms corpus and Clojure's
suite found. What jrt and the closure gained is in JRT-NOTES.md, "Phase 2B (step 5)" (Math and
StrictMath; dates; the rest); here the evaluator's share, and the results.

### The evaluator's share (`arbace/lang/go/Compiler.clj`, `RT.clj`)

- **A primitive local is boxed anew at each use** (`LocalBindingExpr.evalIn`, its primitive type
  cached): the bytecode boxes a `long` or `double` local where an `Object` is wanted
  (`Long.valueOf` with its cache, `Double.valueOf`), so `(let [x ##NaN] (identical? x x))` is
  false, as on the JVM. A fn's `^double` parameter is not a primitive local in the evaluator
  (Q1: no primitive fns), so the same test on one stays true.
- **A fn passed for an interface that is not functional is a checkcast** (`typedArgs`: the
  adaptation is left to `Reflector` only when `FISupport.maybeFIMethod` finds the interface
  functional): `(re-matches #"x" [1])` throws HotSpot's `ClassCastException` message, not
  `Class.cast`'s.
- **`:arglists` of the embedded namespaces' vars** (decided: mirror). The JVM loads core
  AOT-compiled: a def's constant metadata is rebuilt by the class's static initializer
  (`emitValue`: a seq is a `PersistentList`), where `DefExpr.eval` keeps the macro's seq (a
  `ChunkedSeq` from `defn`'s `sigs`). The JVM's own REPL and `load` of a source keep `eval`'s,
  so only the embedded namespaces (`Evaluator.EMBEDDED_LOAD`, phase 2C's flag) build a top-level
  def's constant value and metadata as the compiled class would (`Evaluator.constant`).
- **`RT.baseLoader`** is the application loader when `Compiler/LOADER` is bound to nil (the Go
  build binds no `DynamicClassLoader`): `repl/source-fn`'s `NullPointerException`. With phase
  2A's `proxy` and jrt's resources from `ARBACE_PATH` (JRT-NOTES.md), the suite's `repl` tests
  pass.

### Results

**The oracle's forms corpus** (amd64), the causes of "Failure causes (the forms corpus)" that
were part B's:

| cause | cases before | after |
|---|---:|---:|
| `arbace.math` (jrt's `Math`) | 145 | 0 |
| `Calendar`, `GregorianCalendar`, `Timestamp`, `Instant` | 37 | 0 |
| streams, sequenced collections, `UTF-16BE` | 5 | 0 |
| reducers (`ForkJoinPool`) | 4 | 0 (the corpus's `(reduce + 0 (r/map identity (range 1.0E8)))` takes 280 s of the 300 s a run may take: speed, part 4) |
| `supers`/`bases`/`ancestors` lacking `Constable`, `ConstantDesc` | 4 | 0 |
| `:arglists` | 3 | 0 |
| message formats (array cast, `#inst [2020]`'s cast) | 2 | 0 |
| identity: boxed NaN; a `StringBuilder`'s identity hash | 2 | 1 (`(hash (StringBuilder. "x"))`: the JVM records HotSpot's identity hash, a per-thread xorshift sequence; not reproducible, as decided for `#object` printing) |
| `String/join` of an `Iterable`, `.formatted` | 2 | 0 |
| `UUID/nameUUIDFromBytes` (MD5) | 1 | 0 |

With phases 2A and 2C merged (main at `29e73d6`), the forms corpus passes **10,075 of 10,077**
(the two: `deftype Foo/2`'s error source, part A's, and the identity hash); the whole oracle
**20,221 of 20,253** (forms; the class scripts 8,942 of 8,943, `(Util/equiv ##NaN ##NaN)`, the
JVM's recorded `dcmpg` bug, C2G-NOTES.md; regex 1,204 of 1,233, the 29 that need the JDK's
resource data). linux/arm64 under `qemu-aarch64`: the forms corpus the same, 10,075 of 10,077, the same two (with `--timeout 5000`: the reducers file takes about 70 minutes there).

**Clojure's suite on the Go build** (`CLOJURE_TESTS_GO`, amd64, against main's reference after
phases 2A and 2C: 1,874 assertions passing): **18,781 of 18,806 assertions pass** (588 tests;
19 failures, 6 errors), 61 of 64 namespaces load. Unblocked by part B: `api`'s neighbours
`data-structures`, `edn`, `generators`, `numbers`, `reader` (`clojure.data.generators`:
`java.util.Random`), `vectors` (`Collectors`, parallel streams), `predicates` (`URI`), `math`,
`parse`, `data-structures-interop` and `sequences` (`Math/exp` in `test.check`), `atoms`
(`IntSupplier`), `delays` (`CyclicBarrier`), `streams`, `repl` (`source`), `reducers` (no longer
skipped). Left: `clearing` (19 failures, part 4), `java.io.File` (cut by D6: 3 errors in
`method-thunks`, 2 in `reader`'s `temp-file`, 1 in `sequences`' `test-iteration`; `java.io`
does not load: `ServerSocket`, D6), `pprint` (`Semaphore`: `AbstractQueuedSynchronizer` was tried
and left out, c2g leaves its inner class `ConditionObject` untranslated while emitting the
members that name it), `api` (`arbace.java.api.Clojure`, a class of the JVM build), and
`transducers` now loads but its `seq-and-transducer` (200,000 `test.check` trials) runs out of
the 900 s a namespace may take: it is skipped in the reference with that reason (part 4). The
reference, `test/arbace-go-results.edn`, is updated.

**Checks**: `bin/gate` passes (the class forms compiler's bridge fix leaves the stages
unchanged); `bin/jrt test` on amd64 (and `--race` for the pool) and arm64; `bin/c2g-check --
--program` 9,010 of 9,010 steps (amd64).

**Costs**: the executable is 58 MB on amd64 (was 38: streams and the functional interfaces most
of it), 56 MB on arm64; `bin/c2g --program` about 55 s; `bin/jrt-convert` 340 files in about
85 s; start-up unchanged.

### Proposed amendments (for the user's review)

All accepted 2026-10-09 and folded, renamed S1-S8 (C2G-SPEC §16); B7 by keeping the
transcribed code under its license. JRT-NOTES.md's W1-W4 ("Dates") are B1, B2, B3 and B7.

- **B1 (C2G-SPEC §4.4, Collisions)** the rename table: `java/sql/Date` → `Sql_Date`,
  `java/util/stream/Tripwire` → `Stream_Tripwire`. *Accepted 2026-10-09, folded into C2G-SPEC
  §4.4 as S1.*
- **B2 (JRT-SOURCES.md, the tool)** other modules' files (`added-module-sources`, KIND
  `share:MODULE`: `java.sql.Timestamp`, `Date`); `J2C_PATCH_ALL` (each chunk sees the others'
  sources); `known-differences` (shape differences of known kinds count as converted).
  *Accepted 2026-10-09, folded into JRT-SOURCES.md, "The closure as grown", as S2.*
- **B3 (C2G-SPEC §5.3; the manifest)** `java.util.Date` and `ForkJoinTask` are non-leaf
  hand-written classes; `AtomicInteger` and `AtomicLong` embed the translated `Number` (T12
  done); `String` implements `Constable` and `ConstantDesc`. *Accepted 2026-10-09, folded into
  C2G-SPEC §5.3, §5.4 and §9.1 as S3.*
- **B4 (C2G-SPEC §11, c2g's support)** c2g writes, into jrt's package, the members of
  hand-written classes that name translated classes (`String.join(CharSequence, Iterable)`,
  `formatted`, `lines`, `describeConstable`; `Date.toInstant`, `from`;
  `Throwable.printStackTrace(PrintStream|PrintWriter)`; `System.getenv()`) and forwarders on
  hand-written leaf classes to the translated interfaces' default methods they lack (§5.4).
  *Accepted 2026-10-09, folded into C2G-SPEC §11 and §5.4 as S4.*
- **B5 (C2G-SPEC §8.2) the fork-join pool**: jrt's `ForkJoinPool` runs tasks on worker threads
  (goroutines), the claim/await protocol above, a fork without a free worker running in the
  forking thread; reducers' `fold` and parallel streams are parallel (JAVA-SURFACE.md decision
  6 had cut the pool). *Accepted 2026-10-09, folded into C2G-SPEC §8.4 as S5 (§8.2 is
  `volatile`; the pool's cut was D6's, in JAVA-SURFACE.md's "What the cut leaves out", not its
  decision 6), with B1-PLAN D6 and JAVA-SURFACE.md.*
- **B6 (JRT-NOTES V9)** jrt has six charsets (`UTF-16`, `UTF-16BE`, `UTF-16LE` added); the
  stream readers and writers keep three. *Accepted 2026-10-09, folded into C2G-SPEC §12 and V9
  as S6 (V9 is C2G-SPEC's deviation, which JRT-NOTES cites).*
- **B7 (LICENSE.md)** jrt's own Java for `Calendar`, `GregorianCalendar`, `TimeZone` and
  `TimeText` transcribes parts of jdk26u (GPL version 2 with the Classpath Exception), or those
  parts are rewritten from the documented behaviour: the user's call (JRT-NOTES.md, "Dates").
  *Accepted 2026-10-09 as S7: the transcribed parts are kept under the GPL version 2 with the
  Classpath Exception, as LICENSE.md records them (completed: `java/time/LocalDate.java` among
  the sources, and jrt's FdLibm port).*
- **B8 (the class forms compiler, classes/SPEC.md §9's bridges)** a supertype method's class type
  variables are bounded as the subclass bounds them when bridges are computed. *Accepted
  2026-10-09, folded into classes/SPEC.md §6 (the derived bridges; not §9) as
  COMPILER-NOTES.md's amendment 15, S8 in C2G-SPEC §16.*

## Phase 2B follow-up: Semaphore, Dyn's jrt interfaces, the proxy harvest

Branch `smalls` (2026-10-09), three small items left by phase 2B.

**`Semaphore`** (JRT-NOTES.md, "Semaphore"): why c2g left `AbstractQueuedSynchronizer$ConditionObject`
untranslated (jrt registers that JDK name for `ReentrantLock`'s conditions under another Go
name, and c2g named the type by the derived name: fixed in c2g, amendment Y1), and the decision
for a hand-written `Semaphore` in jrt over the translated AQS (amendment Y3).

**Dyn implements jrt's hand-written interfaces** (`arbace/c2g/dyn.clj`, `interfaces`). With
`Semaphore` the pprint tests loaded but `(future-cancel f)` threw `ClassCastException`:
`future-call` reifies `java.util.concurrent.Future`, an interface jrt hand-writes, and Dyn had
the methods of the translated interfaces only, so a reify of `Future` was not a Go `jrt.Future`
(`(instance? java.util.concurrent.Future (future 1))` was false on main). Dyn now also
implements jrt's public hand-written interfaces that jrt can cast to (a `C_Cast` function: a
slot fn's result is cast): `Future`, `ExecutorService` (with `Executor`), `Lock`, `Condition`
(jrt gains `Condition_Cast`); not `ThreadFactory`, `Member`, `InvocationHandler`, which have
none; 549 interfaces and 1,029 methods
(were 542 and 996), the executable about 200 KB larger. The Go assertion to such an interface
now succeeds for every Dyn, so jrt's `C_InstanceOf` and `C_Cast` of its hand-written interfaces
make the nominal check c2g's make for the translated ones (`jrt.dynNominal`: a Dyn's class
must implement the interface). Amendment Y2.

**The oracle's harvest takes the forms naming `proxy`** (ORACLE.md, "Exclusions"): the suite's
proxy tests are in `java_interop.clj`, a namespace the harvest leaves out whole; it now keeps
that namespace's assertions naming the proxy functions: `forms/harvest/java_interop.clj`, 3
forms after its preamble (the rest use the tests' locals or `proxy.examples`). Recorded alone
(`bin/oracle record harvest/java_interop`; the other harvest files unchanged: a full harvest
only renumbers their `fn*` argument gensyms, so they were not rewritten). Amendment Y4.

### Results

- **The pprint namespace loads** on the Go build: 58 tests, 474 assertions, 470 pass, 4 errors:
  the four `flush-underlying` tests make a proxy of `java.io.BufferedWriter`, which is not one
  of c2g's proxy superclasses (`dyn/proxy-supers`): a proxy superclass must not be a leaf
  (§5.3), and `BufferedWriter` is one in the closed world (no subclass); adding it means making
  it a non-leaf class (its values `BufferedWriter_I`), not done. **Clojure's suite** (amd64):
  646 tests, 19,280 assertions, 19,251 pass, 19 fail, 10 errors (were 588, 18,806, 18,781, 19,
  6); 62 of 64 namespaces load (were 61). `test/arbace-go-results.edn` is updated; with it,
  `bin/clojure-tests` reports no regression. (In one full run `math` was killed by the kernel's
  OOM killer while other agents' builds ran; alone it passes as recorded.)
- **The oracle**: `bin/oracle check jvm` 20,263 of 20,263. The Go build (amd64): 20,230 of
  20,263 (was 20,221 of 20,253): the 10 new cases, 9 matching; the new mismatch serializes a
  proxy (`java.io.ObjectOutputStream`, D6). The other 32 are unchanged.
- **Checks**: `bin/gate` passes; `bin/jrt test` on amd64 (and `--race` for the locks and
  `Semaphore`) and arm64.

### The regex cases that need the JDK's data (not done)

29 of the oracle's regex cases fail on the Go build for want of JDK resource data:

- **5 cases, `\N{name}`** (`basics.clj` 45-47, `errors.clj` 108-109): `Character.codePointOf`
  reads `java/lang/uniName.dat` (177 KB, zlib-compressed) through
  `java.util.zip.InflaterInputStream`, which is not in the closed world (its `Inflater` is
  native zlib). Needed: the file embedded among the executable's resources (they hold text
  only: `embed/sources`), and a Go-build variant of `CharacterName`'s constructor reading it
  through a native inflate over Go's `compress/zlib` (as `UUID.md5` is native). About a day's
  small work; it also gives the REPL `Character/getName` and `Character/codePointOf`. Worth it.
- **24 cases, `CANON_EQ`** (`unicode.clj` 161-195): `Pattern` normalizes with
  `java.text.Normalizer` (NFD), not in the world. Tried: with `java/text/Normalizer` and
  `jdk/internal/icu/text/NormalizerBase` in the closure, c2g finds the ICU loader
  (`ICUBinary`, `NormalizerImpl.load`, `CodePointTrie.fromBinary`) needing `java.nio.ByteBuffer`,
  `IntBuffer` and `Buffer` (the JDK generates them from templates; none is in the world),
  `UCharacterIterator`, `UTF16`, `UCharacterProperty`, `java.text.CharacterIterator`, and
  `Normalizer`'s Go name collides with another class (a rename entry); then `nfc.nrm` (36 KB)
  embedded as a resource. java.nio's buffers are the large part. Alternatives: a Go-build
  variant of `NormalizerImpl.load` over a `byte[]`, or a hand-written NFD over Go's tables
  (Go's std has none exported: `golang.org/x/text/unicode/norm` is vendored inside std, and
  its Unicode version, 15.0, is older than JDK 26's). Several days for one flag Clojure code
  rarely uses: not worth it now; to report to the user.

### Proposed amendments (for the user's review)

- **Y1 (C2G-SPEC §4.4, Names)** c2g names a class jrt provides, and does not translate, by the
  Go name jrt registers for it (the class variable's name less `_class`), which may differ from
  the name derived from the Java name (`ReentrantLock_ConditionObject` for
  `AbstractQueuedSynchronizer$ConditionObject`).
- **Y2 (C2G-SPEC §5.12, Dynamic objects)** Dyn implements, besides the translated public
  interfaces, jrt's public hand-written interfaces that have a cast function; jrt's instance
  checks and casts of its hand-written interfaces make the nominal check (`jrt.dynNominal`).
- **Y3 (C2G-SPEC §8.4; JAVA-SURFACE.md decision 4)** `java.util.concurrent.Semaphore` is
  hand-written in jrt, as the locks are, not `AbstractQueuedSynchronizer` translated (which needs
  `Unsafe` additions and has a two-word race on `Node.waiter`); fairness is not kept.
- **Y4 (ORACLE.md, Exclusions)** the harvest keeps the assertions of `java_interop.clj` that
  name the proxy functions (the suite's proxy tests), the rest of that namespace staying out.

Accepted by the user (2026-10-09) and folded: Y1 C2G-SPEC §4.4, Y2 §5.12, Y3 §8.4 and
JAVA-SURFACE.md decision 4, Y4 ORACLE.md (Exclusions); listed in C2G-SPEC §16.

## Proxies of BufferedWriter: pprint passes

Branch `pprint-bw` (2026-10-09). The last 4 errors of the suite's `pprint` namespace (the
`flush-underlying` tests) made a proxy of `java.io.BufferedWriter` that counts its flushes
(`(proxy [java.io.BufferedWriter] [o] (flush [] (proxy-super flush) (swap! n inc)))`), and
`BufferedWriter` was not one of c2g's proxy superclasses: a class of `proxy-supers` had to be
non-leaf already, and `BufferedWriter` is a leaf of the closed world (nothing extends it).

**The general way: a class of `proxy-supers` is not a leaf.** Its `DynSub_C` extends it, so it has
a subclass in the program, and leafness (§5.3) now counts it: `model/leaf?` answers false for a
translated, non-final class of the list, which moves from `arbace.c2g.dyn` to `arbace.c2g.model`
(`m/proxy-supers`; `dyn/proxy-supers` refers to it) and gains `java/io/BufferedWriter`. A listed
class then gets its class interface `C_I`, its values are `C_I` wherever the static type is `C`,
its methods split into `Impl_` and dispatch methods, and c2g writes `DynSub_C` over it with no
other change: the rule a class must be non-leaf to be listed becomes a consequence of being
listed. (A hand-written class stays as jrt declares it: `ThreadLocal` is non-leaf in jrt.) For
`BufferedWriter` the change touches its own file, `PrintStream` (which holds one) and the class
table; `DynSub_BufferedWriter` has 1,039 methods, as the other `DynSub_C`.

**Behaviour as on the JVM.** The oracle's `forms/types.clj` gains 13 forms (appended, so the
other cases keep their lines): a `BufferedWriter` proxy whose `flush` calls `proxy-super` and
counts, buffering (nothing reaches the underlying writer before a flush), `newLine` and
`write(String, int, int)`, `instance?` and the superclass, `prn` with `*flush-on-newline*` true
and nil (flushes counted as the JVM counts them), `pprint` (the pretty writer over it) and
`cl-format` with a case directive through it, a proxy overriding `write` that calls
`proxy-super` with another string (the buffer size given to the constructor), and a write after
`close` (`IOException` "Stream closed"). All 438 cases of the file match on the Go build.

### Results

- **Clojure's suite** (amd64): `pprint` 58 tests, 474 of 474 (was 470, 4 errors). The whole
  suite: 646 tests, 19,280 assertions, 19,255 pass, 19 fail, 6 errors (were 19,251, 19, 10);
  58 namespaces pass as on the JVM. `test/arbace-go-results.edn` updated; no regression.
- **Executable size** (amd64, built from the same tree with and without the change): 58,603,950
  to 59,584,911 bytes, +981 KB (+1.7%): `DynSub_BufferedWriter` and `BufferedWriter`'s split.
- **The oracle**: `bin/oracle check jvm` 20,276 of 20,276. The Go build (amd64): 20,243 of
  20,276 (the 13 new cases match); the 33 mismatches are those `known-go-amd64.edn` records.
- **Checks**: `bin/c2g-check -- --program` 9,010 of 9,010 steps on amd64 and on arm64; `bin/jrt test` (amd64) passes.
  The change is in c2g only (not compiled by the bootstrap), so `bin/gate` was not needed.

### Proposed amendments (for the user's review)

- **Z1 (C2G-SPEC §5.3, §5.12; accepted by the user 2026-10-10, folded there and in §16)** A translated, non-final class of `proxy-supers` is not a leaf:
  its `DynSub_C` is a subclass in the program, so leafness counts it (it was a condition on the
  list, now a consequence of it; X1's "leafness stays a property of the closed world" reads: of
  the closed world and the proxy types c2g adds to it). The list moves to
  `arbace.c2g.model/proxy-supers` and gains `java.io.BufferedWriter` (pprint's tests); §5.12's
  "a proxy of a leaf class throws (`BufferedWriter` ...)" keeps `BitSet` as its example.

## Sources

Nothing vendored. Studied: upstream Clojure's `Compiler.java` as Arbace's `arbace/lang/Compiler.clj`
holds it (the emit methods each evaluation mirrors), `clojure.main`'s error triage
(`arbace/main.clj`); openjdk/jdk26u `src/java.base/share/classes/java/util/GregorianCalendar.java`
(the default cutover) and HotSpot's `AbstractMethodError` and checkcast messages (from the JVM);
H. Hinnant, "chrono-Compatible Low-Level Date Algorithms" (civil-from-days, days-from-civil).
