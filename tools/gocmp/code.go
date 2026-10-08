package main

// Level 3: the generated code.
//
// Both sides are compiled with -S, which makes gc print, for every symbol it
// emits (functions, their funcdata, read-only data, type descriptors, closures,
// wrappers), the assembly with the instruction offsets, the machine code bytes
// and the relocations. The listing is split per symbol and compared per symbol
// after removing the source position printed on every instruction.

import (
	"fmt"
	"regexp"
	"sort"
	"strings"
)

// asmSym is one symbol of a -S listing.
type asmSym struct {
	name string
	raw  []string // the lines as printed
	norm []string // the lines without positions
}

// parseAsm splits a -S listing into symbols. Lines that start without a tab
// begin a symbol ("NAME KIND attrs..."), the indented lines belong to it,
// "# pkg" lines are the go command's headers.
//
// Without flattened positions, DWARF symbols are left out: they hold
// positions throughout, in encoded form.
func parseAsm(out string, noDWARF bool) map[string][]*asmSym {
	syms := map[string][]*asmSym{}
	var cur *asmSym
	for _, line := range strings.Split(out, "\n") {
		if line == "" || strings.HasPrefix(line, "# ") {
			continue
		}
		if line[0] != '\t' {
			// NAME KIND [dupok ...] size=N ...; names may hold spaces
			m := symHeader.FindStringSubmatch(line)
			if m == nil {
				cur = nil
				continue
			}
			if noDWARF && strings.HasPrefix(m[2], "SDWARF") {
				cur = nil
				continue
			}
			if cur != nil {
				cur.finish()
			}
			cur = &asmSym{name: m[1]}
			syms[m[1]] = append(syms[m[1]], cur)
		}
		if cur == nil {
			continue
		}
		cur.raw = append(cur.raw, line)
		cur.norm = append(cur.norm, stripAsmPos(line))
	}
	if cur != nil {
		cur.finish()
	}
	return syms
}

var symHeader = regexp.MustCompile(`^(.*) (S[A-Z0-9]+) (.* )?size=[0-9]+`)

// finish sorts the symbol's R_INITORDER relocations (in a package's
// ..inittask): they list the imported packages in source order, which carries
// no meaning, since the linker orders initialization by the dependency graph
// and then by import path (cmd/link/internal/ld/inittask.go).
func (s *asmSym) finish() {
	first := -1
	for i, l := range s.norm {
		if strings.Contains(l, " t=R_INITORDER ") {
			if first < 0 {
				first = i
			}
		} else if first >= 0 {
			sortInit(s, first, i)
			first = -1
		}
	}
	if first >= 0 {
		sortInit(s, first, len(s.norm))
	}
}

func sortInit(s *asmSym, i, j int) {
	sort.Strings(s.norm[i:j])
	sort.Strings(s.raw[i:j])
}

// stripAsmPos removes the position from an instruction line,
// "\t0x0012 00018 (file.go:12)\tMOVQ..." -> "\t0x0012 00018\tMOVQ...".
func stripAsmPos(line string) string {
	if !strings.HasPrefix(line, "\t0x") {
		return line
	}
	i := strings.Index(line, " (")
	if i < 0 {
		return line
	}
	j := strings.Index(line[i:], ")\t")
	if j < 0 {
		return line
	}
	return line[:i] + line[i+j+1:]
}

func compareAsm(outA, outB string, noDWARF bool) string {
	a, b := parseAsm(outA, noDWARF), parseAsm(outB, noDWARF)
	names := map[string]bool{}
	for n := range a {
		names[n] = true
	}
	for n := range b {
		names[n] = true
	}
	sorted := make([]string, 0, len(names))
	for n := range names {
		sorted = append(sorted, n)
	}
	sort.Strings(sorted)
	var onlyA, onlyB []string
	for _, n := range sorted {
		sa, sb := a[n], b[n]
		switch {
		case len(sb) == 0:
			onlyA = append(onlyA, n)
		case len(sa) == 0:
			onlyB = append(onlyB, n)
		}
	}
	if len(onlyA) > 0 || len(onlyB) > 0 {
		return fmt.Sprintf("symbols only in A: %s\nsymbols only in B: %s", firstFew(onlyA), firstFew(onlyB))
	}
	for _, n := range sorted {
		sa, sb := a[n], b[n]
		if len(sa) != len(sb) {
			return fmt.Sprintf("symbol %s: printed %d vs %d times", n, len(sa), len(sb))
		}
		// Symbols printed more than once (rare) are matched in order of
		// their normalized text.
		sort.Slice(sa, func(i, j int) bool { return strings.Join(sa[i].norm, "\n") < strings.Join(sa[j].norm, "\n") })
		sort.Slice(sb, func(i, j int) bool { return strings.Join(sb[i].norm, "\n") < strings.Join(sb[j].norm, "\n") })
		for k := range sa {
			if d := diffSym(sa[k], sb[k]); d != "" {
				return d
			}
		}
	}
	return ""
}

func firstFew(s []string) string {
	if len(s) > 8 {
		return strings.Join(s[:8], " ") + fmt.Sprintf(" ... (%d)", len(s))
	}
	if len(s) == 0 {
		return "-"
	}
	return strings.Join(s, " ")
}

// diffSym reports the first differing line of a symbol, with context and the
// positions of both sides.
func diffSym(a, b *asmSym) string {
	n := min(len(a.norm), len(b.norm))
	i := 0
	for i < n && a.norm[i] == b.norm[i] {
		i++
	}
	if i == n && len(a.norm) == len(b.norm) {
		return ""
	}
	var sb strings.Builder
	fmt.Fprintf(&sb, "symbol %s differs at line %d of %d vs %d:\n", a.name, i+1, len(a.norm), len(b.norm))
	for j := max(0, i-3); j < i; j++ {
		fmt.Fprintf(&sb, "    %s\n", a.raw[j])
	}
	for j := i; j < min(len(a.raw), i+3); j++ {
		fmt.Fprintf(&sb, "  A %s\n", a.raw[j])
	}
	for j := i; j < min(len(b.raw), i+3); j++ {
		fmt.Fprintf(&sb, "  B %s\n", b.raw[j])
	}
	return strings.TrimRight(sb.String(), "\n")
}
