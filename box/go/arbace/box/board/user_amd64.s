// The crossings between the kernel and a process (user_amd64.go; doc/go/PROCESS-SPEC.md §3.2):
// the way in (enterUser: SYSRETQ or IRETQ), the ways out (sysentry, SYSCALL's; whim_ufault, an
// exception at ring 3, from vector_common), both into userexit, which returns from enterUser.
// While the kernel runs GS's base is the vCPU's userCPU; while a process runs, KERNEL_GS_BASE.

#include "go_asm.h"
#include "textflag.h"

TEXT ·rdmsr(SB),NOSPLIT,$0-16
	MOVL	msr+0(FP), CX
	RDMSR
	SHLQ	$32, DX
	ORQ	DX, AX
	MOVQ	AX, ret+8(FP)
	RET

TEXT ·wrmsr(SB),NOSPLIT,$0-16
	MOVL	msr+0(FP), CX
	MOVQ	v+8(FP), AX
	MOVQ	AX, DX
	SHRQ	$32, DX
	WRMSR
	RET

TEXT ·lgdt(SB),NOSPLIT,$0-8
	MOVQ	p+0(FP), AX
	LGDT	(AX)
	RET

TEXT ·sgdt(SB),NOSPLIT,$0-8
	MOVQ	p+0(FP), AX
	SGDT	(AX)
	RET

TEXT ·readCR3(SB),NOSPLIT,$0-8
	MOVQ	CR3, AX
	MOVQ	AX, ret+0(FP)
	RET

TEXT ·readCR4(SB),NOSPLIT,$0-8
	MOVQ	CR4, AX
	MOVQ	AX, ret+0(FP)
	RET

TEXT ·writeCR4(SB),NOSPLIT,$0-8
	MOVQ	v+0(FP), AX
	MOVQ	AX, CR4
	RET

// func cpuid(leaf, sub uint32) (a, b, c, d uint32)
TEXT ·cpuid(SB),NOSPLIT,$0-24
	MOVL	leaf+0(FP), AX
	MOVL	sub+4(FP), CX
	CPUID
	MOVL	AX, a+8(FP)
	MOVL	BX, b+12(FP)
	MOVL	CX, c+16(FP)
	MOVL	DX, d+20(FP)
	RET

TEXT ·sysentryAddr(SB),NOSPLIT,$0-8
	LEAQ	sysentry<>(SB), AX
	MOVQ	AX, ret+0(FP)
	RET

// func segData() (rwStart, dataEnd uintptr): the writable segment's first page and the end of its
// file-backed data (the start of bss), from the linker's symbols (doc/go/PROCESS-SPEC.md §2.1).
TEXT ·segData(SB),NOSPLIT,$0-16
	LEAQ	runtime·noptrdata(SB), AX
	ANDQ	$~0xfff, AX
	MOVQ	AX, rwStart+0(FP)
	LEAQ	runtime·bss(SB), AX
	MOVQ	AX, dataEnd+8(FP)
	RET

// func enterUser(c *UserContext): the kernel's SP, BP and g kept in the vCPU's area, the
// process's FPU state, FS base and tables loaded, then SYSRETQ (after a SYSCALL) or IRETQ (every
// register, FS's selector the user's data so that IRETQ does not null it). Returns when the
// process leaves (userexit).
TEXT ·enterUser(SB),NOSPLIT,$0-8
	MOVQ	c+0(FP), BX
	MOVQ	SP, userCPU_ksp(GS)
	MOVQ	BP, userCPU_kbp(GS)
	MOVQ	R14, userCPU_kg(GS)
	MOVQ	BX, userCPU_ctx(GS)
	MOVQ	userCPU_kfx(GS), AX
	FXSAVE64	(AX)
	MOVQ	UserContext_fx(BX), AX
	FXRSTOR64	(AX)
	CMPQ	UserContext_Iret(BX), $0
	JNE	iret

	MOVQ	UserContext_FSBase(BX), AX
	CMPQ	AX, userCPU_kfs(GS)
	JEQ	sysfs
	MOVQ	AX, DX
	SHRQ	$32, DX
	MOVL	$0xc0000100, CX
	WRMSR
