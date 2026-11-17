// Package p is a64i's fixed package for cmd/compile and cmd/asm (queue 294, AR22).
package p

import (
	"errors"
	"strconv"
)

// Number is the constraint of Sum.
type Number interface {
	~int | ~int64 | ~float64
}

// Sum adds xs.
func Sum[T Number](xs ...T) T {
	var s T
	for _, x := range xs {
		s += x
	}
	return s
}

// Stack is a generic stack.
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

var ErrEmpty = errors.New("p: empty")

// Parse sums the decimal fields of ss with Add, the assembly function.
func Parse(ss []string) (int64, error) {
	if len(ss) == 0 {
		return 0, ErrEmpty
	}
	var t int64
	for i, s := range ss {
		n, err := strconv.ParseInt(s, 10, 64)
		if err != nil {
			return 0, errors.Join(err, errors.New("field "+strconv.Itoa(i)))
		}
		t = Add(t, n)
	}
	return t, nil
}

// Add is in add_arm64.s.
func Add(a, b int64) int64
