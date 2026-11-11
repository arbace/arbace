# a64i's program (AR5): add/sub extended register: uxtb..sxtx with shifts, SP
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "80000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "123456789abcdf00\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "000000000001ffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "800000000001fffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "123456789abd4310\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000000b\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000000d\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000003fffffffd\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "80000003fffffffc\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000003\n"
# stdout: "0000000000000000\n"
# stdout: "0000000280000000\n"
# stdout: "0000000000000000\n"
# stdout: "1234567a740da730\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000011\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000017\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffff9\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "7ffffffffffffff8\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000007\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "123456789abcdf70\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000001d\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000002b\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "fffffffffffffff1\n"
# stdout: "0000000000000008\n"
# stdout: "000000000000000f\n"
# stdout: "0000000000000002\n"
# stdout: "800000000000000f\n"
# stdout: "0000000000000009\n"
# stdout: "7ffffffffffffff0\n"
# stdout: "0000000000000003\n"
# stdout: "000000010000000f\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "123456789abffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000035\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000053\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "123456782468ace0\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000008\n"
# stdout: "fffffffffffffffd\n"
# stdout: "0000000000000008\n"
# stdout: "7ffffffffffffffd\n"
# stdout: "0000000000000008\n"
# stdout: "8000000000000002\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffd\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffff80000000\n"
# stdout: "0000000000000008\n"
# stdout: "147ae147ae147ad0\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "fffffffffffffff9\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "fffffffffffffffd\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000008\n"
# stdout: "8000000000000003\n"
# stdout: "0000000000000008\n"
# stdout: "7ffffffffffffffc\n"
# stdout: "0000000000000008\n"
# stdout: "0000000100000003\n"
# stdout: "0000000000000008\n"
# stdout: "0000000280000000\n"
# stdout: "0000000000000008\n"
# stdout: "0da740da740da730\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000011\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000017\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000008\n"
# stdout: "000000009abcdf00\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffb\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffb\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffb\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000008\n"
# stdout: "000000009abc16b0\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffff9\n"
# stdout: "0000000000000008\n"
# stdout: "00000000ffffffef\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "0000000011111100\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000020\n"
# stdout: "0000000000000006\n"
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxtb
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxth #1
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, uxtw #2
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	add x0, x1, w2, sxtb #3
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	adds x0, x1, w2, sxth #4
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	subs x0, x1, w2, sxtw
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	sub x0, x1, x2, sxtx #1
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add x0, x1, x2, uxtx #2
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	add w0, w1, w2, uxtb
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	sub w0, w1, w2, sxth #2
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
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
	adds w0, w1, w2, uxtw
	bl	put_hex
	bl	put_nzcv
	mov	x3, sp
	mov	x1, #32
	sub	sp, sp, x1, uxtx
	add	x0, sp, #0
	sub	x0, x3, x0
	bl	put_hex
	add	sp, sp, w1, uxtw
	cmp	sp, x3
	bl	put_nzcv
	b	exit0
