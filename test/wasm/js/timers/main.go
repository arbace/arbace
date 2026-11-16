//go:build js && wasm

// timers: Go's timers (time.Sleep, time.AfterFunc, a ticker) and JavaScript's setTimeout on the world's event loop, in the order
// their delays give; the monotonic clock never runs back.
package main

import (
	"fmt"
	"sync"
	"syscall/js"
	"time"
)

func main() {
	start := time.Now()
	var mu sync.Mutex
	var order []string
	add := func(s string) { mu.Lock(); order = append(order, s); mu.Unlock() }
	var wg sync.WaitGroup
	wg.Add(4)
	time.AfterFunc(30*time.Millisecond, func() { add("after 30ms"); wg.Done() })
	go func() { time.Sleep(10 * time.Millisecond); add("sleep 10ms"); wg.Done() }()
	var cb js.Func
	cb = js.FuncOf(func(this js.Value, args []js.Value) any {
		add(fmt.Sprint("setTimeout 20ms ", args[0].String()))
		cb.Release()
		wg.Done()
		return nil
	})
	js.Global().Call("setTimeout", cb, 20, "x")
	go func() {
		t := time.NewTicker(5 * time.Millisecond)
		for i := 0; i < 3; i++ {
			<-t.C
		}
		t.Stop()
		add("ticked 3")
		wg.Done()
	}()
	wg.Wait()
	for _, s := range order {
		fmt.Println(s)
	}
	el := time.Since(start)
	fmt.Println("elapsed at least 30ms:", el >= 30*time.Millisecond, "under a second:", el < time.Second)
}
