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
package cfile

import (
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"
)

func init() {
	arbaceCoverHook()
}

func arbaceCoverHook() {
	d := os.Getenv("GOCOVERDIR")
	if d == "" {
		return
	}
	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGTERM, syscall.SIGHUP, syscall.SIGXCPU)
	go func() {
		s := <-c
		emitCounterDataToDirectory(d)
		signal.Reset(s)
		syscall.Kill(os.Getpid(), s.(syscall.Signal))
		time.Sleep(time.Second)
		os.Exit(128 + int(s.(syscall.Signal)))
	}()
	secs := 20
	if v, err := strconv.Atoi(os.Getenv("ARBACE_COVER_FLUSH")); err == nil {
		secs = v
	}
	if secs > 0 {
		go func() {
			for range time.Tick(time.Duration(secs) * time.Second) {
				emitCounterDataToDirectory(d)
			}
		}()
	}
}
