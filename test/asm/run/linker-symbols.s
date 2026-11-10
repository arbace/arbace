# Arbace's run test (AX4): the symbols the linker defines when referenced: __ehdr_start (the ELF header mapped by the first
# PT_LOAD: its magic read), __bss_start and _end around .bss, _etext, __start_/__stop_ of a section named as a C identifier
# exit: 0
# stdout: "ELF\n1\n1\n24\n1\n"
# with: lib/print.s
	.globl	_start
_start:
	lea	__ehdr_start+1(%rip), %rdi
	movb	$10, nl(%rip)
	mov	$3, %esi
	call	put_str
	lea	nl(%rip), %rdi
	mov	$1, %esi
	call	put_str
	lea	__bss_start(%rip), %rax
	lea	bss_first(%rip), %rcx
	xor	%edi, %edi
	cmp	%rax, %rcx
	sete	%dil
	call	put_u64
	lea	_end(%rip), %rax
	lea	bss_last(%rip), %rcx
	xor	%edi, %edi
	cmp	%rcx, %rax
	seta	%dil
	call	put_u64
	lea	__stop_mytab(%rip), %rdi
	lea	__start_mytab(%rip), %rax
	sub	%rax, %rdi
	call	put_u64
	lea	_etext(%rip), %rax
	lea	_start(%rip), %rcx
	xor	%edi, %edi
	cmp	%rcx, %rax
	seta	%dil
	call	put_u64
	xor	%edi, %edi
	call	sys_exit
	.section mytab,"a",@progbits
	.quad	1, 2, 3
	.data
nl:	.byte	0
	.bss
bss_first:
	.zero	8
bss_last:
	.zero	8
