# x86i's hand-written program (AX5.2): the alu, unary, shift and rotate families at the four sizes over twelve edge values,
# each pair and both carries in: the result and the flags the SDM defines for the instruction (pushfq, masked), a line each.
# Its verdict is the CPU's run (AXS9 (a)): the stated exit, stdout the CPU's (`# judge: cpu`). Written by a shell loop over
# OPS x SIZES (doc/asm/AMD64-SURVEY.md, "As built: AX5.2"); edit the loop's output only by regenerating it.
# exit: 0
# judge: cpu
# with: lib/hex.s
	.globl	_start
	.text
_start:
	and	$-16, %rsp
	xor	%r12d, %r12d		# stub index
1:	cmp	$NSTUBS, %r12
	je	9f
	xor	%r13d, %r13d		# i
2:	cmp	$12, %r13
	je	8f
	xor	%r14d, %r14d		# j
3:	cmp	$12, %r14
	je	7f
	xor	%r15d, %r15d		# carry in
4:	cmp	$2, %r15
	je	6f
	lea	vals(%rip), %rax
	mov	(%rax,%r13,8), %rdi
	mov	(%rax,%r14,8), %rsi
	mov	%r15, %rdx
	lea	stubs(%rip), %rax
	call	*(%rax,%r12,8)
	lea	masks(%rip), %rcx
	and	(%rcx,%r12,8), %rdx
	mov	%rax, %rdi
	mov	%rdx, %rsi
	call	put_hex2
	inc	%r15
	jmp	4b
6:	inc	%r14
	jmp	3b
7:	inc	%r13
	jmp	2b
8:	inc	%r12
	jmp	1b
9:	xor	%edi, %edi
	call	sys_exit

add_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add %sil, %al
	pushfq
	pop	%rdx
	ret

adc_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	adc %sil, %al
	pushfq
	pop	%rdx
	ret

sub_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub %sil, %al
	pushfq
	pop	%rdx
	ret

sbb_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sbb %sil, %al
	pushfq
	pop	%rdx
	ret

cmp_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	cmp %sil, %al
	pushfq
	pop	%rdx
	ret

and_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	and %sil, %al
	pushfq
	pop	%rdx
	ret

or_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	or %sil, %al
	pushfq
	pop	%rdx
	ret

xor_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	xor %sil, %al
	pushfq
	pop	%rdx
	ret

test_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	test %sil, %al
	pushfq
	pop	%rdx
	ret

inc_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	inc %al
	pushfq
	pop	%rdx
	ret

dec_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	dec %al
	pushfq
	pop	%rdx
	ret

neg_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	neg %al
	pushfq
	pop	%rdx
	ret

not_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	not %al
	pushfq
	pop	%rdx
	ret

shl_8:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shl %cl, %al
	pushfq
	pop	%rdx
	ret

shr_8:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shr %cl, %al
	pushfq
	pop	%rdx
	ret

sar_8:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	sar %cl, %al
	pushfq
	pop	%rdx
	ret

rol_8:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	rol %cl, %al
	pushfq
	pop	%rdx
	ret

ror_8:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	ror %cl, %al
	pushfq
	pop	%rdx
	ret

shl1_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shl %al
	pushfq
	pop	%rdx
	ret

shr1_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shr %al
	pushfq
	pop	%rdx
	ret

sar1_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sar %al
	pushfq
	pop	%rdx
	ret

rol1_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	rol %al
	pushfq
	pop	%rdx
	ret

addi_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add $-3, %al
	pushfq
	pop	%rdx
	ret

subi_8:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub $0x41, %al
	pushfq
	pop	%rdx
	ret

add_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add %si, %ax
	pushfq
	pop	%rdx
	ret

adc_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	adc %si, %ax
	pushfq
	pop	%rdx
	ret

sub_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub %si, %ax
	pushfq
	pop	%rdx
	ret

sbb_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sbb %si, %ax
	pushfq
	pop	%rdx
	ret

cmp_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	cmp %si, %ax
	pushfq
	pop	%rdx
	ret

and_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	and %si, %ax
	pushfq
	pop	%rdx
	ret

or_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	or %si, %ax
	pushfq
	pop	%rdx
	ret

xor_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	xor %si, %ax
	pushfq
	pop	%rdx
	ret

test_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	test %si, %ax
	pushfq
	pop	%rdx
	ret

inc_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	inc %ax
	pushfq
	pop	%rdx
	ret

dec_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	dec %ax
	pushfq
	pop	%rdx
	ret

neg_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	neg %ax
	pushfq
	pop	%rdx
	ret

not_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	not %ax
	pushfq
	pop	%rdx
	ret

imul_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	imul %si, %ax
	pushfq
	pop	%rdx
	ret

shl_16:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shl %cl, %ax
	pushfq
	pop	%rdx
	ret

shr_16:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shr %cl, %ax
	pushfq
	pop	%rdx
	ret

sar_16:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	sar %cl, %ax
	pushfq
	pop	%rdx
	ret

rol_16:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	rol %cl, %ax
	pushfq
	pop	%rdx
	ret

ror_16:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	ror %cl, %ax
	pushfq
	pop	%rdx
	ret

shl1_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shl %ax
	pushfq
	pop	%rdx
	ret

shr1_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shr %ax
	pushfq
	pop	%rdx
	ret

sar1_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sar %ax
	pushfq
	pop	%rdx
	ret

rol1_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	rol %ax
	pushfq
	pop	%rdx
	ret

addi_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add $-3, %ax
	pushfq
	pop	%rdx
	ret

subi_16:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub $0x41, %ax
	pushfq
	pop	%rdx
	ret

add_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add %esi, %eax
	pushfq
	pop	%rdx
	ret

adc_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	adc %esi, %eax
	pushfq
	pop	%rdx
	ret

sub_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub %esi, %eax
	pushfq
	pop	%rdx
	ret

sbb_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sbb %esi, %eax
	pushfq
	pop	%rdx
	ret

cmp_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	cmp %esi, %eax
	pushfq
	pop	%rdx
	ret

and_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	and %esi, %eax
	pushfq
	pop	%rdx
	ret

or_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	or %esi, %eax
	pushfq
	pop	%rdx
	ret

xor_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	xor %esi, %eax
	pushfq
	pop	%rdx
	ret

test_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	test %esi, %eax
	pushfq
	pop	%rdx
	ret

inc_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	inc %eax
	pushfq
	pop	%rdx
	ret

dec_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	dec %eax
	pushfq
	pop	%rdx
	ret

neg_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	neg %eax
	pushfq
	pop	%rdx
	ret

not_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	not %eax
	pushfq
	pop	%rdx
	ret

imul_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	imul %esi, %eax
	pushfq
	pop	%rdx
	ret

shl_32:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shl %cl, %eax
	pushfq
	pop	%rdx
	ret

shr_32:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shr %cl, %eax
	pushfq
	pop	%rdx
	ret

sar_32:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	sar %cl, %eax
	pushfq
	pop	%rdx
	ret

rol_32:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	rol %cl, %eax
	pushfq
	pop	%rdx
	ret

ror_32:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	ror %cl, %eax
	pushfq
	pop	%rdx
	ret

shl1_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shl %eax
	pushfq
	pop	%rdx
	ret

shr1_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shr %eax
	pushfq
	pop	%rdx
	ret

sar1_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sar %eax
	pushfq
	pop	%rdx
	ret

rol1_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	rol %eax
	pushfq
	pop	%rdx
	ret

addi_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add $-3, %eax
	pushfq
	pop	%rdx
	ret

subi_32:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub $0x41, %eax
	pushfq
	pop	%rdx
	ret

add_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add %rsi, %rax
	pushfq
	pop	%rdx
	ret

adc_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	adc %rsi, %rax
	pushfq
	pop	%rdx
	ret

sub_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub %rsi, %rax
	pushfq
	pop	%rdx
	ret

sbb_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sbb %rsi, %rax
	pushfq
	pop	%rdx
	ret

cmp_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	cmp %rsi, %rax
	pushfq
	pop	%rdx
	ret

and_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	and %rsi, %rax
	pushfq
	pop	%rdx
	ret

or_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	or %rsi, %rax
	pushfq
	pop	%rdx
	ret

xor_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	xor %rsi, %rax
	pushfq
	pop	%rdx
	ret

test_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	test %rsi, %rax
	pushfq
	pop	%rdx
	ret

inc_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	inc %rax
	pushfq
	pop	%rdx
	ret

dec_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	dec %rax
	pushfq
	pop	%rdx
	ret

neg_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	neg %rax
	pushfq
	pop	%rdx
	ret

not_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	not %rax
	pushfq
	pop	%rdx
	ret

