// a64i's Go program (AR20, queue 291; ARS84 (a)): Go's asynchronous preemption: with one P, a goroutine spinning in a loop with no
// call (so no cooperative preemption point) keeps main's timer from running until sysmon preempts it by SIGURG (tgkill to its
// thread; the handler's ctxt.pushCall of asyncPreempt); without signal delivery the program never ends. Deterministic: each
// spinner marks itself started before its loop and main stops them only once all have started (main sleeps until then: its
// sleeps let the first spinner run, and a later one starts only when the running one is preempted)
package main

import (
	"fmt"
	"runtime"
	"sync/atomic"
	"time"
)

var stop atomic.Bool
var started atomic.Int64
var spins atomic.Int64

func spin() {
	started.Add(1)
	n := int64(0)
	for !stop.Load() {
		n++
	}
	spins.Add(n)
}

// await sleeps until k spinners have started
func await(k int64) {
	for started.Load() < k {
		time.Sleep(time.Millisecond)
	}
}

func main() {
	runtime.GOMAXPROCS(1)
	done := make(chan bool)
	go func() { spin(); done <- true }()
	await(1)
	stop.Store(true)
	<-done
	fmt.Println("preempted: the spinner stopped, spun", spins.Load() > 0)
	// a second round: two spinners, the second started only by the first's preemption, main woken by its timer each time
	stop.Store(false)
	started.Store(0)
	for i := 0; i < 2; i++ {
		go func() { spin(); done <- true }()
	}
	await(2)
	stop.Store(true)
	<-done
	<-done
	fmt.Println("preempted twice")
}
