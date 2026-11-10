# Arbace's run test (AR4): .tdata and .tbss in PT_TLS (variant I), the thread pointer set by MSR TPIDR_EL0 to a block of the
# program's own; local exec (ADD's TPREL_HI12 and LO12_NC, MOVW's TPREL_G1 and G0_NC, LDST64's TPREL_LO12_NC) and initial
# exec through a GOT entry (ADR_GOTTPREL_PAGE21, LD64_GOTTPREL_LO12_NC)
# exit: 0
# stdout: "3\n4\n4\n3\n"
# with: lib/print.s
	.globl	_start
_start:
	adrp	x0, block
	add	x0, x0, :lo12:block
	msr	tpidr_el0, x0
	mrs	x19, tpidr_el0
	add	x2, x19, #:tprel_hi12:a, lsl #12
	add	x2, x2, #:tprel_lo12_nc:a
	mov	x3, #3
	str	x3, [x2]
	add	x2, x19, #:tprel_hi12:b, lsl #12
	add	x2, x2, #:tprel_lo12_nc:b
	mov	x3, #4
	str	x3, [x2]
	adrp	x4, :gottprel:a
	ldr	x4, [x4, #:gottprel_lo12:a]
	ldr	x0, [x19, x4]
	bl	put_u64
	ldr	x0, [x19, #:tprel_lo12_nc:b]
	bl	put_u64
	movz	x7, #:tprel_g1:b
	movk	x7, #:tprel_g0_nc:b
	ldr	x0, [x19, x7]
	bl	put_u64
	movz	x7, #:tprel_g1:a
	movk	x7, #:tprel_g0_nc:a
	ldr	x0, [x19, x7]
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
	.section .tdata,"awT",@progbits
	.p2align 3
b:	.xword	9
	.section .tbss,"awT",@nobits
	.p2align 3
a:	.zero	8
	.bss
	.p2align 4
block:	.zero	256
