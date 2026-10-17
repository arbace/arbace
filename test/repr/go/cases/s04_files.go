//go:build tamago || linux

//go:debug panicnil=1

// Command files exercises the file header of SPEC §4.3: build lines, header
// directives, the package doc and every kind of import.
package main

import (
	_ "embed"
	. "math"
	r "math/rand"
	"strings"
	"unicode/utf8"
)

var sink int

func main() {
	sink = utf8.RuneLen('a') + int(Sqrt(4)) + r.Intn(1) + int(Floor(Pi))
	sink += len(strings.Repeat("x", 2))
	fn := func(x int) int { return x + 1 }
	sink = fn(sink)
	g(fn)
	len := func(s string) int { return 0 }
	sink += len("abc")
	true := false
	_ = true
	do, when := 1, 2
	sink += do + when
L:
	for {
		break L
	}
}

func g(f func(int) int) {}
