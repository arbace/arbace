# The second object of merge-strings.s (AX4): the same string in a mergeable section, its address taken
	.section .rodata.str1.1,"aMS",@progbits,1
.Lw:	.asciz	"world\n"
	.data
	.globl	world_ptr
	.p2align 3
world_ptr:
	.quad	.Lw
