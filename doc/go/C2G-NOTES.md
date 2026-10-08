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
