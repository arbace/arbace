# rsi: Rust's eval (spec)

Status: RE1's spec, 2026-11-03, with RE2 built (doc/rust/RUST-EVAL-SURVEY.md, "As built: RE1-RE2 (write-ahead)"). The survey's
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
   -ffp-contract=off`) compiles the printed C, the second judge (survey §8.1); from RE4, ci runs the C forms, and from RE6 cf2g
   compiles them (AOT).
5. **The link** (§4 until RE3, §3 from RE3).

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
| `lower-check PLAN WORK [--jobs N] [--record]` | each program built and run from the original and from its lowered IR, judged (§7) | RE2 |
| `runtime-build`, `run`, `check [--aot]`, `divergences`, `record-ref` | std lowered; ci; cf2g; the classes; the judge's runs recorded | RE3-RE6 |

The pieces: `arbace/rust/lower.ae` (`arbace.rust.lower`: the driver over the lowering, the native-std leg's substitution),
`arbace/rust/check.ae` (`arbace.rust.check`: the sets, the plan's filter, the judge), `arbace/rust/rt/native.c.ae` (§4's layer),
`bin/lib/rsi/wrap` and `bin/lib/rsi/link` (the lowered side's build), `bin/lib/repr-rust/run-program` (RR5's, the original;
its sides take a WRAP since RE2), `test/rust/runtime/` (std's pins; `native/try.rs`).

## 3. std's build (RES3 (a))

One sysroot, built from the pinned `rust-src` (`tools/rsdump/apks.sha256`) by Cargo's `-Zbuild-std` offline (RR5's build: the
library's vendored crates by source replacement), is what every lowered program and the judge compile against from RE3 on; its
IR is lowered as CX7 lowered libstdc++. `test/rust/runtime/std.edn` pins:

- the oracle and the assembler: `rustc 1.99.1 (58cf8f9c3 2026-10-13) (Alpine Linux Rust 1.99.1-r0)` (LLVM 23.1.3) and `Alpine
  clang version 23.1.4` (`bin/rsi std-build` refuses others; a move is a recorded decision, as for `RUSTC_VERSION`);
- the profile: Cargo's release profile with `opt-level 0`, `debug-assertions false`, `overflow-checks false`, `panic unwind`,
  `debug 0`, `trim-paths all` (survey §7.3: at `-O0` the lowering's tail stays the programs'; std's own checks off as in the host's release std,
  so its panics' texts are the judge's);
- the flags: `-Csave-temps -Cembed-bitcode=yes` (the units' bitcode kept; the rlibs carry bitcode for `-C lto` programs), and
  the crates `std,panic_unwind,panic_abort,proc_macro,test`;
- the IR: each codegen unit's bitcode made IR text by `clang -S -emit-llvm`, `test/rust/runtime/ir.sha256` (`CRATE/UNIT.ll`).

Determinism: two builds at the same place give the same 103 units byte for byte (51 MB of IR, 38 s of Cargo on 8 jobs). The
place matters: Cargo's `-Zbuild-std` hashes std's packages by their absolute path into `-C metadata` (measured: the same
library at two places gives other disambiguators, so other symbol names and unit files, whatever `--remap-path-prefix`,
`__CARGO_DEFAULT_LIB_METADATA`, `-Ztrim-paths` or a real copy instead of links), so `bin/rsi std-build` builds in the main
checkout's `.tmp/rsi/std/` (a worktree too, under a lock; `RSI_STD_DIR` names another place, whose IR then differs from the
pin) and the pin holds there. The paths the IR prints are not the place's: std's crates' files are named relative to the
workspace (`library/core/src/any.rs`), `-Ztrim-paths` (the profile's `trim-paths all`) maps the workspace to `.` for the
vendored crates (`./library/vendor/hashbrown-0.17.1/src/raw.rs`), and the units are made IR text from `deps/` (their
`ModuleID` names the bitcode file alone): no unit names `/root`. RES17 proposes building the library's workspace itself, as rust's bootstrap does,
which hashes its members by their path in the workspace.

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
`process::exit`). The synthetic corpus by construct (GT8.6, GTS207 (a)) is RE5's: its generator writes programs by the
Reference's chapters (expressions, patterns, traits and `dyn`, closures and iterators, panics and drops, integer overflow at every
width, i128, floats' formatting, slices and strings, collections, `fmt`), each printing what it computes, a few hundred files.

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
`float16-128`, `c-variadic`, `alias`, `lto`, `wide-int`, `optimized-ir`, `speed`; the classes the survey names (§8.2: `threads`, `process`, `net`,
`inline-asm`, `naked`, `simd`, `speed`, `nondeterministic`, `stack-overflow`) enter as programs fall in them.

## 8. Panics from RE3 (RES4 (a))

The pending-flag layer proper: `eh.c.ae` split into the unwinder's entry points (`_Unwind_RaiseException`, `_Unwind_Resume`,
shared with C++) and C++'s personality part; Rust's personality (`rust_eh_personality`, std's `personality/gcc.rs`) and
`panic_unwind` lowered with std. Rust's landing pads have three clause shapes (survey P6): `cleanup`, `filter [0 x ptr]
zeroinitializer` (a `nounwind` boundary: selector -1, the IR's `panic_cannot_unwind` path), `catch ptr null` (`__rust_try`):
no `type_info` matching. Then `__rust_try` is lowered like any function and `native.c.ae` and `try.rs` are dropped.
