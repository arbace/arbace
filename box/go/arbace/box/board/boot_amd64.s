// The board's entry, exception vectors and the instructions Go has no
// spelling for.

#include "go_asm.h"
#include "textflag.h"

// cpuinit is TamaGo's CPUInit (runtime/goos.CPUInit jumps here): the first
// instruction the guest runs.  The monitor has put the vCPU in 64-bit ring
// 0 with paging on, identity-mapped, interrupts off (guest/abi's entry
// state); what is left is the board's: keep the entry's registers, turn on
// SSE, which Go's code uses (CR0.EM off, CR0.MP on, CR4.OSFXSR and
// OSXMMEXCPT on -- not OSXSAVE, so the runtime finds no AVX to use and no
// extended state is set up), put the stack where the runtime expects it,
// and start the runtime.
TEXT cpuinit(SB),NOSPLIT|NOFRAME,$0
	CLI
	MOVQ	DI, ·bootArgc(SB)
	MOVQ	SI, ·bootArgv(SB)
	MOVQ	DX, runtime∕goos·RamStart(SB)
	MOVQ	CX, runtime∕goos·RamSize(SB)
	MOVQ	R8, runtime∕goos·RamStackOffset(SB)
	MOVQ	R9, ·tickKHz(SB)
	MOVQ	R10, ·ncpu(SB)
	MOVQ	R11, ·wallBase(SB)
	LEAQ	whim_vectors(SB), AX	// kept by the linker: the monitor's IDT names it
	MOVQ	AX, ·vectors(SB)
	LEAQ	whim_apentry(SB), AX	// and the monitor starts an AP here
	MOVQ	AX, ·apEntry(SB)

	MOVQ	CR0, AX
	ANDQ	$~(1<<2), AX
	ORQ	$(1<<1), AX
	MOVQ	AX, CR0
	MOVQ	CR4, AX
	ORQ	$(1<<9 | 1<<10), AX
	MOVQ	AX, CR4

	MOVQ	DX, SP
	ADDQ	CX, SP
	SUBQ	R8, SP

	RDTSC
	SHLQ	$32, DX
	ORQ	DX, AX
	MOVQ	AX, ·tickBase(SB)

	JMP	_rt0_tamago_start(SB)

// whim_apentry is an AP's first instruction (abi.CPUStart, guest/abi): the
// boot vCPU's mode and tables, RDI the M's g0, RSI the function to call
// (the runtime's mstart), RDX its number, RSP its stack.  SSE as cpuinit
// turns it on, FS's base 8 past its slot in tls and g there, where the
// runtime's ABI0 code finds it, and the call, which does not return.
TEXT whim_apentry(SB),NOSPLIT|NOFRAME,$0
	CLI
	MOVQ	CR0, AX
	ANDQ	$~(1<<2), AX
	ORQ	$(1<<1), AX
	MOVQ	AX, CR0
	MOVQ	CR4, AX
	ORQ	$(1<<9 | 1<<10), AX
	MOVQ	AX, CR4
	LEAQ	·tls(SB), AX
	SHLQ	$4, DX
	LEAQ	8(AX)(DX*1), AX
	MOVQ	AX, DX
	SHRQ	$32, DX
	MOVL	$0xc0000100, CX	// IA32_FS_BASE
	WRMSR
	MOVQ	DI, R14
	MOVQ	R14, (TLS)
	CALL	SI
	BYTE	$0x0f; BYTE $0x0b	// UD2: a fault the vectors report

// whim_uentry is a process's first instruction (doc/go/PROCESS-SPEC.md §3.3, KU1): the kernel
// has put the vCPU at ring 3 with the entry registers (as cpuinit's: DI argc, SI argv, DX
// RamStart, CX RamSize, R8 RamStackOffset, R9 tickKHz, R10 ncpu, R11 wallBase). Nothing
// privileged: the kernel holds CR0/CR4 (a process cannot, and SSE is on already). It records
// that it is a process, so doorbell makes a SYSCALL, keeps the entry state, sets the stack and
// starts the runtime -- a second TamaGo runtime, in the process's own address space.
TEXT whim_uentry(SB),NOSPLIT|NOFRAME,$0
	MOVB	$1, ·process(SB)
	MOVQ	DI, ·bootArgc(SB)
	MOVQ	SI, ·bootArgv(SB)
	MOVQ	DX, runtime∕goos·RamStart(SB)
	MOVQ	CX, runtime∕goos·RamSize(SB)
	MOVQ	R8, runtime∕goos·RamStackOffset(SB)
	MOVQ	R9, ·tickKHz(SB)
	MOVQ	R10, ·ncpu(SB)
	MOVQ	R11, ·wallBase(SB)
	MOVQ	DX, SP
	ADDQ	CX, SP
	SUBQ	R8, SP
	RDTSC
	SHLQ	$32, DX
	ORQ	DX, AX
	MOVQ	AX, ·tickBase(SB)
	JMP	_rt0_tamago_start(SB)

