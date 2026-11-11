# a64i's program (AR5): extract: extr, ror (immediate)
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "7fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000040000000\n"
# stdout: "0000000040000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000003ffffffff\n"
# stdout: "0000000000000003\n"
# stdout: "fffffffe00000000\n"
# stdout: "00000000fffffffe\n"
# stdout: "fffffffe00000000\n"
# stdout: "00000000fffffffe\n"
# stdout: "00000001ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "fffffffe00000000\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "bfffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "4000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "800000007fffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000040000000\n"
# stdout: "0000000040000000\n"
# stdout: "091a2b3c4d5e6f78\n"
# stdout: "000000004d5e6f78\n"
# stdout: "7f6e5d4c3b2a1908\n"
# stdout: "000000003b2a1908\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000800000000000\n"
# stdout: "0000000000008000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffbfffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000400000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffff800000007fff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000004000\n"
# stdout: "0000000000004000\n"
# stdout: "6f78091a2b3c4d5e\n"
# stdout: "000000006f784d5e\n"
# stdout: "19087f6e5d4c3b2a\n"
# stdout: "0000000019083b2a\n"
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #0
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #0
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #1
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #1
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
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
	extr x0, x1, x2, #31
	bl	put_hex
	mov	x0, #-1
	extr w0, w1, w2, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ror x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ror x0, x1, #17
	bl	put_hex
	mov	x0, #-1
	ror w0, w1, #17
	bl	put_hex
	b	exit0
