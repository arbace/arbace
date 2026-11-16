# a64i's program (AR5): udf: SIGILL
# with: lib/out.s
# exit: 132
	.text
	.globl	_start
_start:
	udf	#1
