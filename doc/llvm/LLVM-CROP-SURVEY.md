# LLVM IR as an intermediate crop: survey and plan

Status: survey and plan, 2026-11-03, at the user's request (LLVM IR its own row of the REAP matrix: read, eval, AOT,
print), after this assessment: LLVM IR is a hub (C++ goes through it already, Rust's eval will, RUST-EVAL-SURVEY's RES1
(b), decided; Swift, Zig, flang and Julia would), so a first-class row checks the shared lowering once, on its own corpus;
it is counted as an **intermediate crop** beside the ten languages, not one of them; its work starts **after C++'s CX8
lands**, because the lowering is shared and being changed now (CX8 on `cpp-cx8`, Rust's RE2 on `rust-re1`). Nothing here is
adopted: no code under `arbace/`, `go/`, `bin/`, `tools/` or `test/` changed, and only this file was written. The decisions
are the user's, as proposals LLS1-LLS19 (§14); the milestones are LL1-LL9 (§13), with two optional ones.

Base: branch `llvm-survey` from main `c545ede1`. The work is reading (doc/llvm/SPEC.md and LOWER.md, CPP-SURVEY's "As built: CX6",
"CX7", CX8's commit `4d6c0b37` on `cpp-cx8`, RUST-EVAL-SURVEY, GATE-SURVEY's "GT8, the test budget",
PYTHON-SURVEY for the shape) and light probes of seconds each (§3): the host's clang 23.1.4 as assembler, verifier and
printer on tiny files; LLVM 15's `llvm-as`, `opt` and `lli` (the only LLVM tools on the host) on clang 23's IR; the IR
clang emits for gcc's `gcc.c-torture/execute/2*.c` (475 files, extracted from the pinned gcc 15.2.0 tarball into the
scratch) at `-O0` and `-O2`; `bin/repr-llvm roundtrip` and `bin/cppi lower` on the main checkout's stage 0
(`ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`, evaluated from the worktree). Scratch in the worktree's
`.tmp/llvm-crop/` (Appendix A). No Arbace executable was built, no lock taken, nothing installed, nothing downloaded. What
the host does not hold (llvm-project's `llvm/test`, llvm-test-suite: pinned in `test/cpp/corpora.edn`, the first fetched at
CX1 and since removed with its worktree, the second never fetched) is named from memory of the projects, every such size
marked **to verify**.

Words used throughout:

- **LLVM forms**: LLVM IR text as Arbace forms (`arbace.repr.llvm`, doc/llvm/SPEC.md and LOWER.md §1-§6), as C++'s CX6 built them.
- **repr/llvm**: the bridge between IR text and LLVM forms (`bin/repr-llvm read|print|roundtrip`), as repr/c and repr/go are.
- **The lowering**: LLVM forms → C forms, `arbace.cpp.lower` today, `arbace.llvm.lower` once RES16 (b) renames it at RE2.
- **llvmi**: the evaluator of LLVM IR at the REPL and for programs (the name is LLS14's): the lowering, then C23's ci; as
  goi is Go's, ci C's, cppi C++'s, rsi Rust's.
- **The pin**: clang 23.1.4, Alpine's `clang23-23.1.4-r0` (`test/cpp/corpora.edn`'s `:alpine-clang23`), with the
  llvm-project tarball of the same release (`:llvmorg-23.1.4`), both C++'s pins, shared.
- **The IR's own build**: the IR compiled to a program by the pin (`clang -O0 x.ll -o x`), run; the reference of eval
  (LLS8), CP83 (a)'s "original" for C++, here the crop's judge.
- **The canonical text**: the IR as the pin's LLParser reads it and its printer writes it back (`clang -S -emit-llvm
  -Xclang -disable-llvm-passes x.ll`), the host's equivalent of `llvm-as | llvm-dis` (P1).
- **Client crops**: the crops whose eval goes through LLVM IR: C++ (CP4 (a)), Rust (RES1 (b)), and later any hub language.

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 What the probes showed · 4 Read · 5 eval: llvmi ·
6 Undefined behaviour, poison and target intrinsics · 7 AOT · 8 Print · 9 The corpora · 10 The module layout and the client
crops · 11 The row and GT8's budget · 12 The pin and version churn · 13 Plan (LL1-LL9) · 14 Proposals (LLS1-LLS19) ·
15 What to decide first · 16 Risks · 17 Sources · Appendix A: the scratch.

## Summary

- **Most of the row exists, built for C++ and judged only through C++'s and Rust's programs.** repr/llvm reads every
  instruction clang 23 and rustc 1.99.1 emit at `-O0`; the lowering runs C++'s runtime (198 units) and 234 of 234
  libstdc++ tests (CX7, gcc the judge), and since CX8 (on its branch) on ci and compiled. What the row adds is **IR as its
  own subject**: the textual format beyond `-O0` C++, optimized IR, IR written by hand (LLVM's own tests), the verifier's
  rejections, and the lowering judged against the IR's own build rather than against a language's compiler.
- **The probes say the base is sound and the gaps are specific (§3).** repr/llvm round-tripped **449 of 450** of clang's
  `-O2` IR of gcc's torture programs (forms equal, objects equal, and **the canonical text equal on 449 of 449**), the one
  refusal the new `ptrtoaddr` instruction; 10 of 10 random modules from `llvm-stress`. The lowering, with gcc as the
  judge, ran **74 of 76** sampled torture programs (38 at `-O0`, 38 at `-O2`) as the IR's own build, the two refusals
  `blockaddress` (computed goto). Gaps by construct: `callbr`, debug records (`#dbg_value`, any `-g` IR), module summaries
  (`^0 = ...`), Windows EH pads (`catchswitch`...), `ptrtoaddr`; in the lowering: vector arithmetic and `shufflevector`
  (4 of 5 `-O2` probes that had them refused), `blockaddress`/`indirectbr`, and **a C++ runtime it drags into C's IR** (a
  C program's lowered code references `__cxa_begin_catch` and `std::terminate` through the pending-flag layer, P8).
- **Recommendation (LLS7, LLS8):** llvmi's eval is the existing lowering to C forms run on ci, AOT the same C forms
  through cf2g, the reference the IR's own build by the pinned clang, recorded as data; gcc `-O0` on the lowered C stays
  the lowering's second judge. An IR interpreter in Arbace (lli's shape, with poison tracked) is an optional later
  milestone (LL10), a semantics reference, not v1's eval.
- **The reference stays the host's clang (LLS3)**: it is LLParser, the verifier, the canonical printer and the native
  builder in one binary, and every LLVM target is built into it. Alpine ships no `llvm-as`, `opt`, `lli` or `FileCheck`
  for LLVM 23 on this host (only LLVM 15's, which accepts **234 of 450** clang 23 `-O0` modules and **79 of 450** at `-O2`:
  the textual format changes every release, §12).
- **Corpora (LLS11):** `llvm/test`'s IR read and printed (every target, FileCheck never run; `not llvm-as` tests as the
  verifier's corpus), the IR the client crops already emit (C's run corpora through clang, C++'s and Rust's programs:
  deduplicated by digest), a synthetic corpus by LangRef construct (GT8.6), llvm-test-suite's `SingleSource` if the user
  fetches it (pinned `:later` since CX1).
- **Plan (§13): LL1-LL9, about 31 to 52 working days**, after CX8 merges; the rename is RE2's (RES16 (b)). The client
  crops' checks shrink to their language-specific parts (LLS15), and the row's GT8 share is half a crop's, moved from
  theirs (LLS17).
- **To decide first (§15):** LLS1 (the row as an intermediate crop), LLS2 (timing), LLS3 (the reference), LLS7/LLS8 (eval's
  route and reference), LLS11 (the corpora and the fetches), LLS17 (the budget).

## 1. What parity means

The crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions"), for an intermediate language: the subject is the IR
text itself, any producer's; the reference is LLVM's own reading and execution of it at the pin.

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **LLVM read** | repr/llvm `read` (exists, CX6), widened to LLParser 23's whole grammar (LL3); `arbace.llvm.verify` (LL4) | `llvm/test`'s IR (inputs of RUN lines), the client crops' IR, synthetic and random IR | the pin's LLParser and verifier: acceptance, the canonical text, the objects; for rejected files the first line of the error | the forms read back equal; the canonical text of the print equal to the original's; objects equal where the target builds; rejections with LLParser's or the verifier's first line |
| **LLVM eval** | llvmi: the lowering, ci (C5) | IR with a `main`: the client crops' run programs' IR, C's run corpora through clang at `-O0` and `-O2`, synthetic programs, llvm-test-suite's `SingleSource` | the IR's own build by the pin, run (status, stdout, stderr), recorded | byte for byte; every non-pass in a recorded class |
| **LLVM AOT** | the same C forms through cf2g (C6) and Go's compiler | as eval | as eval | as eval |
| **LLVM print** | repr/llvm `print` (exists: one canonical spelling), `:full` (LL8: comments and layout kept) | every read corpus | the original text; LLParser's view of the print | `:full` byte for byte; the canonical text and the objects equal |

