package p

import "cmp"

//go:noinline

func Clamp[T cmp.Ordered](x, lo, hi T) T {
	return min(max(x, lo), hi)
}

func Count(s []byte, c byte) int {
	const (
		none = iota - 1
		one
	)
	n := none + one
retry:
	i := 0
	for i < len(s) && (s[i] == c || c == 0) {
		n++
		i++
	}
	if n < 0 {
		n = 0
		goto retry
	}
	return n
}
