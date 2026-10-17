# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md) (since 2026-10-15), the journal at the tag
[`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
(2026-10-11 to 2026-10-15) and the frozen branches' (before).

## State

- The bootstrap starts from the Go-source seed `seed/arbace-seed-go.tar.xz` (`56fabe20…`), the
  program printed by the Go-hosted fixpoint, its toolchain (TamaGo's go1.27.1) pinned by
  `seed/goroot.sha256`. `bin/seed --check`, `--build` (stage 0), `--make`, `--verify`.
- Stage 0 hosts every Arbace tool (j2g, repr/go, build/go, c2a, the suite's runner), compiled into it;
  `bin/arbace-go --build` is a Go-hosted build, `--fixpoint` the chain G1 → G2 → G3.
- The program's namespaces are compiled to Go (`ARBACE_AOT=1`, the default since S6): the
  executable 102.4 MB (amd64), a one-run build about 2.5 to 3 minutes.
- The box: the same program as a TamaGo guest of go-whim's monitor (/dev/kvm on amd64; KVM/arm64
  and Hypervisor.framework on arm64), and the box rebuilding Arbace inside itself byte for byte
  (`bin/box-rebuild`, run by hand at milestones).
- The JVM runs only the frozen jar of `arbace-for-java-26-v4` (pinned by hash): the oracle's
  reference, repr/java (javac), the edge, the fixtures' forms and the data generators.
- `LICENSE.md` holds all licenses and which files each covers.
- The host: Alpine edge on amd64 with /dev/kvm ([ALPINE.md](ALPINE.md); `bin/alpine-env`).

## Done (brief; the history is in the journals)

- **The JVM work, to the first freeze (2026-10-06 to 2026-10-08).** The class forms, j2a,
  Clojure vendored as `arbace/` and self-hosting, Arbace for Java 26. Frozen on
  `arbace-for-java-26` (tags `-v1` to `-v4`).
- **B1a, Arbace as a native Go executable (2026-10-08 to 2026-10-11).** g2a, a2g, jrt, the
  evaluator, `bin/arbace-go`. Frozen on `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`).
- **After the second freeze (2026-10-11).** `.ae` sources, c2a, the tools renamed, Arbace's own
  version.
- **B1b, the box (2026-10-11 to 2026-10-12).** TamaGo's build as a guest of go-whim's monitor
  ([go/BOX-NOTES.md](go/BOX-NOTES.md)): the boxed REPL, `tamago/` as forms, the store, the
  network, wake on events, TLS with Mozilla's roots. Milestones `arbace-box-amd64-v1` and
  `arbace-box-arm64-v1` (KVM/arm64; Hypervisor.framework on an M2 Max, 42 of 42).
- **The standalone Go-hosted bootstrap, S1-S4 (2026-10-12 to 2026-10-13;
  [STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), [STANDALONE-NOTES.md](STANDALONE-NOTES.md)).**
  The JDK's forms committed, a2g without the JVM's reflection, the tools on the Go executable,
  the seed printed by the fixpoint; the JVM's products removed. Milestone `arbace-seed-is-go-v1`.
- **Arbace inside itself, S5-S8 (2026-10-13 to 2026-10-15).** S5 (the known issues, closed by
  measurement), S7 (tool guests, spawned, the action cache), S6 (the namespaces compiled to Go,
  the default; the size halved, 209.9 → 101.3 MB; the variants merged), S8 (the rebuild in the
  box byte for byte on amd64, in the emulated arm64 VM and on the Mac, 46 of 46). The host VM
  survey's HV1-HV9 decided, HV1 (b) built, HV6/HV7 measured on the Mac. Milestone
  `arbace-inside-itself-v1` (2026-10-15): `bin/gate --full` at `c3fdd36`, `bin/box-rebuild` by
  hand (`3131e754…`).

## Now

- The journal started over after the milestone (the user, 2026-10-15).
- Done (2026-10-15): the docs searched for sloppiness and fixed; the build's speed, first round
  ([STANDALONE-NOTES.md](STANDALONE-NOTES.md), "After the milestone: the build's speed"): a
  one-run build about 2:45 to 3:00 (was about 4:15), a2g alone about 100 s (was 160 to 210), the
  seed remade (`79d4f552…`) and verified. SA160-SA163 decided (a), as built. SA90 (b)'s last
  case: a do evaluated again names the new sibling class in reflection (`6e9af3c`). The box's
  oracle and `bin/box-suite` in `bin/gate --full`.
- Done (2026-10-15): S9, Go's toolchain as forms ([STANDALONE-NOTES.md](STANDALONE-NOTES.md),
  "S9"; STANDALONE-SURVEY.md §7 b1'): `tamago/cmd/`, the 105 packages `compile`, `link` and
  `asm` build from, for linux/amd64, tamago/amd64 and tamago/arm64 (`bin/toolchain-vendor`); the
  nine tools built from the forms byte-identical to the originals'; the box's tool guests built
  from them (SA203 (b)); `verify` and `check` in `bin/gate --full` (SA204 (a)).
- Done (2026-10-15): the build's speed, second round ([STANDALONE-NOTES.md](STANDALONE-NOTES.md),
  "the build's speed, second round", parts A-D): the host's back end run compiled (SA164; about
  half as long), a2g after the compiled namespaces arrive 66 to 75 s (was 123), g2a's collect
  index (SA190; its step about 8 s, was 18 to 21), `jrt` not split (SA180). SA164, SA180, SA181,
  SA190, SA191 decided (a). The seed remade after S9's merge (`9b2a4687…`, was `79d4f552…`) and
  verified: stage 0 = G1 = G2 = G3 (`2ee89825…`).
- Done (2026-10-16): the whole chain in the emulated arm64 VM, nothing given (BX117 (a), at
  `434021a`): the image built in the box = the arm64 host step's, byte for byte (`5e4506de…`),
  in 40 h 29 min (about 247 times the host's 591 s; a2g alone 28.8 h). Seen there: the guest
  waiting on a spawn takes 3.5 vCPUs under emulation, an artifact of emulation (natively a few
  percent of one core; BOX-NOTES.md, BX117; left as it is, the user).
- The Mac moved to macOS 26 (the user, 2026-10-16); the box's checks there not yet run on it.
- Done (2026-10-16): the build's speed, third round (STANDALONE-NOTES.md, "third round"): the
  loader in parts (analysis 25 → 15 s), a2g's load parallel (10.7 → 5 s), reachability (19-21 →
  6 s), the compiled tree in several files (read 2 → 0.8 s), ids independent of the checkout's
  path (SA230); the seed remade (`5858e83c…`) and verified.
- Done (2026-10-16): the oracle independent of thread interleavings (two concurrency cases;
  ORACLE.md's rule).
- Done (2026-10-16): Arbace independent of go-whim (its tests in `box/` on the box's own guests,
  the C guests in Go's assembly) and of cgo (dropped with the race detector); GW2-GW9 (a)
  (BOX-NOTES.md).

- Done (2026-10-16): the build's speed, fourth round (STANDALONE-NOTES.md, "fourth round"):
  reachability's scan in mutable state with shared plans (`:reach` 7-10 → 3-4.5 s), the
  analysis' fields indexed and supertypes memoized (16 → 5 s); SA250 (a), SA260 (a); the seed
  remade (`b566d53a…`) and verified.
- Done (2026-10-16): a tools-only seed measured (JOURNAL.md): -14% to -22% of xz, `tzdb.dat`
  stays; not pursued (the user).
- Done (2026-10-17): g2a's printer mode `:full` (Go forms converted with `--full` printed back to
  the original text byte for byte; the text round trip in `bin/gate --full`; G2F1-G2F8).

## Next

- The direction (the user, 2026-10-17): Go forms first-class at the REPL, interpreted like
  Arbace and Java forms; the three dialects at the same level there "when it is appropriate";
  Haskell forms stay data. The survey written (doc/GO-FORMS-REPL-SURVEY.md): recommended
  Arbace's own interpreter of Go forms (a type checker in Arbace, typed closures, a table of the
  program's compiled packages), milestones GR0-GR6, 22-33 days; proposals G1-G17 open; the build
  after F3, F4 and the renames.

- Done (2026-10-17): the rename (doc/NAMING-SURVEY.md, "The rename as built"): j2a → `repr/java`
  (`bin/repr-java`), g2a → `repr/go` (`bin/repr-go`), its build → `build/go` (`bin/build-go`),
  a2g → `j2g` (the printed `j2g_*` names, `J2G_*`), `G2A_GOROOT` → `ARBACE_GOROOT`, the old
  variables guarded; "Java forms" in prose; c2a and `arbace.classes.*` unchanged; R1-R5 decided.
  The seed remade (`56fabe20…`) and verified; R4 done (`tamago/` converted again, the guests
  identical). Done: repr/go's directory tests (`bin/repr-go roundtrip --dirs [--text]`, 251 to 254
  tests per configuration, every level; D1-D3 decided, in `bin/gate --full`). Done: F2 (`bin/diffcheck`, 12,449 of 12,471 cases agree; F2.1-F2.6 decided).
- `repr/haskell`'s spec written (doc/haskell/SPEC.md; S1-S36; HS1-HS12 decided as recommended).
  Running beside F2 (the user, 2026-10-17): its milestone R1 (`tools/hsdump`, the converter, the
  tree level over `base`; branch `repr-haskell-r1`), and `repr/java print` (Java forms → Java,
  with a round trip; branch `repr-java-print`).
- After the rename (the user, 2026-10-17: "Create jaf print after the naming debate settled"):
  `repr/java print`, Java's bridge printing back, class forms → Java source, with a round trip like
  repr/go's (Java → forms → Java → forms, the two forms equal; the text level as repr/go's
  `:full`); R1: today's `arbace.repr.java.print` (forms as `.ae` text) becomes `layout` with it.
- The survey of Futamura's projections written (doc/FUTAMURA-SURVEY.md; its experiment on the
  local branch `futamura-survey`); decided as suggested (the user, 2026-10-17): after the rename,
  F2 (per-form differential checks), then F3 and F4 (arithmetic fast paths, no initialization
  guards for the classes initialized at start; one re-seed, preceded by the rename of c2a to
  `repr/clojure` and of `arbace.aot` to `c2j`: the user, 2026-10-17), then F5 on the numbers,
  F6, F7. Measured there: `fib`
  6.40 → 0.168 ms (the JVM 0.16) by a `Long` fast path, var speculation and an unboxed worker;
  `loop-long` 11.87 → 1.09 ms without the class-initialization guards.
- Smaller: the box's checks on the Mac on macOS 26.

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through repr/java.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- Futamura's projections (the user, 2026-10-14): Arbace's compiler is written by hand, not
  derived, but it has the pairs the projections relate: the evaluated build (the evaluator and
  the image) against the compiled one (S6), and the class forms' interpreter
  (`arbace/classes/interp`) against j2g, kept in step by the suite and the oracle, not by
  construction. To survey: fns as Go closures (SA156 (c), closure generation, also a Go type
  fewer per fn); code generation (T4) by partial evaluation of the evaluator, ahead of time, in
  the way of Truffle/Graal (speculating on vars, falling back when one is redefined); the second
  projection, j2g derived from the interpreter, one definition of each language's semantics
  (Clojure, the class forms, later Go). The cost: binding times in a dynamic language (vars,
  `eval`, macros at load time).
- A kernel and a user part in the box (the user, 2026-10-14): the infrastructure (the
  collector, the scheduler, the compiler, I/O, the store) at EL1/ring 0 and the application
  level at EL0/ring 3, inside one guest. Today the guest is one TamaGo program at EL1/ring 0, one
  address space, one Go runtime for both. To survey: what crosses the boundary (a call ABI like
  the doorbell's, one level down); one heap the kernel's collector scans across the privilege
  levels, or a heap per user part; the scheduler's goroutines running user code at EL0 (the
  page tables and exception vectors TamaGo's board sets up); the cost of the crossings for a
  REPL's fine-grained calls; against the isolation S7 already has (a guest per tool, spawned) and
  isolation in the language (namespaces, capabilities). A guest per tool needs a hypervisor
  under Arbace: on bare metal (the box's long-term target, TamaGo's boards) there is none unless
  Arbace is it (EL2 on arm64, VMX root on amd64: go-whim's monitor inside Arbace, where the
  hardware boots it at EL2 or gives VT-x), so the EL1/EL0 split is the isolation that works on
  any hardware, the guests only where virtualization is there. Prior art: Go's runtime as a user
  process; GoTEE (github.com/usbarmory/GoTEE: TamaGo unikernels and user-mode applets, TrustZone
  on arm), to check; unikernels with a privilege split.
- The mapped start measured without nested KVM.
- A note from the user (2026-10-13): the tooling of the Mac GUI app Moshi Go may be useful for
  Arbace, to study when Arbace's REPL gets a user interface beyond the terminal. Its build info
  (`go version -m`, on the user's Mac): module `moshi-go` at `1516bf8173a5` (2026-10-12), go1.27.1
  (as ours), darwin/arm64, `CGO_ENABLED=0`, `-trimpath`, tag `mygo_noinspector`. Its deps:
  `github.com/egoist/mygo` v0.4.5 replaced by the fork `github.com/rjyo/mygo` (the GUI toolkit),
  `github.com/ebitengine/purego` v0.11.1 (as the box's launcher uses), `go-text/typesetting`
  v0.3.5, `golang.org/x/image`, `alecthomas/chroma/v2` (highlighting), `yuin/goldmark`
  (Markdown), `creack/pty` and `charmbracelet/x` (pty, conpty, term, termios: a terminal),
  `coder/websocket`, `dlclark/regexp2/v2`. A pure-Go, cgo-free GUI on macOS is the same shape
  as the box's launcher there.
- A touch terminal for Arbace's REPL, a clean-room reimplementation of Moshi's iOS terminal
  interface (the user, 2026-10-14; long term, for discussion): survey and plan in
  [IOS-REPL-SURVEY.md](IOS-REPL-SURVEY.md), proposals IR1-IR13 open; more screenshots from the
  user would answer its §3.
