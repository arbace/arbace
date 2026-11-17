# Arbace's synthetic object test (AX4): code alignment's NOPs of every length, in 64- and 16-bit code
	.text
	.p2align 4
	nop
	.p2align 2
	nop
	.p2align 3
	nop
	.p2align 4
	nop
	.p2align 5
	movl	$1, %eax
	.p2align 6
	ret
	.balign 32
	.code16
	nop
	.p2align 4
	nop
	.code64
	.p2align 4, , 3
	ret
