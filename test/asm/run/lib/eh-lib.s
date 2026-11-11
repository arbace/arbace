# The second object of eh-frame.s (AX4): a function with a frame described by CFI
	.text
	.globl	twice_plus2
	.type	twice_plus2,@function
twice_plus2:
	.cfi_startproc
	push	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	mov	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	lea	2(%rdi,%rdi), %rax
	pop	%rbp
	.cfi_def_cfa %rsp, 8
	ret
	.cfi_endproc
	.size	twice_plus2, .-twice_plus2
