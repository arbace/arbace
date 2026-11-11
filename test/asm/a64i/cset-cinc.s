# a64i's program (AR5): conditional aliases: cset, csetm, cinc, cinv, cneg
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffffb\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000005\n"
# stdout: "fffffffffffffffa\n"
# stdout: "fffffffffffffffb\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffffb\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "fffffffffffffffa\n"
# stdout: "fffffffffffffffb\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000005\n"
	.text
	.globl	_start
_start:
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cset x0, eq
	bl	put_hex
	csetm x0, ne
	bl	put_hex
	cinc x0, x1, mi
	bl	put_hex
	cinv x0, x1, cs
	bl	put_hex
	cneg x0, x1, vs
	bl	put_hex
	cset w0, ge
	bl	put_hex
	cneg w0, w1, lt
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cset x0, eq
	bl	put_hex
	csetm x0, ne
	bl	put_hex
	cinc x0, x1, mi
	bl	put_hex
	cinv x0, x1, cs
	bl	put_hex
	cneg x0, x1, vs
	bl	put_hex
	cset w0, ge
	bl	put_hex
	cneg w0, w1, lt
	bl	put_hex
	mov	x9, #2147483648
	msr	nzcv, x9
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cset x0, eq
	bl	put_hex
	csetm x0, ne
	bl	put_hex
	cinc x0, x1, mi
	bl	put_hex
	cinv x0, x1, cs
	bl	put_hex
	cneg x0, x1, vs
	bl	put_hex
	cset w0, ge
	bl	put_hex
	cneg w0, w1, lt
	bl	put_hex
	mov	x9, #805306368
	msr	nzcv, x9
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cset x0, eq
	bl	put_hex
	csetm x0, ne
	bl	put_hex
	cinc x0, x1, mi
	bl	put_hex
	cinv x0, x1, cs
	bl	put_hex
	cneg x0, x1, vs
	bl	put_hex
	cset w0, ge
	bl	put_hex
	cneg w0, w1, lt
	bl	put_hex
	mov	x9, #4026531840
	msr	nzcv, x9
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cset x0, eq
	bl	put_hex
	csetm x0, ne
	bl	put_hex
	cinc x0, x1, mi
	bl	put_hex
	cinv x0, x1, cs
	bl	put_hex
	cneg x0, x1, vs
	bl	put_hex
	cset w0, ge
	bl	put_hex
	cneg w0, w1, lt
	bl	put_hex
	b	exit0
