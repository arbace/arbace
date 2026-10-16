// The bare guest on arm64 (main.go): go-whim's guest/rt/entry_arm64.S and what of rt.c and the
// stand-ins the tests run, in Go's assembly. The monitor starts the vCPU at EL1t (interrupts
// masked) at _start, SP_EL0 the stack's top, R0 argc, R1 argv and R2 the heap, the MMU on. An
// exception is taken at EL1h, on SP_EL1, the fault stack: a fault in the stack's guard page is
// reported too. The routines keep their link register in R19 (wait, read) or R20 (ahead), the
// stack untouched but by deep.

#include "textflag.h"

// the calls (package abi's numbers)
#define READ_INPUT 9
#define SUSPEND 10
#define EXIT 11
#define WRITE 15
#define FAULT 18
#define WAIT_READ 20
// the read-ahead's buffer: rt.c's WHIM_INPUT_BYTES
#define INPUT_BYTES 250

// the call block, 64 bytes at a 64-byte boundary in callarea (rt.c's whim_call)
GLOBL callarea<>(SB), NOPTR, $128
// what a wait read ahead (rt.c's whim_input, whim_input_kept, _ret and _at)
GLOBL input<>(SB), NOPTR, $256
GLOBL kept<>(SB), NOPTR, $8
GLOBL kret<>(SB), NOPTR, $8
GLOBL kat<>(SB), NOPTR, $8
// the read-ahead stand-in's reads and what it writes
GLOBL buf<>(SB), NOPTR, $256
GLOBL out<>(SB), NOPTR, $640
GLOBL outat<>(SB), NOPTR, $8

DATA hello<>+0(SB)/6, $"hello\n"
GLOBL hello<>(SB), RODATA|NOPTR, $8

// _start: argv[1]'s first byte chooses (main.go); the exit code from R0
TEXT _start(SB),NOSPLIT|NOFRAME,$0
	MOVD	$whim_vectors(SB), R5	// kept by the linker: VBAR_EL1 names it
	CMP	$2, R0
	BLT	hello
	MOVD	8(R1), R5
	MOVBU	(R5), R5
	CMP	$'n', R5
	BEQ	null
	CMP	$'d', R5
	BEQ	deep
	CMP	$'s', R5
	BEQ	spin
	CMP	$'a', R5
	BEQ	ahead
hello:
	MOVD	$WRITE, R8
	MOVD	$hello<>(SB), R9
	MOVD	$6, R10
	BL	hcall<>(SB)
	MOVD	$3, R0
	B	exit
null:
	MOVD	$1, R6
	MOVD	ZR, R5	// a base register: (ZR) would be the stack pointer
	MOVW	R6, (R5)
	B	exit
deep:
	MOVD	$1, R6
	MOVD	RSP, R7	// by a register: the linker's nosplit check counts an SUB from RSP
again1:
	SUB	$4096, R7
	MOVB	R6, (R7)
	MOVD	R7, RSP
	B	again1
spin:
	B	spin
ahead:
	BL	ahead<>(SB)
exit:
	MOVD	R0, R9
	MOVD	$EXIT, R8
again:
	BL	hcall<>(SB)	// the monitor never resumes an exit
	B	again

// hcall makes call R8 with R9-R12 its arguments (rt.c's whim_hcall): the block filled, its
// address stored to the doorbell from R0; R0 the call's ret, R1 its a[0]. R2 and R3 used.
TEXT hcall<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	$callarea<>(SB), R2
	ADD	$63, R2
	AND	$~63, R2
	MOVD	R8, 0(R2)
	MOVD	R9, 8(R2)
	MOVD	R10, 16(R2)
	MOVD	R11, 24(R2)
	MOVD	R12, 32(R2)
	MOVD	ZR, 40(R2)
	MOVD	ZR, 48(R2)
	MOVD	ZR, 56(R2)
	MOVD	R2, R0
	MOVD	$0xf0000000, R3
	MOVD	R0, (R3)
	MOVD	48(R2), R0
	MOVD	8(R2), R1
	RET

// wait is rt.c's musl_wait_for_input(R9 ms): 1 while a read's answer is kept; else one call,
// WAIT_READ, which reads ahead into input when there is input. R0 the answer.
TEXT wait<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	R30, R19
	MOVD	kept<>(SB), R0
	CBNZ	R0, yes
	MOVD	$WAIT_READ, R8
	MOVD	$input<>(SB), R10
	MOVD	$INPUT_BYTES, R11
	BL	hcall<>(SB)
	CBZ	R0, no
	MOVD	$1, R2
	MOVD	R2, kept<>(SB)
	MOVD	R1, kret<>(SB)
	MOVD	ZR, kat<>(SB)
yes:
	MOVD	$1, R0
no:
	MOVD	R19, R30
	RET

// read is rt.c's musl_read_input(R9 buf, R10 len): served from what the wait read while any is
// kept -- the bytes, as many as len, or the read's 0 or -1 -- else a call of its own. R0 the
// count.
TEXT read<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	R30, R19
	MOVD	kept<>(SB), R0
	CBZ	R0, call
	CMP	$0, R10
	BLE	call
	MOVD	kret<>(SB), R0
	CMP	$0, R0
	BGT	take
	MOVD	ZR, kept<>(SB)
	MOVD	R19, R30
	RET
