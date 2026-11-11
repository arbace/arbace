# a64i's program (AR5): pairs: ldp, stp (w, x), pre- and post-indexed, ldpsw, ldnp, stnp
# with: lib/out.s
# exit: 0
# stdout: "0123456789abcdef\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000000000000\n"
# stdout: "8081828384858687\n"
# stdout: "7f7e7d7c7b7a7978\n"
# stdout: "0000000000000010\n"
# stdout: "0000000084858687\n"
# stdout: "0000000080818283\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffff84858687\n"
# stdout: "ffffffff80818283\n"
# stdout: "0000000000000008\n"
# stdout: "8081828384858687\n"
# stdout: "7f7e7d7c7b7a7978\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080818283\n"
# stdout: "000000007b7a7978\n"
# stdout: "0000000000000008\n"
# stdout: "0000002200000011\n"
# stdout: "0000001100000022\n"
# stdout: "0000000000000022\n"
# stdout: "0000000000000024\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000010\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	mov x21, x20
	ldp x1, x2, [x21]
	mov	x0, x1
	bl	put_hex
	mov	x0, x2
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldp x1, x2, [x21, #16]!
	mov	x0, x1
	bl	put_hex
	mov	x0, x2
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldp w1, w2, [x21], #-8
	mov	x0, x1
	bl	put_hex
	mov	x0, x2
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldpsw x1, x2, [x21, #8]
	mov	x0, x1
	bl	put_hex
	mov	x0, x2
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldnp x1, x2, [x21, #8]
	mov	x0, x1
	bl	put_hex
	mov	x0, x2
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	ldp w1, w2, [x21, #12]
	mov	x0, x1
	bl	put_hex
	mov	x0, x2
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	mov	x21, x20
	mov	x1, #0x11
	mov	x2, #0x22
	stp	x1, x2, [x21, #32]!
	stp	w2, w1, [x21, #16]
	stnp	x2, x1, [x21, #24]
	stp	w1, w2, [x21], #4
	ldr	x0, [x20, #32]
	bl	put_hex
	ldr	x0, [x20, #48]
	bl	put_hex
	ldr	x0, [x20, #56]
	bl	put_hex
	sub	x0, x21, x20
	bl	put_hex
	mov	x3, sp
	mov	x1, #7
	mov	x2, #9
	stp	x1, x2, [sp, #-16]!
	ldp	x4, x5, [sp], #16
	mov	x6, sp
	sub	x0, x3, x6
	bl	put_hex
	add	x0, x4, x5
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
