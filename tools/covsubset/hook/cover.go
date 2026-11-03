//go:build arbacecover

// The cover build's hook (doc/GATE-SURVEY.md, GT8.3, GTS206 (a); bin/arbace-go --gate-build --cover): added
// to the main package arbace/cmd/arbace of the cover executables by an -overlay entry and built with
// -tags arbacecover, never part of a shipped build. Go writes a covered program's counters when it exits
// (os.Exit or main's return), not when a signal kills it; a check's limits end a program with SIGTERM (the wall
// cap, timeout) or SIGKILL (the CPU limit, ulimit -t). So, when GOCOVERDIR is set: on SIGTERM or SIGHUP the
// counters are written and the signal raised again (the status a check reads stays the signal's), and every
// ARBACE_COVER_FLUSH seconds (default 20; 0: never) the counters so far are written, so a program killed at its
// CPU limit still leaves the blocks it reached up to the last flush. Each write is a counter file of its own
// (covcounters.HASH.PID.TIME); `go tool covdata textfmt` merges them. After tools/cicover/cirun/cover.go (queue
// 160, which exits 124 instead).
package main

import (
	"os"
	"os/signal"
	"runtime/coverage"
	"strconv"
	"syscall"
	"time"
)

func init() {
	d := os.Getenv("GOCOVERDIR")
	if d == "" {
		return
	}
	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGTERM, syscall.SIGHUP)
	go func() {
		s := <-c
		coverage.WriteCountersDir(d)
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
				coverage.WriteCountersDir(d)
			}
		}()
	}
}
