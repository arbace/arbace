package printer

type P struct{ X int }

type N struct {
	P
	Name string
}

type Pair[K comparable, V any] struct {
	Key K
	Val V
}

type List[E any] struct{ items []E }

// Apply is a generic method (Go 1.27).
func (l List[E]) Apply[F any](f func(E) F) List[F] {
	var out List[F]
	for _, x := range l.items {
		out.items = append(out.items, f(x))
	}
	return out
}

var promoted = N{X: 1, Name: "a"}

var applied = List[int]{}.Apply(func(x int) string { return "" })

func Headers(p P, pr Pair[int, int], f func() int) int {
	switch (Pair[int, int]{}) {
	case pr:
		return 1
	}
	if pr == (Pair[int, int]{1, 2}) {
		return 2
	}
	if &p == &(P{}) {
		return 3
	}
	if g(P{1}) {
		return 4
	}
	for _, x := range (List[int]{}).items {
		return x
	}
	if (func() int)(f)() == 0 {
		return 5
	}
	return 0
}

func g(P) bool { return false }

func Exprs(c chan int, p *P, a, b int) {
	_ = (<-chan int)(c)
	_ = (chan<- int)(c)
	_ = (*P).Get
	_ = <-c
	_ = -<-c
	_ = a - -b
	_ = a + +b
	_ = a / *(&b)
	_ = *&a
	_ = !!true
	_ = ^^a
	_ = -(a + b)
	_ = (a + b) * -(a - b)
	_ = a<<b>>a
	_ = a &^ ^b
	c <- -1
	c <- <-c
	p.X--
}

func (p *P) Get() int { return p.X }
