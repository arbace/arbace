# x86i's hand-written program (AX5.2): ud2, #UD: SIGILL; the process dies by the signal, as the CPU raises it.
# exit: 132
	.globl	_start
	.text
_start:
	ud2
