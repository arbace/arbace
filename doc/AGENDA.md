# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md) (since 2026-11-01), the journals at the tags
[`arbace-reaps-3-crops-v2`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v2/doc/JOURNAL.md)
(2026-10-27 to 2026-11-01),
[`arbace-reaps-3-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v1/doc/JOURNAL.md)
(2026-10-20 to 2026-10-27),
[`arbace-interprets-go1.27.1-v1`](https://github.com/arbace/arbace/blob/arbace-interprets-go1.27.1-v1/doc/JOURNAL.md)
(2026-10-15 to 2026-10-20) and
[`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
(2026-10-11 to 2026-10-15), and the frozen branches' (before).

## State

- The bootstrap starts from the Go-source seed `seed/arbace-seed-go.tar.xz` (`a59cc441…`, committed `1fe1063f`,
  2026-11-02: C9.2's re-seed, embedding C: `arbace/c`, `arbace/repr/c`, `arbace.lang.CInterp`), the program printed by
  the Go-hosted fixpoint, its toolchain (TamaGo's go1.27.1) pinned by `seed/goroot.sha256`; `bin/seed --verify` passed
  (stage 0 = G1 = G2 = G3, `606d45e6…`). `bin/seed --check`, `--build` (stage 0), `--make`, `--verify`.
- Stage 0 hosts every Arbace tool (c2j, j2g, repr/go, repr/clojure, build/go, the suite's runner, ci and repr/c),
  compiled into it; `bin/arbace-go --build` is a Go-hosted build, `--fixpoint` the chain G1 → G2 → G3. The program's
  namespaces are compiled to Go (`ARBACE_AOT=1`, since S6).
- The REAP matrix (`bin/reap`): four crops, Clojure, Java 26, Go 1.27.1 and C (C23 against gcc 15.2), 16 of 16 cells
  checked, none failing, every non-pass in a recorded class ([FULL-SUPPORT-SURVEY.md](FULL-SUPPORT-SURVEY.md), "REAP
  v3's divergences"). The other crops (OCaml, Scheme, C++23, net/http) have their checks and classes, out of the matrix
  and of the gate.
- The gate is keyed and audited (GT1-GT7, [GATE-SURVEY.md](GATE-SURVEY.md)); since 2026-11-02 `bin/gate --full` runs C's
  seven full checks as heavy steps (about 7 to 8 hours more, serial).
- The JVM (Java 26 from jdk26u's own build, 2026-11-02) runs only the frozen jar of `arbace-for-java-26-v4` (pinned by
  hash): the oracle's reference, the edges' recordings, `bin/repr-java check --suite`'s back end, `jrt-convert`'s edge
  and the data generators; repr/java and javac run on javac's tool executable (JN6), a JDK's javac the reference of
  javac on Arbace's class files.
- `LICENSE.md` holds all licenses and which files each covers.
- The host: Alpine edge on amd64 with /dev/kvm ([ALPINE.md](ALPINE.md); `bin/alpine-env`). The Mac peer, an arm64 VM,
  takes queued tasks (reviews, audits, small development branches) through the bare repository's queue.

## Done (brief; the history is in the journals)

- **The JVM work, to the first freeze (2026-10-06 to 2026-10-08).** Frozen on `arbace-for-java-26` (tags `-v1` to
  `-v4`).
- **B1a, Arbace as a native Go executable (2026-10-08 to 2026-10-11).** Frozen on `arbace-for-golang` (tag
  `arbace-for-go1.27.1-v1`).
- **B1b, the box (2026-10-11 to 2026-10-12)**, TamaGo's build as a guest of a monitor
  ([go/BOX-NOTES.md](go/BOX-NOTES.md)). Milestones `arbace-box-amd64-v1` and `arbace-box-arm64-v1`.
- **The standalone bootstrap, S1-S9 (2026-10-12 to 2026-10-15;
  [STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), [STANDALONE-NOTES.md](STANDALONE-NOTES.md)).** The seed is Go forms,
  the Go executable rebuilds itself, the box rebuilds Arbace inside itself byte for byte, Go's toolchain as forms
  (`tamago/cmd/`). Milestones `arbace-seed-is-go-v1` and `arbace-inside-itself-v1`.
- **goi (2026-10-17 to 2026-10-20; [go/GOI-SPEC.md](go/GOI-SPEC.md)).** The interpreter of Go forms, complete for
  Go 1.27.1. Milestone `arbace-interprets-go1.27.1-v1`.
- **The front ends (2026-10-17 to 2026-10-24).** repr/go's native reader (GP1-GP8); javac on Arbace and repr/java
  without a JVM (JN1-JN9f); the Java conformance check (JC1-JC6b); the renames; Futamura's F2-F7; repr/haskell and
  repr/rust (parked 2026-10-26, unparked 2026-11-03).
- **`arbace-reaps-3-crops-v1`** (2026-10-27) and **`-v2`** (2026-11-01, at `c4f85ace`): REAP for Clojure, Java 26 and
  Go 1.27.1, 12 of 12 cells; v2 with the gate made efficient and run clean in a fresh clone
  (`test/gate/clean-arbace-reaps-3-crops-v2.edn`).
- **`arbace-reaps-4-crops-v1`** (not yet tagged: it waits for GT8 and the backlog, the user, 2026-11-03; TODO(tag): the date and the commit): C the fourth row
  (C1-C9, [C23-SURVEY.md](C23-SURVEY.md)): read 5,201 / 232 / 50, eval 2,049 / 124 / 87, AOT 2,058 / 115 / 87, print
  4,767 / 6 / 83 (pass / by design / deferred); 16 of 16 cells, none failing ("REAP v3's divergences"). The merge
  window after v2 (the gate's branches and audits, the crops, JN10), C8, C9 and its re-seed window (C9.2: the seed
  `1fe1063f` verified, the records written and classified, CS265-CS268), the keyed `bin/gate --full` with C's full
  checks (TODO(tag): its result). Its record: an audited clean run of the four crops over GT8's compressed corpora, within budget (the user, 2026-11-03).

## Now: GT8, the test budget, and the backlog, before the tag (the user, 2026-11-03)

The user's decision of 2026-11-02: the checks get a budget before the next crops, so that every crop's full check runs
in bounded time. The tiers, for all ten planned crops (Clojure, Java, Go, C, C++, OCaml, Scheme, Haskell, Rust, Python):

| tier | budget | when |
|---|---|---|
| essential | at most 15 minutes (a goal) | each night's first step, on `next` (the staging branch); by hand rarely by day |
| nightly | 8 hours planned in the 12-hour night shift, 4 of slack for fixes and reruns | each night, a keyed `--nightly` over compressed corpora |
| clean | one night (about 10 hours planned), two at most | a fresh clone, every step audited, at a tag; it replaces that night's nightly |

The day and the night (the user, 2026-11-04; GATE-SURVEY.md, "Decided (2026-11-04)"): the night shift is 19:00-07:00 UTC,
for gating, testing, details and the bugs the checks find; the day is for development and design with sample checks.
Day merges go to the branch `next` after their sample checks; the night's first step gates `next` (the essential gate) and
fast-forwards `main` to it.

The methods: deduplication (byte-identical and equivalent files counted once: `torture-execute-mirror`'s collapse, VM
report 156, the first case, GATE-SURVEY.md's note beside GT7), coverage subsets (the files that keep every back-end
block, every kept class, the controls and one per class: VM reports 160 and 161; the user, 2026-11-02: optimise for
those, keep every deferred and gap program, a `:subset` per corpus), rotation (the rest of a corpus over the nights),
and synthetic corpora where a corpus is large and repetitive. Heavy runs at night, development by day. The Mac peer takes
small development tasks. Nothing of GT8 is written yet: its survey first (GATE-SURVEY.md, a GT8 section).

## The backlog, before the tag, in this order

1. **The Mac VM's held branches** (each built and reported, held because repr/c or ci is embedded and a change reruns
   the whole `--full`), merged one at a time with their main-host checks:
   - 154 (`mac/154-builtins`, ci's missing builtins): first re-record its 7 `ref-gcc15` lines on amd64 (they came from
     aarch64's gcc: `bin/ci record-ref --corpus ci-run --only ...`), check the hand-written record rows, then
     `bin/ci c2j-check`;
   - 164 (`mac/164-bitint-wide`, `_BitInt` wider than 64 bits, the `:wide` class; carries 154's two commits: after 154);
   - 155 (`mac/155-float128`, `_Float128`/`_Float64x` compared exactly);
   - 157 (`mac/157-headers`, C header gaps 4 and 5; no recorded result moves);
   - 158 (`mac/158-transparent-union`, the checker's redeclaration through a `transparent_union` parameter): re-record
     the torture-compile annotations here (ccdump);
   - 159 (`mac/159-rt-control`, the round trip's levels 1-2 control): judge benchmarksgame's known file on amd64 (the
     `nbody-8` entry is VM-only); its docs (GATE-SURVEY.md, C23-SURVEY.md's controls paragraph);
   - 161 (`mac/161-subsets`, coverage subsets of all C run corpora): six amd64 `bin/ci cover` runs here;
   - 162 (`mac/162-overlay-key`, the overlay keyed by its inputs): `bin/jrt overlay` twice on the main host;
   - 163 (`mac/163-c-keys`, the C steps keyed by the namespaces they load): confirmed by `bin/gate --audit` on the listed
     steps;
   - 165 (`mac/165-vla-aot`, VLAs at AOT: 9 of 18 `:vla` programs pass).
2. **CS268 (a)**, the `--record` writers' guess narrowed (VM task 167, `mac/167-cs268`).
3. **CS221 (a) with CS232 (a)**, conflicting specifiers printed as written, `:bad-specs-print` emptied (VM task 168).
4. **Java's `speed` curated** (GTS102): the per-test limits lowered after the next clean run, the catch-all's timeouts
   judged one by one.
5. The night-fix review (VM task 166) and its findings.
6. The journal starts over after the tag (the user's rule), as after v2: JOURNAL.md's header, README's documentation
   line.

## Open

- JCS127 (`mac/039-jcs127` `d6870d37`, volatile fields under Rt's striped locks): not in main; after a test on the host
  (it changes the runtime).
- GTS137 (a): the `--full` step `repr-java-jdk` (`:heavy true`), at the next reader change (GTS133 (b)).
- `arbace/c/repl.ae`'s back-end default and the judge tests that stand in for the compiled parser (night-fix): the
  source's fix waits for the next re-seed.
- The box can no longer build the program: `go/arbace/crt` calls `syscall.Access` and `syscall.Stat_t`, which TamaGo's
  syscall lacks (C9.2's hazard (e)); for the box's revival.
- Waiting for the user: none known; each survey's "Proposed" sections not marked "Decided" are the list.
- Working rules: subagents on Sonnet for as much as possible, Opus for the hard parts (the user, 2026-10-25/26); one
  20 GiB job at a time on the host; Go's build cache trimmed when the disk fills.

## Parked (the user, 2026-10-26; out of `bin/gate --full`, scripts kept, revisited piece by piece)

- The box (with its Mac bundle, the arm64 VM and the KU series), `tamago/` and `tamago/cmd/`'s forms, the macOS user interface (the Moshi Go note below) and the iOS app ([IOS-REPL-SURVEY.md](IOS-REPL-SURVEY.md)).
- KU3 (processes on arm64). A safety classifier stopped both agents before they wrote anything. The worktree `ku3` is
  kept, clean at `80294e9a`. Its findings: TamaGo's arm64 `osInit` reads ID_AA64ISAR0_EL1, ID_AA64ISAR1_EL1 and
  ID_AA64PFR0_EL1, which trap at EL0, so the kernel must answer them (proposal KS15, against KS2's letter, to decide when
  KU3 resumes); a Mac step needs the guest package in box-vendor's darwin/arm64 configuration and `image()` not skipping
  without /dev/kvm. KU4-KU6 were to go on, on amd64 alone.
- KU4 (the process manager, amd64). The classifier stopped its agent too, before it wrote anything (worktree `ku4`,
  clean at `728440fe`). Its findings are in the journal at `arbace-interprets-go1.27.1-v1` (2026-10-18, "KU4 parked").
  The KU series waits for the user's direction.
- A note from the user (2026-10-13): the tooling of the Mac GUI app Moshi Go may be useful for Arbace when its REPL gets
  a user interface beyond the terminal. Its build info (`go version -m`, on the user's Mac): module `moshi-go` at
  `1516bf8173a5` (2026-10-12), go1.27.1, darwin/arm64, `CGO_ENABLED=0`, `-trimpath`, tag `mygo_noinspector`; deps
  `github.com/egoist/mygo` v0.4.5 replaced by the fork `github.com/rjyo/mygo` (the GUI toolkit),
  `github.com/ebitengine/purego` v0.11.1, `go-text/typesetting` v0.3.5, `golang.org/x/image`, `alecthomas/chroma/v2`,
  `yuin/goldmark`, `creack/pty` and `charmbracelet/x`, `coder/websocket`, `dlclark/regexp2/v2`. A pure-Go, cgo-free GUI
  on macOS is the same shape as the box's launcher there.
- A touch terminal for Arbace's REPL, a clean-room reimplementation of Moshi's iOS terminal interface (the user,
  2026-10-14; long term): survey and plan in [IOS-REPL-SURVEY.md](IOS-REPL-SURVEY.md), proposals IR1-IR13 open.

## Long term

- The standalone Arbace: no Java binary compatibility, with Java at the source level through repr/java. The language
  is Clojure plus the Java forms, and later the Go forms. The goal is a self-sustaining REPL in a virtual sandbox, in
  Arbace down to the bare metal ISA, in the style of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux
  host) and Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler, GC), with
  proper tail calls possible.
- **Haskell and Rust unparked** (the user, 2026-11-03): crops in progress again (read and print with their round trips
  exist; eval and AOT not begun), their state checked afresh against the pinned GHC 9.14.1 and rustc 1.99.1. **Python
  surveyed** (doc/python/PYTHON-SURVEY.md, the user, 2026-11-03).
- More crops toward the ten (C++, OCaml, Scheme, Haskell, Rust, Python), each a row of the matrix, after GT8's budget;
  C++, OCaml and Scheme embed at a later re-seed (CS250's decided line).
- Futamura's projections ([FUTAMURA-SURVEY.md](FUTAMURA-SURVEY.md)): the second projection, j2g derived from the
  interpreter, one definition of each language's semantics; staging shared with F7 (G16); speed (F10's inline caches,
  specialization).
- A kernel and a user part in the box ([go/KERNEL-USER-SURVEY.md](go/KERNEL-USER-SURVEY.md)): KU0-KU2 done, KU3 and KU4
  parked.
