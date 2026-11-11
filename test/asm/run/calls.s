# Arbace's run test (AX4): calls and returns, recursion on the stack (5! = 120), a local function, a call through a register
# exit: 120
# stdout: "120\n3628800\n"
# with: lib/print.s
	.text
	.type	fact,@function
fact:
	cmp	$1, %rdi
	jbe	1f
	push	%rdi
	dec	%rdi
	call	fact
	pop	%rdi
	imul	%rdi, %rax
	ret
1:	mov	$1, %eax
	ret
	.size	fact, .-fact

	.globl	_start
_start:
	and	$-16, %rsp
	mov	$5, %edi
	call	fact
	mov	%rax, %rbx
	mov	%rax, %rdi
	call	put_u64
	lea	fact(%rip), %r12
	mov	$10, %edi
	call	*%r12
	mov	%rax, %rdi
	call	put_u64
	mov	%rbx, %rdi
	call	sys_exit
