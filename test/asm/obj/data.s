# Arbace's synthetic object test (AX4): the data directives, symbols and their attributes, the data relocations
	.data
	.globl	counter
	.type	counter,@object
	.size	counter, 8
	.p2align 3
counter:
	.quad	42
bytes:
	.byte	1, 2, 0xff, -1
	.short	0x1234, -2
	.long	0xdeadbeef, ext_sym, ext_sym+16
	.quad	ext_sym, local_ro, local_ro+4
	.long	end_data-bytes
	.long	ext_sym-.
	.long	local_ro-.
	.ascii	"abc", "de\n"
	.asciz	"hello"
	.string	"world"
	.zero	5
	.fill	3, 2, 0x4142
	.skip	2, 0x7f
	.balign	16, 0xcc
	.p2alignw 3, 0x9090
end_data:
	.section .rodata
local_ro:
	.long	1, 2, 3
	.weak	weak_ref
	.quad	weak_ref
	.hidden	hid
	.globl	hid
hid:	.byte	9
	.protected prot
	.globl	prot
prot:	.byte	10
	.comm	common_a, 16, 8
	.comm	common_b, 4
	.lcomm	lcomm_a, 24
	.local	lc2
	.comm	lc2, 8, 8
	.bss
	.globl	zeroed
	.type	zeroed,@object
zeroed:	.zero	64
	.size	zeroed, .-zeroed
	.set	five, 5
	.globl	alias
	alias = counter + 4
	.equ	ten, five*2
	.data
	.long	five, ten
