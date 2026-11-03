// Command covsubset is bin/cover's tool (doc/GATE-SURVEY.md, GT8.3, GTS205 (a), GTS206 (a)): Go's coverage of
// Arbace's cover build (bin/arbace-go --gate-build --cover) turned into block sets per test, and those reduced to a
// subset reaching the same blocks.
//
//	covsubset blocks -go GO -pkgs FILE -o OUT [-j N] DIR...
//	    each DIR a test's GOCOVERDIR (bin/lib/cover.bash's cover_env: its id in DIR/id), turned into text by
//	    `GO tool covdata textfmt -pkg` (the import paths of FILE, one a line: a crop's code under check), its
//	    covered blocks (count above zero) written to OUT/blocks/N.txt as textfmt lines (FILE:SL.SC,EL.EC NSTMT 1,
//	    the reducer's profile), N the test's number; OUT/tests.tsv: ID, its blocks file (- when the run left no
//	    counters: a run killed before its first flush), the blocks it covers; OUT/files.tsv: each source file
//	    of the packages with its blocks, those the tests cover and the share; OUT/summary.tsv: the totals
//	covsubset reduce [-seeds N] [-o SUBSET] [-curve FILE] INDEX
//	    tools/cicover's reducer (reduce.go)
package main

import (
	"bufio"
	"bytes"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"sync"
)

func main() {
	if len(os.Args) < 2 {
		die("usage: covsubset blocks|reduce ...")
	}
	switch os.Args[1] {
	case "blocks":
		blocks(os.Args[2:])
	case "reduce":
		reduce(os.Args[2:])
	default:
		die("unknown verb %s (blocks, reduce)", os.Args[1])
	}
}

type result struct {
	id      string
	file    string
	covered int
	err     string
}

func blocks(args []string) {
	fs := flag.NewFlagSet("blocks", flag.ExitOnError)
	goexe := fs.String("go", "go", "the go command")
	pkgsf := fs.String("pkgs", "", "the packages, one a line")
	out := fs.String("o", "", "the output directory")
	jobs := fs.Int("j", 8, "textfmt runs at once")
	fs.Parse(args)
	if *pkgsf == "" || *out == "" || fs.NArg() == 0 {
		die("usage: covsubset blocks -go GO -pkgs FILE -o OUT [-j N] DIR...")
	}
	pb, err := os.ReadFile(*pkgsf)
	if err != nil {
		die("%v", err)
	}
	pkgs := strings.Join(strings.Fields(string(pb)), ",")
	if pkgs == "" {
		die("%s names no package", *pkgsf)
	}
	if err := os.MkdirAll(filepath.Join(*out, "blocks"), 0o755); err != nil {
		die("%v", err)
	}
	dirs := fs.Args()
	res := make([]result, len(dirs))
	// every block of the packages, from the first full profile: FILE -> blocks
	var mu sync.Mutex
	all := map[string]map[string]bool{}
	cov := map[string]bool{}
	var wg sync.WaitGroup
	sem := make(chan struct{}, *jobs)
	for i, d := range dirs {
		wg.Add(1)
		sem <- struct{}{}
		go func(i int, d string) {
			defer wg.Done()
			defer func() { <-sem }()
			r := &res[i]
			idb, err := os.ReadFile(filepath.Join(d, "id"))
			r.id = strings.TrimSpace(string(idb))
			if err != nil || r.id == "" {
				r.id = filepath.Base(d)
			}
			r.file = "-"
			if m, _ := filepath.Glob(filepath.Join(d, "covcounters.*")); len(m) == 0 {
				return
			}
			tmp := filepath.Join(*out, "blocks", fmt.Sprintf("%d.full", i))
			cmd := exec.Command(*goexe, "tool", "covdata", "textfmt", "-i="+d, "-o="+tmp, "-pkg="+pkgs)
			var eb bytes.Buffer
			cmd.Stderr = &eb
			if err := cmd.Run(); err != nil {
				r.err = strings.TrimSpace(eb.String())
				return
			}
			fh, err := os.Open(tmp)
			if err != nil {
				r.err = err.Error()
				return
			}
			var b strings.Builder
			sc := bufio.NewScanner(fh)
			sc.Buffer(make([]byte, 1<<20), 1<<20)
			mine := map[string]bool{}
			var seen []string
			first := false
			mu.Lock()
			first = len(all) == 0
			mu.Unlock()
			local := map[string]map[string]bool{}
			for sc.Scan() {
				l := sc.Text()
				if strings.HasPrefix(l, "mode:") || l == "" {
					continue
				}
				sp := strings.LastIndexByte(l, ' ')
				sp2 := strings.LastIndexByte(l[:sp], ' ')
				if sp < 0 || sp2 < 0 {
					continue
				}
				key := l[:sp2]
				if first {
					f := key[:strings.LastIndexByte(key, ':')]
					if local[f] == nil {
						local[f] = map[string]bool{}
					}
					local[f][key] = true
				}
				if l[sp+1:] != "0" && !mine[key] {
					mine[key] = true
					seen = append(seen, key)
					b.WriteString(l[:sp] + " 1\n")
				}
			}
			fh.Close()
			os.Remove(tmp)
			r.file = filepath.Join(*out, "blocks", fmt.Sprintf("%d.txt", i))
			r.covered = len(seen)
			if err := os.WriteFile(r.file, []byte("mode: set\n"+b.String()), 0o644); err != nil {
				r.err = err.Error()
			}
			mu.Lock()
			if first && len(all) == 0 {
				all = local
			}
			for _, k := range seen {
				cov[k] = true
			}
			mu.Unlock()
		}(i, d)
	}
	wg.Wait()
	var t strings.Builder
	nerr, nrun := 0, 0
	for _, r := range res {
		if r.err != "" {
			nerr++
			fmt.Fprintf(os.Stderr, "covsubset: %s: %s\n", r.id, r.err)
		}
		if r.file != "-" && r.err == "" {
			nrun++
		}
		fmt.Fprintf(&t, "%s\t%s\t%d\n", r.id, r.file, r.covered)
	}
	write(filepath.Join(*out, "tests.tsv"), t.String())
	files := make([]string, 0, len(all))
	total := 0
	for f, bs := range all {
		files = append(files, f)
		total += len(bs)
	}
	sort.Strings(files)
	var fb strings.Builder
	fb.WriteString("file\tblocks\tcovered\tshare\n")
	for _, f := range files {
		n, c := len(all[f]), 0
		for k := range all[f] {
			if cov[k] {
				c++
			}
		}
		fmt.Fprintf(&fb, "%s\t%d\t%d\t%.3f\n", f, n, c, float64(c)/float64(max(n, 1)))
	}
	write(filepath.Join(*out, "files.tsv"), fb.String())
	s := fmt.Sprintf("tests\t%d\ntests-with-counters\t%d\nerrors\t%d\nsource-files\t%d\nblocks\t%d\nblocks-covered\t%d\nshare\t%.4f\n",
		len(dirs), nrun, nerr, len(files), total, len(cov), float64(len(cov))/float64(max(total, 1)))
	write(filepath.Join(*out, "summary.tsv"), s)
	fmt.Print(s)
	if nerr > 0 {
		os.Exit(1)
	}
}

func write(p, s string) {
	if err := os.WriteFile(p, []byte(s), 0o644); err != nil {
		die("%v", err)
	}
}
