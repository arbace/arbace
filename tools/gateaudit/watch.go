// The reducer's watchdog (GT7's fix round, doc/GATE-SURVEY.md, "As built: GT7's fix round"). GT7's
// clean --full ran repr-go under the audit for 3.5 hours with its processes frozen: the reducer had
// stopped reading strace's FIFO, strace blocked on the full FIFO, every traced process blocked on
// strace. Nothing noticed. The watchdog is a process of its own (this program again, -watch), started
// by a reducer that reads a FIFO, in a process group of its own (no stop of the step's group reaches
// it), that every GATE_AUDIT_POLL_S seconds (30) looks at the reducer's heartbeat (FIFO.progress) and
// calls it stalled when, for GATE_AUDIT_STALL_S seconds (600):
//
//   - its heartbeat is stale (the reducer does not run: stopped, or its runtime wedged), or
//   - bytes wait in the FIFO and none is read (the reader does not read), or
//   - the reducer is gone while a writer (strace) still holds the FIFO (it died before strace ended:
//     strace would go on, its trace lost, its log filling with EPIPE).
//
// A stall fails the step loudly instead of hanging: the reason in NAME.reduce.stalled (beside the
// FIFO, NAME.fifo) and appended to the step's log (NAME.log), the writer's process tree (strace and
// the step's traced processes) and the reducer killed (SIGKILL, by PID). The step then ends with 137;
// bin/gate's summary and the clean record name the stall. The watchdog exits when the reducer does.
package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// envSecs: an environment variable's seconds, else def
func envSecs(name string, def int64) int64 {
	if v, err := strconv.ParseInt(os.Getenv(name), 10, 64); err == nil && v > 0 {
		return v
	}
	return def
}

// startTime: a process's start time (/proc/PID/stat's 22nd field), "" when it is gone
func startTime(pid int) string {
	b, err := os.ReadFile("/proc/" + strconv.Itoa(pid) + "/stat")
	if err != nil {
		return ""
	}
	s := string(b)
	i := strings.LastIndexByte(s, ')')
	if i < 0 {
		return ""
	}
	f := strings.Fields(s[i+1:])
	if len(f) < 20 {
		return ""
	}
	return f[19]
}

// ppids: every process's parent
func ppids() map[int]int {
	m := map[int]int{}
	ents, _ := os.ReadDir("/proc")
	for _, e := range ents {
		pid, err := strconv.Atoi(e.Name())
		if err != nil {
			continue
		}
		b, err := os.ReadFile("/proc/" + e.Name() + "/stat")
		if err != nil {
			continue
		}
		s := string(b)
		i := strings.LastIndexByte(s, ')')
		if i < 0 {
			continue
		}
		f := strings.Fields(s[i+1:])
		if len(f) < 2 {
			continue
		}
		if pp, err := strconv.Atoi(f[1]); err == nil {
			m[pid] = pp
		}
	}
	return m
}

// holders: the processes but skip that hold path open (readlink of /proc/PID/fd/N, no open: the
// watchdog never opens the FIFO)
func holders(path string, skip int) []int {
	var out []int
	ents, _ := os.ReadDir("/proc")
	for _, e := range ents {
		pid, err := strconv.Atoi(e.Name())
		if err != nil || pid == skip || pid == os.Getpid() {
			continue
		}
		fds, err := os.ReadDir("/proc/" + e.Name() + "/fd")
		if err != nil {
			continue
		}
		for _, fd := range fds {
			if l, err := os.Readlink("/proc/" + e.Name() + "/fd/" + fd.Name()); err == nil && l == path {
				out = append(out, pid)
				break
			}
		}
	}
	return out
}

