# a64i's program (AR5): pc-relative addresses: adr, adrp with :lo12:, a label's distance
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "1122334455667788\n"
# stdout: "1122334455667788\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	adr	x1, here
here:	adr	x2, here
	sub	x0, x2, x1
	bl	put_hex
	adrp	x3, value
	add	x3, x3, :lo12:value
	ldr	x0, [x3]
	bl	put_hex
	adrp	x4, value
	ldr	x0, [x4, :lo12:value]
	bl	put_hex
	adr	x5, value
	sub	x0, x5, x3
	bl	put_hex
	adrp	x6, _start
	and	x0, x6, #0xfff
	bl	put_hex
	b	exit0
	.data
	.balign	8
value:	.xword	0x1122334455667788
