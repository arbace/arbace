# x86i's hand-written program (AX5.2): hlt in user mode, #GP: SIGSEGV; the process dies by the signal, as the CPU raises it.
# exit: 139
	.globl	_start
	.text
_start:
	hlt
