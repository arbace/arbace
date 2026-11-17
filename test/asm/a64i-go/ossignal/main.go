// a64i's Go program (AR20, queue 291): os/signal: SIGUSR1 and SIGUSR2 sent to the process by syscall.Kill and by tgkill to the main
// thread, received on Notify's channel (Go's handler queues them, the signal_recv goroutine woken), then Stop and Ignore (SIG_IGN:
// a signal sent then is dropped), Reset; a SIGTERM caught by Notify
package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	c := make(chan os.Signal, 4)
	signal.Notify(c, syscall.SIGUSR1, syscall.SIGUSR2, syscall.SIGTERM)
	pid := os.Getpid()
	syscall.Kill(pid, syscall.SIGUSR1)
	fmt.Println("got", <-c)
	syscall.Tgkill(pid, syscall.Gettid(), syscall.SIGUSR2)
	fmt.Println("got", <-c)
	p, _ := os.FindProcess(pid)
	p.Signal(syscall.SIGTERM)
	fmt.Println("got", <-c)
	signal.Stop(c)
	signal.Ignore(syscall.SIGUSR1)
	syscall.Kill(pid, syscall.SIGUSR1)
	select {
	case s := <-c:
		fmt.Println("unexpected", s)
	case <-time.After(20 * time.Millisecond):
		fmt.Println("ignored: none received")
	}
	fmt.Println("ignored:", signal.Ignored(syscall.SIGUSR1))
	signal.Reset(syscall.SIGUSR1)
	fmt.Println("after reset, ignored:", signal.Ignored(syscall.SIGUSR1))
}
