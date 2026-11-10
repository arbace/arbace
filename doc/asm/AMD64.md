# amd64: the table, the matcher and the encoder (AX2)

The machine half of the x86-64 crop (doc/asm/AMD64-SURVEY.md §7.2, §8.2; the forms are doc/asm/SPEC.md's). AX2's first slice:
the instruction table generated from LLVM's TableGen files by Arbace's own TableGen, the AT&T matcher and the encoder of the
legacy, VEX and XOP encodings (EVEX since its second slice), the check of `MC/X86`'s stated encodings by their own CHECK lines, and since
its third slice the printer (§5) and the CHECK texts' print level. No LLVM tool runs in any of it; llvm-tblgen and llvm-mc are the
oracle tier's, by hand.

## 1. The table (`asm/amd64/table.edn`)

Generated, committed, checked by regeneration (`bin/repr-asm table gen|check`; the Makefile's `repr-asm-table`,
`repr-asm-table-check`), like repr/ocaml's and repr/haskell's tables.

- **Source.** llvm-project 23.1.5's `llvm/lib/Target/X86/X86.td` and what it includes (`llvm/include/llvm/{Target,CodeGen,IR}`:
  144,000 lines in 106 files), from the release tarball pinned in `test/asm/corpora.edn` (`:tables`), extracted with the corpora
  by `bin/repr-asm corpus extract`.
