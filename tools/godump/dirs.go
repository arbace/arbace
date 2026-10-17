package main

// Directory tests of $GOROOT/test (-dirs): a test FILE.go whose action is compiledir, rundir,
// builddir, buildrundir or runindir has a directory FILE.dir of Go files forming several packages, which
// import each other by local paths ("./a"). Go's test driver (cmd/internal/testdir) groups the
// directory's .go files into packages by their package clause, in file order (each file its own
// package with -s), and compiles each with `go tool compile -D test -importcfg CFG`: a package
// other than main as -p test/STEM (STEM its first file's name without .go), so "./a" resolves
// to test/a; main as -p main. builddir and buildrundir compile every .go file as one package
// main (-D .), with the directory's .s files. The helper does the same: it compiles the
// packages other than main in the driver's order, so that the later ones type-check against the
// earlier ones' export data, and dumps every package. runindir is built by the go command in
// module mode: the driver copies FILE.dir into a module of its own, named by the directory's
// base name (FILE.dir), go line 1.14 or the action's -gomodversion, and runs `go run .` there;
// the helper lists that module's packages (go list -export -deps ./...) and dumps them as
// packages (doc/go/HELPER-NOTES.md, "Directory tests").

import (
	"bytes"
	"encoding/json"
	"fmt"
	"go/build"
	"go/parser"
	"go/token"
	"go/version"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"regexp"
	"slices"
	"strconv"
	"strings"
	"sync"
)

// dirActions are the directory tests' actions the helper takes; errorcheckdir and
// errorcheckandrundir are error tests (the program must not compile), runindir builds with the
// go command in module mode (not this mode).
var dirActions = map[string]bool{"compiledir": true, "rundir": true, "builddir": true, "buildrundir": true, "runindir": true}

// dirTest is a directory test: its action line and its packages in the driver's order.
type dirTest struct {
	test    string // the test file, relative to $GOROOT/test (or as given)
	dir     string // its .dir, as given
	action  string
	gcflags []string // the action line's compiler flags
	pkgs    []*dirPkg
	modVers string // runindir: the go line of the module
}

type dirPkg struct {
	name string // the package clause's name
	path string // gc's -p: test/STEM, or main
	stem string // the dump's key in the test's directory: the first file's name without .go
	//                 (runindir: the package's directory in the module, "" for its root)
	files  []string // base names, in the directory's order
	sFiles []string // builddir's .s files
	export string   // the compiled original's archive (packages other than main)
}

var packageRE = regexp.MustCompile(`(?m)^package ([\p{Lu}\p{Ll}\w]+)`)

// splitAction splits an action line as the test driver does (splitQuoted: fields, with double
// quotes grouping).
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

// readDirTest reads a test's action line and groups its directory into packages.
func readDirTest(ctx *build.Context, file string) (*dirTest, string, error) {
	src, err := os.ReadFile(file)
	if err != nil {
		return nil, "", err
	}
	var action string
	for rest := string(src); action == "" && rest != ""; {
		var line string
		line, rest, _ = strings.Cut(rest, "\n")
		if strings.HasPrefix(line, "//go:build") || strings.HasPrefix(line, "// +build") {
			continue
		}
		action = strings.TrimSpace(strings.TrimPrefix(line, "//"))
	}
	f := splitAction(action)
	if len(f) == 0 || !dirActions[f[0]] {
		return nil, "", fmt.Errorf("%s: action %q is not a directory test the helper takes (compiledir, rundir, builddir, buildrundir)", file, action)
	}
	t := &dirTest{test: file, dir: strings.TrimSuffix(file, ".go") + ".dir", action: f[0]}
	single := false
	t.modVers = "1.14"
	args := f[1:]
flags:
	for len(args) > 0 && strings.HasPrefix(args[0], "-") {
		switch a := args[0]; a {
		case "-s":
			single = true
		case "-0", "-1":
		case "-goexperiment":
			return nil, "", fmt.Errorf("%s: needs another GOEXPERIMENT", file)
		case "-gomodversion":
			args = args[1:]
			if len(args) > 0 {
				t.modVers = args[0]
			}
		case "-t", "-godebug":
			args = args[1:] // a timeout, the program's GODEBUG
		case "-ldflags":
			break flags // the rest are the linker's
		default:
			t.gcflags = append(t.gcflags, a)
		}
		if len(args) > 0 {
			args = args[1:]
		}
	}
	fdir, base := filepath.Split(file)
	if fdir == "" {
		fdir = "."
	}
	if ok, err := ctx.MatchFile(fdir, base); err != nil {
		return nil, "", err
	} else if !ok {
		return nil, "excluded", nil
	}
	if t.action == "runindir" {
		return t, "", nil // its packages are the go command's (runindirJobs)
	}
	ents, err := os.ReadDir(t.dir)
	if err != nil {
		return nil, "", err
	}
	var gos, ss []string
	for _, e := range ents {
		switch filepath.Ext(e.Name()) {
		case ".go":
			gos = append(gos, e.Name())
		case ".s":
			ss = append(ss, e.Name())
		}
	}
	if len(gos) == 0 {
		return nil, "", fmt.Errorf("%s: no Go files", t.dir)
	}
	if t.action == "builddir" || t.action == "buildrundir" {
		t.pkgs = []*dirPkg{{name: "main", path: "main", stem: strings.TrimSuffix(gos[0], ".go"), files: gos, sFiles: ss}}
		return t, "", nil
	}
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
			p = &dirPkg{name: pn, stem: strings.TrimSuffix(name, ".go")}
			if pn == "main" {
				p.path = "main"
			} else {
				p.path = path.Join("test", p.stem)
			}
			t.pkgs = append(t.pkgs, p)
			byName[pn] = p
		}
		p.files = append(p.files, name)
	}
	return t, "", nil
}

