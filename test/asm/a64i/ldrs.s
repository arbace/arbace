# a64i's program (AR5): sign-extending loads: ldrsb, ldrsh, ldrsw to w and x
# with: lib/out.s
# exit: 0
# stdout: "ffffffffffffff87\n"
# stdout: "00000000ffffff87\n"
# stdout: "0000000000000078\n"
# stdout: "ffffffffffff8081\n"
# stdout: "00000000ffff8081\n"
# stdout: "ffffffff80818283\n"
# stdout: "000000007b7a7978\n"
# stdout: "ffffffffffffff86\n"
# stdout: "000000000000007f\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	mov	x0, #-1
	ldrsb x0, [x20, #16]
	bl	put_hex
	mov	x0, #-1
	ldrsb w0, [x20, #16]
	bl	put_hex
	mov	x0, #-1
	ldrsb x0, [x20, #24]
	bl	put_hex
	mov	x0, #-1
	ldrsh x0, [x20, #22]
	bl	put_hex
	mov	x0, #-1
	ldrsh w0, [x20, #22]
	bl	put_hex
	mov	x0, #-1
	ldrsw x0, [x20, #20]
	bl	put_hex
	mov	x0, #-1
	ldrsw x0, [x20, #24]
	bl	put_hex
	mov	x0, #-1
	ldursb x0, [x20, #17]
	bl	put_hex
	mov	x0, #-1
	ldrsb w0, [x20, #31]!
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
