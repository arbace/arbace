# Arbace's run test (AR4): mergeable strings in two objects deduplicated into one piece, relocations to them through a
# section symbol (ADRP and ADD's lo12, an ABS64 pointer: the piece found by offset), the addresses compared
# exit: 0
# stdout: "hello\nworld\nsame\n"
# with: lib/print.s lib/merge-lib.s
	.globl	_start
_start:
	adrp	x0, .Lhello
	add	x0, x0, :lo12:.Lhello
	bl	put_str
	adrp	x19, .Lworld
	add	x19, x19, :lo12:.Lworld
	mov	x0, x19
	bl	put_str
	adrp	x1, world_ptr
	ldr	x1, [x1, :lo12:world_ptr]
	cmp	x1, x19
	b.ne	1f
	adrp	x0, same
	add	x0, x0, :lo12:same
	bl	put_str
1:	mov	x0, #0
	bl	sys_exit
	.section .rodata.str1.1,"aMS",@progbits,1
.Lhello:	.asciz	"hello\n"
.Lworld:	.asciz	"world\n"
	.section .rodata
same:	.asciz	"same\n"
