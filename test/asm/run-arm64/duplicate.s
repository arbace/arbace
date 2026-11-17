# Arbace's run test (AR4): two strong definitions of one symbol in two objects: the link refuses it in lld's words
# link-error: duplicate symbol: twice
# with: lib/dup-lib.s
	.globl	_start
	.globl	twice
_start:
	bl	twice
twice:	ret
