# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md) (since 2026-11-07), the journals at the tags
[`arbace-reaps-4-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-4-crops-v1/doc/JOURNAL.md)
(2026-11-01 to 2026-11-07),
[`arbace-reaps-3-crops-v2`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v2/doc/JOURNAL.md)
(2026-10-27 to 2026-11-01),
[`arbace-reaps-3-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v1/doc/JOURNAL.md)
(2026-10-20 to 2026-10-27),
[`arbace-interprets-go1.27.1-v1`](https://github.com/arbace/arbace/blob/arbace-interprets-go1.27.1-v1/doc/JOURNAL.md)
(2026-10-15 to 2026-10-20) and
[`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
(2026-10-11 to 2026-10-15), and the frozen branches' (before).

## State

- The bootstrap starts from the Go-source seed `seed/arbace-seed-go.tar.xz` (`d5548e72…`, committed `496d3348`,
  2026-11-05: the re-seed before the tag, C only: the backlog, the digraphs, the crops' Go forms out of the runtime's and
  javac's keys; before it `a59cc441…`, 2026-11-02, C9.2's, embedding C), the program printed by the Go-hosted fixpoint,
  its toolchain (TamaGo's go1.27.1) pinned by `seed/goroot.sha256`; `bin/seed --verify` passed (stage 0 = G1 = G2 = G3,
  `d503777b…`). `bin/seed --check`, `--build` (stage 0), `--make`, `--verify`.
- Stage 0 hosts every Arbace tool (c2j, j2g, repr/go, repr/clojure, build/go, the suite's runner, ci and repr/c),
  compiled into it; `bin/arbace-go --build` is a Go-hosted build, `--fixpoint` the chain G1 → G2 → G3. The program's
  namespaces are compiled to Go (`ARBACE_AOT=1`, since S6).
- The REAP matrix (`bin/reap`): four crops at the tag, Clojure, Java 26, Go 1.27.1 and C (C23 against gcc 15.2), 16 of 16
  cells checked, none failing, every non-pass in a recorded class ([FULL-SUPPORT-SURVEY.md](FULL-SUPPORT-SURVEY.md), "REAP
  v3's divergences"); with batch 1 (below) eight rows, C++, Scheme, OCaml and Python joining, 30 of 32 cells checked. The
  other crops (Haskell, Rust, LLVM IR, net/http) have their checks and classes, out of the matrix and of the gate; since
  2026-11-04 every crop has a native reader (on held branches for OCaml, Haskell, Rust, C++).
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
- **`arbace-reaps-4-crops-v1`** (2026-11-07, at `a4a84533`; the record's commit `99873f98`): C the fourth row
  (C1-C9, [C23-SURVEY.md](C23-SURVEY.md)): read 5,201 / 231 / 51, eval 2,111 / 124 / 33,
  AOT 2,120 / 115 / 33, print 4,768 / 5 / 78 (pass / by design / deferred); 16 of 16 cells, none failing ("REAP v3's
  divergences"). The merge window after v2 (the gate's branches and audits, the crops, JN10), C8, C9 and its re-seed window
  (C9.2, the seed `1fe1063f`), GT8 and the backlog, the re-seed of 2026-11-05 (seed `d5548e72`, verified). Its record: an
  audited clean run of the four crops over GT8's compressed corpora (`test/gate/clean-arbace-reaps-4-crops-v1.edn`):
  passed, 234 steps clean, every audited one ok, over three runs in two nights (77,700 s: the first run 39,146 s, then two
  `--only` reruns), the budget's two nights at most.

## Done: the tag `arbace-reaps-4-crops-v1` (2026-11-07)

The clean run's record passed (2026-11-07 04:24 UTC, at `99873f98`); tagged at `a4a84533` (the essential gate passed on it)
at the user's go-ahead, and the journal started over. Next: "After the tag" below. How it got there: The tag waited for GT8 and the backlog (the user, 2026-11-03), and stands on an audited
clean run of the four crops over GT8's compressed corpora within budget. The backlog is merged; GT8's tooling is on main (dedup, coverage subsets for C,
goi and Java's interpreted level, the nightly rotation, the tiers, `bin/gate --costs`).

The budgets (the user, 2026-11-04, GTS224-GTS226, GATE-SURVEY.md "Decided (2026-11-04)"):

| tier | budget | when |
|---|---|---|
| essential | at most 15 minutes (a goal) | each night's first step, on `next` (the staging branch); by hand rarely by day |
| nightly | 8 hours planned in the 12-hour night shift, 4 of slack for fixes and reruns | each night, a keyed `--nightly` over compressed corpora |
| clean | one night (about 10 hours planned), two at most | a fresh clone, every step audited, at a tag; it replaces that night's nightly |

The night shift is 19:00-07:00 UTC, for gating, testing, details and the bugs the checks find; the day is for development
and design with sample checks. Day merges go to the branch `next` after their sample checks; the night's first step gates
`next` (the essential gate) and fast-forwards `main` to it.

The nights to the tag:

1. **Night 4 (2026-11-04):** `next` gated and `main` fast-forwarded (the day/night decisions, goi's classes and frames:
   1,664 of 1,837); the cover build; Java compiled and Clojure AOT coverage; the first real `bin/gate --nightly`, measured
   by `bin/gate --costs` (`.tmp/night4/`).
2. **Night 5 (2026-11-04/05): done.** The re-seed (`.tmp/night5/reseed.sh`, C only, with the digraph commit
   8dfd5840; the crops' Go forms out of `arbace_runtime_key` and `arbace_javac_key`; the PATH_FIRST trio removed after)
   passed every step: seed `d5548e72`, verify `d503777b`, main at d86cb874a. The keyed `--full --concurrent` runs from
   03:14 to at most 16:00.
3. **Night 5's clean run (2026-11-05/06) and night 6's two `--only` reruns (2026-11-06/07): passed** (REAP v3's caveats,
   FULL-SUPPORT-SURVEY.md). The plan was: `next` gated and fast-forwarded first (with `java-compiled-subset`, GTS228, and
   `audit-race`, the reducer race of night 5's first gate); GTS229 (C's reads timed) by day from the keyed `--full`'s
   logs; then the clean run of the four crops, `.tmp/night5/clean.sh`, estimated about 12 hours, over one night's plan
   (the options to fit it: `.tmp/night5/clean-fit.md`). Then the tag, by hand, on the record.

## Batch 1 (merged on `batch1`, 2026-11-07, for tonight's night; not yet on `next`)

Merge plan batch 1 (`.tmp/tag-v2/merge-plan-rehearsal-1105.md` §4 step 1, `merge-plan.md` §3), on `e16e4c1d`, one merge
commit a branch: `cpp-cx8`, `cpp-cx9`, `cpp-cx10` + `gt9v-cpp`, `scheme-sm9` + `gt9v-scheme`, `ocaml-o8` + `gt9v-ocaml`,
`e6f88eb3` (embed-crops 1-2: the crops' c2j checks), `gate-crops`, `zed-survey`, `java-deferred-triage`,
`go-design-review`, `gt9-build` (with the keys commit `100f8e0b`, PYS30 (a)) and `gt9-verbs`, three of `crop-controls`'
commits picked (C++/LLVM, OCaml, Scheme: the fragments name their controls), GT9's fragments `cpp`, `scheme`, `ocaml`
spliced, `mac/201-python-py7-py11` (`7df8864b`; W2's embedding and the REQUESTs into goi, crt and the planner left out,
below), `crops-score` with its after-tag patch for goi (CSP5), and `cppi-check` (C++'s eval and AOT cells; not in the
plan, needed by the C++ row: `bin/reap` had 180 problems without it). Not done: `mac/192`'s redo (superseded: GT9's
fragments read SC233's and OC161's records by an `:id-re` of one group, GTS213 (a)).

- `bin/reap`: eight rows, 32 cells, 30 checked (Python's eval and AOT "no check yet" until its amd64 records), 0 problems.
- `bin/crops-score` (main → batch1; completeness / maturity / independence): C++ 0 / 0 / 20 → 75.4 / 90.2 / 20; OCaml
  0 / 0 / 20 → 75.6 / 100 / 20; Scheme 0 / 0 / 80 → 98.3 / 87.5 / 80; Python 0 / 0 / 20 → 0 / 83.3 / 20; Go 93.9 / 86.0 →
  94.1 / 87.5; C maturity 95.8 → 100; Java maturity 98.3 → 100; Haskell, Rust, LLVM IR 0 (their rows are later batches').
- Keys (first 12 hex): main gate `0276a6db9960`, runtime `f29a38759450`, javac `a3300f566f6c`; after the keys commit
  `98ffc164199f`, `c40e01ac561c`, `8c9a6a206dd7`; at batch1's head `d42c7a5dd92d`, `c40e01ac561c`, `ab86bd52d01b` (moved
  after the keys commit only by `bin/lib/version.bash`'s list gaining pyi's lazy modules and `bin/goi`'s CSP5 patch;
  the runtime key not since).
- Tonight (`bin/gate --full --plan`: 288 steps, 272 run, 2 skip; about 10.2 h of measured step seconds, serial, plus the
  builds and the three new crops' first passes, about 1.3 h estimated (GT9): about 11.5 h, over the 8 h plan): every
  step on `:gate`, `:prog`, `:javac` and `:javac-prog` (the keys commit, `bin/arbace-go`, `bin/goi`, embed.ae), C's
  steps (crops-score's CSP5 records and scripts are among their inputs, 2.1 h), and the new crops' 59 never passed.

### Proposed (batch 1)

1. **Tonight's fit.** (a) The essential gate, `bin/reap`, `crops-score` and the three crops' chains tonight, the rest of
   `--full` the night after (keyed: what passed is kept); cost: a second night. (b) Leave CSP5's C part (crops-score's
   rewritten `test/c/ref-gcc15.edn`, `test/repr/c/{annot,roundtrip}-results.edn` and their writers) for the re-seed window
   W, which reruns C anyway; saves C's 2.1 h tonight; cost: C's maturity 100 → 95.8 until W, a revert commit now.
   (c) Run `--full` whole into the slack (about 11.5 h). *Recommended: (a).*
2. **The deferred parts of Python's branch.** Left out of the merge (taken as next's side, so a later merge of the branch
   does not bring them; they are cherry-picked): W2's embedding `101c0d03`, `a420fa0d`, `0525fa89` (with `7ab5364b`'s
   `bin/pyi table-check`, merged but failing until then), the REQUESTs `656dfcdc` and `c9047d52` (goi's), `3db255c5` (crt for
   tamago), `cc463c8d` (the planner's `:slice-re`). (a) The embedding and goi's and crt's REQUESTs at W (PYS18 (a)), the
   planner's with GTS240's runner change (GTS243 (a)), and `test/gate/crops/python.edn` spliced after Python's records
   night; (b) the REQUESTs now, on a day branch with their owners' sample checks; cost: a gate rekey of goi's and C's steps.
   *Recommended: (a).*
3. **cppi-check's `cpp-aot-essential`** (4 GiB measured) is not a step: GTS215 (a) holds a new crop's essential checks to
   3 GiB. (a) Leave it out (the AOT level is checked in `--full`'s `cpp-aot`); (b) run it at `CPPI_THREADS=1` and measure;
   (c) raise C++'s essential limit. *Recommended: (a), then (b).*
4. **cppi-check outside the plan.** Merged because the C++ row needs it (its records, the sampling, the cells); the
   2026-11-06 branch `cppi-deferred` (CP230-CP231, on `cppi-check`) is a clean descendant of batch 1's and the next C++ step.
   (a) Merge `cppi-deferred` with the next batch's day; (b) with batch 1 before tonight. *Recommended: (a).*
5. **Candidates of the 2026-11-06 day branches** (clean on batch 1's branches, no later batch's ancestry, not merged):
   `cppi-deferred` (on cx8-10, cppi-check), `ocaml-libs4` (on o8), `scheme-smr7` (on sm9), `java-jc9c` (on
   java-deferred-triage); independent of every batch (on main's line): `clojure-cdr`, `goi-escape`, `c-deferred`.
   (a) A batch 1b day for them after tonight's gate; (b) each with its crop's later batch. *Recommended: (a) for the four
   on batch 1's branches (they extend its rows), (b) for the three others.*

### Decided (the user, 2026-11-07)

- Proposal 1 (tonight's fit): moot. The night shift is nominally 12 hours (19:00-07:00), overflow into the morning allowed
  to 12:00; tonight runs the whole keyed `bin/gate --full` after the essential gate.
- Proposal 2: Python's deferred parts at the re-seed window W (PYS18 (a)), the planner's `:slice-re` with GTS240, the python
  fragment after Python's records night: (a).
- Proposal 3: `cpp-aot-essential` stays out of the essential tier; measured tonight at `CPPI_THREADS=1`, added if it fits
  3 GiB.
- Proposals 4 and 5: all seven 2026-11-06 day branches into batch 1 today, so that one rekey night carries them:
  `cppi-deferred`, `ocaml-libs4`, `scheme-smr7`, `java-jc9c`, `clojure-cdr`, `goi-escape`, `c-deferred`.

### Batch 1, second part (the main session's `go-controls`, then the seven, 2026-11-07)

On `f7696201` (the main session's merge of `go-controls`: Go's controls, Clojure and Go above 90% on all three), one merge
commit each, at their heads: `cppi-deferred` 948a17e7 (clean), `ocaml-libs4` 628555d4 (Makefile), `scheme-smr7` 9b672f2f
(`steps.edn`: GT9's Scheme steps kept, smr7's one change applied: `si-check` without `:chez`), `java-jc9c` e92a1c8e (clean),
`clojure-cdr` 67adb0ff (clean), `goi-escape` 91d3eb40 (`bin/goi`'s flags: goi-escape's `-m`/`-d=` handling with
go-design-review's judged `-race`; `test/goi/check-linux-amd64.edn` merged row by row, no row changed on both sides),
`c-deferred` 61406468 (journal). None contains 53794ed6.

- Keys (12 hex): at `f7696201` gate `30011618c1ec`, runtime `c40e01ac561c`, javac `ba31e02c1c3c`; at the end `1f886836e11e`,
  `4cd0885457fb`, `c97e87dc55a5`. Moved by: `ocaml-libs4` (`arbace/o2j.ae`: gate, javac), `java-jc9c` (jrt, j2g, javac's world:
  all three), `clojure-cdr` (`Compiler.ae`, jrt, j2g, `overlay/jdk`: all three), `goi-escape` (goi: gate, javac), `c-deferred`
  (ci, crt, repr/c: all three); `cppi-deferred` and `scheme-smr7` moved none.
- `bin/reap`: 30 of 32 cells checked, **1 problem until tonight's C++ record**: `runtime/deferred.cpp` is in g++'s reference
  (cppi-deferred) and not yet in `test/cpp/check-linux-amd64.edn` ("the records are not written by hand", its doc). The matrix
  was recorded with that one reference row held out for the run only, so every cell holds its records' numbers; the cells that
  wait for the night's records stay as their branches recorded them (goi-escape's hand-written rows, ocaml-libs4's, smr7's
  mats, clojure-cdr's suite counts), Python's eval and AOT "no check yet". `bin/crops-score --judge --strict`: 0 problems.
- Scores (completeness / maturity / independence): Clojure 99.0 / 96.3 / 100, Java 72.6 / 100 / 90, Go 94.9 / 95.8 / 100,
  C 95.1 / 100 / 20, C++ 75.4 / 91.7 / 20, OCaml 80.9 / 100 / 20, Scheme 98.4 / 87.5 / 80, Python 0 / 83.3 / 20, Haskell
  0 / 8.3 / 20, Rust 0 / 8.3 / 20, LLVM IR 0 / 4.2 / 40.
- `bin/gate --plan`: 40 steps, 34 run; `--full --plan`: 288 steps, 274 run, 8 after; about 10.3 h of measured step seconds
  (Java 4.8, C 2.1, Go 1.7, shared 1.0, Clojure 0.7) plus the builds and the three crops' first passes (about 1.3 h): about
  11.6 h, before the conversion below and the records after.

### Tonight, in order (heavy ones under `bin/heavy-lock`; one heavy job at a time)

0. Before the gate, else its builds and `j2g-edge` fail: `java-jc9c`'s variants (F2, F3, F4, F12, F13) and `clojure-cdr`'s
   world (CDR4) name classes `jdk/forms` does not hold yet ("not mergeable before its night", JAVA-DEFERRED-TRIAGE), with
   JC9's F1, F5 and JC9b since the third part. First the joint javac check of JC9's lists against jrt's overlays (JC9b's
   `.tmp/jc9b/all9/run.sh` in its worktree, a minute, 1 GB). One conversion takes every area: `REPR_JAVA_EXE=<the newest .tmp/gate-exe/javac-*/arbace-javac> REPR_JAVA_STALE_OK=1
   bin/jrt-convert update` (heavy, the javac tool), `bin/jrt modules`, `bin/j2g-edge update` (frozen JVM), `bin/javac-vendor
   edge`, `bin/generated-check` (the module table), commit. Then `CPPI_THREADS=2 bin/cppi check --record` and `--aot --record`
   (samples and runtime, about 4 GB), `bin/reap --record`, `bin/crops-score --record`, commit: `bin/reap`'s one problem clears.
1. The essential gate on `next`; `bin/gate --pins`.
2. `bin/gate --full`, keyed (above).
3. The records the merged branches ask for, after the gate:
   - goi: `bin/goi check --record` whole (goi-escape's 96 rows confirmed), `bin/goi divergences`.
   - C (c-deferred): `bin/ci check --record`, `check --aot --record`, `bin/ci annotations --only` the touched files,
     `bin/repr-c roundtrip --record`.
   - C++: `bin/cppi check --only test/cpp/essential-check.txt` and `--aot`; `cpp-aot-essential` measured at `CPPI_THREADS=1`
     (added if within 3 GiB); `bin/cppi check --corpus libstdcxx --sample 25 --record` and `--aot` (about 10 GB, 50 min and
     1.5 h); `bin/cppi record-ref --corpus gxx-dg-run` (1 h) and `check --corpus gxx-dg-run --record` (40 min); its `--aot`
     (about 8 h) is a night of its own.
   - OCaml (ocaml-libs4's list): `bin/oi record-ref`, `suite record-ref`, `check --record`, `suite check --record`,
     `check --aot --record`, `suite check --aot --record`, `O2J_SHARD=8 ... --level compiled --record` both (4 to 5 h, up to
     10.8 GB a shard), `bin/oi pins --attest`, the divergences.
   - Scheme (smr7): `bin/si ecraven` whole (`SI_TIMEOUT=300`), `bin/si mats --record` whole.
   - Clojure (clojure-cdr): unskip java-interop's test-dot and test-bean, the suite re-recorded on the test build, `bin/clojure-tests
     --aot` (unskip genclass and the two), `bin/oracle check`, `bin/diffcheck`; CDR1's locale trial optional.
   - Java (java-jc9c): the smoke programs and `bin/java-check --levels interp,compiled` on F12's and F13's areas, `--save`.
   - Last: `bin/reap --record`, `bin/crops-score --record`.
4. Python's amd64 records: a night of their own (as in part one).

### Proposed (batch 1, second part)

6. **java-jc9c without its prerequisites.** Its doc merges it "after `next` holds JC9 and JC9b" (F1 `java-jc9-jca`, F5
   `java-jc9-logging`, `java-jc9b`, `java-jc9b-natives`: none is on main or in batch 1; PKI and TLS need F1's JCA). Merged as
   decided, it may leave tonight's conversion or build failing, which would stop the whole night. (a) Revert its merge today
   (`git revert -m 1 5c67e7145`) and give the JC9 line its own day and conversion night; cost: Java's F2-F4, F12, F13 wait.
   (b) Merge the four prerequisites into batch 1 today (6, 4, 8 and 11 commits; conflicts in the triage doc, `arbace/j2g/model.ae`,
   Java's and Clojure's classes, `cells.edn`, `DriverManager.java`, `go/arbace/jrt.ae`); cost: about an hour by day, a larger
   conversion tonight. (c) Keep it and try step 0; on a failure revert it at night. *Recommended: (a).*

### Decided (the user, 2026-11-07, second round)

- Proposal 6: (b), JC9's four prerequisites merged today; if tonight's conversion fails, the main session reverts `java-jc9c`
  and the four and runs the gate without them (the commits below).
- `java-judge` (JCV1) and `scheme-90` (SCS1) merged; crops-score's mixed stage is the case-weighted mean (doc/CROPS-SCORE.md,
  "Decided (the user, 2026-11-07): a mixed stage"); JCV2 to be built (JCV5, JCV6 not now); SCS2 (a): the scheme-chez-dumps pack
  when `oracle-records-cpp2` merges (not today).

### Batch 1, third part (JC9's four, java-judge, scheme-90, 2026-11-07)

Merge commits (for a revert at night, newest first: `git revert -m 1 ID` each, in this order; `java-judge`'s merge interleaves
`bin/java-check`'s F6 lines with JCV1's, so `java-jc9c`'s revert there may need a hand fix):

| branch | head | merge commit on batch1 |
|---|---|---|
| `java-jc9b-natives` | f89e8e9c | `fb4689814` |
| `java-jc9b` | 51b320c1 | `418af3c7b` |
| `java-jc9-logging` | b8a1951c | `ef37fdb03` |
| `java-jc9-jca` | a4a8e546 | `7d49a3249` |
| `java-jc9c` (part two) | e92a1c8e | `5c67e7145` |

Then `java-judge` 3ab550e0 (`b4216bacc`) and `scheme-90` 43b39f4a (`eb49dcddc`), crops-score's mixed stage (`a6a10d703`).

- Resolutions of note: `DriverManager.java` (both added) clojure-cdr's, F16's java.sql files out of `small-members-sources`
  (CDR4 holds them); `jrt_sources.ae` jca-sources and logging-sources both; `go/arbace/jrt.ae`'s lists both; Java's records
  rederived by `bin/java-check rejudge-records` (equal to java-judge's but t05507, its hand edit kept); JCV1's patch applied to
  `test/crops/independence.edn`; scheme-90's controls replace crop-controls' picked Scheme controls (`test/scheme/repr-controls.edn`
  removed, the steps name `test/scheme/controls.edn`), gt9v-scheme's `--only-list` applied again on scheme-90's
  `arbace/repr/scheme.ae`; scheme-90's two diffs applied (crops.edn's rules, independence.edn); `si-corpora` without `:chibi`, as
  scheme-90 declares it (else Scheme's judge was refuted).
- Keys (12 hex): at `1dd5bcc71` gate `1f886836e11e`, runtime `4cd0885457fb`, javac `c97e87dc55a5`; at the end `2f25331ada6f`,
  `b74523271f12`, `f221ad0dfe97`, moved by JC9's four (jrt, jrt's closure, overlay/jdk: all three); java-judge, scheme-90 and
  crops-score's change moved none.
- `bin/reap`: 30 of 32 checked, the one problem as before (deferred.cpp until tonight's C++ record); Java eval 1,864 / 335 /
  1,123, AOT 1,937 / 308 / 1,077 (JCV1). `bin/crops-score --judge --strict`: 0 problems.
- Scores: Clojure 99.0 / 96.3 / 100, Java 72.9 / 100 / 99.7, Go 94.9 / 95.8 / 100, C 95.1 / 100 / 20, C++ 75.4 / 91.7 / 20,
  OCaml 80.9 / 100 / 20, Scheme 98.4 / 100 / 95.0, Python 0 / 83.3 / 20, Haskell 0 / 8.3 / 20, Rust 0 / 8.3 / 20, LLVM IR
  0 / 4.2 / 40.
- Plans: essential 40 steps, 34 run; `--full` 288, 274 run, 8 after; about 10.3 h measured plus about 1.3 h (unchanged).
- Tonight's step 0 conversion (above) now also carries F1, F5 and JC9b (F16, F8, F7, F11): one `bin/jrt-convert update` for every
  area; JC9b's doc notes that F5's `Logger`/`LogRecord` called `ResourceBundle.getBundle(String, Locale, ClassLoader)`, which jrt's
  overlay lacked (its "Findings": the joint javac check `.tmp/jc9b/all9/run.sh` before the conversion, a minute, 1 GB).
- **JCV2 not built here**: 15 golden files reviewed line by line against the JLS sections the programs cite, their output to be
  produced first (the reference records only digests), and `verdicts.edn`/`run.ae` taught a `:golden` kind for them: a
  reviewing task of its own (an agent, Opus), after tonight's gate.

## Batch 2 (merged on `batch2`, 2026-11-07, on `batch1` 0edacad6; for the day after batch 1's night)

Merge plan batches 2 and 3 together (`.tmp/tag-v2/merge-plan-rehearsal-1105.md` §4 step 3, `merge-plan.md` §3), each line at its newest
head the slot allows, one merge commit a branch: rsi `rust-re5`, `gt9v-rust`, `rust-re6`; LLVM `llvm-read`, `cpp-lower-dbg`, `llvm-full`,
`llvm-verify3`, `gt9v-llvm`, `llvm-ll6` (it holds `cpp-native`, `cpp-native2`, `cpp-sema`, `cpp-codegen`, `cpp-codegen2`, `cpp-g2fix`,
`cpp-templates` and `llvm-row`: batch 5's C++ native line comes with it), `llvm-90`; OCaml `ocaml-native`, `ocaml-errors`, `ocaml-errors2`,
`crop-controls-ocaml`, then `ocaml-t12` and `ocaml-90` (through `batch2-ocaml`, an agent's merge onto batch 2 with `ocaml-libs4`'s
libraries); Rust native `rust-native`, `rust-unicode`, `rust-errors`, `rust-errors2`, `crop-controls-rust`, `rust-rf9b` (RF1-RF9b),
`rust-rf3b`; `rust-row` (Rust's REAP row, not in the plan: the Rust row's cells); crop-controls' Rust round-trip control `7bf1d1e4` picked.
None contains 53794ed6. Skipped: `llvm-ll4` (LL4's corpus loop, a line of its own that conflicts with LL9a: LLS32), `rust-res34` (RES34
write-ahead, changes C's `arbace/c/` and `bin/ci`: its own day), `oracle-records-ocaml`/`-rust` (ORS12, later), `ocaml-libs1` (one doc
commit, OT11-OT15's decision text, already in libs4's doc).

- **Conflicts of note.** `arbace/cpp/lower.ae`: batch 1's lowering (CX8, rust-re4/re5's vectors and rustc intrinsics, cppi-deferred,
  cpp-lower-dbg) against ll6's (LL5's vectors, LL6's `*ci*`): integrated by an agent (`batch2-lower` f703fceb, 3550feb7: LL5's vectors and
  one bitcast path, both va_list schemes kept apart by `*ci*`, a packed-vector size bug fixed); doc/cpp/LLVM.md split by ll6 into
  doc/llvm/SPEC.md and LOWER.md with batch 1's sections 7-11 merged in; `arbace/c/build.ae` (re6's thread-local statics with c-deferred's
  VLAs); `test/gate/steps.edn` (llvm-90's eight steps and group beside GT9's, their `:controls` added); `test/reap/cells.edn` (ten rows);
  repr/ocaml's native-check (crop-controls' hook on errors2's); repr/rust's lexer (unicode's emoji test, errors' error path); OCaml's
  `bin/oi` (the native library and libs4's unix, threads, str and compiler-libs). GT9: Rust's fragment parts spliced (`llvm-rust.edn`
  without its LLVM interim step, `natives.edn`'s Rust part); OCaml's steps off `:ocaml` (OC180), `ocaml-oracle.edn` waiting for the
  planner's `:phase :oracle`.
- **Keys** (12 hex; batch 1 gate `4178700aed97`, runtime `b74523271f12`, javac `0da7d3f90cee`): at the end `1bfe8ffffe7e`, `6b114cbcf8f0`,
  `54e889c46334`. Moved by `rust-re6` (all three: `go/arbace/{ci,crt}`, `arbace/c/build.ae`, `tools/cirun`: C's embedded runtime, rsi's
  threads) and `llvm-ll6` (gate, javac: `arbace/llvm/` joining `arbace_crop_paths` and embed.ae's exclusions); every other merge moved none.
- **Checks (day, light).** gate.crops check 0 problems; `bin/gate --plan`, `--full --plan` clean; C `check --only essential` and `--aot`
  (re6's ci) pass, 4 and 1 recorded failures now pass (binary-trees-5 with threads); llvmi `--essential` 10/10 and a 70-module sample on C's
  own target; the lowering's samples (cppi lower 9/9, 17/17, essential 6/6, libstdcxx sample 39/39, llvmi 175 by feature, rsi native leg);
  OCaml's (the agent's: typer-check 79/79, essential 58/58 both paths, native-check samples 8/8, round trips, libs4's sample 14/18 native).
  One found: `runtime/thread-throw.cpp` on ci now terminates in `cx$thread_start` (re6's threads), its class `threads` extended.
- **`bin/reap`**: ten rows, 40 cells, 37 checked (Python's eval and AOT, Rust's AOT "no check yet"), 1 problem (deferred.cpp's, tonight's
  C++ record; the matrix recorded with that row held out as in batch 1). **`bin/crops-score --judge --strict`**: 0 problems.

| crop | batch1 (completeness / maturity / independence) | batch2 |
|---|---|---|
| Clojure | 99.0 / 96.3 / 100 | 99.0 / 96.3 / 100 |
| Java | 72.9 / 100 / 99.7 | 72.9 / 100 / 99.7 |
| Go | 94.9 / 95.8 / 100 | 94.9 / 95.8 / 100 |
| C | 95.1 / 100 / 99.7 | 95.1 / 100 / 99.7 |
| C++ | 75.4 / 91.7 / 20 | 75.4 / 91.7 / 20 |
| OCaml | 80.9 / 100 / 20 | 80.9 / 100 / 87.4 |
| Scheme | 98.4 / 100 / 95.0 | 98.4 / 100 / 95.0 |
| Haskell | 0 / 8.3 / 20 | 0 / 8.3 / 20 |
| Rust | 0 / 8.3 / 20 | 75.0 / 95.8 / 20 |
| LLVM IR | 0 / 4.2 / 40 | 95.9 / 93.3 / 88.3 |
| Python | 0 / 83.3 / 20 | 0 / 83.3 / 20 |

- **`bin/gate --full --plan`** on batch 2: 309 steps, 295 run, 8 after; `--costs`: nightly 10.3 h of measured step seconds, 86 steps
  unmeasured (the crops' first passes: LLVM's 10, Rust's 12 never passed; about 2 h more by GT9's estimates): about 12 h, over the 8 h plan.

### Batch 2, second round (rust-90, haskell-90, cpp-90, LLS31 (b); 2026-11-07, on `batch2` 590a8588)

One merge commit each: `rust-90` 58ca6783 (`105c549d8`), `haskell-90` 0fb4591b (`b71b3e9be`; no commit containing 53794ed6), `cpp-90`
4e0a8fc5 (`ef0af119c`); then LLS31 (b) (`f95e6c26b`) and the decisions' docs (`ffbcc29cd`).

- **Conflicts of note.** rust-90: `arbace/repr/rust/native.ae` (errors2's rejecting dump kept, rust-90's `tree` verb beside `errors`,
  its `file-trees` taught errors2's diagnostics); `test/gate/steps.edn` (rust-90's six Rust steps replace GT9's four of the same names;
  GT9's rustc levels `rust-iface`, `rust-behaviour`, `rust-native-essential`, `rust-native` kept with a `:rust-helper` group until the
  planner has `:phase :oracle`; `rsi-aot` on the packs; `rsi-essential`'s 4 GiB moved to `--full`, RES61). **Moved:** rust-90's edit
  controls to `test/rust/roundtrip-edit-controls.edn` (GT9's crossed pairs keep `roundtrip-controls.edn`), their switch
  `REPR_RUST_EDIT_CONTROLS`. haskell-90 (on old main d86cb874, 164 commits): LICENSE.md (its musl section folded into the whole
  COPYRIGHT's), Makefile, `bin/gate` (the new crops' pins and the oracles' packs both), `bin/arbace-go` (batch 2's gate_key:
  `arbace_not_crops` already covers hs, libm, hsi), `test/reap/{cells.edn,main.ae}` (eleven rows; one `:corpus-known` reader with
  `:hs-modules`), `test/gate/crops/haskell.edn` (haskell-90's: the oracle tier alone); its five steps auto-merged, their `:controls`
  added. Its packs copied (no clobber) into the main checkout's `.tmp/oracle-packs/` (native-inputs 2, stg-progs 2).
  cpp-90: LICENSE.md, Makefile (`cppi-codegen-check` once), `bin/cppi` (each verb once), `bin/lib/repr-cpp/roundtrip`, ARBACE.md,
  CPP-SURVEY; `arbace/cpp/lower.ae` auto-merged on batch2-lower's integration (cpp-90's x86_fp80 literal already in it). The three
  lines' independence and crops-rules diffs applied (the facts moved into `:stages`; Haskell's ci C cases 0.5 now that batch 1
  derives musl without gcc).
- **LLS31 (b)** (the user): LLVM's four first-record families (2,558 cases on lost clang-made inputs) kept in `cells.edn` as
  `:role :beside`; read fact 1, judge (2,068 + 0.5 x 948) / 3,016 = 0.843. LLS31 (a)'s pack `rt` planned precisely in
  doc/llvm/LLVM-CROP-SURVEY.md, "LLS31 (a): the pack rt, planned".
- **Keys** unchanged by all five commits: gate `1bfe8ffffe7e`, runtime `6b114cbcf8f0`, javac `54e889c46334` (every changed file is under
  `arbace_crop_paths` or no key's).
- **Checks (light).** After each merge: `gate.crops check` 0 problems; `bin/gate --plan` (49 steps, 43 run) and `--full --plan` (319, 305
  run, 8 after) clean; `bin/reap` 41 of 44 cells checked, 1 problem (deferred.cpp's, tonight's record; the matrix recorded with that
  row held out); `bin/crops-score --judge --strict` 0 problems after `--record`. Crops: `bin/rsi judge-test`, `bin/repr-rust controls`
  (7 of 7), a native round trip of two std files (equal at every level, the controls failing); `bin/repr-haskell test` (18 ok), `bin/hsi
  own-verdicts --check` (580 of 908), `bin/oracle-records pins` (the seven Haskell packs); `bin/repr-cpp roundtrip-corpus samples --judge
  own` (9 of 9, the control failing, 1.4 GB). Found: `bin/hsi own-verdicts --check` fetched GHC's source and testsuite tarballs and nofib
  from the network into the worktree's `.tmp/` (no local copy there; the pins matched): a worktree should point `bin/hsi`'s work and
  repr-haskell's corpus at the main checkout's before Haskell's checks.

| crop | batch2 before (590a8588) | batch2 after (ffbcc29c) |
|---|---|---|
| Clojure | 99.0 / 96.3 / 100 | 99.0 / 96.3 / 100 |
| Java | 72.9 / 100 / 99.7 | 72.9 / 100 / 99.7 |
| Go | 94.9 / 95.8 / 100 | 94.9 / 95.8 / 100 |
| C | 95.1 / 100 / 99.7 | 95.1 / 100 / 99.7 |
| C++ | 75.4 / 91.7 / 20 | 75.4 / 100 / 31.4 |
| OCaml | 80.9 / 100 / 87.4 | 80.9 / 100 / 87.4 |
| Scheme | 98.4 / 100 / 95.0 | 98.4 / 100 / 95.0 |
| Haskell | 0 / 8.3 / 20 | 96.3 / 100 / 74.9 |
| Rust | 75.0 / 95.8 / 20 | 75.0 / 95.8 / 80.0 |
| LLVM IR | 95.9 / 93.3 / 88.3 | 96.0 / 100 / 96.9 |
| Python | 0 / 83.3 / 20 | 0 / 83.3 / 20 |

- **`bin/gate --full --costs`**: nightly 10.3 h of measured step seconds (319 steps, 96 unmeasured: Haskell's 5, Rust's 17, LLVM's and
  the others' first passes), about 3 h more for them by the lines' own estimates (hsi-check and hsi-check-aot heavy, Rust's round trips'
  33,346 files): about 13 h, over the 6 h budget: proposal 2 (c), two nights (below).

### Batch 2, third round (batch1's last head, cpp-indep, res34, hfs-i1, proposal 8 (a); 2026-11-07, on `batch2` 93db65e8)

One merge commit each: `mac/batch1` 23d640f5 (`69d8704c2`: java-complete's F10a), `mac/cpp-indep` 605c62e6 (`2bc42fc1c`; scores
recorded `5fc449d4e`), `mac/res34` 5d85bff6 (`b3de90d41`), `mac/hfs-i1` ce89c383 (`d0964c426`); then proposal 8 (a) (`2f9eb4fa4`, `2a124fbe9`).

- **Conflicts of note.** batch1: the journal and JAVA-DEFERRED-TRIAGE (F10a's "As built" with JCS162-JCS167's proposals before batch 2's
  "Decided"). cpp-indep: `bin/gate`'s oracle-packs pins (the same block as haskell-90's, kept in batch 2's place), the Makefile (haskell-90's
  `oracle-records-*` targets already in), `test/oracle-records/kinds.edn` (C++'s six kinds beside Haskell's), CPP-SURVEY (batch 2's Decided
  CP240/CP241/CP243 kept, "As built: cpp-indep" after it); its facts (`.tmp/independence.diff`) applied to `test/crops/independence.edn`.
  Two `--full` parts (`cpp-check.gxx-dg-run-every-25`, `cpp-aot.gxx-dg-run-every-25`) moved to `test/gate/crops/cpp-oracle.edn` (waiting for
  `:phase :oracle`, like `ocaml-oracle.edn`): `--full` 319 to 317 steps. res34: `arbace/c/cf2g.ae` (batch 1's CS277 `apack`/`vplen` with
  RES34's placeholders and `ref-expr`, both kept), the Makefile (`rsi-aot-lib`, `ci-aot-lib`), RUST-EVAL-SURVEY. **Moved:** res34's proposals
  RES61-RES63 renumbered **RES64-RES66** (RES61 is batch 2's rsi-essential memory). hfs-i1: FRONTEND-SURVEY (batch 2's Decided HFS-I1-HFS-I4
  kept); its facts applied (the ref `"hfs-i1"`, no score change); its switch diff not applied (after the night's `compare-front`).
- **Proposal 8 (a), built**: `bin/lib/repr-haskell/inputs.bash` (`haskell_take`, `haskell_take_git`): in a linked worktree or the clean
  clone, GHC's source and testsuite tarballs are hard-linked from the main checkout's `.tmp/repr-haskell/corpus` and `.tmp/hsi/src`, nofib
  fetched from its `.tmp/hsi/nofib`; when the main checkout has none, a refusal, never a download; every pin checked after as before.
  `bin/repr-haskell corpus fetch` and `bin/hsi pins --fetch` take them; the Haskell steps declare `bin/lib/gate-cache.bash`. The main
  checkout had no copy: seeded from batch 2's (hard links, pins verified; nofib at d4750745, tree 1f5a8852). Checked: `bin/hsi pins
  --fetch` with the worktree's copies moved aside takes all three from the main checkout (no fetch); a missing one is refused.
- **Keys** (12 hex; before `1bfe8ffffe7e`, `6b114cbcf8f0`, `54e889c46334`): after batch1 `6725b5801608`, `cb6a54899d99`, `343e3c44918d`
  (all three: F10a's `go/arbace/jrt`, `overlay/jdk`, j2g's names); cpp-indep none; res34 gate `fc2cc71ebe1f`, javac `cc2abcd735f9`, runtime
  unchanged (RES34's `arbace/c/{build,cf2g,runtime,aotlib}.ae`: C's embedded set); hfs-i1 and proposal 8 none. At the end: gate
  `fc2cc71ebe1f`, runtime `cb6a54899d99`, javac `cc2abcd735f9`.
- **Checks (light), after each merge:** `gate.crops check` 0 problems; `bin/gate --plan` 49 steps, 43 run; `--full --plan` 319 (batch1),
  then 317 steps, 303 run, 8 after; `bin/reap` 41 of 44 cells checked, 1 problem (deferred.cpp's, the night's C++ record: CP245);
  `bin/crops-score --judge --strict` 0 problems (after `--record` at cpp-indep). Crops: `bin/oracle-records pins` (ten packs with C++'s two
  and Haskell's new `haskell-ifaces-ghc-9.14.1`, the tampered-pack control refused), `bin/repr-cpp pp-check --essential` on the derived
  tree (1.47 GB), `bin/rsi judge-test` (12 of 12), `bin/repr-haskell test` (ok). Not run by day: C's and Rust's `check --aot` after RES34
  (the shared cache `.tmp/ci-aot-lib` is empty, its first build is the night's), cppi's runtime-ci (3 GB, CP246).
- **Found:** `bin/repr-haskell corpus fetch` runs the packages' configure scripts (the host's C compiler) in native mode: its skip test
  (`[ -n "$live" ] || [ ! -f "$ninputs_pin" ] || ... && continue`) is inverted against its comment; the native reads take the products
  from the pack anyway (proposal 9 below).

| crop | batch2 before (93db65e8) | batch2 after (2a124fbe) |
|---|---|---|
| Clojure | 99.0 / 96.3 / 100 | 99.0 / 96.3 / 100 |
| Java | 72.9 / 100 / 99.7 | 72.9 / 100 / 99.7 |
| Go | 94.9 / 95.8 / 100 | 94.9 / 95.8 / 100 |
| C | 95.1 / 100 / 99.7 | 95.1 / 100 / 99.7 |
| C++ | 75.4 / 100 / 31.4 | 75.4 / 100 / 54.3 |
| OCaml | 80.9 / 100 / 87.4 | 80.9 / 100 / 87.4 |
| Scheme | 98.4 / 100 / 95.0 | 98.4 / 100 / 95.0 |
| Haskell | 96.3 / 100 / 74.9 | 96.3 / 100 / 74.9 |
| Rust | 75.0 / 95.8 / 80.0 | 75.0 / 95.8 / 80.0 |
| LLVM IR | 96.0 / 100 / 96.9 | 96.0 / 100 / 96.9 |
| Python | 0 / 83.3 / 20 | 0 / 83.3 / 20 |

### Batch 2's nights (decided 2 (c): over two nights; one heavy job at a time under `bin/heavy-lock`; at most 1.5 GiB a process outside the gate)

Night A (on `next` once batch 2 is there; the rekey night):

0. On batch1/next, before the gate: the C++ runtime's IR pin re-recorded on clang 23.1.5 (proposal 1 (a), decided: `bin/cppi
   runtime-build --update`, the `!ident` line only); not run on `batch2`.
1. The essential gate (rekeys everything on `:gate`, `:prog`, `:javac`: re6, ll6's list), `bin/gate --pins` (now also `bin/oracle-records
   pins`: the seven Haskell packs in the store), `bin/reap`, `bin/crops-score --judge --strict`.
2. The four new crops' `--full` chains, keyed: LLVM (`llvm-full`), Rust (`rust-full`: `rsi-essential` first, 4 GiB; `rust-roundtrip`
   over 33,346 files natively, RES59 (a)), Haskell (`haskell-roundtrip`; `hsi-check` 10 GiB and `hsi-check-aot` 8 GiB, heavy), OCaml.
3. Records (one process at a time):
   - C++ (`CPPI_THREADS=2`): `bin/cppi check --record`, `--aot --record` (deferred.cpp, thread-throw's new shape; up to 6.7 GB at one
     thread: under the heavy lock); cpp-90's own round trips `bin/repr-cpp roundtrip-corpus KEY --judge own --record-results` for its six
     keys (1-2 h), then CP243 (a) (CPP-SURVEY "The night's commands"); the routes compared (`CPPI_ROUTE=auto bin/cppi check --corpus
     gxx-dg-run --sample 25`, `--aot`, the samples).
   - Haskell: `bin/hsi dumps --record` (the libraries' STG at this helper: GHC, the oracle tier, by hand), then `bin/repr-haskell
     roundtrip --text --layout` whole, `bin/hsi check --corpus nofib,samples,synthetic,testsuite-core --record` and `--aot --record` (T16846
     alone 10 GB: `HSI_MEM_CAPS`).
   - Rust: `bin/rsi ir-pack --corpus ui` (rustc, the oracle tier; then commit `test/rust/ir/ui.sha256`, RES56 (a)); `RSI_BATCH=2
     RSI_THREADS=1 RSI_JOBS=2 RSI_CI_MEMLIMIT=1400MiB bin/rsi check --corpus samples,synthetic --record` (about 2 h), RES61's measure
     (`RSI_BATCH=1 bin/rsi check --essential` under `time -v`).
   - C++ (cpp-indep, before C++'s other records; doc/cpp/CPP-SURVEY.md, "As built: cpp-indep", "The night's commands"): the packs
     re-recorded on clang 23.1.5 (`bin/repr-cpp host record`, `bin/cppi record-ir`, `--runtime`, `runtime-build --update`, the sampling's
     pack `record-ir --corpus gxx-dg-run --every 25`), `bin/oracle-records verify cpp ir runtime-ir --sample 8`, `bin/repr-cpp target
     oracle`, `runtime-build && runtime-ci` (`CPPI_JOBS=1`, CP246 (a)), `check`, `check --aot`, `pp-check`; then CP240 (b)'s routes
     compared (`CPPI_ROUTE=clang|auto`); this replaces step 0's `runtime-build --update` alone.
   - Rust after RES34 (doc/rust/RUST-EVAL-SURVEY.md, "As built: RES34 on rust-90's line", "Night commands"): `bin/rsi check --essential`
     (std's fragments at the merged keys), `RSI_AOT_LIB_CACHED=1 bin/rsi aot-lib` (the shared `.tmp/ci-aot-lib`), then the AOT cell
     `bin/rsi check --aot --corpus samples,synthetic --record` (about 4 h; RES66's three programs above 2 GB last), the UI corpus's
     every 40th judged (RES65).
   - Haskell HFS-I1 (doc/haskell/FRONTEND-SURVEY.md, "As built: HFS-I1 (a)"): after `bin/hsi dumps --record`, `bin/hsi check --front
     native ... --record` and `bin/hsi compare-front` (and `--aot`) against the same night's `check-hsi.edn`; no pass lost: the switch
     (hfs-i1's `.tmp/hfs-i1-switch.diff`, one commit).
   - Last: `bin/reap --record`, `bin/crops-score --record`.

Night B (the night after, keyed: what A did not reach):

1. The rest of `bin/gate --full` (the four crops' chains skip at their keys).
2. Records: rsi's UI corpus `bin/rsi check --corpus ui --record` (about 5.5 h at one builder thread), `bin/rsi divergences`, `own-verdicts`;
   `rsi-lower-check-all` (3,718, about 40 min, heavy); `bin/rsi runtime-build` (4 GiB) and `runtime-ci` (6 GiB) first where stale; C `bin/ci
   check --record`, `--aot --record`; LLVM `bin/llvmi check --aot --record` (33 min, 1.5 GB), `LLVMI_TX_ALL=1 bin/llvmi check` (16 min);
   OCaml's (`OI_TYPER_GOMEMLIMIT=1200MiB`, `O2J_JOBS=1`): `bin/oi stdlib`, `check --record`, `--aot --record`, `suite check --record`, `--aot
   --record`, `bin/repr-ocaml roundtrip --text --record-results all`, `--types samples stdlib`, then OC185's cells; Rust native-check beyond
   the 68 and the 1,400 error mutants (the oracle tier).
3. Last: `bin/reap --record`, `bin/crops-score --record`.

### Proposed (batch 2) (1 (a), 2 (c) and 3 (b) meanwhile decided by the user, 2026-11-07)

1. **The C++ runtime's IR pin after clang 23.1.5.** (a) Re-record `test/cpp/runtime/ir.sha256` (`bin/cppi runtime-build --update`, only
   `!ident` differs) and the clang pins with it, before the night; cost: C++'s and the lowering's records keep their numbers, the pin names
   23.1.5. (b) Pin clang back to 23.1.4 (an apk downgrade on the host). (c) Leave the steps failing. *Recommended: (a).* It affects batch 1's
   night too.
2. **The night's fit.** (a) Revert `rust-re6`'s merge commit for the night (rsi stays at RE5; the keys still move by ll6's list, so the
   rekey stays, but C's steps do not rerun twice); (b) run all of it into the slack, as batch 1's (about 12 h); (c) the essential gate,
   `bin/reap`, `crops-score` and the four new crops' chains tonight, the rest of `--full` the night after (keyed). *Recommended: (c).*
3. **LLS31, the 282 LLVM modules' pack** (their lists and corner sources are gone with removed worktrees): (a) rebuild the pack `rt` from
   what can be rebuilt (260 torture builds, the C++ samples' Mach-O builds, a Rust module, the stress set; four `hw-*` committed), drop the 21
   corner modules, re-make the mutants and records: a day's verb and a night's clang; (b) retire the four families from the read cells (read
   1 now, LLVM independence 94.2%). *Recommended: (a)*, (b) meanwhile if the score is wanted now. (doc/llvm/LLVM-CROP-SURVEY.md, "Proposed
   (batch 2)", with LLS32: rebase `llvm-ll4` onto batch 2.)
4. **OCaml's open items** (doc/ocaml/OCAML-SURVEY.md, Proposed): OC182 (expect natively, with T12), OC186 (ocamllex ported, a day; meanwhile
   `OI_OCAMLLEX=1`), OC187 (`ocaml-oracle.edn` spliced with `oracle-records-cpp2`'s planner), OC188 (compiler-libs' typer gaps in T12, classed
   tonight), OC189, OC190. *Recommended as the survey says.*
5. **Rust's independence (20%) and eval cell.** Rust's cells still read through rustc's helper (`tools/rsdump`) and rsi's eval cell has only
   the samples' record (5 programs; RE6's UI records come tonight). (a) Make the native reader (RF line) the read cells' default once its
   native-check covers the corpora, and record rsi's UI check into the eval cell; (b) leave. *Recommended: (a)*, a Rust agent's day after the
   night's records.
6. **C++'s independence (20%)** is unchanged: the codegen line (now merged with ll6) is not the cells' default. A C++ agent's task, batch 5.
7. **RES61, rsi-essential's memory** (doc/rust/RUST-EVAL-SURVEY.md, "Proposed (batch 2: RES61)"): 3.8 GB measured, the essential tier's
   cap 3 GiB, so the step runs in `--full` now. (a) Measure `RSI_BATCH=1` at night and return it at 3 GiB if it fits; (b) leave it in
   `--full`; (c) a smaller essential list. *Recommended: (a)*.
8. **(built, third round)** **The worktrees' Haskell inputs.** `bin/hsi` and `bin/repr-haskell` fetch their pinned tarballs and nofib when the checkout's `.tmp/`
   lacks them (found on batch 2). (a) Both read the main checkout's `.tmp/hsi` and `.tmp/repr-haskell/corpus` from a linked worktree, as
   Scheme's corpora do (GTS212); (b) leave it. *Recommended: (a)*.
9. **repr-haskell's configure in native mode** (found, third round): (a) invert the skip test so the native mode runs no configure
   script (the products from the pack, as its comment says), checked by a `corpus fetch` in a fresh `.tmp` with cc stubbed; (b) leave
   (the extraction's products unused by the native reads). *Recommended: (a)*, a Haskell agent's light task.
- Open from the third round's lines: CP244-CP246 (C++, CPP-SURVEY), RES61 (proposal 7), RES64-RES66 (res34's, renumbered: the glue as tables, the UI corpus's AOT record, the builders above 2 GB),
  HFS-I5-HFS-I7 (Haskell, FRONTEND-SURVEY), JCS162-JCS167 decided (F10a's).
- Still open from the lines: LLS31 (a) (the pack rt, planned), LLS32, CP242, OCaml's OC182, OC186-OC190; the branches `res34`, `hfs-i1`
  and `cpp-indep` merged in the third round (above); CP240 (b) and (c) after the night's routes compared.

## After the tag

- **The held branches, in batches into `next`** (`.tmp/tag-v2/merge-plan.md`): the crops' rows (batch 1, above), the LLVM IR line (the reader's gaps, `:full`, the verifier, the lowering of
  `-g` IR), the native readers with their error messages (OCaml: Menhir's tables generated from parser.mly; Haskell:
  Happy's from Parser.y, a clean-room traditional cpp; Rust: rustc's parser rule for rule; C++: CX11 with a minimal Sema
  and the tree check), the negative controls, Python's amd64 records night and PY11 (its
  line merged in batch 1), and the re-seed window W (the evaluator's speed, the embedding of C++, Scheme and OCaml's oi).
- **GT9** (the crops' gate, GTS210-GTS223 decided (a)), CS268, LLS1-LLS16.
- Python on the Mac VM: 202 done (`mac/201-python-py7-py11` 9540946b); queued 203-206 (the user, 2026-11-05, in order):
  202's lows, `_csv` and `mmap` as Go ports (PYS22), pinned wheels and the installer (PYS23-PYS25), `bin/pyi c2j-check`.
- **The day's deep-thinking work (the user, 2026-11-05; agents on branches, sample checks only, held for after the tag):**
  1. OCaml's typer and Translcore ported (OC4 (b)'s route): oi without the host's `ocamlc` (`ocaml-typer`; started).
  2. C++ without clang: Sema's rest and a code generator into the LLVM IR forms, the design and a first slice
     (`cpp-codegen`; started).
  3. The crops' oracles recorded as data, so no gate needs a host toolchain (GHC, rustc, clang, ocamlc, Chez, python3): a
     design with proposals (after the keyed `--full`).
  4. Java eval's 1,220 deferred cases triaged by root cause, ranked by tests per day, the top fixes planned
     (`java-deferred-triage`; started).
  5. Haskell's hsi from HE4d: the boot libraries and the C they call (HE5-HE7).
  6. Rust's eval from RE4 (RE5 on).
  7. Go eval's 173 by-design cases reviewed for what is reachable (`gc-flags`' 98: gc's `-m` diagnostics).
  8. GT9 built on a branch (GTS210-GTS223 (a)): the crops' gate steps and keys, checked by `--plan` and gate-tests
     (`gt9-build`; started).
  9. The merge plan's batches 0-5 rehearsed against `next`: conflicts, key changes, order, nights.
- **The second round (the user, 2026-11-05):**
  1. The crops' oracles recorded as data, no gate needing a host toolchain: the design (`oracle-records`; started).
  2. OCaml's typer T7 (Simplif) and T8 (the module system, the stdlib typed natively) (`ocaml-typer2`; started).
  3. C++'s codegen G2: exceptions, lambdas, multiple and virtual inheritance, bit-fields, unions, member pointers, SysV
     passing (`cpp-codegen2`; started).
  4. Haskell without GHC: a survey of the smallest route from the native reader to STG (HES21 (b)).
  5. Java JC9 F1, the JCA: closures, overlays, edge and judging areas prepared for the night's conversion
     (`java-jc9-jca`; started).
  6. goi's liveness: dead slots and `uintptrescapes`.
  7. Scheme's maturity review: R7RS-small's and the corpora's SRFIs' gaps ranked.
  8. GT9's missing verbs: `--only-list`, `--cmi-only`, `--no-expect`, `test --samples`, `pins check`, C++ and OCaml subsets
     (`gt9-verbs`; started).
  9. The re-seed window W prepared: eval-speed rebased, pyi's embedding planned, the two nights scripted (`window-w`;
     started).
  10. LLVM IR's LLS1-LLS19: a decision sheet with fresh recommendations.
- **For the bottom crops (the user, 2026-11-05; C++, Rust, Haskell, OCaml, LLVM IR):**
  1. Rust without rustc: a survey of the route from the native reader to rsi's IR (`rust-frontend-survey`; started).
  2. RES34: std translated once per key by cf2g, designed and proved on one crate (`rust-res34`; started).
  3. Rust's REAP row: read and print cells from the round trips (`rust-row`; started, with 7 and 9).
  4. C++'s eval and AOT cells: `bin/cppi check` over g++'s run corpus, its classes and record (`cppi-check`; started).
  5. C++ print's 914 by-design cases reviewed.
  6. Haskell HE8: AOT, STG to Go forms, designed with a first slice (`haskell-he8`; started).
  7. Haskell's REAP row (`haskell-row`; started).
  8. OCaml's deferred cases triaged (190 at eval, 219 at AOT).
  9. LLVM IR's REAP row (`llvm-row`; started).
- **The bottom crops, third round (the user, 2026-11-05):**
  1. OCaml OT9 fixes 6-8: sockets, processes and signals (OT18), compiler-libs (`ocaml-libs3`; started).
  2. OCaml T11, the run corpus and the REPL on the native path (after T10, running on `ocaml-t10`).
  3. C++ CPR1: the print cell reads the oracle's rejections natively, with cpp-native2 merged; CPR4 at its re-record (`cpp-cpr1`;
     started).
  4. C++ G4, the library headers (`cpp-g4`; running).
  5. Haskell HF5/HF6: the match compiler, CorePrep, CoreToStg and Unarise ported; programs run on hsi from our STG
     (`haskell-hf56`; started).
  6. LLVM LLS21: the ci builtins and the function pointer's return width (`ci-lls21`; started).
  7. Rust: a night script for rsi's records (runtime-ci, aot-lib, check and check --aot, record-ref).

## Open

- JCS127 (`mac/039-jcs127` `d6870d37`, volatile fields under Rt's striped locks): not in main; after a test on the host
  (it changes the runtime).
- GTS137 (a): the `--full` step `repr-java-jdk` (`:heavy true`), at the next reader change (GTS133 (b)).
- `arbace/c/repl.ae`'s back-end default and the judge tests that stand in for the compiled parser (night-fix): the
  source's fix waits for the next re-seed.
- The box can no longer build the program: `go/arbace/crt` calls `syscall.Access` and `syscall.Stat_t`, which TamaGo's
  syscall lacks (C9.2's hazard (e)); for the box's revival.
- Waiting for the user: the clean run's fit and the rotation's state in its record (after night 4's numbers); each
  survey's "Proposed" sections not marked "Decided" are the rest. Parked by the user: Zed (doc/ZED-SURVEY.md on
  `zed-survey`).
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
