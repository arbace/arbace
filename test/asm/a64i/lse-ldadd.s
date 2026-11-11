# a64i's program (AR5): LSE atomics: ldadd, ldadda, ldaddl, ldaddal (b, h, w, x), stadd
# with: lib/out.s
# exit: 0
# stdout: "7f00ff00c3c38181\n"
# stdout: "ff00ff00c3c38182\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0043c38182\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "7f00ff00c3c38181\n"
# stdout: "ff00ff00c3c38182\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0043c38182\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "7f00ff00c3c38181\n"
# stdout: "ff00ff00c3c38182\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0043c38182\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "7f00ff00c3c38181\n"
# stdout: "ff00ff00c3c38182\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0043c38182\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38182\n"
# stdout: "7f00ff00c3c3818c\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldadd	x1, x0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldadd	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddh	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddb	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldadda	x1, x0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldadda	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddah	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddab	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddl	x1, x0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddl	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddlh	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddlb	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddal	x1, x0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddal	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddalh	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	movz	x1, #1
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	str	xzr, [x20, #32]
		movz	x9, #33153
	movk	x9, #50115, lsl #16
	movk	x9, #65280, lsl #32
	movk	x9, #32512, lsl #48
	str	x9, [x20, #32]
	add	x21, x20, #32
	ldaddalb	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	mov	x1, #5
	stadd	x1, [x21]
	staddl	x1, [x21]
	ldr	x0, [x21]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
