# a64i's program (AR21, queue 292): read, pread64, write and getrandom over a buffer that two adjacent mappings hold (two pages
# mapped, the second mapped again MAP_FIXED: Go's heap grows so, and vet's read of its export data met it): the executable's first
# eight bytes read across the boundary (its ELF magic, class, data and version), again by pread64 at offset 0, six bytes written
# from across it, sixteen random bytes into it; each call's count the whole
# with: lib/out.s
# exit: 0
# stdout: "0000000000000008\n"
# stdout: "00010102464c457f\n"
# stdout: "0000000000000008\n"
# stdout: "00010102464c457f\n"
# stdout: "ab\n"
# stdout: "cd\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000010\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	ldr	x20, [sp, #8]
	mov	x0, #-100
	mov	x1, x20
	mov	x2, #0
	mov	x3, #0
	mov	x8, #56
	svc	#0
	mov	x19, x0
	// two pages, the second mapped again in place
	mov	x0, #0
	mov	x1, #8192
	mov	x2, #3
	mov	x3, #0x22
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	add	x0, x21, #4096
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #0x32
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	// read(fd, page+4092, 8)
	add	x22, x21, #4092
	mov	x0, x19
	mov	x1, x22
	mov	x2, #8
	mov	x8, #63
	svc	#0
	bl	put_hex
	ldur	x0, [x22]
	bl	put_hex
	// pread64(fd, page+4090, 8, 0)
	add	x23, x21, #4090
	mov	x0, x19
	mov	x1, x23
	mov	x2, #8
	mov	x3, #0
	mov	x8, #67
	svc	#0
	bl	put_hex
	ldur	x0, [x23]
	bl	put_hex
	// write(1, page+4093, 6): "ab\ncd\n" across the boundary
	add	x24, x21, #4093
	mov	w0, #97
	strb	w0, [x24]
	mov	w0, #98
	strb	w0, [x24, #1]
	mov	w0, #10
	strb	w0, [x24, #2]
	mov	w0, #99
	strb	w0, [x24, #3]
	mov	w0, #100
	strb	w0, [x24, #4]
	mov	w0, #10
	strb	w0, [x24, #5]
	mov	x0, #1
	mov	x1, x24
	mov	x2, #6
	mov	x8, #64
	svc	#0
	bl	put_hex
	// getrandom(page+4088, 16, 0)
	add	x0, x21, #4088
	mov	x1, #16
	mov	x2, #0
	mov	x8, #278
	svc	#0
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
msg:	.ascii	"done.\n"
	.balign	4
