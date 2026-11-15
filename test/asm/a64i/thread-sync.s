# a64i's program (queue 308, review 306's F1): the other atomic paths from two threads (clone, a futex join), each exact: a
# casal loop (2^17 increments each), a caspal loop on a 16-byte pair (2^16 each, both halves), a swpa spinlock released by stlr
# around a read, a delay and a write (2^16 each), and a futex mutex as musl's and glibc's (Drepper's: cas 0 -> 1, else swap 2
# and FUTEX_WAIT; the unlock's swap 0 and FUTEX_WAKE when it was 2) by ldaxr/stlxr around the same section (2^16 each): 2^18,
# 2^17 twice, 2^17 and 2^17
# with: lib/out.s lib/thread.s
# exit: 0
# stdout: "0000000000040000\n"
# stdout: "0000000000020000\n"
# stdout: "0000000000020000\n"
# stdout: "0000000000020000\n"
# stdout: "0000000000020000\n"
	.text
	.globl	_start
_start:
	adrp	x20, area
	add	x20, x20, :lo12:area
	adr	x0, cas
	mov	x1, x20
	bl	two
	ldr	x0, [x20]
	bl	put_hex
	adr	x0, casp
	add	x1, x20, #16
	bl	two
	ldr	x0, [x20, #16]
	bl	put_hex
	ldr	x0, [x20, #24]
	bl	put_hex
	adr	x0, spin
	add	x1, x20, #32
	bl	two
	ldr	x0, [x20, #40]
	bl	put_hex
	adr	x0, mutex
	add	x1, x20, #48
	bl	two
	ldr	x0, [x20, #56]
	bl	put_hex
	b	exit0

// two: the function x0 run on the argument x1 by two threads, joined
two:
	stp	x29, x30, [sp, #-32]!
	stp	x0, x1, [sp, #16]
	adrp	x2, stk1
	add	x2, x2, :lo12:stk1
	add	x2, x2, #65536
	adrp	x3, tid1
	add	x3, x3, :lo12:tid1
	bl	spawn
	ldp	x0, x1, [sp, #16]
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

// the critical section at x0 + 8: a read, a delay of 16 iterations, the write of the value + 1
section:
	ldr	x6, [x0, #8]
	mov	w7, #16
1:	subs	w7, w7, #1
	b.ne	1b
	add	x6, x6, #1
	str	x6, [x0, #8]
	ret

cas:
	mov	w2, #0x20000
1:	ldr	w3, [x0]
2:	add	w4, w3, #1
	mov	w5, w3
	casal	w5, w4, [x0]
	cmp	w5, w3
	mov	w3, w5
	b.ne	2b
	subs	w2, w2, #1
	b.ne	1b
	ret

casp:
	mov	w2, #0x10000
1:	ldp	x4, x5, [x0]
2:	add	x6, x4, #1
	add	x7, x5, #1
	mov	x8, x4
	mov	x9, x5
	caspal	x8, x9, x6, x7, [x0]
	cmp	x8, x4
	ccmp	x9, x5, #0, eq
	mov	x4, x8
	mov	x5, x9
	b.ne	2b
	subs	w2, w2, #1
	b.ne	1b
	ret

spin:
	stp	x29, x30, [sp, #-16]!
	mov	w2, #0x10000
	mov	w3, #1
1:	swpa	w3, w4, [x0]
	cbnz	w4, 1b
	bl	section
	stlr	wzr, [x0]
	subs	w2, w2, #1
	b.ne	1b
	ldp	x29, x30, [sp], #16
	ret

mutex:
	stp	x29, x30, [sp, #-16]!
	mov	x10, x0
	mov	w11, #0x10000
	// lock: cas 0 -> 1
1:	ldaxr	w3, [x10]
	cbnz	w3, 3f
	mov	w4, #1
	stxr	w5, w4, [x10]
	cbnz	w5, 1b
	b	5f
	// contended: swap 2 until it was 0, waiting on the futex while it is 2
3:	clrex
4:	ldaxr	w3, [x10]
	mov	w4, #2
	stxr	w5, w4, [x10]
	cbnz	w5, 4b
	cbz	w3, 5f
	mov	x0, x10
	mov	x1, #0
	mov	x2, #2
	mov	x3, #0
	mov	x8, #98
	svc	#0
	b	4b
5:	mov	x0, x10
	bl	section
	// unlock: swap 0, a wake when it was 2
6:	ldxr	w3, [x10]
	stlxr	w5, wzr, [x10]
	cbnz	w5, 6b
	cmp	w3, #2
	b.ne	7f
	mov	x0, x10
	mov	x1, #1
	mov	x2, #1
	mov	x8, #98
	svc	#0
7:	subs	w11, w11, #1
	b.ne	1b
	ldp	x29, x30, [sp], #16
	ret

	.data
	.balign	16
area:	.zero	64
tid1:	.word	0
tid2:	.word	0
	.bss
	.balign	16
stk1:	.zero	65536
stk2:	.zero	65536
