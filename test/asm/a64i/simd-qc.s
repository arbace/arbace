# a64i's program (queue 276, ARS44 with AR12-AR13): FPSR.QC's stickiness, held to this CPU by the lockstep's step mode: set by a
# saturating sqdmulh, kept by one that does not saturate, by an FP exception's IXC beside it and across write(2); cleared alone
# by msr fpsr with IXC kept, set again by uqxtn's narrowing; sqabs's scalar (AR13) the same; cleared by msr fpsr, xzr
# with: lib/out.s
# exit: 0
# stdout: "7fff7fff7fff7fff\n"
# stdout: "0000000008000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000008000000\n"
# stdout: "0000000008000010\n"
# stdout: "0000000000000010\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000008000010\n"
# stdout: "0000000000007fff\n"
# stdout: "0000000008000000\n"
# stdout: "0000000008000000\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	adrp	x20, qd
	add	x20, x20, :lo12:qd
	msr	fpsr, xzr
	ldr	q1, [x20]
	ldr	q2, [x20, #16]
	// saturating: MIN * MIN doubled, QC set
	sqdmulh	v0.8h, v1.8h, v1.8h
	umov	x0, v0.d[0]
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	// not saturating (2 * 2 * 2 >> 16): QC kept
	sqdmulh	v3.8h, v2.8h, v2.8h
	umov	x0, v3.d[0]
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	// an FP exception beside it: 1/3 inexact, IXC added, QC kept
	fmov	d4, #1.0
	fmov	d5, #3.0
	fdiv	d6, d4, d5
	mrs	x0, fpsr
	bl	put_hex
	// QC cleared alone, IXC kept
	mov	x9, #0x10
	msr	fpsr, x9
	mrs	x0, fpsr
	bl	put_hex
	// a saturating narrow: 0x8000 unsigned into a byte, QC set again
	uqxtn	v7.8b, v1.8h
	umov	x0, v7.d[0]
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	// sqabs's scalar of MIN saturates; of 2 it does not, QC kept
	msr	fpsr, xzr
	sqabs	h8, h1
	umov	w0, v8.h[0]
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	sqabs	h9, h2
	mrs	x0, fpsr
	bl	put_hex
	msr	fpsr, xzr
	mrs	x0, fpsr
	bl	put_hex
	b	exit0

	.data
	.balign	16
qd:	.hword	0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000, 0x8000
	.hword	2, 2, 2, 2, 2, 2, 2, 2
