# The host VM and the box: a survey of their boundaries

Status: survey, 2026-10-13, on main `68e00ce`, for the standalone survey's next stages (S5-S8,
[../STANDALONE-SURVEY.md](../STANDALONE-SURVEY.md) §9.1: the box rebuilding Arbace inside
itself) and the box's design ([BOX-SURVEY.md](BOX-SURVEY.md), [BOX-NOTES.md](BOX-NOTES.md)).
Nothing here is implemented. The proposals (HV1-HV9, §14) are the user's to decide.

## Why

Arbace's work runs inside a cloud virtual machine (the host VM: Alpine edge,
[../ALPINE.md](../ALPINE.md)), and Arbace's box runs as a guest nested inside it: go-whim's
monitor, vendored as `box/`, drives `/dev/kvm` of the host VM's kernel, which is itself a guest
of the cloud's hypervisor. Two virtual machines are therefore stacked, made to opposite designs:

- the host VM is a general-purpose PC emulated by QEMU (firmware, ACPI, PCI, virtio devices, a
  boot loader, a whole Linux distribution);
- the box is a single Go program in ring 0 or EL1, with no firmware, no devices and no
  interrupts. Everything it gets from outside passes through one doorbell page and about 40
  calls.

S5-S8 ask the box to do what the host VM does every day: hold a source tree, run a compiler
toolchain, use several gigabytes of memory for a long time, and keep its results. This survey
measures the host VM's boundaries, sets the box's beside them, and asks what the box lacks and
what the host VM's design teaches.

## Method and redactions

- **Read-only.** No system configuration was changed and no kernel module loaded or unloaded.
  Nothing was installed. The only things written were the scratch files in the worktree's
  `.tmp/hv/` (a 30-line CPUID probe built with the host's `gcc`, the box's probe forms, a
  128 MiB `dd` file that was deleted again) and this document. No network was scanned, and the
  cloud's metadata service was not queried. cloud-init's user-data, SSH keys, credentials and
  `/etc/shadow` were not read.
- **Redacted.** Host names, addresses (IPv4, IPv6, MAC), file-system and partition UUIDs and
  resolver addresses are written as `<redacted>` or by their class only (private, public,
  link-local). The cloud provider was not sought and is not named.
- **Measurements were small.** The I/O figures come from at most 256 MiB of `dd` reads and
  128 MiB of writes. The box probes ran for under a minute each. The machine was shared: a
  `bin/arbace-go --fixpoint` of other work ran during the survey (load average 1 at the start,
  11 at the box probes), so the timings are indicative.
- Each fact names its source: the command, the `/proc`/`/sys` file or the document it comes
  from. Inferences are marked *(inferred)*.

## Summary

- **The host VM is QEMU/KVM, not a cloud-specific platform.** It is a Q35 machine
  (`pc-q35-9.2`) with OVMF firmware (EDK II, UEFI 2.7, Secure Boot off), the KVM CPUID
  signature, and only virtio devices: net, two blk, rng, balloon and a console that carries the
  QEMU guest agent. It runs 64 vCPUs of an AMD EPYC 7763 and booted with 64 GiB of RAM, of
  which the balloon takes 33 GiB back, leaving 29.7 GiB visible. It has no swap, a 120 GiB
  virtio disk (ext4, 68 GB free), IPv4 only (IPv6 is link-local), no firewall inside, and root
  with every capability and no cgroups mounted.
- **Nested SVM is exposed with a reduced feature set.** The guest sees nested paging, VMCB
  clean bits, TSC scaling, vGIF and virtual VMLOAD/VMSAVE. It does not see decode assists,
  AVIC, vNMI or the invariant-TSC bit, and it has only 16 ASIDs. The missing decode assists
  plausibly explain why the box's exits cost 16-18 µs here against about 1 µs on bare metal
  *(inferred)*.
- **The box is small on every axis.** It has 1-32 vCPUs (4 by default) and a fixed 3 GiB slot
  whose collector limit is 2 GiB. Its RAM is never given back to the host. It has no block
  device: its file system is TamaGo's in memory (`/dev`, `/tmp`), lost at exit, and the store
  (blobs of at most 256 MiB, in a host directory) is its only persistence. It has no network
  unless the launcher allows one (then a TCP stream proxy under a policy). It has no processes,
  no firmware and no devices. Its clock is the host's at entry plus the TSC.
