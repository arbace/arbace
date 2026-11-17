# Arbace's synthetic object test (AX4): CFI directives into .eh_frame (a CIE, FDEs, the advances, the register rules)
	.text
	.globl	f
	.type	f,@function
f:
	.cfi_startproc
	pushq	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	movq	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	subq	$64, %rsp
	.fill	70, 1, 0x90
	pushq	%rbx
	.cfi_rel_offset %rbx, 8
	.fill	300, 1, 0x90
	.cfi_remember_state
	leave
	.cfi_def_cfa %rsp, 8
	ret
	.cfi_restore_state
	.cfi_restore %rbx
	.cfi_undefined %rip
	.cfi_same_value %r12
	.cfi_register %r13, %r14
	.cfi_escape 0x2e, 0x10
	.cfi_adjust_cfa_offset -8
	.cfi_offset 17, -24
	ret
	.cfi_endproc
	.size	f, .-f
g:
	.cfi_startproc
	.cfi_signal_frame
	nop
	.cfi_endproc
h:
	.cfi_startproc simple
	.cfi_def_cfa %rsp, 0
	nop
	.cfi_endproc
	.section .text.k,"ax",@progbits
k:
	.cfi_startproc
	.cfi_personality 0x9b, DW.ref.__gxx_personality_v0
	.cfi_lsda 0x1b, .Lexcept
	ret
	.cfi_endproc
	.section .gcc_except_table,"a",@progbits
.Lexcept:
	.byte 0xff
