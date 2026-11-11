# Arbace's run test (AR4): the data relocations: ABS32 and ABS16 of an absolute symbol and of a label, PREL64, PREL32 and
# PREL16 to a label of another section (each read back and added to its own address), ADR_PREL_LO21, LD_PREL_LO19
# exit: 0
# stdout: "4660\n4660\n1\n1\n1\n1\n1\n42\n"
# with: lib/print.s
	.globl	_start
	absv = 0x1234
_start:
	adrp	x1, tab
	add	x1, x1, :lo12:tab
	ldr	w0, [x1]
	bl	put_u64
	adrp	x1, tab
	add	x1, x1, :lo12:tab
	ldrh	w0, [x1, #4]
	bl	put_u64
	adrp	x19, tab
	add	x19, x19, :lo12:tab
	adrp	x20, target
	add	x20, x20, :lo12:target
	ldr	w2, [x19, #8]
	cmp	x2, x20
	cset	x0, eq
	bl	put_u64
	add	x1, x19, #16
	ldr	x2, [x1]
	add	x2, x2, x1
	cmp	x2, x20
	cset	x0, eq
	bl	put_u64
	add	x1, x19, #24
	ldrsw	x2, [x1]
	add	x2, x2, x1
	cmp	x2, x20
	cset	x0, eq
	bl	put_u64
	adrp	x21, near
	add	x21, x21, :lo12:near
	add	x1, x19, #28
	ldrsh	x2, [x1]
	add	x2, x2, x1
	cmp	x2, x21
	cset	x0, eq
	bl	put_u64
	adr	x2, lit
	adrp	x3, lit
	add	x3, x3, :lo12:lit
	cmp	x2, x3
	cset	x0, eq
	bl	put_u64
	ldr	x0, lit
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
	.p2align 3
lit:	.xword	42
	.data
	.p2align 3
tab:	.word	absv
	.hword	absv
	.hword	0
	.word	target
	.word	0
	.xword	target - .
	.word	target - .
	.hword	near - .
	.hword	0
near:	.xword	0
	.section .rodata
target:	.xword	7