sysfs:
	MOVQ	UserContext_CR3(BX), AX
	MOVQ	AX, CR3
	MOVQ	UserContext_IP(BX), CX
	MOVQ	UserContext_Flags(BX), R11
	MOVQ	UserContext_AX(BX), AX
	MOVQ	UserContext_DX(BX), DX
	MOVQ	UserContext_SI(BX), SI
	MOVQ	UserContext_DI(BX), DI
	MOVQ	UserContext_BP(BX), BP
	MOVQ	UserContext_R8(BX), R8
	MOVQ	UserContext_R9(BX), R9
	MOVQ	UserContext_R10(BX), R10
	MOVQ	UserContext_R12(BX), R12
	MOVQ	UserContext_R13(BX), R13
	MOVQ	UserContext_R14(BX), R14
	MOVQ	UserContext_R15(BX), R15
	MOVQ	UserContext_SP(BX), SP
	MOVQ	UserContext_BX(BX), BX
	SWAPGS
	BYTE	$0x48; BYTE $0x0f; BYTE $0x07	// SYSRETQ: Go's SYSRET has no REX.W

iret:
	MOVL	$const_selUserData, AX
	BYTE	$0x8e; BYTE $0xe0	// MOV FS, AX
	MOVQ	UserContext_FSBase(BX), AX
	MOVQ	AX, DX
	SHRQ	$32, DX
	MOVL	$0xc0000100, CX
	WRMSR
	MOVQ	UserContext_CR3(BX), AX
	MOVQ	AX, CR3
	PUSHQ	$const_selUserData
	PUSHQ	UserContext_SP(BX)
	PUSHQ	UserContext_Flags(BX)
	PUSHQ	$const_selUserCode
	PUSHQ	UserContext_IP(BX)
	MOVQ	UserContext_AX(BX), AX
	MOVQ	UserContext_CX(BX), CX
	MOVQ	UserContext_DX(BX), DX
	MOVQ	UserContext_SI(BX), SI
	MOVQ	UserContext_DI(BX), DI
	MOVQ	UserContext_BP(BX), BP
	MOVQ	UserContext_R8(BX), R8
	MOVQ	UserContext_R9(BX), R9
	MOVQ	UserContext_R10(BX), R10
	MOVQ	UserContext_R11(BX), R11
	MOVQ	UserContext_R12(BX), R12
	MOVQ	UserContext_R13(BX), R13
	MOVQ	UserContext_R14(BX), R14
	MOVQ	UserContext_R15(BX), R15
	MOVQ	UserContext_BX(BX), BX
	SWAPGS
	IRETQ

// sysentry is LSTAR: a process's SYSCALL (RAX its call block), at ring 0 on the process's stack,
// RCX its IP, R11 its flags. Its registers into the context; SP is the base meanwhile.
TEXT sysentry<>(SB),NOSPLIT|NOFRAME,$0
	SWAPGS
	MOVQ	SP, userCPU_scratch(GS)
	MOVQ	userCPU_ctx(GS), SP
	MOVQ	AX, UserContext_AX(SP)
	MOVQ	BX, UserContext_BX(SP)
	MOVQ	CX, UserContext_CX(SP)
	MOVQ	DX, UserContext_DX(SP)
	MOVQ	SI, UserContext_SI(SP)
	MOVQ	DI, UserContext_DI(SP)
	MOVQ	BP, UserContext_BP(SP)
	MOVQ	R8, UserContext_R8(SP)
	MOVQ	R9, UserContext_R9(SP)
	MOVQ	R10, UserContext_R10(SP)
	MOVQ	R11, UserContext_R11(SP)
	MOVQ	R12, UserContext_R12(SP)
	MOVQ	R13, UserContext_R13(SP)
	MOVQ	R14, UserContext_R14(SP)
	MOVQ	R15, UserContext_R15(SP)
	MOVQ	CX, UserContext_IP(SP)
	MOVQ	R11, UserContext_Flags(SP)
	MOVQ	userCPU_scratch(GS), AX
	MOVQ	AX, UserContext_SP(SP)
	MOVQ	$const_TrapSyscall, UserContext_Trap(SP)
	MOVQ	$0, UserContext_Iret(SP)
	MOVQ	SP, BX
	JMP	userexit<>(SB)

