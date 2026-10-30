// Command gateaudit is bin/gate --audit's reducer (doc/GATE-SURVEY.md, GT4, GTS30 (a), "As built:
// GT5"): strace's output (-f -y -s 4096; the calls open, openat, openat2, execve, execveat, readlink,
// readlinkat, chdir, clone, clone3, fork, vfork, and with GATE_AUDIT_TRACE=%file the rest of %file),
// read as it is written, reduced to the distinct "OP<TAB>PATH" lines of the paths the traced processes
// touched, absolute and normalized (. and .. resolved; a relative path against its process's
// directory, the checkout's root until a chdir; a child starts in its parent's):
//
//	X an execve        R an open for reading, readlink    S a stat, access
//	D an open of a directory (O_DIRECTORY)                W an open for writing, a creation,
//	                                                      a removal or a rename
//	H an open for reading by sha256sum (GT7's fix round: hashed only, a key's computation; gate.audit
//	  covers it where it is a host's identity check or a build's inner cache key)
//
// The port of test/gate/audit.awk, which busybox awk ran behind strace's FIFO: it was a quarter of
// diffcheck's audit (27 CPU-minutes of 72). The output is the same lines in the same order.
//
// Since GT7's fix round (doc/GATE-SURVEY.md, "As built: GT7's fix round"): the trace is read with a
// blocking descriptor (no netpoll on the FIFO), job-control signals are ignored, and with -progress
// FILE a heartbeat goroutine rewrites FILE every few seconds with "UNIX-TIME BYTES-READ LINES PENDING"
// (PENDING: the bytes waiting in the FIFO, FIONREAD), which the reducer's watchdog (watch.go, a process of
// its own) reads: a heartbeat gone stale, or bytes pending that are not read, for
// GATE_AUDIT_STALL_S seconds (600) is a stalled reducer, and the step fails loudly instead of hanging.
//
// Usage: gateaudit -root CHECKOUT [-signals FILE] [-progress FILE] STRACE-OUTPUT > PATHS; a FIFO's
// reducer's -progress defaults to FIFO.progress, and its watchdog runs (watch.go).
package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"os"
	"os/signal"
	"regexp"
	"strconv"
	"strings"
	"sync/atomic"
	"syscall"
	"time"
	"unsafe"
)

// fionread: ioctl FIONREAD (TIOCINQ), the same number on linux/amd64 and linux/arm64
const fionread = 0x541B

var (
	nread  atomic.Int64 // bytes the scanner has taken from the trace
	nlines atomic.Int64
)

// counting: the trace's reader, its bytes counted for the heartbeat
type counting struct{ r io.Reader }

func (c counting) Read(p []byte) (int, error) {
	n, err := c.r.Read(p)
	nread.Add(int64(n))
	return n, err
}

// pending: the bytes waiting in fd (a FIFO's unread data); -1 when fd is not one or unknown
func pending(fd int) int64 {
	if fd < 0 {
		return -1
	}
	var n int32
	if _, _, e := syscall.Syscall(syscall.SYS_IOCTL, uintptr(fd), fionread, uintptr(unsafe.Pointer(&n))); e != 0 {
		return -1
	}
	return int64(n)
}

// beat: FILE rewritten (a temporary renamed) with the reducer's progress
func beat(file string, fd *atomic.Int64) {
	tmp := file + ".tmp"
	line := fmt.Sprintf("%d %d %d %d\n", time.Now().Unix(), nread.Load(), nlines.Load(), pending(int(fd.Load())))
	if os.WriteFile(tmp, []byte(line), 0o644) == nil {
		os.Rename(tmp, file)
	}
}

// heartbeat: a beat every period, from a goroutine of its own (alive while the reader blocks)
func heartbeat(file string, fd *atomic.Int64, period time.Duration) {
	for {
		beat(file, fd)
		time.Sleep(period)
	}
}

var (
	cwd   = map[int]string{}
	exe   = map[int]string{}
	alive = map[int]bool{}
	seen  = map[string]bool{}
	out   *bufio.Writer
	gitRe = regexp.MustCompile(`/git(-[a-z-]+)?$`)
	sumRe = regexp.MustCompile(`/sha256sum$`)
)

func norm(p string) string {
	var parts []string
	for _, c := range strings.Split(p, "/") {
		switch c {
		case "", ".":
		case "..":
			if len(parts) > 0 {
				parts = parts[:len(parts)-1]
			}
		default:
			parts = append(parts, c)
		}
	}
	return "/" + strings.Join(parts, "/")
}

func resolve(p, base string) string {
	if strings.HasPrefix(p, "/") {
		return norm(p)
	}
	if p == "" {
		return ""
	}
	return norm(base + "/" + p)
}