call:
	MOVD	$READ_INPUT, R8
	BL	hcall<>(SB)
	MOVD	R19, R30
	RET
take:
	CMP	R10, R0
	BLE	copy
	MOVD	R10, R0
copy:
	MOVD	$input<>(SB), R2
	MOVD	kat<>(SB), R3
	ADD	R3, R2
	MOVD	R0, R4
loop:
	MOVBU	(R2), R5
	MOVB	R5, (R9)
	ADD	$1, R2
	ADD	$1, R9
	SUBS	$1, R4
	BNE	loop
	ADD	R0, R3
	MOVD	R3, kat<>(SB)
	MOVD	kret<>(SB), R3
	SUB	R0, R3
	MOVD	R3, kret<>(SB)
	MOVD	ZR, kept<>(SB)
	CMP	$0, R3
	BLE	done
	MOVD	$1, R2
	MOVD	R2, kept<>(SB)
done:
	MOVD	R19, R30
	RET

// take appends R0 bytes of buf and a '|' to out (readahead_test.go's ahead_take)
TEXT take<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	$buf<>(SB), R2
	MOVD	$out<>(SB), R3
	MOVD	outat<>(SB), R4
	ADD	R4, R3
	MOVD	R0, R5
	CMP	$0, R5
	BLE	bar
loop:
	MOVBU	(R2), R6
	MOVB	R6, (R3)
	ADD	$1, R2
	ADD	$1, R3
	SUBS	$1, R5
	BNE	loop
bar:
	MOVD	$'|', R6
	MOVB	R6, (R3)
	ADD	$1, R3
	MOVD	$out<>(SB), R2
	SUB	R2, R3
	MOVD	R3, outat<>(SB)
	RET

// digit appends the digit '0'+R0 to out
TEXT digit<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	$out<>(SB), R3
	MOVD	outat<>(SB), R4
	ADD	R4, R3
	ADD	$'0', R0, R6
	MOVB	R6, (R3)
	ADD	$1, R4
	MOVD	R4, outat<>(SB)
	RET

// ahead is readahead_test.go's aheadStandIn: the runtime's buffer held to its rules call by
// call. R0 the exit code: 0, or which wait said no.
TEXT ahead<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	R30, R20
	MOVD	$-1, R9
	BL	wait<>(SB)
	CBZ	R0, fail1
	MOVD	$buf<>(SB), R9
	MOVD	$3, R10
	BL	read<>(SB)
	BL	take<>(SB)
	MOVD	$SUSPEND, R8
	BL	hcall<>(SB)
	MOVD	$0, R9
	BL	wait<>(SB)
	CBZ	R0, fail2
	MOVD	$buf<>(SB), R9
	MOVD	$250, R10
	BL	read<>(SB)
	BL	take<>(SB)
	MOVD	$-1, R9
	BL	wait<>(SB)
	CBZ	R0, fail3
	MOVD	$buf<>(SB), R9
	MOVD	$250, R10
	BL	read<>(SB)
	BL	take<>(SB)
	MOVD	$-1, R9
	BL	wait<>(SB)
	CBZ	R0, fail3
	MOVD	$buf<>(SB), R9
	MOVD	$250, R10
	BL	read<>(SB)
	BL	take<>(SB)
	MOVD	$0, R9
	BL	wait<>(SB)
	CBZ	R0, fail4
	MOVD	$buf<>(SB), R9
	MOVD	$250, R10
	BL	read<>(SB)
	BL	digit<>(SB)
	MOVD	$buf<>(SB), R9
	MOVD	$250, R10
	BL	read<>(SB)
	BL	digit<>(SB)
	MOVD	$WRITE, R8
	MOVD	$out<>(SB), R9
	MOVD	outat<>(SB), R10
	BL	hcall<>(SB)
	MOVD	$0, R0
	MOVD	R20, R30
	RET
fail1:
	MOVD	$1, R0
	MOVD	R20, R30
	RET
fail2:
	MOVD	$2, R0
	MOVD	R20, R30
	RET
fail3:
	MOVD	$3, R0
	MOVD	R20, R30
	RET
fail4:
	MOVD	$4, R0
	MOVD	R20, R30
	RET

// whim_vectors is VBAR_EL1's table: 16 entries of 128 bytes, 2 KiB aligned, each reporting its
// number, ESR_EL1, ELR_EL1 and FAR_EL1.
TEXT whim_vectors(SB),NOSPLIT|NOFRAME,$0
	PCALIGN	$2048
	MOVD	$0, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$1, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$2, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$3, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$4, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$5, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$6, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$7, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$8, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$9, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$10, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$11, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$12, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$13, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$14, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)
	PCALIGN	$128
	MOVD	$15, R9
	MRS	ESR_EL1, R10
	MRS	ELR_EL1, R11
	MRS	FAR_EL1, R12
	B	vector_common<>(SB)

// vector_common reports the exception by the call FAULT and never returns: the monitor ends the
// guest (rt.c's whim_fault).
TEXT vector_common<>(SB),NOSPLIT|NOFRAME,$0
	MOVD	$FAULT, R8
again:
	BL	hcall<>(SB)
	B	again
