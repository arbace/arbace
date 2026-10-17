package main

// Directory tests of GOROOT/test (gocmp -dirtest, gocmp tests -dirs): a test FILE.go whose
// action is compiledir, rundir, builddir or buildrundir, with a directory FILE.dir of several
// packages importing each other by local paths ("./a"). They are compiled as Go's test driver
// compiles them (go1.27.1, src/cmd/internal/testdir/testdir_test.go: goDirPackages,
// compileInDir, the importcfg, linkFile; builddir's case): the directory's .go files grouped
// into packages by their package clause in file order (each file its own package with -s),
// each compiled by `go tool compile -e -D test -importcfg CFG -p P`, P test/STEM (the first
// file's name without .go) or main; builddir and buildrundir compile every .go file as one
// package main (-D .) with the directory's assembly. The godump helper's -dirs mode groups
// them by the same rule (tools/godump/dirs.go).
//
// Both sides are compiled that way, each against its own earlier packages: the original's
// files from FILE.dir, the candidate's from its directory (the printed files of the same names),
// both flattened (flatten.go) unless -keep-positions, both positioned at the original's
// directory (-trimpath), with the action line's compiler flags, -S and -d=syncframes=0. The
// standard library comes from one importcfg (go list -export, its packages compiled with
// -d=syncframes=0, as the go command's levels compile them). The levels: tree per file, export
// and code per package, and run (rundir, buildrundir): the candidate linked as the driver links
// it, run, and its output compared with FILE.out (or empty), when the configuration runs on
// this machine (linux, the host's GOARCH, or another GOARCH through GOCMP_EXEC); skip
// otherwise.

import (
	"bytes"
	ctxpkg "context"
	"encoding/json"
	"errors"
	"fmt"
	"go/build"
	"go/build/constraint"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strings"
	"sync"
	"time"
)

// dirActions are the directory tests' actions compared; errorcheckdir and errorcheckandrundir
// are error tests (left out like the single-file errorcheck tests). runindir builds with the go
// command in module mode (compareRunindir; doc/go/ROUNDTRIP.md, "Directory tests").
var dirActions = map[string]bool{"compiledir": true, "rundir": true, "builddir": true, "buildrundir": true, "runindir": true}

type dirTest struct {
	file    string // FILE.go
	dir     string // FILE.dir
	action  string
	gcflags []string
	ldflags []string
	args    []string // rundir's program arguments
	pkgs    []*dirPkg
	asms    []string // builddir's .s files
	modVers string   // runindir's go line
}

type dirPkg struct {
	name, path, stem string
	files            []string
}

var packageRE = regexp.MustCompile(`(?m)^package ([\p{Lu}\p{Ll}\w]+)`)

// splitAction splits an action line as the test driver does (splitQuoted).
func splitAction(s string) []string {
	var out []string
	var b strings.Builder
	in, have := false, false
	for _, r := range s {
		switch {
		case r == '"':
			in, have = !in, true
		case !in && (r == ' ' || r == '\t'):
			if have {
				out = append(out, b.String())
				b.Reset()
				have = false
			}
		default:
			b.WriteRune(r)
			have = true
		}
	}
	if have {
		out = append(out, b.String())
	}
	return out
}

// actionLine is the test driver's: the first line that is not a build constraint.
func actionLine(src []byte) string {
	var action string
	for rest := string(src); action == "" && rest != ""; {
		var line string
		line, rest, _ = strings.Cut(rest, "\n")
		if constraint.IsGoBuild(line) || constraint.IsPlusBuild(line) {
			continue
		}
		action = strings.TrimSpace(strings.TrimPrefix(line, "//"))
	}
	return action
}

