# a64i's program (AR5): bitfield move keeping the rest: bfi, bfxil, bfm, bfc
# with: lib/out.s
# exit: 0
# stdout: "a5a5a5a5a5a5a005\n"
# stdout: "00000000a5a5a005\n"
# stdout: "a5a5a5a5a5a5a015\n"
# stdout: "00000000a5a5a015\n"
# stdout: "a5a5a5a5a5a5aff5\n"
# stdout: "00000000a5a5aff5\n"
# stdout: "a5a5a5a5a5a5aff5\n"
# stdout: "00000000a5a5aff5\n"
# stdout: "a5a5a5a5a5a5a005\n"
# stdout: "00000000a5a5a005\n"
# stdout: "a5a5a5a5a5a5aff5\n"
# stdout: "00000000a5a5aff5\n"
# stdout: "a5a5a5a5a5a5a000\n"
# stdout: "00000000a5a5a000\n"
# stdout: "a5a5a5a5a5a5a000\n"
# stdout: "00000000a5a5a000\n"
# stdout: "a5a5a5a5a5a5afff\n"
# stdout: "00000000a5a5afff\n"
# stdout: "a5a5a5a5a5a5afff\n"
# stdout: "00000000a5a5afff\n"
# stdout: "a5a5a5a5a5a5a000\n"
# stdout: "00000000a5a5a000\n"
# stdout: "a5a5a5a5a5a5afff\n"
# stdout: "00000000a5a5afff\n"
# stdout: "a5a5a5a525a5a5a5\n"
# stdout: "0000000025a5a5a5\n"
# stdout: "a5a5a5a5a5a5a5a5\n"
# stdout: "00000000a5a5a5a5\n"
# stdout: "a5a5a5a5a5a5a5a5\n"
# stdout: "00000000a5a5a5a5\n"
# stdout: "a5a5a5a5a5a5a5a5\n"
# stdout: "00000000a5a5a5a5\n"
# stdout: "a5a5a5a525a5a5a5\n"
# stdout: "0000000025a5a5a5\n"
# stdout: "a5a5a5a5a5a5a5a5\n"
# stdout: "00000000a5a5a5a5\n"
# stdout: "a5a5a5a5a5a5a505\n"
# stdout: "a5a5a5a5a5a5a515\n"
# stdout: "a5a5a5a5a5a5a5f5\n"
# stdout: "a5a5a5a5a5a5a5f5\n"
# stdout: "a5a5a5a5a5a5a505\n"
# stdout: "a5a5a5a5a5a5a5f5\n"
# stdout: "ffffffffff0000ff\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #4, #8
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #4, #8
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #4, #8
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #4, #8
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #4, #8
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #4, #8
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #4, #8
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil x0, x1, #8, #12
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil w0, w1, #8, #12
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil x0, x1, #8, #12
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil w0, w1, #8, #12
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil x0, x1, #8, #12
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil w0, w1, #8, #12
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil x0, x1, #8, #12
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil w0, w1, #8, #12
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil x0, x1, #8, #12
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil w0, w1, #8, #12
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil x0, x1, #8, #12
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfxil w0, w1, #8, #12
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #31, #1
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #31, #1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #31, #1
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #31, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #31, #1
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #31, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #31, #1
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #31, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #31, #1
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #31, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi x0, x1, #31, #1
	bl	put_hex
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfi w0, w1, #31, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfm x0, x1, #60, #3
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfm x0, x1, #60, #3
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfm x0, x1, #60, #3
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfm x0, x1, #60, #3
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfm x0, x1, #60, #3
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x0, #42405
	movk	x0, #42405, lsl #16
	movk	x0, #42405, lsl #32
	movk	x0, #42405, lsl #48
	bfm x0, x1, #60, #3
	bl	put_hex
	movz	x0, #65535
	movk	x0, #65535, lsl #16
	movk	x0, #65535, lsl #32
	movk	x0, #65535, lsl #48
	bfi	x0, xzr, #8, #16
	bl	put_hex
	b	exit0
