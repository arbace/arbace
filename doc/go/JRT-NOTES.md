# jrt, phase 1: the core

B1a step 3 ([B1-PLAN.md](B1-PLAN.md)), first phase: the hand-written core of **jrt**, the Java
runtime under the classes c2g translates ([C2G-SPEC.md](C2G-SPEC.md)), as Go forms
([SPEC.md](SPEC.md)). Phase 1 is the object model, strings, `Math`, exceptions and their
tests; phase 2 adds concurrency (threads, the goroutine-local slot, atomics, locks, `Unsafe`),
`System` and the host, and reflection, on top of the API below.

State (2026-10-08, branch `jrt-core`): the package `arbace/jrt` builds and passes `go vet` for
linux/amd64 and linux/arm64. Its 28 Go tests pass on both architectures: amd64 natively and
arm64 under `qemu-aarch64`, and also under Go's race detector (amd64). About 295,000 cases, and the case
mapping of every code point, are checked against values the JVM computed. The edge's members of the classes phase 1 owns are
implemented except 13, all of them phase 2's or waiting for translated classes (Coverage).

## Structure

| path | what |
|---|---|
| `go/arbace/jrt.clj` | the package file: `ns go.arbace.jrt`, `go/package jrt :path "arbace/jrt"`, `:files`, `:test-files`, one `load` per file (SPEC §4.1) |
| `go/arbace/jrt/object.clj` | the header `Object`, `Object_I`, the identity hash, `Equals`/`HashCode`/`ToString`/`GetClass`, `Object_toString`, `Object_New`, `NN`, `NPE`, `CloneNotSupported` |
| `class.clj` | `Class`, `ClassInfo` and the member table types, `Define`, the registry and `ForName`, primitive and array classes, `ClassCast`, `ClassInit` |
| `array.clj` | the nine array types, their constructors, `RefArray.Store`, `NewMultiArray`, `Array.newInstance`, `Arraycopy` |
| `monitor.clj`, `threadid.clj` | monitors (thin lock, inflation, wait/notify); the current thread's number (phase 1's stopgap) |
| `string.clj`, `stringbuilder.clj`, `codec.clj` | `String`, `StringBuilder`, `StringBuffer`; interning, `Concat`, the `StrOf...` conversions; UTF-8, Latin-1 and ASCII |
| `numconv.clj`, `math.clj` | d2i and the other conversions, the float constants, `Double.toString`/`Float.toString`; `Math`, `StrictMath` (FdLibm's log, pow, cbrt ported) |
| `throwable.clj` | `Throwable`, `Thrown`, `Catch`, the run-time error mapping, stack traces (`FrameInfo`), `StackTraceElement`, `printStackTrace`'s text |
| `enum.clj`, `volatile.clj` | `Enum` and `Record`, the superclasses of c2g's enums and records; `Volatile[T]` |
| `standin_lang.clj`, `standin_character.clj` | **generated** stand-ins for JDK classes c2g will translate (below) |
| `manifest.edn`, `manifest_test.clj` | **generated**: the manifest (C2G-SPEC §9.1) and a test file naming every manifest member by its Go name |
| `*_test.clj` | the tests, Go forms printed as `_test.go` files (SPEC §15 Q14) |
| `bin/jrt` | `build`, `test`, `testdata`, `standins`, `manifest`, `print` (below) |
| `test/jrt/*.clj` | the generators, run on the JVM Arbace: `testdata.clj` (with `testdata_numbers.clj`, `testdata_util.clj`), `standins.clj`, `manifest.clj` |

Hand-written forms are about 4,700 lines (the core) and 1,600 (the tests); with the generated
files the package prints as 13,600 lines of Go.

**Where the forms live** (C2G-SPEC §4.3, §16 Q19, the user's decision): a forms root `go/` at
the repository's top. c2g will write its output into `target/c2g/go/` and the build will merge
the two trees. The module is `arbace` (§4.2, Q20). Hand-written files never begin with
`java_`, `jdk_`, `sun_` or `c2g_`.

**`bin/jrt`** runs the printer through `bin/g2c build --print-only --tests --module arbace` (BUILD.md,
amendment B6), which prints the module into `.tmp/jrt-go/mod`. Then:

- `bin/jrt build [--arch amd64|arm64|all]` runs `go build ./...` and `go vet ./...` with BUILD.md's
  environment.
- `bin/jrt test [--arch ...] [--race] [go test flags]` runs `go test`: amd64 natively, arm64 with
  `-exec qemu-aarch64`; `--race` adds the race detector on amd64.
- `bin/jrt testdata [--check]` makes the differential tests' expected values (below).
- `bin/jrt standins` and `bin/jrt manifest` regenerate the generated forms.

**The overlay option** of `bin/g2c build` (C2G-SPEC §9.3, Q19) is in, documented as BUILD.md
amendment B5 and checked with the runtime patch of the C2G-SPEC experiments. The patch itself,
as Go forms, is phase 2's.

## The API (C2G-SPEC §11, as implemented)

All in package `arbace/jrt`. Names in **bold** are additions to §11's table (proposed
amendment A5).

| group | names |
|---|---|
| object model | `Object` (header; methods `Self_Object`, `HashCode__I`, `Equals_O__Z`, **`ClearHeader`**), `Object_I`, `Object_New`, `Object_class`, `Object_toString`, `Equals`, `HashCode`, `ToString`, `GetClass`, `IdentityHash`, **`CloneNotSupported`** |
| classes | `Class` (Java methods `GetName__String`, `IsInstance_O__Z`, `IsAssignableFrom_Class__Z`, `Cast_O__O`, `GetSuperclass__Class`, `GetInterfaces__Class1`, `GetComponentType__Class`, `GetModifiers__I`, `GetSimpleName__String`, `IsArray__Z` ...; Go methods **`Info`**, **`GoName`**, **`ArrayClass`**), `ClassInfo`, `FieldInfo`, `MethodInfo`, `CtorInfo`, `Define`, **`ForName`**, **`Kind`** (`KindClass` ... `KindPrimitive`), **`Acc...`** (modifier bits), **`Prim_boolean` ... `Prim_void`**, `Class_ForName_String__Class`, `Class_GetPrimitiveClass_String__Class` |
| arrays | `BooleanArray` ... `DoubleArray`, `RefArray` (field `A`, `Comp`; methods `Store`, **`Copy`**), `New...Array`, `NewRefArray`, `NewMultiArray`, `...ArrayOf`, `RefArrayOf`, **`Arraycopy`**, **`IndexOutOfBounds`**, `System_Arraycopy_O_I_O_I_I__V`, `System_IdentityHashCode_O__I`, `Array_NewInstance_Class_I__O` |
| class initialization | `ClassInit` (`Done`, `Run`) |
| exceptions | `Throwable_I`, `Throwable`, `Thrown`, `Catch`, **`FromRuntimeError`**, `NN`, `NPE`, `ClassCast`, **`StackTraceString`**, **`Uncaught`**, **`StderrPrint`**, **`FrameInfo`**, **`FrameElide`**, **`RegisterFrames`**, `StackTraceElement` |
| conversions | `D2I`, `D2L`, `F2I`, `F2L`, `NaN32`, `NaN64`, `PosInf32`, `NegInf32`, `PosInf64`, `NegInf64`, `NegZero32`, `NegZero64`, **`FormatDouble`**, **`FormatFloat`**, the natives `Double_DoubleToRawLongBits_D__J_native` and the other three (§9.1) |
| strings | `String` (Go methods **`String`**, **`UTF16`**), `Intern`, **`InternUTF16`**, **`Str`**, **`NewStringUTF16`**, `Concat`, `StrOfInt`, `StrOfLong`, `StrOfChar`, `StrOfBool`, `StrOfFloat`, `StrOfDouble`, `StrOfObj`, **`DecodeUTF8`**, **`EncodeUTF8`**, **`DecodeBytes`**, **`EncodeBytes`**, `StringBuilder`, `StringBuffer` |
| monitors, memory | `MonitorEnter`, `MonitorExit`, `Wait`, **`WaitTimeout`**, `Notify`, `NotifyAll`, **`HoldsLock`**, `Volatile` (`Load`, `Store`), **`Inflated`** (a counter) |
| threads | phase 2: `CurrentThread`, `Thread` |
| Java classes | `String`, `StringBuilder`, `StringBuffer`, `Class`, `Throwable`, `StackTraceElement`, `Enum`, `Record`, `Math`, `StrictMath`, `StringLatin1`/`StringUTF16` (one or two members each), `ThrowableTracer`, by their §4.4 names; the manifest lists them |

## Decisions where the spec was silent

**The header.** `Object` is `struct { hdr uint64 }`, read and written with `sync/atomic`
functions. It is a plain `uint64`, not `atomic.Uint64`, so that c2g's `Clone__O` can copy a
struct without `go vet`'s copylocks complaint; `ClearHeader` then resets the copy's header.
The low 32 bits hold the identity hash and the high 32 bits the lock word.

- **The identity hash** is 31 bits, never 0, as HotSpot's. It is assigned on first use by a
  CAS from a global atomic sequence mixed by a 32-bit finalizer, not HotSpot's per-thread
  xorshift (V3 allows any values).

**Monitors: implemented fully in phase 1.** The lock word holds:

- **thin**: state 1, an 8-bit recursion count, a 22-bit owner thread number;
- **inflated**: state 2, a 30-bit index into jrt's monitor table.

Inflation happens on contention, on recursion beyond 255 and on `wait`, or when the owner's
number does not fit 22 bits. A monitor holds a `sync.Mutex`, an entry condition, the owner, the
count, a reference count of threads entering or waiting, and a waiter queue (one channel per
waiter). It deflates when it is free with no refs and no waiters, under the table's lock, and
the identity hash is kept throughout. `Wait` releases and restores the recursion count; `notify`
wakes waiters first in, first out. `IllegalMonitorStateException` carries the JVM's message.

- **Thread identity** is phase 2's. Phase 1 uses C2G-SPEC §8.4's stopgap, the goroutine id
  from `runtime.Stack` (about 2.9 µs). Phase 2 replaces `currentThreadID` (`threadid.clj`) with
  the current `Thread`'s number from the slot.
- **Interruption**: `Wait` selects on `interruptChan()`, which is nil until phase 2's
  `Thread` sets it, and throws `InterruptedException` after reacquiring.

**Non-leaf hand-written classes** (`Throwable`, `Enum`, `Record`) follow §5.4:

- `Impl_` methods take `this C_I` after the receiver.
- The dispatch methods exist on the concrete class (`Throwable`), and stand-ins and c2g's
  subclasses write forwarders to them.
- Constructor bodies are `Ctor...` methods taking `this C_I` after the receiver, which §15.3's
  `(.Ctor (.-APersistentVector t) t)` implies (amendment A2).
- The promotable methods are `Object`'s `hashCode` and `equals`, and `Enum`'s `name`,
  `ordinal`, `hashCode` and `equals`.

**`new Object()`** is `Object_New() any`: a pointer to an unexported struct whose class is
`Object_class` (A8). Lock objects and sentinels use it.

**`ClassInfo`** has the fields of §5.11 and **`Go`**, the qualified name of the Go type, which
stack traces use. Go rejects initialization cycles that Java's classes have: class A's member
table names class B, B's names A, and a function literal's body counts as a reference.
Therefore `Define` registers only the hierarchy at variable initialization. The function and
table fields (`IsInstance`, `Init`, `Enum`, `FromFn`, `Fields`, `Methods`, `Ctors`) are set in
an `init` function through `C_class.Info()` (A1). jrt does this for its own classes and its
stand-ins.

- `ForName` resolves registered binary names and JVM array names (`[I`, `[[Ljava.lang.String;`).
- Array classes are created on first use and cached in their component class.
- The primitive classes are `Prim_int` and its kin. `Class.forName("int")` fails, as on the JVM.
- Modifiers of arrays and primitives are the JVM's (1041 for `int[]`, `String[]`, `int`).

**Class initialization.** `ClassInit.Run` follows JVMS 5.5 with the current thread's number:

- An exception gives `ExceptionInInitializerError`; an `Error` passes through unwrapped.
- Later uses throw `NoClassDefFoundError: Could not initialize class X`, with the first
  exception as its cause.
- The class name comes from the Go name of `C_clinit` through the frame table (or the Go name
  itself), so `Run(clinit)` keeps §6.2's signature.
- Concurrent first uses wait for the initializing thread (tested with 32 goroutines).

**Arrays.**

- `Store` checks the index first and the store second, as `aastore` does. It skips the check
  when the component is `Object`.
- `NewMultiArray(cls, dims...)` takes the array class (`int[][]`'s), checks every size before
  allocating, and returns a `*RefArray` (A3).
- `Copy()` is clone at the array's own type.
- `Arraycopy` reproduces HotSpot's order of checks and its messages, from
  `objArrayKlass.cpp` and `typeArrayKlass.cpp`. That includes copying elements up to the first
  one that fails a store check.
- `System.arraycopy` and `System.identityHashCode` are defined here, though `System` is
  phase 2's.

**Strings.**

- **Representation.** `String` is `struct { Object; value []uint16; hash int32 }`; the hash is
  cached through atomics, so the race detector stays quiet.
- **`COMPACT_STRINGS` is true**, so translated code builds Latin-1 byte arrays where Java's
  would. `StringUTF16`'s byte arrays are little-endian, as `putChar` and `newString` agree.
- **Interning**: one table, keyed by the UTF-8 text when the code units are valid UTF-16, else
  by the raw units behind a 0xFF byte. Literals (`Intern`, `InternUTF16`) and `String.intern`
  share it. `String.valueOf(boolean)` returns the interned literals, as Java's does.
- **Algorithms that depend on Java's internal coder.** Java keeps a string as Latin-1 when every
  unit is at most 0xFF, and some results depend on it: `compareToIgnoreCase` compares code
  units when either string is Latin-1, and by code points otherwise. jrt reproduces this through
  `isLatin1` (verified against the JVM).
- **Case mapping** follows `StringUTF16.toLowerCase`/`toUpperCase` for the root locale.
  `Character` is not translated yet, so a stand-in maps over Go's `unicode`, which is Unicode
  17.0.0 like JDK 26's `Character`, plus the 102 special uppercase mappings of jdk26u's
  `SpecialCasing.txt` (generated). Every code point's `toUpperCase` and `toLowerCase` equal the
  JVM's (tested, all 1,112,064).
- **Final sigma.** Java finds the word with `java.text.BreakIterator` (cut); jrt takes letters,
  marks, digits, connectors and apostrophes as the word: an approximation, exact on the tests.
- **Charsets**: UTF-8 decoding replaces malformed input exactly as JDK 26's
  `String.decodeUTF8_UTF16` does (21 malformed sequences tested), and encoding writes `?` for
  an unpaired surrogate. ISO-8859-1 and US-ASCII and their aliases are supported; any other
  name throws `UnsupportedEncodingException`.
- **Go's side.** `Str` and `String()` convert Go's UTF-8 to and from Java's UTF-16, with U+FFFD
  for invalid bytes and unpaired surrogates. `String()` makes `*String` a `fmt.Stringer`.
- `StringBuilder` and `StringBuffer` share `AbstractStringBuilder`'s behaviour, an embedded
  `sbuf` whose capacity grows as Java's does (max(min, 2·old+2)). `StringBuffer` synchronizes on
  its own monitor. Their exceptions are Java's, including the mix of `IndexOutOfBounds` and
  `StringIndexOutOfBounds` across methods.

**Exceptions.**

- **Helpful `NullPointerException` messages are not reproduced.** An implicit NPE has a null
  message, as the JVM's with `-XX:-ShowCodeDetailsInExceptionMessages`; the native
  `getExtendedNPEMessage` returns null. The oracle's cases that record helpful messages
  (ORACLE.md) are accepted mismatches (A6).
- **`ClassCastException`'s message** emulates the JVM's, module and loader included: `module
  java.base of loader 'bootstrap'` for `java.`, `javax.`, `jdk.` and `sun.` classes, arrays
  included; `unnamed module of loader 'app'` otherwise. The JVM's messages for classes defined
  by `DynamicClassLoader` include an identity hash and never match.
- **Go's run-time errors** map to Java's exceptions:

  | Go run-time error | Java exception |
  |---|---|
  | nil dereference, `PanicNilError` | `NullPointerException` |
  | index out of range | `ArrayIndexOutOfBoundsException("Index i out of bounds for length n")`, the length read from `runtime.boundsError`'s fields by reflection because Go's message omits it for negative indexes |
  | slice bounds | `IndexOutOfBoundsException` with Go's message (jrt-internal only) |
  | integer divide by zero | `ArithmeticException("/ by zero")` |
  | type assertion | `ClassCastException` with Go's message (c2g never asserts without comma-ok) |
  | `makeslice` | `OutOfMemoryError("Java heap space")` |

  Anything else panics again.
- **`printStackTrace()`** writes through `StderrPrint`, which phase 2 points at `System.err`.
  `StackTraceString` is the text, Java's format: suppressed exceptions, causes, `... n more`,
  `[CIRCULAR REFERENCE: ...]`. `Uncaught` prints `Exception in thread "NAME" ...`.

**Stack traces** (§7.9.6, A4).

- **Capture.** The constructor captures up to 1,024 program counters through
  `fillInStackTrace` (64 first). `getStackTrace` maps them on first use.
- **The frame table.** c2g's `c2g_frames.go` calls `RegisterFrames([]FrameInfo{{Go, Class,
  Method, Flags}})`, where `Go` is `runtime.Frame.Function`'s name and `FrameElide` hides a
  frame.
- **Frames c2g did not register** get a demangler that reads §4.4's names: `(*T).M_..._R`,
  `T_m...`, `Impl_`, `Ctor` (shown as `<init>`), `_New` (an allocation function, elided) and
  `_clinit` (`<clinit>`). jrt's own methods are therefore named as Java's
  (`java.lang.String.charAt`).
- **Elided frames**: runtime, testing and reflect frames, and jrt's helpers.
- **Rules applied to the mapped frames:**
  - Frames before the last `runtime.gopanic` are dropped: an exception built in `Catch` for a
    run-time error starts at the failing frame.
  - At the top, the frames of `fillInStackTrace` and of the constructors of `Throwable`
    classes are dropped, as HotSpot drops them.
  - A forwarder, recognized because its callee is `Impl_` of its own name, is dropped.
  - A function literal stands for its function (a `try` body), and the function's own frame
    below it is dropped.
- **File names and lines** are Go's: the forms' files and lines in a `--line-file` build.
- **`StackTraceElement.toString`** writes `java.base/` before JDK classes, as the JVM does.

**`Enum`**:

- `F_name` and `F_ordinal` are fields, which enum switches read (§7.8).
- `Impl_Clone__O` throws `CloneNotSupportedException` without a message, as `Enum.clone` does
  (c2g forwards to it; A10).
- `Enum.valueOf` searches `ClassInfo.Enum`. A missing name throws `No enum constant X.NAME`,
  where X is the binary name with `$` as `.`.

**`Volatile[T]`** has `Load` and `Store`; a nil store allocates nothing. Compare-and-set on
references is phase 2's (`Unsafe`, `AtomicReference`, with striped locks, §9.2).

**Math** (on the branch `jrt-math`, merged):

- All 34 edge members, and Arbace's: `decrementExact`, `incrementExact`, `multiplyExact(JJ)`,
  `negateExact`, `random`, `subtractExact`, `toIntExact`.
- Beyond the edge: every `*Exact`, `floorDiv`, `floorMod`, `ceilDiv` and `ceilMod` overload,
  `signum`, `copySign`, `rint`, `ulp`, `nextDown`, `nextAfter`, `fma` (D and F), `round(F)`.
- `StrictMath`: `log`, `sqrt`, `cbrt`, `pow`, `rint`.

How they are computed:

- **`log`, `pow`, `cbrt`** are ports of jdk26u's `java.lang.FdLibm`, to which Java's source
  delegates them. Go's `math.Log` has amd64 assembly and `math.Pow` is a different algorithm,
  so neither can be used.
- **Fusion.** Every fusable product goes through `fmul`, an explicit `float64` conversion.
- **Results.** jrt equals `StrictMath` bit for bit. HotSpot's `Math.log`, `pow` and `cbrt`
  intrinsics differ from `StrictMath` by one ulp in 34, 92 and 226 of the tested cases; the
  tests accept that and fail on a larger difference.
- **NaN** results are canonical.
- **`Math.random`** draws from Go's `math/rand/v2`, not `java.util.Random`'s sequence.
- **Float `fma`** rounds once, as Java's does.
- **Messages** are jdk26u's: `integer overflow`, `long overflow`, `Overflow to represent
  absolute value of Integer.MIN_VALUE`, and the `clamp` messages.

**Fusion on arm64.** With x = 1+2^-27 and z = −(1+2^-26):

| expression | amd64 | arm64 |
|---|---|---|
| `fmul(x, x) + z` (the idiom) | 0 | 0 |
| plain Go `x*x + z` | 0 | 2^-54 (gc fused it) |

0 is Java's result. A test asserts the idiom on both architectures.

**`Double.toString` and `Float.toString`** take strconv's shortest digits, use two digits when
the shortest has one (JDK 19+'s rule), and format with Java's thresholds (10^-3 ≤ |d| < 10^7).
They equal the JVM in 60,228 cases: the special values, powers of 2 and 10, and 30,000 random
values of each type.

## Stand-ins

jrt's core throws, catches and implements JDK classes that c2g will translate from jdk26u: the
exceptions (`NullPointerException` ... `UnsupportedEncodingException`, 32 classes) and the
interfaces `Serializable`, `Cloneable`, `Comparable`, `CharSequence` and `Appendable`.

**`standin_lang.clj`** is generated by `test/jrt/standins.clj` in the exact shapes C2G-SPEC
gives translated classes:

- §4.4 names;
- class interfaces `C_I` with `Self_C` for the non-leaf classes;
- constructors `C_New_...` and `Ctor_...`;
- forwarders for `Throwable`'s twelve virtual methods;
- `Ref`, `GetClass__Class`, `Clone__O`;
- `C_InstanceOf` and `C_Cast` with the JVM's `ClassCastException`;
- `C_class` registrations.

jrt and translated code therefore meet at the final names now. When c2g's output joins the
build, **the stand-in files are deleted**, and the translated classes define the same names.
Their leafness may change in the bigger closed world; jrt's core uses only the constructors and
the interfaces, so it is not affected.

**`standin_character.clj`** gives `String` the parts of `Character` it needs, as unexported
functions over Go's `unicode`: `charToUpper`, `charToLower`, `charToUpperEx`,
`charToUpperArray`, `charIsWhitespace` and `isCased`. They are later rewritten to call the
translated `Character_...`.

A translated `CharSequence` will list its default methods `chars()` and `codePoints()`. `String`
and `StringBuilder` will then need forwarders to them; they return `IntStream`, which is cut, so
the forwarders will be stubs.

## The manifest

`go/arbace/jrt/manifest.edn` (§9.1) is **derived**, not hand-kept. `test/jrt/manifest.clj`
reflects on each hand-written class of the running JDK 26 and computes every member's §4.4 Go
name. A member is in the manifest when jrt's forms define that name, or `Impl_` of it, or the
member is promotable. The manifest records:

- the class's flags and superclass;
- the interfaces jrt's implementation has (not the JDK's full list: `Constable` and
  `ConstantDesc` are cut);
- the members with their descriptors and modifiers (`:native` removed: jrt implements them in
  Go);
- the promotable set.

Public methods inherited from non-public superclasses (`AbstractStringBuilder`'s) are listed on
the subclass, as jrt defines them there. The generated `manifest_test.clj` names every member by
its Go name, so the test build fails when forms and manifest drift. `Object`'s members are §5.8's
and are not listed by name.

| class | in the manifest / the JDK class's members |
|---|---:|
| `String` | 77 / 182 |
| `StringBuilder` | 53 / 94 |
| `StringBuffer` | 53 / 97 |
| `Class` | 19 / 170 |
| `Throwable` | 18 / 35 |
| `StackTraceElement` | 9 / 29 |
| `Enum` | 10 / 14 |
| `Record` | 1 / 4 |
| `Math` | 71 / 119 |
| `StrictMath` | 5 / 111 |
| `System` | 2 / 51 |
| `reflect.Array` | 1 / 24 |
| `StringLatin1` | 1 / 51 |
| `StringUTF16` | 2 / 91 |
| `ThrowableTracer` | 1 / 5 |

## Coverage of the edge (JRT-SOURCES.md, `.tmp/jrt/edge.edn`)

The members the translated JDK classes call, for the classes phase 1 owns:

| class | edge | done | not done, and why |
|---|---:|---:|---|
| `String` | 49 | 43 | `<init>([BIILCharset;)`, `getBytes(Charset)` (phase 2: `Charset`); `toLowerCase(Locale)`, `toUpperCase(Locale)` (phase 2: `Locale`; the root-locale bodies are here); `format` (after c2g: `Formatter` is translated); `lines()` (`Stream`: cut) |
| `StringBuilder` | 31 | 31 | |
| `StringBuffer` | 9 | 9 | |
| `Math` | 34 | 34 | |
| `StrictMath` | 2 | 2 | |
| `Throwable` | 13 | 13 | |
| `Object` | 7 | 7 | §5.8's mechanisms (`Object_New`, `Clone__O` per class, `Equals`, `GetClass`, `HashCode`, `ToString`, `WaitTimeout`) |
| `Class` | 9 | 6 | `getDeclaredField`, `getGenericInterfaces` (phase 2: reflection), `getResourceAsStream` (phase 2: the host's resources) |
| `Enum`, `Record` | 5, 1 | 5, 1 | |
| `System` | 6 | 2 | `err`, `getProperty`, `lineSeparator`, `nanoTime` (phase 2: `System`, the host) |
| `reflect.Array`, `StringLatin1`, `StringUTF16`, `ThrowableTracer` | 1, 1, 2, 1 | all | |
| `JavaLangAccess` | 6 | 0 | phase 2, with `SharedSecrets` and `System`: its methods are a few lines over `String`'s internals |

Arbace's own runtime also uses some `String` members (JAVA-SURFACE.md's `:arbace` uses):

- **Done:** `concat`, `contains`, `endsWith`, `intern`, `join`, every `lastIndexOf`,
  `replace(CharSequence, CharSequence)`, `toLowerCase()`, `toUpperCase()`.
- **Waiting for the `java.util.regex` port:** `split`, `replaceAll`, `replaceFirst`.
- **Waiting for c2g:** `format`.

Its `StringBuilder` uses (`reverse`, `<init>(CharSequence)`) are done, as are its `Throwable`
ones (`getLocalizedMessage`, `getStackTrace`, `setStackTrace`, `printStackTrace()`). The
`PrintWriter` overload of `printStackTrace` is phase 2's. Of its `Class` uses, the reflection
members are phase 2's.

## Tests

Go tests written as Go forms (`*_test.clj`, printed as `_test.go`, in package `jrt`):

| test | what |
|---|---|
| `TestStringCases` | 11,861 cases of `strings.txt` (below) |
| `TestChars` | `toUpperCase`/`toLowerCase` of all 1,112,064 code points against the JVM's (`chars.txt` lists the 3,037 that change) |
| `TestExceptionCases` | 107 cases (103 run, 4 have no Go counterpart) of `exceptions.txt`: `String`'s, `StringBuilder`'s, arrays', `arraycopy`'s, `Throwable`'s and monitors' exceptions and messages, the run-time errors' mapping, `StackTraceElement`, class names and modifiers |
| `TestStringBasics`, `TestStringBuilder` | interning, Go conversions, `Concat`, `hashCode`, builders |
| `TestTryCatch`, `TestFinally`, `TestControlCodes` | `try`/`catch`/`finally` and control codes written as §7.9 says c2g writes them |
| `TestRuntimeErrorMessages` | the run-time errors' Java messages |
| `TestThrowableAPI`, `TestStackTraces`, `TestPrintStackTrace` | `Throwable`'s API, stack traces through the frame table and the demangler, `printStackTrace`'s text |
| `TestIdentity`, `TestArrays`, `TestClasses` | the object model, arrays, `Class` |
| `TestClassInit`, `TestClassInitConcurrent` | JVMS 5.5 |
| `TestMonitorReentrant`, `TestMonitorExclusion`, `TestWaitNotify` | monitors |
| `TestEnum`, `TestVolatile` | an enum written as c2g would write it, `Volatile` |
| `TestToString`, `TestDoubles`, `TestFloats`, `TestInts`, `TestNoFusion`, `TestMathExceptions` | 60,228 + 110,312 + 65,072 + 47,754 cases of the numbers files; the FMA idiom |

**Differential test data** comes from the JVM. `test/jrt/testdata.clj` (ns `jrt.testdata`) runs
on `bin/arbace`, the JDK 26 Arbace runs on, and writes `strings.txt`, `chars.txt`,
`exceptions.txt`, `tostring.txt`, `doubles.txt`, `floats.txt` and `ints.txt`.

- **Format:** one case per line with tab-separated fields: the operation, its arguments, the
  result. Strings use Java escapes (`\uXXXX` outside printable ASCII), doubles and floats are
  raw bits in hex, and an exception is `!CLASS: MESSAGE`.
- **Inputs:** fixed, plus seeded `java.util.Random`. Two runs give the same bytes
  (`bin/jrt testdata --check`).
- **Storage:** the data is about 12 MB and is **regenerated, not tracked**: `.tmp/jrt-go/
  testdata`, made by `bin/jrt test` when missing or older than its generator (4 s), and copied
  into the printed package's `testdata/`.
- **Exceptions to the differential method:** float32 arithmetic and shift masking are tested by
  their Go idioms, not against the JVM, where Clojure would round through double or restate the
  rule.

Results (2026-10-08):

| architecture | result | time |
|---|---|---:|
| linux/amd64 | all pass | 1.7 s |
| linux/arm64 (qemu-aarch64) | all pass | 17 s |
| linux/amd64 with `-race` | all pass | 20 s |

## Measurements

`bin/jrt test --arch amd64 -run X -bench . -cpu 1 -benchmem` (`bench_test.clj`), AMD EPYC
7763, go1.27.1, `GOAMD64=v1`:

| operation | ns/op | allocs |
|---|---:|---:|
| `Concat` of a literal, `StrOfInt`, a literal | 131 | 4 |
| `StrOfDouble` | 275 | 5 |
| `hashCode` of a new 16-unit `String` | 11.2 | 0 |
| `equals`, 16 units | 8.4 | 0 |
| `String.intern` of an interned value | 101 | 1 |
| `toUpperCase`, 12 ASCII units | 221 | 2 |
| `new StringBuilder().append(s).append(i).append(c).toString()` | 154 | 5 |
| identity hash, assigned / read | 16.8 / 6.9 | 1 / 0 |
| the current thread's number (phase 1's stopgap) | 2,900 | 1 |
| `MonitorEnter` + `MonitorExit`, uncontended (twice the stopgap) | 8,900 | 2 |
| `C_Init()`'s fast path | 0.63 | 0 |
| checked store into a `String[]` | 5.7 | 0 |
| an entered `try` that does not throw | 8.8 | 0 |
| new exception, throw, catch | 1,740 | 2 |
| a nil dereference caught as `NullPointerException` | 5,700 | 3 |
| `getStackTrace` of a fresh trace | 6,200 | 14 |
| `Math.pow` (FdLibm port) / `Math.log` | 54 / 9.3 | 0 |

The monitor and thread numbers are the stopgap's: with the slot of §9.3, `currentThread()` is
3.7 ns (§13.3), and an uncontended monitor should cost about 10 ns. Throwing is costlier than
the prototype's 305 ns (§13.3), because jrt captures up to 64 frames and the constructor runs
through the class chain's forwarders; it can be tuned (the first capture smaller, or deferred)
when profiles show exceptions on hot paths.

## What phase 2 must add

- **Threads.**
  - `Thread` and `CurrentThread` over the goroutine-local slot. The runtime patch is Go forms
    given to the build by `--overlay`.
  - Replace `currentThreadID`'s body (`threadid.clj`) with the current `Thread`'s number,
    keeping it small, at most 2^22 for thin locks.
  - Set `interruptChan` so that `Wait` throws `InterruptedException`.
  - `ThreadLocal`, `ThreadLocalRandom`'s probes, the uncaught handler (with `Uncaught`), and
    the thread entry that prints and exits 1 for `main`.
- **Concurrency.** `Unsafe` (39 members, striped locks for references, §9.2), `AtomicInteger`,
  `AtomicLong`, `AtomicReference`, `ReentrantLock`, `Condition`, `LockSupport`, executors,
  `CountDownLatch`, `Reference`, `SoftReference` and `WeakReference`.
- **`System` and the host.**
  - `System`: `err`, `out`, `in`, `getProperty`, `lineSeparator`, `nanoTime`,
    `currentTimeMillis`, `exit`; point `StderrPrint` at `System.err`.
  - `Runtime`, `VM`, `CDS`, `SharedSecrets` and `JavaLangAccess` (6 members, over `String`'s
    internals: `countPositives`, `inflateBytesToChars`, ...).
  - The host interface (§9.4), `File` and the streams, `PrintStream`, `OutputStreamWriter`.
  - `Charset`, `UTF_8` and `ISO_8859_1`, over `DecodeBytes`/`EncodeBytes`, and with them
    `String`'s `Charset` overloads.
  - `Locale` (with `String`'s `Locale` overloads: Turkish, Azerbaijani and Lithuanian rules),
    `DecimalFormatSymbols`, `SecureRandom`, `Date`.
- **Reflection** over the member tables:
  - `Class.getMethods`, `getFields`, `getConstructors`, `getDeclared*`, `getField`,
    `getMethod`, `getDeclaredConstructor`;
  - `Method.invoke`, `Field.get`/`set`, `Constructor.newInstance`;
  - `Class.forName(name, init, loader)`, `getResourceAsStream`, `getDeclaringClass`, `cast`'s
    use by `Reflector`.
- **`printStackTrace(PrintStream)` and `(PrintWriter)`**, over `StackTraceString`.
- **Later, with c2g:**
  - delete the stand-ins;
  - have `String` call the translated `Character` (its stand-in functions are unexported, in
    one file);
  - `String.format` over the translated `Formatter`, and `split`/`replaceAll`/`replaceFirst`/
    `matches` over the regex port;
  - forwarders for `CharSequence`'s default methods;
  - the `Dyn` check in c2g's `InstanceOf` for interfaces (§5.12).

## Proposed amendments to C2G-SPEC (for the user's review)

- **A1** (§5.11): `ClassInfo` gains `Go`, the Go type's qualified name, used by stack traces.
  Its function and table fields are set in an `init` function through `C_class.Info()`, not in
  the `Define` call: Go rejects the initialization cycles that Java's mutually referring classes
  make, and a function literal's body counts as a reference. `C_InstanceOf` must not refer to
  `C_class` if it is given at `Define`.
- **A2** (§5.2, §5.4): a non-leaf class's constructor bodies are `Ctor...(t *C, this C_I,
  args...)`, as §15.3's super call implies. A leaf class's take no `this`.
- **A3** (§5.9, §11): the array API as implemented:
  - `NewMultiArray(cls *Class, dims ...int32) *RefArray`, with `cls` the array class;
  - `Copy()` for a clone at the array's type;
  - `IndexOutOfBounds(i, n int)`;
  - `Arraycopy` (with `System_Arraycopy_O_I_O_I_I__V` calling it);
  - `Array_NewInstance_Class_I__O`.
- **A4** (§7.9.6): the frame table:
  - `FrameInfo{Go, Class, Method, Flags}` and `RegisterFrames`, c2g writing `c2g_frames.go`;
  - jrt's demangler as the fallback, so c2g registers what it cannot express by names:
    lambdas (`lambda$m$0`), methods whose Java name contains `_`, frames to elide;
  - a function literal without an entry stands for its function.
- **A5** (§11): the additions in bold in the API table above.
- **A6** (§7.9.5): implicit `NullPointerException`s have a null message (the JVM's helpful
  messages are not reproduced), and the oracle's cases that record them are accepted
  mismatches. Reproducing them would need c2g to pass the expression's text to each check.
- **A7** (§8.1): the lock word's layout as implemented. A thread number that does not fit 22
  bits inflates.
- **A8** (§5.8): `new Object()` is `jrt.Object_New() any`, of an unexported type; a clone's
  header is reset with `ClearHeader`.
- **A9** (§4.3): stand-in files `standin_*` (generated, tracked) hold jrt's stand-ins for
  translated classes until c2g's output joins, and are deleted then.
- **A10** (§7.13, §9.1): `Enum`'s promotable methods are `name`, `ordinal`, `hashCode` and
  `equals`. `clone` is `Impl_Clone__O` (final, throwing `CloneNotSupportedException`), to which
  enum classes forward.
- **BUILD.md B6**: `--print-only`, `--tests`, `--module` (BUILD.md, "Libraries and tests").

## Sources

- jdk26u (`/root/jdk26u`, openjdk/jdk26u at `baf63fb`), all under
  `src/java.base/share/classes/`:
  - `java/lang/String.java` (bounds checks, `indexOf`, `repeat`, `decodeUTF8_UTF16`,
    `CaseInsensitiveComparator`, `regionMatches`);
  - `java/lang/StringUTF16.java` (`toLowerCaseEx`, `toUpperCaseEx`, `compareToCIImpl`,
    `codePointIncluding`) and `java/lang/StringLatin1.java` (`compareToCI`,
    `compareToCI_UTF16`);
  - `java/lang/AbstractStringBuilder.java` (checks, growth, `setLength`, `repeat`);
  - `java/lang/ConditionalSpecialCasing.java` (`isFinalCased`, `isCased`);
  - `jdk/internal/util/Preconditions.java` (`outOfBoundsMessage`);
  - `java/lang/Throwable.java` (`printStackTrace`, `printEnclosedStackTrace`, `initCause`,
    `addSuppressed`), `java/lang/StackTraceElement.java`, `java/lang/Enum.java`;
  - `java/lang/FdLibm.java` (Log, Pow, Cbrt), `java/lang/Math.java`;
  - the exception classes' constructors.
- Other jdk26u files:
  - `src/java.base/share/data/unicodedata/SpecialCasing.txt`;
  - `src/hotspot/share/oops/objArrayKlass.cpp` and `typeArrayKlass.cpp` (arraycopy's checks
    and messages).
- Go go1.27.1 (`/root/tamago-go`):
  - `src/runtime/error.go` (`boundsError`'s fields and messages);
  - `src/unicode/tables.go` (Unicode 17.0.0);
  - the overlay format of `go help build`.
- The JVM oracle's notes ([ORACLE.md](ORACLE.md), "What the Go side should know"); the edge
  ([JRT-SOURCES.md](JRT-SOURCES.md)); the prototype `.tmp/c2g-proto/` (its `jrt.clj`, whose
  header, `Lit`, `Catch` and `ClassInit` this generalizes).

# jrt, phase 2a: threads, concurrency, Unsafe, System and the host

B1a step 3, phase 2a (branch `jrt-threads`, 2026-10-08): the goroutine-local slot of the patched
Go runtime, `Thread` over it, thread locals, interrupts, the `java.util.concurrent` classes the
translated JDK and Arbace use (atomics, locks, executors, latches), `Unsafe`, references,
`System`, `Runtime` and the host interface. Phase 2b (reflection and the remaining shims) runs
in parallel on another branch.

State: the package builds and passes `go vet` for linux/amd64 and linux/arm64 with the patched
runtime; its 46 Go tests (28 of phase 1, 18 new) pass on amd64, on arm64 under `qemu-aarch64`
and under the race detector (amd64). Of the edge's 95 members of these classes, 89 are
implemented; the 6 others wait for classes that are cut or not written yet (Coverage, below).

## Structure

| path | what |
|---|---|
| `overlay/go/runtime.clj` | the patched runtime files as Go forms (not a package of the program): `ns go.runtime`, `go/package runtime :files [...]` naming the three |
| `overlay/go/runtime/arbace_local.clj` | the new file `arbace_local.go`: `arbace_getLocal`, `arbace_setLocal` (one-argument `//go:linkname`, `//go:nosplit`) |
| `overlay/go/runtime/proc.clj`, `runtime2.clj` | `proc.go` and `runtime2.go` of the toolchain's runtime, converted by `bin/g2c convert --goos linux runtime` (the same forms for amd64 and arm64), with the patch's two lines, each marked `; Arbace` |
| `go/arbace/jrt/thread.clj` | the slot (linkname pulls), thread numbers, `Thread` (non-leaf), `CurrentThread`, start/join/sleep/yield/interrupt, handlers, `RunMain`, `Go`, `RunnableOf`, `Thread.ofVirtual()`, `header` |
| `threadlocal.clj` | `ThreadLocal` (non-leaf), `ThreadLocal$SuppliedThreadLocal`, `InheritableThreadLocal`, `ThreadLocalRandom`'s probes |
| `threadid.clj` | `currentThreadID`, now the current `Thread`'s number (phase 1's file, body replaced) |
| `atomic.clj` | `Volatile.CompareAndSet`/`Swap`, `AtomicInteger`, `AtomicLong`, `AtomicBoolean`, `AtomicReference` |
| `locks.clj` | `Lock`, `Condition`, `ReentrantLock` and its conditions, `ReentrantReadWriteLock` (+ `ReadLock`, `WriteLock`), `LockSupport` |
| `executor.clj` | `ThreadFactory`, `Executor`, `ExecutorService`, `Future`, `Executors`, the pools, `FutureTask`, `CountDownLatch` |
| `unsafe.clj` | `jdk.internal.misc.Unsafe`, `RegisterGoType` |
| `reference.clj` | `Reference`, `WeakReference`, `SoftReference`, `ReferenceQueue` |
| `host.clj` | `Host`, `HostFile`, `HostFileInfo`, `OSHost`, `SetHost`/`CurrentHost`, the standard streams `Stdin`/`Stdout`/`Stderr` |
| `system.clj` | `System`'s clocks, properties, `getenv`, `exit`, `gc`, `lineSeparator`; `Runtime`; `StderrPrint` pointed at `Stderr` |
| `standin_timeunit.clj` | a hand-written stand-in for `java.util.concurrent.TimeUnit` (deleted when c2g translates it) |
| `thread_test.clj`, `concurrent_test.clj`, `system_test.clj` | the tests and benchmarks |

