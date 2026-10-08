#include "textflag.h"

TEXT ·asm(SB),NOSPLIT,$0-8
	MOVQ $3, ret+0(FP)
	RET
