# x86i's hand-written program (AX5.2): mul, imul (one, two and three operands), div and idiv at the four sizes over twelve
# edge values: RAX, RDX and the flags the SDM defines (CF and OF for the multiplies, none for the divides), two lines a case; a
# divisor whose low bits are zero (or -1 for idiv) is skipped (#DE is faults.s's). The verdict: exit 0, stdout the CPU's.
# Written by a shell loop over OPS x SIZES (doc/asm/AMD64-SURVEY.md, "As built: AX5.2").
# exit: 0
# judge: cpu
# step-limit: 500000 (AX5.3: the step mode compares the first 500,000 instructions, the sys mode the rest)
# with: lib/hex.s
	.globl	_start
	.text
_start:
	and	$-16, %rsp
	xor	%r12d, %r12d
1:	cmp	$NSTUBS, %r12
	je	9f
	xor	%r13d, %r13d
2:	cmp	$12, %r13
	je	8f
	xor	%r14d, %r14d
3:	cmp	$12, %r14
	je	7f
	lea	vals(%rip), %rax
	mov	(%rax,%r13,8), %rdi
	mov	(%rax,%r14,8), %rsi
	lea	stubs(%rip), %rax
	call	*(%rax,%r12,8)
	lea	masks(%rip), %rcx
	and	(%rcx,%r12,8), %r10
	mov	%r10, %rbx
	mov	%rax, %rdi
	mov	%rdx, %rsi
	call	put_hex2
	mov	%rbx, %rdi
	mov	%r12, %rsi
	call	put_hex2
	inc	%r14
	jmp	3b
7:	inc	%r13
	jmp	2b
8:	inc	%r12
	jmp	1b
9:	xor	%edi, %edi
	call	sys_exit

mul_8:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	mul %sil
	pushfq
	pop	%r10
	ret

imul1_8:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %sil
	pushfq
	pop	%r10
	ret

div_8:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %sil, %sil
	jz 1f
	movzbl %al, %eax
	div %sil
	1:
	pushfq
	pop	%r10
	ret

idiv_8:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %sil, %sil
	jz 1f
	cmp $-1, %sil
	je 1f
	cbw
	idiv %sil
	1:
	pushfq
	pop	%r10
	ret

mul_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	mul %si
	pushfq
	pop	%r10
	ret

imul1_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %si
	pushfq
	pop	%r10
	ret

imul2_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %si, %ax
	pushfq
	pop	%r10
	ret

imul3_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul $-7, %si, %ax
	pushfq
	pop	%r10
	ret

imul3b_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul $1000, %si, %ax
	pushfq
	pop	%r10
	ret

div_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %si, %si
	jz 1f
	xor %edx, %edx
	div %si
	1:
	pushfq
	pop	%r10
	ret

idiv_16:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %si, %si
	jz 1f
	cmp $-1, %si
	je 1f
	cwd
	idiv %si
	1:
	pushfq
	pop	%r10
	ret

mul_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	mul %esi
	pushfq
	pop	%r10
	ret

imul1_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %esi
	pushfq
	pop	%r10
	ret

imul2_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %esi, %eax
	pushfq
	pop	%r10
	ret

imul3_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul $-7, %esi, %eax
	pushfq
	pop	%r10
	ret

imul3b_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul $1000, %esi, %eax
	pushfq
	pop	%r10
	ret

div_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %esi, %esi
	jz 1f
	xor %edx, %edx
	div %esi
	1:
	pushfq
	pop	%r10
	ret

idiv_32:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %esi, %esi
	jz 1f
	cmp $-1, %esi
	je 1f
	cdq
	idiv %esi
	1:
	pushfq
	pop	%r10
	ret

mul_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	mul %rsi
	pushfq
	pop	%r10
	ret

imul1_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %rsi
	pushfq
	pop	%r10
	ret

imul2_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul %rsi, %rax
	pushfq
	pop	%r10
	ret

imul3_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul $-7, %rsi, %rax
	pushfq
	pop	%r10
	ret

imul3b_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	imul $1000, %rsi, %rax
	pushfq
	pop	%r10
	ret

div_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %rsi, %rsi
	jz 1f
	xor %edx, %edx
	div %rsi
	1:
	pushfq
	pop	%r10
	ret

idiv_64:
	mov	%rdi, %rax
	mov	$0x5555, %edx
	test %rsi, %rsi
	jz 1f
	cmp $-1, %rsi
	je 1f
	cqo
	idiv %rsi
	1:
	pushfq
	pop	%r10
	ret

	.equ	NSTUBS, 25
	.section .rodata
	.p2align 3
stubs:
	.quad	mul_8
	.quad	imul1_8
	.quad	div_8
	.quad	idiv_8
	.quad	mul_16
	.quad	imul1_16
	.quad	imul2_16
	.quad	imul3_16
	.quad	imul3b_16
	.quad	div_16
	.quad	idiv_16
	.quad	mul_32
	.quad	imul1_32
	.quad	imul2_32
	.quad	imul3_32
	.quad	imul3b_32
	.quad	div_32
	.quad	idiv_32
	.quad	mul_64
	.quad	imul1_64
	.quad	imul2_64
	.quad	imul3_64
	.quad	imul3b_64
	.quad	div_64
	.quad	idiv_64
masks:
	.quad	0x801
	.quad	0x801
	.quad	0
	.quad	0
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0
	.quad	0
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0
	.quad	0
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0x801
	.quad	0
	.quad	0
vals:	.quad	0, 1, -1, 0x7f, 0x80, 0x7fff, 0x8000, 0x7fffffff, 0x80000000, 0x7fffffffffffffff, 0x8000000000000000
	.quad	0x123456789abcdef0
