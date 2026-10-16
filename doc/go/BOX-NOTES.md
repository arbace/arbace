# The box: notes

B1b's stages ([BOX-SURVEY.md](BOX-SURVEY.md) §6), as they are done. The decisions D3, D8-D14
are the user's of 2026-10-11 (BOX-SURVEY.md §10).

## S0: go-whim's box vendored (2026-10-11)

D3 (a), first step: go-whim's box copied unchanged into Arbace as Go forms, so that printing
them back gives the Go go-whim builds; the generic dispatch is the second step, S1's. D8: the
vendored parts are under the Eclipse Public License 1.0 (`LICENSE.md`).

### What was vendored

From https://github.com/arbace/go-whim (local clone `/root/go-whim`, read only) at
`80c49bf53cd6340487ac623f3d6d135b77ac1db6` (2026-10-06; the survey's commit; the box's files last
changed at `815e11a`). The closure: the five packages below and nothing else of go-whim but
`vmm`'s import of the editor (below). `hv` imports only the standard library (and purego on
darwin); `vmm` imports `hv`, `abi`, `libsys` (darwin) and `github.com/arbace/go-whim/editor`;
`board` imports `abi` and TamaGo's `runtime/goos`; `abi` imports nothing; `libsys` purego.

| go-whim | Arbace (import path) | Go files | lines: Go, tests, assembly |
|---|---|---|---|
| `hv/` | `arbace/box/hv` | 10 (2 tests) | 1,616, 411, 0 |
| `vmm/` (not `vmm/cmd/whim-guest`) | `arbace/box/vmm` | 24 (3 tests) | 2,381, 188, 0 |
| `guest/abi/` | `arbace/box/abi` | 1 | 130, 0, 0 |
| `guest/tamago/board/` | `arbace/box/board` | 2, and 2 `.s` | 323, 0, 432 |
| `internal/libsys/` | `arbace/box/libsys` | 1 | 121, 0, 0 |
| **total** | | **38 Go, 2 assembly** | **4,571, 599, 432** |

The forms: `box/go/arbace/box.../`, 5 package files and 38 file forms, 5,228 lines (the Go
files' lines, SPEC §10.1, plus the package files), and the two `.s` files as they are.

Not vendored: `vmm/cmd/whim-guest` (the launcher: `editor/term`'s terminal host; a model for
Arbace's own, S1), the editor (`editor/`, `editor/term`), the C guest (`guest/rt`), the Go editor
guest (`guest/tamago/main.go`), the test guest `guest/tamago/faulty` and the guest tests
(`guest/*_test.go`, which build and run the guests), the Joker guest.

### The layout

- **A source root of its own, `box/`**, holding `box/go/arbace/box/{abi,board,hv,libsys,vmm}.ae`
  and their directories, laid out as SPEC §4.1 lays out packages, like `overlay/`. Not under
  `go/`: `bin/jrt` prints all of `go/` as jrt's module and builds and vets `./...`, which `vmm`
  (importing go-whim's editor) and the darwin files (purego) would break; S1 decides how the
  box's packages join the program (the a2g program's module is `arbace`, `program.edn`).
- **Import paths `arbace/box/...`**, in the module `arbace` of the Go build, so that S1's guest
  program imports `arbace/box/board` and `arbace/box/abi` from its own module, and the monitor
  is a program of the same module. go-whim's paths (`github.com/arbace/go-whim/...`) could not
  be under the module `arbace`. The rename is the only change to the text: the import specs,
  nothing else (`git diff -M` of the renamed clone: 32 import specs, in the moved files and in
  go-whim's files that import them). No file names a box package's path in a string; the monitor finds the guest's
  symbols by their global names (`whim_vectors`, `whim_apentry`, `runtime/goos.RamStart`).
- **`libsys` leaves `internal/`**: go-whim's `editor/term` (darwin) imports it, and
  `arbace/box/internal/libsys` would refuse go-whim's import in the proof below; nothing else
  changes by it.
- **`guest/abi` and `guest/tamago/board` lose `guest/`**: `arbace/box/abi`, `arbace/box/board`.
- **One tree for every target.** g2a converts one configuration at a time (SPEC §1, item 7);
  the box's files span five (KVM on linux/amd64 and arm64, Hypervisor.framework on
  darwin/arm64, the board on tamago/amd64 and arm64). Converted five times, every file shared
  by configurations gave the same forms, byte for byte, so the trees merge: each file's forms
  once, a package file per package with the union of the files (sorted) and of the test files,
  without `:config` (a program built for several targets, SPEC §4.2) or `:init-order` (it
  differs by configuration, and nothing reads it: the printer and gc derive it), the assembly
  as `:other-files` in the forms directory (SPEC §9.5, §9.6). The build constraints (file names
  and `:build` lines) select each target's files, as gc does.
- Go's comments other than doc comments are kept as `;` comments in the forms text, where the
  converter places them, and are not printed (SPEC §10.4): the printed Go is go-whim's code
  without them.

### How (replayable: `bin/box-vendor convert`)

1. Clone go-whim at the commit (twice: `orig`, `renamed`; the repository is only read).
2. In `renamed`: `git mv` the five packages into `_arbace/box/`, a module `arbace` of its own
   (`go 1.27.1`, requiring go-whim and purego v0.11.1, with replaces for go-whim and its
   `crefactor` so that it builds alone); rewrite the import specs in every `.go` file of the
   clone (`sed`, five exact strings); go-whim's `go.mod`, `guest/tamago/go.mod` and
   `guest/joker/go.mod` require `arbace` and replace it by `_arbace`.
3. `bin/g2a convert --tests --goos GOOS --goarch ARCH` per configuration: linux/amd64,
   linux/arm64 (`hv`, `vmm`, `abi`), darwin/arm64 (also `libsys`), from go-whim's module;
   tamago/amd64, tamago/arm64 (`board`, `abi`), from the guest module `guest/tamago`.
4. Merge the five trees into `box/go/arbace/box` (above).

One change to g2a for it: godump took a module's language version from its `go` line as
written, so a module saying `go 1.27.1` (go-whim's, and so the box's module in the clone) failed
the pinned-language check ("go1.27.1 is newer than the pinned go1.27"). It now takes the line's
language, `go/version.Lang` (`go1.27`), as the go command gives gc `-lang` (`tools/godump/load.go`).
std's packages (no module) are not affected: the round trip `bin/g2a roundtrip amd64` has no
regression against `test/g2a/roundtrip-amd64.edn`; g2a's converter and printer tests pass (150
and 256 assertions).

### The proofs (`bin/box-vendor check`, 2026-10-11)

The forms printed back (`bin/g2a build --print-only --tests --module arbace --layout lines box`),
then:

1. **The round trip's oracle per package.** gocmp (the comparator of `bin/g2a roundtrip`,
   ROUNDTRIP.md) of each printed package against the renamed original, with its tests (`-tests`:
   the trees of all files, the export data and code of the package and its test variants), in
   each configuration it has files for: `hv`, `vmm`, `abi` on linux/amd64, linux/arm64 and
   darwin/arm64, `libsys` on darwin/arm64, `board`, `abi` on tamago/amd64 and tamago/arm64.
   **14 of 14 pass at all three levels** (tree, export, code). g2a's round-trip corpus is std and
   `$GOROOT/test` (GOROOT's layout, `bin/g2a-check`); the box's packages are not in it and have
   no reference file: this check is theirs.
2. **go-whim's binaries, byte for byte.** go-whim's monitor (`vmm/cmd/whim-guest`, built by `go`,
   go-whim's toolchain, go1.27.1, for linux/amd64, linux/arm64 and darwin/arm64,
   `CGO_ENABLED=0`) and its TamaGo guests (`guest/tamago`: the Go editor and `./faulty`, built as
   go-whim builds them, `guest/gotamago.go`: TamaGo's go1.27.1, `GOOS=tamago
   GOOSPKG=github.com/usbarmory/tamago`, `-ldflags "-T 0x201000 -R 0x1000"`, amd64 and arm64):
   - built from the renamed clone with the printed packages in place of the renamed ones, they
     are **identical** to those built from the renamed clone, 7 of 7;
   - built from a clone of go-whim itself with the printed files in its own directories, their
     imports renamed back, they are **identical to go-whim's own**, 7 of 7.

   All builds `-trimpath -buildvcs=false -ldflags=-buildid=`: the build ID hashes the source
   text (the printer writes go-whim's code, not its comments and spacing), and VCS stamps name
   the clone's commit; nothing else differs. The binaries (bytes; go-whim's paths, then
   `arbace/box`'s, shorter by the paths' strings):

   | binary | go-whim | arbace/box |
   |---|---|---|
   | whim-guest linux/amd64 | 3,726,485 | 3,724,592 |
   | whim-guest linux/arm64 | 3,560,656 | 3,558,365 |
   | whim-guest darwin/arm64 | 3,868,578 | 3,866,418 |
   | Go editor guest amd64 | 6,179,591 | 6,178,443 |
   | Go editor guest arm64 | 5,642,080 | 5,641,264 |
   | faulty guest amd64 | 1,656,154 | 1,655,230 |
   | faulty guest arm64 | 1,552,723 | 1,552,043 |
3. **go-whim's tests on the printed packages, on /dev/kvm** (`crw-rw-rw-`, nested SVM): `hv`
   (`TestDoorbellAMD64`, `TestVCPUsExitAMD64`, `TestSysRegsAMD64`, `TestVCPUsAMD64`: real VMs),
   `vmm` (`TestABI`, `TestPlanGo`, `TestLayoutHVFAlignment`, `TestSlotHostAlignment`,
   `TestDirStore`), and the guest tests, which build the guests on the printed board and run them
   on the printed monitor: `TestHello`, `TestFault`, `TestWatchdog` (the C guest), `TestGoGuest`
   (the Go editor in the box answers as natively), `TestGoGuestFault` (the board's vectors, a
   panic, the watchdog, a hello and exit 7), `TestReadAheadC`, `TestReadAheadGo`,
   `TestGoGuestSMP` (1, 2 and 4 vCPUs): **all pass** (about 25 s). The same tests pass on
   go-whim's own clone and on the renamed one. Not run here: `hv`'s arm64 tests (no KVM/arm64
   on this host: S4), Hypervisor.framework (the Mac); their code is checked by gocmp above.

`bin/box-vendor check` holds for `box/` as vendored: once S1 changes `vmm`'s dispatch, its
comparisons show that change, and S0's proof is this record.

### Dependencies outside go-whim

| dependency | used by | how | licence |
|---|---|---|---|
| go-whim's editor, `github.com/arbace/go-whim/editor` | `vmm` (`Config.Host editor.Host`, `editor.Exit`) | not vendored: S1 replaces the seam | (go-whim's; not Arbace's to take) |
| purego, `github.com/ebitengine/purego` v0.11.1 (`h1:2zpWRSQNVKN4eKsKO9eM1ILDgWfYMY9GwqRmK6XeQ/0=`) | `hv/hvf_darwin.go`, `libsys` (darwin only), with its `internal/fakecgo` and `internal/strings` | not vendored; the proof built the darwin monitor from Go's module cache (`GOPROXY=off`); 8,052 lines of Go and 34,081 of assembly (mostly generated) | Apache-2.0 (its text is in `LICENSE.md`, Murmur3's) |
| TamaGo's `goos` module, `github.com/usbarmory/tamago` v1.27.1 (`h1:Zl4w3hu5DaogGvcLzunS4gjwNEqdjFFJedMO3pDQa5k=`) | a guest's link (`GOOSPKG`), not imported by any vendored package | not vendored; module cache | BSD-3-Clause, "Copyright (c) The TamaGo Authors" |
| TamaGo's Go distribution, `/root/tamago-go` (go1.27.1) | the board's `runtime/goos`; the guests' toolchain | already Arbace's toolchain (`G2A_GOROOT`) | Go's BSD-3-Clause (in `LICENSE.md`) |

None of `golang.org/x/sys` or a seccomp library: KVM is raw `syscall.Syscall` ioctls, the
seccomp filter hand-assembled BPF through `prctl`/`seccomp` system calls, and the signal calls
`syscall` (linux) or purego over libSystem (darwin). Arbace's Go builds use no external module
today (`GOPROXY=off`, no `go.sum`; BUILD.md), so purego and the `goos` module are the first; how
they are held is a decision for S1 (the guest's `GOOSPKG`) and S4 (the darwin monitor).

### Licence and provenance

go-whim's `LICENSE` is Vim's licence (added by `8ba7c9d`, 2026-09-21, "init"), and its README
says the product, the modified vim, is under it. The vendored parts are not derived from vim:
the hypervisor API, the monitor, the ABI and the board were written for go-whim from
2026-10-04 (`9fa0d74`, milestone 1, onward). `git -C /root/go-whim log --format=%an -- hv vmm
guest/abi guest/tamago/board internal/libsys | sort -u` gives one author, `rhz` (16 commits, all
`rhz <robert.zawiasa@gmail.com>`, the user, as author and committer); so do all 667 of go-whim's
commits. The files carry no copyright or licence notice. The board implements TamaGo's
`runtime/goos` interface (the hooks it names by `go:linkname`, `_rt0_tamago_start`); a
line-by-line comparison with TamaGo's module and `runtime/goos` finds in common only those
names and generic lines (`#include "textflag.h"`, the hooks' signatures). The user stated on
2026-10-11 (D8) that these parts are licensed to Arbace under the Eclipse Public License 1.0;
`LICENSE.md` records it.

### What S1 needs from these packages

- **The seam, `vmm.Config.Host`** (`vmm.go`): an `editor.Host`, go-whim's vim-shaped host (17
  methods: `Init`, `WinSize`, `TermStart`, `TermStop`, `TTYKeys`, `NowMs`, `Time`, `Delay`,
  `WaitForInput`, `ReadInput`, `Raise`, `Suspend`, `Exit`, `Message`, `Write`, and the rest of
  `editor/host.go`), called by the dispatch `(*vcpu).call` through `m.cfg.Host` under the host
  lock; and `editor.Exit`, the panic a host's `Exit` unwinds with, which `vmm.Run` turns into the
  exit code. S1 replaces both (the survey's call table, or an Arbace-shaped host interface, and an
  exit type of `vmm`'s own), and the import with them; then `vmm` builds in the module `arbace`
  alone, and `vmm`'s `callNames` and `TestABI` stay as they are (the numbers are the ABI).
- **The launcher**: go-whim's `vmm/cmd/whim-guest` runs on `editor/term`'s terminal host (raw
  mode, window size, keys) with `WHIM_GUEST_*` settings; Arbace's launcher (`bin/arbace-box` in
  the survey) is a new main package of the module `arbace`: the image appended (`vmm.Append`,
  `vmm.Appended`, `WHIMGST1`) or beside it, a cooked console (no `TermStart`), its own environment names, rlwrap
  around it on a terminal, the watchdog off or long for the REPL (R2), four vCPUs (D9).
- **The guest side** needs nothing changed to start: `board.Call` and `board.Args` are what
  BOX-SURVEY §9.4's scratch host used. D10's wall clock adds a register at entry (`vmm`'s
  `tamago_ISA.go` and the board's `cpuinit`, an ABI addition), R3's bounded console read is the
  guest's (`WaitRead(ms)`), and the batched `Random` (S2) the board's.
- **The build**: the guest is the a2g program built `GOOS=tamago` with
  `GOOSPKG=github.com/usbarmory/tamago` (a `require` of v1.27.1 in its `go.mod`, today written
  without dependencies by `bin/g2a build`) and `-ldflags "-T 0x201000 -R 0x1000"`, without `-s`
  (the monitor reads the image's symbol table); the monitor a `linux` (or `darwin`, with purego)
  program. `box/` joins the program's source root (copied as a2g's program copies jrt, or a
  second root for `bin/g2a build`).

## S1: a boxed REPL on amd64 (2026-10-11)

Branch `box-s1`. D9 (S1 first), D10 (the wall clock at entry), D11 (files in memory), D13 (no
processes), D14 (the monitor a Go program on the host). Arbace's Go program, unchanged but for
its host, built with TamaGo (`GOOS=tamago`) on the board of `box/`, runs as the guest of
Arbace's own launcher on /dev/kvm: the REPL, `-e`, `-m`, scripts, futures and agents on one
and four vCPUs, the host's wall clock, exit codes, rlwrap and Ctrl-C.

```
$ bin/arbace-go --build --box            # after --build: the guest and the launcher
$ target/arbace-go/box-amd64/arbace-box -e '(java.util.Date.)'
#inst "2026-10-11T15:47:37.360-00:00"
$ target/arbace-go/box-amd64/arbace-box  # the REPL, under rlwrap on a terminal
```

### How the box's packages were changed

`box/`'s forms stay the source. S1's changes to the vendored packages were written as Go in a
renamed clone of go-whim (the one `bin/box-vendor convert` makes) and converted back with
`bin/box-vendor --from CLONE convert` (new: convert an edited clone in place of go-whim's
commit), so that the forms keep the converter's shape and go-whim's comments: the files S1 did
not touch came out byte-identical, the changed ones are the diff of `box/` in git. The new
launcher package was written the same way. From here on, either way works: forms edited by
hand, or Go edited and converted.

### The seam: vmm's own host interface (BX7)

`vmm` no longer imports go-whim's editor. `vmm.Config.Host` is a `vmm.Host`: the console and
the end, the five calls a guest's host must answer (`WaitForInput`, `ReadInput`, `Write`,
`Message`, `Exit`). A host that is also a `vmm.TermHost` (`Init`, `WinSize`, `TermStart`,
`TermStop`, `TTYKeys`, `NowMs`, `Time`, `Delay`, `Raise`, `Suspend`) answers the editor's
terminal calls; for one that is not, the monitor answers them itself: no size, no keys, no
signals, nothing to raise or suspend, its own clocks and sleep. go-whim's `editor.Host` has
exactly these methods, so it is both, unchanged. The exit: a host's `Exit` may return (the run
ends with the code, which `Run` returns; Arbace's launcher's does), end the process (go-whim's
terminal host), or panic with `vmm.Exit(code)`; `Run` recovers any panic value with an
`ExitCode() int` method (`vmm.Exit` has it), which is how go-whim's tests run on the changed
`vmm` (below). The call numbers (`callNames`, `TestABI`) are unchanged: the monitor serves both
kinds of guest. `Config.Env` is new (below).

The S0 proof (`bin/box-vendor check`) no longer holds byte for byte, by design: `vmm`, `abi`,
`board` and `layout`'s files differ. Its successors are `roundtrip` and `kvm-tests` (Checks).

### The ABI's additions (BX8)

- **The environment at entry**: the monitor writes, after argv's pointers and their null, the
  environment's pointers (`NAME=VALUE`) and a null, as a Unix process's stack holds them, then
  the strings, in the command-line area (0x4000-0x10000; `layout.writeArgs`). A guest reading
  argc pointers sees no change; the board's new `board.Env()` reads the rest.
- **The wall clock at entry (D10)**: R11 (arm64: X6) holds the host's Unix time in nanoseconds,
  set just before the boot vCPU first runs (before the seccomp filter, which allows no register
  access but KVM_RUN's). The board keeps it (`cpuinit`) and its `Nanotime` adds it: TamaGo's
  runtime takes `nanotime` for the wall clock too, so `time.Now()`, `new Date()`,
  `System.currentTimeMillis` and `Instant.now()` are the host's (to the TSC's accuracy since the
  entry), and the monotonic clock stays monotonic. go-whim's monitor leaves R11 zero: its guests
  see what they saw. `abi.go` documents both.
- **One vCPU's idle parks**: with one vCPU the board's `Idle` spun to the next timer (nothing
  else can wake it). It now parks in the monitor (`CPUPark`, the existing call, which on one
  vCPU is a host sleep with a deadline) for a wait longer than 200 µs, until 20 µs before the
  deadline, then spins: an idle box no longer burns a host CPU, which the console's polling
  (below) needs. Several vCPUs park as before.
- **Nil dereferences as run-time panics** (amd64): a page fault below the first page, once the
  program has called `board.NilPanics()`, returns from the vectors into a function named
  `runtime.sigpanic` (TamaGo has none; the name makes the unwinder take the faulting frame as a
  signal's) as if the faulting instruction had called it, every register else as it was, as
  Linux's signal handler makes a SIGSEGV a panic; it panics with a `runtime.Error` whose message
  is the runtime's, which jrt turns into Java's implicit `NullPointerException` (A2G-SPEC
  §7.9.5). Without it, a nil dereference ended the guest (the first box oracle run: 7 files
  stopped there). go-whim's guests do not call it: their faults still end the run (its
  `TestGoGuestFault`). arm64's vectors are unchanged (S4).

### The launcher (BX9)

`arbace/box/cmd/arbace-box` (`box/go/arbace/box/cmd/arbace_box/`), a Linux program of the module
`arbace`, modelled on go-whim's `vmm/cmd/whim-guest`: the image appended (`vmm.Append`'s
trailer), `ARBACE_BOX_IMAGE=FILE`, or `arbace-box.elf` beside it; the guest's command line
`arbace` and the launcher's arguments (arbace.main's); its environment the variables a guest
reads (`TZ`, `LANG`, `LANGUAGE`, `LC_*`, `GOGC`, `JAVA_TOOL_OPTIONS`, `ARBACE_*` but
`ARBACE_BOX_*`) and those `ARBACE_BOX_ENV` names; four vCPUs (or the host's CPUs when fewer;
`ARBACE_BOX_CPUS=N|host`); no watchdog unless `ARBACE_BOX_WATCHDOG=DURATION` (R2: an
evaluation may compute for long without a call); the seccomp filter on
(`ARBACE_BOX_SECCOMP=0` off); `ARBACE_BOX_STATS=FILE`, `ARBACE_BOX_PARK`, `ARBACE_BOX_STORE=DIR`
as go-whim's. Its host is a cooked console: `select` on standard input with the call's timeout,
`read`, `write` to standard output, messages to standard error, and an `Exit` that returns;
not a `TermHost`, so the terminal stays as the host has it and Ctrl-C ends the launcher (R8:
SIGINT, Go's default, as the JVM ends; nothing to restore). On a terminal the REPL runs under
rlwrap as the executable's does (EXEC-NOTES.md, "Line editing: rlwrap": the same conditions,
clj's flags, `ARBACE_RLWRAP`, which the guest also sees): the launcher is what rlwrap runs
(the history `~/.arbace-box_history`). The rlwrap code is a copy of the executable's
(`rlwrap_linux.ae`), as the two are main packages of different programs.

### The guest: jrt's box host (BX1-BX5)

The program is a2g's, unchanged: its `main` now sets `jrt.ProgramHost(resources)`, `OSHost`
on Linux and, with `GOOS=tamago`, the box's host (`go/arbace/jrt/hostbox_tamago.ae`, importing
`arbace/box/board` and `abi`):

| `Host` | the box's answer |
|---|---|
| `Stdin` | `WaitRead` without a wait, into a 4 KiB buffer; while there is no input the reading goroutine sleeps 1 ms, doubling to 10 ms (R3, below) |
| `Stdout`, `Stderr` | `Write`; `Message` to the error stream |
| `Open` ... `Getwd` | `OSHost`'s, over Go's `os`: TamaGo's in-memory file system (D11), empty at start, lost at exit |
| `Now`, `Nanotime` | Go's clock: the board's, offset by the host's at entry (D10) |
| `Getenv`, `Environ`, `Args` | the environment and command line written at entry, made `os.Args` and the process's environment by jrt's `init` (before `tuning.go`'s reads it; `GOGC` applied, which TamaGo's runtime read before it was set) |
| `NumCPU` | the vCPUs (`GOMAXPROCS`, which the board sets) |
| `RandomBytes` | `Random` |
| `Resource` | the embedded resources, as on Linux |
| `Exit` | the profiles stopped, then the board's `Exit` (the console flushed) |
| `HostFS`, `HostLinks`, `NetHost`, `HostProc`, `HostExecutable` | none: files' permissions and times are the JDK's failure answers, the default zone `TZ` or UTC, no network (`SocketException`), no processes (`IOException`), resource URLs name `/arbace` |

What failed to build for `GOOS=tamago` (BOX-SURVEY §7, §9.2) moved behind `//go:build linux`:
`OSHost`'s `NetHost` methods (their socket options and `TIOCINQ` by `syscall`) into
`net_linux.ae`; and `host.ae`'s default `theHost` is `ProgramHost(nil)`, not an `OSHost`
(whose `RandomBytes` is Linux's since `gosec`). R4 holds: jrt reaches `os.Stdout`/`os.Stderr`
only in `OSHost` and in `throwable.ae`'s last-resort `StderrPrint` (on TamaGo: the board's
`Printk`, standard error), and `time.Now()` is right in the box. a2g's view of jrt skips files
named for another system (`_tamago.ae`), as a Linux Go build does, so the two `ProgramHost`s
are not a collision (BX3).

