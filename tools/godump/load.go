package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"go/build"
	"go/parser"
	"go/token"
	"go/version"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"slices"
	"strconv"
	"strings"
	"time"
)

// config is the one build configuration of a conversion, recorded in every dump.
type config struct {
	goos, goarch string
	tags         []string // given with -tags, sorted
	toolTags     []string // the toolchain's: architecture level and GOEXPERIMENT tags
	toolchain    string   // go env GOVERSION
	goroot       string   // go env GOROOT (not written: dumps name files relative to it)
	archLevel    string   // GOAMD64 or GOARM64
	goexperiment string   // go env GOEXPERIMENT (non-default experiments)
	env          []string // environment of the go command
}

func (c *config) goCmd(args ...string) *exec.Cmd {
	cmd := exec.Command(*flagGo, args...)
	cmd.Env = c.env
	cmd.Stderr = os.Stderr
	return cmd
}

func loadConfig() (*config, error) {
	c := &config{goos: *flagGOOS, goarch: *flagGOARCH}
	for _, t := range strings.Split(*flagTags, ",") {
		if t = strings.TrimSpace(t); t != "" {
			c.tags = append(c.tags, t)
		}
	}
	slices.Sort(c.tags)
	c.tags = slices.Compact(c.tags)
	c.env = append(os.Environ(), "GOOS="+c.goos, "GOARCH="+c.goarch, "CGO_ENABLED=0", "GOFLAGS=", "GOTOOLCHAIN=local")
	out, err := c.goCmd("env", "GOVERSION", "GOROOT", "GOAMD64", "GOARM64", "GOEXPERIMENT").Output()
	if err != nil {
		return nil, fmt.Errorf("%s env: %v", *flagGo, err)
	}
	v := strings.Split(string(out), "\n")
	if len(v) < 5 {
		return nil, fmt.Errorf("%s env: unexpected output %q", *flagGo, out)
	}
	c.toolchain, c.goroot, c.goexperiment = v[0], v[1], v[4]
	if c.toolchain != pinnedToolchain {
		return nil, fmt.Errorf("the go command is %s; the pinned toolchain is %s", c.toolchain, pinnedToolchain)
	}
	switch c.goarch {
	case "amd64":
		c.archLevel = v[2]
		n, err := strconv.Atoi(strings.TrimPrefix(c.archLevel, "v"))
		if err != nil {
			return nil, fmt.Errorf("GOAMD64=%s", c.archLevel)
		}
		for i := 1; i <= n; i++ {
			c.toolTags = append(c.toolTags, fmt.Sprintf("amd64.v%d", i))
		}
	case "arm64":
		c.archLevel = v[3]
		// GOARM64=v8.N[,lse][,crypto]: tags arm64.v8.0 ... arm64.v8.N (v9.N implies v8.N+5)
		lvl := strings.Split(c.archLevel, ",")[0]
		major, minor, ok := strings.Cut(strings.TrimPrefix(lvl, "v"), ".")
		mi, err := strconv.Atoi(minor)
		if !ok || err != nil || (major != "8" && major != "9") {
			return nil, fmt.Errorf("GOARM64=%s", c.archLevel)
		}
		if major == "9" {
			for i := 0; i <= mi; i++ {
				c.toolTags = append(c.toolTags, fmt.Sprintf("arm64.v9.%d", i))
			}
			mi += 5
		}
		for i := 0; i <= mi; i++ {
			c.toolTags = append(c.toolTags, fmt.Sprintf("arm64.v8.%d", i))
		}
	default:
		return nil, fmt.Errorf("GOARCH=%s: only amd64 and arm64 are configurations of g2c", c.goarch)
	}
	for _, t := range build.Default.ToolTags {
		if strings.HasPrefix(t, "goexperiment.") {
			c.toolTags = append(c.toolTags, t)
		}
	}
	slices.Sort(c.toolTags)
	return c, nil
}

