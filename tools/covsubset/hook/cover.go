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
//
// A deadlock is still one (review 182 L2): the hook's os/signal thread keeps Go's own detector (runtime.checkdead)
// from firing, as a flush timer would (so the flush is a goroutine sleeping in nanosleep, no runtime timer), and a
// deadlocked program would wait for the check's wall cap. The hook detects it as checkdead does, once a second
// (ARBACE_COVER_DEADLOCK=0: not): no runtime timer pending (runtime.timeSleepUntil, which the third overlay file,
// tools/covsubset/hook/runtime/timers.go, pushes to it), no signal of the program's own (with one, os/signal's thread
// keeps Go's detector off too), no goroutine running or runnable but this one (the scheduler's counts, runtime/metrics),
// and then, in all the goroutines' traces (runtime.Stack), every goroutine but the hook's and os/signal's blocked in a
// channel, select or sync wait (not IO wait, a syscall or sleep); then it prints Go's message and those goroutines and
// exits 2, as Go's fatal error does (Go's own exit hook writing the counters).
package cfile

import (
	"internal/runtime/exithook"
	"os"
	"os/signal"
	"runtime"
	"runtime/metrics"
	"strconv"
	"strings"
	"sync"
	"syscall"
	"time"
	_ "unsafe" // go:linkname
)

//go:linkname arbaceTimeSleepUntil runtime.arbaceCoverTimeSleepUntil
func arbaceTimeSleepUntil() int64

// the wait reasons of a goroutine only another goroutine can wake (runtime2.go's waitReasonStrings)
var arbaceBlocked = map[string]bool{
	"chan receive (nil chan)": true, "chan send (nil chan)": true, "select (no cases)": true,
	"chan receive": true, "chan send": true, "select": true, "semacquire": true, "sync.Cond.Wait": true,
	"sync.Mutex.Lock": true, "sync.RWMutex.RLock": true, "sync.RWMutex.Lock": true, "sync.WaitGroup.Wait": true,
}

// arbaceDeadlock: the program's goroutines' traces when it is deadlocked as runtime.checkdead would say (OWN: the
// hook's signal registrations), else ""
func arbaceDeadlock(own int64) string {
	if arbaceTimeSleepUntil() != 1<<63-1 || signal.ArbaceCoverRefs(nil) > own {
		return ""
	}
	// a cheap look first (the scheduler's counts, no stop of the world): a goroutine running beside this one, or
	// runnable, is no deadlock
	ms := []metrics.Sample{{Name: "/sched/goroutines/running:goroutines"}, {Name: "/sched/goroutines/runnable:goroutines"}}
	metrics.Read(ms)
	if ms[0].Value.Kind() == metrics.KindUint64 && (ms[0].Value.Uint64() > 1 || ms[1].Value.Uint64() > 0) {
		return ""
	}
	buf := make([]byte, 1<<16)
	for {
		n := runtime.Stack(buf, true)
		if n < len(buf) {
			buf = buf[:n]
			break
		}
		buf = make([]byte, 2*len(buf))
	}
	var gs []string
	for _, g := range strings.Split(strings.TrimRight(string(buf), "\n"), "\n\n") {
		if strings.Contains(g, "internal/coverage/cfile.arbaceCoverHook") || strings.Contains(g, "os/signal.signal_recv") {
			continue
		}
		hdr, _, _ := strings.Cut(g, "\n")
		i, j := strings.Index(hdr, "["), strings.LastIndex(hdr, "]")
		if i < 0 || j < i {
			return ""
		}
		if st, _, _ := strings.Cut(hdr[i+1:j], ","); !arbaceBlocked[st] {
			return ""
		}
		gs = append(gs, g)
	}
	return strings.Join(gs, "\n\n")
}

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
	dl := os.Getenv("ARBACE_COVER_DEADLOCK") != "0"
	if secs > 0 || dl {
		go func() {
			for t := 1; ; t++ {
				ts := syscall.Timespec{Sec: 1}
				syscall.Nanosleep(&ts, nil) // not time.Sleep: a runtime timer would hide a deadlock
				if secs > 0 && t%secs == 0 {
					arbaceEmit(d)
				}
				if dl {
					if gs := arbaceDeadlock(int64(len(sigs))); gs != "" {
						os.Stderr.WriteString("fatal error: all goroutines are asleep - deadlock!\n\n" + gs + "\n")
						os.Exit(2) // Go's counter hook writes the counters
					}
				}
			}
		}()
	}
}
