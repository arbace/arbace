# a64i's program (AR5): unsigned bitfield: lsl, lsr (immediate), ubfx, ubfiz, uxtb, uxth, ubfm
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "fffffffffffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "fffffffffffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000001fffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "2468acf13579bde0\n"
# stdout: "000000003579bde0\n"
# stdout: "fdb97530eca86420\n"
# stdout: "00000000eca86420\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "4000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "4d5e6f7800000000\n"
# stdout: "0000000000000000\n"
# stdout: "3b2a190800000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "3fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "4000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "000000007fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000040000000\n"
# stdout: "0000000040000000\n"
# stdout: "091a2b3c4d5e6f78\n"
# stdout: "000000004d5e6f78\n"
# stdout: "7f6e5d4c3b2a1908\n"
# stdout: "000000003b2a1908\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000001ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "000000002468acf1\n"
# stdout: "0000000000000001\n"
# stdout: "00000001fdb97530\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ef\n"
# stdout: "00000000000000ef\n"
# stdout: "0000000000000021\n"
# stdout: "0000000000000021\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000ff0\n"
# stdout: "0000000000000ff0\n"
# stdout: "0000000000000ff0\n"
# stdout: "0000000000000ff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000ff0\n"
# stdout: "0000000000000ff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000f00\n"
# stdout: "0000000000000f00\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "000000009abcdef0\n"
# stdout: "000000009abcdef0\n"
# stdout: "0000000076543210\n"
# stdout: "0000000076543210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "2000000000000000\n"
# stdout: "0000000020000000\n"
# stdout: "e000000000000000\n"
# stdout: "00000000e0000000\n"
# stdout: "e000000000000000\n"
# stdout: "00000000e0000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "e000000000000000\n"
# stdout: "00000000e0000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000f0\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "000000000000ffff\n"
# stdout: "000000000000ffff\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000ffff\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000def0\n"
# stdout: "0000000000003210\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	lsl x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	lsl x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsl w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	lsr x0, x1, #1
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	lsr x0, x1, #31
	bl	put_hex
	mov	x0, #-1
	lsr w0, w1, #31
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ubfx x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ubfiz x0, x1, #4, #8
	bl	put_hex
	mov	x0, #-1
	ubfiz w0, w1, #4, #8
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ubfx x0, x1, #0, #32
	bl	put_hex
	mov	x0, #-1
	ubfx w0, w1, #0, #32
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ubfm x0, x1, #3, #2
	bl	put_hex
	mov	x0, #-1
	ubfm w0, w1, #3, #2
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	uxtb w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	uxth w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	uxth w0, w1
	bl	put_hex
	b	exit0
