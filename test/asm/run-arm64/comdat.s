# Arbace's run test (AR4): a COMDAT group defined in two objects, the first one kept and the second's members discarded,
# its data with it (the same signature, different contents: the run shows which was kept)
# exit: 0
# stdout: "1\n1\n"
# with: lib/print.s lib/comdat-lib.s
	.globl	_start
_start:
	bl	inline_fn
	bl	put_u64
	bl	other_caller
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
	.section .text.inline_fn,"axG",@progbits,inline_fn,comdat
	.weak	inline_fn
	.type	inline_fn,@function
inline_fn:
	adrp	x0, inline_data
	ldr	x0, [x0, :lo12:inline_data]
	ret
	.section .data.inline_data,"awG",@progbits,inline_fn,comdat
	.weak	inline_data
	.p2align 3
inline_data:
	.xword	1
