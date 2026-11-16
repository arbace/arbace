# Arbace's synthetic object test (AX4): the relocations X86ELFObjectWriter chooses by fixup kind and specifier
	.text
	.globl	r
r:
	movq	foo@GOTPCREL(%rip), %rax
	movl	foo@GOTPCREL(%rip), %eax
	addq	foo@GOTPCREL(%rip), %rcx
	movq	foo@GOTPCREL+4(%rip), %rax
	call	*foo@GOTPCREL(%rip)
	jmp	*foo@GOTPCREL(%rip)
	leaq	foo(%rip), %rdi
	leaq	local_data(%rip), %rsi
	leaq	local_data+8(%rip), %rsi
	cmpl	$0, foo(%rip)
	movb	$1, foo(%rip)
	movw	$2, foo+2(%rip)
	movl	$foo, %eax
	movq	$foo, %rax
	movabsq	$foo, %rax
	movl	foo, %eax
	movl	foo(,%rbx,4), %eax
	movl	foo+12(%rbx), %eax
	movq	%fs:foo@TPOFF, %rax
	movq	foo@GOTTPOFF(%rip), %rax
	leaq	foo@TLSGD(%rip), %rdi
	leaq	foo@TLSLD(%rip), %rdi
	movq	foo@DTPOFF(%rax), %rcx
	movl	$foo@SIZE, %eax
	movabsq	$foo@SIZE, %rax
	call	foo@PLT
	movq	r@GOTPCREL(%rip), %rax
	movq	local_data@GOTPCREL(%rip), %rax
	ret
	.data
local_data:
	.quad	0, 0
	.quad	foo@GOTOFF
	.long	foo@PLT-.
	.quad	r
	.quad	local_data
