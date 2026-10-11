package groups

func F[T any, U any](a T, b T, c, d U) (x, y T) { return a, b }

func G[T, U any](a, b T) (T, U) {
	var u U
	return a, u
}

type S[T any] struct {
	a, b T
	c    T
	f    func(x, y T)
}
