# Arbace's run test (AR4): a reference to a symbol no object defines: the link refuses it in lld's words (no program to run)
# link-error: undefined symbol: nowhere
	.globl	_start
_start:
	bl	nowhere
