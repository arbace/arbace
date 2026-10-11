# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md).

## State

- Stage 0 of the bootstrap is the binary seed `seed/arbace-seed.jar` (`bin/seed --check`,
  `--verify`). The frozen reference baseline `clojure/` (upstream Clojure `98d735f` plus ASM as
  `clojure.asm`) is on the branch `arbace-for-java-26`.
- `LICENSE.md` holds all licenses and which files each covers: Arbace is EPL-1.0, like Clojure;
  ASM is BSD-3-Clause, Guava's Murmur3 Apache-2.0; the Go build's parts are under Go's
  BSD-3-Clause, jdk26u's GPLv2 with the Classpath Exception and the Unicode License V3.
- javalisp, the pathfinder (an exact Java ↔ s-expression transcription), was dropped once the
  class forms superseded it; it is in git history (last at `959d114`).

## Done (brief; the history is in the journals)

- **The JVM work, to the first freeze (2026-10-06 to 2026-10-08).** The class forms (spec
  [classes/SPEC.md](classes/SPEC.md), compiler `arbace/classes/`), the Java converter (today
  j2a), Clojure vendored as `arbace/` and self-hosting in three byte-identical stages, Arbace
  modernized for Java 26 (the JDK AOT cache, verified classes, `invokedynamic` sites, a `jlink`
  image). Frozen on the branch `arbace-for-java-26` (tag `arbace-for-java-26-v1`, then `-v2` to
  `-v4`); its journal, its
  [FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/FREEZE.md) and its
  [BENCHMARKS.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/BENCHMARKS.md)
  are there. Since then stage 0 is the binary seed `seed/arbace-seed.jar`, and the tools that
  served only the JVM product moved to that branch.
- **B1a, Arbace as a native Go executable (2026-10-08 to 2026-10-11).** Go as Arbace forms
  (today g2a, [go/SPEC.md](go/SPEC.md)) with the round trip; the translator of Arbace to Go
  (today a2g, [go/A2G-SPEC.md](go/A2G-SPEC.md)), jrt, the evaluator and `bin/arbace-go`, static
  executables for `linux/amd64` and `linux/arm64` ([go/B1-PLAN.md](go/B1-PLAN.md), steps 0-8).
  Frozen on the branch `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`, 2026-10-11); its
  journal and its
  [FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/go/FREEZE.md) are
  there.
- **After the second freeze (2026-10-11, main's [JOURNAL.md](JOURNAL.md)).** Arbace's sources
  are `.ae`; c2a, the Clojure → Arbace translator; the tools renamed j2a, g2a, a2g; Arbace's
  own version (`*arbace-version*`, the REPL's banner) and `data_readers.ae`.

## Now: B1b, the box

TamaGo's build of the same program in go-whim's monitor, amd64 on /dev/kvm first
([go/BOX-SURVEY.md](go/BOX-SURVEY.md), stages S0-S6; [go/B1-PLAN.md](go/B1-PLAN.md), "B1b").
The user's decisions D3, D8-D14 are taken, all as recommended.

- S0 done (2026-10-11): go-whim's box vendored unchanged as Go forms under `box/`
  (`bin/box-vendor`; [go/BOX-NOTES.md](go/BOX-NOTES.md)).
- S1 done (2026-10-11): the boxed REPL on amd64 (`bin/arbace-go --build --box`, the launcher
  `arbace-box`; REPL, `-e`, `-m`, scripts, four or one vCPUs; amendments BX1-BX10 accepted). Box
  oracle 21,695 of 21,742 (`test/oracle/known-go-box-amd64.edn`), `bin/box-suite` 19,625 of
  19,627 (`test/arbace-box-results.edn`).
- S2 in progress, hardening: the rest (the start, the collector, the watchdog, the file tests
  and `ARBACE_PATH` in the box, reducers' `fold` on 4 vCPUs). The box's smoke test, oracle and
  suite in `bin/gate --full`: deferred (the user, 2026-10-11).
- R1 done: the launcher maps the guest image (BX11, accepted; mapped by default), 258 MB
  resident.
- In progress beside S2: a2g's overload order (possibly unstable in `a2g_classes.ae`) and
  jrt-convert's headers (a numbered temporary directory), for reproducible builds.
- Checks until the next milestone (the user): the essential `bin/gate` and targeted checks; no
  `bin/gate --full`.

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2a.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- The milestone now (the user, 2026-10-11): the box on amd64, in this order: S2; S6 (the image's
  Go as forms: TamaGo's runtime and std converted by g2a, the image built from the printed forms
  byte-identical to one built from the originals), started once two builds of the same tree are
  identical; then S3 (the store) and S5 (the network, D12), each re-running S6's proof. Its
  freeze runs `bin/gate --full`.
- The next milestone: S4, arm64 (KVM/arm64, then Hypervisor.framework on the Mac; purego,
  `board.NilPanics` for arm64, S6's arm64 identity).
- g2a's open ends: mode `:full` in the printer (columns; positions exact); `$GOROOT/test`'s
  directory tests.
