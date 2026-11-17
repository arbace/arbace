# a64i's program (AR5): compare and branch: cbz, cbnz on x and w registers
# with: lib/out.s
# exit: 0
# stdout: "000000000000000a\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000009\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000005\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	cbz	x1, 1f
	orr	x0, x0, #1
1:	cbnz	x1, 2f
	orr	x0, x0, #2
2:	cbz	w1, 3f
	orr	x0, x0, #4
3:	cbnz	w1, 4f
	orr	x0, x0, #8
4:
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	cbz	x1, 1f
	orr	x0, x0, #1
1:	cbnz	x1, 2f
	orr	x0, x0, #2
2:	cbz	w1, 3f
	orr	x0, x0, #4
3:	cbnz	w1, 4f
	orr	x0, x0, #8
4:
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #1, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	cbz	x1, 1f
	orr	x0, x0, #1
1:	cbnz	x1, 2f
	orr	x0, x0, #2
2:	cbz	w1, 3f
	orr	x0, x0, #4
3:	cbnz	w1, 4f
	orr	x0, x0, #8
4:
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	cbz	x1, 1f
	orr	x0, x0, #1
1:	cbnz	x1, 2f
	orr	x0, x0, #2
2:	cbz	w1, 3f
	orr	x0, x0, #4
3:	cbnz	w1, 4f
	orr	x0, x0, #8
4:
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	mov	x0, #0
	cbz	x1, 1f
	orr	x0, x0, #1
1:	cbnz	x1, 2f
	orr	x0, x0, #2
2:	cbz	w1, 3f
	orr	x0, x0, #4
3:	cbnz	w1, 4f
	orr	x0, x0, #8
4:
	bl	put_hex
	b	exit0
