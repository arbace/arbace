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

# c2g, phase 2A: Reflector, RT's runtime services, printing

B1a step 4, phase 2, part A (branch `c2g-2a`, 2026-10-09; parts B, the evaluator boundary, C,
the rest of the JDK closure, and D, c2g's quality, are parallel branches): what of `RT` and
`Reflector` works in the Go build before `arbace.core` is loaded, and the oracle's class
scripts `Printer`, `VarNamespace`, `UtilRT` and `LispReader` passing every step that does not
need `arbace.core`.

State: every step of the four scripts passes on linux/amd64 and, under `qemu-aarch64`,
linux/arm64, except the 39 that need `arbace.core` (below); with them, all the oracle's class
scripts pass but for 46 steps that need `arbace.core` and the 10 unavailable ones of phase 1
(`String.CASE_INSENSITIVE_ORDER`). Both architectures give the same results, step for step.

## What changed

| where | what |
|---|---|
| `arbace/lang/go/Reflector.clj` (new variant) | `canAccess` calls `Method.canAccess` directly (no `MethodHandle`; the field `CAN_ACCESS_PRED`, the static initializer and `isJava8` are cut); `instanceMethods` collects with a loop (no `java.util.stream`); `boxArg` adapts a Clojure function to a functional interface with `Reflector.adaptFn`, a native calling `jrt.AdaptFn` (amendment R13), instead of `Proxy.newProxyInstance` |
| `arbace/lang/go/RT.clj` | `*out*`, `*err*`, `*in*` bound to `RT$HostWriter` (fd 1), a `PrintWriter` (autoflush) over `RT$HostWriter` (fd 2) and a `LineNumberingPushbackReader` over `RT$HostReader`, two nested classes the variant adds (natives over jrt's host streams); `baseLoader` and `classForName(String, boolean, ClassLoader)` per C2G-SPEC §10.3: no `DynamicClassLoader`, no thread context loader, `Class.forName` over jrt's registry |
| `go/arbace/jrt/natives.clj` | the variants' natives: `Reflector_AdaptFn_Class_O__O_native`; `RT_HostWriter_HostWrite_I_C1_I_I__V_native` and `..._HostFlush_I__V_native` (UTF-16 to UTF-8, an 8 KiB `bufio.Writer` per stream as `OutputStreamWriter`'s encoder buffers, a high surrogate held until its pair, a lone one written as `?` as Java's encoder does, write errors as `IOException`); `RT_HostReader_HostRead_C1_I_I__I_native` (UTF-8 to UTF-16, malformed bytes as U+FFFD, blocking for the first char and then taking only what is buffered, as `InputStreamReader` does; a supplementary character split across reads keeps its low surrogate) |
| `arbace/c2g/world.clj` (shared, 3 lines) | `(c2g/cut ^:static name ^Ret [params])`, a method's head spliced into the cut as C2G-SPEC §4.6's example writes it, was rejected ("Don't know how to create ISeq from Symbol"); only `(c2g/cut (field ...))` and `(c2g/cut (static-initializer n))` worked |
| `arbace/c2g/code.clj` (shared, small) | the store of the constant `null` into a field the world drops (its type is outside it) of `this`, or a static one, is dropped: the field can hold nothing else. `PrintWriter`'s field initializer `psOut = null` (a `PrintStream`) made every `PrintWriter` constructor throw `UnsupportedOperationException`, so `*err*` could not be made |
| `test/c2g/check.clj`, `bin/c2g-check` | the oracle's context: the program first binds `*ns*` to `user` (as the driver runs, `arbace.main -` then `in-ns 'user`); `test/c2g/needs-core.edn`, the steps that need `arbace.core`, counted as "need core" when they fail (a listed step that passes is named, so the list cannot go stale); a total line per architecture; `C2G_SHOW=all` lists the need-core steps too; `bin/jrt overlay`'s path taken from its last line (a first run in a fresh worktree printed its progress before it, and every build failed) |
| `test/c2g/fixtures/FxRuntime.clj` (new) | keyword interning across `System.gc()` (jrt's weak references: a held keyword stays the interned one, a dropped one is interned anew, equal); `Reflector` by name: static methods with run-time overload choice, static fields, instance methods (arity, a `Long` for an `int` parameter, `Boolean` results), public fields, constructors, and the error messages; the stream vars |

## Results

`bin/c2g-check Printer VarNamespace UtilRT LispReader -- --slice '^arbace/lang/' --slice '^java/'
--slice '^jdk/'`, before (main at `30c4a60`) and after; identical on amd64 and arm64:

| script | steps | pass before | pass after | need core | fail after |
|---|---|---|---|---|---|
| Printer | 211 | 194 | 194 | 17 | 0 |
| VarNamespace | 139 | 133 | 135 | 4 | 0 |
| UtilRT | 614 | 612 | 614 | 0 | 0 |
| LispReader | 264 | 239 | 246 | 18 | 0 |
| **the four** | **1228** | **1178** | **1189** | **39** | **0** |

What made the difference: `UtilRT` 518 and 527 (`RT.get`/`nth` on a Java array go through
`Reflector.prepRet`, whose class failed to initialize on `MethodHandles.Lookup`): the `Reflector`
variant. `VarNamespace` 126 and 127 (`Namespace.refer` replacing a mapping warns on `*err*`, which
was unbound): the `*err*` writer and the dropped `psOut` store. `LispReader` 98, 133-135 and 142-144
(`::a` and syntax-quote resolve in `*ns*`, `user` in the oracle): the harness's context.

The whole check (`bin/c2g-check -- --slice ...`, every script and the fixtures, both
architectures, 8 min 48 s): 4,953 of 5,009 steps pass (phase 1's 5,005 and `FxRuntime`'s 4),
0 fail, 10 unavailable, 46 need core; per architecture the same.

Also checked by hand (a program reading forms from `*in*` and printing them with `RT.print` to
`*out*`, then a line to `RT.errPrintWriter()`): UTF-8 both ways with a supplementary character,
output buffered until `flush`, stderr flushed by `println`. And `bin/jrt test` (amd64), `bin/jrt
build` (vet, amd64 and arm64) pass with the new natives.

## The steps that need `arbace.core`

`test/c2g/needs-core.edn` lists them with their reasons; 46 steps, in four kinds:

- **Core's printer** (`print-method`; `RT.print` calls `pr-on` once `*print-initialized*` is
  true, otherwise its own printer runs, which the Go build uses until step 5): `Printer` 22, 24,
  26, 146 (`##Inf`, `##-Inf`, `##NaN`; RT prints `Infinity`, `NaN`), 157 (a `BigInteger` without
  suffix; RT: `BIGINT`), 184, 187, 191 (`java.util` collections as Clojure's), 193, 195 (a class
  by name; RT: `#=java.lang.String`), 197 (a var as `#'`; RT: `#=(var ...)`); `VarNamespace` 80
  (a var in a printed map); `LispReader` 258, 262, 264 (`ReaderConditional`); `PersistentHashMap`
  127 and `PersistentTreeMap` 180-183, 187, 188 (a `BigInteger` key in a printed map).
- **Namespace maps** (core's printer with `*print-namespace-maps*`, a core var `arbace.main`
  binds true): `Printer` 109, 110, 113-116; `LispReader` 162, 164.
- **Data readers** (core's `default-data-readers` for `#inst` and `#uuid`, and its
  `*default-data-reader-fn*`, root `nil`, so an unknown tag throws; RT's root before core is an
  empty map, which answers a tag's form itself): `LispReader` 167-170, 235, 249-256.
- **Core's vars**: `VarNamespace` 137-139 (`arbace.core/+`).

So when step 5 loads `arbace.core`, these are the steps to watch: the printer, the data readers
and `arbace.main`'s bindings, not the classes.

## Decisions where the spec was silent

- **Natives of Arbace's variants.** A `^:native` method a variant adds to an `arbace.lang` class
  is translated as the JDK's are (§9.1), into a call of the jrt function `C_M..._native`; that
  is how the Go build's `arbace.lang` reaches Go-only code (`jrt.AdaptFn`, the host streams)
  without a jrt class with a Java API for it. Only static natives: jrt cannot name
  `arbace/lang`'s types. The manifest does not list them (it declares jrt's classes).
- **Nested classes in variants.** `(c2g/add (defclass ^:static ...))` adds a member class
  (`RT$HostWriter`, `RT$HostReader`); the variant code already allowed it.
- **Interim streams.** jrt has neither `OutputStreamWriter`/`InputStreamReader` (their
  `StreamEncoder`/`StreamDecoder` are `java.nio`'s, cut by R18) nor `System.out`/`err`/`in` yet
  (part C). `RT$HostWriter` and `RT$HostReader` stand in for them, with the JVM's buffering:
  output reaches the host at `flush` (or when 8 KiB are buffered); nothing flushes `*out*` at exit,
  as on the JVM. When jrt has `System`'s streams and the two adapters, the variant's `OUT`, `ERR`
  and `IN` revert to the JVM's initializers and the two classes and their natives go.
- **Reflective targets must be reached.** c2g's reachability does not see the target of a
  reflective call, so a method called only by name is a stub in a sliced translation (the
  fixtures' first run: `PersistentVector.nth(int, Object)` "not translated"). The member tables
  list it, `Reflector` finds it, its invoker throws. A program that calls by name must root the
  public members of the classes it may call: `bin/c2g-check` roots the classes `FxRuntime`
  reflects on (`fixture-reflected`); the REPL program (part D's whole-program `--main`) roots
  all of them.
- **`adaptFn` without an adapter** throws `UnsupportedOperationException("no functional
  interface adapter for C")`. `Compiler.FISupport.maybeFIMethod` asks
  `isAnnotationPresent(FunctionalInterface)`, which jrt answers with `FromFn != nil` (R16); c2g
  does not generate `FromFn` yet (`:fi-adapter`, part B), so `boxArg` never takes that branch
  for now, and `Reflector` passes an `IFn` to a functional interface parameter as the JVM
  would without the adaptation: by `cast` (a `ClassCastException`).
- **The harness's context** is the oracle driver's as far as it needs no `arbace.core`: `*ns*`
  bound to `user`. The driver's other bindings (`arbace.main`'s `with-bindings`) are core vars
  or have no effect on the class scripts.

## Proposed amendments to C2G-SPEC (for the user's review)

- **P2A-1 (§4.6) Variants' natives and member classes.** A variant may add `^:native` static
  methods, translated as calls of jrt's `C_M..._native` functions (hand-written in
  `go/arbace/jrt/natives.clj`), and member classes (`c2g/add` of a `defclass`).
- **P2A-2 (§10.1, §5.11) Reflective roots.** A program that calls members by name roots the
  public members of every class it may call so (the REPL: every class in jrt's registry);
  reachability does not follow reflection.
- **P2A-3 (§4.1, A4) Dropped fields.** A field whose type is outside the closed world is
  dropped from its struct (as implemented); a store of the constant `null` into it, on `this`
  or static, is dropped too; any other access is an operation-level stub (A2).
- **P2A-4 (§9.4, §10.3) RT's variant** binds `*out*`, `*err*`, `*in*` over jrt's host streams
  through `RT$HostWriter`/`RT$HostReader` until jrt has `System`'s streams, and resolves classes
  through `Class.forName` with one loader (`baseLoader` without the context loader,
  `classForName` without `DynamicClassLoader`), as §10.3 describes.

Corrected in passing: §4.6's `(c2g/cut ^:public ^:static loadClassForName ^Class [^String
name])` now works as the spec writes it (it was the implementation, not the spec).

## Sources (phase 2A)

Nothing vendored. Studied: openjdk/jdk26u `src/java.base/share/classes/sun/nio/cs/StreamEncoder.java`
and `StreamDecoder.java` (the 8 KiB buffer, flushing only on `flush`, `?` for an unmappable
lone surrogate in UTF-8, U+FFFD for malformed input, `InputStreamReader.read` returning what is
available after the first char); Arbace's `arbace/main.clj` (`with-bindings`, the oracle
driver's context) and `test/oracle/driver.clj`.

# c2g, phase 2B: the evaluator boundary

Branch `c2g-2b` (2026-10-09), C2G-SPEC §10, §5.12, §7.11; the evaluator's plan is
[EVAL-PLAN.md](EVAL-PLAN.md).

- **`Compiler` as a whole** translates and builds (`--root 'arbace.lang.Compiler$*'`): 94 of its
  96 classes, about 17,400 Go lines; ASM is erased (B2), the back end does not exist in Go,
  `ObjExpr.compile` generates nothing and a `fn*` evaluates to an evaluator class (B3). What
  still throws when reached: `loadFile` (files), `compileStub` (deftype: step 5),
  `isBoxedMath` (annotations' values). Small `RT` and `Reflector` variants go with it
  (`makeClassLoader`/`baseLoader` nil, `classForName` over jrt's registry; `canAccess` without a
  method handle); phase 2A's variants of these classes supersede them where they overlap.
- **The proof** `bin/c2g-evalproof` (`test/c2g/eval/`, expected output
  `test/c2g/eval/expected.txt`): forms read, analyzed, their trees printed from Go and
  evaluated; a run-time class; a fn adapted by `FromFn`. Same output on linux/amd64 and
  linux/arm64 (qemu).
- **`Dyn`** (§5.12) is generated in arbace/lang's `c2g_dyn.go` when `Compiler$Dyn` is translated
  (B4). **`:fi-adapter`** is translated (the fixture `FxFi`, 4 of 4 on the JVM's results), and
  **`FromFn`** is set for every functional interface translated (B5).
- `bin/c2g-check` (amd64): no regression; `UtilRT` 614 of 614 now (the `Reflector` variant),
  `NumbersCompare` 784 of 784 (the oracle re-recorded on main), fixtures 40 of 40.

Found on the way: a missing operation can leave a Go local unused (`declared and not used`:
an `ArrayList.removeIf` in a world without `Predicate`); `fold-items` sees the local as used
because its use was translated before the operation was found missing (phase 2D).

## Proposed amendments to C2G-SPEC, phase 2B (for the user's review)

- **B1 (§4.6) Nested-class variants.** A variant names a nested class by its binary name,
  `(c2g/variant Compiler$ObjExpr ...)`, and replaces, cuts or adds its members as for a
  top-level class. `Compiler`'s back end lives in its nested classes.
- **B2 (§4.6, §10.1) Erased packages.** A variant file's `(c2g/erase "arbace/asm/")` erases the
  classes of a package outside the closed world: an expression of an erased type is nil (its
  operands not evaluated: ASM's factories are pure), a store into a field or array of an
  erased type is dropped, and members whose descriptors name an erased class do not exist in Go
  (as for any class outside the world). Operations of non-erased types on erased values still
  throw. This replaces cutting the about 150 `emit` methods and 120 ASM fields one by one, and
  copies no line of `Compiler` into the variant.
- **B3 (§10.2) The evaluator's classes** are nested classes the `Compiler` variant adds:
  `Compiler$Frame`, `Compiler$EvalFn extends RestFn` (one class for every arity: required
  arity 0, the arguments as a seq; §10.2's `EvalFn`/`EvalRestFn` pair is EVAL-PLAN Q3),
  `Compiler$Evaluator` (`eval(Expr, Frame)`, `invokeFn`) and `Compiler$Dyn`. `ObjExpr.eval` of a
  `FnExpr` is an `EvalFn`.
- **B4 (§5.12) `Dyn` as built.** `lang.Dyn` is `{jrt.Object; D *DynClass; F []any}`; the slot
  table is per class (`DynClass`: `Cls`, `Slots []IFn`, the implemented interfaces with their
  superinterfaces, the methods by name and descriptor); slots 0-2 are `toString`, `hashCode`
  and `equals` (the header's when unset). The interfaces are every public interface translated
  with their superinterfaces (hand-written jrt interfaces are not covered: they have
  hand-written `InstanceOf` without the check); a method's fn is called with the object and
  the boxed arguments, its result converted as compiled deftype methods convert (`RT`'s casts;
  `Boolean`/`Character` unboxed); an unset method calls the most specific default method of an
  implemented interface, else throws `AbstractMethodError`. The nominal check is an assertion
  to the jrt interface `Dynamic` (`DynImplements`) after a successful assertion, in every
  interface's `InstanceOf` and `Cast` (an interface assertion, not §5.12's type-word compare:
  jrt's functions cannot name a type of arbace/lang). jrt adds `Dynamic`, `DefineDynamic` (a
  registration a later deftype of the same name may replace) and `Class.Descriptor`
  (`dyn.clj`). The Java API is `Compiler$Dyn`'s natives: `defineClass`, `setMethod`,
  `newInstance`, `getField`, `setField`.
- **B5 (§7.11, R13) `FromFn` from arbace/lang.** c2g writes `c2g_fromfn.go` in arbace/lang (it
  calls `IFn` and `RT`), whose `init` sets `FromFn` of every `@FunctionalInterface` translated
  (jrt's included; a class with source is asked through its declaration's annotations). The
  adapter behaves as Reflector's proxy: arguments boxed, `applyTo`, the result converted as
  `coerceAdapterReturn` does. Reachability treats these interfaces as lambda targets (their
  default methods). The analyzer's `:fi-adapter` is the interface's adapter calling the
  `FnInvokers` invoker the analyzer chose.
- **B6 (§9.1) Natives in their class's package.** A `^:native` method is a call of
  `C_M..._native` in the class's own package (jrt's for the JDK, arbace/lang's for `Compiler$Dyn`,
  which c2g writes).
- **B7 (bin/c2g) The root spec `CLASS$*`**: the class and its named nested classes.
- **B8 (classes/SPEC.md, the analyzer) `clj-fi-method` for classes from source.** Compiler's
  functional interface test reflected on the running JDK and found nothing for a class with
  class forms in the world (`*from-source*`: c2g's JDK closure), so c2g's world compiled a
  checkcast where the JVM adapts; the analyzer now asks the declaration (its
  `@FunctionalInterface` annotation and single abstract method). The JVM build's choice is
  unchanged.
