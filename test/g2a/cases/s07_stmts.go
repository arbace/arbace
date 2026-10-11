package stmts

import "fmt"

type T struct {
	f  int
	fn func(int) int
	m  map[string]int
	a  [3]int
}

func g() (int, error) { return 1, nil }

func Decls() int {
	x := 1
	a, b := 2, 3
	c, err := g()
	d, err := g()
	var e int
	var f int = 4
	var h = 5
	var i, j int
	var k, l int = 6, 7
	const N = 10
	const M int = 11
	type L struct{ v int }
	type LA = int
	_, _ = err, L{}
	var la LA
	return x + a + b + c + d + e + f + h + i + j + k + l + N + M + la
}

func Assign(t *T, p *int, s []int) {
	x := 0
	x = 1
	x, *p = *p, x
	_ = x
	x += 1
	x -= 1
	x *= 2
	x /= 2
	x %= 3
	x <<= 1
	x >>= 1
	x &= 7
	x |= 8
	x ^= 1
	x &^= 2
	x++
	x--
	s[0] = x
	s[1], s[2] = s[2], s[1]
	t.f = x
	t.a[1] = 2
	t.m["k"] = 3
	*p = 4
	{
		y := x
		_ = y
	}
	{
	}
}

func If(x int) string {
	if x > 0 {
		return "pos"
	}
	if x < -10 {
		x++
	} else {
		x--
	}
	if y := x * 2; y > 4 {
		return "big"
	}
	if x++; x > 3 {
		return "x"
	}
	if x == 1 {
		return "one"
	} else if x == 2 {
		return "two"
	} else {
		return "many"
	}
}

func Else(x int) int {
	if x == 0 {
		return 1
	} else {
		if x == 1 {
			return 2
		}
	}
	if x == 3 {
		x++
	} else if x == 4 {
		x--
	}
	if x == 5 {
		x++
	} else if y := x; y == 6 {
		x--
	} else {
		x = 0
	}
	if x == 7 {
	}
	return x
}

func Switch(x int) int {
	switch x {
	case 1, 2:
		x++
	default:
		x--
	case 3:
		x *= 2
		fallthrough
	case 4:
		x /= 2
	}
	switch {
	case x > 10:
		return 1
	}
	switch y := x * 2; {
	case y > 4:
		return 2
	}
	switch y := x; y {
	case 0:
	}
	switch x := x + 1; x {
	}
	switch {
	}
	return x
}

func TypeSwitch(v any) int {
	switch x := v.(type) {
	case nil:
		return 0
	case int, int64:
		_ = x
		return 1
	case fmt.Stringer:
		return len(x.String())
	default:
		return 3
	}
}

func TypeSwitch2(v any) int {
	switch v.(type) {
	case string:
		return 1
	}
	switch w := v; x := w.(type) {
	case error:
		return len(x.Error())
	}
	return 0
}

func Select(ch chan int, done chan struct{}) (n int) {
	select {
	case ch <- 1:
	case v := <-ch:
		n = v
	case v, ok := <-ch:
		_, n = ok, v
	case n = <-ch:
	case n, _ = <-ch:
	case <-done:
		return
	default:
	}
	select {}
}

func Loops(xs []int, m map[string]int, ch chan int) (n int) {
	for i := 0; i < 10; i++ {
		n += i
	}
	for n < 100 {
		n *= 2
	}
	for {
		break
	}
	for true {
		break
	}
	for i := 0; ; {
		_ = i
		break
	}
	for ; ; n++ {
		if n > 5 {
			break
		}
	}
	for i, j := 0, 10; i < j; i, j = i+1, j-1 {
		n += j - i
	}
	for range xs {
	}
	for i := range xs {
		n += i
	}
	for i, x := range xs {
		n += i * x
	}
	for _, x := range xs {
		n += x
	}
	for k, _ := range m {
		_ = k
	}
	for v := range ch {
		n += v
	}
	for i := range 10 {
		n += i
	}
	var k string
	var v int
	for k, v = range m {
	}
	for k = range m {
	}
	_, _ = k, v
	return
}

func Jumps(xs []int) int {
	n := 0
outer:
	for i := range xs {
		for j := range xs {
			if j > i {
				continue outer
			}
			if j == 5 {
				break outer
			}
			if j == 6 {
				continue
			}
			n++
		}
	}
	goto end
end:
	return n
}

func Empty() {
	goto L
L:
}

func Empty2(b bool) {
	if b {
		goto M
	}
M:
	;
	println()
}

func GoDefer(ch chan int, f func(int)) {
	go f(1)
	defer f(2)
	defer func() {
		recover()
	}()
	go func(x int) {
		ch <- x
	}(3)
	<-ch
	ch <- 4
}

func Return() (int, string) {
	return 1, "a"
}

func Return2() (a int, b string) {
	a, b = Return()
	return
}

func Return3() (int, string) {
	return Return()
}

func Fn(x int) int {
	if x > 0 {
		return x
	}
	return -x
}