**R3, the console read.** A blocking `WaitRead(-1)` stops its vCPU in the monitor: on one vCPU
nothing else runs, on several the monitor serializes host calls (other vCPUs' output waits)
and a stop of the world waits for the reading vCPU. Of the survey's two remedies, the guest's:
`WaitRead(0)` polls (the host's `select` with no timeout), and between polls the reading
goroutine sleeps in the guest, so other goroutines run, the collector stops the world, and an
idle guest parks in the monitor (the board's `Idle`, above). The monitor still serializes host
calls (a poll holds the lock for microseconds); not serializing the read against writes was not
needed. Measured (amd64, nested KVM), a future printing every 300 ms while the REPL waits: its
lines reach the host's pipe 3-6 ms after the guest's clock, as the native executable's (2-8 ms);
the survey's 2.6 s delay (4 vCPUs) and lost output (1 vCPU) are gone. Idle at the prompt, a
poll every 10 ms: about 85 `wait_read` and as many `cpu_park` exits a second on one vCPU;
20 s idle cost 0.3 s of host CPU on one vCPU and 1.2 s on four (beside the start's 0.4).

**R2, the evaluator's yield (BX5).** No `sysmon` and no preemption on TamaGo: a running
goroutine keeps its vCPU until it blocks. `Compiler$CodeRun.tick` (a native in jrt's
`natives.ae`), called at every evaluated method's run and every `recur` (the method's own
loop, `loop`'s), counts and calls `runtime.Gosched` every 65,536 (Joker's `yieldTick`,
go-whim's `798200a`) when `boxYield` is true, which `hostbox_tamago.ae` declares; on Linux
(`programhost.ae`) it is false and the native's body empty, inlined away. The smoke test checks
that a future runs during a 3-million-iteration `loop` on one vCPU. On Linux, measured against
main's executable (fib 27, a 10-million `recur` loop, a 1-million `reduce`; best of 5, three
pairs of runs): 138/845/877, 133/857/889, 146/918/971 ms against 144/853/907, 140/833/896,
146/837/939: within the machine's noise. A tight Go loop in jrt (a large `Arrays.sort`) still
cannot be preempted.

### The build (BX4, BX6)

`bin/arbace-go --build --box` (after `--build`, which it runs first when there is no image):
`bin/g2a build --goos tamago --root box --line-file` of a2g's program as the last `--build`
wrote it, with its image of prepared namespaces (R6: the image is made by the Linux executable
of the same program, so it is valid in the box; `box-<arch>/image.key` records the hash of the
image and of the program's types it was built with), linked again with Arbace's version and
`-T 0x201000 -R 0x1000 -w` (the symbol table kept for the monitor, DWARF dropped: 76.8 MB, the
survey's unstripped 85 MB); then the launcher (`bin/g2a build --module arbace box`, 3.7 MB)
with the guest appended: `target/arbace-go/box-amd64/arbace-box`, 80.5 MB. About 1 min 45 s
on top of the Linux build (the guest's compile, mostly from Go's build cache). `bin/g2a build`
gained `--goos tamago` (TamaGo's `goos` module v1.27.1 required in `go.mod`, `go.sum` from Go's
module cache, `GOPROXY=off`, checked against the pinned hashes) and `--root` (BUILD.md, BX6).
`ARBACE_BOX_ARCHES=arm64` builds the arm64 box too (it runs on KVM/arm64 or the Mac only: S4).
`bin/arbace-go --box ARGS` runs the launcher.

### Checks

- **`bin/arbace-go --smoke --box`** (amd64, /dev/kvm), on 4 and 1 vCPUs: `-e`; the REPL on
  standard input and the executable's REPL session transcript (the JVM's); the REPL's status at
  end of input; `-` (a script on standard input); a script and an `-m` namespace written into
  the in-memory file system first (`ARBACE_PATH=/tmp`); a resource of the image; an error's
  status and message; `System/exit`'s status; a future and `send`; `new Date()` and
  `currentTimeMillis` within 10 s of the host's clock; files in memory and none of the host's;
  a nil dereference as a `NullPointerException`; no network (`SocketException`); no processes
  (`IOException`); a future during a long evaluation (R2); a future's output while the REPL
  waits for a line (R3); then, on a pseudo-terminal, `test/arbace-go-rlwrap.py` on the launcher
  (the REPL under rlwrap, the up arrow, `ARBACE_RLWRAP=off`, `TERM=dumb`, no rlwrap on `PATH`,
  `-e`), which also gained two checks for both programs: Ctrl-C ends the REPL, with and without
  rlwrap. All pass.
- **The oracle's box subset**: every corpus but `forms/files.ae`, `forms/net.ae` and
  `forms/processes.ae` (the host's files, sockets and processes): `bin/oracle check
  'target/arbace-go/box-amd64/arbace-box -' --timeout 900 --exclude forms/files. --exclude
  forms/net. --exclude forms/processes. --expected test/oracle/known-go-box-amd64.edn` (the
  runner's new `--exclude`, and a reference's `:incomplete`, ORACLE.md): 21,695 of 21,742 cases
  match; the 47 mismatches are the native build's 46 (`known-go-amd64.edn`, the same cases) and
  `harvest/reducers.ae:9`, whose reduction peaks at 4.9 GB natively and runs the 3 GiB guest out
  of memory (R1; that run recorded as incomplete). 47 s with `-j 12`. The full corpus in the box
  (before the subset, with `board.NilPanics`): 22,107 of 22,278, the rest the excluded files'
  (files 12 of 324 differ: permissions, times; net 26 of 83; processes 86 of 129). Before
  `NilPanics`, 7 runs ended at their first nil dereference.
- **Clojure's suite in the box**: `bin/box-suite` (new): each test namespace in its own box,
  the renamed suite (from a `bin/clojure-tests` run on the Go build) and its test libraries
  written into the guest's memory by its input (1 MB of `spit`s), then bin/clojure-tests's
  runner, its result printed by a shutdown hook; against `test/arbace-box-results.edn`. 66
  namespaces, 676 tests, 19,627 assertions: 19,625 pass, 2 errors, each namespace as on the
  Linux test build (`arbace-go-results.edn`) but `java.io` (its socket test: no network) and
  `sequences` (a test reads `readme.txt` from the suite's directory, a host file). Excluded (10,
  with reasons in the file): the eight namespaces that need the suite's Java fixtures (the box is
  the shipped program, not the test build), `java.process` (no processes) and `reducers` (the
  same 4.9 GB reduction, R1). 258 s with `-j 12` (each box loads every test namespace first, as
  upstream's runner does: required, since `fails-with-cause?` and the reducers' tests depend on
  it, natively too).
- **`bin/box-vendor roundtrip`** (in `bin/gate --full`): `box/` printed with its tests, the
  printed Go converted back for each configuration and merged as `convert` merges, printed
  again, and the two printings compared by gocmp (tree, export data, code; with tests) per
  configuration: 16 of 16 pass at all three levels (linux/amd64 and arm64: `abi`, `hv`,
  `vmm`, `cmd/arbace-box`; darwin/arm64: `abi`, `hv`, `libsys`, `vmm`; tamago/amd64 and arm64:
  `abi`, `board`), about 2 minutes.
- **`bin/box-vendor kvm-tests`** (in `bin/gate --full`): go-whim at `80c49bf`, renamed, its box
  packages replaced by `box/` printed, its `editor.Exit` given the method `ExitCode` (the one
  adaptation: its tests' recording hosts serve `editor.Main` and `vmm.Run` alike), then `hv`'s,
  `vmm`'s and the guest tests S0 ran (`TestHello`, `TestFault`, `TestWatchdog`, `TestGoGuest`,
  `TestGoGuestFault`, `TestReadAheadC`, `TestReadAheadGo`, `TestGoGuestSMP`): all pass (about
  45 s), the board's one-vCPU parking and the new vmm under go-whim's C and Go guests included.
- **The Linux executable is unaffected**: `ARBACE_GO_ARCHES=amd64 bin/arbace-go --build` (66,523,296
  bytes, as main's), `--smoke` (41 checks, the two new Ctrl-C ones included), the Go oracle
  against `known-go-amd64.edn` (22,232 of 22,278, the 46 recorded), `bin/jrt test` (amd64), and
  Clojure's suite on the test build (`--build-tests`; 76 namespaces, 777 tests, 20,506 of 20,520
  assertions, no regression against `arbace-go-results.edn`): all as before. arm64 was not
  rebuilt here (its executable's code changes only by the moved file and the empty tick). The
  essential `bin/gate` was not run: no source the bootstrap compiles changed (`arbace/a2g`,
  `arbace/g2a` and `arbace/lang/go` are read by the tools only).

### Measured (amd64, nested KVM on the shared 64-core machine; best of 7)

| | box, 4 vCPUs | box, 1 vCPU | native |
|---|---:|---:|---:|
| start to `-e nil` done | 367 ms | 349 ms | 207 ms |
| resident (the launcher's process, the slot touched) | 448 MB | 445 MB | 163 MB |
| `(reduce + (map inc (range 1000000)))` | 987-1,096 ms | 1,036-1,208 ms | 889-901 ms (`GOMAXPROCS=1`: 1,007-1,135) |
| `(fib 25)`, naive | 63 ms | 61 ms | 52-53 ms |
| `(count (sort (map str (range 200000))))` | 286-344 ms | 276-366 ms | 255-266 ms |

The start is the survey's (0.38-0.42 s) less the 62 extra `Random` exits (one now: TamaGo's
seeds come through `getRandomData` once), and the resident size its 445 MB: the guest's 77 MB
image is read into the launcher and loaded into the slot, beside the heap the start touches
(S2). The evaluator runs at 1.15-1.2x native, the exits' cost (16-18 µs each, nested SVM) aside.

### Amendments (BX)

Numbered BX (a prefix not used in `doc/`); proposed 2026-10-11, accepted by the user the same day.

- **BX1 (A2G-SPEC §9.4) The program's host and the box's**: `jrt.ProgramHost(resources)`,
  `OSHost` or (TamaGo) the box's host as tabled above; `theHost`'s default
  `ProgramHost(nil)`; no optional interface in the box.
- **BX2 (A2G-SPEC §9.4) `OSHost`'s network is Linux's**: `net_linux.ae`.
- **BX3 (A2G-SPEC §9.4; a2g) a2g's view of jrt leaves out other systems' files**
  (`_tamago.ae`): `arbace.a2g.jrt/scan`.
- **BX4 (A2G-SPEC §10.6) The main package's host by `ProgramHost`; the box's program** built
  from a2g's output with `GOOS=tamago` and the Linux build's image.
- **BX5 (A2G-SPEC §16; SPEED-NOTES' closure compiler) The evaluator's yield**:
  `Compiler$CodeRun.tick`, `runtime.Gosched` every 65,536 method runs and recurs on TamaGo,
  empty on Linux.
- **BX6 (BUILD.md) `bin/g2a build --goos tamago` and `--root`**.
- **BX7 (box/: vmm) The seam**: `vmm.Host`, `vmm.TermHost`, `vmm.Exit` and the `ExitCode`
  protocol, the monitor's answers for a host without a terminal; go-whim's editor no longer
  imported.
- **BX8 (box/: abi, board, vmm) The ABI's additions**: the environment after argv, the wall
  clock in R11/X6, `board.Env`, one vCPU's idle parking, `board.NilPanics` (amd64).
- **BX9 (box/) The launcher** `arbace-box` and its `ARBACE_BOX_*` settings.
- **BX10 (checks) `bin/arbace-go --smoke --box`, `bin/box-suite` with
  `test/arbace-box-results.edn`, the oracle's box subset with
  `test/oracle/known-go-box-amd64.edn`, `bin/box-vendor roundtrip` and `kvm-tests` in `bin/gate
  --full`, and the pty test's Ctrl-C checks** (which the executable's smoke test runs too).

### The user's decisions (2026-10-11)

- BX1-BX10 accepted.
- The box's smoke test, suite subset and oracle subset in `bin/gate --full` (the survey's §8;
  about 10 minutes more): deferred. Only the round trip and the KVM tests are in it.
- R1 / S2: the launcher maps the appended image instead of reading it whole and copying it into
  the slot (it was 445 MB resident): to do, first in S2.
- Open for S4: `board.NilPanics` and the vectors' return to `sigpanic` need their arm64 form
  (ELR_EL1, the link register) before the box's arm64 oracle can pass.

## S2, R1: the launcher maps the guest's image (2026-10-11)

Branch `box-mmap`, R1's first item, decided by the user 2026-10-11 (above): the launcher read
the 77 MB image appended to it into its own heap (`vmm.Appended`), and the monitor copied its
segments into the guest's slot, so that both copies stayed resident beside the guest's memory
(445 MB against the native executable's 163 MB). The box's packages were changed as in S1: Go
in a printed copy of `box/`, converted back by `bin/g2a convert`; the converted forms of the
changed files merged into `box/`'s by line, so that their comments and literals' text stay, and
the line numbers after an edit follow it (`git diff` shows them).

### What changed (BX11)

- **`vmm.ImageFile`** (`vmm/imagefile.go`, new): a guest image in a file, `Size` bytes from
  `Offset`; `vmm.OpenImage(path)` (a whole ELF file) and `vmm.AppendedFile(path)` (the image
  appended to a program, found by its trailer as `Appended` finds it, which now reads through
  it). `Config.ImageFile`, when set, is the image in place of `Config.Image` (which go-whim's
  tests and monitor still use).
- **The segments mapped from the file**: the monitor parses the ELF through an
  `io.SectionReader` of the file (nothing read whole), and maps each `PT_LOAD` segment's file
  bytes over the slot (`mmap` with `MAP_PRIVATE|MAP_FIXED` at the segment's address, from the
  image's offset plus the segment's; `vmm/mapfixed_linux.go`), the rest of its last page cleared
  where the file has bytes there (the bss beyond is the slot's anonymous memory, zero). Text and
  read-only data are the page cache's, read into the guest as it touches them and shared between
  boxes; a page the guest writes (data) is copied then. A segment whose address or offset is not
  on a host page, or that shares a page with another, is read into the slot instead (`pread`
  through the section reader: one copy, none in the heap); so is every segment on macOS, whose
  `syscall` package has no `mmap` at an address (`mapfixed_darwin.go`, a compile check; S4), and
  with `ImageFile.Copy`.
- **The image on a 64 KiB boundary**: `vmm.Append` and `bin/arbace-go`'s `box_append` pad the
  launcher with zeros to a multiple of `vmm.ImageAlign` (64 KiB, the largest host page) before
  the image, so that the image's segments, on 4 KiB pages of the ELF (`-R 0x1000`), are on pages
  of the launcher's file. A launcher appended without the padding still runs (copied).
- **The symbols read once, without a `Symbol` each**: `loadImage` and `isTamaGo` each called
  `elf.File.Symbols`, three times in all, each making 150,000 `Symbol`s and their names (about
  30 MB of the launcher's heap, 65 MB resident at the end); `imageSymbols` reads the symbol table
  and its strings once and keeps the three values the monitor needs (`whim_vectors`,
  `whim_apentry`, `runtime/goos.RamStart`, which marks a TamaGo image: `image.tamago`, which
  replaces `isTamaGo`).
- **The launcher** passes the image as an `ImageFile` (appended, `ARBACE_BOX_IMAGE`, or beside
  it); **`ARBACE_BOX_MAP=0`** copies the segments instead of mapping them (below).
- Tests (`vmm/imagefile_test.go`, in `kvm-tests` and the round trip): `Append`'s alignment and
  `AppendedFile`; `loadSegments` on the test's own executable, mapped and copied, byte for byte
  equal, the first segment's mapping the file's (`/proc/self/maps`); `imageSymbols` against
  `elf.File.Symbols` on an ELF file made by the test (`go test` strips its executable).

The file stays open and mapped for the run. A guest image must not be rewritten in place while a
box runs from it (a private mapping still reads the file's pages it has not copied): the build
replaces `arbace-box` by a rename; `ARBACE_BOX_IMAGE` or `arbace-box.elf` written over in place
would change the guest under it.

### Measured (amd64, nested KVM on the shared 64-core machine; best of 9, interleaved)

| `-e nil` | before (S1) | mapped (default) | copied (`ARBACE_BOX_MAP=0`) | native |
|---|---:|---:|---:|---:|
| start to done, 4 vCPUs | 0.36 s | 0.55 s | 0.32 s | 0.20 s |
| start to done, 1 vCPU | 0.33 s | 0.53 s | 0.28 s | |
| max resident, 4 vCPUs | 445-448 MB | 257-262 MB | 293-295 MB | 161-164 MB |
| max resident, 1 vCPU | 443-450 MB | 258-265 MB | 291-295 MB | |

Of the mapped run's 258 MB: the guest's memory 214 MB anonymous (the heap and stacks it touched,
the data it wrote), 33 MB of the image's 66 MB of segments from the file (the text and read-only
data the start touches), the launcher's heap 18 MB (the symbol table's 11 MB read once). The
copied run has the whole 66 MB in the slot. Evaluation is unchanged either way (S1's three
measures, three runs each: the `reduce` 934-1,118 ms, `fib 25` 61-69 ms, the `sort` 268-319 ms).

The mapped start is slower here: the guest's first touch of each mapped page is a 4 KiB fault
through two levels of nested paging (no huge page: the page cache's pages are mapped one by one),
about 8,400 of them for 33 MB, where the copy's memory is anonymous and in 2 MiB pages
(transparent huge pages `always`), a few dozen faults. Mapping the text only and copying the
rest measured between the two (0.44 s, 281 MB); the read-only data's faults cost the most. On
/dev/kvm without nesting a fault is a few microseconds, and the difference should be tens of
milliseconds; not measured here. The copied start is faster than S1's: no 77 MB read into the
heap and no symbol tables made thrice.

### Checks

- `bin/arbace-go --build` (amd64; the executable 66,523,296 bytes, as main's) and `--build
  --box`: the guest 76,774,351 bytes, as before; the launcher 3.7 MB, 80,509,919 bytes with the
  image (the padding included).
- `bin/arbace-go --smoke --box`: 64 of 64 checks on 4 and 1 vCPUs and on a terminal, mapped and
  with `ARBACE_BOX_MAP=0`. `ARBACE_BOX_IMAGE`, `arbace-box.elf` beside the launcher and a
  launcher appended without the padding (copied) run.
- The oracle's box subset (`-j 12`): 21,695 of 21,742 cases match, the 47 mismatches those of
  `known-go-box-amd64.edn`, `harvest/reducers.ae`'s run incomplete as recorded (R1's other item:
  the guest's 3 GiB).
- `bin/box-vendor roundtrip`: 16 of 16; `bin/box-vendor kvm-tests`: hv, vmm (with the new
  tests) and the guest tests pass.
- The essential `bin/gate` was not run: no source the bootstrap compiles changed (`box/`,
  `bin/arbace-go`).

### Amendment (proposed 2026-10-11, accepted by the user the same day)

- **BX11 (box/: vmm, the launcher) The image mapped**: `vmm.ImageFile`, `OpenImage`,
  `AppendedFile`, `Config.ImageFile`, `ImageAlign` and `Append`'s padding, the segments mapped
  from the file (copied where they cannot be, on macOS, or with `ImageFile.Copy`), the symbols
  read once; the launcher's `ARBACE_BOX_MAP=0`.

### The user's decisions (2026-10-11)

- BX11 accepted. The default stays mapped (258 MB, 0.55 s here; it shares the image's pages
  between boxes, and the slower start is nested virtualization's); `ARBACE_BOX_MAP=0` copies
  (293 MB, 0.32 s here).

## S2, the rest: measured and hardened (2026-10-11)

Branch `box-s2`, S2's items but R1 (the image mapped: branch `box-mmap`, BX11, the section
before). The changes are in jrt, the evaluator's Go variants (`arbace/lang/go`, read by a2g
only: no source the bootstrap compiles changed), `bin/arbace-go`, `bin/box-suite` and a new
helper, `tools/elfsyms`; `box/` is unchanged.

### The start: the `Random` exits, DWARF, the symbol table (BX12)

- **The `Random` exits**: batched since S1 (TamaGo's seeds come through `getRandomData` once):
  a start's counts (`ARBACE_BOX_STATS`) show `random 1` (the survey's 63).
- **DWARF**: dropped since S1 (`-w`): with it the guest is 91.7 MB, without 76.8 MB; the start
  is the same either way (the monitor never reads it: 370 ms both, best of 9).
- **The symbol table**: the monitor needs three symbols of the guest's 152,806
  (`whim_vectors`, `whim_apentry`, `runtime/goos.RamStart`), but the table is 11 MB of the
  image (3.6 MB of symbols, 7.2 MB of names). `bin/arbace-go --build --box` now cuts it to
  those three after the link (`tools/elfsyms`, a Go program of 170 lines on `debug/elf`: the
  loaded segments, the other sections and the section headers stay as gc wrote them; only the
  symbol table, its strings and the section names move, and the result is read back and
  checked). GNU objcopy (`--strip-all --keep-symbol`) does the same for amd64, but the host's
  binutils know x86 only, and an arm64 guest is built here too; `-s` would drop the three.
  `ARBACE_GO_SYMBOLS=1` keeps the whole table, as for the executables. The guest: 76.8 MB ->
  65.9 MB; the launcher with it 80.5 -> 69.6 MB.
- Measured (best of 9): with S1's launcher, which read the image whole and made the symbol
  table's 150,000 `Symbol`s twice, the cut table took the start from 370 to 310-320 ms and the
  resident size from 448 to 346 MB. With BX11's launcher (main `7417981`; the table read once,
  without `Symbol`s), copied (`ARBACE_BOX_MAP=0`): 320 -> 310 ms, 295 -> 283 MB; mapped: no
  difference beyond the noise of the page cache (below).

**A finding for the mapped start (BX11).** The mapped start (0.53-0.57 s here, against 0.31-0.32
copied) depends on how the image's file is in the page cache, not on the image: the same bytes
written by one large write (`dd bs=128M`, Go's `os.WriteFile`, Python's `write`) start mapped in
0.36-0.38 s, written by `cat` or `cp` in 0.54-0.57 s, and after the file's pages are dropped
(`posix_fadvise(DONTNEED)`) and read again through the guest's faults, in 0.54-0.55 s whatever
wrote them. Linux 6.18's ext4 keeps a file written in large writes in large folios, which the
host maps (and the guest's nested paging then faults) in larger pieces; faults read 4 KiB pages
back. So the fast mapped start is not to be had by writing the launcher differently (it would not
survive the cache); a fix is the monitor's (copy the read-only data, or prefault), which BX11's
notes measured at 0.44 s for the text alone mapped.

### The collector in the box (BX13)

The guest's RAM is fixed (`vmm.GoRAMBytes`, 3 GiB from the image's base) and TamaGo's runtime
takes its heap by `sbrk` from it, never giving memory back; the host has no swap. jrt's settings
are GOGC 200 (`DefaultGOGC`) and a 64 MiB minimum heap: the collector's goal is three times the
live heap, so a guest holding a third of its RAM ran out of memory, and `GOMEMLIMIT`, passed by
the launcher, was not applied (TamaGo's runtime reads it before jrt's `init` sets the
environment from the entry; S1 applied `GOGC` only).

Measured with a program that holds ever more live data (16 MiB arrays, each written) while it
allocates 64 MiB of garbage per array (`.tmp/gc/live.ae` of the branch's work), the live data
reached before the guest ran out of memory:

| memory limit | live data reached | time to the end |
|---|---:|---:|
| none (S1; `GOMEMLIMIT` not applied) | 1,152 MiB | 27 s |
| 2,560 MiB (RAM less 512 MiB) | 2,176 MiB | 54 s |
| 2,304 MiB | 2,304 MiB | 72 s |
| 2,048 MiB (RAM less 1 GiB) | 2,432 MiB | 102 s |
| 1,792 MiB | 2,304 MiB | 115 s |
| 1,536 MiB | 2,432 MiB | 151 s |
| native, no limit (killed at 3.5 GB) | 3,584 MiB | |

Past the limit the collector runs continuously (hence the times); below it, the limit costs
nothing. GOGC, measured on `fold` over 2 million elements (allocating: 600 MB of garbage a
fold): 100 is 5% slower than 200 at 200 MB less resident (568 against 770 MB), 400 2-3% faster
at 400 MB more; 200 stays.

So the box's host (`hostbox_tamago.ae`'s `init`) applies `GOMEMLIMIT` as Go's runtime reads it
(`off`, or bytes with `B`, `KiB`, `MiB`, `GiB`, `TiB`), and without it sets the limit to the
guest's RAM (`goos.RamSize`) less 1 GiB (`boxMemoryHeadroom`): 2 GiB in the 3 GiB slot, the
setting that kept the most live data. GOGC stays 200 and the minimum heap 64 MiB (on TamaGo the
ballast is resident: `sbrk` clears what it takes).

### The watchdog: off (decided, no change)

`ARBACE_BOX_WATCHDOG` stays unset by default, for these reasons:
- it ends a vCPU that runs the guest for its duration without a hypercall, and an evaluation
  makes none while it computes (the evaluator's yield is `runtime.Gosched`, in the guest): the
  suite's reducers case computes for 75 s, and any long evaluation at the REPL would be ended,
  where the JVM computes until Ctrl-C;
- what it would catch is no hang the box has: a guest whose goroutines all wait parks its vCPUs
  (calls), which the watchdog counts as not running, and costs nothing (`@(promise)` at `-e`, 5 s:
  0.3-0.4 s of host CPU on 1 or 4 vCPUs, the runtime's own timers); a runaway loop is ended as
  under the JVM, by Ctrl-C (R8) or the caller's timeout (`bin/box-suite`'s and the oracle's);
- unattended runs set it when they want it (`ARBACE_BOX_WATCHDOG=10m`).

### The suite's file tests and `ARBACE_PATH` (BX17)

The one file test that failed in the box was `sequences`' `test-iteration`, which reads
`readme.txt` by a relative path: `bin/clojure-tests` runs the suite in its directory, where it
copies Clojure's top-level files. `bin/box-suite` now writes the suite directory's files
(`readme.txt` and the six others, 196 KB) into the guest's working directory (`/`), as its
setup; the test is unchanged and passes. `java.io`'s files tests pass as in S1 (its one error is
`test-socket-iofactory`: no network, S5). `ARBACE_PATH` in the box: `bin/box-suite` loads the
whole suite and its libraries from it (`/lib/...:/suite/test`), and the smoke test gained a
check of two namespaces in directories on a two-element path, one requiring the other.

### Reducers: why 4.9 GB, and `fold` on 4 vCPUs (BX14, BX15, BX16)

Two defects of the Go build, natively as in the box:

- **The 4.9 GB (BX14).** `(reduce + 0 (r/map identity (range 1e8)))` (the suite's
  `test-closed-over-clearing`, "will throw OutOfMemory without proper reference clearing")
  grew by 45 bytes an element natively: 0.6 GB at 1e7, 2.0 GB at 4e7, 4.9 GB at 1e8, while
  `(arbace.core.protocols/coll-reduce (range 2e7) + 0)` alone stayed at 0.3 GB. The reducer is a
  `reify` closing over its collection, a `Range`, which the `CollReduce` protocol reduces as an
  `ASeq` (`seq-reduce`), caching its chunks as the walk realizes them; the reify's method calls
  `coll-reduce` in tail position, and its frame held the reify (its slot 0 and `self`) for the
  whole reduction, and through it the head of the range. The JVM's compiled call clears `this`
  first (`InvokeExpr.emitArgsAndCall`, `ObjMethod.emitClearThis`, CLJ-1250's fix: a call in tail
  position in a method that cannot be direct). The closure compiler now does the same:
  `CodeInvoke`'s `clearThis` (set when the `InvokeExpr` is in tail position and its `ObjExpr`
  not direct) clears slot 0, the frame's fn and its object once the arguments are evaluated
  (`CodeRun.clearThis`); `Evaluator.trace` names a frame's method by the method's kind, not by
  its fn. Natively the case now peaks at 0.30 GB (43 s), in the box it runs (75 s on 4 vCPUs).
  Only fn calls clear, as the JVM's boxed calls do; its unboxed host calls (`emitUnboxed`) also
  clear, which the evaluator does not (they hold no closure in the cases the suite has).
- **The reducers' tests failed when their namespace was loaded first (BX15).** After
  `clojure.data.generators` (which the test requires) was loaded, a reducer no longer satisfied
  `CollReduce`: "Don't know how to create ISeq from: arbace.core.reducers$folder$reify". The
  library says `(:require [arbace.core :as core])`; `arbace.core` is not in `*loaded-libs*`
  (on the JVM neither), so `require` loads it, and the Go build's `RT.load` evaluated the
  embedded source again, `defprotocol` and all: `arbace.core.protocols.CollReduce` was a new
  interface, which the reducers' reify, made before, does not implement. On the JVM the
  namespace is AOT-compiled and `RT.load` initializes its `__init` class, which the JVM does
  once. `RT.load` now loads an embedded source once (`LOADED_EMBEDDED`, by script base): a
  second load, `:reload` included, is the JVM's no-op; sources on `ARBACE_PATH` load every
  time, as the JVM's do. With every test namespace loaded first (`bin/clojure-tests`,
  `bin/box-suite`) the order hid it.
- **`fold` on 4 vCPUs (BX16).** Measured on `(r/fold + (r/map work v))` against `reduce`, `v`
  2 million longs and `work` 20 boxed iterations (allocating), and a computing case (200,000
  elements, 300 primitive iterations each):

  | ms, best of 3-5 | box 1 vCPU | box 4 vCPUs, S1 | box 4 vCPUs, S2 | native `GOMAXPROCS=1` | native `GOMAXPROCS=4` |
  |---|---:|---:|---:|---:|---:|
  | `reduce`, allocating | 5,820-6,050 | 5,520-5,920 | 5,690 | 4,520-4,820 | 4,510-4,580 |
  | `fold`, allocating | 6,290-6,570 | 2,810-5,460 | 1,980-2,000 | 6,500-6,620 | 1,790-1,860 |
  | `reduce`, computing | 4,490-4,840 | 4,510 | 4,690 | 3,600 | 3,600 |
  | `fold`, computing | 4,620-4,800 | 3,770 | 1,430 | 5,050 | 1,560 |

  On S1's box 4 vCPUs used 72-103 s of host CPU for 30 s and folded 1.2-2x faster than one: the
  evaluator's yield counted its calls in one global (`evalTicks`), which every vCPU wrote at every
  method run and recur, one cache line fought over through nested paging. The count is now the
  thread's (`Thread.evalTicks`, through the goroutine-local slot, about 4 ns): `fold` is 3.2x
  (allocating) and 3.4x (computing) one vCPU's, as native's 3.5x and 3.2x. jrt's own fork-join
  (`BigInteger.parallelMultiply` of two 1.4-million-digit numbers, no evaluator) was 2.3x on 4
  vCPUs on S1's box: the box's fork-join itself scaled. A larger pool does not help in the box (on S1's: 4,141 ms
  at parallelism 4, 6,677 at 64). The evaluation speed is unchanged (below).

### Measured (amd64, nested KVM on the shared 64-core machine, other builds running; best of 9)

| | box S1, 4 vCPUs | box S2, 4 vCPUs | box S2, 1 vCPU | native |
|---|---:|---:|---:|---:|
| start to `-e nil` done | 370-380 ms | 330 ms | 300 ms | 200-210 ms |
| resident (the launcher's process) | 448 MB | 349 MB | 348 MB | 164-228 MB |
| `(reduce + (map inc (range 1000000)))` | 936-977 ms | 968-979 ms | 1,047-1,156 ms | 904-944 ms |
| `(fib 25)` | 60-69 ms | 62-64 ms | 62-63 ms | 50-53 ms |
| `(count (sort (map str (range 200000))))` | 275-300 ms | 289-292 ms | 274-284 ms | 252-259 ms |

(S2's launcher copies the image, as S1's; BX11's maps it, its own table above.)

Clojure's suite, each namespace alone (the box's `bin/box-suite -j 1`, against the shipped
executable running bin/clojure-tests's runner the same way: every test namespace loaded first,
about 18 s of each run):

| namespace | box, 4 vCPUs | box, 1 vCPU | native |
|---|---:|---:|---:|
| `clojure-set` (mostly the loading) | 20.7 s | 24.8 s | 18.7 s |
| `numbers` | 19.9 s | | 18.8 s |
| `sequences` | 34.7 s | | 30.9 s |
| `reducers` | 77.8 s | 95.7 s | 62.7 s |

The whole suite, 12 at a time: the box's subset (67 namespaces, 685 tests, 19,646 assertions)
222 s (S1: 66 namespaces, 258 s); the native test build's (76 namespaces and test.generative)
214 s.

### Checks

- `bin/arbace-go --build` (amd64; 66,527,392 bytes: the evaluator's and `RT`'s changes) and
  `--build --box` (the guest 65,917,262 bytes, the launcher 69,606,071).
- `bin/arbace-go --smoke` (amd64): 42 checks, one new (an embedded namespace loaded once).
  `--smoke --box`: 70 checks on 4 and 1 vCPUs and on a terminal, three new (namespaces on
  `ARBACE_PATH`, an embedded namespace loaded once, reducers' `fold` and a reduction over a
  reducer).
- The native oracle against `known-go-amd64.edn`: 22,232 of 22,278, the 46 recorded.
- The box's oracle subset against `known-go-box-amd64.edn`: 21,696 of 21,742, the native's 46;
  `harvest/reducers.ae` runs (5 of 5, 74 s); its group and `:incomplete` are gone from the file.
- `bin/box-suite` against `test/arbace-box-results.edn`: 67 namespaces, 19,645 of 19,646
  assertions, as the reference (`reducers` back, `sequences` without its error; the one error
  is `java.io`'s socket test).
- Clojure's suite on the native test build (`--build-tests`): 76 namespaces, 777 tests, 20,506
  of 20,520 assertions, no regression against `arbace-go-results.edn`; `bin/jrt test` (linux
  amd64 and arm64).
- `bin/box-vendor roundtrip` (16 of 16) and `kvm-tests` (all pass): `box/` is unchanged.
- The essential `bin/gate` was not run: no source the bootstrap compiles changed.

### Amendments (proposed 2026-10-11, accepted by the user the same day)

- **BX12 (BUILD.md; bin/arbace-go) The guest's symbol table cut** to the monitor's three
  symbols by `tools/elfsyms` (`ARBACE_GO_SYMBOLS=1` keeps it).
- **BX13 (A2G-SPEC §9.4: the box's host) The collector in the box**: `GOMEMLIMIT` applied, and
  a default memory limit of the guest's RAM less 1 GiB; GOGC 200 and the 64 MiB minimum heap
  kept.
- **BX14 (A2G-SPEC §16; SPEED-NOTES' closure compiler) A call in tail position clears the
  method's object** (`CodeInvoke.clearThis`, `CodeRun.clearThis`), as `emitClearThis`;
  `Evaluator.trace` by the method's kind.
- **BX15 (A2G-SPEC §10.3; EVAL-NOTES' RT.load) An embedded namespace loads once**
  (`RT.LOADED_EMBEDDED`), as the JVM initializes an AOT-compiled namespace once.
- **BX16 (BX5) The evaluator's yield counted per thread** (`Thread.evalTicks`).
- **BX17 (checks) `bin/box-suite`'s setup writes the suite directory's files into the guest's
  working directory**; reducers in `test/arbace-box-results.edn` and in the box oracle's
  reference; the smoke tests' new checks.

### The user's decisions (2026-10-11)

- BX12-BX17 accepted (`tools/elfsyms` kept).
- The watchdog stays off by default; `ARBACE_BOX_WATCHDOG` sets one.
- BX11's mapped start: measured first on a host without nested KVM before any change in the
  monitor (copying the read-only data, or prefaulting).

## S6, the amd64 half: the guest's own Go as forms (2026-10-11)

Branch `box-s6`. BOX-SURVEY §6, S6: the Go the box's guest is built with, TamaGo's runtime and
its whole standard library and TamaGo's `goos` module, held as Go forms in the repository and
built from them; the guest image byte-identical to one built from the originals. The amd64 half
(`tamago/amd64`); arm64 is the next milestone's, with S4.

### What is held (`tamago/`, BX22, BX23)

A source root of its own, `tamago/` (tamago/README.md), beside `box/`, in two roots of forms:

| | packages | from | forms files | bytes |
|---|---|---|---|---|
| `tamago/goroot/go/` | 371 of std for tamago/amd64 (the runtime, `internal/...`, `vendor/golang.org/x/...`; not `cmd/`, not tests), with 68 assembly and header files | TamaGo's go1.27.1 tree (`/root/tamago-go`, https://github.com/usbarmory/tamago-go, `VERSION` go1.27.1, 2026-08-28), `src/` | 2,105 | 31.4 MB (30.6 MB of forms, 579,502 lines; 0.77 MB of assembly) |
| `tamago/goroot/src/` | GOROOT/src's `go.mod`, `go.sum`, `vendor/modules.txt` and `runtime/cgo/abi_amd64.h` (the runtime's `asm_amd64.s` includes it; `runtime/cgo` has no Go files with cgo off) | the same | | 5 KB |
| `tamago/goos/` | 69 of the module's 94 packages (those that build for tamago/amd64: `goos`, `amd64`, `kvm/...`, `soc/intel/...`, `board/...` for amd64 machines), 19 assembly files, its `go.mod` | `github.com/usbarmory/tamago` v1.27.1, Go's module cache | 224 | 0.8 MB |

In all 32.2 MB, 2,422 files; 6.4 MB as a gzipped tar (`git`'s pack grows by about as much; the
repository's pack was 16.4 MB). Left out of std, and proposed as such: the 6 packages with test
files only, and the 2 that embed third-party data and that only `cmd/trace` or tests import,
which no program outside GOROOT can import (`internal/trace/traceviewer`: Catapult's 2.7 MB
trace viewer; `encoding/json/internal/jsontest`: 0.3 MB of JSON corpora). Not converted: the
tests, `cmd/` (the toolchain stays the binaries of `/root/tamago-go`), other configurations.

The forms are the converter's output unchanged, one configuration (each package file keeps its
`:config` and `:init-order`), laid out as SPEC §4.1 (`tamago/goroot/go/runtime.ae`,
`runtime/proc.ae` ...), other files in the forms directories (SPEC §9.5) as `box/` holds them.
For arm64 the next milestone either merges the trees as `box/`'s were (where a file's forms
are the same for both architectures) or holds a second tree; std's files with
architecture-dependent constants may not merge, unmeasured.

### How (`bin/tamago-vendor`, after `bin/box-vendor`)

- `convert`: the toolchain's `src/` checked against a pinned digest (every file but `src/cmd/`),
  std dumped afresh and converted (`bin/g2a convert --corpus amd64`), the goos module copied
  from Go's module cache (its pinned hash checked) and converted (`bin/g2a convert --goos tamago
  --goarch amd64 ./...`, the packages that dump without errors kept), assembled into `tamago/`
  with their other files and the headers included from outside the packages. About 25 s.
- `verify`: a fresh conversion compared with `tamago/` (`diff -r`): identical. About 25 s.
- `print [DIR]`: `tamago/` printed (lines layout) into a GOROOT, `DIR/goroot` (its `src/` the
  printed std and GOROOT/src's files, the rest of the toolchain's tree linked: `bin`, `pkg`, the
  compiler, linker and `pkg/include`), the module `DIR/goos`, and jrt's runtime overlay for that
  GOROOT; reused while `tamago/`'s hash is unchanged. About 20 s (printed in parallel).
- `check` (after `bin/arbace-go --build --box`): below.

The host (standalone S4, 2026-10-12): `bin/box-vendor`, `bin/tamago-vendor` and
`bin/x509roots-vendor` run g2a through `bin/g2a`, on Arbace's Go executable (`bin/lib/g2a-host.bash`:
`G2A_GO_EXE`, else the tools' host, `bin/lib/host.bash`); the times above are the JVM's. On main's
G3 as the host, amd64: `tamago-vendor verify` 383 s (5.9 GB), `print` 24 s, `check` 115 s (after `--build --box`, 1,644 s with a2g on Go: both guests identical); `box-vendor
roundtrip` 51 s (20 of 20), `kvm-tests` 34 s, `purego-check` 10 s; `x509roots-vendor verify`
4 s, `check` 7 s. All pass as on the JVM.

`bin/g2a build` gained `--print-only --module std` (packages at their import paths, no
`go.mod`) and `--goos-dir DIR` (the goos module from DIR by a `replace`, no `go.sum`) (BX21).

### The proof (`bin/tamago-vendor check`)

1. std printed against GOROOT/src, package by package (`bin/g2a-check`: gocmp's tree, export
   and code levels), against the round trip's reference: 371 of 371 pass (`unsafe` skipped at
   export and code, as in the reference), no regression.
2. The goos module's 69 packages printed against the originals (gocmp): 69 of 69 at all three
   levels.
3. The guest image: a2g's program of the box build (`target/arbace-go/work-box-amd64/mod`)
   built twice, as `bin/arbace-go` links it (`-T 0x201000 -R 0x1000 -w`, the version), with
   `-buildid=`: once with GOROOT `/root/tamago-go` and the goos module from Go's module cache,
   once with GOROOT the printed one and the printed module (by a `replace`, `-modfile`), each
   with jrt's runtime overlay (`proc.go`, `runtime2.go`, `arbace_local.go`, printed from
   `overlay/go`) mapped onto its GOROOT; then both cut to the monitor's symbols as `bin/arbace-go`
   ships them (`tools/elfsyms`, S2). On main `b3d119b` merged into the branch:

   | image | bytes | sha256 |
   |---|---:|---|
   | as linked, from the originals | 76,766,368 | `612ed53b7e91f1e8...` |
   | as linked, from the forms | 76,766,368 | `612ed53b7e91f1e8...` |
   | symbols cut, from the originals | 65,909,034 | `b7ab8106f5cdeb37...` |
   | symbols cut, from the forms | 65,909,034 | `b7ab8106f5cdeb37...` |

   **Identical**, both. (Before the merge, on `e4e9033`'s box: 76,766,123 bytes, identical.)
   About 1 min 50 s, mostly std compiled for the printed GOROOT (Go's build cache keys it by
   path and contents: the first build after a change of `tamago/` compiles it anew).

The build ID is the one difference left without `-buildid=`: it hashes the source text, and
the printer keeps no comments. The shipped guests, from the forms and with
`ARBACE_BOX_GO=orig`, differ in 80 bytes, all of the build ID; each is reproducible (two
builds from the forms gave the same bytes).

**What the proof found** (BX20): in the lines layout a literal that begins a Go line (a final
`return true`, `return nil`, `return 0`) was recorded only as a break (`:go/breaks [i]`), and
the printer put it on the line after the last it wrote; with blank or comment lines, or a
closing brace, between, gc's line table differed (a hello program for `GOOS=tamago`: 16 bytes
of `.gopclntab`, 8 functions, such as `runtime.isMarkedOrNotInHeap` at `mbitmap.go:1291`
printed `:1281`, `sync.(*Pool).getSlow` at `pool.go:197` printed `:196`). The round trip's code
level compares the code, not the line tables, and did not see it. The converter now writes
`[i line]` for such a literal (`arbace/g2a/layout.ae`) and the printer honours it
(`print_code.ae`); SPEC §10.1 amended. `overlay/go/runtime/proc.ae` took the conversion's 10
changed breaks (`bin/jrt overlay --check` passes), so the executables' `proc.go` lines are now
the original's there too (the Linux executable's bytes change by it; nothing else does). `box/`'s
forms keep the old breaks (valid: an index alone still means the next line); converted again,
they would take the new ones.

### The build (BX22)

`bin/arbace-go --build --box` prints `tamago/` into `target/arbace-go/tamago` (`bin/tamago-vendor
print`) and builds the amd64 guest with that GOROOT and module, jrt's overlay mapped onto it
(`ARBACE_BOX_GO=forms`, the default); `ARBACE_BOX_GO=orig` builds as before, from the
toolchain's tree and Go's module cache; arm64 guests from the toolchain's tree until S6's arm64
half. The launcher (a Linux program) is built by the toolchain as before. Cost: the print,
about 20 s when `tamago/` changed, seconds otherwise; the guest's compile, 97 s against 94-96 s
(Go's build cache holds both). Recommended and done: the forms are what the box is built from,
so that the proof is about what ships.

### Checks

- `bin/tamago-vendor verify`: identical; `check`: as above.
- `bin/arbace-go --build` (amd64, 66,519,200 bytes after the merge) and `--build --box` (from the
  forms; the guest 65,909,070 bytes, the launcher with it 69,644,638), `--smoke --box`: 70 of 70
  on 4 and 1 vCPUs and on a terminal (64 of 64 before the merge).
- `bin/g2a test` (150 assertions) and `bin/g2a-print-tests` (256): pass. `bin/jrt overlay
  --check`: passes.
- `bin/g2a roundtrip amd64`: no regression against `test/g2a/roundtrip-amd64.edn` (49 s);
  `bin/box-vendor roundtrip`: 16 of 16 at all three levels.
- The essential `bin/gate` was not run: no source the bootstrap compiles changed (`arbace/g2a`
  is a tool's, `go/`, `test/jrt/`, `tamago/`, `bin/` are not compiled by it).
- `test/jrt/tables.ae` follows RP1 (the JVM's order only for the CDS archive's classes, else by
  descriptor): regenerated, `reflect_tables.ae` changes order in five tables (`Thread$Builder$OfVirtual.name`,
  `Condition.await`, `ReentrantReadWriteLock`'s `ReadLock`/`WriteLock.tryLock`,
  `Semaphore.tryAcquire`), each name's overloads of different arities, so `Reflector`'s choice
  is unchanged; two runs give the same file.

### Amendments (proposed 2026-10-11, accepted by the user the same day)

- **BX20 (SPEC §10.1) A literal break's line**: `:go/breaks` entries `[index line]` when the
  literal's Go line is not the line after the last written; the converter writes them, the
  printer honours them.
- **BX21 (BUILD.md) `bin/g2a build --print-only --module std` and `--goos-dir`**.
- **BX22 (box build) The guest's Go from `tamago/`** by default (`ARBACE_BOX_GO=forms|orig`),
  printed by `bin/tamago-vendor print`.
- **BX23 (`tamago/`) What is held**: std's 371 packages for tamago/amd64 (not the 6 test-only
  ones, not `internal/trace/traceviewer` and `encoding/json/internal/jsontest`), GOROOT/src's
  module files and the one outside header, the goos module's 69 packages for tamago/amd64 and
  its `go.mod`; the converter's output unchanged; not tests, not `cmd/`.
- **BX24 (checks) `bin/tamago-vendor verify` and `check`**, proposed for `bin/gate --full`'s box
  step: `verify` (25 s) always; `check` after the Go chain's box build (about 2 minutes, plus the
  box build, about 1 min 40 s, which `--full` does not make today), or only when `tamago/`,
  `arbace/g2a`, `overlay/go` or the toolchain change.

### The user's decisions (2026-10-11)

- BX20-BX24 accepted: the guest's Go from `tamago/` by default.
- `tamago/` kept as committed (all of std for tamago/amd64 and the goos module, 32 MB of forms).
- `bin/tamago-vendor verify` joins `bin/gate --full`'s `box` step, and the box's build from the
  forms with `bin/tamago-vendor check` the go chain, as `box-forms`.

## S3, amd64: the store (2026-10-11)

Branch `box-s3`. BOX-SURVEY §6, S3; D11 (memory, then the store). go-whim's precedent: its
monitor's store (`vmm/store.go`, the calls 25-29) and the Joker guest's namespace `box`
(`guest/joker/box/box.go`, `guest/joker/store.go`; go-whim's doc/LISP-SANDBOX.md, "The store"),
at `80c49bf`, read only. The monitor already had the store and the launcher `ARBACE_BOX_STORE`
(S1, as go-whim's); what S3 adds is Arbace's side of it, in the box and natively.

### The user's decisions (2026-10-11, all as recommended)

- (a) **No store unless one is named**: `ARBACE_BOX_STORE=DIR`, or the launcher's `--store DIR`;
  without it `arbace.box` refuses (`:no-store`), no default directory.
- (b) **`arbace.box` on both builds**: in the box over the monitor's calls, in the native
  executable over a host directory in the same format, so that the same programs run natively and
  boxed and one store directory serves both.
- (c) **Loading from the store**: a `store:` entry kind in `ARBACE_PATH` (require and load find
  namespaces in refs), and scripts by ref: `arbace-box ref:NAME`, `-i ref:NAME`.
- (d) **The in-memory file system's snapshot as a tar**, explicit: `(arbace.box/save-fs! ref)`,
  and `ARBACE_BOX_FS=ref` to seed the file system at the start.
- (e) **A host tool in the launcher**: `arbace-box --store DIR put|get|ref|ls`.

### The design

**Data.** A blob is an immutable byte string named by its SHA-256, written in Clojure as 64
lowercase hexadecimal digits (Joker's); a ref is a name, 1 to 64 of `[A-Za-z0-9._-]` not
beginning with a dot (vmm's `RefName`), pointing at a blob, the store's only mutable state,
changed unconditionally or by compare-and-set. On the host the store is a directory
(`vmm.DirStore`'s format, go-whim's): `blobs/HASH` (the bytes), `refs/NAME` (the hash in hex
and a newline), and since S3 `refs/.lock`, the refs' lock (below). A blob is held to its hash
when read: one a write left short is refused, not served.

**Where it lives.** Nowhere by default (a). The launcher's store is `--store DIR` (before
arbace.main's arguments; it wins over the environment) or `ARBACE_BOX_STORE=DIR`; the native
executable's `ARBACE_BOX_STORE`. The guest does not see the variable (the launcher passes no
`ARBACE_BOX_*` but `ARBACE_BOX_FS`, below): its store is the monitor's. `ARBACE_BOX_STORE_MAX` is
the largest blob either takes (bytes, or with `B`, `KiB`, `MiB`, `GiB`, `TiB` as `GOMEMLIMIT`;
`off`; by default 256 MiB).

**Sharing a directory (b).** The monitor runs under its system-call filter, which allows no
`rename`, `flock` or `mkdir` but allows `fcntl`: so the refs are read and written under a
POSIX record lock of `refs/.lock` (`F_SETLKW`, shared to read, exclusive to write) besides the
process's mutex, by the monitor and by jrt alike. A compare-and-set is then one at a time across
processes, and no reader sees a ref half written (the monitor rewrites a ref in place, as
go-whim's: under the exclusive lock). jrt's own store, outside any filter, also writes a blob to
a temporary name and renames it to its hash, and a ref likewise, both `0644` as the monitor's.
Run one after the other or at once, the native program and boxes see each other's blobs and
refs (the smoke test checks the native executable's write read in a box).

**The monitor's answers (BX30).** go-whim's store answered -1 for every failure, a missing store
included, and -2 for a failed compare-and-set. S3 gives the refusals answers of their own, in
`abi` as `StoreRefused` (-1: no such blob or ref, or the host's error), `StoreConflict` (-2),
`StoreNone` (-3: no store), `StoreTooLarge` (-4: `BlobPut` beyond `DirStore.Max`, `vmm.ErrTooLarge`)
and `StoreBadName` (-5: `RefGet` or `RefSet` with a name `RefName` refuses, checked by the
monitor before the store). go-whim's guests take any negative answer as a failure, so they
work as before (go-whim's `TestDirStore` and the KVM guest tests pass on the changed `vmm`).

**jrt (BX32).** A host may implement a sixth optional interface, `jrt.StoreHost`
(`Store() Store`, nil when it has none), `jrt.Store` being `vmm.Store`'s five methods with
errors of jrt's (`ErrStoreConflict`, `ErrStoreTooLarge`, `ErrStoreName`): the box's host answers
it over the calls (`hostbox_tamago.ae`: `boxStore`, whether there is a store asked once by a
`BlobSize`; the calls' hash and name buffers on the heap, since the monitor reads them by
address and a goroutine's stack moves), `OSHost` on Linux over `ARBACE_BOX_STORE`
(`store_linux.ae`: `dirStore`, opened once). jrt's own Java `jdk.internal.jrt.HostStore`
(`overlay/jdk`, rooted as jrt's own classes are, RD2) has static natives over it answering the
codes above (`store.ae`), and `read`, `pathEntries` and `resource` in Java.

**`arbace.box` (BX34).** A namespace of the Go build only (`arbace/lang/go/ns/box.ae`, embedded
as `arbace/box.ae`; the JVM build has none), over `HostStore`: `put` (a string, as UTF-8, or a
byte array: the hash), `get` (a string, or nil), `get-bytes`, `size`, `ref` (the hash, or nil),
`ref!` (with an old hash, or nil for "no such ref", compare-and-set: true or false), `put!`,
`load` (Joker's: the ref's blob as a string), `load-ref` (the ref's source evaluated, its source
path `ref:NAME`), `save-fs!`, `load-fs!`, `store?`. A refusal throws an `ex-info` whose data's
`:arbace.box/error` is `:no-store`, `:bad-name`, `:bad-hash`, `:too-large`, `:no-blob` (a ref to a
blob the store has not), `:no-ref` (`load-ref`, `load-fs!`) or `:refused`; a failed
compare-and-set is `false`, not an error, and a missing blob or ref `nil`, as Joker's.
Required in 13-17 ms natively (it is not in the image of prepared namespaces).

**Loading from refs (BX33, BX34).** An `ARBACE_PATH` entry `store:PREFIX` finds resource
`a/b_c.ae` (namespace `a.b-c`) as the blob ref `PREFIXa.b_c.ae` points at (the path's `/`s made
`.`s), in order with the directories: `RT.load` (`require`, `load`; its stream is not taken for
an embedded source) and `getResourceAsStream`, not URLs (`io/resource` names directories'
files only). Since `:` separates the entries, a store entry runs from `store:` to the next `:`:
`store:` (or `store::` before another entry) has an empty prefix (`HostStore.pathEntries`, jrt's
`PathEntries`). arbace.main's `load-script` (the Go build's variant, `main.subst.ae`) takes
`ref:NAME` for a script or an `-i` file: `arbace-box ref:NAME ARGS...`, the same natively.

**Snapshots (BX35).** `(save-fs! ref)` in the box, `(save-fs! ref dir)` anywhere, stores the
files under the directory as a tar and points the ref at it; `load-fs!` writes one back. The
tar (jrt's `SnapshotTar`, Go's `archive/tar`, PAX) holds the directories and regular files
(other kinds left out: the box's `/dev` devices), each directory's entries in order, names
relative to the directory, permissions kept, times the epoch and no owner: the same files are the
same tar, so the same blob (checked: a snapshot restored and taken again is the same hash).
`RestoreTar` makes missing directories, replaces files, and refuses absolute names and names
leaving the directory. Both go through the host's file calls (`Host.Open`, `Stat`, `ReadDir`,
`Mkdir`), so they work on TamaGo's in-memory file system and on a host directory. Natively a
directory must be named (`IllegalArgumentException` otherwise: the host's `/` is no snapshot).
`ARBACE_BOX_FS=REF` (passed to the guest) seeds the box's file system at the start, in jrt's
`init` before the program runs: no store, no such ref or a bad tar end the guest with status 1
and the reason (`arbace: ARBACE_BOX_FS=nofs: no such ref`). The host reads the snapshot with
`arbace-box --store DIR get REF | tar tf -`.

**The launcher (BX31).** `arbace-box --store DIR` then `put FILE|- [NAME]` (prints the hash;
points NAME at it), `get HASH|NAME` (the blob on standard output), `ref NAME [HASH]` (prints, or
points), `ls` (the refs, hash and name, by name) is the store's tool, without a guest; exit 1
for a refusal or nothing found, 2 for a usage error. `--store DIR` followed by anything else
runs the guest with that store. rlwrap's decision looks at the guest's arguments (after
`--store DIR`).

### What refuses (the gate's "refusals")

| refusal | box | native | `arbace.box` |
|---|---|---|---|
| no store configured | every call `StoreNone` | `OSHost.Store` nil | `:no-store` |
| a ref's name (empty, 65+, a leading dot, `/`) | `StoreBadName` (the monitor checks; jrt checks first) | `ErrStoreName` | `:bad-name` |
| not a hash (64 lowercase hex digits) | (guest-side) | (guest-side) | `:bad-hash` |
| a compare-and-set that failed | `StoreConflict` | `ErrStoreConflict` | `false` |
| a ref to a blob the store has not | `StoreRefused` | an error | `:no-blob` |
| a blob beyond `ARBACE_BOX_STORE_MAX` | `StoreTooLarge` | `ErrStoreTooLarge` | `:too-large` |
| a blob longer than an array (2^31 - 9) | | | `IllegalStateException` (`HostStore.read`) |
| a script or snapshot ref that is not | | | `:no-ref`; `ARBACE_BOX_FS`: status 1 |
| a tar name outside the directory | | | `:refused` (`load-fs!`) |

### Measured (amd64, nested KVM; two runs each)

| | box, 4 vCPUs | native |
|---|---:|---:|
| 1,000 `put`s of small blobs | 88-136 ms | 51-94 ms |
| 1,000 `ref!` (a `put` each) and `ref` | 353-429 ms | 273-311 ms |
| `put` of 32 MiB | 129-132 ms | 117-126 ms |
| `get-bytes` of 32 MiB | 4 ms | 85-87 ms |

The box's `get` reads the monitor's cache (go-whim's: every blob read is kept, verified, for the
run); jrt's store reads and hashes the file each time. A ref's write is a few hundred
microseconds either way (the lock, a write; natively a rename).

### Checks

- `bin/arbace-go --build` (amd64; 66,801,824 bytes, 282 KB more than main's: `archive/tar`,
  the store, `HostStore`, `arbace.box`'s source) and `--smoke`: 53 of 53, eleven new (the
  store's checks below, natively, and a directory's snapshot saved and loaded, which names its
  directory).
- `bin/arbace-go --build --box` (the guest 66,126,158 bytes, the launcher with it 69,927,262)
  and `--smoke --box`: 86 of 86 (70 before; the store's 16 on 4 vCPUs): none configured; put,
  get, size, ref!, ref; compare-and-set; names, hashes and refs to no blob refused; a blob beyond
  `ARBACE_BOX_STORE_MAX`; a script from a ref, an `-i` file from a ref, a missing one; namespaces
  from `ARBACE_PATH`'s store entries (two, one requiring the other, with and without a prefix);
  the launcher's tool (put then the guest's `load`, the guest's `put!` then get, ref, ls, a bad
  name); `ARBACE_BOX_FS` (the file system seeded from a snapshot the guest saved; the tar read on
  the host; a missing ref); a store directory written by the native executable and read in the box.
- `bin/jrt test` (amd64; new `store_test.ae`: `PathEntries`, the directory store with a second
  store on its directory, a snapshot restored and taken again), `bin/jrt build --arch arm64`.
- The oracle, native, against `known-go-amd64.edn`: 22,232 of 22,278, the 46 recorded; the box's
  subset against `known-go-box-amd64.edn`: 21,696 of 21,742, the 46 recorded.
- `bin/box-suite` (`BOX_SUITE_RUN` main's renamed suite: c2a and the suite are unchanged): 67
  namespaces, 19,645 of 19,646, as the reference.
- `bin/box-vendor roundtrip`: 16 of 16 at all three levels; `kvm-tests`: hv, vmm (with the new
  `TestDirStoreLimits`: the limit, the lock file not a ref, a second store on the directory) and
  the guest tests pass.
- S6's proof holds: `bin/tamago-vendor verify` (identical) and `check` (std 371 of 371, goos 69
  of 69, the guest image from the originals and from the forms identical: 77,000,080 bytes as
  linked, 66,126,122 cut).
- The essential `bin/gate` was not run: no source the bootstrap compiles changed
  (`arbace/lang/go` and `overlay/jdk` are read by a2g and `bin/jrt-convert` only).

How the box's packages were changed: as S1 (BOX-NOTES, S1, "How the box's packages were
changed"): `vmm/store.go` and its test, unchanged since S0, edited from go-whim's own text (its
comments and literals kept) and converted (`bin/g2a convert`), their forms taken whole; `abi`,
the launcher's `main.go` and `rlwrap_linux.go` edited in a printing of `box/`, converted, and the
converted forms merged with `box/`'s comments and literals' spellings; the new `store.go` of the
launcher converted from its Go. `box/` printed again and the edited Go compared: the same code
(comments and literal spellings apart).

### Amendments (proposed 2026-10-11, accepted by the user the same day)

- **BX30 (box/: abi, vmm) The store's answers and the shared directory**: `abi.StoreRefused`,
  `StoreConflict`, `StoreNone`, `StoreTooLarge`, `StoreBadName` (-1 to -5); the monitor answers
  `StoreNone` without a store and checks refs' names; `DirStore.Max`, `vmm.ErrTooLarge`; the
  refs under the `fcntl` lock of `refs/.lock`.
- **BX31 (box/: the launcher) The store's settings and tool**: `--store DIR`,
  `ARBACE_BOX_STORE_MAX` (default 256 MiB), `ARBACE_BOX_FS` passed to the guest, `--store DIR
  put|get|ref|ls`.
- **BX32 (A2G-SPEC §9.4) An optional `StoreHost`**: `jrt.Store`, `StoreHost`, `CurrentStore`; the
  box's host over the calls, `OSHost` on Linux over `ARBACE_BOX_STORE` in the monitor's format
  (the lock, renames); jrt's own Java `jdk.internal.jrt.HostStore` and its natives.
- **BX33 (A2G-SPEC §10.3; RT's variant, `ClassPathResources`) `ARBACE_PATH`'s store entries**
  `store:PREFIX`, for `RT.load` and `getResourceAsStream`, not URLs; the split rule.
- **BX34 (EVAL-NOTES' namespace variants) `arbace.box`**, a namespace of the Go build, and
  arbace.main's `ref:NAME` scripts and `-i` files.
- **BX35 (A2G-SPEC §9.4: the box's host) Snapshots**: `SnapshotTar`, `RestoreTar` (a
  deterministic tar through the host's file calls), `save-fs!`, `load-fs!`, `ARBACE_BOX_FS` at the
  guest's start.
- **BX36 (checks) The store's checks** in `--smoke` and `--smoke --box`, jrt's `store_test.ae`,
  vmm's `TestDirStoreLimits`.

### The user's decisions on the open points (2026-10-11)

- BX30-BX36 accepted.
- The blob limit stays 256 MiB; the monitor's blob cache is to be bounded (least recently used,
  by total size) rather than kept for the whole run.
- `arbace.box` on the JVM Arbace too, over the same directory format, so that programs using the
  store run on the JVM, natively and in the box.
- Confirmed: snapshot tars with epoch times; no transactions (`ref!` with an old hash is the
  compare-and-set); a `store:` entry of `ARBACE_PATH` runs to the next `:` (ref names hold no
  `:`).

### The JVM's `arbace.box` (2026-10-12; amendment BX37, accepted by the user 2026-10-12)

Branch `box-jvm`, the user's decision above. **One source, two host layers.** `arbace/box.ae`
(BX34's namespace, moved from `arbace/lang/go/ns/box.ae` with `git mv`) is now shared: what it
asked of `jdk.internal.jrt.HostStore` it asks of `arbace.box.host`, whose functions (`status`,
`put`, `size`, `read`, `ref`, `set-ref`, `tar`, `untar`) answer as `HostStore`'s statics do (0,
or -1 to -5 as in the table above). The Go build's variant `arbace/lang/go/ns/box/host.ae` is
`HostStore` itself, one call each; the JVM's `arbace/box/host.ae` is `store_linux.ae` and
`store.ae` again over java.nio. So the API, the refusals and their messages are one text, not two
kept alike (the alternative, two copies of `arbace.box` or a `.subst.ae` swapping its calls,
would leave the parity to review). On the JVM: `ARBACE_BOX_STORE` (opened once, the directories
and `refs/.lock` made when missing, a store that cannot be opened said once on standard error),
`ARBACE_BOX_STORE_MAX` read as jrt's `StoreMax`; blobs and refs written to a temporary name
(`.put-*`, `.set-*`) and renamed, 0644; a blob held to its hash when read (streamed, so `size`
holds a blob of any length; `get-bytes` beyond an array's length is `HostStore.read`'s
`IllegalStateException`); refs read and written under the process's lock and
`FileChannel.lock(0, Long.MAX_VALUE, shared)` of `refs/.lock`, which jdk26u's
`UnixFileDispatcherImpl.lock0` (`src/java.base/unix/native/libnio/ch/UnixFileDispatcherImpl.c`)
takes as `fcntl(F_SETLKW)` with `l_whence SEEK_SET, l_start 0, l_len 0` and `F_RDLCK`/`F_WRLCK`,
the call jrt's `lockRefs` makes (checked with strace on both: the same arguments). The lock's
channel stays open (closing any descriptor of the file would drop the process's POSIX locks) and
is opened again if an interrupt closed it. Snapshots are written as Go's `archive/tar` writes
jrt's (`WriteHeader` with `Format: FormatPAX` and the epoch: USTAR when its fields hold the
entry, a name split into USTAR's prefix when it can be, else a PAX extended header named
`PaxHeaders.0`, its `path` and `size` records sorted; directories' entries in byte order; the
same `toASCII`, field formats and checksum) and read as its reader reads (USTAR, PAX local and
global headers, GNU long names, a legacy NUL type, base-256 numbers), with `RestoreTar`'s rules
and Go's error texts (`archive/tar: invalid tar header`, `unexpected EOF`, `stat DIR: no such
file or directory`).

**Loading from refs.** `arbace.main`'s `ref:NAME` is now in `arbace/main.ae` itself (VENDOR-NOTES
hand change 22), for both builds (`main.subst.ae` lost that substitution).

**`ARBACE_PATH` on the JVM (the user's decision with BX37: it works the same on all three).** As
on the Go build, `ARBACE_PATH`'s entries come after what the program has itself (the Go build's
embedded sources, the JVM's class path) and in their order, directories and `store:PREFIX`
entries alike, a namespace's `.ae` looked for in all of them, then its `.clj`, then its `.cljc`.
`RT.load`'s last resort, `RT.loadFromPath` (when `ARBACE_PATH` is set), calls
`arbace.box.host/load-from-path`, which reads them so (a directory's `DIR/a/b_c.ae`, a store
entry's ref `PREFIXa.b_c.ae`; source path `a/b_c.ae`). Resources: the Go build gives
`ARBACE_PATH`'s directories to `getResource`/`getResources` (`ClassPathResources`, so
`io/resource` and `data_readers.ae`), and its store entries to `RT`'s own resource reads only;
on the JVM `bin/arbace-j` appends the directories to the class path, after `ARBACE_CLASSPATH`
(store entries left to `RT.load`), so the class loaders find their resources. A source the class
path then finds in one of those directories is not loaded from there: `RT.load` leaves it to
`RT.loadFromPath` (`RT.inArbacePath`, in Java since it is asked while `arbace.core` loads), so
an earlier store entry still wins, as natively. Started without `bin/arbace-j` (`java -cp ...`),
namespaces still load from all of `ARBACE_PATH`; only resources need the directories on the
class path. The same program and the same `ARBACE_BOX_STORE`/`ARBACE_PATH` thus run on the JVM,
natively and in the box.

**Checks.** `test/native/box_test.ae` (in `bin/native-tests`, so in `bin/build-arbace` and the
gate; 6 tests, 73 assertions, about 17 s): the API, compare-and-set, every refusal, the limit,
the directory's format and modes, a blob left short, a second store on the directory, the
snapshot of a tree with a name that is not ASCII, one longer than USTAR's name and one it splits
(its hash is the native executable's, measured), tars of GNU tar's `gnu`, `posix` and `ustar`
formats restored, names outside the directory and a tar that is not refused; in fresh JVMs, no
store, `ARBACE_BOX_STORE_MAX`, `ref:NAME` and `-i ref:NAME`, `ARBACE_PATH`'s store entries and
(since the decision above) its directories in order with them (a directory's namespace requiring
a store's; an earlier store entry before a later directory; `.ae` before `.clj` across entries;
`.cljc`), and the refs' lock held by the test's JVM keeping another JVM's `ref!` waiting.
`bin/arbace-go --smoke` (amd64, when `target/arbace.jar` is built: in the gate it is) runs
`store_checks` on the JVM too (`bin/arbace-j`), then one directory used by the JVM and the native
executable: each reads the other's refs; the same tree's snapshot is the same tar from both, and
each restores the other's; compare-and-set counters run by two JVMs and two native processes at
once end at 200 (no update lost); the JVM holding the refs' lock keeps the native `ref!` waiting;
`ARBACE_PATH`'s order the same on both (a store entry before a directory, and the reverse), and
on the JVM a directory's `data_readers.ae` and `io/resource`.
The essential `bin/gate` passed (6m46s): the bootstrap (stage 2 = stage 3, 5,777 classes
verified; the native tests 60 of 60, the six new included), Clojure's suite on stage 2 without
regressions, `bin/class-forms-tests`, and the Go executable built again (a2g with the shared
`arbace.box`, its host variant and `RT`'s cut; 66,814,112 bytes) and smoke-tested: 67 of 67 on
amd64, fourteen new (the nine store checks on the JVM, the five shared-directory checks).
With `ARBACE_PATH`'s directories (branch `box-jvm-path`): the essential `bin/gate` passed again
(7m01s; the native tests 60 of 60, `box_test` 75 assertions; the smoke test 69 of 69 on amd64,
the two `ARBACE_PATH` checks new).

- **BX37 (BX34, EVAL-NOTES' namespace variants; VENDOR-NOTES hand change 22) `arbace.box` on
  the JVM**: `arbace/box.ae` shared, its host layer `arbace.box.host` (the JVM's over java.nio,
  the Go build's variant over `HostStore`); `ref:NAME` scripts in `arbace/main.ae`; `RT.load`'s
  last resort, `ARBACE_PATH` on the JVM, its directories and store entries in the Go build's
  order (`RT.loadFromPath`, `RT.inArbacePath`, cut in the Go build's `RT`; `bin/arbace-j`
  appends its directories to the class path for resources); the checks above.

### The blob cache bounded (2026-10-12, the user's decision above)

Branch `box-net-merge`. go-whim's `DirStore` kept every blob it read, verified, for the whole
run (`cache map[[32]byte][]byte`): a box reading many large blobs grew the launcher's heap by
their sum, beside the guest's memory. The cache is now bounded by its total size, the least
recently used blob dropped first (BX38):

- **`DirStore.CacheMax`** (`vmm/store.go`): the most bytes of blobs kept; `OpenDirStore` sets
  `vmm.DefaultCacheMax`, **256 MiB** (the largest blob the launcher's store takes by default,
  `ARBACE_BOX_STORE_MAX`, so that any blob it takes fits; the cache costs nothing until blobs are
  read). A blob read (`Size`, `Get`, the existence check of `Put` and `SetRef`) goes to the
  front of a list (`container/list`); keeping one drops blobs from the back until the total fits.
  A blob larger than `CacheMax` is served and not kept: each `Size` and `Get` of it reads its file
  again (the filter allows no `pread`: a whole read) and holds it to its hash again. A blob
  dropped and read again is verified again, so a file changed under the store since is refused
  (`StoreRefused`), as a short write always was. `CacheMax` 0 keeps none. `DirStore.Cached()`
  says how many blobs and bytes it keeps.
- **The launcher's `ARBACE_BOX_STORE_CACHE=SIZE`**: bytes, or with `B`, `KiB`, `MiB`, `GiB`,
  `TiB` as `ARBACE_BOX_STORE_MAX` (the same parser, `storeSize`); `off` keeps none; by default
  256 MiB.
- **`TestDirStoreCache`** (`vmm/store_test.go`, run by `bin/box-vendor kvm-tests` with vmm's
  other tests): the default; nothing kept by `Put`; three blobs filling a 30-byte cache; the least
  recently used dropped first (a blob used again stays); a blob larger than the cache served and
  not kept, the others left; a dropped blob read again from its file; a kept blob served from the
  cache after its file changed, and refused once dropped and read again; `CacheMax` 0 keeps
  nothing new.
- **The smoke test** (`--smoke --box`): with `ARBACE_BOX_STORE_CACHE=1KiB`, a guest puts five
  blobs of 600 bytes and reads each back twice (all but the last read from their files again):
  their bytes as put.

Checks (after main `a8d59a0` merged): `bin/arbace-go --build` and `--build --box` (the launcher
4,705,915 bytes, with the guest 70,914,398), `--smoke --box` 101 of 101 (the new check
included), `bin/box-vendor kvm-tests` (`TestDirStoreCache` with vmm's tests) and `roundtrip` (16
of 16) pass; the rest of the checks with the wake on events' (below).

Amendment (proposed 2026-10-12, accepted by the user the same day): **BX38 (box/: vmm, the launcher) The blob cache bounded**:
`DirStore.CacheMax`, `vmm.DefaultCacheMax` (256 MiB), least recently used dropped first, a blob
larger than the cache read from its file at each use; the launcher's `ARBACE_BOX_STORE_CACHE`;
`TestDirStoreCache` and the smoke test's check.

## S5: the network, amd64 (2026-10-11)

Branch `box-s5`. BOX-SURVEY §6, S5: "the design first (R5): a stream proxy (`NetHost` over
calls: `Dial`/`Listen` under the launcher's policy) or packets (a datagram call, netstack and
TLS in the guest, go-whim's sketch); the socket REPL from the host". The design note came first
(below), then the user's decisions, then the implementation.

### The design note: a stream proxy or packets

What jrt asks of a network is already narrow: `NetHost` (A2G-SPEC §9.4, `net.ae`) is `Listen`,
`Dial`, `LookupIP`, `LookupAddr`, `Hostname`, `HasFamily`, `SetOption`/`GetOption` (by
`java.net.SocketOptions`' numbers) and `Available`, and gives back Go's `net.Listener` and
`net.Conn` *interfaces*: anything that reads, writes, closes and has deadlines serves. jrt's
`HostNet` natives on top (the socket table, the JVM's error kinds and texts) do not change with
the host. `java.net`'s classes reach no further (`HostSocketImpl` resolves names first and
gives `Connect0` an address literal, `Listen0` a literal or "" for every address). So both
designs end in the same place in jrt: a `NetHost` for the box; they differ in where TCP/IP runs.

| | stream proxy (TCP streams through calls) | packets (a datagram call, netstack in the guest) |
|---|---|---|
| guest side | a `NetHost` over about a dozen calls, its `net.Conn` and `net.Listener` over handles: about 400 lines of Go forms in jrt | gVisor's netstack (`gvisor.dev/gvisor/pkg/tcpip`: IPv4/6, TCP, UDP, ARP, a link endpoint over the datagram call; `gonet` gives `net.Conn`/`net.Listener`) plus its dependencies (`pkg/sync`, `atomicbitops`, `buffer`, `sleep`, `waiter`, `state`, `log`, `rand`, `golang.org/x/time/rate`...): by estimate (not measured here: the module is not in Go's module cache, and this work fetched nothing) several tens of thousands of lines of Go, a new third-party vendoring as Go forms (Apache-2.0, `LICENSE.md` has the text), whose `go` line follows Go's releases closely (R5's coupling twice over); a resolver in the guest (Go's, over `net.SocketFunc` for UDP to a DNS server the host forwards to); TamaGo's `net` already takes a `SocketFunc` for it (`net/net_tamago.go`) |
| host side | the monitor dials and listens with Go's `net`, a handle table, a goroutine reading each connection into a buffer and one writing from it: about 600 lines in `vmm` | a datagram relay, and something to turn the guest's packets into host traffic: a TAP device (root or `CAP_NET_ADMIN` on Linux, `vmnet` and an entitlement on macOS: against the box's point) or a user-space NAT, which is a TCP/IP stack on the host (gvisor-tap-vsock, slirp, passt): the stream proxy again, behind a second netstack |
| the image | unchanged but for the `NetHost` | netstack in every guest: a few MB more (estimate) |
| blocking (R2, R3) | no call waits: a dial and a lookup run in the monitor while the guest goes on, reads and writes take what the monitor's buffers hold or answer EAGAIN, and a waiting goroutine polls one count of events, sleeping 1 to 10 ms between polls, as the console does (S1, R3); a vCPU never stops in the monitor, the collector's stops of the world never wait for one | netstack's own timers (retransmission, delayed ACKs) run in the guest, which has no preemption (R2): a long evaluation on one vCPU delays them; packets arrive by polling the datagram call, as often, and a stream costs an exit per packet (1,500 bytes) where the proxy's costs one per call of up to 1 MiB |
| the host's control | the policy sees what is asked: listen at an address and port, dial an address and port, resolve a name; dial patterns can name hosts (`*.example.org:443`), since the host resolves names and remembers which addresses an allowed name gave | the relay sees packets: addresses and ports; a policy by name needs the guest's DNS snooped, or the NAT's own resolver forced |
| what the host sees | destinations and the bytes of each stream (plain text unless TLS) | the same destinations, and the same bytes unless TLS: the guest's netstack hides nothing from a host that relays its packets |
| TLS | in the guest either way, over any `net.Conn`; jrt's build has no TLS (no `SunJSSE`, no `javax.net.ssl`: A2G-SPEC, the providers are SUN and SunJCE, amendment SC4), so out of S5 for both; when it comes, the proxy carries the ciphertext | the same |
| DNS | the host's resolver, under the policy (only names the dial patterns name, and `localhost`); the guest's lookups are calls | the guest's resolver, through the relay to the host's DNS server; the policy sees UDP to port 53 |
| the socket REPL from the host | the guest listens (`arbace.server.repl`, as natively), the monitor's listener is on the host's loopback: a host client connects to it directly | a host-side port forward into the guest's stack (the NAT's) |
| loopback inside the box | a guest connecting to its own listener goes through the host's loopback (the policy allows the box's own listeners): socket options, `shutdown`, timeouts and errors are the host kernel's, as natively, which is what `forms/net.ae` checks | netstack's own loopback: its options and errors are netstack's, not Linux's |
| `bin/net-check`, `forms/net.ae` | unchanged, the box in place of the executable, with a policy that allows the host's loopback | the same, after port forwards |
| arm64, the Mac | nothing ISA- or hypervisor-specific: the same calls | the same, plus the relay's host side per OS |

**Recommended: the stream proxy.** It is what jrt's `NetHost` already is (Go's `net.Conn` and
`net.Listener`), a fraction of the size, needs no vendoring and no privilege on the host, never
blocks a vCPU, keeps the host's semantics where the oracle checks them, and gives the launcher a
policy by name. The packets design's one advantage, "the host sees nothing", does not hold: the
host relays the packets and sees what the proxy sees; only TLS hides content, and it works the
same over either. Netstack stays possible later (a `NetHost` over `gonet` beside this one), if
UDP, raw IP or box-to-box networking without the host's sockets are wanted.

### The user's decisions (2026-10-11)

- **D12** (BOX-SURVEY §10), decided as recommended: **the stream proxy** (TCP streams through
  hypercalls; the launcher dials and listens on the host).
- **The policy**: no network unless the launcher allows it (`ARBACE_BOX_NET`), listening on
  the loopback addresses by default, dialing by an allow-list of host:port patterns.
- **The scope**: TCP only; DNS resolved by the host under the same policy; TLS in the guest only
  if jrt's Linux build already supports it: it does not (no `SunJSSE`), so TLS is out of S5.
- **The socket REPL**: the same `arbace.server.repl` mechanism as natively (the system property,
  through `JAVA_TOOL_OPTIONS`, which the launcher passes), its port on the host's loopback by the
  launcher's listen policy.

### The calls (BX40)

`abi` and `vmm` number them from 40 (the store keeps 25-29; 30-39 are unknown calls, a fault),
`abi.NCalls` one past the last (53). Not host functions: not serialised with the console's, as
the store's and SMP's are not. A handle is the monitor's (0 up); an answer below 0 is `-errno`
(Linux's numbers, which TamaGo's `syscall` has too) or `NetErrTimeout` (-1000, a dial's time),
`NetErrNotFound`, `NetErrTemp` (a lookup's), `NetErrOther`. Addresses are `host:port` strings,
the host an IP literal, empty for every address.

| # | call | arguments, answer |
|---|---|---|
| 40 | `NetInfo` | ret 0 when the box has a network (-1 without), a[0] the host's families (1 IPv4, 2 IPv6) |
| 41 | `NetEvents` | ret a count of the changes a guest may wait for: data come, a connection accepted, a dial or a lookup done, room to write, a handle closed |
| 42 | `NetListen` | the address: ret a handle |
| 43 | `NetDial` | the address, a local one, a timeout in ms: ret a handle at once, the connection made in the monitor |
| 44 | `NetAccept` | a listener: ret a connection's handle, or EAGAIN |
| 45 | `NetRead` | up to n bytes of what the monitor's buffer holds: the count, 0 at the end, EAGAIN |
| 46 | `NetWrite` | queues up to n bytes (the queue holds 256 KiB): the count taken, EAGAIN when full |
| 47 | `NetClose` | closes the handle (an operation waiting on it ends) |
| 48 | `NetShutdown` | for reading, or for writing once the queue is written |
| 49 | `NetStatus` | 0, EINPROGRESS while a dial or a lookup runs, or its failure; a[0] what there is to read (`Available`) |
| 50 | `NetAddr` | the local or remote address into a buffer: its length |
| 51 | `NetOption` | sets or reads `java.net.SocketOptions`' option (the native `OSHost`'s mapping to the socket's, `vmm/net_linux.go`) |
| 52 | `NetLookup` | a name (or an address's names): ret a handle whose answer `NetRead` reads once `NetStatus` says 0: 16-byte addresses, or names |

The monitor's side (`vmm/net.go`, `Config.Net`, `vmm.NewNet`): Go's `net` on the host, a handle
table; each connection has a goroutine reading it into a buffer of up to 256 KiB (then waiting
for the guest) and one writing the guest's queue to it, a listener one accepting (up to 128
held), a dial and a lookup one each; each change adds one to the count. Nothing the guest asks
waits: a vCPU never stops in the monitor (R3), and the collector's stops of the world never wait
for one. The accepted and dialled connections get the JVM's defaults (no `TCP_NODELAY`, no
keep-alive), as `OSHost`'s.

### The policy (BX41)

`ARBACE_BOX_NET` (the launcher's; `vmm.ParseNetPolicy`): items separated by commas or spaces:
`listen=PATTERN`, `dial=PATTERN`, `listen` (`listen=loopback:*`), `loopback` (listen and dial on
the loopback addresses), `off`. A pattern is `HOST:PORT`: `*`, `loopback` (127.0.0.0/8, ::1), an
address or a CIDR prefix (`[...]` for IPv6), a name or `*.NAME`; the port `*`, `N` or `N-M`.
Unset, empty or `off`: no network, as before (S1: `NetInfo` answers -1, the guest's host has no
`NetHost`, "Network not available"); a value the launcher cannot read ends it (status 1).

- A listen at every address (`ServerSocket(port)`) is made at the loopback address of its
  family unless a pattern allows the wildcard itself (`listen=*:8080`, `listen=0.0.0.0:8080`).
  Elsewhere than the patterns: `EACCES` (`BindException: Permission denied`, as a privileged
  port natively).
- A dial must match a dial pattern, by address, or by a name that an allowed lookup gave that
  address (the host remembers what its lookups answered: `java.net` resolves names in the guest
  and connects to the address), or be one of the box's own open listeners (a box with `listen`
  alone talks to itself through the host's loopback, which `forms/net.ae` needs). Else `EPERM`
  (`SocketException: Operation not permitted`, as a firewall's refusal natively).
- A lookup is the host's resolver's, for names a dial pattern names (or `*`) and `localhost`;
  others are not found (`UnknownHostException: NAME: Name does not resolve`), without a query.
  A reverse lookup only when the policy dials `*`.
- The guest's host name is `localhost` (the box has none of its own; the host's is not the
  guest's to know).
- The system-call filter: `vmm.SeccompNet`, the filter with the sockets' calls and `uname` (Go's
  `net` reads the kernel's version) and `eventfd2` (its poller), only when there is a network.

### The guest (BX42, BX43)

`go/arbace/jrt/netbox_tamago.ae` (new, `//go:build tamago`): `ProgramHost` asks `NetInfo` once;
with a network the host is `boxNetHost`, `boxHost` with `NetHost`'s methods over the calls. Its
connections and listeners are `net.Conn` and `net.Listener` over handles (deadlines kept in the
guest), so net.ae's `HostNet` natives, the JVM's error kinds and texts are unchanged: errors come
back as the `syscall.Errno`s Linux's would be (TamaGo's numbers are Linux's), a dial's timeout as
`os.ErrDeadlineExceeded`, a closed handle as `net.ErrClosed`, a failed lookup as a
`net.DNSError`. Waiting (BOX-SURVEY R3): an operation that has nothing yet takes the poller's
channel, asks again, and waits for the channel or its deadline; one goroutine polls `NetEvents`
while some goroutine waits, from 1 to 10 ms between polls as the console's reader (S1), and
closes the channel when the count changed; it stops when nobody waits.

One defect found on the way (BX43): in the box, `JAVA_TOOL_OPTIONS`' `-D` options were not
system properties: a package variable's initializer made the properties before the box host's
`init` set the environment the monitor wrote, so `arbace.server.repl` was never seen. The
host's `init` now drops them, to be made again at their next use; `-Dfoo=bar` reaches
`System.getProperty` as natively.

### The socket REPL from the host

As natively (EXEC-NOTES.md, JRT-NOTES.md "Sockets"):

```
$ ARBACE_BOX_NET=listen JAVA_TOOL_OPTIONS='-Darbace.server.repl="{:port 5555 :accept arbace.core.server/repl}"' \
    target/arbace-go/box-amd64/arbace-box
$ nc 127.0.0.1 5555        # on the host
user=> (+ 1 2)
3
```

The guest listens at its loopback address; the monitor's listener is the host's
`127.0.0.1:5555`.

### Measured (amd64, nested KVM, the shared machine)

| | box, 4 vCPUs | box, 1 vCPU | native |
|---|---:|---:|---:|
| 64 MiB through a loopback connection within the program (64 KiB writes) | 194-207 ms | 227-244 ms | 32-36 ms |
| a socket REPL's `(+ 1 2)` from the host, median of 200 (p90) | 47.0 ms (48.0) | 47.0 ms (47.1) | 41.0 ms (42.0) |
| host CPU over 20 s idle, a socket REPL server listening (without one) | 1.82 s (0.66) | 1.02 s (0.53) | |

The REPL's round trip is mostly TCP's delayed acknowledgement against Nagle's algorithm (the
server writes the result and the prompt in two writes without `TCP_NODELAY`, the JVM's default),
natively too; the box adds about 6 ms, the poller's waits. An idle server waits in `accept`, so
the poller runs: about 99 `net_events` and as many `cpu_park` exits a second (one vCPU), half a
second of host CPU over 20 s on one vCPU and 1.2 s on four (the console's polling at the prompt
costs as much, S1). Throughput is about 330 MB/s in the box against 1.9 GB/s natively: an exit per
64 KiB written and read, under nested paging.

### Checks

- `bin/arbace-go --build` (amd64; 66,519,200 bytes: the Linux program's sources are unchanged)
  and `--build --box` (the guest 65,974,606 bytes; the launcher 4.6 MB, 3.7 before: Go's `net`;
  with the guest 70,627,678).
- `bin/arbace-go --smoke`: 42 checks pass. `--smoke --box`: 84 on 4 and 1 vCPUs and on a terminal,
  seven new for each vCPU count: no network without `ARBACE_BOX_NET` (S1's), a loopback
  connection, a dial the policy refuses, a listen it refuses, a name it refuses, `localhost` and a
  refused connection under `loopback`, an `ARBACE_BOX_NET` the launcher refuses, a socket REPL
  session from the host (`arbace.server.repl`, `ARBACE_BOX_NET=listen`).
- `bin/net-check` natively and `bin/net-check --box` (new: the launcher in place of the
  executable, `ARBACE_BOX_NET=loopback`): the socket REPL and io-prepl sessions equal the JVM's,
  8 concurrent sessions, `remote-prepl` from the box against the JVM's io-prepl, `start-server`
  and `stop-server`: 5 of 5 each.
- The box's oracle with `forms/net.ae` brought in (`ARBACE_BOX_NET=loopback`, `-j 12`): 21,779 of
  21,825 cases match, `forms/net.ae` 83 of 83 (S1's full run: 57 of 83); the 46 mismatches are
  `known-go-box-amd64.edn`'s, unchanged but for its `:about`. The native oracle: 22,232 of 22,278,
  the 46 of `known-go-amd64.edn`.
- `bin/box-suite` (`ARBACE_BOX_NET=listen` by default now): 67 namespaces, 685 tests, 19,647 of
  19,647 assertions; `java.io`'s `test-socket-iofactory` passes (15 tests, 115 assertions, as on
  Linux), `test/arbace-box-results.edn` updated.
- `bin/box-vendor roundtrip`: 16 of 16 at all three levels; `bin/box-vendor kvm-tests`: `hv`,
  `vmm` (with `TestABI` over the new numbers, `TestNetPolicy`, `TestNetStreams`,
  `TestNetLookup`) and the guest tests pass.
- `bin/tamago-vendor verify`: identical; `check`: std 371 of 371, the goos module 69 of 69, the
  guest image from the forms byte-identical to the one from the originals (76,837,431 bytes as
  linked, 65,974,570 cut): S6's proof holds. `tamago/` is unchanged.
- The essential `bin/gate` was not run: no source the bootstrap compiles changed (`box/`, `go/`,
  `bin/`, `test/`).

How `box/` was changed: Go written in a printed copy of `box/` (the new files, and the edits to
`vmm.go`, `abi.go`, `abi_test.go` and the launcher's `main.go`), converted by `bin/g2a convert` for
the five configurations as `bin/box-vendor` does; the new files' forms taken as converted, the
changed files' forms three-way merged into `box/`'s (`git merge-file` of `box/`'s, the converted
printing of `box/`, and the converted printing of the edit), so that their `;` comments stay; the
round trip and the KVM tests check the result.

### Amendments (BX40-BX44, proposed 2026-10-11, accepted by the user the same day)

- **BX40 (box/: abi, vmm) The network's calls** 40-52 (`NetInfo` ... `NetLookup`), not host
  functions, none waiting; `abi.NCalls` 53; the error answers `-errno` and `NetErr*`.
- **BX41 (box/: vmm, the launcher) The policy**: `ARBACE_BOX_NET`, `vmm.ParseNetPolicy`,
  `vmm.Net` (`Config.Net`), the listen at every address made at the loopback's, the box's own
  listeners dialable, names resolved by the host and remembered for the dial, reverse lookups only
  with `dial=*`; `vmm.SeccompNet` (the filter with the sockets' calls only with a network).
- **BX42 (A2G-SPEC §9.4; jrt) The box's `NetHost`** (`netbox_tamago.ae`): `boxNetHost` when the
  monitor gives a network, the poller of `NetEvents`, `Hostname` `localhost`, `HasFamily`
  `NetInfo`'s.
- **BX43 (A2G-SPEC §9.4; jrt) The box's system properties made after the environment**:
  `JAVA_TOOL_OPTIONS`' `-D` options in the box.
- **BX44 (checks)** `--smoke --box`'s network checks, `bin/net-check --box`, `forms/net.ae` in
  the box's oracle (`ARBACE_BOX_NET=loopback`), `bin/box-suite`'s `ARBACE_BOX_NET=listen` and
  `java.io` back, `vmm`'s network tests.

### The user's decisions on the open points (2026-10-11)

- BX40-BX44 accepted.
- The network's semantics kept as built: the guest's host name `localhost`; a listen at every
  address made at the loopback address of its family unless a pattern allows `*` (the server's
  `getInetAddress` then says 127.0.0.1, where the JVM says 0.0.0.0); a connection to one of the
  box's own listeners once it is closed refused by the policy (`EPERM`) under `listen` alone, as
  the real refusal (`Connection refused`) only under `loopback`; reverse lookups only with
  `dial=*`.
- TLS later, with JSSE (jrt's `SunJSSE`), in the guest over these streams.
- The box's checks in `bin/gate --full` (`--smoke --box`'s network checks, `bin/net-check
  --box`) stay deferred, with the box's smoke test (S1's decision).
- The idle cost of polling (the console's and the network's): replaced by a wake-on-event in the
  monitor (below, "Wake on events").

### Merged with S3 (2026-10-12)

Branch `box-net-merge`, from main `f79e53a` (S3 merged), `box-s5` (`b6addbc`) merged into it.
Git's merge conflicted in `box/README.md`, the docs (B1-PLAN, BOX-NOTES, BOX-SURVEY: both stages'
text kept), jrt's `hostbox_tamago.ae` (its header comment only: the store and the network both
named) and three of `box/`'s forms files: `abi/abi.ae`, `cmd/arbace_box.ae` (the package file:
its files' list, `net.go` and `store.go` both) and `cmd/arbace_box/main.ae`. The two forms files
conflicted where the Go did not: the forms carry the Go's line numbers (`:line`, `:go/end`), and
each stage's additions moved the other's lines. So they were merged as Go, as S1-S5 changed
`box/`:

1. `box/` printed (`bin/g2a build --print-only --tests --layout lines`) at the merge base
   (`4351f04`), at S3 (`f79e53a`) and at S5 (`b6addbc`), and the Go of `abi.go` and the
   launcher's `main.go` merged three ways (`git merge-file`): `main.go` merged cleanly (S3's
   `--store` and store opening, S5's `netConfig(&cfg)` after the filter's choice, so that a
   network's filter is `SeccompNet`); `abi.go` conflicted where both added constants after the
   calls, resolved by keeping S3's `Store*` block and S5's `NetErr*` block, the call numbers
   S5's (the network from 40, `NCalls` 53).
2. The merged Go (with S3's and S5's other files) vetted and converted back (`bin/g2a convert
   --tests`, linux/amd64: the files' forms are the same for every configuration), and the
   decorations of the hand-kept forms carried over: S3's `;` comments and literal spellings from
   S3's forms against S3's printing converted, S5's likewise, onto the merged conversion, with
   the line numbers normalized for the comparison and the conversion's put back.
3. Checked: `box/` printed after the merge is the merged Go, every package, byte for byte.

**A defect of S5's found by the merge's checks.** The box's oracle run with `forms/net.ae`
found one new mismatch, `forms/net.ae:91`: `(slurp (io/reader s))` read "" where the JVM reads
"via io\n", the peer having written the line and closed its socket. It failed on `box-s5`'s own
launcher too (4 of 4 runs here; S5's runs passed by timing): `NetClose` closed the host's
connection at once, and the bytes the guest had written and the monitor not yet sent (its
queue, `out`, or taken by the writer goroutine and not yet written) were dropped. A socket's
close sends what was written to it, so now a connection closed with bytes queued or being
written lingers (`netHandle.lingering`, `writing`): its writer goroutine writes them, then closes
it (after a write's error too); the end of the run (`Net.Close`) still closes everything at once.
vmm's new `TestNetCloseLingers` (20 connections closed right after 100,000 bytes queued, each
read whole by its peer) fails without the change and passes with it. Made in the Go and carried
into the forms as above.

Checks on the merge (amd64): `bin/arbace-go --build` (66,801,824 bytes, as S3's) and `--build
--box` (the guest 66,187,598 bytes, the launcher with it 70,906,206); `--smoke` 53 of 53;
`--smoke --box` 100 of 100 (S3's 86 and S5's 14); `bin/net-check` and `bin/net-check --box` 5 of
5 each; the box's oracle with `forms/net.ae` (`ARBACE_BOX_NET=loopback`) 21,779 of 21,825, the 46
of `known-go-box-amd64.edn`; the native oracle 22,232 of 22,278, the 46 of `known-go-amd64.edn`;
`bin/box-suite` 67 namespaces, 19,647 of 19,647 assertions; `bin/box-vendor roundtrip` 16 of 16
at all three levels and `kvm-tests` (with `TestNetCloseLingers`) pass; `bin/tamago-vendor verify`
(identical) and `check` (the guest image from the forms and from the originals identical:
77,067,117 bytes as linked, 66,187,562 cut). No source the bootstrap compiles changed.

## Wake on events, amd64 (2026-10-12)

Branch `box-net-merge`, the user's decision of 2026-10-11 (S5's open points): replace the
guest's polling -- the console's `WaitRead` without a wait, retried 1 to 10 ms apart (S1, R3),
and the network's EAGAIN loop over `NetEvents`, the same (S5) -- by a wait in the monitor that a
host event ends. Polled, an idle box made about 99 `wait_read` or `net_events` calls and as many
`cpu_park` exits a second: 0.5 s of host CPU per 20 s on one vCPU and 1.2 s on four at the
prompt or with a listening server.

### The design (BX50-BX54, accepted 2026-10-12)

What a wait must keep (BOX-SURVEY R2, R3): the guest's scheduler runs its timers and its other
goroutines (TamaGo's sleeping goroutines, a future's output while the REPL waits for a line),
the collector stops the world, and on four vCPUs the others run on. So no goroutine may stop its
vCPU in a call to wait; the wait belongs where the vCPU is idle anyway, the board's `Idle`
(`goos.Idle`, the scheduler's `beforeIdle`), which already parks in the monitor until the next
timer (S1, BX8). What was missing is a way for the host to end that park, and for the guest's
runtime to know, after it, which goroutine to run. Three parts:

1. **An event area and posts (BX50; `abi.EventArea`, call 53, `abi.NCalls` 54).** The guest
   names 16 bytes of its memory, two counts: `[0]` the events the monitor has posted (the
   monitor's to write), `[1]` those the guest has taken (the guest's). The monitor posts an event
   (`vmm/events.go`, `postEvent`): adds one to `[0]` in guest memory and ends every parked
   vCPU's park (the vCPUs' futex tokens, `vmm/park_linux.go`). It posts:
   - for the console, when input comes for a guest whose last `WaitRead` without a wait found
     none: that `WaitRead` asks a goroutine of the monitor's to watch the host's input
     (`watchInputs`), which waits for it (`select` on standard input, by the launcher's console's
     new `WatchInput`, the `vmm.InputWatcher` a Host may be; 500 ms at a time, to see the run's
     end) and posts once; input that comes while nobody waits for it posts nothing (the next
     `WaitRead` takes it);
   - for the network, at each change NetEvents counts (data come, a connection accepted, a dial
     or a lookup done, room to write, a handle closed): `Net`'s `notify`.

   **`CPUPark`** ends at once, or does not begin, while `[0]` and `[1]` differ, and answers 1
   when a wake or an event ended it (0 at its deadline or the machine's limit). An event that
   finds every vCPU running ends no park and is seen by the guest at its next look for work
   (below); one posted after a vCPU decided to park is seen by the park's first check (the post's
   count is written before the monitor's lock it takes to wake, which the park takes first).

2. **The board (BX51; `board.EventArea`).** `EventArea(area)` names the area (and says whether
   the monitor took it). One idle vCPU then parks until the next timer *or an event*: after a
   `CPUPark` that answers 1 it returns to the scheduler rather than spin to the deadline, and
   with no timer at all (the scheduler's `pollUntil` 0 or `MaxInt64`) it parks without a limit
   (`CPUPark(-1)`) where it returned at once before (a guest without timers could only spin, as
   nothing else woke it: go-whim's guests still do, having no area). Several vCPUs park as before
   (`idleSMP`), their parks ended by events too.

3. **The guest's runtime and jrt (BX52, BX53).** After a park the scheduler looks for work; the
   goroutines waiting for the host must then be runnable. TamaGo's runtime has no netpoll and
   its `beforeIdle` returns no goroutine; the board's `Idle` cannot ready one (it runs in the
   scheduler, on g0, without the runtime's internals). So jrt's runtime overlay (A2G-SPEC §9.3,
   the goroutine-local slot's) gains the mechanism wasm's runtime has for its event handler
   (`beforeIdle` returning the handler's goroutine): `arbace_local.go` keeps one goroutine parked
   in `arbace_eventPark` (`gopark`) and a hook (`arbace_setEventHook`); one line of
   `findRunnable` (proc.go, marked `; Arbace`), right after it runs the P's due timers, makes
   that goroutine runnable and runs it when the hook says an event came. At every look for work,
   idle or not: a vCPU that yields (the evaluator's `Gosched`, BX5) sees the event too. On Linux
   no hook is set: the line is one nil check per `findRunnable` (the executable's bytes change by
   the overlay; its speed does not, below). jrt (`hostbox_tamago.ae`): at `init` the box's host
   names `boxEventArea` (`board.EventArea`), sets the hook (`boxEventsPending`: the two counts
   differ, two atomic loads) and starts `boxEventLoop`, the goroutine that parks in
   `eventPark`, and when run takes the events (`[1] = [0]`) and closes the waiters' channel
   (`boxWaiters.wake`). A waiter takes the channel (`arm`), asks the monitor once (`WaitRead`
   without a wait, `NetRead`, `NetAccept`, `NetStatus`...), and when there is nothing yet waits
   for the channel (and the operation's deadline, a timer of its own: the guest's scheduler
   parks only until it). The console's reader (`boxConsole`) and the network's `boxRetry` are
   both such waiters; S5's `boxPoller` is gone. Without an area (a monitor that refuses it) the
   waiters are woken by a polling goroutine 1 to 10 ms apart, as before.

Not chosen: a blocking call (`WaitRead(-1)`, or a `NetWait`) on a goroutine of its own -- it
stops its vCPU in the monitor, so on one vCPU nothing else runs and on four the collector's
stops of the world wait for it (S1's R3 finding); a netpoll for TamaGo in the runtime overlay
(`netpoll_tamago.go`, the runtime's own blocking path) -- TamaGo's `findRunnable` never reaches
it (`beforeIdle` always answers `otherReady`), and it would be a larger patch of the runtime for
the same single goroutine; the board readying a goroutine from `Idle` by `go:linkname` into the
runtime's `goready` -- not allowed to a package outside the runtime (the linker's
`-checklinkname`), and unsafe from the scheduler.

### Measured (amd64, nested KVM, the shared machine; the same build before and after but for
this change)

| | 1 vCPU, before | 1 vCPU, after | 4 vCPUs, before | 4 vCPUs, after |
|---|---:|---:|---:|---:|
| host CPU over 20 s idle, the REPL at its prompt | 0.25 s | 0.00 s | 1.10 s | 0.00 s |
| the same with a socket REPL server listening | 0.39 s | 0.01 s | 1.26 s | 0.01 s |
| exits over the whole run (start, 22 s idle, end), prompt / server | 4,348 / 7,397 | 11 / 26 | 12,410 / 16,384 | 1,196 / 1,325 |
| a future's line printed while the REPL waits, after the guest's clock (10 lines, median) | 1.1-1.9 ms (1.6) | 1.4-2.4 ms (1.9) | 1.0-1.9 ms (1.5) | 1.2-2.1 ms (1.6) |
| a socket REPL's `(+ 1 2)` from the host, median (p90) of 200 | 47.0 ms (47.1) | 42.0 ms (42.0) | 47.0 ms (48.0) | 42.0 ms (42.0) |
| 64 MiB through a loopback connection within the program, ms (5 runs) | 207-255 | 191-239 | 164-183 | 147-183 |
| `(+ 1 2)` from the host while the evaluator computes for 2.1 s (1 vCPU) | 54 ms | 53 ms | | |

Before, polled: about 108 `wait_read` (and with a server as many `net_events`) and as many
`cpu_park` calls a second; after, an idle box makes no exit at all (the counts are the start's
and the end's: on 4 vCPUs the start's parks). The future's output was never polled (it is a
timer's), so its latency is unchanged; the socket REPL's round trip loses the poller's waits and
is now the native executable's (41 ms, TCP's delayed acknowledgement against Nagle's). With the
evaluator computing on one vCPU, the server still answers at once: the event is seen at the
evaluator's next `Gosched`, as the poller's timer was. A `-e` that used a future still ends
60 s later, as natively and on the JVM (the agents' pool), so the guest's timers run as before.
(Measured by `.tmp/bench/bench.py` of the branch's work: the launcher's process CPU from
`/proc`, its `ARBACE_BOX_STATS`.)

### The final checks (the branch with main `b76ad91` merged, amd64)

- `bin/build-arbace` (stage 2 rebuilds itself), `bin/jrt-convert`, `bin/jrt overlay --check`
  (proc.go is its conversion plus the three marked lines).
- `bin/arbace-go --build` (66,814,112 bytes) and `--build --box` (the guest 66,199,886 bytes, the
  launcher with it 70,918,494); `--smoke` 69 of 69; `--smoke --box` 103 of 103 (new: the bounded
  blob cache, and an idle REPL that makes no more than 6 `WaitRead`s in 2 s: before this change
  it made 132).
- `bin/net-check` and `bin/net-check --box`: 5 of 5 each.
- The box's oracle with `forms/net.ae` (`ARBACE_BOX_NET=loopback`): 21,779 of 21,825, the 46 of
  `known-go-box-amd64.edn`; the native oracle: 22,232 of 22,278, the 46 of `known-go-amd64.edn`.
- `bin/box-suite`: 67 namespaces, 685 tests, 19,647 of 19,647 assertions.
- `bin/box-vendor roundtrip`: 16 of 16 at all three levels; `kvm-tests`: hv, vmm (with
  `TestDirStoreCache`, `TestNetCloseLingers`) and go-whim's guest tests (its C and Go guests on
  the changed monitor and board) pass.
- `bin/tamago-vendor verify` (identical) and `check` (std 371 of 371, the goos module 69 of 69,
  the guest image from the forms and from the originals identical: 77,080,005 bytes as linked,
  66,199,850 cut): S6's proof holds.
- The essential `bin/gate`: passed (6 min 30 s).

### Amendments (BX50-BX54, proposed 2026-10-12, accepted by the user the same day)

- **BX50 (box/: abi, vmm) The event area**: `abi.EventArea` (call 53, `abi.NCalls` 54), the
  monitor's posts (`vmm/events.go`: console input for a reader that found none, the network's
  changes), a park ended by an event or not begun while one is not taken, `CPUPark`'s answer 1
  when a wake or an event ended it; `Stats.Events`.
- **BX51 (box/: board) `board.EventArea`**: one idle vCPU parks without a timer until an event
  once there is an area, and returns to the scheduler after a park an event ended.
- **BX52 (A2G-SPEC §9.3; overlay/go/runtime) The box's event goroutine in the runtime overlay**:
  `arbace_eventPark`, `arbace_setEventHook`, `findRunnable`'s line (`arbaceEventReady`).
- **BX53 (A2G-SPEC §9.4; jrt) The box's waiters**: `boxEventArea`, `boxEventLoop`,
  `boxWaiters` for the console's reader and the network's `boxRetry` (S5's `boxPoller` gone),
  polling kept only without an area.
- **BX54 (box/: vmm, the launcher) `vmm.InputWatcher`**: a Host whose input the monitor may
  watch from its own goroutine; the launcher's console's `WatchInput`.

## S4: arm64, KVM/arm64 and the Mac (design, 2026-10-12)

Branch `box-s4`, from main `b6824c7` (the tag `arbace-box-amd64-v1`). BOX-SURVEY §6, S4: "the
same image for arm64; KVM/arm64 in the emulated aarch64 VM here; Hypervisor.framework on the
Mac", with S6's arm64 half. It leads to the milestone `arbace-box-arm64-v1`, a tag on main as
`arbace-box-amd64-v1` (not a freeze). This section is phase 1: the study, what was tried here,
the design and the amendments proposed (BX60 onward); the implementation follows.

### The user's decisions (2026-10-12)

- (a) **purego vendored as Go forms**, like `box/`, reproducible by a script, its Apache-2.0
  licence in `LICENSE.md`.
- (b) **`tamago/` for arm64 as one tree**: the files both architectures share held once, the
  architecture-specific ones beside them, selected by Go's build constraints.
- (c) **The Mac bundle a tarball built here**, with a checklist script (ad-hoc codesigning with
  the hypervisor entitlement, its output saved for review).
- (d) **The milestone's gate is `bin/gate --full` plus the arm64 VM's box checks.** The Mac run is
  a follow-up after the tag: the bundle and its script are prepared in S4 but do not block it.

- **BX60-BX66 accepted** (2026-10-12, after phase 1): phase 2 as designed. The VM's Alpine pinned to
  a release by the files' SHA-256 (3.24.2). **Before the tag only the box's smoke test in the
  arm64 VM must pass**; the arm64 oracle subset and `bin/box-suite` are run once if the time
  allows, and are not a condition.

### Where arm64 stands (tried here, 2026-10-12)

Much of arm64 was written with S1-S5: the board's entry (`boot_arm64.s`) takes the wall clock
from X6 and the monitor sets it (`vmm/tamago_arm64.go`, `setWallClock`); the event area, the
store, the network, the console's wake and the environment are the ISA's neither in the board
nor in jrt (`hostbox_tamago.ae`, `netbox_tamago.ae` have no architecture of their own); and
`ARBACE_BOX_ARCHES=amd64,arm64 bin/arbace-go --build --box` builds both (arm64's guest from the
toolchain's tree: `tamago/` is amd64's): the arm64 guest 64,037,198 bytes, the launcher with it
68,559,198, 97 s for the guest's compile.

**The aarch64 VM works here.** go-whim's `guest/vm/aarch64.sh` (copied into the branch's
`.tmp/`; go-whim only read): QEMU 11.1.2's `qemu-system-aarch64 -machine
virt,virtualization=on,gic-version=3 -cpu max -accel tcg,thread=multi -smp 8 -m 6144`, Alpine
edge's aarch64 netboot files (Linux 6.18.42-0-virt, 36 MB) fetched from dl-cdn.alpinelinux.org,
booted in 32 s to its job agent, `/dev/kvm` there (KVM in VHE mode under the emulated EL2). On
this machine (AMD EPYC 7763, 64 cores, 33 GB; the disk at 49%):

- go-whim's `hv` arm64 tests (`TestDoorbellARM64`, `TestStoreAgainARM64`, `TestSysRegsARM64`,
  `TestCounterVCPUsARM64`), cross-compiled from a clone at `80c49bf`: **all pass** (0.4 s).
- **The arm64 box as built, unchanged, runs on KVM/arm64**: `arbace-box -e '(println (+ 1 2)
  (java.util.Date.))'` prints `3` and the host's date, status 0, in 13 s on one vCPU and 14 s
  on four (native amd64 in nested KVM: 0.3 s; under TCG everything is emulated, about 40x); the
  REPL on standard input (a future, `spit`/`slurp` in memory), the store's tool (`--store DIR
  put`), a `ServerSocket` under `ARBACE_BOX_NET=loopback` (once the VM's `lo` is up: Alpine's
  netboot brings up `eth0` only), the X6 wall clock: all as on amd64.
- **The one defect found: nil dereferences** (S1's open point). `(aget ^ints (identity nil) 0)`
  ends the guest: "the guest took exception 0 (code 0x96000007) at pc 0x1236250, address 0x8"
  (ESR EC 0x25, a data abort at the current EL, a level-3 translation fault on page 0). Natively
  and in the amd64 box it is a `NullPointerException`. Cases where jrt checks nil itself (a
  `String`'s method, `hashCode`, `locking`) already throw it on arm64.

**purego.** go-whim's `hv/hvf_darwin.go` and `libsys` import `github.com/ebitengine/purego`
v0.11.1 (module cache, `h1:2zpWRSQNVKN4eKsKO9eM1ILDgWfYMY9GwqRmK6XeQ/0=`); for darwin/arm64 with
cgo off its closure is three packages, `purego` (12 Go files, 4 assembly), `internal/fakecgo`
(14, 4) and `internal/strings` (1): 7,462 lines (S0's 8,052 and 34,081 counted every
configuration). No file names its own import path in a string; only the import specs do.

**The launcher for darwin/arm64.** `box/` printed and built `GOOS=darwin GOARCH=arm64
CGO_ENABLED=0` against purego from the module cache: `hv`, `vmm` and `libsys` compile; the
launcher fails on two names only, `execRlwrap` and `newConsole`, whose files are Linux's
(`rlwrap_linux.go`, `console.go`). `vmm` on darwin already has: no system-call filter, the
parker over a channel (`park_other.go`, which the events' posts end), the image's segments
copied (`mapfixed_darwin.go`), the guest's signal death by libsys; socket options answer
`ENOPROTOOPT` (`net_other.go`).

**`tamago/` for arm64.** std converted for tamago/arm64 (`bin/g2a convert --corpus arm64`: 378
packages, 1,738 files, 9 s) against `tamago/goroot/go` (amd64): of the file forms both have,
**1,683 are identical** and 11 differ; 41 files are amd64's only and 52 arm64's only (their
build constraints: `_amd64`, `_arm64`, `_noasm`, `_generic` ...); all 370 package files differ
(`:config`, `:init-order`, the file lists). The 11 differ only by facts of the configuration
that the converter records and the printer does not print: constants' values (`^{:val ...}`:
`goarch.IsAmd64`, `defaultGOARCH`, `runtime/internal/sys`'s, `cpuflags`, `bytealg`, `chacha8`'s
and the FIPS `cpu`'s) in ten, and in `poly1305.go` the embedded field a selector goes through
(`^{:go/via [macGeneric]}`: `mac` is another type on arm64). The goos module for tamago/arm64
(its `arm64`, `arm64/gic`, `arm/...` and the arm64 boards and SoCs) is to be counted as amd64's
69 were.

### The design

1. **The arm64 guest (BX60).** `board.NilPanics` on arm64: the vectors' synchronous entry
   from the current EL on SP_EL0 (entry 0, where Go code runs: EL1t) checks, before it reports,
   for a data abort at the current EL (ESR_EL1's EC 0x25) at an address below 0x1000 with
   `sigpanicPC` set; then it makes the faulting instruction's frame call `sigpanic` as Linux's
   signal handler does on arm64 (the runtime's `sigctxt.preparePanic`, `signal_arm64.go`):
   SP_EL0 lowered by 16, the interrupted LR (X30) stored at the new SP and the frame pointer
   (X29) below it, LR set to ELR_EL1 (the faulting PC), ELR_EL1 set to `sigpanicPC`, and `ERET`
   (SPSR_EL1 returns to EL1t, every other register as it was; R28, `g`, is Go's). The vector's
   scratch registers are saved on SP_EL1 and restored on this path; every other exception is
   reported by `Fault` as before. Written in the Go assembly of `boot_arm64.s`, carried into
   `box/` as the `.s` files are (copied). Nothing else of the guest is arm64's: the clock (X6),
   the environment, the event area, the store and the network are shared and already run.
   `bin/arbace-go --build --box` with `ARBACE_BOX_ARCHES=arm64` (or both) as today; the default
   stays amd64 (an arm64 guest's compile adds about 100 s), the gate's box step building both.
2. **KVM/arm64 here (BX64, BX65).** `bin/box-arm64-vm`, after go-whim's `aarch64.sh` (the same
   commands: `fetch`, `start`, `put`, `run`, `stop`, the job agent over QEMU's `guestfwd`, nothing
   installed on the host), its state in `.tmp/box-arm64-vm/`, with:
   - the netboot files of a **pinned Alpine release** (a `vX.Y` release's `netboot-X.Y.Z`
     directory, the files' SHA-256 pinned in the script), not edge, whose files change daily;
     the VM's packages (`bash`, `python3`, `rlwrap`, `dropbear`) from that release's repository;
   - `lo` brought up, and **an `exec` channel**: dropbear in the VM, QEMU's `hostfwd` from the
     host's loopback to it, a key made under `.tmp/`, so that `bin/box-arm64-vm exec CMD` runs a
     command in the VM with standard streams and status (the job agent's batch polling, 1-2 s a
     job, does for `put` and long jobs only); the oracle's candidate then is `bin/box-arm64-vm
     exec /h/arbace-box -`;
   - `smoke`: the box's smoke test run in the VM. `box_smoke` moves out of `bin/arbace-go` into a
     script of its own (`bin/box-smoke LAUNCHER`; `bin/arbace-go --smoke --box` calls it), so that
     it runs where the launcher is, with the checks that need the host's JVM skipped when there
     is none (the shared store's JVM half) and the native arm64 executable, which runs in the VM,
     in place of the amd64 one for the native half;
   - `hv`: `bin/box-vendor kvm-tests`' tests cross-compiled for linux/arm64 (`hv`, `vmm`, and
     go-whim's arm64 guest tests where they build) and run in the VM;
   - `oracle`: the box's oracle subset on arm64, against `test/oracle/known-go-box-arm64.edn`
     (expected the amd64 box's 46, the arm64 executable's own differences, and timeouts set for
     emulation); `suite`: `bin/box-suite` with the launcher in the VM, if the time allows.
   - The VM's size: `-smp 16 -m 12288` (8 and 6 GiB in go-whim's): the oracle runs a box per
     corpus file, each touching about 260 MB, at `-j 8`. Started and stopped by the script, never
     left running (`stop`, and `run`'s callers stop it on exit).
3. **The Mac (BX61, BX62, BX66).**
   - **purego as Go forms** (decision a): `box/go/arbace/box/purego.ae` and its directory,
     `purego/internal/fakecgo`, `purego/internal/strings`: the import path renamed to
     `arbace/box/purego` as go-whim's packages were (S0), the import specs the only change to the
     text; converted for darwin/arm64 only (the one configuration that imports it), the assembly
     copied; `bin/box-vendor` gains the step (the module from Go's module cache at v0.11.1, its
     hash checked) and its proofs: gocmp of each printed package at the three levels, and the
     darwin/arm64 launcher built from the printed forms byte-identical to one built against the
     module with the imports renamed back (`-trimpath -buildvcs=false -ldflags=-buildid=`, as
     S0). `LICENSE.md` names purego's files under its Apache-2.0 section. Alternative, not
     recommended: keep the path `github.com/ebitengine/purego` (a `replace` to a printed
     directory; no rename to prove, but a second module in the launcher's build).
   - **The launcher on darwin/arm64**: the console for darwin (`console.go`'s `select` with
     darwin's `FdSet`, which `syscall` has: the file tagged `unix` or a darwin twin), rlwrap on
     the Mac (Homebrew's rlwrap; the file made `unix` with each system's `TIOCGWINSZ` and
     `isatty`), socket options on darwin (`vmm/net_darwin.go`: `java.net.SocketOptions`' numbers
     mapped to darwin's `setsockopt`, so that `forms/net.ae`'s options answer as natively), the
     image beside the launcher (`arbace-box.elf`: the code signature covers the Mach-O, so the
     image is not appended there, as go-whim's Mac runs). Built here: `bin/g2a build --module
     arbace --goos darwin --arch arm64` (`--goos darwin`, cgo off: new for the launcher).
   - **The bundle** (decision c): `bin/box-mac-bundle` makes
     `target/arbace-go/arbace-box-mac-arm64.tar.gz`: the launcher, the arm64 guest
     (`arbace-box.elf`), `hv.test` for darwin/arm64, the entitlements plist
     (`com.apple.security.hypervisor`), a `README` and `mac.sh`, modelled on go-whim's
     `guest/mac/mac.sh`: `check` (`sw_vers`, `uname -m`, `sysctl kern.hv_support`), `sign`
     (`codesign --sign - --entitlements ... --force` on the launcher and `hv.test`, displayed), `hv`
     (the framework's tests), then the checklist, each step's output and status saved under
     `mac-results/` and packed into `mac-results.tar.gz` for the user to send back: `-e` with the
     wall clock, the REPL's session on standard input (the smoke test's transcript), a script and
     `-m` from memory, a nil dereference, a future during a long evaluation (R2), the store (put,
     ref, a script by `ref:NAME`, the tool), the network on loopback (a connection within the
     box, the socket REPL from the host by `nc`), idle CPU (the launcher's CPU time over 20 s at
     the prompt, 1 and 4 vCPUs: the events' wake on macOS's parker), the start's and three
     evaluations' times, on 1 and 4 vCPUs; and `run [ARGS]`, the REPL. Not a gate (decision d):
     the follow-up after the tag.
4. **S6's arm64 half (BX63).** One tree (decision b): `tamago/goroot/go` holds each file's forms
   once where both configurations convert it alike (1,683 of std's), and the files only one
   architecture builds beside them, as their build constraints select (41 amd64's, 52 arm64's);
   the package files are merged as `box/`'s were (the union of the files, sorted; no `:config` or
   `:init-order`: the printer and gc derive them). The 11 files whose forms differ only by the
   configuration's facts are held once per architecture, as the converter writes them, in
   `tamago/goroot/arch/amd64/go/` and `.../arm64/go/` (same paths), which `print` lays over the
   shared tree for the architecture it prints (the shared tree has no copy of them). Not chosen:
   dropping the annotations in the shared copy (one file, but the tree no longer the converter's
   output); a second tree (32 MB more, 99% the same). The goos module likewise (the union of its
   amd64 and arm64 packages). `bin/tamago-vendor` gains `--arch` (`convert`, `print`, `check`
   for each of amd64 and arm64; `verify` converts both and merges as `convert` does); `check`
   proves the arm64 guest image too (built from the forms and from the originals, identical as
   linked and cut); `bin/arbace-go --build --box` builds arm64's guest from the forms too.
   S6's amd64 proof must keep holding (`verify`, `check`), and the amd64 image's bytes are
   unchanged by the merge (the printed amd64 tree is the same Go).

### Amendments (proposed 2026-10-12)

- **BX60 (box/: board) `board.NilPanics` on arm64**: the vectors' return to `sigpanic` on a data
  abort below the first page, as `preparePanic` on arm64 (SP_EL0, LR, FP, ELR_EL1); the smoke
  test's nil check and the oracle on arm64.
- **BX61 (box/: the launcher, vmm) The launcher on darwin/arm64**: the console and rlwrap for
  darwin, `vmm/net_darwin.go`'s socket options, the image beside the launcher on the Mac;
  `bin/g2a build --goos darwin` for the launcher.
- **BX62 (box/, `bin/box-vendor`, LICENSE.md) purego v0.11.1 as Go forms**: `arbace/box/purego`
  (with `internal/fakecgo`, `internal/strings`), darwin/arm64, renamed imports; its proofs in
  `bin/box-vendor check`/`roundtrip` (gocmp; the darwin launcher byte-identical to the module's).
- **BX63 (`tamago/`, `bin/tamago-vendor`, BX23's extension) tamago/arm64 in the same tree**: the
  shared files once, each architecture's beside them, the package files merged, the 11 files
  differing by the configuration's facts under `tamago/goroot/arch/ARCH/`; the goos module's
  arm64 packages; `--arch`; the arm64 guest from the forms and its byte identity.
- **BX64 (bin/) `bin/box-arm64-vm`**: the aarch64 VM after go-whim's `aarch64.sh`, a pinned
  Alpine release, the `exec` channel (dropbear, `hostfwd` on the loopback), `smoke`, `hv`,
  `oracle`, `suite`; state in `.tmp/box-arm64-vm/`.
- **BX65 (checks) The box's checks on KVM/arm64**: `bin/box-smoke LAUNCHER` (out of
  `bin/arbace-go`), run in the VM; `test/oracle/known-go-box-arm64.edn`; the arm64 KVM tests;
  together the milestone's arm64 gate beside `bin/gate --full` (decision d), run by
  `bin/box-arm64-vm check` (not inside `bin/gate --full`: it takes an hour or more).
- **BX66 (bin/) The Mac bundle**: `bin/box-mac-bundle`, `mac.sh` and its checklist, its saved
  results; the user's run, after the tag.

### Estimates (this machine)

| item | work | runs |
|---|---|---|
| BX60, the arm64 nil panics | 1-2 h | the VM's checks |
| BX64/BX65, the VM's script and the smoke test moved | 3-4 h | a smoke run in the VM: about 1 h (13-30 s a box, about 100 boxes, some in parallel); the oracle subset: 1-2 h at `-j 8`; `bin/box-suite`: about 3 h (natively 222 s at `-j 12`, about 40x), if time allows |
| BX63, `tamago/` for arm64 | 3-4 h | `convert`/`verify` about 1 min, `check` about 4 min (two guests) |
| BX61, BX62, BX66, purego, the darwin launcher, the bundle | 4-5 h | the round trip and proofs, minutes |

The whole, with the checks: about two days of this machine's time.

## S4: arm64 and the Mac, done (2026-10-12)

Branch `box-s4`, phase 2 of S4 as designed above (BX60-BX66, accepted). What was made, in the
order of the commits:

### The arm64 guest: nil dereferences (BX60)

`boot_arm64.s`: the vectors' first entry (a synchronous exception at EL1 on SP_EL0, where Go code
runs) branches to `vector_sync_sp0`, which keeps R0 and R1 on SP_EL1 and, when `sigpanicPC` is
set (`board.NilPanics`), ESR_EL1's class is 0x25 (a data abort at the current EL) and FAR_EL1 is
below 0x1000, makes the faulting instruction call `sigpanic` as the runtime's `preparePanic` does
for a SIGSEGV on arm64: SP_EL0 lowered by 16, the interrupted LR stored at the new SP and the frame
pointer below it, LR the faulting PC (ELR_EL1), ELR_EL1 `sigpanic`, `ERET` (SPSR_EL1 returns to
EL1t, the flags with it). Any other exception is reported by `Fault` as vector 0, as before. The
board's doc strings name both ISAs. In the VM, `(aget ^ints (identity nil) 0)`, `alength`,
`.length` and `locking` on nil are `NullPointerException`s (the first ended the guest before:
"exception 0 (code 0x96000007) ... address 0x8"). Nothing else of the guest is arm64's.

### The aarch64 VM and the smoke test there (BX64, BX65)

- **`bin/box-arm64-vm`** (`fetch`, `start`, `put`, `exec`, `sh`, `stop`, `status`, `smoke`, `hv`,
  `oracle`, `check`; its state in `.tmp/box-arm64-vm/`): Alpine 3.24.2's aarch64 netboot files
  (`netboot-3.24.2`: `vmlinuz-virt`, `initramfs-virt`, `modloop-virt`, Linux 6.18.52-0-virt),
  their SHA-256 pinned in the script; `qemu-system-aarch64 -machine
  virt,virtualization=on,gic-version=3 -cpu max -accel tcg,thread=multi -smp 16 -m 12288`; an
  apkovl served by the script through QEMU's `guestfwd` (go-whim's mechanism) that installs
  `bash python3 rlwrap dropbear coreutils procps` from the release's repositories at boot, brings
  `lo` up (`/etc/network/interfaces`) and starts dropbear with a key made for the VM; the commands
  go in by ssh through `hostfwd` on a free port of the host's loopback. Booted to ssh in 50 s. A
  step that starts the VM stops it at its end; `check` (start, smoke, hv, stop) stops it however it
  ends.
- **`bin/box-smoke LAUNCHER`**: `bin/arbace-go`'s `box_smoke` made a script of its own (the store's
  checks, shared with the native smoke test, in `bin/lib/store-checks.bash`), so that it runs where
  the launcher is: `bin/arbace-go --smoke --box` calls it for the host's architecture,
  `bin/box-arm64-vm smoke` copies it, the store's checks, `test/arbace-go-rlwrap.py`, the arm64
  launcher and the native arm64 executable (`--native`, for the shared store directory) into the
  VM and runs it there with `--slow 30`: the times it waits and tolerates multiplied (the launcher's
  timeout, the wall clock's tolerance, the waits before input; the pty test's timeouts by
  `ARBACE_SMOKE_SLOW`), and S2's 1e7-element reduction over a reducer made 1e5 (15 minutes and more
  emulated). Unchanged on the host: 103 of 103.
- **Emulation's cost**: everything in the VM is emulated (TCG), and KVM/arm64 runs nested in it:
  the box starts in 13-20 s (0.3 s natively in nested KVM here), `(reduce + 0 (map inc (range
  1000000)))` takes 86 s on one vCPU (about 0.9 s natively): 40x to 100x.
- **`bin/box-vendor kvm-tests-arm64 DIR`**: go-whim's renamed clone with `box/` printed, as
  `kvm-tests`, and `hv`'s and `vmm`'s tests compiled for linux/arm64; `bin/box-arm64-vm hv` runs
  them in the VM (go-whim's guest tests build their guests as they run: not there).

### tamago/ for both architectures (BX63)

`bin/tamago-vendor convert` converts std and the goos module for tamago/amd64 and tamago/arm64 and
merges them (`merge_trees`): std, 371 and 370 packages, 1,759 files once, the 11 that differ by the
configuration's facts in `tamago/goroot/arch/{amd64,arm64}/go/`, 371 package files merged; the goos
module, 69 and 62 packages, 75 merged, 189 files, none differing; the headers
`runtime/cgo/abi_amd64.h` and `abi_arm64.h`. 2,416 forms files, 2,567 files, 32.6 MB (32.2 MB for
amd64 alone): arm64 adds 0.4 MB. `print` lays `arch/amd64` over the shared tree; one printing
serves both guests (`check`'s step 0 proves arm64's files print the same Go). `check [ARCH]` runs
S6's proofs for each architecture: the goos module's packages that build for it (`go list
-export`), std against `roundtrip-ARCH.edn`, and the guest images. `bin/arbace-go --build --box`
builds the arm64 guest from the forms too; the gate's `box-forms` builds and checks both.

| `bin/tamago-vendor check` | amd64 | arm64 |
|---|---|---|
| std printed (g2a-check, the round trip's reference) | no regression (371 + `unsafe`) | no regression (370 + `unsafe`) |
| the goos module (gocmp, three levels) | 69 of 69 | 62 of 62 |
| the guest as linked, from the originals and from the forms | 77,080,005 bytes, identical | 74,904,477 bytes, identical |
| the guest as shipped (symbols cut) | 66,199,850 bytes, identical | 64,037,162 bytes, identical |

The amd64 image is the bytes it was on main (S6's proof holds); `bin/tamago-vendor verify`: the
tree is a fresh conversion, byte for byte.

### purego and the launcher on darwin/arm64 (BX61, BX62)

- **purego** (`bin/box-vendor purego`; box/README.md): its packages' darwin/arm64 files, 41 files
  with the assembly and headers, 0.24 MB of forms, at `arbace/box/purego`; `hv/hvf_darwin` and
  `libsys` import it there (their import specs edited). The risk named in phase 1 is cleared:
  `internal/fakecgo`'s `go:linkname`s name the runtime's and libc's symbols, not its own path, and
  the renamed package links (the linker's `-checklinkname` passes). `bin/box-vendor purego-check`:
  the three packages printed against the renamed originals pass gocmp at all three levels; the
  darwin/arm64 launcher (3,492,882 bytes without DWARF) from the printed forms is identical to
  one from the renamed originals, and at purego's own path (3,494,034 bytes) to one from
  purego's module, both by a `replace` to the same directory (the build information names it).
  Found on the way: with DWARF the launchers differ in four `DW_AT_decl_line`s: in purego's
  `collectStackArgs` (`struct_arm64.go:442-444`) the parameter list spans three lines, and the
  printer, in the lines layout, puts every parameter name on the list's first line and places
  the types by `/*line*/` directives, so the names' declaration lines move (the code and the line
  tables do not). So the proofs link with `-w`, as the box's guests are (S6); the printer's
  placement of a parameter group's names is left as a note for g2a (the round trip's levels do
  not see it).
- **The launcher for macOS**: `console.go` builds for Linux and macOS, its `select` through
  `selectRead` (`console_linux.go`; `console_darwin.go` counts the ready descriptors, as macOS's
  `select` returns none, go-whim's `editor/term` `sel`) and `tcgetattr`'s request `tcgets`
  (`TCGETS`, `TIOCGETA`); `rlwrap_linux.go` became `rlwrap_unix.go` (`linux || darwin`; the parent's
  name from `/proc` on Linux only: on macOS `ARBACE_RLWRAP=wrapped` alone keeps the child from
  wrapping again); `vmm/net_linux.go` became `net_unix.go` (`linux || darwin`), `SO_REUSEPORT` in
  `net_linux.go` (15) and `net_darwin.go` (macOS's 0x200), `net_other.go` for neither. The forms
  were edited by hand where a file was renamed or a call changed (the lines kept), the new files'
  forms converted from their Go. `bin/g2a build --goos darwin` builds it (cgo off). Not run on a
  Mac here: built, vetted (linux/amd64, linux/arm64, darwin/arm64) and its round trip passes.

### The Mac bundle (BX66)

`bin/box-mac-bundle` makes `target/arbace-go/arbace-box-mac-arm64.tar.gz` (21.7 MB): the
darwin/arm64 launcher (4,896,434 bytes), the arm64 guest as `arbace-box.elf` beside it
(64,037,198), `hv.test` for darwin/arm64, `arbace-box.entitlements`, `README.txt`, `VERSION` (the
describe and the files' SHA-256) and `mac.sh` (`box/mac/`, after go-whim's `guest/mac/mac.sh`):
`check` (macOS, the chip, `kern.hv_support`), `sign` (ad hoc, the hypervisor entitlement), `hv`
(the framework's tests), `checklist` (on 4 and 1 vCPUs: `-e`, the wall clock, the REPL on standard
input, a script and `-m` from memory, an error's status, `System/exit`, files in memory, nil
dereferences, a future and `send`, R2, R3, no processes; the network: none by default, a loopback
connection, socket options, a socket REPL from the Mac by `nc`; the idle REPL's CPU over 20 s
(under 0.5 s), the times of the start and three evaluations, best of 5; the store: the tool, a
guest's `load`, `get`, `ls`, a script by `ref:`, compare-and-set), `all` (the four, then
`mac-results.tar.gz` to send back) and `run`. Linux's `timeout` and `shasum` are not assumed on
the Mac (perl's alarm; `shasum`). The checklist ran here on Linux with the amd64 launcher: 42 of
42. The user's run on the M2 Max is the follow-up after the tag (decision d).

### Checks (the branch with main `5f8a469` merged)

- **The milestone's arm64 gate, `bin/box-arm64-vm check`** (decision d): the box's smoke test on
  KVM/arm64 in the VM, **103 of 103** on 4 and 1 vCPUs, the store, the shared directory with the
  native arm64 executable and the REPL under rlwrap on a pseudo-terminal (38 minutes); go-whim's
  `hv` tests (4: `TestDoorbellARM64`, `TestStoreAgainARM64`, `TestSysRegsARM64`,
  `TestCounterVCPUsARM64`) and `vmm`'s (15) on box/: all pass; the VM stopped at the end.
- **amd64 unchanged**: `bin/arbace-go --build` (66,814,112 bytes, as main's) and `--build --box`
  (the guest 66,199,886 bytes, as main's; the launcher 4,715,847), `--smoke` (96: amd64's 69 and
  arm64's under qemu-aarch64), `--smoke --box` 103 of 103 (through `bin/box-smoke`); the box's
  oracle subset (`ARBACE_BOX_NET=loopback`) 21,779 of 21,825, the 46 of `known-go-box-amd64.edn`;
  the native oracle 22,232 of 22,278, the 46 of `known-go-amd64.edn`.
- `bin/box-vendor roundtrip`: **20 of 20** at all three levels (16 before: darwin/arm64 gained the
  launcher and purego's three packages); `kvm-tests`: hv, vmm and go-whim's guest tests pass on
  /dev/kvm; `purego-check` as above; `kvm-tests-arm64` as above.
- `bin/tamago-vendor verify` (identical) and `check` (both architectures, as tabled above).
- `bin/box-mac-bundle`: the tarball; the checklist on Linux amd64: 42 of 42.
- Not run (the user's decision: not conditions of the tag): **the arm64 oracle subset** was
  started once (`bin/box-arm64-vm oracle`, `-j 12`, four vCPUs a box) and stopped after 1 h 40 min
  with no corpus file reported: under emulation a file took 30 to 60 minutes (the amd64 box's whole
  subset is 352 s of runs; `harvest/reducers.ae` alone 61 s natively, beyond the run's 2-hour
  timeout emulated); estimated at 4 to 5 hours or more. `bin/box-suite` in the VM was not tried
  (natively 222 s at `-j 12`: some hours there). `test/oracle/known-go-box-arm64.edn` is not
  made: the script writes it on a first complete run.
- The essential `bin/gate` was not run: no source the bootstrap compiles changed (`box/`,
  `tamago/`, `bin/`, `test/`, the docs; `arbace/g2a`, a tool's).

### For the milestone

- `bin/gate --full` (the user's; its `box` step now also runs `bin/box-vendor purego-check`, and
  `box-forms` builds and checks both guests: about 100 s more) and `bin/box-arm64-vm check`.
- The Mac: `bin/box-mac-bundle`, then on the M2 Max `./mac.sh all`, and `mac-results.tar.gz` back:
  the follow-up after the tag. Untested before it: the launcher on macOS at all (its console's
  `select`, the image read beside it, purego's `fakecgo` under the renamed path, HVF with this
  guest), and `board.NilPanics` on Hypervisor.framework (the same vectors as KVM/arm64's).
- Left for later: the printer's placement of a multi-line parameter list's names (DWARF only);
  the arm64 oracle in the VM with fewer vCPUs a box, or on an arm64 host with KVM.

### The Mac run (2026-10-12)

The user ran the bundle (built from `120da5f`; the launcher's and the guest's code unchanged up
to the tag `arbace-box-arm64-v1`, only `box/mac/` and docs added) on macOS 14.8.7 (23J520), an
Apple M2 Max (12 cores, 96 GB, `kern.hv_support: 1`): `./mac.sh all`, every step status 0.

- Signed with `com.apple.security.hypervisor` (ad hoc): the launcher and `hv.test`.
- go-whim's hv tests on Hypervisor.framework: 4 of 4 (doorbell, store again, system registers,
  counter with vCPUs).
- The checklist 42 of 42 on 4 and 1 vCPUs: `-e`, the Mac's wall clock, the REPL, stdin and
  in-memory scripts, `-m`, errors and `System/exit`'s status, files in memory only, nil
  dereferences as NullPointerException (BX60 on HVF), futures and `send`, R2, R3, no processes;
  no network without `ARBACE_BOX_NET`, loopback connections, socket options, a socket REPL from
  the Mac by `nc`; the store (put and `arbace.box/load`, get, ls, `ref:NAME`, compare-and-set).
  So the console's `select` on darwin, the image read beside the signed launcher, purego's
  `fakecgo` under `arbace/box/purego` and HVF with this guest all work.
- Idle at the prompt: 0.01 s of CPU over 20 s (4 vCPUs: 505 exits, 449 parks; 1 vCPU: 14 exits).
- Times, best of 5 (4 vCPUs / 1 vCPU): `nil` 171 / 162 ms (the start), `(reduce + (map inc
  (range 1000000)))` 449 / 525 ms, `(fib 25)` 29 / 28 ms, sorting 200,000 strings 135 / 151 ms.

### The printer's name lists: purego-check with DWARF (2026-10-12)

The user's decision after S4: fix the placement of a multi-line parameter list's names now, so
that `bin/box-vendor purego-check` compares the launchers with DWARF.

- **Cause.** The forms already record the lines (SPEC §10.1: a name that begins a Go line
  carries `:line`, `^{:line 444 :tag (slice any)} keepAlive`); the printer did not use them.
  `param-list` handed `element-list` groups of entries (maps, which carry no `:line`), so no
  group broke its line, and the names of a group were written without looking at theirs; only
  the type forms, lists with the reader's `:line`, reached their lines, by `/*line :N:1*/`
  directives (no line break is allowed after a name). gc declares a parameter at its name's
  line, so DWARF's `decl_line` moved (`collectStackArgs`' `keepAlive` and `pNumInts`... at 442
  instead of 444 and 445: the four differences); the code and line tables did not.
- **The change** (`arbace/g2a/print_code.ae`; the forms are unchanged, so SPEC.md is, and so
  are `box/` and `tamago/`): each name of a parameter, result, type parameter or struct field
  group and of a const or var spec goes on its recorded line (`name-list`; a break after the
  comma); `element-list` takes the line of a group (its first name's, else its type's); a name
  that begins a line starts a group of its own outside `:go/grouped` (`a,\n b int` prints as
  `a int,\n b int`: N3, gc's output is the same); outside generic declarations names of equal
  type, doc and tag group (a field's doc is each of its names': `h1, h2 huffmanDecoder` under a
  doc comment had been split in two fields with the doc twice, the second name lines below its
  own), and `*explicit-groups*` (a file with `:go/grouped` marks) now applies to generic
  declarations only, where the converter marks. The parameters' `)` stays on the line of the
  results; a function whose signature spans lines indents its body from the line `func` began
  on; the values of a spec or a return after the first follow their `:go/breaks` (`= 1, 2,\n
  3`). Notes: PRINTER-NOTES.md, Layout. `test/g2a/golines` now also compares the line of every
  name a field list or a spec declares (named struct fields as their names), and the printer's
  case `test/g2a/cases/s10_names` (struct fields, a var spec, parameters, results, a function
  literal, an interface method, type parameters spanning lines) checks it.
- **purego-check with DWARF**: the launcher links with `-ldflags=-buildid=` (no `-w`): the
  darwin/arm64 launcher from the printed purego is identical to one from the renamed originals
  (4,896,402 bytes with DWARF), and at purego's path to one from purego's module (4,897,554).
- **Checks** (`g2a-params`, from main `a6a6b86`): `bin/g2a-print-tests` 5 tests, 262 assertions,
  all pass; `bin/g2a roundtrip amd64` and `linux/amd64`: no regressions (std 373 and 376, the
  programs 1,707 and 1,713, every level as recorded); `bin/box-vendor roundtrip` 20 of 20;
  `bin/tamago-vendor verify` identical; `bin/arbace-go --build --box` for amd64 and arm64, then
  `bin/tamago-vendor check`: both guest images identical to the originals', as linked and with
  the symbol table cut. golines over std printed for tamago/amd64 (1,742 files against
  GOROOT/src): 1,622 have the original's lines throughout (1,574 before); of the rest, 70 are
  the known `var ( ... )` groups and labels in function bodies printed a line off, and 50 the
  fields and methods of struct and interface types written inside a declaration's `:tag` (a
  `var x struct { ... }`, an interface parameter type): the forms put such a type on the
  declaration's line (SPEC §10.1, lists inside metadata), so its fields are a line or more off.
  Neither carries a `decl_line` (struct members and interface methods have none in gc's DWARF),
  and the round trip's code level passes.
- The essential `bin/gate` (the bootstrap compiles `arbace/g2a`): passed in 6 min 46 s (Clojure's
  suite on stage 2 with no regressions, the class forms tests, the Go executable's smoke test).

### The printer's expression lists: a case list over two lines (2026-10-12)

Found on the TLS branch (`box-tls`, `7561fb6`): its guest links `net/url` (through
`crypto/x509`), and `bin/tamago-vendor check amd64` reported the guest from the forms different
from the originals': in `validUserinfo` (`net/url/url.go:1295-1296`) the `case` list spans two
lines, the forms record the break (`(case ^{:go/breaks [9]} [\- ... \' \( ... \%] ...)`), and
the printer wrote the list on one line, which moved one `.gopclntab` line (the function is
inlined into `parseAuthority`).

- **The change** (`arbace/g2a/print_code.ae`, `print_emit.ae`; the forms are unchanged): case
  values and types, a return's results, `:=` targets and range variables follow `:go/breaks`
  and their own lines, as calls and composite literals did (`comma-list`); a case list may
  begin on the line after `case` (`go/parser`'s `case\n\ttoken.DEFINE, ...`). A break is now
  resolved as the converter counts it, the line after the last one a form began on, not the
  line after the cursor: otherwise `}, nil` (a composite literal's `}` the layout puts on its
  own line, then a result the forms record as a break) got `nil` a line further. A call's `)`
  after a function literal follows its `}`. PRINTER-NOTES.md, Layout.
- **Checks** (`g2a-cases`, from main `cc76fbf`): `bin/g2a-print-tests` 5 tests, 268
  assertions (golines also compares case values, return results and assignment sides; the case
  `s10_breaks`), all pass; `bin/g2a roundtrip amd64` and `linux/amd64` no regressions;
  `bin/box-vendor roundtrip` 20 of 20; `bin/tamago-vendor verify` identical, and after
  `bin/arbace-go --build --box` for amd64 and arm64 `check`: both guests identical. On the TLS
  branch with this merged (a scratch branch, not kept): `bin/tamago-vendor check amd64`, the
  guest identical, as linked and with the symbol table cut. golines over std printed for
  tamago/amd64: 1,622 of 1,742 files with the original's lines throughout, with golines as it
  was and with the new items alike (the same 120 files under both: no list item differs). The
  essential `bin/gate` passed (5 min 17 s).

## TLS (option 2): Go's crypto/tls in jrt (2026-10-12)

Branch `box-tls`, the user's decision of 2026-10-12 ("do option 2 now"): TLS for Arbace through
Go's `crypto/tls`, already in TamaGo's std (`tamago/goroot/go/crypto/tls`), rather than
converting jdk26u's JSSE (S5's decision "TLS later, with JSSE" set aside). The motivating use: the
Claude API (`https://api.anthropic.com`, port 443) called from a REPL in the box, the key and the
encryption inside the guest, the launcher carrying only cipher text over its stream proxy
(`ARBACE_BOX_NET=dial=api.anthropic.com:443`).

One correction to the premise: `crypto/tls` was in `tamago/`'s forms but **not linked** into the
guest (nor into the native executable): no symbol or string of `crypto/tls` or `crypto/x509` in
either (`target/arbace-go/box-amd64/arbace.elf`, `amd64/arbace` at `a6a6b86`). It is linked now,
which is most of the size below.

### The design

**Where TLS runs.** In jrt, over the same Go-level stream the java.net sockets use: the socket
table's `net.Conn` (`net.ae`), which in the box is `netbox_tamago.ae`'s `boxConn` over the
monitor's handle and natively `OSHost`'s TCP connection. A client handshake replaces the table's
connection by `tls.Client(conn, cfg)` and keeps the host's under it (`netSock.raw`): reads and
writes (`HostNet.read0`, `write0`) then carry plain text through the TLS connection, while socket
options, addresses and `available` ask the host's. Nothing new crosses the monitor: the launcher
proxies the cipher text as any stream (its policy, its name resolution); the keys, the session and
the plain text never leave the guest. Wake on events is kept as it is: `tls.Conn` reads and writes
`boxConn`, whose waits are the box's waiters (BX53), so a TLS read parks as a plain one does.

**The Java side (jrt's own Java, overlay/jdk).** `jdk.internal.jrt.TlsSocket extends
java.net.Socket` over a `HostSocketImpl` of its own (Socket's protected constructor): it is
connected as any socket (`connect`, the box's policy and the host's resolver), then
`startTls(serverName, alpn, roots, timeout, error)` makes the handshake in place
(`HostSocketImpl.startTls`, holding the read and write locks, then `HostNet.tls0`); its
`getInputStream`/`getOutputStream` are Socket's own, so everything that takes a Socket
(`arbace.java.io/reader`, `with-open`) takes it. `session()` (protocol, cipher suite, ALPN, server
name: `HostNet.tlsInfo0`) and `peerCertificates()` (DER: `tlsPeer0`). New error kinds of `HostNet`:
`TLS_VERIFY` -11 (the peer's certificate: `tls.CertificateVerificationError`), `TLS` -12 (any other
handshake failure: an alert, no common version or suite, the peer's end), `TLS_ROOTS` -13 (roots
given without a certificate); the network's errors during the handshake keep their kinds
(timeouts, resets, refusals). Under TLS `available()` is 0 (the host's count is of cipher text;
Go's `tls.Conn` does not tell what plain text it holds; 0 is always a correct answer),
`shutdownOutput` sends `close_notify` before the host's half-close, `close` sends `close_notify`
then closes.

**The Arbace surface.** Two namespaces, one source each for the JVM, natively and the box, as
`arbace.box` (BX37):

- `arbace.tls` (`arbace/tls.ae`): `(connect host port opts)` a connected `java.net.Socket`, the
  handshake made; opts `:server-name` (default the host; sent as SNI and verified; an IP literal is
  verified against the certificate's addresses and not sent, on both builds), `:alpn` (a sequence
  of protocols), `:roots` (PEM, a string or bytes), `:timeout` (ms, the connection and the
  handshake each); `session` (`:protocol "TLSv1.3"`, `:cipher-suite`, `:application-protocol`,
  `:server-name`, the JVM's names on both builds), `peer-certificates` (DER byte arrays).
  Verification is always on; there is no option to turn it off. Failures: java.net's exceptions for
  the network, an `ex-info` whose `:arbace.tls/error` is `:verify`, `:handshake` or `:roots` for
  TLS. Its host layer `arbace.tls.host`: the Go build's variant (`arbace/lang/go/ns/tls/host.ae`)
  over `TlsSocket`, the JVM's (`arbace/tls/host.ae`) over `javax.net.ssl` (an `SSLSocket` layered
  over a connected Socket, `SNIHostName`, endpoint identification `HTTPS`, ALPN by
  `SSLParameters`, a `TrustManagerFactory` over a `KeyStore` of the given roots).
- `arbace.http` (`arbace/http.ae`): `(request {:url :method :headers :body :as :timeout :tls})`,
  HTTP/1.1 written in Arbace, one request per connection (`Connection: close`), over
  `arbace.tls` for `https:` (ALPN `http/1.1`) and a plain Socket for `http:`; the response's body by
  `Content-Length`, chunked (trailers read and left out), or to the connection's end; none for HEAD,
  1xx, 204, 304; interim 1xx skipped; `{:status :reason :headers :body}` with lower-case header
  names. Enough to POST JSON to `https://api.anthropic.com/v1/messages`.

**Roots.** The guest has no files, so the Go build carries Mozilla's roots: the package `bundle` of
`golang.org/x/crypto/x509roots/fallback` (Go's extraction of NSS's store: 120 roots as DER with
their distrust-after constraints), vendored as Go forms under a new forms root `x509roots/`
(`x509roots/README.md`, `bin/x509roots-vendor`), import path `arbace/x509roots/fallback/bundle`.
jrt parses them once at the first handshake (`jrt.TLSRoots`) into a `CertPool` given to each
connection (`AddCertWithConstraint` for the constrained ones); the module's package `fallback`
(`x509.SetFallbackRoots` in an `init`) is not taken. The same roots natively (not the host's
`/etc/ssl`: the executable behaves as the box does, and is self-contained); the JVM keeps its own
(`cacerts`). The override, for private CAs and tests: `ARBACE_TLS_ROOTS` (read once per process by
`arbace.tls`; the launcher passes `ARBACE_*` to the guest) names a PEM bundle, `ref:NAME` in the
store (in the box the launcher's `--store`) or a file (natively the host's; in the box its in-memory
file system, e.g. from `ARBACE_BOX_FS`); `:roots` per connection. Either replaces the default roots
(not adds to them), so that a test's CA alone is trusted.

**Not offered: `javax.net.ssl` in the Go build.** An `SSLSocketFactory`/`SSLSocket` backed by
`crypto/tls` would let translated Java (j2a) use TLS unchanged, but `javax.net.ssl` is not in the
closed world: `SSLSocket` and `SSLSocketFactory` are abstract classes whose API names
`SSLSession`, `SSLParameters`, `SNIServerName`, `HandshakeCompletedListener`,
`java.security.cert.Certificate`/`X509Certificate` (and through them `java.security.cert`'s
parsing), `SSLContext` and its SPI with the providers' machinery; a minimal subset (the factory,
a socket with `startHandshake`, a session with protocol, suite and DER certificates) is perhaps
1,000-1,500 lines of variants and stand-ins and a few hundred classes more in the program, for no
present user (Arbace code has `arbace.tls`). Recommended: not now; revisit when a Java library
translated by j2a needs it (`TlsSocket` is the place it would sit on).

### What was built

- `go/arbace/jrt/tls.ae` (new, jrt's file list): `HostNet`'s natives `tls0`, `tlsInfo0`,
  `tlsPeer0`, `TLSRoots`; `net.ae`: `netSock.raw`, the table's connection read under its lock
  (`conn`, `rawOf`), `available` 0 under TLS, `shutdown` with `close_notify`, options and addresses
  from the host's connection.
- `overlay/jdk/java.base/jdk/internal/jrt/TlsSocket.java` (new); `HostNet.java` (the kinds, the
  three natives), `HostSocketImpl.java` (`startTls`, `tlsInfo`, `tlsPeer`).
- `arbace/tls.ae`, `arbace/tls/host.ae`, `arbace/lang/go/ns/tls/host.ae`, `arbace/http.ae` (new;
  not AOT-compiled by the bootstrap, loaded from source as `arbace.box` is; embedded in the Go
  build).
- `x509roots/` (new forms root) and `bin/x509roots-vendor` (`convert`, `verify`, `check`); the
  builds give it as a root (`bin/g2a build --root x509roots`): `bin/arbace-go` (the executables,
  the box's guest; `gate_key` hashes it), `bin/jrt`, `bin/a2g-race`, `bin/a2g-evalproof`,
  `test/a2g/check.ae`; `bin/gate --full`'s `box` step runs `x509roots-vendor verify` and `check`.
  `LICENSE.md` records it (Go's BSD 3-Clause; the certificates are NSS's, as the module ships them).
- `tools/tlsserver` (new, Go): the checks' TLS server on the loopback, a test CA and a wrong CA made
  at each start (no key kept in the repository), HTTPS (`/hello`, `/echo`, `/big`, `/chunked`,
  `/eof`, `/json` 401) and a raw TLS echo; `bin/lib/tls-checks.bash` (new): `tls_server`,
  `tls_checks`, shared by `bin/box-smoke` (a section "TLS" on 4 vCPUs, `ARBACE_BOX_NET=loopback`,
  the CA by `ARBACE_TLS_ROOTS=ref:test-ca.pem` from the launcher's store, plus a wake-on-events
  check: a TLS read waiting 2 s polls nothing), `bin/arbace-go --smoke` (natively, and on the JVM
  with `target/arbace.jar`) and `bin/box-arm64-vm smoke` (the server cross-built for arm64:
  `--tls-server`).

### Measured (amd64, nested KVM, the shared machine)

| | before (`a6a6b86`) | with TLS | change |
|---|---:|---:|---:|
| the native executable (`amd64/arbace`) | 66,814,112 | 69,169,312 | +2,355,200 (3.5%) |
| the box's guest (`arbace.elf`, symbols cut) | 66,199,886 | 68,604,238 | +2,404,352 (3.6%) |
| the launcher (`arbace-box.bin`) | 4,713,159 | 4,713,023 | none (main's merge) |

The growth is `crypto/tls`, `crypto/x509` and what they pull (`net/url`, the ECDH, RSA, AES-GCM,
ChaCha20 code, the HPKE/ECH code), not linked before, and the roots (128 KB of DER, parsed at the
first handshake, not at the start). A handshake to `api.anthropic.com` (TLS 1.3, ECDHE, from
here): natively 79 ms the first (the roots parsed), then 10-11 ms; in the box 119 ms, then 61-64
ms (the name resolved by the host, the dial through the monitor, the handshake's crypto in the
guest under nested paging). Idle (wake on events): a box waiting 20 s on a TLS read costs the host
what a box sleeping 20 s does: 1 vCPU 0.63 s of CPU against 0.69 (the start's), 4 vCPUs 0.92 s
against 1.58; 8 `net_read` calls in the whole run (1 vCPU). `bin/box-smoke`'s check: a read
waiting 2 s makes at most a few network calls.

### The real request (no key)

`ARBACE_BOX_NET=dial=api.anthropic.com:443 arbace-box -` with `arbace.http/request` POSTing a
messages request to `https://api.anthropic.com/v1/messages` without any API key: `401
"application/json"`, body `{"type":"error","error":{"type":"authentication_error","message":"x-api-key
header is required"},...}`; `arbace.tls/session`: TLSv1.3, `TLS_AES_128_GCM_SHA256`, ALPN `h2`
when offered (`http/1.1` by `arbace.http`), 3 certificates, verified by the built-in Mozilla roots.
The same natively. A name the policy does not dial (`example.org`) is `UnknownHostException`.

### Checks (the branch with main `ff53e17` merged, amd64)

- `bin/jrt build` (jrt with `tls.ae`); `bin/jrt-convert update` (jrt's three Java files: `jdk/`
  committed).
- `bin/arbace-go --build` and `--build --box` (sizes above); `--smoke` 83 of 83 (69 before; 7
  TLS checks natively and 7 on the JVM, `target/arbace.jar`); `--smoke --box` 111 of 111 (103
  before; 8 TLS checks).
- The native oracle 22,232 of 22,278 and the box's subset (`ARBACE_BOX_NET=loopback`, with
  `forms/net.ae`) 21,779 of 21,825: the 46 known mismatches each, as recorded.
- `bin/x509roots-vendor verify` (byte for byte) and `check` (gocmp at three levels for
  linux/amd64, linux/arm64, tamago/amd64, tamago/arm64; the package's tests on the printed copy).
- `bin/tamago-vendor check amd64`: std and the goos module pass as before, but **the guest image
  from the forms differs from the one from TamaGo's originals** (same size, 4 MB of `.gopclntab`):
  the guest now links `net/url` (through `crypto/x509`), whose `validUserinfo` has a `case` list
  over two lines (`net/url/url.go:1295-1296`); its forms record the break (`:go/breaks [9]` on the
  case's vector) but the printer writes the list on one line, so the inlined code's line in
  `parseAuthority` is 1295 where gc says 1296. Proven the only difference: with that one line split
  by hand in the printed tree, the two images are identical (as linked and cut). A printer defect
  (g2a's `print`, the kind the `g2a-params` branch fixes for parameter lists), not of these forms or
  of this change; `bin/gate --full`'s `box-forms` step will fail on it until the printer keeps a
  case list's breaks.
- The real request above; the loopback checks in `.tmp` before they went into the smoke tests.
- arm64: `bin/arbace-go --build` and `--build --box` (the guest 66,191,694 bytes, the launcher
  with it 70,713,694); the native arm64 executable under qemu-aarch64 made the real request above
  (401, TLSv1.3, the roots verified). Not run: `bin/box-arm64-vm smoke` (the box's TLS checks on
  KVM/arm64; about 40 min emulated), which now ships `tools/tlsserver` built for arm64.
- The essential `bin/gate`: passed (6m19s): stage 2 = stage 3 (5,777 classes), the native tests 60 of 60, Clojure's suite on stage 2 20,750 of 20,750, `bin/class-forms-tests`, the Go executable built again and its smoke test 83 of 83.

### Amendments (proposed 2026-10-12)

- **BX70 (A2G-SPEC §9.1, §9.4; jrt, overlay/jdk) TLS in jrt over the socket table**: Go's
  `crypto/tls` as a client over the table's connection (`netSock.raw` under the `tls.Conn`), the
  natives `HostNet.tls0`, `tlsInfo0`, `tlsPeer0`, the kinds `TLS_VERIFY`, `TLS`, `TLS_ROOTS`;
  jrt's own `TlsSocket` (a `java.net.Socket` upgraded in place); under TLS `available()` 0,
  `shutdownOutput` with `close_notify`. In the box the launcher carries cipher text only; no new
  call, no change to `box/`.
- **BX71 (EVAL-NOTES' namespace variants) `arbace.tls` and `arbace.http`**: one source each for
  the JVM, the native executable and the box; `arbace.tls.host` over `TlsSocket` (Go build) and
  `javax.net.ssl` (JVM); `connect`, `session`, `peer-certificates`; verification always on; the
  errors `:verify`, `:handshake`, `:roots`; `arbace.http/request`, HTTP/1.1 in Arbace.
- **BX72 (jrt, `arbace.tls`) The roots**: Mozilla's built into the Go build (natively too, rather
  than the host's `/etc/ssl`), the JVM's own on the JVM; `ARBACE_TLS_ROOTS` (`ref:NAME` or a file)
  and `:roots` replace them.
- **BX73 (LICENSE.md; a new forms root) `x509roots/`**: `golang.org/x/crypto/x509roots/fallback`'s
  package `bundle` at `3b414e3e8284` vendored as Go forms (import path
  `arbace/x509roots/fallback/bundle`), `bin/x509roots-vendor`, given to the builds as
  `--root x509roots`; in `bin/gate --full`'s `box` step.
- **BX74 (scope) No `javax.net.ssl` in the Go build for now**: recommended deferral (above).
- **BX75 (checks)** `tools/tlsserver`, `bin/lib/tls-checks.bash`: TLS's checks in `bin/box-smoke`
  (also in the arm64 VM), `bin/arbace-go --smoke` (natively and on the JVM).

### TLS: the user's decisions (2026-10-12)

BX70-BX73 accepted as built; BX74 accepted (no `javax.net.ssl` in the Go build for now); BX75
accepted. The roots' licence: LICENSE.md records `bundle.der`'s certificates under NSS's
Mozilla Public License 2.0 as well as x/crypto's BSD 3-Clause (rather than BSD-3 only). The
branch merged after the printer's case-list fix (`g2a-cases`), so `box-forms` stays identical.

## After standalone S4: the JVM's host layers gone (2026-10-12)

With the JVM Arbace off main (doc/STANDALONE-NOTES.md, "S4: the JVM removed"), BX37's and BX71's
JVM halves are on the frozen branch only: `arbace/box/host.ae` and `arbace/tls/host.ae` are the
Go build's host layers (jrt's `HostStore` and `TlsSocket`), moved from `arbace/lang/go/ns/` with
`git mv`; the JVM's (java.nio, `javax.net.ssl`) left. `arbace.box`, `arbace.tls` and
`arbace.http` are one source for the native executable and the box. `bin/arbace-go --smoke` no
longer runs the store's and TLS's checks on the JVM (`jvm_store_checks`, the JVM's
`tls_checks`), nor `test/native/box_test.ae` (`bin/native-tests` left too); the native and boxed
checks are unchanged. The default TLS roots are Mozilla's everywhere (BX72's "the JVM's own on the
JVM" no longer applies).

## HV1 (b): RAM above 4 GiB (2026-10-13)

Branch `hv1-ram`, from main `f7926f1`. HOST-VM-SURVEY §14 HV1, decided (b) by the user after
its measurement (a2g under a 2 GiB collector limit 3.0× slower, 6.4× on 4 threads): give a Go
guest RAM above 4 GiB with the doorbell where it is (`0xf0000000`, under 4 GiB), so that a tool
guest runs a2g under `GOMEMLIMIT=12GiB` rather than in the 3 GiB slot. The experiment first, then
the design it led to, built and checked on amd64 (nested KVM here) and on KVM/arm64 (the
emulated aarch64 VM); the Mac's side prepared.

### What the slot was

One span from the image's base (2 MiB) to `GoRAMBytes` (3 GiB) above it, identity-mapped, under
the doorbell; the low 2 MiB hold the system tables (`0x1000`), the command line and environment
(`0x4000`) and the page tables (`0x10000`). The monitor `mmap`s the slot `MAP_NORESERVE` and maps
it into the VM as one range from guest-physical 0 (`hv.VMMap(mem, 0, ...)`), and passes the
runtime its RAM in registers (amd64 RDX, RCX, R8: `RamStart`, `RamSize`, the first stack's offset
under the top; arm64 X2-X4). TamaGo's runtime (`tamago/goroot`, go1.27.1) manages one contiguous
range: its heap is a break (`sbrk`, `mem_tamago.go`) from the image's end (`initBloc`) up to the
first stack, under `RamStart + RamSize`, unless `goos.Bloc` names another start (`os_tamago.go`,
`osinit`); its heap addresses have 40 bits (`goos.HeapAddrBits`, TamaGo's goos module), so 1 TiB.

### The experiment: the image linked above 4 GiB (`-T 0x100201000`)

The first try was HV1 (b) as the survey wrote it: the guest linked at `0x100201000` (its RAM from
`0x100200000`), the monitor mapping a low range (`[0, 2 MiB)`: the tables, the command line) and
the high one, the doorbell in the hole between them.

- **The links work**: TamaGo's linker takes `-T 0x100201000 -R 0x1000` for both architectures
  (the amd64 guest's text at `0x100200000`, data at `0x104211000`; arm64's likewise), no
  relocation out of range.
- **The amd64 guest dies at its fourth instruction of the runtime**: "the guest took exception 14
  (code 0x2) at pc 0x10027e922, address 0x4367150": `rt0_amd64_tamago`'s first store through TLS
  (`MOVQ $0x123, g(BX)`, `runtime/sys_tamago_amd64.s:52`), at `m0.tls` (`0x104367150`) cut to
  32 bits. TamaGo's `runtime·settls` at ring 0 writes `IA32_FS_BASE` with `MOVQ DI, AX; MOVQ
  $0x0, DX; WRMSR` (`/root/tamago-go/src/runtime/sys_tamago_amd64.s`, lines 152-163): the high
  half is always zero, so a TLS block above 4 GiB cannot be addressed. (The board's own
  `whim_apentry` sets the high half for APs; the boot vCPU goes through `settls`.)
- So the image above 4 GiB needs a change to TamaGo's runtime: in `tamago/` (which breaks
  `bin/tamago-vendor check`'s byte identity with TamaGo's tree), or as one more file in the
  runtime's overlay (`overlay/go/`, the `-overlay` that jrt's runtime patches already use: a
  copy of `sys_tamago_amd64.s` with `MOVQ DI, DX; SHRQ $32, DX`). And it moves every address of
  the image: the symbols the monitor reads, the build's `-T` in three places (`bin/arbace-go`,
  `arbace.g2a.build/tamago-ldflags`, `bin/tamago-vendor`), every existing image unbootable on the
  new layout and the other way round.

### The design: the image where it is, the RAM above 4 GiB (BX80)

The runtime does not need its image inside its RAM: only its heap and first stack must be there,
and the heap's start is a hook the TamaGo port already has (`goos.Bloc`, "to redefine the heap
memory start address"). So the image stays linked at `0x201000`, and RAM that does not fit under
the doorbell goes above it:

| guest-physical | what | mapped |
|---|---|---|
| `0` - `0x1000` | nothing (a nil dereference faults) | low range |
| `0x1000` - `0x200000` | system tables, command line and environment, page tables | low range |
| `0x200000` - the image's end (2 MiB up) | the image, as today | low range |
| up to `0xf0000000`, `0xf0001000` - 4 GiB | the hole: no memory | no |
| `0xf0000000` | the doorbell (no memory: a store exits) | no |
| `0x100000000` - `+ RAM` | the runtime's RAM: heap from its start, first stack and the vectors' stacks at its top | high range |

- **vmm** (`layout.ae`, `tamago.ae`, `vmm.ae`, `smp.ae`): `Config.RAM` (bytes; 0 is
  `GoRAMBytes`, 3 GiB). `planGo` keeps the slot as it was when the RAM fits under the doorbell
  (image base + RAM ≤ `0xf0000000`: one range, the same layout, registers and tables as before);
  else `RamStart` is `highBase` (4 GiB), `RamSize` the RAM, and the layout records two RAM ranges
  (`layout.ram`: `[0, roundUp(image end, 2 MiB))` and `[4 GiB, 4 GiB + RAM)`), each mapped by its
  own `hv.VMMap` (KVM: two memory slots; the framework: two `hv_vm_map`s, both 2 MiB-aligned).
  The host side stays one `mmap` from 0 to the top (`MAP_NORESERVE`: the hole costs address
  space, nothing else). Every check of a guest address against the slot's size (a call block,
  `machine.guest`, `CPUStart`'s release) asks `layout.inRAM` instead, so the hole is outside the
  guest's memory for the calls too. `boot` refuses RAM that would reach the doorbell from below
  (as before) and RAM ending beyond `maxGuestEnd`: amd64 1 TiB (the runtime's 40-bit heap
  addresses), arm64 64 GiB (below).
- **The board** (`board.ae`): `Hwinit0`, empty until now, sets `goos.Bloc = RamStart` when
  `RamStart` is past the image (above the board's own variables, in its bss): the runtime's
  break then starts at 4 GiB. `osinit` reads it after. Nothing else changes: the entry registers, the stacks (at the
  top of `RamStart + RamSize`, as before), the TLS (in the image's bss, under 4 GiB), the
  doorbell's store, the vectors.
- **The launcher**: `ARBACE_BOX_RAM=SIZE` (`storeSize`'s units: bytes, KiB ... TiB; `off` or
  unset: 3 GiB), not passed to the guest. jrt is unchanged: its default collector limit is
  `RamSize` less 1 GiB (BX13), so `ARBACE_BOX_RAM=16GiB` gives `GOMEMLIMIT` 15 GiB, and
  `ARBACE_BOX_RAM=14GiB GOMEMLIMIT=12GiB` is a2g's run as the survey measured it natively.
- **arm64** (`setup_arm64.ae`): the guest's translation was a 32-bit space (TCR_EL1 T0SZ 32);
  now 36 bits (T0SZ 28, still 3 levels from level 1, IPS 36 bits as before), so the guest
  reaches 64 GiB of addresses: RAM up to 60 GiB above 4 GiB.
- **Not changed**: the doorbell ABI and its numbers (`abi`), the entry state's registers,
  go-whim's guests (`kvm-tests`' `TestGoGuest`, `TestGoGuestSMP` ... pass: they ask no RAM, so
  they get the old layout), `tamago/` and the build's link address, the default 3 GiB slot.
  An old board given RAM above the doorbell would put its heap in the hole and fault at its
  first allocation; the launcher carries its own guest, so the two always match.

### Measured (amd64, nested KVM on the shared machine, 4 vCPUs, the balloon then deflated)

All in the box built by `ARBACE_BOX_ARCHES=amd64,arm64 bin/arbace-go --build --box` on this
branch, `target/arbace-go/box-amd64/arbace-box`, against the native executable of the same build.

| run | box | native | notes |
|---|---|---|---|
| start, `-e nil` | 0.60 s, 248 MB RSS (3 GiB); 0.61 s, 248 MB (`ARBACE_BOX_RAM=16GiB`) | | the slot's size costs nothing until touched |
| 10 × 1 GiB `byte-array`s, each filled (10 GiB touched), 16 GiB | 24.3 s, 10.76 GB max RSS | 24.8 s, 10.69 GB | `maxMemory` 15,360 MiB in the box |
| the same in the default 3 GiB slot | "out of memory: cannot allocate 1073741824-byte block (2352480256 in use)" after 3.7 s | | |
| 25 M `[i (str i)]` in a vector, 9.3 GiB live after `System/gc`, 16 GiB | 148.6 s, 13.9 GB | 123.6 s (`GOMEMLIMIT=15GiB`), 14.3 GB | 1.2×: 4 vCPUs against the host's threads |
| 40 M of them, 14.0 GiB live (at the 15 GiB limit), 16 GiB | 243.5 s, 15.1 GB | | the collector at its limit, and it holds |

Boundaries: `ARBACE_BOX_RAM=60GiB` boots (`maxMemory` + 1 GiB = 61,440 MiB; nothing touched);
`1100GiB` is refused ("the guest's RAM ends at 0x11400000000, beyond this monitor's
0x10000000000"); `3700MiB` still fits under the doorbell (one range, `maxMemory` 2,805,989,376);
`ARBACE_BOX_RAM=1x` is refused by the launcher. `a2g --program` in the box was not run (not
required); the probes above hold a larger live heap than a2g's (1.1-1.6 GB live, 10 GB peak).

### arm64: KVM/arm64 and Hypervisor.framework

- **KVM/arm64** (the aarch64 VM, `bin/box-arm64-vm`, QEMU `-cpu max`): the VM's kernel reports
  "kvm [1]: IPA Size Limit: 52 bits"; the monitor creates its VM with type 0, which is KVM's
  default 40-bit IPA (1 TiB; `KVM_CAP_ARM_VM_IPA_SIZE` and the type's IPA field are there if
  more is ever needed: `hv.VMConfig.IPASize`, `vmType`). The arm64 box of this branch (T0SZ 28)
  runs there: `-e` with the default slot (`maxMemory` 2,147,483,648) in 18 s; with
  `ARBACE_BOX_RAM=8GiB` a 1 GiB array allocated and written, `maxMemory` 7,168 MiB, in 25 s; nil
  dereferences and futures as before; and the box's smoke test (below).
- **Hypervisor.framework**: its default IPA size is 36 bits (64 GiB) — the reason for 36 bits
  here, with no `hv_vm_config_set_ipa_size` call: RAM up to 60 GiB above 4 GiB fits the default.
  `hv_vm_config_get_max_ipa_size` exists since macOS 13; the maximum is 40 bits from macOS 15 on
  where the hardware supports it (39 before) (QEMU's hvf commits: "hvf: arm: Implement and use
  hvf_get_physical_address_range", qemu `d54ffa54`; the patch "hvf: support changing IPA granule
  size", 2025-12-19). Not run: the Mac is the user's (below).

### What the Mac run needs (prepared)

`box/mac/mac.sh`'s checklist gains a section "RAM above 4 GiB": `ARBACE_BOX_RAM=6GiB` (a 1 GiB
array, the collector's limit), `60GiB` (the most under the framework's default IPA: boots,
`maxMemory` + 1 GiB = 61,440 MiB, nothing touched), `61GiB` (refused, beyond 36 bits), and when
`hw.memsize` is 24 GiB or more, 16 GiB with 10 GiB allocated and touched (its time and the
Mac's memory in `measures.txt`). `bin/box-mac-bundle` packs it as before; the user's run of the
new bundle answers whether `hv_vm_map` of a 60 GiB range (unwired, untouched) is accepted and how
fast the framework faults 10 GiB in.

### The forms' layout

A form's position in its `.ae` file is its Go line (the converter's layout), and its explicit
layout metadata (`:line`, `:go/end`, the lines in `:go/breaks`) are Go lines too. Edited by hand,
the changed files first printed (`bin/g2a build --layout lines`, as `bin/box-vendor roundtrip`
prints) with `//line` directives where a form came later than its line, and the first round trip
failed on one: a directive in a one-line function's body (`board.go`'s `sigpanic`, after the
package comment grew), which the converter takes back as a statement in an expression's place.
So the edits keep the layout: the explicit metadata after an inserted or removed `.ae` line moved
with it, blank lines put before a new form for its doc comment, and the board's change made
without growing the package comment or its imports. The changed files now print with exactly
the directives main's do (4 in `board.go`, 2 in `smp.go`, 1 in `net_unix.go`, 1 in
`seccomp_net_linux_amd64.go`, all go-whim's own layout), gofmt'd. (The one-off script that did it,
`.tmp/hv1/tools/relayout.py` in the worktree, is not committed.) A rule for future hand edits:
reserve a form's Go lines (its doc comment's) as blank `.ae` lines before it, and move the
explicit metadata after any line inserted.

### Checks (the branch, amd64 here; arm64 in the VM)

- `ARBACE_BOX_ARCHES=amd64,arm64 bin/arbace-go --build --box`: built (the amd64 guest 68,419,918
  bytes, as before but for the board).
- `bin/arbace-go --smoke --box` (amd64, the final build): 113 ok, none failed, with the new check
  "ARBACE_BOX_RAM=6GiB: RAM above 4 GiB (HV1 (b)) ..." on 1 and 4 vCPUs.
- `bin/box-arm64-vm smoke` (KVM/arm64 in the VM, the same smoke test, the arm64 build before the
  board's last edit, which only changed how `Hwinit0` tells the RAM is past the image): the RAM
  check ok on 4 and 1 vCPUs, and every check ok (112 ok, 0
  failed).
- `bin/box-vendor kvm-tests`: 27 passed, none failed (go-whim's tests on `box/` as it is: hv, vmm
  with the new `TestPlanGoHigh`, the C and TamaGo guests, go-whim's own guests on the old layout).
- `bin/box-vendor roundtrip`: every package in every configuration passes (tree, export, code).
- `bin/tamago-vendor check`: both guest images identical to the originals' (amd64 and arm64):
  `tamago/` is untouched, so its identity holds.
- `bin/gate` (essential) on the branch with main `40154ca` merged: passed (14m48s; the suite with no
  regressions, the oracle 54 known mismatches, 0 new).

### Amendments (proposed 2026-10-13)

- **BX80 (box/: vmm, board, launcher) Where a Go guest's RAM beyond the doorbell goes.** Options:
  - (a) *the image where it is linked, the RAM above 4 GiB* (built here): two RAM ranges, the
    doorbell between them; the board starts the heap there by `goos.Bloc`; `tamago/`, the link,
    the ABI and go-whim's guests unchanged;
  - (b) the image linked at `0x100201000` with its RAM: needs TamaGo's `settls` fixed for a TLS
    above 4 GiB (an overlay of `sys_tamago_amd64.s`, or `tamago/` changed and its identity with
    TamaGo's tree given up), the build's `-T` changed everywhere, old and new images incompatible;
  - (c) the doorbell moved above RAM (STANDALONE-SURVEY D8 (b)): an ABI change shared with
    go-whim's guests and board, for no gain over (a).

  *Recommended: (a)*.
- **BX81 (launcher, later the spawn call) The slot's size.** Options:
  - (a) 3 GiB by default as today, `ARBACE_BOX_RAM` per launch, and per spawn the RAM a spawn asks
    for (HV5's resource asks, within HV4's budget);
  - (b) a larger default (8 or 16 GiB): free until touched, but with no return of memory to the
    host (HV2) a long-lived REPL keeps its peak;
  - (c) a fraction of the host's memory.

  *Recommended: (a)*; a2g's tool guest asks 14 GiB with `GOMEMLIMIT=12GiB`.
- **BX82 (box/: vmm on arm64) The guest's addresses on arm64.** Options:
  - (a) 36 bits (T0SZ 28, built here): RAM up to 60 GiB, the framework's default IPA, no
    configuration;
  - (b) 40 bits with `hv_vm_config_set_ipa_size(40)` and the IPA type on KVM: more RAM, macOS 15
    and hardware that has it.

  *Recommended: (a)*, (b) when a guest needs more than 60 GiB.
- **BX83 (checks)** `TestPlanGoHigh` (vmm), the smoke test's `ARBACE_BOX_RAM=6GiB` check (also in
  the arm64 VM), and the Mac checklist's RAM section (6, 60, 61 GiB; 10 GiB touched at 16 GiB).
- **BX84 (HV2, follow-up) Memory back to the host for large slots.** With slots of 14-16 GiB, a
  guest's peak stays committed until it exits (no `sysUnused` reaches the monitor). Options:
  - (a) nothing for tool guests, which exit (HV2 (a));
  - (b) HV2 (b)'s release call before a long-lived box gets a large slot.

  *Recommended: (a) now, (b) with the first long-lived large box* (as HV2 decided).

**Decided (2026-10-13, the user):** BX80 (a), BX81 (a), BX82 (a), BX84 (a) now and (b) with the
first long-lived large box, all as recommended; BX83's checks stand as built.

## S7: tool guests (2026-10-13)

Branch `s7-tools`, from main `38bcad1`. STANDALONE-SURVEY §9.1 row S7, D9 (a) (gc as tool guests
through a spawn call), D10 (a) (trees: a blob per file and EDN tree maps); HOST-VM-SURVEY HV3 (b),
HV4 (b), HV5 (a), HV8 (a), HV9 (a); BX81 (a) (a spawn asks the RAM it needs). Exit criterion: in
the box, a Go package compiled and linked into a guest image which the launcher then boots from
the store. Go's toolchain is TamaGo's go1.27.1 (`/root/tamago-go`) as it is (S9, the toolchain as
forms, is later).

### The plan (written first)

- **Tool images.** `cmd/compile`, `cmd/link` and `cmd/asm` built for `GOOS=tamago` (amd64,
  arm64) with the board of `box/`, and a shim that gives the tool its command line, its
  environment and its file system: a `-overlay` adds the board, the call ABI, the tree format and
  the shim to the `cmd` module's tree (a package of `cmd` cannot import the module `arbace`), and
  one file to each tool's `main` package that starts the shim. The images go into the store as
  blobs; refs name them (`tool.compile.amd64`, ...).
- **Trees (D10).** A tree is a blob holding an EDN map from a name to `[:file HASH SIZE]`,
  `[:exec HASH SIZE]` or `[:dir HASH]`, printed canonically (names sorted by their bytes, one entry
  a line), so that the same files are the same hash. The launcher's store tool imports a
  directory (or a tar on standard input, `git archive`'s) as a tree and exports one.
- **The spawn call (HV5).** A guest asks the monitor to run a tool guest by a request blob's hash:
  an EDN map naming the image (a hash), the command line, the environment, the input trees and
  where each is mounted (read only, HV3), the output directory, the working directory, and the
  resources asked (RAM, vCPUs). The answer, once the child has exited, is a result blob's hash:
  its exit code, its output tree and its console (a blob). Nothing else crosses: no host paths,
  no network (HV8), no refs. The calls do not wait (as the network's): `Spawn` answers a handle,
  `SpawnPoll` the result when it is there, and an event is posted at each end.
- **Where a child runs.** A tool guest is a launcher process of its own (Hypervisor.framework
  allows one VM a process, and the monitor's system-call filter allows no `fork` or `exec`):
  a helper process, started by the launcher before the filter goes on, runs the children
  (`arbace-box --store DIR --tool REQUEST`) and holds the budget (HV4): the RAM and vCPUs of
  the children running together, the rest queued.
- **The child's policy.** Its store is a view: blobs it puts, and the blobs reachable from its
  input trees (and its image); no refs. Its console is captured (bounded) into the result. No
  network, no spawn of its own (a leaf), no host environment.
- **The child's file system.** The input trees appear as read-only files in TamaGo's in-memory
  file system, each file's contents read from the store at its first read (on demand, HV3 (b) in
  the guest's terms); the output directory is saved as a tree when the tool exits with 0, and
  declared to the monitor (`SpawnOut`).
- **The action cache (HV9).** The build driver keys an action by the hash of its request less the
  resources, and keeps the result's hash under a ref; a rerun after an interruption redoes only
  what has no result.
- **The build driver** in Arbace (`arbace.box.build`): from a package graph (`go list`'s, imported
  as data), the actions `go build` runs (compile, asm, pack, link), each a spawn, the archives
  passed as trees; the image put in the store under a ref, which the launcher boots.
- Order: R3 first (what gc needs, by experiment); then the spawn call with trees in and out; then
  compile, link and the boot by ref on amd64; then arm64 in the emulated VM.

### R3: what gc needs in a guest (measured first)

Experiments in `.tmp/s7/` (not committed), TamaGo's go1.27.1 as it is, guests on /dev/kvm (nested,
the shared host), the launcher of main `38bcad1`:

- **It builds as a guest.** `cmd/compile`, `cmd/link` and `cmd/asm` link for `GOOS=tamago` with the
  board when a `-overlay` adds the board, `abi` and a shim to GOROOT's `cmd` module (a package of
  `cmd` imports only the standard library and `cmd`). The board's assembly needs a directory that
  exists (the go command runs `asm` in the package's directory, which an overlay cannot make): the
  board is the package `cmd/internal`, a directory with no Go files of its own.
- **Its command line and environment.** TamaGo's runtime reads neither (`goenvs` is empty;
  `os.Args` is `["tamago"]`): the shim sets `os.Args` from `board.Args` and the environment from
  `board.Env` in the tool's `init`. `internal/buildcfg` reads `GOOS` and `GOARCH` at its package
  initialisation, before that, and falls back to `runtime.GOOS` and `runtime.GOARCH`: a tool targets
  tamago and its own architecture (BX97).
- **Files.** It uses TamaGo's in-memory file system (`syscall/fs_tamago.go`) through `os` only: no
  `mmap` (`tamago` is not a `unix` GOOS, so `cmd/internal/bio` and the linker's output buffer read
  and write), no temporary files and no `os/exec` (the linker's internal linking; the external
  linker and `-tmpdir` are not used), no network.
- **A bug in TamaGo's file system.** The first link in a guest failed ("could not look up
  runtime.mapinitnoop"): the linker read only the first member of each archive. `fsysFile.seek`
  past the end sets the file's size to the *old* offset plus one ("simulate sparse file"), so a
  seek to an odd-aligned member offset (`nextar`) shrank the archive for every later read. Two
  more: a write past the end adds the offset to the size (`Size += offset`), and `O_TRUNC` leaves
  the size. Patched for the tool guests (BX95): with it, every step below works.
- **Exit.** The shim stores the output when the tool exits with 0 (`goos.Exit` wrapped): the
  runtime's fatal errors exit with 2 on the system stack, where nothing can be allocated (the first
  version, which stored on any exit, crashed there).
- **Byte for byte.** `compile` of a small package in a guest writes the same archive as the host's
  `compile` with the same flags (`-trimpath` mapping the guest's paths). A whole TamaGo guest (a
  hello world on the board: 33 compiles, 48 assemblies, a link; `go build -a -n`'s plan replayed,
  each step a guest, the archives packed on the host) built entirely by tool guests is
  byte-identical to the same plan run by the host's tools, and boots: "hello from a guest compiled
  and linked in the box".
- **Cost.** That build: 82 guests, 15.4 s of tool time against 2.9 s on the host (each guest's
  start about 0.1 s); `runtime`'s compile the longest, 3.0 s and 484 MB of the launcher's RSS
  (the guest's touched memory). The default 3 GiB slot is ample for the standard library's
  packages; Arbace's `jrt` (7-8 GB, STANDALONE-SURVEY §8.2) is S5's.

### What was built

- **Trees** (`box/`'s new package `arbace/box/tree`, standard library only, shared by the launcher
  and the tool guests; BX93). A tree is a blob holding an EDN map, printed canonically (names
  sorted by their bytes, one entry a line, `"` and `\` escaped):

      {"a.go" [:file "HASH" 1234]
       "bin" [:exec "HASH" 87]
       "sub" [:dir "HASH"]}

  `tree.Format`, `Parse`, `PutDir`, `Walk`, `Extract`, `Reachable`; a small reader of the EDN it
  uses (maps, vectors, strings, keywords, integers). The launcher's store tool:
  `arbace-box --store DIR tree put DIR [NAME]`, `tree tar FILE|- [NAME]` (`git archive REV |`),
  `tree get HASH|NAME DIR`, `tree ls HASH|NAME`. The same files are the same tree (checked: a tree
  extracted and stored again is the same hash).
- **The spawn calls** (`abi`, `vmm`; BX90). After the events' call: `Spawn` (54; a[0] the request's
  hash: a handle), `SpawnPoll` (55; handle a[0]: 0 and the result's hash at a[1], or
  `SpawnRunning`, -11), `SpawnOut` (56; a spawned guest declares its output tree);
  `SpawnRefused` (-1) for the rest. None waits; a spawn's end posts an event (the event area, as
  the network's changes do). `vmm.Config` gains `Spawner` (an interface: `Start`, `Poll`) and
  `Output`.
- **The request and the result** (`tree.Request`, `tree.Result`; BX94):

      {:image "HASH" :args ["compile" "-p" "p" ...] :env ["GOARCH=amd64"]
       :trees [["/src" "HASH"] ["/deps/b8" "HASH"]] :out "/out" :cwd "/src"
       :ram 3221225472 :cpus 4}

      {:exit 0 :out "HASH" :console "HASH"}

  `:out` only when the guest exited with 0 and declared one; `:console` both streams in order, a
  blob (at most 16 MiB); `:fault` instead of `:exit` when the monitor ended the guest. The result
  holds nothing of the run's time or place: the same request gives the same result blob.
- **The launcher** (BX91, BX92). `ARBACE_BOX_SPAWN=1` (with a store) lets the guest spawn: before
  the system-call filter goes on, the launcher starts its helper, `arbace-box --spawner DIR`, and
  answers the calls over its standard input and output. The helper reads each request, refuses one
  asking more than the budget (`ARBACE_BOX_SPAWN_RAM`, storeSize's units, by default half the
  host's `MemAvailable`; `ARBACE_BOX_SPAWN_CPUS`, by default the host's CPUs), queues the others
  first come first served, and runs each as `arbace-box --store DIR --tool REQUEST`: a launcher
  whose guest is the request's image (read from the store, its hash checked), its command line
  and environment the request's only (and `ARBACE_TOOL_REQUEST`), its store a view (the blobs it
  puts, and those reachable from its trees and the request; no refs), no network, no spawn of its
  own, its console captured, its RAM and vCPUs the request's (3 GiB and 1 by default), the filter
  on. It writes the result blob and prints its hash. At the end of the parent the helper's input
  ends and it kills the children still running.
- **The tool guests' shim** (`box/`'s new package `arbace/box/tool`, GOOS=tamago). `tool.Start`,
  called from an `init` the overlay adds to each tool's `main`: the command line and environment;
  then, spawned, the request read from the store, each input tree mounted (directories made, each
  file a lazy file of the patched file system: its size known, its contents read from the store at
  the first read or write: HV3 (b) in the guest's terms), the working and output directories made,
  and `goos.Exit` wrapped to store the output directory as a tree and declare it on exit 0.
- **TamaGo's file system for the tool guests** (`overlay/go/syscall.ae`, `syscall/fs_tamago.ae`;
  BX95): the seek, write and truncation fixes above, and `syscall.MkLazy` (marked `//go:linkname`
  so the shim may reference it from outside the standard library). Converted from the patched
  file (`bin/g2a convert --goos tamago syscall` from a GOROOT whose `fs_tamago.go` is the patched
  one), the patch's lines marked "Arbace".
- **The tool images** (`bin/box-tools build [ARCH...]`, `put STORE [ARCH...]`; BX96): box/ printed
  for tamago, the overlay written (the board at `cmd/internal`, `abi`, `tree`, `tool` under
  `cmd/internal/arbacebox`, their imports rewritten; `fs_tamago.go` replaced; one `init` file in
  each tool's `main`), and each tool built with TamaGo's go as a box guest is linked; `put` stores
  them under the refs `tool.compile.amd64` ... Sizes: amd64 `compile` 28,114,072 bytes, `link`
  7,159,758, `asm` 5,407,243; arm64 26,386,992, 6,653,897, 5,147,626. About 70 s for both.
- How the forms were made: the new files written in Go and converted (`bin/g2a convert --tests`,
  every configuration that holds them, identical across them); the four changed files (`abi.go`,
  `vmm.go`, the launcher's `main.go` and `store.go`) edited in a printing of box/ with
  `--layout lines`, converted, and merged three ways into box/'s forms (base: the unedited
  printing converted), which keeps box/'s comments and literal spellings; the only conflicts were
  lines with a comment whose line number moved, resolved by taking the new numbers. The printed
  code of every changed and new file equals the edited Go (comments and literal spellings apart),
  and no `//line` directive appears.

### Measured (amd64, nested KVM, the shared machine)

- A spawn from a guest (a test guest calling `Spawn` and `SpawnPoll` directly) of `compile` on a
  one-file package: 84-170 ms from the call to the result, the launcher's start included; the
  archive the same as the host's `compile`'s.
- Five spawns at once in a 7 GiB budget (3 GiB each by default): one refused at once (it asked
  64 GiB), two running, the others queued; all done in 0.3 s.
- The child's view: a file of its tree readable (46 bytes), its image, another request and
  `RefGet` refused, `Spawn` refused (a leaf); a failing compile: `{:exit 2 :console ...}`, its
  console "/src/p.go:3:23: undefined: x", no `:out`.

### Checks (so far)

- `bin/box-vendor roundtrip`: every package in every configuration passes, `tree` (five
  configurations) and `tool` (tamago/amd64, tamago/arm64) added to its list (`own_pkgs`).
- `bin/box-vendor kvm-tests`: 27 passed, none failed.
- `tree`'s tests (`tree_test.go`: the format, a directory stored, extracted and stored again, the
  request and result formats) pass, on the printed forms.
- `bin/box-smoke` on the new launcher (built from the forms) with main's guest image appended (the
  guest is unchanged but for `abi`'s new constants, which it does not use): 113 ok, none failed.
- Not run: `bin/tamago-vendor check` (`tamago/` is untouched); the essential `bin/gate` (no source
  the bootstrap compiles changed; `overlay/go/syscall` is printed by `bin/box-tools` only, though
  it enters the gate's key).

### Next (S7's rest)

(Since done: "S7's exit criterion (2026-10-13)" below.)


- `arbace.box` gains the spawn (`spawn`, `spawn-wait`, the request and result as maps) and the
  trees (`put-tree`, `tree`, `get-tree`, mounted or extracted), over new natives of jrt's
  `HostStore` in the box (the calls) and natively (the launcher's `--tool` as a process, or a
  refusal); this touches jrt (`go/arbace/jrt`, `overlay/jdk`), S5's lane, and needs the Go build
  and the box rebuilt (heavy).
- The build driver in Arbace (`arbace.box.build`): from a package graph imported as data (`go
  list -deps -json`'s fields, put in the store by the host tool with the source trees), the
  actions `go build` runs (asm `-gensymabis`, compile with `-symabis -asmhdr`, asm, the archive's
  members appended in Arbace, link), each a spawn with its inputs as trees; the action cache
  (HV9: the key the request's hash less `:ram` and `:cpus`, the result under a ref `ac.` and 61
  hexadecimal digits of it, BX98); the image's blob under a ref the launcher boots
  (`ARBACE_BOX_IMAGE=ref:NAME`, to add). The exit criterion: in the box, the hello guest (or the
  box's own test guest) built from its sources and booted by ref.
- arm64: the tool images built (above); their run in the aarch64 VM (`bin/box-arm64-vm`).
- A spawn check in `bin/box-smoke` once `arbace.box` has the calls.

### Amendments (proposed 2026-10-13)

- **BX90 (box/: abi, vmm) The spawn calls.** Options:
  - (a) three calls that do not wait (as the network's): `Spawn` answers a handle, `SpawnPoll` the
    result's hash once the child has ended, `SpawnOut` the child's output tree; an event at each
    end (built);
  - (b) one call that waits until the child ends (its vCPU parked meanwhile);
  - (c) host commands by name (HOST-VM-SURVEY HV5 (c)).

  *Recommended: (a)*: the driver keeps several children running from one guest, and an idle guest
  parks on the event; (b) holds a vCPU for each child; (c) was declined (HV5).
- **BX91 (box/: the launcher) Where a child runs.** Options:
  - (a) a launcher process of its own, run by a helper the launcher starts before its system-call
    filter goes on (built);
  - (b) a second VM in the monitor's process (KVM allows several; Hypervisor.framework one a
    process, so not on the Mac);
  - (c) `execve` allowed by the filter.

  *Recommended: (a)*: the same on Linux and macOS, the filter unchanged, and a child's fault or
  memory ends only its process.
- **BX92 (box/: the launcher) The policy and the budget.** Options:
  - (a) spawning off unless `ARBACE_BOX_SPAWN=1` and a store; a child's store a view (its puts,
    the blobs reachable from its trees and its request), no refs, no network, no spawn (a leaf), its
    environment the request's only, its console captured (16 MiB); the budget
    `ARBACE_BOX_SPAWN_RAM` (by default half the host's available memory) and
    `ARBACE_BOX_SPAWN_CPUS` (by default the host's CPUs), first come first served, a request beyond
    it refused (built);
  - (b) as (a), and a child may spawn through its parent's helper, under the same budget;
  - (c) as (a), and a child reads refs.

  *Recommended: (a) now, (b) when a tool guest is itself a driver*; (c) would let a child's
  result depend on more than its request, which the action cache (HV9) relies on.
- **BX93 (D10) The tree format.** Options:
  - (a) an EDN map from a name to `[:file HASH SIZE]`, `[:exec HASH SIZE]` or `[:dir HASH]`,
    printed canonically (built), and the store tool's `tree put|tar|get|ls`;
  - (b) the same without sizes (a stat then reads the blob's size from the store);
  - (c) git's tree objects (binary, SHA-1 or SHA-256 per repository).

  *Recommended: (a)*: the Clojure reader reads it, the size lets a lazy file answer `stat`
  without a call, and a tree is the same blob wherever it is made.
- **BX94 The request and the result.** Options:
  - (a) both EDN blobs (`tree.Request`, `tree.Result` above), the result holding nothing of the
    run's time or place (built);
  - (b) the result with the run's times and counts;
  - (c) the spawn's arguments in the call block, without a request blob.

  *Recommended: (a)*: the request's hash is the action's key (HV9), and the same request gives
  the same result blob; the times belong in a log or stats, not in what the cache keeps.
- **BX95 (overlay/go/syscall) TamaGo's file system.** Its seek past the end shrinks the file
  (`Size = f.offset + 1`, the old offset), a write past the end adds the offset to the size, and
  `O_TRUNC` keeps the size. Options:
  - (a) patched for the tool guests only (built), with `MkLazy`;
  - (b) (a), and the same patch in Arbace's guest (jrt's files: `RandomAccessFile.seek` past the
    end then a read, a truncated file's size);
  - (c) (a) or (b), and reported to TamaGo (usbarmory/tamago-go).

  *Recommended: (b) and (c)*: the bugs are TamaGo's and touch any guest that seeks; (b) changes
  the Arbace guest's build (bin/arbace-go's overlay), which is S5's and S6's to schedule.
- **BX96 (bin/box-tools) The tool images.** Options:
  - (a) GOROOT's `cmd/compile`, `cmd/link` and `cmd/asm` as they are, the board and the shim added
    by an overlay into the `cmd` module (built); put in the store under `tool.<tool>.<arch>`;
  - (b) a copy of `cmd` as a module of its own, its imports renamed;
  - (c) wait for S9 (the toolchain as forms).

  *Recommended: (a)*: no source of the toolchain changes; S9 replaces the source, not the way.
- **BX97 (box/: tool) A tool's target.** TamaGo's runtime has no environment when `internal/buildcfg`
  initialises, so a tool guest targets tamago and its own architecture whatever `GOARCH` its
  request says. Options:
  - (a) a tool image per target architecture, each run on its own architecture (built);
  - (b) the board hands the runtime its environment before the packages initialise (`runtime.envs`
    set in `Hwinit1`), so that `GOARCH` and `GOOS` in a request choose the target (an amd64 box
    building arm64 objects);
  - (c) the tools built with `GOARCH`'s default changed (a `buildcfg` overlay per target).

  *Recommended: (a) for S7 and S8* (each architecture rebuilds itself), (b) if a cross build is
  wanted later.
- **BX98 (HV9) The action cache.** Options:
  - (a) in the driver: the key the hash of the request without `:ram` and `:cpus`, the result's
    hash under a ref `ac.` plus 61 hexadecimal digits of the key (a ref name has at most 64
    characters), compare-and-set (to build);
  - (b) in the helper: a request whose result the store has is answered without a run;
  - (c) a cache blob (an EDN map) under one ref.

  *Recommended: (a)*: it is the build driver's (HV9 as decided), the refs survive a reboot, and the
  store's tool lists and removes them (R6's collection works on refs).
- **BX99 (checks)** `tree_test.go`; `bin/box-vendor roundtrip` with `tree` and `tool`; the spawn's
  and the trees' checks in `bin/box-smoke` (to add with `arbace.box`'s calls); `bin/box-tools build`
  in `bin/gate --full` beside `bin/arbace-go --build --box`.

**Decided (2026-10-13, the user):** BX90 (a), BX91 (a), BX92 (a) now and (b) when a tool guest
is itself a driver, BX93 (a), BX94 (a), BX96 (a), BX97 (a) ((b) if a cross build is wanted
later), BX98 (a), all as recommended; BX95 (b) and (c): the patch in Arbace's guest too, and an
issue for usbarmory/tamago-go drafted for the user to review before anything is posted; BX99
without `bin/box-tools build` in `bin/gate --full` (the other checks as proposed).

### S7's exit criterion (2026-10-13)

After BX90-BX99 were decided, on `s7-tools` with main `86b744f` (S5) merged.

**The exit criterion is met on amd64 and arm64.** In the box, a Go program was built from its
sources in the store by tool guests: 55 packages, among them `runtime`, `os` and the board. The
launcher then booted the image from the store by its ref.

- **amd64 (nested KVM):**
  - `ARBACE_BOX_SPAWN=1 arbace-box --store S -e '(arbace.box.build/build "plan.hello.amd64"
    {:ref "hello.amd64" :jobs 8})'` ran 108 tool guests in 9.3 s.
  - `runtime`'s compile was the longest, 4.5 s with seven other guests running.
  - The image is 1,308,832 bytes, `cf8857b3...`, the same as the one the same driver built
    natively (7.3 s).
  - `ARBACE_BOX_IMAGE=ref:hello.amd64 arbace-box --store S "booted by ref"` printed "hello from a
    guest compiled and linked in the box: booted by ref".
  - Spawned from the box (`(box/run {:image (box/ref "hello.amd64") :args [...]})`), its exit and
    console came back in the result.
  - A rerun took 1.1 s, every action from the action cache.
- **arm64 (KVM/arm64 in the emulated aarch64 VM, `bin/box-arm64-vm`, 16 vCPUs):**
  - The same build of the arm64 plan, with the arm64 tool images, ran 103 tool guests in
    621 s (every instruction is emulated).
  - The image is 1,242,173 bytes, `5ee5360c...`, and booted by ref in the VM.

### What was added for it

- **jrt** (new files, and the package's file list): `jdk.internal.jrt.HostSpawn` (overlay/jdk, its
  forms in jdk/ by `bin/jrt-convert update`). Its natives are in `go/arbace/jrt/spawn.ae` (the
  optional `SpawnHost`).
  - In the box, the calls (`spawn_tamago.ae`).
  - Natively, OSHost (`spawn_linux.ae`) runs `ARBACE_BOX_LAUNCHER --store ARBACE_BOX_STORE --tool
    REQUEST` as a process, with no budget. The native executable is a host tool for the driver.
- **`arbace.box`:**
  - trees: `tree-text`, `put-tree`, `tree`, `put-file`, `put-dir`, `get-tree`, `tree-get`; the
    printing is the same as `tree.Format`'s, and the same directory is the same hash from the
    launcher's `tree put` and from `put-dir`;
  - spawns: `request-text` (the same printing as `tree.Request.Format`), `spawn`, `spawn-wait`,
    `run`, `result`.
- **`arbace.box.build`, the driver in Arbace:**
  - `import-plan` stores a plan's package directories as trees: each directory's files, and the
    `.h` files of its subdirectories, which an assembly file includes by relative paths. It also
    stores `GOROOT/pkg/include`.
  - `build` builds the program:
    - each package in a future that waits for those it imports;
    - for each, asm `-gensymabis`, then compile (`-symabis -asmhdr`, `-complete` as go build gives
      it, `-std`, `-p main` for the command), then one asm per `.s`;
    - then the archive's members are appended in Arbace (Go's ar format, a name cut to 16 bytes
      as pack cuts it), and the program is linked;
    - every action is a spawn, whose trees are `/s` sources, `/d` archives, `/c` configuration
      and `/i` includes, with its output in `/o`.
  - The action cache (BX98) keeps the result under `ac.` plus 61 hexadecimal digits of the
    request's hash without `:ram` and `:cpus`, written unconditionally: a request has one result.
  - The driver's flags are its own: no build IDs, `-trimpath` mapping its mount points. Its image
    is not `go build`'s byte for byte, but it is the same wherever the driver runs.
- **`tools/goplan`:** `go list -deps -json` printed as the plan (EDN). It refuses cgo, `.syso`
  and embedded files.
- **The launcher:** `ARBACE_BOX_IMAGE=ref:NAME`, or a hash, boots a blob of the store. The blob's
  hash is checked before the monitor maps the store's file.
- **BX95 (b):** the patched `fs_tamago.go` in Arbace's own guest (`bin/lib/box-overlay.bash`:
  `bin/arbace-go --build --box`, both of `bin/tamago-vendor check`'s images, `bin/box-tools`).
- **`bin/box-smoke`'s tool-guest checks (BX99):**
  - a tree from the launcher's tool read by `arbace.box`;
  - no spawn without `ARBACE_BOX_SPAWN`;
  - compile spawned with a tree in and out;
  - a failing compile's exit and console;
  - a spawn beyond the budget refused;
  - an image booted by `ref:NAME`.

  The images come from `--tools DIR`, by default `target/box-tools/ARCH`; the section is skipped
  without them.
- **Found on the way:**
  - The archive's member names must be cut to 16 bytes: `rt0_tamago_amd64.o` broke the archive
    and made the linker choose external linking ("unrecognized file format").
  - Go's `-complete` has exceptions for the standard packages whose declarations the runtime
    supplies (`sync`, `os`, `syscall`, ...).
  - The embedded `arbace.box` wins over `ARBACE_PATH`'s, so iterating on it needs `load-file`.

### Checks (exit criterion)

- `bin/box-smoke` (amd64, the rebuilt box and native executable): 119 ok, none failed. These are
  113 as before and six tool-guest checks.
- `bin/box-vendor roundtrip`: 27 of 27 pass. `kvm-tests`: 27 passed, none failed.
- `bin/jrt build --arch amd64`: build and vet pass.
- `bin/jrt-convert update`: 838 of 838 converted, one form added (`HostSpawn`), one changed (the
  package file).
- `bin/tamago-vendor check`: both architectures' guest images, from the originals and from
  `tamago/`'s forms, are identical (amd64 and arm64, with and without the symbols cut), both now
  with the file system's patch; the goos module's packages pass (69 of 69, 62 of 62).
- The essential `bin/gate` (on this branch with main `86b744f` merged): passed in 13m27s.
  - The suite: no regressions.
  - The oracle: 53 known mismatches, 0 new.
- The arm64 box's smoke test in the VM was not rerun (the build in the VM above exercises the
  calls). `bin/box-tools build` is not in the gate (BX99 as decided).

### For S8

- The driver builds what `go list` gives it. For Arbace's own program it needs:
  - the printed module (a2g's output and box/'s forms printed, `overlay/go`'s runtime files given
    as an overlay: the plan's directories replaced per file);
  - `jrt` and `lang` compiled in a 12 GiB guest (SA101 (b): the request's `:ram`; through the
    host's `.tmp/heavy` here);
  - the version stamped with `-X` (link's flags in the plan).
- The image is not `go build`'s byte for byte. Its build IDs and flags are the driver's own, so S8
  compares the image built in the box with the one the same driver builds natively (as above,
  `cf8857b3...` both ways).

## S8: the rebuild in the box (2026-10-13)

Branch `s8-rebuild`, from main `920564e` (S7 merged). STANDALONE-SURVEY §9.1 row S8: S3's chain
inside the guest, sources from a tree ref, the new guest image to the store, the launcher boots
it by ref; the image built in the box equals the host-built one byte for byte, on amd64
(/dev/kvm), then arm64 (the emulated VM, the Mac's bundle). S6 (Arbace compiled to Go) lands
later and changes the printed program: the whole run is one script, `bin/box-rebuild`, so that
the byte-for-byte run is redone cheaply after it.

### The plan (written first)

- **Every step an action.** The chain of `bin/arbace-go --build --box`, each step a spawned
  guest kept in the action cache (BX98): a2g (`--program`), g2a's printing (tamago/'s standard
  library and goos module with overlay/go's runtime and file system patches; the program with
  box/ and x509roots/), the plan, compile and asm per package, the link, the namespaces
  prepared, the main package compiled and linked again with the image, the symbol table cut.
  The guest that runs the driver only orchestrates: a 6 GiB slot is enough for it.
- **Arbace's guest is a tool guest too.** a2g and g2a need Arbace; the a2g step needs 14 GiB
  (BX81). Rather than the REPL guest itself (`ARBACE_BOX_RAM`), Arbace's own image runs them as
  spawned guests: jrt's box host calls box/'s `tool.Start` (the request's trees mounted, the
  output stored at an exit with 0), so a request names Arbace's image as it names `compile`'s.
  The same makes the new image prepare its own namespaces (`ARBACE_PREPARE` in a spawned run).
- **The plan in Arbace.** S7's plans came from `go list` on the host (tools/goplan); in the box
  there is no go command. `arbace.box.plan` computes the same from trees: go/build's file
  selection (GOOS/GOARCH suffixes, `//go:build`, cgo off), imports from each file's header,
  std's vendor directory, GOOSPKG's runtime/goos, the module's language version, embedded
  files. The toolchain's facts it needs (its tool tags, release tags, `pkg/include`) are put
  in the store beside the tool guests (`tool.config.<arch>`). Checked against goplan on the
  same trees.
- **Embedded files, version, symbols.** The driver gains `-embedcfg` (the main package embeds
  the namespaces' sources and the image of prepared namespaces; x509roots its bundle), link's
  `-X main.arbaceVersion` from the tree's `VERSION` (git archive's export-subst, as a tarball's
  build reads it), and `tools/elfsyms`'s cut in Arbace (`arbace.box.elf`).
- **The host-built reference.** The driver's image is not `go build`'s (its flags, no build
  IDs, no module information; S7). The reference is the same steps run natively with the
  host's tools: a2g, g2a and the preparing on the native executable, the toolchain's own
  compile, asm and link, each action a host process chrooted to the request's trees
  (`arbace.box.rebuild/host-run`, `tools/chrootrun`). Its intermediate results are compared
  too, and a2g's program and the image of prepared namespaces with `bin/arbace-go --build`'s
  (BX110).
- **arm64.** The tool guests and the toolchain's facts for arm64; the box step in the aarch64 VM
  (every instruction emulated: hours), the host step on amd64 cross-compiling (GOARCH=arm64);
  the Mac's bundle gets the same run.
- Order: the guest as a tool guest and the driver's additions; the planner checked against
  goplan; the steps one by one on amd64 natively, then in the box; the comparison; arm64; the
  Mac bundle.

### What was built

- **Arbace's guest as a tool guest** (`go/arbace/jrt/hostbox_tamago.ae`; BX111). jrt's box host
  calls box/'s `tool.Start` in its `init` (in place of its own reading of the command line and
  the environment, which `tool.Start` does the same way), and its `Exit` ends the guest through
  `goos.Exit`, which `tool.Start` wraps in a spawned guest. A request naming Arbace's image
  then runs it as a tool guest: its trees mounted lazily, its working directory made, its output
  directory stored as a tree at an exit with 0. A guest not spawned is as before.
- **The plan in Arbace** (`arbace.box.plan`; BX112). From GOROOT/src, the modules' trees and the
  main package: go/build's selection of files (the name's GOOS and GOARCH, `//go:build`
  expressions over the toolchain's tags, test files and files beginning with `_` or `.` left
  out, files importing `"C"` left out with cgo off), each Go file's package clause and imports
  read from its header (a small tokenizer: comments, strings, Go's semicolon insertion),
  `//go:embed` patterns, imports resolved (a module's path prefix; GOOSPKG's runtime/goos in the
  goos module, not standard, as go list says; the standard library's vendor directory for its
  own imports, with the import map), the language version from each go.mod, the packages in go
  list's order (each after those it imports, imports in the order written). A package's source
  tree is its directory's files, the `.h` files below it and its embedded files, so that an
  action's key changes only with what it reads. Checked: on Arbace's box program (the trees
  `bin/arbace-go --build --box` prints, with its overlay), the plan equals tools/goplan's (go list
  -deps with that overlay) for every package and field, amd64 (181 packages) and arm64 (180);
  2.4 s natively, 3 s in the box. tools/goplan now reports embedded files (`:embed-patterns`,
  `:embed-files`) instead of refusing them, and the driver refuses to build such a package
  from a plan that does not say how (`:embed`).
- **The toolchain's facts** (`bin/box-tools put`; BX113): beside the tool guests, the ref
  `tool.config.<arch>`, `{:tool-tags [...] :release-tags [...] :include TREE :goversion
  "go1.27.1"}`: go list's `context.ToolTags` and `ReleaseTags` for tamago/<arch> (the default
  experiments' tags, `amd64.v1` or `arm64.v8.0`), `GOROOT/pkg/include` as a tree.
- **The driver** (`arbace.box.build`): `archives` and `link` apart (the main package is compiled
  again with the image of prepared namespaces, the others' archives kept: `:only`,
  `:archives`), `action` public (the rebuild's own steps are actions), `-embedcfg` from the
  plan's `:embed` (JSON, keys sorted, files at `/s`), per-package resources (`:resources`, and
  `:link`), and the executor as an option (`:run`).
- **The printing** (`arbace.box.print`), run as an action in Arbace's guest with g2a's namespaces
  from the sources (`ARBACE_PATH=/s`): `print-goroot` prints tamago/'s standard library and goos
  module as `bin/tamago-vendor print` does (each `go/` with `arch/amd64/go` laid over it, layout
  lines) and overlay/go's runtime and syscall as `bin/jrt overlay` and `box_overlay` do (layout
  gofmt); `print-program` prints a2g's program as `bin/arbace-go --build --box` does (`--goos
  tamago --line-file`, the roots box/ and x509roots/, the goos module named `/goos`). g2a's
  `program` takes `:go` (go.mod's go line) from its caller, and asks the toolchain (a process)
  only when neither program.edn nor the caller gives it and the module is not std.
- **The symbol cut** (`arbace.box.elf/cut-symbols`; BX118): tools/elfsyms in Arbace. On the
  guest image linked by `go build`, the same bytes as elfsyms's (3.0 s natively).
- **The rebuild** (`arbace.box.rebuild/steps`, `rebuild`): the chain above, every step an action
  (a2g `--program` with `ARBACE_PATH=/s`, `GOGC=400`, `GOMEMLIMIT=12GiB`, `A2G_JOBS=8` in a
  14 GiB, 8-vCPU guest; the two printings at once, 8 GiB and 4 vCPUs each; jrt's and lang's
  compiles in 12 GiB, 4 vCPUs; the link in 8 GiB; the rest 3 GiB, 1 vCPU), GOROOT/src assembled
  from the printed trees (std, tamago/goroot/src's own files over it, the runtime overlay's
  files, the patched `syscall/fs_tamago.go`), the goos module with its go.mod, the plan, the
  first link with Arbace's version (`-X main.arbaceVersion=` the tree's `VERSION` read as
  `bin/lib/version.bash` reads a tarball's), the namespaces prepared by that image spawned with
  `ARBACE_PREPARE=/o/image.bin` and bin/arbace-go's prepare script, the main package compiled
  and linked again, the cut. It answers every intermediate result (the trees, the plan, the
  version, both images, the image of prepared namespaces).
- **The host's executor** (`arbace.box.rebuild/host-run`, `tools/chrootrun`; BX110): an action
  for an image it knows runs as a host process, `chroot`ed into a directory holding the
  request's trees at their mount points (`tools/chrootrun ROOT DIR PROG ARG...`: chroot, chdir,
  exec), its output directory stored as a tree, its console as a blob, the result written as
  `tree.Result` prints it. The tool guests' images run the toolchain's own
  `pkg/tool/linux_amd64/{compile,asm,link}` with `GOOS=tamago GOARCH=<arch>`; Arbace's image the
  native executable; the image's own preparing run the native executable (the image of
  prepared namespaces is the same for every GOOS and GOARCH: bin/arbace-go prepares it once).
- **`bin/box-rebuild`**: `prepare` (the commit's sources as a tree, `git archive` of `VERSION
  arbace box go jdk overlay tamago x509roots`; the tool guests and facts; the image that runs the
  tools, bin/arbace-go's guest, as `arbace.<arch>`; one store for the box and one for the host,
  so that neither run takes the other's results from the action cache), `box` (arm64: in the
  aarch64 VM, detached, its log followed), `host`, `compare` (every step's result; a2g's
  program and the image of prepared namespaces against `bin/arbace-go --build`'s, which also
  proves that the host step prepared with an executable of the same program), `boot`, `smoke`
  (bin/box-smoke on a launcher with the rebuilt image appended). `--dev` loads the driver's
  namespaces from the source tree over the image's; `--given DIR` takes a2g's output and the
  two printings from another run's box step (their blobs copied into this run's store) and
  does not run them (`:given`; BX117); `plan-check` compares the box's plan with go list's on
  the box's printed trees. Redoing the byte-for-byte run after S6: `bin/arbace-go --build --box`
  (with arm64), then `bin/box-rebuild`, `bin/box-rebuild --arch arm64 prepare host`, the VM's
  `--given` run, `bin/box-mac-bundle`.
- **The store's race** (box/vmm `DirStore.Put`; BX115). The first run in the box failed with
  "the store refused" while compiling: the driver and its spawned guests write the same
  directory, and a blob two of them put at once (an empty `go_asm.h`, a shared tree) was refused
  to the second, whose exclusive create found the first's file. The monitor's system-call
  filter allows no rename, so `Put` cannot write aside and rename; it now waits (20 ms steps, at
  most a minute) for the other writer's file to be whole (its size by `fstat`, its hash by the
  read) before refusing, as it refused a short one before.
- **The Mac's bundle** (`bin/box-mac-bundle`, `box/mac/mac.sh`): `rebuild/store` (the commit's
  sources and the arm64 tool guests and facts) and `rebuild/expected.txt` (the arm64 image the
  host step gives); `./mac.sh rebuild` puts the bundle's guest as `arbace.arm64`, runs the
  rebuild in the box (the spawns' budget 60% of the Mac's memory), times it (HV7), requires the
  expected image, boots it by ref, and runs it again from the action cache; `./mac.sh clock` is
  HV6's hour (the guest's clock against the Mac's, a line a minute); `./mac.sh all` runs the
  clock beside check, sign, hv, the checklist and the rebuild.

### Measured (amd64, nested KVM, the shared machine)

`bin/box-rebuild --dir .tmp/box-rebuild/amd64-clean prepare box host compare boot smoke` from empty
stores, the commit `53d9b5d` (the aarch64 VM's run going on beside it, 16 emulated vCPUs; the
times are this machine's under that load, not the box's best):

| step | in the box | on the host |
|---|---:|---:|
| a2g `--program` (8 jobs) | 407 s | 387 s |
| the two printings, at once | 138 s | 47 s |
| the plan (181 packages) | 3 s | 4 s |
| 273 compiles and assemblies, 4 at once, and the link | 183 s | 150 s |
| of which jrt's compile, lang's | 108 s, 44 s | 90 s, 35 s |
| the namespaces prepared (the new image; natively the executable) | 21 s | 16 s |
| the main package and the link again, the cut | 41 s | 35 s |
| all | 773 s | 623 s |

- **The image built in the box equals the host's byte for byte**: `a3e7c069…`, 68,911,402 bytes,
  and so does every step's result: the sources' tree, a2g's program (`d57cb321…`), GOROOT/src as
  printed (`108bd7bc…`), the goos module, the module, the plan, the version, the first image, the
  image of prepared namespaces (`de06d439…`), the image before the cut.
- a2g's program (but `image.bin`) and the image of prepared namespaces equal `bin/arbace-go
  --build`'s: the box's a2g and the box-prepared image are the host's.
- Booted by its ref (`ARBACE_BOX_IMAGE=ref:rebuilt.box.amd64`): "booted by ref:
  arbace-seed-is-go-v1-62-g53d9b5d amd64", the version from the tree's `VERSION`.
- `bin/box-smoke` on a launcher with the rebuilt image appended: 119 ok, none failed.
- `plan-check`: 181 packages planned in Arbace, 181 by go list: the same.
- A rerun from the action cache: 65 s in the box (every action cached; the driver reads the
  trees and the plan again, and the guest's start).
- The guest that runs the driver: 6 GiB; the spawns' budget 40 GiB, 16 vCPUs.

### The exit criterion's runs (the commit `90c6f70`)

`bin/arbace-go --build` and `--build --box` (amd64 and arm64) at `90c6f70`, then:

- **amd64** (`bin/box-rebuild --dir .tmp/box-rebuild/amd64-final prepare box host compare boot smoke
  plan-check`): in the box 670 s (a2g 328 s; the printings 102 s; jrt's compile 90 s, lang's 37 s;
  the preparing 16 s), on the host 625 s. **The image built in the box = the host's, byte for
  byte**: `6c53c3b0432c0bafe633da0fb806d9c8f6c497e9b56032d5cf73f80b923ca9eb`, 68,911,402 bytes;
  every step's result equal; a2g's program and the image of prepared namespaces equal `bin/arbace-go
  --build`'s; booted by its ref ("booted by ref: arbace-seed-is-go-v1-66-g90c6f70 amd64");
  bin/box-smoke on it 119 ok, none failed; plan-check the same (181 packages).
- **arm64, the host's reference** (`bin/box-rebuild --arch arm64 --dir .tmp/box-rebuild/arm64-final
  prepare host`, cross-compiled here with the arm64 tool guests' facts): 523 s, the image
  `f78f49598272fbde58e3c5c045ebc78ac239eb26a6255eb575730f49dbf2a672`, 66,498,858 bytes. a2g's
  program and both printings are the amd64 runs' (`83011b56…`, `108bd7bc…`, `149a24de…`,
  `cbf675fa…`): they do not depend on the architecture.
- **arm64 in the aarch64 VM** (`.tmp/s8/vm-run2.sh`: the VM with 16 vCPUs and 28 GiB, then
  `bin/box-rebuild --arch arm64 --dir .tmp/box-rebuild/arm64-final --given .tmp/box-rebuild/amd64-final
  box boot`, then `compare`; BX117 (b)): 9,588 s in the VM (2 h 40 min: the plan
  204 s; jrt's compile 3,957 s, lang's 1,660 s, 44 times the amd64 box's; the first link 124 s;
  the preparing by the new arm64 image 1,287 s). **The arm64 image built in the box = the host's,
  byte for byte**: `f78f4959…`, 66,498,858 bytes, and every step's result equal (the plan, 180
  packages, `7b94168f…`; the first image `9092e16c…`; the image of prepared namespaces
  `60dd1605…`, the amd64 runs' too); booted by its ref in the VM ("booted by ref:
  arbace-seed-is-go-v1-66-g90c6f70 aarch64"). The whole chain in the VM, tried first, is in BX117.
- **The Mac** (`bin/box-mac-bundle` with `MAC_REBUILD_DIR=.tmp/box-rebuild/arm64-final`):
  `target/arbace-go/arbace-box-mac-arm64.tar.gz`, 46,884,873 bytes, for the user to run
  (`./mac.sh all`): the checklist as before with HV1's RAM checks, the rebuild of the whole chain
  (expected `f78f4959…`), timed for HV7, and HV6's hour of the clock beside them.

### Checks

- The essential `bin/gate` at `63e6ce0` (the code as at `90c6f70`): passed in 14m11s; the suite on
  the Go test build 20,506 of 20,520, no regressions; the oracle against the frozen v4 22,505 of
  22,558, the 53 known mismatches, 0 new. (An earlier run at `bfc52bd`, before the store's change
  moved files and `:given`, passed too, 17m30s.)
- `bin/arbace-go --smoke --box` (amd64): 119 ok, none failed. `bin/box-smoke` on the rebuilt
  image: 119 ok.
- `bin/box-vendor roundtrip`: 27 of 27 pass (vmm's new `storewait.go`; the first try, with the
  wait in `store.go` and its import, failed on a `/*line*/` directive the moved lines made:
  BOX-NOTES's layout rule, so the wait has a file of its own). `bin/box-vendor kvm-tests`: hv,
  vmm, go-whim's guest tests pass.
- `bin/tamago-vendor check` (amd64 and arm64, the guest with jrt's tool guest change): the guest
  images from the originals and from `tamago/`'s forms identical, as linked and cut; std's
  printing no regressions; the goos module's packages pass.
- `arbace.box.elf/cut-symbols` = tools/elfsyms on a linked guest image; `arbace.box.plan` =
  goplan on amd64 and arm64 (`.tmp/s8/ptest/run.sh`, and `plan-check`).

### Amendments (proposed 2026-10-13)

- **BX110 (S7's "For S8") What "the host-built image" is.** The driver's image is not `go
  build`'s: no build IDs (`-buildid=`), no module information, its own `-trimpath` mappings.
  Options:
  - (a) the same steps run on the host with the host's tools: a2g, g2a and the preparing on the
    native executable, the toolchain's own compile, asm and link, each action a host process
    chrooted to its trees (`host-run`); its intermediate results compared too, and a2g's program
    and the image of prepared namespaces against `bin/arbace-go --build`'s (built);
  - (b) the driver's flags made `go build`'s: its action and content IDs computed as cmd/go
    computes them, the module information generated, so that the image equals
    `bin/arbace-go --build --box`'s;
  - (c) the same driver run natively, its actions still guests (S7's comparison).

  *Recommended: (a)*: every step's tool runs outside the box, so the equality says the box
  computes what the host computes; (b) reimplements cmd/go's build ID scheme for no gain in what
  is proved; (c) runs the same guests twice.
- **BX111 (jrt) Arbace's guest as a tool guest.** a2g's step needs 14 GiB (BX81); the printing
  and the preparing need Arbace. Options:
  - (a) jrt's box host calls `tool.Start`, its exit goes through `goos.Exit`: Arbace's image is
    spawned like `compile`, each Arbace step an action in its own guest and slot (built);
  - (b) the REPL guest runs a2g and g2a itself, with `ARBACE_BOX_RAM=16GiB`, and only Go's tools
    are spawned;
  - (c) a separate image for the tools (a2g, g2a built into a program of their own).

  *Recommended: (a)*: the steps are cached and resumable like the compiles, the driver's guest
  stays small, and the new image prepares its own namespaces the same way; (b) has no action
  cache for the longest step; (c) waits for S6.
- **BX112 Where the plan comes from in the box.** Options:
  - (a) `arbace.box.plan`, go/build's rules for these trees in Arbace, checked against go list
    (built);
  - (b) cmd/go built as a tool guest, `go list -deps -json` spawned (its module loading, file
    locks and caches on TamaGo: untried), its JSON read in Arbace;
  - (c) the plan computed on the host and imported with the sources.

  *Recommended: (a)*: 300 lines, the same plan as go list on both architectures, and the driver
  stays Arbace's; (c) leaves part of the build outside the box.
- **BX113 (bin/box-tools) The toolchain's facts.** The planner needs the toolchain's tags (its
  default experiments) and `pkg/include`. Options:
  - (a) a blob `tool.config.<arch>` put beside the tool guests, from go list on the host (built);
  - (b) computed in Arbace from `internal/buildcfg`'s sources in the tree;
  - (c) written into the driver as constants.

  *Recommended: (a)*: they are facts of the toolchain, as its images are, and change with it.
- **BX114 (bin/box-rebuild) The source tree.** Options:
  - (a) `git archive REV` of what the build reads (`VERSION arbace box go jdk overlay tamago
    x509roots`), `VERSION` filled by export-subst, the version read from it as a tarball's
    build reads it (built);
  - (b) the whole commit;
  - (c) the checkout's files as they are (uncommitted changes in).

  *Recommended: (a)*: the same commit gives the same tree, 56 MB (7 MB as xz, for the Mac's
  bundle); (b) carries the tests and documents for nothing; (c) has no version and no commit to
  name.
- **BX115 (box/vmm) Two writers of one blob.** Options:
  - (a) `DirStore.Put` waits for the other writer's file to be whole, then succeeds (built);
  - (b) write aside and rename (the filter allows no rename: the filter widened);
  - (c) the driver serializes its puts (the spawned guests write the same directory still).

  *Recommended: (a)*: the filter stays as it is, and a short file left by a crash is still
  refused, after the wait.
- **BX116 The steps' resources.** Options:
  - (a) a2g 14 GiB and 8 vCPUs (`A2G_JOBS=8`); each printing 8 GiB, 4 vCPUs; jrt's and lang's
    compiles 12 GiB, 4 vCPUs (SA101 (b)); the main package 6 GiB; the link 8 GiB; the rest the
    3 GiB default; the driver's guest 6 GiB; the spawns' budget 40 GiB here, 60% of memory on the
    Mac, 20 GiB in the VM (built);
  - (b) as (a) with lang at the 3 GiB default (it needs about 2.3-4 GiB);
  - (c) every compile 12 GiB.

  *Recommended: (a)*, to be lowered once measured per guest (the launcher's statistics do not
  yet report a guest's peak).
- **BX117 arm64 in the emulated VM.** Every instruction is emulated there: the evaluator ran 40
  times slower than in the amd64 box (24.9 s against 0.62 s for the same loop). The whole chain
  was started in the VM (16 vCPUs, 32 GiB) and stopped after 2 h 15 min: a2g's guest was still
  loading (1.4 GB touched, 15 CPU-hours of the guest's process), against 407 s in the amd64
  box; the whole chain would take well over ten hours there. Options:
  - (a) the whole chain in the VM, detached and followed (`bin/box-rebuild --arch arm64 box`);
  - (b) in the VM the architecture's own steps (the plan, the compiles, the link, the preparing
    by the new image, the cut), a2g's output and the two printings taken from the amd64 box's
    run of the same sources (they are the same on both: the arm64 host step's equal the amd64
    box's), `bin/box-rebuild --arch arm64 --given DIR` (built); the whole chain on the Mac
    (`./mac.sh rebuild`);
  - (c) arm64 on the Mac only.

  *Recommended: (b) now, (a) again after S6*, whose compiled a2g and g2a are what the VM is
  slowest at; the Mac runs the whole chain on arm64 hardware either way.
- **BX118 The symbol cut in the box.** Options:
  - (a) `arbace.box.elf`, tools/elfsyms in Arbace, the same bytes (built);
  - (b) elfsyms built as a tool guest by the driver;
  - (c) the image kept whole in the store (the monitor reads the whole table).

  *Recommended: (a)*: 140 lines, no other image to keep.
- **BX119 (checks)** `bin/box-rebuild` (amd64: prepare, box, host, compare, boot, smoke, and
  `plan-check`, the box's plan against go list's on the box's printed trees) in `bin/gate
  --full` beside `bin/arbace-go --build --box` (about 25 minutes, 40 GiB of guests at most); the
  arm64 VM's run (hours) and the Mac's at the milestone.

### The runs again on the final program (2026-10-14, the commits `3bf351b`, `338c4a2`)

After S6 and its size work (`.tmp/s8b/run.sh`, at `3bf351b`; the journal at the tag
`arbace-inside-itself-v1`, the entries of 2026-10-14 "S8 again on the final program; the Mac byte
for byte" and "S8 in the emulated arm64 VM on the final program, byte for byte"):

- **amd64** (`bin/box-rebuild --dir .tmp/box-rebuild/amd64-v1 prepare box host compare boot smoke
  plan-check`): in the box 785 s, on the host 661 s. **The image built in the box = the host's,
  byte for byte**: `b7c00302bd689c975a01dc2151980fff1fcff88765495f64d32025eb6e5e3008`, 100,757,802
  bytes; booted by its ref; bin/box-smoke 119 ok; plan-check 281 packages, the same as go list's.
- **arm64, the host's reference** (`prepare host`): 656 s,
  `a733f9bc43971bbb78e56a10efeba25688d552b132919637c103359e6ffc3772`, 98,464,042 bytes.
- **arm64 in the aarch64 VM** (16 vCPUs, 28 GiB; `--given .tmp/box-rebuild/amd64-v1 box boot`,
  then `compare`): 30,039 s (8 h 21 min). The run began before `77b20b6`, so `--given` gave a2g's
  program and the printings but not the compiled namespaces: the VM compiled them itself,
  emulated, 11,183 s (100 sources, 7,689 classes, the amd64 runs' `effde3ca…`). Then the plan
  (280 packages, `1e70e360…`) 146 s; jrt's compile 8,118 s and lang's 2,526 s (86 and 77 times
  the amd64 box's); the links 151 s and 148 s; the preparing 38 s. **The arm64 image built in the
  box = the host's, byte for byte**: `a733f9bc…`, every step's result equal, a2g's program and the
  image of prepared namespaces equal `bin/arbace-go --build`'s; booted by its ref in the VM
  ("booted by ref: arbace-seed-is-go-v1-145-g3bf351b aarch64").
- **The Mac** (Apple M2 Max, macOS 14.8.7): the bundle of `3bf351b` rebuilt `a733f9bc…` byte for
  byte in 353 s, with two failures of the checklist script's own: `mac.sh`'s `box` read `cpus`,
  set only inside the checklist, and its boot check did not expect `aarch64`; and a guest of 61
  GiB was refused by Hypervisor.framework's `hv_vm_map` (`HV_BAD_ARGUMENT`) before the monitor's
  limit check could name the limit. Fixed in `338c4a2`: the monitor checks the guest's RAM against
  `maxGuestEnd` before any memory is mapped (moved in `c3fdd36` to
`vmm/guestram.go`'s `mapGuest`, `vmm.go`'s vendored layout restored: `bin/box-vendor roundtrip`
27 of 27); `box` defaults to 4 vCPUs; the boot check expects
  `aarch64`. The bundle of `338c4a2` (its arm64 host image `579c8b3b…`): the checklist 46 of 46,
  the rebuild 4 of 4, the image rebuilt on the Mac = Linux's, `579c8b3b…`, 98,464,042 bytes, in 352
  s, booted by its ref. HV1 on the Mac: 6 GiB and 60 GiB slots, 61 GiB refused with the limit
  named, 16 GiB with 10 GiB touched in 7.1 s. HV6: about 5 ppm over an hour, no resync. HV7: 352 s
  on the Mac against 785 s in the amd64 box (nested KVM). The whole chain on arm64 hardware holds.

So S8's exit criterion holds on the final program: byte for byte on amd64, in the emulated arm64
VM and on the Mac. The whole chain in the VM (BX117 (a)) is running since 2026-10-14 22:49 (the
namespaces compiled in 8,839 s; no result yet, as of 2026-10-15). On 2026-10-16 05:45, 28.5 h
into a2g's step (the user: kept running): in the VM, the a2g tool guest's process 8 d 16 h of
CPU (about 250 times the amd64 box's a2g of 303 s wall), 13.6 GB resident and flat; the
requesting guest, waiting on the spawn, 3.5 of the VM's 16 vCPUs (5 d 3 h of CPU): its wait is
not idle under emulation, to look at (WFI or the event area polled).

### The milestone (2026-10-15, `c3fdd36`)

`bin/gate --full` passed at `c3fdd36` in 37 min 48 s, after two fixes of its first run's
failures: `jdk/edge.edn` recorded again (a2g's program unchanged), and the guest's RAM check moved
from `vmm.go` to `vmm/guestram.go` (`mapGuest`), `vmm.go`'s vendored layout restored
(`bin/box-vendor roundtrip` 27 of 27). `bin/box-rebuild` by hand at `c3fdd36` (amd64): in the box
702 s, on the host 622 s, **the image built in the box = the host's, byte for byte**:
`3131e754d401aad5774c023091147ed7c60cd890c0e78a96d9df51919acd68e7`, 100,757,802 bytes; booted by
its ref ("arbace-seed-is-go-v1-155-gc3fdd36 amd64"); bin/box-smoke 119 ok; plan-check 281
packages. Tagged `arbace-inside-itself-v1` on main (`e0ad476`).

### Left

- The whole chain in the VM (BX117 (a)): running since 2026-10-14 (no result yet).
- The guests' peak memory per step is not measured (the launcher's statistics report counts,
  not a guest's touched pages); BX116's sizes are SA101's and S7's.
- `bin/box-rebuild` stays out of `bin/gate --full`, run by hand at milestones (BX119).

**Decided (2026-10-14, the user):** BX110 (a), BX111-BX116 (a), BX118 (a), as recommended;
BX117 (b) now, (a) tried again after S6, the Mac running the whole chain on arm64; BX119 against
the recommendation: `bin/box-rebuild` stays out of `bin/gate --full`, run by hand at milestones;
the Mac's bundle run once, built from the program after S6.
