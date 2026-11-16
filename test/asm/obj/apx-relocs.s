# Arbace's synthetic object test (AX4, at the asm branches' merge): the GOT and TLS relocations of REX2- and EVEX-prefixed
# instructions as llvm-mc 23.1.5 chooses them (X86MCCodeEmitter's _rex2 and _evex fixup kinds, X86ELFObjectWriter's CODE_4 and
# CODE_6 types), beside their REX and legacy forms, and `call *a@tlscall(base)`'s zero displacement
	.text
	.globl	g
g:
	movl	foo@GOTPCREL(%rip), %eax
	movl	foo@GOTPCREL(%rip), %r8d
	movl	foo@GOTPCREL(%rip), %r16d
	movq	foo@GOTPCREL(%rip), %rax
	movq	foo@GOTPCREL(%rip), %r17
	addq	foo@GOTPCREL(%rip), %r18
	testq	%r19, foo@GOTPCREL(%rip)
	leaq	foo@GOTPCREL(%rip), %r20
	addq	foo@GOTPCREL(%rip), %rax, %rcx
	movq	tv@GOTTPOFF(%rip), %rax
	movq	tv@GOTTPOFF(%rip), %r21
	addq	tv@GOTTPOFF(%rip), %r22
	addq	tv@GOTTPOFF(%rip), %rax, %rcx
	{nf} addq	tv@GOTTPOFF(%rip), %rcx
	leaq	tv@tlsdesc(%rip), %rax
	call	*tv@tlscall(%rax)
	leaq	tv@tlsdesc(%rip), %r16
	call	*tv@tlscall(%r16)
	ret
