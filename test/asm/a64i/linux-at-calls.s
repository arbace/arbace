# a64i's program (queue 314, 309's DECISION (a)): os.Root's remaining *at calls and pselect6 as Linux 7.3 answers them. umask(022)
# (arm64 166); mkdirat at AT_FDCWD (0), then at the directory's descriptor d (0, again EEXIST), at its O_PATH descriptor (x/y with
# 07777: mode 041755, S_ISVTX kept), at a file's ENOTDIR, at 99 EBADF, the empty name ENOENT; under umask(0) mode 040777; fchmodat
# (53, no flags) of the link: the file's mode 0100600; fchmodat2 (452) of the link with AT_SYMLINK_NOFOLLOW EOPNOTSUPP, of the file
# 0100640, a bad flag's EINVAL before the bad descriptor's, AT_EMPTY_PATH on the file's descriptor 0100604; fchownat (54) with ids -1:
# the link's own 0, a bad flag EINVAL, 99 EBADF; utimensat (88): the file's times {1000.000000005, 2000.999999999}, the link's own
# with AT_SYMLINK_NOFOLLOW (its mtime 7000, the file's 2000), both UTIME_OMIT 0 for a missing name with a bad flag, a tv_nsec of 1e9
# EINVAL for the file and ENOENT for a missing name (the lookup first), a NULL name at the file's descriptor (futimens) its mtime
# 11000, with AT_SYMLINK_NOFOLLOW EINVAL; pselect6 (72): a read and an O_PATH descriptor in the read set, the read one in the write
# and except sets, a zero timeout: 3, the sets as wanted (each shown xored with the wanted: 0, 0; the except set 0), an unopened 63
# EBADF, nfds -1 EINVAL, a tv_nsec of 1e9 EINVAL, a signal mask of size 4 EINVAL, of size 8 1; then the names removed
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffef\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000043ed\n"
# stdout: "ffffffffffffffec\n"
# stdout: "fffffffffffffff7\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000041ff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000008180\n"
# stdout: "ffffffffffffffa1\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000081a0\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000008184\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffea\n"
# stdout: "fffffffffffffff7\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000003e8\n"
# stdout: "0000000000000005\n"
# stdout: "00000000000007d0\n"
# stdout: "000000003b9ac9ff\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000001b58\n"
# stdout: "00000000000007d0\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffea\n"
# stdout: "fffffffffffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000002af8\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000000003\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "fffffffffffffff7\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000000001\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	sub	sp, sp, #512
	// a clean start: the names removed, their results not shown
	bl	clean
	// umask(022), its old value not shown (the runner's)
	mov	x0, #022
	mov	x8, #166
	svc	#0
	// mkdirat(AT_FDCWD, dir, 0755); d = openat(dir, O_DIRECTORY), dp = openat(dir, O_PATH|O_DIRECTORY)
	mov	x0, #-100
	adr	x1, ndir
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, ndir
	mov	x2, #0x4000
	mov	x8, #56
	svc	#0
	mov	x19, x0
	mov	x0, #-100
	adr	x1, ndir
	mov	x2, #0x4000
	movk	x2, #0x20, lsl #16
	mov	x8, #56
	svc	#0
	mov	x20, x0
	// the file f in d (openat(d, f, O_WRONLY|O_CREAT|O_TRUNC, 0644)), the link l -> f (symlinkat)
	mov	x0, x19
	adr	x1, nf
	mov	x2, #0x241
	mov	x3, #0644
	mov	x8, #56
	svc	#0
	mov	x21, x0
	adr	x0, nf
	mov	x1, x19
	adr	x2, nl
	mov	x8, #36
	svc	#0
	// mkdirat(d, x, 0755), again
	mov	x0, x19
	adr	x1, nx
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	mov	x0, x19
	adr	x1, nx
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	// mkdirat(dp, x/y, 07777): its mode 041755 (S_ISVTX kept, umask 022)
	mov	x0, x20
	adr	x1, nxy
	mov	x2, #07777
	mov	x8, #34
	svc	#0
	bl	put_hex
	adr	x1, nxy
	bl	mode
	// mkdirat(file, y) ENOTDIR, mkdirat(99, y) EBADF, mkdirat(d, "") ENOENT
	mov	x0, x21
	adr	x1, ny
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	mov	x0, #99
	adr	x1, ny
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	mov	x0, x19
	adr	x1, empty
	mov	x2, #0755
	mov	x8, #34
	svc	#0
	bl	put_hex
	// umask(0), mkdirat(d, z, 0777): its mode 040777; umask(022)
	mov	x0, #0
	mov	x8, #166
	svc	#0
	mov	x0, x19
	adr	x1, nz
	mov	x2, #0777
	mov	x8, #34
	svc	#0
	bl	put_hex
	adr	x1, nz
	bl	mode
	mov	x0, #022
	mov	x8, #166
	svc	#0
	// fchmodat(d, l, 0600): the link followed, f's mode 0100600
	mov	x0, x19
	adr	x1, nl
	mov	x2, #0600
	mov	x8, #53
	svc	#0
	bl	put_hex
	adr	x1, nf
	bl	mode
	// fchmodat2(d, l, 0644, AT_SYMLINK_NOFOLLOW) EOPNOTSUPP; fchmodat2(d, f, 0640, AT_SYMLINK_NOFOLLOW): 0100640
	mov	x0, x19
	adr	x1, nl
	mov	x2, #0644
	mov	x3, #0x100
	mov	x8, #452
	svc	#0
	bl	put_hex
	mov	x0, x19
	adr	x1, nf
	mov	x2, #0640
	mov	x3, #0x100
	mov	x8, #452
	svc	#0
	bl	put_hex
	adr	x1, nf
	bl	mode
	// fchmodat2(99, f, 0644, 0x200): the bad flag's EINVAL first
	mov	x0, #99
	adr	x1, nf
	mov	x2, #0644
	mov	x3, #0x200
	mov	x8, #452
	svc	#0
	bl	put_hex
	// fchmodat2(file, "", 0604, AT_EMPTY_PATH): 0100604
	mov	x0, x21
	adr	x1, empty
	mov	x2, #0604
	mov	x3, #0x1000
	mov	x8, #452
	svc	#0
	bl	put_hex
	adr	x1, nf
	bl	mode
	// fchownat(d, l, -1, -1, AT_SYMLINK_NOFOLLOW) 0, (d, f, -1, -1, 0x200) EINVAL, (99, f, -1, -1, 0) EBADF
	mov	x0, x19
	adr	x1, nl
	mov	x2, #-1
	mov	x3, #-1
	mov	x4, #0x100
	mov	x8, #54
	svc	#0
	bl	put_hex
	mov	x0, x19
	adr	x1, nf
	mov	x2, #-1
	mov	x3, #-1
	mov	x4, #0x200
	mov	x8, #54
	svc	#0
	bl	put_hex
	mov	x0, #99
	adr	x1, nf
	mov	x2, #-1
	mov	x3, #-1
	mov	x4, #0
	mov	x8, #54
	svc	#0
	bl	put_hex
	// utimensat(d, f, {1000.5, 2000.999999999}, 0): f's times
	add	x2, sp, #256
	mov	x0, #1000
	mov	x1, #5
	stp	x0, x1, [x2]
	mov	x0, #2000
	mov	x1, #0xc9ff
	movk	x1, #0x3b9a, lsl #16
	stp	x0, x1, [x2, #16]
	mov	x0, x19
	adr	x1, nf
	mov	x3, #0
	mov	x8, #88
	svc	#0
	bl	put_hex
	adr	x1, nf
	bl	times
	// utimensat(d, l, {6000, 7000}, AT_SYMLINK_NOFOLLOW): the link's own mtime 7000, f's 2000
	add	x2, sp, #256
	mov	x0, #6000
	stp	x0, xzr, [x2]
	mov	x0, #7000
	stp	x0, xzr, [x2, #16]
	mov	x0, x19
	adr	x1, nl
	mov	x3, #0x100
	mov	x8, #88
	svc	#0
	bl	put_hex
	adr	x1, nl
	bl	mtime
	adr	x1, nf
	bl	mtime
	// utimensat(d, missing, {UTIME_OMIT, UTIME_OMIT}, 0x200): 0, nothing looked at
	add	x2, sp, #256
	mov	x0, #0xfffe
	movk	x0, #0x3fff, lsl #16
	str	x0, [x2, #8]
	str	x0, [x2, #24]
	mov	x0, x19
	adr	x1, nmissing
	mov	x3, #0x200
	mov	x8, #88
	svc	#0
	bl	put_hex
	// a tv_nsec of 1e9: f's EINVAL, missing's ENOENT (the lookup first)
	add	x2, sp, #256
	mov	x0, #0xca00
	movk	x0, #0x3b9a, lsl #16
	str	x0, [x2, #8]
	str	xzr, [x2, #24]
	mov	x0, x19
	adr	x1, nf
	mov	x3, #0
	mov	x8, #88
	svc	#0
	bl	put_hex
	add	x2, sp, #256
	mov	x0, x19
	adr	x1, nmissing
	mov	x3, #0
	mov	x8, #88
	svc	#0
	bl	put_hex
	// utimensat(file, NULL, {10000, 11000}, 0) (futimens): f's mtime 11000; with AT_SYMLINK_NOFOLLOW EINVAL
	add	x2, sp, #256
	mov	x0, #10000
	stp	x0, xzr, [x2]
	mov	x0, #11000
	stp	x0, xzr, [x2, #16]
	mov	x0, x21
	mov	x1, #0
	mov	x3, #0
	mov	x8, #88
	svc	#0
	bl	put_hex
	adr	x1, nf
	bl	mtime
	add	x2, sp, #256
	mov	x0, x21
	mov	x1, #0
	mov	x3, #0x100
	mov	x8, #88
	svc	#0
	bl	put_hex
	// pselect6: rf = openat(d, f, O_RDONLY), pf = openat(d, f, O_PATH); read {rf, pf}, write {rf}, except {rf}, a zero timeout:
	// 3, the sets {rf, pf}, {rf}, {} (shown as each set xor what is wanted: 0)
	mov	x0, x19
	adr	x1, nf
	mov	x2, #0
	mov	x8, #56
	svc	#0
	mov	x22, x0
	mov	x0, x19
	adr	x1, nf
	mov	x2, #0x200000
	mov	x8, #56
	svc	#0
	mov	x23, x0
	mov	x0, #1
	lsl	x24, x0, x22
	lsl	x25, x0, x23
	orr	x25, x25, x24
	str	x25, [sp, #320]
	str	x24, [sp, #328]
	str	x24, [sp, #336]
	stp	xzr, xzr, [sp, #352]
	cmp	x22, x23
	csel	x0, x22, x23, hi
	add	x0, x0, #1
	add	x1, sp, #320
	add	x2, sp, #328
	add	x3, sp, #336
	add	x4, sp, #352
	mov	x5, #0
	mov	x8, #72
	svc	#0
	bl	put_hex
	ldr	x0, [sp, #320]
	eor	x0, x0, x25
	bl	put_hex
	ldr	x0, [sp, #328]
	eor	x0, x0, x24
	bl	put_hex
	ldr	x0, [sp, #336]
	bl	put_hex
	// read {rf, 63}, nfds 64: 63 not open, EBADF
	mov	x0, #1
	lsl	x0, x0, #63
	orr	x0, x0, x24
	str	x0, [sp, #320]
	mov	x0, #64
	add	x1, sp, #320
	mov	x2, #0
	mov	x3, #0
	add	x4, sp, #352
	mov	x5, #0
	mov	x8, #72
	svc	#0
	bl	put_hex
	// nfds -1 EINVAL; a timeout's tv_nsec 1e9 EINVAL; a signal mask of size 4 EINVAL
	mov	x0, #-1
	mov	x1, #0
	mov	x2, #0
	mov	x3, #0
	add	x4, sp, #352
	mov	x5, #0
	mov	x8, #72
	svc	#0
	bl	put_hex
	mov	x0, #0xca00
	movk	x0, #0x3b9a, lsl #16
	str	x0, [sp, #360]
	mov	x0, #0
	mov	x1, #0
	mov	x2, #0
	mov	x3, #0
	add	x4, sp, #352
	mov	x5, #0
	mov	x8, #72
	svc	#0
	bl	put_hex
	str	xzr, [sp, #360]
	str	x24, [sp, #320]
	str	xzr, [sp, #384]
	add	x0, sp, #384
	mov	x1, #4
	stp	x0, x1, [sp, #368]
	add	x0, x22, #1
	add	x1, sp, #320
	mov	x2, #0
	mov	x3, #0
	add	x4, sp, #352
	add	x5, sp, #368
	mov	x8, #72
	svc	#0
	bl	put_hex
	// the same with the mask's size 8: 1
	mov	x1, #8
	str	x1, [sp, #376]
	add	x0, x22, #1
	add	x1, sp, #320
	mov	x2, #0
	mov	x3, #0
	add	x4, sp, #352
	add	x5, sp, #368
	mov	x8, #72
	svc	#0
	bl	put_hex
	// the end: the descriptors closed, the names removed
	mov	x0, x23
	mov	x8, #57
	svc	#0
	mov	x0, x22
	mov	x8, #57
	svc	#0
	mov	x0, x21
	mov	x8, #57
	svc	#0
	mov	x0, x20
	mov	x8, #57
	svc	#0
	mov	x0, x19
	mov	x8, #57
	svc	#0
	bl	clean
	mov	x0, #1
	adr	x1, msg
	mov	x2, #6
	mov	x8, #64
	svc	#0
	b	exit0
// mode: newfstatat(d (x19), x1, AT_SYMLINK_NOFOLLOW)'s st_mode (or its error) shown
mode:
	stp	x29, x30, [sp, #-16]!
	mov	x0, x19
	add	x2, sp, #16
	mov	x3, #0x100
	mov	x8, #79
	svc	#0
	cmp	x0, #0
	b.lt	1f
	ldr	w0, [sp, #32]
1:	bl	put_hex
	ldp	x29, x30, [sp], #16
	ret
// mtime: newfstatat(d, x1, AT_SYMLINK_NOFOLLOW)'s st_mtime's seconds shown
mtime:
	stp	x29, x30, [sp, #-16]!
	mov	x0, x19
	add	x2, sp, #16
	mov	x3, #0x100
	mov	x8, #79
	svc	#0
	ldr	x0, [sp, #104]
	bl	put_hex
	ldp	x29, x30, [sp], #16
	ret
// times: newfstatat(d, x1, AT_SYMLINK_NOFOLLOW)'s st_atime, its nanoseconds, st_mtime, its nanoseconds shown
times:
	stp	x29, x30, [sp, #-16]!
	mov	x0, x19
	add	x2, sp, #16
	mov	x3, #0x100
	mov	x8, #79
	svc	#0
	ldr	x0, [sp, #88]
	bl	put_hex
	ldr	x0, [sp, #96]
	bl	put_hex
	ldr	x0, [sp, #104]
	bl	put_hex
	ldr	x0, [sp, #112]
	bl	put_hex
	ldp	x29, x30, [sp], #16
	ret
// clean: unlinkat(AT_FDCWD, ...) of the files and links, then AT_REMOVEDIR of the directories, the results not shown
clean:
	mov	x0, #-100
	adr	x1, plk
	mov	x2, #0
	mov	x8, #35
	svc	#0
	mov	x0, #-100
	adr	x1, pf
	mov	x2, #0
	mov	x8, #35
	svc	#0
	mov	x0, #-100
	adr	x1, pxy
	mov	x2, #0x200
	mov	x8, #35
	svc	#0
	mov	x0, #-100
	adr	x1, px
	mov	x2, #0x200
	mov	x8, #35
	svc	#0
	mov	x0, #-100
	adr	x1, pz
	mov	x2, #0x200
	mov	x8, #35
	svc	#0
	mov	x0, #-100
	adr	x1, ndir
	mov	x2, #0x200
	mov	x8, #35
	svc	#0
	ret
ndir:	.asciz	"a64i-at.d"
plk:	.asciz	"a64i-at.d/l"
pf:	.asciz	"a64i-at.d/f"
pxy:	.asciz	"a64i-at.d/x/y"
px:	.asciz	"a64i-at.d/x"
pz:	.asciz	"a64i-at.d/z"
nf:	.asciz	"f"
nl:	.asciz	"l"
nx:	.asciz	"x"
nxy:	.asciz	"x/y"
ny:	.asciz	"y"
nz:	.asciz	"z"
nmissing:	.asciz	"missing"
empty:	.asciz	""
msg:	.ascii	"done.\n"
	.balign	4
