# a64i's program (queue 283, ARS77 (a)): the M2's cryptographic extension against the standards' published test vectors, a
# second judge beside synth-neon-crypto's model: AES-128 (FIPS 197 C.1) encrypted by aese and aesmc and decrypted by aesd
# and aesimc (the decryption keys by aesimc), SHA-1 (sha1c, sha1p, sha1m, sha1h, sha1su0, sha1su1), SHA-256 (sha256h,
# sha256h2, sha256su0, sha256su1) and SHA-512 (sha512h, sha512h2, sha512su0, sha512su1) of "abc" (FIPS 180-4's examples),
# SHA3-256 of "abc" (FIPS 202) by Keccak-f[1600] on eor3, rax1, xar and bcax, and pmull and pmull2 1q against a shift-and-xor
# loop on general registers; self-checking (each result compared byte for byte with the vector, a difference prints FAIL).
# Written by .tmp/q283/scripts/gen_fips.py (queue 283's report): the constants (the round keys, K, H, rho, RC) computed
# from the standards' definitions.
# with: lib/out.s
# exit: 0
# stdout: "aes-128 encrypt (FIPS 197 C.1): ok\n"
# stdout: "aes-128 decrypt (FIPS 197 C.1): ok\n"
# stdout: "sha-1 abc (FIPS 180-4): ok\n"
# stdout: "sha-256 abc (FIPS 180-4): ok\n"
# stdout: "sha-512 abc (FIPS 180-4): ok\n"
# stdout: "sha3-256 abc (FIPS 202): ok\n"
# stdout: "pmull 1q, pmull2 1q against a shift-and-xor loop: ok\n"
	.text
	.globl	_start
_start:
	mov	x19, #0
// AES-128: state v0, round keys v1-v11 (rk0-rk10)
	adrp	x0, aes_rk
	add	x0, x0, :lo12:aes_rk
	ld1	{v1.16b, v2.16b, v3.16b, v4.16b}, [x0], #64
	ld1	{v5.16b, v6.16b, v7.16b, v8.16b}, [x0], #64
	ld1	{v9.16b, v10.16b, v11.16b}, [x0]
	adrp	x0, aes_pt
	add	x0, x0, :lo12:aes_pt
	ld1	{v0.16b}, [x0]
	aese	v0.16b, v1.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v2.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v3.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v4.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v5.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v6.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v7.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v8.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v9.16b
	aesmc	v0.16b, v0.16b
	aese	v0.16b, v10.16b
	eor	v0.16b, v0.16b, v11.16b
	adrp	x0, out
	add	x0, x0, :lo12:out
	st1	{v0.16b}, [x0]
	mov	x5, #0
	bl	verify
// the equivalent inverse cipher: dk = rk10, aesimc(rk9) .. aesimc(rk1), rk0
	aesimc	v2.16b, v2.16b
	aesimc	v3.16b, v3.16b
	aesimc	v4.16b, v4.16b
	aesimc	v5.16b, v5.16b
	aesimc	v6.16b, v6.16b
	aesimc	v7.16b, v7.16b
	aesimc	v8.16b, v8.16b
	aesimc	v9.16b, v9.16b
	aesimc	v10.16b, v10.16b
	adrp	x0, aes_ct
	add	x0, x0, :lo12:aes_ct
	ld1	{v0.16b}, [x0]
	aesd	v0.16b, v11.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v10.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v9.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v8.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v7.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v6.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v5.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v4.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v3.16b
	aesimc	v0.16b, v0.16b
	aesd	v0.16b, v2.16b
	eor	v0.16b, v0.16b, v1.16b
	adrp	x0, out
	add	x0, x0, :lo12:out
	st1	{v0.16b}, [x0]
	mov	x5, #1
	bl	verify
