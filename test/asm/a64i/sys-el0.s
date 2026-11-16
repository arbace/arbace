# a64i's program (AR19, queue 288): the EL0 system space Go's runtime and std use, as this M2 runs it under Linux: MRS of
# ctr_el0, dczid_el0 and tpidrro_el0 (the M2's values), cntfrq_el0 (24 MHz) and cntvct_el0, cntvctss_el0 (counted: two reads in
# order, the second not below the first, both nonzero; the registers cleared after), PSTATE.DIT and PSTATE.SSBS by MSR
# (immediate), MSR and MRS; the bits DIT and FPSR keep of a write of all ones; dc zva over a 64-byte block in a 0xff-filled buffer (the block zeroed, its neighbours kept); dc cvac,
# cvau, civac, cvap, cvadp and ic ivau (no effect); sb; FEAT_LOR's ldlar and stllr; the hints yield, sevl, wfe and sev
# with: lib/out.s
# exit: 0
# stdout: "000000009444c004\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "00000000016e3600\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000001000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000001000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000001000000\n"
# stdout: "000000000800009f\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000012345678\n"
# stdout: "0000000012345678\n"
	.text
	.globl	_start
_start:
	mrs	x0, ctr_el0
	bl	put_hex
	mrs	x0, dczid_el0
	bl	put_hex
	mrs	x0, tpidrro_el0
	bl	put_hex
	mrs	x0, cntfrq_el0
	bl	put_hex
	// the counter: two reads, nonzero and in order
	mrs	x1, cntvct_el0
	isb
	mrs	x2, cntvct_el0
	cmp	x2, x1
	cset	x0, hs
	cmp	x1, #0
	csel	x0, xzr, x0, eq
	mov	x1, #0
	mov	x2, #0
	bl	put_hex
	mrs	x1, s3_3_c14_c0_6		// cntvctss_el0 (FEAT_ECV's self-synchronised view)
	mrs	x2, s3_3_c14_c0_6
	cmp	x2, x1
	cset	x0, hs
	cmp	x1, #0
	csel	x0, xzr, x0, eq
	mov	x1, #0
	mov	x2, #0
	bl	put_hex
	// PSTATE.DIT (bit 24)
	mrs	x0, dit
	bl	put_hex
	msr	dit, #1
	mrs	x0, dit
	bl	put_hex
	msr	dit, xzr
	mrs	x0, dit
	bl	put_hex
	mov	x1, #0x1000000
	msr	dit, x1
	mrs	x0, dit
	bl	put_hex
	msr	dit, #0
	// PSTATE.SSBS (bit 12): set at the process's start
	mrs	x0, ssbs
	bl	put_hex
	msr	ssbs, #0
	mrs	x0, ssbs
	bl	put_hex
	mov	x1, #0x1000
	msr	ssbs, x1
	mrs	x0, ssbs
	bl	put_hex
	// the bits each register keeps of a write of all ones (FPSR: QC and the cumulative flags; its bits 31-28 are AArch32's)
	mov	x1, #-1
	msr	dit, x1
	mrs	x0, dit
	bl	put_hex
	msr	dit, xzr
	mov	x1, #-1
	msr	fpsr, x1
	mrs	x0, fpsr
	bl	put_hex
	msr	fpsr, xzr
	mov	x1, #0
	// dc zva: the 64-byte block holding x20+70 (buf+64 .. buf+127) zeroed
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	add	x1, x20, #70
	dc	zva, x1
	ldr	x0, [x20, #56]
	bl	put_hex
	ldr	x0, [x20, #64]
	bl	put_hex
	ldr	x0, [x20, #120]
	bl	put_hex
	ldr	x0, [x20, #128]
	bl	put_hex
	// the other cache maintenance: no effect on the bytes
	add	x1, x20, #8
	dc	cvac, x1
	dc	cvau, x1
	dc	civac, x1
	dc	cvap, x1
	dc	cvadp, x1
	ic	ivau, x1
	dsb	ish
	isb
	sb
	ldr	x0, [x20, #8]
	bl	put_hex
	// FEAT_LOR: ldlar and stllr
	mov	x1, #0x5678
	movk	x1, #0x1234, lsl #16
	stllr	x1, [x20]
	ldlar	x0, [x20]
	bl	put_hex
	add	x2, x20, #8
	stllr	w1, [x2]
	ldlar	w0, [x2]
	mov	x2, #0
	bl	put_hex
	yield
	sevl
	wfe
	sev
	mov	x1, #0
	b	exit0

	.data
	.p2align	6
buf:
	.fill	256, 1, 0xff