// whim_ufault is vector_common's way for an exception at ring 3 (boot_amd64.s): on IST1, 0 the
// vector, 8 its code, 16 RIP, 24 CS, 32 RFLAGS, 40 RSP, 48 SS. The flags made the kernel's (DF
// and AC clear), the registers and the frame into the context.
TEXT whim_ufault(SB),NOSPLIT|NOFRAME,$0
	SWAPGS
	PUSHQ	$2
	POPFQ
	PUSHQ	BX
	MOVQ	userCPU_ctx(GS), BX
	MOVQ	AX, UserContext_AX(BX)
	MOVQ	CX, UserContext_CX(BX)
	MOVQ	DX, UserContext_DX(BX)
	MOVQ	SI, UserContext_SI(BX)
	MOVQ	DI, UserContext_DI(BX)
	MOVQ	BP, UserContext_BP(BX)
	MOVQ	R8, UserContext_R8(BX)
	MOVQ	R9, UserContext_R9(BX)
	MOVQ	R10, UserContext_R10(BX)
	MOVQ	R11, UserContext_R11(BX)
	MOVQ	R12, UserContext_R12(BX)
	MOVQ	R13, UserContext_R13(BX)
	MOVQ	R14, UserContext_R14(BX)
	MOVQ	R15, UserContext_R15(BX)
	POPQ	AX
	MOVQ	AX, UserContext_BX(BX)
	MOVQ	0(SP), AX
	MOVQ	AX, UserContext_Vector(BX)
	MOVQ	8(SP), AX
	MOVQ	AX, UserContext_Code(BX)
	MOVQ	16(SP), AX
	MOVQ	AX, UserContext_IP(BX)
	MOVQ	32(SP), AX
	MOVQ	AX, UserContext_Flags(BX)
	MOVQ	40(SP), AX
	MOVQ	AX, UserContext_SP(BX)
	MOVQ	CR2, AX
	MOVQ	AX, UserContext_CR2(BX)
	MOVQ	$const_TrapFault, UserContext_Trap(BX)
	MOVQ	$1, UserContext_Iret(BX)
	JMP	userexit<>(SB)

// userexit: BX the context, GS the vCPU's area. The kernel's tables, the process's FPU state
// saved and the kernel's MXCSR, the kernel's FS base, SP, BP and g: enterUser returns.
TEXT userexit<>(SB),NOSPLIT|NOFRAME,$0
	MOVQ	userCPU_kcr3(GS), AX
	MOVQ	AX, CR3
	MOVQ	UserContext_fx(BX), AX
	FXSAVE64	(AX)
	MOVQ	userCPU_kfx(GS), AX
	FXRSTOR64	(AX)
	MOVQ	userCPU_kfs(GS), AX
	MOVQ	AX, DX
	SHRQ	$32, DX
	MOVL	$0xc0000100, CX
	WRMSR
	MOVQ	userCPU_ksp(GS), SP
	MOVQ	userCPU_kbp(GS), BP
	MOVQ	userCPU_kg(GS), R14
	RET

// func sysentryBenchAddr() uintptr
TEXT ·sysentryBenchAddr(SB),NOSPLIT,$0-8
	LEAQ	sysentryBench<>(SB), AX
	MOVQ	AX, ret+0(FP)
	RET

