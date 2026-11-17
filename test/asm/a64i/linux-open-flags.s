# a64i's program (queue 309, review 306's findings 2 and 3): open's flags at arm64's values and the links' calls. symlinkat makes
# a link to a file (0), again EEXIST; openat of the file with O_DIRECTORY (arm64 0x4000) ENOTDIR, with O_DIRECT (0x10000) a
# descriptor whose F_GETFL is O_DIRECT|O_LARGEFILE (0x30000); the link with O_NOFOLLOW (0x8000) ELOOP, with O_LARGEFILE (0x20000,
# x86-64's O_NOFOLLOW: musl's every open) opened through, its 5 bytes read, F_GETFL 0x20000; with O_PATH|O_NOFOLLOW the link itself
# (F_GETFL 0x208000, fstat's type S_IFLNK, read EBADF, readlinkat(fd, "") its target's 11 bytes); O_DIRECTORY|O_NOFOLLOW on the
# link ENOTDIR, O_NOFOLLOW|O_CREAT|O_EXCL EEXIST, O_CREAT|O_DIRECTORY EINVAL; F_SETFL O_APPEND|O_NONBLOCK, F_GETFL 0x20c00;
# linkat of the file (0, its nlink 2), of the link without AT_SYMLINK_FOLLOW (a link: readlinkat 11), with it (the file:
# readlinkat EINVAL), a bad flag EINVAL; then the five names unlinked (0)
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffef\n"
# stdout: "ffffffffffffffec\n"
# stdout: "0000000000030000\n"
# stdout: "ffffffffffffffd8\n"
# stdout: "0000000000000005\n"
# stdout: "0000000000020000\n"
# stdout: "0000000000208000\n"
# stdout: "000000000000a000\n"
# stdout: "fffffffffffffff7\n"
# stdout: "000000000000000b\n"
# stdout: "ffffffffffffffec\n"
# stdout: "ffffffffffffffef\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000020c00\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000002\n"
# stdout: "0000000000000000\n"
# stdout: "000000000000000b\n"
# stdout: "0000000000000000\n"
# stdout: "ffffffffffffffea\n"
# stdout: "ffffffffffffffea\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "done.\n"
	.text
	.globl	_start
