# a64i's program (AR20, queue 291; ARS84 (a)): synchronous faults delivered to a handler (SA_SIGINFO, SA_ONSTACK, SA_RESTORER)
# on an alternate stack, as this M2's Linux delivers them: a load from 0 (SIGSEGV, SEGV_MAPERR, the ESR's level-2 translation
# fault), a store into .rodata (SEGV_ACCERR, the ESR's class and WnR), udf (SIGILL), brk (SIGTRAP), an unaligned ldxr (SIGBUS);
# the handler prints the signal, si_code, si_addr, uc_mcontext.fault_address, the ESR record (0: none), the frame's place below the
# alternate stack's top, uc_stack.ss_flags and the mask, writes x22 into the frame and steps pc over the instruction; rt_sigreturn
# brings it back with x22 the frame's; last, the same load with SIGSEGV blocked ends the process (a blocked fault is fatal)
# with: lib/out.s
# exit: 139
# stdout: "0000000000000000\n"
# stdout: "000000000000000b\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000092000006\n"
# stdout: "ffffffffffffeda0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000400\n"
# stdout: "0000000000000002\n"
# stdout: "000000000000000b\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000201000\n"
# stdout: "0000000000201000\n"
# stdout: "0000000092000040\n"
# stdout: "ffffffffffffeda0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000400\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000211084\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffeda0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000211090\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffeda0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000010\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000221319\n"
# stdout: "0000000000221319\n"
# stdout: "0000000092000021\n"
# stdout: "ffffffffffffeda0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000040\n"
# stdout: "0000000000000006\n"
	.text
	.globl	_start
_start:
	adrp	x0, ss
	add	x0, x0, :lo12:ss
	adrp	x1, altstk
	add	x1, x1, :lo12:altstk
	str	x1, [x0]
	mov	x2, #16384
	str	x2, [x0, #16]
	mov	x1, #0
	mov	x8, #132			// sigaltstack
	svc	#0
	bl	put_hex
	mov	x0, #11
	bl	setact
	mov	x0, #4
	bl	setact
	mov	x0, #5
	bl	setact
	mov	x0, #7
	bl	setact
	mov	x22, #1
	mov	x1, #0
	ldr	x0, [x1]			// SIGSEGV
	mov	x0, x22
	bl	put_hex
	adrp	x1, ro
	add	x1, x1, :lo12:ro
	mov	x23, #1				// the handler masks the ESR's fault status
	str	x0, [x1]			// SIGSEGV, SEGV_ACCERR
	mov	x23, #0
	mov	x0, x22
	bl	put_hex
	udf	#0				// SIGILL
	mov	x0, x22
	bl	put_hex
	brk	#1				// SIGTRAP
	mov	x0, x22
	bl	put_hex
	adrp	x1, buf
	add	x1, x1, :lo12:buf
	add	x1, x1, #9
	ldxr	x0, [x1]			// SIGBUS
	mov	x0, x22
	bl	put_hex
	// SIGSEGV blocked: the fault ends the process
	adrp	x1, mask
	add	x1, x1, :lo12:mask
	mov	x2, #1024
	str	x2, [x1]
	mov	x0, #0
	mov	x2, #0
	mov	x3, #8
	mov	x8, #135			// rt_sigprocmask(SIG_BLOCK)
	svc	#0
	mov	x1, #0
	ldr	x0, [x1]
	b	exit0
// setact: rt_sigaction(x0, &act, 0, 8)
setact:
	stp	x29, x30, [sp, #-16]!
	adrp	x1, act
	add	x1, x1, :lo12:act
	mov	x2, #0
	mov	x3, #8
	mov	x8, #134
	svc	#0
	ldp	x29, x30, [sp], #16
	ret
// the handler: x0 the signal, x1 siginfo, x2 the ucontext
handler:
	mov	x24, x30
	mov	x19, x1
	mov	x20, x2
	mov	x21, sp
	bl	put_hex
	ldrsw	x0, [x19, #8]			// si_code
	bl	put_hex
	ldr	x0, [x19, #16]			// si_addr
	bl	put_hex
	ldr	x0, [x20, #176]			// fault_address
	bl	put_hex
	add	x1, x20, #464			// the records: the ESR's, else 0
	mov	x0, #0
1:	ldr	w2, [x1]
	ldr	w3, [x1, #4]
	cbz	w2, 3f
	movz	w4, #0x5201
	movk	w4, #0x4553, lsl #16
	cmp	w2, w4
	b.ne	2f
	ldr	x0, [x1, #8]
2:	add	x1, x1, x3
	b	1b
3:	cbz	x23, 4f
	and	x0, x0, #~0x3f
4:	bl	put_hex
	adrp	x1, altstk
	add	x1, x1, :lo12:altstk
	add	x1, x1, #16384
	sub	x0, x21, x1			// the frame's place below the top: -4704
	bl	put_hex
	ldr	w0, [x20, #24]			// uc_stack.ss_flags
	bl	put_hex
	ldr	x0, [x20, #40]			// uc_sigmask
	bl	put_hex
	ldr	x0, [x20, #448]			// pstate
	bl	put_hex
	mov	x0, #-1				// the mask in the handler
	mov	x1, #0
	adrp	x2, mask
	add	x2, x2, :lo12:mask
	mov	x0, #0
	mov	x3, #8
	mov	x8, #135
	svc	#0
	ldr	x0, [x2]
	bl	put_hex
	ldr	x0, [x20, #440]
	add	x0, x0, #4
	str	x0, [x20, #440]			// pc past the instruction
	ldr	x0, [x20, #360]
	add	x0, x0, #1
	str	x0, [x20, #360]			// x22 + 1
	ret	x24
restorer:
	mov	x8, #139
	svc	#0
	.data
	.p2align	3
act:	.quad	handler, 0x0c000004, restorer, 0	// SA_SIGINFO | SA_ONSTACK | SA_RESTORER
ss:	.quad	0, 0, 0
mask:	.quad	0
	.section	.rodata
	.p2align	12
ro:	.quad	0
	.bss
	.p2align	4
buf:	.space	64
altstk:	.space	16384
