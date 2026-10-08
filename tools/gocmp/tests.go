package main

// gocmp tests: the single-file programs of GOROOT/test, for the helper's
// corpus and the oracle's alike (one rule, one place; doc/go/ROUNDTRIP.md).

import (
	"bytes"
	"flag"
	"fmt"
	"go/build"
	"go/build/constraint"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

// testDirs are the directories of Go's test driver (go1.27.1,
// src/cmd/internal/testdir/testdir_test.go, dirs).
var testDirs = []string{".", "ken", "chan", "interface", "internal/runtime/sys", "syntax", "dwarf",
	"fixedbugs", "codegen", "abi", "typeparam", "typeparam/mdempsky", "arenas", "simd"}

// programActions are the actions that compile one program from the file (and
// the files its action line names).
var programActions = map[string]bool{"run": true, "runoutput": true, "build": true,
	"buildrun": true, "compile": true, "asmcheck": true}

const testsUsage = `usage: gocmp tests [-v] TESTDIR
Prints the single-file programs of TESTDIR (GOROOT/test) for the configuration
in the environment (GOOS, GOARCH), one per line, relative to TESTDIR:
FILE, or FILE,EXTRA.go,... when the action line names more files of the
program. A file is taken when it is in a directory of Go's test driver, its
action (the first line that is not a build constraint, as the driver reads it)
is run, runoutput, build, buildrun, compile or asmcheck, it needs no other
GOEXPERIMENT (-goexperiment), and go/build matches it for the configuration
(build constraints and file name, cgo off). -v prints the files left out and
why on stderr.
`

func mainTests(args []string) int {
	fs := flag.NewFlagSet("gocmp tests", flag.ExitOnError)
	verbose := fs.Bool("v", false, "print the files left out, and why, on stderr")
	fs.Usage = usage(fs, testsUsage)
	fs.Parse(args)
	if fs.NArg() != 1 {
		fs.Usage()
		return 2
	}
	root := fs.Arg(0)
	ctxt := build.Default
	ctxt.CgoEnabled = false
	var out []string
	for _, d := range testDirs {
		names, err := filepath.Glob(filepath.Join(root, d, "*.go"))
		if err != nil {
			fmt.Fprintln(os.Stderr, err)
			return 2
		}
		sort.Strings(names)
		for _, name := range names {
			rel, _ := filepath.Rel(root, name)
			rel = filepath.ToSlash(rel)
			entry, why, err := testProgram(&ctxt, root, rel)
			if err != nil {
				fmt.Fprintln(os.Stderr, err)
				return 2
			}
			if entry == "" {
				if *verbose && why != "" {
					fmt.Fprintf(os.Stderr, "%s\t%s\n", rel, why)
				}
				continue
			}
			out = append(out, entry)
		}
	}
	for _, e := range out {
		fmt.Println(e)
	}
	return 0
}

// testProgram decides on one file: its entry, or "" and why not.
func testProgram(ctxt *build.Context, root, rel string) (entry, why string, err error) {
	src, err := os.ReadFile(filepath.Join(root, rel))
	if err != nil {
		return "", "", err
	}
	var action string
	for rest := string(src); action == "" && rest != ""; {
		var line string
		line, rest, _ = strings.Cut(rest, "\n")
		if constraint.IsGoBuild(line) || constraint.IsPlusBuild(line) {
			continue
		}
		action = strings.TrimSpace(strings.TrimPrefix(line, "//"))
	}
	f := strings.Fields(action)
	if len(f) == 0 || !programActions[f[0]] {
		return "", "", nil // not a single-program test: no reason worth printing
	}
	var extra []string
	for _, a := range f[1:] {
		if a == "-goexperiment" {
			return "", "needs another GOEXPERIMENT: " + action, nil
		}
		if strings.HasSuffix(a, ".go") && !strings.HasPrefix(a, "./") {
			extra = append(extra, a)
		}
	}
	dir, base := filepath.Split(filepath.Join(root, rel))
	ok, err := ctxt.MatchFile(dir, base)
	if err != nil {
		return "", "", err
	}
	if !ok {
		return "", "excluded by its build constraints or name: " + firstConstraint(src), nil
	}
	return strings.Join(append([]string{rel}, extra...), ","), "", nil
}

func firstConstraint(src []byte) string {
	for _, l := range bytes.Split(src, []byte("\n")) {
		s := string(l)
		if constraint.IsGoBuild(s) || constraint.IsPlusBuild(s) {
			return s
		}
		if strings.HasPrefix(s, "package ") {
			break
		}
	}
	return "(file name)"
}