_start:
	sub	sp, sp, #256
	// a clean start: the names unlinked, their results not shown
	adr	x1, nl
	bl	unl
	adr	x1, nh
	bl	unl
	adr	x1, nh2
	bl	unl
	adr	x1, nh3
	bl	unl
	adr	x1, nf
	bl	unl
	// the file: openat(O_WRONLY|O_CREAT|O_TRUNC, 0644), "data\n", close
	mov	x0, #-100
	adr	x1, nf
	mov	x2, #0x241
	mov	x3, #0644
	mov	x8, #56
	svc	#0
	mov	x19, x0
	adr	x1, data
	mov	x2, #5
	mov	x8, #64
	svc	#0
	mov	x0, x19
	mov	x8, #57
	svc	#0
	// symlinkat(nf, AT_FDCWD, nl), twice
	adr	x0, nf
	mov	x1, #-100
	adr	x2, nl
	mov	x8, #36
	svc	#0
	bl	put_hex
	adr	x0, nf
	mov	x1, #-100
	adr	x2, nl
	mov	x8, #36
	svc	#0
	bl	put_hex
	// the file with O_DIRECTORY
	adr	x1, nf
	mov	x2, #0x4000
	bl	op
	bl	put_hex
	// the file with O_DIRECT: F_GETFL
	adr	x1, nf
	mov	x2, #0x10000
	bl	op
	bl	getfl
	// the link with O_NOFOLLOW
	adr	x1, nl
	mov	x2, #0x8000
	bl	op
	bl	put_hex
	// the link with O_LARGEFILE: read, F_GETFL
	adr	x1, nl
	mov	x2, #0x20000
	bl	op
	mov	x19, x0
	add	x1, sp, #128
	mov	x2, #64
	mov	x8, #63
	svc	#0
	bl	put_hex
	mov	x0, x19
	bl	getfl
	// the link with O_PATH|O_NOFOLLOW: F_GETFL, fstat's type, read, readlinkat(fd, "")
	adr	x1, nl
	mov	x2, #0x8000
	movk	x2, #0x20, lsl #16
	bl	op
	mov	x19, x0
	mov	x1, #3
	mov	x8, #25
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x1, sp
	mov	x8, #80
	svc	#0
	ldr	w0, [sp, #16]
	and	w0, w0, #0xf000
	bl	put_hex
	mov	x0, x19
	add	x1, sp, #128
	mov	x2, #64
	mov	x8, #63
	svc	#0
	bl	put_hex
	mov	x0, x19
	adr	x1, empty
	add	x2, sp, #128
	mov	x3, #64
	mov	x8, #78
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x8, #57
	svc	#0
	// the link with O_DIRECTORY|O_NOFOLLOW, with O_NOFOLLOW|O_CREAT|O_EXCL|O_WRONLY; a new name with O_CREAT|O_DIRECTORY
	adr	x1, nl
	mov	x2, #0xc000
	bl	op
	bl	put_hex
	adr	x1, nl
	mov	x2, #0x80c1
	bl	op
	bl	put_hex
	adr	x1, nh
	mov	x2, #0x4040
	bl	op
	bl	put_hex
	// the file O_RDONLY: F_SETFL O_APPEND|O_NONBLOCK, F_GETFL
	adr	x1, nf
	mov	x2, #0
	bl	op
	mov	x19, x0
	mov	x1, #4
	mov	x2, #0xc00
	mov	x8, #25
	svc	#0
	mov	x0, x19
	bl	getfl
	// linkat(nf, nh, 0): newfstatat's nlink
	mov	x0, #-100
	adr	x1, nf
	mov	x2, #-100
	adr	x3, nh
	mov	x4, #0
	mov	x8, #37
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, nf
	mov	x2, sp
	mov	x3, #0
	mov	x8, #79
	svc	#0
	ldr	w0, [sp, #20]
	bl	put_hex
	// linkat(nl, nh2, 0): a link; linkat(nl, nh3, AT_SYMLINK_FOLLOW): the file; a bad flag
	mov	x0, #-100
	adr	x1, nl
	mov	x2, #-100
	adr	x3, nh2
	mov	x4, #0
	mov	x8, #37
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, nh2
	add	x2, sp, #128
	mov	x3, #64
	mov	x8, #78
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, nl
	mov	x2, #-100
	adr	x3, nh3
	mov	x4, #0x400
	mov	x8, #37
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, nh3
	add	x2, sp, #128
	mov	x3, #64
	mov	x8, #78
	svc	#0
	bl	put_hex
	mov	x0, #-100
	adr	x1, nf
	mov	x2, #-100
	adr	x3, nh3
	mov	x4, #1
	mov	x8, #37
	svc	#0
	bl	put_hex
	// the names unlinked
	adr	x1, nl
	bl	unl
	bl	put_hex
	adr	x1, nh
	bl	unl
	bl	put_hex
	adr	x1, nh2
	bl	unl
	bl	put_hex
	adr	x1, nh3
	bl	unl
	bl	put_hex
	adr	x1, nf
	bl	unl
	bl	put_hex
	adr	x0, msg
	mov	x1, #6
	bl	put_str
	add	sp, sp, #256
	b	exit0
// op: openat(AT_FDCWD, x1, x2, 0) into x0
op:
	mov	x0, #-100
	mov	x3, #0
	mov	x8, #56
	svc	#0
	ret
// getfl: fcntl(x0, F_GETFL) shown, then x0 closed (x0 < 0: shown as it is)
getfl:
	stp	x29, x30, [sp, #-32]!
	str	x19, [sp, #16]
	mov	x19, x0
	cmp	x0, #0
	b.lt	1f
	mov	x1, #3
	mov	x8, #25
	svc	#0
	bl	put_hex
	mov	x0, x19
	mov	x8, #57
	svc	#0
	b	2f
1:	bl	put_hex
2:	ldr	x19, [sp, #16]
	ldp	x29, x30, [sp], #32
	ret
// unl: unlinkat(AT_FDCWD, x1, 0) into x0
unl:
	mov	x0, #-100
	mov	x2, #0
	mov	x8, #35
	svc	#0
	ret
nf:	.asciz	"a64i-open.f"
nl:	.asciz	"a64i-open.l"
nh:	.asciz	"a64i-open.h"
nh2:	.asciz	"a64i-open.h2"
nh3:	.asciz	"a64i-open.h3"
empty:	.asciz	""
data:	.ascii	"data\n"
msg:	.ascii	"done.\n"
	.balign	4
