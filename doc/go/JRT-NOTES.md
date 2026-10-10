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

All accepted 2026-10-08; C2G-SPEC names them J1-J10 (§16).

- **A1** (§5.11): `ClassInfo` gains `Go`, the Go type's qualified name, used by stack traces.
  Its function and table fields are set in an `init` function through `C_class.Info()`, not in
  the `Define` call: Go rejects the initialization cycles that Java's mutually referring classes
  make, and a function literal's body counts as a reference. `C_InstanceOf` must not refer to
  `C_class` if it is given at `Define`. *(J1; accepted 2026-10-08, folded into C2G-SPEC §5.11.)*
- **A2** (§5.2, §5.4): a non-leaf class's constructor bodies are `Ctor...(t *C, this C_I,
  args...)`, as §15.3's super call implies. A leaf class's take no `this`. *(J2; accepted
  2026-10-08, folded into C2G-SPEC §5.2, §5.4.)*
- **A3** (§5.9, §11): the array API as implemented:
  - `NewMultiArray(cls *Class, dims ...int32) *RefArray`, with `cls` the array class;
  - `Copy()` for a clone at the array's type;
  - `IndexOutOfBounds(i, n int)`;
  - `Arraycopy` (with `System_Arraycopy_O_I_O_I_I__V` calling it);
  - `Array_NewInstance_Class_I__O`. *(J3; accepted 2026-10-08, folded into C2G-SPEC §5.9,
    §11.)*
- **A4** (§7.9.6): the frame table:
  - `FrameInfo{Go, Class, Method, Flags}` and `RegisterFrames`, c2g writing `c2g_frames.go`;
  - jrt's demangler as the fallback, so c2g registers what it cannot express by names:
    lambdas (`lambda$m$0`), methods whose Java name contains `_`, frames to elide;
  - a function literal without an entry stands for its function. *(J4; accepted 2026-10-08,
    folded into C2G-SPEC §7.9.6.)*
- **A5** (§11): the additions in bold in the API table above. *(J5; accepted 2026-10-08,
  folded into C2G-SPEC §11.)*
- **A6** (§7.9.5): implicit `NullPointerException`s have a null message (the JVM's helpful
  messages are not reproduced), and the oracle's cases that record them are accepted
  mismatches. Reproducing them would need c2g to pass the expression's text to each check. *(J6;
  accepted 2026-10-08, folded into C2G-SPEC §7.9.5, §9.1 and §3.2 as V11.)*
- **A7** (§8.1): the lock word's layout as implemented. A thread number that does not fit 22
  bits inflates. *(J7; accepted 2026-10-08, folded into C2G-SPEC §8.1.)*
- **A8** (§5.8): `new Object()` is `jrt.Object_New() any`, of an unexported type; a clone's
  header is reset with `ClearHeader`. *(J8; accepted 2026-10-08, folded into C2G-SPEC §5.8.)*
- **A9** (§4.3): stand-in files `standin_*` (generated, tracked) hold jrt's stand-ins for
  translated classes until c2g's output joins, and are deleted then. *(J9; accepted 2026-10-08,
  folded into C2G-SPEC §4.3.)*
- **A10** (§7.13, §9.1): `Enum`'s promotable methods are `name`, `ordinal`, `hashCode` and
  `equals`. `clone` is `Impl_Clone__O` (final, throwing `CloneNotSupportedException`), to which
  enum classes forward. *(J10; accepted 2026-10-08, folded into C2G-SPEC §5.4, §7.13, §9.1.)*
- **BUILD.md B6**: `--print-only`, `--tests`, `--module` (BUILD.md, "Libraries and tests").
  *(Accepted 2026-10-08, marked in BUILD.md.)*

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

# Phase 2b: reflection and the remaining shims

B1a step 3, phase 2b (branch `jrt-reflect`, 2026-10-08; phase 2a, threads, concurrency,
`Unsafe`, `System` and the host, is a parallel branch): reflection over the member tables, the
member-table format c2g generates, and the edge's remaining shims (charsets, locales, `Date`,
`ClassLoader`), as hand-written Go forms in new files of `go/arbace/jrt/`.

State: the package builds and passes `go vet` for linux/amd64 and linux/arm64; all its tests
pass on amd64, on arm64 under `qemu-aarch64` and under the race detector. Phase 2b adds 6 Go
tests and 1,423 differential cases computed by the JVM. Every edge member of the classes it owns
is implemented; what the edge still lacks is phase 2a's or c2g's (Coverage).

## Structure (phase 2b)

| path | what |
|---|---|
| `reflect.clj` | the tables' value convention (`As`, `Box`, `Unbox`); `AccessibleObject`, `Executable`, `Method`, `Constructor`, `Field`, `Member`, `InvocationHandler`, `Proxy`; `Class`'s reflective methods (`getMethods` ... `forName(name, init, loader)`); `AdaptFn`; the `Class` objects of `Math`, `StrictMath`, `StringLatin1`, `StringUTF16`, `ThrowableTracer`, `foreign.Utils`; `Math.PI`, `E`, `TAU` |
| `reflect_array.clj` | `java.lang.reflect.Array` (phase 1 had `newInstance(Class, int)`) |
| `reflect_tables.clj` | **generated** (`test/jrt/tables.clj`): the member tables of jrt's hand-written classes, as c2g writes tables |
| `classloader.clj` | `ClassLoader` (minimal), the application and platform loaders; `jdk.internal.foreign.Utils.checkNonNegativeArgument` |
| `charset.clj` | `Charset`, `sun.nio.cs.Unicode`, `UTF_8`, `ISO_8859_1`, `US_ASCII`, `StandardCharsets`, `String`'s `Charset` overloads |
| `locale.clj` | `Locale`, `Locale.Category`, `DecimalFormatSymbols`, `LocaleProviderAdapter`, `LocaleResources`, `ResourceBundleBasedAdapter`, `String`'s `Locale` overloads |
| `date.clj` | `java.util.Date` |
| `standin_reflect.clj` | **generated** (`test/jrt/standins_reflect.clj`): stand-ins for the wrapper classes (`Number`, `Boolean`, `Character`, `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `Void`), `java.lang.reflect.Type` and `InvocationTargetException` |
| `reflect_test.clj`, `shims_test.clj` | the tests |
| `test/jrt/testdata_reflect.clj` | the differential data: `reflect.txt`, `charsets.txt`, `locales.txt`, `dates.txt` (made by `jrt.testdata` with the others) |

Hand-written forms: about 2,000 lines, tests 950; generated 1,700 (`reflect_tables.go` prints
as 1,200 lines of Go: 487 methods, 38 constructors, 18 fields).

Changes to phase 1's files, each small:

- `codec.clj`: `charsetOf` looks names up through `lookupCharset` (JDK 26's names and aliases;
  phase 1's list had `DEFAULT`, `US_ASCII` and `ISO-LATIN-1`, which JDK 26 rejects, and lacked
  some aliases).
- `test/jrt/standins.clj`: six more exceptions (`NoSuchMethodException`,
  `NoSuchFieldException`, `IllegalAccessException`, `InstantiationException`,
  `UnsupportedCharsetException`, `IllegalCharsetNameException`).
- `test/jrt/manifest.clj`: the new classes; interfaces' methods count as defined; a method a
  hand-written class inherits from the hand-written superclass its struct embeds counts as
  defined (Go promotes it: `Method.getParameterTypes` is `Executable`'s); an interface's
  member is referred to as `(method-expr J M)`; `Class` implements `java.lang.reflect.Type`.
- `test/jrt/testdata.clj` calls `jrt.testdata-reflect`; `bin/jrt standins` and `bin/jrt
  manifest` run the two new generators; `jrt.clj` lists the new files.

## The member tables (normative for c2g)

Per class, c2g's `c2g_classes.go` sets `C_class.Info()`'s `Methods`, `Ctors` and `Fields` in an
`init` function (A1), with the types of `class.clj`, unchanged from phase 1:

```clojure
(go/type MethodInfo (struct ^string Name ^{:tag (slice (* Class))} Params ^{:tag (* Class)} Return
                            ^int32 Modifiers ^{:tag (func [any (slice any)] [any])} Invoke))
(go/type CtorInfo (struct ^{:tag (slice (* Class))} Params ^int32 Modifiers
                          ^{:tag (func [(slice any)] [any])} New))
(go/type FieldInfo (struct ^string Name ^{:tag (* Class)} Type ^int32 Modifiers
                           ^{:tag (func [any] [any])} Get ^{:tag (func [any any])} Set))
```

**Which members.** Every public member the class declares (methods, constructors, fields,
static or not), bridges and synthetic methods included; and, for the classes in c2g's list of
declared members (C2G-SPEC §16 Q13), every member it declares. A member appears once, in the
table of the class that declares it; jrt computes inheritance (`getMethods` merges as
`java.lang.PublicMethods` does). The order is the table's (the JVM's is unspecified); jrt's
own tables sort by name and descriptor. Interfaces list their abstract, default and static
methods and their constants. An enum's `values`, `valueOf` and constants are ordinary members.

**Fields of an entry.**

- `Name`: the Java name (`"charAt"`; constructors have none).
- `Params`, `Return`, `Type`: `Class` objects, as expressions valid at `init`: `Prim_int` ...
  `Prim_void`, `C_class`, `(.ArrayClass X)` for arrays. `Params` is nil for no parameters.
- `Modifiers`: the JVM's access flags as `getModifiers` reports them: for methods
  `ACC_BRIDGE` (0x40), `ACC_VARARGS` (0x80) and `ACC_SYNTHETIC` (0x1000) included, which
  `isBridge`, `isVarArgs` and `isSynthetic` read; `native` as the class file has it; for
  fields `ACC_ENUM` and `ACC_SYNTHETIC` too. `toString` masks them as the JDK does.
- `Invoke`, `New`, `Get`, `Set`: Go function literals, **in the tables' value convention**
  (below). `Set` is nil for a final field (`Field.set` throws `IllegalAccessException`). `New`
  is nil for an abstract class (`newInstance` throws `InstantiationException` anyway).
  `Invoke` is never nil as c2g writes it; jrt throws `AbstractMethodError` for a nil one.

**The value convention** (A11). Invokers take and return values in their Go representation
(§5.1), not boxed:

- a primitive as the Go value of its type: `bool`, `int8`, `uint16`, `int16`, `int32`,
  `int64`, `float32`, `float64`; `void` as nil;
- a reference as itself (`any`; nil for null).

jrt converts before calling: it checks the receiver (instance methods: `NullPointerException`
for null, `IllegalArgumentException("object of type X is not an instance of C")`), the argument
count (`"wrong number of arguments: n expected: m"`), and each argument: a primitive parameter
gets `Unbox` (unboxing, then widening, JLS 5.1.2; null or anything else is
`IllegalArgumentException`), a reference parameter an instance check (`"argument type
mismatch"`). It boxes a primitive result with `Box` (the wrappers' `valueOf`, with their
caches). So an invoker is one call:

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
  virtually: `Base.name`'s `Method` invoked on a `Square` runs `Square`'s override.
- A primitive argument is `(assert T (aget args i))`; a reference argument
  `((inst jrt/As T) (aget args i))`, which maps nil to T's zero value; an `Object` argument is
  `(aget args i)` itself.
- A default method's invoker is the interface call; a static method of an interface `J` calls
  `J_M...`; a bridge's invoker calls the bridge's own Go method.
- A static field's `Get`/`Set` read and write the package variable `C_f` (or the constant);
  jrt runs `C_Init` before a static field access and before `newInstance` (`ClassInfo.Init`).

jrt's own tables (`reflect_tables.clj`) are generated exactly so by `test/jrt/tables.clj` from
the manifest; `reflect_test.clj` writes a small hierarchy (an interface with a constant, a
default and a static method, an abstract class with overloads and a protected method, a leaf
with a covariant override and its bridge, a varargs static) as c2g would.

## Reflection: what is implemented

