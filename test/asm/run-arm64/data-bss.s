# Arbace's run test (AR4): .data and .bss (and their .data.*/.bss.* pieces merged), a common symbol, loads and stores by
# ADRP and the LDST relocations of every width
# exit: 0
# stdout: "1234\n0\n42\n7\n"
# with: lib/print.s
	.globl	_start
	.comm	shared, 16, 8
	.data
word:	.xword	1234
	.section .data.more,"aw"
half:	.hword	7
	.section .bss.zero,"aw",@nobits
	.p2align 3
zero:	.zero	8
	.text
_start:
	adrp	x0, word
	ldr	x0, [x0, :lo12:word]
	bl	put_u64
	adrp	x0, zero
	ldr	x0, [x0, :lo12:zero]
	bl	put_u64
	adrp	x1, shared
	add	x1, x1, :lo12:shared
	mov	x2, #42
	str	x2, [x1, #8]
	ldr	x0, [x1, #8]
	bl	put_u64
	adrp	x0, half
	ldrh	w0, [x0, :lo12:half]
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
