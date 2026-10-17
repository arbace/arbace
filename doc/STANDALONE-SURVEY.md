# The standalone Arbace — survey and plan

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

Status: survey, 2026-10-12, for the main line after the box's milestones (`arbace-box-amd64-v1`,
`arbace-box-arm64-v1`), by the user's decision of 2026-10-12 (the journal at the tag
`arbace-inside-itself-v1`,
[JOURNAL.md](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md),
2026-10-12, "The user's decisions after S4"). Nothing here was implemented then. The decisions
(§9) are the user's.

Since then (2026-10-15): D1-D13 decided, S1-S8 done (doc/STANDALONE-NOTES.md; the milestones
`arbace-seed-is-go-v1`, 2026-10-13, and `arbace-inside-itself-v1`, 2026-10-15), S9 open. The
survey is otherwise as written on 2026-10-12. The measurements
were taken on main at `a6a6b86` with its build outputs (`target/`, `.tmp/jrt`); the experiments
(§8) ran in `.tmp/standalone-survey/`.

The goal, as CLAUDE.md states it: Arbace as a standalone ecosystem, not binary compatible with
Java, Java usable at the source level through repr/java; the language is Clojure plus the Java forms,
later the Go forms; a self-sustaining REPL inside the box, written in Arbace down to the bare
metal ISA, in the way of Go plus TamaGo. This document asks what "standalone" removes, what the
runtime should become, what the box must hold for Arbace to rebuild itself inside it, and what
"down to the ISA" would take; it ends with stages and decisions.

## Executive summary

- **Today every Arbace program is built by the JVM.** The Go executable and the box's guest come
  out of a chain whose every Arbace step runs on the JVM's `target/stage2`: repr/java converts 836
  jdk26u files with javac's front end (`bin/jrt-convert`, 108 s), j2g translates 3,040 classes
  into Go forms (62 s), repr/go prints them, and gc builds them. The seed is a jar. Standalone
  starts by moving that chain onto Arbace's own Go build: **the bootstrap becomes Go-hosted**,
  and only then can the JVM leave main's path (§3, §4).
- **What blocks a Go-hosted bootstrap is small and known**: (1) repr/java needs javac, but its output
  can be committed as forms, as `tamago/` holds TamaGo's Go (§4.2); (2) j2g's analyzer learns the
  JDK's classes by reflection on the JVM and reads class files with ASM, while the Go build's
  reflection lists only public members (§4.3); (3) the build (build-go) runs `go build` as a process, which
  the native executable can do and the box cannot (§6). Measured: repr/go's printer already runs on
  the Go executable with identical output, about 7 times slower than on the JVM (§8.3).
- **jrt is mostly breadth, not need.** The runtime and the REPL's namespaces use 274 JDK classes
  (970 members), whose translation closure was 422 classes from 184 files; jrt today translates
  2,134 JDK classes from 836 files (1.06 million lines of generated Go in one package), the rest
  being java.base completed for interop (java.time, java.text, java.util.stream and
  concurrent, java.security's basics, sockets). Standalone does not remove it: Java at the source
  level needs java.base as a library. It re-scopes it: **a core the runtime needs, and a library
  a program takes**, split into Go packages (§3.2).
- **Compiling jrt does not fit in the box.** gc needs 7.1-8.0 GB to compile the `jrt` package
  and 3.1-4.1 GB for `lang`; a guest has 3 GiB (`GoRAMBytes`). The split above, or a larger
  guest (an ABI change), is a precondition of rebuilding in the box (§8.2).
- **Go's own toolchain builds for the box unchanged**: `cmd/compile`, `cmd/link` and `cmd/asm` of
  TamaGo's go1.27.1 build for `GOOS=tamago` on amd64 and arm64 without a change (36.7, 9.8 and
  7.0 MB, not yet run in a guest; §8.1). The box has no processes (BOX-SURVEY D13), so the
  recommended shape is **tool guests**: the monitor runs another guest image on the guest's
  request, inputs and outputs passing through the store (§6.3).
- **The runtime: keep Go's.** The recommendation is to keep Go's runtime (TamaGo's, already held
  as forms in `tamago/` and patched in `overlay/go/`), keep j2g's translation of `arbace.lang`,
  slim jrt, and give Arbace's compiler a third back end, **Arbace code compiled to Go forms**
  (namespaces ahead of time), since the closure evaluator is 54 times slower than the JVM
  (geometric mean, SPEED-NOTES). An Arbace-written collector or scheduler, and proper tail calls
  in general, need Arbace's own code generator: they belong to the ISA horizon, not to the next
  stages (§5).
