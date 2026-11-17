# a64i's program (AR5): signed bitfield: asr (immediate), sbfx, sbfiz, sxtb, sxth, sxtw, sbfm
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "3fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "c000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "000000007fffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000040000000\n"
# stdout: "00000000c0000000\n"
# stdout: "091a2b3c4d5e6f78\n"
# stdout: "00000000cd5e6f78\n"
# stdout: "ff6e5d4c3b2a1908\n"
# stdout: "000000003b2a1908\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffff00000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "00000000ffffffff\n"
# stdout: "000000002468acf1\n"
# stdout: "00000000ffffffff\n"
# stdout: "fffffffffdb97530\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffef\n"
# stdout: "00000000ffffffef\n"
# stdout: "0000000000000021\n"
# stdout: "0000000000000021\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000010\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffff00\n"
# stdout: "00000000ffffff00\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "fffffffffffffff8\n"
# stdout: "00000000fffffff8\n"
# stdout: "fffffffffffffff9\n"
# stdout: "00000000fffffff9\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0800000000000000\n"
# stdout: "0000000008000000\n"
# stdout: "f800000000000000\n"
# stdout: "00000000f8000000\n"
# stdout: "f800000000000000\n"
# stdout: "00000000f8000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "f800000000000000\n"
# stdout: "00000000f8000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffff0\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffdef0\n"
# stdout: "0000000000003210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffff80000000\n"
# stdout: "ffffffff9abcdef0\n"
# stdout: "0000000076543210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffdef0\n"
# stdout: "0000000000003210\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	asr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	asr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sbfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sbfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	sbfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sbfx x0, x1, #28, #4
	bl	put_hex
	mov	x0, #-1
	sbfx w0, w1, #28, #4
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sbfm x0, x1, #5, #2
	bl	put_hex
	mov	x0, #-1
	sbfm w0, w1, #5, #2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sxtb x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sxth x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sxtw x0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sxtb w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	sxth w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	sxth w0, w1
	bl	put_hex
	b	exit0
