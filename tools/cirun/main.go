// Command cirun runs a C program's node program (arbace.c.build's output, as text) on ci's back
// end, outside Arbace's executable: the light path of bin/ci run (doc/C23-SURVEY.md, "As built:
// C5 (write-ahead)"). Usage: cirun PROGRAM.nodes [ARG...]; the program's status is cirun's; a fault
// is reported on the standard error as "cirun: fault: ...". cirun's own failures (no node program, a parse
// error, no entry, a panic of the interpreter's) are "cirun: internal: ..." with status 70, which bin/ci check's
// judge tells from a program's own status (doc/C23-SURVEY.md, "As built: review 006's fixes", finding 4).
// cirun --env PROGRAM.nodes [ARG...] gives the program cirun's own environment (RES29 (a), doc/rust/EVAL.md §10);
// without it the program starts with an empty one, as before.
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
	env := false
	if len(args) > 0 && args[0] == "--env" {
		env, args = true, args[1:]
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
	if fault != nil {
		fmt.Fprintln(os.Stderr, "cirun: fault:", fault)
	}
	os.Exit(status)
}
