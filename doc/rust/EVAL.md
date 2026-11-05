# rsi: Rust's eval (spec)

Status: RE1's spec, 2026-11-03, with RE2, RE3 and RE4 built (doc/rust/RUST-EVAL-SURVEY.md, "As built: RE1-RE2 (write-ahead)", "As built:
RE3 (write-ahead)"). The survey's
decisions bind it (RES1-RES16, all as recommended, the user 2026-11-03): Rust's eval and AOT are rustc's `-O0` compilation run
on Arbace through C++'s path (RES1 (b)). This file says what rsi does and how its results are judged; the survey says why.

Words: **the oracle** (rustc 1.99.1, `tools/rsdump/RUSTC_VERSION`), **the judge** (the oracle's build of the original, run),
**LLVM forms** (`arbace.repr.llvm`), **the lowering** (`arbace.cpp.lower`, LLVM forms → C forms), **std's IR** (std's crates
compiled by the oracle to IR and lowered), as the survey defines them.

## 1. The pipeline

A program (a crate root and its modules, given as Rust forms or as text that repr/rust reads and prints back, RR3's `:full`
byte for byte) goes:

1. **The oracle**: `rustc --edition E FILE -C opt-level=0 -C debuginfo=0 -C codegen-units=1 -C save-temps --emit=llvm-ir,link`
   with the program's own flags (RES2 (a), (c): a UI test's `-O` or `-C` flags are kept, the judge built the same way). One
   codegen unit: the program's IR is one file. `-C save-temps` keeps each unit's IR beside its object and the allocator shim's
   bitcode (P5), which `clang -S -emit-llvm` makes IR text.
2. **LLVM forms**: `arbace.repr.llvm` reads each IR (the program's and the shim's); its round trip (the forms' `.ae` text read
   back equal, printed, read again equal, `clang -c` of the print equal byte for byte to `clang -c` of the IR) is recorded per
   unit (`rt.status`, `object`), not required for the run.
