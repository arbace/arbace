// Declarations (SPEC §6): constants with their values, variables, types, functions,
// methods, doc comments and directives (§9).
package decls

import _ "unsafe"

// Weekday is a day.
type Weekday int

const (
	Sunday Weekday = iota
	Monday
	Tuesday
)

// Numbers fundamental to the encoding.
const (
	RuneError = '�' // the "error" Rune
	MaxRune   = '\U0010FFFF'
	Big       = 1 << 100
	Small     = Big >> 98
	F         = 2.5
	F2        = 1e400
	Third     = 1.0 / 3
	C         = 1 + 2i
	S         = "s"
	Bad       = "\xff"
	T         = true
	Typed     float32 = 0.1
	TC        complex64 = 1i
	Neg       = -1
	Int       = 1.0 + 0
)

const Single = 4

const X, Y = 1, 2

// ErrX is a variable.
var ErrX = 1

var (
	a, b int
	c, d = 1, 2
	// e's doc.
	e float64 = 3
)

var _ any = (*T1)(nil)

var p, q = pair()

func pair() (int, int) { return 1, 2 }

var first = [4]uint8{
	1, 2,
	3, 4,
}

//go:noinline
//go:nosplit
func f() {}

// nanotime is implemented elsewhere.
//
//go:linkname nanotime runtime.nanotime
func nanotime() int64

//go:linkname other runtime.other

func init() {}

func init() {}

// T1 is a type.
type T1 struct{}

func (T1) M() {}

// N doubles.
func (t *T1) N(x int) (r int) {
	r = x * 2
	return
}

type (
	// G1's doc.
	G1 int
	G2 = string
)
