# c2g: class forms compiled to Go forms

Status: accepted (2026-10-08; the user took the recommendation of each of the 23 questions of
§16), B1a step 2 ([B1-PLAN.md](B1-PLAN.md)), with the 25 amendments found while implementing
jrt (J1-J10, R11-R19, T11-T16, [JRT-NOTES.md](JRT-NOTES.md)) folded in (§16), and the 20
amendments found while implementing c2g (C1-C8, P1-P4, E1-E8, [C2G-NOTES.md](C2G-NOTES.md)) with
the evaluator's decisions Q1-Q7 ([EVAL-PLAN.md](EVAL-PLAN.md)) that touch §10, accepted
2026-10-09 and folded in (§16), and the 9 amendments of c2g's phase 2 parts C and D (K1-K3,
W1-W6, [C2G-NOTES.md](C2G-NOTES.md)), accepted 2026-10-09 and folded in (§16), with part D's
performance work deferred to step 7 (§13.4), and the amendments of the evaluator, B1a step 5
(M1-M8, X1-X3, S1, S3-S6, [EVAL-NOTES.md](EVAL-NOTES.md)), accepted 2026-10-09 and folded in
(§16), and the amendments of java.util's completion (JB1-JB5,
[JAVA-BASE.md](JAVA-BASE.md)), accepted 2026-10-10 and folded in (§16). The
decisions it builds on: D1 (c2g + jrt), D2 (an evaluator first), D4 (Java's UTF-16 strings), D5
(a port of `java.util.regex`), D6 (the first REPL without class forms, `gen-class`, `proxy`,
interop beyond jrt's classes; `proxy` came back with amendment X2, and the fork-join pool with
S5), and the seven decisions of 2026-10-08 in
[JAVA-SURFACE.md](JAVA-SURFACE.md) (reflection hand-written over member tables c2g generates;
the plain Java of the closure translated from jdk26u; a hand-written UTF-16 `String`;
concurrency mixed; thread identity by a goroutine-local slot in the Go runtime; a trimmed REPL
start; `#inst` over a small `Date`).

This spec defines **c2g**, the compiler from **class forms** ([classes/SPEC.md](../classes/SPEC.md),
the Java constructs as Clojure: the source of `arbace/lang/` and of j2c's output for the JDK
classes) to **Go forms** ([SPEC.md](SPEC.md), Go programs as Clojure data, printed and built by
`bin/g2c build`). It says how Java's types, objects, statics and code become Go, what c2g writes
and what it leaves to **jrt** (the Java runtime in Go forms under the translated classes), and
how the evaluator of B1a step 5 sits on top. It is the counterpart for Go of the class forms
compiler's back end (`arbace.classes.emit`), and follows the two form specs in structure.

Words used throughout:

- **class forms**, **the analyzer**: the forms of classes/SPEC.md and Arbace's class forms
  compiler `arbace.classes`, whose front end (entering, member resolution, the analysis of code
  into typed nodes, `arbace.classes.analyze`) c2g reuses unchanged (§4.1).
- **Go forms**, **the printer**, **the build**: SPEC.md's forms, `arbace.g2c.print` and
  `bin/g2c build` ([BUILD.md](BUILD.md)).
- **jrt**: the Go package `arbace/jrt` holding the JDK classes of the closed world, translated by
  c2g from j2c's conversion of jdk26u or hand-written in Go forms (the VM's edge:
  JAVA-SURFACE.md, "shim"), and the run-time helpers c2g's output calls (§11).
- **the closed world**: every class c2g translates or jrt hand-writes for one build (§4.1). No
  other class exists at run time, except the evaluator's dynamic types (§5.12).
- **leaf class**: a class that no class of the closed world extends (§5.3).
- **Go type of** a Java type: the Go type c2g gives values of that static type (§5.1).
- Java names are binary names (`arbace.lang.PersistentVector$Node`); descriptors are JVM
  descriptors, as the analyzer works with them.

Contents: 1 Principles · 2 A first example · 3 Equivalence · 4 The target: packages, files,
names · 5 Types and objects · 6 Statics and initialization · 7 Code · 8 Concurrency and the
memory model · 9 The boundary to jrt · 10 The boundary to the evaluator · 11 jrt's API as c2g
uses it · 12 Out of scope · 13 Performance · 14 Coverage · 15 Worked examples · 16 Open
questions · 17 Sources.

## 1. Principles

1. **Java's semantics, exactly, where Arbace can observe them.** A translated class behaves as
   the JVM runs it: arithmetic, conversions, `null`, exceptions, string contents, class
   initialization order, identity, overload choice (made by the analyzer, as on the JVM). The
   few places where Go cannot or need not follow are listed (§3.2).
2. **The analyzer decides, c2g translates.** c2g runs the class forms compiler's front end and
   translates its typed nodes. Name resolution, overloads, conversions, boxing, constant
   folding, bridges, inner class plumbing, enum and record members, switch maps and the other
   derivations of class forms §6 are the analyzer's; c2g never re-derives them. So the Go build
   and the JVM build cannot disagree on what a form means.
3. **Go's machinery where it fits.** Objects are Go structs, references Go interface values or
   pointers, dispatch Go interface calls, exceptions Go panics, threads goroutines, the heap
   Go's garbage collector. jrt adds only what Go lacks (class initialization, monitors, UTF-16
   strings, reflection tables). No `unsafe` in c2g's output.
4. **A closed world.** c2g compiles all classes of a build at once and knows every class and
   its subclasses. It uses that knowledge (§5.3), and the result is valid only for that world:
   loading classes at run time is out of scope (D6).
5. **Mechanical and stable names.** Every Go identifier follows from the Java name by a fixed
   rule (§4.4), so hand-written jrt code and translated code meet at known names, and a change
   in one class does not rename members of another.
6. **Output is Go forms.** c2g writes Go forms as the converter writes them for Go source:
   readable, one Go file per Java source file, printed by the printer and built by the build.
   Nothing is generated as Go text.

## 2. A first example

`arbace/lang/Reduced.clj`:

```clojure
(defclass ^:public ^:final Reduced
  :implements [IDeref]
  (field val)
  (constructor ^:public [this val] (set! (.-val this) val))
  (method ^:public deref [this] val))
```

c2g writes, in `go/arbace/lang/Reduced.clj` (package `arbace/lang`, §4.2):

```clojure
(in-ns 'go.arbace.lang)
(go/file "Reduced.go" :imports [[jrt "arbace/jrt"]])

(go/type Reduced (struct jrt/Object ^any F_val))

(go/func Reduced_New_O ^{:tag (* Reduced)} [^any val]
  (let [t (addr (lit Reduced))]
    (.Ctor_O t val)
    t))

(go/method Ctor_O [^{:tag (* Reduced)} t ^any val]
  (set! (.-F_val t) val))

(go/method Deref__O ^any [^{:tag (* Reduced)} t]
  (.-F_val t))

(go/method Is_IDeref [^{:tag (* Reduced)} t])
(go/method Ref ^any [^{:tag (* Reduced)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* jrt/Class)} [^{:tag (* Reduced)} t] Reduced_class)
(go/method ToString__String ^{:tag (* jrt/String)} [^{:tag (* Reduced)} t]
  (jrt/Object_toString t))
```

and `IDeref` (an interface, in `IDeref.clj`) is

```clojure
(go/type IDeref (interface jrt/Object_I (Is_IDeref []) (Deref__O ^any [])))
```

- The class is a struct embedding `jrt.Object`, the object header (§5.2). The field `val` is
  `F_val`; untyped, it is `Object`, whose Go type is `any` (§5.1).
- `Reduced` is final, so its values are `*Reduced` pointers; a call `(.deref r)` on one is a
  direct Go method call that gc can inline (§5.3).
- `IDeref` is a Go interface. Its marker `Is_IDeref` makes Go's structural typing nominal: only
  classes that implement `IDeref` in Java have it (§5.5).
- Method names carry their descriptor: `deref()Object` is `Deref__O` (§4.4). The constructor is
  an allocation function `Reduced_New_O` and a body `Ctor_O`; `super()` of `Object` is empty.
- `Ref` converts to `any` without turning `null` into a non-nil interface (§5.6); `GetClass` and
  `ToString` are generated for every concrete class, the others of `Object`'s methods come from
  the header (§5.8). The class's member table, which reflection reads, is in the package's
  generated tables file (§5.11).

## 3. Equivalence

### 3.1 What is compared

A translated class is correct when it **behaves** as the same class on the JVM Arbace: the same
results, side effects, printed output and exceptions (class and message) for the same
operations. There is no structural comparison as for class files (classes/SPEC.md §3) or Go
objects (SPEC.md §3): the check is differential testing (B1-PLAN.md, point 3 and step 4):

1. **jrt's own tests**, against the JVM, for what jrt hand-writes (strings, reflection,
   monitors, threads) and for the translated JDK classes where Arbace's tests do not reach
   (regex on a corpus of patterns, `BigDecimal` and number printing, `Formatter`, the
   collections' contracts).
2. **Per class, in plan step 4's order** (`Util`, `Murmur3`, `Numbers`, the collections,
   `Symbol`/`Keyword`/`Var`/`Namespace`, seqs, `LispReader`, printing, `RT`): the same
   operations in a Go test program and in a Clojure script on the JVM, the printed results
   compared.
3. **Clojure's test suite** through the evaluator (plan step 5), recorded per namespace.

### 3.2 Deviations

What a translated class may do differently, by design. Each is either unobservable to
Arbace's tests or listed here with its reason; anything else is a bug.

| | deviation | why |
|---|---|---|
| V1 | `StackOverflowError` is not thrown by translated code: Go's stack grows to 1 GB, then the program dies (`fatal error: stack overflow`, not recoverable) | Go has no recoverable stack overflow; the evaluator counts its own depth and throws (§10.5, §16 Q15) |
| V2 | `OutOfMemoryError` is not thrown: the program dies | Go's allocator |
| V3 | identity hash codes differ (they differ between JVM runs too); so does the iteration order of identity-keyed hash maps | jrt assigns them (§5.8) |
| V4 | a lambda's class is one class per functional interface (`Predicate$$Lambda`), not one per call site | §7.11 |
| V5 | stack traces show translated frames (class, method, `.clj` file and line), and some jrt-internal frames are elided; no frames of hidden classes | §7.9.6 |
| V6 | a thread that uses an instance of a class while another thread runs that class's static initializer does not wait for it | guards are elided in instance code (§6.2) |
| V7 | a data race on a non-`volatile` field of interface type (two words in Go) can tear | §8.3; c2g lists such fields, the race detector finds the races |
| V8 | `finalize` is never called | deprecated in Java; jrt has no finalization (references use `runtime.AddCleanup`) |
| V9 | the classes of the closed world only: `Class.forName` of anything else fails, nothing can be defined at run time but the classes the evaluator defines through `Compiler$Dyn` (`deftype`, `defrecord`, `reify`, interfaces made at run time, and `proxy`'s classes, of `Object` or of a class of §5.12's list: §5.12, §10.4; amendments E4 and X1, accepted 2026-10-09); likewise the data of §12's reduced scope only (six charsets since amendment S6, the root locale's data) | D6 (§12) |
| V10 | Thread priorities, thread groups and daemon status have no effect on scheduling; the program ends when `main` returns or `System.exit` is called | goroutines |
| V11 | an implicit `NullPointerException` (Go's nil dereference, c2g's receiver check `jrt.NN`, §7.9.5) has a null message, as the JVM's with `-XX:-ShowCodeDetailsInExceptionMessages`; the native `getExtendedNPEMessage` returns null, so the JVM's helpful messages are not reproduced (accepted 2026-10-08, amendment J6) | the message describes the failing expression, which only c2g knows: reproducing it would need c2g to pass each check the expression's text. Helpful messages may come later, generated by c2g |
| V12 | a `SoftReference` holds its referent strongly: it is cleared only by `clear()`, never by the collector (accepted 2026-10-08) | Go has no pointers sensitive to memory pressure; Arbace's soft references are `DynamicClassLoader`'s class cache (cut) and `CharacterName`'s cache (§8.4) |
| V13 | an uncontended monitor costs about 14 ns, not the 10 ns first estimated (accepted 2026-10-08, amendment T16) | performance only: two reads of the thread's number and two compare-and-swaps on the header, the exit's needed because another thread may inflate the word or set the identity hash meanwhile (§8.1) |

Everything else Java specifies is kept, including the corner cases that are easy to lose in a
translation: integer overflow, division of `MIN_VALUE` by `-1`, shift masking, float-to-int
saturation, `-0.0`, NaN comparisons, no fused multiply-add (§7.4), `null` in string
concatenation and `switch`, the order of evaluation (§7.2), the order of static initializers
(§6.2), `finally` on every exit (§7.9), monitor release on exceptions (§8.1), the `Integer`
cache and `==` on boxes (§7.6), the exception thrown by each implicit check (§7.9.5).

## 4. The target: packages, files, names

### 4.1 Inputs and the closed world

c2g runs on the JVM Arbace (`target/stage2`, as the other tools, `bin/lib/tools.bash`), as
`bin/c2g`, implemented in `arbace/c2g/` (namespaces `arbace.c2g.*`). Its input is a set of
class forms files and a jrt manifest:

1. **Arbace's runtime**, `arbace/lang/*.clj`, with the Go-build variants of §4.6.
2. **The translated JDK closure**: j2c's conversion of the jdk26u source files of the closure
   (JAVA-SURFACE.md, "The translation closure": 184 files, 422 classes; with D5's regex), with
   their own variants; since phase 2C, 223 files (below), and since step 5's phase 2B, 340
   (streams, the functional interfaces, `java.time.Instant`, `java.sql`'s `Timestamp` and
   `Date` as other modules' files by amendment S2; [JRT-SOURCES.md](JRT-SOURCES.md), "The
   closure as grown").
3. **The jrt manifest** `go/arbace/jrt/manifest.edn` (§9.1): for every hand-written jrt class,
   its Java declaration (supertypes, the members jrt implements, with descriptors and
   modifiers). c2g enters these as declarations and checks every use against them.

c2g enters all classes into one class environment (classes/SPEC.md §9.2: the compilation set,
then sources), runs the analyzer over every class body (`resolve-members!`, `analyze-class!`,
`add-bridges!`, as `arbace.classes.compiler` does before emitting), and translates the analyzed
classes. The closed world is exactly these classes. A reference to any other class is an error
that names the class and the referring member; the cure is a variant (§4.6) or a jrt stub
(JAVA-SURFACE.md, "cut": a class that must exist as a name, whose members throw
`UnsupportedOperationException`).

**jrt's own Java and the JDK's variants** (amendment K1, accepted 2026-10-09). The JDK input is
`bin/jrt-convert`'s output: the closure's jdk26u files, the jdk26u files added to it (the
classes jrt had as stand-ins, the standard streams' `PrintStream` and its interfaces,
`SignedMutableBigInteger`), and **jrt's own Java**, `overlay/jdk/java.base/` (under the JDK's
paths), which replaces or adds to jdk26u's files where a JDK class's source needs what is cut
(`java.nio`'s encoders and decoders, the VM's file natives): `java.io.FileDescriptor`,
`FileInputStream`, `FileOutputStream`, `OutputStreamWriter`, `InputStreamReader`, and
`jdk.internal.jrt.HostFiles`, `StandardStreams`, `CaseInsensitiveComparator`. It is Java, not
hand-written Go in jrt, because it extends translated classes (`Writer`, `OutputStream`), which
jrt's own build cannot name; j2c converts it and it is checked against javac with the closure,
then c2g translates it like any JDK class. What only Go can do is a `native` method of it
(static, with primitive and array parameters, so that jrt builds without the translated types:
§9.1). A class of jrt's Java that stands for a JDK class jrt cannot translate (a nested class
of a hand-written one) is registered under the JDK's name, by c2g's table `java-names`
(`arbace.c2g.out`): `jdk/internal/jrt/CaseInsensitiveComparator` answers `getName` as
`java.lang.String$CaseInsensitiveComparator`. The JDK's Go-build variants are in
`overlay/jdk/variants/` (§4.6), read whenever the JDK closure is an input.

