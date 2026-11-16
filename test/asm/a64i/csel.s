# a64i's program (AR5): conditional select: csel, csinc, csinv, csneg
# with: lib/out.s
# exit: 0
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000001111\n"
# stdout: "0000000000001111\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000080000000\n"
	.text
	.globl	_start
_start:
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, eq
	bl	put_hex
	csel	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, ne
	bl	put_hex
	csel	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, hs
	bl	put_hex
	csel	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, lt
	bl	put_hex
	csel	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, gt
	bl	put_hex
	csel	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, le
	bl	put_hex
	csel	w0, w1, w2, le
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, eq
	bl	put_hex
	csinc	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, ne
	bl	put_hex
	csinc	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, hs
	bl	put_hex
	csinc	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, lt
	bl	put_hex
	csinc	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, gt
	bl	put_hex
	csinc	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, le
	bl	put_hex
	csinc	w0, w1, w2, le
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, eq
	bl	put_hex
	csinv	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, ne
	bl	put_hex
	csinv	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, hs
	bl	put_hex
	csinv	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, lt
	bl	put_hex
	csinv	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, gt
	bl	put_hex
	csinv	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, le
	bl	put_hex
	csinv	w0, w1, w2, le
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, eq
	bl	put_hex
	csneg	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, ne
	bl	put_hex
	csneg	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, hs
	bl	put_hex
	csneg	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, lt
	bl	put_hex
	csneg	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, gt
	bl	put_hex
	csneg	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, le
	bl	put_hex
	csneg	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, eq
	bl	put_hex
	csel	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, ne
	bl	put_hex
	csel	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, hs
	bl	put_hex
	csel	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, lt
	bl	put_hex
	csel	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, gt
	bl	put_hex
	csel	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, le
	bl	put_hex
	csel	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, eq
	bl	put_hex
	csinc	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, ne
	bl	put_hex
	csinc	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, hs
	bl	put_hex
	csinc	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, lt
	bl	put_hex
	csinc	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, gt
	bl	put_hex
	csinc	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, le
	bl	put_hex
	csinc	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, eq
	bl	put_hex
	csinv	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, ne
	bl	put_hex
	csinv	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, hs
	bl	put_hex
	csinv	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, lt
	bl	put_hex
	csinv	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, gt
	bl	put_hex
	csinv	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, le
	bl	put_hex
	csinv	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, eq
	bl	put_hex
	csneg	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, ne
	bl	put_hex
	csneg	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, hs
	bl	put_hex
	csneg	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, lt
	bl	put_hex
	csneg	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, gt
	bl	put_hex
	csneg	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, le
	bl	put_hex
	csneg	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, eq
	bl	put_hex
	csel	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, ne
	bl	put_hex
	csel	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, hs
	bl	put_hex
	csel	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, lt
	bl	put_hex
	csel	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, gt
	bl	put_hex
	csel	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, le
	bl	put_hex
	csel	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, eq
	bl	put_hex
	csinc	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, ne
	bl	put_hex
	csinc	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, hs
	bl	put_hex
	csinc	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, lt
	bl	put_hex
	csinc	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, gt
	bl	put_hex
	csinc	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, le
	bl	put_hex
	csinc	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, eq
	bl	put_hex
	csinv	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, ne
	bl	put_hex
	csinv	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, hs
	bl	put_hex
	csinv	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, lt
	bl	put_hex
	csinv	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, gt
	bl	put_hex
	csinv	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, le
	bl	put_hex
	csinv	w0, w1, w2, le
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, eq
	bl	put_hex
	csneg	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, ne
	bl	put_hex
	csneg	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, hs
	bl	put_hex
	csneg	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, lt
	bl	put_hex
	csneg	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, gt
	bl	put_hex
	csneg	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, le
	bl	put_hex
	csneg	w0, w1, w2, le
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, eq
	bl	put_hex
	csel	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, ne
	bl	put_hex
	csel	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, hs
	bl	put_hex
	csel	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, lt
	bl	put_hex
	csel	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, gt
	bl	put_hex
	csel	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csel	x0, x1, x2, le
	bl	put_hex
	csel	w0, w1, w2, le
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, eq
	bl	put_hex
	csinc	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, ne
	bl	put_hex
	csinc	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, hs
	bl	put_hex
	csinc	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, lt
	bl	put_hex
	csinc	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, gt
	bl	put_hex
	csinc	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinc	x0, x1, x2, le
	bl	put_hex
	csinc	w0, w1, w2, le
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, eq
	bl	put_hex
	csinv	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, ne
	bl	put_hex
	csinv	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, hs
	bl	put_hex
	csinv	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, lt
	bl	put_hex
	csinv	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, gt
	bl	put_hex
	csinv	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csinv	x0, x1, x2, le
	bl	put_hex
	csinv	w0, w1, w2, le
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, eq
	bl	put_hex
	csneg	w0, w1, w2, eq
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, ne
	bl	put_hex
	csneg	w0, w1, w2, ne
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, hs
	bl	put_hex
	csneg	w0, w1, w2, hs
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, lt
	bl	put_hex
	csneg	w0, w1, w2, lt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, gt
	bl	put_hex
	csneg	w0, w1, w2, gt
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	movz	x1, #4369
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	csneg	x0, x1, x2, le
	bl	put_hex
	csneg	w0, w1, w2, le
	bl	put_hex
	b	exit0
