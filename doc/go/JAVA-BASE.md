# java.base in the Go build

How much of the JDK's `java.base` module the Go build of Arbace implements (B1a, before the
`arbace-for-golang` freeze; [B1-PLAN.md](B1-PLAN.md)). jrt is the Go build's Java runtime:
jdk26u sources translated by `bin/jrt-convert` (j2c) and c2g, jrt's own Java in `overlay/`
translated the same way, and hand-written Go forms in `go/arbace/jrt/`; c2g stubs what it does
not translate ([JRT-SOURCES.md](JRT-SOURCES.md), [JRT-NOTES.md](JRT-NOTES.md),
[C2G-SPEC.md](C2G-SPEC.md) §4.1).
Measured on the branch `jbase` from main at `6a33be8` (2026-10-10): c2g's program output made
there (`bin/jrt-convert`, then `bin/c2g --program`, as `bin/arbace-go --build` does), JDK
26.0.2.1 built from `/root/jdk26u`.

Summary: java.base exports **1,636 API classes with 17,546 members** (public and protected
methods, constructors and fields, of its 58 exported packages). The Go build has **384 of the
classes (23.5%)**, and 91 more as names only, and provides **5,598 of the members (31.9%)**:
4,414 translated with their bodies, 108 translated with an operation that throws (it names
something outside the build), 1,076 hand-written in jrt; 15 more exist as stubs that throw.
The classes the Go build has are nearly complete (90.5% of their members); the rest is whole
packages left out: `java.lang.classfile`, `java.security` and `javax.*`, `java.time` but
`Instant`, `java.text`, `java.nio` buffers and channels, `java.lang.invoke`,
`java.lang.foreign`, most of `java.net` and of `java.util.concurrent`. Of the **896 members
Arbace's runtime and REPL namespaces reference, 793 are provided (88.5%)**; the others are
D6's cuts and reworks (processes, sockets, method handles, class loading). Two gaps found on
the way are worth closing before the freeze: primitive `Arrays.sort` and
`Comparator.naturalOrder` throw on Go ("Notable gaps").

## Method

`bin/jrt-coverage [OUT]` (bash) with its Clojure in `test/g2c/jrt_coverage.clj`
(`g2c.jrt-coverage`, next to `g2c.java-surface`). It runs on Arbace's `target/stage2`
(`bin/lib/tools.bash`), takes about 15 s, and writes:

- `doc/go/java-base-coverage.edn` (OUT; Clojure reader): totals, Arbace's use, per package
  family, per package, and per class its status and its members' counts by status;
- `.tmp/jrt-coverage/members.edn`: every class and member with its status;
- `.tmp/jrt-coverage/summary.md`: the tables of this document.

Inputs: c2g's output for the program (`C2G_OUT`, default `target/arbace-go/c2g`, written by
`bin/arbace-go --build`, or `bin/c2g --program --out DIR` after `bin/jrt-convert`), jrt's forms
(`go/arbace/jrt/`, its `manifest.edn`), `bin/jrt-convert`'s `sources.txt` (`JRT_WORK`, default
`.tmp/jrt`) and Arbace's use (`SURFACE`, default `doc/go/java-surface.edn`, from
`bin/java-surface`). Deterministic: the output depends only on these and the JDK.

**The API** is read by reflection on the running JDK 26 (26.0.2.1, built from `/root/jdk26u`,
the sources jrt translates): the packages `java.base` exports to every module
(`ModuleDescriptor.exports`, unqualified), their class files in the module image (`jrt:/`), of
those the public classes and the public or protected member classes of accessible classes (not
synthetic, anonymous or local), and of each class its *declared* public and protected methods,
constructors and fields (protected ones only in non-final classes), without synthetic members
and bridges. Members are named as the JVM names them, name and descriptor, which is how c2g
names them too (its stubs, its report, its Go names, §4.4). Reflection rather than jdk26u's
sources: it gives the compiled API exactly (implicit members such as an enum's `values`,
generated classes, no doc-comment conventions to apply) in the descriptors c2g uses; the jmods
hold the same class files. Inherited members are counted once, on the declaring class.

