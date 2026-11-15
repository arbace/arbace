# a64i's program (queue 311, review 306 F6): an address-space reservation as JITs, wasm runtimes and sanitizers make them: 1 TiB
# PROT_NONE (MAP_NORESERVE), two pages in its middle made writable by mprotect and written, a MiB in it mapped over (MAP_FIXED)
# and filled by getrandom (crt's call into the reservation), then all of it unmapped and reserved again at the same address (the
# hole reused, top-down). Before queue 311 a64i kept a permission a page and backed every byte at mmap time: 1 TiB ended it (out of
# memory), and 8 GiB cost 171 MB and 0.2 s. The lockstep compares the addresses themselves; the output what ASLR keeps
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000055\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000040\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	// mmap(0, 1 TiB, PROT_NONE, MAP_PRIVATE|MAP_ANONYMOUS|MAP_NORESERVE): page-aligned (an error is not)
	mov	x0, #0
	mov	x1, #1
	lsl	x1, x1, #40
	mov	x2, #0
	bl	map
	mov	x19, x0
	and	x0, x19, #0xfff
	bl	put_hex
	// mprotect(the middle, 2 pages, RW): 0
	mov	x20, #1
	lsl	x20, x20, #39
	add	x20, x19, x20
	mov	x0, x20
	mov	x1, #0x2000
	mov	x2, #3
	mov	x8, #226
	svc	#0
	bl	put_hex
	// written on both pages and read back: 0x2a + 0x2b
	mov	x9, #42
	str	x9, [x20]
	mov	x9, #43
	str	x9, [x20, #4104]
	ldr	x9, [x20]
	ldr	x10, [x20, #4104]
	add	x0, x9, x10
	bl	put_hex
	// a page beside them still PROT_NONE: mprotect of the three pages from it to PROT_READ is 0, its byte 0
	sub	x0, x20, #0x1000
	mov	x1, #0x3000
	mov	x2, #1
	mov	x8, #226
	svc	#0
	bl	put_hex
	sub	x9, x20, #0x1000
	ldrb	w0, [x9]
	bl	put_hex
	// mmap(a quarter in, 1 MiB, RW, MAP_FIXED|MAP_PRIVATE|MAP_ANONYMOUS): the address asked; getrandom fills its 64 bytes there
	mov	x21, #1
	lsl	x21, x21, #38
	add	x21, x19, x21
	mov	x0, x21
	mov	x1, #0x100000
	mov	x2, #3
	mov	x3, #0x32
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	sub	x0, x0, x21
	add	x0, x0, #0
	mov	x22, x0
	add	x0, x21, #0xff000
	mov	x1, #64
	mov	x2, #0
	mov	x8, #278			// getrandom
	svc	#0
	bl	put_hex
	mov	x0, x22
	bl	put_hex
	// munmap(all of it): 0; reserved again: the same address
	mov	x0, x19
	mov	x1, #1
	lsl	x1, x1, #40
	mov	x8, #215
	svc	#0
	bl	put_hex
	mov	x0, #0
	mov	x1, #1
	lsl	x1, x1, #40
	mov	x2, #0
	bl	map
	sub	x0, x0, x19
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
map:	// mmap(x0, x1, x2, MAP_PRIVATE|MAP_ANONYMOUS|MAP_NORESERVE, -1, 0)
	mov	x3, #0x4022
	mov	x4, #-1
	mov	x5, #0
	mov	x8, #222
	svc	#0
	ret
msg:	.ascii	"done.\n"
	.balign	4
