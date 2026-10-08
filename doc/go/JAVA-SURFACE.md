# The Java surface of Arbace

B1a step 1 ([B1-PLAN.md](B1-PLAN.md)): every JDK class, method, constructor and field that
Arbace's runtime and namespaces use, measured from the compiled classes, so that the scope of
jrt (the Java runtime in Go forms under the c2g-translated runtime, decision D1) can be decided.
Measured on main at `c8f271b` (2026-10-08), JDK 26.0.2.1, jdk26u's sources at `/root/jdk26u`.

Summary for the reader in a hurry: the runtime plus the namespaces loaded at a REPL's start use
**278 JDK classes and 970 distinct members**. Of these, 150 classes (509 members) are plain Java
that j2c already converts exactly: translated with what they need in turn, they make a closure
of **422 JDK classes, 3,223 methods, 279 KB of bytecode, from 184 source files of which all
176 that j2c-check covers convert to javac's class shapes** (the other 8 are generated at the
JDK's build). What is left for hand-written Go is 57 classes used directly (300 members) and
the 69 classes (342 members) where the translated code stops, largely the same ones: the
object model, strings, `Class` and reflection, threads, atomics and locks, `Unsafe`'s CAS, the
host's files and clock. 7 `java.lang.invoke` classes and class loading are made unnecessary by
the evaluator, and 62 classes are cut (D6). The port of `java.util.regex` (D5) is 5,148 code lines that j2c
converts exactly; its closure is dominated by Unicode tables. The decisions for the user are
in the last section.

## Method

