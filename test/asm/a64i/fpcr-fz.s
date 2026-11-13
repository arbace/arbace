# a64i's program (queue 282, ARS75 (a)): FPCR.FZ at single and double precision against this CPU: FZ written reads back; a
# subnormal operand flushed to a zero of its sign with IDC (fmul, fadd, fcmp, fcvt from double, fcvt from single to half,
# fmlal's single accumulator, fjcvtzs and its Z), a result tiny before rounding flushed with UFC alone (fmul, fcvt to single,
# frecpe of 2^127); not flushed: fneg, a half operand (FZ16 clear: fadd h, fcvt from h, fmlal's halves); cleared by xzr
# with: lib/out.s
# exit: 0
# stdout: "0000000001000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "000000003f800000\n"
# stdout: "0000000000000080\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000060000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "3e70000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "3380000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000000001\n"
# stdout: "0000000040000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	// FZ written: read back
	mov	x9, #0x1000000
	msr	fpcr, x9
	mrs	x0, fpcr
	bl	put_hex
	// fmul of the smallest subnormal by 2: the operand flushed (+0), IDC
	mov	x10, #1
	fmov	d1, x10
	fmov	d2, #2.0
	msr	fpsr, xzr
	fmul	d0, d1, d2
	bl	res_fpsr
	// fmul of the smallest normal by 0.5: tiny before rounding, flushed (+0), UFC alone
	mov	x10, #0x0010000000000000
	fmov	d3, x10
	fmov	d4, #0.5
	msr	fpsr, xzr
	fmul	d0, d3, d4
	bl	res_fpsr
	// fadd of a single subnormal and 1.0: exactly 1.0, IDC (not IXC)
	mov	w10, #1
	fmov	s5, w10
	fmov	s6, #1.0
	msr	fpsr, xzr
	fadd	s0, s5, s6
	bl	res_fpsr
	// fneg of a subnormal: not flushed, no flag
	msr	fpsr, xzr
	fneg	d0, d1
	bl	res_fpsr
	// fcmp of a subnormal with 0.0: equal (Z, C), IDC
	msr	fpsr, xzr
	fcmp	d1, #0.0
	mrs	x0, nzcv
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	// fcvt to single of a double subnormal: flushed, IDC; of 2^-130: tiny for single, flushed, UFC
	msr	fpsr, xzr
	fcvt	s0, d1
	bl	res_fpsr
	mov	x10, #0x37d0000000000000
	fmov	d7, x10
	msr	fpsr, xzr
	fcvt	s0, d7
	bl	res_fpsr
	// fcvt to half of a single subnormal: flushed, IDC; fcvt to double of a half subnormal: not (2^-24), no flag
	msr	fpsr, xzr
	fcvt	h0, s5
	bl	res_fpsr
	mov	w10, #1
	fmov	s16, w10
	msr	fpsr, xzr
	fcvt	d0, h16
	bl	res_fpsr
	// fadd of two half subnormals: FZ16 clear, not flushed (0x0002)
	msr	fpsr, xzr
	fadd	h0, h16, h16
	bl	res_fpsr
	// fmlal: lane 0 the single subnormal accumulator flushed (1.0 * 0 + 0 = +0, IDC), lane 1 1.0 times the half subnormal
	// 2^-24, not flushed (0x33800000)
	mov	x10, #1
	fmov	d17, x10
	mov	w11, #0x3c00
	lsl	x11, x11, #16
	movk	x11, #0x3c00
	fmov	d18, x11
	mov	x12, #0x10000
	fmov	d19, x12
	msr	fpsr, xzr
	fmlal	v17.2s, v18.2h, v19.2h
	fmov	x0, d17
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	// fjcvtzs of the double subnormal: flushed, 0, Z clear, IDC; of 1.0: 1, Z set
	msr	fpsr, xzr
	fjcvtzs	w0, d1
	mrs	x13, nzcv
	bl	put_hex
	mov	x0, x13
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	fmov	d20, #1.0
	fjcvtzs	w0, d20
	mrs	x13, nzcv
	bl	put_hex
	mov	x0, x13
	bl	put_hex
	// frecpe of 2^127: a zero, UFC (FPRecipEstimate's flush under FZ)
	mov	w10, #0x7f000000
	fmov	s21, w10
	msr	fpsr, xzr
	frecpe	s0, s21
	bl	res_fpsr
	msr	fpcr, xzr
	mrs	x0, fpcr
	bl	put_hex
	b	exit0

// res_fpsr: d0's bits and FPSR
res_fpsr:
	stp	x29, x30, [sp, #-16]!
	fmov	x0, d0
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	ldp	x29, x30, [sp], #16
	ret
