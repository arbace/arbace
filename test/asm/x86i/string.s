# x86i's hand-written program (AX5.2): the string family: movs, stos, lods, scas and cmps at the four sizes, with rep, repe and
# repne, forward and backward (DF), the buffers and the registers printed after each. The verdict: exit 0, stdout the CPU's.
# exit: 0
# judge: cpu
# with: lib/hex.s
	.globl	_start
	.text
_start:
	and	$-16, %rsp
	cld
	xor	%eax, %eax		# the flags known (and's depend on the stack's address)
	# rep movsb, forward, 13 bytes into dst
	lea	src(%rip), %rsi
	lea	dst(%rip), %rdi
	mov	$13, %ecx
	rep movsb
	call	regs
	call	dump
	# rep movsw, movsl, movsq
	lea	src(%rip), %rsi
	lea	dst+1(%rip), %rdi
	mov	$5, %ecx
	rep movsw
	call	regs
	lea	src+2(%rip), %rsi
	lea	dst+16(%rip), %rdi
	mov	$3, %ecx
	rep movsl
	call	regs
	lea	src(%rip), %rsi
	lea	dst+32(%rip), %rdi
	mov	$2, %ecx
	rep movsq
	call	regs
	call	dump
	# overlapping, backward: memmove's way
	std
	lea	dst+20(%rip), %rsi
	lea	dst+23(%rip), %rdi
	mov	$21, %ecx
	rep movsb
	cld
	call	regs
	call	dump
	# stos at each size, one without rep
	mov	$0x1122334455667788, %rax
	lea	dst(%rip), %rdi
	stosb
	mov	$3, %ecx
	rep stosw
	mov	$2, %ecx
	rep stosl
	mov	$2, %ecx
	rep stosq
	call	regs
	call	dump
	std
	lea	dst+47(%rip), %rdi
	mov	$0xa5, %al
	mov	$7, %ecx
	rep stosb
	cld
	call	regs
	call	dump
	# lods
	lea	src(%rip), %rsi
	lodsb
	lodsw
	lodsl
	lodsq
	call	regs
	# repne scasb: the first 'k' of src
	lea	src(%rip), %rdi
	mov	$'k', %al
	mov	$40, %ecx
	repne scasb
	call	regs
	# repe scasb over equal bytes, then a difference
	lea	same(%rip), %rdi
	mov	$'z', %al
	mov	$10, %ecx
	repe scasb
	call	regs
	# repe cmpsb: src and src2 differ at 6
	lea	src(%rip), %rsi
	lea	src2(%rip), %rdi
	mov	$16, %ecx
	repe cmpsb
	call	regs
	lea	src(%rip), %rsi
	lea	src2(%rip), %rdi
	mov	$4, %ecx
	repe cmpsq
	call	regs
	lea	src(%rip), %rsi
	lea	src(%rip), %rdi
	mov	$5, %ecx
	repne cmpsw
	call	regs
	# rep with RCX 0 does nothing
	xor	%ecx, %ecx
	lea	dst(%rip), %rdi
	rep stosb
	call	regs
	xor	%edi, %edi
	call	sys_exit

# regs: RSI, RDI, RCX, RAX (relative to src for the pointers) and the flags (CF PF AF ZF SF OF)
regs:
	pushfq
	pop	%r8
	and	$0x8d5, %r8
	push	%r8
	push	%rax
	push	%rcx
	lea	src(%rip), %rax
	sub	%rax, %rsi
	sub	%rax, %rdi
	push	%rdi
	mov	%rsi, %rdi
	pop	%rsi
	call	put_hex2
	pop	%rdi
	pop	%rsi
	call	put_hex2
	pop	%rdi
	xor	%esi, %esi
	call	put_hex2
	ret

dump:
	lea	dst(%rip), %rdi
	mov	$48, %esi
	jmp	put_mem

	.data
src:	.ascii	"abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOP"
src2:	.ascii	"abcdefXhijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOP"
same:	.ascii	"zzzzzzzyzz"
dst:	.zero	64
