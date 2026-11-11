# a64i's program (AR5): conditional compare: ccmp, ccmn (register, immediate), taken and not
# with: lib/out.s
# exit: 0
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000000f\n"
# stdout: "000000000000000f\n"
# stdout: "000000000000000f\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "000000000000000a\n"
# stdout: "000000000000000a\n"
# stdout: "000000000000000a\n"
# stdout: "000000000000000f\n"
# stdout: "000000000000000f\n"
# stdout: "000000000000000f\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
	.text
	.globl	_start
_start:
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, x2, #5, eq
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn x1, x2, #10, ne
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp x1, #3, #15, hs
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmn w1, #31, #0, cc
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	ccmp w1, w2, #2, mi
	bl	put_nzcv
	b	exit0
