# x86i's hand-written program (AX5.4): signal handlers. SIGSEGV (a null load, a store into .rodata, a fetch from .data, hlt's
# general protection fault), SIGFPE (a division by zero), SIGILL (ud2) and SIGTRAP (int3) caught by a handler installed with
# rt_sigaction and a restorer of its own; the handler writes the signal, si_code, si_addr and the mcontext's TRAPNO, ERR, CR2
# and OLDMASK, sets RBX in the mcontext and resumes at a label; rt_sigreturn restores the rest. Then an alternate stack
# (sigaltstack, SA_ONSTACK) and a signal sent by kill while blocked, delivered when rt_sigprocmask unblocks it.
# The verdict: exit 0, stdout the CPU's.
# exit: 0
# judge: cpu
# with: lib/hex.s
	.globl	_start
	.text
_start:
	and	$-16, %rsp
	# the handler for SIGSEGV 11, SIGFPE 8, SIGILL 4, SIGTRAP 5, SIGUSR1 10
	lea	act(%rip), %rax
	lea	handler(%rip), %rcx
	mov	%rcx, (%rax)
	movq	$0x04000004, 8(%rax)		# SA_RESTORER | SA_SIGINFO
	lea	restorer(%rip), %rcx
	mov	%rcx, 16(%rax)
	movq	$0, 24(%rax)
	mov	$11, %edi
	call	install
	mov	$8, %edi
	call	install
	mov	$4, %edi
	call	install
	mov	$5, %edi
	call	install
	mov	$10, %edi
	call	install
	# the old action of SIGSEGV, read back: the handler, its flags
	mov	$13, %eax
	mov	$11, %edi
	xor	%esi, %esi
	lea	old(%rip), %rdx
	mov	$8, %r10d
	syscall
	mov	old(%rip), %rdi
	lea	handler(%rip), %rax
	sub	%rax, %rdi
	mov	old+8(%rip), %rsi
	call	put_hex2

	# the faults, each resumed at the next label with RBX set by the handler
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	xor	%ebx, %ebx
	xor	%eax, %eax
	mov	(%rax), %rcx			# SIGSEGV: a null load
1:	call	report
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	movq	$5, ro(%rip)			# SIGSEGV: a store into .rodata
1:	call	report
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	lea	nx(%rip), %rax
	call	*%rax				# SIGSEGV: a fetch from .data
1:	call	report
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	hlt					# SIGSEGV: #GP
1:	call	report
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	mov	$7, %eax
	xor	%edx, %edx
	xor	%ecx, %ecx
	div	%ecx				# SIGFPE
1:	call	report
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	ud2					# SIGILL
1:	call	report
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	int3					# SIGTRAP
1:	call	report

	# an alternate stack, SIGSEGV with SA_ONSTACK: the handler's RSP inside it
	lea	ss(%rip), %rax
	lea	altstack(%rip), %rcx
	mov	%rcx, (%rax)
	movl	$0, 8(%rax)
	movq	$8192, 16(%rax)
	mov	$131, %eax
	lea	ss(%rip), %rdi
	lea	oldss(%rip), %rsi
	syscall
	mov	%rax, %rdi
	mov	oldss+8(%rip), %rsi		# SS_DISABLE before
	call	put_hex2
	lea	act(%rip), %rax
	movq	$0x0c000004, 8(%rax)		# | SA_ONSTACK
	mov	$11, %edi
	call	install
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	xor	%eax, %eax
	mov	%rax, (%rax)			# SIGSEGV: a null store
