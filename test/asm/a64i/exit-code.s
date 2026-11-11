# a64i's program (AR5): exit_group's status: the low 8 bits of x0
# with: lib/out.s
# exit: 52
	.text
	.globl	_start
_start:
	mov	x0, #0x1234
	b	exit_x0