// buildContext matches single files against the configuration as the go command would.
func (c *config) buildContext() *build.Context {
	ctx := build.Default
	ctx.GOOS, ctx.GOARCH, ctx.GOROOT = c.goos, c.goarch, c.goroot
	ctx.CgoEnabled = false
	ctx.Compiler = "gc"
	ctx.BuildTags = c.tags
	ctx.ToolTags = c.toolTags
	minor, _ := strconv.Atoi(strings.TrimPrefix(pinnedLang, "go1."))
	ctx.ReleaseTags = nil
	for i := 1; i <= minor; i++ {
		ctx.ReleaseTags = append(ctx.ReleaseTags, fmt.Sprintf("go1.%d", i))
	}
	return &ctx
}

// listPkg is the part of `go list -json` the helper uses.
type listPkg struct {
	Dir                string
	ImportPath         string
	Name               string
	Export             string
	Goroot             bool
	Standard           bool
	DepOnly            bool
	ForTest            string
	GoFiles            []string
	CgoFiles           []string
	CFiles             []string
	CXXFiles           []string
	MFiles             []string
	HFiles             []string
	FFiles             []string
	SFiles             []string
	SwigFiles          []string
	SwigCXXFiles       []string
	SysoFiles          []string
	IgnoredGoFiles     []string
	IgnoredOtherFiles  []string
	EmbedPatterns      []string
	EmbedFiles         []string
	TestGoFiles        []string
	XTestGoFiles       []string
	TestEmbedPatterns  []string
	TestEmbedFiles     []string
	XTestEmbedPatterns []string
	XTestEmbedFiles    []string
	Imports            []string
	ImportMap          map[string]string
	Module             *struct {
		Path      string
		GoVersion string
	}
	Error      *struct{ Err string }
	DepsErrors []*struct{ Err string }
}

// goList runs `go list -e -json -export -deps` for the configuration.
func (c *config) goList(args []string) ([]*listPkg, error) {
	if len(args) == 0 {
		return nil, nil
	}
	a := []string{"list", "-e", "-json", "-export", "-deps"}
	if *flagTests {
		a = append(a, "-test")
	}
	if len(c.tags) > 0 {
		a = append(a, "-tags", strings.Join(c.tags, ","))
	}
	a = append(a, "--")
	a = append(a, args...)
	t0 := time.Now()
	out, err := c.goCmd(a...).Output()
	if err != nil {
		return nil, fmt.Errorf("go list: %v", err)
	}
	var pkgs []*listPkg
	dec := json.NewDecoder(bytes.NewReader(out))
	for {
		p := new(listPkg)
		if err := dec.Decode(p); err == io.EOF {
			break
		} else if err != nil {
			return nil, fmt.Errorf("go list output: %v", err)
		}
		pkgs = append(pkgs, p)
	}
	fmt.Fprintf(os.Stderr, "godump: go list: %d packages, %.1f s\n", len(pkgs), time.Since(t0).Seconds())
	return pkgs, nil
}

// exportMap finds export data by package path (after the importing package's ImportMap).
type exportMap struct {
	export map[string]string
}

func newExportMap(pkgs []*listPkg) *exportMap {
	m := &exportMap{export: map[string]string{}}
	for _, p := range pkgs {
		if p.Export != "" {
			m.export[p.ImportPath] = p.Export
		}
	}
	return m
}

func packageJobs(c *config, args []string) ([]*job, *exportMap, error) {
	pkgs, err := c.goList(args)
	if err != nil {
		return nil, nil, err
	}
	byPath := map[string]*listPkg{}
	for _, p := range pkgs {
		byPath[p.ImportPath] = p
	}
	var jobs []*job
	for _, p := range pkgs {
		if p.DepOnly && !*flagDeps || p.ForTest != "" {
			continue // test variants are found from their package
		}
		if *flagTests && p.Name == "main" && strings.HasSuffix(p.ImportPath, ".test") && byPath[strings.TrimSuffix(p.ImportPath, ".test")] != nil {
			continue // a generated test main
		}
		lang := pinnedLang
		if p.Module != nil && p.Module.GoVersion != "" {
			lang = "go" + p.Module.GoVersion
		}
		j := &job{key: p.ImportPath, path: p.ImportPath, dir: p.Dir, pkg: p, lang: lang, importMap: p.ImportMap, listErr: p.Error}
		j.files = append(append(j.files, p.GoFiles...), p.CgoFiles...)
		if !*flagTests {
			jobs = append(jobs, j)
			continue
		}
		// with -tests: the package with its in-package _test.go files, as go test compiles it
		// ("p [p.test]"), and the external test package p_test ("p_test [p.test]")
		if v := byPath[p.ImportPath+" ["+p.ImportPath+".test]"]; v != nil {
			j.tests = true
			j.forTest = p.ImportPath
			j.files = slices.Clone(v.GoFiles)
			j.importMap, j.listErr = v.ImportMap, v.Error
		}
		jobs = append(jobs, j)
		if v := byPath[p.ImportPath+"_test ["+p.ImportPath+".test]"]; v != nil {
			xj := &job{key: p.ImportPath + "_test", path: p.ImportPath + "_test", dir: v.Dir, pkg: p, lang: lang,
				xtest: true, forTest: p.ImportPath, files: slices.Clone(v.GoFiles), importMap: v.ImportMap, listErr: v.Error}
			jobs = append(jobs, xj)
		}
	}
	return jobs, newExportMap(pkgs), nil
}

