// a64i's Go program (AR20, queue 291): channels with select (a fan-in of three producers, a quit channel, a default case, a closed
// channel's zero value), a panicking goroutine recovered and its error sent back, a runtime error (index out of range, division by
// zero: SIGFPE-free on arm64, the compiler's check) recovered, and a timeout by time.After
package main

import (
	"errors"
	"fmt"
	"sort"
	"time"
)

func producer(id int, out chan<- int, quit <-chan bool) {
	for {
		select {
		case out <- id:
		case <-quit:
			return
		}
	}
}

func safe(f func()) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("recovered: %v", r)
		}
	}()
	f()
	return nil
}

func main() {
	out := make(chan int)
	quit := make(chan bool)
	for id := 1; id <= 3; id++ {
		go producer(id, out, quit)
	}
	got := map[int]int{}
	for len(got) < 3 || got[1] < 5 || got[2] < 5 || got[3] < 5 {
		v := <-out
		got[v]++
	}
	close(quit)
	keys := []int{}
	for k := range got {
		keys = append(keys, k)
	}
	sort.Ints(keys)
	fmt.Println("producers heard:", keys)

	errc := make(chan error)
	go func() {
		errc <- safe(func() { panic(errors.New("goroutine's panic")) })
	}()
	fmt.Println(<-errc)
	var a []int
	i := 3
	fmt.Println(safe(func() { _ = a[i] }))
	z := 0
	fmt.Println(safe(func() { _ = 10 / z }))

	c := make(chan int)
	close(c)
	v, ok := <-c
	fmt.Println("closed:", v, ok)
	select {
	case <-make(chan int):
		fmt.Println("unexpected")
	default:
		fmt.Println("default taken")
	}
	select {
	case <-make(chan int):
		fmt.Println("unexpected")
	case <-time.After(5 * time.Millisecond):
		fmt.Println("timed out")
	}
}