3. **The lowering**: `arbace.cpp.lower` (C++'s, with the Rust delta of §5), driven by `arbace.rust.lower`. A construct it does
   not take is refused with a class (`{:lower/refused CLASS}`), recorded (`lower.refused`).
4. **The back end**: until RE4, gcc `-O0` (cppi's flags: `-std=gnu23 -O0 -w -fno-builtin -fno-strict-aliasing
   -ffp-contract=off`) compiles the printed C, the second judge (survey §8.1); since RE4 ci runs the C forms and cf2g compiles
   them (AOT, §9).
5. **The link**: since RE3 with std's lowered units (§8); RE2's native-std leg (§4) stays as `--leg native`.

The run is compared with the judge's on status, stdout and stderr (§7).

## 2. Commands (RES15 (a))

`bin/rsi` (the tools' host, stage 0, evaluated from the checkout; out of `gate_key`, no gate step until the row fits GT8's
budget, RES14 (a)):

| verb | what | milestone |
|---|---|---|
| `lower FILE.ll [-o OUT.c] [--native-std]` | one IR lowered and printed as C | RE2 |
| `list [--set all\|run\|threads\|process\|net\|samples] [--sample N] [--only RE] [-o PLAN]` | the UI programs (RR5's `:programs`) as run-program's plan; the sets of §6; rsi's samples (`test/rust/eval/`) | RE1, RE2 |
| `sets` | `test/rust/eval-programs.edn` made again | RE1 |
| `std-build [--update]` | std's sysroot with its IR, pinned (§3) | RE1 |
| `lower-check PLAN WORK [--jobs N] [--record] [--leg lowered\|native] [--runtime OUT]` | each program built and run from the original and from its lowered IR, judged (§7); the leg: std lowered (RE3, the default) or the host's native std (RE2) | RE2, RE3 |
| `runtime-build [OUT] [--jobs N]` | std's IR lowered, compiled and archived by crate, the pending-flag layer (§8) | RE3 |
| `runtime-ci [OUT]` | std's lowered units, the layer and compiler_builtins read as ci's libraries, ci's ports (§9) | RE4 |
| `run [--edition E] FILE.rs [-- ARG...]`, `check [--aot] [--corpus samples,ui] [--set S] [--only LIST] [--sample N [--offset K]] [--record]`, `divergences [--aot]`, `record-ref [--sample N] [--only LIST]` | ci; cf2g; the classes; the judge's runs recorded (§9) | RE4 |
| `lower-unit [FILE.ll...]` | the lowering's unit cases (`test/rust/lower/`) against lli | RE4 |
| `synthetic [generate\|check\|ref]` | the synthetic corpus (§6, `test/rust/synthetic/`) generated from `test/rust/synthetic.edn` and its templates; checked as committed; its expected statuses against the judge's runs | RE5 |
| `judge-test` | the check's judge without ci: the samples and the synthetic corpus judged from the judge's own runs (all pass), one output changed (fails), the controls differing; seconds | RE5 |
| `check --essential`; `list`/`record-ref`/`check --corpus samples,synthetic,ui` | the nightly's essential programs (`test/rust/essential.txt`); the synthetic corpus as a corpus of the check | RE5 |

The pieces: `arbace/rust/lower.ae` (`arbace.rust.lower`: the driver over the lowering, the native-std leg's substitution),
`arbace/rust/check.ae` (`arbace.rust.check`: the sets, the plan's filter, the judge), `arbace/rust/rt/native.c.ae` (§4's layer),
`bin/lib/rsi/wrap` and `bin/lib/rsi/link` (the lowered side's build), `bin/lib/repr-rust/run-program` (RR5's, the original;
its sides take a WRAP since RE2), `test/rust/runtime/` (std's pins; `native/try.rs`).

## 3. std's build (RES3 (a))

One sysroot, built from the pinned `rust-src` (`tools/rsdump/apks.sha256`), is what every lowered program and the judge compile
against from RE3 on; its IR is lowered as CX7 lowered libstdc++. Since RE3 (RES17 (b)) it is built as rust's bootstrap builds std:
Cargo on `library/`'s own workspace (`library/Cargo.toml`, the package `sysroot` with the features `panic-unwind backtrace`), offline,
the vendored crates by source replacement (`--config`), `RUSTC_BOOTSTRAP=1`, `-Zforce-unstable-if-unmarked` (as bootstrap gives every
std crate: without it std's const `HashMap::with_hasher` is refused). Until RE3 it was Cargo's `-Zbuild-std` from a scratch package,
which hashes std's packages by their absolute path, so the pin held at one place alone; a workspace's members are hashed by their
path in it, and three builds at three places (two scratch directories and `.tmp/rsi/std`, in a worktree) gave the same 110 digests.
`test/rust/runtime/std.edn` pins:

- the oracle and the assembler: `rustc 1.99.1 (58cf8f9c3 2026-10-13) (Alpine Linux Rust 1.99.1-r0)` (LLVM 23.1.3) and `Alpine
  clang version 23.1.4` (`bin/rsi std-build` refuses others; a move is a recorded decision, as for `RUSTC_VERSION`);
- the profile: Cargo's release profile with `opt-level 0`, `debug-assertions false`, `overflow-checks false`, `panic unwind`,
  `debug 0`, `trim-paths all` (survey §7.3: at `-O0` the lowering's tail stays the programs'; std's own checks off as in the host's release std,
  so its panics' texts are the judge's), and the workspace manifest's own per-package settings overridden (`--config`, which Cargo
  takes over the manifest): `compiler_builtins` at 16 codegen units (the manifest's 10,000 made 337 units), the backtrace crates at
  `opt-level 0` (the manifest's `"s"`);
- the flags: `-Csave-temps -Cembed-bitcode=yes -Zforce-unstable-if-unmarked`; the package `sysroot`, its features;
- the IR: each codegen unit's bitcode made IR text by `clang -S -emit-llvm`, `test/rust/runtime/ir.sha256` (`CRATE/UNIT.ll`): 110
  units of 25 crates (the package `sysroot`'s own unit and std's dylib's allocator shim left out), 51 MB, 45 s of Cargo on 8 jobs.

The paths the IR prints are the workspace's: std's crates' files relative to `library/` (`core/src/any.rs`, where `-Zbuild-std`'s
scratch package gave `library/core/src/any.rs`), the vendored crates' `./vendor/hashbrown-0.17.1/src/raw.rs`; no unit names `/root`.
The panics' locations in std print so on both sides (the judge compiles against the same sysroot). The sysroot is the host's but
for its rlibs, the build's (std's dylib is left out: a `-C prefer-dynamic` program does not build on either side).

**share-generics** (survey §7.1): at `-O0` a program calls generic instances exported by std's crates under their
disambiguators, so a program must be compiled against the sysroot whose IR is lowered: every cache key from RE3 on includes the
sysroot's key (the digest of `ir.sha256`).

## 4. The native-std leg (RE2)

Until RE3 lowers std, the lowered program is linked with **the host's native std** (survey P4's method): rustc's own link, its
command line as rustc gives it to the linker (crt objects, the host sysroot's rlibs, `-lgcc_s`, `-lc`), with the program's
codegen unit's object and the allocator shim's replaced by the gcc objects of their lowered IR (`bin/lib/rsi/link`, rustc's
`-C linker` with `-C linker-flavor=gcc`, called after codegen, when the IR and the shim's bitcode exist). So the lowered code
is the program's own and its monomorphized generics; std's non-generic code is native.

Panics on this leg unwind **natively**: std's `panic_unwind` raises them with libgcc's unwinder. gcc `-O0` writes unwind tables
(`.eh_frame`) for the lowered C, so the unwinder passes through the lowered frames, but they have no personality: their
landing pads do not run. The layer `arbace/rust/rt/native.c.ae` therefore never raises the pending flag (its `cx$exception`,
`cx$catches` answer as if nothing were in flight; `cx$resume`, `cx$uncaught` abort; the renamed `pthread_create`, `atexit`,
`__cxa_atexit`, `at_quick_exit` forward to musl's). Where the original catches, the lowered program catches too: rustc's
internal `__rust_try` (catch_unwind's frame: `invoke` of the closure, a `catch ptr null` landing pad, the catch function
called with the payload) is not lowered but called as `cx_rust_try` (`test/rust/runtime/native/try.rs`, `#![no_std]`, the
`catch_unwind` intrinsic itself, compiled by the oracle natively), so a panic in a lowered closure is caught by a native frame
and handed to the lowered `do_catch`. What does not run: the drops of the lowered frames a panic passes through (their
cleanup pads), so a program whose output shows a drop while unwinding, or that panics in a drop, differs; and a test built
with `-O` (fat LTO) whose catch was inlined into lowered code has no native frame to catch its panic (std aborts: "failed to
initiate panic"): the class `native-unwind` (RE3).

## 5. The lowering's Rust delta (RE2)

What rustc's `-O0` IR uses that clang's C++ rarely does (an IR scan of the UI suite's 3,718 programs, survey §5.3), each its
own commit in `arbace.cpp.lower` (RES16: the rename to `arbace.llvm.lower` waits for C++'s CX8):

- **Intrinsics**: `llvm.{u,s}{add,sub}.sat` (gcc's overflow builtins, the limit on overflow), `llvm.fsh{l,r}` (rotates and
  funnel shifts, a shift of 0 the operand), `llvm.{s,u}cmp` (-1, 0, 1), `llvm.bitreverse` (8 to 128 bits), `llvm.bswap.i128`,
  `llvm.fpto{s,u}i.sat` (Rust's `as` from floats: NaN 0, the bounds by `__builtin_ldexpl`), `llvm.ptrmask`, `llvm.powi`,
  `llvm.{minimum,maximum}num`, `llvm.load.relative`, `llvm.vector.reduce.*`, and any elementwise intrinsic on vectors
  (`.vNty`) as its scalar form element by element. Already taken for C++: the `with.overflow` family on i128, `ctlz`/`cttz`
  on i8/i16, `ctpop`/`ctlz`/`cttz` on i128, `threadlocal.address`, `x86.sse2.pause`.
- **Vectors** (RES6 (a)): a fixed-width vector is the struct of one array the lowering already gave it (loads, stores,
  `extractelement`, `insertelement`, constants); arithmetic, `icmp`, `fcmp`, casts, `select` on vectors and `fneg` are the
  scalar operation element by element; `shufflevector` copies the mask's elements (poison ones zero); `<N x i1>` holds a byte
  an element and its `bitcast` to and from `iN` packs and unpacks the bits (element i is bit i); `splat (T v)` constants.
  hashbrown's SSE2 `Group` (every `HashMap`) and `core::simd`'s portable vectors lower; refused (`:vector`, `:intrinsic`):
  scalable vectors, `llvm.masked.*` (gathers, scatters, masked loads and stores: four `simd/` programs), a bitcast of a mask
  to another width.
- **i128**: C forms' `__int128` (CS34); the scan found no i128 operation the lowering did not take (the sample
  `test/rust/eval/intrinsics.rs` prints i128 arithmetic, casts and formatting).
- **Names**: v0 mangling through `__asm__` labels (as built for C++).
- **Inline assembly**: an empty template with no outputs (`asm sideeffect "", "~{memory}"`, `black_box`'s and fences'
  barrier, in nearly every program) is nothing at `-O0`, as for C++; any other is refused (`:inline-asm`).

## 6. The program lists (RE1)

The UI suite's run programs are RR5's `:programs` of `test/rust/corpus-rustc-1.99.1-ui.edn` (3,718 revisions: run-pass, run-fail,
run-crash; the 574 left out by reason in its header). `test/rust/eval-programs.edn` (`bin/rsi sets`) sets apart the programs
whose own text uses threads, processes or sockets (RES7 (a): a class `threads` in v1; `process` and `net` classes as C's), by a
search of the file's text (a program's revisions follow its file; a program's out-of-line modules are not searched):

| set | programs (files) | searched for |
|---|---:|---|
| threads | 175 (157) | `thread::spawn`, `scope`, `Builder`, `std::thread`, `thread::sleep`, `mpsc::`, `Barrier`, `spawn(` |
| process | 63 (61) | `process::Command`, `Command::new`, `fork(`, `execv`, `process::Stdio`, `.spawn()`, `current_exe` |
| net | 5 (5) | `std::net`, `TcpListener`, `TcpStream`, `UdpSocket`, `net::` |
| run (in none of them) | 3,506 | |

A program may be in several sets (212 programs are in at least one). The search over-approximates (a test that names
`thread::sleep` or a `.spawn()` of its own); RE4 narrows it by what a program does on ci (a class is judged by the run, the
set only says where to look first).

On the native-std leg the sets run like the rest (std's threads and processes are native); they matter from RE4, where ci is
single-threaded and spawns no process.

rsi's own samples (`test/rust/eval/`, `bin/rsi list --set samples`): `intrinsics.rs` (§5's intrinsics at several widths, i128),
`vectors.rs` (HashMap, HashSet, `core::simd`), `panics.rs` (catch_unwind's payloads, overflow checks, nested catches,
`process::exit`). **The synthetic corpus** by construct (GT8.6, GTS207 (a); RE5, RES35): `bin/rsi synthetic` writes
`test/rust/synthetic/*.rs` from the table `test/rust/synthetic.edn` (`{:id :template :status :params}`) and the templates
`test/rust/synthetic-templates/*.tmpl` (`@NAME@` replaced by the row's params), `arbace.rust.synthetic`; the files are committed
(`check` requires them to be what the table gives). 67 programs at RE5: integer operations at each of the 12 widths (`int-ops`:
checked, wrapping, saturating and overflowing arithmetic, bit counts, rotations, byte swaps, shifts, radix parsing, formatting of
the extremes, casts to floats), floats' formatting and operations at f32 and f64, 15 handwritten programs by chapter (patterns,
traits and `dyn`, closures, iterators, drops and unwinding, strings and unicode, collections, `fmt`, i128, casts at every width pair,
`Rc`/`RefCell`, algorithms, errors, generics and lifetimes, structs/enums/unions/statics), 15 integer-overflow panics (each operator,
at several widths) and 19 other panics (index, unwrap, expect, `RefCell`, string boundary, `assert_eq!`, slice range, ...) with
status 101 and the message, and the exits: `process::exit(37)`, `main` returning `Err` (1), `abort` (134), the trap (132). Every
program prints what it computes, with no input, time or hash order (maps are sorted before printing). A program is a row of the
check's corpus `synthetic` (prefix `test/rust/synthetic`); the table's `:status` is what rustc's run gives
(`bin/rsi synthetic ref` compares it with `test/rust/ref-rustc-1.99.1.edn`).

## 7. The judgment and the classes

`bin/rsi lower-check` builds and runs each program with RR5's `run-program`: the baseline `orig` (rustc, the host's sysroot until
RE3, then std's), the side `lowered` (the same command line given to `bin/lib/rsi/wrap`), the run's environment and
normalizations RR5's (thread ids, a harness's time; 60 s; empty stdin; `--test` with one thread), and its nondeterminism rule (a
differing program's baseline run twice more). `arbace.rust.check judge` gives each program a result and a reason:

| result | when |
|---|---|
| `pass` | stdout, stderr and status equal (or equal once columns are normalized) |
| `refused` | the lowering refused the program's or the shim's IR: `refused CLASS MESSAGE` |
| `gcc` | gcc did not compile the lowered C |
| `build` | the lowered side's rustc failed otherwise |
| `differs` | a run differs; `(the original panics)` when the original's stderr shows a panic |
| `nondet`, `skip` | the baseline's own runs differ; the baseline does not build or times out (not judged) |

Every program that does not pass must fall in a class of `test/rust/divergences-lower.edn` (goi's format, RES13 (a): the first
class whose `:match` finds `RESULT REASON`); one in none fails the check. `--record` writes the results by id into
`test/rust/lower-results.edn`. From RE5 the same judge reads the judge's recorded runs (`test/rust/ref-rustc-1.99.1.edn`, RES5)
and the classes `test/rust/divergences{,-aot}.edn`. RE2's classes (11): `native-unwind`, `known-bug`, `inline-asm`, `simd`,
`float16-128`, `c-variadic`, `alias`, `lto`, `wide-int`, `optimized-ir`, `speed`; RE3's (the lowered std's leg, `:legs [:lowered]`;
`native-unwind` is the native leg's alone): `not-lowered` (a stub of std reached, §8), `backtrace` (the original prints the native
unwinder's backtrace under `RUST_BACKTRACE`); RE3's records `test/rust/lower-std-results.edn`, RE2's `lower-results.edn`; the classes the survey names (§8.2: `threads`, `process`, `net`,
`inline-asm`, `naked`, `simd`, `speed`, `nondeterministic`, `stack-overflow`) enter as programs fall in them.

## 8. std lowered (RE3)

`bin/rsi runtime-build [OUT]` (default `.tmp/rsi/runtime`; `std-build` first when `.tmp/rsi/std`'s IR is not at the pin): each unit of
std's crates but `compiler_builtins` (94 of 110) read as LLVM forms, round-tripped (the forms' print assembled by clang, its object
equal to the IR's), given the runtime's pre-pass, lowered, compiled by gcc `-O0` and archived by crate (`OUT/lib/libCRATE.a`), the
layer `arbace/rust/rt/eh.c.ae` as `OUT/rt.o`; `OUT/results.tsv` (unit, round trip, objects, lowering, gcc, stubs), `OUT/stubbed.tsv`,
`OUT/key` (as cppi's runtime key: the lowering's namespaces, the layer, std's pins, the build's steps, gcc's flags and version, clang's,
the tools' host). About 2 minutes at 8 jobs (lowering 105 s, gcc 12 s).

**compiler_builtins stays native**: it is the C compiler's builtins (libgcc's role for the lowered C++ of CX7), and its units are the
ones the lowering cannot take (f16 and f128 arithmetic, `global_asm!`'s probestack, `xgetbv`); its rlib from the sysroot is linked as
it is. On ci (RE4) the builtins are ci's own.

**The pre-pass** (`arbace.rust.lower`'s `runtime-forms`, `lower-list --runtime`):

- the inline assembly std runs, answered by the layer (RES8 (a)): `core::arch`'s `__cpuid_count` as std_detect inlines it (the `cpuid`
  template with rbx saved) calls `rsi_cpuid`, `llvm.x86.xgetbv` is `rsi_xgetbv`; the CPU answered is a fixed x86-64 baseline (leaf 0:
  the highest leaf 1, the vendor `ArbaceRsiCPU`; leaf 1: fpu, tsc, cx8, cmov, mmx, fxsr, sse, sse2; nothing else), so std's
  runtime-detected features are off and its generic paths run;
- functions whose IR the lowering does not take, replaced by the layer (`replaced-functions`: the body a call of the layer's function
  with the same parameters): u128's `carrying_mul_add`, which multiplies in i256 and which u128's `Display` calls
  (`rsi_u128_carrying_mul_add`, by 64-bit limbs);
- the other defines whose IR names a type the lowering does not take (`half`, `bfloat`, `fp128`, integers past 128 bits) or inline
  assembly it does not answer: stubs of the same name and linkage (`void`, no parameters) that print `rsi: not lowered: NAME (WHY)` and
  abort; a direct call of one goes through a cast pointer, as calls whose types differ do; declarations of such types no longer
  referenced dropped. At RE3: 33 functions, all `core`'s f16 (its formatting, parsing and arithmetic); a program that reaches one is
  in the class `not-lowered`.

**The link** (`bin/lib/rsi/link` with `RSI_RUNTIME`, `RSI_SYSROOT`): rustc's own link line against the sysroot, the program's unit and
the allocator shim (per program, from `-C save-temps`) as their lowered objects, the layer's `rt.o` beside them, each rlib of the
sysroot replaced by its crate's archive in the same place of the line (the line's order is the crates' dependency order, and one
archive a crate keeps `panic_unwind` and `panic_abort`, which both define `__rust_start_panic`, apart), `compiler_builtins` native, no
`-lgcc_s`: the lowered program links musl alone (`ldd`).

**Panics** (RES4 (a); the layer `arbace/rust/rt/eh.c.ae`): the unwinder's entry points (`_Unwind_RaiseException`, `_Unwind_Resume`,
`_Unwind_Resume_or_Rethrow`, `_Unwind_DeleteException`) raise the pending flag; the lowered frames return while it is set, through
their cleanup pads (drops while unwinding run), to a `catch ptr null` pad (`__rust_try`, lowered like any function: `cx$catches` takes
every exception for a null clause, Rust has no typed catch) or to a `filter [0 x ptr] zeroinitializer` pad (selector -1, the IR's
`panic_cannot_unwind`/`panic_in_cleanup` path, std's abort). `rust_eh_personality` is lowered with std and never called; the queries
the personality and std's backtrace make of the unwinder (`_Unwind_GetIP`, `_Unwind_Backtrace`, ...) answer as an unwinder that finds
no frame. A panic no frame catches (none in std's threads: `lang_start` and the thread start catch every panic) prints what std's
`rust_panic` prints when the native unwinder finds no handler, `fatal runtime error: failed to initiate panic, error 5, aborting`, and
aborts. The trampolines of `pthread_create` and the exit handlers are C++'s (CP120 (b), CP160 (b)), Rust's copy. RES4 (a)'s one layer
for both languages (`eh.c.ae` split into the entry points and C++'s personality part) waits for RES16's rename (RES23): until then the
Rust layer holds the entry points it needs.

The native-std leg's `native.c.ae` and `test/rust/runtime/native/try.rs` stay for `--leg native` (RE2's records) and nothing else.

## 9. The ci leg (RE4)

`bin/rsi run|check|check --aot|record-ref|divergences|runtime-ci` (`bin/lib/rsi/{run,check,record-ref,wrap-ir,link-ir,plan.bash}`,
`arbace/rust/run.ae`, the judge `arbace.rust.check/run-judge`). The program's IR comes from the oracle as for the lowered leg
(`bin/lib/rsi/wrap-ir`: the plan's command line, one codegen unit, `-C save-temps`), but its linker `bin/lib/rsi/link-ir` links
nothing: it keeps the program's unit (`x.ll`) and the allocator shim's (`shim.ll`). `arbace.rust.run` lowers both, reads their C by
repr/c with ci's options (C++'s ci macros: gcc's `__atomic_*` as plain operations on ci's one thread, the math builtins ci lacks
calling musl's), checks them with ci's checker, and links them as a static link pulls archive members from its libraries in order:

1. `RSI_EXTRA_LIBS`: the check's `tests/auxiliary/rust_test_helpers.c` (compiletest's native helpers, the original's `-L native=`),
   read as a library;
2. ci's ports, `test/rust/runtime/ci/*.c` (what the lowered std needs that ci does not give);
3. std's lowered units (`bin/rsi runtime-build`'s 94, their C kept since RE4) and the pending-flag layer (`rt.c`), read by
   `bin/rsi runtime-ci` into `OUT/ci/std` (keyed: the runtime's key, run.ae's read options, repr/c, the host, the ports);
4. musl's forms (`arbace.c.musl`, ci's libc);
5. compiler_builtins, lowered for ci with the runtime's pre-pass (RES25 (b) on ci, `OUT/ci/cb`): after musl, so musl's `mem*`
   and the rest come first; its `global_asm!` unit (probestack) is not read and left out.

Each library is ci's prebuilt runtime (`arbace.c.runtime`, doc/C23-SURVEY.md "As built: ci's prebuilt runtime"): a unit's
fragment checked and built once and cached by the library's key (`CI_RUNTIME`, the checkout's `.tmp/ci-runtime/` by default), the
Rust libraries' candidates cut at the link to what the program reaches. The node program is run by `tools/cirun` (eval) or
translated by cf2g into a Go package built by TamaGo's go in shards (AOT).

**The run** is run-program's (§7): a fresh directory, an empty standard input, `env -i` with `PATH`, `HOME` and the program's
`exec-env`, its run flags (`--test-threads=1` for a harness), 60 s (`test/rust/check-budget.edn`), a 1 MiB output cap, the thread
ids and harness times normalized. **The judge** (RES5 (a)) is rustc's `-O0` build of the original against std's build, run so,
three times, recorded as data by `bin/rsi record-ref` into `test/rust/ref-rustc-1.99.1.edn` (status, the outputs' SHA-256,
`:det false` when the three runs differ). A program passes when its status and both outputs' digests are the judge's; a program
whose judge runs differ and that does not pass is `:nondet` (not a failure); every other non-pass falls in a class of
`test/rust/divergences.edn` (AOT: `divergences-aot.edn` first) or fails the check; the record `test/rust/check-linux-amd64.edn`
(`check-aot-linux-amd64.edn`) holds `[ID RESULT REASON]`, a recorded pass that fails is a regression; the per-program seconds
(IR, lowering, ci's build, run) in `test/rust/check{,-aot}-times-linux-amd64.edn` (GT8, GTS205); the negative control
`test/rust/controls.edn` (GTS81 (a)). `--only LIST`, `--sample N --offset K`, `--set` choose subsets (never `--record`ed); `--essential` is `--only test/rust/essential.txt`.

**Signals** (RE5, closing RE4's open mapping): rustc's `llvm.trap` (`core::intrinsics::abort`, an `unreachable` the code reaches) is a
`ud2`, SIGILL, status 132 in the judge's run (`128 + signal`, as the shell reports it); the lowering makes it `__builtin_trap`, which
gcc compiles to `ud2` (132) and ci raises as `Fault{SIGILL}` (`go/arbace/ci/natives.ae`), which `RunMain` reports as 128 + 4. The
mapping is the identity, no judge rule is needed (checked: `bin/ci run` of a C program gives 132 for `__builtin_trap`, 134 for `abort`,
as gcc's; the synthetic programs `trap.rs` and `abort.rs` are the Rust cases, rustc's 132 and 134). `std::process::abort` and
std's own aborts (a panic in a drop while unwinding, a panic that cannot unwind) are `abort()`, 134 (rustc's run: checked natively; on ci not yet run).