The members Arbace's `Reflector`, `Compiler` (its interop analysis, `FISupport`,
`NewInstanceExpr.gatherMethods`), `RT`, `core` and the translated JDK call
(`java-surface.edn`'s runtime and REPL groups, the edge):

- **`Class`**: `getMethods`, `getMethod` (the most specific return type among same-signature
  methods, as `getMethod` picks over a bridge), `getDeclaredMethods`, `getDeclaredMethod`,
  `getFields` (the class's, then its superinterfaces' recursively, then its superclass's),
  `getField`, `getDeclaredFields`, `getDeclaredField`, `getConstructors`, `getConstructor`,
  `getDeclaredConstructors`, `getDeclaredConstructor`, `getDeclaringClass`, `getEnclosingClass`,
  `isMemberClass`, `getTypeName`, `getCanonicalName`, `getPackageName`, `isSynthetic`,
  `isAnnotationPresent`, `getGenericInterfaces`, `getGenericSuperclass`, `getEnumConstants`,
  `getClassLoader`, `forName(String, boolean, ClassLoader)`; phase 1's `getName`,
  `getSimpleName`, `getSuperclass`, `getInterfaces`, `isInterface`, `isArray`,
  `getComponentType`, `isPrimitive`, `isAssignableFrom`, `isInstance`, `cast`, `getModifiers`,
  `forName(String)`. `NoSuchMethodException` and `NoSuchFieldException` carry the JVM's
  messages (`java.lang.String.nope(int,java.lang.String)`, `C.<init>(int)`, the field's name).
  `getMethods` is computed once per class (a `sync.Map`); each call returns new `Method`
  objects, as the JVM's copies.
- **`Method`**: `invoke` (above; an exception the method throws, Go run-time errors mapped as
  `Catch` maps them, becomes an `InvocationTargetException`), `getName`, `getModifiers`,
  `getDeclaringClass`, `getParameterTypes`, `getParameterCount`, `getReturnType`,
  `getExceptionTypes` (empty), `isBridge`, `isVarArgs`, `isSynthetic`, `isDefault`, `toString`
  (the JVM's text without the throws clause), `toGenericString` (the same), `equals`,
  `hashCode`, and `AccessibleObject`'s `setAccessible`, `trySetAccessible`, `isAccessible`,
  `canAccess` (true), `isAnnotationPresent` (false).
- **`Constructor`**: `newInstance` (`InstantiationException` for an abstract class), the same
  accessors; **`Field`**: `get`, `set` (the JVM's `Can not set [static] [final] T field C.f to
  X` messages, `null value`, `IllegalAccessException` for a final field), `getType`,
  `isEnumConstant`, `toString`.
- **`java.lang.reflect.Array`**: `get`, `set`, the eight `getX` and `setX`, `getLength`,
  `newInstance(Class, int...)`, with HotSpot's order of checks and messages (`Argument is not
  an array`, `Argument is not an array of primitive type`, `argument type mismatch`, `array
  element type mismatch`, an `ArrayIndexOutOfBoundsException` without message).
- **`Modifier`** is plain Java and translated (the closure has `Modifier.java`); jrt keeps its
  `Acc...` constants.
- **`InvocationHandler`** is an interface; **`Proxy.newProxyInstance`** throws
  `UnsupportedOperationException`: classes cannot be made at run time. `Reflector.boxArg`'s
  adaptation of an `IFn` to a functional interface goes through `jrt.AdaptFn(c, f)`, which is
  `ClassInfo.FromFn` (A13).
- **`ClassLoader`**: the application loader (`getSystemClassLoader`, `getClassLoader` of
  Arbace's classes) and the platform loader (its parent); `loadClass` reads the registry
  without initializing. `getResource*` are the host's (phase 2a).

Arbace's `Reflector` itself is tested in Go (`rGetMethods`, `rMatch`: its `getMethods`,
`isCongruent`, `paramArgTypeMatch` and `Compiler.subsumes`, transcribed): arity, staticness,
the bridge set aside, interface methods with `Object`'s added, overloads by argument class
(`over(String)` over `over(Object)` for a String).

## Charsets, locales, dates

**Charsets.** `Charset.forName` (with `Charset.lookup`'s checks: `Null charset name`,
`IllegalCharsetNameException` for an illegal name, `UnsupportedCharsetException` for an unknown
one), `forName(name, fallback)`, `isSupported`, `defaultCharset` (UTF-8, JEP 400), `name`,
`displayName`, `aliases` (as Go strings), `contains`, `canEncode`, `isRegistered`, `equals`,
`hashCode`, `compareTo`, `toString`; the classes `sun.nio.cs.UTF_8` (extending `Unicode`),
`ISO_8859_1`, `US_ASCII` with their `INSTANCE`s; `StandardCharsets.UTF_8`, `ISO_8859_1`,
`US_ASCII`; `new String(byte[], Charset)`, `new String(byte[], int, int, Charset)`,
`getBytes(Charset)` over phase 1's coding. jrt has these three charsets only (V9);
`StandardCharsets.UTF_16*` are left out. Encoders, decoders and buffers (`java.nio`) are cut.

**Locales** (root locale data only, as decided for `Formatter`):

- `Locale`: `ROOT`, `ENGLISH`, `US`, `UK`, `of` and the deprecated constructors (language
  lower case, country upper case), `getLanguage`, `getCountry`, `getVariant`, `toString`,
  `toLanguageTag` (`und`, a malformed variant as `x-lvariant-V`), `equals`, `hashCode`
  (BaseLocale's), `clone`.
- **The default locale is `en_US`** (`getDefault()`, `getDefault(Category)`): `Formatter`
  takes its `Locale.US` path for grouping, which needs no `DecimalFormat` (cut), and a JVM
  started in an English, US environment has the same default.
- `Locale.Category` is an enum (`DISPLAY`, `FORMAT`).
- `DecimalFormatSymbols`: the root locale's symbols (`0 , . - % ‰ # ; ∞ NaN E`), the currency
  `$`/`USD` for the United States and `¤`/`XXX` otherwise.
- `LocaleProviderAdapter.getAdapter`, `getResourceBundleBased`, `getLocaleResources` and
  `LocaleResources.getNumberPatterns` (`#,##0.###`, `¤ #,##0.00`, `#,##0%`) exist for
  `Formatter`'s declarations; the path that uses them needs `DecimalFormat`, which is cut.
- `String.toLowerCase(Locale)` and `toUpperCase(Locale)` apply the root locale's mapping for
  every locale: no Turkish, Azerbaijani or Lithuanian rules (a deviation for those locales,
  which jrt cannot name in the JDK's way anyway).

**`Date`** (the user's decision: `#inst` over a small `Date` on Go's `time`):

- `Date()`, `Date(long)`, the deprecated `Date(y, m, d[, h, mi[, s]])`, `getTime`, `setTime`,
  `before`, `after`, `equals`, `hashCode`, `compareTo`, `clone`, `toString` (`Thu Jan 01
  00:00:00 GMT 1970`), `toGMTString`, the deprecated getters `getYear` ... `getSeconds`,
  `getDay`, `getTimezoneOffset` (0) and `Date.UTC`.
- The default time zone is GMT, so the deprecated getters and `Date.UTC` are UTC.
- The calendar is Java's: Julian before the cutover 1582-10-15, Gregorian from it.
  - `Date.UTC` normalizes as jdk26u's `Date.normalize` does: out-of-range fields count on,
    the year's calendar is chosen first and the other one when the result falls across the
    cutover, the year 1582 as `GregorianCalendar` (Gregorian when the date is on or after
    the cutover, else Julian).
  - Printed years are years of the era (`toString` of 1 BC prints 1).
- **What the `arbace.instant` rework needs** (a Go-build variant, c2g's, later):
  - **Reading** `#inst`: the regex and `validated` as they are, then `Date.UTC(years - 1900,
    months - 1, days, hours, minutes, seconds)`, minus the offset (sign × (hours × 60 +
    minutes) × 60,000), plus nanoseconds ÷ 10^6. This equals the JVM's
    `GregorianCalendar` construction (tested on 15 timestamps, the cutover's included).
  - **Printing** (`print-method`, `print-dup` of `Date`): `#inst "yyyy-MM-ddTHH:mm:ss.SSS-00:00"`
    from `getYear() + 1900` (zero-padded to 4 digits; already the year of the era), `getMonth()
    + 1`, `getDate`, `getHours`, `getMinutes`, `getSeconds`, and `getTime()` mod 1000
    (floored). `Date.InstantText` (Go) is the same text, for jrt's own use.
  - The `Calendar` and `Timestamp` readers and printers are cut: `read-instant-calendar` and
    `read-instant-timestamp` are dropped by the variant.
  - `ThreadLocal`, `SimpleDateFormat`, `TimeZone` and `GregorianCalendar` are no longer
    needed by it.

## Coverage of the edge

Phase 2b's classes, the members the translated JDK calls (`.tmp/jrt/edge.edn`), all
implemented:

| class | edge | done |
|---|---:|---:|
| `Class` | 9 | 8 (`getResourceAsStream`: the host, phase 2a) |
| `reflect.Array` | 1 | 1 (and the 20 Arbace uses) |
| `Charset`, `sun.nio.cs.UTF_8`, `ISO_8859_1` | 2, 1, 1 | all |
| `String` (`Charset` and `Locale` overloads) | 4 | 4 |
| `Locale`, `Locale$Category` | 6, 1 | all |
| `DecimalFormatSymbols` | 6 | 6 |
| `LocaleProviderAdapter`, `LocaleResources`, `ResourceBundleBasedAdapter` | 3, 1, class | all |
| `Date` | 1 (`<init>()`) | 1 |
| `ClassLoader` (rework) | 1 (`getSystemClassLoader`) | 1 |
| `jdk.internal.foreign.Utils` | 1 | 1 |

`bin/jrt manifest` reports 7 edge members of the hand-written classes undefined, none of them
phase 2b's: `Class.getResourceAsStream`, `System.err`, `getProperty`, `lineSeparator`,
`nanoTime` (phase 2a), `String.format` (after c2g, over the translated `Formatter`),
`String.lines` (`Stream`: cut).

Arbace's reflective uses (`java-surface.edn`, runtime and REPL): all of `Class`'s 23 and 19
except `getAnnotation`, `getModule`, `getPackage`, `getNestHost`, `getRecordComponents`,
`getTypeParameters` (annotations, modules and generics are cut, §12); all of `Method`'s,
`Executable`'s, `Field`'s, `Constructor`'s and `Array`'s except `Method.getAnnotation` (used for
`WarnBoxedMath`: the `Compiler` variant drops it), `getGenericParameterTypes` and
`getTypeParameters`.

**The rework items** (JRT-SOURCES.md):

- `ClassLoader`: minimal, above.
- `VarHandle` and `MethodHandles.byteArrayViewVarHandle` (`jdk.internal.util.ByteArray`,
  `ByteArrayLittleEndian`): no jrt code; c2g's variants of the two classes write the
  accesses as shifts.
- `SerializedLambda` (`$deserializeLambda$` of `Comparator`, `Map`, `TreeMap`),
  `ConstantDescs` and `DynamicConstantDesc` (`describeConstable` of the wrappers): unreached,
  so c2g's reachability pass stubs the methods and the references disappear (the user's
  decision for the closure); no jrt code.
- `ObjectMethods.bootstrap` (a record's `equals`, `hashCode`, `toString`): c2g generates them
  (§7.13); no jrt code.

**Left to phase 2a** (the host and `SharedSecrets`): `SecureRandom` (seeded by the host),
`JavaObjectInputStreamAccess.checkArray` and `JavaUtilCollectionAccess` (with
`SharedSecrets`), `PrintStream`, `OutputStreamWriter`, `FileOutputStream`, `File`. `System`'s
`Class` object `System_class` is also 2a's: once it exists, `bin/jrt manifest` generates
`System`'s member table.

**`Objects`, `Arrays`** are translated (both in the closure): no jrt code.

## Tests (phase 2b)

| test | what |
|---|---|
| `TestReflectLookups` | the hand-written hierarchy: `getMethods`' merge (overrides, the bridge, the default method, an interface's static method not inherited, `Object`'s methods), `getMethod`'s choice of the covariant method over the bridge, `getDeclaredMethods` with a protected member, `Reflector`'s `getMethods` and `matchMethod`, varargs and modifiers, `getFields`' order, constructors, `Method.equals` |
| `TestReflectInvoke` | 23 cases: virtual dispatch through a superclass's and an interface's `Method`, a default method, widening (`Integer` to `long`, `Short` to `int`), mismatches, null arguments, a static varargs method, receivers of another class and null, a Go run-time error wrapped in `InvocationTargetException`, constructors (widening, a thrown exception, an abstract class), fields (widening, final, null, another class, statics, an interface's constant) |
| `TestBoxing` | `Box`'s caches, `Unbox`'s widenings and refusals |
| `TestClassLoaders` | the loaders, `loadClass`, `getClassLoader`, `forName` of an array class, `checkNonNegativeArgument` |
| `TestReflectAgainstJVM` | 279 cases of `reflect.txt`: `getMethods` of 17 jrt classes (the signatures the JVM lists that jrt's tables hold), `getMethod`, `getConstructor`, `getField`, `getFields`, `getConstructors` (their `toString`), 61 `Method.invoke`, `newInstance`, `Field.get`/`set`, `Class`'s names, modifiers, loaders and enum constants, 38 `Array` operations |
| `TestCharsetsAgainstJVM` | 148 cases: names and aliases, the checks, encoding and decoding (malformed input), `Charset`'s methods |
| `TestLocalesAgainstJVM` | 113 cases: `Locale`'s methods, the default, `DecimalFormatSymbols`, case mapping |
| `TestDatesAgainstJVM` | 883 cases: 536 instants (fixed ones around the cutover, years 1 BC to 292,278,994, the extremes of `long`, 500 random) each with `toString`, `toGMTString`, the getters, `hashCode`, the `#inst` text (`InstantText` and the getters' composition); 332 `Date.UTC` (the cutover, overflowing fields, 300 random); 15 `#inst` readings through `Date.UTC` |

The JVM's helpful `NullPointerException` messages are accepted as the bare class (A6). The
JVM's charset `UTF-16` is skipped (V9).

Results (2026-10-08), the whole package: linux/amd64 2.0 s, linux/arm64 under `qemu-aarch64`
17 s, linux/amd64 with `-race`: all pass.

## Decisions (phase 2b)

- **Boxing lives in jrt, not in the invokers** (A11): one `Unbox` and one `Box` instead of a
  conversion per parameter of every generated invoker, and the evaluator can call an invoker
  with Go values directly (no boxing on hinted interop).
- **The wrappers are stand-ins** (`standin_reflect.clj`) until c2g translates jdk26u's
  `Integer` and the others; jrt's `Box`/`Unbox` use their final names (`F_value`,
  `Integer_ValueOf_I__Integer`), and `valueOf` caches as jdk26u's do, so that
  `(identical? (Integer/valueOf 5) (Integer/valueOf 5))` holds through reflection too.
- **Annotations**: none kept, except that `Class.isAnnotationPresent(FunctionalInterface)` is
  answered by `ClassInfo.FromFn != nil`, which `Compiler.FISupport.maybeFIMethod` needs.
- **`getExceptionTypes` is empty** and `toString` has no throws clause: `NewInstanceExpr`'s
  `exclasses` only feed the class writer, which the evaluator does not have.
- **`getGenericInterfaces`** returns the interfaces as `Class` objects in a `Type[]`; generic
  types are cut, so `HashMap.comparableClassFor` finds no `ParameterizedType` and orders tree
  bins of comparable keys by `tieBreakOrder`, not `compareTo` (iteration order unchanged).
- **`Proxy`** is unsupported; `AdaptFn` replaces it for functional interfaces.
- **Access** is not checked: the tables hold what Clojure may reach; `canAccess` is true,
  `setAccessible` records a flag, a final field's `Set` is nil.
- **`Object`'s table** lists its 9 public methods and its protected `clone` (a declared member
  `NewInstanceExpr.gatherMethods` offers `deftype` and `reify`).

## Proposed amendments (phase 2b, for the user's review)

All accepted 2026-10-08; C2G-SPEC names them R11-R19 (§16), apart from phase 2a's A11-A16.

- **A11** (§5.11): the tables' value convention above: invokers take and return Go
  representations; jrt's `Method.invoke`, `Constructor.newInstance` and `Field.get`/`set` unbox
  and widen arguments and box results. §5.11's "boxing as Field.get/set do" in the invokers is
  replaced. `Invoke` is never nil; `Set` is nil for final fields; `New` nil for abstract classes.
  *(R11; accepted 2026-10-08, folded into C2G-SPEC §5.11, §10.1.)*
- **A12** (§5.11): the normative format of "The member tables": the modifiers as
  `getModifiers` reports them (bridge, varargs, synthetic, native kept), `Params` nil for no
  parameters, class expressions valid at `init`, one entry per member in its declaring class's
  table. *(R12; accepted 2026-10-08, folded into C2G-SPEC §5.11.)*
- **A13** (§5.11, §10.1): `Proxy.newProxyInstance` throws `UnsupportedOperationException`; the
  `Reflector` variant's `boxArg` calls `jrt.AdaptFn(paramType, arg)` (the interface's
  `FromFn`) instead. c2g sets `FromFn` on every interface `Compiler.FISupport` may adapt (the
  `@FunctionalInterface` ones), and `isAnnotationPresent(FunctionalInterface)` reads it. *(R13;
  accepted 2026-10-08, folded into C2G-SPEC §5.11, §7.11, §10.1, §12.)*
- **A14** (§11, A5): jrt's new API names: `As`, `Box`, `Unbox`, `AdaptFn`, `Method.Info`,
  `Constructor.Info`, `Field.Info`, `Charset.Aliases`, `Date.InstantText`; the classes of the
  structure table; `Math_PI`, `Math_E`, `Math_TAU` (and `StrictMath`'s) as constants. *(R14;
  accepted 2026-10-08, folded into C2G-SPEC §11.)*
- **A15** (§16 Q13): c2g's list of classes whose declared members the tables hold, so far:
  `java.util.Random` (`getDeclaredField("seed")`, with phase 2a's `Unsafe`). `Object`'s
  protected `clone` is jrt's. *(R15; accepted 2026-10-08, folded into C2G-SPEC §5.11, §16 Q13.)*
- **A16** (§12): `getExceptionTypes` empty, `Method.toString` without throws clause, no
  annotations but `FunctionalInterface` through `FromFn`, generic reflection returning plain
  `Class` objects. *(R16; accepted 2026-10-08, folded into C2G-SPEC §5.10, §12.)*
- **A17** (JAVA-SURFACE.md decision 2): the default locale is `en_US` with the root locale's
  data, and `String`'s `Locale` overloads use the root mapping for every locale. *(R17; accepted
  2026-10-08, folded into C2G-SPEC §12.)*
- **A18** (V9): jrt's charsets are UTF-8, ISO-8859-1 and US-ASCII; `Charset.forName` throws
  `UnsupportedCharsetException` for the others. *(R18; accepted 2026-10-08, folded into C2G-SPEC
  §12, with V9 widened.)*
- **A19** (A9): `standin_reflect.clj` (the wrappers, `Type`, `InvocationTargetException`)
  joins the stand-in files deleted when c2g's output joins the build. *(R19; accepted 2026-10-08,
  folded into C2G-SPEC §4.3.)*

## Sources (phase 2b)

- jdk26u (`/root/jdk26u`, openjdk/jdk26u at `baf63fb`), `src/java.base/share/classes/`:
  `java/lang/PublicMethods.java` (`MethodList.merge`), `java/lang/Class.java`
  (`privateGetPublicMethods`, `getMethod`'s search, `methodToString`, the field search),
  `java/lang/reflect/Executable.java` (`sharedToString`), `Method.java`, `Field.java`,
  `Modifier.java` (`toString`, `methodModifiers`), `InvocationTargetException.java`,
  `jdk/internal/reflect/DirectMethodHandleAccessor.java` and
  `DirectConstructorHandleAccessor.java` (the argument checks' messages),
  `FieldAccessorImpl.java` (`getSetMessage`), `java/util/Date.java` (`normalize`, `UTC`,
  `toString`, `toGMTString`, `getCalendarSystem`), `java/util/GregorianCalendar.java`
  (`computeTime`'s cutover rule), `java/util/Locale.java`, `sun/util/locale/BaseLocale.java`
  (`hashCode`), `java/util/Formatter.java` (its locale uses),
  `jdk/internal/foreign/Utils.java`.
- The JVM (JDK 26 as Arbace runs on it) for the differential data: charset aliases, locale
  hash codes and tags, `DecimalFormatSymbols`, the reflection messages, the `#inst` texts.
- Arbace: `arbace/lang/Reflector.clj`, `Compiler.clj` (`FISupport`, `QualifiedMethodExpr`,
  `NewInstanceExpr.gatherMethods`, `subsumes`), `arbace/instant.clj`.

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
`maxMemory` (Go's memory limit), `gc`, `exit`, `halt`, `addShutdownHook` (the hooks run at
`System.exit` and, since amendment FS5, when `RunMain`'s main and the non-daemon threads end). `Runtime.version()`
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

All accepted 2026-10-08; C2G-SPEC names them T11-T16 (§16), apart from phase 2b's A11-A16.

- **A11** (§9.2, §5.11): c2g calls `jrt.RegisterGoType(C_class, reflect.TypeFor[C]())` in its
  init for every class whose fields reach `Unsafe.objectFieldOffset` (or every class, if
  simpler); field offsets are keys of jrt's field table, array offsets the JVM's numbers (base
  16, reference scale 4). A `Type` field of `ClassInfo` would serve as well, and reflection
  (2b) may want it. *(T11; accepted 2026-10-08, folded into C2G-SPEC §5.11, §9.2.)*
- **A12** (§5.3): `AtomicInteger` and `AtomicLong` embed `Object`, with `Number`'s methods, until
  the translated `Number` joins; then they embed it (`Number` is non-leaf, so they become
  `Number_I` values). *(T12; accepted 2026-10-08, folded into C2G-SPEC §5.3.)*
- **A13** (§8.4, §11): the threads API as implemented: `CurrentThread() *Thread` (the struct;
  `Thread_CurrentThread__Thread()` is Java's, returning `Thread_I`), `RunMain(func()) int` for
  §10.6's program entry, `Go(name, func()) *Thread`, `RunnableOf(func()) Runnable`,
  `WaitNonDaemon()`; the interrupt token; `Thread_defaultHandler`. *(T13; accepted 2026-10-08,
  folded into C2G-SPEC §7.9.6, §8.4, §10.6, §11.)*
- **A14** (§9.4): the host interface as implemented (`Host`, `HostFile`, `HostFileInfo`,
  `OSHost`, `SetHost`, `CurrentHost`, `Stdin`/`Stdout`/`Stderr`); `System.in`/`out`/`err` are
  defined with `PrintStream`/`InputStream` over these streams. *(T14; accepted 2026-10-08, folded
  into C2G-SPEC §9.4, §11.)*
- **A15** (§9.3): the patched runtime files live in `overlay/go/runtime/` (a forms root of
  their own, outside `go/`, whose package files `bin/g2c build` would otherwise collect), are
  printed by `bin/jrt overlay`, and are checked against the toolchain by `bin/jrt overlay
  --check`; BUILD.md's overlay section can name them. *(T15; accepted 2026-10-08, folded into
  C2G-SPEC §9.3, §16 Q19.)*
- **A16** (§8.1): `MonitorEnter`/`MonitorExit` require a Java object (the header at offset 0),
  as c2g's output guarantees; the uncontended cost is about 14 ns, not 10. *(T16; accepted
  2026-10-08, folded into C2G-SPEC §8.1 and §3.2 as V13.)*

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

# Phase 2C: the rest of the JDK closure

B1a step 4, phase 2, part C (branch `c2g-2c`, 2026-10-09; parts A, B and D are parallel
branches, C2G-NOTES.md): every stand-in replaced by a translated class, the standard streams,
files, `String.CASE_INSENSITIVE_ORDER`, `String`'s regex methods, BigInteger and BigDecimal
checked, and the `java.util.regex` port run on the oracle's regex corpus.

## The closure, grown (`bin/jrt-convert`)

`bin/jrt-convert` now translates **223 files** (215 of jdk26u, 8 generated), all compiled from
their class forms to javac's class shapes (630 classes), against the measured closure's 184:

- **31 jdk26u files added** (`g2c.jrt-sources/added-sources`, outside the measured closure,
  each for a reason recorded there):
  - the 26 classes jrt had as stand-ins without sources (all plain Java: the exceptions
    `AbstractMethodError`, `ArrayStoreException`, `CloneNotSupportedException`,
    `IllegalAccessException`, `IllegalMonitorStateException`, `IllegalThreadStateException`,
    `InstantiationException`, `InterruptedException`, `NegativeArraySizeException`,
    `NoClassDefFoundError`, `NoSuchFieldException`, `NoSuchMethodException`,
    `StackOverflowError`, `InvocationTargetException`, `UnsupportedEncodingException`,
    `IllegalCharsetNameException`, `UnsupportedCharsetException`, `CancellationException`,
    `ExecutionException`, `RejectedExecutionException`, `TimeoutException`; the interfaces
    `Cloneable`, `Runnable`, `Serializable`, `java.lang.reflect.Type`, `Supplier`);
  - `PrintStream`, `Closeable`, `Flushable`, `AutoCloseable` (the standard streams);
  - `java.math.SignedMutableBigInteger` (`BigInteger.modInverse`, `modPow` with a negative
    exponent).
- **jrt's own Java, `overlay/jdk/java.base/`** (new; KIND `overlay` in `sources.txt`): classes
  whose jdk26u sources need what is cut (`java.nio`'s encoders, decoders and channels, the VM's
  file natives), written for jrt from their documented APIs (Arbace's own, EPL; LICENSE.md),
  converted and checked like the rest:
  - `java.io.FileDescriptor` (jdk26u's public members and the package-private `set` and
    `close` the streams use), `FileInputStream`, `FileOutputStream` (unbuffered, over jrt's
    file table; the constructors taking a `File` are stubs, `File` being outside the world);
  - `java.io.OutputStreamWriter` and `InputStreamReader`: jrt's three charsets (R18), with the
    JDK's REPLACE behaviour: a malformed or unmappable character as `?` (a surrogate pair as one
    character), a high surrogate kept for the next write and written as `?` at `close`;
    UTF-8's malformed sequences as U+FFFD with the lengths of jdk26u's `sun.nio.cs.UTF_8`'s
    decoder, an incomplete sequence at the end as one U+FFFD; a read blocks only while it has
    decoded nothing; historical encoding names (`UTF8`, `ISO8859_1`, `ASCII`);
  - `jdk.internal.jrt.HostFiles` (the file table: handles 0, 1, 2 the host's standard streams,
    the others `Host.Open`'s files; descriptors registered by identity), `StandardStreams`
    (`System.in`, `out`, `err` as `System.initPhase1` makes them: a `BufferedInputStream`, and
    autoflush `PrintStream`s over a 128-byte `BufferedOutputStream`, in `stdout.encoding` and
    `stderr.encoding`), `CaseInsensitiveComparator` (`String.CASE_INSENSITIVE_ORDER`, comparing
    as `compareToIgnoreCase`; c2g registers it as `java.lang.String$CaseInsensitiveComparator`).
- **Go-build variants of JDK classes, `overlay/jdk/variants/`** (read by c2g with the JDK
  input): `CaseFolding`'s key array, built through a stream (cut), built by a loop.
- jrt's own files are compared with javac in one chunk of `bin/class-forms-check`: they use one
  another's members that jdk26u's classes of the same names lack.

## The 26 stand-ins: decisions

All 26 translated from jdk26u (above): each is plain Java with nothing jrt must do by hand, and
the stand-in shapes were exactly the translated ones, so jrt's hand-written code needed no
change. With them **c2g replaces all 64 of jrt's stand-ins**: in a c2g program no `standin_*`
class remains hand-written. The stand-in files stay for jrt's own build (`bin/jrt build`,
`bin/jrt test` without c2g), which has no translated classes; nothing in them is meant to stay.
None was cut.

## New in jrt

- `files.clj`: `HostFiles`' natives (`open0` with the JVM's reasons in `FileNotFoundException`'s
  message, `No such file or directory`, `Permission denied`, `Is a directory`; `read0`, `write0`,
  `available0` (a file's bytes after its position, 0 for the standard input), `skip0` (a file
  seeks, the standard input reads), `close0`; errors as `IOException`).
- From c2g's support (C2G-NOTES.md, phase 2C), when their classes are translated: `System_in`,
  `System_out`, `System_err` and `System.setIn`/`setOut`/`setErr`; `StderrPrint`
  (`printStackTrace()`, uncaught exceptions) through `System.err`; `String_CASE_INSENSITIVE_ORDER`;
  `String.split` (both), `replaceAll`, `replaceFirst`, `matches` over the translated
  `java.util.regex`.
- `bin/jrt build|test --prog DIR` builds or tests a c2g program root (`bin/c2g --tests --out
  OUT` writes `OUT/prog`): jrt's tests with the stand-ins replaced.
- `TestReflectAgainstJVM`'s expectations are generated twice (`test/jrt/testdata_reflect.clj`):
  `methods` for jrt's own build (the hand-written tables), `methods-c2g` for a c2g program,
  where the translated supertypes' tables add their public methods whose types are in the world
  (the bridges `compareTo(Object)` through `Comparable`, `append(C)Appendable` through
  `Appendable`; not `chars()`/`codePoints()`, whose `IntStream` is cut). The test takes the one
  that applies (a c2g program registers `jdk.internal.jrt.StandardStreams`).

## Tests (phase 2C)

| what | result |
|---|---|
| `bin/jrt test`, jrt alone, amd64 and arm64 | all pass |
| `bin/jrt test --prog` on c2g's JDK slice (all 64 stand-ins replaced), amd64 and arm64 | all pass (phase 1: 53 of 54) |
| fixture `FxStreams` (`bin/c2g-check fixtures`): the writers and readers in the three charsets with malformed and unmappable input, one char at a time and in bulk, `PrintStream`, `PrintWriter`, files (write, append, read, `available`, `skip`, closed streams, the three `FileNotFoundException` messages), `System`'s streams, `CASE_INSENSITIVE_ORDER` (sorting through the translated `TimSort`) | 13 of 13, amd64 and arm64, as the JVM's |

## Not done

- `java.text.Normalizer` (regex `CANON_EQ`) and `CharacterName` (`\N{name}`) need the JDK's
  resource data (ICU's `nfc.nrm` read through `java.nio.ByteBuffer`, `uniName.dat` through
  `java.util.zip`): resources and those classes are a later step (`getResourceAsStream` stays
  undefined).
- `File`, so `new FileInputStream(File)` and `PrintStream(File)` are stubs.

# Phase 2B (step 5): jrt's surface for the REPL

B1a step 5, phase 2, part B (branch `eval-2b`, 2026-10-09; EVAL-NOTES.md, "A split for phase
2"): the members of jrt's hand-written classes that the REPL's interop reaches but the runtime
did not need, and the JDK classes the oracle's forms corpus found missing.

## Math and StrictMath

`arbace.math` did not load: jrt's `Math` lacked the trigonometric, exponential and hyperbolic
functions (145 of the corpus's failing forms). jrt's `Math` and `StrictMath` now define **every
public member of JDK 26's classes** (109 each; `bin/jrt manifest`'s coverage lists none
missing; the member tables follow from the manifest).

- **`fdlibm.clj`** (new) ports the rest of `java.lang.FdLibm` (openjdk/jdk26u at `baf63fb`,
  `src/java.base/share/classes/java/lang/FdLibm.java`; phase 1 had `Log`, `Cbrt`, `Pow`):
  `Sin`, `Cos`, `Tan` with `__kernel_sin`, `__kernel_cos`, `__kernel_tan`, `RemPio2` and
  `KernelRemPio2` (only the precision `RemPio2` uses, `prec` 2), `Asin`, `Acos`, `Atan`,
  `Atan2`, `Hypot`, `Exp`, `Log10`, `Log1p`, `Expm1`, `Sinh`, `Cosh`, `Tanh`,
  `IEEEremainder` with `__ieee754_fmod`. As written, in phase 1's style: every floating-point
  product through `fmul` (no fused multiply-add on arm64), the constants by their bits (Java
  writes them as hexadecimal floats), Java's `>>>` on an `int` as `fdUshr`, `(int)` of a
  double as `D2I`, unsigned comparisons through `uint32`.
- `math.clj`: `Math`'s functions of FdLibm call the port (`Math.sin` is `StrictMath.sin`, as
  in Java's source), `toRadians` and `toDegrees` (one product with the JDK's constants),
  `clamp(double...)`, `divideExact`, `floorDivExact`, `ceilDivExact`, `multiplyFull`,
  `nextAfter(float, double)`, `nextDown(float)`, `powExact`, `unsignedMultiplyExact`,
  `unsignedPowExact` (as jdk26u's `Math.java`, with its exception messages); and all of
  `StrictMath`, delegating as `StrictMath.java` does (`copySign` with a NaN sign as positive;
  `ceil`, `floor` and `rint`, StrictMath's own Java code in the JDK, give the same results as
  `Math`'s, being exact).

**Decision: jrt's `Math` is `StrictMath`** on every architecture, as phase 1 decided for `log`,
`pow` and `cbrt`. HotSpot on amd64 replaces `Math.sin`, `cos`, `tan`, `exp`, `log`, `log10`,
`pow`, `cbrt`, `sinh` and `tanh` by intrinsic stubs (Intel's libm) whose results differ from
`StrictMath`'s in the last bits for some arguments; `asin`, `acos`, `atan`, `atan2`, `log1p`,
`expm1`, `cosh`, `hypot` and `sqrt` are not replaced (identical). Measured on 200,000 random
arguments per function (JDK 26.0.2.1, amd64, `-Xint` the same): differing results for sin 1.8%,
cos 1.9%, tan 2.2%, exp 5.3%, log 1.8%, log10 3.6%, pow 9.7%, cbrt 8.3%, sinh 19%, tanh 18%.
The JVM's own results therefore depend on its platform (HotSpot on arm64 has other
intrinsics), and Java allows it (1 ulp of error, 2.5 for the hyperbolic functions). jrt gives
`StrictMath`'s results, the same on amd64 and arm64. Matching amd64 HotSpot bit for bit would
mean porting its stubs (`macroAssembler_x86_*.cpp`, table-driven), not done.

**Tests** (`test/jrt/testdata_numbers.clj`, `math_test.clj`): each of the 16 functions on about
9,700 arguments (special values, arbitrary bits, and random values in the ranges the
algorithms branch on: `[-2, 2]`, `[-800, 800]`, magnitudes 1e-310 to 1e300, near multiples
of π/2 for `KernelRemPio2`), the two-argument ones on about 10,900 pairs; `StrictMath` must
equal the JVM's bit for bit (it does, for all of them, on amd64 and on arm64 under qemu), and
`Math` must be within one ulp of the JVM's `Math` (two for `sinh`, `cosh`, `tanh`), the
differences counted and logged: sin 145, cos 154, tan 147, exp 314, log10 260, sinh 657 and
tanh 491 cases (at most 2 ulps), the others none. The integer additions (`divideExact` ...
`unsignedPowExact`, `multiplyFull`) and the float ones (`nextDown`, `nextAfter(float,
double)`, `StrictMath.copySign`) join `TestInts` and `TestFloats`. TestDoubles: 572,188
cases. `bin/jrt test`: passes on amd64 and arm64.

**The oracle**: `forms/harvest/math` 149 of 149 (was 4); the forms corpus 9,509 of 9,652 (was
9,364; amd64).

## Dates: Calendar, Timestamp, Instant

B1a step 5, phase 2, part B (branch `eval-2b-time`, 2026-10-09): the 37 forms of the oracle's
`forms/inst.clj` that need `java.util.Calendar`, `java.sql.Timestamp` or `java.time.Instant`
(EVAL-NOTES.md, "Failure causes"). Decision 7 stands: `#inst` reads and prints over jrt's small
hand-written `Date`; the other classes join the world around it.

**Per class, and why:**

| class | how | why |
|---|---|---|
| `java.time.Instant` | translated from jdk26u (`added-sources`), with `DateTimeException`; a Go-build variant (`overlay/jdk/variants/Instant.clj`) for `toString` and `now()` | plain Java over two fields; what it needs of the rest of `java.time` (`DateTimeFormatter` for `toString` and `parse`, `Clock` for `now`, `ChronoField`/`ChronoUnit` for the temporal accessors) stays outside the world as operation-level stubs. `toString` calls jrt's own `jdk.internal.jrt.TimeText.instant`, `ISO_INSTANT`'s text (10,000-year periods, signed years beyond 9999, the fraction in groups of three); `now()` is `ofEpochMilli(currentTimeMillis)` (millisecond precision) |
| `java.util.Date` | hand-written (decision 7), now **non-leaf** (`Date_I`, `Impl_` methods, `Ctor_...`; manifest) | `Timestamp` extends it. Its implementations call `getTime()` virtually where the JDK's do (`equals`, `hashCode`, `compareTo`, `before`, `after`); `clone` copies the whole object (a subclass's `CloneShallow`). `toInstant` and `from(Instant)` name the translated `Instant`, which jrt's own build cannot: c2g writes their table entries (`arbace/c2g/out.clj`, `support-table-forms`, as for `String.format`), so reflection (the REPL) finds them; translated code calling them would be a missing operation (none does) |
| `java.sql.Timestamp` | translated from jdk26u (`src/java.sql/share/classes/java/sql/Timestamp.java`), with `java.sql.Date` (its `toString` uses `Date.formatDecimalInt`) | plain Java over `Date`'s deprecated fields. Both are another module's files: `bin/jrt-convert` copies them into the java.base tree (KIND `share:java.sql` in `sources.txt`, `added-module-sources` in `test/g2c/jrt_sources.clj`), and javac's `--patch-module java.base` takes them. `java.sql.Date`'s Go name would collide with jrt's `Date`: c2g's rename table names it `Sql_Date` |
| `java.util.Calendar`, `GregorianCalendar`, `TimeZone`, `sun.util.calendar.ZoneInfo` | jrt's own Java (`overlay/jdk/java.base/`), behaving as the JDK's | jdk26u's `Calendar`, `GregorianCalendar`, `TimeZone` and `SimpleTimeZone` are 9,600 lines and need `sun.util.calendar` (12 files, 4,700 lines), the time zone database (`ZoneInfoFile` reads `tzdb.dat`) and the locale providers (`CalendarDataUtility` for the first day of the week): too much for fixed-offset zones. jrt's are 2,300 lines |

**jrt's calendar** (`Calendar.java`, `GregorianCalendar.java`, `TimeZone.java`, `ZoneInfo.java`):

- Zones are fixed offsets without daylight saving time. `TimeZone.getTimeZone` knows the
  zero-offset IDs (`GMT`, `UTC`, `UCT`, `Universal`, `Zulu`, `Greenwich`, `GMT0` and their `Etc/`
  forms), `Etc/GMT+h` and `Etc/GMT-h` (POSIX signs), and the custom IDs, normalized as the JDK
  does (`GMT+5` is `GMT+05:00`, `GMT+0` is `GMT+00:00`); any other ID is GMT, as the JDK falls
  back for an unknown one. A deviation: region IDs (`Europe/Paris`) are GMT too. The default
  zone is GMT, as jrt's `Date`. Display names are the root locale's English (`GMT`, `UTC`,
  `Greenwich Mean Time`, `Coordinated Universal Time`, a custom ID itself, else `GMT+hh:mm`).
- The calendar is the JDK's: Julian before the cutover (default 1582-10-15, settable with
  `setGregorianChange`), the fields computed in the zone, the time computed lazily from the
  fields as the JDK resolves them (`selectFields`: day of month, week of month, day of week in
  month, day of year, week of year with day of week; hour of day or hour and AM/PM), leniently or
  not (the JDK's `IllegalArgumentException` messages), `add`, `roll`, `getActualMaximum`,
  `getWeekYear`, the week numbering around the cutover and the JDK's quirks with it (a BC year's
  week maxima are those of the AD year of the same number). `setTimeZone` before the time is
  computed reinterprets the fields in the new zone, which `arbace.instant`'s
  `construct-calendar` relies on.
- The week parameters are en_US's (Sunday, 1) for every locale, display names English. Not
  there: `setWeekDate`, `getWeeksInWeekYear`, `toZonedDateTime`, serialization.
- `String.format`'s `%t` conversions now work over a `Calendar`, a `Date` or a `long` (the
  translated `Formatter`'s `printDateTime` reaches `Calendar.getInstance`, `get`,
  `getTimeZone`), except those that need `DateFormatSymbols` (month and day names, AM/PM:
  `%tB`, `%tA`, `%tp`, `%tc` ...), which is outside the world.

**The `arbace.instant` variant** (`arbace/lang/go/ns/instant.clj`): `read-instant-calendar`,
`read-instant-timestamp`, `construct-calendar` and `construct-timestamp`, and the `Calendar`
printer are `arbace/instant.clj`'s again (the printer through `String.format`'s `%t`); the
`Timestamp` printer writes the date with the variant's `Date` arithmetic and the nanoseconds
(`arbace/instant.clj` uses `SimpleDateFormat` in a proxy'd `ThreadLocal`). `read-instant-date`
and the `Date` printer are unchanged. `arbace/c2g/embed.clj` no longer cuts `java.sql.Timestamp`
always: it is in the world, and core's `when-class` guard finds it.

**Checks:**

- A differential harness on the JVM (scratch, `.tmp/caltest`): jrt's four Java classes compiled
  with `--patch-module java.base` replace the JDK's in a JVM and run the same programs as the
  JDK's: 31,000 lines (zone IDs, the 17 fields of 12,000 times in four zones around the cutover
  and in BC years, times from fields, lenient overflow, `add` and `roll` of every field with
  random amounts, the actual maxima, week-based field resolution with random week parameters,
  `getWeekYear`, `%t` output, `ISO_INSTANT` against `Instant.toString`) are equal.
- c2g's fixture `test/c2g/fixtures/FxDates.clj` (`bin/c2g-check fixtures`): the same ground in
  the Go program, against the JVM's recording: 8 of 8 on amd64 and arm64.
- `bin/jrt test` (amd64, arm64): a `Date` subclass in Go (`shims_test`: virtual `getTime`,
  clone of the whole object).
- The oracle's `forms/inst.clj`: 190 of 231 before, **227 of 231** after, on amd64 and arm64
  (`qemu-aarch64`); the four left are not dates (two helpful `NullPointerException` messages,
  a `ClassCastException` message, `UUID/nameUUIDFromBytes`). The whole forms corpus on amd64:
  9,364 to 9,401 of 9,652, no regression.

**Sources.** Translated: openjdk/jdk26u at `baf63fb`,
`src/java.base/share/classes/java/time/Instant.java`, `java/time/DateTimeException.java`,
`src/java.sql/share/classes/java/sql/Timestamp.java`, `java/sql/Date.java`. Studied and partly
transcribed into jrt's own Java (each file's header names the parts; LICENSE.md: GPL version 2
with the Classpath Exception): `java/util/Calendar.java` (`selectFields`, the stamps),
`java/util/GregorianCalendar.java` (`computeTime`, `getFixedDate`, `add`, `roll`,
`getWeekYear`, `getActualMaximum`, the week numbering in `computeFields`, the field tables),
`java/util/TimeZone.java` (`parseCustomTimeZone`), `sun/util/calendar/ZoneInfoFile.java`
(`toCustomID`), `java/time/format/DateTimeFormatter.java` (`InstantPrinterParser`) and
`java/time/LocalDate.java` (`toString`'s year).

**Proposed amendments** (numbered W for the merge): merged into EVAL-NOTES.md's phase 2B list
as B1, B2, B3 and B7, all accepted 2026-10-09 and folded, renamed S1, S2, S3 and S7 (C2G-SPEC
§16; these W numbers are not C2G-SPEC's W1-W6):

- **W1 (C2G-SPEC §4.4, Collisions) A rename table entry**: `java/sql/Date` is `Sql_Date` (jrt's
  `java.util.Date` is `Date`). *Folded into C2G-SPEC §4.4 as S1.*
- **W2 (JRT-SOURCES.md, the tool) Other modules' files**: `added-module-sources` lists files of
  other modules (`src/MODULE/share/classes/...`) compiled into the java.base tree; `sources.txt`
  gives them KIND `share:MODULE`. *Folded into JRT-SOURCES.md as S2.*
- **W3 (C2G-SPEC §5.3; the manifest) `java.util.Date` is a non-leaf hand-written class.**
  *Folded into C2G-SPEC §5.3 as S3.*
- **W4 (LICENSE.md) Code transcribed from jdk26u in jrt's own Java**: `Calendar.java`,
  `GregorianCalendar.java`, `TimeZone.java` and `TimeText.java` under `overlay/jdk/` hold parts
  of jdk26u's code under the GPL version 2 with the Classpath Exception (LICENSE.md now says so
  and holds the text); the alternative, the user's call, is to rewrite those parts from the
  documented behaviour alone, as the overlay's other files are. *Decided: kept (S7), as
  LICENSE.md records it.*

## The rest of part B: the JDK surface the REPL and Clojure's suite reach

The other causes of EVAL-NOTES.md's "Failure causes" that are jrt's, and the blockers of
Clojure's test suite on the Go build (EVAL-NOTES.md, "Phase 2C") that are jrt's surface. Per
item, what was done; the oracle and suite results are in EVAL-NOTES.md, "Phase 2B".

### The closure, grown again (`test/g2c/jrt_sources.clj`, `added-sources`)

`bin/jrt-convert` now translates 340 files (was 223), all converted and compiled to javac's
shapes but four of known kinds (below):

| files | for |
|---|---|
| `java/util/SequencedCollection`, `SequencedSet`, `SequencedMap`, `ReverseOrderDequeView`, `ReverseOrderSortedSetView`, `ReverseOrderSortedMapView`, `jdk/internal/util/NullableKeyValueHolder` | JDK 21's sequenced collections: the closure's `List`, `Deque`, `SortedSet`, `LinkedHashSet` and `LinkedHashMap` implement them; `LinkedHashMap`'s views are `SequencedSet`s (`(seq (LinkedHashMap.))` threw) |
| `java/lang/constant/Constable`, `ConstantDesc` | `supers`/`bases`/`ancestors` of `Long`, `String` (the wrappers and `String` implement them; their methods name `MethodHandles.Lookup`, outside the world: `resolveConstantDesc` does not exist in Go) |
| `java/util/stream/*` (39 files), `java/util/function/*` (43), `Optional`, `OptionalInt`, `OptionalLong`, `OptionalDouble`, the three `*SummaryStatistics`, `StringJoiner`, `Spliterator`, `PrimitiveIterator`, `EnumSet`, `RegularEnumSet`, `JumboEnumSet`, `EnumMap`, `java/util/Tripwire`, `java/util/random/RandomGenerator`, `java/util/concurrent/CountedCompleter`, `ConcurrentMap` | streams: `String.lines`, `chars`, `codePoints`, `Collectors`, core's `stream-reduce!` and the like, `.stream`/`.parallelStream` of Clojure's collections; the functional interfaces (`IntSupplier`: the `atoms` tests); `EnumSet`/`EnumMap` (`Collectors`' characteristics, `StreamOpFlag`); `RandomGenerator` (`Collections.shuffle`) |
| `java/net/URI`, `URISyntaxException` | `uri?` (the `predicates` tests) |
| `java/util/concurrent/CyclicBarrier`, `BrokenBarrierException` | the `delays` tests (over jrt's `ReentrantLock`) |

Decision (streams): **translated whole** rather than left cut (D6's default) or rewritten:
java.util.stream is plain Java (28,700 lines), and with it come the REPL's interop over
streams and four of the suite's namespaces. The cost: the executable grows from 38 to 58 MB
(streams and the functional interfaces are most of it; the rest of the step is small), `bin/c2g
--program` from about 45 to 55 s, the Go build by about a minute. Parallel streams run on jrt's
fork-join pool (below). Left out by variants: `Stream.gather`'s evaluation (`GathererOp.evaluate`:
local classes nested in local classes over captured variables, which c2g does not translate
yet) and `Gatherers.mapConcurrent` (a local subclass of jrt's hand-written, leaf `FutureTask`);
both throw `UnsupportedOperationException`.

**Go-build variants** (`overlay/jdk/variants/`, new): `Random` (its static initializer took the
offset of its private field `seed` through `getDeclaredField`, which jrt's tables of public
members cannot answer, then `Unsafe.objectFieldOffset(Field)`; the variant asks
`objectFieldOffset(Class, String)`: this blocked `clojure.data.generators`, so six of the
suite's namespaces); `URI` (its static initializer's `SharedSecrets.setJavaNetUriAccess`, cut);
`ReferencePipeline.toList` and `Collectors.toUnmodifiableList` (`SharedSecrets`'
`JavaUtilCollectionAccess`, which `ImmutableCollections` would register: an unmodifiable list
over the array, `List.copyOf`); `GathererOp`, `Gatherers` (above); `UUID.nameUUIDFromBytes`
(MD5 from the host: Go's `crypto/md5`, a native `UUID.md5`).

**`bin/jrt-convert`**: `J2C_PATCH_ALL` (j2c's `jdk` conversion, `arbace/j2c/main.clj`): each
chunk of packages sees the other chunks' sources on its patch path, its own first. jrt's own
Java in `jdk.internal.jrt` is used across packages (`java.io`), and with more files the chunks
split them apart (javac: "package jdk.internal.jrt does not exist"). Opt-in, so
`bin/j2c-check --jdk` is unchanged. And `known-differences`: files whose shapes differ from
javac's in known ways count as converted (the exit status): a lambda constructing a local
class passes the class's captured variables in capture order, javac in reverse
(`Collectors`, `Gatherers`, `MatchOps`: the synthetic lambda method's parameter order), and an
`InnerClasses` entry javac's stack map frames name (`Nodes`; CONVERTER-NOTES.md's kind).

### jrt (hand-written)

- **Charsets** (`charset.clj`, `codec.clj`): `UTF-16`, `UTF-16BE`, `UTF-16LE` (classes
  `sun.nio.cs.UTF_16`, `UTF_16BE`, `UTF_16LE`, extending `Unicode`; JDK 26's names and aliases),
  `StandardCharsets.UTF_16*`; coding as jdk26u's `sun.nio.cs.UnicodeDecoder`/`UnicodeEncoder`
  with `String`'s REPLACE (a byte-order mark read by `UTF-16` at the start, big-endian
  without one; written by `UTF-16`'s encoder before a non-empty string; an unpaired surrogate
  as U+FFFD, both ways; the bytes left at the end one U+FFFD). Tested against the JVM
  (`charsets.txt`: the encode and decode cases of the three new charsets, 15 more byte
  sequences). The overlay's `InputStreamReader` and `OutputStreamWriter` still code UTF-8,
  ISO-8859-1 and US-ASCII only and now refuse the others (`UnsupportedOperationException`)
  instead of decoding them as UTF-8. (V9: jrt has six charsets; amendment B6 of EVAL-NOTES.md,
  accepted and folded into C2G-SPEC §12 and V9 as S6.)
- **`ForkJoinTask`, `ForkJoinPool`** (`forkjoin.clj`, rewritten; both now in the manifest, so
  in the REPL's tables): a pool of parallelism P runs a task given from outside (`invoke`,
  `execute`, `submit`) in a worker thread of its own, and a task forked in a worker in a new
  worker thread (a jrt thread: a goroutine with its `Thread`, which knows its pool:
  `inForkJoinPool`, `getPool`) while fewer than P − 1 forked tasks run, else in the forking
  thread at once. A task runs once: whoever claims it first (`fork`'s worker, `join`,
  `invoke`) runs it, the others wait on a channel closed at completion; a task whose `exec`
  returns false (a `CountedCompleter`) is done when completed explicitly (`quietlyComplete`,
  `trySetThrown`). `adapt` (Callable, Runnable, Runnable and result; the adapters' class names
  are the JDK's), `invokeAll(t1, t2)`, `cancel`, `complete`, `completeExceptionally`, `get`'s
  `ExecutionException`; `commonPool` (processors − 1). Not the JDK's work-stealing: no
  `ForkJoinWorkerThread`, no `managedBlock`, `getException` gives the exception itself (the
  JDK may rewrap it for the joining thread). Reducers' `fold` and parallel streams run
  in parallel (`TestForkJoin`: a fold-shaped sum in a pool of 4 used 2 to 4 workers at once, never
  more; `bin/jrt test --race` clean). (EVAL-NOTES.md's B5: C2G-SPEC §8.4, S5.)
- **`String`**: `indent`, `stripIndent`, `translateEscapes` (jdk26u's algorithms over lines
  split as `lines()` splits them; tested against the JVM on 17 multi-line strings),
  `contentEquals(StringBuffer)`, `String(byte[], int)`, and the interfaces `Constable`,
  `ConstantDesc` (stand-ins, markers only, in jrt's own build). c2g writes the members that
  name translated classes (`out.clj`, `support-forms`, as for `format`): `join(CharSequence,
  Iterable)`, `formatted`, `lines` (over an `ArrayList`'s stream: sequential, not jdk26u's lazy
  spliterator), `describeConstable`, and, for every hand-written leaf class implementing a
  translated interface, forwarders to the default methods it does not define (`chars`,
  `codePoints` of `CharSequence` on `String`, `StringBuilder`, `StringBuffer`). (C2G-SPEC §11,
  §5.4, §9.1: S3, S4.)
- **`StringBuilder`, `StringBuffer`**: `insert(int, double|float)`, `insert(int, CharSequence,
  int, int)`, `insert(int, char[], int, int)` (with the JDK's checks); both now have all of
  their JDK 26 public members.
- **`Thread`**: `MIN_PRIORITY`, `NORM_PRIORITY`, `MAX_PRIORITY`, `dumpStack`,
  `startVirtualThread`. Left: `getState` (`Thread.State` is outside the world),
  `getThreadGroup`, `enumerate`, `activeCount`, `getAllStackTraces`, `sleep`/`join` of a
  `Duration`, `ofPlatform`.
- **Atomics**: the other memory orders (`getOpaque`, `setOpaque`, `getPlain`, `setPlain`,
  `setRelease`, `weakCompareAndSet*`, `compareAndExchange*`; Go's atomics are sequentially
  consistent, so all are the same operations). `AtomicInteger` and `AtomicLong` now embed
  `Number` (amendment T12: `(pos? (AtomicInteger. 1))` casts to `Number`; C2G-SPEC §5.3,
  S3). Left: the functional ones (`updateAndGet`, `accumulateAndGet` ...).
- **`Class`**: `asSubclass`, `componentType`, `arrayType`, `descriptorString`,
  `forPrimitiveName`, `getNestHost`, `newInstance` (the deprecated one), `isAnonymousClass`,
  `isLocalClass`, `isHidden`, `isSealed` (false), `getEnclosingMethod`,
  `getEnclosingConstructor` (null).
- **c2g-written table entries** (`support-table-forms`): `Throwable.printStackTrace(PrintStream)`
  and `(PrintWriter)` (the trace's text printed), `System.getenv()` (the host's environment,
  unmodifiable).
- `JavaLangAccess.getEnumConstantsShared` and `join` (`access.clj`; `EnumSet`, `StringJoiner`),
  `Unsafe.weakCompareAndSetInt`.
- `ClassLoader.getResourceAsStream` (c2g's entry) finds a resource in the directories of
  `ARBACE_PATH` after the embedded ones (`host.clj`, `ResourceOrPath`), as `RT.load` does:
  `repl/source-fn` of a namespace loaded from there.

**Coverage of the hand-written classes' public members** (the JDK's, by reflection, against the
manifest; `.tmp/missing.clj`-style count): `Math`, `StrictMath` 109 of 109, `StringBuilder` 42
of 42, `StringBuffer` 57 of 57; `String` 84 of 104 in the manifest, and of the other 20 the
REPL's tables have 14 through c2g's entries (`format`, `formatted`, `join(..., Iterable)`,
`lines`, `describeConstable`, the regex methods, `CASE_INSENSITIVE_ORDER`) or the interface's
(`chars`, `codePoints`); missing: `compareToFoldCase`, `equalsFoldCase`,
`splitWithDelimiters`, `transform`, `resolveConstantDesc`, `UNICODE_CASEFOLD_ORDER`. `Thread`
40 of 56 (left: above); `Class` 56 of 81 (left: annotations, modules, packages, nest members,
record components, type parameters, signers, `getResource*`, `toGenericString`).

### c2g and the class forms compiler (found on the way)

- **Bridges with a source supertype** (`arbace/classes/analyze.clj`, `erased-params`): a
  supertype method's class type variables are substituted by the subclass's view, so they are
  bounded as the subclass bounds them. With `Map` from source (c2g's world), `Map.put(K, V)`'s
  own scope bounded `K` by `Object` over `EnumMap`'s `K extends Enum<K>`, and `EnumMap` got no
  bridge `put(Object, Object)`: `Map.put` on an `EnumMap` ran `AbstractMap.put` (it throws).
  The class forms check compiles against the JDK's classes by reflection, which carry no such
  scope, so the bug did not show there. `bin/gate` passes (the stages are unchanged).
  (classes/SPEC.md §6, COMPILER-NOTES.md amendment 15; S8.)
- **A bridge narrowing a generic return** casts (`decls.clj`, `derived-body`):
  `Spliterators$EmptySpliterator$OfDouble.trySplit()` returns the `Spliterator` of its
  superclass's erased `T_SPLITR` as a `Spliterator.OfDouble`.
- `java.util.stream.Tripwire` is `Stream_Tripwire` in Go (the rename table; `java.util.Tripwire`
  has the plain name).
- `C2g_CastArray` throws `ClassCastException` with HotSpot's message (`jrt.ClassCast`), not
  `Class.cast`'s (`(longs (int-array [1]))`).

# Semaphore (step 5 follow-up)

Branch `smalls` (2026-10-09): `java.util.concurrent.Semaphore`, which Clojure's pprint tests
need (`test_pretty.clj`'s `future-unfilled` blocks in `(.acquire (Semaphore. 0))`); without it
the suite's `pprint` namespace did not load. Phase 2B had tried translating
`AbstractQueuedSynchronizer` (EVAL-NOTES.md, "Phase 2B").

**Why c2g left `AbstractQueuedSynchronizer$ConditionObject` untranslated.** jrt's
`ReentrantLock_ConditionObject` (`locks.clj`) registers the JDK's name for a `ReentrantLock`'s
conditions, `java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject`, so that
`(class (.newCondition (ReentrantLock.)))` answers what the JVM answers. c2g's world therefore
counted that class as jrt's (its scan of jrt's `ClassInfo` names) and did not translate it, but
named its Go type by the name derived from the Java name, `AbstractQueuedSynchronizer_ConditionObject`,
which nothing defines: AQS's public members that take a `ConditionObject` (`owns`, `hasWaiters`,
`getWaitQueueLength`, `getWaitingThreads`, stubs since unreached) did not compile. Neither the
closure nor the inner-class handling was at fault. The general fix is c2g's
(`arbace.c2g.model/go-name`): a class jrt provides and c2g does not translate is named by the
Go name jrt gives it (its class variable's name less `_class`). Eight other jrt classes
register a JDK name under another Go name (`ThreadPoolExecutor` as `threadPool`,
`AbstractStringBuilder` as `sbuf` ...); no translated code names them yet, so the output is
otherwise unchanged (but for gensym counters). EVAL-NOTES.md's amendment Y1.

**Decision: a hand-written `Semaphore`, not `AbstractQueuedSynchronizer` translated.** Measured
with the fix, AQS, `AbstractOwnableSynchronizer` and `Semaphore` translated: the closure grows
by 3 files, the program builds (58.59 MB), and `Semaphore` then fails at run time on jrt's
missing `Unsafe.putIntOpaque`, `getAndBitwiseAndInt`, `weakCompareAndSetReference` (and
`Unsafe.park`); `ConditionNode` is unavailable (`ForkJoinPool.ManagedBlocker`, not in jrt's
pool); and c2g reports `AbstractQueuedSynchronizer$Node.waiter` as a two-word race field: AQS
reads a node's `waiter` (a `Thread`, an interface value in Go) in `signalNext` while its owner
clears it, a benign race in Java and a torn interface read in Go (C2G-SPEC §8.3). The AQS
condition methods would also take jrt's `ReentrantLock` condition, a different class under the
same name. jrt's locks are hand-written for these reasons (JAVA-SURFACE.md decision 4, phase
2a's "Decisions"), and `Semaphore` joins them, as `CountDownLatch` did:

- `executor.clj`: `Semaphore` (leaf; `Serializable`) as the locks are: the permits (an `int`,
  possibly negative, as Java's) under a `sync.Mutex`; a thread that must wait takes the gate,
  which every release opens, and tries again; `waiting` counts the waiters. `acquire(int)`,
  `acquireUninterruptibly` (an interrupt leaves the status set), `tryAcquire` (now, or timed and
  interruptible; a timeout of zero or less tries once, after the interrupt check, as
  `tryAcquireSharedNanos`), `release(int)` (Java's `Error("Maximum permit count exceeded")` on
  overflow), `availablePermits`, `drainPermits` (also of negative permits, as Java's),
  `isFair`, `hasQueuedThreads`, `getQueueLength`, `toString` (`...[Permits = n]`); a negative
  count throws `IllegalArgumentException`. Fairness is not kept (a fair semaphore behaves as a
  non-fair one), as for `ReentrantLock`. In the manifest (`test/jrt/manifest.clj`; `bin/jrt
  manifest` counts 18 of 21 JDK members): every public member; not the protected
  `reducePermits` and `getQueuedThreads`.
- Tests (`concurrent_test.clj`, `TestSemaphore`): the counts and messages, a negative start,
  mutual exclusion of 8 threads over one permit (a plain counter, clean under `--race`), an
  interrupted `acquire`, an `acquireUninterruptibly` that keeps the interrupt. `bin/jrt test`
  passes on amd64 (and `--race`) and arm64.
- Speed (`BenchmarkSemaphore`, amd64): 21 ns per uncontended acquire and release, against 17 ns
  for `ReentrantLock`'s lock and unlock. The translated AQS would be a CAS on the state
  uncontended, about the same; it was not measured further, as it does not run without the
  `Unsafe` additions above.
- Cost: the executable grows by about 30 KB (58.41 MB with `Semaphore` alone).

With it and Dyn's hand-written interfaces (EVAL-NOTES.md, "Phase 2B follow-up"), the suite's
`pprint` namespace loads and passes 470 of its 474 assertions.

# Sockets (go-net)

Branch `go-net` (2026-10-09; the user's decision of that day, reversing D6's cut of sockets,
JAVA-SURFACE.md decision 6 for `arbace.core.server`): `java.net`'s `ServerSocket`, `Socket`,
`InetAddress`, `InetSocketAddress` and what they need in the Go build, over Go's `net` package
as a host interface; then `arbace.core.server` (the socket REPL, `prepl`, `io-prepl`,
`remote-prepl`, `start-server`, `stop-server`) and the start of servers from the
`arbace.server.*` system properties, as on the JVM.

## Translated, hand-written, and why

**Decision: jdk26u's plain Java translated, the platform `SocketImpl` written for jrt, the OS in
Go behind a host interface.** `ServerSocket`, `Socket`, `SocketImpl`, `InetAddress`,
`Inet4Address`, `Inet6Address`, `InetSocketAddress`, the socket options and the exceptions are
plain Java over two seams: `SocketImpl.createPlatformSocketImpl` (jdk26u: `sun.nio.ch.NioSocketImpl`,
which needs `java.nio`'s channels, the poller and the VM's natives) and the name service
(`Inet6AddressImpl`'s JNI natives over `getaddrinfo`). Translating the plain Java keeps the
JVM's behaviour where it is decided (the state checks and messages of `Socket` and
`ServerSocket`, literal parsing in `IPAddressUtil`, `InetAddress`'s printing, equality, the
resolver's ordering policy and its cache), and the seams are small. Hand-writing `Socket` and
`InetAddress` (the alternative) would have meant re-deciding all of that; translating
`NioSocketImpl` would have brought `java.nio` in. The added files: JRT-SOURCES.md, "The closure
as grown".

- **`jdk.internal.jrt.HostSocketImpl`** (overlay/jdk, jrt's Java): the platform `SocketImpl`
  (`PlatformSocketImpl`), after `NioSocketImpl`: its states (`Socket not created`, `Not
  connected`, `Socket closed`, `Already bound` ...), the legacy read behaviour (-1 after the
  end, `Connection reset` remembered), the read lock and write lock, the `SO_TIMEOUT` of reads
  and accepts (`Read timed out`, `Accept timed out`, `Connect timed out`), the streams, the
  options by `SocketOptions` number and by `StandardSocketOptions`, `shutdownInput` and
  `shutdownOutput`. Differences: a server socket listens when it is bound (the host has no
  unbound sockets; `ServerSocket.bind` always binds then listens, so this is not observable but
  in the backlog, which is the host's); a client socket's `bind` is recorded and used by the
  connect (its local port is the requested one until then); options set before the host's
  socket exists are kept and applied when it does; urgent data is not supported
  (`supportsUrgentData` false). It transcribes `NioSocketImpl`'s code where it keeps it
  (LICENSE.md).
- **`jdk.internal.jrt.HostNet`** (overlay/jdk): the natives, static and over primitives, arrays
  and `String` only (C2G-SPEC §9.1), on a table of handles as `HostFiles`' (phase 2C): listen,
  accept, connect, read, write, available, close, shutdown, the local and remote addresses and
  ports, options; the name service (lookup, reverse lookup, the host's name, whether it has
  IPv4 or IPv6). A native reports an error as a negative kind with the text, and
  `HostNet.exception` makes the JVM's exception of it: `BindException`, `ConnectException`,
  `NoRouteToHostException`, `SocketTimeoutException`, `UnknownHostException`, `Socket closed`,
  `Connection reset`, else `SocketException`.
- **`go/arbace/jrt/net.clj`**: the natives and **`NetHost`**, the network as a host gives it: an
  interface of its own, beside `Host` (`host.clj` is unchanged), that a `Host` may implement
  (jrt asks `CurrentHost()` at each use); a host without it has no network and every native
  fails (`Network not available`), which is what B1b's box has until its monitor gives one.
  `OSHost` implements it over Go's `net` (the pure Go resolver: `/etc/hosts`, then DNS) and
  `syscall` for the options (`getsockopt`/`setsockopt` on the descriptor, `TIOCINQ` for
  `available`). Go's defaults that differ from the JVM's are undone: no TCP keep-alive on
  dialed and accepted connections, no `TCP_NODELAY`. A blocked accept or read ends when another
  thread closes the socket (Go's `net.ErrClosed`), with the JVM's `Socket closed`.
- **The JVM's texts.** An `errno`'s text is the C library's `strerror`, and this machine's JVM
  is built on musl: `Address in use`, `Connection refused`, `Connection reset by peer`,
  `Broken pipe`, `Host is unreachable` ... (net.clj's table; glibc's JVMs say `Address already in
  use`). An unknown host is `host: Name does not resolve` (musl's `gai_strerror`).

**Variants** (`overlay/jdk/variants/`):

| class | what |
|---|---|
| `SocketImpl` | `createPlatformSocketImpl` makes a `HostSocketImpl` |
| `Socket` | the state bits and the streams' installation through `Unsafe` (a compare-and-set loop for `getAndBitwiseOr`, `compareAndSetReference`) instead of `VarHandle`s; a client's impl is the platform's itself, not wrapped in a `SocksSocketImpl` (no proxies); the streams record no JFR events |
| `ServerSocket` | `implAccept(Socket)` without the `DelegatingSocketImpl` step (no SOCKS or HTTP-tunnel impls; c2g has no cast for a pattern-matching `instanceof` of a cut class) |
| `InetAddress` | the static initializers without the native library, `SharedSecrets` and the native `init`; IPv4 and IPv6 availability from the host; the built-in resolver only (no `ServiceLoader`); the cache a map of `CachedLookup`s expiring as the JVM's default policy without a security manager (30 s, failures 10 s), checked when used (the JVM's `ConcurrentSkipListSet` of expiries is outside the world), without the one-lookup-per-host lock; `PlatformResolver` without `Blocker` |
| `Inet4Address`, `Inet6Address` | no native `init`; `Inet6Address(String, byte[])` without `NetworkInterface` (outside the world: null in the JDK there); `Inet6AddressHolder.getHostAddress` without the scope's interface (a dropped field) |
| `Inet4AddressImpl`, `Inet6AddressImpl` | the natives over `HostNet`; `isReachable` throws `UnsupportedOperationException`; the loopback address without asking `NetworkInterface` whether it is bound |
| `jdk.internal.util.Exceptions` | `jdk.includeInExceptions` from the system property, defaulting to JDK 26's `java.security` value `hostInfoExclSocket` (security properties are not in the Go build); merged with go-file's variant of the same class, which took the default alone |
| `IPAddressUtil` | `parseBsdLiteralV4` walks the text by an index instead of a `java.nio.CharBuffer` (outside the world): IPv4 literal checks, `ofPosixLiteral` |
| `URLStreamHandler` (go-file's `URL.clj`) | its `hashCode` and `hostsEqual` variants, which compared host names while `InetAddress` was outside the world, are removed: the JDK's code, resolving the host, as on the JVM |

`java.net.Proxy` stays outside the world, a cut class named `Net_Proxy` in Go (c2g's rename
table: jrt's `java.lang.reflect.Proxy` is `Proxy`). `NetworkInterface`, `URL`, `Proxy`, the
SOCKS and HTTP impls, the resolver providers and `isReachable` stay out.

## `System.getProperties()` and `JAVA_TOOL_OPTIONS`

- **`System.getProperties()`**: a `Properties` holding a copy of the properties (jrt keeps them in
  its own table, where `setProperty` writes; a change to the copy does not reach
  `System.getProperty`). c2g writes it (`C2g_SystemGetProperties`, out.clj) and its member
  table entry when `Properties` is translated; `Properties` came with `Hashtable` and
  `Dictionary` (its superclasses, added).
- **The servers' start.** The JVM's `RT.doInit` requires `arbace.core.server` and calls
  `start-servers` with `System.getProperties()`; so does the Go build's
  (`arbace/lang/go/RT.clj`), through the reflection tables for `getProperties`. With step 6's
  image of prepared namespaces the load costs about 20 ms (0.36 s against 0.38 s for `-e` with
  and without a `require` of it; from the sources it took 0.5 s of 4.7 s, and a first version
  loaded it only when a server was asked for).
- **Where the properties come from.** The Go executable has no `java` launcher to take `-D`
  options. jrt reads **`JAVA_TOOL_OPTIONS`**, as the JVM does at its start: its `-Dname=value`
  options become system properties (`tooloptions.clj`, split as HotSpot's
  `Arguments::parse_options_buffer` splits: white space outside quotes, `'` and `"` grouping,
  quotes removed; other options ignored). So one setting starts a server on both builds:

      JAVA_TOOL_OPTIONS='-Darbace.server.repl="{:port 5555 :accept arbace.core.server/repl}"' target/arbace-go/amd64/arbace
      JAVA_TOOL_OPTIONS='-Darbace.server.repl="{:port 5555 :accept arbace.core.server/repl}"' bin/arbace

  The JVM prints `Picked up JAVA_TOOL_OPTIONS: ...` on its standard error; the Go executable
  does not (NT2).

## `arbace.core.server`

Embedded in the executable (embed.clj no longer leaves it out), with one namespace variant,
`arbace/lang/go/ns/core/server.subst.clj`: `prepl` sets no `DynamicClassLoader` as the thread's
context loader (one loader in the Go build, as the REPL's `main.subst.clj`). Nothing else
changes: `start-server`, `stop-server`, `stop-servers`, `repl`, `prepl`, `io-prepl`,
`remote-prepl`, `parse-props` and `start-servers` are the JVM's code.

## Tests

| what | result |
|---|---|
| `bin/jrt test` (`net_test.clj`: `TestSockets`, `TestSocketErrors`, `TestSocketOptions`, `TestLookup`, `TestNoNetwork`, `TestToolOptions`) | all pass: amd64 (with the rest of jrt's tests) and arm64 under `qemu-aarch64` |
| `bin/net-check` (new; `test/net/client.clj`, a client on the JVM Arbace): a socket REPL session (forms, output, `*err*`, errors, a reader error, namespaces, `*session*`) and an `io-prepl` session, each against a JVM server and a Go server started from `JAVA_TOOL_OPTIONS`; 8 concurrent sessions; `remote-prepl` run by the Go executable against the JVM's `io-prepl`; `start-server`/`stop-server` at the Go REPL | 5 of 5 on amd64, 5 of 5 on arm64 under `qemu-aarch64`: the transcripts equal the JVM's (the evaluated fns' class names `user/evalN` aside), prepl's messages equal (`:ms` and frames aside) |
| the oracle's new forms file `test/oracle/forms/net.clj` (83 cases: literals, bytes, printing, equality, kinds, socket addresses, a loopback connection and its exceptions; recorded twice, identical) | 83 of 83 on amd64 (and go-file's `files.clj`, 324 of 324, after the merge) |
| Clojure's suite on Go: `clojure.test-clojure.server`, `clojure.test-clojure.java.io` | `server` 2 tests, 13 of 13 assertions, as on the JVM; `java.io` loads now (it imports `Socket` and `ServerSocket`): 15 tests, 109 of 113, its socket test passing, the 4 errors `URLClassLoader` and the class loader's `getResource` ("Files"). The full suite on Go, under the lock, after merging main (suite-last): 66 namespaces, all load, 681 tests, 19,632 assertions, 19,628 pass, 0 fail, 4 errors (java.io's), test.generative 26 of 26, no regression against `test/arbace-go-results.edn` (updated) |

Known differences: the buffer sizes the kernel reports (`getReceiveBufferSize`,
`getSendBufferSize`) differ between the two processes (the JVM: 65536 and 1313280 on a loopback
connection here, the Go executable: 131072 and 2626560; both read the kernel), and an unconnected
socket's are a constant in the Go build (131072, 16384: there is no socket to ask yet).

## Size

Measured against main at `b20b577` (go-file merged), both built by `bin/arbace-go --build` with
the image of prepared namespaces: amd64 66,931,956 to 68,856,988 bytes (+1.93 MB, +2.9%), arm64
64,071,080 to 65,932,776 (+1.86 MB); the image 3,834,917 to 3,893,244 bytes (+58 KB:
`arbace.core.server`). The translated java.net classes, `HostNet`, `HostSocketImpl` and jrt's
`net.clj` are about 176 KB of symbols (`go tool nm -size`), the rest their type and member
tables. Start (`-e nil`, amd64, 5 runs): 0.38-0.41 s against 0.34-0.44 s.

## Amendments (accepted by the user 2026-10-10)

- **NT1** (RT.doInit, C2G-SPEC §10): the Go build's `RT.doInit` loads `arbace.core.server`
  and starts the servers of the `arbace.server.*` properties as the JVM's does (the comment of
  the Go variant said it had no socket server). *Accepted 2026-10-10: C2G-SPEC §10.6 (and §16, "Sockets").*
- **NT2** (system properties): `JAVA_TOOL_OPTIONS`' `-D` options are the Go executable's system
  properties (the JVM's mechanism, the same text for both builds); no "Picked up" line is
  printed. Leading `-Dname=value` arguments of the executable, as the `java` launcher takes
  them, were not added: they belong to step 6's command line (`bin/arbace-go`, the program's
  `main`). *Accepted 2026-10-10: C2G-SPEC §9.4, §11.*
- **NT3** (licensing): `HostSocketImpl.java` transcribes `NioSocketImpl`'s code and the variants
  keep jdk26u's code of the methods they replace, so they are recorded in LICENSE.md as
  jdk26u-derived (GPL 2 with the Classpath Exception), as the B7 files are. *Accepted 2026-10-10: LICENSE.md; C2G-SPEC §4.1.*
- **NT4** (`InetAddress`'s cache): the variant's cache expires entries when they are used, and
  concurrent first lookups of one host each ask the name service (the JVM: one lookup per host
  at a time, expiries kept in a `ConcurrentSkipListSet`). *Accepted 2026-10-10: C2G-SPEC §4.1.*
- **NT5** (the musl texts): the `errno` and `gai_strerror` texts are musl's, as this machine's
  JVM gives them; a glibc JVM's differ (`Address already in use`, `Name or service not known`).
  *Accepted 2026-10-10: C2G-SPEC §9.4.*

With them the sockets in the closed world and `NetHost` are in C2G-SPEC §4.1, §9.1 and §9.4, D6's
reversal for sockets in B1-PLAN.md, and the oracle's `net.clj` in ORACLE.md.

# Files: java.io.File and the file system in the Go build

The user's decision of 2026-10-09, reversing part of D6 (B1-PLAN.md): `java.io.File` and the file
system, `arbace.java.io` on files and `file:` URLs, `slurp`/`spit` on paths, `load-file`
(branch `go-file`). Sources and their origins: JRT-SOURCES.md, "The closure as grown", `Files`.

## What was done

**`java.io.File` translated from jdk26u, its file system jrt's.** `File`, the abstract
`FileSystem`, `FileFilter`, `FilenameFilter`, `DeleteOnExitHook`, `FileReader`, `FileWriter`
(`src/java.base/share/classes/java/io`) and the Unix `UnixFileSystem` and `DefaultFileSystem`
(`src/java.base/unix/classes/java/io`, KIND `unix`, amendment FS1) are translated as they are.
UnixFileSystem's 15 natives read the `File`'s path field through JNI, which a jrt native cannot
(jrt builds without the translated classes, C2G-SPEC §9.1): the variant
`overlay/jdk/variants/UnixFileSystem.clj` replaces each by a method passing the path string to a
static native added to the class (`hostCanonicalize`, `hostBooleanAttributes` ...), and those
natives are jrt's Go (`filesystem.clj`), with the results of jdk26u's C
(`UnixFileSystem_md.c`: stat-based attributes, `access(2)`, `chmod`, `utimes` keeping the access
time, `O_CREAT|O_EXCL` creation, `remove`, `mkdir 0777`, `rename`, `statvfs`, `pathconf`;
`canonicalize_md.c`'s `JDK_Canonicalize` and `path_util.c`'s `collapse`, transcribed: LICENSE.md).
Other variants: the constructor reads the separators and `user.dir` through
`System.getProperty` (`StaticProperty` and `System.getProperties`' `Hashtable`-based
`Properties` were outside the world); `File$TempDirectory` reads `java.io.tmpdir` the same way and
draws its names from `java.util.Random` (`SecureRandom` is outside the world; the file is created
`O_EXCL`, so the names need not be unpredictable); `File.toPath` makes jrt's `HostPath`;
`DeleteOnExitHook` registers with `Runtime.addShutdownHook`.

**The host: an optional `HostFS`** (`hostfs.clj`). `Host` (host.clj) is unchanged: what `File`
needs beyond its `Open`, `Stat`, `ReadDir`, `Remove`, `Mkdir`, `Rename` is a second interface,
`HostFS` (`Access`, `Chmod`, `Chtimes`, `Realpath`, `Statfs`), which a host implements when it
can; `CurrentHostFS()` is the current host as one, or nil. B1a's `OSHost` implements it on Linux
(`hostfs_linux.clj`, `//go:build linux`: `syscall.Access`, `syscall.Chmod`, `os.Chtimes` with a
zero access time, `filepath.EvalSymlinks`, `syscall.Statfs`). Without it (B1b's host until it has
one) the natives answer as the JDK does when the system call fails (no permission changes or
times, canonical paths only collapsed, no space). Kept in files of their own, apart from the
socket work's host additions.

**Shutdown hooks at the end of main.** `RunMain` now runs the shutdown hooks after main and the
non-daemon threads end, as the JVM's `DestroyJavaVM` does; before, they ran only at
`System.exit`, so `deleteOnExit` would not have deleted when a script ended normally.

**`java.nio.file`, as far as Arbace and the suite use it** (decision FS2 below). jrt's own
`Path` (the documented interface without `getFileSystem` and `register`), `Files` (streams,
readers and writers, whole-file reads and writes, `lines`, the tests, `size`, creating and
deleting files and directories, temporary files and directories, `copy`, `move`) and
`jdk.internal.jrt.HostPath` (UnixPath's path operations transcribed over the path's chars, with
UnixPath's `compareTo` and `hashCode` over the UTF-8 bytes); jdk26u's `Paths`, the option enums
and interfaces, `FileAttribute`, and the exceptions (`NoSuchFileException`,
`FileAlreadyExistsException`, `DirectoryNotEmptyException`, `AccessDeniedException`,
`InvalidPathException`, `ProviderMismatchException`, `FileSystemException`). `Files`' errors are
the documented ones; its readers replace malformed input as `InputStreamReader` does where the
JDK's report it (`MalformedInputException`).

**`java.net.URL` translated, with the `file:` connection.** `slurp` of a path string tries
`(URL. s)` first, so `URL` is needed for the plainest file read. Translated from jdk26u: `URL`,
`URLStreamHandler`, `URLStreamHandlerFactory`, `URLConnection`, `MalformedURLException`,
`UnknownServiceException`, `URLDecoder`, `ContentHandler`, `FileNameMap`;
`sun.net.www.ParseUtil`, `URLConnection`, `MessageHeader`, the `file:` connection
`FileURLConnection` and the handlers of `file:` (Unix), `http:`, `https:` and `jar:` (their
syntax and default ports; their connections stay outside the world); what they use,
`sun.net.util.IPAddressUtil` (the URL checks), `jdk.internal.util.Exceptions`, `HexFormat`,
`HexDigits`, `Hashtable` and `Dictionary`. Variants: `URL` without the static initializer that
hands its handler to `SharedSecrets`, without the `ScopedValue` and `ObjectStreamField` fields,
the providers' lookup finding none, and a default factory of the four handlers; `URLStreamHandler`'s
`hashCode` and `hostsEqual` by host name (the address lookup, `InetAddress`, is outside the world:
they answer as the JDK for a host that does not resolve, exactly as the JDK for `file:` URLs);
the `file:` handler's `openConnection(URL)` without the `Proxy` overload; `ParseUtil.decode`
over `String`'s UTF-8 with the decoder's REPORT (valid UTF-8 is what encodes back to the same
bytes); `URLConnection.getFileNameMap` knowing no MIME type (the JDK's table is a resource of
the image); `Exceptions.setup` with the JDK's default `jdk.includeInExceptions`
(`hostInfoExclSocket`); `HexFormat.parseHex(char[] ...)` over a `String`. c2g's rename table
gains `Www_URLConnection`, `File_Handler`, `Http_Handler`, `Https_Handler`, `Jar_Handler` and
`Net_Proxy` (jrt's `Proxy` is `java.lang.reflect`'s).

**The namespaces.** `arbace/lang/go/ns/java/io.subst.clj`: `copy` between two files through
their streams (Clojure's goes through their channels, outside the world), and the escape of `+`
written out (`"%2B"`; Clojure computes it with `URLEncoder`, whose encoders are outside the
world). `main.subst.clj` no longer replaces the error report's `Files/createTempFile`: the
report goes to a temporary file as on the JVM. `Compiler.loadFile`'s Go-build variant is gone:
`load-file` is the JVM's, with `File`'s absolute path and name.

**Not done.** `RandomAccessFile` (nothing in Arbace or the suite uses it); `java.nio`'s
channels, buffers and coders (`FileChannel`, `URLEncoder`); `Files`' attribute views, directory
streams, walks and links; `jar:`, `http:` and `https:` connections; `URLClassLoader`
(`java.io` suite's two resource tests); `java.net.ServerSocket` and `Socket` (the sockets
branch).

## Decisions

- **`java.nio.file`: jrt's own subset, not jdk26u's file system providers.** jdk26u's `Path`
  and `Files` go through `FileSystemProvider` and `sun.nio.fs` (`UnixPath`,
  `UnixFileSystemProvider`, `UnixNativeDispatcher`'s some hundred natives, channels, attribute
  views, watch services): thousands of lines and a second native layer for what `java.io.File`'s
  15 natives already reach. What Arbace and the suite use is small: `arbace.main`'s error report
  (`Files/createTempFile`, `Path.toFile`), the suite's `test-iteration`
  (`Files/newBufferedReader`, `File.toPath`); Clojure's `java.io` namespace uses `java.nio` only
  for `copy`'s channels. So `Path`, `Files` and `HostPath` are jrt's own Java over `File` and the
  file streams, with the documented behaviour of the common operations and UnixPath's path
  arithmetic, and jdk26u's small plain files (`Paths`, the options, the exceptions) translated.
  Alternative considered: translating `sun.nio.fs` with its natives in jrt, kept for when
  channels or attribute views are needed.
- **`RandomAccessFile`: not now.** Nothing in Arbace, its namespaces or Clojure's suite uses it;
  jdk26u's needs `FileChannel` and its own natives over the file table. It would join `HostFiles`
  (a seek and a read/write mode) when wanted.
- **`java.net.URL` translated, not written.** `slurp` of a path string goes through `URL` first,
  and the suite's `java.io` tests build `URL`s of every shape; jdk26u's parser
  (`URLStreamHandler.parseURL`, `IPAddressUtil`'s checks) is plain Java, so the URLs behave as
  the JDK's. The cost: 23 more files (the handlers, the connection classes, `Hashtable`,
  `HexFormat`) and seven variants. Connections other than `file:` stay outside the world.

## Results

| check | result |
|---|---|
| `bin/jrt-convert` | 389 files (340 before), all compiled to javac's class shapes |
| `bin/c2g --program` | 2,021 classes (1,961 with `File` alone), no errors |
| `bin/jrt test -run 'TestCollapsePath\|TestFileSystemNatives'` (`filesystem_test.clj`: the natives on a temporary directory, `collapse`) | pass on amd64 and on arm64 under `qemu-aarch64` |
| the oracle's new `forms/files.clj`, 324 cases | the JVM 324 of 324; Go 323 of 324 (the hash of a `localhost` URL, in `known-go-amd64.edn`) |
| `bin/oracle check jvm` | 20,587 of 20,587 |
| `bin/oracle check 'target/arbace-go/amd64/arbace -' --expected test/oracle/known-go-amd64.edn` | 20,553 of 20,587; the mismatches exactly the 34 known |
| Clojure's suite on Go (amd64) | 19,398 assertions, 19,375 pass, 19 fail, 4 errors (was 19,280, 19,251, 19, 10): `method-thunks` (20 of 20), `reader` (7,796 of 7,796, `load-file` of temporary files) and `sequences` (1,148 of 1,148) now pass as on the JVM; `java.io` still fails to load on `java.net.ServerSocket` (the sockets branch); without its socket test and imports, 14 tests and 107 of 111 assertions pass, the 4 errors `URLClassLoader` and the class loader's `getResource` |
| size, amd64 | 60.70 MB, against 58.60 MB: +2.1 MB |

## Amendments (FS)

All accepted by the user 2026-10-10 and folded where they belong (C2G-SPEC §16, "Files"):

- **FS1 (JRT-SOURCES.md, the tool) Files of `src/java.base/unix/classes`.** `bin/jrt-convert`
  copies them like the shared ones; `sources.txt` gives them KIND `unix`
  (`g2c.jrt-sources/print-sources`). For `UnixFileSystem`, `DefaultFileSystem` and the `file:`
  handler, whose shared counterparts are abstract or absent. *Accepted 2026-10-10, folded into
  JRT-SOURCES.md ("The closure as grown", Files).*
- **FS2 (decision) `java.nio.file` as jrt's own `Path`, `Files` and `HostPath`** (above,
  Decisions), and no `RandomAccessFile`. *Accepted 2026-10-10; it stays here, in Decisions, and
  C2G-SPEC §4.1 refers to it.*
- **FS3 (C2G-SPEC §9.4, the host interface) An optional `HostFS` beside `Host`.** A host's
  file system calls beyond `Host`'s are a second interface a host may implement
  (`CurrentHostFS`); jrt's natives fall back to the JDK's failure results without it. Keeps
  `Host` unchanged for B1b's monitor and for the other host additions (sockets). *Accepted
  2026-10-10, folded into C2G-SPEC §9.4.*
- **FS4 (C2G-SPEC §4.4, Collisions) Rename table entries**: `sun/net/www/URLConnection`
  `Www_URLConnection`, the handlers `File_Handler`, `Http_Handler`, `Https_Handler`,
  `Jar_Handler`, and `java/net/Proxy` `Net_Proxy` (jrt's `Proxy` is `java.lang.reflect`'s).
  *Accepted 2026-10-10, folded into C2G-SPEC §4.4.*
- **FS5 (C2G-SPEC §8.4, threads) Shutdown hooks at the end of main.** `RunMain` runs them once
  main and the non-daemon threads have ended, as the JVM does; before, only `System.exit` ran
  them. *Accepted 2026-10-10, folded into C2G-SPEC §8.4 (and `Runtime.addShutdownHook`'s
  description in "`System`, `Runtime` and the host" below holds as amended).*
- **FS6 (C2G-SPEC §4.1 and §10.3, the world and the namespace variants) `java.io.File`,
  `java.net.URL` with the `file:` connection, and the `java.nio.file` subset in the closed
  world**, with the variants listed above; `java/io.subst.clj` (copy between files by streams,
  `"%2B"`) and `main.subst.clj` (the error report's temporary file as on the JVM) changed, and
  `Compiler.loadFile`'s variant removed. *Accepted 2026-10-10, folded into C2G-SPEC §4.1 and
  §10.3.*
- **FS7 (LICENSE.md) Transcribed code**: `HostPath.java` (UnixPath's path operations) and
  `filesystem.clj` (`JDK_Canonicalize`, `collapse`) hold jdk26u code under the GPL version 2 with
  the Classpath Exception, recorded by the rule of B7; the alternative is rewriting those parts
  from the documented behaviour. *Accepted 2026-10-10 (kept, as LICENSE.md records it).*

# The JDK's resource data (regex `\N{name}` and `CANON_EQ`)

Branch `regex-res` (2026-10-09, the user's decision): the 29 oracle regex cases that failed on
the Go build for want of the JDK's resource data (EVAL-NOTES.md, "Phase 2B follow-up": 5
`\N{name}`, 24 `CANON_EQ`). With them the REPL gains `Character/getName`,
`Character/codePointOf` and `java.text.Normalizer` (NFC, NFD, NFKC, NFKD), and Clojure code gets
java.nio's heap buffers (`ByteBuffer/wrap`, `CharBuffer/wrap`, ...). Sources and data from
jdk26u at `baf63fb`; how `bin/jrt-convert` makes them: JRT-SOURCES.md, "The JDK's resource
data".

## The data in the executable

- `bin/jrt-convert`'s `generate` step makes the data as the JDK build does, into
  `.tmp/jrt/data` under their resource paths: `java/lang/uniName.dat` (Gendata.gmk's
  `CharacterName` tool on `UnicodeData.txt`), ICU's `nfc.nrm` and `nfkc.nrm` (copied); all three
  byte-identical to the JDK build's module image. `uprops.icu` and `ubidi.icu` are left out:
  nothing reached loads them (`UCharacterProperty`'s static initializer reads no data; its
  `INSTANCE`, which would, is not reached).
- `bin/c2g --program` embeds them next to the namespaces' sources
  (`go/arbace/cmd/arbace/res/...`, `arbace.c2g.embed/data`, the directory `data` beside the
  `--jdk` input), as binary files: the embedding held only text so far. 270 KB more data.
- **`Class.getResourceAsStream`** (an edge member jrt left undefined since phase 2b) is c2g's
  support method on jrt's `Class` once `ByteArrayInputStream` is translated (jrt cannot name the
  translated stream): the name resolved as `Class.resolveName` does (absolute without its `/`,
  else in the package of the class, or of an array class's element class), then
  `jrt.ResourceOrPath` (the embedded resources, then `ARBACE_PATH`'s directories, as
  `ClassLoader.getResourceAsStream`), as a `ByteArrayInputStream`; null when absent. Deviation:
  the JVM encapsulates a named module's resources, so `(.getResourceAsStream Character
  "uniName.dat")` is nil from Clojure on the JVM and the data on Go; jdk26u's own classes read
  them as on the JVM.

## `\N{name}`: `CharacterName` translated unchanged

`java.lang.CharacterName` (already in the closure) reads `uniName.dat` through
`java.util.zip.InflaterInputStream`, whose `Inflater` is the VM's zlib (natives over native
memory, a `Cleaner`, direct buffers). Considered: a variant of `CharacterName`'s constructor
(the previous proposal), which would copy its 60 lines to change one; translating `Inflater` with
natives over Go's `compress/flate` (its streaming protocol, `inflateBytesBytes` resuming on more
input, does not map onto Go's pull reader). Chosen: **jrt's own `InflaterInputStream`**
(`overlay/jdk/java.base/java/util/zip/InflaterInputStream.java`, the `InputStream` constructor
only, as `FileInputStream`'s overlay is jrt's), which reads its input whole at the first read and
inflates it with a native over Go's `compress/zlib` (`natives.clj`,
`InflaterInputStream_Inflate0_B1_String1__B1_native`); a truncated stream throws
`EOFException("Unexpected end of ZLIB input stream")`, bad data `ZipException` (jdk26u's
`ZipException.java`, added). So `CharacterName` and `Character.getName`/`codePointOf` are
jdk26u's code, unchanged. Cost: Go's `compress/flate` and `zlib`, about 20 KB.

## `CANON_EQ`: java.nio's heap buffers translated (the route)

`Pattern` normalizes with `java.text.Normalizer` (NFD, NFC), jdk.internal.icu's normalizer, whose
loader (`ICUBinary.getRequiredData`, `NormalizerImpl.load`, `CodePointTrie.fromBinary`) reads
`nfc.nrm` through `java.nio.ByteBuffer` (`getInt`, `getChar`, `get(int)`, `order`, `position`,
`asCharBuffer().get(char[])`, `asIntBuffer().get(int[])`, `asCharBuffer().subSequence`). The
routes considered:

1. **Translate jdk26u's buffers** (chosen): the generated `ByteBuffer`, `CharBuffer`,
   `IntBuffer`, `HeapByteBuffer`, `HeapCharBuffer`, `HeapIntBuffer` and the big- and
   little-endian char and int views of a byte buffer, with `Buffer`, `ByteOrder`,
   `StringCharBuffer` and the four exceptions, made by `bin/jrt-convert` exactly as the JDK build
   makes them (byte-identical). The heap buffers reach memory through
   `jdk.internal.misc.ScopedMemoryAccess` (`getIntUnaligned(session, hb, offset, bigEndian)`, a
   null session for heap buffers), itself generated: translated too, and `Unsafe`'s byte-order
   overloads and copies are added to jrt. The constructors and `session()` name
   `java.lang.foreign.MemorySegment` and `jdk.internal.foreign.MemorySessionImpl`, so those two
   files are in the world as types (none of their code is reached: the segment is always null);
   without them c2g drops every constructor (C2G-SPEC §4.1, M8). Two variants only: `Buffer`'s
   static initializer (it hands `SharedSecrets` the package's `JavaNioAccess`: direct and
   mapped buffers, segments, the buffer pool) and `ScopedMemoryAccess`'s (its VM natives
   `registerNatives`, cut; `closeScope0`, which closes a shared session, throws). The ICU code is
   jdk26u's, unchanged.
2. A variant of the ICU loader over a `byte[]` (no java.nio): `ByteBuffer` is in the signatures
   of `ICUBinary` (5 methods), `NormalizerImpl.load`, `CodePointTrie.fromBinary` and its six
   typed variants, `Trie2`, so the variant would copy about 300 lines of ICU with the type
   swapped for a cursor class of jrt's: smaller (no buffers in the world), but a rewrite of the
   exact code that parses the data.
3. jrt's own minimal `java.nio` buffers (an overlay): the ICU code unchanged, but `ByteBuffer` and
   `CharBuffer` would be partial, non-JDK classes visible to the REPL.

Route 1 keeps both the loader and the buffers jdk26u's own code, and gives the REPL the JDK's
buffers whole (heap only: `allocateDirect`, mapped buffers, `asLongBuffer` and the other views
whose classes are not in the world throw). It costs the most, measured below; most of the cost
was `ScopedMemoryAccess`'s 400 public members, rooted only because the REPL's world roots every
public member (P2), which led to amendment RD2.

## Found on the way

- **A missing class initialization** (c2g, `decls.clj`, `touches-statics?`): a static method of
  a class with benign initialization goes without an entry guard when its body reads none of
  the class's statics (W3); the walk looking for static reads skipped maps that are not nodes,
  and a `switch`'s arms are such maps (`{:labels :body}`). `NormalizerBase.toMode` returns the
  class's `NFC` ... `NFKD` from switch arms, so it read them unguarded, before `NormalizerBase`'s
  initialization: null, then a `NullPointerException` in `Normalizer.normalize`. The walk now
  enters every map. Three translated methods of the existing closure had the same latent bug
  and now get their guard: `RoundingMode.valueOf(int)` (`BigDecimal`'s rounding modes by
  number), `Calendar.names`, `Nodes.emptyNode`; `arbace/lang` is unchanged.
- **A name collision**: `java.text.Normalizer` and `sun.text.Normalizer` (which `Pattern` calls for
  combining classes) are both `Normalizer` in Go; the rename table (C2G-SPEC §4.4) makes the
  latter `Sun_Normalizer`.
- `CharBuffer`'s code cast to `StringCharBuffer`, outside the world, as an undefined
  `StringCharBuffer_Cast` (a c2g gap for a checkcast to a class outside the world that the
  build caught); with `StringCharBuffer` in the world (`CharBuffer.wrap(CharSequence)` needs
  it anyway) it does not arise; the gap itself is not fixed here.
- jrt: `Reference.reachabilityFence` (`runtime.KeepAlive`; `ScopedMemoryAccess`'s every access);
  `Unsafe`'s `get`/`put` `Char`/`Short`/`Int`/`Long` `Unaligned` with a byte order, `getShortUnaligned`
  and `putShortUnaligned`, `copyMemory` and `copySwapMemory` between primitive arrays.

## The closure and the executable, measured (amd64)

| | main (`815d9d9`'s code) | RD2 alone | this branch |
|---|---:|---:|---:|
| `bin/jrt-convert`: files (share, gensrc, overlay, java.sql) | 340 (317, 8, 13, 2) | 340 | 375 (340, 19, 14, 2) |
| c2g: files, classes analyzed | 482, 1,950 | 482, 1,950 | 517, 2,021 |
| c2g: classes, methods reached | 1,945, 16,499 | 1,932, 16,189 | 1,981, 16,985 |
| methods with missing parts | 511 | 484 | 561 |
| executable (bytes) | 58,603,718 | 58,294,936 | 61,131,717 |

The resource data and the classes that read it cost **+2.84 MB** over RD2 alone (+4.9%; 49
classes and 796 methods more reached), the branch as a whole +2.53 MB over main (+4.3%): the
Go symbols grow by 0.89 MB (the reflection tables' initialization about 0.5 MB, the buffers
about 0.15 MB, `NormalizerBase` and the ICU loader, `compress/flate`), the embedded data by
0.27 MB, the rest is the tables of the runtime (line tables, types). Without RD2 the branch was
62,794,179 bytes (+4.19 MB): `ScopedMemoryAccess`'s 400 public members alone were 0.34 MB of
code and most of the reflection tables' growth.

## Results

- **Against the JVM, directly** (`Character/getName` of 17 code points, among them the
  algorithmic CJK and Hangul names, surrogates, unassigned; `Character/codePointOf` of 11 names,
  lower case and errors included; `Normalizer/normalize` and `isNormalized` of 17 strings in the
  four forms, among them Hangul, compatibility ligatures, the Ångström sign, a supplementary
  character, reordering of combining marks): the 97 lines printed are the JVM's.
- **The regex corpus** (`bin/c2g-regex`): 1,233 of 1,233 cases, 11,682 of 11,682 inputs (amd64).
- **The oracle on the Go build** (amd64): 20,259 of 20,263; the 29 cases pass, none new; the
  reference `test/oracle/known-go-amd64.edn` rewritten with `--write-expected` (the two
  resource-data groups gone, the other 4 cases and their reasons kept; checked by hand).
- **No regressions**: `bin/arbace-go --smoke`; `bin/jrt test` on amd64 and arm64;
  `bin/c2g-check -- --program` 9,010 of 9,010 steps on amd64 and arm64; Clojure's suite on the
  Go build 19,251 of 19,280 assertions, no regression against `test/arbace-go-results.edn`
  (unchanged: the suite does not use `\N{}`, `CANON_EQ` or `Normalizer`). The essential
  `bin/gate` was not run: the bootstrap compiles none of the changed sources (`arbace/c2g` is a
  tool), as the gate policy of 2026-10-09 allows. arm64's executable: 58,465,447 bytes.
- **After merging main** (step 6's image, the files of `go-file`): 424 files translated; the
  executable 69,415,929 bytes with step 6's image of prepared namespaces; the smoke test, the
  direct checks (identical to the JVM's), the oracle 20,595 of 20,600 (exactly the 5 known
  mismatches), the regex corpus 1,233 of 1,233, `bin/jrt test` on amd64 pass again; Clojure's
  suite on the Go build 19,379 of 19,398 assertions, no regression against
  `test/arbace-go-results.edn`.
- The amendments are numbered RD (resource data): the single letters are taken (Z since by
  EVAL-NOTES.md's proxies of `BufferedWriter`).

## Decisions

- Route 1 for `CANON_EQ` (above), against the smaller ICU-loader variant: fidelity, as the user
  prefers, at +2.8 MB.
- jrt's own `InflaterInputStream` over Go's zlib rather than a `CharacterName` variant: the JDK's
  class whose source needs what is cut (the VM's zlib) is the one replaced (K1's rule).
- The data are made, not kept: `bin/jrt-convert` makes them from the jdk26u checkout, as the
  translated sources; LICENSE.md records what the executables carry (ICU4J's and the Unicode
  Character Database's Unicode License V3, as jdk26u's `legal/icu.md` and `unicode.md`).
- The regex check program (`bin/c2g-regex`, which embeds nothing) finds the data through
  `ARBACE_PATH`, set to `.tmp/jrt/data` by `test/c2g/regex_check.clj`.

## Amendments (accepted by the user 2026-10-10)

All five were accepted on 2026-10-10 and folded into C2G-SPEC (§16, "The JDK's resource data");
each says where below.

- **RD1 (C2G-SPEC §10.3, Classes and resources by name; M5)** The program embeds, besides the
  namespaces' sources, the JDK's resource data that `bin/jrt-convert` makes (`.tmp/jrt/data`,
  under their resource paths: `uniName.dat`, `nfc.nrm`, `nfkc.nrm`), as binary files.
  `Class.getResourceAsStream` is c2g's support method on jrt's `Class` (when
  `ByteArrayInputStream` is translated): the name resolved as `Class.resolveName`, then the
  embedded resources and `ARBACE_PATH`, as `ClassLoader.getResourceAsStream`. No module
  encapsulation of resources (a deviation: the JVM hides java.base's from Clojure code). Accepted 2026-10-10: C2G-SPEC §10.3, "The JDK's resource data".
- **RD2 (C2G-SPEC §10.6, the REPL's world; P2)** The REPL's world roots every public member of
  every class with class forms **except the classes of JDK packages their module does not
  export** (`jdk.internal.*`, `sun.*`): the JVM refuses Clojure code access to them
  (`IllegalAccessError`), so rooting them only grew the executable. Classes of no JDK module
  (Arbace's, jrt's own `jdk.internal.jrt`) stay rooted. Alone it saves 310 methods and 0.31 MB on
  main; Clojure's suite, the oracle and the smoke test are unchanged by it (below). Accepted 2026-10-10: C2G-SPEC §10.6, "The REPL's world".
- **RD3 (C2G-SPEC §4.1, jrt's own Java; K1)** jrt's own Java gains
  `java.util.zip.InflaterInputStream`, the `InputStream` constructor only, which inflates its
  whole input at the first read through a native over Go's `compress/zlib`. Accepted 2026-10-10: C2G-SPEC §4.1, "The JDK's resource data".
- **RD4 (JRT-SOURCES.md; C2G-SPEC §4.1, the inputs)** The closure takes generated files beyond
  the measured closure's (`added-gensrc`: java.nio's heap buffers and their views, and
  `ScopedMemoryAccess`), generated by `bin/jrt-convert` as the JDK build generates them and
  compared byte for byte with the build's; and the resource data likewise
  (`generated.edn`'s `:data`, against the build's module image). Accepted 2026-10-10: C2G-SPEC §4.1 and JRT-SOURCES.md, "The closure as grown".
- **RD5 (C2G-SPEC §4.4, the rename table)** `sun.text.Normalizer` is `Sun_Normalizer` in Go
  (`java.text.Normalizer` keeps `Normalizer`). Accepted 2026-10-10: C2G-SPEC §4.4, the rename table.

Fixed, not amended: §6.2's benign-initialization rule (W3) counts a static read anywhere in the
method's body, `switch` arms included (it always meant that; the implementation missed them).

## Sources

https://github.com/openjdk/jdk26u at `baf63fb`:
`src/java.base/share/classes/java/nio/{X-Buffer,X-Buffer-bin,Heap-X-Buffer,ByteBufferAs-X-Buffer}.java.template`,
`Buffer.java`, `ByteOrder.java`, `StringCharBuffer.java` and the four exceptions;
`jdk/internal/misc/X-ScopedMemoryAccess{,-bin}.java.template`;
`java/lang/foreign/MemorySegment.java`, `jdk/internal/foreign/MemorySessionImpl.java`;
`java/text/Normalizer.java`, `CharacterIterator.java`; `jdk/internal/icu/` (the files listed in
JRT-SOURCES.md) and `jdk/internal/icu/impl/data/icudata/nfc.nrm`, `nfkc.nrm`;
`java/lang/CharacterName.java`, `java/util/zip/ZipException.java` (and `InflaterInputStream.java`,
`Inflater.java`, studied for the overlay's behaviour);
`make/modules/java.base/gensrc/GensrcBuffer.gmk`, `GensrcScopedMemoryAccess.gmk`,
`make/common/modules/GensrcStreamPreProcessing.gmk`, `make/modules/java.base/Gendata.gmk`,
`make/ToolsJdk.gmk`, `make/jdk/src/classes/build/tools/spp/Spp.java`,
`make/jdk/src/classes/build/tools/generatecharacter/CharacterName.java`;
`src/java.base/share/data/unicodedata/UnicodeData.txt`; `src/java.base/share/legal/icu.md`,
`unicode.md`. The JDK build's outputs compared against:
`build/linux-x86_64-server-release/support/gensrc/java.base/` and `jdk/modules/java.base/`.
