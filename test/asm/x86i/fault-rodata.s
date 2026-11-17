# x86i's hand-written program (AX5.2): a store into a read-only segment: SIGSEGV; the process dies by the signal, as the CPU raises it.
# exit: 139
	.globl	_start
	.text
_start:
	lea ro(%rip), %rax
	movb $1, (%rax)
	mov $60, %eax
	syscall
	.section .rodata
	ro: .byte 0
