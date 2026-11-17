# a64i's program (AR5): division: udiv, sdiv, by zero, the overflow
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "000000000000000e\n"
# stdout: "000000000000000e\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "fffffffffffffff1\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000000e\n"
# stdout: "000000000000000e\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #7
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #2
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #100
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #7
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	udiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	udiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #7
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #2
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	movz	x1, #100
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #7
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sdiv x0, x1, x2
	bl	put_hex
	mov	x0, #-1
	sdiv w0, w1, w2
	bl	put_hex
	b	exit0
