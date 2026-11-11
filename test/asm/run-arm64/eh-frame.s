# Arbace's run test (AR4): CFI in two objects (.eh_frame's CIEs merged, the FDEs re-pointed), the program run through both
# exit: 0
# stdout: "42\n"
# with: lib/print.s lib/eh-lib.s
	.globl	_start
	.text
_start:
	mov	x0, #21
	bl	f
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
	.type	f,@function
f:
	.cfi_startproc
	stp	x29, x30, [sp, #-16]!
	.cfi_def_cfa_offset 16
	.cfi_offset w30, -8
	.cfi_offset w29, -16
	mov	x29, sp
	.cfi_def_cfa_register w29
	bl	twice
	ldp	x29, x30, [sp], #16
	.cfi_def_cfa sp, 0
	ret
	.cfi_endproc
	.size	f, .-f
