// Command chrootrun runs a program with its root directory and working
// directory set, for the box's build driver run on the host
// (arbace.box.rebuild's host executor, doc/go/BOX-NOTES.md, S8): an action's
// trees are written at their mount points under ROOT, and its program sees
// the same paths a tool guest sees.
//
// Usage:
//
//	chrootrun ROOT DIR PROG [ARG...]
//
// It changes the root directory to ROOT (which needs root's privilege), the
// working directory to DIR (in ROOT) and runs PROG (a path in ROOT) with the
// arguments ARG... (PROG's own name is its first argument, as the request's
// first argument names the tool) and its own environment.
package main

import (
	"fmt"
	"os"
	"syscall"
)

func main() {
	if len(os.Args) < 4 {
		fmt.Fprintln(os.Stderr, "usage: chrootrun ROOT DIR PROG [ARG...]")
		os.Exit(2)
	}
	fail := func(err error) {
		fmt.Fprintln(os.Stderr, "chrootrun:", err)
		os.Exit(126)
	}
	if err := syscall.Chroot(os.Args[1]); err != nil {
		fail(err)
	}
	if err := syscall.Chdir(os.Args[2]); err != nil {
		fail(err)
	}
	args := os.Args[4:]
	if len(args) == 0 {
		args = []string{os.Args[3]}
	}
	fail(syscall.Exec(os.Args[3], args, os.Environ()))
}
