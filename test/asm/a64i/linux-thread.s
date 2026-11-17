# a64i's program (queue 302): a thread (clone with CLONE_THREAD, CLONE_PARENT_SETTID and CLONE_CHILD_CLEARTID) that prints and
# exits, joined by a futex wait on its id (cleared and woken at its end). The lockstep follows a threaded program to its first
# thread: ptrace follows the traced thread alone and the native's threads run in its scheduler's order (Arbace's executable, as
# every Go program, starts sysmon after about 160 system calls), so its verdict is EQUAL TO THE FIRST THREAD, every call before
# the clone compared
# with: lib/out.s
# lockstep: first-thread
# exit: 0
# stdout: "start\n"
# stdout: "child\n"
# stdout: "0000000000000001\n"
# stdout: "joined\n"
	.text
	.globl	_start
_start:
	adr	x0, m1
	mov	x1, #6
	bl	put_str
	// clone(CLONE_VM|CLONE_FS|CLONE_FILES|CLONE_SIGHAND|CLONE_THREAD|CLONE_SYSVSEM|CLONE_PARENT_SETTID|CLONE_CHILD_CLEARTID,
	// the child's stack, &tid, 0, &tid): the child prints and exits, the parent waits on tid (set before clone returns, cleared
	// and woken at the child's end)
	mov	x0, #0x0f00
	movk	x0, #0x0035, lsl #16
	adr	x1, stack_top
	adr	x2, tid
	mov	x3, #0
	adr	x4, tid
	mov	x8, #220
	svc	#0
	cbz	x0, child
	mov	x19, x0
wait:	adr	x9, tid
	ldr	w1, [x9]
	cbz	w1, joined
	// futex(&tid, FUTEX_WAIT, its value)
	mov	x0, x9
	mov	x2, x1
	mov	x1, #0
	mov	x3, #0
	mov	x8, #98
	svc	#0
	b	wait
joined:	// the child's id was > 0 (printed after the join: the two threads' writes in one order)
	cmp	x19, #0
	cset	x0, gt
	bl	put_hex
	adr	x0, m3
	mov	x1, #7
	bl	put_str
	b	exit0
child:	adr	x0, m2
	mov	x1, #6
	bl	put_str
	mov	x0, #0
	mov	x8, #93
	svc	#0
m1:	.ascii	"start\n"
m2:	.ascii	"child\n"
m3:	.ascii	"joined\n"
	.data
	.balign	16
tid:	.word	0, 0, 0, 0
stack:	.space	16384
stack_top:
