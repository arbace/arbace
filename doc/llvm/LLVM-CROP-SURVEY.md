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
- **The 23.1.5 bump (2026-11-08, the user's decision)**: the host's clang, `llvm23` and its libs are 23.1.5-r0; `:alpine-clang23`'s version and
  installed packages, `:llvmorg-23.1.5` and `:llvm-test-suite-23.1.5` (`test/cpp/corpora.edn`), `bin/llvmi`'s and `bin/llvm-corpus`'s tarball names
  now name 23.1.5, and `bin/repr-llvm pins check` holds. The 23.1.4 text of this survey is the record of its writing. Re-recorded at night:
  the references and checks listed in the journal entry of that date (`test/llvm/ref-lli23.edn`, `ref-opt23.edn`, `check-*`, `ir/*.cmd`,
  `llvm-test-counts.tsv`, `essential/`'s corpus lists against the new tarball).
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
| LL5 the lowering's breadth | partial (`cpp-lower-dbg`: debug, `callbr`; vectors and integers wider than 128 bits done, "As built: LL5"); `blockaddress`, `freeze` flags, `ptrtoaddr`, `-O2` intrinsics open |
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

## Decided (2026-11-05, the user: after LL1)

Both as recommended: (1) LL4 closed as a corpus loop over the wrongly accepted rejections and the false errors, the full loop (opt's messages) for Assembler/ and Verifier/, acceptance only for the target directories; (2) GT9's interim llvm-roundtrip step takes llvm-test-valid.list (300 modules) as a part.

## As built: llvmi (LL2's run, LL6, LL7), 2026-11-05

Branch `llvm-llvmi` from `llvm-ll4`, with `cpp-g2fix` (C++ codegen's G2 and its fixes: the settled lowering), `llvm-row` (the REAP row) and main
(the re-seed d5548e72, whose stage 0 embeds main's repr/c) merged in. Light runs only: `ARBACE_HOST_EXE` the main checkout's stage 0, every
Arbace process `GOMEMLIMIT=1500MiB`; peak RSS (busybox `time -v`): the eval check 0.9 GB; AOT's translation (`compile-list`, 2 threads) 0.8 GB a
shard of 40 (1.6 GB for all 230 at once), Go's compiler at most 0.55 GB a package but for two, `c-O{0,2}.synth.lib-musl-tables` (11 MB of Go
each, musl's tables), 2.9 GB: those two are `test/llvm/aot-heavy.txt`, built only with `LLVMI_AOT_HEAVY=1` (the night's run; the record's rows
come from such a run), their recorded rows kept by a light run.

**The route** (LLS7 (a)): `bin/llvmi` over `arbace.llvm.run` and `arbace.llvm.check` (`arbace/llvm/`, out of the embedded sources and
`gate_key` like `arbace/cpp/`). An IR module is read by repr/llvm, normalized for the lowering (`arbace.llvm.run/normalize`: typed
pointers `(* T)` as `ptr`, as LLVM 23 reads them; LLParser's implicit numbers of unnamed arguments, blocks and value instructions
written in, which old `llvm/test` files rely on and clang never writes), lowered by the shared lowering (`arbace.cpp.lower`, LOWER.md;
the move to `arbace.llvm.lower` is left to LL2's rename, below), printed by repr/c with **the personality-free layer**
`arbace/llvm/rt/none.c.ae` appended (§5.3's "none": `cx$pending` never raised, `cx$resume`/`cx$uncaught` abort, the renamed `atexit`,
`at_quick_exit`, `__cxa_atexit` and `pthread_create` passed to musl), so a module is **one C translation unit that links with musl
alone**; a module that unwinds (a personality, `invoke`, `landingpad`, `resume`) is refused `:personality`. ci runs no
`__attribute__((constructor))`/`((destructor))` function, so llvmi strips them from the lowering's `cx$ctor$N`/`cx$dtor$N` and `main` calls
the constructors by priority and registers the destructors with `atexit`. Eval: `arbace.c.run`'s `nodes-list` builds the node programs
(musl's prebuilt runtime), `tools/cirun` runs them. AOT (§7): `arbace.c.cf2g`'s `compile-list` translates them into Go packages over
`go/arbace/ci` and `go/arbace/crt`, built by TamaGo's go in shards of 40 (one executable a shard, as `bin/ci check --aot`).

**The judge** (LLS8, as the brief took it): lli 23's run of the same module (the host's `/usr/lib/llvm23/bin/lli`; empty stdin, no
arguments, 10 s), status, stdout and stderr by SHA-256, recorded as data (`test/llvm/ref-lli23.edn`, `bin/llvmi record-ref`), so the check
needs no lli; each entry also records the module's digest (a reference from other IR is `:stale-ref`, not a divergence) and, as a second
opinion, whether clang 23 `-O0`'s build of the module agrees with lli (`:clang`: 240 `:same`, 14 `:no-build`, 4 `:differs`: argv[0],
environment, a 32-bit data layout, and `test-interp-variable-arguments`, whose output is not deterministic under lli either).
Rows `[ID RESULT REASON]` (goi's format) in `test/llvm/check-eval-linux-amd64.edn` and `check-aot-linux-amd64.edn`; classes in GR10's format,
`test/llvm/divergences-eval.edn` (AOT: `divergences-aot.edn` first, empty, then eval's); a recorded pass that does not pass fails; the
negative controls `test/llvm/controls.edn` (three: a subject's run against another module's reference) must fail, and do.

**The corpus** (`bin/llvmi modules`, 258 modules): `ee`, `llvm/test/ExecutionEngine`'s 80 modules with a `main` whose first RUN line is a
plain lli run (read in place from the main checkout's `.tmp/llvm/src`); `c-O0` and `c-O2`, test/c's `synth` and `run` programs through clang 23
`-S -emit-llvm` (81 of 97 each; 16 are gcc's dialect clang rejects); `cpp`, C++'s samples and runtime samples with a `main` (16). The C++ runtime's
units have no `main`: read and lowered by `bin/cppi runtime-build`, not run here.

**Numbers** (eval and AOT the same modules, the same results):

| level | modules | lowered | pass | not passing, in classes | seconds (lower / build / run) |
|---|---:|---:|---:|---|---|
| eval (`bin/llvmi check`) | 258 | 230 | **218** | 40: personality 13, vector 10, ci-builtins 5, wide-int 2, crash-report 2, ub-refinement 2, abi-punning 2, argv0, datalayout, target-type, va-arg | 81 s wall: 15 / 33 / 6 |
| AOT (`bin/llvmi check --aot`) | 258 | 230 | **218** | the same 40 in the same classes (none of AOT's own) | 18.7 min wall: 14 s lowering, then 6 shards of translation, `go build` and runs, 1,075 s |

None failing; every non-pass in a class; the controls fail as they must. What the check found and fixed in the lowering: the vector casts
and `shufflevector` crashed it (now refused `:vector`), `llvm.global_dtors` was refused (now `destructor` wrappers), and the intrinsics
`scmp`/`ucmp`, `fshl`/`fshr` and `l(l)round`/`l(l)rint` (C23's `<stdbit.h>` and `-O2` code) are in its table (LOWER.md §7).

**The REAP row**: `test/reap/cells.edn`'s `[:llvm :eval]` and `[:llvm :aot]` read the two records (the `:until` gone).

### The night's runs (commands and what to expect)

| what | command | expected |
|---|---|---|
| the references | `bin/llvmi record-ref` (after `bin/llvm-corpus extract`) | `258 runs recorded`; `git diff test/llvm/ref-lli23.edn` empty but `test-interp-variable-arguments`'s stdout (lli's own nondeterminism; the module is refused `:va-arg`) |
| eval | `bin/llvmi check` (`make llvmi-check`) | about 90 s, under 1 GB: 218 pass, 40 in classes, the controls 3 judged 0 passing, exit 0 |
| AOT | `LLVMI_AOT_HEAVY=1 bin/llvmi check --aot --record` (`make llvmi-check-aot`, without the heavy two) | about 19 minutes (6 shards), the same 218; `git diff test/llvm/check-aot-linux-amd64.edn` empty; one `go build` at 2.9 GB |
| the classes alone | `bin/llvmi divergences [--aot]` | exit 0 |
| the matrix | `bin/reap` | the LLVM row's four cells checked |

### Proposed

1. **LLS21 ci's gaps the row found.** The lowering writes what gcc takes and ci's builder lacks: `__builtin_fmodf`/`fmodl` (`frem` on float and
   long double: `builtin-aliases` has `fmod` only) and gcc's `__atomic_*` builtins (`__atomic_fetch_add` ...); and ci converts a lowered
   comparator's `unsigned` result to the `int` of musl's `qsort` call as a value (`abi-punning`: gcc reads the same register). (a) add the
   aliases and the `__atomic_*` family to ci (C's crop, `arbace/c/build.ae`, about 0.5 day) and, for the punning, have ci truncate a call's
   result through a function pointer to the pointer's return width (C's crop, 0.5 day; or (a') the lowering types i32 results of address-taken
   functions as `int`, which changes C++'s records); (b) keep them as classes. Recommend (a): 7 of the 40 go, C's own corpora gain.
2. **LLS22 the C++ personality on ci.** 13 modules are refused `:personality`. (a) link C++'s lowered runtime (`bin/cppi runtime-build`'s
   libsupc++ and libstdc++ as C forms) into ci's build for such modules, the way `arbace.cpp.run` (branch `cppi-check`) does for cppi: one
   runtime per key, cached (1-2 days, the memory of a ci build of libstdc++ to measure); (b) leave them to cppi's own check (LLS15 (a): the
   producer tests itself). Recommend (b) now, (a) with LL2's split of the layer.
3. **LL2's rename** (`arbace.cpp.lower` to `arbace.llvm.lower`): still not done, since C++'s codegen branches edit the file; recommend doing it
   by `git mv` and a one-line `require` change in each client after C++'s next merge to main, when no branch holds an edit of it.
4. **The essential tier** (§11.2): three llvmi modules on ci (a C module, an ExecutionEngine module, a constructor module, about 10 s) as an
   essential gate step beside the interim llvm-roundtrip step, once the row enters the gate; the eval check (90 s) nightly, AOT (19 min) in
   the clean run.

## Decided (2026-11-05, the user: LLS21, LLS22 and llvmi's two)

LLS21 (a): fmodf/fmodl, the __atomic_* family and the function-pointer return width in ci; LLS22 (b) now, the C++ personality left to cppi's check, (a) with LL2; arbace.cpp.lower renamed arbace.llvm.lower once no branch edits it; an essential step of 3 llvmi modules when the row enters the gate, eval nightly, AOT in the clean run.

## As built: LL5 (vectors and integers wider than 128 bits in the lowering), 2026-11-05

Branch `llvm-ll5` (from `llvm-llvmi`); all in `arbace/cpp/lower.ae`, the new functions in two clearly separate blocks (the vector
section before `call-stmts`, `inst-stmts` wrapping the scalar `inst-stmts*`, `intrinsic` wrapping `intrinsic*`) so C++'s branches merge
without touching them.

- **Vectors, scalarized.** A vector value was already a C struct with the member `e`, the array of its lanes. `vector-stmts` takes every
  instruction with a vector operand and writes one scalar operation a lane on the lanes' C expressions (`(aget (.-e v) i)`): the integer
  and floating binary operators (through `binop`), `fneg`, `icmp` and `fcmp` (the scalar bodies are now `icmp-expr` and `fcmp-expr`; the
  result type `<N x i1>`), the casts between vectors lane by lane (`cast-expr`), `select` with a vector condition, `shufflevector` (a mask
  of constants, `zeroinitializer` or a splat; an undefined lane takes lane 0), and `bitcast` (`<N x i1>` and iN by bits, every other by
  bytes through memory, little endian). `splat` constants are a compound literal of N copies. The vector intrinsics: the elementwise ones
  (`fabs`, `sqrt`, `smax`, `abs`, `ctpop`, `fshl`, `fma`, ... `lane-intrinsics`) call the scalar intrinsic's lowering per lane (the lane
  as a `(c-expr E)` operand, which `value` takes), and `llvm.vector.reduce.{add,mul,and,or,xor,smax,smin,umax,umin,fadd,fmul,fmax,fmin}`
  fold the lanes in order. Not done: scalable vectors (refused `:vector`), `llvm.masked.*`, gather and scatter, the vector `*.with.overflow`
  and `.sat` intrinsics, vector constant expressions (`add` of two vector constants), a `splat` in a static initializer.
- **Integers wider than 128 bits as C's `_BitInt(N)`** (`(unsigned-bit-int N)`, ci has it since C23's crop; the literal `(lit (unsigned-bit-int N) n)`),
  exactly N bits, so no masking: `container-bits` returns N above 128, `utype` and `stype` are functions, `int-lit`, `mask` and `store-bytes`
  take the case (a load and a store of i200 move its 25 bytes, not the 32 of the C type). `llvm.fshl/fshr` at any width, and `ctpop`, `ctlz`
  and `cttz` above 64 bits (i128's rules kept) by 64-bit limbs. Still refused: `switch` on more than 64 bits, the overflow intrinsics above
  128, atomics.
- **Numbers.** `bin/llvmi check` (whole, 2 min wall on the loaded host, ci with musl's forms): 258 modules, 242 lowered, **229 pass (218
  before)**, the 29 others in the recorded classes; the `:vector` (10) and `:wide-int` (2) classes are gone from
  `test/llvm/divergences-eval.edn`. The ten modules of `:vector` and the two of `:wide-int` now pass at eval and AOT but one:
  `ee.MCJIT.test-interp-vec-arithm_float` lowers and runs up to `frem <N x float>`, which is ci's missing `__builtin_fmodf` (class
  `ci-builtins`, LLS21 (a)). AOT: `--aot --only` on the thirteen modules (the twelve above and `c-O2.run.ptr-sub-narrow`, whose `splat` it also
  took), 12 pass, the thirteenth the same `fmodf` fault; their rows in `check-aot-linux-amd64.edn` set from that run (a whole AOT run, 19
  minutes and heavy, not made: the other rows are unchanged by construction, the change touching only vector and wide-integer code).
  `bin/llvmi check --record` rewrote `check-eval-linux-amd64.edn`.
- **gcc as the judge.** The C of the vector modules and of `c-O0.run.builtin-stdc` compiles with gcc (gnu23); the vector ones run the same.
  `builtin-stdc`, which uses `_BitInt(129)` and `_BitInt(256)` counts, passes on ci against lli but segfaults under gcc at its `_BitInt(129)`
  line (gcc's own `_BitInt` library; the lowering's C is valid): C++'s `bin/cppi lower-check` is not affected unless a program uses an
  integer above 128 bits.

### Proposed

1. Add `fmodf`/`fmodl` to ci's `builtin-aliases` (arbace/c/build.ae) with LLS21 (a): the last vector module passes and `frem` in
   general; recommended, one line and a ci rebuild.
2. A whole `bin/llvmi check --aot --record` at night (LLVMI_AOT_HEAVY=1) to replace the hand-set AOT rows by a recorded run; recommended.

## Decided (2026-11-05, the user: LL5's proposals)

LL5's two as recommended: fmodf/fmodl via ci's builtin-aliases (built on ci-lls21, LLS21 (a)); a whole bin/llvmi check --aot --record with LLVMI_AOT_HEAVY=1 at night replacing the hand-set rows.

## Decided (2026-11-06, the user: the torture corpora's IR pinned)

The clang-made IR of llvmi's torture corpora (`tx-O0`, `tx-O2`: gcc 9.1.0's `gcc.c-torture/execute` through clang 23) is committed as
one deterministic tar.xz pinned by its SHA-256, so the eval check needs no clang; the IR is regenerated by a recorded command when the
pin (clang's version, the flags or the sources) moves.

## As built: LL6 (llvmi's coverage, the pinned IR, the records for opt, clang and llvm-as), 2026-11-06

Branch `llvm-ll6` from `llvm-ll5`. Light runs only (the main checkout's stage 0 as `ARBACE_HOST_EXE`, every process under 1.5 GiB,
samples): no gate step, no whole corpus. The C++ crops' forms are unchanged: the new lowering behaviour is under `*ci*` (below), false
for C++'s lowering for gcc.

- **The torture corpora** (`bin/llvmi`'s `tx-O0`, `tx-O2`): gcc 9.1.0's `gcc.c-torture/execute` and `ieee/` (C's `:torture-execute`,
  ccorpus2 at its pin; not `builtins/`, which links `builtins/lib/`) through clang 23 in gnu17 with gcc's leniencies as warnings
  (implicit declarations and int, int conversions): 1,520 modules at each level (44 files clang refuses, or without a `main`).
  The default check takes a deterministic sample, every 10th module (`test/llvm/tx-sample.txt`, 304), the whole with
  `LLVMI_TX_ALL=1` (the night's). lli's runs of the sample are in `test/llvm/ref-lli23.edn`.
- **The IR pinned** (the user's decision above): `test/llvm/ir/tx.tar.xz` (3,040 modules, 829 KB) and `c.tar.xz` (C's synth and run
  programs at `-O0`/`-O2` and C++'s samples, 176 modules, 222 KB), each deterministic (GNU tar sorted, mtime 0, xz -9e; made twice, the
  same SHA-256), pinned by `PACK.tar.xz.sha256`, with its manifest `PACK.tsv` (ID, source, the source's SHA-256, the IR's) and the
  command that made it `PACK.cmd` (clang's version and flags); `bin/llvmi ir-make [PACK]` makes them again. The check reads the IR from
  the packs: no clang (proved with clang, clang++, lli and opt shadowed by failing scripts on `PATH`: none called, the same rows); a
  source of the manifest present here with another digest makes the pack stale, an error; `LLVMI_CLANG=1` compiles afresh as before.
- **The lowering's faults the corpus found** (root causes):
  1. `arbace.llvm.run`'s numbering of unnamed values counted a void call to a variadic function (`(call (fn void [...]) ...)`, its
     type written as the callee's) as a value and gave it the next number, overwriting `%1..%N`: their declarations went missing
     (`'l$1' undeclared`, 6 modules). The return type is the `fn` type's, and an explicit `%N` now sets the counter, as LLParser does.
  2. va_list on ci (6 modules, segfaults): clang's inline `va_arg` code reads SysV's `{gp_offset, fp_offset, overflow, reg_save}`,
     ci's `va_list` is a slot pointer. Under `arbace.cpp.lower/*ci*` the lowering keeps the SysV form with every register used and
     the overflow area ci's slots, and passes the overflow pointer to libc's v* functions (LOWER.md, "LL6"); the `va_arg`
     instruction is lowered (both models), so the `:va-arg` class is gone.
  3. `blockaddress` and `indirectbr` (computed goto; clang hoists the label table into a global): a token per block and a switch.
  4. `llvm.{u,s}{add,sub}.sat`, `llvm.returnaddress`, `llvm.frameaddress` in the intrinsic table.
  5. `llvm.stackrestore` was nothing, so a loop's VLAs exhausted ci's stack (`vla-dealloc-1`): under `*ci*` a restoring function's
     dynamic allocas come from the heap on a chain freed back to the saved head, onto a spare list reused (the million iterations in
     seconds, not a minute).
- **Classes** (`test/llvm/divergences-eval.edn`): `:long-double` (design: ci's long double is binary64, CS35: an aggregate with an
  x86_fp80 has another size, 10 of the sample), `:lli-reference` (design: lli's JIT is not the module's meaning: it links every
  external at once, crashes where clang's build runs, or the module is the interpreter's, `test-interp-variable-arguments`, whose CHECK
  line llvmi's output meets), `:stackrestore` (deferred, emptied by 5 above; kept for the night's whole corpus).
- **opt recorded** (`bin/repr-llvm record-opt LIST...`, `test/llvm/ref-opt23.edn`, 1,288 modules: llvm/test's three samples, its 446
  modules `opt -passes=verify` rejects (`test/llvm/llvm-test-reject-opt.list`), the probes, essential and hand-written lists): each
  module's SHA-256, opt's verdict, first message and its whole stderr's SHA-256 with the module's path and opt's own written `<input>`
  and `opt`. `verify-check` without opt (or with `REPR_LLVM_REF`) judges from the record: the same verdicts, messages and stderr
  equalities as with opt on every list (checked list by list); a module not recorded is `unrecorded`, one changed `stale`.
- **clang and llvm-as recorded** (`bin/repr-llvm record-rt LIST WORK`, `test/llvm/ref-rt23.edn`, 304 modules: the valid sample,
  essential, hand-written): the original's and the print's SHA-256 with the object's and the bitcode's verdicts; `roundtrip` without
  clang (or with `REPR_LLVM_RTREF`) carries the verdicts to the same original printed to the same text, and calls any other print
  `unjudged`, a failure (a printer change needs `record-rt` with the tools: the print is judged only by LLVM itself). The negative
  control is judged by its forms then.
- **The verifier**: `GEP into vector with non-byte-addressable element type`, `get_dynamic_area_offset`'s width, the `modular-format`
  attribute's rules, MemProf's `!memprof` and `!callsite` (each message and culprit as opt's, checked on the test and on the 120
  llvm/test files that use them), and in the reader LLParser's own errors for a word that is no type (llvm/test's RUN-line templates,
  `iXLen`: refused as llvm-as refuses them, 8 of a fresh 999-file sample, the others of that sample read: forms 991 of 991, bitcode
  988), for `captures(...)` (parseCapturesAttr: `ret:` once, `none` alone) and `initializes(...)` (parseInitializesAttr), and for
  `!DIAssignID` with an operand (19 of 19 split-file sections of `Assembler/captures-errors` and `initializes-attribute-invalid`, opt's
  whole stderr; 150 of 150 llvm/test files that use the two attributes). False errors on valid modules, from the 150 and a fresh
  999-file valid sample, fixed at their root: a `bitcast`/`addrspacecast` in `@llvm.used` walked as a GEP (a crash), byte counts above
  2^63 parsed as longs (a crash), a self-reference in an unreachable block (allowed: `!DT.isReachableFromEntry`), a named type that
  aliases a non-struct type (`%T = type ptr` is `ptr` itself: `alias-type` in `norm-type`), and `common` globals' null values (+0.0, and
  aggregates of nulls: `null-value?`). That sample's other false errors are AutoUpgrade's old intrinsic forms (x86's
  `*-upgrade` tests, `llvm.experimental.vector.extract`, old argument counts: 7) and two debug-info warnings; they are not in a record
  (the sample's verdicts were checked against opt, not recorded). Four probes known since LL4 now pass (`attrs-p17`, `cc-c10`,
  `musttail-q38`, `metadata-w51`), out of `test/llvm/probes-known.tsv`; the probes' record is rewritten (475 of 478 verdicts).
- **The REAP row**: read gains llvm/test's 806 verified modules and 304 round-tripped (judged from the records), print the same 304
  printed back byte for byte; the non-passes of the 806 are in `:llvmtest-rules` (14: rules not ported, mostly AMDGPU's, VP and
  gc.relocate intrinsics, inline asm constraints) and `:llvmtest-messages` (45: the verdict opt's, another first message or culprit,
  mostly LLParser's error order: an earlier error the lexer or the parser reports first). `test/reap/matrix.edn` re-recorded.

**Numbers** (before: `llvm-ll5`'s records; after: this branch's):

| what | before | after |
|---|---|---|
| REAP LLVM read (pass / design / deferred) | 2,695 / 1 / 58 | 3,750 / 1 / 113 (llvm/test's 1,110 rows added) |
| REAP LLVM eval | 229 / 7 / 22 (258 modules) | 519 / 22 / 21 (562 modules: the 258 and the torture sample; `bin/llvmi check` exits 0, 2.5 min, 1.5 GB) |
| REAP LLVM AOT | 229 / 8 / 21 (LL5's hand-set rows among them) | 519 / 22 / 21 (`bin/llvmi check --aot --record`, a recorded run, 33 min, 1.5 GB; every row's result the eval's; the two heavy modules' rows kept) |
| REAP LLVM print | 282 / 0 / 0 | 586 / 0 / 0 |
| llvmi on the torture sample (304) | 271 | 290 (AOT before the stackrestore fix, `--aot --corpus tx-O0,tx-O2`: 288, the same rows as eval's then, 7.9 min, 1.4 GB) |
| verify on llvm/test's 446 opt rejections: verdicts / first message / whole stderr | 428 / 399 / 381 | 433 / 406 / 388 |
| verify on llvm/test's 806 (the samples and the rejections) | 791 verdicts, 744 whole stderr (this branch's first record) | 792, 747 |
| verify on a fresh 999-file valid sample of llvm/test (not recorded) | 984 verdicts | 988 (AutoUpgrade's old intrinsic forms 7, debug-info warnings 2, a SPIR-V target warning, a template opt also rejects but with its own first message) |
| verify on llvm/test's 300 valid, 200 rejection, 60 split samples | 300, 195, 59 verdicts | the same |
| host LLVM tools a check needs | eval: clang (c, cpp); verify-check: opt; roundtrip: clang, llvm-as | none (records; the tools only to record) |

### The night's runs (commands and what to expect)

| what | command | expected |
|---|---|---|
| lli's runs of the whole torture corpora | `LLVMI_TX_ALL=1 bin/llvmi record-ref --corpus tx-O0,tx-O2` | `3040 runs recorded`; the sample's 304 entries unchanged (`git diff` adds lines only) |
| eval, whole | `LLVMI_TX_ALL=1 bin/llvmi check` (about 16 min, 1.5 GB) | about 95 % pass by the sample; new non-passes classed in `divergences-eval.edn` (`:long-double` by its static assertions, `:lli-reference`), then `--record` without `LLVMI_TX_ALL` for the committed rows |
| AOT, the heavy two | `LLVMI_AOT_HEAVY=1 bin/llvmi check --aot --record` (about 35 min, one `go build` at 2.9 GB: above the day's limit) | the day's recorded run (519 of 562, the eval's rows) unchanged; the two heavy modules' rows from their own run |
| the matrix | `bin/reap --record` after the records | 20 of 20, 0 problems |

### Proposed

1. **LLS23 the torture corpora whole in the default check.** (a) keep the 304 sample by default and the whole at night (as built:
   2.5 min for the 562 modules); (b) the whole by default (about 16 min, the eval check then over GT8's half share). Recommend (a).
2. **LLS24 a canonical printer** (AsmWriter's output, `llvm-dis`'s text, byte for byte): the round trip could then be judged with
   no LLVM tool at all, the reference the recorded digest of `llvm-as | llvm-dis` of the original, robust to Arbace's printer changes
   (the record-rt judges hold only for the print they saw). Cost: a port of AsmWriter's slot numbering, attribute groups and comments,
   about 3-5 days. Recommend after LL9, if the row's prints change often.
3. **LLS25 the verifier's tail as data.** The 62 llvm/test non-passes are mostly target intrinsics (AMDGPU) and LLParser's error order.
   (a) port AMDGPU's intrinsic rules and LLParser's order for Assembler/ (2-3 days); (b) leave them classed. Recommend (b) for the
   target rules (the decided "acceptance for the target directories" holds: they are in Verifier/AMDGPU), (a) for Assembler/'s order
   when LL9 puts the row in the gate.
4. **LLS26 the torture IR's license.** `test/llvm/ir/tx.tar.xz` is a derivative of gcc's testsuite (GPL-3.0-or-later): the repo then
   distributes GPL-licensed test data, not ported code (the rule "GPL sources are not ported" is about code). (a) keep it and add the
   GPL version 3's full text to LICENSE.md (it is named there now; the text is not on this host: one fetch of
   `https://www.gnu.org/licenses/gpl-3.0.txt`, or gcc 15.2.0's `COPYING3` out of the pinned tarball C's crop already uses); (b) keep
   only `c.tar.xz` committed and make `tx` locally by `bin/llvmi ir-make tx` (clang again at the check for the torture corpora);
   (c) a corpus of permissive programs in its place (CompCert's tests are not permissive either; tcc's tests2 are LGPL). Recommend (a)
   with gcc 15.2.0's `COPYING3` from the pinned tarball (no network): done so in LICENSE.md, for the user to ratify or undo.

## Decided (2026-11-06, the user: LLS23-LLS26)

- **LLS23 (a):** the 304-module torture sample stays in the default check; all 3,040 modules run at night (`LLVMI_TX_ALL=1`).
- **LLS24 (a):** a printer writing exactly `llvm-dis`'s text, so the round trip needs no LLVM tool, after LL9.
- **LLS25 (a):** the AMDGPU verifier rules stay classed; the Assembler errors' order is fixed when LL9 puts the row in the gate.
- **LLS26 (a), ratified:** `test/llvm/ir/tx.tar.xz` stays committed as GPL-3.0-or-later test data (clang's IR of gcc's torture
  tests), with GPL version 3's text in `LICENSE.md` from the pinned gcc 15.2.0 tarball's `COPYING3`; the rule against porting GPL
  sources is about code, not test data.

## Decided (2026-11-07, the user: the judges, independence)

- **A test's own verdict is a judge no toolchain made** (1 in `bin/crops-score`'s independence). Every check judges by the test's
  own verdict wherever it has one: gcc's torture execute programs by exit 0 (`abort()` fails), the round trip by its forms read
  again equal and the print's acceptance by Arbace's own verifier, mode `:full` by the text printed back byte for byte. The
  toolchain's recorded outputs (`ref-lli23.edn`, the clang, llvm-as and opt runs) move to the oracle tier, run by hand at a tag. A test
  without an own verdict keeps its recorded reference (0.5).
- **A mixed stage is the case-weighted mean of its cases** (relayed 2026-11-07, about 07:45): a case judged by its own verdict counts
  1, one judged against a recorded toolchain reference 0.5.

## As built: LL9a (the own verdicts, the gate's steps, the controls), 2026-11-07

Branch `llvm-90` from `llvm-ll6` (`153ac9e91`). Light runs only, by day: the main checkout's stage 0 as `ARBACE_HOST_EXE`, every
process under 1.5 GiB (measured below), the committed lists and llvmi's default sample; no whole corpus, no gate run but `--plan`.

**The judges.** `bin/repr-llvm` and `bin/llvmi` run no LLVM tool unless asked. The judge's mode is `REPR_LLVM_JUDGE`: `own` (the
default), `record` (also when `REPR_LLVM_REF` or `REPR_LLVM_RTREF` names a record), `live` (also `REPR_LLVM_LIVE=1`). llvmi's is
`LLVMI_JUDGE`: `own` (the default) or `lli`. Before LL9a, a host with `/usr/lib/llvm23/bin/opt` or `clang` on `PATH` ran them at the
check. The record-* verbs run live (`record-rt` sets `REPR_LLVM_JUDGE=live`).

- **The round trip** (`bin/repr-llvm roundtrip`): the forms read again equal (as before), and the print's verdict by Arbace's verifier
  the original's (`WORK/verifier.tsv`: `verify-list` over both). clang's objects and llvm-as's bitcode are `record` or `live` only.
  `bin/lib/repr-llvm/results roundtrip` takes the verifier as a judge.
- **verify-check**: a module's own verdict comes from its RUN lines (`own_verdicts`, `WORK/own.tsv`). `invalid`: a `not llvm-as`, or
  a `not opt` with no pass but verify, on the file with no option that changes how it is read. Only `-o`, `-S`, `-disable-output`,
  `-passes=verify` and redirections are allowed. `-data-layout "P39"` is such an option, as in
  `Bitcode/function-nonzero-address-spaces`, whose plain `not llvm-as` line decides it. With it goes the `error:` fragment that the
  check lines of the prefixes those RUN lines give FileCheck declare. `valid`: RUN lines, none of them a `not` of an LLVM tool, and
  no split-file. `-`: none of these, so opt's record judges. An own `valid` is met by verify's valid or `debug`; an own `invalid` is
  met by invalid, with the fragment in verify's output when one is declared. `WORK/verify.tsv`'s eighth column says whose reference
  judged the module, and `full.tsv` says `own` for those.
- **llvmi** (`arbace.llvm.check/own-verdict`):
  - `ee.*`: lit's verdict. The RUN lines run lli on the file with no argument; the run must exit 0, and when its output is piped to
    FileCheck the `CHECK:` lines must be found in stdout, in order and with blanks collapsed (FileCheck's rule). Any other directive
    gives no own verdict: CHECK-NEXT, a prefix, `{{regex}}`, an argument, a `not`, an XFAIL for x86_64. stdout is kept for this
    (`LLVMI_RUN_OUT`, `WORK/out/ID.out`).
  - `tx-*`: gcc's torture harness's verdict, exit 0.
  - The C and C++ samples' modules keep lli's record (no own verdict: C's crop judges them by gcc's record).
- **ci without gcc**: `bin/ci musl prepare` configures musl with `CC=gcc ./configure` when its tree is absent. `bin/llvmi`'s
  `musl_prepare` derives configure's products first, as musl's Makefile makes them (`sed` over `arch/x86_64`'s `.in` files,
  `VERSION`), with a `config.mak` that holds `ARCH`. The three headers are byte for byte gcc's configure's (checked against the
  configured tree), so ci's forms cache stays fresh. C's own target (branch `c-indep`, `c/README.md`) does the same in `bin/lib/ci/musl`.

**The check families as gate steps** (`bin/repr-llvm check FAMILY [--essential] [--record]`):
- Families: `roundtrip-llvmtest`, `text-llvmtest`, `verify-llvmtest` and `verify-probes`. Each runs its lists (llvm/test's from the
  pinned tarball's extraction, the rest the checkout's) and judges the rows against the family's record. A recorded pass that does
  not pass, or an unrecorded module, fails; a fixed one is reported. A negative control that passes, or is not judged, fails.
- `--essential` takes `test/llvm/essential.txt`'s modules in the family's record, with four llvm/test rejections copied into
  `test/llvm/essential/` (`essential-verify.txt`) and four probes (`essential-probes.txt`).
- `bin/llvmi check --essential`: `test/llvm/essential-check.txt`, ten modules: a C program, ExecutionEngine modules, a constructor
  module, two torture programs and the controls' modules. This is LLS21's essential tier.
- `test/gate/steps.edn`, group `:llvm`, crop `:llvm` (`test/gate/costs.ae` takes it):
  - Essential, chain `llvm-essential`: `llvm-read-essential`, `llvm-print-essential`, `llvmi-essential` and `llvm-controls-test`.
  - `--full`, chain `llvm-full`: `llvm-read` (parts by family), `llvm-print`, `llvmi-check` and `llvmi-aot`.
  - All of them run on `:stage0` (llvmi's also on `:go`, TamaGo's); none runs on `:llvm`, `:cxx` or `:gcc`.
  - `bin/gate --plan` and `--full --plan` plan them; `test/gate/costs-test.bash` passes.
  - `reap`'s inputs name `test/llvm/*.edn`. `bin/llvm-corpus` reads the invoking checkout's extraction from a clean run's clone.

**Negative controls** (GTS81 (a)):
- `test/repr/llvm/verify-controls.txt` has four controls:
  - a valid module against a rejection's own verdict;
  - a rejection against another's declared fragment;
  - two pairs of probes against opt's record.
- `test/repr/llvm/text-controls.txt` has two: a subject's `:full` print against another module's text (`arbace.repr.llvm
  text-control`).
- `test/llvm/controls.edn` gains a fourth: `c-O0.run.ptr-sub-unsigned` exits 3, against a torture program's own verdict.
- The round trip's controls are unchanged; in mode `own` the forms decide them.
- `test/repr/llvm/controls-test.bash` (the gate's `llvm-controls-test`, 42 s) checks the wiring. Each verb is given a control that
  crosses a module with itself, and must fail and say so: roundtrip, `--text`, verify-check, `bin/repr-llvm check` and
  `bin/llvmi check`. With the committed controls each must pass. Result: 9 of 9 ok.

**Numbers** (stubs first on `PATH`: `clang clang++ clang-23 gcc g++ cc llvm-as opt lli llvm-dis split-file FileCheck`, each logging
and exiting 99; the log was empty after every run below):

| check | command | result | wall, peak |
|---|---|---|---|
| verify, llvm/test's 806 | `bin/repr-llvm check verify-llvmtest --record` | 781 pass (747 before); 690 by own verdict, 116 by opt's record; 34 fixed (other first messages and stderr that opt's record called differences), 0 regressed | 17 s, 0.65 GB |
| round trip, 304 | `bin/repr-llvm check roundtrip-llvmtest --record` | 304 of 304, the verifier taking every print | 21 s, 0.66 GB |
| `:full`, 304 | `bin/repr-llvm check text-llvmtest` | 304 of 304, both controls fail | 9 s, 0.39 GB |
| probes, 478 | `bin/repr-llvm check verify-probes` | 472, as recorded (opt's record) | 12 s, 0.52 GB |
| the essential subsets | `bin/repr-llvm check FAMILY --essential` | 43, 43 + 4, 4 and 43 modules, as recorded | 8 s each |
| llvmi eval, 562 | `bin/llvmi check --record` | **522 pass** (519 before): 385 by own verdict, 177 by lli's record | 145 s, 1.48 GB |
| llvmi AOT | LL6's recorded AOT run (`.tmp/ll6/aot-all`) judged again, with the seven FileCheck and control modules rebuilt (`--aot --only`, 38 s, 0.8 GB) | 522, every row eval's | |
| llvmi essential | `bin/llvmi check --essential` | 10 of 10, 4 controls fail; also with ci's musl tree removed (derived again, no gcc) | 12 s |

What the own verdicts changed in llvmi:
- **Now pass**:
  - `ee.MCJIT.2002-12-16-ArgTest`: lli's argv[0] was the difference.
  - `tx-O0.ieee.fp-cmp-7`: lli could not resolve `link_error`.
  - `tx-O2.20010122-1`: lli crashed.
- **New classes**:
  - `:dg-options` (deferred): `restrict-1` (`-fgnu89-inline`) and `20040409-3w` (`-fwrapv`). The pack's single command does not pass
    a test's dg-options, so its IR is another program.
  - `:envp` (design): `2003-08-21-EnvironmentTest` reads `envp[0]`, and ci's runs have an empty environment.
- **`:lli-reference` keeps only `test-interp-variable-arguments`**. Its pointer-wide `va_list` is the interpreter's; llvmi prints
  `result is 4`, not the CHECK line's 6. LL6's note that llvmi met the CHECK line was wrong; the own verdict found it.
- `bin/reap`: 20 of 20, 0 problems. The sources' names say "own verdicts".

**The crop's scores** (`bin/crops-score --ref llvm-90 --crop llvm`, from a scratch worktree of `mac/crops-score` with
`.tmp/crops-rules.diff` and `.tmp/independence.diff` applied):

| | completeness (without by design) | maturity | independence |
|---|---|---|---|
| `mac/llvm-ll6`, the baseline | 91.7 (92.9) | 71.4 (`llvm-ll6` at 153ac9e91 with mac/crops-score's rules: 70.0) | 40.0 |
| `llvm-90` | **95.9** (97.3) | **93.3** (controls 6 of 10 families, gate 1) | **68.3** |

- Independence by stage: read 0.706, eval 0.5, AOT 0.5, print 1, judge 0.711.
- Read: 1,588 cases on hand-written IR and 2,276 on clang's.
- Eval and AOT: the packs, and ci's host record `test/c/host/predefined`.
- Judge: 2,350 of 5,574 cases by their own verdict.
- The four families without a control are the first records' 282-module lists and mutants, which cannot be rerun (LLS28).

### The night's runs (commands and what to expect)

| what | command | expected |
|---|---|---|
| AOT, a recorded run | `bin/llvmi check --aot --record` (about 33 min, 1.5 GB; `LLVMI_AOT_HEAVY=1` adds the two heavy modules, one `go build` at 2.9 GB) | 522 of 562, the eval's rows; `git diff test/llvm/check-aot-linux-amd64.edn` empty |
| the torture corpora whole | `LLVMI_TX_ALL=1 bin/llvmi check` (about 16 min, 1.5 GB) | by own verdict; new non-passes classed (`:long-double`, `:dg-options`) |
| the oracle tier, by hand | `REPR_LLVM_JUDGE=live bin/repr-llvm check FAMILY` for each family; `LLVMI_JUDGE=lli bin/llvmi check` | the toolchain's verdicts against the own verdicts: differences reported, not recorded |
| the gate's steps | `bin/gate` (the four essential steps, about 1.5 min) and `bin/gate --full` (`llvm-read`, `llvm-print`, `llvmi-check`, `llvmi-aot`) | every step passes; then `bin/gate --audit llvm-read-essential llvmi-essential` (inputs declared) |

### Proposed

1. **LLS27: a pinned input corpus in the crop's own language.**
   - The question: does IR that clang made, committed and pinned (`test/llvm/ir`), count as "a toolchain's product consumed" (0.5)?
     Or is it the crop's test data, as hand-written IR is (1)?
   - The rule's text (doc/CROPS-SCORE.md, "Independence") names "rustc's and clang's IR" among the inputs a stage consumes, so as
     written it is 0.5.
   - The case for 1: OCaml's Lambda, GHC's STG and C++'s clang IR stand in for a front end the crop lacks, so the toolchain does part
     of the crop's work. For the LLVM IR crop the IR is the language under test, the toolchain does none of the crop's work (reading,
     lowering, running and the own verdicts are Arbace's), and nothing the toolchain printed about the IR is the reference. It is as
     gcc's torture C sources are to C.
   - Options:
     - (a) a pinned input corpus in the crop's own language is test data, 1, when no output of the toolchain about it is a
       reference (doc/CROPS-SCORE.md's rule, one sentence);
     - (b) as written, 0.5; 1 only for hand-written IR or IR from Arbace's own producers (C++'s codegen, `cpp-codegen`), weighted
       by case;
     - (c) replace clang's IR by Arbace's producers. That gives 1 under either rule, but costs C++'s codegen's coverage of the
       torture programs (weeks).
   - Recommended: (a), with (c) as the long-term route. Under (a), with C's own target merged (ci's predefined macros from
     `c/target`, not gcc's record) and LLS28 (a), independence is (1 + 1 + 1 + 1 + 0.711) / 5 = **94.2**. Under (a) without C's
     target, it is 74.2: eval and AOT stay at 0.5.
2. **LLS28: the first records' 282-module lists.** The 282 modules (C's torture programs at -O0/-O2/-g, C++ samples, Rust crates,
   the six corner modules) and their 630 and 1,082 mutants were made by the host's clang and are in no checkout. Their four record
   families (2,558 cases) cannot be rerun, and they have neither a gate step nor a control.
   - (a) Pack the 282 modules as a pinned IR pack (`bin/llvmi ir-make`'s way; one clang run by hand, at night). The mutants are
     regenerated by `bin/repr-llvm mutants` (deterministic); the families join `bin/repr-llvm check` with controls.
   - (b) Retire them from the REAP cells.
   - Recommended: (a). Maturity's controls item would go to 10 of 10 (maturity 100), and the read stage to 1 under LLS27 (a).
3. **LLS29: dg-options in the torture pack.** `bin/llvmi ir-make tx` to pass each torture test's `dg-options` and
   `dg-additional-options` (the `.c` file's own lines). The pack changes for the files that have them, and needs clang once, at
   night. Recommended with the next pack, when LLS28 (a) packs.
4. **LLS30: the probes' own verdicts.** The 478 probes are Arbace's hand-written modules, judged by opt's record (0.5). Writing
   each probe's expected verdict and message into it would copy opt's output into the test. Recommended: no, keep the record (the
   probes are the verifier's regression set, not its specification).

## Decided (2026-11-07, the user: LLS27-LLS30)

- **LLS27 (a):** a pinned input corpus in the crop's own language counts 1 when no output of a toolchain about it is a
  reference (doc/CROPS-SCORE.md, "Decided (the user, 2026-11-07): a pinned input corpus in the crop's own language"). The
  packs `test/llvm/ir/{tx,c}.tar.xz` and llvm/test's files from the pinned tarball are test data (1); opt's and lli's records stay
  the judge's 0.5; the first records' 282 modules and mutants, in no checkout, stay 0.5 until LLS28's pack.
- **LLS28 (a):** pack the 282 modules once with clang, at night, and regenerate the mutants with Arbace's mutator
  (`bin/repr-llvm mutants`, deterministic by index).
- **LLS29:** dg-options with the next pack (`bin/llvmi ir-make tx` passing each torture test's `dg-options` and
  `dg-additional-options`).
- **LLS30:** the probes keep opt's record (they are the verifier's regression set, not its specification).

## As merged: batch 2 (2026-11-07, branch `batch2`)

`llvm-read`, `cpp-lower-dbg`, `llvm-full`, `llvm-verify3`, `gt9v-llvm`, `llvm-ll6` (with `cpp-native`, `cpp-native2`, `cpp-sema`,
`cpp-codegen`, `cpp-codegen2`, `cpp-g2fix`, `cpp-templates` and `llvm-row` inside it) and `llvm-90`, one merge commit each, on
batch 1 and rsi's `rust-re5`, `gt9v-rust`, `rust-re6`. Not merged: `llvm-ll4` (LL4's corpus loop, 8 commits on `llvm-verify3`, never
merged into `llvm-ll5`/`ll6`: it conflicts with LL9a in `arbace/repr/llvm/verify.ae`, the SPEC and this survey; to be rebased onto
batch 2 with its records re-made, proposal LLS32).

- **The doc split.** `llvm-ll6` moved doc/cpp/LLVM.md's sections 1-6 to doc/llvm/SPEC.md and 7-10 to doc/llvm/LOWER.md; batch 1's
  changes to the old file (CX8's section 11, cppi-deferred's slot unions, atomics and va_list fields, cpp-lower-dbg's debug
  records) were merged into LOWER.md.
- **The lowering** (`arbace/cpp/lower.ae`): batch 1's lowering (CX8's ci lowering, rust-re4/re5's rustc intrinsics and vectors,
  cppi-deferred, cpp-lower-dbg) and ll6's (LL5's vectors, LL6's `*ci*`) are two implementations of the same features; the merge
  commit takes ll6's and the next commit integrates batch 1's (branch `batch2-lower`).
- **The keys' list.** `arbace/llvm/` (llvmi's namespaces) joined `bin/lib/version.bash`'s `arbace_crop_paths`, `test/gate/audit.ae`'s
  `crop-rules` and `arbace/j2g/embed.ae`'s exclusions (the keys commit had `arbace/rust/` but not it).
- **The gate.** llvm-90's eight steps (`llvm-essential`: `llvm-read-essential`, `llvm-print-essential`, `llvmi-essential`,
  `llvm-controls-test`; `llvm-full`: `llvm-read`, `llvm-print`, `llvmi-check`, `llvmi-aot`), all on `:stage0` and `:go`, with their
  `:controls` (verify-controls, text-controls, `test/llvm/controls.edn`; `llvm-controls-test` waived as the wiring test itself);
  `test/gate/crops/llvm-rust.edn`'s LLVM interim step `llvm-roundtrip` superseded. `gate.crops check`: 0 problems.
- **llvmi on C's own target** (LLS27 (a)'s condition for eval and AOT): `bin/llvmi` sources `bin/lib/repr-c/sysroot.bash` and runs
  ci on `c/target` and Arbace's sysroot (`c_sysroot`; `ARBACE_C_TARGET=gcc`: gcc's `test/c/host`, the oracle tier's). Checked:
  `bin/llvmi check --essential` 10 of 10 (4 controls fail as they must, 22 s, 0.6 GB); a sample of 70 modules of the eval record
  (every 8th) 66 pass and 4 in their recorded classes, exit 0 (49 s, 0.75 GB); `--aot --only` 5 of them, 5 of 5 (39 s, 1.1 GB).
- **Independence** (`test/crops/independence.edn`'s `:llvm :stages`, batch 2's): read 0.706, eval 1, AOT 1, print 1, judge 0.711:
  **88.3%** (68.3% on `llvm-90`).

### LLS28 prepared: what the pack needs

The 282 modules' ids (`test/llvm/check-roundtrip-linux-amd64.edn`) are: 52 torture programs of gcc's `execute` in five builds each
(`.g0`, `.g2`, `.o2`, `.lto`, `.win`: 260), nine C++ samples for a Mach-O target (`.mac`), one Rust module (`rs.lt`), ten
`stress1`-`stress10`, the 21 corner modules (`dbg-O0` ... `typed`: debug info, Windows EH, callbr, LTO's summaries and CFI, the
sanitizers, `typed` pointers) and the four hand-written `hw-*` (committed in `test/llvm/hw/`). Their lists and the corner modules'
sources were in the worktrees of `llvm-read` and `llvm-full` and are gone with them (no copy under the main checkout's `.tmp/`): the
commands that made them are not recorded either. So the pack cannot be made as the record's inputs byte for byte; what can be
made at night, after `bin/llvmi ir-make` learns a pack `rt` (a day's task, below):

    bin/llvmi ir-make rt                         # clang 23.1.4: the 52 torture programs x {-O0 -g, -O2 -g, -O2, -O2 -flto, --target=x86_64-pc-windows-msvc},
                                                 # the C++ samples for x86_64-apple-macosx, one Rust crate's IR (rustc --emit=llvm-ir), the stress set; ~10 min, < 1 GB
    bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-a 315 0      # the two fitted sets and the held-out sets, at their indices
    bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-b 315 315
    bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-h1 543 500000
    bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-h2 539 600000
    REPR_LLVM_JUDGE=live bin/repr-llvm record-rt .tmp/llvm/rt.list .tmp/llvm/rt-rec     # the oracle tier's references, once
    bin/repr-llvm check roundtrip-rt --record; bin/repr-llvm check text-rt --record; bin/repr-llvm check verify-rt --record

The families `roundtrip-rt`, `text-rt` and `verify-rt` (with controls) are new verbs of `bin/repr-llvm check`, as LL9a's four. The
record's numbers change (new inputs). See LLS31 below.

### Proposed (batch 2)

- **LLS31 The 282 modules' pack, without their sources.** (a) Rebuild what can be rebuilt as the pack `rt` (the 260 torture builds,
  the C++ samples' Mach-O builds, a Rust module, the stress set; the four `hw-*` committed), drop the 21 corner modules (their
  constructs are among llvm/test's 304 and 806 and the probes), re-make the mutants from the pack and re-record the families: cost a
  day's verb (`ir-make rt`, three `check` families with controls) and a night's clang run; the REAP read cell's numbers change once.
  (b) Retire the four families of the 282 lists from the read cells (LLS28's old option (b)): the read stage is then llvm/test's and
  the probes' alone, all pinned (read 1 now, independence 94.2%), completeness counted on fewer cases; cost: the C torture programs'
  and the C++ samples' IR leave the read level (llvmi's eval still runs the packs'). *Recommended: (a)*, (b) meanwhile if the user
  wants the score now.
- **LLS32 `llvm-ll4`.** LL4's corpus loop (wider rejection samples, LLParser's messages at more parseToken sites, three valid samples'
  false errors) is on a line of its own. (a) Rebase it onto batch 2 (its `verify.ae` changes against LL9a's, SPEC §6.2.3 against
  ll6's SPEC, the survey's sections), its records re-made by verify-check (light: seconds); an agent's day; (b) leave it. *Recommended: (a)*.

## Decided (the user, 2026-11-07): LLS31 (b) now, (a) later

- **LLS31 (b), built on batch 2** (2026-11-07): the first records' four families (the 282 modules' round trip, their verify-check with
  the 630 mutants, the 1,082 held-out mutants, and the 282 modules' mode `:full` text: 2,558 cases on inputs the host's clang made and
  no checkout holds) leave the REAP cells: `test/reap/cells.edn` keeps them as `:role :beside` sources (listed, not counted), their
  records unchanged. The read cell is llvm/test's pinned files, the probes and `hw` (1,557 / 1 / 30 of 1,588), print llvm/test's
  304. `test/crops/independence.edn`: read 1 (LLS27 (a): pinned input in the crop's language), judge (2,068 + 0.5 x 948) / 3,016 =
  0.843 (the retired families' 912 + 1,082 opt records, 282 objects and bitcode and 282 own texts no longer counted). LLVM IR
  95.9 / 93.3 / 88.3 -> **96.0 / 100 / 96.9** (maturity's controls item 6 of 6 families: the four without a control are beside).
  The estimate of 94.2 kept the judge at 0.711; it moves with the cases.
- **LLS31 (a), later**: the pack `rt` rebuilt and the families back as checks, below.

### LLS31 (a): the pack rt, planned

What the first records were made from, by the ids of `test/llvm/check-roundtrip-linux-amd64.edn` (282): 42 torture programs of gcc
9.1.0's `gcc.c-torture/execute` (ccorpus2 at its pin, `bin/llvmi`'s `$tx`) in up to five builds (`.g0` 51 and `.g2` 51 with the nine C++
samples, `.o2` 41, `.lto` 50, `.win` 44: the builds clang accepted), the nine C++ samples of `test/cpp/samples/` for a Mach-O target
(`.mac`), one Rust module (`rs.lt`), `stress1`-`stress10` (`llvm-stress -size=60`, seeds 1-10), the 21 corner modules (`dbg-O0` ...
`typed`, sources lost) and the four `hw-*` (committed, `test/llvm/hw/`). The pack rebuilds all but the corners:

1. **The day's verb (light, no clang run by day).** `bin/llvmi`: a pack `rt` beside `tx` and `c` (`pack_of`, `ir-make`'s case, the
   manifest `test/llvm/ir/rt.tsv` with `rt.cmd` and `rt.tar.xz.sha256`), its corpus set `rt` made from a committed list
   `test/llvm/rt-builds.tsv` (ID, SOURCE, BUILD), one line per id above but the corners, written from the record's ids by a
   `bin/llvmi rt-list` (deterministic: the ids' suffixes give the build). The builds, as `modules`' tx lines do (gnu17, `-w
   -fno-strict-aliasing $tx_flags -ffile-prefix-map=TX/=`): `.g0` `-O0 -g`, `.g2` `-O2 -g`, `.o2` `-O2`, `.lto` `-O2 -flto` (its
   IR text), `.win` `-O2 --target=x86_64-pc-windows-msvc`; `.mac` `clang++ -O0 -std=c++23 --target=x86_64-apple-macosx` of the C++
   samples; `rs.lt` `rustc --emit=llvm-ir -C lto=thin` of `test/rust/sample/shapes.rs` (the oracle tier's rustc, by hand); `stressN`
   `${REPR_LLVM_BIN:-/usr/lib/llvm23/bin}/llvm-stress -size=60 -seed=N`. A module clang refuses is not one (as `ir-make`'s rule).
   `bin/repr-llvm check` learns the families `roundtrip-rt`, `text-rt`, `verify-rt` (lists from the pack, as LL9a's four), each
   with a crossed-pair control in `test/repr/llvm/{roundtrip,text,verify}-controls.txt`.
2. **The night's clang run (one heavy-free job, under 1 GB):**

       bin/llvmi ir-make rt                                                  # clang 23.1.5 (the host's since 2026-11-06), about 10 min
       bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-a 315 0         # the fitted sets and the held-out sets, at their indices
       bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-b 315 315
       bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-h1 543 500000
       bin/repr-llvm mutants .tmp/llvm/rt.list .tmp/llvm/mut-h2 539 600000
       REPR_LLVM_JUDGE=live bin/repr-llvm record-rt .tmp/llvm/rt.list .tmp/llvm/rt-rec   # the oracle tier's references, once
       bin/repr-llvm check roundtrip-rt --record; bin/repr-llvm check text-rt --record; bin/repr-llvm check verify-rt --record

3. **Then (a day):** the four `:beside` sources of `test/reap/cells.edn` replaced by the three families' records as checks,
   `bin/reap --record`, the read and judge facts recounted (read stays 1: the pack is pinned input in the crop's language, LLS27 (a);
   the judge gains the families' own verdicts and opt's records at 0.5), the gate's `llvm-read` and `llvm-print` taking the new
   families as parts. The numbers change once (new inputs: the corners dropped, the builds clang 23.1.5's).
## As built: LLS31 (a), the pack rt's verb and families (2026-11-08, branch `llvm-rt-pack`)

Decided by the user 2026-11-07, built on next `7696e50da` (clang 23.1.5 since cpp-repin). The day's part of "LLS31 (a): the pack rt,
planned": the verb, the mutant sets, the families with controls, proved on subsets. The pack itself, its records and the cells' switch
are the night's (below). Corrections to the plan: the first records held **41** torture programs, not 42 (the 41 `.o2` ids). Five of
them are not in gcc 9.1.0's execute but are in gcc 15.2.0's: `alias-access-path-2`, `pr110165-1`, `pr119071`, `pr93582`, `pr99751`.
The first records' fitted sets were 340 indices each (0-339 and 1000-1339, 315 mutants each, of the base family's 17 kinds), not "315 0"
and "315 315". The held-out sets were 600 indices each (500000-500599 and 600000-600599, every kind).

**`test/llvm/rt-builds.tsv`** (`bin/llvmi rt-list`, from the ids of `test/llvm/check-roundtrip-linux-amd64.edn`) has 257 modules, one line
each with ID, SOURCE and BUILD:
- the 41 torture programs from the pinned gcc 15.2.0 tarball (`gcc15:NAME.c`, `test/c/corpora.edn`'s `:gcc-15.2.0`, the main checkout's
  `.tmp/c1/gcc/`), in the builds the first records had: `g0` 41, `g2` 41, `o2` 41, `lto` 41, `win` 39;
- the nine C++ samples (`checkout:test/cpp/samples/NAME.cpp`): `g0`, `g2`, `lto` for all nine, `win` for five, `mac` for nine;
- Rust's `test/rust/sample/shapes.rs` as `rs.g0`, `rs.g2` and `rs.lt`;
- `stress1`-`stress10`.

The 21 corner modules are dropped. The four `hw-*` stay committed (`test/llvm/hw.list`).

**`bin/llvmi ir-make rt`** builds each line (`rt_compile`), as `modules` builds the tx lines:
- torture: `clang -O0 -g` | `-O2 -g` | `-O2` | `-O2 -flto` | `-O2 --target=x86_64-pc-windows-msvc`, gnu17, `-w -fno-strict-aliasing`,
  the tx flags;
- C++: `clang++` with the same five builds, c++23, where `mac` is `-O0 --target=x86_64-apple-macosx -nostdlibinc -nostdinc++` on the
  headers made without g++ (`cpp_full_sysroot`: the host has no macOS SDK, and the host's libstdc++ is not read);
- Rust: `rustc -C opt-level=0 -g` | `-C opt-level=2 -g` | `-C opt-level=2 -C lto=fat`, with `-C codegen-units=1 --emit=llvm-ir`
  (fat LTO writes one module; thin writes two);
- stress: `llvm-stress -size=60 -seed=N`.

Every build takes `-ffile-prefix-map=DIR/= -fdebug-compilation-dir=.` (`--remap-path-prefix` for rustc), so no checkout's path
reaches the IR: without the flag, `-g`'s `DIFile` directory is the working directory.

The verb then makes Arbace's mutants (`bin/repr-llvm mutants`, `arbace.repr.llvm.mutate`, the first records' kinds). Their input is
the pack's modules followed by `hw.list`, in that order. The sets, `rt_mutant_sets`:

| set | N | first index | family | input |
|---|---|---|---|---|
| a | 340 | 0 | base | modules |
| b | 340 | 1000 | base | modules |
| h1 | 600 | 500000 | all | modules |
| h2 | 600 | 600000 | all | modules |
| ctl | 1 | 900016 | base | hw |
| ctlh | 1 | 900050 | base | hw |
| ctlu | 1 | 900055 | base | hw |

`ctl` and `ctlh` are `:noterm` mutants (index 2 mod 17): `hw-dbg.noterm.900016` and `hw-notes.noterm.900050`. `ctlu` is `:undef`
(7 mod 17): `hw-implicit.undef.900055`. They are the verify controls' subjects and references, known by day whatever the pack holds.

The pack `test/llvm/ir/rt.tar.xz` holds `ID.ll` and `mut/NAME.KIND.K.ll`. Its manifest `rt.tsv` gives ID, SOURCE, SOURCE-SHA256 and
IR-SHA256; a mutant's SOURCE is `mutant-SET:BASE` and its SOURCE-SHA256 is the base's IR digest. Beside it are `rt.tar.xz.sha256`
and `rt.cmd`. The tar is deterministic, as tx's is. `ir-make rt --mutants` makes the mutants again from the pack's modules, with no
compiler. The mutants are packed rather than made at check time: they are judged against opt's record by digest, so a later change
to the reader or the printer would otherwise make the whole set stale.

The mutator's forms cache now holds at most four modules (it held them all). The mutants are unchanged: a subset made before and
after the change is the same pack byte for byte. The subset's peak fell from 1.49 to 0.67 GB; the whole pack's 261 modules would
not fit in 2 GiB.

**`bin/repr-llvm check`** has four new families, read from the pack (`rt_pack`: the pack is checked against its pin and extracted once
into `.tmp/repr-llvm/rt/PIN`; a checkout source whose digest is not the manifest's makes it stale, an error):

| family | modules |
|---|---|
| `roundtrip-rt` | the 257 modules |
| `text-rt` | the 257 modules |
| `verify-rt` | the 257 modules, `hw.list`, and sets a, b, ctl |
| `verify-rt-heldout` | sets h1, h2, ctlh, ctlu |

The records are `test/llvm/check-{roundtrip,text,verify,verify-heldout}-rt-linux-amd64.edn`. A first `--record` writes their header.
The families have no `--essential` subset. `hw-*` are left out of `roundtrip-rt` and `text-rt` because `roundtrip-llvmtest` and
`text-llvmtest` already count them. `bin/repr-llvm rt-lists DIR` writes the lists, with absolute paths, for the oracle tier's records.

The controls (`test/repr/llvm/*-controls.txt`) follow. A line whose names a list lacks is left, as before.

| control | crossing | expected |
|---|---|---|
| `_control/rt-g0-vs-o2` | `20000224-1.g0` against `.o2` | the forms differ |
| `_control/rt-stress1-vs-stress2` | `stress1` against `stress2` | the text differs |
| `_control/rt-noterm-vs-valid` | `hw-dbg.noterm.900016` against `20000224-1.g0`'s opt reference | the verdict differs |
| `_control/rt-noterm-vs-undef` | `hw-notes.noterm.900050` against `hw-implicit.undef.900055`'s | the message differs |

**The reader: one rule ported.** A held-out mutant of the subset (`rs.g2.attr-returned.500016`, `metadata returned !5236` in a call)
showed that `parseParameterList` was not followed. In LLParser a metadata argument takes no attributes, and its value is
parseMetadataAsValue's: a word that is not a type (an attribute, or `null`) gets parseValueAsMetadata's "expected metadata operand" at
that word. `arbace/repr/llvm/read.ae`'s `parse-args` now does the same. llvm-as 23.1.5 agrees on `metadata null` and on
`metadata noundef !{}`. Arbace took both before.

**Proved (light; stage 0, `REPR_LLVM_THREADS=2`):**

| check | result |
|---|---|
| `bin/llvmi rt-list` | 257 modules; run twice, the same file |
| `ir-make rt` on 17 ids (`LLVMI_RT_ONLY`: 20000224-1 in its five builds, pr119071.g0, auto in five, io.mac, io.g0, rs.g2, rs.lt, stress1, stress2), 20 mutants a set (`LLVMI_RT_MUT_N=20`) | 17 modules, 83 mutants; made twice from scratch, the same `rt.tsv` and `rt.tar.xz` byte for byte (`--mutants` again: the same); 1 min 30 s, 1.49 GB with the old cache, 0.67 GB with the bounded one |
| `check roundtrip-rt` on that pack | 17 of 17 forms equal, 17 of 17 prints taken by the verifier, `rt-g0-vs-o2` fails as it must; 40 s, 0.79 GB |
| `check text-rt` | 17 of 17 byte for byte, `rt-stress1-vs-stress2` fails as it must; 0.71 GB |
| `check verify-rt` (`REPR_LLVM_JUDGE=live`) | 62 of 62 verdicts, 41 of 41 messages and 62 of 62 whole stderr as opt 23.1.5's; `rt-noterm-vs-valid` fails as it must |
| `check verify-rt-heldout` (`REPR_LLVM_JUDGE=live`) | 41 of 42 before the reader's fix (the one above), 42 of 42 after; `rt-noterm-vs-undef` fails as it must |
| `rs.g0` alone (27,924 lines, the largest module) | round trip equal, the verifier's verdict the same; 83 s, 0.84 GB |
| unchanged by the reader's fix | `check roundtrip-llvmtest` 304/304, `verify-llvmtest` 792/806, `verify-probes` 472/478, `text-llvmtest` 304/304 (0 failing against the records); `bin/llvmi check --essential` 10 of 10, 4 controls fail; `test/repr/llvm/controls-test.bash` all ok |
| `bin/reap --record` | `test/reap/matrix.edn` gains the four sources as absent; `bin/reap`: 0 problems |

Without records, the subset's checks print "not recorded" for every module, as they must until the night records them.

**The cells.** `test/reap/cells.edn` names the four families as sources with `:until "LLS31 (a)'s night: ..."`. They print "not recorded
yet" without a problem. The three retired `:beside` sources stay until the records exist; an `:until` whose file exists is then a
problem, which forces the switch.

### ORS30 (a): LLVM IR's oracle tier (built here)

The fragment `test/gate/crops/llvm-oracle.edn`, spliced by hand into `test/gate/steps.edn`'s oracle tier (the crops entry `:llvm`, the
controls), has two steps on the hosts `[:stage0 :llvm :cxx]`:
- **`llvm-live`**: `bin/repr-llvm check {part}` with `REPR_LLVM_JUDGE=live` (clang -c's objects and llvm-as's bitcode, opt's verdict
  and stderr), with the parts `roundtrip-llvmtest` and `verify-probes`. Measured: roundtrip-llvmtest passes 304 of 304 (256 objects and
  304 bitcode equal), 1 min 41 s, 0.67 GB; verify-probes 472 of 478, as recorded. `verify-llvmtest` is not a part: its record is the
  tests' own verdicts, and live opt judges every module by opt's whole stderr, where 30 of its 792 passes differ.
- **`llvm-records-verify`**: `bin/repr-llvm records-verify --sample 25`, a new verb. It records every 25th entry of `ref-opt23.edn` and
  `ref-rt23.edn` again into scratch records and compares them entry for entry. Each entry is `equal`, `input`, `print`, `address` (a
  stderr that prints a value by its address), `moved` (another LLVM: reported, not failed, ORS6 (a)) or `differs` (a failure).
  `:stderr-bytes` holds the raw path and is not compared. The verb also checks a crossed pair, which must differ (ORS7 (a)). Measured
  on LLVM 23.1.5 against the 23.1.4 records: 52 opt entries (50 equal, 2 address) and 13 rt entries equal, 21 s, 0.5 GB.

`gate.crops check` finds 0 problems, `test/gate/crops-test.bash` and `costs-test.bash` pass, and `bin/gate --oracle --plan` plans the
three parts. The rt families join `llvm-live` as parts once their records exist.

### The night's commands (LLS31 (a))

Light (each process under 1 GB, measured on the subset), no heavy lock:

    bin/llvmi ir-make rt                                   # clang 23.1.5, clang++, rustc 1.99.1, llvm-stress; then the mutants (Arbace):
                                                           # about 257 builds and 2,483 mutant indices; expect 257 modules and about 2,300 mutants
    git add test/llvm/ir/rt.*                              # the pack, its pin, manifest and command
    bin/repr-llvm rt-lists .tmp/llvm/rt-lists
    bin/repr-llvm record-opt .tmp/llvm/rt-lists/verify-rt.list .tmp/llvm/rt-lists/verify-rt-heldout.list   # opt's records, test/llvm/ref-opt23.edn
    REPR_LLVM_THREADS=2 bin/repr-llvm record-rt .tmp/llvm/rt-lists/roundtrip-rt.list .tmp/llvm/rt-rec    # clang's and llvm-as's judges, ref-rt23.edn
    for f in roundtrip-rt text-rt verify-rt verify-rt-heldout; do REPR_LLVM_THREADS=2 bin/repr-llvm check $f --record; done

Then, by day:
- in `test/reap/cells.edn`, remove the four `:until` keys and the three retired `:beside` sources, and delete their records
  (`check-roundtrip`, `check-text`, `check-verify` and `check-verify-heldout-linux-amd64.edn`; `rt-builds.tsv` stays the source);
- run `bin/reap --record`;
- recount `test/crops/independence.edn`'s read and judge for LLVM IR;
- add the parts `roundtrip-rt`, `verify-rt` and `verify-rt-heldout` to `llvm-read`, and `text-rt` to `llvm-print`;
- add `roundtrip-rt`, `verify-rt` and `verify-rt-heldout` to `llvm-live`.

**Expected.** Read:

| family | expected |
|---|---|
| roundtrip-rt | 257 of 257 |
| verify-rt | 261 modules about 261 of 261, the fitted mutants about 99 % (the old 905 of 912) |
| verify-rt-heldout | about 96 % (the old 1,040 of 1,082) |

So about 1,568 + 2,500 cases at about 98 %. Print: 304 + 257. Completeness stays about 96 (the old families were at these rates).
Maturity stays 100: the four families have controls. Independence: read 1, and the judge gains the rt families' opt records at 0.5
(about 2,600 of them): (2,068 + 0.5 x (948 + 2,600)) / (3,016 + 2,600), about 0.68 by the case-weighted rule, so independence goes
from 96.9 to about 93.6. That is LLS33 below.

### Proposed (llvm-rt-pack)

- **LLS33 The rt families' judge.** The mutants and the pack's modules have no own verdict: opt's record (0.5) judges them, and their
  number moves the judge from 0.843 to about 0.68.
  - (a) Count them as LLS31 (b) did: beside, at the read level only. Cost: none. The read cell is unchanged.
  - (b) Give each mutant an own verdict. The mutator knows the rule it broke, so each kind could write its expected verdict (invalid)
    and LLParser's or the Verifier's message family. The verdict is then Arbace's own (1), and opt only checks the message (the oracle
    tier). Cost: a day, a table of 41 kinds to messages (the Verifier's text, ported).
  - (c) Accept the drop.
  - *Recommended: (b)*, with (c) meanwhile. The mutants test LLVM's rules, and their expected rejection is known by construction.
- **LLS34 The pack's C++ for win.** Four of the nine C++ samples have no `win` build (no MSVC headers), as in the first records.
  - (a) Build them against the headers made without g++, as `mac` is. Cost: minutes; the IR is then a GNU STL for an MSVC triple, an
    odd but valid input.
  - (b) Keep the first records' set.
  - *Recommended: (b)*.

## Decided (the user, 2026-11-08): LLS33 (b), LLS34 (b)

- **LLS33 (b):** each rt-family mutant gets its own verdict from the mutator's rule. The mutator knows the rule it broke, so each of
  its 41 kinds writes the expected verdict and the message family of LLParser or the Verifier. The texts are ported from the pinned
  llvm-project-23.1.5 tarball. The verdict is then Arbace's own (1); opt's record only checks the message, at the oracle tier.
- **LLS34 (b):** the pack keeps the first records' set: no `win` build for the four C++ samples without MSVC headers.

## As built: LLS33 (b), the mutants' own verdicts (2026-11-08, branch `llvm-lls33`)

Built on next `5ec43f7b7` (llvm-rt-pack merged). The pack itself is still the night's (LLS31 (a)): this changes what the night records,
not what is recorded now.

**The verdict line.** `bin/repr-llvm mutants` (`arbace.repr.llvm.mutate`) now writes each mutant's own verdict as its first line:

    ; MUTANT: KIND VERDICT LAYER: MESSAGE      (VERDICT invalid or debug)
    ; MUTANT: KIND valid
    ; MUTANT: KIND unknown: WHY

`debug` is a module whose only fault is its debug info, which opt strips with a warning (exit 0). `valid` is a change LLVM's rules
accept, such as an attribute on an intrinsic's declaration, which AutoUpgrade replaces. `unknown` means the kind cannot tell; such a
mutant is judged by opt's record, as before. The rest of the mutant is unchanged, byte for byte: the expectation draws no random number.
MESSAGE is a fragment of the first message, as llvm/test's CHECK lines declare one. Where LLVM names the mutant's own values (an LLParser
naming error), the message is whole.

**Who computes it.** Each kind attaches what it changed to the mutant's forms as metadata (the function, block and item, the attribute
and its site, the bundle set, the field). `arbace.repr.llvm.expect` then derives the expectation from the rule:
- LLParser's naming is ported as `parse-sim`: `PerFunctionState`'s `getVal`, `setInstName`, `defineBB`, `checkValueID`,
  `checkValidVariableType` and `finishFunction` over the mutant's text order. So the kinds that move or rename a value know the
  parser's first error from the mutant's own text, including the renumbering kinds' own slips (an `entrybr` that leaves a stale
  `%0` gets `'%0' is not a basic block`).
- The Verifier's checks are taken in its visiting order: `visitFunction` before the blocks, `visitBasicBlock`'s phi checks before the
  instructions, each instruction's own visit before `visitInstruction`. Also its dominance rules: no dominance in an unreachable block,
  none of a value wrapped in metadata.
- For attributes, `verifyParameterAttrs` and `verifyFunctionAttrs` are ported check by check: the exclusive groups, the nine pairs,
  `typeIncompatible` (Attributes.cpp:2483), nest, returned, sret, the swift attributes and inalloca. So are the function attributes'
  pairs and values (allocsize, allockind, vscale_range, frame-pointer, the unsigned ones, sign-return-address,
  branch-target-enforcement), and LLParser's placement and value checks (Attributes.td's `FnAttr`, `ParamAttr` and `RetAttr`). Two
  more rules come from LLParser: a function's attribute groups replace a written attribute of the same kind, and a call of an old
  debug intrinsic becomes a debug record.

The module calls none of `arbace.repr.llvm.verify`'s checks, only its structural helpers (an instruction's operands and result type).
Each message has its source line (`msgs`, P: LLParser.cpp, V: Verifier.cpp of 23.1.5).

**The judge.** `own_verdicts` reads the line. `verify-check` then holds the mutant to it:
- `valid` is met by Arbace's valid alone. An llvm/test file's `valid` still accepts `debug`.
- `invalid` and `debug` are met by the same verdict, with MESSAGE found in verify's output.

The negative control `rt-noterm-vs-undef` now crosses two own verdicts: the `:undef` mutant's message, `use of undefined value
'%nosuch.value'`, is not in the `:noterm` mutant's output. `bin/repr-llvm mutants-oracle LIST WORK` is the oracle tier's check of the
lines. Against opt's record (`:first`) or live opt's whole stderr (`REPR_LLVM_JUDGE=live`), it compares the verdict and the message per
mutant and sums the results by kind.

**A verifier bug found.** The first comparison found `arbace.repr.llvm.verify` taking `metadata i32 %nosuch.value` (an `:undef` mutant
of a `llvm.dbg.value` call) as valid. LLParser resolves a local wrapped in metadata as any operand (parseValueAsMetadata,
LLParser.cpp:3352, 6750): a forward reference, so the mutant gets `use of undefined value`. `use-value` now resolves locals wrapped in
metadata, in calls' metadata arguments, debug records and DIArgList. verify-llvmtest (792 of 806), verify-probes (472 of 478),
roundtrip-llvmtest and text-llvmtest (304 of 304) are as recorded, as is `bin/llvmi check --essential` (10 of 10, 4 controls fail);
`test/repr/llvm/controls-test.bash` all ok.

**Proved (light; stage 0, `REPR_LLVM_THREADS=2`, the stubs first on PATH, no LLVM tool called).** The day's inputs are 41 modules:
- 21 of the tx pack's (O0 and O2);
- the four `hw-*`;
- 16 llvm/test files with landing pads, funclets or debug info.

An llvm/test file whose own verdict is `debug` was left out: the kinds assume a valid base, as the pack's modules are. The mutants were
made at the pack's set indices: a (340 from 0) and b (340 from 1000) of the base family, and 600 from 500000 and 600 from 600000 of
every kind; `bin/repr-llvm mutants LIST OUT N FIRST [base]`. Each set took about 1 min and 0.56 GB.

| check | result |
|---|---|
| own verdicts | base family 676 of 680 (99.4%), every kind 1,020 of 1,192 (85.6%) |
| `verify-check` in own mode (1,872 mutants; 2 min, 1.1 GB) | all 1,696 own verdicts met by Arbace's verifier, 1,567 of 1,567 rejections with the declared message |
| `REPR_LLVM_JUDGE=live mutants-oracle` (the oracle tier, by hand: opt 23.1.5) | 1,696 of 1,696 own verdicts as opt's, verdict and message (opt's whole stderr); 176 unknown, judged by opt as before (about 6 min, 0.5 GB) |
| the families unchanged by the verifier's fix | verify-llvmtest 792 of 806, verify-probes 472 of 478, roundtrip-llvmtest and text-llvmtest 304 of 304, 0 failing against the records; `bin/llvmi check --essential` 10 of 10 (4 controls fail); `test/repr/llvm/controls-test.bash` all ok |

Two earlier passes found what the kinds now say. First, the `:undef` metadata case above: Arbace's verifier was wrong. Second, a
DILocation that only an old `llvm.dbg.declare` call uses. That call is a debug record by the time the Verifier runs, so opt says
`location requires a valid scope` (V:1076), not V:3335's message. Arbace's verifier had the same slip: its `*inst-locs*` now skips those
calls and functions without a subprogram (V:3313).

**The 41 kinds.** Each kind's verdict, as written, with its day numbers (1,872 mutants; "as opt" is the oracle run's):

| kind | family | the verdict the kind writes | day's mutants | own | as Arbace's verifier | as opt (live) |
|---|---|---|---|---|---|---|
| `type` | base | LLParser's naming over the new type (`'%x' defined with type 'A' but expected 'B'`, a forward reference's type), then the instruction's own: ret's result type (P:7927), select's operands, a cast's opcode (P:8572), a call's signature against an intrinsic's (V:3850); a load, store or binop no use sees: valid | 68 | 68 | 68 | 68 |
| `hoist` | base | parse-sim (numbering, P:3598); a real operand defined among the items jumped over: `Instruction does not dominate all uses!` (V:5211); only metadata uses, or an unreachable block: valid | 68 | 68 | 68 | 68 |
| `noterm` | base | `expected instruction opcode` (P:7626) before a label or `}`; an unlabelled next block read as the same block, the text parse-simmed again; a function's only block emptied: `function body requires at least one basic block` (P:7373) | 68 | 67 | 67 | 67 |
| `phiblock` | base | `use of undefined value '%nosuch.bb'` (P:3969); another block: `PHI node entries do not match predecessors!` (V:3409), `PHI node ` when that block has another entry (V:3402 or V:3409 by the sorted order) | 68 | 68 | 68 | 68 |
| `phimove` | base | parse-sim; the item now above the phi uses it: dominance, else `PHI nodes not grouped at top of basic block!` (V:3804) | 68 | 68 | 68 | 68 |
| `align` | base | `alignment is not a power of two` (P:2525) | 69 | 69 | 69 | 69 |
| `linkage` | base | `invalid linkage for function declaration` (P:7099); a global: `symbol with local linkage must have default visibility` (P:1432), the printer's last :linkage and :visibility the ones read | 70 | 70 | 70 | 70 |
| `undef` | base | `use of undefined value '%nosuch.value'` (P:3969), also inside metadata | 70 | 70 | 70 | 70 |
| `dupdef` | base | parse-sim: `multiple definition of local value named 'x'` (P:4113) or the numbering (P:3598) | 70 | 70 | 70 | 70 |
| `lpad` | base | parse-sim; an invoke of an earlier block unwinding there: `The unwind destination does not have an exception handling instruction!` (V:4253); else dominance or `LandingPadInst not the first non-PHI instruction in the block.` (V:4885) | 70 | 70 | 70 | 70 |
| `intrinsic` | base | `Intrinsic called with incompatible signature` (V:3850); undeclared or variadic: unknown | 70 | 70 | 70 | 70 |
| `hoistr` | base | as hoist, renumbered: dominance | 70 | 69 | 69 | 69 |
| `phimover` | base | as phimove, renumbered | 70 | 70 | 70 | 70 |
| `lpadr` | base | as lpad, renumbered | 70 | 70 | 70 | 70 |
| `entrybr` | base | parse-sim (a stale number: `'%0' is not a basic block`, P:1859), else `Entry block to function must not have predecessors!` (V:3222) | 70 | 70 | 70 | 70 |
| `phicount` | base | `PHINode should have one entry for each predecessor of its parent basic block!` (V:3383) | 70 | 70 | 70 | 70 |
| `selfref` | base | parse-sim (`instruction forward referenced with type 'T'`, P:4100), else `Only PHI nodes may reference their own value!` (V:5670) in a reachable block; a block with EH pads: unknown | 70 | 68 | 68 | 68 |
| `attr-type` | attrs | LLParser's placement; then verifyParameterAttrs (V:2064): the groups, the pairs, `' applied to incompatible type!` (typeIncompatible); a metadata argument: `expected metadata operand` (P:6797); an intrinsic's declaration: valid (AutoUpgrade.cpp:2001) | 30 | 30 | 30 | 30 |
| `attr-pair` | attrs | the pair's check in V:2064 or V:2241 order (`Attributes 'zeroext and signext' are incompatible!`, `Attribute 'optnone' requires 'noinline'!`, ...); P:7193 for sret on a first parameter | 30 | 29 | 29 | 29 |
| `attr-place` | attrs | `this attribute does not apply to return values|parameters|functions` (P:2116, P:2118, P:1832) | 30 | 30 | 30 | 30 |
| `attr-dup` | attrs | V:2241's per-parameter order: `More than one parameter has attribute nest!`, returned's compatibility, `Cannot have multiple 'sret'|'swiftself'|'swifterror'|'swiftasync' parameters!`; swifterror's uses: unknown | 30 | 30 | 30 | 30 |
| `attr-returned` | attrs | `Incompatible argument and return types for 'returned' attribute` (V:2241, also a varargs call's extra arguments V:3985) | 30 | 30 | 30 | 30 |
| `attr-value` | attrs | LLParser's values (P:2616, P:2659, P:2929, initializes' range list); V:2241's (vscale_range, allocsize, allockind, frame-pointer, the unsigned ones, sign-return-address, branch-target-enforcement); a group's attribute of the same key wins (P:240) | 30 | 28 | 28 | 28 |
| `attr-immarg` | attrs | `immarg attribute only applies to intrinsics` (V:2241); a call of an intrinsic: `immarg may not apply only to call sites` (V:3929) | 30 | 19 | 19 | 19 |
| `cc-def` | cc | visitFunction's switch (V:3076-3135): void return, sret, byval, preallocated, inalloca, varargs, x86_intrcc's byval; fastcc without varargs: valid; an old debug intrinsic's declaration: valid | 30 | 30 | 30 | 30 |
| `cc-call` | cc | `calling convention does not permit calls` (V:3854, CallingConv.h:308) | 30 | 30 | 30 | 30 |
| `musttail` | musttail | P:3364 in a varargs caller; verifyMustTailCall's order (V:4165-4236); ABI attributes to compare: unknown | 30 | 30 | 30 | 30 |
| `bundle` | bundles | visitCallBase's bundle loop (V:4030-4103), `Direct call cannot have a ptrauth bundle`, the convergencectrl token; an unknown tag: valid | 30 | 30 | 30 | 30 |
| `intr-decl` | intrinsics | unknown: AutoUpgrade (upgradeIntrinsicFunction1) may rewrite the declaration | 30 | 0 | 0 | 0 |
| `intr-arg` | intrinsics | `immarg operand has non-immediate parameter` (V:3936), lifetime's pointer, `expected metadata operand`; an intrinsic visitIntrinsicCall checks: unknown; else valid | 30 | 30 | 30 | 30 |
| `intr-rename` | intrinsics | unknown, as intr-decl | 30 | 0 | 0 | 0 |
| `funclet` | funclets | the personality dropped: as personality; the other changes: unknown | 21 | 9 | 9 | 9 |
| `personality` | funclets | the first EH instruction's `... needs to be in a function with a personality.` (V:4880, 4904, 4922, 4957, 5137) | 28 | 28 | 28 | 28 |
| `unwind-dest` | funclets | `The unwind destination does not have an exception handling instruction!` (V:4253) | 28 | 28 | 28 | 28 |
| `md-attach` | metadata | unknown: the attachment rules not ported | 28 | 0 | 0 | 0 |
| `md-edit` | metadata | `incorrect number of operands in module flag` (V:1915), `... in llvm.ident metadata` (V:1793); other nodes: unknown | 28 | 18 | 18 | 18 |
| `di-drop` | debug | `missing required field 'F'` (P:5755, the REQUIRED fields); an optional field: unknown | 28 | 11 | 11 | 11 |
| `di-retarget` | debug | a DILocation's scope: debug `DILocation's scope must be a DILocalScope` (V:3335) for an instruction's, `location requires a valid scope` (V:1076) for a record's; other fields: unknown | 28 | 8 | 8 | 8 |
| `di-record` | debug | debug `invalid #dbg record variable` (V:7168), `invalid #dbg record expression` (V:7172); `invalid debug location` (P:347) | 28 | 22 | 22 | 22 |
| `di-attach` | debug | a function's: debug `function !dbg attachment must be a subprogram` (V:3244); an instruction's, a global's: unknown | 28 | 23 | 23 | 23 |
| `di-version` | debug | debug `ignoring debug info with an invalid version (N)` (AutoUpgrade.cpp:6052, DiagnosticInfo.cpp:121) | 28 | 28 | 28 | 28 |
| all | | | 1872 | 1696 | 1696 | 1696 |

The unknowns, 176 of 1,872, are the kinds or cases whose rule is not ported:
- `md-attach` (28): visitInstruction's and visitMDNode's metadata rules;
- `intr-decl` and `intr-rename` (30 each): AutoUpgrade's `upgradeIntrinsicFunction1` may rewrite a declaration whose signature it does
  not take, as for `llvm.ctlz.i32` with one argument;
- `di-drop` and `di-retarget`: an optional field, or a field other than a DILocation's scope;
- funclet operand changes (12), md-edit of an attachment's node (10), `attr-immarg` on an immediate or an aggregate (11), a few
  debug record and attachment cases, two self references in EH blocks.

They stay on opt's record (0.5).

**Independence.** The checkout's facts are unchanged: the rt families are not recorded, and `bin/crops-score --crop llvm` before and
after gives LLVM IR independence 96.9 (judge 0.843). The projection on the night's records (`test/crops/independence.edn`'s comment):

| | judge | independence |
|---|---|---|
| without LLS33 | (2582 + 0.5 x 3092) / 5674 = 0.728 | 94.6 |
| with it | (4288 + 0.5 x 1386) / 5674 = 0.878 | 97.6 |

The night's counts replace both.

### The night's commands (LLS33 (b), lane LA after LLS31 (a)'s)

LLS31 (a)'s commands are unchanged, but `ir-make rt` must run on a tree with this branch (next after its merge): its mutator writes the
verdict lines into the pack. If LLS31 (a)'s night runs first, `bin/llvmi ir-make rt --mutants` makes the mutants again without a
compiler (light, about 10 min), and `record-opt` then records them afresh (the lines change their digests). After the records:

    bin/repr-llvm rt-lists .tmp/llvm/rt-lists
    REPR_LLVM_JUDGE=record bin/repr-llvm mutants-oracle .tmp/llvm/rt-lists/verify-rt.list .tmp/llvm/mo-rt                  # the oracle tier: each own verdict against opt's :first
    REPR_LLVM_JUDGE=record bin/repr-llvm mutants-oracle .tmp/llvm/rt-lists/verify-rt-heldout.list .tmp/llvm/mo-rt-heldout
    REPR_LLVM_JUDGE=live bin/repr-llvm mutants-oracle .tmp/llvm/rt-lists/verify-rt-heldout.list .tmp/llvm/mo-live        # opt's whole stderr (light, ~3 min)

A difference in `mutants-oracle` is a rule to fix in `arbace.repr.llvm.expect` (by day), not a record to take. The two `check verify-rt*
--record` runs then hold each mutant to its own verdict. A row that fails there is Arbace's verifier disagreeing with the rule: a
verifier bug to fix, or a class. Then by day: the judge recount for `test/crops/independence.edn`: the mutants whose line is not
`unknown` (`grep -L 'unknown:'` over the pack's `mut/`) count 1, the rest 0.5.

### Proposed (llvm-lls33)

- **LLS35 `mutants-oracle` in the oracle tier.**
  - (a) A part of `llvm-records-verify` (`test/gate/crops/llvm-oracle.edn`) over the pack's two mutant lists against the record, once
    the pack is recorded. Cost: a fragment edit, seconds a run.
  - (b) By hand at tags.
  - *Recommended: (a).*
- **LLS36 The unknown kinds.**
  - (a) Port what they need:
    - for `intr-decl` and `intr-rename`, `upgradeIntrinsicFunction1`'s name rules and `isSignatureValid`'s messages (`intr.ae` has
      the matcher);
    - for `md-attach`, visitInstruction's attachment checks (!range, !prof, !nonnull, !align, !dereferenceable, !noundef,
      !invariant.load, !fpmath, !tbaa);
    - for `di-drop` and `di-retarget`, the visitDI* field rules.

    Cost: about a day. It would take the held-out sets from 85.6% to about 97%, and the judge to about 0.90.
  - (b) Leave them on opt's record (0.5).
  - *Recommended: (a)*, `md-attach` and `intr-*` first (88 of the 176).
- **LLS37 A mutant of an invalid base.**
  - (a) At check time, a mutant whose base module does not pass its own row is judged by opt's record, its verdict line ignored.
    Cost: an awk join in `verify-check`.
  - (b) Nothing: the pack's bases are clang's output, and verify-rt's module rows show them valid.
  - *Recommended: (b)* while only the pack is mutated; (a) if another corpus is.

## Decided (the user, 2026-11-09): LLS35 (a), LLS36 (a), LLS37 (b)

- **LLS35 (a):** `mutants-oracle` is a part of the oracle tier's `llvm-records-verify`, over the pack's two mutant lists against opt's
  record.
- **LLS36 (a):** the rules the unknown kinds need are ported, `md-attach` and `intr-*` first, then the debug info's field rules.
- **LLS37 (b):** nothing: the pack's bases are clang's output, valid by verify-rt's module rows.

## As built: LLS35 (a), LLS36 (a) (2026-11-09, branch `llvm-props`)

Built on `mac/llvm-lls33` `60026c61b`. The pack rt is still the night's (LLS31 (a)); this changes the verdict lines the night's
`ir-make rt` writes and the oracle tier's check of them.

**LLS35 (a).** `bin/repr-llvm records-verify` ends with the mutants' part: when `test/llvm/ir/rt.tar.xz` is there, it runs
`mutants-oracle` (`REPR_LLVM_JUDGE=record`) over verify-rt's and verify-rt-heldout's lists, whole, and prints
`records-verify mutants FAMILY: N own verdicts, N as opt's (record), 0 differ; U unknown`. A difference fails the step. Before the pack
exists it says so and skips. The step's command line is unchanged; only its comment and waiver are new, in
`test/gate/crops/llvm-oracle.edn` and `test/gate/steps.edn`. That keeps `.tmp/night10/slots/llvm-cells.patch` applying as made
(`git apply --check`: clean), so the slot need not be regenerated. Its `llvm-live` parts are lines this branch leaves alone.

**LLS36 (a): the rules.** `arbace.repr.llvm.expect` gains these rules, ported from llvm-project 23.1.5 (the pinned tarball;
Apache-2.0 with LLVM exceptions, as LLS33 (b)'s):
- **`intr-decl`, `intr-rename`.** LLParser's `validateEndOfModule` upgrades every function first (`UpgradeCallsToIntrinsic`,
  AutoUpgrade.cpp:1995). The port covers:
  - `upgradeIntrinsicFunction1`'s name rules (AutoUpgrade.cpp:1341-1950), by the name's first letter as the switch has them. A
    `ctlz.`/`cttz.` of one parameter is declared again and its calls rebuilt (valid). The rules that rewrite calls from their own
    arguments (memcpy/memset with alignment, lifetime of two, masked of four, objectsize, the annotations, the targets) are named and
    left unknown.
  - `remangleIntrinsicFunction`: a valid signature under another mangling is declared again under it. Its calls are retargeted, or
    rewritten by UpgradeIntrinsicCall's own case for the IDs that have one (left unknown).
  - Then the Verifier in the module's function order (Verifier.cpp:7671). The first caller defined before the declaration says
    `Intrinsic called with incompatible signature` (V:3846) for a changed declaration. For a renamed one, visitIntrinsicCall's case
    for its calls applies (V:5952: ptrmask's four checks; is.fpclass's mask when constant; memset none). Then the declaration's
    visitFunction: isSignatureValid's message (V:3297), through `intr.ae`'s matcher.
- **`md-attach`.** visitInstruction's attachment checks in its order (V:5779-5913): fpmath, range (verifyRangeLikeMetadata, V:4469,
  without wrapping ranges), noalias.addrspace, invariant.group and invariant.load, nonnull, noundef, dereferenceable(_or_null)
  (V:5214), tbaa's first check, the scope lists (V:5518, V:5494), align, and `!prof` (visitProfMetadata, V:5241, with CondBrInst's
  successors). A call of an old debug intrinsic becomes a debug record, its attachments dropped: valid.
- **`di-drop`, `di-retarget`.** The visitDI* field rules (V:1074-1723):
  - `line specified with no file` (V:1095, V:1514);
  - a definition's `subprogram definitions must have a compile unit` (V:1577);
  - `array types must have a base type` (V:1394);
  - `invalid scope`, `invalid file` (visitDIType, visitDIScope, visitDIVariable);
  - DISubprogram's type, unit, containingType, declaration and retainedNodes; the variables' `invalid type ref` and `invalid type`;
    the subroutine and composite types' elements, base type and vtable holder.

  An optional field whose absence no check takes is valid (`di-drop-valid`, by node). A debug verdict on a node that StripDebugInfo
  leaves reachable is made invalid: opt verifies it again with debug errors hard (`stripped-reachable`, DebugInfo.cpp's leftovers:
  llvm.dbg.* named metadata, `!dbg` attachments, records and the old debug intrinsics' calls removed).
- **`di-attach` on a declaration.** The call site's `!dbg` must not be distinct (V:3202: `function declaration may only have a unique
  !dbg attachment`); it is not checked as a subprogram. LLS33 (b)'s rule said `function !dbg attachment must be a subprogram` there:
  wrong, and opt showed it on the first held-out set.

`mutate.ae` attaches the change to the three kinds' mutants as metadata (`ctx`). No random number is drawn, so the mutants are
unchanged byte for byte but for their first line: the 900 held-out mutants of llvm-lls33's mutator against this branch's, 0 bodies
differ.

**Arbace's verifier fixed (four rules the comparisons found):**
- a local variable's scope that is no DIScope is `invalid scope` (V:1692) before visitDILocalVariable's local scope;
- an old debug intrinsic's call's metadata arguments are reachable as a record's, so their nodes are verified (its variable's `file`
  sent to itself was missed);
- such a call's attachments but `!dbg` are not checked (the call is a record by then);
- a declaration's distinct `!dbg` of any kind is `function declaration may only have a unique !dbg attachment` (it took only
  subprograms with DISPFlagDefinition).

**Proved (light; stage 0, `REPR_LLVM_THREADS=2`, the stubs first on PATH; live opt 23.1.5 only for the oracle, by hand).** The day's
41 modules as LLS33 (b), at its set indices (fa, fb, f1, f2: 1,872 mutants). These were fitted: their opt answers were read while
writing the rules. Two held-out sets of the three families the rules cover (intrinsics, metadata, debug; 300 each): h1 at 700000,
read once to fix three rules, then h2 at 800000, not looked at.

| set | own verdicts before (LLS33 (b)) | after | as Arbace's verifier | as opt (live) |
|---|---|---|---|---|
| fitted, 1,872 (fa, fb, f1, f2) | 1,694 (178 unknown) | 1,818 (54 unknown), 97.1% | 1,818 of 1,818 | 1,818 of 1,818 |
| of which every kind (f1, f2), 1,192 | 1,018 (85.4%) | 1,142 (95.8%) | | |
| h1, 900 (700000) | (not measured; this branch's first rules: 820, 10 of them wrong against opt) | 819 (81 unknown) | 819 of 819 | 819 of 819 |
| h2, 900 (800000), held out | 458 (50.9%) | 815 (90.6%) | 815 of 815 | 815 of 815 |

h2 by kind, after (before): intr-decl 68 of 75 (0), intr-rename 70 (0), md-attach 149 of 150 (0), di-drop 58 of 60 (21), di-retarget
49 of 60 (16); di-attach 52, di-record 55, di-version 60, intr-arg 68, intrinsic 75, md-edit 111 of 150, as before. On h1 the
first rules had 10 own verdicts opt rejected, which led to the fixes above (so 820 to 819 is not a loss):
- 5 `!prof` on an old debug intrinsic's call;
- 2 DI nodes kept by StripDebugInfo (BPF's preserve-access-index);
- 2 declarations' `!dbg`;
- an array type's base type.

The unknowns left (h2's 85): md-edit's replaced operands of module flags (36) and llvm.ident (3); intr-arg on intrinsics
visitIntrinsicCall checks (7); is.fpclass renamed over a non-constant mask (4: the Verifier's cast is undefined there); the AutoUpgrade
rewrites that use the calls' own arguments (4); a declaration visited before its calls with attributes on a changed parameter (3);
instructions' and globals' `!dbg` (5); a subprogram attached to another function (3); variables sent to other variables (3); a local
variable's scope sent to another scope (4); a DILocation no instruction uses (3); and a few more of one or two each.

**The families the verifier's fixes touch**, against their records: verify-llvmtest, verify-probes, roundtrip-llvmtest and text-llvmtest
792 of 806, 472 of 478, 304 of 304 and 304 of 304 as recorded (0 failing, 0 fixed, their controls failing); `bin/llvmi check --essential` 10 of 10 (4 controls fail); `test/repr/llvm/controls-test.bash` all ok. The stubs' log is empty.

**Independence.** The checkout's facts are unchanged, since the rt families are not recorded yet. `bin/crops-score` gives LLVM IR the
same scores before and after. The projection on the night's records (`test/crops/independence.edn`'s comment), with verify-rt-heldout's
~1,202 mutants at h2's every-kind rate rather than 85.6%: about 1,152 own and 50 on opt's record, so the judge goes from about 0.878 to
(4411 + 0.5 x 1263) / 5674 = 0.889 and independence from about 97.6 to about 97.8. The night's counts replace both.

### The night's commands (LLS35 (a), LLS36 (a), after LLS33 (b)'s)

LLS33 (b)'s are unchanged. `bin/llvmi ir-make rt` (or `ir-make rt --mutants` if the pack was made before this merge) must run on a tree
with this branch: the mutator's verdict lines change, so the pack's digests and `record-opt`'s entries are made afresh. Then, on the
oracle tier (by hand, opt 23.1.5):

    bin/gate --oracle --plan                                                    # llvm-records-verify planned, its mutants' part included
    bin/repr-llvm records-verify --sample 25                                    # the records' sample, then "records-verify mutants verify-rt / verify-rt-heldout"
    REPR_LLVM_JUDGE=live bin/repr-llvm mutants-oracle .tmp/llvm/rt-lists/verify-rt-heldout.list .tmp/llvm/mo-live   # live opt's whole stderr (~3 min)

Expected: 0 differ; the unknowns are those of the classes above, about 10% of the held-out list. A difference is a rule to fix in
`arbace.repr.llvm.expect` by day.

### Proposed (llvm-props)

- **LLS38 The module flags' operands (md-edit, 36 of h2's 85 unknowns).** visitModuleFlag's checks of a flag's behaviour, key and value
  (V:1905-1990: `invalid behavior operand in module flag (expected constant integer)`, the `require` pairs, the known keys' value
  types).
  - (a) Port them. Cost: half a day. h2 to about 94.6%.
  - (b) Leave them on opt's record.
  - *Recommended: (a).*
- **LLS39 visitIntrinsicCall's checked intrinsics (intr-arg 7, intr-rename on is.fpclass 4).** Each case of the switch of the mutated
  intrinsics that the corpus declares: memcpy's and memset's immarg flags, lifetime's, the vector reductions'.
  - (a) Port the cases of the intrinsics the pack declares (`bin/repr-llvm` lists them from rt.tsv). Cost: half a day.
  - (b) Leave them.
  - *Recommended: (a) after LLS38*, as the pack's records name the intrinsics.

## Decided (the user, 2026-11-09): LLS38 (a), LLS39 (a)

- **LLS38 (a):** visitModuleFlag's checks of a flag's behaviour, key and value are ported (md-edit).
- **LLS39 (a):** the rest of visitIntrinsicCall's cases are ported, each rule for rule (intr-arg, intr-rename on is.fpclass, the
  other checked intrinsics of the switch).

## As built: LLS38, LLS39 (2026-11-09, branch `llvm-props2`)

Built on next `e6214cd13`. As LLS36 (a), this changes the verdict lines the night's `ir-make rt` writes (the mutants' bodies are
unchanged byte for byte: h3's 900 against the base's mutator, 0 differ), not what is recorded now. Ported from llvm-project 23.1.5
(the pinned tarball; Apache-2.0 with LLVM exceptions, LICENSE.md, doc/ARBACE.md), into `arbace.repr.llvm.expect`.

**LLS38: the module flags and llvm.ident** (`module-md-verdict`, used by `ex-md-edit` for a flag's or an ident's node):
- `Verifier::visitModuleFlags` (V:1831) and `visitModuleFlag` (V:1913), check for check: the three operands; the behaviour by
  `Module::isValidModFlagBehavior` (Module.cpp:329: a ConstantInt whose `getLimitedValue` is 1 to 8, Module.h:120), `expected
  constant integer` before `unexpected constant`; the ID a string; the behaviours' values (Min: a non-negative ConstantInt by the
  sign bit of its own width, so `i1 true` is negative; Max; Require: a pair whose first is a string; Append and AppendUnique: a
  node); the IDs unique but Require's; `wchar_size`, `SemanticInterposition`, `Linker Options`; then the loop's pauth names'
  integer values, `ptrauth-init-fini`'s rules, the pauthabi pair, and the requirements (`flag is not present`, `flag does not have
  the required value`). `CG Profile`'s entries and the AMDGPU flags are left to opt.
- `visitModuleIdents` (V:1784): one operand, a string.
- LLParser's end (P:475-481) in its order: `UpgradeDebugInfo` (AutoUpgrade.cpp:6052) reads the version as
  `getModuleFlagNameSafely` does; version 3: the flags verified as written, a failure final; else `StripDebugInfo`
  (DebugInfo.cpp:709; `strip-modifies?` ports whether it changes the module) and the version warning (`debug`); then
  `UpgradeModuleFlags` (AutoUpgrade.cpp:6489: PIC Level's Error and Max to Min, PIE Level's Error to Max,
  branch-target-enforcement's and `sign-return-address*`'s Error to Min; the Objective-C and AMDGPU rewrites left to opt); then
  opt's verifier over the upgraded flags.

**LLS39: visitCallBase's immarg check and visitIntrinsicCall's switch** (`call-site-intrinsic`, used by `ex-intr-arg` and, for the
renamed calls, `ex-intr-rename`):
- the immarg check (V:3932): a ConstantInt or ConstantFP of no vector type, then the range attribute; the callee's attributes as
  `UpgradeIntrinsicFunction` leaves them (AutoUpgrade.cpp:2001: `Intrinsic::getAttributes` when the signature is valid, from the
  table's immarg indices and ranges; else the declaration as written);
- visitIntrinsicCall's switch (V:5952-7055), every case named: ported are assume (its i1 true with bundles), ucmp/scmp, coro.begin,
  is.fpclass (its mask, `i1 true` a ConstantInt; a non-ConstantInt mask is undefined behaviour, left), init.trampoline,
  stackprotector, localescape, localrecover, eh.exceptioncode/pointer, get.active.lane.mask, experimental.get.vector.length,
  va_start, get.dynamic.area.offset, the fixed-point family, lrint/llrint/lround/llround, bswap, invariant.start, stepvector,
  preserve.array/struct.access.index, ptrmask (now with the vectors' element counts), thread.pointer, threadlocal.address (not a
  global), lifetime (alloca, poison or structured.alloca), sponentry; memcpy/memmove/memset and their inline forms, the debug
  intrinsics and noalias.scope.decl check nothing. The cases that read only types (vector.insert/extract, the partial reductions,
  vector.match, memset.pattern) pass for an intr-arg mutant (the base valid, the types unchanged) and are left for a renamed callee.
  Left to opt, by name: coro.id, fptrunc.round and the arbitrary-fp conversions' strings, VP and constrained intrinsics, the
  element-wise atomics, preallocated calls, the GC intrinsics, statepoints, patchpoints, guard, deoptimize, the matrices, reloc.none,
  the targets' (aarch64, arm, nvvm, amdgcn), ptrauth, structured and convergence intrinsics, callbr.landingpad,
  write_volatile_register, and (for a renamed callee) the funclet check under a scoped EH personality (V:7041);
- the default: no check.
- AutoUpgrade's ctlz/cttz of one parameter over a non-integer (AutoUpgrade.cpp:1438, `ctlz-rebuilt`): declared again as
  `getOrInsertDeclaration` makes it, (T, i1) -> T at the module's end, the calls rebuilt, so the new declaration's
  `isSignatureValid` message is the verdict (a used result of another type left to opt).

**Arbace's verifier fixed (the comparisons found four):**
- AutoUpgrade's ctlz/cttz over a non-integer: the upgraded declaration (T, i1) -> T is now checked (it was skipped as an old form);
  `tx-O2.va-arg-20.intr-rename.800207` was valid for Arbace, invalid for opt;
- a Min flag's value negative by its own width's sign bit (`i1 true`, `i32 -1`);
- PIC Level's Max upgraded to Min as its Error was, and `sign-return-address*` by prefix;
- `invalid requirement on flag, flag does not have the required value` (the value compared; only the flag's presence was).

**Unit tests and controls.** `test/repr/llvm/expect_test.ae` (`test/repr/llvm/expect-test.bash`, seconds): 26 cases, a small
module each with the kind's context (19 of LLS38, 7 of LLS39), the verdict and message each must give, and 6 negative controls
crossing one case's verdict with another's expectation by the same judge, each failing. `controls-test.bash` runs it (the step
`llvm-controls-test`, no change to the steps). `--dump DIR` writes the cases' modules: Arbace's verifier gives all 26 the same
verdict and message, and opt 23.1.5 by hand too (the oracle, to confirm).

**Measured (light; stage 0, `REPR_LLVM_THREADS=2`, the stubs first on PATH; live opt only for the oracle, by hand).** The day's 41
modules of LLS33 (b). h2 (800000, 300 each of intrinsics, metadata and debug) was fitted: its opt answers (llvm-props' oracle
record) were read while writing these rules. h3 (900000, the same families and size, a seed never used before) was made only after
the rules were done; its "before" is the base `e6214cd13`'s mutator over the same mutants.

| set | own before | own after | unknown after | as Arbace's verifier (`verify-check`) | as opt (live) |
|---|---|---|---|---|---|
| h2, 900 (800000), fitted | 815 (90.6%) | 864 (96.0%) | 36 | 864 of 864, 0 disagree | 864 of 864 (llvm-props' record of h2) |
| h3, 900 (900000), held out | 817 (90.8%) | 865 (96.1%) | 35 | 865 of 865, 0 disagree | 865 of 865, 0 differ (`REPR_LLVM_JUDGE=live mutants-oracle`, opt 23.1.5) |

By kind (h3 before, after): md-edit 116, 150 of 150; intr-arg 66, 75 of 75; intr-rename 64, 69 of 75; the rest unchanged
(intr-decl 67, intrinsic 75, md-attach 149, di-attach 55, di-drop 59, di-record 55, di-retarget 51, di-version 60). The unknowns
left on h3 (35): is.fpclass renamed over a non-ConstantInt mask (6: undefined behaviour in the Verifier's cast, opt's answer
garbage); intr-decl's declaration visited before its calls with a changed parameter's attributes (4) and AutoUpgrade's rewrites
of memory intrinsics with their alignment (4); the debug info's (19: retargeted fields, variables and locations sent to others,
instructions' and globals' `!dbg`); a wrapping range (1).

**The families the verifier's fixes touch**, against their records (`bin/repr-llvm check ... --essential`, the llvm-essential
chain): roundtrip-llvmtest, verify-llvmtest, text-llvmtest 43 of 43 and verify-probes 4 of 4, 0 failing against the records, their controls failing; `bin/llvmi check --essential` could not run here (no extracted `llvm/test` of 23.1.5 under the main checkout's `.tmp/llvm/src`: `bin/llvm-corpus extract`, left to the night); `test/repr/llvm/controls-test.bash` with the new expect-test line run at the night. The stubs' log of the measuring runs is empty but the host identities `bin/crops-score` and `bin/gate --plan`
read (`gcc`, `g++`, `clang++`, `rustc --version`), which are not checks.

**Scores.** `bin/crops-score` before and after: LLVM IR 96.4% / 97.9% / 100.0% / 96.9% (completeness, completeness without by
design, maturity, independence), the same: the rt families are not recorded yet, so the checkout's facts do not move. The projection
on the night's records (`test/crops/independence.edn`'s comment), with verify-rt-heldout's ~1,202 mutants at h3's rate (96.1%
rather than LLS36 (a)'s 90.6%): about 1,155 own and 47 on opt's record, judge about 0.889 to (4414 + 0.5 x 1260) / 5674 = 0.889,
and independence about 97.8, unchanged in the first decimal: the rt pack's mutants are mostly of the base and attrs families,
which these rules leave. The night's counts replace both.

### The night's commands (LLS38, LLS39, after LLS36 (a)'s)

LLS36 (a)'s are unchanged: `bin/llvmi ir-make rt` (or `ir-make rt --mutants` if the pack was made before this merge) must run on a
tree with this branch, so the verdict lines are this branch's and `record-opt`'s entries are made afresh. Then:

    test/repr/llvm/expect-test.bash                                              # 26 cases, 6 controls, 0 failing (seconds)
    bin/repr-llvm records-verify --sample 25                                    # the oracle tier: "records-verify mutants verify-rt / verify-rt-heldout ... 0 differ"
    REPR_LLVM_JUDGE=live bin/repr-llvm mutants-oracle .tmp/llvm/rt-lists/verify-rt-heldout.list .tmp/llvm/mo-live   # live opt (~3 min)
    bin/repr-llvm check verify-rt --record; bin/repr-llvm check verify-rt-heldout --record                         # each mutant held to its line

Expected: 0 differ, 0 own verdicts failing against Arbace's verifier. A difference is a rule to fix by day.

### Proposed (llvm-props2)

- **LLS40 The debug info's remaining field rules (19 of h3's 35 unknowns).** visitDISubprogram's and visitDILocalVariable's scope
  rules for a scope sent to another scope, visitDIDerivedType's baseType by tag, the variables and locations a record names sent to
  others (visitDbgVariableRecord's checks, V:7130-7200), an instruction's and a global's `!dbg` (V:3313-3335, visitDIGlobalVariable).
  - (a) Port them. Cost: about half a day. h3 to about 98%.
  - (b) Leave them on opt's record.
  - *Recommended: (a)*, the next largest group and all of one source (Verifier.cpp's visitDI*).
- **LLS41 The undefined cases (is.fpclass over a non-ConstantInt mask, 6).** The Verifier's `cast<ConstantInt>` reads a callee or an
  instruction as a ConstantInt; opt's answer depends on memory layout.
  - (a) A verdict line `unknown` kept by design, and the mutator made to skip is.fpclass as a rename target when the mask is not
    a ConstantInt (the mutants change, the pack's digests with them). Cost: an hour.
  - (b) Nothing: they stay on opt's record (0.5).
  - *Recommended: (b)*: six mutants in 900, and the mutator's bodies stay as recorded.
- **LLS42 AutoUpgrade's call rewrites (intr-decl, 8 of h3).** `UpgradeIntrinsicCall`'s cases for memcpy/memmove/memset with their
  alignment and lifetime of two parameters, and the declaration visited before its calls with a changed parameter.
  - (a) Port UpgradeIntrinsicCall's memory-intrinsic and lifetime cases (AutoUpgrade.cpp:5177-...). Cost: half a day.
  - (b) Leave them.
  - *Recommended: (a) after LLS40.*

## Decided (the user, 2026-11-09, about 11:10): LLS40 (a), LLS41 (b), LLS42 (a)

- **LLS40 (a):** the debug info's remaining field rules are ported (visitDI*, visitDbgVariableRecord's checks, the instructions' and
  globals' `!dbg`).
- **LLS41 (b):** is.fpclass over a non-ConstantInt mask stays on opt's record (0.5): six mutants in 900, the mutator unchanged.
- **LLS42 (a), after LLS40:** AutoUpgrade's call rewrites of the memory intrinsics with their alignment and of lifetime of two
  parameters, and the declaration visited before its calls with a changed parameter.

## As built: LLS40 (a), LLS42 (a) (2026-11-09, branch `llvm-props3`)

Built on next `4be141f28`. Ported from llvm-project 23.1.5 (the pinned tarball, read in place: `llvm/lib/IR/Verifier.cpp`,
`AutoUpgrade.cpp`, `AsmParser/LLParser.cpp`; Apache-2.0 with LLVM exceptions, LICENSE.md, doc/ARBACE.md), into
`arbace.repr.llvm.expect`; `arbace.repr.llvm.verify` (Arbace's verifier) fixed where the comparisons found it short. As LLS36 (a) and
LLS38, this changes the verdict lines the night's `ir-make rt` writes; the mutants' bodies are unchanged byte for byte (the mutator draws
no new random number: `m-di-attach`'s instruction and global cases now attach their context, `{:case :inst :at [fi bi ii]}` and
`{:case :global :gi i}`, as metadata).

**LLS40 (a): the debug info's scopes, variables, locations and attachments.** New in `expect.ae`, each with its Verifier line:
- the walks, as the Verifier makes them: `scope-sp` (getSubprogram, V:7094, up the lexical blocks), `inlined-at-scope`
  (getInlinedAtScope), visitFunction's VisitDebugLoc over every instruction's `!dbg` and its `llvm.loop` node's locations (V:3313-3357:
  `DILocation's scope must be a DILocalScope`, `!dbg attachment points at wrong subprogram for function`, its Seen set), and
  visit(DbgVariableRecord)'s scope agreement with verifyFnArgs (V:7212-7222, V:7467: `mismatched subprogram between #dbg record variable
  and DILocation`, `conflicting debug info for argument`), function by function in the module's order (`scope-verdict`; a module with
  an old debug intrinsic's call is left to opt);
- `di-retarget`: a type's or a subprogram's scope sent to any DIScope, valid (visitDIType, visitDISubprogram: ODR uniquing is off in
  opt); a local variable's scope: not local, `invalid retained nodes, retained node is not local` when a subprogram retains it (that
  subprogram is reached first, from its function), else `local variable requires a valid scope` (V:1720); local, `... retained node does
  not belong to subprogram` (V:1557) or the walks; a derived or composite type's base sent to another type: valid, but a set type's
  (V:1278: an enumeration, a subrange or an integer basic type, else `invalid set base type`); a variable's type to another type: valid;
  a compile unit's enums, retained types, globals, imports, macros sent to a node: `invalid enum list` ... (V:1162-1214); a DILocation's
  scope sent to another local scope: the walks (a key instruction's location, `atomGroup`, and a subprogram declaration as the scope are
  left); a DILocation used only by an `llvm.loop` node: the walk's message;
- `di-record`: a variable sent to another variable, a location to another location: the record's scope agreement and verifyFnArgs;
- `di-attach`: a function's `!dbg` sent to another subprogram: `function definition may only have a distinct !dbg attachment`,
  `DISubprogram attached to more than one function` (V:3245-3253), else the walks; an instruction's `!dbg` sent to a node that is no
  DILocation: LLParser's `invalid !dbg metadata` (P:354), else the walks; a global's `!dbg` not a DIGlobalVariableExpression: `!dbg
  attachment of global variable must be a DIGlobalVariableExpression` (V:794), else valid;
- `di-drop`: a compile unit's `language` without `sourceLanguageName`: LLParser's `missing one of 'language' or 'sourceLanguageName',
  required for !DICompileUnit` (P:6255).
Where a fragment expression is anywhere in the module, the rules that change a variable's type are left to opt (verifyFragmentExpression
reads the variable's size through its type, not ported).

A fix of the base found by h4: a DILocation used by an instruction with few operands (`ret void, !dbg !10`) was taken for unused (the
attachment was looked for after the third operand); `inst-md` finds the `:md` key itself.

**LLS42 (a): AutoUpgrade's rewrites.**
- `mem5-upgraded`: memcpy./memmove. of five parameters declared again over (dest, src, len), memset. over (dest, len)
  (AutoUpgrade.cpp:1612-1641, getOrInsertDeclaration: the intrinsic's type and attributes); each call by UpgradeIntrinsicCall's case
  (AutoUpgrade.cpp:5759): five arguments rewritten from them (left to opt), any other the default case (AutoUpgrade.cpp:5240):
  retargeted when its type is the new declaration's, else its callee cast, `Intrinsic called with incompatible signature` (V:3846); then
  the new declaration's signature. `.inline` names and parameters of other types are left to opt.
- `life2-upgraded`: lifetime.start/end of two parameters declared again over the second's type (AutoUpgrade.cpp:1589); a call of two
  arguments (rebuilt on an alloca or erased, AutoUpgrade.cpp:5856) left, the others as above.
- the declaration visited before its calls with a changed parameter: with an invalid signature AutoUpgrade keeps the written attributes
  (AutoUpgrade.cpp:2001), so visitFunction's verifyFunctionAttrs over the parameters in order comes first (`param-attrs-check`: `Attribute
  'captures(none)' applied to incompatible type!`), then isSignatureValid's message; a valid signature (the intrinsic's own attributes)
  and a vector or aggregate parameter are left.

**Arbace's verifier fixed (the comparisons found five):** a definition's `!dbg` subprogram distinct and attached to one function
(V:3245-3253), visitFunction ending at a failed attachment check before its walk; the compile unit's enum, retained type, imported
entity and macro lists (V:1162-1214); the retained local variables' and labels' scopes (V:1546-1566), and a variable a subprogram
retains reported by the subprogram first; AutoUpgrade's new declaration for an old memcpy/memmove/memset or lifetime form
(`upgraded-fty`): a call of another arity than the old form's whose type is not the new one's, `Intrinsic called with incompatible
signature`.

**Unit tests and controls.** `test/repr/llvm/expect_test.ae`: 36 cases (10 new: 6 of LLS40 on a two-function debug module, the
retargets made in its forms; 4 of LLS42) and 10 negative controls (4 new), 0 failing. The four LLS42 cases' modules (`--dump`) by opt
23.1.5 by hand: the same verdicts and messages (`Intrinsic called with incompatible signature`, `Attribute 'captures(none)' applied to
incompatible type!`, `intrinsic has incorrect number of args. Expected 1, but got 0`, valid).

**Measured (light; stage 0, `REPR_LLVM_THREADS=2`, the stubs first on PATH: no stub called; live opt 23.1.5 only for the oracle, by
hand).** The day's 41 modules of LLS33 (b), 300 mutants each of the intrinsics, metadata and debug families. h2 (800000) and h3 (900000)
were fitted: their opt answers (llvm-props' and llvm-props2's oracle records) were read while writing the rules. h4 (1000000, a seed never
used before) was made only after the rules were done, read once, and showed the base's `!dbg` lookup fault above, which was fixed and
measured again; its "before" is the base `4be141f28`'s mutator over the same mutants (the bodies: 900 compared, 0 differ).

| set | own before | own after | unknown after | as Arbace's verifier (`verify-check`) | as opt (live `mutants-oracle`) |
|---|---|---|---|---|---|
| h2, 900 (800000), fitted | 864 (96.0%) | 895 (99.4%) | 5 | 895 of 895, 0 disagree | 895 of 895, 0 differ |
| h3, 900 (900000), fitted | 865 (96.1%) | 891 (99.0%) | 9 | 891 of 891, 0 disagree | 891 of 891, 0 differ |
| h4, 900 (1000000), held out | 856 (95.1%) | 889 (98.8%) | 11 | 889 of 889, 0 disagree | 889 of 889, 0 differ |

h4 by kind, before and after: di-attach 53, 59 of 60; di-record 51, 60 of 60; di-retarget 48, 59 of 60; intr-decl 68, 75 of 75; the rest
unchanged (di-drop 60, di-version 60, intr-arg 75, intr-rename 67, intrinsic 75, md-attach 149, md-edit 150). The unknowns left on h4 (11):
is.fpclass renamed over a non-ConstantInt mask (6, LLS41 (b): by design on opt's record); ctlz/cttz rebuilt with a used result of another
type (2); an instruction's `!dbg` in a module with an old debug intrinsic's call (1); a DILocation used only as another's inlinedAt (1); a
wrapping range (1). h2's and h3's are of the same classes, with a declaration's `!dbg` sent to a node that is no subprogram (the
Verifier's cast, undefined), a variable's type changed in a module with a fragment and a DISubrange's dropped `count`.

The h3 `intrinsic` mutant `tx-O0.pr58277-1.intrinsic.900040` gives `Intrinsic called with incompatible signature`, opt's second message
(its first is `Invalid user of intrinsic instruction!`): as before this branch, and the live oracle's whole-stderr comparison takes it.

**The families the verifier's fixes touch** (`bin/repr-llvm check ... --essential`, the llvm-essential chain): roundtrip-llvmtest,
verify-llvmtest, text-llvmtest 43 of 43, verify-probes 4 of 4 (and whole: 478 modules, 472 pass, 0 failing against the record), their
controls failing. The whole llvmtest families and `bin/llvmi check --essential` need `llvm/test` of 23.1.5 extracted under the main
checkout's `.tmp/llvm/src` (`bin/llvm-corpus extract`), absent here: the night's. `test/repr/llvm/controls-test.bash`: every line ok but
llvmi's (the same absence).

**Scores.** `bin/crops-score` before and after: LLVM IR 96.4% / 97.9% / 100.0% / 96.9% (completeness, without by design, maturity,
independence), the same: the rt families are not recorded yet (LLS31 (a)'s pack is the night's), so the checkout's facts do not move.
The projection on the night's records (`test/crops/independence.edn`'s comment), with verify-rt-heldout's ~1,202 mutants at h4's rate
(98.8% rather than h3's 96.1%): about 1,188 own and 14 on opt's record; the judge's facts then (4447 + 0.5 x 1227) / 5674 = 0.892
(from about 0.889) and independence about 97.8 to 97.9. The rt pack's mutants are mostly of the base and attrs families, which these
rules leave; the night's counts replace both.

`.tmp/night10/slots/llvm-cells.patch` still applies (`git apply --check`: clean): this branch touches no gate step or record.

### The night's commands (LLS40, LLS42, after LLS38's)

LLS36 (a)'s and LLS38's are unchanged: `bin/llvmi ir-make rt` (or `ir-make rt --mutants` if the pack was made before this merge) must
run on a tree with this branch, so the verdict lines are this branch's and `record-opt`'s entries are made afresh. Then:

    test/repr/llvm/expect-test.bash                                              # 36 cases, 10 controls, 0 failing (seconds)
    bin/llvm-corpus extract                                                     # if .tmp/llvm/src lacks 23.1.5's llvm/test
    bin/repr-llvm check verify-llvmtest; bin/repr-llvm check roundtrip-llvmtest; bin/repr-llvm check text-llvmtest   # whole, against the records
    bin/repr-llvm records-verify --sample 25                                    # the oracle tier: "records-verify mutants verify-rt / verify-rt-heldout ... 0 differ"
    REPR_LLVM_JUDGE=live bin/repr-llvm mutants-oracle .tmp/llvm/rt-lists/verify-rt-heldout.list .tmp/llvm/mo-live   # live opt (~3 min)
    bin/repr-llvm check verify-rt --record; bin/repr-llvm check verify-rt-heldout --record                         # each mutant held to its line

Expected: 0 differ, 0 own verdicts failing against Arbace's verifier, the whole llvmtest families as recorded (the verifier's five fixes
add checks valid modules never meet). A difference is a rule to fix by day.

### Proposed (llvm-props3)

- **LLS43 verifyFragmentExpression's sizes (the variables' types in a module with a fragment).** The rules that change a variable's or a
  type's type are left to opt wherever a `DW_OP_LLVM_fragment` is in the module, since the check reads the variable's size through its
  type chain (DIVariable::getSizeInBits: derived types to their base, composite and basic sizes).
  - (a) Port getSizeInBits and the two fragment checks (`fragment is larger than or outside of variable`, `fragment covers entire
    variable`, V:7451) and drop the condition. Cost: an hour or two. One or two mutants in 900 of the debug family.
  - (b) Leave them on opt's record. *Recommended: (a)*: it also lets Arbace's verifier check the fragment rule on the corpus's own modules.
- **LLS44 The DILocation used only as an inlinedAt, and the old debug intrinsics' calls.** The location walks are ported for the
  instructions' and loops' locations; a location reached only through another's `inlinedAt` (getInlinedAtScope over a non-local scope: a
  `cast` the Verifier does not guard) and a module with an old `llvm.dbg.*` call (made a record by AutoUpgrade first) are left.
  - (a) Port the inlinedAt chain's visit (visitDILocation over the chain, V:1074) and the old calls' conversion order
    (upgradeDbgIntrinsicToDbgRecord) into the walks. Cost: half a day; two or three mutants in 900.
  - (b) Leave them. *Recommended: (b)*: the cast is undefined for the first; the second is the old intrinsics' form, which the corpus
    keeps only in a few modules.
- **LLS45 A held-out set in the gate.** h2, h3 and h4 live only in the worktrees' `.tmp`; the night's rt pack holds the recorded mutants,
  but a set made by the current mutator from a fixed seed is not checked anywhere.
  - (a) A part of `llvm-full`: `bin/repr-llvm mutants` of a fixed seed (300 a family), `verify-check` in own mode (own verdicts against
    Arbace's verifier, no opt), about 7 minutes, with the existing controls. Cost: an hour (a step's part, the fragment, its waiver).
  - (b) Leave it to the rt pack. *Recommended: (a)*: a rule change that breaks an own verdict would then fail by night without opt.

## Decided (the user, 2026-11-09, about 14:20): LLS43 (a), LLS44 (b), LLS45 (a)

- **LLS43 (a):** port getSizeInBits and verifyFragmentExpression's two checks, and drop the fragment condition from the own verdicts.
- **LLS44 (b):** the DILocation used only as an inlinedAt and the old debug intrinsics' calls stay left to opt.
- **LLS45 (a):** a held-out set made afresh from a fixed seed, its own verdicts held to Arbace's verifier by night, without opt.

## As built: LLS43 (a), LLS44 (b), LLS45 (a) (2026-11-09, branch `llvm-props4`)

On `llvm-props4` from `mac/next` 6c3c87c02, main's stage 0, `REPR_LLVM_THREADS=2`. Sources read: llvm-project 23.1.5's
`llvm/lib/IR/Verifier.cpp` (V:7425-7464 verifyFragmentExpression, V:1749-1753 visitDIGlobalVariableExpression, V:799
visitGlobalVariable's direct call, V:536 visitDbgRecords, V:1713-1722 visitDILocalVariable) and `DebugInfoMetadata.cpp:1653`
(DIVariable::getSizeInBits), from the pinned tarball, extracted into the worktree's `.tmp` alone.

**LLS43 (a), Arbace's verifier** (`arbace/repr/llvm/verify.ae`): `frag-info` (getFragmentInfo of a valid expression),
`var-size-bits` (getSizeInBits: up the type chain, a type's constant non-zero size, through derived types to their base, else
none) and `check-fragment` (`fragment is larger than or outside of variable` when the Verifier's unsigned 32-bit sum of size and
offset exceeds the variable's size, `fragment covers entire variable` when the size is the variable's; culprits the record or the
expression, then the variable). A record's check runs after the record's visit, whatever that found, also in a definition the
location walk leaves (no subprogram, a failed attachment check: InstVisitor visits every instruction); `dbg_assign` too; an
artificial variable is skipped (V:7443). A global variable expression's check runs where visitMDNode reaches it (a compile unit's
`globals`) and again for each global's `!dbg` (visitGlobalVariable calls visitDIGlobalVariableExpression itself, so opt prints the
message twice). Each new message byte for byte opt 23.1.5's whole stderr on eight modules (the six cases below and two by hand,
a global's and two records', run by hand with opt).

**LLS43 (a), the own verdicts** (`arbace/repr/llvm/expect.ae`): `module-fragments?` no longer leaves a rule to opt. The
fragment checks are written apart from the verifier's (`frag-of`, `var-bits`, `frag-msg`, `record-frag`, `gve-frag`) and
`fn-scope-checks` walks them in the Verifier's order: each definition's subprogram first (its compile unit's global variable
expressions, each the first time it is reached), the location walk, the records (each: scope and argument checks, then its
fragment; a definition without a subprogram: the fragments alone), then the globals' `!dbg` and `!llvm.dbg.cu`'s units. A
variable's type, a type's base (the set type's base rule kept first; both a set base error and a fragment message: left to opt,
their order is the node visit's) and a global's `!dbg` sent elsewhere now give the fragments' first message or valid; a record's
variable sent to another variable its scope checks then its fragment.

**Found by the held-out set (LLS45 (a)):** Arbace's verifier lacked visitDILocalVariable's last check (V:1721-1722), `invalid
type` for a local variable whose type is a subroutine type (the own verdict had it): added, reached only with a local scope.

**LLS44 (b):** nothing changes; the two cases stay `unknown`, judged by opt's record.

**LLS45 (a):** `bin/repr-llvm heldout-check [WORK]`: `test/llvm/heldout-bases.txt`'s 41 modules (LLS33 (b)'s day inputs: 21 of
the pinned pack tx, the four `hw-*`, 16 llvm/test files from the pinned tarball's extraction), 300 mutants of each of the
intrinsics, metadata and debug families from the fixed seed 1200000 (`REPR_LLVM_HELDOUT_SEED`), `verify-check` in own mode: every
own verdict must be met by Arbace's verifier (verdict and declared message), the unknowns only counted, and the negative controls
`test/repr/llvm/heldout-controls.txt` (two mutants of the set crossed: a rejection against a valid mutant, two rejections with other
messages) must fail. The gate step `llvm-heldout` (`llvm-full`, after `llvm-print`; `:mem-gib 1`), its controls file in `:controls`,
the crop's step list and `test/crops/independence.edn`'s read and judge stages (it adds no case: its unknowns are not judged).

| command | result |
|---|---|
| `bin/repr-llvm heldout-check` (before the `invalid type` fix) | 886 own of 900, 885 met: `hw-dbg.di-retarget.1200276` (`invalid type`, opt agrees by hand) |
| `bin/repr-llvm heldout-check` | 900 mutants, 886 own verdicts (98.4%), 886 met by Arbace's verifier, 14 unknown; 2 controls fail as they must; 480 s, 0.71 GB |
| `test/repr/llvm/expect-test.bash` | 42 cases (6 new: LLS43's), 12 controls (2 new), 0 failing; the six mutants (`--dump`, now the mutated forms printed) by opt 23.1.5 by hand: the same first message, and Arbace's verifier opt's whole stderr byte for byte |
| llvm-read-essential's and llvm-print-essential's commands | roundtrip, verify, probes 43, 43, 4 and text 43 pass, 0 failing, the controls fail |
| `bin/repr-llvm check verify-probes` (whole) | 478 modules, 472 pass, 0 failing against the record |
| `test/repr/llvm/controls-test.bash` | every line ok but llvmi's (no 23.1.5 `llvm/test` extraction in the main checkout, as on llvm-props3) |
| `bin/llvmi check --essential` | not run here: the same absence (`bin/llvm-corpus extract`, the night's); llvmi is untouched |
| `test/gate/crops-test.bash`, `bin/gate --full --plan` | ok; exit 0, `llvm-heldout` planned |
| `bin/reap`, `bin/crops-score --judge --strict` | 0 problems; 11 crops, 0 problems |

`bin/crops-score --crop llvm` before (mac/next) and after: 96.4% / 97.9% / 100.0% / 96.9%, the same (the held-out set's
verdicts are no cell's case; the rt families are still the night's).

### The night's commands (LLS43, LLS45)

    bin/llvm-corpus extract                                       # if the main checkout's .tmp/llvm/src lacks 23.1.5's llvm/test
    env REPR_LLVM_THREADS=2 bin/repr-llvm heldout-check           # the gate's llvm-heldout: about 8 min, 0.7 GB
    bin/repr-llvm check verify-llvmtest; bin/repr-llvm check roundtrip-llvmtest; bin/repr-llvm check text-llvmtest   # whole
    bin/llvmi check --essential; test/repr/llvm/controls-test.bash
    REPR_LLVM_JUDGE=live bin/repr-llvm mutants-oracle .tmp/repr-llvm/heldout/mutants.list .tmp/llvm/mo-heldout   # oracle tier, by hand

Expected: 0 own verdicts unmet, the whole llvmtest families as recorded (Verifier/fragment.ll's own verdict is valid with the
warning, which the new message now matches), live opt agreeing with the 886 own verdicts.

## As built: LLS21 (a), ci's gaps (2026-11-05, branch `ci-lls21`)

What the user decided (LLS21 (a)) is in ci, C's crop (`arbace/c/build.ae`, `arbace/c/check.ae`), no new native and no Go
change: nothing of go/arbace/ci or crt moves, so cf2g's AOT needs no new node kind.

- `fmodf` and `fmodl` are in the builder's `builtin-aliases`: `__builtin_fmodf` and `__builtin_fmodl` call musl 1.2.6's functions
  (long double is double in ci, CS35), as `fmod` always did.
- The `__atomic_*` and `__sync_*` families are lowered by the builder to plain loads and stores (`atomic-call`, `atomic-name`):
  ci runs one thread, so every memory order is sequential consistency and a read-modify-write is not interrupted, no `sync/atomic`
  needed; the memory-order arguments are evaluated for their effects and ignored. Covered: `load_n`, `store_n`, `exchange_n`,
  `compare_exchange_n` and the generic `load`, `store`, `exchange`, `compare_exchange` (their value arguments by pointer),
  `fetch_{add,sub,and,or,xor,nand}`, `{add,sub,and,or,xor,nand}_fetch`, `test_and_set`, `clear`, `thread_fence`, `signal_fence`,
  and the `__sync_` spellings (`fetch_and_OP`, `OP_and_fetch`, `bool_compare_and_swap`, `val_compare_and_swap`, `lock_test_and_set`,
  `lock_release`, `synchronize`). `__atomic_always_lock_free` and `__atomic_is_lock_free` stay as before. The object is an integer,
  a pointer or a `_Bool` (anything else is refused by the builder: a `_BitInt` wider than 64 bits, an `__int128`, a float). The checker
  types the result (`atomic-res`: the pointed-to type for a value, `_Bool` for compare-exchange, test-and-set and
  `__sync_bool_compare_and_swap`, void for the rest) and no longer lets the parser's prelude declarations `void __atomic_load ();`,
  `__atomic_store ()`, `__atomic_exchange ()` (cc's, for `<stdatomic.h>`) make the generic forms "too many arguments".
- A call through a function pointer reads its result at the pointer's return width (`call-node`: `(cv "i32" ...)` around an integer
  call of 8, 16 or 32 bits when the callee is not a named function): gcc's code reads `eax`, whatever type the callee was defined
  with, so a comparator lowered as returning `unsigned`, called by musl's `qsort` and `bsearch` as `int`, gives -1. Direct calls are
  untouched (the callee's own type is the call's).
- Samples in `test/c/run/` (the `ci-run` corpus, 11 files to 13, gcc 15.2's runs in `test/c/ref-gcc15.edn`, results in
  `test/c/check-linux-amd64.edn`, in the corpora's `subset-cf2g`, `subset-read` and `subset-run`): `builtin-atomic.c` (every
  operation above, at char, short, int, long long, pointer and `_Bool`, both compare-exchange forms succeeding and failing, the
  `__sync_` spellings, fmodf/fmodl) and `fnptr-result-width.c` (qsort with a comparator returning unsigned, a long read as int, an
  int read as unsigned char). Both equal gcc -O0's output byte for byte.

Checked (on main's stage 0 with `arbace.c.check` and `arbace.c.build` loaded from the branch: the embedded copies are older, so the
tools' host loads these two files over them; light, nothing built):

| check | result |
|-|-|
| `bin/llvmi check --only` of the 7 modules (`ee.MCJIT.frem`, `c-O{0,2}.synth.{decl-atomic,decl-qualifiers,ext-callbacks}`) | 7 of 7 pass (were: ci-builtins 5, abi-punning 2) |
| `bin/ci check --corpus ci-run --only` (the two new files and three old) | 5 of 5 pass |
| `bin/ci judge-test` | 104 tests, 464 assertions, 0 failures |
| `bin/ci dg --essential` | 92 of 92 listed files pass, the control differs |
| `bin/ci check --only test/c/essential-check.txt` | every program passes (the essential run check) |

`test/llvm/check-eval-linux-amd64.edn`: the 7 rows are `:pass` now (edited by hand from the 7-module run; the next full
`bin/llvmi check --record` confirms them). The classes `ci-builtins` and `abi-punning` stay in `divergences-eval.edn` because
`test/llvm/check-aot-linux-amd64.edn` still records the 7 as non-passes (AOT not rerun: cf2g's Go build is not a light check);
once the AOT record is rewritten both classes have no members and go. This moves `gate_key` (ci is embedded): the main host merges
it in a rekey night, and the 7 rows of the AOT record and the new samples' `check --aot` rows (`test/c/check-aot-linux-amd64.edn`:
`ci-run/builtin-atomic.c`, `ci-run/fnptr-result-width.c`, not recorded here) are the night's.
## As built: LL4, the corpus loop (2026-11-05)

Branch `llvm-ll4` from `llvm-ll1`. Light runs only (main's keyed `--full`, then tonight's gate): every process under 2 GiB,
`ARBACE_HOST_EXE` the main checkout's stage 0, the lists a few hundred files, opt the pin's `/usr/lib/llvm23/bin/opt`. The rules,
the tables and the machinery are doc/llvm/SPEC.md §6.2.3; the sources read are llvm-project 23.1.4's (the pinned tarball, LLS11),
`lib/IR/{Verifier,Intrinsics,AutoUpgrade,Type,Attributes,AsmWriter,DebugInfoMetadata}.cpp`, `lib/AsmParser/{LLParser,LLLexer}.cpp`,
`include/llvm/IR/Intrinsics*.td`, `DebugInfoFlags.def`, `include/llvm/BinaryFormat/Dwarf.{def,h}` (extracted into the worktree's
`.tmp/`; no other network). Two tables are generated from them and committed: `arbace/repr/llvm/intr_table.ae` (`bin/llvm-corpus
intrinsics`, the pin's `llvm-tblgen --dump-json` over Intrinsics.td: 16,423 intrinsics) and `di_table.ae` (`bin/llvm-corpus
di-table`: 246 fields of 29 node kinds, 234 print fields, the DWARF names, flags and op codes).

**The samples, before and after** (`bin/llvm-corpus verify-record LIST WORK OUT`; the records `test/llvm/llvm-test-*-verify.tsv`,
NAME, opt's verdict, Arbace's, verdicts same, first messages same, whole stderr same; "before" the verifier of `llvm-ll1`):

| list | files | verdicts as opt's | first messages | whole stderr |
|---|---|---|---|---|
| `llvm-test-valid.list` (LL1's 300 valid) | 300 | 277 → **300** | | 277 → **300** |
| `llvm-test-valid-wide.list` (new: every 97th module of input.list, under 60 KB) | 420 | **420** | | **420** |
| `llvm-test-reject.list` (LL1's 200 rejections) | 200 | 77 → **195** | 60 → **189** | 43 → **183** |
| `llvm-test-split.list` (60 sections) | 60 | 57 → **60** | | 57 → **60** |
| `llvm-test-reject-av.list` (new: every Assembler/ and Verifier/ rejection opt rejects) | 578 | 227 → **554** (95.8 %) | 184 → **538** | 133 → **516** (89.3 %) |
| `llvm-test-reject-other.list` (new: the other directories', acceptance only) | 179 | 43 → **176** (98.3 %) | 25 → 173 | 24 → 172 |

Two more valid samples held out (not committed, each run once after the rules of the day): every 97th from the 50th (415 modules):
414 of 415 verdicts, the one a template file (`[SIZE x ptr]`, a parse error read in other words); every 37th from the 11th (1,088
modules): **1,087 of 1,088 verdicts**, 1,086 whole stderr; the one false error an AMDGPU `wmma` declaration AutoUpgrade rewrites.
Their false errors found on the way (named types not resolved in operand checks, common globals' float zeros, speculatable
intrinsic callees, self-references in unreachable blocks, AutoUpgrade's old forms) were fixed before.

The tests' own declarations (`bin/llvm-corpus reject-check`, an `error:` in a CHECK-style line): of the 200, 67 declare one, opt
prints it in 65, Arbace's verify now in **64** (17 before); of the 578, 190 declare, opt 188, Arbace **185**; of the 179, 27, opt 16,
Arbace 14. The reader side holds: `roundtrip` over the 300 valid: forms **300 of 300**, bitcode 300 of 300, objects 252 (48 `none`);
`roundtrip --text` **300 of 300** byte for byte; `test/llvm/essential.txt` 43 of 43 forms, the control fails as it must. The regression
sets of LL4b/c: the probes (`test/llvm/probes.list`) verdicts 472 → **474** of 478, whole stderr 470 → **471** (the rest the classes of
`probes-known.tsv`); 269 mutants of the 300 valid (`bin/repr-llvm mutants .tmp/valid.abs OUT 300 700000`, held out, generated once):
verdicts 252 → **268** of 269, whole stderr 226 → **251**.

The 24 Assembler/ and Verifier/ rejections Arbace still takes (or cannot read): the AMDGPU intrinsics' and allocas' rules (9, Verifier/
AMDGPU), the vector-predication, vector insert/extract, `get_dynamic_area_offset`, statepoint relocation (2), `callbr` intrinsic,
memprof, `modular-format`, funclet-token, logical-pointer, structured-gep and non-byte vector GEP rules (one each), `DILocation not
allowed within this metadata node`, and two of the lexer (`*/` at the top level, `u0x` integers). Of the other directories':
`dso_local_equivalent`, a DIAssignID operand, `amdhsa_code_object_version` in a constant. The 38 with the verdict right but not the
rest of the output: mostly later messages of the same families (statepoints, matrix, convergence, deoptimize, guard, the use-list
order of two users of one intrinsic, a node opt prints by its address, attribute groups of intrinsic declarations).

### The night's runs (commands and what to expect)

As LL1's (under `bin/heavy-lock` on the loaded host; `ARBACE_HOST_EXE` unset in the gate):

| what | command | expected |
|---|---|---|
| the lists | `bin/llvm-corpus full-lists; bin/llvm-corpus reject-lists` | 578 and 179 files; `git diff test/llvm/llvm-test-reject-{av,other}.list` empty |
| the tables | `bin/llvm-corpus intrinsics; bin/llvm-corpus di-table` | `git diff arbace/repr/llvm/{intr,di}_table.ae` empty (llvm-tblgen and python3 of the host) |
| the records | `bin/llvm-corpus valid-wide`, then `bin/llvm-corpus verify-record LIST WORK test/llvm/llvm-test-NAME-verify.tsv` for valid, valid-wide, reject, split, reject-av, reject-other (resolved by `bin/llvm-corpus resolve`) | `git diff test/llvm/*-verify.tsv` empty; under a minute each at 4 threads |
| every rejection test | `bin/repr-llvm verify-check .tmp/llvm/lists/reject.list WORK` | 1,706 files: the 757 opt rejects as the two records (verdicts about 96.5 %), the rest (`not llc`, passes' rejections opt -passes=verify takes) `valid`/`valid` |
| the valid corpus | `bin/repr-llvm verify-check LIST WORK` over `.tmp/llvm/lists/input.list` in shards of 2,000 (`split -l`) | the held-out samples (1,087 of 1,088) predict about 0.1-0.5 % false errors, a few hundred of 42,555, mostly AutoUpgrade's rewrites and large CodeGen modules; each one a class of `test/llvm/divergences-verify.edn` or a rule to fix |
| the mutants | `bin/repr-llvm mutants .tmp/valid.abs OUT 600 800000` then `verify-check` | about 99 % verdicts, 93 % whole stderr (the 269 held out) |

### Proposed

1. **LL4's close** (LLS6). (a) Close LL4 at these numbers: the A/V rejections at 95.8 % of verdicts and 89.3 % of opt's whole
   stderr, the target directories at 98.3 % of verdicts, the valid samples 300 of 300 and 420 of 420 (1,087 of 1,088 held out); the rest classed after the night's whole-corpus
   run in `test/llvm/divergences-verify.edn` (`:target-intrinsic`, `:intrinsic-family`, `:md-debug-locs`, `:lexer`, `:use-list-order`,
   `:printed-address`), ported when a client crop needs them. Cost: the classing, an hour, after the night. (b) One more day for
   the target intrinsics (AMDGPU, NVPTX, the vector-predication and matrix families) and the rest of the first-message-only list:
   about 1 day for A/V verdicts near 99 % and whole stderr near 95 %. Recommend (a): the residue is target-specific, and LL2's
   lowering, not the verifier, is the row's next need.
2. **The rejection lists as the interim llvm step's parts** (GT9, LLS20). (a) Add `llvm-test-reject.list`, `-reject-av.list` and
   `-reject-other.list` as parts of the interim `llvm-roundtrip` step's verify check, their records the expected outputs (957 files,
   about 1.5 minutes at 4 threads, under 2 GiB a process; opt of the pin as the reference, which the step already needs; and
   `llvm-test-valid-wide.list`, 420, for the false errors). (b) Only
   the 300 valid modules (as decided after LL1) until LL4 closes. Recommend (a): the rejection tests are what moved; without them a
   regression of the verifier's rules is seen only by hand.
3. **The generated tables** (`intr_table.ae`, `di_table.ae`: 0.9 MB and 30 KB, from the pinned tarball with the host's llvm-tblgen
   and python3). (a) Committed, with the two verbs re-run and diffed in the night's vendoring checks. (b) Generated at use from the
   tarball (no committed copy; every verify run would need the tarball and llvm-tblgen). Recommend (a).

## Decided (2026-11-05, the user: LL4's proposals)

LL4's three proposals (a): LL4 closed at these numbers, the remainder classed in test/llvm/divergences-verify.edn after the night's run; the rejection lists and the 420 valid modules as parts of the interim llvm gate step; the generated intrinsic and debug-info tables committed, their regeneration diffed in the night's vendoring checks.

## Decided (the user, 2026-11-07: batch 3's proposal 2) and as built

**2 (a)+(b).** The own verdict (LL9a) takes lit's substitution: `bin/repr-llvm`'s `own_verdicts` gives, as a fourth column, the first RUN
line's `sed 's/A/B/g' %s |` (a plain substitution alone), and `verify-check` reads a module judged by its own verdict as sed's output
(`WORK/subst/NAME.ll`), as the test's tools read it; a `valid` claim from a RUN line reading the module with `--disable-verify` is no own
verdict (the test claims no verification). On batch 3 (`55f9900c8`): valid-wide 413 -> 419 of 420 (`CodeGen-RISCV-rvv-{masked-tama,
sf_vfexp32e,vbrev,vfmv.s.f,vmnor}`, `s/iXLen/i32/g`; `tools-llubi-divrem_ub3`, `s/DIVREM/sdiv/g`), the reader's answers unchanged; the
one left, `LTO-Resolution-X86-protected-def-dllimport` (`llvm-as --disable-verify`), judged by opt's verdict, not yet recorded: classed
`disable-verify` in `test/llvm/valid-wide-known.tsv` (`REPR_LLVM_KNOWN`) until the night's `bin/repr-llvm record-opt`, the class removed
when it passes. The other lists' records unchanged (valid, reject, split, reject-av, reject-other); `check verify-llvmtest` re-recorded
(792 of 806: 11 fixed by LL4's messages), roundtrip-llvmtest and text-llvmtest 304/304, verify-probes 472/478.

## As built: llvm-compl2, deferred non-passes closed (2026-11-10, branch `llvm-compl2`)

A light lane, decision-free: the deferred classes whose cause was a gap in Arbace's own tools, re-judged by whole runs of the
families and recorded from passing runs.

- `bin/llvmi` ran the seed's ci, not the checkout's: it never sourced `bin/lib/ci/fresh.bash`, so a seed older than the checkout's
  `arbace/c/` (LLS21 (a)'s `__atomic_*`, `fmodf` and function-pointer result widths, all after `d5548e72`) checked and built the lowered C
  with the old builder, and the records kept `:ci-builtins` and `:abi-punning` items that the checkout had fixed. `run`, `lower`, `check`
  and `modules` now call `c_ci_fresh`, as `bin/ci` does (the checkout's ci evaluated until the next re-seed).
- `bin/llvmi check --aot`'s module had no `cilib` link (RES34's prebuilt libraries, which cf2g's programs import since then): every
  program importing a musl unit failed to build. The module links `CI_AOT_LIB` (default the main checkout's `.tmp/ci-aot-lib`), as
  `bin/lib/ci/check-aot`'s does.
- `arbace/repr/llvm/verify.ae` (LLVM 23's Verifier, visitIntrinsicCall and visitVPIntrinsic): gc.relocate tied to a statepoint (or
  undef, or a landingpad) with constant base and derived indices inside its gc-live bundle; the VP casts' equal vector lengths, vp.fcmp's
  and vp.icmp's predicates, experimental.vp.splice's index range (times the vscale_range minimum); the module flag
  require-logical-pointer refusing plain getelementptr and alloca; AMDGPU (an amdgcn triple): allocas in addrspace(5),
  s.prefetch.data's address spaces, av.load/av.store's metadata string, the cooperative atomics' pointer and metadata node.

Results (whole families, recorded): `bin/llvmi check` 537 of 562 (was 529; with the checkout's ci the x86_fp80 aggregates are
the data layout's size, so 7 of `:long-double`'s 10 pass), `check --aot` 537 of 562 (was 522; 22 min and 45 min evaluated, under
1.5 GB); `bin/repr-llvm check verify-llvmtest` 800 of 806 (8 of `:llvmtest-rules` fixed), verify-probes 472/478, roundtrip-llvmtest
and text-llvmtest 304/304 unchanged. `:ci-builtins` and `:abi-punning` removed; `:long-double`'s tests are the 3 left. The crop's
completeness 96.4% to 97.6% (read 98.7 to 99.2, eval 94.1 to 95.6, AOT 92.9 to 95.6). Left deferred: `:personality` 13 a level
(the C++ runtime on ci), `:dg-options` 2 (the pack remade by clang with each test's options, by hand), the read's 4 rules (the whole-wave
call, load-to-lds, mfma-scale, DILocation placement) and 2 messages, the probes' 5.

## As built: LLS46, which stages stay below 100 and what removes it (2026-11-11, branch `llvm-ind1`, from mac/next)

**Where the independence is lost.** Four of the five stages are 1.0 (read, eval, AOT, print: no toolchain at check time, the pinned
packs counting as input in the crop's language, LLS27 (a)). Only the judge is below: 0.843 (2068 cases by their own verdict, 948 by a
toolchain's record at 0.5), independence (4 + 0.843) / 5 = 96.9. The 948 recorded cases are three populations:

| cases | judged by | why there is no own verdict |
|---|---|---|
| 478 probes (`test/llvm/probes`) | opt's record (`ref-opt23.edn`: verdict, first message, stderr's digest) | bare modules written to provoke one Verifier rule; no RUN or CHECK line, no stated expectation |
| 116 `llvm/test` modules (verify-llvmtest) | opt's record | the test's own verdict is not a verify verdict Arbace can read: 57 `valid` split-file sections (their RUN lines are in the parent file), 41 whole files of split-file tests, and 18 whose RUN line reads the file with an option or by a redirection |
| 177 llvmi modules, at each level (354) | lli's record (`ref-lli23.edn`) | clang 23's IR of the checkout's C and C++ programs that print values (162 `c-`, 14 `cpp.`, 1 `ee.`); no expected output beside them |

**Built (LLS46 (a)): a redirection is not an option.** `own_verdicts` (bin/repr-llvm) refused a RUN line such as
`not llvm-as < %s > /dev/null 2>&1` or `not llvm-as < %s >/dev/null 2> %t | FileCheck %s < %t` because `plain` took the redirection's
file (`/dev/null`, `%t`) for an option that changes how the module is read. A redirection, `>`, `>>`, `1>`, `2>`, `&>` with its file as the
next word, `2>&1`, is no option now. Sixteen llvm/test modules (the Verifier/GC/Assembler tests of the old `not llvm-as` form) are
judged by their own `invalid` (and the CHECK fragment where one is declared) and no longer by opt's record; every one is still met by
Arbace's verifier (verify-llvmtest: 800 of 806 as recorded, 706 by their own verdict, was 690; 446 of 448 rejections with the
reference's message). A negative control for the new form: `_control/redirect-vs-valid` (`Analysis-BasicAA-128-bit-ptr` against
`Verifier-gcwrite-ptrptr`'s own reference), fails as it must. The judge stage 0.8428 to 0.8455 (2084 own, 932 recorded: independence
96.86 to 96.91; `test/crops/independence.edn`'s `:llvm :judge`).

**Built (LLS46 (b)): StripDebugInfo and the loop ids' locations (the second reported gap).** `arbace.repr.llvm.expect/stripped-reachable`
and the verifier's own `verify/stripped-reachable-md` walked an instruction's `!llvm.loop` node whole, so a node reached only through the
loop id's `DILocation` operands (start and end location: scope, then the subprogram, the unit, its `globals:` list, the variable) counted
as left behind by StripDebugInfo, and a debug verdict was made `invalid` (opt's second verifyModule). DebugInfo.cpp's
`stripDebugLocFromLoopID` drops those operands: opt says `debug`. Both walks skip a loop id's DILocation operands now. That is
`pr79354.g2.di-retarget.500483` (a DIGlobalVariable's `file:` sent to a DILocation, `!llvm.loop !56` in `foo`): the mutant's line said
`invalid`, opt's record `debug` with the message the same (`invalid file`); and it was the cause of three of the pack's recorded
`:verdict` rows in verify-rt-heldout too (`coroutine.g0.di-retarget.500278`, `io.g0.di-retarget.600072`, `io.g0.di-drop.600358`, verify
said invalid where opt says debug): those three pass now. Unit cases `retarget-gv-file-loop` (`debug`) and `retarget-gv-file-kept` (a plain
`!keep` attachment names the variable: `invalid`), crossed as a control; the first fails on the old rule (checked by reverting the file).
expect-test: 44 cases, 13 controls, 0 failing. The pack's mutants carry the line as it was made, so the pack's `pr79354.g2.di-retarget.500483`
still says `invalid` and now verify says `debug`: its row in `check-verify-rt-heldout-linux-amd64.edn` is a `:verdict` ("the test says
invalid, verify says debug"), in `:debug-info-rules`; the pack's remake (a night's, `ir-make rt --mutants`) writes the corrected line and the
row passes. Whole families on the checkout: verify-llvmtest 800 of 806, verify-probes 472 of 478, verify-rt 904 of 905 (all as recorded),
verify-rt-heldout 1,096 of 1,101 (was 1,095: three fixed, one new, the stale line's); `bin/repr-llvm heldout-check` (900 mutants made afresh
with both rules): 886 own verdicts, 886 met by Arbace's verifier, the controls fail. `bin/reap --record`, `bin/crops-score --record`: the
LLVM row's completeness 97.65 to 97.66 (read 3,826 to 3,828 passes, deferred 19 to 17: three fixed, the stale line one more), independence 0.9686 to 0.9690 (scores.edn).

**Why `mutants-oracle` reads "differs" in record mode and "same" live (the first gap)** is a limit of the record, not of the rules:
`ref-opt23.edn` keeps opt's first message (`:first`) and a digest of the whole stderr. `builtin-prefetch-5`'s intrinsic mutants
(`builtin-prefetch-5.win.intrinsic.265`, `.g0.intrinsic.500128`) get from opt `Invalid user of intrinsic instruction!` and then
`Intrinsic called with incompatible signature`; the mutant names the second, which is in live opt's stderr and not in `:first`. The two
attr-dup mutants (`rs.g0.attr-dup.600096`, `p1-first.lto.attr-dup.600342`) are the same shape. Measured over the pack's two lists in
record mode: 636 own verdicts, 635 as opt's (verify-rt) and 1069, 1065 (verify-rt-heldout); the 5 differences are these four message
gaps and the di-retarget verdict above.

### Proposed (llvm-ind1)

- **LLS47 The mutants' message in record mode.**
  - (a) Record mode compares the verdict only; the message is the live mode's (opt's whole stderr, `REPR_LLVM_JUDGE=live`, the oracle
    tier's authority). Cost: one awk condition in `mutants-oracle`; no record changes; the 4 false differences go. Loses the message
    check on the 1,700 mutants by record (it stays live and in `verify-check`, which holds each own message to Arbace's verifier).
  - (b) The rules emit opt's first message for a kind whose message is a later one (prefetch's `Invalid user of intrinsic instruction!`,
    the attr-dup order). Cost: the Verifier's order for those kinds, then the pack's remake (the mutants' hashes change: `ir-make rt
    --mutants` and the night's `record-opt`). Changes what a mutant's message means (the first, not the rule's own).
  - (c) `ref-opt23.edn` keeps all of opt's messages (`:messages`), record mode finds the fragment among them. Cost: `record-opt` over
    about 2,000 modules (the oracle tier, opt 23.1.5, minutes) and an awk change; record mode then equals live.
  - *Recommended: (a)*: the record is a day-time approximation by design (above), live is the check that counts, and (a) costs nothing;
    (c) if record mode is to be authoritative on its own.
- **LLS48 The 478 probes' own verdicts** (the largest part: +1.6 points of independence).
  - (a) Leave them at 0.5: opt's record is what they are for (a differential of Arbace's verifier against opt's on hand-picked rules).
  - (b) Each probe states its expectation (`; EXPECT: invalid|valid|debug FRAGMENT`) derived by hand from Verifier.cpp's and
    LLParser.cpp's 23.1.5 source, as the mutator's rules are, never copied from opt's record (a copy would be the record under another
    name, and the check could not tell). Cost: 478 files read against the source, days; the header read by `own_verdicts` is a few lines.
  - *Recommended: (a)* until the probes are rewritten as rule tests; (b) is the way if the user wants the stage at 1.0.
- **LLS49 llvmi's 177 modules (162 C, 14 C++, 1 ee).**
  - (a) Leave them at 0.5, as the C crop's synth and ci-run corpora and C++'s samples are (gcc's and g++'s records).
  - (b) Judge each by agreement with ci's run of the C source. *Not valid*: llvmi lowers the IR to C and runs it on ci, so both
    runs are ci's; an error in ci agrees with itself.
  - (c) Expected output beside each program (`.expect`), written from the language's rules by hand: 176 programs, days, and the C crop
    would want the same files.
  - *Recommended: (a)*; (c) only together with C's and C++'s decision on the same programs.
- **LLS50 The split-file sections (57 `valid`, +0.9 points of the judge).** A section `NAME.ll-part` whose parent test passes
  `%t/part.ll` to opt, llc, llvm-link or llvm-as with no `not` and no `--disable-verify` states that the tools take it, and they verify
  their input: `valid`, met by Arbace's valid or debug as the other `valid` verdicts are.
  - (a) `own_verdicts` reads the parent file's RUN lines for a section (the section's parent path is in the split manifest). Cost:
    half a day, an awk change and a control; the 41 whole-file entries (the RUN lines run on sections, not on the file) stay opt's.
  - (b) Leave them. *Recommended: (a)*.

### The night's commands (LLS46)

    bin/repr-llvm check verify-llvmtest                    # 800 of 806, 706 own: about 30 s, under 1 GB (run by day)
    bin/repr-llvm heldout-check                            # 886 of 886 own, met by Arbace's verifier: 7 min, 0.7 GB (run by day)
    REPR_LLVM_JUDGE=live bin/repr-llvm mutants-oracle .tmp/repr-llvm/heldout/mutants.list .tmp/llvm/mo-heldout   # oracle tier: the new rule against live opt, by hand

A fresh worktree needs the musl forms' cache for `bin/llvmi` (`.tmp/ci-musl/forms`, 11 MB: `bin/ci musl read`, or copy the main
checkout's); without it llvmi's build step finds no forms and `test/repr/llvm/controls-test.bash` fails its llvmi parts (found here).

## Decided (the user, 2026-11-11): LLS47 (a) and LLS50 now; LLS48 and LLS49 as they are

## As built: LLS47, LLS50 (2026-11-11, branch `llvm-ind2`, from mac/next)

**Built (LLS47 (a)): `mutants-oracle` in record mode compares the verdict only.** `ref-opt23.edn` keeps opt's first message and a digest
of its stderr, so a mutant naming a later message (`builtin-prefetch-5`'s `Intrinsic called with incompatible signature`, the attr-dup
order) read "differs" there and "same" live. The message check is the live mode's now (opt's whole stderr, `REPR_LLVM_JUDGE=live`, the
oracle tier's authority, and `verify-check`, which holds each own message to Arbace's verifier); record mode's message column is `-`.
No record changed. Measured (record mode, the pack's two lists): verify-rt 636 own, 636 as opt's (was 635); verify-rt-heldout 1,069 own,
1,068 as opt's (was 1,065): the four message gaps (`builtin-prefetch-5.win.intrinsic.265`, `builtin-prefetch-5.g0.intrinsic.500128`,
`rs.g0.attr-dup.600096`, `p1-first.lto.attr-dup.600342`) are "same" without a live opt (checked on these four: 4 own, 4 as opt's); the
one that differs is the stale line of `pr79354.g2.di-retarget.500483` (LLS46 (b); the pack's remake corrects it).

**Built (LLS50): the split-file sections' own verdicts.** `own_verdicts` (bin/repr-llvm) sends a module whose path is a section
(`.../split/DIR/TEST.ll/SECTION.ll`) to `section_verdict`, which reads the parent test's RUN lines (`LLVM_SRC` or the checkout's
extraction, `llvm-project-23.1.5.src/llvm/test/DIR/TEST.ll`). `valid`: some command (a pipeline's or a `;`/`&&` list's part) of opt, llc,
llvm-as or llvm-link names `%t/SECTION` as an input (not after `-o`; after a `cd %t` the bare name counts), and no command that names
it is under `not` or reads it with `--disable-verify`; those tools verify their input, so the test says the section is taken, as it
does for a whole file, and Arbace's verifier must meet it (valid or debug). FileCheck, cat, cp and other tools naming it change
nothing. Otherwise `-` (opt's record). Of the family's 60 split entries 53 are `valid` by their own verdict now (one was before): 52 more
own verdicts (verify-llvmtest: 758 of 806 by their own verdict, was 706; 48 opt's, was 100). The 7 left on opt's record are sections
no RUN line feeds to those tools as a file of its own (concatenated by `cat`, read by `llvm-lto2`, `llvm-diff`, `llvm-profdata`...),
the proposal's "57" counting every `valid` row. All 800 recorded passes are unchanged (0 failing, 0 fixed); the 4 verdicts that differ
are the same whole-file `invalid` rows as before (AMDGPU, dilocation-in-wrong-place), not sections.
Controls: `_control/section-vs-reject` (`Verifier-gcwrite-ptrptr` against the section `Analysis-CtxProfAnalysis-full-cycle.ll-m1`'s own
reference: the verdict differs) in `test/repr/llvm/verify-controls.txt`, met in the full family's run (4 controls fail as they must; an
essential run leaves it, the sections' parents being in the tarball); and `controls-test.bash`'s fake tree (a parent test and four
sections under `$w/fk`, `LLVM_SRC`): a and d valid, b under `not` and c an output only without an own verdict, and the crossed control
failing as it must. Checks: `bin/repr-llvm check verify-llvmtest` 806 modules, 800 pass; `--essential` of roundtrip-llvmtest (43),
verify-llvmtest (43) and verify-probes (4), 0 failing; expect-test 44 cases, 13 controls, 0 failing; controls-test all ok;
`bin/reap` 0 problems; `bin/crops-score --judge --strict` 0.

**Independence.** The judge stage 0.8455 to 0.8541 (2136 own, 880 recorded: independence 96.91 to 97.08; `test/crops/independence.edn`'s
`:llvm :judge`, `scores.edn`). Left: the 478 probes (LLS48 (a)) and llvmi's 177 modules (LLS49 (a)), as decided, and 48 llvm/test
modules on opt's record (41 whole files of split-file tests, 7 sections).

## As built: review 1111's fixes, finding 5 and 7 (2026-11-11, branch `review-fix-1`, from mac/next a7916bb2a)

The independent review of 2026-11-11 (`.tmp/reviews/1111/REVIEW-hs-rust-ml-scm-llvm.md`, findings 5 and 7). **Finding 5**: LLS50's
section rule gave `valid` to sections the tools may never verify, latent (none in the 147 parents of the pinned corpus). `section_verdict`
now leaves a section on opt's record (`-`) when the parent is `XFAIL`, when a RUN line naming it has `||` (its failure allowed; the `|`
split made the `opt %t/a.ll` part a command of its own), when llc starts after the IR verifier (`-start-after`, `-start-before`,
`-run-pass`, TargetPassConfig's addIRPasses holding the verifier) and when llvm-link may drop the unused (`--only-needed`,
`--internalize`: only the composite is verified); `--disable-verify` is matched in its `=VALUE` spellings too. The whole-file rule gains
`XFAIL` and the same spellings. `test/repr/llvm/controls-test.bash`'s fake tree adds a parent `D/u.ll` (sections e: `|| true`, f:
`--disable-verify=true`, g: `llc -start-after=codegenprepare`, h: `llvm-link --only-needed`) and an XFAIL parent `D/x.ll`: next's
`bin/repr-llvm` judged all five `valid` by their own verdict ("5 by their own verdict, 0 opt's"), the branch's none ("0 by their own
verdict, 5 opt's", failing for want of a record, as the case expects). **Finding 7**: `bin/llvmi rt-list`'s default record is
`check-roundtrip-rt-linux-amd64.edn` (fixes-1111's name; the verb ran again, 252 modules; the committed `rt-builds.tsv` left as made),
and `:llvm :read`'s evidence in `test/crops/independence.edn` names `check-verify-rt-linux-amd64.edn` (not scored).
Checks: llvm-read-essential's and llvm-print-essential's commands (43, 43, 4 and 43 modules, 0 failing), `bin/llvmi check --essential` 10 of
10, controls-test all ok (14 cases), `bin/reap` 0 problems, `bin/crops-score --judge --strict` 0, no record changed.

## As built: llvm-eh, llvmi runs modules that unwind (2026-11-13, branch `llvm-eh` from mac/cpp-s12 ff95fe3e3)

The user's decision of 2026-11-13: llvmi runs C++'s exceptions on Arbace's own engine (`invoke`, `landingpad` with cleanup, catch and filter
clauses, `resume`, the Itanium ABI as libsupc++ uses it, `__gxx_personality_v0`'s selection by type info, std::terminate, nested
exceptions, frames llvmi runs), in llvmi's model of the program, not by host unwinding; f8 and library.cpp (cpp-s12: generated, refused
by llvmi) first. This is LLS22 (a) (Proposed of LL4), built. Light, one process at a time, main's stage 0, a watcher killing by PID any
process of the worktree past 2.5 GiB; no gate, big lock, clang, g++ or gcc.

**What was there.** The shared lowering (`arbace/cpp/lower.ae`) already lowers exceptions by a pending flag (doc/cpp/LLVM.md §8, CP86 (c)):
a call that may unwind is tested after it, an invoke takes its unwind edge while the flag is set, a landing pad computes its selector by
asking the runtime of each clause (`cx$catches`, which does `__gxx_personality_v0`'s search for one clause through the type_info's own
`__do_catch`), `resume` raises again; the unwinder and the personality are the hand-written layer `arbace/cpp/rt/eh.c.ae`, and
everything else of the ABI is libsupc++'s own code, lowered with libstdc++ from the recorded IR pack (`bin/cppi runtime-build`,
`runtime-ci`: ci's library). cppi's check route runs clang's IR of C++ programs that way on ci. llvmi refused any such module
(`:personality`) because its single unit links musl and the personality-free layer (`arbace/llvm/rt/none.c.ae`) alone.

**Built.**
- `arbace.llvm.run`: a module that unwinds with C++'s personality (`__gxx_personality_v0`, or gcc's C cleanup personality) is lowered as
  C++'s check route lowers a program (`*ci*` false: the runtime it links is lowered that way, so their va_list and stack conventions
  agree), without the personality-free layer, and marked `:eh` (`lower`: `OUT.c.eh` beside; `lower-list`: class `eh` on an `ok` row); a
  module with another personality (Rust's) is still refused `:personality`, naming it.
- `bin/llvmi run`: an `:eh` module is linked by `arbace.cpp.run nodes` with C++'s lowered runtime (`bin/cppi runtime-ci`, built when stale;
  `LLVMI_CPP_RUNTIME` another runtime directory, `LLVMI_CPP_RTLIB` a read library used as it is, by hand) and musl's forms, and run by
  ci's back end (tools/cirun). `bin/llvmi check`: the `:eh` modules built by `arbace.cpp.run nodes-list` (eval) or `compile-list`
  (AOT, in shards of their own), the rest as before; so llvmi-check and llvmi-aot need the cpp-runtime build (`test/gate/steps.edn`:
  `:needs` cpp-runtime, inputs `:cpp`).
- The pending-flag layer, completed where the user's list asked more than it did:
  - **filter clauses' rejection** (a dynamic exception specification, the landing pad's selector -1, `__cxa_call_unexpected`): the lowering
    asks a filter's types through `cx$spec_catches`, which records them (reset at each landing pad, `cx$exception`); `__cxa_call_unexpected`
    is eh_personality.cc's: the exception caught, the unexpected handler called, its new exception goes on when the specification admits it,
    else a `std::bad_exception` when the specification admits that (libsupc++'s vtable and type_info), else std::terminate, the original's
    catch ended either way (before: it called the handler, then std::terminate even when the handler's exception was admitted);
  - **the adjusted pointer** starts as the thrown object at each landing pad (eh_personality.cc saves it for the handler it finds, a
    filter's rejection too): `__cxa_begin_catch` after a rejection read zero before (probe i1a's empty filter faulted).
- `bin/cppi runtime-ci`'s chunked read (`CPPI_CI_CHUNK`) writes the reader's key and the units' digests first, so a read stopped part way
  (a chunk killed by the watcher) resumes at the units not read instead of reading the 199 again.

**Probes** (`.tmp/probe/` of the worktree; each control exits by the bit it tests, so a throw caught by the wrong handler fails):

| Probe | What | Result |
|---|---|---|
| i1a (IR by hand) | a cleanup with a catch of `long` that an int passes (the cleanup runs, `resume`), clause order and `llvm.eh.typeid.for`'s selectors (`long`, `int`, catch-all), a filter that admits the exception (resumed to the caller), an empty filter's rejection (selector < 0, the thrown object read through `__cxa_begin_catch`) | llvmi 0 (after the adjusted pointer's fix; before: a fault at 0) |
| i1n | i1a with the catch-all before `int` in the caller: the int taken by the catch-all | llvmi 1 (bit 0) |
| i2a (IR by hand) | `throw(long)` rejecting an int, the unexpected handler throwing a long (admitted: caught as 77); `throw(long, std::bad_exception)` with the handler throwing an int (std::bad_exception in its place); `throw(long)` with an int from the handler (std::terminate, the program's terminate handler) | llvmi 0 |
| i2n | i2a without std::bad_exception in the second specification: std::terminate at its stage | llvmi 4 |

The C++ probes e1a-e4n (the ABI from C++ through Arbace's code generator: type selection, base and pointer conversions, multiple
inheritance, cleanups across frames in order, rethrow, nested handlers, constructors' members, std::exception's hierarchy, exception_ptr,
nested_exception, uncaught_exceptions, typeid, bad_cast, noexcept's std::terminate, an exception leaving main) are written with their
controls (`.tmp/probe/e*.cpp`) and not run: the night's df rule stopped the session at their generation (below).

**Programs that now run on llvmi** (Arbace's code generator's IR of cpp-s12, `bin/llvmi run`'s steps, judged by test/cpp/ref-gxx15.edn's
status and stdout; f8 by its own verdict, "3"): of the 22 of the 81 run programs whose IR unwinds (all refused by llvmi before), with f8
and library.cpp:

| Result | Programs |
|---|---|
| pass (16) | gxx-dg-run's cpp0x/initlist-throw2, eh/ctor-fntry1, eh/omit-frame-pointer2, eh/uncaught4, expr/for5, inherit/virtual15, init/array66, init/no-elide2, opt/dtor1, opt/unroll1, torture/pr40389, ubsan/vptr-1; runtime/deferred; samples/classes, io, p1-first |
| fail, cppi's own classes for clang's IR (3) | template/pretty1 (`__PRETTY_FUNCTION__` is clang's text, the test compares g++'s: test/cpp/check-every-25's `oracle-text`), runtime/thread-throw and runtime/threads (ci's threads, test/cpp/check-linux-amd64.edn's rows: status 139 there too) |
| not run: the link above 2.5 GiB (5) | f8 and library.cpp (their IR lowered: 41,366 and 45,920 lines; the link's read and check of their C passed 2.5 GiB), gxx-dg-run/pr77550, runtime/atomics, runtime/hello (the link of libstdc++'s iostream and string units); the night's command below |

Each small program: lowered in 13-21 s, linked in 43-64 s (io 221 s), run in about a second, 1.0-1.1 GB. Not run here: llvm-90's own
corpora's 13 modules refused `:personality` (ee.MCJIT.eh, eh-lg-pic; cpp.samples.classes, coroutine, io, p1-first; cpp.runtime.atomics,
ctor-throw, hello, library, thread-throw, threads, uncaught) through `bin/llvmi check`, and the checks after it (below).

**The runtime read.** `bin/cppi runtime-ci` read 198 of the 199 units here: `c++20/tzdb.cc` alone passed the watcher's 2.5 GiB (2.6 GB
before it was killed), so the library the runs above used leaves it out (`list-notzdb.tsv`, by hand; `LLVMI_CPP_RTLIB`), keyless: no
program here names its symbols. The gate's cpp-runtime step declares 3 GiB.

### The night's commands (not run: each above 2.5 GiB, or stopped by the disk at 86%)

```
# 1. the runtime whole, at the branch's key (about 25 min; tzdb.cc's read alone 2.6-3 GB)
CPPI_CI_CHUNK=4 CPPI_JOBS=2 bin/cppi runtime-build && CPPI_CI_CHUNK=1 bin/cppi runtime-ci
# 2. the five programs whose link passes 2.5 GiB (3-4 GB each, 5-15 min): .tmp/judge2.sh's steps, as bin/llvmi run
bin/llvmi run IR.ll   # for f8, library, pr77550, atomics, hello (the IR of cpp-s12's .tmp/probe/f8, .tmp/LIB, .tmp/G10)
# 3. llvmi's checks with the 13 modules that unwind, then record (eval about 5 min, AOT about 40 min, 1.5-3 GB)
bin/llvmi check --record && bin/llvmi check --aot --record && bin/reap --record && bin/crops-score --record
# 4. the C++ probes (each 1-4 min, 1.5 GB): .tmp/pg.sh e1a e1n e2a e2n e3a e3n e4a e4n
```

### Proposed (llvm-eh)

- **LLS51 The search phase.** The pending-flag model is one phase: an exception no handler takes runs the cleanups of the frames it leaves,
  then std::terminate (`cx$uncaught`), where libgcc's two phases terminate before any cleanup ([except.handle]/9 leaves it to the
  implementation; doc/cpp/LLVM.md §8 says so). A program printing in a destructor on that path differs from g++'s run (no recorded program
  does). (a) A search phase: each invoke whose landing pad has clauses registers them on a thread-local handler stack, and
  `_Unwind_RaiseException` asks it before raising (the lowering and the layer, the runtime rebuilt, about 1 day); (b) keep one phase, a class
  where a program shows it. **Recommended: (b).**
- **LLS52 The memory of a C++ program's link on ci.** f8's, library.cpp's and iostream's links pass 2.5 GiB (the program's C read and
  checked with libstdc++'s units it pulls). (a) Run them in the night lanes as cpp-check does (`CPPI_CI_MEMLIMIT=2GiB`, `:mem-gib` 3-5); (b)
  cut the link's memory first (the pruned units' checked forms kept per unit on disk). **Recommended: (a) now, (b) when cpp-check's runtime
  part grows.**
- **LLS53 llvmi-check needs cpp-runtime.** The branch makes llvmi's full steps depend on C++'s runtime build (`:needs`, `:cpp` inputs): a
  C++ change reruns them. (a) as built; (b) keep the `:eh` modules out of the gate's llvmi-check (a `--no-eh` there, cppi's check covering
  them). **Recommended: (a)**: the same modules are the row's.

### Keys

The branch changes `arbace/llvm/`, `arbace/cpp/`, `bin/llvmi`, `bin/cppi` and `test/gate/steps.edn` (llvmi's two steps), and doc/: crop paths
and the steps' table (see the report's key check).

### Checked (2026-11-14, after the disk came back to 82%; one at a time, peaks by busybox `time -v`)

| Run | Result |
|---|---|
| llvm-essential's commands: `bin/repr-llvm check roundtrip-llvmtest`, `verify-llvmtest`, `verify-probes`, `text-llvmtest` (`--essential`) | 43, 43, 4, 43 pass, 0 failing, their controls failing as they must; 0.17-0.28 GB |
| `bin/llvmi check --essential` | 10 of 10 pass (none unwinds: the musl-only route, unchanged), 4 controls judged, 0 pass; 0.38 GB |
| `test/repr/llvm/controls-test.bash` | all ok |
| cpp-print-essential's command | gxx-dg-essential 33 of 33, samples 9 of 9, `_control/bitfields-vs-io` failing as it must; 1.44 GB |
| `bin/cppi codegen-gen` | 155 of 155 as recorded; 1.17 GB, 9.5 min |
| `bin/reap` | 49 of 52 cells checked, 0 problems |
| `bin/crops-score --judge --strict` | exit 0 (no record changed) |

## Decided (the main session for the user's overnight rule, 2026-11-14): LLS51 (b), LLS52 (a), LLS53 (a)

One phase stays (a class where a program shows it); the links above 2.5 GiB run in the night lanes only when no gate runs and the main
session schedules them (the night's commands above); llvmi's full steps keep needing cpp-runtime.

## As built: h-judges, the judge's recorded rows audited (2026-11-17, branch `h-judges`)

Every row still judged by opt's or lli's record, and why (`test/crops/independence.edn`, LLVM's judge, the list):
- verify-llvmtest, measured by its run's `verify.tsv`: 756 by their own verdict and 50 by opt's record (the fact said 758 and 48).
  One more moved: Transforms/LowerTypeTests/cfi-icall-alias.ll's section `test` (split-file into `%t.dir`, CRLF lines, `cd
  %t.dir`, `opt test.ll`); `section_verdict` (bin/repr-llvm) takes split-file's directory as the RUN line names it and strips the
  CR. 757 and 49 now. The 49 have no own verdict for the module as the check reads it: whole split-file tests (the concatenation
  no RUN line feeds a tool), sections reached only through `cat`, target options, passes other than verify, XFAIL, llvm-diff.
  `bin/repr-llvm check verify-llvmtest`: 806 modules, 800 pass, 0 failing against the record; the new control
  `_control/dir-section-vs-reject` fails as it must; `test/repr/llvm/controls-test.bash` all ok.
- The probes' 478: LLS30 (opt's record kept).
- lli's 177 at each level: c-O0's and c-O2's 74 synth-c23 programs and 7 ci-run programs, the 14 C++ samples, all Arbace's own
  programs that print what they compute with no expected output; ee.MCJIT.2003-05-07-ArgumentTest (an argument after the file).
- verify-rt's 268: the pack's 252 modules (clang's IR of the torture programs) and 16 hw modules, no RUN line.
- verify-rt-heldout's 32: mutants of unknown kinds (a typed access's attachment 7, an intrinsic's bundle checks 5, musttail 4,
  personality 3, the DI kinds 6 of which 4 are the expectation's own IllegalArgumentException, others 1 each). Their verdict is the
  pack's first line: a rule fixed in `arbace.repr.llvm.expect` reaches them only with a re-made pack (night work).
Judge 0.8933 to 0.8932 (the count corrected by 2, one row moved): (4345 + 0.5 x 1181) / 5526.

## As built: the read and eval cells' cheap deferred groups (2026-11-17, branch `h-haskell-deferred` from next e4a7a30f3)

Light, one process at a time, no gate. The REAP cells' deferred rows taken by cause (read 17, eval 15, AOT 15):

- **Read: constant expression types** (`arbace/repr/llvm/verify.ae`'s `check-constexpr-type`). A `getelementptr` constant's own type
  is its base's pointer type (the address space kept, when neither the base nor an index is a vector), and a `blockaddress` of a
  function already defined is a pointer in that function's address space: both are held to the expected type as LLParser's
  ValID::t_Constant does ("constant expression type mismatch: got type ... but expected ..."). `parse-global` checks the initializer
  before the name's redefinition and forward reference, as LLParser::parseGlobal does (opt's first message on
  Bitcode/blockaddress-addrspace.ll is the initializer's), and `run-phases-parser` (the module the reader stopped in) binds the data
  layout's address spaces as `run-phases` does (its functions were in address space 0). layouts.g0.type.1122 (`:constant-type-rules`)
  and Bitcode-blockaddress-addrspace (`:llvmtest-messages`) pass: read 3,828 / 1 / 17 to 3,830 / 1 / 15.
- **Eval: the modules that unwind** (`:personality`, 13). llvm-eh (2026-11-13) runs them; the record still held the refusals.
  Recorded by `bin/llvmi check --only` the 13 with `--record`, which now rewrites a subset's own rows and keeps the others
  (`arbace/llvm/check.ae`, as `bin/hsi check --record --only`): 10 pass, ctor-throw and uncaught in `:abort-report`, thread-throw in
  `:cpp-threads`. runtime/threads.cpp timed out at llvmi's flat 10 s on the loaded host (17 s of CPU, 21 s of wall): `run_budget`
  gives a C++ program check-budget.edn does not list bin/cppi check's default, 30 s of CPU (CPPI_BUDGET). The C++ runtime was the
  next worktree's `bin/cppi runtime-ci` library (`LLVMI_CPP_RTLIB`, by hand; its key differs from this checkout's by the tools' host
  alone, the re-seed). Eval 537 / 10 / 15 to 547 / 12 / 3.
- **Left.** AOT's 13 `:personality` rows: the `:eh` modules' AOT build (cf2g with C++'s runtime) is above the light tier, the night's
  `bin/llvmi check --aot --only` them `--record`. `:dg-options` (2): the pack tx made again with each torture test's dg-options (clang,
  the oracle tier; LLS54 below). Read's 15: the Verifier's AMDGPU and debug-info rules, intrinsic tables, the probes' reader and metadata
  numbering (the classes' own :why).

### Proposed

- **LLS54 The torture pack's dg-options.** (a) `bin/llvmi ir-make tx` passes each test's `dg-options`/`dg-additional-options` (by hand,
  clang 23.1.5, the pack's pin moves; the other modules' IR byte for byte the same), tx-O0.restrict-1 and tx-O2.20040409-3w judged on
  their own program; (b) keep the class. **Recommended: (a)** at the next oracle-tier recording.

## Decided (the user, 2026-11-17, round 51): LLS54 (a)

## As built: LLS54 (a), the torture pack's dg-options (2026-11-17, branch `h-hes-lls`)

`bin/llvmi ir-make tx` compiles each torture test with its own `dg-options` and `dg-additional-options` after the level's (gcc's order:
an `-O2` of them wins over tx-O0's `-O0`): `tx_dg_options FILE` takes a directive without a target selector, or whose selector names
`x86_64-*-*` (not with `ia32`), or `fpic`, and of its options `-f*`, `-std=*`, `-O*`, `-D*`, `-m*` that clang accepts (`-mpc64` is
not), never `-W*` (the build is `-w`) nor `-Wl,*`; 39 of the 52 tests with a directive get options (`-fgnu89-inline` 14, `-fwrapv` 9,
...), listed per module in `test/llvm/ir/tx.cmd`. The pack made again by hand with the host's clang 23.1.5 (the oracle tier's), 3,040
modules as before: 61 modules' IR differs (all of them modules with options; 17 others, `-fno-common`, `-std=c99`, `-fno-ipa-cp` and the
like at one level or both, give the same IR), the other 2,979 differ from the old pack (clang 23.1.4) by `!llvm.ident`'s version alone,
compared with that line normalized. The pin moves (`tx.tar.xz.sha256`), every module's IR digest with it (lli's recorded runs in
ref-lli23.edn name the old digests: every torture module is judged by its own verdict, exit 0, so none reads them). Also fixed:
`tx.cmd`'s `corpora:` line named the first corpus alone. The records' rows of the five changed modules in the sample (tx-O0.20040409-3w,
tx-O0.pr68532, tx-O0.restrict-1, tx-O2.20040409-3w, tx-O2.restrict-1) re-run by `bin/llvmi check [--aot] --only ... --record`: all five
pass at both levels, so tx-O0.restrict-1 and tx-O2.20040409-3w left `:dg-options`, which is gone (no member); a run with
c-O0.run.ptr-sub-unsigned and tx-O0.20000121-1 judged the negative control, which fails as it must. `bin/repr-llvm heldout-check`
(its 21 tx bases from the pack, none of the 61) on the new pack: 900 mutants, 886 own verdicts, all met, 2 negative controls failing. Eval 547 / 12 / 3 to 549 / 12 / 1, AOT 537 / 10 / 15 to 539 / 10
/ 13. For the main host's whole night run: `LLVMI_TX_ALL=1 bin/llvmi check` and `--aot` judge the whole 3,040 on the new pack (not
recorded: the records are the sample's).

## As built: queue 335, the read cell's 15 deferred rows (2026-11-17, branch `335-llvm-verifier` from next d23b2215c)

Light, on the VM (arm64): each rule ported from llvm-project 23.1.5 (the pinned tarball's lib/IR/Verifier.cpp, VerifierAMDGPU.cpp,
ConvergenceVerifier.cpp, AsmWriter.cpp's slot tracker, lib/AsmParser/LLParser.cpp, AutoUpgrade.cpp's UpgradeTBAANode) as a commit
of its own, each checked against the VM's opt 23.1.5 live (the whole stderr, byte for byte) on the rows it is for, then the rows
re-run against the records by `bin/repr-llvm check FAMILY --only ID,...` (new: the named modules with the family's negative
controls' modules, judged against the record; `--record` rewrites the named rows alone) and re-recorded so. The 15 rows and what
took them:

- **AMDGPU's intrinsics** (verifyAMDGPUIntrinsicCall, whole): `llvm.amdgcn.call.whole.wave` with visitFunction's
  AMDGPU_Gfx_WholeWave and isCallableCC (amdgpu_ls, amdgpu_gfx_whole_wave), the twelve load-to-LDS data sizes, mfma.scale's and
  wmma f8f6f4's formats and operands; also cs.chain, init.exec.from.input, set.inactive.chain.arg, amdgcn.kill's callbr
  (visitCallBrInst: a callbr of a function is no visitCallBase), s.prefetch.data's message without a culprit, av.load/store's last
  argument a node of one string (six valid CodeGen/AMDGPU modules were rejected), and amdgpu_es out of visitFunction's shader
  conventions. A remangled declaration is visited at the module's end (getOrInsertDeclaration appends it); a call's undefined
  attribute group prints none. Verifier-AMDGPU-intrinsic-amdgcn-call-whole-wave, -intrinsic-load-to-lds, -mfma-scale
  (`:llvmtest-rules`); Verifier/AMDGPU's 15 files all print opt's stderr but intrinsic-immarg (half constants and `i1 1` in the
  printer, below).
- **Debug info**: visitMDNode's AreDebugLocsAllowed (a DILocation under an attachment other than !dbg and !llvm.loop), opt's
  second verification numbered on the stripped module (Verifier-dilocation-in-wrong-place, `:llvmtest-rules`); visitDICompileUnit's
  global variables list (a DIGlobalVariableExpression, its variable's scope no local scope; a DILocation as the variable is
  `invalid tag`, after the unit and at visitGlobalVariable's direct call, which visits a DIGlobalVariable again: V:1742)
  (auto.g2.di-retarget.500565, `:debug-info-rules`).
- **Intrinsics**: is.fpclass's and vp.is.fpclass's test mask (alias-access-path-2.g0.intr-rename.600557: the Verifier's unchecked
  cast of another operand reads bits outside the mask on the pin, as its record shows); a declaration with other suffixes than its
  type's is remangled (pr57344-4.win.intr-decl.500228); the convergence verifier's messages numbered by Value::print's slot tracker,
  the module's metadata and the printed function's alone (pr79354.g2.bundle.500227). The intrinsic table regenerated from 23.1.5 by
  `bin/llvm-corpus intrinsics`: every line as 23.1.4's.
- **The parser and the probes**: skipModuleSummaryEntry's end of file (Assembler-thinlto-bad-summary3, `:llvmtest-messages`);
  parseMemoryAttr's errors and parseFnAttributeValuePairs's words in a header (attrs-p87, attrs-p52); a label defined twice is one
  block moved to the function's end (defineBB), pred_empty counting a terminator anywhere, `Terminator found in the middle` printing
  the block (verify-lab3); MDTuple::get's uniquing of equal tuples and UpgradeTBAANode's tags (metadata-w68, metadata-w77).
  test/llvm/probes-known.tsv keeps cc-s39 alone (an address in opt's message, `:probe-pointer`); the classes `:probe-label`,
  `:probe-reader` and `:probe-metadata` are gone.
- **Left**: pr79354.g2.di-retarget.500483 (`:debug-info-rules`): Arbace's verifier now prints opt's stderr whole (three `invalid
  file`), but the pack's mutant line says `invalid` and opt `debug` (LLS46 (b)): the row passes when the night re-makes the pack
  (`bin/llvmi ir-make rt --mutants`).

Also fixed on the way: arbace.repr.llvm.diag took the lexer's Toks by its class, and a read.ae evaluated from the checkout (any
change to it) made every parser error's caret a crash; it reads the arrays through `rd/token-arrays` now.

Checks (live opt 23.1.5 against Arbace's verify, A/B against next d23b2215c, whole stderr byte for byte; the VM's light tier, no
whole family run): the 478 probes 472 to 477 (cc-s39 left), 475 to 478 verdicts; Assembler/ and Verifier/ (931 files) 813 to 827,
882 to 889 verdicts, 592 to 601 first messages, no file worse; the pack rt's 715 mutants of the kinds touched 690 to 699, 714 to
715 verdicts; its 252 modules, DebugInfo/ and TypeBasedAliasAnalysis/ (78), 150 FunctionAttrs and Attributor modules with
`memory(`: unchanged (all of the pack's and the 150 equal before and after); llvm/test's 33 modules with summary entries 31 to 32;
the AMDGPU slices (55 and 72 files) 46 to 51 and 57 to 70. `check verify-llvmtest --only` (5 rows), `verify-probes --only` (5),
`verify-rt-heldout --only` (5): 14 fixed, 0 failing, their families' negative controls failing (5, 2, 1); the essential subsets of
verify-llvmtest, verify-probes, roundtrip-llvmtest and text-llvmtest pass as recorded; expect-test 44 cases, 13 controls. The
14 rows re-recorded by `--only --record`; `bin/reap --record`: LLVM read 3,830 / 1 / 15 to 3,844 / 1 / 1, the classes
`:probe-label`, `:probe-reader` and `:probe-metadata` gone and `:llvmtest-rules`' and `:llvmtest-messages`' lists without the
fixed tests; `bin/crops-score --record`: the LLVM row's completeness 0.9829 to 0.9839, meant 0.9927 to 0.9936.

Not taken, seen on the way (no row of the cell): the printer's half constants (`half 1.0`, opt `half 1.000000e+00`: LLVM 23's
writeAPFloatInternal prints every type's decimal when it reads back) and an i1 written `1` (opt `true`), in
Verifier/AMDGPU/intrinsic-immarg.ll's stderr; CodeGen/AMDGPU's wmma gfx1250 tests (llvm.amdgcn.wmma.*.gfx1250.w32, three) rejected
as `intrinsic has incorrect number of args. Expected 8, but got 7` where opt upgrades the old form (AutoUpgrade); Verifier/callbr.ll's
callbr.landingpad rules; a lifetime intrinsic's failure ending the call's visit before the convergence verifier's message (a
module of the VM's, opt prints both).

## As built: round 52, the pack's own verdicts made current (2026-11-17/18, branch `n-llvm-defer` from next 42bf8614b)

The night's rule: no build, no corpus run, each process under 1 GiB (the full gate on the host). What keeps LLVM IR's scores below
100 (test/crops/scores.edn at 42bf8614b: completeness 98, meant 99, independence 98):
- **Deferred rows**: read 1 (pr79354.g2.di-retarget.500483, `:debug-info-rules`), eval 1 (cpp.runtime.thread-throw, `:cpp-threads`: ci's
  abort off the main thread, C's crop's runtime, the same row as bin/cppi check's runtime/thread-throw.cpp), AOT 13 (`:personality`: the
  modules that unwind, recorded as refusals before llvm-eh; their AOT build, cf2g with C++'s lowered runtime, is 1.5-3 GB). No pending row.
- **Independence**: the judge stage alone (0.893; read, eval, AOT and print 1): 1,181 cases on a recorded reference, the probes' 478
  (LLS30), lli's 177 at each level (Arbace's own programs with no expected output), verify-rt's 268 (the pack's 252 clang modules and
  the hand-written ones with no RUN line), verify-rt-heldout's 32 mutants of unknown kinds, verify-llvmtest's 49.

**What was found.** Each mutant's own verdict is the first line the mutator wrote into the pack (`; MUTANT: ...`), and the pack
(55f819e2e, 2026-11-11 07:19) predates LLS46 (b2a5988b2, StripDebugInfo and the loop ids in `arbace.repr.llvm.expect`): the read
cell's deferred row is such a stale line (the pack says `invalid`, the current expectation `debug`, as opt's record). Measured by
regenerating every mutant of the pack from its base, kind and index (arbace.repr.llvm.mutate's own loop, `java.util.Random` of 1000
x K + J, the printed text compared with the pack's) and comparing the current expectation's line with the pack's: of the pack's 1,750
mutants, 1,697 regenerated (rs.g0's 53 left: its 2.2 MB base above the night's 1 GiB), every one's text the pack's; before this
round's fixes one line differs (the deferred row), after them 21: the deferred row (`invalid` to `debug`) and 20 `unknown` to an own
verdict, 9 of verify-rt (sets a, b) and 11 of verify-rt-heldout (h1, h2). Each of the 21 equals opt's recorded verdict and first
message (test/llvm/ref-opt23.edn); every other line is unchanged. Peaks 0.88-0.94 GB (GOMEMLIMIT 650MiB).

**Built** (arbace/repr/llvm/expect.ae):
- `body-md`: an instruction's attachments read after its `:md`. `stripped-reachable` and `strip-modifies?` paired `opts-of (drop 2 b)`,
  which takes the getelementptr's flags written before its type (`inbounds nuw`) as keys: IllegalArgumentException on such a GEP,
  so four debug mutants (coroutine.g0.di-retarget.500278, 20020418-1.g0.di-retarget.500524, io.g0.di-retarget.600072,
  io.g0.di-drop.600358) were `unknown: the expectation failed`; now debug, each with opt's first message.
- `ex-type`: a load's or store's unknown (an atomic access, an attachment) is the Verifier's, so an LLParser error the function's
  simulation finds comes first (`::verifier`): 15 mutants `unknown: an attachment on the access` are LLParser's `'%N' defined with
  type ...` or `instruction forward referenced with type ...`, as opt's record.
- `const-error`: LLParser's conversion of a constant to a scalar type (P:6908 convertValIDToValue's `null must be a pointer type`,
  P:1874/P:1904 getGlobalVal's `global variable reference must have pointer type`) before ret's and a call argument's other checks:
  20011113-1.lto.type.255, 20011113-1.win.type.1309, p1-first.g2.type.500487.
- Unit cases (test/repr/llvm/expect_test.ae): a GEP with `inbounds nuw` under StripDebugInfo's walk, the attachment case with and
  without a use, ret of null and of 0, a call with a global as i64; two new negative controls. `test/repr/llvm/expect-test.bash`:
  50 cases, 15 controls, 0 failing.

Every changed line was checked against opt's recorded verdict and first message (test/llvm/ref-opt23.edn): all equal.

**Not committed: the hand-written modules' RUN lines.** test/llvm/hw/{dbg,implicit,unnamed}.ll have no RUN line, so verify-rt
judges them on opt's record (hw-notes has one). `; RUN: opt -passes=verify -disable-output %s` after each first line gives them an
own verdict (`bin/repr-llvm verify-check` on the four: 4 by their own verdict, all valid as Arbace's verifier says), but a hw file
is a mutant's base in the pack's manifest, so the edit makes the pack stale (`rt_pack`) until `ir-make rt --mutants`: it goes in with
the re-make (LLS56 below).

### Decided (taken overnight, round 52)

- **LLS55 The pack's stale own verdicts.** (a) `bin/llvmi ir-make rt --mutants` (no compiler: the pack's modules kept, the mutants
  and their lines made again by the current mutator and expectation; the pin moves), then verify-rt and verify-rt-heldout recorded
  whole; (b) keep the pack until the next clang re-make. **Recommended (a); taken overnight (round 52)**, its run by day (the mutator
  in one process is above the night's 1 GiB).
- **LLS56 The hand-written modules' own verdict.** (a) a plain `opt -passes=verify` RUN line in hw/dbg.ll, implicit.ll and unnamed.ll,
  committed with LLS55's re-make; (b) keep opt's record for them. **Recommended (a); taken overnight (round 52).**

### The day's commands (main host, in this order; none run here)

```
# 1. LLS56's lines, then LLS55's re-make (the mutator, about 1.5-2 GB) and the two families recorded whole (light, about 0.3 GB)
for f in dbg implicit unnamed; do sed -i '1a ; RUN: opt -passes=verify -disable-output %s' test/llvm/hw/$f.ll; done
bin/llvmi ir-make rt --mutants
bin/repr-llvm check verify-rt --record && bin/repr-llvm check verify-rt-heldout --record
bin/repr-llvm check roundtrip-rt && bin/repr-llvm check text-rt && bin/repr-llvm heldout-check
# 2. AOT's 13 modules that unwind (1.5-3 GB each build; eval recorded them on 2026-11-17)
printf '%s\n' ee.MCJIT.eh ee.MCJIT.eh-lg-pic cpp.samples.classes cpp.samples.coroutine cpp.samples.io cpp.samples.p1-first \
  cpp.runtime.atomics cpp.runtime.ctor-throw cpp.runtime.hello cpp.runtime.library cpp.runtime.thread-throw cpp.runtime.threads \
  cpp.runtime.uncaught > .tmp/aot-eh.txt
bin/llvmi check --aot --only .tmp/aot-eh.txt --record
# 3. the row's records
bin/reap --record && bin/crops-score --record && bin/crops-score --judge --strict
```

**Projected** (after the day's commands; no record changed here): read 3,844 / 1 / 1 to 3,845 / 1 / 0; AOT 539 / 10 / 13 to 549 / 12 / 1
when the 13 run as at eval (10 pass, ctor-throw and uncaught `:abort-report`, thread-throw `:cpp-threads`); completeness 0.9839 to
0.9884 (98 to 99), meant 0.9936 to 0.9991 (99 to 100); the judge's cases 4,345 own and 1,181 recorded to 4,368 and 1,158 (the 20
mutants and three hw modules), (4368 + 0.5 x 1158) / 5526 = 0.8952, independence 0.9786 to 0.9790 (98). What is left of the judge:
the probes (LLS30), lli's 177 a level, the pack's 252 clang modules, verify-llvmtest's 49, 21 unknown mutants of verify-rt-heldout
(an intrinsic's bundle checks 5, musttail 4, personality 3, others 1 each) and 4 of verify-rt; independence 99 needs about
350 more cases on an own verdict (lli's 354, Arbace's synth-c23 and C++ samples with expected outputs, would be the one group that size).

## As built: queue 358, the last points (2026-11-18, branch `358-go-llvm` from mac/n-all 9cd2e4a96)

The VM's light tier (stage 0 of next's seed, each process under 3.2 GiB, a watcher killing by PID past it), round 52's day's
commands run, and two fixes found on the way.
- **LLS55 (a), LLS56 (a).** `bin/llvmi ir-make rt --mutants` (23 min, 0.77 GB): the 252 modules and 1,750 mutants of the pack,
  every mutant's text the old pack's but the 21 first lines round 52 named (pr79354.g2.di-retarget.500483 `invalid` to `debug`;
  15 `unknown: an attachment on the access` to LLParser's type errors, 4 di-retarget/di-drop expectation failures to `debug`,
  2 constants and a global to LLParser's scalar errors); the manifest's other 13 lines the hw bases' digests, their mutants'
  IR unchanged. hw/dbg.ll, implicit.ll and unnamed.ll take `; RUN: opt -passes=verify -disable-output %s`. `check verify-rt
  --only` (9 mutants, the three hw) and `verify-rt-heldout --only` (12): all pass, all by their own verdict, pr79354 fixed, one
  control failing in each; roundtrip-llvmtest and text-llvmtest on the three hw: pass. Read 3,844 / 1 / 1 to 3,845 / 1 / 0.
- **RUN lines' arguments.** `own-verdict` takes the plain words after the file (`%lli %s test`, the same on every RUN line) as
  `:args`, `run-args` writes them to WORK/args.tsv and bin/llvmi's runs pass them (`run_args`): ee.MCJIT.2003-05-07-ArgumentTest
  passes at eval and AOT by its own verdict (it puts argv[1]; it faulted with no argument, lli too: `:crash-report`, removed).
  `LLVMI_JUDGE=lli` gives back the old row (the control). No other module has arguments after its file.
- **AOT's modules that unwind.** bin/llvmi took a built program's package from the shard list's sixth column, which an llvm-eh
  list (`ID LOWERED.C EXTRA-LIB pN`) has not: no eh shard ever built (`"arbace/progs/": trailing slash`); the last column now.
  With C++'s lowered runtime (`bin/cppi runtime-build`, then `runtime-ci` at `CPPI_CI_CHUNK=1` for the last 13 units: c++20/tzdb.cc's
  chunk passed 3.3 GB at 4 a process; peak 2.9 GB), `bin/llvmi check --aot --only` of the 13 (16 min, peak 3.4 GB for the Go build
  of a shard: the watcher killed library's package compile at 3.3 GB): 9 pass; ctor-throw, uncaught and thread-throw exit 134 with
  g++'s stderr byte for byte (test/cpp/ref-gxx15.edn), lli adding its stack dump: `:abort-report`, thread-throw's at AOT
  `:thread-abort-report` (divergences-aot.edn; at eval ci's abort off the main thread still faults, `:cpp-threads`);
  cpp.runtime.library is test/llvm/aot-heavy.txt's now (its row kept for `LLVMI_AOT_HEAVY=1`). AOT 539 / 10 / 13 to 549 / 12 / 1.
- **Scores** (bin/crops-score --record): completeness 0.9839 to 0.9888 (98 to 99), meant 0.9936 to 0.9991 (99 to 100), maturity 1,
  independence 0.9786 to 0.9790 (98): the judge's cases 4,370 own and 1,156 recorded (the 20 mutants, the three hw, ArgumentTest at
  two levels), 0.8954.

What is left, each a row with its cause: read `:probe-pointer` (cc-s39: an address in opt's message, by design); eval `:cpp-threads`
(thread-throw: ci's abort off the main thread, C's crop's runtime, deferred); AOT `:personality` (library: its build above the light
tier, the night's `LLVMI_AOT_HEAVY=1`); by design at eval and AOT: `:long-double` 3 (ci's binary64 long double, CS35), `:ub-refinement`
2, `:abort-report` 2 (+1 at AOT), `:envp` (ci's empty environment), `:lli-reference` (a module for lli's interpreter's va_list),
`:datalayout`, `:target-type`.

**Round 2 (D358-5 (a), the user, 2026-11-18).** A cpp corpus module whose lli record and g++ 15's (test/cpp/ref-gxx15.edn) agree on
status 134 and stdout is judged against g++'s status, stdout and stderr (`arbace.llvm.check/gxx-reference`): lli's signal handler's
stack dump is not the module's meaning. ctor-throw, uncaught and thread-throw pass at eval and AOT (thread-throw exits 134 at eval
now, three runs on this tree's lowered runtime: ci's abort off the main thread no longer faults); the control (ctor-throw's run
against thread-throw's g++ run: same status and stdout, another stderr) fails as it must; `:abort-report`, `:thread-abort-report` and
`:cpp-threads` removed. Eval 550 / 11 / 1 to 553 / 9 / 0, AOT 549 / 12 / 1 to 552 / 9 / 1; completeness 0.9888 to 0.9915 (99), meant
0.9991 to 0.9995 (100); the judge stays a recorded one (0.5): independence 0.9790.

## As built: n-llvm-indep, what holds the judge and the cells (2026-11-18, branch `n-llvm-indep` from mac/358-go-llvm 9769b6772)

Stage 0 of the main checkout (`ARBACE_HOST_EXE`), each process under 3 GiB (peaks 1.4-1.8 GB), no gate, no LLVM tool.

**What holds independence (0.9790).** Read, eval, AOT and print are 1 (independence.edn's facts cross-checked with the steps:
llvm-essential, llvm-read, llvm-print, llvmi-check, llvmi-aot run on `:stage0` and `:go` alone). The judge alone is below 1, its
1,156 recorded cases measured from the runs (`bin/repr-llvm check verify-rt`, `verify-rt-heldout`, `verify-llvmtest`: WORK/run/verify.tsv's
eighth column; test/llvm/check-eval-linux-amd64.edn's ids): the probes 478 (opt's record, LLS30, LLS48 (a)); llvmi's 176 at each level
(c-O0 and c-O2's 74 synth-c23 and 7 test/c/run programs, the 14 C++ samples and runtime programs: Arbace's own programs with no
expected output, lli's record, LLS49 (a)); verify-rt's 256 (the pack's 252 modules, IR made by clang, rustc and llvm-stress with no RUN
line, and 4 mutants); verify-llvmtest's 49 (whole split-file tests, tool options, XFAIL); verify-rt-heldout's 21 mutants of unknown kinds.

**Built: the heldout set's unknowns that a rule reaches** (arbace/repr/llvm/expect.ae; Verifier.cpp of llvm-project 23.1.5):
- `ex-personality`: a personality dropped from a function no instruction of which needs one (no landingpad, resume, catchswitch,
  catchpad, cleanuppad) is valid: the Verifier asks nothing more of it (was `unknown: no exception handling instruction`).
- `ex-bundle`: visitIntrinsicCall runs before visitCallBase's bundle loop, and only some intrinsics' own checks read the bundles
  (assume's tags, deoptimize's and guard's deopt, the statepoint's, preallocated's, the convergence intrinsics', ARC's, coroutines':
  `bundle-intrinsics`, left `unknown`); any other intrinsic's checks held before the mutator added the bundles, so the loop's checks
  give the verdict (was `unknown: an intrinsic's own bundle checks` for every intrinsic).
- `ex-musttail`: getParameterABIAttributes ported (`abi-key`: StructRet, ByVal, InAlloca, InReg, StackAlignment, SwiftSelf, SwiftAsync,
  SwiftError, Preallocated, ByRef, and align beside byval or byref), each of the caller's parameters against the call's,
  `mismatched ABI impacting function attributes` (was `unknown: ABI attributes to compare` whenever any such attribute appeared);
  a callee of any spelling checked by the call's own type, inline asm first (was `unknown: the callee is no name` for a quoted name).
- Unit cases (test/repr/llvm/expect_test.ae): musttail with the same, a missing byval, a differing align beside byval, a quoted callee;
  an intrinsic's deopt pair, an unknown tag, assume's (left unknown); a personality with no pad, and with resume; three new negative
  controls. `test/repr/llvm/expect-test.bash`: 59 cases, 18 controls, 0 failing.
- `bin/llvmi ir-make rt --mutants` (33 min, 1.48 GB): the 252 modules and 1,750 mutants, every mutant's text the old pack's; 12 first
  lines changed (verify-rt-heldout's sets h1, h2: personality 3, bundle 5, musttail 4), each equal to opt's recorded verdict and first
  message (test/llvm/ref-opt23.edn). verify-rt and verify-rt-heldout recorded whole; heldout-check run.

**Scores** (bin/crops-score --record): the judge 4,382 own and 1,144 recorded, (4382 + 0.5 x 1144) / 5526 = 0.8965 (the score's stage 0.8960), independence 0.9790 to 0.9792 (98); bin/reap --record: the matrix unchanged (every record's rows the same); --judge --strict and bin/records-check: 0 problems.
Completeness unchanged (0.9915): its non-passes are by design but one (eval 9, AOT 9: `:long-double` 3, `:ub-refinement` 2, `:envp`,
`:lli-reference`, `:datalayout`, `:target-type`; read 1, `:probe-pointer`) and AOT's `:personality` (cpp.runtime.library, whose Go
package's build is above 3 GiB, aot-heavy.txt's).

**Left, each with its cause**: verify-rt-heldout's 9 (attribute pairs and values, immarg, an intrinsic's declaration and rename, three
debug-info fields, an attachment on a typed access: each a further port of the Verifier's or AutoUpgrade's rule); verify-rt's 4 mutants;
verify-llvmtest's 49 (a whole split-file test has no one verdict; the rest need lit's tool options read); the three large groups below.

### Proposed (n-llvm-indep)

- **LLS57 The pack's 252 modules in verify-rt.** They are roundtrip-rt's and text-rt's modules too, judged there by their own levels
  (1); in verify-rt they are judged against opt's record of `valid` (0.5). (a) The pack's maker writes `; RUN: opt -passes=verify
  -disable-output %s` into each module it compiled (a producer's output claimed valid: the claim is clang's, rustc's and
  llvm-stress's, so it changes what counts as own); (b) count each module once, by its round trip, verify-rt's 252 rows out of the
  judge's cases (the cell keeps them); (c) keep the record. Gain of (a) or (b): independence 97.9 to about 98.4.
- **LLS49 again (c), with C's and C++'s decision**: synth-c23's table gets an `:expect` beside each `:show`, written from C23's rules
  (bin/ci synth writing NAME.expect; C's selfref and llvmi's own-verdict judging stdout against it, tcc-tests2's convention), and the 7
  test/c/run programs and the 14 C++ programs a `ci-run verdict` self-check (the c pack then re-made by clang, by hand). Gain: 352
  cases, independence to about 98.6 (and C's and C++'s synth and samples with it).
- **LLS58 A fixed environment for llvmi's runs** (completeness): ee.MCJIT.2003-08-21-EnvironmentTest reads envp[0], ci starts every
  program with an empty environment. A fixed, host-independent one (`LIT=1`, say) keeps runs reproducible and passes it at eval and AOT:
  completeness 0.9915 to 0.9924. It changes C's runtime's design (go/arbace/ci's RunMain), so it is C's crop's decision too.

## Decided (the user, 2026-11-18): LLS57 (b), LLS49 (c)

- **LLS57 (b):** the pack's 252 modules counted once, by their own-judged round trip (roundtrip-rt, text-rt): verify-rt's rows of
  them stay in the read cell, out of the judge's cases.
- **LLS49 (c):** synth-c23's `:expect` worked out by hand, test/c/run's programs checking themselves, the C++ programs' outcomes,
  each with a negative control; C's side as built on mac/n-c-last 31edae682 (test/c/selfref-derived.edn, test/c/selfcheck-mutants.tsv),
  its form reused.

## As built: LLS57 (b), LLS49 (c) (2026-11-18, branch `n-llvm-indep`)

- **synth-c23's `:expect`** (test/c/synth/table.edn, test/c/synth/gen.ae): each row's values in `:show`'s order, worked out by hand
  from ISO/IEC 9899:2024 and the target (four parallel derivations of the 79 programs, 726 values, no program compiled or run, no
  record read); `bin/ci synth` writes ID.expect (the whole stdout, "EXPR = VALUE" a line) beside ID.c, `--check` compares them; the
  .c files unchanged (the pack's sources). Checked afterwards against lli's recorded stdout digests (test/llvm/ref-lli23.edn) of the
  74 programs in the pack c: 73 equal; pp-has-attribute's nine standard attributes were derived as 202311L, and C23's table
  (6.7.13.1: deprecated, fallthrough, maybe_unused 201904L, nodiscard 202003L, noreturn 202202L, unsequenced, reproducible 202207L)
  makes each `== 202311L` false: corrected from the standard, then equal. The five programs clang refuses (no module) are unchecked.
- **test/c/run's programs**: the seven of the pack, as mac/n-c-last made them (each `/* ci-run verdict: exit N.` and its own
  checks; C's mutants test/c/selfcheck-mutants.tsv prove they fail when a value is wrong), copied byte for byte.
  `bin/llvmi ir-make c --stale` (new: only the modules whose checkout source changed compiled again by the corpus' command, clang
  23.1.5; the others' IR the pack's; c.cmd's `refreshed` line names them): 14 modules.
- **C++'s 14 programs**: test/llvm/selfref-derived.edn (test/c/selfref-derived.edn's form): the status, the exact stdout and, for the
  three aborts, std::terminate's stderr, worked out from each source (each with its `:why`); checked afterwards: all 14 equal lli's
  recorded digests, the aborts' stderr g++'s (test/cpp/ref-gxx15.edn). The sources are not given self-check lines, unlike the
  decision's wording: test/cpp/samples are the pack rt's mutant bases (a change makes the pack stale and its mutants' records with it)
  and C++'s crop's own records (ref-gxx15, check, lower, round trip) read them.
- **The judge** (arbace/llvm/check.ae's own-verdict): c-O0/c-O2.synth.N by test/c/synth/N.expect (exit 0, stdout's digest),
  c-O0/c-O2.run.N by the source's first line's status, cpp.N by test/llvm/selfref-derived.edn (status, stdout, stderr when given).
  Negative controls (test/llvm/controls.edn): expr-arithmetic's run against expr-cast's .expect, ptr-sub-narrow's against
  ptr-sub-unsigned's `exit 3`, io's against threads' derived stdout, uncaught's against ctor-throw's derived outcome: each fails.
- **Two lowering gaps the self-checking programs found** (arbace/cpp/lower.ae): libatomic's `__atomic_is_lock_free` (clang's call for
  a size known at run time) is called as gcc's builtin of the same signature, which ci answers (`builtin-externals`), not as an
  undefined external; llvm.experimental.cttz.elts (builtin-stdc at -O2) lowered as the first true lane's index, else the lane count.
- **Runs** (stage 0, peaks 1.7-2.6 GB): `bin/llvmi check --corpus c-O0,c-O2 --record` 162 of 162, all by their own verdicts
  (nan-to-unsigned's self-check passes: `:ub-refinement` removed); `--aot` the same, 162 of 162 (lib-musl-tables at both levels is
  aot-heavy.txt's, its rows kept); C++'s lowered runtime built here (`bin/cppi runtime-build`, 198 units; `runtime-ci` at
  CPPI_CI_CHUNK=1, the index under CPPI_CI_MEMLIMIT=2300MiB: the default's 6 GiB limit let it pass 3 GiB, stopped by the watcher);
  `--corpus cpp` 14 of 14 by their own verdicts.
- **Scores** (bin/reap --record, bin/crops-score --record): eval 553 / 9 to 555 / 7, AOT 552 / 9 / 1 to 554 / 7 / 1 (nan-to-unsigned
  at both levels); the judge's cases 4,731 own and 543 recorded ((4731 + 0.5 x 543) / 5274 = 0.9485, the score's stage 0.9490):
  independence 0.9792 to 0.9898 (99), completeness 0.9915 to 0.9933 (99), meant 0.9995. `bin/llvmi check` over all 562 modules: every
  one by its own verdict, 555 pass, the 7 by design; `--judge --strict`, records-check, expect-test, controls-test, `bin/ci synth
  --check` and `bin/cppi lower-essential` (6 of 6, the lowering's change): 0 problems. What stays recorded: the probes 478 (LLS30),
  verify-llvmtest's 49, verify-rt-heldout's 9, verify-rt's 4 mutants, AOT's 3 aot-heavy modules. C's crop can read the same
  test/c/synth/*.expect (its selfref, on mac/n-c-last's line): not wired here, to keep that file's merge to one side.

## As built: llvm-judge-1120, llvmi's last lli-judged modules by their IR's reviewed outcome (2026-11-20, branch `llvm-judge-1120` from next 939037429)

Stage 0 of the main checkout (`ARBACE_HOST_EXE`), no gate (the user), no LLVM tool, nothing run but one verify-check of 7 modules
(0.9 GB) and no module run by llvmi.

**The recorded cases, measured** (the judge's 547 of 5,274 at next): `arbace.llvm.check/own-verdict` evaluated over llvmi's 562
modules (`bin/llvmi modules`, the packs) and `bin/repr-llvm`'s `own_verdicts` over verify-llvmtest's 806 (the four lists resolved, no
verify run). llvmi's "177 at each level" are history: LLS49 (c) moved 176 to their own outcomes and queue 358 the ee module; what
lli's record still judged at eval was c-O0's and c-O2's `.synth.pp-has-attribute` alone (n-hasattr left them to it), at AOT the same
two and aot-heavy.txt's 3 kept rows. So: the probes 478 (LLS30), verify-llvmtest's 49, verify-rt-heldout's 9 and verify-rt's 4 mutants
of unknown kinds, llvmi's 2 x 2, AOT's 3.

**Built: an outcome worked out from the module's IR** (`test/llvm/selfref-derived.edn`, `:from :ir`). The two pp-has-attribute
modules are clang 23.1.4's IR of `test/c/synth/pp-has-attribute.c`, whose preprocessor answered `__has_c_attribute` with the C2x
drafts' dates: each `== 202311L` of a0-a8 was false, so the IR holds 0 there, while the C source's `.expect` (C23's 202311, n-hasattr)
says 1. The module is the test, so its outcome is the IR's, read instruction by instruction: at -O0 (optnone) main makes 13 calls
`printf(@.str = "%s = %d\0A", @.str.N, load @aK)` in the order a0 ... a11, in_if, the globals internal and never stored (main's one
store is to its retval alloca), so each load reads its initializer (@a0-@a8 `i32 0`, @a9 @a10 @a11 @in_if `i32 1`); at -O2 the same 13
tail calls with the folded constants; `ret i32 0`; nothing on stderr: 96 bytes. Each entry carries a `:review` row a printed line
(the line, its basis in the IR) and `:ir`, the reviewed module's SHA-256 (the pack's manifest, `test/llvm/ir/c.tsv`): a pack made again
by another clang has no outcome from it, and lli's record judges it until it is reviewed again. `arbace.llvm.check`'s own verdict reads
any module's entry first (`derived-verdict`, `entry-verdict`; the C++ programs' entries as before, `:from` absent: their source).
The golden file's way, Java's JCS361 (a) and Haskell's queue 325g, within LLS49 (c)'s form (the user, 2026-11-18): written from the
IR, then held to the record.

**Built: `bin/llvmi derived-check`** (`make llvmi-derived-check`; `arbace.llvm.check/derived-check`): every reviewed outcome judged by
the check's own judge (`own-result`) against the module's recorded run, `test/llvm/ref-lli23.edn` (an abort's stderr g++'s,
`gxx-reference`, D358-5 (a)), nothing run, as Java's golden files are held to the JDK's digests; an `:ir` entry must be the IR the run
was recorded from and the pack's, its `:review` rows its stdout's lines, each with a basis. Controls, each must not pass: every
entry's outcome with its stdout's middle byte changed (an empty stdout: its status), every entry's record against the next entry of
another outcome, and `test/llvm/controls.edn`'s pairs whose reference has an entry (the subject's recorded run). Result: 16 of 16
outcomes met by the recorded runs (3 aborts' stderr g++'s), 36 controls, 0 pass. Its own controls proved it: the first version passed
all 16 changed outcomes (it looked the entry up again by id and ignored the change), fixed by `entry-verdict`; a review row changed and
an `:ir` changed each fail it (checked by hand, reverted). `own-verdict` over the 562 modules: all 562 by their own verdict, the two
"worked out from the module's IR"; the -O0 module with `@a9`'s initializer changed: no own verdict (nil). A new control in
`test/llvm/controls.edn`: `c-O2.synth.pp-define`'s run against pp-has-attribute's reviewed stdout (both exit 0; 189 bytes against 96),
judged in llvmi's full runs, and now by derived-check on pp-define's recorded run: it fails as it must.

**Built: `-allow-incomplete-ir` is a relaxing option** (`bin/repr-llvm` `own_verdicts`, LLS46 (a)'s way). LLParser.cpp of the pinned
23.1.5 reads it in three places, all in validateEndOfModule (334: unknown metadata attachments dropped; 406: undefined globals
declared; 461: an assertion): it never adds an error, so a `not llvm-as -allow-incomplete-ir` rejection is a rejection without it. The
first message may differ (the smallest undefined metadata id over every use, or an undefined value), so the verdict is taken without
its fragment. One module: `Assembler-incomplete-ir-metadata-unsupported` (`invalid`), met by Arbace's verifier (`check verify-llvmtest
--only`: 7 modules, 7 by their own verdict, 7 pass, 0 failing, its record unchanged). Control `_control/relaxed-vs-valid`
(`Analysis-BasicAA-128-bit-ptr` against it) fails as it must.

**Scores** (`bin/crops-score --record`): the judge's cases 4,727 own and 547 recorded to 4,732 and 542, (4732 + 0.5 x 542) / 5274 =
0.94862, the stage (rounded to three places) 0.948 to 0.949, LLVM's independence 0.9896 to 0.9898 (floor 98 both); completeness
unchanged (0.9933); `--judge --strict` 0 problems, `bin/records-check` 0 problems; no record changed.

**Left on a record, and why** (542):
- the probes 478: LLS30 (the user, 2026-11-07), not touched.
- verify-llvmtest's 48 (each RUN line read; opt's recorded verdicts compared only after the reading):
  - 32 whole split-file tests (Assembler 21, Verifier 7, Bitcode, CodeGen 2, Analysis, Transforms: 215 sections, 189 `not llvm-as`/`not
    opt` lines): the RUN lines feed the sections one at a time; nothing reads the whole file, whose opt verdict is about the
    concatenation (`expected top-level entity` where a section is YAML, `invalid redefinition` where two sections define one function):
    no verdict of the test's. D1120-2.
  - 6 sections no RUN line gives a verifying tool alone: five only concatenated by `cat` (ptrauth-sign-personality's authflag,
    stack-guard-tls's a, Mips stack-alignment's _32, InstrProfiling comdat's disable, SimpleLoopUnswitch's main with a `.prof`) and
    llvm-diff's a (`not llvm-diff`).
  - 2 `XFAIL: *` (X86 GlobalISel ext, StructurizeCFG backedge-id-bug): the test expects its own failure.
  - 8 read under a target, a pass or an extra check: AMDGPU lds-reject-mixed-absolute-addresses (the test says opt takes it under
    `-mtriple=amdgpu--`, whose datalayout makes the `!absolute_symbol` range's i32 the pointer's width; plain verify on the default
    datalayout rejects it: the verdict depends on the triple, none of the test's for plain verify); GCOVProfiling/version (the RUN lines
    feed `cat little.txt %s version.txt`, the file needs the appended `!9`); and 6 whose test states the verdict under an option the
    source shows leaves the rule alone: D1120-1.
- verify-rt-heldout's 9 and verify-rt's 4 mutants of unknown kinds (n-llvm-indep's list: an attachment on a typed access 4, a debug
  record above a landingpad, attribute pairs and values, immarg, an intrinsic's declaration and rename, three debug-info fields): each
  a further port into `arbace.repr.llvm.expect`, then the pack's mutants made again (`bin/llvmi ir-make rt --mutants`, 33 min, 1.5 GB).
- AOT's 3 aot-heavy rows (c-O0/c-O2.synth.lib-musl-tables, cpp.runtime.library): their own verdicts exist (the `.expect`, the C++
  entry); their kept rows date from 2026-11-05's run, judged against lli then. By hand, under the lock (Go's compiler 2.9-3.3 GB, the C++
  runtime built): `LLVMI_AOT_HEAVY=1 bin/heavy-lock bin/llvmi check --aot --only test/llvm/aot-heavy.txt --record`: 3 cases.

**The floor.** The stage is scored rounded to three places: 0.950 (independence 0.99, floor 99) needs at most 532 recorded, 10 fewer
than now: D1120-1's 6, the heavy rows' 3 and one mutant rule; or D1120-2 alone.

### Proposed (llvm-judge-1120)

- **D1120-1 Reviewed verify verdicts for llvm/test modules read under an option that leaves the rule alone** (6 cases).
  - (a) *Recommended.* A reviewed table, `test/llvm/llvmtest-reviewed.tsv` (NAME, VERDICT, FRAGMENT, BASIS, WHY), read by `own_verdicts`
    for a module its RUN lines leave `-`: the test's `not` line and CHECK message, and the 23.1.5 source line that makes it plain verify's
    verdict; the golden file's way (JCS361 (a)) for a verdict; a control. The six, with their basis: DirectX discard_error,
    firstbitshigh_error, firstbituhigh_error, saturate_errors (`not opt -dxil-op-lower -mtriple=dxil-...`): the CHECK message is
    `Intrinsic::isSignatureValid`'s (Intrinsics.cpp:975-976), called by LLParser.cpp:378 and Verifier.cpp:3297 from the intrinsic's
    table, not the triple: opt fails on its input before the pass, `invalid` with that message; AMDGPU llvm.amdgcn.cooperative.atomic
    (`-mtriple=amdgcn -mcpu=gfx1250 -passes=verify`): VerifierAMDGPU.cpp:371-380, keyed by the intrinsic's ID and called unconditionally
    (Verifier.cpp:7087): `invalid`, "cooperative atomic intrinsics require a generic or global pointer"; noalias_scope_decl
    (`--verify-noalias-scope-decl-dom`, an added check): the first CHECK's rule (Verifier.cpp:7597-7599) runs before the option's guard
    (7605): `invalid`, "!id.scope.list must point to a list with a single scope". Gain 6: 0.9486 to 0.9492 (the stage 0.949 either way).
    A caution the review found: my first reading of the DirectX tests (the pass's message reached, so opt's input verification took the
    module: `valid`) was wrong; the message is the verifier's own, printed before the pass (found when opt's record was compared after
    the reading, confirmed in the source). Each row needs its source line, not a reading of the tools' order.
  - (b) Keep opt's record for them: the expectation is derived from LLVM's source, not stated by the test for plain verify (LLS30's
    reading for the probes).
- **D1120-2 The 32 whole split-file tests.**
  - (a) *Recommended.* Their sections in their place: `section_verdict` extended to `invalid` (a `not llvm-as` or `not opt
    -passes=verify` naming `%t/SECTION` with plain options, the fragment from the line's check prefix, as `own_verdicts` does for a whole
    file), the sections (about 215) added to verify-llvmtest's lists and the whole files counted out of the judge's cases (the read cell
    keeping them as reader input, as LLS57 (b) kept the pack's modules). A corpus change: the record re-recorded (light, about 30 s), the
    sections' opt records the oracle tier's. Gain: 32 recorded cases leave, about 215 own join: the stage about 0.953, independence 0.99
    (floor 99).
  - (b) Count the whole files out only (no sections): the stage 0.951; but 32 of the read cell's cases would have no judge at all.
  - (c) Keep opt's record (LLS46/LLS50's boundary: a whole split-file test has no one verdict).

## Decided (the main session for the user, 2026-11-20): D1120-1 (a), D1120-2 (a)

## As built: D1120-2 (a), D1120-1 (a) (2026-11-20, branch `llvm-judge-1120`)

**D1120-2 (a): the sections in the whole split-file tests' place.** `section_verdict` (bin/repr-llvm) gives `invalid` to a section
every command naming which is a `not llvm-as` or `not opt` (no pass but verify) reading it as its input (its last positional word, else
its stdin's `<`) with only own_verdicts' plain options (-S, -disable-output, -passes=verify, -f, -o FILE, redirections;
-allow-incomplete-ir relaxing: no fragment), as own_verdicts takes a whole file's `not llvm-as %s`; the fragment what follows `error:` on
the first check line of that line's FileCheck prefixes in FileCheck's check file (`%s` the parent, `%t/NAME` a section, read from the
section's directory). A section also taken by a plain command, or under `not --crash` or another tool's `not`, has none; a read with
`--disable-verify` claims nothing and leaves a rejection standing (ptrtoaddr's dst_not_addr_size: parsed by `llvm-as --disable-verify`,
rejected by `not llvm-as`), and still bars `valid` (LLS50 unchanged: the 60 earlier sections' verdicts computed again, all the same).
The sections: `bin/llvm-corpus split-tests` (new; `scan` runs it after its own split) expands the parents of
`test/llvm/llvm-test-split-reject.list` by the pin's split-file with each test's own options (--leading-lines) into the main checkout's
`.tmp/llvm/split/`, each test into a fresh directory swapped in; the list holds the 203 sections of 30 of the 32 tests that have an own
verdict (182 invalid, 21 valid; 9 have none: `not --crash` 2, a pass 7; loop-md-errs and ctx-instrumentation-invalid-roots none at all),
byte for byte the scratch split they were chosen from. verify-llvmtest reads the list beside its four: 1,009 modules, 967 by their own
verdict, 42 opt's (the 32 whole files, kept in the read cell and counted out of the judge's cases as LLS57 (b) did, 6 sections only
`cat` or llvm-diff reads, 2 XFAIL, lds-reject, GCOVProfiling/version). Of the 203: 141 pass; 62 do not (61 verdicts, 1 first message),
rejections the whole file's opt record hid behind its first section's error: class `:llvmtest-section-rules` (deferred,
test/llvm/divergences-read.edn: the rules LLParser and the Verifier of 23.1.5 have and repr/llvm lacks: llvm.convert.to/from.arbitrary.fp
8, the ptrauth global_ctors autoupgrade 7, blockaddress across address spaces 7, target extension types' properties 7, AMDGPU's
work-group-size attributes 6, value-profile !prof 5, symbolic address spaces 6, constant splats 4, ptrtoaddr's width 3, pauthabi module
flags 2, branch_weights' unknown kinds 2 (one a false error), skip-value-numbers 2, three of 1). Recorded (`check verify-llvmtest
--record`, 36 s, 1.4 GB), `bin/reap --record` (LLVM read 3,845 / 1 / 0 to 3,986 / 1 / 62, 0 problems). Controls:
`_control/section-reject-vs-valid` (verify-controls.txt) fails as it must; controls-test.bash's fake tree: section b (a plain `not opt`)
now `invalid` (its valid content: the verdict differs), new parent D/v.ll: i (`not llvm-as <`, its parent's prefix: the fragment) and
j (`FileCheck %t/j.ll` after a `--disable-verify` read: the section's own CHECK line) invalid and met, k (`not --crash`) and l (a pass)
none; all 17 cases ok.

**D1120-1 (a): the reviewed verdicts.** `test/llvm/llvmtest-reviewed.tsv` (NAME, VERDICT, FRAGMENT, BASIS, WHY), read by own_verdicts
for a module its RUN lines leave `-` (a row for one they judge is refused, the RUN lines' verdict kept; the fifth column `reviewed`).
The six rows, each with its RUN and CHECK lines and the 23.1.5 source lines: the four DirectX tests declare none of their intrinsics,
so LLParser's validateEndOfModule declares each from its call and rejects the call's signature against the intrinsic's table
(LLParser.cpp:377-389, Intrinsics.cpp:975-976) while parsing, before opt sets `-mtriple` (optdriver.cpp:614-616); cooperative.atomic's
rule is keyed by the intrinsic and the pointer's address space number (VerifierAMDGPU.cpp:371-380, called for every intrinsic call at
Verifier.cpp:7087); noalias_scope_decl's first CHECK is the single-scope rule (Verifier.cpp:7586-7599), before the option's guard
(7603-7605). The review corrected itself once: saturate_errors' undefined attribute group #0 is no error at all (LLParser.cpp:217-227
merges only the groups it knows), not a later one. `bin/repr-llvm reviewed-check` (new): five columns, VERDICT valid or invalid, a
BASIS with a RUN line and a source line, a WHY, a module of verify-llvmtest its RUN lines alone leave `-`; 6 rows, 0 refused; three bad
rows (a module its RUN lines judge, a VERDICT `broken`, a BASIS with no source line) refused. `check verify-llvmtest --only` the six:
each `invalid` by its reviewed verdict, verdict and message met; `_control/reviewed-vs-valid` fails as it must.

**Scores** (`bin/crops-score --record`): the judge's cases 4,732 own and 542 recorded to 4,941 and 504 of 5,445 (the 32 whole files out,
the 203 sections and 6 reviewed in), the stage 0.949 to 0.954, LLVM's independence 0.9898 to 0.9908 (floor 98 to 99); completeness
0.9933 to 0.9894 (floor 99 to 98): the read cell's 62 new deferred rows, the gaps the sections show. `--judge --strict`,
`bin/records-check`, `make check-makefile`: 0 problems. The rules to port are arbace/repr/llvm's reader and verify.ae (embedded since
W2, so in `gate_key`): porting ten of the 62 brings completeness back to 0.990.

**C++'s golden files read by llvmi** (the coordinator's item, from mac/c-cpp-golden-1120 3dbf9f3a6: its 26 golden files beside
test/cpp/samples and test/cpp/runtime/samples and their reviews test/cpp/selfref-golden.edn taken into this branch by `git checkout`,
nothing else of it). llvmi's 14 C++ modules were already judged by their own outcomes (LLS49 (c): test/llvm/selfref-derived.edn), not
by g++'s record, so no case moves; the two reviews agreed, compared file by file, but for runtime/hello, whose stderr `to stderr\n` the
C++ review records and the llvmi entry left out. One source for both crops now: `arbace.llvm.check/golden-entries` reads N.expect,
N.cpp.expectrc and N.expect-stderr of each reviewed program (a golden file without its review is no verdict: checked, hello's review
renamed gives nil), and selfref-derived.edn keeps the two IR entries. `bin/llvmi derived-check`: 16 of 16 met by the recorded runs
(hello's stderr by lli's 10 bytes), 36 controls fail; runtime/deferred has golden files and no module of llvmi's (it is not in the pack
c: C++'s alone, reported). own-verdict over the 562 modules: all own, the 14 by C++'s golden files.

**The 13 mutants (the coordinator's step 3): done already, the facts stale.** The pack's 1,750 mutants all carry a verdict line (1,522
invalid, 124 valid, 104 debug; none `unknown`): queue 407 (2026-11-19, `61c565059`, the VM's 407-llvm-verifier taken on the main host)
ported the last Verifier rules and made the mutants again (`ir-make rt --mutants`), its journal saying so, while independence.edn's
judge kept counting 9 + 4 recorded. Measured here on the checkout's pack: `bin/repr-llvm check verify-rt` 905 of 905 pass, 653 by
their own verdict, 252 opt's (exactly the pack's modules, out of the judge's cases since LLS57 (b); 4 min 55 s, 1.8 GB) and
`verify-rt-heldout` 1,101 of 1,101, every one by its own verdict (9 min 55 s, 1.8 GB); records unchanged. No port and no remake left:
13 cases from recorded to own, the facts corrected (4,954 own, 491 recorded: the probes 478, verify-llvmtest's 10, AOT's 3 kept rows),
the stage 0.954 to 0.955, independence 0.9908 to 0.9910. `test/gate/steps.edn`: llvm-essential's inputs gain
`test/cpp/selfref-golden.edn` (llvmi's judge reads it now: the audit's rule).

### Proposed (llvm-judge-1120, after D1120-2 (a))

- **D1120-3 The 62 sections' rules** (completeness 0.9894: ten of them bring it back to 0.990, all 62 to about 0.9988).
  - (a) *Recommended.* Port them into repr/llvm's reader and `verify.ae` by group, the largest first (llvm.convert.to/from.arbitrary.fp
    8, the ptrauth global_ctors autoupgrade 7, blockaddress across address spaces 7, target extension types' properties 7, AMDGPU's
    work-group-size attributes 6, symbolic address spaces 6, value-profile 5, then the small ones), each group a commit with its
    sections passing and `:llvmtest-section-rules` shrinking. `arbace/repr/llvm` is embedded since W2, so each change moves `gate_key`
    (the night's builds run again): batch them in one or two nights.
  - (b) Leave them deferred: the read cell holds them as found gaps.

## Decided (the user, 2026-11-20): LLS30 revisited, the probes' own verdicts; a case without a stated outcome leaves the judge

## As built: the probes' reviewed verdicts, the cases out of the judge (2026-11-20, branch `llvm-judge-1120`)

**The reviews.** Each of the 478 probes now states its verdict in its first two lines: `; verdict: VERDICT[ MESSAGE]` (valid;
invalid or debug with the first message LLVM 23.1.5 prints; out) and `; rule: SOURCE WHY` (the path:line in llvm-project 23.1.5 of the
check that fires first, and why it is first). Five reviews in parallel (attrs 121, cc and intrinsics 81, metadata and debug 97,
musttail and funclets 106, verify 73; the guide `.tmp/probes-review/GUIDE.md`) each worked from the probe's IR and the pinned tarball's
source alone (LLParser.cpp, LLLexer.cpp, Verifier.cpp, VerifierAMDGPU.cpp, AutoUpgrade.cpp, Intrinsics.cpp, Function.h, ...), forbidden
to open opt's record or run any tool; only then were they compared with opt's record. 470 agreed at once; the 8 that did not (all in
verify) were settled again from the source, each ending where opt's record is and each row saying it was corrected: verify-fpc
(a decimal `1.5` lexes as a FloatLiteral, LLLexer.cpp:1308, so LLParser.cpp:4218's "floating-point constant invalid for type", not the
APFloat token's 6873), verify-intr4 (the parser's UpgradeCallsToIntrinsic, LLParser.cpp:472, remangles llvm.smax.i64(i32, i32) to
llvm.smax.i32, AutoUpgrade.cpp:1979: valid), verify-intr7, o1, o2, o3, o7 (the declaration is visited before the caller; a call of
another function type is a use Function::hasAddressTaken counts, Function.cpp:987: "Invalid user of intrinsic instruction!",
Verifier.cpp:3284), verify-o5 (the first function's own lifetime.start on an argument, Verifier.cpp:7019). Three are `out`, the
user's rule below: cc-c8 (`cc 99999` breaks setCallingConv's assertion, Function.h:278; a release build truncates the id into other
flags), funclets-v22 (visitFuncletPadInst calls getParentPad on a landingpad, an unchecked cast<CatchSwitchInst>, Verifier.cpp:4776:
undefined behaviour), metadata-w29 (ProfDataUtils.cpp:167's assertion; an assertions build aborts). Counts: 309 invalid, 154 valid,
12 debug, 3 out.

**The checks.** `own_verdicts` reads a module's `; verdict:` line when it has no RUN line (the fifth column `probe`: a valid is valid
alone, as a mutant's); `out` leaves it to opt's record. `bin/repr-llvm probes-check`: every probe a verdict and a rule line of the form
(478 stated, 0 refused; a probe with `maybe`, and one without its rule line, refused: checked by hand and reverted); `probes-oracle`
(the oracle tier: no tool run, opt's record read): 475 stated, 475 agree with opt's verdict and first message, 3 out; every verdict
flipped is a control, 475, none agrees. `check verify-probes --record`: 478 of 478 pass, 475 by their own verdict and 323 rejections with
the stated message, Arbace's verifier meeting each; cc-s39 (`:probe-pointer`, opt printing an address) passes by its own verdict now.
The probes' files changed by two comment lines, so opt's runs of them were recorded again by hand (`bin/repr-llvm record-opt
test/llvm/probes.list`, the pinned host opt 23.1.5, the oracle tier's verb): every probe's verdict and first message the same as
before, only the digests new, and no other entry of ref-opt23.edn changed. Controls: verify-controls.txt's two probe controls now cross
stated verdicts; controls-test.bash's p-good (addf, stated invalid, met) and p-flip (addf stated valid: differs, the verb fails); all
20 cases ok.

**A case without a stated outcome leaves the judge** (the user, 2026-11-20: undefined, unspecified or host-dependent expected outcomes
leave the judge's cases with a written reason, their rows still counting in completeness). `test/llvm/judge-out.tsv`, a row a case
with its reason: verify-llvmtest's 42 opt-judged modules (the 32 whole split-file tests of D1120-2 (a), 6 sections the tests only
concatenate or diff, 2 `XFAIL: *`, lds-reject whose verdict depends on the target's datalayout, GCOVProfiling/version read only
concatenated) and the 3 out probes. `bin/repr-llvm judge-out-check` holds it to the checks: every module of verify-llvmtest and the
probes that its own verdict leaves to opt's record is a row, and every row such a module (1,487 modules, 45 rows, 0 problems).

**Scores** (`bin/crops-score --record`): the judge's cases 4,954 own and 491 recorded to 5,429 own and 3 recorded of 5,432 (the 3:
AOT's kept rows of aot-heavy.txt until `LLVMI_AOT_HEAVY=1`'s run), (5429 + 1.5) / 5432 = 0.99972, the stage 1.000, LLVM's independence
0.9910 to 1.0000 (100); completeness 0.9895 (99 to 98 since D1120-2 (a): D1120-3); `--judge --strict`, `bin/records-check`, `bin/reap`
(LLVM read 3,987 / 0 / 62), `make check-makefile`: 0 problems. Makefile: repr-llvm-probes-check, -probes-oracle, -reviewed-check,
-judge-out-check.

**The heavy AOT rows (the coordinator's step 2).** `LLVMI_AOT_HEAVY=1 bin/heavy-lock bin/llvmi check --aot --only
test/llvm/aot-heavy.txt --record` (3 h 43 min wall, about half of it in the lock's queue; a fresh worktree builds C++'s lowered runtime,
`cppi runtime-build` 35 min, and ci reads its 199 units, `read-lib` 52 min at up to 8.6 GB; then two Go builds, the eh one 3.8 GB): 3 of
3 pass by their own verdicts (the two lib-musl-tables modules by synth-c23's `.expect`, cpp.runtime.library by C++'s golden files, from
its 2026-11-05 `:refused` row: the `:personality` class holds none now). `bin/reap --record`: LLVM AOT 554 / 7 / 1 to 555 / 7 / 0.
**Final:** the judge's cases 5,432, every one by its own verdict (45 out with their reasons, test/llvm/judge-out.tsv): the stage 1.0000,
LLVM's independence 1.0000 (100); completeness 0.9899 (98: D1120-3's 62 sections), meant 0.9962, maturity 1.0000.

## Decided (the main session for the user, 2026-11-20): D1120-3 (a)

## As built: D1120-3 (a), the 62 sections' rules (2026-11-20, branch `llvm-sections-1120` from a5e100f56)

Every rule from llvm-project 23.1.5 (the pinned tarball), its lines cited where it is ported (arbace/repr/llvm/verify.ae, read.ae), each
group proven on its sections (verify-llvmtest `--only` the test's sections, the valid ones beside; every first message compared with
the test's CHECK line where it carries none of `error:`), in five commits (they move `gate_key`):
- batch 1 (29): visitIntrinsicCall's convert_to/from_arbitrary_fp (Verifier.cpp:6124-6211; APFloat.cpp:6020-6038, FPEnv.cpp:25-36) 8;
  visitProfMetadata's `unknown` and VP rules, verifyUnknownProfileMetadata and verifyFunctionMetadata's `unknown` (Verifier.cpp:2621-2644;
  ProfDataUtils.cpp:50/132, InstrProfData.inc:259-281) 7, one of them a false error gone; target extension types' properties at globals,
  allocas and byval (Type.cpp:74-104, 1050-1140; Verifier.cpp:810-813, 4670-4673, 2172-2175) 7; LLParser's forward block addresses
  (LLParser.cpp:276-279, 4383-4440, 7298-7349, 1861) 7.
- batch 2 (12): reqd_work_group_size (Verifier.cpp:2678-2704) and verifyAMDGPUReqdWorkGroupSize (VerifierAMDGPU.cpp:60-113) 6;
  parseOptionalAddrSpace's symbolic address spaces (LLParser.cpp:2041-2060; DataLayout.cpp:828-835), the reader keeping the data layout
  it has read, 6, positions LLVM's columns.
- batch 3 (9): AutoUpgrade's upgradePtrauthInitFiniArrays decided as the parser does (AutoUpgrade.cpp:6389-6486) and the Verifier's
  rule on a ptrauth structor it leaves 7; checkValueID at numbered globals, aliases and functions (LLParser.cpp:875, 7124, 3595-3600) 2.
- batch 4 (5): checkPtrToAddr's address width (Verifier.cpp:3709-3724; DataLayout.h:518-520) at the instruction and through
  visitConstantExprsRecursively (2709-2744, 2755; 830, 5775) 3; visitModuleFlags' ptrauth and PAuth ABI flags (1839-1886) 2.
- batch 5 (7): a splat's types (LLParser.cpp:4725-4737, 6949-6957) 4; atomicrmw's store size (9250-9254) 1; the dialect field's integer
  (5351-5360, 5219; Dwarf.h:212) 1; module asm's property value (666, 1958-1963) 1.
Checks at the end, on the last commit's code: verify-llvmtest 1,009 of 1,009 pass (629 of 629 rejections with the stated message),
verify-probes 478, roundtrip-llvmtest and text-llvmtest 304 each (the reader changed), verify-rt 905 and verify-rt-heldout 1,101
(rerun on the final code beside this note). `:llvmtest-section-rules` is gone (62 to 0); LLVM read 3,987 / 0 / 62 to 4,049 / 0 / 0;
completeness 0.9899 (D1120-2's sections) to 0.9938, meant 1.0000 (the 7 by-design cases of eval and of AOT are what remain), independence
1.0000, maturity 1.0000; crops-score --judge --strict, records-check, reap: 0 problems. No gate (the user).
