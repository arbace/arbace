# Rust's eval and AOT: survey and plan

Status: survey and plan, 2026-11-03, at the user's request (Rust a full row of the REAP matrix: read, eval, AOT, print;
doc/JOURNAL.md, "2026-11-03: Haskell and Rust unparked"). Nothing here is adopted: no code under `arbace/`, `go/`, `bin/`,
`tools/` or `test/` changed, and only this file was written. The decisions are the user's, as proposals RES1-RES16
(§12); the milestones are RE1-RE9 (§11). They revisit REPR-RUST-SURVEY's RS15 ("no translator now") and its use RU4
("much later, if ever"): the user's unparking of 2026-11-03 asks for the row, so the question is how, not whether.

Base: branch `rust-eval-survey` from main `a25d12d1`. The work is reading (the crop surveys, doc/cpp/LLVM.md, RUST-NOTES,
GATE-SURVEY's GT8) and light probes of seconds each (§3): rustc 1.99.1's `--emit=llvm-ir` on three files of 3 to 9 lines,
their IR through `bin/repr-llvm read` and `roundtrip` and `bin/cppi lower` (C++'s path, CX6-CX7) on the main checkout's
stage 0 (`ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`, evaluated from the worktree), gcc on the lowered C.
Scratch in the worktree's `.tmp/rust-eval/` (Appendix A). No Arbace executable was built, no lock taken, nothing installed,
nothing downloaded. What the host does not hold (Miri's and mrustc's sources, rustc's compiler sources beyond the pinned
corpora) is named from memory of the projects, every such size, URL or license marked **to verify**.

Words used throughout:

- **Rust forms**: Rust as Arbace forms (doc/rust/SPEC.md), read and printed by repr/rust (`bin/repr-rust`), which exists
  (RR1-RR6) and whose round trips are the row's read and print cells (§10).
- **The oracle**: rustc 1.99.1 (`58cf8f9c3`, Alpine's `rust-1.99.1-r0`, LLVM 23.1.3; `tools/rsdump/RUSTC_VERSION`), which
  compiles Rust to what Arbace runs, as clang is C++'s oracle (CPP-SURVEY CP4 (a)). **The judge**: the same rustc's `-O0`
  build of the original, run, recorded as data (§8).
- **LLVM forms**: LLVM IR as Arbace forms (`arbace.repr.llvm`, doc/cpp/LLVM.md), C++'s, unchanged.
- **The lowering**: `arbace.cpp.lower`, LLVM forms → C forms (LLVM.md §7), C++'s, extended (§5.3).
- **rsi**: the evaluator of Rust at the REPL and for programs (the name is RES15's), as goi is Go's, ci C's, cppi C++'s.
- **std's IR**: Rust's standard library (core, alloc, std, panic_unwind and their dependencies, from the pinned `rust-src`)
  compiled by the oracle to IR and lowered, as CX7 lowered libstdc++ and libsupc++ (§7).

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 What the probes showed · 4 The candidates for eval ·
5 eval: rsi on C++'s path · 6 AOT · 7 The runtime: std · 8 The judge and the corpora · 9 Proc-macros, build scripts,
multi-crate programs · 10 The row and GT8's budget · 11 Plan (RE1-RE9) · 12 Proposals (RES1-RES16) · 13 What to decide
first · 14 Risks · 15 Sources · Appendix A: the scratch.

## Summary

- **Rust's meaning is decided by rustc after the parser** (name resolution, macro expansion including proc-macros, trait
  resolution, inference, monomorphization, drop elaboration, layout, const evaluation). As for C++, Arbace's own front end
  for that is out of reach (§4.3: rustc's is the 2,167 files of the compiler corpus, RR4; mrustc, the one independent
  compiler, took years), so **every plan below takes the meaning from the oracle**, and the eval cell is "rustc compiled
  this, Arbace runs what it produced", stated as a divergence from parity as C++'s is (CPP-SURVEY §1).
- **The probe settles that C++'s path takes rustc's IR as it is (§3).** rustc's `-O0` IR read by `arbace.repr.llvm` and
  printed back gave **the same objects as the original IR** (`clang -c`, 3 of 3 files, 5 s); `bin/cppi lower` lowered two
  of the three to C that gcc compiles; linked with the host's native std and the lowered allocator shim, `a.rs`
  (recursion, a loop, overflow checks, `process::exit`) **exits 193 as rustc's build does**, and a `no_std` program exits 6
  as its original. The third (`HashMap`, `println!`, `catch_unwind`: 676 KB of IR, 267 functions, 181 `invoke`s, 50 landing
  pads) is refused by the lowering at three missing intrinsics (`llvm.uadd.sat`, `llvm.fshl`, `llvm.scmp`) and, past them,
  at one `shufflevector` (hashbrown's SSE2 group: `<16 x i8>` splat, compares to `<16 x i1>`). The gaps are a table's
  rows and one vector idiom, not a design problem.
- **Recommendation (RES1 (b)):** eval and AOT through C++'s existing path: rustc's `-O0` IR → LLVM forms → C forms → ci
  (eval) and cf2g (AOT, C6), with std compiled by the oracle from the pinned `rust-src` to IR and lowered (§7), panics
  as C++'s pending flag with Rust's personality (simpler than C++'s: three clause shapes, §5.4). One back end then serves
  C, C++ and Rust. A MIR interpreter with a Miri-like memory model (RES1 (a)) is the route to a Rust that does not need
  LLVM, and a later option, not v1: it costs about twice as much (§4.1), duplicates the back end and is slower.
- **The judge (RES5):** rustc's `-O0` build of the original against the same std, run, recorded as data
  (`test/rust/ref-rustc-1.99.1.edn`), with RR5's normalizations (thread ids, harness timings) and its nondeterminism rule.
  **Corpora (RES9):** the UI suite's 3,718 run programs already listed by RR5 (`:programs`: run-pass, run-fail, run-crash),
  a synthetic corpus by construct (GT8.6), std's own `coretests`/`alloctests` later, the crate set's binaries last.
- **Plan (§11): RE1-RE9, about 33 to 58 working days**, after C++'s ci leg (CX7's heavy list: the lowered runtime run on
  ci) or sharing it. For scale: C++'s eval and AOT (CX6-CX8) were estimated at 26 to 44 and CX6-CX7 came in within days.
  The long poles are std's runtime lowered (RE3: TLS, the allocator, musl under std's `sys`) and the ci leg (RE4).
- **GT8 from day one (§10):** Rust's read and print levels already take 40 to 60 minutes whole (the journal, 2026-11-03),
  more than Rust's nightly share of about 35 minutes before eval adds anything; so the row enters the gate over
  deduplicated, coverage-subset corpora (GTS205 (a)) with the UI suite rotated over seven nights (GTS203 (a)).
- **To decide first (§13):** RES1 (the route), RES3 (std's build), RES7 (threads in v1), RES10 (the order against C++'s ci
  leg and the other crops).

## 1. What parity means

The crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions"; CPP-SURVEY §1), for Rust:

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **Rust read** | repr/rust `read` (RR1-RR6, exists): rsdump's trees as Rust forms | std's five crates, `library/`, the compiler, the crate set, the UI suite (13,397 + 19,942 files) | rustc's trees, interfaces, HIR, MIR, objects of the original | the tree, text and interface levels; HIR, MIR and objects of the print equal (RR6) |
| **Rust eval** | rsi: rustc's `-O0` IR as LLVM forms, lowered to C forms with std's lowered IR, run by ci | the UI suite's run programs, the synthetic corpus, later std's tests | rustc `-O0`'s build and run of the originals (status, stdout, stderr) | byte for byte after RR5's normalizations; every non-pass in a recorded class |
| **Rust AOT** | the same C forms through cf2g (C6) and Go's compiler | as eval | as eval | as eval |
| **Rust print** | repr/rust `print` (`:lines`, `:full`, pretty; exists) | every read corpus | the original text; the forms read back; rustc's view of the print | `:full` byte for byte; behaviour (`--run`, `--std`) and code levels (`--code`) |

Same as the crops: linux/amd64 alone, results recorded as data, `bin/reap` judging from the records. **Print stays as it
is** (the user's brief): RR1-RR6's levels are already stronger than most crops' (std built from its printed files, the
UI suite's behaviour, HIR/MIR/objects equal), and the only work for the print cell is putting its records in the reap
format and subsets (§10, RE8).

**What differs from Go, Java and C:** as for C++, Arbace has no semantic analysis of Rust; rsi runs the oracle's
compilation. Rust adds **proc-macros** (Rust code run by the compiler at compile time: the oracle runs them, as clang
instantiates C++'s templates; §9) and a **panic runtime** whose unwinding the pending flag carries (§5.4).

## 2. The host and prior art

### 2.1 The host

- rustc 1.99.1 (`58cf8f9c3 2026-10-13`, LLVM 23.1.3), cargo 1.99.1; host `x86_64-alpine-linux-musl`. clang 23.1.4 (LLVM
  23.1.4), which `bin/repr-llvm` uses as the IR's assembler: rustc's 23.1.3 IR text is read by clang 23.1.4 (P2).
- The sysroot's rlibs (`/usr/lib/rustlib/x86_64-alpine-linux-musl/lib/`, 30 crates: core, alloc, std, panic_unwind,
  panic_abort, unwind, libc, hashbrown, std_detect, addr2line, gimli, object, miniz_oxide, memchr, rustc_demangle, ...,
  proc_macro, test, getopts, and `rustc_public`/`rustc_public_bridge`) hold **object code only** (`libstd`: `lib.rmeta`,
  `lib.rmeta-link` and one `.rcgu.o`), no bitcode: std's IR must be built (§7).
- `rust-src` 1.99.1 is the pinned apk (`tools/rsdump/apks.sha256`), fetched into `.tmp/` by `bin/repr-rust`, with the
  vendored crates std's build needs (RR5's `-Zbuild-std` build of std: 48 s); `rustc-1.99.1-src.tar.xz` (SHA-256 pinned
  in `test/rust/corpus-rustc-1.99.1-ui.edn`) holds `tests/ui`. Neither is extracted in the main checkout today.
- `librustc_driver` and `rustc-dev` (RR's `tools/rsdump`, a `rustc_private` driver; `rsdump rustc --mir` writes MIR as
  text since RR6). `rustc_public` (stable MIR) is in the sysroot as an rlib, usable from a driver (route (a)).
- No `llvm-dis`, `opt`, `lli` (CPP-SURVEY §2.1); `clang -S -emit-llvm x.bc` turns bitcode into IR text (P5).

### 2.2 Prior art (from memory, to verify)

| project | what | relevance | license |
|---|---|---|---|
| **Miri** (`rust-lang/miri`) | an interpreter of MIR with a checked memory model (allocations with provenance, Stacked/Tree Borrows, data-race detection), the shims of libc/Linux calls std makes, run on a std built with `-Zalways-encode-mir` (`cargo miri setup`) | route (a)'s model; its speed (commonly reported 100x to 10,000x slower than native code, to verify) and coverage (much of std's own test suite runs under it in rust's CI; FFI to C does not run) | MIT or Apache-2.0 |
| rustc's const evaluator (`compiler/rustc_const_eval/src/interpret`) | the MIR interpreter Miri extends | route (a)'s core, about 15 to 20 thousand lines (to verify) | MIT or Apache-2.0 |
| **mrustc** (`thepowersgang/mrustc`) | a Rust compiler in C++ that emits C, able to build rustc (older versions) | the only independent front end; route (c)'s scale (to verify: on the order of 100k lines, years of one author's work) | MIT |
| rustc_codegen_clr (`FractalFir/rustc_codegen_clr`) | a rustc back end from MIR to .NET CIL and to C | route (a′)'s analogue (MIR → C) | MIT or Apache-2.0 (to verify) |
| rustc_codegen_cranelift, rustc_codegen_gcc | rustc's other back ends, from MIR | how much a MIR back end takes (intrinsics, ABI, unwinding) | MIT or Apache-2.0 |
| llvm-cbe (`JuliaHubOSS/llvm-cbe`) | LLVM IR → C | route (b)'s idea, as CX6 built it | University of Illinois/NCSA (to verify) |
| evcxr (`evcxr/evcxr`) | a Rust REPL: each input compiled by rustc as a crate depending on the previous ones | rsi's REPL model (§5.6) | MIT or Apache-2.0 |

### 2.3 What Arbace already has

- **C++'s path** (CPP-SURVEY "As built: CX6", "CX7"; doc/cpp/LLVM.md): `arbace.repr.llvm` (reads every instruction clang 23
  emits at `-O0`, round trip judged by `clang -c`), `arbace.cpp.lower` (structs of the data layout's sizes, SSA as locals,
  phi copies, GEP folded, an intrinsics table, `invoke`/`landingpad` as the pending flag, `x86_fp80` as CS35's
  `long-double`, `__asm__` labels for names C cannot spell, TLS, aliases, constructors), the pending-flag layer
  `arbace/cpp/rt/eh.c.ae` (the unwinder's four entry points, the personality's search for one clause), `bin/cppi
  lower|runtime|runtime-build|lower-check`. CX7: libstdc++'s 198 units lowered, 234 of 234 testsuite programs run as
  the original — **judged by gcc `-O0`, not yet by ci** (CX7.4's heavy list: "the ci leg").
- **ci and cf2g** (C5, C6; C23-SURVEY): musl 1.2.6 as C forms from `__libc_start_main`, a flat 64-bit memory (CS9),
  `setjmp`, single-threaded (class `threads`), atomics as plain operations; ci ran `fib(30)` in 0.61 s against gcc
  `-O0`'s under 0.01 s (C5.4); C's eval 2,049 and AOT 2,058 passes in the matrix.
- **repr/rust's behaviour levels** (RR5): `bin/lib/repr-rust/run-program` builds and runs a UI program as compiletest
  does (flags, `exec-env`, `--test` harness with one thread, normalizations, three-run nondeterminism rule), and std built
  by Cargo from `rust-src` into a sysroot (`-Zbuild-std`, offline, the vendored crates by source replacement). Both are
  rsi's judge and std's build, nearly as they are.

## 3. What the probes showed

Files in Appendix A; times on the shared host. All IR by `rustc --edition 2024 -C opt-level=0 -C debuginfo=0
--emit=llvm-ir`.

| probe | what | result |
|---|---|---|
| P1 the IR's size | `a.rs` (fib, a loop, `wrapping_add`, `process::exit`); `b.rs` (`no_std`, `no_main`, `panic=abort`, an `extern "C" main` summing an array); `c.rs` (`HashMap`, `split`, `entry`, `Vec::sort`, `println!("{:?}")`, `catch_unwind` of an out-of-bounds index) | a: 18 KB, 15 `define`s, 6 `declare`s, 0.08 s; b: 8.8 KB, 6 `define`s; c: **676 KB, 267 `define`s, 67 `declare`s, 181 `invoke`s, 50 `landingpad`s**, 0.17 s. Generic std code (HashMap, sort, fmt's Debug) is monomorphized into the program's IR: a Rust program's IR is C++'s size class (CX6: 0.2 to 2 MB) |
| P2 LLVM forms | `bin/repr-llvm roundtrip` of a, b, c | **3 of 3 forms equal, 3 of 3 objects equal** (`clang -c` of the print = of the original), 5 s. Nothing in rustc's IR was new to C++'s reader: v0-mangled names (`_RNv...`), `personality ptr @rust_eh_personality`, `!guid` metadata, `allocalign`/`allocptr` attributes, `captures(...)` |
| P3 the lowering | `bin/cppi lower` of each | a: 2 s, 633 lines of C; b: 308 lines; c: **refused** (`intrinsic`: `llvm.uadd.sat.i64`); with it and then `llvm.fshl.i64` and `llvm.scmp.i8.i64` replaced by IR functions in a copy (`c3.ll`): refused at `shufflevector` (one, `<16 x i8>` splat to `<16 x i32> zeroinitializer`; the IR also has 23 `<16 x i8>`, 63 `<2 x i64>`, 5 `<16 x i1>` uses: hashbrown's SSE2 `Group`) |
| P4 the runs | a.c, b.c by gcc `-O0` (cppi's flags), the pending-flag layer (`bin/cppi runtime`), linked with the host's rlibs | b with core and compiler_builtins (plus a stub `rust_eh_personality` core's objects reference): **exit 6 = the original's**; a with std's rlibs, `-lgcc_s`, and the lowered allocator shim (P5): **exit 193 = rustc's build** |
| P5 the allocator shim | rustc emits `__rust_alloc`, `__rust_dealloc`, `__rust_realloc`, `__rust_alloc_zeroed`, `__rust_no_alloc_shim_is_unstable_v2` (forwarding to `__rdl_*`, std's System allocator) in a separate codegen unit at the final link, absent from `--emit=llvm-ir` (a's link: 156 + 300 + 23 + 2 + 156 undefined references) | `-C save-temps --emit=link,llvm-bc` keeps the shim's bitcode (`a.<hash>.rcgu.bc`), `clang -S -emit-llvm` gives its IR (5 functions), `bin/cppi lower` lowers it, the link resolves. rsi needs the shim per program (or one generated per allocator choice: RES4) |
| P6 Rust's landing pads | c.ll's 50 `landingpad`s | three clause shapes only: `cleanup` (24), `filter [0 x ptr] zeroinitializer` (25: a `nounwind` boundary, followed by `panic_cannot_unwind`/`panic_in_cleanup`), `catch ptr null` (`__rust_try`, catch_unwind's intrinsic). No typed catch: no `type_info` matching |

What P4 does not show: std's own code lowered (it ran native, as CX6 did with libstdc++ before CX7), musl as C forms, and
ci; those are RE3 and RE4.

## 4. The candidates for eval

### 4.1 Overview

| route | input | what Arbace implements | reuses | verdict |
|---|---|---|---|---|
| (a) **MIR interpreted** (stable MIR / `rustc_public`, monomorphized) with a Miri-like memory model | MIR of every mono item of the program and of std (std rebuilt with `-Zalways-encode-mir`) | a MIR reader (a driver over `rustc_public`, forms), an interpreter of places, operands, rvalues, terminators; layouts from the oracle's facts; about 300 rustc intrinsics (to verify: `core::intrinsics` and `core::arch`'s LLVM links); vtables and drop glue; unwinding over `UnwindAction`s; a memory of allocations with provenance or crt's flat memory; the shims of the libc calls std makes (Miri's `shims/`, hundreds of functions) or calls into musl on ci | ci's memory and syscalls in part; nothing of the back end | **later, optional (RE10)**: about 70 to 120 days, a second back end, slow |
| (a′) MIR lowered to C forms (rustc_codegen_clr's and mrustc's shape) | the same MIR | as (a) without the interpreter, plus the C emission | ci, cf2g | the same front costs as (a); its gain over (b) is only independence from LLVM |
| (b) **LLVM IR through C++'s path** | rustc's `-O0` IR (P1) | the lowering's Rust delta (intrinsics, the vector idiom, i128 breadth), std's IR built and lowered, Rust's panic runtime on the pending flag, the shim | **repr/llvm, the lowering, the pending flag, ci, cf2g, CX7's runtime build, RR5's judge** | **recommended**: P2-P4 already run Rust; 33 to 58 days |
| (c) a Rust front end in Arbace | Rust forms | rustc's semantic analysis | none | out of reach (§4.3) |

### 4.2 Route (a): MIR and a Miri-like interpreter

What it would take: (1) a dumper: `rustc_public` gives a crate's items, `Instance`s and their bodies (generic
arguments substituted, to verify), types and layouts; std's non-generic functions have MIR in the rlibs only when built
with `-Zalways-encode-mir` (Miri's `cargo miri setup` rebuilds std for this), so std is rebuilt anyway (as in (b)); (2) MIR
forms (a spec: about 30 statement and terminator kinds, 25 rvalue kinds, places with projections, constants as
allocations); (3) the interpreter: Miri's memory model has allocations with ids, provenance, alignment checks,
uninitialized bytes, and its *checks* (borrows, data races) are what make it slow and what Arbace does not need to run
correct programs; a flat memory (crt's, CS9) with pointers as addresses suffices for programs without UB, which is what
the corpora are; (4) intrinsics (`core::intrinsics`, the SIMD `simd_*` family `core::arch` and `portable-simd` use, and
`llvm.*` links in `std_detect` and `core::arch`, which Miri implements one by one); (5) the foreign functions std calls
(`libc` crate: `write`, `mmap`, `pthread_*`, `clock_gettime`, `getrandom`, ...): either Miri's way (shims emulating Linux)
or calls into musl as C forms on ci (one memory shared with ci: crt's); (6) unwinding (MIR's `UnwindAction::Cleanup` and
`UnwindTerminate` make it explicit, easier than IR's); (7) for AOT, a second translator (MIR → Go forms or → C forms).

Cost, by analogy (to verify at a trial): Miri plus rustc's interpreter are tens of thousands of lines of Rust written over
years; goi (Go forms interpreted, GR1-GR10) and ci (C5, 8 to 14 days estimated) are Arbace's own scale points. An
interpreter for monomorphic MIR is closer to ci than to goi (no type checker of its own: the oracle's MIR is typed and
checked), but the intrinsics, layouts, vtables and shims are each a goi-sized tail. **About 70 to 120 days**, with AOT a
further back end. Speed: an interpreter of MIR on Go's runtime, evaluated through Arbace's nodes as ci is, would be of
ci's order (60x and more against `-O0`), which (b) also pays in eval; but (b)'s AOT is cf2g, already built.

Its merits: no LLVM in the path (rustc's MIR is Rust's own semantics; a future native Rust front end would target it);
MIR's unwinding and drops are explicit; UB detection possible (not a goal). Its weaknesses for v1: it shares nothing with
C++'s back end; its coverage is Miri's after years, and Miri still lacks FFI and some platform calls; std must still be
rebuilt. **Not recommended for v1**; listed as RE10 (optional, later), with a dumper of MIR forms (`repr/mir`, RS15's
idea) as its first step if the user ever wants Rust's semantics without LLVM.

### 4.3 Route (c): a Rust front end in Arbace

Out of reach, as C++'s Sema is (CPP-SURVEY CP4 (e)): rustc's analysis is name resolution with hygiene, `macro_rules!`
and the built-in macros, **proc-macros (Rust programs that must themselves be compiled and run)**, the trait solver
(coherence, auto traits, associated types, specialization in std), type inference with closures and coercions, the borrow
checker (not needed to run correct code, but needed to reject), const evaluation (itself a MIR interpreter: route (a)
again), monomorphization, drop elaboration, layout (niche filling, enum layouts) and the ABI. The compiler corpus RR4
reads is 2,167 files; mrustc, which skips borrow checking, is years of one author (to verify). And std uses nightly
features throughout, so any front end must track rustc's unstable surface release by release. Hundreds of days, not
planned.

## 5. eval: rsi on C++'s path

### 5.1 The pipeline

Rust forms (repr/rust) → **printed** to Rust text (`:full` or `:lines`, RR1-RR3) → the oracle: `rustc -C opt-level=0
-C debuginfo=0 --emit=llvm-ir,link` against **the pinned std's sysroot** (§7), `-C save-temps` for the allocator shim (P5)
→ LLVM forms (`arbace.repr.llvm`, P2) → C forms (`arbace.cpp.lower` with the Rust delta, §5.3) → linked with std's lowered
C forms and musl's (C5) → **ci** (eval) or **cf2g** (AOT, §6). The program's IR and its lowered C forms are cached by key
(IR digest, the lowering's sources), as CX7 keys the runtime.

Programs given as text (the corpora) are read by repr/rust first and printed back, so eval's input is always forms (the
row's rule); `:full` prints the original byte for byte (RR3), so the program compiled is the original. The judge
compiles the original (§8).

### 5.2 Why `-O0`

At `-O0` rustc keeps overflow checks and debug assertions (P1: `panic_const_add_overflow`), emits no vectorized loops
(hashbrown's explicit SSE2 is the one vector source, P3), and inlines nothing: the IR is the program's MIR almost line
by line, which keeps the lowering's long tail short. The judge's build uses the same flags (RES5).

### 5.3 The lowering's Rust delta

From P3 and from what rustc's code generator emits that clang's `-O0` C++ rarely does (to measure over the corpora at
RE2, the list below is a start):

- **Intrinsics**: saturating arithmetic (`llvm.{u,s}{add,sub}.sat`), funnel shifts and rotates (`llvm.fsh{l,r}`), three-way
  compares (`llvm.{s,u}cmp`, LLVM 19's), `llvm.bitreverse`, `llvm.abs` on every width, `llvm.ctlz`/`cttz` on `i8`/`i16`,
  `llvm.ptrmask`, `llvm.read_register`/`frameaddress` (std's stack probes, to check), `llvm.x86.*` called by `core::arch`
  and `std_detect` (`cpuid`, `xgetbv`, `rdrand`: answered by a fixed CPU model, RES8).
- **Vectors**: hashbrown's `Group` (SSE2: `_mm_set1_epi8`, `_mm_cmpeq_epi8`, `_mm_movemask_epi8` lower to a splat
  `shufflevector`, vector `icmp`, `bitcast <16 x i1> to i16`); memchr's and `core::str`'s SSE2 paths. Options (RES6): lower
  fixed-width vectors as arrays elementwise (what CX6 wrote for load/store/element access, extended to `shufflevector`,
  vector compares, bitcasts of `<N x i1>`); or build std and the programs with hashbrown's generic group
  (`-C target-feature=-sse2` is not an option on x86-64: floats need SSE2).
- **`i128`** everywhere in `core::fmt` (u128 formatting), `core::num`, `Duration`: C forms' `__int128` (CS34's limbs on ci).
- **Names**: v0 mangling (`_RNvCs...`, with `.` and `$` in some) through `__asm__` labels (LLVM.md §2), as built.
- **Calls through `extern "rust-call"`, `#[track_caller]`** (an extra `&Location` argument: plain IR), **`#[naked]`** (asm:
  refused, a class), `asm!` (std uses it for `cpuid` and stack probes: a table of the few std needs, the rest refused as
  `inline-asm`, a class), **`#[thread_local]`** (`thread_local!`'s fast path: `llvm.threadlocal.address`, already
  handled for C++).

### 5.4 Panics on the pending flag

Rust's unwinding is the Itanium ABI's with its own personality (`rust_eh_personality`, `library/std/src/sys/personality/gcc.rs`)
and exception class (`panic_unwind`'s `gcc.rs`: `_Unwind_RaiseException` on a boxed payload). P6's three clause shapes
map onto LLVM.md §8 directly and more simply than C++'s:

- `catch ptr null` (`__rust_try`, under `catch_unwind` and `std::rt::lang_start`'s call of `main`): every panic;
- `filter [0 x ptr] zeroinitializer`: a `nounwind` boundary (`extern "C"` without `-unwind`, drop during unwinding): the
  selector -1, whose path the IR already fills with `panic_cannot_unwind`/`panic_in_cleanup` (abort, status 134);
- `cleanup`: drops while unwinding.

So `panic_unwind` and the personality's search are lowered as code (as CX7 lowered libsupc++), and only the unwinder's
entry points are the pending-flag layer (`_Unwind_RaiseException` and `_Unwind_Resume` raise the flag; the
`_Unwind_Exception` header is the same struct). No `type_info` matching is needed; the layer is a Rust variant of
`eh.c.ae` without `__cxa_*` (RES4: `arbace/rust/rt/eh.c.ae`, or `eh.c.ae` split into a common part and C++'s). The no-
search-phase difference of LLVM.md §8 (cleanups run before an uncaught exception terminates) mostly vanishes for Rust:
every thread's entry (`lang_start`, `std::thread`'s spawn closure) catches every panic, so no panic is uncaught; a panic in
a `Drop` during unwinding aborts at the `filter` as rustc's build does. `-C panic=abort` programs (UI's `panic-runtime/`)
need nothing: no landing pads. Panics through musl's native-turned-lowered frames (`qsort` callbacks) are C++'s class
`native-callback-throw`, empty once musl is C forms on ci.

### 5.5 What ci must take

The lowering's output for C++ already uses GNU attributes, `__builtin_*`, `_Thread_local`, statement expressions,
`__asm__` labels (CX6.3, CX7.3), which **ci must accept for C++'s ci leg** (CX7.4). Rust adds nothing of that kind beyond
`__int128` breadth and vector arrays. The ci leg is therefore shared work: whichever of C++ and Rust goes first does it
(RES10).

### 5.6 The REPL

A REPL input of Rust forms is an item or an expression. evcxr's model (each input a crate compiled by rustc against the
previous inputs' crates, `-L` and `--extern`, the session's state in `static`s): rsi compiles each input to IR as a crate,
lowers and loads it into ci's running program (ci's builder links by symbol; v0 names are unique per crate). Variables
that live across inputs are boxed in a session map, as evcxr does. This is RE7; the check's programs need none of it.

## 6. AOT

Rust's AOT is C's AOT (C6, cf2g: C forms' node program → Go forms → Go's compiler) on the lowering's C forms, as C++'s
CX8 plans: rustc IR → LLVM forms → C forms → Go forms. New work: none in cf2g beyond what ci needs (§5.5); the cost is
the shards (a UI program plus lowered std is a large node program: std's lowered code translated **once per std key** and
linked, as the Java build's compiled namespaces are, CP100 (c)'s store) and the check (`bin/rsi check --aot`). **Java
forms through j2g** are a possible second target by a lowering to Java forms; not proposed (one back end).

## 7. The runtime: std

### 7.1 What must be built

The program's IR holds its own code and std's *generic* code instantiated for it (P1). Std's *non-generic* code
(`std::rt::lang_start_internal`, `std::io::stdio`, `fmt::write`, `panicking`, `sys::*` over `libc`) is in the rlibs as
objects only (§2.1). And at `-O0`, rustc's `share-generics` makes the program call **instances exported by the upstream
crates** (P1's c.ll declares `RawVecInner::reserve::do_reserve_and_handle` instantiated in `addr2line`), named with the
upstream crates' disambiguators (`CseZIPsj5T9LK_3std`). So std's IR must come **from the same build of std the program
is compiled against**: one sysroot built from the pinned `rust-src` with its IR kept, pinned by digest, and every program
(and the judge's build) compiled with `--sysroot` pointing at it (RES3). RR5's `-Zbuild-std` build (`.tmp/repr-rust/std/orig`,
48 s, offline, vendored crates by source replacement) is that build with `-C save-temps` or `--emit=llvm-bc` added and
`clang -S -emit-llvm` per codegen unit (P5's method), as CX7's `runtime-build` builds libstdc++'s IR from its tarball.

### 7.2 The crates and their sizes (to measure at RE3)

core, alloc, std, panic_unwind (and panic_abort), unwind (replaced: the pending-flag layer stands for libgcc's unwinder),
compiler_builtins (C's builtins: largely redundant with the lowering's own arithmetic; its `mem*` and `__udivti3` family
lowered or mapped to ci's), libc (the crate: declarations over musl), hashbrown, std_detect, rustc_demangle, addr2line,
gimli, object, miniz_oxide, adler2, memchr, cfg_if (backtrace's crates: RES8 drops backtraces), and for `--test`
programs `test` and getopts. CX7's libstdc++ IR was 38 MB and lowered in 4 to 5 minutes; std's IR at `-O0` is likely of
that order (to verify).

### 7.3 std's profile (RES3)

Built at `opt-level=0` (no autovectorization, the lowering's tail stays P3's), `debug-assertions` and `overflow-checks`
**off** in std (as the host's release std, so the panics' texts match the judge's against the same std), `panic=unwind`,
`codegen-units` as Cargo's default (the shim and the CGU split do not matter: the lowering links by symbol). The judge's
builds use the same sysroot, so the comparison holds whatever profile is chosen; RR5 already showed the UI programs
behave the same against a source-built std as against the host's (3,683 the same, 25 nondeterministic, 10 not judged).

### 7.4 std over musl on ci

std's `sys/pal/unix` calls musl through the `libc` crate: `write`/`read`, `mmap`/`munmap` and `malloc` (std's System
allocator: musl's `malloc`, mallocng on ci, C5.3), `pthread_*` (threads, TLS keys, `pthread_getattr_np` for the main
thread's stack guard), `clock_gettime`, `getrandom` (HashMap's random keys: P1's `hashmap_random_keys`; the judge and
rsi differ unless fixed: RES8), `sigaction`/`sigaltstack` (stack overflow handler: install succeeds or is skipped),
`getenv`, `args` (`std::env::args` from `lang_start`'s argc/argv, as musl's `__libc_start_main` passes them on ci),
`open`/`stat`/`readdir` (`std::fs`), sockets (`std::net`), `posix_spawn`/`fork`/`exec` (`std::process::Command`).
ci provides musl's syscalls through Go (C5): the first five and `std::fs` run; threads are RES7; process spawning and
networking are classes (`process`, `net`) as C's are.

### 7.5 Threads and atomics (RES7)

ci is single-threaded; atomics lower as plain operations (CX6). The UI suite has about a hundred files under thread-
named paths (`threads-sendsync/` and others; 112 paths match "thread", to count among the programs at RE1). Options:
(a) a class `threads` (C's) for programs that spawn, as v1; (b) ci's threads as goroutines over musl's own primitives
(C23-SURVEY §5.1's plan, "threads as goroutines", not built), which C, C++ and Rust would all use; (c) a deterministic
scheduler in ci (one goroutine at a time, switches at atomics and syscalls), which keeps runs reproducible. Recommended
(a) for v1, (c) as a shared C/C++/Rust milestone later: std's `Mutex`/`Condvar` are futex-based on Linux, so the
scheduler needs `futex` and `clone`-like `pthread_create` and nothing more.

## 8. The judge and the corpora

### 8.1 The judge (RES5)

rustc 1.99.1 at `-C opt-level=0 -C debuginfo=0`, against the pinned std's sysroot (§7.1), the program built from its
original text as RR5's `run-program` builds it (edition, compiletest's flags, `--remap-path-prefix`), run with RR5's
environment (60 s, empty stdin, `PATH`, `HOME`, `exec-env`), compared on status, stdout and stderr after RR5's
normalizations (thread ids, harness timings) and its rule for nondeterminism (two more baseline runs). Recorded as data,
`test/rust/ref-rustc-1.99.1.edn` (C's `ref-gcc15`, Java's `ref-jdk26` format), so the check needs no rustc for the
reference, only for the IR (or the IR recorded: RES12). A second judge before ci exists, as CX6-CX7 did: the lowered C
compiled by gcc `-O0` and run (P4's method), which separates the lowering's bugs from ci's.

run-fail tests (173 revisions in the corpus file's directives) are judged on status 101 and the panic message (stderr);
run-crash (115) on the signal or abort status (`llvm.trap` lowered to `__builtin_trap`: ci's trap status against the
judge's SIGILL, a mapping to fix at RE5).

### 8.2 The corpora

| corpus | where | size | use | license |
|---|---|---:|---|---|
| **the UI suite's run programs** | `rustc-1.99.1-src.tar.xz`'s `tests/ui`, listed by `test/rust/corpus-rustc-1.99.1-ui.edn`'s `:programs` (RR5) | 3,718 revisions (run-pass most; run-fail, run-crash); 574 left out by RR5 with reasons (411 need auxiliary crates, §9) | eval, AOT | MIT or Apache-2.0 |
| **a synthetic corpus** (GT8.6, GTS207 (a)) | generated by construct of the Reference (expressions, patterns, traits and dyn, closures, iterators, panics and drops, integer overflow at every width, i128, floats' formatting, slices and strings, HashMap/BTreeMap, `fmt`) | a few hundred small programs | eval, AOT, the essential subset | Arbace's |
| std's own tests | `rust-src`'s `library/coretests`, `alloctests` (in the library corpus, RR4: 144 and 67 file mentions), std's `#[test]`s | to count | eval of the `--test` harness (one thread), later (RE8) | MIT or Apache-2.0 |
| the crate set's binaries and tests | `crates-2026-10-18` (402 crates, RR4) | to count | eval of real programs (Cargo builds, build scripts, proc-macros: §9), later | each crate's own (mostly MIT or Apache-2.0; listed per crate, to verify) |
| the benchmarks game's Rust programs | not on the host | about 10 programs (to verify) | speed, as C's benchmarksgame | revised BSD (to verify) |

Judging per class (the crops' way): every non-pass in a recorded class of `test/rust/divergences{,-aot}.edn` (goi's
format): `threads`, `process`, `net`, `inline-asm`, `naked`, `simd` (vector shapes the lowering refuses), `long-double`
(none expected: Rust has no `f80`; `f128` and `f16` are nightly types in some UI tests: `f128` as C23's `_Float128`
(mac/155's branch) or a class), `speed`, `nondeterministic`, `stack-overflow` (the guard page and SIGSEGV handler).

## 9. Proc-macros, build scripts, multi-crate programs

- **Proc-macros run in the oracle**, at compile time, as Rust code compiled to a host dylib and loaded by rustc. They are
  part of "rustc compiled this" (§1), exactly as C++'s templates are clang's: their output is in the IR. The read level
  already records the expanded code's effects only through rustc (RR6's HIR level). rsi never runs a proc-macro;
  running one on Arbace would need rustc's `proc_macro` bridge served by Arbace, i.e. route (c). Recommended (RES11 (a)).
- **Build scripts** (`build.rs`) are programs Cargo runs before compiling a crate; their outputs (`cargo:rustc-cfg`,
  generated files) change the crate's flags and sources. In v1 they run on the host under the oracle's Cargo build (as
  RR4's interface level runs Cargo through a wrapper) and their outputs are recorded with the crate's IR; a later level
  could run a build script on rsi itself (it is an ordinary Rust program) and compare its stdout with the host's, a cheap
  extra check (RES11 (b), later).
- **Multi-crate programs** (`aux-build`, `aux-crate`: 411 UI revisions RR5 leaves out; every crate set binary): each crate
  compiled by the oracle to IR and an rlib against the same sysroot, lowered, linked by symbol. The lowering already links
  libstdc++'s 198 units this way (CX7). RE8 adds compiletest's auxiliaries to the program list.

## 10. The row and GT8's budget

### 10.1 The row

`test/reap/cells.edn` gains `:rust` with four cells: read and print from repr/rust's existing records (the tree, text and
behaviour levels put in the reap format, `test/rust/divergences-{read,print}.edn` from the known files RR1-RR6 keep:
`roundtrip-*-known.edn`), eval and AOT from rsi's records (`test/rust/check-linux-amd64.edn`,
`check-aot-linux-amd64.edn`). Makefile: `rsi-*` targets beside `repr-rust-*` (`make check-makefile`). As every crop since
OC120 (b), its steps enter the gate only when its row fits GT8's budget; until then it is out of `gate_key` and the
gate, evaluated from the checkout (CS250's line: embedding at a later re-seed, batched).

### 10.2 The budget (GTS201 (a): about 35 minutes nightly, about 2.4 hours clean, a share of the essential 15 minutes)

What the levels cost today and would cost (rates: rustc's IR 0.1 to 0.2 s a program (P1); repr/llvm's read and the lowering
evaluated on stage 0 about 1 s per 100 KB of IR (CX7.2; P2-P3: 676 KB read in about 2 s, lowered in 3 s up to the refusal),
so 3 to 10 s a UI program; ci about 60 times gcc `-O0` (C5.4); std's lowering once per key, minutes):

| tier | Rust's content (proposed) | estimate (to measure at RE5, RE6) |
|---|---|---|
| essential (about 1 min) | `bin/repr-rust test` (58 s today: already the share); three lowered programs from recorded IR on ci (hello, a panic caught, a HashMap), one AOT | about 1 to 1.5 min: `repr-rust test` must shrink to its essential cases (the samples) |
| nightly (35 min) | read and print: the deduplicated subset of the corpora (GT8.2, GT8.3: today's whole text levels are 40 to 60 min alone, journal 2026-11-03) about 10 min; eval: the coverage subset of the UI programs (GTS205 (a): every block of the lowering, of ci and of cf2g reached, every class, the controls, every deferred program; C's rule gave 229 of 3,582 programs) plus a seventh of the rest, about 12 min at 4 jobs; AOT: the same subset in shards, about 10 min | at the share if the subset is under about 400 programs; std's lowered IR cached by key, never rebuilt nightly unless its key moved |
| clean (2.4 h) | every level over the compressed corpora, audited; the rotation's record complete | whole UI eval: about 3,718 × (3 to 10 s) / 4 jobs = 0.8 to 2.6 h; so the clean run uses the subsets and the rotation's record (GTS204 (a)), never the whole suite |

Caches that make it fit (RES12): the IR keyed by (source digest, flags, rustc, sysroot key); the lowered C forms by (IR
digest, the lowering's sources); the judge's runs recorded once per (source, sysroot key). Embedding repr/llvm and the
lowering at a re-seed (CP87 (a)'s plan for C++) makes the read and lowering several times faster than evaluated (to
measure; stage 0's compiled namespaces against evaluated ones: C's ci went from evaluated to compiled at C9).

## 11. Plan (RE1-RE9)

Estimates in working days, as the other surveys give them; each milestone ends with its check green and its records. C++'s
CX6-CX7 (built) are the base; C's C5-C6 (built) the back end. RE4 overlaps C++'s ci leg (CX7.4): counted here in full,
shared if C++ does it first.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **RE1** the spec and pins | `doc/rust/EVAL.md` (rsi's pipeline, the std build, the panic layer, the classes); pins: the std build's flags and the IR's digests (`test/rust/runtime/ir.sha256`, CX7's way), rustc's and clang's versions; the program list from RR5's `:programs` with thread/process/net counted; the synthetic corpus's generator's plan | the spec accepted; lists counted | 1-2 |
| **RE2** the lowering's Rust delta | the intrinsics of §5.3 in the table; vectors as arrays (`shufflevector`, vector `icmp`, `<N x i1>` bitcasts); i128 breadth; `bin/rsi lower`, `bin/rsi lower-check` (cppi's driver with RR5's `run-program` as the original) over the UI programs **with the host's native std** (P4's method, the second judge gcc) | every UI program lowered and run by gcc as the original, or refused in a class; costs recorded | 4-7 |
| **RE3** std's IR lowered | `bin/rsi runtime-build`: the sysroot from `rust-src` (RR5's build plus IR kept, P5's bitcode method), each crate's IR read (round trip, objects equal), lowered, archived; the allocator shim per program; the Rust pending-flag layer (§5.4, RES4); `std_detect`'s `cpuid` answered; programs linked against lowered std, musl native, gcc the judge | `hello`, `HashMap`, `fmt`, `catch_unwind`, a panic's message and status 101, `process::exit`, `env::args` run as the original; every UI program as RE2 or classed | 6-10 |
| **RE4** the ci leg | the lowered program and std, musl as C forms, on ci (the C forms ci must take, §5.5; shared with C++'s CX7.4) | the RE3 samples and the UI subset run on ci as the judge | 4-8 |
| **RE5** rsi's check | `bin/rsi run|check|divergences|record-ref`; `test/rust/ref-rustc-1.99.1.edn`; classes; controls (GTS81 (a)); the synthetic corpus (GT8.6); the coverage subset (GTS205/206) | the check over the UI programs and the synthetic corpus: none failing, every non-pass in a class; costs recorded against §10.2 | 6-10 |
| **RE6** AOT | the node programs through cf2g, std translated once per key and shared, shards; `bin/rsi check --aot` | as RE5, compiled | 3-6 |
| **RE7** the REPL | Rust forms at the REPL (§5.6): each input a crate, lowered and loaded into the running ci program; the session's state | a REPL session of the samples, recorded | 3-5 |
| **RE8** auxiliaries and the wider corpora | multi-crate programs (UI's 411 auxiliary revisions but proc-macro crates' own runs), std's `coretests`/`alloctests` harness (one thread), a slice of the crate set's binaries with build scripts' outputs recorded; repr/rust's records into the reap format | each in a result or a class | 4-7 |
| **RE9** the row and the milestone | `cells.edn` `:rust`, the classes files, `bin/reap`, essential subsets, steps with `:parts`, Makefile, the nightly within its share (`bin/gate --costs`), the embedding batched with a re-seed if decided (RES14) | `bin/reap`: Rust's four cells checked, none failing; the tag | 2-3 |
| *RE10* (optional, later) MIR | `repr/mir`: stable MIR as forms by a `rustc_public` driver; then an interpreter (route (a)) or a MIR → C forms lowering (a′), judged against rsi's runs | — | 70-120 |

**Total without RE10: about 33 to 58 working days** (RE1 1-2, RE2 4-7, RE3 6-10, RE4 4-8, RE5 6-10, RE6 3-6, RE7 3-5,
RE8 4-7, RE9 2-3); about 29 to 50 if C++'s ci leg is done first. For scale: C++'s CX6-CX8 26 to 44, Python's PY1-PY11 57 to
98 (with read and print, which Rust already has). The surveys have run under their estimates (CX6 and CX7 each in about a
day of agents' work); the least known parts here are std's `sys` layer on ci (RE3-RE4) and the vectors (RE2), to
re-estimate at RE2's end.

**Sonnet and Opus** (the user's split): Sonnet for RE1's lists and spec, RE2's intrinsics table and the driver, RE3's build
script and `cpuid` table, RE5's classing and records, RE6's shards, RE8's lists, RE9. Opus for the vector lowering, the
panic layer's split from C++'s, std's `sys` and TLS on ci, the REPL's session model, and the classes' judgment.

**Re-seed needs.** RE1-RE3 change tools' namespaces (evaluated from the checkout, out of `gate_key`) and scripts.
`arbace.repr.llvm` and the lowering are C++'s and not embedded yet (CP87 (a): embedded at CX10's re-seed): Rust's eval
rides on that re-seed, batched with C++'s, OCaml's and Scheme's (CS250's line).

## 12. Proposals (RES1-RES16)

Each with options, the recommended one first in the text of the recommendation.

- **RES1 What eval interprets.**
  - Options: (a) MIR (stable MIR / `rustc_public`, monomorphized), interpreted by an Arbace interpreter with a Miri-like
    memory model; (a′) MIR lowered to C forms; (b) **rustc's `-O0` LLVM IR through C++'s path** (repr/llvm → the lowering →
    C forms → ci), std compiled to IR and lowered as libstdc++ was; (c) a Rust front end in Arbace.
  - *Recommended: (b).* P2-P4 ran Rust programs through it today with a table's worth of gaps; one back end for C, C++
    and Rust; AOT is cf2g at no extra cost. (a) costs about twice as much (§4.2), shares nothing with the back end and
    needs std rebuilt anyway; it stays the optional RE10 for a Rust without LLVM. (c) is out of reach (§4.3). The
    consequence, as C++'s CP4: **Rust's eval and AOT are the oracle's compilation run on Arbace**; stated so the user
    decides it knowingly.
- **RES2 The optimization level of the IR.**
  - Options: (a) **`-C opt-level=0 -C debuginfo=0`** for programs (overflow checks on, as the judge's `-O0`); (b) `-O1`
    with vectorization off (smaller IR, faster runs, but more intrinsics and less MIR-shaped code); (c) the UI test's own
    `-O` flags when it has them.
  - *Recommended: (a)*, with (c) for the few tests whose behaviour depends on optimization (they are classed, the judge
    built the same way).
- **RES3 std's build.**
  - Options: (a) **one sysroot built from the pinned `rust-src` with its IR kept** (RR5's `-Zbuild-std`, `opt-level=0`,
    debug assertions and overflow checks off, `panic=unwind`), pinned by the IR's digests, programs and the judge
    compiled against it; (b) the host's sysroot for the judge and a source-built one for rsi (two stds: their disambiguators
    differ, share-generics breaks, §7.1); (c) programs with `-Zshare-generics=no` against the host's std and only std's
    non-generic IR built (still a second build).
  - *Recommended: (a)*: one std, one truth; RR5 showed the UI programs behave alike against a source-built std.
- **RES4 The panic layer and the allocator shim.**
  - Options: (a) **`eh.c.ae` split into the unwinder's entry points (shared) and C++'s personality part; Rust's personality
    lowered from std** (§5.4); the allocator shim taken per program from `-C save-temps` (P5) and lowered; (b) a separate
    `arbace/rust/rt/eh.c.ae`; (c) `panic=abort` for every program (no landing pads; `catch_unwind` and run-fail's status
    101 then differ: a large class).
  - *Recommended: (a)*: one layer for both languages; the shim is five functions, keyed by the allocator (System or
    `#[global_allocator]`).
- **RES5 The judge.**
  - Options: (a) **rustc 1.99.1 at `-O0` against the pinned sysroot, the original built as RR5's `run-program` builds it,
    runs recorded as data** (`test/rust/ref-rustc-1.99.1.edn`), gcc `-O0` on the lowered C as the second judge until ci
    runs; (b) the host's sysroot's build (RR5's baseline) as the judge; (c) compiletest's expected outputs (`.run.stdout`
    and `.run.stderr` files) as the judge.
  - *Recommended: (a)*; (c) is a witness for the class `nondeterministic` and for run-fail's messages, not the judge (few UI
    tests have run outputs recorded).
- **RES6 Vectors.**
  - Options: (a) **fixed-width vectors lowered elementwise as arrays** (`shufflevector`, vector `icmp`/`select`/arithmetic,
    `<N x i1>` bitcasts), refusing scalable vectors; (b) hashbrown's and memchr's generic (non-SIMD) paths forced by a
    patched std (a change to the sources, against RES3's "the sources stay the tarball's"); (c) a class `simd` for every
    program whose IR has vectors (it would hold every HashMap user).
  - *Recommended: (a)*: a bounded set of shapes at `-O0`; `core::arch`'s and `portable-simd`'s UI tests (`simd/`) are the
    stress, their unsupported shapes a class.
- **RES7 Threads.**
  - Options: (a) **a class `threads` for programs that spawn in v1** (ci single-threaded, atomics plain, std's TLS as
    `_Thread_local` of one thread); (b) ci's threads as goroutines for C, C++ and Rust together (C23-SURVEY §5.1's plan);
    (c) a deterministic scheduler in ci (one runnable thread, switches at futex and atomics), shared by the three crops.
  - *Recommended: (a)* now, *(c)* as a shared milestone after the row: reproducible runs suit the judge's recorded outputs.
- **RES8 What std asks of the platform.**
  - Options: (a) **a fixed platform for std on ci**: `cpuid` answered as a fixed x86-64-v1 CPU (std_detect's features off:
    the generic paths), `getrandom` from a fixed seed **in both rsi and the judge** (`HashMap`'s order then agrees; the
    judge's runs under an `LD_PRELOAD`-free seed: std reads `getrandom`, so a seccomp-free way is to build the sysroot
    with `std`'s `hashmap_random_keys` constant, a source change) or programs whose output depends on HashMap's order
    classed `nondeterministic`; backtraces off (`RUST_BACKTRACE` unset, the `backtrace` crates never called); the stack
    guard's handler installed if `sigaltstack` works, else skipped; (b) Miri-like shims replacing std's `sys` calls; (c)
    every such program a class.
  - *Recommended: (a)* with HashMap-order programs judged by RR5's nondeterminism rule first (the judge's three runs
    differ already when the order shows), the fixed seed only if the class is large (to count at RE5).
- **RES9 The corpora.**
  - Options: (a) **the UI suite's 3,718 run programs and a synthetic corpus now; std's `coretests`/`alloctests` and the
    crate set's binaries at RE8; the benchmarks game later (a fetch)**; (b) the UI suite alone; (c) the crate set first
    (real programs, but Cargo builds, build scripts and proc-macros from the start).
  - *Recommended: (a)*: all on the host but the benchmarks; the synthetic corpus is GT8.6's rule for a new crop.
- **RES10 The order.**
  - Options: (a) **after the tag `arbace-reaps-4-crops-v1` and GT8; RE1-RE3 (judged by gcc) may start at once, beside
    C++'s work, since they need nothing new from ci; RE4 waits for or does C++'s ci leg; Rust's row after C++'s or with
    it**; (b) Rust's eval before C++'s ci leg, doing the leg itself; (c) after every surveyed crop.
  - *Recommended: (a)*: RE1-RE3 repeat CX6-CX7's proven steps (light tier, no gate); the ci leg is one piece of work
    whoever does it.
- **RES11 Proc-macros and build scripts.**
  - Options: (a) **run by the oracle (rustc and Cargo on the host) in v1, as part of "rustc compiled this"; build scripts'
    outputs recorded with the crate's IR**; (b) (a), and later build scripts also run on rsi with their stdout compared to
    the host's (a cheap extra check); (c) proc-macros on Arbace (needs route (c)).
  - *Recommended: (a)*, (b) at RE8 if cheap.
- **RES12 What is recorded and cached.**
  - Options: (a) **the judge's runs recorded as data; the IR and the lowered C forms cached by key in `.tmp/` (CP100 (c)'s
    shared store when the nightly runs them), the IR committed only for the essential subset (kilobytes, compressed)**;
    (b) every program's IR committed (megabytes per program); (c) nothing cached.
  - *Recommended: (a)*: the essential subset then runs without rustc (the gate's hosts need no Rust toolchain), the rest
    needs the pinned rustc, as repr/rust's levels already do.
- **RES13 The classes' shape.**
  - Options: (a) **goi's format, `test/rust/divergences{,-aot,-read,-print}.edn`, classes of §8.2, every deferred class
    naming its proposal**; (b) one known file per level as RR1-RR6 keep.
  - *Recommended: (a)* for eval and AOT; read and print keep RR's known files and gain the reap-format classes files that
    summarize them (RE8).
- **RES14 Embedding.**
  - Options: (a) **evaluated from the checkout until the batched re-seed that embeds C++'s repr/llvm and lowering (CP87
    (a)); Rust adds only its panic layer and the driver**; (b) embed rsi alone in an earlier re-seed; (c) never embed (tools
    only).
  - *Recommended: (a)*.
- **RES15 Names.**
  - Options: (a) **`rsi` (`bin/rsi`: `lower`, `lower-check`, `runtime-build`, `run`, `check`, `check --aot`, `divergences`,
    `record-ref`), `arbace/rust/` for the Rust-specific runtime pieces, `test/rust/` for the records, milestones RE1-RE9,
    `doc/rust/EVAL.md` the spec**; (b) `rusti` (the name of Rust's own long-gone REPL); (c) fold everything into `bin/cppi`
    with a language flag.
  - *Recommended: (a)*, the crops' pattern (goi, ci, cppi, oi, si, pyi); the lowering stays `arbace.cpp.lower` (renaming it
    `arbace.llvm.lower` once two languages use it is a separate, mechanical choice, RES16).
- **RES16 The lowering's namespace.**
  - Options: (a) **keep `arbace.cpp.lower`, used by both**; (b) rename to `arbace.llvm.lower` (and `cpp/rt/eh` to
    `llvm/rt/eh`) at RE2, with C++'s records unchanged; (c) a Rust copy.
  - *Recommended: (b)* at RE2 (one mechanical commit before Rust's delta lands): the lowering is LLVM's, not C++'s; (c)
    never.

## 13. What to decide first

1. **RES1**: the route, and that Rust's eval is "rustc compiles, Arbace runs" (as C++'s).
2. **RES3**: one source-built std with its IR, the judge against it.
3. **RES10**: the order against the tag, GT8 and C++'s ci leg; RE1-RE3 can start in the light tier.
4. **RES7**: threads a class in v1, a shared scheduler later.
5. **RES16**: the lowering renamed now or not.

## 14. Risks

- **The oracle is the front end** (as for C++): every run needs rustc for the IR, or the IR recorded. The standalone
  direction (CLAUDE.md, "Long term") cannot keep rustc; RE10 (MIR as forms) is the way to Rust semantics without LLVM, a
  native front end is not planned (§4.3). Stated, RES1.
- **std's `sys` layer on ci** (RE3-RE4): TLS (`thread_local!` with destructors registered through
  `__cxa_thread_atexit_impl`), the main thread's stack guard (`pthread_getattr_np`), `sigaltstack`, `getrandom`,
  `std::fs`'s `statx` fallback chain: each a syscall ci must answer or a class; their number is unknown until RE3 runs.
- **Vectors** (RES6): hashbrown makes every `HashMap` program vector-using; the elementwise lowering must be right at
  the first try or a large share of the corpus fails together. `core::arch` and `portable-simd`'s tests stress shapes the
  lowering may refuse (a class `simd`).
- **Speed.** ci is about 60x gcc `-O0` (C5.4); Rust's `-O0` code is heavy (iterator adapters, `fmt` machinery not
  inlined); a UI program that loops a million times may hit the 60 s cap (class `speed`). The whole UI suite does not fit
  a nightly (§10.2); subsets and rotation are mandatory, not optional.
- **The budget is already exceeded by read and print**: 40 to 60 minutes whole against a 35-minute share before eval;
  repr/rust's levels need GT8's dedup and subsets as much as rsi does.
- **Two LLVMs**: rustc's 23.1.3 IR is read and judged by clang 23.1.4 (P2: objects equal); a rustc or clang upgrade on
  Alpine edge (CPP-SURVEY's "pinned to a moving compiler") is a re-record; the pins (`RUSTC_VERSION`, clang's version in
  `test/cpp/host/`) catch it.
- **share-generics and crate hashes** (§7.1): a program compiled against another std than the lowered one links to
  instances that do not exist; the sysroot key must be part of every cache key.
- **HashMap's random order** (RES8): the judge's runs are themselves nondeterministic for programs that print a
  HashMap's iteration; RR5's three-run rule catches most, a fixed seed is a source change to std.
- **Licenses.** Rust's std, `rust-src`, the UI suite: MIT or Apache-2.0 (rust-lang's dual license, in the tarball's
  `LICENSE-MIT`, `LICENSE-APACHE`, `COPYRIGHT`); vendored crates of `library/`'s build each under their own (mostly the same
  dual license; `compiler_builtins` MIT/Apache with LLVM's compiler-rt parts under Apache-2.0 with LLVM Exceptions, to
  verify). Nothing is vendored by this plan (the IR and the lowered std are built into `.tmp/` from the pinned apk);
  LICENSE.md gains an entry if the essential subset's IR is committed (RES12 (a)): it is derived from std.
- **Estimates.** RE3 and RE4 are the least known (as CX6 and CX7 were for C++, which came in far under); re-estimate at
  RE2's end.

## 15. Sources

- The host: `rustc 1.99.1 (58cf8f9c3 2026-10-13) (Alpine Linux Rust 1.99.1-r0)`, `LLVM version: 23.1.3`, host
  `x86_64-alpine-linux-musl`; its sysroot `/usr/lib/rustlib/x86_64-alpine-linux-musl/lib/` (30 crates' rlibs, objects only);
  `cargo 1.99.1`; `clang` 23.1.4 (`/usr/bin/clang`); gcc 15.2.0; musl 1.2.6.
- Arbace: CLAUDE.md; doc/REPR-RUST-SURVEY.md (RS15, RU4, §5.2); doc/rust/SPEC.md and doc/rust/RUST-NOTES.md (RR1-RR6:
  `tools/rsdump`, the corpora, RR5's `run-program` and `--std`, RR6's `--code`); `test/rust/corpus-rustc-1.99.1-ui.edn`
  (`:programs` 3,718, its header's exclusions), `corpus-rustc-1.99.1-library.edn`, `tools/rsdump/apks.sha256`;
  doc/cpp/CPP-SURVEY.md (§5-§6, CP4, CP7, "As built: CX6", "CX7"), doc/cpp/LLVM.md (§7, §8, §10), `arbace/cpp/lower.ae`'s
  intrinsics table, `bin/cppi`, `bin/repr-llvm`; doc/C23-SURVEY.md (C5's ci, C5.4's speed, C6's cf2g, the class `threads`);
  doc/GATE-SURVEY.md ("GT8, the test budget", GTS200-GTS209 decided); doc/python/PYTHON-SURVEY.md and
  doc/ocaml/OCAML-SURVEY.md (the shape); doc/JOURNAL.md (2026-11-03: Haskell and Rust unparked, their checks' times).
- Rust's sources (pinned by repr/rust, not extracted here): `rust-src-1.99.1-r0.apk` (`library/std/src/sys/personality/gcc.rs`,
  `library/panic_unwind/src/gcc.rs`, `library/std/src/rt.rs`, `library/alloc/src/alloc.rs` for the shim's names, hashbrown's
  `raw/sse2.rs`), `rustc-1.99.1-src.tar.xz` (`tests/ui`, `src/tools/compiletest`). Read from memory for this survey, to verify
  at RE1.
- Prior art (not fetched; to verify): Miri `https://github.com/rust-lang/miri` (MIT or Apache-2.0); rustc's
  `compiler/rustc_const_eval/src/interpret`, `compiler/rustc_public` (stable MIR); mrustc `https://github.com/thepowersgang/mrustc`
  (MIT); rustc_codegen_clr `https://github.com/FractalFir/rustc_codegen_clr`; rustc_codegen_cranelift and rustc_codegen_gcc
  (in rust-lang/rust's `compiler/`); llvm-cbe `https://github.com/JuliaHubOSS/llvm-cbe`; evcxr `https://github.com/evcxr/evcxr`;
  the benchmarks game `https://benchmarksgame-team.pages.debian.net/benchmarksgame/`.

## Appendix A: the scratch

The worktree's `.tmp/rust-eval/` (8.7 MB): `a.rs`, `b.rs`, `c.rs` (the probes' programs); `a.ll`, `b.ll`, `c.ll` (P1's IR;
b with `-C panic=abort --crate-type=staticlib`); `a.ae`, `b.ae` (`bin/repr-llvm read`); `rt.list`, `rt/` (P2's round trip:
`rt.tsv`, `objects.tsv`); `a.c`, `b.c`, `a.lc.ae`, `b.lc.ae` and the logs (P3); `c2.ll`, `c3.ll` (c.ll with
`llvm.uadd.sat.i64`, then `llvm.fshl.i64` and `llvm.scmp.i8.i64`, replaced by IR functions), `c.lower.log`, `c2.log`,
`c3.log` (the three refusals); `intr.txt` (c.ll's intrinsics); `rt.c` (`bin/cppi runtime`); `stub.c` (a
`rust_eh_personality` stub for P4's native links); `a.gcc`, `b.gcc`, `b.orig`, `libb.a` (P4); `st/` (P5: `-C save-temps`
output, `shim.ll` from the shim's bitcode by `clang -S -emit-llvm`), `shim.c`.

Rule slips: one probe command redirected a pipe through `/dev/fd/1` (`rustc ... 2>/dev/fd/1 | head -0`, harmless, but
against the conventions); the first two gcc invocations ran with `TMPDIR` unset, so gcc's temporary objects went to
`/tmp` (removed by gcc itself); `TMPDIR` was `.tmp/tmp` from then on. No process was left running.

## Decided (2026-11-03, the user): RES1-RES16

All as recommended. The trial RE1-RE2 starts now, by an agent on the main host, beside GT8 and C++'s CX8 (light work by
day).

## As built: RE1-RE2 (write-ahead, 2026-11-03)

Branch `rust-re1` from main `c545ede1`, by an agent on the main host beside C++'s CX8 (light tier: 4 jobs, `GOMEMLIMIT`
1500 MiB a lowering, no heavy lock; stage 0 `/root/arbace/target/seed/amd64/arbace`, the namespaces evaluated from the
worktree). Spec: doc/rust/EVAL.md. Written ahead of the night's whole run (RE2.5): the counts below are the day's two 10%
slices of the UI programs.

### RE2.1 What was built

| piece | file | what |
|---|---|---|
| the spec | `doc/rust/EVAL.md` | rsi's pipeline, std's build, the native-std leg, the Rust delta, the lists, the judgment and classes, panics from RE3 |
| command | `bin/rsi` (`lower`, `list`, `sets`, `std-build`, `lower-check`); Makefile `rsi-samples`, `rsi-lower-check`, `rsi-lower-check-all`, `rsi-std-build` | RES15 (a) |
| the driver | `arbace/rust/lower.ae` (`arbace.rust.lower`) | read, round trip, the intrinsics a unit declares, the lowering; the native-std leg's substitution (`__rust_try` → `cx_rust_try`) |
| the judge | `arbace/rust/check.ae` (`arbace.rust.check`) | the sets, the plan's filter, `judge` (results, reasons, classes, `--record`) |
| the lowered side | `bin/lib/rsi/wrap`, `bin/lib/rsi/link`; `bin/lib/repr-rust/run-program`'s sides take a WRAP (`NAME:SRC:SYSROOT:NORM:WRAP`, `RUN_PROGRAM_ID`) | rustc's own link with the program's and the shim's objects replaced by gcc's of their lowered IR |
| the layer | `arbace/rust/rt/native.c.ae`, `test/rust/runtime/native/try.rs` | EVAL.md §4 |
| the lowering's Rust delta | `arbace/cpp/lower.ae`, four commits of its own (intrinsics; vectors; bit-packed vectors and the vector intrinsics' order; bitreverse's widths and load.relative.i64) | EVAL.md §5 |
| pins | `test/rust/runtime/std.edn` (rustc's and clang's versions, std's profile, flags, crates), `test/rust/runtime/ir.sha256` (std's 103 units) | RE1 |
| lists | `test/rust/eval-programs.edn` (threads 175 programs, process 63, net 5; 3,506 in none), `test/rust/eval/` (three samples) | RE1 |
| classes, results | `test/rust/divergences-lower.edn` (11 classes), `test/rust/lower-results.edn` (756 programs) | RES13 (a) |

### RE2.2 Results

The IR scan (every one of the 3,718 programs' `-O0` IR with its flags, 31 s at 12 jobs; 3,716 compile): the intrinsics the
table lacked were the survey's §5.3 list and `fpto{s,u}i.sat` (Rust's `as` from floats: 29 and 26 programs), `vector.reduce.*`
(90), `{s,u}cmp` (157), `{u,s}{add,sub}.sat` (91), `fsh{l,r}` (31), `bitreverse`, `bswap.i128`, `powi`, `minimumnum`,
`ptrmask`, `load.relative`; 86 programs use vector instructions (hashbrown's group in most: `load`, `icmp`, `sext`/`trunc`
`<16 x i1>`, `bitcast <16 x i1> to i16`, splat `shufflevector`; `core::simd`'s in `simd/`). The samples: 3 of 3 pass.

`bin/rsi lower-check` over every 10th program (372), then every 10th from the 5th (372), then the tests the fixes touched again
(21 revisions, 12 of them new); recorded in `test/rust/lower-results.edn`:

| run | programs | pass | nondet | in a class | unclassified | wall (4 jobs) |
|---|---:|---:|---:|---:|---:|---:|
| every 10th | 372 | 360 | 4 | 8 | 0 (5 before the fixes, rerun) | 420 s |
| every 10th from the 5th | 372 | 358 | 2 | 12 | 0 (1 before the fixes) | 305 s |
| the record (both and the fixes' revisions) | 756 | 718 | 6 | 32 | 0 | |

The 32 by class: `native-unwind` 10 (drops of unwound lowered frames, catches inlined into lowered code by `-O` builds),
`lto` 6 (thin LTO: 19 units), `c-variadic` 3, `inline-asm` 2 (asm goto's `callbr`; `global_asm!`'s LLVM 23 header
`module asm(target_cpu: ...)`, which repr/llvm does not read), `float16-128` 2 (`half`), `known-bug` 2 (`equal-pointers-unequal`'s
miscompilations at `-O2`, which the lowered build does not reproduce), `wide-int` 2 (i256 of fat LTO's optimized std),
`optimized-ir` 2 (a vector GEP, a static initializer gcc does not fold: fat LTO at `-O2`, `-O3`), `simd` 1
(`llvm.masked.gather`), `alias` 1 (externally implementable items' static alias), `speed` 1 (`mir_heavy_promoted.rs`: a 32 MiB
constant, 33 MB of IR, past the build's 300 s). Every class but `native-unwind`, `known-bug` and `speed` is a refusal by the
lowering (or repr/llvm) with its class, never a wrong run. What the slices found and was fixed (the lowering's fourth and fifth
commits): the vector intrinsics' order (`llvm.ctpop.v4i32` taken by the scalar row: a crash), bit-packed vectors' bitcasts in
constant expressions (`bitcast (<1 x i4> splat (i4 5) to <4 x i1>)`: gcc refused the C), `bitreverse.i3`, `load.relative.i64`;
and three programs built with `-g` whose IR has debug records repr/llvm does not read (the lowered side adds `-C debuginfo=0`).
The round trip through LLVM forms: every unit read is equal as forms and as objects (the units it does not cover are the reader's
refusals, `callbr`, `module asm(target_cpu:`, and the timeout).

Costs: the lowering evaluated on stage 0 takes about 1.6 s of CPU a program (602 s for 371 units; 0.1 to 7.5 s, 2.2 MB of IR
for 53 programs), the whole build-and-run about 1.1 s of wall a program at 4 jobs: the whole suite about 70 minutes at 4 jobs,
35 to 40 at 8. std's build: 38 s of Cargo (8 jobs) and 2 s of `clang -S`, 103 units, 51 MB of IR (CX7's libstdc++: 38 MB).

C++ unchanged: the 16 C++ samples (`test/cpp/samples`, `test/cpp/runtime/samples`) lowered by this branch's lowering and by
main's give the same C byte for byte (the refactor of icmp/fcmp and casts is neutral); `bin/cppi lower-check` was not rerun
(its lowered runtime is keyed by `lower.ae` and would rebuild, minutes: for the merge).

### RE2.3 Against the exit criteria

RE1 ("the spec accepted; lists counted"): the spec is written (EVAL.md), the lists counted (§6 there), std's flags and IR pinned
(at the main checkout's place, RES17), rustc's and clang's versions pinned; the synthetic corpus's generator is planned (EVAL.md
§6), not written. RE2 ("every UI program lowered and run by gcc as the original, or refused in a class"): on the two slices and the fixes'
revisions (756 programs, 20% of the 3,718), 718 run as the original (95.0%; 96.3% of those judged), 6 are nondeterministic, 32 are
in a class, none is outside one; the whole 3,718 is the night's run (RE2.5). Costs recorded (RE2.2).

### RE2.4 Decisions taken in the build, and proposals (RES17-RES22)

- **RES17 Where std is built.** Cargo's `-Zbuild-std` hashes std's packages by their absolute path (measured: two places, two
  sets of disambiguators, whatever the remaps, `__CARGO_DEFAULT_LIB_METADATA`, `-Ztrim-paths` or a copy). Options: (a) build at
  the main checkout's `.tmp/rsi/std` (as built; the pin holds there and nowhere else: a clean run in another clone re-records);
  (b) **at RE3, build the library's own workspace (`library/`, the package `sysroot`) with Cargo directly, as rust's bootstrap
  does**, which hashes its members by their path in the workspace (to verify: the dylib's link and an empty sysroot); (c) pin
  the IR modulo the disambiguators. *Recommended: (b) at RE3, (a) until then.*
- **RES18 The native-std leg's catch.** `__rust_try` called as a native `cx_rust_try` (as built) makes catch_unwind work before
  RE3; drops of unwound lowered frames do not run (class `native-unwind`). Options: (a) **keep it until RE3 and drop it with the
  layer**; (b) no substitution (every caught panic a difference). *Recommended: (a).*
- **RES19 Debug records.** Options: (a) **the lowered side adds `-C debuginfo=0` after a test's `-g` (as built)**; (b) repr/llvm
  reads `#dbg_` records. *Recommended: (a)*: debuginfo changes only backtraces, which RES8 turns off.
- **RES20 The night's whole run and its record.** Options: (a) **`make rsi-lower-check-all` by night at 8 jobs (about 40
  minutes, light: about 1.5 GiB a lowering), recorded into `test/rust/lower-results.edn`**, then the classes completed by day;
  (b) the slices only until RE3. *Recommended: (a).*
- **RES21 A negative control** (GTS81 (a)) for `lower-check`: a sample's lowered run judged against another's original must
  differ. *Recommended: at RE5, with the judge's recorded runs.*
- **RES22 The sets' search.** The thread, process and net sets are a text search (over-approximate). Options: (a) **keep it
  as the list's first cut and judge the classes by the runs on ci (RE4)**; (b) sets by the IR's calls (`pthread_create`,
  `posix_spawn`, `socket` reached from `main`). *Recommended: (a).*
- **RES16's rename** (`arbace.cpp.lower` → `arbace.llvm.lower`, decided (b)) is not done: C++'s CX8 is touching the namespace
  on `cpp-cx8`; the rename is one mechanical commit after CX8 merges, the Rust delta's four commits rebase over either side.

### RE2.5 Night runs

- `make rsi-lower-check-all` (`bin/rsi list -o .tmp/rsi/all.tsv && bin/rsi lower-check .tmp/rsi/all.tsv .tmp/rsi/all --record`;
  `RSI_JOBS=8` by night): the 3,718 programs, recorded into `test/rust/lower-results.edn`; every unclassified program a class
  or a fix the next day.
- `bin/rsi std-build` in the main checkout once merged (the pin's place).

## As built: RE3 (write-ahead, 2026-11-03)

Branch `rust-re3` from `rust-re1` (`3802394e`), by an agent on the main host beside C++'s CX8 and the Rust trial's RE2 (light tier: 4 jobs
for the checks, 8 for std's build and lowering, no heavy lock; stage 0 `/root/arbace/target/seed/amd64/arbace`, the namespaces evaluated
from the worktree). Spec: doc/rust/EVAL.md §3, §8. Written ahead of the night's whole run (RE3.4): the counts below are the day's.

### RE3.1 What was built

| piece | file | what |
|---|---|---|
| std's build (RES17 (b)) | `bin/rsi std-build`, `test/rust/runtime/std.edn`, `ir.sha256` | `library/`'s own workspace built as bootstrap does (package `sysroot`, features `panic-unwind backtrace`, `-Zforce-unstable-if-unmarked`, the manifest's per-package profile overridden by `--config`: compiler_builtins 16 codegen units, the backtrace crates at `-O0`); 110 units, 51 MB, 45 s of Cargo |
| std lowered | `bin/rsi runtime-build` (`make rsi-runtime-build`) | 94 units (compiler_builtins' 16 native), round trip, pre-pass, lowering, gcc, one archive a crate, `rt.o`, a key as cppi's |
| the pre-pass | `arbace/rust/lower.ae` (`runtime-forms`, `lower-list --runtime`) | cpuid and xgetbv answered (RES8 (a)); u128's `carrying_mul_add` (i256) replaced by the layer's; other unlowerable defines (f16) stubbed |
| the layer (RES4) | `arbace/rust/rt/eh.c.ae` | the unwinder's entry points on the pending flag, `cx$catches` for `catch ptr null`, the unwinder's queries as no frame, `cx$uncaught` as std's "failed to initiate panic", C++'s trampolines, `rsi_cpuid`, `rsi_xgetbv`, `rsi_u128_carrying_mul_add`, `rsi_not_lowered` |
| the link | `bin/lib/rsi/link` (`RSI_RUNTIME`, `RSI_SYSROOT`) | each sysroot rlib replaced in place by its crate's lowered archive, no `-lgcc_s`: the program links musl alone |
| the check | `bin/rsi lower-check --leg lowered\|native` (default lowered), `arbace/rust/check.ae` (`--leg`, classes' `:legs`) | both sides against std's build; RE2's leg kept as `--leg native` |
| samples | `test/rust/eval/std.rs`, `unwind.rs` | formatting, collections, strings, `env::args`, `dyn`, i128 and floats; drops while unwinding, nested catches, `resume_unwind`, a panic leaving main (status 101) |
| classes, records | `test/rust/divergences-lower.edn` (`not-lowered`, `backtrace`; `native-unwind` `:legs [:native]`), `test/rust/lower-std-results.edn` (196 programs) | RES13 (a) |

The lowering (`arbace/cpp/lower.ae`) is unchanged by RE3: nothing of C++'s namespace moved, so no commit there to rebase over CX8.

### RE3.2 Results

- **Determinism (RES17 (b))**: std built in three places (two scratch directories of different depth and `.tmp/rsi/std`, in a worktree,
  so neither the main checkout's place) gave the same 110 digests; the pin no longer depends on an absolute path.
- **runtime-build**: 94 of 94 units read, forms equal, objects equal (`clang -c` of the print = of the IR), lowered, compiled by gcc;
  33 functions stubbed (all `core`'s f16); about 2 minutes at 8 jobs (lowering 103 s, gcc 12 s).
- **The exit's programs** (rsi's five samples, gcc `-O0` the judge): `hello`, `HashMap`/`BTreeMap`/`HashSet`, `fmt` (widths, `{:#?}`,
  floats, i128 and u128), `catch_unwind` (payloads, nested, `resume_unwind`), drops while unwinding, a panic's message and status 101,
  `process::exit`, `env::args`: **5 of 5 run as the original**, linked against the lowered std, musl alone native. The first run found u128's
  `Display` reaching the i256 stub (`carrying_mul_add`), replaced by the layer since.
- **The native-unwind class emptied**: RE2's 10 `native-unwind` programs, rerun on the lowered leg: **10 of 10 pass** (drops of unwound
  frames, catches inlined by `-O` builds, `extern "Rust"` unwinding). The sample `unwind.rs` passes on the lowered leg and is
  `native-unwind` on the native leg.
- **A slice**: every 20th UI program from the 7th (186, none in RE2's slices): 179 pass, 3 nondeterministic, 4 in a class (`alias` 1,
  `optimized-ir` 1, `speed` 1, `backtrace` 1: `location-detail-unwrap-multiline.rs` runs with `RUST_BACKTRACE=1` and the original prints
  the native unwinder's backtrace), none outside one; 395 s of wall at 4 jobs (about 2.1 s a program: the lowering's 1.9 s of CPU and a
  link against 13 MB of archives).

### RE3.3 Against the exit criteria

"`hello`, `HashMap`, `fmt`, `catch_unwind`, a panic's message and status 101, `process::exit`, `env::args` run as the original": met by
the samples (5 of 5). "Every UI program as RE2 or classed": on the day's 196 programs (the slice and RE2's native-unwind programs), every
non-pass in a class; the whole 3,718 is the night's run. "The native-unwind class emptied": 10 of 10 pass. The UI programs are judged by
gcc `-O0` until RE4's ci leg, as the brief says.

### RE3.4 Night runs (for tonight)

- `make rsi-lower-check-all` (`RSI_JOBS=8`): the 3,718 programs on the lowered leg, recorded into `test/rust/lower-std-results.edn`;
  at the slice's rate about 2.1 s a program at 4 jobs, so about 70 minutes at 8 (light: about 1.5 GiB a lowering); every unclassified
  program a class or a fix the next day.
- `make rsi-lower-check-native-all` if RE2's whole native record (RE2.5) has not run: it is the same 3,718 on the native leg, about 40
  minutes at 8; with RE3 it only feeds the comparison (RES27).
- Nothing in the main checkout is needed for the pin any more: `bin/rsi std-build` holds wherever it runs.

### RE3.5 Proposals (RES23-RES27)

- **RES23 One layer for C++ and Rust (RES4 (a)'s split).** Options: (a) **split `arbace/cpp/rt/eh.c.ae` into the unwinder's entry points
  and C++'s personality part in the same mechanical commit as RES16's rename (`arbace/llvm/rt/`), after C++'s CX8 merges**, Rust's layer
  then only its own part (`cx$catches` for null, the unwinder's queries, the CPU's answers); (b) keep two layers (about 40 duplicated
  lines: the entry points and the trampolines); (c) split now (a conflict with CX8's changes to `eh.c.ae`). *Recommended: (a).*
- **RES24 std's functions the lowering does not take.** As built: stubs that abort with their name (class `not-lowered`, 33 f16
  functions) and a table of replacements in the layer (u128's `carrying_mul_add`). Options: (a) **as built now; f16 and f128 lowered
  as gcc's `_Float16` and `_Float128` at RE5 with the programs' class `float16-128`, the stubs then gone**; (b) lower them now (C forms'
  types, repr/c's printer and ci's later support); (c) the stubs for good. *Recommended: (a).*
- **RES25 compiler_builtins.** Options: (a) **native until RE4 (as libgcc is for the lowered C++), ci's own builtins there**; (b) lowered
  with its f16/f128 and `global_asm!` units stubbed. *Recommended: (a)*: the lowered C calls libgcc's builtins anyway.
- **RES26 The CPU std sees (RES8 (a)).** As built: an x86-64 baseline (SSE2, vendor `ArbaceRsiCPU`). Options: (a) **as built; a program
  that prints `is_x86_feature_detected!` results differs from the native judge and is classed (`cpu-features`) when the night finds one**;
  (b) the judge's runs under the same answers (an `LD_PRELOAD`-free way does not exist for cpuid: no); (c) answer the host's real cpuid
  (the runs then depend on the host). *Recommended: (a).*
- **RES27 RE2's native leg.** Options: (a) **kept as `--leg native` and `lower-results.edn` until RE4's ci leg, then removed with
  `native.c.ae` and `try.rs`** (RES18 (a) said drop it with the layer; it costs nothing kept and shows the layer's effect, `unwind.rs`);
  (b) removed now. *Recommended: (a).*

### RE3.6 Rule slips, scratch

No slip found: every rustc, clang and gcc ran with `TMPDIR` the worktree's `.tmp/tmp`; no `/dev/null`, no process left running.
`python3` was used for multi-line edits of the sources (no package installed). Scratch: the worktree's `.tmp/re3/` (`a/`,
`elsewhere/` the determinism builds; `low1/`, `low2/` the first lowerings of std; `h/` hello's link line; `w-samples/`,
`w-samples-native/`, `w-nu/`, `w-s20/` the checks' work directories), `.tmp/rsi/std/` and `.tmp/rsi/runtime/` (std's build and its
lowering), `.tmp/repr-rust/` (hard links of `rust-re1`'s fetched apks and UI sources).

## As built: RE4 (write-ahead, 2026-11-03)

Branch `rust-re4` from `rust-re3` (`dfd4bb90`) merged with `ci-prebuilt` (`447b1102`, then its head `544ad8f3`), by an agent on the
main host (light tier: 4 jobs, 3 builder threads; stage 0 `/root/arbace/target/seed/amd64/arbace`, ci judged from the checkout's sources,
`ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1`; the worktree's own `.tmp/ci-musl/forms` (copied from `ci-prebuilt`'s, its key
the same) and `.tmp/ci-runtime/`). Spec: doc/rust/EVAL.md §9. Written ahead of the night's whole run (RE4.5): the counts are the day's.

### RE4.1 What was built

| piece | file | what |
|---|---|---|
| the ci leg's build | `bin/lib/rsi/wrap-ir`, `link-ir` | the plan's rustc command line (one codegen unit, `-C save-temps`), its linker keeping the program's IR (`x.ll`) and the allocator shim's (`shim.ll`), linking nothing |
| the program on ci | `arbace/rust/run.ae` (`arbace.rust.run`) | each unit lowered, read by repr/c, checked by ci; linked by archive rule with the libraries (EVAL.md §9: compiletest's helpers, ci's ports, std, musl, compiler_builtins), each ci's prebuilt runtime; the node program for tools/cirun or cf2g (`nodes-list`, `compile-list`, `nodes`). The machinery is a copy of C++'s `arbace.cpp.run` (cpp-cx8, as `ci-prebuilt-cpp` adapted it): CX8 is not merged here (RES28) |
| std for ci | `bin/rsi runtime-ci` | runtime-build's 94 lowered units (their C now kept) and the layer read by repr/c into a library (95 units, 70 MB of forms); resumable (a unit read whole is kept by an interrupted read at the same key); compiler_builtins lowered for ci with the runtime's pre-pass (15 of 16 units; `cgu.09`, `global_asm!`'s probestack, is not read by repr/llvm and left out), linked after musl (RES25 (b) on ci); ci's ports keyed apart |
| ci's ports | `test/rust/runtime/ci/builtins.c` | `__builtin_ldexpl` (fptoi.sat's bounds), `__builtin_powi`/`powif` (compiler_builtins' `__powidf2` algorithm), `__builtin_fmod`/`fmodf`, `__cxa_thread_atexit_impl` (registers nothing: on musl the native weak reference is null and the main thread's TLS destructors never run either); `prog-opts` declares powi for the programs |
| the judge (RES5 (a)) | `bin/rsi record-ref` (`bin/lib/rsi/record-ref`), `test/rust/ref-rustc-1.99.1.edn` | rustc -O0 against std's build, run three times as run-program runs it, normalized; status, the outputs' SHA-256, `:det` |
| the check | `bin/rsi check [--aot]` (`bin/lib/rsi/check`), `bin/rsi run`, `bin/rsi divergences [--aot]`; `arbace.rust.check` `run-judge`, `write-ref`, `divergences` | the corpora samples and ui; `--only`, `--sample`/`--offset`, `--set`; per-program times; the record `test/rust/check{,-aot}-linux-amd64.edn` and times; `test/rust/check-known.edn`; a failing program's exec-env noted |
| classes, control | `test/rust/divergences.edn` (process, threads, speed, not-lowered, env, ci-param-width, c-variadic, lto), `divergences-aot.edn` (none yet), `controls.edn`, `check-budget.edn` | RES13 (a), GTS81 (a) |
| the lowering (review 190) | `arbace/cpp/lower.ae`, two commits; `bin/rsi lower-unit` (each `test/rust/lower/*.ll` run by lli 23.1.4 and lowered, gcc -O0, the outputs equal) | a bit-packed vector (`<N x i1>`) in memory as LLVM lays it out (its N·k bits' bytes; loads and stores through the bits; inside an aggregate refused; L4's float bitcast refused, not thrown); a bitcast from a byte-element vector through a temporary (gcc refused the compound literal) |
| Makefile | `rsi-runtime-ci`, `rsi-record-ref`, `rsi-check`, `rsi-check-aot`, `rsi-check-all`, `rsi-divergences` | |

### RE4.2 Results

- **The lowering's fixes** (review 190): `packed-vector-memory.ll` as lli (`a5`, `24`, `2`, `15`, `101`; the base lowering gave
  `100010000010001`, `10`, `4`), `bitcast-byte-vector.ll` as lli (the base's C: 4 gcc errors). C++'s 16 samples (`test/cpp/samples`,
  `test/cpp/runtime/samples`, `clang++ -std=c++23 -O0` on this host) lower to the same C byte for byte with `rust-re3`'s lowering and this
  branch's (941 B to 1.69 MB).
- **hello on ci**: `hello, 42`, 36 s end to end warm (a process's start on the evaluated sources included; ci's build 16.6 s), 550 s cold
  (the first link makes the fragments of the 367 units it pulls: std's, core's, musl's).
- **The samples** (rsi's five, `test/rust/eval/`): **5 of 5 pass on ci** (`intrinsics`, `panics`, `std`, `unwind`, `vectors`: i128,
  saturating and funnel ops, HashMap's SSE2 group, `core::simd`, catch_unwind, drops while unwinding, status 101), recorded in
  `test/rust/check-linux-amd64.edn`; the control differs as it must. Builds 36 to 290 s a program (std.rs's monomorphized collections:
  33 s lowering, 290 s ci's build).
- **A UI sample** (every 40th of the 3,718, 93 programs, the judge's runs recorded): first run 85 pass, 1 threads, 7 unclassified; after
  the ports and classes those 7 rerun (`--only`): 2 pass, and **the 93: 87 pass, 6 in a class, none outside one** (process 1:
  `posix_spawn`; threads 1: `stringify.rs`'s harness thread; env 1: ci's empty environment; ci-param-width 1: `rust_int8_to_int32(-1)`
  gives 255; c-variadic 1; lto 1: thin LTO's 64 units). Not recorded (a subset, C's rule). Times: ci's build about 12.7 s a program warm
  (87 of 93 under 100 s; three at 943 s were the cold fragments after merging ci-prebuilt's new key), lowering 1.4 s, IR 0.25 s, run 1.2 s.
- **AOT**: `panics.rs` compiled by cf2g, built by TamaGo's go and run: **passes, as interpreted** (ci's build and cf2g's translation
  1,088 s on the evaluated host, go build 116 s, run 0.1 s against ci's 1.1 s). The five samples in one process (shards of 4) were stopped
  after an hour without a program translated (a 7 GiB process, std.rs's node program the largest): cf2g's translation of a node program
  that holds the linked std is the AOT level's cost, as C++'s CX8 found (CS273's next step: a unit's functions translated once). Not
  recorded (one program).
- **The judge's runs**: 98 programs (the samples and the 93) in 18 s at 4 jobs; no nondeterministic and no unbuilt program among them.

### RE4.3 Costs and what they mean for GT8

- std's read for ci is the long pole: about 100 minutes of wall (340 min CPU) on stage 0 evaluated, a 7 GiB process at 4 threads; it is
  keyed and resumable, so it reruns only when the lowering, repr/c, the host or run.ae's read options move.
- The first link after a key change makes std's fragments (about 15 minutes); warm, a UI program's ci build is about 13 s.
- Whole UI suite on ci: about 3,718 × 16 s ≈ 16.5 h of thread time, about 5.5 h at 3 builder threads: a night at most with more
  threads, far over GT8's share; the nightly needs the coverage subset (GTS205), as C++'s does.

### RE4.4 Against the exit criteria

"The samples and a sample of the UI programs run on ci as the originals, every non-pass classed": the samples 5 of 5; the UI sample 87 of
93, the 6 others classed. compiler_builtins: lowered for ci (15 of 16 units; RES25's native leg kept for gcc's). "bin/rsi check and check
--aot with classes, controls, per-program times and --only lists": built; AOT run on one sample (passes), its record for the night (RE4.5).

### RE4.5 Night runs (for tonight; through `/root/arbace/.tmp/heavy`, at most 4 jobs)

- `/root/arbace/.tmp/heavy make rsi-check-all` (`bin/rsi record-ref && bin/rsi check --record`): the judge's runs of all 3,718 (minutes),
  then every UI program on ci, recorded into `test/rust/check-linux-amd64.edn` and its times; about 5.5 h at 3 threads (one ci process,
  about 7 GiB); every unclassified program a class or a fix the next day.
- `/root/arbace/.tmp/heavy env RSI_THREADS=1 RSI_SHARD=1 bin/rsi check --aot --corpus samples --record`: the AOT level's record of the five
  samples (about 20 minutes to 1.5 hours a program on the evaluated host). The UI programs' AOT waits for cf2g's per-unit translation (RES34):
  at about 18 minutes a program, every 40th alone is a night.

### RE4.6 Proposals (RES28-RES34)

- **RES28 One ci leg for C++ and Rust.** `arbace.rust.run` copies `arbace.cpp.run`'s machinery (the options, read-lib, the closure, the
  constructors, the prebuilt link). Options: (a) **fold both into one namespace of the LLVM path (`arbace.llvm.run`) in RES16's rename
  commit after CX8 merges**, each language its libraries and units; (b) keep two copies. *Recommended: (a).*
- **RES29 ci's environment.** ci's `RunMain` gives an empty envp by design; compiletest's `exec-env` programs differ (class `env`).
  Options: (a) **the runner's environment as envp (tools/cirun and cf2g's main, behind a flag C's records keep off)**, C's crop to decide;
  (b) the class for good. *Recommended: (a).*
- **RES30 prog-opts.** The powi declarations are programs' options apart from std's read (std's units use none) to avoid a 100-minute
  reread. Options: (a) **fold them into `opts` at the next read that a lowering change forces anyway**; (b) keep them apart.
  *Recommended: (a).*
- **RES31 ci's gaps found (to C's crop).** A callee does not narrow a parameter to its type (`ci-param-width`: bin/ci run of a cast call
  gives 255 where gcc gives -1); a weak undefined function is called, not null (the port of `__cxa_thread_atexit_impl` works around it).
  Options: (a) **report both to C's crop as gaps, the class and the port removed once fixed**; (b) lower Rust's narrow parameters with
  explicit conversions. *Recommended: (a).*
- **RES32 std's ci library's cost.** Options: (a) **share `.tmp/rsi/runtime/ci` and the fragments across worktrees by key (CS272 (b)'s
  shape) and read std on the compiled ci after the re-seed that embeds it**; (b) per checkout, evaluated, as built. *Recommended: (a).*
- **RES34 AOT's translation.** cf2g translates each program's whole node program, the linked std included (about 1,000 s for `panics.rs`).
  Options: (a) **std's fragments translated once per key into a Go package the programs' packages call (CS273's next step, shared with
  C++)**; (b) as built. *Recommended: (a)*, before the UI programs' AOT.
- **RES33 The nightly's Rust eval.** Options: (a) **the samples and a coverage subset (GTS205) of the UI programs, the rest rotated
  (GTS203), the whole only in a clean run**; (b) the whole nightly (about 5.5 h: no). *Recommended: (a).*

### RE4.7 Rule slips, scratch

Two slips: one check ran with `--only <(echo ...)` (a process substitution) and one rustc's stderr went to `/dev/fd/2`, both against the
conventions and harmless; redone with files. The first runtime-ci read (about 97 minutes) was terminated from outside during the host's
memory pressure; read-lib now resumes an interrupted read. The day's AOT run was stopped and restarted under `.tmp/heavy` at the
coordinator's resource note. Scratch in the worktree's `.tmp/`: `rsi/` (std's build, the lowered runtime with `ci/`), `ci-runtime/`,
`ci-musl/`, `rsi-check/`, `rsi-check-f/`, `rsi-aot/`, `rsi-ref/`, `t1/`, `t2/`, `cb/`, `cppsame/` (the C++ comparison), `base-unit/`.

## As built: RE5 (write-ahead, 2026-11-05)

Branch `rust-re5` from `rust-re4` (`5ea3caa2`), by an agent on the main host during the day's keyed `--full` (light tier: no gate, no
build, no heavy lock; stage 0 `/root/arbace/target/seed/amd64/arbace`, checkout sources evaluated with `ARBACE_PATH_FIRST=1
ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1`; processes under 2 GiB). **What the day could not do**: no program ran on ci. The UI suite's source
(`bin/repr-rust corpus fetch rustc-1.99.1-ui`) and `rust-src` are not on the host and the day had no network; std's lowered ci library
(`bin/rsi runtime-ci`: about 100 minutes, a 7 GiB process) is far over the day's limits; and `bin/rsi lower-check` stopped itself at the
light tier's disk rule (the disk at 83%, `RSI_DISK_MAX` 80). So RE5's exit criterion (the check over the UI programs and the synthetic
corpus, none failing, every non-pass in a class) is **not met**; its machinery and the corpus are built, proved without ci, and the
night's commands written (RE5.4).

### RE5.1 What was built

| piece | file | what |
|---|---|---|
| the verbs | `bin/rsi run`, `check`, `divergences`, `record-ref` were RE4's (§RE4.1); RE5 adds `synthetic [generate\|check\|ref]`, `judge-test`, `check --essential`, and the corpus `synthetic` in `list`, `record-ref`, `check`, `lower-check` (programs of the checkout need no UI source tree) | the records' format is RE4's: `test/rust/ref-rustc-1.99.1.edn`, `test/rust/check{,-aot}-linux-amd64.edn`, `check{,-aot}-times-linux-amd64.edn`, classes `divergences{,-aot}.edn`, `check-known.edn`, `check-budget.edn` |
| the synthetic corpus (GT8.6, GTS207 (a)) | `arbace/rust/synthetic.ae`, `test/rust/synthetic.edn` (67 rows), `test/rust/synthetic-templates/` (23 templates), `test/rust/synthetic/` (the generated programs, committed) | EVAL.md §6; every program compiles with rustc 1.99.1 at `-O0` under the plan's flags, runs deterministically (two runs equal but the thread id the judge's normalization removes), and gives the status of its row |
| the judge's runs | `test/rust/ref-rustc-1.99.1.edn` | the 67 and the 5 samples recorded (165 maps with the UI sample's 93); the 5 samples' digests came out as before |
| the controls (GTS81 (a)) | `test/rust/controls.edn` | three more: i32 against i64's run, two panics' stderr, the trap's status against the abort's |
| the judge's test | `bin/rsi judge-test`, `arbace.rust.check/make-fake` | without ci: the samples and the synthetic corpus as built and run with the judge's own results (72 of 72 pass, the 4 controls differ), then `fmt.rs`'s output changed (it fails, `differ stdout`); 34 s on stage 0 |
| the essential list | `test/rust/essential.txt`, `check --essential` | 13 programs, one per class of what the lowering and ci do; the coverage subset proper (GTS205) is chosen from the first whole run's records (RES38) |
| Makefile | `rsi-synthetic`, `rsi-synthetic-check`, `rsi-judge-test`, `rsi-check-essential` | |
| doc | EVAL.md §2 (verbs), §6 (the corpus), §9 (the signals) | |

### RE5.2 Results

- **SIGILL (RE4's open mapping)**: none needed. rustc's trap is a `ud2` and status 132 (`128 + signal`); the lowering's `__builtin_trap`
  is 132 on gcc and on ci (`Fault{SIGILL}` reported as 128 + 4); `abort` is 134 on both. Checked: `bin/ci run` of a C program: 132 for
  the trap, 134 for `abort()`; rustc: `std::process::abort` 134, `core::intrinsics::abort` 132. The Rust cases on ci (`trap.rs`, `abort.rs`)
  are in the essential list.
- **The corpus against rustc**: `bin/rsi synthetic ref`: 67 programs, 0 against the judge, 0 nondeterministic (statuses: 0 in 29, 101 in
  34, 1, 37, 132, 134). `bin/rsi synthetic check`: 67 programs, 0 stale.
- **Judge**: `bin/rsi judge-test` passes (above).
- The synthetic programs were first drafted with 7 rustc errors (macro syntax, borrows in one `println!`), fixed in the templates; one
  drafted program (a panic in a drop while unwinding) was left out: std prints a backtrace with addresses on that abort, which no
  normalization of run-program's covers (RES39).
- The judge's runs of the day were recorded against the older std build the host had (`/root/arbace/.tmp/rsi/std`, 103 units, from the
  RE3 day's first profile), not the pin's 110 units (`test/rust/runtime/ir.sha256`; `rust-src` was not at hand to build it): the 5
  samples' digests are the same as recorded at RE4 against the pin's, and the night's `record-ref` records everything again.

### RE5.3 What is left of RE5 and after

- RE5: the whole check on ci (RE5.4): the UI programs and the synthetic corpus, classes and fixes for what comes out; the costs against
  §10.2 (RE4.3 measured the UI sample: ci's build about 13 s a program warm); the coverage subset chosen from the records (RES38).
- RE6 (AOT): not started. RE4 measured cf2g's translation of a node program with the linked std at about 1,000 s (`panics.rs`); RES34 (a),
  std's fragments translated once per key into a Go package the programs call, precedes any UI program's AOT, and the synthetic
  corpus's AOT with it (the essential list is the first AOT subset). RE7 (the REPL) and RE8 (auxiliaries) wait on RE5-RE6.

### RE5.4 Night commands (through `/root/arbace/.tmp/heavy`, at most 4 jobs; the checkout's `.tmp/` with the UI source and std)

1. `bin/repr-rust corpus fetch rustc-1.99.1-ui` (network), `bin/rsi std-build` (the pin's std), `bin/rsi runtime-build`, then
   `/root/arbace/.tmp/heavy bin/rsi runtime-ci` (about 100 minutes; keyed, resumable).
2. `bin/rsi synthetic check && bin/rsi judge-test` (seconds): the corpus and the judge.
3. `/root/arbace/.tmp/heavy bin/rsi record-ref` (every UI program, the samples and the synthetic corpus, three runs each: minutes), then
   `bin/rsi synthetic ref`.
4. `/root/arbace/.tmp/heavy bin/rsi check --essential` first (13 programs, about 4 to 10 minutes warm): any non-pass is a class or a
   fix before the rest; then `/root/arbace/.tmp/heavy bin/rsi check --corpus samples,synthetic --record` (72 programs, about 20
   minutes warm), then `/root/arbace/.tmp/heavy bin/rsi check --corpus ui --record` (about 5.5 h at 3 threads: RE4.5's), then
   `bin/rsi divergences`.
5. AOT, after RES34: `bin/rsi check --aot --essential`, then the samples and the synthetic corpus in shards.

### RE5.5 Proposals (RES35-RES39)

- **RES35 The synthetic corpus's shape.** Options: (a) **as built: a table and templates, 67 programs, committed generated files (the
  files read as the programs; `check` keeps them honest)**; (b) the generator writes files at each run, nothing committed (the corpus
  is then no program of the record, the ids move); (c) grow it to the few hundred of EVAL.md §6 by more tables (floats at more values,
  `fmt` specs, integer ops by pair) once the 67 run on ci. *Recommended: (a), then (c) in the order the classes show where ci or the
  lowering fail.*
- **RES36 The Rust crop's namespaces and the gate's key.** `arbace/rust/*.ae` (the lowering driver, run.ae, check.ae, synthetic.ae)
  are not in `arbace/j2g/embed.ae`'s exclusions or `bin/lib/version.bash`'s, unlike `arbace/cpp/` and `arbace/repr/{rust,cpp,llvm}`: at a
  merge they would be embedded and compiled by c2j into every executable (and a change to them moves `gate_key`). Options: (a)
  **exclude `arbace/rust/` beside `arbace/cpp/` in both until a re-seed embeds the crop (CX10's plan)**; (b) embed now. *Recommended: (a);
  not done here (embed.ae and version.bash are the gate's, and main's keyed run is in flight).*
- **RES37 The judge's std.** The reference runs are recorded against std's build at the pin; a host without the pin's build (the day's)
  records against any build and says so. Options: (a) **as built: the night's `record-ref` records against the pin and rewrites every
  map**; (b) `record-ref` refuses a std not at the pin (as `std_current` does when it may build one) with a flag to override, the record
  marking the maps. *Recommended: (a); the digests agree today.*
- **RES38 The coverage subset.** GTS205's rule (every block of the lowering, of ci and of cf2g reached, every class, the controls, every
  deferred program) needs per-program coverage data this check does not yet write. Options: (a) **the first whole run writes each
  program's features (the intrinsics of `x/intrinsics.txt`, the lowering's constructs, the classes) beside its times, and a greedy
  cover over them makes `test/rust/subset.txt`, as C's rule did (229 of 3,582)**; (b) `essential.txt` plus every 40th UI program, rotated
  (GTS203). *Recommended: (a) after the first run; (b) meanwhile.*
- **RES39 Output with addresses.** A run that prints a backtrace (std's abort after a double panic) has addresses neither side can
  match. Options: (a) **leave such programs out of the corpus (as built) and class a UI program that prints one**; (b) normalize
  `0x[0-9a-f]+` frames in `plan_norm` for both sides (run-program's too: RR5's). *Recommended: (a).*
