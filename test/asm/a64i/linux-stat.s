# a64i's program (AR21, queue 292): fstat and newfstatat with arm64's 128-byte struct stat (gofmt's and vet's files). The buffers sit
# in a page of 0xaa bytes: the 16 bytes after the buffer (the page's middle) and before it (the page's end, the next page unmapped)
# are still 0xaa after the call; the executable's own fstat is a regular file with one link, its size lseek's end, the padding
# (bytes 40-47, 60-63, 120-127) zero; newfstatat by path and by AT_EMPTY_PATH give the same st_dev and st_ino; ENOENT, EBADF and
# EFAULT leave the buffer as it was; ftruncate of the read-only descriptor EINVAL, of a closed one EBADF, fdatasync of it 0 (gofmt -w)
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000008000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "fffffffffffffffe\n"
# stdout: "fffffffffffffff7\n"
# stdout: "fffffffffffffff2\n"
# stdout: "0000000000000001\n"
# stdout: "ffffffffffffffea\n"
# stdout: "fffffffffffffff7\n"
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
	// two pages of 0xaa, the second unmapped again
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
	mov	x8, #215
	svc	#0
	mov	x9, #0xaaaa
	orr	x9, x9, x9, lsl #16
	orr	x9, x9, x9, lsl #32
	mov	x10, #0
1:	str	x9, [x21, x10]
	add	x10, x10, #8
	cmp	x10, #4096
	b.lo	1b
	// fstat(fd, page+256)
	add	x22, x21, #256
	mov	x0, x19
	mov	x1, x22
	mov	x8, #80
	svc	#0
	bl	put_hex
	ldr	w0, [x22, #16]
	and	w0, w0, #0xf000
	bl	put_hex
	ldr	w0, [x22, #20]
	bl	put_hex
	ldr	x0, [x22, #40]
	ldr	w1, [x22, #60]
	orr	x0, x0, x1
	ldr	x1, [x22, #120]
	orr	x0, x0, x1
	bl	put_hex
	ldr	x0, [x22, #128]
	ldr	x1, [x22, #136]
	and	x0, x0, x1
	cmp	x0, x9
	cset	x0, eq
	bl	put_hex
	mov	x0, x19
	mov	x1, #0
	mov	x2, #2
	mov	x8, #62
	svc	#0
	ldr	x1, [x22, #48]
	cmp	x0, x1
	cset	x0, eq
	bl	put_hex
	// newfstatat(AT_FDCWD, argv[0], page+4096-128, 0): the buffer at the page's end
	add	x23, x21, #3968
	mov	x0, #-100
	mov	x1, x20
	mov	x2, x23
	mov	x3, #0
	mov	x8, #79
	svc	#0
	bl	put_hex
	ldr	x0, [x22, #0]
	ldr	x1, [x23, #0]
	cmp	x0, x1
	ldr	x0, [x22, #8]
	ldr	x1, [x23, #8]
	ccmp	x0, x1, #0, eq
	cset	x0, eq
	bl	put_hex
	ldur	x0, [x23, #-16]
	ldur	x1, [x23, #-8]
	and	x0, x0, x1
	cmp	x0, x9
	cset	x0, eq
	bl	put_hex
	// newfstatat(fd, "", page+512, AT_EMPTY_PATH)
	add	x24, x21, #512
	mov	x0, x19
	adr	x1, empty
	mov	x2, x24
	mov	x3, #0x1000
	mov	x8, #79
	svc	#0
	bl	put_hex
	ldr	x0, [x22, #8]
	ldr	x1, [x24, #8]
	cmp	x0, x1
	cset	x0, eq
	bl	put_hex
	// the errors, into page+1024: ENOENT (no follow), EBADF, EFAULT (address 16)
	add	x25, x21, #1024
	mov	x0, #-100
	adr	x1, nofile
	mov	x2, x25
	mov	x3, #0x100
	mov	x8, #79
	svc	#0
	bl	put_hex
	mov	x0, #99
	mov	x1, x25
	mov	x8, #80
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x1, #16
	mov	x8, #80
	svc	#0
	bl	put_hex
	mov	x0, #-1
	mov	x10, #0
2:	ldr	x1, [x25, x10]
	and	x0, x0, x1
	add	x10, x10, #8
	cmp	x10, #144
	b.lo	2b
	cmp	x0, x9
	cset	x0, eq
	bl	put_hex
	// ftruncate(fd, 0), ftruncate(99, 0), fdatasync(fd)
	mov	x0, x19
	mov	x1, #0
	mov	x8, #46
	svc	#0
	bl	put_hex
	mov	x0, #99
	mov	x1, #0
	mov	x8, #46
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x8, #83
	svc	#0
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
nofile:	.asciz	"/nonexistent/a64i"
empty:	.asciz	""
msg:	.ascii	"done.\n"
	.balign	4
