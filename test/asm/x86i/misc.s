# x86i's hand-written program (AX5.2): the mov, stack, branch, conv, atomic and misc families: cmov and set over every condition
# after compares of edge values, xchg, xadd, cmpxchg, bswap, movzx, movsx, movsxd, the high byte registers, cbw to cqo, lahf, sahf,
# clc, stc, cmc, pushfq, popfq, loop, loope, loopne, jrcxz, push and pop of memory, leave, ret imm, lea with an index and with a
# 32-bit address, movabs to and from an absolute address, and brk, mmap, mprotect, munmap. The verdict: exit 0, stdout the CPU's.
# exit: 0
# judge: cpu
# with: lib/hex.s
	.globl	_start
	.text
_start:
	and	$-16, %rsp
	# conditions: for each pair (a, b) of vals, cmp a, b at 64 and 32 bits, then all sixteen setcc as a bit mask
	xor	%r12d, %r12d
1:	cmp	$6, %r12
	je	5f
	xor	%r13d, %r13d
2:	cmp	$6, %r13
	je	4f
	lea	vals(%rip), %rax
	mov	(%rax,%r12,8), %rdi
	mov	(%rax,%r13,8), %rsi
	cmp	%rsi, %rdi
	call	conds
	mov	%rax, %rbx
	lea	vals(%rip), %rax
	mov	(%rax,%r12,8), %rdi
	mov	(%rax,%r13,8), %rsi
	cmp	%esi, %edi
	call	conds
	mov	%rax, %rdi
	mov	%rbx, %rsi
	call	put_hex2
	# cmov at 64 and 32 bits after the compare: taken or not, and the 32-bit form's zeroed upper half
	lea	vals(%rip), %rax
	mov	(%rax,%r12,8), %rdi
	mov	(%rax,%r13,8), %rsi
	mov	$-1, %r8
	mov	$-1, %r9
	mov	$0x1111, %r10
	cmp	%rsi, %rdi
	cmovl	%r10, %r8
	cmovbe	%r10d, %r9d
	mov	%r8, %rdi
	mov	%r9, %rsi
	call	put_hex2
	inc	%r13
	jmp	2b
4:	inc	%r12
	jmp	1b
5:
	# xchg, xadd, cmpxchg
	mov	$0x1122334455667788, %rax
	mov	$0x99, %rbx
	xchg	%rax, %rbx
	xchg	%bl, %bh
	xchg	%eax, %ecx
	mov	%rbx, %rdi
	mov	%rcx, %rsi
	call	put_hex2
	movq	$5, word(%rip)
	mov	$7, %eax
	xadd	%rax, word(%rip)
	mov	word(%rip), %rdi
	mov	%rax, %rsi
	call	put_hex2
	mov	$12, %eax
	mov	$99, %ecx
	lock cmpxchg %rcx, word(%rip)
	pushfq
	mov	word(%rip), %rdi
	mov	%rax, %rsi
	call	put_hex2
	pop	%rdi
	and	$0x8d5, %rdi
	mov	$99, %eax
	mov	$100, %ecx
	cmpxchg	%rcx, word(%rip)
	pushfq
	pop	%rsi
	and	$0x8d5, %rsi
	call	put_hex2
	mov	word(%rip), %rdi
	mov	%rax, %rsi
	call	put_hex2
	# bswap, extensions, the high bytes
	mov	$0x0102030405060708, %rax
	bswap	%rax
	mov	%rax, %rdi
	mov	$0x0a0b0c0d, %ecx
	bswap	%ecx
	mov	%rcx, %rsi
	call	put_hex2
	mov	$0x8180, %eax
	movzbl	%al, %edi
	movsbq	%al, %rsi
	call	put_hex2
	mov	$0x8180, %eax
	movzwl	%ax, %edi
	movswq	%ax, %rsi
	call	put_hex2
	mov	$0x80000000, %eax
	movslq	%eax, %rdi
	movsbw	%ah, %si
	call	put_hex2
	mov	$0x1234, %eax
	mov	%ah, %bl
	mov	%al, %bh
	add	$0x10, %ah
	movzbl	%bl, %edi
	mov	%rax, %rsi
	call	put_hex2
	# cbw, cwde, cdqe, cwd, cdq, cqo
	mov	$0x80, %eax
	cbw
	mov	%rax, %rdi
	mov	$0xffff8000, %eax
	cwde
	mov	%rax, %rsi
	call	put_hex2
	mov	$0x80000000, %eax
	cdqe
	mov	%rax, %rdi
	mov	$0x8000, %eax
	cwd
	mov	%rdx, %rsi
	call	put_hex2
	mov	$-5, %eax
	cdq
	mov	%rdx, %rdi
	mov	$-5, %rax
	cqo
	mov	%rdx, %rsi
	call	put_hex2
	# lahf, sahf, clc, stc, cmc, pushfq, popfq
	mov	$1, %eax
	cmp	$2, %eax
	lahf
	movzbl	%ah, %edi
	mov	$0xd5, %ah
	sahf
	pushfq
	pop	%rsi
	and	$0x8d5, %rsi
	call	put_hex2
	stc
	cmc
	pushfq
	pop	%rdi
	and	$0x8d5, %rdi
	clc
	stc
	pushfq
	pop	%rsi
	and	$0x8d5, %rsi
	call	put_hex2
	pushq	$0x8d5
	popfq
	pushfq
	pop	%rdi
	and	$0x8d5, %rdi
	xor	%esi, %esi
	call	put_hex2
	# loop, loope, loopne, jrcxz
	mov	$10, %ecx
	xor	%eax, %eax
6:	add	%rcx, %rax
	loop	6b
	mov	%rax, %rdi
	mov	$5, %ecx
	xor	%eax, %eax
