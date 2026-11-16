# a64i's program (AR5): mov between registers, to and from SP, w registers zeroing the upper half
# with: lib/out.s
# exit: 0
# stdout: "123456789abcdef0\n"
# stdout: "000000009abcdef0\n"
# stdout: "0000000000000040\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
	.text
	.globl	_start
_start:
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	mov	x0, x1
	bl	put_hex
	mov	x0, #-1
	mov	w0, w1
	bl	put_hex
	mov	x5, sp
	mov	x6, #-64
	add	x6, x5, x6
	mov	sp, x6
	mov	x0, sp
	sub	x0, x5, x0
	bl	put_hex
	mov	sp, x5
	mov	x0, #7
	mov	w0, wzr
	bl	put_hex
	mov	x0, xzr
	bl	put_hex
	mov	x0, #-1
	mov	w1, w0
	mov	x0, x1
	bl	put_hex
	b	exit0
