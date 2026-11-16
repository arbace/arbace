# a64i's program (AR5): Linux by name: clock_gettime (realtime, monotonic), its fields' ranges; getpid's sign
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffea\n"
	.text
	.globl	_start
_start:
	sub	sp, sp, #32
	mov	x0, #1
	mov	x1, sp
	mov	x8, #113
	svc	#0
	bl	put_hex
	ldp	x19, x20, [sp]
	mov	x0, #1
	add	x1, sp, #16
	mov	x8, #113
	svc	#0
	ldp	x21, x22, [sp, #16]
	cmp	x21, x19
	ccmp	x22, x20, #0, eq
	cset	x0, hs
	cmp	x21, x19
	csinc	x0, x0, xzr, ls
	bl	put_hex
	mov	x9, #51712
	movk	x9, #15258, lsl #16
	cmp	x22, x9
	cset	x0, lo
	bl	put_hex
	mov	x0, #0
	mov	x1, sp
	mov	x8, #113
	svc	#0
	ldr	x0, [sp]
	mov	x9, #0x6000
	movk	x9, #0x6a00, lsl #16
	cmp	x0, x9
	cset	x0, hi
	bl	put_hex
	mov	x8, #172
	svc	#0
	cmp	x0, #0
	cset	x0, gt
	bl	put_hex
	mov	x0, #99
	mov	x1, sp
	mov	x8, #113
	svc	#0
	bl	put_hex
	add	sp, sp, #32
	b	exit0
