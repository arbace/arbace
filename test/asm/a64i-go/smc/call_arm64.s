// a64i's Go program smc (queue 303): the calls into code written at run time

#include "textflag.h"

// func call(addr, a0, a1 uintptr) uint64
TEXT ·call(SB), NOSPLIT, $16-32
	MOVD	addr+0(FP), R2
	MOVD	a0+8(FP), R0
	MOVD	a1+16(FP), R1
	CALL	(R2)
	MOVD	R0, ret+24(FP)
	RET

// func sync(addr, n uintptr): dc cvau over the lines, dsb ish, ic ivau over them, dsb ish, isb (64-byte lines: CTR_EL0)
TEXT ·sync(SB), NOSPLIT, $0-16
	MOVD	addr+0(FP), R0
	MOVD	n+8(FP), R1
	ADD	R0, R1, R1
	MOVD	R0, R2
dc:
	WORD	$0xd50b7b22 // dc cvau, x2
	ADD	$64, R2
	CMP	R1, R2
	BLO	dc
	WORD	$0xd5033b9f // dsb ish
	MOVD	R0, R2
ic:
	WORD	$0xd50b7522 // ic ivau, x2
	ADD	$64, R2
	CMP	R1, R2
	BLO	ic
	WORD	$0xd5033b9f // dsb ish
	WORD	$0xd5033fdf // isb
	RET
