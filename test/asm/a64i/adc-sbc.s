# a64i's program (AR5): add/subtract with carry: adc, adcs, sbc, sbcs, ngc, ngcs, a 128-bit sum
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000009\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "8000000000000000\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000100000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000006\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffffd\n"
# stdout: "000000000000000a\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "00000000fffffffd\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000100000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000009\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "8000000000000000\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000100000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000006\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffffd\n"
# stdout: "000000000000000a\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "00000000fffffffd\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffff00000001\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000002\n"
# stdout: "edcba98765432110\n"
# stdout: "0000000000000002\n"
# stdout: "0000000065432110\n"
# stdout: "0000000000000002\n"
# stdout: "0123456789abcdf0\n"
# stdout: "0000000000000002\n"
# stdout: "0000000089abcdf0\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffff00000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "0000000000000008\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000000\n"
# stdout: "edcba9876543210f\n"
# stdout: "0000000000000008\n"
# stdout: "000000006543210f\n"
# stdout: "0000000000000000\n"
# stdout: "0123456789abcdef\n"
# stdout: "0000000000000000\n"
# stdout: "0000000089abcdef\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000006\n"
	.text
	.globl	_start
_start:
	mov	x9, #0
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbc x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbc w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #65535
	movk	x2, #65535, lsl #16
	movk	x2, #65535, lsl #32
	movk	x2, #65535, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sbcs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sbcs w0, w1, w2
	bl	put_hex
	bl	put_nzcv
	mov	x9, #536870912
	msr	nzcv, x9
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ngc x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngc w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	ngcs x0, x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ngcs w0, w1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #1
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	movz	x3, #65535
	movk	x3, #65535, lsl #16
	movk	x3, #65535, lsl #32
	movk	x3, #32767, lsl #48
	movz	x4, #0
	movk	x4, #0, lsl #16
	movk	x4, #0, lsl #32
	movk	x4, #0, lsl #48
	adds	x0, x1, x2
	bl	put_hex
	adc	x0, x3, x4
	bl	put_hex
	bl	put_nzcv
	b	exit0
