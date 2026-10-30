package main

// A run of clang through the filter, its numbers, the layouts pass and the cache (doc/cpp/CPP-SURVEY.md, "As built: CX3").

import (
	"bufio"
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// version is part of every cache key: a change of the filter's output changes it.
const version = "cxxdump-7"

type countReader struct {
	r io.Reader
	n int64
}

func (c *countReader) Read(p []byte) (int, error) {
	n, err := c.r.Read(p)
	c.n += int64(n)
	return n, err
}

type countWriter struct {
	w io.Writer
	n int64
}

func (c *countWriter) Write(p []byte) (int, error) {
	n, err := c.w.Write(p)
	c.n += int64(n)
	return n, err
}

// stats are a run's numbers: the JSON read, the declarations kept and dropped, the bytes written, the wall
// seconds of the AST pass (clang and the filter together) and of the layouts pass, the peak resident sets
// of the filter and of clang (getrusage's maxrss, KiB), the status and the cache's answer.
type stats struct {
	in, out           int64
	kept, dropped     int64
	layouts           int
	wall, layoutWall  time.Duration
	selfRSS, childRSS int64
	status, cache     string
	stderr            string
	key               string
	layoutErr         string
}

func writeStats(name string, s stats) {
	var ru, rc syscall.Rusage
	syscall.Getrusage(syscall.RUSAGE_SELF, &ru)
	syscall.Getrusage(syscall.RUSAGE_CHILDREN, &rc)
	if s.selfRSS == 0 {
		s.selfRSS = ru.Maxrss
	}
	if s.childRSS == 0 {
		s.childRSS = rc.Maxrss
	}
	le := ""
	if s.layoutErr != "" {
		le = " :layouts-error true"
	}
	line := fmt.Sprintf("{:json-bytes %d :out-bytes %d :kept %d :dropped %d :layouts %d :seconds %.3f :layout-seconds %.3f :filter-rss-kib %d :clang-rss-kib %d :status %q :cache %q%s}",
		s.in, s.out, s.kept, s.dropped, s.layouts, s.wall.Seconds(), s.layoutWall.Seconds(), s.selfRSS, s.childRSS, s.status, s.cache, le)
	if name == "" {
		fmt.Fprintln(os.Stderr, "cxxdump: stats", line)
		return
	}
	os.WriteFile(name, []byte(line+"\n"), 0o644)
}

type job struct {
	clang   string
	args    []string
	opts    options
	layouts bool
}

// do runs clang's AST dump through the filter into out (and the layouts pass when asked); depfile, when not
// empty, receives clang's -MD list of the files the unit read. clang's standard error goes to errw.
func (j job) do(out io.Writer, depfile string, errw io.Writer) (stats, error) {
	var st stats
	start := time.Now()
	full := append([]string{"-Xclang", "-ast-dump=json", "-fsyntax-only"}, j.args...)
	if depfile != "" {
		full = append([]string{"-MD", "-MF", depfile}, full...)
	}
	cmd := exec.Command(j.clang, full...)
	cmd.Stderr = errw
	p, err := cmd.StdoutPipe()
	if err != nil {
		return st, err
	}
	if err := cmd.Start(); err != nil {
		return st, err
	}
	cin := &countReader{r: p}
	cw := &countWriter{w: out}
	bw := bufio.NewWriterSize(cw, 1<<16)
	kept, dropped, ferr := runOpts(cin, bw, j.opts)
	io.Copy(io.Discard, cin)
	werr := cmd.Wait()
	st.in, st.kept, st.dropped = cin.n, kept, dropped
	st.wall = time.Since(start)
	if cmd.ProcessState != nil {
		if ru, ok := cmd.ProcessState.SysUsage().(*syscall.Rusage); ok {
			st.childRSS = ru.Maxrss
		}
	}
	if werr != nil {
		st.status = "rejected"
		bw.Flush()
		st.out = cw.n
		return st, fmt.Errorf("clang rejected the unit: %v", werr)
	}
	if ferr != nil {
		st.status = "filter-error"
		return st, ferr
	}
	if j.layouts {
		t := time.Now()
		n, err := j.layoutPass(bw, errw)
		st.layouts, st.layoutWall = n, time.Since(t)
		if err != nil { // the AST stands; the unit's code does not compile (a layout needs CodeGen)
			st.layoutErr = err.Error()
		}
	}
	if !j.opts.norm && j.opts.edn {
		j.directivesPass(bw, errw) // a unit the preprocessor refuses has no executed set: the converter keeps the outer directives
	}
	st.status = "ok"
	err = bw.Flush()
	st.out = cw.n
	return st, err
}

// ---------------------------------------------------------------------------------------------
// Record layouts (doc/cpp/SPEC.md §14.2, CP20 (a)): a CodeGen pass with -fdump-record-layouts prints the AST
// layout of every record the unit's code uses, then its IRgen layout (the record's declaration with its
// location, and the LLVM type). The main file's records are those whose IRgen record is in the main file; each
// is paired with the latest AST layout of the same name before it.

type astLayout struct {
	typ         string
	size, dsize int64
	align       int64
	nvsize      int64
	nvalign     int64
	hasNV       bool
	vptr        []int64
	bases       []string
	fields      []string
	used        bool
}

var (
	reMember = regexp.MustCompile(`^\s*([0-9]+)(?::([0-9]+)-([0-9]+))? \|( *)(.*)$`)
	reSizes  = regexp.MustCompile(`(sizeof|dsize|align|nvsize|nvalign)=([0-9]+)`)
	reRecord = regexp.MustCompile(`^Record: (\w+) 0x[0-9a-f]+ <([^:,>]+):([0-9]+):([0-9]+)`)
	reLLVM   = regexp.MustCompile(`^\s*LLVMType:(.*)$`)
)

func baseName(typ string) string {
	t := typ
	for _, kw := range []string{"struct ", "class ", "union "} {
		t = strings.TrimPrefix(t, kw)
	}
	depth := 0
	var b strings.Builder
	for _, r := range t { // drop template arguments
		switch {
		case r == '<':
			depth++
		case r == '>':
			depth--
		case depth == 0:
			b.WriteRune(r)
		}
	}
	t = b.String()
	if i := strings.LastIndex(t, "::"); i >= 0 {
		t = t[i+2:]
	}
	return strings.TrimSpace(t)
}

// directivesPass writes {:executed [LINE ...]}: the lines of the main file's directives the preprocessor executed
// (#define, #undef, #include, #pragma inside a group that was taken), from clang -E -dD -dI, whose line markers
// give the main file's lines (doc/cpp/CPP-SURVEY.md, CP45 (a)).
func (j job) directivesPass(w io.Writer, errw io.Writer) error {
	full := append([]string{"-E", "-dD", "-dI", "-o", "-"}, j.args...)
	cmd := exec.Command(j.clang, full...)
	var eb bytes.Buffer
	cmd.Stderr = &eb
	p, err := cmd.StdoutPipe()
	if err != nil {
		return err
	}
	if err := cmd.Start(); err != nil {
		return err
	}
	sc := bufio.NewScanner(p)
	sc.Buffer(make([]byte, 1<<20), 1<<28)
	marker := regexp.MustCompile(`^# (\d+) "([^"]*)"`)
	in := false
	line := 0
	var lines []int
	seen := map[int]bool{}
	for sc.Scan() {
		t := sc.Text()
		if m := marker.FindStringSubmatch(t); m != nil {
			n, _ := strconv.Atoi(m[1])
			line = n - 1
			in = m[2] == j.opts.main
			line++
			continue
		}
		if !in {
			continue
		}
		// the line this output line stands for: markers count the next line
		cur := line
		line++
		if strings.HasPrefix(t, "#define ") || strings.HasPrefix(t, "#undef ") || strings.HasPrefix(t, "#include") || strings.HasPrefix(t, "#pragma") {
			if !seen[cur] {
				seen[cur] = true
				lines = append(lines, cur)
			}
		}
	}
	io.Copy(io.Discard, p)
	if err := cmd.Wait(); err != nil {
		return fmt.Errorf("the directives pass: %v", err)
	}
	fmt.Fprint(w, "{:executed [")
	for i, l := range lines {
		if i > 0 {
			fmt.Fprint(w, " ")
		}
		fmt.Fprint(w, l)
	}
	fmt.Fprintln(w, "]}")
	return nil
}

func (j job) layoutPass(w io.Writer, errw io.Writer) (int, error) {
	tmp, err := os.MkdirTemp("", "cxxdump-layout")
	if err != nil {
		return 0, err
	}
	defer os.RemoveAll(tmp)
	full := append([]string{"-S", "-emit-llvm", "-o", filepath.Join(tmp, "unit.ll"), "-Xclang", "-fdump-record-layouts"}, j.args...)
	cmd := exec.Command(j.clang, full...)
	cmd.Stderr = errw
	outp, err := cmd.Output()
	if err != nil {
		return 0, fmt.Errorf("the layouts pass: %v", err)
	}
	return writeLayouts(w, string(outp), j.opts.main, j.opts.edn), nil
}

// writeLayouts parses the layouts dump and writes one line per main-file record.
func writeLayouts(w io.Writer, dump, main string, edn bool) int {
	var asts []*astLayout
	var cur *astLayout
	mode := ""
	type irRec struct {
		kind, file string
		line, col  int
		name, llvm string
	}
	var ir *irRec
	n := 0
	flushIR := func() {
		if ir == nil || ir.file != main {
			ir = nil
			return
		}
		var a *astLayout
		for i := len(asts) - 1; i >= 0; i-- {
			if !asts[i].used && baseName(asts[i].typ) == ir.name {
				a = asts[i]
				break
			}
		}
		if a == nil {
			ir = nil
			return
		}
		a.used = true
		n++
		if edn {
			fmt.Fprintf(w, "{:layout {:record-kind %s :line %d :col %d :type %s :size %d :dsize %d :align %d", ednString(ir.kind), ir.line, ir.col, ednString(a.typ), a.size, a.dsize, a.align)
			if a.hasNV {
				fmt.Fprintf(w, " :nvsize %d :nvalign %d", a.nvsize, a.nvalign)
			}
			if len(a.vptr) > 0 {
				fmt.Fprintf(w, " :vptr %d", a.vptr[0])
			}
			fmt.Fprintf(w, " :bases [%s] :fields [%s] :llvm %s}}\n", strings.Join(a.bases, " "), strings.Join(a.fields, " "), ednString(ir.llvm))
		} else {
			fmt.Fprintf(w, "{\"layout\":{\"type\":%q,\"line\":%d,\"size\":%d,\"align\":%d,\"llvm\":%q}}\n", a.typ, ir.line, a.size, a.align, ir.llvm)
		}
		ir = nil
	}
	for _, l := range strings.Split(dump, "\n") {
		switch {
		case strings.HasPrefix(l, "*** Dumping AST Record Layout"):
			flushIR()
			mode, cur = "ast", nil
			continue
		case strings.HasPrefix(l, "*** Dumping IRgen Record Layout"):
			flushIR()
			mode, ir = "ir", &irRec{}
			continue
		}
		switch mode {
		case "ast":
			if m := reSizes.FindAllStringSubmatch(l, -1); m != nil && cur != nil && strings.Contains(l, "=") {
				for _, kv := range m {
					v, _ := strconv.ParseInt(kv[2], 10, 64)
					switch kv[1] {
					case "sizeof":
						cur.size = v
					case "dsize":
						cur.dsize = v
					case "align":
						cur.align = v
					case "nvsize":
						cur.nvsize, cur.hasNV = v, true
					case "nvalign":
						cur.nvalign = v
					}
				}
				continue
			}
			m := reMember.FindStringSubmatch(l)
			if m == nil {
				continue
			}
			off, _ := strconv.ParseInt(m[1], 10, 64)
			depth := len(m[4]) / 2
			text := m[5]
			if cur == nil {
				cur = &astLayout{typ: text}
				asts = append(asts, cur)
				continue
			}
			if depth != 1 { // the members of the record itself; a base's members are its own layout's
				continue
			}
			switch {
			case strings.HasSuffix(text, " vtable pointer)"):
				cur.vptr = append(cur.vptr, off)
			case strings.HasSuffix(text, "(primary base)") || strings.HasSuffix(text, "(base)") || strings.HasSuffix(text, "(primary virtual base)") || strings.HasSuffix(text, "(virtual base)"):
				i := strings.LastIndex(text, " (")
				fmt.Fprintf(&strBuf{&cur.bases}, "{:type %s :offset %d :primary %v :virtual %v}", ednString(text[:i]), off, strings.Contains(text, "primary"), strings.Contains(text, "virtual base"))
			default:
				i := strings.LastIndex(text, " ")
				f := fmt.Sprintf("{:name %s :decl %s :offset %d", ednString(text[i+1:]), ednString(text), off)
				if m[2] != "" {
					f += fmt.Sprintf(" :bits [%s %s]", m[2], m[3])
				}
				cur.fields = append(cur.fields, f+"}")
			}
		case "ir":
			if m := reRecord.FindStringSubmatch(l); m != nil {
				ir.kind, ir.file = m[1], m[2]
				ir.line, _ = strconv.Atoi(m[3])
				ir.col, _ = strconv.Atoi(m[4])
				f := strings.Fields(l)
				for i, x := range f {
					if (x == "struct" || x == "class" || x == "union") && i+1 < len(f) {
						ir.name = f[i+1]
					}
				}
			} else if m := reLLVM.FindStringSubmatch(l); m != nil && ir.llvm == "" {
				ir.llvm = m[1]
			}
		}
	}
	flushIR()
	return n
}

type strBuf struct{ to *[]string }

func (s *strBuf) Write(p []byte) (int, error) { *s.to = append(*s.to, string(p)); return len(p), nil }

// ---------------------------------------------------------------------------------------------
// The cache: DIR/KEY.out (the output), KEY.deps (each file the unit read with its SHA-256, from clang's -MD),
// KEY.stats (the numbers of the run that made it), KEY.err (clang's standard error) and KEY.status. A key is the
// SHA-256 of the filter's version, clang's version, the options, the arguments and the main file's bytes; an
// entry is used only when every file of its deps still has its digest.

func fileDigest(p string) (string, error) {
	b, err := os.ReadFile(p)
	if err != nil {
		return "", err
	}
	h := sha256.Sum256(b)
	return hex.EncodeToString(h[:]), nil
}

func clangVersion(clang string) string {
	out, err := exec.Command(clang, "--version").Output()
	if err != nil {
		return "unknown"
	}
	return strings.SplitN(string(out), "\n", 2)[0]
}

func (j job) key() (string, error) {
	h := sha256.New()
	fmt.Fprintf(h, "%s\x00%s\x00%v %v %v %v %v %v\x00", version, clangVersion(j.clang), j.opts.implicit, j.opts.edn, j.opts.norm, j.layouts, j.opts.instances, j.opts.main)
	for _, a := range j.args {
		h.Write([]byte(a))
		h.Write([]byte{0})
	}
	b, err := os.ReadFile(j.args[len(j.args)-1])
	if err != nil {
		return "", err
	}
	h.Write(b)
	return hex.EncodeToString(h.Sum(nil)), nil
}

// depPaths reads a make rule's prerequisites.
func depPaths(text string) []string {
	text = strings.ReplaceAll(text, "\\\n", " ")
	if i := strings.Index(text, ": "); i >= 0 {
		text = text[i+2:]
	}
	var out []string
	var cur strings.Builder
	for i := 0; i < len(text); i++ {
		c := text[i]
		switch {
		case c == '\\' && i+1 < len(text) && text[i+1] == ' ':
			cur.WriteByte(' ')
			i++
		case c == ' ' || c == '\n' || c == '\t':
			if cur.Len() > 0 {
				out = append(out, cur.String())
				cur.Reset()
			}
		default:
			cur.WriteByte(c)
		}
	}
	if cur.Len() > 0 {
		out = append(out, cur.String())
	}
	return out
}

func depsValid(depsFile string) bool {
	b, err := os.ReadFile(depsFile)
	if err != nil {
		return false
	}
	for _, l := range strings.Split(strings.TrimSpace(string(b)), "\n") {
		f := strings.SplitN(l, "\t", 2)
		if len(f) != 2 {
			return false
		}
		d, err := fileDigest(f[1])
		if err != nil || d != f[0] {
			return false
		}
	}
	return true
}

func copyTo(src, dst string) error {
	b, err := os.ReadFile(src)
	if err != nil {
		return err
	}
	if dst == "" {
		_, err = os.Stdout.Write(b)
		return err
	}
	return os.WriteFile(dst, b, 0o644)
}

func cached(dir string, j job, outName string) (stats, bool, error) {
	var st stats
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return st, false, err
	}
	k, err := j.key()
	if err != nil {
		return st, false, err
	}
	// 256 subdirectories by the key's first byte: a flat directory of some 80,000 entries makes ext4 refuse a rename (ENOSPC)
	if err := os.MkdirAll(filepath.Join(dir, k[:2]), 0o755); err != nil {
		return st, false, err
	}
	base := filepath.Join(dir, k[:2], k)
	st.key = k
	if status, err := os.ReadFile(base + ".status"); err == nil && depsValid(base+".deps") {
		st.status = strings.TrimSpace(string(status))
		if st.status == "ok" {
			if err := copyTo(base+".out", outName); err != nil {
				return st, false, err
			}
		} else {
			e, _ := os.ReadFile(base + ".err")
			st.stderr = string(e)
		}
		if b, err := os.ReadFile(base + ".stats"); err == nil {
			fmt.Sscanf(string(b), "{:json-bytes %d :out-bytes %d :kept %d :dropped %d", &st.in, &st.out, &st.kept, &st.dropped)
		}
		return st, true, nil
	}
	tmp := fmt.Sprintf("%s.%d.tmp", base, os.Getpid())
	fh, err := os.Create(tmp)
	if err != nil {
		return st, false, err
	}
	var errb bytes.Buffer
	st, runErr := j.do(fh, tmp+".d", &errb)
	st.key, st.cache = k, "miss"
	fh.Close()
	defer os.Remove(tmp + ".d")
	if st.status == "" || st.status == "filter-error" {
		os.Remove(tmp)
		return st, false, runErr
	}
	db, err := os.ReadFile(tmp + ".d")
	if err != nil { // no deps (clang stopped before writing them): the run is not cached
		var cerr error
		if st.status == "ok" {
			cerr = copyTo(tmp, outName)
		}
		os.Remove(tmp)
		st.stderr = errb.String()
		return st, false, cerr
	}
	paths := depPaths(string(db))
	sort.Strings(paths)
	var deps strings.Builder
	for _, p := range paths {
		d, err := fileDigest(p)
		if err != nil {
			continue
		}
		fmt.Fprintf(&deps, "%s\t%s\n", d, p)
	}
	os.WriteFile(base+".deps", []byte(deps.String()), 0o644)
	os.WriteFile(base+".err", errb.Bytes(), 0o644)
	if st.status == "ok" {
		if err := os.Rename(tmp, base+".out"); err != nil {
			return st, false, err
		}
		if err := copyTo(base+".out", outName); err != nil {
			return st, false, err
		}
	} else {
		os.Remove(tmp)
		st.stderr = errb.String()
	}
	writeStats(base+".stats", st)
	os.WriteFile(base+".status", []byte(st.status+"\n"), 0o644)
	return st, false, nil
}
