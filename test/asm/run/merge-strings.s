# Arbace's run test (AX4): mergeable strings in two objects deduplicated into one piece (MergeInputSection), relocations
# to them through a section symbol (the piece found by offset), the addresses compared
# exit: 0
# stdout: "hello\nworld\nsame\n"
# with: lib/print.s lib/merge-lib.s
	.globl	_start
_start:
	lea	.Lhello(%rip), %rdi
	mov	$6, %esi
	call	put_str
	lea	.Lworld(%rip), %rdi
	mov	$6, %esi
	call	put_str
	lea	.Lworld(%rip), %rax
	cmp	world_ptr(%rip), %rax
	jne	1f
	lea	same(%rip), %rdi
	mov	$5, %esi
	call	put_str
	xor	%edi, %edi
	call	sys_exit
1:	mov	$1, %edi
	call	sys_exit

	.section .rodata.str1.1,"aMS",@progbits,1
.Lhello:
	.asciz	"hello\n"
.Lworld:
	.asciz	"world\n"
	.section .rodata
same:	.ascii	"same\n"