**Files in the closed world** (amendment FS6, accepted 2026-10-10; JRT-NOTES.md, "Files"). D6's
cut of `java.io.File` is reversed: `File`, `FileSystem` and its Unix implementation
(`UnixFileSystem`, `DefaultFileSystem`, files of `src/java.base/unix/classes`, KIND `unix`:
JRT-SOURCES.md, FS1), `FileReader`, `FileWriter`, `java.net.URL` with its handlers (`file:`,
and `http:`, `https:`, `jar:` for their syntax only) and the `file:` connection, and a subset of
`java.nio.file` (jrt's own `Path`, `Files` and `jdk.internal.jrt.HostPath`, with jdk26u's
`Paths`, options and exceptions; decision FS2 in JRT-NOTES.md) are in the world. Their Go-build
variants: `UnixFileSystem` (its natives replaced by static natives on path strings, §9.1, over
§9.4's `HostFS`), `File` (`toPath`; `TempDirectory` without `StaticProperty` and
`SecureRandom`), `DeleteOnExitHook` (a `Runtime` shutdown hook), `URL`, `URL$DefaultFactory`,
`URLStreamHandler` (`hashCode` and `hostsEqual` compared host names while `InetAddress` was
outside the world; with sockets, below, the variant is gone and they are the JDK's),
`URLConnection` (no MIME table), `sun.net.www.ParseUtil` and `URI` (`decode` without
`CharsetDecoder`), the `file:` handler (no `Proxy`), `HexFormat`, `jdk.internal.util.Exceptions`
(the JDK's default `jdk.includeInExceptions`). Connections other than `file:`,
`RandomAccessFile`, channels and `java.nio`'s coders stay outside the world.

**Sockets in the closed world** (amendments NT1-NT5, accepted 2026-10-10; JRT-NOTES.md, "Sockets
(go-net)"). D6's cut of sockets is reversed: jdk26u's `ServerSocket`, `Socket`, `SocketImpl`,
`InetAddress`, `Inet4Address`, `Inet6Address`, their `InetAddressImpl`s, `InetSocketAddress`, the
socket options and exceptions, the resolver interface, `InterruptedIOException` and
`sun.net.PlatformSocketImpl` are in the world (JRT-SOURCES.md), over jrt's own Java
`jdk.internal.jrt.HostSocketImpl` (the platform `SocketImpl`, in place of `NioSocketImpl`) and
`jdk.internal.jrt.HostNet` (static natives, §9.1, over §9.4's `NetHost`). Their Go-build
variants: `SocketImpl` (`createPlatformSocketImpl`), `Socket` (its `VarHandle`s as `Unsafe`
compare-and-set, no SOCKS wrapper, no JFR events), `ServerSocket` (no `DelegatingSocketImpl`),
`InetAddress` (no native library or `SharedSecrets`, the built-in resolver only, a cache of
`CachedLookup`s expiring when used: NT4), `Inet4Address`, `Inet6Address` (no `NetworkInterface`),
`Inet4AddressImpl`, `Inet6AddressImpl` (natives over `HostNet`, no `isReachable`),
`IPAddressUtil` (no `CharBuffer`) and `jdk.internal.util.Exceptions`
(`jdk.includeInExceptions` from the system property, JDK 26's default otherwise). Code kept from
jdk26u in `HostSocketImpl` and these variants is recorded in LICENSE.md (NT3).
`NetworkInterface`, `Proxy`, the SOCKS and HTTP-tunnel impls and the resolver providers stay out.

**The JDK's resource data** (amendments RD3, RD4, accepted 2026-10-10; JRT-NOTES.md, "The JDK's
resource data"). For `\N{name}` and `CANON_EQ`, `java.text.Normalizer` and `jdk.internal.icu`'s
normalizer are in the world, with java.nio's heap buffers through which ICU's loader reads its
data: `Buffer`, `ByteOrder`, `StringCharBuffer`, the buffer exceptions, and files the JDK build
generates beyond the measured closure's (`added-gensrc`: `ByteBuffer`, `CharBuffer`, `IntBuffer`,
their heap classes, the char and int views of a byte buffer in either byte order, and
`jdk.internal.misc.ScopedMemoryAccess`), which `bin/jrt-convert` generates as the JDK build does
and compares byte for byte with the build's (RD4; JRT-SOURCES.md). `MemorySegment` and
`MemorySessionImpl` are in the world as the types the buffers name. Variants: `Buffer` without
its static initializer (`SharedSecrets`' `JavaNioAccess`), `ScopedMemoryAccess` without its VM
natives (`closeScope0` throws). jrt's own Java gains `java.util.zip.InflaterInputStream` (RD3),
the `InputStream` constructor only, which inflates its whole input at the first read through a
native over Go's `compress/zlib`, so that `CharacterName` reads `uniName.dat` unchanged; the
VM's `Inflater` stays outside the world. `bin/jrt-convert` also makes the data these classes read
(RD4): `uniName.dat` as the JDK build's `Gendata.gmk` makes it, ICU's `nfc.nrm` and `nfkc.nrm`,
into `.tmp/jrt/data`, compared with the build's module image; the program embeds them (§10.3).

The analyzer runs as it runs for the JVM, with one difference: classes the closed world takes
from source are never resolved by reflection on the running JDK, even when the JDK has them
(the analyzer's environment prefers the compilation set; c2g makes that a rule and fails on a
reflective entry). The shims' declarations come from the manifest, so that a member jrt does
not implement is an error at translation, not at run time.

The analyzer's own reflection follows the same rule: where `Compiler` decides by asking a
class, the analyzer asks the class's declaration when the class has class forms in the world.
So `clj-fi-method` (`Compiler.FISupport`'s functional interface test: is the parameter type a
`@FunctionalInterface`, and which is its single abstract method) reads the declaration's
`@FunctionalInterface` annotation and abstract methods for a class from source, instead of
reflecting on the running JDK, which found nothing for c2g's JDK closure and made c2g's world
compile a `checkcast` where the JVM adapts the fn. The JVM build's choice is unchanged
(amendment E8, accepted 2026-10-09).

**Roots, reachability and slices** (amendment C2, accepted 2026-10-09). c2g translates what is
reachable from the program's roots (rapid type analysis: instantiated classes, virtual calls
resolved over their instantiated subtypes, lambdas' functional interfaces, static
initializers). Roots are given as `--root C` (every public member of `C`), `--root C#name` (its
members of that name; `C#<init>`, its constructors), and `--root 'C$*'`, which names `C` and
every named class nested in it with all their public members (`arbace.lang.Compiler$*`;
amendment E7, accepted 2026-10-09). Reachability does not follow reflection: a program that
calls members by name must root the members it may call (§10.6; amendment P2, accepted
2026-10-09).

- `--slice REGEX` restricts **translated code**, not the closed world: the method bodies of classes
  whose internal name matches no slice (`--slice '^arbace/lang/'`) are not translated, but those
  classes still exist in Go with their types, member tables and class objects, and their methods are
  stubs that throw `UnsupportedOperationException("c2g: not translated: ...")`. (Dropping classes
  from the world instead left the analyzer to fill them in by reflection on the running JDK.) -
  **Operation-level stubs.** An operation that names something outside the closed world (a class,
  member or descriptor neither translated nor in the manifest) does not fail its method: it becomes
  `((inst jrt/C2g_Missing T) why)`, typed by the Go type the context wants, which throws
  `UnsupportedOperationException("c2g: ...")` when reached; the rest of the method is translated.
  `report.edn` lists every method with missing parts and why, which is the list of what still needs
  a variant or a jrt class (§4.6). This refines the "error" above: a reference outside the world is
  reported, and fails at run time only if reached.

**Classes and fields at the world's edge** (amendments C4 and P3, accepted 2026-10-09):

- **Cut classes.** A class literal of a class outside the world (`Thread.State` in a switch
  map) is a class registered by name in the package's `c2g_cut.go`, with no members and no
  instances; `instance?` of it is false (`(jrt/C2g_Discard x)`, which evaluates `x`).
- **Reflected marker interfaces.** A JDK interface without class forms and without methods
  (`RandomAccess` and similar markers) that translated classes implement is declared from
  reflection: its marker and class, no code, so that `instance?` works. Interfaces with methods
  are never declared that way (an attempt let `IntStream` into the world).
- **Dropped fields.** A field whose type is outside the closed world is dropped from its struct
  and from the member tables. A store of the constant `null` into it, on `this` or static, is
  dropped too (it can hold nothing else: `PrintWriter`'s `psOut = null`, a `PrintStream`); any
  other access is an operation-level stub.
- **Constructors, catches and superinterfaces outside the world** (amendment M8, accepted
  2026-10-09). A constructor whose parameters name a class outside the world does not exist in
  Go, and a `this(...)` or `super(...)` call of one is an operation-level stub; a `catch` clause
  whose classes are all outside the world is dropped (nothing can throw them); a superinterface
  outside the world stands for its own superinterfaces in the class's `Interfaces` (JDK 21's
  `SequencedCollection` between `List` and `Collection`, before step 5's phase 2B translated
  it).
- **The REPL's cut classes and absent members** (amendment M3, accepted 2026-10-09). With
  `--program` (§10.6), c2g reads the embedded namespaces' sources (§10.3), collects the class
  names their forms mention (qualified symbols and their namespaces, import lists, metadata), and
  registers those the JVM has (from the JDK, `arbace.lang` or `arbace.asm`) and the world lacks,
  with the JDK classes their public members name (one level), as cut classes whose member tables
  come from the JVM's reflection and whose members throw `UnsupportedOperationException("... is
  not in the Go build")`; likewise, the member tables of translated classes list their public
  methods and constructors that do not exist in Go (`RT.toUrl`, `FileInputStream(File)`),
  throwing. Code naming them analyzes, as on the JVM, and fails when it runs (D6's "names exist
  and throw"; `arbace.c2g.embed`, `out/*absent-members*`).
- **A cut class's constants are readable** (amendment CF1, accepted 2026-10-10). In a cut
  class's member table, a static final field of a primitive type or `String` with a constant
  value answers that value (read by reflection when c2g runs) instead of throwing; its other
  members still throw. The class forms' analysis reads ASM's `Opcodes` this way
  (CLASSFORMS-REPL.md; `arbace.c2g.out/cut-constant`).
- **Library cuts** (amendment SL7, accepted 2026-10-10). Besides the classes the embedded
  namespaces name, `--program` cuts a fixed list of JDK classes that libraries loaded from
  `ARBACE_PATH` import, when the world lacks them (`arbace.c2g.embed/library-cuts`:
  `java.util.jar.JarFile`, `JarEntry`, `java.net.URLClassLoader`, `java.io.FileReader`,
  `java.text.SimpleDateFormat`, named by test.generative's runner, tools.namespace,
  java.classpath and tools.reader), so that those libraries load on the Go build (Clojure's suite
  runs its test.generative phase there).

### 4.2 Go packages

Go packages cannot import each other in a cycle; Java packages do (`java.lang` and `java.util`
use each other, and the hand-written `String` uses translated classes that use `String`). So:

- c2g computes the **strongly connected components** of the Java package dependency graph of
  the closed world (a package depends on another when one of its classes names a class of the
  other in a declaration or in code).
- **A Go package holds one or more whole components**, by a fixed table, and the table must
  induce no Go import cycle (checked). The table for B1a:

| Java packages | Go package | forms namespace |
|---|---|---|
| `java.*`, `jdk.internal.*`, `sun.*` (the translated closure, the shims, the stubs) | `arbace/jrt` | `go.arbace.jrt` |
| `arbace.lang` (the runtime, the evaluator's classes) | `arbace/lang` | `go.arbace.lang` |
| the program's `main` (§10.6) | `arbace/cmd/arbace` | `go.arbace.cmd.arbace` |

`arbace/lang` imports `arbace/jrt`; jrt imports only Go's standard library and the patched
runtime's two functions (§9.3). The module is `arbace` (`program.edn`: `{:module "arbace" :go
"1.27"}`, BUILD.md); the build is `bin/g2c build` over the forms tree. The synthetic check of
§13.6 shows that gc handles a package of jrt's size.

### 4.3 Files and layout

- **One Go file per Java source file** (per class forms file), holding the Go forms of its
  top-level class and every class nested in it, in the order of the class forms. Its name is
  the class forms file's name for `arbace/lang` (`PersistentVector.go`) and, in jrt, the Java
  source path with `/` replaced by `_` (`java_util_HashMap.go`,
  `jdk_internal_math_FloatingDecimal.go`; jrt's own Java likewise, by its JDK path:
  `java_io_FileDescriptor.go`, `jdk_internal_jrt_StandardStreams.go`, amendment K1). A
  class forms file holding several top-level classes gives one Go file for all of them,
  named after it (`FxClasses.go`; amendment W1, accepted 2026-10-09). A name whose last `_`-separated element before `.go`
  is a GOOS, a GOARCH or `test` gets `_c2g` appended, since gc reads those suffixes as build
  constraints.
- **Generated files per package**: `c2g_classes.go` (the member tables and class registrations,
  §5.11), `c2g_strings.go` (the string literal pool, §7.5), `c2g_frames.go` (the frame table,
  §7.9.6), `c2g_cut.go` (cut classes, §4.1), `c2g_up.go` (the `Up_` conversions, §5.6),
  `c2g_lambdas.go` (the adapters `F_Fn` of the package's functional interfaces, §7.11); in
  `arbace/lang` also `c2g_dyn.go` (the dynamic object type and `Compiler$Dyn`'s natives, §5.12)
  and `c2g_fromfn.go` (the functional interfaces' `FromFn`, §7.11); in `arbace/jrt` also
  `c2g_support.go` (c2g's helpers `C2g_*` and what c2g generates into jrt's package, §11) and
  `c2g_init.go` (the eager initialization of replaced stand-ins, below) (amendments C2-C5, E4,
  E5, accepted 2026-10-09).
- The forms tree follows Go forms SPEC §4.1: c2g writes `target/c2g/go/arbace/jrt.clj` (`ns`,
  `go/package`, `load`s), `target/c2g/go/arbace/jrt/java_util_HashMap.clj` and so on. It is
  regenerated, not tracked, as converted Go is (g2c decision 9).
- **jrt's hand-written forms** are source, tracked, in a forms root of their own (§16 Q19),
  `go/arbace/jrt/*.clj`; the build merges the two trees into one program root before
  `bin/g2c build`. c2g's files and jrt's are told apart by name: hand-written files never begin
  with `java_`, `jdk_`, `sun_` or `c2g_`.
- **Stand-ins** (amendment J9, accepted 2026-10-08). Until c2g's output joins the build, jrt's
  hand-written classes throw, catch and implement JDK classes c2g will translate (the
  exceptions, `Serializable`, `Comparable`, `CharSequence`, `Runnable` ...). The files
  `go/arbace/jrt/standin_*.clj` hold stand-ins for them, tracked, in exactly the shapes this
  spec gives translated classes (§4.4's names, `C_I` and `Self_C` for non-leaf classes,
  `C_New_...` and `Ctor_...`, forwarders, `Ref`, `GetClass__Class`, `Clone__O`, `C_InstanceOf`,
  `C_Cast`, `C_class`), so that jrt and translated code meet at the final names now. They are
  generated (`test/jrt/standins.clj`), except `standin_timeunit.clj`, hand-written because the
  generator writes only exceptions and interfaces. `standin_reflect.clj` (generated by
  `test/jrt/standins_reflect.clj`: the wrappers `Number`, `Boolean`, `Character` ... `Void`,
  `java.lang.reflect.Type`, `InvocationTargetException`) is one of them (amendment R19, accepted
  2026-10-08). When c2g's output joins, the translated classes define the same names; jrt's
  core uses only their constructors and interfaces, so a change of leafness in the larger
  closed world does not affect it. **All 64 stand-ins are translated classes in a c2g program**
  (amendment K3, accepted 2026-10-09; the 26 without sources before phase 2C were added to the
  JDK input from jdk26u, K1): no `standin_*` class remains hand-written there, each replaced as
  below. The stand-in files are not deleted: they remain **only for jrt's own build**
  (`bin/jrt build`, `bin/jrt test` without c2g, which has no translated classes); nothing in
  them is meant to stay otherwise. `bin/jrt build|test --prog DIR` builds or tests jrt within a
  c2g program root, the stand-ins replaced.
- **Replaced stand-ins** (amendment C3, accepted 2026-10-09). A stand-in whose class has class
  forms in the world is replaced by the translation: c2g filters jrt's hand-written forms for
  the program (a stand-in file whose classes are all replaced is dropped, otherwise the
  stand-in's forms are removed, its groups and helpers included), and the replacing class's
  static initializer and members are reachability roots. A replacing class whose static
  initialization is not trivial (§6.2) is **initialized eagerly**, by `init()` of package jrt
  (`c2g_init.go`), because jrt's hand-written code reads its statics directly as the stand-in's
  package variables, without `C_Init()`. This is the one exception to §6.3.
- **Positions.** c2g writes no Go positions (Go forms SPEC §10, forms built by a program): the
  printer lays them out as gofmt would. For stack traces (§7.9.6) every function form carries
  the `:line` of its class forms member, and the build uses the printer's `--line-file` option,
  so that gc's line tables name the class forms file (`arbace/lang/PersistentVector.clj:137`).
  **As built** (amendment W1, accepted 2026-10-09): the analyzer keeps each form's line on its
  node and each member's line on the member (`arbace.classes.analyze/analyze`,
  `arbace.classes.parse/parse-member`; no emitted byte changes). c2g binds the line of the node
  it translates and puts it, as `:c2g/line`, on every statement it writes and on each method's
  declaration, and lays the forms text out on those lines: a form whose line the text has not
  yet passed is written on that line, so that the reader's `:line` is the class forms line;
  otherwise it gets an explicit `^{:line n}`, and so does every list inside it without a line of
  its own (the reader keeps an explicit `:line`, Go forms SPEC §10.1). Package-level
  declarations are written sorted by line (Go does not care about their order), so explicit
  lines are rare. Every build of c2g's output (`bin/c2g-check` included) uses `--line-file`,
  so gc's line tables, Go's tracebacks, pprof and jrt's `StackTraceElement`s name the class
  forms file and line (`at c2g.fixtures.FxOuter.classify(FxClasses.clj:144)`). Costs measured:
  the forms grow 11%, a warm build about 10%, the executables shrink 4%.

### 4.4 Names

Every generated Go identifier is exported, so that `arbace/lang` can use what jrt declares and
the reflection tables can refer to everything. Java names are kept verbatim where Go allows;
`$` (not allowed in Go identifiers) becomes `_`.

**Types.**

| Java | Go |
|---|---|
| class, interface, enum, record `p.Outer$Inner` | `Outer_Inner`: the binary name after the package, `$` → `_`; `X` prepended when the first character is not an upper-case letter |
| anonymous and local classes `Outer$1`, `Outer$1Local` | `Outer_1`, `Outer_1Local` (the analyzer's javac names) |
| the interface type of a non-leaf class `C` (§5.3) | `C_I` |
| the adapter of a functional interface `F` (§7.11) | `F_Fn` |

**Classes jrt provides** (amendment Y1, accepted 2026-10-09). A class that jrt provides and c2g
does not translate is named by the Go name jrt registers for it (its class variable's name less
`_class`), which may differ from the name the table derives from the Java name: jrt's
`ReentrantLock_ConditionObject` is `AbstractQueuedSynchronizer$ConditionObject`.

**Members of classes** (methods with a receiver on the class's struct; fields of the struct):

| Java | Go | example |
|---|---|---|
| instance field `f` | `F_f` | `cnt` → `F_cnt`, `_meta` → `F__meta` |
| instance method `m` with descriptor `(P1...Pn)R` | `M_P1_..._Pn__R`, with `M` the name with its first letter upper-cased; `M__R` without parameters | `equiv(Object)boolean` → `Equiv_O__Z`; `count()int` → `Count__I`; `invoke(Object,Object)Object` → `Invoke_O_O__O` |
| its implementation, in a non-leaf class (§5.4) | `Impl_` + the method's name | `Impl_Equiv_O__Z` |
| constructor body `<init>(P1...Pn)V` | `Ctor_P1_..._Pn`, or `Ctor` | `Ctor_I_I_PersistentVector_Node_O1` |

**Package members** (one Go package holds many classes, so class members are prefixed by the
class's Go name):

| Java | Go | example |
|---|---|---|
| static field `f` of `C` | `C_f` (a `var`, or a `const` for a constant, §6.1) | `PersistentVector_EMPTY`, `Var_dvals` |
| static method of `C` | `C_` + the mangled method name | `Util_Equiv_O_O__Z`, `Murmur3_HashInt_I__I` |
| allocation and construction (`new C(...)`) | `C_New_P1_..._Pn`, or `C_New` | `Reduced_New_O` |
| interface `J`'s default, private and static methods | `J_` + the mangled name, taking the receiver first for the first two | `Iterator_ForEachRemaining_Consumer__V` |
| the `Class` object of `C` | `C_class` (`class` cannot be a Java field) | `Reduced_class` |
| class initialization (§6.2) | `C_Init()`, with its state `C_init` | |
| `instanceof C`, checked cast to `C` (§5.7) | `C_InstanceOf(x any) bool`, `C_Cast(x any) T` | |

**Codes of the descriptor.** The parameter and return codes are: `Z B C S I J F D` for the
primitive types and `V` for `void`, as in descriptors; `O` for `java.lang.Object`; the Go type
name for any other class (`String`, `ISeq`, `PersistentVector_Node`); an array is its element's
code followed by its dimension count (`O1` for `Object[]`, `I1` for `int[]`, `String2` for
`String[][]`). The descriptor is the **erased** one the analyzer records, return type included:
two methods of one class differ in it exactly when the JVM tells them apart, so bridge methods,
covariant returns and overloads all get distinct names, and an override always has its
overridden method's name. That is what lets Go's method sets do Java's dispatch (§5.4).

**Generated members that are not Java's** have names no Java member produces: Java's fields
start with `F_`, Java's methods contain `__`, constructors start with `Ctor`; so `Ref` (§5.6),
the markers `Is_J` and the accessors `Self_C` (§5.3, §5.5) are free. Per class, the package
members `C_class`, `C_Init`, `C_init`, `C_clinit` (the static initializer, §6.2),
`C_InstanceOf`, `C_Cast`, `C_New...` and `Up_C_I` (§5.6) are generated.

**Package-private methods across packages.** A package-private method is not overridden by a
method of a subclass in another package (JVMS 5.4.5); with the same Go name it would be. When
the closed world has such a pair, c2g appends `_pp_` and the Go package's name to the
package-private method's names (`Tailoff__I_pp_lang`) and reports it. None is expected in B1a's
world (checked at translation). **As built** (amendment W4, accepted 2026-10-09): the check
compares **Java packages**, not Go packages (`java.util` and `java.util.concurrent` share jrt,
and a pair across them would collide just the same), and a pair found is an **error** (c2g
exits 1, `report.edn` `:package-private-overrides`), not renamed: the cure, a member entry in
the rename table below, is added when one is found. None has been found in any world built
(the check's, the whole program's, all of `arbace.lang`).

**Locals, parameters, labels.** Local and parameter names are kept, munged as Clojure's
`munge` does for the characters Go does not allow (hand-written class forms use Clojure names:
`i-1` becomes `i_1`; `$` becomes `_` everywhere), and with `_` appended when they equal a Go
keyword, a predeclared identifier c2g's output uses (`len`, `cap`, `append`, `copy`, `make`,
`new`, `panic`, `recover`, `any`, the Go type names) or an import name (`jrt`): `len_`. The
same munging applies to field and method names, which the class forms take verbatim from the
JVM's rules (a JVM name may contain `-`). Labels are keywords (`:L1`, `:L2`, numbered per
function) and exist only where a jump targets them (gc rejects unused labels).

**Collisions.** These rules are injective except in three cases: a Java name containing `_`
followed by what reads as codes (`get_O()` against `get(Object)`), two parameter classes of one
overload with the same Go name from different packages, and a class whose name contains `_`
colliding with a nested class (`A_B` against `A$B`). c2g checks every Go package and every
struct and interface for duplicate names and fails with both sources named. A **rename table**
in c2g (data, `arbace.c2g.names/renames`) resolves a reported collision by giving one class or
member another name. It holds these entries: `jdk/internal/util/ByteArray` → `Jdk_ByteArray`,
since jrt's `ByteArray` is `byte[]` (§5.9) (amendment C6, accepted 2026-10-09);
`java/sql/Date` → `Sql_Date`, since jrt's `java.util.Date` is `Date`, and
`java/util/stream/Tripwire` → `Stream_Tripwire`, since `java.util.Tripwire` is `Tripwire`
(amendment S1, accepted 2026-10-09); `sun/net/www/URLConnection` → `Www_URLConnection`
(`java.net.URLConnection`'s subclass), the URL handlers, all named `Handler`, →
`File_Handler`, `Http_Handler`, `Https_Handler`, `Jar_Handler`, and `java/net/Proxy` →
`Net_Proxy`, since jrt's `Proxy` is `java.lang.reflect`'s (amendment FS4, accepted
2026-10-10; `Socket(Proxy)` names it too, JRT-NOTES.md "Sockets (go-net)"); `sun/text/Normalizer` → `Sun_Normalizer`, since `java.text.Normalizer` is
`Normalizer` (amendment RD5, accepted 2026-10-10).
The check runs after translation (`arbace.c2g.checks`) over every Go package's package-level
names and every type's method names, across c2g's generated files **and jrt's hand-written
ones** (the stand-in files excepted, since replaced stand-ins are removed, §4.3); a collision is
an error (c2g exits 1) listed in `report.edn` `:errors` (amendment W4, accepted 2026-10-09).

### 4.5 What a class becomes

| Java declaration | Go forms |
|---|---|
| class `C` | `go/type C (struct Super F_x ...)`, the methods of §5.4, its `C_I` interface if non-leaf, `C_New_*`, `C_class`, statics (§6) |
| interface `J` | `go/type J (interface ...)`, `J_` functions for default, private and static methods, statics |
| enum, record | a class, with the members the analyzer derives (§7.13) |
| annotation type | an interface with its marker only, and its `Class` (annotations are not kept, §12) |
| nested, inner, local, anonymous class | a class of its own, with the analyzer's outer and capture fields (§7.12) |
| `defmodule`, `defpackage` | nothing |

### 4.6 Go-build variants

JAVA-SURFACE.md's "rework" (about 300 lines of `RT`, `Compiler` and `Reflector`) and the cuts
need the Go build's `arbace.lang` to differ in places from the JVM's. The source stays one: the
differences are **variant files**, `arbace/lang/go/<Class>.clj` (and, for the translated JDK,
whose class forms j2c regenerates, `overlay/jdk/variants/<Class>.clj`, in the class's package:
amendment K1, accepted 2026-10-09), read by c2g only:

```clojure
(in-ns 'arbace.lang)
(c2g/variant RT
  (method ^:public ^:static classForName ^Class [^String name]     ; replaces RT.classForName(String)
    (Class/forName name))
  (c2g/cut ^:public ^:static loadClassForName ^Class [^String name]) ; removes a member: its head only
  (c2g/add (field ^:public ^:static ^:final ^String PLATFORM "go")))  ; adds a member
```

- A member form in `c2g/variant` replaces the class's member of the same name and erased
  signature (a static initializer replaces the static initializer); `c2g/cut` removes one;
  `c2g/add` adds one. A variant naming no existing member is an error.
- **Indexed static initializers** (amendment C1, accepted 2026-10-09). A class with several
  `static-initializer` forms (`Compiler`) has them addressed by index, 0-based in source order:
  `(c2g/cut (static-initializer n))` removes the n-th, and a replacement is a
  `static-initializer` tagged `^{:c2g/nth n}`. Without the tag a static initializer replaces the
  only one (index 0). Fields are cut by their declaration, `(c2g/cut (field ...))`.
- **Nested classes** (amendment E1, accepted 2026-10-09). A variant names a nested class by its
  binary name, `(c2g/variant Compiler$ObjExpr ...)`, and replaces, cuts or adds its members as
  for a top-level class (`Compiler`'s back end lives in its nested classes).
- **Added classes and natives** (amendment P1, accepted 2026-10-09). `c2g/add` of a `defclass`
  adds a member class (`Compiler$EvalFn`; `RT$HostWriter` until amendment K2), translated
  like the rest. A variant
  may declare `^:native` methods: they are calls of the Go function `C_M..._native` (§9.1),
  which is how the Go build's `arbace.lang` reaches Go-only code (the host streams,
  `jrt.AdaptFn`, `Dyn`) without a jrt class with a Java API for it.
- **Erased packages** (amendment E2, accepted 2026-10-09). A variant file's top-level
  `(c2g/erase "p/")` erases the classes of a package outside the closed world (by internal-name
  prefix): an expression whose type is an erased class is `nil`, its operands not evaluated (the
  erased code must be pure where it is skipped: ASM's factories are); a store into a field or an
  array element of an erased type is dropped; members whose descriptors name an erased class
  do not exist in Go, as for any class outside the world (§4.1). An operation of a non-erased
  type on an erased value still throws (an operation-level stub). The Go build erases ASM so,
  instead of cutting `Compiler`'s about 150 `emit` methods and 120 ASM fields one by one, and
  the variant copies no line of `Compiler` (§10.1, EVAL-PLAN Q7).

```clojure
(in-ns 'arbace.lang)
(c2g/erase "arbace/asm/")                            ; every ASM value is nil in Go
(c2g/variant Compiler
  (c2g/cut (static-initializer 0))                   ; the first static initializer: ARG_TYPES
  ^{:c2g/nth 1}
  (static-initializer                                ; replaces the second
    (set! COMPILER_OPTIONS
          (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                                   (Symbol/intern "*compiler-options*") nil)))))
(c2g/variant Compiler$ObjExpr                         ; a nested class, by its binary name
  (method ^:synchronized getCompiledClass ^Class [this] nil))
(c2g/variant RT
  (c2g/add                                           ; a member class with natives (abridged;
                                                     ; P1's interim class, gone with K2)
    (defclass ^:static ^:final HostWriter
      :extends Writer
      (method ^:private ^:static ^:native hostFlush ^void [^int fd]))))
```
- **The JDK's variants** (amendment K1, accepted 2026-10-09) are the same forms, `in-ns` the
  class's package: `overlay/jdk/variants/CaseFolding.clj` replaces
  `jdk.internal.lang.CaseFolding`'s field `expanded_case_cps`, whose initializer collected a
  map's keys through a stream (`java.util.stream` is cut), with a loop in an added method.
  `ByteArray` and `ByteArrayLittleEndian` read and write byte by byte instead of through
  `VarHandle`s (`java.lang.invoke` is not in the Go build), and `UUID.randomUUID` draws its
  bytes from the host (`SecureRandom` is not in the world) (amendment M7, accepted 2026-10-09).
  Step 5's phase 2B added `Random` (its seed's offset by `Unsafe.objectFieldOffset(Class,
  String)`), `URI` and `ReferencePipeline`, `Collectors` (no `SharedSecrets`), `GathererOp` and
  `Gatherers` (`Stream.gather`'s evaluation and `mapConcurrent` throw), `Instant` (`toString`,
  `now`) and `UUID.nameUUIDFromBytes` (MD5 from the host, a native): JRT-NOTES.md, "Phase 2B
  (step 5)". java.util's completion added `TreeMap` (its sorted build without the
  `ObjectInputStream` parameter, for the copy constructor, `clone`, `putAll` and
  `addAllForTreeSet`), `BitSet` (`valueOf(byte[])` and `toByteArray` by shifts, no
  `ByteBuffer`) and `RandomGenerator` (`isDeprecated` false: no annotations, §12) (amendment
  JB2, accepted 2026-10-10). A replacing member's parameter tags are written as j2c writes the
  original's, generics included (`^{:tag (SortedMap K (? extends V))}`): the variant finds the
  member by them.
- Variants are class forms like any other and are analyzed with the class. c2g reports the
  number of replaced, cut and added members per class, and the differential tests (§3.1) cover
  them as the rest.
- c2g also reports every remaining reference to a cut or unknown class (§4.1), so the list of
  what still needs a variant is always known.

## 5. Types and objects

### 5.1 The Go type of a Java type

| Java static type | Go type | zero value |
|---|---|---|
| `boolean`, `byte`, `short`, `char` | `bool`, `int8`, `int16`, `uint16` | `false`, `0` |
| `int`, `long`, `float`, `double` | `int32`, `int64`, `float32`, `float64` | `0` |
| `java.lang.Object`, and a type variable erasing to it | `any` | `nil` |
| `java.lang.String` | `*jrt.String` | `nil` |
| a leaf class `C` (§5.3), final ones included | `*C` | `nil` |
| any other class `C` | `C_I` (a Go interface) | `nil` |
| an interface `J` | `J` (a Go interface) | `nil` |
| `boolean[]` ... `double[]` | `*jrt.BooleanArray` ... `*jrt.DoubleArray` | `nil` |
| any array of references | `*jrt.RefArray` | `nil` |
| a type variable with a bound | the Go type of its erasure | |
| `void` | no result | |

A Java value has one Go representation whatever path it took, so that identity, `==` and
reflection see the same thing: an object is always a pointer to its class's struct, held in a
variable of the Go type of the static type. Generics are erased as the analyzer erases them:
c2g's output has no Go type parameters (jrt's helpers may).

### 5.2 Objects

- **A class is a struct.** Its first field embeds the superclass's struct (`jrt.Object` at the
  root), followed by its own instance fields in declaration order: `(struct APersistentVector
  ^int32 F_cnt ...)`. Superclass fields are reached through Go's promotion (`t.F_hash` for a
  field of `ASeq` in a subclass) or, when a subclass hides them, through the embedded struct
  (`(.-F_x (.-B t))`).
- **The header** is `jrt.Object`, one 64-bit word holding the identity hash and the lock state
  (§5.8, §8.1). It also makes every object non-empty: Go may give distinct zero-size variables
  the same address, Java's `new Object()` must be distinct.
- **Allocation** is `C_New_...`: it runs `C_Init()` (§6.2), allocates the struct (`(addr (lit
  C))`, all fields at Java's default values, which are Go's zero values) and calls the constructor
  body `Ctor_...` with the new object. A constructor body begins with the superclass constructor
  call the analyzer gives (`(super. ...)` or `(this. ...)`, with the outer instance and the
  captures of §7.12), then the field initializers and instance initializers the analyzer folded
  in, then the body; `Object`'s constructor is empty and not called.
- **The constructor body of a non-leaf class** (§5.3) takes the whole object after the struct,
  as `Impl_` methods do (§5.4): `Ctor...(t *C, this C_I, args...)`, so that `this` passed on
  or called virtually in a constructor is the object being constructed; a subclass's `super`
  call is `(.Ctor (.-B t) this ...)` (§15.3: `(.Ctor (.-APersistentVector t) t)`, where the leaf
  `PersistentVector`'s `t` is the whole object). A leaf class's constructor bodies take no
  `this` (amendment J2, accepted 2026-10-08).
- An abstract class has no `C_New_*`. An interface has no struct.

### 5.3 Leaf classes and class interfaces

A class value must be usable as any of its superclasses, and Go has no subtyping of structs. So
a class `C` with subclasses in the closed world gets a Go interface `C_I`, and values whose
static type is `C` have that Go type. It lists:

- `Self_C() *C`, the accessor of the `C` part of the object (promoted from `C`'s struct into
  every subclass, it returns the embedded `C`), which is also `C`'s marker;
- every virtual method of `C` (its own and inherited, abstract or not: the instance methods
  that are not private, static or constructors), by their mangled names;
- the superclass's interface, embedded (`jrt.Object_I` at the root), and the interfaces `C`
  implements.

A **leaf class** (no subclass in the closed world) needs no such interface: its values are
`*C`, its virtual methods are Go methods on `*C`, and calls on them are direct and inlinable.
Every final class is a leaf, and no abstract class is (it gets its interface even without
subclasses, so that code using its type compiles). Most of `arbace.lang` is: 326 of its 352
classes (the other 422 of the 774 class files of stage 2's `arbace/lang` are interfaces;
measured by reflection over `target/stage2`), among them `PersistentVector` and its `Node`,
`Keyword`, `Symbol`, `Cons`, `PersistentHashMap`'s nodes. The 26 others are `AFn`,
`AFunction`, `RestFn`, `Obj`, `ASeq`, the abstract collections and references
(`APersistentVector`, `ARef` ...), `PersistentTreeMap`'s nodes and the bases of `Compiler`'s
expressions. This is class hierarchy analysis over the closed world (principle 4): it is sound
because nothing can subclass a class at run time in B1a (deftype and reify implement
interfaces only, §5.12; `gen-class` is cut, D6) but a proxy of a class of a fixed list, which
c2g provides for ahead of time with a Go type per class (§5.12, amendment X1, accepted
2026-10-09): a translated, non-final class the list names is not a leaf, since its `DynSub_C`
is a subclass in the program (amendment Z1, accepted 2026-10-10: leafness counts the proxy types
c2g adds to the closed world; hand-written classes keep jrt's leafness), and a proxy of a leaf
class throws. A class made non-leaf by a later change changes the Go type of its uses, which
c2g recomputes as a whole.

A hand-written class whose Java superclass is translated embeds that superclass's struct once
it exists. Until then it embeds `jrt.Object` and has the superclass's methods itself:
`AtomicInteger` and `AtomicLong` extend `Number`, so they embed `Object` with `Number`'s methods
until the translated `Number` joins the build, and then embed `Number` (non-leaf, so they
become `Number_I` values where the static type is `Number`) (amendment T12, accepted
2026-10-08). **Done** (amendment S3, accepted 2026-10-09): they embed `Number` and define its
methods themselves (`atomic.clj`; `(pos? (AtomicInteger. 1))` casts to `Number`).

A hand-written class is non-leaf where the world extends it, as the manifest's generator
declares (`test/jrt/manifest.clj`). Since amendment S3 (accepted 2026-10-09) these include
`java.util.Date` (the translated `java.sql.Timestamp` extends it) and `ForkJoinTask` (the
translated `RecursiveTask` and `CountedCompleter` extend it).

### 5.4 Virtual dispatch

Java dispatches a call `x.m(...)` on the dynamic class of `x`; inside the selected method `this`
is the whole object, so a self-call is virtual too and `this` keeps its identity. Go embedding
promotes the embedded struct's methods with the *embedded struct* as receiver, which loses
both. So c2g writes, for a non-leaf class `C` and its subclasses:

- **The implementation** of each method `m` that `C` declares with a body is a Go method on
  `*C` named `Impl_m...`, taking the receiver twice: `t *C` (the struct, for field access) and
  `this C_I` (the whole object, for self-calls, identity and passing `this` on). It is promoted
  into subclasses, which is what makes `super` calls simple.
- **The dispatch method** `m...` is a Go method on `*D` for every concrete (not abstract) class
  `D` and every virtual method of `D`: on the declaring class it calls `t.Impl_m...(t, ...)`; on
  a subclass that does not override `m` it is a **forwarder** to the inherited implementation,
  the same call, which Go's promotion resolves to the nearest declaring ancestor's `Impl_m...`.
  Go's interface call on `C_I` (or on any interface listing `m`) then reaches the most derived
  implementation with the whole object as `this`: Java's dispatch. Only methods whose
  implementation is promotable (jrt's `Object.hashCode` and `equals`, and `Enum`'s `name`,
  `ordinal`, `hashCode` and `equals`: §5.8, §7.13, §9.1) get no forwarder.
- **Hand-written non-leaf classes** (`Throwable`, `Enum`, `Record`, `Thread`, `ThreadLocal`;
  `Date` and `ForkJoinTask` since amendment S3, §5.3)
  follow the same rules: their `Impl_` methods and constructor bodies take `this C_I` after the
  receiver (§5.2), their dispatch methods exist on the concrete class, and stand-ins (§4.3) and
  c2g's subclasses write forwarders to them.
- **Hand-written leaf classes and translated default methods** (amendment S4, accepted
  2026-10-09). A hand-written leaf class implementing a translated interface lacks the
  interface's default methods it does not define, which jrt's own build cannot name; c2g writes
  them into jrt's package as forwarders to the default method (`arbace.c2g.out/
  jrt-default-forwarders`: `CharSequence`'s `chars` and `codePoints` on `String`,
  `StringBuilder` and `StringBuffer`).
- A **leaf** class's own methods need no split: the dispatch method holds the body, `this` is
  `t`. It still has forwarders for inherited methods.
- **Abstract** classes have implementations but no dispatch methods (their struct does not
  implement their own interface, and no instance exists); abstract methods have no Go method
  at all until a concrete subclass's (the analyzer guarantees one, as javac does).
- **Final** methods dispatch like the others (forwarders included); being final changes only
  what the analyzer allows.
- **Private** instance methods are not virtual: `Impl_m...` (or, in a leaf, the method on `*C`)
  called through the declaring struct, `(.Impl_m t this args)`, never through an interface.
- **Static** methods are package functions `C_m...` (§4.4).
- **`super` calls** `(.m super args)` call the superclass's implementation on the embedded
  struct: `(.Impl_m (.-B t) this args)`, where `B` is the direct superclass (promotion finds the
  nearest declaring ancestor). `Outer/super` calls (§7.12) go through the outer instance the
  same way.
- **Bridge methods** (derived by the analyzer: covariant returns, generic overrides) are
  dispatch methods that call the bridged method and convert its result (§5.6).

Forwarders are the price of keeping Java's dispatch on Go's interfaces; they are small and gc
inlines most of them. §13.6 measures the code size.

### 5.5 Interfaces

- A Java interface `J` is a Go interface listing `Is_J()` (its **marker**), its abstract and
  default methods by mangled names, and, embedded, its superinterfaces and `jrt.Object_I`
  (Java lets any interface-typed value call `Object`'s methods).
- A class implementing `J` (directly or through a superinterface) has `Is_J` on its struct (an
  empty method, promoted to subclasses). Without markers Go would let any struct with the same
  method names satisfy `J`; with them, `x.(J)` succeeds exactly for Java's implementors.
- **Default methods** are package functions `J_m...(this J, ...)`; a class that inherits a
  default without overriding it has a forwarder to it (the maximally specific default, as the
  analyzer selects it, JVMS 5.4.3.3). `(.m J/super args)` in a class calls `J_m...` directly.
- **Static and private** interface methods are package functions, the private ones with the
  receiver first.
- **Constants** of interfaces are static fields of the interface (§6.1).

### 5.6 `null`

- `null` is Go's `nil` in every Go type of §5.1.
- **The typed-nil rule.** A Go interface holding a nil pointer is not `nil`. So whenever a value
  of a pointer Go type (a leaf class, `String`, an array) is converted to an interface Go type
  (`any`, `C_I`, `J`) and may be `nil`, c2g converts it **nil-preservingly**: to `any` with the
  generated method `Ref` (`(.Ref p)`, which every concrete struct has, §2), to another interface
  with a function `Up_T_I(p) I` that c2g generates in the using package for each pair it needs.
  The conversion is plain (implicit in Go) when the value cannot be `nil`: `this`, a `new`, a
  string literal, a value just dereferenced or checked. This happens in every assignment
  context: arguments, returns, field and array stores, bindings, operands of `identical?`.
- **NullPointerException** comes from Go's own checks where Go makes them at Java's point
  (§7.9.5): a method call on a nil interface, a field access or array access through a nil
  pointer. A method call on a nil *pointer* is not checked by Go (the method runs with a nil
  receiver), so c2g checks the receiver of a call with a pointer receiver with `(jrt/NN p)`
  (an inlinable generic function that throws `NullPointerException` on `nil` and returns `p`),
  after the arguments are evaluated (JLS 15.12.4.4: arguments with side effects go to
  temporaries first), unless it is known not to be `nil`; gc removes the checks it can prove
  redundant.

### 5.7 `instanceof`, casts, conversions between reference types

- **Up-casts** (to a supertype) are Go conversions, nil-preserving where §5.6 says.
- **`instance?`** of a class or interface `T`: `(T_InstanceOf x)`, a generated function that is
  a Go type assertion `x.(G)` to `T`'s Go type `G` (a pointer for a leaf class, else the
  interface), with the dynamic-object check of §5.12 for interfaces dynamic objects can
  implement. `nil` gives `false`.
- **`cast`** (checkcast): `(T_Cast x)` returns `nil` for `nil`, the value converted to `G` if
  it is an instance, and otherwise throws `ClassCastException` with the JVM's message
  (`class X cannot be cast to class Y`). When the static type already guarantees the result
  (the analyzer marks casts of `null` and up-casts, classes/SPEC.md §5.5), the cast is a plain
  conversion.
- Assertions are always comma-ok forms inside these functions: a failing `x.(T)` panics in Go
  with a `*runtime.TypeAssertionError`, and on a nil interface, where Java's checkcast succeeds.
- `instance?` and `cast` of array types compare the array's component class (§5.9).
- **The dynamic flag** (amendment O4, accepted 2026-10-10): bit 31 of the header's low word
  marks the objects of classes made at run time (set by `MarkDynamic` when c2g's `Dyn` and
  `DynSub_C` objects are made, before they are published; the identity hash keeps 31 bits,
  §5.8); the nominal check of an interface's `instance?` and `cast` asserts `jrt.Dynamic` only
  for flagged objects (`jrt.IsDynamic`, inlined: a load of the header through the interface's
  data word, every Java object being a pointer to a struct whose first field is the header).
- `instance?` of a cut class (§4.1, a class outside the world registered by name) is `false`,
  its operand still evaluated (`jrt.C2g_Discard`); `instance?` of a reflected marker interface
  is the ordinary assertion to its marker (amendment C4, accepted 2026-10-09).
- gc's per-site caches make an assertion to an interface about 1-2 ns when the receiver's type
  is predictable (§13.1).

### 5.8 The `java.lang.Object` protocol

`jrt.Object_I` is the interface every Java object implements: `Self_Object`, `GetClass__Class`,
`HashCode__I`, `Equals_O__Z`, `ToString__String`, `Clone__O`. jrt's `Object` struct implements
the ones that need no dynamic `this`, promoted into every class: `Self_Object`, `HashCode__I`
(the identity hash) and `Equals_O__Z` (identity, comparing headers). Each concrete class
gets, unless it overrides them: `GetClass__Class` (returns `C_class`), `ToString__String`
(`getClass().getName() + "@" + Integer.toHexString(hashCode())`, through `jrt.Object_toString`,
which calls the dynamic `hashCode`), `Clone__O` (a shallow copy of the struct with a fresh
header if `C` implements `Cloneable`, else `CloneNotSupportedException`), and `Ref`.

- On a value whose Go type is `any`, `Object`'s methods are calls of jrt helpers that assert
  `jrt.Object_I` (`(jrt/Equals x y)`, `(jrt/HashCode x)`, `(jrt/ToString x)`,
  `(jrt/GetClass x)`), throwing `NullPointerException` on `nil`. On any other Go type they are
  direct calls.
- **Identity** (`identical?`, `==` on references) is Go's `==` on the two values converted to a
  common Go type (`any` when they differ), nil-preservingly (§5.6). Every Java object is a
  pointer, so the comparison never panics.
- **The identity hash** is assigned on first use and stored in the header; it is never the
  address (gc may keep an object that does not escape on the stack, and stacks move).
  (Correction, 2026-10-08, to follow jrt: the hash comes from one global atomic sequence
  mixed by a 32-bit finalizer, 31 bits and never 0, not from a per-thread xorshift sequence
  as HotSpot's; V3 permits it, the values differing from the JVM's anyway.)
  The header's low word holds the hash in bits 0-30 and the dynamic flag in bit 31 (§5.7,
  amendment O4, accepted 2026-10-10); its high word is the lock word (§8.1).
  **A class's identity hash** (amendment U2, accepted 2026-10-09) is its name's
  `String.hashCode`, 31 bits and never 0, set when the `Class` is made (`jrt.presetClassHash`:
  `Define`, `DefineDynamic`, the primitive and array classes), so that classes hash alike in
  every run of the program: the image of prepared namespaces (§10.3) keeps the layout of its
  hashed collections keyed by classes, and orders over such collections are the same in every
  run.
- `wait`, `notify`, `notifyAll` are jrt functions on the header (§8.1); `finalize` is not used
  (V8).
- **`new Object()`** (lock objects, sentinels) is `(jrt/Object_New)`, of Go type `any`: a
  pointer to an unexported struct of jrt's whose class is `jrt.Object_class`. **A clone's
  header** is reset with the header's method `ClearHeader`: `Clone__O` copies the struct, then
  calls `(.ClearHeader c)` on the copy (promoted from the header), so that it has neither the
  identity hash nor the lock of the original. The header is a plain `uint64` read and written
  with `sync/atomic`'s functions, not an `atomic.Uint64`, so that copying a struct passes
  `go vet`'s copylocks check (amendment J8, accepted 2026-10-08).

### 5.9 Arrays

- **Primitive arrays** are `*jrt.IntArray` and its kin: a struct with the header and `A []int32`
  (`[]bool`, `[]int8`, `[]int16`, `[]uint16`, `[]int64`, `[]float32`, `[]float64`).
- **Reference arrays** of every element type are one Go type, `*jrt.RefArray` (header, `Comp
  *jrt.Class` the component class, `A []any`), because Java's arrays are covariant (`String[]`
  is an `Object[]`) and Go's slices are not. Multi-dimensional arrays are reference arrays of
  arrays.
- **One allocation** (amendment O2, accepted 2026-10-10): a reference array of up to 32 slots is
  one Go object, the `RefArray` followed by its slots (`unsafe.Slice` over a trailing `[N]any`,
  N rounded up to Go's size classes); longer arrays are a header and a slice. The Go type and
  the slice `A` stay, so c2g's output does not change.
- `(new T/n d)` is `jrt.NewIntArray(d)` or `jrt.NewRefArray(T_class, d)` (`NegativeArraySize
  Exception` for `d < 0`); several dimensions `jrt.NewMultiArray(cls, dims...)`, whose `cls` is
  the array class itself (`int[][]`'s, `(.ArrayClass (.ArrayClass jrt/Prim_int))`) and which
  checks every size before allocating and returns a `*jrt.RefArray` (amendment J3, accepted
  2026-10-08); an initializer
  `(new T/1 [a b])` a composite literal of the slice wrapped by `jrt.IntArrayOf` or
  `jrt.RefArrayOf`.
- `(aget a i)` is `(aget (.-A a) i)`, `(alength a)` is `(len (.-A a))`; Go's bounds check panics
  at Java's point and becomes `ArrayIndexOutOfBoundsException` (§7.9.5). A read from a
  reference array whose static element type is not `Object` converts the element to the
  element's Go type with an unchecked comma-ok assertion (the array store check guarantees it).
- **`aset`** into a reference array is `(.Store a i v)`, which checks the store against `Comp`
  (`ArrayStoreException`) unless the static element type is final or a leaf class, or `Object`
  with a statically `Object[]`-created array, where it cannot fail; then it is a plain slice
  store.
- `clone` of an array copies the slice: `(.Copy a)`, a clone at the array's own Go type;
  `System.arraycopy` is jrt's `Arraycopy` (which `System_Arraycopy_O_I_O_I_I__V` calls), with
  HotSpot's order of checks and messages and overlapping copies; `jrt.IndexOutOfBounds(i, n)`
  throws the bounds exception with Java's message; `java.lang.reflect.Array.newInstance(Class,
  int)` is `jrt.Array_NewInstance_Class_I__O` (amendment J3, accepted 2026-10-08).
- `getClass` of an array is the array class for its component (jrt creates array classes on
  demand, with Java's names `[I`, `[Ljava.lang.String;`).

### 5.10 Generics

Erased, as the analyzer erases them: a type variable is its bound's Go type, a parameterized
type its class's. The analyzer has already written the erasure casts javac's `TransTypes`
inserts (classes/SPEC.md §5.5), which are `C_Cast` calls here. `Signature` attributes have no
counterpart; generic reflection is cut (JAVA-SURFACE.md): its methods that jrt keeps return
plain `Class` objects (§12, amendment R16).

### 5.11 Class objects and member tables

Reflection is hand-written in jrt over tables c2g generates (decision 1 of JAVA-SURFACE.md):
`Compiler` and `Reflector` are translated unchanged and call `Class.getMethods` and the rest,
which read these tables.

**Per class**, the generated file `c2g_classes.go` of its package declares `C_class`, a
`*jrt.Class`, and registers it at Go package initialization (data only, no Java code runs)
with a `jrt.ClassInfo`, in two steps (amendment J1, accepted 2026-10-08):

- `C_class`'s variable initializer, a `jrt.Define` call, gives the hierarchy only: `Name`,
  `Modifiers`, `Kind`, `Super`, `Interfaces`, `Declaring`, `Simple`, `Go`.
- An `init` function sets the function and table fields (`IsInstance`, `Init`, `Enum`,
  `FromFn`, `Fields`, `Methods`, `Ctors`) through `(.Info C_class)`, which returns the class's
  `*jrt.ClassInfo`.

Go rejects the initialization cycles that Java's mutually referring classes make (class A's
member table names class B, B's names A), and a function literal's body counts as a reference;
an `init` function's references do not. So nothing given to `Define` may refer to a `C_class`
of the program: `C_InstanceOf`, if it were given there, must not refer to `C_class`. jrt does
the same for its own classes and its stand-ins (§4.3).

| field | content | used by |
|---|---|---|
| `Name` | the binary name, `"arbace.lang.PersistentVector$Node"` | `getName`, `forName`, printing |
| `Modifiers` | `java.lang.reflect.Modifier` bits (from `InnerClasses` flags for nested classes, as `Class.getModifiers`) | `Modifier.isPublic` etc. |
| `Kind` | class, interface, enum, record, annotation | `isInterface`, `isEnum` ... |
| `Super`, `Interfaces` | the direct supertypes | `getSuperclass`, `getInterfaces`, `supers`, `isAssignableFrom` |
| `Declaring`, `Simple` | the enclosing class for member classes; the simple name | `getDeclaringClass`, `getSimpleName` |
| `Go` | the qualified name of the class's Go type (amendment J1) | stack traces (§7.9.6), class initialization's messages |
| `Fields` | per field (`jrt.FieldInfo`): name, type class, modifiers, `Get func(obj any) any`, `Set func(obj, v any)` | `getFields`, `getField`, `Field.get/set` |
| `Methods` | per method (`jrt.MethodInfo`): name, parameter classes, return class, modifiers (varargs, bridge, synthetic among them), `Invoke func(this any, args []any) any` | `getMethods`, `Method.invoke` |
| `Ctors` | per constructor (`jrt.CtorInfo`): parameter classes, modifiers, `New func(args []any) any` | `getConstructors`, `newInstance` |
| `IsInstance` | `C_InstanceOf` | `isInstance`, `cast`, `instance?` |
| `Init` | `C_Init` | `Class.forName(name, true, ...)`, static field access and `newInstance` through reflection |
| `Enum` | the constants in order | `getEnumConstants`, `Enum.valueOf` |
| `FromFn` | for a functional interface: wraps an `IFn` into its adapter (§7.11); set on every interface `Compiler.FISupport` may adapt, the `@FunctionalInterface` ones (amendment R13), by `arbace/lang`'s `c2g_fromfn.go` (§7.11, amendment E5) | `jrt.AdaptFn`, `Reflector`'s functional interface adaptation, `isAnnotationPresent(FunctionalInterface)` |

The tables' entry types (jrt's `class.clj`):

```clojure
(go/type MethodInfo (struct ^string Name ^{:tag (slice (* Class))} Params ^{:tag (* Class)} Return
                            ^int32 Modifiers ^{:tag (func [any (slice any)] [any])} Invoke))
(go/type CtorInfo (struct ^{:tag (slice (* Class))} Params ^int32 Modifiers
                          ^{:tag (func [(slice any)] [any])} New))
(go/type FieldInfo (struct ^string Name ^{:tag (* Class)} Type ^int32 Modifiers
                           ^{:tag (func [any] [any])} Get ^{:tag (func [any any])} Set))
```

**The entries** (amendment R12, accepted 2026-10-08):

- `Name`: the Java name (`"charAt"`; constructors have none).
- `Params`, `Return`, `Type`: `Class` objects, as expressions valid in the `init` function:
  `jrt/Prim_int` ... `jrt/Prim_void`, `C_class`, `(.ArrayClass X)` for arrays. `Params` is nil
  for no parameters.
- `Modifiers`: the JVM's access flags as `getModifiers` reports them: for methods `ACC_BRIDGE`
  (0x40), `ACC_VARARGS` (0x80) and `ACC_SYNTHETIC` (0x1000) included, which `isBridge`,
  `isVarArgs` and `isSynthetic` read, and `native` as the class file has it; for fields
  `ACC_ENUM` and `ACC_SYNTHETIC` too. `toString` masks them as the JDK does.
- `Invoke` is never nil (jrt throws `AbstractMethodError` for a nil one); `Set` is nil for a
  final field (`Field.set` throws `IllegalAccessException`); `New` is nil for an abstract class
  (`newInstance` throws `InstantiationException`).
- A member appears once, in the table of the class that declares it; jrt computes inheritance
  (`getMethods` merges as `java.lang.PublicMethods` does). The order is the table's (the JVM's is
  unspecified), and a name's overloads are listed in the order the JVM's reflection gives them
  (c2g's tables by `getDeclaredMethods` of the class on the JVM running c2g, jrt's by
  `getMethods`, `test/jrt/tables.clj`), since `Reflector`'s choice among applicable overloads
  follows the order (amendment M4, accepted 2026-10-09: `(Math/floorDiv c 1461)` with `c` an
  `Object` chose `(int, int)` before). Interfaces list their abstract, default and static methods
  and their constants; an enum's `values`, `valueOf` and constants are ordinary members.
- A `volatile` field that is not final has a `Set`, a volatile write (§8.2), as its `Get` is a
  volatile read (amendment M8, accepted 2026-10-09; the tables had no `Set` for them).

**The value convention** (amendment R11, accepted 2026-10-08). Invokers take and return values
in their Go representation (§5.1), not boxed: a primitive as the Go value of its type (`bool`,
`int8`, `uint16`, `int16`, `int32`, `int64`, `float32`, `float64`; `void` as nil), a reference as
itself (`any`, nil for null). jrt's `Method.invoke`, `Constructor.newInstance` and
`Field.get`/`set` convert around them:

- they check the receiver of an instance member (`NullPointerException` for null,
  `IllegalArgumentException("object of type X is not an instance of C")`), the argument count
  (`"wrong number of arguments: n expected: m"`) and each argument: a primitive parameter gets
  `jrt.Unbox` (unboxing, then widening, JLS 5.1.2; null or anything else is
  `IllegalArgumentException`), a reference parameter an instance check (`"argument type
  mismatch"`);
- they box a primitive result with `jrt.Box` (the wrappers' `valueOf`, with their caches);
- an exception the member throws (Go run-time errors mapped as `jrt.Catch` maps them) becomes an
  `InvocationTargetException`, as the JVM's does.

So an invoker is one call (in package `jrt`; elsewhere the jrt names are qualified):

```clojure
;; String.charAt(int), an instance method of a leaf class
(lit MethodInfo :Name "charAt" :Params (lit (slice (* Class)) Prim_int) :Return Prim_char :Modifiers 0x1
     :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.CharAt_I__C (assert (* String) this) (assert int32 (aget args 0)))))
;; a static method: the receiver is ignored
(lit MethodInfo :Name "valueOf" :Params (lit (slice (* Class)) Object_class) :Return String_class :Modifiers 0x9
     :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (String_ValueOf_O__String (aget args 0))))
;; void: the call, then nil
(lit MethodInfo :Name "printStackTrace" :Return Prim_void :Modifiers 0x1
     :Invoke (fn ^any [^any this ^{:tag (slice any)} args] (.PrintStackTrace__V (assert Throwable_I this)) nil))
(lit CtorInfo :Params (lit (slice (* Class)) String_class) :Modifiers 0x1
     :New (fn ^any [^{:tag (slice any)} args] (String_New_String ((inst As (* String)) (aget args 0)))))
(lit FieldInfo :Name "PI" :Type Prim_double :Modifiers 0x19 :Get (fn ^any [^any o] Math_PI))
(lit FieldInfo :Name "id" :Type Prim_int :Modifiers 0x1
     :Get (fn ^any [^any o] (.-F_id (.Self_Base (assert Base_I o))))
     :Set (fn [^any o ^any v] (set! (.-F_id (.Self_Base (assert Base_I o))) (assert int32 v))))
```

- The receiver is asserted to the class's Go type: `(* C)` for a leaf, `C_I` for a non-leaf
  class, `J` for an interface. The call is the method's ordinary call, so it dispatches
  virtually: a superclass's `Method` invoked on a subclass's instance runs the override.
- A primitive argument is `(assert T (aget args i))`; a reference argument
  `((inst jrt/As T) (aget args i))`, which maps nil to T's zero value; an `Object` argument is
  `(aget args i)` itself.
- A default method's invoker is the interface call; a static method of an interface `J` calls
  `J_M...`; a bridge's invoker calls the bridge's own Go method.
- A static field's `Get`/`Set` read and write the package variable `C_f` (or the constant); jrt
  runs `ClassInfo.Init` before a static field access and before `newInstance`.

**Which members.** Every public member a class declares (methods, constructors, fields, static
or not), bridges and synthetic methods included; and every declared member of the classes on
c2g's list for `getDeclared*` use, so far `java.util.Random` (`getDeclaredField("seed")`, with
`Unsafe`, §9.2) (amendment R15, accepted 2026-10-08). `Object`'s table, jrt's, lists its public
methods and its protected `clone`, which `NewInstanceExpr.gatherMethods` offers `deftype` and
`reify`. This is what `Compiler` and `Reflector` see on the JVM, where Clojure reaches only
public members (§16 Q13).

**Tables list, reachability translates** (amendment P2, accepted 2026-10-09). A table lists
every public member whether or not reachability (§4.1) reached it; a member not reached is a
stub, and its invoker throws `UnsupportedOperationException("c2g: not translated: ...")`.
Reachability does not see the target of a reflective call, so a program that calls members by
name (`Reflector`, the evaluator's interop) must root the public members of every class it may
call so: `bin/c2g-check` roots the classes its fixtures reflect on, and the REPL's program roots
every public member of every built-in class (§10.6).

**Go types for `Unsafe`.** For every class whose fields reach `Unsafe.objectFieldOffset` (or
every class, if simpler), the same `init` function calls `(jrt/RegisterGoType C_class
((inst reflect/TypeFor C)))`, giving jrt the class's Go struct type (§9.2) (amendment T11,
accepted 2026-10-08).

- `Class` objects for primitive types, `void` and arrays are jrt's. The registry maps names to
  classes for `Class.forName` and `RT.classForName` (§10.3).
- **`getClass` by a field** (amendment M2, accepted 2026-10-09). A class with an instance field
  named `c2g$class` (a `Class`) answers `getClass` with that field's value when it is set, else
  with its own class (`arbace.c2g.decls`). The evaluator's `EvalFn` has one, so that each
  evaluated fn shows its run-time class (§10.2).

### 5.12 Dynamic objects

The evaluator (plan step 5) must create, at run time, objects that implement interfaces of the
closed world: `deftype`, `defrecord` and `reify` instances. Go cannot create types at run time,
so c2g generates one Go type for all of them, **`Dyn`** in `arbace/lang`'s `c2g_dyn.go`:

- `Dyn` is a struct with the header, a pointer to its run-time class (a `*jrt.Class` the
  evaluator creates, with its name, supertypes and dynamic member tables) and its field values
  (`[]any`).
- It has a Go method for **every method of every interface of the closed world** a dynamic type
  may implement (all public interfaces, with `Object`'s methods), and every interface marker
  (no class accessor: a dynamic type extends only `Object`). Each method
  looks its implementation up by a slot number c2g assigns to the mangled name
  (`c2g_dyn.go` lists them) in the class's slot table (filled by the evaluator with closures)
  and calls it with the object and the arguments; a missing one throws `AbstractMethodError`.
  `Object`'s methods default to the header's.
- So `x.(J)` succeeds for every `Dyn`. `J_InstanceOf` and `J_Cast` therefore add one check when
  the assertion succeeded on a `Dyn`: whether the object's run-time class implements `J`.
  Interfaces no dynamic type may implement (none in B1a's world besides those of jrt's
  internals) skip it.
- Since `Dyn` satisfies all interfaces structurally, a translated method that receives a `Dyn`
  through an interface it implements dispatches to the evaluator's closure, exactly as a JVM
  class generated by `deftype` would be called.

**As built** (amendment E4, accepted 2026-10-09; it makes the description above precise and
replaces the type-word compare):

```clojure
(go/type Dyn (struct jrt/Object ^{:tag (* DynClass)} D ^{:tag (slice any)} F))
```

- **The slot table is per class**: `DynClass` holds `Cls` (the `*jrt.Class`), `Slots []IFn`
  (one fn per slot, nil when unset), the implemented interfaces with all their
  superinterfaces, and the methods by name and descriptor (for the member table). Slots 0-2 are
  `toString`, `hashCode` and `equals`; unset, they are the header's.
- **The interfaces covered** are every public interface translated, with its superinterfaces,
  and jrt's public hand-written interfaces that have a cast function (`Future`,
  `ExecutorService`, `Executor`, `Lock`, `Condition`; amendment Y2, accepted 2026-10-09): their
  hand-written `InstanceOf` and `Cast` make the nominal check too (`jrt.dynNominal`), so
  `(instance? java.util.concurrent.Future (future 1))` holds as on the JVM.
- **A method** calls its slot's fn with the object and the boxed arguments, and converts the
  result as compiled `deftype` methods convert it (`RT`'s casts, `intCast`, `longCast` ...;
  `Boolean` and `Character` unboxed). An unset method calls the most specific default method of
  an implemented interface, else throws `AbstractMethodError`.
- **The nominal check** is an interface assertion, not a compare of Go's type word (jrt's
  functions cannot name a type of `arbace/lang`): after a successful assertion to `J`, every
  interface's `InstanceOf` and `Cast` assert the value to the jrt interface `jrt.Dynamic` and,
  when it is one, require `(.DynImplements x J_class)`.
- **jrt's part** (`dyn.clj`): the interface `Dynamic` (`DynImplements`), `DefineDynamic` (the
  registration of a run-time class under its binary name, which a later `deftype` of the same
  name may replace; a class of the closed world cannot be replaced) and `Class.Descriptor` (the
  class's JVM descriptor, the evaluator's method keys).
- **The Java API** is `Compiler$Dyn`'s natives (§9.1), which c2g writes in `c2g_dyn.go`:
  `defineClass(name, interfaces, fields)`, `setMethod(class, name, params, ret, fn)`,
  `newInstance(class, fieldValues)`, `getField(o, i)`, `setField(o, i, v)`. Step 5 adds
  `defineClass` of an interface kind, for `definterface` and `defprotocol` (§10.4).

**Proxies of a class: `DynSub_C`** (amendment X1, accepted 2026-10-09). A proxy of `Object` is
a `Dyn`. A proxy of a class `C` must be a Go value of `C_I`, since translated code uses it as a
`C` (`(PrintWriter. w)` over a proxy of `Writer`), so for each class of a fixed list,
`arbace.c2g.model/proxy-supers` (`java.io.Writer`, `Reader`, `PushbackReader`, `InputStream`,
`OutputStream`, `BufferedWriter` (pprint's tests), `arbace.lang.APersistentMap`, and jrt's
hand-written `ThreadLocal`; each must be translated or hand-written, in the world and not
final; a translated one is non-leaf by being listed, amendment Z1), c2g writes a Go type
**`DynSub_C`** in `c2g_dyn.go`:

- a struct embedding `C`'s struct (so `Self_C`, `C`'s fields and its `Impl_` methods are
  promoted), with `Dyn`'s `D` and `F`, and a slot table of its own (`SlotMap`), which the
  proxy's `DynClass` holds;
- a method for every method of every interface of the world (as `Dyn`) and every virtual method
  of `C`, each calling its slot's fn; when the slot is unset or the fn answers the marker
  `Compiler$Dyn.superMarker()`, it runs `C`'s implementation (`(.Impl_m t t ...)`, the super
  call), else the interface's default method, else throws `AbstractMethodError` with HotSpot's
  text; `toString`, `hashCode` and `equals` fall back to `C`'s or the header's. `Dyn`'s own three
  methods honour the marker too;
- constructors for `C`'s non-private ones, which allocate the `DynSub_C` and run `C`'s
  initialization and constructor body, as a translated subclass's constructor does. c2g roots
  `C`'s constructors and instance methods up its superclass chain (`dyn/proxy-roots`), so that
  the implementations are translated.

`Compiler$Dyn` gains the natives `defineProxyClass(name, super, interfaces)` (over `Dyn` for
`Object`, with one private field, the fn map, and a no-argument constructor; else over
`DynSub_C`, with `C`'s constructors), `superMarker()` and `fillProxySlots(class, factory)`
(gives the superclass's methods reflection does not list, the protected ones of jrt's classes
such as `ThreadLocal.initialValue`, the fn `(factory name)`). `setMethod` on a proxy's method
that has a slot sets only the slot, so reflection through `C`'s member reaches the Go method
and `proxy-super`'s reflective call runs `C`'s implementation. The natives work on both kinds of
object through the Go interface `dynObject`. A proxy of a class outside the list, or of a leaf
class (`BitSet`: making it non-leaf would change every use), throws
`UnsupportedOperationException` "proxy of C is not in the Go build" when evaluated, so code
naming it loads. Each `DynSub_C` has about 650 methods; the seven add about 4 MB to the
executable. jrt's stack traces leave out their dispatch frames as they do `Dyn`'s.

**A duplicate method is `ClassFormatError`** (amendment X3, accepted 2026-10-09): a class made
at run time that defines a method twice with one descriptor (`(reify java.util.List (size [_]
10) java.util.Collection (size [_] 20))`) throws the JVM's `ClassFormatError` ("Duplicate method
name "size" with signature "()I" in class file ...") from `Evaluator.defineType` when it is
made, which the analyzer wraps in `CompilerException` as on the JVM. `ClassFormatError` is a jrt
stand-in (§4.3).

**Interpreted classes and the superclasses of the world** (amendment CF4, accepted 2026-10-10;
CLASSFORMS-REPL.md §2.3). `DynClass` gains a field `CF`, the `Compiler$CF$Klass` of a class of
class forms made at the REPL (nil for `deftype`, `reify`, `proxy`). The DynSub types are made for
`arbace.c2g.dyn/class-supers` too (`Throwable`, `Exception`, `RuntimeException`, `Error`,
`IllegalArgumentException`, `IllegalStateException`, `Enum`, `Record`, `AFn`; a leaf, such as
`UnsupportedOperationException`, cannot have one), so these classes can be proxied as well. An
interpreted class extending one of `sub-supers` (or an interpreted class that does) has DynSub_C
objects: c2g writes for each C, in `c2g_cf.go`, its allocation without a constructor
(`dynCfNew_C`: C's initialization, then the object), C's constructors on an allocated object
(`dynCfCtors_C`, by descriptor: the interpreted constructor's super call) and C's implementations
of its virtual methods (`dynCfSupers_C`, by name and descriptor: `super.m()`). The natives of
`Compiler$CFGo` (`defineClass`, `alloc`, `klassOf`, `setMethod` with the members' modifiers and a
virtual member table entry, `addCtor`, `addField`, `addStatic`, `canExtend`, `superCtor`,
`superCall`, `setEnum`, `setOuter`) are `arbace.c2g.dyncf`'s.

Functions (`fn*`) need no dynamic type: they are instances of ordinary evaluator classes (§10.2).

## 6. Statics and initialization

### 6.1 Static fields and constants

- A static field is a package-level `go/var` named `C_f`, of its Go type, **without an
  initializer**: its Java initializer runs in the class's initialization (§6.2), never in Go's
  package initialization, whose order is not Java's.
- A **constant** (a final static field with a constant initializer of primitive type, which the
  analyzer gives a `ConstantValue`) is a `go/const` with its type and `:val`:
  `(go/const ^{:tag int32 :val -862048943} Murmur3_C1 -862048943)`. The analyzer inlines
  constant reads as javac does; c2g writes a folded constant as a Go literal of its value, never
  as a Go constant expression (gc folds untyped constant expressions exactly and rejects
  overflow, while Java's folded value has already wrapped).
- A `String` constant is a `go/var` initialized at Go package initialization with the pooled
  literal (§7.5), so that it is the same object as every equal literal.
- `float` and `double` constants that Go constants cannot express (NaN, the infinities, `-0.0`:
  Go constants have no negative zero) are `go/var`s initialized from jrt's
  (`jrt.NaN64`, `jrt.NegZero64` ...).

### 6.2 Class initialization

Java initializes a class lazily, at its first active use (JVMS 5.5: `new`, a static method
call, a non-constant static field access, a subclass's initialization, reflection), once,
superclass first, with a per-class lock that lets the initializing thread re-enter (a class
in progress is seen half-initialized by its own thread) and makes other threads wait. Clojure's
runtime depends on this: `RT`'s static initializer loads `arbace/core`, which uses `Var`,
`Namespace`, `Symbol`, `Compiler` and the collections, whose initializers in turn read `RT`'s
statics; the JVM's order resolves these cycles. Go's package initialization is eager and orders
variables by their dependencies, rejecting cycles, so it cannot do this. c2g reproduces Java's
protocol:

- A class `C` with **non-trivial** initialization has a state `C_init` (`jrt.ClassInit`) and a
  function `C_Init()`, inlinable, whose fast path is one atomic load
  (`(when (not (.Done C_init)) (.Run C_init C_clinit))`); `C_clinit` is the class's static
  initializer: the superclass's `Super_Init()` first (and those of superinterfaces that declare
  default methods and have non-trivial initialization), then the static field initializers and
  `static-initializer` bodies in textual order, as the analyzer orders them for `<clinit>`.
- `jrt.ClassInit.Run` is JVMS 5.5's procedure: done → return; in progress by the current thread
  (the thread identity of §8.4) → return; in progress by another → wait; erroneous → throw
  `NoClassDefFoundError`; otherwise run the initializer, and on an exception mark the class
  erroneous and throw `ExceptionInInitializerError` wrapping it (unless it is an `Error`).
- **Trivial** initialization (no static initializer code, no non-constant static field
  initializer, a superclass and superinterfaces with trivial initialization) has no state and no
  function: the class is always initialized. Most classes are trivial (`Murmur3`, `Reduced`).
- **Where `C_Init()` is called**: at the start of every static method of `C` and of every
  allocation function `C_New_*` (the callee checks, so call sites need nothing); before a static
  field read or write of `C` from code outside `C` and its subclasses; in reflection's `Init`.
  Not in `C`'s own static methods' calls to each other, not in instance code of `C` or its
  subclasses for `C`'s statics (an instance exists, so initialization has begun; deviation V6),
  not for constants (inlined).
- **Benign initialization** (amendment W3, accepted 2026-10-09). A static method of `C` has no
  entry guard when `C`'s initialization is **benign** and the method reads no static field of
  `C` or of its superclasses. Benign: trivial, or a static initializer that only stores pure
  values (constants, arrays, objects of `C`'s own file built by constructors that only store
  fields) into `C`'s own statics, with no assertion and no Clojure constant, over a superclass
  whose initialization is benign too (`arbace.c2g.model/benign-init?`). Such an initialization
  is observable only through `C`'s statics, so running it later, at the first guarded access,
  cannot be told apart. `Integer`, `Long` and `Murmur3`-like classes are benign; `Numbers` is
  not (`new BigDecimalOps()` initializes `RT`). The guard kept gc from inlining small static
  methods (`Integer.rotateLeft`): `Murmur3.hashLong` went from 9.4 to 3.7 ns.
- The guard costs about 0.07 ns per access when inlined, 0.3 ns at a non-inlined static
  method's entry (§13.4).

### 6.3 Go's package initialization

Go's own package initialization runs only data: the class registrations of §5.11, the string
literal pool, jrt's tables. No Java code runs before `main` starts the program (§10.6), which
then initializes classes as Java would. The exception is the replaced stand-ins with
non-trivial initialization, initialized by jrt's `init()` (§4.3, amendment C3, accepted
2026-10-09): `c2g_init.go`'s `init` calls their `C_Init()` in the order of their names, and each
initializes what its initializer uses by §6.2's protocol, so their initializers run before
`main`.

## 7. Code

### 7.1 Overview

| class forms (classes/SPEC.md §5.1) | Go forms |
|---|---|
| `(do s1 s2)`, a body | statements in sequence |
| `(let [^T x e] ...)`, `^:mutable`, `^:const` | `(let [^G x e] ...)`; every Go variable is assignable; a `^:const` local is its folded value |
| `(set! x e)`, compound assignment | `(set! x e)` |
| `if`, `when`, `cond`, `if-not`, `when-not` | `if`, `when`, `cond` statements; as values, through a variable (§7.2) |
| `while`, `loop`/`recur`, `for-each`, `dotimes` | `(while true ...)` with labeled `continue`/`break` (§7.7) |
| `label`, `break`, `continue`, `return` | Go labels on `for` or `switch`, `(break :L)`, `(continue :L)`, `(return e)`; through `try`, control codes (§7.9) |
| `switch`, `if-instance`, `when-instance` | `switch`, `type-switch`, `if` chains (§7.8) |
| `throw`, `try`/`catch`/`finally`, `with-resources` | `(panic (jrt/Thrown e))`, a function literal with `(defer (jrt/Catch ...))` (§7.9) |
| `locking`, `^:synchronized` | `jrt.MonitorEnter`/`MonitorExit` with `defer` (§8.1) |
| `java-assert` | `(when (and (not C_assertionsDisabled) (not c)) (panic (jrt/Thrown ...)))` (§7.13) |
| `letclass`, `anon` | classes of their own (§7.12) |
| `this`, `Outer/this`, `super` | `t`/`this` (§5.4), the outer instance field, the embedded struct |
| `(.-f x)`, `C/f`, `(.-f super)` | `(.-F_f x)`, `C_f`, `(.-F_f (.-B t))` |
| `(.m x a)`, `(C/m a)`, `(.m super a)` | `(.M_..._R x a)`, `(C_M_..._R a)`, `(.Impl_M_..._R (.-B t) this a)` |
| `(C. a)`, `(.new o I a)`, `anon` | `(C_New_... a)`, `(I_New_... o a)` |
| `(new T/n d)`, `(new T/1 [a b])`, `aget`, `aset`, `alength` | jrt array constructors, `(aget (.-A a) i)`, `(.Store a i v)`, `(len (.-A a))` (§5.9) |
| `(cast T e)`, primitive conversions | `(T_Cast e)`, `(conv G e)` or a jrt conversion (§7.4) |
| `(instance? T e)` | `(T_InstanceOf e)` |
| `identical?`, `nil?`, `some?` | `(== a b)` after §5.6's conversions, `(== x nil)`, `(!= x nil)` |
| `==`, `<` ... on primitives, `not`, `and`, `or` | Go's operators |
| the operators of classes/SPEC.md §5.4 | Go's operators with Java's corrections (§7.4) |
| `(java-str a b)` | `(jrt/Concat ...)` (§7.5) |
| `(lambda FI ...)`, `(method-ref ...)` | the functional interface's adapter with a Go function literal (§7.11) |
| `T`, `Integer/TYPE`, `T/1` (class literals) | `T_class`, jrt's primitive and array classes |
| literals | Go literals of the Go type (§7.4); string literals from the pool (§7.5) |

### 7.2 Contexts, temporaries and the order of evaluation

The analyzer's nodes are expressions (Clojure's `if`, `let`, `loop`, `try` have values); Go
separates statements from expressions and has no conditional expression. c2g translates each
node in one of four **contexts**, as the JVM back end does (`arbace.classes.emit`: `:stmt`,
`:expr`, `:return`):

- **statement**: the value is discarded;
- **return**: the value is the enclosing Go function's result (a method, a function literal of
  a lambda or of a `try` body, §7.9);
- **assign v**: the value is stored into the Go variable `v`, declared before;
- **expression**: the node becomes one Go expression.

A node goes to expression context only if it is **simple**: constants, locals, field and static
reads, calls, operators, casts and conversions over simple operands. Any other node (an `if`, a
`let`, a loop, a `try`, a `switch`, a `throw`, a `label` used as a value) in an operand position
is translated before the enclosing expression into a fresh temporary, in assign context.

**The order of evaluation.** Java evaluates operands strictly left to right (JLS 15.7). Go
orders only function calls, method calls, receive operations and `&&`/`||` among themselves;
"the order of those events compared to the evaluation and indexing of x and the evaluation of y
and z is not specified" (Go spec, "Order of evaluation"). So in `g(t.x, h())` Go may read `t.x`
after calling `h`. c2g therefore moves an operand into a temporary when a later operand of the
same expression (or a later argument, or the right side of an assignment) has a side effect
(a call, an allocation, an assignment, a class initialization) and the operand reads state a
side effect could change: a field, a static, an array element. Java locals cannot be changed
by a callee (lambdas and inner classes capture only effectively final ones), so local reads stay
in place; so do constants and reads of final fields outside their class's constructors (and,
inside them, of final fields already assigned). gc keeps such temporaries in registers.

**The second rule** (amendment C8, accepted 2026-10-09). When an operand is moved into a
temporary, every earlier operand of the same expression that is not stable (not a constant, a
local or a final field read as above) is moved too, before it, so that the temporaries keep
Java's left-to-right order: in `g(f(), t.x, h())`, moving `t.x` (because `h()` follows) without
moving `f()` would read `t.x` before calling `f`. (The fixture `tLazySeq` found the rule
missing.)

The same rule orders a class initialization (§6.2) triggered by a static field access in the
middle of an expression after the operands to its left, and an assignment's target before its
right side (JLS 15.26: the array reference and index of `a[i] = f()` are evaluated before
`f()`).

### 7.3 Locals

- A Java local is a Go variable declared by a `let` at the point Java declares it, scoping over
  the rest of the block (Go forms SPEC §7.3), with its Go type: `(let [^int32 i 0] ...)`.
  Untyped locals take the analyzer's type of their initializer.
- A local declared before its first assignment (the converter's `^:mutable` locals with default
  values, locals shared across `switch` arms) is declared with Java's default value, which is
  Go's zero value: `(let [^int32 n (zero int32)] ...)`.
- **Captured locals** (by a lambda's function literal, §7.11) are captured by reference in Go;
  Java captures only effectively final locals, so the two agree. Anonymous and local classes
  copy them into fields at construction, as javac does (§7.12). A `for-each` variable is
  declared inside the loop body, so each iteration has its own.
- Parameters are Go parameters; a `^:mutable` parameter is assigned in place.

### 7.4 Primitive types, arithmetic and conversions

Go's integer arithmetic is two's complement with wrap-around for signed types ("may legally
overflow", Go spec), like Java's, so most operators translate directly. The table gives the
exceptions.

| Java (classes/SPEC.md §5.4) | Go forms | why |
|---|---|---|
| `+ - *` on `int`, `long` | `(+ a b)`, `(- a b)`, `(* a b)` | wrap as in Java |
| `/`, `%` on `int`, `long` | `(/ a b)`, `(% a b)` | truncating like Java; `MIN_VALUE / -1` is `MIN_VALUE` and `MIN_VALUE % -1` is 0 in both; division by zero panics in Go and becomes `ArithmeticException("/ by zero")` (§7.9.5) |
| `+ - /` on `float`, `double` | Go's operators at `float32`, `float64` | IEEE, rounded per operation |
| `*` on `float`, `double` | `(conv float64 (* a b))` (`float32` likewise) | Go may fuse `x*y + z` into one fused multiply-add (Go spec, "Floating-point operators"); gc does on arm64 (`FMADDD`), and on amd64 with `GOAMD64=v3`; an explicit conversion forbids it (§13.5). Java rounds each operation |
| `%` on `float`, `double` | `(math/Mod a b)` (for `float`, on the widened values, then narrowed: exact) | Java's `%` is C's `fmod`, not IEEE remainder; so is Go's `math.Mod`, NaN and infinities included |
| `-a` | `(- a)` | |
| `& \| ^ ~` | `bit-and`, `bit-or`, `bit-xor`, `bit-not` | |
| `a << n`, `a >> n` on `int` | `(<< a (bit-and n 31))`, `(>> a (bit-and n 31))` | Java masks the count to 5 bits (to 6 bits, `63`, for `long`); Go shifts by the full count (giving 0 or -1 beyond the width) and panics on a negative one. A constant count is masked at translation |
| `a >>> n` on `int` (`long`) | `(conv int32 (>> (conv uint32 a) (bit-and n 31)))` | Go has no unsigned shift of signed types |
| `< <= > >= ==` and `!=` | Go's, at the promoted type | NaN compares as in Java |
| widening `i2l`, `i2f`, `i2d`, `l2f`, `l2d`, `f2d` | `(conv int64 x)`, `(conv float32 x)` ... | Go rounds `int64` → `float32` and `float64` → `float32` to nearest, as Java |
| narrowing `l2i`, `i2b`, `i2s`, `i2c` | `(conv int32 x)`, `(conv int8 x)`, `(conv int16 x)`, `(conv uint16 x)` | Go truncates two's complement, sign-extending signed sources first, as Java |
| `f2i`, `f2l`, `d2i`, `d2l` | `(jrt/D2I x)` ... | Java saturates and maps NaN to 0; Go's result is "implementation-dependent" for values out of range |
| `char` operands | `uint16`, promoted to `int32` as Java promotes (the analyzer gives each operator's type) | |
| `boolean` `&`, `\|`, `^` | written by the converter as `let` and `and`/`or`/`not`, classes/SPEC.md §5.4 | |
| `Math.addExact` and the other exact operations | jrt's `Math` (shim), with Go's overflow checks | |

**Literals.** An integer literal or folded constant is written as a Go literal of its Java value
at its Go type (the analyzer narrows Clojure's `long` literals where Java's context is `int`,
`short`, `byte` or `char`); a `char` constant as a number or a Go rune literal in the BMP; a
`float` or `double` as its shortest decimal representation (`Float/toString`,
`Double/toString`), which Go's exact conversion of the decimal to `float32` or `float64` brings
back to the same value. NaN, infinities and `-0.0` are jrt's variables (§6.1).

**Conversions of constants.** Go rejects a conversion of a constant its target type cannot
represent (`uint32(-8)` is a compile error, "constant -8 overflows uint32"), where Java would
convert the value. So when c2g itself puts a conversion around an operand that is a constant (the
`uint32` of `>>>` on `-8`, the `float64` of a product of constants the analyzer did not fold), it
writes the converted value instead (`4294967288`). The prototype of §15 met this case.

### 7.5 Strings

- **`jrt.String`** is hand-written (D4): `(struct jrt/Object ^{:tag (slice uint16)} value ^int32
  hash)`, immutable, final (`*jrt.String`), with Java's `String` API as the manifest declares it
  (`length`, `charAt`, `hashCode` as `s[0]*31^(n-1) + ...`, `compareTo` by UTF-16 code units,
  `equals`, `substring`, case mapping through the translated `Character` ...). A string of up
  to 108 code units is one Go object, its units after the header (`unsafe.Slice` over a
  trailing array, the length rounded up to Go's size classes); `new StringBuilder()` holds its
  first 16 units in itself (amendment O2, accepted 2026-10-10).
- **Literals** are interned: every string literal (and folded constant string) of a package is
  a variable of the package's pool `c2g_strings.go`, `Lit_<n>`, initialized at Go package
  initialization by `jrt.Intern` from a Go string literal (or, when the Java string has unpaired
  surrogates, which UTF-8 cannot hold, from a `uint16` slice). `jrt.Intern` keeps one global
  table, so equal literals of all classes are one object, and `String.intern` uses it too.
- **`java-str`** is `(jrt/Concat p1 ... pn)`, each operand converted to `*jrt.String` by its
  static type as JLS 5.1.11 says: `(jrt/StrOfInt i)` (`byte`, `short`, `int`), `StrOfLong`,
  `StrOfChar`, `StrOfBool`, `StrOfFloat` and `StrOfDouble` (Java's `Float.toString` and
  `Double.toString`, translated from `jdk.internal.math`), and for references
  `(jrt/StrOfObj x)`: `"null"` for `nil`, else `toString()` (`"null"` again if that returns
  `null`). Each operand is converted as soon as it is evaluated, left to right, which is javac's
  order since JDK 19 (JDK-8273914). Constant operands are already folded into literals by the
  analyzer.
- **`switch` on a string** follows javac's two steps: `(switch (.HashCode__I s) (case [h1] (when
  (.Equals_O__Z s Lit_1) (set! k 0))) ...)` with the Java hash codes of the labels as constants,
  then a `switch` on `k` holding the arms. A `nil` selector throws `NullPointerException`
  through the call.
- The host's text (files, standard streams, Go strings in jrt's internals) is converted at the
  boundary: UTF-8 Go strings to and from UTF-16 by jrt's charset classes.

### 7.6 Members, calls, boxing

- **Field access**: `(.-F_f x)` for a pointer Go type; `(.-F_f (.Self_C x))` for a class
  interface; `(.-F_f t)` for the class's own fields. Volatile fields are §8.2's.
- **Calls**: `(.M_..._R x args)` on the receiver's Go type (a leaf's pointer, a class's or an
  interface's Go interface): the analyzer's call names the static type of the receiver as its
  owner, so the receiver has a Go type with the method, except for `Object`'s own methods on an
  `Object`-typed receiver, which are jrt's helpers (§5.8). A call on a leaf class's pointer is a
  direct call.
- **Overloads** are the analyzer's choice, recorded on the node; c2g only names the chosen
  method. Signature polymorphic calls (`MethodHandle.invoke`) are out of scope (§12).
- **Variable arity**: the analyzer packs the array; it is an ordinary argument.
- **Boxing and unboxing** are the analyzer's `valueOf` and `xxxValue` calls, translated like any
  call: `Integer.valueOf` is jdk26u's, with its cache of -128 to 127, so `(identical?
  (Integer/valueOf 5) (Integer/valueOf 5))` is true and `(identical? (Integer/valueOf 500)
  (Integer/valueOf 500))` false, as on the JVM. Unboxing `nil` throws through the method call.
- **Class literals** are `C_class`; `Integer/TYPE` and the other primitive classes are jrt's.

### 7.7 Control flow

- **`if`** in statement context is Go's `if`/`when`; in assign or return context each branch
  assigns or returns. `cond` is an `else if` chain. Tests are Java booleans (the analyzer has
  converted Clojure truth on references to `nil` checks, and `Boolean` tests to
  `booleanValue`).
- **`loop`/`recur`** (and `while`, `dotimes`, which the analyzer expands into loops): the loop's
  bindings are Go variables declared, in a block of their own (`(do (let ...))`, so that they do
  not collide with later declarations of the Go block), before a labeled `(while true ...)`; the
  body is translated in a loop context where a tail `recur` assigns the new values (one parallel
  `(set! (values a b) (values ...))`, temporaries per §7.2) and continues, and any other tail
  value is assigned to the loop's result variable (when the loop has a value) followed by
  `(break :L)`:

  ```clojure
  ;; (loop [^int i 0] (when (< i n) (f i) (recur (unchecked-inc-int i))))
  (do
    (let [^int32 i 0]
      (label :L1
        (while true
          (when (< i n)
            (f i)
            (set! i (+ i 1))
            (continue :L1))
          (break :L1)))))
  ```

  gc compiles this as the obvious loop. `(continue args)` and `(continue :L args)` from any
  position assign and continue the targeted loop's label.
- **`for-each`** over an array is an index loop over a copy of the array reference and its
  length; over an `Iterable`, `(.Iterator__Iterator xs)` with `HasNext__Z` and `Next__O`, the
  element cast to the binding's type as the analyzer says. A loop without jumps out of its body
  is a plain `(while cond ...)` without label (§15.1).
- **`label`, `break`** on a non-loop form: `(label :L (switch (default ...)))`, a labeled
  `switch` with only a `default` clause, which `(break :L)` leaves; an unlabeled `continue`
  inside still reaches the enclosing loop (c2g labels every jump anyway). A labeled form with a
  value assigns its result variable before `(break :L)`.
- **`return`** is Go's `return`, with the method's result converted (§5.6), except inside a `try`
  body (§7.9).
- Forms that do not complete (classes/SPEC.md §5.7) end their Go statement list; where Go
  requires a terminating statement (a function with results ending in a loop), c2g adds
  `(panic "unreachable")`, which gc's termination analysis accepts.

### 7.8 `switch` and patterns

- On `int`-like selectors, `(switch sel (case [1 2] ...) (default ...))`: Go's `switch` on
  constants, with each arm complete (class forms switches do not fall through; Go's do not
  either without `fallthrough`). The analyzer's folded labels are the case constants.
- On strings: §7.5. On enums: `(switch (.-F_ordinal e) ...)` (through `(.Self_Enum e)` when
  the enum has constant bodies and so is an interface type), with the constants'
  ordinals (whether javac would use a `$SwitchMap$` holder does not matter here: the ordinals of
  the closed world are known); `nil` throws `NullPointerException`.
- **Patterns** (`[^T x]`, record patterns, guards) and `nil` labels: when every label is a type
  pattern without guard and no type is an interface a dynamic object may implement, a Go
  `type-switch` (`(case [nil] ...)` for the `nil` label; the first matching case wins, as Java's
  dominance order guarantees); otherwise an `if` chain of `T_InstanceOf` tests, binding the cast
  values, with the guards, and record components read through their accessors (an exception
  from one wrapped in `MatchException`, as javac does). `if-instance` and `when-instance` are the
  one-pattern case.
- **The `if` chain as built** (amendment W2, accepted 2026-10-09) is in a labeled block,
  `(label :L (switch (default ...)))`: an arm whose pattern and guard match runs its body and
  leaves the block (`(break :L)`); a failed guard falls through to the next arm. (Repeating the
  later arms under each guard grew exponentially with the guards.) A record component is read
  from its field when the accessor is the derived one (a record is final, so the derived
  accessor can neither be overridden nor throw); a declared accessor is called in a function
  literal that catches, and its exception `t` is thrown as `new MatchException(t.toString(),
  t)`, as javac's code does. `MatchException` is a translated class when the closure has its
  source, else a jrt stand-in (§4.3).
- Exhaustive switches carry the converter's explicit `MatchException` default (classes/SPEC.md
  §5.8).

### 7.9 Exceptions

#### 7.9.1 Throwing

Exceptions are Go panics whose value is the Java exception object (a `jrt.Throwable_I`).
`(throw e)` is `(panic (jrt/Thrown e))`: `jrt.Thrown` returns `e`, or a new
`NullPointerException` when `e` is `nil`, and the `panic` is written by c2g itself because a
call of `panic` is a terminating statement for Go (a method with results may end in a `throw`)
while a call of a jrt function that panics is not. The stack trace is captured when the
`Throwable` is constructed (`fillInStackTrace`, §7.9.6), not when it is thrown, as in Java;
rethrowing keeps it.

#### 7.9.2 `try` and `catch`

A Go `recover` only stops a panic in a deferred function, and a recovered panic resumes at the
return of the function that deferred, so a `try` body must be a function of its own. c2g writes
it as a **function literal called in place** (it does not escape, so its captured variables
stay where they are), and runs the handlers **after** it returns, in the enclosing function:

```clojure
;; (try body (catch IOException e h1) (catch Throwable e h2))     in statement context
(let [exc ((fn [] :results [^jrt/Throwable_I exc]
             (defer (jrt/Catch (addr exc)))
             <body>
             (return)))]
  (when (!= exc nil)
    (cond
      (jrt/IOException_InstanceOf exc) (let [e (jrt/IOException_Cast exc)] <h1>)
      :else (let [e exc] <h2>))))       ; without a catch of Throwable: :else (panic exc)
```

- `jrt.Catch(&exc)` is the deferred function: it calls `recover` itself (Go requires the call to
  be in the deferred function), stores a Java exception into `exc`, converts a Go run-time error
  into its Java exception first (§7.9.5), and re-panics anything else (a bug in jrt). A body
  that completes normally leaves `exc` `nil`.
- The handlers are tested in order with `T_InstanceOf`; a multi-catch `(catch [A B] e ...)`
  tests both; with no matching handler, `(panic exc)` rethrows the same object.
- Handlers run in the enclosing function, so a `return`, `break` or `continue` in a handler is
  an ordinary Go statement.
- A `try` with nothing to catch (no clause naming a class of the closed world, no
  normal-completion code) has no literal of its own: its body is translated in place, inside
  the `finally`'s literal when there is one (§7.9.4), else into the enclosing block (amendment
  O6, accepted 2026-10-10; an entered literal costs about 7.5 ns, §13.3).

#### 7.9.3 Control transfers out of a `try` body

A `return`, `break`, `continue` or `recur` inside the body leaves the function literal, so it
becomes a **control code**: the literal's results are `ctl int32`, the value `rv` (when needed)
and `exc`; each exit sets `ctl` (0 normal completion, 1 return, 2 + k the k-th jump target
outside) and returns; after the call, a `switch` on `ctl` performs the transfer in the
enclosing function (`(return rv)`, `(break :L)`, `(continue :L)`). A `try` body without such
exits has only `exc` (and `rv` if it has a value), as in the example. A `try` in expression
position has a value: `rv`, or the handler's.

#### 7.9.4 `finally`

```clojure
;; (try body (catch E e h) (finally f))
(let [(values ctl rv exc) ((fn [] :results [^int32 ctl ^T rv ^jrt/Throwable_I exc]
                             (defer (jrt/Catch (addr exc)))
                             <the try and its handlers, as in §7.9.2, with control codes>))]
  <f>                                   ; the finally code, in the enclosing function
  (when (!= exc nil) (panic exc))       ; an exception from the body or a handler
  (switch ctl ...))                     ; then the pending transfer
```

The body and its handlers run in a function literal whose own `jrt.Catch` takes whatever
escapes them; the `finally` code runs once, on every path, in the enclosing function; then the
pending exception is rethrown or the pending transfer performed. A `return`, `break` or `throw`
inside the `finally` code takes effect before and so discards the pending one, as in Java.
`with-resources` arrives from the analyzer already desugared into `try`, `catch Throwable`,
`addSuppressed` and `close` (javac's pattern), and `:normal-finally` (code run only on normal
completion) is translated in the same position as `finally`, without the rethrow path.

#### 7.9.5 Implicit exceptions

| Java's check | in Go | becomes |
|---|---|---|
| call, field or array access on `null` | the run-time panic "invalid memory address or nil pointer dereference", or c2g's receiver check (§5.6) | `NullPointerException` |
| array index out of bounds | "index out of range [i] with length n" | `ArrayIndexOutOfBoundsException("Index i out of bounds for length n")` |
| integer division or remainder by zero | "integer divide by zero" | `ArithmeticException("/ by zero")` |
| negative array size | jrt's array constructors check | `NegativeArraySizeException("-1")` |
| failed `checkcast` | `T_Cast` checks | `ClassCastException` |
| array store | `RefArray.Store` checks | `ArrayStoreException` |
| `String` index | jrt's `String` checks | `StringIndexOutOfBoundsException` |

`jrt.Catch` recognizes Go's `runtime.Error` values and builds the Java exception; its stack
trace is taken at that point, which lies inside the frame that failed. (Correction,
2026-10-08, to follow jrt: the index and length of an out-of-range index are read from the
fields `x` and `y` of the runtime's `boundsError` by reflection, not parsed from the message.) Go's fatal errors (stack
overflow, out of memory, concurrent map writes in jrt) cannot be recovered (V1, V2).

**The message of an implicit `NullPointerException` is null** (amendment J6, accepted
2026-10-08; V11): the JVM's helpful messages (JEP 358: `Cannot invoke "..." because "..." is
null`) are not reproduced, as on a JVM run with `-XX:-ShowCodeDetailsInExceptionMessages`, and
the native `NullPointerException.getExtendedNPEMessage` (§9.1) returns null. The oracle's
cases that record helpful messages ([ORACLE.md](ORACLE.md)) are accepted mismatches, compared
as the bare class. Reproducing them would need c2g to pass the failing expression's text to
each check; c2g may generate them later.

#### 7.9.6 Stack traces, chaining, uncaught exceptions

- `Throwable` is jrt's (shim), with Java's API: cause, `initCause`, suppressed exceptions,
  `getStackTrace`, `printStackTrace`. Its constructor captures the program counters with
  `runtime.Callers` (about 240 ns, §13.3); `getStackTrace` maps them to `StackTraceElement`s
  through a table c2g generates (Go function name → declaring class, method name), with the file
  and line of the class forms (§4.3). Frames of forwarders, function literals of `try` bodies and
  jrt's machinery are elided; frames of the evaluator show its Java methods, and the evaluated
  Clojure code's own frames come from the evaluator's frame chain (§10.7).
- **The frame table** (amendment J4, accepted 2026-10-08). c2g writes a file `c2g_frames.go`
  per package whose `init` calls `(jrt/RegisterFrames (lit (slice jrt/FrameInfo) ...))`, each
  entry `{Go, Class, Method, Flags}`: `Go` is the function's name as `runtime.Frame.Function`
  gives it, `Class` and `Method` the Java names, and the flag `jrt.FrameElide` hides the frame.
  Frames without an entry go to jrt's **demangler**, which reads §4.4's names (`(*T).M_..._R`,
  `T_m...`, `Impl_`, `Ctor` shown as `<init>`, `_New` elided as an allocation function,
  `_clinit` as `<clinit>`). So c2g registers only what the names cannot express: lambdas
  (`lambda$m$0`), methods whose Java name contains `_`, and frames to elide. A function literal
  without an entry stands for its function (a `try` body), whose own frame below it is
  dropped. jrt also drops a forwarder (its callee is `Impl_` of its own name), the frames
  before the last `runtime.gopanic` (an exception built in `Catch` for a run-time error starts
  at the failing frame), and at the top `fillInStackTrace` and the constructors of `Throwable`
  classes, as HotSpot does.
- **Lines** (amendment W1, accepted 2026-10-09). The frame table stays as J4 has it, names
  only: a `StackTraceElement`'s file and line are gc's, from the `--line-file` build's line
  tables (§4.3), so an element of translated code names its class forms file and line. A
  function literal nested in a function literal (`f.func1.1`) stands for its function as
  `f.func1` does. jrt's translated classes are named by their Go file
  (`java_util_HashMap.clj`, not `HashMap.java`), and a member a variant replaces shows the
  variant file's line under its class's file.
- An exception that leaves a thread's `run` (or `main`) is handed to the thread's uncaught
  exception handler, else the default one, else printed as the JVM prints it (`Exception in
  thread "main" ...`, `jrt.Uncaught`) by jrt's thread entry; for `main`, `jrt.RunMain` then
  makes the program's status 1 (§8.4, §10.6). In a c2g program, `printStackTrace()` and the
  uncaught exceptions print through `System.err`, as the JVM's do (jrt's `StderrPrint`, which
  c2g's support redirects; the host's stream while `System.err` is null: amendment K2, §11).

### 7.10 Monitors in code

`(locking x body)` and `^:synchronized` methods are in §8.1, with the memory model.

### 7.11 Lambdas and method references

- For each functional interface `F` the closed world uses as a lambda's or method reference's
  target, c2g generates an **adapter** class `F_Fn`: a struct with the header and `Fn`, a Go
  function of the erased SAM signature; it implements `F` (its marker, its SAM method calling
  `Fn`, forwarders for `F`'s default methods) and has a `Class` named `F$$Lambda`. One adapter
  serves all lambdas of `F` (V4).
- `(lambda F params body)` is `(addr (lit F_Fn :Fn (fn ... )))`: a Go function literal over the
  erased signature, whose parameters are cast to the lambda's instantiated types (as
  `LambdaMetafactory` does) and whose result is converted to the erased return type. The literal
  captures what the lambda captures: the enclosing method's `this` and `t`, and effectively
  final locals (§7.3). `(return v)` inside returns from the literal.
- `(method-ref F ...)`: a static method reference is a literal calling the function; a bound
  receiver (`expr::m`) is evaluated once and null-checked (`Objects.requireNonNull`, as javac)
  before the literal captures it; an unbound one takes the receiver from the first argument;
  `C/new` allocates; the analyzer's lambda form of array constructor references is a lambda.
- Marker interfaces of intersection targets (`(& Comparator Serializable)`, javac's
  `(Comparator<T> & Serializable)`) give the lambda an adapter of its own, `F_Fn_M1_..._Mn`,
  registered as `F$$Lambda$M1...`, that also implements the markers in the world which `F` does
  not already extend (Serializable when the target is serializable, then the others, sorted), as
  `altMetafactory` makes the lambda's class implement them; javac's cast to `Serializable`
  after such a lambda then succeeds (`Comparator.comparing`, `thenComparing`,
  `Map.Entry.comparingByKey`) (amendment JB5, accepted 2026-10-10). Serializability itself is
  cut (§12).
- `FromFn` in the functional interface's class (§5.11) wraps an `IFn` into the adapter, `Fn`
  calling `invoke`: that is how jrt's `Reflector` support adapts a Clojure function passed where
  a functional interface is expected, without `java.lang.reflect.Proxy` (cut).
  `Proxy.newProxyInstance` throws `UnsupportedOperationException`; the `Reflector` variant's
  `boxArg` calls `jrt.AdaptFn(paramType, arg)`, which is the interface's `FromFn`. c2g therefore
  generates the adapter and sets `FromFn` for every interface `Compiler.FISupport` may adapt,
  the `@FunctionalInterface` ones, whether or not a lambda targets it, and
  `Class.isAnnotationPresent(FunctionalInterface)` answers `FromFn != nil` (amendment R13,
  accepted 2026-10-08).
- **`FromFn` is written in `arbace/lang`** (amendment E5, accepted 2026-10-09), in the
  generated `c2g_fromfn.go`, since it calls `IFn` and `RT`, which jrt cannot name. Its `init`
  sets `FromFn` of every `@FunctionalInterface` interface translated, jrt's included (the
  annotation of a class with class forms is read from its declaration, §4.1). The adapter
  behaves as the JVM's `Reflector` proxy does: the arguments boxed, the fn called with
  `applyTo`, the result converted as `Reflector.coerceAdapterReturn` converts it; an array
  result is checked against the return type's array class, as `Dyn`'s results are (amendment
  JB3, accepted 2026-10-10). Reachability
  treats these interfaces as lambda targets (their adapters and default methods are reached).
- **`:fi-adapter`** (the analyzer's conversion of a Clojure `fn` to a functional interface
  inside class bodies) is the interface's adapter whose `Fn` calls the `FnInvokers` invoker the
  analyzer chose (amendment E5).

### 7.12 Nested, inner, local and anonymous classes

- Every nested class is a class of its own (§4.4 names). Static nested classes need nothing
  more.
- **Inner, local and anonymous classes** get the fields the analyzer derives: the outer instance
  `this$0` (Go `F_this_0`, of the outer class's Go type) and the captured locals `val$x`
  (`F_val_x`), passed to the constructor in the analyzer's order (javac's, `ctor-real-desc`) and
  stored before the superclass constructor call. `Outer/this` reads `F_this_0` (a chain of them
  for deeper nesting, as the analyzer's `:this-path` says); `Outer/super` calls the outer
  class's implementation through it; the converter's `this1` receivers are the outer instance
  itself.
- `(Inner. args)` in an instance context passes `this` as the outer instance; `(.new o Inner
  args)` passes `o` after its null check.
- An anonymous class's constructor passes its arguments to the superclass's constructor, as
  the analyzer derives it. `anon` and `letclass` expressions are allocations of these classes.

### 7.13 Enums, records, assertions, initializers

- **Enums** extend jrt's `Enum` (shim: `F_name`, `F_ordinal`, the final methods). The constants
  are static fields set by the class initialization in order; `values()` clones `$VALUES`;
  `valueOf(String)` searches the constants (the analyzer derives both members; c2g writes their
  bodies as `arbace.classes.emit` does). A constant with a body is an anonymous subclass, which
  makes the enum non-leaf. `Enum`'s promotable methods (§5.4, §9.1) are `name`, `ordinal`,
  `hashCode` and `equals`; `clone` is `Impl_Clone__O` (final, throwing
  `CloneNotSupportedException` without a message, as `Enum.clone` does), to which every enum
  class's `Clone__O` forwards (amendment J10, accepted 2026-10-08).
- **Records** extend jrt's `Record`. Their fields, accessors and canonical constructor come from
  the analyzer; `toString`, `hashCode` and `equals`, which javac links to
  `ObjectMethods.bootstrap`, are generated as `java.lang.runtime.ObjectMethods` computes them in
  jdk26u (`Name[a=1, b=x]`; `31 * h + hash(c)` over the components in order; component-wise
  equality, `==` for primitives as `Float.compare`/`Double.compare` and `Objects.equals` for
  references). They are derived members written in Go (amendment C7, accepted 2026-10-09):
  `toString` is `Name[c1=v1, c2=v2]` (the simple name, each component through `StrOf*`),
  `hashCode` is `31 * h + hash(c)` from 0, a component's hash its wrapper's `hashCode` for a
  primitive (over `C2g_FloatBits` and `C2g_DoubleBits`, `floatToIntBits` and
  `doubleToLongBits`, for the floating ones) and `C2g_ObjHash` (`Objects.hashCode`) for a
  reference, and `equals` is identity, or the same class (`getClass`) and every component
  equal: `==`, on those bits for the floating ones, and `C2g_ObjEquals` (`Objects.equals`) for
  references. The helpers are c2g's, in jrt's `c2g_support.go` (§11).
- **`java-assert`**: each top-level class has `C_assertionsDisabled` (`true` unless jrt's
  configuration enables assertions for it, as `-ea` does); a failed assertion throws
  `AssertionError` with the analyzer's constructor overload.
- **Instance initializers and field initializers** are already folded into the constructors by
  the analyzer (§5.2); static ones into the class initializer (§6.2).
- **Interfaces' static fields** and nested classes of interfaces are ordinary statics and classes.

### 7.14 Clojure inside class bodies

Class bodies may use Clojure outside the Java subset (classes/SPEC.md §5.13): vars, keywords,
`fn`. The analyzer lowers these into Java: constants in synthetic static fields (initialized in
the class initialization), `fn` into anonymous `AFunction` classes, var calls into
`Var.getRawRoot().invoke(...)`. c2g translates the result like any other code. Calls the
analyzer leaves reflective (`Reflector.invokeInstanceMethod`) go through jrt's reflection at run
time, as on the JVM.

## 8. Concurrency and the memory model

### 8.1 Monitors

- Every object can be locked. The **lock state lives in the header word** (§5.2): unlocked; a
  **thin lock** holding the owner's thread number and a recursion count, taken and released by
  compare-and-swap; or **inflated**, an index into jrt's monitor table, where a
  `jrt.Monitor` (a `sync.Mutex`, the owner, the count and a condition queue for `wait`/`notify`)
  lives while contended or waited on. jrt inflates on contention and on `wait`, and deflates when
  the monitor is free with no waiters. Uncontended locking costs one compare-and-swap (about
  5 ns, as `sync.Mutex`, §13.4) and the current thread's number (3.7 ns through the slot, §8.4):
  monitors are reentrant, so they need the owner's identity, which `sync.Mutex` has not.
  Measured, an uncontended `MonitorEnter` and `MonitorExit` cost about 14 ns, not the estimated
  10: two reads of the thread's number and two compare-and-swaps, since the exit must CAS too
  (another thread may inflate the word or set the identity hash meanwhile) (amendment T16,
  accepted 2026-10-08; V13).
- **The header word** (amendment J7, accepted 2026-10-08): the low 32 bits hold the identity
  hash, the high 32 bits the lock word, which is unlocked (0), **thin** (state 1, an 8-bit
  recursion count and a 22-bit owner thread number) or **inflated** (state 2, a 30-bit index
  into jrt's monitor table). jrt inflates on contention, on recursion beyond 255, on `wait`, and
  when the owner's number does not fit 22 bits; jrt recycles thread numbers (§8.4), so that last
  case does not arise in practice. The identity hash is kept through inflation and deflation.
- **`MonitorEnter` and `MonitorExit` require a Java object**: they take the header by address
  (the `any`'s data word: every Java object begins with its header), with no assertion to
  `jrt.Object_I`; c2g's output passes only Java objects (amendment T16, accepted 2026-10-08).
- `(locking x body)` is `(jrt/MonitorEnter x)` (which throws `NullPointerException` on `nil`)
  followed by the body in a function literal with `(defer (jrt/MonitorExit x))`, with control
  codes as for `try` (§7.9.3): the monitor is released on every exit, exceptions included.
- A `^:synchronized` method begins with `(jrt/MonitorEnter this)` (`C_class` for a static one)
  and `(defer (jrt/MonitorExit this))`: Go's open-coded defers make this cheap, and they run on
  panics.
- `wait`, `notify` and `notifyAll` are `jrt.Wait(x)`, `jrt.Notify(x)`, `jrt.NotifyAll(x)`, with
  `IllegalMonitorStateException` when the current thread does not own the monitor; the timed
  `wait` is `jrt.WaitTimeout`, and `jrt.HoldsLock(x)` tells whether the current thread owns the
  monitor (§11).

### 8.2 `volatile`

Java's `volatile` reads and writes are sequentially consistent; Go's `sync/atomic` operations
are too ("all the atomic operations executed in a program behave as though executed in some
sequentially consistent order", Go memory model). So a volatile field's Go type is an atomic
one, and every access is an atomic load or store:

| volatile field type | Go field type |
|---|---|
| `boolean`, `int`, `long` | `atomic.Bool`, `atomic.Int32`, `atomic.Int64` |
| `byte`, `short`, `char`, `float`, `double` | `atomic.Int32`, `atomic.Uint32` or `atomic.Uint64` holding the value (bits for floats) |
| a pointer Go type (leaf class, `String`, array) | `atomic.Pointer[T]` |
| an interface Go type (`any`, `C_I`, `J`) | `jrt.Volatile[T]`: an `atomic.Pointer` to a boxed copy of the two-word value; a store allocates (16 bytes), a load is an atomic load and a copy |

Volatile statics likewise. jrt's atomics (`AtomicInteger`, `AtomicReference` ...) and the
`Unsafe` subset are built the same way.

### 8.3 Data races and two-word references

A Java program may race on a plain field and still be memory-safe: a reference is one word and
reads see some written value. A Go interface value is two words (type and data), and a racing
read can combine the two halves of different writes, which breaks memory safety. Pointers,
`int32`, `int64` and smaller are single words on amd64 and arm64, so the risk is limited to
**non-volatile fields of interface Go type written after construction without a lock**.

- c2g lists those fields per class (a report, not an error): a non-final, non-volatile field of
  interface Go type assigned outside constructors and not inside a `synchronized` method or
  `locking` on `this`. Most of Clojure's racy idioms cache single-word values (`_hash`,
  `_hasheq`, `Keyword._str` as `*jrt.String`).
- jrt's and Arbace's tests run under Go's race detector (`-race`), which reports such races.
- A field found racing becomes volatile in a variant (§4.6), or jrt.Volatile directly (§16 Q10).
- **As built** (amendment W5, accepted 2026-10-09). c2g records every store into a candidate
  field as defined above (outside its class's constructors and static initializer; a lambda's
  body starts a context of its own, outside any lock) and lists them in `report.edn`
  `:race-candidates`, by class, with the writing methods (67 fields in 36 classes for the
  check's world: the JDK collections' caches and iterators, `Compiler`'s expression builders,
  `LazySeq` and `Delay` fields written under a `ReentrantLock`, which the report cannot see).
  `bin/c2g-race` builds a program of threads sharing translated objects as Clojure's runtime
  shares them (hash caches, `Symbol` and `Keyword` interning, `Namespace.findOrCreate`, an
  `Atom`'s compare-and-set, `RT.nextID`; `test/c2g/race`) with `-race`, and summarizes the
  reports; it also runs a given c2g program so. `-race` needs cgo, which the race build alone
  turns on (it works on Alpine's musl). Found: races on the `int32` hash caches only, one-word
  races Java allows; none on a two-word field.

**Final fields.** Java guarantees that an object's final fields are seen initialized by any
thread that obtains a reference to it, even through a race. Go makes no such promise for racy
publication (and arm64 reorders stores). Arbace publishes shared objects through atoms, refs,
vars and agents (atomic operations, which publish everything written before them in Go too), so
this only matters for racy publication, which the race detector also finds.

### 8.4 Threads and thread identity

- `java.lang.Thread` is jrt's (shim). `start` runs `run` in a new goroutine, which first sets the
  goroutine-local slot (§9.3) to its `Thread`. `currentThread()` reads the slot; a goroutine jrt
  did not start (Go's own) gets a `Thread` created on first use. `join`, `sleep`, `interrupt`,
  `isAlive` are implemented over channels and `time`.
- **The threads API** (amendment T13, accepted 2026-10-08):
  - `jrt.CurrentThread() *jrt.Thread` returns the current thread's struct (3.4 ns);
    `Thread_CurrentThread__Thread()` is Java's `currentThread()`, returning `Thread_I` (`Thread`
    is non-leaf: `ForkJoinWorkerThread` extends it).
  - `jrt.RunMain(run func()) int` runs the program's main as the JVM does (§10.6): the calling
    goroutine becomes the thread `main`; an uncaught exception is reported (§7.9.6) and makes
    the status 1; then it waits for the non-daemon threads (`jrt.WaitNonDaemon()`), runs the
    shutdown hooks as the JVM's `DestroyJavaVM` does (amendment FS5, accepted 2026-10-10:
    before, only `System.exit` ran them, so `deleteOnExit` did not delete at a normal end) and
    returns the status (`System.exit` ends the process before).
  - `jrt.Go(name, f func()) *jrt.Thread` starts a Go function as a daemon jrt thread;
    `jrt.RunnableOf(f func())` is a `Runnable` of a Go function; `Thread_defaultHandler` holds
    the default uncaught exception handler.
  - **The interrupt status** is a token in a channel of capacity 1: `interrupt` sends it without
    blocking, `isInterrupted` is its length, `Thread.interrupted()` receives it. Every
    interruptible wait (`Object.wait`, `sleep`, `join`, `lockInterruptibly`, `Condition.await`,
    `Future.get`, `CountDownLatch.await`, `ReferenceQueue.remove`) selects on it and consumes it
    when it throws `InterruptedException`, which is Java's "the status is cleared when the
    exception is thrown"; `park` returns on it and leaves it set.
  - **Thread numbers** (the thin lock's owner field, §8.1) are recycled: a jrt thread frees its
    number when its run ends, an adopted goroutine's by a `runtime.AddCleanup` on its `Thread`,
    so numbers stay below the peak count of live threads.
- `ThreadLocal.get` finds the current `Thread` through the slot (3.7 ns, §13.3) and its value in
  the thread's map. `Var`'s dynamic bindings, `LockingTransaction` and `Agent`'s nesting use it.
- **Stopgap**, before the runtime patch was in the build (jrt's phase 1; replaced in phase 2a):
  the goroutine id parsed from `runtime.Stack`'s header, with a `sync.Map` from id to `Thread`
  (about 3 µs per lookup, §13.3), entries removed at a jrt thread's end.
- Executors, `CountDownLatch`, `LockSupport`, locks with `Condition` and `Semaphore` are jrt's
  shims over `sync` and goroutines (JAVA-SURFACE.md decision 4; `Semaphore` by amendment Y3,
  accepted 2026-10-09: the full public API with Java's messages, fairness not kept, as for
  `ReentrantLock`; `AbstractQueuedSynchronizer` translated was measured and not taken: it needs
  `Unsafe` additions and has a two-word race on `Node.waiter`, §8.3); `ConcurrentHashMap` and the blocking
  queues are translated over `Unsafe`'s compare-and-set (§9.2).
- **The fork-join pool** (amendment S5, accepted 2026-10-09; it reverses the cut of
  `ForkJoinPool` under D6, JAVA-SURFACE.md "What the cut leaves out") is jrt's hand-written
  `ForkJoinTask` (non-leaf, §5.3) and `ForkJoinPool` (`forkjoin.clj`), with the JDK's semantics
  but not its work-stealing: a pool of parallelism P runs a task given from outside (`invoke`,
  `execute`, `submit`) in a worker of its own, and a task forked in a worker in a new worker
  while fewer than P − 1 forked tasks run, else in the forking thread at once. Workers are jrt
  threads (goroutines) that know their pool (`inForkJoinPool`, `getPool`), not
  `ForkJoinWorkerThread`s. A task runs once: whoever claims it first (the forked worker,
  `join`, `invoke`) runs it, the others wait for its completion; a task whose `exec` returns
  false (a `CountedCompleter`) is done when completed explicitly, and runs `exec` again when it
  is forked again before then, as the JDK's `doExec` runs any task not done
  (`ArrayPrefixHelpers` reforks a parent to continue its cumulation; amendment JB4, accepted
  2026-10-10). `commonPool` has the
  processors less one. So reducers' `fold` and parallel streams run in parallel.
- **References** are jrt's: a `WeakReference` holds a `weak.Pointer` to the referent's header
  and its type word, and a `runtime.AddCleanup` on the referent enqueues it on its
  `ReferenceQueue`. A `SoftReference` holds its referent strongly and is cleared only by
  `clear()` (V12).

## 9. The boundary to jrt

### 9.1 The manifest

`go/arbace/jrt/manifest.edn` (Clojure reader) declares, for each hand-written jrt class, what
the translated code may use, as the analyzer needs it:

```clojure
{java.lang.String
 {:flags #{:public :final}
  :super java.lang.Object
  :interfaces [java.io.Serializable java.lang.Comparable java.lang.CharSequence
               java.lang.constant.Constable java.lang.constant.ConstantDesc]   ; amendment S3
  :members [[:ctor "([C)V" #{:public}]
            [:method "length" "()I" #{:public}]
            [:method "charAt" "(I)C" #{:public}]
            [:method "valueOf" "(Ljava/lang/Object;)Ljava/lang/String;" #{:public :static}]
            [:field "CASE_INSENSITIVE_ORDER" "Ljava/util/Comparator;" #{:public :static :final}]
            ...]
  :promotable #{"hashCode()I" "equals(Ljava/lang/Object;)Z"}}
 ...}
```

- c2g enters these declarations into the class environment; a use of an undeclared member is a
  translation error naming it, so jrt's coverage is checked before anything runs.
- The jrt Go forms must define every declared member under its §4.4 name; the build fails
  otherwise (gc reports the missing identifier), so manifest and implementation cannot drift.
- `:promotable` lists the methods whose implementation needs no dynamic `this` (§5.4), for
  which c2g writes no forwarders in subclasses (`Object`'s `hashCode` and `equals`, `Enum`'s
  `name`, `ordinal`, `hashCode` and `equals`; not `Enum`'s final `clone`, to which enum classes
  forward, §7.13: amendment J10, accepted 2026-10-08).
- A hand-written class declares the JDK's interfaces even when it cannot implement all their
  methods (amendment S3, accepted 2026-10-09): `String` implements `java.lang.constant.Constable`
  and `ConstantDesc` (translated in a c2g program, marker stand-ins in jrt's own build), so that
  `supers` and `ancestors` of `String` and the boxed numbers are the JVM's; `describeConstable`
  is c2g's (§11), and `resolveConstantDesc`, whose parameter is `MethodHandles.Lookup`, does not
  exist in Go.
- **Natives.** A `^:native` method of a translated JDK class is a call of the jrt function
  `C_M..._native`, hand-written; the manifest lists them. The closure has five, in three classes
  (`java-surface.edn`, `:native`): `Double.longBitsToDouble` and `doubleToRawLongBits`,
  `Float.intBitsToFloat` and `floatToRawIntBits`, `NullPointerException.getExtendedNPEMessage`
  (which returns null: §7.9.5, V11). jrt's own Java (§4.1, amendment K1, accepted 2026-10-09)
  adds `jdk.internal.jrt.HostFiles`' natives (`open0`, `read0`, `write0`, `available0`,
  `skip0`, `close0`: the file table over the host, in `go/arbace/jrt/files.clj`), static and
  with primitive and array parameters only, so that jrt builds without the translated types;
  and, with sockets (NT1-NT5, accepted 2026-10-10), `jdk.internal.jrt.HostNet`'s (`listen0`,
  `accept0`, `connect0`, `read0`, `write0`, `available0`, `close0`, `shutdown0`, `address0`,
  `port0`, `setOption0`, `getOption0`, `lookup0`, `reverse0`, `hostName0`, `hasFamily0`: the
  socket table and the name service over §9.4's `NetHost`, in `go/arbace/jrt/net.clj`), the same
  way (`String` parameters too).
- **Natives of `arbace.lang`** (amendments P1 and E6, accepted 2026-10-09). A `^:native` method
  a variant declares (§4.6) is translated the same way, a call of `C_M..._native` with the
  receiver first for an instance method. The function is **jrt's when jrt defines it**: the
  hand-written natives in `go/arbace/jrt/natives.clj` (`Reflector.adaptFn`; `RT$HostWriter`'s
  `hostWrite` and `hostFlush` and `RT$HostReader`'s `hostRead` until amendment K2 removed
  them, §9.4), called qualified, `(jrt/Reflector_AdaptFn_Class_O__O_native c f)`; such a
  native must be static and take and
  return only types jrt can name (jrt cannot import `arbace/lang`). **Otherwise it is in the
  class's own Go package, where c2g writes it**: `Compiler$Dyn`'s `defineClass`, `setMethod`,
  `newInstance`, `getField` and `setField`, in `arbace/lang`'s `c2g_dyn.go` (§5.12). The
  manifest does not list these (it declares jrt's classes).

### 9.2 What c2g's output calls

The functions and types of §11, and the hand-written classes through their Java API. The
translated JDK code reaches jrt's edge through 69 boundary classes (JAVA-SURFACE.md, "Boundary";
`String`, `StringBuilder`, `Unsafe`, `Math`, `Throwable`, `Class` ...), all declared in the
manifest. `Unsafe`'s compare-and-set on reference slots (used by `ConcurrentHashMap`'s table)
cannot be one hardware operation on a two-word interface value; jrt implements the `Unsafe`
reference operations on array slots and fields under a striped lock keyed by the slot's address
(64 stripes), and every reference access `ConcurrentHashMap` makes to its table goes through
them (`tabAt`, `casTabAt`, `setTabAt`), so they are atomic with respect to each other.

**`Unsafe`'s offsets** (amendment T11, accepted 2026-10-08):

- **Array offsets** are the JVM's numbers: base 16, scales 1, 2, 4 and 8, and 4 for references
  (as compressed oops), so Java's arithmetic on them holds (`ConcurrentHashMap`'s `ABASE + ((long)
  i << ASHIFT)`); jrt maps an offset back to the element of the Go slice, or to its bytes for the
  unaligned and byte accesses, bounds-checked.
- **Field offsets** are keys into jrt's table of fields: `objectFieldOffset(Class, name)` finds
  `F_name` in the class's Go struct type (through embedded superclasses) and records its Go
  offset and how its Go type is accessed (`sync/atomic` for `int32`, `int64` and their atomic
  types, atomic pointer operations for pointer slots, the striped locks for `any` and other
  interface slots, its own CAS for a `jrt.Volatile`). The struct type comes from
  `jrt.RegisterGoType(C_class, reflect.TypeFor[C]())`, which c2g calls in `c2g_classes.go` for
  the classes whose fields reach `objectFieldOffset` (§5.11). (A `reflect.Type` field of
  `ClassInfo` would serve as well; the registration call is what jrt implements.)

### 9.3 The goroutine-local slot

The Go runtime held as forms (B1-PLAN.md, decision 5 of JAVA-SURFACE.md) gets one field and two
functions, for `linux` now and `tamago` later:

- `runtime2.go`: in `type g struct`, after `coroarg`, the field `arbaceLocal unsafe.Pointer`
  (scanned by the GC as any pointer field of `g`).
- `proc.go`: in `gdestroy`, `gp.arbaceLocal = nil` before the `g` is put on the free list, so
  that a reused `g` starts empty.
- a new file `arbace_local.go` in `runtime`:

  ```go
  //go:linkname arbace_getLocal
  //go:nosplit
  func arbace_getLocal() unsafe.Pointer { return getg().arbaceLocal }

  //go:linkname arbace_setLocal
  //go:nosplit
  func arbace_setLocal(p unsafe.Pointer) { getg().arbaceLocal = p }
  ```

  (the one-argument `//go:linkname` pushes the symbol, as go1.23 and later require for pulling
  runtime symbols), pulled by jrt with `//go:linkname getLocal runtime.arbace_getLocal`.

New goroutines start with `nil`: Java threads do not inherit thread locals (`Inheritable
ThreadLocal`, unused by the closure's run-time paths, would be copied by jrt's `Thread.start`).
Checked with go1.27.1 (§13.3): the slot is per goroutine (100 goroutines, each reading back its
own value), costs 3.7 ns per `currentThread()` (the linknamed function is not inlined), and
works on arm64 (under `qemu-aarch64`). The build needs the three runtime files replaced: either
`bin/g2c build` gains an overlay option (Go's `-overlay`) or the runtime package is part of the
forms tree; that is an amendment to BUILD.md (§16 Q19), accepted as `--overlay` (BUILD.md,
amendment B5).

**Where the patch lives** (amendment T15, accepted 2026-10-08). The patched runtime files are Go
forms in a forms root of their own, `overlay/go/runtime/`, outside `go/` (whose package files
`bin/g2c build` collects): `overlay/go/runtime.clj` is the package file naming the three files,
`arbace_local.clj` is hand-written, and `proc.clj` and `runtime2.clj` are the toolchain's
`proc.go` and `runtime2.go` as `bin/g2c convert --goos linux runtime` converts them, with the
patch's two lines each marked `; Arbace` (the same forms serve amd64, arm64 and, having no
build constraints, `GOOS=tamago`). `bin/jrt overlay` prints them and writes the overlay file
for `$G2C_GOROOT`; `bin/jrt overlay --check` converts the toolchain's runtime again and requires
the forms to equal the conversion but for the marked lines, so a toolchain update shows up as a
failure. Without the overlay jrt fails to link (`relocation target runtime.arbace_setLocal not
defined`), so a build cannot silently miss the patch.

### 9.4 The host interface

jrt's use of the operating system is one Go interface, `jrt.Host` (B1-PLAN.md, point 4):
standard streams, files (open, read, write, close, stat, list, delete), the clocks
(`currentTimeMillis`, `nanoTime`), the environment and system properties, the command line,
exit, random seeds for `SecureRandom`, the embedded resources (§10.3). B1a's implementation is
over Go's `os` and `time`; B1b replaces it with the monitor's host calls. Translated code never
reaches the host except through jrt's classes (`FileInputStream`, `System`, `PrintStream` ...).

**As implemented** (amendment T14, accepted 2026-10-08): `jrt.Host` (in `host.clj`) has the
standard streams, files (`Open` with `os.O_*` flags, `Stat`, `ReadDir`, `Remove`, `Mkdir`,
`Rename`, `Getwd`, through `jrt.HostFile` and `jrt.HostFileInfo`), the clocks (`Now`,
`Nanotime`), the environment (`Getenv`, `Environ`), the command line (`Args`), `NumCPU`,
`RandomBytes`, the embedded resources (`Resource`) and `Exit`. `jrt.OSHost` is B1a's, over `os`,
`time` and `crypto/rand` (its `Resources` field takes an `fs.FS`, such as an `embed.FS`);
`jrt.SetHost` and `jrt.CurrentHost` swap and read it. The standard streams are `jrt.Stdin`,
`jrt.Stdout` and `jrt.Stderr`, unbuffered byte streams over the host's; `System.in`, `out` and
`err` are defined with `InputStream` and `PrintStream` over them, and `printStackTrace()`'s
`StderrPrint` goes through `System.err`, so that `setErr` redirects it. jrt's timed waits use
Go's runtime timers, which TamaGo's runtime provides too.

**An optional `HostFS`** (amendment FS3, accepted 2026-10-10; JRT-NOTES.md, "Files"). What
`java.io.File` asks beyond `Host`'s file calls is a second interface, `jrt.HostFS` (in
`hostfs.clj`): `Access` (`access(2)`), `Chmod`, `Chtimes` (the modification time, the access
time kept), `Realpath` and `Statfs` (space and the longest name). A host implements it when it
can; `jrt.CurrentHostFS()` is the current host as one, or nil. `OSHost` implements it on Linux
(`hostfs_linux.clj`, `//go:build linux`, over `syscall`, `os` and `path/filepath`). Without it
the natives of `UnixFileSystem`'s variant (`filesystem.clj`) answer as the JDK does when the
system call fails (no permission or time changes, canonical paths only collapsed, no space).
`Host` itself is unchanged, so B1b's host and other additions (sockets) are not touched by it.

**An optional `NetHost`** (JRT-NOTES.md, "Sockets (go-net)"; accepted with NT1-NT5,
2026-10-10). The network is a third interface, `jrt.NetHost` (in `net.clj`): `Listen`, `Dial`
(with a local address and a timeout), `LookupIP`, `LookupAddr`, `Hostname`, `HasFamily`,
`SetOption`/`GetOption` (by `java.net.SocketOptions`' numbers) and `Available`. jrt asks the
current host for it at each use; a host without it has no network (every `HostNet` native fails,
`Network not available`). `OSHost` implements it over Go's `net` (the pure Go resolver) and
`syscall` (options, `TIOCINQ`), undoing Go's defaults that differ from the JVM's (keep-alive,
`TCP_NODELAY`). Errors are kinds that `HostNet.exception` turns into the JVM's exceptions, with
the C library's texts as this machine's (musl) JVM gives them (amendment NT5).

**`JAVA_TOOL_OPTIONS`** (amendment NT2, accepted 2026-10-10). The system properties are made
from the host (above) and then from the `-Dname=value` options of the environment variable
`JAVA_TOOL_OPTIONS`, split as HotSpot splits it (`tooloptions.clj`), as the JVM reads it at its
start; no "Picked up" line is printed. Leading `-D` arguments of the executable are the
launcher's (step 6). `System.getProperties()` is c2g's (§11): a `Properties` holding a copy.

**`RT`'s streams, for now** (amendment P4, accepted 2026-10-09). Until jrt has `System`'s
streams with `OutputStreamWriter` and `InputStreamReader` (whose `StreamEncoder` and
`StreamDecoder` are `java.nio`'s, cut by R18), `RT`'s variant binds `*out*` to an `RT$HostWriter`
over fd 1, `*err*` to a `PrintWriter` (autoflush) over an `RT$HostWriter` over fd 2, and `*in*`
to a `LineNumberingPushbackReader` over an `RT$HostReader`: two member classes the variant adds
(§4.6), whose natives (§9.1) reach the host's streams with the JVM's behavior: UTF-16 to UTF-8
through an 8 KiB buffer per stream written at `flush` (or when full), a lone surrogate written
as `?`, write errors as `IOException`; UTF-8 to UTF-16 with malformed bytes as U+FFFD, a read
blocking for the first char and then taking only what is buffered. Nothing flushes `*out*` at
exit, as on the JVM. When jrt has the two adapters, the variant's `OUT`, `ERR` and `IN` return
to the JVM's initializers and the two classes and their natives go.

**`System`'s streams in a c2g program** (amendment K2, accepted 2026-10-09; it supersedes P4's
interim streams above, which are gone, and P4's class lookup stays, §10.3). jrt now has the two
adapters, as jrt's own Java (§4.1, K1): `OutputStreamWriter` and `InputStreamReader` over jrt's
three charsets with the JDK's REPLACE behavior (the behavior P4 describes, now the JDK's
classes'), over `FileInputStream` and `FileOutputStream` on `HostFiles`' file table (handles 0,
1 and 2 are the host's standard streams). `RT`'s variant no longer touches `*out*`, `*err*`,
`*in*`: they are `RT`'s own fields with the JVM's initializers (an `OutputStreamWriter` over
`System.out`, a `PrintWriter` over one over `System.err`, a `LineNumberingPushbackReader` over
an `InputStreamReader` over `System.in`), all translated. `System.in`, `out` and `err` are
package variables of jrt that c2g writes (§11), initialized at jrt's package initialization by
jrt's Java `StandardStreams`, as `System.initPhase1` makes them: a `BufferedInputStream`, and
autoflush `PrintStream`s over a 128-byte `BufferedOutputStream` in `stdout.encoding` and
`stderr.encoding`. This is how T14's "`System.in`, `out` and `err` ... over them" is met: jrt's
hand-written code defines no `System` stream, and in jrt's own build (no translated classes)
there is none, `StderrPrint` writing to the host's `Stderr`.

## 10. The boundary to the evaluator

The evaluator is plan step 5; this section fixes what c2g gives it.

### 10.1 `Compiler` translated

`arbace.lang.Compiler` is translated like any class: the reader, the analyzer (`analyze`, the `Expr`
classes and their parsers), macroexpansion, `eval`. Its back end (the `emit` methods, `ObjExpr`'s
class generation, `Intrinsics`, everything naming ASM) is removed by its variant (§4.6), and its
`eval` paths that load generated classes (`FnExpr`, `NewInstanceExpr`) are replaced by evaluator
classes. **ASM is erased by package** (amendment E2, accepted 2026-10-09; EVAL-PLAN Q7, decided
2026-10-09): the variant file's `(c2g/erase "arbace/asm/")` makes every ASM value nil and its
computation skipped, so the `emit` methods, whose descriptors name ASM, do not exist in Go, and the
variant replaces only what loads or writes classes (`ObjExpr.compile` generates nothing,
`getCompiledClass` is nil, AOT compilation throws, `StaticInvokeExpr.parse` links nothing directly),
copying no line of `Compiler`. `bin/c2g --root 'arbace.lang.Compiler$*'` translates 94 of its 96
classes. Interop expressions resolve and run through reflection (§5.11): `InstanceMethodExpr`,
`StaticMethodExpr`, `InstanceFieldExpr`, `NewExpr` find their `Method`, `Field` or `Constructor`
with jrt's `Class.getMethods` and the rest at analysis, and their `eval` calls the invoker;
un-hinted calls go through `Reflector` at run time, as on the JVM. The invokers take Go
representations (§5.11), so the evaluator may call one with Go values directly, without boxing, on
hinted interop. A Clojure function passed where a functional interface is expected is adapted by the
`Reflector` variant's `boxArg` through `jrt.AdaptFn`, not `Proxy` (§7.11, amendment R13, accepted
2026-10-08).

**Closure compilation** (amendments EC1, EC2, EC5, accepted 2026-10-10; SPEED-NOTES.md, "The
evaluator: closure compilation"). The evaluator compiles each method of an evaluated fn,
deftype, defrecord or reify, at its first call, into a tree of `Code` nodes (the variant file
`arbace/lang/go/CompilerCode.clj`): locals as slot indexes, `long` and `double` locals unboxed
in the frame's `prims`, constants made once, a node of a primitive analyzed type answering
`runLong`, `runDouble` or `runBool` (the bytecode's `emitUnboxed`), `recur` by a frame flag;
`evalIn` remains the evaluation of top-level forms and of node kinds the compiler does not know
(compat mode). The public static methods of `Numbers`, `RT`, `Util` and jrt's `Math` with at
most three parameters of the kinds long, int, double, boolean or a reference are called
directly, as Go calls: `arbace/lang/go/CompilerOps.clj` (`CodeOp0` .. `CodeOp3`), generated by
`test/c2g/eval_ops.clj` from the JVM's classes and checked in, regenerated when those methods
change. Other resolved methods go through `Evaluator.invokeResolved` (their invoker directly,
when the declaring class is public), resolved constructors through jrt's
`Compiler_CodeRun_Construct_Constructor_O1__O`, exceptions unwrapped.

### 10.2 Functions

`fn*` evaluates to an instance of an **evaluator class**, written in class forms in a variant of
`Compiler`, holding the analyzed `FnExpr` and the closed-over values; its `invoke` methods run
the body. c2g translates these classes like the rest: an evaluated function is a Go value of a
struct type implementing `IFn` (and `AFunction`'s other interfaces), and translated code calls
it with a Go interface call, `(.Invoke_O__O f x)`. Plan step 7's closure compilation keeps the
classes and replaces the tree walk with Go function values.

**The evaluator's classes** (amendment E3, accepted 2026-10-09; EVAL-PLAN Q3, decided
2026-10-09) are member classes the `Compiler` variant adds (§4.6): `Compiler$Frame` (the slots
of one invocation, indexed by `LocalBinding.idx`), `Compiler$EvalFn`, `Compiler$Evaluator` (the
walk: `eval(Expr, Frame)`, `invokeFn`) and `Compiler$Dyn` (§5.12). **One `EvalFn` serves every
arity**, fixed or variadic: it extends `RestFn` with required arity 0, so every `invoke`
arrives at `doInvoke` with the arguments as a seq, and `invokeFn` picks the `FnMethod` of the
arity (fixed first, then the variadic one). This replaces the `EvalFn`/`EvalRestFn` pair this
section first gave as its example (`(instance? RestFn f)` is true of every evaluated fn; nothing
in the namespaces asks). Step 7 gives it per-arity `invoke` methods. `ObjExpr.eval` of a
`FnExpr` is an `EvalFn`.

**A run-time class per fn** (amendment M2, accepted 2026-10-09). Each `FnExpr` has a class
made at run time (`Compiler$Dyn.defineFnClass`), a subclass of `EvalFn` named as the JVM names
the fn's class (`arbace.core$map`, `user$eval12$fn__13`), held in `EvalFn`'s field `c2g$class`,
which `getClass` answers (§5.11): messages, `class`, printing and stack traces show the JVM's
names.

**A fn's class declares its closed-over locals** (amendment SL1, accepted 2026-10-10): one field
per closed-over local, named and typed as `ObjExpr.compile` declares them (not public; a primitive
local's field of its primitive type), whose reflective `get` reads the `EvalFn`'s captured value
(and `set`, for a reference type, writes it): `Compiler$Dyn.defineFnField`, a native c2g writes
(`arbace/c2g/dyn.clj`). The evaluator clears locals as compiled code does (EVAL-PLAN §2.1), so
such a field of a `^:once` fn reads null once the fn has run (Clojure's `clearing` tests).

**Primitive fns are evaluated boxed** (EVAL-PLAN Q1, decided 2026-10-09). An `EvalFn` cannot
answer `invokePrim`: one Go type cannot implement, per fn, whichever of `IFn`'s 322 primitive
interfaces (`IFn$LL` ...) the analyzer picks, and `Dyn` covers only the interfaces the world
uses. So the Go build's variant makes
`FnMethod.primInterface` return nil: the analyzer then makes no fn primitive and its callers
call `invoke`, not `invokePrim`. `invokeFn` converts a `^long` or `^double` parameter with
`RT.longCast`/`doubleCast` on entry and a primitive return on exit, so the arithmetic is the
JVM's. `IFn$LL` and the rest stay in the closed world for translated code.

### 10.3 Classes and resources by name

`RT.classForName` and `Class.forName` look names up in jrt's registry (§5.11): the closed world, the
classes the evaluator defined (§5.12), and array classes (`"[Ljava.lang.String;"`, Clojure's
`String/1`). There is one loader: `RT`'s variant makes `baseLoader` ignore the thread's context
loader, `makeClassLoader` return nil, and `classForName(String, boolean, ClassLoader)` call
`Class.forName` without `DynamicClassLoader` (amendment P4, accepted 2026-10-09). `RT.load` reads a
namespace's source from jrt's embedded resources (the `.clj` files of the REPL's namespaces,
embedded with `//go:embed`) or from the file system through the host, and evaluates it; there are no
`__init` classes. `DynamicClassLoader`, `FnLoaderThunk`, `Compile` are not translated.

**Embedded sources** (amendment M5, accepted 2026-10-09). `bin/c2g --program` writes the
namespaces' sources under the main package's forms directory
(`go/arbace/cmd/arbace/res/arbace/...`), marked as data for `bin/g2c` by a file `.g2c-data`
(BUILD.md, "The program's layout"), lists them as the package's `:embed-files` and declares
`//go:embed res`; `main` sets the host's `Resources` to `fs.Sub(resources, "res")` (§9.4)
before `Main.main`. The embedded tree is `arbace/**/*.clj` less the tools (`classes`, `j2c`,
`g2c`, `c2g`), the runtime's class forms (`arbace/lang`), ASM, and the namespaces the Go build
leaves out (JAVA-SURFACE.md decision 6; D6's Swing and SAX: `inspector`, `xml`)
(`arbace.c2g.embed/excluded`). `RT.load` (variant) finds `NAME.clj` or `.cljc` among the
embedded sources, then in the directories of the environment variable `ARBACE_PATH`, the class
path's counterpart; `ClassLoader.getResourceAsStream` (a table entry c2g writes, §11) searches
the same places. RT's initialization loads `arbace.core` when the program has its sources.

**The JDK's resource data** (amendment RD1, accepted 2026-10-10; JRT-NOTES.md, "The JDK's
resource data"). Besides the sources, `--program` embeds the data that `bin/jrt-convert` makes
(the directory `data` beside the `--jdk` input, `.tmp/jrt/data`: `java/lang/uniName.dat`,
`jdk/internal/icu/impl/data/icudata/nfc.nrm` and `nfkc.nrm`), as binary files under their
resource paths (`arbace.c2g.embed/data`). `Class.getResourceAsStream` is a method c2g writes on
jrt's `Class` (in `c2g_support`, when `ByteArrayInputStream` is translated; jrt cannot name the
translated stream): the name resolved as `Class.resolveName` resolves it (absolute without its
`/`, else in the package of the class or of an array class's element class), then searched as
`ClassLoader.getResourceAsStream` searches (`jrt.ResourceOrPath`), the bytes as a
`ByteArrayInputStream`, null when absent. Deviation: no module encapsulation of resources (the
JVM gives Clojure code nil for `java.base`'s; jdk26u's own classes read them as on the JVM).

**Prepared namespaces** (amendment U1, accepted 2026-10-09; EXEC-NOTES.md). The program holds an
image of its embedded namespaces analyzed at build time, and `RT.load` of an embedded source the
image holds replays it instead of reading and analyzing it (`Compiler$Image`, in the `Compiler`
variant): for each *unit* (a top-level form, or each form of a top-level `do`, as
`Compiler.eval` splits them) it decodes the `Expr` tree the analysis made, makes again the
*events* of that analysis (deftype's stub and class, `gen-interface`'s interfaces and methods,
`proxy`'s class, the boot `ns` macro's `*ns*`), and evaluates the tree as `Compiler.eval` does.
Evaluation is unchanged; only the analysis is skipped, as for the JVM's AOT-compiled namespaces.
The image is recorded by the program itself, run once with `ARBACE_PREPARE=FILE` requiring every
embedded namespace (`bin/arbace-go --build`): `Compiler.load` hands each top-level form of an
embedded source to `Compiler$Image.evalUnit`, which records the unit between its analysis and
its evaluation, with the events gathered while the analysis runs (`genclass.clj` and
`core_proxy.clj` report theirs, the `RT` variant's boot `ns` macro its own). Objects are encoded
by Go's reflection over the program's struct types, by name (`Var`, `Keyword`, `Namespace`,
`Class`, members), by value (strings, boxes, arrays, patterns) or as the canonical empty
collections and `Compiler`'s constant nodes; the graph keeps its sharing and the objects their
identity hashes; fields only the bytecode back end reads (`ObjExpr.src`, the clearing paths) are
not encoded. A load from source (`ARBACE_PATH`, `load-file`, a program without an image,
`ARBACE_NO_IMAGE`) is as before.

**Namespace variants** (amendment M1, accepted 2026-10-09). The Go build's differences in the
namespaces live in `arbace/lang/go/ns/`, applied by `--program` when it embeds the sources:
`P.clj` replaces `arbace/P.clj`, `P.after.clj` is appended to it, and `P.subst.clj`, a vector of
strings `[old new ...]`, replaces each `old`, which must occur exactly once. They are
`genclass.clj` (`gen-interface` at run time, `gen-class` throws), `core_proxy.clj` (§10.4),
`instant.clj` (JAVA-SURFACE.md decision 7), `main.after.clj` and `main.subst.clj` (the REPL's
trimmed start, no `DynamicClassLoader`, the error report printed with `prn`), and
`java/io.subst.clj` (no reflection warnings; `copy` between two files through their streams,
not their channels; the escape of `+` written out as `"%2B"`, not computed with `URLEncoder`).
Since files are in the world (amendment FS6, accepted 2026-10-10), the error report goes to a
temporary file through `Files/createTempFile` as on the JVM, and `Compiler.loadFile` has no
variant: `load-file` is the JVM's, with `File`'s absolute path and name.

**The class forms' analysis** (amendment CF2, accepted 2026-10-10; CLASSFORMS-REPL.md). The
embedded tree holds the class forms compiler's analysis (`arbace/classes/types`, `env`, `parse`,
`lower`, `analyze`; not `emit`, `compiler`, `shape`, `verify`, `build`, `boot`), with namespace
variants in `arbace/lang/go/ns/classes/`: `types.subst.clj` (the runtime's package name),
`env.subst.clj` (a `HashMap` for reflection's cache; the constructors jrt's reflection does not
list), `analyze.subst.clj` (ASM's `TypeReference` and the world's generic signatures from
`arbace.classes.go`; no source path lookup), `native.subst.clj` (the classes interpreted:
`arbace.classes.interp`'s `compile-and-load!`), and the Go build's own namespaces `go.clj` and
`interp.clj`. `Compiler.classForms`' variant loads them on the first class form without
checking the macros' specs, as the JVM loads them compiled.

**The world's generic signatures** (amendment CF5, accepted 2026-10-10). `--program` also
embeds `arbace/classes/generics.edn`: the generic view (`class-generics`) of the world's interfaces
and of the classes an interpreted class can extend, and the constructors of abstract ones, which
jrt's reflection does not have and javac's bridges need (`arbace.c2g.dyncf/generics-text`).

### 10.4 Types made at run time

`deftype`, `defrecord` and `reify` create a `jrt.Class` at run time (name, `Object` as
superclass, interfaces, fields, methods as closures, member tables for reflection) and their
instances are `Dyn` objects (§5.12), through `Compiler$Dyn`'s natives (amendment E4, accepted
2026-10-09): `NewInstanceExpr.compileStub` becomes `defineClass`, each method an `EvalFn`-like
fn set per descriptor by `setMethod` (a covariant override answers every signature
`gatherMethods` gives it), a constructor call `newInstance`. Protocols work as on the JVM:
`instance?` on the protocol's interface, and the class-keyed method cache on `Class` objects.

**Interfaces made at run time** (EVAL-PLAN Q5, decided 2026-10-09): `definterface` and
`defprotocol`'s `gen-interface` are `Compiler$Dyn.defineClass` of an interface kind: a
`jrt.Class` with the interface's methods in its member table, no Go type. A `Dyn` class
implementing one lists it among its interfaces (`instance?` holds through the nominal check of
§5.12); its methods, which no Go interface declares, are called by reflection (`Reflector`, the
class's member table), so inline protocol implementations in `deftype` work as reflective calls
until step 7. This is not §12's `gen-interface`, which generates a class file.

**`proxy` over `Dyn`** (amendment X2, accepted 2026-10-09; it reverses D6 for `proxy`, B1-PLAN.md;
`gen-class` stays out). The namespace variant `core_proxy.clj` (§10.3) keeps upstream's
`proxy-name`, `get-proxy-class`, `construct-proxy`, `init-proxy`, `update-proxy`,
`proxy-mappings`, `proxy`, `proxy-call-with-super` and `proxy-super`, and replaces
`generate-proxy` with `define-proxy`: a class made by `Compiler$Dyn.defineProxyClass` (§5.12),
named and cached as on the JVM (`user.proxy$java.lang.Object$IDeref$...`), whose methods are the
ones `generate-proxy` chooses (the superclass's non-final public and protected instance methods,
the interfaces' and the abstract ones, the bridges of covariant groups; of `Object`'s, the three
a `Dyn` can override), each set to a fn that looks its name up in the fn map (field 0) and
applies the mapped fn to the proxy and the arguments; without one, a superclass method answers
the super marker and an interface or abstract method throws `UnsupportedOperationException`
with its name, as the generated bytecode does. `bean` is upstream's over the readable properties
found by reflection as `java.beans.Introspector` finds them (`java.beans` is not in the Go
build). Not provided: proxies of leaf classes and of classes outside §5.12's list,
serialization of proxies, and methods of more than 18 parameters (the JVM calls super for them;
here the fn is called).

**Class forms at the REPL** (amendment CF3, accepted 2026-10-10; it reverses D6 for `defclass`
and the code forms, B1-PLAN.md; `gen-class` stays out). `defclass` and the code forms (SPEC
§9.5) are analyzed by the embedded analysis (§10.3) and interpreted (CLASSFORMS-REPL.md):
`arbace.classes.native`'s boundary is kept, `compile-and-load!` is `arbace.classes.interp`'s,
which builds from the analyzed nodes trees of the interpreter's nodes (`Compiler$CF`, class forms
in the `Compiler` variant `arbace/lang/go/ClassForms.clj`, translated), one per method, and
makes the classes through the host `Compiler$CFGo` (§5.12, amendment CF4): classes made at run
time whose objects are `Dyn` (DynSub_C for a superclass of the world, `Compiler$CF$FnObj` for a
Clojure fn's class), their members in the member tables and `Dyn`'s slots.

### 10.5 Recursion depth

Go's stack overflow is fatal (V1). The evaluator counts its own depth (fn invocations,
`eval` of nested forms) per thread and throws `StackOverflowError` beyond a limit (§16 Q15);
translated code does not count. The counter is per thread, kept by `invokeFn` (EVAL-PLAN Q4,
decided 2026-10-09), and the limit is about **10,000 evaluator frames**, about the JVM's default
depth on simple recursion and far below Go's 1 GB stack.

### 10.6 The program

`arbace/cmd/arbace`'s `main` (hand-written Go forms) sets up jrt (host, main thread, slot),
then calls `arbace.main`'s entry through the evaluator as `clojure.main` would (`RT.init`,
`require`, `apply`), inside jrt's thread entry (§7.9.6): `(os/Exit (jrt/RunMain (fn [] ...)))`,
`jrt.RunMain` making the calling goroutine the thread `main` and returning the exit status
(§8.4, amendment T13, accepted 2026-10-08).

**`--program`** (amendment W6, accepted 2026-10-09). `bin/c2g --program` translates the whole
program, without a slice: `arbace.lang.Main#main` is a root, and c2g writes the main package
`arbace/cmd/arbace` (`DIR/prog/go/arbace/cmd/arbace.clj`), whose `main` passes the command line
as a `String[]` to `Main.main` inside `jrt.RunMain`:

```clojure
(go/func main []
  (let [args (jrt/NewRefArray jrt/String_class (conv int32 (- (len os/Args) 1)))]
    (range [i a (subslice os/Args 1)]
      (aset (.-A args) i (jrt/Str a)))
    (os/Exit (jrt/RunMain (fn [] (lang/Main_Main_String1__V args))))))
```

So the main package is c2g's output, not hand-written, and `Main.main` takes the place of the
calls above. Until the evaluator loads `arbace.core` (step 5), `RT`'s initialization runs and
`RT.doInit` stops at `arbace.core/refer`, unbound; the uncaught exception is printed as the
JVM prints it, with class forms lines (§7.9.6), and the status is 1. Measured: 668 classes and
5,022 methods reached; a cold `--line-file` build 30-55 s, warm 9-16 s; a static executable of
22-24 MB; 7 ms from start to that exception on amd64. Since step 5, `--program` also embeds the
namespaces' sources (§10.3, amendment M5) and registers the classes they name outside the world
(§4.1, M3), and the program loads `arbace.core` and runs `arbace.main` (`bin/arbace-go`;
EVAL-NOTES.md). `RT.doInit` then loads `arbace.core.server` and starts the socket servers of
the `arbace.server.*` properties as the JVM's does (amendment NT1, accepted 2026-10-10;
`System.getProperties()` through the reflection tables; about 20 ms with the image of prepared
namespaces).

**The main package's files** (amendment U3, accepted 2026-10-09). Besides `main.go` and the
embedded sources (`res/`), `--program` writes `image.go` (the image's encoding, hand-written Go
forms `go/arbace/cmd/arbace/image.clj`, copied), `image_types.go` (generated: every exported,
non-generic struct type of `arbace/lang` and `arbace/jrt`, registered by name for decoding) and an
empty `image.bin`, embedded as a string (`//go:embed image.bin`), which `bin/arbace-go --build`
replaces with the prepared image before linking the executables again. `main` installs the image
(`setupImage`) before `Main.main` and writes it after, when preparing (`finishImage`); jrt reaches
it through `jrt.ImageHooks` (`jrt.Image`), which `Compiler$Image`'s natives call. A variant of
`Main` tells the image when `arbace.main` is loaded (`Compiler$Image.started`).

**The REPL's world** (EVAL-PLAN Q2, decided 2026-10-09; amendment P2): what the REPL can name
is the closed world, and it reaches members by reflection, which reachability does not follow
(§4.1). So the REPL's program roots **every public member of every built-in class** (all of
`arbace.lang` and the JDK closure's public API, each class `--root C`), so that reflection finds
translated code, not stubs that throw. **Except the classes of JDK packages their module does not
export** (`jdk.internal.*`, `sun.*`; the running JDK's boot layer decides): the JVM refuses
Clojure code access to them (`IllegalAccessError`), so rooting them only grew the executable;
classes of no JDK module (Arbace's, jrt's own `jdk.internal.jrt`) stay rooted (amendment RD2,
accepted 2026-10-10; `arbace.c2g.main/repl-visible?`).

**The Java API** (amendment SL6, accepted 2026-10-10): `--program` also translates
`arbace/java/api/Clojure.clj`, so `arbace.java.api.Clojure` (`Clojure/var`, `Clojure/read`; its
static initializer requires `arbace.edn` at first use) exists in the Go build as on the JVM. It is
a class of the program, not an embedded namespace's source.

### 10.7 Stack traces

The evaluated Clojure code's frames are not Go frames: the Go stack shows only the
evaluator's methods (§7.9.6). So the evaluator records the **Clojure fn and line per frame**
(EVAL-PLAN Q6, decided 2026-10-09): `Compiler$Frame` holds the running fn and, in a field,
the line of the form being evaluated, and the `StackTraceElement`s of the evaluated code come
from the chain of frames (the fn's name and line, as the JVM's compiled fns give them), not from
the evaluator's Go frames, which are unreadable to the user. How they join the translated
frames of §7.9.6 is step 5's.

**Evaluated frames, as built** (amendment M6, accepted 2026-10-09). When an exception is made,
jrt asks the evaluator for the current thread's evaluated frames through a hook, `jrt.EvalTrace`
(a `Supplier` the evaluator sets from `Compiler$Evaluator`'s static initializer, through the
native `Compiler$Evaluator.setTraceHook`), as `StackTraceElement`s: the fn's class name, `invoke`
or the method's name, the source file the fn was analyzed in, the line of the call being
evaluated (`Compiler$Frame`, per-thread `Compiler$EvalState`). jrt's frame mapping (§7.9.6) puts
each in place of the Go frame of the evaluator's call body (`Evaluator.invokeFn`'s `try`
literal) and leaves out the evaluator's own frames, the `Dyn` and `DynSub_C` dispatch, member
tables' invokers, and the reflective call frames under an evaluated host call, which compiled
code calls directly. So `arbace.main`'s error report names the Clojure frame (`Execution error
(ArithmeticException) at user/f (REPL:1)`).

With the closure compiler (amendment EC5, accepted 2026-10-10) the call body is still
`Evaluator.invokeFn`'s and `invokeMethod`'s `try` literal; the compiled nodes' frames
(`arbace/lang.(*Compiler_Code...`, `Compiler_Code...`, jrt's `Compiler_CodeRun_` natives) are
the evaluator's own, and a `CodeHost*` node (a host call) hides the reflective frames above it,
as the host nodes' `evalIn` did. A compiled node sets the frame's line before each call, so the
line is the call's.

## 11. jrt's API as c2g uses it

The Go identifiers c2g's output refers to, all in package `arbace/jrt`. c2g checks that no Java
class of the closed world has one of these names (§4.4). The table is the API as jrt implements
it; names in **bold** were added by amendments J5 (jrt's core), R14 (reflection and the shims)
and T11, T13, T14 (threads and the host), accepted 2026-10-08.

| group | names |
|---|---|
| object model | `Object` (the header struct; methods `Self_Object`, `HashCode__I`, `Equals_O__Z`, **`ClearHeader`**), `Object_I`, **`Object_New`**, **`Object_class`**, `Object_toString`, `Equals`, `HashCode`, `ToString`, `GetClass`, `IdentityHash`, **`CloneNotSupported`** |
| classes | `Class` (Java methods by §4.4's names; Go methods **`Info`**, **`GoName`**, **`ArrayClass`**), `ClassInfo`, `FieldInfo`, `MethodInfo`, `CtorInfo`, `Define` (registration), **`ForName`**, **`Kind`** (`KindClass` ... `KindPrimitive`), **`Acc...`** (modifier bits), **`Prim_boolean` ... `Prim_void`**, **`RegisterGoType`** (§9.2), `Class_ForName_String__Class`, `Class_GetPrimitiveClass_String__Class` |
| reflection | **`As`** (a reference argument's conversion), **`Box`**, **`Unbox`** (§5.11), **`AdaptFn`** (§7.11), **`Method.Info`**, **`Constructor.Info`**, **`Field.Info`** |
| arrays | `BooleanArray` ... `DoubleArray`, `RefArray` (fields `A`, `Comp`; methods `Store`, **`Copy`**); `NewIntArray` ..., `NewRefArray`, `NewMultiArray`, `IntArrayOf` ..., `RefArrayOf`; **`Arraycopy`**, **`IndexOutOfBounds`**, `System_Arraycopy_O_I_O_I_I__V`, `System_IdentityHashCode_O__I`, `Array_NewInstance_Class_I__O` (§5.9) |
| class initialization | `ClassInit` (`Done`, `Run`) |
| exceptions | `Throwable_I`, `Throwable`, `Thrown`, `Catch`, **`FromRuntimeError`**, `NN` (the receiver check, §5.6), `NPE`, `ClassCast`, **`StackTraceString`**, **`Uncaught`**, **`StderrPrint`**, **`FrameInfo`**, **`FrameElide`**, **`RegisterFrames`** (§7.9.6), `StackTraceElement` |
| conversions | `D2I`, `D2L`, `F2I`, `F2L`; `NaN32`, `NaN64`, `PosInf32` ..., `NegZero32`, `NegZero64`; **`FormatDouble`**, **`FormatFloat`**; the natives `Double_DoubleToRawLongBits_D__J_native` and the other three (§9.1); **`Math_PI`**, **`Math_E`**, **`Math_TAU`** and `StrictMath`'s, as constants |
| strings | `String` (Go methods **`String`**, **`UTF16`**), `Intern`, **`InternUTF16`**, **`Str`**, **`NewStringUTF16`**, `Concat`, `StrOfInt`, `StrOfLong`, `StrOfChar`, `StrOfBool`, `StrOfFloat`, `StrOfDouble`, `StrOfObj`, **`DecodeUTF8`**, **`EncodeUTF8`**, **`DecodeBytes`**, **`EncodeBytes`**, `StringBuilder`, `StringBuffer`; **`Charset.Aliases`**, **`Date.InstantText`** |
| monitors, memory | `MonitorEnter`, `MonitorExit`, `Wait`, **`WaitTimeout`**, `Notify`, `NotifyAll`, **`HoldsLock`**, `Volatile` (`Load`, `Store`, **`CompareAndSet`**, **`Swap`**), **`Inflated`** (a counter) |
| threads | **`CurrentThread`** (`*Thread`), `Thread`, **`RunMain`**, **`Go`**, **`RunnableOf`**, **`WaitNonDaemon`**, **`Thread_defaultHandler`** (§8.4) |
| the host | **`Host`**, **`HostFile`**, **`HostFileInfo`**, **`OSHost`**, **`SetHost`**, **`CurrentHost`**, **`Stdin`**, **`Stdout`**, **`Stderr`** (§9.4); the natives of jrt's own Java (§9.1): *`HostFiles_Open0_String_I_String1__I_native`*, *`HostFiles_Read0_I_B1_I_I__I_native`*, *`HostFiles_Write0_I_B1_I_I__V_native`*, *`HostFiles_Available0_I__I_native`*, *`HostFiles_Skip0_I_J__J_native`*, *`HostFiles_Close0_I__V_native`* |
| run-time classes | *`Dynamic`* (`DynImplements`), *`DefineDynamic`*, *`Class.Descriptor`* (§5.12) |
| c2g's helpers (`c2g_support.go`, written by c2g into jrt's package) | *`C2g_Missing`* (§4.1), *`C2g_NotTranslated`*, *`C2g_NotNull`*, *`C2g_Truth`* (a `Boolean` or `Object` condition), *`C2g_CastArray`*, *`C2g_Discard`* (§5.7), *`C2g_RefP`*, *`C2g_ObjectClone`*, *`C2g_ObjHash`*, *`C2g_ObjEquals`*, *`C2g_DoubleBits`*, *`C2g_FloatBits`* (§7.13); *`String_Format_String_O1__String`* and its `Locale` overload; jrt's members with translated values (K2, below): *`System_in`*, *`System_out`*, *`System_err`*, *`System_SetIn_InputStream__V`*, *`System_SetOut_PrintStream__V`*, *`System_SetErr_PrintStream__V`*, *`String_CASE_INSENSITIVE_ORDER`*, and `String`'s Go methods *`Split_String__String1`*, *`Split_String_I__String1`*, *`ReplaceAll_String_String__String`*, *`ReplaceFirst_String_String__String`*, *`Matches_String__Z`*; jrt's members naming translated classes (S4, below): *`String_Join_CharSequence_Iterable__String`*, `String`'s Go methods *`Formatted_O1__String`*, *`Lines__Stream`*, *`DescribeConstable__Optional`*, *`C2g_PrintStackTraceWriter`*, *`C2g_PrintStackTraceStream`*, *`C2g_SystemGetenv`*, and the default-method forwarders of §5.4 |
| natives of `arbace.lang` and of the JDK's variants (hand-written, §9.1) | *`Reflector_AdaptFn_Class_O__O_native`*; the evaluator's (M6 and step 5): *`Compiler_Evaluator_SetTraceHook_Supplier__V_native`*, *`Compiler_Evaluator_MonitorEnter_O__V_native`*, *`Compiler_Evaluator_MonitorExit_O__V_native`*, *`Compiler_Evaluator_CheckCast_Class_O__O_native`*, *`RT_HostResource_String__B1_native`*; `UUID`'s variant's (M7; the MD5 since step 5's phase 2B): *`UUID_HostRandomBytes_B1__V_native`*, *`UUID_Md5_B1__B1_native`* (P1's `RT_HostWriter_...` and `RT_HostReader_...` natives are gone with amendment K2, §9.4) |

The hand-written classes (`Object`, `String`, `StringBuilder`, `Class`, `Throwable`, `Enum`,
`Record`, `Thread`, `Math`, `System`, the reflection classes, atomics, locks ...) are used through
their Java API by the names of §4.4, like translated classes; the manifest (§9.1) lists them,
and so do the structure tables of [JRT-NOTES.md](JRT-NOTES.md) (amendment R14).

Names in *italics* were added on 2026-10-09 by amendments C2, C4, C5, C7 (the helpers), P1, E4
and E6, K1 (`HostFiles`' natives) and K2 (jrt's members c2g writes, below), and, with step 5, the
evaluator's natives (by P1's rule; the trace hook is M6's), `UUID`'s (M7) and more members c2g
writes (S4, below).

**`String.format`** (amendment C5, accepted 2026-10-09): when `java.util.Formatter` is
translated and jrt does not define them, c2g generates `String_Format_String_O1__String` and
`String_Format_Locale_String_O1__String` in `c2g_support.go`, over `Formatter`, as `String.format`
is in jdk26u; jrt's hand-written `String` cannot call a translated class otherwise. The
helpers `C2g_*` are generated likewise, so that their names are c2g's (§4.4's check covers
them).

**jrt's members that c2g writes** (amendment K2, accepted 2026-10-09). For the same reason,
jrt's members whose values are translated objects, or whose code calls translated classes, are
written by c2g into `c2g_support.go`, each only when its classes are translated:

- `System_in`, `System_out`, `System_err` (package variables of jrt, declared to the analyzer as
  `System`'s static fields, their classes' members rooted) and `System.setIn`, `setOut`,
  `setErr` (`System_SetIn_InputStream__V` ...), when `jdk.internal.jrt.StandardStreams` is
  translated: an `init` of jrt's package sets the three from `StandardStreams.in()`, `out()`,
  `err()` (§9.4), and redirects `StderrPrint` through `System.err` (`print`, then `flush`; the
  host's stream while `System.err` is null), so that `printStackTrace()` and uncaught
  exceptions print to `System.err` and follow `setErr`, as on the JVM;
- `String_CASE_INSENSITIVE_ORDER`, set by the same `init` to the instance of
  `jdk.internal.jrt.CaseInsensitiveComparator` (registered as
  `java.lang.String$CaseInsensitiveComparator`, §4.1);
- `String`'s methods over `java.util.regex`, Go methods of jrt's hand-written `String` declared by
  c2g when `Pattern` is translated: `split` (both), `replaceAll`, `replaceFirst`, `matches`,
  as jdk26u's `String` defines them (`split`'s one-character fast path gives what
  `Pattern.split` gives, so it is left out):

```clojure
(go/method Split_String_I__String1 "Split_String_I__String1 is String.split(regex, limit).\n"
  ^{:tag (* RefArray)} [^{:tag (* String)} t ^{:tag (* String)} regex ^int32 limit]
  (.Split_CharSequence_I__String1 (Pattern_Compile_String__Pattern regex) t limit))
```

**jrt's members naming translated classes** (amendment S4, accepted 2026-10-09). The same rule
covers the members of hand-written classes whose signatures or code name translated classes,
each written when its classes are translated: `String.join(CharSequence, Iterable)`,
`formatted`, `lines` (over an `ArrayList`'s stream: sequential, not jdk26u's lazy spliterator)
and `describeConstable`, as Go functions and methods in `c2g_support.go` with their entries in
the member tables (`reflect_tables_c2g.go`, whose `init` runs after jrt's `reflect_tables.go`);
`Throwable.printStackTrace(PrintStream)` and `(PrintWriter)` (the trace's text printed) and
`System.getenv()` (the host's environment, unmodifiable) and `System.getProperties()` (a
`Properties` copy of the properties, when `Properties` is translated; NT2), as functions `C2g_*`
with table entries; `Date.toInstant` and `Date.from(Instant)` and `ClassLoader.getResourceAsStream` (§10.3)
as table entries only, for reflection. Translated code calling a member that exists only as a
table entry is an operation-level stub (§4.1; none does). And the forwarders of hand-written
leaf classes to translated default methods (§5.4).

## 12. Out of scope

D6 and the evaluator make these unnecessary for B1a; each comes back when its Go-side meaning is
defined.

- **Class loading and bytecode**: `ClassLoader`, `defineClass`, `DynamicClassLoader`, class files,
  ASM, `Compiler`'s back end, the class forms compiler at run time (`defclass` and the code forms
  at the REPL), `gen-class`, `gen-interface` as class generation (the interfaces the evaluator
  makes at run time are §10.4's, EVAL-PLAN Q5).
- **`java.lang.reflect.Proxy`** (the functional interface adaptation is done by adapters,
  §7.11). `InvocationHandler` exists as an interface; `Proxy.newProxyInstance` throws
  `UnsupportedOperationException`, and `Reflector` adapts functions through `jrt.AdaptFn`
  (amendment R13, accepted 2026-10-08). Clojure's `proxy` is no longer out of scope: it is made
  over `Dyn` (§10.4, amendment X2, accepted 2026-10-09), but for proxies of leaf classes and of
  classes outside §5.12's list.
- **`invokedynamic` as such**: `java-str` and lambdas are compiled directly (§7.5, §7.11);
  `KeywordInvokeSite`, `ReflectorCallSite`, `MethodHandle`, `VarHandle`, `MethodType` and
  signature polymorphic calls are reworked or cut (JAVA-SURFACE.md). A signature polymorphic
  call in translated code is a translation error.
- **Serialization** (`writeReplace`, `readObject`, serializable lambdas' `$deserializeLambda$`):
  cut with `ObjectInputStream`/`ObjectOutputStream`; their members are cut by variants.
- **Annotations at run time** and generic reflection (`getAnnotation`, `ParameterizedType`)
  (amendment R16, accepted 2026-10-08):
  - no annotations are kept: `isAnnotationPresent` is false, except
    `isAnnotationPresent(FunctionalInterface)`, answered by `ClassInfo.FromFn != nil` (§7.11),
    which `Compiler.FISupport` needs;
  - generic reflection returns plain `Class` objects: `getGenericInterfaces` and
    `getGenericSuperclass` give the classes in a `Type[]` and as a `Type`, so no
    `ParameterizedType` is ever seen (`HashMap.comparableClassFor` finds none and orders tree
    bins of comparable keys by `tieBreakOrder`, which leaves iteration order unchanged);
    `getTypeParameters` and `getGenericParameterTypes` are cut;
  - `getExceptionTypes` is empty and `Method.toString` has no throws clause
    (`NewInstanceExpr`'s `exclasses` feed only the class writer, which the evaluator has not).
- **Modules and packages** as run-time objects (`Module`, `Package`), the security manager.
- **Finalization** (V8), thread groups and priorities (V10).
- **Locales** (amendment R17, accepted 2026-10-08; refines JAVA-SURFACE.md decision 2): the
  default locale is `en_US` (`Locale.getDefault()`, for every `Locale.Category`), as a JVM
  started in an English, US environment has it, so that `Formatter` takes its `Locale.US` path
  for grouping, which needs no `DecimalFormat` (cut). Its data is the root locale's
  (`DecimalFormatSymbols`, the number patterns), with the currency `$`/`USD` for the United
  States and `¤`/`XXX` otherwise. `String.toLowerCase(Locale)` and `toUpperCase(Locale)` apply
  the root locale's mapping for every locale: no Turkish, Azerbaijani or Lithuanian rules.
- **Charsets** (amendment R18, accepted 2026-10-08; V9): three only, UTF-8, ISO-8859-1 and
  US-ASCII, with JDK 26's names and aliases; `Charset.forName` throws
  `UnsupportedCharsetException` for any other legal name (`UTF-16` included, and
  `StandardCharsets.UTF_16*` are left out), and the `String` constructors and `getBytes` taking
  a name throw `UnsupportedEncodingException`. Encoders, decoders and buffers (`java.nio`) are
  cut. **Six since amendment S6** (accepted 2026-10-09): `UTF-16`, `UTF-16BE` and `UTF-16LE`
  are added (`sun.nio.cs.UTF_16`, `UTF_16BE`, `UTF_16LE`, extending `Unicode`, with JDK 26's
  names and aliases, and `StandardCharsets.UTF_16*`), coded as jdk26u's
  `UnicodeDecoder`/`UnicodeEncoder` with `String`'s REPLACE (a byte-order mark read by `UTF-16`
  at the start, big-endian without one, and written by its encoder before a non-empty string;
  unpaired surrogates and a trailing odd byte as U+FFFD). The stream adapters of jrt's own Java
  (`InputStreamReader`, `OutputStreamWriter`, §9.4) still code the first three only and throw
  `UnsupportedOperationException` for the others.

## 13. Performance

Measured on this machine (AMD EPYC 7763, linux/amd64, go1.27.1 from `/root/tamago-go`,
`go test -bench -cpu 1`, `GOAMD64=v1`), with the experiments in `.tmp/c2g-exp/` (scratch, not
tracked; the programs are described with each number). The numbers are per operation and
include the benchmark loop.

### 13.1 Dispatch and type tests

| operation | ns | experiment |
|---|---:|---|
| direct call of a non-inlined function | 1.9 | `dispatch`, `CallDirectNoinline` |
| Go interface call, one receiver type | 3.5 | `CallIfaceMono` |
| Go interface call, two types alternating | 4.1 | `CallIfacePoly` |
| Go interface call, three types in a pseudo-random order | 10.2 | `HashViaObjIface` (branch misprediction, as for any indirect call) |
| explicit vtable call (class pointer → table → function) | 1.9 | `VtableCall` (the table loads hoisted out of the loop by gc: a lower bound; §13.2 compares the two models on real work) |
| `any` → interface assertion, then call, two types | 4.4 | `CallAnyAssertIfacePoly` |
| the same, three types random | 14.7 | `HashViaAnyAssert` |
| `instanceof` interface (comma-ok assertion), two types | 1.9 | `InstanceofIfacePoly` |
| `instanceof` leaf class (pointer type assertion) | 1.3 | `InstanceofConcretePoly` |
| up-cast interface → interface (`convI2I`) | 2.2 | `UpcastIfaceToIfacePoly` |
| up-cast interface → `any` | 1.1 | `UpcastIfaceToAnyPoly` |
| up-cast pointer → interface | 0.6 | `UpcastConcreteToIface` |
| nil-preserving up-cast pointer → `any`: per-type method / generic / plain | 0.97 / 1.43 / 0.95 | `UpPerType`, `UpGeneric`, `UpRaw` |
| field read on a pointer | 0.65 | `FieldConcrete` |
| field read through a class interface's accessor, one type | 1.95 | `FieldViaAccessorMono` |

Choices: Go interfaces for dispatch (an indirect call either way, §13.2; they give type tests
and the itab cache for free); `any` for `Object` (up-casts to `Object` are the most frequent
conversion and the cheapest); pointers for leaf classes (direct, inlinable calls and field
reads, the largest win, §5.3); nil-preserving conversions as generated methods, not generics.

### 13.2 References: interface values or thin pointers

The alternative object model makes every reference a one-word pointer to a header holding a
class pointer, with explicit vtables and interface tables and `unsafe` casts. A model of both
(`model`: a 1M-element cons list of boxed longs, built and walked through an `ISeq` interface; a
32-way trie of depth 3 indexed at random):

| | interface values (chosen) | thin pointers |
|---|---:|---:|
| build 1M conses with boxed longs | 100 ms, 64 MiB | 100 ms, 56 MiB |
| walk the list | 4.6 ms | 4.9 ms |
| trie lookup (`nth`) | 57 ns | 54 ns |
| `Object[]` slot | 16 bytes | 8 bytes |

Speed is the same; thin pointers save memory where references dominate (an `Object[32]` vector
node is 512 bytes against 256) and make every reference CAS-able and tear-free (§8.3, §9.2), but
need `unsafe` in all translated code, an interface dispatch of jrt's own and the loss of Go's
type tests. §16 Q1.

### 13.3 Exceptions and threads

| operation | ns | experiment |
|---|---:|---|
| a call | 2.0 | `exc`, `PlainCall` |
| the same call inside a `try` (function literal, `defer`, `recover`), not throwing | 9.0 | `TryNoThrow` |
| inside `try`/`finally`, not throwing | 6.0 | `TryFinallyNoThrow` |
| throw and catch, one frame deep | 305 | `TryThrowCatch` |
| throw and catch, 20 frames deep | 795 | `TryThrowCatchDepth20` |
| capture a stack trace (`runtime.Callers`) | 240 | `CallersCapture` |
| goroutine id from `runtime.Stack` (the stopgap) | 2,970 | `GoidRuntimeStack` |
| `ThreadLocal` through that id and a `sync.Map` | 3,110 | `ThreadLocalViaGoidSyncMap` |
| `currentThread()` through the runtime slot | 3.7 | `gls`, `CurrentThread`, with the runtime patch as an overlay |

A JVM throw with a stack trace costs about a microsecond too; Clojure's runtime does not use
exceptions for ordinary control flow, so the 9 ns of an entered `try` matters more than the
throw. The slot is 800 times faster than the stopgap, which would cost about 3 µs on every
dynamic var access while bindings exist.

### 13.4 Statics, locks, allocation

| operation | ns | experiment |
|---|---:|---|
| static field read, no guard / with an inlined `C_Init()` guard | 0.32 / 0.39 | `exc`, `StaticGetNoGuard`, `StaticGetGuardInline` |
| non-inlined static method, without / with guard at entry | 1.9 / 2.2 | `StaticGetUnguardedCall`, `StaticGetGuardedCall` |
| `sync.Mutex` lock and unlock | 5.5 | `SyncMutexLockUnlock` |
| thin lock in the header (CAS), lock and unlock | 5.0 | `ThinLockUnlock` |
| allocate a 16-byte object (a boxed `Long`) | 20 | `alloc`, `AllocLong16` |
| allocate a 64-byte object (a `Cons`: header and three references) | 31 | `AllocCons56` |
| build 1M conses with a large live heap, `GOGC=100` / `GOGC=400` | 100 / 53 ms | `model`, `FatBuildList` |

Allocation is the main expected cost against the JVM (whose TLAB allocation is a pointer bump):
Clojure allocates freely (seqs, boxed numbers, persistent updates). Remedies, in order: `GOGC`
(and `GOMEMLIMIT`) tuned by jrt at start (halved the build time above), fewer allocations in
hot paths (the evaluator's environments, plan step 7), and later Go's profile-guided
optimization. Boxing follows Java (`Long.valueOf` caches -128 to 127); a larger cache is allowed
by Java's specification and would be a jrt choice, not c2g's.

**The entry guard, measured** (amendment W3, accepted 2026-10-09). On Go benchmarks of
translated functions, the guard at a static method's entry cost more than the 0.3 ns above: it
kept gc from inlining small static methods (`Integer.rotateLeft` into `Murmur3`), so
`Murmur3.hashLong` took 9.4 ns with it and 3.7 without, `hashUnencodedChars` 36.5 and 26.3.
§6.2's benign initialization removes it where that cannot be observed.

**The collector, decided by step 7a** (amendment O1, accepted 2026-10-10, closing D7;
SPEED-NOTES.md, "Step 7a"). With Go's defaults the mark workers took about 60% of all CPU (64
processors), and a program with a small live heap collected hundreds of times a second (Go's 4
MiB minimum heap). jrt's package initialization sets `GOGC=200` unless `GOGC` is set, and a
64 MiB minimum heap unless `ARBACE_MIN_HEAP_MB` sets another (0: none): a ballast, a byte slice
never written and without pointers, so neither scanned nor resident, raising the collector's
goal as a JVM's initial heap does (Go has no minimum-heap setting; `GOMEMLIMIT` only lowers the
goal). **The start's collector** (amendment U4, accepted 2026-10-09, EXEC-NOTES.md): while the
program starts, until `Main.main` has loaded `arbace.main`, the main package runs the collector
at `GOGC=400` unless `GOGC` is set, then restores jrt's setting. Arrays of references and
strings are one allocation (O2, §5.9, §7.5); the 16-byte slots stay (§13.2's thin pointers not
taken). `bin/c2g-perf`'s workloads went from 3.7-49 times the JVM's time to 2.3-32 times.

**Profiles and profile-guided builds** (amendments O7 and O5, accepted 2026-10-10).
`ARBACE_CPUPROFILE=FILE` and `ARBACE_MEMPROFILE=FILE` write Go's CPU and heap profiles of any
program run under `jrt.RunMain`; allocation sampling is off otherwise (its stack walks cost
about 2% on the evaluator's deep stacks). `bin/arbace-go --build --pgo` runs the host's
executable on `test/arbace-go-pgo.clj` with a CPU profile and builds every executable again
with `go build -pgo` (`bin/g2c build --pgo FILE` for other programs): gc devirtualizes and
inlines the hot interface calls, 5-15% on the executable. Opt-in for ordinary builds; the
freeze's executables are built with it.

### 13.5 Floating point

gc fuses `x*y + z` into a fused multiply-add on arm64 (`FMADDD`, `FMADDS`) and on amd64 with
`GOAMD64=v3` (`VFMADD231SD`), and not with `GOAMD64=v1`; `float64(x*y) + z` is never fused
(checked with `go build -gcflags=-S`, experiment `fma`). Java forbids fusion, so c2g converts
every floating-point product explicitly (§7.4). The conversion costs nothing where no fusion
would happen.

### 13.6 Code size and compile time

A synthetic package shaped like c2g's output (`big`: 1,200 classes in random single-inheritance
hierarchies, 30 methods each, implementation methods, forwarders for every inherited method,
class interfaces with markers, 117,181 functions, 451,498 lines of Go) compiles as one package in
34 s with 6 GB of peak memory, and links to a 60 MB executable when reflection keeps every
method (4.5 MB when only reachable code is kept). The real closure is smaller (422 JDK classes
and 774 runtime classes, most with far fewer methods, most of them leaves): jrt as one package is
within gc's means. Member tables keep every method they list reachable, which is why §5.11 lists
public members only.

### 13.7 Choices and alternatives

| choice | cost | alternative considered |
|---|---|---|
| Go interfaces for dispatch, forwarders | code size (forwarders), 2 words per reference | explicit vtables over thin pointers (§13.2, Q1) |
| leaf classes as pointers | a closed-world analysis, recomputed as a whole | interfaces for every class: an interface call per field access of another object (§13.1) |
| `any` for `Object` | an assertion for `Object`'s methods on `Object`-typed values | `jrt.Object_I`: direct `hashCode`/`equals`, but 2.2 ns instead of 1.1 for every up-cast |
| lazy class initialization with guards | about 0.07-0.3 ns per guarded access | Go's eager package initialization: cannot express `RT`'s cycles (§6.2) |
| exceptions as panics | 9 ns per entered `try` | Go-style error results on every call: the normal path pays everywhere |
| monitors in the header word | 8 bytes per object (shared with the identity hash) | a `sync.Mutex` per object (8 bytes more); an external table (a map operation per lock) |

## 14. Coverage

These tables check the spec against the class forms spec and the analyzer's output.

### 14.1 Declarations (classes/SPEC.md §4)

| class forms | c2g |
|---|---|
| `defclass` (class, `^:interface`, `^:enum`, `^:record`, `^:annotation`) | §4.5, §5.2-§5.5, §7.13 |
| record components | fields, accessors, canonical constructor from the analyzer; §7.13 |
| `:extends`, `:implements`, `:permits`, `:type-params`, `:package` | struct embedding and class interface; markers; nothing (sealing is checked by the analyzer); erased; the Go package by §4.2 |
| modifiers `public` `protected` `private` (package access) | checked by the analyzer; `Modifiers` in the member tables; private methods non-virtual (§5.4); package-private pairs across packages (§4.4) |
| `static` | package members (§4.4, §6.1) |
| `final` | classes: leaves (§5.3); fields: nothing beyond §8.3; methods: dispatch as others; locals, parameters: nothing |
| `abstract` | no Go method, no allocation function (§5.2, §5.4) |
| `synchronized` | §8.1 |
| `native` | jrt natives (§9.1) |
| `transient` | nothing (serialization cut) |
| `volatile` | atomic field types (§8.2) |
| `default`, `sealed`, `non-sealed`, `strictfp`, `deprecated`, `synthetic`, `bridge` | default methods (§5.5); nothing; nothing; nothing (Java has no other floating point mode, nor has this spec, §7.4); `Modifiers`; as members; bridges (§5.4) |
| annotations | dropped (§12) |
| `field`, initializer | §5.2, §6.1, §7.13 |
| `method`, overloads, untyped methods, `:throws` | §4.4 names from the analyzer's descriptor; checked exceptions are not analyzed (as javac's don't run) |
| `constructor`, `(super. ...)`, `(this. ...)`, `(.super o ...)` | §5.2, §7.12 |
| `initializer`, `static-initializer` | folded by the analyzer (§5.2, §6.2) |
| member classes, `anon`, `letclass` | §7.12 |
| `constants` | §7.13 |
| `defmodule`, `defpackage` | nothing (§4.5) |
| member macros | expanded by the analyzer |

### 14.2 Code (classes/SPEC.md §5)

Every row of classes/SPEC.md §5.1's table is in §7.1. Further:

| class forms | c2g |
|---|---|
| names in class bodies (§5.2) | resolved by the analyzer; Go names §4.4 |
| `^:mutable`, `^:const`, exact primitive types, reference tags (§5.3) | §7.3; tags that were checkcasts are `C_Cast` |
| literals, constant expressions (§5.4) | §7.4, §6.1 |
| conversions, erasure casts, conditions, branch types (§5.5) | §7.4, §5.7, §5.10; branches typed by the analyzer |
| qualifying types, accessibility, overloads, constants, array `clone`, null checks (§5.6) | the analyzer's; §7.6, §5.9, §5.6 |
| control flow, `for-each`, forms that do not complete (§5.7) | §7.7 |
| `switch`, patterns (§5.8) | §7.8 |
| exceptions, `with-resources`, `locking`, `java-assert` (§5.9) | §7.9, §8.1, §7.13 |
| `java-str` (§5.10) | §7.5 |
| arrays (§5.11) | §5.9 |
| `lambda`, `method-ref` (§5.12) | §7.11 |
| Clojure inside class bodies (§5.13) | §7.14 |

### 14.3 The analyzer's nodes

The node kinds `arbace.classes.analyze` produces (the `:op`s of `emit` and `emit-extra`):

| `:op` | Go forms |
|---|---|
| `:const` | a literal, a pooled string, a jrt float variable (§7.4, §7.5) |
| `:local`, `:set-local` | the variable; `set!` |
| `:this-path`, `:outer-param-path` | `t`/`this`; a chain of `F_this_0` reads (§7.12) |
| `:class-lit` | `C_class`, jrt's primitive and array classes |
| `:get-field`, `:set-field` | field access, atomic for volatile (§7.6, §8.2) |
| `:get-static`, `:set-static` | `C_f`, guarded (§6.2) |
| `:invoke` (`:static`, `:virtual`, `:interface`, `:special`) | a package function call; a method call; a `super`/private call on the struct (§5.4) |
| `:new`, `:ctor-call` | `C_New_...`; the constructor body's superclass or `this.` call (§5.2) |
| `:new-array`, `:array-init`, `:aget`, `:aset`, `:alength` | §5.9 |
| `:arith`, `:compare`, `:convert` | §7.4 |
| `:cast`, `:instance?`, `:null-checked` | §5.7; `Objects.requireNonNull` as called |
| `:not`, `:and`, `:or`, `:nil?`, `:identical?`, `:bool=`, `:truth` | `not`, `and`, `or`, `(== x nil)`, §5.8's identity, `==`, nil and `Boolean` tests |
| `:if`, `:do`, `:let`, `:loop`, `:recur`, `:label`, `:break`, `:return` | §7.2, §7.3, §7.7 |
| `:try`, `:throw`, `:monitor` | §7.9, §8.1 |
| `:switch`, `:if-instance` | §7.8 |
| `:for-each` | §7.7 |
| `:java-str` | §7.5 |
| `:assert` | §7.13 |
| `:lambda`, `:method-ref`, `:fi-adapter` | §7.11 (`:fi-adapter`, Clojure's conversion of a `fn` to a functional interface, is `F_Fn` around the `FnInvokers` invoker the analyzer chose, amendment E5) |
| `:var-deref`, `:var-invoke` | calls of `Var`'s methods on the class's synthetic var fields (§7.14) |
| `:deser-indy`, `:param0` | serialization: out of scope (§12) |
| `:none` | nothing (a form that does not complete) |

### 14.4 Class file attributes and flags

None of classes/SPEC.md §8.2-§8.3's attributes and flags exist in Go. What they meant is kept
as: access flags in `Modifiers` (§5.11); `ConstantValue` as Go constants (§6.1); `Code` as the
translation of §7; `Signature`, annotations, `MethodParameters`, `Deprecated` dropped (generic
reflection, annotations and parameter names are cut, §12); `InnerClasses`, `EnclosingMethod`,
`NestHost` in `Declaring` and the modifiers of nested classes; `PermittedSubclasses`, `Record`
in `Kind` and the record's members; `BootstrapMethods` unnecessary (§12); `Exceptions` dropped
(`getExceptionTypes` is not used by the closure).

## 15. Worked examples

Written by hand from the current `arbace/lang` sources, as c2g should produce them. Only the
interesting members are shown; every class also has its `Ref`, `GetClass__Class`,
`ToString__String` and markers (§2), its member table (§5.11), and its file's `in-ns` and
`go/file` forms. Forms in `arbace/lang` qualify jrt's names with `jrt/`.

**Checked.** A prototype (`.tmp/c2g-proto/`, scratch) holds §15.1's `Murmur3` forms and §15.4's
`Delay.realize` and `deref` as written here (and `LazySeq.sval`'s control code, on a `Delay`),
the `Reduced` of §2, `Util`'s class initialization, a three-class
hierarchy with implementations, forwarders and a `super` call (§5.4), and a minimal hand-written
jrt (header, `String` over UTF-16, `Throwable`, `Catch` with the run-time error mapping of
§7.9.5, `ClassInit`, `Volatile`, `RefArray`). `bin/g2c build` prints and builds it for
linux/amd64 and linux/arm64; both executables print the same, and `hashInt`, `hashLong` and
`hashCombine` give the JVM Arbace's values (`(Murmur3/hashInt 42)` is -1134849565,
`(Murmur3/hashLong 1234567890123)` 1740798302, `(Util/hashCombine 17 99)` -1640530319). The
inherited `describe` of the hierarchy's leaf calls the leaf's `area` through `this`; `Util`'s
initializer runs once; a `nil` receiver, an index out of bounds and a division by zero arrive in
Java's `catch` as `NullPointerException`, `ArrayIndexOutOfBoundsException` and
`ArithmeticException("/ by zero")`; the `return` from inside `try`/`finally` runs the `finally`
code first.

### 15.1 `Murmur3`: statics, constants, arithmetic

```clojure
(defclass ^:public ^:final Murmur3
  (field ^:private ^:static ^:final ^int seed 0)
  (field ^:private ^:static ^:final ^int C1 (unchecked-int 0xcc9e2d51))
  (field ^:private ^:static ^:final ^int C2 0x1b873593)
  (method ^:public ^:static hashInt ^int [^int input]
    (if (== input 0)
        0
        (let [k1 (Murmur3/mixK1 input) h1 (Murmur3/mixH1 seed k1)] (Murmur3/fmix h1 4))))
  (method ^:public ^:static hashLong ^int [^long input]
    (if (== input 0)
        0
        (let [low (unchecked-int input)
              high (unchecked-int (unsigned-bit-shift-right input 32))
              ^:mutable k1 (Murmur3/mixK1 low)
              ^:mutable h1 (Murmur3/mixH1 seed k1)]
          (set! k1 (Murmur3/mixK1 high))
          (set! h1 (Murmur3/mixH1 h1 k1))
          (Murmur3/fmix h1 8))))
  (method ^:public ^:static hashOrdered ^int [^Iterable xs]
    (let [^:mutable ^int n 0
          ^:mutable ^int hash 1]
      (for-each [x xs]
        (set! hash (unchecked-add-int (unchecked-multiply-int 31 hash) (Util/hasheq x)))
        (set! n (unchecked-inc-int n)))
      (Murmur3/mixCollHash hash n)))
  (method ^:private ^:static mixK1 ^int [^:mutable ^int k1]
    (set! k1 (unchecked-multiply-int k1 C1))
    (set! k1 (Integer/rotateLeft k1 15))
    (set! k1 (unchecked-multiply-int k1 C2))
    k1)
  (method ^:private ^:static fmix ^int [^:mutable ^int h1 ^int length]
    (set! h1 (bit-xor-int h1 length))
    (set! h1 (bit-xor-int h1 (unsigned-bit-shift-right-int h1 16)))
    (set! h1 (unchecked-multiply-int h1 (unchecked-int 0x85ebca6b)))
    (set! h1 (bit-xor-int h1 (unsigned-bit-shift-right-int h1 13)))
    (set! h1 (unchecked-multiply-int h1 (unchecked-int 0xc2b2ae35)))
    (set! h1 (bit-xor-int h1 (unsigned-bit-shift-right-int h1 16)))
    h1)
  ;; mixH1, mixCollHash, hashUnordered, hashUnencodedChars alike
  )
```

```clojure
(go/type Murmur3 (struct jrt/Object))

(go/const ^{:tag int32 :val 0} Murmur3_seed 0)
(go/const ^{:tag int32 :val -862048943} Murmur3_C1 -862048943)
(go/const ^{:tag int32 :val 461845907} Murmur3_C2 461845907)

(go/func Murmur3_HashInt_I__I ^int32 [^int32 input]
  (if (== input 0)
    (return 0)
    (let [k1 (Murmur3_MixK1_I__I input)
          h1 (Murmur3_MixH1_I_I__I 0 k1)]            ; seed: a constant, inlined
      (return (Murmur3_Fmix_I_I__I h1 4)))))

(go/func Murmur3_HashLong_J__I ^int32 [^int64 input]
  (if (== input 0)
    (return 0)
    (let [low (conv int32 input)
          high (conv int32 (conv int64 (>> (conv uint64 input) 32)))
          k1 (Murmur3_MixK1_I__I low)
          h1 (Murmur3_MixH1_I_I__I 0 k1)]
      (set! k1 (Murmur3_MixK1_I__I high))
      (set! h1 (Murmur3_MixH1_I_I__I h1 k1))
      (return (Murmur3_Fmix_I_I__I h1 8)))))

(go/func Murmur3_HashOrdered_Iterable__I ^int32 [^jrt/Iterable xs]
  (let [^int32 n 0
        ^int32 hash 1]
    (do
      (let [it (.Iterator__Iterator xs)]
        (while (.HasNext__Z it)
          (let [x (.Next__O it)]
            (set! hash (+ (* 31 hash) (Util_Hasheq_O__I x)))
            (set! n (+ n 1))))))
    (Murmur3_MixCollHash_I_I__I hash n)))

(go/func Murmur3_MixK1_I__I ^int32 [^int32 k1]
  (set! k1 (* k1 -862048943))
  (set! k1 (jrt/Integer_RotateLeft_I_I__I k1 15))
  (set! k1 (* k1 461845907))
  k1)

(go/func Murmur3_Fmix_I_I__I ^int32 [^int32 h1 ^int32 length]
  (set! h1 (bit-xor h1 length))
  (set! h1 (bit-xor h1 (conv int32 (>> (conv uint32 h1) 16))))
  (set! h1 (* h1 -2048144789))
  (set! h1 (bit-xor h1 (conv int32 (>> (conv uint32 h1) 13))))
  (set! h1 (* h1 -1028477387))
  (set! h1 (bit-xor h1 (conv int32 (>> (conv uint32 h1) 16))))
  h1)
```

- `Murmur3` has only constants: its initialization is trivial (§6.2), so there is no
  `Murmur3_Init` and no guard. `Util` and `Integer` have non-trivial initialization: their
  static methods begin with their guards, and callers do nothing.
- The constants are `go/const`s; their uses are the analyzer's inlined values (`0` for `seed`,
  the folded `-862048943` for `(unchecked-int 0xcc9e2d51)`), never a Go expression gc would fold
  and reject.
- `int` multiplication wraps in Go as in Java; `>>>` is a shift of the `uint32` (`uint64`)
  conversion; constant shift counts are already masked.
- The `for-each` over an `Iterable` is the iterator loop in a block of its own (`do`), so that
  `it` does not collide with a later declaration; `x` is declared inside the loop body. Reads of
  the locals `hash` and `n` need no temporaries around the call `Util_Hasheq_O__I` (a callee
  cannot change a Java local, §7.2).
- A `NullPointerException` for a `nil` `xs` comes from Go's call on a nil interface.

### 15.2 `Util`: an interface, anonymous classes, class initialization

```clojure
(defclass ^:public Util
  (method ^:public ^:static equiv ^boolean [k1 k2]
    (cond
      (identical? k1 k2) true
      (some? k1)
        (cond
          (and (instance? Number k1) (instance? Number k2))
            (Numbers/equal (cast Number k1) (cast Number k2))
          (or (instance? IPersistentCollection k1) (instance? IPersistentCollection k2))
            (Util/pcequiv k1 k2)
          :else (.equals k1 k2))
      :else false))

  (defclass ^:public ^:interface EquivPred
    (method equiv ^boolean [this k1 k2]))

  (field ^:static ^EquivPred equivNull
    (anon EquivPred []
      (method ^:public equiv ^boolean [this k1 k2] (nil? k2))))
  ;; equivEquals, equivNumber, equivColl alike

  (method ^:public ^:static hashCombine ^int [^:mutable ^int seed ^int hash]
    (set! seed
          (bit-xor-int seed
                       (unchecked-add-int
                         (unchecked-add-int (unchecked-add-int hash (unchecked-int 0x9e3779b9))
                                            (bit-shift-left-int seed 6))
                         (bit-shift-right-int seed 2))))
    seed))
```

```clojure
(go/type Util (struct jrt/Object))

(go/type Util_EquivPred
  (interface jrt/Object_I
    (Is_Util_EquivPred [])
    (Equiv_O_O__Z ^bool [^any k1 ^any k2])))

(go/var ^Util_EquivPred Util_equivNull)
;; Util_equivEquals, Util_equivNumber, Util_equivColl alike

(go/var ^jrt/ClassInit Util_init)

(go/func Util_Init []
  (when (not (.Done Util_init))
    (.Run Util_init Util_clinit)))

(go/func Util_clinit []
  (set! Util_equivNull (Util_1_New))
  (set! Util_equivEquals (Util_2_New))
  (set! Util_equivNumber (Util_3_New))
  (set! Util_equivColl (Util_4_New)))

(go/func Util_Equiv_O_O__Z ^bool [^any k1 ^any k2]
  (Util_Init)
  (cond
    (== k1 k2) (return true)
    (!= k1 nil) (cond
                  (and (jrt/Number_InstanceOf k1) (jrt/Number_InstanceOf k2))
                  (return (Numbers_Equal_Number_Number__Z (jrt/Number_Cast k1) (jrt/Number_Cast k2)))
                  (or (IPersistentCollection_InstanceOf k1) (IPersistentCollection_InstanceOf k2))
                  (return (Util_Pcequiv_O_O__Z k1 k2))
                  :else (return (jrt/Equals k1 k2)))
    :else (return false)))

(go/func Util_HashCombine_I_I__I ^int32 [^int32 seed ^int32 hash]
  (Util_Init)
  (set! seed (bit-xor seed (+ hash -1640531527 (<< seed 6) (>> seed 2))))
  seed)

;; Util$1, the anonymous EquivPred of equivNull: a leaf class without outer instance
(go/type Util_1 (struct jrt/Object))

(go/func Util_1_New ^{:tag (* Util_1)} [] (addr (lit Util_1)))

(go/method Equiv_O_O__Z ^bool [^{:tag (* Util_1)} t ^any k1 ^any k2] (== k2 nil))

(go/method Is_Util_EquivPred [^{:tag (* Util_1)} t])
```

- `Util`'s static field initializers make its initialization non-trivial: `Util_clinit` runs
  them in textual order, `Util_Init` guards every static method of `Util`. Inside them, `Util`'s
  own statics need no guard.
- `identical?` of two `any` values is Go's `==`; both are interface values holding pointers.
- `Number` has subclasses, so it is an interface type (`jrt.Number_I`); `instance?` and `cast`
  are its generated functions. `(.equals k1 k2)` on an `Object`-typed receiver is jrt's
  `Equals`, which asserts `jrt.Object_I` (§5.8).
- The `cond` arms in return context return; Go's `else if` chain is the `cond`.
- The anonymous classes are created in a static context: no `F_this_0`. Their constructor
  (`Object`'s) is empty.
- `hashCombine`'s left-nested additions are one n-ary Go form, Java's association kept.

### 15.3 `PersistentVector`: a leaf class, a nested class, an inner anonymous class

```clojure
(defclass ^:public PersistentVector
  :extends APersistentVector
  :implements [IObj IEditableCollection IReduce IKVReduce IDrop]
  (defclass ^:public ^:static Node
    :implements [Serializable]
    (field ^:public ^:final ^:transient ^{:tag (AtomicReference Thread)} edit)
    (field ^:public ^:final ^Object/1 array)
    (constructor ^:public [this ^{:tag (AtomicReference Thread)} edit ^Object/1 array]
      (set! (.-edit this) edit)
      (set! (.-array this) array)))
  (field ^:static ^:final ^{:tag (AtomicReference Thread)} NOEDIT (AtomicReference. nil))
  (field ^:public ^:static ^:final ^Node EMPTY_NODE (Node. NOEDIT (new Object/1 32)))
  (field ^:final ^int cnt)
  (field ^:public ^:final ^int shift)
  (field ^:public ^:final ^Node root)
  (field ^:public ^:final ^Object/1 tail)
  (field ^:final ^IPersistentMap _meta)
  (field ^:public ^:static ^:final ^PersistentVector EMPTY
    (PersistentVector. 0 5 EMPTY_NODE (new Object/1 [])))
  (field ^:private ^:static ^:final ^IFn TRANSIENT_VECTOR_CONJ
    (anon AFn [] ...))                                    ; PersistentVector$1
  (constructor [this ^int cnt ^int shift ^Node root ^Object/1 tail]
    (set! (.-_meta this) nil)
    (set! (.-cnt this) cnt)
    (set! (.-shift this) shift)
    (set! (.-root this) root)
    (set! (.-tail this) tail))
  (method ^:final tailoff ^int [this]
    (if (< cnt 32)
        0
        (bit-shift-left-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 1) 5) 5)))
  (method ^:public arrayFor ^Object/1 [this ^int i]
    (if (and (>= i 0) (< i cnt))
        (if (>= i (.tailoff this))
            tail
            (let [^:mutable node root]
              (loop [^int level shift]
                (when (> level 0)
                  (set! node
                        (cast Node
                              (aget (.-array node)
                                    (bit-and-int (unsigned-bit-shift-right-int i level) 0x01f))))
                  (recur (unchecked-subtract-int level 5))))
              (.-array node)))
        (throw (IndexOutOfBoundsException.))))
  (method rangedIterator ^Iterator [this ^:final ^int start ^:final ^int end]
    (anon Iterator []
      (field ^int i start)
      (field ^int base (unchecked-subtract-int i (unchecked-remainder-int i 32)))
      (field ^Object/1 array
        (when (< start (.count PersistentVector/this)) (.arrayFor PersistentVector/this i)))
      (method ^:public hasNext ^boolean [this] (< i end))
      (method ^:public next [this]
        (if (< i end)
            (do
              (when (== (unchecked-subtract-int i base) 32)
                (set! array (.arrayFor PersistentVector/this i))
                (set! base (unchecked-add-int base 32)))
              (let [i-1 i] (set! i (unchecked-inc-int i)) (aget array (bit-and-int i-1 0x01f))))
            (throw (NoSuchElementException.))))
      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))
  ;; ...
  )
```

```clojure
(go/type PersistentVector
  (struct APersistentVector
          ^int32 F_cnt
          ^int32 F_shift
          ^{:tag (* PersistentVector_Node)} F_root
          ^{:tag (* jrt/RefArray)} F_tail
          ^IPersistentMap F__meta))

(go/type PersistentVector_Node
  (struct jrt/Object
          ^{:tag (* jrt/AtomicReference)} F_edit
          ^{:tag (* jrt/RefArray)} F_array))

(go/var ^{:tag (* jrt/AtomicReference)} PersistentVector_NOEDIT)
(go/var ^{:tag (* PersistentVector_Node)} PersistentVector_EMPTY_NODE)
(go/var ^{:tag (* PersistentVector)} PersistentVector_EMPTY)
(go/var ^IFn PersistentVector_TRANSIENT_VECTOR_CONJ)
(go/const ^{:tag int64 :val -7896022351281214157} PersistentVector_serialVersionUID -7896022351281214157)
(go/var ^jrt/ClassInit PersistentVector_init)

(go/func PersistentVector_Init []
  (when (not (.Done PersistentVector_init))
    (.Run PersistentVector_init PersistentVector_clinit)))

(go/func PersistentVector_clinit []
  (set! PersistentVector_NOEDIT (jrt/AtomicReference_New_O nil))
  (set! PersistentVector_EMPTY_NODE
        (PersistentVector_Node_New_AtomicReference_O1 PersistentVector_NOEDIT
                                                      (jrt/NewRefArray jrt/Object_class 32)))
  (set! PersistentVector_EMPTY
        (PersistentVector_New_I_I_PersistentVector_Node_O1 0 5 PersistentVector_EMPTY_NODE
                                                          (jrt/RefArrayOf jrt/Object_class)))
  (set! PersistentVector_TRANSIENT_VECTOR_CONJ (PersistentVector_1_New)))

(go/func PersistentVector_New_I_I_PersistentVector_Node_O1 ^{:tag (* PersistentVector)}
  [^int32 cnt ^int32 shift ^{:tag (* PersistentVector_Node)} root ^{:tag (* jrt/RefArray)} tail]
  (PersistentVector_Init)
  (let [t (addr (lit PersistentVector))]
    (.Ctor_I_I_PersistentVector_Node_O1 t cnt shift root tail)
    t))

(go/method Ctor_I_I_PersistentVector_Node_O1
  [^{:tag (* PersistentVector)} t ^int32 cnt ^int32 shift ^{:tag (* PersistentVector_Node)} root
   ^{:tag (* jrt/RefArray)} tail]
  (.Ctor (.-APersistentVector t) t)       ; super(): APersistentVector's body, this = the object
  (set! (.-F__meta t) nil)
  (set! (.-F_cnt t) cnt)
  (set! (.-F_shift t) shift)
  (set! (.-F_root t) root)
  (set! (.-F_tail t) tail))

(go/method Tailoff__I ^int32 [^{:tag (* PersistentVector)} t]
  (if (< (.-F_cnt t) 32)
    (return 0)
    (return (<< (conv int32 (>> (conv uint32 (- (.-F_cnt t) 1)) 5)) 5))))

(go/method ArrayFor_I__O1 ^{:tag (* jrt/RefArray)} [^{:tag (* PersistentVector)} t ^int32 i]
  (if (and (>= i 0) (< i (.-F_cnt t)))
    (if (>= i (.Tailoff__I t))
      (return (.-F_tail t))
      (let [node (.-F_root t)]
        (do
          (let [^int32 level (.-F_shift t)]
            (label :L1
              (while true
                (when (> level 0)
                  (set! node (PersistentVector_Node_Cast
                               (aget (.-A (.-F_array node))
                                     (bit-and (conv int32 (>> (conv uint32 i) (bit-and level 31))) 31))))
                  (set! level (- level 5))
                  (continue :L1))
                (break :L1)))))
        (return (.-F_array node))))
    (panic (jrt/Thrown (jrt/IndexOutOfBoundsException_New)))))

(go/method RangedIterator_I_I__Iterator ^jrt/Iterator [^{:tag (* PersistentVector)} t ^int32 start ^int32 end]
  (PersistentVector_2_New_PersistentVector_I_I t start end))

;; PersistentVector$2, the anonymous Iterator of rangedIterator: an inner class capturing two locals
(go/type PersistentVector_2
  (struct jrt/Object
          ^int32 F_i
          ^int32 F_base
          ^{:tag (* jrt/RefArray)} F_array
          ^{:tag (* PersistentVector)} F_this_0
          ^int32 F_val_start
          ^int32 F_val_end))

(go/func PersistentVector_2_New_PersistentVector_I_I ^{:tag (* PersistentVector_2)}
  [^{:tag (* PersistentVector)} this_0 ^int32 start ^int32 end]
  (let [t (addr (lit PersistentVector_2))]
    (.Ctor_PersistentVector_I_I t this_0 start end)
    t))

(go/method Ctor_PersistentVector_I_I
  [^{:tag (* PersistentVector_2)} t ^{:tag (* PersistentVector)} this_0 ^int32 start ^int32 end]
  (set! (.-F_val_start t) start)                        ; captures and outer instance first,
  (set! (.-F_val_end t) end)                            ; before the superclass constructor
  (jrt/Objects_RequireNonNull_O__O (.Ref this_0))
  (set! (.-F_this_0 t) this_0)
  (set! (.-F_i t) (.-F_val_start t))                    ; then the field initializers
  (set! (.-F_base t) (- (.-F_i t) (% (.-F_i t) 32)))
  (if (< (.-F_val_start t) (.Count__I (.-F_this_0 t)))
    (set! (.-F_array t) (.ArrayFor_I__O1 (.-F_this_0 t) (.-F_i t)))
    (set! (.-F_array t) nil)))

(go/method HasNext__Z ^bool [^{:tag (* PersistentVector_2)} t]
  (< (.-F_i t) (.-F_val_end t)))

(go/method Next__O ^any [^{:tag (* PersistentVector_2)} t]
  (if (< (.-F_i t) (.-F_val_end t))
    (do
      (when (== (- (.-F_i t) (.-F_base t)) 32)
        (set! (.-F_array t) (.ArrayFor_I__O1 (.-F_this_0 t) (.-F_i t)))
        (set! (.-F_base t) (+ (.-F_base t) 32)))
      (let [i_1 (.-F_i t)]
        (set! (.-F_i t) (+ (.-F_i t) 1))
        (return (aget (.-A (.-F_array t)) (bit-and i_1 31)))))
    (panic (jrt/Thrown (jrt/NoSuchElementException_New)))))

(go/method Remove__V [^{:tag (* PersistentVector_2)} t]
  (panic (jrt/Thrown (jrt/UnsupportedOperationException_New))))

(go/method ForEachRemaining_Consumer__V [^{:tag (* PersistentVector_2)} t ^jrt/Consumer action]
  (jrt/Iterator_ForEachRemaining_Consumer__V t action))   ; Iterator's default method

(go/method Is_Iterator [^{:tag (* PersistentVector_2)} t])
```

- `PersistentVector` is not final, but nothing extends it: a leaf, so `*PersistentVector`
  everywhere, `tailoff` a direct (inlined) call, `cnt` a direct field read. `APersistentVector`
  has subclasses (`PersistentVector`, `AMapEntry`, `SubVector`): it is a struct embedded here
  and an interface type elsewhere. `PersistentVector`'s own forwarders for the methods it
  inherits from `APersistentVector` and `AFn` (about forty) are not shown.
- `Node` is a static nested class and a leaf; `AtomicReference` (a jrt shim) too.
- The class initialization runs the four static initializers in order. `EMPTY`'s allocation
  calls `PersistentVector_Init` again from inside the initialization: the same thread, so it
  returns at once and the half-initialized class is used, as on the JVM.
- `arrayFor`'s `loop` is a labeled `while true`, `recur` an assignment and `continue`, its
  bindings in a block of their own. The checked `cast` to `Node` is `PersistentVector_Node_Cast`,
  the read of `Object[]` an element of `[]any`. `(throw ...)` is a `panic`, which is also what
  Go requires to end a function with results.
- The anonymous `Iterator` stores its captures (`val$start`, `val$end`) and the outer instance
  (`this$0`, null-checked as the analyzer emits it) before the superclass constructor, then runs
  its field initializers; `PersistentVector/this` is `F_this_0`. Its `i-1` local is munged to
  `i_1`. It inherits `forEachRemaining` from `Iterator` as a default method, hence the forwarder.
- In the constructor, `(< (.-F_val_start t) (.Count__I ...))` needs no temporary although a call
  follows the field read: `val$start` is final and already assigned (§7.2).

### 15.4 `Delay` and `LazySeq`: exceptions and `finally`

```clojure
(defclass ^:public Delay
  :implements [IDeref IPending]
  (field val)
  (field ^Throwable exception)
  (field ^IFn fn)
  (field ^:volatile ^Lock lock)
  (method ^:private realize ^void [this]
    (let [l lock]
      (when (some? l)
        (.lock l)
        (try
          (when (some? fn)
            (try (set! val (.invoke fn)) (catch Throwable t (set! exception t)))
            (set! fn nil)
            (set! lock nil))
          (finally (.unlock l))))))
  (method ^:public deref [this]
    (when (some? lock) (.realize this))
    (when (some? exception) (throw (Util/sneakyThrow exception)))
    val))
```

```clojure
(go/type Delay
  (struct jrt/Object
          ^any F_val
          ^jrt/Throwable_I F_exception
          ^IFn F_fn
          ^{:tag (jrt/Volatile jrt/Lock)} F_lock))

(go/method Realize__V [^{:tag (* Delay)} t]
  (let [l (.Load (.-F_lock t))]
    (when (!= l nil)
      (.Lock__V l)
      (let [exc ((fn [] :results [^jrt/Throwable_I exc]
                   (defer (jrt/Catch (addr exc)))
                   (when (!= (.-F_fn t) nil)
                     (let [exc2 ((fn [] :results [^jrt/Throwable_I exc]
                                   (defer (jrt/Catch (addr exc)))
                                   (set! (.-F_val t) (.Invoke__O (.-F_fn t)))
                                   (return)))]
                       (when (!= exc2 nil)                ; catch Throwable: every exception
                         (set! (.-F_exception t) exc2))
                       (set! (.-F_fn t) nil)
                       (.Store (.-F_lock t) nil)))
                   (return)))]
        (.Unlock__V l)                                    ; the finally code
        (when (!= exc nil)
          (panic exc))))))

(go/method Deref__O ^any [^{:tag (* Delay)} t]
  (when (!= (.Load (.-F_lock t)) nil)
    (.Realize__V t))
  (when (!= (.-F_exception t) nil)
    (panic (jrt/Thrown (Util_SneakyThrow_Throwable__RuntimeException (.-F_exception t)))))
  (.-F_val t))
```

`LazySeq.sval` returns from inside a `try` with a `finally`, which needs a control code:

```clojure
(method ^:private ^:final sval [this]
  (let [l lock]
    (when (some? l)
      (.lock l)
      (try (when (some? lock) (.force this) (return sv)) (finally (.unlock l))))
    s))
```

```clojure
(go/method Sval__O ^any [^{:tag (* LazySeq)} t]
  (let [l (.Load (.-F_lock t))]
    (when (!= l nil)
      (.Lock__V l)
      (let [(values ctl rv exc) ((fn [] :results [^int32 ctl ^any rv ^jrt/Throwable_I exc]
                                   (defer (jrt/Catch (addr exc)))
                                   (when (!= (.Load (.-F_lock t)) nil)
                                     (.Force__V t)
                                     (return 1 (.-F_sv t) nil))   ; ctl 1: the method returns rv
                                   (return 0 nil nil)))]
        (.Unlock__V l)
        (when (!= exc nil)
          (panic exc))
        (when (== ctl 1)
          (return rv))))
    (.-F_s t)))
```

- The `volatile` `Lock` field is an interface type, so a `jrt.Volatile`: atomic loads, and a
  store of `nil` that allocates nothing (a nil box).
- Each `try` body is a function literal called in place, with `jrt.Catch` deferred; the
  handlers and the `finally` code run after it, in the method. The inner `catch Throwable`
  catches everything, so it needs no test.
- `finally` runs on every path: after normal completion, after the `return` (control code 1,
  performed only after `unlock`), and before rethrowing an exception with `panic`.
- `(throw (Util/sneakyThrow ...))`: `sneakyThrow` throws itself (its generic `sneakyThrow0`
  rethrows the `Throwable`); the `panic` after it is never reached but keeps Go's termination
  rules and Java's meaning.

### 15.5 A lambda and an enum switch

From `Compiler$QualifiedMethodExpr.methodOverloads`, whose static method has the parameters
`methodName` and `kind` (an enum `MethodKind` with the constants `CTOR`, `INSTANCE`, `STATIC`):

```clojure
(lambda Predicate ^boolean [^Executable m]
  (.equals (.getName m) methodName))
(lambda Predicate ^boolean [^Executable m]
  (switch kind
    STATIC (arbace.lang.Compiler/isStaticMethod m)
    INSTANCE (arbace.lang.Compiler/isInstanceMethod m)
    false))
```

```clojure
(addr (lit jrt/Predicate_Fn
        :Fn (fn ^bool [^any m0]
              (let [m (jrt/Executable_Cast m0)]
                (.Equals_O__Z (jrt/NN (.GetName__String m)) (.Ref methodName))))))

(addr (lit jrt/Predicate_Fn
        :Fn (fn ^bool [^any m0]
              (let [m (jrt/Executable_Cast m0)]
                (switch (.-F_ordinal kind)
                  (case [2] (return (Compiler_IsStaticMethod_Executable__Z m)))
                  (case [1] (return (Compiler_IsInstanceMethod_Executable__Z m)))
                  (default (return false)))))))
```

and, generated once in jrt for `java.util.function.Predicate`:

```clojure
(go/type Predicate_Fn (struct Object ^{:tag (func [any] [bool])} Fn))

(go/method Test_O__Z ^bool [^{:tag (* Predicate_Fn)} t ^any x] ((.-Fn t) x))
(go/method And_Predicate__Predicate ^Predicate [^{:tag (* Predicate_Fn)} t ^Predicate other]
  (Predicate_And_Predicate__Predicate t other))
;; Negate__Predicate, Or_Predicate__Predicate alike; Is_Predicate; GetClass__Class (Predicate$$Lambda) ...
```

- The lambda's instantiated parameter type `Executable` is cast from the erased `Object`, as
  `LambdaMetafactory`'s adaptation does; `Executable` has subclasses (`Method`,
  `Constructor`), hence the interface type `jrt.Executable_I` its `Cast` returns.
- The function literal captures `methodName` and `kind`, parameters that Java requires to be
  effectively final.
- `getName` returns a `String` (a pointer), the receiver of `equals`: `jrt.NN` checks it
  (§5.6; the argument has no side effect, so checking before it is evaluated is the same).
- `MethodKind` is an enum without constant bodies, so final, a leaf: `kind` is a pointer and
  `F_ordinal` a field promoted from `jrt.Enum`; the switch is on the ordinals of the closed
  world (`STATIC` is 2), and a `nil` `kind` is a `NullPointerException` from the field read.

## 16. Open questions

**Settled (2026-10-08):** the user accepted all 23 recommendations; they are part of the spec.

**Amendments (2026-10-08).** Implementing jrt (B1a step 3) raised 25 proposed amendments to
this spec, in [JRT-NOTES.md](JRT-NOTES.md), whose three phases numbered them independently
(phase 1 A1-A10; phase 2b A11-A19; phase 2a A11-A16). This spec renames them by phase:
**J1-J10** (jrt's core, phase 1's A1-A10), **R11-R19** (reflection and the shims, phase 2b's
A11-A19) and **T11-T16** (threads, concurrency and the host, phase 2a's A11-A16), keeping each
number. The user accepted all of them on 2026-10-08, with the deviations V11-V13 (§3.2) and the
reduced scope of §12, and BUILD.md's B6 (`--print-only`, `--tests`, `--module`). They are
folded into the text above, marked "(amendment Xn, accepted 2026-10-08)" where they apply:

- **Phase 1 (J):** J1 `ClassInfo.Go`, function and table fields set in an `init` function:
  §5.11. J2 a non-leaf class's constructor bodies take `this C_I`: §5.2, §5.4. J3 the array
  API (`NewMultiArray` with the array class, `Copy`, `IndexOutOfBounds`, `Arraycopy`,
  `Array_NewInstance_Class_I__O`): §5.9, §11. J4 the frame table and the demangler: §7.9.6. J5
  the API additions: §11. J6 implicit `NullPointerException`s with a null message: §7.9.5,
  §9.1, V11. J7 the header and lock word layout: §8.1. J8 `Object_New`, `ClearHeader`: §5.8.
  J9 the stand-in files: §4.3. J10 `Enum`'s promotable methods and `clone`: §5.4, §7.13, §9.1.
- **Phase 2b (R):** R11 the tables' value convention, boxing in jrt: §5.11, §10.1. R12 the
  member tables' format: §5.11. R13 no `Proxy.newProxyInstance`, `AdaptFn` and `FromFn` on
  every `@FunctionalInterface` interface: §5.11, §7.11, §10.1, §12. R14 the API additions: §11.
  R15 c2g's list of classes with declared members (`java.util.Random`): §5.11, Q13. R16 no
  annotations but `FunctionalInterface`, generic reflection as plain `Class` objects, no
  exception types: §5.10, §12. R17 the default locale `en_US` with root data: §12. R18 three
  charsets: §12, V9. R19 `standin_reflect.clj` among the stand-ins: §4.3.
- **Phase 2a (T):** T11 `RegisterGoType` and `Unsafe`'s offsets: §5.11, §9.2. T12
  `AtomicInteger` and `AtomicLong` embed `Object` until `Number` is translated: §5.3. T13 the
  threads API (`CurrentThread`, `RunMain`, `Go`, `RunnableOf`, `WaitNonDaemon`, the interrupt
  token, `Thread_defaultHandler`): §7.9.6, §8.4, §10.6, §11. T14 the host interface as
  implemented: §9.4, §11. T15 the runtime patch's forms in `overlay/go/runtime/`: §9.3, Q19.
  T16 monitors take a Java object's header by address; about 14 ns uncontended: §8.1, V13.
- **Deviations and scope** (accepted with them): V11 (J6), V12 (`SoftReference` strong, from
  phase 2a's notes), V13 (T16); §12's `Proxy` (R13), annotations and generic reflection (R16),
  locales (R17), charsets (R18).

Where amendments meet each other or the text they amend, this is what holds:

- J10 wins over §9.1's "`Enum`'s final methods" as the promotable set: `Enum.clone` is final
  but forwarded to, not promoted.
- R11 replaces §5.11's original invokers, which boxed and unboxed themselves: boxing and
  widening are jrt's (`Box`, `Unbox`), the invokers take Go representations.
- R13 extends §7.11: adapters are generated for every `@FunctionalInterface` interface, not
  only those a lambda targets, since `AdaptFn` needs their `FromFn`.
- T11 proposed `RegisterGoType` or, alternatively, a `reflect.Type` field of `ClassInfo`; phase
  2b added no such field, so `RegisterGoType`, as implemented, holds (§9.2).
- T13 and T16 supersede phase 1's stopgap (thread numbers from `runtime.Stack`'s goroutine id,
  §8.4) and its cost (8,900 ns per uncontended monitor): the slot gives 3.6 ns per thread
  number, and V13's 14 ns replaces both that and §8.1's and Q8's estimate of about 5 to 10 ns.
  J7's inflation of a thread number beyond 22 bits stays, unreached with T13's recycling.
- T16 wins over phase 1's `MonitorEnter`, which asserted `jrt.Object_I` and rejected non-Java
  values: a monitor requires a Java object, as c2g's output guarantees.
- R18 (three charsets) is a scope cut under V9, which phase 2b's notes cite for it; V9 is
  widened to name it.
- The two lists' shared numbers A11-A16 are kept apart by the prefixes alone: R11-R16 and
  T11-T16 are unrelated amendments.
- J5, R14 and T11, T13, T14 all add to §11's table, which now lists the API as implemented.

**Amendments (2026-10-09).** Implementing c2g (B1a step 4) raised 20 more, in
[C2G-NOTES.md](C2G-NOTES.md), whose sections numbered them independently: the core (phase 1)
A1-A8, phase 2A P2A-1..4, phase 2B B1-B8. This spec renames them by source: **C1-C8** (the
core's A1-A8; renamed so as not to clash with JRT-NOTES's A1-A19 above), **P1-P4** (phase 2A's
P2A-1..4) and **E1-E8** (phase 2B's B1-B8, the evaluator boundary; renamed so as not to clash
with BUILD.md's amendments B5, B6), keeping each number. The user accepted all of them on
2026-10-09, together with [EVAL-PLAN.md](EVAL-PLAN.md)'s Q1-Q7 as recommended, which are cited
as "EVAL-PLAN Qn" to keep them apart from this section's questions. They are folded into the
text above, marked "(amendment Xn, accepted 2026-10-09)" or "(EVAL-PLAN Qn, decided
2026-10-09)":

- **The core (C), from C2G-NOTES "c2g, phase 1":** C1 indexed static initializers in variants:
  §4.6. C2 slices restrict code, not the world, and operation-level `C2g_Missing` stubs: §4.1,
  §4.3, §11. C3 replaced stand-ins with non-trivial initialization initialized eagerly: §4.3,
  §6.3. C4 cut classes and reflected marker interfaces: §4.1, §5.7. C5 `String.format` generated
  by c2g: §11. C6 the rename table's entry `Jdk_ByteArray`: §4.4. C7 records' object methods in
  Go, with c2g's helpers: §7.13, §11. C8 the second hoisting rule: §7.2.
- **Phase 2A (P), from C2G-NOTES "c2g, phase 2A":** P1 variants' `^:native` methods and member
  classes: §4.6, §9.1, §11. P2 reflective roots (reachability does not follow reflection):
  §4.1, §5.11, §10.6. P3 dropped fields and their constant-`null` stores: §4.1. P4 `RT`'s
  stream vars over the host and its class lookup with one loader: §9.4, §10.3.
- **Phase 2B (E), from C2G-NOTES "c2g, phase 2B":** E1 variants of nested classes by binary
  name: §4.6. E2 erased packages (`c2g/erase`): §4.6, §10.1. E3 the evaluator's classes, one
  `EvalFn` over `RestFn`: §10.2. E4 `Dyn` as built (`DynClass`, the nominal check as an
  interface assertion, jrt's `Dynamic`, `DefineDynamic`, `Class.Descriptor`, `Compiler$Dyn`'s
  natives): §5.12, §10.4, §11, V9. E5 `FromFn` written in `arbace/lang`, `:fi-adapter`: §5.11,
  §7.11, §14.3. E6 natives in their class's package: §9.1. E7 the root spec `CLASS$*`: §4.1. E8
  the analyzer's `clj-fi-method` asks the declaration of a class from source: §4.1.
- **EVAL-PLAN's decisions on the boundary (§10):** Q1 primitive fns boxed (`primInterface` nil
  in the Go build): §10.2. Q2 the REPL's world roots every public member of every built-in
  class: §10.6. Q3 one `EvalFn` over `RestFn`: §10.2. Q4 a per-thread depth counter,
  `StackOverflowError` at about 10,000 frames: §10.5. Q5 run-time interfaces by `defineClass` of
  an interface kind: §10.4, §12. Q6 the Clojure fn and line per frame in stack traces: §10.7,
  §7.9.6. Q7 ASM erased by package: §10.1 (as E2).

Where they meet each other or the text they amend, this is what holds:

- C2 refines §4.1's "a reference to any other class is an error": it is reported per method and
  becomes an operation-level stub, failing only if reached at run time; a slice does not remove
  classes from the world.
- C3 is the one exception to §6.3 (no Java code before `main`): the replaced stand-ins with
  non-trivial initialization run their initializers from jrt's `init()`.
- C6 replaces §4.4's "empty for B1a" rename table.
- P1 and E6 differ on where a native of `arbace.lang` lives (P1: jrt's hand-written
  `natives.clj`; E6: the class's own package). Both hold, split by who writes the function:
  c2g-written natives (`Compiler$Dyn`'s) are in the class's package, hand-written ones in jrt,
  called qualified; c2g calls jrt's function when jrt defines one of that name, else the class's
  package's (§9.1).
- P4 and phase 2B's small `RT` and `Reflector` variants overlapped; as merged, P4's variants hold
  and phase 2B adds `makeClassLoader` returning nil (§10.3).
- E2 replaces §10.1's "cut by variants" for ASM: erasure by package, with only the class-loading
  and class-writing paths replaced by name (EVAL-PLAN Q7).
- E3 (EVAL-PLAN Q3) wins over §10.2's example of an `EvalFn`/`EvalRestFn` pair.
- E4 wins over §5.12's nominal check by a compare of Go's type word: it is an assertion to
  `jrt.Dynamic` and `DynImplements`, since jrt's functions cannot name `arbace/lang`'s `Dyn`.
- E5 extends R13: `FromFn` is set by `arbace/lang`'s `c2g_fromfn.go`, not by jrt, for jrt's
  functional interfaces too.
- P2 and EVAL-PLAN Q2 concern reachability (what is translated), not §5.11's member tables
  (what reflection lists): the tables stay "public plus listed" (§16 Q13).
- EVAL-PLAN Q4 fixes §16 Q15's limit at about 10,000 frames. EVAL-PLAN Q5 narrows §12's
  `gen-interface` and V9's "nothing can be defined at run time": interfaces and `Dyn` classes
  are defined through `Compiler$Dyn`, class files never.
- Parts C (the rest of the JDK closure) and D (c2g's quality) of phase 2 may propose more
  amendments; they will be added here under their own prefixes. (They were: K and W, below.)

**Amendments (2026-10-09, phase 2 parts C and D).** Parts C and D of c2g's phase 2 raised 10
more, in [C2G-NOTES.md](C2G-NOTES.md): phase 2C's P2C-1..3 and phase 2D's D1-D7. This spec
renames them **K1-K3** (phase 2C's P2C-1..3) and **W1-W6** (phase 2D's D1-D6), keeping each
number; the letters avoid the prefixes already in use here (C, P, E; EVAL-PLAN's Q and this
section's questions; J, R, T; the deviations V; BUILD.md's B) and, for W, the decisions D1-D6
this spec builds on. The user accepted K1-K3 and W1-W6 on 2026-10-09. D7 (performance: `GOGC`
set at start, arrays of references as one allocation) the user deferred to plan step 7: it is
recorded in §13.4 as planned work, not normative, and has no W number. They are folded into the
text above, marked "(amendment Xn, accepted 2026-10-09)":

- **Phase 2C (K), from C2G-NOTES "c2g, phase 2C":** K1 jrt's own Java under
  `overlay/jdk/java.base`, the JDK's Go-build variants under `overlay/jdk/variants`, and the
  `java-names` table: §4.1, §4.3, §4.6, §9.1, §11. K2 the jrt members c2g writes (`System`'s
  streams and setters, `String.CASE_INSENSITIVE_ORDER`, `String`'s regex methods;
  `printStackTrace()` and uncaught exceptions through `System.err`): §7.9.6, §9.1, §9.4, §11.
  K3 all 64 stand-ins translated in a c2g program, the stand-in files only for jrt's own
  build: §4.3.
- **Phase 2D (W), from C2G-NOTES "Phase 2D":** W1 line positions in the forms and `--line-file`
  builds: §4.3, §7.9.6. W2 pattern switches as a labeled `if` chain, with `MatchException`
  wrapping: §7.8. W3 benign-initialization guard elision: §6.2, §13.4. W4 the name checks,
  across Java packages, as errors: §4.4. W5 the race report and `bin/c2g-race`: §8.3. W6
  `--program`: §10.6.

Where they meet each other or the text they amend, this is what holds:

- K2 supersedes P4's interim streams (`RT$HostWriter`, `RT$HostReader` and their natives, §9.4,
  §9.1, §11): `*out*`, `*err*`, `*in*` are `RT`'s own, over `System`'s translated streams. P4's
  class lookup with one loader (§10.3) stays. §4.6's example of a member class with natives
  keeps `RT$HostWriter` as the form's illustration only.
- K2 makes T14's "`System.in`, `out` and `err` are defined with `InputStream` and
  `PrintStream`" precise: c2g writes them into jrt's package, and jrt's own build has none.
- K2 extends C5: `String.format` is no longer the only jrt member c2g writes; the same rule
  (`c2g_support.go`, only when the classes are translated) covers K2's.
- K1 replaces §4.6's location of the JDK's variants (`go/arbace/jrt/variants/`, never used) with
  `overlay/jdk/variants/`, and widens §4.1's JDK input beyond JAVA-SURFACE.md's measured
  closure (184 files) to `bin/jrt-convert`'s 223.
- K3 replaces J9's "the stand-in files are deleted": they are kept for jrt's own build, and C3's
  replacement removes all of them from a c2g program.
- W4 replaces §4.4's renaming of package-private pairs (`_pp_`) with an error, and compares Java
  packages, not Go packages; the rename table stays one entry (C6).
- W1 keeps J4's frame table as it is: lines come from gc's line tables, not from the table.
- W3 refines §6.2's "at the start of every static method of `C`": not where `C`'s
  initialization is benign and the method reads none of its class's or superclasses' statics.
- W6 replaces §10.6's hand-written `main`: c2g writes the main package, which calls
  `arbace.lang.Main.main`.

**Amendments (2026-10-09, B1a step 5: the evaluator).** Step 5 raised 20 more, in
[EVAL-NOTES.md](EVAL-NOTES.md), whose phases numbered them independently: phase 1 (the core,
loading `arbace.core`) V1-V8, phase 2A (types made at run time) A1-A4, phase 2B (jrt's surface
for the REPL) B1-B8. They are renamed **M1-M8** (phase 1's V1-V8), **X1-X4** (phase 2A's A1-A4)
and **S1-S8** (phase 2B's B1-B8), keeping each number: V collides with this spec's deviations
(§3.2, V1-V13), A with JRT-NOTES's A1-A19, B with BUILD.md's and C2G-NOTES's B; M, S and X were
used by no spec (nor U, Y, Z). The user accepted V1-V8, then A1-A4 and B1-B8, on 2026-10-09,
B7 by keeping the code transcribed from jdk26u under its license (LICENSE.md). Those of this
spec are folded into the text above, marked "(amendment Xn, accepted 2026-10-09)"; the others
went to their own documents:

- **Phase 1 (M), from EVAL-NOTES "Proposed amendments":** M1 namespace variants: §10.3. M2
  `getClass` by a field `c2g$class`, a run-time class per fn: §5.11, §10.2. M3 the REPL's cut
  classes and absent members: §4.1. M4 overloads in the JVM's order: §5.11. M5 embedded sources:
  §10.3 (and BUILD.md, "The program's layout": `.g2c-data`). M6 evaluated frames: §10.7, §11. M7
  the JDK variants for `VarHandle` and `SecureRandom`: §4.6, §11. M8 c2g's fixes from the whole
  world: §4.1, §5.11 (and §5.8's identity, which already compares as `any` when the Go types
  differ).
- **Phase 2A (X), from EVAL-NOTES "Phase 2A":** X1 proxies of a class, `DynSub_C`: §5.12, §5.3,
  V9. X2 `proxy` over `Dyn`: §10.4, §12 (and B1-PLAN.md D6, EVAL-PLAN.md §2.5). X3 a duplicate
  method is `ClassFormatError`: §5.12. X4 the oracle covers `proxy`: ORACLE.md.
- **Phase 2B (S), from EVAL-NOTES "Phase 2B":** S1 the rename table's `Sql_Date` and
  `Stream_Tripwire`: §4.4. S2 `bin/jrt-convert`'s other-module files, `J2C_PATCH_ALL` and
  `known-differences`: JRT-SOURCES.md (and §4.1's count). S3 `Date` and `ForkJoinTask`
  non-leaf, T12 done, `String`'s `Constable` and `ConstantDesc`: §5.3, §5.4, §9.1. S4 jrt
  members naming translated classes, default-method forwarders: §5.4, §11. S5 the fork-join
  pool: §8.4. S6 six charsets: §12, V9. S7 the code transcribed from jdk26u kept, under its
  license: LICENSE.md. S8 the class forms compiler's bridges under a subclass's bounds:
  classes/SPEC.md §6 (COMPILER-NOTES.md, amendment 15).

Where they meet each other or the text they amend, this is what holds:

- X1 narrows §5.3's "nothing can subclass a class at run time": nothing but a proxy of a class
  of `proxy-supers`, whose Go type c2g writes ahead of time; leafness stays a property of the
  closed world.
- X1 and X2 reverse D6 and §12 for `proxy` (the user's decision of 2026-10-09);
  `java.lang.reflect.Proxy` and `gen-class` stay out.
- S5 reverses the cut of `ForkJoinPool` (JAVA-SURFACE.md, "What the cut leaves out", under D6).
- S3 completes T12: `AtomicInteger` and `AtomicLong` embed the translated `Number`.
- S4 extends C5 and K2: the members c2g writes into jrt's package now include table entries
  without a Go function (`Date.toInstant`, `from`), which translated code cannot call.
- S6 widens R18 and V9 from three charsets to six; the stream adapters of K2 keep three.
- M5's `ARBACE_PATH` and the embedded sources refine §10.3's "from the file system through the
  host": a namespace not embedded is found only in `ARBACE_PATH`'s directories.
- M6 settles §10.7's open "how they join the translated frames": jrt's frame mapping (§7.9.6)
  replaces the evaluator's Go frames by the evaluated ones.

**Step 5's follow-up** (EVAL-NOTES.md, "Phase 2B follow-up"; accepted by the user 2026-10-09):
Y1 c2g names a jrt-provided class by jrt's registered Go name: §4.4. Y2 `Dyn` implements jrt's
hand-written interfaces with a cast function, with the nominal check: §5.12. Y3 `Semaphore`
hand-written in jrt: §8.4, JAVA-SURFACE.md decision 4. Y4 the harvest keeps `java_interop`'s
proxy assertions: ORACLE.md. Y2 narrows E4's "hand-written jrt interfaces are not covered".

**Step 6** (EXEC-NOTES.md, the executable; accepted by the user 2026-10-09): U1 the image of
prepared namespaces, replayed by `RT.load`: §10.3. U2 a class's identity hash is its name's:
§5.8. U3 the main package's files and `jrt.ImageHooks`: §10.6. U4 the start's collector:
§13.4's step 7 plan (D7). U5 the executable's smoke test in the essential `bin/gate`, on an
executable cached by a hash of its inputs: B1-PLAN.md, "Checks". U1 refines M5's `RT.load`
(an embedded source is replayed when the image holds it); U2 refines §5.8's identity hash for
`Class` objects.

**Proxies of BufferedWriter** (EVAL-NOTES.md, "Proxies of BufferedWriter"; accepted by the user
2026-10-10): Z1 a translated, non-final class of `proxy-supers` is not a leaf, and the list moves
to `arbace.c2g.model` and gains `java.io.BufferedWriter`: §5.3, §5.12. Z1 refines X1 (leafness
is a property of the closed world and of the proxy types c2g adds to it).

**Files** (JRT-NOTES.md, "Files"; accepted by the user 2026-10-10): FS1 files of
`src/java.base/unix/classes` in the closure, KIND `unix`: JRT-SOURCES.md. FS2 `java.nio.file` as
jrt's own `Path`, `Files` and `HostPath`, and no `RandomAccessFile`: JRT-NOTES.md, "Files",
Decisions. FS3 the optional `HostFS`: §9.4. FS4 the rename table's entries for the URL classes
and `java.net.Proxy`: §4.4. FS5 shutdown hooks at the end of `RunMain`: §8.4. FS6 `File`, `URL`
and the `java.nio.file` subset in the closed world, with their variants and the namespace
variants' changes: §4.1, §10.3. FS7 the code transcribed from jdk26u in `HostPath.java` and
`filesystem.clj`: LICENSE.md.

**Sockets** (JRT-NOTES.md, "Sockets (go-net)"; accepted by the user 2026-10-10): NT1
`RT.doInit` loads `arbace.core.server` and starts the servers of the `arbace.server.*`
properties, as the JVM's: §10.6. NT2 `JAVA_TOOL_OPTIONS`' `-D` options as system properties, and
`System.getProperties()`: §9.4, §11. NT3 the code kept from jdk26u in `HostSocketImpl.java` and
the java.net variants: LICENSE.md. NT4 `InetAddress`'s simpler cache: §4.1. NT5 the musl texts of
errors: §9.4. With them the sockets in the closed world, `jrt.NetHost` and `HostNet`'s natives:
§4.1, §9.1, §9.4; B1-PLAN.md D6.

**The JDK's resource data** (JRT-NOTES.md, "The JDK's resource data"; accepted by the user
2026-10-10): RD1 the program embeds the JDK's resource data, and `Class.getResourceAsStream`:
§10.3. RD2 the REPL's world roots no member of a JDK package its module does not export: §10.6.
RD3 jrt's own `InflaterInputStream` over Go's zlib: §4.1. RD4 generated sources beyond the
measured closure's and the resource data, made as the JDK build makes them and compared with its
output: §4.1, JRT-SOURCES.md. RD5 the rename table's `Sun_Normalizer`: §4.4. With them, fixed
(not amended): §6.2's benign initialization counts a static read in a `switch` arm.

**Class forms at the REPL** (CLASSFORMS-REPL.md; accepted by the user 2026-10-10): CF1 a cut
class's constants readable: §4.1. CF2 the class forms' analysis embedded, with its namespace
variants, loaded without spec checks: §10.3. CF3 class forms at the REPL interpreted: §10.4,
B1-PLAN.md D6. CF4 `DynClass.CF`, the DynSub types of `class-supers`, the natives of
`Compiler$CFGo`: §5.12. CF5 `generics.edn` embedded: §10.3. CF6 the oracle's `defclass` forms:
ORACLE.md. CF3 reverses D6 for `defclass`; CF4 extends X1 (the classes a proxy may extend gain
`class-supers`).

**The suite's last failures** (EVAL-NOTES.md, "The suite's last failures"; accepted by the user
2026-10-10): SL1 locals clearing in the evaluator and a fn class's fields for its closed-over
locals: EVAL-PLAN §2.1, §10.2. SL2 hinted calls through the member table's invoker:
EVAL-PLAN §2.6. SL3 single tests skipped and per-namespace timeouts in the Go build's suite
reference: EVAL-NOTES.md, "Phase 2C" (the runner). SL6 `arbace.java.api.Clojure` in the program:
§10.6. SL7 library cuts: §4.1. SL1 settles phase 2's "locals clearing" (EVAL-NOTES.md, "A split
for phase 2"); SL2 does part of EVAL-PLAN §2.6's "the invoker directly" ahead of step 7; SL7
extends M3. SL4 (the suite's Java fixtures in a test build) and SL5 (D6's namespaces embedded
with their classes cut) were not taken.

**Step 7b** (SPEED-NOTES.md, "The evaluator: closure compilation"; accepted by the user
2026-10-10): EC1 each method of an evaluated fn or deftype compiled at its first call into
`Code` nodes, `long` and `double` locals unboxed in the frame, `recur` by a frame flag:
§10.1, EVAL-PLAN §2, §6. EC2 the direct static calls of `Numbers`, `RT`, `Util` and `Math`
generated into `arbace/lang/go/CompilerOps.clj`: §10.1. EC3 calls of fixed arity without a
seq: EVAL-PLAN §2.2. EC4 frames reused per thread by depth: EVAL-PLAN §2.1. EC5 resolved
members through their invoker (`Evaluator.invokeResolved`, `Compiler_CodeRun_Construct`) and
the `Compiler_Code*` frames in jrt's stack-trace mapping: §10.1, §10.7. EC6 the image stores
the analyzed trees, methods compiled lazily: EXEC-NOTES.md, "The image of prepared
namespaces". EC7 (un-skipping `transducers`' `seq-and-transducer`) not taken. EC1 refines §10.1's
"the evaluator may call one with Go values directly" and EVAL-PLAN Q3's per-arity `invoke`.

**java.util completed** (JAVA-BASE.md, "java.util completed"; accepted by the user 2026-10-10):
JB1 the java.util files of the closure, `util-sources`: JRT-SOURCES.md. JB2 the variants of
`TreeMap`, `BitSet` and `RandomGenerator`, and a replacing member's tags as j2c writes them:
§4.6. JB3 FromFn's array results: §7.11. JB4 a forked task not yet done runs again: §8.4.
JB5 lambdas with marker interfaces get adapters implementing them: §7.11. JB4 refines S5's "a
task runs once".

**Step 7a** (SPEED-NOTES.md, "Step 7a"; decided by the user 2026-10-10): O1 the collector's
settings, closing D7: §13.4. O2 reference arrays and strings in one allocation: §5.9, §7.5. O4
the dynamic flag: §5.7. O5 profile-guided builds, opt-in, the freeze's executables built with
them: §13.4. O6 a `try` with nothing to catch has no literal: §7.9.2. O7 the profiles: §13.4.
O3 (one shared array from `getParameterTypes`, a deviation) was not taken: jrt keeps Java's
contract, a fresh copy per call. O4 refines §5.8's header (the identity hash is 31 bits beside
the flag).

Each with a recommendation, which the text above follows, for the user's review.

1. **References.** Go interface values for every non-leaf static type (two words; Go's dispatch,
   type tests and itab caches; no `unsafe`), or one-word pointers to a header with a class
   pointer, explicit vtables and interface tables, and `unsafe` casts (half the memory per
   reference, every reference CAS-able and tear-free, but `unsafe` in all translated code and a
   dispatch of jrt's own)? Measured equal in speed (§13.2). *Recommendation: interface values;
   revisit if memory per reference turns out to matter (vector nodes, maps).*
2. **The Go type of `Object`.** `any` (free up-casts, an assertion for `Object`'s methods), or
   `jrt.Object_I` (direct `hashCode`/`equals`, 2.2 ns instead of 1.1 per up-cast)?
   *Recommendation: `any`.*
3. **Leaf classes as pointers.** Use the closed world's class hierarchy to type every class
   without subclasses as a pointer (direct calls, field reads, inlining), or only declared-final
   classes (stable under additions, but `PersistentVector`, its `Node`, `Delay` and most of
   `arbace.lang` would be interfaces)? *Recommendation: the closed world's leaves; the
   analysis is recomputed by every c2g run.*
4. **Go packages.** One Go package per component of Java packages, grouped as `arbace/jrt` (the
   JDK) and `arbace/lang` (Arbace), or everything in one package (no package boundary, one
   larger compilation), or finer (impossible: `java.lang` and `java.util` form one cycle)?
   *Recommendation: the two packages of §4.2.*
5. **Method names.** Always mangled with the erased descriptor (`Equiv_O_O__Z`: stable, Java's
   dispatch for free, bridges distinct), or bare names where a name has one descriptor in the
   closed world (more readable, but adding an overload anywhere renames methods everywhere,
   hand-written jrt included)? *Recommendation: always mangled.*
6. **Fields as `F_name`.** A prefix keeps Java's spelling and makes fields unable to collide
   with methods, embedded structs and generated members; capitalized names (`Cnt`) read better
   but collide (a field `init`, a field named like a superclass). *Recommendation: `F_`.*
7. **Class initialization.** Lazy per class with JVMS 5.5's protocol and guards (exact, as
   `RT`'s cycles need; 0.07-0.3 ns per guarded access), or eager at start in an order computed
   once (cheaper, but the JVM's order is not a static property and the cycles of `RT`,
   `Compiler` and `Var` need the half-initialized states)? *Recommendation: lazy, with the
   elisions of §6.2 (callee-side guards, none in instance code, none for trivial classes).*
8. **Monitors.** In the header word, a thin lock inflating to a jrt monitor (8 bytes per object,
   shared with the identity hash; 5 ns uncontended), a `sync.Mutex` per object (16 bytes), or
   an external table (no memory per object, a map operation per lock: too slow for the locks of
   `LazySeq` and `Delay`)? *Recommendation: the header word.* (Measured with the slot: about
   14 ns uncontended, §8.1, amendment T16, V13.)
9. **Exceptions.** Panics carrying the Java exception, `try` bodies as function literals with
   control codes (9 ns per entered `try`, 300-800 ns per throw), or Go-style error results on
   every call (no `recover`, but every call pays and every signature changes)?
   *Recommendation: panics.*
10. **Two-word races.** Accept the risk for non-volatile interface fields (§8.3) with c2g's
    report and the race detector, or make every non-final interface field a `jrt.Volatile`
    (safe, but an allocation per store and atomic loads everywhere)? *Recommendation: accept,
    report and test; make a racing field volatile by a variant when one is found.*
11. **CAS on references** (`Unsafe` for `ConcurrentHashMap`, atomics): striped locks in jrt's
    `Unsafe` (CHM translated as decided), or a hand-written CHM over a locked Go map?
    *Recommendation: striped locks, as decision 4 of JAVA-SURFACE.md implies.*
12. **Dynamic objects.** One generated `Dyn` type implementing every interface, with a nominal
    check added to `instance?` and `cast` (0.3 ns), or per-combination types generated ahead of
    time (impossible for `reify` at the REPL)? *Recommendation: `Dyn`.*
13. **Member tables.** Public members of all classes plus declared members of listed classes
    (what Clojure sees; keeps the executable small), or every member of every class (complete
    `getDeclared*`, much larger)? *Recommendation: public plus listed.* (The list so far:
    `java.util.Random`, §5.11, amendment R15.)
14. **Go-build variants.** Variant files with replaced, cut and added members, read only by
    c2g (one source, explicit differences), or edits in `arbace/lang` that work on both
    platforms (no second file, but JVM code paths for Go's sake), or a fork of the runtime for
    Go? *Recommendation: variant files.*
15. **Stack overflow.** The evaluator counts its depth and throws `StackOverflowError` at a limit
    (Clojure's tests and REPL users rely on it), translated code does not count (cost on every
    call); or count everywhere? *Recommendation: the evaluator only, with a limit set so that
    Go's 1 GB stack is never reached by the evaluator's frames.*
16. **Fused multiply-add.** Convert every floating-point product (exact, as Java), or allow
    fusion (faster on arm64, results differing from the JVM in the last bit)?
    *Recommendation: convert, as Java's semantics and the differential tests require.*
17. **String concatenation.** `jrt.Concat` over per-operand conversions (one allocation per
    converted number), or a builder with typed appends (fewer allocations, more forms)?
    *Recommendation: `Concat` first; a builder if profiles show it.*
18. **Lambdas.** One adapter class per functional interface (V4: a lambda's class differs from
    Java's per-site class), or a class per lambda site (javac's naming, much more code)?
    *Recommendation: per interface.*
19. **Where jrt's forms live, and the runtime patch.** jrt's hand-written Go forms as a forms
    root `go/` at the repository's top (`go/arbace/jrt/*.clj`, the layout of BUILD.md), merged
    with c2g's generated tree for the build; the patched runtime files given to the build by an
    overlay option added to `bin/g2c build` (an amendment of BUILD.md), the patch held as Go
    forms (the runtime's converted files with the three changes of §9.3). Alternatives:
    hand-written jrt under `arbace/` (which holds Arbace's Clojure namespaces, not Go forms);
    a copy of the whole runtime in the tree. *Recommendation: as described.* (The overlay
    option is BUILD.md's `--overlay`, amendment B5; the patch's forms are in
    `overlay/go/runtime/`, §9.3, amendment T15.)
20. **The module path.** `arbace` (short, no network meaning; Go accepts a dot-less module
    path for a main module, checked), or `github.com/arbace/arbace/...`? *Recommendation:
    `arbace`.*
21. **Stack traces.** Map Go frames to Java frames with a generated table and the forms' lines
    (`--line-file` builds), or show Go's frames as they are? *Recommendation: mapped, as
    Clojure's error reporting reads class and method names from them.*
22. **Receivers that are pointers.** Check them for `nil` at each call unless provably non-null
    (exact `NullPointerException`s), or rely on the callee's first field access (fewer checks,
    but a method that touches no field runs on `nil`)? *Recommendation: check; gc removes
    redundant checks.*
23. **Temporaries for Go's order of evaluation.** Hoist an operand into a temporary whenever a
    later one may have a side effect and it reads mutable state (exact, conservative), or only
    where a static analysis shows the side effect can change that state (fewer temporaries,
    harder to get right)? *Recommendation: the conservative rule of §7.2; gc's register
    allocation makes temporaries free.*

## 17. Sources

- Arbace: `doc/classes/SPEC.md` (the source language; its §5.1 table and §6 derivations),
  `doc/go/SPEC.md` (the target forms), `doc/go/B1-PLAN.md`, `doc/go/JAVA-SURFACE.md` and
  `doc/go/java-surface.edn` (the closure: 422 translated classes with no simple-name collision
  under §4.4's rule, checked with the edn's `:closure`; five native methods; the boundary),
  `doc/go/BUILD.md`; `doc/go/JRT-NOTES.md` (jrt's three phases: the amendments J1-J10, R11-R19,
  T11-T16, the deviations V11-V13 and §12's reduced scope, with their own sources);
  `doc/go/C2G-NOTES.md` (c2g's core and phase 2's parts A to D: the amendments C1-C8, P1-P4,
  E1-E8, K1-K3, W1-W6, and D7's measurements) and `doc/go/EVAL-PLAN.md` (the evaluator's design, its questions Q1-Q7); the variants
  `arbace/lang/go/Compiler.clj`, `RT.clj`, `Reflector.clj` and jrt's `dyn.clj`, `natives.clj`;
  jrt's own Java `overlay/jdk/java.base/` and the JDK's variants `overlay/jdk/variants/`,
  `go/arbace/jrt/files.clj`, `arbace/c2g/out.clj` (`string-regex-methods`, `java-names`,
  `support-forms`), `arbace/c2g/checks.clj`, `arbace/c2g/model.clj` (`benign-init?`);
  `doc/go/JRT-NOTES.md`, "Phase 2C" (the closure grown, the 26 stand-ins).
- The class forms compiler, `arbace/classes/`: `analyze.clj` (the node kinds, `invoke-node`'s
  kinds, `analyze-with-resources`' desugaring into `try` with `:normal-finally`,
  `analyze-lambda`'s instantiated types and captures), `emit.clj` (the contexts `:stmt`,
  `:expr`, `:return`, `children`, the `emit-extra` node kinds), `compiler.clj` (the
  enter-analyze sequence c2g reuses).
- Arbace's runtime: `arbace/lang/Reduced.clj`, `Murmur3.clj`, `Util.clj`,
  `PersistentVector.clj`, `Delay.clj`, `LazySeq.clj`, `RT.clj` (the static initializer that
  loads `arbace/core`), `Var.clj` (`dvals`), `Compiler.clj` (`Expr`, `QualifiedMethodExpr`'s
  lambdas and `MethodKind`); `target/stage2`'s `PersistentVector$2.class` (`javap`: the
  captures `val$start`, `val$end`, `this$0`, the constructor descriptor
  `(Larbace/lang/PersistentVector;II)V`).
- Go, go1.27.1 at `/root/tamago-go`: the language specification (`doc/go_spec.html`: "Order of
  evaluation", "Floating-point operators", "Integer overflow", "Conversions between numeric
  types", "Constant expressions", "Handling panics", "Defer statements", "Type assertions",
  "Comparison operators", "Size and alignment guarantees" on zero-size variables); the memory
  model (`doc/go_mem.html`: atomics sequentially consistent, races on multiword values);
  `src/runtime/runtime2.go` (`type g struct`), `src/runtime/proc.go` (`gdestroy`),
  `src/cmd/compile/doc.go` and the go1.23 linkname rules (push and pull).
- Experiments in `.tmp/c2g-exp/` (scratch): `dispatch` (calls, assertions, conversions),
  `model` (interface values against thin pointers), `exc` (exceptions, goroutine ids, guards,
  locks), `gls` (the runtime patch through `go build -overlay`, on amd64 and arm64 under
  `qemu-aarch64`), `fma` (fusion per architecture), `alloc` (allocation), `big` (a synthetic
  package of c2g's shape), `modpath` (a main module named `arbace`), `leaves.clj` (the leaf
  classes of stage 2's `arbace.lang`); `.tmp/c2g-proto/` (the
  prototype of §15, built with `bin/g2c build` for both architectures and compared with
  `bin/arbace`).
- JDK: jdk26u (`/root/jdk26u`): `java/util/function/Predicate.java` (default and static
  methods), `java/lang/runtime/ObjectMethods.java` (records' methods), JDK-8273914 (string
  concatenation's order of conversion, fixed in JDK 19). The JVM specification 5.4.3.3 (default
  method selection), 5.4.5 (overriding and package access), 5.5 (initialization); the JLS 5.1.11
  (string conversion), 15.7 (evaluation order), 15.12.4 (method invocation), 15.26
  (assignment), 17.5 (final field semantics).
