# Arbace's run test (AR4): an exit status alone
# exit: 7
	.globl	_start
_start:
	mov	x0, #7
	mov	x8, #93
	svc	#0