// localPrefix is gc's -D of the test's compiles.
func (t *dirTest) localPrefix() string {
	if t.action == "builddir" || t.action == "buildrundir" {
		return "."
	}
	return "test"
}

// dirJobs makes one job per package of each directory test, after compiling the packages other
// than main into a temporary directory (removed by the returned function). Tests the
// configuration excludes (the test file's build constraints and name) and tests whose originals
// do not compile are results.
func dirJobs(c *config, tests []string) ([]*job, []result, *exportMap, func(), error) {
	ctx := c.buildContext()
	var dts []*dirTest
	var results []result
	imports := map[string]bool{}
	fset := token.NewFileSet()
	for _, file := range tests {
		key := fileKey(c, strings.TrimSuffix(file, ".go")+".dir")
		t, status, err := readDirTest(ctx, file)
		if err != nil {
			results = append(results, result{key: key, status: "fail", msg: err.Error()})
			continue
		}
		if status != "" {
			results = append(results, result{key: key, status: status})
			continue
		}
		bad := ""
		for _, p := range t.pkgs {
			for _, name := range p.files {
				f, err := parser.ParseFile(fset, filepath.Join(t.dir, name), nil, parser.ImportsOnly)
				if err != nil {
					bad = err.Error()
					break
				}
				for _, s := range f.Imports {
					if ip, err := strconv.Unquote(s.Path.Value); err == nil && ip != "unsafe" && ip != "C" && !build.IsLocalImport(ip) {
						imports[ip] = true
					}
				}
			}
		}
		if bad != "" {
			results = append(results, result{key: key, status: "fail", msg: bad})
			continue
		}
		dts = append(dts, t)
	}
	var paths []string
	for p := range imports {
		paths = append(paths, p)
	}
	slices.Sort(paths)
	pkgs, err := c.goList(paths)
	if err != nil {
		return nil, nil, nil, nil, err
	}
	exports := newExportMap(pkgs)
	tmp, err := os.MkdirTemp("", "godump-dirs-")
	if err != nil {
		return nil, nil, nil, nil, err
	}
	cleanup := func() { os.RemoveAll(tmp) }
	var cfg bytes.Buffer
	var known []string
	for p := range exports.export {
		known = append(known, p)
	}
	slices.Sort(known)
	for _, p := range known {
		fmt.Fprintf(&cfg, "packagefile %s=%s\n", p, exports.export[p])
	}
	// compile the originals, the tests in parallel, a test's packages in order
	errs := make([]string, len(dts))
	var wg sync.WaitGroup
	next := make(chan int)
	for range max(1, *flagJobs) {
		wg.Go(func() {
			for i := range next {
				errs[i] = compileDirTest(c, dts[i], filepath.Join(tmp, strconv.Itoa(i)), cfg.Bytes())
			}
		})
	}
	for i := range dts {
		next <- i
	}
	close(next)
	wg.Wait()
	var jobs []*job
	for i, t := range dts {
		key := fileKey(c, t.dir)
		if t.action == "runindir" {
			js, err := runindirJobs(c, t, filepath.Join(tmp, strconv.Itoa(i)))
			if err != nil {
				results = append(results, result{key: key, status: "fail", msg: err.Error()})
				continue
			}
			jobs = append(jobs, js...)
			continue
		}
		if errs[i] != "" {
			results = append(results, result{key: key, status: "fail", msg: errs[i]})
			continue
		}
		local := map[string]string{}
		for _, p := range t.pkgs {
			if p.export != "" {
				local[p.path] = p.export
			}
		}
		for _, p := range t.pkgs {
			j := &job{key: key + "/" + p.stem, path: p.path, dir: t.dir, files: p.files, lang: pinnedLang,
				dirTest: t, dirPkg: p, local: local, importMap: map[string]string{}}
			for _, name := range p.files {
				f, err := parser.ParseFile(fset, filepath.Join(t.dir, name), nil, parser.ImportsOnly)
				if err != nil {
					continue // reported by the dump
				}
				for _, s := range f.Imports {
					if ip, err := strconv.Unquote(s.Path.Value); err == nil && build.IsLocalImport(ip) {
						j.importMap[ip] = path.Join(t.localPrefix(), ip)
					}
				}
			}
			jobs = append(jobs, j)
		}
	}
	return jobs, results, exports, cleanup, nil
}

