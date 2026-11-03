// covsubset reduce: the reducer of tools/cicover (queue 160, 161: branch mac/161-subsets, eda4ecfc,
// tools/cicover/main.go), copied whole for every crop (doc/GATE-SURVEY.md, GT8.3, GTS205 (a), GTS206 (a)), its
// index and outputs unchanged; only its entry is reduce(args) instead of main, and a line without spaces is
// refused before it is cut (review 182 L8). Its comment, as it came:
//
// Command cicover is bin/ci cover's reducer (queue 160, the test-budget survey's input; a prototype, in no
// gate step): Go's coverage profiles of ci's back end (go/arbace/ci and go/arbace/crt, tools/cirun built with
// -cover, one GOCOVERDIR per program, each turned into text by `go tool covdata textfmt`) reduced to a
// small set of programs reaching the same covered blocks, by greedy set cover: the programs that must stay
// (KEEP 1: a program of a deferred class) taken first; then, for each class (CLASS, queue 161: the user's (a)
// for 160's DECISION, one program per class of the corpus's records) none of them is in, its program adding
// the most blocks (ties: the shorter run, then the id); then repeatedly the program adding the most blocks not
// yet reached (ties: the shorter run, then the id), until none adds one; then each pick but the kept ones
// whose blocks the rest of the set reaches, and whose class the rest still has, is dropped (the latest
// first). A block is a profile line's FILE:START,END; covered when its count is above zero.
//
// Usage: cicover [-seeds N] [-o SUBSET] [-curve FILE] INDEX
//
//	INDEX  a TSV, one program a line: ID, PROFILE (its textfmt file, or - when it was not run), KEEP (1 or
//	       0), SECONDS (its plain run's wall time, cirun without -cover), and optionally CLASS (its class
//	       in the corpus's record, - for none: a pass)
//	-o     the subset's ids, one a line (the kept ones first, then the per-class picks, then the greedy
//	       picks in order)
//	-curve the greedy order with the blocks reached after each pick: "N<TAB>ID<TAB>BLOCKS"
//	-seeds the random subsets of the subset's size drawn for the baseline (seeds 1..N)
//
// Prints "key<TAB>value" lines: the programs, the run ones, the blocks of the profiles and those the corpus
// covers, the subset's size and its kept part, the random baseline's mean, min and max share (drawn from
// every program, and again with the kept ones always in), and the run time of the subset against the corpus's.
package main

import (
	"bufio"
	"flag"
	"fmt"
	"math/bits"
	"math/rand"
	"os"
	"strconv"
	"strings"
)

type prog struct {
	id   string
	keep bool
	secs float64
	run  bool
	cls  string // its class, "" for none
	set  []uint64 // the covered blocks, a bit set
}

type bitset []uint64

func (b bitset) count() int {
	n := 0
	for _, w := range b {
		n += bits.OnesCount64(w)
	}
	return n
}

func (b bitset) or(c []uint64) {
	for i, w := range c {
		b[i] |= w
	}
}

// gain counts c's blocks not in b.
func (b bitset) gain(c []uint64) int {
	n := 0
	for i, w := range c {
		n += bits.OnesCount64(w &^ b[i])
	}
	return n
}

func die(format string, a ...any) {
	fmt.Fprintf(os.Stderr, "covsubset: "+format+"\n", a...)
	os.Exit(1)
}

