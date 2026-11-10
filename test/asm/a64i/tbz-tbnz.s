# a64i's program (AR5): test bit and branch: tbz, tbnz on bits 0, 31, 32, 63
# with: lib/out.s
# exit: 0
# stdout: "0000000000001f00\n"
# stdout: "0000000000001e01\n"
# stdout: "0000000000001b04\n"
# stdout: "0000000000001708\n"
# stdout: "0000000000000d12\n"
# stdout: "000000000000001f\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	tbz	x1, #0, 1f
	orr	x0, x0, #1
1:	tbnz	w1, #0, 2f
	orr	x0, x0, #256
2:
	tbz	x1, #5, 1f
	orr	x0, x0, #2
1:	tbnz	w1, #5, 2f
	orr	x0, x0, #512
2:
	tbz	x1, #31, 1f
	orr	x0, x0, #4
1:	tbnz	w1, #31, 2f
	orr	x0, x0, #1024
2:
	tbz	x1, #32, 1f
	orr	x0, x0, #8
1:	tbnz	x1, #32, 2f
	orr	x0, x0, #2048
2:
	tbz	x1, #63, 1f
	orr	x0, x0, #16
1:	tbnz	x1, #63, 2f
	orr	x0, x0, #4096
2:
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	tbz	x1, #0, 1f
	orr	x0, x0, #1
1:	tbnz	w1, #0, 2f
	orr	x0, x0, #256
2:
	tbz	x1, #5, 1f
	orr	x0, x0, #2
1:	tbnz	w1, #5, 2f
	orr	x0, x0, #512
2:
	tbz	x1, #31, 1f
	orr	x0, x0, #4
1:	tbnz	w1, #31, 2f
	orr	x0, x0, #1024
2:
	tbz	x1, #32, 1f
	orr	x0, x0, #8
1:	tbnz	x1, #32, 2f
	orr	x0, x0, #2048
2:
	tbz	x1, #63, 1f
	orr	x0, x0, #16
1:	tbnz	x1, #63, 2f
	orr	x0, x0, #4096
2:
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	tbz	x1, #0, 1f
	orr	x0, x0, #1
1:	tbnz	w1, #0, 2f
	orr	x0, x0, #256
2:
	tbz	x1, #5, 1f
	orr	x0, x0, #2
1:	tbnz	w1, #5, 2f
	orr	x0, x0, #512
2:
	tbz	x1, #31, 1f
	orr	x0, x0, #4
1:	tbnz	w1, #31, 2f
	orr	x0, x0, #1024
2:
	tbz	x1, #32, 1f
	orr	x0, x0, #8
1:	tbnz	x1, #32, 2f
	orr	x0, x0, #2048
2:
	tbz	x1, #63, 1f
	orr	x0, x0, #16
1:	tbnz	x1, #63, 2f
	orr	x0, x0, #4096
2:
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #1, lsl #32
	movk	x1, #0, lsl #48
	mov	x0, #0
	tbz	x1, #0, 1f
	orr	x0, x0, #1
1:	tbnz	w1, #0, 2f
	orr	x0, x0, #256
2:
	tbz	x1, #5, 1f
	orr	x0, x0, #2
1:	tbnz	w1, #5, 2f
	orr	x0, x0, #512
2:
	tbz	x1, #31, 1f
	orr	x0, x0, #4
1:	tbnz	w1, #31, 2f
	orr	x0, x0, #1024
2:
	tbz	x1, #32, 1f
	orr	x0, x0, #8
1:	tbnz	x1, #32, 2f
	orr	x0, x0, #2048
2:
	tbz	x1, #63, 1f
	orr	x0, x0, #16
1:	tbnz	x1, #63, 2f
	orr	x0, x0, #4096
2:
	bl	put_hex
	movz	x1, #32
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	mov	x0, #0
	tbz	x1, #0, 1f
	orr	x0, x0, #1
1:	tbnz	w1, #0, 2f
	orr	x0, x0, #256
2:
	tbz	x1, #5, 1f
	orr	x0, x0, #2
1:	tbnz	w1, #5, 2f
	orr	x0, x0, #512
2:
	tbz	x1, #31, 1f
	orr	x0, x0, #4
1:	tbnz	w1, #31, 2f
	orr	x0, x0, #1024
2:
	tbz	x1, #32, 1f
	orr	x0, x0, #8
1:	tbnz	x1, #32, 2f
	orr	x0, x0, #2048
2:
	tbz	x1, #63, 1f
	orr	x0, x0, #16
1:	tbnz	x1, #63, 2f
	orr	x0, x0, #4096
2:
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	mov	x0, #0
	tbz	x1, #0, 1f
	orr	x0, x0, #1
1:	tbnz	w1, #0, 2f
	orr	x0, x0, #256
2:
	tbz	x1, #5, 1f
	orr	x0, x0, #2
1:	tbnz	w1, #5, 2f
	orr	x0, x0, #512
2:
	tbz	x1, #31, 1f
	orr	x0, x0, #4
1:	tbnz	w1, #31, 2f
	orr	x0, x0, #1024
2:
	tbz	x1, #32, 1f
	orr	x0, x0, #8
1:	tbnz	x1, #32, 2f
	orr	x0, x0, #2048
2:
	tbz	x1, #63, 1f
	orr	x0, x0, #16
1:	tbnz	x1, #63, 2f
	orr	x0, x0, #4096
2:
	bl	put_hex
	b	exit0
