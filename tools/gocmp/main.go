// Command gocmp decides whether two Go sources of a package are the same
// program: the round-trip oracle of g2c (doc/go/ROUNDTRIP.md).
//
// Usage:
//
//	gocmp [-go CMD] [-levels tree,export,code] [-keep-positions] A B
//	gocmp reprint [-canon] SRCROOT DSTROOT
//	gocmp mutate -op swap|const|drop [-seed N] A OUTDIR
//	gocmp tests [-v] TESTDIR
//
// A is the original: a package directory, or a program of .go files of one
// directory (F.go, or F.go,G.go,... with the other files' names relative to
// F's directory) that the go command can build for the configuration in the
// environment (GOOS, GOARCH, GOROOT, ...). B is the candidate: a directory
// holding a file of the same name for each of A's files for the
// configuration, or the files of a program, in the same spelling as A. A
// program is compiled as Go's test driver compiles it, without -complete (a
// function may lack a body).
//
// The levels, each stronger than the one before:
//
//	tree    the go/ast trees, after normalizations that cannot change the program
//	export  gc's export data (types, method sets, constants, inlinable bodies)
//	code    gc's generated code, per symbol, without source positions
//
// For export and code, B is compiled in A's place through the go command's
// -overlay, so both sides have the same import path, file names, flags and
// dependencies (the original ones). Both sides compile from the canonical
// layout of their tokens (flatten.go) unless -keep-positions is given.
//
// Diagnostic knobs: GOCMP_GCFLAGS (more compiler flags for the package
// compared, both sides), GOCMP_KEEP=DIR (keep both -S listings as a.s, b.s),
// GOCMP_CANON_SKIP and GOCMP_CANON_ALSO (canon.go).
//
// Exit status: 0 all levels equal, 1 a level differs, 2 an error, 3 the
// original does not build for the configuration (export and code skipped).
// The last line of the output is "RESULT tree=pass export=fail code=fail"
// (pass, fail, error or skip per level).
package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"sync"
)

var (
	goCmd         string
	keepPositions bool
)

func defaultGo() string {
	if g := os.Getenv("GOCMP_GO"); g != "" {
		return g
	}
	if r := os.Getenv("GOROOT"); r != "" {
		return filepath.Join(r, "bin", "go")
	}
	return "go"
}

func main() {
	if len(os.Args) > 1 {
		switch os.Args[1] {
		case "reprint":
			os.Exit(mainReprint(os.Args[2:]))
		case "mutate":
			os.Exit(mainMutate(os.Args[2:]))
		case "tests":
			os.Exit(mainTests(os.Args[2:]))
		}
	}
	os.Exit(mainCompare(os.Args[1:]))
}

func usage(fs *flag.FlagSet, text string) func() {
	return func() {
		fmt.Fprint(os.Stderr, text)
		fs.PrintDefaults()
	}
}

const compareUsage = `usage: gocmp [-go CMD] [-levels tree,export,code] A B

A is the original package directory or .go file, B the candidate.
Exit status: 0 equal, 1 different, 2 error, 3 A does not build.
`

func mainCompare(args []string) int {
	fs := flag.NewFlagSet("gocmp", flag.ExitOnError)
	fs.StringVar(&goCmd, "go", defaultGo(), "the go command (default $GOCMP_GO, else $GOROOT/bin/go, else go)")
	levels := fs.String("levels", "tree,export,code", "levels to compare: tree, export, code (comma-separated) or all")
	fs.BoolVar(&keepPositions, "keep-positions", false, "compile with the sources' own positions (default: flattened; the code level then ignores DWARF)")
	fs.Usage = usage(fs, compareUsage)
	fs.Parse(args)
	if fs.NArg() != 2 {
		fs.Usage()
		return 2
	}
	want := map[string]bool{}
	for _, l := range strings.Split(*levels, ",") {
		switch l {
		case "all":
			want["tree"], want["export"], want["code"] = true, true, true
		case "tree", "export", "code":
			want[l] = true
		default:
			fmt.Fprintf(os.Stderr, "gocmp: unknown level %q\n", l)
			return 2
		}
	}
	res := compare(fs.Arg(0), fs.Arg(1), want)
	for _, l := range []string{"tree", "export", "code"} {
		if r, ok := res.status[l]; ok {
			if msg := res.msg[l]; msg != "" {
				fmt.Printf("%s: %s\n%s\n", l, r, indent(msg))
			} else {
				fmt.Printf("%s: %s\n", l, r)
			}
		}
	}
	var parts []string
	code := 0
	for _, l := range []string{"tree", "export", "code"} {
		r, ok := res.status[l]
		if !ok {
			continue
		}
		parts = append(parts, l+"="+r)
		switch r {
		case "fail":
			code = max(code, 1)
		case "error":
			code = 2
		}
	}
	if res.skip && code < 2 {
		code = 3
	}
	fmt.Printf("RESULT %s\n", strings.Join(parts, " "))
	return code
}

func indent(s string) string {
	return "  " + strings.ReplaceAll(strings.TrimRight(s, "\n"), "\n", "\n  ")
}

