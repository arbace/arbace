# a64i's program (AR5): move wide: movz, movn, movk and mov's immediate aliases (wide, inverted, bitmask)
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "000000000000ffff\n"
# stdout: "0000000000010000\n"
# stdout: "00000000ffff0000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "fffffffffffffffe\n"
# stdout: "ffffffffffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000123400000000\n"
# stdout: "fffffffffffff0ff\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000ffff\n"
# stdout: "0000000080000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000055555555\n"
# stdout: "00000000ffff1234\n"
# stdout: "1234000000000000\n"
# stdout: "ffffffffedcbffff\n"
# stdout: "00000000ffffedcb\n"
# stdout: "ef010000abcd0001\n"
# stdout: "000000001234ffff\n"
	.text
	.globl	_start
_start:
	mov	x0, #0
	bl	put_hex
	mov	x0, #1
	bl	put_hex
	mov	x0, #0xffff
	bl	put_hex
	mov	x0, #0x10000
	bl	put_hex
	mov	x0, #0xffff0000
	bl	put_hex
	mov	x0, #-1
	bl	put_hex
	mov	x0, #-2
	bl	put_hex
	mov	x0, #-65536
	bl	put_hex
	mov	x0, #0x5555555555555555
	bl	put_hex
	mov	x0, #0x7fffffffffffffff
	bl	put_hex
	mov	x0, #0x123400000000
	bl	put_hex
	mov	x0, #0xfffffffffffff0ff
	bl	put_hex
	mov	x0, #-1
	mov	w0, #0
	bl	put_hex
	mov	x0, #-1
	mov	w0, #0xffff
	bl	put_hex
	mov	x0, #-1
	mov	w0, #0x80000000
	bl	put_hex
	mov	x0, #-1
	mov	w0, #-1
	bl	put_hex
	mov	x0, #-1
	mov	w0, #0xfffffffe
	bl	put_hex
	mov	x0, #-1
	mov	w0, #0x55555555
	bl	put_hex
	mov	x0, #-1
	mov	w0, #0xffff1234
	bl	put_hex
	movz	x0, #0x1234, lsl #48
	bl	put_hex
	movn	x0, #0x1234, lsl #16
	bl	put_hex
	movn	w0, #0x1234
	bl	put_hex
	movz	x0, #1
	movk	x0, #0xabcd, lsl #16
	movk	x0, #0xef01, lsl #48
	bl	put_hex
	mov	x0, #-1
	movk	w0, #0x1234, lsl #16
	bl	put_hex
	b	exit0
