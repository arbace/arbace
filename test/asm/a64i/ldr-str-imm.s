# a64i's program (AR5): loads and stores, unsigned offsets: ldr, str, ldrb, strb, ldrh, strh (w and x)
# with: lib/out.s
# exit: 0
# stdout: "0123456789abcdef\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000001234567\n"
# stdout: "0000000000000089\n"
# stdout: "0000000000000123\n"
# stdout: "7f7e7d7c7b7a7978\n"
# stdout: "ffffffffffff00ff\n"
# stdout: "ffff0000ffffffff\n"
# stdout: "ffffffff00000000\n"
# stdout: "0000000000000000\n"
# stdout: "5566778877880088\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	ldr x0, [x20]
	bl	put_hex
	ldr x0, [x20, #8]
	bl	put_hex
	ldr w0, [x20, #4]
	bl	put_hex
	ldrb w0, [x20, #3]
	bl	put_hex
	ldrh w0, [x20, #6]
	bl	put_hex
	ldr x0, [x20, #24]
	bl	put_hex
	mov	x0, #-1
	str	x0, [x20, #32]
	strb wzr, [x20, #33]
	ldr	x0, [x20, #32]
	bl	put_hex
	mov	x0, #-1
	str	x0, [x20, #32]
	strh wzr, [x20, #36]
	ldr	x0, [x20, #32]
	bl	put_hex
	mov	x0, #-1
	str	x0, [x20, #32]
	str wzr, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	mov	x0, #-1
	str	x0, [x20, #32]
	str xzr, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #30600
	movk	x1, #21862, lsl #16
	movk	x1, #13124, lsl #32
	movk	x1, #4386, lsl #48
	str	x1, [x20, #40]
	strb	w1, [x20, #48]
	strh	w1, [x20, #50]
	str	w1, [x20, #52]
	ldr	x0, [x20, #48]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
