# Arbace's run test (AX4): musl 1.2.6's x86-64 setjmp and longjmp (src/setjmp/x86_64): a nonlocal return through two frames
# exit: 3
# stdout: "0\n5\n5\n"
# with: lib/print.s musl:src/setjmp/x86_64/setjmp.s musl:src/setjmp/x86_64/longjmp.s
	.globl	_start
_start:
	and	$-16, %rsp
	mov	$3, %ebx
	lea	jb(%rip), %rdi
	call	setjmp
	mov	%rax, %r12
	mov	%rax, %rdi
	call	put_u64
	test	%r12, %r12
	jnz	1f
	call	deeper
	mov	$9, %edi
	call	sys_exit
1:	mov	%rbx, %rdi
	call	sys_exit
deeper:
	push	%rbp
	mov	$77, %ebx
	call	deepest
	pop	%rbp
	ret
deepest:
	mov	$5, %edi
	call	put_u64
	lea	jb(%rip), %rdi
	mov	$5, %esi
	call	longjmp
	.bss
	.p2align 3
jb:	.zero	200