- **TableGen** (`arbace.asm.tablegen`): the language read and evaluated after `TGLexer.cpp`, `TGParser.cpp`, `Record.cpp` and
  ProgRef: the preprocessor's `#ifdef`, classes with template arguments (positional and named; the let stack around a class's
  definition applied after its superclasses, ParseObjectBody's ApplyLetStack, since AX2's fourth slice), defs, multiclasses and
  `defm` (several multiclasses in one defm, the defm's classes after them), `let` (fields and bit ranges), `foreach`,
  `defvar`, `defset`, `if`, values (bits, lists, dags, `!` operators, `!cond`, anonymous records `C<...>`), names pasted with
  `#`. A record's fields are made as TGParser makes them (superclasses in order, a later one's field overriding; the body;
  the let stack; for a defm's records its classes and the let stack around it, level by level outwards) and evaluated
  lazily against the finished record (TableGen's late resolution). The generator keeps only the fields it reads of the
  instructions and aliases (evaluated when each record is complete) and drops the fields of the record kinds it never reads
  (intrinsics, patterns, scheduling): about 2 minutes, 1.2 GiB.
- **The matchables** (`arbace.asm.amd64.gen`, after `AsmMatcherEmitter.cpp`): each instruction and `InstAlias` of the AT&T
  variant (not `isCodeGenOnly`, no `#` in its string, no operand written twice), its string flattened
  (`FlattenAsmStringVariants`: the outer `{a|b}` only) and tokenized with the variant's characters; the operand classes
  (register sets named as LLVM names them, by CodeGenRegBank's order: the size info, the larger set, the name; singleton
  registers; the `AsmOperandClass` records with their predicate and render methods; tokens); the order of
  `shouldBeMatchedBefore` (mnemonic, operand count, the classes' partial order, the record position for `HasPositionOrder`,
  the number of required features) by a stable merge sort; the conversion of each matchable into the machine operands (an
  asm operand rendered, a tied operand copied, an alias's fixed register or immediate). The `MnemonicAlias` records
  (`applyMnemonicAliases`), the assembler predicates, the registers (AsmName, HWEncoding) and each instruction's encoding
  fields (`X86Inst`: opcode, format, map, prefix, operand and address size, encoding, immediate, the VEX/EVEX bits,
  `CD8_Scale`, the operands and ties) of the matchables' instructions and of those processInstruction rewrites them into
  (optimizeShiftRotateWithImmediateOne's shifts by 1). Since the third slice the printer's fields too (after `AsmWriterInst.cpp` and
  `AsmWriterEmitter.cpp`): each instruction's `:print`, its AT&T AsmString (variant 0) parsed into literal fragments and
  `[MI-OPERAND PRINT-METHOD]` for each `$operand` (the Operand's or RegisterOperand's `PrintMethod`, else `printOperand`), and
  `:print-aliases`, printAliasInstr's patterns (InstAliases of `EmitPriority` 1 or more with no more operands than their
  instruction, their conditions on the MCInst's operands, the ambiguous ones dropped): X86 has three, `aad`, `aam`, `xstorerng`.
- **Its oracle** (`bin/repr-asm table oracle`, by hand): llvm-tblgen 23.1.5's `-gen-asm-matcher` on the same files; its
  `MatchTable0` and the table's matchables are the same 23,014 rows in the same order with the same classes (MatchTable1 and
  `:match-intel` 22,609). Since AXS37 (`test/asm/table_oracle.ae`) also each row's conversion (ConversionTable's steps by
  convertToMCInst's cases: the asm operand, its render method and machine-operand count, TiedAsmOperandTable's result operand,
  the immediates and registers), every `:insns` entry's encoding fields, operands (MIOperandInfo's counts), defs and ties
  against `-print-records`' resolved fields, and every `:print` and `:print-intel` against `-gen-asm-writer`'s
  printInstruction decoded (AsmStrs, the OpInfo words, each fragment's command): 20,477 entries, equal.
- **The conversion** (since AXS37) is buildInstructionResultOperands': an operand of one machine operand tied to an earlier one
  copies it (`[:t EARLIER]`); else the first asm operand whose name, canonicalized to the untied operand (`$src1` tied to
  `$dst` is `$dst`, buildInstructionOperandReference), is the operand's renders it (`[:a K]`); else one immediate 0.

## 2. The matcher (`arbace.asm.amd64.asm`)

An AX1 form's operands as `X86Operand`s (registers by AsmName, `%dbN` as `%drN`, the 64-bit-only registers refused outside
64-bit mode; immediates and displacements folded when constant, `.set`/`=` constants inlined as LLVM's parser does; memory
with mode and size 0, AT&T's), then `parseInstruction`'s rewrites (`{disp8}`, `{disp32}`, `{vex3}`, `.d8`/`.d32`, `lock`/`rep`/
`notrack` as flags, the prefix words cs, ds, rex64, data16, xacquire... as instructions of their own, `data32` in 16-bit mode,
`setXXb`, the comparison predicates of `cmpXXps`/`vcmpXX*`/`vpcmpXX*`/`vpcomXX*`, the string instructions' default operands,
`(%dx)`, `mov` of a segment register, x87's `fsub`/`fdiv` alone, `finit`'s FWAIT), `MatchInstructionImpl` (the mnemonic
aliases, the entries of the mnemonic in order, each operand's class, the assembler predicates for the mode, X86's
`checkTargetMatchPredicate`), `matchAndEmitATTInstruction`'s suffix retry and its messages, `processInstruction` (VEX3 to
VEX2, `int $3`, shifts by 1). `.code16gcc` matches as 32-bit.

## 3. The encoder

`X86MCCodeEmitter::encodeInstruction` with LLVM's choices (AXS5 (a): the mode `:llvm`): the segment, `rep`, address-size,
operand-size, `lock` and mandatory prefixes, REX (W, R, X, B; `%spl`-`%dil`), VEX2 or VEX3 (`determineOptimalKind`) and
XOP, the maps, every legacy and VEX format, ModRM, SIB and the displacement's choices (none, disp8, disp32; 16-bit
addressing; RIP-relative), the immediates, `Imm8Reg`, and fixups (a symbolic value's bytes shown as llvm-mc shows them,
`A`, `B`, ...). Since AX2's second slice EVEX too (AVX-512, AVX10, AVX512-FP16's maps 5 and 6, map 7): `emitVEXOpcodePrefix`'s
EVEX operands form by form (R and R' of ModRM.reg, B and X of ModRM.rm, X of a memory index and V' of a VSIB index, vvvv and
V', aaa from the opmask), the instruction's W, z, L'L, b and U, the embedded rounding (`{rn-sae}`...`{rz-sae}` an immediate
rendered into L'L with EVEX.b, `{sae}` a token: L'L zero), the broadcast's `{1toN}` (its own instruction, EVEX.b), and the
displacement compressed by `CD8_Scale` (`isDispOrCDisp8`: disp8*N when aligned and in range, else disp32); the matcher takes
`%xmm16`-`%xmm31`, `%ymm16`-`%ymm31`, `%zmm`, `%k0`-`%k7` (the 64-bit-only ones refused outside 64-bit mode, as
`MatchRegisterByName` does), the opmask and `{z}` decorations, the mask pairs of `vp2intersect` (`addMaskPairOperands`) and
the `{evex}` pseudo prefix (`checkTargetMatchPredicate`). Since AX2's fourth slice APX: the extended GPRs `%r16`-`%r31` at each width
(UseApxExtendedReg: an instruction takes them when canUseApxExtendedReg says so, EVEX or the legacy maps OB and TB but XSAVE*/XRSTOR*),
REX2 (`D5`, its M R4 X4 B4 W R X B; chosen when a register's bit 4 is set, for the ExplicitREX2 instructions JMPABS, PUSHP and POPP, or
by `{rex2}`; its M bit the 0F escape), EVEX map 4 with APX's fields (B4 of a GPR rm or base, not inverted; X4 of an index in EVEX.U; the
new data destination in vvvv, hasNewDataDest, skipped by the forms' operand walk; NF in aaa's bit 2; CCMP's and CTEST's `{dfv=...}` in
vvvv and their condition in V' and aaa), `{nf}` (checkTargetMatchPredicate's ForcedNoFlag, CFCMOV excepted), `{evex} cmp`/`test` as
`ccmpt`/`ctest {dfv=}` (replaceWithCCMPCTEST), the shift by 1 of an NDD form (ROL8r1_ND, DisassembleOnly, in the table for it),
validateInstruction's high byte registers with EVEX, REX2 or REX (`can't encode 'ah' ...`), and VEX's maps 5 and 7 (AMX-FP8, USERMSR,
MSR_IMM). Not yet: relaxation (llvm-mc's `-show-encoding` prints the unrelaxed instruction, so the stated encodings need
none; objects do, AX4), gas's mode (`:gnu`, AXS5).

## 4. The check (`arbace.asm.amd64.check`, `bin/repr-asm encode-check`)

Each `MC/X86` file's RUN lines give its runs (mode from the triple, the FileCheck prefixes, `-show-encoding`, `not llvm-mc`);
its statements are read by AX1's reader and assembled in each run's mode (`.code16`, `.code32`, `.code64`, `.code16gcc`
followed); each stated encoding (`<PREFIX>: ... encoding: [...]`) is matched as FileCheck matches it in llvm-mc's output: the
first instruction, from the last one matched on, whose `encoding: [...]` the pattern matches (`{{regex}}`, blanks collapsed).
An unmet one is classed (`test/asm/divergences-encode.edn`). An error test's `error:` lines are matched in order against
the assembler's own messages. Rows by file in `test/asm/encode-results-amd64.edn`; negative controls in
`test/asm/controls-encode.txt` (a test's statements judged against another test's CHECK lines must fail).

Its print level (AX2's third slice): each run's statements are printed (§5), and each CHECK line's text (its part before an
`encoding:`, a trailing `#` dropped) of a run that is not an error test is held to the printed lines as FileCheck holds it to
llvm-mc's output: forward from the last match, the rest of the matched line first (`-SAME` only there), blanks collapsed, the
encoding's comment beside the text when the run shows encodings. A text of a line the printer does not make (a directive, a label,
an assignment, a fixup's or a verbose comment, an MCInst dump, a diagnostic: by its content, or its first word no instruction's
mnemonic) is classed by its kind; the runs whose output is not this printer's (Intel's output variant, another tool reading an
object, the LFI rewriter, options that change the instructions) by the run. The rows' `:text` in `test/asm/encode-results-amd64.edn`,
the classes `test/asm/divergences-print.edn`, the negative controls `test/asm/controls-print.txt` (other instructions, another mode,
and a test's own statements printed with immediates in hex). `--record` only from a run with no failure at either level.

## 5. The printer (`arbace.asm.amd64.print`)

`X86ATTInstPrinter::printInst` over the matcher's MCInst (the table's instruction, its operands `{:reg}`, `{:imm}`, `{:expr}` after
processInstruction, the statement's flags): `printInstFlags` (lock, notrack, rep and repne, `{nf}`, `{vex}` `{vex2}` `{vex3}`
`{evex}` from the statement or the instruction's explicit prefix, `{disp8}` `{disp32}`), `callq` for CALLpcrel32 in 64-bit mode,
`data32` for DATA16_PREFIX in 16-bit mode, then printAliasInstr, `printVecCompareInstr` (`cmp`/`vcmp`/`vpcmp`/`vpcom` with the
predicate in the mnemonic, `{1toN}` and `{sae}`, the mask), else the instruction's `:print` fragments with the print methods
(`printOperand`, `printMemReference` and the `print*mem` methods, `printSrcIdx*`, `printDstIdx*`, `printMemOffs*`, `printU8Imm`,
`printSTiRegOperand`, `printCondCode`, `printCondFlags`, `printRoundingControl`, `printPCRelImm`, `printVKPair`). Immediates as
`formatImm` writes them (decimal, `--print-imm-hex` hex); expressions as `MCExpr::print` (only `+`/`-` group without parentheses,
`X-42` for an addition of a negative constant, symbols quoted when needed, a specifier after the symbol, `tlscall`/`tlsdesc` in lower
case, a specifier after an expression put on its symbols as `applyModifierToExpr` does; a negative literal is the text's unary minus,
and a constant subexpression that needed parentheses where it stands is folded as LLVM's parser folds a parenthesized one).
`printExprOperand`'s parentheses around an expression starting with `$`. Not printed yet (classed): a directional local label (`1f`:
llvm-mc's `.Ltmp` name) and the location counter.

By hand (`bin/repr-asm print-oracle`, the oracle tier): every AT&T file's printed lines against the host's llvm-mc 23.1.5 run with
the file's triple, file by file.

## 6. Intel syntax (AX3)

The table's Intel variant (§1): `:match-intel` (MatchTable1, llvm-tblgen's row for row by hand), `:aliases-intel`,
`:print-aliases-intel` and each instruction's `:print-intel`, made by the same generator with variant 1's flattening, the empty
register prefix (an isolated token naming a register is a singleton class) and `;` as the comment delimiter.

The matcher (`arbace.asm.amd64.asm`, `*intel*` true for a statement read in Intel, `*variant*` the table): the operands in the text's
order; parseIntelOperand's address swaps (an index with RIP drops the base; without a written scale ESP/RSP as index and a vector register
as base swap; SI/DI as base with BX/BP as index swap; a scale with a 16-bit index refused); the value as the expression state machine
computes it (one symbol plus the rest folded, `intel-value`; two symbols refused); a bracketed or sized `jmp`/`call` target not a direct
branch (`isAbsMem`); matchAndEmitIntelInstruction (`call`, `jmp`, `push`, `pop` with an unsized memory operand at the pointer's width; a
push of a constant that fits matched in AT&T's table with the mode's suffix; an unsized memory operand tried at 8 ... 512 bits, each new
instruction a match; one match wins, more is `ambiguous operand size for instruction`); the comparison predicate taken from the mnemonic
appended last; the string instructions' defaults in Intel's order; `xlat mem8` as `xlatb`.

