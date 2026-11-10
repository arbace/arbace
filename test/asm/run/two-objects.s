# Arbace's run test (AX4): relocations across two objects: PLT32 calls, PC32 data, R_X86_64_64 pointers, R_X86_64_32S
# absolutes, GOTPCRELX relaxed (mov to lea, call and jmp through the GOT to direct ones, add to an immediate), an absolute
# symbol through the GOT (an entry kept, as lld keeps it), GOTPCREL with an addend (not relaxed)
# exit: 0
# stdout: "6\n41\n9\n42\n4660\n9\n4660\n42\n"
# with: lib/print.s lib/two-lib.s
	.globl	_start
_start:
	mov	$1, %edi
	mov	$2, %esi
	mov	$3, %edx
	call	add3@PLT
	mov	%rax, %rdi
	call	put_u64
	incq	counter(%rip)
	mov	counter(%rip), %rdi
	call	put_u64
	mov	ptrs+8(%rip), %rax
	call	*%rax
	mov	%rax, %rdi
	call	put_u64
	movq	ret9@GOTPCREL(%rip), %rax
	mov	counter@GOTPCREL(%rip), %rcx
	incq	(%rcx)
	mov	(%rcx), %rdi
	call	put_u64
	mov	absval@GOTPCREL(%rip), %rdi
	call	put_u64
	call	*ret9@GOTPCREL(%rip)
	mov	%rax, %rdi
	call	put_u64
	xor	%eax, %eax
	addq	absval@GOTPCREL(%rip), %rax
	mov	$absval, %edi
	call	put_u64
	movq	counter@GOTPCREL+0(%rip), %rax
	mov	(%rax), %rdi
	call	put_u64
	jmp	*done@GOTPCREL(%rip)
	.globl	done
done:
	xor	%edi, %edi
	call	sys_exit
