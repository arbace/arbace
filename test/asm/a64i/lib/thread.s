// a64i's programs' threads (queue 308): spawn and join by clone(2) and futex(2), no libc. x0-x11 not kept.
	.text
	.globl	spawn
	.globl	join
// spawn: a thread running the function x0 with the argument x1 on the stack whose top is x2 (16-aligned), its tid word at x3
// (set nonzero here; CLONE_CHILD_CLEARTID clears it and wakes it at the thread's end); x0 the thread's id. The thread ends by
// exit(2), alone, when its function returns.
spawn:
	mov	x9, x0
	mov	x10, x1
	mov	w11, #1
	str	w11, [x3]
	stp	x9, x10, [x2, #-16]!
	mov	x1, x2
	mov	x4, x3
	mov	x2, #0
	mov	x3, #0
	// CLONE_VM | CLONE_FS | CLONE_FILES | CLONE_SIGHAND | CLONE_THREAD | CLONE_SYSVSEM | CLONE_CHILD_CLEARTID
	mov	x0, #0x0f00
	movk	x0, #0x25, lsl #16
	mov	x8, #220
	svc	#0
	cbz	x0, 1f
	ret
1:	ldp	x9, x0, [sp], #16
	blr	x9
	mov	x0, #0
	mov	x8, #93
	svc	#0
// join: wait until the tid word at x0 is 0 (its thread ended): futex(FUTEX_WAIT) while it is not
join:
	mov	x9, x0
1:	ldr	w2, [x9]
	cbz	w2, 2f
	mov	x0, x9
	mov	x1, #0
	mov	x3, #0
	mov	x8, #98
	svc	#0
	b	1b
2:	ret