type result struct {
	status map[string]string // level -> pass, fail, error, skip
	msg    map[string]string
	skip   bool // the original does not build
}

// pkgFiles is the original's list of files for the configuration.
type pkgFiles struct {
	dir     string   // the directory the go command runs in
	pattern []string // "." or the files' names
	files   []string // base names
}

// programFiles splits a program F.go,G.go,... into its files: F's path, and
// the others in F's directory.
func programFiles(a string) []string {
	parts := strings.Split(a, ",")
	for i := 1; i < len(parts); i++ {
		parts[i] = filepath.Join(filepath.Dir(parts[0]), parts[i])
	}
	return parts
}

// listPackage asks the go command which files make up the original.
func listPackage(a string) (*pkgFiles, error) {
	files := programFiles(a)
	st, err := os.Stat(files[0])
	if err != nil {
		return nil, err
	}
	abs, err := filepath.Abs(files[0])
	if err != nil {
		return nil, err
	}
	if !st.IsDir() {
		p := &pkgFiles{dir: filepath.Dir(abs)}
		for _, f := range files {
			if _, err := os.Stat(f); err != nil {
				return nil, err
			}
			p.files = append(p.files, filepath.Base(f))
		}
		p.pattern = append([]string(nil), p.files...)
		return p, nil
	}
	if len(files) > 1 {
		return nil, errors.New("a package directory or the files of a program, not both")
	}
	out, stderr, err := runGo(abs, "list", "-e", "-json=ImportPath,GoFiles,CgoFiles,Error", ".")
	if err != nil {
		return nil, fmt.Errorf("go list: %v\n%s", err, stderr)
	}
	var p struct {
		ImportPath string
		GoFiles    []string
		CgoFiles   []string
		Error      *struct{ Err string }
	}
	if err := json.Unmarshal(out, &p); err != nil {
		return nil, err
	}
	if p.Error != nil {
		return nil, errors.New(p.Error.Err)
	}
	if len(p.CgoFiles) > 0 {
		return nil, fmt.Errorf("%s: cgo files are not supported", p.ImportPath)
	}
	return &pkgFiles{dir: abs, pattern: []string{"."}, files: p.GoFiles}, nil
}

func runGo(dir string, args ...string) (stdout []byte, stderr string, err error) {
	cmd := exec.Command(goCmd, args...)
	cmd.Dir = dir
	var o, e bytes.Buffer
	cmd.Stdout, cmd.Stderr = &o, &e
	err = cmd.Run()
	return o.Bytes(), e.String(), err
}

// built is one side compiled for the export and code levels.
type built struct {
	export string // the package archive
	asm    string // the -S listing
	err    string // why it did not compile
}

// gcflags are the compiler flags of both sides: -S prints the code of the
// package compared, and syncframes=0 writes self-describing export data, for
// every package (an export file copies parts of its dependencies' export data
// as they are). Neither changes the generated code (ROUNDTRIP.md, "Level 2").
var gcflags = []string{"-gcflags=all=-d=syncframes=0", "-gcflags=-S -d=syncframes=0"}

// buildSide compiles one side: sources maps each of the original's files to
// the file compiled in its place (the original itself for side A). With flat,
// every file is compiled with flattened positions (flatten.go).
func buildSide(p *pkgFiles, sources map[string]string, flat bool) built {
	args := append([]string{"list", "-e", "-export", "-json=ImportPath,Export,Error,DepsErrors"}, gcflags...)
	if p.pattern[0] != "." {
		// a program of GOROOT/test: as Go's test driver compiles it (go tool
		// compile without -complete); the flag only allows body-less functions
		// (noder/writer.go), it changes no code
		args[len(args)-1] += " -complete=false"
	}
	if extra := os.Getenv("GOCMP_GCFLAGS"); extra != "" {
		// a diagnostic knob: more flags for the package compared, both sides
		args[len(args)-1] += " " + extra
	}
	overlay := map[string]string{}
	var tmp string
	if flat {
		var err error
		tmp, err = os.MkdirTemp("", "gocmp-flat-")
		if err != nil {
			return built{err: err.Error()}
		}
		defer os.RemoveAll(tmp)
	}
	for orig, src := range sources {
		if !flat {
			if src != orig {
				overlay[orig] = src
			}
			continue
		}
		b, err := os.ReadFile(src)
		if err != nil {
			return built{err: err.Error()}
		}
		f := filepath.Join(tmp, filepath.Base(orig))
		if err := os.WriteFile(f, flatten(orig, b), 0o644); err != nil {
			return built{err: err.Error()}
		}
		overlay[orig] = f
	}
	if len(overlay) > 0 {
		f, err := os.CreateTemp("", "gocmp-overlay-*.json")
		if err != nil {
			return built{err: err.Error()}
		}
		defer os.Remove(f.Name())
		json.NewEncoder(f).Encode(struct{ Replace map[string]string }{overlay})
		f.Close()
		args = append(args, "-overlay="+f.Name())
	}
	args = append(args, p.pattern...)
	out, stderr, err := runGo(p.dir, args...)
	if err != nil {
		return built{err: fmt.Sprintf("go list: %v\n%s", err, stderr)}
	}
	var r struct {
		Export     string
		Error      *struct{ Err string }
		DepsErrors []*struct{ Err string }
	}
	if err := json.Unmarshal(out, &r); err != nil {
		return built{err: err.Error()}
	}
	if r.Export == "" {
		msg := stderr
		if r.Error != nil {
			msg = r.Error.Err + "\n" + msg
		}
		for _, e := range r.DepsErrors {
			msg += e.Err + "\n"
		}
		return built{err: "does not compile:\n" + firstLines(msg, 12)}
	}
	if dir := os.Getenv("GOCMP_KEEP"); dir != "" {
		// a diagnostic knob: keep the listings
		side := "a"
		for orig, src := range sources {
			if src != orig {
				side = "b"
			}
		}
		os.WriteFile(filepath.Join(dir, side+".s"), []byte(stderr), 0o644)
	}
	return built{export: r.Export, asm: stderr}
}