- **Down to the ISA** is G2C-SURVEY's b1-b4, re-measured: the sources are held (b1) except Go's
  own toolchain (`cmd/compile` 626k lines with its generated rules, `cmd/link` 44k,
  `cmd/internal/obj` 108k, `cmd/asm` 5.8k), which repr/go can hold as `tamago/` was held. The rest,
  an Arbace back end, linker and runtime, is a multi-year horizon, outlined only (§7).
- **Stages (§9.1):** S1 the JDK's forms committed; S2 j2g without the JVM's reflection; S3 the
  Go-hosted fixpoint (native); S4 a Go seed, the JVM off main's path; S5 jrt split and slimmed;
  S6 namespaces compiled to Go; S7 the toolchain as tool guests; S8 the rebuild inside the box;
  S9 Go's toolchain as forms. About 40-70 agent-assisted days in all, the first four 11-21.

## 1. Where things stand (2026-10-12)

Three products, one source tree:

| product | how it is built | where it stands |
|---|---|---|
| Arbace on the JVM | seed jar → stage 1 → stage 2 = stage 3 (5,777 classes), AOT cache, jlink image | frozen on `arbace-for-java-26`; on main it is the build host of everything else |
| Arbace as a Go executable | stage 2 runs repr/java (javac), j2g, repr/go; gc links; the executable prepares its image | frozen on `arbace-for-golang`; on main, 66.8 MB on amd64 |
| the box | the same program with `GOOS=tamago`, the board, `tamago/`'s Go | milestones `arbace-box-amd64-v1`, `arbace-box-arm64-v1`; the Mac run 42 of 42 |

The Go build's chain, with the host of each step:

```
jdk26u sources ──repr/java (javac's front end; JVM)──▶ Java forms of 836 files (.tmp/jrt/conv, untracked)
arbace/lang/*.ae + overlay/jdk + go/arbace/jrt ──j2g (Java forms analyzer; JVM)──▶ Go forms
Go forms ──repr/go print (JVM)──▶ Go source (1,063 files, 35 MB) ──gc go1.27.1 (host process)──▶ executable
executable ──ARBACE_PREPARE (the Go executable itself)──▶ the image of prepared namespaces, linked in
```

Only the last step runs on Arbace's Go build. The gate's timings (`.tmp/gate/*.secs`):
`jrt-convert` 207 s, the Go build 123 s (j2g 87 s, gc 23 s, prepare 11 s).

## 2. What the program is made of (measured)

