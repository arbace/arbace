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
- **The oracle's box subset** (RESULT_ORACLE).
- **Clojure's suite in the box** (RESULT_SUITE).
- **`bin/box-vendor roundtrip`** (in `bin/gate --full`): `box/` printed with its tests, the
  printed Go converted back for each configuration and merged as `convert` merges, printed
  again, and the two printings compared by gocmp (tree, export data, code; with tests) per
  configuration: RESULT_RT.
- **`bin/box-vendor kvm-tests`** (in `bin/gate --full`): go-whim at `80c49bf`, renamed, its box
  packages replaced by `box/` printed, its `editor.Exit` given the method `ExitCode` (the one
  adaptation: its tests' recording hosts serve `editor.Main` and `vmm.Run` alike), then `hv`'s,
  `vmm`'s and the guest tests S0 ran (`TestHello`, `TestFault`, `TestWatchdog`, `TestGoGuest`,
  `TestGoGuestFault`, `TestReadAheadC`, `TestReadAheadGo`, `TestGoGuestSMP`): RESULT_KVM.
- **The Linux executable is unaffected**: RESULT_LINUX.

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

Numbered BX (a prefix not used in `doc/`); proposed 2026-10-11, for the user.

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

### Decisions for the user

- Accept BX1-BX10.
- The box's smoke test, suite subset and oracle subset in `bin/gate --full` (as the survey's §8
  proposed for S1/S2)? Only the round trip and the KVM tests were added, as decided; the box's
  build adds about 2 minutes and its checks about RESULT_TIME.
- R1 / S2: the launcher's 445 MB resident (the image read whole, then copied into the slot);
  mapping the appended image instead of reading it would save most of the 77 MB twice.
- arm64 (S4): `board.NilPanics` and the vectors' return to `sigpanic` need their arm64 form
  (ELR_EL1, the link register) before the box's arm64 oracle can pass.
