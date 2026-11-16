# Arbace's run test (AX4): musl 1.2.6's crti and crtn around this object's .init and .fini pieces: _init's prologue,
# this file's code, its epilogue concatenated in link order into one .init, called from _start
# exit: 0
# stdout: "init\nmain\nfini\n"
# before: musl:crt/x86_64/crti.s
# with: lib/print.s musl:crt/x86_64/crtn.s
	.section .init,"ax",@progbits
	lea	m_init(%rip), %rdi
	mov	$5, %esi
	call	put_str
	.section .fini,"ax",@progbits
	lea	m_fini(%rip), %rdi
	mov	$5, %esi
	call	put_str
	.text
	.globl	_start
_start:
	and	$-16, %rsp
	call	_init
	lea	m_main(%rip), %rdi
	mov	$5, %esi
	call	put_str
	call	_fini
	xor	%edi, %edi
	call	sys_exit
	.section .rodata
m_init:	.ascii	"init\n"
m_main:	.ascii	"main\n"
m_fini:	.ascii	"fini\n"
