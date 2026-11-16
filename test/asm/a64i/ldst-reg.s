# a64i's program (AR5): register offsets: lsl, uxtw, sxtw, sxtx, scaled and not
# with: lib/out.s
# exit: 0
# stdout: "fedcba9876543210\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000076543210\n"
# stdout: "0000000000000089\n"
# stdout: "0000000000000123\n"
# stdout: "8081828384858687\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000076543210\n"
# stdout: "0123666789abcdef\n"
# stdout: "0000000000000066\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	mov	x1, #1
	mov	x2, #-1
	ldr x0, [x20, x1, lsl #3]
	bl	put_hex
	mov	x1, #8
	mov	x2, #-1
	ldr x0, [x20, x1]
	bl	put_hex
	mov	x1, #2
	mov	x2, #-1
	ldr w0, [x20, w1, uxtw #2]
	bl	put_hex
	mov	x1, #3
	mov	x2, #-1
	ldrb w0, [x20, x1]
	bl	put_hex
	mov	x1, #3
	mov	x2, #-1
	ldrh w0, [x20, w1, sxtw #1]
	bl	put_hex
	mov	x1, #2
	mov	x2, #-1
	ldr x0, [x20, x1, sxtx #3]
	bl	put_hex
	add	x21, x20, #16
	mov	x2, #-1
	ldr	x0, [x21, x2, sxtx #3]
	bl	put_hex
	add	x21, x20, #16
	mov	w2, #-2
	ldr	w0, [x21, w2, sxtw #2]
	bl	put_hex
	mov	x1, #5
	mov	x3, #0x66
	strb	w3, [x20, x1]
	str	x3, [x20, x1, lsl #3]
	ldr	x0, [x20]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
