# The second object of weak.s (AR4): a strong definition overriding the first object's weak one
	.globl	overridden
overridden:
	mov	x0, #2
	ret
