// Command godump is g2c's front end helper: it loads Go packages for one build configuration,
// type-checks them with go/types and writes, per package, one forms file for Arbace's reader
// (the Clojure reader with *read-eval* false, not strict EDN). The file holds the typed AST of
// every file, generically (one form per go/ast node), with what go/types decided as reader
// metadata, the package view (the build configuration, file lists, init order, method sets,
// struct layouts, directives) and the type table, every type mentioned as structured data. It
// decides nothing: the converter (arbace.g2c) does.
//
// Usage:
//
//	godump [flags] PACKAGE...      import paths or patterns, as `go list` takes them; with
//	                               -tests, each with its _test.go files, and its PATH_test
//	godump [flags] -files FILE...  single-file programs ($GOROOT/test); FILE,FILE... is one
//	                               program of several files (the others in the first one's directory)
//
// The output format is described in doc/go/HELPER-NOTES.md.
package main

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"slices"
	"strings"
	"sync"
	"time"
)

// The pinned Go: the language version the helper accepts, and the toolchain it runs with
// (TamaGo's release). Moving it is a recorded decision (journal, 2026-10-07).
const (
	pinnedLang      = "go1.27"
	pinnedToolchain = "go1.27.1"
)

var (
	flagGOOS   = flag.String("goos", "tamago", "target GOOS")
	flagGOARCH = flag.String("goarch", "amd64", "target GOARCH (amd64 or arm64)")
	flagTags   = flag.String("tags", "", "comma-separated build tags")
	flagOut    = flag.String("o", "godump.out", "output directory")
	flagJobs   = flag.Int("j", runtime.GOMAXPROCS(0), "packages dumped in parallel")
	flagGo     = flag.String("go", "go", "the go command (TamaGo's, for GOOS=tamago)")
	flagFiles  = flag.Bool("files", false, "arguments are single-file programs, not packages")
	flagDeps   = flag.Bool("deps", false, "also dump the dependencies of the named packages")
	flagTests  = flag.Bool("tests", false, "dump packages with their _test.go files, and external test packages (PATH_test)")
)

// A job is one package to dump.
type job struct {
	key   string   // output name, without .edn
	path  string   // import path given to go/types
	dir   string   // directory of the files
	files []string // file names in dir
	pkg   *listPkg // go list's view (nil for single files)
	lang  string   // language version

	importMap map[string]string     // the import paths of the files as go list resolved them
	listErr   *struct{ Err string } // go list's error for the package (or its test variant)
	tests     bool                  // with -tests: the package with its in-package test files
	xtest     bool                  // with -tests: the external test package of pkg
	forTest   string                // the package tested, for tests and xtest
}

// A result is one line of the summary.
type result struct {
	key    string
	status string // ok, errors, excluded, fail
	files  int
	bytes  int64
	dur    time.Duration
	errs   int
	msg    string
}

func main() {
	flag.Usage = func() {
		fmt.Fprintln(os.Stderr, "usage: godump [flags] PACKAGE... | godump [flags] -files FILE...")
		flag.PrintDefaults()
	}
	flag.Parse()
	if flag.NArg() == 0 {
		flag.Usage()
		os.Exit(2)
	}
	if v := runtime.Version(); !strings.HasPrefix(v, pinnedLang+".") && v != pinnedLang {
		fatalf("godump is built with %s; it must be built with %s (its go/types is the language's)", v, pinnedToolchain)
	}
	cfg, err := loadConfig()
	if err != nil {
		fatalf("%v", err)
	}
	var jobs []*job
	var results []result
	var exports *exportMap
	if *flagFiles {
		jobs, results, exports, err = fileJobs(cfg, flag.Args())
	} else {
		jobs, exports, err = packageJobs(cfg, flag.Args())
	}
	if err != nil {
		fatalf("%v", err)
	}
	if err := os.MkdirAll(*flagOut, 0o777); err != nil {
		fatalf("%v", err)
	}
	t0 := time.Now()
	out := make([]result, len(jobs))
	var wg sync.WaitGroup
	next := make(chan int)
	for range max(1, *flagJobs) {
		wg.Go(func() {
			for i := range next {
				out[i] = dump(cfg, exports, jobs[i])
			}
		})
	}
	for i := range jobs {
		next <- i
	}
	close(next)
	wg.Wait()
	results = append(results, out...)
	slices.SortFunc(results, func(a, b result) int { return strings.Compare(a.key, b.key) })
	var n, nerr, nfiles int
	var bytes int64
	for _, r := range results {
		fmt.Printf("%s\t%s\t%d\t%d\t%d\t%d", r.status, r.key, r.files, r.bytes, r.dur.Milliseconds(), r.errs)
		if r.msg != "" {
			fmt.Printf("\t%s", strings.ReplaceAll(r.msg, "\n", " | "))
		}
		fmt.Println()
		if r.status == "ok" || r.status == "errors" {
			n++
			nfiles += r.files
			bytes += r.bytes
		}
		if r.status == "errors" || r.status == "fail" {
			nerr++
		}
	}
	fmt.Fprintf(os.Stderr, "godump %s/%s: %d dumped (%d files, %.1f MB), %d with errors, %d results, %.1f s\n",
		cfg.goos, cfg.goarch, n, nfiles, float64(bytes)/1e6, nerr, len(results), time.Since(t0).Seconds())
	if nerr > 0 {
		os.Exit(1)
	}
}

func fatalf(format string, args ...any) {
	fmt.Fprintf(os.Stderr, "godump: "+format+"\n", args...)
	os.Exit(2)
}

// outPath is the output file of a job: the import path (or the file's path) as directories.
func outPath(key string) string {
	return filepath.Join(*flagOut, filepath.FromSlash(key)+".edn")
}
