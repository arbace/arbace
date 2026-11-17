# a64i's program (AR5): a store into a read-only page: SIGSEGV
# with: lib/out.s
# exit: 139
	.text
	.globl	_start
_start:
	adr	x1, _start
	str	x1, [x1]
