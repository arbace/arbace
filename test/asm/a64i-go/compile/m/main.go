// Command m is the compile directory's program (AR23, queue 295): r's report and p's functions (Go and arm64 assembly), linked by
// cmd/link on a64i.
package main

import (
	"fmt"
	"os"

	"a64ic/p"
	"a64ic/r"
)

func main() {
	for _, n := range []int{3, 10, 26} {
		k, s, xs, t := r.Report(n)
		fmt.Printf("report %d: %d keys, sum %g, %v, %d\n", n, k, s, xs, t)
	}
	fmt.Println("add:", p.Add(40, 2))
	if len(os.Args) > 1 {
		fmt.Fprintln(os.Stderr, "m: no arguments wanted:", os.Args[1:])
		os.Exit(3)
	}
}
