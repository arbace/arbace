# a64i's program (AR19, queue 288; ARS82 (a)): a load with writeback into its own base, ldr x1, [x1, #8]! (CONSTRAINED
# UNPREDICTABLE in the Arm ARM; no assembler takes it, hence the word), UNDEFINED as the M2 resolves it: SIGILL at the word, after a
# load with writeback into another register that runs
# with: lib/out.s
# exit: 132
# stdout: "0000000000000007\n"
	.text
	.globl	_start
_start:
	adrp	x1, buf
	add	x1, x1, :lo12:buf
	ldr	x0, [x1, #8]!
	bl	put_hex
	.inst	0xf8408c21		// ldr x1, [x1, #8]!
	b	exit0

	.data
	.p2align	4
buf:
	.quad	3, 7, 9
