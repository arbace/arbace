# a64i's program (AR5): LSE compare and swap: cas, casa, casl, casal (b, h, w, x), casp
# with: lib/out.s
# exit: 0
# stdout: "1111111122222222\n"
# stdout: "0000000000003333\n"
# stdout: "1111111122222222\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000000033\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000003333\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000007\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	add	x21, x20, #32
		movz	x9, #8738
	movk	x9, #8738, lsl #16
	movk	x9, #4369, lsl #32
	movk	x9, #4369, lsl #48
	str	x9, [x21]
	mov	x1, x9
	mov	x2, #0x3333
	cas	x1, x2, [x21]
	mov	x0, x1
	bl	put_hex
	ldr	x0, [x21]
	bl	put_hex
	add	x21, x20, #32
		movz	x9, #8738
	movk	x9, #8738, lsl #16
	movk	x9, #4369, lsl #32
	movk	x9, #4369, lsl #48
	str	x9, [x21]
	mov	x1, x9
	mov	x2, #0x3333
	casa	x1, x2, [x21]
	mov	x0, x1
	bl	put_hex
	ldr	x0, [x21]
	bl	put_hex
	mov	x1, #5
	mov	x2, #6
	casl	x1, x2, [x21]
	mov	x0, x1
	bl	put_hex
	ldr	x0, [x21]
	bl	put_hex
	mov	x1, #5
	mov	x2, #6
	casal	w1, w2, [x21]
	mov	x0, x1
	bl	put_hex
	ldr	x0, [x21]
	bl	put_hex
	mov	x1, #5
	mov	x2, #6
	casb	w1, w2, [x21]
	mov	x0, x1
	bl	put_hex
	ldr	x0, [x21]
	bl	put_hex
	mov	x1, #5
	mov	x2, #6
	cash	w1, w2, [x21]
	mov	x0, x1
	bl	put_hex
	ldr	x0, [x21]
	bl	put_hex
	add	x21, x20, #48
	mov	x2, #1
	mov	x3, #2
	stp	x2, x3, [x21]
	mov	x4, #7
	mov	x5, #8
	casp	x2, x3, x4, x5, [x21]
	mov	x0, x2
	bl	put_hex
	ldp	x0, x1, [x21]
	bl	put_hex
	mov	x0, x1
	bl	put_hex
	caspal	x2, x3, x4, x5, [x21]
	mov	x0, x2
	bl	put_hex
	mov	w2, #9
	mov	w3, #9
	casp	w2, w3, w4, w5, [x21]
	mov	x0, x2
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
