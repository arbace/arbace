package literals

const (
	i1 = 42
	i2 = 0x2A
	i3 = 0o52
	i4 = 052
	i5 = 0b101010
	i6 = 1_000
	i7 = 1 << 62
	i8 = 123456789012345678901234567890
	f1 = 1.5
	f2 = 1e10
	f3 = .5
	f4 = 1e400
	f5 = 0x1p-2
	f6 = 0.1
	f7 = 1.0
	f8 = 0x1p-1074
	f9 = 6.02214076e23
	c1 = 2i
	c2 = 1.5e3i
	r1 = 'a'
	r2 = '\n'
	r3 = 'é'
	r4 = '\x00'
	r5 = '😀'
	r6 = '\''
	r7 = '\\'
	r8 = '\u2028'
	s1 = "abc"
	s2 = `a\b`
	s3 = "\xff\x00a"
	s4 = "tab\tquote\"backslash\\"
	s5 = "ünïcödé €"
	s6 = "\a\b\f\r\v"
	s7 = "\u00a0\ufeff"
	b1 = true
	b2 = false
	n1 = -1
	n2 = -0.0
	n3 = - -1
	n4 = -1.5
	n5 = -0x1p-2
)

const raw = `line one
line two	tabbed
`

const big = 1 << 100

const bigf = 1e100

var (
	nilv     error = nil
	neg            = -i1
	negBig         = -12345678901234567
	runeConv       = rune(65)
	sum            = 'a' + 1
)
