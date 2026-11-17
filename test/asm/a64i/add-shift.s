# a64i's program (AR5): add/sub shifted register: lsl, lsr, asr; adds, subs
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
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "1111111111111100\n"
# stdout: "0000000000000000\n"
# stdout: "0000000011111100\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffff9\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffff9\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "7ffffffffffffff8\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffff8\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000007\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000007\n"
# stdout: "0000000000000000\n"
# stdout: "0000000480000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "091a2b3c4d5e6f70\n"
# stdout: "0000000000000000\n"
# stdout: "000000004d5e6f70\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000001d\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000001d\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000002b\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000002b\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000009\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000009\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "00000000c0000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000c0000000\n"
# stdout: "0000000000000008\n"
# stdout: "91a2b3c4d5e6f7f8\n"
# stdout: "0000000000000009\n"
# stdout: "00000000d5e6f7f8\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000078000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000088000000\n"
# stdout: "0000000000000000\n"
# stdout: "12468acf13579bcf\n"
# stdout: "0000000000000000\n"
# stdout: "0000000093579bcf\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "13579be02468ace0\n"
# stdout: "0000000000000000\n"
# stdout: "000000002468ace0\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000002\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000000009\n"
# stdout: "ffffffff7fffffff\n"
# stdout: "000000000000000a\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000002\n"
# stdout: "7fffffff7fffffff\n"
# stdout: "0000000000000002\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000002\n"
# stdout: "8000000080000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000009\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000002\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000002\n"
# stdout: "c000000080000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000000\n"
# stdout: "000000000000000a\n"
# stdout: "d70a3d709abcdef0\n"
# stdout: "0000000000000008\n"
# stdout: "000000009abcdef0\n"
# stdout: "000000000000000a\n"
# stdout: "fffffffe80000005\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000005\n"
# stdout: "0000000000000009\n"
# stdout: "fffffffd80000003\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000003\n"
# stdout: "0000000000000009\n"
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	add x0, x1, x2, lsl #3
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, w2, lsl #3
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	adds x0, x1, x2, lsr #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, w2, lsr #1
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	sub x0, x1, x2, asr #4
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, w2, asr #4
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
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
	subs x0, x1, x2, lsl #31
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, w2, lsl #31
	bl	put_hex
	bl	put_nzcv
	b	exit0