// sysentryBench is LSTAR while the kernel measures the crossings (BenchEntry; doc/go/PROCESS-SPEC.md,
// "As built: KU2", KS13): RAX 1 is the bare SYSCALL+SYSRETQ, nothing saved; 2 the crossing's two
// CR3 writes as built (the kernel's tables, then the process's again, its TLB entries kept); 3 the
// same with the process's entries flushed (its CR3 without bit 63); 4 all the crossing's machine
// state as built, the CR3 writes, the FPU states and the FS base, without the Go side; any other
// RAX the ordinary entry, sysentry. The process's RAX is lost in 1-4, and RDX in 4, nothing else.
TEXT sysentryBench<>(SB),NOSPLIT|NOFRAME,$0
	CMPQ	AX, $1
	JNE	cr3keep
	BYTE	$0x48; BYTE $0x0f; BYTE $0x07	// SYSRETQ
cr3keep:
	CMPQ	AX, $2
	JNE	cr3flush
	SWAPGS
	MOVQ	userCPU_kcr3(GS), AX
	MOVQ	AX, CR3
	MOVQ	userCPU_ctx(GS), AX
	MOVQ	UserContext_CR3(AX), AX
	MOVQ	AX, CR3
	SWAPGS
	BYTE	$0x48; BYTE $0x0f; BYTE $0x07	// SYSRETQ
cr3flush:
	CMPQ	AX, $3
	JNE	ordinary
	SWAPGS
	MOVQ	userCPU_kcr3(GS), AX
	MOVQ	AX, CR3
	MOVQ	userCPU_ctx(GS), AX
	MOVQ	UserContext_CR3(AX), AX
	BTRQ	$63, AX
	MOVQ	AX, CR3
	SWAPGS
	BYTE	$0x48; BYTE $0x0f; BYTE $0x07	// SYSRETQ
ordinary:
	CMPQ	AX, $4
	JNE	entry
	// 4: what the crossing does but the Go side: CR3 both ways, the FPU states swapped both ways,
	// the kernel's FS base written and the process's again when it differs (RCX kept in scratch)
	SWAPGS
	MOVQ	CX, userCPU_scratch(GS)
	MOVQ	userCPU_kcr3(GS), AX
	MOVQ	AX, CR3
	MOVQ	userCPU_ctx(GS), AX
	MOVQ	UserContext_fx(AX), AX
	FXSAVE64	(AX)
	MOVQ	userCPU_kfx(GS), AX
	FXRSTOR64	(AX)
	MOVQ	userCPU_kfs(GS), AX
	MOVQ	AX, DX
	SHRQ	$32, DX
	MOVL	$0xc0000100, CX
	WRMSR
	MOVQ	userCPU_kfx(GS), AX
	FXSAVE64	(AX)
	MOVQ	userCPU_ctx(GS), AX
	MOVQ	UserContext_fx(AX), AX
	FXRSTOR64	(AX)
	MOVQ	userCPU_ctx(GS), AX
	MOVQ	UserContext_FSBase(AX), AX
	CMPQ	AX, userCPU_kfs(GS)
	JEQ	samefs
	MOVQ	AX, DX
	SHRQ	$32, DX
	MOVL	$0xc0000100, CX
	WRMSR
samefs:
	MOVQ	userCPU_ctx(GS), AX
	MOVQ	UserContext_CR3(AX), AX
	MOVQ	AX, CR3
	MOVQ	userCPU_scratch(GS), CX
	SWAPGS
	BYTE	$0x48; BYTE $0x0f; BYTE $0x07	// SYSRETQ
entry:
	JMP	sysentry<>(SB)

// func benchCalls(ax uint64, n uint64) (ticks uint64): n SYSCALLs with RAX ax, from a process at
// ring 3, and the counter's ticks they took. The registers it keeps across them (R12, R13) are
// ones every way through the kernel restores.
TEXT ·benchCalls(SB),NOSPLIT,$0-24
	MOVQ	n+8(FP), R12
	RDTSC
	SHLQ	$32, DX
	ORQ	DX, AX
	MOVQ	AX, R13
again:
	MOVQ	ax+0(FP), AX
	SYSCALL
	DECQ	R12
	JNZ	again
	RDTSC
	SHLQ	$32, DX
	ORQ	DX, AX
	SUBQ	R13, AX
	MOVQ	AX, ticks+16(FP)
	RET
