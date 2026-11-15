//go:build arbacecover

// The cover build's second hook file (doc/GATE-SURVEY.md, GT8.3; bin/lib/cover.bash's cover_go_flags): added to Go's
// os/signal by an -overlay entry of the cover builds alone, built with -tags arbacecover, never part of a shipped
// build. It tells tools/covsubset/hook/cover.go (internal/coverage/cfile's hook) whether the program has signal
// handlers of its own (review 182 L3): os/signal keeps no other account of them that another package can read.
//
// And whether the program's handlers received a given delivery (gate 13d's hang, 2026-11-15): the number of channels
// wanting a signal when the hook reads it is not the number its delivery was sent to. A SIGTERM early in the program's
// start reached the hook's channel alone; the program's signal.Notify came before the hook read the count, the hook
// took the signal for the program's and left it, and the program waited for it forever. So the signal loop is this
// file's (watchSignalLoop, set by an init after signal_unix.go's: the go command orders a package's files by name, and
// the overlay's zz_arbace_cover.go is the last), its delivery process's (signal.go) with the channels it sent to counted
// under the same lock (linux, the cover build's one target: signal_unix.go's signal_recv).
package signal

import (
	"os"
	"sync/atomic"
	"syscall"
)

// ArbaceCoverRefs: the number of channels that receive sig (signal.Notify's registrations not stopped, the hook's
// own among them); with sig nil, the number of registrations of every signal summed.
func ArbaceCoverRefs(sig os.Signal) int64 {
	handlers.Lock()
	defer handlers.Unlock()
	if sig == nil {
		var n int64
		for _, r := range handlers.ref {
			n += r
		}
		return n
	}
	n := signum(sig)
	if n < 0 {
		return 0
	}
	return handlers.ref[n]
}

// arbaceDelivered: per signal, the number of registered channels (stopping ones not counted) its latest delivery was
// sent to; arbaceLooping: the loop below is os/signal's
var (
	arbaceDelivered [numSig]atomic.Int64
	arbaceLooping   atomic.Bool
)

// ArbaceCoverDelivered: the number of channels (the hook's among them) the latest delivery of sig was sent to, read
// once that delivery is done; -1 when this file's loop does not deliver (the caller then reads ArbaceCoverRefs)
func ArbaceCoverDelivered(sig os.Signal) int64 {
	handlers.Lock()
	defer handlers.Unlock()
	n := signum(sig)
	if n < 0 || !arbaceLooping.Load() {
		return -1
	}
	return arbaceDelivered[n].Load()
}

func init() {
	watchSignalLoop = arbaceLoop
}

func arbaceLoop() {
	arbaceLooping.Store(true)
	for {
		arbaceProcess(syscall.Signal(signal_recv()))
	}
}

// arbaceProcess: process (signal.go) with the channels sent to counted under its lock
func arbaceProcess(sig os.Signal) {
	n := signum(sig)
	if n < 0 {
		return
	}
	handlers.Lock()
	defer handlers.Unlock()
	var k int64
	for c, h := range handlers.m {
		if h.want(n) {
			k++
			select {
			case c <- sig:
			default:
			}
		}
	}
	for _, d := range handlers.stopping {
		if d.h.want(n) {
			select {
			case d.c <- sig:
			default:
			}
		}
	}
	arbaceDelivered[n].Store(k)
}
