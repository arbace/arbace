# Arbace's run test (AX4): a COMDAT group defined in two objects, the first one kept and the second's members discarded,
# its data with it (the same signature, different contents: the run shows which was kept)
# exit: 0
# stdout: "1\n1\n"
# with: lib/print.s lib/comdat-lib.s
	.globl	_start
_start:
	call	inline_fn
	mov	%rax, %rdi
	call	put_u64
	call	other_caller
	mov	%rax, %rdi
	call	put_u64
	xor	%edi, %edi
	call	sys_exit

	.section .text.inline_fn,"axG",@progbits,inline_fn,comdat
	.weak	inline_fn
	.type	inline_fn,@function
inline_fn:
	mov	inline_data(%rip), %rax
	ret
	.section .data.inline_data,"awG",@progbits,inline_fn,comdat
	.weak	inline_data
inline_data:
	.quad	1
