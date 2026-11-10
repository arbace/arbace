# x86i's hand-written programs' helper (AX5.2; doc/asm/AMD64.md §10), linked by the programs that name it (`# with: lib/hex.s`);
# not a program of its own: put_hex2(rdi, rsi) writes both as 16 hex digits, a space between, a newline after;
# put_mem(rdi = address, rsi = length) writes the bytes as hex digits and a newline; sys_exit(rdi = status)
	.text
	.globl	put_hex2
	.type	put_hex2,@function
put_hex2:
	push	%rbx
	lea	line(%rip), %rbx
	mov	%rdi, %rax
	mov	%rbx, %rcx
	call	hex16
	movb	$' ', 16(%rbx)
	mov	%rsi, %rax
	lea	17(%rbx), %rcx
	call	hex16
	movb	$10, 33(%rbx)
	mov	$1, %eax
	mov	$1, %edi
	mov	%rbx, %rsi
	mov	$34, %edx
	syscall
	pop	%rbx
	ret
	.size	put_hex2, .-put_hex2

# hex16: rax's 16 hex digits at rcx (clobbers rdx, r8)
	.type	hex16,@function
hex16:
	mov	$16, %r8d
1:	mov	%rax, %rdx
	and	$15, %edx
	lea	digits(%rip), %r9
	movzbl	(%r9,%rdx), %edx
	mov	%dl, -1(%rcx,%r8)
	shr	$4, %rax
	dec	%r8
	jnz	1b
	ret
	.size	hex16, .-hex16

	.globl	put_mem
	.type	put_mem,@function
put_mem:
	push	%rbx
	push	%r12
	push	%r13
	mov	%rdi, %r12
	mov	%rsi, %r13
	lea	line(%rip), %rbx
2:	test	%r13, %r13
	jz	3f
	movzbl	(%r12), %eax
	mov	%eax, %edx
	shr	$4, %eax
	and	$15, %edx
	lea	digits(%rip), %r9
	movzbl	(%r9,%rax), %eax
	movzbl	(%r9,%rdx), %edx
	mov	%al, (%rbx)
	mov	%dl, 1(%rbx)
	mov	$1, %eax
	mov	$1, %edi
	mov	%rbx, %rsi
	mov	$2, %edx
	syscall
	inc	%r12
	dec	%r13
	jmp	2b
3:	movb	$10, (%rbx)
	mov	$1, %eax
	mov	$1, %edi
	mov	%rbx, %rsi
	mov	$1, %edx
	syscall
	pop	%r13
	pop	%r12
	pop	%rbx
	ret
	.size	put_mem, .-put_mem

	.globl	sys_exit
	.type	sys_exit,@function
sys_exit:
	mov	$60, %eax
	syscall
	hlt

	.section .rodata
digits:	.ascii	"0123456789abcdef"
	.bss
line:	.zero	40
