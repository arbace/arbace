# x86i's hand-written program (AX5.2): a store to address 0: SIGSEGV; the process dies by the signal, as the CPU raises it.
# exit: 139
	.globl	_start
	.text
_start:
	xor %eax, %eax
	movq $1, (%rax)
	mov $60, %eax
	syscall