// killTree: the roots and their descendants SIGKILLed, the descendants first (a tracee dies before its
// tracer can detach it), three passes for what forked meanwhile
func killTree(roots []int, also int) {
	for pass := 0; pass < 3; pass++ {
		pp := ppids()
		kids := map[int][]int{}
		for c, p := range pp {
			kids[p] = append(kids[p], c)
		}
		var order []int
		var walk func(int)
		walk = func(p int) {
			for _, c := range kids[p] {
				walk(c)
			}
			order = append(order, p)
		}
		for _, r := range roots {
			walk(r)
		}
		for _, p := range order {
			if p != os.Getpid() {
				syscall.Kill(p, syscall.SIGKILL)
			}
		}
		time.Sleep(200 * time.Millisecond)
	}
	if also > 0 {
		syscall.Kill(also, syscall.SIGKILL)
	}
}

// startWatch: the watchdog of this reducer of fifo, in a process group of its own; its output on the
// reducer's stderr. GATE_AUDIT_WATCH=0: none
func startWatch(fifo, progress string) {
	if os.Getenv("GATE_AUDIT_WATCH") == "0" {
		return
	}
	self, err := os.Executable()
	if err != nil {
		fmt.Fprintln(os.Stderr, "gateaudit: no watchdog:", err)
		return
	}
	c := exec.Command(self, "-watch", strconv.Itoa(os.Getpid()), "-progress", progress, fifo)
	c.Stdout, c.Stderr = os.Stderr, os.Stderr
	c.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
	if err := c.Start(); err != nil {
		fmt.Fprintln(os.Stderr, "gateaudit: no watchdog:", err)
		return
	}
	go c.Wait()
}

// watch: the watchdog's loop (above)
func watch(red int, fifo, progress string) {
	lim := envSecs("GATE_AUDIT_STALL_S", 600)
	poll := time.Duration(envSecs("GATE_AUDIT_POLL_S", 30)) * time.Second
	if abs, err := filepath.Abs(fifo); err == nil {
		fifo = abs
	}
	if p, err := filepath.EvalSymlinks(fifo); err == nil {
		fifo = p
	}
	st0 := startTime(red)
	base := strings.TrimSuffix(fifo, ".fifo")
	var lastBytes int64 = -1
	since := time.Now().Unix()
	gone := 0
	for {
		time.Sleep(poll)
		now := time.Now().Unix()
		why := ""
		if startTime(red) != st0 || st0 == "" {
			// the reducer ended; a writer that still holds the FIFO is strace tracing on without it
			// (twice in a row: strace closes the FIFO as it exits, a moment after the reducer's EOF)
			if w := holders(fifo, red); len(w) > 0 {
				if gone++; gone >= 2 {
					why = "the reducer exited while strace still writes its trace (its stderr: " + base + ".reduce.err)"
				}
			} else {
				return
			}
		} else if b, err := os.ReadFile(progress); err == nil {
			var t, n, lines, pend int64
			fmt.Sscan(string(b), &t, &n, &lines, &pend)
			if n != lastBytes {
				lastBytes, since = n, now
			}
			switch {
			case now-t >= lim:
				why = fmt.Sprintf("its heartbeat is %d s old (the reducer does not run)", now-t)
			case pend > 0 && now-since >= lim:
				why = fmt.Sprintf("%d bytes wait in the FIFO and none was read for %d s (%d bytes, %d lines read)", pend, now-since, n, lines)
			}
		} else if now-since >= lim {
			why = fmt.Sprintf("no heartbeat (%s) for %d s", progress, now-since)
		}
		if why == "" {
			continue
		}
		msg := fmt.Sprintf("gate: AUDIT STALLED: the reducer of %s: %s; strace and the step's processes killed (GT7's fix round's watchdog, GATE_AUDIT_STALL_S=%d)", fifo, why, lim)
		fmt.Fprintln(os.Stderr, msg)
		os.WriteFile(base+".reduce.stalled", []byte(why+"\n"), 0o644)
		if f, err := os.OpenFile(base+".log", os.O_WRONLY|os.O_APPEND, 0); err == nil {
			fmt.Fprintln(f, msg)
			f.Close()
		}
		alive := red
		if startTime(red) != st0 {
			alive = 0
		}
		killTree(holders(fifo, red), alive)
		return
	}
}
