# java.base in the Go build

How much of the JDK's `java.base` module the Go build of Arbace implements (B1a, before the
`arbace-for-golang` freeze; [B1-PLAN.md](B1-PLAN.md)). jrt is the Go build's Java runtime:
jdk26u sources translated by `bin/jrt-convert` (j2a) and a2g, jrt's own Java in `overlay/`
translated the same way, and hand-written Go forms in `go/arbace/jrt/`; a2g stubs what it does
not translate ([JRT-SOURCES.md](JRT-SOURCES.md), [JRT-NOTES.md](JRT-NOTES.md),
[A2G-SPEC.md](A2G-SPEC.md) §4.1).
Measured on the branch `jbase` (2026-10-10), first from main at `6a33be8`, then after
"java.util completed" (from main at `8ace8b1`); measured again on the branch `go-juc` from main
at `d97e0f9` (2026-10-10: sockets, the JDK's resource data, java.time and java.util.concurrent
merged), on the branch `gosec` (2026-10-10, java.security's basics, with main at `7cc3637`
merged: processes), and last on the branch `gotext` (2026-10-11, java.text and the locales,
with main at `6cbed45` merged: gosec and gofix's resources by URL; the default `JRT_LOCALES=none`
build): a2g's program output made there (`bin/a2g --program`), JDK 26.0.2.1 built from
`/root/jdk26u`. The numbers below are the last measurement's (`gotext`), as in
`doc/go/java-base-coverage.edn`, unless a section says otherwise.

Summary: java.base exports **1,636 API classes with 17,546 members** (public and protected
methods, constructors and fields, of its 58 exported packages). The Go build has **666 of the
classes (40.7%)**, and 79 more as names only, and provides **9,807 of the members (55.9%)**:
8,709 translated with their bodies, 47 translated with an operation that throws (it names
something outside the build), 1,051 hand-written in jrt; 35 more exist as stubs that throw.
The classes the Go build has are nearly complete (95.6% of their members); the rest is whole
packages left out: `java.lang.classfile`, most of `java.security` (its basics are in:
JRT-NOTES.md, "Security") and of `javax.*`,
`java.nio`'s channels, `java.lang.invoke`, most of `java.lang.foreign`. Of the **896 members
Arbace's runtime and REPL namespaces reference, 833 are provided (93.0%)**; the others are
D6's cuts and reworks (method handles, class loading). The first measurement (main
at `6a33be8`: 5,598 members, 31.9%) found primitive `Arrays.sort` and
`Comparator.naturalOrder` throwing on Go; the branch `jbase` fixed them and added java.util's
plain Java ("java.util completed": 5,787 members, 33.0%); sockets, java.time and
java.util.concurrent followed (JRT-NOTES.md, "Sockets (go-net)", "Time", "Concurrency"), then
java.security's basics (9,124 members, 52.0%, on `gosec`) and, on `gotext`
(JRT-NOTES.md, "Text and locales"), java.text whole with jdk26u's `Locale` and `Currency`:
`java.text` 575 of 580 members (99.1%; was 50, 8.6%), `java.util` 2,203 of 2,695 (81.7%).

## Method

`bin/jrt-coverage [OUT]` (bash) with its Clojure in `test/g2a/jrt_coverage.ae`
(`g2a.jrt-coverage`, next to `g2a.java-surface`). It runs on Arbace's `target/stage2`
(`bin/lib/tools.bash`), takes about 15 s, and writes:

- `doc/go/java-base-coverage.edn` (OUT; Clojure reader): totals, Arbace's use, per package
  family, per package, and per class its status and its members' counts by status;
- `.tmp/jrt-coverage/members.edn`: every class and member with its status;
- `.tmp/jrt-coverage/summary.md`: the tables of this document.

Inputs: a2g's output for the program (`A2G_OUT`, default `target/arbace-go/a2g`, written by
`bin/arbace-go --build`, or `bin/a2g --program --out DIR` after `bin/jrt-convert`), jrt's forms
(`go/arbace/jrt/`, its `manifest.edn`), `bin/jrt-convert`'s `sources.txt` (`JRT_WORK`, default
`.tmp/jrt`) and Arbace's use (`SURFACE`, default `doc/go/java-surface.edn`, from
`bin/java-surface`). Deterministic: the output depends only on these and the JDK.

**The API** is read by reflection on the running JDK 26 (26.0.2.1, built from `/root/jdk26u`,
the sources jrt translates): the packages `java.base` exports to every module
(`ModuleDescriptor.exports`, unqualified), their class files in the module image (`jrt:/`), of
those the public classes and the public or protected member classes of accessible classes (not
synthetic, anonymous or local), and of each class its *declared* public and protected methods,
constructors and fields (protected ones only in non-final classes), without synthetic members
and bridges. Members are named as the JVM names them, name and descriptor, which is how a2g
names them too (its stubs, its report, its Go names, §4.4). Reflection rather than jdk26u's
sources: it gives the compiled API exactly (implicit members such as an enum's `values`,
generated classes, no doc-comment conventions to apply) in the descriptors a2g uses; the jmods
hold the same class files. Inherited members are counted once, on the declaring class.

