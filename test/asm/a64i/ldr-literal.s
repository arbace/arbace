# a64i's program (AR5): pc-relative loads: ldr (w, x), ldrsw literal
# with: lib/out.s
# exit: 0
# stdout: "8877665544332211\n"
# stdout: "0000000080000001\n"
# stdout: "ffffffff80000001\n"
# stdout: "0102030405060708\n"
	.text
	.globl	_start
_start:
	ldr x0, lit64
	bl	put_hex
	ldr w0, lit32
	bl	put_hex
	ldrsw x0, lit32
	bl	put_hex
	ldr x0, lit64 + 8
	bl	put_hex
	b	exit0
	.balign	8
lit64:	.xword	0x8877665544332211, 0x0102030405060708
lit32:	.word	0x80000001
	.word	0
	b	exit0
