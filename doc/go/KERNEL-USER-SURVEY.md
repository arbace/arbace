# A kernel and a user part in the box — survey

Status: survey, 2026-10-17, written at the user's request. Nothing here is adopted. The decisions
(§7, K1-K12) are the user's. The note it answers is in doc/AGENDA.md's Later section:

> A kernel and a user part in the box (the user, 2026-10-14): the infrastructure (the
> collector, the scheduler, the compiler, I/O, the store) at EL1/ring 0 and the application
> level at EL0/ring 3, inside one guest. Today the guest is one TamaGo program at EL1/ring 0, one
> address space, one Go runtime for both. To survey: what crosses the boundary (a call ABI like
> the doorbell's, one level down); one heap the kernel's collector scans across the privilege
> levels, or a heap per user part; the scheduler's goroutines running user code at EL0 (the
> page tables and exception vectors TamaGo's board sets up); the cost of the crossings for a
> REPL's fine-grained calls; against the isolation S7 already has (a guest per tool, spawned) and
> isolation in the language (namespaces, capabilities). A guest per tool needs a hypervisor
> under Arbace: on bare metal (the box's long-term target, TamaGo's boards) there is none unless
> Arbace is it (EL2 on arm64, VMX root on amd64: go-whim's monitor inside Arbace, where the
> hardware boots it at EL2 or gives VT-x), so the EL1/EL0 split is the isolation that works on
> any hardware, the guests only where virtualization is there. Prior art: Go's runtime as a user
> process; GoTEE (github.com/usbarmory/GoTEE: TamaGo unikernels and user-mode applets, TrustZone
> on arm), to check; unikernels with a privilege split.

Everything was read and measured on main at `ca4d3eed`. The machine was amd64, an AMD EPYC 7763
(Zen 3) with nested SVM, running the box's monitor on /dev/kvm. The toolchain was TamaGo's go1.27.1
(`/root/tamago-go`). The experiment's scratch is in `.tmp/kernel-user-survey/` and is not tracked.
Appendix A describes the experiment: a TamaGo guest on the box's board that goes to ring 3 and
measures the round trips. GoTEE was cloned into the scratch directory at `813dc65a` (2026-10-09)
and read, not built. Nothing was fetched into the repo.

"Kernel" here means the code at EL1 or ring 0. "User" means the code at EL0 or ring 3. "Monitor"
means the box's VMM on the host (`arbace/box/vmm`). "Board" means `arbace/box/board`.

## Executive summary

- **Today (§2.1, §2.2).** The guest runs at ring 0 on amd64 and at EL1t on arm64. Nothing in it
  can run at a lower privilege. The monitor builds the system state and the board never changes
  it:
  - On amd64 the GDT has only DPL 0 segments and the page tables never set the U/S bit. No
    SYSCALL MSR is set, and the IDT's 32 gates all report a fault and end the guest.
  - On arm64 every page is AP=00 (no EL0 access) and UXN. TTBR1 is disabled.
  - Go's code runs on SP_EL0 at EL1, the stack register that EL0 would use.
  - TamaGo's own `InitMMU` (not used by the box) also maps everything EL1-only.
  - TamaGo's runtime has no signals and no asynchronous preemption (`preemptMSupported = false`).
    The box's guest runs with interrupts off.
- **The mechanism is cheap and small.** It was measured in a TamaGo guest on the box's board
  (§5.1, Appendix A). The guest was given identity tables with the user bit, a GDT copy with two
  DPL 3 segments, SYSCALL's MSRs and an IDT gate of DPL 3. It then dropped to ring 3. About 250
  lines of Go and assembly, all in the guest, needed no change to the monitor.
  - **SYSCALL+SYSRETQ round trip: 24.5 ns (60 cycles).**
  - INT+IRETQ: 102 ns (250 cycles).
  - MOV CR3 without PCID: 96-136 ns.
  - A Go call: 2 ns.
  - The doorbell (a VM exit, nested): 13-18 µs.
  - Linux's `getpid` on the same host: 200 ns.
  - A spawned tool guest (S7): 84-170 ms.
- **The hard part is the one runtime (§3).** In the experiment, ordinary Go ran at ring 3
  unchanged: fib, `make`, map inserts, at the same speed as at ring 0. It ran because every page
  was a user page. The runtime's code and data (mheap, mcache, the g and the P, the write
  barrier's buffers) must be writable by user code if user code is to allocate. Then a crossing
  isolates nothing.
  - On the second round the runtime's collector, run as an allocation's assist *at ring 3*,
    scanned the goroutine's stack and threw ("traceback did not unwind completely") at the
    crossing frame.
  - With kernel pages unmapped at EL0, every allocation, every write barrier during a GC, every
    stack growth, every channel operation and every park becomes a system call. The kernel must
    then validate a heap that user code writes, or a forged pointer breaks the collector.
  - **One heap across the privilege levels only works when user code is memory-safe by
    construction, and then the hardware boundary adds nothing.** That is language-level isolation
    (c).
- **The model that works is GoTEE's applet and TamaGo's own `user/linux` (§4).**
  - **A process is a second TamaGo runtime at EL0, with its own heap, collector and scheduler.**
  - TamaGo's runtime already needs only a handful of hooks from its board: `Hwinit`, `Nanotime`,
    `Printk`, the RNG, `Exit`, `Idle`, `Task`, `Wake`.
  - TamaGo ships a `user/linux` package that answers them with five Linux system calls, so that a
    `GOOS=tamago` program runs as a Linux process.
  - GoTEE runs a TamaGo applet in ARM user mode under a TamaGo supervisor, through SVC and
    JSON-RPC.
  - The box already has the ABI such a process needs: the doorbell's call block (nr, a[5], ret).
    A "user board" would make the same calls by SYSCALL or SVC instead of the doorbell's store.
    **The same tool image would then run as a guest (S7) or as a process.**
- **Crossings for a REPL (§5.2).** In the process model the REPL's fine-grained calls (`first`,
  `conj`, a var's deref, an allocation) never cross. The whole Arbace program runs inside the
  process: evaluator, runtime, collections. Only I/O, the store, spawns and time cross, as on
  Linux. At 25-100 ns a crossing, I/O-sized calls cost nothing measurable.
- **Memory (§5.3).** The guest image is 42 MB of text, 57 MB of read-only data and 3.5 MB of data
  and bss. A process of the same image shares the first two, so it costs 3.5 MB plus its heap and
  stacks.
- **Recommendation (§7).** Treat the process as the unit of isolation on bare metal (option (b)).
  - Its system calls are the doorbell's calls, one level down.
  - Values cross as bytes (the store's blobs, streams), never as heap pointers.
  - Keep language-level isolation (c) inside a process for the REPL's own sandboxes.
  - Keep a guest per tool (d) where a hypervisor is there.
  - Do not build (a), privilege per goroutine over one heap.
  - Staged: the mechanics on amd64 in a test guest, the user board, arm64, the kernel's process
    manager, SMP processes, then timer preemption. **About 17-27 working days.**

## 1. What would run where

### 1.1 Today

The guest is one TamaGo program (`GOOS=tamago`, the board `arbace/box/board`), linked at
`0x201000`, identity-mapped, at ring 0 or EL1. One Go runtime serves everything in it:

- the collector and the scheduler;
- the drivers, which are the doorbell's clients: console, store, network, spawn, events;
- Arbace's runtime (jrt and the compiled namespaces);
- the evaluator and the Java forms' interpreter;
- the REPL's user code.

Everything is in one heap and can write everything. A nil dereference becomes a panic (BX60). Any
other fault ends the guest through `abi.Fault`. Isolation exists at two levels:

- **between guests.** Since S7 a guest spawns tool guests (`abi.Spawn`, `vmm.Spawner`). Each one
  is a VM of its own with a store view, no network and no spawn. That costs a hypervisor under
  the box (KVM, Hypervisor.framework) and 84-170 ms a spawn.
- **in the language.** Namespaces and vars hide nothing: interop reaches any class, `eval` any
  code, `alter-var-root` any var. Nothing in Arbace is a capability today.

### 1.2 The note's split

The note puts "the collector, the scheduler, the compiler, I/O, the store" at EL1/ring 0 and "the
application level" at EL0/ring 3. Read concretely, the parts are:

| part | kernel (EL1/ring 0) | user (EL0/ring 3) | why |
|---|---|---|---|
| exception vectors, page tables, the call ABI's entry | yes | — | only the kernel can |
| the doorbell (calls to the monitor) | yes | no: through the kernel | the monitor trusts the guest. Inside, the kernel must check what a user part asks for (store view, network policy, RAM) |
| drivers on bare metal (virtio, NIC, storage, timer, IRQ) | yes | — | devices are the kernel's |
| the store (blobs, refs, the views S7 gives a child) | yes | a client | its integrity is the point: the kernel holds the refs |
| process management (load, map, schedule vCPUs, budgets, kill) | yes | — | |
| the Go runtime: collector, scheduler | the kernel's own | **depends on the option** (§3): the kernel's in (a), each process's own in (b) | the crux of this survey |
| compilers (c2j, j2g, gc's `compile`/`link`/`asm`) | **no** | yes | they read untrusted input. S7 runs them as tool guests for this reason |
| the evaluator, the Java and Go forms interpreters | no | yes | they run user code |
| user namespaces, REPL input, interpreted forms | no | yes | |

This differs from the note in one respect, argued in §3 and §6. The collector and the scheduler
can stay at EL1 only in (a). A collector at EL1 that manages user memory has to trust that memory,
so in the model that isolates, each user part has its own collector and scheduler at EL0. The
kernel's runtime collects only the kernel's heap. The same goes for the compilers: they are
applications.

### 1.3 Why a split at all: three kinds of isolation

| | a guest per tool (S7, (d)) | EL1/EL0 in one guest ((a), (b)) | the language ((c)) |
|---|---|---|---|
| needs | a hypervisor under Arbace (KVM, HVF, or Arbace at EL2 or VMX root on bare metal) | an MMU and two privilege levels: any hardware TamaGo runs on | nothing |
| protects against | everything in the guest, the guest's kernel included | user code against the kernel and other processes. The kernel is trusted | code that keeps to the language: no `unsafe`, no native code, no bugs in the runtime |
| resources (RAM, CPU) | per guest, enforced by the monitor | per process, enforced by the kernel (CPU only with timer interrupts, §2.6) | none enforced: one heap, cooperative scheduling (a loop never yields) |
| cost of a crossing | a VM exit: 13-18 µs nested, about 1 µs on bare metal (BOX-SURVEY.md §5) | 25 ns (SYSCALL) to 100 ns (INT), plus 100 ns if the address space changes (§5.1) | a call: 2 ns |
| cost of a start | 84-170 ms (S7) | a page table and a copy of 3.5 MB of data: about a millisecond (estimate) | nothing |
| works on bare metal | only if Arbace is the hypervisor | yes | yes |
| works in the box on the Mac (HVF) | no nested virtualization on the M2 | yes: EL0 is the guest's | yes |

The note's point holds: a guest per tool needs virtualization, and on bare metal Arbace would have
to be the hypervisor. The EL1/EL0 split is the hardware isolation that works everywhere,
including inside the box under KVM and HVF, which give the guest EL1 and EL0 (ring 0 and ring 3)
but no nested hypervisor (§1.3's last row). Language isolation is free, but it holds only as far
as the runtime is correct, and it bounds no resource.

## 2. Mechanisms on both architectures

### 2.1 amd64 today (`vmm/setup_amd64.ae`, `board/boot_amd64.s`)

The guest starts "in 64-bit ring 0 at its first instruction" (`setup_amd64`'s doc). The monitor
writes:

- **the GDT** at `sysBase` (0x1000): null, 0x08 code (`0x00af9b000000ffff`: P, DPL 0, L), 0x10
  data (DPL 0), 0x18 the TSS (16 bytes). No DPL 3 descriptor exists.
- **the TSS**: IST1 the fault stack of each vCPU (`tssStride` 0x80). RSP0 is not set, because no
  ring change ever happens.
- **the IDT**: 32 interrupt gates into `whim_vectors`, all DPL 0 on IST1. Nothing above vector 31
  is there, so there is no gate for INT from ring 3.
- **the page tables**: 4-level identity (`pageFormat`: `ptP`, `ptRW`, `ptNX`, `ptPS`). Bit 2 (U/S)
  is never set, so every page is supervisor-only.
- **CR0** PE MP ET NE WP PG, **CR4** PAE (the board adds OSFXSR and OSXMMEXCPT). SMEP and SMAP are
  off. **EFER** LME LMA NXE. SCE is off, and STAR, LSTAR and FMASK are not set.

The board's `whim_vectors` handles vector 14 below 0x1000 as a nil dereference when `NilPanics`
is on. It rewrites the interrupted frame so that it returns into `sigpanic`. Every other exception
reports `abi.Fault` by the doorbell and never returns.

### 2.2 arm64 today (`vmm/setup_arm64.ae`, `board/boot_arm64.s`)

The guest starts at **EL1t** (CPSR `0x3c4`: EL1 on SP_EL0, DAIF masked) with the MMU on. The
monitor sets:

- **the page tables**: 3-level identity for 36 bits (4 KiB granule, 2 MiB blocks). Every leaf is
  `dAF | dUXN`, plus `dPXN` unless executable and `dRO` unless writable. AP[1] is never set, so
  EL0 can access nothing (AP=00 or 10), and UXN is everywhere, so EL0 can execute nothing.
- **TCR_EL1** with EPD1 (bit 23): TTBR1 walks disabled. The guest uses only TTBR0, low addresses.
- **VBAR_EL1** = `whim_vectors`, 16 entries. The board treats only the first entry, synchronous
  at EL1t (where Go runs), as possibly a nil dereference. The "lower EL" entries (offsets 0x400 to
  0x780) report and end the guest like any other.
- **SP_EL0** the stack (Go's code runs on it at EL1t), **SP_EL1** the fault stack.

TamaGo's own arm64 `InitMMU` (`tamago/goos/.../arm64/mmu.ae`, not used by the box's board, whose
MMU the monitor sets up) is the same in spirit. It uses a flat map, `TTE_AP_00` ("EL1 or above:
read/write, EL0: none") on memory and devices, XN on everything but the text, and TTBR1 disabled.
amd64's TamaGo (`amd64/mmu.ae`) only reads and edits CR3's tables (`FindPTE`, the C-bit for SEV).
**Neither board has any notion of EL0 or ring 3.** TamaGo's runtime itself runs at whatever level
it is entered at. Its `user/linux` package runs it at ring 3 under Linux (§4.2).

### 2.3 Entering and leaving user mode

**amd64.**

- *Set-up, once*:
  - a GDT with user data and user code at the positions SYSRET expects. STAR[63:48] is the base:
    SS = base+8, CS = base+16, both with RPL 3. In the experiment the base was 0x28, so the user
    data segment was 0x30 and the user code segment 0x38, after the monitor's TSS at 0x18;
  - STAR[47:32] the kernel's CS 0x08, LSTAR the entry point, FMASK (IF, DF, TF);
  - EFER.SCE;
  - the U/S bit on every level of the user's mappings;
  - TSS.RSP0, or IST entries for the gates a ring-3 exception takes, so that a fault in user code
    switches to a kernel stack;
  - SMEP and SMAP (CR4 bits 20 and 21) so that the kernel neither executes nor silently reads
    user pages.
- *Into user*: IRETQ with a frame (SS, RSP, RFLAGS, CS, RIP) the first time; SYSRETQ after a
  system call. Go's assembler spells SYSRET without REX.W, so 64-bit SYSRETQ must be written as
  `BYTE $0x48; BYTE $0x0f; BYTE $0x07`, as the experiment did.
- *Out of user*: SYSCALL (RCX the return RIP, R11 RFLAGS, no stack switch: the entry must switch
  stacks itself), INT n through a DPL 3 gate, or any exception (CPU pushes SS:RSP, uses RSP0 or
  IST).
- *Segment detail*: IRETQ to ring 3 nulls a data segment register whose DPL is below 3. The
  experiment loaded FS with the user data selector and rewrote IA32_FS_BASE before going down, so
  that Go's code at ring 3 still found `g` where the runtime keeps it (FS-relative TLS). A real
  process sets its own FS base, which the kernel saves and restores at each switch.

**arm64.**

- *Set-up*:
  - AP[1] set on the user's pages (AP=01 user read/write, 11 user read-only);
  - UXN cleared on the user's text, PXN set on it;
  - CNTKCTL_EL1.EL0VCTEN so that the user runtime reads CNTVCT_EL0 for `nanotime`;
  - PAN (ARMv8.1, `SPSR.PAN` or `MSR PAN, #1`) so that the kernel does not touch user pages by
    accident;
  - the "lower EL, AArch64" half of the vector table (0x400 synchronous, 0x480 IRQ).
- *Into user*: ERET with SPSR_EL1 = EL0t (M = 0b0000), ELR_EL1 the user's PC, SP_EL0 the user's
  stack.
- *Out of user*: SVC #imm → VBAR_EL1 + 0x400 with ESR_EL1.EC = 0x15. Data and instruction aborts
  from EL0 have EC 0x24 and 0x20.
- *The SP_EL0 detail*: the kernel's Go code runs on SP_EL0 at EL1t, and EL0 uses the same
  register. Entering user mode therefore saves the kernel goroutine's SP_EL0 first. On the way
  back the handler, on SP_EL1, saves the user's SP_EL0, restores the kernel's and returns into Go.
  This is GoTEE's `Exec` pattern on ARMv7 (it saves `g_sp` in the context and restores it in
  `MONITOR_EXCEPTION`).
- *Rule*: memory writable at EL0 is never executable at EL1 (the architecture's implicit PXN for
  EL0-writable pages). That is harmless here and wanted.

On both architectures the kernel side of a crossing must be written like the board's vectors:
NOSPLIT assembly that runs without a Go stack of its own, then returns into a Go function on the
kernel goroutine. gVisor's `ring0` states the same rules: hooks run on the interrupt stack, must be
`go:nosplit`, and `SwitchToUser` is wrapped in `entersyscall`/`exitsyscall`.

### 2.4 A system call: the doorbell's ABI, one level down

The doorbell's call is one store of a call block's address: nr, a[5], ret, event, 64 bytes,
aligned to its size (`abi.BlockSize`). The monitor answers it. A system call from a user process
can be the same block:

- **the instruction**: SYSCALL with RAX the block's address (amd64), SVC #0 with X0 (arm64), in
  place of the doorbell's store. The user board picks the instruction. The block, the numbers and
  the answers stay the same.
- **the kernel's answer**:
  - check that the block and every buffer it names lie in the process's memory (GoTEE's
    `TransferRegion` does exactly this);
  - apply the process's policy (its store view, its network, its budget);
  - then either answer the call itself or forward it to the monitor by the doorbell (console,
    store, network on the box).
- **numbers**: the existing ones mean the same (`Write` 15, `Random` 19, the store 25-29, the
  network 40-52, `EventArea` 53, `Spawn` 54-56). A process adds a few: its own `Exit`, `CPUPark`
  and `CPUWake` among its own threads, and later a `Map` for more heap.

A tool guest already makes exactly these calls (`arbace/box/tool`). So a process is a tool guest
whose "monitor" is the kernel, and S7's spawn request (an EDN blob: image, args, trees, RAM,
vCPUs) works unchanged as a process's spawn request. This is the main result of §2: **the
boundary already exists as an ABI. It moves one level down.**

### 2.5 Faults in user code

A fault at EL0 or ring 3 arrives on a kernel stack: RSP0 or IST on amd64, SP_EL1 on arm64. The
kernel then has three choices:

- **the user runtime's own panic**: a nil dereference (FAR or CR2 below 0x1000) is redirected to
  the process's `sigpanic`, as the board's vectors already do for the kernel itself (BX60), and as
  Linux's signal handler does for a Go process. The user runtime then panics normally.
- **the process ends**: any other fault ends the process. The kernel writes a report like
  `abi.Fault`'s (vector, ESR or error code, PC, FAR or CR2) as the process's result, and the kernel
  goes on.
- **a fault in the kernel** still ends the guest, as today.

Stack overflow in a process is the user runtime's own business (its stack guards). The kernel
needs no help from it.

### 2.6 Timers, interrupts and preemption

The box's guest runs with interrupts off. Idle vCPUs park through the doorbell (`CPUPark`), and
there is no timer interrupt. TamaGo's runtime has no asynchronous preemption (`preemptM` is
empty). A goroutine yields only at calls, through the stack guard. For a kernel and processes this
means:

- **without interrupts**, a process that never makes a system call keeps its vCPU. The kernel
  sees it again only at the process's next call. The monitor's watchdog is the only stop.
- **with a timer interrupt** (the LAPIC timer on amd64 and the generic timer's virtual counter on
  arm64, both delivered by KVM and HVF), the kernel takes the vCPU back at each tick. It can stop
  a process, schedule another, or let its own goroutines run. That is preemption of *processes*.
  The user runtime's own goroutines still switch cooperatively, as today.
- **the box's design rule today** is that a vCPU never waits in the monitor, and timers stay in the
  guest. Taking interrupts changes the board: an IRQ vector, EOI, and unmasking at idle and in user
  mode. This is the largest change below the runtime (§6, K8).

## 3. The hard part: one Go runtime

### 3.1 What a goroutine touches

Plain Go code reaches the runtime constantly:

- the stack guard check in every prologue, which reads `g.stackguard0` through R14 or TLS;
- `mallocgc` on every allocation (the P's mcache, the mheap, spans, the GC's credit);
- write barriers during a collection (the P's `wbBuf`, then `wbBufFlush` into the mark queue);
- `morestack` and `newstack`, which copy the stack and edit `g`;
- `gopark` and `goready` on every channel operation, select, mutex and sleep (the P's run queues,
  `sched`);
- map growth, interface conversions (`itab` tables), `defer` and `panic` records, timers.

All of it reads and writes runtime state that one runtime shares among all goroutines.

### 3.2 The experiment: Go at ring 3 in the box

The guest of Appendix A ran `userWork` (fib 25, 2,000 `make([]byte, 512)`, 2,000 map inserts) at
ring 3. It ran on the current goroutine's stack, with every page a user page and no change to the
runtime:

| round | at CPL 3 (cycles) | at CPL 0 (cycles) |
|---|---:|---:|
| 1 | 1,958,456 | 1,630,867 |
| 2 | 6,089,279 | 1,586,498 |
| 3 | 1,511,846 | 4,695,278 |
| 4 | 1,584,881 | 1,579,025 |
| 5 | 1,611,414 | 14,473,326 |

The rounds are noisy because the host is shared and nested. The best of each is the same, about
1.5 million cycles (0.6 ms), so privilege costs nothing for computation. This was with the
collector off. With it on, the second round threw:

```
runtime: g1: frame.sp=0x95dd40 top=0x95ffe0
fatal error: traceback did not unwind completely
runtime.scanstack ... runtime.gcAssistAlloc1 ... runtime.mallocgc ... main.userWork ... userGo()
```

An allocation at ring 3 was charged a GC assist. The assist ran the collector *at ring 3*, on the
user goroutine, and the stack scan could not unwind through the hand-made crossing frame.

The two results together frame option (a):

- **privilege per goroutine is mechanically trivial**: the same code runs at either level;
- **it isolates nothing**: the user goroutine was also running the collector, the allocator and
  the scheduler, all writing the kernel's state.

### 3.3 One heap, kernel pages unmapped at EL0

To make (a) isolate, user code must not be able to write runtime state. Then every operation of
§3.1 that writes it becomes a system call:

- **allocation**: a crossing per `mallocgc`. At 25 ns that is about double the allocation itself
  (`mallocgc` of a small object is 10-20 ns). It could be reduced by giving each user goroutine a
  user-writable allocation buffer (a TLAB) that the kernel refills. But the kernel must then trust
  the headers and heap bitmaps the user wrote.
- **write barriers**: during a collection every pointer write records into a buffer the collector
  reads. If the buffer is user-writable, the collector must validate every entry. If not, every
  pointer write during a GC crosses.
- **stack growth**: `g` and the stack bounds are kernel state. `morestack` becomes a crossing. If
  the stack itself is user memory, which it must be to run, the collector's scan of it reads
  values the user wrote.
- **the scheduler**: `gopark` and `goready` cross. `gogo`, which switches goroutines, must know
  each g's level and return by IRETQ/SYSRETQ or ERET instead of a jump. The runtime would have to
  mark every g with its level and every M would switch level per g. That is a change to `proc.go`,
  `asm_amd64.s` and `asm_arm64.s` of the patched runtime (`overlay/go`).
- **the collector's trust**: this one is decisive. The kernel's collector scans user-writable
  memory (user heap objects, user stacks) and follows the pointers it finds. User code can write
  any integer into a pointer-typed slot through `unsafe` or a bug in jrt's natives. It can point
  at a kernel object, so that the collector marks it, keeps it alive or misreads it. Or it can
  point into a span with the wrong type, so that the collector scans garbage as pointers. A
  collector that follows user-written pointers is only safe if user code cannot forge pointers,
  which means user code is memory-safe by construction.

So (a) either crosses on every allocation and still trusts the user's pointers, or it requires
memory-safe user code. In the second case the hardware boundary only catches privileged
instructions, which memory-safe Go never executes. Singularity drew the same conclusion and put
its processes (SIPs) in one address space at one privilege level, relying on verified code (§4.5).

### 3.4 A heap per user part: a second runtime at EL0

The alternative is the Linux and GoTEE model. Each user part is a whole runtime instance
(`GOOS=tamago` on a "user board") with its own heap, collector, scheduler, stacks and timers, at
EL0/ring 3. The kernel's runtime knows nothing of its goroutines. Then:

- **write barriers, stack growth, scheduling and GC are local** to the process, as in a Linux Go
  program. Nothing in the runtime changes.
- **values cross as bytes**: call blocks with buffers (the store's blobs, the console's and the
  network's streams), copied by the kernel between address spaces, or later by shared pages that
  both map (GoTEE copies, with `ctx.Poke` and the transfer region). Arbace already does this
  between guests: a spawn's request and result are EDN blobs in the store, and S7's tools exchange
  file trees. Pointers never cross. A kernel object appears to a process as a handle, a small
  integer as the network's handles are. **Handles are where capabilities enter** (§4.6): a process
  can only name what it was handed.
- **the cost is memory and start-up, not crossings**: a second runtime per process (§5.3).
- **SMP**: a process's runtime starts Ms through `goos.Task` (`newosproc`). In a process that is a
  system call, `CPUStart` one level down. The kernel either gives the process vCPUs of its own
  (simplest, like a guest's vCPUs) or multiplexes processes' threads over vCPUs, which needs §2.6's
  timer. The first stage can run one-vCPU processes.
- **the kernel side** is a goroutine per process thread that calls `Exec(ctx)` (GoTEE's name): it
  runs the process until it traps, then handles the call and loops. While the process runs, that
  goroutine's M is busy, as in a blocking system call. With several vCPUs the kernel's other
  goroutines run elsewhere. With one vCPU they wait for the process's next call, or for a timer
  tick (§2.6).

### 3.5 Address spaces for (b)

There are two ways to place a process:

- **(b1) the same image, its own address space.** The Arbace guest image is not position
  independent (linked at 0x201000). Two instances of it need the same virtual addresses for
  different data. So each process gets its own page tables: the text and read-only data map the
  kernel's physical pages (shared, read-only, user-executable), while data, bss, heap and stacks
  map the process's own RAM. The kernel's mappings are present but EL1-only. Each crossing that
  switches tables writes CR3 or TTBR0. Without PCID or ASID that cost 96-136 ns in the experiment,
  plus TLB refills. With PCID (amd64) or ASIDs (arm64, which the TCR already provides for) the
  switch keeps the TLB. A call that returns to the same process needs no switch at all, since the
  kernel's mappings are in every process's tables, as Linux does without KPTI.
- **(b2) a separate image linked elsewhere** (for example at 0x4000_0000 + n·size), in the kernel's
  one address space, with its pages user-accessible and the kernel's not. That avoids the table
  switch, but each process kind is a second build of the program (100 MB, minutes), and processes
  of the same image cannot coexist without relinking.

(b1) is the process model every OS uses. It keeps one image for the kernel and its processes, and
the kernel can be the same Arbace program started in "kernel mode". The process's RAM is one
physically contiguous range mapped at a fixed offset, so the kernel translates a user pointer by
one addition after a bounds check. That is GoTEE's model too: the applet's `dma.Region`.

### 3.6 Signals and preemption in a process

A Go process on Linux gets SIGURG for asynchronous preemption, SIGSEGV for nil, SIGPROF for
profiles. TamaGo has none of them, in the kernel or in a process. Within a process, goroutines
switch cooperatively, as they do in the box today. That is acceptable, because the process is the
unit the kernel preempts (§2.6). An upcall mechanism that delivers "signals" into a process (the
kernel pushes a frame and returns into the user runtime's handler) would allow asynchronous
preemption inside processes later. The board's nil-dereference trick already does this for one
case.

## 4. Prior art

### 4.1 Go's runtime as a Linux user process

A Go program on Linux is the reference for (b):

- one runtime per process, its own heap and collector;
- system calls wrapped in `entersyscall`/`exitsyscall`, so that the P is handed off while a call
  blocks;
- signals for preemption (SIGURG since Go 1.14), nil (SIGSEGV) and profiling;
- values cross the boundary only as bytes (`read`, `write`, `mmap`).

Measured on the same host, `getpid` costs about 200 ns (raw or through `syscall.Getpid`). The
host's kernel has retbleed, SRSO and Spectre mitigations; the bare SYSCALL+SYSRETQ pair costs
25 ns (§5.1). Linux's entry path and mitigations account for the difference.

### 4.2 TamaGo's `user/linux`

`github.com/usbarmory/tamago/user/linux` (v1.27.1, the module cache; `tamago/goos/.../user/` in
Arbace's forms) runs a `GOOS=tamago` program as a Linux user process. Its `cpuinit` mmaps one
512 MB region at 0x80000000 as the runtime's RAM and sets the stack. It then links the goos hooks
to system calls:

- `Nanotime` → `clock_gettime`;
- `Printk` → `write`;
- `GetRandomData` → `getrandom`;
- `Exit` → `exit`.

**That is the whole board a TamaGo runtime needs at ring 3:** a RAM region, a clock, a console
byte, random bytes and exit, plus `Idle`/`Task`/`Wake` for SMP. A user board for Arbace's
processes is this package with SYSCALL/SVC to the kernel instead of Linux.

### 4.3 GoTEE (https://github.com/usbarmory/GoTEE, read at `813dc65a`)

GoTEE is the closest prior art. It "implements concurrent instantiation of TamaGo based unikernels
in privileged and unprivileged modes, interacting with each other through monitor mode and custom
system calls" (README). On ARMv7 (i.MX6UL) the Trusted OS is a TamaGo program in Secure World
system mode. The Trusted Applet is *another whole TamaGo program* in Secure World **user mode**. On
RISC-V (FU540) the OS is in M-mode and the applet in S-mode under PMP. There is no arm64 or amd64
port.

- **Applet = a second runtime.** `applet/applet.go` links the goos hooks to system calls:
  `Printk` → `syscall.Print` (SWI), `GetRandomData` → `SYS_GETRANDOM`, `Exit` → `SYS_EXIT`, and
  `Nanotime` from the generic timer (or a system call with the tag `syscall_nanotime`). A crash is
  a nil write (`applet.Crash`), so that the supervisor sees an abort.
- **The supervisor's side.** `monitor.Load(entry, mem, secure)` makes an `ExecCtx` over the
  applet's `dma.Region`, sets its domain and access permissions (`TTE_AP_011` for user), and sets
  SPSR to USR mode. `ctx.Run()` loops:
  - `Schedule()`: install the monitor's vector table, `Exec(ctx)`, restore the system's table;
  - the `Handler` (the default `SecureHandler`: exit, write, nanotime, getrandom, RPC);
  - `runtime.Gosched()`.

  `Exec` (`exec_arm.s`) saves the goroutine's registers and SP in `g_sp`, keeps the context pointer
  in TPIDRPRW, and loads the applet's registers with `ldmia r0, {r0-r15}^`. Every exception vector
  (`MONITOR_EXCEPTION`) stores the applet's state back into the context and returns into the Go
  code that called `Exec`. So **a process is run by a goroutine, as a long call.**
- **The ABI.** Registers r0-r2: the call number, then the arguments. `Read` and `Write` take a
  buffer that `TransferRegion` checks against the applet's region. Structured calls are **JSON-RPC
  over those two system calls** (`net/rpc/jsonrpc` on both sides, `SYS_RPC_REQ`, `SYS_RPC_RES`):
  values cross as bytes.
- **Isolation demonstrated**: the example applet reads the Trusted OS's memory after 5 s and is
  ended by the supervisor on the data abort ("SM stopped mode:USR ... err:ABT").
- **Extras**: soft lockstep (a shadow context run in tandem and compared), and `allgptr` (the
  supervisor inspects the applet's goroutines).

GoTEE shows that (b) works with an unmodified TamaGo runtime on both sides. The supervisor's code
is about 600 lines of Go and 180 of assembly per architecture (`monitor/`). Its applet board is
about 50 lines.

### 4.4 Biscuit (https://github.com/mit-pdos/biscuit; OSDI '18)

Cody Cutler, M. Frans Kaashoek and Robert T. Morris, "The benefits and costs of writing a POSIX
kernel in a high-level language"
(https://www.usenix.org/conference/osdi18/presentation/cutler). Biscuit is a POSIX-subset
monolithic kernel for x86-64 in Go: a fork of Go 1.10.1 whose runtime changes are "mostly in
src/runtime/os_linux.go" (README).

- The Go runtime and its collector run at ring 0 for the kernel only. User processes are C
  programs (its `litc`) at ring 3 in their own address spaces, so there is no Go runtime in user
  space.
- Its hardest problem was kernel heap exhaustion, which it handled by reserving memory for each
  system call, from a static analysis of the kernel's maximum live data.
- High-level language features (mostly GC and stack checks) cost up to 13% of kernel CPU time.
  The longest GC pause for NGINX was 115 µs. The Go paths were 5-15% slower than C ones.

Biscuit is the "kernel in Go, user anything" half of (b). It confirms that a collected kernel
scanning only its own heap is practical.

### 4.5 gVisor's Sentry (https://gvisor.dev/docs/architecture_guide/platforms/; `pkg/ring0`)

gVisor's Sentry is an application kernel in Go. With the KVM platform the Sentry acts "as both
guest OS and VMM". Its Go code runs in the host process and, through `pkg/ring0`, at guest ring 0.
The sandboxed application runs at guest ring 3 in its own address spaces.

`ring0.CPU.SwitchToUser` "performs either a sysret or an iret". Hooks run on the interrupt stack
and must be `go:nosplit`. Callers wrap it in `entersyscall`/`exitsyscall`
(https://pkg.go.dev/gvisor.dev/gvisor/pkg/ring0). This is §2.3's kernel side, done in production
Go, on amd64 and arm64. The application is not Go and has no runtime that the Sentry shares, which
is (b) again.

### 4.6 Isolation in one address space, and capabilities

- **Singularity** (Hunt and Larus, "Singularity: Rethinking the Software Stack", ACM SIGOPS OSR
  41(2), 2007; https://www.microsoft.com/en-us/research/project/singularity/): software-isolated
  processes (SIPs) in one address space at ring 0. Each SIP has *its own heap and collector*.
  Isolation comes from verified, type-safe code, and messages cross through channels with an
  exchange heap whose ownership moves. That is (c) done thoroughly, and it is still a heap per
  process.
- **Theseus** (Boos et al., OSDI '20, https://github.com/theseus-os/Theseus): Rust, one address
  space, one privilege level, isolation by the language. **Tock** (Levy et al., SOSP '17,
  https://www.tockos.org/): Rust kernel "capsules" isolated by the language, and applications as
  processes isolated by the MPU. Both split the same way as §1.2: trusted code by the language,
  untrusted code by the hardware.
- **Unikernels with a privilege or compartment split**:
  - KylinX (Zhang et al., USENIX ATC '18, https://www.usenix.org/conference/atc18/presentation/zhang-yiming):
    pVM "processes" of unikernels with fork, through the hypervisor;
  - CubicleOS (Sartakov, Vilanova and Pietzuch, ASPLOS '21, https://github.com/lsds/CubicleOS):
    library-OS components isolated with Intel MPK in one address space;
  - FlexOS (Lefeuvre et al., ASPLOS '22, https://project-flexos.github.io/), on Unikraft
    (https://unikraft.org/): compartments chosen at build time, backed by MPK, EPT or none.

  MPK-style keys (Intel PKU; arm64's POE in ARMv8.9 and later) would give "domains" inside one
  address space that change without a system call. This host's CPU has PKU (Zen 3; the flag shows
  in the host's `/proc/cpuinfo`, and KVM can pass it to a guest). The M2 has no POE. Keys protect
  data only, not privileged instructions, and an unprivileged `WRPKRU` changes them, so they need
  a trusted code generator, as CubicleOS and FlexOS have. They are a later option for amd64, not a
  base.
- **seL4** (Klein et al., SOSP '09, https://sel4.systems/): a capability microkernel. A process
  holds capabilities (handles to kernel objects with rights) and can name nothing else. The
  kernel is formally verified. For Arbace the relevant part is the shape: §3.4's handles, given
  by the kernel per process (a store view, a network policy, a spawn right), are capabilities.
  S7 already passes such a view to a tool guest.
- **In Clojure's world**: clojail (https://github.com/flatland/clojail) and SCI's allow and deny
  lists (https://github.com/babashka/sci) sandbox evaluation by restricting what the analyzer
  resolves. Erlang/BEAM gives each process its own heap and copies messages, which is (b)'s
  value-passing rule inside one VM.

## 5. Costs

### 5.1 Measured crossings (amd64, a TamaGo guest on the box's board, nested KVM)

Three rounds of 10⁶ iterations each; the doorbell was measured over 20,000 calls. The TSC ran at
2,445 MHz.

| crossing | ns | cycles |
|---|---:|---:|
| a Go call (noinline) | 1.9-3.0 | 4.6-7.3 |
| **SYSCALL + SYSRETQ** (ring 3 → 0 → 3, empty handler) | **24.3-25.0** | **59-61** |
| INT 0x80 + IRETQ (DPL 3 gate on IST1) | 101-109 | 248-266 |
| MOV CR3, same root, no PCID (a full TLB flush) | 96-136 | 235-332 |
| the doorbell: `abi.CPUSelf` (a VM exit, nested SVM) | 13,300-18,700 | 32-46 million per 1000 |
| for comparison: Linux `getpid` on the host (mitigations on) | 197-222 | |
| for comparison: a spawned tool guest (S7, BOX-NOTES.md) | 84-170 ms | |

Neither SYSCALL nor SYSRET exits to the hypervisor, so their cost is the CPU's even under nesting.
The doorbell's figure is the nested exit (BOX-NOTES.md gives about 1 µs on bare metal). A process
call that the kernel forwards to the monitor costs both a crossing and an exit. Only calls the
kernel answers itself are cheap.

**arm64 was not measured.** The emulated aarch64 VM (`bin/box-arm64-vm`) is TCG, so its timings
mean nothing. An SVC+ERET round trip on Apple's cores and Neoverse is commonly of the same order
as SYSCALL (tens of cycles before the OS's own work). The Mac's run of a port of the experiment
would settle it (K10).

### 5.2 The REPL's fine-grained calls

The note's concern is a REPL whose evaluated code makes many small calls. Which of them cross
depends on the option:

| option | what crosses | per REPL form, typical |
|---|---|---|
| (a) one runtime, user code at EL0, kernel pages unmapped | every allocation, every park and wake, every stack growth, write barriers during GC; calls into compiled `arbace.core` if it is kernel code | thousands: a `(reduce + (map inc (range 1000)))` allocates on the order of 10³ objects (boxed longs, chunks, seqs; an estimate, not measured), so tens of µs of crossings, about as much as the form's own run time |
| (b) a runtime per process | the process's I/O: console reads and writes, store calls, spawns, network, time (only if the counter is not readable at EL0) | a few per form: the REPL's read and its print |
| (c) the language only | nothing | none |
| (d) a guest per tool | a spawn per tool run | none per form. 84-170 ms per spawn |

Under (b) the whole Arbace program runs inside the process: the evaluator, `arbace.core`, jrt,
the collections and the Java forms' interpreter. A `first` is a Go call (2 ns). The REPL's
crossings are its I/O, a handful per form at 25-100 ns each, against the 13-18 µs exit the console
already costs today. **(b)'s crossings are invisible for a REPL. (a)'s dominate it.**

### 5.3 Memory

The amd64 guest image (`target/arbace-go/box-amd64/arbace.elf`, 102 MB on disk) has three
segments:

- text, 42.4 MB (R X);
- read-only data, 56.9 MB (R);
- data and bss, 2.6 MB on file and 3.5 MB in memory (RW).

Under (b1) a process of the same image maps the first two from the kernel's pages and copies the
third. A process costs **3.5 MB plus its heap, its stacks** (8 MB per M as TamaGo allocates them;
`abi.TaskStackBytes`) **and its page tables** (a few hundred KB with 2 MiB pages). A REPL process
with a 64 MB heap is about 75 MB. A tool guest today is a VM of 3 GiB by default (BX81 (a)), most
of it never touched. A second build linked elsewhere (b2) adds 100 MB of image per kind.

### 5.4 Complexity

| piece | estimate (lines) | prior art |
|---|---:|---|
| user page tables: per-process roots, the kernel's mappings EL1-only, the shared text | 200-300 Go | the monitor's `buildTables`, GoTEE's `SetAccessPermissions` |
| entry and exit, amd64: GDT/TSS/MSRs, IDT lower-level gates, SYSCALL entry, IRETQ/SYSRETQ, the context | 150-250 asm + Go | the experiment (250 with measurements), gVisor's `ring0` |
| entry and exit, arm64: lower-EL vectors, ERET, SP_EL0 handling, CNTKCTL, PAN | 150-250 asm + Go | GoTEE's `exec_arm.s` (180) |
| the user board: goos hooks as calls, the RAM region, `Idle`, `Task` | 100-200 Go + asm | TamaGo `user/linux` (about 150), GoTEE `applet` (about 50) |
| the kernel's call handler: validation, policy, forwarding to the monitor | 400-700 Go | `vmm`'s call table (the same calls) |
| the process manager: load an image (the monitor's ELF code), budgets, wait, result | 300-500 Go | `vmm/spawn.ae`, `vmm/imagefile.ae` |
| timer interrupts (preemption of processes) | 200-400 asm + Go | TamaGo's `amd64/lapic`, `arm64/timer`, `arm64/gic` |
| SMP processes | 200-400 Go | the monitor's `CPUStart`/`CPUPark` |

For comparison, (a) means patching `proc.go`, `malloc.go`, `mbarrier.go`, `stack.go`, `mgcmark.go`
and their assembly in `overlay/go`. That is the hottest code of the runtime, rebuilt into every
program, against every Go release. Its correctness argument (§3.3) still ends in "user code must be
memory-safe".

## 6. Options

### (a) User code at EL0 within the one runtime

Privilege per goroutine: a g carries its level, `gogo` returns to EL0 or ring 3 for a user g, and
kernel pages are EL1-only. One heap and one collector, as the note sketches.

- *For*: one heap, so values cross by pointer, as in the note. One collector, one scheduler.
- *Against*:
  - every allocation, park and stack growth crosses (§3.3, §5.2);
  - the collector follows pointers that user code wrote, so isolation needs memory-safe user code
    anyway;
  - it is a deep patch to Go's runtime (§5.4);
  - the experiment showed the collector itself ending up running at ring 3.
- *Verdict*: not recommended. It costs the most and isolates the least.

### (b) A second Go runtime at EL0 as a process (GoTEE's applet model)

Each user part is a TamaGo runtime instance on a user board, at EL0 or ring 3, in its own address
space (b1) or a separately linked image (b2). Its calls are the doorbell's ABI by SYSCALL or SVC.
Values cross as bytes and handles.

- *For*:
  - works on any hardware with an MMU, in the box under KVM and HVF too;
  - no change to Go's runtime;
  - crossings only at I/O (§5.2);
  - the ABI exists, so tool images run as processes or as guests unchanged;
  - resource budgets per process, as per guest today;
  - faults end a process, not the guest;
  - proven by GoTEE, TamaGo's `user/linux`, Biscuit and gVisor.
- *Against*:
  - one runtime per process (memory §5.3; a millisecond's start, estimated);
  - no shared heap: values are serialized (EDN, blobs) or put in shared pages;
  - the kernel's vCPU waits while a process runs, until there are timer interrupts (§2.6);
  - SMP processes add scheduling;
  - two privilege-level mechanisms to maintain, one per architecture.

### (c) Language-level isolation only

Namespaces and a restricted evaluator: a resolver that admits only allowed vars and classes, no
interop outside an allow list, no `eval` of unrestricted code. Capabilities are values the
sandbox is handed (a store view, a printer).

- *For*: free crossings, a shared heap, no hardware.
- *Against*:
  - Arbace's surface is large: jrt's natives, `unsafe` in the runtime, the Java forms
    interpreter, reflection;
  - one bug breaks it, and it bounds neither memory nor CPU (no preemption in TamaGo: a loop
    holds its vCPU);
  - it protects nothing against code that is not Arbace (a native tool, gc's `compile`).
- *Verdict*: right inside a process, for the REPL's own sandboxes (e.g. evaluating a form from the
  network with a store view only). Wrong as the only boundary.

### (d) Guests per tool where virtualization exists, (b) on bare metal

S7's guests stay what the box uses under KVM and HVF. On bare metal, where there is no hypervisor
unless Arbace is one, the same spawn request starts a process instead.

- *For*: the strongest isolation where available. One interface (the spawn request) for both. Tool
  images unchanged (§2.4).
- *Against*: two back ends for one interface. A guest's start (84-170 ms) is two orders above a
  process's.
- *A refinement*: processes under KVM and HVF too, for the cheap cases. A REPL's sandbox, a short
  tool run or many small actions would use processes, and the strong cases (untrusted native
  code, a whole build) guests. Which back end serves a spawn is then policy, not hardware.

### (e) Arbace as the hypervisor on bare metal

go-whim's monitor inside Arbace at EL2 or VMX root, so that S7's guests work on bare metal too.

- *For*: one isolation model everywhere.
- *Against*:
  - Arbace would become a hypervisor (EL2 page tables, VMCS or VMCB, virtual interrupts);
  - nested virtualization would be needed to develop it inside the box (not on the M2, slow
    nested here);
  - it gives no smaller crossings than (b).
- *Verdict*: later, if bare-metal guests are wanted for their own sake.

### Side by side

| | (a) one runtime, EL0 goroutines | (b) process per user part | (c) language | (d) guests + (b) | (e) Arbace hypervisor |
|---|---|---|---|---|---|
| isolation | weak (user-written pointers) | hardware, per process | as good as the runtime | strongest where possible | strongest |
| bare metal | yes | yes | yes | yes, by (b) | yes |
| REPL's call cost | crossings per allocation | none | none | none | none |
| values across | pointers | bytes, handles | pointers | bytes, blobs | bytes, blobs |
| Go runtime changed | deeply | no | no | no | no |
| start | none | about 1 ms (estimate) | none | 1 ms or 84-170 ms | tens of ms |
| effort (working days) | 25-40 | 17-27 (§6.2) | 5-10 (a sandboxing evaluator) | (b) plus 2-3 | 30+ |

### 6.1 Risks

- **TamaGo's identity mapping.** TamaGo's boards and `dma` assume VA = PA. A process under (b1)
  has its data at the kernel's VAs but other PAs. The user runtime does no DMA, so this affects
  only the kernel's translation of user pointers, which is one offset (§3.5).
- **Kernel goroutines starving** while a process runs on the only vCPU, until timer interrupts
  (§2.6). Mitigation: give processes vCPUs of their own first, as guests have.
- **The crossing code is hostile ground for Go**: nosplit, no allocation, no write barriers, the
  stack switch, `g` in R14 and FS on amd64. gVisor's and GoTEE's code show the rules. The
  experiment hit the FS base and SYSRET's encoding at once.
- **The runtime's traceback** across a crossing frame (§3.2) matters only for (a). Under (b) the
  kernel's goroutine is in an ordinary call (`Exec`) when the collector scans it.
- **Side channels** (Spectre-class, Meltdown on Intel without KPTI) are out of scope at first. The
  AMD host and Apple's cores are not affected by Meltdown. A KPTI-like split of the kernel's
  mappings is a later hardening.
- **Two mechanisms, two architectures, three hypervisor back ends** (KVM amd64, KVM arm64, HVF).
  EL0 and ring 3 are the guest's own and need nothing from the monitor (the experiment changed
  nothing in it). HVF's guest gets EL0 too, which K10's run on the Mac should still confirm.

### 6.2 The staged plan

| milestone | content | check | days |
|---|---|---|---:|
| **KU0** | the spec: `doc/go/PROCESS-SPEC.md` (the process, its address space, the call ABI one level down, faults, handles), from this survey and the user's decisions | — | 1 |
| **KU1** | amd64 mechanics in a test guest (`box/guest/cmd/`, as `faulty`): per-process tables, GDT/TSS/MSRs, lower-level gates, `Exec` and its return, a fault in user code ending the process | `guest` tests on /dev/kvm: round trip, fault, nil → the user's panic | 2-3 |
| **KU2** | the user board (`arbace/box/uboard`: TamaGo's goos hooks as calls, `user/linux`'s shape); the demo guest run as a process inside a kernel test guest | the demo's output as a process equal to its output as a guest | 2-3 |
| **KU3** | arm64 the same (lower-EL vectors, ERET, SP_EL0, CNTKCTL, PAN) | the emulated VM's KVM/arm64; the Mac bundle's checklist | 2-3 |
| **KU4** | the kernel's process manager and call handler: the store view, console, events, spawn as a process; S7's tool images as processes | `bin/box-tools`' actions as processes, byte for byte as guests | 3-5 |
| **KU5** | SMP processes (`Task` as a call, vCPUs per process) | `TestGoGuestSMP`'s guest as a process | 3-5 |
| **KU6** | timer interrupts: the board takes the LAPIC and generic timers, processes preempted | a spinning process stopped by the kernel, not the watchdog | 2-4 |
| KU7 (later) | the REPL in a process: `arbace.box` spawns a REPL process with a store view; (c)'s sandbox inside it | the box's smoke test through a process | 2-3 |

KU1-KU4 change only `box/` (the board, a new user board, test guests) and add no runtime patch.
`overlay/go` is untouched. KU6 changes the board's design rule (interrupts off).

## 7. Recommendation and proposals

**Recommendation.** Take (b): the process, a TamaGo runtime at EL0/ring 3 with its own heap,
collector and scheduler, as the unit of isolation that works on any hardware.

- Its system calls are the doorbell's ABI one level down, and the kernel validates them and
  forwards them.
- Values cross as bytes and handles; the handles are where capabilities live.
- Keep (c) inside a process for fine-grained sandboxes, and (d), S7's guests, where a hypervisor
  is there, behind the same spawn request.
- Do not build (a). The note's "collector and scheduler at EL1" holds for the kernel's own heap.
  A user part's memory is managed by the user part's own runtime, because a collector that manages
  memory user code writes cannot be isolated from it (§3.3, §3.2's experiment).

The proposals:

- **K1 The model.**
  - Options:
    - (a) one runtime, privilege per goroutine (§6 (a));
    - (b) a runtime per process (§6 (b));
    - (c) the language only;
    - (e) Arbace as hypervisor.
  - *Recommended: (b), with (c) inside processes.*
- **K2 The system call ABI.**
  - Options:
    - (a) the doorbell's call block and numbers, by SYSCALL/SVC with the block's address in
      RAX/X0, plus a few process calls (§2.4);
    - (b) GoTEE's registers r0-r2 and JSON-RPC over read and write;
    - (c) a new ABI.
  - *Recommended: (a).* Tool images then run as guests or processes unchanged.
- **K3 The address space.**
  - Options:
    - (a) (b1): the same image, a page table per process, text and read-only data shared, the
      kernel's mappings EL1-only in every table, PCID and ASID later;
    - (b) (b2): images linked per slot in one address space.
  - *Recommended: (a).*
- **K4 Values across.**
  - Options:
    - (a) bytes only (call buffers, the store's blobs, streams), kernel objects as handles;
    - (b) also shared pages mapped in both, for large transfers (later);
    - (c) shared heap objects.
  - *Recommended: (a) now, (b) when a measurement asks for it; not (c).*
- **K5 The kernel's contents.**
  - Options:
    - (a) the runtime, the board, the drivers, the store and its views, the process manager, the
      call handler; the compilers and the REPL in processes;
    - (b) the compilers in the kernel too, as the note lists.
  - *Recommended: (a).* The compilers read untrusted input, and S7 already isolates them.
- **K6 Guests and processes.**
  - Options:
    - (a) one spawn request, two back ends (a guest where a hypervisor is there, a process
      otherwise or on request);
    - (b) processes only;
    - (c) guests only (today).
  - *Recommended: (a).*
- **K7 Faults.**
  - Options:
    - (a) a nil dereference in a process becomes its own panic (the board's BX60 trick, one level
      down); any other fault ends the process with a Fault-like report as its result; a kernel
      fault still ends the guest;
    - (b) every fault ends the process.
  - *Recommended: (a).*
- **K8 Preemption.**
  - Options:
    - (a) processes get vCPUs of their own first (no interrupts), timer interrupts later (KU6);
    - (b) timer interrupts from the start.
  - *Recommended: (a).* It keeps the box's interrupt-free design until KU6.
- **K9 The staged plan.**
  - Options:
    - (a) KU0-KU6 as §6.2, amd64 first, each stage in test guests before Arbace's program uses it;
    - (b) arm64 first (the Mac).
  - *Recommended: (a).*
- **K10 Measurements to complete.**
  - Options:
    - (a) the experiment ported to arm64 and run on the Mac (HVF) and in the emulated VM for
      correctness; a process's start time and memory measured at KU2;
    - (b) none until KU3.
  - *Recommended: (a).* The Mac run also confirms that HVF gives the guest EL0.
- **K11 Arbace as a hypervisor on bare metal (§6 (e)).**
  - Options:
    - (a) not now, recorded as Later;
    - (b) surveyed next.
  - *Recommended: (a).*
- **K12 The REPL's sandbox (c).**
  - Options:
    - (a) a restricted evaluator (allowed vars and classes, capabilities as values) inside a
      process, surveyed after KU4;
    - (b) none.
  - *Recommended: (a), later.*

## 8. Sources

- Arbace (at `ca4d3eed`):
  - doc/AGENDA.md (the note);
  - doc/go/BOX-SURVEY.md (§5, exit costs);
  - doc/go/BOX-NOTES.md (S1-S8: BX60, BX81, S7's spawn and its measurements);
  - doc/STANDALONE-NOTES.md (S7);
  - `box/README.md`;
  - `box/go/arbace/box/vmm/setup_amd64.ae`, `setup_arm64.ae`, `layout.ae`;
  - `box/go/arbace/box/board/boot_amd64.s`, `boot_arm64.s`, `board.ae`, `smp.ae`;
  - `box/go/arbace/box/abi/abi.ae`;
  - `box/go/arbace/box/guest/` (the test guests, `Build`);
  - `tamago/goos/go/github_com/usbarmory/tamago/{amd64,arm64}/mmu.ae`, `user/linux.ae`;
  - `target/arbace-go/box-amd64/arbace.elf` (segments).
- TamaGo v1.27.1 (`/root/go/pkg/mod/github.com/usbarmory/tamago@v1.27.1`): `amd64/mmu.go`,
  `arm64/mmu.go`, `user/linux/runtime.go`, `user/linux/syscall_amd64.s`. TamaGo's go1.27.1
  runtime: `/root/tamago-go/src/runtime/os_tamago.go` (`newosproc`, `preemptM`,
  `preemptMSupported`), `rt0_tamago_amd64.s`, `signal_tamago.go`, `idle_tamago.go`.
- GoTEE: https://github.com/usbarmory/GoTEE at `813dc65a89241b78e307fbcec1174c1b7baa3460`
  (2026-10-09): README, `monitor/exec_arm.go`, `monitor/exec_arm.s`, `monitor/handler.go`,
  `monitor/syscall.go`, `monitor/rpc.go`, `syscall/syscall.go`, `syscall/syscall_arm.s`,
  `syscall/rpc.go`, `applet/applet.go`, `applet/nanotime_arm.go`, `monitor/exec_riscv64.go`;
  the example https://github.com/usbarmory/GoTEE-example (README excerpt in GoTEE's README).
- Biscuit: https://github.com/mit-pdos/biscuit (README); Cutler, Kaashoek, Morris, OSDI '18,
  https://www.usenix.org/conference/osdi18/presentation/cutler (abstract).
- gVisor: https://gvisor.dev/docs/architecture_guide/platforms/;
  https://pkg.go.dev/gvisor.dev/gvisor/pkg/ring0.
- Singularity: https://www.microsoft.com/en-us/research/project/singularity/ (Hunt, Larus, OSR
  2007). Theseus: https://github.com/theseus-os/Theseus. Tock: https://www.tockos.org/.
- KylinX: https://www.usenix.org/conference/atc18/presentation/zhang-yiming. CubicleOS:
  https://github.com/lsds/CubicleOS. FlexOS: https://project-flexos.github.io/. Unikraft:
  https://unikraft.org/.
- seL4: https://sel4.systems/.
- clojail: https://github.com/flatland/clojail. SCI: https://github.com/babashka/sci.
- Architecture:
  - AMD64 Architecture Programmer's Manual vol. 2 (SYSCALL/SYSRET, STAR/LSTAR/SFMASK, IRET's
    segment nulling, CR4.SMEP/SMAP);
  - Arm Architecture Reference Manual for A-profile (D5 stage 1 permissions AP and UXN/PXN, PAN,
    the vector table's lower-EL entries, ESR_EL1.EC 0x15/0x20/0x24, CNTKCTL_EL1), as TamaGo's
    `arm64/mmu.go` cites its sections.

## Appendix A: the experiment

The experiment ran in `.tmp/kernel-user-survey/` and is not tracked. It was built and run as
follows:

1. `bin/box-vendor --work .tmp/kernel-user-survey/bv print .tmp/kernel-user-survey/print`
   printed `box/` (on the main checkout's stage 0, `ARBACE_HOST_EXE`).
2. In the printed module, a guest `box/guest/cmd/ring3` (`main.go`, `ring3_amd64.s`) was added to
   `guest.Programs`, with a test `TestRing3` that runs it under `vmm.Run` on a recording host.
3. It was built by `guest.Build` (TamaGo's go1.27.1, `GOOS=tamago`, the board) and run by
   `go test -run TestRing3 arbace/box/guest` on /dev/kvm.

The guest, at ring 0 on the board:

- copies the monitor's GDT (SGDT) and adds user data at 0x30 and user code at 0x38 (DPL 3), then
  LGDT;
- copies the IDT's 32 gates and adds vector 0x80, an interrupt gate of DPL 3 on IST1 whose
  handler is a bare IRETQ, then LIDT;
- sets EFER.SCE, STAR (`0x28<<48 | 0x08<<32`), LSTAR (an entry that SYSRETQs at once for RAX 0
  and, for RAX 1, restores the saved kernel SP, BP and R14 and returns into Go), and FMASK;
- builds identity page tables for 16 GiB with 2 MiB pages, P|RW|U at every level, in a Go slice,
  and writes CR3;
- `enterUser`: IRETQ to ring 3 at an assembly loop that makes 10⁶ SYSCALLs (or INT 0x80s) between
  two RDTSCs, then reports by a last SYSCALL;
- `enterUserGo`: loads FS with the user data selector and puts IA32_FS_BASE back, then IRETQ to
  ring 3 at a trampoline that calls the Go function `userWork` (through its ABI0 wrapper, which
  finds g in TLS) on the current goroutine's stack 8 KiB below SP;
- measures in ring 0 for comparison: MOV CR3 of the same root, a noinline Go call, and
  `board.Call(abi.CPUSelf)`.

The monitor was not changed. CR4 was 0x620 (PAE, OSFXSR, OSXMMEXCPT: no SMEP, SMAP or PCID).
Results: §3.2 and §5.1. Two pitfalls from the run:

- Go's assembler's `SYSRET` lacks REX.W, so it returns to compatibility mode. It must be written
  as bytes.
- A sysentry that writes its result into the caller's frame by offset corrupts a frame of another
  size. The result went to a global instead.

The host's `getpid` figure came from a linux/amd64 program built with the same toolchain
(`RawSyscall(SYS_GETPID)` and `syscall.Getpid`, 2·10⁶ calls, three rounds).

## Decided (2026-10-17, the user)

K1-K12 as recommended: a TamaGo runtime per process at EL0 (b1, the same image, a page table
per process, text shared), language isolation inside processes (K1, K3); the doorbell's call
block by SYSCALL/SVC (K2); bytes and handles across, never shared heap objects (K4); the
compilers and the REPL outside the kernel (K5); one spawn request, guest or process (K6); faults:
a nil dereference the process's panic, others end the process (K7); vCPUs per process first,
timer preemption at KU6 (K8); the plan KU0-KU6, amd64 first (K9); the experiment ported to arm64
and run on the Mac (K10); Arbace as hypervisor not now (K11); a capability evaluator later (K12).
KU0, the spec, now.
