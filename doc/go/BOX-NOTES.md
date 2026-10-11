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
