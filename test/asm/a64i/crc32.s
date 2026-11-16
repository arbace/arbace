# a64i's program (AR18, queue 285): FEAT_CRC32 against the published check values: CRC-32 (0xcbf43926) and CRC-32C
# (0xe3069283) of "123456789", the accumulator 0xffffffff and the result inverted, by bytes (crc32b, crc32cb), halfwords
# (crc32h, crc32ch, the last byte by crc32b), words (crc32w, crc32cw) and a doubleword (crc32x, crc32cx), the rest by bytes
# with: lib/out.s
# exit: 0
# stdout: "00000000cbf43926\n"
# stdout: "00000000cbf43926\n"
# stdout: "00000000cbf43926\n"
# stdout: "00000000cbf43926\n"
# stdout: "00000000e3069283\n"
# stdout: "00000000e3069283\n"
# stdout: "00000000e3069283\n"
# stdout: "00000000e3069283\n"
	.text
	.globl	_start
_start:
	adrp	x20, msg
	add	x20, x20, :lo12:msg
	// CRC-32 by bytes
	mov	w0, #-1
	mov	x2, #0
1:	ldrb	w1, [x20, x2]
	crc32b	w0, w0, w1
	add	x2, x2, #1
	cmp	x2, #9
	b.ne	1b
	mvn	w0, w0
	bl	put_hex
	// by halfwords
	mov	w0, #-1
	mov	x2, #0
2:	ldrh	w1, [x20, x2]
	crc32h	w0, w0, w1
	add	x2, x2, #2
	cmp	x2, #8
	b.ne	2b
	ldrb	w1, [x20, #8]
	crc32b	w0, w0, w1
	mvn	w0, w0
	bl	put_hex
	// by words
	mov	w0, #-1
	ldr	w1, [x20]
	crc32w	w0, w0, w1
	ldr	w1, [x20, #4]
	crc32w	w0, w0, w1
	ldrb	w1, [x20, #8]
	crc32b	w0, w0, w1
	mvn	w0, w0
	bl	put_hex
	// by a doubleword
	mov	w0, #-1
	ldr	x1, [x20]
	crc32x	w0, w0, x1
	ldrb	w1, [x20, #8]
	crc32b	w0, w0, w1
	mvn	w0, w0
	bl	put_hex
	// CRC-32C by bytes
	mov	w0, #-1
	mov	x2, #0
3:	ldrb	w1, [x20, x2]
	crc32cb	w0, w0, w1
	add	x2, x2, #1
	cmp	x2, #9
	b.ne	3b
	mvn	w0, w0
	bl	put_hex
	// by halfwords
	mov	w0, #-1
	mov	x2, #0
4:	ldrh	w1, [x20, x2]
	crc32ch	w0, w0, w1
	add	x2, x2, #2
	cmp	x2, #8
	b.ne	4b
	ldrb	w1, [x20, #8]
	crc32cb	w0, w0, w1
	mvn	w0, w0
	bl	put_hex
	// by words
	mov	w0, #-1
	ldr	w1, [x20]
	crc32cw	w0, w0, w1
	ldr	w1, [x20, #4]
	crc32cw	w0, w0, w1
	ldrb	w1, [x20, #8]
	crc32cb	w0, w0, w1
	mvn	w0, w0
	bl	put_hex
	// by a doubleword
	mov	w0, #-1
	ldr	x1, [x20]
	crc32cx	w0, w0, x1
	ldrb	w1, [x20, #8]
	crc32cb	w0, w0, w1
	mvn	w0, w0
	bl	put_hex
	b	exit0
	.data
	.balign	16
msg:	.ascii	"123456789"
