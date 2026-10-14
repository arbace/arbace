# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- The bootstrap starts from the Go-source seed `seed/arbace-seed-go.tar.xz`, the program printed
  by the Go-hosted fixpoint, pinned by `seed/arbace-seed-go.tar.xz.sha256`, its toolchain
  (TamaGo's go1.27.1) by `seed/goroot.sha256`. `bin/seed --check`, `--build` (stage 0),
  `--make`, `--verify` (the fixpoint from the seed's own stage 0 prints the same seed).
- Stage 0 hosts every Arbace tool (a2g, g2a, c2a, the suite's runner); `bin/arbace-go --build`
  is a Go-hosted build, `--fixpoint` the chain G1 → G2 → G3.
- The JVM runs only the frozen jar of `arbace-for-java-26-v4` (pinned by hash): the oracle's
  reference, j2a (javac), and the recording or checking of what the JVM answered (`jdk/`'s forms,
  the edge, the fixtures' forms, data generators). The JVM Arbace's build, ASM and the bytecode
  back ends are on the frozen branch `arbace-for-java-26`.
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
  - S1: the JDK's forms committed in `jdk/`.
  - S2: a2g without the JVM's reflection and ASM (`jdk/edge.edn`); the test fixtures as forms.
  - S3: g2a and a2g on the Go executable; the fixpoint G1 = G2 = G3.
  - S4: the seed printed by the fixpoint and committed; the build Go-hosted from it; a2g
    translating in parallel (a build 4.8 minutes); the JVM's products, ASM and the back ends
    removed (132 files, 25,474 lines); the oracle against the frozen v4.
  - The milestone `arbace-seed-is-go-v1` (2026-10-13): `bin/gate --full`, run with a failing
    `go` first on PATH, then the tag on main (not a freeze).

## Now

- The user accepted the amendments SA20-SA25 and SA40-SA88 as applied (2026-10-13;
  [STANDALONE-NOTES.md](STANDALONE-NOTES.md)).
- Fixed (2026-10-13): the two Go-build bugs the native tests found (a top-level `do` of mutually
  referring `defclass`es; a class forms `lambda` in a handed-over fn), SA90-SA92 accepted. Left:
  reflection on a member typed by a sibling defined later reports `Object` (SA90 (b) if needed).
- The seed remade (2026-10-13, the user): the fixpoint at `68e00ce`, `f7d27e48…`; verified
  (stage 0 = G1 = G2 = G3, `d144af21…`).
- The host rebooted into 6.18.56 (2026-10-13): nested KVM works, the box's smoke test 111 ok.
- No Mac bundle for this milestone (the user).

## Next

- The host VM survey ([go/HOST-VM-SURVEY.md](go/HOST-VM-SURVEY.md)): HV1-HV9 accepted
  (2026-10-13). HV1 measured (a2g under 2 GiB is 3.0× slower), so HV1 (b): RAM above 4 GiB
  for a Go guest, built and merged ([go/BOX-NOTES.md](go/BOX-NOTES.md), "HV1 (b)"), BX80-BX84
  decided as recommended (the slot 3 GiB by default, `ARBACE_BOX_RAM` per launch); the Mac's
  RAM checks with the next Mac bundle. HV6 measured (29 ppm, no resync).

- The next milestone, `arbace-inside-itself-v1` (the user, 2026-10-13), includes all of this
  section: the standalone survey's S5-S8 (the box rebuilding Arbace inside itself), the known
  issues below and the speed item. `bin/gate --full` runs at it. Decided (the user): three
  tracks in parallel, S5 with the known issues, S6 (which carries the speed item), S7; S8 when
  they land. S8 must pass byte for byte on amd64 (/dev/kvm), in the emulated arm64 VM and on the
  Mac (one bundle at the end, also carrying the Mac's HV1 RAM checks and HV6/HV7 clock run). S9
  (Go's toolchain as forms) stays later.
- Status: S5 closed by measurement (SA100-SA107); S7 done; S8 done before S6 (byte for byte in
  the box on amd64 and, with a2g's output from amd64, in the arm64 VM; BX110-BX119); S6 merged
  (2026-10-14): Arbace compiled to Go the default (`ARBACE_AOT=1`), a build 4:46 to 5:14, the
  suite and the oracle as recorded, the fixpoint, the seed remade from it (`62c699af…`, was
  `f7d27e48…`) and verified, the box's rebuild byte for byte on amd64 (`9c802100…`); SA141-SA152
  accepted as (a). The size (the user: inside the milestone), merged: the compiled program
  101.3 MB (was 209.9; the evaluated 72.4), the box's guest 100.8 MB, a build 4:13 to 4:41, the
  gate 10:32, the test build reusing the compiled namespaces; the seed remade (`c7f5e3d5…`) and
  verified, the box's rebuild byte for byte (`4f394cc6…`); SA153-SA159 accepted as (a).
  Then: S8's runs again on the final program (`bin/box-rebuild`, amd64, the arm64 VM, the whole chain there tried again), the Mac bundle for the user
  (`./mac.sh all`; on macOS 14, as the earlier 42 of 42: the user upgrades to 26 only after it),
  `bin/gate --full` with `bin/box-rebuild` by hand, the tag.
- Known issues: fixed in S5 (SA90 (b), `reflect_tables.ae`, the box's oracle run, `Var`,
  `Reflector` and the `.subst` variants merged, `bin/box-vendor convert`); RT, Main and
  Compiler's hand-written variants merged in S6 (SA150).
- Speed: S6 (a2g compiled: load 119 s to 38 s, reach 60 s to 16 s; the compiled build 4:46,
  SA140's bound held; the size halved, SA153-SA159).

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2a.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- Futamura's projections (the user, 2026-10-14): Arbace's compiler is written by hand, not
  derived, but it has the pairs the projections relate: the evaluated build (the evaluator and
  the image) against the compiled one (S6), and the class forms' interpreter
  (`arbace/classes/interp`) against a2g, kept in step by the suite and the oracle, not by
  construction. To survey: fns as Go closures (SA156 (c), closure generation, also a Go type
  fewer per fn); code generation (T4) by partial evaluation of the evaluator, ahead of time, in
  the way of Truffle/Graal (speculating on vars, falling back when one is redefined); the second
  projection, a2g derived from the interpreter, one definition of each language's semantics
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
- The box's oracle and `box-suite` in `bin/gate --full` (manual today); the mapped start measured
  without nested KVM; the arm64 box's oracle subset and `box-suite` (an arm64 host with KVM).
- The build's use of the host's 64 cores (the user, 2026-10-14): S6's AOT builds (about 5
  minutes) keep 5 to 9 cores busy on average. a2g is about 70% of a build on at most 16 threads
  (`A2G_JOBS`'s cap, chosen for memory), its load and analysis partly sequential; g2a's build
  waits on the largest runtime packages; the namespaces' compiling, prepare and the link are one
  process each. To try after the milestone: a higher cap within memory, more of the analysis in
  parallel, the largest packages split; aim 2 to 3 minutes.
- g2a's open ends: mode `:full` in the printer (columns; positions exact); `$GOROOT/test`'s
  directory tests.
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