Same as the crops: linux/amd64 alone for eval and AOT (the lowering takes x86-64 SysV's data layout, LLVM.md §7); read and
print take every target's IR, since the pin's clang has all targets (P1). Results recorded as data, `bin/reap` judging
from the records.

**What differs from a language crop.** There is no source language and no front end to stand in for: the oracle problem
of C++ and Rust (CPP-SURVEY §1, RUST-EVAL-SURVEY §1) does not arise. **The IR's meaning is Arbace's own** (the lowering
is Arbace's semantics of LLVM IR), and the row is where that semantics is checked against LLVM's on its own terms. That is
the hub argument: a lowering bug found through C++ costs a libstdc++ test to reduce; here it is a 20-line module.

## 2. The host and prior art

### 2.1 The host

- `clang` 23.1.4 (Alpine `clang23-23.1.4-r0`, `/usr/bin/clang`), target `x86_64-alpine-linux-musl`, **every LLVM target
  registered** (`clang -print-targets`: x86, AArch64, ARM, RISC-V, AMDGPU, NVPTX, PowerPC, SystemZ, WebAssembly, LoongArch,
  Mips, Hexagon, SPIR-V, BPF, Sparc, VE, XCore, AVR, Lanai, MSP430). `llvm23-libs` and `llvm23-linker-tools` only: **no
  `llvm-as`, `llvm-dis`, `opt`, `llc`, `lli`, `llvm-link`, `FileCheck`, `llvm-reduce`, `llvm-stress` for LLVM 23**.
- `/usr/lib/llvm15/bin` (Alpine `llvm15-15.0.7-r20`, a dependency of something older): the whole LLVM 15 tool set, `lli`,
  `opt`, `llvm-as`, `llvm-stress` among them. Useful only as a measure of version churn (P3) and for `llvm-stress` (P6).
- `rustc` 1.99.1 with LLVM 23.1.3 (RUST-EVAL-SURVEY §2.1); gcc 15.2.0 (the lowering's second judge); musl 1.2.6.
- Not present: swiftc, zig, flang, julia, ldc2, tinygo (the hub's other producers); GHC 9.14.1 is here but its LLVM back
  end (`-fllvm`) needs `opt` and `llc`.

### 2.2 Prior art (from memory, to verify)

| project | what | relevance | license |
|---|---|---|---|
| **LLParser and the Verifier** (`llvm/lib/AsmParser/LLParser.cpp`, `llvm/lib/IR/Verifier.cpp`) | the textual grammar and the IR's well-formedness rules, with their messages | the read level's reference; LL3's and LL4's ports (LLParser on the order of 10,000 lines of C++, the Verifier about 7,000; to verify) | Apache-2.0 WITH LLVM-exception |
| **LangRef** (`llvm/docs/LangRef.rst`) | the IR's specification, including poison, undef and `freeze` | the forms' and the lowering's specification; the synthetic corpus's index | same |
| **lli**'s interpreter (`llvm/lib/ExecutionEngine/Interpreter`) | a direct interpreter of IR (no JIT), known gaps (no `invoke` unwinding, few intrinsics) | LL10's shape and its limits | same |
| **Alive2** (`AliveToolkit/alive2`) | a formal semantics of IR (poison, undef, memory) for translation validation | the reference for poison and undef (§6), not a tool to run here | MIT (to verify) |
| **Vellvm** (`vellvm/vellvm`) | a Coq semantics of LLVM IR with an executable interpreter | a second statement of the semantics | GPL-3.0 (to verify) |
| **llvm-cbe** (`JuliaHubOSS/llvm-cbe`) | an IR → C back end | the lowering's idea, as CX6 built it independently | NCSA (to verify) |
| **llvm-stress**, **llvm-reduce** | random IR; test-case reduction | generated corpus (P6); reducing a lowering failure | Apache-2.0 WITH LLVM-exception |
| **llvm-test-suite** | `SingleSource`, `MultiSource`, `Benchmarks` programs with reference outputs | eval's program corpus | LLVM's for the harness, the programs under their own (to verify; pinned `:later`) |

### 2.3 What Arbace already has

- **repr/llvm** (CX6; LLVM.md §1-§6): `arbace/repr/llvm.ae`, `read.ae` (694 lines), `print.ae` (345): a regex lexer and a
  recursive-descent parser of LangRef's grammar as clang 23 writes it, specialized metadata kept as text (`(!DI "...")`),
  refusals with a class (`:typed-pointer`, `:uselistorder`, `callbr`); `bin/repr-llvm roundtrip` (forms, `.ae` text read back,
  print, read again; `clang -c` objects compared).
- **The lowering** (`arbace/cpp/lower.ae`, 1,080 lines; LLVM.md §7-§10): about 45 intrinsic families in its table, every
  instruction clang emits at `-O0` and the atomics; refusals by class (`:vector` 6 sites, `:intrinsic`, `:blockaddress`,
  `:indirectbr`, `:inline-asm`, `:wide-int`, `:float-format` ...). The pending-flag layer `arbace/cpp/rt/eh.c.ae` (217
  lines). On `rust-re1` (RE2, in progress) 14 more intrinsic families for rustc (`{u,s}{add,sub}.sat`, `fsh{l,r}`,
  `{s,u}cmp`, `bitreverse`, ...).
- **cppi on ci** (CX8, `cpp-cx8` `4d6c0b37`, not merged): `arbace.cpp.run` links the lowered program, the lowered runtime and
  musl's forms as a static link pulls members, checks and builds one node program for ci (`tools/cirun`) and for cf2g;
  `__atomic_*` as plain macros for ci's one thread; classes `threads`, `long-double`, `ci-compound-literal`, `speed`.
- **ci and cf2g** (C5, C6): C forms' interpreter and AOT, about 60 times gcc `-O0`'s time (C5.4).

## 3. What the probes showed

Files in Appendix A. Times on the shared host, 4 threads where the tool takes them.

| probe | what | result |
|---|---|---|
| **P1** clang as the IR's tools | a valid module, a dominance violation, a type error, a module with `poison` and `nsw` overflow; `clang -c`, `clang -S -emit-llvm -Xclang -disable-llvm-passes` | `clang -c` assembles and **verifies** IR input: `error: invalid LLVM IR input: Instruction does not dominate all uses!` with the two instructions; the type error as LLParser's `parse.ll:2:19: error: expected value token` with a caret line. `-S -emit-llvm` on IR input prints the canonical text (llvm-dis's), with two artefacts: a `; ModuleID` line and the host's triple and data layout injected when the module has none (`-Woverride-module`; a module with another target's triple needs `-target` from its own) |
| **P2** LLParser's upgrades | `nocapture` (LLVM ≤ 20's attribute) and typed pointers `i8*` | the pin accepts both: `nocapture` printed as `captures(none)`, `i8*` read as `ptr`. repr/llvm refuses typed pointers (`:typed-pointer`): it is stricter than the reference |
| **P3** version churn | LLVM 15's `llvm-as` over clang 23's IR of 450 torture programs | **234 of 450** accepted at `-O0`, **79 of 450** at `-O2`; first errors `expected type` (`getelementptr inbounds nuw`), `expected ')' at end of argument list` (`captures(none)`), `expected '(' in constantexpr` (`inrange(-16, 8)`), `unterminated attribute group` (`memory(none)`), `expected icmp predicate` (`icmp samesign`). LLVM 15's `lli` ran a hand-written module and clang 23's `-O0` IR of a 3-line C program (exit 4, output equal to the native build's) |
| **P4** what clang emits for C | `gcc.c-torture/execute/2*.c` (475 files; 450 compile with clang) at `-O0` and `-O2` | 3.7 MB and 2.4 MB of IR. `-O0`: 42 opcodes in use, `load` 13,994, `store` 9,305, `call` 6,348 ... `phi` 95, `select` 10; 14 distinct intrinsics (`memcpy`, `memset`, `va_*`, `stacksave`, `abs`, `fmuladd`, math); vector types in 8 files; `poison`/`undef` 3 times. `-O2`: 45 opcodes, `phi` 425, `select` 201, `insertelement` 140, `freeze` 53, `extractelement` 33, `shufflevector` 12; **30 distinct intrinsics** (`lifetime.*`, `assume`, `vector.reduce.add`, `smax`/`smin`/`umax`, `fshl`/`fshr`, `usub.sat`, `ctpop`); vector types in 22 files; `poison`/`undef` 113 times; metadata `!tbaa` 3,649, `!llvm.loop` 105, `!srcloc` 50, `!range`, `!noundef` |
| **P5** repr/llvm over optimized IR | `bin/repr-llvm roundtrip` of the 450 `-O2` modules; then the canonical text of print and original compared | **449 of 450 forms equal, 449 objects equal**, 44 s wall (the objects' 900 `clang -c` runs serial in the script); the refusal `ptrtoaddr` (LLVM 23's new instruction, `20011126-2`). **Canonical text equal: 449 of 449** (`; ModuleID` and `source_filename` lines aside) |
| **P6** random IR | LLVM 15's `llvm-stress -size=60`, seeds 1-10 (vectors, `shufflevector` with `undef` masks, odd widths, `select` on vectors) | clang 23 accepts 10 of 10; **round trip 10 of 10** forms and objects equal |
| **P7** constructs beyond clang's `-O0` | 12 hand-written modules, each valid for the pin | round trip: `bfloat`/`half`, fast-math flags, `or disjoint`, `trunc nuw nsw`, `zext nneg`, a GC statepoint, `ifunc`, `musttail`, `<vscale x 4 x i32>` **pass**; refused: `callbr` (`:instruction`), `ptrtoaddr`, Windows EH (`catchswitch`), **debug records** (`#dbg_value`: `refused:lex` on any `clang -g` IR, `-O0` or `-O2`), module summaries (`^0 = module:`). The scalable vector module has no object on x86 (the back end cannot legalize it): its judge is the canonical text |
| **P8** the lowering on C's IR | `bin/cppi lower` on 8 torture programs at `-O0`, gcc `-O0` with `bin/cppi runtime`'s layer | the lowered C does not link alone: **the layer references `__cxa_begin_catch`, `std::terminate()`, `std::unexpected()`** (a C program's `main` is wrapped as `cx$main` and its calls test `cx$pending`). With `-lstdc++` added, **8 of 8 run as the native build** |
| **P9** the lowering, `-O0` against `-O2` | every 12th of the 450 (38 programs), each level lowered, gcc, run, compared with the source's clang `-O0` build | `-O0`: 37 pass, 1 refused (`blockaddress`, `20040302-1`); `-O2`: 36 pass, the same refusal, and `20040409-3w` aborts (status 134). **The IR's own build aborts too**: the test needs `-fwrapv`, clang's `-O2` IR folded the signed overflow, and the lowering ran the IR as written. Judged against the IR's own build: **74 of 76 agree, 2 refused**. `-O2` vector programs outside the sample (`20000412-6`, `20010224-1`, `20000706-1`: `add`/`sub`/`icmp` on `<8 x i16>`, `<4 x i32>`; `20000801-1`: `shufflevector`) are refused (`:vector`, `:instruction`). About 1 s a module (the process's start dominates small files) |
| **P10** the intrinsic space | the names in `libLLVM-23.so`'s intrinsic table (`strings`, so approximate) | 14,331 `llvm.*` strings: x86 1,685, AArch64 1,727, NVVM 2,611, AMDGCN 1,472, LoongArch 1,535, RISC-V 808, PowerPC 735, ...; 723 outside the target prefixes, among them metadata names and overload spellings (the generic intrinsics a few hundred base names; to verify against `Intrinsics.td`) |

## 4. Read

### 4.1 What exists and what the reference accepts

The reader takes what clang 23 and rustc write at `-O0` (CX6), and in practice optimized IR and random IR too (P5, P6).
The read level's reference is broader: **whatever the pin's LLParser accepts and its Verifier passes**, including what
LLParser upgrades (P2). The gaps found (P2, P5, P7), by kind:

| gap | where it appears | size of the work |
|---|---|---|
| debug records `#dbg_value(...)`, `#dbg_declare`, `#dbg_assign`, `#dbg_label` (LLVM 19 on) | **any `-g` IR**: every client crop's debug builds, much of `llvm/test/DebugInfo` | lexer and a form per record; with LLS19 the `DI*` nodes structured |
| specialized metadata (`!DILocation(...)`, `!DISubprogram(...)` ...) kept as text | read and printed today, but opaque | about 40 node kinds with keyword fields (to verify): a table |
| `callbr`, `ptrtoaddr`, `catchswitch`/`catchpad`/`cleanuppad`/`catchret`/`cleanupret`, `uselistorder`, `uselistorder_bb` | `asm goto` (Linux kernel style C), LLVM 23's address-only casts, Windows EH, bitcode round trips | an instruction each; the forms' shapes in LLVM.md §5's style |
| module summaries `^N = module:/gv:/typeid:` and `flags:`/`blockcount:` | ThinLTO tests in `llvm/test` | a sublanguage of its own (summaries are data): forms by keyword, read and printed, never lowered |
| typed pointers (`i8*`) and other upgraded spellings | old IR, some `llvm/test` files (to count at LL1) | read as written, the canonical text judges (LLS4) |
| target extension types, `x86_amx`, `ppc_fp128`, `addrspace` everywhere, `vscale` | other targets' IR | mostly present (`target(...)`, `vscale`), the rest by type |
| GC (`gc "..."`, `gc.statepoint`), `prologue`, `prefix`, `partition`, `sanitize_*`, `no_sanitize_*`, `code_model` | `llvm/test`'s corners | attributes and options: the keyword scheme takes them |

### 4.2 The judges of read (LLS5)

1. **Forms**: `.ae` text read back equal; the print read again equal (exists).
2. **Canonical text**: the pin's canonical text of the print equal to the original's (P5: 449 of 449), with `-target`
   from the module's own triple and the `; ModuleID`/`source_filename` lines aside. Catches what objects cannot see
   (metadata, attributes without codegen effect, other targets' IR, scalable vectors) and needs no back end.
3. **Objects**: `clang -c` of print and original equal (exists), for x86-64 modules and where the target's back end builds;
   kept for continuity with C++'s records, a file without an object counted by the canonical text alone.

### 4.3 The verifier and the error tests (LLS6)

LLVM's tests of the assembler and the verifier (`llvm/test/Assembler`, `llvm/test/Verifier`, many with `RUN: not llvm-as`
and a `CHECK: error:` line) are the read level's rejection corpus, as `dg-error` tests are C's and gc's error tests Go's.
Arbace's reader rejects some of them today, by accident of its grammar, with its own messages. Proposed: **`arbace.llvm.verify`**,
staged: (i) acceptance (the file rejected or not, as the pin decides), (ii) LLParser's message and position for parse errors
(`file:line:col: error: MESSAGE`), (iii) the Verifier's structural rules with their first line (dominance, a block's single
terminator, `phi` at the head with one entry per predecessor, operand and result types, `landingpad` placement, an
intrinsic's signature, `musttail`'s constraints, metadata well-formedness of the kinds the lowering reads). The lowering
relies on (iii) already (it assumes verified input), so the verifier is also its guard. Message text is LLVM's: a message
reworded between releases is a re-record (§12).

## 5. eval: llvmi

### 5.1 The candidates (LLS7)

| route | what | for | against |
|---|---|---|---|
| **(a) the lowering to C forms, run by ci** (exists for C++) | `arbace.llvm.lower` → C forms → ci (C5); AOT the same C forms by cf2g | one back end for C, C++, Rust and LLVM; AOT for free; gcc `-O0` as an independent judge of the lowering; 74 of 76 already (P9) | two steps of meaning (IR → C → ci): a ci bug and a lowering bug look alike, separated only by gcc's judgment |
| (b) an IR interpreter in Arbace (lli's shape) | SSA values in a frame, blocks, the data layout's memory, intrinsics by table | no C detour; can track `poison` per value and report UB (§6); a semantics reference | a second interpreter beside ci, no AOT (or a second AOT); every intrinsic and every EH path twice; speed unknown (to measure), likely of ci's order since both are Arbace node interpreters |
| (c) (a) for eval and AOT, (b) later as the second judge | | the row's semantics checked twice, by two independent routes | (b)'s cost (LL10, 15 to 25 days) |

*Recommended (LLS7): (a) now, (c)'s interpreter optional later (LL10)*, when the standalone direction or the UB
question (§6) makes it worth having. gcc `-O0` on the lowered C remains the lowering's second judge (LLVM.md §7), so the
row has two judges from day one: gcc for the lowering, the IR's own build for the whole.

### 5.2 The reference (LLS8)

The IR's own build by the pin: `clang -O0 x.ll -o x` (the IR input runs no optimization at `-O0`; the back end's code is
LLVM's meaning of the module), run, status and output recorded as data (`test/llvm/ref-clang23.edn`, `bin/llvmi
record-ref`, the client crops' way). P9 shows why this and not the source's compiler: a module whose IR exploits UB (the
`-O2` fold of `20040409-3w`) is run by the lowering as written, and agrees with the IR's own build, not with the source's
`-O0` build. lli is not a candidate: no LLVM 23 `lli` on the host (P3), and its interpreter has gaps the native build lacks.

### 5.3 The lowering as LLVM's, not C++'s (LL2)

P8's finding: lowered C IR references libsupc++ through the pending-flag layer, because the layer's `cx$uncaught` calls
`__cxa_begin_catch` and `std::terminate`, and every program's `main` is the C++ wrapper. For the row (and for C's IR, and
for Rust, whose panics have their own personality, RUST-EVAL-SURVEY §5.4) the lowering takes the personality as a
parameter: **none** (no `invoke`/`landingpad` in the module: no flag, no wrapper, nothing linked), **Itanium C++**
(`arbace/cpp/rt/`, today's), **Rust** (`arbace/rust/rt/`, RE3); the generic part (the flag, the unwinder's four entry
points, `resume`) in `arbace/llvm/rt/`. The choice is read from the module (its `personality` functions), so a C module
lowers to C that links with musl alone.

### 5.4 The lowering's generic breadth (LL5)

What the row needs beyond the client crops' `-O0` IR, in the order the corpora meet it (P4, P9): vectors elementwise
(arithmetic, `icmp`/`fcmp` to `<N x i1>`, `select`, `shufflevector`, `insertelement`/`extractelement`, `vector.reduce.*`,
`<N x i1>` bitcasts; shared with Rust's RE2, which needs hashbrown's SSE2 group), `blockaddress`/`indirectbr` (as GNU C's
labels-as-values `&&label` and `goto *p`, which gcc takes; ci's support **to verify**), `freeze` and the flags (§6),
`ptrtoaddr`, `callbr` with an empty template (as the barrier case), the generic intrinsics of `-O2` code (`assume`,
`lifetime.*` as nothing, saturating and funnel-shift arithmetic, `vector.reduce.*`, `masked.*` load/store, `fshl`/`fshr`,
`bitreverse`), odd widths and `i128`+ in every operation (the existing masking scheme), `x86_fp80` per CS35. Refused
with a class: inline assembly (`:inline-asm`, as now), target intrinsics outside a small table (§6), `vscale` (no x86 back end
either), GC statepoints, coroutine intrinsics left in the IR (`-O0` clang splits them: none arrive).

### 5.5 Speed

The lowering: about 1 s a small module on stage 0 evaluated (P9), 1 s per 100 KB of IR on larger ones (CX7.2); compiled at
the re-seed that embeds it (LLS16), several times faster (to measure). ci: about 60 times gcc `-O0` (C5.4), with C++'s
startup cost for a C++ module and none for a C module. The class `speed` (C's, goi's, cppi's) takes what passes the cap.

## 6. Undefined behaviour, poison and target intrinsics

- **poison and undef.** The lowering refines them deterministically: `undef` and `poison` constants as zero, `freeze` as a
  copy, `nsw`/`nuw`/`exact`/`disjoint`/`nneg`/`samesign`/`inbounds`/fast-math flags ignored (wrapping arithmetic, plain
  comparisons, IEEE operations). Every such choice is a legal refinement of LangRef's semantics: where the IR has defined
  behaviour, the lowered program computes the same; where it has UB (a branch on poison, a poison value stored and then
  used as an address), any behaviour is allowed, and the IR's own build may have made another choice. So a run that
  differs from the reference **on a module with UB** is not a lowering bug. Proposed (LLS9 (a)): the run corpora are
  UB-free by construction where they can be (the client crops' run programs, llvm-test-suite, the synthetic programs
  generated UB-free), and a difference is classed `ub-refinement` only with evidence (the source's clang `-O0` and `-O2`
  builds disagree, or later LL10's interpreter reports the poison); never by a `:match` on the output alone.
- **Undefined behaviour in the lowered C itself.** The lowering writes C whose behaviour is defined where the IR's is
  (unsigned arithmetic at the container's width, `__builtin_memcpy` for punning, `-fno-strict-aliasing`), so gcc and ci
  cannot exploit what the IR did not allow: LLVM.md §7's rules, kept.
- **Target intrinsics** (`llvm.x86.*`, 1,685 names on x86 alone, P10). The lowering has `llvm.x86.sse2.pause`. Proposed
  (LLS10 (a)): a class `target-intrinsic` for the rest, with a table grown only by what the run corpora reach (x86's SSE2
  and AVX2 integer subsets are the likely first: clang's `<immintrin.h>` code at `-O0` calls them); other targets' IR is
  read and printed, never run (class `target`).
- **Other recorded classes** (proposed, test/llvm/divergences.edn in goi's format): `inline-asm`, `vscale`, `gc-statepoint`,
  `long-double` (CS35, CP84 (a)), `threads` (ci's one thread), `speed`, `float-env` (rounding modes and
  `llvm.experimental.constrained.*`, which ci does not honour), `ub-refinement`, `target-intrinsic`, `target`.

## 7. AOT

The same C forms through cf2g (C6) and Go's compiler, as CX8's `bin/cppi check --aot`; a C module needs no runtime but
musl's forms, so its AOT build is C's. The work is the driver (`bin/llvmi check --aot`) and the shards; no new back end.

## 8. Print

`print` writes one canonical spelling (exists). For the row's print cell, proposed (LLS18 (a)): a **`:full` mode** that
keeps comments, blank lines and the text's spellings at item granularity (C3b's way, repr/c's `:full`), so `llvm/test`'s
files, whose RUN and CHECK lines are comments, print back byte for byte; the forms of `:lines` unchanged. Judges: `:full`
byte for byte; the forms read back equal; the canonical text and the objects equal (§4.2).

## 9. The corpora

| corpus | where | size | levels | license (as data) |
|---|---|---:|---|---|
| **`llvm/test`** IR files: the `.ll` inputs of RUN lines (`llvm-as`, `opt`, `llc`, `llvm-dis`), every target; `split-file` sections as separate modules; FileCheck's `CHECK` lines read as comments, **never run** (they test LLVM's passes, not Arbace) | the pinned `llvm-project-23.1.4.src.tar.xz` (`:llvmorg-23.1.4`, 179 MB, 197,228 entries; fetched at CX1, since removed: a re-fetch at its pin) | on the order of 40,000 `.ll` files (to verify at LL1 from `tar -t`) | read, print | Apache-2.0 WITH LLVM-exception |
| the same, rejections: `Assembler/`, `Verifier/`, `Bitcode/` files with `not llvm-as`/`not opt -passes=verify` | the same | about 1,500 to 3,000 (to verify) | read (rejection) | same |
| **C's run corpora through clang** at `-O0` and `-O2` (torture-execute, the other run corpora of test/c/corpora.edn) | the pinned gcc 15.2.0 tarball, C's lists | 1,532 torture-execute programs (P4: 95% compile with clang) and C's other run corpora | read, print, eval, AOT | GPL-3.0-or-later (gcc's tests) |
| **the client crops' IR**: C++'s run programs and runtime units (38 MB of IR, CX7), Rust's UI programs and std's units (RE2-RE3) | made by their checks, cached by digest | thousands of modules, deduplicated (GT8.2) | read, print (eval stays the crop's: LLS15) | the crops' |
| **synthetic IR** by LangRef construct (GT8.6, GTS207 (a)): an instruction or intrinsic per module, with a `main` that prints its results | generated, committed as a generator | about 300 to 600 modules (to verify) | all four | Arbace's |
| random IR (`llvm-stress`, LLVM 15's: P6) | generated by seed | as needed | read, print (no `main`) | Apache-2.0 WITH LLVM-exception |
| **llvm-test-suite** `SingleSource` (`UnitTests`, `Regression`, `Benchmarks`), compiled to IR by the pin | `:llvm-test-suite-23.1.4`, pinned `:later` at CX1, 173 MB, **not fetched** | about 1,800 programs (to verify) | all four | mixed, to verify at the fetch |

**What `llvm/test` cannot give**: programs with outputs (its files are pass inputs), so eval's corpus is the client
crops', C's through clang, the synthetic one and llvm-test-suite.

## 10. The module layout and the client crops

### 10.1 Layout (LLS13, LLS14)

| piece | today | proposed |
|---|---|---|
| reader, printer | `arbace.repr.llvm` (`arbace/repr/llvm{.ae,/}`) | unchanged, plus `arbace.repr.llvm.full` (LL8) |
| verifier | none | `arbace.llvm.verify` (LL4) |
| lowering | `arbace.cpp.lower` | `arbace.llvm.lower` (RES16 (b), at RE2), the personality a parameter (§5.3) |
| the pending-flag layer | `arbace/cpp/rt/eh.c.ae` | the generic part `arbace/llvm/rt/eh.c.ae`; C++'s personality in `arbace/cpp/rt/`; Rust's in `arbace/rust/rt/` (RE3) |
| linking for ci, the node program | `arbace.cpp.run` (CX8) | `arbace.llvm.run`, with the runtime libraries a parameter (C++'s, Rust's, none) |
| the check and its judges | `arbace.cpp.check` | `arbace.llvm.check` for the IR-level judges; C++'s keeps its own |
| commands | `bin/repr-llvm read|print|roundtrip`; `bin/cppi lower|lower-check|run|check` | `bin/repr-llvm read|print|roundtrip|check|verify`; **`bin/llvmi lower|lower-check|run|check|check --aot|divergences|record-ref|selftest`**; `bin/cppi` keeps C++'s steps and calls `llvmi`'s namespaces |
| spec | `doc/cpp/LLVM.md` | `doc/llvm/SPEC.md` (the forms, LLVM.md §1-§6) and `doc/llvm/LOWER.md` (§7-§11), moved with `git mv` and the references updated; C++'s survey keeps its As-built records as they are |
| records | `test/cpp/lower-*.edn` | `test/llvm/`: `corpora.edn`, `ref-clang23.edn`, `check{,-aot}-linux-amd64.edn`, `divergences{,-read,-aot,-print}.edn`, `roundtrip-known.edn`, `controls.edn`, the synthetic generator |

### 10.2 What changes for C++ and Rust (LLS15)

Their checks keep the language-specific parts: the source to IR by their oracle, their runtimes' builds (libstdc++,
std), their personalities, and their run programs judged against **their** references (g++ 15.2, rustc 1.99.1). What moves
to the LLVM row: the IR-level round trip of every module they produce (deduplicated by digest into the row's corpus: the
round trip runs once per module, whoever made it), the lowering's generic breadth (a missing intrinsic or a vector shape is
the LLVM row's failure, found on a 20-line synthetic module before a client meets it), and gcc's judgment of the lowering
alone (`lower-check`). C++'s `bin/cppi lower-check` and Rust's `bin/rsi lower-check` keep their lists but drop their own
round trip level when the module's digest is in the row's record. C's row is not a client (its eval is C forms directly)
but **its run corpora through clang are the row's best eval corpus**, and a C program run both ways (ci on C forms; ci on
clang's IR lowered) is a cheap differential check of both.

### 10.3 Embedding (LLS16)

C++ embeds at the first re-seed after the tag (CP180, decided). `arbace.repr.llvm` and the lowering are what C++ embeds
first; the row's own namespaces (`arbace.llvm.verify`, `.run`, `.check`) ride on the same re-seed if they exist by then,
else the next batched one (CS250's line). Until then everything is evaluated from the checkout, out of `gate_key`.

## 11. The row and GT8's budget

### 11.1 The row

`test/reap/cells.edn` gains `:llvm` with four cells: read (round trip, canonical text, objects; the verifier's rejections),
eval and AOT (llvmi's records), print (`:full` and the canonical text). Makefile: `repr-llvm-*`, `llvmi-*` (`make
check-makefile`). As every crop since OC120 (b), its steps enter the gate only when its row fits GT8's share. The matrix's
count grows by four cells as an **intermediate row**, printed after the ten languages' rows and named so.

### 11.2 The budget (LLS17: half a crop's share, moved from C++'s and Rust's)

GTS201 (a) gives a crop about 35 minutes nightly and 2.4 hours clean. The row's work is partly the client crops' work
today (the IR round trips, the lowering's breadth), so it takes **half a share** (about 17 minutes nightly, 1.2 hours clean)
and C++ and Rust shed the same. Rates from the probes: the round trip about 0.1 s a module (P5: 450 modules, 44 s at 4
threads, the objects' serial `clang -c` included); the lowering about 1 s a small module (P9); ci about 60 times gcc `-O0`.

| tier | the row's content (proposed) | estimate (to measure at LL6) |
|---|---|---|
| essential (about 30 s) | the synthetic modules' round trip (a few dozen, one per construct family), three llvmi programs from recorded IR on ci (a C module, a vector module, an `invoke`) | 20 to 40 s |
| nightly (17 min) | read and print: the coverage subset of `llvm/test` (GTS205 (a): every block of the reader, the printer and the verifier reached, every class, the controls) plus a seventh of the rest; eval: the synthetic programs and the coverage subset of C's IR at both levels plus a seventh; AOT: the same subset in shards | `llvm/test` whole is about 40,000 × 0.1 s = 67 min at 4 threads (to verify): the subset and rotation are mandatory; eval at 200 to 400 programs about 8 min |
| clean (1.2 h) | every level over the compressed corpora, audited; the rotation's record complete (GTS204 (a)) | within the share if the subsets hold |

Caches (as RES12): IR by (source digest, flags, pin); the round trip's result by (IR digest, repr/llvm's sources); the lowered
C forms by (IR digest, the lowering's sources); references by (IR digest, pin). A module emitted by C++'s and Rust's checks
is judged once.

## 12. The pin and version churn

LLVM promises no stability of the textual format (bitcode is read back across versions; text is not). P3 measures one
span: LLVM 15 reads about half of LLVM 23's `-O0` C IR and a sixth of its `-O2`. Alpine edge moves clang about every
minor release (C++'s CX1 recorded the moving compiler as a risk). Proposed (LLS4 (a)):

- **One version at a time**: the reader's grammar is the pin's LLParser's, plus the upgrades the pin's LLParser itself
  performs (P2: `nocapture`, typed pointers), read as written and printed as written (the canonical text judges).
- **A bump is an event**, as a re-seed is: the pin's lines (`:alpine-clang23`, `:llvmorg-23.1.4`, rustc's LLVM), the IR
  digests (C++'s `test/cpp/runtime/ir.sha256`, Rust's), the references and the verifier's messages re-recorded; the new
  release's `llvm/test` and LangRef's "Changes" read for new spellings; the forms extended, never renamed. clang and rustc's
  LLVM must agree on the version (rustc 1.99.1 is LLVM 23.1.3, read by clang 23.1.4: same minor, P2 of RUST-EVAL-SURVEY).
- Bitcode as an input (LL11, optional) would decouple readers from the text's churn; not v1.

## 13. Plan (LL1-LL9)

Estimates in working days, as the other surveys give them; each milestone ends with its check green and its records.
**Prerequisites, not counted:** C++'s CX8 merged to main; RES16 (b)'s rename at RE2 (Rust's, one mechanical commit).

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **LL1** the spec and pins | `doc/llvm/SPEC.md` and `LOWER.md` moved from `doc/cpp/LLVM.md`; `test/llvm/corpora.edn` (C++'s pins shared; the tarball re-fetched at its pin, the user's go-ahead; llvm-test-suite's fetch decided); `llvm/test` counted by directory and RUN-line kind (inputs, rejections, `split-file`, typed pointers, targets); the synthetic generator's plan | lists counted; the spec accepted | 1-2 |
| **LL2** the move | `arbace.llvm.run`, `arbace.llvm.check`, the layer split and the personality parameter (§5.3), `bin/llvmi` (lower, lower-check), C++'s and Rust's callers updated | C++'s and Rust's records unchanged byte for byte; a C module lowers and links with musl alone (P8 fixed) | 2-3 |
| **LL3** read: the whole grammar | debug records, structured `DI*` metadata (LLS19), `callbr`, `ptrtoaddr`, Windows EH pads, summaries, `uselistorder`, typed pointers as LLParser reads them, the remaining attributes; the canonical-text judge in `bin/repr-llvm roundtrip`; `bin/repr-llvm check` over `llvm/test` | every accepted `llvm/test` module round-trips (forms, canonical text, objects where built) or is in a class | 5-9 |
| **LL4** the verifier | `arbace.llvm.verify`: (i) acceptance, (ii) LLParser's messages and positions, (iii) the structural rules; `bin/repr-llvm verify` | every rejection test rejected with the pin's first line, or classed (`:message`) | 5-8 |
| **LL5** the lowering's breadth | vectors (shared with RE2), `blockaddress`/`indirectbr`, `freeze` and flags as §6, `ptrtoaddr`, the `-O2` intrinsics, the target table's first rows; `bin/llvmi lower-check` (gcc the judge) over C's IR at both levels and the synthetic corpus | every module lowered and run by gcc as the IR's own build, or refused in a class | 6-10 |
| **LL6** llvmi's check | `bin/llvmi run|check|divergences|record-ref` on ci; `test/llvm/ref-clang23.edn`; classes; controls (GTS81 (a)); coverage subsets (GTS205/206) | the check over the run corpora: none failing, every non-pass in a class; costs recorded against §11.2 | 5-8 |
| **LL7** AOT | `bin/llvmi check --aot`, shards | as LL6, compiled | 2-4 |
| **LL8** `:full` | comments and layout at item granularity, byte for byte | every read corpus's `:full` print equal to its text, or classed | 3-5 |
| **LL9** the row | `cells.edn` `:llvm`, the classes files, `bin/reap`, essential subset, steps with `:parts`, Makefile, the nightly within its share (`bin/gate --costs`), C++'s and Rust's checks shrunk (LLS15), the embedding with C++'s re-seed (LLS16) | `bin/reap`: the four LLVM cells checked, none failing | 2-3 |
| *LL10* (optional) an IR interpreter | `arbace.llvm.interp`: SSA frames, memory by the data layout, poison tracked, intrinsics by the lowering's table, `invoke` by the pending flag; a second judge of llvmi | its runs equal llvmi's on the run corpora, poison reported where it decides a branch | 15-25 |
| *LL11* (optional) bitcode | a bitcode reader (`.bc` → LLVM forms): version-stable input, the hub languages that emit bitcode (Julia, Swift) | the canonical text of `.bc` and of its forms' print equal | 8-12 |

**Total without LL10 and LL11: about 31 to 52 working days** (LL1 1-2, LL2 2-3, LL3 5-9, LL4 5-8, LL5 6-10, LL6 5-8, LL7
2-4, LL8 3-5, LL9 2-3); LL5's vectors shared with RE2 (2 to 3 days fewer if Rust does them first). For scale: C++'s CX6-CX8
26 to 44, Rust's RE1-RE9 33 to 58; the surveys have run under their estimates (CX6 and CX7 in about a day each). The least
known parts are LL3's `llvm/test` tail (its size and corners are counted only at LL1) and LL4's verifier; re-estimate at
LL1's end.

**Sonnet and Opus** (the user's split): Sonnet for LL1's counts and spec move, LL2's mechanical move, LL3's grammar
additions and metadata table, LL4's parse errors, LL6's classing and records, LL7's shards, LL8, LL9. Opus for the
personality split (LL2), the verifier's structural rules (LL4), the vector lowering and UB's treatment (LL5, §6), the
classes' judgment, and LL10 if taken.

**Re-seed needs.** LL1-LL8 change tools' namespaces (evaluated from the checkout, out of `gate_key`) and scripts. The
embedding rides on C++'s first re-seed after the tag (LLS16).

## 14. Proposals (LLS1-LLS19)

Each with options, the recommended one named.

- **LLS1 The row.** (a) an intermediate crop, its own row `:llvm`, counted beside the ten languages and not among them,
  printed after them; (b) a part of C++'s row; (c) no row: a library the client crops share. *Recommended: (a)*, the
  user's assessment: the hub's lowering checked once, on its own corpus.
- **LLS2 Timing.** (a) after CX8 merges: LL1 (counts, spec, pins) may start in the light tier beside it, LL2 only after it
  and after RE2's rename; (b) now; (c) after C++'s and Rust's rows. *Recommended: (a)*: the lowering is being changed by
  CX8 and RE2 now; (c) leaves both crops duplicating the IR-level work.
- **LLS3 The reference.** (a) the host's clang 23.1.4 (C++'s pins) as assembler, verifier, canonical printer and native
  builder, no new package; (b) Alpine's `llvm23` tools apk added and pinned (`llvm-as`, `opt`, `lli`, `llc`; `FileCheck` in
  another subpackage, to verify); (c) LLVM 23.1.4's tools built from the pinned tarball into `.tmp/` (hours of build).
  *Recommended: (a)*: P1 shows clang covers every judge the row needs; (b) only if a milestone needs `opt`'s passes, which
  none does.
- **LLS4 Version churn.** (a) one version, the pin's grammar and its LLParser's own upgrades, a bump an event that
  re-records (§12); (b) a reader of several versions' spellings kept forever; (c) bitcode as the only stable input.
  *Recommended: (a)*; (c) as LL11 later.
- **LLS5 Read's judges.** (a) forms equal, the canonical text equal, objects equal where the target builds; (b) objects
  only (today); (c) the canonical text only. *Recommended: (a)*: P5's 449 of 449 shows the canonical text is cheap and
  stricter; objects kept for C++'s continuity.
- **LLS6 The verifier.** (a) `arbace.llvm.verify` staged: acceptance, LLParser's messages and positions, the structural
  rules with their first line; (b) acceptance only; (c) none, clang's verdict recorded. *Recommended: (a)*, with (b) as
  LL4's floor if the messages prove costly.
- **LLS7 Eval's route.** (a) the lowering to C forms on ci (llvmi), gcc `-O0` the lowering's second judge; (b) an IR
  interpreter as eval; (c) (a) and later (b) as a second judge. *Recommended: (a) now, (c)'s interpreter as LL10*.
- **LLS8 Eval's reference.** (a) the IR's own build by the pin, recorded as data; (b) the source's compiler (g++, rustc,
  gcc); (c) lli (absent for LLVM 23 here). *Recommended: (a)*: the subject is the IR (P9's `-fwrapv` case); the source's
  compiler stays the client crops' reference.
- **LLS9 Poison, undef, UB.** (a) a deterministic refinement (§6), run corpora UB-free by construction, `ub-refinement`
  only with evidence; (b) poison tracked by the lowering (a check per flagged operation; slower); (c) modules with
  poison-generating flags refused. *Recommended: (a)*; (b)'s precision belongs to LL10.
- **LLS10 Target intrinsics.** (a) a class `target-intrinsic`, a table grown by what the run corpora reach; other targets'
  IR read and printed only; (b) x86's whole table; (c) refuse. *Recommended: (a)*.
- **LLS11 The corpora.** (a) `llvm/test`'s IR (read, print, rejections), C's run corpora through clang at `-O0` and `-O2`,
  the client crops' IR deduplicated, synthetic IR by construct, random IR, and llvm-test-suite's `SingleSource` if fetched;
  (b) the client crops' IR alone; (c) (a) plus `MultiSource`. *Recommended: (a)*, with the re-fetch of the llvm-project
  tarball at its pin and llvm-test-suite's fetch as the user's go-aheads.
- **LLS12 Optimization levels.** (a) `-O0` and `-O2` IR; (b) `-O0` only. *Recommended: (a)*: `-O2` brings vectors,
  `freeze`, poison and twice the intrinsics (P4); the row is about IR, not clang's `-O0` subset.
- **LLS13 The layout.** (a) as §10.1: `arbace.repr.llvm`, `arbace.llvm.{lower,run,check,verify}`, `arbace/llvm/rt/`, `bin/llvmi`,
  `doc/llvm/`, `test/llvm/`; (b) everything left under `cpp/`. *Recommended: (a)*.
- **LLS14 The evaluator's name.** (a) `llvmi`; (b) `lli` (LLVM's own tool: a clash on any host with LLVM); (c) `iri`.
  *Recommended: (a)*, the crops' pattern.
- **LLS15 What C++ and Rust keep.** (a) their language-specific parts; the IR round trip and the lowering's generic breadth
  judged once in the row, modules deduplicated by digest; (b) both keep everything (the work twice). *Recommended: (a)*.
- **LLS16 Embedding.** (a) with C++ at the first re-seed after the tag (CP180); (b) at a later re-seed of its own.
  *Recommended: (a)*: the reader and the lowering are C++'s embedded namespaces anyway.
- **LLS17 The budget.** (a) half a crop's share (about 17 minutes nightly, 1.2 hours clean), moved from C++'s and Rust's;
  (b) a full share, an eleventh; (c) inside C++'s share. *Recommended: (a)*.
- **LLS18 Print's `:full`.** (a) comments and layout kept at item granularity, `llvm/test`'s files printed back byte for byte;
  (b) the canonical spelling only. *Recommended: (a)*, every crop's print cell has its `:full`.
- **LLS19 Specialized metadata.** (a) the `DI*` nodes and debug records as structured forms (keyword fields); (b) kept as
  text (today). *Recommended: (a)* at LL3: debug records need forms anyway, and the text kept opaque hides what a print changes.

## 15. What to decide first

1. **LLS1, LLS2**: the row as an intermediate crop, and its start after CX8.
2. **LLS3, LLS8**: clang as the only reference, the IR's own build as eval's.
3. **LLS7**: eval through the lowering on ci, the interpreter later.
4. **LLS11**: the corpora, with the two fetches (the llvm-project tarball again, llvm-test-suite).
5. **LLS17**: half a share, taken from C++'s and Rust's.

## 16. Risks

- **The lowering is shared and moving** (CX8, RE2 now): the row started before they merge would chase two branches. LLS2's
  order, and LL2's exit (the clients' records unchanged byte for byte), contain it.
- **`llvm/test` is large and target-heavy**: tens of thousands of files, most for back ends Arbace never runs; the read and
  print levels alone exceed the nightly share whole (§11.2). Dedup, coverage subsets and rotation are mandatory from LL3.
- **Version churn** (§12): Alpine edge moves clang; the textual format changes every release (P3); a bump re-records the
  references, the verifier's messages and the client crops' IR digests at once. The pins catch it; the cost is a day or two
  a bump (to verify at the first).
- **UB in the corpora**: `-O2` IR of programs with UB gives runs that differ legitimately (P9's `-fwrapv` case was caught
  because the IR's own build is the reference; with a source reference it would have looked like a bug). Classing UB
  without evidence would hide real bugs: LLS9 (a) forbids it.
- **Vectors**: the largest generic gap (P9: four of five vector programs refused); shared with Rust's hashbrown. Done
  wrong, a large share of `-O2` programs fails together.
- **Two meanings in one chain** (IR → C → ci): a ci gap (CX8's `ci-compound-literal`) and a lowering bug look alike; gcc's
  judgment of the lowered C separates them, so `lower-check` stays in the nightly.
- **The verifier's messages** are LLVM's prose; matching them is cheap per message but there are many (to count at LL1).
- **Licenses**: `llvm/test`, LangRef and llvm-test-suite's harness are Apache-2.0 WITH LLVM-exception (`LICENSE.TXT` in the
  tarball); llvm-test-suite's programs are under their own licenses (to verify at the fetch). Nothing is vendored by this
  plan: the corpora are read in place from pinned tarballs (no LICENSE.md entry, as C's and C++'s corpora); the synthetic
  generator is Arbace's. A LICENSE.md entry is needed only if a port of LLParser's or the Verifier's code is vendored (LL3,
  LL4 write their own code from LangRef and the messages as data: to decide then).

## 17. Sources

- The host: Alpine `clang23-23.1.4-r0`, `clang23-libs`, `clang23-headers`, `llvm23-libs-23.1.4-r0`,
  `llvm23-linker-tools-23.1.4-r0` (no LLVM 23 tools), `llvm15-15.0.7-r20` (`/usr/lib/llvm15/bin`: `llvm-as`, `opt`, `lli`,
  `llvm-stress`), `/usr/lib/libLLVM-23.so` (P10's names); rustc 1.99.1 (LLVM 23.1.3); gcc 15.2.0; musl 1.2.6.
- Pins (`test/cpp/corpora.edn`, CX1): `:llvmorg-23.1.4` (llvm-project tag `llvmorg-23.1.4`, commit
  `7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`, `llvm-project-23.1.4.src.tar.xz` SHA-256
  `819ecf7351f1a66f76b2d701d8942faaf5faeb324cb90c9d5b090de142434bb2`; Apache-2.0 WITH LLVM-exception), `:llvm-test-suite-23.1.4`
  (`test-suite-23.1.4.src.tar.xz`, SHA-256 `f3c8d4da...` from the release's API, not fetched), `:alpine-clang23` (aports
  `8a39512a`, `main/clang23/APKBUILD`), `:gcc-15.2.0` (the torture programs of P4-P9, `gcc/testsuite/gcc.c-torture/execute/2*.c`).
- In llvm-project (not on the host now; read from memory, to verify at LL1): `llvm/docs/LangRef.rst`,
  `llvm/lib/AsmParser/LLParser.cpp`, `llvm/lib/IR/Verifier.cpp`, `llvm/include/llvm/IR/Intrinsics.td` and the targets'
  `Intrinsics*.td`, `llvm/lib/ExecutionEngine/Interpreter/`, `llvm/test/{Assembler,Verifier,Bitcode,Transforms,CodeGen,DebugInfo}`,
  `llvm/tools/llvm-stress`.
- Arbace: CLAUDE.md; doc/llvm/SPEC.md (§1-§6) and LOWER.md (§7-§10; §11 on `cpp-cx8`); doc/cpp/CPP-SURVEY.md (§5, §6, §8, "As built: CX6",
  "CX7", CP82-CP88, CP100-CP105, CP120-CP127, CP180); `cpp-cx8`'s `4d6c0b37`; `rust-re1`'s `252d9af2`;
  doc/rust/RUST-EVAL-SURVEY.md (RES1, RES12, RES16; decided 2026-11-03); doc/GATE-SURVEY.md ("GT8, the test budget",
  GTS200-GTS209 decided); doc/python/PYTHON-SURVEY.md (the shape); doc/C23-SURVEY.md (C5.4, CS35, CS250);
  `arbace/repr/llvm.ae`, `arbace/repr/llvm/{read,print}.ae`, `arbace/cpp/lower.ae`, `arbace/cpp/rt/eh.c.ae`, `bin/repr-llvm`,
  `bin/cppi`; `test/cpp/corpora.edn`, `test/reap/cells.edn`.
- Prior art (not fetched; to verify): Alive2 `https://github.com/AliveToolkit/alive2`; Vellvm `https://github.com/vellvm/vellvm`;
  llvm-cbe `https://github.com/JuliaHubOSS/llvm-cbe`.

## Appendix A: the scratch

The worktree's `.tmp/llvm-crop/` (26 MB): `ok.ll`, `dom.ll`, `parse.ll`, `ub.ll`, `ok.canon.ll` (P1); `feat/` (P2, P7: twelve
modules, `old.ll`, `typed.ll`, their clang logs); `hello.c`, `hello.ll`, `hello-O2.ll`, `h2.ll`, `hello15.bc`, `lli.out`,
`cpp-{classes,io,coroutine,layouts}.ll` (P3's first probes); `as15/` (P3: LLVM 15's errors over the 450 modules); `gcc/` (the
475 torture programs from the pinned tarball); `t0/`, `t2/` (P4: their IR at `-O0` and `-O2`); `c23/`, `c23o2/` (test/c/c23's
samples' IR, too small to count: left); `rt.list`, `rt/`, `rt2.list`, `rt2/` (P5's round trips), `canon/` (P5's canonical
texts); `stress/`, `stress.list`, `stressrt/` (P6); `featrt/` (P7's round trip); `lw/` (P8: the lowered C, `rt.c`, the
runs), `o2.sample`, `lw2/` (P9: `results.txt`); `intr-names.txt` (P10).

Rule slips: one probe command redirected clang++'s stderr to `/dev/null` (`2>/dev/null`, P3's C++ IR), and one aborted
comparison line used `<(true)` and `>/dev/null` (a no-op left in a loop, replaced by file comparisons); both against the
conventions, neither left a file or a process. `TMPDIR` was the worktree's `.tmp/tmp` for every clang, gcc and Arbace run.
No process was left running.

## Note (2026-11-03): LLVM 23's own tools installed

The user installed `llvm23` and `llvm23-test-utils` (23.1.4, clang's version): `llvm-as`, `opt`, `llvm-dis`, `lli` and
`FileCheck` in `/usr/lib/llvm23/bin` (not on PATH; the host's other LLVM tools are GHC's `llvm15`). The probes above used
clang 23 for want of them; LL1's pins name `llvm23`'s version beside `clang23`'s, and LLS3/LLS8 can take `opt -verify` and
`lli` at the pinned version. doc/ALPINE.md lists the line.

## Decided (2026-11-03, the user): LLS17

LLVM IR's GT8 share is half a crop's (about 17 minutes nightly, 1.2 hours clean), carved from C++'s and Rust's shares (their
checks shrink to the language-specific parts once LLVM checks the shared lowering). The crops are "10+1": ten languages and
LLVM IR beside them. LLS1-LLS16 wait for after the tag (the user).

## As built: LL8 (`:full`, LLS18 (a)) and LL4 (the verifier, LLS6 (a)), 2026-11-04 (write-ahead)

Branch `llvm-full` from `llvm-read` (`5249bc5c`); the details in doc/llvm/SPEC.md §6.1 and §6.2. Both run on the main
checkout's stage 0 (`ARBACE_HOST_EXE`), evaluated from the checkout; no gate step, no whole corpus.

- **`:full`** (`arbace/repr/llvm/full.ae`; `bin/repr-llvm read --full`, `print --full`, `roundtrip --text`): the plain
  forms and a layer kept item by item (a top-level form; a definition's header, labels, instructions, debug records,
  use-list orders and `}`): the gap before each item when it is not the canonical layout's, the item's text when its
  spelling is not the printer's, with the hash of its canonical print (an edited form prints canonically). The printer
  was rebuilt on the same items, its output unchanged byte for byte on the 278 modules. The reader now starts a new block
  after a terminator when the text writes no label, as LLParser does. Over the round trip's lists (278 modules: C's
  torture programs at `-O0`/`-O2`/`-g`, C++ samples at `-O0`/`-O2`/LTO and for Windows, Rust, the six hand-written corner
  modules) and four new hand-written modules with comments (`notes`, `implicit`, `dbg`, `unnamed`): **282 of 282 printed
  back byte for byte**, the `.ae` text read back to the same forms and layer, the plain print read in `:full` with an
  empty layer; 4,322 forms with an entry, 7,130 gaps, 18 texts. The plain round trip over the same 282: forms 282 of 282,
  objects 279 (3 `none`), bitcode 282.
- **The verifier** (`arbace/repr/llvm/verify.ae`; `bin/repr-llvm verify`, `verify-check`): LLParser's checks beyond the
  grammar in the text's order, then the Verifier's rules by function and module, 42 parser rules and 21 Verifier rules
  named, messages in LLVM 23's words (the ported and the missing ones in LLVM.md §6.2). The mutants
  (`arbace/repr/llvm/mutate.ae`, `bin/repr-llvm mutants`): 17 kinds, the moves also renumbered so that the Verifier's
  rules decide. Against `opt -passes=verify` of the pin: the 282 valid modules 0 errors on both; 630 mutants (two sets
  of 315, the second held out from the fitting): **912 of 912 verdicts, 613 of 613 rejections with opt's first message**;
  73 hand-written probes 72 of 73 (a twice-defined label, LLParser merges the blocks).

What is left: `llvm/test`'s rejection tests as the corpus (LLS6 (i), (ii): the tarball's re-fetch), LLParser's syntax
errors with positions (the reader's refusals keep their own words), the attribute, calling-convention, `musttail`,
funclet and metadata-structure rules, a fuller intrinsic table (`Intrinsics.td`'s), and the Makefile's targets for
`roundtrip --text`, `verify-check` and `mutants` (this branch touches only repr/llvm, its script and the docs).

## Decision sheet (2026-11-05)

For the user to decide in one pass. Written from the branches `llvm-read`, `llvm-full`, `llvm-verify2`, `llvm-verify3`
(this branch's base), `cpp-lower-dbg` and `cpp-codegen` (doc/cpp/CODEGEN.md, via `git show cpp-codegen:...`), and the GT9
fragment `gt9-build:test/gate/crops/llvm-rust.edn`. Reading only: nothing was built or run, the figures are those the branches'
docs record. LLS17 and LLS18 are decided (above); LLS5, LLS6 and LLS19 have been done in fact (rows marked "done in fact":
the user may ratify them in a word).

### What changed since the survey (2026-11-03)

- **repr/llvm reads the whole grammar** (`llvm-read`): debug records, the `DI*` nodes as keyword forms, `callbr`,
  `ptrtoaddr`, Windows EH pads, summaries, `uselistorder`; three judges per module (forms, object per the module's own
  triple, `llvm-as`'s bitcode byte for byte: the bitcode is the canonical-text judge of LLS5 (a)). The lists: 282 modules
  (C torture `-O0`/`-O2`/`-g`, C++, LTO, Windows, Rust, hand-written); forms 282 of 282, bitcode 282, objects 279 (3 `none`).
- **`:full`** (`llvm-full`, LLS18 (a)): 282 of 282 printed back byte for byte (LL8 done on that list).
- **The verifier** (`llvm-full`, `llvm-verify2`, `llvm-verify3`): `arbace/repr/llvm/verify.ae` after LLParser and LLVM 23's
  Verifier, now **opt's whole stderr** (messages, positions, culprits): 282 of 282 valid modules as opt's; the held-out
  mutants 1,040 of 1,082 whole stderr (96.1 %), verdicts about 99.8 %; probes 470 of 478 whole stderr (8 known in
  `test/llvm/probes-known.tsv`). Not done: `llvm/test`'s rejection tests as the corpus, a few attribute/DI rules.
- **The lowering took the reader's new forms** (`cpp-lower-dbg`): debug records and `!dbg` dropped (`-g` lowers as `-g0`),
  structured metadata values refused as `:metadata`, `callbr` with an empty template lowered, `:callbr` and `:windows-eh`
  refused. Vectors, `blockaddress` and `ptrtoaddr` are not reported done there; LL5 is open.
- **`llvm23` and `llvm23-test-utils` are installed** (`opt`, `llvm-as`, `lli`, `FileCheck` at clang's 23.1.4): the verifier's
  judge is `opt -passes=verify`; `lli` is available as an IR reference (LLS3, LLS8, LL10). The llvm-project 23.1.4 tarball
  is pinned in `test/cpp/corpora.edn` (fetched-into `.tmp/llvm/`, SHA-512 against Alpine's APKBUILD); llvm-test-suite's pin
  is still `:later`.
- **cpp-codegen makes Arbace a second producer of LLVM forms** (G1: 38 of 38 samples through three levels: lowered and run by
  gcc equal to clang's route, the IR printed and built by `clang++`, every defined name one clang defines). Decided there
  (P1, 2026-11-05): the output is LLVM forms, through the same lowering and runtime. Consequences for this crop: the
  lowering is now the contract between **two** producers (clang's IR and ours), so judging it once, on its own corpus (LLS1),
  is worth more; our IR is a corpus by construction (byte-offset memory, no struct types, no attributes, so it shows what
  the lowering does without clang's attributes: P4 will add `nounwind`), and the verifier of repr/llvm can judge it
  (`opt -passes=verify` is already the native level's check); G2-G6 (about 22 to 33 days) will keep changing
  `arbace.cpp.lower` (SysV classification, exceptions, vtables) for a long time, which makes the LL2 move costlier early.
  `cpp-cx8` (the lowering on ci and compiled; samples 10 of 14, 4 in classes) is not in main as seen from this branch.
- **GT9's llvm fragment** (`test/gate/crops/llvm-rust.edn`, group `:llvm` = `bin/repr-llvm`, `arbace/repr/llvm{.ae,/}`,
  `test/cpp/runtime/`, `test/repr/llvm/`): one interim step, `llvm-roundtrip` (`:phase :full`, chain `llvm-full`, 3 GiB, needs
  `cpp-runtime`; `bin/repr-llvm roundtrip .tmp/cppi/runtime/ir-units.txt .tmp/gate/llvm-roundtrip`; hosts stage0, cxx, llvm;
  pin `test/cpp/corpora.edn`; summary `same|differ|control|FAIL`), over C++'s pinned runtime IR (198 units), half a share
  (GTS221 (a)), control `test/repr/llvm/roundtrip-controls.txt`. It assumes: the verb `roundtrip LIST WORK` (exists on
  `llvm-read`), the controls file (NEW; `test/repr/llvm/` does not exist on this branch), the list
  `test/llvm/essential.txt` (NEW, LL1: until then the runtime's units), and a plan without the runtime's units is GTS221's
  open item. It judges neither `:full`, the verifier nor eval: the step is the round trip alone, so everything built since
  (LL4, LL8) is outside the gate. It needs the files `test/repr/llvm/*` and the essential list added before it can pass.

### The REAP row as it would be now

`test/reap/cells.edn`'s `:languages` is `[:clojure :java :go :c]`; the new levels are the same four. Row `:llvm`, an
intermediate row printed after the languages (LLS1 (a)); cells and their sources:

| cell | sources today | status |
|---|---|---|
| read | `bin/repr-llvm roundtrip` over the 282 modules (forms, object, bitcode); `bin/repr-llvm verify-check` (sets and held-out mutants, probes, vs `opt`), classes by `test/llvm/divergences-read.edn` (new; the 8 probes' known file is the seed) | can be recorded now on the 282-module list and the mutant sets; `llvm/test` not read (needs the tarball extracted and LL1's count) |
| eval | `llvmi`'s run of recorded IR: lowering to C forms on ci, against the IR's own build by clang 23 (`test/llvm/ref-clang23.edn`); today only C++'s `cppi lower-check` (234 of 234 libstdc++ tests, gcc the judge) and cpp-codegen's `codegen-check` (38) exist | no `llvmi`, no reference file: LL2 and LL6 |
| AOT | `llvmi check --aot`: the same C forms through cf2g | C++'s AOT record of the samples (10 of 14) is the only record; LL7 |
| print | `bin/repr-llvm roundtrip --text` (`:full`, 282 of 282), the printed canonical text read by `llvm-as` equal to the original's bitcode | can be recorded now; classes file new |

So at once only read and print could be checked (about two cells of four, on small corpora); eval and AOT wait for LL2, LL6,
LL7. The judging code (`test/reap/main.ae`) needs a reader for repr/llvm's results files and a `:languages` entry for an
intermediate row (a few hours, LL9's).

### Milestones: effectively done

| milestone | state |
|---|---|
| LL1 spec and pins | pins mostly done (tarball pinned, llvm23 named by the note above); the spec is still `doc/cpp/LLVM.md`, not moved; `llvm/test` not counted, no `test/llvm/corpora.edn`, no synthetic generator. Open, about 1 day |
| LL2 the move | not started; see LLS13/LLS2 |
| LL3 read: the whole grammar | **done in substance** (`llvm-read`) on the 282 and the probes; open: `llvm/test`'s whole as the corpus, the classes file, `bin/repr-llvm check` over it |
| LL4 the verifier | **done beyond the survey's stage (iii)** (whole stderr); open: `llvm/test`'s rejection corpus (needs the tarball), a few rules |
| LL5 the lowering's breadth | partial (`cpp-lower-dbg`: debug, `callbr`); vectors, `blockaddress`, `freeze` flags, `ptrtoaddr`, `-O2` intrinsics open |
| LL6, LL7 llvmi's check, AOT | not started |
| LL8 `:full` | **done** on the 282 (`llvm-full`); open: `llvm/test`'s files |
| LL9 the row | not started (the GT9 step is the interim) |
| LL10, LL11 | not started, optional |

Remaining by the survey's estimates: LL1 1-2, LL2 2-3, LL3 about 1-2 (was 5-9), LL4 about 1-2 (was 5-8), LL5 6-10, LL6 5-8, LL7
2-4, LL8 about 0.5, LL9 2-3: **about 21 to 35 days** (was 31 to 52), the saving all in read, verifier and `:full`.

### Decisions

Recommendations carry costs in working days. "Done in fact" rows ask only for ratification.

| id | question (two lines) | recommendation | cost | why, and what changed |
|---|---|---|---|---|
| LLS1 | The row: (a) an intermediate crop, its own row beside the ten languages; (b) part of C++'s row; (c) no row. | **(a)** | LL9 2-3 | Unchanged, stronger: two producers (clang's IR, cpp-codegen's) feed one lowering; one row judges it once. |
| LLS2 | Timing: (a) after CX8 merges, LL2 only after it and RE2's rename; (b) now; (c) after C++'s and Rust's rows. | **(a), refined: LL1, LL3, LL4, LL8 (reader-side, repr/llvm only) go on now; LL2 and LL5 wait until codegen's G2 settles `arbace.cpp.lower`** | 0 extra | The reader side is done without waiting; the lowering is the moving part (CX8 not in main, codegen G2-G6 coming). |
| LLS3 | Reference: (a) the host's clang 23.1.4; (b) the `llvm23` apk; (c) built from the tarball. | **(a)+(b) as installed**: clang for builds and objects, `opt`/`llvm-as` for the verifier and bitcode, `lli` optional; pin both versions in LL1; (c) no | 0 (installed) | The apk is installed and used: the verifier and bitcode judges depend on it. Ratify, and record `llvm23`'s apk version in the pins. |
| LLS4 | Version churn: (a) one version, a bump an event; (b) several versions' spellings; (c) bitcode as the only stable input. | **(a)**; LL11 later | 0 | Unchanged. The bitcode judge already gives a version-pinned check. |
| LLS5 | Read's judges: (a) forms, canonical text, objects. | **done in fact, as (a) with bitcode as the canonical-text judge**; ratify | 0 | `llvm-read` implements forms, object, bitcode: 282 of 282. |
| LLS6 | Verifier: (a) staged, (b) acceptance only, (c) none. | **done in fact, beyond (a)**; ratify. Open: `llvm/test`'s rejections as corpus | 0 now; 1-2 later with the tarball | Whole stderr as opt's, 96 % held out. Stop extending the rules; the corpus's tail finds the rest. |
| LLS7 | Eval's route: (a) lowering to C forms on ci; (b) an IR interpreter; (c) (a), then (b) as a second judge. | **(a), (b) as LL10 only if wanted** | LL6 5-8; LL10 15-25 | cpp-codegen's whole route is (a) (P1); `lli` is installed and gives a cheap semantics reference, so LL10 is less urgent than before. |
| LLS8 | Eval's reference: (a) the IR's own build by the pin as data; (b) the source's compiler; (c) lli. | **(a)**; `lli` as a spot check on small modules | in LL6 | cpp-codegen's check already uses the IR's own build by clang (its "native" level) as one judge: the same machinery. |
| LLS9 | Poison, undef, UB: (a) deterministic refinement, UB-free corpora; (b) tracked; (c) refuse. | **(a)** | in LL5 | Unchanged. Our own IR (no flags written) is UB-free by construction. |
| LLS10 | Target intrinsics: (a) a class, a table grown by what the corpora reach; (b) x86's whole table; (c) refuse. | **(a)** | in LL5 | The verifier's intrinsic table (Intrinsics.td's immarg positions) already covers the read side. |
| LLS11 | Corpora: (a) `llvm/test`, C's run corpora at `-O0`/`-O2`, client crops' IR deduplicated, synthetic, random, llvm-test-suite if fetched; (b) client IR alone; (c) (a)+MultiSource. | **(a)**, adding cpp-codegen's IR; **the go-ahead of the llvm-project tarball's fetch** (about 150 MB, SHA-512 pinned); llvm-test-suite's fetch still deferred | fetch minutes; LL1 counts 1-2 | `llvm/test` is the one corpus the reader, the verifier (rejections) and `:full` all still lack; it unblocks LL1, LL3's close and LL4's close. |
| LLS12 | Optimization levels: (a) `-O0` and `-O2`; (b) `-O0` only. | **(a)** | in LL5 | Already read at both (torture at `-O2` round-trips); only the lowering is `-O0`-centred. |
| LLS13 | Layout: (a) `arbace.repr.llvm`, `arbace.llvm.{lower,run,check,verify}`, `bin/llvmi`, `doc/llvm/`, `test/llvm/`; (b) all left under `cpp/`. | **(a) as the end state, moving LL2 after codegen G2 (or last, by renames only)**; the reader and verifier stay where they are | LL2 2-3 | `arbace.cpp.codegen`, `.sema`, `.abi` now sit on `arbace.cpp.lower`; moving it while G2 edits it costs merges in two crops. The repr half already lives in `arbace/repr/llvm/`. |
| LLS14 | Evaluator's name: (a) `llvmi`; (b) `lli`; (c) `iri`. | **(a)**; `lli` is now installed, which confirms the clash | 0 | Decided by the facts. |
| LLS15 | What C++ and Rust keep: (a) language-specific parts; the generic IR work judged once, deduplicated by digest; (b) both keep everything. | **(a)**; add: C++'s codegen-check stays C++'s (the producer), the lowering's breadth (LL5) is the row's | 0.5 in LL9 | Two producers make the split clearer: producers test themselves, the row tests the shared consumer. |
| LLS16 | Embedding: (a) with C++ at the first re-seed after the tag; (b) its own later re-seed. | **(a)** | 0 (rides the re-seed) | The 2026-11-03 re-seed (d5548e72) was C only; C++'s embedding is the next, now with codegen and sema beside the lowering. |
| LLS19 | Metadata: (a) `DI*` and debug records as structured forms; (b) text. | **done in fact, (a)**; ratify | 0 | `llvm-read` has them (`(!DILocation :line 3 ...)`); the lowering refuses them as `:metadata`, drops debug records. |
| (new) LLS20 | GT9's interim `llvm-roundtrip` step: (a) keep as GT9 has it (the round trip alone, the runtime's units); (b) replace its list by the 282-module list and add `verify-check` and `roundtrip --text` as parts; (c) leave all out of the gate until LL9. | **(b)**, once `test/llvm/essential.txt` and `test/repr/llvm/roundtrip-controls.txt` exist | 0.5-1 | The step as written gates only the oldest part; the verifier and `:full` are most of the new code and have no gate coverage. Within half a share (the 282 list is minutes, the mutant sets must be a sampled part). |

### What to decide first

1. **LLS11's fetch** (the llvm-project tarball): unblocks LL1, the verifier's last corpus and `:full`'s.
2. **LLS2/LLS13's refinement** (reader side now, the move after codegen's G2): the one change to the plan.
3. **Ratify** LLS3, LLS5, LLS6, LLS19 (done in fact) and take LLS1, LLS4, LLS7-LLS10, LLS12, LLS14-LLS16 as recommended in one word.
4. **LLS20**: whether the gate's interim step grows with the verifier and `:full`.

## Decided (2026-11-05, the user)

The decision sheet as recommended: LLS11, the llvm-project tarball fetched once (its SHA-512 pinned); LLS2 and LLS13, the reader side (LL1, LL3, LL4, LL8) now, LL2 and LL5 after C++ codegen's G2; LLS3, LLS5, LLS6 and LLS19 ratified as done; LLS1, LLS4, LLS7-LLS10, LLS12, LLS14-LLS16 as recommended; LLS20, the interim llvm-roundtrip step extended with verify-check and roundtrip --text once its control and essential files exist.

## As built: LL1 (the spec, the pins, llvm/test as a corpus) and the reader side's corpora, 2026-11-05

Branch `llvm-ll1` from `llvm-decisions`. Light runs only (the keyed `--full` of main was running): every process under 2 GiB, `ARBACE_HOST_EXE` the
main checkout's stage 0, samples of a few hundred files. The whole corpus is the night's (below).

- **The fetch** (LLS11, once): `llvm-project-23.1.4.src.tar.xz` from the release's URL in `test/cpp/corpora.edn`, 179,272,572 bytes, SHA-512
  `1443531768d7...bde23` equal to the pin (`bin/llvm-corpus pin`); stored at the main checkout's `.tmp/llvm/`, `llvm/test` and `LICENSE.TXT` extracted
  into `.tmp/llvm/src/` (`bin/llvm-corpus extract`, 1.2 GB). No other network.
- **The spec moved**: `doc/cpp/LLVM.md` is `doc/llvm/SPEC.md` (§1-§6: forms, reader, printer, `:full`, verifier; `git mv`, history kept) and
  `doc/llvm/LOWER.md` (§7-§10, section numbers kept). References updated in `arbace/repr/llvm*`, `bin/repr-llvm` and this survey; left as they are:
  C++'s survey (its records), `arbace/cpp/`, `bin/cppi`, `test/cpp/` and Rust's survey (the lowering's code moves at LL2; C++'s codegen branches edit
  those files).
- **`llvm/test` counted** (`bin/llvm-corpus scan`, 90 s, `test/llvm/llvm-test-counts.tsv`): **44,886 `.ll` files**, 731,590,527 bytes (the survey
  guessed about 40,000). By kind of RUN line: 42,555 inputs, **1,706 rejections** (a `not llvm-as`/`opt`/`llc`/... RUN line), 149 `split-file` tests
  (**328 `.ll` sections** of 147 of them, expanded by the pin's `split-file`), 476 without a RUN line (libraries of other tests). By tool named:
  `llc` 26,431 (CodeGen: 26,261 files, the targets' tests), `opt` 16,094, `llvm-as` 1,322. Typed pointers (`i8*`) in code: **32 files**, of which Bitcode 18
  (upgrade tests). Directories: CodeGen 26,261, Transforms 11,655, Analysis 1,538, DebugInfo 1,183, Instrumentation 610, Assembler 523, Verifier 408
  (309 of them rejections), Bitcode 308, ThinLTO 331, Linker 371, tools 863, LTO 170, Other 172, the rest under 170 each. The counts are by regular
  expression over the RUN lines (a heuristic: `typed` in particular; `ptr0`, `-opaque-pointers` flags, are left out of the samples).
- **The corpora as lists** (`bin/llvm-corpus full-lists`: `.tmp/llvm/lists/{input,reject,split}.list`, NAME<tab>PATH): 42,555 + 1,706 + 328 modules. The
  committed samples (deterministic, stratified by directory, every k-th file of a stratum by sorted path, at most 60 KB each):
  `test/llvm/llvm-test-valid.list` (300, taken by `llvm-as` 23 with no flags: the reference's verdict), `llvm-test-reject.list` (200: `not llvm-as`/`not opt`
  tests that `opt -passes=verify` also rejects; a `not opt` with other passes is left out), `llvm-test-split.list` (60 sections, not all rejected);
  paths relative (`llvm/test/...`, `split/...`), `bin/llvm-corpus resolve LIST OUT` makes them absolute for the `repr-llvm` verbs.
- **A fault the corpus found, fixed**: quoted names (`@"\01L_foo"`) were kept escaped by the lexer, printed `\\01` and read back as another name (3 of 300
  forms differed). The lexer now unescapes `%"..."`, `@"..."` and `$"..."` as LLParser does (`arbace/repr/llvm/read.ae`). And a judge's fault: a module with no
  `source_filename` made clang's object and `llvm-as`'s bitcode depend on the input's path; `roundtrip` copies original and print to one name before
  judging (`bin/repr-llvm`, `judge`). The 282-module lists all wrote one; llvm/test's mostly do not.
- **Read and `:full` over the 300 valid** (`bin/repr-llvm roundtrip`, `roundtrip --text`; records `test/llvm/llvm-test-valid-{rt,judges}.tsv`): forms
  **300 of 300**, bitcode **300 of 300** (the canonical-text judge, LLS5), objects 252 of 300 equal (48 `none`: targets clang cannot build, the
  bitcode judge decides them, no `differs`), `:full` **300 of 300 printed back byte for byte** (2,268 forms with a layer entry, 4,118 gaps, 395 texts).
  Of the 300, 130 CodeGen, 95 Transforms, 21 Analysis, 13 DebugInfo, 12 tools, the rest small directories. One minute at 2 threads.
- **The verifier on the valid sample** (`bin/repr-llvm verify-check`, `test/llvm/llvm-test-valid-verify.tsv`): verdicts **277 of 300** as `opt -passes=verify`'s;
  23 differ: 20 Arbace rejects what opt takes (false positives: `elementtype` on non-intrinsics in 3, `Access type node must be a valid scalar type` in 3, `scope
  points into the type hierarchy` in 6 (old-style `isDefinition:`/`isLocal:` DISubprograms that LLParser upgrades; the declaration rule was fixed to see
  `isDefinition: true`, which exposed the scope rule), `align 1 applied to incompatible type`, `Calling convention disallows stack byref`,
  `branch-target-enforcement`, an addrspace cast, a `<1 x ptr>` cast, one more), 3 where opt only warns (the DXIL target machine, a `DIAssignID` kind).
  These are LL4's tail, listed, not fixed here.
- **The rejection corpus** (`test/llvm/llvm-test-reject-verify.tsv`, `-split-verify.tsv`; `bin/llvm-corpus reject-check LIST WORK`): 200 rejections by the pin:
  Arbace's verdict equal on **77** (invalid), 95 accepted (valid) — missing rules and parse errors, 17 refused by the reader in its own words (not LLParser's),
  11 where opt rejects with the debug-info warning only; the first message equal on 60 of 200, opt's whole stderr on 43. The tests' own declarations
  are the references: 67 of the 200 name an `error:` in a CHECK-style comment (`bin/llvm-corpus expect`), the pin's opt prints that fragment in 65 and Arbace's
  `verify` in 17 (`WORK/expect.tsv`). Over the 60 split sections: verdicts 57 of 60 (3 rejections, 1 with the first message equal). **The verifier is far from the
  rejection tests**: the survey's "a few rules" was written from the mutants Arbace's own mutator makes; llvm/test's tests are the long tail of LLParser's
  messages (DI* field checks, atomics, uselistorder, ptrauth, intrinsic signatures by target: the classes seen in the 95 are `Assembler/invalid-*`, DI field
  ranges, target intrinsics' immargs, `Verifier/` attribute and addrspace rules). Estimate for LL4's close: 3-5 working days, not 1-2 (re-estimate at the
  next check of the whole corpus).
- **LLS20's prerequisites**: `test/llvm/essential.txt` (43 modules: 39 small valid llvm/test files copied to `test/llvm/essential/`, unchanged, and the four
  hand-written `hw` ones; LICENSE.md has the LLVM section for them) and `test/repr/llvm/roundtrip-controls.txt` (the verb's negative control: `bin/repr-llvm
  roundtrip` crosses the subject's print with the reference's original, forms by `arbace.repr.llvm control` and objects by `cmp`, both must differ; the
  mechanism is crop-controls' commit `01c732aa6` ported to this branch's judge file names; the C++ samples' line from that branch is kept and is
  left on lists that lack its names). `bin/repr-llvm roundtrip test/llvm/essential.txt WORK`: 43 of 43 forms, 37 of 43 objects equal (6 `none`), bitcode
  43 of 43, the control fails as it must; 9 s.

### The night's runs (commands and what to expect)

All under `bin/heavy-lock` if run on the loaded host; `ARBACE_HOST_EXE` unset in the gate. `bin/llvm-corpus extract` once, then `scan` and `full-lists`.

| what | command | expected |
|---|---|---|
| pin, counts | `bin/llvm-corpus pin; bin/llvm-corpus extract; bin/llvm-corpus scan` | `pin: ... sha512 ok (179272572 bytes)`; 44,886 `.ll`; counts file unchanged (`git diff test/llvm/llvm-test-counts.tsv` empty); 328 sections |
| read, whole corpus | `bin/llvm-corpus full-lists; bin/repr-llvm roundtrip .tmp/llvm/lists/input.list .tmp/llvm/rt-input` (split in shards of 2,000 with `split -l`; CodeGen's IR is large) | forms: a refusal for every module the reader does not take, listed in `WORK/rt.tsv`; the sample's 300 of 300 predicts most pass; objects `none` for the targets clang cannot build, the bitcode judge `same`. Not known: the tail (the exotic targets' types, `ptr0` tests, old upgrades): classify what `rt.tsv` shows into `test/llvm/divergences-read.edn` |
| `:full` | `bin/repr-llvm roundtrip --text LIST WORK` per shard | every `ok`; any other status a read fault (the sample: 300 of 300) |
| verifier, valid | `bin/repr-llvm verify-check .tmp/llvm/lists/input.list WORK` per shard | most verdicts `valid`/`valid`; the sample's 277 of 300 (92 %) predicts about 8 % false positives, to class |
| verifier, rejections | `bin/llvm-corpus reject-check .tmp/llvm/lists/reject.list WORK`; `... split.list WORK` | the sample's rates: verdicts about 40 %, first message about 30 %; opt rejects only part of the 1,706 under `-passes=verify` (the rest are `llc`'s and the passes' rejections: the verdict column `valid/valid` is then not a failure) |
| peak memory | each verb runs `GOMEMLIMIT=1500MiB`, 4 threads by default | under 2 GiB per process on the sample; a 25 MB module (`CodeGen` has some) may need more: shard by size |

### Proposed

1. **LL4's close as a corpus loop** (3-5 days, Sonnet): take the 95 accepted rejections and the 20 false positives as the work list, grouped by message family; the whole
   corpus after each family. Alternative: leave the tail in `test/llvm/divergences-read.edn` classed `:message`/`:rule` and close LL4 at "every rejection test
   rejected" (acceptance only, 1 day). Recommend the first for `Assembler/` and `Verifier/` (931 files, the part LLVM itself tests the verifier with), the second for
   the target directories.
2. **Whether the interim gate step takes `llvm-test-valid.list` as a part** (LLS20): 300 modules, one minute at 2 threads, 3 GiB: in budget. The sample's records
   (`-valid-rt/judges/verify.tsv`) are the expected outputs.
