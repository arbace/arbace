# Arbace's run test (AR4): musl 1.2.6's aarch64 crti and crtn around this object's .init and .fini pieces: _init's prologue,
# this file's code, its epilogue concatenated in link order into one .init, called from _start
# exit: 0
# stdout: "init\nmain\nfini\n"
# before: musl:crt/aarch64/crti.s
# with: lib/print.s musl:crt/aarch64/crtn.s
	.section .init,"ax",@progbits
	adrp	x0, m_init
	add	x0, x0, :lo12:m_init
	bl	put_str
	.section .fini,"ax",@progbits
	adrp	x0, m_fini
	add	x0, x0, :lo12:m_fini
	bl	put_str
	.text
	.globl	_start
_start:
	bl	_init
	adrp	x0, m_main
	add	x0, x0, :lo12:m_main
	bl	put_str
	bl	_fini
	mov	x0, #0
	bl	sys_exit
	.section .rodata
m_init:	.asciz	"init\n"
m_main:	.asciz	"main\n"
m_fini:	.asciz	"fini\n"
