// Arbace's synthetic object test (AR4): every relocation specifier the object assembler writes, against local, global and
// undefined symbols, in .text and in data
	.text
	.globl	g
l:	b	g
	bl	u
	b.ne	u
	cbz	x0, u
	tbz	x0, #3, u
	ldr	x0, u
	adr	x0, u
	adrp	x0, u
	adrp	x0, :got:u
	ldr	x0, [x0, :got_lo12:u]
	add	x0, x0, :lo12:u
	ldrb	w0, [x0, :lo12:u]
	ldrh	w0, [x0, :lo12:u]
	ldr	w0, [x0, :lo12:u]
	ldr	x0, [x0, :lo12:u]
	ldr	q0, [x0, :lo12:u]
	movz	x0, #:abs_g3:u
	movk	x0, #:abs_g2_nc:u
	movk	x0, #:abs_g1_nc:u
	movk	x0, #:abs_g0_nc:u
	movz	x0, #:abs_g1_s:u
	movz	x0, #:prel_g1:u
	adrp	x0, :gottprel:t
	ldr	x0, [x0, :gottprel_lo12:t]
	add	x0, x0, :tprel_hi12:t
	add	x0, x0, :tprel_lo12_nc:t
	movz	x0, #:tprel_g1:t
	movk	x0, #:tprel_g0_nc:t
g:	b	l
	b	.Lfwd
	nop
.Lfwd:	ret
	.data
	.p2align 3
	.xword	u, l + 8, g
	.word	u, u - .
	.hword	u
	.xword	u - .
	.section .tbss,"awT",@nobits
t:	.zero	8
