# x86-64 assembly as a crop: survey and plan

Status: survey and plan, 2026-11-09, at the user's request (x86-64 assembly a crop of its own, read, eval, AOT and print, the way
LLVM IR became one: doc/llvm/LLVM-CROP-SURVEY.md is the model). Nothing here is adopted: only this file and a journal entry were
written; no code under `arbace/`, `go/`, `bin/`, `tools/` or `test/` changed. The decisions are the user's, as proposals AXS1-AXS16
(§14) and the shared ones AS1-AS8 (§13); the milestones are AX1-AX10 (§12), with two optional ones. A survey of arm64 assembly is
written at the same time (doc/asm/ARM64-SURVEY.md, branch `asm-arm64-survey`); §13 proposes what the two crops share, so the two
plans can be merged into one.

Base: branch `asm-amd64-survey` from `mac/next` `7da5badcf`. The work is reading (doc/llvm/LLVM-CROP-SURVEY.md whole, doc/CROPS-SCORE.md,
test/crops/crops.edn, test/reap/cells.edn, doc/ORACLE-RECORDS-SURVEY.md §3, doc/C23-SURVEY.md's crt and ci, doc/go/GOI-SPEC.md's
assembly class, `go/arbace/crt/{mem,syscall}.ae`, `tamago/cmd/`'s `cmd/asm` and `cmd/internal/obj/x86`, TamaGo's Go tree's
`cmd/vendor/golang.org/x/arch/x86/x86asm`) and light probes of seconds each (§3), in the worktree's `.tmp/asm/` (Appendix A): the
host's GNU as 2.45.1 and llvm-mc 23.1.5 on musl's x86-64 assembly, Intel syntax against AT&T, TamaGo's `go tool asm` on Go's encoding
tests, a static program assembled, linked and run. No Arbace executable was built, no lock taken, nothing installed, nothing
downloaded. Sizes of what the host does not hold are marked **to verify**.

Words used throughout:

- **Assembly forms**: assembly text as Arbace forms (§4.2): a file's items (directives, labels, instructions), each instruction an
  opcode and operands in one vocabulary whatever the syntax it was read from.
- **The machine level**: an instruction as the machine sees it, opcode and operands resolved (LLVM's `MCInst`, x86asm's `Inst`): what
  the encoder takes, the decoder gives and the emulator runs. The assembly forms' instructions are this level plus their spelling.
- **repr/asm**: the bridge between assembly text and assembly forms (`bin/repr-asm read|print|roundtrip`), as repr/c and repr/llvm.
- **The three syntaxes**: GNU as's AT&T syntax (the default of gas, gcc `-S`, clang `-S`, musl, the Linux kernel), Intel syntax as gas
  and LLVM accept it (`.intel_syntax noprefix`, gcc `-masm=intel`), and Go's Plan 9 assembler syntax (`cmd/asm`).
- **x86i**: the evaluator, a user-mode emulator of x86-64 Linux programs over crt's memory and syscall layer (the name is AXS12's).
- **The CPU**: the host's processor running an executable natively. It is not a toolchain (§9, AXS9).
- **Own verdict**: a test's verdict that no toolchain made (the user's ruling of 2026-11-07 for LLVM IR, every crop since): an exit
  status, an expected output written in the test, an encoding or a printed instruction written in the test by its authors.

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 What the probes showed · 4 Read · 5 Print · 6 eval: x86i ·
7 AOT: Arbace's assembler and linker · 8 The judges · 9 Independence and the oracle tier · 10 The corpora · 11 Layout, row and budget ·
12 Plan (AX1-AX10) · 13 Shared with arm64 (AS1-AS8) · 14 Proposals (AXS1-AXS16) · 15 What to decide first · 16 Risks · 17 Sources ·
Appendix A: the scratch.

## Summary

- **Assembly is the bottom of the hub.** Every crop that reaches machine code passes through it (Go's compiler emits Plan 9 listings,
  clang and gcc emit AT&T, LLVM IR's own build is `llc` to assembly), and the long-term goal (CLAUDE.md: Arbace all the way down to the
  bare metal ISA, in the way of Go plus TamaGo) needs Arbace to read, run and write machine code itself. Like LLVM IR it is an
  **intermediate crop**, its own REAP row, counted beside the ten languages.
- **Much exists as prior art Arbace already holds under BSD**: Go's assembler (`cmd/asm`: lexer, parser, the x86 back end
  `cmd/internal/obj/x86` with its optab through AVX-512) as Go forms in `tamago/cmd/`; Go's x86 decoder and its three printers (GNU,
  Intel, Plan 9: `x86asm`, 22,979 lines, AVX-512 from Intel's XED data) in TamaGo's Go tree, a `repr/go` conversion away; Go's ELF reader
  (`debug/elf`) and ELF writer (`cmd/link/internal/ld/elf.go`). And crt (C's run time) is already a flat 64-bit little-endian address
  space with the Linux x86-64 syscalls musl makes: x86i's memory and kernel.
- **Every judge can be an own verdict** (§8). llvm/test's `MC/X86` states the expected encoding of 106,075 instructions and the
  canonical text of each (`# CHECK: ... encoding: [...]`); Go's `amd64enc.s` states 10,709 (`// 4815674523f1`); 96 error tests state their
  messages; run programs state their exit status. What has no own verdict (the stdout of a program with no expected output) is judged
  by the CPU running the executable natively, and per instruction by a **lockstep tracer** (`ptrace` single-step of the native run against
  x86i's, a Go program of Arbace's own, no toolchain).
- **The host's assemblers only at the oracle tier**: GNU as 2.45.1 (GPL: run, never read for porting), llvm-mc 23.1.5 (Apache-2.0 with
  LLVM exception; the same pin as LLVM IR's clang). They already disagree on real code (§3, P2: a jump to a global symbol), so the
  reference must be named per corpus.
- **Corpora on the host, offline** (§10): llvm/test's `MC/X86` (632 files, 17 MB, 427,555 lines) with `MC/AsmParser` (162),
  `MC/ELF` (265), `MC/Disassembler/X86` (187: 13,545 byte strings with their instruction); Go's `cmd/asm` testdata (amd64: 12,252
  lines, AVX-512: 14,302) and the 67 hand-written amd64 `.s` files in `tamago/goroot` (33,780 lines); musl 1.2.6's 32 x86-64 `.s`/`.S`
  (711 lines); gcc 15.2.0's `gcc.target/i386` (9,288 files, 3,504 run tests, 470 with inline assembly) and `gcc.target/x86_64` (153,
  16 `.s`/`.S` ABI tests). To fetch, with the user's OK: binutils 2.45.1's `gas/testsuite` (GPL test data), Go's `x/arch` (`x86.csv`,
  x86asm's `decode.txt`), Intel XED (Apache-2.0), musl's `libc-test` (MIT).
- **Plan (§12): AX1-AX10, about 53 to 92 working days**, of which about 11 to 15 are shared with arm64 if §13 is taken (the forms'
  frame, GNU's directive language, the ELF writer and linker, the emulator's frame, the judges, the row). The first milestone (AX1) is
  small and measurable: musl's 32 files and `MC/AsmParser`'s x86 subset read, printed back byte for byte (`:full`) and read again to the
  same forms.