// unq undoes strace's escapes of \" and \\ (as the awk's unq: the two the paths of a checkout hold)
func unq(s string) string {
	s = strings.ReplaceAll(s, `\"`, `"`)
	return strings.ReplaceAll(s, `\\`, `\`)
}

// quoted: the leading "..." of s (strace's escapes inside), its text and the rest; ok false without one
func quoted(s string) (string, bool) {
	if !strings.HasPrefix(s, `"`) {
		return "", false
	}
	for i := 1; i < len(s); i++ {
		switch s[i] {
		case '\\':
			i++
		case '"':
			return unq(s[1:i]), true
		}
	}
	return "", false
}

var (
	fdcwdDir = regexp.MustCompile(`^AT_FDCWD<([^>]*)>, `)
	fdcwd    = regexp.MustCompile(`^AT_FDCWD, `)
	fdDir    = regexp.MustCompile(`^[0-9]+<([^>]*)>, `)
	fdBare   = regexp.MustCompile(`^[0-9]+, `)
)

// target: the path argument of a call's text: [dirfd, ] "path"; "" without one
func target(args string, pid int, pathFirst bool) string {
	if pathFirst {
		if q, ok := quoted(args); ok {
			return resolve(q, cwd[pid])
		}
		return ""
	}
	base := cwd[pid]
	if m := fdcwdDir.FindStringSubmatch(args); m != nil {
		base, args = m[1], args[len(m[0]):]
	} else if m := fdcwd.FindString(args); m != "" {
		args = args[len(m):]
	} else if m := fdDir.FindStringSubmatch(args); m != nil {
		base, args = m[1], args[len(m[0]):]
	} else if fdBare.MatchString(args) {
		return ""
	}
	if q, ok := quoted(args); ok {
		return resolve(q, base)
	}
	return ""
}

var (
	writes = set("unlink unlinkat rmdir rename renameat renameat2 mkdir mkdirat symlink symlinkat link linkat chmod fchmodat fchmodat2 chown lchown fchownat truncate utimensat utimes futimesat mknod mknodat setxattr lsetxattr removexattr lremovexattr creat")
	opens  = set("open openat openat2")
	links  = set("readlink readlinkat")
	path1  = set("open creat stat lstat access readlink unlink rmdir mkdir chmod chown lchown truncate utimes rename link symlink mknod setxattr lsetxattr removexattr lremovexattr getxattr lgetxattr listxattr llistxattr statfs utime")
	dirfd1 = set("openat openat2 newfstatat fstatat64 statx faccessat faccessat2 readlinkat unlinkat mkdirat renameat renameat2 linkat symlinkat fchmodat fchmodat2 fchownat utimensat futimesat mknodat")
	forks  = set("clone clone3 fork vfork")
	wrRe   = regexp.MustCompile(`O_WRONLY|O_RDWR|O_CREAT|O_TRUNC`)
)

func set(s string) map[string]bool {
	m := map[string]bool{}
	for _, w := range strings.Fields(s) {
		m[w] = true
	}
	return m
}

func opFor(call, args string) string {
	switch {
	case writes[call]:
		return "W"
	case opens[call]:
		if strings.Contains(args, "O_DIRECTORY") {
			return "D"
		}
		if wrRe.MatchString(args) {
			return "W"
		}
		return "R"
	case links[call]:
		return "R"
	case strings.HasPrefix(call, "execve"):
		return "X"
	}
	return "S"
}

// emit: one OP<TAB>PATH line, once; git's reads (the version stamp's describe --dirty, status) are no
// inputs, the commit being in no key
func emit(op, path string, pid int) {
	if gitRe.MatchString(exe[pid]) {
		return
	}
	if op == "R" && sumRe.MatchString(exe[pid]) {
		op = "H"
	}
	k := op + "\t" + path
	if !seen[k] {
		seen[k] = true
		out.WriteString(k + "\n")
	}
}

var (
	pidRe     = regexp.MustCompile(`^[0-9]+ +`)
	resumedRe = regexp.MustCompile(`^<\.\.\. ([a-z_0-9]+) resumed>`)
	retRe     = regexp.MustCompile(`\) += ([0-9]+)`)
	callRe    = regexp.MustCompile(`^([a-z_0-9]+)\(`)
	sigRe     = regexp.MustCompile(`^[0-9]+ +(\+\+\+|---)`)
)

func child(pid int, s string) {
	if m := retRe.FindStringSubmatch(s); m != nil {
		c, _ := strconv.Atoi(m[1])
		if c > 0 && !alive[c] {
			cwd[c] = cwd[pid]
			exe[c] = exe[pid]
		}
	}
}