**The Go build**, per class:

| status | meaning |
|---|---|
| translated | a2g translated it (report's `:translated`) from jdk26u's source, or from jrt's own Java (`overlay/jdk`, KIND `overlay` in `sources.txt`) |
| declared | a2g declared it from reflection (a JDK marker interface without source) |
| hand-written | jrt defines it (a `Define` registration in `go/arbace/jrt/`, or the manifest) |
| cut | a name only: a2g registers it for the evaluator (`a2g_cut.go`), its members throw `UnsupportedOperationException` (D6; A2G-SPEC §4.1, amendment M3) |
| absent | not in the Go build |

and per member:

| status | meaning |
|---|---|
| translated | translated with its body (abstract: declared in the Go interface) |
| partial | translated, but an operation in it names a class or member outside the build and throws when reached (an operation-level stub; a2g's report, `:unavailable`) |
| hand-written | jrt implements it: in the manifest, or its Go name (§4.4) defined in the program's jrt package, or in a member table of jrt's classes (`reflect_tables`, `reflect_tables_a2g`, `a2g_support`) |
| stub | exists in Go and throws `a2g: not translated` (not reached from the program's roots, or its translation failed) |
| outside type | not in Go: its descriptor names a class outside the build (A2G-SPEC §4.1) |
| cut | a member of a cut class |
| absent | not in the build: of an absent class, a member jrt does not implement, or a member the translated source lacks (jrt's own Java is a subset; a variant cut it) |

"Provided" is translated, partial or hand-written. **Cross-check:** for every member of a
translated class, its Go name (§4.4: the method, `Impl_` method, static function, `_New`
function or `Ctor` method, static variable) is looked up in the program's forms; it must exist
exactly when the status says the member is in Go. 0 mismatches.

**Reachability.** With `--program` a2g roots every public member of every class with class forms
(A2G-SPEC §10.6), so a translated class's public members are all reached and translated; `stub`
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
| **java.base** | 1,636 | 666 | 79 | 17,546 | 9,807 | 55.9% | 8,709 | 47 | 1,051 | 35 | 104 | 7,600 |

The 666 classes in Go have 10,258 API members, of which 9,807 (95.6%) are provided. Of the
translated classes, 13 are jrt's own Java rather than jdk26u's (provided of members):
`FileDescriptor` 6/6, `FileInputStream` 13/14, `FileOutputStream` 10/11, `InputStreamReader`
9/10, `OutputStreamWriter` 11/12, `java.net.URLClassLoader` 13/16, `java.nio.file.Files` 44/70,
`Path` 30/33, `Calendar` 110/111, `GregorianCalendar` 35/35, `ResourceBundle` 12/23 and its
`Control` 14/16, `java.util.zip.InflaterInputStream` 11/15.

| kind | members | provided | % |
|---|---:|---:|---:|
| ctor | 1,597 | 815 | 51.0% |
| field | 2,300 | 1,202 | 52.3% |
| method | 13,649 | 7,790 | 57.1% |

### The big packages

|  | classes | in Go | cut | members | provided | % | translated | partial | hand-written | stub | outside type | cut or absent |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `java.io` | 86 | 45 | 3 | 922 | 493 | 53.5% | 490 | 3 | 0 | 5 | 4 | 420 |
| `java.lang` | 129 | 87 | 26 | 2,226 | 1,857 | 83.4% | 1,215 | 4 | 638 | 0 | 4 | 365 |
| `java.lang.invoke` | 25 | 0 | 2 | 331 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 331 |
| `java.lang.ref` | 7 | 4 | 1 | 22 | 15 | 68.2% | 0 | 0 | 15 | 0 | 0 | 7 |
| `java.lang.reflect` | 34 | 12 | 7 | 347 | 119 | 34.3% | 37 | 0 | 82 | 0 | 0 | 228 |
| `java.math` | 4 | 4 | 0 | 177 | 177 | 100.0% | 177 | 0 | 0 | 0 | 0 | 0 |
| `java.net` | 66 | 30 | 4 | 760 | 387 | 50.9% | 384 | 3 | 0 | 3 | 12 | 358 |
| `java.nio` | 14 | 9 | 4 | 365 | 190 | 52.1% | 179 | 11 | 0 | 0 | 4 | 171 |
| `java.nio.charset` | 12 | 4 | 0 | 106 | 24 | 22.6% | 4 | 0 | 20 | 0 | 0 | 82 |
| `java.nio.file` | 47 | 18 | 0 | 252 | 120 | 47.6% | 120 | 0 | 0 | 0 | 17 | 115 |
| `java.security` | 98 | 27 | 5 | 663 | 198 | 29.9% | 198 | 0 | 0 | 5 | 2 | 458 |
| `java.text` | 35 | 35 | 0 | 559 | 554 | 99.1% | 554 | 0 | 0 | 5 | 0 | 0 |
| `java.time` | 19 | 19 | 0 | 794 | 794 | 100.0% | 794 | 0 | 0 | 0 | 0 | 0 |
| `java.util` | 134 | 126 | 0 | 2,239 | 2,089 | 93.3% | 2,059 | 4 | 26 | 3 | 3 | 144 |
| `java.util.concurrent` | 81 | 72 | 0 | 1,199 | 1,107 | 92.3% | 1,009 | 4 | 94 | 5 | 3 | 84 |
| `java.util.concurrent.atomic` | 16 | 13 | 0 | 319 | 259 | 81.2% | 151 | 0 | 108 | 0 | 0 | 60 |
| `java.util.concurrent.locks` | 14 | 14 | 0 | 201 | 170 | 84.6% | 102 | 0 | 68 | 6 | 0 | 25 |
| `java.util.function` | 43 | 43 | 0 | 79 | 79 | 100.0% | 79 | 0 | 0 | 0 | 0 | 0 |
| `java.util.random` | 7 | 6 | 0 | 90 | 70 | 77.8% | 64 | 6 | 0 | 0 | 0 | 20 |
| `java.util.regex` | 4 | 4 | 0 | 82 | 82 | 100.0% | 82 | 0 | 0 | 0 | 0 | 0 |
| `java.util.stream` | 21 | 21 | 0 | 300 | 300 | 100.0% | 300 | 0 | 0 | 0 | 0 | 0 |

### By package family

Each package with its subpackages, except those counted as families of their own
(`java.util.concurrent` with `atomic` and `locks`, `java.util.regex`, `java.util.stream`,
`java.util.function`; `java.util` keeps `jar`, `zip`, `spi` and `random`). Every package is in `doc/go/java-base-coverage.edn` and `.tmp/jrt-coverage/summary.md`.

|  | classes | in Go | cut | members | provided | % | translated | partial | hand-written | stub | outside type | cut or absent |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `java.util` | 180 | 142 | 5 | 2,695 | 2,203 | 81.7% | 2,167 | 10 | 26 | 6 | 6 | 480 |
| `java.lang` | 129 | 87 | 26 | 2,226 | 1,857 | 83.4% | 1,215 | 4 | 638 | 0 | 4 | 365 |
| `java.util.concurrent` | 111 | 99 | 0 | 1,719 | 1,536 | 89.4% | 1,262 | 4 | 270 | 11 | 3 | 169 |
| `java.lang.classfile` | 220 | 0 | 0 | 1,628 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 1,628 |
| `java.time` | 70 | 58 | 0 | 1,565 | 1,355 | 86.6% | 1,355 | 0 | 0 | 0 | 0 | 210 |
| `java.security` | 206 | 30 | 6 | 1,307 | 199 | 15.2% | 199 | 0 | 0 | 5 | 2 | 1,101 |
| `java.io` | 86 | 45 | 3 | 922 | 493 | 53.5% | 490 | 3 | 0 | 5 | 4 | 420 |
| `java.net` | 71 | 32 | 4 | 774 | 395 | 51.0% | 392 | 3 | 0 | 3 | 12 | 364 |
| `java.text` | 41 | 41 | 0 | 580 | 575 | 99.1% | 575 | 0 | 0 | 5 | 0 | 0 |
| `javax.crypto` | 62 | 6 | 1 | 407 | 37 | 9.1% | 36 | 1 | 0 | 0 | 0 | 370 |
| `java.nio.file` | 74 | 19 | 1 | 405 | 122 | 30.1% | 122 | 0 | 0 | 0 | 17 | 266 |
| `java.nio` | 14 | 9 | 4 | 365 | 190 | 52.1% | 179 | 11 | 0 | 0 | 4 | 171 |
| `java.lang.reflect` | 34 | 12 | 7 | 347 | 119 | 34.3% | 37 | 0 | 82 | 0 | 0 | 228 |
| `javax.net` | 45 | 0 | 0 | 338 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 338 |
| `java.lang.invoke` | 25 | 0 | 2 | 331 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 331 |
| `java.nio.channels` | 62 | 0 | 2 | 323 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 323 |
| `java.util.stream` | 21 | 21 | 0 | 300 | 300 | 100.0% | 300 | 0 | 0 | 0 | 0 | 0 |
| `java.lang.foreign` | 25 | 2 | 12 | 261 | 44 | 16.9% | 33 | 11 | 0 | 0 | 51 | 166 |
| `javax.security` | 43 | 2 | 0 | 209 | 4 | 1.9% | 4 | 0 | 0 | 0 | 0 | 205 |
| `java.math` | 4 | 4 | 0 | 177 | 177 | 100.0% | 177 | 0 | 0 | 0 | 0 | 0 |
| `java.lang.constant` | 12 | 2 | 0 | 168 | 1 | 0.6% | 1 | 0 | 0 | 0 | 1 | 166 |
| `java.lang.module` | 19 | 0 | 0 | 148 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 148 |
| `java.nio.charset` | 13 | 4 | 0 | 109 | 24 | 22.0% | 4 | 0 | 20 | 0 | 0 | 85 |
| `java.util.regex` | 4 | 4 | 0 | 82 | 82 | 100.0% | 82 | 0 | 0 | 0 | 0 | 0 |
| `java.util.function` | 43 | 43 | 0 | 79 | 79 | 100.0% | 79 | 0 | 0 | 0 | 0 | 0 |
| `java.lang.annotation` | 12 | 0 | 5 | 35 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 35 |
| `java.lang.runtime` | 3 | 0 | 0 | 24 | 0 | 0.0% | 0 | 0 | 0 | 0 | 0 | 24 |
| `java.lang.ref` | 7 | 4 | 1 | 22 | 15 | 68.2% | 0 | 0 | 15 | 0 | 0 | 7 |

### Arbace's use

The members Arbace's runtime and REPL namespaces reference (java-surface.edn), in java.base's
exported API:

| members used | translated | partial | hand-written | stub | outside type | cut | absent |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 896 | 585 | 0 | 248 | 0 | 2 | 23 | 38 |

833 of 896 (93.0%) are provided (827 at the measurement on `gosec`, 805 on `go-juc`, 793 at
the first). The 63 others are cut or reworked by decision: method handles and call sites
(`java.lang.invoke`, 35: the evaluator replaces `invokedynamic`), class loading (`ClassLoader`'s
`defineClass`, `findClass`, `findLoadedClass`, `resolveClass`, `getSystemResource` and
`getSystemResourceAsStream`, 6; `URLClassLoader.findClass`; jars: `JarURLConnection.getJarFile`,
`JarFile.getEntry`, `ZipEntry.getTime`), channels (`FileChannel`, `getChannel`,
`Reader.read(CharBuffer)`, `AbstractInterruptibleChannel.close`, 6), annotations (`Retention`,
`RetentionPolicy`, `getAnnotation` twice, 4), modules (`Class.getModule`, `Module.getName`),
`Runtime.version`/`Version.feature`, `System.loadLibrary`, `URLEncoder.encode`, serialization's
`defaultWriteObject`, and `Executable.getName` (declared abstract there; `Method` and
`Constructor` provide it). Processes (`Process`, `ProcessBuilder`, its `Redirect`, 21; amendments
PR1-PR7), sockets (`Socket`, `ServerSocket`, `InetAddress`; NT1-NT5), `Runtime.exec`,
`System.getProperties`, `Thread$Builder.factory`, `DateFormat`/`SimpleDateFormat` (TL) and
`ClassLoader.getResource*` with most of `URLClassLoader` (gofix, "Resources by URL") are
provided since the earlier measurements.

## What is implemented

- **Translated from jdk26u** (plain Java, exact semantics): the boxed numbers, `Character` with
  JDK 26's Unicode tables, the exceptions and errors of `java.lang`; `java.math` whole;
  `java.util`'s collections (lists, sets, maps, deques, `Hashtable`/`Vector`/`Stack`,
  `IdentityHashMap`, `EnumSet`/`EnumMap`, the sequenced and immutable collections),
  `Arrays` (with the primitive and parallel sorts since JB1), `Collections`, `Objects`,
  `Optional*`, `Comparator`'s natural-order comparators, `SortedSet`, `BitSet`,
  `PriorityQueue`, `WeakHashMap`, `StringTokenizer`, `Base64`, `SplittableRandom`, the event
  classes, `Formatter`, `Properties`, `Random`,
  `UUID`, `HexFormat`, `StringJoiner`, `Spliterators`; `java.util.stream` and
  `java.util.function` whole; `java.util.regex` (D5); `java.util.random`'s
  `RandomGenerator`; `java.util.concurrent` but for `ForkJoinPool`, the field updaters and
  `StructuredTaskScope` (its collections and queues, synchronizers, `CompletableFuture`, the
  executors, `ThreadLocalRandom`, the atomic arrays, adders and accumulators, `StampedLock`,
  `AbstractQueuedSynchronizer`; JRT-NOTES.md, "Concurrency");
  `java.io`'s readers, writers and streams, `File` and the Unix file system; `java.net.URI`,
  `URL` with the `file:` connection, `URLDecoder`, `Socket`, `ServerSocket`, `InetAddress`
  (JRT-NOTES.md, "Sockets (go-net)"); `Process`, `ProcessBuilder`, `ProcessHandle`
  ("Processes"); `java.time` with its ISO chronology, formats and zone rules, and
  `java.util.TimeZone`/`SimpleTimeZone` ("Time"); `java.text` whole, `Locale` and `Currency`
  ("Text and locales"); `java.nio`'s heap buffers (amendment RD4), `CRC32`, `ZipException`;
  `java.nio.file`'s `Paths`, options and exceptions; java.security's basics (`MessageDigest`
  and its streams, `SecureRandom`, `Provider`, `Security`, `javax.crypto.Mac`, `SecretKeySpec`,
  the SUN and SunJCE providers' digests, generators and HMACs; JRT-NOTES.md, "Security").
- **jrt's own Java** (overlay, translated by a2g): `FileDescriptor`, `FileInputStream`,
  `FileOutputStream`, `InputStreamReader`, `OutputStreamWriter`, `java.nio.file.Path` and
  `Files` (a subset), `Calendar`, `GregorianCalendar`, `ResourceBundle` and its `Control`,
  `java.util.zip.InflaterInputStream`, `URLClassLoader` (over directories), and outside the API
  `jdk.internal.jrt.ClassPathResources` (the class path's resources; "Resources by URL").
- **Hand-written Go**: `Object`, `Class` and `java.lang.reflect` (over a2g's member tables),
  `String`, `StringBuilder`, `StringBuffer` (UTF-16, D4), `Math`, `StrictMath`, `System`,
  `Runtime`, `Thread`, `ThreadLocal`, `Throwable`, `StackTraceElement`, `Enum`, `Record`;
  references; atomics (`AtomicBoolean`, `Integer`, `Long`, `Reference`), locks
  (`ReentrantLock`, `ReentrantReadWriteLock`, `Condition`, `LockSupport`), the executors'
  interfaces, `ForkJoinPool`/`ForkJoinTask`, `CountDownLatch`, `Semaphore`; `Charset`
  (six charsets), `Date`.

## Stubbed and partial

At the last measurement (`gotext`) there are 35 stubs, 47 partial members and 104 outside type,
each listed with its status in `.tmp/jrt-coverage/members.edn` (`bin/jrt-coverage`); per class,
`doc/go/java-base-coverage.edn`'s `:classes` counts them. By class: the stubs are
`ForkJoinWorkerThread` (3), `AbstractQueuedSynchronizer` (3), `AbstractQueuedLongSynchronizer`,
`PrintStream`, `PrintWriter`, `Provider`, `SocketImpl`, `AttributedCharacterIterator$Attribute`
(2 each), and one each in `ObjectStreamException`, `ServerSocket`, `MessageDigest`,
`SecureRandom`, `SecureRandomSpi`, `DateFormat$Field`, `MessageFormat$Field`,
`NumberFormat$Field`, `Calendar`, `Observable`, `ResourceBundle`, `CompletionException`,
`TimeUnit`, `AbstractOwnableSynchronizer` and three `java.util.spi` providers; the partial
members are `MemorySegment`'s 11, the heap buffers' 11 (`ByteBuffer`, `CharBuffer`,
`IntBuffer`), the `RandomGenerator` family's 6 (`of(String)`), and 19 in `DataInputStream` (2),
`ByteArrayInputStream`, the four `describeConstable`, `InetAddress`, `URLConnection`,
`URLStreamHandler`, `List`, `Map`, `Properties` (2), `CountedCompleter`,
`ForkJoinWorkerThread` (3) and `MacSpi`; the outside-type
members are `MemorySegment`'s 51, `Files`' 14 and `Path`'s 3, `ByteBuffer`'s 4, and one or two in
the file streams, readers and writers (`getChannel`), the boxed numbers (`resolveConstantDesc`),
the sockets and `URL`'s classes (`Proxy`, `NetworkInterface`), `InflaterInputStream`,
`ResourceBundle`, `Executors`, `BitSet`, `AlgorithmConstraints`. The lists that follow are the
measurement after "java.util completed" (2026-10-10), kept as that record: several of their
members are provided since (`Instant.parse` and `Instant`'s `java.time.temporal` members,
`TimeUnit`'s and `TimeZone`'s `java.time` conversions, `UUID.ofEpochMillis`).

**Stubs (12 members).** Every public member of a translated class is a root, so the stubs are
protected members and constructors nothing reaches: `ObjectStreamException`'s two cause
constructors, `PrintStream`'s and `PrintWriter`'s `setError`/`clearError`, `Calendar()`,
`Observable.setChanged`, `ForkJoinWorkerThread(ForkJoinPool)`, `onStart`, `onTermination`,
`TimeUnit.timedWait`. A subclass the evaluator makes (`proxy`) calling one of them throws.

**Partial (25 members).** Translated, with one operation that names a class or member outside
the build and throws when that path runs (a2g's report, `:unavailable`, gives each reason):

| members | outside the build | what throws |
|---|---|---|
| the six `RandomGenerator` `of(String)` factories | `RandomGeneratorFactory` | always |
| `describeConstable` of `Boolean`, `Byte`, `Character`, `Short` | `ConstantDescs` | always |
| `DataInputStream.readLine`, `readUTF` | `PushbackInputStream`, `DataInput` | `readLine` on a lone `\r` (`\r` not followed by `\n`), `readUTF` always |
| `UUID.ofEpochMillis`, `List.ofLazy`, `Map.ofLazy`, `Properties.loadFromXML`/`storeToXML`, `Instant.parse`, `ByteArrayInputStream.transferTo`, `URLConnection.getHeaderFieldDate`, `URLStreamHandler.setURL`, `CountedCompleter.helpComplete`, three of `ForkJoinWorkerThread` | `SecureRandom`, `LazyCollections`, XML, `DateTimeFormatter`, ... | when the path runs |

The status is per method: a method whose own code is complete but which calls a partial private
method is counted as translated. One such case is known: `BigInteger.nextProbablePrime` (and
`isProbablePrime` of large numbers) reach `passesMillerRabin`, which needs
`ThreadLocalRandom.current()`, which works since java.util.concurrent was translated.

**Outside type (72 members).** Members of translated classes whose descriptors name a class
outside the build do not exist in Go: `Instant`'s 21 `java.time.temporal` members, `Files`' 17
attribute, channel and directory-stream members, `getChannel`, the `ByteBuffer`, `LongBuffer`,
`CharBuffer` and coder overloads (`Base64`, `BitSet`, the readers and writers), `TimeUnit`'s and
`TimeZone`'s `java.time` conversions, `resolveConstantDesc`, `Matcher.toMatchResult`
(`MatchResult` is cut), `URL`'s `Proxy` overloads. A further 14 are absent from jrt's own Java
(`Files.list`, `isSymbolicLink`, links, attributes, `mismatch`, `probeContentType`;
`TimeZone.availableIDs`).

## Cut, and why

D6 ([B1-PLAN.md](B1-PLAN.md)) left out of the first REPL what has no Go-side meaning yet;
JAVA-SURFACE.md lists what of Arbace each cut breaks, A2G-SPEC §12 what is out of scope. The
cut classes Arbace names exist as names whose members throw; everything else is absent.

| area | in the Go build | why |
|---|---|---|
| class loading, `java.lang.invoke`, `java.lang.classfile`, `java.lang.constant` (but `Constable`, `ConstantDesc`) | absent or cut | the evaluator replaces bytecode, call sites and method handles (D2); no class files |
| `java.lang.reflect`'s generic and annotated types, annotations at run time | absent | closed world over a2g's member tables (JAVA-SURFACE.md decision 1; amendment R16) |
| processes (`Process`, `ProcessBuilder`), `ProcessHandle` | translated (since 2026-10-10) | D6 reversed for them (amendments PR1-PR7; JRT-NOTES.md, "Processes"): over the host's `HostProc`; `sh` and `arbace.java.process` are in |
| `java.net`'s URL connections but `file:`, `HttpURLConnection`, `DatagramSocket`, `NetworkInterface`, `Proxy`, `URLEncoder` | cut or absent | D6; sockets, `InetAddress` and the socket REPL are translated since 2026-10-10 (amendments NT1-NT5; JRT-NOTES.md, "Sockets (go-net)"), `URLClassLoader` over directories is jrt's own Java ("Resources by URL") |
| `java.nio`'s direct and mapped buffers and the `Long`, `Short`, `Float`, `Double` buffers, channels, charset coders, most of `java.nio.file` | absent or cut | D6; the heap `ByteBuffer`, `CharBuffer` and `IntBuffer` are translated (amendment RD4); the file system is reached through `File` and jrt's `Path`/`Files` (FS2) |
| `java.time` (but its non-ISO chronologies, absent), `java.text` | translated (since 2026-10-10, 2026-10-11) | D6 reversed for them (amendments TM1-TM9, TL1-TL8; JRT-NOTES.md, "Time", "Text and locales"); the shipped build's locale data are java.base's root and English (`JRT_LOCALES=none`), `common` or `all` add jdk.localedata's |
| serialization (`ObjectInputStream`, `ObjectOutputStream`) | absent | D6, A2G-SPEC §12 |
| `java.security`, `javax.crypto` beyond their basics (signatures, ciphers, keys, key stores, certificates, the providers but SUN and SunJCE); `javax.net`, `javax.security` (but `Destroyable`) | absent | not used by Arbace; the basics (`MessageDigest`, `SecureRandom`, `Mac`, `SecretKeySpec`, `Provider`, `Security`) are translated since 2026-10-10 (amendments SC1-SC7; JRT-NOTES.md, "Security") |
| `java.util.jar`, `java.util.zip` (but `CRC32`, `Checksum`, `ZipException` and jrt's `InflaterInputStream`), `ServiceLoader`, `java.util.spi` (but the locale service providers), `java.lang.module`, `Module`, `Package` | absent or cut | no jars or modules in a static executable; `ResourceBundle` is jrt's own Java (amendments TM4, TL5), `InflaterInputStream` too (RD3) |
| `java.lang.foreign`, `ScopedValue`, `StructuredTaskScope`, `StackWalker` | absent or cut | VM features without a Go counterpart yet |

## Notable gaps

- **Fixed on this branch** ("java.util completed" below): primitive `Arrays.sort`,
  `parallelSort`, `parallelPrefix`, `Comparator.naturalOrder`/`nullsFirst`/`nullsLast`,
  `Comparator.comparing` and every other serializable lambda (they threw
  `ClassCastException`), `SortedSet`, `TreeMap`'s copies from sorted maps.
- **`java.util`**: no `Scanner`, `ServiceLoader`, `Timer` (`Cleaner`); `ResourceBundle` 12 of 23
  (jrt's own: no `getBundle` by module or class loader); `Date` 26 of 35 (no deprecated setters,
  `parse`, `toLocaleString`). `Locale` (75 of 75), `Locale.Builder`, `Currency`, `TimeZone` and
  `SimpleTimeZone` are jdk26u's since the branches `go-time` and `gotext` (they were gaps at the
  first measurements: `Locale` 21 of 75).
- **`java.util.concurrent`**: 72 of 81 classes (atomic 13 of 16, locks 14 of 14); missing
  are the field updaters, `StructuredTaskScope` and its kin, and most of jrt's hand-written
  `ForkJoinPool` (its work stealing; JRT-NOTES.md, "Concurrency", Decisions).
- **`java.lang`**: `Class` 57 of 81 members (no annotations, modules, packages, nest, sealed or
  record reflection, member classes, `getResource`), `ClassLoader` 11 of 43, `System` 20 of 31 (no
  `loadLibrary`, `Logger`), `Thread` 44 of 57 (no thread groups, `ofPlatform`, the `Duration`
  overloads of `join` and `sleep`), `Runtime` 15 of 20 (no `version`; `exec` since PR4; `System.getProperties`, `Thread.getState`
  since the sockets (NT) and JC8).
- **`java.lang.reflect`**: 119 of 347 members; `Field` 12 of 35 (no typed getters and setters),
  `Executable` 7 of 22, no `Parameter`, `RecordComponent`, generic types.
- **`java.io`**: no `DataOutputStream`/`DataOutput`, `RandomAccessFile`, `PushbackInputStream`,
  `CharArrayWriter`, `Piped*`, `Console`, `StreamTokenizer`, serialization.
- **`java.nio.charset`**: no `CharsetEncoder`/`Decoder`; six charsets (R18, S6).

## Going further

What it would take, roughly by cost (agent-assisted days at the pace of B1a):

1. **More plain Java of `java.util`**: done on this branch (below). What is left of
   `java.util` needs other parts: `Scanner` (`CharBuffer`), `Currency` and `ResourceBundle`
   (data files, class loading), `Timer` (`Cleaner`), `Locale`'s rest (locale data); `Currency`
   and `Locale` are done since (`gotext`), `ResourceBundle` is jrt's own (TM4, TL5).
2. **`java.util.concurrent` beyond the shims** (done, branch `go-juc`, 2026-10-10: amendments
   JC1-JC10, JRT-NOTES.md "Concurrency"; as planned here): JDK 26's concurrent classes use
   `VarHandle` (`ConcurrentLinkedQueue`, `ConcurrentSkipListMap`, `CompletableFuture`,
   `Exchanger`, the atomic arrays and field updaters, `Striped64`/`LongAdder`) or AQS over
   `Unsafe` (`AbstractQueuedSynchronizer`, `StampedLock`). a2g would compile field `VarHandle`s
   (`MethodHandles.lookup().findVarHandle` constants and their access modes) to Go atomics, as
   jrt's `Unsafe` does for `ConcurrentHashMap`; then the classes translate. `ThreadPoolExecutor`,
   `Executors` and `AbstractExecutorService` are plain Java over AQS and could replace the
   hand-written executors (a decision: exact Java semantics against goroutine-native shims).
3. **`java.time`** (done, branch `go-time`, 2026-10-10: amendments TM1-TM9, JRT-NOTES.md
   "Time"; the non-ISO chronologies left out) (2-4 days): 58,000 lines of plain Java; it needs the time-zone database
   (`tzdb.dat` as a resource of the executable, `ZoneRulesProvider`) and `java.text`-free
   formatting paths; `DateTimeFormatter`'s localized text needs locale data.
4. **`java.text` and locales** (done 2026-10-11, JRT-NOTES.md "Text and locales"): `DecimalFormat`, `SimpleDateFormat`,
   `MessageFormat`, `Collator`, `BreakIterator` need the CLDR locale providers (large generated
   data); a root-and-`en_US`-only provider, as jrt has for `DecimalFormatSymbols` (R17), is the
   small way.
5. **`java.nio` buffers and channels** (the heap buffers done, amendment RD4; channels open)
   (3-5 days): buffers are generated from templates and lean
   on `ScopedMemoryAccess` and `Unsafe`; `FileChannel` and the socket channels need the host
   interface (§9.4) to grow; the charset coders follow.
6. **Sockets and `InetAddress`** (done, branch `go-net`, 2026-10-10: amendments NT1-NT5,
   JRT-NOTES.md "Sockets (go-net)"): over Go's `net`, through the host interface.
7. **Out of reach without a VM** (or by design): `java.lang.invoke` beyond what the evaluator
   needs, `java.lang.classfile` as a run-time loader, `java.lang.foreign`, the rest of `java.security`
   and `javax.crypto` (portable Java, but large: providers in `sun.security.*`; the basics are
   translated since 2026-10-10, their block functions over Go's `crypto`; Go's `crypto` would
   back a small provider), modules.

## java.util completed (branch `jbase`, 2026-10-10)

The user's decision of 2026-10-10: close the gaps above and add the plain Java of `java.util`
before the freeze (java.time and java.util.concurrent are other branches'). Done, from main at
`8ace8b1`:

- **25 jdk26u files added to the closure** (`util-sources` in `test/g2a/jrt_sources.ae`,
  amendment JB1): `DualPivotQuicksort`, `ArraysParallelSortHelpers`, `ArrayPrefixHelpers`,
  `Comparators`, `SortedSet`, `jdk.internal.util.random.RandomSupport`, `java.math.BitSieve`,
  `BitSet`, `PriorityQueue`, `WeakHashMap`, `StringTokenizer`, `Base64`, `SplittableRandom`,
  `EventObject`, `EventListener`, `EventListenerProxy`, `Observable`, `Observer`, and the
  exceptions and constants `TooManyListenersException`, `InputMismatchException`,
  `MissingResourceException`, `IllformedLocaleException`, `InvalidPropertiesFormatException`,
  `FormattableFlags`, `ServiceConfigurationError`. `bin/jrt-convert`: 414 files, all compiled to
  javac's class shapes (410 identical, the 4 known differences).
- **Three Go-build variants** (`overlay/jdk/variants/`, amendment JB2): `TreeMap` (its
  sorted-build methods without the `ObjectInputStream` parameter, called from the copy
  constructor, `clone`, `putAll` and `addAllForTreeSet`), `BitSet` (`valueOf(byte[])` and
  `toByteArray` by shifts instead of a little-endian `ByteBuffer`), `RandomGenerator`
  (`isDeprecated` is false: no annotations in the Go build, R16).
- **a2g**: FromFn's array results (amendment JB3) and lambdas with marker interfaces
  (amendment JB5); **jrt**: forked tasks not yet done run again (amendment JB4).
- **The oracle**: `test/oracle/forms/java_util.ae`, 161 cases recorded on the JVM: all pass on
  Go but the three `nextProbablePrime` cases (`ThreadLocalRandom.current`, listed in
  `known-go-amd64.edn` with that reason).

| | before (main `8ace8b1`) | after |
|---|---:|---:|
| java.base members provided | 5,598 (31.9%) | 5,787 (33.0%) |
| of which partial | 108 | 25 |
| classes in Go | 384 | 405 |
| `java.util` (the package) provided | 1,731 of 2,239 (77.3%) | 1,916 (85.6%) |
| executable, amd64 | 80,126,079 bytes | 82,283,432 bytes (+2.16 MB, +2.7%) |

Checks (amd64; arm64 not built on this branch): `bin/jrt-convert` (414 files, shape check as
above), `bin/a2g --program`, `bin/arbace-go --build` and `--smoke` pass; Clojure's suite on Go
19,506 of 19,506 assertions, no regression against `test/arbace-go-results.edn`; the oracle
against `known-go-amd64.edn`: the only new mismatches besides the three recorded above are the
271 `defclass`/`defclass_corpus` cases that main at `8ace8b1` has too (the class forms at the
REPL, checked on an executable built from `8ace8b1`: the same 271), not recorded here.

### Amendments (JB)

All accepted by the user 2026-10-10 and folded where they belong (A2G-SPEC §16, "java.util
completed"):

- **JB1 (JRT-SOURCES.md, "The closure as grown") The java.util files**: the list above, as
  `util-sources` beside `added-sources`, each group with its reason. *Accepted 2026-10-10, folded into JRT-SOURCES.md, "The closure as grown".*
- **JB2 (A2G-SPEC §4.6, the JDK's variants) `TreeMap`, `BitSet`, `RandomGenerator`**, as above.
  A replacing member's parameter tags are written as j2a writes the original's (generic tags
  included: `^{:tag (SortedMap K (? extends V))}`), since the variant finds the member by them. *Accepted 2026-10-10, folded into A2G-SPEC §4.6.*
- **JB3 (A2G-SPEC §7.11, FromFn) An array result is checked against the return type's array
  class**, as `Dyn`'s results are. Before, the adapter checked the result against its own class
  through the generic `jrt.NN`, which Go cannot instantiate on an `any`, and wrote the call
  twice; no functional interface with an array result had been in the world
  (`DualPivotQuicksort`'s private `PartitionOperation` is the first). *Accepted 2026-10-10, folded into A2G-SPEC §7.11.*
- **JB4 (A2G-SPEC §8.4, the fork-join pool) A forked task not yet done runs again.** jrt ran a
  task once (claimed `fjNew` to `fjRunning`); a `CountedCompleter` whose `exec` returned
  without completing and that is forked again (`ArrayPrefixHelpers`' cumulation reforks its
  parent) never ran again, and `parallelPrefix` deadlocked. Forking now runs `exec` whenever
  the task is not done, as the JDK's `doExec`; joining is unchanged. (A change to jrt's
  `forkjoin.ae`, which the java.util.concurrent branch also owns.) *Accepted 2026-10-10, folded into A2G-SPEC §8.4.*
- **JB5 (A2G-SPEC §7.11, lambdas) A lambda whose target is an intersection with marker
  interfaces** (`(lambda (& Comparator Serializable) ...)`, javac's
  `(Comparator<T> & Serializable)`) gets an adapter that implements them, `F_Fn_M1_..._Mn`,
  registered as `F$$Lambda$M1...`. Before, the adapter implemented only `F`, and the cast to
  `Serializable` that javac writes threw `ClassCastException`: `Comparator.comparing`,
  `comparingInt`, `thenComparing`, `Map.Entry.comparingByKey` and the like failed on Go. *Accepted 2026-10-10, folded into A2G-SPEC §7.11.*

## Decisions for the user

- Whether `doc/go/java-base-coverage.edn` is regenerated in the freeze's checklist (it needs
  a2g's output: `bin/arbace-go --build`, then `bin/jrt-coverage`), or kept as this measurement.
  Decided since: regenerated at the freeze (step 8; [AGENDA.md](../AGENDA.md)), with this
  document's tables.
