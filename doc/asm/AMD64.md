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
  ProgRef: the preprocessor's `#ifdef`, classes with template arguments (positional and named), defs, multiclasses and
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
  `CD8_Scale`, the operands and ties). Since the third slice the printer's fields too (after `AsmWriterInst.cpp` and
  `AsmWriterEmitter.cpp`): each instruction's `:print`, its AT&T AsmString (variant 0) parsed into literal fragments and
  `[MI-OPERAND PRINT-METHOD]` for each `$operand` (the Operand's or RegisterOperand's `PrintMethod`, else `printOperand`), and
  `:print-aliases`, printAliasInstr's patterns (InstAliases of `EmitPriority` 1 or more with no more operands than their
  instruction, their conditions on the MCInst's operands, the ambiguous ones dropped): X86 has three, `aad`, `aam`, `xstorerng`.
- **Its oracle** (`bin/repr-asm table oracle`, by hand): llvm-tblgen 23.1.5's `-gen-asm-matcher` on the same files; its
  `MatchTable0` and the table's matchables are the same 23,014 rows in the same order with the same classes.

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
the `{evex}` pseudo prefix (`checkTargetMatchPredicate`). Not yet: APX (REX2, map 4's NDD, NF, CCMP/CTEST, `%r16`-`%r31`,
`{rex2}`, `{evex} cmp`), relaxation (llvm-mc's `-show-encoding` prints the unrelaxed instruction, so the stated encodings need
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
  reference) and the RIP-relative bias of a following immediate. An instruction that may need relaxation (JCC_1, JMP_1, the short
  immediate forms of X86EncodingOptimizationForImmediate.def with a symbolic immediate) is a chunk of its own; the rest is data.
- **The layout** (MCAssembler::layout, relaxOnce): the sections laid out, then relaxed to a fixed point, each pass forward with the
  stretch of a later target; a branch relaxed when its target is not resolved (a global or undefined symbol, another section) or out of
  a signed byte, or has a specifier; JCC_2/JMP_2 in 16-bit code. The relaxed branches' encodings (codegen-only in the table) are given
  after X86InstrControl.td.
- **The fixups** (evaluateFixup, X86AsmBackend, X86ELFObjectWriter, ELFObjectWriter::recordRelocation): the PC-relative bias (-1, -2,
  -4), resolved within a section for a local symbol, else a relocation: its type by kind, size and specifier (PC32, PLT32, 32, 32S, 64,
  PC64, 16, 8, GOTPCREL, GOTPCRELX, REX_GOTPCRELX, GOT32/64, GOTPC32/64, GOTOFF64, TPOFF32/64, DTPOFF32/64, SIZE32/64, TLSGD, TLSLD,
  GOTTPOFF, TLSDESC, TLSDESC_CALL, PLTOFF64), against the section symbol for a defined local unless the specifier needs the symbol, a
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
COMDAT groups beyond the one tested, x32 and i386 objects, the linker and the decoder (AX4's next slices).