| part | size | source |
|---|---:|---|
| `arbace/` (all `.ae`) | 115,708 lines | of which `arbace.lang` 46,796; ASM 14,708; Java forms compiler 8,406; repr/java 4,410; repr/go 5,496; j2g 7,934 |
| `arbace/lang/go/` (the Go build's variants) | 8,168 lines | `Compiler`'s evaluator and closure compiler, `ClassForms`, `RT` |
| jrt hand-written (`go/arbace/jrt`) | 88 files, 28,681 lines | the VM's edge: `String`, reflection, threads, the host |
| jrt's own Java (`overlay/jdk`) | 110 files | variants and additions to jdk26u |
| jdk26u converted (`bin/jrt-convert`) | 836 files (31 generated), 2,222 classes, 202,034 lines of forms (11 MB) | all shape-identical to javac's but 5 known |
| the edge (where translated JDK code stops) | 226 classes, 1,223 members | shim 99 classes (815 members), rework 9 (174), cut 81 (177), translate 36 (55); `Unsafe` alone 274 members |
| j2g's closed world | 3,040 classes translated (905 `arbace.lang`, 2,134 JDK), 27,896 methods reached | `target/arbace-go/j2g/report.edn` |
| the generated Go | `jrt` 1,055,778 lines, `lang` 284,414, `cmd/arbace` 1,644 | one Go package each |
| Go's runtime patches (`overlay/go`) | 3 files: the goroutine-local slot, the box's event goroutine | `proc.go`, `runtime2.go`, `arbace_local.go` |
| TamaGo's runtime and std as forms (`tamago/`) | 2,152 files, 32 MB | byte-identical guest (S6) |

The JDK classes j2g translates, by package family (classes, inner classes included):
`java.util` 1,246 (of which `java.util.stream` 454 and `java.util.concurrent` 296),
`jdk.internal` 164, `java.time` 124, `java.lang` 123, `sun.util` 74, `java.text` 72,
`sun.security` 65, `java.io` 60, `java.net` 53, `java.nio` 44, `java.security` 42, others 67.

What Arbace itself references (JAVA-SURFACE.md, regenerated 2026-10-11): the runtime and the 22
namespaces of a REPL's start use **274 JDK classes and 970 members**; translated with what they
need in turn, **422 classes from 184 files**. Of the 901 members the runtime and REPL reference,
the Go build provides 837; of java.base's 17,546 public members it provides 9,807
(JAVA-BASE.md). The difference between 422 and 2,134 is the breadth B1-PLAN's D6 reversals added
for interop at the REPL.

## 3. What "standalone" removes or replaces

### 3.1 Java binary compatibility

What exists on main only because Arbace runs on the JVM:

- bytecode: ASM (`arbace/asm`, 44 files, 14,708 lines), the Java forms compiler's back end
  (`emit`, `shape`, `verify`, `compiler`, `build`, `boot`: 2,724 lines), `Compiler`'s bytecode
  emission (`Compiler.ae`, 8,717 lines, shared with the Go variants that replace it),
  `gen-class` (760), `DynamicClassLoader`;
- the products: `bin/arbace-j`, the AOT cache and its training, the jlink image
  (`bin/arbace-image`), the three-stage bootstrap with the verifier;
- the JVM as a host: the seed jar, every tool (`bin/lib/tools.bash` runs them on stage 2), the
  oracle's reference side, Clojure's suite on stages 1 and 2.

The first two can leave main once nothing depends on them; the frozen branch keeps them
(`arbace-for-java-26`, fixes only). The third cannot leave until the bootstrap is Go-hosted
(§4). Two JVM uses outlive it and are better kept than replaced early:

- **the oracle.** The JVM Arbace is the reference for the Go build's differential tests
  (`bin/oracle`, 22,232 of 22,278 natively). Once the seed is no longer a jar (§4.4), the
  reference is the frozen jar (`arbace-for-java-26-v*`), and every intended divergence of the
  standalone language is recorded in the known-mismatch files, as the 46 are today;
- **repr/java's front end.** repr/java parses and attributes Java with javac's tree API (`com.sun.source`,
  `com.sun.tools.javac`). Java at the source level means repr/java; without the JVM it needs javac
  itself carried over (348 files, 162,441 lines of jdk26u's `jdk.compiler`, plain Java that the
  same repr/java → j2g path could translate) or a Java front end of Arbace's own. Neither is needed
  for self-hosting if repr/java's output is committed (§4.2): converting new Java is then a host-side
  tool, as `tools/godump` is for repr/go (D12).

### 3.2 jrt's breadth

jrt is Java's library as much as Java's runtime. Standalone keeps the library, because a Java
program converted by repr/java calls `java.util` and `java.time` as written, and drops what only served
binary compatibility:

| kind | today | standalone |
|---|---|---|
| the VM's edge, hand-written (`String`, `StringBuilder`, reflection over member tables, `Thread`, locks, atomics, `Unsafe`'s 274 members, the host) | Go forms in `go/arbace/jrt` | Arbace's own runtime already: keep, trimmed of what only bytecode needed |
| the runtime's closure (collections, `Character` and Unicode tables, `java.math`, regex, `Formatter`, readers and writers) | translated by j2g from repr/java's forms | the **core**: kept, translated as now |
| java.base's breadth (time, text, streams, concurrent, security, net, nio's files) | translated into the one `jrt` package, every program carries it | a **library**: per Java package or group, taken by the programs that use it |
| `java.lang.invoke`, class loading, `Class` as the JVM's, serialization, `sun.misc` | reworked or cut (D6) | gone for good: no class files to load |

The obstacle to splitting is Java's own package cycles (`java.lang` ↔ `java.util`, `Collection`'s
`stream()` default method): Go packages cannot import each other in a cycle. The split follows the
strongly connected components of the reached classes' dependency graph, after variants cut the
few edges that make the core reach the library (as `overlay/jdk` already does). It is measured
before it is promised (S5). Its payoffs: the box can compile the program (§8.2), a program that
needs no java.time does not carry it, and the core's size shows what Arbace's own runtime is.

### 3.3 The class file and the JVM path

They stay on the frozen branch. On main after S4 there is no class file: the Java forms are
compiled by j2g (ahead of time) or interpreted (at the REPL, CLASSFORMS-REPL.md), and the class
forms compiler's front end (`parse`, `lower`, `analyze`, `env`, `types`: 5,538 lines) is shared
by both. The Java forms' semantics stay Java's (they represent Java), but nothing is promised
about a JVM running their output.

## 4. The bootstrap without a JVM

### 4.1 The fixpoint

Today: seed jar → stage 1 → stage 2 = stage 3. The Go-hosted analogue: an executable G1 (built
by today's chain) runs j2g, prints the Go and builds G2; G2 does the same and builds G3; **G2's
printed Go equals G3's byte for byte**, and G2 = G3 as executables (RP1 already makes two builds
of a commit byte-identical). The image of prepared namespaces is made by the executable itself
already.

### 4.2 repr/java's output as sources

