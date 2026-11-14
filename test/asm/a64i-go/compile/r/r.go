// Package r imports p (the compile run's own export data) and slices (generic std code instantiated here).
package r

import (
	"slices"

	"a64ic/p"
)

// Report is p's functions over n: generic code from p's export data, inlined and instantiated.
func Report(n int) (int, float64, []int, int64) {
	var st p.Stack[string]
	for i := range n {
		st.Push(string(rune('a' + i%26)))
	}
	xs := p.Fib(n)
	slices.Sort(xs)
	m, _ := p.Areas(n)
	keys := slices.Sorted(func(yield func(string) bool) {
		for k := range m {
			if !yield(k) {
				return
			}
		}
	})
	t, _ := p.Parse(keys[:0:0])
	return len(keys), p.Sum(1.5, 2.5, float64(len(xs))), slices.Compact(xs), t
}
