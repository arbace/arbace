# Arbace's run test (AR4): `hello` written and exit 42, by system calls alone (Linux's generic numbers: write 64, exit 93)
# exit: 42
# stdout: "hello\n"
	.section .rodata
msg:	.ascii "hello\n"
	len = 6
	.globl _start
	.text
_start:
	mov x0, #1
	adrp x1, msg
	add x1, x1, :lo12:msg
	mov x2, #len
	mov x8, #64
	svc #0
	mov x0, #42
	mov x8, #93
	svc #0
