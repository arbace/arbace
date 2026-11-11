# Arbace's run test (AR4): an undefined weak symbol resolved to zero (through the GOT), a weak definition overridden by a strong
# one in another object, a weak definition kept when nothing overrides it
# exit: 0
# stdout: "0\n2\n3\n"
# with: lib/print.s lib/weak-lib.s
	.globl	_start
	.weak	missing
	.weak	overridden
	.weak	kept
_start:
	adrp	x0, :got:missing
	ldr	x0, [x0, :got_lo12:missing]
	bl	put_u64
	bl	overridden
	bl	put_u64
	bl	kept
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
overridden:
	mov	x0, #1
	ret
kept:
	mov	x0, #3
	ret