// SHA-1: abcd v0, e s1 (saved v2, v3), the schedule v4-v7, K v16-v19
	adrp	x0, sha1_h
	add	x0, x0, :lo12:sha1_h
	ld1	{v0.4s}, [x0], #16
	ldr	s1, [x0]
	adrp	x0, sha1_k
	add	x0, x0, :lo12:sha1_k
	ld1	{v16.4s, v17.4s, v18.4s, v19.4s}, [x0]
	adrp	x0, blk64
	add	x0, x0, :lo12:blk64
	ld1	{v4.16b, v5.16b, v6.16b, v7.16b}, [x0]
	rev32	v4.16b, v4.16b
	rev32	v5.16b, v5.16b
	rev32	v6.16b, v6.16b
	rev32	v7.16b, v7.16b
	mov	v2.16b, v0.16b
	mov	v3.16b, v1.16b
	add	v20.4s, v4.4s, v16.4s
	sha1h	s21, s0
	sha1c	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v4.4s, v5.4s, v6.4s
	sha1su1	v4.4s, v7.4s
	add	v20.4s, v5.4s, v16.4s
	sha1h	s21, s0
	sha1c	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v5.4s, v6.4s, v7.4s
	sha1su1	v5.4s, v4.4s
	add	v20.4s, v6.4s, v16.4s
	sha1h	s21, s0
	sha1c	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v6.4s, v7.4s, v4.4s
	sha1su1	v6.4s, v5.4s
	add	v20.4s, v7.4s, v16.4s
	sha1h	s21, s0
	sha1c	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v7.4s, v4.4s, v5.4s
	sha1su1	v7.4s, v6.4s
	add	v20.4s, v4.4s, v16.4s
	sha1h	s21, s0
	sha1c	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v4.4s, v5.4s, v6.4s
	sha1su1	v4.4s, v7.4s
	add	v20.4s, v5.4s, v17.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v5.4s, v6.4s, v7.4s
	sha1su1	v5.4s, v4.4s
	add	v20.4s, v6.4s, v17.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v6.4s, v7.4s, v4.4s
	sha1su1	v6.4s, v5.4s
	add	v20.4s, v7.4s, v17.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v7.4s, v4.4s, v5.4s
	sha1su1	v7.4s, v6.4s
	add	v20.4s, v4.4s, v17.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v4.4s, v5.4s, v6.4s
	sha1su1	v4.4s, v7.4s
	add	v20.4s, v5.4s, v17.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v5.4s, v6.4s, v7.4s
	sha1su1	v5.4s, v4.4s
	add	v20.4s, v6.4s, v18.4s
	sha1h	s21, s0
	sha1m	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v6.4s, v7.4s, v4.4s
	sha1su1	v6.4s, v5.4s
	add	v20.4s, v7.4s, v18.4s
	sha1h	s21, s0
	sha1m	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v7.4s, v4.4s, v5.4s
	sha1su1	v7.4s, v6.4s
	add	v20.4s, v4.4s, v18.4s
	sha1h	s21, s0
	sha1m	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v4.4s, v5.4s, v6.4s
	sha1su1	v4.4s, v7.4s
	add	v20.4s, v5.4s, v18.4s
	sha1h	s21, s0
	sha1m	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v5.4s, v6.4s, v7.4s
	sha1su1	v5.4s, v4.4s
	add	v20.4s, v6.4s, v18.4s
	sha1h	s21, s0
	sha1m	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v6.4s, v7.4s, v4.4s
	sha1su1	v6.4s, v5.4s
	add	v20.4s, v7.4s, v19.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	sha1su0	v7.4s, v4.4s, v5.4s
	sha1su1	v7.4s, v6.4s
	add	v20.4s, v4.4s, v19.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	add	v20.4s, v5.4s, v19.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	add	v20.4s, v6.4s, v19.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	add	v20.4s, v7.4s, v19.4s
	sha1h	s21, s0
	sha1p	q0, s1, v20.4s
	mov	v1.16b, v21.16b
	add	v0.4s, v0.4s, v2.4s
	add	v1.4s, v1.4s, v3.4s
	rev32	v0.16b, v0.16b
	rev32	v1.16b, v1.16b
	adrp	x0, out
	add	x0, x0, :lo12:out
	st1	{v0.16b}, [x0]
	str	s1, [x0, #16]
	mov	x5, #2
	bl	verify
// SHA-256: abcd v0, efgh v1 (saved v2, v3), the schedule v4-v7, K from x1
	adrp	x0, sha256_h
	add	x0, x0, :lo12:sha256_h
	ld1	{v0.4s, v1.4s}, [x0]
	adrp	x0, blk64
	add	x0, x0, :lo12:blk64
	ld1	{v4.16b, v5.16b, v6.16b, v7.16b}, [x0]
	rev32	v4.16b, v4.16b
	rev32	v5.16b, v5.16b
	rev32	v6.16b, v6.16b
	rev32	v7.16b, v7.16b
	mov	v2.16b, v0.16b
	mov	v3.16b, v1.16b
	adrp	x1, sha256_k
	add	x1, x1, :lo12:sha256_k
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v4.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v4.4s, v5.4s
	sha256su1	v4.4s, v6.4s, v7.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v5.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v5.4s, v6.4s
	sha256su1	v5.4s, v7.4s, v4.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v6.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v6.4s, v7.4s
	sha256su1	v6.4s, v4.4s, v5.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v7.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v7.4s, v4.4s
	sha256su1	v7.4s, v5.4s, v6.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v4.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v4.4s, v5.4s
	sha256su1	v4.4s, v6.4s, v7.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v5.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v5.4s, v6.4s
	sha256su1	v5.4s, v7.4s, v4.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v6.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v6.4s, v7.4s
	sha256su1	v6.4s, v4.4s, v5.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v7.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v7.4s, v4.4s
	sha256su1	v7.4s, v5.4s, v6.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v4.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v4.4s, v5.4s
	sha256su1	v4.4s, v6.4s, v7.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v5.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v5.4s, v6.4s
	sha256su1	v5.4s, v7.4s, v4.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v6.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v6.4s, v7.4s
	sha256su1	v6.4s, v4.4s, v5.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v7.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	sha256su0	v7.4s, v4.4s
	sha256su1	v7.4s, v5.4s, v6.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v4.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v5.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v6.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	ld1	{v16.4s}, [x1], #16
	add	v16.4s, v16.4s, v7.4s
	mov	v18.16b, v0.16b
	sha256h	q0, q1, v16.4s
	sha256h2	q1, q18, v16.4s
	add	v0.4s, v0.4s, v2.4s
	add	v1.4s, v1.4s, v3.4s
	rev32	v0.16b, v0.16b
	rev32	v1.16b, v1.16b
	adrp	x0, out
	add	x0, x0, :lo12:out
	st1	{v0.16b, v1.16b}, [x0]
	mov	x5, #3
	bl	verify
// SHA-512: the schedule W (80 doublewords) by sha512su0 and sha512su1, two rounds a step by sha512h and sha512h2 on the
// state a-h in x9-x16: sha512h's d [h + K(t) + W(t), g + K(t+1) + W(t+1)] (high, low), n [g, f], m [e, d]; sha512h2's d its
// result [T1, T1'], n [_, c], m [b, a]; e' = d + T1, e'' = c + T1'
	adrp	x0, blk128
	add	x0, x0, :lo12:blk128
	adrp	x1, w512
	add	x1, x1, :lo12:w512
	mov	x2, #16
1:	ldr	x3, [x0], #8
	rev	x3, x3
	str	x3, [x1], #8
	subs	x2, x2, #1
	b.ne	1b
	mov	x2, #32
1:	ldur	q20, [x1, #-128]
	ldur	q21, [x1, #-112]
	sha512su0	v20.2d, v21.2d
	ldur	q22, [x1, #-16]
	ldur	q23, [x1, #-56]
	sha512su1	v20.2d, v22.2d, v23.2d
	str	q20, [x1], #16
	subs	x2, x2, #1
	b.ne	1b
	adrp	x0, sha512_h
	add	x0, x0, :lo12:sha512_h
	ldp	x9, x10, [x0]
	ldp	x11, x12, [x0, #16]
	ldp	x13, x14, [x0, #32]
	ldp	x15, x16, [x0, #48]
	adrp	x20, w512
	add	x20, x20, :lo12:w512
	adrp	x21, sha512_k
	add	x21, x21, :lo12:sha512_k
	mov	x22, #40
1:	ldp	x0, x1, [x20], #16
	ldp	x2, x3, [x21], #16
	add	x0, x0, x2
	add	x0, x0, x16
	add	x1, x1, x3
	add	x1, x1, x15
	fmov	d24, x1
	mov	v24.d[1], x0
	fmov	d25, x14
	mov	v25.d[1], x15
	fmov	d26, x12
	mov	v26.d[1], x13
	sha512h	q24, q25, v26.2d
	fmov	x0, d24
	mov	x1, v24.d[1]
	add	x4, x12, x1
	add	x5, x11, x0
	fmov	d27, x11
	fmov	d28, x9
	mov	v28.d[1], x10
	sha512h2	q24, q27, v28.2d
	fmov	x2, d24
	mov	x3, v24.d[1]
	mov	x16, x14
	mov	x15, x13
	mov	x14, x4
	mov	x13, x5
	mov	x12, x10
	mov	x11, x9
	mov	x10, x3
	mov	x9, x2
	subs	x22, x22, #1
	b.ne	1b
	adrp	x0, sha512_h
	add	x0, x0, :lo12:sha512_h
	adrp	x1, out
	add	x1, x1, :lo12:out
	ldr	x2, [x0, #0]
	add	x9, x9, x2
	rev	x9, x9
	str	x9, [x1, #0]
	ldr	x2, [x0, #8]
	add	x10, x10, x2
	rev	x10, x10
	str	x10, [x1, #8]
	ldr	x2, [x0, #16]
	add	x11, x11, x2
	rev	x11, x11
	str	x11, [x1, #16]
	ldr	x2, [x0, #24]
	add	x12, x12, x2
	rev	x12, x12
	str	x12, [x1, #24]
	ldr	x2, [x0, #32]
	add	x13, x13, x2
	rev	x13, x13
	str	x13, [x1, #32]
	ldr	x2, [x0, #40]
	add	x14, x14, x2
	rev	x14, x14
	str	x14, [x1, #40]
	ldr	x2, [x0, #48]
	add	x15, x15, x2
	rev	x15, x15
	str	x15, [x1, #48]
	ldr	x2, [x0, #56]
	add	x16, x16, x2
	rev	x16, x16
	str	x16, [x1, #56]
	mov	x0, x1
	mov	x5, #4
	bl	verify
// SHA3-256 of "abc": the state's lanes A[x + 5y] in v0-v24 (d lane 0), its one padded block absorbed into the zero state,
// Keccak-f[1600]'s 24 rounds: C in v25-v29 by eor3, D by rax1, rho and pi by xar into B (memory, keccak_b), chi by bcax, iota by eor
	adrp	x0, blk136
	add	x0, x0, :lo12:blk136
	ldr	d0, [x0, #0]
	ldr	d1, [x0, #8]
	ldr	d2, [x0, #16]
	ldr	d3, [x0, #24]
	ldr	d4, [x0, #32]
	ldr	d5, [x0, #40]
	ldr	d6, [x0, #48]
	ldr	d7, [x0, #56]
	ldr	d8, [x0, #64]
	ldr	d9, [x0, #72]
	ldr	d10, [x0, #80]
	ldr	d11, [x0, #88]
	ldr	d12, [x0, #96]
	ldr	d13, [x0, #104]
	ldr	d14, [x0, #112]
	ldr	d15, [x0, #120]
	ldr	d16, [x0, #128]
	ldr	d17, [x0, #136]
	ldr	d18, [x0, #144]
	ldr	d19, [x0, #152]
	ldr	d20, [x0, #160]
	ldr	d21, [x0, #168]
	ldr	d22, [x0, #176]
	ldr	d23, [x0, #184]
	ldr	d24, [x0, #192]
	adrp	x21, keccak_rc
	add	x21, x21, :lo12:keccak_rc
	adrp	x22, keccak_b
	add	x22, x22, :lo12:keccak_b
	mov	x23, #24
1:
	eor3	v25.16b, v0.16b, v5.16b, v10.16b
	eor3	v25.16b, v25.16b, v15.16b, v20.16b
	eor3	v26.16b, v1.16b, v6.16b, v11.16b
	eor3	v26.16b, v26.16b, v16.16b, v21.16b
	eor3	v27.16b, v2.16b, v7.16b, v12.16b
	eor3	v27.16b, v27.16b, v17.16b, v22.16b
	eor3	v28.16b, v3.16b, v8.16b, v13.16b
	eor3	v28.16b, v28.16b, v18.16b, v23.16b
	eor3	v29.16b, v4.16b, v9.16b, v14.16b
	eor3	v29.16b, v29.16b, v19.16b, v24.16b
	rax1	v30.2d, v29.2d, v26.2d
	xar	v31.2d, v0.2d, v30.2d, #0
	str	q31, [x22, #0]
	xar	v31.2d, v5.2d, v30.2d, #28
	str	q31, [x22, #256]
	xar	v31.2d, v10.2d, v30.2d, #61
	str	q31, [x22, #112]
	xar	v31.2d, v15.2d, v30.2d, #23
	str	q31, [x22, #368]
	xar	v31.2d, v20.2d, v30.2d, #46
	str	q31, [x22, #224]
	rax1	v30.2d, v25.2d, v27.2d
	xar	v31.2d, v1.2d, v30.2d, #63
	str	q31, [x22, #160]
	xar	v31.2d, v6.2d, v30.2d, #20
	str	q31, [x22, #16]
	xar	v31.2d, v11.2d, v30.2d, #54
	str	q31, [x22, #272]
	xar	v31.2d, v16.2d, v30.2d, #19
	str	q31, [x22, #128]
	xar	v31.2d, v21.2d, v30.2d, #62
	str	q31, [x22, #384]
	rax1	v30.2d, v26.2d, v28.2d
	xar	v31.2d, v2.2d, v30.2d, #2
	str	q31, [x22, #320]
	xar	v31.2d, v7.2d, v30.2d, #58
	str	q31, [x22, #176]
	xar	v31.2d, v12.2d, v30.2d, #21
	str	q31, [x22, #32]
	xar	v31.2d, v17.2d, v30.2d, #49
	str	q31, [x22, #288]
	xar	v31.2d, v22.2d, v30.2d, #3
	str	q31, [x22, #144]
	rax1	v30.2d, v27.2d, v29.2d
	xar	v31.2d, v3.2d, v30.2d, #36
	str	q31, [x22, #80]
	xar	v31.2d, v8.2d, v30.2d, #9
	str	q31, [x22, #336]
	xar	v31.2d, v13.2d, v30.2d, #39
	str	q31, [x22, #192]
	xar	v31.2d, v18.2d, v30.2d, #43
	str	q31, [x22, #48]
	xar	v31.2d, v23.2d, v30.2d, #8
	str	q31, [x22, #304]
	rax1	v30.2d, v28.2d, v25.2d
	xar	v31.2d, v4.2d, v30.2d, #37
	str	q31, [x22, #240]
	xar	v31.2d, v9.2d, v30.2d, #44
	str	q31, [x22, #96]
	xar	v31.2d, v14.2d, v30.2d, #25
	str	q31, [x22, #352]
	xar	v31.2d, v19.2d, v30.2d, #56
	str	q31, [x22, #208]
	xar	v31.2d, v24.2d, v30.2d, #50
	str	q31, [x22, #64]
	ldp	q25, q26, [x22, #0]
	ldp	q27, q28, [x22, #32]
	ldr	q29, [x22, #64]
	bcax	v0.16b, v25.16b, v27.16b, v26.16b
	bcax	v1.16b, v26.16b, v28.16b, v27.16b
	bcax	v2.16b, v27.16b, v29.16b, v28.16b
	bcax	v3.16b, v28.16b, v25.16b, v29.16b
	bcax	v4.16b, v29.16b, v26.16b, v25.16b
	ldp	q25, q26, [x22, #80]
	ldp	q27, q28, [x22, #112]
	ldr	q29, [x22, #144]
	bcax	v5.16b, v25.16b, v27.16b, v26.16b
	bcax	v6.16b, v26.16b, v28.16b, v27.16b
	bcax	v7.16b, v27.16b, v29.16b, v28.16b
	bcax	v8.16b, v28.16b, v25.16b, v29.16b
	bcax	v9.16b, v29.16b, v26.16b, v25.16b
	ldp	q25, q26, [x22, #160]
	ldp	q27, q28, [x22, #192]
	ldr	q29, [x22, #224]
	bcax	v10.16b, v25.16b, v27.16b, v26.16b
	bcax	v11.16b, v26.16b, v28.16b, v27.16b
	bcax	v12.16b, v27.16b, v29.16b, v28.16b
	bcax	v13.16b, v28.16b, v25.16b, v29.16b
	bcax	v14.16b, v29.16b, v26.16b, v25.16b
	ldp	q25, q26, [x22, #240]
	ldp	q27, q28, [x22, #272]
	ldr	q29, [x22, #304]
	bcax	v15.16b, v25.16b, v27.16b, v26.16b
	bcax	v16.16b, v26.16b, v28.16b, v27.16b
	bcax	v17.16b, v27.16b, v29.16b, v28.16b
	bcax	v18.16b, v28.16b, v25.16b, v29.16b
	bcax	v19.16b, v29.16b, v26.16b, v25.16b
	ldp	q25, q26, [x22, #320]
	ldp	q27, q28, [x22, #352]
	ldr	q29, [x22, #384]
	bcax	v20.16b, v25.16b, v27.16b, v26.16b
	bcax	v21.16b, v26.16b, v28.16b, v27.16b
	bcax	v22.16b, v27.16b, v29.16b, v28.16b
	bcax	v23.16b, v28.16b, v25.16b, v29.16b
	bcax	v24.16b, v29.16b, v26.16b, v25.16b
	ldr	d30, [x21], #8
	eor	v0.16b, v0.16b, v30.16b
	subs	x23, x23, #1
	b.ne	1b
	adrp	x0, out
	add	x0, x0, :lo12:out
	stp	d0, d1, [x0]
	stp	d2, d3, [x0, #16]
	mov	x5, #5
	bl	verify
// pmull and pmull2 1q of each pair (x, y) of pm_pairs against a shift-and-xor loop on general registers
	adrp	x20, pm_pairs
	add	x20, x20, :lo12:pm_pairs
	mov	x22, #6
	mov	x24, #0
1:	ldp	q1, q2, [x20], #32
	pmull	v0.1q, v1.1d, v2.1d
	pmull2	v3.1q, v1.2d, v2.2d
	fmov	x0, d1
	fmov	x1, d2
	bl	clmul
	fmov	x2, d0
	mov	x3, v0.d[1]
	cmp	x2, x6
	ccmp	x3, x7, #0, eq
	cinc	x24, x24, ne
	mov	x0, v1.d[1]
	mov	x1, v2.d[1]
	bl	clmul
	fmov	x2, d3
	mov	x3, v3.d[1]
	cmp	x2, x6
	ccmp	x3, x7, #0, eq
	cinc	x24, x24, ne
	subs	x22, x22, #1
	b.ne	1b
	adrp	x0, msg6
	add	x0, x0, :lo12:msg6
	mov	x1, #53
	cbz	x24, 2f
	bl	fail
	b	3f
2:	bl	put_str
3:	mov	x0, x19
	b	exit_x0

// clmul: x6 (low) and x7 (high) the carry-less product of x0 and x1
clmul:
	mov	x6, #0
	mov	x7, #0
	mov	x2, #0
1:	lsr	x3, x1, x2
	tbz	x3, #0, 2f
	lsl	x3, x0, x2
	eor	x6, x6, x3
	cbz	x2, 2f
	mov	x4, #64
	sub	x4, x4, x2
	lsr	x3, x0, x4
	eor	x7, x7, x3
2:	add	x2, x2, #1
	cmp	x2, #64
	b.ne	1b
	ret

// verify: the x5th check: its length's bytes at x0 against its vector; its message, or FAIL and the message (x19 set)
verify:
	stp	x29, x30, [sp, #-16]!
	adrp	x1, checks
	add	x1, x1, :lo12:checks
	add	x1, x1, x5, lsl #5
	ldp	x2, x3, [x1]
	ldp	x6, x7, [x1, #16]
1:	ldrb	w8, [x0], #1
	ldrb	w9, [x2], #1
	cmp	w8, w9
	b.ne	2f
	subs	x3, x3, #1
	b.ne	1b
	mov	x0, x6
	mov	x1, x7
	bl	put_str
	ldp	x29, x30, [sp], #16
	ret
2:	mov	x0, x6
	mov	x1, x7
	bl	fail
	ldp	x29, x30, [sp], #16
	ret

// fail: FAIL, then the message at x0 (x1 bytes); x19 = 1
fail:
	stp	x29, x30, [sp, #-16]!
	stp	x0, x1, [sp, #-16]!
	adrp	x0, failmsg
	add	x0, x0, :lo12:failmsg
	mov	x1, #5
	bl	put_str
	ldp	x0, x1, [sp], #16
	bl	put_str
	mov	x19, #1
	ldp	x29, x30, [sp], #16
	ret

	.data
	.p2align	4
aes_rk:
	.byte	0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf
	.byte	0xd6, 0xaa, 0x74, 0xfd, 0xd2, 0xaf, 0x72, 0xfa, 0xda, 0xa6, 0x78, 0xf1, 0xd6, 0xab, 0x76, 0xfe
	.byte	0xb6, 0x92, 0xcf, 0xb, 0x64, 0x3d, 0xbd, 0xf1, 0xbe, 0x9b, 0xc5, 0x0, 0x68, 0x30, 0xb3, 0xfe
	.byte	0xb6, 0xff, 0x74, 0x4e, 0xd2, 0xc2, 0xc9, 0xbf, 0x6c, 0x59, 0xc, 0xbf, 0x4, 0x69, 0xbf, 0x41
	.byte	0x47, 0xf7, 0xf7, 0xbc, 0x95, 0x35, 0x3e, 0x3, 0xf9, 0x6c, 0x32, 0xbc, 0xfd, 0x5, 0x8d, 0xfd
	.byte	0x3c, 0xaa, 0xa3, 0xe8, 0xa9, 0x9f, 0x9d, 0xeb, 0x50, 0xf3, 0xaf, 0x57, 0xad, 0xf6, 0x22, 0xaa
	.byte	0x5e, 0x39, 0xf, 0x7d, 0xf7, 0xa6, 0x92, 0x96, 0xa7, 0x55, 0x3d, 0xc1, 0xa, 0xa3, 0x1f, 0x6b
	.byte	0x14, 0xf9, 0x70, 0x1a, 0xe3, 0x5f, 0xe2, 0x8c, 0x44, 0xa, 0xdf, 0x4d, 0x4e, 0xa9, 0xc0, 0x26
	.byte	0x47, 0x43, 0x87, 0x35, 0xa4, 0x1c, 0x65, 0xb9, 0xe0, 0x16, 0xba, 0xf4, 0xae, 0xbf, 0x7a, 0xd2
	.byte	0x54, 0x99, 0x32, 0xd1, 0xf0, 0x85, 0x57, 0x68, 0x10, 0x93, 0xed, 0x9c, 0xbe, 0x2c, 0x97, 0x4e
	.byte	0x13, 0x11, 0x1d, 0x7f, 0xe3, 0x94, 0x4a, 0x17, 0xf3, 0x7, 0xa7, 0x8b, 0x4d, 0x2b, 0x30, 0xc5
aes_pt:
	.byte	0x0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff
aes_ct:
	.byte	0x69, 0xc4, 0xe0, 0xd8, 0x6a, 0x7b, 0x4, 0x30, 0xd8, 0xcd, 0xb7, 0x80, 0x70, 0xb4, 0xc5, 0x5a
sha1_h:
	.word	0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476
	.word	0xc3d2e1f0
	.p2align	4
sha1_k:
	.word	0x5a827999, 0x5a827999, 0x5a827999, 0x5a827999
	.word	0x6ed9eba1, 0x6ed9eba1, 0x6ed9eba1, 0x6ed9eba1
	.word	0x8f1bbcdc, 0x8f1bbcdc, 0x8f1bbcdc, 0x8f1bbcdc
	.word	0xca62c1d6, 0xca62c1d6, 0xca62c1d6, 0xca62c1d6
sha256_h:
	.word	0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a
	.word	0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
sha256_k:
	.word	0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5
	.word	0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5
	.word	0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3
	.word	0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174
	.word	0xe49b69c1, 0xefbe4786, 0xfc19dc6, 0x240ca1cc
	.word	0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da
	.word	0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7
	.word	0xc6e00bf3, 0xd5a79147, 0x6ca6351, 0x14292967
	.word	0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13
	.word	0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85
	.word	0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3
	.word	0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070
	.word	0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5
	.word	0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3
	.word	0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208
	.word	0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
sha512_h:
	.xword	0x6a09e667f3bcc908, 0xbb67ae8584caa73b, 0x3c6ef372fe94f82b, 0xa54ff53a5f1d36f1
	.xword	0x510e527fade682d1, 0x9b05688c2b3e6c1f, 0x1f83d9abfb41bd6b, 0x5be0cd19137e2179
sha512_k:
	.xword	0x428a2f98d728ae22, 0x7137449123ef65cd, 0xb5c0fbcfec4d3b2f, 0xe9b5dba58189dbbc
	.xword	0x3956c25bf348b538, 0x59f111f1b605d019, 0x923f82a4af194f9b, 0xab1c5ed5da6d8118
	.xword	0xd807aa98a3030242, 0x12835b0145706fbe, 0x243185be4ee4b28c, 0x550c7dc3d5ffb4e2
	.xword	0x72be5d74f27b896f, 0x80deb1fe3b1696b1, 0x9bdc06a725c71235, 0xc19bf174cf692694
	.xword	0xe49b69c19ef14ad2, 0xefbe4786384f25e3, 0xfc19dc68b8cd5b5, 0x240ca1cc77ac9c65
	.xword	0x2de92c6f592b0275, 0x4a7484aa6ea6e483, 0x5cb0a9dcbd41fbd4, 0x76f988da831153b5
	.xword	0x983e5152ee66dfab, 0xa831c66d2db43210, 0xb00327c898fb213f, 0xbf597fc7beef0ee4
	.xword	0xc6e00bf33da88fc2, 0xd5a79147930aa725, 0x6ca6351e003826f, 0x142929670a0e6e70
	.xword	0x27b70a8546d22ffc, 0x2e1b21385c26c926, 0x4d2c6dfc5ac42aed, 0x53380d139d95b3df
	.xword	0x650a73548baf63de, 0x766a0abb3c77b2a8, 0x81c2c92e47edaee6, 0x92722c851482353b
	.xword	0xa2bfe8a14cf10364, 0xa81a664bbc423001, 0xc24b8b70d0f89791, 0xc76c51a30654be30
	.xword	0xd192e819d6ef5218, 0xd69906245565a910, 0xf40e35855771202a, 0x106aa07032bbd1b8
	.xword	0x19a4c116b8d2d0c8, 0x1e376c085141ab53, 0x2748774cdf8eeb99, 0x34b0bcb5e19b48a8
	.xword	0x391c0cb3c5c95a63, 0x4ed8aa4ae3418acb, 0x5b9cca4f7763e373, 0x682e6ff3d6b2b8a3
	.xword	0x748f82ee5defb2fc, 0x78a5636f43172f60, 0x84c87814a1f0ab72, 0x8cc702081a6439ec
	.xword	0x90befffa23631e28, 0xa4506cebde82bde9, 0xbef9a3f7b2c67915, 0xc67178f2e372532b
	.xword	0xca273eceea26619c, 0xd186b8c721c0c207, 0xeada7dd6cde0eb1e, 0xf57d4f7fee6ed178
	.xword	0x6f067aa72176fba, 0xa637dc5a2c898a6, 0x113f9804bef90dae, 0x1b710b35131c471b
	.xword	0x28db77f523047d84, 0x32caab7b40c72493, 0x3c9ebe0a15c9bebc, 0x431d67c49c100d4c
	.xword	0x4cc5d4becb3e42b6, 0x597f299cfc657e2a, 0x5fcb6fab3ad6faec, 0x6c44198c4a475817
keccak_rc:
	.xword	0x1, 0x8082, 0x800000000000808a, 0x8000000080008000
	.xword	0x808b, 0x80000001, 0x8000000080008081, 0x8000000000008009
	.xword	0x8a, 0x88, 0x80008009, 0x8000000a
	.xword	0x8000808b, 0x800000000000008b, 0x8000000000008089, 0x8000000000008003
	.xword	0x8000000000008002, 0x8000000000000080, 0x800a, 0x800000008000000a
	.xword	0x8000000080008081, 0x8000000000008080, 0x80000001, 0x8000000080008008
blk64:
	.byte	0x61, 0x62, 0x63, 0x80, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x18
blk128:
	.byte	0x61, 0x62, 0x63, 0x80, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x18
blk136:
	.byte	0x61, 0x62, 0x63, 0x6, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x80, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
	.byte	0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
sha1_exp:
	.byte	0xa9, 0x99, 0x3e, 0x36, 0x47, 0x6, 0x81, 0x6a, 0xba, 0x3e, 0x25, 0x71, 0x78, 0x50, 0xc2, 0x6c
	.byte	0x9c, 0xd0, 0xd8, 0x9d
	.p2align	4
sha256_exp:
	.byte	0xba, 0x78, 0x16, 0xbf, 0x8f, 0x1, 0xcf, 0xea, 0x41, 0x41, 0x40, 0xde, 0x5d, 0xae, 0x22, 0x23
	.byte	0xb0, 0x3, 0x61, 0xa3, 0x96, 0x17, 0x7a, 0x9c, 0xb4, 0x10, 0xff, 0x61, 0xf2, 0x0, 0x15, 0xad
sha512_exp:
	.byte	0xdd, 0xaf, 0x35, 0xa1, 0x93, 0x61, 0x7a, 0xba, 0xcc, 0x41, 0x73, 0x49, 0xae, 0x20, 0x41, 0x31
	.byte	0x12, 0xe6, 0xfa, 0x4e, 0x89, 0xa9, 0x7e, 0xa2, 0xa, 0x9e, 0xee, 0xe6, 0x4b, 0x55, 0xd3, 0x9a
	.byte	0x21, 0x92, 0x99, 0x2a, 0x27, 0x4f, 0xc1, 0xa8, 0x36, 0xba, 0x3c, 0x23, 0xa3, 0xfe, 0xeb, 0xbd
	.byte	0x45, 0x4d, 0x44, 0x23, 0x64, 0x3c, 0xe8, 0xe, 0x2a, 0x9a, 0xc9, 0x4f, 0xa5, 0x4c, 0xa4, 0x9f
sha3_exp:
	.byte	0x3a, 0x98, 0x5d, 0xa7, 0x4f, 0xe2, 0x25, 0xb2, 0x4, 0x5c, 0x17, 0x2d, 0x6b, 0xd3, 0x90, 0xbd
	.byte	0x85, 0x5f, 0x8, 0x6e, 0x3e, 0x9d, 0x52, 0x5b, 0x46, 0xbf, 0xe2, 0x45, 0x11, 0x43, 0x15, 0x32
pm_pairs:
	.xword	0x8000000000000001, 0x123456789abcdef, 0xffffffffffffffff, 0xfedcba9876543210
	.xword	0xffffffffffffffff, 0x1, 0xffffffffffffffff, 0x8000000000000000
	.xword	0x87, 0x0, 0xc200000000000000, 0x5555555555555555
	.xword	0xd5a3c4b2e1f00718, 0x13579bdf02468ace, 0x6b2f9e01c3d48a75, 0x2468ace013579bdf
	.xword	0x8000000000000000, 0xffffffff, 0x8000000000000000, 0xffffffff00000000
	.xword	0xf0f0f0f0f0f0f0f, 0x3, 0xf0f0f0f0f0f0f0f0, 0x3
checks:
	.xword	aes_ct, 16, msg0, 35
	.xword	aes_pt, 16, msg1, 35
	.xword	sha1_exp, 20, msg2, 27
	.xword	sha256_exp, 32, msg3, 29
	.xword	sha512_exp, 64, msg4, 29
	.xword	sha3_exp, 32, msg5, 28
msg0:	.ascii	"aes-128 encrypt (FIPS 197 C.1): ok\n"
msg1:	.ascii	"aes-128 decrypt (FIPS 197 C.1): ok\n"
msg2:	.ascii	"sha-1 abc (FIPS 180-4): ok\n"
msg3:	.ascii	"sha-256 abc (FIPS 180-4): ok\n"
msg4:	.ascii	"sha-512 abc (FIPS 180-4): ok\n"
msg5:	.ascii	"sha3-256 abc (FIPS 202): ok\n"
msg6:	.ascii	"pmull 1q, pmull2 1q against a shift-and-xor loop: ok\n"
failmsg:	.ascii	"FAIL "

	.bss
	.p2align	4
out:	.zero	64
w512:	.zero	640
keccak_b:	.zero	400
