# Arbace's run test (AX4): initialized data summed, a .bss buffer filled and written, a common symbol, .data.* and .bss.*
# sections merged into .data and .bss, absolute and RIP-relative addressing of them (R_X86_64_32S, PC32)
# exit: 0
# stdout: "150\nabcdefghij\n7\n"
# with: lib/print.s
	.globl	_start
_start:
	xor	%eax, %eax
	xor	%ecx, %ecx
1:	add	table(,%rcx,8), %rax
	inc	%ecx
	cmp	$5, %ecx
	jne	1b
	mov	%rax, %rdi
	call	put_u64
	lea	buf(%rip), %rdi
	xor	%ecx, %ecx
	mov	$'a', %al
2:	mov	%al, (%rdi,%rcx)
	inc	%al
	inc	%ecx
	cmp	$10, %ecx
	jne	2b
	movb	$10, buf+10
	mov	$11, %esi
	call	put_str
	movq	$7, common_var(%rip)
	mov	common_var, %rdi
	call	put_u64
	cmpq	$0, zeros+64
	jne	3f
	xor	%edi, %edi
	call	sys_exit
3:	mov	$1, %edi
	call	sys_exit

	.section .data.table,"aw",@progbits
	.p2align 3
table:	.quad	10, 20, 30, 40, 50
	.section .bss.buf,"aw",@nobits
buf:	.zero	16
	.bss
zeros:	.zero	128
	.comm	common_var, 8, 8
