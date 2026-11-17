# a64i's program (AR22, queue 294): mmap of a file (cmd/compile maps its imports' export data MAP_SHARED PROT_READ): the executable
# (12 KiB of data in it) mapped MAP_PRIVATE at offset 0, its ELF magic and its 8 KiB equal to pread64's; MAP_SHARED at offset 4096
# equal to pread64's; its last page at the file's end, its bytes pread64's and the rest of the page zero; a private writable copy
# written, the file unchanged; read(2) into the read-only mapping EFAULT; the errors in Linux's order: an offset not page-aligned
# EINVAL, a closed descriptor EBADF, no mapping type EINVAL, MAP_SHARED PROT_WRITE of a read-only descriptor EACCES, a directory
# ENODEV
# with: lib/out.s
# exit: 0
# stdout: "00000000464c457f\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "fffffffffffffff2\n"
# stdout: "ffffffffffffffea\n"
# stdout: "fffffffffffffff7\n"
# stdout: "ffffffffffffffea\n"
# stdout: "fffffffffffffff3\n"
# stdout: "ffffffffffffffed\n"
# stdout: "0000000000000000\n"
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
	// a buffer of 8 KiB for pread64
	mov	x0, #0
	mov	x1, #8192
	mov	x2, #3
	mov	x3, #0x22
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x22, x0
	// mmap(0, 8192, PROT_READ, MAP_PRIVATE, fd, 0): the magic, the bytes pread64's
	mov	x0, #0
	mov	x1, #8192
	mov	x2, #1
	mov	x3, #2
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	ldr	w0, [x21]
	bl	put_hex
	mov	x0, x19
	mov	x1, x22
	mov	x2, #8192
	mov	x3, #0
	mov	x8, #67
	svc	#0
	mov	x0, x21
	mov	x1, x22
	mov	x2, #8192
	bl	memeq
	bl	put_hex
	// mmap(0, 4096, PROT_READ, MAP_SHARED, fd, 4096)
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #1
	mov	x4, x19
	mov	x5, #4096
	mov	x8, #222
	svc	#0
	mov	x23, x0
	mov	x0, x19
	mov	x1, x22
	mov	x2, #4096
	mov	x3, #4096
	mov	x8, #67
	svc	#0
	mov	x0, x23
	mov	x1, x22
	mov	x2, #4096
	bl	memeq
	bl	put_hex
	// the last page: size = lseek(fd, 0, SEEK_END), mapped at size & ~4095; its size & 4095 bytes pread64's, the rest zero
	mov	x0, x19
	mov	x1, #0
	mov	x2, #2
	mov	x8, #62
	svc	#0
	and	x25, x0, #4095
	and	x26, x0, #0xfffffffffffff000
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #2
	mov	x4, x19
	mov	x5, x26
	mov	x8, #222
	svc	#0
	mov	x24, x0
	mov	x0, #1
	cbz	x25, 2f
	mov	x0, x19
	mov	x1, x22
	mov	x2, x25
	mov	x3, x26
	mov	x8, #67
	svc	#0
	mov	x0, x24
	mov	x1, x22
	mov	x2, x25
	bl	memeq
2:	bl	put_hex
	mov	x0, #1
	mov	x10, x25
	cbz	x25, 4f
3:	ldrb	w11, [x24, x10]
	cmp	w11, #0
	csel	x0, x0, xzr, eq
	add	x10, x10, #1
	cmp	x10, #4096
	b.lo	3b
4:	bl	put_hex
	// mmap(0, 4096, PROT_READ|PROT_WRITE, MAP_PRIVATE, fd, 0): byte 0 written 0, the file's still 0x7f
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #2
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x27, x0
	strb	wzr, [x27]
	ldrb	w0, [x27]
	cmp	w0, #0
	cset	x0, eq
	bl	put_hex
	mov	x0, x19
	mov	x1, x22
	mov	x2, #1
	mov	x3, #0
	mov	x8, #67
	svc	#0
	ldrb	w0, [x22]
	cmp	w0, #0x7f
	cset	x0, eq
	bl	put_hex
	// read(fd, the read-only mapping, 16) at offset 0: EFAULT
	mov	x0, x19
	mov	x1, #0
	mov	x2, #0
	mov	x8, #62
	svc	#0
	mov	x0, x19
	mov	x1, x21
	mov	x2, #16
	mov	x8, #63
	svc	#0
	bl	put_hex
	// the errors: offset 100, descriptor 99, flags 0, MAP_SHARED PROT_WRITE of the read-only descriptor, a directory
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #2
	mov	x4, x19
	mov	x5, #100
	mov	x8, #222
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #2
	mov	x4, #99
	mov	x5, #0
	mov	x8, #222
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #0
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #1
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, root
	mov	x2, #0
	mov	x3, #0
	mov	x8, #56
	svc	#0
	mov	x4, x0
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #2
	mov	x5, #0
	mov	x8, #222
	svc	#0
	bl	put_hex
	// munmap(the first mapping)
	mov	x0, x21
	mov	x1, #8192
	mov	x8, #215
	svc	#0
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
// memeq: x0 1 when the x2 bytes at x0 and x1 are equal, else 0 (x9-x12 used)
memeq:
	mov	x9, #0
	mov	x12, #1
1:	cmp	x9, x2
	b.hs	2f
	ldrb	w10, [x0, x9]
	ldrb	w11, [x1, x9]
	cmp	w10, w11
	csel	x12, x12, xzr, eq
	add	x9, x9, #1
	b	1b
2:	mov	x0, x12
	ret
root:	.asciz	"/"
msg:	.ascii	"done.\n"
	.balign	4
	.data
	.fill	3072, 4, 0x5a5a5a5a