1:	call	report
	mov	onalt(%rip), %rdi
	mov	altflags(%rip), %rsi
	call	put_hex2

	# SIGUSR1 blocked, sent by kill, pending; delivered when unblocked
	movq	$0x200, set(%rip)		# 1 << (10 - 1)
	mov	$14, %eax
	xor	%edi, %edi			# SIG_BLOCK
	lea	set(%rip), %rsi
	xor	%edx, %edx
	mov	$8, %r10d
	syscall
	lea	1f(%rip), %rax
	mov	%rax, resume(%rip)
	mov	$39, %eax
	syscall					# getpid
	mov	%rax, %rdi
	mov	$62, %eax
	mov	$10, %esi
	syscall					# kill(getpid(), SIGUSR1): pending
	mov	%rax, %rdi
	mov	count(%rip), %rsi
	call	put_hex2
	mov	$14, %eax
	mov	$1, %edi			# SIG_UNBLOCK: delivered on the way back
	lea	set(%rip), %rsi
	lea	oldset(%rip), %rdx
	mov	$8, %r10d
	syscall
1:	call	report
	mov	oldset(%rip), %rdi
	mov	count(%rip), %rsi
	call	put_hex2
	xor	%edi, %edi
	call	sys_exit

# install(edi = signal): rt_sigaction(signal, act, 0, 8)
install:
	mov	$13, %eax
	lea	act(%rip), %rsi
	xor	%edx, %edx
	mov	$8, %r10d
	syscall
	ret

# report: the handler's RBX (set through the mcontext) and the count of signals handled
report:
	mov	%rbx, %rdi
	mov	count(%rip), %rsi
	jmp	put_hex2

# handler(sig, info, uc): writes what the kernel gave, sets the mcontext's RBX and RIP
handler:
	mov	%rdi, %r12
	mov	%rsi, %r13
	mov	%rdx, %r14
	incq	count(%rip)
	# on the alternate stack?
	lea	altstack(%rip), %rax
	mov	%rsp, %rcx
	sub	%rax, %rcx
	xor	%eax, %eax
	cmp	$8192, %rcx
	setb	%al
	mov	%rax, onalt(%rip)
	mov	24(%r14), %eax			# uc_stack.ss_flags
	mov	%rax, altflags(%rip)
	mov	%r12, %rdi
	movslq	8(%r13), %rsi			# si_code
	call	put_hex2
	mov	16(%r13), %rdi			# si_addr (a code address made relative); a kill's sender's pid not written
	cmp	$10, %r12
	jne	2f
	xor	%edi, %edi
2:	lea	_start(%rip), %rax
	cmp	%rax, %rdi
	jb	1f
	sub	%rax, %rdi
1:	mov	200(%r14), %rsi			# TRAPNO
	call	put_hex2
	mov	192(%r14), %rdi			# ERR
	mov	208(%r14), %rsi			# OLDMASK
	call	put_hex2
	mov	216(%r14), %rdi			# CR2 (a code address made relative)
	lea	_start(%rip), %rax
	cmp	%rax, %rdi
	jb	1f
	sub	%rax, %rdi
1:	mov	(%r14), %rsi			# uc_flags
	call	put_hex2
	# the mask inside the handler
	mov	$14, %eax
	xor	%edi, %edi
	xor	%esi, %esi
	lea	inmask(%rip), %rdx
	mov	$8, %r10d
	syscall
	mov	inmask(%rip), %rdi
	mov	%r12, %rsi
	call	put_hex2
	mov	count(%rip), %rax
	shl	$8, %rax
	or	%r12, %rax
	mov	%rax, 128(%r14)			# RBX
	mov	resume(%rip), %rax
	mov	%rax, 168(%r14)			# RIP (gregs[16] at uc+40+128)
	ret

restorer:
	mov	$15, %eax
	syscall

	.section .rodata
ro:	.quad	1
	.data
nx:	.byte	0xc3
	.bss
	.align	16
act:	.zero	32
old:	.zero	32
ss:	.zero	24
oldss:	.zero	24
set:	.zero	8
oldset:	.zero	8
inmask:	.zero	8
resume:	.zero	8
count:	.zero	8
onalt:	.zero	8
altflags: .zero	8
	.align	16
altstack: .zero	8192
