# Arbace's run test (AX4): thread-local storage in a static executable: .tdata and .tbss in PT_TLS, the thread pointer set
# by arch_prctl(ARCH_SET_FS) to a block made from the TLS image (variant II: the block below the TCB), TPOFF32 and GOTTPOFF
# relaxed to the local-exec form as lld relaxes it (movq x@GOTTPOFF(%rip), %reg into movq $off, %reg)
# exit: 0
# stdout: "11\n22\n0\n33\n"
# with: lib/print.s
	.globl	_start
_start:
	and	$-16, %rsp
	# the TLS block: the image (.tdata's 16 bytes, .tbss's 8) at tls_area, the TCB at tls_area+24 pointing at itself
	lea	tls_area(%rip), %rdi
	movq	$11, (%rdi)
	movq	$22, 8(%rdi)
	movq	$0, 16(%rdi)
	lea	24(%rdi), %rsi
	mov	%rsi, (%rsi)
	mov	$158, %eax
	mov	$0x1002, %edi
	syscall
	test	%rax, %rax
	jnz	1f
	mov	%fs:t1@TPOFF, %rdi
	call	put_u64
	movq	t2@GOTTPOFF(%rip), %rax
	mov	%fs:(%rax), %rdi
	call	put_u64
	mov	%fs:0, %rax
	mov	t3@TPOFF(%rax), %rdi
	call	put_u64
	movq	$33, %fs:t3@TPOFF
	movq	t3@GOTTPOFF(%rip), %rcx
	mov	%fs:(%rcx), %rdi
	call	put_u64
	xor	%edi, %edi
	call	sys_exit
1:	mov	$2, %edi
	call	sys_exit
	.section .tdata,"awT",@progbits
	.p2align 3
	.globl	t1
t1:	.quad	11
t2:	.quad	22
	.section .tbss,"awT",@nobits
	.p2align 3
t3:	.zero	8
	.bss
	.p2align 4
tls_area:
	.zero	64
