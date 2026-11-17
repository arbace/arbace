# a64i's program (queue 310, review 306 F4): a MAP_SHARED file mapping's write-back writes only the bytes the program changed
# through it, and on an O_APPEND descriptor too: a file of its own (the executable's name, ".mswbk." and 16 letters of
# getrandom's), 8192 zero bytes, both pages mapped shared; 'X' at 0 and 'Y' at 100 stored, "WWWW" at 50 by pwrite64 between them,
# 'Z' at 4104 and 'Q' at 4110 stored, "VV" at 4106 by pwrite64 between them; msync MS_SYNC; the file's bytes: the write(2)'s kept
# (before queue 310 a64i wrote each page's span from its first changed byte to its last, the write(2)'s bytes zeros again); munmap;
# then the file opened O_RDWR|O_APPEND and mapped shared: 'A' at 0 and 'B' at 4097 stored, msync MS_SYNC, the file's bytes ('A'
# and 'B': before, both lost, crt's pwrite64 refusing the append-mode file); 'C' at 2 stored and munmap: in the file; the
# descriptor still appends ("xyz" by write(2) at the end: the size 8195, the bytes at 8192) and F_GETFL still has O_APPEND; the file
# removed
# with: lib/out.s
# exit: 0
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "0000575757570000\n"
# stdout: "0000000000000058\n"
# stdout: "0000005900000000\n"
# stdout: "005100005656005a\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000041\n"
# stdout: "0000000000004200\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000430041\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000002003\n"
# stdout: "00000000007a7978\n"
# stdout: "0000000000000402\n"
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
	// the name: argv[0], ".mswbk.", 16 letters of getrandom's 8 bytes (runs side by side do not meet)
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
	// openat(AT_FDCWD, name, O_RDWR|O_CREAT|O_TRUNC, 0600); ftruncate to 8192: 0
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
	mov	x0, x19
	mov	x1, #8192
	mov	x8, #46
	svc	#0
	bl	put_hex
	// mmap(0, 8192, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0); 'X' at 0, 'Y' at 100, "WWWW" at 50 by pwrite64 (4); 'Z' at 4104,
	// 'Q' at 4110, "VV" at 4106 by pwrite64 (2)
	bl	map2
	mov	w9, #0x58
	strb	w9, [x21]
	mov	w9, #0x59
	strb	w9, [x21, #100]
	adr	x1, wwww
	mov	x2, #4
	mov	x3, #50
	bl	pwrite
	bl	put_hex
	add	x10, x21, #4096
	mov	w9, #0x5a
	strb	w9, [x10, #8]
	mov	w9, #0x51
	strb	w9, [x10, #14]
	adr	x1, vvv
	mov	x2, #2
	mov	x3, #4106
	bl	pwrite
	bl	put_hex
	// msync MS_SYNC: 0; the file's bytes at 48 (the write(2)'s "WWWW" kept), 0 ('X'), 96 ('Y'), 4104 ('Z', "VV" kept, 'Q')
	mov	x0, x21
	mov	x1, #8192
	mov	x2, #4
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x1, #48
	bl	pread8
	bl	put_hex
	mov	x1, #0
	bl	pread8
	bl	put_hex
	mov	x1, #96
	bl	pread8
	bl	put_hex
	mov	x1, #4104
	bl	pread8
	bl	put_hex
	mov	x0, x21
	mov	x1, #8192
	mov	x8, #215
	svc	#0
	bl	put_hex
	// the descriptor closed, the file opened O_RDWR|O_APPEND, both pages mapped shared: 'A' at 0, 'B' at 4097, msync MS_SYNC 0,
	// the file's bytes at 0 and 4096
	mov	x0, x19
	mov	x8, #57
	svc	#0
	mov	x0, #-100
	mov	x1, x23
	mov	x2, #0x402
	mov	x3, #0
	mov	x8, #56
	svc	#0
	mov	x19, x0
	cmp	x0, #3
	cset	x0, ge
	bl	put_hex
	bl	map2
	mov	w9, #0x41
	strb	w9, [x21]
	add	x10, x21, #4096
	mov	w9, #0x42
	strb	w9, [x10, #1]
	mov	x0, x21
	mov	x1, #8192
	mov	x2, #4
	mov	x8, #227
	svc	#0
	bl	put_hex
	mov	x1, #0
	bl	pread8
	bl	put_hex
	mov	x1, #4096
	bl	pread8
	bl	put_hex
	// 'C' at 2, munmap: 0, in the file
	mov	w9, #0x43
	strb	w9, [x21, #2]
	mov	x0, x21
	mov	x1, #8192
	mov	x8, #215
	svc	#0
	bl	put_hex
	mov	x1, #0
	bl	pread8
	bl	put_hex
	// the descriptor still appends: write(2) "xyz" 3, the size 8195, the bytes at 8192; F_GETFL & (O_APPEND|O_ACCMODE) 0x402
	adr	x1, xyz
	mov	x0, x19
	mov	x2, #3
	mov	x8, #64
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x1, #0
	mov	x2, #2
	mov	x8, #62
	svc	#0
	bl	put_hex
	mov	x1, #8192
	bl	pread8
	bl	put_hex
	mov	x0, x19
	mov	x1, #3
	mov	x8, #25
	svc	#0
	mov	x9, #0x403
	and	x0, x0, x9
	bl	put_hex
	// the file removed, the descriptor closed
	mov	x0, #-100
	mov	x1, x23
	mov	x2, #0
	mov	x8, #35
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x8, #57
	svc	#0
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
// map2: x21 mmap(0, 8192, PROT_READ|PROT_WRITE, MAP_SHARED, x19, 0)
map2:
	mov	x0, #0
	mov	x1, #8192
	mov	x2, #3
	mov	x3, #1
	mov	x4, x19
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x21, x0
	ret
// pwrite: x0 pwrite64(x19, x1, x2, x3)
pwrite:
	mov	x0, x19
	mov	x8, #68
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
suffix:	.ascii	".mswbk."
wwww:	.ascii	"WWWW"
vvv:	.ascii	"VV"
xyz:	.ascii	"xyz"
msg:	.ascii	"done.\n"
	.balign	4
