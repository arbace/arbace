package p

import "strconv"

type shape interface {
	area() float64
	name() string
}

type rect struct{ w, h float64 }
type square struct{ rect }

func (r rect) area() float64 { return r.w * r.h }
func (r rect) name() string  { return "rect " + strconv.FormatFloat(r.area(), 'g', -1, 64) }
func (s square) name() string { return "square" }

// Areas maps each shape's name to its area, deferring a counter.
func Areas(n int) (m map[string]float64, calls int) {
	defer func() { calls++ }()
	m = make(map[string]float64)
	for i := 1; i <= n; i++ {
		var s shape = rect{float64(i), 2}
		if i%2 == 0 {
			s = square{rect{float64(i), float64(i)}}
		}
		m[s.name()] += s.area()
	}
	return m, calls
}

// Fib's closures and a switch.
func Fib(n int) []int {
	a, b := 0, 1
	next := func() int { a, b = b, a+b; return a }
	out := make([]int, 0, n)
	for range n {
		switch v := next(); {
		case v%3 == 0:
			out = append(out, -v)
		default:
			out = append(out, v)
		}
	}
	return out
}
