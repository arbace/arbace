package types

import (
	"io"
	"unsafe"
)

type (
	P   *int
	S   []string
	A   [4]byte
	AN  [N]int
	AE  [N * 2]int
	M   map[string][]int
	C   chan int
	CR  <-chan int
	CS  chan<- int
	CC  chan (<-chan int)
	CSR chan<- <-chan int
	F   func(int, ...any) (int, error)
	FN  func(x int) (n int, err error)
	FV  func()
	FR  func() func() int
	U   unsafe.Pointer
	PP  **Point
)

const N = 3

type Point struct{ X, Y int32 }

type Fields struct {
	Point
	*io.SectionReader
	// Name is documented.
	Name string `json:"name"`
	a, b int
	f    func()
	Pair[string, int]
	t string "plain"
}

type Pair[K comparable, V any] struct {
	Key K
	Val V
}

type Shape interface {
	Area() float64
	io.Reader
	Read2(p []byte) (n int, err error)
}

type Number interface {
	~int | ~float64 | string
}

type Both interface {
	~int
	String() string
}

type slice[T any] []T

type Uses struct {
	s  slice[int]
	p  Pair[string, int]
	r  io.Reader
	e  struct{}
	i  interface{}
	an any
	l  List[Pair[int, slice[byte]]]
}

type List[T any] struct {
	next *List[T]
	val  T
}

type Ptr[P *int,] struct{ p P }

type Ord[T interface{ ~int | ~string }] struct{ v T }

type Small struct{ x map[string]int }

type Large struct {
	x map[string]map[string]int64
}

type OneMethod interface{ String() string }

type LongMethod interface {
	Method(a, b, c int) (string, error)
}
