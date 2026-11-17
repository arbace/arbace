# a64i's program (AR5): branches to registers: br through a jump table, blr, ret to a register
# with: lib/out.s
# exit: 0
# stdout: "000000000000006f\n"
# stdout: "000000000000002a\n"
# stdout: "000000000000004d\n"
	.text
	.globl	_start
_start:
	mov	x19, #0
	adr	x20, table
	mov	x21, #0
1:	ldr	x9, [x20, x21, lsl #3]
	br	x9
case0:	add	x19, x19, #1
	b	2f
case1:	add	x19, x19, #10
	b	2f
case2:	add	x19, x19, #100
2:	add	x21, x21, #1
	cmp	x21, #3
	b.ne	1b
	mov	x0, x19
	bl	put_hex
	adr	x9, fn
	blr	x9
	bl	put_hex
	adr	x10, back
	mov	x11, x10
	ret	x11
	mov	x0, #1
back:	mov	x0, #77
	bl	put_hex
	b	exit0
fn:	mov	x0, #42
	ret
	b	exit0
	.data
	.balign	8
table:	.xword	case0, case1, case2
