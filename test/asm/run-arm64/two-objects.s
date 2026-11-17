# Arbace's run test (AR4): relocations across two objects: CALL26 and JUMP26, ADRP with ADD and LDST lo12, ABS64 pointers,
# PREL32 data, the GOT (ADR_GOT_PAGE and LD64_GOT_LO12_NC) for a function, data and an absolute symbol, MOVW_UABS_G0-G3
# building an address
# exit: 0
# stdout: "6\n41\n9\n42\n4660\n9\n9\n42\n"
# with: lib/print.s lib/two-lib.s
	.globl	_start
_start:
	mov	x0, #1
	mov	x1, #2
	mov	x2, #3
	bl	add3
	bl	put_u64
	adrp	x19, counter
	ldr	x0, [x19, :lo12:counter]
	add	x0, x0, #1
	str	x0, [x19, :lo12:counter]
	bl	put_u64
	adrp	x1, ptrs
	add	x1, x1, :lo12:ptrs
	ldr	x9, [x1, #8]
	blr	x9
	bl	put_u64
	adrp	x1, :got:counter
	ldr	x1, [x1, :got_lo12:counter]
	ldr	x0, [x1]
	add	x0, x0, #1
	str	x0, [x1]
	bl	put_u64
	adrp	x0, :got:absval
	ldr	x0, [x0, :got_lo12:absval]
	bl	put_u64
	adrp	x9, :got:ret9
	ldr	x9, [x9, :got_lo12:ret9]
	blr	x9
	bl	put_u64
	adrp	x1, rel
	add	x1, x1, :lo12:rel
	ldrsw	x2, [x1]
	add	x9, x1, x2
	blr	x9
	bl	put_u64
	movz	x1, #:abs_g3:counter
	movk	x1, #:abs_g2_nc:counter
	movk	x1, #:abs_g1_nc:counter
	movk	x1, #:abs_g0_nc:counter
	ldr	x0, [x1]
	bl	put_u64
	b	done
	.globl	done
done:
	mov	x0, #0
	bl	sys_exit
