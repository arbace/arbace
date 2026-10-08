# g2c's program build: Go forms → a static Linux executable

A Go program held as Go forms ([SPEC.md](SPEC.md)) is printed back to Go (`arbace.g2c.print`,
[PRINTER-NOTES.md](PRINTER-NOTES.md)) into a temporary module and built by gc into a static
executable for `linux/amd64` or `linux/arm64`. This is B1a's step 0 ([B1-PLAN.md](B1-PLAN.md)):
the path by which Arbace itself will be built from forms.

```sh
bin/g2c build [--arch amd64|arm64] [-o OUT] [--layout gofmt|lines] [--line-file] [--work DIR]
              [--overlay FILE] DIR
bin/g2c build --print-only --work DIR [--tests] [--module PATH] [--layout gofmt|lines]
              [--line-file] SRC
bin/g2c-build-tests [--arch amd64|arm64]... [PROG...]
```

The logic is `arbace/g2c/build.clj` (`arbace.g2c.build`), run on Arbace's `target/stage2`
through `bin/lib/tools.bash` like the rest of `bin/g2c` (so `bin/build-arbace` first).
`bin/g2c build` without arguments still builds g2c's helper (`tools/godump`).

## The program's layout

`DIR` is a source root of Go forms, laid out as SPEC §4.1 lays out packages, plus an optional
module file:

```
DIR/
  program.edn                          optional: {:module "example.com/multi" :go "1.27"}
  go/example_com/multi.clj             package file of example.com/multi (ns, go/package, load)
  go/example_com/multi/main.clj        its main.go: in-ns, go/file, declarations
  go/example_com/multi/greet.clj       package file of example.com/multi/greet
  go/example_com/multi/greet/greet.clj
  go/example_com/multi/greet/banner.txt     a file greet embeds (:embed-files)
  go/example_com/multi/internal/wc.clj
  go/example_com/multi/internal/wc/wc.clj
  runs/                                (the tests' expected runs; ignored by the build)
```

- **Packages.** Every `.clj` file under `DIR/go` holding an `ns` form and a `go/package` form
  is a package file; it is collected with the files it `load`s (SPEC §4.1, as the printer
  does). Each package's `:path` is its import path. Exactly one package is named `main`: the
  program.
- **The module.** `program.edn` (read with the Clojure reader) gives go.mod's `module` path
  (`:module`) and `go` line (`:go`). Without it, or without a key, the module path is the main
  package's path and the go line the toolchain's language version (`go1.27.1` → `1.27`). Every
  package's path is the module path or below it; its directory in the module is the rest of
  the path (`example.com/multi/internal/wc` → `internal/wc`). Local packages import each other
  by these paths, as in any module (`[wc "example.com/multi/internal/wc"]`); Go's `internal`
  rule applies. The program has no dependencies outside the module and the standard library
  (`GOPROXY=off`; no `go.sum`). (Amendment B1, accepted 2026-10-08, folded into SPEC §4.1
  and §4.2.)
- **Other and embedded files** (`:other-files`, `:embed-files`, SPEC §9.5-9.6): with no
  original package directory, they live in the package's forms directory, the package file's
  path without `.clj` (`go/example_com/multi/greet/banner.txt`); embedded files are checked
  against the SHA-256 the forms record. (Amendment B2, accepted 2026-10-08, folded into SPEC
  §9.5, §9.6 and §12.4.)
- **Files.** One Go file per `go/file` form; when `go/package` has `:files`, the two must name
  the same files.
- **Configuration.** The target is the command's: `GOOS=linux`, `GOARCH` from `--arch`. A
  package's `:config` (SPEC §4.2) is not needed; its `:goos` and `:goarch` are not used, so one
  forms tree builds for both architectures (files for one architecture only carry `:build`
  lines, which gc evaluates). The main package's `:config` `:tags` and `:goexperiment`, when
  present, are passed to the build. (Amendment B4, accepted 2026-10-08, folded into SPEC
  §4.2.)

