# a64i's program (AR5): SIMD&FP registers' loads and stores: ldr/str of b, h, s, d, q, ldp/stp, the rest zeroed
# with: lib/out.s
# exit: 0
# stdout: "8081828384858687\n"
# stdout: "7f7e7d7c7b7a7978\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000001234567\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000089ab\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000cd\n"
# stdout: "0000000000000000\n"
# stdout: "5432100123456789\n"
# stdout: "858687fedcba9876\n"
# stdout: "8081828384858687\n"
# stdout: "fedcba9876543210\n"
# stdout: "8081828384858687\n"
# stdout: "0000000000000002\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	ldr	q0, [x20]
	ldr q1, [x20, #16]
	str	q1, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	ldr	q0, [x20]
	ldr d1, [x20, #8]
	str	q1, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	ldr	q0, [x20]
	ldr s1, [x20, #4]
	str	q1, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	ldr	q0, [x20]
	ldr h1, [x20, #2]
	str	q1, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	ldr	q0, [x20]
	ldr b1, [x20, #1]
	str	q1, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	ldr	q0, [x20]
	ldur q1, [x20, #3]
	str	q1, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #40]
	bl	put_hex
	ldp	q2, q3, [x20]
	stp	q3, q2, [x20, #32]
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #56]
	bl	put_hex
	ldp	d4, d5, [x20, #8]!
	str	d5, [x20, #-8]
	ldr	x0, [x20, #-8]
	bl	put_hex
	sub	x20, x20, #8
	ldr	q6, lit
	str	q6, [x20, #48]
	ldr	x0, [x20, #56]
	bl	put_hex
	b	exit0
	.balign	16
lit:	.xword	1, 2
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
