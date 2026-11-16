# Arbace's run test (AX4): two strong definitions of one symbol in two objects: the link refuses it in lld's words
# link-error: duplicate symbol: twice
# with: lib/dup-lib.s
	.globl	_start
	.globl	twice
_start:
twice:
	mov	$60, %eax
	syscall