The converted JDK is regenerated on every build and untracked (the user's decision for repr/java's
output, JRT-SOURCES.md). For a Go-hosted build it becomes a source: the 909 forms files (11 MB,
202,034 lines; the 31 sources the JDK's build generates among them) committed, as `tamago/` holds TamaGo's
Go, with `bin/jrt-convert` turned into a check (`verify`: repr/java on the JVM gives the same forms)
run by `bin/gate --full`. Then no build needs javac, jdk26u's checkout or the JDK's generators
(D2). The resource data the program embeds (`tzdb.dat`, `uniName.dat`, ICU's `nfc.nrm`,
`java.security`) is committed the same way.

### 4.3 j2g without the JVM's reflection

j2g reuses the Java forms analyzer, whose class environment (`arbace.classes.env`) learns
classes three ways: from the forms being compiled, from classes it defined, and from the class
path by reflection (`getDeclaredFields`, `getDeclaredMethods`, constants read from class files
with ASM's `ClassReader`). On the JVM the JDK's classes come by the third way; on the Go build,
reflection lists only public members of the world (CLASSFORMS-REPL.md §7), not enough for
translating jrt itself (package-private and protected members). And j2g's `access.ae` writes
javac-style accessor methods with ASM's `ClassWriter` to read back the one instruction each
holds. Both are replaced by data:

- the whole closed world entered from forms: `arbace/lang`, the committed JDK forms,
  `overlay/jdk`; nothing needs reflection for them;
- the hand-written edge declared: the 226 edge classes and 1,223 members (and the shims' own
  public surface) as a declaration file of class infos in `env`'s own shape, generated once
  from the JVM, checked by the JVM while it stays (`bin/jrt-convert`'s `edge` step already lists
  them);
- the analyzer keeping an accessor's target member as data beside its bytecode-writing
  function, so `access.ae` needs no ASM.

Exit: j2g on the JVM with the class path's reflection refused for the world gives the same
program byte for byte (S2). Then it can run on the Go build.

### 4.4 The seed

Candidates, once the fixpoint holds:

| seed | size | what it needs to start | notes |
|---|---:|---|---|
| (a) the fixpoint's printed Go source (`work-amd64/mod`, plus `overlay/go`'s runtime files) | 35 MB; 4.4 MB as `tar.xz`, 6.3 MB gzip | a Go toolchain (go1.27.1, or TamaGo's) | architecture-neutral; readable; `bin/seed --verify` rebuilds it from its tag; Go's toolchain bootstraps itself from source |
| (b) the fixpoint's executable per architecture | 66.8 MB each (amd64, arm64) | nothing but the OS | two binaries to pin; opaque |
| (c) keep the jar | 7.5 MB | a JDK 26 | the JVM stays in main's path |

Recommended: (a) (D3). It moves the trust from a JVM build to gc, which Arbace needs anyway, and
its pin is a hash as now.

## 5. The runtime's design

### 5.1 What the runtime is today

Three layers: **Go's runtime** (TamaGo's in the box: its collector, scheduler, goroutines,
stacks; held as forms in `tamago/`, three files patched in `overlay/go`), **jrt** (Java's object
model, strings, reflection, threads on goroutines), **`arbace.lang`** translated by j2g. Arbace
code runs on the evaluator: `Compiler`'s analyzer, then each method compiled once into a tree of
`Code` closures (SPEED-NOTES.md, EC1-EC6): 5 to 500 times slower than the JVM's compiled code,
54x as geometric mean; start 0.21 s natively, 0.17 s in the box on the Mac. In the box TamaGo has
no `sysmon`: the evaluator yields every N calls (BOX-SURVEY R2).

### 5.2 What gc fixes

As long as gc compiles everything, Go's runtime contract holds: precise stack maps that gc
emits, contiguous growable stacks, the hybrid write barrier, goroutines (G2C-SURVEY §5.1). An
Arbace collector or scheduler cannot replace Go's under gc-compiled code. Go has no proper tail
calls and gc does not make them. What can change without a back end of Arbace's own is what the
runtime's Go forms say (as `overlay/go` already does): preemption points for the box, the
goroutine-local slot, event wake-ups.

### 5.3 Options

- **R1. Keep Go's runtime; Arbace's runtime by translation; compile Arbace to Go.** j2g keeps
  translating `arbace.lang` (one implementation of Clojure's runtime, the one the JVM proved);
  jrt slimmed into core and library (§3.2); `Compiler` gains a third back end beside bytecode
  and the closure evaluator: **Expr → Go forms**, so namespaces (and the tools: j2g, repr/go, the
  Java forms compiler) are built into the executable as Go, the evaluator staying for the REPL.
  G2C-SURVEY §5.2's option (ii); B1-PLAN step 7's unscheduled half.
- **R2. A runtime written for Arbace, Joker's way.** A new, Go-shaped runtime (Go strings, Go
  interfaces, no Java object model) in Go forms or Arbace. Joker (candid82/joker v1.10.0, as
  measured in go-whim's LISP-SANDBOX.md) is the precedent: 28,000 hand-written lines of Go, a
  bytecode VM, a 29 MB binary started in 20 ms, but no sorted collections, records, protocols or
  types, single-threaded under a global lock, and a persistent vector bug found at once. It
  gives up "Arbace represents its own implementation", reopens what Clojure's suite and the
  oracle settle, and loses Java at the source level unless jrt stays beside it.
- **R3. An Arbace collector and scheduler now.** Not possible under gc (§5.2): it needs Arbace's
  own code generator first (§7).

**Recommended: R1** (D5). It keeps the one runtime, uses what Go does well (collector, scheduler,
goroutines on vCPUs, which `fold` scaling 3.2-3.4x on 4 vCPUs shows), and its Go-forms back end
is also the first piece of an Arbace compiler: §7's code generator later takes the same
analyzed `Expr`s.

### 5.4 Proper tail calls

| option | cost | reach |
|---|---|---|
| T1 Clojure's: `recur`, `trampoline` | none | what Clojure promises |
| T2 in the evaluator: a call in tail position returns a marker its caller's loop runs | a check on every evaluated call; stack traces lose frames | evaluated code only |
| T3 in the Go back end: tail calls trampolined, for all calls or those marked | measured on Clojure's benchmarks before any choice; calls into Go code are not covered | compiled Arbace code |
| T4 in Arbace's own back end (§7): a tail call is a jump | none at run time | everything Arbace compiles |

**Recommended: T1 now, T4 as the goal** (D6): the language does not promise proper tail calls
until its own back end makes them free; T3's cost is measured in S6 so that the choice is made on
numbers. The Go-forms back end must not rule them out (one calling convention for compiled fns,
as `IFn.invoke` is now).

### 5.5 Strings

jrt's `String` is UTF-16 (J2G-SPEC D4), as Java source semantics need (`charAt`, `length`,
surrogates). Go's strings are UTF-8 bytes. Standalone could switch Arbace's strings to Go's, at
the price of Java's semantics in code converted by repr/java and in `arbace.core`'s own interop. **Recommended:
keep UTF-16 for `java.lang.String`** (D11); Go strings enter as their own type when the Go forms
join the language.

## 6. Self-sustaining in the box

### 6.1 What a rebuild needs, step by step

| step | today | natively, after S3 | in the box |
|---|---|---|---|
| the sources | a git checkout | the same | the store: a tree of blobs (§6.4) |
| the JDK's forms | repr/java on the JVM | committed (S1) | in the source tree |
| j2g | the JVM | the executable (S2, S3) | the guest; 7-8 GB peaks are gc's, not j2g's: j2g's own heap to be measured |
| printing Go | the JVM | the executable (§8.3: identical, about 7x slower) | the guest |
| gc and the linker | a host process | a host process (`HostProc`) | **no processes**: §6.3 |
| the image of prepared namespaces | the new executable | the same | the new guest, run once |
| running the result | the launcher with the image appended | the new executable | the launcher booting an image from the store (new) |

### 6.2 Memory

The guest's slot is 3 GiB (`GoRAMBytes`, under the doorbell at `0xf0000000`; arm64's layout keeps
the guest in 32-bit addresses). gc's peak for the program's two packages (§8.2) is above it:
7.1 GB for `jrt` compiled with one back-end worker, 3.1 GB for `lang`. Remedies: smaller packages
(§3.2; gc's memory grows with the package), or a larger slot (the doorbell moved above RAM: an
ABI change in the vendored monitor and board, a divergence from go-whim; on arm64 a layout
change). Recommended: the split first, the slot as the fallback (D8).

