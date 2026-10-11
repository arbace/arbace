# The box: go-whim's VMM for Arbace — survey and plan

Status: survey, 2026-10-10, for B1b ([B1-PLAN.md](B1-PLAN.md), "B1b, after the freeze: the
box"). The decisions (§10) are the user's, taken 2026-10-11, after the `arbace-for-golang`
freeze. Stage S0 is done (2026-10-11: go-whim's box vendored as Go forms under `box/`;
[BOX-NOTES.md](BOX-NOTES.md)), S1 (2026-10-11: the boxed REPL on amd64, BOX-NOTES.md, S1)
and S2 (2026-10-11: measured and hardened, BOX-NOTES.md, S2); the rest of this document is the
survey as written. The experiments (§9) ran in `.tmp/boxsurvey/` on copies; go-whim
was read only.

Sources: go-whim at `80c49bf` (2026-10-06, `/root/go-whim`, `github.com/arbace/go-whim`);
TamaGo's Go distribution `/root/tamago-go` (`VERSION` go1.27.1, 2026-08-28), its `goos` module
`github.com/usbarmory/tamago` v1.27.1 (module cache); purego v0.11.1. Arbace at main `105db68`,
with the work-in-progress branches `goproc` (`HostProc`), `gosec`, `gotext` noted where they
change the surface (all three merged since: `goproc` and `gosec` 2026-10-10, `gotext`
2026-10-11).

**Since the survey** (main, 2026-10-11), what changes its findings: `gofix` added a fifth
optional host interface, `HostExecutable` (`resources.ae`: the executable's path, which the
embedded resources' `jar:` URLs name; a host without it, the box's, names `/arbace`); `gosec`
moved `OSHost.RandomBytes` into the Linux-tagged `hostrand_linux.ae`, so on `GOOS=tamago`
`OSHost` alone no longer satisfies `Host` and a box host supplies `RandomBytes` itself (§9.4's
scratch host already does: over `Random`), and `host.ae`'s default `theHost` (`OSHost`, in an
untagged file) would then not compile for `GOOS=tamago`: by reading, not built, a second file
besides `net.ae` that the box's build must tag or give a stub (§7, §9.2); the native executable is 66.4 MB on amd64 (the
default locale data, stripped since amendment SZ1), against the 58 MB the survey names.

## Executive summary

- **go-whim has a working box for both hypervisors and both ISAs**, about 5,000 lines of Go
  and assembly that do not depend on the editor except at one seam: `hv/` (1,616 lines: one
  API in the shape of Hypervisor.framework's C API; KVM by raw ioctl, HVF by purego, no cgo
  anywhere), `vmm/` (2,381: ELF loading into one identity-mapped slot, boot straight into
  64-bit ring 0 / EL1, the exit loop, SMP, watchdog, seccomp, the store), `guest/abi` (130:
  the doorbell ABI as constants) and `guest/tamago/board` (755: a TamaGo board for that
  monitor, amd64 and arm64). KVM amd64 is run here; KVM/arm64 was gated in an emulated
  aarch64 VM; Hypervisor.framework on the user's M2 Max (milestone 4, 2026-10-04/05). The
  Go editor and a Joker REPL (a Clojure dialect, 17-23 MB image) already run in it on TamaGo.
- **The seam is `vmm.Config.Host`, typed `editor.Host`**: the monitor answers 17 of its 29
  calls by calling the vim-shaped editor host (tty raw mode, winsize, wait/read keys, write
  the screen). Arbace would vendor `hv/`, `guest/abi` and the board as they are, `vmm/` with
  its dispatch made generic (a call table instead of `editor.Host`), and write its own
  launcher and host. The editor-specific parts (the C guest's `rt.c`, `editor/term`, the
  suites) stay behind.
- **The surfaces differ radically, as the user expected**, but the overlap is exactly what
  a REPL needs: jrt's `Host` has 20 methods plus 4 optional interfaces (files beyond `Host`,
  network, links, processes: about 40 methods more; a fifth since, `HostExecutable`); the doorbell offers a console (merged
  wait-and-read, write, message), exit, random bytes, two clocks, SMP, and a content-addressed
  store. **A boxed REPL needs no new hypercall** (Joker's did not; neither did §9's): the
  wall clock is the one gap (`Time` says seconds), best closed by an offset at entry rather
  than a call. Files, sockets, processes have no host surface at all, by design.
