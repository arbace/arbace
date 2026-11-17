# Arbace's run test (AX4): the smallest program, an exit status and nothing written
# exit: 7
	.globl	_start
_start:
	mov	$60, %eax
	mov	$7, %edi
	syscall
