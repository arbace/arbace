# Arbace's run test (AR4): the linker-defined symbols: __ehdr_start (the ELF header's magic read through it), __start_ and
# __stop_ of a section named like a C identifier (its size), __bss_start and _end (the BSS between them), _etext after _start
# exit: 0
# stdout: "1\n16\n1\n1\n"
# with: lib/print.s
	.globl	_start
_start:
	adrp	x0, __ehdr_start
	add	x0, x0, :lo12:__ehdr_start
	ldr	w1, [x0]
	movz	w2, #0x457f
	movk	w2, #0x464c, lsl #16
	cmp	w1, w2
	cset	x0, eq
	bl	put_u64
	adrp	x1, __start_mysec
	add	x1, x1, :lo12:__start_mysec
	adrp	x2, __stop_mysec
	add	x2, x2, :lo12:__stop_mysec
	sub	x0, x2, x1
	bl	put_u64
	adrp	x1, __bss_start
	add	x1, x1, :lo12:__bss_start
	adrp	x2, _end
	add	x2, x2, :lo12:_end
	sub	x3, x2, x1
	cmp	x3, #64
	cset	x0, hs
	bl	put_u64
	adrp	x1, _etext
	add	x1, x1, :lo12:_etext
	adr	x2, _start
	cmp	x1, x2
	cset	x0, hi
	bl	put_u64
	mov	x0, #0
	bl	sys_exit
	.section mysec,"aw"
	.xword	1, 2
	.bss
buf:	.zero	64
