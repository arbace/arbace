# a64i's program (AR5): conditional branch: b.cond for each condition under each NZCV
# with: lib/out.s
# exit: 0
# stdout: "0000000000015ab2\n"
# stdout: "000000000001a9b2\n"
# stdout: "000000000001568e\n"
# stdout: "000000000001a58e\n"
# stdout: "0000000000019ab1\n"
# stdout: "000000000001a9b1\n"
# stdout: "0000000000019a8d\n"
# stdout: "000000000001a98d\n"
# stdout: "000000000001aa72\n"
# stdout: "0000000000015972\n"
# stdout: "000000000001a64e\n"
# stdout: "000000000001554e\n"
# stdout: "000000000001aa71\n"
# stdout: "0000000000019971\n"
# stdout: "000000000001aa4d\n"
# stdout: "000000000001994d\n"
	.text
	.globl	_start
_start:
	mov	x9, #0
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #268435456
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #536870912
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #805306368
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #1073741824
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #1342177280
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #1610612736
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #1879048192
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #2147483648
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #2415919104
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #2684354560
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #2952790016
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #3221225472
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #3489660928
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #3758096384
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	mov	x9, #4026531840
	msr	nzcv, x9
	mov	x0, #0
	b.eq	1f
	b	2f
1:	orr	x0, x0, #1
2:
	b.ne	1f
	b	2f
1:	orr	x0, x0, #2
2:
	b.cs	1f
	b	2f
1:	orr	x0, x0, #4
2:
	b.hs	1f
	b	2f
1:	orr	x0, x0, #8
2:
	b.cc	1f
	b	2f
1:	orr	x0, x0, #16
2:
	b.lo	1f
	b	2f
1:	orr	x0, x0, #32
2:
	b.mi	1f
	b	2f
1:	orr	x0, x0, #64
2:
	b.pl	1f
	b	2f
1:	orr	x0, x0, #128
2:
	b.vs	1f
	b	2f
1:	orr	x0, x0, #256
2:
	b.vc	1f
	b	2f
1:	orr	x0, x0, #512
2:
	b.hi	1f
	b	2f
1:	orr	x0, x0, #1024
2:
	b.ls	1f
	b	2f
1:	orr	x0, x0, #2048
2:
	b.ge	1f
	b	2f
1:	orr	x0, x0, #4096
2:
	b.lt	1f
	b	2f
1:	orr	x0, x0, #8192
2:
	b.gt	1f
	b	2f
1:	orr	x0, x0, #16384
2:
	b.le	1f
	b	2f
1:	orr	x0, x0, #32768
2:
	b.al	1f
	b	2f
1:	orr	x0, x0, #65536
2:
	bl	put_hex
	b	exit0
