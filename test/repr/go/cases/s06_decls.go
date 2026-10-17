package decls

import (
	"errors"
	"io"
)

// Celsius is a temperature.
type Celsius float64

type IntAlias = int

type Set[K comparable] = map[K]bool

type (
	// A is documented in the group.
	A int
	B = string
)

type (
	One int
)

// Weekday is a day.
type Weekday int

const (
	Sunday Weekday = iota
	Monday
	Tuesday
)

const UTFMax = 4

// Big is big.
const Big = 1 << 100

const X, Y = 1, "y"

const (
	k1, k2 = iota, iota * 10
	k3, k4
)

const typed int32 = 5

const (
	c1 = 1
)

var ErrEmpty = errors.New("empty")

var first [4]uint8

var a, b int

var r, w, _ = pipe()

var _ io.Reader = (*T)(nil)

var (
	// v1 is documented.
	v1     = 1
	v2 int = 2
	v3 string
)

var (
	g1 = 1
)

func pipe() (int, int, error) { return 0, 1, nil }

type T struct{}

func (*T) Read(p []byte) (int, error) { return 0, nil }

func Divmod(a, b int) (q, r int) {
	q = a / b
	r = a % b
	return
}

// Move moves.
func (p *Point) Move(dx int32) { p.X += dx }

func (p Point) Get() (x int32, y int32) { return p.X, p.Y }

type Point struct{ X, Y int32 }

func init() {}

func init() { a = 1 }

func Len(x []int) int { return len(x) }

func Map[T, U any](xs []T, f func(T) U) []U {
	var out []U
	for _, x := range xs {
		out = append(out, f(x))
	}
	return out
}

func Keys[M ~map[K]V, K comparable, V any](m M) []K {
	var ks []K
	for k := range m {
		ks = append(ks, k)
	}
	return ks
}

type Stack[T any] struct{ items []T }

func (s *Stack[T]) Push(x T) { s.items = append(s.items, x) }

func (s Stack[T]) Len() int { return len(s.items) }

func Nothing() {}

func Panics() int { panic("no") }

func Named() (err error) { return }

func Variadic(xs ...int) int { return len(xs) }

func Variadic2(f string, xs ...any) {}
