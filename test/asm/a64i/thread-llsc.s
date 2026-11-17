# a64i's program (queue 308, review 306's F1): load/store-exclusive increments from two threads (clone, a futex join), the global
# monitor's effect: a store-exclusive whose load-exclusive came before the other thread's store to the word fails and its loop
# retries, so no update is lost. ldxr/stxr from both threads (2^18 increments each), ldaxr/stlxr in one against LSE's ldadd in
# the other (2^18 each), and ldxp/stxp on a 16-byte pair (2^17 each, both halves): exactly 2^19, 2^19, 2^18 and 2^18
# with: lib/out.s lib/thread.s
# lockstep: first-thread
# exit: 0
# stdout: "0000000000080000\n"
# stdout: "0000000000080000\n"
# stdout: "0000000000040000\n"
# stdout: "0000000000040000\n"
	.text
	.globl	_start
_start:
	adrp	x20, word
	add	x20, x20, :lo12:word
	// ldxr/stxr against ldxr/stxr
	adr	x0, llsc
	mov	x1, x20
	bl	two
	ldr	x0, [x20]
	bl	put_hex
	// ldaxr/stlxr against ldadd
	add	x21, x20, #16
	adr	x0, llsc_acq
	adr	x22, lse
	mov	x1, x21
	bl	two_mixed
	ldr	x0, [x21]
	bl	put_hex
	// ldxp/stxp
	add	x21, x20, #32
	adr	x0, pair
	mov	x1, x21
	bl	two
	ldr	x0, [x21]
	bl	put_hex
	ldr	x0, [x21, #8]
	bl	put_hex
	b	exit0

// two: the function x0 run on the argument x1 by two threads, joined
two:
	mov	x22, x0
// two_mixed: x0 in the first thread, x22 in the second, on the argument x1
two_mixed:
	stp	x29, x30, [sp, #-32]!
	stp	x1, x0, [sp, #16]
	adrp	x2, stk1
	add	x2, x2, :lo12:stk1
	add	x2, x2, #65536
	adrp	x3, tid1
	add	x3, x3, :lo12:tid1
	bl	spawn
	ldp	x1, x0, [sp, #16]
	mov	x0, x22
	adrp	x2, stk2
	add	x2, x2, :lo12:stk2
	add	x2, x2, #65536
	adrp	x3, tid2
	add	x3, x3, :lo12:tid2
	bl	spawn
	adrp	x0, tid1
	add	x0, x0, :lo12:tid1
	bl	join
	adrp	x0, tid2
	add	x0, x0, :lo12:tid2
	bl	join
	ldp	x29, x30, [sp], #32
	ret

llsc:
	mov	w2, #0x40000
1:	ldxr	x3, [x0]
	add	x3, x3, #1
	stxr	w4, x3, [x0]
	cbnz	w4, 1b
	subs	w2, w2, #1
	b.ne	1b
	ret
llsc_acq:
	mov	w2, #0x40000
1:	ldaxr	w3, [x0]
	add	w3, w3, #1
	stlxr	w4, w3, [x0]
	cbnz	w4, 1b
	subs	w2, w2, #1
	b.ne	1b
	ret
lse:
	mov	w2, #0x40000
	mov	w5, #1
1:	ldadd	w5, w3, [x0]
	subs	w2, w2, #1
	b.ne	1b
	ret
pair:
	mov	w2, #0x20000
1:	ldxp	x3, x5, [x0]
	add	x3, x3, #1
	add	x5, x5, #1
	stxp	w4, x3, x5, [x0]
	cbnz	w4, 1b
	subs	w2, w2, #1
	b.ne	1b
	ret

	.data
	.balign	16
word:	.zero	48
tid1:	.word	0
tid2:	.word	0
	.bss
	.balign	16
stk1:	.zero	65536
stk2:	.zero	65536
