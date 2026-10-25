# The program build (`arbace.build.go`, `bin/build-go`): Go forms → a static Linux executable

> Names: since 2026-10-17 g2a is repr/go and j2a repr/java (doc/NAMING-SURVEY.md, "Decided"):
> `arbace.g2a.*` is `arbace.repr.go(.*)`, its build `arbace.g2a.build` is `arbace.build.go`;
> `bin/g2a convert` is `bin/repr-go read` (`bin/g2a read`: `read --dump`), `bin/g2a build`
> `bin/build-go`, `bin/g2a-print` `bin/repr-go print`, `bin/g2a-check` `bin/repr-go check`,
> `bin/g2a-convert-tests` and `-print-tests` `bin/repr-go test read` and `test print`,
> `bin/g2a-build-tests` `bin/build-go test`; `G2A_GOROOT` is `ARBACE_GOROOT`, the other `G2A_*`
> `REPR_GO_*`, `J2A_*` `REPR_JAVA_*`; `bin/j2a-check` is `bin/repr-java check`. a2g is j2g
> (`arbace.a2g.*`, `bin/a2g*`, `A2G-SPEC.md`, `A2G_*`, the printed `a2g_*`, `A2g_*`, `a2g/*`: `j2g`
> likewise), and class forms are Java forms (the front end keeps `arbace.classes.*`). This document
> uses the current names throughout, its dated passages included; branch names keep theirs.

A Go program held as Go forms ([SPEC.md](SPEC.md)) is printed back to Go (`arbace.repr.go.print`,
[PRINTER-NOTES.md](PRINTER-NOTES.md)) into a temporary module and built by gc into a static
executable for `linux/amd64` or `linux/arm64`. This is B1a's step 0 ([B1-PLAN.md](B1-PLAN.md)):
the path by which Arbace itself is built from forms (`bin/arbace-go`).

```sh
bin/build-go [--arch amd64|arm64] [--goos linux|tamago|darwin] [-o OUT] [--layout gofmt|lines]
              [--line-file] [--work DIR] [--overlay FILE] [--pgo FILE] [--go-flag FLAG]...
              [--print-cache DIR] [--root ROOT]... [--goos-dir DIR] DIR
bin/build-go --print-only --work DIR [--tests] [--module PATH] [--layout gofmt|lines]
              [--line-file] SRC
bin/build-go test [--arch amd64|arm64]... [PROG...]
```

The logic is `arbace/build/go.ae` (`arbace.build.go`), run on Arbace's Go executable like
the rest of `bin/repr-go` (`bin/lib/repr-go-host.bash`: `REPR_GO_EXE`, else the tools' host,
`bin/lib/host.bash`).
`bin/build-go` without arguments still builds repr/go's helper (`tools/godump`).

## The program's layout

`DIR` is a source root of Go forms, laid out as SPEC §4.1 lays out packages, plus an optional
module file:

```
DIR/
  program.edn                          optional: {:module "example.com/multi" :go "1.27"}
  go/example_com/multi.ae             package file of example.com/multi (ns, go/package, load)
  go/example_com/multi/main.ae        its main.go: in-ns, go/file, declarations
  go/example_com/multi/greet.ae       package file of example.com/multi/greet
  go/example_com/multi/greet/greet.ae
  go/example_com/multi/greet/banner.txt     a file greet embeds (:embed-files)
  go/example_com/multi/internal/wc.ae
  go/example_com/multi/internal/wc/wc.ae
  runs/                                (the tests' expected runs; ignored by the build)
```

- **Packages.** Every `.ae` file under `DIR/go` holding an `ns` form and a `go/package` form
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
  path without `.ae` (`go/example_com/multi/greet/banner.txt`); embedded files are checked
  against the SHA-256 the forms record. (Amendment B2, accepted 2026-10-08, folded into SPEC
  §9.5, §9.6 and §12.4.)
- **Data directories** (amendment M5 of J2G-SPEC §16, accepted 2026-10-09). A directory under
  `DIR/go` holding a file `.build-go-data` holds data, not forms: its `.ae` files (and those of its
  subdirectories) are not read as Go forms. `bin/j2g --program` marks so the main package's
  embedded namespace sources (`go/arbace/cmd/arbace/res/`, its `:embed-files`).
- **Files.** One Go file per `go/file` form; when `go/package` has `:files`, the two must name
  the same files.
