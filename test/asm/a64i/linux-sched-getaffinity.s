# a64i's program (queue 302): sched_getaffinity, Go's runtime's CPU count (getproccount at start, and its GOMAXPROCS update each
# second: Arbace's executable asks it about once a second of a64i's run). a64i is one CPU, CPU 0, its mask's word 1; the lockstep
# takes the native's mask (the world's). The bytes written (8: one word, on a host of at most 64 CPUs), a mask not empty, the next
# word untouched; len 4 and len 12 EINVAL; pid -1 ESRCH; a mask at 0 EFAULT; the process's own pid 8
# with: lib/out.s
# exit: 0
# stdout: "0000000000000008\n"
# stdout: "0000000000000001\n"
# stdout: "5555555555555555\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffea\n"
# stdout: "fffffffffffffffd\n"
# stdout: "fffffffffffffff2\n"
# stdout: "0000000000000008\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	sub	sp, sp, #32
	mov	x9, #0x5555555555555555
	str	x9, [sp]
	str	x9, [sp, #8]
	str	x9, [sp, #16]
	// sched_getaffinity(0, 16, buf): the bytes written (8 for at most 64 CPUs)
	mov	x0, #0
	mov	x1, #16
	mov	x2, sp
	mov	x8, #123
	svc	#0
	bl	put_hex
	// the mask's word holds a CPU (not 0); the next word untouched
	ldr	x0, [sp]
	cmp	x0, #0
	cset	x0, ne
	bl	put_hex
	ldr	x0, [sp, #8]
	bl	put_hex
	// len 4 and len 12: not a multiple of the word's size, EINVAL
	mov	x0, #0
	mov	x1, #4
	mov	x2, sp
	mov	x8, #123
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #12
	mov	x2, sp
	mov	x8, #123
	svc	#0
	bl	put_hex
	// pid -1: no such task, ESRCH
	mov	x0, #-1
	mov	x1, #8
	mov	x2, sp
	mov	x8, #123
	svc	#0
	bl	put_hex
	// a mask at 0: EFAULT
	mov	x0, #0
	mov	x1, #8
	mov	x2, #0
	mov	x8, #123
	svc	#0
	bl	put_hex
	// the process's own pid (getpid): 8
	mov	x8, #172
	svc	#0
	mov	x1, #8
	mov	x2, sp
	mov	x8, #123
	svc	#0
	bl	put_hex
	add	sp, sp, #32
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
msg:	.ascii	"done.\n"
	.balign	4
