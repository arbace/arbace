# a64i's program (queue 311, review 306 F5): uname(2)'s machine field, aarch64 on arm64 (before queue 311 a64i gave crt's x86_64).
# The lockstep takes uname's buffer from the native (the world's), so only this program's own output compares a64i's field with
# the native's: eval, AOT and the CPU each print it
# with: lib/out.s
# exit: 0
# stdout: "aarch64\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	adrp	x19, uts
	add	x19, x19, :lo12:uts
	mov	x0, x19
	mov	x8, #160			// uname
	svc	#0
	mov	x20, x0
	add	x21, x19, #260			// utsname.machine: the fifth of six 65-byte fields
	mov	x1, #0
1:	ldrb	w2, [x21, x1]
	cbz	w2, 2f
	add	x1, x1, #1
	cmp	x1, #64
	b.lo	1b
2:	mov	w2, #10
	strb	w2, [x21, x1]			// the NUL a newline
	add	x1, x1, #1
	mov	x0, x21
	bl	put_str
	mov	x0, x20
	bl	put_hex				// uname's result
	b	exit0
	.bss
	.p2align	3
uts:	.zero	390