Hand-written: about 3,600 lines of jrt forms and 900 of tests; the overlay's converted files are
5,800 lines of forms (370 KB).

## The goroutine-local slot (C2G-SPEC §9.3)

The patch is §9.3's, unchanged: a field `arbaceLocal unsafe.Pointer` after `coroarg` in `g`
(`runtime2.go`), `gp.arbaceLocal = nil` in `gdestroy` (`proc.go`, with the other fields cleared
there), and `arbace_local.go` with the two linknamed functions.

**As Go forms.** `proc.go` and `runtime2.go` are held as the converter's forms of the toolchain's
files plus the two lines; `arbace_local.go` as hand-written forms. This is §16 Q19's
"the runtime's converted files with the three changes".

**How it is built.** `bin/jrt overlay` prints the three files (`bin/g2c-print --layout gofmt`)
into `$JRT_WORK/overlay/runtime/` and writes `$JRT_WORK/overlay/overlay.json`, which replaces
`$G2C_GOROOT/src/runtime/proc.go` and `runtime2.go` and adds `arbace_local.go` (Go's `-overlay`,
BUILD.md amendment B5). `bin/jrt build` and `bin/jrt test` make it when it is missing or older
than the forms, and pass it to `go build`, `go vet` and `go test` (`-race` included). A program
built with `bin/g2c build --overlay $JRT_WORK/overlay/overlay.json DIR` links. Without the
overlay, jrt fails to link (`relocation target runtime.arbace_setLocal not defined`), so a build
cannot silently miss the patch. Checked with a small program of Go forms using `RunMain`, a
thread and a thread local, built for amd64 and arm64 (run under `qemu-aarch64`).

