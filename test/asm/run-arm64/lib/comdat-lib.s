# The second object of comdat.s (AR4): the same COMDAT group, discarded at the link
	.text
	.globl	other_caller
other_caller:
	b	inline_fn
	.section .text.inline_fn,"axG",@progbits,inline_fn,comdat
	.weak	inline_fn
	.type	inline_fn,@function
inline_fn:
	adrp	x0, inline_data
	ldr	x0, [x0, :lo12:inline_data]
	add	x0, x0, #100
	ret
	.section .data.inline_data,"awG",@progbits,inline_fn,comdat
	.weak	inline_data
	.p2align 3
inline_data:
	.xword	2
