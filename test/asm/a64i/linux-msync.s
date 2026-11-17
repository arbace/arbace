# a64i's program (AR23, queue 295): MAP_SHARED file mappings written back to their file as Linux does (cmd/link maps its output
# file shared and writable): a file of its own (the executable's name, ".msync." and 16 letters of getrandom's), fallocate (8192
# bytes; KEEP_SIZE; its errors EINVAL, EOPNOTSUPP, EBADF, EBADF), two pages mapped shared, written, msync MS_SYNC and pread64's
# bytes; msync's errors (EINVAL thrice); write(2) into a mapped page kept at the write-back (only the bytes the program changed
# go back); munmap of one page written back, msync over the hole ENOMEM; a mapping that outlives its descriptor; the last page
# past the file's end (written, the file's size unchanged); mprotect PROT_WRITE over a shared mapping of a read-only descriptor
# EACCES, of a read-write one 0 and written back; MAP_FIXED over a shared mapping written back first; a mapping at an offset,
# MS_ASYNC; the file removed
# with: lib/out.s
# exit: 0
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000002000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000002000\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffa1\n"
# stdout: "fffffffffffffff7\n"
# stdout: "fffffffffffffff7\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000004241\n"
# stdout: "7788556633441122\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000099\n"
# stdout: "00000000007a7978\n"
# stdout: "fffffffffffffff4\n"
# stdout: "0000000000000043\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000004443\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000002064\n"
# stdout: "0000000000000055\n"
# stdout: "fffffffffffffff3\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000454443\n"
# stdout: "0000000000000001\n"
# stdout: "0000000046454443\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000047\n"
# stdout: "0000000000000000\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	ldr	x20, [sp, #8]
	// a page for the buffers: pread64's at x22, getrandom's at x22+512, the file's name at x22+1024
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #0x22
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x22, x0
	add	x23, x22, #1024
	// the name: argv[0], ".msync.", 16 letters of getrandom's 8 bytes (runs side by side do not meet)
	mov	x9, #0
1:	ldrb	w10, [x20, x9]
	cbz	w10, 2f
	strb	w10, [x23, x9]
	add	x9, x9, #1
	b	1b
2:	adr	x11, suffix
	mov	x12, #0
