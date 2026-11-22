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

## The build in bounded steps (2026-11-20, write-ahead)

The user (2026-11-20): javac's tool build is a real bottleneck; split it into digestible chunks.
Measured that day on the 62 GB host: `bin/javac-vendor tool`'s j2g reached 40.8 GB RSS
(`GOMEMLIMIT=40GiB` set by hand), then `bin/build-go`'s printer 27 GiB while a `go tool compile`
beside it took 29 GiB, and the kernel killed the driver (15:05 UTC); `javac-tool`'s `:mem-gib`
went 27 → 45 → 51 in a week. The gate's `go-build` has the same shape (the printer 34.5 GB, the
link 5 GB), and Java's compiled level's shards run the same two tools. The goal: every process
of the build at most about 12-16 GiB, one stage's process gone before the next starts, the
executable byte for byte what the build made before.

### The stages' memory (what each holds, and why it grows with the world)

| stage | process | what it holds at its peak | why it scales |
|---|---|---|---|
| 0. the namespaces compiled | the host, `host-compile*.ae` (when the base's `aot/` is not current) | every embedded namespace prepared and c2j's units | the embedded namespaces (GOGC 400, no limit); ran *beside* j2g (`--await`, SA160) |
| 1. j2g | `arbace -m arbace.j2g.main` | the world's Java forms, read with line metadata, and their analysis; then every Go file's forms (`files`) until the texts | the world: 45,838 classes for javac's tool (jdk/forms 28 MB, jdk/javac/forms 9 MB, the compiled namespaces 221 MB, the reader 27 MB of forms text); GOGC 400 lets the heap grow to `GOMEMLIMIT` (20 GiB by default, 40 by hand) |
| 2. the print | `arbace -m arbace.build.go` | `read-all` read *every* forms file of j2g's program (554 MB, 6,425 files for javac's tool) and the packages held them all through the print | a file's forms are about 49 times its bytes in memory (measured: `com_sun_tools_javac_comp_Attr.ae`, 1.24 MB, 61 MB of heap), so 554 MB is about 27 GB; GOGC 200, no limit; and the process stayed alive, holding that heap, while `go build` ran as its child |
| 3. the compile | `go build`, its `compile`s and `link` | gc's live heap is most of a compile's peak (doc/STANDALONE-NOTES.md, "What gc needs") and grows with the package: `arbace/jrt` is one package (4,276 files, 92 MB of Go for javac's tool), `arbace/lang` 67 MB, the compiled namespaces' packages up to 12 MB each | `-p` is GOMAXPROCS (64) and go 1.27 gives a compile up to GOMAXPROCS back-end workers (`-c`, cmd/go's `compilerConcurrency`), so jrt compiled with up to 64 workers beside the packages that do not import it, and the 240 compiled namespaces' packages up to 64 at once |

So the print and the compile were resident together (27 + 29 GiB), the host's compile beside j2g,
and in `--build` the two architectures' prints and compiles at once.

### The design

- **One stage at a time** (`bin/arbace-go`, `bin/build-go`): the host's compilation of the
  namespaces, then j2g (the compiled tree an input, read as `--await` read it: a tree with
  `packages.edn` is a late input, the same two rounds, so the same output), then each
  architecture's print, then its `go build`, then the next architecture (its print is the print
  cache's). `arbace.build.go --go-cmd FILE` prints the module and writes go build's command and
  environment into FILE (lines `dir`, `rm`, `env K=V`, `arg A`) and exits; `bin/build-go` runs it
  after (`bin/lib/go-build.bash`'s `go_build_plan`). javac's tool build's second build reuses the
  first's compiled namespaces (`aot_reusable`: `$dir/aot-javac` of the checkout's key).
- **The print in chunks** (`arbace.build.go`, `chunked?`): the program's files are read once for
  what decides package files and what a package's collection finds without its declarations
  (`light-forms`: the forms but those whose head is qualified, `X/package` and `X/file` kept); a
  package's forms are read when it is printed (`:collect`, its files read in parallel) and nothing
  holds them after; the packages printed at once are held to `ARBACE_PRINT_BUDGET_MB` (96 MiB) of
  forms files, the costliest that fits first, one over it alone (`budget-map`); `ARBACE_PRINT_JOBS`
  threads (16). The printer runs under `GOMEMLIMIT` `ARBACE_PRINT_GOMEMLIMIT` (10 GiB, capped by
  the gate's admission, `bin/gate-policy mem`), not under a limit the caller set for j2g. The
  printed module is the same (the same collect, the same print of each package; a package
  collected again must equal its first collection, as the collect index's must). A file is read
  twice when its package is printed (once light): about twice the reading time of a cold print.
- **The compile bounded** (`bin/lib/go-build.bash`, `bin/lib/go-toolexec`): `go build` under
  `GOMAXPROCS=ARBACE_GO_BUILD_PROCS` (16: go's `-p`) and `-toolexec bin/lib/go-toolexec`, which
  admits each compile by the Go bytes compiling beside it (`ARBACE_GO_BUDGET_MB`, 48 MiB, or alone),
  holds every compile's back end to `-c` at most `ARBACE_GO_C` (4), so that its memory follows its
  bytes, and runs a package of `ARBACE_GO_BIG_MB` (16 MiB) or more with `-c` at most
  `ARBACE_GO_BIG_C` (4) and `GOGC=ARBACE_GO_BIG_GOGC` (50). None of it
  changes the executable: `-p`, `-c`, GOGC and GOMAXPROCS are not in go's action IDs nor in the
  build info, and go asks a tool for its ID through the wrapper (`-V=full`, passed through), so the
  build IDs are the same (checked: `test/build/go/hello` byte for byte with and without, its
  packages cache hits across the two). The image's link and the profile-guided build
  (`bin/arbace-go`) take the same policy. `ARBACE_GO_BUILD_TIMES=FILE`: a line per compile and
  link, its package, Go bytes, seconds and peak RSS.
- `ARBACE_BUILD_CHUNKS=0` is the build as before (the whole program's forms read and held, go's
  own parallelism, the host's compile beside j2g, the architectures at once), but the printer
  still exits before go build: the baseline of the proof below.

**Expected peaks** (to be measured by one instrumented build of javac's tool): the print at most
the budget's forms (about 5 GB; jrt alone, 110 MB of forms, about 5.4 GB) under its 10 GiB limit;
the compile jrt's alone at `-c=4`, GOGC 50 (S5's fit, 0.63 GiB + 2.0 KiB per AST node at `-c=1`,
puts javac's tool's jrt, about three times S5's, near 15-20 GiB: a DECISION if it is over 16),
else 48 MiB of small packages; j2g's is the other branch's (`j2g-chunks-1120`).

### The contract between j2g and the print (for `j2g-chunks-1120`)

- The printer reads j2g's program tree (`OUT/prog`: `program.edn`, `go/<pkg>.ae` package files
  whose `load` forms name the files, `go/<pkg>/<file>.ae`, the `.build-go-data` resources) from disk,
  after j2g's process has exited; nothing else passes between them. j2g may write the tree in
  any order, by any number of processes, as long as the final tree is byte for byte what one run
  writes today (the print cache keys a package by its files' bytes; the proof compares the trees).
  Since j2g-stream-1120 (merged with this branch in n-merge-1120b, 2026-11-20) j2g writes the tree a window
  of classes at a time as they are merged (doc/go/J2G-SPEC.md §4.1, "The run in bounded memory"), the tree byte for
  byte the whole run's: the contract holds, and the printer is unchanged by it.
- The print's peak is its largest package's forms, about 49 times its files' bytes (jrt's
  110 MB: about 5.4 GB). A layout with jrt split into several Go packages (a DECISION of its own:
  doc/STANDALONE-NOTES.md, "What a split would take") would lower the print's and the compile's
  peaks together; the printer needs nothing new for it.
- Each package file's files are found by its `load` forms; the light pass drops a file's
  declarations, so a file needs nothing beyond what `collect` requires today (a declaration after
  a `go/file`).

### As built: the measured build (2026-11-20)

One instrumented `bin/arbace-go --build-java` of javac's tool (`.tmp/proof/` of the branch's worktree: a 2 s sampler of the
build's process tree by stage, `ARBACE_GO_BUILD_TIMES` for each compile), on a host shared with other agents' runs (the times
are long and noisy; the memory is the build's own). Its two tool builds (the first without the compiled reader, the second
with it) ran on the gate's executable `8d0e4648` as j2g's host (`ARBACE_AOT_ONEPASS=force`: no host of the checkout's
runtime), a fresh `GOCACHE` and print cache. The largest single process of each stage, the time of the first build's:

| stage | before (the brief; the baseline below, `ARBACE_BUILD_CHUNKS=0`) | in bounded steps |
|---|---|---|
| goi's table (`bin/goi table`, made once) | | 9.7 GiB, 6 min |
| the host's compilation of the namespaces | beside j2g (`--await`) | 29.9 GiB, 298 s, alone (GOGC 400, no limit; since the next commit under `GOMEMLIMIT` 16 GiB, whose effect is not yet measured) |
| j2g (unchanged here) | 40.8 GB at `GOMEMLIMIT=40GiB`; 28.6 GB the gate's `javac-tool` | 27.5 GiB (first build), 29.0 GiB (second), at its default 20 GiB, alone |
| the print | 27 GiB, resident through the compile; the baseline's 37.2 GiB, 333 s | 9.7 GiB, 172 s (cold), under its 10 GiB limit; gone before go build |
| the compile | 29 GiB (one `compile`) beside the printer; the baseline's largest 26.9 GiB, go build 1,549 s | `arbace/jrt` 24.0 GiB (92 MB of Go, 332 s) and `arbace/lang` 24.3 GiB (67 MB, 349 s), each alone; every other package under 2 GiB (the largest `aot_arbace_repr_llvm_verify` and `aot_arbace_cpp_sema`, 1.6 GiB), 48 MiB of Go at once; go build 933 s |
| the link | about 5 GB (the gate's executable); the baseline's 9.3 GiB | 9.4 GiB |
| the reader's compilation | | 4.8 GiB, 29 s |
| the build's tree, at any moment | 56 GiB (the printer and a compile at once; the kernel killed the driver) | 29.9 GiB (one stage at a time) |

**The proof of equal output.** The baseline (`.tmp/proof/base.sh`): the chunked run's own j2g program (j2g is not changed on
the branch, so a second j2g run would make the same tree; the coordinator: no redundant runs) printed and compiled as before,
`ARBACE_BUILD_CHUNKS=0` (the whole print, go's own `-p` 64 and `-c`), a fresh `GOCACHE` and print cache, then the image
prepared by its first link and linked as `image()` links it. Its printed module equals the chunked run's (6,637 files, `diff
-r` empty, the image's file included) and its executable is the chunked run's byte for byte (sha256
`01b16a8d8b5a2eae0b46ed73015160f656b44186aa358e0ab89da8058beb284f`, 670,052,512 bytes). The times are a shared host's (other
agents' runs beside both): the chunked print and compile were the faster ones there (172 against 333 s, 933 against 1,549 s).

**DECISION BC1: jrt's and lang's compiles (24 GiB each).** gc's live heap is most of a compile's peak, and both packages are
one compile each. Options:
- (a) *recommended now*: keep `-c=4` and GOGC 50 for packages of 16 MiB or more (24.0 and 24.3 GiB, 332 and 349 s, each
  alone): under j2g's 29 GiB as measured here (j2g-stream-1120, merged apart, lowers j2g's);
- (b) `-c=1` and GOGC 25 for them (`ARBACE_GO_BIG_C=1 ARBACE_GO_BIG_GOGC=25`, a setting): S5's ratios (doc/STANDALONE-NOTES.md,
  "What gc needs": 8.46 → 5.22 GiB, 52 → 125 s) put them near 17-18 GiB at about 2.4 times the time, 11 more minutes on the
  critical path (lang waits for jrt, every compiled namespace for lang); still over 16 GiB;
- (c) split `arbace/jrt` and `arbace/lang` into several Go packages each (j2g's output layout; "What a split would take": 5-10
  days at S5, the cycles closed by linkname pulls, outlined bodies or hooks): the only lever under 16 GiB per compile, and it
  shrinks the print's largest package too; the executable changes (a new layout, not the same bytes).
Recommended: (a) now; (c) if the 12-16 GiB goal holds once j2g's own run is under it; (b) only on a host where 24 GiB does not fit.

**DECISION BC2: the time one stage at a time costs.** The host's compilation no longer overlaps j2g's load (SA160 saved up to
its 5 minutes when both fit), go builds at `-p` 16 with `-c` 4 (jrt and lang alone at `-c` 4: 332 and 349 s, against go's own
`-c` up to 64), `--build`'s architectures one after another (the second's print from the cache, its compile alone) and a cold
print reads each file twice. Options: (a) *recommended*: as built, the memory first (the user's request); (b) the overlap back
for a host with room (`ARBACE_BUILD_CHUNKS=0`, or per stage); (c) a wider compile (`ARBACE_GO_BUILD_PROCS`, `ARBACE_GO_C`,
`ARBACE_GO_BUDGET_MB`) where memory allows.

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

## A mutant (2026-11-17: the Ghidra demo's A20, GH20 (a); round 52)

A program's sensitivity control (the Ghidra case study's §3.5 seeded mutant): one function's body
rewritten by a named, recorded rewrite, Go rebuilt alone, no j2g run; the mutant must fail at least one
of the tests that pass on the original. `bin/build-go --mutant FILE` (also with `--print-only`) applies
it; `bin/arbace-go --build-program DIR --mutant FILE` (`make program-mutant`) applies it to a kept
program, its forms as j2g printed them (`ARBACE_PROGRAM_KEEP=1`'s `programs/NAME/j2g/prog`, with the
same flags as the program's build: the lines layout, the overlay, x509roots, the program's `go/` root,
the link flags), into `programs/NAME/mutant-ARCH/NAME`; a Go program (no `:main`, no `:main-class`) from
DIR itself.

- **The hook: the forms.** `arbace.build.go/apply-mutant` rewrites the collected package's declaration
  before `print-module` prints it, so the rewrite is a Go form, names a declaration as the forms do
  (package path, name, receiver type), keeps the head (doc, type parameters, parameters, results and
  their metadata) and is printed by repr/go like every other form. The printed text (an edit of
  `work-ARCH/mod/...go` and `go build` by hand) was the other hook considered: no reprint, but a
  rewrite by line or by text, which positions and `--line-file`'s layout make fragile and which no
  printer checks. The mutated package is printed past the print cache, read and written (its forms
  files are the original's, so the cache's key would be theirs): the original's next build prints the
  original.
- **The record** (one map, the Clojure reader's; `read-mutant`): `:name` (in the panic's message and
  the applied record), `:package` (the import path), `:func` (as the forms spell it, j2g's mangled
  names included), `:receiver` (a `go/method`'s receiver type without `*` and type arguments; absent: a
  `go/func`), `:file` (the `go/file`, when needed), `:rewrite`, `:forms` (`:prepend`'s and
  `:replace-body`'s Go forms, in the file's context: its imports and the parameters' names),
  `:sha256` (the original declaration's digest, its form printed with its metadata: another is an error,
  the kept program changed since the rewrite was recorded) and `:why` (what it breaks, which tests reach
  it). Exactly one declaration must match; an `^:extern` one is refused.
- **The named rewrites**: `:panic` (`(panic "arbace mutant: NAME")` before the body, the body kept,
  unreachable, so its imports and variables stay used: any test that reaches it fails), `:prepend`
  (`:forms` before the body, such as an early `return` on a condition) and `:replace-body` (`:forms` in
  place of the body: a value off by a field, the subtle mutant a sensitivity control wants).
- **The applied record**, `OUT.mutant.edn` beside the executable (`--print-only`: `WORK/mutant.edn`):
  the record, its path, the package, the Go file, the forms file, the declaration, the digests before
  and after, and both forms; one line `build-go: mutant ...` on standard error.
- **Tests.** `bin/build-go mutant-test` (`test/build/mutant_test.ae` on the tools' host, no Go build, 29
  checks, about 350 MB): the record's refusals, the declaration found by name, receiver and file (and
  every miss refused), each rewrite's form, the digest pin, a package from the collect index, the printed
  `main.go` of `test/build/go/hello` with each mutant of `test/build/mutants/`; `--build` (`make
  build-go-mutant-test`) also builds hello and its two mutants (Go alone, about 350 MB, 12 s) and runs
  them on hello's runs: the original passes both, `hello-greeting` (`:replace-body`) and `hello-panic`
  fail both.
- **On a test program** (2026-11-17): `bin/arbace-go --build-program test/programs/hello-go --mutant
  test/programs-mutants/hello-go-wc-string.edn` (the method `(Counts).String` of
  `example.com/hellogo/wc`, the lines' count printed for the words', 4 s, 450 MB): the run `noargs`,
  which prints no counts that differ, passes; `args` fails (`lines 2 words 2` for `lines 2 words 7`).
- **A large program's** (the Ghidra demo's G2, by day: the mutant built from G2's kept program, about
  5-10 GiB and 10-20 minutes, Go alone, then run over the classes that reach the rewritten method):
  `ARBACE_PROGRAM_KEEP=1 bin/arbace-go --build-program DIR` once (the program kept), then
  `bin/heavy-lock bin/arbace-go --build-program DIR --mutant RECORD`, the record's `:sha256` taken from a
  first run's refusal or applied record.

## A user's program (REAP7, 2026-10-25; REAP7b)

`bin/arbace-go --build-program DIR` builds a user's program into one static executable: its
Clojure namespaces compiled by c2j, its Java forms and the compiled namespaces translated by j2g
with the runtime's (the shipped build's compiled namespaces reused), its Go forms compiled
through a goi table of its own (REAP7b), the
module built by `arbace.build.go` (this document's build); or, for a program of Go forms alone, that
build alone. It is the tool `bin/clojure-tests --aot` (REAP3) and `bin/java-check`'s compiled level
(JC2) build for their corpora, made general (doc/FULL-SUPPORT-SURVEY.md, "As built: REAP7", "As
built: REAP7b").

```sh
bin/arbace-go --build-program DIR [--arch ARCH[,ARCH]] [-o OUT]
bin/arbace-go --build-program --check [NAME...]        # test/programs' samples (make programs-check;
                                                       # bin/gate runs mixed, --full all of them)
make program DIR=...
```

```
DIR/
  program.edn     {:main hello.core}         a Clojure program: the namespace whose -main runs
                  {:main-class hello.Main}   a Java forms program: the class whose static main runs
                  {}, or no file             a Go program (go/ alone)
                  :name "hello"              the executable's name (default: DIR's name)
                  :module, :go               a Go program's go.mod lines (the build's program.edn)
                  :roots :entry              the program's own Java classes rooted from the entry, the
                                             services and the metadata alone (default :all, every member)
  src/            Clojure namespaces (.ae, .clj, .cljc) and resources: a directory of the class path
  java/           Java forms (doc/classes/SPEC.md §9.2): each package's ns file and its classes' files
  go/             Go forms (SPEC.md §4.1): an Arbace program's namespaces of Go forms (go.P), or the
                  Go program
  resources/      the class path's jars: each subdirectory one, in their names' order (META-INF/services,
                  META-INF/native-image metadata, data)
  runs/           the samples' runs (NAME.args, .in, .out, .err, .code: "The test programs" below)
```

- **Resources, services, metadata** (A4-A7 of the Ghidra case study's G1 plan, 2026-11-10; J2G-SPEC §4.1,
  "Roots from a program's metadata", §10.3). Each subdirectory of `resources/` is given to j2g as a
  `--resources DIR`, in their names' order, as the jars of a class path: their files embedded, a path several
  hold once a directory (ClassLoader.getResources finds each, the first by getResource), their
  `META-INF/services` files the service registry ServiceLoader iterates, their native-image metadata and the
  program's constant reflective sites roots. `:roots :entry` (j2g's `--own-roots entry`) roots the program's
  own Java classes from the entry and those roots alone, so that a class nothing names is not in the program;
  a class j2g refuses (a type outside the world) does not fail the build: j2g's `report.edn` lists it under
  `:refused`, `:stubbed` (reached: its uses throw, naming the refusal) or `:dropped`. The samples `services`,
  `resources`, `reflect-meta` and `refusals` of `test/programs/` show each.

- **The entry.** `:main NS` runs `NS/-main` with the command line (the program's main package puts
  `-m NS` before it: j2g's `--entry-arg`); `:main-class CLASS` calls `CLASS.main(String[])` directly,
  as `java CLASS` does (j2g's `--entry`, which makes it a root). Neither starts rlwrap: the command
  line is the program's own. Without either, `go/` must hold a main package and `src/` and `java/`
  must be absent: the program is `bin/build-go DIR`'s.
- **Clojure.** Every namespace of `src/` (a file whose line begins `(ns`) is required on the shipped
  build's executable (the runtime's namespaces from their compiled loaders; the program's sources
  recorded as embedded ones, `ARBACE_PATH_FIRST`), `java/`'s package namespaces first, so that the
  namespaces' imports of its classes resolve; then c2j compiles the units of the program's own
  sources (`src/`'s, and the `go/` sources they load when goi interprets them), as `--build-java` compiles repr/java's
  reader. `src/`'s files are embedded at their paths (j2g's `--resources DIR`): at run time `RT.load`
  finds each namespace embedded and runs its compiled loader, as for the runtime's; a source c2j did
  not compile (printed `aot: not compiled:`) loads from its embedded text, evaluated. When `src/`
  holds `.clj` or `.cljc` files, `bin/repr-clojure read -o programs/NAME/src src/` translates the
  tree first (the vendored Clojure's names, a `.cljc`'s conditionals resolved for `:clj`: repr/clojure's
  rules, as `bin/clojure-tests` translates the suite), and that tree is compiled and embedded
  (RPS94 (c)).
- **Java forms.** `java/` is one of j2g's `--input` trees: its classes are the program's, translated
  with the runtime's (named `J_<binary name>_J`, J2G-SPEC §4.4). A namespace imports them
  (`(:import (mixed.text Words))`), as a JVM program imports its jar's classes. Each package's
  namespace is embedded as a no-op loader, `(ns P)` (j2g's `--java-namespaces DIR`), so that code
  written at the REPL, which requires the package first, builds unchanged; a namespace of `src/`
  requiring one is warned of at build time (`aot: warning: ... requires the Java package ...`;
  RPS93 (c)).
- **Go forms.** In an Arbace program `go/`'s packages are compiled (RPS92 (b)): `bin/goi table
  --program-list programs/NAME/gotable-program.edn --program-root DIR --configs linux/ARCH` writes a
  table of the program's own (`programs/NAME/gotable`: goi's list and `go/`'s packages, each checked
  by goi's checker as std's are), j2g takes it (`--gotable`), and `bin/build-go --root DIR` prints
  `go/`'s packages into the module: a package outside the module `arbace` (`mixed/caps`) is a module
  of its own, its path's first element, printed under `_modules/` and required by a `replace` in
  `go.mod` (`arbace.build.go`). A namespace requiring `go.mixed.caps` then gets the table's namespace
  (`RT.load` asks the table first), whose vars call the compiled Go functions: goi interprets
  nothing of it. A package goi's checker refuses for the table is a build error, as `go build` refuses a package that does
  not compile (RPS102 (b); until the v2 milestone `go/` was embedded and goi interpreted the package):
  the build stops with the checker's message. `test/programs-fail/bad-go` is the test of it
  (`--check` and the essential gate build it and require the message in `expected-error`).
  A Go program (no entry) is built by `arbace.build.go` alone: no Arbace runtime, a few MB.
- **The base.** The shipped build whose compiled namespaces (`aot/`) the program reuses:
  `ARBACE_PROGRAM_BASE`, else `target/arbace-go`, else the gate's build at the checkout's key `ARBACE_GATE_EXES/KEY20/amd64` (`bin/lib/gate-cache.bash`; `ARBACE_GATE_CACHE` overrides),
  then stage 0, `target/seed` (`bin/seed --build` compiles the checkout's namespaces with it into `aot/`,
  `aot.key` and `aot.runtime` when its runtime is the checkout's; `--no-aot` skips; a program build
  compiles them again when the sources moved and the runtime did not, RPS95 (b)), the first whose
  `aot.key` is the checkout's (`aot_key`: the runtime key and Arbace's sources) and whose
  executable's runtime is the checkout's; else a `--build` for the host's architecture first.
  Its executable compiles the namespaces and runs j2g and repr/go (`J2G_GO_EXE`, `REPR_GO_EXE`), as
  the compiled suite's does; j2g waits for `J2G_LOCK` when it is set (`bin/j2g`).
- **The output.** `target/arbace-go/programs/NAME/ARCH/NAME` (and `-o OUT`), stripped (`-s -w`,
  `ARBACE_GO_SYMBOLS=1` keeps the symbols) and linked with Arbace's version as `--build`'s; no image
  of prepared namespaces (the namespaces are compiled). The last line printed names it. It is cached
  by a key (`programs/NAME/key`: the base's executable and compiled namespaces, goi's table (the
  program's own with `go/`), DIR's
  files but `runs/`, the runtime, the build's scripts, Go's version): a build whose key is unchanged
  takes seconds. The intermediate trees (c2j's, j2g's, the work modules: about 300 MB) are removed
  after the build (`ARBACE_PROGRAM_KEEP=1` keeps them).
- **The samples** (`test/programs/`): `hello-clojure` (three namespaces, one a `.cljc` file, a
  record, a protocol, a transducer, standard input), `hello-java` (a package of two classes, an
  interface and nested classes, `java.io`'s readers, `TreeMap`), `hello-go` (a module of two
  packages), `mixed` (a namespace calling a Go forms package, compiled, and a Java forms class, which
  calls back a Clojure fn; it requires the Java package's namespace and prints `mixed/caps compiled`
  when the package is in the program's table and goi's interpreter, `arbace.go.repl`, was never
  loaded).
  `--check` builds each for the host's architecture and runs its runs as `bin/build-go test` does,
  an Arbace program's with `ARBACE_LOAD_TIMES=1`: none of its own sources may load from source.

### A program's properties (2026-11-11: the Ghidra case study's A13, GH8 (ii))

`program.edn`'s `:properties {"K" "V" ...}`: system properties set at the start of the program's `main`
(j2g's `--property K=V`, repeated, in the keys' order), after `arbace.version` and before jrt's host, the
image and any class of the program is initialized, so a class initializer reads them (a library's
switches, `java.awt.headless`, a test harness's `-D` options). Keys are strings without `=` or a newline,
values strings without a newline; the map is checked by `program_edn`. The glue as data: no launcher class.
A path among them is taken relative to the run's working directory, as the JDK takes `-D` paths. The
sample `test/programs/properties` reads them in a class initializer; `bin/j2g-unit`'s `properties:` checks
the main package's forms (with none, both mains byte for byte as before).

### Java sources by module (2026-11-10, write-ahead: the Ghidra case study's A1, A2, A3, A11)

The design note before the build (GH2 (a), GH3 (a) of `.tmp/plans/GHIDRA-CASE-STUDY.md`; its first user
is the demo repository `arbace-ghidra`'s `plan/softwaremodeling.edn`, but nothing here is Ghidra's: the rule
is that a piece serves another large Java program unchanged, and each is tested by Arbace's own fixtures).

Until now `java/` held Java **forms**: a program written in Java had to be read by `bin/repr-java read`
by hand, one tree, with no dependencies but the platform. A large Java program is modules of sources
in a graph, Maven artifacts by pin, per-module javac options and resources. `program.edn` takes them as
data, `:java`:

```clojure
{:main-class demo.app.Main
 :java {:release 25                       ; -source/-target and the multi-release layer (default: none, layer 26)
        :javac ["-encoding" "UTF-8"]      ; every module's javac options, before its own
        :repository "maven"               ; a Maven repository (its layout): the artifacts' jars
        :modules
        [{:name "util" :src ["util/src"] :resources ["util/res"]}
         {:name "toylib" :artifact "org.example:toylib:1.0"        ; read from its sources jar (A2)
          :resources :jar                                           ; its binary jar's build outputs
          :artifacts ["org.example:toyapi:1.0"]                     ; binary jars: the class path alone (A11)
          :closure {:roots ["org.example.toylib.Greeter"]           ; what resolves from these (A3)
                    :optional ["org.example.toylib.ext.*"]}}
         {:name "app" :src ["app/src"] :deps ["util" "toylib"] :artifacts ["org.example:toyapi:1.0"]}]}}
```

- **A module** (A1) is `:src` (source directories; a path is under DIR, absolute, or `$NAME/...` under a
  root the command line names, `--root NAME=DIR`: a tree outside the program, a generator's output),
  or `:artifact` (a coordinate `group:artifact:version`, its `-sources.jar` in the repository). `:deps`
  are modules (transitively: Gradle's `api` edges), `:artifacts` binary jars (transitively too),
  `:javac` its own options, `:release` its own release, `:resources` directories embedded at their
  paths. Unknown keys, a dependency that is not a module, a cycle and a module named twice are errors.
- **Each step is cached** by the key of its inputs (the files' digests, its dependencies' keys, the
  options, javac's tool executable and repr/java's checkout sources, the tool's own sources), in a
  work directory: `sources` (the source roots; an artifact's jar unpacked), `classes` (javac on
  Arbace, `WORK/classes/M`, against its dependencies' classes and the jars), `read` (repr/java,
  `WORK/forms/M`, its dependencies' **sources** on javac's source path and the jars on its class path:
  the read needs no compilation, and a dependency's file that does not compile never stops a module
  that does not name it). The forms' headers name each file `MODULE/PATH` (read's `--origins`), so the
  forms do not depend on where the work directory is. The modules' forms are merged into one tree for
  j2g (`WORK/forms.all`; a package split over modules, as class-path programs have them, gets one
  namespace file with the loads of both), the resources likewise (`WORK/resources.all`; a
  `META-INF/services` file in two modules concatenated; another `META-INF` file in two modules that
  differ, as artifacts' `LICENSE`, `NOTICE` and `DEPENDENCIES` do, the first module's, as a class path's
  `getResource` answers, the later ones listed in `WORK/resources.shadowed` and kept in their modules'
  `resources.jars`; any other file in two modules an error unless equal). A class in two modules (A16,
  the Ghidra case study's GH14 (a): a library's old and new releases on one class path, hamcrest 2.2
  before hamcrest-core 1.3) is the first module's in the plan's order, as a class path loads it: the
  later modules' differing copies are listed in `WORK/classes.shadowed` (`REL MODULE` a line), an equal
  copy (but for its header's file name) is silent, and javac's class path and source path already take
  the modules in that order; `:shadow false` in the `:java` map keeps a class in two modules an error.
- **An artifact sources-first** (A2): its sources jar unpacked with the release's multi-release layer
  laid over the base (`META-INF/versions/9..R`, in order, as javac's file manager reads a multi-release
  jar for a release). A binary jar's versioned classes whose source the sources jar lacks (log4j's
  `META-INF/versions/9` classes are compiled from a source set its sources jar does not carry) are
  listed in `WORK/src/M.mr.edn`: the base's sources are what is read, and the list says where the
  program differs from the jar. `:resources :jar` takes the binary jar's build outputs as resources
  (every entry of the chosen layer but classes, the manifest, Maven's metadata and signatures: an
  annotation processor's `Log4j2Plugins.dat`, `META-INF/services/*`, native-image metadata), since
  regenerating them would need the processor run.
- **Compile what resolves** (A3): `:closure` cuts an artifact's sources to what its roots reach.
  `:roots` must resolve (classes); `:optional` are taken when they resolve (`p.C`, `p.*`, `p.**`,
  `**`). javac on Arbace compiles the roots with `-implicit:class` over the unpacked tree on the source
  path (javac's own closure: every file a reached type is declared in), `-verbose` naming the files it
  parsed; a file with an error (a type or a package javac cannot find, or any other error) is removed
  and the compilation run again, so a file naming a removed file fails next (the cascade) until a run
  has no error; a root that fails is the step's failure. A root names a class's file; a nested class by
  dots (`p.Outer.Inner`, the file `p/Outer.java`; `p.Outer$Inner` does not resolve). Classes a library
  loads by computed names (bouncycastle's provider: its `$Mappings` and service classes) are roots twice:
  to the cut here, and to j2g as native-image metadata, a `reflect-config.json` under
  `META-INF/native-image/` of a `:resources` directory of the module (A6; the Ghidra case study's G2,
  its `plan/BC.md`). The report is data,
  `WORK/closure/M/report.edn`: the roots, the rounds, the files kept, each file left out with its round,
  its cause (`:absent` and the name javac could not find, `:left-out` when that name is a file left out
  before, `:other` with javac's message) and the absent names counted. javac's default diagnostics are
  read (the full path, `symbol:` lines), not `-XDrawDiagnostics` (which prints the file's name alone).
- **Jars on the class path** (A11): unpacked into directories (`WORK/jars/DIGEST-rR`, by the jar's
  digest and the release, the multi-release layer chosen as above), which javac on Arbace takes on
  `-cp`. Chosen over a zip-reading javac file manager (`overlay/jdk/javac/.../jdk/internal/jrt/javac/
  FileManager.java` serves directories alone, JFS5 (b)) because every reader of the class path reads
  directories: javac on Arbace, repr/java's native provider (`arbace.repr.java.decls`' registry takes
  the class path's directories, and `read --no-javac` is the nread level), j2g's `--resources` and the
  runtime's class-path resources; one unpacking serves them all, it changes nothing of javac's world
  (no tool rebuild, no key moved), and the multi-release choice is made once, visibly. A zip file
  manager stays possible (jrt has `ZipFile` since JC6b) when a program's jars are too many to unpack.
  A binary-only artifact is for compiling against: its classes are not translated (they have no
  forms); a class-retention annotation jar (jspecify's kind) is the common case, and the forms keep
  the annotations as written.
- **Declarations outside the world** (`:outside`, 2026-11-16, ghidra-a8: A8 and A8b of the Ghidra case study's G1
  plan, the user's decision of round 48; J2G-SPEC §4.1, "Declarations outside the world"): `:java`'s `:outside`, a
  vector of `:platform` and `:class-path`, names what j2g declares from class files beside the world: `:platform`
  the platform's modules but java.base (`bin/repr-java platform-dir`, the platform javac compiles against,
  jdk/javac/platform.sha256's pin), `:class-path` every module's binary jars on the class path alone, unpacked as
  above, once each in the plan's order, named by their coordinates. `read` writes j2g's arguments one a line into
  `WORK/outside.args` (`--outside-platform DIR`, `--outside-classes DIR=COORD`), which `--build-program` appends to
  j2g's and puts in the program's key with the platform's pin. A class of the sources naming such a class is then
  translated, its operations on it J2g_Missing stubs that throw when reached (naming the source), where without
  `:outside` the whole class is refused (A7). Without `:outside` nothing changes. The fixture `test/programs/outside`
  (java.beans, java.awt.Desktop, javax.sql and the binary-only toy jar `org.example:toyext:1.0`; its runs `reach`
  and `reach-jar` the negative controls, which must fail naming the source); `bin/java-project test` checks
  `outside.args`, `bin/j2g-unit`'s `test-outside` the declarations and the stubs. Since A8c (2026-11-17, J2G-SPEC
  §4.1) `:platform` also declares java.base's API the world and the edge lack and completes the world's variants
  of platform classes (java.awt.Color's Paint and ColorSpace members), with no change to `outside.args`.
- **A JDK module as the program's world extension** (`:jdk-extra`, 2026-11-17, write-ahead: A15 of the Ghidra demo's G2 plan,
  the user's GH13 (a), and round 53: java.xml stays out of the shipped world, per program only): `:java`'s `:jdk-extra`, a
  vector of `{:module NAME :closure {:roots [...] :optional [...]} :variants [PATH ...]}` or with `:classes` (class
  names and patterns, a vector or a file of them, one a line) in place of `:closure`. Each is a module of kind `jdk` in
  the plan, first (its resources before the class path's, as the platform's loaders find them): its sources jdk26u's
  (`$jdk26u`, `--root jdk26u=DIR`, default `$JDK` or /root/jdk26u) at `jdk/javac/provenance.edn`'s commit (the
  platform's; a tree at another commit, or whose `src/MODULE` differs, refused), the module's `share`, `unix` and
  `linux` layers laid over in that order (module-info.java and snippet files aside); `:closure` cut by A3 as an
  artifact's, javac's `--patch-module MODULE=TREE -Xprefer:source` in place of the source path (the module's classes
  from its sources, the other modules' from the platform), `:classes` the set itself, no cut; read by `bin/repr-java
  read-jdk` (`--patch-module` against the platform, as `bin/jrt-convert` and `bin/javac-vendor` read the JDK; the
  headers name `jdk26u src/MODULE/LAYER/classes/PATH`; `JAVA_PROJECT_JDK_THREADS`, default 1), both fronts and nread
  as a module's; no `classes` step (the platform has them). Its forms join `WORK/forms.all` (j2g's `pkg-of` puts the
  JDK's packages into jrt, `tool-module-prefixes` names java.xml's classes by their binary names; a class with forms
  wins over the edge's declaration, `env/known?`), its resources (the module's other files but HTML: Xerces'
  messages, the JDK catalog) are `WORK/resources.jars/001-MODULE`, and its `:variants` (files, or directories of
  `.ae` files) are copied into `WORK/variants/DIGEST` and named by `--variants` in `outside.args`, which
  `--build-program` appends to j2g's arguments and keys: `bin/arbace-go`, j2g, the shipped runtime and `gate_key`
  are unchanged. The rest of the module stays declared outside with `:outside [:platform]` (A8). The fixture of the
  light proof is `bin/java-project test`'s (`test/java/project/controls/jdk-extra.edn`: java.xml cut from
  SAXParseException to three files, their forms byte for byte jdk/javac's; `:classes`; seven controls); the program
  `test/programs-pending/xml` (DOM and SAX by JAXP's default factories) waits for the day's build before it joins
  `test/programs/`. Measured: the cut from `javax.xml.namespace.QName` alone keeps 1,493 of java.xml's 1,855 files
  (JAXP's FactoryFinder, jdk.xml.internal's security and catalog code reach most of Xerces and Xalan), where the
  JDK's runs load 171 java.xml classes for SAX and 97 for StAX: hence `:classes` for a measured set.
  Clojure's suite takes java.xml this way (round 53 (1), 2026-11-17, write-ahead): `test/clojure/jdk-extra/java-xml.edn`,
  a plan of `:jdk-extra` alone (no `:modules`: the suite's Java is its committed fixtures' forms), `:classes`
  `java-xml-classes.txt` (javac's tool world's 221 java.xml files, `overlay/jdk/javac/closure.txt`, and StAX's stream reader's 25:
  246), the tool world's two variants, no `:outside`; `bin/clojure-tests` reads it for `--aot`'s program when it compiles a
  namespace of `test/clojure/jdk-extra/namespaces.txt` (clojure-xml, reflect) and for the eval level's extended test executable
  (`--jdk-extra-build`, `CLOJURE_TESTS_GO_EXTRA`); the test build, `bin/arbace-go` and `bin/lib` unchanged, so `gate_key` too.
  Since queue 349 (the user's D343-1 (a), D343-2 (a), 2026-11-18) j2g takes javac's tool world's edge beside the shipped one
  (`--edge jdk/edge.edn --edge jdk/javac/edge.edn`: the classes the 246 name outside themselves, which j2g otherwise refuses the
  classes naming, JCS69 (b)), and `--aot` compiles the suite on the extended test executable when one of those namespaces is
  compiled (reflect's `javax.xml.stream` does not resolve on the test build); `bin/clojure-tests --jdk-extra-plan [--essential |
  --list FILE]` prints that selection without reading or building.
- **The commands.** `bin/java-project plan|sources|classes|read MODEL WORK` (MODEL a program's
  directory or a plan file holding the same map; `--root`, `--repository`, `--only M,...`, `--check
  front|nread`: each module also read from the native trees, the forms equal to javac's trees' as
  `bin/repr-java front-check` requires, or read without javac, as the nread level requires; `--chunk N`:
  a module of more than N files read by several processes, whole packages each, the module's tree on
  each one's source path, the forms equal to one process's: a large module within one process's memory), the data
  side `java.project` (`test/java/project.ae`, on the tools' host). `bin/arbace-go --build-program DIR`
  runs `read` when `program.edn` has `:java` and takes `WORK/forms.all` as j2g's input (with `java/`
  when present) and, since 2026-11-11 (ghidra-g1b), `WORK/resources.jars/NNN-MODULE` as the resources, one
  `--resources` a module in the plan's order (the class path's jars: A5's one URL a jar for
  `ClassLoader.getResources`, each module's services and native-image metadata its own; `resources.all`,
  the modules' resources merged, stays for reading).
  `java.class.path` is then the modules' directories' names in that order (`001-util:002-app`; names only, GH4 (iii),
  ghidra-gh4iii, J2G-SPEC §10.3 "The class path's entries by name"), also for `resources/`'s directories.
  Since A17 (2026-11-17, the Ghidra case study's G2 plan) each module's entry also lists its classes: `--entry-classes
  NNN-MODULE=WORK/forms/MODULE` for each `resources.jars` directory, so that `getResources` of a package and an entry's
  `JarFile` list them names only (J2G-SPEC §10.3, "Package listings"; fixture `test/programs/scan`).
- **A dependency's variants** (`:patches`, 2026-11-11, ghidra-g1b; GH8 (iii) for third-party sources): an
  artifact module's `:patches`, unified diffs applied in order (`patch -p1`) over its unpacked sources jar
  before the closure, each in the module's sources key and listed with its digest and the files it changes
  in `WORK/src/M.patches.edn`; a patch that does not apply fails the sources step. For a pinned library whose
  sources a current javac rejects: JUnit 4.13.2's `Result` (jdk26u's `GetField.get` throws
  `ClassNotFoundException`, declared since JDK 18) and hamcrest-core 1.3's `AnyOf` and `AllOf` (two calls
  javac on Arbace rejects for a wildcard's capture; the JDK's javac's answer is the oracle tier's to
  confirm), `test/programs/junit4/patches/`. A variant is a program's, reported, never Arbace's world's.
- **Pinned artifacts of Arbace's own fixtures** (`bin/java-project fetch [--from DIR] [--check]`,
  2026-11-11): `test/java/project/maven-pins.edn`'s files (JUnit 4.13.2, hamcrest-core 1.3: path, size,
  SHA-256) in the main checkout's `.tmp/maven` (a linked worktree's and the clean clone's too), Maven's
  layout, never committed; a model's `$maven-pins` root names it (`--root maven-pins=` overrides,
  `JAVA_PROJECT_MAVEN_PINS` too). `--from` copies from a local repository (no network), else Maven Central
  (by hand). `programs-full` checks them first (`fetch --check`).
- **The fixtures**: `test/programs/junit4` (A10, GH7 (b), 2026-11-11: JUnit 4.13.2 and hamcrest-core 1.3
  read from their pinned sources jars with their variants, Arbace's own runner `arbace.junit.EdnRun`, one
  EDN map a method, and tests of what JUnit does by reflection: the lifecycle's annotations, rules on
  annotated fields with a class rule and a `RuleChain`, `TestName`, `TemporaryFolder`,
  `ExpectedException`, `ErrorCollector`, `TestWatcher`, hamcrest's matchers, `@RunWith(Parameterized)`, a
  timeout's thread; `runs/all.out` is the runner's output when every test's verdict is the designed one,
  written from the tests' sources); `test/programs/properties` (A13); `test/programs/java-project` (two modules of sources with a split package, a
  library read from its toy sources jar at release 25's layer and cut to its closure, a binary-only
  annotations jar; its toy repository made by `test/java/project/make-jars` from
  `test/java/project/jars/`, byte for byte again with `--check`) and `bin/java-project test`
  (`test/java/project/test.bash`): the closure's report, the layer chosen, the multi-release list, the
  merged package, the forms equal from both trees and without javac, and the negative controls (a model
  with an unknown key, a cycle or `:closure` on a source module refused; a root that names an absent
  type fails; the app without the annotations jar does not compile; release 10 reads the base layer).
