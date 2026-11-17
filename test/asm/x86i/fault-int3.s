# x86i's hand-written program (AX5.2): int3, #BP: SIGTRAP; the process dies by the signal, as the CPU raises it.
# exit: 133
	.globl	_start
	.text
_start:
	int3
	mov $60, %eax
	syscall
