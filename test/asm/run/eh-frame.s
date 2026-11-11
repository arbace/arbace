# Arbace's run test (AX4): functions with CFI in two objects, their .eh_frame merged as lld merges it (one CIE kept for
# both, the FDEs re-pointed to it, their PC-relative pc_begin relocated): the run proves the program's code untouched by it,
# bin/repr-asm link-dump shows the merged section
# exit: 0
# stdout: "12\n"
# with: lib/print.s lib/eh-lib.s
	.globl	_start
	.type	_start,@function
_start:
	.cfi_startproc
	and	$-16, %rsp
	mov	$5, %edi
	call	twice_plus2
	mov	%rax, %rdi
	call	put_u64
	xor	%edi, %edi
	call	sys_exit
	.cfi_endproc
