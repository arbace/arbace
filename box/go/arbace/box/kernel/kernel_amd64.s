#include "textflag.h"

// func symaddrs() (text, etext, nd, bss, end uintptr): the image's segment boundaries, from the
// linker's own symbols (doc/go/PROCESS-SPEC.md §2.1): text and its end, the writable segment's
// first symbol (noptrdata), bss's start (the end of the file-backed data) and the image's end.
TEXT ·symaddrs(SB),NOSPLIT,$0-40
	LEAQ	runtime·text(SB), AX
	MOVQ	AX, text+0(FP)
	LEAQ	runtime·etext(SB), AX
	MOVQ	AX, etext+8(FP)
	LEAQ	runtime·noptrdata(SB), AX
	MOVQ	AX, nd+16(FP)
	LEAQ	runtime·bss(SB), AX
	MOVQ	AX, bss+24(FP)
	LEAQ	runtime·end(SB), AX
	MOVQ	AX, end+32(FP)
	RET
