// Goroutines on wasip1's one thread: a pipeline of channels, a WaitGroup, a mutex and select with a default (WA5's own programs).
package main

import (
	"fmt"
	"sync"
)

func main() {
	src := make(chan int)
	sq := make(chan int)
	go func() {
		for i := 1; i <= 100; i++ {
			src <- i
		}
		close(src)
	}()
	go func() {
		for x := range src {
			sq <- x * x
		}
		close(sq)
	}()
	sum := 0
	for x := range sq {
		sum += x
	}
	fmt.Println("sum of squares", sum)

	var wg sync.WaitGroup
	var mu sync.Mutex
	total := 0
	for i := 0; i < 50; i++ {
		wg.Add(1)
		go func(n int) {
			defer wg.Done()
			mu.Lock()
			total += n
			mu.Unlock()
		}(i)
	}
	wg.Wait()
	fmt.Println("total", total)

	c := make(chan string, 1)
	select {
	case s := <-c:
		fmt.Println("got", s)
	default:
		fmt.Println("nothing ready")
	}
	c <- "x"
	select {
	case s := <-c:
		fmt.Println("got", s)
	default:
		fmt.Println("nothing ready")
	}
	done := make(chan bool)
	go func() { panicked := func() (r any) { defer func() { r = recover() }(); panic("inner"); }(); fmt.Println("recovered", panicked); done <- true }()
	<-done
}
