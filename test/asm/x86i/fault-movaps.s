# x86i's hand-written program (AX5.5): movaps from a 16-byte operand that is not 16-byte aligned: #GP, SIGSEGV; the process dies
# by the signal, as the CPU raises it (legacy SSE's aligned operands; movups would not fault).
# exit: 139
	.globl	_start
	.text
_start:
	lea	buf+4(%rip), %rax
	movups	(%rax), %xmm0
	movaps	(%rax), %xmm1
	mov	$60, %eax
	xor	%edi, %edi
	syscall
	.data
	.align	16
buf:	.zero	32
