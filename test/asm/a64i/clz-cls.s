# a64i's program (AR5): count leading: clz, cls
# with: lib/out.s
# exit: 0
# stdout: "0000000000000040\n"
# stdout: "0000000000000020\n"
# stdout: "000000000000003f\n"
# stdout: "000000000000001f\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000020\n"
# stdout: "0000000000000020\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000020\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000020\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000003e\n"
# stdout: "000000000000001e\n"
# stdout: "000000000000003f\n"
# stdout: "000000000000001f\n"
# stdout: "000000000000003e\n"
# stdout: "000000000000001e\n"
# stdout: "000000000000003f\n"
# stdout: "000000000000001f\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000001f\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000001f\n"
# stdout: "000000000000001f\n"
# stdout: "000000000000001f\n"
# stdout: "000000000000001f\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "000000000000001f\n"
# stdout: "000000000000000f\n"
# stdout: "000000000000001f\n"
# stdout: "000000000000003d\n"
# stdout: "000000000000001d\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #255, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #32767, lsl #32
	movk	x1, #65535, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	clz x0, x1
	bl	put_hex
	mov	x0, #-1
	clz w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #255, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #32767, lsl #32
	movk	x1, #65535, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	cls x0, x1
	bl	put_hex
	mov	x0, #-1
	cls w0, w1
	bl	put_hex
	b	exit0
