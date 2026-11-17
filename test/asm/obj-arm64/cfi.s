// Arbace's synthetic object test (AR4): CFI into .eh_frame: two frames sharing a CIE, a frame pointer, saved registers, a
// remembered state, a simple frame of its own CIE
	.text
	.globl	a
a:	.cfi_startproc
	stp	x29, x30, [sp, #-32]!
	.cfi_def_cfa_offset 32
	.cfi_offset w30, -24
	.cfi_offset w29, -32
	mov	x29, sp
	.cfi_def_cfa_register w29
	str	d8, [sp, #16]
	.cfi_offset b8, -16
	.cfi_remember_state
	ldp	x29, x30, [sp], #32
	.cfi_def_cfa sp, 0
	ret
	.cfi_restore_state
	ret
	.cfi_endproc
b:	.cfi_startproc
	sub	sp, sp, #16
	.cfi_adjust_cfa_offset 16
	add	sp, sp, #16
	.cfi_adjust_cfa_offset -16
	.cfi_negate_ra_state
	ret
	.cfi_endproc
c:	.cfi_startproc simple
	.cfi_def_cfa sp, 0
	ret
	.cfi_endproc