**Pinned to the toolchain.** The forms are TamaGo's go1.27.1 `proc.go` (which differs from
upstream go1.27.1's by TamaGo's `GOOS=tamago` paths) and `runtime2.go` (identical to
upstream's). `bin/jrt overlay --check` converts `$G2C_GOROOT`'s runtime again and requires the
two forms files to equal the conversion but for the lines marked `; Arbace`: with Alpine's
upstream tree (`G2C_GOROOT=/usr/lib/go`) it fails on `proc.go`, as it should, and a toolchain
update shows up the same way. The same forms serve `GOOS=tamago` (B1b): the files have no
build constraints.

## Threads (C2G-SPEC §8.4)

- **`Thread`** is a non-leaf class (`ForkJoinWorkerThread` extends it): `Thread_I`, `Impl_...`,
  `Ctor...`, the dispatch methods (§5.4). Its struct holds the whole object (`self`), the Java id
  (`tid`, from 1, never reused), the lock number (`num`), the name, target, state (new, alive,
  terminated), daemon flag, priority, `done` (closed at the end, for `join`), the interrupt token
  `intr`, the permit `park`, the uncaught-exception handler and the thread-local maps.
- **The current thread**: `CurrentThread()` reads the slot (3.4 ns). A goroutine jrt did not
  start (Go's `main`, tests, a `go` statement in Go code) gets a Thread on first use
  ("adopted"): a daemon named `Thread-N`, alive.
- **Thread numbers** (the thin lock's 22-bit owner field; A7): numbers come from a free list.
  A jrt thread frees its number when its run ends. An adopted goroutine's number is freed by a
  `runtime.AddCleanup` on its Thread, which becomes unreachable when the goroutine ends and
  `gdestroy` clears the slot. So numbers stay below the peak count of live threads (tested:
  2,000 sequential threads use numbers below 1,000), and the inflation fallback for numbers
  beyond 2^22 is never reached in practice.
- **The thread entry** (`threadEntry`, every goroutine Java code starts): takes a number, puts
  the Thread in the slot, runs `this.run()` under `Catch`, hands an uncaught exception to the
  thread's handler, else the default handler, else `Uncaught` (`Exception in thread "NAME"
  ...` on the standard error), then ends the thread (maps dropped, `done` closed, slot cleared,
  number freed, the non-daemon count decremented).
- **`RunMain(run) int`**: the program's main as the JVM runs it. The calling goroutine becomes
  the thread `main` (#1 when first); an uncaught exception is reported and makes the status 1;
  then it waits for the non-daemon threads and returns the status (`System.exit` ends the
  process before). `Go(name, f)` starts a Go function as a daemon jrt thread; `RunnableOf(f)` is
  a `Runnable` of a Go function.
- **Start, join, sleep, yield**: `start` copies the `InheritableThreadLocal` values (through
  `childValue`) and starts the goroutine; a second `start` throws
  `IllegalThreadStateException`. `join([millis[, nanos]])` waits on `done`, a timer and the
  interrupt token; `sleep` likewise, with Java's messages (`sleep interrupted`, `timeout value
  is negative`, `nanosecond timeout value out of range`); `yield` is `runtime.Gosched`.
- **Daemon status**: a new thread is a daemon when its creator is (adopted goroutines are
  daemons, `main` is not), as Java's rule; `setDaemon` after `start` throws
  `IllegalThreadStateException`.
- **Interrupts**: the status is a token in `intr` (a channel of capacity 1). `interrupt` sends
  it without blocking; `isInterrupted` is its length; `Thread.interrupted()` receives it without
  blocking. Every interruptible wait (`Object.wait` through phase 1's `interruptChan`, `sleep`,
  `join`, `lockInterruptibly`, `Condition.await`, `Future.get`, `CountDownLatch.await`,
  `ReferenceQueue.remove`) selects on the token and consumes it when it throws
  `InterruptedException`, which is Java's "the status is cleared when the exception is
  thrown". `park` returns on it and puts it back (the status stays set, as Java's).
- **Virtual threads**: goroutines like every thread, `isVirtual` true, always daemons, unnamed
  unless the builder names them. `Thread.ofVirtual().name(prefix, start).factory()` (Agent's)
  counts the names as Java's builder does.
- **Names, priorities, ids, `toString`**: as Java's: `Thread[#21,name,5,main]`,
  `VirtualThread[#22,name]/runnable`.
- **`getStackTrace()`**: the current thread's Java frames (phase 1's frame mapping); another
  thread's stack cannot be read from Go, so it is empty.
- **The context class loader** is not there (`ClassLoader` is reworked, JAVA-SURFACE.md).

## Thread locals

- `ThreadLocal` (non-leaf: Var's dynamic bindings subclass it for `initialValue`) keeps its
  values in the current Thread's map, keyed by the `ThreadLocal` struct. The map is the
  thread's own, so no lock: `get` is a slot read and a map lookup (9.4 ns). Java's maps hold
  their keys weakly; jrt's hold them strongly, so a `ThreadLocal` dropped by the program stays
  in the maps of the threads that set it until they end (Arbace's are static).
- `withInitial(Supplier)` is `ThreadLocal$SuppliedThreadLocal`; `InheritableThreadLocal` uses a
  second map, copied by `start`.
- `ThreadLocalRandom`'s probes (`getProbe`, `advanceProbe`, `localInit`, used by
  `ConcurrentHashMap`'s counter cells) live in the Thread, with Java's constants.
  `ThreadLocalRandom.current()` waits for the translated `Random`, its superclass.

## Concurrency

**Atomics** (§8.2): `AtomicInteger`, `AtomicLong`, `AtomicBoolean` are `sync/atomic` values;
`AtomicReference` is a `Volatile[any]` with a compare-and-set. `Volatile.CompareAndSet` compares
by identity (Go's `==` on the values as `any`: pointers for Java objects) and swaps the box
pointer; if that CAS fails while the value is still identical (another store of the same
reference), it retries, so it is linearizable as Java's. Java's `AtomicInteger` and `AtomicLong`
extend `Number`, which is translated: until it joins, they embed `Object` and have `Number`'s
methods themselves (A12).

**Locks.** A lock's state is under a `sync.Mutex`; a thread that must wait takes the lock's
*gate*, a channel the next release closes, and selects on it, its interrupt token and a timer.
That gives `lockInterruptibly`, timed `tryLock` and timed `await` without polling.

- `ReentrantLock`: owner and hold count; `unlock` by another thread throws
  `IllegalMonitorStateException`; fairness is not kept (a fair lock is a non-fair one).
- Its conditions: waiters first in, first out, one channel each. `await` releases the lock fully,
  waits, takes it again uninterruptibly with its hold count, then throws
  `InterruptedException` if it was interrupted; a signal that races with the interrupt or the
  timeout wins and the interrupt status is set again, so no signal is lost (Java's
  `transferAfterCancelledWait`). `signal` without the lock throws
  `IllegalMonitorStateException`.
- `ReentrantReadWriteLock`: reentrant read holds per thread, a reentrant writer that may take
  the read lock (downgrading), and Java's non-fair rule against writer starvation: a waiting
  writer blocks new readers that hold no read lock yet. The write lock's `newCondition` throws
  `UnsupportedOperationException` (unused).
- `LockSupport`: the permit is a channel of one; `park` may return spuriously, as Java's.

**Executors.** `newCachedThreadPool` (no limit, idle workers kept 60 s),
`newFixedThreadPool` (n workers, kept, an unbounded queue), `newSingleThreadExecutor`,
`newThreadPerTaskExecutor(factory)` and `newVirtualThreadPerTaskExecutor()` (a new thread per
task). Workers are jrt threads made by the pool's `ThreadFactory` (Agent's factories name them),
so they are Java threads with their slot. A task given to `execute` that throws ends its worker,
whose thread reports the exception (Java's `ThreadPoolExecutor`); `submit` wraps the task in a
`FutureTask`, which keeps it. `shutdown`, `isShutdown`, `isTerminated`, `awaitTermination`,
`close`; `shutdownNow` (it returns a `List`) and `invokeAll`/`invokeAny` are not there.
Rejection: `RejectedExecutionException("Task ... rejected from ...")`.

**`FutureTask`**: `get` (with `ExecutionException(cause)`, `CancellationException`,
`InterruptedException`, and `TimeoutException` for the timed one), `cancel(mayInterrupt)`
(succeeds on a running task, as Java's, and interrupts its runner), `isDone`, `isCancelled`.
**`CountDownLatch`**: the count under a mutex, a channel closed at zero.
`CyclicBarrier` and `Semaphore` are not used by Arbace or the translated classes.

**References.** A `WeakReference` holds a `weak.Pointer` to the referent's header (every Java
object's first field, so its address is the object's) and the referent's type word, from which
`get` rebuilds the `any` (no allocation). With a queue, a `runtime.AddCleanup` on the referent
enqueues the reference when the referent is collected (Keyword's and Symbol's tables;
`Util.clearCache` polls). Tested with forced GCs: an unreachable referent is cleared and its
reference enqueued, a reachable one stays, the referent's Java type survives. `SoftReference`
holds its referent strongly: Go has no memory-pressure-sensitive pointers, so it is cleared only
by `clear` (Arbace's soft references are `DynamicClassLoader`'s class cache, cut, and
`CharacterName`'s cache). Go's caveat applies: an object without pointers smaller than 16 bytes
may share a tiny-allocator block and never be collected separately; interned keywords and
symbols have pointers.

**Monitors with the slot.** Phase 1's monitors are unchanged but for two things: the thread
number comes from the slot, and `MonitorEnter`/`MonitorExit` take the header by address (the
`any`'s data word: every Java object begins with its header) instead of asserting `Object_I`
and calling `Self_Object` (a change to phase 1's `monitor.clj`, two lines). A non-Java Go value
is no longer rejected there; c2g passes only Java objects.

**Memory model.** Go's `sync/atomic` operations are sequentially consistent, as Java's volatile
accesses, so atomics, `Unsafe`'s int, long and pointer slots and `Volatile` give Java's
guarantees. The locks, the gate channels and the striped locks give the happens-before edges
of Java's locks (the race detector checks the tests' plain counters guarded by them). The
weaker accesses (`lazySet`, `setRelease`, `getAcquire`, `putReferenceRelease`,
`getReferenceAcquire`, `storeFence`) are the sequentially consistent ones: stronger than Java
requires, correct. What §8.3 says of racy publication stays: a data race on an interface field
outside `Unsafe` and `Volatile` is a bug the race detector finds.

## `Unsafe` (C2G-SPEC §9.2)

- **Array offsets** are the JVM's numbers: base 16, scales 1, 2, 4, 8 and 4 for references (as
  compressed oops). Java's arithmetic on them holds (`ConcurrentHashMap`'s
  `ABASE + ((long) i << ASHIFT)`, `ArraysSupport`'s byte offsets), and `Unsafe` maps an offset
  back to the element of the Go slice, or to its bytes for the unaligned and byte accesses
  (`getLongUnaligned`, `putCharUnaligned`, `putByte`), bounds-checked
  (`ArrayIndexOutOfBoundsException`).
- **Field offsets** are keys into jrt's table of fields: `objectFieldOffset(Class, name)` finds
  `F_name` in the class's Go struct type (through embedded superclasses), and records its Go
  offset and how its Go type is accessed. The struct type comes from `RegisterGoType(C_class,
  reflect.TypeFor[C]())`, which c2g calls for the classes whose fields reach
  `objectFieldOffset` (A11).
- **Access by representation**: `int32`/`atomic.Int32` and `int64`/`atomic.Int64` slots with
  `sync/atomic`; a pointer slot (a leaf class, `String`, an array, or `atomic.Pointer[T]` for a
  volatile one) with atomic pointer operations and the pointer type's type word; an `any` slot
  (array elements) or another interface slot (`C_I`, through `reflect`) under one of 64 striped
  locks keyed by the slot's address; a `Volatile` field through its own CAS. `float32` and
  `int8` fields for `putFloat`/`putByte`.
- **Fences** are an atomic operation (Go has no weaker one); `isBigEndian` is false on both
  targets.

## `System`, `Runtime` and the host (C2G-SPEC §9.4)

**The host interface** is `jrt.Host`, one Go interface in `host.clj`: the standard streams,
files (`Open` with `os.O_*` flags, `Stat`, `ReadDir`, `Remove`, `Mkdir`, `Rename`, `Getwd`),
the clocks (`Now`, `Nanotime`), the environment (`Getenv`, `Environ`), the command line
(`Args`), `NumCPU`, `RandomBytes` (for `SecureRandom`), embedded resources (`Resource`), and
`Exit`. `OSHost` is B1a's, over `os`, `time` and `crypto/rand` (its `Resources` field takes an
`fs.FS`, such as an `embed.FS`). `SetHost` swaps it (B1b's monitor calls; the tests capture the
standard error and the exit code this way). jrt reaches the operating system nowhere else
(phase 1's `os.Stderr` in `StderrPrint` is replaced at init); the timed waits use Go's runtime
timers (`time.NewTimer`, deadlines from `time.Now`), which TamaGo's runtime provides too.

**The standard streams** are `jrt.Stdin`, `Stdout`, `Stderr` (`*HostStream`): unbuffered byte
streams over the host's, one lock each, with `Write`, `WriteString`, `Read`. `System.in`,
`out` and `err` are `InputStream` and `PrintStream` fields, classes that are translated (or are
2b's shims): when they exist, `System_err` and the others are defined over these streams, and
`StderrPrint` (now writing to `Stderr`) goes through `System.err`, so that `setErr` redirects
`printStackTrace`.

**`System`**: `nanoTime` (the host's monotonic clock), `currentTimeMillis`, `getenv(String)`,
`exit` (runs the `Runtime` shutdown hooks, then the host's `Exit`), `gc`, `lineSeparator`,
`getProperty` (both), `setProperty`, `clearProperty` with Java's checks (`key can't be null`,
`key can't be empty`), and `PropertyNames` for Go. The properties are made from the host on
first use: `java.version` 26, `java.class.version` 70.0, `os.name` Linux, `os.arch` (`amd64`,
`aarch64` as the JVM names it), separators, encodings UTF-8, `java.io.tmpdir`, `user.dir`,
`user.home`, `user.name`. `System.getProperties()` and `getenv()` return translated classes
(`Properties`, a `Map`) and come with them.

**`Runtime`**: `getRuntime`, `availableProcessors` (the host's), `freeMemory`, `totalMemory`,
`maxMemory` (Go's memory limit), `gc`, `exit`, `halt`, `addShutdownHook`. `Runtime.version()`
waits for `Runtime$Version`.

## Coverage of the edge

The members the translated JDK classes call (`.tmp/jrt/edge.edn`), for phase 2a's classes:

| class | edge | done | not done, and why |
|---|---:|---:|---|
| `Unsafe` | 39 | 38 | `objectFieldOffset(Field)` (`Random`'s `readObject`, serialization; needs 2b's `Field`, then one line over the `Class, String` form) |
| `Thread` | 10 | 7 | `<init>(ThreadGroup, ...)`, `getThreadGroup`, `setContextClassLoader`: `ForkJoinWorkerThread`'s, with `ForkJoinPool` cut (`ThreadGroup` cut, `ClassLoader` reworked) |
| `ThreadLocal`, `ThreadLocalRandom` | 2, 4 | 2, 3 | `ThreadLocalRandom.current()` (`BigInteger`'s primality tests): its superclass `Random` is translated |
| `AtomicInteger`, `AtomicLong`, `AtomicReference` | 7, 5, 3 | all | |
| `ReentrantLock`, `Condition`, `LockSupport` | 7, 3, 2 | all | |
| `Reference`, `SoftReference`, `WeakReference` | 2, 2, 1 | all | |
| `System` | 6 | 5 | `err` (a `PrintStream`: with that class) |
| `Runtime` | 2 | 2 | |

Arbace's own uses (JAVA-SURFACE.md) of these classes are all there but `Thread`'s context class
loader, `Runtime.version()`, `Runtime.exec` (cut), `System.in`/`out`/`err`,
`System.getProperties()` and `loadLibrary` (cut). `sun.misc.Signal` (the REPL's interrupt
handler) is not in phase 2a.

The manifest (`bin/jrt manifest`) now lists 29 more classes, among them the interfaces jrt
writes (`Lock`, `Condition`, `Executor`, `ExecutorService`, `Future`, `ThreadFactory`,
`Thread$UncaughtExceptionHandler`, `Thread$Builder$OfVirtual`), whose methods
`test/jrt/manifest.clj` now reads from the Go interface types.

**Stand-ins added** (`test/jrt/standins.clj`): `IllegalThreadStateException`,
`ExecutionException`, `CancellationException` (which made `IllegalStateException` non-leaf),
`TimeoutException`, `RejectedExecutionException`, and the interfaces `Runnable`, `Callable`,
`Supplier`. `TimeUnit` is a hand-written stand-in (`standin_timeunit.clj`, an enum with
`toNanos` and `toMillis`), as the generator writes exceptions and interfaces only.

## Tests

| test | what |
|---|---|
| `TestCurrentThreadPerGoroutine` | 100 goroutines: each its own adopted Thread, stable, daemon, `Thread-N`; distinct numbers below 2^22 |
| `TestThreadStartJoin` | start, run, join, timed join, a second start, `setDaemon` after start, `toString`, names, priorities, ids |
| `TestThreadLocal` | per-thread values in 8 threads, `remove`, `withInitial` (one `initialValue` per thread), `InheritableThreadLocal` |
| `TestInterrupts` | interrupted `sleep`, `Object.wait`, `join` and `park`; the status set, read, cleared by `interrupted()` and by the exception |
| `TestUncaughtExceptions` | the message without a handler (through a captured host), the thread's and the default handler, `printStackTrace` on the standard error |
| `TestRunMain` | status 1 after an uncaught exception, the message, waiting for a non-daemon thread |
| `TestThreadNumbersRecycled` | 2,000 sequential threads keep their numbers below 1,000 |
| `TestVirtualThreads` | the builder's counted names, `isVirtual`, daemon only, a started virtual thread |
| `TestAtomics` | 8 threads × 5,000: `incrementAndGet`, a `compareAndSet` loop on a long and on a reference (boxes), `AtomicBoolean` as a spin lock around a plain counter; identity CAS (an equal but different `String` does not match) |
| `TestUnsafeFields` | a class as c2g writes one: CAS on int, long and volatile int fields under contention; pointer, `atomic.Pointer`, `any`, interface and `Volatile` fields; `putFloat`, `putByte`; a superclass's field through a subclass |
| `TestUnsafeArrays` | `ConcurrentHashMap`'s `tabAt`/`casTabAt`/`setTabAt` arithmetic under contention (8 threads × 2,000 CAS increments of boxes in 16 slots: no lost update); unaligned reads, `putCharUnaligned`, `putByte`, bounds |
| `TestReentrantLock` | 8 threads × 2,000 nested lock/unlock around a plain counter; `tryLock`, timed `tryLock`, an interrupted `lockInterruptibly`, `unlock` by a non-owner |
| `TestCondition` | a bounded buffer (3 producers, 3 consumers, `notFull`/`notEmpty`); `awaitNanos` timing out with the hold count restored; an interrupted `await`; `signal` without the lock |
| `TestReadWriteLock` | 8 threads mixing reads and writes (no reader inside a write), downgrading, a timed write lock against a reader, the read unlock's message |
| `TestExecutors` | fixed pool (at most 3 at once, 3 threads, 20 futures), rejection after `shutdown`, `awaitTermination`; cached pool reusing one worker, `ExecutionException`, `TimeoutException`, `cancel(true)` interrupting the runner, `CancellationException`, a throwing `execute` task reported; per-task executor with named virtual threads |
| `TestCountDownLatch` | 4 waiters released at zero, a timed await, a negative count |
| `TestWeakReferences` | forced GCs: cleared and enqueued when unreachable, kept when reachable; the referent's type; `SoftReference`, `clear`, `enqueue`, timed `remove` |
| `TestSystem` | properties and their checks, `nanoTime`, `currentTimeMillis`, `getenv`, `availableProcessors`, `exit` through the host after a shutdown hook, the host's standard error |

Results (2026-10-08, `bin/jrt test`):

| architecture | result | time (wall, with printing) |
|---|---|---:|
| linux/amd64 | 46 tests pass | 2.0 s (6.7 s) |
| linux/arm64 (qemu-aarch64) | 46 tests pass | 17 s (22 s) |
| linux/amd64 with `-race` | 46 tests pass | 21 s (28 s) |

The concurrency tests also pass with `-count 5`, and under `-race` with `-cpu 1,2,8 -count 3`.
(Phase 1's `TestClasses` defines a class by name and cannot run twice in one process.)

## Measurements

`bin/jrt test --arch amd64 -run X -bench ... -cpu 1 -benchmem`, AMD EPYC 7763, go1.27.1,
`GOAMD64=v1`:

| operation | ns/op | allocs |
|---|---:|---:|
| `Thread.currentThread()` (the slot) | 3.4 | 0 |
| the current thread's number (phase 1's stopgap: 2,900) | 3.6 | 0 |
| `MonitorEnter` + `MonitorExit`, uncontended (phase 1: 8,900) | 14.4 | 0 |
| `ReentrantLock` lock + unlock, uncontended | 14.5 | 0 |
| `ThreadLocal.get` | 9.4 | 0 |
| `AtomicInteger.incrementAndGet` | 2.4 | 0 |
| `AtomicReference.compareAndSet`, succeeding | 35 | 1 (the 16-byte box) |
| `Unsafe.compareAndSetInt` on a field | 5.4 | 0 |
| `Unsafe.compareAndSetReference` on an `Object[]` slot (striped lock) | 13.9 | 0 |
| new `Thread`, `start`, `join` | 964 | 8 |

The slot is as §13.3 measured (3.7 ns): the linknamed function is not inlined. An uncontended
monitor is 14 ns rather than §8.1's estimate of about 10: two thread-number reads (7 ns) and two
CAS on the header (the exit must CAS: another thread may inflate the word or set the identity
hash meanwhile). Before taking the header by address it was 21 ns (an `Object_I` assertion and
an interface call each way). The tabAt path of `ConcurrentHashMap` costs a striped lock per
access (about 10 ns); a single-word representation of reference array slots would remove it,
if profiles ask.

## Decisions where the spec was silent

- The interrupt status as a channel token, consumed by the waits that throw (above).
- Thread numbers recycled, adopted goroutines' through `runtime.AddCleanup`.
- `MonitorEnter`/`MonitorExit` take the header by address.
- Locks over a mutex and gates rather than `sync.Mutex` alone (which has no owner, no timeout,
  no interruption) or a translated `AbstractQueuedSynchronizer` (JAVA-SURFACE.md decision 4).
- `SoftReference` strong; weak references through the header's address and the type word.
- Unsafe's field offsets as table keys, array offsets as the JVM's numbers.
- Executors' workers made by the factory, as Java's, so that Arbace's factories name them and
  its threads are Java threads.

## Proposed amendments (for the user's review)

- **A11** (§9.2, §5.11): c2g calls `jrt.RegisterGoType(C_class, reflect.TypeFor[C]())` in its
  init for every class whose fields reach `Unsafe.objectFieldOffset` (or every class, if
  simpler); field offsets are keys of jrt's field table, array offsets the JVM's numbers (base
  16, reference scale 4). A `Type` field of `ClassInfo` would serve as well, and reflection
  (2b) may want it.
- **A12** (§5.3): `AtomicInteger` and `AtomicLong` embed `Object`, with `Number`'s methods, until
  the translated `Number` joins; then they embed it (`Number` is non-leaf, so they become
  `Number_I` values).
- **A13** (§8.4, §11): the threads API as implemented: `CurrentThread() *Thread` (the struct;
  `Thread_CurrentThread__Thread()` is Java's, returning `Thread_I`), `RunMain(func()) int` for
  §10.6's program entry, `Go(name, func()) *Thread`, `RunnableOf(func()) Runnable`,
  `WaitNonDaemon()`; the interrupt token; `Thread_defaultHandler`.
- **A14** (§9.4): the host interface as implemented (`Host`, `HostFile`, `HostFileInfo`,
  `OSHost`, `SetHost`, `CurrentHost`, `Stdin`/`Stdout`/`Stderr`); `System.in`/`out`/`err` are
  defined with `PrintStream`/`InputStream` over these streams.
- **A15** (§9.3): the patched runtime files live in `overlay/go/runtime/` (a forms root of
  their own, outside `go/`, whose package files `bin/g2c build` would otherwise collect), are
  printed by `bin/jrt overlay`, and are checked against the toolchain by `bin/jrt overlay
  --check`; BUILD.md's overlay section can name them.
- **A16** (§8.1): `MonitorEnter`/`MonitorExit` require a Java object (the header at offset 0),
  as c2g's output guarantees; the uncontended cost is about 14 ns, not 10.

## Sources (phase 2a)

- TamaGo's go1.27.1 tree (`/root/tamago-go`, https://github.com/usbarmory/tamago-go, `VERSION`
  go1.27.1, 2026-08-28): `src/runtime/proc.go` (SHA-256 `94814182...67ca`, TamaGo's
  `GOOS=tamago` changes over upstream's), `src/runtime/runtime2.go` (`7d66ed91...66da`, as
  upstream's), converted as the overlay's forms; `src/weak`, `src/runtime/mcleanup.go`
  (`AddCleanup`'s rules); the overlay experiment `.tmp/c2g-exp/gls/` (C2G-SPEC §13.3).
- jdk26u (`/root/jdk26u`, `baf63fb`), `src/java.base/share/classes/`:
  `jdk/internal/misc/Unsafe.java` (the members, `ARRAY_*` constants as `long` bases and `int`
  scales), `java/lang/Thread.java` (messages, `toString`, daemon inheritance),
  `java/lang/ThreadLocal.java`, `java/util/concurrent/ThreadLocalRandom.java` (`localInit`,
  `advanceProbe`, `PROBE_INCREMENT`, `SEEDER_INCREMENT`), `java/util/concurrent/FutureTask.java`,
  `java/util/concurrent/ThreadPoolExecutor.java` (rejection's message, a worker ending on an
  exception), `java/util/concurrent/locks/ReentrantLock.java`,
  `ReentrantReadWriteLock.java` (the non-fair reader rule), `AbstractQueuedSynchronizer.java`
  (`await`'s interrupt modes), `java/util/concurrent/CountDownLatch.java`,
  `java/lang/System.java` (`checkKey`'s messages); the translated classes' uses of `Unsafe` in
  `.tmp/jrt/conv` (`ConcurrentHashMap`, `BufferedInputStream`, `BigInteger`, `BigDecimal`,
  `HashMap`, `Random`, `ArraysSupport`, `DecimalDigits`).