### 6.3 Processes: how gc runs in the box

- **P1. gc linked into the guest as a library.** `cmd/compile` and `cmd/link` are `main`
  packages written to run once per process (global state, `os.Exit`, `base.Flag`); their
  `internal` packages are importable only from `cmd`. Running them repeatedly in one guest means
  editing gc (as forms, S9) to reset its state.
- **P2. Tool guests.** A new call asks the monitor to run another guest image (from the store)
  with arguments and a file tree (a store ref seeding its in-memory file system, as
  `ARBACE_BOX_FS` does), and returns its status and its output tree as a ref; the requesting
  guest waits on the event area (BX50-BX54). gc runs unmodified, one guest per compilation, as
  `go build` runs one process per package; the monitor stays the host program it is (BOX-SURVEY
  D14). This is D13's "start another box", which that decision left open.
- **P3. The host builds.** The launcher runs gc on the host. Simple, and not self-sustaining.

**Recommended: P2** (D9). It needs: a guest `main` for each tool with the board (the experiment
of §8.1 built them without one), a spawn call and its policy (which images, how many at once, the
memory of each), and a driver in Arbace replacing `go build`'s (the order of packages, the
action cache: `bin/build-go`'s plan, held in the store by content hash).

### 6.4 The store as the source of truth

The store (S3) holds blobs by SHA-256 and named refs; the in-memory file system snapshots to a
tar blob (`save-fs!`); blobs are capped at 256 MiB by default (`ARBACE_BOX_STORE_MAX`). A
source tree of Arbace is about 50 MB of forms (`arbace/` 5.3 MB, the JDK's forms 11 MB,
`tamago/` 32 MB, `box/` 0.7 MB). Options: (a) a tar per tree (exists; every edit stores the whole
tree again); (b) **a blob per file and a tree as an EDN map of names to hashes**, git's model
in Arbace's data (rebuilds share unchanged files; gc's action cache keys on the same hashes);
(c) git itself in the box (go-git, a large vendoring). Recommended: (b) (D10), with the host
tool importing a git commit into a tree ref and exporting one back, git staying the record and
the journal's place.

