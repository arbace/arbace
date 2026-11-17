# x86i's hand-written program (queue 315; a64i's smc.go of queue 303 on x86-64): code rewritten after it ran runs as rewritten.
# x86i's per-page decode cache compares an instruction's bytes at every fetch, and keeps a page's bytes only while emu's AS.Gen
# is unchanged (a mapping, an unmapping or a protection change). Each line: a step's number and its value.
# 1, 2: an anonymous RWX mapping (8192 bytes) at r15, `mov $1, %eax; ret` copied to its start and called (1); the immediate's
#   byte stored 2 and called again (2);
# 3: a loop copied to r15+64 that rewrites, in the page it runs in, an instruction it ran: `mov $1, %eax` run once, its immediate
#   stored 5, run again: 1 + 5 = 6;
# 4, 5, 6: an instruction across the two pages' boundary, `mov $0x44332211, %eax` at r15+4094 (its immediate's last three bytes
#   on the second page) and `ret` after it, called (44332211); the immediate's third byte, on the second page, stored 0xaa
#   (44aa2211); its first byte, on the first page, stored 0xbb (44aa22bb);
# 7, 8, 9: a file of its own (the executable's name, ".smc." and 16 letters of getrandom's) holds a page of code, A' at 0
#   (`mov $9, %eax; syscall; mov $2, %eax; ret`) and B' at 32 (`mov $4, %eax; ret`); an anonymous RWX page at r14 holds A
#   (the same with `mov $1, %eax`) and B (`mov $3, %eax`): B called (3); A called with mmap's arguments, which maps the file
#   MAP_PRIVATE|MAP_FIXED over the page running it: the instruction after the syscall is the file's (2); B called again: the
#   file's B' (4). The file removed. The verdict: exit 0, stdout the CPU's.
# exit: 0
# judge: cpu
# with: lib/hex.s
	.globl	_start
	.text
_start:
	# the file's name: argv[0], ".smc.", 16 letters of getrandom's
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
	cmp	$5, %rdx
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

	# 1, 2: r15 = mmap(0, 8192, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0); one, called, rewritten
	mov	$8192, %esi
	call	anon
	mov	%rax, %r15
	mov	%r15, %rdi
	lea	one(%rip), %rsi
	call	copy16
	call	*%r15
	mov	$1, %edi
	mov	%eax, %esi
	call	put_hex2
	movb	$2, 1(%r15)
	call	*%r15
	mov	$2, %edi
	mov	%eax, %esi
	call	put_hex2

	# 3: the loop at r15+64 rewriting its own instruction
	lea	64(%r15), %rdi
	lea	loop(%rip), %rsi
	mov	$48, %ecx
	rep movsb
	lea	64(%r15), %rax
	call	*%rax
	mov	$3, %edi
	mov	%eax, %esi
	call	put_hex2

	# 4, 5, 6: b8 11 22 33 44 c3 at r15+4094
	movb	$0xb8, 4094(%r15)
	movb	$0x11, 4095(%r15)
	movb	$0x22, 4096(%r15)
	movb	$0x33, 4097(%r15)
	movb	$0x44, 4098(%r15)
	movb	$0xc3, 4099(%r15)
	lea	4094(%r15), %rbx
	call	*%rbx
	mov	$4, %edi
	mov	%eax, %esi
	call	put_hex2
	movb	$0xaa, 4097(%r15)
	call	*%rbx
	mov	$5, %edi
	mov	%eax, %esi
	call	put_hex2
	movb	$0xbb, 4095(%r15)
	call	*%rbx
	mov	$6, %edi
	mov	%eax, %esi
	call	put_hex2

	# 7, 8, 9: the file's page (A' at 0, B' at 32) built in page and written; r12 = openat(AT_FDCWD, name, O_RDWR|O_CREAT|O_TRUNC, 0600)
	lea	page(%rip), %rdi
	lea	newa(%rip), %rsi
	call	copy16
	lea	page+32(%rip), %rdi
	lea	newb(%rip), %rsi
	call	copy16
	mov	$-100, %rdi
	mov	%r13, %rsi
	mov	$0x242, %edx
	mov	$0x180, %r10d
	mov	$257, %eax
	syscall
	mov	%rax, %r12
	mov	%r12, %rdi
	lea	page(%rip), %rsi
	mov	$4096, %edx
	mov	$1, %eax
	syscall
	# r14 = an anonymous RWX page holding A at 0 and B at 32
	mov	$4096, %esi
	call	anon
	mov	%rax, %r14
	mov	%r14, %rdi
	lea	olda(%rip), %rsi
	call	copy16
	lea	32(%r14), %rdi
	lea	oldb(%rip), %rsi
	call	copy16
	lea	32(%r14), %rax
	call	*%rax
	mov	$7, %edi
	mov	%eax, %esi
	call	put_hex2
	# A with mmap(r14, 4096, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_PRIVATE|MAP_FIXED, r12, 0)
	mov	%r14, %rdi
	mov	$4096, %esi
	mov	$7, %edx
	mov	$0x12, %r10d
	mov	%r12, %r8
	xor	%r9d, %r9d
	call	*%r14
	mov	$8, %edi
	mov	%eax, %esi
	call	put_hex2
	lea	32(%r14), %rax
	call	*%rax
	mov	$9, %edi
	mov	%eax, %esi
	call	put_hex2
	# the file removed (unlinkat), its descriptor closed
	mov	$-100, %rdi
	mov	%r13, %rsi
	xor	%edx, %edx
	mov	$263, %eax
	syscall
	mov	%r12, %rdi
	mov	$3, %eax
	syscall
	xor	%edi, %edi
	call	sys_exit

# anon: rax = mmap(0, rsi, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0)
anon:
	xor	%edi, %edi
	mov	$7, %edx
	mov	$0x22, %r10d
	mov	$-1, %r8
	xor	%r9d, %r9d
	mov	$9, %eax
	syscall
	ret

# copy16: 16 bytes from rsi to rdi
copy16:
	mov	$16, %ecx
	rep movsb
	ret

# the code copied (each at most 16 bytes; what follows it in the copy never runs)
one:
	mov	$1, %eax
	ret
	.align	16
loop:
	xor	%ecx, %ecx
	mov	$2, %edx
1:	mov	$1, %eax
	add	%eax, %ecx
	movb	$5, 1b+1(%rip)
	dec	%edx
	jnz	1b
	mov	%ecx, %eax
	ret
	.align	16
olda:
	mov	$9, %eax
	syscall
	mov	$1, %eax
	ret
	.align	16
oldb:
	mov	$3, %eax
	ret
	.align	16
newa:
	mov	$9, %eax
	syscall
	mov	$2, %eax
	ret
	.align	16
newb:
	mov	$4, %eax
	ret
	.align	16

	.section .rodata
suffix:	.ascii	".smc."
	.bss
	.align	4096
page:	.zero	4096
rnd:	.zero	8
name:	.zero	1024