// fileJobs makes one job per single-file program. Files the configuration excludes (build
// constraints, file name) are results, not jobs. Their imports are listed together.
func fileJobs(c *config, files []string) ([]*job, []result, *exportMap, error) {
	ctx := c.buildContext()
	var jobs []*job
	var results []result
	imports := map[string]bool{}
	fset := token.NewFileSet()
	for _, arg := range files {
		// FILE or FILE,FILE...: one program (the extra files in FILE's directory)
		parts := strings.Split(arg, ",")
		key := fileKey(c, parts[0])
		dir, _ := filepath.Split(parts[0])
		if dir == "" {
			dir = "."
		}
		j := &job{key: key, path: "command-line-arguments", dir: dir, lang: pinnedLang}
		status, msg := "", ""
		for _, file := range parts {
			fdir, name := filepath.Split(file)
			if fdir == "" {
				fdir = "."
			}
			if filepath.Clean(fdir) != filepath.Clean(dir) {
				file = filepath.Join(dir, file)
				fdir, name = filepath.Split(file)
			}
			ok, err := ctx.MatchFile(fdir, name)
			if err != nil {
				status, msg = "fail", err.Error()
				break
			}
			if !ok {
				status = "excluded"
				break
			}
			f, err := parser.ParseFile(fset, file, nil, parser.ImportsOnly)
			if err != nil {
				status, msg = "fail", err.Error()
				break
			}
			for _, s := range f.Imports {
				if p, err := strconv.Unquote(s.Path.Value); err == nil && p != "unsafe" && p != "C" && !build.IsLocalImport(p) {
					imports[p] = true
				}
			}
			j.files = append(j.files, name)
		}
		if status != "" {
			results = append(results, result{key: key, status: status, msg: msg})
			continue
		}
		jobs = append(jobs, j)
	}
	var paths []string
	for p := range imports {
		paths = append(paths, p)
	}
	slices.Sort(paths)
	pkgs, err := c.goList(paths)
	if err != nil {
		return nil, nil, nil, err
	}
	return jobs, results, newExportMap(pkgs), nil
}

// fileKey names a single file's dump: its path relative to $GOROOT/test, $GOROOT or the
// working directory, else its absolute path without the leading separator.
func fileKey(c *config, file string) string {
	abs, err := filepath.Abs(file)
	if err != nil {
		abs = file
	}
	wd, _ := os.Getwd()
	for _, base := range []string{filepath.Join(c.goroot, "test"), c.goroot, wd} {
		if rel, err := filepath.Rel(base, abs); err == nil && !strings.HasPrefix(rel, "..") {
			return filepath.ToSlash(rel)
		}
	}
	return strings.TrimLeft(filepath.ToSlash(abs), "/")
}

// relDir names a package directory in a dump: relative to $GOROOT as "$GOROOT/...", else as is.
func relDir(c *config, dir string) string {
	if rel, err := filepath.Rel(c.goroot, dir); err == nil && !strings.HasPrefix(rel, "..") {
		return "$GOROOT/" + filepath.ToSlash(rel)
	}
	return filepath.ToSlash(dir)
}

func langTooNew(v string) bool {
	return v != "" && version.Compare(v, pinnedLang) > 0
}
