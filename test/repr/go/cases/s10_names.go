package names

type Point struct {
	X, Y,
	Z int
	Name string
}

var a, b,
	c int = 1, 2,
	3

func Collect(args []int, start int, numInts, numFloats int,
	keep []any, addInt, addFloat func(uintptr),
	pInts, pFloats *int) ([]int, []any) {
	return args[start:], keep
}

func Named(x int,
	y int) (r,
	s int) {
	return x, y
}

func Plain(a, b int,
	c string) {
	println(a, b, c)
}

func Literal() func(int, int) int {
	return func(p,
		q int) int {
		return p + q
	}
}

type Iface interface {
	Method(a int,
		b string) error
}

func Generic[T any,
	U comparable](t T,
	u U) {
	println(t, u)
}
