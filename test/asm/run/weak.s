# Arbace's run test (AX4): an undefined weak symbol resolved to zero, a weak definition overridden by a strong one in
# another object, a weak definition kept when nothing overrides it
# exit: 0
# stdout: "0\n2\n3\n"
# with: lib/print.s lib/weak-lib.s
	.weak	missing
	.weak	which
	.weak	alone
	.globl	_start
_start:
	movabs	$missing, %rdi
	call	put_u64
	call	which
	mov	%rax, %rdi
	call	put_u64
	call	alone
	mov	%rax, %rdi
	call	put_u64
	xor	%edi, %edi
	call	sys_exit
which:
	mov	$1, %eax
	ret
alone:
	mov	$3, %eax
	ret