func firstLines(s string, n int) string {
	lines := strings.Split(strings.TrimRight(s, "\n"), "\n")
	if len(lines) > n {
		lines = append(lines[:n], fmt.Sprintf("... (%d more lines)", len(lines)-n))
	}
	return strings.Join(lines, "\n")
}

// candidateFile returns B's file for the original file name (the i-th of a
// program's files).
func candidateFile(b string, bIsDir bool, name string, i int) string {
	if bIsDir {
		return filepath.Join(b, name)
	}
	if fs := programFiles(b); i < len(fs) {
		return fs[i]
	}
	return b
}

func compare(a, b string, want map[string]bool) *result {
	res := &result{status: map[string]string{}, msg: map[string]string{}}
	fail := func(level, status, msg string) {
		res.status[level] = status
		res.msg[level] = msg
	}
	errAll := func(msg string) *result {
		for l := range want {
			fail(l, "error", msg)
		}
		return res
	}
	p, err := listPackage(a)
	if err != nil {
		return errAll(err.Error())
	}
	stB, err := os.Stat(programFiles(b)[0])
	if err != nil {
		return errAll(err.Error())
	}
	bIsDir := stB.IsDir()
	if bIsDir != (p.pattern[0] == ".") {
		return errAll("A and B must both be directories or both be files")
	}
	if !bIsDir && len(programFiles(b)) != len(p.files) {
		return errAll("A and B must name as many files")
	}
	overlay := map[string]string{}
	for i, name := range p.files {
		fb, err := filepath.Abs(candidateFile(b, bIsDir, name, i))
		if err != nil {
			return errAll(err.Error())
		}
		if _, err := os.Stat(fb); err != nil {
			return errAll(fmt.Sprintf("candidate lacks %s", name))
		}
		overlay[filepath.Join(p.dir, name)] = fb
	}
	sort.Strings(p.files)

	var wg sync.WaitGroup
	var bA, bB built
	if want["export"] || want["code"] {
		wg.Add(2)
		self := map[string]string{}
		for orig := range overlay {
			self[orig] = orig
		}
		go func() { defer wg.Done(); bA = buildSide(p, self, !keepPositions) }()
		go func() { defer wg.Done(); bB = buildSide(p, overlay, !keepPositions) }()
	}

	if want["tree"] {
		var msgs []string
		status := "pass"
		for _, name := range p.files {
			fa, err := parseFile(filepath.Join(p.dir, name))
			if err != nil {
				status, msgs = "error", append(msgs, err.Error())
				continue
			}
			fb, err := parseFile(overlay[filepath.Join(p.dir, name)])
			if err != nil {
				if status == "pass" {
					status = "fail"
				}
				msgs = append(msgs, "candidate does not parse: "+err.Error())
				continue
			}
			if d := compareFiles(fa, fb); d != "" {
				if status == "pass" {
					status = "fail"
				}
				msgs = append(msgs, name+": "+d)
			}
		}
		fail("tree", status, strings.Join(msgs, "\n"))
	}

	wg.Wait()
	if !want["export"] && !want["code"] {
		return res
	}
	if bA.err != "" {
		res.skip = true
		for _, l := range []string{"export", "code"} {
			if want[l] {
				fail(l, "skip", "original "+bA.err)
			}
		}
		return res
	}
	if bB.err != "" {
		for _, l := range []string{"export", "code"} {
			if want[l] {
				fail(l, "fail", "candidate "+bB.err)
			}
		}
		return res
	}
	if want["export"] {
		d, err := compareExport(bA.export, bB.export)
		switch {
		case err != nil:
			fail("export", "error", err.Error())
		case d != "":
			fail("export", "fail", d)
		default:
			fail("export", "pass", "")
		}
	}
	if want["code"] {
		if d := compareAsm(bA.asm, bB.asm, keepPositions); d != "" {
			fail("code", "fail", d)
		} else {
			fail("code", "pass", "")
		}
	}
	return res
}
