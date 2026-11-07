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
   world (CDR4) name classes `jdk/forms` does not hold yet ("not mergeable before its night", JAVA-DEFERRED-TRIAGE). One
   conversion takes every area: `REPR_JAVA_EXE=<the newest .tmp/gate-exe/javac-*/arbace-javac> REPR_JAVA_STALE_OK=1
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
