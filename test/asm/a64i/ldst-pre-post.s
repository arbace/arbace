# a64i's program (AR5): pre- and post-indexed loads and stores: the base written back
# with: lib/out.s
# exit: 0
# stdout: "fedcba9876543210\n"
# stdout: "0000000000000008\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000000000010\n"
# stdout: "00000000000000fe\n"
# stdout: "000000000000000f\n"
# stdout: "00000000000087fe\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fedcba98\n"
# stdout: "000000000000000c\n"
# stdout: "000000000000002d\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	mov	x21, x20
	ldr x0, [x21, #8]!
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldr x0, [x21], #8
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldrb w0, [x21, #-1]!
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldrh w0, [x21], #-7
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldr w0, [x21, #4]!
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	mov	x22, #3
	str	x22, [x21, #32]!
	strb	w22, [x21], #1
	sub	x0, x21, x20
	bl	put_hex
	ldr	x0, [x20, #36]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
