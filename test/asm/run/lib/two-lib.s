# The second object of two-objects.s (AX4): a global function, global data, an absolute symbol, a table of pointers
	.text
	.globl	add3
	.type	add3,@function
add3:
	lea	(%rdi,%rsi), %rax
	add	%rdx, %rax
	ret
	.size	add3, .-add3

	.globl	ret9
	.type	ret9,@function
ret9:
	mov	$9, %eax
	ret

	.data
	.globl	counter
	.type	counter,@object
	.p2align 3
counter:
	.quad	40
	.size	counter, 8
	.globl	ptrs
	.type	ptrs,@object
ptrs:	.quad	add3, ret9, counter
	.size	ptrs, 24
	.globl	absval
	absval = 0x1234
