// Types (SPEC §5): type forms, signatures, structs, interfaces, generics.
package types5

import "io"

type Point struct{ X, Y int32 }

type Named struct {
	Point              // embedded
	*io.SectionReader  `json:"r"`
	Name  string `json:"name"`
	// F's doc.
	F func(int, ...any) (int, error)
	_ int
}

type (
	A = int
	B []map[string]*Point
	C [4]chan<- int
	D <-chan []byte
	E [2 * 3]struct{}
	F interface {
		io.Reader
		// Area's doc.
		Area() float64
		Read2(p []byte) (n int, err error)
		Close()
	}
	G func(x int) (n int, err error)
	H chan (<-chan int)
	I interface{}
)

type Number interface {
	~int | ~float64 | string
}

type Both interface {
	~int
	comparable
}

type Pair[K comparable, V any] struct {
	Key K
	Val V
}

type List[T any] []T

type Set[K comparable] = map[K]bool

type Tree[T interface{ Less(T) bool }] struct {
	Left, Right *Tree[T]
	Val         T
}

func (l List[T]) Len() int { return len(l) }

func (l List[_]) Cap() int { return cap(l) }

func Map[S ~[]E, E any, F any](s S, f func(E) F) []F {
	var out []F
	for _, x := range s {
		out = append(out, f(x))
	}
	return out
}

func Two[A, B any](a A, b B) {}

func Grouped[T any](a, b []T, c T) {}

func Variadic(format string, args ...any) {}

func Unnamed(int, string) (bool, error) { return false, nil }

func Use() {
	_ = Map(List[int]{1}, func(i int) string { return "" })
	_ = Pair[string, int]{"a", 1}
	f := Map[[]int, int, bool]
	_ = f
	Two[int](1, "x")
	var s Set[int]
	_ = s
}
