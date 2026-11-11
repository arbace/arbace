# Arbace's run test (AX4): the survey's P4, `hello` written and exit 42, by syscalls alone
# exit: 42
# stdout: "hello\n"
	.globl _start
	.text
_start:
	mov $1, %eax
	mov $1, %edi
	lea msg(%rip), %rsi
	mov $len, %edx
	syscall
	mov $60, %eax
	mov $42, %edi
	syscall
	.section .rodata
msg:	.ascii "hello\n"
	len = . - msg
