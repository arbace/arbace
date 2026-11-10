# Arbace's synthetic object test (AX4): the relaxation of branches and of immediates (short against near), labels local and global
	.text
	.globl	f
	.type	f,@function
f:
	jmp	1f
	je	1f
	jne	far
	nop
1:	jmp	1b
	jl	f
	jmp	g
	jmp	undefined_fn
	jg	undefined_fn
	call	local_fn
	call	g
	call	undefined_fn
	call	undefined_fn@PLT
	addq	$sym8, %rax
	addl	$(2f-1b), %eax
	subq	$five, %rsp
	cmpl	$big, %ecx
	pushq	$sym8
	imull	$sym8, %eax, %edx
	xorl	$255, %eax
	andq	$-1, %rbx
2:	.p2align 4
local_fn:
	ret
	.fill	130, 1, 0x90
far:
	jmp	f
	jmp	2b
	je	local_fn
	.size	f, .-f
	.globl	g
g:	ret
	five = 5
	big = 1000
	.p2align 5, 0x90, 7
	jmp	g
	.p2align 3,,2
	ret
