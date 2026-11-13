# a64i's program (AR18, queue 285): FEAT_LSE2's alignment, as the M2 (uscat) runs it: an acquire, a release, an exclusive or an
# atomic access that is unaligned but stays within 16 bytes runs (ldar, ldapr, ldapur, stlur, ldxr/stxr, ldaddh, cas); one that
# crosses a 16-byte boundary faults (ldapur x0, [x20, #9]: bytes 9-16): SIGBUS. The buffer holds the bytes 0, 1, ..., 63.
# with: lib/out.s
# exit: 135
# stdout: "0807060504030201\n"
# stdout: "0000000006050403\n"
# stdout: "000000000f0e0d0c\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000feed\n"
# stdout: "0000000000000403\n"
# stdout: "0000000000000504\n"
# stdout: "0000000000000000\n"
# stdout: "1817161514131211\n"
# stdout: "0000000000001918\n"
	.text
	.globl	_start
_start:
	adrp	x20, buf
	add	x20, x20, :lo12:buf
	add	x2, x20, #1
	ldar	x0, [x2]
	bl	put_hex
	add	x2, x20, #3
	ldapr	w0, [x2]
	bl	put_hex
	ldapur	w0, [x20, #12]
	bl	put_hex
	// stlur of a halfword at 33, read back
	mov	w1, #0xfeed
	stlur	wzr, [x20, #36]
	stlurh	w1, [x20, #33]
	ldrh	w0, [x20, #36]
	bl	put_hex
	ldrh	w0, [x20, #33]
	bl	put_hex
	// ldaddh at 3: the old 0x0403, 0x0504 stored
	mov	w1, #0x101
	add	x2, x20, #3
	ldaddh	w1, w0, [x2]
	bl	put_hex
	ldrh	w0, [x2]
	bl	put_hex
	// ldxr/stxr at 17 (bytes 17-24 within 16-31): status 0, the value unchanged
	add	x2, x20, #17
	ldxr	x3, [x2]
	stxr	w0, x3, [x2]
	bl	put_hex
	ldr	x0, [x2]
	bl	put_hex
	// cas at 24 (a halfword): compares 0x1918, swaps it for itself
	add	x2, x20, #24
	mov	w5, #0x1918
	cash	w5, w5, [x2]
	mov	x0, x5
	bl	put_hex
	// crosses 16 bytes: SIGBUS
	ldapur	x0, [x20, #9]
	bl	put_hex
	b	exit0
	.data
	.balign	64
buf:	.byte	0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
	.byte	32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63
