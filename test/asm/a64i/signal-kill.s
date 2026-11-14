# a64i's program (AR20, queue 291; ARS84 (a)): signals a thread sends itself, delivered to their handlers on the thread's stack
# at the system call's return, as this M2's Linux delivers them: tgkill (si_code SI_TKILL) and kill (SI_USER) of SIGUSR1 to a
# SA_SIGINFO handler (the frame at sp, x2 = x1 + 128, the frame record 4,688 bytes above, the signal masked in the handler); the
# same signal sent again inside its handler, pending until rt_sigreturn restores the mask, then delivered (the handler runs twice);
# rt_sigreturn restoring x0 (the call's result) and x19 as the handler left it in the frame; SIGUSR2 blocked, sent, held, delivered
# when rt_sigprocmask unblocks it (a handler without SA_SIGINFO); SIGURG by default ignored; last, SA_RESETHAND: SIGUSR1's second
# delivery is the default's, the process's end (status 138)
# with: lib/out.s
# exit: 138
# stdout: "000000000000000a\n"
# stdout: "fffffffffffffffa\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000001250\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000200\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001901\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000001250\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000200\n"
# stdout: "0000000000001902\n"
# stdout: "000000000000000a\n"
# stdout: "fffffffffffffffa\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000001250\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000200\n"
# stdout: "0000000000001010\n"
# stdout: "000000000000000a\n"
# stdout: "fffffffffffffffa\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000001250\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000200\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000002323\n"
# stdout: "0000000000001212\n"
# stdout: "000000000000000c\n"
# stdout: "0000000000002121\n"
# stdout: "000000000000000a\n"
# stdout: "fffffffffffffffa\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000001250\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000200\n"
# stdout: "0000000000003333\n"
	.text
	.globl	_start
_start:
	mov	x0, #10
	adrp	x1, act1
	add	x1, x1, :lo12:act1
	bl	setact
	mov	x0, #12
	adrp	x1, act2
	add	x1, x1, :lo12:act2
	bl	setact
	mov	x19, #0
	mov	x0, #10
	mov	x1, #0
	bl	send				// tgkill(SIGUSR1)
	bl	put_hex				// x0: tgkill's 0
	mov	x0, x19
	bl	put_hex				// x19: the handler's
	mov	x0, #10
	mov	x1, #1
	bl	send				// kill(SIGUSR1)
	mov	x0, x19
	bl	put_hex
	adrp	x0, again
	add	x0, x0, :lo12:again
	mov	x1, #1
	str	x1, [x0]			// the handler sends SIGUSR1 again
	mov	x0, #10
	mov	x1, #0
	bl	send
	adrp	x0, count
	add	x0, x0, :lo12:count
	ldr	x0, [x0]
	bl	put_hex				// the handler ran twice more
	mov	x0, #23
	mov	x1, #0
	bl	send				// SIGURG: ignored by default
	mov	x0, #0x2323
	bl	put_hex
	mov	x0, #2048			// SIGUSR2 blocked
	mov	x1, #0
	bl	mask
	mov	x0, #12
	mov	x1, #0
	bl	send
	mov	x0, #0x1212
	bl	put_hex				// held
	mov	x0, #2048
	mov	x1, #1
	bl	mask				// unblocked: delivered at rt_sigprocmask's return
	mov	x0, #0x2121
	bl	put_hex
	mov	x0, #10				// SA_RESETHAND
	adrp	x1, act3
	add	x1, x1, :lo12:act3
	bl	setact
	mov	x0, #10
	mov	x1, #0
	bl	send
	mov	x0, #0x3333
	bl	put_hex
	mov	x0, #10
	mov	x1, #0
	bl	send				// the default: the end
	b	exit0
// setact: rt_sigaction(x0, x1, 0, 8)
setact:
	mov	x2, #0
	mov	x3, #8
	mov	x8, #134
	svc	#0
	ret
// mask: rt_sigprocmask(x1 (SIG_BLOCK 0, SIG_UNBLOCK 1), {x0}, 0, 8)
mask:
	adrp	x2, set
	add	x2, x2, :lo12:set
	str	x0, [x2]
	mov	x0, x1
	mov	x1, x2
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
// handler1: SA_SIGINFO
handler1:
	mov	x24, x30
	mov	x20, x1
	mov	x21, x2
	bl	put_hex				// the signal
	ldrsw	x0, [x20, #8]
	bl	put_hex				// si_code
	mov	x0, sp
	sub	x0, x20, x0
	bl	put_hex				// x1 - sp: 0
	sub	x0, x21, x20
	bl	put_hex				// x2 - x1: 128
	sub	x0, x29, x20
	bl	put_hex				// the frame record: 4688 above
	ldr	x0, [x21, #40]
	bl	put_hex				// uc_sigmask
	mov	x0, #0
	mov	x1, #0
	adrp	x2, set
	add	x2, x2, :lo12:set
	mov	x3, #8
	mov	x8, #135
	svc	#0
	ldr	x0, [x2]
	bl	put_hex				// the mask in the handler: SIGUSR1's
	adrp	x0, count
	add	x0, x0, :lo12:count
	ldr	x1, [x0]
	add	x1, x1, #1
	str	x1, [x0]
	mov	x2, #0x1900
	add	x1, x1, x2
	str	x1, [x21, #336]			// x19 in the frame: 0x1900 + the count
	adrp	x0, again
	add	x0, x0, :lo12:again
	ldr	x1, [x0]
	cbz	x1, 1f
	str	xzr, [x0]
	mov	x0, #10
	mov	x1, #0
	bl	send				// pending: SIGUSR1 is masked here
	mov	x0, #0x1010
	bl	put_hex
1:	ret	x24
// handler2: no SA_SIGINFO
handler2:
	mov	x24, x30
	bl	put_hex
	ret	x24
restorer:
	mov	x8, #139
	svc	#0
	.data
	.p2align	3
act1:	.quad	handler1, 0x04000004, restorer, 0	// SA_SIGINFO | SA_RESTORER
act2:	.quad	handler2, 0x04000000, restorer, 0	// SA_RESTORER
act3:	.quad	handler1, 0x84000004, restorer, 0	// SA_RESETHAND too
set:	.quad	0
again:	.quad	0
count:	.quad	0
