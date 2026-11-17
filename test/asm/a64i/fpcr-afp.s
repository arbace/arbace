# a64i's program (queue 276, ARS71 (a)): FPCR's FEAT_AFP bits against this CPU (the M2 has no FEAT_AFP): FIZ, AH and NEP written
# read back as zero, and the FP instructions under the write behave as without them: a subnormal input not flushed (FIZ), a
# scalar's upper lanes cleared (NEP), fneg of a NaN flipping its sign (AH); every bit msr fpcr keeps read back; cleared by xzr
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "fff8000000000000\n"
# stdout: "0000000007ff9f00\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	// FIZ, AH and NEP written: read back as zero
	mov	x9, #7
	msr	fpcr, x9
	mrs	x0, fpcr
	bl	put_hex
	// fmul of the smallest subnormal by 2: not flushed
	mov	x10, #1
	fmov	d1, x10
	fmov	d2, #2.0
	fmul	d0, d1, d2
	fmov	x0, d0
	bl	put_hex
	// a scalar fadd clears the upper lanes (NEP would keep them)
	mov	x11, #-1
	mov	v3.d[1], x11
	fadd	d3, d1, d2
	umov	x0, v3.d[1]
	bl	put_hex
	// fneg of the default NaN flips its sign (AH would not)
	mov	x12, #0x7ff8000000000000
	fmov	d4, x12
	fneg	d5, d4
	fmov	x0, d5
	bl	put_hex
	// every writable bit: what this CPU keeps
	mov	x9, #0x9f07
	movk	x9, #0x07ff, lsl #16
	msr	fpcr, x9
	mrs	x0, fpcr
	msr	fpcr, xzr
	bl	put_hex
	mrs	x0, fpcr
	bl	put_hex
	b	exit0