## 7. Down to the ISA (outline)

G2C-SURVEY §5.2's ladder, re-measured (go1.27.1, non-test lines):

| step | Arbace owns | still Go's | state |
|---|---|---|---|
| b1 | every source as forms: Arbace, the JDK's forms, TamaGo's runtime and std (`tamago/`), the box | gc, the assembler, the linker | done but for Go's toolchain itself: `cmd/compile` 625,880 lines (with the generated SSA rules), `cmd/internal/obj` 108,447, `cmd/link` 44,109, `cmd/asm` 5,793 |
| b1' | the toolchain as forms, the toolchain built from them byte-identical (S9, as S6 for `tamago/`) | none of the sources | not started; repr/go's round trip covers the std these import |
| b2 | the front end: Go forms type-checked and lowered to an SSA-like IR in Arbace | gc's back end | repr/go's helper uses `go/types` (24,923 lines) |
| b3 | a back end for Arbace code first (the `Expr`s of §5.3 to amd64, then arm64), honouring Go's runtime contract: stack maps, write barriers, `morestack`, ABIInternal; proper tail calls as jumps (T4) | gc for Go code, the linker | |
| b4 | the linker, `pclntab`, the object layout | none | |
| b5 | the runtime's collector and scheduler as Arbace code, once b3 compiles the runtime (`runtime` 138,487 lines over all ports; the collector's `mgc*` 11,030; `proc.go` 8,233) | none | |

The order matters more than the sizes: b3 for Arbace code alone is bounded (one IR, two ISAs,
Go's contract as the spec and gc as the oracle), whereas b3 for Go code is gc's back end again.
Nothing past b1' is staged here.

## 8. Experiments (`.tmp/standalone-survey/`, 2026-10-12)

### 8.1 Go's toolchain for the box

With TamaGo's go1.27.1 (`/root/tamago-go`), unchanged:
`GOOS=tamago GOARCH=amd64 go build ./cmd/compile` (also `./cmd/link`, `./cmd/asm`, and
`./cmd/compile` for arm64) builds without an error: 36,690,213, 9,778,862 and 7,035,205 bytes on
amd64, 34,476,650 for `compile` on arm64. Not run: a guest needs the board's `main` and its
inputs in the in-memory file system (S7).

### 8.2 What gc needs to compile Arbace

The amd64 program's printed Go (`target/arbace-go/work-amd64/mod`) built with a fresh cache and
`-toolexec` measuring each tool (the final link fails without `overlay/go`'s runtime files, as
expected; the compilations are what was measured):

| package | Go lines | default (`-c` parallel back end) | `-c=1` |
|---|---:|---|---|
| `arbace/jrt` | 1,055,778 | 53.7 s, 8.0 GB | 2 min 3 s, 7.1 GB |
| `arbace/lang` | 284,414 | 18.2 s, 4.1 GB | 50.8 s, 3.1 GB |
| `main` (`cmd/arbace`) | 1,644 | | 2.5 s, 0.6 GB |
| the link | | 3.2 s, 1.1 GB | |

### 8.3 Arbace's tools on the Go executable

repr/go's printer (`arbace.repr.go.print`, loaded from source through `ARBACE_PATH`) run by the native
executable against `bin/repr-go print` on the JVM: `strconv` (5 files) 2.6 s against 1.8 s;
`net/http` (29 files, 18,006 lines of Go) 12.0 s against 3.2 s, the output identical. Without
the JVM's start (about 1.5 s), about 7 times slower: printing the whole program (1.34 million
lines) would take about 15 minutes in the evaluator, against 2 on the JVM. j2g (62 s on the JVM:
load 14 s, reach 5 s, translate 37 s, write 7 s) was not tried: it needs S2.

