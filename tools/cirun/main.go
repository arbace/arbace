// Command cirun runs a C program's node program (arbace.c.build's output, as text) on ci's back
// end, outside Arbace's executable: the light path of bin/ci run (doc/C23-SURVEY.md, "As built:
// C5 (write-ahead)"). Usage: cirun PROGRAM.nodes [ARG...]; the program's status is cirun's; a fault
// is reported on the standard error as "cirun: fault: ...".
package main

import (
	"bufio"
	"fmt"
	"os"

	"arbace/ci"
	"arbace/crt"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "usage: cirun PROGRAM.nodes [ARG...]")
		os.Exit(2)
	}
	text, err := os.ReadFile(os.Args[1])
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}
	tops, err := ci.Parse(string(text))
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}
	out := bufio.NewWriterSize(os.Stdout, 1<<16)
	m := crt.NewMem()
	sys := crt.NewSys(m, os.Stdin, out, os.Stderr)
	p := ci.NewProg(sys)
	l := ci.NewLoader(p)
	for _, t := range tops {
		l.Top(t)
	}
	if l.Entry == nil {
		fmt.Fprintln(os.Stderr, "cirun: no entry")
		os.Exit(2)
	}
	argv := append([]string{"a.out"}, os.Args[2:]...)
	status, fault := p.RunMain(l.Start, l.Entry, argv)
	out.Flush()
	if fault != nil {
		fmt.Fprintln(os.Stderr, "cirun: fault:", fault)
	}
	os.Exit(status)
}