// readDirTest reads a directory test: why it is left out ("" when taken), or the test.
func readDirTest(file string) (*dirTest, string, error) {
	src, err := os.ReadFile(file)
	if err != nil {
		return nil, "", err
	}
	action := actionLine(src)
	f := splitAction(action)
	if len(f) == 0 || !strings.HasSuffix(f[0], "dir") {
		return nil, "not a directory test", nil
	}
	if !dirActions[f[0]] {
		return nil, "action " + f[0] + ": " + map[string]string{
			"errorcheckdir":       "an error test (the program must not compile)",
			"errorcheckandrundir": "an error test (a package must not compile)",
		}[f[0]], nil
	}
	t := &dirTest{file: file, dir: strings.TrimSuffix(file, ".go") + ".dir", action: f[0]}
	single := false
	t.modVers = "1.14"
	args := f[1:]
	for len(args) > 0 && strings.HasPrefix(args[0], "-") {
		switch a := args[0]; a {
		case "-s":
			single = true
		case "-0", "-1":
		case "-t":
			args = args[1:]
		case "-goexperiment":
			return nil, "needs another GOEXPERIMENT: " + action, nil
		case "-godebug":
			args = args[1:]
		case "-gomodversion":
			args = args[1:]
			if len(args) > 0 {
				t.modVers = args[0]
			}
		default:
			t.gcflags = append(t.gcflags, a)
		}
		args = args[1:]
	}
	t.args = args
	for i, fl := range t.gcflags {
		if fl == "-ldflags" {
			t.ldflags = t.gcflags[i+1:]
			t.gcflags = t.gcflags[:i]
			break
		}
	}
	if t.action == "runindir" {
		return t, "", nil // its packages are the go command's (compareRunindir)
	}
	ents, err := os.ReadDir(t.dir)
	if err != nil {
		return nil, "", err
	}
	var gos []string
	for _, e := range ents {
		switch filepath.Ext(e.Name()) {
		case ".go":
			gos = append(gos, e.Name())
		case ".s":
			t.asms = append(t.asms, e.Name())
		}
	}
	if len(gos) == 0 {
		return nil, "", fmt.Errorf("%s: no Go files", t.dir)
	}
	if t.builddir() {
		t.pkgs = []*dirPkg{{name: "main", path: "main", stem: strings.TrimSuffix(gos[0], ".go"), files: gos}}
		return t, "", nil
	}
	t.asms = nil
	byName := map[string]*dirPkg{}
	for _, name := range gos {
		data, err := os.ReadFile(filepath.Join(t.dir, name))
		if err != nil {
			return nil, "", err
		}
		m := packageRE.FindSubmatch(data)
		if m == nil {
			return nil, "", fmt.Errorf("%s: no package clause", filepath.Join(t.dir, name))
		}
		pn := string(m[1])
		p, ok := byName[pn]
		if single || !ok {
			p = &dirPkg{name: pn, stem: strings.TrimSuffix(name, ".go"), path: "main"}
			if pn != "main" {
				p.path = path.Join("test", p.stem)
			}
			t.pkgs = append(t.pkgs, p)
			byName[pn] = p
		}
		p.files = append(p.files, name)
	}
	return t, "", nil
}

func (t *dirTest) builddir() bool { return t.action == "builddir" || t.action == "buildrundir" }

func (t *dirTest) localPrefix() string {
	if t.builddir() {
		return "."
	}
	return "test"
}

func (t *dirTest) runs() bool {
	return t.action == "rundir" || t.action == "buildrundir" || t.action == "runindir"
}

// dirTestsList lists the directory tests of root for the configuration (gocmp tests -dirs).
func dirTestsList(ctxt *build.Context, root string, verbose bool) ([]string, error) {
	var out []string
	for _, d := range testDirs {
		names, err := filepath.Glob(filepath.Join(root, d, "*.go"))
		if err != nil {
			return nil, err
		}
		sort.Strings(names)
		for _, name := range names {
			rel, _ := filepath.Rel(root, name)
			rel = filepath.ToSlash(rel)
			_, why, err := readDirTest(name)
			if err != nil {
				return nil, err
			}
			if why == "" {
				dir, base := filepath.Split(name)
				ok, err := ctxt.MatchFile(dir, base)
				if err != nil {
					return nil, err
				}
				if !ok {
					src, _ := os.ReadFile(name)
					why = "excluded by its build constraints or name: " + firstConstraint(src)
				}
			}
			if why == "not a directory test" {
				continue
			}
			if why != "" {
				if verbose {
					fmt.Fprintf(os.Stderr, "%s\t%s\n", rel, why)
				}
				continue
			}
			out = append(out, rel)
		}
	}
	return out, nil
}

