# The kernel and processes in the box: the process model

> The name. The survey ([KERNEL-USER-SURVEY.md](KERNEL-USER-SURVEY.md) §6.2, the KU0 row) calls
> this document `doc/go/PROCESS-SPEC.md`, and it keeps that name: the **process** is the one new
> thing the KU series builds, named so throughout the code (`arbace/box/kernel`, `arbace/box/uboard`,
> the namespace `arbace.box`'s spawn), and the kernel is the box's program in a mode this document
> also fixes. `KERNEL-SPEC.md` was considered and set aside: the kernel is the box as it is today
> plus a process manager, while the process is what is specified anew.

Status: draft for the user's review, 2026-10-17: milestone KU0 of the box's kernel/user split
([KERNEL-USER-SURVEY.md](KERNEL-USER-SURVEY.md) §6.2), written at the user's decision of
2026-10-17. It is bound by the survey's proposals K1-K12, all decided as recommended on 2026-10-17
(the survey's "Decided" section); the choices made while writing it are S1-S40 (§11.1), the
questions for the user KS1-KS12 (§11.2), each with the recommendation this text follows. The
experiments that validate its key choices (Appendix A) were run with TamaGo's go1.27.1
(`/root/tamago-go`) on amd64, the machine shared, a guest on /dev/kvm; their programs are in
`.tmp/ku0-spec/` (not tracked). KU1-KU6 (§10) come after the survey's decision and change only
`box/` until KU6.

This spec defines how the box runs **processes**: a second TamaGo runtime at EL0/ring 3, with its
own heap, collector and scheduler, in its own address space, isolated from the **kernel** (the
box's program at EL1/ring 0) and from other processes by the MMU, and talking to the kernel by
system calls that are the doorbell's call block one level down. It is the counterpart for the box
of what the monitor and the doorbell are for a guest ([BOX-NOTES.md](BOX-NOTES.md), the box's
`abi`): the boundary moves inward, from guest/monitor to process/kernel, with the same ABI. It
follows [GOI-SPEC.md](GOI-SPEC.md) and [SPEC.md](SPEC.md) in structure and principles, and
[classes/SPEC.md](../classes/SPEC.md) before them.

Words used throughout:

- **the kernel**: the box's program running at EL1 (arm64) or ring 0 (amd64), the only code the
  monitor trusts. Its parts are the TamaGo runtime, the board (`arbace/box/board`), the drivers
  (the doorbell's clients: console, store, network, spawn, events), the **process manager** and
  the **call handler** this document adds. The compilers and the REPL are not in it (§1.3, S5).
- **a process**: a whole TamaGo runtime instance at EL0/ring 3, its own heap, collector,
  scheduler, stacks and timers, in an address space of its own, running one program's code. It is
  to the kernel what a tool guest is to the monitor ([BOX-NOTES.md](BOX-NOTES.md), S7): untrusted,
  budgeted, ended on a fault, reachable only through a narrow ABI.
- **the user board** (`arbace/box/uboard`): the TamaGo board a process runs on, the counterpart of
  `arbace/box/board` for a guest. As built (KU1, KU2): not a package of its own but the process
  mode of `arbace/box/board`, one two-mode board (the goos hooks link once per image). It answers the runtime's goos hooks (§3.4) by system calls
  instead of the doorbell, without patching Go's runtime (S1).
- **a crossing**: a transition between levels — a process's system call (ring 3 → 0 or EL0 → EL1
  and back) or a fault taken into the kernel. Measured in Appendix A: 24-31 ns for SYSCALL+SYSRET,
  110 ns for INT+IRETQ, plus up to 146 ns when the address space changes without PCID/ASID.
- **a handle**: a small non-negative integer by which a process names a kernel object it was given
  (a console stream, a store view, a spawn, a network connection), the only way a process names
  anything outside its own memory (§5). Handles are where capabilities enter (K12, later).
- **the call block**: the 64-byte block the doorbell already uses (`arbace/box/abi`: `nr`, `a[5]`,
  `ret`, `event`), reused unchanged as the system-call block (§4).
- **the monitor**: the box's VMM on the host (`arbace/box/vmm`), under KVM or Hypervisor.framework.
  It is below the kernel and unchanged by this document (S2): EL0/ring 3 is the guest's own and
  needs nothing of it (Appendix A; the survey §6.1).

Contents: 1 Scope and principles · 2 The address space · 3 Entering and leaving a process ·
4 The system-call ABI · 5 Handles and values across · 6 The process manager · 7 Scheduling ·
8 Faults and errors · 9 The check · 10 Milestones · 11 Decisions and open questions · 12 Sources ·
Appendix A: the experiments.

## 1. Scope and principles

### 1.1 Scope

In scope: a process as a second TamaGo runtime at EL0/ring 3 on amd64 (KU1, KU2) and arm64 (KU3);
its address space (b1: the same image, a page table per process, text and read-only data shared);
the user board; entering and leaving a process (trap frames, SYSCALL/SYSRET, SVC/ERET, faults);
the system-call ABI (the doorbell's block, the calls a process needs); handles and value-passing
by bytes; the kernel's process manager (spawn, wait, kill) and call handler; scheduling (vCPUs per
process first, KU6's timer preemption); faults (a nil dereference the process's own panic, others
end the process); S7's tool images run as processes (KU4). The box runs on TamaGo under KVM and
Hypervisor.framework; the kernel is the box's own program (SPEC.md, the Go build) in a mode this
document fixes.

Out of scope, each recorded where it belongs: a shared heap or shared pointers across the boundary
(K4, never; §5.4 keeps shared pages as a later option KS6); privilege per goroutine over one heap
(the survey's option (a), rejected, K1); Arbace as a hypervisor on bare metal (K11, Later); the
REPL as a process and the capability evaluator inside it (KU7 and K12, later, §10); SMP within the
kernel beyond what the box has today; the doorbell's semantics and the monitor's code (unchanged,
S2); the language-level sandbox inside a process (the survey's option (c), KU7). Side channels
(Spectre, Meltdown) are out of scope at first (§8.6, the survey §6.1).

### 1.2 Principles

1. **A process is a guest one level down.** Everything the box already does to isolate a tool
   guest under the monitor — a separate address space, a budget, a narrow call ABI, values as
   bytes in the store, a result on exit, an end on a fault — the kernel does to isolate a process
   under itself. The mechanisms differ (a page table and SYSCALL in place of a VM and a VM exit),
   the model does not. This is the survey's main result (§2.4, K6): the boundary is already an ABI;
   it moves inward.
2. **The hardware boundary isolates; the kernel is trusted.** A process cannot read or write the
   kernel's memory or another process's, cannot execute a privileged instruction, and names
   nothing outside its own memory but through a handle the kernel gave it. The kernel validates
   every system call against the process's memory and policy (§4.3). The monitor still trusts the
   kernel, as it trusts the guest today.
3. **Each runtime manages its own memory.** The kernel's collector scans only the kernel's heap; a
   process's collector scans only the process's. No pointer crosses the boundary, so no collector
   follows a pointer the other side wrote (the survey §3.3's decisive point, K4). Values cross as
   bytes and handles (§5).
4. **No patch to Go's runtime.** A process is `GOOS=tamago` on the user board, as a guest is on
   the board. The user board answers the goos hooks (§3.4) with system calls, as `arbace/box/board`
   answers them with the doorbell and TamaGo's own `user/linux` answers them with Linux calls. Go's
   runtime (`overlay/go`, TamaGo's `/root/tamago-go`) is unchanged by the KU series until KU6, and
   KU6 touches only the board's interrupt policy, not the runtime (S1, §7, the survey §5.4, §6.2).
5. **The same image is the kernel and the process.** The box's program, linked once at `0x201000`
   (BOX-NOTES.md), runs as the kernel when the monitor boots it at its ELF entry and as a process
   when the kernel enters it at `whim_uentry`; which it is, it learns at that first instruction
   (S3, §3.3, b1/K3). One build serves both, as Appendix A confirms it links.
6. **gc, the doorbell's ABI and the box as they are define correctness.** A process's output is
   compared byte for byte against the same image run as a guest (§9, KU2's exit criterion); S7's
   tools run as processes give the same artifacts as the tool guests (KU4). The box's smoke test
   and oracle are the gate.

### 1.3 The kernel, a process, what lives where

The survey's §1.2 table, as decided (K5):

| part | kernel (EL1/ring 0) | a process (EL0/ring 3) |
|---|---|---|
| exception vectors, page tables, the crossings' entry | yes | — |
| the TamaGo runtime (collector, scheduler) | the kernel's own heap | the process's own, separate |
| the drivers: the doorbell's clients (console, store, network, events, spawn under the monitor) | yes | reached by a system call to the kernel |
| the process manager: load, map, schedule, budget, wait, kill | yes | — |
| the call handler: validate, apply policy, answer or forward to the monitor | yes | — |
| the compilers (c2j, j2g, gc's `compile`/`link`/`asm`) | no | yes |
| the evaluator, the Java and Go forms interpreters, the REPL | no | yes (KU7) |

A process holds untrusted or large or crash-prone work: a compile (gc reads untrusted input and is
not Arbace), an evaluation of a form from the network, a tool run. The kernel holds the trusted
core that owns the devices and the store. The compilers are applications, isolated as S7 already
isolates them in guests (the survey §1.2, §3.3; this is why K5 puts "the compiler" that the
original note placed at EL1 into a process instead).

### 1.4 Relation to the box's existing isolation

The box has two isolations already (BOX-NOTES.md): a **guest per tool** (S7, under the monitor,
needs a hypervisor) and none in the language. The process is a third, between them: hardware
isolation that needs only an MMU and two privilege levels, which KVM and Hypervisor.framework give
the guest (ring 0 and ring 3, EL1 and EL0) without a nested hypervisor. One spawn request chooses
the back end (K6, §6.4): a guest where a hypervisor is there and the work is heavy or truly
untrusted (a whole build, native code), a process for the cheap and frequent cases (a single
compile, a REPL sandbox, many small actions). The language sandbox (KU7, K12) lives inside a
process, for the REPL's own fine-grained restriction, not as a boundary against non-Arbace code.

## 2. The address space

### 2.1 The model (b1): the same image, a page table per process

A process uses the same image as the kernel, at the same virtual addresses (the image is not
position independent: linked at `0x201000`, and two processes of it would need the same addresses
for different data). So each process has **its own page tables** (S6):

- the **text** and **read-only data** segments map the kernel's physical pages, shared, read-only,
  and **user-executable** (text) or user-readable (rodata). The image's own ELF segments already
  separate these: Appendix A shows the three LOAD segments `R E`, `R`, `RW`. One physical copy
  serves the kernel and every process.
- **data, bss, heap and stacks** map the process's own physical RAM, read-write at EL0, mapped at
  the kernel's virtual addresses for those segments but to private physical pages. The RW segment
  is copied into the process's RAM at load (its initialized part), the rest zeroed.
- the **kernel's mappings** (its own data, heap, stacks, the drivers' buffers, the monitor's
  system tables and the doorbell page) are present in every process's tables but **EL1-only /
  supervisor-only** (no U/S bit on amd64, AP=00 and UXN on arm64), so a process can neither read
  nor write nor execute them. A system call that returns to the same process needs no table
  switch, because the kernel's mappings are already in the process's tables (as Linux runs without
  KPTI; §3).

The process's RAM is one physically contiguous range mapped at a fixed virtual offset, so the
kernel translates a user pointer to a kernel pointer by one addition after a bounds check (GoTEE's
`dma.Region`; the survey §3.5). This is where the kernel reads a process's call buffers (§4.3).

The alternative (b2), a separate image linked elsewhere per process slot, is rejected (K3, S6): it
makes each process kind a second 100 MB build and bars two processes of one image.

### 2.2 amd64

The kernel's own state is as the board sets it today (BOX-NOTES.md, `vmm/setup_amd64.go`): 64-bit
ring 0, a GDT of DPL-0 segments and the TSS, an IDT of 32 gates into `whim_vectors`, 4-level
identity page tables with the U/S bit never set, SMEP and SMAP off. For processes the kernel
extends this (the monitor is unchanged; the kernel does all of it itself, once, at start-up —
Appendix A did exactly this from inside a guest):

- **the GDT** gains a user data and a user code segment at the positions SYSRET expects:
  `STAR[63:48]` the base B, with SS = B+8 and CS = B+16, both DPL 3, long mode. After the monitor's
  TSS at `0x18`, the user data segment is `0x30` (`0x00cff3000000ffff`) and the user code segment
  `0x38` (`0x00affb000000ffff`), with STAR's base `0x28` (Appendix A, the survey §2.3).
- **the TSS** gains `RSP0`, the kernel stack a crossing from ring 3 lands on. Per vCPU, as the
  fault stacks are (`tssStride` 0x80).
- **the IDT**: the 32 gates stay. A fault from ring 3 uses the same gates (the CPU switches to
  `RSP0` or the gate's IST); a process makes no `INT`, so no DPL-3 gate is needed (SYSCALL is the
  system call, §3). Page-fault and general-protection gates must leave the kernel able to tell a
  process fault from a kernel fault by the saved CS (§8).
- **the MSRs**: `EFER.SCE` on; `STAR` = B<<48 | kernelCS<<32 (kernel CS `0x08`); `LSTAR` the
  SYSCALL entry; `FMASK` masks IF, DF, TF (and AC) on entry.
- **CR4**: `SMEP` (bit 20) and `SMAP` (bit 21) on, so the kernel never executes a user page and
  never silently reads one (reads of user buffers are bracketed by STAC/CLAC, or go through the
  kernel alias of the process's RAM, §4.3). `FSGSBASE` (bit 16) may be on (the host has it,
  Appendix A): it lets the kernel save and restore a process's FS base cheaply (§3.5). `PCIDE`
  (bit 17) for PCIDs (§2.4). The host has `smep smap pku invpcid pcid fsgsbase` (Appendix A).
- **per-process tables**: the user's data/heap/stack pages get the U/S bit at every level; the
  kernel's keep it clear. `CR3` holds the process's root while it runs, the kernel's while the
  kernel runs; a crossing that changes process changes `CR3` (§3.5, §2.4).

The runtime keeps `g` in the FS-relative TLS on amd64 (`settls` writes `IA32_FS_BASE`, the survey
§2.3, `/root/tamago-go/.../sys_tamago_amd64.s`). A process has its own FS base for its own `g`; the
kernel saves and restores it across a crossing (`WRFSBASE`/`RDFSBASE` with FSGSBASE, else the MSR).
`settls` already branches on CPL (it does `arch_prctl` when CPL≠0), which the process path reuses
(S8, §3.5).

### 2.3 arm64

The kernel's own state is as `vmm/setup_arm64.go` sets it: EL1t (on SP_EL0, DAIF masked), MMU on,
3-level identity tables with AP never granting EL0 and UXN everywhere, TTBR1 disabled (EPD1),
VBAR_EL1 = `whim_vectors`. For processes the kernel adds:

- **the user's pages**: AP[1] set (AP=01 user read/write, 11 user read-only), UXN cleared on the
  user's text, PXN set on it (a page writable at EL0 is never executable at EL1 — the architecture's
  implicit rule, wanted here).
- **TTBR0/TTBR1**: a process's low addresses are its own translation. Two placements (KS1): (a) keep
  the single-TTBR0 scheme — the kernel switches TTBR0 to the process's tables on entry and back on
  exit, the kernel's own mappings present EL1-only in every table (the amd64 scheme's twin;
  *recommended*, symmetric with amd64, one mechanism); (b) use TTBR1 for the kernel (high half,
  enable it by clearing EPD1) and TTBR0 for the current process (low half), so a crossing that
  returns to the same process changes no table and a process switch changes only TTBR0 — the usual
  arm64 split, but it relinks the kernel's data into the high half and so is left for later.
- **CNTKCTL_EL1.EL0VCTEN** so a process reads `CNTVCT_EL0` for its `nanotime` without a crossing
  (the board reads the counter directly, §3.4). `CNTKCTL_EL1.EL0PCTEN` likewise if a process reads
  the physical counter.
- **PAN** (Privileged Access Never, ARMv8.1): set while the kernel runs so it does not touch user
  pages by accident; the kernel reads a process's buffers through the kernel alias of its RAM
  (§4.3), not through the user VA, so PAN stays on. The host and Apple's cores have PAN.
- **the vectors**: the "lower EL, AArch64" quarter of `whim_vectors` (offsets 0x400 synchronous,
  0x480 IRQ) handles a process's SVC (ESR_EL1.EC 0x15) and its aborts (EC 0x20 instruction, 0x24
  data). Today those entries report a fault and end the guest (`vmm/setup_arm64.go`, the survey
  §2.2); the kernel rewrites them to the process path (§3.3, §8).

The runtime keeps `g` in `TPIDR_EL0` on arm64 (`MRS TPIDR_EL0, R0`, `tls_arm64.h`, GOOS_tamago).
EL0 reads the same register, so the kernel saves and restores `TPIDR_EL0` across a crossing and
sets the process's own before ERET (§3.5). CPU feature detection reads EL1 ID registers at start
(`cpu_arm64_tamago.go`, `getisar0` etc.); a process must not run that at EL0 (it would trap). The
kernel's runtime has already run it; a process's runtime must skip it. TamaGo's `osInit` for arm64
does read them, so the user board overrides `osInit` to a no-op for a process, or the process's
runtime reuses the kernel's already-parsed `cpu` flags (S9, KS2): *recommended* — the user board's
`osInit` is empty and the process trusts the kernel's feature set, which is the same CPU.

### 2.4 PCID and ASID

Without PCID (amd64) or ASID (arm64) a `CR3` or `TTBR0` write flushes the TLB, measured at 145 ns
in the box (Appendix A, the survey §5.1). With them the switch keeps the TLB:

- amd64: `CR4.PCIDE` on, each process a PCID (1..4095, the kernel 0), `CR3` written with the PCID
  and bit 63 set to keep entries. `INVPCID` (the host has it) flushes one process's entries when
  its tables change (at load and at free). A call that returns to the same process writes no CR3 at
  all (§3.5), so PCID matters only at a process switch.
- arm64: the TCR already provides ASIDs; TTBR0 carries the ASID in its high bits. `TLBI ASIDE1`
  flushes one.

KU1 may start without PCID/ASID (the cost is one TLB flush per process switch, not per call) and
add them when a measurement asks (S7, the survey K3 "PCID and ASID later"). A call that does not
switch process pays nothing either way.

### 2.5 What the monitor and the board change

- **The monitor** (`arbace/box/vmm`): nothing (S2). EL0/ring 3 is the guest's own privilege to
  grant; the experiment changed nothing in the monitor (Appendix A, the survey §6.1). The monitor
  still sees only the kernel, its doorbell calls and its faults. A process's system calls never
  reach the monitor; a call the kernel forwards (console, store, network) reaches the monitor as
  the kernel's own doorbell call, as today.
- **The board** (`arbace/box/board`): the kernel's board. It gains the process-side set-up (§2.2,
  §2.3: the GDT/TSS/MSR additions on amd64, the AP/CNTKCTL/PAN/vector additions on arm64), the
  crossing entry and exit (§3), and at KU6 the timer (§7). Through KU5 it keeps interrupts off.
- **The user board** (`arbace/box/uboard`, new): a process's board (§3.4). It is `arbace/box/board`
  reduced to one that makes calls by SYSCALL/SVC instead of the doorbell, with no vectors of its own
  (faults go to the kernel's), no SMP set-up (the kernel starts a process's vCPUs, §7). Appendix A's
  `uboard` is a two-mode prototype: one board that is the kernel's at a guest's entry and a
  process's at `whim_uentry`, selected at the first instruction (S3).

## 3. Entering and leaving a process

### 3.1 The context and the trap frames

The kernel keeps a **context** per process vCPU (GoTEE's `ExecCtx`): the saved user registers, the
user SP, the user FS base / TPIDR_EL0, the process's page-table root (CR3/TTBR0 with its PCID/ASID),
and a pointer to the process. A kernel goroutine runs a process by `Exec(ctx)` (§3.2): it loads the
user state and drops to EL0/ring 3; the process runs until it makes a system call or faults; the
crossing saves the user state back into the context and returns into the Go code that called
`Exec`. So **a process is run by a kernel goroutine, as one long call**, and the collector scans
that goroutine in an ordinary call frame (the survey §3.4, §6.1 — this is why (b) has no traceback
problem that (a) had).

The crossing's kernel side is written like the board's vectors (the survey §2.3, gVisor's
`ring0`): NOSPLIT assembly with no Go stack of its own, running on the kernel stack (RSP0/IST on
amd64, SP_EL1 on arm64), that saves or restores the context and then returns into a Go function on
the kernel goroutine. `Exec` is bracketed by `entersyscall`/`exitsyscall` so the P is handed off
while the process runs (§7; gVisor and Go's own blocking-syscall convention).

### 3.2 amd64: SYSCALL/SYSRET, IRETQ

- **Into a process the first time**: `IRETQ` with a frame `(SS=userData|3, RSP=userStack,
  RFLAGS=2, CS=userCode|3, RIP=entry)`. The entry is `whim_uentry` (§3.3). Appendix A's `enterUser`
  does exactly this.
- **Into a process after a system call**: `SYSRETQ`, which restores RIP from RCX and RFLAGS from
  R11 and goes to ring 3. Go's assembler spells `SYSRET` without REX.W, so 64-bit `SYSRETQ` is
  written as the bytes `0x48 0x0f 0x07` (Appendix A, the survey §2.3 — a pitfall the experiment hit).
- **Out of a process, a system call**: `SYSCALL` (the process's `doorbell` shim does this when it
  is a process, Appendix A's `uboard`). SYSCALL puts the return RIP in RCX, RFLAGS in R11, and does
  **not** switch stacks — the LSTAR entry switches to the kernel stack itself (RSP0 from the TSS, or
  a per-vCPU kernel stack it loads) before touching a Go stack. It saves the user RSP first.
- **Out of a process, a fault**: any exception (page fault, #GP, #UD, ...) goes through the IDT; the
  CPU pushes SS:RSP and switches to RSP0 or the gate's IST. The kernel handler reads the saved CS to
  tell a ring-3 fault (process) from a ring-0 fault (kernel) (§8).
- **The SYSRET frame detail**: IRETQ to ring 3 nulls a data-segment register whose DPL is below 3.
  The process sets its own FS base for `g`; the kernel restores it after the segment load (Appendix
  A's `enterUserGo` loaded FS with the user selector and rewrote `IA32_FS_BASE`; a real process uses
  its own FS base, saved and restored by the kernel, §3.5).

### 3.3 The process's first instruction and its runtime's start

The process is entered at the ELF symbol **`whim_uentry`** (the user board's, §2.5), the twin of
`cpuinit` (a guest's entry) and `whim_apentry` (an AP's). At `whim_uentry` the process is at ring 3
with the entry registers the kernel set (the same register contract as a guest's `cpuinit`: argc,
argv, RamStart, RamSize, RamStackOffset, tickKHz, ncpu, wallBase — see `board/boot_amd64.s`). It:

- records that it is a process (so `doorbell` makes a SYSCALL, not a store; Appendix A's `uboard`
  sets a `process` flag — S3);
- does **not** touch CR0/CR4 (privileged; the kernel set them — a process's entry omits the SSE/FP
  enabling a guest's `cpuinit` does, because the kernel has done it and a process cannot);
- on arm64, does **not** read the EL1 ID registers (§2.3, S9): its `osInit` is empty;
- takes the entry registers, sets the stack, and jumps to `_rt0_tamago_start`, TamaGo's runtime
  start, unchanged.

**TamaGo's runtime at EL0, what needs changing (nothing in the runtime; only the board).** The
survey's §4.2 and the goos hooks (`/root/tamago-go/src/runtime/goos`) show the runtime asks its
board for exactly: `Hwinit0`/`Hwinit1`, `Nanotime`, `Printk`, `GetRandomData`, `InitRNG`, `Exit`,
`Idle`, and for SMP `Task`, `Wake`, `ProcID`. A process answers them from the user board by system
calls, as `arbace/box/board` answers them by the doorbell and TamaGo's `user/linux` answers them by
Linux calls (S1, §3.4). Four points of care, all in the board, none in the runtime:

- **`settls` / TLS**: amd64 `settls` already branches on CPL and does not execute `WRMSR` at CPL 3
  (it would `#GP`); it does `arch_prctl` instead (`sys_tamago_amd64.s`). For a process the kernel
  sets the FS base (there is no `arch_prctl`), so the user board's set-up establishes `g` before
  `settls` runs, or `settls`'s application path becomes a `SetFSBase` system call (KS3). arm64 keeps
  `g` in `TPIDR_EL0`, writable at EL0, so no change.
- **memory**: the runtime grows its heap by `sbrk` within `[RamStart, RamStart+RamSize)`
  (`mem_tamago.go`); it never asks the OS for more. The kernel gives the process its RAM window up
  front, and the process's `sbrk` stays inside it, faulting into the kernel (which maps a page) or
  calling a `Grow` system call (§4.4, KS4). No `mmap` (unlike `user/linux`'s `cpuinit`, which mmaps
  under Linux — a process's RAM is the kernel's gift, §2.1).
- **time**: `Nanotime` reads the counter directly (RDTSC on amd64, CNTVCT_EL0 on arm64 with
  EL0VCTEN) — no crossing (§3.4). The wall-clock base comes in an entry register.
- **exit and idle**: `Exit` and `Idle` become system calls (§4). `Idle` with no timer (through KU5)
  is a `Yield`/`Park` call (§7).

### 3.4 The user board's hooks

`arbace/box/uboard` (as built: `arbace/box/board` in its process mode, KU1, KU2) implements the goos hooks for a process (the survey §4.2's "the whole board a
TamaGo runtime needs at ring 3"):

| hook | a guest (`board`) | a process (`uboard`) |
|---|---|---|
| `Nanotime` | RDTSC / CNTVCT_EL0 scaled | the same, read at EL0 — no crossing |
| `Printk` | `abi.Message` by the doorbell | `Write` on handle 2 (stderr) by SYSCALL/SVC (§4) |
| `GetRandomData` | `abi.Random` by the doorbell | `Random` by SYSCALL/SVC |
| `Exit` | `abi.Exit` by the doorbell | `Exit` by SYSCALL/SVC (never returns) |
| `Idle(until)` | `abi.CPUPark` or spin | `Yield`/`Park` by SYSCALL/SVC (§7) |
| `Hwinit0` | heap start at RamStart | the same |
| `Task`/`Wake`/`ProcID` | `abi.CPUStart`/`CPUWake`/`CPUSelf` | the same calls, one level down (§7, KU5) |

The user board's `Call(nr, a...)` builds the call block exactly as `board.Call` does (the 64-byte
block, aligned, on the stack) but issues `SYSCALL` (RAX = block address) or `SVC #0` (X0 = block
address) instead of the store to the doorbell address (Appendix A's `uboard.doorbell` branches on
the process flag — S3). The kernel reads the block from the process's memory (§4.3).

### 3.5 Saving and restoring across a crossing

On a system call or fault the kernel's entry saves what SYSCALL/the CPU did not: the general
registers it will use, the user RSP/SP_EL0, and — lazily, only at a process switch — the user FS
base / TPIDR_EL0. A call that returns to the same process restores them and `SYSRETQ`/`ERET` with
no CR3/TTBR0 write (the kernel's mappings are in the process's tables, §2.1), so the only cost is
the register save/restore and the crossing itself (24-31 ns, Appendix A). A call or fault that
leads the kernel to run another process, or its own goroutines on another vCPU, saves the full
context (§3.1) and, when it next resumes a different process, writes CR3/TTBR0 (with PCID/ASID,
§2.4) and the FS base / TPIDR_EL0.

### 3.6 arm64: SVC/ERET

- **Into a process**: `ERET` with `SPSR_EL1` = EL0t (M=0b0000), `ELR_EL1` the entry/return PC,
  `SP_EL0` the user stack, `TPIDR_EL0` the process's `g`.
- **Out of a process, a system call**: `SVC #0` → `VBAR_EL1 + 0x400`, ESR_EL1.EC = 0x15, X0 the
  block address.
- **Out of a process, a fault**: `VBAR_EL1 + 0x400` with EC 0x20 (instruction abort) or 0x24 (data
  abort); FAR_EL1 the faulting address (§8).
- **The SP_EL0 detail**: the kernel's Go code runs on SP_EL0 at EL1t, and EL0 uses the same
  register. Entering a process saves the kernel goroutine's SP_EL0; the handler, on SP_EL1, saves
  the process's SP_EL0, restores the kernel's, and returns into Go (GoTEE's `Exec`/`MONITOR_EXCEPTION`
  pattern, the survey §2.3).

## 4. The system-call ABI

### 4.1 The call block, reused

A system call is the doorbell's call block, one level down (K2, the survey §2.4): the 64-byte block
`arbace/box/abi` defines (`BlockNr` 0, `BlockArgs` 8, five 8-byte args, `BlockRet` 48, `BlockEvent`
56), in the process's memory, aligned to its size. The instruction is `SYSCALL` with RAX = the
block's address (amd64) or `SVC #0` with X0 = the block's address (arm64), in place of the store to
the doorbell. The numbers, arguments and results keep their meaning. So **a process's board is a
tool guest's board with SYSCALL/SVC in place of the doorbell's store** (the survey §2.4), and a tool
image runs as a guest or a process unchanged (K6, KU4).

The alternative ABIs (GoTEE's r0-r2 registers with JSON-RPC; a fresh ABI) are rejected (K2, S10):
the block ABI exists, is proven, and makes tool images portable between the two back ends.

### 4.2 The numbers

The existing numbers mean the same when the kernel can serve or forward them:

- **served or forwarded by the kernel**: `Write` (15) and the merged `WaitRead` on handles (§5),
  `Random` (19), `NowMs`/`Time`, the store (`BlobPut` 25 … `RefSet` 29), the network (40-52),
  `EventArea` (53), `Spawn`/`SpawnPoll`/`SpawnOut` (54-56). The kernel checks the process's policy
  (its handles, its store view, its budget, §4.3) and then answers from its own driver or forwards
  to the monitor by the doorbell (console, store, network — which the kernel already does as a
  guest).
- **a process adds** (new numbers, from a block the kernel owns, above the box's `NCalls`, KS5):
  - `ProcExit` — end this process with a[0] (the user board's `Exit`); never returns.
  - `Yield` — give up the vCPU to the kernel until runnable again or a[0] ns pass (the board's
    `Idle`, §7); `Park`/`Wake` among a process's own vCPUs for SMP (KU5), the twins of
    `CPUPark`/`CPUWake`.
  - `Grow` — ask for n more bytes of heap at the top of the process's RAM window (§4.4), when the
    window is not all mapped up front.
  - `SetFSBase` (amd64, if `settls`'s application path is kept, KS3) — set this vCPU's FS base.
  - `NilHandler` — tell the kernel the PC a nil dereference resumes at, so a page fault below the
    first page becomes the process's own panic (§8, Appendix A's `uboard.NilPanics` makes this call
    when it is a process).

The numbers a process may **not** use (the kernel refuses them with a fault-style error): the
guest-only SMP boot calls as issued to the monitor, `HostInit`, `Fault` (a process does not report
its own faults; the kernel observes them, §8), and `Suspend`/terminal calls that are the embedding
host's. A number outside the process's policy is refused per §4.3, not faulted.

### 4.3 The kernel's answer: validate, apply policy, forward

For each call the kernel (GoTEE's `TransferRegion`, the survey §2.4):

1. **reads the block** from the process's memory through the kernel alias of the process's RAM (the
   one addition of §2.1), after checking the block's address lies in the process's RAM and is
   aligned. With SMAP/PAN on, the kernel never dereferences a user VA directly (S11).
2. **checks every buffer** the block names (`a[i]` that is a pointer, with its length) lies wholly
   in the process's RAM — as the monitor checks a guest's buffers against its RAM (`vmm`'s
   `m.guest(addr, n)`, §6 of the box). A buffer outside ends the process with a fault report
   (§8), as a guest's bad buffer faults the guest.
3. **applies the process's policy**: its handle table (§5) resolves a handle to a kernel object;
   its store view (the blobs it may read, as a tool guest's view, BOX-NOTES.md S7); its network
   policy; its budget (RAM, vCPUs). A call naming a handle it does not hold, or an action its policy
   refuses, returns the ABI's refusal (`EBADF`, `EPERM`, `StoreRefused`, …), not a fault.
4. **answers or forwards**: a call the kernel serves itself (handle I/O to a kernel buffer, time,
   yield, grow) it answers at once; a call that needs a device or the store or the network it makes
   to the monitor by the doorbell on the process's behalf, under the process's policy (the kernel
   already holds these as a guest). A forwarded call costs a crossing and a VM exit; a served call
   only a crossing (the survey §5.1).

5. **writes `ret`** (and any out-arguments) back into the process's block, and returns to the
   process (§3.5).

### 4.4 Heap growth

A process's RAM window (§2.1) can be mapped in full at load (simplest; the whole window faults in
on touch as the box's guest RAM does, committed by the monitor as touched — BOX-NOTES.md, the
balloon) or grown on demand by `Grow` (KS4). TamaGo's `sbrk` stays within `[RamStart,
RamStart+RamSize)` and never calls the OS, so the window's size is the process's maximum heap. The
kernel sets `RamSize` to the process's budget. *Recommended* (S12): map the window in full and let
the monitor commit pages as the process touches them, as the box's guest already does — no `Grow`
call in KU1-KU2, added only if a process needs to grow past its initial budget.

### 4.5 Errors

Results follow the box's ABI conventions (BOX-NOTES.md, `abi`): a count or 0 on success, a negative
number on failure — the network's `-errno` (`EAGAIN`, `EBADF`, `EPERM`, …), the store's `Store*`
(`StoreRefused` −1 …), the spawn's `SpawnRefused`. A process's own calls add `ProcExit` (no return)
and `Yield`/`Grow` (0 or a negative error). A call the kernel cannot make sense of (a bad block, a
buffer outside the process's RAM, a forbidden number) is not an error result: it **ends the
process** with a fault report (§8), as the monitor ends a guest that misuses the doorbell.

## 5. Handles and values across

### 5.1 Bytes and handles, never pointers

Values cross the boundary only as **bytes** (call buffers the kernel copies between its alias of the
process's RAM and its own buffers; the store's blobs; the console's and the network's streams) and
as **handles** (small non-negative integers) (K4, the survey §3.4). No heap pointer and no heap
object crosses. This is the box's rule between guests already: a spawn's request and result are EDN
blobs in the store, and S7's tools exchange file trees (BOX-NOTES.md S7). A process is one more
party to that rule.

### 5.2 The kernel's handle table

The kernel keeps a **handle table per process**: the index is the handle the process holds, the
entry is the kernel object (a console stream, a store view, a network connection or listener, a
spawn, an event area) with the rights the process was given. A process names a kernel object only by
a handle, and can forge none: an integer it did not receive resolves to nothing (`EBADF`). This is
seL4's capability shape and the box's network handles already (the survey §4.6, §3.4; `abi`'s
network handles 0 up). The standard handles a process starts with: 0 stdin, 1 stdout, 2 stderr
(streams the kernel bridges to the process's console, captured into its result as a tool guest's is,
§6), and whatever its spawn request granted (a store view, input trees as a read-only file source,
an output sink). Handles are **where capabilities enter** (K12, KU7): a process can act on only what
it was handed.

### 5.3 The console and streams

`Write` (15) and the merged `WaitRead` on a handle are the process's stdio, the kernel bridging them
to the box's console driver (which reaches the monitor). A process's stdout and stderr are captured
(bounded) into its result (§6), exactly as a tool guest's console is (BOX-NOTES.md S7, `tree.Result`
`:console`). Interpreted Go and the REPL in a process write to the process's own stdout (as GOI-SPEC
GS15 has interpreted Go do), which is this captured stream.

### 5.4 Later: shared pages

For large transfers (a big archive between a process and the kernel, or between two processes), the
kernel may map a page range into both, read-only in one or read-write in both, named by a handle
(GoTEE's transfer region; K4 (b), the survey §3.4). This is not built in KU1-KU6; it is KS6, added
only when a measurement shows the per-call copy dominating. Even then no Go pointer crosses: the
shared range holds bytes, and each side keeps its own view of them. The first version copies
(§4.3), as the store and the network already do.

## 6. The process manager

### 6.1 Spawn, wait, kill

The kernel's **process manager** is the twin of the monitor's spawner (`vmm.Spawner`,
`vmm/spawn.go`) one level down. It:

- **loads** a process from an image blob (the same image as the kernel, or any `GOOS=tamago` box
  image — a tool image, BOX-NOTES.md S7): it allocates the process's RAM window, builds its page
  tables (text/rodata shared, data/bss copied, heap/stack zeroed, the kernel's mappings EL1-only,
  §2.1), writes its argv/env into its low memory as the monitor does for a guest (`vmm/layout.go`'s
  `writeArgs`), sets the entry registers and `whim_uentry` as the entry, and makes a handle table
  with the request's grants (§5.2).
- **spawns** it: gives it a vCPU (§7) and a kernel goroutine that runs `Exec(ctx)` in a loop
  (§3.1), serving its system calls, until it exits or faults.
- **waits** for it: a kernel caller (the REPL, the build driver) gets the process's result — its
  exit code, its output (a store tree if it declared one), its captured console — a `tree.Result`,
  the same shape a tool guest returns. The spawn and wait calls are the box's `Spawn`/`SpawnPoll`
  (§4.2), so a process spawning a child uses the same calls as a guest spawning a tool guest.
- **kills** it: on a budget overrun, a watchdog, or the parent's request, the kernel frees the
  process's RAM and tables and records a fault/kill result. A process cannot refuse (it holds no
  privilege).

### 6.2 The request and the result

A process's spawn request is S7's request unchanged (`arbace/box/tree`'s `Request`: `:image`,
`:args`, `:env`, `:trees`, `:out`, `:cwd`, `:ram`, `:cpus`), and its result is S7's `Result`
(`:exit`, `:out`, `:console`, or `:fault`). The resources `:ram` and `:cpus` are the process's RAM
window and vCPU count. The trees become a read-only file source the process reads lazily through its
file system, as a tool guest does (`arbace/box/tool`'s `MkLazy`, BOX-NOTES.md S7) — the kernel
serves the file reads as store-blob reads under the process's store view. This is why one request
serves both back ends (§6.4).

### 6.3 Relation to S7's tool guests

S7 runs each tool (gc's `compile`, `link`, `asm`) as a **guest** under the monitor
(BOX-NOTES.md S7): a VM of its own, 84-170 ms a spawn. The process manager runs the same tool image
as a **process** under the kernel: a page table and a copy of the RW segment, about a millisecond a
spawn (estimate, measured at KU2), with the same request, the same trees in and out, the same
result. The tool image is unchanged (K6, §4.1): `arbace/box/tool`'s shim makes the same calls, which
are now SYSCALL/SVC to the kernel instead of the doorbell to the monitor. KU4's exit criterion is
that `bin/box-tools`' actions run as processes produce byte-for-byte the same archives and images as
run as guests (§9).

### 6.4 One request, two back ends: how `arbace.box`'s spawn chooses

`arbace.box/spawn` ([arbace/box.ae](../../arbace/box.ae)) today stores a request and calls
`host/spawn`, which the monitor answers with a guest (BOX-NOTES.md S7). With processes it gains a
back end: the kernel's process manager. Which one serves a spawn is **policy, not hardware** (K6,
the survey §6 (d)):

- **a process** for the cheap and frequent cases: a single compile, a REPL sandbox, a short tool
  run, many small actions — where a millisecond start and MMU isolation suffice.
- **a guest** where a hypervisor is there and the case is heavy or needs the strongest isolation: a
  whole build, truly untrusted native code, a run that must not touch the kernel even through a bug
  in the call handler.
- **the choice**: `arbace.box/spawn` takes an optional `:where` (`:process`, `:guest`, or `:auto`),
  defaulting to `:auto` (S13): a process when the kernel offers the back end and the request fits a
  process's budget, a guest otherwise or when `:where :guest`. On bare metal, where there is no
  hypervisor unless Arbace is one (K11), `:auto` and `:guest` both become a process (the survey §6
  (d)). The request and result are identical, so a caller need not know which ran it.

## 7. Scheduling

### 7.1 vCPUs per process first (KU1-KU5)

Through KU5 the box keeps its interrupt-free design (BOX-NOTES.md: the guest runs with interrupts
off, `preemptMSupported = false`, goroutines switch cooperatively). A process is given **vCPUs of
its own**, as a guest is (K8, the survey §2.6, §3.4):

- a one-vCPU process runs on a kernel goroutine's vCPU through `Exec` (§3.1). While the process
  runs, that goroutine's M is busy (bracketed by `entersyscall`/`exitsyscall`, so the P is handed
  off and the kernel's other goroutines run on other vCPUs). The process yields the vCPU back to the
  kernel only at a system call or a fault — including its `Idle`, which is a `Yield` call (§3.4).
- a process that never makes a call keeps its vCPU until the watchdog (§8). This is the same bound
  the box's guest has today (the survey §2.6): acceptable through KU5.
- an SMP process (KU5) starts more vCPUs by `Task` → a `CPUStart`-style call one level down (§4.2);
  the kernel gives it vCPUs of its own, as the monitor gives a guest its vCPUs
  (`vmm/smp.go`, `board/smp.go`). Its goroutines still switch cooperatively within the process; the
  kernel does not schedule them.

### 7.2 Timer preemption (KU6)

KU6 adds a timer interrupt so the kernel takes a vCPU back at each tick and can stop a process that
does not yield, schedule another, or run its own goroutines (K8, the survey §2.6). This is
preemption **of processes**, not of a process's goroutines (those stay cooperative). It changes the
**board's** interrupt policy, not Go's runtime (S1, the survey §6.2): an IRQ vector, the LAPIC timer
(amd64) or the generic timer's virtual counter (arm64) that KVM and HVF deliver, EOI, and unmasking
at idle and in user mode. The kernel's own idle (`board.idle`, which spins or parks, `board.go`)
gains the timer as the thing it waits for. KU6's exit criterion is a spinning process stopped by the
kernel, not the watchdog (§10). This is the largest change below the runtime and is deferred to
last.

## 8. Faults and errors

A fault at EL0/ring 3 arrives on a kernel stack (RSP0 or an IST on amd64, SP_EL1 on arm64). The
kernel tells it from a kernel fault by the saved CS (amd64) or SPSR's mode and the vector quarter
(arm64), then (K7, the survey §2.5):

- **a nil dereference → the process's own panic.** A page fault with CR2/FAR below the first page
  (`0x1000`), in the process, is redirected to the process's `sigpanic`, exactly as the board does
  for the kernel itself (BX60, `board.go`'s `NilPanics` and `vector_common`/`vector_sync_sp0`), and
  as Linux's signal handler makes a SIGSEGV a run-time panic. The process named its `sigpanic` PC by
  the `NilHandler` call at start (§4.2; Appendix A's `uboard.NilPanics` makes this call when it is a
  process). The kernel rewrites the process's trap frame so the faulting instruction's frame returns
  into `sigpanic`, then `SYSRETQ`/`ERET` back into the process. The process then panics normally and
  may `recover` (its runtime's `runtime.Error`, Arbace's NullPointerException). This is the one case
  a fault returns to the process.
- **any other fault → the process ends.** A fault elsewhere (a bad address, #GP, #UD, a stack
  overflow the process's own guard did not catch, an instruction abort) ends the process. The kernel
  writes a report like `abi.Fault`'s (the vector/EC, the error code/ESR, the PC, CR2/FAR) as the
  process's result (`:fault`, `tree.Result`), frees the process, and goes on. The kernel's other
  work is unaffected.
- **a fault in the kernel → the guest ends**, as today (`abi.Fault` to the monitor,
  `vector_common`). The kernel is trusted; its fault is fatal to the box, as a guest's fault is
  fatal to the monitor's run.

Stack overflow in a process is the process's own runtime's business (its stack guards grow the
stack within its RAM window); the kernel needs no part in it unless the growth faults past the
window, which ends the process (a budget overrun).

### 8.6 Side channels

Spectre-class and Meltdown attacks are out of scope at first (the survey §6.1). The AMD host and
Apple's cores are not affected by Meltdown, so the kernel's mappings may stay in every process's
tables (no KPTI) without exposing kernel memory by Meltdown. A KPTI-like split is a later hardening
(KS7), not KU1-KU6.

## 9. The check

Each milestone ends with its check green and its records (journal, agenda; this spec amended where
building it finds otherwise, as SPEC.md's and GOI-SPEC's amendments were).

- **KU1 (amd64 mechanics)**: a kernel test guest (`box/guest/cmd/`, as the survey's `ring3`) runs a
  trivial process and checks: a SYSCALL into the kernel that serves a `Write` and returns; a page
  fault at ring 3 that ends the process with a fault report; a nil dereference at ring 3 that
  becomes the process's own panic; a per-process CR3 switch with shared text (two processes of one
  image with different data). On /dev/kvm. These are Appendix A's three validations, promoted into
  the kernel.
- **KU2 (the user board, a process)**: the box's demo guest (`box/guest/cmd/demo`) built on the user
  board and run as a process inside a kernel test guest gives **byte for byte** the output it gives
  as a guest (the KU2 exit criterion; the survey §6.2). A process's start time and memory measured.
- **KU3 (arm64)**: the same in the emulated aarch64 VM (`bin/box-arm64-vm`, KVM/arm64) and on the
  Mac (the `bin/box-mac-bundle` checklist, HVF). The ring-3 experiment ported to arm64 (K10) runs
  there for correctness and to confirm HVF gives the guest EL0.
- **KU4 (tools as processes)**: `bin/box-tools`' actions (gc's `compile`, `link`, `asm`) run as
  processes produce byte-for-byte the same archives and images as run as guests (§6.3); the in-box
  rebuild (BOX-NOTES.md S8) with processes in place of tool guests produces the same image. A spawn
  check in `bin/box-smoke`.
- **KU5 (SMP processes)**: a multi-vCPU guest test (`TestGoGuestSMP`'s program) run as a process
  gives the same result; the process starts and uses its own vCPUs.
- **KU6 (timer preemption)**: a spinning process is stopped by the kernel, not the watchdog.
- **The box's gate**: the box's smoke test (`bin/box-smoke`), its oracle subset and `bin/box-suite`
  stay green at each milestone (BOX-NOTES.md). KU1-KU5 change only `box/` and add no runtime patch,
  so the essential `bin/gate`'s runtime key is unchanged and the Go build need not be rebuilt for
  them; the kernel test guests build with TamaGo's go as the box's guests do.

## 10. Milestones

From the survey's §6.2, refined. amd64 first (K9). KU1-KU4 change only `box/` (the board, the new
user board, test guests, the process manager) and add no runtime patch; `overlay/go` is untouched.
KU6 changes the board's interrupt policy (§7.2).

| milestone | content | exit criteria | effort (days) |
|---|---|---|---:|
| **KU1** amd64 mechanics | in a kernel test guest (`box/guest/cmd/`): per-process page tables (text/rodata shared, data/bss/heap/stacks private, the kernel's mappings EL1-only, §2.1, §2.2); GDT/TSS/MSRs, the SYSCALL entry and `IRETQ`/`SYSRETQ` (§3.2); the context and `Exec` (§3.1); a page fault and a nil dereference at ring 3 (§8); a CR3 switch between two processes of one image | the test on /dev/kvm: a SYSCALL served (a `Write`), a page fault ends the process, a nil → the process's panic, two processes with distinct data; no change to the monitor | 2-3 |
| **KU2** the user board, a process | `arbace/box/uboard` (as built: the board's process mode) (the goos hooks as SYSCALL/SVC, `whim_uentry`, §3.3, §3.4); the kernel's call handler for a process (validate, policy, forward, §4); the console/stdio handles (§5.3); the demo guest built on the user board and run as a process inside a kernel test guest | the demo's output as a process **equal byte for byte** to its output as a guest; a process's start time and RSS measured (the survey §5.3) | 2-3 |
| **KU3** arm64 | the same on arm64: lower-EL vectors, `ERET`, SP_EL0 handling, CNTKCTL (EL0VCTEN), PAN, AP (§2.3, §3.6); the ring-3 experiment ported to arm64 (K10) | the emulated VM's KVM/arm64 and the Mac bundle's checklist: the demo as a process equal to as a guest; the port confirms HVF gives EL0 | 2-3 |
| **KU4** the process manager and tools | the kernel's process manager (load, spawn, wait, kill, §6); the handle table and store view (§5.2); S7's tool images as processes; `arbace.box/spawn`'s `:where` (§6.4) | `bin/box-tools`' actions as processes byte for byte as guests; the in-box rebuild with processes; a spawn check in `bin/box-smoke` | 3-5 |
| **KU5** SMP processes | `Task`/`Park`/`Wake` as calls, vCPUs per process (§7.1) | a multi-vCPU guest test (`TestGoGuestSMP`) as a process gives the same result | 3-5 |
| **KU6** timer preemption | the board takes the LAPIC (amd64) and generic (arm64) timers; processes preempted (§7.2) | a spinning process stopped by the kernel, not the watchdog | 2-4 |
| KU7 (later) | the REPL in a process: `arbace.box` spawns a REPL process with a store view; the language sandbox (K12's option (c)) inside it; a capability evaluator (K12) | the box's smoke test through a process | 2-3 |

**Effort to KU6: 14-23 working days**, within the survey's 17-27 for the whole staged plan (the
survey counted KU0 and KU7 too).

### As built: KU1 (2026-10-17)

KU1 is done, on amd64, in a kernel test guest, on /dev/kvm. All of the KU1 row's exit criteria are
met. The work is in `box/` only; no runtime patch; the essential gate's runtime key is unchanged
(S1, S31).

What was built, as the spec describes it, with the small deviations noted:

- **The board's two-mode entry** (`box/go/arbace/box/board/`, the KU0 prototype folded into the box's
  own board, S3, §2.5): `whim_uentry` (`boot_amd64.s`), a process's first instruction — it sets a
  `process` flag, keeps the entry registers (cpuinit's contract), touches nothing privileged, and
  jumps to `_rt0_tamago_start`; `board.Process`, `board.UEntryAddr`. The `process` flag is `0` in
  every shipped guest, so their path is unchanged (one `CMPB` added to `doorbell`, which issues a
  SYSCALL in place of the store when a process). `board.NilPanics`, when a process, tells the
  kernel its `sigpanic` PC by the new `NilHandler` call (§4.2, §8). The board snapshots the image's
  pristine writable data at `hwinit1` (before any package `init` runs), so the kernel can give a
  process the image's initial data (the inittask states `NotStarted`, not the kernel's once it has
  run) — the one point §2.1 ("the RW segment is copied into the process's RAM at load") needed to
  make work for a second runtime of the same image.
- **The crossings** (`board/user_amd64.go`, `user_amd64.s`): `UserSetup` (the GDT's user data/code
  segments at `0x30`/`0x38`, STAR/LSTAR/FMASK/EFER.SCE, GS its per-vCPU area, SMAP and PCIDs when
  the CPU has them; SMEP left off because a crossing runs the shared text as user pages with the
  process's tables loaded, §2.2); `UserContext` and `EnterUser` (the kernel goroutine runs a process
  as one long call, `entersyscall`/`exitsyscall` bracketed, S4, S22); `enterUser` (IRETQ first,
  SYSRETQ after a call — written as the bytes `0x48 0x0f 0x07`, S33), `sysentry` (SYSCALL's entry,
  the context saved), `whim_ufault` (a fault from ring 3, reached from `vector_common`'s new
  ring-3 branch, S21). The FS base and (new) the kernel's and the process's FPU state are saved and
  restored across a crossing; a call that returns to the same process writes CR3 anyway, because b1
  puts the process's data at the kernel's virtual addresses (one image), so the kernel's globals are
  not addressable while the process's tables are loaded — the one departure from S23 ("a call
  returning to the same process writes no CR3"): KU1 writes CR3 on every crossing. The cost is a TLB
  reload per crossing where S23 wanted none; with PCIDs on (the host has them) the reload keeps the
  TLB, so it is a CR3 write, not a flush. Revisited at KU2 if it shows in the numbers (KS13 below).
- **The kernel** (`box/go/arbace/box/kernel/`, amd64 only): `NewProc` loads a process of this image
  — private data+bss (the board's pristine snapshot, then zeroed bss), a RAM window for heap and
  stacks, and per-process 4-level page tables (`tables_amd64.go`): the whole kernel space
  identity-mapped supervisor-only, the image's 2 MiB region page by page (text shared user-exec,
  rodata shared user-read, data/bss private user-rw), the window mapped at its own high VA to private
  pages. `Run` enters the process and serves its calls: `arch_prctl` (settls' `SetFSBase`, KS3) and
  the block ABI (`Write`/`Message` captured to the process's output, `Random` forwarded to the
  monitor, `ProcExit`/`Exit`, `Yield`, `NilHandler`). A fault below the first page with the process's
  `sigpanic` named is turned into the process's own panic (the trap frame rewritten, as the board's
  BX60 does for the kernel); any other fault ends the process with a report (§8).
- **The test** (`box/guest/cmd/ktest`, `box/guest/ku1_test.go`): one image that is the kernel at its
  ELF entry and a process at `whim_uentry`. `TestKernelKU1`, on /dev/kvm, checks all four: a SYSCALL
  served (a `Write`, then exit 7); a page fault at ring 3 ends the process with a vector-14 report;
  a nil dereference becomes the process's own panic, recovered; two processes of one image with
  distinct data (`alpha`/`bravo`) on different CR3s. It is part of `bin/box-vendor kvm-tests` and the
  box's suite.

Measurements (TamaGo go1.27.1, amd64, /dev/kvm, the machine shared): a process's RAM window 64 MiB
(the default; TamaGo's heap arenas are 4 MiB, so a small runtime needs a few); `TestKernelKU1` runs
all four checks, each spawning a fresh process (new page tables, a copy of the ~27 KiB data image),
in about 0.35 s total. The crossing cost itself (SYSCALL/SYSRET plus the register and FPU save)
was not separately re-measured here beyond the survey's and Appendix A's 24-31 ns for the bare
round trip; KU2 measures a process's start time and RSS against a guest's, as its exit criterion asks.

Deviations from the spec's letter, each sound: KU1 writes CR3 on every crossing (b1's shared virtual
addresses, above; S23 revisited at KU2); the user board is not a separate `arbace/box/uboard`
package but the two-mode board (S3, Appendix A, §2.5: the goos hooks are linknamed once, so one
image cannot link both a board and a uboard — the prototype's two-mode board is the realisable form
of b1). KS3's `SetFSBase` is served by interpreting settls' `arch_prctl` SYSCALL rather than a new
call number, since TamaGo's runtime issues `arch_prctl` and KU1 adds no runtime patch; `SetFSBase`
(64) is reserved for a future explicit path. The new process-call numbers (`NilHandler` 64 and the
reserved `ProcExit`/`Yield`/`SetFSBase`) sit above the box's `NCalls` (KS5), in `arbace/box/abi`.

New proposals for the user: **KS13** (the per-crossing CR3 write). b1 (one image, the process's data
at the kernel's addresses) forces a CR3 switch on every crossing, not only at a process switch
(§3.5's S23 wanted none for a same-process call). Options: (a) keep it — with PCIDs the switch keeps
the TLB, and the measured cost is small; the KU2 numbers decide whether it matters (*recommended*,
built); (b) a KPTI-like split or a per-process kernel alias at distinct addresses, to let a
same-process call avoid the CR3 write, at the cost of a second set of kernel mappings. Recommend (a),
revisit at KU2 with the numbers.

### As built: KU2 (2026-10-17)

KU2 is done, on amd64, on /dev/kvm. The KU2 row's exit criteria are met: **the demo guest's own
image, run as a process under a kernel, gives byte for byte what it gives as a guest**, and a
process's start time and memory are measured (below). The work is in `box/` only; no runtime patch,
no change to the monitor (S1, S2, S31).

What was built:

- **The user board** is the board's process mode (KU1's departure, followed: the goos hooks link
  once per image, so `arbace/box/uboard` is not a package; §2.5, §3.4 read "the board in its process
  mode"). KU2 completes §3.4's table: every call is a SYSCALL to the kernel when a process (KU1's
  `doorbell`); `Exit` is now `ProcExit` (§4.2) and `Idle` is `Yield` — to near the deadline, then a
  spin, as a guest's park (`board.idleProcess`); `Printk` stays `Message`, which is the box ABI's
  stderr (§3.4's "`Write` on handle 2": the box's console calls carry no handle, so stderr is
  `Message`'s stream and stdout `Write`'s); `Nanotime` reads the TSC at ring 3; `Task`/`Wake`/`ProcID`
  are the same calls (KU5 gives them meaning).
- **The call handler** (`box/go/arbace/box/kernel/calls.ae`, §4.3's five steps): the block read
  through the kernel alias, which must lie in the process's RAM and be aligned; every buffer a call
  names checked against the process's RAM (the window or its private data); the policy applied
  (`kernel.Config`); the call answered by the kernel or **forwarded to the monitor as the kernel's
  own call with the buffers' addresses translated to the kernel alias** — the monitor reads and
  writes the process's pages in place, no copy — and the answer copied back (ret, the event, the
  out-arguments but the buffers' addresses). The event is copied too, so a deadly signal the monitor
  catches during a forwarded wait reaches the process's own deathtrap. Served: `NilHandler`,
  `ProcExit`/`Exit`, `Yield` and a process's `CPUPark` (the kernel goroutine sleeps), `SetFSBase`,
  settls' `arch_prctl`, `CPUSelf`/`CPUWake` (one vCPU, the process's 0), `CPUStart` refused (-1:
  no such vCPU until KU5). Forwarded: `Random`, `NowMs`, `Time`, and the console's calls under the
  console grant. Refused by policy (S26): the store (`StoreRefused`), the network (-1, "no
  network"), `EventArea` (-1), the spawns (`SpawnRefused`) — KU4 grants them. Ending the process
  (S25): a block or buffer outside its RAM, an empty `WaitRead` buffer, `Fault`, `Alloc`, `Free`,
  any number neither the guest's nor a process's. A zero-length buffer is forwarded pointing at a
  kernel byte, so no process can make the monitor fault the kernel.
- **The console and stdio** (§5.3, KS10): stdout (`Write`) and stderr (`Message`) are always captured,
  bounded, into `Proc.Output`; with `Config.Console` they are also the kernel's own (forwarded), stdin
  is the box's (`WaitRead`, `ReadInput`, `WaitForInput` forwarded), and the terminal's calls
  (`HostInit`, `Suspend`, the tty's, `Raise`, `Delay`) are made on the process's behalf. Without the
  grant stdin is at its end, `Delay` is the kernel's sleep, and the terminal's calls answer 0
  (refused). This refines §4.2's list of numbers a process "may not use" (KS14 below).
- **The kernel** also: `kernel.New(Config)` (args *and* environment written as the monitor writes a
  guest's; the RAM window `Config.RAM`, default 64 MiB); the image's page-by-page region generalised
  from one 2 MiB block to all the image's blocks (the demo with the kernel linked in reaches
  `0x3fdc50`, and the box's program is ~100 MB, KU4); the window mapped a leaf table per 2 MiB in one
  go; **the PCID kept after a process's first entry** (KU1 flushed the process's TLB entries on every
  entry: its CR3 never had bit 63; now the first entry flushes, the later ones keep); `Proc.Free`
  and a pool of windows (below); `Proc.Stats` and `Proc.Memory` (the start and the resident set from
  the page tables' accessed and dirty bits); `kernel.RunSelf`, a program run as a process of itself.
- **The check** (`TestKernelKU2`, `box/guest/ku2_test.ae`, in `bin/box-vendor kvm-tests`): the demo
  guest (`box/guest/cmd/demo`) is its own kernel when booted with `BOX_KERNEL=1` in its environment
  (`cmd/demo/kernel_amd64.ae`; on arm64 the guest as always): it runs itself as a process with the
  same command line and the console grant. On the same host with the same script, as a guest and as a
  process, five sessions — `TestGoGuest`'s (`demo work hello`), `TestGoGuestSMP`'s keys on one vCPU,
  and `TestReadAheadGo`'s three (keys read ahead across a suspension, a pending SIGWINCH, a deadly
  signal during the merged wait) — give **the same bytes, the same exit code and the same host log**
  (the reads, the suspension, the signal). This is stronger than §9's letter ("built on the user
  board and run as a process inside a kernel test guest"): the image is the demo's own, not the demo
  linked into ktest, and the comparison includes the console's input side and a signal's event.
  `ktest bench` (`box/guest/cmd/ktest/bench.ae`, `TestKernelKU2Crossings`) and
  `TestKernelKU2Measure` make the measurements; `board.BenchEntry` installs a measuring SYSCALL
  entry for them (dead code in every other image). `guest.TestImages` leaves the amd64-only ktest out
  for arm64 (since KU1 it failed to build there).

Measurements (TamaGo go1.27.1, amd64, a guest on /dev/kvm, the machine shared; PCIDs on; best of
five runs of 100,000 crossings; ns per round trip from ring 3):

| crossing | ns |
|---|---:|
| bare SYSCALL+SYSRETQ | 24-32 |
| SYSCALL, CR3 to the kernel's and back (TLB kept, PCIDs), SYSRETQ | 143-173 |
| the same with the process's TLB entries flushed (KU1's every entry) | 228-266 |
| SYSCALL, CR3, both FPU states swapped (FXSAVE64/FXRSTOR64 each way), the FS base, SYSRETQ: all the crossing's machine state as built, no Go | 385 |
| a call served as built (block address in RAX: the entry, CR3, FPU, FS, `Run`'s loop, the block decoded and answered) | 444-575 |
| the same through `board.Call(abi.Yield)` from Go | 510-646 |
| for scale: the kernel's own doorbell call (`CPUSelf`, a VM exit) | 16,400-21,400 |

So a call back to the same process costs about 0.45 µs as built, of which the two CR3 writes are
about 120 ns (~27%) and the FPU swap about 240 ns (~55%); the Go side (entersyscall/exitsyscall, the
loop, the decode) is about 60 ns. A call the kernel forwards adds a VM exit, 16-21 µs, 35-45 times a
served call.

A process's start (the ktest "write" process: prints a line and exits; and the demo):

| | fresh window | window used again (`Free`) |
|---|---:|---:|
| `New` (tables, the data copy, the window) | 17-72 ms (of it the window: all but 0.3 ms) | 0.09-0.15 ms |
| from the first entry to its first call (its runtime's start) | 0.4-0.7 ms | 0.2-0.6 ms |
| to its main's first console call (the demo's `HostInit`) | 1.4-2.3 ms | — |
| to its exit (the trivial process) | — | 0.7-1.9 ms |
| the demo guest, for comparison: `vmm.Run` to main's first call (a VM made, the image loaded, the runtime started) | 10-40 ms | |

The fresh window's cost is TamaGo's `sbrk`, which clears the memory its heap grows into: the
kernel's runtime clears the 64 MiB window it allocates (and the host commits every page of it) before
the process runs. A window freed by an ended process is cleared of only the pages its tables say it
wrote (dirty bits) and those the kernel handed out as buffers, and used again: then a process starts
in about 0.1 ms plus its runtime's start, **about a millisecond to a trivial exit**, against a
guest's 10-40 ms to main (§6.3's estimate holds once the window is reused).

A process's memory (its tables' accessed and dirty bits, 4 KiB pages): private resident 12.4 MB
(dirty 12.4 MB) of a 64 MiB window — the process's runtime clears what its heap grows into, so its
initial arenas are resident at once — plus 0.4 MB of private data+bss, 160 KiB of page tables, and
0.5-0.9 MB of shared text/rodata touched (the kernel's own pages). The same for the trivial process
and the demo (whose `work` allocates little). Host side, the demo run added 17-18 MB of RSS as a guest
and 88 MB as a process (the kernel's 64 MiB fresh window, cleared whole, plus its own heap); with a
window used again the process's own share is its 12.4 MB.

Deviations from the spec's letter, each sound: the user board is the board's process mode (above);
§3.4's stderr is `Message`, not `Write` on a handle 2; the demo is its own kernel (`BOX_KERNEL`)
rather than run inside ktest, which makes the check stronger; the terminal's calls are allowed under
the console grant (KS14); a process's SMP calls on one vCPU are served (`CPUSelf` 0, `CPUPark` as
`Yield`) rather than refused, being "the same calls, one level down" (§3.4).

**KS13, decided again with the numbers.** The CR3 written on every crossing costs about 120 ns of a
0.45 µs served call (with PCIDs kept, which KU2 fixed; KU1's per-entry flush cost another 85-95 ns)
and nothing measurable next to a forwarded call (16-21 µs). The larger share is the FPU swap (~240
ns): Go's ABI makes the vector registers caller-saved at the kernel's `enterUser` call and at the
process's `doorbell`, so a SYSCALL crossing could restore only MXCSR and the x87 control word,
keeping the full save for faults (and KU6's preemption) — the next lever if crossings ever matter
(an estimated 0.45 → ~0.2 µs), before (b)'s second set of kernel mappings. *Recommended: keep (a).*

New proposal for the user: **KS14 the console grant.** §4.2 lists `Suspend` and "the terminal calls
that are the embedding host's" among the numbers a process may not use, while §5.3 and KS10 bridge a
process's stdio to the box's console. Options: (a) a grant: a process given the console
(`Config.Console`; for `arbace.box/spawn`, the REPL's foreground process at KU7) has its stdio and
the terminal's calls (`HostInit`, `Suspend`, the tty's, `Raise`, `Delay`) made on its behalf; without
it its stdout and stderr are only captured, its stdin is at its end, the terminal's calls answer 0
(*recommended*, built: the demo as a process needs `HostInit` and `Suspend`, and a process with no
grant is still isolated from the terminal); (b) never: the terminal's calls end the process, as §4.2
says, and a process's console is only ever captured.

## 11. Decisions and open questions

### 11.1 Decisions taken while writing (S1-S40)

Each on the terms of K1-K12; the user may revise any (§11.2 asks about the ones with real
alternatives).

| S | decision | reason |
|---|---|---|
| S1 | No patch to Go's runtime in KU1-KU5; KU6 touches only the board's interrupt policy | (b)'s whole point (the survey §5.4); a process is `GOOS=tamago` on a board |
| S2 | The monitor (`vmm`) is unchanged; EL0/ring 3 is the guest's own | the experiment changed nothing in it (Appendix A) |
| S3 | One image is kernel or process, chosen at the first instruction (`whim_uentry` vs the ELF entry), by a flag the entry sets | b1 (K3); the prototype's `process` flag (Appendix A) |
| S4 | A process is run by a kernel goroutine as one long `Exec` call | the collector scans it in an ordinary frame; (a)'s traceback problem is absent (the survey §3.4) |
| S5 | The compilers and the REPL are processes, not the kernel | they read untrusted input / run user code (K5) |
| S6 | Per-process page tables, text/rodata shared read-only user-executable, the kernel's mappings EL1-only in every table | b1 (K3); the image's own three LOAD segments (Appendix A) |
| S7 | PCID/ASID may start off in KU1 and be added by measurement | the cost is one flush per process switch, not per call (§2.4) |
| S8 | amd64 `settls` reused: it already branches on CPL and avoids WRMSR at CPL 3 | TamaGo's own runtime (`sys_tamago_amd64.s`) |
| S9 | A process's `osInit` is a no-op; it trusts the kernel's CPU feature set | EL1 ID registers trap at EL0 (§2.3); same CPU |
| S10 | The system-call ABI is the doorbell's block by SYSCALL/SVC | K2; tool images portable between back ends |
| S11 | The kernel reads a process's buffers through the kernel alias of its RAM, SMAP/PAN on | one addition (§2.1); no user-VA deref |
| S12 | A process's RAM window is mapped in full and committed as touched; no `Grow` in KU1-KU2 | the box's guest RAM already works so (§4.4) |
| S13 | `arbace.box/spawn` takes `:where` (`:process`/`:guest`/`:auto`), default `:auto` | K6; one request, policy chooses (§6.4) |
| S14 | A process's result is S7's `tree.Result` (`:exit`/`:out`/`:console`/`:fault`) | one shape for both back ends (§6.2) |
| S15 | A process's spawn request is S7's `tree.Request` unchanged | K6; §6.2 |
| S16 | Faults: nil → the process's panic (BX60 one level down), others end the process, a kernel fault ends the guest | K7; the board's trick already exists (§8) |
| S17 | Through KU5 a process gets vCPUs of its own; interrupts stay off | K8; the box's design rule until KU6 (§7.1) |
| S18 | Values cross as bytes and handles only; shared pages are later (KS6) | K4; the box's rule between guests (§5) |
| S19 | A process names a kernel object only by a handle from a per-process table | capabilities' shape (§5.2); the network's handles already |
| S20 | A process's stdout/stderr are captured into its result, as a tool guest's | §5.3; `tree.Result` `:console` |
| S21 | The crossing's kernel side is NOSPLIT assembly returning into a Go function on the kernel goroutine | the board's vectors, gVisor's `ring0` (§3.1) |
| S22 | `Exec` is bracketed by `entersyscall`/`exitsyscall` | the P is handed off while the process runs (§3.1, §7) |
| S23 | A call returning to the same process writes no CR3/TTBR0 | the kernel's mappings are in the process's tables (§2.1, §3.5) |
| S24 | The FS base / TPIDR_EL0 is saved/restored lazily, only at a process switch | a same-process call costs only the crossing (§3.5) |
| S25 | A bad block, a buffer outside the process's RAM, or a forbidden call number ends the process with a fault report | the monitor ends a guest that misuses the doorbell (§4.5) |
| S26 | A refused policy (bad handle, forbidden action) returns the ABI's negative error, not a fault | the network/store already do (§4.3, §4.5) |
| S27 | `NilHandler` is a process call that names its `sigpanic` PC to the kernel | the board sets `sigpanicPC` locally; a process tells the kernel (§4.2, §8) |
| S28 | `Nanotime` reads the counter at EL0 (RDTSC, CNTVCT_EL0 with EL0VCTEN); no crossing | speed; the board already reads it directly (§3.4) |
| S29 | The process manager is the monitor's spawner one level down | §6.1; `vmm.Spawner`'s twin |
| S30 | Input trees reach a process as a lazy read-only file source, read as store blobs under its view | S7's tool guest already (§6.2) |
| S31 | KU1-KU4 change only `box/`; the essential gate's runtime key is unchanged | no runtime patch (S1) |
| S32 | A kernel test guest (`box/guest/cmd/`) is the harness for KU1-KU3, as `ring3` was | the survey's experiment ran so (Appendix A) |
| S33 | SYSRETQ is written as bytes (`0x48 0x0f 0x07`); the experiment's two pitfalls are recorded | Go's assembler lacks REX.W SYSRET (the survey §2.3, Appendix A) |
| S34 | arm64 keeps the single-TTBR0 switch scheme (TTBR1 for the kernel is later, KS1) | symmetric with amd64, one mechanism (§2.3) |
| S35 | SMEP and SMAP (amd64), PAN (arm64) on while the kernel runs | the kernel never executes or silently reads a user page (§2.2) |
| S36 | Through KU5 a process's RAM window is the whole heap; its budget is `RamSize` | TamaGo's `sbrk` never calls the OS (§4.4) |
| S37 | The timer (KU6) changes the board, not the runtime | K8; the preemption is of processes, not goroutines (§7.2) |
| S38 | No KPTI in KU1-KU6; the AMD host and Apple's cores are not Meltdown-affected | side channels out of scope at first (§8.6) |
| S39 | A process's SMP vCPUs come from the kernel, as a guest's from the monitor | `Task` → `CPUStart` one level down (§7.1) |
| S40 | The name is `doc/go/PROCESS-SPEC.md` | the process is the new thing (the header) |

### 11.2 Open questions for the user (KS1-KS12)

Each with the recommendation this text follows.

- **KS1 arm64's translation registers.** (a) one TTBR0, switched on entry and process switch, the
  kernel EL1-only in every table (§2.3, symmetric with amd64); (b) TTBR1 for the kernel (high half),
  TTBR0 per process (low half), so a same-process call changes no table and the kernel's data is
  relinked into the high half. *Recommended: (a)* now, (b) if a measurement asks.
- **KS2 arm64 CPU features in a process.** (a) the process's `osInit` is a no-op and it trusts the
  kernel's parsed feature set (§2.3); (b) the kernel passes the features in an entry register.
  *Recommended: (a)*: same CPU, EL1 registers trap at EL0.
- **KS3 amd64 TLS for a process.** (a) the kernel sets the FS base and `settls`'s application path
  becomes a `SetFSBase` call (§3.3); (b) the user board establishes `g` before `settls` and
  `settls` is a no-op for a process. *Recommended: (a)*: explicit, matches `arch_prctl`'s shape.
- **KS4 heap growth.** (a) map the RAM window in full, commit as touched, no `Grow` (§4.4); (b) a
  `Grow` call that extends the mapping. *Recommended: (a)* in KU1-KU2, (b) when a process must grow
  past its budget.
- **KS5 the process-call numbers.** (a) new numbers above the box's `NCalls` (`ProcExit`, `Yield`,
  `Park`/`Wake`, `Grow`, `SetFSBase`, `NilHandler`), §4.2; (b) reuse the guest SMP/exit numbers with
  a process meaning. *Recommended: (a)*: the box's numbers stay a guest's; a process's are its own.
- **KS6 shared pages.** (a) copy bytes in KU1-KU6, add a shared-page handle later by measurement
  (§5.4); (b) build shared pages from the start. *Recommended: (a)*: the store and network already
  copy; measure first.
- **KS7 KPTI-like hardening.** (a) none in KU1-KU6, the kernel mapped (EL1-only) in every process
  (§8.6); (b) a split now. *Recommended: (a)*: the hosts are not Meltdown-affected.
- **KS8 `:where` default.** (a) `:auto` — a process when the kernel offers it and the request fits,
  a guest otherwise (§6.4); (b) default to a guest (today's behaviour) until processes are proven.
  *Recommended: (a)*, with (b) as a fallback switch while KU4 is young.
- **KS9 the kernel as a mode of the box, or a separate entry.** (a) the box's program is the kernel
  when booted at its ELF entry, a process at `whim_uentry` — one build, one mode flag (§1 principle
  5, S3); (b) a separate kernel build. *Recommended: (a)*: one image (b1).
- **KS10 a process's console vs the kernel's.** (a) a process's stdio are handles the kernel bridges
  to the box console and captures into the result (§5.3); (b) a process shares the box console
  directly. *Recommended: (a)*: capture and isolation, as a tool guest.
- **KS11 the watchdog for a non-yielding process (pre-KU6).** (a) a per-process budget the kernel
  enforces at its next crossing, and the monitor's watchdog for a process that never crosses (§7.1,
  §8); (b) nothing until KU6's timer. *Recommended: (a)*: the box has a watchdog already.
- **KS12 KU7 and the capability evaluator.** (a) the REPL as a process and the language sandbox
  (K12 (c)) inside it, surveyed after KU4 (§10); (b) sooner. *Recommended: (a)*, later, as K12
  decided.

## 12. Sources

- Arbace (at `d8b7c76c`, and the survey's reading at `ca4d3eed`):
  - [KERNEL-USER-SURVEY.md](KERNEL-USER-SURVEY.md) (the survey this spec builds on; K1-K12 decided);
  - [BOX-NOTES.md](BOX-NOTES.md) (S1-S8: the doorbell, BX60's nil-dereference trick, S7's spawn and
    tool guests, the store, the network, events, SMP); [BOX-SURVEY.md](BOX-SURVEY.md) (exit costs);
  - [doc/AGENDA.md](../AGENDA.md); [doc/STANDALONE-NOTES.md](../STANDALONE-NOTES.md) (S7);
  - `box/go/arbace/box/abi/abi.ae` (the call block, the numbers); `.../vmm/setup_amd64.ae`,
    `setup_arm64.ae`, `layout.ae`, `vmm.ae` (the monitor's set-up, the call dispatch, buffer
    checking), `spawn.ae` (the spawner), `tamago.ae` (the Go guest's RAM); `.../board/board.ae`,
    `boot_amd64.s`, `boot_arm64.s`, `smp.ae` (the board, the vectors, SMP); `.../tool/tool.ae`,
    `.../tree/request.ae` (the tool shim, the request and result); `.../guest/guest.ae`,
    `guest_test.ae` (the test-guest harness); `arbace/box.ae` (`arbace.box/spawn`).
- TamaGo v1.27.1 (`/root/tamago-go`, `/root/go/pkg/mod/github.com/usbarmory/tamago@v1.27.1`):
  `src/runtime/goos/` and `goos/goos_*.s` (the hooks, `CPUInit`); `src/runtime/os_tamago.go`
  (`newosproc`, `haveSysmon`=false for tamago), `mem_tamago.go` (`sbrk`), `sys_tamago_amd64.s`
  (`settls` branching on CPL, `IA32_FS_BASE`), `tls_arm64.h` (`TPIDR_EL0` for GOOS_tamago),
  `internal/cpu/cpu_arm64_tamago.go` (`osInit` reading EL1 IDs); `user/linux/runtime.go`,
  `syscall_amd64.s` (the Linux user board: the five calls, the model for the user board).
- GoTEE (https://github.com/usbarmory/GoTEE at `813dc65a`, read in the survey): the applet as a
  second runtime, `monitor.Exec`/`ExecCtx`, `TransferRegion`, SVC and JSON-RPC (the process model,
  the context, value-passing by bytes).
- gVisor `pkg/ring0` (https://gvisor.dev/docs/architecture_guide/platforms/): `SwitchToUser`
  (sysret/iret), nosplit hooks, `entersyscall`/`exitsyscall` wrapping (the crossing's kernel side).
- Biscuit (OSDI '18, https://github.com/mit-pdos/biscuit): a collected kernel scanning only its own
  heap (§1 principle 3). seL4 (https://sel4.systems/): capabilities as handles (§5.2).
- Architecture: AMD64 Architecture Programmer's Manual vol. 2 (SYSCALL/SYSRET, STAR/LSTAR/SFMASK,
  IRET's segment nulling, CR4.SMEP/SMAP/PCIDE/FSGSBASE); Arm Architecture Reference Manual for
  A-profile (D5 stage-1 AP and UXN/PXN, PAN, the vector table's lower-EL entries, ESR_EL1.EC
  0x15/0x20/0x24, CNTKCTL_EL1, TTBR0/TTBR1 and ASIDs).

## Appendix A: the experiments

The experiments ran in `.tmp/ku0-spec/` (and reused the survey's `.tmp/kernel-user-survey/`), not
tracked. TamaGo's go1.27.1 (`/root/tamago-go`), amd64, the machine shared, a guest on /dev/kvm.

### A.1 The survey's ring-3 round trip, re-run (2026-10-17)

The survey's `box/guest/cmd/ring3` (a TamaGo guest on the box's board that builds user page tables,
a GDT with DPL-3 segments, SYSCALL's MSRs and a DPL-3 IDT gate, drops to ring 3, and measures;
Appendix A of the survey) was re-run unchanged to confirm the figures and the infrastructure. On
this run (TSC 2445 MHz):

| crossing | ns | cycles |
|---|---:|---:|
| SYSCALL + SYSRETQ (ring 3 → 0 → 3, empty handler) | 26-31 | 64-76 |
| INT 0x80 + IRETQ (DPL-3 gate on IST1) | 110-114 | 269-279 |
| MOV CR3, same root, no PCID (a full TLB flush) | 145-147 | 356-359 |
| a Go call (noinline) | 2.6-2.9 | 6-7 |
| the doorbell `CPUSelf` (a VM exit, nested) | 18,200-18,600 | ~45,000 |
| Go `userWork` at CPL 3 vs CPL 0 (best) | ~0.9 ms either | — |

These match the survey's §5.1 (SYSCALL 24.5 ns, INT 102 ns, CR3 96-136 ns, the doorbell 13-18 µs)
within the shared host's noise. The collector was left on for `userWork` and the nil-panic case was
not exercised here; the survey's §3.2 already recorded the collector-at-ring-3 throw that motivates
the process model (option (b), not (a)). **Conclusion: the mechanism and the figures reproduce; the
process model's cost assumptions hold (§3.5, §7).**

### A.2 A two-mode board and a process program compile from one image (2026-10-17)

To validate b1 (§2.1) and principle 5 (one image is kernel or process) at the build level, a
prototype user board `box/uboard` (`uboard.go`, `uboard_amd64.s`) was written as **one board with
two entries**: `cpuinit` (a guest's, ring 0, enables SSE) and `whim_uentry` (a process's, ring 3,
sets a `process` flag and touches nothing privileged), joining at a common start that takes the
entry registers and jumps to `_rt0_tamago_start`. Its `Call` issues the doorbell's store when a
guest and `SYSCALL` (RAX = the block) when a process; its `NilPanics` sets `sigpanicPC` locally and,
when a process, makes a `NilHandler` call to tell the kernel. A test program `box/guest/cmd/uhello`
(ordinary Go: allocation, a GC, a map, a channel, a recovered nil dereference, a sleep, 20,000
`CPUSelf` calls) builds against it for `GOOS=tamago`:

```
go build -trimpath -ldflags "-T 0x201000 -R 0x1000" ./box/guest/cmd/uhello   # 2,234,136 bytes
nm: cpuinit, whim_vectors, runtime/goos.CPUInit.abi0, arbace/box/uboard.Call  all present
readelf -l: three LOAD segments  R E (text+rodata exec) | R (rodata) | RW (data+bss)
```

**Conclusions:**

- the image links with a board that distinguishes a guest entry from a process entry — one build
  serves both (S3, principle 5);
- the three LOAD segments are exactly the split §2.1 needs: the first two (`R E`, `R`) shared
  read-only / user-executable across the kernel and every process, the third (`RW`, 0x32cd0 in
  memory of which 0x10a20 on file: data then bss) copied into each process's private RAM;
- the process path of the board omits the privileged CR0/CR4 set-up a guest does, confirming a
  process's `whim_uentry` must touch nothing privileged (§3.3).

Not done at KU0 (they are KU1's work, requiring a kernel that hosts a process, which this appendix
deliberately does not build): running `uhello` as a process under a kernel and comparing its output
to its output as a guest (KU2's exit criterion); the per-process CR3 switch exercised live (the
survey measured the switch's cost in A.1; KU1 wires it into a kernel). `whim_uentry` is
dead-code-eliminated here because nothing references it yet, as `whim_apentry` would be without the
board's `LEAQ`; KU1's kernel references it and keeps it.

## Decided (2026-10-17, the user)

KS1-KS12 as recommended: one TTBR0 on arm64, the kernel EL1-only in every table (KS1); a
process's `osInit` trusting the kernel's CPU features (KS2); the FS base set by the kernel,
`settls` a `SetFSBase` call (KS3); the heap window mapped in full, no `Grow` in KU1-KU2 (KS4); new
process-call numbers above `NCalls` (KS5); bytes copied, shared pages later by measurement (KS6);
no KPTI-like split (KS7); spawn's `:where :auto`, with a guest-only switch while KU4 is young
(KS8); the kernel a mode of the one box image (KS9); a process's stdio as captured handles
(KS10); a per-process budget and the monitor's watchdog before KU6 (KS11); KU7 surveyed after KU4
(KS12).

**Decided (2026-10-17, the user):** KS13 (a): CR3 written on every crossing (one image puts a
process's data at the kernel's addresses; with PCIDs the TLB is kept), decided again with KU2's
numbers.

**Decided (2026-10-18, the user), with KU2's numbers:** KS13 (a) again: the CR3 write on every
crossing kept (about 120 ns of a 444 ns call back to the same process; the FPU swap's 240 ns the
bigger lever, recorded, not built); KS14 (a): the console grant as built.
