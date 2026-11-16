# Arbace's run test (AR4): loops: 1 + ... + 10 by a backward conditional branch, a CBNZ countdown and a TBZ forward test
# exit: 0
# stdout: "55\n55\n1\n"
# with: lib/print.s
	.globl	_start
_start:
	mov	x19, #0
	mov	x1, #1
1:	add	x19, x19, x1
	add	x1, x1, #1
	cmp	x1, #10
	b.le	1b
	mov	x0, x19
	bl	put_u64
	mov	x2, #10
	mov	x0, #0
2:	add	x0, x0, x2
	sub	x2, x2, #1
	cbnz	x2, 2b
	bl	put_u64
	mov	x0, #4
	mov	x1, #1
	tbz	x0, #1, 3f
	mov	x1, #9
3:	mov	x0, x1
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
