// Command cirun runs a C program's node program (arbace.c.build's output, as text) on ci's back
// end, outside Arbace's executable: the light path of bin/ci run (doc/C23-SURVEY.md, "As built:
// C5 (write-ahead)"). Usage: cirun PROGRAM.nodes [ARG...]; the program's status is cirun's; a fault
// is reported on the standard error as "cirun: fault: ...". cirun's own failures (no node program, a parse
// error, no entry, a panic of the interpreter's) are "cirun: internal: ..." with status 70, which bin/ci check's
// judge tells from a program's own status (doc/C23-SURVEY.md, "As built: review 006's fixes", finding 4).
// cirun --env PROGRAM.nodes [ARG...] gives the program cirun's own environment (RES29 (a), doc/rust/EVAL.md §10);
// without it the program starts with an empty one, as before. --signals: a run a signal ended (a fault, an abort) ends
// cirun by that signal (crt.DieBy), as the kernel ends the C program, so a parent sees WIFSIGNALED (RE6); without it
// cirun exits with 128 + the signal, as before. --quiet-faults (the children's, crt's spawn.go): no fault line.
package main

import (
	"bufio"
	"fmt"
	"os"
	"path/filepath"
	"runtime/debug"

	"arbace/ci"
	"arbace/crt"
)

// internal reports a failure of cirun's own, the program's output flushed first.
func internal(out *bufio.Writer, format string, a ...any) {
	if out != nil {
		out.Flush()
	}
	fmt.Fprintf(os.Stderr, "cirun: internal: "+format+"\n", a...)
	os.Exit(70)
}

func main() {
	args := os.Args[1:]
	env, sigs, quiet := false, false, false
	for len(args) > 0 && (args[0] == "--env" || args[0] == "--signals" || args[0] == "--quiet-faults") {
		switch args[0] {
		case "--env":
			env = true
		case "--signals":
			sigs = true
		default:
			quiet = true
		}
		args = args[1:]
	}
	if len(args) < 1 {
		internal(nil, "usage: cirun [--env] PROGRAM.nodes [ARG...]")
	}
	text, err := os.ReadFile(args[0])
	if err != nil {
		internal(nil, "%v", err)
	}
	tops, err := ci.Parse(string(text))
	if err != nil {
		internal(nil, "%v", err)
	}
	out := bufio.NewWriterSize(os.Stdout, 1<<16)
	defer func() {
		if r := recover(); r != nil {
			internal(out, "%v\n%s", r, debug.Stack())
		}
	}()
	m := crt.NewMem()
	sys := crt.NewSys(m, os.Stdin, out, os.Stderr)
	p := ci.NewProg(sys)
	if env {
		p.Env = os.Environ()
	}
	// the program run again as a child process (crt's spawn.go): this cirun, its --env, the node program's absolute path
	if exe, err := os.Executable(); err == nil {
		if r, err := filepath.EvalSymlinks(exe); err == nil {
			exe = r
		}
		nodes, _ := filepath.Abs(args[0])
		sys.SelfExe, sys.Argv0 = exe, "a.out"
		sys.Self = []string{exe}
		if env {
			sys.Self = append(sys.Self, "--env")
		}
		if sigs {
			// a child (the program run again) dies by its signal as the original's child does, and prints no fault line
			// of ci's: its parent reads its standard error as the original's parent reads its child's
			sys.Self = append(sys.Self, "--signals", "--quiet-faults")
		}
		sys.Self = append(sys.Self, nodes)
	}
	sys.StdFiles = [3]*os.File{os.Stdin, os.Stdout, os.Stderr}
	l := ci.NewLoader(p)
	for _, t := range tops {
		l.Top(t)
	}
	if l.Entry == nil {
		internal(out, "no entry")
	}
	argv := append([]string{"a.out"}, args[1:]...)
	status, fault := p.RunMain(l.Start, l.Entry, argv)
	out.Flush()
	if fault != nil && !quiet {
		fmt.Fprintln(os.Stderr, "cirun: fault:", fault)
	}
	if sigs && p.Signal != 0 {
		os.Stderr.Sync()
		crt.DieBy(p.Signal)
	}
	os.Exit(status)
}
