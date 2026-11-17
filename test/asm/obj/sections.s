# Arbace's synthetic object test (AX4): sections by name, flags and type, the section stack, .ident and .file
	.file	"sections.c"
	.text
	nop
	.section .text.hot,"ax",@progbits
hot:	ret
	.pushsection .rodata.cst8,"aM",@progbits,8
	.quad	1
	.popsection
	nop
	.section .rodata.str1.1,"aMS",@progbits,1
s1:	.asciz	"a string"
s2:	.asciz	"string"
	.previous
	leaq	s1(%rip), %rax
	leaq	s2+1(%rip), %rax
	.section .init_array,"aw"
	.p2align 3
	.quad	hot
	.section .note.GNU-stack,"",@progbits
	.section .tdata,"awT",@progbits
tv:	.long	7
	.section .tbss,"awT",@nobits
tb:	.zero	4
	.text
	movl	%fs:tv@TPOFF, %eax
	.section .data.rel.ro,"aw"
	.quad	hot
	.section .custom,"a",@progbits
	.byte	1
	.section .text.f,"axG",@progbits,f_group,comdat
	.globl	f_group
f_group:
	ret
	.ident	"Arbace AX4"
	.ident	"again"
