# LLVM forms and the lowering to C forms

Status: built write-ahead in CX6 (doc/cpp/CPP-SURVEY.md, "As built: CX6 (write-ahead)"), for the user's acceptance.
CP1-CP14 bind it, CP4 (a) above all: C++'s eval is clang's `-O0` IR read as LLVM forms, lowered to C forms and run by
C23's interpreter (ci, C5) and AOT (C6); CP7 (a): exceptions by a pending flag (survey §5.4). Until ci exists, gcc
`-O0` compiling the lowered C is the second judge (CX6's exit).

Pieces:

- `arbace.repr.llvm` (`arbace/repr/llvm.ae`, `arbace/repr/llvm/{read,print}.ae`): IR text → LLVM forms (`read`), LLVM
  forms → IR text (`print`), the forms as `.ae` text (`module-text`); `bin/repr-llvm read|print|roundtrip`.
- `arbace.cpp.lower` (`arbace/cpp/lower.ae`): LLVM forms → C forms; `bin/cppi lower`, `bin/cppi lower-check`.
- The runtime (CX7, §10): libsupc++ and libstdc++-v3/src compiled to IR by the oracle and lowered by `arbace.cpp.lower`
  (`bin/cppi runtime-build`, `test/cpp/runtime/units.edn`), and the pending-flag layer of exceptions,
  `arbace/cpp/rt/eh.c.ae`: C forms written by hand, printed by repr/c (`bin/cppi runtime`).

All three are tools' namespaces evaluated from the checkout on the tools' host (stage 0); nothing is embedded.

## 1. Principles

1. **The text's grammar, LangRef's, as clang 23 writes it at `-O0`.** One form per top-level entity in the text's
   order; one form per instruction in its block's order. What the forms do not keep is spelling (white space,
   comments, the `; preds =` annotations, the spelling of an attribute group's members); the judge is the object:
   `clang -c` of the print equals `clang -c` of the original byte for byte (§6).
2. **Plain Clojure syntax**, read by Arbace's reader without data readers. LLVM's sigils that the reader cannot take
   are spelled otherwise: `@x` (which the reader reads as a deref) is `g/x`; names the reader would not read back as a
   symbol are `(% "...")` and `(g "...")`.
3. **Positional where LLVM is positional, keyword options where LLVM's syntax is optional**: an instruction is its
   opcode, its flags (keywords), its type and operands in the text's order, then `:key value` options (`:align`,
   `:volatile`, `:md` ...) that are written only when present.

## 2. Names

| IR | form |
|---|---|
| `%x`, `%x.addr`, `%0`, `%struct.S` (local values, labels, named types) | the symbols `%x`, `%x.addr`, `%0`, `%struct.S` |
| `%"class.std::basic_ostream"`, any local name the reader would not read back | `(% "class.std::basic_ostream")` |
| `@main`, `@.str` would be `g/.str`: `@_ZN1A1fEv`, `@llvm.memcpy.p0.p0.i64` | `g/main`, `g/_ZN1A1fEv`, `g/llvm.memcpy.p0.p0.i64` |
| `@.str.1`, `@0`, `@"\01foo"` | `(g ".str.1")`, `(g 0)`, `(g "\u0001foo")` |
| `$comdat` | the string `"comdat"` (in `llvm/comdat` and `:comdat`) |
| `!0`, `!llvm.loop` | the symbols `!0`, `!llvm.loop` |
| `#3` (an attribute group) | the integer `3` in an `:attrs` vector |

A global's name is a plain `g/NAME` when it starts with a letter, `$` or `_` and holds neither `..` nor a final `.`;
a string's bytes are chars 0-255 (`\XX` escapes decoded).

## 3. Types

| IR | form |
|---|---|
| `void`, `i1`, `i32`, `i128`, `float`, `double`, `x86_fp80`, `fp128`, `half`, `ptr`, `label`, `metadata`, `token` | the symbol |
| `ptr addrspace(1)` | `(ptr 1)` |
| `[4 x i8]`, `<4 x i32>`, `<vscale x 4 x i32>` | `(array 4 i8)`, `(vector 4 i32)`, `(vscale 4 i32)` |
| `{ i32, ptr }`, `{}`, `<{ i8, i32 }>` | `(struct i32 ptr)`, `(struct)`, `(packed i8 i32)` |
| `i32 (ptr, ...)` | `(fn i32 [ptr &])` |
| `%struct.S` | `%struct.S` (a named type: `(llvm/type %struct.S (struct ...))` or `(llvm/type %struct.S opaque)`) |
| `target("x", i8, 1)` | `(target "x" i8 1)` |

