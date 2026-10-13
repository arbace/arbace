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
- The two Go-build bugs the native tests found (agent, branch `go-bugs`): a top-level `do` of
  mutually referring `defclass`es; a class forms `lambda` in a handed-over fn.
- The host's reboot (the user: after the milestone): it runs kernel 6.18.52 with 6.18.56's
  modules installed; after it, `bin/arbace-go --smoke --box` for nested KVM.
- The Mac bundle for this milestone, if wanted (`bin/box-mac-bundle`).

## Next

- The standalone survey's S5-S8: the box rebuilding Arbace inside itself.
- Known issues from S4 (STANDALONE-NOTES, "S4: the JVM removed", Left):
  - `go/arbace/jrt/reflect_tables.ae` stale against `bin/jrt manifest` (regenerating it changes
    the build);
  - `test/oracle/known-go-box-amd64.edn` updated but not run on the box;
  - the class forms' Go variants and the `.subst` variants to merge into their bases.
- Speed: a2g's load (about 106 s) and reach (55 s) on Go stay sequential; S6 (Arbace compiled
  to Go) is the remedy for the evaluator's cost.
- `bin/box-vendor convert` fails on the launcher (`cmd/arbace-box`, not in go-whim's clone):
  S0's replay, outside the gate.

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
