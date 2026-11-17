# Arbace's run test (AR4): musl 1.2.6's aarch64 setjmp and longjmp (src/setjmp/aarch64): a nonlocal return through two frames,
# the callee-saved x19 restored
# exit: 3
# stdout: "0\n5\n5\n5\n"
# with: lib/print.s musl:src/setjmp/aarch64/setjmp.s musl:src/setjmp/aarch64/longjmp.s
	.globl	_start
_start:
	mov	x19, #3
	adrp	x0, jb
	add	x0, x0, :lo12:jb
	bl	setjmp
	mov	x20, x0
	bl	put_u64
	cbnz	x20, 1f
	bl	deeper
	mov	x0, #9
	bl	sys_exit
1:	mov	x0, x20
	bl	put_u64
	mov	x0, x19
	bl	sys_exit
deeper:
	stp	x29, x30, [sp, #-16]!
	mov	x19, #77
	bl	deepest
	ldp	x29, x30, [sp], #16
	ret
deepest:
	mov	x0, #5
	bl	put_u64
	adrp	x0, jb
	add	x0, x0, :lo12:jb
	mov	x1, #5
	bl	longjmp
	.bss
	.p2align 4
jb:	.zero	256
