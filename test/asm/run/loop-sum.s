# Arbace's run test (AX4): a counted loop, backward branches short and near, a decimal print through a second object
# exit: 55
# stdout: "55\n5050\n"
# with: lib/print.s
	.globl	_start
_start:
	xor	%ebx, %ebx
	mov	$10, %ecx
1:	add	%rcx, %rbx
	loop	1b
	mov	%rbx, %rdi
	call	put_u64
	xor	%eax, %eax
	mov	$1, %ecx
2:	add	%rcx, %rax
	inc	%rcx
	.fill	130, 1, 0x90
	cmp	$100, %rcx
	jbe	2b
	mov	%rax, %rdi
	call	put_u64
	mov	%rbx, %rdi
	call	sys_exit