7:	inc	%eax
	cmp	$3, %eax
	loopne	7b
	mov	%rcx, %rsi
	call	put_hex2
	mov	$5, %ecx
	xor	%eax, %eax
8:	inc	%eax
	cmp	%eax, %eax
	loope	8b
	mov	%rax, %rdi
	xor	%esi, %esi
	xor	%ecx, %ecx
	jrcxz	9f
	mov	$1, %esi
9:	call	put_hex2
	# push and pop of memory, leave, ret imm
	movq	$0x4242, word(%rip)
	pushq	word(%rip)
	popq	word2(%rip)
	mov	word2(%rip), %rdi
	mov	%rsp, %rbx
	push	%rbp
	mov	%rsp, %rbp
	sub	$64, %rsp
	leave
	mov	%rsp, %rsi
	sub	%rbx, %rsi
	call	put_hex2
	mov	%rsp, %rbx
	pushq	$1
	pushq	$2
	call	retimm
	mov	%rsp, %rdi
	sub	%rbx, %rdi
	mov	%rax, %rsi
	call	put_hex2
	# lea with an index and a scale, and with a 32-bit address
	mov	$0x100000010, %rax
	mov	$3, %rcx
	lea	-8(%rax,%rcx,4), %rdi
	lea	0x10(%eax,%ecx,8), %rsi
	call	put_hex2
	# movabs from and to an absolute address
	movabs	absword, %rax
	mov	%rax, %rdi
	mov	$0x7777, %rax
	movabs	%rax, absword
	mov	absword(%rip), %rsi
	call	put_hex2
	# brk: grow by 3 pages, write, shrink; mmap anonymous, mprotect, munmap
	mov	$12, %eax
	xor	%edi, %edi
	syscall
	mov	%rax, %rbx
	lea	0x3000(%rbx), %rdi
	mov	$12, %eax
	syscall
	sub	%rbx, %rax
	movq	$0x55, 0x2ff8(%rbx)
	mov	0x2ff8(%rbx), %rdi
	mov	%rax, %rsi
	call	put_hex2
	mov	%rbx, %rdi
	mov	$12, %eax
	syscall
	sub	%rbx, %rax
	mov	%rax, %rdi
	xor	%esi, %esi
	call	put_hex2
	mov	$9, %eax
	xor	%edi, %edi
	mov	$8192, %esi
	mov	$3, %edx
	mov	$0x22, %r10d
	mov	$-1, %r8
	xor	%r9d, %r9d
	syscall
	mov	%rax, %rbx
	movq	$0x66, 4096(%rbx)
	mov	$10, %eax
	mov	%rbx, %rdi
	mov	$4096, %esi
	mov	$1, %edx
	syscall
	mov	%rax, %rdi
	mov	4096(%rbx), %rsi
	call	put_hex2
	mov	$11, %eax
	mov	%rbx, %rdi
	mov	$8192, %esi
	syscall
	mov	%rax, %rdi
	mov	%rbx, %rsi
	and	$0xfff, %rsi
	call	put_hex2
	xor	%edi, %edi
	call	sys_exit

retimm:
	mov	$0x33, %eax
	ret	$16

# conds: the sixteen conditions of the flags as bits 0-15 of rax
conds:
	pushfq
	call	more
	ret
more:
	# the flags at the caller's pushfq: test each condition again on them
	mov	8(%rsp), %rdx
	xor	%r8d, %r8d
	mov	$15, %r9d
10:	push	%rdx
	popfq
	lea	setcc_table(%rip), %r10
	call	*(%r10,%r9,8)
	shl	$1, %r8
	or	%rcx, %r8
	dec	%r9
	jns	10b
	mov	%r8, %rax
	ret	$8
set_o:
	seto	%cl
	movzbl	%cl, %ecx
	ret
set_no:
	setno	%cl
	movzbl	%cl, %ecx
	ret
set_b:
	setb	%cl
	movzbl	%cl, %ecx
	ret
set_ae:
	setae	%cl
	movzbl	%cl, %ecx
	ret
set_e:
	sete	%cl
	movzbl	%cl, %ecx
	ret
set_ne:
	setne	%cl
	movzbl	%cl, %ecx
	ret
set_be:
	setbe	%cl
	movzbl	%cl, %ecx
	ret
set_a:
	seta	%cl
	movzbl	%cl, %ecx
	ret
set_s:
	sets	%cl
	movzbl	%cl, %ecx
	ret
set_ns:
	setns	%cl
	movzbl	%cl, %ecx
	ret
set_p:
	setp	%cl
	movzbl	%cl, %ecx
	ret
set_np:
	setnp	%cl
	movzbl	%cl, %ecx
	ret
set_l:
	setl	%cl
	movzbl	%cl, %ecx
	ret
set_ge:
	setge	%cl
	movzbl	%cl, %ecx
	ret
set_le:
	setle	%cl
	movzbl	%cl, %ecx
	ret
set_g:
	setg	%cl
	movzbl	%cl, %ecx
	ret
	.section .rodata
	.p2align 3
setcc_table:
	.quad	set_o
	.quad	set_no
	.quad	set_b
	.quad	set_ae
	.quad	set_e
	.quad	set_ne
	.quad	set_be
	.quad	set_a
	.quad	set_s
	.quad	set_ns
	.quad	set_p
	.quad	set_np
	.quad	set_l
	.quad	set_ge
	.quad	set_le
	.quad	set_g
vals:	.quad	0, 1, -1, 0x7fffffff, 0x80000000, 0x8000000000000000
	.data
	.p2align 3
word:	.quad	0
word2:	.quad	0
absword: .quad	0x1234