- **For S5-S8 the gaps are memory and files first.** The tools need far more memory than the
  3 GiB slot:

  | what runs | peak memory |
  |---|---:|
  | a2g on Go | 10.1 GB RSS |
  | gc compiling `jrt` | 7.1-8.0 GB |
  | the program's printing | 4.6 GB |

  Then come a source and output tree that cannot live in a heap-backed file system inside such
  a slot, a way to run gc (S7's tool guests), and resource budgets for several guests at once.
  Time and entropy are adequate. The network is not needed if inputs come through the store.
- **What the host VM teaches:**
  - put RAM above the 4 GiB hole rather than moving the device page;
  - let memory flow back to the host (balloon, free-page reporting);
  - give storage a block device's shape: content outside the guest's RAM, read on demand;
  - keep the control channel narrow (the host VM's QEMU agent allows every command);
  - mark the clock's stability honestly.

## 1. The hypervisor and the platform

### The host VM

| fact | value | source |
|---|---|---|
| hypervisor | KVM: CPUID leaf `0x40000000` signature `KVMKVMKVM`, max leaf `0x40000001`; no Hyper-V leaves at `0x40000100`; CPUID.1:ECX[31] (hypervisor) = 1 | `.tmp/hv/cpuid` (the probe), `dmesg`: "Hypervisor detected: KVM", "Booting paravirtualized kernel on KVM" |
| KVM paravirtual features | CPUID `0x40000001` EAX `0x01007efb`: clocksource and clocksource2, NOP_IO_DELAY, async PF (with VM exit and interrupt delivery), steal time, PV EOI, PV unhalt, PV TLB flush, PV send IPI, poll control, PV sched yield, and the clocksource-stable bit. Not offered: MSI extended destination ID, `HC_MAP_GPA_RANGE`, migration control | the probe; `dmesg` ("kvm-guest: PV spinlocks enabled", "setup PV IPIs", "PV sched yield") |
| VMM and machine | QEMU, "Standard PC (Q35 + ICH9, 2009)", machine version `pc-q35-9.2`; ACPI tables made by Bochs/BXPC (RSDP, XSDT, FACP, DSDT, APIC, HPET, MCFG, WAET); `qemu_fw_cfg` present | `/sys/class/dmi/id/{sys_vendor,product_name,product_version}`, `dmesg` (ACPI), `/sys/firmware/qemu_fw_cfg` |
| firmware | EDK II (OVMF), UEFI 2.7, 64-bit (`fw_platform_size` 64), BIOS date 02/02/2022, version "unknown"; SMBIOS, ACPI 2.0, MEMATTR and an EFI RNG table handed to the kernel; Secure Boot disabled | `/sys/class/dmi/id/bios_*`, `/sys/firmware/efi/*`, `dmesg` ("efi: EFI v2.7 by EDK II", "Secure boot disabled") |
| paravirtual devices | virtio only (PCI vendor `1af4`): net, rng, balloon, console, two blk. No ENA, gVNIC, MANA or Hyper-V devices, although the image loads those drivers (the cmdline's `modules=...ena,gve,mana`, the initramfs's `pci-hyperv`): a generic cloud image *(inferred)* | `/sys/bus/virtio/devices/*`, `dmesg` (pci), `lsmod` |
| clock source | `kvm-clock` (current), also `tsc`, `hpet`, `acpi_pm`; the TSC detected at 2445.404 MHz | `/sys/devices/system/clocksource/clocksource0/*`, `dmesg` |
| nested virtualization (this VM as L1) | `kvm_amd` loaded: "Nested Virtualization enabled", "Nested Paging enabled", TSC scaling, LBR virtualization, virtual VMLOAD/VMSAVE, virtual GIF; parameters `nested=1 npt=Y avic=N vnmi=N vgif=1 vls=1 tsc_scaling=1 nrips=1 pause_filter_count=3000`; `kvm`: `tdp_mmu=Y halt_poll_ns=200000 enable_pmu=Y nx_huge_pages=N eager_page_split=Y` | `dmesg`, `/sys/module/kvm{,_amd}/parameters/*` |
| SVM as L0 exposes it | CPUID `0x8000000A`: revision 1, **16 ASIDs**, EDX `0x1001947b`: NPT, LBRV, NRIPS, TSC rate MSR, VMCB clean, flush by ASID, pause filter and threshold, virtual VMSAVE/VMLOAD, vGIF, bit 28. **No** SVM lock, **no decode assists**, no AVIC or x2AVIC, no vNMI, no GMET, no supervisor shadow stack | the probe |
| `/dev/kvm` | `crw-rw-rw- root kvm` (world read-write) | `ls -l /dev/kvm` |
| nested guest time since boot | 84 s in `/proc/stat`'s guest column (the box's and other KVM users' runs) | `/proc/stat` |

### The box

- **Monitor**: go-whim's, vendored as Go forms (`box/go/arbace/box/{hv,vmm}`), run by the launcher
  `arbace-box`. It is one Linux process of the host VM: one VM per process, each vCPU on its own
  locked thread (BOX-SURVEY §2.2-2.3). On amd64 it uses KVM by raw ioctls; on arm64 it uses
  KVM/arm64 (checked in an emulated aarch64 VM) or Hypervisor.framework through purego on the
  Mac.
- **Nesting**: here the box is an L2 guest. The cloud's KVM is L0, the host VM's `kvm_amd` is
  L1, and the box is L2.
- **Platform**: there is none in the PC sense. The box has no firmware, ACPI, PCI, interrupt
  controller or timer device. The monitor writes the page tables (amd64: 4 levels with 2 MiB
  pages, long mode, ring 0; arm64: EL1h with the MMU on) and starts the image at its entry with
  the registers set (BOX-SURVEY §2.3).
- **Paravirtual interface**: one MMIO page, the doorbell at `0xf0000000`
  (`box/go/arbace/box/abi/abi.ae`: `Doorbell`). The guest stores the address of a 64-byte call
  block there and exits. The calls are numbered 1-29 (go-whim's, of which Arbace uses 12-14),
  40-52 (the network) and 53 (the event area), with `abi.NCalls` 54 (BOX-NOTES S1 BX8, S5 BX40,
  BX50).
- **Clock source**: the TSC, scaled by `KVM_GET_TSC_KHZ` (`hv/tsc_linux_amd64.ae`), or
  `CNTVCT_EL0` on arm64. The board's `Nanotime` computes it in the guest, without an exit.
- **Nesting from inside**: the box cannot nest a VM of its own. It has no processes, and running
  another guest would be the monitor's job (S7's spawn call).

## 2. CPU

### The host VM

| fact | value | source |
|---|---|---|
| model | AMD EPYC 7763 64-Core Processor (Zen 3, family 25 model 1 stepping 1), microcode `0xa0011d1` | `/proc/cpuinfo` |
| vCPUs | 64; one package, one die, 64 cores, no SMT (`siblings` 64, `thread_siblings_list` 0, `smt/control` notsupported); `nr_cpu_ids` 64 | `/proc/cpuinfo`, `/sys/devices/system/cpu/cpu0/topology/*`, `dmesg` |
| caches as presented | L1d 64K, L1i 64K, L2 512K, L3 16 MiB shared by all 64 vCPUs. A physical 7763 has 32 MB of L3 per 8-core CCD: the topology is synthetic and does not show the physical layout *(inferred)* | `/sys/devices/system/cpu/cpu0/cache/index*/` |
| address sizes | 48 bits physical, 48 bits virtual (CPUID `0x80000008` EAX `0x303030`) | `/proc/cpuinfo`, the probe |
| virtualization flags | `svm npt nrip_save tsc_scale vmcb_clean flushbyasid pausefilter pfthreshold v_vmsave_vmload vgif lbrv`; absent: `avic`, `x2avic`, `decodeassists`, `vnmi`, `sev*` | `/proc/cpuinfo` flags |
| flags for Go and TamaGo | `x2apic`, `tsc_deadline_timer`, `tsc_known_freq`, `rdtscp`, `rdpid`, `rdrand`, `rdseed`, `avx avx2 fma bmi1 bmi2 adx sha_ni aes vaes vpclmulqdq`, `fsgsbase`, `pcid invpcid`, `pku`, `xsave xsavec xsaveopt xsaves`. No AVX-512 (Zen 3). XCR0 supports x87, SSE, AVX and PKRU (CPUID `0xD` EAX `0x207`) | `/proc/cpuinfo`, the probe |
| invariant TSC | **not advertised**: CPUID `0x80000007` EDX[8] = 0, no `constant_tsc` or `nonstop_tsc` flag. The kernel therefore uses kvm-clock, whose stable bit is set | the probe, `/proc/cpuinfo` |
| frequency scaling | none visible (no `cpufreq` directory) | `/sys/devices/system/cpu/cpu0/` |
| scheduler | `CONFIG_PREEMPT_DYNAMIC` with mode `none` (server); `HZ=1000`; `NO_HZ_FULL` | `/sys/kernel/debug/sched/preempt`, `/boot/config-6.18.56-0-virt` |
| overcommit evidence | steal time 0.047% of all CPU time since boot | `/proc/stat` |
| mitigations | Retpolines, Safe RET (SRSO), IBPB conditional; "Transient Scheduler Attacks: Vulnerable: No microcode" | `dmesg`, `/sys/devices/system/cpu/vulnerabilities/*` |

### The box

- **vCPUs**: 1 to 32 (`abi.MaxCPUs`: one page of the amd64 TSSs, 128 bytes apart). The default
  is 4, or the host's count when fewer (`ARBACE_BOX_CPUS=N|host`). All are created before the
  guest runs (BOX-SURVEY §2.3, BOX-NOTES S1 BX9).
- **Measured** (`.tmp/hv/probe.ae`): `(.availableProcessors (Runtime/getRuntime))` is 1 or 4, as
  given.
- **CPUID**: what KVM supports on the host VM, set by `KVM_GET_SUPPORTED_CPUID` and
  `KVM_SET_CPUID2` (`hv/kvm_linux_amd64.ae`). The board enables SSE without XSAVE, so the guest
  has **no AVX** (BOX-SURVEY §2.5). Go's runtime then takes its SSE paths.
- **Scheduling**: there is no `sysmon` and no asynchronous preemption, and Ms are bound to Ps
  (TamaGo's runtime). The evaluator yields every 65,536 method runs and recurs (BX5, BX16). An
  idle vCPU parks in the monitor (a host futex), and events end the park (BX50-BX51).
- **Measured cost** (BOX-NOTES S2): about 1.15-1.2 times native for the evaluator. `fold`
  scales 3.2-3.4 times on 4 vCPUs.
- **Measured here today**: `-e nil` started in 0.57-0.94 s at 4 vCPUs, against 0.20-0.31 s for
  the native executable (best of 3, on the loaded host).
- **Exit counts** (`ARBACE_BOX_STATS` of the probe): 27 exits on 1 vCPU; 1,708 exits on 4
  vCPUs, of which 1,595 were `cpu_park`.

## 3. Memory

### The host VM

| fact | value | source |
|---|---|---|
| RAM at boot | 67,065,548 KiB (64 GiB): "Memory: 65731048K/67065548K available"; RAM in two ranges, below about 2 GiB and from `0x100000000` (4 GiB) to `0x107fffffff`. The PC's hole below 4 GiB holds the firmware and the PCI windows | `dmesg` |
| RAM now | `MemTotal` 31,161,232 kB (29.7 GiB), `MemAvailable` 29.4 GB | `/proc/meminfo`, `free -m` |
| balloon | virtio-balloon, **8,659,792 pages inflated (33.0 GiB)**, none deflated. Its features (bits of `features`): the stats queue and free-page reporting; not deflate-on-OOM *(read from the bit string)*. Earlier records give the host 33 GB (BOX-NOTES S4) and the user 36 GB: the outer host changes the balloon *(inferred)* | `/proc/vmstat` (`balloon_inflate`, `nr_balloon_pages`), `/sys/bus/virtio/devices/virtio2/features`; `CONFIG_PAGE_REPORTING=y` |
| balloon, later (2026-10-13, after noon) | `free`: 65,800,400 KiB total (62.8 GiB), 61 GiB available: the balloon deflated by the outer host, the full 64 GiB nearly back. Without deflate-on-OOM or swap, plan around the low watermark seen (29.7 GiB) and take the rest as headroom | `free`, `/proc/vmstat` |
| balloon, evening (2026-10-13) | `free`: 19,367 MiB total (18.9 GiB), 17.6 GiB available; `/proc/vmstat` `nr_balloon_pages` 11,491,993 (43.8 GiB held). The outer host inflates and deflates at will: the floor is now 18.9 GiB, so one memory-heavy job at a time (a Go build peaks at about 10.6 GB) and a tool guest's 14 GiB slot (BX81) fits only alone | `free`, `/proc/vmstat` |
| swap | none (`SwapTotal` 0, no `/proc/swaps` entry); no zswap in use | `/proc/meminfo`, `/proc/swaps` |
| overcommit | heuristic (`overcommit_memory` 0, ratio 50), `CommitLimit` 15.6 GB (not enforced in mode 0), `max_map_count` 65,530 | `/proc/sys/vm/*`, `/proc/meminfo` |
| huge pages | THP `always` (defrag `madvise`), 434 MB of anonymous huge pages in use; no hugetlbfs pages reserved (`nr_hugepages` 0); the direct map mostly 1 GiB pages | `/sys/kernel/mm/transparent_hugepage/*`, `/proc/meminfo` |
| NUMA | one node ("No NUMA configuration found", a faked node 0) | `dmesg`, `/sys/devices/system/node` |
| KSM | off (`/sys/kernel/mm/ksm/run` 0) | sysfs |
| tmpfs ceilings | `/dev/shm` 32 GB, `/run` 13 GB (sized from boot RAM, before the balloon) | `df`, `/proc/mounts` |

### The box

- **Slot**: one span, `GoRAMBytes` = **3 GiB** above the image's base at 2 MiB, under the
  doorbell at 3.75 GiB (`vmm/tamago.ae`, `vmm/layout.ae`). It is `mmap(MAP_NORESERVE)` and
  committed as the guest touches it. arm64's layout keeps the guest in 32-bit addresses
  (STANDALONE-SURVEY §6.2).
- **Collector**: `GOMEMLIMIT` defaults to the RAM less 1 GiB (BX13). Measured: `maxMemory`
  2,147,483,648. The most live data the box can hold is about 2.3-2.4 GiB (BOX-NOTES S2's
  table).
- **No return to the host**: TamaGo's heap grows by `sbrk` and is never given back (BOX-NOTES
  S2, BX13). The monitor's one `MADV_DONTNEED` is for the 8 MiB stacks of APs started by
  `CPUStart` (`vmm/smp.ae` `release`). There is no balloon and no free-page reporting.
- **Image**: the text and read-only data are mapped privately from the launcher's file (BX11).
  The page cache shares them between boxes.
- **Resident size measured today**: 248 MB for the launcher at `-e nil`, against 161 MB native.
  The probe's run reached 279 MB.
- **Other memory**: no huge pages inside the guest beyond the monitor's 2 MiB identity map, and
  no NUMA. The host's THP backs the anonymous slot (BOX-NOTES BX11's 2 MiB faults).
- **Limits measured** (BOX-NOTES S1-S2): `harvest/reducers.ae`'s 4.9 GB reduction ran the
  3 GiB guest out of memory before BX14 fixed its leak. Several boxes run at once: the oracle
  runs 12 at a time.

## 4. Storage

### The host VM

| fact | value | source |
|---|---|---|
| block devices | `vda`: virtio-blk, 251,658,240 sectors of 512 bytes (120 GiB), 64 hardware queues, scheduler `none`, write cache "write back", discard up to 2 GiB; `vdb`: virtio-blk, 374 KiB, ISO 9660 labelled `cidata` (cloud-init's NoCloud seed, not mounted, not read) | `dmesg`, `/sys/block/*/queue/*`, `blkid` |
| partitions | `vda1` EFI system partition (vfat, 486K, `/boot/efi`), `vda2` ext4 (113 G, `/`, 40 G used, 68 G free, 38%) | `df -hT`, `blkid`, `/etc/fstab` |
| root file system | ext4 with a **1 KiB block size**, 30.8 million inodes, `metadata_csum`, journal; mounted `rw,noatime` | `dumpe2fs -h /dev/vda2`, `/proc/mounts` |
| NVMe, virtio-fs, 9p, pmem | none present (drivers are built, no devices) | `/sys/bus/virtio/devices`, kernel config |
| sequential read, O_DIRECT, 1 MiB blocks (256 MiB) | 426 MB/s | `dd if=/dev/vda bs=1M count=256 iflag=direct` |
| 4 KiB reads, O_DIRECT (4,096) | 42.5 MB/s, about 96 µs each | `dd ... bs=4k iflag=direct` |
| sequential write, O_DIRECT (128 MiB, a file in the worktree's `.tmp/`) | 574 MB/s | `dd ... oflag=direct` |
| 4 KiB writes, O_DIRECT and O_DSYNC (1,000) | 2.9 MB/s, about 1.4 ms each | `dd ... oflag=direct,dsync` |

### The box

- **No block device.**
- **Files**: TamaGo's in-memory file system (`syscall/fs_tamago.go`) through Go's `os`. It is
  empty at the start but for what TamaGo makes. Measured by the probe: `/` holds `dev` (`null`,
  `random`, `urandom`, `zero`) and `tmp`, and there is no `/etc` or `/proc`. Its contents live
  in the Go heap, so they count against the 3 GiB, and they are lost at exit.
- **Seeding and snapshots**: `ARBACE_BOX_FS=REF` seeds the file system from a tar in the store.
  `(arbace.box/save-fs! ref)` snapshots it as a deterministic tar (BX35).
- **The store** (BX30-BX38) is the box's only persistence:
  - a host directory (`--store DIR` or `ARBACE_BOX_STORE`; none by default) of `blobs/SHA256`
    and `refs/NAME`;
  - blobs of at most 256 MiB (`ARBACE_BOX_STORE_MAX`), read through the monitor's verified
    LRU cache of 256 MiB (`ARBACE_BOX_STORE_CACHE`);
  - refs changed by compare-and-set under a POSIX record lock.
- **Measured**: the probe put "hello" and read back its hash. BOX-NOTES S3 measured 1,000
  small `put`s in 88-136 ms, and a 32 MiB `put` in 129-132 ms.
- **Reads**: a blob is copied whole into the guest's heap (`get-bytes`). There is no read on
  demand: the filter allows no `pread`.

## 5. Network

### The host VM

| fact | value | source |
|---|---|---|
| interfaces | `lo`, `eth0` (virtio-net, PCI `1af4:1000`, the transitional device); one RX and one TX queue; MTU 1500 (max 65,535); TSO/GSO/GRO at 64 KiB | `ip -br link`, `ip -d link show eth0`, `/sys/class/net/eth0/*` |
| IPv4 | a private /24 with the default gateway in it, and a **public /32 alias on the same interface** (the instance's public address: a 1:1 address on the NIC rather than a NAT behind it *(inferred)*); addresses `<redacted>` | `ip -o addr` (classes only), `ip route` |
| IPv6 | **link-local only** (`fe80::/64`), no global address and no default route: egress is IPv4 only *(inferred from the routes)* | `ip -o -6 addr`, `ip -6 route` |
| configuration | static, rendered by cloud-init into `/etc/network/interfaces` (ENI) from the NoCloud seed | `/etc/network/interfaces` (its header) |
| DNS | two resolvers, both outside the private range, `<redacted>` | `/etc/resolv.conf` |
| firewall inside | **none**: no `ip_tables`/`ip6_tables` names, no netfilter module loaded, no `iptables` or `nft` binary. What filters traffic outside the VM is not visible | `/proc/net/ip_tables_names`, `lsmod`, `which` |
| listening | TCP 22 (sshd, IPv4 and IPv6), UDP 60001-60002 (mosh) | `netstat -tlnu` |
| stack settings | `ip_forward` 0, `accept_ra` 1, congestion control `cubic`, qdisc `pfifo_fast`, ephemeral ports 32768-60999 | `/proc/sys/net/*` |
| egress observed | NTP to `pool.ntp.org` (4 sources, stratum 1-2); HTTPS to `dl-cdn.alpinelinux.org` (ALPINE.md, the arm64 VM's netboot) and `api.anthropic.com:443` (BOX-NOTES "TLS"). Not probed here | `chronyc -n sources`, the records |

### The box

- **No network by default.** Measured by the probe: `(java.net.Socket. "127.0.0.1" 22)` throws
  `SocketException: Network not available`.
- **With the launcher's `ARBACE_BOX_NET` policy**: a TCP **stream proxy** (calls 40-52, BX40).
  The monitor dials, listens and resolves with Go's `net` on the host VM. The policy (BX41)
  allows listening on loopback addresses by default and dialling an allow-list of `host:port`
  patterns, and the host resolves names under the same allow-list.
- **Not offered**: UDP, ICMP and raw IP. The guest's host name is `localhost` (measured).
- **TLS** runs in the guest (Go's `crypto/tls`, Mozilla's roots built in, BX70-BX73), and the
  monitor carries cipher text only.
- **Measured** (BOX-NOTES S5): about 330 MB/s through a loopback connection, against 1.9 GB/s
  natively. Waits are woken by events (BX50).
- **The two policies stack**: the box's addresses and families are the host VM's, as filtered by
  the launcher's policy. Without a global IPv6 address the box has no IPv6 egress either
  *(inferred)*.

## 6. Devices and I/O

### The host VM

| fact | value | source |
|---|---|---|
| PCI | `00:00.0` Q35 host bridge (`8086:29c0`); `00:01.0` Bochs VGA (`1234:1111`, simpledrm and bochs DRM); `00:02.0` virtio-net; `00:03.0` virtio-rng (`1af4:1005`); `00:04.0` virtio-balloon (`1af4:1002`); `00:05.0` virtio-console (`1af4:1003`); `00:06.0`, `00:07.0` virtio-blk (`1af4:1001`); `00:1f.0` ICH9 LPC, `00:1f.2` ICH9 AHCI, `00:1f.3` ICH9 SMBus | `dmesg` (pci lines); `lspci` is not installed |
| consoles | `ttyS0` (16550A at `0x3f8`, the kernel's console and GRUB's, 115200 8N1); `tty0` (framebuffer); `ttyS1-3` present without a UART; one virtio-serial port, `org.qemu.guest_agent.0` (`/dev/vport3p1`) | `/proc/tty/driver/serial`, `/proc/consoles`, `/sys/class/virtio-ports/*/name` |
| guest agent | `qemu-ga` 11.1.2 running on the virtio-serial port with **no `--block-rpcs`**: the outer host may use every agent command, including `guest-exec` and `guest-file-*` *(inferred from the defaults)* | `/etc/init.d/qemu-guest-agent`, `ps` |
| RNG | `virtio_rng.0` as `hw_random` (quality 1024); CPU `rdrand` and `rdseed`; an EFI RNG seed at boot ("crng init done" at 0.000000 s); `entropy_avail` 256 of 256 | `/sys/class/misc/hw_random/*`, `dmesg`, `/proc/sys/kernel/random/*` |
| TPM | none (no `/dev/tpm*`); the firmware records a variable for TPM2 PCR banks, but no TPM device exists | `ls /dev/tpm*`, efivars names |
| watchdog | no device (`/sys/class/watchdog` empty); the kernel's `watchdogd` thread only | sysfs, `ps` |
| other | RTC `rtc_cmos` and `rtc-efi`; i8042 keyboard and mouse; efi pstore; no vsock, no pvpanic, no PTP clock device | `dmesg`, `/sys/bus/platform/devices` |

### The box

- **No devices.** The doorbell's calls stand in for all of them (BOX-SURVEY §2.4, BOX-NOTES).
- **Console**:
  - input: `WaitRead` without a wait, with the event area waking the reader (BX50, BX53);
  - output: `Write` for standard output, and `Message` for standard error and TamaGo's `Printk`
    (256-byte lines);
  - the launcher is a cooked console, run under rlwrap on a terminal; Ctrl-C ends the launcher.
- **RNG**: `Random`, the host's `crypto/rand`. TamaGo's runtime seeds once through
  `getRandomData`. Measured: 8 `random` calls in the probe, which made 4 `SecureRandom`s.
- **None of these**: TPM, watchdog device or RTC. The monitor has a watchdog that ends a
  silent guest, off by default (`ARBACE_BOX_WATCHDOG`; BOX-NOTES S2).
- **Faults**: reported by `Fault`. A nil dereference becomes a `NullPointerException`
  (`board.NilPanics`, amd64 BX8 and arm64 BX60).
- **Guest devices as files**: TamaGo's `/dev/{null,random,urandom,zero}` in the in-memory file
  system (measured).

## 7. Isolation and privilege

### The host VM

- **Privilege inside**: the tools run as root (uid 0) with every capability (`CapEff`
  `000001ffffffffff`), `Seccomp: 0` and `NoNewPrivs: 0` (`/proc/self/status`).
- **Namespaces**: all are the initial ones (the kernel's fixed initial inode numbers
  `4026531832`-`4026531839`, `ls -l /proc/self/ns`). init is busybox with OpenRC
  (`/proc/1/exe`, `rc-status`).
- **cgroups**: supported by the kernel (`cgroup`, `cgroup2` in `/proc/filesystems`) but **not
  mounted** (`/sys/fs/cgroup` empty, `/proc/self/cgroup` empty). No process has a resource
  limit beyond `ulimit` (§9).
- **LSMs**: `lockdown,capability,landlock,yama` (`/sys/kernel/security/lsm`). Lockdown is
  `[none]`. AppArmor is built (`CONFIG_SECURITY_APPARMOR=y`) but not active. Yama
  `ptrace_scope` is 1.
- **Hardening**:
  - `kexec_load_disabled` 1 and `unprivileged_bpf_disabled` 1;
  - `dmesg_restrict` 1, `kptr_restrict` 0;
  - `modules_disabled` 0: modules are signed (`CONFIG_MODULE_SIG=y`), but the kernel does not
    require it (no `CONFIG_MODULE_SIG_FORCE`);
  - `perf_event_paranoid` 2 (`/proc/sys/kernel/*`, the kernel config).
- **`/dev/kvm`** is mode 0666: any local user could make VMs.
- **What the VM can see of the outside**:
  - the hypervisor's identity (KVM, QEMU 9.2's machine type, OVMF);
  - the CPU model (passed through by name);
  - steal time and the balloon's size;
  - which SVM features are offered.
- **What the VM cannot see**:
  - the L0 host and its other tenants;
  - the physical topology (§2's synthetic caches);
  - the firewall in front of it;
  - the metadata service, which was not queried.
- **The outer host's view in**: the outer host has a control channel in, through the guest
  agent, with every command allowed (§6).

### The box

- **The guest's privilege**: it runs in ring 0 or EL1, but only within its slot. It sees
  identity-mapped memory: text RX, data NX, page 0 and the doorbell unmapped.
- **Every interaction is a call** the monitor checks. The monitor bounds-checks every guest
  pointer against the slot, and an unknown call is a fault (BOX-SURVEY §2.3, `vmm.ae`).
- **The monitor's own confinement**:
  - after the VM is built it runs under a seccomp filter of about 45 system calls, with
    `KVM_RUN` and `KVM_GET/SET_SREGS` as the only ioctls (`vmm/seccomp_linux_amd64.ae`);
  - the network's filter adds the socket calls, `uname` and `eventfd2`, and only when there is
    a network (BX41);
  - on macOS there is no filter: the hypervisor entitlement alone.
- **Policy points**, all in the launcher:
  - the environment's allow-list (`TZ`, `LANG`, `LC_*`, `GOGC`, `JAVA_TOOL_OPTIONS`,
    `ARBACE_*` but `ARBACE_BOX_*`, and `ARBACE_BOX_ENV`'s names; BX9);
  - the network's policy (BX41);
  - the store's directory and size limits.
- **What the guest sees of itself** (measured by the probe):
  - `os.name` `Tamago`, `user.dir` `/`, `user.home` and `user.name` `?`;
  - the environment is only what the launcher passed (`LANG`, `LC_COLLATE` in this shell);
  - no host files;
  - processes refused: `ProcessBuilder.start` fails with error 38, "Function not implemented"
    (D13).
- **What the box does not have**: cgroups, namespaces or users inside it. The monitor is one
  host process, subject to whatever confines that process. Here nothing does: no cgroups, and
  the launcher runs as root.

## 8. Time, entropy and boot

### The host VM

| fact | value | source |
|---|---|---|
| clock | kvm-clock with the stable bit; the TSC is known-frequency but not marked invariant; RTC `rtc_cmos` set the system clock at 1.555 s | `dmesg`, the probe |
| synchronization | chrony to `pool.ntp.org` (`iburst`, `makestep 1.0 3`, `rtcsync`): stratum 2, 11 µs slow of NTP, RMS offset 258 µs, frequency 29.991 ppm slow, skew 0.126 ppm, an update every 129 s | `/etc/chrony/chrony.conf`, `chronyc -n tracking` |
| entropy | §6 (virtio-rng, `rdrand`/`rdseed`, the EFI seed, cloud-init's `seed_random` module) | |
| boot chain | OVMF → `\EFI\boot\bootx64.efi` (it holds shim-lock strings) and `\EFI\alpine\grubx64.efi` → GRUB 2.14 (EFI variable `LoaderInfo`; serial and console terminals, timeout 10 s) → `vmlinuz-virt` (Linux 6.18.56-0-virt, built 2026-10-12) with `initramfs-virt` (10.5 MB, mkinitfs features `ata base cdrom ext4 keymap kms mmc nvme raid scsi usb virtio ena gve mana pci-hyperv`) → busybox init and OpenRC | `/boot`, `/boot/efi/EFI/*`, efivars, `/etc/default/grub`, `/etc/mkinitfs/mkinitfs.conf` |
| kernel command line | `root=UUID=<redacted> ro modules=sd-mod,usb-storage,ext4,ena,gve,mana console=ttyS0,115200n8 console=ttyAMA0,115200n8 console=tty0` | `dmesg`, `/boot/grub/grub.cfg` |
| cloud-init | 26.2. Datasource `DataSourceNoCloud [seed=/dev/vdb]`; status done. Configures, by `/etc/cloud/cloud.cfg`'s module lists: random seed, growpart and resizefs, host name and `/etc/hosts`, `resolv.conf`, CA certificates, users and groups (the default user `alpine`, root login disabled, no password SSH), SSH, NTP, time zone, locale, apk, `runcmd` and per-boot/instance scripts, the hotplug service. The network goes to ENI. User-data not read | `cloud-init status --long` (without ids), `/etc/cloud/cloud.cfg` |
| boot to userland | cloud-init's `init-local` about 13 s and its final stage about 20 s after the kernel starts | `/var/log/cloud-init-output.log` ("Up 20.29 seconds"), `btime` |

### The box

- **Wall clock**: the host's Unix time in nanoseconds at entry, in R11 (amd64) or X6 (arm64),
  plus the TSC scaled by the rate the monitor gives. `time.Now()`, `new Date()` and
  `currentTimeMillis` are the host's (D10, BX8). Nothing synchronizes the clock after entry.
- **Measured**: a box on 1 vCPU printed `currentTimeMillis` four times, 20 s apart; each line
  was compared with the host's clock as it arrived. Guest minus host: -4.5, -1.4, -2.2 and
  -3.3 ms. The difference is pipe latency; no drift showed over 60 s.
- **Time zone**: `GMT` unless `TZ` is passed (measured: `TimeZone/getDefault`).
- **Entropy**: the host's `crypto/rand` through `Random` (§6).
- **Boot**:
  - the ELF image is appended to the launcher on a 64 KiB boundary and mapped (BX11), or put
    beside it (`arbace-box.elf`, the Mac);
  - argv and the environment are written at `0x4000`-`0x10000` (BX8);
  - registers carry `RamStart`, `RamSize`, the TSC rate and the vCPU count;
  - there is no firmware, boot loader, initramfs or init: the first instruction is the Go
    runtime's.
- **Start time**: 0.3-0.55 s in BOX-NOTES's measurements, and 0.57-0.94 s here today on a
  loaded host. The host VM takes about 20 s to reach cloud-init's final stage.

## 9. Lifecycle and limits

### The host VM

- **Boots**: the current one began at 2026-10-13 07:58:57 UTC (`btime` in `/proc/stat`). The
  survey started 12 minutes later (`uptime`).
- **Boot history**: cloud-init logged 15 boots since 2026-08-21 (the `init-local` lines of
  `/var/log/cloud-init-output.log`) under one instance directory (`/var/lib/cloud/instances`).
  The same VM persists across reboots *(inferred)*.
- **Previous boot**: 2026-09-21, 22 days before this one. The reboot into 6.18.56 was the
  user's (journal, `68e00ce`).
- **On panic**: `panic=120` reboots the machine two minutes after a panic. `panic_on_oops` is 0
  (`/proc/sys/kernel/*`).
- **Ceilings** (`ulimit -a`, `/proc/sys/kernel/{pid_max,threads-max}`, `/proc/sys/fs/*`):
  - open files 163,840; processes 256,974;
  - `pid_max` 65,536; `threads-max` 513,949; `file-max` 6.58 million;
  - stack 8 MiB; locked memory 8 MiB; core files 0;
  - no cgroup limits;
  - memory 29.7 GiB as the balloon leaves it; disk 68 GB free; 64 vCPUs.
- **Contention**: the machine is shared by concurrent agents' builds (`flock .tmp/heavy.lock`
  in the scripts). Steal from the outer host is negligible (§2).

### The box

- **A run is one launcher process.** It ends with the guest's `Exit` code, with a fault (and a
  diagnostic), with Ctrl-C (the launcher dies of the signal), or with the watchdog when one is
  set.
- **No lifecycle beyond the run**: no reboot, suspend, snapshot of memory or migration. What
  persists is the store (§4).
- **Ceilings**:
  - 3 GiB of RAM (2 GiB collector limit) and 32 vCPUs;
  - blobs of 256 MiB; up to 128 pending accepts per listener;
  - 256 KiB network buffers per connection;
  - argv and environment within 48 KiB (`0x4000`-`0x10000`);
  - the host VM's own limits apply to the launcher.
- **Concurrency**: many boxes run at once (the oracle runs 12 with `-j 12`, `bin/box-suite`
  likewise).

## 10. Side by side

| boundary | the host VM | the box |
|---|---|---|
| hypervisor | KVM under QEMU 9.2 (`pc-q35-9.2`), L1 of the cloud | go-whim's monitor on `/dev/kvm` of the host VM (L2), KVM/arm64, Hypervisor.framework |
| firmware, boot | OVMF (UEFI 2.7, Secure Boot off) → GRUB 2.14 → Linux 6.18.56 + initramfs → OpenRC, about 20 s | none: the ELF mapped from the launcher's file, entry in long mode or EL1 with registers set, 0.3-0.9 s |
| platform | PC: ACPI, PCI, LAPIC (x2APIC), HPET, RTC, 16550 UART | none: one doorbell page at `0xf0000000`, 43 calls (1-29, 40-53) |
| paravirtual devices | virtio net, blk ×2, rng, balloon, console (guest agent) | calls: console, store, network proxy, random, SMP, event area |
| vCPUs | 64 (one socket, no SMT), Zen 3, AVX2, no invariant-TSC bit | 1-32, default 4; SSE only (no XSAVE, no AVX); no preemption, evaluator yields |
| nested virtualization | offered (NPT, VMCB clean, TSC scaling; no decode assists, no AVIC, 16 ASIDs) | none |
| memory | 64 GiB booted, 33 GiB ballooned away, 29.7 GiB visible; no swap; THP always; one NUMA node | 3 GiB fixed slot, 2 GiB collector limit, committed on touch, never returned |
| memory return | balloon with free-page reporting | none (AP stacks only) |
| storage | virtio-blk 120 GiB, ext4 (1 KiB blocks), 68 GB free; 426 MB/s sequential read, about 1.4 ms per synced 4 KiB write | no block device; in-memory file system in the heap, lost at exit; the store (blobs of 256 MiB at most, a host directory) |
| network | virtio-net, MTU 1500, IPv4 private /24 plus a public /32, IPv6 link-local only, no in-VM firewall, sshd and mosh listening | none by default; a TCP stream proxy under the launcher's policy (loopback listening, dial allow-list, host-side DNS), TLS in the guest, about 330 MB/s |
| console | ttyS0 (16550A), tty0, virtio-serial for the agent | `WaitRead`/`Write`/`Message`, cooked, rlwrap on the host |
| RNG | virtio-rng, `rdrand`/`rdseed`, EFI seed | `Random` (host `crypto/rand`) |
| clock | kvm-clock (stable), chrony to NTP, 11 µs off | the host's time at entry plus the TSC; no resync; no drift seen in 60 s |
| TPM, watchdog | none, none | none; the monitor's watchdog off by default |
| privilege inside | root, every capability, no seccomp, initial namespaces, no cgroups, lockdown none | ring 0 or EL1 in its slot; no processes, users or files of the host |
| confinement from outside | the cloud's KVM and QEMU; the guest agent open to the outer host | the monitor: pointer checks, a seccomp filter of about 45 calls, the launcher's policies (environment, network, store) |
| lifecycle | persistent instance, 15 boots in 8 weeks, panic → reboot in 120 s | one process per run; the store persists; no snapshot |
| limits | 64 vCPUs, 29.7 GiB, 68 GB disk, 163,840 files | 32 vCPUs, 3 GiB, blobs of 256 MiB, the host's limits |

## 11. What the box lacks for S5-S8

The memory needs of the tools are measured; the rest follows from §6 of
[../STANDALONE-SURVEY.md](../STANDALONE-SURVEY.md) and the records.

| need of S5-S8 | measured or stated need | the box today | gap |
|---|---|---|---|
| **memory for a2g** (S8: the Go-hosted chain inside) | peak RSS 10.1 GB with 16 threads, 12.2 GB with 64, 9.6 GB sequential; live heap 1.1-1.6 GB; run with `GOGC=400 GOMEMLIMIT=12GiB` (STANDALONE-NOTES, "a2g in parallel", SA20) | 3 GiB slot, 2 GiB limit | the live heap fits, but only with a collector limit of about 2 GiB. Unmeasured: a2g at `GOMEMLIMIT=2GiB` may spend most of its time collecting (BX13's table: past the limit "the collector runs continuously") |
| **memory for gc** (S7: tool guests) | `jrt` 7.1 GB (`-c=1`) to 8.0 GB; `lang` 3.1-4.1 GB; the link 1.1 GB (STANDALONE-SURVEY §8.2) | 3 GiB per guest | S5's split is to bring each package under about 2.5 GB (D8 (a)); until then no guest compiles `jrt` |
| **memory for printing** | the program's printing 4.6 GB; the round trip 13 GB (STANDALONE-NOTES, "Speed") | 3 GiB | the same question as a2g's |
| **a writable file system for a build** | sources about 50 MB of forms; the printed Go 35 MB; gc's objects and cache per build (STANDALONE-SURVEY §6.4, R6); `tamago/` alone 32 MB | in-memory, in the heap of the same 3 GiB, empty at start; the store's blobs at most 256 MiB, read whole | trees in memory compete with the tool's heap; nothing reads on demand; outputs persist only by an explicit snapshot or put |
| **a compiler toolchain inside** | `cmd/compile`, `cmd/link`, `cmd/asm` build for `GOOS=tamago` (36.7, 9.8 and 7.0 MB) but were never run in a guest (STANDALONE-SURVEY §8.1, R3) | no processes (D13), no spawn call | S7's tool guests (D9): the spawn call, its policy, a board `main` per tool, a build driver in Arbace |
| **several guests at once** | gc compiles packages in parallel; a2g's 16 threads | 1-32 vCPUs per guest; no budget across guests | a budget in the monitor for memory and vCPUs across the tool guests it starts |
| **network for fetching** | `GOPROXY=off`; every input already pinned and local (ALPINE.md, "What apk does not provide") | none by default; a proxy with an allow-list | **none needed** if inputs reach the box through the store, imported by the host tool (D10) |
| **time** | a Go-hosted build takes 4 min 47 s natively (a2g 258 s); in the box the evaluator runs at about 1.15-1.2 times native, plus nested exits | the wall clock at entry, the TSC after; no watchdog by default | adequate. A long run's drift is unmeasured beyond a minute, and file times are the epoch in snapshots anyway (BX35) |
| **entropy** | seeds, `SecureRandom`, TLS | `Random` | none |
| **CPU features** | gc and the runtime run on SSE; AVX only speeds some code | no AVX (no XSAVE) | none required. Builds are byte-identical whatever the CPU (gc's output does not depend on the build host's features) *(inferred)* |
| **surviving the host's reboot** | the host VM rebooted 15 times in 8 weeks | the store survives on ext4; a run does not | long runs must be resumable from the store (S7's action cache keyed by content hash) |

## 12. What the host VM teaches about the box's boundaries

1. **The memory hole below 4 GiB is the PC's answer to the doorbell's problem.**
   - The host VM's RAM is split: below about 2 GiB, then from 4 GiB to 66 GiB. The space
     between holds the firmware and PCI's windows (`dmesg`, "Early memory node ranges").
   - The box puts its one device page at 3.75 GiB and its RAM below it, so its RAM cannot grow
     without moving the doorbell, an ABI change (STANDALONE-SURVEY §6.2, D8 (b)).
   - The PC's layout suggests the other way: keep the doorbell where it is and put RAM, or
     more of it, above 4 GiB.
   - The cost is in the guest: TamaGo's `sbrk` heap is one span, so the image itself and its
     heap would move above 4 GiB (linked there, the identity map extended). That is to be
     tried, not assumed: gc's amd64 code is position-dependent in a static link *(not checked)*.
     arm64's limit is the IPA size, which on Hypervisor.framework has a default worth measuring
     *(not checked here)*. HV1.
2. **Memory flows back to the outer host.**
   - The host VM's balloon holds 33 GiB of its 64, and the outer host gets free pages
     reported.
   - The box's slot only grows. TamaGo's heap is never returned, and the monitor's one
     `MADV_DONTNEED` is for AP stacks.
   - Short tool guests make this moot: each exit frees everything. A long-lived REPL box with
     a large slot does not. HV2.
3. **Storage is a device outside the guest's RAM.**
   - The host VM reads its 120 GiB disk on demand; the page cache, not the program, holds
     what is hot.
   - The box has only a heap-backed file system and whole-blob reads.
   - A read-only tree served by the monitor on demand (the store's blobs, cached in the
     monitor, not copied into the guest) would put the box's inputs where the host VM's are,
     outside the guest's memory. That matters most when every guest has 3 GiB. HV3.
4. **Several workloads need a budget, not just limits per guest.**
   - The host VM has no cgroups, so nothing bounds the boxes the launcher starts except the
     host's memory, which the balloon shrinks.
   - Tool guests (S7) multiply the slots. The monitor that starts them is the natural place
     for the budget. HV4.
5. **The control channel is a boundary too.**
   - The host VM carries a guest agent that lets the outer host run anything inside it: a
     convenience that is also the widest door in.
   - The box's equivalent is its call table; the spawn call (S7) will be its most powerful
     call.
   - It should be as narrow as the store's: named images from the store, explicit trees in
     and out, no host paths. HV5.
6. **Say what the clock is.**
   - The host VM's CPU does not claim an invariant TSC; the kernel prefers kvm-clock, whose
     stable bit KVM sets, and chrony disciplines it (30 ppm).
   - The box trusts the TSC from entry, with the rate rounded to a kHz (at most about
     0.4 ppm) and no resync.
   - That is fine for a minute (measured). For hours it is unmeasured, and the box cannot
     correct it. HV6.
7. **Nested numbers are a worst case.**
   - With no decode assists offered to L1, every MMIO exit is an instruction KVM in L1 must
     fetch and decode (*inferred*: the cause of the 16-18 µs exits).
   - With no AVIC, there are no posted interrupts (the box has none anyway).
   - With 16 ASIDs, L1's guests share TLB tags.
   - The box's costs measured here overstate the exits by an order of magnitude (BOX-SURVEY
     R7). S8's times should also be measured off this host. HV7.
8. **Hermetic inputs suit both machines.**
   - The host VM's egress is IPv4 only, through an outside firewall it cannot see. Its builds
     already run with `GOPROXY=off` and pinned inputs.
   - A build box that dials nothing is reproducible by construction. HV8.
9. **Reboots happen.**
   - The host VM reboots about every few days to weeks, and recovers from panics in 120 s.
   - What a long build in the box has done must be in the store as it goes, not in a guest's
     memory. HV9.

## 13. The box's boundaries, revisited for S5-S8 (a sketch)

Not a design: the shape the proposals below add up to, if accepted.

- **Memory**: a slot sized per guest by the launcher, or by the spawn call within a budget,
  with RAM above 4 GiB (HV1). The collector limit stays the slot less a headroom (BX13). The
  memory given back is reported to the monitor once guests live long (HV2).
- **Files**: inputs as read-only trees by ref, served on demand by the monitor from the store
  (HV3; D10's trees); outputs in memory, saved to the store as trees.
- **Processes**: tool guests (D9), each with its slot, vCPUs and trees, under the monitor's
  budget (HV4, HV5).
- **Network**: none in a build box (HV8). The proxy stays for REPL boxes.
- **Time**: the clock at entry; a resync only if measured drift needs it (HV6).
- **Persistence**: the store, with the build driver's action cache making every step
  resumable (HV9).

## 14. Proposals for the user

Numbered HV (a prefix not used in `doc/`). Each lists options with one *Recommended*.

**Decided (the user, 2026-10-13): HV1-HV9 all accepted as recommended.** HV1's measurement
(a2g natively under `GOMEMLIMIT=2GiB`) and HV6's (an hour's drift) come first; their results
are recorded below each.

- **HV1 The guest's RAM beyond 3 GiB.** a2g's run (S8) needs a 1.1-1.6 GB live heap and has
  peaked at 10 GB under `GOMEMLIMIT=12GiB`. gc's `jrt` needs 7-8 GB until S5 splits it (D8).
  Options:
  - (a) keep the 3 GiB slot and fit the tools to it: S5's split, and a2g and the printer run
    under a 2 GiB collector limit;
  - (b) RAM above 4 GiB, the doorbell unchanged: the image linked and loaded above 4 GiB, the
    slot's size set by the launcher (or the spawn call). This needs an experiment first: a
    TamaGo guest linked at `-T 0x100201000` on amd64, and the IPA limits on KVM/arm64 and
    Hypervisor.framework;
  - (c) move the doorbell above RAM (STANDALONE-SURVEY D8 (b)), a change to the ABI shared
    with go-whim.

  *Recommended: (a) measured first, (b) as the fallback.* Run a2g natively under
  `GOMEMLIMIT=2GiB` (one cheap run on the host) and record its time. If it is within about 1.5
  times the 12 GiB run, the slot is enough for a2g and D8 stands as decided. Otherwise try (b),
  which keeps the doorbell and go-whim's guests as they are, before (c).

  **Measured (2026-10-13), HV1's test:** `bin/a2g --program` natively on stage 0, sequential
  runs, the host otherwise idle; all three outputs byte for byte the same (`diff -r`, the report's
  times aside):

  | run | wall | peak RSS | load / reach / translate / write (s) |
  |---|---|---|---|
  | `GOMEMLIMIT=12GiB`, 16 threads (today's) | 4m03s | 10.6 GB | 107 / 57 / 32 / 23 |
  | `GOMEMLIMIT=2GiB`, 16 threads | 12m03s (3.0×) | 2.34 GB | 113 / 67 / 226 / 157 |
  | `GOMEMLIMIT=2GiB`, 4 threads (a box's vCPUs) | 25m55s (6.4×) | 2.32 GB | 113 / 66 / 800 / 496 |

  a2g fits in the slot but the collector runs constantly in translate and write: beyond the
  1.5× criterion, so by the user's decision HV1 goes to (b), RAM above 4 GiB with the doorbell
  unchanged, starting with its experiment (a TamaGo guest linked and loaded above 4 GiB on amd64;
  the IPA limits on KVM/arm64 and Hypervisor.framework).

- **HV2 Memory back to the host.** Options:
  - (a) nothing: tool guests are short, and their exit frees all;
  - (b) a release call: TamaGo's scavenger (`sysUnused`) reaches the board, which asks the
    monitor to `MADV_DONTNEED` whole pages, as `CPUStart`'s release does;
  - (c) a balloon: the monitor asks the guest for memory back.

  *Recommended: (a) for S7-S8, and (b) when a long-lived box gets a slot larger than 3 GiB*
  (HV1 (b)). (c) answers an outer host's needs that the box does not have.

- **HV3 Input trees served on demand.** Options:
  - (a) inputs written into the in-memory file system from the store (`ARBACE_BOX_FS`, a tar
    per tree): they occupy the heap;
  - (b) a read-only tree by ref, which the guest's file system resolves name by name: file
    contents read through new calls (`TreeOpen` and a ranged `BlobRead`) from the monitor's
    verified cache, mapped or copied into the guest as read. This is a block device's role,
    in the store's terms (D10's trees);
  - (c) a host directory proxied by calls (rejected by BOX-SURVEY D11).

  *Recommended: (b)*, designed with D10's tree format in S7: it takes the sources (50 MB),
  `tamago/`'s Go (32 MB) and the toolchain out of each guest's 3 GiB. Outputs stay in memory
  and are saved as trees.

- **HV4 A resource budget for tool guests.** Options:
  - (a) per-spawn limits only (the slot and vCPUs a spawn asks for);
  - (b) a budget in the monitor: the total memory and vCPUs of the running guests, defaulting
    to a fraction of the host's `MemAvailable` and processors at the launcher's start, with
    spawns that do not fit queued;
  - (c) the host's cgroups around the launcher.

  *Recommended: (b)*. The host VM has no cgroups mounted, and the macOS host has none at all.
  The monitor knows each slot's size. (c) is Linux only.

- **HV5 The spawn call's surface.** Options:
  - (a) a guest image named by a store hash, arguments, input tree refs and resource asks;
    the answer an exit status and an output tree ref; nothing else crosses;
  - (b) (a) plus a stream to the child's console;
  - (c) host commands by name (as the QEMU agent's `guest-exec`).

  *Recommended: (a)*, with the console's output captured into the output tree. (c) would
  give the guest the host's processes, which D13 cut.

- **HV6 The clock over long runs.** Options:
  - (a) keep the clock at entry plus the TSC;
  - (b) a resync: a `WallTime` call (BOX-SURVEY D10's alternative) read at each event wake
    or every few minutes, slewing the board's offset;
  - (c) a shared page the monitor updates (kvm-clock's design).

  *Recommended: (a), and measure*: a box idle for an hour beside the host's clock, here and
  on the Mac. Take (b) only if the drift exceeds about a second an hour.

  **Measured (2026-10-13), HV6's test here:** an idle box (1 vCPU, the launcher of
  `target/arbace-go/box-amd64`) printed `System/currentTimeMillis` every minute for an hour,
  each line stamped by the host's clock on arrival (`.tmp/hv6/run.sh`). The guest's clock minus
  the host's went from -6 ms to -111 ms, steadily, about 1.75 ms a minute: the guest's clock
  runs about 29 ppm slow, about 0.1 s an hour (2.5 s a day), presumably the TSC's rate as the
  board takes it, not tracking the host's NTP-disciplined clock. Five single-minute outliers
  (-73, -96, -94, -75 ms and one of -4.3 s at minute 51, while another agent's large guest
  was starting) came back to the line the next minute: delivery delays, not clock steps. Below
  the 1 s an hour criterion, so (a) stands here; the Mac's run is due at the next box
  milestone (HV7). A resync (b) would be wanted for boxes living for days.

- **HV7 Where S8's times are measured.** Options:
  - (a) here only (nested SVM without decode assists: exits 16-18 µs);
  - (b) here and on the Mac's Hypervisor.framework (not nested; `bin/box-mac-bundle`'s
    checklist extended with a timed build);
  - (c) a bare-metal amd64 host.

  *Recommended: (b)*: the Mac is already part of every box milestone, and its numbers are the
  realistic ones (BOX-SURVEY R7).

- **HV8 Network in a build box.** Options:
  - (a) none: inputs only from the store, imported by the host tool from git (D10);
  - (b) dialling allowed to source hosts (git, module proxies) under the policy;
  - (c) the policy left to the caller.

  *Recommended: (a)*. It matches `GOPROXY=off` and the pinned inputs, makes the build
  hermetic by construction, and does not depend on the host VM's IPv4-only egress or its
  outside firewall.

- **HV9 Surviving interruption.** Options:
  - (a) the build driver's action cache in the store (S7's plan), so a rerun after the host's
    reboot redoes only unfinished steps;
  - (b) snapshots of a guest's memory to the store;
  - (c) nothing: rerun from the start.

  *Recommended: (a)*. (b) is a large feature (the slot's pages, the vCPUs' state) that the
  host VM's reboot pattern does not need, given (a).

## 15. Sources

- The host VM, 2026-10-13 07:58-08:20 UTC, read only:
  - `/proc/{cpuinfo,meminfo,vmstat,stat,mounts,swaps,consoles,cmdline,self/status,self/ns}`;
  - `/proc/sys/{vm,kernel,net,fs}/*`;
  - `/sys/class/dmi/id/*`, `/sys/firmware/{efi,qemu_fw_cfg}`, `/sys/bus/virtio/devices/*`;
  - `/sys/module/kvm{,_amd}/parameters/*`, `/sys/devices/system/{cpu,clocksource,node}`;
  - `/sys/kernel/{mm,security}`, `/sys/kernel/debug/sched/preempt`, `/sys/block/*/queue`,
    `/sys/class/{net,misc/hw_random,virtio-ports,watchdog,rtc}`;
  - `dmesg`, `blkid`, `dumpe2fs -h`, `df`, `ip`, `netstat`, `chronyc`, `lsmod`, `ps`,
    `rc-status`, `cloud-init status --long`, `ulimit -a`;
  - `/etc/{os-release,alpine-release,fstab,resolv.conf,network/interfaces,chrony/chrony.conf,cloud/cloud.cfg,default/grub,mkinitfs/mkinitfs.conf,init.d/qemu-guest-agent}`;
  - `/boot`, `/boot/efi`, `/boot/config-6.18.56-0-virt`;
  - the `init-local` timestamps of `/var/log/cloud-init-output.log`;
  - the CPUID probe (`.tmp/hv/cpuid.c`, GCC's `<cpuid.h>`);
  - `dd` on `/dev/vda` (reads) and in `.tmp/hv/` (writes, removed).
- The box: main `68e00ce`, the launcher `target/arbace-go/box-amd64/arbace-box` as built at
  04:21 that day. `.tmp/hv/probe.ae` ran on 1 and 4 vCPUs (`ARBACE_BOX_STATS`, `/usr/bin/time
  -v`); `-e nil` was timed against `target/arbace-go/amd64/arbace`; the clock comparison ran
  over 60 s; a store probe used a scratch store.
- Arbace's code and records:
  - `box/go/arbace/box/{abi/abi.ae,vmm/{layout,tamago,smp,seccomp_linux_amd64,dontneed_linux}.ae,hv/{kvm_linux_amd64,tsc_linux_amd64}.ae}`;
  - doc/go/BOX-SURVEY.md, doc/go/BOX-NOTES.md (S1-S6, the store, the network, wake on events,
    TLS, BX1-BX75);
  - doc/STANDALONE-SURVEY.md (§6, §8, §9), doc/STANDALONE-NOTES.md ("Speed", "a2g in
    parallel"), doc/ALPINE.md, doc/AGENDA.md, doc/JOURNAL.md (D8-D10).
