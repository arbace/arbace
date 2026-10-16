// The bare guest on amd64 (main.go): go-whim's guest/rt/entry_amd64.S and what of rt.c and the
// stand-ins the tests run, in Go's assembly. The monitor starts the vCPU in 64-bit mode at _start,
// RSP the stack's top, DI argc, SI argv and DX the heap; every vector runs on the TSS's IST1
// stack, so a fault in the stack's guard page is reported too.

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

// _start: argv[1]'s first byte chooses (main.go); the exit code from AX
TEXT _start(SB),NOSPLIT|NOFRAME,$0
	LEAQ	whim_vectors(SB), AX	// kept by the linker: the monitor's IDT names it
	CMPQ	DI, $2
	JLT	hello
	MOVQ	8(SI), AX
	MOVBQZX	0(AX), AX
	CMPQ	AX, $'n'
	JEQ	null
	CMPQ	AX, $'d'
	JEQ	deep
	CMPQ	AX, $'s'
	JEQ	spin
	CMPQ	AX, $'a'
	JEQ	ahead
hello:
	MOVQ	$WRITE, R8
	LEAQ	hello<>(SB), R9
	MOVQ	$6, R10
	CALL	hcall<>(SB)
	MOVQ	$3, AX
	JMP	exit
null:
	XORQ	AX, AX
	MOVL	$1, (AX)
	JMP	exit
deep:
	SUBQ	$4096, SP
	MOVB	$1, (SP)
	JMP	deep
spin:
	JMP	spin
ahead:
	CALL	ahead<>(SB)
exit:
	MOVQ	$EXIT, R8
	MOVQ	AX, R9
again:
	CALL	hcall<>(SB)	// the monitor never resumes an exit
	JMP	again

// hcall makes call R8 with R9-R12 its arguments (rt.c's whim_hcall): the block filled, its
// address stored to the doorbell from AX; AX the call's ret, BX its a[0]. DI and DX used.
TEXT hcall<>(SB),NOSPLIT|NOFRAME,$0
	LEAQ	callarea<>(SB), DI
	ADDQ	$63, DI
	ANDQ	$~63, DI
	MOVQ	R8, 0(DI)
	MOVQ	R9, 8(DI)
	MOVQ	R10, 16(DI)
	MOVQ	R11, 24(DI)
	MOVQ	R12, 32(DI)
	MOVQ	$0, 40(DI)
	MOVQ	$0, 48(DI)
	MOVQ	$0, 56(DI)
	MOVQ	DI, AX
	MOVQ	$0xf0000000, DX
	MOVQ	AX, (DX)
	MOVQ	48(DI), AX
	MOVQ	8(DI), BX
	RET

// wait is rt.c's musl_wait_for_input(R9 ms): 1 while a read's answer is kept; else one call,
// WAIT_READ, which reads ahead into input when there is input. AX the answer.
TEXT wait<>(SB),NOSPLIT|NOFRAME,$0
	MOVQ	kept<>(SB), AX
	CMPQ	AX, $0
	JNE	yes
	MOVQ	$WAIT_READ, R8
	LEAQ	input<>(SB), R10
	MOVQ	$INPUT_BYTES, R11
	CALL	hcall<>(SB)
	CMPQ	AX, $0
	JEQ	no
	MOVQ	$1, kept<>(SB)
	MOVQ	BX, kret<>(SB)
	MOVQ	$0, kat<>(SB)
yes:
	MOVQ	$1, AX
	RET
no:
	RET

// read is rt.c's musl_read_input(R9 buf, R10 len): served from what the wait read while any is
// kept -- the bytes, as many as len, or the read's 0 or -1 -- else a call of its own. AX the
// count.
TEXT read<>(SB),NOSPLIT|NOFRAME,$0
	MOVQ	kept<>(SB), AX
	CMPQ	AX, $0
	JEQ	call
	CMPQ	R10, $0
	JLE	call
	MOVQ	kret<>(SB), AX
	CMPQ	AX, $0
	JGT	take
	MOVQ	$0, kept<>(SB)
	RET
call:
	MOVQ	$READ_INPUT, R8
	CALL	hcall<>(SB)
	RET
take:
	CMPQ	AX, R10
	JLE	copy
	MOVQ	R10, AX
copy:
	LEAQ	input<>(SB), SI
	ADDQ	kat<>(SB), SI
	MOVQ	AX, CX
