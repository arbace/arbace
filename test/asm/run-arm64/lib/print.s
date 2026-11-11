# the arm64 run tests' helpers (AR4): put_u64 (x0 in decimal and a newline), put_str (x0 a NUL-terminated string), sys_exit (x0)
	.globl	put_u64
	.globl	put_str
	.globl	sys_exit
	.text
put_u64:
	sub	sp, sp, #32
	add	x1, sp, #31
	mov	w2, #10
	strb	w2, [x1]
	mov	x3, #10
1:	udiv	x4, x0, x3
	msub	x5, x4, x3, x0
	add	w5, w5, #48
	sub	x1, x1, #1
	strb	w5, [x1]
	mov	x0, x4
	cbnz	x0, 1b
	add	x2, sp, #32
	sub	x2, x2, x1
	mov	x0, #1
	mov	x8, #64
	svc	#0
	add	sp, sp, #32
	ret
put_str:
	mov	x1, x0
	mov	x2, #0
1:	ldrb	w3, [x1, x2]
	cbz	w3, 2f
	add	x2, x2, #1
	b	1b
2:	mov	x0, #1
	mov	x8, #64
	svc	#0
	ret
sys_exit:
	mov	x8, #93
	svc	#0
