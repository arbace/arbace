// a64i's Go program (AR20, queue 291; ARS84 (a)): runtime errors raised by faults and recovered: a nil pointer's load and store
// (SIGSEGV at address 0 and 8, Go's sigpanic), a call of a nil func (the fetch at 0), each recovered as Go's runtime error, in the
// main goroutine and in another one
package main

import (
	"fmt"
	"sync"
)

type T struct {
	a, b int
}

//go:noinline
func load(p *T) int { return p.b }

//go:noinline
func store(p *T) { p.a = 7 }

//go:noinline
func call(f func() int) int { return f() }

func try(name string, f func()) {
	defer func() {
		if e := recover(); e != nil {
			fmt.Printf("%s: recovered: %v\n", name, e)
		}
	}()
	f()
	fmt.Printf("%s: no panic\n", name)
}

func main() {
	var p *T
	try("load", func() { fmt.Println(load(p)) })
	try("store", func() { store(p) })
	try("call", func() { fmt.Println(call(nil)) })
	q := &T{1, 2}
	try("valid", func() { fmt.Println(load(q)) })
	var wg sync.WaitGroup
	got := make([]string, 4)
	for i := 0; i < 4; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			defer func() { got[i] = fmt.Sprint("goroutine ", i, ": recovered: ", recover()) }()
			store(p)
		}(i)
	}
	wg.Wait()
	for _, s := range got {
		fmt.Println(s)
	}
	n := 0
	for i := 0; i < 100; i++ {
		func() {
			defer func() {
				if recover() != nil {
					n++
				}
			}()
			load(p)
		}()
	}
	fmt.Println("recovered", n, "times")
}
