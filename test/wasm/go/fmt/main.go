// fmt and strconv: verbs, widths, floats in every format, quoting, a Stringer, errors wrapped (WA5's own programs).
package main

import (
	"errors"
	"fmt"
	"math"
	"strconv"
	"strings"
)

type point struct{ X, Y int }

func (p point) String() string { return fmt.Sprintf("(%d,%d)", p.X, p.Y) }

func main() {
	fmt.Printf("%5d|%-5d|%05d|%x|%X|%o|%b\n", 42, 42, 42, 255, 255, 8, 5)
	fmt.Printf("%.3f|%e|%g|%G|%8.2f\n", math.Pi, 123456.789, 1e21, 1e-7, -2.5)
	fmt.Printf("%q|%v|%+v|%#v|%T\n", "a\tb", point{1, 2}, struct{ A int }{3}, []int{1, 2}, map[string]int{})
	fmt.Println(strconv.FormatFloat(0.1+0.2, 'g', -1, 64), strconv.Quote("héllo\n"), strconv.Itoa(-99))
	f, _ := strconv.ParseFloat("6.02214076e23", 64)
	fmt.Println(f, math.Inf(1), -math.Inf(1), math.NaN(), float32(1)/3)
	base := errors.New("base")
	err := fmt.Errorf("wrapped: %w", base)
	fmt.Println(err, errors.Is(err, base), errors.Unwrap(err) == base)
	var b strings.Builder
	for i := 0; i < 3; i++ {
		fmt.Fprintf(&b, "[%d]", i)
	}
	fmt.Println(b.String(), strings.Repeat("ab", 3), strings.ToUpper("wasm"))
}
