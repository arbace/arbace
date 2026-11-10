# The second object of eh-frame.s (AR4): a function with its own CFI (a second FDE, the same CIE)
	.text
	.globl	twice
	.type	twice,@function
twice:
	.cfi_startproc
	stp	x29, x30, [sp, #-16]!
	.cfi_def_cfa_offset 16
	.cfi_offset w30, -8
	.cfi_offset w29, -16
	add	x0, x0, x0
	ldp	x29, x30, [sp], #16
	.cfi_def_cfa_offset 0
	.cfi_restore w30
	.cfi_restore w29
	ret
	.cfi_endproc
	.size	twice, .-twice
