// Time on WASI: clock_time_get's two clocks, sleeps and timers through poll_oneoff, a ticker, a timeout by select (WA5's own
// programs). The output states only what holds on every run.
package main

import (
	"fmt"
	"time"
)

func main() {
	t0 := time.Now()
	time.Sleep(20 * time.Millisecond)
	d := time.Since(t0)
	fmt.Println("slept at least 20ms:", d >= 20*time.Millisecond, "under 5s:", d < 5*time.Second)
	fmt.Println("year after 2025:", time.Now().Year() > 2025)
	tm := time.NewTimer(10 * time.Millisecond)
	select {
	case <-tm.C:
		fmt.Println("timer fired")
	case <-time.After(5 * time.Second):
		fmt.Println("timer late")
	}
	tk := time.NewTicker(5 * time.Millisecond)
	n := 0
	for range tk.C {
		n++
		if n == 3 {
			tk.Stop()
			break
		}
	}
	fmt.Println("ticks", n)
	c := make(chan int)
	go func() { time.Sleep(5 * time.Millisecond); c <- 7 }()
	select {
	case v := <-c:
		fmt.Println("received", v)
	case <-time.After(5 * time.Second):
		fmt.Println("timeout")
	}
	u := time.Date(2026, 11, 13, 12, 30, 0, 0, time.UTC)
	fmt.Println(u.Format(time.RFC3339), u.Weekday(), u.Add(36*time.Hour).Format("2006-01-02 15:04"))
}
