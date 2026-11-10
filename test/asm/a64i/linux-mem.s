# a64i's program (AR5): Linux by name: brk, mmap, munmap, mprotect; the pages written and read
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000002000\n"
# stdout: "0000000000000077\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	mov	x0, #0
	mov	x8, #214
	svc	#0
	mov	x19, x0
	and	x0, x0, #0xfff
	bl	put_hex
	add	x0, x19, #8192
	mov	x8, #214
	svc	#0
	sub	x0, x0, x19
	bl	put_hex
	mov	x1, #0x77
	mov	x9, #8191
	strb	w1, [x19, x9]
	ldrb	w0, [x19, x9]
	bl	put_hex
	mov	x0, x19
	mov	x8, #214
	svc	#0
	sub	x0, x0, x19
	bl	put_hex
	mov	x0, #0
	mov	x1, #12288
	mov	x2, #3
	mov	x3, #0x22
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x20, x0
	and	x0, x0, #0xfff
	bl	put_hex
	ldr	x0, [x20, #4096]
	bl	put_hex
	mov	x1, #-1
	str	x1, [x20, #8184]
	ldr	x0, [x20, #8184]
	bl	put_hex
	add	x0, x20, #4096
	mov	x1, #4096
	mov	x2, #1
	mov	x8, #226
	svc	#0
	bl	put_hex
	ldr	x0, [x20, #4096]
	bl	put_hex
	mov	x0, x20
	mov	x1, #12288
	mov	x8, #215
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #4095
	mov	x8, #215
	svc	#0
	bl	put_hex
	b	exit0
