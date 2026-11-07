# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-reaps-4-crops-v1`, by the user's decision (2026-11-07). It
started over the same way at the two freezes and after the milestones `arbace-inside-itself-v1`,
`arbace-interprets-go1.27.1-v1`, `arbace-reaps-3-crops-v1` and `arbace-reaps-3-crops-v2`. The history before it is on the
frozen branches, at five tags, and in git history:

- 2026-10-06 to 2026-10-08 (the class forms, j2c, the vendored and self-hosting Clojure, the work
  for Java 26, the g2c survey): on
  [`arbace-for-java-26`](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
  tag `arbace-for-java-26-v1`;
- 2026-10-08 to 2026-10-11 (milestone B1a: g2c, c2g, jrt, the evaluator, the Go executable and
  its feature completion, the freeze of the Go build): on
  [`arbace-for-golang`](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/JOURNAL.md),
  tag `arbace-for-go1.27.1-v1`;
- 2026-10-11 to 2026-10-15 (the `.ae` sources, c2a, B1b the box and its milestones
  `arbace-box-amd64-v1`, `arbace-box-arm64-v1`; the standalone Arbace S1-S8 and the milestones
  `arbace-seed-is-go-v1`, `arbace-inside-itself-v1`): at the tag
  [`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
  (main's `e0ad476`);
- 2026-10-15 to 2026-10-20 (S9, Go's toolchain as forms; the docs' audit; the build's speed; the
  renames to repr/java, repr/go, repr/clojure, c2j and j2g; Futamura's F2-F7; repr/java print;
  repr/haskell R1-R6 and HR25; repr/rust RR1-RR6; goi GR0-GR10, the arm64 box check, GC81, and
  the milestone `arbace-interprets-go1.27.1-v1`): at the tag
  [`arbace-interprets-go1.27.1-v1`](https://github.com/arbace/arbace/blob/arbace-interprets-go1.27.1-v1/doc/JOURNAL.md)
  (main's `965cbed3`).
- 2026-10-20 to 2026-10-27 (the Go parser GP1-GP8 and its surveys; the Java front end's native parser,
  converter and attribution JN8-JN9d; Java's conformance check JC1-JC6b; goi after v1; the REAP survey and
  REAP1-REAP9, the three crops' matrix; three batched re-seeds; the parked sub-projects; the milestone
  `arbace-reaps-3-crops-v1`): at the tag
  [`arbace-reaps-3-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v1/doc/JOURNAL.md)
  (main's `83538e0b`).
- 2026-10-27 to 2026-11-01 (the scripts' sweep; the efficient gate GT1-GT7 and its clean run; REAP's small and medium
  leftovers: JC4b, JC4c, Clojure's deferred cases, JN9e, JN9f and JFS133, RPS150, RPS97 and RPS161; bin/java-check's
  shadowed judge and the Java levels re-recorded; two re-seeds; C23's survey and C1-C3; the net/http survey, NH0 and
  NH1; the OCaml, Scheme and C++23 surveys; the milestone `arbace-reaps-3-crops-v2`): at the tag
  [`arbace-reaps-3-crops-v2`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v2/doc/JOURNAL.md)
  (main's `c4f85ace`).
- 2026-11-01 to 2026-11-07 (the merge window after v2; C8 and C9, C the fourth row, and its re-seed window; the test
  budget, GT8's compressed corpora and tiers, the staging branch `next`; the crops unparked and surveyed, OCaml, Scheme,
  C++, LLVM IR, Haskell, Rust and Python on branches; the re-seed `d5548e72`; the clean run of the four crops and its two
  reruns; the milestone `arbace-reaps-4-crops-v1`): at the tag
  [`arbace-reaps-4-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-4-crops-v1/doc/JOURNAL.md)
  (main's `a4a84533`).

## 2026-11-07: the journal starts over

- **The user's decision** of 2026-11-07 ("tag it when the gate passes, then reset the journal"): the milestone
  `arbace-reaps-4-crops-v1`, an annotated tag as v2's, on main's `a4a84533` (the record's commit `99873f98` with the tag's
  docs and the record above it, the essential gate passed on it), made by the session at the user's go-ahead; this
  journal starts over after it.
  - The earlier one, 474 lines and 18 entries from 2026-11-01 to the tag's entry, is at the tag (linked above).
  - CLAUDE.md's Records paragraph, the agenda's header and README name the seven places of the history.
- **Where things stand** (doc/AGENDA.md): the REAP matrix at the tag, 16 of 16 cells checked, none failing, every
  non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v3's divergences"); the clean record
  `test/gate/clean-arbace-reaps-4-crops-v1.edn` (234 steps, three runs, 77,700 s); the seed `d5548e72`.
- **Next** (doc/AGENDA.md, "After the tag"): the after-tag fixes (`.tmp/tag-v2/ppcheck-srcdir.patch`: `embed-11.c`'s
  digest writes `${srcdir}` and `absolute-name` goes; crops-score's CSP5 goi patch), a whole `--full` night to complete
  the rotation (nine parts incomplete at the tag), then the held branches in batches into `next`
  (`.tmp/tag-v2/merge-plan.md`; the `arbace/cpp/lower.ae` conflict between `llvm-ll6` and `cppi-deferred`), each batch's
  night records, and Python's branch from the Mac VM.

## 2026-11-06: the day's work, held for after the tag (recorded after the tag)

Not journaled before the tag (the night's clean rerun held the session). The user, 2026-11-06 07:22: spend the weekly
allowance on the crops' completeness, maturity and independence. Fifteen Opus agents on branches, sample checks only,
under the day's rules (light on the host; main frozen until the record), all pushed to `mac` and held for after the tag;
then further agents as the first finished. Each branch's doc records its work, its proposals and the user's decisions;
the night commands each needs are in its doc.

- **OCaml** (`ocaml-t12`): the native path 244 to 273 of 309, equal to ocamlc's; TY23 (a) built, TY24 (a) prepared
  (native the default after the night's record). `ocaml-libs4`: eval 428 / 125 / 118 to 441 / 205 / 25, AOT 394 / 114 /
  159 to 405 / 202 / 64; OT27-OT29 decided.
- **Haskell** (`haskell-hf7`): programs run from our STG 16 to 60 of 63. `haskell-eval9`: testsuite core 600 to 666 of
  701, check-hsi 806 of 910, AOT 209 programs; HES80-HES82 decided. `haskell-simpl`: GHC's -O0 simplifier ported, the
  default; Core 37 to 53 of 61.
- **Rust** (`rust-rf14`): RF7's collection 1,189 of 1,191 items, RF9a's type checking; `rust-rf9b`: typeck 99.7% on the
  samples, 99.6% on the crates; RFP14-RFP21 decided. `rust-re6`: threads on ci as goroutines over musl's futexes,
  posix_spawn, signals, LTO; samples 69 of 69, UI sample 90 of 93; RES46-RES53 proposed.
- **C++** (`cpp-g5`): P15-P16, new/delete, initializer lists, iostream: the corpus sample 7 to 32 of 42.
  `cppi-deferred`: long double layout, float atomics, va_arg on ci. `oracle-records-cpp2`: the five packs re-recorded
  deterministically, ORS28 (b). P17-P19, CP230, CP231 decided.
- **LLVM IR** (`llvm-ll6`): eval 229 to 519 of 562 modules, AOT recorded, no LLVM tool at check time; LLS23-LLS26 decided
  (LLS26: the torture IR pack is GPL test data, its text in LICENSE.md).
- **Scheme** (`scheme-smr7`): psyntax boots on si (no Chez), Chez's format floats ported; mats 436 to 560 pass.
- **Java** (`java-jc9c`): F6, F12, F13 (java.lang.management), F19 write-ahead; JCS158-JCS161 decided.
- **Go** (`goi-escape`): gc's escape analysis ported for `-m`: 58 of 96 such tests, goi check 1,664 to 1,722 of 1,837
  (rows hand-written until the night's `--record`).
- **C** (`c-deferred`): VLA structs, complex integers, signals, self-include: eval and AOT deferred 33 to 6, print 78 to
  45; CS274-CS278 decided (CS278 after the tag).
- **Clojure** (`clojure-cdr`): CDR1-CDR11 built or started; eval 21,053 / 81 / 1 to an expected 21,110 / 18 / 1-2 after
  the night's conversion and build.
- **The crops' score** (`crops-score`): `bin/crops-score` (`doc/CROPS-SCORE.md`): completeness, maturity and
  independence per crop; CSP1-CSP5 decided and built.
- **Incidents:** `goi-escape`'s hard-linked corpus summaries rewrote main's scratch summaries
  (`.tmp/repr-go/forms/linux-{test,dirtest,errtest}`, `.tmp/godump/corpus/linux-errtest`; only `:seconds` expected to
  differ; the clean rerun used its own clone, unaffected); two rule slips
  (memory peaks of 5.6 and 6.4 GB in a day's check; one `<(...)`).

## 2026-11-07: merge plan batch 1 on `batch1` (the rows, the keys commit, GT9, Python, the small ones)

Batch 1 of the held branches' merge plan (`.tmp/tag-v2/merge-plan-rehearsal-1105.md` §4 step 1 and §2, `merge-plan.md` §3,
§4), merged by a day agent on the branch `batch1` from `e16e4c1d` (main = next, the tag plus the journal's reset), for the
night's gate; `next` takes it when the night's first step passes. One merge commit a branch, each naming its conflicts and
their resolutions; the analysis `.tmp/tag-v2/merge-check.sh` rerun on the day's heads first (18 steps, 12 conflicting).

- **The rows.** `cpp-cx8` (6d16c65a), `cpp-cx9` (b2117cc6), `cpp-cx10` (5889aa52), `scheme-sm9` (4ff31506), `ocaml-o8`
  (de10d3d1), each followed by its GT9 verbs branch (`gt9v-cpp` 0d5cae5f, `gt9v-scheme` 6ba75ecc, `gt9v-ocaml` 58b91bc1: the
  `--only-list`, `--cmi-only`, `--no-expect`, `pins check` verbs and the subsets the fragments rotate by). As GT9 prescribes
  (doc/GATE-SURVEY.md, "As built: GT9"), the rows' `bin/lib/gate-run.bash` hunks (their `gate_*_id` functions), the gate
  tests' host fixtures and their `steps.edn` hunks but `reap`'s were taken as next's: their steps came back from the fragments
  `test/gate/crops/{cpp,scheme,ocaml}.edn` spliced by `gate.crops splice`, their hosts from `:host-ids`. `test/reap/cells.edn`
  was rebuilt by hand at Python's merge: o8's merge had put a second header map in place of the readers' comment (a resolution
  error of this day, repaired there). `test/cpp/runtime/ir.sha256` is CX8's lines (cppi-check's finding: its relative shim
  path makes CX10's rewrite a no-op), so it needed no runtime build.
- **The small ones**, clean: `e6f88eb3` (embed-crops 1-2: `cpp-c2j`, `scheme-c2j`, the reflection hints; the steps moved to
  the `:cpp` and `:scheme` crops, CP190 (a), with waived controls), `gate-crops` (GT9 decided), `zed-survey` (parked),
  `java-deferred-triage` (JCS128-JCS135), `go-design-review` (goi's records: Go eval 1,664 to 1,673; after the tag, as planned).
- **The keys commit** came with `gt9-build` (11579f82, its `100f8e0b`: PYS30 (a), `bin/lib/version.bash`'s `arbace_crop_paths`
  for the six keys, the audit's `crop-rules`), then `gt9-verbs` (67d84a29: GTS246, GTS247). `bin/lib/crops/` (e6f88eb3's c2j
  script) joined the list instead of its own exclusion. Keys (12 hex): main gate `0276a6db9960`, runtime `f29a38759450`, javac
  `a3300f566f6c`; after the keys commit `98ffc164199f`, `c40e01ac561c`, `8c9a6a206dd7`; at the end `d42c7a5dd92d`,
  `c40e01ac561c`, `ab86bd52d01b` (gate and javac moved twice more: pyi's lazy modules added to the list, `bin/goi`'s CSP5
  patch; the runtime key not).
- **The controls.** The fragments name the round trips' controls of `crop-controls`, a batch 5 branch: its three commits for
  C++ and LLVM (01c732aa), OCaml (0807bbbc) and Scheme (defbea23) were picked, not merged (Haskell's and Rust's stay for its
  merge), `arbace/repr/scheme.ae` resolved by hand (sm9's records and gt9v-scheme's `--only-list` with the controls' helpers).
  `gate.crops check`: 0 problems.
- **Python** (`mac/201-python-py7-py11` 7df8864b, PY6-PY10 and queue 202-223, 526 commits) merged whole but for the parts the
  plan puts later: W2's embedding (101c0d03, a420fa0d, 0525fa89; PYS18 (a): pyi embeds at the re-seed after Python's tag) and
  the REQUESTs into code of other owners (goi's 656dfcdc and c9047d52, crt's 3db255c5, the planner's cc463c8d: GTS243 (a)
  batches it with GTS240), taken as next's side; doc/AGENDA.md lists them for their cherry-picks. pyi's lazy modules joined
  `arbace_crop_paths`. The alternative, merging the embedding now, was rejected: it would rekey the executables for a re-seed
  that waits, with `bin/pyi c2j-check` (its precondition) not built.
- **crops-score** (0def98ca: the essential step, CSP1-CSP5) with its after-tag patch `test/crops/after-tag/csp5-goi-goroot.patch`
  applied after goi's merge, and **cppi-check** (5c31582f, 2026-11-05, not in the plan): without it the C++ row had 180 problems
  in `bin/reap` (cx10's cells predate cx8's and cx9's records); it is a clean descendant of the rows. Its 4 GiB
  `cpp-aot-essential` was left out (GTS215's 3 GiB). `mac/192`'s redo was dropped: GT9's fragments supersede it (SC233's and
  OC161's readings as an `:id-re`, GTS213 (a)).
- **Checks by day** (stage 0 by `ARBACE_HOST_EXE`, light): `bin/ci judge-test` 104 tests, 0 failures; `test/gate/crops-test`,
  `gate-key-crops-test`, `rotation-test` (65 of 65), `costs-test`, `lanes-test` (21, 0 failed), `audited-test` (19 of 19),
  `claude-md-test`, `make check-makefile`; the scheme namespace loads; `bin/gate --plan` and `--full --plan`; `bin/reap --record`:
  eight rows, 32 cells, 30 checked, 0 problems; `bin/crops-score --record`, `--judge --strict`: 0 problems. Three gate tests
  expected next's ungated table and were brought to the gated one (crops-test's pins, chains and splice cases; rotation-test's
  one C pattern; gate-key-crops-test's `bin/lib/crops/`).
- **The scores** (completeness / maturity / independence, main then batch1): C++ 0 / 0 / 20 to 75.4 / 90.2 / 20, OCaml 0 / 0 / 20
  to 75.6 / 100 / 20, Scheme 0 / 0 / 80 to 98.3 / 87.5 / 80, Python 0 / 0 / 20 to 0 / 83.3 / 20 (its eval and AOT wait for
  its records), C maturity 95.8 to 100, Java 98.3 to 100, Go 86.0 to 87.5.
- **Docs**: CLAUDE.md (bin/reap's eight rows, the other crops' paragraph with Python, the one list, the gated crops), the
  agenda's batch 1 section and its proposals (tonight's fit, Python's deferred parts, `cpp-aot-essential`, cppi-check outside
  the plan, the day branches that are candidates for a batch 1b).
