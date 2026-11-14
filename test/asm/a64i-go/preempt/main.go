// a64i's Go program (AR20, queue 291; ARS84 (a)): Go's asynchronous preemption: with one P, a goroutine spinning in a loop with no
// call (so no cooperative preemption point) keeps main's timer from running until sysmon preempts it by SIGURG (tgkill to its
// thread; the handler's ctxt.pushCall of asyncPreempt); without signal delivery the program never ends
package main

import (
	"fmt"
	"runtime"
	"sync/atomic"
	"time"
)

var stop atomic.Bool
var spins atomic.Int64

func spin() {
	n := int64(0)
	for !stop.Load() {
		n++
	}
	spins.Store(n)
}

func main() {
	runtime.GOMAXPROCS(1)
	done := make(chan bool)
	go func() { spin(); done <- true }()
	time.Sleep(time.Millisecond)
	stop.Store(true)
	<-done
	fmt.Println("preempted: the spinner stopped, spun", spins.Load() > 0)
	// a second round: two spinners, main woken by its timer each time
	stop.Store(false)
	for i := 0; i < 2; i++ {
		go func() { spin(); done <- true }()
	}
	time.Sleep(2 * time.Millisecond)
	stop.Store(true)
	<-done
	<-done
	fmt.Println("preempted twice")
}
