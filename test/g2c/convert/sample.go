package sample

import (
	"errors"
	"fmt"
)

type Point struct{ X, Y int32 }

type Named struct {
	Point // embedded
	Name  string
}

func (p *Point) Move(dx int32) { p.X += dx }

func (p Point) String() string { return fmt.Sprintf("(%d,%d)", p.X, p.Y) }

type Stack[T any] struct{ items []T }

func (s *Stack[T]) Push(x T) { s.items = append(s.items, x) }

func (s *Stack[T]) Pop() (T, bool) {
	var zero T
	if len(s.items) == 0 {
		return zero, false
	}
	x := s.items[len(s.items)-1]
	s.items = s.items[:len(s.items)-1]
	return x, true
}

const (
	A = iota * 10
	B
	Big = 1 << 100
	Small = Big >> 98
)

var ErrEmpty = errors.New("empty")

func Sum(xs []uint8) (s uint8) {
	for _, x := range xs {
		s += x // wraps
	}
	return
}

func Safe(f func()) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("recovered: %v", r)
		}
	}()
	f()
	return nil
}

func Pipeline(n int) int {
	ch := make(chan int)
	done := make(chan struct{})
	go func() {
		defer close(ch)
		for i := range n {
			select {
			case ch <- i:
			case <-done:
				return
			}
		}
	}()
	total := 0
	for v := range ch {
		total += v
	}
	return total
}

func Describe(v any) string {
	switch x := v.(type) {
	case nil:
		return "nil"
	case fmt.Stringer:
		return x.String()
	case int, int64:
		return fmt.Sprint(x)
	}
	return "?"
}

func Use() int32 {
	n := Named{Point{1, 2}, "n"}
	n.Move(Small)
	p := &n.Point.Y
	*p++
	return n.X + *p
}
