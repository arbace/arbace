# a64i's program (AR24, queue 299): mkdirat at AT_FDCWD (os.Mkdir's and os.MkdirTemp's call, $GOROOT/test's programs that make a
# temporary directory): the directory made (0), made again EEXIST, newfstatat of it a directory, a path under a missing directory
# ENOENT, then unlinkat with AT_REMOVEDIR removes it (0) and again ENOENT
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffef\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000004000\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffffe\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	// mkdirat(AT_FDCWD, dir, 0755), twice
	mov	x0, #-100
	adr	x1, dir
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, dir
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	// newfstatat(AT_FDCWD, dir, buf, 0): its type
	sub	sp, sp, #128
	mov	x0, #-100
	adr	x1, dir
	mov	x2, sp
	mov	x3, #0
	mov	x8, #79
	svc	#0
	bl	put_hex
	ldr	w0, [sp, #16]
	and	w0, w0, #0xf000
	bl	put_hex
	add	sp, sp, #128
	// mkdirat(AT_FDCWD, missing/sub, 0755)
	mov	x0, #-100
	adr	x1, missing
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	// unlinkat(AT_FDCWD, dir, AT_REMOVEDIR), twice
	mov	x0, #-100
	adr	x1, dir
	mov	x2, #0x200
	mov	x8, #35
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, dir
	mov	x2, #0x200
	mov	x8, #35
	svc	#0
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	b	exit0
dir:	.asciz	"a64i-mkdirat.d"
missing:	.asciz	"a64i-mkdirat-missing/sub"
msg:	.ascii	"done.\n"
	.balign	4
