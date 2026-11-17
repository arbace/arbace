# a64i's program (queue 311, review 306 F7): a pending signal is discarded when its disposition becomes one that ignores it, as
# Linux's do_sigaction flushes it (sig_handler_ignored): SIGUSR1 blocked, sent (kill), set to SIG_IGN, then to a handler, then
# unblocked: the handler does not run; SIGURG (ignored by default) blocked with a handler, sent (tgkill), set to SIG_DFL, then to
# the handler, unblocked: not run; SIGTSTP (a stop signal, not ignored by SIG_DFL: sig_kernel_ignore) the same: kept, run once;
# SIGUSR2 blocked, sent, its handler set again (not ignored): kept, run once. Each line the handler's count of runs so far
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000014\n"
# stdout: "0000000000000001\n"
# stdout: "000000000000000c\n"
# stdout: "0000000000000002\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	// SIGUSR1: blocked, sent by kill, SIG_IGN, a handler, unblocked: discarded
	mov	x0, #10
	bl	block
	mov	x0, #10
	mov	x1, #1
	bl	send
	mov	x0, #10
	adrp	x1, ign
	add	x1, x1, :lo12:ign
	bl	setact
	mov	x0, #10
	adrp	x1, act
	add	x1, x1, :lo12:act
	bl	setact
	mov	x0, #10
	bl	unblock
	bl	count
	// SIGURG: a handler, blocked, sent by tgkill, SIG_DFL (ignored by default), the handler, unblocked: discarded
	mov	x0, #23
	adrp	x1, act
	add	x1, x1, :lo12:act
	bl	setact
	mov	x0, #23
	bl	block
	mov	x0, #23
	mov	x1, #0
	bl	send
	mov	x0, #23
	adrp	x1, dfl
	add	x1, x1, :lo12:dfl
	bl	setact
	mov	x0, #23
	adrp	x1, act
	add	x1, x1, :lo12:act
	bl	setact
	mov	x0, #23
	bl	unblock
	bl	count
	// SIGTSTP: blocked, sent, SIG_DFL (a stop signal: kept), the handler, unblocked: run
	mov	x0, #20
	bl	block
	mov	x0, #20
	mov	x1, #0
	bl	send
	mov	x0, #20
	adrp	x1, dfl
	add	x1, x1, :lo12:dfl
	bl	setact
	mov	x0, #20
	adrp	x1, act
	add	x1, x1, :lo12:act
	bl	setact
	mov	x0, #20
	bl	unblock
	bl	count
	// SIGUSR2: blocked, sent, its handler set (not ignored), unblocked: run
	mov	x0, #12
	bl	block
	mov	x0, #12
	mov	x1, #1
	bl	send
	mov	x0, #12
	adrp	x1, act
	add	x1, x1, :lo12:act
	bl	setact
	mov	x0, #12
	bl	unblock
	bl	count
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
// count: the handler's count of runs on stdout
count:
	mov	x25, x30
	adrp	x0, runs
	add	x0, x0, :lo12:runs
	ldr	x0, [x0]
	bl	put_hex
	ret	x25
// setact: rt_sigaction(x0, x1, 0, 8)
setact:
	mov	x2, #0
	mov	x3, #8
	mov	x8, #134
	svc	#0
	ret
// block, unblock: rt_sigprocmask(SIG_BLOCK or SIG_UNBLOCK, {x0}, 0, 8)
block:
	mov	x1, #0
	b	mask
unblock:
	mov	x1, #1
mask:
	sub	x0, x0, #1
	mov	x2, #1
	lsl	x2, x2, x0
	adrp	x3, set
	add	x3, x3, :lo12:set
	str	x2, [x3]
	mov	x0, x1
	mov	x1, x3
	mov	x2, #0
	mov	x3, #8
	mov	x8, #135
	svc	#0
	ret
// send: x0 to this thread by tgkill (x1 0) or to the process by kill (x1 1)
send:
	mov	x9, x0
	mov	x10, x1
	mov	x8, #172			// getpid
	svc	#0
	mov	x11, x0
	cbnz	x10, 1f
	mov	x8, #178			// gettid
	svc	#0
	mov	x1, x0
	mov	x0, x11
	mov	x2, x9
	mov	x8, #131			// tgkill
	svc	#0
	ret
1:	mov	x0, x11
	mov	x1, x9
	mov	x8, #129			// kill
	svc	#0
	ret
// handler: the signal on stdout, the count of runs incremented
handler:
	mov	x24, x30
	bl	put_hex
	adrp	x0, runs
	add	x0, x0, :lo12:runs
	ldr	x1, [x0]
	add	x1, x1, #1
	str	x1, [x0]
	ret	x24
restorer:
	mov	x8, #139
	svc	#0
msg:	.ascii	"done.\n"
	.data
	.p2align	3
act:	.quad	handler, 0x04000000, restorer, 0	// SA_RESTORER
ign:	.quad	1, 0, 0, 0				// SIG_IGN
dfl:	.quad	0, 0, 0, 0				// SIG_DFL
set:	.quad	0
runs:	.quad	0
