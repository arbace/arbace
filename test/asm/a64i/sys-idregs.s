# a64i's program (AR20, queue 291; ARS85 (a)): the ID registers Linux emulates for EL0, MRS of op0 3, op1 0, CRn 0, CRm 0-7, op2
# 0-7 (by their encodings), each printed, ID_AA64ISAR1_EL1 without API, GPI, BF16 and I8MM and ID_AA64PFR1_EL1 without BT (the
# features a64i does not implement: on this M2 they are set, in a64i 0); a register Linux does not emulate (CRm 0's op2 1-4 and 7,
# CRm 1) is SIGILL, its handler printing 0x51611 and stepping over it (those by .inst: the forms have no system register for them)
# with: lib/out.s
# exit: 0
# stdout: "00000000610f0000\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000051611\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0001000000110011\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000006\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0021100110212120\n"
# stdout: "0000001100211002\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000111ff000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000100000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
	.text
	.globl	_start
_start:
	mov	x0, #4
	adrp	x1, act
	add	x1, x1, :lo12:act
	mov	x2, #0
	mov	x3, #8
	mov	x8, #134			// rt_sigaction(SIGILL)
	svc	#0
	mov	x0, #0
	mrs	x0, s3_0_c0_c0_0
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380020			// mrs x0, s3_0_c0_c0_1: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380040			// mrs x0, s3_0_c0_c0_2: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380060			// mrs x0, s3_0_c0_c0_3: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380080			// mrs x0, s3_0_c0_c0_4: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c0_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c0_6
	bl	put_hex
	mov	x0, #0
	.inst	0xd53800e0			// mrs x0, s3_0_c0_c0_7: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380100			// mrs x0, s3_0_c0_c1_0: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380120			// mrs x0, s3_0_c0_c1_1: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380140			// mrs x0, s3_0_c0_c1_2: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380160			// mrs x0, s3_0_c0_c1_3: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd5380180			// mrs x0, s3_0_c0_c1_4: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd53801a0			// mrs x0, s3_0_c0_c1_5: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd53801c0			// mrs x0, s3_0_c0_c1_6: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	.inst	0xd53801e0			// mrs x0, s3_0_c0_c1_7: SIGILL (no assembler form: UNDEFINED at EL0)
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_0
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_1
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_2
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_3
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_4
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_6
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c2_7
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_0
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_1
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_2
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_3
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_4
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_6
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c3_7
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_0
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_1
	bic	x0, x0, #0xf
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_2
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_3
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_4
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_6
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c4_7
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_0
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_1
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_2
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_3
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_4
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_6
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c5_7
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_0
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_1
	movz	x1, #0x0f00
	movk	x1, #0xf000, lsl #16
	movk	x1, #0xf000, lsl #32
	movk	x1, #0x00f0, lsl #48
	bic	x0, x0, x1
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_2
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_3
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_4
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_6
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c6_7
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_0
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_1
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_2
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_3
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_4
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_5
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_6
	bl	put_hex
	mov	x0, #0
	mrs	x0, s3_0_c0_c7_7
	bl	put_hex
	b	exit0
handler:
	ldr	x0, [x2, #440]
	add	x0, x0, #4
	str	x0, [x2, #440]
	mov	x0, #0x1611
	movk	x0, #0x5, lsl #16
	str	x0, [x2, #184]			// x0 in the frame
	ret
restorer:
	mov	x8, #139
	svc	#0
	.data
	.p2align	3
act:	.quad	handler, 0x04000004, restorer, 0
