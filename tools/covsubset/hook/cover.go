//go:build arbacecover

// The cover build's hook (doc/GATE-SURVEY.md, GT8.3, GTS206 (a); bin/arbace-go --gate-build --cover): added
// to Go's internal/coverage/cfile (the package that writes a covered program's data, linked into every cover
// build) by an -overlay entry of the cover builds alone (bin/lib/cover.bash's cover_go_flags), built with
// -tags arbacecover, never part of a shipped build. Not in the main package: the main package is covered (Go
// initializes the output only from a covered main) and the cover tool reads its files without the overlay. Go writes a covered program's counters when it exits
// (os.Exit or main's return), not when a signal kills it, nor on a panic's exit; a check's limits end a program
// with SIGTERM (the wall cap, timeout) or, under coverage, SIGXCPU (the CPU limit's soft limit, 5 s below the
// hard one's SIGKILL: bin/lib/limit.bash's limit_cpu_cmd). So, when GOCOVERDIR is set: on SIGTERM, SIGHUP or
// SIGXCPU the counters are written and the signal raised again (the status a check reads stays the signal's:
// 124 under timeout, 152), and every
// ARBACE_COVER_FLUSH seconds (default 20; 0: never) the counters so far are written, so a program killed at its
// CPU limit still leaves the blocks it reached up to the last flush. Each write is a counter file of its own
// (covcounters.HASH.PID.TIME); `go tool covdata textfmt` merges them. After tools/cicover/cirun/cover.go (queue
// 160, which exits 124 instead).
//
// A program's own signal handling stays its own (review 182 L3): a signal the program receives on a channel of its
// own (os/signal's count, tools/covsubset/hook/signal/refs.go's ArbaceCoverRefs, the second overlay file) has the
// counters written and is then left to the program, neither raised again nor its handler reset (signal.Stop of the
// hook's channel alone, never signal.Reset); a write in progress holds the program's exit (an exit hook) so that no
// tmp.covcounters file is left; and a signal ignored at the start (SIGHUP under nohup) is not caught, so it stays
// ignored.
package cfile

import (
	"internal/runtime/exithook"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"syscall"
	"time"
)

// arbaceEmitMu: one write of the hook's at a time, and the program's exit waits for it
var arbaceEmitMu sync.Mutex

func arbaceEmit(d string) {
	arbaceEmitMu.Lock()
	defer arbaceEmitMu.Unlock()
	emitCounterDataToDirectory(d)
}

func init() {
	arbaceCoverHook()
}

func arbaceCoverHook() {
	d := os.Getenv("GOCOVERDIR")
	if d == "" {
		return
	}
	// an exit (os.Exit, main's return: Go's own counter hook has run by then, registered after this one) waits for the
	// hook's write in progress, never released: the process is exiting
	exithook.Add(exithook.Hook{F: func() { arbaceEmitMu.Lock() }, RunOnFailure: true})
	var sigs []os.Signal
	for _, s := range []os.Signal{syscall.SIGTERM, syscall.SIGHUP, syscall.SIGXCPU} {
		if !signal.Ignored(s) {
			sigs = append(sigs, s)
		}
	}
	if len(sigs) > 0 {
		c := make(chan os.Signal, 1)
		signal.Notify(c, sigs...)
		go func() {
			for s := range c {
				arbaceEmit(d)
				if signal.ArbaceCoverRefs(s) > 1 {
					continue // the program's own handler has it too
				}
				signal.Stop(c) // the hook's alone: the signal's default action back (no other channel wants it)
				syscall.Kill(os.Getpid(), s.(syscall.Signal))
				time.Sleep(time.Second)
				os.Exit(128 + int(s.(syscall.Signal)))
			}
		}()
	}
	secs := 20
	if v, err := strconv.Atoi(os.Getenv("ARBACE_COVER_FLUSH")); err == nil {
		secs = v
	}
	if secs > 0 {
		go func() {
			for range time.Tick(time.Duration(secs) * time.Second) {
				arbaceEmit(d)
			}
		}()
	}
}
