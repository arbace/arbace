// queue 288: goroutines, channels, a WaitGroup, a Mutex, select, runtime.Gosched and GC
package main

import (
	"fmt"
	"runtime"
	"sync"
)

func main() {
	var wg sync.WaitGroup
	var mu sync.Mutex
	total := 0
	results := make([]int, 16)
	for i := 0; i < 16; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			s := 0
			for k := 0; k <= 1000*(i+1); k++ {
				s += k
				if k%500 == 0 {
					runtime.Gosched()
				}
			}
			results[i] = s
			mu.Lock()
			total += s
			mu.Unlock()
		}(i)
	}
	wg.Wait()
	fmt.Println("results", results[0], results[7], results[15], "total", total)

	ch := make(chan int)
	done := make(chan struct{})
	go func() {
		for i := 1; i <= 100; i++ {
			ch <- i
		}
		close(ch)
	}()
	sum := 0
	go func() {
		for v := range ch {
			sum += v
		}
		close(done)
	}()
	<-done
	fmt.Println("channel sum", sum)

	a, b := make(chan string), make(chan string)
	go func() { a <- "a" }()
	go func() { b <- "b" }()
	got := map[string]bool{}
	for len(got) < 2 {
		select {
		case s := <-a:
			got[s] = true
		case s := <-b:
			got[s] = true
		}
	}
	fmt.Println("select", got["a"], got["b"])

	var bufs [][]byte
	for i := 0; i < 2000; i++ {
		bufs = append(bufs, make([]byte, 1024))
	}
	runtime.GC()
	var ms runtime.MemStats
	runtime.ReadMemStats(&ms)
	fmt.Println("gc", ms.NumGC > 0, len(bufs))

	defer func() { fmt.Println("recovered", recover()) }()
	var m map[string]int
	m["x"] = 1
}