// stdImportcfg is the importcfg of the standard library for the configuration: GOCMP_STDCFG
// when set (bin/repr-go check writes it once), else made here.
var stdImportcfg = sync.OnceValues(func() (string, error) {
	if f := os.Getenv("GOCMP_STDCFG"); f != "" {
		return f, nil
	}
	out, stderr, err := runGo(".", "list", "-export", "-gcflags=all=-d=syncframes=0", "-f",
		"{{if .Export}}packagefile {{.ImportPath}}={{.Export}}{{end}}", "std")
	if err != nil {
		return "", fmt.Errorf("go list std: %v\n%s", err, stderr)
	}
	f, err := os.CreateTemp("", "gocmp-stdcfg-")
	if err != nil {
		return "", err
	}
	f.Write(out)
	f.Close()
	stdcfgTemp = f.Name()
	return f.Name(), nil
})

var stdcfgTemp string

// dirSide is one side of a directory test compiled.
type dirSide struct {
	export map[string]string // per package path, its archive
	asm    map[string]string // per package path, its -S listing
	exe    string            // the linked program (run level)
	err    string            // why it did not compile
	tmp    string
}

func goTool() string {
	return filepath.Join(filepath.Dir(goCmd), "go")
}

// tool runs go tool ARGS in dir, its stdout and an error with its output.
func tool(dir string, args ...string) ([]byte, error) {
	cmd := exec.Command(goTool(), append([]string{"tool"}, args...)...)
	cmd.Dir = dir
	var o, e bytes.Buffer
	cmd.Stdout, cmd.Stderr = &o, &e
	if err := cmd.Run(); err != nil {
		return o.Bytes(), fmt.Errorf("go tool %s: %v\n%s%s", args[0], err, firstLines(e.String(), 12), firstLines(o.String(), 4))
	}
	return o.Bytes(), nil
}

