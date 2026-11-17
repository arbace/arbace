#include "textflag.h"

TEXT ·f(SB), NOSPLIT, $0-8
	MOVD	R0
	ADD	$1, R1, R2, R3
	NOSUCH	R1, R2
	RET
