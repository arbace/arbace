# c2g, phase 1: the core

B1a step 4 ([B1-PLAN.md](B1-PLAN.md)), first phase: **c2g**, the translator of class forms into
Go forms ([C2G-SPEC.md](C2G-SPEC.md)), printed by `arbace.g2c.print` and built with
`bin/g2c build` against jrt ([JRT-NOTES.md](JRT-NOTES.md)). Phase 1 is the translator itself
(types and objects, all code forms, concurrency, the generated tables, the manifest boundary),
its differential harness `bin/c2g-check`, and the end-to-end proof: the oracle's class scripts
and c2g's own fixtures, translated, built for linux/amd64 and linux/arm64, run, and compared
with the JVM's results.

State (2026-10-08, branch `c2g-core`): c2g translates every class of `arbace/lang` that the
oracle's class scripts and the fixtures reach, with the JDK closure j2c converted
(`.tmp/jrt/conv`, `bin/jrt-convert`), and replaces 38 of jrt's 64 stand-ins (all those whose
classes have sources in the closure). The programs build for both architectures; the results
per script are in "End-to-end results" below.

## Structure

`arbace/c2g/` (namespaces `arbace.c2g.*`, run on Arbace's `target/stage2` by `bin/c2g`;
exempt from tools.bash's staleness warning like g2c):

| namespace | what |
|---|---|
| `names` | §4.2, §4.4: Go packages of classes, munging, locals, class names (with the rename table), descriptor codes, mangled member names |
| `jrt` | a scan of jrt's hand-written forms (`go/arbace/jrt/*.clj`, tests excluded): types, methods per receiver (embedded ancestors included), functions, variables, constants, interfaces, and the classes jrt registers (`Define`), stand-ins marked |
| `world` | §4.1: reads the inputs (Arbace's `arbace/lang/*.clj`, the JDK closure, `--input` trees), applies the Go-build variants (§4.6, `arbace/lang/go/*.clj`), enters everything into one class environment and runs the analyzer exactly as the class forms compiler does (`declare-class!`, `resolve-header!`, `resolve-members!`, `add-bridges!`, `analyze-class!`), with `env/*from-source*` so that no class with source is resolved by reflection; failures are recorded per class |
| `model` | the closed world as c2g sees it: translated / hand-written / stand-in / reflected classes, leaves, Go types (§5.1), virtual methods and their implementations, trivial class initialization |
| `access` | javac-style `access$NNN` accessors decoded back into the member access they wrap |
| `reach` | reachability (rapid type analysis): roots, instantiated classes, virtual calls resolved over the instantiated subtypes, lambdas' functional interfaces, static initializers, slices; the "missing parts" (operations naming what the world lacks) |
| `code` | §7: expressions, statements, contexts (statement, assign, return, jump), temporaries and the order of evaluation, arithmetic, conversions, strings, calls, switch, patterns, loops and labels, try/catch/finally with control codes, monitors, lambdas and method references |
| `decls` | §5, §6: struct types, class interfaces and markers, constructors (allocation function and body), methods, forwarders, the Object protocol, dispatch, statics and class initialization, derived members (enum `values`/`valueOf`, bridges, record accessors and `toString`/`hashCode`/`equals`) |
| `out` | the file printer, generated per-package files (strings, conversions `Up_`, adapters `F_Fn`, support helpers, cut classes, member tables, `ClassInfo` registration with `RegisterGoType`, frame table) |
| `main` | the command: options, the world, the reachability fixpoint with stand-ins as roots, translation, the program tree |

`bin/c2g [--lang] [--jdk DIR] [--input DIR]... [--slice REGEX]... [--root C#m]... [--tests]
[--main DIR]... --out DIR` writes the Go forms (`DIR/gen`), a program root `DIR/prog` (jrt's
hand-written forms with the replaced stand-ins filtered out, the generated files merged in,
`program.edn`) and `DIR/report.edn` (classes, reached methods, missing parts, stand-ins
replaced, analysis failures, variants, times). `bin/g2c build DIR/prog` (or `bin/jrt` with the
overlay) builds it.

`bin/c2g-check [SCRIPT...|fixtures] [-- C2G-OPTIONS]` is the differential harness (below).

## Decisions where the spec was silent

- **Slices restrict code, not classes.** `--slice REGEX` limits which classes' method bodies
  are translated; classes outside a slice still exist in Go, with stubs that throw
  `UnsupportedOperationException("c2g: not translated: ...")`. Dropping classes from the world
  instead broke the analyzer (reflection on the running JDK fills in what source should).
- **Operation-level stubs.** An operation that names something outside the closed world (a
  class, member or descriptor jrt does not have) becomes `C2g_Missing[T](why)`, typed by the
  wanted Go type, which throws `UnsupportedOperationException("c2g: ...")`; the rest of the
  method is translated. `report.edn` lists each method with missing parts and why.
- **Stand-ins.** A jrt stand-in (J9) whose class has class forms in the world is replaced: its
  file is filtered (whole file dropped when all its classes are replaced, else the stand-in's
  forms removed, groups and helpers included), and its static initializer and members are
  roots. Replaced stand-ins with a non-trivial `<clinit>` are initialized at Go package
  initialization (`c2g_init.go`), because jrt's hand-written code reads their statics as the
  stand-ins' package variables (proposed amendment A3).
- **Cut classes.** A class literal of a class outside the world (e.g. `Thread.State` in a
  switch map) is a class registered by name in `c2g_cut.go`, with no instances; `instanceof`
  of such a class is false (`C2g_Discard`).
- **Reflected marker interfaces.** A JDK interface without class forms and without methods
  (`RandomAccess`, `Serializable`-like markers) that translated classes implement is declared
  from reflection (markers and a class, no code), so `instanceof` works. Interfaces with
  methods are not declared that way (an attempt let `IntStream` leak in).
- **Indexed static initializers in variants.** A variant replaces or cuts the n-th
  `static-initializer` of a class: `(c2g/cut (static-initializer 0))`, or a replacement tagged
  `^{:c2g/nth 1}` (proposed amendment A1). `Compiler` and `RT` have variants
  (`arbace/lang/go/`): `RT`'s initializer does not load `arbace.core`, its streams are unbound
  vars; `Compiler`'s `ARG_TYPES` initializer is cut and `JVM_BYTECODE_VERSION` fixed.
- **`String.format`** is c2g's: when `java.util.Formatter` is translated, `c2g_support.go`
  defines `String_Format_String_O1__String` (and the `Locale` overload) over it, unless jrt has
  one.
- **Names.** The rename table holds `jdk/internal/util/ByteArray` → `Jdk_ByteArray` (jrt's
  `ByteArray` is `byte[]`). Locals get `_` appended when they would be Go keywords, predeclared
  names, c2g's reserved names (`t`, `this`, `rv`, `ctl`, `exc`, package names), or start with an
  upper-case letter, `c2g_`, `tmpN` or `LN`.
- **Evaluation order.** Go orders calls among themselves but not reads of variables, fields and
  array elements relative to calls. An operand that reads state is moved to a temporary when a
  later operand has effects; and when an operand is moved, every earlier operand that is not
  stable is moved too, so that the temporaries keep Java's left-to-right order (the fixture
  `tLazySeq` caught the second rule missing).
- **Typed `null` constants and `C2g_*` helpers** (`c2g_support.go` in jrt): `C2g_Missing`,
  `C2g_NotTranslated`, `C2g_NotNull`, `C2g_Truth` (a `Boolean`/`Object` condition),
  `C2g_CastArray`, `C2g_Discard`, `C2g_RefP`, `C2g_ObjectClone`, and for records
  `C2g_ObjHash`, `C2g_ObjEquals`, `C2g_DoubleBits`, `C2g_FloatBits`.
- **Records** get `toString`, `hashCode` and `equals` as `java.lang.runtime.ObjectMethods`
  makes them (`Name[c=v, ...]`, `31 * h + hash(c)`, same class and equal components).
- **Record patterns** are matched with a flag: the pattern's bindings are declared first, the
  tests and accessor calls nest, the innermost sets the flag, and the `then` arm runs when it
  is set. An accessor's exception is not wrapped in `MatchException` (phase 2).
- **A switch without default in return position** returns the zero value (Java: the switch
  completes normally and the method's `return` after it is what the analyzer typed).
- **Adapters** (`F_Fn`) are generated once per functional interface, in its package, however
  many packages use it.

## Coverage of C2G-SPEC §14

§14.1 (declarations): classes, interfaces (class interfaces, markers, forwarders, default
methods as package functions), enums (constants, constant bodies as subclasses, `values`,
`valueOf`, `$values`, ordinal switches), records (fields, canonical and compact constructors,
accessors, `toString`/`hashCode`/`equals`), member, inner, local and anonymous classes (outer
instance `this$0`, captured `val$x` fields), `static`, `final` (leaves), `abstract`,
`synchronized` (monitor enter, deferred exit), `native` (jrt's `_native` functions),
`volatile` (atomic field types), initializers folded by the analyzer, bridges (when reached and
their target is in the world). Annotations, `transient`, sealing: nothing, as specified.

§14.2 and §14.3 (code): every analyzer node kind is translated except `:fi-adapter` (Clojure's
`fn` as a functional interface: an operation-level stub, "not translated yet") and the
serialization nodes `:deser-indy`/`:param0` (out of scope, §12). In particular: Java's
arithmetic with Go's wrapping, the FMA rule (`conv float64` around products), shift masks,
`>>>` with constant folding, `Math.*Exact`; conversions (NaN and saturation in `D2I`/`D2L`);
boxing/unboxing with nil preservation; `java-str` as `jrt.Concat` over the `StrOf*` functions;
`switch` on ints, strings (hash then equals, two steps), enums (ordinals) and patterns (chains,
guards, `null` labels, record patterns); labels, `break`/`continue` with values, loops,
`for-each` over arrays and `Iterable`s; `try`/`catch`/`finally` as function literals with
control codes for jumps out; multi-catch; `with-resources` (suppressed exceptions);
`locking`; `java-assert`; lambdas and method references (static, bound, unbound,
constructor), adapters with the functional interface's default methods; casts, `instanceof`,
arrays (multi-dimensional, stores, clone); Clojure inside class bodies (`:var-deref`,
`:var-invoke` through `Var`).

§14.4: none of the class file attributes exist; member tables (`Modifiers`), Go constants for
`ConstantValue`, `ClassInfo` (`Declaring`, `Kind`) as specified.

The fixtures (`test/c2g/fixtures/`) exercise these forms: `FxCode` (arithmetic,
conversions, compound assignment, labels, loops, `for-each`, `yield`, the four kinds of
switch, `finally` overriding `return`, nested try with multi-catch, jumps through `finally`,
causes, an uncaught exception, `with-resources`, nested `locking`, strings, arrays and their
exceptions, casts, varargs) and `FxCode`/`FxClasses`'s classes (static initialization order
across a hierarchy with constant fields that do not initialize, inheritance with `super`
calls and a virtual call from a superclass constructor, interface default and static methods,
inner/nested/anonymous/local classes, volatile fields, records and record patterns, enums
with constant bodies, lambdas and method references, default methods of a functional
interface, §15.4's `Delay` and `LazySeq` over an anonymous `AFn`, §15.5's lambda with an enum
switch).

## The harness

`bin/c2g-check` translates what the selected scripts call (each recorded step's member is a
root), writes one Go `main` package per script (`prog-<script>`) that runs every step with
the translated members by their Go names (direct calls, no reflection), builds it with
`bin/g2c build` for each architecture in `C2G_ARCH` (default `amd64,arm64`; arm64 runs under
`qemu-aarch64`), runs it and compares each step with the JVM's record in
`test/oracle/expected/classes/<script>.edn`. Steps print `@@c2g i W|N|U|T|V|S|P|X ...`;
results are normalized as the oracle runner does. A step whose member is a stub, outside the
slice, or uses a binding an unavailable step should have made is **unavailable**; a step whose
expected result is printed (`:pr`) needs `RT.printString` translated and reached, else it is
counted apart. The JVM's helpful `NullPointerException` messages are accepted as a null
message (V11). A Go panic that is no Java exception ends its step, not the program.

`fixtures` (in the script list, and by default) loads `test/c2g/fixtures/*.clj` into this JVM
(`load-file`), runs every public static method named `t[A-Z]...` without parameters, records
the results the same way, adds the fixtures as an `--input` and compares likewise.

## End-to-end results

Run: `bin/c2g-check -- --slice '^arbace/lang/' --slice '^java/' --slice '^jdk/'` (every script
and the fixtures; one translation of 739 classes, 6153 methods reached; 19 programs, each
built and run for linux/amd64 and, under `qemu-aarch64`, linux/arm64). **Both architectures
give identical results**, step for step:

| script | steps | pass | fail | unavailable |
|---|---|---|---|---|
| Murmur3 | 189 | 189 | 0 | 0 |
| Names | 191 | 191 | 0 | 0 |
| Numbers | 640 | 640 | 0 | 0 |
| NumbersCompare | 784 | 748 | 36 | 0 |
| PersistentArrayMap | 146 | 146 | 0 | 0 |
| PersistentHashMap | 280 | 279 | 1 | 0 |
| PersistentList | 129 | 129 | 0 | 0 |
| PersistentTreeMap | 213 | 201 | 6 | 6 |
| PersistentVector | 269 | 269 | 0 | 0 |
| RatioBigInt | 131 | 131 | 0 | 0 |
| Seqs | 210 | 210 | 0 | 0 |
| Sets | 142 | 138 | 0 | 4 |
| Strings | 417 | 417 | 0 | 0 |
| UtilRT | 614 | 602 | 12 | 0 |
| LispReader | 264 | 239 | 25 | 0 |
| Printer | 211 | 194 | 17 | 0 |
| VarNamespace | 139 | 133 | 6 | 0 |
| fixtures FxCode | 26 | 26 | 0 | 0 |
| fixtures FxOuter | 10 | 10 | 0 | 0 |
| **total** | **5005** | **4892** | **103** | **10** |

The 103 failures, by cause (none is a translation error known to us):

- 37: the JVM Arbace's NaN comparison bug ("Found on the way"): the recorded results are
  wrong, c2g's are Java's (`NumbersCompare` 36, `PersistentTreeMap` 1).
- 54: `arbace.core` is not loaded (`RT`'s variant): printing through `print-method`
  (`##Inf`, `BIGINT` suffixes, namespaced maps, Java collections, classes and vars;
  `Printer` 17, `PersistentHashMap` 1, `PersistentTreeMap` 5), the reader's data readers,
  `*ns*` (`user`) and reader conditionals' printing (`LispReader` 25), `arbace.core`'s vars
  (`VarNamespace` 6). Phase 2's evaluator boundary.
- 12: `Reflector`'s static initializer needs `MethodHandles.Lookup`, outside the world
  (`UtilRT`: `RT.get`/`nth` on Java arrays): it needs a variant.

The 10 unavailable steps use `String.CASE_INSENSITIVE_ORDER`, which jrt lacks.

**All of `arbace.lang`.** With every top-level class of `arbace/lang` as a root (all their
public members): 811 classes in Go, 441 of them `arbace.lang`'s, 6993 methods reached, 341
with missing parts. The 333 classes of `arbace/lang` not in Go are 322 primitive-signature
interfaces nested in `IFn` (nothing uses them), `DynamicClassLoader` (a `URLClassLoader`),
`ReflectorCallSite` (method handles), `XMLHandler` (SAX) and 8 nested classes of these and of
`Compiler`, `Var`, `LispReader`, `LockingTransaction`, `KeywordInvokeSite` that nothing reaches.
The packages `arbace/lang` and `arbace/jrt` build for both architectures.

**The JDK slice and jrt's stand-ins** (`bin/c2g --jdk .tmp/jrt/conv --slice '^java/' --slice
'^jdk/' --tests`): 134 classes translated; 38 of jrt's 64 stand-ins are replaced (every one
whose class has class forms in the closure; the other 26 have no sources there). jrt's own Go
tests, run on that program (the stand-ins replaced), pass on amd64 and on arm64 under qemu:
53 of 54; `TestReflectAgainstJVM` fails because its expected member lists are filtered by
jrt's hand-written tables, which the translated classes now extend (inherited bridges such as
`append(C)Appendable` on `StringBuilder`): phase 2 regenerates them.

## Measurements

On this machine (64 cores), cold JVM, `-Xmx12g`:

| | |
|---|---|
| loading and analyzing the world (Arbace's 142 files, the JDK closure's 184; 1392 classes) | 6.5 s |
| reachability (739 classes, 6153 methods; all of `arbace.lang`: 811, 6993) | 12-16 s |
| translation (739 classes / 811 classes) | 21-22 s |
| writing the forms | 1.3-3 s |
| `bin/c2g` end to end, all of `arbace.lang` | 48 s wall |
| Go lines of all of `arbace.lang` plus jrt (printed by g2c) | 248,000 |
| `go build` of the packages, cold cache | 25.7 s amd64, 21.4 s arm64 |
| `bin/g2c build` of one check program, warm cache (cold: first program) | 10 s (32 s) |
| a check program's executable (static, not stripped) | 26.5-26.8 MB amd64, 25.1-25.5 MB arm64 |
| `bin/c2g-check`, all scripts and fixtures, both architectures | 8 min |
| `bin/c2g`, the JDK slice alone (134 classes) | 12 s wall |

The translation is single-threaded; nothing has been tuned yet.

## Found on the way

- **The JVM Arbace compares with NaN wrongly in some branches.** `arbace/classes/emit.clj`'s
  `emit-cond` picks `dcmpg`/`dcmpl` from the comparison before negating it for the jump:
  `(< 1.0 ##NaN)` is true on the JVM Arbace. The fix is to choose `g?` from the comparison as
  jumped on: `g? (case (if jump-if c (negate-cmp c)) (:< :<=) true (:> :>=) false (:== :!=)
  true)`. The oracle's `NumbersCompare` script recorded the wrong results (the 36 NaN steps
  c2g "fails"); after the fix they must be re-recorded (`bin/oracle record classes`). Not fixed
  here (outside c2g's scope).
- **The JDK closure's ICU normalizer does not analyze**: `NormalizerImpl` cannot resolve
  `CodePointTrie$Fast16` (a nested class referred to across files), which fails
  `Norm2AllModes` and `CodePointTrie` too (8 classes). Nothing reached needs them yet.
- **jrt gaps filled here** (`go/arbace/jrt/`): `VM.getSavedProperty`, `VM.isBooted`, `CDS`
  (`vm.clj`); `NullPointerException.getExtendedNPEMessage` native (null, J6; `natives.clj`);
  `ForkJoinTask` and `ForkJoinPool` (a task runs at `join`; `forkjoin.clj`);
  `SharedSecrets.getJavaLangAccess` with a `JavaLangAccess` (`access.clj`).

## What phase 2 must do

Work that can proceed in parallel, each part with its own check:

1. **The evaluator boundary (§10)**: `RT`'s initialization with `arbace.core` loaded (the
   printer, `print-method`, data readers `#inst`/`#uuid`, `*ns*`), `Compiler` as a whole, and
   `Reflector` (needs a variant: no `MethodHandles`). This is what most of the remaining
   oracle failures (`Printer`, `LispReader`, `VarNamespace`, `UtilRT`) wait for.
2. **Streams and writers in jrt**: `*out*`/`*err*` bound to `PrintWriter`s over the host's
   file descriptors (`RT`'s variant leaves them unbound now), `StringBuilder` gaps the
   `Strings` script shows.
3. **The rest of the JDK closure**: 26 stand-ins have no sources in the closure (`bin/jrt-convert`
   must add them or they stay hand-written); the ICU normalizer's analysis failure; the
   BigInteger/BigDecimal paths `RatioBigInt` and the printer need.
4. **jrt's reflection test data**: `TestReflectAgainstJVM` filters its expectations by jrt's
   own tables; with c2g's member tables replacing the stand-ins' they must be regenerated from
   the translated classes.
5. **The JVM NaN fix** above, then re-record `NumbersCompare`.
6. **Code quality and the rest of §7**: frame table with line positions (§7.9.6; today only
   names), `MatchException` from record accessors, the guard-chain duplication of pattern
   switches (an arm's fall-through chain is repeated under each guard: code size), `:fi-adapter`
   (`FromFn` adapters), `Dyn` (§5.12), the package-private collision check across packages
   (§4.4), race reporting (§8.3), performance work (§13: inline caches, fewer `NN` checks).
7. **Wider closure, whole program**: translate without slices (`bin/c2g` with no `--slice`,
   all of `arbace/lang` reached from `Main`), and a `--main` program that starts `RT`.

## Phase 2D: quality and the whole program

Branch `c2g-2d` (2026-10-09): items 6 and 7 of "What phase 2 must do", less `:fi-adapter` and
`Dyn` (phase 2B). Changed namespaces: `arbace.c2g.code` (`*line*`, `with-line`, `emit!`,
`decl!`, `fold-items`, `stmt!`, `translate-ctx`, `note-store!`, `*races*`,
`derived-accessor`, `accessor-call!`, `match-pattern!`, the `:pattern` case of
`translate-switch`, `translate-monitor`, `lambda-val`), `arbace.c2g.decls` (`member-line`,
`at-line`, `method-code-forms`, `ctor-forms`, `init-forms`, `method-forms`,
`touches-statics?`, `needs-entry-guard?`, `terminating?`, `ensure-terminated`),
`arbace.c2g.out` (the layout: `*lay*`, `out!`, `write-meta`, `write-list`, `write-form`,
`form-text`, `file-text`), `arbace.c2g.model` (`benign-init?`, `pure-node?`, `pure-ctor?`),
`arbace.c2g.world` (`:source-of`), `arbace.c2g.main` (`--program`, `source-top`, the
checks), the new `arbace.c2g.checks`; outside c2g: `arbace.classes.analyze/analyze` (nodes
keep their form's `:line`) and `arbace.classes.parse/parse-member` (members keep `:line`),
neither changing any emitted byte (stages 2 and 3 identical, the gate passes); jrt:
`asObject` (`object.clj`), `Arraycopy` (`array.clj`), `isLiteralOf` (`throwable.clj`), the
stand-in `MatchException` (`test/jrt/standins.clj`, `standin_lang.clj` regenerated).

**1. Line positions.** The analyzer's nodes carry the line of their form, members the line of
their member form. c2g binds the line of the node it translates and puts it, as
`:c2g/line`, on every statement it emits and on each method's declaration. `out` turns these
into the forms' positions: a form whose line the text has not passed is written on that line
(the reader's position is then the class forms line), else it gets an explicit `^{:line n}`,
and so does every list inside it without a line of its own (the reader keeps an explicit
`:line`). Package-level declarations are written sorted by line (Go does not care about their
order), so explicit lines are rare. In `arbace/lang` the Go file is named after the class
forms file (`FxClasses.go` for the fixtures' many classes), so that `bin/g2c build
--line-file` makes gc's line tables, Go's tracebacks, pprof and jrt's `StackTraceElement`s
name the class forms file and line:

```
Exception in thread "main" java.lang.MatchException: java.lang.IllegalStateException: negative
	at c2g.fixtures.FxOuter.classify(FxClasses.clj:144)
	...
Caused by: java.lang.IllegalStateException: negative
	at c2g.fixtures.FxTouchy.v(FxClasses.clj:69)
```

`bin/c2g-check` now builds with `--line-file`. Costs: the forms grow 11% (8.1 to 9.0 MB),
a check program's warm build 11.6 to 12.8 s on average, the executables shrink 4% (25.95 to
24.87 MB). jrt's frames: a function literal nested in a literal (`f.func1.1`) now stands for
its function as `f.func1` does. Left: jrt's classes are named by their Go file
(`java_util_HashMap.clj`, not `HashMap.java`); a member a variant replaces shows the
variant file's line in its class's file.

**2. Pattern switches and `MatchException`.** A pattern `switch` is now an `if` chain in a
labeled block (`(label :L (switch (default ...)))`): an arm whose pattern and guard match
runs its body and leaves the block; a failed guard falls through to the next arm. Before,
each guarded arm repeated the translation of all later arms under its guard, growing
exponentially with the guards. A record component is read from the field when the accessor
is the derived one (records are final, so it cannot be overridden or throw); a declared
accessor is called in a function literal and its exception thrown as
`MatchException(t.toString(), t)`, as javac does. `MatchException` is a jrt stand-in (`:st`)
until the closure has its source. Fixtures `tPatternGuards` and `tMatchException` (record
`FxTouchy` with a throwing accessor) pass on both architectures.

**3. Names.** `arbace.c2g.checks` checks, after translation, every Go package's
package-level names and every type's method names across the generated files and jrt's
hand-written ones (stand-in files excepted), and the package-private methods of translated
classes redeclared by a subclass in another Java package (JVMS 5.4.5; the Go package does not
matter, `java.util` and `java.util.concurrent` share jrt). Both are errors (c2g exits 1) and
are listed in `report.edn` (`:errors`, `:package-private-overrides`). Found: none in the
world of the check, of the whole program, or of all of `arbace.lang`. The rename table stays
one entry (class names); member renames are not needed yet.

**4. Races.** c2g records every store into a candidate two-word field (§8.3: non-final,
non-volatile, interface Go type, outside its class's constructors or static initializer, not
in a `synchronized` method or `locking` on `this`; lambdas reset the context) and lists them
in `report.edn` `:race-candidates`, by class, with the writing methods: 67 fields in 36
classes for the check's world (the JDK's collections' caches and iterators, `Compiler`'s
expression builders, `LazySeq`/`Delay` fields written under a `ReentrantLock`). `bin/c2g-race`
builds a program of threads sharing translated objects as Clojure's runtime shares them
(vector and map hash caches, `Symbol`/`Keyword` interning, `Namespace.findOrCreate`, an
`Atom`'s compare-and-set, `RT.nextID`; `test/c2g/race`) with `-race` (cgo on; it works on
Alpine's musl) and summarizes the reports: 4, all on the `int32` hash caches `_hasheq` and
`_hash` of `APersistentVector`/`APersistentMap`, one-word races Java allows; no race on a
two-word field; the atom ends at 16,000 as expected. A `bin/c2g-check` program (`Seqs`) runs
clean under `-race` (`bin/c2g-race .tmp/c2g/check/prog-Seqs`), and so do jrt's own tests
(`bin/jrt test --race`, with the changes here).

**5. Performance.** `bin/c2g-perf` runs the workloads of `test/c2g/bench/BnWork.clj` (class
forms over `arbace.lang` only) on the JVM Arbace and translated to Go (linux/amd64, `GOGC`
default), and checks that both give the same checksums (they do). ns per element, this
machine shared with three other agents (±20% between runs):

| workload | JVM | Go | Go/JVM |
|---|---:|---:|---:|
| `vecConj` (`cons` 100k, `nth`) | 92 | 804 | 8.7 |
| `hashMapAssoc` (100k) | 420 | 6,221 | 14.8 |
| `arrayMapSmall` (8 keyword assocs) | 208 | 4,179 | 20 |
| `hashing` (`Murmur3`, `hasheq`) | 24 | 93 | 3.8 |
| `numbers` (boxed `Numbers.add`, `multiply`) | 6.0 | 320 | 53 |
| `seqWalk` (`PersistentList` `first`/`next`) | 11 | 140 | 12.5 |
| `vecEquiv` (1000 elements) | 17,057 | 106,872 | 6.3 |
| `strings` (`StringBuilder`) | 49 | 265 | 5.4 |

Profiles (`C2G_PROF=FILE` on the program; pprof shows class forms lines now) put most of the
difference outside c2g's code: allocation and the collector (`mallocgc`, the mark workers at
37% of all CPU with `GOGC=100`; the JVM allocates from a TLAB and its escape analysis removes
the boxes of `numbers`), arrays (`jrt.NewRefArray` is two allocations, a struct and a slice of
16-byte `any` slots: an `Object[32]` is 512 bytes against the JVM's 144 with compressed
oops), `any` → `Object_I` assertions behind `getClass`/`hashCode`/`equals` on `Object`
values, and no inlining across interface calls. `GOGC=400` alone gains 20-40%
(`hashMapAssoc` 3,975 to 2,899).

What c2g's code did cost, measured on Go benchmarks of the translated functions (`go test
-bench`, before/after, 3 runs each, ns/op): the class initialization guard at the entry of
static methods, which keeps small methods like `Integer.rotateLeft` from being inlined
(`Murmur3.hashLong` 9.4 → 3.7, `hashUnencodedChars` 36.5 → 26.3). A static method now has
no guard when its class's initialization is benign (`model/benign-init?`: the static
initializer only stores constants, arrays and objects of its own file built by field-storing
constructors into its own statics, over a benign superclass; no assertions, no Clojure
constants) and the method reads no static of its class or superclasses: running the
initialization later, at the first guarded access, cannot be observed. `Numbers` is not
benign (`new BigDecimalOps()` initializes `RT`), `Integer`, `Long`, `Murmur3`-like classes
are. With it, `ensure-terminated` recognizes terminating `if`/`let`/`do` (fewer
`panic("c2g: unreachable")`), and in jrt `asObject` became inlinable (`GetClass` 5.7 → 4.7,
`Numbers.category` 8.8 → 7.7, `Util.equiv` 53.9 → 50.1) and `Arraycopy` checks reference
arrays without element checks first. The workloads move within the noise except `hashing`
(-25%). The remaining temporaries, `NN` checks and `Up_` conversions are inlined by gc and
did not show in the profiles.

**6. The whole program.** `bin/c2g --program` (no slice): `arbace.lang.Main#main` is a root
and c2g writes the main package `arbace/cmd/arbace` (`DIR/prog/go/arbace/cmd/arbace.clj`),
whose `main` passes the command line as a `String[]` to `Main.main` inside `jrt.RunMain`:
`RT`'s initialization runs, then `RT.doInit` stops at `arbace.core/refer`, unbound until the
evaluator (phase 2B) loads `arbace.core`; the uncaught exception is printed as the JVM prints
it, with lines, and the status is 1. Measured (the machine loaded; the first numbers from a
quieter moment):

| | |
|---|---|
| `bin/c2g --program` | 668 classes, 5,022 methods reached, 329 with missing parts; 46-72 s wall, 2.9 GB peak |
| `bin/g2c build --line-file`, cold (empty `GOCACHE`, std included) | amd64 33-55 s, arm64 30-48 s (2.5 GB peak) |
| the same, warm | amd64 9.7-13 s, arm64 9.0-15.8 s |
| executable (static, not stripped) | amd64 23.5 MB, arm64 22.3 MB |
| start to the uncaught exception (`RT` initialized) | amd64 7 ms, 18 MB resident; arm64 under qemu 0.65 s |

### Proposed amendments (phase 2D)

- **D1 (§4.3, §7.9.6) Positions.** The analyzer keeps each form's line on its node and each
  member's line; c2g puts it on the statements and declarations it writes and lays the forms
  out on those lines (explicit `^{:line n}` where the text has passed them; declarations by
  line); an `arbace/lang` Go file is named after its class forms file; builds use
  `--line-file`. The frame table stays as J4 says (names only): lines come from gc.
- **D2 (§7.8) Pattern switches** are an `if` chain in a labeled block left by each matching
  arm; derived record accessors are field reads; a declared accessor's exception is wrapped
  in `MatchException` (a jrt stand-in until the closure has `java.lang.MatchException`).
- **D3 (§6.2, §13.4) Benign initialization.** A static method needs no entry guard when its
  class's initialization is benign (defined above) and it reads none of its class's or
  superclasses' statics.
- **D4 (§4.4) Checks.** The collision check covers generated and hand-written names; the
  package-private check compares Java packages, not Go packages; both are errors.
- **D5 (§8.3) Race report.** `report.edn` `:race-candidates`, and `bin/c2g-race`
  (`-race` needs cgo, which the race build alone turns on).
- **D6 (§10.6) `--program`** writes `arbace/cmd/arbace` as above.
- **D7 (§13.4, for jrt)** `GOGC` set by jrt at start (400 measured here), and arrays of
  references as one allocation (or 8-byte slots, §13.2's thin pointers) are where the factor
  lies, not c2g's code.

## Proposed amendments to C2G-SPEC (for the user's review)

- **A1 (§4.6) Indexed static initializers.** Variants address a class's n-th
  `static-initializer` (0-based, in source order): `(c2g/cut (static-initializer n))`, and a
  replacement is `(static-initializer ...)` tagged `^{:c2g/nth n}`. Needed by `Compiler`, whose
  static initializers are several.
- **A2 (§4.1) Slices.** `--slice REGEX` restricts translated code, not the closed world:
  classes outside keep their types, tables and stubs. And operation-level stubs: an operation
  naming something outside the world becomes `C2g_Missing[T]` (throwing
  `UnsupportedOperationException("c2g: ...")`) instead of failing the method.
- **A3 (§9.1, J9) Replaced stand-ins initialize eagerly.** A translated class replacing a
  stand-in whose static initializer is not trivial is initialized from `init()` of package
  jrt, as jrt's hand-written code reads its statics directly.
- **A4 (§4.1, §5.7) Cut classes and reflected marker interfaces.** A class literal of a class
  outside the world is a registered class without instances (`instanceof` false); a JDK
  marker interface without class forms is declared from reflection (no methods).
- **A5 (§11) `String.format` by c2g.** When `java.util.Formatter` is translated, c2g
  generates `String_Format_String_O1__String` (and the `Locale` overload) in jrt's package.
- **A6 (§4.4) The rename table** starts with `jdk/internal/util/ByteArray` → `Jdk_ByteArray`.
- **A7 (§7.13) Records' object methods** are generated in Go as `ObjectMethods` defines them,
  with the support helpers above.
- **A8 (§7.2) Order of evaluation**: the second hoisting rule above, stated in §7.2.

Corrected in passing (marked "Correction" in the spec): §5.8's identity hash (jrt's global
sequence mixed by a finalizer, V3) and §7.9.5's bounds errors (jrt reads `boundsError`'s
fields).

## Sources

Nothing vendored. Studied: `java.lang.runtime.ObjectMethods` (openjdk/jdk26u,
`src/java.base/share/classes/java/lang/runtime/ObjectMethods.java`) for the records' methods;
the Go specification's order of evaluation (go.dev/ref/spec#Order_of_evaluation) for A8.
