# a64i's program (AR5): a load through a misaligned SP: SIGBUS
# with: lib/out.s
# exit: 135
	.text
	.globl	_start
_start:
	sub	sp, sp, #8
	ldr	x0, [sp]
