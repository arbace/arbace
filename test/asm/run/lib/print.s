# Arbace's run tests' helpers (AX4), linked by the tests that name them (`# with: lib/print.s`); not a test of its own:
# put_str(rdi = address, rsi = length), put_u64(rdi = value, decimal, then a newline), sys_exit(rdi = status)
	.text
	.globl	put_str
	.type	put_str,@function
put_str:
	mov	%rsi, %rdx
	mov	%rdi, %rsi
	mov	$1, %edi
	mov	$1, %eax
	syscall
	ret
	.size	put_str, .-put_str

	.globl	put_u64
	.type	put_u64,@function
put_u64:
	lea	digits_end(%rip), %rsi
	movb	$10, -1(%rsi)
	lea	-1(%rsi), %rcx
	mov	%rdi, %rax
	mov	$10, %r8d
1:	xor	%edx, %edx
	div	%r8
	add	$'0', %dl
	dec	%rcx
	mov	%dl, (%rcx)
	test	%rax, %rax
	jnz	1b
	mov	%rcx, %rdi
	sub	%rcx, %rsi
	jmp	put_str
	.size	put_u64, .-put_u64

	.globl	sys_exit
	.type	sys_exit,@function
sys_exit:
	mov	$60, %eax
	syscall
	hlt

	.bss
	.p2align 4
digits:	.zero	32
digits_end:
