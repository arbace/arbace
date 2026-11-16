# Arbace's run test (AX4): data relocations: R_X86_64_32, 16 and 8 to small absolutes, PC64 and PC32 differences across
# sections, SIZE32 and SIZE64 of a sized global symbol (a local one is a section symbol to MC, its size 0), GOTOFF64 and GOTPC32 against _GLOBAL_OFFSET_TABLE_ (.got.plt kept for it)
# exit: 0
# stdout: "4660\n52\n18\n1\n1\n24\n24\n1\n"
# with: lib/print.s
	.globl	_start
	small = 0x1234
_start:
	mov	v32(%rip), %edi
	call	put_u64
	movzwl	v16(%rip), %edi
	call	put_u64
	movzbl	v8(%rip), %edi
	call	put_u64
	# PC64: the distance from vpc64 to obj, against the addresses
	lea	obj(%rip), %rax
	lea	vpc64(%rip), %rcx
	sub	%rcx, %rax
	xor	%edi, %edi
	cmp	vpc64(%rip), %rax
	sete	%dil
	call	put_u64
	lea	obj(%rip), %rax
	lea	vpc32(%rip), %rcx
	sub	%rcx, %rax
	movslq	vpc32(%rip), %rcx
	xor	%edi, %edi
	cmp	%rcx, %rax
	sete	%dil
	call	put_u64
	mov	$obj@SIZE, %edi
	call	put_u64
	movabs	$obj@SIZE, %rdi
	call	put_u64
	# GOTOFF64: obj - GOT, and GOT found by GOTPC32: their sum is obj
	lea	_GLOBAL_OFFSET_TABLE_(%rip), %rax
	add	vgotoff(%rip), %rax
	lea	obj(%rip), %rcx
	xor	%edi, %edi
	cmp	%rcx, %rax
	sete	%dil
	call	put_u64
	xor	%edi, %edi
	call	sys_exit
	.data
	.p2align 3
vpc64:	.quad	obj - .
vgotoff: .quad	obj@GOTOFF
v32:	.long	small
vpc32:	.long	obj - .
v16:	.short	0x34
v8:	.byte	0x12
	.section .rodata
	.p2align 3
	.globl	obj
	.type	obj,@object
obj:	.quad	1, 2, 3
	.size	obj, 24
