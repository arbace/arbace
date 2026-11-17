# a64i's program (AR5): tst (immediate, register), mvn, the bic/orn immediates
# with: lib/out.s
# exit: 0
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffff00000000\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "0000000000000004\n"
# stdout: "edcba9876543210f\n"
# stdout: "0000000000000004\n"
# stdout: "0123456789abcdef\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffdf\n"
# stdout: "0000000000000004\n"
# stdout: "000000000000001f\n"
# stdout: "0000000000000004\n"
# stdout: "000000000000001f\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffe00000001f\n"
# stdout: "0000000000000004\n"
# stdout: "ffffffefffffffff\n"
# stdout: "0000000000000004\n"
# stdout: "b97530eca86421ff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000008\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000008\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000004\n"
# stdout: "2468acf13579bdff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffff00\n"
# stdout: "0000000000000008\n"
# stdout: "7fffffffffffff00\n"
# stdout: "0000000000000008\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "00000000ffffff00\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000008\n"
# stdout: "123456789abcde00\n"
# stdout: "0000000000000008\n"
# stdout: "fedcba9876543200\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffff00ff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffff00ff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffff00ff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffff00ff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffdeff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffff32ff\n"
# stdout: "0000000000000008\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
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
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
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
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
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
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
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
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
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
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2
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
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
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
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
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
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
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
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
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
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
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
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #32768, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #12816
	movk	x2, #30292, lsl #16
	movk	x2, #47768, lsl #32
	movk	x2, #65244, lsl #48
	mov	x0, #0x55
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
	bl	put_hex
	bl	put_nzcv
	movz	x1, #5
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #3
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
	bl	put_hex
	bl	put_nzcv
	movz	x1, #3
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #5
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	tst x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	tst w1, w2, lsl #31
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	mvn x0, x1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	mvn x0, x1, lsl #5
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	tst x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	bic x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	orn x0, x1, #0xff00
	bl	put_hex
	bl	put_nzcv
	b	exit0
