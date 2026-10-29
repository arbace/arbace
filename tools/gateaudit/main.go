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
//
// The port of test/gate/audit.awk, which busybox awk ran behind strace's FIFO: it was a quarter of
// diffcheck's audit (27 CPU-minutes of 72). The output is the same lines in the same order.
//
// Usage: gateaudit -root CHECKOUT [-signals FILE] STRACE-OUTPUT > PATHS
package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"os"
	"regexp"
	"strconv"
	"strings"
)

var (
	cwd   = map[int]string{}
	exe   = map[int]string{}
	alive = map[int]bool{}
	seen  = map[string]bool{}
	out   *bufio.Writer
	gitRe = regexp.MustCompile(`/git(-[a-z-]+)?$`)
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
	flag.Parse()
	var in io.Reader = os.Stdin
	if flag.NArg() > 0 {
		f, err := os.Open(flag.Arg(0))
		if err != nil {
			fmt.Fprintln(os.Stderr, "gateaudit:", err)
			os.Exit(2)
		}
		defer f.Close()
		in = f
	}
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
}
