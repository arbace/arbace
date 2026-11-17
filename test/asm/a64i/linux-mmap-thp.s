# a64i's program (queue 302): mmap's placement of a private anonymous mapping of a multiple of 2 MiB with no hint (Go's heap
# arenas, its 32 MiB and 64 MiB reservations: Arbace's executable's first mmaps), 2 MiB-aligned as Linux (6.7 on, transparent huge
# pages configured) places it; a smaller one page-aligned only; a free hint near the top of mmap's area taken as it is (before
# queue 302 a64i refused every hint above x86-64's stack area, so on arm64 every hint in mmap's area); a shared anonymous one
# written and read. The lockstep compares the addresses themselves (the native without ASLR); the output only what ASLR keeps
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000002a\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	// mmap(0, 4 MiB, RW, MAP_PRIVATE|MAP_ANONYMOUS): a PMD multiple with no hint, 2 MiB-aligned
	mov	x0, #0
	mov	x1, #0x400000
	bl	map
	mov	x19, x0
	and	x0, x19, #0x1fffff
	bl	put_hex
	// mmap(0, 1 MiB): not a PMD multiple, the plain top-down search; page-aligned
	mov	x0, #0
	mov	x1, #0x100000
	bl	map
	mov	x20, x0
	and	x0, x20, #0xfff
	bl	put_hex
	// mmap(0, 6 MiB): aligned, and page-aligned
	mov	x0, #0
	mov	x1, #0x600000
	bl	map
	mov	x21, x0
	and	x0, x21, #0x1fffff
	bl	put_hex
	// mmap(hint, 2 MiB) at a free page-aligned hint 16 MiB + 4 KiB below the first: the hint itself, not aligned
	mov	x9, #0x1000000
	sub	x22, x19, x9
	sub	x22, x22, #0x1000
	mov	x0, x22
	mov	x1, #0x200000
	bl	map
	sub	x0, x0, x22
	bl	put_hex
	// MAP_SHARED|MAP_ANONYMOUS, 2 MiB: written and read back
	mov	x0, #0
	mov	x1, #0x200000
	mov	x2, #3
	mov	x3, #0x21
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	mov	x9, #42
	str	x9, [x0, #8]
	ldr	x0, [x0, #8]
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
map:	// mmap(x0, x1, RW, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0)
	mov	x2, #3
	mov	x3, #0x22
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	ret
msg:	.ascii	"done.\n"
	.balign	4
