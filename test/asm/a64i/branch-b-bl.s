# a64i's program (AR5): b and bl: forward, backward, calls and returns (x30)
# with: lib/out.s
# exit: 0
# stdout: "0000000000000069\n"
# stdout: "000000000000000e\n"
	.text
	.globl	_start
_start:
	mov	x19, #0
	b	2f
1:	add	x19, x19, #1
	cmp	x19, #105
	b.ne	1b
	b	3f
2:	mov	x19, #100
	b	1b
3:	mov	x0, x19
	bl	put_hex
	mov	x19, #0
	bl	f1
	bl	f1
	bl	f2
	mov	x0, x19
	bl	put_hex
	b	exit0
f1:	add	x19, x19, #1
	ret
f2:	mov	x20, x30
	bl	f1
	bl	f1
	mov	x30, x20
	add	x19, x19, #10
	ret
	b	exit0
