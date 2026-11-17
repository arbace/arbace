# a64i's program (AR19, queue 288; ARS82 (a)): a load pair into one register twice, ldp x4, x4, [x1] (CONSTRAINED UNPREDICTABLE in
# the Arm ARM; no assembler takes it, hence the word), UNDEFINED as the M2 resolves it: SIGILL at the word, after a load pair into
# two registers that runs
# with: lib/out.s
# exit: 132
# stdout: "000000000000000a\n"
	.text
	.globl	_start
_start:
	adrp	x1, buf
	add	x1, x1, :lo12:buf
	ldp	x4, x5, [x1]
	add	x0, x4, x5
	bl	put_hex
	.inst	0xa9401024		// ldp x4, x4, [x1]
	b	exit0

	.data
	.p2align	4
buf:
	.quad	3, 7
