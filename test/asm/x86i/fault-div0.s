# x86i's hand-written program (AX5.2): div by zero, #DE: SIGFPE; the process dies by the signal, as the CPU raises it.
# exit: 136
	.globl	_start
	.text
_start:
	mov $7, %eax
	xor %edx, %edx
	xor %ecx, %ecx
	div %ecx
	mov $60, %eax
	syscall
