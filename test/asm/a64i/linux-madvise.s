# a64i's program (AR24, queue 299): madvise. Go's scavenger gives its free heap pages back with MADV_DONTNEED and reuses them as
# zeros (fixedbugs/issue22781.go's GC under SetGCPercent(1): "marked free object in span" while a64i's madvise left the old bytes).
# Two private anonymous pages of 0xab: MADV_DONTNEED of the first makes it read zero, the second keeps its bytes; MADV_WILLNEED a
# hint (0), the bytes kept; an address not page-aligned EINVAL; a shared anonymous page keeps its bytes after MADV_DONTNEED
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "abababababababab\n"
# stdout: "0000000000000000\n"
# stdout: "abababababababab\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000000000\n"
# stdout: "abababababababab\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	// mmap(0, 8192, RW, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0), filled with 0xab
	mov	x0, #0
	mov	x1, #8192
	mov	x2, #3
	mov	x3, #0x22
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x19, x0
	mov	x0, x19
	mov	x1, #8192
	bl	fill
	// madvise(page 0, 4096, MADV_DONTNEED)
	mov	x0, x19
	mov	x1, #4096
	mov	x2, #4
	mov	x8, #233
	svc	#0
	bl	put_hex
	ldr	x0, [x19, #2048]
	bl	put_hex
	add	x9, x19, #4096
	ldr	x0, [x9, #8]
	bl	put_hex
	// madvise(page 1, 4096, MADV_WILLNEED)
	add	x0, x19, #4096
	mov	x1, #4096
	mov	x2, #3
	mov	x8, #233
	svc	#0
	bl	put_hex
	add	x9, x19, #4096
	ldr	x0, [x9, #4088]
	bl	put_hex
	// madvise(page 0 + 8, 4096, MADV_DONTNEED): EINVAL
	add	x0, x19, #8
	mov	x1, #4096
	mov	x2, #4
	mov	x8, #233
	svc	#0
	bl	put_hex
	// a shared anonymous page: mmap(0, 4096, RW, MAP_SHARED|MAP_ANONYMOUS, -1, 0), 0xab, MADV_DONTNEED, its bytes kept
	mov	x0, #0
	mov	x1, #4096
	mov	x2, #3
	mov	x3, #0x21
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x20, x0
	mov	x0, x20
	mov	x1, #4096
	bl	fill
	mov	x0, x20
	mov	x1, #4096
	mov	x2, #4
	mov	x8, #233
	svc	#0
	bl	put_hex
	ldr	x0, [x20, #16]
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
	// fill(x0, x1): x1 bytes of 0xab at x0
fill:	mov	x9, #0xabab
	orr	x9, x9, x9, lsl #16
	orr	x9, x9, x9, lsl #32
	mov	x10, #0
1:	str	x9, [x0, x10]
	add	x10, x10, #8
	cmp	x10, x1
	b.lo	1b
	ret
msg:	.ascii	"done.\n"
	.balign	4