## The build

1. The module is printed into `WORK/mod` (`--work DIR`; by default a temporary directory,
   removed afterwards): `go.mod`, then per package its Go files and the files it names. The
   layout is gofmt's by default (readable Go whatever the forms' lines); `--layout lines`
   puts each form on its forms line (SPEC §12.3). `--line-file` prints in the lines layout
   with a `//line FILE.clj:1` directive first in each file, `FILE.clj` relative to the
   package's forms directory, so that gc's positions, and panics' tracebacks, name the forms
   file and its lines: `example.com/multi/greet/greet.clj:15` for
   `DIR/go/example_com/multi/greet/greet.clj` line 15. (Amendment B3, accepted 2026-10-08,
   folded into SPEC §12.3.)
2. In `WORK/mod`, with only this environment set by the build (the rest inherited, `GOCACHE`
   and `HOME` included):

   ```
   GOROOT=$G2C_GOROOT GOTOOLCHAIN=local GOFLAGS= GOWORK=off GO111MODULE=on GOPROXY=off
   CGO_ENABLED=0 GOEXPERIMENT= GOOS=linux GOARCH=amd64|arm64 GOAMD64=v1 | GOARM64=v8.0
   $G2C_GOROOT/bin/go build -trimpath -buildvcs=false [-tags ...] -o OUT <main package path>
   ```

   `OUT` defaults to the main package path's last element in the current directory, as
   `go build` names it. Go's output and exit status are the command's.

**Toolchain.** `$G2C_GOROOT/bin/go`, by default TamaGo's go1.27.1 at `/root/tamago-go`, the
toolchain of the rest of g2c (the round trip); it builds `linux` targets like upstream Go.
Alpine's own go1.27.1 (`/usr/lib/go`, `G2C_GOROOT=/usr/lib/go`) works too and passes the same
tests, but its executables differ from TamaGo's (code and data, not only the build ID: the two
trees differ in `cmd/` and std, TamaGo's additions and Alpine's patches); each is reproducible.

**Static.** `CGO_ENABLED=0` makes gc's internal linker write a static executable: `file` says
"statically linked", `readelf -l` shows no `INTERP` segment and `readelf -d` no dynamic
section. (The executables keep their symbols and DWARF; `-ldflags=-s -w` would strip them.)

**Reproducible.** Two builds of one program are byte-identical, from the same directory or
another, with a warm or a cold build cache (checked with a fresh `GOCACHE`): `-trimpath`
removes the host's paths (`example.com/multi/main.go`, `runtime/proc.go`), the
build ID is a hash of the inputs, `-buildvcs=false` keeps VCS stamps out (the temporary module
may lie inside a git checkout), and the environment above fixes everything else the build
info records (`go version -m`: the module path, `-trimpath=true`, `CGO_ENABLED=0`, `GOOS`,
`GOARCH`, `GOAMD64`). No `-buildid=` is needed. `--line-file` keeps this: its `//line` names
are relative to the package's directory, which `-trimpath` rewrites (a path outside it, such
as `../go/...`, would keep the host's directory).

## Overlays (amendment B5, accepted 2026-10-08)

