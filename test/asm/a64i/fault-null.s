# a64i's program (AR5): a load from address 0: SIGSEGV
# with: lib/out.s
# exit: 139
	.text
	.globl	_start
_start:
	mov	x1, #0
	ldr	x0, [x1]