The printer (`arbace.asm.amd64.print`, `*intel*` true for `-output-asm-variant=1`): X86IntelInstPrinter's printOperand (no `%` or `$`,
an expression after `offset`), printMemReference (`seg:[base + scale*index + disp]`, ` - N` after a register), the `print*mem` size words,
printSrcIdx, printDstIdx (`es:[...]`), printMemOffs, printU8Imm, printSTiRegOperand, printVecCompareInstr's Intel layout (destination and
mask first, the memory size its encoding implies), printAliasInstr from `:print-aliases-intel`, and Intel output's quoting of register
names and keywords (populateReservedIdentifiers).

The check (§4): each run reads the file in its own syntax (`-x86-asm-syntax=intel` in the run's own command), `.intel_syntax` and
`.att_syntax` switch it statement by statement, and the CHECK texts are held to the printer of the run's output variant (the last llvm-mc
of the pipe). The cross-syntax judge (`cross-judge`): every matched instruction of the file's first AT&T run printed in Intel, read back
and matched in the statement's mode with the file's constant symbols, the same instruction and operands (quoted and bare names one);
the rows' `:cross`, the classes `test/asm/divergences-cross.edn`, the controls `test/asm/controls-cross.txt` (`control-cross`).

## 7. Objects (AX4, first slice)

`arbace.asm.amd64.obj` assembles a file's forms into an ELF64 relocatable object as `llvm-mc -filetype=obj` writes it, and
`arbace.asm.elf` writes the object and reads any ELF64 object back (doc/asm/AMD64-SURVEY.md §7.3; "As built: AX4 (first slice)").

