# a64i's program (AR18, queue 285; AR19, queue 288): AT_HWCAP and AT_HWCAP2 from the initial stack's auxiliary vector, each masked
# by what a64i claims (emu.a64Hwcap 0x2fb3f7fb: FP, ASIMD, AES, PMULL, SHA1, SHA2, CRC32, ATOMICS, FPHP, ASIMDHP, ASIMDRDM, JSCVT,
# FCMA, LRCPC, DCPOP, SHA3, ASIMDDP, SHA512, ASIMDFHM, DIT, USCAT, ILRCPC, FLAGM, SB; emu.a64Hwcap2 0x181: DCPODP, FLAGM2, FRINT):
# the masks themselves, in a64i and on the CPU, which reports every one of them
# with: lib/out.s
# exit: 0
# stdout: "000000002fb3f7fb\n"
# stdout: "0000000000000181\n"
	.text
	.globl	_start
_start:
	mov	x20, sp
	ldr	x1, [x20]
	add	x2, x20, #8
	add	x2, x2, x1, lsl #3
	add	x2, x2, #8
1:	ldr	x3, [x2], #8
	cbnz	x3, 1b
	mov	x21, #0
	mov	x22, #0
2:	ldp	x3, x4, [x2], #16
	cbz	x3, 4f
	cmp	x3, #16
	b.ne	3f
	mov	x21, x4
3:	cmp	x3, #26
	b.ne	2b
	mov	x22, x4
	b	2b
4:	movz	x5, #0xf7fb
	movk	x5, #0x2fb3, lsl #16
	and	x0, x21, x5
	bl	put_hex
	mov	x5, #0x181
	and	x0, x22, x5
	bl	put_hex
	b	exit0
