// Code (SPEC §7): statements and expressions.
package code

import "fmt"

type P struct{ X, Y int }

type N struct {
	P
	Name string
}

func (p *P) Move(d int) { p.X += d }

func Stmts(xs []int, m map[string]int, ch chan int) (n int, err error) {
	x := 1
	var y int
	var z = 2
	var w int = 3
	const k = 10
	type L struct{ v int }
	_ = L{}
	x, err = 2, nil
	a, err := fmt.Println()
	_ = a
	v, ok := m["b"]
	_, _ = v, ok
	x += y
	x &^= z
	x <<= 2
	x++
	w--
	xs[0] = 1
	xs[0], xs[1] = xs[1], xs[0]
	p := &P{1, 2}
	*p = P{}
	pp := &p.X
	*pp = 3
	{
		q := 1
		_ = q
	}
	if x > 0 {
		x = 0
	}
	if y := x; y > 0 {
		x = 1
	} else {
		x = 2
	}
	if x == 1 {
	} else if x == 2 {
		x = 3
	} else if x == 3 {
	} else {
		x = 4
	}
	if x == 1 {
	} else {
		if y == 2 {
		}
	}
	switch x {
	case 1, 2:
		x = 5
		fallthrough
	default:
		x = 6
	case 3:
	}
	switch q := x; {
	case q > 1:
	}
	var i any = x
	switch t := i.(type) {
	case nil:
	case int, string:
		_ = t
	case fmt.Stringer:
		_ = t.String()
	}
	switch i.(type) {
	}
	select {
	case ch <- 1:
	case r := <-ch:
		_ = r
	case r, ok := <-ch:
		_, _ = r, ok
	case x = <-ch:
	case <-ch:
	default:
	}
	for {
		break
	}
	for x < 10 {
		x++
	}
	for true {
		break
	}
	for i := 0; i < 10; i++ {
		continue
	}
	for ; ; x++ {
		break
	}
	for range xs {
	}
	for i := range xs {
		_ = i
	}
	for _, v := range xs {
		_ = v
	}
	for x = range 10 {
	}
	for k, v := range m {
		_, _ = k, v
	}
outer:
	for {
		for {
			break outer
		}
	}
	goto end
end:
	go func() {}()
	defer fmt.Println("x")
	ch <- 1
	<-ch
	f := func(a int) int { return a * 2 }
	_ = f(1)
	n = x + w + k
	return
}

func Exprs(a, b int, s []byte, str string, p *P, nn N, i any, f func(...int), is []int) bool {
	_ = a + b + 3
	_ = a + (b + 3)
	_ = a - b - 1
	_ = a*b/2%3<<1>>2
	_ = a&b | a ^ b&^a
	_ = -a + +b ^ 1
	_ = !(a < b) && a <= b || a > b && a >= b
	_ = a == b != false
	_ = &a
	_ = *p
	_ = s[1:]
	_ = s[:2]
	_ = s[1:2]
	_ = s[:]
	_ = s[1:2:3]
	_ = s[:2:3]
	_ = str[0]
	_ = p.X
	_ = nn.X
	nn.Move(1)
	_ = nn.Move
	_ = (*P).Move
	_ = fmt.Sprint
	_ = []int{1, 2}
	_ = map[string]int{"a": 1}
	_ = [...]int{3: 1}
	_ = []P{{1, 2}, {X: 3}}
	_ = []*P{{1, 2}}
	_ = N{P: P{}, Name: "n"}
	_ = N{X: 1, Name: "a"}
	_ = i.(int)
	_, ok := i.(string)
	_ = ok
	f(1, 2)
	f(is...)
	_ = append(s, "abc"...)
	_ = len(s) + cap(s)
	_ = make([]int, 1, 2)
	_ = make(map[int]int)
	_ = new(int)
	_ = new(42)
	_ = complex(1, 2)
	var u uint8 = 1 << a
	_ = u
	_ = min(a, b)
	_ = int64(a)
	_ = (*int)(nil)
	_ = func() {}
	return true
}

func Names() int {
	fn := func(x int) {}
	fn(1)
	len := func() int { return 0 }
	return len()
}