// runindirJobs lists a runindir test's module as the driver builds it (a copy of the directory
// in a module named by its base name; go list -export -deps ./... there) and makes one job per
// package of the module, its files read from the original directory.
func runindirJobs(c *config, t *dirTest, tmp string) ([]*job, error) {
	mod := filepath.Base(t.dir)
	src := filepath.Join(tmp, "src", mod)
	err := filepath.WalkDir(t.dir, func(p string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		rel, _ := filepath.Rel(t.dir, p)
		if d.IsDir() {
			return os.MkdirAll(filepath.Join(src, rel), 0o777)
		}
		data, err := os.ReadFile(p)
		if err != nil {
			return err
		}
		return os.WriteFile(filepath.Join(src, rel), data, 0o666)
	})
	if err != nil {
		return nil, err
	}
	if err := os.WriteFile(filepath.Join(src, "go.mod"), fmt.Appendf(nil, "module %s\ngo %s\n", mod, t.modVers), 0o666); err != nil {
		return nil, err
	}
	cmd := c.goCmd("list", "-e", "-json", "-export", "-deps", "./...")
	cmd.Dir = src
	cmd.Env = append(cmd.Env, "GOPATH="+tmp, "GO111MODULE=on", "GOWORK=off", "GOPROXY=off", "PWD="+src)
	var stderr bytes.Buffer
	cmd.Stderr = &stderr
	out, err := cmd.Output()
	if err != nil {
		return nil, fmt.Errorf("go list in the module %s: %v: %s", mod, err, firstLine(stderr.String()))
	}
	var pkgs []*listPkg
	dec := json.NewDecoder(bytes.NewReader(out))
	for dec.More() {
		p := new(listPkg)
		if err := dec.Decode(p); err != nil {
			return nil, err
		}
		pkgs = append(pkgs, p)
	}
	local := map[string]string{}
	for _, p := range pkgs {
		if p.Export != "" {
			local[p.ImportPath] = p.Export
		}
	}
	var jobs []*job
	for _, p := range pkgs {
		if p.DepOnly || p.Module == nil || p.Module.Path != mod {
			continue
		}
		rel, err := filepath.Rel(src, p.Dir)
		if err != nil {
			return nil, err
		}
		rel = filepath.ToSlash(rel)
		key, dir := fileKey(c, t.dir), t.dir
		if rel != "." {
			key, dir = key+"/"+rel, filepath.Join(t.dir, filepath.FromSlash(rel))
		} else {
			rel = ""
		}
		lang := pinnedLang
		if p.Module.GoVersion != "" {
			lang = version.Lang("go" + p.Module.GoVersion)
		}
		t.pkgs = append(t.pkgs, &dirPkg{name: p.Name, path: p.ImportPath, stem: rel, files: p.GoFiles})
		jobs = append(jobs, &job{key: key, path: p.ImportPath, dir: dir, pkg: p, lang: lang,
			files: append(slices.Clone(p.GoFiles), p.CgoFiles...), importMap: p.ImportMap, listErr: p.Error,
			dirTest: t, local: local})
	}
	if len(jobs) == 0 {
		return nil, fmt.Errorf("go list in the module %s: no packages", mod)
	}
	return jobs, nil
}

// compileDirTest compiles a test's packages other than main, as the test driver does, into dir;
// "" or why the original does not compile.
func compileDirTest(c *config, t *dirTest, dir string, stdcfg []byte) string {
	if err := os.MkdirAll(filepath.Join(dir, "test"), 0o777); err != nil {
		return err.Error()
	}
	cfg := append([]byte(nil), stdcfg...)
	for _, p := range t.pkgs {
		if p.path != "main" {
			cfg = fmt.Appendf(cfg, "packagefile %s=%s\n", p.path, filepath.Join(dir, p.path+".a"))
		}
	}
	cfgFile := filepath.Join(dir, "importcfg")
	if err := os.WriteFile(cfgFile, cfg, 0o666); err != nil {
		return err.Error()
	}
	gotool := filepath.Join(c.goroot, "bin", "go")
	for _, p := range t.pkgs {
		if p.path == "main" {
			continue
		}
		args := []string{"tool", "compile", "-e", "-D", t.localPrefix(), "-importcfg=" + cfgFile,
			"-o", filepath.Join(dir, p.path+".a"), "-p", p.path}
		args = append(args, t.gcflags...)
		for _, name := range p.files {
			abs, _ := filepath.Abs(filepath.Join(t.dir, name))
			args = append(args, abs)
		}
		cmd := exec.Command(gotool, args...)
		cmd.Env = c.env
		cmd.Dir = dir
		if out, err := cmd.CombinedOutput(); err != nil {
			return fmt.Sprintf("go tool compile %s: %v: %s", p.path, err, firstLine(string(out)))
		}
		p.export = filepath.Join(dir, p.path+".a")
	}
	return ""
}

func firstLine(s string) string {
	l, _, _ := strings.Cut(strings.TrimSpace(s), "\n")
	return l
}
