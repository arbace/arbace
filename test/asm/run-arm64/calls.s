# Arbace's run test (AR4): recursion with frames (stp/ldp, bl, ret) and a call through a register (blr)
# exit: 120
# stdout: "120\n"
# with: lib/print.s
	.globl	_start
_start:
	mov	x0, #5
	bl	fact
	mov	x19, x0
	adr	x9, put_u64
	blr	x9
	mov	x0, x19
	bl	sys_exit
fact:
	stp	x29, x30, [sp, #-32]!
	mov	x29, sp
	str	x19, [sp, #16]
	mov	x19, x0
	cmp	x0, #1
	b.le	1f
	sub	x0, x0, #1
	bl	fact
	mul	x0, x0, x19
	b	2f
1:	mov	x0, #1
2:	ldr	x19, [sp, #16]
	ldp	x29, x30, [sp], #32
	ret
