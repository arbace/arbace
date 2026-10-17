package exprs

import (
	"io"
	"slices"
	"unsafe"
)

type Point struct{ X, Y int }

type Named struct {
	Point
	Name string
}

func (p Point) Len() int     { return p.X + p.Y }
func (p *Point) Scale(k int) { p.X *= k }

type Fns struct{ f func(int) int }

type Pair[K comparable, V any] struct {
	Key K
	Val V
}

func Ident[T any](x T) T { return x }

func Two[A, B any](a A, b B) (A, B) { return a, b }

type List[T any] struct{ items []T }

func (l *List[T]) Add(x T) { l.items = append(l.items, x) }

func Ops(a, b, c int, u uint, p *int, f, g bool) int {
	x := a + b*c
	x = (a + b) * c
	x = a - (b - c)
	x = a - b - c
	x = a + b + c
	x = a / *p
	x = a & ^b
	x = a &^ b
	x = a - -1
	x = - -a
	x = -(-1)
	x = +a
	x = ^a
	x = a<<2 | b>>1
	x = a % b << 1
	x = int(u >> 3)
	x = a | b&c ^ c
	x = (a | b) & c
	x = *p * *p
	_ = !f && g || f && !g
	_ = (f || g) && f
	_ = f || g && f
	_ = a < b == (c < a)
	_ = a == b && b != c && a <= c && a >= b && a > 0
	_ = x & 1
	return x
}

func Selectors(n Named, pn *Named, fs Fns, r io.Reader, l *List[int]) int {
	x := n.X
	x += n.Point.Y
	x += pn.Len()
	pn.Scale(2)
	n.Scale(3)
	x += fs.f(1)
	m := n.Len
	x += m()
	me := Point.Len
	x += me(n.Point)
	mp := (*Point).Scale
	mp(&n.Point, 2)
	ri := io.Reader.Read
	_ = ri
	_ = r.Read
	l.Add(1)
	y := &n.Point.Y
	*y++
	pp := &y
	**pp = 2
	return x + *y
}

func Literals() any {
	p := Point{1, 2}
	q := Point{X: 1, Y: 2}
	e := Point{}
	pe := &Point{}
	s := []int{1, 2, 3}
	a := [...]string{"a", "b"}
	a2 := [4]int{1: 10, 3: 30}
	m := map[string]int{"a": 1, "b": 2}
	ps := []Point{{1, 2}, {X: 3}}
	pps := []*Point{{1, 2}, {}}
	mm := map[Point]string{{1, 2}: "a"}
	nn := Named{Point{1, 2}, "n"}
	pr := Pair[string, int]{"a", 1}
	nested := [][]int{{1}, {2, 3}}
	fn := func(x int) int { return x * 2 }
	st := struct{ A, B int }{1, 2}
	return []any{p, q, e, pe, s, a, a2, m, ps, pps, mm, nn, pr, nested, fn, st}
}

func Index(s []int, m map[string]int, str string, arr *[4]int) int {
	x := s[0] + s[len(s)-1]
	x += m["k"]
	v, ok := m["k"]
	_ = ok
	x += v
	x += int(str[1])
	x += arr[2]
	_ = s[1:]
	_ = s[:2]
	_ = s[1:2]
	_ = s[:]
	_ = s[1:2:3]
	_ = s[:2:3]
	_ = s[x+1 : x+2]
	_ = s[:x+1]
	_ = str[1:]
	_ = arr[:]
	return x
}

func Conv(p *int, c chan int, f func(), a any, u uintptr) {
	_ = (*int)(p)
	_ = (<-chan int)(c)
	_ = (func())(f)
	_ = (chan int)(c)
	_ = unsafe.Pointer(p)
	_ = []byte("abc")
	_ = string(rune(65))
	_ = float64(3)
	_ = a.(int)
	_ = a.(*Point)
	_ = a.(interface{ Len() int })
	n, ok := a.(int)
	_, _ = n, ok
	_ = unsafe.Pointer(u)
	_ = (*Point)(unsafe.Pointer(p))
	_ = [2]int([]int{1, 2})
	_ = Pair[string, int](Pair[string, int]{})
}

func Calls(xs []int, args ...any) int {
	n := len(xs) + cap(xs)
	xs = append(xs, 1, 2)
	xs = append(xs, xs...)
	bs := append([]byte("x"), "abc"...)
	copy(xs, xs[1:])
	m := make(map[string]int)
	m2 := make(map[string]int, 10)
	delete(m, "a")
	clear(m2)
	s := make([]int, 2, 10)
	c := make(chan int, 1)
	close(c)
	p := new(int)
	q := new(42)
	r := new(Pair[string, int])
	z := complex(1, 2)
	_ = real(z) + imag(z)
	_ = min(1, 2, n)
	_ = max(n, 3)
	print(n)
	println(bs, s, p, q, r)
	sink(args...)
	_ = Ident(1)
	_ = Ident[string]("a")
	f := Ident[int]
	_ = f
	_, _ = Two[int](1, "b")
	slices.Sort(xs)
	_ = unsafe.Sizeof(n)
	_ = unsafe.Slice(&xs[0], 1)
	_ = unsafe.String(&bs[0], 1)
	_ = unsafe.Add(unsafe.Pointer(p), 8)
	_ = unsafe.Alignof(n)
	_ = unsafe.Offsetof(Point{}.Y)
	func() {}()
	defer func(x int) {}(1)
	recover()
	return n + <-c
}

func Header(p Point, ps []Point) int {
	if p == (Point{}) {
		return 1
	}
	if p == (Point{1, 2}) {
		return 2
	}
	for _, q := range []Point{{1, 2}} {
		if q == p {
			return 3
		}
	}
	for i := (Point{}); i.X < 3; i.X++ {
	}
	if p == func() Point { return Point{} }() {
		return 4
	}
	return 0
}
func sink(xs ...any) {}