- **Configuration.** The target is the command's: `GOOS` from `--goos` (default `linux`;
  `tamago` and `darwin` below and in BOX-NOTES.md, S1 and S4), `GOARCH` from `--arch`. A
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
   with a `//line FILE.ae:1` directive first in each file, `FILE.ae` relative to the
   package's forms directory, so that gc's positions, and panics' tracebacks, name the forms
   file and its lines: `example.com/multi/greet/greet.ae:15` for
   `DIR/go/example_com/multi/greet/greet.ae` line 15. (Amendment B3, accepted 2026-10-08,
   folded into SPEC §12.3.)
2. In `WORK/mod`, with only this environment set by the build (the rest inherited, `GOCACHE`
   and `HOME` included):

   ```
   GOROOT=$ARBACE_GOROOT GOTOOLCHAIN=local GOFLAGS= GOWORK=off GO111MODULE=on GOPROXY=off
   CGO_ENABLED=0 GOEXPERIMENT= GOOS=linux GOARCH=amd64|arm64 GOAMD64=v1 | GOARM64=v8.0
   $ARBACE_GOROOT/bin/go build -trimpath -buildvcs=false [-tags ...] -o OUT <main package path>
   ```

   `OUT` defaults to the main package path's last element in the current directory, as
   `go build` names it. Go's output and exit status are the command's. `--overlay FILE` adds
   `-overlay FILE` ("Overlays" below), `--pgo FILE` adds `-pgo FILE` (made absolute): a CPU
   profile for gc's profile-guided optimization (amendment O5, SPEED-NOTES.md); `--go-flag FLAG`
   (repeated) adds FLAG before `-o`: a test's build flags, `-gcflags=...`, `-tags=...` (2026-10-24,
   REAP2: `bin/goi check --aot` passes a `$GOROOT/test` action's flags, RPS17).

**Toolchain.** `$ARBACE_GOROOT/bin/go`, by default TamaGo's go1.27.1 at `/root/tamago-go`, the
toolchain of the rest of repr/go (the round trip); it builds `linux` targets like upstream Go.
Alpine's own go1.27.1 (`/usr/lib/go`, `ARBACE_GOROOT=/usr/lib/go`) works too and passes the same
tests, but its executables differ from TamaGo's (code and data, not only the build ID: the two
trees differ in `cmd/` and std, TamaGo's additions and Alpine's patches); each is reproducible.

