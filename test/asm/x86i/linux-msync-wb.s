# x86i's hand-written program (queue 310, review 306 F4; emu's kernel, a64i's linux-msync-wb.s on x86-64): a MAP_SHARED file
# mapping's write-back writes only the bytes the program changed through it, and on an O_APPEND descriptor too. A file of its
# own (the executable's name, ".mswbk." and 16 letters of getrandom's), 8192 zero bytes, both pages mapped shared; 'X' at 0 and
# 'Y' at 100 stored, "WWWW" at 50 by pwrite64 between them, 'Z' at 4104 and 'Q' at 4110 stored, "VV" at 4106 by pwrite64
# between them; msync MS_SYNC; the file's bytes (the write(2)'s kept); munmap; then the file opened O_RDWR|O_APPEND and mapped
# shared: 'A' at 0 and 'B' at 4097, msync MS_SYNC, the file's bytes; 'C' at 2 and munmap: in the file; the descriptor still
# appends ("xyz" at the end, the size 8195) and F_GETFL still has O_APPEND; the file removed. Each line: a step's number and its
# value. The verdict: exit 0, stdout the CPU's.
# exit: 0
# judge: cpu
# with: lib/hex.s
	.globl	_start
	.text
_start:
	mov	8(%rsp), %r12
	lea	name(%rip), %r13
	xor	%ecx, %ecx
1:	movb	(%r12,%rcx), %al
	test	%al, %al
	jz	2f
	movb	%al, (%r13,%rcx)
	inc	%rcx
	jmp	1b
2:	lea	suffix(%rip), %rsi
	xor	%edx, %edx
3:	movb	(%rsi,%rdx), %al
	movb	%al, (%r13,%rcx)
	inc	%rcx
	inc	%rdx
	cmp	$7, %rdx
	jb	3b
	mov	%rcx, %rbx
	lea	rnd(%rip), %rdi
	mov	$8, %esi
	xor	%edx, %edx
	mov	$318, %eax
	syscall
	mov	rnd(%rip), %r8
	mov	$16, %r9d
