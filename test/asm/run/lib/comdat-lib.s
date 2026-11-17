# The second object of comdat.s (AX4): the same COMDAT group, discarded at the link
	.text
	.globl	other_caller
other_caller:
	jmp	inline_fn

	.section .text.inline_fn,"axG",@progbits,inline_fn,comdat
	.weak	inline_fn
	.type	inline_fn,@function
inline_fn:
	mov	inline_data(%rip), %rax
	add	$100, %rax
	ret
	.section .data.inline_data,"awG",@progbits,inline_fn,comdat
	.weak	inline_data
inline_data:
	.quad	2