**Host.** `bin/build-go` (and `bin/repr-go print`) runs on the native Go executable
(`REPR_GO_EXE`, else the tools' host: `ARBACE_HOST_EXE`, else stage 0 built from the Go seed),
with the output the JVM gave, byte for byte (standalone S3, [../STANDALONE-NOTES.md](../STANDALONE-NOTES.md),
"S3: repr/go on Go"; `bin/lib/repr-go-host.bash`). The JVM host (`G2A_HOST=jvm`) left with S4.

**Static.** `CGO_ENABLED=0` makes gc's internal linker write a static executable: `file` says
"statically linked", `readelf -l` shows no `INTERP` segment and `readelf -d` no dynamic
section. (The executables keep their symbols and DWARF; `-ldflags=-s -w` would strip them.
`bin/arbace-go` strips its executables (amendment SZ1; `ARBACE_GO_SYMBOLS=1` keeps them).)

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
This is how the patched Go runtime of J2G-SPEC §9.3 (the goroutine-local slot: a field of `g`,
its clearing in `gdestroy`, the new file `arbace_local.go`) enters a build: the overlay maps
`$ARBACE_GOROOT/src/runtime/runtime2.go` and `proc.go` to the patched copies and adds
`arbace_local.go`, all printed from Go forms (the patch itself is jrt's phase 2). The option
is J2G-SPEC §16 Q19's amendment of this file, accepted with the spec. The build stays
reproducible: the overlay's files are inputs of gc's build IDs like any source. The patch's
forms are `overlay/go/runtime/` (jrt phase 2a, JRT-NOTES.md; J2G-SPEC §9.3, amendment T15);
`bin/jrt overlay` prints them and writes the overlay file for `$ARBACE_GOROOT`, and `bin/jrt
overlay --check` compares them with the toolchain's runtime. Since goi's GR9a (GOI-SPEC.md, "As
built: GR9a-c"; GC65 (a)) the overlay also adds one file to package `reflect`,
`arbace_named.go` (`overlay/go/reflect/`): goi's interpreted named types made at run time as
runtime types of their own. The seed carries it as `overlay/reflect/` beside
`overlay/runtime/` (bin/lib/seed-go.bash: `overlay/PKG/F.go` is `GOROOT/src/PKG/F.go`; a seed
made before has the runtime's only).

## The print cache (standalone S6, SA146)

`--print-cache DIR` keeps the printed files of each package in `DIR`, keyed by the host's
identity (`REPR_GO_HOST_ID`, the executable's and `arbace/repr/go/`'s sources' hashes, set by
`bin/lib/repr-go-host.bash` for this option), the options that change the printing (`--layout`,
`--line-file`, the tests) and the package's forms files (their paths and contents): a package
the same host printed from the same forms is taken from the cache, not printed again. Without
`REPR_GO_HOST_ID` the option does nothing. `bin/arbace-go` passes `.tmp/repr-go-print-cache` and trims it
before each build of entries unused for six hours (`bin/lib/gocache.bash`,
`ARBACE_GOCACHE_KEEP_MIN`). The printed module is the same either way (STANDALONE-NOTES.md, "The
build's time (SA146-SA148)").

The cache also keeps a collect index per source root and host (`DIR/collect/`, a file named by
the SHA-256 of `REPR_GO_HOST_ID` and the root's path; SA190): which forms files are package files
(by their contents' SHA-256), and for each package file what collecting it found (its path, name
and `go/package` options) and the files it read with their SHA-256. A package whose files all
have the same SHA-256 is taken from the index without reading its forms; it is read and
collected only if it must be printed (its printed files not in the cache), and must then equal
the index's. Reading the forms was most of repr/go's time on the Go executable: a build whose
packages are all in the cache reads none. The printed files of a cached package are copied as
they are. Indexes are trimmed with the entries (by mtime, set when read).

Checked (2026-10-08) with the experiment's overlay of `.tmp/j2g-exp/gls/` (J2G-SPEC §13.3): a
program of Go forms pulling `runtime.arbace_getLocal` with `//go:linkname` fails to link
without the overlay (`relocation target runtime.arbace_getLocal not defined`) and builds and
runs with it, for amd64 and for arm64 under `qemu-aarch64`.

## Libraries and tests (amendment B6, accepted 2026-10-08)

A library has no main package, so it is not built into an executable: `--print-only` prints
the module into `--work DIR`'s `mod/` (as step 1 of the build does) and stops, so that other
tools run `go build`, `go vet` or `go test` there. It requires no main package; `--module PATH`
gives the module path when `SRC` has no `program.edn`. `--tests` also prints each package's
`:test-files` (SPEC §4.2, §15 Q14), which the build leaves out otherwise (a test file is still
collected by the package file's `load`, but not printed). `bin/jrt`
([JRT-NOTES.md](JRT-NOTES.md)) builds, vets and tests jrt this way, with the environment of
"The build" above, and `go test -exec qemu-aarch64` for arm64. (Amendment B6, proposed in
JRT-NOTES.md and accepted 2026-10-08 with J2G-SPEC's amendments, §16; it adds options to
`bin/build-go` only and changes no form, so nothing of SPEC is amended.)

## The box's guest and other roots (amendment BX6, accepted 2026-10-11)

`--goos tamago` builds the program for B1b's box (doc/go/BOX-NOTES.md, S1): `GOOS=tamago` with
TamaGo's `goos` module as `GOOSPKG` (`github.com/usbarmory/tamago` v1.27.1: the printed `go.mod`
requires it, imported by no package, and `go.sum` holds its two hashes, taken from Go's module
cache, `GOPROXY=off` as always, and checked against the ones `arbace.build.go/tamago-goos`
pins), linked with `-ldflags "-T 0x201000 -R 0x1000 -w"` (go-whim's addresses; the symbol table
kept, which the monitor reads; DWARF dropped). `--root ROOT` (repeated) adds another source root
of forms: its packages but a main one join the program (`box/`, whose board and `abi` the guest
imports, and whose launcher is a program of its own: `bin/build-go --module arbace box`).
`--print-only` takes `--root` too. No form changes, so SPEC is not amended. `bin/arbace-go
--build --box` then cuts the guest's symbol table to the three symbols the monitor reads
(`tools/elfsyms`; amendment BX12, BOX-NOTES.md, S2): 11 MB less, the rest of the file as gc wrote it.

## The standard library and the goos module from forms (amendment BX21, accepted 2026-10-11)

B1b S6 (doc/go/BOX-NOTES.md, S6; `tamago/`): `--print-only --module std` prints a root whose
packages are the standard library's: each at its import path, as GOROOT/src lays them out
(`runtime` in `mod/runtime/`), without a `go.mod` (GOROOT/src's `go.mod`, `go.sum` and
`vendor/modules.txt` are files of the root that `bin/tamago-vendor print` copies beside them).
`--goos-dir DIR`, with `--goos tamago`, takes TamaGo's `goos` module from DIR instead of Go's
module cache: the printed `go.mod` requires it and replaces it by DIR (absolute), and no
`go.sum` is written. Neither changes what is built from the same sources: the replace is not in
the executable's build information (the module provides only `runtime/goos`, which the go
command resolves through `GOOSPKG`), and `-trimpath` names the files by import path whichever
GOROOT or directory they are in. Packages are printed in parallel (`pmap`): std, 373 packages,
in about 20 s. No form changes, so SPEC is not amended by this.

`--goos darwin` (amendment BX61, doc/go/BOX-NOTES.md, S4) builds a program for macOS, cgo off:
the box's launcher for the Mac (`bin/box-mac-bundle`), whose Hypervisor.framework and libSystem
calls go through purego, vendored in `box/`; the build's environment is otherwise the Linux
build's (`GOOS=darwin`).

## Cross-compilation and arm64

gc cross-compiles without a C toolchain: `--arch arm64` gives an `ARM aarch64` executable on
this amd64 host. It is tested under QEMU's user-mode emulation, `/usr/bin/qemu-aarch64 EXE
ARGS...`, which runs a static Linux arm64 executable as a process here (Linux system calls
translated, no system image needed); `bin/build-go test` runs the arm64 executables that way
and the amd64 ones natively, against the same expected files.

## Reproducibility of Arbace's executables (2026-10-11)

The whole chain from a clean checkout is deterministic: `bin/jrt-convert` (its `--twice`; since
standalone S1 its output is committed, `jdk/`, and the chain starts at j2g),
`bin/j2g --program` (two runs, separate JVMs, give identical trees but `report.edn`'s timings),
the image of prepared namespaces, and `go build -trimpath`. Two removed nondeterminisms: j2g's
member tables followed the JVM's overload order for classes whose order changes from one JVM
process to the next (J2G-SPEC §5.11, RP1), and repr/java's headers named its numbered scratch copies
(JRT-SOURCES.md, convert). Checked at commit `fe7e878` (branch `determinism`): `bin/arbace-go
--build` twice from the same tree, the first moved aside, the second with an empty `GOCACHE`
(`ARBACE_GO_ARCHES=amd64`), then `--build --box` after each: the same bytes for
`amd64/arbace` (sha256 `b892e17f...`, also the gate's executable built a third time),
`box-amd64/arbace.elf` (`f00e3921...`), `box-amd64/arbace-box` (`313f654f...`) and the image
(`af936f3e...`). The version linked in (`bin/lib/version.bash`) names the commit, `-dirty` for a
dirty tree, so builds compare only from the same clean commit.

## The test programs and the tests

- `test/build/go/hello/`: one package, `main.go`: prints `hello, world`, then each argument
  (`arg N: "..."`), and exits with the number of arguments.
- `test/build/go/multi/`: the module `example.com/multi` (`program.edn`), three packages: the
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
`bin/build-go test` builds every program for both architectures into
`.tmp/build-go-tests/ARCH/`, checks the executable static and of the right machine, builds it
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

## A user's program (REAP7, 2026-10-25)

`bin/arbace-go --build-program DIR` builds a user's program into one static executable: its
Clojure namespaces compiled by c2j, its Java forms and the compiled namespaces translated by j2g
with the runtime's (the shipped build's compiled namespaces reused), its Go forms embedded, the
module built by `arbace.build.go` (this document's build); or, for a program of Go forms alone, that
build alone. It is the tool `bin/clojure-tests --aot` (REAP3) and `bin/java-check`'s compiled level
(JC2) build for their corpora, made general (doc/FULL-SUPPORT-SURVEY.md, "As built: REAP7").

```sh
bin/arbace-go --build-program DIR [--arch ARCH[,ARCH]] [-o OUT]
bin/arbace-go --build-program --check [NAME...]        # test/programs' samples (make programs-check)
make program DIR=...
```

```
DIR/
  program.edn     {:main hello.core}         a Clojure program: the namespace whose -main runs
                  {:main-class hello.Main}   a Java forms program: the class whose static main runs
                  {}, or no file             a Go program (go/ alone)
                  :name "hello"              the executable's name (default: DIR's name)
                  :module, :go               a Go program's go.mod lines (the build's program.edn)
  src/            Clojure namespaces (.ae) and resources: a directory of the class path
  java/           Java forms (doc/classes/SPEC.md §9.2): each package's ns file and its classes' files
  go/             Go forms (SPEC.md §4.1): an Arbace program's namespaces of Go forms (go.P), or the
                  Go program
  runs/           the samples' runs (NAME.args, .in, .out, .err, .code: "The test programs" below)
```

- **The entry.** `:main NS` runs `NS/-main` with the command line (the program's main package puts
  `-m NS` before it: j2g's `--entry-arg`); `:main-class CLASS` calls `CLASS.main(String[])` directly,
  as `java CLASS` does (j2g's `--entry`, which makes it a root). Neither starts rlwrap: the command
  line is the program's own. Without either, `go/` must hold a main package and `src/` and `java/`
  must be absent: the program is `bin/build-go DIR`'s.
- **Clojure.** Every namespace of `src/` (a file whose line begins `(ns`) is required on the shipped
  build's executable (the runtime's namespaces from their compiled loaders; the program's sources
  recorded as embedded ones, `ARBACE_PATH_FIRST`), `java/`'s package namespaces first, so that the
  namespaces' imports of its classes resolve; then c2j compiles the units of the program's own
  sources (`src/`'s and the `go/` sources they load), as `--build-java` compiles repr/java's
  reader. `src/`'s files are embedded at their paths (j2g's `--resources DIR`): at run time `RT.load`
  finds each namespace embedded and runs its compiled loader, as for the runtime's; a source c2j did
  not compile (printed `aot: not compiled:`) loads from its embedded text, evaluated. Only `.ae`
  sources are compiled (`ARBACE_PATH_FIRST` takes a `.ae` alone); `.clj` and `.cljc` are embedded and
  load from source.
- **Java forms.** `java/` is one of j2g's `--input` trees: its classes are the program's, translated
  with the runtime's (named `J_<binary name>_J`, J2G-SPEC §4.4). A namespace imports them
  (`(:import (mixed.text Words))`), as a JVM program imports its jar's classes; it does not require
  their package namespace, which is not embedded (at the REPL the package is required first).
- **Go forms.** In an Arbace program `go/`'s files are embedded under `go/` (`--resources DIR/go=go`):
  a namespace requires `go.mixed.caps` and goi interprets its package, its imports of std from goi's
  table (the shipped build's, `bin/goi table`), as at the REPL. A Go program (no entry) is built by
  `arbace.build.go` alone: no Arbace runtime, a few MB.
- **The base.** The shipped build whose compiled namespaces (`aot/`) the program reuses:
  `ARBACE_PROGRAM_BASE`, else `target/arbace-go`, else the gate's cache `.tmp/arbace-go-gate/amd64`,
  the first whose `aot.key` is the checkout's (`aot_key`: the runtime key and Arbace's sources) and
  whose executable's runtime is the checkout's; else a `--build` for the host's architecture first.
  Its executable compiles the namespaces and runs j2g and repr/go (`J2G_GO_EXE`, `REPR_GO_EXE`), as
  the compiled suite's does; j2g waits for `J2G_LOCK` when it is set (`bin/j2g`).
- **The output.** `target/arbace-go/programs/NAME/ARCH/NAME` (and `-o OUT`), stripped (`-s -w`,
  `ARBACE_GO_SYMBOLS=1` keeps the symbols) and linked with Arbace's version as `--build`'s; no image
  of prepared namespaces (the namespaces are compiled). The last line printed names it. It is cached
  by a key (`programs/NAME/key`: the base's executable and compiled namespaces, goi's table, DIR's
  files but `runs/`, the runtime, the build's scripts, Go's version): a build whose key is unchanged
  takes seconds. The intermediate trees (c2j's, j2g's, the work modules: about 300 MB) are removed
  after the build (`ARBACE_PROGRAM_KEEP=1` keeps them).
- **The samples** (`test/programs/`): `hello-clojure` (two namespaces, a record, a protocol, a
  transducer, standard input), `hello-java` (a package of two classes, an interface and nested
  classes, `java.io`'s readers, `TreeMap`), `hello-go` (a module of two packages), `mixed` (a
  namespace calling a Go forms package and a Java forms class, which calls back a Clojure fn).
  `--check` builds each for the host's architecture and runs its runs as `bin/build-go test` does,
  an Arbace program's with `ARBACE_LOAD_TIMES=1`: none of its own sources may load from source.
