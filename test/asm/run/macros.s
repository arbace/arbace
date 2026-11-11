# Arbace's run test (AX11): the object assembler's conditional assembly and macro-like blocks, as llvm-mc's AsmParser expands
# them: .macro with positional, default, keyword and vararg arguments, \@ and \(), a macro invoking another, a macro defined by
# a macro's expansion, .purgem; .rept, .irp, .irpc; .if, .elseif, .else, .ifdef, .ifndef, .ifc, .ifnc, .ifb, .ifnb, nested and
# in an ignored part
# exit: 42
# stdout: "6\n15\n10\n6\n2\n"
# with: lib/print.s
	.globl	_start

	.macro	addto reg, val=1
	add	$\val, \reg
	.endm

	.macro	sum3 reg, a, b, c
	addto	\reg, \a
	addto	\reg, \b
	addto	\reg, \c
	.endm

	.macro	all reg, rest:vararg
	.irp	v, \rest
	addto	\reg, \v
	.endr
	.endm

	# a unique label each time (\@), jumped over
	.macro	skip_one reg
	jmp	over\@
	add	$100, \reg
over\@:
	.endm

	# an instruction's name made of an argument and a suffix (\())
	.macro	opq op, reg
	\op\()q	$1, \reg
	.endm

	# a macro whose expansion defines another
	.macro	define_twice
	.macro	twice reg
	addto	\reg
	addto	\reg
	.endm
	.endm

_start:
	# 1 + 2 + 3
	xor	%ebx, %ebx
	sum3	%rbx, 1, 2, 3
	mov	%rbx, %rdi
	call	put_u64

	# five times 3
	xor	%ebx, %ebx
	.rept	5
	add	$3, %rbx
	.endr
	mov	%rbx, %rdi
	call	put_u64

	# 1 + 2 + 3 by .irp, then 4 by keywords
	xor	%ebx, %ebx
	all	%rbx, 1, 2, 3
	addto	val=4, reg=%rbx
	mov	%rbx, %rdi
	call	put_u64

	# the digits of 123 by .irpc
	xor	%ebx, %ebx
	.irpc	d, 123
	add	$\d, %rbx
	.endr
	mov	%rbx, %rdi
	call	put_u64

	# \@, \(), a macro's macro, .purgem: 0 + 0 + 1 + 1 - 1 + 1 = 2
	xor	%ebx, %ebx
	skip_one %rbx
	skip_one %rbx
	opq	add, %rbx
	define_twice
	twice	%rbx
	opq	sub, %rbx
	.purgem	twice
	.macro	twice reg
	.endm
	twice	%rbx
	mov	%rbx, %rdi
	call	put_u64

	# the conditionals: 42 = 32 + 8 + 2
	xor	%ebx, %ebx
	.if	0
	add	$1000, %rbx
	.elseif	2 - 2
	add	$2000, %rbx
	.elseif	1
	add	$32, %rbx
	.if	0
	.else
	.ifdef	_start
	add	$8, %rbx
	.else
	add	$3000, %rbx
	.endif
	.endif
	.else
	add	$4000, %rbx
	.endif
	.ifndef	nowhere
	.ifc	a,a
	.ifnc	a , b
	.ifb
	add	$1, %rbx
	.endif
	.ifnb	x
	add	$1, %rbx
	.endif
	.endif
	.endif
	.else
	add	$5000, %rbx
	.endif
	.if	0
	.if	1
	add	$6000, %rbx
	.else
	add	$7000, %rbx
	.endif
	.rept	3
	add	$8000, %rbx
	.endr
	.endif
	mov	%rbx, %rdi
	call	sys_exit