4:	mov	%r8, %rax
	and	$15, %eax
	add	$97, %eax
	movb	%al, (%r13,%rbx)
	inc	%rbx
	shr	$4, %r8
	dec	%r9d
	jnz	4b
	movb	$0, (%r13,%rbx)
	# 1: openat(AT_FDCWD, name, O_RDWR|O_CREAT|O_TRUNC, 0600) >= 3; 2: ftruncate to 8192
	mov	$-100, %rdi
	mov	%r13, %rsi
	mov	$0x242, %edx
	mov	$0x180, %r10d
	mov	$257, %eax
	syscall
	mov	%rax, %r14
	xor	%esi, %esi
	cmp	$3, %rax
	setge	%sil
	mov	$1, %edi
	call	put_hex2
	mov	%r14, %rdi
	mov	$8192, %esi
	mov	$77, %eax
	syscall
	mov	$2, %edi
	mov	%rax, %rsi
	call	put_hex2
	# both pages mapped shared; 'X' at 0, 'Y' at 100; 3: pwrite64 "WWWW" at 50; 'Z' at 4104, 'Q' at 4110; 4: pwrite64 "VV" at 4106
	call	map2
	movb	$0x58, (%r15)
	movb	$0x59, 100(%r15)
	lea	wwww(%rip), %rsi
	mov	$4, %edx
	mov	$50, %r10d
	call	pwrite
	mov	$3, %edi
	mov	%rax, %rsi
	call	put_hex2
	movb	$0x5a, 4104(%r15)
	movb	$0x51, 4110(%r15)
	lea	vvv(%rip), %rsi
	mov	$2, %edx
	mov	$4106, %r10d
	call	pwrite
	mov	$4, %edi
	mov	%rax, %rsi
	call	put_hex2
	# 5: msync MS_SYNC; 6-9: the file's bytes at 48 ("WWWW" kept), 0, 96, 4104 ("VV" kept); 10: munmap
	mov	%r15, %rdi
	mov	$8192, %esi
	mov	$4, %edx
	mov	$26, %eax
	syscall
	mov	$5, %edi
	mov	%rax, %rsi
	call	put_hex2
	mov	$48, %esi
	mov	$6, %edi
	call	show8
	mov	$0, %esi
	mov	$7, %edi
	call	show8
	mov	$96, %esi
	mov	$8, %edi
	call	show8
	mov	$4104, %esi
	mov	$9, %edi
	call	show8
	mov	%r15, %rdi
	mov	$8192, %esi
	mov	$11, %eax
	syscall
	mov	$10, %edi
	mov	%rax, %rsi
	call	put_hex2
	# the descriptor closed; 11: openat O_RDWR|O_APPEND >= 3; both pages mapped shared, 'A' at 0, 'B' at 4097; 12: msync MS_SYNC;
	# 13, 14: the file's bytes at 0 and 4096
	mov	%r14, %rdi
	mov	$3, %eax
	syscall
	mov	$-100, %rdi
	mov	%r13, %rsi
	mov	$0x402, %edx
	xor	%r10d, %r10d
	mov	$257, %eax
	syscall
	mov	%rax, %r14
	xor	%esi, %esi
	cmp	$3, %rax
	setge	%sil
	mov	$11, %edi
	call	put_hex2
	call	map2
	movb	$0x41, (%r15)
	movb	$0x42, 4097(%r15)
	mov	%r15, %rdi
	mov	$8192, %esi
	mov	$4, %edx
	mov	$26, %eax
	syscall
	mov	$12, %edi
	mov	%rax, %rsi
	call	put_hex2
	mov	$0, %esi
	mov	$13, %edi
	call	show8
	mov	$4096, %esi
	mov	$14, %edi
	call	show8
	# 'C' at 2; 15: munmap; 16: the file's bytes at 0
	movb	$0x43, 2(%r15)
	mov	%r15, %rdi
	mov	$8192, %esi
	mov	$11, %eax
	syscall
	mov	$15, %edi
	mov	%rax, %rsi
	call	put_hex2
	mov	$0, %esi
	mov	$16, %edi
	call	show8
	# 17: write(2) "xyz" 3; 18: the size 8195 (lseek SEEK_END); 19: the bytes at 8192; 20: F_GETFL & (O_APPEND|O_ACCMODE)
	mov	%r14, %rdi
	lea	xyz(%rip), %rsi
	mov	$3, %edx
	mov	$1, %eax
	syscall
	mov	$17, %edi
	mov	%rax, %rsi
	call	put_hex2
	mov	%r14, %rdi
	xor	%esi, %esi
	mov	$2, %edx
	mov	$8, %eax
	syscall
	mov	$18, %edi
	mov	%rax, %rsi
	call	put_hex2
	mov	$8192, %esi
	mov	$19, %edi
	call	show8
	mov	%r14, %rdi
	mov	$3, %esi
	mov	$72, %eax
	syscall
	and	$0x403, %eax
	mov	$20, %edi
	mov	%rax, %rsi
	call	put_hex2
	# 21: the file removed (unlinkat); the descriptor closed
	mov	$-100, %rdi
	mov	%r13, %rsi
	xor	%edx, %edx
	mov	$263, %eax
	syscall
	mov	$21, %edi
	mov	%rax, %rsi
	call	put_hex2
	mov	%r14, %rdi
	mov	$3, %eax
	syscall
	xor	%edi, %edi
	call	sys_exit

# map2: r15 = mmap(0, 8192, PROT_READ|PROT_WRITE, MAP_SHARED, r14, 0)
map2:
	xor	%edi, %edi
	mov	$8192, %esi
	mov	$3, %edx
	mov	$1, %r10d
	mov	%r14, %r8
	xor	%r9d, %r9d
	mov	$9, %eax
	syscall
	mov	%rax, %r15
	ret

# pwrite: rax = pwrite64(r14, rsi, rdx, r10)
pwrite:
	mov	%r14, %rdi
	mov	$18, %eax
	syscall
	ret

# show8: put_hex2(rdi, the 8 bytes of r14's file at offset rsi: pread64 into buf, zero first)
show8:
	push	%rdi
	movq	$0, buf(%rip)
	mov	%rsi, %r10
	mov	%r14, %rdi
	lea	buf(%rip), %rsi
	mov	$8, %edx
	mov	$17, %eax
	syscall
	pop	%rdi
	mov	buf(%rip), %rsi
	call	put_hex2
	ret

	.section .rodata
suffix:	.ascii	".mswbk."
wwww:	.ascii	"WWWW"
vvv:	.ascii	"VV"
xyz:	.ascii	"xyz"
	.bss
	.align	16
rnd:	.zero	8
buf:	.zero	8
name:	.zero	1024