## 9. Plan and decisions

### 9.1 Stages

Each stage a gate, as B1a's steps and the box's. Estimates in agent-assisted days, rough.

| stage | content | exit criteria | days |
|---|---|---|---:|
| **S1 the JDK's forms as sources** | repr/java's output (909 forms files) and the embedded data committed (under `jdk/`, like `tamago/`); `bin/jrt-convert verify` in `bin/gate --full`; the provenance (jdk26u's commit) recorded as for `tamago/` | `bin/arbace-go --build` from the committed forms gives the same executable byte for byte, without javac or jdk26u's checkout | 1-2 |
| **S2 j2g without the JVM's reflection** | the closed world entered from forms; the edge's declaration file; accessors as data, no ASM in j2g | j2g on the JVM with the world's reflection refused writes the same program byte for byte | 3-5 |
| **S3 the Go-hosted fixpoint, native** | j2g, repr/go and `bin/build-go`'s driver run by the Go executable (linux/amd64, `go build` as a host process); a `bin/` script for the chain | G1 (today's chain) → G2 → G3: G2 = G3, their printed Go identical; time recorded | 4-8 |
| **S4 the Go seed; the JVM off main's path** | the seed as the fixpoint's Go source (D3), pinned by hash, rebuilt from its tag; ASM, the bytecode back ends, `gen-class`, `bin/arbace-j`, the AOT cache, jlink and the three JVM stages leave main (kept on `arbace-for-java-26`); the JVM stays for the oracle (the frozen jar) and repr/java | `bin/gate` runs no JVM except the oracle's reference side and repr/java's checks; Clojure's suite on the Go build as the gate's suite | 3-6 |
| **S5 jrt as core and library** | the reached classes' package graph and its components measured; jrt split into Go packages; reachability per program; the core named | no package needs more than about 2.5 GB to compile; the suite and the oracle unchanged; sizes recorded | 5-10 |
| **S6 Arbace compiled to Go** | `Compiler`'s Go-forms back end; namespaces and the tools built into the executable; the image reduced to what stays evaluated; T3 measured | Clojure's suite and the oracle unchanged; the benchmarks against the evaluator and the JVM; j2g's time on Go | 10-15 |
| **S7 tool guests** | the spawn call and its policy; gc, the linker and the assembler as guest images (amd64, arm64); file trees in the store (D10); a build driver in Arbace | in the box: a Go package compiled and linked into a guest image the launcher then boots from the store | 6-10 |
| **S8 the rebuild in the box** | S3's chain inside the guest: sources from a tree ref, the new guest image to the store; the launcher boots by ref | the image built in the box equals the host-built one byte for byte; amd64 on /dev/kvm, then arm64 (the VM, the Mac's bundle) | 5-10 |
| **S9 Go's toolchain as forms** | `cmd/compile`, `cmd/link`, `cmd/asm` and `cmd/internal` as Go forms (repr/go), built from them | the tools built from the forms identical to the originals' | 3-5 |

S1-S4 make Arbace build itself without a JVM, natively; S5-S6 make it small and fast enough for
the box; S7-S8 close the loop in the box; S9 completes b1'. S9 can run at any time after S1.

### 9.2 Decisions for the user

- **D1 The order.** (a) the Go-hosted bootstrap first (S1-S4), then the box's self-hosting
  (S5-S8); (b) the box first, its builds driven from the host; (c) the runtime's redesign first.
  *Recommended: (a)*: every later stage runs on what S3 proves, and it retires the JVM from the
  build; (b) builds the box's loop on a JVM-made chain; (c) has nothing to measure against
  without (a).
