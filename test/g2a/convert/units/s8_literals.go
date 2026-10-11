// Literals and constant values (SPEC §8).
package lits

const (
	I1 = 42
	I2 = 0x2A
	I3 = 0o52
	I4 = 052
	I5 = 0b101010
	I6 = 1_000
	I7 = 0x1_0000_0000_0000_0000
	F1 = 1.5
	F2 = 1e10
	F3 = .5
	F4 = 1e400
	F5 = 0x1p-2
	F6 = 0.1
	F7 = 1.
	F8 = 0x1p4
	F9 = 1e100
	C1 = 2i
	C2 = 1.5e3i
	C3 = 0123i
	R1 = 'a'
	R2 = '\n'
	R3 = 'é'
	R4 = '\x00'
	R5 = '😀'
	R6 = '\''
	S1 = "abc"
	S2 = `a\b`
	S3 = "\xff\x00a"
	S4 = "tab\there\"q\"\\"
	S5 = "😀"
	S6 = `multi
line`
	B1 = true
)

var V1 float32 = 0.1
var V2 = []byte("\x00\xc3")