Typed pointers (`i8*`) are refused (`:typed-pointer`): clang 23 writes opaque pointers only.

## 4. Values and constants

| IR | form |
|---|---|
| `42`, `-1`, `true`, `false` | `42`, `-1`, `true`, `false` (integers beyond a long as BigInts) |
| `1.500000e+00`, `0x3FB999999999999A`, `0xK4000C000000000000000` | `(fp "1.500000e+00")`, `(fp "0x3FB999999999999A")`, `(fp "0xK4000C000000000000000")` (the spelling: LLVM's floats are exact in it) |
| `null`, `undef`, `poison`, `zeroinitializer`, `none` | the symbols |
| `c"ab\00"` | `(c-string "ab\u0000")` |
| `{ i32 1, ptr null }`, `<{ ... }>`, `[i32 1, i32 2]`, `<i32 1, i32 2>` | `(struct [i32 1] [ptr null])`, `(packed ...)`, `(array [i32 1] [i32 2])`, `(vector ...)` |
| `getelementptr inbounds (T, ptr @g, i64 0, i32 1)` | `(getelementptr :inbounds T [ptr g/g] [i64 0] [i32 1])`; `inrange(-16, 8)` is the flag `(:inrange "-16, 8")` |
| `ptrtoint (ptr @f to i64)`, `add (i64 a, i64 b)` | `(ptrtoint ptr g/f i64)`, `(add i64 a b)` (a constant cast or binary operator has its instruction's form) |
| `blockaddress(@f, %bb)`, `dso_local_equivalent @f`, `no_cfi @f`, `splat (i32 1)` | `(blockaddress g/f %bb)`, `(dso_local_equivalent g/f)`, `(no_cfi g/f)`, `(splat [i32 1])` |
| `asm sideeffect "nop", ""` | `(asm "nop" "" :sideeffect)` |

A typed operand `T v` is the vector `[T v]`; a call argument or a parameter with attributes `[T attr... v]`.

**Attributes**: a bare word is a keyword (`:nounwind`, `:noundef`); `align 8` is `(:align 8)`; `word(ARGS)` is
`(:word TYPE)` for the type-taking ones (`byval`, `sret`, `byref`, `inalloca`, `preallocated`, `elementtype`) and
`(:word "ARGS")` (the text) for the others (`dereferenceable(8)`, `memory(argmem: readwrite)`); `word=N` is
`(:word= N)`; a string attribute `"k"="v"` is `["k" "v"]`, `"k"` is `["k"]`.

**Metadata**: `!N`, `!name` symbols; `!"s"` is `(! "s")`; `!{...}` is `(! [...])`; a specialized node
(`!DILocation(...)`) is `(!DI "!DILocation(...)")`, its text kept (no `-g` at `-O0`: none in the corpora); a typed
constant in a node is `[T v]`.

## 5. Instructions and the top level

```clojure
(= %r (add :nsw i32 %a 1))                       ; %r = add nsw i32 %a, 1
(= %c (icmp :slt i32 %a %b))                     ; icmp slt (samesign before the predicate: (icmp :samesign :ult ...))
(= %d (sext i8 %x i32))                          ; sext i8 %x to i32
(= %p (alloca %struct.S :align 8))
(= %v (load i32 [ptr %p] :align 4 :volatile true))
(store [i32 %v] [ptr %p] :align 4)
(= %q (getelementptr :inbounds %struct.S [ptr %p] [i32 0] [i32 1]))
(= %m (phi i32 [%a %bb1] [0 %bb2]))
(= %s (select [i1 %c] [i32 %a] [i32 %b]))
(= %r (call i32 g/f [[i32 :noundef %a]] :attrs [5]))
(= %r (call (fn i32 [ptr &]) g/printf [[ptr :noundef (g ".str")] [i32 %v]]))
(= %r (invoke ptr g/_Znwm [[i64 8]] :to %ok :unwind %lpad :attrs [12]))
(= %lp (landingpad (struct ptr i32) :cleanup (catch ptr g/_ZTIi) (catch ptr null)))
(br i1 %c %then %else) (br %next) (switch i32 %x %default [[i32 1 %one] [i32 2 %two]])
(ret i32 0) (ret void) (resume (struct ptr i32) %lp) (unreachable)
;; metadata attachments, in order:
(br %for.cond :md [!llvm.loop !8])
```

The rest: `fneg`, `fcmp`, every cast, `extractvalue`/`insertvalue` (indices after the typed operands),
`extractelement`, `insertelement`, `shufflevector`, `freeze`, `va_arg`, `fence ORDERING`, `atomicrmw OP [ptr p] [T v]
ORDERING`, `cmpxchg [ptr p] [T c] [T n] SUCCESS FAILURE`, `indirectbr`, `tail call` (`:tail :tail`), atomic loads and
stores (`:atomic :seq_cst`, `:syncscope "x"`). `uselistorder` and `callbr` are refused.

Top level: `(llvm/source-filename "x.cpp")`, `(llvm/datalayout "...")`, `(llvm/triple "...")`, `(llvm/type NAME T)`,
`(llvm/comdat "name" :any)`, `(llvm/global NAME T INIT? :linkage :internal :constant true :align 4 ...)`, `(llvm/alias
...)`, `(llvm/declare NAME RET [PARAMS] :attrs [3] ...)`, `(llvm/define NAME RET [PARAMS] :linkage :linkonce_odr :comdat
true :personality [ptr g/__gxx_personality_v0] :attrs [0] :body [(block LABEL inst...) ...])` (the entry block's
label `nil` when the text does not write it), `(llvm/attributes 0 [...])`, `(llvm/meta !5 MD :distinct true)`,
`(llvm/named-meta !llvm.ident [!5])`, `(llvm/module-asm "...")`. A parameter is `[T attr... %name]` (`[T attr...]`
unnamed in a declare), `&` for `...`.

## 6. The round trip

`bin/repr-llvm roundtrip LIST WORK`, and the first levels of `bin/cppi lower-check`:

1. **Forms**: the forms written as `.ae` text and read back are equal; the print read again gives the same forms.
2. **Object**: `clang -c` of the print equals `clang -c` of the original, byte for byte. (`clang -c` of the
   original IR, not of the C++ source: the IR is the eval's input.)

## 7. The lowering (`arbace.cpp.lower`)

The C forms are those of doc/c/SPEC.md, printed by repr/c's printer at `:gnu23` and judged by
`gcc -std=gnu23 -O0 -w -fno-builtin -fno-strict-aliasing -ffp-contract=off`.

- **One target** (x86-64 SysV): the IR's data layout must be clang's for it
  (`e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128`), else `:datalayout`.
- **Types.** `iN` is its container's unsigned type (`unsigned-char`, `unsigned-short`, `unsigned`, `unsigned-long`,
  `unsigned-__int128`; `i1` an `unsigned-char` 0 or 1; widths between masked: loaded masked to the width, and a width whose
  store size is smaller than its container's (`i24`, `i40`, `i48`, `i56`: bit-field access units clang does not widen over the
  next field) loaded and stored by its store size's bytes, `__builtin_memcpy`, as LLVM reads and writes it; a `load volatile` or `store volatile` of such a width is a plain `__builtin_memcpy`: the qualifier is dropped on that path, harmless for the gcc -O0 builds the check makes, a known divergence of review 018's R8), `ptr` is `(* void)`, `float`, `double`,
  `x86_fp80` as `long-double` (CS35: ci computes it as `double`, the class `long-double`; gcc as x87), aggregates as C
  structs whose members are `f0`, `f1` ... (arrays and vectors as a struct of one member `e`, packed structs
  `__attribute__((packed))`, vectors aligned as LLVM aligns them), each followed by `(c/static-assert (== (sizeof-type
  (struct T)) N))` with N the data layout's size: C's layout and the IR's agree or gcc refuses the unit.
- **Names.** A global keeps its name when C takes it, else `g$` and its escape (`.` as `$_`, other bytes `$XX`) with an
  `__asm__` label holding the linker's name; locals are `l$` and their escape, labels `:b$NAME`. `main` is `cx$main`,
  called by a C `main` that checks for an uncaught exception. No other name is the lowering's own: since CX7 the Itanium
  ABI's entry points (`__cxa_throw` ...) are libsupc++'s, lowered (§10); CX6 renamed them to a hand-written runtime's.
- **Functions.** One `c/defn` per define (linkage: `internal`/`private` static, `linkonce`/`weak` `__attribute__((weak))`,
  `available_externally` a declaration only), prototypes first; `llvm.global_ctors` as `constructor` wrappers (§8). The
  IR is already lowered to the ABI, so C's parameter and return types reproduce its calls: `byval(T)` parameters are
  the struct by value (the body takes its address), `sret` a pointer, coerced aggregates C structs of the same members.
- **Bodies.** A C variable per SSA value, declared at the top (so `goto` never jumps into a scope); each block a
  label; `phi` as copies into a `$p` variable on each incoming edge and a copy out at the block's start (parallel
  copies: the swap problem cannot arise); `alloca` in the entry block an aligned `unsigned char` array (elsewhere, and
  dynamic ones, `__builtin_alloca_with_align`); `load`/`store` through a cast pointer (`volatile` kept);
  `getelementptr` as `(char *)base +` the offsets the data layout gives, constant parts folded; arithmetic at the wide
  unsigned type (wrapping), signed operations on the signed views; `icmp` signed or unsigned, `fcmp`'s ordered and
  unordered predicates by C's comparisons; casts by C's conversions, a `bitcast` between non-pointer types by
  `__builtin_memcpy` from a compound literal of the source type in a statement expression (gcc's C has no
  `__builtin_bit_cast`); an inline `asm` whose template is empty and which has no result (a compiler barrier, `asm
  volatile("" : : "rm"(x))`) as nothing, other inline assembly refused (`:inline-asm`); atomics (CP121 (c), since
  `std::thread` runs on musl's pthreads: until then plain operations, CP1) as gcc's `__atomic_*` builtins with the IR's
  orderings (`unordered` and `monotonic` `__ATOMIC_RELAXED` 0, `acquire` 2, `release` 3, `acq_rel` 4, `seq_cst` 5, written as
  numbers): `load atomic`/`store atomic` `__atomic_load_n`/`__atomic_store_n` on an integer of 1 to 16 bytes or a pointer
  (`volatile` kept in the pointer's type), the generic `__atomic_load`/`__atomic_store` through memory on a float;
  `atomicrmw` `__atomic_exchange_n` (`xchg`; the generic `__atomic_exchange` on a float) and `__atomic_fetch_add`, `_sub`,
  `_and`, `_or`, `_xor`, `_nand`, the others (`max`, `min`, `umax`, `umin`, `fadd`, `fsub`) a loop of the generic
  `__atomic_compare_exchange` on the old value (refreshed by a failed exchange), the rest refused (`:atomic`); `cmpxchg`
  `__atomic_compare_exchange_n` into the result's two fields (`weak` kept, both orderings); `fence`
  `__atomic_thread_fence`, `syncscope("singlethread")` `__atomic_signal_fence` (an operation of that scope stays a
  thread-wide atomic: stronger, the same result); `unreachable` as `__builtin_trap()`.
- **Intrinsics** by a table: `memcpy`/`memmove`/`memset`, `lifetime.*` and the other hints (nothing), `expect`,
  `threadlocal.address`, `objectsize`, `is.constant`, `trap`, `va_start`/`va_end`/`va_copy` (`__builtin_va_*` on the
  `va_list` the IR allocated), `stacksave`/`stackrestore`, the `*.with.overflow` family (`__builtin_*_overflow`),
  `smax`/`smin`/`umax`/`umin`, `abs`, `ctpop`/`ctlz`/`cttz` (`i128` by halves), `bswap`, `fmuladd` (unfused, as clang's
  x86-64 code), `frexp` (`__builtin_frexp*` into the result's two fields), `x86.sse2.pause` (nothing),
  `eh.typeid.for` (a number per type_info), the math functions (`fabs`, `sqrt`, `floor`, ... as `__builtin_*`). Others
  are refused (`:intrinsic`), `llvm.coro.*` among them (`:coroutine`; clang's `-O0` pipeline runs CoroSplit, so the
  IR it writes holds none: the coroutine sample lowers and runs).
- **Constants**: integers as `(lit unsigned N)`/`(lit unsigned-long N)` (an `i128` by shifts), doubles as their
  shortest decimal, floats `(lit float x)`, NaNs by their payload (`__builtin_nan("0x...")`, `__builtin_nans` for a
  signaling one, negated for the sign: constant expressions, also in static initializers), `x86_fp80` as the exact decimal of its 64-bit
  significand (`(lit long-double 1.25M)`); static initializers as `(init ...)` with address constants
  `(conv (* void) (+ (conv (* char) (addr g)) 16))`; a static thread-local has its definition only (C has no tentative
  definition of one).

## 8. Exceptions (CP7, the survey's §5.4)

Since CX7 the runtime of exceptions is libsupc++'s own code, lowered (§10): `__cxa_allocate_exception`, `__cxa_throw`,
`__cxa_begin_catch`, `__cxa_end_catch`, `__cxa_rethrow`, `std::exception_ptr` (`__cxa_init_primary_exception`,
`std::rethrow_exception`, dependent exceptions), `std::terminate` and its verbose handler, the `type_info` classes. Only
the two parts that need DWARF unwinding are replaced, by the hand-written pending-flag layer `arbace/cpp/rt/eh.c.ae`
(CP86 (c)): the unwinder (libgcc's `_Unwind_RaiseException`, `_Unwind_Resume`, `_Unwind_Resume_or_Rethrow`,
`_Unwind_DeleteException`) and the personality routine (`eh_personality.cc`'s `__gxx_personality_v0` and
`__cxa_call_unexpected`).

`cx$pending` is raised by the unwinder's entry points (which `__cxa_throw`, `__cxa_rethrow` and `std::rethrow_exception`
call) and by `cx$resume` (the `resume` instruction); each records the in-flight `_Unwind_Exception` and returns at once.
A `call` that may unwind (not `nounwind` at the call or the callee, not an intrinsic) is followed by `if (cx$pending)
return z;` (`z` the function's zero value): so `__cxa_throw` itself returns after `_Unwind_RaiseException`, before the
`std::terminate` that follows it in libsupc++; an `invoke` by `if (cx$pending) goto unwind; else goto normal;` (with the
edges' phi copies). A landing pad's `landingpad` computes its two fields: the in-flight `_Unwind_Exception`
(`cx$exception()`, what the IR passes to `__cxa_begin_catch`) and the selector: the first `catch` clause whose type_info
`cx$catches` (null: every exception) gives that type_info's number (the same `llvm.eh.typeid.for` gives); a `filter`
clause (a dynamic exception specification) gives -1 unless one of its types catches the exception (an empty one always:
the IR's `ehspec.unexpected` path calls `__cxa_call_unexpected`); else 0 for a `cleanup`, else the frame does not handle
the exception and returns with the flag still set; then the flag is cleared. `cx$catches` does for one clause what the
personality's search phase does (`get_adjusted_ptr`): the thrown object and type from the exception's header (a primary
exception's or, for a dependent one, its primary's; a foreign exception only matches `catch (...)`), the thrown pointer
dereferenced when the thrown type is a pointer (`__is_pointer_p`), then the clause's `std::type_info`'s own `__do_catch`
(virtual slot 4), and on a match the adjusted pointer stored in the header's `adjustedPtr`, where `__cxa_begin_catch`
reads it. The flag is lowered while `__do_catch` runs (it is lowered code, whose calls return at once while the flag is
set) and raised again after. An exception that leaves `main` calls `cx$uncaught`, which does what `__cxa_throw` does when
no handler is found: `__cxa_begin_catch` on it, then `std::terminate()` (libstdc++'s verbose handler: "terminate called
after throwing an instance of 'T'", `what()`, `abort()`, status 134).

What differs from the Itanium ABI, by design: there is no search phase, so an exception that no handler catches runs
the cleanups of the frames it leaves before `std::terminate` (libstdc++ terminates first: the class
`uncaught-cleanups` when a cleanup prints); native code that calls lowered code (musl's `qsort`, `pthread_once`, an
`atexit` handler) does not test the flag, so an exception thrown through a native frame returns into it as if nothing were
thrown (the class `native-callback-throw`; the C library is native until C5's musl as C forms). Two native frames are
not such frames. Musl's thread start (CP120 (b)): the lowering renames the lowered code's `pthread_create` to the layer's
`cx$pthread_create`, which hands musl a trampoline (`cx$thread_start`, the routine and its argument in a `malloc`ed pair)
that calls the routine and then `cx$uncaught` when the flag is set, so an exception that leaves a `std::thread`'s callable
(libstdc++'s lowered `execute_native_thread_routine` returns with the flag set, its cleanups run) terminates as g++'s
build does, "terminate called after throwing ..." and status 134 (the runtime sample `thread-throw.cpp`). And the C
library's loop over the constructors: each `llvm.global_ctors` entry is a C `constructor` wrapper (`cx$ctor$N`) that calls the function and
then `cx$uncaught` when the flag is set, so a throw that leaves a dynamic initializer terminates there, before a later
constructor or `main` runs (review 007's finding 9). Until CX6 the
library itself was native and exceptions it threw unwound natively through the lowered frames (the classes
`library-throw` and `callback-throw`, gone with CX7).

## 9. The classes of the lowering check

Every file of `bin/cppi lower-check` that does not pass is in a class of `test/cpp/lower-known.edn` (NAME → class):

| class | what |
|---|---|
| `uncaught-cleanups` | an uncaught exception runs cleanups that print before `std::terminate` (§8) |
| `native-callback-throw` | an exception thrown through a native frame of the C library (§8) |
| a refusal's class (`inline-asm`, `vector`, `intrinsic`, `coroutine`, `datalayout`, ...) | the lowering refuses the construct |

Since CX7 the lowered program links the lowered runtime (§10), not libstdc++.so, and the testsuite's own library
(`util/`'s `testsuite_*.cc`) is lowered with it: clang compiles it to IR in each run, the lowering and gcc make
`WORK/libltestc++.a`. The original (CP83 (a): the same IR linked by clang) links the native libstdc++ and libtestc++.
The numbers are in the survey's "As built: CX6 (write-ahead)" and "As built: CX7 (write-ahead)".

## 10. The runtime (CX7)

`bin/cppi runtime-build [OUT]` (default `.tmp/cppi/runtime`; `bin/cppi lower-check` runs it when `OUT` has no library)
builds the runtime a lowered program links in place of libstdc++.so, from `test/cpp/runtime/units.edn`:

1. **Sources**: libsupc++ and libstdc++-v3/src of the pinned gcc 15.2.0 tarball (with `libiberty/cp-demangle.c`, which
   libsupc++ compiles as C, and `libgcc/unwind-pe.h`), extracted into `.tmp/cppi/gcc/`, its digest checked. The units are
   the members of the static `libstdc++.a` the build makes for this host's configuration (Makefile.am's lists: libsupc++,
   `src/c++98` ... `src/c++26`, the compatibility files of `libstdc++.la`, configure's links for the generic locale
   model, `os/generic`, the atomic builtins and `basic_file_stdio`) and those of `libstdc++exp` (the contracts and the
   Filesystem TS; not `std::stacktrace`, which needs libbacktrace). The configured headers are the host's
   (`/usr/include/c++/15.2.0`, Alpine's gcc 15.2.0, the same g++ and the test programs use). The build's two generated
   files (`tzdata.zi.h`, `bits/largefile-config.h`) are made from the tarball as its Makefiles make them; two units go
   through a wrapper of `test/cpp/runtime/shims/` (clang rejects `assert_fail.cc`'s `[[__noreturn__]]` on a later
   declaration; `random.cc`'s rdrand and rdseed use g++'s builtins). Not lowered (`:excluded`): `eh_personality.cc`
   (replaced, §8), `src/c++17/cow-string-inst.cc` (clang rejects it), `stacktrace.cc`.
2. **IR** by the oracle, `clang++ -O0 -S -emit-llvm` with each directory's `-std` and template flags and the per-file
   rules, from the tarball's root with `-ffile-prefix-map`s, so the IR names its sources by their paths in the tarball and
   does not depend on the checkout's place; the digests against `test/cpp/runtime/ir.sha256` (`--update` rewrites them).
3. **The forms' round trip** of each unit (§6: `clang -c` of the print equals `clang -c` of the IR), **the lowering**, gcc
   `-O0` (§7's flags) on the lowered C; the objects archived as `OUT/liblstdc++.a` (all but `exp/`) and
   `OUT/liblstdc++exp.a`, the pending-flag layer as `OUT/rt.o`. `OUT/results.tsv` and `OUT/summary.edn` record each unit's
   levels; every unit is to pass them all.

A lowered program links `rt.o -Wl,--start-group [libltestc++.a liblstdc++exp.a] liblstdc++.a -Wl,--end-group -lm` by gcc:
the C library (musl) and libgcc are native (C5 lowers musl), no libstdc++.so. What the lowered runtime does not have, by
the configuration: `std::random_device`'s rdrand and rdseed, `std::stacktrace`. Threads are musl's pthreads, natively: the
pending flag and the in-flight exception are per thread (CP103 (b)), a thread's start is the layer's trampoline (CP120 (b),
§8), and the atomics of the lowered library and programs are gcc's `__atomic_*` builtins (CP121 (c), §7): the refcounts of
`std::shared_ptr` and `std::exception_ptr`, the guards of function-local statics (`__cxa_guard_acquire`, libsupc++'s,
lowered) and `std::atomic` are shared state that works across threads (the runtime sample `atomics.cpp`).
