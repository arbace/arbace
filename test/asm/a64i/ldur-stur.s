# a64i's program (AR5): unscaled offsets: ldur, stur, negative and unaligned
# with: lib/out.s
# exit: 0
# stdout: "fedcba9876543210\n"
# stdout: "5432100123456789\n"
# stdout: "0000000083848586\n"
# stdout: "00000000000000fe\n"
# stdout: "000000000000dcba\n"
# stdout: "0123456789abcdef\n"
# stdout: "5432100123456789\n"
# stdout: "a1a2a3a4a5a6a7a8\n"
# stdout: "a8a80000000000a1\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	add	x21, x20, #16
	ldur x0, [x21, #-8]
	bl	put_hex
	ldur x0, [x21, #-13]
	bl	put_hex
	ldur w0, [x21, #1]
	bl	put_hex
	ldurb w0, [x21, #-1]
	bl	put_hex
	ldurh w0, [x21, #-3]
	bl	put_hex
	ldr x0, [x21, #-16]
	bl	put_hex
	ldr x0, [x20, #3]
	bl	put_hex
	movz	x1, #42920
	movk	x1, #42406, lsl #16
	movk	x1, #41892, lsl #32
	movk	x1, #41378, lsl #48
	stur	x1, [x21, #17]
	sturh	w1, [x21, #30]
	sturb	w1, [x21, #31]
	ldur	x0, [x21, #17]
	bl	put_hex
	ldur	x0, [x21, #24]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
