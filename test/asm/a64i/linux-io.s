# a64i's program (AR5): Linux by name: openat, read, write, close (the executable's own ELF header)
# with: lib/out.s
# exit: 0
# stdout: "0000000000000001\n"
# stdout: "0000000000000004\n"
# stdout: "00000000464c457f\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffff7\n"
# stdout: "fffffffffffffffe\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	ldr	x1, [sp, #8]
	mov	x0, #-100
	mov	x2, #0
	mov	x3, #0
	mov	x8, #56
	svc	#0
	mov	x19, x0
	cmp	x0, #3
	cset	x0, ge
	bl	put_hex
	sub	sp, sp, #16
	mov	x0, x19
	mov	x1, sp
	mov	x2, #4
	mov	x8, #63
	svc	#0
	bl	put_hex
	ldr	w0, [sp]
	bl	put_hex
	mov	x0, x19
	mov	x8, #57
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x8, #57
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, nofile
	mov	x2, #0
	mov	x8, #56
	svc	#0
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	add	sp, sp, #16
	b	exit0
nofile:	.asciz	"/nonexistent/a64i"
msg:	.ascii	"done.\n"
	.balign	4
	b	exit0
