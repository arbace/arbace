# a64i's program (AR5): compare: cmp, cmn (immediate, shifted, extended), the flags
# with: lib/out.s
# exit: 0
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "000000000000000a\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000009\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000003\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "000000000000000a\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000009\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000055\n"
# stdout: "000000000000000a\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "000000000000000a\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "000000000000000a\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000003\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
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
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, x2, lsl #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, w2, lsl #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmn x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmn w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	cmp x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	cmp w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	b	exit0
