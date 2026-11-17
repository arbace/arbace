# x86i's hand-written program (AX5.4; AX5.5's model): cpuid answered by x86i's model (AXS58, AXS59): leaves 0 (the highest 7),
# 1 (RDRAND, POPCNT, MOVBE, CMPXCHG16B; SSE, SSE2), 0x80000000, 0x80000001 (LZCNT) and 7 (BMI1, BMI2, RDSEED, ADX; RDPID), EAX EBX then ECX EDX a line; rdtsc and rdtscp run, their values not written.
# The verdict: the stated stdout, the model's; the CPU's own answer differs (this CPU cannot fault cpuid: `# cpu: cpuid`, the
# class :cpuid for the CPU's run and the sys mode), and the step mode overwrites the native's answer with the model's.
# exit: 0
# stdout: "0000000000000007 0000000061627241\n000000004d566936 0000000038586563\n0000000000000000 0000000000000000\n0000000040c02000 00000000178bfbff\n0000000080000001 0000000000000000\n0000000000000000 0000000000000000\n0000000000000000 0000000000000000\n0000000000000020 0000000020100800\n0000000000000000 00000000000c0108\n0000000000400000 0000000000000000\n"
# cpu: cpuid
# with: lib/hex.s
	.globl	_start
	.text
_start:
	and	$-16, %rsp
	xor	%r12d, %r12d
1:	lea	leaves(%rip), %rax
	mov	(%rax,%r12,4), %eax
	xor	%ecx, %ecx
	cpuid
	mov	%rcx, %r13
	mov	%rdx, %r14
	mov	%rax, %rdi
	mov	%rbx, %rsi
	call	put_hex2
	mov	%r13, %rdi
	mov	%r14, %rsi
	call	put_hex2
	inc	%r12
	cmp	$5, %r12
	jne	1b
	rdtsc
	rdtscp
	xor	%eax, %eax
	xor	%ecx, %ecx
	xor	%edx, %edx
	xor	%edi, %edi
	call	sys_exit
	.section .rodata
	.align	4
leaves:	.long	0, 1, 0x80000000, 0x80000001, 7
