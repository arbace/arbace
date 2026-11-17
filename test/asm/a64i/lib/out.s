// a64i's programs' library (AR5): output by write(2), the end by exit_group(2); every register but x30 kept
	.text
	.globl	put_hex
	.globl	put_nzcv
	.globl	put_str
	.globl	exit0
	.globl	exit_x0
// put_hex: x0 as 16 lower-case hexadecimal digits and a newline on stdout
put_hex:
	stp	x29, x30, [sp, #-80]!
	mov	x29, sp
	stp	x0, x1, [sp, #16]
	stp	x2, x3, [sp, #32]
	stp	x4, x8, [sp, #48]
	mrs	x4, nzcv
	str	x4, [sp, #64]
	mov	x2, #15
	mov	x1, x0
	sub	sp, sp, #32
1:	and	x3, x1, #15
	cmp	x3, #10
	add	x4, x3, #48
	b.lo	2f
	add	x4, x3, #87
2:	strb	w4, [sp, x2]
	lsr	x1, x1, #4
	subs	x2, x2, #1
	b.ge	1b
	mov	w4, #10
	strb	w4, [sp, #16]
	mov	x0, #1
	mov	x1, sp
	mov	x2, #17
	mov	x8, #64
	svc	#0
	add	sp, sp, #32
	ldr	x4, [sp, #64]
	msr	nzcv, x4
	ldp	x4, x8, [sp, #48]
	ldp	x2, x3, [sp, #32]
	ldp	x0, x1, [sp, #16]
	ldp	x29, x30, [sp], #80
	ret
// put_nzcv: the flags N Z C V as the hexadecimal digit NZCV, a line
put_nzcv:
	stp	x29, x30, [sp, #-32]!
	str	x0, [sp, #16]
	mrs	x0, nzcv
	lsr	x0, x0, #28
	bl	put_hex
	ldr	x0, [sp, #16]
	ldp	x29, x30, [sp], #32
	ret
// put_str: the x1 bytes at x0 on stdout
put_str:
	stp	x29, x30, [sp, #-48]!
	stp	x0, x1, [sp, #16]
	stp	x2, x8, [sp, #32]
	mov	x2, x1
	mov	x1, x0
	mov	x0, #1
	mov	x8, #64
	svc	#0
	ldp	x2, x8, [sp, #32]
	ldp	x0, x1, [sp, #16]
	ldp	x29, x30, [sp], #48
	ret
exit0:
	mov	x0, #0
exit_x0:
	mov	x8, #94
	svc	#0
