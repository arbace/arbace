# a64i's program (AR19, queue 288): dc zva on a read-only page: SIGSEGV at the dc zva (dc cvau on it runs: cache maintenance
# needs the page mapped, not writable)
# with: lib/out.s
# exit: 139
	.text
	.globl	_start
_start:
	adr	x1, _start
	dc	cvau, x1
	dc	civac, x1
	dc	zva, x1
	b	exit0