**The Go build**, per class:

| status | meaning |
|---|---|
| translated | c2g translated it (report's `:translated`) from jdk26u's source, or from jrt's own Java (`overlay/jdk`, KIND `overlay` in `sources.txt`) |
| declared | c2g declared it from reflection (a JDK marker interface without source) |
| hand-written | jrt defines it (a `Define` registration in `go/arbace/jrt/`, or the manifest) |
| cut | a name only: c2g registers it for the evaluator (`c2g_cut.go`), its members throw `UnsupportedOperationException` (D6; C2G-SPEC §4.1, amendment M3) |
| absent | not in the Go build |

and per member:

| status | meaning |
|---|---|
| translated | translated with its body (abstract: declared in the Go interface) |
| partial | translated, but an operation in it names a class or member outside the build and throws when reached (an operation-level stub; c2g's report, `:unavailable`) |
| hand-written | jrt implements it: in the manifest, or its Go name (§4.4) defined in the program's jrt package, or in a member table of jrt's classes (`reflect_tables`, `reflect_tables_c2g`, `c2g_support`) |
| stub | exists in Go and throws `c2g: not translated` (not reached from the program's roots, or its translation failed) |
| outside type | not in Go: its descriptor names a class outside the build (C2G-SPEC §4.1) |
| cut | a member of a cut class |
| absent | not in the build: of an absent class, a member jrt does not implement, or a member the translated source lacks (jrt's own Java is a subset; a variant cut it) |

"Provided" is translated, partial or hand-written. **Cross-check:** for every member of a
translated class, its Go name (§4.4: the method, `Impl_` method, static function, `_New`
function or `Ctor` method, static variable) is looked up in the program's forms; it must exist
exactly when the status says the member is in Go. 0 mismatches.

**Reachability.** With `--program` c2g roots every public member of every class with class forms
(C2G-SPEC §10.6), so a translated class's public members are all reached and translated; `stub`
catches protected members not reached and failed translations. What Arbace itself reaches is
measured instead from its references: the members the runtime (`arbace.lang`) and the namespaces
loaded at a REPL's start reference (java-surface.edn, groups `:runtime` and `:repl`; regenerated
for this measurement, unchanged).

## Numbers

### Overall

Columns: classes (the API's), in Go (translated, declared or hand-written), cut (names only);
members, provided (translated, partial or hand-written) and their share, then the members by
status ("cut or absent" together).

|  | classes | in Go | cut | members | provided | % | translated | partial | hand-written | stub | outside type | cut or absent |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| **java.base** | 1,636 | 384 | 91 | 17,546 | 5,598 | 31.9% | 4,414 | 108 | 1,076 | 15 | 79 | 11,854 |

The 384 classes in Go have 6,185 API members, of which 5,598 (90.5%) are provided. Of the
translated classes, 10 are jrt's own Java rather than jdk26u's (provided of members):
`FileDescriptor` 6/6, `FileInputStream` 13/14, `FileOutputStream` 10/11, `InputStreamReader`
8/10, `OutputStreamWriter` 11/12, `java.nio.file.Files` 41/70, `Path` 30/33, `Calendar`
110/111, `GregorianCalendar` 33/35, `TimeZone` 24/28.

| kind | members | provided | % |
|---|---:|---:|---:|
| ctor | 1,597 | 522 | 32.7% |
| field | 2,300 | 855 | 37.2% |
| method | 13,649 | 4,221 | 30.9% |

### The big packages

|  | classes | in Go | cut | members | provided | % | translated | partial | hand-written | stub | outside type | cut or absent |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `java.io` | 86 | 41 | 4 | 922 | 466 | 50.5% | 463 | 3 | 0 | 6 | 8 | 442 |
| `java.lang` | 129 | 77 | 32 | 2,226 | 1,738 | 78.1% | 1,117 | 4 | 617 | 3 | 4 | 481 |
| `java.lang.invoke` | 25 | 0 | 2 | 331 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 331 |
| `java.lang.ref` | 7 | 4 | 0 | 22 | 14 | 63.6% | 0 | 0 | 14 | 0 | 0 | 8 |
| `java.lang.reflect` | 34 | 12 | 0 | 347 | 119 | 34.3% | 37 | 0 | 82 | 0 | 0 | 228 |
| `java.math` | 4 | 4 | 0 | 177 | 177 | 100.0% | 176 | 1 | 0 | 0 | 0 | 0 |
| `java.net` | 66 | 11 | 10 | 760 | 150 | 19.7% | 148 | 2 | 0 | 0 | 5 | 605 |
| `java.nio` | 14 | 0 | 2 | 365 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 365 |
| `java.nio.charset` | 12 | 4 | 0 | 106 | 24 | 22.6% | 4 | 0 | 20 | 0 | 0 | 82 |
| `java.nio.file` | 47 | 15 | 0 | 252 | 114 | 45.2% | 114 | 0 | 0 | 0 | 20 | 118 |
| `java.security` | 98 | 0 | 2 | 663 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 663 |
| `java.text` | 35 | 1 | 8 | 559 | 22 | 3.9% | 0 | 0 | 22 | 0 | 0 | 537 |
| `java.time` | 19 | 2 | 8 | 794 | 25 | 3.1% | 24 | 1 | 0 | 0 | 21 | 748 |
| `java.util` | 134 | 96 | 1 | 2,239 | 1,731 | 77.3% | 1,615 | 65 | 51 | 1 | 15 | 492 |
| `java.util.concurrent` | 81 | 29 | 0 | 1,199 | 324 | 27.0% | 223 | 4 | 97 | 5 | 4 | 866 |
| `java.util.concurrent.atomic` | 16 | 4 | 0 | 319 | 108 | 33.9% | 0 | 0 | 108 | 0 | 0 | 211 |
| `java.util.concurrent.locks` | 14 | 8 | 0 | 201 | 65 | 32.3% | 0 | 0 | 65 | 0 | 0 | 136 |
| `java.util.function` | 43 | 43 | 0 | 79 | 79 | 100.0% | 79 | 0 | 0 | 0 | 0 | 0 |
| `java.util.regex` | 4 | 3 | 1 | 82 | 69 | 84.1% | 69 | 0 | 0 | 0 | 1 | 12 |
| `java.util.stream` | 21 | 21 | 0 | 300 | 300 | 100.0% | 300 | 0 | 0 | 0 | 0 | 0 |

### By package family

Each package with its subpackages, except those counted as families of their own
(`java.util.concurrent` with `atomic` and `locks`, `java.util.regex`, `java.util.stream`,
`java.util.function`; `java.util` keeps `jar`, `zip`, `spi` and `random`). Every package is in `doc/go/java-base-coverage.edn` and `.tmp/jrt-coverage/summary.md`.

|  | classes | in Go | cut | members | provided | % | translated | partial | hand-written | stub | outside type | cut or absent |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `java.util` | 180 | 102 | 6 | 2,695 | 1,801 | 66.8% | 1,657 | 93 | 51 | 1 | 15 | 878 |
| `java.lang` | 129 | 77 | 32 | 2,226 | 1,738 | 78.1% | 1,117 | 4 | 617 | 3 | 4 | 481 |
| `java.util.concurrent` | 111 | 41 | 0 | 1,719 | 497 | 28.9% | 223 | 4 | 270 | 5 | 4 | 1,213 |
| `java.lang.classfile` | 220 | 0 | 0 | 1,628 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 1,628 |
| `java.time` | 70 | 2 | 17 | 1,565 | 25 | 1.6% | 24 | 1 | 0 | 0 | 21 | 1,519 |
| `java.security` | 206 | 0 | 3 | 1,307 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 1,307 |
| `java.io` | 86 | 41 | 4 | 922 | 466 | 50.5% | 463 | 3 | 0 | 6 | 8 | 442 |
| `java.net` | 71 | 11 | 10 | 774 | 150 | 19.4% | 148 | 2 | 0 | 0 | 5 | 619 |
| `java.text` | 41 | 1 | 9 | 580 | 22 | 3.8% | 0 | 0 | 22 | 0 | 0 | 558 |
| `javax.crypto` | 62 | 0 | 0 | 407 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 407 |
| `java.nio.file` | 74 | 16 | 1 | 405 | 116 | 28.6% | 116 | 0 | 0 | 0 | 20 | 269 |
| `java.nio` | 14 | 0 | 2 | 365 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 365 |
| `java.lang.reflect` | 34 | 12 | 0 | 347 | 119 | 34.3% | 37 | 0 | 82 | 0 | 0 | 228 |
| `javax.net` | 45 | 0 | 0 | 338 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 338 |
| `java.lang.invoke` | 25 | 0 | 2 | 331 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 331 |
| `java.nio.channels` | 62 | 0 | 1 | 323 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 323 |
| `java.util.stream` | 21 | 21 | 0 | 300 | 300 | 100.0% | 300 | 0 | 0 | 0 | 0 | 0 |
| `java.lang.foreign` | 25 | 0 | 0 | 261 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 261 |
| `javax.security` | 43 | 0 | 0 | 209 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 209 |
| `java.math` | 4 | 4 | 0 | 177 | 177 | 100.0% | 176 | 1 | 0 | 0 | 0 | 0 |
| `java.lang.constant` | 12 | 2 | 0 | 168 | 1 | 0.6% | 1 | 0 | 0 | 0 | 1 | 166 |
| `java.lang.module` | 19 | 0 | 0 | 148 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 148 |
| `java.nio.charset` | 13 | 4 | 0 | 109 | 24 | 22.0% | 4 | 0 | 20 | 0 | 0 | 85 |
| `java.util.regex` | 4 | 3 | 1 | 82 | 69 | 84.1% | 69 | 0 | 0 | 0 | 1 | 12 |
| `java.util.function` | 43 | 43 | 0 | 79 | 79 | 100.0% | 79 | 0 | 0 | 0 | 0 | 0 |
| `java.lang.annotation` | 12 | 0 | 3 | 35 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 35 |
| `java.lang.runtime` | 3 | 0 | 0 | 24 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 24 |
| `java.lang.ref` | 7 | 4 | 0 | 22 | 14 | 63.6% | 0 | 0 | 14 | 0 | 0 | 8 |

### Arbace's use

The members Arbace's runtime and REPL namespaces reference (java-surface.edn), in java.base's
exported API:

| members used | translated | partial | hand-written | stub | outside type | cut | absent |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 896 | 544 | 0 | 249 | 0 | 3 | 51 | 49 |

793 of 896 (88.5%) are provided. The 103 others are cut or reworked by decision: method
handles and call sites (`java.lang.invoke`, 35: the evaluator replaces `invokedynamic`),
processes (`Process`, `ProcessBuilder`, its `Redirect`, 21), class loading and resources
(`ClassLoader` 7, `URLClassLoader` 3, `JarURLConnection`, `JarFile`, `ZipEntry`), sockets
(`Socket`, `ServerSocket`, `InetAddress`, 9), channels (`FileChannel`, `getChannel`,
`Reader.read(CharBuffer)`, `Readable`, 7), `DateFormat`/`SimpleDateFormat` (3), annotations
(`Retention`, `RetentionPolicy`, `getAnnotation` twice, 4), modules (`Class.getModule`,
`Module.getName`), `Runtime.exec`/`version`/`Version.feature`, `System.getProperties`/
`loadLibrary`, `Thread$Builder.factory`, `URLEncoder.encode`, serialization's
`defaultWriteObject`, and `Executable.getName` (declared abstract there; `Method` and
`Constructor` provide it).

## What is implemented

- **Translated from jdk26u** (plain Java, exact semantics): the boxed numbers, `Character` with
  JDK 26's Unicode tables, the exceptions and errors of `java.lang`; `java.math` whole;
  `java.util`'s collections (lists, sets, maps, deques, `Hashtable`/`Vector`/`Stack`,
  `IdentityHashMap`, `EnumSet`/`EnumMap`, the sequenced and immutable collections),
  `Arrays` (but its primitive sorts), `Collections`, `Objects`, `Optional*`, `Formatter`, `Properties`, `Random`,
  `UUID`, `HexFormat`, `StringJoiner`, `Spliterators`; `java.util.stream` and
  `java.util.function` whole; `java.util.regex` (D5); `java.util.random`'s
  `RandomGenerator`; in `java.util.concurrent`: `ConcurrentHashMap`, `ArrayBlockingQueue`,
  `LinkedBlockingQueue`, `CyclicBarrier`, `CountedCompleter`, `RecursiveTask`, `TimeUnit`;
  `java.io`'s readers, writers and streams, `File` and the Unix file system; `java.net.URI`,
  `URL` with the `file:` connection, `URLDecoder`; `java.time.Instant`;
  `java.nio.file`'s `Paths`, options and exceptions.
- **jrt's own Java** (overlay, translated by c2g): `FileDescriptor`, `FileInputStream`,
  `FileOutputStream`, `InputStreamReader`, `OutputStreamWriter`, `java.nio.file.Path` and
  `Files` (a subset), `Calendar`, `GregorianCalendar`, `TimeZone` (fixed offsets).
- **Hand-written Go**: `Object`, `Class` and `java.lang.reflect` (over c2g's member tables),
  `String`, `StringBuilder`, `StringBuffer` (UTF-16, D4), `Math`, `StrictMath`, `System`,
  `Runtime`, `Thread`, `ThreadLocal`, `Throwable`, `StackTraceElement`, `Enum`, `Record`;
  references; atomics (`AtomicBoolean`, `Integer`, `Long`, `Reference`), locks
  (`ReentrantLock`, `ReentrantReadWriteLock`, `Condition`, `LockSupport`), the executors,
  `ForkJoinPool`/`ForkJoinTask`, `FutureTask`, `CountDownLatch`, `Semaphore`; `Charset`
  (six charsets), `Locale`, `DecimalFormatSymbols`, `Date`.

## Stubbed and partial

**Stubs (15 members).** Every public member of a translated class is a root, so the stubs are
protected members and constructors nothing reaches: `ObjectStreamException`'s two cause
constructors, `PrintStream`'s and `PrintWriter`'s `setError`/`clearError`, the
`(String, Throwable, boolean, boolean)` constructors of `Error`, `Exception` and
`RuntimeException`, `Calendar()`, `CountedCompleter(CountedCompleter, int)`,
`ForkJoinWorkerThread(ForkJoinPool)`, `onStart`, `onTermination`, `TimeUnit.timedWait`. A
subclass the evaluator makes (`proxy`) calling one of them throws.

**Partial (108 members).** Translated, with one operation that names a class outside the build
and throws when that path runs (c2g's report, `:unavailable`, gives each reason):

| members | outside the build | what throws |
|---|---|---|
| `Arrays.sort` of the primitive arrays (14), `parallelSort` (18), `parallelPrefix` (8) | `DualPivotQuicksort`, `ArrayPrefixHelpers` | always (`Arrays.sort` of objects works, and with it Clojure's `sort`) |
| `RandomGenerator`'s bounded and stream defaults (22), the `of(String)` factories (6), `Random`'s 12 stream methods | `RandomSupport`, `RandomGeneratorFactory` | `RandomGenerator`'s defaults (`Random` overrides `nextInt(int)` and the like) and the factories always; `Random.ints`/`longs`/`doubles` always |
| `Comparator.naturalOrder`, `nullsFirst`, `nullsLast`, `Collections.reverseOrder(Comparator)` | `Comparators` | always; `reverseOrder(c)` only to reverse `reverseOrder()` |
| `TreeMap(SortedMap)`, `TreeMap.clone`, `TreeMap.putAll`; `TreeSet.addAll` | `ObjectInputStream` (in `buildFromSorted`'s signature); `SortedSet` | the constructor and `clone` always, `putAll` of a `SortedMap` into an empty map; `addAll` never (`instanceof SortedSet` is false on Go: the slow path, the same result) |
| `describeConstable` of `Boolean`, `Byte`, `Character`, `Short` | `ConstantDescs` | always |
| `BigInteger.nextProbablePrime` | `BitSieve` | for large numbers |
| `DataInputStream.readLine`, `readUTF` | `PushbackInputStream`, `DataInput` | `readLine` on a lone `\r` (`\r` not followed by `\n`), `readUTF` always |
| `UUID.ofEpochMillis`, `List.ofLazy`, `Map.ofLazy`, `Properties.loadFromXML`/`storeToXML`, `Instant.parse`, `ByteArrayInputStream.transferTo`, `URLConnection.getHeaderFieldDate`, `URLStreamHandler.setURL`, `CountedCompleter.helpComplete`, three of `ForkJoinWorkerThread` | `SecureRandom`, `LazyCollections`, XML, `DateTimeFormatter`, ... | when the path runs |

**Outside type (79 members).** Members of translated classes whose descriptors name a class
outside the build do not exist in Go: `Instant`'s 21 `java.time.temporal` members, `Files`' 17
attribute, channel and directory-stream members, `TreeSet`, `NavigableSet` and `Collections`'
11 `SortedSet` members, `getChannel`, the `CharBuffer` and coder overloads of the readers and
writers, `TimeUnit`'s and `TimeZone`'s `java.time` conversions, `resolveConstantDesc`,
`Matcher.toMatchResult` (`MatchResult` is cut), `URL`'s `Proxy` overloads. A further 14 are
absent from jrt's own Java (`Files.list`, `isSymbolicLink`, links, attributes, `mismatch`,
`probeContentType`; `TimeZone.availableIDs`).

## Cut, and why

D6 ([B1-PLAN.md](B1-PLAN.md)) left out of the first REPL what has no Go-side meaning yet;
JAVA-SURFACE.md lists what of Arbace each cut breaks, C2G-SPEC §12 what is out of scope. The
cut classes Arbace names exist as names whose members throw; everything else is absent.

| area | in the Go build | why |
|---|---|---|
| class loading, `java.lang.invoke`, `java.lang.classfile`, `java.lang.constant` (but `Constable`, `ConstantDesc`) | absent or cut | the evaluator replaces bytecode, call sites and method handles (D2); no class files |
| `java.lang.reflect`'s generic and annotated types, annotations at run time | absent | closed world over c2g's member tables (JAVA-SURFACE.md decision 1; amendment R16) |
| processes (`Process`, `ProcessBuilder`), `ProcessHandle` | cut | D6: `sh`, `arbace.java.process` |
| `java.net` sockets, `InetAddress`, `URLClassLoader`, other URL connections | cut or absent | D6 (sockets and the socket REPL are in progress on another branch) |
| `java.nio` buffers, channels, charset coders, most of `java.nio.file` | absent | D6; the file system is reached through `File` and jrt's `Path`/`Files` (FS2) |
| `java.time` (but `Instant`), `java.text` (but `DecimalFormatSymbols`) | cut or absent | D6; `#inst` over `Date`, `Calendar`, `Instant` (JAVA-SURFACE.md decision 7) |
| serialization (`ObjectInputStream`, `ObjectOutputStream`) | absent | D6, C2G-SPEC §12 |
| `java.security`, `javax.crypto`, `javax.net`, `javax.security` | absent | not used by Arbace |
| `java.util.jar`, `java.util.zip`, `ResourceBundle`, `ServiceLoader`, `java.util.spi`, `java.lang.module`, `Module`, `Package` | absent or cut | no jars or modules in a static executable |
| `java.lang.foreign`, `ScopedValue`, `StructuredTaskScope`, `StackWalker` | absent or cut | VM features without a Go counterpart yet |

## Notable gaps

- **Sorting primitive arrays and natural-order comparators throw**:
  `(java.util.Arrays/sort (int-array [3 1 2]))` throws `UnsupportedOperationException ("c2g:
  class java.util.DualPivotQuicksort is not in the closed world")`, likewise every primitive
  `sort`, `parallelSort` and `parallelPrefix`; `(java.util.Comparator/naturalOrder)` throws
  (`Comparators`). Checked on the executable. Both are files outside the closure, stubbed per
  the decision of 2026-10-08 (JRT-SOURCES.md, "Decided"); Clojure's `sort` is unaffected
  (it sorts an `Object[]`). Also checked: `TreeMap`'s copy constructor from a `SortedMap`
  and `clone` throw (`buildFromSorted` names `ObjectInputStream`).
- **`SortedSet` is cut** (its file is not in the closure; `NavigableSet` and `TreeSet` are):
  `(instance? java.util.SortedSet (java.util.TreeSet.))` is false on Go, and `TreeSet`'s
  `headSet`/`subSet`/`tailSet` of one or two arguments, its `SortedSet` constructor and
  `Collections`' four `SortedSet` methods are missing (outside type).
- **`java.util`**: no `BitSet`, `PriorityQueue`, `WeakHashMap`, `StringTokenizer`, `Base64`,
  `Scanner`, `Currency`, `ResourceBundle`, `ServiceLoader`, `Timer`, `SplittableRandom`;
  `Locale` 21 of 75 members (no `Builder`, `forLanguageTag`, display names, ISO lists, extensions, `setDefault`, most locale constants); `Date` 26 of
  35 (no deprecated setters, `parse`, `toLocaleString`).
- **`java.util.concurrent`**: 29 of 81 classes; no `CompletableFuture`,
  `ConcurrentLinkedQueue`/`Deque`, `ConcurrentSkipListMap`/`Set`, `CopyOnWriteArrayList`/`Set`,
  `PriorityBlockingQueue`, `LinkedBlockingDeque`, `SynchronousQueue`, `Phaser`, `Exchanger`,
  `ScheduledThreadPoolExecutor`, `ThreadLocalRandom`'s API (jrt has only its probes); the
  hand-written `ForkJoinPool` has 11 of its 55 members, `ThreadPoolExecutor` 6 of 38,
  `Executors` 8 of 24. Atomics: no arrays, field updaters, adders or accumulators. Locks: no
  AQS, `StampedLock`.
- **`java.lang`**: `Class` 56 of 81 members (no annotations, modules, packages, nest, sealed or
  record reflection, member classes, `getResource*`), `ClassLoader` 10 of 43, `System` 19 of 31 (no `getProperties`,
  `loadLibrary`, `Logger`), `Thread` 43 of 57 (no thread groups, `getState`, `ofPlatform`, the
  `Duration` overloads of `join` and `sleep`), `Runtime` 9 of 20 (no `exec`, `version`).
- **`java.lang.reflect`**: 119 of 347 members; `Field` 12 of 35 (no typed getters and setters),
  `Executable` 7 of 22, no `Parameter`, `RecordComponent`, generic types.
- **`java.io`**: no `DataOutputStream`/`DataOutput`, `RandomAccessFile`, `PushbackInputStream`,
  `CharArrayWriter`, `Piped*`, `Console`, `StreamTokenizer`, serialization.
- **`java.nio.charset`**: no `CharsetEncoder`/`Decoder`; six charsets (R18, S6).

## Going further

What it would take, roughly by cost (agent-assisted days at the pace of B1a):

1. **More plain Java of `java.util`** (cheap, about 1 day): first the files whose absence makes
   translated members throw: `DualPivotQuicksort` (primitive `Arrays.sort`), `Comparators`,
   `SortedSet`, `ArrayPrefixHelpers`, `ArraysParallelSortHelpers`, `RandomSupport`,
   `BitSieve`; then `BitSet`, `PriorityQueue`,
   `WeakHashMap` (over jrt's references), `StringTokenizer`, `Base64`, `SplittableRandom`,
   `Timer`, `Scanner` (needs `CharBuffer`, or a variant): `added-sources` in
   `test/g2c/jrt_sources.clj`, then c2g; no VarHandle, no natives. `SortedSet` is cut (an
   interface with methods outside the closure), which costs `TreeSet`'s and `Collections`'
   `SortedSet` members: adding its file restores them.
2. **`java.util.concurrent` beyond the shims** (2-4 days): JDK 26's concurrent classes use
   `VarHandle` (`ConcurrentLinkedQueue`, `ConcurrentSkipListMap`, `CompletableFuture`,
   `Exchanger`, the atomic arrays and field updaters, `Striped64`/`LongAdder`) or AQS over
   `Unsafe` (`AbstractQueuedSynchronizer`, `StampedLock`). c2g would compile field `VarHandle`s
   (`MethodHandles.lookup().findVarHandle` constants and their access modes) to Go atomics, as
   jrt's `Unsafe` does for `ConcurrentHashMap`; then the classes translate. `ThreadPoolExecutor`,
   `Executors` and `AbstractExecutorService` are plain Java over AQS and could replace the
   hand-written executors (a decision: exact Java semantics against goroutine-native shims).
3. **`java.time`** (2-4 days): 58,000 lines of plain Java; it needs the time-zone database
   (`tzdb.dat` as a resource of the executable, `ZoneRulesProvider`) and `java.text`-free
   formatting paths; `DateTimeFormatter`'s localized text needs locale data.
4. **`java.text` and locales** (3-5 days): `DecimalFormat`, `SimpleDateFormat`,
   `MessageFormat`, `Collator`, `BreakIterator` need the CLDR locale providers (large generated
   data); a root-and-`en_US`-only provider, as jrt has for `DecimalFormatSymbols` (R17), is the
   small way.
5. **`java.nio` buffers and channels** (3-5 days): buffers are generated from templates and lean
   on `ScopedMemoryAccess` and `Unsafe`; `FileChannel` and the socket channels need the host
   interface (§9.4) to grow; the charset coders follow.
6. **Sockets and `InetAddress`** (in progress elsewhere): over Go's `net`, through the host
   interface.
7. **Out of reach without a VM** (or by design): `java.lang.invoke` beyond what the evaluator
   needs, `java.lang.classfile` as a run-time loader, `java.lang.foreign`, `java.security` and
   `javax.crypto` (portable Java, but large: providers in `sun.security.*`; Go's `crypto` would
   back a small provider), modules.

## Decisions for the user

- **The two throwing gaps** (primitive `Arrays.sort`, `parallelSort`, `parallelPrefix`;
  `Comparator.naturalOrder`/`nullsFirst`/`nullsLast`; and `TreeMap`'s copy from a
  `SortedMap`): add their files to the closure before the freeze (`DualPivotQuicksort`,
  `Comparators`, `ArrayPrefixHelpers`, `ArraysParallelSortHelpers`, and a variant or
  `ObjectInputStream`-free `buildFromSorted` for `TreeMap`), or record them as known. This
  revisits, for these few files, the decision of 2026-10-08 to stub rather than grow the
  closure (JRT-SOURCES.md, "Decided").
- Whether `doc/go/java-base-coverage.edn` is regenerated in the freeze's checklist (it needs
  c2g's output: `bin/arbace-go --build`, then `bin/jrt-coverage`), or kept as this measurement.
- Whether any other item of "Going further" belongs before the freeze.