`bin/java-surface` (script) and `test/g2c/java_surface.clj` (the tool, on Arbace's stage 2)
read every class file of `target/stage2` (5,740 classes, after `bin/build-arbace`) with the
JDK's class file API (`java.lang.classfile`), and collect each reference to a class outside
Arbace (`java.*`, `javax.*`, `jdk.*`, `sun.*`, `org.xml`, `org.w3c`):

- **instructions**: `invoke*` (owner, name, descriptor), `get/putfield`, `get/putstatic`, `new`,
  `checkcast`, `instanceof`, `anewarray`, `multianewarray`, `ldc` of a class, method handle or
  method type, `invokedynamic` (its bootstrap method, and the method handles among its static
  arguments), the catch types of exception handlers;
- **the class itself**: its superclass and interfaces, and every method it declares that
  overrides or implements a method of a JDK supertype (a translated `java.util.List` needs those
  methods, and JDK code calls them);
- **descriptors**: the JDK classes in field and method descriptors, as type-only uses.

A member is resolved as the JVM resolves it: a call whose owner is an Arbace class but whose
method is inherited from the JDK (`Ratio.byteValue`, from `Number`) counts for the declaring
JDK class; a JDK reference keeps its owner and records its declaring class
(`StringBuilder.length`, declared in `AbstractStringBuilder`).

Every class belongs to a **unit** (the top-level class of a class form; for AOT-compiled
namespace classes the namespace, found from the file's `ns` or `in-ns` form) and every unit to
a **group**:

| group | what | classes | units |
|---|---|---:|---:|
| runtime | `arbace.lang`, the class forms of the runtime | 774 | 142 |
| REPL | `arbace.core` and the 21 namespaces loaded at a REPL's start (measured: stage 2's REPL, `loaded-libs` at the first prompt) | 2,779 | 22 |
| shipped | the other namespaces Clojure ships (pprint's parts are in REPL), spec, `arbace.java.api` | 693 | 18 |
| asm | `arbace.asm`, the vendored ASM | 45 | 42 |
| class forms | `arbace.classes`, the class forms compiler (AOT-compiled into the stage) | 1,449 | 11 |

The REPL's start loads, besides `arbace.core`: `core.protocols`, `core.server`, `edn`,
`instant`, `java.basis`, `java.basis.impl`, `java.browse`, `java.io`, `java.javadoc`,
`java.process`, `java.shell`, `main`, `pprint`, `repl`, `repl.deps`, `spec.alpha`,
`spec.gen.alpha`, `string`, `tools.deps.interop`, `uuid`, `walk` (all `arbace.*`).

The namespaces' bytecode holds, besides their source's interop, what the Clojure compiler emits
on its own: boxing (`Integer.valueOf` for every line number in metadata), unboxing, `Boolean`
constants, `Arrays.asList` for constants, `Object.<init>`, `getClassLoader` in `__init`,
`Pattern.compile` for regex literals. These 23 members are flagged as **plumbing** (the
evaluator does not emit them; jrt has each anyway, the runtime uses them all).

**Cross-checks.** (1) `javap -v -p` over the 774 runtime classes: the JDK member references in
their constant pools (573) are exactly those the tool collects with a JDK owner (573; none only
on either side; recorded in the data as `:javap-check`). (2) The class forms compiler's own
resolution: the runtime's classes are its output, so the bytecode records what its analyzer
resolved for every member reference of `arbace/lang/*.clj` (owner, name, descriptor, as
`arbace.classes.analyze` chose them); the javap check confirms the tool reads them back exactly.
A separate hook in the analyzer would see the same references before emission and was not
needed. (3) The REPL measurement and the output are deterministic: two runs give the same
file.

**The translation closure.** For the classes proposed for translation, the tool follows their
JDK bytecode (the running JDK's `jrt:` image, built from jdk26u): from the members Arbace uses,
every method reached, with a rapid type analysis for virtual calls (a call reaches the
overrides in the classes that reached code instantiates), static initializers of classes
touched, until it reaches a class not proposed for translation (the **boundary**: what the
translated code needs of the shims). JDK-internal classes count as translated (`jdk.internal.math`,
`jdk.internal.util`, ICU), except the VM's interfaces (`Unsafe`, `VM`, `SharedSecrets`,
`sun.nio.cs`, the locale providers), which count as shims. Each source file of the closure is
looked up in a `bin/j2c-check --jdk java.base` run (2026-10-08, this branch: 3,002 of 3,025
compiled files shape-identical, as in [CONVERTER-NOTES.md](../classes/CONVERTER-NOTES.md),
"The converted JDK").

**Treatments.** Each JDK class gets one proposed treatment (the table `treatments` in the
tool; by class, else by its top-level class, else by category):

- **translate**: c2g over j2c's conversion of jdk26u's own source; exact Java semantics.
- **shim**: hand-written Go forms over Go's std, or over jrt's host interface (plan, point 4).
- **c2g**: a language feature c2g compiles itself, no jrt class: Java's string `+`
  (`StringConcatFactory`) and lambdas (`LambdaMetafactory`) in the class forms.
- **rework**: Arbace's runtime changed to do without it; here the call sites and method
  handles (`java.lang.invoke`) and class loading, which the evaluator replaces.
- **cut**: left out of the first REPL (D6), with what breaks listed below.

The raw data is [java-surface.edn](java-surface.edn) (Clojure reader; per JDK class: category,
treatment, source file and its lines, and per group the members with their reference kinds,
sites and units; the bootstraps, reflective sites, watched runtime classes, the namespaces'
`arbace.lang` members, the closures (runtime + REPL, with shipped, and regex alone) and the
javap check). Rerun: `bin/build-arbace`, then
`bin/java-surface` (about 15 s; the tables below are written to `.tmp/java-surface-tables.md`;
the j2c statuses come from a prior `bin/j2c-check --jdk java.base`, about 10 minutes, and are
`:unchecked` without it).

## Headline numbers

| | JDK classes | distinct members |
|---|---:|---:|
| runtime (`arbace.lang`) | 224 | 682 (573 referenced directly; the rest overrides and inherited members) |
| REPL namespaces | 161 | 560 (20 plumbing) |
| runtime + REPL | **278** | **970** |
| runtime + REPL + shipped | 308 | 1,092 |
| asm (cut) | 39 | 125 |
| class forms compiler (cut) | 59 | 144 |

By proposed treatment, runtime + REPL:

| treatment | JDK classes | members | what |
|---|---:|---:|---|
| translate | 150 | 509 | boxed numbers, `Character`, exceptions, the collections, `Arrays`, `Collections`, `Objects`, the functional interfaces, `java.math`, `java.util.regex`, readers and writers, `ConcurrentHashMap`, the blocking queues, `TimeUnit`, `UUID`, `Random`, `Formatter`, `Properties` |
| shim | 57 | 300 | `Object`, `String`, `StringBuilder`, `Math`, `Class`, `java.lang.reflect`, `System`, `Runtime`, `Thread`, `ThreadLocal`, `Throwable`, `Enum`, atomics, locks, executors, `CountDownLatch`, references, `File` and its streams, `PrintStream`, the charset readers and writers, `Date`, `Locale`, `Charset`, `sun.misc.Signal` |
| c2g | 2 | 2 | `StringConcatFactory`, `LambdaMetafactory` (the runtime's own string `+` and lambdas: 246 and 9 sites) |
| rework | 7 | 47 | `ClassLoader`; `CallSite`, `MutableCallSite`, `MethodHandle`, `MethodHandles`, `Lookup`, `MethodType` |
| cut | 62 | 112 | `java.net`, `java.nio`, processes, `java.beans`, `java.sql`, `java.time`, `java.text`, calendars, streams, serialization, XML, the VM-only error classes of `RT`'s default imports, annotations |

The translation closure (runtime + REPL; adding the shipped namespaces adds nothing):

| | |
|---|---:|
| classes | 422 |
| methods | 3,223 |
| bytecode | 278,666 bytes |
| source files | 184 (147,831 lines, 67,756 code lines, whole files) |
| of which j2c-identical | 176 (62,090 code lines) |
| of which generated at the JDK's build | 8 (`CharacterData*`, `CaseFolding`, `IndicConjunctBreak`; 5,666 code lines) |
| boundary | 69 classes, 342 members |

The largest parts of the closure are Unicode (`Character`'s blocks and scripts and the
`CharacterData` tables, 75 KB, and `CaseFolding`, 26 KB), `java.math` (35 KB), the collections
(28 KB), `java.util.regex` (28 KB), `Formatter`, the sorts and the immutable collections (27 KB), and
`jdk.internal.math` (the `Double`/`Float` printing and parsing, about 22 KB).

## Reflection, invokedynamic, class loading

### Reflection

Reflection is not an optional feature of the runtime: **the evaluator needs it for every
interop form**. Clojure's `Compiler` resolves `(.m x)`, `(C/m ...)`, `(C. ...)` and fields when
it analyses them, with `Class.getMethods`, `getFields`, `getConstructors`,
`Method.getParameterTypes`, `getReturnType`, `Modifier` (the `Executable` and `Method`
members below); `eval` of an `InstanceMethodExpr` and its kin then calls `Method.invoke`,
`Field.get`, `Constructor.newInstance`, and `Reflector` does both at run time for un-hinted
calls. Evaluating `arbace.core` from source goes through this path for every interop form:
the REPL namespaces reference **588 distinct `arbace.lang` members of 97 classes** (445 apart
from the compiler's plumbing) and the 560 JDK members above. Measured uses:

| class | runtime members | REPL members | runtime users |
|---|---:|---:|---|
| `Class` | 23 | 19 | `Compiler`, `Reflector`, `RT`, `Util`, `Numbers`, `LispReader`, ... (18) |
| `Method` | 11 | 7 | `Compiler`, `Reflector`, `ReflectorCallSite`, `ProxyHandler` |
| `Executable` | 6 | | `Compiler` |
| `Field` | 5 | 3 | `Compiler`, `Reflector`, `ReflectorCallSite` |
| `Constructor` | 2 | 2 | `Compiler`, `Reflector`, `ReflectorCallSite`, `LispReader`, `FnLoaderThunk` |
| `Modifier` | 5 | 6 | `Compiler`, `Reflector` |
| `Array` | 4 | 12 | `RT`, `ArraySeq`, `ArrayIter` (`aget`/`aset` reflective paths, `make-array`) |
| `Proxy`, `InvocationHandler` | 1, 1 | | `Reflector.boxArg`: Clojure 1.12's adaptation of an `IFn` to a functional interface |
| `ParameterizedType` and the generic types | | | none in runtime + REPL (`arbace.reflect`, shipped; the class forms compiler) |

What depends on reflection at the Clojure level: all interop (analysis and evaluation), un-hinted
calls and `set!` of fields (`Reflector`), functional-interface adaptation, `instance?`,
`class`, `cast`, `supers`/`bases`, `make-array`/`into-array`, reflective `aget`/`aset`,
`print-method` of objects (class name, `identityHashCode`), `arbace.reflect` (shipped), `bean`
(also needs `java.beans`: cut). The compiled namespaces contain only **32 reflective call
sites** (REPL group; 8 shipped): Clojure's own code is hinted, so on the JVM reflection is rare
at run time; in the evaluator every call is resolved at run time anyway.

Proposed: **shim `java.lang.reflect` over member tables that c2g generates** for every class it
translates and that jrt's shims declare: per class, its methods, constructors and fields with
names, parameter and return classes, modifiers, and a Go function that invokes each (boxing as
`Method.invoke` does). `Compiler` and `Reflector` are then translated unchanged; the closed
world is exactly D6's "JVM interop beyond jrt's classes" left out: only classes built into the
executable can be named. The tables are the size of the classes exposed: the 97 `arbace.lang`
classes the REPL's namespaces name (or all 142) and the ~150 translated plus ~60 shimmed JDK classes.
The generic-type reflection (`ParameterizedType`, ...) is cut.

### invokedynamic

| bootstrap | runtime | REPL | shipped | asm | class forms |
|---|---:|---:|---:|---:|---:|
| `KeywordInvokeSite.bootstrap` (hand change 12) | | 579 | 104 | | 2,648 |
| `ReflectorCallSite.bootstrap` (hand change 10) | | 32 | 8 | | 6 |
| `StringConcatFactory.makeConcatWithConstants` (hand change 13, and Java's `+`) | 246 | 298 | 28 | 29 | 332 |
| `LambdaMetafactory.metafactory` (Java lambdas) | 9 | | | | |

The sites in the namespaces are the Clojure compiler's emissions (keyword invokes, reflective
calls, `str` with two or more arguments). In the Go build the namespaces are evaluated, so
these become the `eval` of `KeywordInvokeExpr` (`RT.get`), of `StrConcatExpr` (`str`) and of
the interop exprs (reflection above): no call sites, no method handles. `ReflectorCallSite`
(354 lines) and `KeywordInvokeSite` (131 lines) are not translated; `KeywordLookupSite`,
`ILookupSite`, `ILookupThunk` stay (records implement `getLookupThunk`). The runtime's own sites
are Java-level string `+` and lambdas in the class forms: c2g compiles both directly. So
`java.lang.invoke` needs no jrt counterpart: the 7 classes and 47 members are all used by
`ReflectorCallSite`, `KeywordInvokeSite`, `Reflector`'s method-handle helpers and `Compiler`'s
`MethodType` use for emission (rework).

### Class loading

Users of the class loading surface (runtime classes referencing `ClassLoader`,
`DynamicClassLoader` and the loading members of `RT` and `Compiler`): `RT` (`load`,
`loadResourceScript`, `classForName`, `baseLoader`, `lastModified` of resources through `URL`,
`JarURLConnection`, `JarFile`), `DynamicClassLoader` (`defineClass`, `findInMemoryClass`,
`URLClassLoader`, `SoftReference` cache), `Compiler` (`defineClass` at `eval`, `LOADER`),
`FnLoaderThunk` (lazy loading of fn classes), `Compile` (the AOT driver); in the namespaces,
`Compiler.LOADER` in every `__init` (plumbing), `RT.classForName` for class constants (52 sites
in REPL), `DynamicClassLoader.` in `main` and `core.server` (the REPL's context loader),
`addURL` in `repl.deps`, `defineClass` in `arbace.core` (`gen-interface`, cut with the class
forms). In the Go build: `RT.load` reads a namespace's source (embedded in the executable or
from files) and evaluates it, with no `__init` classes; `classForName` looks a name up in the
closed world's registry (the reflection tables); `defineClass` disappears (`deftype`, `reify`,
`defrecord` become evaluator-made types through jrt's dispatch, plan step 5);
`DynamicClassLoader`, `FnLoaderThunk`, `Compile` are not translated. Rework, an estimated 300 lines
of `RT` and `Compiler` touched.

### ASM

Only `Compiler` and `Intrinsics` (the opcode table of the §5.4 operators) reference
`arbace.asm`. With the evaluator, `Compiler`'s back end (its `emit*` methods: about 1,730 of its
8,717 lines, plus `ObjExpr`'s class generation) is not translated, `Intrinsics` is cut, and
ASM (45 classes) is not needed in the Go build. Its JDK surface (39 classes, 125 members)
does not count. The class forms compiler (`arbace.classes`, cut by D6) uses ASM,
`java.lang.classfile` and generic reflection.

## The regex finding (D5)

`java.util.regex` in jdk26u (`src/java.base/share/classes/java/util/regex`): 9 files, 9,592
lines, **5,148 code lines** (`Pattern` 3,668, `Matcher` 627, `CharPredicates` 336, `ASCII` 205,
`PrintPattern` 174 (a debugging aid), `IntHashSet` 60, `PatternSyntaxException` 42,
`MatchResult` 35). With it come `jdk.internal.util.regex.Grapheme` (221 code lines) and two
generated files, `IndicConjunctBreak` (579) and `jdk.internal.lang.CaseFolding` (569).

**j2c converts the package exactly**: in `bin/j2c-check --jdk java.base` every file of
`java/util/regex` and `jdk/internal/util/regex` compiles from its class forms to javac's class
shapes. The generated files are not in `share/classes` and so not in `j2c-check`; they are
plain tables and code (j2c converts them like the rest, one more step).

Its closure alone (from the 17 `Pattern`/`Matcher` members Arbace uses): 250 classes, 1,489
methods, 162,584 bytes, of which `java.util.regex` 59 classes (28 KB), Unicode data 71 KB
(`Character`'s scripts and blocks, `CharacterData*`), `CaseFolding` and the ICU normalizer
(`jdk.internal.icu`, for `CANON_EQ` and `\X`), plus the collections and `ConcurrentHashMap`
it uses. At its boundary: `String`/`StringBuilder` (47 members), `Unsafe` (19, through
`ConcurrentHashMap`), `java.text.Normalizer` (`CANON_EQ`), `java.nio.ByteBuffer` (ICU reads its
data file `nfc.nrm` from the image: a resource to embed). Proposal: translate it whole (D5),
with `CANON_EQ` either cut at first (it alone needs the normalizer's data) or kept with the
data file embedded. Tests: Java's behaviour checked against the JVM on a corpus of patterns, as
the plan says.

## What the cut leaves out (D6), runtime + REPL

| cut | used by | what breaks |
|---|---|---|
| `java.net` (`URL`, `URI`, `Socket`, `ServerSocket`, `InetAddress`, `URLClassLoader`, ...) | `RT`, `DynamicClassLoader`; `java.io`, `core.server`, `repl.deps`, `spec.gen.alpha`, `java.browse` | resources by URL (reworked with loading), `io/as-url` and readers on URLs, the socket REPL, `add-lib` |
| processes (`Process`, `ProcessBuilder`) | `java.process`, `java.shell`, `java.browse` (loaded at start) | `sh`, `arbace.java.process` |
| `java.nio` (`Files`, `Path`, `FileChannel`, ...) | `main`, `java.io` | the temp file of `main`'s error report, `io/copy`'s channel fast path |
| `java.beans` | `arbace.core` | `bean` |
| `java.sql` | `arbace.core`, `instant` | `resultset-seq`, `#inst` as `Timestamp` |
| `java.time.Instant`, `java.text`, `Calendar`, `GregorianCalendar`, `TimeZone` | `instant`, `core` | `#inst` reading and printing as they are written (`instant` reworked over a `Date` shim, below) |
| `java.util.stream` | `Compiler`, `Reflector` (a few uses: rewritten as loops); `core` | `stream-reduce!`, `stream-seq!`, `stream-transduce!`, `stream-into!` (no streams exist) |
| `ObjectInputStream`, `ObjectOutputStream` | `LazySeq`, `Iterate`, `IteratorSeq`, `EnumerationSeq`, `FnLoaderThunk`, `core` | Java serialization (`writeReplace`, `readObject`) |
| `org.xml.sax` | `XMLHandler` (runtime), `arbace.xml` | `arbace.xml` |
| `Module`, `ModuleLayer`, `Package`, `StackWalker`, `ProcessHandle`, `ThreadGroup`, `ThreadDeath`, `RuntimePermission`, the VM's error classes, annotations | `RT`'s default imports (class constants only), `java.javadoc` | those names unresolvable unqualified; `javadoc` |
| `java.util.jar`, `java.util.zip` | `RT` | loading from jars |
| `ForkJoinPool`, `ForkJoinTask` (shipped) | `core.reducers` | `r/fold` in parallel |
| `javax.swing`, `java.awt` (shipped) | `inspector`, `java.browse-ui` | the inspector, the browser UI |

`ProxyHandler` (53 lines) is unused upstream legacy and cut with `proxy`.

## Effort per treatment

Agent-assisted days, at the pace of the class forms and g2c work; they refine plan step 3
(6-10) and touch steps 4 and 5.

| treatment | content | size | estimate |
|---|---|---|---:|
| translate | c2g over the closure; c2g's coverage of the JDK's idioms (nested and anonymous classes, generics, static initialization order, `switch`, arrays, intrinsic fallbacks); the 8 generated files through j2c; tests (regex against the JVM, `BigDecimal` and number printing, `Formatter`, the collections' contracts) | 422 classes, 3,223 methods, 279 KB bytecode, 184 files all convertible | 4-6 |
| shim | the object model (`Object`, `Class`, identity hash, monitors), UTF-16 `String`/`StringBuilder`/`StringBuffer` (D4), `Math`, `System`, `Runtime`, `Thread` over goroutines, `ThreadLocal`, `Throwable` and stack traces, `Enum`, `Record`; reflection over c2g's tables; atomics, locks with `Condition`, executors, `CountDownLatch`, `LockSupport`; references over Go's `weak` and `runtime.AddCleanup`; `Unsafe`'s 25 members (CAS, array offsets) as used by the translated code; the host's files and streams, UTF-8 charsets, `Locale.ROOT`/`US` and `DecimalFormatSymbols`, `SecureRandom` over `crypto/rand`, `Date`, signals | 57 classes / 300 members used directly; boundary 69 classes / 342 members (largely the same classes) | 6-9 |
| c2g | string `+` and lambdas | 255 sites in the runtime | in steps 2 and 4 |
| rework | the evaluator instead of `invokedynamic`, `defineClass` and `__init` loading; `RT.load`/`classForName` over source and the registry; `Reflector`'s method-handle helpers and stream uses | 7 `java.lang.invoke` classes; `ReflectorCallSite`, `KeywordInvokeSite`, `DynamicClassLoader`, `FnLoaderThunk`, `Compile` (710 lines) not translated; about 300 lines of `RT`, `Compiler`, `Reflector` changed (an estimate) | 2-3 (with step 5) |
| cut | stubs that throw `UnsupportedOperationException` where a name must exist, the REPL's start set, `instant` over the `Date` shim | 62 classes | 1 |
| **all** | | | **13-19** |

So jrt (plan step 3) is about 10-15 days rather than 6-10, the translated part being cheap
because j2c already converts it exactly; the rest of the difference moves into steps 4 and 5.

## Decisions for the user

1. **Reflection.** (a) Shim `java.lang.reflect` and `Class` over member tables c2g generates
   for every class built in, translating `Compiler` and `Reflector` unchanged (closed world, as
   D6 says); (b) rework the analyzer and `Reflector` onto a jrt registry with its own API
   (smaller tables, but `Compiler` and `Reflector` drift from Arbace's); (c) only hinted
   interop (breaks un-hinted code and `Reflector`'s overload choice by argument classes).
   *Recommended: (a).*
2. **How much of the JDK to translate.** (a) Translate everything that is plain Java (the
   closure above: 422 classes, all convertible by j2c), shim only at the VM's edge; (b) shim
   more by hand over Go (`HashMap` over Go maps, `BigInteger` over `math/big`, `Character` over
   `unicode`): less code, but different behaviour (iteration order, hash codes,
   `BigDecimal`'s scale and printing, Unicode version) that the differential tests would keep
   finding. Sub-choices under (a): `BigInteger`/`BigDecimal` translated rather than over
   `math/big` as the plan's step 3 wrote; `Character` from the JDK's generated tables (exact
   Unicode of JDK 26); `Formatter` (`format`, `printf`) translated with a root-locale shim.
   *Recommended: (a) with the three sub-choices.*
3. **Strings.** (a) A hand-written UTF-16 `String`/`StringBuilder` (D4) over `[]uint16`, with
   `hashCode`, `compareTo`, case mapping through the translated `Character`; (b) translate
   OpenJDK's compact strings (`String`, `AbstractStringBuilder`, `StringLatin1`, `StringUTF16`:
   5,216 code lines, written around VM intrinsics and `Unsafe`). *Recommended: (a).*
4. **Concurrency.** (a) Shim atomics, locks (with `Condition`), executors, `CountDownLatch`,
   `LockSupport` over `sync` and goroutines; translate `ConcurrentHashMap` and the blocking
   queues over a shim of `Unsafe`'s CAS; (b) translate `java.util.concurrent.locks` (AQS) too,
   over `LockSupport` and `Unsafe` (exact, more code); (c) shim `ConcurrentHashMap` as a locked
   map (less code, different iteration). *Recommended: (a).*
5. **Thread identity** (`Thread.currentThread`, `ThreadLocal`: `Var`'s dynamic bindings,
   `LockingTransaction`, `Agent`'s nesting; Go has no goroutine-local storage). (a) A
   goroutine-local slot in the Go runtime held as forms (one field of `g` and two functions;
   a patched runtime for linux and later TamaGo builds); (b) an explicit thread argument that
   c2g threads through every method (no runtime patch, every signature changes); (c) the
   goroutine id parsed from `runtime.Stack` with a map (no patch, about a microsecond per
   lookup, paid on every dynamic var access). *Recommended: (a); (c) as a stopgap for the
   first REPL if the patch waits.*
6. **The REPL's start set.** (a) The Go build's REPL loads `arbace.core`, `main`, `repl`,
   `pprint`, `string`, `walk`, `edn`, `java.io`, `instant`, `uuid`, `core.protocols`,
   `spec.alpha`, `spec.gen.alpha` and not `repl.deps`, `java.basis(.impl)`,
   `tools.deps.interop`, `java.process`, `java.shell`, `java.browse`, `java.javadoc`,
   `core.server`, which need processes, URLs and sockets; (b) load all 22 with stubs that fail
   on use. *Recommended: (a).*
7. **`#inst`.** (a) Rework `arbace.instant` for the Go build over a `Date` shim (millis,
   RFC 3339 printing and parsing over Go's `time`); (b) translate `Calendar`,
   `GregorianCalendar`, `TimeZone` and `sun.util.calendar` with the time zone data; (c) no
   `#inst` at first. *Recommended: (a).*

## Tables

Generated by `bin/java-surface` (`.tmp/java-surface-tables.md`); member counts are distinct
members (methods with their descriptor, fields by name), sites are instructions and
declarations.

### Groups

| group | classes | units | JDK classes | JDK members | of which plumbing | reference sites |
|---|---:|---:|---:|---:|---:|---:|
| runtime | 774 | 142 | 224 | 682 | 23 | 14,899 |
| REPL | 2,779 | 22 | 161 | 560 | 20 | 45,169 |
| shipped | 693 | 18 | 73 | 217 | 13 | 10,787 |
| asm | 45 | 42 | 39 | 125 | 15 | 1,533 |
| class forms | 1,449 | 11 | 59 | 144 | 18 | 27,336 |
| **runtime + REPL** |  |  | 278 | 970 |  |  |
| **runtime + REPL + shipped** |  |  | 308 | 1,092 |  |  |

### Categories

Classes / distinct members per group; the treatment is the category's default, with the
classes treated otherwise counted under "other treatments".

| category | treatment | runtime | REPL | shipped | asm | class forms | other treatments |
|---|---|---:|---:|---:|---:|---:|---|
| lang-core | translate | 28 / 118 | 23 / 108 | 11 / 66 | 13 / 68 | 14 / 46 | cut 3, shim 7 |
| lang-exceptions | translate | 52 / 25 | 11 / 15 | 6 / 9 | 10 / 14 | 6 / 3 | shim 1 |
| lang-system | shim | 14 / 27 | 8 / 52 | 2 / 7 | 1 / 2 | 3 / 3 | cut 5 |
| class | shim | 5 / 34 | 3 / 24 | 2 / 8 | 2 / 11 | 5 / 34 | rework 1, cut 3 |
| reflect | shim | 8 / 35 | 5 / 30 | 3 / 15 | 2 / 4 | 9 / 27 | cut 5, translate 1 |
| invoke | rework | 8 / 36 | 1 / 1 | 1 / 1 | 1 / 1 | 1 / 1 | c2g 2 |
| ref | shim | 4 / 5 |  |  |  |  |  |
| lang-misc | cut | 4 / 0 |  |  |  |  |  |
| regex | translate | 2 / 10 | 2 / 17 | 1 / 1 | 1 / 1 | 1 / 1 |  |
| functional | translate | 8 / 6 | 9 / 5 |  |  |  |  |
| stream | cut | 2 / 4 | 1 / 1 |  |  |  |  |
| atomics-locks | shim | 9 / 30 | 2 / 7 |  |  |  |  |
| concurrency | translate | 9 / 23 | 12 / 21 | 3 / 6 |  | 1 / 0 | shim 6, cut 2 |
| collections | translate | 30 / 182 | 16 / 95 | 6 / 17 | 5 / 11 | 6 / 12 |  |
| util-misc | translate | 2 / 1 | 6 / 11 | 1 / 0 |  |  | cut 4, shim 1 |
| math | translate | 3 / 60 | 2 / 6 |  |  | 1 / 3 |  |
| io | translate | 21 / 47 | 29 / 120 | 6 / 3 | 4 / 13 | 6 / 9 | shim 7, cut 2 |
| nio | cut | 1 / 1 | 8 / 7 |  |  | 2 / 1 | shim 1 |
| net | cut | 7 / 13 | 9 / 18 | 1 / 0 |  | 1 / 1 |  |
| text | cut |  | 2 / 3 |  |  |  |  |
| time | cut |  | 4 / 9 |  |  |  |  |
| desktop | cut |  |  | 24 / 65 |  |  |  |
| xml | cut | 4 / 23 |  | 6 / 19 |  |  |  |
| jdk-internal | translate |  | 2 / 3 |  |  |  | shim 2 |
| other | cut | 3 / 2 | 6 / 7 |  |  | 3 / 3 |  |

### Treatments, runtime + REPL

| treatment | JDK classes | members | sites | classes (runtime + REPL + shipped) | members |
|---|---:|---:|---:|---:|---:|
| translate | 150 | 509 | 17,559 | 150 | 509 |
| shim | 57 | 300 | 41,467 | 57 | 343 |
| c2g | 2 | 2 | 553 | 2 | 2 |
| rework | 7 | 47 | 172 | 7 | 47 |
| cut | 62 | 112 | 317 | 92 | 191 |

### The translation closure, runtime + REPL

| category | classes | methods | bytecode bytes |
|---|---:|---:|---:|
| lang-core | 32 | 495 | 74,950 |
| jdk-internal | 56 | 277 | 61,790 |
| math | 8 | 316 | 34,736 |
| collections | 122 | 909 | 28,424 |
| regex | 59 | 367 | 28,309 |
| util-misc | 56 | 376 | 27,356 |
| concurrency | 34 | 209 | 14,901 |
| io | 27 | 207 | 7,510 |
| lang-exceptions | 23 | 57 | 614 |
| reflect | 1 | 6 | 76 |
| functional | 4 | 4 | 0 |
| **all** | 422 | 3,223 | 278,666 |

Source files touched: 184, 147,831 lines, 67,756 code lines (whole files).
By j2c (`bin/j2c-check --jdk java.base`): 8 files generated (5,666 code lines), 176 files identical (62,090 code lines).
Boundary (calls from translated code into shimmed, reworked or cut classes): 69 classes, 342 members.

| boundary class | treatment | members | examples |
|---|---|---:|---|
| `java.lang.String` | shim | 41 | `<init>`, `<init>`, `<init>`, `<init>`, `<init>` |
| `java.lang.StringBuilder` | shim | 27 | `<init>`, `<init>`, `<init>`, `append`, `append` |
| `jdk.internal.misc.Unsafe` | shim | 25 | `ARRAY_BOOLEAN_INDEX_SCALE`, `ARRAY_BYTE_BASE_OFFSET`, `ARRAY_BYTE_INDEX_SCALE`, `ARRAY_CHAR_INDEX_SCALE`, `ARRAY_DOUBLE_INDEX_SCALE` |
| `java.lang.Math` | shim | 22 | `abs`, `abs`, `abs`, `addExact`, `addExact` |
| `java.lang.StringBuffer` | shim | 15 | `<init>`, `append`, `append`, `append`, `append` |
| `java.time.temporal.ChronoField` | cut | 15 | `AMPM_OF_DAY`, `CLOCK_HOUR_OF_AMPM`, `DAY_OF_MONTH`, `DAY_OF_WEEK`, `DAY_OF_YEAR` |
| `java.lang.Throwable` | shim | 11 | `<init>`, `<init>`, `<init>`, `<init>`, `addSuppressed` |
| `java.nio.ByteBuffer` | cut | 11 | `asCharBuffer`, `asIntBuffer`, `get`, `get`, `getChar` |
| `java.util.Calendar` | cut | 8 | `clone`, `get`, `getInstance`, `getTimeInMillis`, `getTimeZone` |
| `java.util.concurrent.locks.ReentrantLock` | shim | 8 | `<init>`, `<init>`, `hasWaiters`, `lock`, `lockInterruptibly` |
| `java.lang.Class` | shim | 7 | `desiredAssertionStatus`, `getComponentType`, `getDeclaredField`, `getGenericInterfaces`, `getName` |
| `java.lang.Enum` | shim | 7 | `<init>`, `compareTo`, `equals`, `hashCode`, `ordinal` |
| `java.util.concurrent.atomic.AtomicInteger` | shim | 7 | `<init>`, `get`, `getAndDecrement`, `getAndIncrement`, `getAndSet` |
| `java.util.concurrent.atomic.AtomicLong` | shim | 7 | `<init>`, `<init>`, `compareAndSet`, `get`, `intValue` |
| `java.lang.Object` | shim | 6 | `<init>`, `clone`, `equals`, `getClass`, `hashCode` |
| `java.nio.CharBuffer` | cut | 6 | `array`, `arrayOffset`, `get`, `hasArray`, `position` |
| `java.text.DateFormatSymbols` | cut | 6 | `getAmPmStrings`, `getInstance`, `getMonths`, `getShortMonths`, `getShortWeekdays` |
| `java.text.DecimalFormatSymbols` | shim | 6 | `getDecimalSeparator`, `getGroupingSeparator`, `getInstance`, `getLocale`, `getMinusSign` |
| `java.util.Locale` | shim | 6 | `ENGLISH`, `ROOT`, `US`, `equals`, `getDefault` |
| `java.lang.System` | shim | 5 | `arraycopy`, `getProperty`, `identityHashCode`, `lineSeparator`, `nanoTime` |
| `java.security.SecureRandom` | shim | 5 | `<init>`, `next`, `nextBytes`, `setSeed`, `toString` |
| `java.text.DecimalFormat` | cut | 5 | `<init>`, `equals`, `getGroupingSize`, `hashCode`, `toString` |
| `java.util.zip.InflaterInputStream` | cut | 5 | `<init>`, `available`, `close`, `read`, `read` |
| `java.nio.Buffer` | cut | 4 | `isReadOnly`, `limit`, `position`, `remaining` |
| `java.time.temporal.TemporalAccessor` | cut | 4 | `get`, `getLong`, `isSupported`, `query` |
| `java.util.concurrent.ForkJoinTask` | cut | 4 | `<init>`, `fork`, `invoke`, `join` |
| `java.io.ObjectStreamField` | cut | 3 | `<init>`, `compareTo`, `toString` |
| `java.lang.Thread` | shim | 3 | `currentThread`, `isVirtual`, `yield` |
| `java.util.concurrent.ThreadLocalRandom` | shim | 3 | `advanceProbe`, `getProbe`, `localInit` |
| `jdk.internal.access.JavaLangAccess` | shim | 3 | `uncheckedGetUTF16Char`, `uncheckedNewStringWithLatin1Bytes`, `uncheckedPutCharUTF16` |
| `sun.util.locale.provider.LocaleProviderAdapter` | shim | 3 | `getAdapter`, `getLocaleResources`, `getResourceBundleBased` |
| `java.lang.Runtime` | shim | 2 | `availableProcessors`, `getRuntime` |
| `java.lang.StringUTF16` | shim | 2 | `newString`, `putChar` |
| `java.lang.ref.Reference` | shim | 2 | `clear`, `get` |
| `java.lang.ref.SoftReference` | shim | 2 | `<init>`, `get` |
| `java.lang.reflect.ParameterizedType` | cut | 2 | `getActualTypeArguments`, `getRawType` |
| `java.nio.ByteOrder` | cut | 2 | `BIG_ENDIAN`, `LITTLE_ENDIAN` |
| `java.text.Normalizer$Form` | cut | 2 | `NFC`, `NFD` |
| `java.text.NumberFormat` | cut | 2 | `getNumberInstance`, `isGroupingUsed` |
| `java.time.ZoneId` | cut | 2 | `getId`, `getRules` |
| `java.time.temporal.TemporalQueries` | cut | 2 | `chronology`, `zone` |
| `java.util.TimeZone` | cut | 2 | `getDisplayName`, `getTimeZone` |
| `java.util.concurrent.ForkJoinPool` | cut | 2 | `getCommonPoolParallelism`, `getParallelism` |
| `java.util.concurrent.locks.Condition` | shim | 2 | `await`, `signal` |
| `java.util.concurrent.locks.LockSupport` | shim | 2 | `park`, `unpark` |
| `jdk.internal.misc.CDS` | shim | 2 | `getRandomSeedForDumping`, `initializeFromArchive` |
| `jdk.internal.misc.VM` | shim | 2 | `getSavedProperty`, `isBooted` |
| `java.lang.Record` | shim | 1 | `<init>` |
| `java.lang.StringLatin1` | shim | 1 | `newString` |
| `java.lang.ThreadLocal` | shim | 1 | `withInitial` |
| `java.lang.invoke.LambdaMetafactory` | c2g | 1 | `metafactory` |
| `java.lang.invoke.MethodHandles` | rework | 1 | `byteArrayViewVarHandle` |
| `java.lang.ref.WeakReference` | shim | 1 | `<init>` |
| `java.lang.reflect.Array` | shim | 1 | `newInstance` |
| `java.lang.runtime.ObjectMethods` | rework | 1 | `bootstrap` |
| `java.nio.IntBuffer` | cut | 1 | `get` |
| `java.nio.ReadOnlyBufferException` | cut | 1 | `<init>` |
| `java.text.Normalizer` | cut | 1 | `normalize` |
| `java.time.Instant` | cut | 1 | `from` |
| `java.time.zone.ZoneRules` | cut | 1 | `isDaylightSavings` |
| `java.util.Locale$Category` | shim | 1 | `FORMAT` |
| `java.util.random.RandomGenerator` | cut | 1 | `nextInt` |
| `java.util.stream.IntStream` | cut | 1 | `toArray` |
| `java.util.stream.Stream` | cut | 1 | `mapToInt` |
| `java.util.stream.StreamSupport` | cut | 1 | `stream` |
| `jdk.internal.access.SharedSecrets` | shim | 1 | `getJavaLangAccess` |
| `jdk.internal.event.ThrowableTracer` | shim | 1 | `traceError` |
| `sun.nio.cs.ISO_8859_1` | shim | 1 | `INSTANCE` |
| `sun.util.locale.provider.LocaleResources` | shim | 1 | `getNumberPatterns` |

### The translation closure, runtime + REPL + shipped

| category | classes | methods | bytecode bytes |
|---|---:|---:|---:|
| lang-core | 32 | 495 | 74,950 |
| jdk-internal | 56 | 277 | 61,790 |
| math | 8 | 316 | 34,736 |
| collections | 122 | 909 | 28,424 |
| regex | 59 | 367 | 28,309 |
| util-misc | 56 | 376 | 27,356 |
| concurrency | 34 | 209 | 14,901 |
| io | 27 | 207 | 7,510 |
| lang-exceptions | 23 | 57 | 614 |
| reflect | 1 | 6 | 76 |
| functional | 4 | 4 | 0 |
| **all** | 422 | 3,223 | 278,666 |

Source files touched: 184, 147,831 lines, 67,756 code lines (whole files).
By j2c (`bin/j2c-check --jdk java.base`): 8 files generated (5,666 code lines), 176 files identical (62,090 code lines).
Boundary (calls from translated code into shimmed, reworked or cut classes): 69 classes, 342 members.

### The translation closure, java.util.regex alone (Pattern and Matcher as used)

| category | classes | methods | bytecode bytes |
|---|---:|---:|---:|
| lang-core | 24 | 334 | 71,121 |
| jdk-internal | 45 | 145 | 38,786 |
| regex | 59 | 362 | 28,194 |
| collections | 61 | 384 | 12,816 |
| concurrency | 17 | 100 | 7,648 |
| util-misc | 18 | 104 | 3,268 |
| lang-exceptions | 17 | 38 | 455 |
| io | 6 | 19 | 296 |
| functional | 3 | 3 | 0 |
| **all** | 250 | 1,489 | 162,584 |

Source files touched: 90, 85,272 lines, 41,196 code lines (whole files).
By j2c (`bin/j2c-check --jdk java.base`): 8 files generated (5,666 code lines), 82 files identical (35,530 code lines).
Boundary (calls from translated code into shimmed, reworked or cut classes): 38 classes, 159 members.

### Runtime classes by treatment of what they use

JDK members used by each `arbace.lang` class (with its nested classes), by treatment.

| class | translate | shim | c2g | rework | cut | uses arbace.asm |
|---|---:|---:|---:|---:|---:|---|
| AFn | 2 | 3 |  |  |  |  |
| AFunction | 3 |  |  |  |  |  |
| AMapEntry | 3 |  |  |  |  |  |
| APersistentMap | 38 | 4 |  |  |  |  |
| APersistentSet | 36 | 4 |  |  |  |  |
| APersistentVector | 71 | 4 |  |  |  |  |
| ARef | 4 |  |  |  |  |  |
| AReference |  | 1 |  |  |  |  |
| ASeq | 46 | 3 |  |  |  |  |
| ATransientMap | 3 | 1 |  |  |  |  |
| Agent | 4 | 24 |  |  |  |  |
| ArityException | 2 | 1 | 1 |  |  |  |
| ArrayChunk | 1 | 1 |  |  |  |  |
| ArrayIter | 10 | 3 |  |  |  |  |
| ArraySeq | 18 | 4 |  |  |  |  |
| Atom |  | 4 |  |  |  |  |
| BigInt | 26 | 4 |  |  |  |  |
| Binding |  | 1 |  |  |  |  |
| Box |  | 1 |  |  |  |  |
| ChunkBuffer |  | 1 |  |  |  |  |
| Compile | 5 | 7 | 1 |  |  |  |
| Compiler | 96 | 91 | 2 | 4 | 4 | yes |
| Cycle | 3 | 1 |  |  |  |  |
| Delay |  | 4 |  |  |  |  |
| DynamicClassLoader | 8 | 6 |  | 6 | 4 |  |
| EdnReader | 40 | 13 | 1 |  |  |  |
| EnumerationSeq | 3 | 3 |  |  |  |  |
| ExceptionInfo | 1 | 3 | 1 |  |  |  |
| FnInvokers | 8 | 2 |  |  |  |  |
| FnLoaderThunk | 1 | 3 |  |  |  |  |
| IDeref | 5 |  |  |  |  |  |
| Intrinsics | 1 | 1 |  |  |  | yes |
| Iterate | 3 | 2 |  |  |  |  |
| IteratorSeq | 3 | 3 |  |  |  |  |
| Keyword | 9 | 7 | 1 |  |  |  |
| KeywordInvokeSite | 2 | 2 |  | 12 |  |  |
| KeywordLookupSite | 1 | 2 |  |  |  |  |
| LazilyPersistentVector | 1 | 1 |  |  |  |  |
| LazySeq | 45 | 5 |  |  | 1 |  |
| LineNumberingPushbackReader | 11 | 7 | 1 |  |  |  |
| LispReader | 44 | 24 | 1 |  |  |  |
| LockingTransaction | 34 | 23 | 1 |  |  |  |
| LongRange | 11 | 3 |  |  |  |  |
| Main |  | 1 |  |  |  |  |
| MapEntry | 2 |  |  |  |  |  |
| MethodImplCache | 1 | 1 |  |  |  |  |
| MultiFn | 7 | 8 |  |  |  |  |
| Murmur3 | 6 | 1 |  |  |  |  |
| Namespace | 9 | 11 | 1 |  |  |  |
| Numbers | 63 | 19 | 1 |  |  |  |
| Obj |  | 1 |  |  |  |  |
| PersistentArrayMap | 16 | 5 | 1 |  |  |  |
| PersistentHashMap | 22 | 9 | 1 |  |  |  |
| PersistentHashSet | 4 | 1 | 1 |  |  |  |
| PersistentList | 55 | 4 |  |  |  |  |
| PersistentQueue | 22 | 3 |  |  |  |  |
| PersistentStructMap | 13 | 2 |  |  |  |  |
| PersistentTreeMap | 20 | 5 | 1 |  |  |  |
| PersistentTreeSet | 2 | 1 |  |  |  |  |
| PersistentVector | 28 | 7 |  |  |  |  |
| ProxyHandler | 23 | 7 | 1 |  |  |  |
| RT | 80 | 43 | 1 | 4 | 11 |  |
| Range | 10 | 1 |  |  |  |  |
| Ratio | 16 | 3 | 1 |  |  |  |
| ReaderConditional | 2 | 4 |  |  |  |  |
| RecordIterator | 4 | 1 |  |  |  |  |
| Reduced |  | 1 |  |  |  |  |
| Ref | 4 | 12 | 1 |  |  |  |
| Reflector | 50 | 35 | 2 | 4 | 4 |  |
| ReflectorCallSite | 13 | 8 | 1 | 30 |  |  |
| Repeat | 3 | 1 |  |  |  |  |
| Repl |  | 1 |  |  |  |  |
| Script |  | 1 |  |  |  |  |
| SeqEnumeration | 2 | 1 |  |  |  |  |
| SeqIterator | 5 | 1 |  |  |  |  |
| StringSeq | 3 |  |  |  |  |  |
| Symbol | 1 | 9 | 1 |  |  |  |
| TaggedLiteral |  | 4 |  |  |  |  |
| TransactionalHashMap | 45 | 2 |  |  |  |  |
| TransformerIterator | 14 | 3 | 1 |  |  |  |
| Tuple |  | 1 |  |  |  |  |
| Util | 16 | 13 |  |  |  |  |
| Var | 6 | 14 | 1 |  |  |  |
| Volatile |  | 1 |  |  |  |  |
| XMLHandler |  |  |  |  | 23 |  |

### invokedynamic bootstraps

| bootstrap | runtime | REPL | shipped | asm | class forms |
|---|---:|---:|---:|---:|---:|
| `arbace.lang.KeywordInvokeSite.bootstrap` |  | 579 | 104 |  | 2,648 |
| `arbace.lang.ReflectorCallSite.bootstrap` |  | 32 | 8 |  | 6 |
| `java.lang.invoke.LambdaMetafactory.metafactory` | 9 |  |  |  |  |
| `java.lang.invoke.StringConcatFactory.makeConcatWithConstants` | 246 | 298 | 28 | 29 | 332 |

### Reflective call sites (`ReflectorCallSite`), by group

| group | sites | distinct (kind, class, member) | units |
|---|---:|---:|---|
| REPL | 32 | 18 | arbace.core, arbace.java.browse, arbace.main, arbace.repl, arbace.spec.gen.alpha |
| shipped | 8 | 6 | arbace.inspector, arbace.java.browse-ui, arbace.test.junit, arbace.xml |
| class forms | 6 | 5 | arbace.classes.analyze, arbace.classes.emit, arbace.classes.env, arbace.classes.shape, arbace.classes.types |

### arbace.lang members the namespaces reference

What the evaluator must reach by name when it evaluates the namespaces' interop into the
runtime (`(. RT (count x))`, `(Numbers/add x y)` in inlines, `(.meta x)`); plumbing as for the
JDK (members the compiler emits on its own: fn classes, `Var` roots, literals) apart.

| group | classes | members | of which plumbing | sites |
|---|---:|---:|---:|---:|
| REPL | 97 | 588 | 143 | 52,950 |
| shipped | 34 | 124 | 68 | 16,097 |
| class forms | 28 | 176 | 85 | 24,100 |
| REPL + shipped | 99 | 601 | 144 | 69,047 |

### Per JDK class

Distinct members used per group (R runtime, P REPL, S shipped, A asm, K class forms; `t`
for a type-only use: a cast, an `instanceof`, a catch, a descriptor, a supertype, a class
constant); the runtime's users; the code lines of the class's jdk26u source file.

| class | category | treatment | R | P | S | A | K | runtime users | source code lines |
|---|---|---|---:|---:|---:|---:|---:|---|---:|
| `java.lang.Appendable` | lang-core | translate | t | 3 |  |  |  | RT | 7 |
| `java.lang.AutoCloseable` | lang-core | translate | t | 1 |  |  |  | RT | 4 |
| `java.lang.Boolean` | lang-core | translate | 7 | 4 | 3 | 4 | 3 | AFunction, Agent, ArrayIter, ArraySeq +11 | 84 |
| `java.lang.Byte` | lang-core | translate | 3 | 2 |  | 4 | 4 | ArrayIter, ArraySeq, Compiler, FnInvokers +5 | 138 |
| `java.lang.CharSequence` | lang-core | translate | 2 | 3 |  |  |  | Murmur3, RT, StringSeq | 121 |
| `java.lang.Character` | lang-core | translate | 9 | 7 | 1 | 3 | 2 | ArrayIter, ArraySeq, Compiler, EdnReader +6 | 6,469 |
| `java.lang.Cloneable` | lang-core | translate | t |  |  |  |  | RT | 3 |
| `java.lang.Comparable` | lang-core | translate | 1 | 1 |  |  |  | APersistentVector, Keyword, PersistentTreeMap, RT +4 | 5 |
| `java.lang.Deprecated` | lang-core | cut | t |  |  |  |  | RT | 10 |
| `java.lang.Double` | lang-core | translate | 6 | 8 | 1 | 6 | 6 | ArrayIter, ArraySeq, Compiler, EdnReader +8 | 186 |
| `java.lang.Enum` | lang-core | shim | 4 | t |  |  | 2 | Compiler, Numbers, RT | 110 |
| `java.lang.Float` | lang-core | translate | 4 | 5 |  | 6 | 3 | Compiler, FnInvokers, Numbers, ProxyHandler +3 | 226 |
| `java.lang.FunctionalInterface` | lang-core | cut | t |  |  |  |  | Compiler | 6 |
| `java.lang.Integer` | lang-core | translate | 8 | 7 | 1 | 4 | 5 | APersistentVector, Agent, ArityException, ArraySeq +16 | 593 |
| `java.lang.Iterable` | lang-core | translate | 2 | 3 | 1 |  |  | APersistentMap, APersistentSet, APersistentVector, ASeq +16 | 18 |
| `java.lang.Long` | lang-core | translate | 3 | 4 | 1 | 3 | 2 | Agent, ArrayIter, Compiler, FnInvokers +8 | 596 |
| `java.lang.Math` | lang-core | shim | 16 | 5 | 45 | 2 | 1 | Compiler, LongRange, Numbers, PersistentArrayMap +3 | 863 |
| `java.lang.Number` | lang-core | translate | 7 | 3 | 1 |  | 2 | AFunction, APersistentVector, ArraySeq, BigInt +11 | 16 |
| `java.lang.Object` | lang-core | shim | 6 | 6 | 6 | 5 | 5 | AFn, AFunction, AMapEntry, APersistentMap +135 | 58 |
| `java.lang.Override` | lang-core | cut | t |  |  |  |  | RT | 6 |
| `java.lang.Readable` | lang-core | translate | t | 1 |  |  |  | RT | 5 |
| `java.lang.Runnable` | lang-core | translate | 1 | 1 |  |  |  | AFn, Agent, Compiler, IFn +4 | 5 |
| `java.lang.Short` | lang-core | translate | 3 | 2 |  | 4 | 4 | ArraySeq, Compiler, FnInvokers, Numbers +4 | 146 |
| `java.lang.StrictMath` | lang-core | shim | t |  |  |  |  | RT | 367 |
| `java.lang.String` | lang-core | shim | 27 | 26 | 2 | 15 | 5 | APersistentMap, APersistentSet, APersistentVector, ASeq +38 | 1,879 |
| `java.lang.StringBuffer` | lang-core | shim | t | 2 |  |  |  | RT | 363 |
| `java.lang.StringBuilder` | lang-core | shim | 8 | 13 | 4 | 11 | 2 | Compiler, EdnReader, LineNumberingPushbackReader, LispReader +1 | 246 |
| `java.lang.Void` | lang-core | translate | 1 | 1 |  | 1 |  | Compiler, ProxyHandler, RT, ReflectorCallSite +1 | 6 |
| `java.lang.AbstractMethodError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.ArithmeticException` | lang-exceptions | translate | 1 |  |  |  | t | LongRange, Numbers, RT | 11 |
| `java.lang.ArrayIndexOutOfBoundsException` | lang-exceptions | translate | t |  |  |  |  | RT | 14 |
| `java.lang.ArrayStoreException` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.AssertionError` | lang-exceptions | translate | t | 1 | 1 | 2 |  | RT | 37 |
| `java.lang.BootstrapMethodError` | lang-exceptions | translate | t |  |  |  |  | RT | 19 |
| `java.lang.ClassCastException` | lang-exceptions | translate | 1 |  |  | 1 |  | PersistentTreeMap, PersistentTreeSet, RT | 11 |
| `java.lang.ClassCircularityError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.ClassFormatError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.ClassNotFoundException` | lang-exceptions | translate | 1 | t |  | t | t | Compiler, RT | 39 |
| `java.lang.CloneNotSupportedException` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.EnumConstantNotPresentException` | lang-exceptions | translate | t |  |  |  |  | RT | 16 |
| `java.lang.Error` | lang-exceptions | translate | 1 |  |  |  |  | LockingTransaction, RT | 38 |
| `java.lang.Exception` | lang-exceptions | translate | 2 | 1 | 1 |  | t | ARef, Compiler, EdnReader, FnLoaderThunk +6 | 22 |
| `java.lang.ExceptionInInitializerError` | lang-exceptions | translate | 1 |  |  |  |  | KeywordInvokeSite, RT, ReflectorCallSite | 40 |
| `java.lang.IllegalAccessError` | lang-exceptions | translate | 1 | 1 |  |  |  | PersistentArrayMap, PersistentHashMap, PersistentVector, RT | 11 |
| `java.lang.IllegalAccessException` | lang-exceptions | translate | t |  |  |  |  | RT, Reflector | 10 |
| `java.lang.IllegalArgumentException` | lang-exceptions | translate | 2 | 1 | 1 | 2 | 1 | APersistentMap, APersistentVector, ATransientMap, ArityException +16 | 17 |
| `java.lang.IllegalCallerException` | lang-exceptions | translate | t |  |  |  |  | RT | 17 |
| `java.lang.IllegalMonitorStateException` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.IllegalStateException` | lang-exceptions | translate | 3 | 1 | t | 3 |  | ARef, ArrayChunk, Compiler, LispReader +10 | 17 |
| `java.lang.IllegalThreadStateException` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.IncompatibleClassChangeError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.IndexOutOfBoundsException` | lang-exceptions | translate | 1 | 1 |  | 1 |  | AMapEntry, APersistentVector, PersistentArrayMap, PersistentVector +1 | 17 |
| `java.lang.InstantiationError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.InstantiationException` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.InternalError` | lang-exceptions | translate | t |  |  |  |  | RT | 16 |
| `java.lang.InterruptedException` | lang-exceptions | translate | t |  |  |  |  | LockingTransaction, RT | 11 |
| `java.lang.LayerInstantiationException` | lang-exceptions | translate | t |  |  |  |  | RT | 16 |
| `java.lang.LinkageError` | lang-exceptions | translate | t |  |  |  |  | RT | 14 |
| `java.lang.NegativeArraySizeException` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.NoClassDefFoundError` | lang-exceptions | translate | t |  |  |  | t | RT | 11 |
| `java.lang.NoSuchFieldError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.NoSuchFieldException` | lang-exceptions | translate | t |  |  |  |  | Compiler, RT | 11 |
| `java.lang.NoSuchMethodError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.NoSuchMethodException` | lang-exceptions | translate | t |  |  |  |  | Compiler, RT, Reflector | 11 |
| `java.lang.NullPointerException` | lang-exceptions | translate | 2 |  |  |  |  | Namespace, RT, Util | 36 |
| `java.lang.NumberFormatException` | lang-exceptions | translate | 1 | t |  |  |  | EdnReader, LispReader, RT | 23 |
| `java.lang.OutOfMemoryError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.ReflectiveOperationException` | lang-exceptions | translate | t |  |  |  |  | KeywordInvokeSite, RT, ReflectorCallSite | 17 |
| `java.lang.RuntimeException` | lang-exceptions | translate | 3 | 1 |  | 1 |  | ARef, Compiler, EdnReader, ExceptionInfo +4 | 22 |
| `java.lang.SecurityException` | lang-exceptions | translate | t |  |  |  |  | RT | 17 |
| `java.lang.StackOverflowError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.StringIndexOutOfBoundsException` | lang-exceptions | translate | t |  |  |  |  | RT | 14 |
| `java.lang.Throwable` | lang-exceptions | shim | 3 | 6 | 4 | 1 | 2 | Agent, ArityException, Compiler, Delay +7 | 304 |
| `java.lang.TypeNotPresentException` | lang-exceptions | translate | t |  |  | 1 |  | RT | 11 |
| `java.lang.UnknownError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.UnsatisfiedLinkError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.UnsupportedClassVersionError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.UnsupportedOperationException` | lang-exceptions | translate | 2 | 2 | 2 | 2 |  | AMapEntry, APersistentMap, APersistentSet, APersistentVector +23 | 16 |
| `java.lang.VerifyError` | lang-exceptions | translate | t |  |  |  |  | RT | 11 |
| `java.lang.VirtualMachineError` | lang-exceptions | translate | t |  |  |  |  | RT | 17 |
| `java.lang.InheritableThreadLocal` | lang-system | shim | t |  |  |  |  | RT | 15 |
| `java.lang.Process` | lang-system | cut | t | 6 |  |  |  | RT | 303 |
| `java.lang.ProcessBuilder` | lang-system | cut | t | 10 |  |  |  | RT | 336 |
| `java.lang.ProcessBuilder$Redirect` | lang-system | cut |  | 5 |  |  |  |  | 336 |
| `java.lang.ProcessHandle` | lang-system | cut | t |  |  |  |  | RT | 41 |
| `java.lang.Runtime` | lang-system | shim | 3 | 3 |  |  | 1 | Agent, Compiler, RT | 364 |
| `java.lang.Runtime$Version` | lang-system | shim | 1 |  |  |  | 1 | Compiler | 364 |
| `java.lang.StackTraceElement` | lang-system | shim | t | 4 | 4 |  |  | RT | 203 |
| `java.lang.StackWalker` | lang-system | cut | t |  |  |  |  | RT | 146 |
| `java.lang.System` | lang-system | shim | 11 | 7 |  | 2 | 1 | ArraySeq, Compile, Compiler, LispReader +7 | 773 |
| `java.lang.Thread` | lang-system | shim | 5 | 8 | 3 |  |  | Agent, DynamicClassLoader, PersistentHashMap, PersistentVector +2 | 790 |
| `java.lang.Thread$Builder$OfVirtual` | lang-system | shim | 2 |  |  |  |  | Agent | 790 |
| `java.lang.Thread$State` | lang-system | shim | t |  |  |  |  | RT | 790 |
| `java.lang.Thread$UncaughtExceptionHandler` | lang-system | shim | t |  |  |  |  | RT | 790 |
| `java.lang.ThreadLocal` | lang-system | shim | 5 | 9 |  |  |  | Agent, LockingTransaction, RT, Var | 371 |
| `java.lang.Class` | class | shim | 23 | 19 | 7 | 9 | 27 | AFn, Compiler, DynamicClassLoader, EnumerationSeq +14 | 1,482 |
| `java.lang.ClassLoader` | class | rework | 11 | 4 | 1 | 2 | 3 | Compiler, DynamicClassLoader, FnLoaderThunk, RT | 846 |
| `java.lang.ClassValue` | class | shim | t |  |  |  |  | RT | 362 |
| `java.lang.Module` | class | cut |  | 1 |  |  | 1 |  | 911 |
| `java.lang.ModuleLayer` | class | cut | t |  |  |  | 2 | RT | 303 |
| `java.lang.Package` | class | cut | t |  |  |  | 1 | RT | 215 |
| `java.lang.reflect.Array` | reflect | shim | 4 | 12 |  |  |  | ArrayIter, ArraySeq, RT | 59 |
| `java.lang.reflect.Constructor` | reflect | shim | 2 | 2 | 5 | 1 | 3 | Compiler, FnLoaderThunk, LispReader, Reflector +1 | 328 |
| `java.lang.reflect.Executable` | reflect | shim | 6 |  |  |  |  | Compiler | 369 |
| `java.lang.reflect.Field` | reflect | shim | 5 | 3 | 4 |  | 3 | Compiler, Reflector, ReflectorCallSite | 683 |
| `java.lang.reflect.GenericArrayType` | reflect | cut |  |  |  |  | 1 |  | 4 |
| `java.lang.reflect.InvocationHandler` | reflect | shim | 1 |  |  |  |  | ProxyHandler, Reflector | 15 |
| `java.lang.reflect.Method` | reflect | shim | 11 | 7 | 6 | 3 | 8 | Compiler, ProxyHandler, Reflector, ReflectorCallSite | 342 |
| `java.lang.reflect.Modifier` | reflect | translate | 5 | 6 |  |  | 4 | Compiler, Reflector | 120 |
| `java.lang.reflect.ParameterizedType` | reflect | cut |  |  |  |  | 2 |  | 6 |
| `java.lang.reflect.Proxy` | reflect | shim | 1 |  |  |  |  | Reflector | 535 |
| `java.lang.reflect.RecordComponent` | reflect | cut |  |  |  |  | 2 |  | 102 |
| `java.lang.reflect.TypeVariable` | reflect | cut |  |  |  |  | 2 |  | 7 |
| `java.lang.reflect.WildcardType` | reflect | cut |  |  |  |  | 2 |  | 5 |
| `java.lang.invoke.CallSite` | invoke | rework | 1 |  |  |  |  | KeywordInvokeSite, ReflectorCallSite | 127 |
| `java.lang.invoke.LambdaMetafactory` | invoke | c2g | 1 |  |  |  |  | Compiler, Reflector | 115 |
| `java.lang.invoke.MethodHandle` | invoke | rework | 7 |  |  |  |  | KeywordInvokeSite, Reflector, ReflectorCallSite | 411 |
| `java.lang.invoke.MethodHandles` | invoke | rework | 6 |  |  |  |  | KeywordInvokeSite, Reflector, ReflectorCallSite | 2,082 |
| `java.lang.invoke.MethodHandles$Lookup` | invoke | rework | 5 |  |  |  |  | KeywordInvokeSite, Reflector, ReflectorCallSite | 2,082 |
| `java.lang.invoke.MethodType` | invoke | rework | 11 |  |  |  |  | Compiler, KeywordInvokeSite, Reflector, ReflectorCallSite | 625 |
| `java.lang.invoke.MutableCallSite` | invoke | rework | 4 |  |  |  |  | KeywordInvokeSite, ReflectorCallSite | 28 |
| `java.lang.invoke.StringConcatFactory` | invoke | c2g | 1 | 1 | 1 | 1 | 1 | ArityException, Compile, Compiler, EdnReader +20 | 733 |
| `java.lang.ref.Reference` | ref | shim | 1 |  |  |  |  | DynamicClassLoader, Keyword, Util | 146 |
| `java.lang.ref.ReferenceQueue` | ref | shim | 2 |  |  |  |  | DynamicClassLoader, Keyword, Util | 120 |
| `java.lang.ref.SoftReference` | ref | shim | 1 |  |  |  |  | DynamicClassLoader | 19 |
| `java.lang.ref.WeakReference` | ref | shim | 1 |  |  |  |  | Keyword | 9 |
| `java.lang.RuntimePermission` | lang-misc | cut | t |  |  |  |  | RT | 15 |
| `java.lang.SuppressWarnings` | lang-misc | cut | t |  |  |  |  | RT | 7 |
| `java.lang.ThreadDeath` | lang-misc | cut | t |  |  |  |  | RT | 7 |
| `java.lang.ThreadGroup` | lang-misc | cut | t |  |  |  |  | RT | 268 |
| `java.util.regex.Matcher` | regex | translate | 7 | 12 |  |  |  | Compiler, EdnReader, LispReader, RT | 627 |
| `java.util.regex.Pattern` | regex | translate | 3 | 5 | 1 | 1 | 1 | Compiler, EdnReader, LispReader, RT | 3,668 |
| `java.util.function.BiConsumer` | functional | translate |  | t |  |  |  |  | 13 |
| `java.util.function.BiFunction` | functional | translate |  | t |  |  |  |  | 10 |
| `java.util.function.BooleanSupplier` | functional | translate | 1 | 1 |  |  |  | IDeref | 5 |
| `java.util.function.Consumer` | functional | translate | 1 | t |  |  |  | APersistentVector, PersistentVector | 10 |
| `java.util.function.DoubleSupplier` | functional | translate | 1 | 1 |  |  |  | IDeref | 5 |
| `java.util.function.Function` | functional | translate | t | t |  |  |  | Compiler, Reflector | 17 |
| `java.util.function.IntSupplier` | functional | translate | 1 | 1 |  |  |  | IDeref | 5 |
| `java.util.function.LongSupplier` | functional | translate | 1 | 1 |  |  |  | IDeref | 5 |
| `java.util.function.Predicate` | functional | translate | t |  |  |  |  | Compiler, Reflector | 27 |
| `java.util.function.Supplier` | functional | translate | 1 | 1 |  |  |  | IDeref, Var | 5 |
| `java.util.stream.BaseStream` | stream | cut |  | 1 |  |  |  |  | 15 |
| `java.util.stream.Collectors` | stream | cut | 1 |  |  |  |  | Compiler, Reflector | 687 |
| `java.util.stream.Stream` | stream | cut | 3 |  |  |  |  | Compiler, Reflector | 217 |
| `java.util.concurrent.atomic.AtomicBoolean` | atomics-locks | shim | 3 |  |  |  |  | Var | 95 |
| `java.util.concurrent.atomic.AtomicInteger` | atomics-locks | shim | 7 | 4 |  |  |  | LockingTransaction, RT, Ref | 150 |
| `java.util.concurrent.atomic.AtomicLong` | atomics-locks | shim | 4 |  |  |  |  | Agent, LockingTransaction, Ref | 150 |
| `java.util.concurrent.atomic.AtomicReference` | atomics-locks | shim | 5 |  |  |  |  | Agent, Atom, Namespace, PersistentHashMap +1 | 128 |
| `java.util.concurrent.locks.Lock` | atomics-locks | shim | 2 |  |  |  |  | Delay, LazySeq | 10 |
| `java.util.concurrent.locks.ReentrantLock` | atomics-locks | shim | 1 | 3 |  |  |  | Delay, LazySeq | 208 |
| `java.util.concurrent.locks.ReentrantReadWriteLock` | atomics-locks | shim | 3 |  |  |  |  | LockingTransaction, MultiFn, Ref | 431 |
| `java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock` | atomics-locks | shim | 2 |  |  |  |  | LockingTransaction, MultiFn, Ref | 431 |
| `java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock` | atomics-locks | shim | 3 |  |  |  |  | LockingTransaction, MultiFn, Ref | 431 |
| `java.util.concurrent.ArrayBlockingQueue` | concurrency | translate |  | 3 |  |  |  |  | 899 |
| `java.util.concurrent.BlockingQueue` | concurrency | translate |  | 2 |  |  |  |  | 18 |
| `java.util.concurrent.Callable` | concurrency | translate | 1 | 1 | t |  | t | AFn, Compiler, IFn, Keyword +5 | 5 |
| `java.util.concurrent.ConcurrentHashMap` | concurrency | translate | 8 |  |  |  |  | DynamicClassLoader, Keyword, Namespace, Util | 4,319 |
| `java.util.concurrent.ConcurrentMap` | concurrency | translate | 4 |  |  |  |  | TransactionalHashMap | 114 |
| `java.util.concurrent.CountDownLatch` | concurrency | shim | 3 | 5 |  |  |  | LockingTransaction | 47 |
| `java.util.concurrent.Executor` | concurrency | shim | 1 | t |  |  |  | Agent | 4 |
| `java.util.concurrent.ExecutorService` | concurrency | shim | 1 | 1 |  |  |  | Agent | 45 |
| `java.util.concurrent.Executors` | concurrency | shim | 3 | 1 |  |  |  | Agent | 341 |
| `java.util.concurrent.ForkJoinPool` | concurrency | cut |  |  | 2 |  |  |  | 2,029 |
| `java.util.concurrent.ForkJoinTask` | concurrency | cut |  |  | 4 |  |  |  | 941 |
| `java.util.concurrent.Future` | concurrency | shim |  | 5 |  |  |  |  | 77 |
| `java.util.concurrent.LinkedBlockingQueue` | concurrency | translate |  | 1 |  |  |  |  | 675 |
| `java.util.concurrent.ThreadFactory` | concurrency | shim | 1 | 1 |  |  |  | Agent | 4 |
| `java.util.concurrent.TimeUnit` | concurrency | translate | 1 | 1 |  |  |  | LockingTransaction | 197 |
| `java.util.concurrent.TimeoutException` | concurrency | translate |  | t |  |  |  |  | 8 |
| `java.util.AbstractCollection` | collections | translate | 4 |  |  |  |  | APersistentMap, TransactionalHashMap | 154 |
| `java.util.AbstractMap` | collections | translate | 10 |  |  |  |  | TransactionalHashMap | 332 |
| `java.util.AbstractSet` | collections | translate | 2 |  |  |  |  | APersistentMap, TransactionalHashMap | 45 |
| `java.util.ArrayDeque` | collections | translate | 1 |  |  |  |  | Reflector | 605 |
| `java.util.ArrayList` | collections | translate | 12 | 8 | 1 | 1 |  | ASeq, Compiler, EdnReader, LazySeq +9 | 1,062 |
| `java.util.Arrays` | collections | translate | 7 | 2 | 1 | 3 | 1 | Compiler, PersistentArrayMap, RT, Reflector +1 | 2,306 |
| `java.util.Collection` | collections | translate | 16 | 15 | 1 |  |  | APersistentMap, APersistentSet, APersistentVector, ASeq +18 | 47 |
| `java.util.Collections` | collections | translate | 2 | 1 |  |  |  | ASeq, PersistentList, TransactionalHashMap | 3,387 |
| `java.util.Comparator` | collections | translate | 1 | 1 |  |  |  | AFunction, Compiler, PersistentTreeMap, PersistentTreeSet +2 | 92 |
| `java.util.Deque` | collections | translate | 2 |  |  |  |  | Reflector | 34 |
| `java.util.Enumeration` | collections | translate | 2 | t |  |  |  | EnumerationSeq, SeqEnumeration | 15 |
| `java.util.HashMap` | collections | translate | 7 |  |  | 2 |  | Compiler, DynamicClassLoader, LockingTransaction | 1,704 |
| `java.util.HashSet` | collections | translate | 6 |  |  |  | 3 | Compiler, LockingTransaction, Reflector | 119 |
| `java.util.IdentityHashMap` | collections | translate | 3 |  |  |  | 4 | Compiler | 938 |
| `java.util.Iterator` | collections | translate | 3 | 3 | t |  |  | APersistentMap, APersistentSet, APersistentVector, ASeq +27 | 14 |
| `java.util.LinkedHashSet` | collections | translate |  |  |  |  | 1 |  | 69 |
| `java.util.LinkedList` | collections | translate | 2 |  |  |  |  | Compiler, LispReader, PersistentList, TransformerIterator | 808 |
| `java.util.List` | collections | translate | 27 | 27 |  | 4 |  | APersistentMap, APersistentVector, ASeq, ArraySeq +19 | 160 |
| `java.util.ListIterator` | collections | translate | 9 | 9 |  |  |  | APersistentVector, ASeq, LazySeq, PersistentList | 12 |
| `java.util.Map` | collections | translate | 18 | 25 | 14 | 1 |  | APersistentMap, Compiler, MethodImplCache, PersistentArrayMap +6 | 277 |
| `java.util.Map$Entry` | collections | translate | 3 | 2 |  |  |  | AMapEntry, APersistentMap, ARef, ATransientMap +14 | 277 |
| `java.util.NoSuchElementException` | collections | translate | 1 | 1 |  |  |  | APersistentVector, ArrayIter, LongRange, PersistentArrayMap +10 | 17 |
| `java.util.Objects` | collections | translate | 2 |  |  |  |  | AFunction, APersistentMap, APersistentSet, APersistentVector +12 | 96 |
| `java.util.Optional` | collections | translate |  |  |  |  | 2 |  | 133 |
| `java.util.Queue` | collections | translate | 4 |  |  |  |  | TransformerIterator | 9 |
| `java.util.RandomAccess` | collections | translate | t | t |  |  |  | APersistentVector, RT | 3 |
| `java.util.SequencedCollection` | collections | translate |  | 1 |  |  |  |  | 28 |
| `java.util.Set` | collections | translate | 15 | t | t |  |  | APersistentMap, APersistentSet, Compiler, LockingTransaction +8 | 87 |
| `java.util.SortedMap` | collections | translate | 6 |  |  |  |  | Compiler | 21 |
| `java.util.Spliterator` | collections | translate | 6 | t |  |  |  | APersistentVector, PersistentVector | 145 |
| `java.util.Stack` | collections | translate | 4 |  |  |  |  | PersistentTreeMap | 34 |
| `java.util.TreeMap` | collections | translate | 6 |  |  |  |  | Compiler, LockingTransaction | 2,191 |
| `java.util.TreeSet` | collections | translate | 1 |  |  |  |  | Compiler | 156 |
| `java.util.WeakHashMap` | collections | translate |  |  |  |  | 1 |  | 793 |
| `java.util.Calendar` | util-misc | cut |  | 4 |  |  |  |  | 1,191 |
| `java.util.Date` | util-misc | shim |  | 1 |  |  |  |  | 547 |
| `java.util.EmptyStackException` | util-misc | translate | t |  |  |  |  | PersistentTreeMap | 7 |
| `java.util.EventListener` | util-misc | cut |  |  | t |  |  |  | 3 |
| `java.util.GregorianCalendar` | util-misc | cut |  | 2 |  |  |  |  | 1,724 |
| `java.util.Properties` | util-misc | translate | 1 | 1 |  |  |  | Compiler | 746 |
| `java.util.TimeZone` | util-misc | cut |  | 1 |  |  |  |  | 307 |
| `java.util.UUID` | util-misc | translate |  | 2 |  |  |  |  | 233 |
| `java.math.BigDecimal` | math | translate | 31 | 5 |  |  | 3 | BigInt, EdnReader, LispReader, Numbers +2 | 2,896 |
| `java.math.BigInteger` | math | translate | 27 | 1 |  |  |  | BigInt, EdnReader, LispReader, Numbers +3 | 2,548 |
| `java.math.MathContext` | math | translate | 2 |  |  |  |  | Numbers, Ratio | 83 |
| `java.io.BufferedInputStream` | io | translate |  | 1 |  |  |  |  | 215 |
| `java.io.BufferedOutputStream` | io | translate |  | 1 |  |  |  |  | 78 |
| `java.io.BufferedReader` | io | translate |  | 3 |  |  |  |  | 303 |
| `java.io.BufferedWriter` | io | translate |  | 2 |  |  |  |  | 135 |
| `java.io.ByteArrayInputStream` | io | translate |  | 1 |  |  |  |  | 108 |
| `java.io.ByteArrayOutputStream` | io | translate |  | 3 |  | 5 |  |  | 74 |
| `java.io.CharArrayReader` | io | translate |  | 1 |  |  |  |  | 113 |
| `java.io.Closeable` | io | translate |  | 1 |  |  |  |  | 5 |
| `java.io.DataInputStream` | io | translate |  |  |  | 4 |  |  | 212 |
| `java.io.File` | io | shim | 7 | 15 |  |  | 4 | Compiler, RT | 562 |
| `java.io.FileInputStream` | io | shim | 2 | 3 |  |  |  | Compiler | 258 |
| `java.io.FileNotFoundException` | io | translate | 1 |  |  |  |  | RT | 16 |
| `java.io.FileOutputStream` | io | shim | 4 | 4 |  |  |  | Compiler | 156 |
| `java.io.FileWriter` | io | shim |  | 1 |  |  |  |  | 31 |
| `java.io.FilterReader` | io | translate | 2 | 8 |  |  |  | LineNumberingPushbackReader | 32 |
| `java.io.Flushable` | io | translate |  | 1 |  |  |  |  | 5 |
| `java.io.IOException` | io | translate | 1 | 1 |  | 1 |  | Compile, Compiler, EdnReader, LispReader +1 | 17 |
| `java.io.InputStream` | io | translate | 1 | 2 | 1 | 3 | 1 | RT | 238 |
| `java.io.InputStreamReader` | io | shim | 2 | 3 |  |  | 1 | Compiler, RT | 55 |
| `java.io.LineNumberReader` | io | translate | 5 | 2 |  |  |  | LineNumberingPushbackReader | 145 |
| `java.io.NotSerializableException` | io | translate | 2 | 1 | 1 |  |  | EnumerationSeq, FnLoaderThunk, IteratorSeq | 11 |
| `java.io.ObjectInputStream` | io | cut | t | t | t |  |  | FnLoaderThunk, Iterate | 2,153 |
| `java.io.ObjectOutputStream` | io | cut | 1 | t | t |  |  | EnumerationSeq, FnLoaderThunk, Iterate, IteratorSeq +1 | 1,162 |
| `java.io.OutputStream` | io | translate |  | 2 |  |  | 2 |  | 42 |
| `java.io.OutputStreamWriter` | io | shim | 3 | 5 |  |  |  | Compile, RT | 72 |
| `java.io.PrintStream` | io | shim | 1 |  |  |  |  | LispReader | 418 |
| `java.io.PrintWriter` | io | translate | 4 | 1 |  |  |  | Compile, Compiler, Namespace, RT | 304 |
| `java.io.PushbackReader` | io | translate | 3 | 22 |  |  | 1 | EdnReader, LineNumberingPushbackReader, LispReader, RT | 119 |
| `java.io.Reader` | io | translate | 3 | 13 |  |  | t | Compiler, EdnReader, LineNumberingPushbackReader, LispReader +1 | 294 |
| `java.io.Serializable` | io | translate | t |  | t |  |  | AFunction, APersistentMap, APersistentSet, APersistentVector +15 | 3 |
| `java.io.StringReader` | io | translate | 1 | 1 |  |  |  | EdnReader, RT | 50 |
| `java.io.StringWriter` | io | translate | 2 | 3 | 1 |  |  | RT | 54 |
| `java.io.Writer` | io | translate | 2 | 19 |  |  |  | RT | 116 |
| `java.nio.CharBuffer` | nio | cut |  | t |  |  |  |  | 472 |
| `java.nio.channels.FileChannel` | nio | cut |  | 2 |  |  |  |  | 102 |
| `java.nio.channels.WritableByteChannel` | nio | cut |  | t |  |  |  |  | 8 |
| `java.nio.channels.spi.AbstractInterruptibleChannel` | nio | cut |  | 1 |  |  |  |  | 75 |
| `java.nio.charset.Charset` | nio | shim | 1 | 2 |  |  |  | RT | 294 |
| `java.nio.file.Files` | nio | cut |  | 1 |  |  | 1 |  | 852 |
| `java.nio.file.Path` | nio | cut |  | 1 |  |  | t |  | 122 |
| `java.nio.file.attribute.FileAttribute` | nio | cut |  | t |  |  |  |  | 5 |
| `java.net.InetAddress` | net | cut |  | 1 |  |  |  |  | 873 |
| `java.net.JarURLConnection` | net | cut | 1 |  |  |  |  | RT | 66 |
| `java.net.MalformedURLException` | net | cut | 2 | t |  |  |  | RT | 11 |
| `java.net.ServerSocket` | net | cut |  | 4 |  |  |  |  | 326 |
| `java.net.Socket` | net | cut |  | 4 |  |  |  |  | 682 |
| `java.net.SocketException` | net | cut |  | t |  |  |  |  | 17 |
| `java.net.URI` | net | cut | 2 | 3 |  |  |  | RT | 1,588 |
| `java.net.URISyntaxException` | net | cut | t |  |  |  |  | RT | 41 |
| `java.net.URL` | net | cut | 2 | 4 | t |  | 1 | DynamicClassLoader, RT | 728 |
| `java.net.URLClassLoader` | net | cut | 4 |  |  |  |  | DynamicClassLoader | 290 |
| `java.net.URLConnection` | net | cut | 2 |  |  |  |  | RT | 622 |
| `java.net.URLDecoder` | net | cut |  | 1 |  |  |  |  | 73 |
| `java.net.URLEncoder` | net | cut |  | 1 |  |  |  |  | 129 |
| `java.text.DateFormat` | text | cut |  | 2 |  |  |  |  | 323 |
| `java.text.SimpleDateFormat` | text | cut |  | 1 |  |  |  |  | 1,404 |
| `java.sql.ResultSet` | time | cut |  | 3 |  |  |  |  | 272 |
| `java.sql.ResultSetMetaData` | time | cut |  | 2 |  |  |  |  | 27 |
| `java.sql.Timestamp` | time | cut |  | 3 |  |  |  |  | 244 |
| `java.time.Instant` | time | cut |  | 1 |  |  |  |  | 403 |
| `java.awt.BorderLayout` | desktop | cut |  |  | 3 |  |  |  | 336 |
| `java.awt.Component` | desktop | cut |  |  | t |  |  |  | 4,517 |
| `java.awt.Container` | desktop | cut |  |  | 2 |  |  |  | 2,512 |
| `java.awt.LayoutManager` | desktop | cut |  |  | t |  |  |  | 8 |
| `java.awt.Window` | desktop | cut |  |  | 3 |  |  |  | 1,581 |
| `javax.swing.JButton` | desktop | cut |  |  | 1 |  |  |  | 100 |
| `javax.swing.JEditorPane` | desktop | cut |  |  | 2 |  |  |  | 1,275 |
| `javax.swing.JFrame` | desktop | cut |  |  | 3 |  |  |  | 295 |
| `javax.swing.JPanel` | desktop | cut |  |  | 1 |  |  |  | 77 |
| `javax.swing.JScrollPane` | desktop | cut |  |  | 1 |  |  |  | 581 |
| `javax.swing.JTable` | desktop | cut |  |  | 3 |  |  |  | 4,695 |
| `javax.swing.JToolBar` | desktop | cut |  |  | 2 |  |  |  | 399 |
| `javax.swing.event.HyperlinkEvent` | desktop | cut |  |  | 2 |  |  |  | 63 |
| `javax.swing.event.HyperlinkEvent$EventType` | desktop | cut |  |  | 1 |  |  |  | 63 |
| `javax.swing.event.HyperlinkListener` | desktop | cut |  |  | 1 |  |  |  | 5 |
| `javax.swing.event.TableModelEvent` | desktop | cut |  |  | t |  |  |  | 39 |
| `javax.swing.event.TableModelListener` | desktop | cut |  |  | t |  |  |  | 6 |
| `javax.swing.event.TreeModelListener` | desktop | cut |  |  | t |  |  |  | 8 |
| `javax.swing.table.AbstractTableModel` | desktop | cut |  |  | 21 |  |  |  | 75 |
| `javax.swing.table.TableModel` | desktop | cut |  |  | 9 |  |  |  | 15 |
| `javax.swing.text.JTextComponent` | desktop | cut |  |  | 2 |  |  |  | 2,502 |
| `javax.swing.text.html.HTMLFrameHyperlinkEvent` | desktop | cut |  |  | t |  |  |  | 38 |
| `javax.swing.tree.TreeModel` | desktop | cut |  |  | 8 |  |  |  | 13 |
| `javax.swing.tree.TreePath` | desktop | cut |  |  | t |  |  |  | 127 |
| `javax.xml.parsers.SAXParser` | xml | cut |  |  | 1 |  |  |  | 151 |
| `javax.xml.parsers.SAXParserFactory` | xml | cut |  |  | 2 |  |  |  | 95 |
| `org.xml.sax.Attributes` | xml | cut | t |  | 3 |  |  | XMLHandler | 16 |
| `org.xml.sax.ContentHandler` | xml | cut | 11 |  | 12 |  |  | XMLHandler | 31 |
| `org.xml.sax.Locator` | xml | cut | t |  | t |  |  | XMLHandler | 7 |
| `org.xml.sax.XMLReader` | xml | cut |  |  | 1 |  |  |  | 25 |
| `org.xml.sax.helpers.DefaultHandler` | xml | cut | 12 |  |  |  |  | XMLHandler | 104 |
| `sun.misc.Signal` | jdk-internal | shim |  | 2 |  |  |  |  | 89 |
| `sun.misc.SignalHandler` | jdk-internal | shim |  | 1 |  |  |  |  | 8 |
| `java.beans.BeanInfo` | other | cut |  | 1 |  |  |  |  | 16 |
| `java.beans.FeatureDescriptor` | other | cut |  | 1 |  |  |  |  | 193 |
| `java.beans.Introspector` | other | cut |  | 1 |  |  |  |  | 869 |
| `java.beans.PropertyDescriptor` | other | cut |  | 2 |  |  |  |  | 460 |
| `java.lang.annotation.Annotation` | other | cut | t |  |  |  |  | WarnBoxedMath | 7 |
| `java.lang.annotation.Retention` | other | cut |  | 1 |  |  | 1 |  | 7 |
| `java.lang.annotation.RetentionPolicy` | other | cut |  | 1 |  |  |  |  | 6 |
| `java.lang.annotation.Target` | other | cut |  |  |  |  | 1 |  | 7 |
| `java.lang.module.ModuleDescriptor` | other | cut |  |  |  |  | 1 |  | 1,156 |
| `java.util.jar.JarFile` | other | cut | 1 |  |  |  |  | RT | 545 |
| `java.util.zip.ZipEntry` | other | cut | 1 |  |  |  |  | RT | 291 |
