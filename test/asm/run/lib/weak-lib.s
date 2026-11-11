# The second object of weak.s (AX4): the strong definition that overrides the first object's weak one
	.globl	which
which:
	mov	$2, %eax
	ret
