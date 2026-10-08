//go:build tamago

package rt

//go:generate echo among the imports

import "fmt"

func Local() any {
	type Box[T any] struct{ v T }
	type Pair[K comparable, V any] struct {
		k K
		v V
	}
	return Box[Pair[string, int]]{}
}

func Param(true bool) bool { return !true }

func Labels(n int) int {
outer:
inner:
	for i := 0; i < n; i++ {
		if i > 2 {
			break inner
		}
		if i > 3 {
			continue inner
		}
	}
	if n < 0 {
		goto outer
	}
	var x = -0x8000000000000000
	const ()
	switch {
	case n > 0:
		return x + n
	}
	return 0
}

func Logic(a, b, c bool) bool {
	return (a || b) && (b && c) || a && (false || c)
}

var _ = fmt.Sprint