- **To decide first (§15):** AXS1 (the row, two crops), AS1 (one tool family for both architectures), AXS4 (the instruction table's
  source), AXS9 (the CPU and the lockstep tracer as judges), AXS7 (AOT's meaning), AXS14 (the fetches).

## 1. What parity means

The crops' rule for an intermediate language (doc/llvm/LLVM-CROP-SURVEY.md §1): the subject is the text itself, any producer's; the
reference is the machine's own reading and execution of it.

| cell | in Arbace (proposed) | corpus | own verdict | oracle tier (by hand, at a tag) |
|---|---|---|---|---|
| **read** | repr/asm `read`: AT&T, Intel, Plan 9 → assembly forms; the assembler's semantic checks (operand kinds, sizes, ranges) as the reader's acceptance | `MC/X86`, `MC/AsmParser`, musl, Go's `.s`, gcc's `-S` packs, binutils' gas tests (fetched) | the forms read again equal; the canonical print equal to the test's stated text (`# CHECK: vaddps ...`); a rejection's message fragment as the error test states it | gas and llvm-mc accept or reject the same files |
| **eval** | x86i: decode, run user-mode over crt's memory and syscalls; undefined symbols bound to ci's musl (§6.4) | programs with an entry: synthetic programs per instruction family, musl's `.s` under harnesses, gcc's run tests and C's run corpora through gcc `-S` and clang `-S` (packs), Go's leaf `.s` functions (AX9) | exit status (torture's `abort`), expected output in the test; else the CPU's run of the same executable, and the lockstep trace | the executable assembled by gas and linked by ld, run |
| **AOT** | Arbace's encoder, ELF writer and static linker: an executable, run by the CPU | every run corpus; every encoding test | the stated encodings byte for byte; the run's own verdict | gas's and llvm-mc's objects compared section by section |
| **print** | repr/asm `print`: canonical in each syntax; `:full` (comments, layout, spellings) | every read corpus | `:full` byte for byte; the print read again equal; AT&T → forms → Intel → forms equal | the printed file assembled by gas to the original's bytes |

linux/amd64 alone for eval and AOT (x86i is user-mode Linux; the box's bare-metal mode later, §16). Read and print take 32-bit and
16-bit code too (MC's `-32`/`-16` files, `.code32`), the encoder those modes in AX2's second half; x86i runs 64-bit mode only.

**What differs from LLVM IR.** There is no lowering into C: the instructions are the semantics, the machine (the CPU) is present on the
host, and so an own reference exists for every program the CPU can run. The difficult parts move: the encoder's **choices** (an
instruction has several encodings; the judge's bytes fix one) and the emulator's **breadth** (about 1,500 mnemonics with AVX-512, each
with flags, exceptions and corner cases).

## 2. The host and prior art

### 2.1 The host

- GNU Binutils 2.45.1 (`as`, `ld`, `objdump`, `objcopy`; Alpine's `binutils`), GPL-3.0-or-later.
- LLVM 23.1.5's tools in `/usr/lib/llvm23/bin`: `llvm-mc`, `llvm-objdump`, `llvm-readobj`, `llvm-mca`, `llvm-exegesis`, `llvm-tblgen`,
  `FileCheck`, `not`, `split-file`; no `ld.lld` (lld is not installed); clang 23.1.5 (LLVM IR's pin, `test/cpp/corpora.edn`).
- gcc 15.2.0 (emits AT&T, assembled by gas), musl 1.2.6, gdb, `qemu-aarch64` (user mode, arm64's; no `qemu-x86_64`).
- TamaGo's go1.27.1 (`/root/tamago-go`): `go tool asm`, `compile -S`, `link`; not counted as a host toolchain (doc/CROPS-SCORE.md).
- The CPU: a Zen 3 server part with SSE4.2, AVX, AVX2, BMI2, FMA, SHA, VAES, **no AVX-512, no AMX, no APX** (`/proc/cpuinfo`). What
  the CPU lacks, only x86i can run (§8.3).

### 2.2 Prior art (licences checked where on the host; else to verify)

| project | what | relevance | licence | use |
|---|---|---|---|---|
| **Go's assembler** `cmd/asm` (`internal/lex`, `asm/parse.go`, `arch`) and `cmd/internal/obj/x86` (`asm6.go`, `avx_optabs.go`, `evex.go`, `obj6.go`) | Plan 9 syntax's reader and the x86 encoder through AVX-512; `obj6.go` inserts prologues and stack checks | the Plan 9 reader as is; the encoder's tables and choices a second view; already Go forms in `tamago/cmd/` (21,562 lines of forms for these two) | BSD-3 (Go's, in LICENSE.md) | reuse (AX9), reference |
| **x86asm** (`golang.org/x/arch/x86/x86asm`, vendored in TamaGo's `src/cmd/vendor`) | a decoder (tables from `x86.csv`; AVX/AVX2/AVX-512 from XED's data, 2026) with GNU, Intel and Plan 9 printers | x86i's decoder; the disassembler that reads an object back (AOT's own verdict); the three printers as a reference for the canonical prints | BSD-3 | port by conversion (repr/go), AX5 |
| **`debug/elf`**, `cmd/link/internal/ld/elf.go` | ELF reading; ELF writing for Go's linker | the ELF writer's and reader's shape (relocatable objects are not cmd/link's output: the writer is new) | BSD-3 | reference, reader reused |
| **LLVM MC** (`lib/MC/MCParser` 608 KB, `lib/MC` 1.9 MB, `Target/X86/AsmParser` 218 KB, `MCTargetDesc` 490 KB, `Disassembler` 129 KB, the `.td` instruction definitions 2.6 MB) | GNU-compatible directive parser, x86 AT&T and Intel parsers, the encoder, ELF writer | the read and encode levels' semantics, rule for rule; the tables via `llvm-tblgen` (AXS4) | Apache-2.0 WITH LLVM-exception (in LICENSE.md) | port where needed, reference |
| **Intel XED** (`intelxed/xed`) | Intel's encoder/decoder, its datafiles the most complete instruction description | an instruction table's source (AXS4) | Apache-2.0 (to verify) | data, fetch |
| **Intel SDM**, **AMD APM** | the ISA's specification | the emulator's semantics | proprietary documents | reference only, never copied |
| **Blink** (`jart/blink`) | a user-mode x86-64 Linux emulator in C, a small JIT, lazy flags | x86i's shape: the closest prior art, permissive | ISC (to verify) | reference, porting allowed |
| **box64**, **FEX** | x86-64 user-mode on arm64 (dynarec) | binary translation (AX11), syscall layers | MIT (to verify) | reference |
| **iced**, **Zydis**, **fadec** | encoder/decoder libraries | decode tables, encoder choices | MIT, MIT, BSD-3 (to verify) | reference |
| **Remill**, **ACL2 x86isa**, **K's x86-64 semantics** | formal or lifted instruction semantics | a second statement of the semantics for flags and corners | Apache-2.0, BSD-3, UIUC/NCSA (to verify) | reference |
| **GNU binutils** (`gas/config/tc-i386.c`, `opcodes/i386-*`) | the reference assembler | **never read for porting**: GPL, as the Haskell crop's rule (the GPL port `haskell-pp` never entered history); its testsuite as test data only (LLS26 (a)'s rule) | GPL-3.0-or-later | run at the oracle tier; tests as data |
| **QEMU**, **unicorn**, **Bochs** | emulators | not ported, not read for porting, not a dependency | GPL-2.0, GPL-2.0, LGPL-2.1 | none (qemu-aarch64 is arm64's oracle question) |

### 2.3 What Arbace already has

- **crt** (`go/arbace/crt`, C5): one flat 64-bit little-endian address space of slabs (`Mem`: Text, Rodata, Data, Heap, mmap, a stack
  at `StackTop`), faults as `SIGSEGV`/`SIGFPE` panics, and `Sys.Syscall`, the Linux x86-64 system calls musl 1.2.6 makes (1,211 lines:
  read, write, mmap, brk, rt_sigaction, futex for one thread, arch_prctl, exit_group, ... `-ENOSYS` for the rest). Its function
  addresses are synthetic (`Text + 16 * index`): x86i needs a Text slab holding real bytes and a call gate for C functions (§6.4).
- **ci and cf2g** (C5, C6): musl as C forms, interpreted and compiled; C's own target (`c/`, x86_64-linux-musl, SysV ABI's layouts in
  `c/target/abi.edn`): the ABI x86i's bridge needs.
- **repr/c's preprocessor**: `.S` files are C-preprocessed assembly (musl's, gcc's); the preprocessor is repr/c's, so no `gcc -E`.
- **Go's toolchain as forms** (`tamago/cmd/`, S9): the 105 packages `compile`, `link` and `asm` build from, byte-identical tools when
  printed; `cmd/asm` and `cmd/internal/obj/x86` among them.
- **The classes where assembly stops the other crops**: C's `by-design` (inline assembly, CS3 (a)), the LLVM lowering's `:inline-asm`,
  goi's "assembly, cgo, go:linkname" (9 programs) and every std package goi runs on its pure-Go fallback. x86i would be the place where
  each of these runs (§6.5), a cross-crop dividend.

## 3. What the probes showed

All in `.tmp/asm/` (Appendix A), seconds each.

- **P1, gas against llvm-mc on musl** (`musl-1.2.6/src/*/x86_64/*.s`, `.S` preprocessed): 32 files; the `.text` bytes (objcopy) equal on
  **31 of 32**.
- **P2, where they differ**: `thread/x86_64/syscall_cp.s`: `jne __cp_cancel` to a global symbol in the same section. gas resolves it
  as a short jump (`75 24`); llvm-mc emits `jne rel32` with a relocation (a global symbol may be preempted). The two reference assemblers
  disagree on ordinary code, so the encoder's reference is per corpus (MC's tests: llvm-mc's choices; gcc `-S` output: gas's) and the
  choice a mode of Arbace's encoder (AXS5).
- **P3, Intel against AT&T**: the same program in both syntaxes; gas's bytes differ: `mov edx, len` with `len` an equate is a **memory
  operand** in Intel syntax (`8b 14 25 06 00 00 00`, a load from address 6), where AT&T's `mov $len, %edx` is an immediate. Intel
  syntax's operand kind depends on the symbol's definition (MASM's rule, `offset` makes it an immediate), so its reader must resolve
  symbols before it decides; the forms must be the machine level, not the text.
- **P4, a static program**: AT&T, `as` + `ld -static`, 8,944 bytes, run by the CPU: exit 42, `hello` on stdout. The freestanding run
  corpus needs no libc.
- **P5, Go's encoding tests**: `go tool asm` (TamaGo's) assembles `amd64enc.s` (10,709 lines, each with its encoding in a comment,
  generated by Go's `x86test` against GNU's objdump) in 0.06 s. `amd64enc_extra.s` adds 1,065 lines, `avx512enc/` 17 files, 14,302 lines;
  `amd64error.s` and `amd64dynlinkerror.s` state errors (`// ERROR "..."`).
- **P6, `MC/X86`'s shape**: 632 files (620 `.s`); 465 run `llvm-mc -show-encoding`; 462 hold `encoding:` lines, **106,075 of them**;
  96 are `not llvm-mc` error tests, 380 `error:` lines; 161 select Intel syntax (`-x86-asm-syntax=intel`, `-output-asm-variant=1`,
  `.intel_syntax`); 110 write objects (`-filetype=obj`, checked by `llvm-readobj` or `llvm-objdump`: oracle-tier tools); 221 are 32-bit.
  RUN lines: 701 `llvm-mc`, 106 `not`, 34 `FileCheck`, 27 `llvm-objdump`, 11 `llc`, 10 `llvm-readobj`.
- **P7, `MC/Disassembler/X86`**: 187 files, 327 RUN lines of `--disassemble`, 13,545 byte strings each followed by the instruction it
  decodes to (`# CHECK:`): the decoder's own verdicts.

## 4. Read

### 4.1 The three syntaxes and what each brings

| syntax | where | grammar | corpus | reuse |
|---|---|---|---|---|
| **AT&T** (GNU as) | gas's default; gcc `-S`, clang `-S`, musl, the kernel, glibc | `op src, dst`, size suffixes, `%reg`, `$imm`, `disp(base,index,scale)`; GNU's directive language (sections, symbols, data, `.macro`, `.rept`, `.irp`, `.if`, expressions, `.cfi_*`, `.loc`, `.file`, local labels `1f`/`2b`, `.set`/`=`) | `MC/X86` (459 AT&T), `MC/AsmParser`, `MC/ELF`, musl, gcc's `.s`/`.S` and `-S` packs, binutils' gas tests | LLVM's `AsmParser.cpp` (the directive language) and `X86AsmParser.cpp` (operands), Apache: port rule for rule |
| **Intel** | `.intel_syntax noprefix`, gcc `-masm=intel` (37 of gcc's i386 tests), LLVM's `-output-asm-variant=1`; MASM's operand rules | `op dst, src`, `dword ptr [base + index*scale + disp]`, symbols as memory unless `offset` (P3) | `MC/X86`'s 161, gcc's 37, the same programs printed in Intel by Arbace (a corpus by translation, AT&T → Intel, §5) | `X86AsmParser.cpp`'s Intel half (`ParseIntelOperand`, its expression state machine) |
| **Plan 9** (Go) | Go's `.s` files: runtime, crypto, math, `internal/bytealg`, `syscall` | `MOVQ src, dst`, `TEXT ·f(SB), NOSPLIT, $framesize-argsize`, pseudo-registers `SB`, `FP`, `SP`, `PC`, `DATA`/`GLOBL`, `#include "textflag.h"`, the C preprocessor's macros; the assembler rewrites (prologue and stack split by `obj6.go`, `MOVQ $sym` → `LEAQ`, jump sizing) | `tamago/goroot`'s 67 amd64 files (33,780 lines), Go's `cmd/asm` testdata (amd64 12,252 lines, AVX-512 14,302), the error tests | `cmd/asm` and `obj/x86` themselves, already Go forms |

AT&T and Intel are two spellings of one machine language with one directive language: one reader with two operand grammars. Plan 9
is a different language over the same machine: its text is not one machine instruction per line (the assembler adds instructions and
chooses forms), its symbols are Go's (`·`, `<>`, ABI0 and ABIInternal), and its object files are Go's (`goobj`), not ELF. Its read is
its own forms, and its machine level is what `obj/x86` produces (AX9).

### 4.2 The forms (to settle in doc/asm/SPEC.md, AX1)

Proposed (AXS2 (a)), as repr/llvm's are LangRef's items:

```clojure
(asm/file {:arch :amd64 :syntax :att}
  (asm/globl memcpy)
  (asm/type memcpy :function)
  (asm/label memcpy)
  (mov {:w 64} (reg rax) (reg rdi))                      ; dst first (the SDM's order), whatever the syntax
  (cmp {:w 64} (reg rdx) (imm 8))
  (jc (local 1 :f))
  (rep-movs {:w 64})
  (mov {:w 32} (reg edx) (mem {:base rip :disp (sym msg)})) ; Intel's `mov edx, len` when len is data (P3)
  (asm/section ".rodata")
  (asm/label msg) (asm/ascii "hello\n")
  (asm/set len (- . msg)))
```

- Instructions in the **SDM's operand order** with an explicit width where the mnemonic does not fix it: the forms are the machine
  level, the same for AT&T and Intel, so a print in the other syntax is free and is a judge (§8.1).
- The **spelling** (AT&T's suffix or none, `movabs`, Intel's `ptr` sizes, `0x` against decimal, aliases such as `jc`/`jb`/`jnae`, `sal`
  /`shl`) kept in `:full`'s metadata, not in the forms, as repr/llvm's `:full` (LLS18 (a)) and repr/c's.
- An explicit **encoding request** where the text makes one (`{disp32}`, `{vex3}`, `{evex}`, `.byte` sequences, `rex64`) kept as a key
  of the instruction (`{:enc :vex3}`): it changes the bytes.
- Directives as `asm/` forms one for one with GNU's, macros kept unexpanded in the forms (`asm/macro`, `asm/rept`), expanded by the
  assembler (AX1 reads them; AX2's assembler expands them as LLVM's `AsmParser` does).
- Plan 9 forms under `p9/` (`p9/TEXT`, `p9/DATA`, `p9/GLOBL`, instructions by Go's opcode names, `AMOVQ` as `MOVQ`), with a mapping
  to the machine level only through `obj/x86` (AX9).

### 4.3 The reader's acceptance

LLVM's parser checks operands (an immediate's range, a register's class, the instruction's existence in the mode) while it matches
(`X86AsmParser::MatchAndEmitInstruction`): acceptance is matching against the instruction table, so read's rejections need AX2's
table. AX1 reads syntax alone; AX2 adds the match, and with it the 96 error tests' messages (`invalid operand for instruction`,
`instruction requires: AVX512BW`, ...), each the fragment the test states.

## 5. Print

- **Canonical** in each syntax: AT&T as llvm-mc prints it (`-show-encoding`'s text is the `CHECK` line, so a canonical print of the
  forms equal to the test's line is the read level's own verdict, §8.1); Intel as llvm-mc's variant 1; Plan 9 as `go tool asm`'s
  listing.
- **`:full`**: comments (`#`, `//`, `/* */`, `;` in Intel and Plan 9), blank lines, tabs, the spellings of §4.2, CRLF, printed back
  byte for byte (LLS18 (a)'s shape). Every read corpus's `:full` is a print check.
- **Cross-syntax**: AT&T → forms → Intel text → forms equal; both assembled to the same bytes. Cheap and strict (P3 is exactly the bug
  it catches).

## 6. eval: x86i

### 6.1 The candidates (AXS6)

- (a) **An interpreter of decoded instructions** in Go forms (`go/arbace/x86i`), built by TamaGo's go as a light driver (as `go/arbace/oi`
  and `go/arbace/scm`), out of every key (`arbace_crop_paths`): decode once per address into the machine level, cache by page with
  invalidation on writes to code (self-modifying code, JIT tests), run with lazy flags (the result and operands of the last flag-setting
  instruction, flags computed when read: Blink's and QEMU's technique, here from the SDM).
- (b) The same with a translation of hot blocks to Go closures (a threaded interpreter): later, behind the same frame, if speed asks.
- (c) Binary translation of a whole program to Go forms (AX11): AOT's other meaning (AXS7 (b)).

Recommended: (a), (b) only with a measured need.

### 6.2 The machine

- Registers: 16 GPRs, RIP, RFLAGS, segment bases FS and GS (`arch_prctl`), the SSE/AVX file (16 × 256 bits; 32 × 512 and the k masks when
  AVX-512 is in), MXCSR, x87 (8 × 80 bits, its control and status words, tags), CPUID's answers as data (the feature level, AXS8).
- Memory: crt's `Mem`, a Text slab with real bytes added (ELF's `PT_LOAD` segments mapped at their addresses), page protections
  enforced (a store to code faults unless writable), a fault a `SIGSEGV` with its address (crt's `Fault`), alignment faults where the SDM
  gives them (`movaps`), `#DE` as `SIGFPE`, `ud2` and undefined encodings as `SIGILL`, `int3` as `SIGTRAP`.
- Floating point: SSE arithmetic as Go's IEEE `float32`/`float64` in round-to-nearest; MXCSR's other rounding modes, DAZ and FTZ by a
  software path (crt's limbs or a small softfloat); x87's 80-bit arithmetic in software (exact for `+ - * / sqrt`, the conversions, the
  compares); x87's transcendental instructions (`fsin`, `fyl2x`, `f2xm1`, `fpatan`) are implemented to Intel's documented accuracy, not
  bit for bit: their results differ between Intel and AMD parts, a class (`x87-transcendental`, judged by the test's own tolerance where
  it has one, AXS10).
- Kernel: crt's `Sys.Syscall` for the `syscall` instruction (rax and the six argument registers), extended where programs need it
  (`clone` for threads later, `rt_sigreturn` and signal frames as the kernel builds them, `vdso` absent: musl falls back to the syscall).
- Start: the kernel's process start (an ELF loader: `PT_LOAD`, `PT_INTERP` refused (static only, AXS11), `PT_TLS`, the stack with argc,
  argv, envp and auxv: `AT_PHDR`, `AT_ENTRY`, `AT_PAGESZ`, `AT_RANDOM`, `AT_HWCAP`).

### 6.3 Breadth, in order

1. The integer ISA of x86-64-v1 (about 300 mnemonics with their flags), `syscall`, the string instructions with `rep`, `cmpxchg`, `xadd`,
   `lock` (one thread), `cpuid`, `rdtsc` (a counter), `rdrand` (crt's `getrandom`).
2. SSE and SSE2 (the baseline every x86-64 program may use: gcc's `-O2` float code), x87 (musl's `long double` math is x87 `.s`).
3. SSE3 to SSE4.2, AVX, AVX2, BMI1/2, FMA, F16C, AES-NI, SHA, VAES, PCLMUL (x86-64-v3 and the CPU's extras: everything the CPU can also
   run, so everything the lockstep judge covers).
4. AVX-512 (F, BW, DQ, VL, ...), AVX10, AMX, APX: only x86i can run them on this host (§8.3); read, print and encode come first.

### 6.4 The bridge to C (AXS13)

A run corpus program calls libc. Two ways, both proposed, in order:

- **Bound to ci's musl** (eval): an undefined symbol of the program (`printf`, `malloc`) is bound to the C forms' function of ci's musl;
  a call from x86i to such an address is a call gate: the SysV ABI's arguments read from the registers and the stack (`c/target/abi.edn`'s
  classification), ci's function run on the same `Mem`, its result put back in rax/xmm0. A C function pointer called back into assembly
  (`qsort`'s comparator) goes the other way. Both share crt's address space, so pointers need no translation. This is the cheap way to
  run gcc's `-S` output of the run corpora, and the way C's `by-design` inline assembly and goi's assembly class can run (§6.5).
- **Assembled musl** (AOT, and eval's second level): musl's C compiled to assembly by clang `-S` at the oracle tier (a pack, §9), its 32
  `.s` files as written, all assembled by Arbace into a `libc.a` of Arbace's making; the program linked statically against it; then x86i
  and the CPU run the same executable, bit for bit.

### 6.5 What the other crops gain

- C and C++: inline assembly (`asm volatile`, gcc's extended asm with its constraints) run by x86i on ci's memory: the operands bound by the
  constraint letters (`r`, `m`, `a`, `=&r`, ...), the template read by repr/asm. C's `by-design` class for inline assembly shrinks (470 of
  gcc's i386 tests hold inline assembly; 5 programs of C's check fail on `known asm`).
- LLVM IR: the lowering's `:inline-asm` refusals, by the same bridge.
- Go: goi's assembly class (9 programs) and the leaf functions of std's `.s` (`crypto/*`, `math/big`, `internal/bytealg`): called from goi
  by ABI0 (arguments on the stack at `FP`) or ABIInternal (registers), x86i running the machine code `obj/x86` makes from the Plan 9
  source. Leaf `NOSPLIT` functions with no call into Go first; the runtime's own assembly (`g`, stack switching) never (AX9).

## 7. AOT: Arbace's assembler and linker

### 7.1 What AOT means here (AXS7)

- (a) **Native**: the forms encoded to machine code by Arbace's encoder, written as an ELF64 relocatable object by Arbace's ELF writer,
  linked statically by Arbace's linker into an executable, run by the CPU. The level the brief names, and the one no other crop has: the
  bytes are Arbace's.
- (b) **Translated to Go**: the program's instructions translated to Go forms over x86i's machine state (binary translation), built by
  TamaGo's go with cf2g's musl: the other crops' meaning of AOT, and the one that runs on an arm64 host and in the box.

Recommended: (a) for v1, (b) as the optional AX11.

### 7.2 The encoder

- The table of instruction forms (opcode, operand kinds, encoding: legacy prefix, REX, VEX, EVEX, map, ModRM, immediate sizes) from one
  source (AXS4), and the **choices** where several encodings exist: the shortest displacement and immediate, the register-register
  direction (`89` against `8b`), VEX2 against VEX3, `mov $imm` as `c7` or `b8` (movabs), jumps by relaxation to a fixed point (short
  where the target is in range, long for an undefined or preemptible symbol: P2's case decides the reference per corpus, AXS5).
- 16- and 32-bit modes (`.code16`, `.code32`, MC's `-32`/`-16` files, 221 files) for encoding only.
- Fixups and relocations: `R_X86_64_64`, `PC32`, `PLT32`, `GOTPCREL`, `GOTPCRELX`/`REX_GOTPCRELX` (with the linker's relaxation), `32`,
  `32S`, `TPOFF32`, `GOTTPOFF`, `TLSGD`/`TLSLD` (later), `SIZE32/64`.

### 7.3 The ELF writer and the linker

- **Writer**: ELF64 relocatable objects: sections and their flags (`.text`, `.data`, `.bss`, `.rodata`, comdat groups, `.note.GNU-stack`),
  symbols (binding, visibility, type, size), `.rela` sections, `.eh_frame` from the `.cfi_*` directives (CIE/FDE as GNU writes them),
  `.debug_line` from `.loc`/`.file` (later). Shared with arm64 (§13).
- **Linker**: static executables (`ET_EXEC`, then static PIE), the sections merged by a fixed script (ld's default for the static case),
  relocations applied, `_start` from musl's `crt1` (assembled by Arbace), `PT_TLS`, archives pulled member by member as ld does.
  Dynamic linking is out of v1 (AXS11).
- **Reader**: `debug/elf` (Go's, in `tamago/goroot` as forms) to read objects back; x86asm to disassemble them.

## 8. The judges

Every judge is an own verdict but the oracle tier's (§9).

### 8.1 read and print

- The forms read, printed (canonical and `:full`), read again: equal; `:full` byte for byte.
- `MC/X86`'s `CHECK` lines: the canonical print of an instruction equal to the text the test states (`-show-encoding`'s text),
  normalized as FileCheck does (blanks collapsed; `{{.*}}` patterns honoured or the line not used: LL9a's rule for lli's tests).
- Error tests (`not llvm-mc`, Go's `// ERROR`): rejected, with the stated fragment.
- Cross-syntax (§5): AT&T and Intel to the same forms.
- Disassembler tests (P7): the byte string decoded by x86i's decoder, printed, equal to the stated instruction (a read-level check of the
  decoder, beside: it judges the machine level, not text → forms).

### 8.2 AOT

- The encodings: each instruction's bytes equal the test's `encoding: [...]` (MC) or the comment's hex (Go's `amd64enc.s`, through the
  Plan 9 → machine-level map). 106,075 + 10,709 + the AVX-512 files' own verdicts with no tool run.
- The object read back: Arbace's ELF reader and x86asm's decoder give back the forms' machine level (a round trip through bytes).
- The executable run by the CPU: its own verdict (exit status, stated output).

### 8.3 eval

- The program's own verdict (exit 0 and no `abort`, the stated output: gcc's `dg-output`, musl's libc-test's `t_status`).
- Where none: **the CPU's run** of the same executable (Arbace-assembled, AOT's): stdout, stderr and status equal (AXS9).
- **Lockstep** (AXS9): a tracer of Arbace's own (Go, `CGO_ENABLED=0`, `syscall.PtraceSingleStep` and `PtraceGetRegs`; a `tools/` program
  built by TamaGo's go) runs the executable natively one instruction at a time and compares the registers (and the flags, masking those
  the SDM leaves undefined per instruction) with x86i's after each instruction; the first divergence names the instruction. The strictest
  judge the row can have, and no toolchain in it. Syscalls are stepped over and their results fed to x86i (a recorded kernel), so the two
  runs see the same world.
- What the CPU cannot run (AVX-512, AMX, APX on this host): the own verdicts alone, and the encodings; the lockstep at the oracle tier on
  a host that has them, or Intel's SDE (proprietary, by hand) never.

## 9. Independence and the oracle tier

| stage | pipeline (check time) | judge (check time) | score (proposed) |
|---|---|---|---|
| read | repr/asm, Arbace's alone; inputs hand-written (MC, Go, musl, gas tests: 1) or made by gcc/clang `-S` and committed as packs (0.5 for those cases) | own verdicts | case-weighted, about 0.85 to 0.95 |
| eval | x86i, crt, ci's musl | own verdicts; the CPU and the lockstep (AXS9 (a): 1) | 1 for hand-written inputs, 0.5 for packed `-S` inputs |
| AOT | Arbace's encoder, ELF writer, linker; musl's asm from a pack (0.5) or freestanding programs (1) | own verdicts, the CPU | as eval |
| print | repr/asm | `:full` bytes, the forms | 1 |

**The oracle tier** (`:phase :oracle`, `bin/gate --oracle`; doc/ORACLE-RECORDS-SURVEY.md's design): the host `:binutils` (gas, ld,
objdump; a new host id beside `:gcc`) and `:llvm` (llvm-mc, llvm-objdump, llvm-readobj), by hand at a tag:
- packs (`test/asm/records/KIND-CORPUS.tsv`, the bodies in `.tmp/oracle-packs/`): `asm-amd64-gcc-S` (gcc `-O0`/`-O2 -S` of the C run
  corpora and of `gcc.target/i386`'s run tests: inputs, GPL test data), `asm-amd64-clang-S` (clang `-S` of the same and of LLVM IR's
  pack rt: inputs), `asm-amd64-musl-S` (clang `-S` of musl's C: AOT's libc), `asm-amd64-gas-obj` and `asm-amd64-mc-obj` (the objects
  and disassembly of every read corpus by each reference: compared at the oracle tier only);
- steps `asm-amd64-live` (gas and llvm-mc on the corpora against Arbace's objects, section by section, relocations by symbol) and
  `asm-amd64-records-verify` (the packs remade every 25th entry, ORS7 (a)'s crossed pair).

No check-time step runs `as`, `ld`, `objdump`, `llvm-mc` or `gcc`. TamaGo's `go tool asm` is not a host toolchain (doc/CROPS-SCORE.md), so
Plan 9's encodings may also be compared with it live, as Go's judge is gc (CSP2); proposed only beside the own verdicts (AXS15).

## 10. The corpora

| corpus | where (offline) | size | levels | own verdict | licence (as data) |
|---|---|---:|---|---|---|
| **llvm/test `MC/X86`** | the pinned llvm-project 23.1.5 tarball (`.tmp/llvm/`, 179 MB; 23.1.4's extracted `llvm/test`) | 632 files, 17 MB, 427,555 lines; 106,075 encodings; 96 error tests; 161 Intel; 221 32-bit | read, print, AOT (encodings) | encodings, CHECK text, errors | Apache-2.0 WITH LLVM-exception |
| `MC/AsmParser` (x86-gated) | same | 162 files, 215 KB, 4,421 lines | read, print (the directive language) | CHECK text | same |
| `MC/ELF` | same | 265 files, 547 KB | AOT (objects: CHECK lines of llvm-readobj's output, a judge only where the test states fields) | partial | same |
| `MC/Disassembler/X86` | same | 187 files, 1.8 MB, 13,545 byte strings | the decoder (§8.1) | stated instruction | same |
| `tools/llvm-objdump/X86` | same | 90 files, 278 KB | the decoder, ELF reading | partial | same |
| **Go's `cmd/asm` testdata** | `/root/tamago-go/src/cmd/asm/internal/asm/testdata` | amd64: 5 files, 12,252 lines; `avx512enc/`: 17 files, 14,302 lines; 386: 2 files | read, print, AOT (Plan 9) | encodings, `// ERROR` | BSD-3 |
| **Go's amd64 `.s`** | `tamago/goroot` (in the checkout), TamaGo's `src` | 67 files, 33,780 lines (TamaGo's tree: 109 amd64 `.s` with cmd's) | read, print; eval of leaf functions (AX9) | their packages' Go tests | BSD-3 |
| **musl 1.2.6's x86-64 assembly** | `.tmp/c1/musl/musl-1.2.6.tar.gz` | 32 `.s`/`.S`, 711 lines; 18 `.c` with inline assembly | read, print, eval, AOT | libc-test (fetch) or harnesses | MIT |
| **gcc 15.2.0 `gcc.target/i386`** | `.tmp/c1/gcc/gcc-15.2.0.tar.xz` (101 MB) | 9,288 files, 14 MB; 3,504 `dg-do run` (1,633 without AVX-512/AMX/APX/AVX10/`-m32`); 470 with inline assembly; 4,234 `scan-assembler`; 114 `dg-do assemble`; 37 `-masm=intel` | eval, AOT via the `-S` pack; inline assembly via ci (§6.5) | exit status (`abort`) | GPL-3.0-or-later (test data, LLS26 (a)'s rule) |
| `gcc.target/x86_64` | same | 153 files; 16 `.s`/`.S` (the ABI tests' callers in assembly) | eval (ABI bridge) | exit status | same |
| **C's run corpora through gcc/clang `-S`** | packs (§9) from C's pinned lists | 1,532 torture execute programs × levels, C's other run corpora | eval, AOT | exit status | GPL (gcc's tests) |
| **LLVM IR's pack rt through `llc`/clang `-S`** | pack from `test/llvm/rt-builds.tsv` | 257 modules | read, eval where a `main` | as LLVM's | as LLVM's |
| **synthetic programs** by instruction family | generated (a generator committed, as LLVM's GT8.6) | about 300 to 600 programs (each family's instructions over edge values, results printed by `write`) | all four | the CPU and lockstep | Arbace's |

**To fetch** (the user's OK, by pin; none needed before AX2):
- **binutils 2.45.1** (`binutils-2.45.1.tar.xz`, about 28 MB, to verify): `gas/testsuite/gas/i386` (about 3,000 files with `.d` expected
  dumps: own verdicts as objdump's text, to verify). GPL test data; **tc-i386.c and opcodes/ never read**.
- **Go's `x/arch`** at the revision TamaGo's vendor names (`x86/x86.csv`, x86asm's `testdata/decode.txt`: about 50,000 decodings, to verify;
  `x86/x86spec`): BSD-3.
- **Intel XED** (if AXS4 (c)): its `datafiles/`, Apache-2.0.
- **musl's libc-test** (MIT): eval's harness for musl's `.s` functions (`string`, `math` long double, `setjmp`, `fenv`).

## 11. Layout, row and budget

### 11.1 Layout (AXS3, with §13's sharing)

| piece | proposed |
|---|---|
| reader, printer | `arbace.repr.asm` (`arbace/repr/asm{.ae,/}`): `lex`, `gnu` (the directive language, shared), `expr`, `full`, `print`; `amd64/{att,intel,plan9}`; arm64's beside |
| table, encoder, assembler | `arbace.asm.amd64.{table,encode,match,relax}`; `arbace.asm.assemble` (sections, fixups, shared) |
| ELF writer, reader, linker | `arbace.asm.elf`, `arbace.asm.link` (shared; the per-architecture relocations in `arbace.asm.amd64.reloc`) |
| emulator | `go/arbace/x86i` (Go forms, hand-written, a light driver built alone by TamaGo's go), the frame `go/arbace/emu` (shared: loader, memory over crt, syscalls, the call gate, the trace) |
| decoder | `go/arbace/x86i/x86asm` (x86asm converted by repr/go, BSD-3, its origin in doc/ARBACE.md, LICENSE.md's Go section) |
| the tracer | `tools/asmtrace` (Go, ptrace, shared) |
| check, judges | `arbace.asm.check` (shared), `test/asm/` (`corpora.edn`, `check-*-linux-amd64.edn`, `divergences{,-read,-aot,-print}.edn`, `controls.edn`, `records/`) |
| commands | `bin/repr-asm read|print|roundtrip|check`, `bin/x86i run|check|check --aot|trace|divergences`, `bin/asm-ld` (or `bin/repr-asm link`); Makefile `asm-*` |
| spec | `doc/asm/SPEC.md` (the forms, both architectures), `doc/asm/AMD64.md` (the machine, the encoder, x86i) |

All of it in `arbace_crop_paths` (evaluated from the checkout, out of every key) until an embedding is decided (as LLS16).

### 11.2 The row

`test/reap/cells.edn` gains `:asm-amd64` (AXS1) with four cells as §1, an **intermediate row** printed after LLVM's; `test/crops/crops.edn`
its crop (`:steps #"^(asm-amd64|x86i)-"`, controls by family); `test/crops/independence.edn` its five stages. The gate's fragment
`test/gate/crops/asm-amd64.edn` (GT9's way), `:mem-gib` declared, essential at most 3 GiB.

### 11.3 The budget (AXS16)

GT8's crop share is about 35 minutes nightly and 2.4 hours clean. Proposed: **half a share** (as LLS17 for LLVM IR), the encodings and
read checks being fast (P5: Go's 10,709 lines in 0.06 s by gc's assembler; Arbace's to measure, expect 10 to 50 times slower), the runs
the cost: essential about 30 s (AX1's files, a few hundred encodings, three synthetic programs on x86i, one lockstep); nightly the whole
`MC/X86` and Go's tests (estimate a few minutes), the run corpora by rotation; clean the rest.

## 12. Plan (AX1-AX10)

Working days, as the other surveys give them; each milestone ends with its check green and its records. Days in **[S]** are shared
with arm64 if §13 is adopted (counted once across the two plans; here in full).

| milestone | content | exit criteria (measurable) | days |
|---|---|---|---:|
| **AX1** read and print, AT&T | `doc/asm/SPEC.md` [S]; `test/asm/corpora.edn`; the lexer and GNU's directive language [S]; AT&T operands; `:full`; `bin/repr-asm read|print|roundtrip` | **musl's 32 files and `MC/AsmParser`'s 162 read, printed `:full` byte for byte, read again equal**; then `MC/X86`'s AT&T files (459) at the same levels, the rest classed | 4-7 [S 2] |
| **AX2** the table and the encoder | the instruction table (AXS4); matching (acceptance, the error messages); encoding with LLVM's choices and gas's (AXS5); relaxation; 16/32-bit modes | **`MC/X86`'s 106,075 stated encodings and 96 error tests** equal or classed; the CHECK text printed equal | 8-14 |
| **AX3** Intel syntax | Intel's operand grammar and MASM's symbol rule (P3); the Intel print | `MC/X86`'s 161 Intel files at AX1's and AX2's levels; the cross-syntax judge over every AT&T file | 3-5 |
| **AX4** objects and the linker | ELF64 writer (symbols, relocations, `.eh_frame` from CFI) [S]; static linker [S]; Arbace's reader back (debug/elf); x86asm converted (the decoder) | musl's 32 objects read back to their forms; P4's program and the first synthetic programs assembled, linked, **run by the CPU** with their own verdicts; `MC/Disassembler/X86`'s 13,545 decodings | 6-10 [S 4] |
| **AX5** x86i, the integer machine | the frame (loader, Mem with real Text, syscalls, signals) [S]; x86-64-v1's integer ISA, SSE2, lazy flags; the tracer [S] | the synthetic integer and SSE2 programs pass at x86i, **lockstep-equal to the CPU**; freestanding programs from AX4 | 8-14 [S 4] |
| **AX6** the bridge and the run corpora | the call gate to ci's musl (SysV); the packs `asm-amd64-gcc-S`, `-clang-S` (oracle tier, by hand); x87 | C's torture execute programs at `-O0` and `-O2` through gcc's `-S`, run by x86i with ci's musl: none failing, each non-pass classed; gcc's 16 x86_64 ABI tests | 5-8 |
| **AX7** AOT whole | musl assembled by Arbace from `asm-amd64-musl-S` plus its `.s` (`libc.a`); the run corpora linked statically and run by the CPU; x86i on the same executables | the run corpora at AOT: own verdicts; x86i's run equal to the CPU's on every one | 4-7 |
| **AX8** the vector ISA | SSE3-SSE4.2, AVX, AVX2, BMI, FMA, F16C, AES, SHA, PCLMUL in x86i; MXCSR's modes; x87 complete | `gcc.target/i386`'s run tests within the CPU's features (1,633 candidates; through the `-S` pack) at eval and AOT; lockstep on the synthetic vector programs | 8-15 |
| **AX9** Plan 9 | Go's syntax read into `p9/` forms and printed (`:full`); `obj/x86` from `tamago/cmd/` as the machine level; leaf functions of std's `.s` run by x86i from goi (ABI0) | Go's amd64 testdata's encodings equal; the 67 `.s` files round-trip; `math`, `internal/bytealg` and one `crypto` package's tests in goi on their assembly | 5-9 |
| **AX10** the row | `cells.edn`, classes, controls (GTS81 (a): a case judged against another's reference must fail, per family), steps (`:parts`), Makefile, `crops.edn`, `independence.edn`, the budget measured (`bin/gate --costs`) [S 1] | `bin/reap`: the four cells checked, none failing; `bin/crops-score --crop asm-amd64` printed | 2-3 |
| *AX11* (optional) translation to Go | x86-64 → Go forms (blocks, x86i's state), cf2g's musl: AOT on any host, the box, arm64 | AX7's corpora equal | 10-20 |
| *AX12* (optional) AVX-512 and beyond | EVEX in x86i, masks, AVX10, APX, AMX: judged by own verdicts alone on this host | gcc's AVX-512 run tests' own verdicts | 8-14 |

**Total without AX11 and AX12: about 53 to 92 working days, of which about 11 to 15 [S]** (and the shared work's other half lands in
arm64's plan). For scale: LLVM IR's LL1-LL9 31 to 52, with its reader and lowering in hand; the surveys have run under their estimates.
The least known are AX2's table (its source decides a week either way) and AX8's breadth. Re-estimate at AX2's end.

**Sonnet and Opus** (the user's split): Sonnet for AX1's grammar and `:full`, AX3, AX4's writer, AX6's packs and classes, AX9's
conversion, AX10. Opus for the table and the encoder's choices (AX2), the lazy flags and the x87 (AX5, AX6), the call gate (AX6), the
vector semantics (AX8), the lockstep's undefined-flags masks.

**Re-seed needs**: none until an embedding (§11.1).

## 13. Shared with arm64 (AS1-AS8)

The two instruction sets share nothing at the machine level, and nearly everything around it. Proposed for both surveys to adopt, so
the two plans merge into one with a common trunk (call it AS0, about 15 to 25 days, built once) and two branches:

| shared piece | what | where |
|---|---|---|
| **AS1 one tool family** | `bin/repr-asm --arch amd64|arm64`, one namespace tree `arbace.repr.asm.*` with per-architecture parts, one spec `doc/asm/SPEC.md`; the evaluators `bin/x86i` and `bin/a64i` (or one `bin/asmi --arch`) over one frame | §11.1 |
| **AS2 the forms' frame** | `asm/file` with `{:arch :syntax}`; directives as `asm/` forms (the same GNU directive set both architectures use: sections, symbols, data, macros, `.cfi_*`, expressions, local labels); instructions as `(OPCODE {attrs} OPERANDS...)` in each architecture's vendor manual's operand order (the SDM's dst-first, Arm's ARM dst-first); operand forms `reg`, `imm`, `mem`, `sym`, `local` with architecture-specific keys; `:full`'s metadata layout | §4.2 |
| **AS3 GNU's directive language and expressions** | one reader for both (gas's language is the same on every target, with a few per-target directives: `.code32`, `.intel_syntax` for x86; `.inst`, `.arch_extension`, `.xword` for arm64); the expression evaluator and relocation specifiers (`@PLT`, `@GOTPCREL` for x86; `:lo12:`, `:got:` for arm64) | §4.1 |
| **AS4 the assembler's driver** | sections, the symbol table, fixups, relaxation to a fixed point, CFI → `.eh_frame` (the CIE/FDE writer; only the register numbering and the initial instructions differ), `.loc` → `.debug_line` | §7.2 |
| **AS5 ELF writer, reader and static linker** | ELF64 for `EM_X86_64` and `EM_AARCH64`: the writer, `debug/elf` as the reader, the linker with per-architecture relocation application (x86's `R_X86_64_*`, arm64's `R_AARCH64_*` with veneers) | §7.3 |
| **AS6 the emulator's frame** | `go/arbace/emu`: the ELF loader and the process start (stack, auxv), memory over crt's `Mem` with real Text and protections, the decoded-instruction cache with invalidation, the syscall layer (crt's `Sys`, with the per-architecture numbering: arm64 Linux's generic table, `svc #0`, x8 and x0-x5), signals and their frames, the call gate to ci's musl (ABI per architecture: SysV x86-64, AAPCS64), the trace format | §6 |
| **AS7 the judges** | the own verdicts' readers (llvm/test's `CHECK`/`encoding:` lines and FileCheck's blank rule; Go's `// hex` and `// ERROR`; gas's `.d` files if fetched), the cross-check by the CPU, the lockstep tracer (`tools/asmtrace`: `PTRACE_SINGLESTEP` and the regsets, amd64 here, arm64 on the Mac VM's Linux or by KVM), the controls, `arbace.asm.check`; the oracle tier's packs by one recipe (`asm-ARCH-KIND-CORPUS`) | §8, §9 |
| **AS8 the row's shape** | two intermediate rows `:asm-amd64` and `:asm-arm64` (AXS1 (a)), one GT9 fragment each, one budget split between them | §11 |

What is **not** shared: the instruction tables, the encoders, the decoders (x86asm for amd64; Go's `x/arch/arm64/arm64asm` or LLVM's for
arm64), Intel syntax and Plan 9's x86 half (`obj/x86` against `obj/arm64`), the machines' semantics, the floating-point corners (x87 is
amd64's alone). Plan 9 is shared as a reader (one `p9/` grammar, `cmd/asm`'s lexer and parser are architecture-generic) and split at
`obj/x86` and `obj/arm64`.

The order across the two plans: AS0's trunk first (AX1's `[S]` part, AX4's, AX5's frame, AX10's), each architecture's AX2 (encoder)
right after; the amd64 branch can run eval natively from day one (the CPU on this host), the arm64 branch on the Mac VM or under
`qemu-aarch64` at the oracle tier (arm64's survey decides).

## 14. Proposals (AXS1-AXS16)

Each with options, the recommended one named.

- **AXS1 The row.** (a) two intermediate crops `:asm-amd64` and `:asm-arm64`, each its own REAP row, scored apart (their ISAs share no
  cases), printed after LLVM's; (b) one crop `:asm` with both architectures' cases in one row; (c) a part of C's row. *Recommended:
  (a)*: completeness and independence differ by architecture (the CPU judges amd64 here, not arm64).
- **AXS2 The forms.** (a) the machine level in the SDM's operand order with the spelling in `:full`'s metadata, one vocabulary for AT&T
  and Intel (§4.2); (b) each syntax its own forms (AT&T's order and suffixes as written). *Recommended: (a)*: P3 shows the text is not the
  instruction; (b) makes Intel and AT&T two crops' worth of forms.
- **AXS3 The layout.** (a) §11.1 (`arbace.repr.asm`, `arbace.asm.*`, `go/arbace/x86i` on `go/arbace/emu`, `test/asm/`, `doc/asm/`);
  (b) everything under one `arbace/asm/` namespace including the reader. *Recommended: (a)*, the crops' pattern (repr/ the bridge).
- **AXS4 The instruction table's source.** (a) LLVM's X86 `.td` files through the host's `llvm-tblgen --dump-json`, filtered and
  committed as data once at the oracle tier (Apache-2.0 WITH LLVM-exception), so the matcher's acceptance and the encoder's choices are
  MC's, the judge's; (b) Go's `obj/x86` optab (BSD, already forms; its choices are Go's, its coverage misses system instructions and
  some legacy forms); (c) Intel XED's datafiles (fetch, Apache-2.0; the most complete, its choices XED's); (d) Arbace's own from the SDM
  by hand. *Recommended: (a)*, (c) as the semantics' cross-check if fetched; (b) for Plan 9 only (AX9).
- **AXS5 The encoder's choices.** (a) a mode per reference (`:llvm`, `:gnu`), MC's corpora judged in `:llvm`, gcc `-S`'s in `:gnu`,
  every difference listed by rule (P2's jump to a global, ...); (b) LLVM's alone, gas's differences classed. *Recommended: (a)*, the GNU
  rules written from gas's observed output only (never its source).
- **AXS6 eval's route.** (a) an interpreter of decoded instructions with lazy flags (§6.1); (b) a threaded interpreter at once; (c) binary
  translation as eval. *Recommended: (a)*.
- **AXS7 AOT's meaning.** (a) native: Arbace's encoder, ELF and linker, run by the CPU; (b) translated to Go forms (AX11). *Recommended:
  (a)* for v1, (b) optional: it is what runs amd64 code on arm64 and in the box.
- **AXS8 x86i's feature level (CPUID).** (a) x86-64-v3 plus the CPU's extras (AES, SHA, VAES, PCLMUL), so the lockstep covers every
  instruction x86i claims; AVX-512 off until AX12; (b) v1 only (SSE2), the rest refused; (c) everything at once. *Recommended: (a)*,
  reached through (b) in AX5.
- **AXS9 The CPU and the lockstep as judges.** (a) the CPU running Arbace's executable is not a toolchain: its run and the lockstep trace
  count as own judges (1), recorded in doc/CROPS-SCORE.md as a ruling beside TamaGo's go; (b) they count 0.5 (a machine Arbace did not
  make); (c) not judges, own verdicts only. *Recommended: (a)*: every crop runs on the CPU; it is the subject's machine, not a tool.
- **AXS10 Floating-point corners.** (a) SSE bit-exact (IEEE), x87 arithmetic bit-exact in software, x87 transcendentals to the documented
  accuracy with a class `x87-transcendental` where the CPU's bits differ; (b) x87 refused. *Recommended: (a)*: musl's `long double`
  math is x87.
- **AXS11 Linking.** (a) static only in v1 (`ET_EXEC`, then static PIE), dynamic (`PT_INTERP`, PLT/GOT at load) later; (b) dynamic in v1.
  *Recommended: (a)*: musl's static link is the crops' (C, C++).
- **AXS12 The evaluator's name.** (a) `x86i` (the crops' pattern: goi, ci, llvmi); (b) `asmi --arch amd64`; (c) `amd64i`.
  *Recommended: (a)* with AS1's shared driver, unless arm64's survey prefers one `asmi`.
- **AXS13 The bridge to C.** (a) both, in order: undefined symbols bound to ci's musl (eval), then musl assembled by Arbace from clang
  `-S` (AOT); (b) only the assembled musl; (c) only freestanding programs. *Recommended: (a)*.
- **AXS14 The fetches.** (a) binutils 2.45.1's source tarball for `gas/testsuite` only (test data; tc-i386.c and opcodes/ never
  opened), Go's `x/arch` at TamaGo's vendored revision, musl's libc-test; XED only with AXS4 (c); (b) none: the host's corpora (§10)
  alone. *Recommended: (a)*, after AX2 (nothing before needs them).
- **AXS15 Go's assembler as a live judge for Plan 9.** (a) TamaGo's `go tool asm` beside the own verdicts (it is the seed's toolchain,
  not a host one, CSP2's ruling); (b) own verdicts only. *Recommended: (a)*: Plan 9 has no other reference and gc's is pinned.
- **AXS16 The budget.** (a) half a crop's share, as LLVM IR's (LLS17); (b) a full share; (c) a quarter, sharing arm64's half.
  *Recommended: (a)*, both architectures together a full share.

## 15. What to decide first

1. **AXS1, AS1, AS8**: two intermediate crops, one tool family; the merge of the two surveys into one plan.
2. **AXS2**: the forms as the machine level (the spec waits on it).
3. **AXS4, AXS5**: the table's source and the encoder's references (AX2's week either way).
4. **AXS9**: the CPU and the lockstep as own judges (the independence score depends on it).
5. **AXS7**: AOT native, translation optional.
6. **AXS14**: the fetches (only from AX2 on).

## 16. Risks

- **Breadth.** x86's mnemonics with AVX-512 are over 1,500; each with flags and faults. The table makes the encoder mechanical; the
  emulator's semantics are not. Mitigation: the order of §6.3 and the lockstep, which finds the first wrong instruction itself.
- **The two references disagree** (P2) and so would Arbace with one of them; AXS5's modes and a rule list per difference keep that
  visible rather than classed in bulk.
- **Intel syntax's semantic ambiguity** (P3): a reader that decides operand kinds before the symbols are known reads a different
  program. Mitigation: AXS2 (a), the forms the machine level, judged by encodings.
- **x87 transcendentals and undefined flags**: results that differ between CPU vendors; a lockstep run on AMD would teach x86i AMD's
  bits. Mitigation: the SDM's undefined flags masked by instruction, `x87-transcendental` a class with the documented accuracy.
- **What the CPU cannot run** (AVX-512, AMX, APX here): own verdicts only; a host change (a CPU with AVX-512) would add a judge, not move one.
- **ptrace on the host**: the lockstep needs `PTRACE_SINGLESTEP` (allowed for a child process; to verify under the host's settings,
  `kernel.yama.ptrace_scope`); in the box, KVM's single-step instead (parked).
- **Speed**: an interpreter in Go of about 50 to 200 host instructions per guest instruction (Blink's order without its JIT, to verify):
  gcc's `-O2` torture programs in seconds, larger programs need AX6's rotation or (b) of AXS6.
- **Licences**: binutils and GCC's sources never read for porting; gcc's tests are test data (LLS26 (a)); the SDM is a reference, its
  tables not copied; LLVM's tables committed as generated data under Apache-2.0 WITH LLVM-exception (in LICENSE.md already); x86asm and
  obj/x86 under Go's BSD-3 (in LICENSE.md already); Blink's ISC added to LICENSE.md only if code is ported.
- **The box**: x86i is user-mode Linux; Arbace's bare-metal goal (TamaGo, the box) needs a system-mode machine (paging, interrupts, the
  doorbell): later, on the same decoder and semantics, KVM the judge (parked by the user, 2026-10-26).

## 17. Sources

- The host: Alpine `binutils` 2.45.1 (`as --version`), LLVM 23.1.5's tools (`/usr/lib/llvm23/bin`), clang 23.1.5, gcc 15.2.0, musl
  1.2.6, `qemu-aarch64`, gdb; `/proc/cpuinfo`'s flags; TamaGo's go1.27.1 (`/root/tamago-go`, `pkg/tool/linux_amd64/asm`).
- Pins and tarballs: `llvm-project-23.1.5.src.tar.xz` (`.tmp/llvm/`, `test/cpp/corpora.edn`'s `:llvmorg-23.1.5`; 197,239 entries,
  `llvm/lib/Target/X86` 230 files, 11.2 MB); `gcc-15.2.0.tar.xz` (`.tmp/c1/gcc/`, `test/c/corpora.edn`'s `:gcc-15.2.0`); `musl-1.2.6.tar.gz`
  (`.tmp/c1/musl/`).
- In llvm-project 23.1.4/23.1.5: `llvm/test/MC/{X86,AsmParser,ELF,Disassembler/X86}`, `llvm/test/tools/llvm-objdump/X86`,
  `llvm/lib/MC/MCParser/AsmParser.cpp`, `llvm/lib/Target/X86/AsmParser/X86AsmParser.cpp`, `llvm/lib/Target/X86/MCTargetDesc/`,
  `llvm/lib/Target/X86/X86Instr*.td` (read by name and size; the code to be read at AX2).
- In TamaGo's Go tree (go1.27.1): `src/cmd/asm/internal/{lex,asm,arch}`, `src/cmd/asm/internal/asm/testdata/{amd64,amd64enc,amd64enc_extra,
  amd64error,amd64dynlinkerror}.s`, `testdata/avx512enc/`, `src/cmd/internal/obj/x86/{asm6,avx_optabs,evex,obj6,ytab}.go`,
  `src/cmd/vendor/golang.org/x/arch/x86/x86asm/{decode,avx,avx_tables,tables,gnu,intel,plan9x,inst}.go`, `src/debug/elf/`,
  `src/cmd/link/internal/ld/elf.go`; in the checkout `tamago/cmd/go/cmd/{asm,internal/obj/x86}` (forms), `tamago/goroot` (the `.s` files).
- Arbace: CLAUDE.md; doc/llvm/LLVM-CROP-SURVEY.md (the model: §1, §9-§14, LL9a's own verdicts, ORS30 (a)); doc/CROPS-SCORE.md;
  doc/ORACLE-RECORDS-SURVEY.md §3; doc/C23-SURVEY.md (crt, ci's natives of musl's assembly, CS3 (a)); doc/go/GOI-SPEC.md (§1.1's
  assembly); `go/arbace/crt/{mem,syscall}.ae`; `test/crops/crops.edn`, `test/reap/cells.edn`, `test/c/divergences.edn`'s `by-design`,
  `test/cpp/divergences-lower.edn`'s `inline-asm`.
- Prior art (not fetched; licences to verify): Blink `https://github.com/jart/blink`; Intel XED `https://github.com/intelxed/xed`;
  box64 `https://github.com/ptitSeb/box64`; FEX `https://github.com/FEX-Emu/FEX`; iced `https://github.com/icedland/iced`; Zydis
  `https://github.com/zyantific/zydis`; fadec `https://github.com/aengelke/fadec`; Remill `https://github.com/lifting-bits/remill`;
  ACL2 x86isa `https://github.com/acl2/acl2` (`books/projects/x86isa`); K x86-64 `https://github.com/kframework/X86-64-semantics`;
  Go's `x/arch` `https://go.googlesource.com/arch`; musl libc-test `https://repo.or.cz/libc-test.git`; binutils
  `https://sourceware.org/pub/binutils/releases/binutils-2.45.1.tar.xz`.

## Appendix A: the scratch

The worktree's `.tmp/asm/` (about 42 MB, kept): `musl-1.2.6/` (its `src`, `arch/x86_64`, `crt`), `gcc-15.2.0/gcc/testsuite/gcc.target/
{i386,x86_64}` (extracted for the counts), `p2/` (P1-P2: each musl file's gas and llvm-mc objects and `.text` bytes), `hello.s`,
`hello-intel.s`, `h`, `h.o`, `hi.o` (P3, P4), `go1.o` (P5), `llvm-list.txt` (the 23.1.5 tarball's listing). The commands are in this
file's §3; each ran in seconds under 200 MB.

## As built: AX1 (first slice, 2026-11-09/10; branch `asm-ax1`)

The user's decisions of 2026-11-09 (~14:2x): two crops `:asm-amd64` and `:asm-arm64` in one directory (AXS1 (a), ARS1/SH1, AXS16 (a),
ARS15), the forms at the machine level, GNU syntax first, byte for byte and a canonical print (AXS2 (a)), Plan 9 later through Go's
`cmd/asm` (AXS15 (a)), the table from LLVM's `.td` (AXS4 (a)), one encoder mode per reference (AXS5 (a)), the CPU's run and the lockstep
as own judges (AXS9 (a)), AOT native (AXS7 (a)); readers first, then encoders and ELF.

Built: doc/asm/SPEC.md (the draft); `arbace.repr.asm` (`arbace/repr/asm.ae`) with `lex` (the lexer), `gnu` (the directive language,
expressions, statements, the canonical print of items), `amd64/att` (AT&T operands and instructions), `full` (mode `:full`'s layer);
`bin/repr-asm read|print|corpus extract|corpus lists|roundtrip [--record]|divergences`, the Makefile's `repr-asm-*`; `test/asm/`:
`corpora.edn` (the pins restated: musl 1.2.6's tarball, llvm-project 23.1.5's), `roundtrip-results-amd64.edn` (810 rows),
`divergences-read.edn` (six classes), `controls.txt` (three negative controls), `judge.ae`. The namespaces are a crop's
(`arbace_crop_paths`, `embed.ae`'s exclusions, the audit's `crop-rules`): evaluated from the checkout, in no key after this commit.
The run: all three corpora in about 50 s on stage 0 (four threads, under 1.5 GiB), no assembler.

| corpus | files | pass (read, `:full` byte for byte, `.ae` back equal, canonical re-read equal with no layer) | classed |
|---|---:|---:|---|
| musl 1.2.6 x86-64 `.s` | 32 (711 lines) | **32** | none |
| `MC/AsmParser` (split-file tests as parts; AArch64/ apart) | 157 | **139** (3,855 lines) | 15 `:error-test`, 1 `:error-input`, 1 `:intel`, 1 `:lexer-option` |
| `MC/X86` (620 files, one split into two) | 621 | **464** (345,290 lines, 88,344 items) | 152 `:intel` (AX3), 4 `:error-test`, 1 `:not-input` |

So every AT&T file of `MC/X86` passes at AX1's levels (the plan's 459; 464 by the RUN lines' own syntax option), and every file not
passing is in a class named from its test's own RUN lines. What differs from the plan: the forms are the syntax level in the
machine's operand order until AX2's table resolves the mnemonics (AXS17); `split-file` tests are read as their parts (AXS20). The
canonical layout is llvm-mc's (a tab after the mnemonic, `, ` between operands, decimal integers), so AX2 can hold the canonical print
to `MC/X86`'s CHECK lines.

Not built in this slice: the gate step and the REAP row (AX10; AXS21), the CHECK-text judge (AX2), Intel (AX3).

## Proposed

AXS1-AXS16 (§14) and AS1-AS8 (§13): the user decided AXS1, AXS2, AXS4, AXS5, AXS7, AXS9, AXS15, AXS16 as recommended (2026-11-09).
New with AX1 (each with options, the recommended named):

- **AXS17 The mnemonic until AX2.** (a) AX1's forms keep the mnemonic as written (lower case: `movl`, `cmovl`, `vmovaps.s`) and
  the operands reversed; AX2's table makes the machine level (`(mov {:w 32} ...)`), the spelling moving into the layer, and the
  recorded forms are rewritten then; (b) a hand-written table of the suffixes now (`movl` → `mov` 32), which cannot tell `cmovl`'s
  condition from a suffix without the table; (c) keep the written mnemonic for good, the machine level a separate pass.
  *Recommended: (a)*: the decision was the machine level (AXS2 (a)), and only the table (AXS4 (a)) can make it.
- **AXS18 The header form.** (a) `(asm/unit {:arch :syntax})`, the items after it as top-level forms; (b) the survey's `(asm/file
  {...} items...)`, which clashes with the `.file` directive's item `(asm/file ...)`. *Recommended: (a)*.
- **AXS19 Macro and repetition bodies.** (a) as text, `(asm/macro HEAD BODY END)`, `(asm/rept ...)`, `(asm/irp ...)`, and a macro's
  invocation `(asm/invoke NAME ARGS)` with its arguments as text: gas's own model (substitution is textual); AX2's assembler expands
  them; (b) parsed into items, with the substitutions as forms (`\arg`, `\()`): more structure, but altmacro and `\()` make bodies
  that are not statements until expanded. *Recommended: (a)*.
- **AXS20 split-file tests.** (a) read as their parts (`NAME.PART`), the files llvm-mc reads (6 files of the two MC corpora);
  (b) read whole, the `#--- name` lines comments. *Recommended: (a)*: (b) mixes a test's good and bad inputs in one file.
- **AXS21 The gate and the row.** (a) at AX10 as planned: `test/gate/crops/asm-amd64.edn` (`asm-read-essential`: musl and
  `MC/AsmParser`, about 10 s, under 1.5 GiB; `--full` `asm-roundtrip` with `MC/X86`, about 50 s), the row's cells, `crops.edn` and
  `independence.edn` together; (b) the essential step now, before the row. *Recommended: (a)*: the row's cells need AX2's
  judges to mean what the other rows' do; the round trip runs by hand meanwhile (`make repr-asm-roundtrip`).
- **AXS22 The corpus's source tree.** (a) extracted from the pinned tarballs by `bin/repr-asm corpus extract` into the main checkout's
  `.tmp/asm/src` (the clean clone's too, as `bin/llvm-corpus`), nothing of llvm/test committed; (b) the corpora committed under
  `test/asm/`. *Recommended: (a)*: 17 MB of tests, all of it behind one pin already on the host.


## As built: AX2 (first slice, 2026-11-10; branch `asm-ax2`)

On the light lane (stage 0, one run at a time, under 2 GiB, no lock, no LLVM tool in any check), the user's decisions of
2026-11-09 followed (AXS4 (a): the table from LLVM's `.td`; AXS5 (a): one encoder mode per reference, here `:llvm`), and
AXS17-AXS22 as recommended. doc/asm/AMD64.md describes what was built.

- **The table** (`asm/amd64/table.edn`, 8.9 MB, one entry a line): generated from llvm-project 23.1.5's X86 TableGen files (144,000
  lines in 106 files, extracted with the corpora: `test/asm/corpora.edn`'s `:tables`) by **Arbace's own TableGen**
  (`arbace.asm.tablegen`, the language read and evaluated lazily; 22,373 instructions and 2,796 aliases, as llvm-tblgen counts
  them) and **AsmMatcherEmitter's rules** (`arbace.asm.amd64.gen`): 23,014 matchables in the matcher's order, 107 operand
  classes, 207 mnemonic aliases, 7 assembler predicates, 322 registers (those with an AsmName), and the encoding fields of
  the 20,465 instructions the matchables name. `bin/repr-asm table gen|check` (about 2.5 minutes, 1.2 GiB): `check` regenerates and compares byte for
  byte. Its oracle, by hand: `bin/repr-asm table oracle` runs the host's llvm-tblgen 23.1.5 `-gen-asm-matcher` on the same
  files; its `MatchTable0` is the table's matchables row for row, 23,014 of 23,014 in the same order with the same classes.
- **The matcher and the encoder** (`arbace.asm.amd64.asm`): X86AsmParser's AT&T path (parseInstruction's rewrites, the
  generated MatchInstructionImpl, the suffix retry and its messages, processInstruction) and X86MCCodeEmitter for the legacy,
  REX, VEX and XOP encodings with LLVM's choices.
- **The check** (`arbace.asm.amd64.check`, `bin/repr-asm encode-check [--record]`; the Makefile's `repr-asm-encode-check`,
  `repr-asm-encode-record`): each stated encoding of `MC/X86` judged by its test's own CHECK lines as FileCheck reads them, in the
  runs' modes; `test/asm/encode-results-amd64.edn` (a row a file), `test/asm/divergences-encode.edn` (the classes),
  `test/asm/controls-encode.txt` (three negative controls, failing as they must). About 6 minutes on a loaded host, 1.5 GiB, four threads.

| `MC/X86` (621 files) | stated | met | classed | failing |
|---|---:|---:|---|---:|
| encodings (`encoding: [...]`) | 106,075 | **32,673**: legacy 13,753 (base integer, x87, MMX, SSE), VEX 17,128, XOP 1,792 | 73,402: `:evex` 50,896, `:intel` 20,725, `:forced-encoding` 1,175, `:apx` 605, `:no-run` 1 | **0** |
| error tests (96 files, `not llvm-mc`) | 108 `error:` lines in the 79 AX1 reads | 60 (57 files every line) | 17 files AX1 refuses or reads as Intel | 48 lines in 15 files (pending: messages of the operand and directive checks not ported yet) |

Every stated encoding the encoder produces is the test's: no `:mismatch`, `:rejected`, `:crash` or `:unpaired`. The judge holds
the record (met not fewer, failures not more, every class known) and the three controls fail as they must. The four files of
the first trial (`I86-64`, `I86-32`, `SSE2-32`, `x86-64`) and the VEX and XOP files (`AVX-64` 4,804 of 4,804, `AVX-32`, `AVX2-64`,
`FMA-64`, `XOP-64`, `avx-64-att`) meet every encoding they state outside the classes. AX1's and AR1's round trips rerun: the
same rows (`roundtrip: ok` on both architectures); `bin/reap` (0 problems) and `bin/crops-score --judge --strict` pass;
`test/gate/gate-key-crops-test.bash` passes with `arbace/asm/` in `arbace_crop_paths`, embed.ae's exclusions and the audit's
`crop-rules` (the table `asm/amd64/table.edn` lies outside every key's tree).

Not built in this slice: EVEX (AVX-512, AVX10) and APX, the largest classes; the CHECK text printed equal (the AT&T print of the
matched instruction from its AsmString: AX2's next slice); gas's mode (AXS5's `:gnu`) and relaxation (no stated encoding needs it:
llvm-mc's `-show-encoding` prints the instruction unrelaxed; objects need it, AX4); Intel syntax (AX3); the gate step and the row
(AX10, AXS21 (a)).

## Proposed (AX2)

Each with options, the recommended one named.

- **AXS23 The table's generator.** (a) Arbace's own TableGen evaluator and AsmMatcherEmitter's rules, the table committed and
  checked by regeneration, llvm-tblgen the oracle by hand (`table oracle`), as built; (b) llvm-tblgen's `--dump-json` filtered and
  committed at the oracle tier (AXS4 (a) as first written). *Recommended: (a)*: the generator is the crop's (independence), and
  the oracle shows the result is llvm-tblgen's row for row; the same evaluator reads AArch64's `.td` for AR2.
- **AXS24 The regeneration check's place.** (a) a `--full` step with the row's steps (AX10; about 2.5 minutes, 1.2 GiB), the
  essential gate reading the committed table; (b) essential. *Recommended: (a)*: the table changes only with the pin.
- **AXS25 Pairing a stated encoding with its instruction.** (a) FileCheck's order, each stated encoding matched forward from the
  last match in the run's output, an unmet one classed by the nearest instruction not matched, as built (the tests write a
  CHECK before, after or beside its instruction, and in blocks); (b) by adjacency in the file. *Recommended: (a)*: it is the
  test's own verdict; a wrong encoding can only cost more lines, never pass one.
- **AXS26 The machine level of the forms (AXS17's first part).** The matcher now names every encoded statement's machine
  instruction (LLVM's, e.g. `ADD32ri8`) and its operands: (a) the machine level as a second view made from the read forms by
  the matcher, `(x86/ADD32ri8 (reg eax) (reg eax) (imm 1))`, kept beside AX1's syntax-level forms and records, the SDM mnemonic and
  width (`(add {:w 32} ...)`) derived from it with AX2's CHECK-text slice; (b) AX1's forms rewritten to `(add {:w 32} ...)` now, the
  written mnemonic into the layer (AXS17 (a)), every AX1 record rewritten; (c) the syntax level only. *Recommended: (a) now, then
  (b) once the print from the AsmString makes the round trip `(add {:w 32} ...)` → text → the same forms checkable*: the match
  falls out of the table naturally, the rewrite of the reader's forms needs the printer to keep `:full`'s byte-for-byte claim.
- **AXS27 EVEX and APX next.** (a) EVEX (disp8*N from `CD8_Scale`, masks, broadcast, rounding: 50,000-odd of the classed lines)
  then the CHECK text, then APX; (b) the CHECK text first. *Recommended: (a)*: the classes are the measure of "equal or classed",
  and the table already holds EVEX's fields.

## As built: AX2 (second slice, 2026-11-10; branch `asm-ax2b`)

On the light lane, AXS23-AXS27 still unanswered and followed as recommended (AXS27 (a): EVEX first). doc/asm/AMD64.md §3
describes what was built: the EVEX prefix after `X86MCCodeEmitter::emitVEXOpcodePrefix` and `X86OpcodePrefixHelper::emit`
(llvm-project 23.1.5), the opmask, `{z}`, broadcast, embedded rounding and `{sae}`, disp8*N from the table's `CD8_Scale`
(`isDispOrCDisp8`), EVEX's maps 5-7, the AVX-512 registers and operand classes in the matcher (`MatchRegisterByName`'s 64-bit-only
registers, `ParseRoundingModeOp`, `isVK*Pair` and `addMaskPairOperands`), and `{evex}` (`checkTargetMatchPredicate`). The table
was not regenerated: its encoding fields already held EVEX's bits (`table check` byte for byte).

| `MC/X86` (621 files) | stated | met | classed | failing |
|---|---:|---:|---|---:|
| first slice | 106,075 | 32,673: legacy 13,753, VEX 17,128, XOP 1,792 | 73,402: `:evex` 50,896, `:intel` 20,725, `:forced-encoding` 1,175, `:apx` 605, `:no-run` 1 | 0 |
| **second slice** | 106,075 | **81,966**: legacy 13,753, VEX 17,215, XOP 1,792, **EVEX 49,206** | 24,109: `:intel` 20,725, `:apx` 3,383, `:no-run` 1 | **0** |
| error tests' `error:` lines | 108 | 60 → 63 | | 45 pending |

Every EVEX encoding the encoder produces is the test's: on the first run over the 132 files that held `:evex` or
`:forced-encoding` lines, 49,969 met and 169 rejected, all of one cause each: `vp2intersect`'s mask pair (`isVK2Pair` looked for a
class `VK2` the table does not name: the pairs' sets are `VK1`'s, K0-K7) and a `{dFV=...}` in another case; both fixed, none left.
VEX gains 87 lines: the opmask instructions (`kmov`, `kand`, ... VEX-encoded) whose `%k` registers the first slice refused as EVEX's. The classes `:evex`
and `:forced-encoding` are empty and kept; what EVEX cannot take yet is APX's and moves to `:apx` by its finer cause: EVEX's map 4
(NDD, NF, CCMP and CTEST, their `{dfv=...}`), `%r16`-`%r31`, `{rex2}` and `{evex} cmp`/`test` (CCMP's alias). A fourth negative
control (`avx512bw-64-att`'s statements against `avx512dq-att`'s CHECK lines) fails as it must. The table check regenerates the
table byte for byte; AX1's and AR1's round trips give the same rows (`roundtrip: ok` on both architectures); `bin/reap` (0
problems), `bin/crops-score --judge --strict`, `test/gate/gate-key-crops-test.bash` and `make check-makefile` pass. The encode
check takes about 1.7 minutes (four threads, under 1.5 GiB).

Not built in this slice: the CHECK text (AXS27's second step), APX (its third), Intel syntax (AX3), gas's mode, relaxation.

### Proposed (AX2, second slice)

- **AXS28 APX's place.** (a) APX next in AX2 (REX2, map 4's promoted forms with NDD and NF, CCMP/CTEST's flags, the EGPRs in
  ModRM, SIB, REX2 and EVEX's B4/X4), the encoder's last family, 3,383 lines; (b) the CHECK text first (AXS27 (a)'s order).
  *Recommended: (b)*, as AXS27 (a) orders it: the CHECK text judges every met encoding's instruction a second time (the matcher's
  choice, not only its bytes), which APX's NDD and NF forms need as much as the rest.

- **AXS29 The CHECK text's shape (the next slice).** What it takes, measured on this slice: the table's instructions with their
  AT&T AsmString and each operand's `PrintMethod` (printOperand, printMemReference's `print*mem`, printU8Imm, printSTiRegOperand,
  printCondCode, printRoundingControl, printSrcIdx/DstIdx, printMemOffset, printPCRelImm), the `InstAlias` records with
  `EmitPriority` above 0 (a few hundred by a rough count of X86's `.td`, to count exactly: an InstAlias prints by default, the quiet ones say `0`) and their operand
  conditions after `AsmWriterEmitter.cpp`'s printAliasInstr, `X86ATTInstPrinter::printInst`'s special cases (callq, data32,
  printVecCompareInstr's `vcmp`/`vpcmp`/`vpcom` mnemonics) and `X86InstPrinterCommon`'s flags (lock, rep, `{vex3}`, `{evex}`,
  `{disp32}`), MCExpr's printing for symbolic operands. Options: (a) the table regenerated with those fields (the matchables
  untouched, so `table oracle`'s MatchTable0 row for row still holds; llvm-tblgen `-gen-asm-writer` a second oracle by hand for
  the alias table), the text judged as the encodings are (each non-encoding CHECK line matched forward over the printed
  instructions, FileCheck's whitespace folding), its classes apart (`:print-alias`, `:directive` for CHECK lines of directives and
  labels), recorded beside the encodings' rows; (b) a FileCheck emulation over llvm-mc's whole output (text and encoding on one
  line, CHECK-NEXT, CHECK-SAME, CHECK-NOT), replacing AXS25's pairing. *Recommended: (a) first*: it keeps the encodings' record
  and judge as they are; (b) once the text is printed equal, as a stricter judge of both.

## As built: AX2 (third slice, 2026-11-10; branch `asm-ax2c`)

On the light lane, AXS23-AXS29 still unanswered and followed as recommended (AXS28 (b): the CHECK text before APX; AXS29 (a): the
table regenerated with the printer's fields, the text judged as the encodings are, its classes apart). doc/asm/AMD64.md §1, §4 and §5
describe what was built.

- **The table regenerated** with the printer's fields, after `AsmWriterInst.cpp` and `AsmWriterEmitter.cpp` (llvm-project 23.1.5,
  read and written again): each instruction's AT&T AsmString parsed into its fragments with each operand's MI index and
  `PrintMethod` (28 print methods), and printAliasInstr's patterns. AXS29's "few hundred" aliases counted exactly: X86 has three
  InstAliases that print (`aad`, `aam` for the immediate 10, `xstorerng`); every other one says `EmitPriority` 0 or has more operands
  than its instruction. The matchables are untouched: `table oracle`'s MatchTable0 is still 23,014 of 23,014 rows, and it now also
  compares llvm-tblgen `-gen-asm-writer`'s printAliasInstr with the table's `:print-aliases` (3 of 3). `table check` byte for byte.
- **The printer** (`arbace.asm.amd64.print`): X86ATTInstPrinter, X86InstPrinterCommon, MCInstPrinter's number formats and
  matchAliasPatterns, MCExpr's and MCSymbol's printing (doc/asm/AMD64.md §5).
- **The print level** of `bin/repr-asm encode-check`: each CHECK text held to the printed lines as FileCheck holds it, the rows'
  `:text`, the classes `test/asm/divergences-print.edn`, four negative controls in `test/asm/controls-print.txt` (each fails as it
  must), `--record` refused from a run with a failure at either level. `bin/repr-asm print-oracle` (by hand): llvm-mc beside.
- **The matcher**, two gaps the print level found (statements whose CHECK lines state no encoding): `(%dx)` alone is
  ParseMemOperand's DX register (in, out, ins, outs), not a 16-bit address the base check refuses; a symbol equated to a register
  (`v_ecx = %ecx`) is that register as an operand, a base, an index or a segment (`v_gs:(,v_eax)`; the class `:segment-symbol` emptied).

| `MC/X86` (621 files) | stated | met | classed | failing |
|---|---:|---:|---|---:|
| encodings (unchanged) | 106,075 | 81,966 | 24,109: `:intel` 20,725, `:apx` 3,383, `:no-run` 1 | 0 |
| **CHECK texts** | 108,601 | **82,838** | 25,763: `:intel` 21,318, `:apx` 2,221, `:directive` 1,202, `:other-tool` 772, `:comment` 116, `:unread` 63, `:lfi` 61, `:other-target` 7, `:options` 2, `:fixup` 1 | **0** |

The first run over the whole corpus failed 178 texts, of three causes, all fixed: the `(%dx)` operand (x86-64's in and out), the
register equates (x86_operands), and the specifiers' spelling (`a@tlsdesc`, `tlscall` in lower case as X86MCAsmInfo spells them);
the LFI files (triple `x86_64_lfi`, whose rewriter changes the instructions) classed. By hand against llvm-mc 23.1.5
(`bin/repr-asm print-oracle`): of 372 AT&T files 282 print llvm-mc's very instruction lines; of the 83,157 lines the printer
prints, 83,156 are llvm-mc's in llvm-mc's order (the other files differ by lines the check does not print: APX, macro bodies); the
one left is `pltoff`'s statement under `.ifdef ERR`, which llvm-mc skips and the check assembles (AXS32). Two differences the oracle
found that no run's CHECK text reached, both fixed: `(3+$foo)@ABS8` printed `3+$foo@ABS8` (the specifier on the symbol) and
`($foo@ABS8-4)`. The encode level is unchanged (81,966 met, 0 failing, the rows equal but their new `:text`); AX1's and AR1's round
trips give the same rows (`roundtrip: ok`, both architectures); `bin/reap` (0 problems), `bin/crops-score --judge --strict`,
`test/gate/gate-key-crops-test.bash` and `make check-makefile` pass. The encode check with its print level takes about 3 minutes on a
loaded host (four threads, under 1.5 GiB).

Not built in this slice: the asm streamer's directives, labels and comments (classed), FileCheck's `-NEXT` held strictly (AXS30),
`.if`/`.ifdef` and macro expansion in the check's assembly (AXS32), the machine-level forms (AXS26), APX, Intel (AX3).

### Proposed (AX2, third slice)

- **AXS30 The text judge's strictness.** (a) as built: FileCheck's forward search over the printed instructions alone, `-NEXT` and
  `-DAG` searched like `CHECK`, `-SAME` on the matched line, `-NOT` not judged (the printer makes no directive lines, so `-NEXT`
  cannot be held to adjacency), with llvm-mc as the oracle by hand that the lines themselves are llvm-mc's; (b) AXS29 (b) now: the
  streamer's whole output (directives, labels, assignments, comments, fixups) printed and FileCheck emulated exactly. *Recommended:
  (a) until AX4*, whose ELF streamer prints the directives; then (b), which also retires `:directive`, `:comment` and `:fixup`.
- **AXS31 The `:directive` class's rule.** (a) as built: a CHECK text is a non-instruction line when its content says so (a leading
  `.`, a label, `fixup`, `imm =`, a diagnostic) or its first word is no mnemonic, prefix or comparison mnemonic the printer writes;
  (b) only by content, the rest failing until the streamer prints them. *Recommended: (a)*: a text whose first word no instruction
  prints cannot be an instruction's line, and a wrong mnemonic the printer writes still fails.
- **AXS32 Conditional assembly and macros in the checks.** (a) the check's assembly follows `.if`/`.ifdef`/`.else` (with RUN's
  `--defsym`, now classed `:options`) and expands `.macro`/`.rept`/`.irp` (AXS19 (a)'s text bodies) as LLVM's AsmParser does, in AX2's
  next slice; (b) at AX4 with the object writer. *Recommended: (a)*: the oracle shows the one line not llvm-mc's comes from an
  `.ifdef`'d statement, and macro bodies hide instructions (`compact-unwind-cfi_def_cfa`) from both levels.
- **AXS33 VEX map 7 under `:apx`.** USERMSR's and MSR_IMM's VEX map 7 forms (`urdmsr $123, %r9`) are classed `:apx` at both levels
  though they are VEX: (a) encode VEX map 7 with APX's slice and keep the class until then; (b) a class of its own now. *Recommended:
  (a)*: a handful of lines, and the APX slice touches the same map decoding.

## As built: AX3 (2026-11-10; branch `asm-ax3`)

On the light lane (stage 0, one run at a time, under 1.5 GiB, no lock, no LLVM tool in any check), AXS17-AXS33 still unanswered and
followed as recommended where they touch this slice. llvm-project 23.1.5's X86AsmParser Intel path (parseIntelOperand,
ParseIntelMemoryOperandSize, ParseIntelExpression with IntelExprStateMachine and InfixCalculator, ParseIntelDotOperator,
HandleAVX512Operand, matchAndEmitIntelInstruction, parseInstruction's Intel branches), X86IntelInstPrinter.{cpp,h},
X86MCTargetDesc.cpp's populateReservedIdentifiers and AsmMatcherEmitter.cpp's variant handling (tokenizeAsmString's isolated tokens,
extractSingletonRegisterForAsmOperand with an empty register prefix) read from the pinned tarball and written again in Arbace; no code
copied. doc/asm/SPEC.md §5a and doc/asm/AMD64.md describe what was built.

- **The table regenerated** with the Intel variant (`arbace.asm.amd64.gen`): `:match-intel` (MatchTable1: each instruction and
  InstAlias of the variants `""` and `intel`, the string flattened to variant 1, a singleton register any isolated token naming one,
  `;` the comment delimiter), `:aliases-intel` (the MnemonicAliases of `intel`), `:print-aliases-intel` (X86GenAsmWriter1's
  printAliasInstr) and each instruction's `:print-intel` (its variant-1 AsmString's fragments). The AT&T parts are unchanged
  (`:match` row for row, the classes, `:insns` but the new field). Oracle by hand (`bin/repr-asm table oracle`, extended): llvm-tblgen's
  MatchTable0 23,014 of 23,014 rows and **MatchTable1 22,609 of 22,609** in the same order, printAliasInstr 3 of 3 for each writer.
  `table check` byte for byte (2.6 minutes, 1.3 GiB).
- **The reader** (`arbace.repr.asm.amd64.intel`): Intel instructions into forms in the text's order (the SDM's, as AT&T's reversed
  forms are), `ptr` sizes, segment overrides, `[base + index*scale + disp]` and its spellings (`16[rbx]`, `[eax][ebx*4]`, `[ebx].4`),
  the calculator's precedence, the registers taken out of the sum as the state machine takes them, decorations, rounding operands, `short`,
  `acquire`/`release`; MASM's symbol rule (P3) as llvm-mc applies it, the constants assigned so far inlined (`gnu/*equ*`). A file is read
  in Intel when its first llvm-mc run reads it so (`-x86-asm-syntax=intel` or `-x86-asm-syntax intel`, in that run's own command, not a
  later llvm-mc of the pipe), and `.intel_syntax`/`.att_syntax` switch the reader and the printer item by item.
- **The matcher**: Intel's operands (no reversal, the address swaps of parseIntelOperand, `rip` refused as an operand, `:nodirect` for a
  bracketed or sized `jmp`/`call` target), Sym + Imm as the state machine computes it (`intel-value`), matchAndEmitIntelInstruction (the
  pointer-sized `call`/`jmp`/`push`/`pop`, push of a constant matched in AT&T's table, the unsized memory operand tried at each size, the
  ambiguous-size and other messages), the comparison predicate appended last, the string instructions' defaults in Intel order, `xlat
  mem8` as `xlatb` (both syntaxes).
- **The printer**: X86IntelInstPrinter (printOperand with `offset`, printMemReference, the `print*mem` size words, the string indexes,
  the memory offsets, printU8Imm, printSTiRegOperand, printVecCompareInstr's Intel layout, the Intel aliases), Intel's quoting of
  reserved identifiers.
- **The cross-syntax judge** (`cross-judge`, a third level of `encode-check`, the rows' `:cross`): every matched instruction of each
  file's first AT&T run printed in Intel, read back by the Intel reader and matched in the same mode must be the same machine instruction;
  `test/asm/divergences-cross.edn` (the by-design classes are llvm-mc's own, each with its oracle by hand), `test/asm/controls-cross.txt`
  (three controls), and two Intel controls each in `controls-encode.txt` and `controls-print.txt`; all fail as they must.

| `MC/X86` | AX2 (third slice) | AX3 | failing |
|---|---|---|---:|
| round trip (621 files) | 464 pass, 152 `:intel` | **609 pass**; 9 error tests, 2 lexer options (`-masm-integers`), 1 not input | 0 |
| stated encodings (106,075) | 81,966 met; `:intel` 20,725, `:apx` 3,383 | **99,509 met** (EVEX 64,953, VEX 18,693, legacy 14,071, XOP 1,792); `:apx` 6,565, `:no-run` 1 | 0 |
| CHECK texts (108,601) | 82,838 met; `:intel` 21,318 | **100,884 met**; `:apx` 4,245, `:directive` 2,425, `:other-tool` 772, `:comment` 119, `:unread` 74, `:lfi` 61, `:options` 13, other 8 | 0 |
| cross-syntax (83,505 AT&T instructions) | none | **82,765 read back**; `:ambiguous` 566, `:pcrel-imm` 118, `:mask-operand` 32, `:other-instruction` 21, `:dollar-name` 2, `:local-label` 1 | 0 |
| error tests' `error:` lines | 63 of 108 | 74 of 130 (more files read) | 56 pending |

`MC/AsmParser`: 140 of 157 pass (139 before; `directive_seh`'s AT&T file no longer taken for Intel by a later llvm-mc of its pipe).
`:intel` is now empty at all three levels (kept for an Intel file the reader refuses). The first whole encode run met every Intel
encoding but printed one text wrong (`msg+-89793`: Sym + a negative Imm is LLVM's subtraction) and the first cross run failed 41: the
judge took the run's mode for statements after `.code32` (fixed: each statement's own mode), and the rest were llvm-mc's own, confirmed by
hand with llvm-mc 23.1.5 (`-output-asm-variant=1`, then `-x86-asm-syntax=intel`): `vgatherpf0dps {k1}, ...`, `[3+$foo@ABS8]`, `je 0`,
`call [rax]` for `lcall`, `mov ax, cs`, `pop ss` read as POPSS16, `vmovups.s` printed as `vmovups`. AR1's round trip gives the same rows;
`bin/reap` (0 problems), `bin/crops-score --judge --strict`, `test/gate/gate-key-crops-test.bash` and `make check-makefile` pass. The
encode check with its two text levels takes about 1.6 minutes (four threads, under 1.5 GiB).

Not built in this slice: MASM (`-masm-integers`, `.MODEL`, MS inline assembly's identifiers), the error tests' remaining messages, APX,
gas's mode, the streamer's directives.

### Proposed (AX3)

- **AXS34 The cross-syntax judge's by-design classes.** The judge's verdict is the machine instruction read back from the Intel print;
  llvm-mc itself fails it where its Intel printer writes text its parser reads otherwise. (a) as built: the judge classes such a case by
  a rule on the instruction and its print (`:ambiguous` the same text, `:other-instruction` another opcode, `:pcrel-imm`,
  `:mask-operand`, `:dollar-name`), by design, each class's examples checked against llvm-mc by hand; (b) the judge compares encodings
  instead of instructions (hides `_REV` and the masked immediates but not the far jumps); (c) a recorded list of the cases llvm-mc fails,
  made by the oracle tier. *Recommended: (a)*: the rules are the printer's and parser's own asymmetries, a bug of Arbace's printer or
  reader that keeps the opcode and changes an operand still fails (`:mismatch`), and the oracle by hand confirms each class.
- **AXS35 MASM integers.** `-masm-integers` (`10h`, `0B0h`; `intel-syntax-hex`, `pr27884`): (a) a lexer option of the reader when a RUN
  line names it, AsmLexer's LexMasmIntegers ported; (b) keep them under `:lexer-option` with `-motorola-integers`. *Recommended: (b)*
  until a corpus beyond these two tests needs it.
- **AXS36 The Intel forms' operand order.** (a) as built: Intel's forms in the text's order and AT&T's reversed, so both are the SDM's
  order and the machine level is one; (b) Intel's reversed too, a form being the text's syntax in a fixed order. *Recommended: (a)*: the
  survey's machine-level forms (AXS2 (a)) want the one order, and the AT&T reader already makes it.

## As built: AX2 (fourth slice, APX; 2026-11-10; branch `asm-ax2d`)

On the light lane (stage 0, one run at a time, under 1.5 GiB, no lock, no LLVM tool in any check), AXS17-AXS36 still unanswered and
followed as recommended where they touch this slice (AXS27 (a): APX after EVEX and the CHECK text; AXS33 (a): VEX's map 7 with APX).
llvm-project 23.1.5's `X86MCCodeEmitter.cpp` (X86OpcodePrefixHelper's REX2 and EVEX with APX's bits, determineOptimalKind,
emitVEXOpcodePrefix's forms with the new data destination and the conditional operands, emitREXPrefix, emitOpcodePrefix's REX2 M bit,
encodeInstruction's IsND skips), `X86BaseInfo.h` (hasNewDataDest, getMemoryOperandNo, needSIB, isApxExtendedReg, canUseApxExtendedReg),
`X86AsmParser.cpp` (MatchRegisterByName's UseApxExtendedReg, parseCFlagsOp, the pseudo prefixes `{rex2}` and `{nf}`,
checkTargetMatchPredicate, processInstruction's replaceWithCCMPCTEST, validateInstruction's high byte registers),
`X86EncodingOptimization.cpp` (optimizeShiftRotateWithImmediateOne), `X86InstPrinterCommon.cpp` (printInstFlags' `{nf}`) and
`llvm/lib/TableGen/TGParser.cpp` (ParseObjectBody's ApplyLetStack), read from the pinned tarball and written again in Arbace; no code
copied. doc/asm/AMD64.md §1 and §3 describe what was built.

- **The matcher**: the extended GPRs `%r16`-`%r31` taken (the 64-bit-only rule kept), an instruction matched with one only when
  canUseApxExtendedReg says so; `{dfv=of,sf,zf,cf}` read as CCMP's and CTEST's flags immediate (its two messages); `{nf}` as
  ForcedNoFlag (CFCMOV excepted); `{rex2}` a REX2 lower bound; `{evex} cmp`/`test` matched in 64-bit mode and rewritten to `ccmpt`/`ctest
  {dfv=}` (the flag `{evex}` dropped, as LLVM drops IP_USE_EVEX); validateInstruction's `can't encode 'ah' in an instruction requiring
  EVEX/REX2/REX prefix`.
- **The encoder**: REX2 (`D5`, M R4 X4 B4 W R X B) when a register's bit 4 is set, for JMPABS, PUSHP and POPP (ExplicitREX2) or under
  `{rex2}`, its M bit the 0F escape; EVEX's prefix rewritten form by form after emitVEXOpcodePrefix with APX's bits (B4 of a GPR rm or base,
  not inverted; X4 of a GPR index in EVEX.U; a vector rm's X and a VSIB's V' only for vector registers; the new data destination in vvvv and
  V'; NF in aaa's bit 2; the flags in vvvv and the condition in V' and aaa), the memory operand's number and the forms' operand walk with
  the new data destination skipped, SIB for `%r20` and `%r28` as bases (needSIB), VEX's maps 4-7 (map 7: USERMSR's and MSR_IMM's
  `urdmsr $123, %r9`, AXS33; map 5: AMX-FP8's `tdpbf8ps`).
- **The table regenerated** for two causes the slice found (`table check` byte for byte): Arbace's TableGen now applies the let stack around
  a class's definition to the class (ParseObjectBody's ApplyLetStack; AMX-FP8's `let Constraints = "$src1 = $dst" in { class AMX_FP8_BASE
  ... }` had left TDPBF8PS and its three siblings without their tie, so the matchable's conversion had `[:z]` where llvm-tblgen has the tied
  operand: the oracle compared the matchables' classes, not their conversions); and `:insns` holds the instructions processInstruction
  rewrites matchables into (optimizeShiftRotateWithImmediateOne's ROL8r1_ND and its siblings, DisassembleOnly, so no matchable names them).
  The 24 lines that change are those 4 instructions' entries and conversions and the 12 shifts' entries; the rest is unchanged.
- **The check**: a CHECK text is an instruction's line also when its first word is a matcher's mnemonic (`ccmpbw`): AXS31 (a)'s rule took
  the first word of the AsmString's text, which for CCMP, CTEST, CFCMOV and SETZU is `ccmp`, `ctest`, ... before printCondCode, and so
  had classed 2,322 APX texts `:directive` in AX3's record, where a wrong print would not fail (found by the new print control
  `_control-print/apx-ccmp-imm-hex`, which passed under the old rule); the text level's own `:apx` rule (an APX register or prefix in
  the text) removed: an unmet APX text now fails. `bin/repr-asm encode-check --record` now also requires every control to fail as it must
  (it wrote the record from a run whose control did not). `REPR_ASM_ENC_ROWS=1` writes every classed encoding row into `fails.tsv`.
- **The cross-syntax judge**: a new by-design class, `:disassemble-only` (12): `rolb $1, %al, %bl` is ROL8r1_ND, which prints `rol bl,
  al` in Intel and no Intel matchable reads (llvm-mc 23.1.5 by hand: `error: invalid operand for instruction`).
- **Controls**: `apx-add-att` against `apx-sub-att` at the three levels, `apx-ccmp-att` against `apx-ctest-att` (encodings) and
  `apx-ccmp-att` printed in hex (texts); each fails as it must (20 controls in all).

| `MC/X86` | AX3 | AX2 (fourth slice) | failing |
|---|---|---|---:|
| stated encodings (106,075) | 99,509 met; `:apx` 6,565, `:no-run` 1 | **106,074 met** (EVEX 71,212, VEX 18,717, legacy 14,353, XOP 1,792); `:no-run` 1 | 0 |
| CHECK texts (108,601) | 100,884 met; `:apx` 4,245, `:directive` 2,425, `:other-tool` 772, ... | **107,451 met**; `:other-tool` 772, `:comment` 119, `:directive` 102, `:unread` 74, `:lfi` 61, `:options` 13, `:other-target` 7, `:fixup` 1, `:local-label` 1 | 0 |
| cross-syntax | 82,765 of 83,505 | **86,350 of 87,103**; `:ambiguous` 566, `:pcrel-imm` 118, `:mask-operand` 33, `:other-instruction` 21, `:disassemble-only` 12, `:dollar-name` 2, `:local-label` 1 | 0 |
| error tests' `error:` lines | 74 of 130 | 80 of 130 | 50 pending |

`:apx` is empty at both levels (kept for an APX form not taken); the 102 `:directive` texts left are directives, labels, data lines and
`x86_64-asm-match`'s matcher trace; `jge Ltmp0` (`intel-syntax-directional-label`), a `:directive` while `jge` was no known mnemonic,
is now `:local-label` (the text judge names an unmet text's class by the nearest statement, now the earlier of two as near). The first subset run (the 146 files that held `:apx` rows) met all but 28 encodings, of two causes, both
the table's (above); the first whole run met every encoding and text and failed only the record's stated counts (the cross judge's rows grew
with the instructions now matched); the record was written from the fourth (the second wrote one with a control passing, the third
failed `jge Ltmp0` before its class's tie-break above). AX1's and AR1's round trips give the same rows; `bin/reap`,
`bin/crops-score --judge --strict`, `test/gate/gate-key-crops-test.bash` and `make check-makefile` pass. The encode check takes about 6
minutes on a loaded host (four threads, under 1.5 GiB); `table gen` 2.7 minutes, 1.2 GiB.

Not built in this slice: gas's mode, relaxation, the streamer's directives, the error tests' remaining messages (50 lines), MASM.

### Proposed (AX2, fourth slice)

- **AXS37 The table oracle's reach.** `bin/repr-asm table oracle` compares MatchTable0/1's mnemonics, instructions and classes, so a
  conversion that differs (TDPBF8PS's missing tie) passes it. (a) extend it to the conversions (llvm-tblgen's `ConversionTable` and
  `ConvertFn`'s kinds, the tied operands) and to the encoding fields of `:insns` (`-gen-instr-info`'s TSFlags decoded), by hand as now;
  (b) as built. *Recommended: (a)*: the encoder reads those fields and the oracle is the only independent look at them.
- **AXS38 TableGen's let order for defs.** TGParser applies the let stack before a def's body (ParseObjectBody), Arbace's TableGen after it
  (a body's `let` of a field an outer `let` sets keeps the outer value here, the body's in LLVM). (a) follow TGParser for defs too and show
  the table unchanged (`table check`) or explain each change; (b) keep it until a record differs. *Recommended: (a)*: the class case above
  shows such gaps hide in the conversions, which the oracle does not see yet.
- **AXS39 The `:directive` rule after the matchers' mnemonics.** (a) as built: a text is an instruction's line when its first word is a
  printed mnemonic, a matcher's mnemonic, a prefix or a comparison mnemonic; (b) by content alone (AXS31 (b)). *Recommended: (a)* until AX4
  prints the streamer's lines: the rule's gap was a missing source of mnemonics, now the matcher's own.

## As built: AXS37, AXS38 (2026-11-10; branch `asm-tg`)

Both taken as (a), recommended (correctness fixes of Arbace's own generator; the user can still reject them). Sources read from the
pinned llvm-project 23.1.5 tarball and written again in Arbace, no code copied: `llvm/lib/TableGen/TGParser.cpp` (ParseObjectBody,
ApplyLetStack, ParseDef, ParseDefm, AddSubClass, AddSubMultiClass, ParseTopLevelLet, SetValue) and
`llvm/utils/TableGen/AsmMatcherEmitter.cpp` (buildInstructionOperandReference, buildInstructionResultOperands,
buildAliasResultOperands, emitConvertFuncs).

- **AXS38, the let order.** `arbace.asm.tablegen`'s `complete-def` now makes a def's record in TGParser's order: the superclasses, the
  let stack it was written in (ParseObjectBody's ApplyLetStack, before the body), the body; then level by level outwards the defm's
  superclasses (ParseDefm's AddSubClass) and the let stack around that defm (its ApplyLetStack). A class's record already took the stack
  after its superclasses and before its body (the fourth slice); foreach, `if` and defset add no level of their own, as in TGParser, whose
  stack is lexical; a multiclass's base (`multiclass M : B`) takes no let around M's definition (AddSubMultiClass resolves B's records
  without ApplyLetStack; X86 has none). Effect: in X86.td's world four records change (a body's `let IntrProperties` under an outer `let`
  of the same field: NVVM's `int_nvvm_fence_proxy_tensormap_generic_acquire_{cta,cluster,gpu,sys}`, Intrinsics, which the generator
  drops); no record the table reads, so `table check` was byte for byte with this change alone.
- **AXS37, the oracle's reach.** `bin/repr-asm table oracle` (by hand, the host's llvm-tblgen 23.1.5) now runs every part and fails on
  any difference, its new half `test/asm/table_oracle.ae` (`asm.table-oracle`, which reads llvm-tblgen's output as text): each
  MatchTable0/1 row's conversion (ConversionTable's steps by convertToMCInst's cases: the asm operand, its render method and machine
  operand count, TiedAsmOperandTable's result operand, an immediate, a register) against `:match`/`:match-intel`; every `:insns` entry's
  encoding fields (opcode, format, map, prefix, operand and address size, encoding, immediate, explicit prefix, CD8 scale, the 18 flag
  bits), operands with MIOperandInfo's counts, defs and ties against `-print-records`' resolved fields (extracted by awk; the 110 MB
  dump is not kept); every `:print`/`:print-intel` against `-gen-asm-writer`'s printInstruction decoded (getMnemonic's AsmStrs and
  OpInfo words, each fragment's command by its bits). About 80 s; llvm-tblgen under 600 MB.
- **What it found.** The fields and the printers were llvm-tblgen's (20,477 entries each, the same). The conversions were not: 763 rows of
  each table (1,526 lines of `asm/amd64/table.edn`). A tied pair was written with the later operand rendered and the earlier copied
  forward (`ADC8rr`: `[[:t 1] [:a 1] [:a 0]]`), where LLVM canonicalizes the asm operand `$src1` to the untied `$dst`, renders `$dst`
  and copies it into `$src1` (`[[:a 1] [:t 0] [:a 0]]`); and a tied operand of several machine operands was copied operand by operand
  (VGATHERDPD's `[:t 8]`), where LLVM ties only single operands. `arbace.asm.amd64.gen`'s instruction conversion is now
  buildInstructionResultOperands' (doc/asm/AMD64.md §1); every MatchTable row's conversion is equal. The encoder resolves a tie by slot
  either way, so no encoding changed (the same counts below).

| check | before | after |
|---|---|---|
| table oracle: MatchTable0/1 rows | 23,014 / 22,609 equal | equal |
| conversions (new) | 763 / 763 rows differ | 23,014 / 22,609 equal |
| `:insns` fields, `:print`, `:print-intel` (new) | 20,477 equal each | equal |
| print aliases | 3 / 3 equal | equal |
| `encode-check` (`MC/X86`) | 106,074 met, 0 failing | 106,074 met (EVEX 71,212, VEX 18,717, legacy 14,353, XOP 1,792); texts 107,451; cross 86,350 of 87,103; 0 failing |

arm64: AR2's generator (`arbace/repr/asm/arm64/tablegen.ae` on `asm-ar2`) reads Go's arm64asm and obj sources as text, not TableGen:
unaffected.

## As built: AX4 (first slice, 2026-11-10; branch `asm-ax4`)

On the light lane (stage 0, one run at a time, under 2 GiB, no lock, no LLVM tool in any check; llvm-mc 23.1.5 by hand to record the
references), AXS17-AXS36 still unanswered and followed as recommended where they touch this slice. The slice is §7.3's writer and reader,
not yet the linker nor the decoder. llvm-project 23.1.5's MC (MCObjectStreamer, MCELFStreamer, MCAssembler, MCDwarf's FrameEmitterImpl,
ELFAsmParser, AsmParser's directives, ELFObjectWriter, StringTableBuilder) and X86's back end (X86AsmBackend, X86ELFObjectWriter,
X86MCCodeEmitter's fixup kinds, X86EncodingOptimizationForImmediate.def) read from the pinned tarball and written again in Arbace; no
code copied. doc/asm/AMD64.md §7 describes what was built.

- **`arbace.asm.elf`** (`arbace/asm/elf.ae`, shared with arm64 by design: AS5): the ELF64 writer as ELFWriter lays an object out
  (`.strtab` first and tail-merged, sections with their `.rela`, groups, `.symtab` with STT_FILE and the locals first) and a reader of any
  ELF64 little-endian object (sections and bytes, symbols, relocations), with the judge's canonical view.
- **`arbace.asm.amd64.obj`** (`arbace/asm/amd64/obj.ae`): the object assembler over AX2's encoder: sections, chunks as MC's fragments,
  symbols in MC's registration order with their bindings, types, visibility, sizes, commons and variables; the data, fill, NOP and
  alignment directives; the relaxation of branches and short immediates to a fixed point with MC's stretch; the fixups evaluated and the
  relocations X86ELFObjectWriter chooses (section symbols as ELFObjectWriter converts them); CFI into `.eh_frame`.
- **The encoder** (`arbace.asm.amd64.asm`, a small additive change): each fixup now carries its expression, X86MCCodeEmitter's kind,
  PC-relativity and ImmOffset (`[OFF SIZE EXPR KIND PCREL ADJ]`; the first two as before), and AddCCFrm's JCC_4 to a symbol is
  reloc_branch_4byte_pcrel. The encode check is unchanged (99,509 met, 100,884 texts, 82,765 cross, 0 failing).
- **`bin/repr-asm object FILE OUT`**, `object-dump OBJ`, `object-check [--record] [CORPUS...]`, `object-record CORPUS` (the oracle tier's,
  by hand); the Makefile's `repr-asm-object`, `repr-asm-object-check`, `repr-asm-object-record`, `repr-asm-object-oracle`.
- **The references**: llvm-mc 23.1.5's objects (`-triple x86_64-linux-musl -filetype=obj`, each made twice and compared) recorded as small
  pinned records in the checkout, `test/asm/records/obj-{musl,synthetic,mc-x86-obj}-llvm-mc-23.1.5.edn` (hex with each SHA-256; the file
  pinned by its `.sha256`, checked before it is read), not packs in the store (AXS40). The check never runs llvm-mc.

| corpus | files | equal to llvm-mc's object byte for byte | classed | failing |
|---|---:|---:|---|---:|
| musl 1.2.6's x86-64 `.s` | 32 | **32** | none | 0 |
| synthetic (`test/asm/obj/`: data, branches, relocations, sections, NOPs, CFI) | 6 | **6** | none | 0 |
| `MC/X86`'s `-filetype=obj` tests for x86-64 ELF (47 llvm-mc assembles of 52 selected) | 47 | **30** | 11 `:unsupported-apx`, 5 `:unsupported-directive` (`.macro`, `.ifdef`, `.rept`, `.reloc` twice), 1 `:unsupported-debug-line` | 0 |

So every object Arbace writes is llvm-mc's, byte for byte (no `:canonical` row left): 68 of 85, the rest classed by what is not
assembled yet. Controls (`test/asm/controls-object.txt`): five, a file's object against another's record, each failing as it must. The
check takes about 46 s (three corpora, one process each, under 1.5 GiB). What the runs found on the way, all fixed: the relaxed
branches' encodings are not in the table (JMP_4/JCC_4 codegen-only: their fields given after X86InstrControl.td), JCC_4's fixup kind
(PLT32, not PC32), absolute variables' LOCAL binding and aliases' inherited type and size, a section name read as an expression
(`.note.GNU-stack`), SHF_LINK_ORDER's linked-to section, a section's name as its section symbol (`.GOT64@GOT`), variables followed
through (`bar = foo + 4`), and fills as fragments (`large-bss`'s 2^52-byte `.bss`, first held as bytes, stopped at 5 GiB by hand).
AX1's and AR1's round trips give the same rows; `bin/reap` (0 problems), `bin/crops-score --judge --strict`,
`test/gate/gate-key-crops-test.bash` and `make check-makefile` pass.

Not built in this slice: the static linker and the runs by the CPU (P4's program, the synthetic programs), the decoder (x86asm converted)
and with it "musl's objects read back to their forms" (here read back to their sections, symbols and relocations only), `MC/Disassembler/X86`,
`.debug_line`, `.reloc`, macros and conditionals in the object assembler (AXS32), the MC tests' own CHECK lines as the judge (AXS40 (c)).

### Proposed (AX4, first slice)

- **AXS40 The object check's reference.** (a) as built: llvm-mc's objects recorded by hand as small pinned records in the checkout
  (`test/asm/records/`, 175 KB of hex for 85 objects), the check byte for byte, then canonical; (b) packs in the store
  (`bin/oracle-records`, ORS's conventions) for larger corpora (gcc `-S` outputs, AX6); (c) the MC tests' own CHECK lines
  (llvm-readobj's and llvm-objdump's text printed from Arbace's reader), a test's own verdict. *Recommended: (a) now, (b) from the first
  corpus above a megabyte, and (c) for `MC/X86` once the decoder prints llvm-objdump's `-d` (64 of the 114 tests check its text)*: (c) is
  the independent judge the score counts, (a) the stricter one meanwhile.
- **AXS41 The fixups' shape.** (a) as built: the encoder's fixup vector grows to `[OFF SIZE EXPR KIND PCREL ADJ]`, the first two fields
  unchanged for the encode check; (b) a separate pass recomputing kinds from the instruction. *Recommended: (a)*: the kinds are chosen
  where X86MCCodeEmitter chooses them, and the other users read only the offset and size.
- **AXS42 The object check in the gate.** (a) at AX10 with the row: an essential step over musl and synthetic (about 25 s, 1.5 GiB),
  `mc-x86-obj` in `--full`; (b) now. *Recommended: (a)*, as AXS21 (a).
- **AXS43 The next slice.** (a) the static linker (`ET_EXEC`, then static PIE; §7.3) and the first runs by the CPU (P4's program, the
  synthetic programs, musl's crt and string functions linked against a harness), then the decoder; (b) the decoder first (x86asm
  converted by repr/go's reader, `MC/Disassembler/X86`'s 13,545 decodings, the objects read back to forms). *Recommended: (a)*: the
  exit criterion "run by the CPU with their own verdicts" needs only the writer built here, and the decoder is a conversion of its own.
- **AXS44 `.debug_line`.** (a) `.loc`/`.file N` into `.debug_line` as MCDwarfLineTable writes it (DWARF 5 by default) with the next
  slice; (b) when a corpus needs it (gcc and clang `-S` output with `-g`). *Recommended: (b)*: no corpus of AX4's exit needs it.

## As built: AX4 (second slice, 2026-11-10; branch `asm-ax4b`)

On the light lane (stage 0, one run at a time, under 2 GiB, no lock; no assembler, linker or compiler of the host in any check), on
`asm-ax4`'s object assembler. The slice is §7.3's linker and §12's AX4 exit "P4's program and the first synthetic programs assembled,
linked, run by the CPU". llvm-project 23.1.5's `lld/ELF` read from the pinned tarball (extracted into `.tmp/`, never committed) and
written again in Arbace; no code copied. doc/asm/AMD64.md §8 describes what was built.

- **`arbace.asm.link`** (`arbace/asm/link.ae`): ELF64 relocatable objects → a static `ET_EXEC` for x86-64 Linux as lld lays one out with
  no script: COMDAT, symbol resolution (global, weak, common, local; hidden made local), the output sections by lld's names and rank,
  mergeable sections' pieces deduplicated, `.eh_frame`'s CIEs deduplicated and dead FDEs dropped, `.got`/`.got.plt`/`.relro_padding`,
  the program headers (PT_PHDR, PT_LOADs by permission, PT_TLS, PT_GNU_RELRO, PT_GNU_STACK), addresses from 0x200000, congruent file
  offsets, the relocations AX4 emits with GOTPCRELX relaxed as lld relaxes it and GOTTPOFF relaxed to local exec, the linker-defined
  symbols, the symbol table, entry `_start`. `arbace.asm.elf`'s reader gives each relocation its target section's index (`:target`,
  additive; its canonical view unchanged).
- **The run check** (`bin/repr-asm run-check [--record]`): `test/asm/run/`'s 18 tests (Arbace's own; their helpers and second objects in
  `test/asm/run/lib/`) with their own verdicts in their headers, assembled and linked in one Arbace process, each program run by the CPU
  under `timeout` (10 s), judged by its verdict; rows `test/asm/run-results-amd64.edn`, classes `test/asm/divergences-run.edn` (three
  deferred, none used), four negative controls `test/asm/controls-run.txt` (a program's run judged by another's verdict). About 12 s,
  0.75 GiB. `bin/repr-asm link OBJ... -o OUT`, `link-dump EXE`, `link-oracle` (by hand); the Makefile's `repr-asm-link`,
  `repr-asm-link-dump`, `repr-asm-run-check`, `repr-asm-run-record`, `repr-asm-link-oracle`.

| test | what it runs | verdict |
|---|---|---|
| `p4-hello` | §3's P4: `write` and `exit` | 42, `hello` |
| `exit-status`, `loop-sum`, `calls`, `data-bss` | an exit status; loops (short and near back branches); recursion and calls through a register; `.data.*`/`.bss.*` merged, a common | stated |
| `two-objects` | PLT32, PC32, R_X86_64_64, 32S across objects; GOTPCRELX relaxed (`mov`→`lea`, `call *`, `jmp *`), an absolute through a `.got` entry, GOTPCREL with an addend | stated |
| `weak`, `comdat`, `merge-strings` | an undefined weak (0), a weak overridden; a COMDAT group in two objects (the first kept); strings deduplicated across objects | stated |
| `eh-frame` | CFI in two objects (one CIE, two FDEs re-pointed: `readelf --debug-dump=frames` by hand agrees) | stated |
| `tls` | `.tdata`/`.tbss` in PT_TLS, the thread pointer by `arch_prctl`, TPOFF32, GOTTPOFF relaxed | stated |
| `linker-symbols`, `relocs-data` | `__ehdr_start`, `__bss_start`, `_end`, `_etext`, `__start_`/`__stop_`; R_X86_64_32/16/8, PC64, PC32, SIZE32/64, GOTOFF64 and GOTPC32 with `_GLOBAL_OFFSET_TABLE_` | stated |
| `musl-crti`, `musl-string`, `musl-setjmp` | musl 1.2.6's `crti`/`crtn` around `.init`/`.fini`; its x86-64 `memcpy`, `memmove` (to the hidden `__memcpy_fwd` across objects), `memset`; `setjmp`/`longjmp` | stated |
| `undefined`, `duplicate` | a missing symbol, two strong definitions | the link's refusal in lld's words |

**18 of 18 pass**, none classed, none failing; the four controls fail as they must. What the runs found on the way: two tests' own
mistakes (setjmp's second return printed, a GOT entry dereferenced), and a SIZE relocation against a local symbol, which MC (and so
llvm-mc) writes against the section symbol: its size 0, as lld would give (the test made the symbol global). Freestanding only: musl's
`libc.a` cannot be had from the tarball without compiling its C (its x86-64 `.s` are 32 files beside 1,538 `.c` in `src/`), so linking against musl waits
for AX7 (AXS47); its `crt1` is C too.

**The oracle, by hand** (`bin/repr-asm link-oracle`): the host has no `ld.lld` (none in `/usr/lib/llvm23/bin`), so GNU ld 2.45.1's
`ld.bfd -static` linked the same Arbace-assembled objects. Every program's exit status and stdout are the same as Arbace's (16 of 16;
GNU ld refuses the two link-error tests too). The canonical views (entry, segments, sections, symbols as section+offset) differ only by
cause: GNU ld's default script with `-z separate-code` (base 0x400000, the headers alone in an R PT_LOAD, `.text` on its own pages, the
read-only data in an R PT_LOAD after it; `.init` before `.text`, `.got.plt` before `.data`), no PT_PHDR and no PT_GNU_STACK in its static
output of objects without `.note.GNU-stack`, `.eh_frame` kept as SHT_X86_64_UNWIND (lld's synthetic one is PROGBITS), GOTPCRELX relaxed
for absolute symbols too and GOTPCREL with an addend (no `.got` at all; lld keeps entries for both), `.tdata` in PT_GNU_RELRO with no
`.relro_padding`, its linker-defined symbols always defined and global (lld's only when referenced, hidden), a hidden global kept global
(`__memcpy_fwd`; lld makes it local), an undefined weak left out of `.symtab`; every other symbol at the same section and offset. A
byte-level reference needs lld itself (AXS46).

AX1's and AR1's round trips, the encode check and the object check (68 of 85 byte for byte, unchanged), `bin/reap`, `bin/crops-score
--judge --strict`, `test/gate/gate-key-crops-test.bash` and `make check-makefile` pass.

Not built in this slice: archives, shared objects, static PIE, TLS GD/LD/TLSDESC, `--gc-sections`, ICF, scripts, lld's hash-sharded order
of merged pieces (Arbace's first seen), the decoder (AXS50).

### Proposed (AX4, second slice)

Numbering: AX4's first slice proposed AXS37-AXS41 while AX2d's branch proposed its own AXS37-AXS39; at the merge (branch
`asm-merged`, 2026-11-10) AX4's were renumbered AXS40-AXS44 (above), filling the gap below these, which start at AXS45; the
commit messages of `asm-ax4` keep the old numbers.

- **AXS45 The run check's judge.** (a) as built: each program's own verdict stated in its header (exit status, stdout, or the link's
  refusal), the CPU's run judged by it; (b) also x86i's run and the lockstep (§8.3) once x86i exists. *Recommended: (a) now, (b) at AX5*:
  the CPU is the strictest judge the AOT level has, and no tool of the host is in it.
- **AXS46 The linker's byte-level reference.** (a) as built: lld's layout as the model, held by the runs, the canonical view checked by
  hand against GNU ld (by cause); (b) ld.lld 23.1.5 (the pinned tarball's lld, or an Alpine package, by the user's OK) by hand, its
  executables recorded and pinned like llvm-mc's objects (`test/asm/records/exe-run-lld-23.1.5.edn`), the check byte for byte then
  canonical. *Recommended: (b)* when the user allows the package: a byte-for-byte reference catches what a run cannot (padding, symbol
  order, `.comment`'s line aside, which is Arbace's by design).
- **AXS47 Linking against musl.** (a) freestanding only until AX7 (as built); (b) a pack of clang `-S` of musl's C (§9's
  `asm-amd64-musl-S`) assembled by Arbace and archived into Arbace's own `libc.a` (archives then read member by member as ld pulls them),
  `crt1` included; (c) ci's musl as C forms lowered to assembly by an Arbace back end (beyond v1). *Recommended: (b) at AX7*: the pack is
  0.5 of independence for those cases, the freestanding corpus stays 1.
- **AXS48 The run check in the gate.** (a) at AX10 with the row: an essential step (about 12 s, 0.75 GiB), the programs run under
  `timeout`; (b) now. *Recommended: (a)*, as AXS42 (a).
- **AXS49 Merged pieces' order.** (a) first seen (as built); (b) lld's: xxh3 of each piece into 32 shards, laid out shard by shard.
  *Recommended: (b) with AXS46 (b)* only: without lld's executables to compare, the order changes nothing a program can see.
- **AXS50 The next slice.** (a) the decoder (x86asm converted by repr/go's reader, `MC/Disassembler/X86`'s 13,545 decodings, the
  objects read back to forms), AX4's remaining exit; (b) more run tests first (a generator by instruction family, §10's synthetic
  programs). *Recommended: (a)*, then (b) with x86i (AX5).

## As built: AX4 (third slice, 2026-11-10; branch `asm-ax4c`)

On the light lane (stage 0, one run at a time, under 2 GiB, no lock; no llvm-mc nor any LLVM tool in any check), on `asm-ax4b`. The slice
is AX4's remaining exit (§12): "x86asm converted (the decoder)", `MC/Disassembler/X86`'s decodings, and the objects read back to their
forms. doc/asm/AMD64.md §9 describes what was built.

**The choice: LLVM's disassembler rebuilt from the table, not x86asm.** Two sources were weighed. Go's `golang.org/x/arch/x86/x86asm`
(TamaGo's GOROOT, `src/cmd/vendor/`; BSD; 22,979 lines, 17,223 of them its generated `tables.go` and `avx_tables.go`) decodes into
its own `Inst` (its `Op` names, its `Args`, GNU's, Intel's and Plan 9's printers): to give LLVM-equal results it would need a map from
each of its ops and argument shapes to the table's instructions and MCInst operands (and LLVM's choices where both read the same bytes
otherwise: NOOP against XCHG, the `_REV` forms, EIZ, prefixes kept as instructions), and without that map its result could be neither
printed by AX2's printer nor re-encoded by AX2's encoder. LLVM's X86 disassembler decodes into the very instruction records the table already holds
(the matcher's and the printer's): its tables are TableGen's `-gen-disassembler`, made from each instruction's encoding fields, which
`asm/amd64/table.edn` has (the generator now also lists the 250 instructions only the disassembler emits, `_REV` forms and other
ForceDisassemble ones, with `:dis` and the mode predicate). So the decoder is X86Disassembler.cpp's logic and X86RecognizableInstr.cpp's
and X86DisassemblerTables.cpp's rules (contexts, filters, specifiers) written again, 1,100 lines of Arbace, with no generated table
committed (each decision made at its first use, by opcode type, context and opcode); x86asm would have been its decoder plus its tables
and the map. The judge then reuses AX2's printer and encoder unchanged. This is
AX5-DESIGN's AXS53 (a) as the coordinator relayed it; x86asm stays an optional second view.

- **`arbace.asm.amd64.decode`** (`arbace/asm/amd64/decode.ae`): the contexts and ranks, `inheritsFrom` (LLVM's default arguments in its
  recursive calls kept, the IC case's `AdSize64` passed as noPrefix as LLVM passes it), `outranks`, the attribute mask's context,
  `insnContext`, the operand specifiers and ModRM filters, `setTableFields` per decision; readPrefixes, readOpcode, getInstructionID with
  its corrections, readModRM/readSIB/readDisplacement, fixupReg, readOperands, the translation, getInstruction's flags. `decode`,
  `decode-all`.
- **`arbace.asm.amd64.decode-check`** (`arbace/asm/amd64/decode_check.ae`) and **`bin/repr-asm decode MODE [intel] HEX...`,
  `decode-object OBJ`, `decode-check [--record] [dis|rt|obj]`**; the Makefile's `repr-asm-decode`, `-decode-object`, `-decode-check`,
  `-decode-record`. The corpus `test/asm/corpora.edn`'s `:decode-corpora` (`:mc-x86-dis`, extracted with the others, no round trip's).
- **The printer** (`arbace.asm.amd64.print`, additive): `addr32`/`addr16` for the decoder's address-size flag (printInstFlags'
  `IP_HAS_AD_SIZE`, X86_MC::needsAddressSizeOverride). **The encoder** (additive): `memory-operand`, public for the decoder.
- **Arbace's TableGen fixed**: the `let` stack around a `class` now applies to it (after its superclasses, before its body, as
  TGParser's ParseObjectBody does); before, AMX-FP8's four `TDP*F8PS` lost their `$src1 = $dst` tie (the decode check's one crash: the
  printer read an operand the MCInst did not have). The table changes in those four instructions' ties and matcher conversions, and in
  the disassembler's instructions added.

| level | judged | met | classed | failing |
|---|---:|---:|---|---:|
| `dis`: MC/Disassembler/X86's 186 tests, their CHECK lines (13,546 byte strings, AT&T and Intel runs, 16-, 32- and 64-bit, APX, AMX, AVX10, `--hex`, the error tests' diagnostics) | 24,445 | **24,441** | 4 `:markup` (`--mdis`'s two tests) | 0 |
| `rt`: every encoding encode-check meets in MC/X86, decoded back to the matcher's instruction and operands | 99,509 | **99,505** (of them 16 with llvm-mc's lone LOCK first, 66 another instruction of the same bytes) | 3 `:llvm-nop` (`xchg %ax,%ax` read as `nop`, LLVM's NOOP rule), 1 `:eiz-dropped` (`(,%eiz)` in 64-bit, LLVM's EIZ rule) | 0 |
| `obj`: musl 1.2.6's 32 files assembled into objects by AX4's assembler, their code decoded in order | 479 instructions | **479** | none | 0 |

The negative controls (`test/asm/controls-decode.txt`): eight, each failing as it must (a test's bytes against another's CHECK lines,
twice across modes; met encodings decoded in another mode; a musl object against another file's instructions). About 6 minutes for the
three levels (four threads, under 1.5 GiB). The encode check (99,509 met), the object check, the run check, AX1's and AR1's round trips
and the table check are unchanged and pass.

Not built in this slice: `--mdis`'s markup printer, FileCheck's variables (none in these tests), a symbolizer (llvm-objdump's branch targets
and relocations beside the code).

### Proposed (AX4, third slice)

Numbering from AXS63 (AX5-DESIGN uses AXS51-AXS62, the coordinator's note).

- **AXS63 The decoder's source.** (a) as built: LLVM's X86 disassembler rebuilt from `asm/amd64/table.edn`, its tables made at first use;
  (b) x86asm converted (its ops mapped onto the table's instructions); (c) both, x86asm as a second view checked against (a).
  *Recommended: (a)*, with (c) only if AX5's x86i wants Go's decoder in goi (AXS53's note).
- **AXS64 The decode tables committed.** (a) as built: made from the table at first use, never committed; (b) generated into
  `asm/amd64/dis.edn` by `bin/repr-asm table gen` and checked by regeneration, as LLVM ships X86GenDisassemblerTables.inc; (c) checked by
  hand against llvm-tblgen's `-gen-disassembler` (the oracle tier's, like `table oracle`). *Recommended: (a) and (c)*: the tables are a
  pure function of the committed table; (c) proves them equal to LLVM's (the decision per context and opcode) at no cost to the check.
- **AXS65 The decode check in the gate.** (a) at AX10 with the row: `dis` and `obj` an essential step (about 2.5 minutes, 1.5 GiB), `rt`
  in `--full` (about 3 minutes); (b) now. *Recommended: (a)*, as AXS39 (a) and AXS48 (a).
- **AXS66 `--mdis`.** (a) the printer's markup (`markup(O, Markup::Register)` ...) as a dynamic flag of arbace.asm.amd64.print, the two
  tests met; (b) leave classed. *Recommended: (a)* when the printer is next touched: small, and it closes the level.
- **AXS67 The next slice.** (a) AX5's x86i over this decoder (its instruction records, as AX5-DESIGN's AXS53 (a)); (b) llvm-objdump's
  `-d` text (a symbolizer and the object's relocations), which turns MC/X86's object tests' CHECK lines into their own judges (AXS37
  (c)). *Recommended: (a)*, then (b) with AX10's row.

## As built: the asm branches integrated (2026-11-10; branch `asm-merged`)

One assembly branch for the 11-11 merge: `asm-merged`, from `asm-tg` (`69ca9140d`: AX1, AR1, AX2's four slices, AX3, AXS37/38)
with `asm-ax4b` (`abc8c2b97`: AX4's objects, linker and run check) and then `asm-ar2` (`da6bfe8f8`: AR2's A64 table and encoder)
merged into it, every finding of the second review of the 11-11 merges (`.tmp/merge-1111/REVIEW2.md` §0-§3) fixed and every check
run again on the merged tree. On the light lane (stage 0, one run at a time, under 2 GiB, no lock; llvm-mc and llvm-tblgen 23.1.5 by
hand only). `asm-ax4c` (`49bd71464`, AX4's third slice, the decoder) was merged on top afterwards (the last item below).

- **The REX conflict** (`asm.ae`, REVIEW2 §0.1): asm-tg's `emitREXPrefix` (`rex-kind`: `:rex`, `:rex2` or nil) kept, asm-ax4b's
  `rex?` volatile dropped; `riprel-kind` reads the prefix's kind and chooses X86MCCodeEmitter 23.1.5's fixup kinds (ported, not
  classed): `reloc_riprel_4byte_movq_load{,_rex2}`, `_relax`, `_relax_rex`, `_relax_rex2`, and `_relax_evex` for ADD64's NDD and NF
  memory forms; `obj.ae`'s relocation types as X86ELFObjectWriter maps them (GOTPCREL: GOTPCRELX, REX_GOTPCRELX, CODE_4_GOTPCRELX;
  GOTTPOFF: CODE_4_GOTTPOFF, CODE_6_GOTTPOFF; TLSDESC: CODE_4_GOTPC32_TLSDESC; EVEX's GOTPCREL stays R_X86_64_GOTPCREL, as LLVM's).
  With APX encoded, the object check reached `tlsdesc-64` and found one more gap: `call *a@tlscall(base)` is mod 0 with no
  displacement and an FK_NONE fixup at the instruction's start (emitMemModRMByte's S_TLSCALL case), now ported. The linker leaves the
  CODE_4/CODE_6 types to its `:reloc` class (lld relaxes them; not yet here). A new synthetic object, `test/asm/obj/apx-relocs.s`,
  holds every one of these kinds (recorded by llvm-mc 23.1.5 by hand; `gotpcrelx.s`, outside the corpus, equal by hand too).
- **AX4's records on the new table and encoder** (§0.2): object-check and run-check run on the merged tree, both recorded from
  passing runs. The object record changes by the 11 tests ax4b had classed `:unsupported-apx` (now `:pass`) and the new synthetic
  file; the run record is unchanged.
- **`bin/repr-asm`** (§0.3): `table` and `encode-check` dispatch by `--arch` (amd64 by default: `table gen|check|oracle`; arm64:
  `table gen|check`, AR2's `table [--check]` renamed, with the Makefile's targets, SPEC §7 and §12); the amd64-only verbs refuse
  `--arch arm64`; the Makefile's `.PHONY` keeps both lines.
- **AXS numbering** (§0.4): AX4's first slice's AXS37-AXS41 are AXS40-AXS44 (above; the journal's entries of this branch edited before
  they reach `next`, `asm-ax4`'s commit messages keep the old numbers); AXS45-AXS50 (AX4b), AXS51-AXS62 (AX5's design) and AX4c's
  from AXS63 unchanged.
- **Controls** (§2.2, §2.3): `arbace.asm.link/control` and `arbace.asm.amd64.obj/control` return `left`, a failing control, when the
  subject did not run or assemble or the reference has no verdict or record (probed: each gives `left`); object-check fails a control
  whose subject is not in its corpus's list; `controls-run.txt`'s header counts its four.
- **AR2's record** (§3.1, §3.2): `encode-check --record` merges the run's corpora's rows into the record (the other corpora's kept)
  and is refused from a run with a problem; without `--record` a recorded file of the run's corpora that the run did not judge fails
  ("recorded, not run": probed with `arm-poe2` left out of the list). Recorded from the merged tree: counts and non-pass set
  unchanged, three rejected lines' reasons (`umov w1, v0.d[0]`) newer than the record (asm-ar2's own tree gives the same today).
- **The decoder merged** (`mac/asm-ax4c` `49bd71464`, made on `asm-ax4b`'s older table: before asm-tg's 763 conversions and
  AX2d's APX). Conflicts and resolutions: `tablegen.ae`, both branches had fixed the `let` around a `class` the same way (the let
  stack recorded with the class and applied after its superclasses, before its body, TGParser's ParseObjectBody): asm-tg's
  `:frames` kept, ax4c's `:lets` and its `declare` dropped, one implementation; `gen.ae`, asm-tg's `used-insns` (the shifts by 1
  of NDD forms) kept with ax4c's `dis?` and `dis-insns` added; `asm/amd64/table.edn` not merged by hand but regenerated by `bin/repr-asm
  table gen` from the resolved generator (`table check` byte for byte; 20,715 `:dis` instructions); `bin/repr-asm`, the decode verbs
  amd64's (refused with `--arch arm64`, their help before arm64's); the Makefile's `.PHONY` keeps both lines; the docs both texts.
  The table oracle's `insns` part (asm-tg's AXS37) did not know ax4c's `:dis` key: it now derives it from llvm-tblgen's records with
  RecognizableInstr's rule (the extract carries each def's superclasses and `isCodeGenOnly`, `ForceDisassemble`, `isAsmParserOnly`,
  `Predicates`), all 20,718 rows equal; and by hand (AXS64 (c), in part) the instructions `-gen-disassembler`'s ModRM tables name
  are the table's 20,715 `:dis` instructions exactly (the decision per context and opcode not yet compared). The decode check
  rerun on the merged tree with 0 failing, then recorded: `dis` and `obj` unchanged (24,441 of 24,445, 479 of 479), `rt` 106,070 of
  106,074 (99,505 of 99,509 on ax4c's table): the 6,565 more are encode-check's APX, AMX-FP8, MOVRS, MSR-immediate and USERMSR
  encodings now met and decoded back (145 `rt/` rows rise, none falls), the same four by design (3 `:llvm-nop`, 1 `:eiz-dropped`);
  eight controls fail as they must.
- **Low** (§1.1, §2.4, §0.7): the table oracle's alias half decodes `\xNN` without gawk's `strtonum` (busybox awk; the oracle run by
  hand passes every part); `object_judge.ae`'s docstring names `:unsupported-crash`; LICENSE.md and doc/ARBACE.md say that
  `arbace/asm/` was ASM's path until 2026-10-12 and holds the assembly crop's assembler since 2026-11-10.

| check (merged tree) | result |
|---|---|
| AX1's round trip (amd64) | musl 32/32, `MC/X86` 609/621, `MC/AsmParser` 140/157, the rest classed; 3 controls fail as they must |
| AR1's round trip (arm64) | musl 15/15, libgcc 7/7, box 7/7, Go's testdata 5/5, `MC/AArch64` 400/409, the rest error tests; 6 controls |
| `encode-check` (amd64) | 106,074 of 106,075 stated encodings met (EVEX 71,212, VEX 18,717, legacy 14,353, XOP 1,792; `:no-run` 1); texts 107,451 of 108,601; cross 86,350 of 87,103; error lines 80 of 130; 0 failing; 20 controls |
| `--arch arm64 encode-check` | encode 7,685 of 12,298, print 12,156 of 12,227, reject 5,030 lines classed; 0 failing; 6 controls |
| `table check` (amd64), `--arch arm64 table check` | byte for byte, both; `table oracle` by hand: every part equal |
| `decode-check` (after ax4c) | `dis` 24,441 of 24,445 (4 `:markup`), `rt` 106,070 of 106,074 (3 `:llvm-nop`, 1 `:eiz-dropped`), `obj` 479 of 479; 0 failing; 8 controls |
| `object-check` | **80 of 86** byte for byte (musl 32/32, synthetic 7/7, `mc-x86-obj` 41/47; `:unsupported-directive` 5, `:unsupported-debug-line` 1); 0 failing; 5 controls |
| `run-check` | 18 of 18 by their own verdicts; 4 controls |
| `bin/reap`, `bin/crops-score --judge --strict`, `gate-key-crops-test.bash`, `make check-makefile` | pass (reap: 0 problems) |

## As built: AX5.2 (2026-11-10; branch `asm-ax5`)

On the light lane (stage 0 and TamaGo's go, one run at a time, under 2 GiB but for one slip below; no lock, no gate), on `asm-merged`
(`9f880b7ab`, the decoder of AX4's third slice in it). The user decided AXS17-AXS67 as recommended on 2026-11-10 (~10:30 UTC), AX5's
design among them (AXS51-AXS62). The slice is AX5-DESIGN §8's AX5.2, the frame and the core integer ISA, and the first part of
AX5.3, the lockstep's `sys` mode. doc/asm/AMD64.md §10 describes what was built.

- **Where** (AXS51 (a), AXS52 (a)): `go/arbace/emu` (the address space over crt's `Mem`, the ELF loader, Linux's stack, the kernel
  layer, the tracer) and `go/arbace/x86i` (the decoder, the machine, the flags, the families, the lockstep), Go forms built alone
  with crt and the driver `test/asm/x86i-driver` by TamaGo's go (`bin/repr-asm x86i light`, seconds), in `arbace_crop_paths`, the
  audit's `crop-rules` and `gate-key-crops-test.bash`'s list (no key moves; the test passes). crt is untouched.
- **The decoder from the table** (AXS53 (a), AXS63 (a)): `arbace.asm.amd64.dgen` writes the decisions `arbace.asm.amd64.decode`
  makes as Go forms data (`go/arbace/x86i/dectab.ae`, 846 KB of hexadecimal strings: the 15 legacy contexts of 64-bit mode by the
  attribute mask, each of the four legacy maps' 256 opcodes' decision, every instruction a decision names with its Intel mnemonic
  and specifiers); `bin/repr-asm x86i gen|gen-check` (14 s, 1.2 GiB). The Go decoder is decode.ae's legacy and REX path written
  again in Go; VEX, EVEX, XOP, REX2 and 3DNow! are refused as `:unimplemented` (their contexts at AX5.5 and AX8).
- **The machine** (AXS54 (a), AXS55 (a)): the families alu, unary, mov, stack, branch, muldiv, shift (no rcl/rcr), string, conv,
  atomic (cmpxchg, xadd) and misc, chosen by the table's Intel mnemonic, one routine a family over every size and operand kind; lazy
  flags with the SDM's undefined bits named per kind (`Undef`). About 28 million instructions a second on `alu.s` (8.6 million
  instructions in 0.32 s, one thread).
- **The lockstep's sys mode** (AXS56 (a)-AXS58 (a), AX5.3's first part): pure Go over ptrace, ASLR off by `personality`, x86i started
  from the native's stack (the vdso's auxv entry made AT_IGNORE in both), compared at each system call's entry and exit, the world
  list copied, mmap's addresses and the break's start the native's.
- **The corpus**: `test/asm/run/`'s 16 runnable programs (the two link refusals stay the linker's: `duplicate`, `undefined`, not run)
  and x86i's own hand-written programs, `test/asm/x86i/`: `alu.s` (99 stubs: the alu, unary, shift and rotate families at four sizes
  over twelve edge values, both carries in, the flags the SDM defines; 28,512 lines), `muldiv.s` (25 stubs), `string.s`, `misc.s`
  (every condition by setcc and cmov, xchg, xadd, cmpxchg, bswap, the extensions, high bytes, lahf/sahf, pushfq/popfq, the loops,
  leave, ret imm, lea with a 32-bit address, movabs, brk, mmap, mprotect, munmap) judged by the CPU's runs (`# judge: cpu`, AXS9
  (a)), and eight `fault-*.s` by their stated status (SIGFPE for #DE twice, SIGILL, SIGTRAP, SIGSEGV for a null store, a read-only
  store, hlt and a non-executable fetch). `alu.s` and `muldiv.s` were written by a shell loop over the operations and sizes; the
  generator proper is AX5.4's (AXS60 (a)).

| check | result |
|---|---|
| `bin/repr-asm x86i-check` (`x86i/`) | **28 of 28** programs (16 of `test/asm/run/`, 12 of `test/asm/x86i/`) by their own verdicts and equal to the CPU's runs (status, stdout, stderr) |
| `decode/` | 28 of 28: every instruction of their code sections (about 2,600) as x86i's decoder decodes it equal to `arbace.asm.amd64.decode`'s decoding of the same bytes |
| `lockstep/` (`x86i trace --mode sys`) | 28 of 28 equal (e.g. `misc` 105 system calls with brk and mmap, `string` 288, the faults' signals by number and RIP) |
| controls (`test/asm/controls-x86i.txt`) | 10 fail as they must: verdict ×2, cpu ×2, decode ×2, `X86I_INJECT` against the CPU ×2 (`alu:cf`, `alu:zf`), against the lockstep ×2 (`alu:cf`, `string:df`) |
| `bin/repr-asm x86i gen-check` | the committed decode tables equal to a fresh generation |
| run-check, `bin/reap`, `bin/crops-score --judge --strict`, `gate-key-crops-test.bash`, `make check-makefile` | pass (run 18 of 18; reap 0 problems) |

About 50 s for the check (the programs assembled and linked, run twice and traced, the decode judge), 1.2 GiB at its peak.
What the runs found: xadd's memory form puts the register first (`XADD64rm`'s operands), a 32-bit shift or rotate by 0 still
zeroes the register's upper half, and `and $-16, %rsp`'s flags depend on the stack's address (a test's own mistake, its flags now set
first). Not run in this slice: the decode check (`decode.ae` unchanged) and AX1's and AR1's round trips (no reader touched).

### Proposed (AX5.2)

Numbering from AXS68 (AX4c used AXS63-AXS67).

- **AXS68 How x86i's Go forms are written.** (a) as built: written as Go and converted by repr/go's native reader (`bin/repr-go read
  --goos linux --goarch amd64`, the converter's style, as SC70 (a) and oi's; one package at a time, the decode tables stubbed for the
  read: about 80 s and 1.5 GiB a package), the decode tables written as forms by dgen; (b) hand-written forms (goi's style).
  *Recommended: (a)*: forms are forms; the printer, `go vet` and the build accept both.
- **AXS69 The decode tables' reach.** (a) as built: the legacy and REX maps of 64-bit mode, VEX, EVEX, XOP and REX2 refused as
  `:unimplemented` until the families that need them (AX5.5's SSE/BMI by VEX, AX8's AVX by VEX and EVEX), dgen extended then by their
  contexts; (b) every map now. *Recommended: (a)*: a table no family uses is untested weight (the vector contexts are about 200 of the
  context list).
- **AXS70 The Go decoder's judge.** (a) as built: the programs' code decoded by both decoders (about 2,600 instructions); (b) also
  every legacy encoding the decode check's `rt` level meets and `MC/Disassembler/X86`'s 64-bit strings, decoded by x86i and held to
  `arbace.asm.amd64.decode`'s results, a level of its own (`x86i-decode-check`). *Recommended: (b) at AX5.4*: the corpus is there and
  the Go decoder is a second port of the same rules.
- **AXS71 The hand-written programs' verdict.** (a) as built: the stated exit and `# judge: cpu` (the CPU's run, AXS9 (a)), the flags
  printed masked by what the SDM defines; (b) stated stdout. *Recommended: (a)*: the outputs are tens of thousands of lines no one
  checks by hand; the lockstep (sys now, step at AX5.3) judges them further.
- **AXS72 The lockstep's flags.** (a) as built: at each comparison the six status flags and DF less those the last flag-setting
  instruction leaves undefined (its kind's: a logical operation's AF, a shift's AF and, past a count of one, OF, a multiply's SF ZF AF
  PF, a divide's six, a rotate's OF past one); (b) the design's per-family mask applied to every compare. *Recommended: (a)*: it is
  the SDM's rule per instruction, narrower than a family's, and the step mode needs it anyway.
- **AXS73 The check in the gate.** (a) at AX10 with the row (AXS61 (a)): an essential step, about 50 s and 1.2 GiB, ptrace on the
  host (`ptrace_scope` 1); (b) now. *Recommended: (a)*.
- **AXS74 The next slice.** (a) AX5.3's rest: the `step` mode, the loader's judge (x86i's own stack against the native's snapshot),
  the bisection and the injected faults named by instruction; (b) AX5.4 first (the generator, rcl/rcr, shld/shrd, the bit family,
  signal frames). *Recommended: (a)*: small, and every later slice's synthetic programs are judged by it.

**Rule slips**: (1) the first conversion of the three packages by `bin/repr-go read` in one run, with the decode tables as a
900 KB string concatenation of 9,000 pieces, peaked at 11.6 GB before failing (the light lane's limit is 2 GiB); every later read was
one package at a time with the tables stubbed, under a watcher that kills the session past 2 GiB (`.tmp/memwatch.sh`), at most
1.54 GB, and dgen now writes each table as one literal; (2) two commands named `/dev/fd/1` and `/dev/null` (a `sed` whose output
went nowhere, a `cp` of a file to `/dev/null`), harmless but against the rule.

## As built: AX5.3 and AX5.4 (2026-11-10; branch `asm-ax5b`)

On the light lane (stage 0 and TamaGo's go, one run at a time, every conversion and check under a 2 GiB watcher; no lock, no gate), on
`asm-ax5` (`5c7142cdf`). AXS74 (a): AX5.3's rest first (the step mode, the loader's judge, the bisection), then as much of AX5.4 as the
slice allowed: the rest of the integer ISA AX5-DESIGN §1 names but BMI and ADX (AX5.5's, with their VEX contexts), signal handlers,
and the synthetic programs. doc/asm/AMD64.md §10 describes what was built.

- **The step mode** (`x86i trace --mode step`, AXS56 (a)): the native process single-stepped by PTRACE_SINGLESTEP, x86i stepped beside
  it, compared after every instruction: the registers, RIP, the status flags and DF but the bits the last flag-setting instruction
  left undefined (AXS72 (a): the instruction's own; the native's undefined bits then copied into x86i, AXS76), FS and GS bases, the
  bytes the instruction stored (x86i's address space logs each store; the bytes read back from the native), XMM and MXCSR when the
  instruction names an XMM register, and the writable mappings every 4,096 instructions and at each system call but write(2); mmap's
  address and the break's start are x86i's own there, held to the native's. Single-stepping's artifacts are rules, each named in
  doc/asm/AMD64.md §10 (`rep` per iteration, a `lock` LLVM decodes apart, TF in `pushfq`'s word and in a syscall's R11, rt_sigreturn's
  orig_rax, the forced SIGTRAP of each step's trap, cpuid's and rdtsc's answers).
- **The loader's judge** (AXS57 (a)): every trace's second line; x86i's own initial stack and registers against the native's at its
  exec, masked: AT_HWCAP, AT_HWCAP2, AT_MINSIGSTKSZ and the kernel's AT_RSEQ_FEATURE_SIZE and AT_RSEQ_ALIGN (and AT_HWCAP3/4); the
  vdso's auxv entry x86i omits; and the vdso's and vvar's eight pages below mmap's top (`emu.VdsoSize`, the layout's first mmap below
  them). It found a loader bug at once: Linux maps a segment's whole file pages, so the bytes before `.data` in its page are the file's
  (the ELF header's `0x7f 'E' 'L' 'F'` at 0x202000 in `misc`), not zeros; `emu.LoadELF` maps them so now.
- **The bisection** (`x86i trace --mode bisect`): the sys mode, and at a difference the sys mode again to the last system call both
  agreed on and the step mode from there: the first differing instruction with its text and family, in under a second for `alu`'s
  injected `shift:of` (8.6 million instructions, the difference at instruction 1,572,701, `SHL8r1 AL AL`). The sys mode now also compares
  the bytes write(2) writes (a difference used to show only as "stdout differs" at the end, which no system call bisects).
- **AX5.4's families**: rcl, rcr, shld, shrd (shift); bt, bts, btr, btc, bsf, bsr, tzcnt, lzcnt, popcnt (a new family, bit); enter,
  xlatb, movbe, rdtscp; cpuid's model (leaf 0, 1 with POPCNT and MOVBE, 0x80000000, 0x80000001 with LZCNT, SYSCALL, NX, LM; AXS59).
- **Signals with handlers**: rt_sigaction, rt_sigprocmask, sigaltstack and rt_sigreturn in x86i's layer (crt untouched, AXS52 (a);
  crt's table and mask kept in step, so a kill of the process itself reaches x86i through crt's `Handler` hook), the x86-64 frame laid
  out from the psABI and musl's `arch/x86_64/bits/signal.h` (vendored already, MIT), with the thread's last trap (TRAPNO, ERR, CR2 kept
  stale as Linux keeps them: found by the CPU's run), RF in a fault's saved RFLAGS (found by the lockstep), uc_stack's stored flags (not
  whether the stack is in use: found by the lockstep). The lockstep delivers the native first and takes the host's XSAVE size from its
  frame (AXS77).
- **The synthetic programs** (AXS60 (a)): `test/asm/x86i/gen.ae` (seeded) writes five programs (bit, rotc, shiftd, cond, misc; 82,437
  lines, 3.1 million instructions) into the check's work directory; their digests are committed (`test/asm/x86i/synth-digests.edn`,
  `bin/repr-asm x86i synth DIR [--record]`). Each case sets the flags by popfq and the operands (edge values and seeded ones, a
  register's upper bits filled), runs one instruction, and writes the result and the flags the SDM defines for it. Beside them two
  hand-written programs: `signals.s` (seven faults caught and resumed, the alternate stack, a kill while blocked delivered when
  unblocked) and `cpuid.s` (the model's answers as its stated stdout; `# cpu: cpuid`).

| check | result |
|---|---|
| `bin/repr-asm x86i-check` (`x86i/`) | **34 of 35** programs by their own verdicts and equal to the CPU's runs (16 of `test/asm/run/`, 14 of `test/asm/x86i/`, 5 synthetic); `cpuid` by its stated verdict, the class `:cpuid` against the CPU |
| `decode/` | 35 of 35 (85,633 instructions) as `arbace.asm.amd64.decode` decodes them |
| `lockstep/` (sys) | 34 of 35 equal, `cpuid` the class `:cpuid` |
| `step/` | **35 of 35** equal in the step mode (every instruction compared; `alu` and `muldiv` their first 500,000, `# step-limit`) |
| `loader/` | 35 of 35: x86i's own initial state as Linux's |
| controls (`test/asm/controls-x86i.txt`) | **18** fail as they must: AX5.2's ten, `step` ×3 (`misc` `shift:of`, `string` `string:df`, `synth-bit` `bit:cf`: caught at an instruction of the family), `bisect` ×3 (`alu` `shift:of`, `calls` `alu:cf`, `synth-rotc` `shift:cf`: bisected to one), `loader` ×2 |
| `bin/repr-asm x86i gen-check`, run-check, `bin/reap`, `bin/crops-score --judge --strict`, `gate-key-crops-test.bash`, `make check-makefile` | pass (run 18 of 18; reap 41 of 44 cells checked, 0 problems) |

The step mode runs at 20,000-37,000 instructions a second on an idle host (25,000 for `signals`, 33,000 for `synth-rotc`) and 5,000-11,000
under the night's load (the recorded check ran beside night 11: 15.5 minutes, 1.3 GiB at its peak); a system call costs the most (a
write's single step and the memory comparison).

### Proposed (AX5.3, AX5.4)

Numbering from AXS75.

- **AXS75 The step mode's reach.** (a) as built: every program in both modes, the step mode bounded by a header (`# step-limit: N`, alu's
  and muldiv's 500,000; the rest in sys mode); (b) the step mode for the synthetic and the essential programs only (the design's words),
  sys for the rest. *Recommended: (a)*: whole runs in step mode cost minutes only for the long hand-written ones, and the bound is stated
  where it applies.
- **AXS76 The undefined flags in the step mode.** (a) as built: masked in the compare, then the native's copied into x86i, so a later
  reader (pushfq, adc, a condition) sees the same bits in both; (b) masked only, a later reader's difference then a finding to class.
  *Recommended: (a)*: the masks still come from the SDM, never from the CPU (the design's risk), and x86i's own runs keep its fixed values.
- **AXS77 The fpstate's size in a frame.** (a) as built: x86i's model's (`XSaveModel`, 580 bytes: FXSAVE, the XSAVE header, the end's
  magic word) in its own runs, the host's in the lockstep (from the native's first frame), the host's XSAVE components x86i does not model
  copied after MXCSR and XMM are compared; (b) the model's always, the native's frames compared shifted. *Recommended: (a)*.
- **AXS78 Single-stepping's signal artifacts.** (a) as built: x86i follows the traced native in step mode (SIGTRAP unblocked by each
  step's forced trap, the thread's last trap #DB), so a SIGTRAP handler is judged in both modes; (b) SIGTRAP handlers judged in sys mode
  only. *Recommended: (a)*: the rule is the kernel's documented forced-signal behaviour, and only the step mode applies it.
- **AXS79 The layout's vdso pages.** (a) as built: x86i reserves Linux 6.18's eight vdso and vvar pages below mmap's top, held to the
  native's maps by the loader's judge (a kernel that maps another size fails it, naming the span); (b) the native's mmap addresses always
  taken (the sys mode's way), the layout never judged. *Recommended: (a)*.
- **AXS80 tzcnt and the model.** (a) as built: tzcnt runs as BMI1's (this CPU's behaviour, which the lockstep holds x86i to) while the
  model claims BMI1 only at AX5.5 with andn and the rest; (b) claim BMI1 now and refuse its VEX forms as `:unimplemented`.
  *Recommended: (a)* until AX5.5, a slice away.
- **AXS81 The next slice.** (a) AX5.5: the VEX contexts in dgen, BMI1/2 and ADX, cmpxchg16b, rdrand/rdseed/rdpid, SSE/SSE2 with MXCSR's
  flags (AXS62 (a)), the per-run coverage, and AXS70 (b)'s decode level; (b) AX5.6's musl harnesses first. *Recommended: (a)*.

**Rule slips**: three wait loops used a process substitution (`<(...)`) in a `grep`, against the rule; harmless (they read the
check's and the conversion's logs). One command ran `x86i trace` without the worktree's TMPDIR exported, so its temporary files went to /tmp (removed by the
run itself).

## As built: AX5.5 (2026-11-10; branch `asm-ax5c`)

On the light lane (stage 0 and TamaGo's go, one run at a time, every conversion and check under a 2 GiB watcher; no lock, no gate), on
`asm-ax5b` (`81a49bc21`). AXS81 (a): the VEX contexts, AX5's integer extensions (AXS59 (a)), SSE and SSE2 with MXCSR's flags computed
(AXS62 (a)), the per-run coverage and AXS70 (b)'s decode level. doc/asm/AMD64.md §10 describes what was built.

- **The VEX contexts** (AXS69's "until needed"): `arbace.asm.amd64.dgen`'s attribute mask takes VEX and VEX.L beside the five legacy bits
  (128 masks, 32 contexts: the sixteen IC_VEX ones added), so `go/arbace/x86i/dectab.ae` holds every VEX instruction of maps 0F, 0F38
  and 0F3A (846 KB to 1.9 MB; `x86i gen`, 56 s, 1.39 GB). The Go decoder reads C4 and C5 as decode.ae does (REX's bits from VEX's
  inverted R, X, B and its W, VEX.pp as the mandatory prefix in the context, VEX.L, VEX.vvvv with its validity, an imm8 naming a register,
  VSIB, YMM, mask and tile registers as operands x86i does not model); EVEX, XOP, REX2, 3DNow! and VEX's maps 5-7 stay refused.
- **The integer extensions** behind the model's CPUID (leaf 0's highest 7; leaf 1's ECX RDRAND, POPCNT, MOVBE, CMPXCHG16B; leaf 7's EBX
  BMI1, BMI2, RDSEED, ADX, ECX RDPID; `cpuid.s`'s stated stdout follows): a family `bmi` (andn bextr blsi blsmsk blsr bzhi, their
  undefined flags per instruction; mulx pdep pext rorx sarx shlx shrx; adcx adox), cmpxchg8b and cmpxchg16b, rdrand, rdseed, rdpid (the
  step mode copies the native's value, as rdtsc's).
- **SSE and SSE2** (`sem_sse`): the families `sse` (moves, halves, sign masks, words, logic, shuffles, unpacks, maskmovdqu, ldmxcsr and
  stmxcsr, movnti, the hints), `sseint`, `ssefp` and `ssecvt`, 342 instructions claimed; an instruction with an XMM operand is bound in
  SSE's table, so `movsd` and `cmpsd` go to SSE's or the string family's by their operands. The floating point in software at round to
  nearest: Go's float64 arithmetic (IEEE 754's), a single's result the double's rounded again (innocuous: 53 ≥ 2·24 + 2), MXCSR's
  flags from the operands and the exact error terms (two-sum, a fused multiply-add; math/big near the range's bottom, where a term can
  underflow: the step mode found divpd's lost PE there), tininess after rounding as x86 detects it. A rounding mode other than nearest,
  DAZ, FTZ or an unmasked exception raises SIGILL as `:unimplemented` (AX8), as rcpps and rsqrtps do (each vendor's approximation).
- **The lockstep** compares XMM and MXCSR after every SSE instruction; `X86I_INJECT=FAMILY:xmm|pe` flips an XMM destination's lowest bit or
  MXCSR's PE.
- **The coverage** (`x86i run --coverage`, `x86i claimed`): x86i-check sums the programs' executed instructions against the families'
  claims, by family, into `coverage.txt`, held to `test/asm/x86i-coverage-amd64.txt`.
- **The decode check** (`bin/repr-asm x86i-decode-check [--record]`, AXS70 (b)): MC/X86's 64-bit met encodings and MC/Disassembler/X86's
  64-bit byte strings, each decoded alone by `x86i decode-hex` against arbace.asm.amd64.decode (`decode-check`'s `x86i-corpus`); rows
  `test/asm/x86i-decode-results-amd64.edn`, the class `:encoding-deferred`, controls `test/asm/controls-x86i-decode.txt`.
- **The synthetic programs**: seven new families (bmi, adx, ssemov, sseint, ssefp, ssecmp, ssecvt; `lib/hex.s`'s `put_vec`), seeded,
  their digests beside AX5.4's five (unchanged); and `fault-movaps.s` (a misaligned movaps: SIGSEGV).

What the CPU's runs and the step mode found on the way: 0 divided by infinity is exact (the fused multiply-add's term is a NaN there); an
invalid operation and a division by zero report their flag alone, without the denormal operand's (AXS84); divpd's inexact result with a
denormal operand needs the exact value (the remainder underflows).

| check | result |
|---|---|
| `bin/repr-asm x86i-check` (`x86i/`) | **42 of 43** programs by their own verdicts and equal to the CPU's runs (16 of `test/asm/run/`, 15 of `test/asm/x86i/`, 12 synthetic); `cpuid` by its stated verdict (the widened model), the class `:cpuid` against the CPU |
| `decode/` | 43 of 43 (201,218 instructions) as `arbace.asm.amd64.decode` decodes them |
| `lockstep/` (sys) | 42 of 43 equal, `cpuid` the class `:cpuid` |
| `step/` | **43 of 43** equal in the step mode; the twelve synthetic programs 5.3 million instructions compared one by one (the seven new ones 2.2 million, XMM and MXCSR at each SSE instruction) |
| `loader/` | 43 of 43 |
| coverage (`test/asm/x86i-coverage-amd64.txt`) | 702 of the 1,291 instructions the families claim executed: bmi 48 of 60, sse 116 of 134, sseint 92 of 100, ssefp 48 of 72, ssecvt 26 of 36, atomic 10 of 18 (the rest by family, mostly memory and `_REV` forms the programs do not write: AXS88) |
| controls (`test/asm/controls-x86i.txt`) | **23** fail as they must: AX5.4's 18 and AX5.5's five (`step` synth-bmi `bmi:cf`, synth-ssefp `ssefp:xmm`, synth-sseint `sseint:xmm`; `bisect` synth-ssecvt `ssecvt:pe`; `inject` synth-adx `bmi:of`) |
| `bin/repr-asm x86i-decode-check` | 487 files, 82,988 byte strings: **20,531 decoded as arbace.asm.amd64.decode decodes them** (13,336 of them VEX), none different; 62,457 in encodings x86i does not read yet (EVEX 60,624, XOP 1,287, REX2 426, VEX maps 5-7 36, 3DNow! and prefixed 84), 324 files `:encoding-deferred`, 163 all equal; three controls fail as they must; about 3 minutes, 1.45 GB |
| `bin/repr-asm x86i gen-check`, run-check, `bin/reap`, `bin/crops-score --judge --strict`, `gate-key-crops-test.bash`, `make check-makefile` | pass (run 18 of 18 ok; reap 41 of 44 cells checked, 0 problems) |

The recorded check took 19 minutes beside night 11 (1.35 GB at its peak), the step mode at 8,000-14,000 instructions a second. The
decode check found two of the Go decoder's corner cases on its first run: a byte string read in its stream's context (a lone F3 is
XRELEASE before 90: the corpus gives each instruction the fifteen bytes from it and its length), and fifteen prefixes with no room for
an opcode (invalid, as LLVM's decoder finds with the bytes after).

### Proposed (AX5.5)

Numbering from AXS82.

- **AXS82 The VEX tables' reach.** (a) as built: every VEX instruction of maps 0F, 0F38 and 0F3A in the decode tables (1.9 MB), though
  only BMI's run, the rest decoded and judged by x86i-decode-check, SIGILL at run time; (b) only the rows of the claimed families.
  *Recommended: (a)*: the decode check judges them all, and AX8's AVX needs them.
- **AXS83 The floating point's source.** (a) as built: Go's IEEE double arithmetic with the exact error terms and math/big near the bottom,
  round to nearest only; (b) a software-float port (Berkeley SoftFloat 3, BSD-3, its 8086-SSE specialization) for every rounding mode
  now. *Recommended: (a)*, (b) weighed at AX8 with the rounding modes, DAZ and FTZ.
- **AXS84 The denormal flag beside an invalid operation or a division by zero.** (a) as built: DE dropped then (the SDM's priority of
  the SIMD exceptions read as the highest alone for these, which this AMD CPU does; a QNaN operand, by the SDM, already suppresses DE),
  another vendor's difference a class `:vendor`; (b) both flags. *Recommended: (a)*: the step mode compares MXCSR at every
  instruction, and the reading is the one both the CPU and the SDM's QNaN rule support.
- **AXS85 rcpps, rsqrtps, rcpss, rsqrtss.** (a) as built: unclaimed (SIGILL, `:unimplemented`): their results are each vendor's
  approximation tables; (b) AMD's and Intel's tables written, the model's vendor choosing. *Recommended: (a)* until a program needs them.
- **AXS86 The coverage's record.** (a) as built: the claimed instructions never executed listed per family in a committed file, an
  instruction executed when recorded and not now failing the check; (b) a report only. *Recommended: (a)*.
- **AXS87 The decode check in the gate.** (a) at AX10 with x86i-check (AXS73 (a)); (b) now. *Recommended: (a)*.
- **AXS88 The next slice.** (a) AX5.6: musl's harnesses; (b) the claimed instructions the synthetic programs leave unexecuted first.
  *Recommended: (a)*, the coverage list beside it.

**Rule slips**: one command ran process substitutions (`<(...)`: a leftover no-op `cmp` loop and a `diff` comparing a printed file
with its source) and one test fed `x86i decode-hex` from `/dev/stdin` (a here-string), both against the rule and both harmless; every other wait loop polled a
log file.

## As built: AX10 (2026-11-10; branch `asm-ax10`)

The user decided on 2026-11-10 to bring AX10 forward (reversing the roadmap's CSP7, which had kept assembly out of `crops.edn` until
AX10; doc/CROPS-SCORE.md, "Decided (the user, 2026-11-10)"), with the gate steps AXS21 (a), AXS42 (a), AXS65 (a), AXS73 (a) and AXS87
(a) had left to it. On the light lane, on `mac/asm-ax5c` (`f05515abf`) with `mac/asm-ar2b` (`8744bfdba`) merged (both on `asm-merged`;
the conflicts in `bin/repr-asm`'s help and the journal, both sides kept), every asm check rerun after the merge, none failing.

- **The REAP rows** (`test/reap/cells.edn`, `:asm-amd64` "x86-64 asm" and `:asm-arm64` "arm64 asm", 13 rows, 52 cells): three readers in
  `test/reap/main.ae` take bin/repr-asm's records with the class each check's own judge found (`:class-id`): `:asm-rows` (rows
  `[ID RESULT REASON]`: the round trips, by REASON as `test/asm/judge.ae` classifies; the object, run, x86i and x86i-decode checks, whose
  RESULT is the class), `:asm-counts` (encode-check's and decode-check's per-file `{:stated :met :by}`, the file's own level or `:text` or
  `:cross`; stated must be met plus the classes) and `:asm-arm64` (the arm64 encode check's `{:files :non-pass}` at a level, classed as
  `arbace.repr.asm.arm64.encheck` classifies). The classes' `:by-design` is `:design`; `:pending` ("a failure to fix") maps to nothing, so a
  case in one fails the matrix. The cells:
  - x86-64 read: the round trip (810 files) and decode-check's dis level (MC/Disassembler/X86, 24,445 CHECK lines): 25,222 / 1 / 32.
  - x86-64 eval: x86i-check's programs by their verdicts and the CPU's runs, the lockstep's sys and step modes (43 programs each):
    127 / 2 / 0 (`cpuid`, by design); beside it the loader's judge, x86i's decoder on the programs and x86i-decode-check (163 of 487 files,
    324 `:encoding-deferred`).
  - x86-64 AOT: run-check (18 of 18) and object-check (80 of 86, 6 deferred): 98 / 0 / 6; beside it encode-check's 106,074 of 106,075
    encodings and decode-check's rt and obj levels.
  - x86-64 print: encode-check's print level (107,451 of 108,601 CHECK texts), the cross-syntax judge (86,350 of 87,103) and the round
    trip's mode `:full`: 194,582 / 1,532 / 400.
  - arm64 read: AR1's round trip (434 of 444) and AR2's input level (6,212 of 6,212): 6,646 / 0 / 10; the reject level beside (4,176 of
    5,030, the rest classed).
  - arm64 print: AR2's print level (12,222 of 12,227) and the round trip's mode `:full`: 12,656 / 5 / 10.
  - arm64 eval and AOT: no check yet (a64i, AR5; arm64's ELF, linker and runs, AR4), the encode level (12,290 of 12,298) beside AOT.
  `bin/reap`: 47 of 52 cells checked, 0 problems; `test/reap/matrix.edn` recorded.
- **Controls** (GTS81 (a)): every check family already had its own (`test/asm/controls*.txt`) but arm64's print level, whose judge
  the encode controls do not reach: `arbace.repr.asm.arm64.encheck/control-print` (`_control/print-...`: the subject's CHECK text read and
  printed as the print level prints it, held to the reference's text) and four pairs in `test/asm/controls-encode-arm64.txt` (two ADD
  extends, LDR against STR, FADD against FSUB in FP16, two system registers); each fails as it must, and a self-pair, tried once, is
  reported "same" and fails the check.
- **crops-score** (`test/crops/crops.edn`: the two crops, their steps `^(asm-amd64|x86i)-` and `^(asm-arm64|a64i)-`, a control rule
  per check family; `independence.edn`: the stages; the host `:llvm-mc` a toolchain host): x86-64 assembly completeness 97.9% (98.5%
  without by design), maturity 100.0%, independence 100.0% (judge 0.9998: 221,916 cases by their own verdicts, the object check's 86
  files against llvm-mc's recorded objects); arm64 assembly 49.9% (49.9%), 91.7% (two of four cells), 60.0% (eval and AOT 0, no
  stage yet). `crops.score`'s record reader took a crop id of letters alone; it takes hyphens now.
- **The gate** (`test/gate/crops/asm.edn`, `asm-oracle.edn`, spliced into `test/gate/steps.edn` by `gate.crops splice`): essential
  `asm-amd64-read-essential` (musl and MC/AsmParser's round trip), `asm-amd64-run-essential`, `asm-amd64-object-essential` (musl and
  synthetic, AXS42 (a)) in the chain `asm-amd64-essential`, `asm-arm64-read-essential` (the whole arm64 round trip) and
  `asm-arm64-print-essential` (the whole encode check) in `asm-arm64-essential`; `--full` `asm-amd64-roundtrip`, `asm-amd64-encode`,
  `asm-amd64-decode` (parts dis, rt, obj), `asm-amd64-object` (parts musl, synthetic, mc-x86-obj), `asm-amd64-table` (`table check`,
  `x86i gen-check`), `x86i-check`, `x86i-decode`, `asm-arm64-table`; the oracle tier's `asm-amd64-print-oracle`, `asm-amd64-table-oracle`,
  `asm-amd64-link-oracle`, `asm-arm64-table-oracle` on the host `:llvm-mc` (llvm-mc, llvm-tblgen, ld.lld). Every step on stage 0 (and
  TamaGo's go where x86i's light driver or Go's testdata are read), each with `:mem-gib` and a control file or a waiver (the table
  checks: a generation compared byte for byte). AXS73's essential x86i-check is `--full`: it measured 14 min 44 s (the step mode of 43
  programs), not AXS73's 50 s; AXS65's decode check likewise (dis 5 min 39 s). `bin/repr-asm corpus check` (the pins, `:crop-pins`) and
  `corpus ensure` (the corpora extracted once per `test/asm/corpora.edn`, under a lock) are new; the steps call `ensure`. Budget: half a
  share each (AXS16 (a), ARS15), `:budgets :crops` 11.
- **The budget measured** (by hand, `/usr/bin/time -v`, one at a time, written as `.tmp/gate/NAME.mem` for `bin/gate --costs`; no pass
  recorded): the essential chains 1 min 20 s (amd64: 10 s, 23 s, 47 s; 0.8 GiB at most) and 1 min 18 s (arm64: 33 s, 45 s; 0.9 GiB),
  under GTS215's 2 minutes and 3 GiB. `--full` whole was 40 min 24 s for amd64, 594% of half a share (6 min 48 s); the heavy parts are
  `:clean-only` (encode-check 9 min 31 s, decode dis 5 min 39 s and rt 2 min 1 s, the tables 3 min 41 s, x86i-check 14 min 44 s), the
  night's 4 min 48 s (71%); arm64's table 1 min 28 s (22%). Every step's peak under 1.5 GiB.

### Proposed (AX10)

Numbering from AXS89.

- **AXS89 x86i-check at night.** (a) as built: `:clean-only` (the eval cell's only check runs at the clean run and by hand, `make
  x86i-check`); (b) nightly whole, over the share; (c) `x86i-check --only LIST` and a rotation of its programs (GT8.4's `:rotate`), the
  step mode's cost spread over nights. *Recommended: (c)*, a small change to bin/repr-asm, then the part nightly.
- **AXS90 encode-check at night.** (a) as built: `:clean-only`; (b) by corpus file rotation (`encode-check` takes corpora, not files).
  *Recommended: (a)* until the share is measured from passes.

## As built: AXS89, AXS90 (2026-11-10; branch `asm-axs89`, on `asm-ax10`)

The user decided on 2026-11-10 (the brief's lettering: its (a) for both): AXS89, x86i-check's `--only LIST` and its programs' rotation (the
survey's option (c) above), and AXS90, encode-check from `:clean-only` into the nightly `--full` (whole: the survey's options named no such
choice; (b), a rotation by corpus file, is not built). On the light lane, one check at a time.

- **`bin/repr-asm x86i-check --only LIST`**: LIST a file of program names (a line each; the names of `test/asm/run/`, `test/asm/x86i/` and the
  synthetic programs; an unknown name or an empty list refused). The slice's programs are assembled, linked and run, each with its lockstep's
  sys and step modes, its decode and its loader judge; the rest is judged on what it ran: `test/asm/x86i_judge.ae`'s `main` cuts the recorded
  rows to the list's programs ("recorded, not run" only among them), the per-run coverage is printed and not held to the record (a slice
  executes a part of it), a control whose subject (for verdict, cpu and decode also whose reference program) is not in the list is not judged
  (the count printed: the subset 15 of 23 judged, a slice of 5 programs 2), `--record` refuses `--only`. The negative control of a partial
  run, `slice-foreign` (the user's): the same run held to the recorded rows of the programs it did not run must fail (`not recorded`); it prints
  `control slice-foreign: fails as it must (N of its rows not in the other slices' reference)`, and the check fails when it does not. The shell
  now writes each program's seconds into `WORK/times.tsv` and prints the build's and the run's seconds. Without `--only` the verb is as before
  (the whole run, tried: 43 of 43 programs by their verdicts but `cpuid`'s two by design, rows equal, 18 controls and the coverage held).
- **The rotation** (`test/gate/crops/asm.edn`, `test/gate/steps.edn`: `x86i-check`'s part `all` loses `:clean-only` and gains `:rotate {:only
  "--only {list}" :universe "test/asm/x86i-results-amd64.edn" :subset "test/asm/subsets.edn" :id-re ... :failed-re ...}`; the id is the
  program's name, the five rows of a program one id, `:failed-re` reads `FAIL: failing: x86i/NAME ...`): GT8.4's mechanism as Scheme's and
  OCaml's (`test/asm/subsets.edn`: the map `{:id :all :subset [...]}`): a subset every night (`x86i-check.all.sub`: the 13 quick programs
  that hold the controls' subjects and references: p4-hello, exit-status, calls, loop-sum, tls, relocs-data, weak, comdat, musl-string,
  musl-setjmp, two-objects, string, misc), and one of seven slices of the other 30 a night (`x86i-check.all.sK`, a program's K the first 8 hex
  digits of the SHA-256 of its name mod 7, the planner's own); `--full` runs the whole set and the clean run's whole tier too (`GATE_CLEAN_WHOLE`
  as every rotated part). Not built: slices cut by hand to a time (the planner's slices are its hash's: with this subset they hold 6, 43, 20,
  21, 19, 48 and 69 seconds of run each by `times.tsv`, the heaviest, `alu`, `synth-bit` and three quick ones, being slice 6).
  `bin/gate --nightly --plan` (GATE_NIGHT=3 and 6) names `x86i-check.all.sub` (13 files) and `.s3` or `.s6` (5 files); `--full --plan` the whole
  `x86i-check.all`, `--oracle --plan` no table error.
- **AXS90**: `asm-amd64-encode.mc-x86` no longer `:clean-only` (the nightly whole, 9 min 31 s).
- **Measured** (by hand, `/usr/bin/time -v`, one at a time on stage 0, the memory checked first; the encode check's and the other asm
  steps' numbers the earlier measures', `.tmp/gate/NAME.mem` of `asm-ax10`): x86i-check whole 8 min 39 s (asm-ax10 measured 14 min 44 s, another
  load; 1.3 GiB; build 173 s, runs 228 s), the subset 38 s (1.0 GiB), slice 6 (5 programs) 2 min 28 s (1.2 GiB: build 34 s, run 62 s,
  the rest the synthetic programs' generation, the light driver and the judge). A night is the subset and a slice: 38 s plus about 1 min
  30 s on average (an estimate from `times.tsv`), 3 min 6 s the heaviest. `bin/gate --full --costs`: asm-amd64's full tier 22 min 58 s, 338% of
  half a share (6 min 48 s), because `--costs` counts the whole `x86i-check.all` (it does not know the rotation: Scheme's and OCaml's rotated
  parts have the same blind spot); counted as the night runs it, 16 min 29 s on average (242%) and 17 min 25 s the heaviest night (256%).
  The encode check is the cost: without it the night is 6 min 58 s (102%). Proposed (AXS91): encode-check rotated by corpus file in sevenths
  (about 1 min 22 s a night, the night at about 8 min 20 s, 122%), or the share raised.

## As built: AXS91 (2026-11-11; branch `fixes-1111`)

The user decided AXS91 (a): the nightly `asm-amd64-encode.mc-x86` rotated by corpus file in sevenths (GT8.4), a seventh a night, the whole once a week.

- **`bin/repr-asm encode-check --only LIST`** (amd64): LIST a file of mc-x86's file names, bare, a line each (the names of the record's `mc-x86/NAME`
  rows; an unknown name, an empty list, another corpus or `--record` refused). Only those files are checked; `test/asm/encode_judge.ae`'s `main`
  takes a seventh argument (`partial`): every file of the run is still held to its record row, the recorded files the run did not hold are
  not "recorded, not run", and the controls whose subject or reference is not in the list print "left (not in this run)" as before.
- **The step**: `asm-amd64-encode` has a `:rotate` (test/gate/crops/asm.edn and steps.edn, the same hunk: `gate.crops splice` adds a fragment's
  steps and does not replace one already spliced): `:universe` the encode record, `:id-re` its bare names, `:subset` test/asm/subsets.edn's
  `:mc-x86`, `:always` the same 15 files, `:failed-as "\\3"` (sed -E: no `(?:`). The subset is the subjects and references of the encode, print
  and cross-syntax controls but the two heavy pairs, AVX-64/AVX2-64 and avx512-intel/avx512f_vl-intel (17,000 of the 106,000 stated encodings:
  with them the subset alone ran 4 min 47 s); those two controls run in `--full` and in the clean run's whole tier, and in a slice only when both
  files fall in it. A night is the subset and one slice of the other 606 files (`.sub` and `.sK`, `bin/gate --nightly --plan`: 426 rows of the
  universe, 15 in the subset); `--full` runs the whole.
- **Measured** (by hand, one at a time on stage 0, 2026-11-11): the subset (15 files, 8,992 stated encodings, all 14 controls that have both files
  judged: fail as they must) 2 min 11 s and 0.63 GiB, so the fixed part (the table and the judge) is about 1 min 20 s of it; the whole 9 min 31 s
  (AXS90), so a slice about 1 min 10 s of work above the fixed part: a night about 3 min 30 s against the whole's 9 min 31 s, not the 1 min 22 s the
  proposal estimated (it left the fixed part out). The night is then about 10 min 20 s (about 150% of half a share by AXS89's count). Not built:
  fewer controls in the subset (each file taken out is a control that runs one night in seven or none).
- Proofs: `gate.crops check` 0 problems, `test/gate/crops-test.bash` ok, `rotation-test.bash` 65 of 65, `bin/gate --full --plan` no table error,
  `GATE_NIGHT=3 bin/gate --nightly --plan` names `asm-amd64-encode.mc-x86.sub` and `.s3`.
- Not changed, to look at: x86i-check's `:failed-re` (steps.edn, AXS89) uses `(?:...)`, which `sed -E` (the runner's use of `:failed-re`) does not
  read; its `:id-re`'s `(?:...)` is the planner's Java regex and fine.

## As built: AX11 (2026-11-11; branch `asm-ax11`, from `mac/next` `15278e9c6`)

The x86-64 AOT cell's non-pass classes, cheapest first: the object check's six `MC/X86` files llvm-mc 23.1.5 assembles and Arbace did
not (five `:unsupported-directive`: `.ifdef`, `.reloc` twice, `.rept` with `.reloc`, `.macro`; one `:unsupported-debug-line`). All six are
now llvm-mc's objects byte for byte; the cell went from 98 / 0 / 6 (94.2%) to 105 / 0 / 0 (100.0%), with a new run program. No llvm-mc ran:
the objects are held to the records made by hand (`test/asm/records/obj-mc-x86-obj-llvm-mc-23.1.5.edn`, unchanged).

- **Conditional assembly** (`arbace.asm.obj/run-items!`, after AsmParser's parseDirectiveIf*, Else*, EndIf: TheCondStack): `.if`,
  `.ifne`, `.ifeq`, `.ifge`, `.ifgt`, `.ifle`, `.iflt`, `.ifdef`, `.ifndef`/`.ifnotdef`, `.ifb`, `.ifnb`, `.ifc`, `.ifnc`, `.ifeqs`, `.ifnes`,
  `.elseif`, `.else`, `.endif`, nested, an ignored part's conditionals still counted, an unmatched one an error. The reader already
  read both branches as forms; the assembler skips the ignored ones. `pltoff` (`.ifdef ERR` around an error line) passes.
- **Macros and repetitions** (parseDirectiveMacro, handleMacroEntry, expandMacro at a GNU target, parseMacroArguments,
  parseDirectiveRept/Irp/Irpc): the reader keeps a block's body as text; the assembler expands it as text (`\NAME`, `\@` the
  instantiations so far, `\+` the macro's count, `\()`; positional, default, keyword `NAME=V`, `:req` and `:vararg` arguments; commas,
  or blanks between operands) and reads it with the arch's new `:read-text` in the syntax current at the expansion, 20 levels deep
  at most; `.purgem`; a macro defined by an expansion is invoked from the file through `:insn-text` (the reader took the line for an
  instruction: printed back, the name looked up among the macros first, as AsmParser does). `.rept`'s body without a backslash is
  read once. `compact-unwind-cfi_def_cfa` (two macros with CFI) passes.
- **`$` and an integer as a name**: `sub $$0x80+8, %rsp` (the macro's text at an ELF target: no Darwin `$$` substitution) is an
  immediate of the undefined symbol `$0x80` plus 8 in llvm-mc (AsmParser::parseIdentifier joins `$` with an adjacent identifier or
  integer); `arbace.repr.asm.gnu`'s primary does too, the integer as spelled (`*src*`, bound by `read-items`). A shared file: it moves
  the round trip's rows that refused `$` followed by an integer (see below).
- **x86-64's `.reloc`** (`arbace.asm.amd64.obj`'s directive: X86AsmBackend::getFixupKind at ELF, `R_X86_64_*` by name and
  `BFD_RELOC_NONE/8/16/32/64`); the shared relocation of a directive now names its symbol, a local one too (`.Lbase` stays `.Lbase`, as
  llvm-mc writes it; a section's own symbol is the section's), and with the arch's `:reloc-directives-merged` (x86-64's) a section's
  directives are merged with its own relocations by offset, the directives in their order, a tie the own first: llvm-mc 23.1.5's
  order in `reloc-directive-with-inst-relocs` ("emitted in groups") and `reloc-directive-tlsgd`. arm64 keeps them after its own: the
  merge reorders MC/AArch64's `patchinst` (its PATCHINST after both JUMP26s, as llvm-mc's AArch64 writer puts them; AXS93).
  `reloc-directive-elf-64`, `-with-inst-relocs` and `-tlsgd` pass.
- **The DWARF 4 line table's header** (MCDwarfLineTableHeader::Emit at llvm-mc's default version 4): `.file N "NAME"` and `.file N
  "DIR" "NAME"` (tryGetFile: a NAME's directory split off, the directories numbered from 1) make `.debug_line` at the end (after
  `.eh_frame`, as finishImpl orders them) with its header and no line program; `.loc` checks its file and marks a location seen, which a
  section switch clears (MCStreamer::switchSection); an instruction or data while one is seen is a line entry, still
  `:unsupported-debug-line` (the program is not written yet: AXS92). `line-table-sections` (a `.loc` at a section's end) passes.
- **The run check's new program** `test/asm/run/macros.s` (by its own verdict: exit 42, five lines): the macros' arguments, `\@`, `\()`,
  a macro's macro, `.purgem`, `.rept`, `.irp`, `.irpc` and the conditionals nested and ignored, run by the CPU; 19 of 19; its control
  `_control/macros-vs-loop` fails as it must. x86i runs it too: `x86i-check` 44 programs, 43 by their verdicts and equal to the CPU's
  (`cpuid` by design, as before), every mode, recorded; the eval cell 127 / 2 / 0 -> 130 / 2 / 0.
- **`x86i-check`'s native runs** used `$runner`, which only `run-check` sets (arm64's emulator, AR4): under `set -u` every CPU run
  failed (`runner: unbound variable`) on `mac/next`; now `${runner:-}`.
- **The round trip's rows** of the two files that refused `$` and an integer now refuse later in them, the same class
  (`mc-asmparser/motorola_integers`: `unexpected %` at line 8, `-motorola-integers`' `%01010`; `mc-x86/x86_errors`: an error test, at
  line 129); recorded. arm64's round trip unchanged.
- **Proofs** (stage 0, one at a time, each under 1.5 GiB): `object-check mc-x86-obj` 47 of 47 (with its control), `musl` 32 of 32,
  `synthetic` 6 of 6; `run-check` 19 of 19 and five controls; `x86i-check` whole (7 min 20 s) with its controls; the round trip 810
  files; arm64's `object-check` (both corpora), `object-verdict`, `run-check`, `bin/a64i check` and round trip unchanged; `bin/reap` 49 of 52
  cells checked, 0 problems (recorded); `bin/crops-score --judge --strict` 0 (x86-64 assembly 99.3%, 99.9% without by design).
- Shared files touched (arm64's agent works beside): `bin/repr-asm` (one line, above), `arbace/asm/obj.ae` (the items' loop now `run-items!`, conditionals, macros, the
  line table, `.reloc`'s symbol and order hook, `line-entry!` in `data!` and `emit-insn!`, `switch!`'s clearing) and
  `arbace/repr/asm/gnu.ae` (`$` and an integer); arm64's object, object-verdict, run and a64i checks rerun, unchanged.
- **Merged with AR7** (branch `asm-ax11b`, from `mac/next` `f6ac54829`, which holds asm-ar7; 2026-11-11): `arbace/asm/obj.ae` holds
  both. AR7's error collection (ARS43 (a)) moved from `assemble`'s loop into `run-items!`: each item at depth 0 sets the place
  (`:pos`, [START END]), an expansion's items (read from its text) keep the invoking item's, and every item, the expanded ones too,
  reports its `:obj/error` and goes on (a macro body's two errors are two errors at the invocation, in order, the rest of the body
  assembled); a conditional whose condition is in error pushes its state first (parseDirectiveIf), so its `.endif` matches; the
  unmatched `.if` at the end is a reported error (no place) beside the others, not a throw; `assemble` throws the collected errors before
  the layout as AR7 does. AR7's `.org`, fixup places (`with-pos`), the symbol table's errors and AX11's `.reloc`, line table and
  `line-entry!` merged without overlap. `bin/repr-asm`: the one `${runner:-}` line. Both crops' checks rerun as the gate's steps run
  them, every record unchanged (doc/JOURNAL.md, the merge's entry).

### Proposed (AX11)

Numbering from AXS92.

- **AXS92 The line program.** (a) MCDwarfLineAddr's program (DW_LNE_set_address with its R_X86_64_64, the special opcodes, the
  sequences per section, end_sequence) written, held by the MC/X86 tests with `.loc` the object list's filter leaves out and by new
  synthetic objects recorded in the oracle tier by hand (llvm-mc 23.1.5); (b) as built: the header only, a line entry unsupported.
  *Recommended: (a)*, after the oracle tier records them.
- **AXS93 arm64's `.reloc` order.** (a) arm64 sets `:reloc-directives-merged` and moves `R_AARCH64_PATCHINST` after its offset's other
  relocations (the order llvm-mc's AArch64 writer gives `patchinst`), held by `mc-aarch64-obj`; (b) as built: after its own. For the arm64
  line (asm-ar7). *Recommended: (a)*, by the arm64 line.
- **AXS94 The object check's corpus widened.** The filter of `mc-x86-obj` leaves out `--defsym` and `split-file` tests, which conditional
  assembly and macros now could take: (a) `--defsym` taken (the assembler's `:defsym` option) and the objects recorded by hand in the
  oracle tier; (b) as built. *Recommended: (a)*.

## As built: AX12 (2026-11-11; branch `asm-ax12`, from `mac/next` `4ec952309`, which holds asm-ax11b and asm-ar8)

The user's decisions of 2026-11-11: AXS92 (a), the line program itself, and AXS94 (a), MC/X86's `--defsym` tests into the object
corpus; the new llvm-mc 23.1.5 objects recorded by hand at the oracle tier (the host's llvm-mc, for these records only; no gate step runs
it). Read from llvm-project 23.1.5's MCDwarf.cpp (MCDwarfLineEntry::make, MCDwarfLineTable::emitOne, MCDwarfLineAddr::encode),
MCObjectStreamer.cpp (emitInstruction, emitBytes, emitValueImpl, emitDwarfLocDirective, changeSection, emitDwarfAdvanceLineAddr,
emitDwarfLineEndEntry), MCStreamer.cpp (endSection, popSection), AsmParser.cpp (parseDirectiveLoc), MCAssembler.cpp (relaxDwarfLineAddr)
and X86ELFObjectWriter.cpp (checkIs32, checkIs64), Apache-2.0 WITH LLVM-exception, extracted from the pinned tarball; written again in
Arbace, no code copied.

- **The line program** (`arbace.asm.obj`: `loc!`, `line-entry!`, `line-addr-bytes`, `emit-debug-line!`). A `.loc` (its file checked,
  the line and column, `basic_block`, `prologue_end`, `epilogue_begin`, `is_stmt 0|1`, `isa N`, `discriminator N`; the flags start from
  the last one's is_stmt; LLVM's messages for a bad one) first gives a still unused `.loc` its entry at the current place (two in a row),
  then becomes the current one; the next instruction, `.byte`/`.short`/`.long`/`.quad` or `.ascii`/`.asciz` (emitInstruction,
  emitValueImpl, emitBytes) makes its line entry, a temporary label at that place, in the section's list; `.zero`, `.fill`, `.org` and
  the alignments do not (emitFill, emitValueToOffset); a section change clears it, `.popsection` too (changeSection). At the end, after
  `.eh_frame`, `.debug_line`'s header (AX11's) is followed by one sequence per section in the order of its first entry: each entry's
  changes from the state machine's (DW_LNS_set_file, set_column, DW_LNE_set_discriminator, set_isa, negate_stmt, set_basic_block,
  set_prologue_end, set_epilogue_begin), then the first row by DW_LNE_set_address (an 8-byte relocation to the label: R_X86_64_64
  against the section's symbol) with its line advance, the next by MCDwarfLineAddr's encoding of the line and address deltas from the
  final layout (a special opcode, DW_LNS_const_add_pc and one, DW_LNS_advance_line, DW_LNS_advance_pc, DW_LNS_copy), as LLVM's Dwarf
  fragments relax to, and DW_LNE_end_sequence at the section's end. The header's minimum instruction length is 1 for both arches
  (AX11's header took arm64's code alignment, 4: llvm-mc writes 1; no arm64 record had a `.file N`, so nothing moved).
- **The synthetic object** `test/asm/obj/debug-line.s` (four files, two with directories; every flag and option; two `.loc` in a row;
  a jump relaxed between two entries; advances of 0, 17 (const_add_pc), large (advance_pc), backwards and out of the special opcodes'
  range with no address advance (copy); entries in `.text`, `.text.g`, `.data` and a `.pushsection`; `.loc` before a section switch),
  llvm-mc's object byte for byte. `line-table-sections` (MC/X86) still passes. An arm64 version of the same shapes, assembled by hand
  against llvm-mc `-triple aarch64-linux-musl -mattr=+all`, is byte for byte too (not recorded: arm64's records are the arm64 line's).
- **`--defsym` (AXS94 (a))**: `mc-x86-obj`'s filter no longer leaves out a file whose first runs use `--defsym`
  (`intel-syntax-quote-reserved`: its x86-64 object run has none; its i386 run sets `I386`), and each object run of a kept file that
  sets symbols with `--defsym` for x86-64 is a case of its own, `NAME@SYM=V`, its options in the list's third field (`--defsym SYM=V`),
  assembled with the assembler's `:defsym`. MC/X86 has one: `pltoff`'s error run (`not llvm-mc ... --defsym ERR=1`), which llvm-mc
  refuses; the record holds the refusal (`:refused`, its diagnostics with the input as `<input>`, two runs equal) and the check holds
  Arbace's errors, printed as llvm-mc prints them (the diagnostics ARS43 (a) wrote for arm64's verdict, moved from
  `arbace.asm.arm64.objcheck` into `arbace.asm.obj`; an AT&T `$NAME`'s caret on the name), to it line for line: `:assembled` and
  `:error-diagnostics` fail. For it, x86-64's relocation types report X86ELFObjectWriter's errors (checkIs32: PLT, GOTPCREL, TLSGD,
  GOTTPOFF, TLSLD; checkIs64: PLTOFF; GOTOFF's "unsupported relocation type") instead of `:unsupported-relocation`, and an
  instruction's fixups and relaxable chunks carry their item's place. A new control `_control/pltoff-vs-err` (pltoff's object judged
  against the refusal) fails as it must. The other MC/X86 files that use `--defsym` have no object run (`displacement-overflow`,
  `intel-syntax-branch`, `intel-syntax-displacement-overflow`, `register-assignment`). A file llvm-mc refuses with no `--defsym` is
  still left out, as before (five: Mach-O's and COFF's directives).
- **Recorded by hand** (the oracle tier, the host's `/usr/lib/llvm23/bin/llvm-mc`, LLVM version 23.1.5): `bin/repr-asm object-record
  synthetic` (each file `llvm-mc -triple x86_64-linux-musl -filetype=obj FILE -o OUT`, twice, the same bytes; the seven earlier objects
  unchanged, `debug-line` new) and `bin/repr-asm object-record mc-x86-obj` (the same, with the case's `--defsym ERR=1` for `pltoff@ERR=1`,
  whose refusal and diagnostics are recorded; the 47 earlier objects unchanged, `intel-syntax-quote-reserved` new); the records'
  headers say so, their `.sha256` pins rewritten.
- **Results**: `object-check` musl 32 of 32, synthetic 7 -> 8 of 8, mc-x86-obj 47 -> 49 of 49 (each with its controls, six now);
  the AOT cell 105 / 0 / 0 -> 108 / 0 / 0. `:unsupported-debug-line` keeps no row (kept for DWARF 5's `.file 0` and a `.file` with an
  MD5 or source).
- Shared files touched (arm64's line works beside): `arbace/asm/obj.ae` (above; `data!` now `line-entry!` then `data-only!`, which
  `fill!` and `.org` take), `arbace/asm/arm64/objcheck.ae` (its diagnostics now `arbace.asm.obj`'s, the same function), `bin/repr-asm`
  (`mc-x86-obj`'s list, `object-record`'s options and refusals). arm64's object checks rerun unchanged.

### Proposed (AX12)

Numbering from AXS95.

- **AXS95 The left-out refusals.** Five MC/X86 object files llvm-mc refuses at an ELF triple (Mach-O's and COFF's directives) are left
  out of `mc-x86-obj`. (a) Record their refusals as `pltoff@ERR=1`'s and hold Arbace's diagnostics to them; (b) as built.
  *Recommended: (a)*, a light oracle-tier recording.
- **AXS96 MC/AsmParser's DWARF tests.** MC/AsmParser's `directive_loc*.s` and `directive_file*.s` are i386 or `-g` runs; the line program
  could also be held by their x86-64 objects recorded by hand: (a) a corpus `mc-asmparser-obj` of those that assemble for x86-64; (b) the
  synthetic file alone, as built. *Recommended: (b)* until the `-g` (MCGenDwarfInfo) work.

## As built: review 1111's fixes (2026-11-11; branch `review-fix-3`, from `mac/next` `a7916bb2a` with `mac/asm-ar9` `198c25291` merged)

The independent review of the assembly crops (`.tmp/reviews/1111/REVIEW-asm.md`, read whole) found nine things; the shared object
assembler's are fixed here (arm64's a64i findings 4, 5 and 9: doc/asm/ARM64-SURVEY.md, "As built: review 1111's fixes"). One commit a
finding. Sources read (llvm-project 23.1.5, the pinned tarball, extracted into the worktree's `.tmp/llvm`): `lib/MC/MCParser/AsmParser.cpp`
(parseDirectiveRept, instantiateMacroLikeBody, handleMacroEntry, printMacroInstantiations, DiagHandler, parseDirectiveLoc,
parseDirectiveReloc), `lib/MC/MCContext.cpp` (reportCommon), `lib/MC/MCAssembler.cpp` (layout's `.reloc` offsets),
`lib/MC/MCObjectStreamer.cpp` (emitRelocDirective), `lib/MC/MCDwarf.cpp` (tryGetFile), `lib/Support/Path.cpp`, `lib/Support/StringRef.cpp`,
`tools/llvm-mc/llvm-mc.cpp` (fillCommandLineSymbols). No code copied.

- **(1) An expansion's places.** A `.rept`, `.irp` or macro runs as one instantiation, AsmParser's `<instantiation>` buffer: a `.rept`'s or
  `.irp`'s copies concatenated (a body with no backslash too: it ran at depth 0 before, each item writing its body-relative offsets as the
  file's place), the body from the line after its head. An error inside is at `<instantiation>:LINE:COL` with a `note: while in macro
  instantiation` a level, innermost first; an error at the layout (a fixup's, a `.reloc`'s, a symbol's) has no note, ActiveMacros being
  empty then. A repetition counts toward the nesting limit, which is checked at a macro's entry (handleMacroEntry; a `.rept` itself is not
  checked). `read-body`'s offsets are the text's own (its macro declarations' length taken off). `.loc`'s file-number errors are at the
  number's token. Arbace's own error files, `test/asm/obj-err/` (x86-64) and `test/asm/obj-err-arm64/`, each `NAME.s` with `NAME.err`, the
  diagnostics derived by hand from LLVM's rules (not recorded: the user's llvm-mc approval covers AXS92's and AXS94's objects), are run
  with the synthetic corpus (`object-check synthetic`, `--arch arm64 object-check a64-synthetic`; `arbace.asm.obj` verb `diag-check`):
  `rept-place` (a layout error in each copy), `rept-notes` (a parse error in each copy, its note at the `.rept`), `reloc-place` (6).
- **(2) `--defsym` by keyword.** Both arches' `assemble-file` take `[PATH OPTIONS]` (`{:defsym}`; arm64's `{:implicit :defsym}`), the
  object check's call the same for both: arm64's second argument was IMPLICIT, a defsym there turned `-implicit-mapsyms` on and was lost.
- **(3) The refusal's control.** A control runs with its reference's corpus, its subject from any list with the list's options or from
  `test/asm/obj-err*/`, a reference there being expected diagnostics. New: `rept-notes-vs-err` (a refused file against llvm-mc's recorded
  refusal), `err-vs-rept-place`, `rept-notes-vs-rept-place`, arm64's `loc-zero-vs-rept-notes`. `control` answers `left` for a refused
  reference when the subject neither assembles nor is refused with errors. Shown by hand: a judge that calls any two refusals equal
  leaves `pltoff-vs-err` passing (vacuously) and makes `rept-notes-vs-err` fail.
- **(6) `.reloc`'s places.** Each directive keeps its place and section; its errors are reported at its offset expression and the next
  directive is evaluated. An absolute offset is the directive's section's (emitRelocDirective), where it was refused.
- **(7) `.file N "/x.c"`.** Split by sys::path's filename and parent_path (ported, POSIX style): `/x.c` is directory `/` and file `x.c`.
- **(8) One `--defsym` parser.** llvm-mc's radix sensing (`0x`, `0b`, `0o`, a leading `0`), a leading `-`, the whole text, int64's range,
  read inside the check's try (a value llvm-mc refuses: unsupported `:defsym` for the case). `010` is 8, `0b101` 5, `-0x10` -16.

Checks (stage 0, light, one at a time after 18:45): object-check musl 32/32, synthetic 8/8 with obj-err 3/3, mc-x86-obj 49/49, the nine
x86-64 object controls failing as they must; arm64 object-check a64-musl 13/13, a64-synthetic 3/3 with obj-err-arm64 2/2, mc-aarch64-obj
110/110, five controls; the other steps of `test/gate/crops/asm.edn` in the journal's entry. No record changed but a64i's (arm64's survey).

### Proposed (review 1111)

Numbering from AXS97.

- **AXS97 Tabs in the diagnostics.** SMDiagnostic::print expands a source line's tabs to tab stops of 8 (and the caret line with them);
  `arbace.asm.obj/diagnostics` prints the line as it is. FileCheck's default whitespace folding hides it in the error tests; a recorded
  refusal (AXS95) with a tab would not. (a) expand as printSourceLine does; (b) leave it until a record needs it. *Recommended: (a)*.
- **AXS98 `.file`'s compilation directory and repeats.** tryGetFile empties a directory equal to llvm-mc's compilation directory (its
  working directory) and refuses a file number seen before; Arbace keeps the directory and allows the same file again. (a) check both
  against llvm-mc's sources and the recorded objects (the oracle tier's, by hand); (b) as built. *Recommended: (a)*.


## As built: queue 315, x86i's per-page decode cache (2026-11-16; branch `315-asm-x86i-speed`, from `98cfb63c4`; the Mac VM)

a64i's decode cache of queue 303 (doc/asm/ARM64-SURVEY.md, "As built: queue 303") for x86i. The Mac VM is aarch64: x86i's CPU
rows and lockstep are the main host's; here x86i's interpreter ran in a probe build of the printed light driver for arm64 (the
lockstep, amd64's ptrace, stubbed), over every program of x86i-check, assembled and linked by Arbace.

- **The decoded instructions a page's array** (cpu.ae's `Inst`, `decodeAt`): `pages`, a `codePage` of 4,096 entries a code page by
  offset, each a copy of the instruction's bytes and its decoding; the last fetched page (`AS.ExecPage`'s bytes, its array) kept while
  `emu.AS.Gen` is unchanged; an entry used only when its bytes equal the page's now (compared at every fetch, `bytes.Equal` over
  its length), so a store into code is seen at the next fetch and nothing invalidates: x86i no longer marks code pages (emu's
  `PermCode`, `MarkCode`, `ClearCode` and `OnCode` are unused by both machines now; emu unchanged). The hit is Inst's, inline.
  Decoding reads the page's own bytes when 15 fit in it, else `AS.Fetch` across its end.
- **An instruction across a page's end** is in no page's array (a page's entry could only compare its own bytes): it is kept in
  `cross` by address, used when the bytes fetched now from both pages (the second page's permission checked by AS.Fetch) are its
  own. Rare (a fetch then costs AS.Fetch and a map lookup, as every fetch did before).
- **PendingSignal** returns at once when nothing is pending and unblocked: Step asks after every instruction, and its loop over 64
  signals was 40% of a fetch-bound loop's time (pprof) once the fetch was cheap.
- **Injected faults** (`X86I_INJECT=any:F`, any family): `w` an entry used whatever the bytes now, `g` the last page's bytes kept
  across Gen's changes, `x` an instruction across a page's end kept in its first page's array and compared there alone. The program
  `test/asm/x86i/smc.s`: code in an RWX page called, its immediate rewritten and called again; a loop rewriting an instruction it ran
  in the page it runs in; `mov $imm32, %eax` across two pages' boundary, its immediate rewritten on the second page, then on the
  first; a file mapped MAP_PRIVATE|MAP_FIXED over the page running the mmap (the instruction after the syscall the file's), then
  the page's other function called again. Controls `u315-inject-smc-w` (steps 2, 3, 5, 6, 9 run the old code), `-g` (step 8),
  `-x` (step 5), `u315-cpu-smc-vs-misc`: fail as they must (probed: judged by x86i_judge with the CPU's run stood in for by x86i's).
- **Results unchanged** (probed, base and head built alike): every one of x86i-check's 46 programs' stdout, stderr, status, coverage
  and decode-text equal; the coverage computed from the runs equal to `test/asm/x86i-coverage-amd64.txt` (smc executes no
  instruction not already executed). a64i's `check --record` (980 rows) and `goroot-check --record` (1,178) rewrite their files
  byte for byte, go-check's controls fail as they must.

| x86i run (arm64 probe, wall) | base 98cfb63c4 | the cache alone | head | |
|---|---|---|---|---|
| a fetch-bound loop, 72 M instructions | 4.24 s (17 M/s) | 3.35 s | 1.42 s (51 M/s) | 3.0x |
| alu.s (8.6 M) | 0.50 s | | 0.15 s | 3.3x |
| synth-ssefp (0.65 M, decode-heavy) | 61 ms | | 27 ms | 2.2x |
| x86i-check's 46 programs run, 3 passes | 5.0 s | | 2.6 s | 1.9x |

x86i-check's wall time on amd64 is mostly the CPU's runs, the eval's builds and the lockstep's ptrace, as a64i check's: x86i's own runs
are a part of it (the main host's measurement).

## As built: the deferred rows' cheap groups (2026-11-17; branch `h-asm-deferred`, from `next` `e4a7a30f3`)

The x86-64 cells' deferred rows grouped by cause (read 32, print 400 at `e4a7a30f3`), the cheap groups fixed, each proven on its
own files (no whole-corpus run), the rows re-recorded alone:

- **The printer's markup (AXS66 (a)).** `arbace.asm.amd64.print/*markup*` (MCInstPrinter's UseMarkup): registers `<reg:%rcx>`,
  immediates and pcrel immediates `<imm:...>`, a scale `<imm:4>`, a memory reference `<mem:...>` (printOperand, printPCRelImm,
  printMemReference, printOptionalSegReg); decode-check binds it for `--mdis` runs. `dis/marked-up` and `marked-up-32`: 4 of 4
  (read: `:markup` 4 to 0).
- **llvm-mc's lexer options (AXS35 (a), its alternative to (b)).** The header's map takes `:masm-integers` and
  `:motorola-integers`, set from the first llvm-mc's RUN options (`bin/repr-asm`'s list flags; `check.ae`'s runs, `:lexer`);
  `arbace.repr.asm.lex` ports LexDigit's LexMasmIntegers branch (radix suffixes h, t, o/q, y, a last d or b; `0x..h`) and the
  Motorola `$hex` / `%binary` integers. `motorola_integers`, `pr27884`, `intel-syntax-hex` read and printed back byte for byte
  (read and print: `:lexer-option` 3 to 0), intel-syntax-hex's 11 CHECK texts met (print: `:unread` 74 to 63).
- **llvm-mc's comments (narrowing `:comment`).** `inst-comment`: X86InstComments' EmitAnyX86InstComments for PALIGNR, PSHUFD,
  PUNPCK*, SHUFP*, UNPCK*P* and INSERTPS without a mask (X86ShuffleDecode's PALIGNR, PSHUF, UNPCKL/H, SHUFP and INSERTPS masks,
  printMasks), else printOperand's `imm = 0x...` (AT&T). The judge holds a comment-kind CHECK text to the printed lines with their
  comments, FileCheck's way; a miss beside an instruction the printer comments is `:mismatch`, else still `:comment`. shuffle-comments,
  imm-comments, vinsertps_decode: 119 of 119 (`:comment` 119 to 0). A mutant of shuffle-comments (`palignr $4`, `shufps $28`) fails
  the judge by hand; the new control `_control-print/vinsertps-vs-shuffle-comments` fails as it must.
- **--defsym (AXS32 (a)'s part).** A run's --defsym is no `:options` any more: the print level assembles the statements
  `.ifdef`/`.ifndef`/`.else`/`.endif` select for its names (assemble-file's `defs`; without --defsym nothing changes).
  intel-syntax-branch: 7 texts met, 2 labels left `:directive`; displacement-overflow's and intel-syntax-displacement-overflow's
  2 + 2 texts are warnings, now judged by the existing by-design `:diagnostic` rule (deferred 4 to design 4).
- `bin/repr-asm encode-check --record --only LIST` records the listed files' rows alone (encode_judge's partial record).

Proof: encode-check `--only` over 29 files (the touched ones and every control's subject and reference): 25,681 of 25,681 encodings,
26,179 of 26,199 texts, 14,589 of 14,713 cross, every control failing as it must, the judge ok against the record (13 minutes,
1.9 GB); the round trip of the three lexer files and of 25 musl x86_64 files, 31 arm64 files (a dialect without options unchanged).
After: read 25,229 pass / 1 design / 25 deferred; print 194,722 / 1,536 / 256.

Left deferred (not cheap): the error tests (24 read and print rows, one input; AX2's judge: the reader stops at the first error,
LLVM's messages and places per statement), the streamer's lines (`:directive` 104: x86_64-asm-match's 57 lines of
`-debug-only=asm-matcher`, `.cfi_*` with APX registers, labels, `.reloc`, assignments, data), the LFI rewriter (61, X86MCLFIRewriter:
no source in the corpora to port), the local label (1 + 1), a fixup (1).

### Proposed (the deferred rows)

- **AXS95 The objects behind `:other-tool`.** 655 of the print level's 772 by-design `:other-tool` texts are the llvm-objdump,
  llvm-readobj and -filetype=obj CHECK lines of files whose objects no Arbace check judges (mc-x86-obj leaves out the relaxation,
  padding and branch-alignment runs, -mcpu, CREL, other triples: x86_long_nop 40, align-via-relaxation 37, apx-*-reloc 228,
  align-branch-* 170, prefix-padding 32, ...); the other 117 are files object-check holds to llvm-mc's recorded objects. (a) those 655
  `:deferred` under a class of their own (`:object-unjudged`) until AXS67 (b)'s objdump text or object-check's corpus takes their runs;
  (b) keep all 772 by design (another tool's output is no printer's text). *Recommended: (a)*: the texts are the tests' own verdicts
  on objects nothing of Arbace's checks; by design hides a gap of the AOT level.
- **AXS96 The error tests' read rows.** (a) AX2's judge for the round trip: the reader recovers per statement as AsmParser does and
  each refusal is held to the test's error CHECK lines (place, then message); (b) a refused error test passes the read when the
  refusal's line is one the test states an error at (12 of 24 today); (c) keep `:error-test` deferred. *Recommended: (a)*, (b) being
  a judge looser than the test.
- **AXS97 `-debug-only=asm-matcher`.** x86_64-asm-match's 57 texts are LLVM's matcher's debug trace (REQUIRES: asserts), no
  streamer's line: (a) a by-design class `:debug-trace` (no printer writes it; the matcher's choice is judged by the encodings);
  (b) keep `:directive`. *Recommended: (a)*.

## As built: AXS95 (a), AXS96 (a), AXS97 (a) (2026-11-17; branch `h-asm-deferred`, from `next` `af41f0944`; the user's round 51)

- **AXS95 (a).** The print level's other-tool texts are `:other-tool` (by design) only for a file whose object object-check holds
  to llvm-mc's recorded one (its `mc-x86-obj/NAME` row passing in `test/asm/object-results-amd64.edn`, read by check.ae beside the
  table), else `:object-unjudged` (deferred). **Correction to the proposal's numbers:** 406 of the 772 texts are unjudged, not 655
  (the proposal's count missed that `apx/*-reloc.s` are object-check's `add-reloc` and the like, by base name); 366 stay by design.
- **AXS97 (a).** A run with `-debug-only` prints LLVM's debug trace: `:debug-trace`, by design (x86_64-asm-match's 57).
- **AXS96 (a).** An error test's read (the list's `reject` flag) goes on after a refused statement (`gnu/*recover*`: the
  statement kept as `(asm/unread TEXT MESSAGE)`, as inside a conditional block, ARS49), so the file reads, round-trips and prints
  back byte for byte; each refusal outside a conditional block is then held to the lines the test states errors at
  (`arbace.repr.asm/error-lines`): a `[[#@LINE+N]]` place, a `NAME.s:N:C:` place, else the statement just before and just after the
  CHECK line (labels between kept); a CHECK line counts when it holds `error:`, its prefix names ERR, or every llvm-mc run of the
  test is `not llvm-mc`; a split-file part is named by its test (`split=`, `part=` in the list's flags) and its lines counted
  from its `#---` line. A refusal elsewhere is `:error-misplaced`, deferred: a test need not state every error llvm-mc reports,
  so llvm-mc may refuse it too (the oracle tier's, by hand). The message is not judged (the reader's words are not LLVM's).
  amd64: the 24 error tests pass (66 refusals at their tests' errors); arm64, the same judge: 6 of its 8 pass, 2 `:error-misplaced`
  (basic-a64-diagnostics' `;; Exponent too large`, a statement in AArch64's ELF syntax; neon-diagnostics' `ld1 v0.8b, v1.8b}`).
  Controls `test/asm/controls-error.txt` (an error test's refusals against another's error lines; `bin/repr-asm roundtrip`, amd64):
  4, each fails as it must by hand, and a test against itself passes. The one `:error-input` (an Inputs/ file with no RUN line) stays.
- Proof: encode-check `--record --only` over the 64 files with other-tool or debug texts (the judge ok, every listed control left);
  the round trip of the 24 + 8 error tests; the earlier musl, arm64 and lexer samples unchanged.

After: x86-64 read 25,253 pass / 1 design / 1 deferred, print 194,746 / 1,187 / 581; arm64 read 6,654 / 0 / 2, print 12,664 / 5 / 2.
