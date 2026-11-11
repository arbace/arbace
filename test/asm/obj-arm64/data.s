// Arbace's synthetic object test (AR4): data directives and their mapping symbols ($d), code after data ($x), alignment
// padded with NOPs in code and zeros in data, sections with flags, a COMDAT group, .comm and .lcomm, sizes and types
	.text
	.globl	f
	.type	f,@function
f:	mov	x0, #1
	.word	0x12345678
	.p2align 4
	ret
	.size	f, .-f
	.section .rodata,"a"
	.byte	1, 2, 3
	.p2align 3
	.xword	f
	.hword	7
	.ascii	"ab"
	.asciz	"cd"
	.section .text.g,"axG",@progbits,g,comdat
	.weak	g
g:	nop
	.inst	0xd503201f
	.comm	c, 16, 8
	.lcomm	lc, 4
	.section .note.GNU-stack,"",@progbits