- **Most of arbace-go needs nothing from the host**: the language, collections, numbers,
  strings, regex, the image, class forms at the REPL, time zones (tzdb is embedded), text
  once `gotext` lands (merged 2026-10-11). Files can be TamaGo's in-memory file system with no host surface
  (Go's `os` works on it), later backed by the store. Sockets and the socket REPL need a
  network design (go-whim sketched netstack in the guest over a datagram call); processes do
  not make sense in a box and stay cut; rlwrap moves to the host side (rlwrap runs the
  launcher).
- **Recommended first target (S1)**: the same a2g program, built `GOOS=tamago` with a jrt
  host over the doorbell (console, clocks, random, exit, embedded resources; no net or
  proc interface; files in memory, since Go's `os` gives them for free), amd64 on /dev/kvm,
  go-whim's default of four vCPUs, the console read bounded so that it yields: `arbace -e`,
  `-m` and the REPL on the console; the oracle and Clojure's suite run in the box on their
  subsets. Then hardening and measurement, the store, arm64 (KVM/arm64, the Mac), the
  network, and the image's own Go as round-tripped forms (§6).
- **Tried here: arbace-go already runs in go-whim's box.** The printed program built with
  `GOOS=tamago` fails only in `jrt/net.go` (raw socket syscalls, five constants and
  `Syscall6`); stubbed in a copy, with a scratch `main` of about 80 lines (a jrt host over the
  board's calls) and go-whim's monitor unmodified, the REPL reads forms from the console,
  evaluates, prints, `spit`s and `slurp`s in TamaGo's in-memory fs, runs a future, and exits
  0 on /dev/kvm: start 0.4 s (native 0.2), a 1M-element reduce 1.4 s on 1 vCPU (native 1.0-1.2
  at `GOMAXPROCS=1`), 1.0 s on 4 (§9). Two defects showed at once: `new Date()` says 1970,
  and a blocking console read starves every other goroutine (1 vCPU) or delays their output
  until the next line (4 vCPUs).
- **Here, /dev/kvm works** (`crw-rw-rw- root kvm`, AMD EPYC 7763 nested SVM): go-whim's `hv`
  KVM tests pass from a clone (§9). arm64 boxes: images cross-compile here, KVM/arm64 runs
  under `qemu-system-aarch64 virtualization=on` (go-whim's `guest/vm/aarch64.sh`), HVF only
  on the Mac.
- **Licence**: go-whim's `LICENSE` is Vim's licence and covers "the product", the modified
  vim; the box code is not vim-derived and every one of go-whim's 667 commits is the user's
  (`rhz`). So the user can license the box parts to Arbace (EPL-1.0, as Arbace) by
  recording it in `LICENSE.md`; purego (Apache-2.0) and TamaGo's `goos` module (BSD-3,
  "The TamaGo Authors") get sections when they enter a build (§3).

## 1. Context

B1a's executable is a2g's translation of `arbace.lang` plus the JDK closure, on jrt (hand-written
Go forms), printed and built by gc as a static `linux/amd64` and `linux/arm64` program
(EXEC-NOTES.md). Its build already uses TamaGo's distribution as `GOROOT` (`bin/arbace-go`,
`go_env`: `G2A_GOROOT=/root/tamago-go`, `GOOS=linux`), and g2a's round trip covers std for
`tamago` and `linux` (AGENDA.md). jrt reaches the operating system only through `jrt.Host`
(A2G-SPEC §9.4), so that "B1b swaps one value" (`go/arbace/jrt/host.ae`).

B1b: the same program built with `GOOS=tamago`, its host answered by go-whim's doorbell
through a TamaGo board, run by go-whim's monitor; amd64 on /dev/kvm here, arm64 under QEMU
here and on Hypervisor.framework on the Mac. D3 (how Arbace depends on go-whim) was moved to
B1b (B1-PLAN.md, "Decisions").

## 2. go-whim's box

### 2.1 The shape

```
 guest (TamaGo image, ring 0 / EL1, 1..32 vCPUs)        monitor (one host process, Go)
 ┌──────────────────────────────────────┐              ┌────────────────────────────────────┐
 │ program (Go editor | Joker | ...)    │              │ cmd/whim-guest: image appended or  │
 │ Host as calls (guest/tamago/main.go) │  doorbell:   │   beside, env WHIM_GUEST_*         │
 │ board: runtime/goos hooks, SMP,      │  one 64-bit  │ vmm: slot, boot, exit loop, SMP,   │
 │   vectors, doorbell (boot_ISA.s)     │──store of a ─▶│   watchdog, seccomp, store         │
 │ TamaGo runtime + std (GOOS=tamago)   │  call block's│ hv: VMCreate/VMMap/VCPURun ...     │
 └──────────────────────────────────────┘  address     │   KVM (ioctl) | HVF (purego)       │
                                                       └────────────────────────────────────┘
```

No firmware, no devices, no interrupts, no timer: the guest runs until it stores to the
doorbell (an unmapped guest-physical page, `0xf0000000`), which exits on all three targets
(KVM's `KVM_EXIT_MMIO`, HVF's data abort with a valid syndrome). go-whim's doc/GUEST.md, "The shape",
"The hypercall ABI".

### 2.2 `hv/`: one API, two hypervisors, two ISAs

- `hv/hv.go`: the API is Hypervisor.framework's arm64 C API one function for one (`VMCreate`,
  `VMMap(mem, ipa, flags)`, `VCPUCreate` returning the vCPU and its exit record, `VCPURun`,
  `VCPUsExit`, `VCPUGetReg`/`SetReg`, `GetSysReg`/`SetSysReg`), Apple's register names; the
  exit is `hv_vcpu_exit_t`'s shape with an ESR syndrome decoder (`Syndrome.EC/ISV/SRT/WnR`).
  Decided 2026-10-04 (`5b6f8bf`, go-whim's doc/GUEST.md "Decided"): KVM follows HVF's concepts so the
  Mac port is "almost trivial".
- `kvm_linux.go` (438), `kvm_linux_amd64.go`, `kvm_linux_arm64.go`, `tsc_linux_amd64.go`:
  `/dev/kvm` by raw `syscall.Syscall` ioctls, no cgo; an MMIO store is reported as HVF's data
  abort, so the monitor decodes one form; the pointer arguments kept as pointers across the
  call (`ioctlPtr`, a goroutine's stack moves; the bug found on KVM/arm64, `f026bf2`); the
  VM's lock across `VCPUsExit` and `VCPUDestroy`.
- `hvf_darwin.go` (250): the framework by purego (`Dlopen`, `RegisterLibFunc`), so
  `GOOS=darwin GOARCH=arm64 CGO_ENABLED=0` compiles it anywhere (`0e2e3dc`); every vCPU on the
  first's vTimer offset.
- amd64 sits behind the same shape with x86 names (`regs_amd64.go`: RIP, CR0/3/4, EFER,
  segments in VMX encoding). Apple's x86 API is not the model.
- One VM per process; a vCPU is created, run and destroyed on one locked thread
  (`runtime.LockOSThread`), the framework's rule, kept on KVM too.

### 2.3 `vmm/`: the monitor

| part | files | what it does |
|---|---|---|
| memory | `layout.go`, `tamago.go`, `dontneed_*.go` | one slot from guest-physical 0, `mmap(MAP_NORESERVE)`, committed on touch; identity map, text RX, data NX, page 0 and the doorbell unmapped; 16 KiB alignment (Apple silicon) held by `layout_test.go`; a TamaGo image (known by the symbol `runtime/goos.RamStart`) gets one RW span to `GoRAMBytes` = 3 GiB above its 2 MiB base, the runtime's heap growing up from its bss, its first stack under the top, the vectors' stacks above |
| boot | `setup_amd64.go`, `setup_arm64.go`, `tamago_ISA.go` | amd64: GDT, TSS, IDT, 4-level tables (2 MiB pages), `SREGS` long mode, ring 0; arm64: EL1h, MAIR/TCR/TTBR0/VBAR/SCTLR with the MMU on; a Go image's registers: argc/argv, `RamStart`, `RamSize`, `RamStackOffset`, the TSC rate in kHz (amd64, `KVM_GET_TSC_KHZ`), the vCPU count |
| exit loop | `vmm.go` (735) | decode the doorbell store, bounds-check every guest pointer against the slot, answer, resume; any other exit ends the run with a diagnostic; counts (`Stats`); the image appended to the launcher (`WHIMGST1` trailer) or beside it (the Mac: `codesign`) |
| SMP | `smp.go`, `park_linux.go`, `park_other.go` | up to 32 vCPUs, all created before the guest runs (so seccomp's `TSYNC` covers them), each its own thread and exit loop; `CPUStart`, `CPUPark` (a futex, ns), `CPUWake` (a token; -1 all), `CPUSelf`; parks bounded by 1 ms while another vCPU runs; Host calls serialized one at a time |
| watchdog | `vmm.go` | ends a guest that runs N (default 60 s) without a hypercall |
| seccomp | `seccomp_linux*.go` | after the VM is built: 45 syscalls and `KVM_RUN`/`KVM_*SREGS` ioctls (amd64 list in `seccomp_linux_amd64.go`); none on macOS (signed with `com.apple.security.hypervisor` alone) |
| store | `store.go` (267) | blobs by SHA-256 and refs (names `[A-Za-z0-9._-]{1,64}`, compare-and-set), `DirStore` a host directory under the filter as it stands (`0a9f5b4`) |
| alternatives | `alt_ISA.go` | the `out`-port and `HVC` traps, measured against the doorbell (milestone 5, `b83df4f`) |
| death | `die_linux.go`, `die_darwin.go` (+ `internal/libsys`, purego bindings of libSystem's signal calls) | the monitor dies of the guest's signal, as the editor would |

### 2.4 The ABI: the doorbell and its calls

The guest fills a 64-byte call block (`nr`, `a[5]`, `ret`, `event`) on its own stack, aligned,
and stores its address to `0xf0000000` (one `STR` from X0 on arm64, so the syndrome is valid).
Pointers are guest-physical (= virtual: identity map). `event` carries a deadly signal the
monitor caught during the call. `guest/abi/abi.go`; the monitor's numbers held to it by
`vmm`'s `TestABI`.

| # | call | answered by | generic? |
|---|---|---|---|
| 1 | `HostInit` | `Host.Init(deathtrap)`: install signal handlers | editor |
| 2 | `GetWinsize` | `TIOCGWINSZ` | terminal |
| 3, 4 | `TermStart`, `TermStop` | raw mode on / off | terminal |
| 5 | `TTYKeys` | erase/intr keys, ICRNL/ONLCR | terminal (vim) |
| 6 | `NowMs` | monotonic ms | generic |
| 7 | `Delay` | sleep | generic |
| 8, 9 | `WaitForInput`, `ReadInput` | poll and read the tty (a signal as its key sequence) | console |
| 10 | `Suspend` | `SIGTSTP` the monitor | terminal |
| 11 | `Exit` | end the run with a code | generic |
| 12 | `Message` | write to stdout or stderr (`a[2]`) | generic |
| 13, 14 | `Alloc`, `Free` | never made (the C guest's allocator) | — |
| 15 | `Write` | write the screen's output to the tty | console |
| 16 | `Time` | Unix time, **seconds** (`WHIM_TIME` pins it) | generic |
| 17 | `Raise` | raise a signal in the monitor | editor |
| 18 | `Fault` | the guest's exception report (vector, address) | generic |
| 19 | `Random` | host `crypto/rand` into a buffer | generic |
| 20 | `WaitRead` | wait (ms, -1 for ever) and read into a buffer in one exit | console |
| 21-24 | `CPUStart`, `CPUPark`, `CPUWake`, `CPUSelf` | SMP | generic |
| 25-29 | `BlobPut`, `BlobSize`, `BlobGet`, `RefGet`, `RefSet` | the store | generic |

17 of these are `editor.Host`'s methods (`editor/host.go`): the monitor's dispatch (`vmm.go`,
`(*vcpu).call`) calls `cfg.Host`, an `editor.Host`, and the launcher passes `editor/term`'s
terminal host. That import (`vmm.go`: `github.com/arbace/go-whim/editor`) and the launcher's
(`vmm/cmd/whim-guest`: `editor/term`) are the only ties of `vmm/` to the editor.

### 2.5 The guest side

- **The board** (`guest/tamago/board`: `board.go`, `smp.go`, `boot_amd64.s`, `boot_arm64.s`)
  answers TamaGo's `runtime/goos` hooks:

  | hook | answer | exit? |
  |---|---|---|
  | `CPUInit` | its own `cpuinit` (TamaGo's amd64 one rebuilds tables at 0x9000 and would clobber the monitor's): entry registers kept, SSE on without XSAVE (no AVX), FP/SIMD on arm64, the stack | no |
  | `Hwinit0` / `Hwinit1` | nothing / `goos.Exit`, `goos.Idle` set | no |
  | `Nanotime` | TSC (`CNTVCT_EL0`) scaled by the monitor's rate, 32.32 fixed point, zero at entry | **no** (in the guest) |
  | `Printk` | a 256-byte line, then `Message` to **stderr** | yes |
  | `GetRandomData` | `Random` | yes, once a run |
  | `Exit` | flush, `Exit` | yes |
  | `Idle` | 1 vCPU: spin to the next timer (nothing else can wake it); several: spin 20 µs, then `CPUPark`; a deadline within 200 µs (a stop of the world) wakes all (`e1d9d90`) | parks |
  | `Task`, `Wake`, `ProcID` | `CPUStart` (the AP on its g0's 16 KiB stack, the 8 MiB TamaGo allocates given back), `CPUWake`, `CPUSelf` | yes |
  | exceptions | the monitor's IDT/VBAR into `whim_vectors`, reported by `Fault` | yes |

  `board.Call(nr, a0..a4)` is the one primitive (`nosplit`, the block on the caller's stack);
  `board.Args()` the command line the monitor wrote.
- **The Go editor guest** (`guest/tamago/main.go`, 138 lines): `editor.Host` as calls, with the
  merged wait-and-read's buffer in the guest (`e8854cd`).
- **The Joker guest** (`guest/joker/main.go`): a REPL whose input is an `io.Reader` over
  `WaitRead(-1)` and whose output is `os.Stdout` (TamaGo's `write1` → `Printk` → `Message`).
  Namespaces `box` (the store), `ed`, `term`, `rt` (the collector's metrics). Fork of Joker
  (EPL-1.0) at `guest/joker/joker` with two fixes. go-whim's doc/LISP-SANDBOX.md.
- **TamaGo's own std on `GOOS=tamago`** (`/root/tamago-go/src`): `os` over an in-memory file
  system (`syscall/fs_tamago.go`, NaCl's), fds 0-2 reserved, writes to 1 and 2 going to
  `Printk`, reads of 0 failing; `net` needs `net.SocketFunc` set by the program
  (`net/net_tamago.go`); `time.Local` is UTC (`time/zoneinfo_tamago.go`); `runtime.walltime`
  is `nanotime` (`runtime/os_tamago.go:165`: the wall clock starts at 1970 unless the board's
  `Nanotime` is offset); no `sysmon`, no asynchronous preemption (`preemptMSupported = false`),
  Ms bound to Ps (`runtime/proc.go`, "Ms are bound to P on tamago").

### 2.6 Build and load

`guest/gotamago.go`: TamaGo's `bin/go build -trimpath -ldflags "-T 0x201000 -R 0x1000"` with
`GOOS=tamago GOARCH=amd64|arm64 GOOSPKG=github.com/usbarmory/tamago GOROOT=$TAMAGO_ROOT
GOTOOLCHAIN=local CGO_ENABLED=0`, the guest a Go module of its own (so the main module stays
standard Go), its `go.mod` requiring `github.com/usbarmory/tamago v1.27.1` for `GOOSPKG`. The
ELF is appended to the monitor's executable (`vmm.Append`, `WHIMGST1`) or put beside it
(`PROGRAM.elf`, the Mac). Go editor image 6.2 MB, launcher 9.8 MB; Joker 23 MB with its
standard namespaces.

Without `GOOSPKG`, `GOOS=tamago` builds against `runtime/goos/linux_user.go`: TamaGo as a
Linux user process (raw `write`, `clock_gettime`, `getrandom`, `exit`). §9 uses it.

### 2.7 Measured (go-whim's numbers)

- Exit cost 16-18 µs here (nested SVM), about 1 µs on bare metal; the Mac's guest runs at
  1.2-1.4x the native C (go-whim's doc/GUEST.md, "Expected performance", "Running on the Mac").
- Go editor guest: start and `:q!` 20-40 ms, 35 MB; heavy case 1.3-1.4x the C here.
- Joker in the box: start 0.06 s; `fib 27` 0.44 s (host 0.26); the views' index of a 9.66 MB
  EDN graph 7.08 s (host `GOMAXPROCS=1` 5.28); collector pauses p50 3.6-5.1 µs on one vCPU,
  82-131 µs on four (go-whim's LISP-SANDBOX.md, "The collector in the box").
- **The one-vCPU stall** (`798200a`): with no `sysmon` nothing preempts a running goroutine; a
  collection that starts during a long computation waits for it to block, the computation
  assisting with the write barrier on: `fib 27` 1,280 ms instead of 321. Joker's two
  backends now yield every 65,536 calls (`yieldTick`).

### 2.8 Generic vs the editor's

| part | Arbace's use | notes |
|---|---|---|
| `hv/` | as is | the KVM tests and `hv_arm64_test.go` with it |
| `vmm/` layout, setup, tamago, smp, park, store, seccomp, die, watchdog, `Append` | as is | `die_darwin.go` needs `internal/libsys` |
| `vmm.go`'s dispatch | **rewrite the seam** | `Config.Host editor.Host` → a call table (`map[nr]func(*Call)`) or an Arbace-shaped host interface; `callNames` |
| `guest/abi` | as is, then extended | keep the numbers (the monitor serves both kinds of guest), add Arbace's calls past 29 |
| `guest/tamago/board` | as is | Printk to stderr is a choice to revisit (§4) |
| `vmm/cmd/whim-guest` | model only | Arbace's launcher: its own env names, no `editor/term`, rlwrap |
| `guest/rt` (C guest), `editor/`, `editor/term`, `guest/tamago/main.go`, suites, `guest/mac/shim.h` | no | the editor's |
| `guest/joker`, `doc/LISP-SANDBOX.md` | precedent | the REPL loop, the store's namespace, the yield |
| `guest/vm/aarch64.sh`, `guest/mac/mac.sh` | model / copy | the arm64 gate VM, the Mac checklist |

## 3. Licence and provenance

- go-whim's `LICENSE` is Vim's licence, added by `8ba7c9d` (2026-09-21, "init"); README.md,
  "License": "The product is modified vim, under vim's licence". It covers the editor
  derived from vim (`src/`, `editor/`, `guest/rt`'s `vim_snprintf`). `hv/`, `vmm/`,
  `guest/abi`, `guest/tamago/board` and `internal/libsys` are not derived from vim, and all
  667 commits are by `rhz <robert.zawiasa@gmail.com>`: the user can license them to Arbace.
  Recommended: EPL-1.0 like the rest of Arbace, a `LICENSE.md` section naming go-whim, the
  paths and the commit, and `doc/ARBACE.md`/the journal recording the origin (CLAUDE.md,
  "Records").
- Built into a box but not vendored: TamaGo's `goos` module (`github.com/usbarmory/tamago`
  v1.27.1, BSD-3-Clause, "Copyright (c) The TamaGo Authors"): needs its own `LICENSE.md`
  section once the image contains it; TamaGo's Go tree is already covered ("Go: BSD 3-Clause",
  from TamaGo's go1.27.1 `LICENSE`).
- purego v0.11.1 (Apache-2.0): only the macOS monitor (`hv/hvf_darwin.go`,
  `internal/libsys`). Apache-2.0 is already in `LICENSE.md` (Murmur3's).
- The Joker fork (EPL-1.0) is not needed; Arbace has its own Clojure.

Provenance (go-whim): design `5b9ea72`, `5b6f8bf`, `5c57c40`; milestone 1 `9fa0d74`, 2
`6e69d23`, 3 `ff7c615`, HVF `0e2e3dc`, 5 `b83df4f`, the Mac `d622316`; TamaGo guest
`1544268` (arm64 `e0f7c8c`); merged wait/read `e8854cd`; SMP `a2baeda`, `f026bf2`; store
`0a9f5b4`; futex park `8b88bd8`; STW wake `e1d9d90`; yield `798200a`; Joker `edab746`.
The box's files last changed at `815e11a`; head `80c49bf`.

## 4. The surfaces mapped

jrt's host today (`go/arbace/jrt/host.ae`, `hostfs.ae`, `net.ae`, `timezone.ae`; `goproc`'s
`hostproc.ae`) against what the box offers. "Boot" means data the monitor writes at entry
(argv today; the environment would be new), "guest" answered inside the image, "new" a call
to add.

| jrt | used for | box answer | kind |
|---|---|---|---|
| `Stdin()` | `System.in`, `*in*` | `WaitRead` into a guest buffer, as Joker's console | existing call |
| `Stdout()` | `System.out` | `Write` (the tty) | existing call |
| `Stderr()` | `System.err`, `printStackTrace` | `Message(err=1)` | existing call |
| `Open`, `Stat`, `ReadDir`, `Remove`, `Mkdir`, `Rename`, `Getwd` | `java.io.File`, file streams, `load-file`, `ARBACE_PATH` | TamaGo's in-memory fs through `os` (OSHost unchanged); later backed by the store | guest (S1), store (S3) |
| `Now()` | `currentTimeMillis`, `Date`, `Instant.now` | `Time` is seconds: **new call `WallTime` (ns)**, or the host's ns at entry plus `Nanotime` | new call or boot |
| `Nanotime()` | `nanoTime`, timed waits | the board's TSC clock (Go's `time.Since`) | guest |
| `Getenv`, `Environ` | properties, `TZ`, `ARBACE_*`, `GOGC`, `JAVA_TOOL_OPTIONS` | a curated environment written at entry (the launcher's allow-list) | boot (new) |
| `Args()` | the command line | `board.Args()` | boot |
| `NumCPU()` | `availableProcessors`, fork-join | the vCPU count (`runtime.NumCPU()` = GOMAXPROCS the board set) | boot |
| `RandomBytes(b)` | `SecureRandom` seeds, `UUID` | `Random` | existing call |
| `Resource(name)` | `RT.load`, `getResourceAsStream`, tzdb, ICU data | `go:embed` in the image, as now | guest |
| `HostExecutable.Executable` (since `gofix`) | the embedded resources' `jar:` URLs | absent: the URLs name `/arbace` | none |
| `Exit(code)` | `System.exit`, the end | `Exit` (after flushing Printk) | existing call |
| `HostFS`: `Access`, `Chmod`, `Chtimes`, `Realpath`, `Statfs` | `File`'s queries | absent (the JDK's failure answers), or over the in-memory fs | none |
| `HostLinks.Readlink` | default zone from `/etc/localtime` | absent: `TZ` from the environment, else UTC/`GMT+hh:mm` | none |
| `NetHost`: `Listen`, `Dial`, `LookupIP`, `LookupAddr`, `Hostname`, `HasFamily`, `Set/GetOption`, `Available` | sockets, the socket REPL, `URL` | absent ("Network not available"); later §6 S5 | none, then new |
| `HostProc` (`goproc`): `Start`, `Pid`, `ProcStat`, `ProcInfo`, `Pids`, `Kill`; `HostChild` | `ProcessBuilder`, `ProcessHandle`, `sh` | absent: `start` throws the JVM's `IOException`; `Pid` 1 | none (cut) |
| terminal (`cmd/arbace/rlwrap_linux.ae`: `isatty`, `TIOCGWINSZ`, `execve` rlwrap) | line editing | the launcher's, on the host: rlwrap runs the monitor; the guest's `rlwrap_other.ae` is already empty | host side |
| signals | none in jrt (no `os/signal`); Ctrl-C ends the JVM | the monitor dies of it (`die_*.go`), the tty stays cooked | host side |
| threads | goroutines, `sync`, timers | TamaGo's scheduler, 1..32 vCPUs | guest |
| `tuning.ae` (`GOGC`, the 64 MiB minimum heap, `ARBACE_CPUPROFILE` via `os.Create`) | the collector, profiles | works; a profile lands in the in-memory fs (or `box/put`) | guest |

The doorbell's calls Arbace would not use: `HostInit`, `GetWinsize`, `TermStart`/`Stop`,
`TTYKeys`, `Suspend`, `Raise` (the editor's terminal; Arbace's REPL reads cooked lines, as
under the JVM), `WaitForInput`/`ReadInput` alone (superseded by `WaitRead`), `NowMs`/`Delay`
(the board's clock and Go's timers do it in the guest). It would use `Exit`, `Message`, `Write`,
`WaitRead`, `Random`, `Fault`, SMP's four and, from S3, the store's five: **12 to 14 of 29**,
plus one new clock call.

So the box's surface for Arbace is about 6 host functions (console in, console out, error
out, exit, entropy, wall clock) against jrt's 20 + about 40 optional (and `HostExecutable`'s one since); what is between is
answered in the guest or absent.

## 5. arbace-go's features in the box

| area | namespaces / JDK | in the box | needs |
|---|---|---|---|
| reader, evaluator, core, collections, seqs, transients, protocols, multimethods, `deftype`/`reify`/`proxy` over `Dyn` | `arbace.core`, `arbace.lang` | works | nothing |
| numbers (`BigInteger`, `BigDecimal`, `Math`, fdlibm) | `java.math`, jrt `math.ae` | works | nothing |
| strings, `StringBuilder`, `Character`, `Formatter`, charsets | `java.lang`, jrt | works | nothing |
| regex (the `java.util.regex` port), its resources | `java.util.regex` | works (resources embedded) | nothing |
| `java.util` (collections, `Random`, `UUID`, `BitSet`, `TreeMap`), `arbace.set`, `walk`, `zip`, `data`, `edn`, `pprint`, `spec`, `test`, `test.check` | | works | `UUID.randomUUID`: `Random` |
| the image of prepared namespaces | `cmd/arbace/image.ae` | works (embedded; architecture-independent, as arm64 reuses amd64's) | nothing |
| class forms at the REPL (`defclass`) | `arbace.classes` | works (interpreted, CLASSFORMS-REPL.md) | nothing |
| concurrency: futures, agents, atoms, refs, `pmap`, fork-join | `java.util.concurrent`, jrt `thread.ae`, `executor.ae`, `forkjoin.ae` | works (§9: `@(future ...)`), but **cooperatively**: no preemption (§2.7); on 1 vCPU nothing else runs while the REPL waits for a line (§9.6), on several a background `println` waits for it | an evaluator yield (R2), a bounded console read (R3) |
| timed waits, `Thread/sleep`, `deref` with timeout | Go timers | works (board idle spins or parks) | nothing |
| `System/nanoTime` | | works | nothing |
| `System/currentTimeMillis`, `Date`, `Instant/now`, `#inst` now | jrt `system.ae`, `date.ae` | `currentTimeMillis` through the host works (§9); `new Date()` reads `time.Now()` directly and says 1970 | the board's clock offset (D10) |
| time zones | `TimeZone`, tzdb (`lib/tzdb.dat.z` embedded) | rules work; default zone from `TZ` | boot env |
| text, locales (`gotext`), security (`gosec`: digests, `SecureRandom`) | | works (data embedded; seeds from `Random`) | `Random` |
| standard streams, `*in*`/`*out*`/`*err*`, `read-line`, the REPL | `arbace.main`, `arbace.repl` | works over the console | existing calls |
| `-e`, `-m`, scripts by name | `arbace.main` | `-e`, `-m` work; a script by name needs a file: written into the in-memory fs first, or from the store (S3) | S3 |
| files (`java.io.File`, streams, `slurp`/`spit`, `arbace.java.io`, `load-file`, `ARBACE_PATH`) | jrt `files.ae`, `filesystem.ae` | emulated: TamaGo's in-memory fs, empty at start, lost at exit (§9: `spit`/`slurp` work); persisted through the store later | none (S1), store (S3) |
| sockets, `InetAddress`, the socket REPL (`arbace.core.server`, `prepl`), `URL` beyond `file:` | jrt `net.ae` | absent (no `NetHost`) | a network design (S5) |
| processes (`ProcessBuilder`, `arbace.java.shell`, `ProcessHandle`) | `goproc` | does not make sense: no other programs in the box; cut (absent `HostProc`); later perhaps "a process is another box" | none |
| rlwrap, terminal | `cmd/arbace` | host side: `rlwrap <launcher>` | launcher |
| signals, shutdown hooks | `Runtime.addShutdownHook` | hooks run at `System/exit`; a host signal kills the monitor | none |
| environment, system properties | jrt `system.ae`, `tooloptions.ae` | from the launcher's allow-list at entry | boot env |
| profiles, `runtime/debug`, `runtime/metrics` | `tuning.ae` | work; output to the in-memory fs or the store | none |

Classes:
- **No host surface (the large majority)**: everything above but the rows below.
- **A small, well-defined call**: console in/out/err, exit, entropy (all existing); the wall
  clock (new, or boot data); the store (existing, S3); the environment and argv (boot data).
- **Does not make sense boxed**: processes (cut), the terminal's raw mode and rlwrap (the
  host's), `HostFS`'s permissions and `HostLinks` (no host file system), JVM-style signals.
- **Makes sense, but a design first**: files beyond memory (the store as the file system?),
  the network (proxied streams or packets with netstack in the guest).

## 6. Staging

Each stage a gate, as B1a's steps.

| stage | content | gate |
|---|---|---|
| **S0 vendor** (after the freeze). *Done 2026-10-11 ([BOX-NOTES.md](BOX-NOTES.md), S0): copied unchanged as Go forms under `box/` (`arbace/box/{hv,vmm,abi,board,libsys}`), the dispatch left to S1 (D3's second step)* | `hv/`, `vmm/` (dispatch made a call table), `guest/abi`, the board, `internal/libsys` into Arbace as Go forms (g2a convert, round trip), or as a pinned checkout (D3, §10); `LICENSE.md`, `doc/ARBACE.md`, journal | go-whim's `hv` and `vmm` tests pass from Arbace's copy; a hello TamaGo image on the vendored board and monitor runs on /dev/kvm |
| **S1 the boxed REPL, amd64**. *Done 2026-10-11 ([BOX-NOTES.md](BOX-NOTES.md), S1): vmm's own host interface, the launcher `arbace-box`, jrt's box host, the wall clock at entry, the evaluator's yield, the console polled; checks: the box's smoke test, the oracle's subset (`known-go-box-amd64.edn`), Clojure's suite in the box (`bin/box-suite`, `test/arbace-box-results.edn`)* | jrt's `BoxHost` (console via `WaitRead`/`Write`/`Message`, `Exit`, `Random`, the wall clock, args, env, resources; no `NetHost`, `HostProc`, `HostFS`, `HostLinks`); the main package's box variant (no rlwrap, the board imported); the launcher `bin/arbace-box` (image appended, rlwrap around it on a tty, an env allow-list); the evaluator's periodic yield (R2); files in TamaGo's in-memory fs (OSHost's file calls, unchanged; its `RandomBytes` is Linux-only since `gosec`, the box host's own); four vCPUs, the console read bounded (R3) | `-e`, `-m`, the REPL on stdin answer as the native executable on the smoke test's forms; the oracle's box subset; Clojure's suite in the box, its known exclusions recorded (`test/arbace-box-results.edn`) |
| **S2 measured and hardened**. *Done 2026-10-11 ([BOX-NOTES.md](BOX-NOTES.md), S2): the image mapped (BX11), the symbol table cut to the monitor's three symbols, a default memory limit, the watchdog kept off, the suite's file tests and reducers in the box, `fold` 3.2-3.4x on 4 vCPUs; the times against native in BOX-NOTES* | the start (63 `Random` exits batched, the 85 MB image's DWARF: the box image keeps its symbol table, which the monitor reads, §9.4, while the native executables are stripped since SZ1; `-w` alone could drop the DWARF), the collector's setting in the box, the watchdog's setting for long evaluations, the suite's file tests over the in-memory fs, `ARBACE_PATH` inside it | the suite's file tests pass; reducers' `fold` scales on 4 vCPUs; start and suite times recorded against native |
| **S3 the store** | an `arbace.box` namespace (`put`, `get`, `ref`, `ref!` as Joker's `box`); scripts and namespaces loaded from refs; optionally the in-memory fs seeded from / saved to a ref (a tar or EDN snapshot) | a script run from a ref; `load` from the store; the store's refusals |
| **S4 arm64** | the same image for arm64; KVM/arm64 in the emulated aarch64 VM here; Hypervisor.framework on the Mac (the user's run, go-whim's `mac.sh` as the model) | the S1 gate in the aarch64 VM and on the Mac |
| **S5 the network** | the design first (R5): a stream proxy (`NetHost` over calls: `Dial`/`Listen` under the launcher's policy) or packets (a datagram call, netstack and TLS in the guest, go-whim's sketch); the socket REPL from the host | `java.net`'s oracle forms; a socket REPL session from the host |
| **S6 the image's Go as forms** | the board, TamaGo's runtime and std in the image as round-tripped Go forms; the image byte-identical to one built from the originals (B1-PLAN.md, B1b) | byte identity, amd64 and arm64 |

Not staged: processes (cut for good in the box), the terminal's raw mode.

## 7. The build: one source, two programs

- **What differs.** Only the host side: (a) jrt's host for the box, a new file
  `go/arbace/jrt/hostbox.ae` with `//go:build tamago`, over the board's `Call` (or a small
  interface the board package fills, to keep jrt free of go-whim's import path); (b) the main
  package's `SetHost` choice, behind build tags (`main_tamago.go` sets `BoxHost`, imports the
  board); `rlwrap_other.ae` is already the non-Linux case; (c) `hostfs_linux.ae` is already
  `//go:build linux`; (d) `net.ae` reaches `syscall` directly for socket options and
  `TIOCINQ`: the only part of the program that does not build for `tamago` (§9.2); it moves
  to a `//go:build linux` file with a stub beside it (`goproc`'s `hostproc_linux.ae` is
  already tagged).
- **The closure stays the same at first.** Cutting by build configuration (no `NetHost`, no
  `HostProc` on `tamago`) leaves a2g's program and its type table (`image_types.go`) identical,
  so the image prepared by the linux/amd64 executable is valid in the box, as it is for
  arm64 today (EXEC-NOTES.md, "Determinism"). A slimmer closure (no `java.net`, no processes:
  a smaller image) is a second a2g program with its own image, prepared by its own linux
  build: later, and only if the size matters (58 MB executable at the survey, 66.4 MB on 2026-10-11; go-whim's slot is 3 GiB).
- **The tools.** `bin/g2a build` takes `--goos tamago` (it fixes `GOOS=linux` today:
  `arbace.g2a.build/go-env`, `bin/arbace-go`'s `go_env`) and the go.mod's `require
  github.com/usbarmory/tamago v1.27.1` with `GOOSPKG` (from the module cache, `GOPROXY=off`);
  the ldflags `-T 0x201000 -R 0x1000`. `bin/arbace-go --build` gains the box flavour
  (`ARBACE_GO_TARGETS=linux/amd64,linux/arm64,tamago/amd64,tamago/arm64`), sharing a2g's
  output, the overlay (the patched `proc.go`/`runtime2.go` are TamaGo's own files, converted
  from `/root/tamago-go`, so they carry the `GOOS == "tamago"` branches already) and the
  image. `bin/jrt-convert` is unchanged. The monitor is a separate, ordinary `linux` (or
  `darwin`) Go program: `bin/arbace-box --build` builds it with the image appended.
- **The board** is the guest's main package's import; its `go:linkname`s define the
  `runtime/goos` hooks. With `GOOSPKG` unset, the same `GOOS=tamago` program links against
  TamaGo's `linux_user` goos instead: a Linux process with the box's runtime (§8).

## 8. Checks

- **/dev/kvm here**: usable (§9). The box runs as an ordinary process of this machine, so
  the smoke test, the oracle and the suite can run it as they run the native executable (the
  executable's path swapped for the launcher's), under `flock .tmp/heavy.lock`.
- **The tamago flavour without KVM**: the same program with TamaGo's `linux_user` goos runs
  as a Linux process and starts (§9.3), but TamaGo gives it no argv and no stdin; with a small
  host of its own over raw `read(0)` it would be a check of `GOOS=tamago` without a monitor,
  arm64 included (`qemu-aarch64`). The box itself is cheap enough here (0.4 s a start) that
  this is optional.
- **The oracle's box subset**: `bin/oracle check --expected test/oracle/known-box-amd64.edn`
  with the categories that need files beyond memory, sockets, processes and the host's zone
  marked as expected differences (as `known-go-amd64.edn` is for the native build).
- **Clojure's suite in the box**: a result file of its own (`test/arbace-box-results.edn`);
  the socket tests excluded; the file tests run over the in-memory fs (made to pass in S2).
- **arm64**: images cross-compile here (`GOARCH=arm64`); the `linux_user` flavour under
  `qemu-aarch64`; the real box under KVM/arm64 inside `qemu-system-aarch64 -machine
  virt,virtualization=on -cpu max` (go-whim's `guest/vm/aarch64.sh`: Alpine Edge netbooted, a
  job agent; slow: the Go guest's quick suite took 938 s there); Hypervisor.framework only on
  the Mac, by the user (go-whim's `guest/mac/mac.sh` checklist; `GOOS=darwin GOARCH=arm64
  CGO_ENABLED=0` compiles the monitor here; `codesign` with the hypervisor entitlement there;
  the image beside the launcher, not appended).
- **The gate**: S1's smoke test in `bin/gate --full` beside the Go chain; the box's suite
  and oracle there from S2.

## 9. Experiments here (`.tmp/boxsurvey/`, 2026-10-10)

All on copies: go-whim cloned to `.tmp/boxsurvey/go-whim` (at `80c49bf`), arbace-go's printed
module (`target/arbace-go/work-amd64/mod`, main `105db68`'s build) copied to
`.tmp/boxsurvey/mod`, jrt's runtime overlay (`.tmp/jrt-go/overlay`) copied. Builds under
`flock .tmp/heavy.lock`.

1. **KVM works here.** `/dev/kvm` is `crw-rw-rw- root kvm`; the CPU an AMD EPYC 7763 (`svm`),
   nested. go-whim's `go test ./hv/ ./vmm/` pass from the clone (`TestDoorbellAMD64`,
   `TestVCPUsExitAMD64`, `TestSysRegsAMD64`, `TestVCPUsAMD64`: real VMs), and with
   `TAMAGO_ROOT=/root/tamago-go` its guest tests too (`TestHello`, the C guest;
   `TestGoGuest`, `TestGoGuestFault`, the TamaGo guest): 9.1 s.
2. **arbace-go compiles for `GOOS=tamago` but for one file.** `go build ./...` of the printed
   module with `GOOS=tamago`, amd64 and arm64, fails only in `jrt/net.go` (from
   `go/arbace/jrt/net.ae`): `syscall.IP_TOS`, `SO_OOBINLINE`, `SYS_GETSOCKOPT`, `SYS_IOCTL`,
   `TIOCINQ` undefined; with those replaced by numbers, the link fails on `syscall.Syscall6`
   (and `Syscall`), declared but not defined on tamago. Stubbed in the copy (an `ENOSYS`
   function), everything compiles and links: the whole of `arbace.lang`, the JDK closure, jrt
   (its other host files are already fine: `hostfs_linux` and `rlwrap_linux` are tagged
   `linux`), the image, the patched runtime overlay. So the code change for the box's build is
   moving `net.ae`'s socket options and `Available` behind `//go:build linux` (or `!tamago`).
3. **The `linux_user` flavour** (`GOOS=tamago`, no `GOOSPKG`; 61.7 MB amd64, 59.6 MB arm64)
   starts as a Linux process: the image replayed, `arbace.main` loaded, the banner printed.
   It is no test vehicle as is: TamaGo's `linux_user` passes no argv (`-e` was not seen: the
   REPL started) and stdin is closed (every read an `IOException`; the REPL's error loop then
   exhausted its fixed 512 MiB). A box host over raw `read(0)` and the argv would be needed.
4. **Arbace in the box on KVM.** A scratch main (`.tmp/boxsurvey/mod-box/cmd/arbace/main_box.go`,
   about 80 lines) sets `jrt.SetHost` to a host embedding `jrt.OSHost` (files: TamaGo's
   in-memory fs; since `gosec` `OSHost`'s `RandomBytes` is in a Linux-tagged file, so a box host
   must define it, as this one does) with `Stdin` over `WaitRead(-1)`, `Stdout` over `Write`, `Stderr` over
   `Message(err)`, `Args` from `board.Args()`, `RandomBytes` over `Random`, `Exit` over
   `board.Exit`, `Now` as `Time` (seconds) at entry plus Go's clock, an empty environment;
   imports go-whim's board; built with TamaGo (`GOOSPKG=github.com/usbarmory/tamago`,
   `-ldflags "-T 0x201000 -R 0x1000"`; 85 MB amd64, 82 MB arm64: the monitor reads the
   image's symbol table, so `-s` cannot be used; 4 min under contention) and run by
   go-whim's unmodified launcher (`vmm/cmd/whim-guest`, 3.7 MB, `WHIM_GUEST_IMAGE=`):

   ```
   Clojure 1.13.0-master-SNAPSHOT
   user=> 3 (\c \b \a) bbb                      ; (+ 1 2), reverse, re-find
   user=> 499999500000                          ; (reduce + (range 1000000))
   user=> #inst "1970-01-01T00:00:02.257-00:00" 1791660833219   ; (java.util.Date.), currentTimeMillis
   user=> in memory                             ; spit, slurp of /tmp/x.txt
   user=> 15 42                                 ; (count (all-ns)), @(future (+ 40 2))
   exit 0
   exits 90 ... calls: exit 1, write 22, time 1, random 63, wait_read 3
   ```

   No change to go-whim, no new call. Findings: `new Date()` reads Go's `time.Now()` directly
   (`jrt/date.ae:66`), not the host, so it says 1970 (TamaGo's `walltime` is `nanotime`):
   the board's clock must carry the wall-clock offset (D10). The runtime's seeds cost 63
   `Random` exits at start (one per request: batch them).
5. **Measured** (amd64, nested KVM, best of 3): start with `-e nil` 0.38-0.42 s in the box at
   1 or 4 vCPUs, 445 MB resident (the monitor's process, the slot touched), against 0.20-0.21 s
   and 160 MB native; `(time (reduce + (map inc (range 1000000))))` 1,396-1,401 ms in the box
   on 1 vCPU, 961-1,034 on 4, native 984-1,178 at `GOMAXPROCS=1` and 891-1,029 on 64 cores.
6. **R3 confirmed.** `(future (Thread/sleep 300) (println "late"))`, then the REPL waits 3 s
   for its next line: natively "late" is printed at +0.3 s; in the box on 1 vCPU it is never
   printed (the vCPU stops in `WaitRead`; the future's goroutine was never run before EOF
   ended the guest); on 4 vCPUs it is printed 2.6 s late, when the read returns (time-stamped
   on the host: the monitor serializes Host calls, so the future's `Write` waited for the read).

7. **Cross-compiled**: the arm64 box image (82 MB, above) and go-whim's monitor for
   `darwin/arm64` (Mach-O, 3.9 MB, `CGO_ENABLED=0`: purego) and `linux/arm64` (3.6 MB).

Not run: the arm64 images under KVM/arm64 (the aarch64 VM is go-whim's `guest/vm/aarch64.sh`,
an Alpine netboot: left for B1b), the Mac.

## 10. Risks, open questions, recommendations

Decisions for the user (after the freeze). **Decided by the user 2026-10-11, all as recommended**: D3 (a)
vendored as Go forms, copied unchanged first, then the dispatch made generic; D8 EPL-1.0 for the
vendored parts, stated in LICENSE.md; D9 S1 first; D10 the host's time at entry in a register,
the board's clock offset by it; D11 memory, then the store; D12 at S5; D13 processes cut for
good in the box; D14 the monitor stays a Go program on the host.

- **D3 How Arbace depends on go-whim.** (a) vendor `hv/`, `vmm/`, `guest/abi`, the board and
  `internal/libsys` as Go forms under Arbace (g2a convert; the round trip proves them), the
  dispatch made generic, the origin recorded; (b) a checkout pinned by commit, built as Go;
  (c) a Go module dependency on `github.com/arbace/go-whim`. *Recommended: (a)*, on the
  CLAUDE.md line (Arbace in Arbace down to the metal; vendoring allowed), in two steps: first
  copy and convert unchanged (byte-identical build), then the generic dispatch as an Arbace
  change. go-whim keeps its own copy; nothing flows back automatically.
- **D8 The licence of the vendored box.** The user's own code under Vim's licence by
  repository default: *recommended*: the user states in Arbace's `LICENSE.md` that the
  vendored go-whim parts are EPL-1.0 (or dual), since they are not vim-derived.
- **D9 The first target.** *Recommended: S1* as stated: the REPL, `-e`, `-m`; the console
  as `WaitRead`/`Write`/`Message`; files in memory (free: Go's `os` on TamaGo); no network, no
  processes; four vCPUs by default (one vCPU is S1's worst case, §9.6), one kept in the tests.
- **D10 The wall clock.** A new call `WallTime` (ns), or the host's ns at entry in a
  register with the board's `Nanotime` offset by it (then `time.Now()` is right and
  monotonic, at the TSC's accuracy). *Recommended: the register* (no exit; go-whim's
  `Time` stays for the editor), plus the call later if drift matters.
- **D11 Files in the box.** In-memory only (S1), the store as the persistence (S3), or a
  host directory proxied by calls (a 9p-like surface: largest, and against the box's
  point). *Recommended: memory (S1), then the store (S3)*; no host file system.
- **D12 The network.** *Recommended: decide at S5*, between a stream proxy (small, jrt's
  `NetHost` shape as calls, the launcher's policy) and packets with netstack and TLS in the
  guest (go-whim's sketch: the host sees nothing). The socket REPL is the first user either
  way.
- **D13 Processes.** *Recommended: cut for good in the box* (absent `HostProc`); revisit only
  as "start another box".
- **D14 The monitor's language.** The monitor is a Linux/macOS Go program, not part of the
  box: it can stay Go (vendored as Go forms, built by gc) for a long time. *Recommended*: yes;
  the box's guest side is where Arbace-down-to-the-metal applies.

Risks:

- **R1 Size and memory.** The executable is 58-62 MB (66.4 MB on 2026-10-11, stripped, the default locale data); the slot 3 GiB (`GoRAMBytes`, under the
  doorbell at 3.75 GiB, a 32-bit space on arm64). The suite's peak heap in the box is
  unmeasured; the minimum heap (64 MiB) and `GOGC=200` stay. Raising the slot means moving
  the doorbell (an ABI change).
- **R2 No preemption.** No `sysmon` on TamaGo: on one vCPU a goroutine runs until it blocks;
  a long evaluation starves futures, agents' sends, timers' callbacks, and stretches the
  collector (go-whim's `fib 27` 4x, `798200a`). Remedy as Joker's: the evaluator yields
  (`runtime.Gosched`) every N calls (`recur`, `invoke`), cheap on Linux too. A tight Go loop
  in jrt (a large `Arrays.sort`) still cannot be preempted; the watchdog must be off or long
  for the REPL (it ends a guest silent for 60 s: a computation without output is "silent").
- **R3 A blocking console read stops the guest** (measured, §9.6). A vCPU in `WaitRead(-1)`
  is stopped in the monitor; on one vCPU nothing else runs while the REPL waits for a line, and with several
  the monitor serializes Host calls, so another vCPU's `Write` waits for the read too.
  Remedy: the guest reads with a bound (`WaitRead(ms)` in a loop, idling between), or the
  monitor stops serializing the console's read against writes. Measure exits while idle.
- **R4 TamaGo's std differs.** `time.Local` is UTC, `walltime` starts at 1970, `os` is in
  memory, `net` needs `SocketFunc`, stdin is closed, stdout/stderr go through `Printk` (the
  board's: to `Message`, stderr). jrt must not reach `os.Stdout`/`os.Stderr` or `time.Now()`
  for the wall clock directly (`throwable.ae` uses `os/Stderr` once, `date.ae` `time/Now`):
  through the host only, or the board's clock carries the offset.
- **R5 Go-version coupling.** TamaGo is pinned at go1.27.1, as Arbace's toolchain is; a Go
  upgrade waits for TamaGo's release.
- **R6 Image and closure.** The image is valid in the box only while the a2g program is
  the same; a different closure needs its own preparation (§7).
- **R7 KVM here is nested** (16-18 µs an exit): the box's start and per-line costs measured
  here are worst cases; the Mac's numbers are the realistic ones.
- **R8 Signals.** A REPL's Ctrl-C ends the monitor (as the JVM ends), but the tty mode is the
  host's (cooked): nothing to restore. Should the box ever want an interrupt key (stop an
  evaluation), it needs the `event` path and a guest-side handler: later.

## 11. What can be prepared before the freeze

Nothing in code: the freeze's gate must not move, and every code change above touches jrt,
the main package or the build. What could be done without disturbing it:

- this survey;
- keep `go:build`-cleanliness in new jrt work: OS calls (`syscall`, `os.Stdout`) only in
  files tagged `linux`, so that the `tamago` compile stays one file away (§9.2); `net.ae`'s
  split itself is a small change, but in jrt: after the freeze;
- a `LICENSE.md` paragraph and the journal entry for the vendored parts, written when D3
  and D8 are decided (after the freeze).

## 12. Sources

- go-whim (`/root/go-whim`, head `80c49bf`): `hv/*.go`; `vmm/*.go`, `vmm/cmd/whim-guest/main.go`;
  `guest/abi/abi.go`; `guest/tamago/{board/*,main.go,go.mod}`; `guest/gotamago.go`;
  `guest/joker.go`, `guest/joker/{main.go,go.mod}`; `internal/libsys/libsys.go`;
  `editor/host.go`; `doc/GUEST.md`, `doc/LISP-SANDBOX.md`, `CLAUDE.md` (layout), `README.md`
  ("License"), `LICENSE`.
- TamaGo: `/root/tamago-go` (go1.27.1): `src/runtime/os_tamago.go`, `src/runtime/proc.go`,
  `src/runtime/goos/{stub.go,linux_user.go,README.md}`, `src/syscall/{fs,fd,net}_tamago.go`,
  `src/net/net_tamago.go`, `src/time/zoneinfo_tamago.go`, `LICENSE`; module
  `github.com/usbarmory/tamago@v1.27.1` (`goos/goos.go`, `user/linux`, `LICENSE`).
- purego `github.com/ebitengine/purego@v0.11.1` (`LICENSE`).
- Arbace: `go/arbace/jrt/{host,hostfs,hostfs_linux,net,timezone,tuning,throwable}.ae`,
  `go/arbace/cmd/arbace/{rlwrap_linux,rlwrap_other,image}.ae`, branch `goproc`'s
  `go/arbace/jrt/hostproc.ae`; `bin/arbace-go`, `bin/jrt`; `doc/go/{B1-PLAN,A2G-SPEC,EXEC-NOTES,JAVA-BASE}.md`.
