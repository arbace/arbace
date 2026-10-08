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