loop:
	MOVB	(SI), DX
	MOVB	DX, (R9)
	INCQ	SI
	INCQ	R9
	DECQ	CX
	JNZ	loop
	ADDQ	AX, kat<>(SB)
	SUBQ	AX, kret<>(SB)
	MOVQ	$0, kept<>(SB)
	CMPQ	kret<>(SB), $0
	JLE	done
	MOVQ	$1, kept<>(SB)
done:
	RET

// take appends AX bytes of buf and a '|' to out (readahead_test.go's ahead_take)
TEXT take<>(SB),NOSPLIT|NOFRAME,$0
	LEAQ	buf<>(SB), SI
	LEAQ	out<>(SB), DI
	ADDQ	outat<>(SB), DI
	MOVQ	AX, CX
	CMPQ	CX, $0
	JLE	bar
loop:
	MOVB	(SI), DX
	MOVB	DX, (DI)
	INCQ	SI
	INCQ	DI
	DECQ	CX
	JNZ	loop
bar:
	MOVB	$'|', (DI)
	INCQ	DI
	LEAQ	out<>(SB), SI
	SUBQ	SI, DI
	MOVQ	DI, outat<>(SB)
	RET

// digit appends the digit '0'+AX to out
TEXT digit<>(SB),NOSPLIT|NOFRAME,$0
	LEAQ	out<>(SB), DI
	ADDQ	outat<>(SB), DI
	ADDQ	$'0', AX
	MOVB	AX, (DI)
	INCQ	outat<>(SB)
	RET

// ahead is readahead_test.go's aheadStandIn: the runtime's buffer held to its rules call by
// call. AX the exit code: 0, or which wait said no.
TEXT ahead<>(SB),NOSPLIT|NOFRAME,$0
	MOVQ	$-1, R9
	CALL	wait<>(SB)
	CMPQ	AX, $0
	JEQ	fail1
	LEAQ	buf<>(SB), R9
	MOVQ	$3, R10
	CALL	read<>(SB)
	CALL	take<>(SB)
	MOVQ	$SUSPEND, R8
	CALL	hcall<>(SB)
	MOVQ	$0, R9
	CALL	wait<>(SB)
	CMPQ	AX, $0
	JEQ	fail2
	LEAQ	buf<>(SB), R9
	MOVQ	$250, R10
	CALL	read<>(SB)
	CALL	take<>(SB)
	MOVQ	$-1, R9
	CALL	wait<>(SB)
	CMPQ	AX, $0
	JEQ	fail3
	LEAQ	buf<>(SB), R9
	MOVQ	$250, R10
	CALL	read<>(SB)
	CALL	take<>(SB)
	MOVQ	$-1, R9
	CALL	wait<>(SB)
	CMPQ	AX, $0
	JEQ	fail3
	LEAQ	buf<>(SB), R9
	MOVQ	$250, R10
	CALL	read<>(SB)
	CALL	take<>(SB)
	MOVQ	$0, R9
	CALL	wait<>(SB)
	CMPQ	AX, $0
	JEQ	fail4
	LEAQ	buf<>(SB), R9
	MOVQ	$250, R10
	CALL	read<>(SB)
	CALL	digit<>(SB)
	LEAQ	buf<>(SB), R9
	MOVQ	$250, R10
	CALL	read<>(SB)
	CALL	digit<>(SB)
	MOVQ	$WRITE, R8
	LEAQ	out<>(SB), R9
	MOVQ	outat<>(SB), R10
	CALL	hcall<>(SB)
	XORQ	AX, AX
	RET
fail1:
	MOVQ	$1, AX
	RET
fail2:
	MOVQ	$2, AX
	RET
fail3:
	MOVQ	$3, AX
	RET
fail4:
	MOVQ	$4, AX
	RET

// whim_vectors is the monitor's IDT's 32 gates (vmm/setup_amd64.go), 16 bytes apart, every one on
// IST1: the vector, and an error code where the CPU pushes none, then the common report.
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

// vector_common reports the exception by the call FAULT -- the vector, the error code, RIP and
// CR2 -- and never returns: the monitor ends the guest (rt.c's whim_fault).
TEXT vector_common<>(SB),NOSPLIT|NOFRAME,$0
	POPQ	R9
	POPQ	R10
	MOVQ	(SP), R11
	MOVQ	CR2, R12
	MOVQ	$FAULT, R8
again:
	CALL	hcall<>(SB)
	JMP	again
