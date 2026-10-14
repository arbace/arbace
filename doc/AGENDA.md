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
  the box on amd64 and, with a2g's output from amd64, in the arm64 VM; BX110-BX119); S6's
  checkpoint merged (`ARBACE_AOT=1`, SA130-SA140), its remaining work: the suite on the compiled
  test build, an AOT build no slower than 5 minutes, the AOT fixpoint, then AOT the default and
  the seed remade; RT, Main and Compiler's variants merged into their bases. Then: S8's runs
  again on the final program (`bin/box-rebuild`, amd64, the arm64 VM, the whole chain there
  tried again), the Mac bundle for the user (`./mac.sh all`), `bin/gate --full` with
  `bin/box-rebuild` by hand, the tag.
- Known issues: fixed in S5 (SA90 (b), `reflect_tables.ae`, the box's oracle run, `Var`,
  `Reflector` and the `.subst` variants merged, `bin/box-vendor convert`); left: RT, Main and
  Compiler's variants (with S6).
- Speed: S6 (a2g compiled: load 119 s to 38 s, reach 60 s to 16 s); left: the AOT build itself
  no slower than 5 minutes (SA140).

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2a.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- The box's oracle and `box-suite` in `bin/gate --full` (manual today); the mapped start measured
  without nested KVM; the arm64 box's oracle subset and `box-suite` (an arm64 host with KVM).
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
