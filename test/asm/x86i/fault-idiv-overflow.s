# x86i's hand-written program (AX5.2): idiv of the least 64-bit value by -1, #DE: SIGFPE; the process dies by the signal, as the CPU raises it.
# exit: 136
	.globl	_start
	.text
_start:
	mov $1, %rax
	shl $63, %rax
	cqo
	mov $-1, %rcx
	idiv %rcx
	mov $60, %eax
	syscall