- **D2 repr/java's output committed.** (a) the JDK's forms as sources in the repository (11 MB), repr/java a
  check; (b) regenerated on every build (today, the user's earlier decision). *Recommended: (a)*:
  without it no build is free of javac, and it matches `tamago/`.
- **D3 The seed.** (a) the fixpoint's printed Go source, pinned by hash (4.4 MB compressed);
  (b) the executables per architecture; (c) the jar stays. *Recommended: (a)*.
- **D4 The JVM on main after S4.** (a) only as the oracle's reference (the frozen jar) and repr/java's
  front end, the JVM products removed from main; (b) kept built and gated beside the Go build.
  *Recommended: (a)*: the frozen branch takes fixes; keeping both doubles every change.
- **D5 The runtime's direction.** (a) R1: Go's runtime, `arbace.lang` by j2g, jrt slimmed,
  Arbace compiled to Go forms; (b) R2: a new Go-shaped runtime; (c) R3: Arbace's own collector
  and scheduler now. *Recommended: (a)*; (c) after §7's b3.
- **D6 Proper tail calls.** (a) T1 now, T4 with the own back end, T3 measured in S6; (b) T2 in
  the evaluator now; (c) T3 for all compiled calls in S6. *Recommended: (a)*.
- **D7 jrt's scope.** (a) a core and a library, split into Go packages by the dependency graph's
  components; (b) one package, reachability per program only; (c) the runtime's closure alone
  (about 422 classes), the breadth dropped. *Recommended: (a)*: (b) leaves the compile memory as
  it is; (c) gives up Java at the source level.
- **D8 Compiling in a 3 GiB guest.** (a) smaller packages (D7) first, a larger slot only if
  needed; (b) a larger slot now (the doorbell moved: an ABI change on both architectures).
  *Recommended: (a)*.
- **D9 gc in the box.** (a) P2, tool guests through a spawn call; (b) P1, gc linked in and
  reset; (c) P3, the host builds. *Recommended: (a)*.
- **D10 Source trees in the store.** (a) a blob per file and EDN tree maps, imported from and
  exported to git by the host tool; (b) a tar per tree; (c) git in the box. *Recommended: (a)*.
- **D11 Strings.** (a) UTF-16 `java.lang.String` kept; (b) Go's UTF-8 strings for Arbace.
  *Recommended: (a)*.
- **D12 Java at the source level without the JVM.** (a) repr/java stays a host-side tool on the JVM
  for now, its outputs committed; (b) javac translated into the Go build through repr/java and j2g
  (162,441 lines), after S6; (c) a Java front end of Arbace's own. *Recommended: (a) now, (b)
  as a later stage*: it is the same path as the JDK's, and javac stays the reference.
- **D13 The language's divergence.** From S4 on, Arbace may differ from Clojure on the JVM where
  standalone requires it (no `gen-class`, no class loading, no bytecode-level interop).
  (a) each divergence recorded as an amendment and as a known mismatch of the oracle, Clojure's
  suite (through c2a) staying the yardstick; (b) divergence free, the oracle retired.
  *Recommended: (a)*.

### 9.3 Risks

- **R1 Package cycles.** jrt's split may find one large component (java.lang, java.util and
  java.io reach each other everywhere); then the memory remedy is the slot (D8 (b)).
- **R2 The evaluator's speed.** Before S6 the Go-hosted chain runs its tools on the evaluator
  (§8.3: about 7x for the printer; j2g's ratio is unmeasured and may be closer to the 54x of
  the benchmarks). S3 is slower than today's chain; S6 is what makes S8 practical.
- **R3 gc in a guest.** gc on TamaGo is built, not run: its use of the file system, `os/exec`
  (none expected in `compile`), temporary files and memory is unknown until S7.
- **R4 The toolchain's coupling.** Arbace's build follows TamaGo's Go releases (BOX-SURVEY
  R5); a Go-source seed (D3 (a)) pins the toolchain that builds it.
- **R5 The oracle drifts.** With the JVM frozen, every standalone change to the language makes
  the reference less useful; the known-mismatch files must stay small and explained.
- **R6 The store's size.** A tree of about 50 MB, the program's Go (35 MB) and gc's cache per
  build: the monitor's blob cache (256 MiB by default, BX38) and the host directory's growth
  need a collection policy before S8.

## 10. Sources

- Arbace at main `a6a6b86` (2026-10-12): CLAUDE.md, doc/AGENDA.md, doc/JOURNAL.md;
  doc/G2C-SURVEY.md §5; doc/go/{B1-PLAN, BOX-SURVEY, BOX-NOTES, J2G-SPEC, JAVA-SURFACE,
  JAVA-BASE, JRT-SOURCES, JRT-NOTES ("Size"), CLASSFORMS-REPL, SPEED-NOTES, EXEC-NOTES}.md;
  `arbace/classes/env.ae`, `arbace/j2g/{access,reach,embed}.ae`, `arbace/build/go.ae`,
  `bin/{arbace-go,jrt-convert,j2g,repr-go}`; build outputs `target/arbace-go/j2g/report.edn`,
  `target/arbace-go/work-amd64/mod`, `.tmp/jrt/{report.md,edge.edn}`, `.tmp/gate/*.secs`.
- TamaGo's Go distribution `/root/tamago-go` (go1.27.1): `src/cmd/{compile,link,asm,internal/obj,go}`,
  `src/runtime`, `src/go/types` (sizes).
- jdk26u (`/root/jdk26u`): `src/jdk.compiler/share/classes` (javac's size).
- go-whim `doc/LISP-SANDBOX.md` (`/root/go-whim`): Joker (candid82/joker v1.10.0) measured.