// buildDirSide compiles one side: srcDir holds its files (the original's .dir, or the
// candidate). With link, the main package is linked too.
func buildDirSide(t *dirTest, srcDir string, flat, link bool) *dirSide {
	s := &dirSide{export: map[string]string{}, asm: map[string]string{}}
	tmp, err := os.MkdirTemp("", "gocmp-dir-")
	if err != nil {
		s.err = err.Error()
		return s
	}
	s.tmp = tmp
	origDir, _ := filepath.Abs(t.dir)
	stdcfg, err := stdImportcfg()
	if err != nil {
		s.err = err.Error()
		return s
	}
	std, err := os.ReadFile(stdcfg)
	if err != nil {
		s.err = err.Error()
		return s
	}
	src := filepath.Join(tmp, "src")
	os.MkdirAll(filepath.Join(tmp, "test"), 0o777)
	os.MkdirAll(src, 0o777)
	cfg := append([]byte(nil), std...)
	cfg = append(cfg, '\n')
	for _, p := range t.pkgs {
		if p.path != "main" {
			cfg = fmt.Appendf(cfg, "packagefile %s=%s\n", p.path, filepath.Join(tmp, p.path+".a"))
		}
	}
	cfgFile := filepath.Join(tmp, "importcfg")
	if err := os.WriteFile(cfgFile, cfg, 0o666); err != nil {
		s.err = err.Error()
		return s
	}
	// the side's files, flattened, under tmp/src, positioned at the original's directory
	for _, p := range t.pkgs {
		for _, name := range p.files {
			b, err := os.ReadFile(filepath.Join(srcDir, name))
			if err != nil {
				s.err = "lacks " + name
				return s
			}
			if flat {
				b = flatten(filepath.Join(origDir, name), b)
			}
			if err := os.WriteFile(filepath.Join(src, name), b, 0o666); err != nil {
				s.err = err.Error()
				return s
			}
		}
	}
	var asmFlags []string
	if len(t.asms) > 0 {
		// as the driver: an empty go_asm.h for the symbol ABIs, the compiler writes the real one
		os.WriteFile(filepath.Join(tmp, "go_asm.h"), nil, 0o666)
		args := []string{"asm", "-p=main", "-gensymabis", "-I", tmp, "-o", filepath.Join(tmp, "symabis")}
		for _, a := range t.asms {
			args = append(args, filepath.Join(origDir, a))
		}
		if _, err := tool(tmp, args...); err != nil {
			s.err = err.Error()
			return s
		}
		asmFlags = []string{"-asmhdr", filepath.Join(tmp, "go_asm.h"), "-symabis", filepath.Join(tmp, "symabis")}
	}
	var mainObj string
	for _, p := range t.pkgs {
		out := filepath.Join(tmp, p.path+".a")
		if t.builddir() {
			out = filepath.Join(tmp, "go.o") // as the driver names it, for pack
		}
		args := []string{"compile", "-e", "-D", t.localPrefix(), "-importcfg=" + cfgFile, "-p", p.path,
			"-o", out, "-trimpath", src + "=>" + origDir, "-S", "-d=syncframes=0"}
		args = append(args, asmFlags...)
		args = append(args, t.gcflags...)
		if e := os.Getenv("GOCMP_GCFLAGS"); e != "" {
			args = append(args, strings.Fields(e)...)
		}
		for _, name := range p.files {
			args = append(args, filepath.Join(src, name))
		}
		listing, err := tool(tmp, args...)
		if err != nil {
			s.err = "does not compile:\n" + err.Error()
			return s
		}
		s.export[p.path] = out
		s.asm[p.path] = string(listing)
		if p.path == "main" {
			mainObj = out
		}
	}
	if !link || mainObj == "" {
		return s
	}
	if len(t.asms) > 0 {
		args := []string{"asm", "-p=main", "-e", "-I", tmp, "-o", filepath.Join(tmp, "asm.o")}
		for _, a := range t.asms {
			args = append(args, filepath.Join(origDir, a))
		}
		if _, err := tool(tmp, args...); err != nil {
			s.err = err.Error()
			return s
		}
		all := filepath.Join(tmp, "all.a")
		if _, err := tool(tmp, "pack", "c", all, mainObj, filepath.Join(tmp, "asm.o")); err != nil {
			s.err = err.Error()
			return s
		}
		mainObj = all
	}
	s.exe = filepath.Join(tmp, "a.exe")
	args := []string{"link", "-s", "-w", "-buildid=test", "-o", s.exe, "-importcfg=" + cfgFile}
	args = append(args, t.ldflags...)
	args = append(args, mainObj)
	if _, err := tool(tmp, args...); err != nil {
		s.err = "does not link:\n" + err.Error()
	}
	return s
}

// hostRuns: the configuration's programs run on this machine, natively or through
// GOCMP_EXEC (a command that runs a program of the configuration, as the test driver's
// go_GOOS_GOARCH_exec: qemu-aarch64 for linux/arm64 on amd64).
func hostRuns() bool {
	if os.Getenv("GOCMP_EXEC") != "" {
		return true
	}
	goos, goarch := os.Getenv("GOOS"), os.Getenv("GOARCH")
	if goos == "" {
		goos = runtime.GOOS
	}
	if goarch == "" {
		goarch = runtime.GOARCH
	}
	return goos == runtime.GOOS && goarch == runtime.GOARCH
}

// runProgram runs a linked test program as the driver does (in its directory, with the
// action's arguments), its output with \r\n as \n.
func runProgram(exe string, args []string) (string, error) {
	ctx, cancel := ctxpkg.WithTimeout(ctxpkg.Background(), 5*time.Minute)
	defer cancel()
	argv := append([]string{exe}, args...)
	if e := strings.Fields(os.Getenv("GOCMP_EXEC")); len(e) > 0 {
		argv = append(e, argv...)
	}
	cmd := exec.CommandContext(ctx, argv[0], argv[1:]...)
	cmd.Dir = filepath.Dir(exe)
	cmd.Env = append(os.Environ(), "GOENV=off", "GOFLAGS=", "PWD="+cmd.Dir)
	var buf bytes.Buffer
	cmd.Stdout, cmd.Stderr = &buf, &buf
	err := cmd.Run()
	if ctx.Err() != nil {
		err = errors.New("timed out")
	}
	return strings.ReplaceAll(buf.String(), "\r\n", "\n"), err
}

