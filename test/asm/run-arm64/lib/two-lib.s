# The second object of two-objects.s (AR4): a global function, global data, an absolute symbol, a table of pointers
	.text
	.globl	add3
	.type	add3,@function
add3:
	add	x0, x0, x1
	add	x0, x0, x2
	ret
	.size	add3, .-add3
	.globl	ret9
	.type	ret9,@function
ret9:
	mov	x0, #9
	ret
	.data
	.globl	counter
	.type	counter,@object
	.p2align 3
counter:
	.xword	40
	.size	counter, 8
	.globl	ptrs
	.type	ptrs,@object
ptrs:	.xword	add3, ret9, counter
	.size	ptrs, 24
	.globl	rel
rel:	.word	ret9 - .
	.globl	absval
	absval = 0x1234
