# a64i's program (AR5): LSE atomics: swp, swpa, swpl, swpal
# with: lib/out.s
# exit: 0
# stdout: "7f00ff00c3c38181\n"
# stdout: "8000000000000001\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0080000001\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c30001\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38101\n"
# stdout: "7f00ff00c3c38181\n"
# stdout: "8000000000000001\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0080000001\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c30001\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38101\n"
# stdout: "7f00ff00c3c38181\n"
# stdout: "8000000000000001\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0080000001\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c30001\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38101\n"
# stdout: "7f00ff00c3c38181\n"
# stdout: "8000000000000001\n"
# stdout: "00000000c3c38181\n"
# stdout: "7f00ff0080000001\n"
# stdout: "0000000000008181\n"
# stdout: "7f00ff00c3c30001\n"
# stdout: "0000000000000081\n"
# stdout: "7f00ff00c3c38101\n"
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
	swp	x1, x0, [x21]
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
	swp	w1, w0, [x21]
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
	swph	w1, w0, [x21]
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
	swpb	w1, w0, [x21]
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
	swpa	x1, x0, [x21]
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
	swpa	w1, w0, [x21]
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
	swpah	w1, w0, [x21]
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
	swpab	w1, w0, [x21]
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
	swpl	x1, x0, [x21]
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
	swpl	w1, w0, [x21]
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
	swplh	w1, w0, [x21]
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
	swplb	w1, w0, [x21]
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
	swpal	x1, x0, [x21]
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
	swpal	w1, w0, [x21]
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
	swpalh	w1, w0, [x21]
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
	swpalb	w1, w0, [x21]
	bl	put_hex
	ldr	x0, [x20, #32]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