func reduce(args []string) {
	fs := flag.NewFlagSet("reduce", flag.ExitOnError)
	seeds := fs.Int("seeds", 20, "random subsets drawn for the baseline")
	out := fs.String("o", "", "the subset's ids")
	curve := fs.String("curve", "", "the greedy order and the blocks reached")
	fs.Parse(args)
	if fs.NArg() != 1 {
		die("usage: covsubset reduce [-seeds N] [-o SUBSET] [-curve FILE] INDEX")
	}
	idx, err := os.ReadFile(fs.Arg(0))
	if err != nil {
		die("%v", err)
	}
	blocks := map[string]int{}
	var progs []*prog
	covered := map[*prog][]int{}
	for _, line := range strings.Split(strings.TrimRight(string(idx), "\n"), "\n") {
		if line == "" {
			continue
		}
		f := strings.Split(line, "\t")
		if len(f) != 4 && len(f) != 5 {
			die("index line %q: want 4 or 5 fields", line)
		}
		secs, _ := strconv.ParseFloat(f[3], 64)
		p := &prog{id: f[0], keep: f[2] == "1", secs: secs, run: f[1] != "-"}
		if len(f) == 5 && f[4] != "-" && f[4] != "" {
			p.cls = f[4]
		}
		progs = append(progs, p)
		if !p.run {
			continue
		}
		fh, err := os.Open(f[1])
		if err != nil {
			die("%v", err)
		}
		sc := bufio.NewScanner(fh)
		sc.Buffer(make([]byte, 1<<20), 1<<20)
		seen := map[int]bool{}
		for sc.Scan() {
			l := sc.Text()
			if strings.HasPrefix(l, "mode:") || l == "" {
				continue
			}
			// FILE:SL.SC,EL.EC NSTMTS COUNT
			sp := strings.LastIndexByte(l, ' ')
			if sp < 0 {
				die("%s: bad line %q", f[1], l)
			}
			sp2 := strings.LastIndexByte(l[:sp], ' ')
			if sp2 < 0 {
				die("%s: bad line %q", f[1], l)
			}
			key := l[:sp2]
			b, ok := blocks[key]
			if !ok {
				b = len(blocks)
				blocks[key] = b
			}
			if l[sp+1:] != "0" && !seen[b] {
				seen[b] = true
				covered[p] = append(covered[p], b)
			}
		}
		fh.Close()
		if err := sc.Err(); err != nil {
			die("%s: %v", f[1], err)
		}
	}
	words := (len(blocks) + 63) / 64
	all := bitset(make([]uint64, words))
	for _, p := range progs {
		p.set = make([]uint64, words)
		for _, b := range covered[p] {
			p.set[b/64] |= 1 << (b % 64)
		}
		all.or(p.set)
	}
	total := all.count()

	// greedy: the kept ones, then the best gain each round
	reached := bitset(make([]uint64, words))
	var subset []*prog
	in := map[*prog]bool{}
	for _, p := range progs {
		if p.keep {
			subset = append(subset, p)
			in[p] = true
			reached.or(p.set)
		}
	}
	nkeep := len(subset)
	// one program per class: a class none of the set has gets its program of the most gain
	has := map[string]bool{}
	var classes []string
	for _, p := range progs {
		if p.cls != "" && !has[p.cls] {
			has[p.cls] = true
			classes = append(classes, p.cls)
		}
	}
	for c := range has {
		has[c] = false
	}
	for _, p := range subset {
		if p.cls != "" {
			has[p.cls] = true
		}
	}
	for _, c := range classes {
		if has[c] {
			continue
		}
		var best *prog
		bg := -1
		for _, p := range progs {
			if p.cls != c || in[p] {
				continue
			}
			g := reached.gain(p.set)
			if g > bg || (g == bg && (p.secs < best.secs || (p.secs == best.secs && p.id < best.id))) {
				best, bg = p, g
			}
		}
		subset = append(subset, best)
		in[best] = true
		has[c] = true
		reached.or(best.set)
	}
	nclass := len(subset) - nkeep
	type step struct {
		p *prog
		n int
	}
	var order []step
	for {
		var best *prog
		bg := 0
		for _, p := range progs {
			if in[p] || !p.run {
				continue
			}
			g := reached.gain(p.set)
			if g > bg || (g == bg && g > 0 && (p.secs < best.secs || (p.secs == best.secs && p.id < best.id))) {
				best, bg = p, g
			}
		}
		if best == nil {
			break
		}
		subset = append(subset, best)
		in[best] = true
		reached.or(best.set)
		order = append(order, step{best, reached.count()})
	}
	// redundancy: a pick (per class or greedy) the others reach, its class still in them, is dropped, the latest first
	for i := len(subset) - 1; i >= nkeep; i-- {
		rest := bitset(make([]uint64, words))
		clsIn := false
		for j, q := range subset {
			if j != i {
				rest.or(q.set)
				if q.cls == subset[i].cls {
					clsIn = true
				}
			}
		}
		if rest.count() == total && (subset[i].cls == "" || clsIn) {
			if i < nkeep+nclass {
				nclass--
			}
			subset = append(subset[:i], subset[i+1:]...)
		}
	}
	for _, c := range classes {
		ok := false
		for _, p := range subset {
			if p.cls == c {
				ok = true
			}
		}
		if !ok {
			die("the subset has no program of class %s", c)
		}
	}
	got := bitset(make([]uint64, words))
	var subSecs, allSecs float64
	for _, p := range subset {
		got.or(p.set)
		subSecs += p.secs
	}
	nrun := 0
	for _, p := range progs {
		allSecs += p.secs
		if p.run {
			nrun++
		}
	}
	if got.count() != total {
		die("the subset reaches %d of %d blocks", got.count(), total)
	}

	// the random baseline: subsets of the same size, drawn from every program; and with the kept ones in
	k := len(subset)
	share := func(force bool, seed int64) float64 {
		r := rand.New(rand.NewSource(seed))
		s := bitset(make([]uint64, words))
		n := 0
		var pool []*prog
		for _, p := range progs {
			if force && p.keep {
				s.or(p.set)
				n++
			} else {
				pool = append(pool, p)
			}
		}
		r.Shuffle(len(pool), func(i, j int) { pool[i], pool[j] = pool[j], pool[i] })
		for i := 0; n < k && i < len(pool); i++ {
			s.or(pool[i].set)
			n++
		}
		if total == 0 {
			return 1
		}
		return float64(s.count()) / float64(total)
	}
	stats := func(force bool) (mean, lo, hi float64) {
		lo, hi = 2, -1
		for i := 1; i <= *seeds; i++ {
			v := share(force, int64(i))
			mean += v
			if v < lo {
				lo = v
			}
			if v > hi {
				hi = v
			}
		}
		return mean / float64(*seeds), lo, hi
	}
	fmt.Printf("programs\t%d\n", len(progs))
	fmt.Printf("programs-run\t%d\n", nrun)
	fmt.Printf("blocks-profiled\t%d\n", len(blocks))
	fmt.Printf("blocks-covered\t%d\n", total)
	fmt.Printf("subset\t%d\n", k)
	fmt.Printf("subset-kept\t%d\n", nkeep)
	fmt.Printf("subset-per-class\t%d\n", nclass)
	fmt.Printf("subset-greedy\t%d\n", k-nkeep-nclass)
	fmt.Printf("classes\t%d\t%s\n", len(classes), strings.Join(classes, " "))
	m, lo, hi := stats(false)
	fmt.Printf("random-share\t%.4f\t%.4f\t%.4f\t(mean min max over %d seeds)\n", m, lo, hi, *seeds)
	m, lo, hi = stats(true)
	fmt.Printf("random-share-kept\t%.4f\t%.4f\t%.4f\t(the kept ones always in)\n", m, lo, hi)
	fmt.Printf("seconds-subset\t%.2f\n", subSecs)
	fmt.Printf("seconds-corpus\t%.2f\n", allSecs)
	if allSecs > 0 {
		fmt.Printf("seconds-share\t%.4f\n", subSecs/allSecs)
	}
	if *out != "" {
		var b strings.Builder
		for _, p := range subset {
			b.WriteString(p.id + "\n")
		}
		if err := os.WriteFile(*out, []byte(b.String()), 0o644); err != nil {
			die("%v", err)
		}
	}
	if *curve != "" {
		var b strings.Builder
		fmt.Fprintf(&b, "0\t(kept: %d)\t%d\n", nkeep, func() int {
			s := bitset(make([]uint64, words))
			for _, p := range progs {
				if p.keep {
					s.or(p.set)
				}
			}
			return s.count()
		}())
		for i, s := range order {
			fmt.Fprintf(&b, "%d\t%s\t%d\n", i+1, s.p.id, s.n)
		}
		if err := os.WriteFile(*curve, []byte(b.String()), 0o644); err != nil {
			die("%v", err)
		}
	}
}
