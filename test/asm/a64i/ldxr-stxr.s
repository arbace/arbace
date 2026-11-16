# a64i's program (AR5): exclusives: ldxr, stxr (b, h, w, x) in a retry loop, a failing stxr after clrex
# with: lib/out.s
# exit: 0
# stdout: "0123456789abcdf2\n"
# stdout: "0123456789abcdf5\n"
# stdout: "0123456789abcdf8\n"
# stdout: "0123456789abcdfb\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
1:	ldxr	x1, [x20]
	add	x1, x1, #3
	stxr	w2, x1, [x20]
	cbnz	w2, 1b
	ldr	x0, [x20]
	bl	put_hex
1:	ldxr	w1, [x20]
	add	w1, w1, #3
	stxr	w2, w1, [x20]
	cbnz	w2, 1b
	ldr	x0, [x20]
	bl	put_hex
1:	ldxrb	w1, [x20]
	add	w1, w1, #3
	stxrb	w2, w1, [x20]
	cbnz	w2, 1b
	ldr	x0, [x20]
	bl	put_hex
1:	ldxrh	w1, [x20]
	add	w1, w1, #3
	stxrh	w2, w1, [x20]
	cbnz	w2, 1b
	ldr	x0, [x20]
	bl	put_hex
	ldxr	x1, [x20]
	clrex
	stxr	w2, x1, [x20]
	mov	w0, w2
	bl	put_hex
	add	x21, x20, #8
	stxr	w2, x1, [x21]
	mov	w0, w2
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
