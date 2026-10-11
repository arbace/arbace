// The board's entry, exception vectors and the instructions Go has no
// spelling for, on arm64.

#include "go_asm.h"
#include "textflag.h"

// cpuinit is TamaGo's CPUInit: the monitor has put the vCPU at EL1t (on
// SP_EL0, D, A, I and F masked) with the MMU on and identity-mapped
// (vmm/setup_arm64.go), X0 argc, X1 argv, X2 RamStart, X3 RamSize and X4
// RamStackOffset (guest/abi).  What is left is the board's: keep them,
// enable FP and SIMD, which Go's code uses (CPACR_EL1.FPEN), take the
// counter's rate and zero, put the stack where the runtime expects it, and
// start the runtime.
TEXT cpuinit(SB),NOSPLIT|NOFRAME,$0
	MOVD	R5, ·ncpu(SB)
	MOVD	R0, ·bootArgc(SB)
	MOVD	R1, ·bootArgv(SB)
	MOVD	R2, runtime∕goos·RamStart(SB)
	MOVD	R3, runtime∕goos·RamSize(SB)
	MOVD	R4, runtime∕goos·RamStackOffset(SB)
	MOVD	$whim_vectors(SB), R5	// kept by the linker: VBAR_EL1 names it
	MOVD	R5, ·vectors(SB)
	MOVD	$whim_apentry(SB), R5	// and the monitor starts an AP here
	MOVD	R5, ·apEntry(SB)

	MRS	CPACR_EL1, R5
	ORR	$(3<<20), R5
	MSR	R5, CPACR_EL1
	ISB	$15

	MRS	CNTFRQ_EL0, R5
	MOVD	$1000, R6
	UDIV	R6, R5, R5
	MOVD	R5, ·tickKHz(SB)
	MRS	CNTVCT_EL0, R5
	MOVD	R5, ·tickBase(SB)

	ADD	R3, R2, R5
	SUB	R4, R5, R5
	MOVD	R5, RSP
	B	_rt0_tamago_start(SB)

// whim_apentry is an AP's first instruction (abi.CPUStart, guest/abi): the
// boot vCPU's mode, tables and system registers, its own SP_EL1, X0 the
// M's g0, X1 the function to call (the runtime's mstart), SP_EL0 its
// stack.  FP and SIMD as cpuinit turns them on, g in its register, and
// the call, which does not return.
TEXT whim_apentry(SB),NOSPLIT|NOFRAME,$0
	MRS	CPACR_EL1, R5
	ORR	$(3<<20), R5
	MSR	R5, CPACR_EL1
	ISB	$15
	MOVD	R0, g
	CALL	(R1)
	WORD	$0	// UDF: a fault the vectors report

// func doorbell(cb unsafe.Pointer): the hypercall, one STR from X0 to the doorbell
// (an ISV data abort the monitor decodes; KVM is checked against X0).
TEXT ·doorbell(SB),NOSPLIT,$0-8
	MOVD	cb+0(FP), R0
	MOVD	$const_doorbellAddr, R1
	MOVD	R0, (R1)
	RET

// func ticks() uint64: the virtual counter
TEXT ·ticks(SB),NOSPLIT,$0-8
	ISB	$15
	MRS	CNTVCT_EL0, R0
	MOVD	R0, ret+0(FP)
	RET

// func pause()
TEXT ·pause(SB),NOSPLIT,$0-0
	YIELD
	RET

// whim_vectors is VBAR_EL1's table: 16 entries of 128 bytes, 2 KiB
// aligned, each reporting its number, ESR_EL1, ELR_EL1 and FAR_EL1.
TEXT whim_vectors(SB),NOSPLIT|NOFRAME,$0
	PCALIGN	$2048
	MOVD	$0, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$1, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$2, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$3, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$4, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$5, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$6, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$7, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$8, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$9, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$10, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$11, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$12, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$13, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$14, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$15, R0
	MRS	ESR_EL1, R1
	MRS	ELR_EL1, R2
	MRS	FAR_EL1, R3
	B	vector_common<>(SB)
	PCALIGN	$128

// vector_common reports the exception by abi.Fault from a block of its own,
// and never returns: the monitor ends the guest as the C editor dies of a
// SIGSEGV.
TEXT vector_common<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	$·faultArea(SB), R5
	ADD	$(const_blockSize-1), R5
	AND	$~(const_blockSize-1), R5
	MOVD	$const_callFault, R6
	MOVD	R6, 0(R5)
	MOVD	R0, 8(R5)
	MOVD	R1, 16(R5)
	MOVD	R2, 24(R5)
	MOVD	R3, 32(R5)
	MOVD	R5, R0
	MOVD	$const_doorbellAddr, R1
again:
	MOVD	R0, (R1)
	B	again