func main() {
	root := flag.String("root", "", "the checkout: a process's directory until its first chdir")
	signals := flag.String("signals", "", "a file the signal and exit lines go to (debugging)")
	progress := flag.String("progress", "", "a file the heartbeat rewrites: UNIX-TIME BYTES-READ LINES PENDING")
	period := flag.Duration("period", 5*time.Second, "the heartbeat's period")
	stallAfter := flag.Int64("test-stall-after", 0, "testing the watchdog: stop reading after N lines, the heartbeat alive (GATE_AUDIT_TEST_STALL_AFTER)")
	watchPid := flag.Int("watch", 0, "the watchdog of reducer PID (watch.go), started by a reducer of a FIFO")
	flag.Parse()
	if *watchPid > 0 {
		watch(*watchPid, flag.Arg(0), *progress)
		return
	}
	if *stallAfter == 0 {
		*stallAfter, _ = strconv.ParseInt(os.Getenv("GATE_AUDIT_TEST_STALL_AFTER"), 10, 64)
	}
	// a FIFO's reducer (the gate's audit): its heartbeat beside the FIFO, NAME.fifo.progress, and its
	// watchdog, unless -progress names another file (the runner passes no flag: bin/lib/gate-run.bash,
	// in every key, is unchanged by GT7's fix round)
	if flag.NArg() > 0 {
		if st, err := os.Stat(flag.Arg(0)); err == nil && st.Mode()&os.ModeNamedPipe != 0 {
			if *progress == "" {
				*progress = flag.Arg(0) + ".progress"
			}
			startWatch(flag.Arg(0), *progress)
		}
	}
	// a job-control stop of the step's process group must not stop the reader of its trace
	signal.Ignore(syscall.SIGTSTP, syscall.SIGTTIN, syscall.SIGTTOU)
	var fd atomic.Int64
	fd.Store(-1)
	if *progress != "" {
		go heartbeat(*progress, &fd, *period)
	}
	var in io.Reader = os.Stdin
	if flag.NArg() > 0 {
		// a blocking descriptor (os.NewFile of one is not pollable): the trace's FIFO read by plain
		// read(2), the kernel's own wakeup, no netpoll between strace's writes and this reader
		var n int
		var err error
		for {
			n, err = syscall.Open(flag.Arg(0), syscall.O_RDONLY|syscall.O_CLOEXEC, 0)
			if err != syscall.EINTR {
				break
			}
		}
		if err != nil {
			fmt.Fprintln(os.Stderr, "gateaudit:", flag.Arg(0)+":", err)
			os.Exit(2)
		}
		fd.Store(int64(n))
		f := os.NewFile(uintptr(n), flag.Arg(0))
		defer f.Close()
		in = f
	}
	in = counting{in}
	var dbg *os.File
	if *signals != "" {
		var err error
		if dbg, err = os.Create(*signals); err != nil {
			fmt.Fprintln(os.Stderr, "gateaudit:", err)
			os.Exit(2)
		}
		defer dbg.Close()
	}
	out = bufio.NewWriterSize(os.Stdout, 1<<16)
	defer out.Flush()
	cwd[0] = *root
	sc := bufio.NewScanner(in)
	sc.Buffer(make([]byte, 1<<20), 1<<26)
	for sc.Scan() {
		if nlines.Add(1) == *stallAfter {
			select {} // the watchdog's test: a reader that no longer reads
		}
		line := sc.Text()
		if dbg != nil && sigRe.MatchString(line) {
			fmt.Fprintln(dbg, line)
		}
		m := pidRe.FindString(line)
		if m == "" {
			continue
		}
		pid, _ := strconv.Atoi(strings.TrimRight(m, " "))
		line = line[len(m):]
		if _, ok := cwd[pid]; !ok {
			cwd[pid] = *root
		}
		alive[pid] = true
		// a call resumed: only a clone's result matters (its path arguments came in the unfinished line)
		if r := resumedRe.FindStringSubmatch(line); r != nil {
			if forks[r[1]] {
				child(pid, line)
			}
			continue
		}
		c := callRe.FindStringSubmatch(line)
		if c == nil {
			continue
		}
		call, args := c[1], line[len(c[0]):]
		switch {
		case forks[call]:
			child(pid, args)
		case call == "chdir":
			if p := target(args, pid, true); p != "" && !strings.Contains(args, "= -1 ") {
				cwd[pid] = p
			}
		case call == "execve":
			if p := target(args, pid, true); p != "" {
				exe[pid] = p
				emit("X", p, pid)
			}
		case call == "execveat":
			if p := target(args, pid, false); p != "" {
				emit("X", p, pid)
			}
		case path1[call]:
			if p := target(args, pid, true); p != "" {
				emit(opFor(call, args), p, pid)
			}
		case dirfd1[call]:
			if p := target(args, pid, false); p != "" {
				emit(opFor(call, args), p, pid)
			}
		}
	}
	if err := sc.Err(); err != nil {
		fmt.Fprintln(os.Stderr, "gateaudit:", err)
		os.Exit(1)
	}
	if *progress != "" {
		beat(*progress, &fd)
	}
}
