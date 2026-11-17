# a64i's program (AR10, ARS57 (a)): the Advanced SIMD structure loads and stores and the FP lanes in the essential gate:
# ld1-ld4 (multiple structures), ld1r and ld4r, a lane's ld1 and st3, st1 and st2 with post-index (immediate and register),
# the upper half cleared by a 64-bit load; fadd, fcmge, fcmeq on 4s, fmul on 2d, fdiv's FPSR (IXC)
# with: lib/out.s
# exit: 0
# stdout: "0f0e0d0c0b0a0908\n"
# stdout: "0d0c090805040100\n"
# stdout: "1f1e1b1a17161312\n"
# stdout: "1312111007060504\n"
# stdout: "0000000000000030\n"
# stdout: "1d1915110d090501\n"
# stdout: "0000000000000000\n"
# stdout: "0302010001000100\n"
# stdout: "1716151413121110\n"
# stdout: "0d0c090805040100\n"
# stdout: "131211100f0e0d0c\n"
# stdout: "0000000017161514\n"
# stdout: "0f0e0d0c03020100\n"
# stdout: "0000000040000000\n"
# stdout: "3f00000040000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffff00000000\n"
# stdout: "ffffffff00000000\n"
# stdout: "4002000000000000\n"
# stdout: "3eaaaaab3eaaaaab\n"
# stdout: "0000000000000010\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	adrp	x21, out
	add	x21, x21, :lo12:out
	// ld1 of one register: the upper doubleword
	ld1	{v0.16b}, [x20]
	umov	x0, v0.d[1]
	bl	put_hex
	// ld2 of halfwords: the even ones in v1, the odd ones in v2
	ld2	{v1.8h, v2.8h}, [x20]
	umov	x0, v1.d[0]
	bl	put_hex
	umov	x0, v2.d[1]
	bl	put_hex
	// ld3 of words, post-index immediate: words 1, 4, 7, 10 in v4; x20 moved by 48
	ld3	{v3.4s, v4.4s, v5.4s}, [x20], #48
	umov	x0, v4.d[0]
	bl	put_hex
	adrp	x9, buf
	add	x9, x9, :lo12:buf
	sub	x0, x20, x9
	bl	put_hex
	sub	x20, x20, #48
	// ld4 of bytes on 8b (a range): bytes 1, 5, ..., 29 in v17, its upper half cleared
	movi	v17.16b, #0xff
	ld4	{v16.8b-v19.8b}, [x20]
	umov	x0, v17.d[0]
	bl	put_hex
	umov	x0, v17.d[1]
	bl	put_hex
	// ld1r of a word, then a halfword lane loaded into lane 5
	ld1r	{v6.4s}, [x20]
	ld1	{v6.h}[5], [x20]
	umov	x0, v6.d[1]
	bl	put_hex
	// ld4r of doublewords: the third in v22, both halves
	ld4r	{v20.2d, v21.2d, v22.2d, v23.2d}, [x20]
	umov	x0, v22.d[1]
	bl	put_hex
	// st2 of bytes: v16 and v17 interleaved
	st2	{v16.8b, v17.8b}, [x21]
	ldr	x0, [x21]
	bl	put_hex
	// st3 of the lanes 1 of v3, v4 and v5 (words 3, 4, 5)
	st3	{v3.s, v4.s, v5.s}[1], [x21]
	ldr	x0, [x21]
	bl	put_hex
	ldr	w0, [x21, #8]
	bl	put_hex
	// st1 of four registers, post-index register: v3's low doubleword the last 16 bytes' first
	mov	x22, #64
	st1	{v0.16b, v1.16b, v2.16b, v3.16b}, [x21], x22
	ldr	x0, [x21, #-16]
	bl	put_hex
	sub	x21, x21, #64
	// the FP lanes: fadd, fcmge and fcmeq on 4s
	adrp	x9, fa
	add	x9, x9, :lo12:fa
	ld1	{v24.4s, v25.4s}, [x9]
	fadd	v26.4s, v24.4s, v25.4s
	umov	x0, v26.d[0]
	bl	put_hex
	umov	x0, v26.d[1]
	bl	put_hex
	fcmge	v27.4s, v26.4s, v24.4s
	umov	x0, v27.d[0]
	bl	put_hex
	umov	x0, v27.d[1]
	bl	put_hex
	fcmeq	v27.4s, v26.4s, v24.4s
	umov	x0, v27.d[1]
	bl	put_hex
	// fmul on 2d: 1.5 * 1.5
	add	x10, x9, #32
	ld1r	{v29.2d}, [x10]
	fmul	v30.2d, v29.2d, v29.2d
	umov	x0, v30.d[1]
	bl	put_hex
	// fdiv on 4s: 1/3, inexact (FPSR.IXC)
	msr	fpsr, xzr
	add	x10, x9, #40
	ld1r	{v7.4s}, [x10], #4
	ld1r	{v8.4s}, [x10]
	fdiv	v9.4s, v7.4s, v8.4s
	umov	x0, v9.d[0]
	bl	put_hex
	mrs	x0, fpsr
	bl	put_hex
	b	exit0

	.data
	.balign	16
buf:	.xword	0x0706050403020100, 0x0f0e0d0c0b0a0908, 0x1716151413121110, 0x1f1e1d1c1b1a1918
	.xword	0x2726252423222120, 0x2f2e2d2c2b2a2928, 0x3736353433323130, 0x3f3e3d3c3b3a3938
	.balign	16
// 1.5, -2.25, 3.0, 0.5 and 0.5, 2.25, -1.0, 0.0 (single); 1.5 (double); 1.0 and 3.0 (single)
fa:	.word	0x3fc00000, 0xc0100000, 0x40400000, 0x3f000000
	.word	0x3f000000, 0x40100000, 0xbf800000, 0x00000000
	.xword	0x3ff8000000000000
	.word	0x3f800000, 0x40400000
	.balign	16
out:	.zero	128
