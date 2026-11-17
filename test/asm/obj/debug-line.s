# Arbace's synthetic object test (AX12, AXS92 (a)): the DWARF 4 line program .loc makes in .debug_line (MCDwarfLineTable's
# header, emitOne's opcodes, MCDwarfLineAddr's advances), held to llvm-mc 23.1.5's object recorded by hand
	.file	1 "dir/a.c"
	.file	2 "/usr/include" "b.h"
	.file	3 "c.c"
	.file	4 "dir/d.h"
	.text
	.globl	f
	.type	f,@function
f:
	.loc	1 7 3 prologue_end
	pushq	%rbp
	# two in a row: the first gets its entry at the same place
	.loc	2 9 is_stmt 0 isa 2 discriminator 5
	.loc	1 30
	movq	%rsp, %rbp
	.loc	1 3 basic_block epilogue_begin
	jmp	1f
	# a jump relaxed by the bytes between: the advance from the final offsets
	.fill	300, 1, 0x90
1:	.loc	1 2000
	ret
	.loc	1 1 is_stmt 1
	.byte	1
	.loc	3 1
	.ascii	"abc"
	.loc	3 1 0
	nop
	# a line advance out of the special opcodes' range with no address advance: DW_LNS_copy
	.loc	3 100
	.loc	3 50 4 discriminator 300
	nop
	nop
	# an address advance of 17 (DW_LNS_const_add_pc and a special opcode)
	.zero	16
	.loc	4 52 isa 0
	ret
	.zero	30
	.loc	4 53
	.short	7
	.loc	4 53
	.zero	3000
	.loc	4 40
	nop
	.size	f, .-f
	# a .loc at the end of a section, before a switch: no entry
	.loc	1 99
	.section	.text.g,"ax",@progbits
	.loc	2 10 7
	nop
	.p2align	6
	.loc	2 4
	.long	3
	.loc	2 5
	.zero	17
	.data
	.loc	1 12
	.quad	f
	.pushsection	.text.h,"ax",@progbits
	.loc	3 77
	ret
	.popsection
	.loc	1 13
	.long	2
	.text
	.loc	1 88
	jmp	f
	.zero	14
