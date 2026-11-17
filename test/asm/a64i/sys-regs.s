# a64i's program (AR5): system registers: msr/mrs tpidr_el0 and nzcv, fpcr, fpsr; hints and barriers
# with: lib/out.s
# exit: 0
# stdout: "0000ffff12345678\n"
# stdout: "0000000060000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000400000\n"
# stdout: "0000000000000001\n"
	.text
	.globl	_start
_start:
	movz	x1, #22136
	movk	x1, #4660, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #0, lsl #48
	msr	tpidr_el0, x1
	mrs	x0, tpidr_el0
	bl	put_hex
	mov	x1, #0x60000000
	msr	nzcv, x1
	mrs	x0, nzcv
	bl	put_hex
	bl	put_nzcv
	mrs	x0, fpcr
	bl	put_hex
	mov	x1, #0x400000
	msr	fpcr, x1
	mrs	x0, fpcr
	bl	put_hex
	msr	fpcr, xzr
	nop
	yield
	dmb	ish
	dsb	sy
	isb
	dmb	ishld
	dsb	ishst
	prfm	pldl1keep, [sp]
	prfm	pstl2strm, [sp, #64]
	mov	x0, #1
	bl	put_hex
	b	exit0
