# a64i's program (AR5): acquire/release exclusives and pairs: ldaxr, stlxr, ldxp, stxp, ldaxp, stlxp
# with: lib/out.s
# exit: 0
# stdout: "0123456789abcd10\n"
# stdout: "0123456789abcd11\n"
# stdout: "fedcba9876543212\n"
# stdout: "89abcd1101234567\n"
# stdout: "89abcd1101234500\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
1:	ldaxr	x1, [x20]
	eor	x1, x1, #0xff
	stlxr	w3, x1, [x20]
	cbnz	w3, 1b
	ldr	x0, [x20]
	bl	put_hex
2:	ldxp	x1, x2, [x20]
	add	x1, x1, #1
	add	x2, x2, #2
	stxp	w3, x1, x2, [x20]
	cbnz	w3, 2b
	ldr	x0, [x20]
	bl	put_hex
	ldr	x0, [x20, #8]
	bl	put_hex
3:	ldaxp	w1, w2, [x20]
	stlxp	w3, w2, w1, [x20]
	cbnz	w3, 3b
	ldr	x0, [x20]
	bl	put_hex
4:	ldaxrb	w1, [x20]
	stlxrb	w3, wzr, [x20]
	cbnz	w3, 4b
	ldr	x0, [x20]
	bl	put_hex
	b	exit0
	.data
	.balign	16
buf:	.xword	0x0123456789abcdef, 0xfedcba9876543210, 0x8081828384858687, 0x7f7e7d7c7b7a7978
	.zero	64