// compareDirTest compares the directory test file (FILE.go, its FILE.dir) with the candidate
// directory b.
func compareDirTest(file, b string, want map[string]bool) *result {
	res := &result{status: map[string]string{}, msg: map[string]string{}}
	set := func(level, status, msg string) {
		if want[level] {
			res.status[level], res.msg[level] = status, msg
		}
	}
	errAll := func(msg string) *result {
		for l := range want {
			set(l, "error", msg)
		}
		return res
	}
	t, why, err := readDirTest(file)
	if err != nil {
		return errAll(err.Error())
	}
	if why != "" {
		return errAll(file + ": " + why)
	}
	if st, err := os.Stat(b); err != nil || !st.IsDir() {
		return errAll("the candidate must be a directory: " + b)
	}
	if t.action == "runindir" {
		return compareRunindir(t, b, want)
	}
	link := want["run"] && t.runs() && hostRuns()
	var wg sync.WaitGroup
	var sA, sB, rA, rB *dirSide
	if want["export"] || want["code"] || want["run"] {
		// the program run is compiled from its own positions (a test may check its line
		// numbers: fixedbugs/issue29919.go), the levels export and code from the canonical
		// layout unless -keep-positions
		flat := !keepPositions
		wg.Add(2)
		go func() { defer wg.Done(); sA = buildDirSide(t, t.dir, flat, link && !flat) }()
		go func() { defer wg.Done(); sB = buildDirSide(t, b, flat, link && !flat) }()
		if link && flat {
			wg.Add(2)
			go func() { defer wg.Done(); rA = buildDirSide(t, t.dir, false, true) }()
			go func() { defer wg.Done(); rB = buildDirSide(t, b, false, true) }()
		}
	}
	if want["tree"] {
		var msgs []string
		status := "pass"
		for _, p := range t.pkgs {
			for _, name := range p.files {
				fa, err := parseFile(filepath.Join(t.dir, name))
				if err != nil {
					status, msgs = "error", append(msgs, err.Error())
					continue
				}
				fb, err := parseFile(filepath.Join(b, name))
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
		}
		set("tree", status, strings.Join(msgs, "\n"))
	}
	wg.Wait()
	if sA == nil {
		return res
	}
	defer os.RemoveAll(sA.tmp)
	defer os.RemoveAll(sB.tmp)
	if rA == nil {
		rA, rB = sA, sB
	} else {
		defer os.RemoveAll(rA.tmp)
		defer os.RemoveAll(rB.tmp)
	}
	if dir := os.Getenv("GOCMP_KEEP"); dir != "" {
		for side, s := range map[string]*dirSide{"a": sA, "b": sB} {
			var all strings.Builder
			for _, p := range t.pkgs {
				all.WriteString("# " + p.path + "\n" + s.asm[p.path])
			}
			os.WriteFile(filepath.Join(dir, side+".s"), []byte(all.String()), 0o644)
		}
	}
	if sA.err != "" {
		res.skip = true
		for _, l := range []string{"export", "code", "run"} {
			set(l, "skip", "original "+sA.err)
		}
		return res
	}
	if sB.err != "" && (len(sB.export) < len(t.pkgs)) {
		for _, l := range []string{"export", "code", "run"} {
			set(l, "fail", "candidate "+sB.err)
		}
		return res
	}
	if want["export"] {
		set("export", "pass", "")
		for _, p := range t.pkgs {
			d, err := compareExport(sA.export[p.path], sB.export[p.path])
			if err != nil {
				set("export", "error", p.path+": "+err.Error())
				break
			}
			if d != "" {
				set("export", "fail", p.path+": "+d)
				break
			}
		}
	}
	if want["code"] {
		set("code", "pass", "")
		for _, p := range t.pkgs {
			if d := compareAsm(sA.asm[p.path], sB.asm[p.path], keepPositions); d != "" {
				set("code", "fail", p.path+": "+d)
				break
			}
		}
	}
	if want["run"] {
		switch {
		case !t.runs():
			set("run", "skip", t.action+" does not run the program")
		case !link:
			set("run", "skip", "the configuration does not run on this machine")
		case rA.err != "":
			set("run", "skip", "original "+rA.err)
		case rB.err != "":
			set("run", "fail", "candidate "+rB.err)
		default:
			wantOut, err := os.ReadFile(strings.TrimSuffix(file, ".go") + ".out")
			if err != nil && !errors.Is(err, os.ErrNotExist) {
				set("run", "error", err.Error())
				break
			}
			outA, errA := runProgram(rA.exe, t.args)
			if errA != nil || outA != string(wantOut) {
				set("run", "skip", fmt.Sprintf("the original does not run as the test expects (%v): %s", errA, firstLines(outA, 4)))
				break
			}
			outB, errB := runProgram(rB.exe, t.args)
			switch {
			case errB != nil:
				set("run", "fail", fmt.Sprintf("the candidate fails: %v\n%s", errB, firstLines(outB, 8)))
			case outB != string(wantOut):
				set("run", "fail", "the candidate's output differs from the expected:\n"+firstLines(outB, 8))
			default:
				set("run", "pass", "")
			}
		}
	}
	return res
}

// A runindir test is built by the go command in module mode: the test driver copies FILE.dir
// into a module of its own, named by the directory's base name, go line 1.14 or the action's
// -gomodversion, and runs `go run .` there. Both sides are such copies, the candidate's with
// its printed .go files in place of the original's (both flattened unless -keep-positions);
// both are compiled by go list -export -trimpath (so that positions are module-relative on
// both sides), the export data and -S listings of every package of the module compared, and
// for the level run each is built from its own positions and run, its output compared with
// FILE.out.

// modSide is one side of a runindir test.
type modSide struct {
	src    string            // the module's directory
	pkgs   []modPkg          // the module's packages
	export map[string]string // per import path
	asm    map[string]string
	err    string
}

type modPkg struct {
	ImportPath string
	Dir        string
	GoFiles    []string
	DepOnly    bool
	Export     string
	Module     *struct{ Path string }
	Error      *struct{ Err string }
	DepsErrors []*struct{ Err string }
}

// makeModule copies the original directory into tmp/src/MOD with a go.mod; with files
// (relative path -> source), those files are written in place of the original's, and with
// flat flattened, positioned at the original's path; with nonce, every .go file gets a
// comment that misses the build cache.
func makeModule(t *dirTest, tmp string, files map[string]string, flat bool, nonce string) (string, error) {
	mod := filepath.Base(t.dir)
	src := filepath.Join(tmp, "src", mod)
	origDir, _ := filepath.Abs(t.dir)
	err := filepath.WalkDir(t.dir, func(p string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		rel, _ := filepath.Rel(t.dir, p)
		if d.IsDir() {
			return os.MkdirAll(filepath.Join(src, rel), 0o777)
		}
		from := p
		if f, ok := files[filepath.ToSlash(rel)]; ok {
			from = f
		}
		data, err := os.ReadFile(from)
		if err != nil {
			return err
		}
		if strings.HasSuffix(p, ".go") {
			if flat {
				data = flatten(filepath.Join(origDir, rel), data)
			}
			if nonce != "" {
				data = append(data, "\n// gocmp: compiled again, "+nonce+"\n"...)
			}
		}
		return os.WriteFile(filepath.Join(src, rel), data, 0o666)
	})
	if err != nil {
		return "", err
	}
	return src, os.WriteFile(filepath.Join(src, "go.mod"), fmt.Appendf(nil, "module %s\ngo %s\n", mod, t.modVers), 0o666)
}

// moduleEnv is the environment of the go command in a side's module.
func moduleEnv(tmp, src string) []string {
	return append(os.Environ(), "GOPATH="+tmp, "GO111MODULE=on", "GOWORK=off", "GOPROXY=off", "GOFLAGS=", "PWD="+src)
}

func listModule(t *dirTest, tmp string, files map[string]string, flat bool, nonce string) *modSide {
	s := &modSide{export: map[string]string{}, asm: map[string]string{}}
	src, err := makeModule(t, tmp, files, flat, nonce)
	if err != nil {
		s.err = err.Error()
		return s
	}
	s.src = src
	args := append([]string{"list", "-e", "-export", "-deps", "-trimpath",
		"-json=ImportPath,Dir,GoFiles,DepOnly,Export,Module,Error,DepsErrors"}, gcflags...)
	args = append(args, t.gcflags...)
	args = append(args, "./...")
	cmd := exec.Command(goCmd, args...)
	cmd.Dir = src
	cmd.Env = moduleEnv(tmp, src)
	var o, e bytes.Buffer
	cmd.Stdout, cmd.Stderr = &o, &e
	if err := cmd.Run(); err != nil {
		s.err = fmt.Sprintf("go list: %v\n%s", err, firstLines(e.String(), 12))
		return s
	}
	dec := jsonDecoder(o.Bytes())
	for dec.More() {
		var p modPkg
		if err := dec.Decode(&p); err != nil {
			s.err = err.Error()
			return s
		}
		if p.DepOnly || p.Module == nil || p.Module.Path != filepath.Base(t.dir) {
			continue
		}
		if p.Export == "" {
			msg := e.String()
			if p.Error != nil {
				msg = p.Error.Err + "\n" + msg
			}
			for _, de := range p.DepsErrors {
				msg += de.Err + "\n"
			}
			s.err = "does not compile:\n" + firstLines(msg, 12)
			return s
		}
		s.pkgs = append(s.pkgs, p)
		s.export[p.ImportPath] = p.Export
	}
	if len(s.pkgs) == 0 {
		s.err = "no package in the module"
		return s
	}
	s.asm = splitListings(e.String(), "")
	return s
}

// buildModule lists a side's module, again under a fresh cache key when the go command
// replayed a compile without its -S output (buildSide).
func buildModule(t *dirTest, files map[string]string, flat bool) (*modSide, string) {
	tmp, err := os.MkdirTemp("", "gocmp-mod-")
	if err != nil {
		return &modSide{err: err.Error()}, ""
	}
	s := listModule(t, tmp, files, flat, "")
	if s.err != "" {
		return s, tmp
	}
	for _, p := range s.pkgs {
		if strings.TrimSpace(s.asm[p.ImportPath]) == "" {
			os.RemoveAll(filepath.Join(tmp, "src"))
			return listModule(t, tmp, files, flat, fmt.Sprint(time.Now().UnixNano())), tmp
		}
	}
	return s, tmp
}

// runModule builds a side's module from its own positions (go build, as go run does) and runs
// it.
func runModule(t *dirTest, files map[string]string, args []string) (string, error) {
	tmp, err := os.MkdirTemp("", "gocmp-run-")
	if err != nil {
		return "", err
	}
	defer os.RemoveAll(tmp)
	src, err := makeModule(t, tmp, files, false, "")
	if err != nil {
		return "", err
	}
	exe := filepath.Join(src, "a.exe")
	cmd := exec.Command(goCmd, append([]string{"build", "-o", exe}, append(t.gcflags, ".")...)...)
	cmd.Dir = src
	cmd.Env = moduleEnv(tmp, src)
	if out, err := cmd.CombinedOutput(); err != nil {
		return "", fmt.Errorf("go build: %v\n%s", err, firstLines(string(out), 8))
	}
	return runProgram(exe, args)
}

func compareRunindir(t *dirTest, b string, want map[string]bool) *result {
	res := &result{status: map[string]string{}, msg: map[string]string{}}
	set := func(level, status, msg string) {
		if want[level] {
			res.status[level], res.msg[level] = status, msg
		}
	}
	// the original's packages and files, and the candidate's files in their place
	sA, tmpA := buildModule(t, nil, !keepPositions)
	defer os.RemoveAll(tmpA)
	if sA.err != "" {
		res.skip = true
		set("tree", "error", "original "+sA.err)
		for _, l := range []string{"export", "code", "run"} {
			set(l, "skip", "original "+sA.err)
		}
		return res
	}
	files := map[string]string{}
	var rels []string
	for _, p := range sA.pkgs {
		rel, _ := filepath.Rel(sA.src, p.Dir)
		for _, name := range p.GoFiles {
			r := filepath.ToSlash(filepath.Join(rel, name))
			files[r] = filepath.Join(b, filepath.FromSlash(r))
			rels = append(rels, r)
		}
	}
	if want["tree"] {
		var msgs []string
		status := "pass"
		for _, r := range rels {
			fa, err := parseFile(filepath.Join(t.dir, filepath.FromSlash(r)))
			if err != nil {
				status, msgs = "error", append(msgs, err.Error())
				continue
			}
			fb, err := parseFile(files[r])
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
				msgs = append(msgs, r+": "+d)
			}
		}
		set("tree", status, strings.Join(msgs, "\n"))
	}
	if !want["export"] && !want["code"] && !want["run"] {
		return res
	}
	for _, f := range files {
		if _, err := os.Stat(f); err != nil {
			for _, l := range []string{"export", "code", "run"} {
				set(l, "fail", "candidate lacks "+f)
			}
			return res
		}
	}
	sB, tmpB := buildModule(t, files, !keepPositions)
	defer os.RemoveAll(tmpB)
	if sB.err != "" {
		for _, l := range []string{"export", "code", "run"} {
			set(l, "fail", "candidate "+sB.err)
		}
		return res
	}
	if want["export"] {
		set("export", "pass", "")
		for _, p := range sA.pkgs {
			d, err := compareExport(sA.export[p.ImportPath], sB.export[p.ImportPath])
			if err != nil {
				set("export", "error", p.ImportPath+": "+err.Error())
				break
			}
			if d != "" {
				set("export", "fail", p.ImportPath+": "+d)
				break
			}
		}
	}
	if want["code"] {
		set("code", "pass", "")
		for _, p := range sA.pkgs {
			a, b := sA.asm[p.ImportPath], sB.asm[p.ImportPath]
			if strings.TrimSpace(a) == "" || strings.TrimSpace(b) == "" {
				set("code", "error", p.ImportPath+fmt.Sprintf(": empty -S listing (A %d bytes, B %d bytes)", len(a), len(b)))
				break
			}
			if d := compareAsm(a, b, keepPositions); d != "" {
				set("code", "fail", p.ImportPath+": "+d)
				break
			}
		}
	}
	if want["run"] {
		if !hostRuns() {
			set("run", "skip", "the configuration does not run on this machine")
			return res
		}
		wantOut, err := os.ReadFile(strings.TrimSuffix(t.file, ".go") + ".out")
		if err != nil && !errors.Is(err, os.ErrNotExist) {
			set("run", "error", err.Error())
			return res
		}
		outA, errA := runModule(t, nil, t.args)
		if errA != nil || outA != string(wantOut) {
			set("run", "skip", fmt.Sprintf("the original does not run as the test expects (%v): %s", errA, firstLines(outA, 4)))
			return res
		}
		outB, errB := runModule(t, files, t.args)
		switch {
		case errB != nil:
			set("run", "fail", fmt.Sprintf("the candidate fails: %v\n%s", errB, firstLines(outB, 8)))
		case outB != string(wantOut):
			set("run", "fail", "the candidate's output differs from the expected:\n"+firstLines(outB, 8))
		default:
			set("run", "pass", "")
		}
	}
	return res
}

func jsonDecoder(b []byte) *json.Decoder { return json.NewDecoder(bytes.NewReader(b)) }
