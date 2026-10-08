package positions

import "strings"
import "bytes"

var x = strings.ToUpper("a") + string(bytes.ToUpper(nil))

var table = [8]uint8{
	1, 2, 3, 4,
	5, 6, 7, 8,
}

var rows = [...]string{
	"a", "b",
	"c",
}

func Call(a, b, c int) int {
	return add(a,
		b,
		c)
}

func Multi(x int) int {
	y := x +
		1
	z := add(
		x,
		y,
	)
	return z
}

func add(xs ...int) int { return len(xs) }

func Lit(n int) []int {
	s := []int{
		n,
		n + 1,
	}
	m := map[string]int{
		"a":   1,
		"bcd": 2,
	}
	_ = m
	return s
}

func Closure() func() int {
	f := func() int {
		return 1
	}
	return f
}

type Doc struct {
	// A is a.
	A int

	// B is b.
	B string
}

func Label() {
L:
	for {
		break L
	}
}

func Short() int { return 1 }
func Short2() int { return 2 }