3:	ldrb	w10, [x11, x12]
	strb	w10, [x23, x9]
	add	x9, x9, #1
	add	x12, x12, #1
	cmp	x12, #7
	b.lo	3b
	add	x0, x22, #512
	mov	x1, #8
	mov	x2, #0
	mov	x8, #278
	svc	#0
	ldr	x13, [x22, #512]
	mov	x12, #16
4:	and	x10, x13, #15
	add	x10, x10, #97
	strb	w10, [x23, x9]
	add	x9, x9, #1
	lsr	x13, x13, #4
	subs	x12, x12, #1
	b.ne	4b
	strb	wzr, [x23, x9]
	// openat(AT_FDCWD, name, O_RDWR|O_CREAT|O_TRUNC, 0600)
	mov	x0, #-100
	mov	x1, x23
	mov	x2, #0x242
	mov	x3, #0x180
	mov	x8, #56
	svc	#0
	mov	x19, x0
	cmp	x0, #3
	cset	x0, ge
	bl	put_hex
	// fallocate(fd, 0, 0, 8192): 0, the size 8192; with FALLOC_FL_KEEP_SIZE to 16384: 0, the size still 8192
	mov	x0, x19
	mov	x1, #0
	mov	x2, #0
	mov	x3, #8192
	mov	x8, #47
	svc	#0
	bl	put_hex
	bl	size
	bl	put_hex
	mov	x0, x19
	mov	x1, #1
	mov	x2, #0
	mov	x3, #16384
	mov	x8, #47
	svc	#0
	bl	put_hex
	bl	size
	bl	put_hex
	// fallocate's errors: no length EINVAL, FALLOC_FL_PUNCH_HOLE without KEEP_SIZE EOPNOTSUPP, a closed descriptor EBADF, one
	// not open for writing (the executable's, kept in x24) EBADF
	mov	x0, x19
	mov	x1, #0
	mov	x2, #0
	mov	x3, #0
	mov	x8, #47
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x1, #2
	mov	x2, #0
	mov	x3, #4096
	mov	x8, #47
	svc	#0
	bl	put_hex
	mov	x0, #99
	mov	x1, #0
	mov	x2, #0
	mov	x3, #4096
	mov	x8, #47
	svc	#0
	bl	put_hex
	mov	x0, #-100
	mov	x1, x20
	mov	x2, #0
	mov	x3, #0
	mov	x8, #56
	svc	#0
	mov	x24, x0
	mov	x0, x24
	mov	x1, #0
	mov	x2, #0
	mov	x3, #4096
	mov	x8, #47
	svc	#0
	bl	put_hex
	// mmap(0, 8192, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0): "AB" at 0, a word at 4196; msync MS_SYNC 0; the file's bytes
	mov	x0, #0
	mov	x1, #8192
	mov	x2, #3
	mov	x3, #1
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	mov	w9, #0x4241
	strh	w9, [x21]
	movz	x9, #0x1122
	movk	x9, #0x3344, lsl #16
	movk	x9, #0x5566, lsl #32
	movk	x9, #0x7788, lsl #48
	add	x10, x21, #4096
	str	x9, [x10, #100]
	mov	x0, x21
	mov	x1, #8192
	mov	x2, #4
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x1, #0
	bl	pread8
	bl	put_hex
	mov	x1, #4196
	bl	pread8
	bl	put_hex
	// msync's errors: an address not page-aligned, MS_ASYNC with MS_SYNC, an unknown flag: EINVAL
	add	x0, x21, #1
	mov	x1, #4096
	mov	x2, #4
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x0, x21
	mov	x1, #4096
	mov	x2, #5
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x0, x21
	mov	x1, #4096
	mov	x2, #8
	mov	x8, #227
	svc	#0
	bl	put_hex
	// write(2) of "xyz" at 200; 'C' at the mapping's byte 8 and 0x99 at 4296; munmap of the second page: 0, 0x99 in the file;
	// "xyz" kept (only the bytes the program changed go back); msync of both pages ENOMEM (the second unmapped), byte 8's 'C'
	// written back
	mov	x0, x19
	mov	x1, #200
	mov	x2, #0
	mov	x8, #62
	svc	#0
	adr	x1, xyz
	mov	x0, x19
	mov	x2, #3
	mov	x8, #64
	svc	#0
	bl	put_hex
	mov	w9, #0x43
	strb	w9, [x21, #8]
	add	x10, x21, #4096
	mov	x9, #0x99
	str	x9, [x10, #200]
	add	x0, x21, #4096
	mov	x1, #4096
	mov	x8, #215
	svc	#0
	bl	put_hex
	mov	x1, #4296
	bl	pread8
	bl	put_hex
	mov	x1, #200
	bl	pread8
	bl	put_hex
	mov	x0, x21
	mov	x1, #8192
	mov	x2, #4
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x1, #8
	bl	pread8
	bl	put_hex
	// 'D' at byte 9; the descriptor closed, then the page unmapped: written back all the same (the mapping holds its file)
	mov	w9, #0x44
	strb	w9, [x21, #9]
	mov	x0, x19
	mov	x8, #57
	svc	#0
	bl	put_hex
	mov	x0, x21
	mov	x1, #4096
	mov	x8, #215
	svc	#0
	bl	put_hex
	mov	x0, #-100
	mov	x1, x23
	mov	x2, #2
	mov	x3, #0
	mov	x8, #56
	svc	#0
	mov	x19, x0
	mov	x1, #8
	bl	pread8
	bl	put_hex
	// the file's last page: ftruncate to 8292, three pages mapped, bytes at 8242 (in the file) and 8392 (past its end, in its
	// last page) written, munmap: the size still 8292, byte 8242's 0x55
	mov	x0, x19
	mov	x1, #8292
	mov	x8, #46
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #12288
	mov	x2, #3
	mov	x3, #1
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	add	x10, x21, #8192
	mov	w9, #0x55
	strb	w9, [x10, #50]
	mov	w9, #0x66
	strb	w9, [x10, #200]
	mov	x0, x21
	mov	x1, #12288
	mov	x8, #215
	svc	#0
	bl	put_hex
	bl	size
	bl	put_hex
	mov	x1, #8242
	bl	pread8
	bl	put_hex
	// mprotect PROT_WRITE: over a shared mapping of the read-only descriptor EACCES; of the read-write one mapped PROT_READ 0,
	// 'E' written at 10, munmap: in the file
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #1
	mov	x4, x24
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x25, x0
	mov	x0, x25
	mov	x1, #4096
	mov	x2, #3
	mov	x8, #226
	svc	#0
	bl	put_hex
	mov	x0, x25
	mov	x1, #4096
	mov	x8, #215
	svc	#0
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #1
	mov	x3, #1
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	mov	x0, x21
	mov	x1, #4096
	mov	x2, #3
	mov	x8, #226
	svc	#0
	bl	put_hex
	mov	w9, #0x45
	strb	w9, [x21, #10]
	mov	x0, x21
	mov	x1, #4096
	mov	x8, #215
	svc	#0
	mov	x1, #8
	bl	pread8
	bl	put_hex
	// MAP_FIXED (anonymous, private) over a shared mapping: its 'F' at 11 written back first
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #1
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	mov	w9, #0x46
	strb	w9, [x21, #11]
	mov	x0, x21
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #0x32
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	cmp	x0, x21
	cset	x0, eq
	bl	put_hex
	mov	x1, #8
	bl	pread8
	bl	put_hex
	mov	x0, x21
	mov	x1, #4096
	mov	x8, #215
	svc	#0
	// at offset 4096: 'G' at 16, msync MS_ASYNC 0, the file's byte 4112
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #1
	mov	x4, x19
	mov	x5, #4096
	mov	x8, #222
	svc	#0
	mov	x21, x0
	mov	w9, #0x47
	strb	w9, [x21, #16]
	mov	x0, x21
	mov	x1, #4096
	mov	x2, #1
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x1, #4112
	bl	pread8
	bl	put_hex
	mov	x0, x21
	mov	x1, #4096
	mov	x8, #215
	svc	#0
	// the file removed, the descriptors closed
	mov	x0, #-100
	mov	x1, x23
	mov	x2, #0
	mov	x8, #35
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x8, #57
	svc	#0
	mov	x0, x24
	mov	x8, #57
	svc	#0
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
// size: x0 the size of the file of x19 (lseek SEEK_END)
size:
	mov	x0, x19
	mov	x1, #0
	mov	x2, #2
	mov	x8, #62
	svc	#0
	ret
// pread8: x0 the 8 bytes of the file of x19 at offset x1 (pread64 into x22, zero first)
pread8:
	str	xzr, [x22]
	mov	x3, x1
	mov	x0, x19
	mov	x1, x22
	mov	x2, #8
	mov	x8, #67
	svc	#0
	ldr	x0, [x22]
	ret
suffix:	.ascii	".msync."
xyz:	.ascii	"xyz"
msg:	.ascii	"done.\n"
	.balign	4