`--overlay FILE` passes `-overlay FILE` (made absolute) to `go build`: a JSON file in Go's
overlay format, `{"Replace": {"/path/of/a/file/the/build/reads.go": "/path/of/its/replacement.go",
...}}`, which replaces or adds files of the build without changing the tree they belong to.
This is how the patched Go runtime of C2G-SPEC §9.3 (the goroutine-local slot: a field of `g`,
its clearing in `gdestroy`, the new file `arbace_local.go`) enters a build: the overlay maps
`$G2C_GOROOT/src/runtime/runtime2.go` and `proc.go` to the patched copies and adds
`arbace_local.go`, all printed from Go forms (the patch itself is jrt's phase 2). The option
is C2G-SPEC §16 Q19's amendment of this file, accepted with the spec. The build stays
reproducible: the overlay's files are inputs of gc's build IDs like any source. The patch's
forms are `overlay/go/runtime/` (jrt phase 2a, JRT-NOTES.md); `bin/jrt overlay` prints them and
writes the overlay file for `$G2C_GOROOT`, and `bin/jrt overlay --check` compares them with the
toolchain's runtime.

Checked (2026-10-08) with the experiment's overlay of `.tmp/c2g-exp/gls/` (C2G-SPEC §13.3): a
program of Go forms pulling `runtime.arbace_getLocal` with `//go:linkname` fails to link
without the overlay (`relocation target runtime.arbace_getLocal not defined`) and builds and
runs with it, for amd64 and for arm64 under `qemu-aarch64`.

## Libraries and tests (amendment B6, proposed 2026-10-08)

A library has no main package, so it is not built into an executable: `--print-only` prints
the module into `--work DIR`'s `mod/` (as step 1 of the build does) and stops, so that other
tools run `go build`, `go vet` or `go test` there. It requires no main package; `--module PATH`
gives the module path when `SRC` has no `program.edn`. `--tests` also prints each package's
`:test-files` (SPEC §4.2, §15 Q14), which the build leaves out otherwise (a test file is still
collected by the package file's `load`, but not printed). `bin/jrt`
([JRT-NOTES.md](JRT-NOTES.md)) builds, vets and tests jrt this way, with the environment of
"The build" above, and `go test -exec qemu-aarch64` for arm64.

## Cross-compilation and arm64

gc cross-compiles without a C toolchain: `--arch arm64` gives an `ARM aarch64` executable on
this amd64 host. It is tested under QEMU's user-mode emulation, `/usr/bin/qemu-aarch64 EXE
ARGS...`, which runs a static Linux arm64 executable as a process here (Linux system calls
translated, no system image needed); `bin/g2c-build-tests` runs the arm64 executables that way
and the amd64 ones natively, against the same expected files.

## The test programs and the tests

- `test/g2c/build/hello/`: one package, `main.go`: prints `hello, world`, then each argument
  (`arg N: "..."`), and exits with the number of arguments.
- `test/g2c/build/multi/`: the module `example.com/multi` (`program.edn`), three packages: the
  main package, `greet` (strings, a `//go:embed`ded banner, a panic on request) and
  `internal/wc` (counts lines, words and bytes of an `io.Reader` with `bufio.Scanner`; a type
  with a `String` method). `main` greets each argument in a goroutine of its own
  (`sync.WaitGroup`, a buffered channel of structs), counts standard input in another, prints
  the greetings in argument order (range over the closed channel) and the counts (`select`
  over a result and an error channel); without arguments it writes to standard error and exits
  with 2; an argument `panic` panics in its goroutine (exit 2, Go's traceback).

Each program's `runs/NAME.args` (arguments, one per line), `NAME.in` (standard input; absent:
empty), `NAME.out` (expected standard output), `NAME.err` (what standard error must begin with;
absent: empty) and `NAME.code` (expected exit code; absent: 0) define a run.
`bin/g2c-build-tests` builds every program for both architectures into
`.tmp/g2c-build-tests/ARCH/`, checks the executable static and of the right machine, builds it
again from a copy at another path (also with `--line-file`, twice) and compares the bytes, and
checks every run.

Result (2026-10-08, TamaGo's go1.27.1): 30 checks, all pass, in about 40 s (each build starts
Arbace's JVM: about 2-3 s).

| program | arch | size (bytes) | static | reproducible | runs |
|---|---|---|---|---|---|
| hello | amd64 | 2,357,129 | yes | yes | 2 of 2 |
| hello | arm64 | 2,350,285 | yes | yes | 2 of 2, under qemu-aarch64 |
| multi | amd64 | 2,433,452 | yes | yes | 3 of 3 |
| multi | arm64 | 2,372,712 | yes | yes | 3 of 3, under qemu-aarch64 |
