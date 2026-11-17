# a64i's program (AR5): add/sub immediate: add, adds, sub, subs with lsl #12, on SP
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "123456789abcdef0\n"
# stdout: "0000000000000000\n"
# stdout: "000000009abcdef0\n"
# stdout: "0000000000000000\n"
# stdout: "fedcba9876543210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000076543210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000fff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000fff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000ffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000ffe\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000ffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000ffe\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000fff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000fff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000ffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000ffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000fff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000fff\n"
# stdout: "0000000000000000\n"
# stdout: "123456789abceeef\n"
# stdout: "0000000000000000\n"
# stdout: "000000009abceeef\n"
# stdout: "0000000000000000\n"
# stdout: "fedcba987654420f\n"
# stdout: "0000000000000000\n"
# stdout: "000000007654420f\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000009\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000001\n"
# stdout: "0000000000000008\n"
# stdout: "123456789abcdef1\n"
# stdout: "0000000000000000\n"
# stdout: "000000009abcdef1\n"
# stdout: "0000000000000008\n"
# stdout: "fedcba9876543211\n"
# stdout: "0000000000000008\n"
# stdout: "0000000076543211\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "fffffffffffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "7fffffffffffffff\n"
# stdout: "0000000000000003\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000000000000a\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000002\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000003\n"
# stdout: "123456789abcdeef\n"
# stdout: "0000000000000002\n"
# stdout: "000000009abcdeef\n"
# stdout: "000000000000000a\n"
# stdout: "fedcba987654320f\n"
# stdout: "000000000000000a\n"
# stdout: "000000007654320f\n"
# stdout: "0000000000000002\n"
# stdout: "fffffffffffff000\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffff000\n"
# stdout: "0000000000000002\n"
# stdout: "fffffffffffff001\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffff001\n"
# stdout: "0000000000000002\n"
# stdout: "ffffffffffffefff\n"
# stdout: "0000000000000002\n"
# stdout: "00000000ffffefff\n"
# stdout: "0000000000000002\n"
# stdout: "7fffffffffffefff\n"
# stdout: "0000000000000002\n"
# stdout: "00000000ffffefff\n"
# stdout: "0000000000000002\n"
# stdout: "7ffffffffffff000\n"
# stdout: "0000000000000002\n"
# stdout: "00000000fffff000\n"
# stdout: "0000000000000002\n"
# stdout: "00000000ffffefff\n"
# stdout: "0000000000000002\n"
# stdout: "00000000ffffefff\n"
# stdout: "0000000000000002\n"
# stdout: "000000007ffff000\n"
# stdout: "0000000000000002\n"
# stdout: "000000007ffff000\n"
# stdout: "0000000000000002\n"
# stdout: "123456789abccef0\n"
# stdout: "0000000000000002\n"
# stdout: "000000009abccef0\n"
# stdout: "0000000000000002\n"
# stdout: "fedcba9876542210\n"
# stdout: "0000000000000002\n"
# stdout: "0000000076542210\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000fff000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000fff000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000fff001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000fff001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000ffefff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000ffefff\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000ffefff\n"
# stdout: "0000000000000009\n"
# stdout: "0000000000ffefff\n"
# stdout: "0000000000000002\n"
# stdout: "8000000000fff000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000fff000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100ffefff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000ffefff\n"
# stdout: "0000000000000002\n"
# stdout: "0000000080fff000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080fff000\n"
# stdout: "0000000000000008\n"
# stdout: "123456789bbccef0\n"
# stdout: "0000000000000000\n"
# stdout: "000000009bbccef0\n"
# stdout: "0000000000000008\n"
# stdout: "fedcba9877542210\n"
# stdout: "0000000000000008\n"
# stdout: "0000000077542210\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000ff0\n"
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
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #0
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
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	add x0, x1, #4095
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	add w0, w1, #4095
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
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #1
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
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	subs x0, x1, #1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	subs w0, w1, #1
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
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	sub x0, x1, #1, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	sub w0, w1, #1, lsl #12
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
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	movz	x2, #0
	movk	x2, #0, lsl #16
	movk	x2, #0, lsl #32
	movk	x2, #0, lsl #48
	mov	x0, #0x55
	adds x0, x1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	adds w0, w1, #4095, lsl #12
	bl	put_hex
	bl	put_nzcv
	mov	x3, sp
	sub	sp, sp, #4096
	add	sp, sp, #16
	mov	x4, sp
	sub	x0, x3, x4
	bl	put_hex
	mov	sp, x3
	b	exit0
