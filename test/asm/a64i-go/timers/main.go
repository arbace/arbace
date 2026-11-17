// queue 288: time: Sleep, Since, a Timer, a Ticker, time.After in select, a context's deadline
package main

import (
	"context"
	"fmt"
	"time"
)

func main() {
	t0 := time.Now()
	time.Sleep(20 * time.Millisecond)
	fmt.Println("slept", time.Since(t0) >= 20*time.Millisecond)

	tm := time.NewTimer(10 * time.Millisecond)
	<-tm.C
	fmt.Println("timer fired")

	tk := time.NewTicker(5 * time.Millisecond)
	n := 0
	for range tk.C {
		n++
		if n == 5 {
			break
		}
	}
	tk.Stop()
	fmt.Println("ticks", n)

	select {
	case <-time.After(15 * time.Millisecond):
		fmt.Println("after")
	case <-make(chan int):
		fmt.Println("never")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Millisecond)
	defer cancel()
	<-ctx.Done()
	fmt.Println("context", ctx.Err())

	d, _ := time.ParseDuration("1h2m3.5s")
	fmt.Println("duration", d, time.Date(2026, 11, 14, 3, 0, 0, 0, time.UTC).Weekday())
	fmt.Println("elapsed ok", time.Since(t0) >= 75*time.Millisecond, time.Since(t0) < 30*time.Second)
}