imul_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	imul %rsi, %rax
	pushfq
	pop	%rdx
	ret

shl_64:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shl %cl, %rax
	pushfq
	pop	%rdx
	ret

shr_64:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	shr %cl, %rax
	pushfq
	pop	%rdx
	ret

sar_64:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	sar %cl, %rax
	pushfq
	pop	%rdx
	ret

rol_64:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	rol %cl, %rax
	pushfq
	pop	%rdx
	ret

ror_64:
	mov	%rdi, %rax
	mov %rsi, %rcx
	and $7, %ecx
	mov %rdx, %r8
	neg %r8
	ror %cl, %rax
	pushfq
	pop	%rdx
	ret

shl1_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shl %rax
	pushfq
	pop	%rdx
	ret

shr1_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	shr %rax
	pushfq
	pop	%rdx
	ret

sar1_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sar %rax
	pushfq
	pop	%rdx
	ret

rol1_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	rol %rax
	pushfq
	pop	%rdx
	ret

addi_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	add $-3, %rax
	pushfq
	pop	%rdx
	ret

subi_64:
	mov	%rdi, %rax
	mov %rdx, %r8
	neg %r8
	sub $0x41, %rax
	pushfq
	pop	%rdx
	ret

	.equ	NSTUBS, 99
	.section .rodata
	.p2align 3
stubs:
	.quad	add_8
	.quad	adc_8
	.quad	sub_8
	.quad	sbb_8
	.quad	cmp_8
	.quad	and_8
	.quad	or_8
	.quad	xor_8
	.quad	test_8
	.quad	inc_8
	.quad	dec_8
	.quad	neg_8
	.quad	not_8
	.quad	shl_8
	.quad	shr_8
	.quad	sar_8
	.quad	rol_8
	.quad	ror_8
	.quad	shl1_8
	.quad	shr1_8
	.quad	sar1_8
	.quad	rol1_8
	.quad	addi_8
	.quad	subi_8
	.quad	add_16
	.quad	adc_16
	.quad	sub_16
	.quad	sbb_16
	.quad	cmp_16
	.quad	and_16
	.quad	or_16
	.quad	xor_16
	.quad	test_16
	.quad	inc_16
	.quad	dec_16
	.quad	neg_16
	.quad	not_16
	.quad	imul_16
	.quad	shl_16
	.quad	shr_16
	.quad	sar_16
	.quad	rol_16
	.quad	ror_16
	.quad	shl1_16
	.quad	shr1_16
	.quad	sar1_16
	.quad	rol1_16
	.quad	addi_16
	.quad	subi_16
	.quad	add_32
	.quad	adc_32
	.quad	sub_32
	.quad	sbb_32
	.quad	cmp_32
	.quad	and_32
	.quad	or_32
	.quad	xor_32
	.quad	test_32
	.quad	inc_32
	.quad	dec_32
	.quad	neg_32
	.quad	not_32
	.quad	imul_32
	.quad	shl_32
	.quad	shr_32
	.quad	sar_32
	.quad	rol_32
	.quad	ror_32
	.quad	shl1_32
	.quad	shr1_32
	.quad	sar1_32
	.quad	rol1_32
	.quad	addi_32
	.quad	subi_32
	.quad	add_64
	.quad	adc_64
	.quad	sub_64
	.quad	sbb_64
	.quad	cmp_64
	.quad	and_64
	.quad	or_64
	.quad	xor_64
	.quad	test_64
	.quad	inc_64
	.quad	dec_64
	.quad	neg_64
	.quad	not_64
	.quad	imul_64
	.quad	shl_64
	.quad	shr_64
	.quad	sar_64
	.quad	rol_64
	.quad	ror_64
	.quad	shl1_64
	.quad	shr1_64
	.quad	sar1_64
	.quad	rol1_64
	.quad	addi_64
	.quad	subi_64
masks:
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0d5
	.quad	0x0d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x801
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0d5
	.quad	0x0d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x801
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0d5
	.quad	0x0d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x801
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0c5
	.quad	0x0d5
	.quad	0x0d5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8c5
	.quad	0x8d5
	.quad	0x8d5
	.quad	0x8d5
vals:	.quad	0, 1, -1, 0x7f, 0x80, 0x7fff, 0x8000, 0x7fffffff, 0x80000000, 0x7fffffffffffffff, 0x8000000000000000
	.quad	0x123456789abcdef0
