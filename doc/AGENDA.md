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
- S2 done (2026-10-11): the launcher maps the guest image (BX11, mapped by default; its start
  under nested KVM to be measured on a host without nesting); the symbol table cut, the
  collector's limit in the box, the watchdog off, the file tests and `ARBACE_PATH` in the box,
  reducers' 4.9 GB (a tail call clears the method's object) and `fold` scaling 3.2-3.4x on 4
  vCPUs (BX12-BX17, accepted). Box oracle 21,696 of 21,742 (the native's 46), `bin/box-suite`
  19,645 of 19,646. The box's checks in `bin/gate --full`: deferred.
- Reproducible builds (RP1, accepted): two builds of a clean commit are byte-identical.
- S6 done for amd64 (2026-10-11): TamaGo's runtime, std (371 packages) and `goos` module as Go
  forms in `tamago/` (`bin/tamago-vendor`; 32 MB), the box's guest built from them by default,
  byte-identical to one built from TamaGo's originals (BX20-BX24, accepted); `bin/gate --full`
  runs `tamago-vendor verify` and the proof (`box-forms`).
- S3 done (2026-10-11), the store: `arbace.box` (BX30-BX36), on the JVM too (BX37), `ARBACE_PATH`
  the same on all three; the monitor's blob cache bounded (BX38).
- S5 done (2026-10-12), the network (D12: a stream proxy; BX40-BX44): TCP through the launcher
  under `ARBACE_BOX_NET`'s policy; the socket REPL from the host; TLS later, with JSSE.
- Wake on events (BX50-BX54): the guest parks until console, network or timer events instead of
  polling; idle host CPU 0.00 s per 20 s.
- The milestone reached (2026-10-12): `bin/gate --full` passed (19m47s) and the tag
  `arbace-box-amd64-v1` is on main (a milestone, not a freeze: no branch, the journal continues).
- S4 done (2026-10-12), arm64 (BX60-BX66): nil dereferences raise NullPointerException on
  arm64 (the vectors' return to `sigpanic`); `tamago/` one tree for amd64 and arm64, the arm64
  guest byte-identical to the originals'; purego vendored as Go forms and the launcher on
  darwin/arm64; `bin/box-smoke`, `bin/box-arm64-vm` (KVM/arm64 in an emulated aarch64 VM:
  the smoke test 103 of 103, go-whim's hv and vmm tests), `bin/box-mac-bundle`.
- The milestone reached (2026-10-12): `bin/gate --full` passed and the tag `arbace-box-arm64-v1`
  is on main (not a freeze).
- The Mac run (2026-10-12, the user's, macOS 14.8.7 on an M2 Max): the bundle's checklist 42 of
  42 on Hypervisor.framework with 4 and 1 vCPUs (the REPL, scripts, `-m`, nil dereferences as
  NullPointerException, futures, R2/R3, the loopback network and a socket REPL from the Mac, the
  store), go-whim's hv tests 4 of 4, idle 0.01 s of CPU per 20 s, start about 0.17 s.
- The standalone survey done ([STANDALONE-SURVEY.md](STANDALONE-SURVEY.md)); D1-D13 decided as
  recommended: the Go-hosted bootstrap first (S1-S4), then the box (S5-S8).
- Next milestone (the user): `arbace-seed-is-go-v1`, at the end of the standalone S4 (the seed
  Go source pinned by hash, the Go-hosted fixpoint, the JVM only for the oracle and j2a); a tag on
  main after `bin/gate --full`, not a freeze.
- Standalone S1 done (2026-10-12): the JDK's forms committed in `jdk/`, `bin/jrt-convert verify`
  in `--full`; the build reaches no javac or jdk26u.
- Now: standalone S2, a2g without the JVM's reflection and ASM (agent, branch `standalone-s2`);
  TLS in the box through Go's crypto/tls, for the Claude API from the box (agent, branch
  `box-tls`; the user: option 2, now).
  `bin/gate --full` runs the amd64 box smoke test (`box-smoke`). The Mac: a bundle per box
  milestone, the user's run following the tag.
- Checks until the next milestone (the user): the essential `bin/gate` and targeted checks; no
  `bin/gate --full`.

## Later

- The standalone Arbace: no Java binary compatibility, with Java at the source level through j2a.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- After `arbace-box-arm64-v1`: the box's oracle and `box-suite` in `bin/gate --full` (manual); the mapped start measured without nested
  KVM; the arm64 box's oracle subset and
  `box-suite` (hours in the emulated VM; an arm64 host with KVM).
- g2a's open ends: mode `:full` in the printer (columns; positions exact); `$GOROOT/test`'s
  directory tests.