- **The streamer** (MCObjectStreamer, MCELFStreamer): sections by name and group, made by `.text`, `.data`, `.bss`, `.section`
  (ELFAsmParser's defaults by name, the flags `awxMSTGoeR`, the types, the merge size, the linked-to symbol of `o`, the comdat group),
  `.pushsection`/`.popsection`/`.previous`; each section a list of chunks as MC's fragments are: data (bytes and fixups), a relaxable
  instruction, an alignment, a fill (`.zero`, `.skip`, `.space`, `.fill`, `.lcomm`'s zeros), NOPs (`.nops`); `.text` made first and aligned
  to 4 by an empty code alignment (initSections). Labels (local `1:` labels and `.` as temporary symbols), `.globl`, `.weak`, `.local`,
  `.hidden`, `.protected`, `.internal`, `.type` (CombineSymbolTypes), `.size`, `.comm`, `.lcomm`, assignments (`=`, `.set`, `.equ`: a
  variable, its value followed), `.byte` ... `.quad`, `.ascii`, `.asciz`, `.string`, `.align` (bytes on ELF), `.balign`, `.p2align` and
  their `w`/`l` forms (code alignment with NOPs in an executable section without a fill), `.even`, `.file` (STT_FILE), `.ident`
  (`.comment`), `.code16/32/64`. A symbol is registered in MC's order (its first label, attribute or use).
- **The instructions**: arbace.asm.amd64.asm's encoder, whose fixups carry X86MCCodeEmitter's kinds (FK_Data_N, reloc_signed_4byte and
  `_relax`, the RIP-relative kinds with the GOTPCRELX choices by opcode, reloc_branch_4byte_pcrel for `call`/`jmp`/`jcc` to a symbol's
  reference; since the asm branches' merge the prefix's kinds too: `_rex` under REX, `_rex2` under REX2, `_relax_evex` for ADD64's
  NDD and NF memory forms, and TLSCALL's FK_NONE at the instruction's start with no displacement) and the RIP-relative bias of a
  following immediate. An instruction that may need relaxation (JCC_1, JMP_1, the short
  immediate forms of X86EncodingOptimizationForImmediate.def with a symbolic immediate) is a chunk of its own; the rest is data.
- **The layout** (MCAssembler::layout, relaxOnce): the sections laid out, then relaxed to a fixed point, each pass forward with the
  stretch of a later target; a branch relaxed when its target is not resolved (a global or undefined symbol, another section) or out of
  a signed byte, or has a specifier; JCC_2/JMP_2 in 16-bit code. The relaxed branches' encodings (codegen-only in the table) are given
  after X86InstrControl.td.
- **The fixups** (evaluateFixup, X86AsmBackend, X86ELFObjectWriter, ELFObjectWriter::recordRelocation): the PC-relative bias (-1, -2,
  -4), resolved within a section for a local symbol, else a relocation: its type by kind, size and specifier (PC32, PLT32, 32, 32S, 64,
  PC64, 16, 8, GOTPCREL, GOTPCRELX, REX_GOTPCRELX, CODE_4_GOTPCRELX, GOT32/64, GOTPC32/64, GOTOFF64, TPOFF32/64, DTPOFF32/64, SIZE32/64, TLSGD, TLSLD,
  GOTTPOFF, CODE_4_GOTTPOFF, CODE_6_GOTTPOFF, TLSDESC, CODE_4_GOTPC32_TLSDESC, TLSDESC_CALL, PLTOFF64), against the section symbol for a defined local unless the specifier needs the symbol, a
  mergeable section's nonzero addend or a TLS section forbid it; a difference with its subtrahend in the fixup's section PC-relative.
- **CFI** (MCDwarf's FrameEmitterImpl, emitted after the layout): `.cfi_startproc [simple]`, `.cfi_endproc`, `def_cfa`,
  `def_cfa_offset`, `adjust_cfa_offset`, `def_cfa_register`, `offset`, `rel_offset`, `val_offset`, `restore`, `undefined`,
  `same_value`, `register`, `remember_state`, `restore_state`, `escape`, `gnu_args_size`, `signal_frame`, `return_column`,
  `personality`, `lsda` into `.eh_frame` (SHT_X86_64_UNWIND): the frames sorted by their CIE's key, a CIE for each (augmentation `zPLRS`,
  x86-64's initial frame state), an FDE for each (PC-relative `sdata4`), the advances from the final offsets.
- **The file** (`arbace.asm.elf`, ELFWriter): `.strtab` first (StringTableBuilder's tail-merged table, sorted by reversed names),
  each section's data aligned with its `.rela` after it in the table, the groups' sections, `.symtab` (STT_FILE, then the locals, then
  the rest in registration order; MC's membership: a symbol used by a relocation, else not temporary nor a section's), the
  relocations, the string table, the section headers.
- **The reader** (`read-object`): any ELF64 little-endian object's header, sections and bytes, symbols and relocations; `canonical`,
  the judge's view (the sections with contents, the symbols, the relocations, by name).
- **The check** (`bin/repr-asm object-check`): each file of `musl`, `synthetic` (`test/asm/obj/*.s`, Arbace's own) and `mc-x86-obj`
  (MC/X86's object tests for x86-64 ELF) assembled and held to llvm-mc 23.1.5's object recorded in `test/asm/records/obj-CORPUS-llvm-mc-
  23.1.5.edn` (made by hand, `object-record`, each object twice; pinned by its `.sha256`, checked first): byte for byte, else its sections,
  symbols and relocations compared (`:canonical` when only the layout differs); rows `test/asm/object-results-amd64.edn`, classes
  `test/asm/divergences-object.edn`, controls `test/asm/controls-object.txt` (a file's object against another's record).

Not yet: `.debug_line` (`.loc`), `.reloc`, macros and conditional assembly (AXS32), APX (the encoder's), `.symver`, `.weakref`,
COMDAT groups beyond the one tested, x32 and i386 objects, the decoder (AX4's next slice; the linker is §8).

## 8. The static linker and the runs (AX4, second slice)

`arbace.asm.link` (`arbace/asm/link.ae`) links ELF64 relocatable objects into a static executable (`ET_EXEC`) for x86-64 Linux as lld
lays one out with no linker script (llvm-project 23.1.5's `lld/ELF`, read and written again; doc/asm/AMD64-SURVEY.md §7.3, "As built:
AX4 (second slice)").

- **Inputs**: ELF64 relocatable objects (an archive or a shared object is classed: `:archive`, `:shared`). The object's own tables, its
  groups, `.note.GNU-stack` and SHF_EXCLUDE sections never reach the output.
- **COMDAT** (InputFiles): the first group of a signature kept, a later one's members discarded; a symbol defined in a discarded section
  is an undefined reference of its binding; a relocation from a live section to a local symbol of a discarded one is an error.
- **Symbols** (Symbol::resolve): the global table in the order names are first seen; defined over common over undefined, strong over
  weak, two strong definitions `duplicate symbol: NAME`, the larger common (the stricter alignment) kept, the most constraining
  visibility of all references; an undefined strong symbol a relocation needs is `undefined symbol: NAME`, an undefined weak one 0.
  Commons are allocated into `.bss` (COMMON) in the table's order. The symbols lld defines when referenced: `__ehdr_start`,
  `__executable_start`, `_etext`/`etext`, `_edata`/`edata`, `_end`/`end`, `__bss_start`, `_GLOBAL_OFFSET_TABLE_` (`.got.plt`'s start),
  `__start_SEC`/`__stop_SEC` of a section named as a C identifier, `__{preinit,init,fini}_array_{start,end}`; hidden.
- **Output sections** (getOutputSectionName, finalizeInputSections, getSectionRank): `.text.*` into `.text` and GNU's other prefixes
  likewise, created in input order, then sorted stably by lld's rank (R, RX, RW TLS, RW RELRO, RW, NOBITS after PROGBITS, non-alloc
  last); the flags ORed, the alignment the largest; a mergeable section (SHF_MERGE) split into pieces (strings at their terminators, else
  by entry size) and deduplicated into one synthetic section at its first input's place; `.eh_frame` parsed into CIEs and FDEs, the CIEs
  deduplicated by contents and personality, an FDE whose function is not live dropped, each FDE's CIE pointer rewritten (one CIE for a
  program's MC objects); `.got` (an entry for each symbol a GOT relocation keeps: its address, no dynamic relocation), `.got.plt` (three
  header words) when GOTOFF/GOTPC or `_GLOBAL_OFFSET_TABLE_` need it, `.relro_padding` after a RELRO section, `.comment` with the
  linker's line, then `.symtab`, `.shstrtab`, `.strtab`. An executable section's padding is `int3` (0xcc).
- **Program headers** (createPhdrs): PT_PHDR; the PT_LOADs by permission (the first holds the ELF header and the program headers with
  the read-only sections; a RELRO run ends a PT_LOAD; a PROGBITS section after a NOBITS one starts one); PT_TLS; PT_GNU_RELRO;
  PT_GNU_STACK (RW). An empty PT_LOAD is removed after the layout (removeEmptyPTLoad).
- **Addresses and offsets** (assignAddresses, fixSectionAlignments, assignFileOffsets): from 0x200000 past the headers; a PT_LOAD's first
  section on the next 4 KiB page at the same page offset (the TLS one aligned to PT_TLS's alignment), each section aligned; `.tbss` takes
  no address space; `.relro_padding` reaches the next common page; a PT_LOAD's first section's file offset congruent to its address, the
  rest at their distance from it, a NOBITS section at the current offset; the section headers last, aligned to 8.
- **Relocations** (X86_64::relocate, RelocScan::process): `R_X86_64_64`, `32` (unsigned), `32S`, `16`, `8`, `PC64`, `PC32`, `PC16`,
  `PC8`, `PLT32` (direct: nothing is preemptible), `GOTPCREL`, `GOTPCRELX` and `REX_GOTPCRELX` relaxed as adjustGotPcExpr and relaxGot
  relax them (an addend of -4 and not an absolute, undefined weak or TLS symbol: `mov` into `lea`, `call *` into `addr32 call`, `jmp *`
  into `jmp; nop`, `test` and the binary operations of REX_GOTPCRELX into immediates; the rest through a `.got` entry), `GOT32`,
  `GOT64`, `GOTOFF64`, `GOTPC32`, `GOTPC64`, `PLTOFF64`, `SIZE32`, `SIZE64`, `TPOFF32`, `TPOFF64`, `DTPOFF32`/`64` (TP-relative in an
  executable), `GOTTPOFF` relaxed to local exec (relaxTlsIeToLe); a TLS symbol's address is its offset in PT_TLS and the thread pointer's
  offset getTlsTpOffset's (variant II). Range checks give lld's `relocation R_X86_64_... out of range`. TLS GD/LD/TLSDESC are classed
  (`:tls-gd-ld`), and so are relocations inside a mergeable section.
- **The symbol table** (demoteAndCopyLocalSymbols, sortSymTabSymbols): each file's locals but section symbols and `.L` symbols of
  mergeable sections, then the globals in the table's order with their final binding (a hidden or internal one local, grouped with its
  file's locals); `.strtab` in the order of addition, `.shstrtab` deduplicated.
- **Entry**: `_start` (else lld's warning and 0).

**The run check** (`bin/repr-asm run-check [--record]`): each test of `test/asm/run/` (Arbace's own freestanding programs: syscalls,
loops, calls, data and `.bss`, commons, relocations across objects, GOT relaxation, weak symbols, COMDAT, merged strings, `.eh_frame`,
TLS with `arch_prctl`, linker-defined symbols, musl 1.2.6's `crti`/`crtn`, `memcpy`/`memmove`/`memset` and `setjmp`/`longjmp` assembled
from its tarball) states its verdict in its header (`# exit:`, `# stdout:`, `# with:` and `# before:` the other sources, `# link-error:`
a refusal in lld's words); the sources are assembled (`arbace.asm.amd64.obj`) and linked by Arbace in one process, each program run by
the CPU under a time limit, and judged by its own verdict: no assembler, linker or compiler of the host at check time. Rows
`test/asm/run-results-amd64.edn`, classes `test/asm/divergences-run.edn`, controls `test/asm/controls-run.txt` (a program's run judged
by another test's verdict). `bin/repr-asm link OBJ... -o OUT` links by hand, `link-dump EXE` reads an executable back canonically,
`link-oracle` is the oracle tier's (the host's ld.lld when present, else GNU ld, on the same objects: runs and canonical views compared,
by hand only).

Not yet: archives (musl's `libc.a` needs its C compiled: AX7), shared objects and dynamic linking (AXS11), static PIE, TLS GD/LD/TLSDESC
relaxation, `--gc-sections`, ICF, linker scripts, the merge pieces' order by lld's hash shards (Arbace's: first seen).

## 9. The decoder (AX4, third slice)

`arbace.asm.amd64.decode` (`arbace/asm/amd64/decode.ae`) reads machine code back into the machine instructions the encoder encodes:
the table's instruction and its MCInst operands (`{:reg R}`, `{:imm V}`; a memory operand's five), with the flags the printer reads,
as LLVM's X86 disassembler decodes them (llvm-project 23.1.5's `lib/Target/X86/Disassembler/X86Disassembler.cpp`, read from the
pinned tarball and written again in Arbace; doc/asm/AMD64-SURVEY.md, "As built: AX4 (third slice)"). No LLVM tool runs in it.

- **The tables** (TableGen's `-gen-disassembler`, made here from `asm/amd64/table.edn` at their first use, lazily): the table holds
  every instruction the disassembler emits (X86RecognizableInstr's shouldBeEmitted: not Pseudo's form, not codegen-only unless
  ForceDisassemble, not asm-parser-only; `:dis` 0, 32 or 64 for its mode predicate, In64BitMode or Not64BitMode/In32BitMode). Each
  one's instruction context (`insnContext`), ModRM filter (`emitDecodePath`'s Dumb, Mod, Extended, ExtendedRM and Exact filters),
  opcodes (eight for AddRegFrm, sixteen for the condition-code forms) and operand specifiers (`emitInstructionSpecifier`: each
  operand's encoding and type, a tied operand a DUP of its source, the compressed displacement's scale); a decision, the 256
  ModRM bytes' instructions of an opcode type, a context and an opcode, is filled as `setTableFields` fills it, the instructions in
  the enum's order (pseudos first, then by name), a context taking an instruction when it inherits from the instruction's
  (`inheritsFrom` with noPrefix, VEX_LIG, WIG and AdSize64, LLVM's defaults in its recursive calls kept), a later instruction
  replacing an earlier one unless the earlier's context outranks it (the contexts' ranks of X86DisassemblerDecoderCommon.h), NOOP
  kept against XCHG*ar. A decision is one entry when its 256 are the same (no ModRM read).
- **The decoder**: readPrefixes (legacy prefixes, the first LOCK and xacquire/xrelease's F2/F3 left as instructions of their own, the
  mandatory prefix, REX, REX2, VEX2, VEX3, XOP and EVEX with LES/LDS/BOUND/POP outside 64-bit mode), readOpcode (the maps, 3DNow!'s
  ModRM before its opcode), getInstructionID (the attribute mask, the context it names, the W bit outside 64-bit mode, the
  absolute moves' and umonitor's prefixes, OpSize's 16-bit equivalent, `90` with REX.B), readModRM and readSIB (16-, 32- and 64-bit
  addressing, RIP, the extension bits of REX, REX2 and EVEX), readOperands (each specifier: registers fixed up by type, VSIB,
  immediates, the register in an immediate, vvvv, the opmask, the rounding control, the condition code and flags), and the
  translation into the MCInst (immediates sign-extended by encoding, EIZ/RIZ, the string instructions' indexes, the offsets'
  segment), its flags (`:lock`, `:rep`, `:repne` when not mandatory; `:addr32`/`:addr16` when the operands do not need the
  address-size prefix, X86_MC::needsAddressSizeOverride, printed by printInstFlags). `decode-all` reads a byte stream as llvm-mc
  does, an undecodable instruction skipped by its bytes read (at least one).
- **Printing** is §5's printer, unchanged but for the decoder's address-size flags.
- **The check** (`arbace.asm.amd64.decode-check`, `bin/repr-asm decode-check [--record] [dis|rt|obj]`): `dis`, MC/Disassembler/X86's
  tests (`test/asm/corpora.edn`'s `:decode-corpora`) by their own CHECK lines: each run's options (the triple's mode, the output
  variant, `--print-imm-hex`, `--show-encoding` re-encoded by the encoder, `--hex`, split-file parts), the input read as llvm-mc's
  `Disassembler.cpp` reads it (tokens, `#` comments, `[...]` atomic blocks, its errors), decoded, printed, the CHECK lines held to
  that output as FileCheck holds them (CHECK, -NEXT, -SAME, -EMPTY, -NOT, `--implicit-check-not`, `{{regex}}`, blanks collapsed);
  `rt`, the round trip: every encoding encode-check meets in MC/X86 decoded back to the matcher's instruction and operands (a
  symbol's operand any immediate; LLVM's lone LOCK first and another instruction of the same bytes counted apart); `obj`, musl
  1.2.6's files assembled into objects by §7's assembler, their code sections decoded and held to the files' instructions in order
  (a branch relaxed to its long form). Rows `test/asm/decode-results-amd64.edn`, classes `test/asm/divergences-decode.edn`,
  controls `test/asm/controls-decode.txt`. `bin/repr-asm decode MODE [intel] HEX...` and `decode-object OBJ` by hand.

Not yet: `--mdis`'s markup printer; FileCheck's variables (`[[...]]`); a symbolizer (llvm-objdump's targets and relocations).

## 10. x86i, the x86-64 integer machine (AX5.2-AX5.4)

x86i interprets a static x86-64 Linux executable's integer instructions (doc/asm/AMD64-SURVEY.md, "As built: AX5.2"; the design
AX5-DESIGN, AXS51-AXS62, decided by the user 2026-11-10). It is Go forms built alone by TamaGo's go, a light driver as si's and
oi's are (AXS51 (a)), out of every key (`arbace_crop_paths`): no Arbace build reads it.

- **`go/arbace/emu`**, the frame of the machine emulators: `AS`, a process's address space over crt's `Mem` (crt's slabs hold the
  bytes; emu keeps each page's R/W/X permissions, Linux's layout and a 256-entry software TLB; an access returns a fault instead
  of panicking, crt's `R`/`W` never on the hot path); `ReadELF`/`LoadELF` (an ELF64 executable's PT_LOADs page-aligned with their
  permissions, file bytes then zeros, a page two segments share given both's permissions; PT_INTERP refused, a static PIE at
  Linux's no-ASLR base); `Stack` (the stack as Linux gives it from 0x7ffffffff000: argc, argv, envp, auxv with AT_PHDR, AT_ENTRY,
  AT_RANDOM, AT_EXECFN, AT_PLATFORM, AT_HWCAP, AT_MINSIGSTKSZ ..., no AT_SYSINFO_EHDR: no vdso; from the psABI and getauxval(3),
  not the kernel's source); `Kernel`, the system calls beyond crt's (brk from the page after the highest PT_LOAD; anonymous
  mmap top-down from 128 MiB below the stack's top, MAP_FIXED, munmap, mprotect, with their permissions; exit and exit_group;
  clone, fork, vfork, execve -ENOSYS), every other call crt's `Sys.Syscall` on the same `Mem` unchanged (crt untouched,
  AXS52 (a)); the tracer `Tracee` (`trace_linux`: ptrace from one locked thread, ASLR off by `personality(ADDR_NO_RANDOMIZE)`,
  PTRACE_O_TRACESYSGOOD and EXITKILL, memory through /proc/PID/mem).
- **`go/arbace/x86i`**: the decoder (`decode`, `tables`) is §9's, LLVM's X86 disassembler, rebuilt in Go for 64-bit mode's legacy
  and REX encodings over the tables `arbace.asm.amd64.dgen` writes from `asm/amd64/table.edn` (`dectab.ae`, generated, checked by
  regeneration: `bin/repr-asm x86i gen|gen-check`): the contexts the attribute mask names, each map's (one byte, 0F, 0F38, 0F3A)
  decisions by context and opcode (one instruction, or 256 by ModRM), every instruction a decision names with its Intel mnemonic
  and operand specifiers (encoding, type, size or tied operand). readPrefixes, readOpcode, getInstructionID's corrections,
  readModRM/SIB/displacement, readOperands and the translation follow §9's port; VEX, EVEX, XOP, REX2 and 3DNow! are refused
  (SIGILL, `:unimplemented`). The machine (`cpu`): sixteen registers, RIP, lazy flags (`flags`, AXS55 (a): the last
  flag-setting instruction's kind, size, operands and result; a condition computed from them, a `cmp`'s directly; RFLAGS
  materialized on a whole read; `Undef`, the bits the SDM leaves undefined after it), FS and GS bases; instructions decoded once
  per address (a store into a page holding decoded code invalidates it). The families (`sem`, hand-written from the Intel SDM vol. 2
  and the AMD APM vol. 3 as references, AXS54 (a)), chosen by the instruction's Intel mnemonic from the table: alu (add or adc sbb
  and sub xor cmp test), unary (inc dec neg not), mov (mov movabs movzx movsx movsxd lea xchg bswap cmovcc setcc), stack (push pop
  pushfq popfq leave), branch (jmp jcc call ret loop loope loopne jrcxz jecxz), muldiv (mul imul div idiv; #DE is SIGFPE), shift
  (shl sal shr sar rol ror), string (movs stos lods scas cmps, rep/repe/repne, DF), conv (cbw cwde cdqe cwd cdq cqo), atomic
  (cmpxchg xadd; one thread), misc (nop and the prefixes LLVM decodes as instructions, clc stc cmc cld std lahf sahf, syscall,
  cpuid with x86i's own model, rdtsc, ud2/int3/hlt as SIGILL/SIGTRAP/SIGSEGV). Every other mnemonic, and an operand of a register
  file x86i does not model (XMM, x87, MMX, segment, debug, control), raises SIGILL naming the instruction (`:unimplemented`),
  never a silent wrong result. A fault (an unmapped or protected access, a fetch from a page without X) is SIGSEGV; with no
  handler (AX5.4 builds the frames) the process ends with 128 + the signal, as the shell sees the CPU's.
- **The lockstep's sys mode** (`lockstep_linux`, AX5.3's first part, AXS56 (a)-AXS58 (a)): the executable run natively under
  ptrace and on x86i from the native's own initial state (its stack's bytes, the auxv's AT_SYSINFO_EHDR made AT_IGNORE in both),
  compared at each system call's entry (the number, RIP, every register but RAX, RCX and R11, the flags the last instruction
  defined, FS's base) and exit (the result), the world's results and buffers copied from the native (clock_gettime,
  gettimeofday, time, getrandom, getpid, gettid, getppid, uname, sched_getaffinity), mmap's address and the break's start taken
  from it; a signal compared by number and RIP; at the end the status, stdout and stderr. The verdict: `equal`, or the first
  difference.
- **The driver** (`test/asm/x86i-driver`, `x86idriver`): `run [--stats] EXE [ARG...]`, `decode-text EXE`, `trace --mode sys|step|bisect EXE`;
  `X86I_INJECT=FAMILY:FLAG` flips the flag after each instruction of the family (the negative controls' fault; `loader:random` alters
  x86i's own stack for the loader's judge). `bin/repr-asm x86i
  light` prints the forms by repr/go and builds them by TamaGo's go into `.tmp/x86i/x86i` (seconds); `x86i run|decode-text|trace`.
- **The check** (`bin/repr-asm x86i-check [--record]`, `test/asm/x86i_judge.ae`): `test/asm/run/`'s programs and x86i's own
  hand-written ones (`test/asm/x86i/`: `alu.s`, `muldiv.s`, `string.s`, `misc.s`, eight `fault-*.s`; `# judge: cpu` in a header:
  its stdout's judge is the CPU's run, AXS9 (a)), assembled and linked by Arbace, each run by the CPU and by x86i: `x86i/NAME`, x86i's
  run by the program's own verdict and equal to the CPU's (status, stdout, stderr); `decode/NAME`, every instruction of its code
  as x86i decodes it equal to `arbace.asm.amd64.decode`'s decoding of the same bytes; `lockstep/NAME`, the sys mode equal. Rows
  `test/asm/x86i-results-amd64.edn`, classes `test/asm/divergences-x86i.edn`, controls `test/asm/controls-x86i.txt` (a run judged by
  another's verdict or against another's CPU run, a decoding held to another's bytes, injected faults the CPU's run and the
  lockstep must catch).

- **The lockstep's step mode, the bisection and the loader's judge** (AX5.3, AXS56 (a), AXS57 (a), AXS72 (a)): `trace --mode step`
  single-steps the native process (PTRACE_SINGLESTEP from the tracing thread, pure Go) and compares after every instruction the
  sixteen registers, RIP, the status flags and DF less those the last flag-setting instruction left undefined (the SDM's rule for that
  instruction, not a family's; the native's undefined bits are then copied into x86i so the two stay one state), FS's and GS's bases,
  the bytes the instruction stored (x86i's store log, read back from the native), XMM and MXCSR when the instruction names an XMM
  register (PTRACE_GETFPREGS), and the writable mappings every 4,096 instructions and at each system call but write(2) (a page one maps
  writable and the other does not is a difference). mmap's address and the break's start are x86i's own choice there, held to the
  native's (the layout; the vdso's and vvar's eight pages below mmap's top, `emu.VdsoSize`, judged against the native's maps).
  Single-stepping's artifacts handled by rule: a `rep` instruction traps after each iteration (stepped until RIP moves), a prefix LLVM
  decodes as an instruction of its own (`lock` before an instruction it decodes apart) is one native step, `pushfq` stores TF (cleared
  in the native's word), a syscall's R11 holds TF (cleared), rt_sigreturn's orig_rax is -1, each step's trap is a SIGTRAP the kernel
  forces (it unblocks SIGTRAP and is the thread's last trap, #DB: x86i's mask and trap state follow), cpuid's native answer overwritten
  with x86i's model's, rdtsc's and rdtscp's copied from the native. `X86I_STEP_LIMIT=N` ends the step mode after N instructions, the
  rest in sys mode (a header's `# step-limit: N`: alu, muldiv). `trace --mode bisect` runs the sys mode and, at a difference, the sys
  mode again to the last system call both agreed on and the step mode from there: the first differing instruction, decoded, with its
  family. The sys mode also compares the bytes write(2) writes. The loader's judge (every trace's second line): x86i's own initial
  stack (`emu.Stack` with the native's AT_RANDOM bytes) against the native's at its exec: the registers at the entry (zero but RSP,
  RIP the entry, the flags clear), argc, argv's and envp's addresses, the auxv in order (AT_HWCAP, AT_HWCAP2, AT_MINSIGSTKSZ and the
  kernel's AT_RSEQ_* masked; AT_SYSINFO_EHDR x86i omits), the strings' bytes, RSP by the entries x86i omits. It found the loader's
  first difference: Linux maps a segment's whole file pages, so the bytes before a segment in its first page (the ELF header before
  `.data`) are the file's, not zeros (`emu.LoadELF` now maps them so).
- **AX5.4's families**: shift's rcl and rcr (the count modulo 9 and 17 for bytes and words) and shld and shrd (a 16-bit count beyond 16
  leaves the result and flags undefined: x86i computes the 32-bit concatenation, :vendor); bit: bt bts btr btc (a register offset into
  memory reaches beyond the operand, signed, in operand-sized steps), bsf bsr (a zero source sets ZF and leaves the destination, the
  APM's choice), tzcnt lzcnt popcnt; enter with its nesting levels; xlatb; movbe; rdtscp. cpuid answers x86i's model (AXS59): leaf 0 the
  vendor "ArbaceX86iVM" and the highest leaf 1, leaf 1's EDX `0x178bfbff` (AT_HWCAP) and ECX POPCNT and MOVBE, 0x80000001's LZCNT,
  SYSCALL, NX and LM; tzcnt runs as BMI1's though the model claims BMI1 only with its VEX forms (AX5.5).
- **Signals with handlers** (`signal`): rt_sigaction, rt_sigprocmask and sigaltstack are x86i's own (crt's table and mask kept in step
  through it, so crt's kill of the process itself reaches x86i's hook: pending, delivered at the next instruction's boundary);
  a fault with a handler, or a pending signal, gets the frame Linux builds on x86-64 (the restorer, the ucontext with uc_stack, the
  mcontext's 23 registers with ERR, TRAPNO, OLDMASK and CR2 (the thread's last trap's, CR2 the last page fault's: a later frame shows
  them stale, as Linux's), the fpstate's address, the mask, the siginfo with si_code and si_addr or a kill's sender, the fpstate an
  XSAVE image of the model's size, `XSaveModel`; RFLAGS saved with RF for a fault), below the red zone or on the alternate stack
  (SA_ONSTACK), 16-aligned less 8; RDI, RSI, RDX the signal, siginfo and ucontext, RAX 0, DF and TF cleared, the mask with sa_mask and
  the signal (SA_NODEFER, SA_RESETHAND); rt_sigreturn restores the registers, the user flags, the mask, MXCSR and XMM, the alternate
  stack. A fault blocked or with no handler kills the process. The lockstep delivers the native first (single-stepped into the
  handler: its fpstate's address gives the host's XSAVE size, which x86i's frame then takes) and compares the frame byte for byte, the
  fpstate's MXCSR and XMM, the rest of it (the host's XSAVE components) copied.
- **The synthetic programs** (`test/asm/x86i/gen.ae`, AXS60 (a)): `bin/repr-asm x86i synth DIR [--record]` writes one program a family
  (bit, rotc, shiftd, cond: every setcc and cmovcc over the 32 combinations of CF PF ZF SF OF and after the lazily computed producers,
  misc: enter, xlatb, movbe, bswap, cmpxchg, xadd), seeded, into `.tmp`; their digests in `test/asm/x86i/synth-digests.edn`. x86i-check
  writes them into its work directory and judges them with the rest: the CPU's run and both lockstep modes.
- **The check** adds the rows `step/NAME` (the step mode equal) and `loader/NAME` (the loader's judge), the hand-written `signals.s` and
  `cpuid.s` (`# cpu: cpuid`: its own stated stdout, the model's; its CPU-compared run and sys mode the class `:cpuid`), and the controls
  `step`, `bisect` (an injected fault caught at, or bisected to, an instruction of its family) and `loader` (`X86I_INJECT=loader:random`).

Not yet (AX5.5-AX5.6): BMI, ADX, cmpxchg16b, rdrand/rdseed/rdpid, SSE/SSE2 and the VEX contexts; per-run coverage (the opcodes executed
against the families' claims); musl's harnesses.