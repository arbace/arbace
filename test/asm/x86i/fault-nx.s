# x86i's hand-written program (AX5.2): a jump into a writable, non-executable segment: SIGSEGV; the process dies by the signal, as the CPU raises it.
# exit: 139
	.globl	_start
	.text
_start:
	lea code(%rip), %rax
	jmp *%rax
	.data
	code: .byte 0xc3
