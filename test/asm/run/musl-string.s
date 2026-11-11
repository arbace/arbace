# Arbace's run test (AX4): musl 1.2.6's x86-64 memcpy, memmove and memset (src/string/x86_64) assembled by Arbace and called
# from a freestanding harness; memmove's jump to the hidden __memcpy_fwd crosses the two objects
# exit: 0
# stdout: "abcdefghijklmnopqrstuvwxyz\nabcabcdefghijklmnopqrstuvw\nxxxxxxxxxxxxxxxxxxxxxxxxxx\nabcdefghijklmnxxxxxxxxxxxx\n"
# with: lib/print.s musl:src/string/x86_64/memcpy.s musl:src/string/x86_64/memmove.s musl:src/string/x86_64/memset.s
	.globl	_start
_start:
	and	$-16, %rsp
	lea	buf(%rip), %rdi
	lea	alpha(%rip), %rsi
	mov	$26, %edx
	call	memcpy
	movb	$10, buf+26(%rip)
	lea	buf(%rip), %rdi
	mov	$27, %esi
	call	put_str
	lea	buf+3(%rip), %rdi
	lea	buf(%rip), %rsi
	mov	$23, %edx
	call	memmove
	lea	buf(%rip), %rdi
	mov	$27, %esi
	call	put_str
	lea	buf2(%rip), %rdi
	mov	$'x', %esi
	mov	$26, %edx
	call	memset
	movb	$10, buf2+26(%rip)
	lea	buf2(%rip), %rdi
	mov	$27, %esi
	call	put_str
	lea	buf2(%rip), %rdi
	lea	alpha(%rip), %rsi
	mov	$14, %edx
	call	memmove
	lea	buf2(%rip), %rdi
	mov	$27, %esi
	call	put_str
	xor	%edi, %edi
	call	sys_exit
	.section .rodata
alpha:	.ascii	"abcdefghijklmnopqrstuvwxyz"
	.bss
buf:	.zero	64
buf2:	.zero	64
