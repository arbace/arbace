# a64i's program (queue 308): an exception's return clears the exclusive monitor (the Arm ARM's B2.9.5): a load-exclusive, a
# system call (getpid), then the store-exclusive fails (status 1) and stores nothing; the same for ldaxp and stlxp across
# sched_yield; with no call between, the retry loop's store succeeds
# with: lib/out.s
# exit: 0
# stdout: "0000000000000001\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000009\n"
# stdout: "0000000000000008\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	ldxr	x1, [x20]
	add	x1, x1, #1
	mov	x8, #172
	svc	#0
	stxr	w2, x1, [x20]
	mov	w0, w2
	bl	put_hex
	ldr	x0, [x20]
	bl	put_hex
	ldaxp	x1, x3, [x20]
	add	x1, x1, #2
	mov	x8, #124
	svc	#0
	stlxp	w2, x1, x3, [x20]
	mov	w0, w2
	bl	put_hex
	ldr	x0, [x20]
	bl	put_hex
1:	ldaxp	x1, x3, [x20]
	add	x1, x1, #2
	add	x3, x3, #1
	stlxp	w2, x1, x3, [x20]
	cbnz	w2, 1b
	ldr	x0, [x20]
	bl	put_hex
	ldr	x0, [x20, #8]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	7, 7
