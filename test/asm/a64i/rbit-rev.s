# a64i's program (AR5): bit and byte reversal: rbit, rev16, rev32, rev, rev64
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "fffffffffffffffe\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffff00000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000001\n"
# stdout: "0f7b3d591e6a2c48\n"
# stdout: "000000000f7b3d59\n"
# stdout: "084c2a6e195d3b7f\n"
# stdout: "00000000084c2a6e\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000100\n"
# stdout: "0000000000000100\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ff7fffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0080000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000800000\n"
# stdout: "0000000000800000\n"
# stdout: "34127856bc9af0de\n"
# stdout: "00000000bc9af0de\n"
# stdout: "dcfe98ba54761032\n"
# stdout: "0000000054761032\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0100000000000000\n"
# stdout: "0000000001000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffff7f\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000080\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffff00000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000008000000000\n"
# stdout: "0000000000000080\n"
# stdout: "f0debc9a78563412\n"
# stdout: "00000000f0debc9a\n"
# stdout: "1032547698badcfe\n"
# stdout: "0000000010325476\n"
# stdout: "0000000000000000\n"
# stdout: "0000000001000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffff7fffffffff\n"
# stdout: "0000008000000000\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000000000080\n"
# stdout: "78563412f0debc9a\n"
# stdout: "98badcfe10325476\n"
# stdout: "0000000000000000\n"
# stdout: "0100000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "ffffffffffffff7f\n"
# stdout: "0000000000000080\n"
# stdout: "ffffffff00000000\n"
# stdout: "0000008000000000\n"
# stdout: "f0debc9a78563412\n"
# stdout: "1032547698badcfe\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	rbit x0, x1
	bl	put_hex
	mov	x0, #-1
	rbit w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	rev16 x0, x1
	bl	put_hex
	mov	x0, #-1
	rev16 w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	rev x0, x1
	bl	put_hex
	mov	x0, #-1
	rev w0, w1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	rev32 x0, x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	rev64 x0, x1
	bl	put_hex
	movz	x1, #12816
	movk	x1, #30292, lsl #16
	movk	x1, #47768, lsl #32
	movk	x1, #65244, lsl #48
	rev64 x0, x1
	bl	put_hex
	b	exit0
