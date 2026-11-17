# a64i's program (AR5): variable shifts: lsl, lsr, asr, ror by register (lslv...), amounts beyond the size
# with: lib/out.s
# exit: 0
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000001\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "4000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "4000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "c000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffff00000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "c000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "c000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000300000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000180000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "c000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000001\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "c000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000300000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000180000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "c000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000002\n"
	.text
	.globl	_start
_start:
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #31
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #32
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #63
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #64
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	lsl x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #31
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #32
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #63
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #64
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	lsr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #31
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #32
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #63
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #64
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	asr x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	asr w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #31
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #32
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #63
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #64
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	ror x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #31
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #32
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #63
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #64
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	lslv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	lslv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #31
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #32
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #63
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #64
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	rorv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	rorv w0, w1, w2
	bl	put_hex
	b	exit0
