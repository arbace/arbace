//go:build cicover

// The cover build's addition to tools/cirun (bin/ci cover, queue 160; copied beside cirun's main.go only in
// the -cover build, -tags cicover): SIGTERM, timeout(1)'s signal at a program's budget, exits through os.Exit, whose exit hook
// writes the coverage counters to GOCOVERDIR, so a program beyond its budget still leaves the blocks it reached.
package main

import (
	"os"
	"os/signal"
	"syscall"
)

func init() {
	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGTERM)
	go func() {
		<-c
		os.Exit(124)
	}()
}