// func UEntryAddr() uintptr
TEXT ·UEntryAddr(SB),NOSPLIT,$0-8
	LEAQ	whim_uentry(SB), AX
	MOVQ	AX, ret+0(FP)
	RET

// func doorbell(cb unsafe.Pointer): a guest's hypercall is one 64-bit store from RAX to the
// doorbell, which exits to the monitor and resumes after the store; a process's (process set,
// whim_uentry) is a SYSCALL with RAX the block's address, which the kernel serves (§3.4, §4.1).
TEXT ·doorbell(SB),NOSPLIT,$0-8
	MOVQ	cb+0(FP), AX
	CMPB	·process(SB), $0
	JNE	sys
	MOVQ	$const_doorbellAddr, BX
	MOVQ	AX, (BX)
	RET
sys:
	SYSCALL
	RET

// func ticks() uint64: the time-stamp counter
TEXT ·ticks(SB),NOSPLIT,$0-8
	RDTSC
	SHLQ	$32, DX
	ORQ	DX, AX
	MOVQ	AX, ret+0(FP)
	RET

// func pause()
TEXT ·pause(SB),NOSPLIT,$0-0
	PAUSE
	RET

// whim_vectors is the monitor's IDT's 32 gates (vmm/setup_amd64.go), 16
// bytes apart, every one on IST1: the vector, and an error code where the
// CPU pushes none, then the common report.
TEXT whim_vectors(SB),NOSPLIT|NOFRAME,$0
	PUSHQ	$0
	PUSHQ	$0
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$1
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$2
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$3
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$4
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$5
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$6
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$7
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$8
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$9
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$10
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$11
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$12
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$13
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$14
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$15
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$16
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$17
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$18
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$19
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$20
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$21
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$22
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$23
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$24
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$25
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$26
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$27
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$28
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$29
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$30
	JMP	vector_common<>(SB)
	PCALIGN	$16
	PUSHQ	$0
	PUSHQ	$31
	JMP	vector_common<>(SB)
	PCALIGN	$16

// vector_common reports the exception by abi.Fault -- the vector, the
// error code, RIP and CR2 -- from a block of its own, and never returns:
// the monitor ends the guest as the C editor dies of a SIGSEGV.  But for
// a page fault below the first page once NilPanics has set sigpanicPC (a
// nil dereference, Arbace's addition): that returns to sigpanic, as if the
// faulting instruction had called it -- its address pushed on the
// interrupted stack, every register else as it was -- as Linux's signal
// handler makes a SIGSEGV a run-time panic.
TEXT vector_common<>(SB),NOSPLIT|NOFRAME,$0
	// 0 the vector, 8 the error code, 16 RIP, 24 CS: an exception at ring 3 is a process's,
	// the kernel's to answer (user_amd64.s; doc/go/PROCESS-SPEC.md §8, KU1)
	TESTB	$3, 24(SP)
	JNZ	user
	CMPQ	0(SP), $14
	JNE	report
	PUSHQ	AX
	MOVQ	CR2, AX
	CMPQ	AX, $0x1000
	JCC	reportAX
	MOVQ	·sigpanicPC(SB), AX
	TESTQ	AX, AX
	JZ	reportAX
	// 0 BX, 8 AX, 16 the vector, 24 the error code, 32 RIP, 40 CS, 48 RFLAGS, 56 RSP, 64 SS
	PUSHQ	BX
	MOVQ	56(SP), BX
	SUBQ	$8, BX
	MOVQ	32(SP), AX
	MOVQ	AX, (BX)
	MOVQ	BX, 56(SP)
	MOVQ	·sigpanicPC(SB), AX
	MOVQ	AX, 32(SP)
	POPQ	BX
	POPQ	AX
	ADDQ	$16, SP
	IRETQ
reportAX:
	POPQ	AX
report:
	POPQ	DI
	POPQ	SI
	MOVQ	(SP), DX
	MOVQ	CR2, CX
	LEAQ	·faultArea(SB), AX
	ADDQ	$(const_blockSize-1), AX
	ANDQ	$~(const_blockSize-1), AX
	MOVQ	$const_callFault, 0(AX)
	MOVQ	DI, 8(AX)
	MOVQ	SI, 16(AX)
	MOVQ	DX, 24(AX)
	MOVQ	CX, 32(AX)
	MOVQ	$const_doorbellAddr, BX
again:
	MOVQ	AX, (BX)
	JMP	again
user:
	JMP	whim_ufault(SB)
