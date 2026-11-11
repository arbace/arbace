# a64i's program (AR5): load-acquire, store-release: ldar, stlr (b, h, w, x), ldapr
# with: lib/out.s
# exit: 0
# stdout: "0123456789abcdef\n"
# stdout: "0000000089abcdef\n"
# stdout: "00000000000000ef\n"
# stdout: "000000000000cdef\n"
# stdout: "0123456789abcdef\n"
# stdout: "00000000ffff0000\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	ldar x0, [x20]
	bl	put_hex
	ldar w0, [x20]
	bl	put_hex
	ldarb w0, [x20]
	bl	put_hex
	ldarh w0, [x20]
	bl	put_hex
	ldapr x0, [x20]
	bl	put_hex
	mov	x1, #-2
	add	x21, x20, #32
	stlr	x1, [x21]
	stlrb	wzr, [x21]
	stlrh	wzr, [x21, #0]
	add	x22, x21, #4
	stlr	wzr, [x22]
	ldr	x0, [x21]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
