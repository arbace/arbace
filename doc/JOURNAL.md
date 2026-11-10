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

## 2026-11-07: batch 1, the seven day branches (addendum)

The user decided (2026-11-07, relayed by the main session) on batch 1's proposals: the night runs the whole keyed `bin/gate
--full` (a nominal 12-hour shift, overflow to 12:00); Python's deferred parts go to the re-seed window W, the planner's with
GTS240; `cpp-aot-essential` stays out of the essential tier and is measured tonight at `CPPI_THREADS=1`; and all seven
2026-11-06 day branches go into batch 1, so one rekey night carries them. On the main session's merge of `go-controls`
(`f7696201`), merged in this order, one commit each: `cppi-deferred` 948a17e7, `ocaml-libs4` 628555d4, `scheme-smr7` 9b672f2f,
`java-jc9c` e92a1c8e, `clojure-cdr` 67adb0ff, `goi-escape` 91d3eb40, `c-deferred` 61406468. Their conflicts were small (the
Makefile, `steps.edn`'s Scheme steps, `bin/goi`'s flag cases, goi's record merged row by row); their branch journal entries
are the main journal's 2026-11-06 entry. Keys at the end: gate `1f886836e11e`, runtime `4cd0885457fb`, javac `c97e87dc55a5`
(ocaml-libs4, java-jc9c, clojure-cdr, goi-escape and c-deferred move them; doc/AGENDA.md names which). `bin/reap` keeps one
problem until the night's C++ record (cppi-deferred's `runtime/deferred.cpp`, its reference recorded, its run not); the matrix
was recorded with that row held out for the run. Found: `java-jc9c` names JC9's other branches (F1, F5, JC9b) as its
prerequisites, which are on no merged line; the night needs `bin/jrt-convert update` before the gate for it and for
`clojure-cdr`. Proposed in doc/AGENDA.md (6): revert `java-jc9c`'s merge today and give the JC9 line its own night.

## 2026-11-07: batch 1, JC9's prerequisites, JCV1, SCS1 and the mixed stage (addendum)

The user decided (2026-11-07, second round): proposal 6 (b), so `java-jc9-jca` (F1) a4a8e546, `java-jc9-logging` (F5)
b8a1951c, `java-jc9b` 51b320c1 and `java-jc9b-natives` f89e8e9c were merged after `java-jc9c` (the docs' tree: F1, F5, F4, F2,
F3 and JC9b together, converted in one night); `java-judge` 3ab550e0 (JCV1: each test's own verdict, the JDK's records the
oracle tier) and `scheme-90` 43b39f4a (SCS1: Scheme's controls for every family and its own-verdict judge); crops-score's mixed
stage scored as the case-weighted mean (built in `crops.score`, from the facts' case counts); JCV2 to build, left to an agent
of its own (the golden files need a review against the JLS); SCS2 (a) with `oracle-records-cpp2`. Conflicts: the triage doc
section by section, `DriverManager.java` (both added; clojure-cdr's kept, F16's java.sql files out of its list), the closure's
lists, jrt's file lists, `bin/java-check`'s check_one (F6's tool branch with JCV1's verdict lookup), `arbace/repr/scheme.ae`
(scheme-90's controls in place of crop-controls' picked ones, gt9v-scheme's verb applied again). Java's records were rederived by
`bin/java-check rejudge-records` and its t05507 edit kept; `si-corpora` lost `:chibi` as scheme-90 declares. Keys at the end:
gate `2f25331ada6f`, runtime `b74523271f12`, javac `f221ad0dfe97` (JC9's four move them). Scores: Java 72.9 / 100 / 99.7,
Scheme 98.4 / 100 / 95.0. The merge commits a revert would take are in doc/AGENDA.md.

## 2026-11-07: C's own target and the judge by each test's own verdict, ported onto batch 1 (branch c-indep-b1)

Branch `c-indep-b1` from `mac/batch1` 1dd5bcc71 (the day's merge batch), with C's independence work of branch `c-indep`
(`mac/c-indep` b84f1a4f1, on `mac/oracle-records-cpp2`) ported onto it, the main session's request: `c-indep` carried
oracle-records-cpp2's 166 commits beyond the batch. The goal as before: C's independence above 90% without lowering its
completeness or maturity. On `mac/batch1` C scores 20.0% (every C step on `:gcc`); on `c-indep-b1` 99.7%.

- **Ported**: `c/` (Arbace's headers and target description), `test/c/target.ae`, `test/c/selfref.ae` and its exceptions,
  `bin/lib/ci/selfref.bash`, the two `:run` controls, `ARBACE_C_HOST` in `arbace.repr.c.host/load-host`, the judge switch
  (`gcc-judge?`; the token check's gcc level, its `:gcc` known entries and the round trip's acceptance), musl's derived
  configure products, crops-score's mixed judge (the user's case-weighted mean, not on batch1 yet).
- **Carried from oracle-records-cpp2, the minimal part**: the sysroot (`ARBACE_C_SYSROOT`: `arbace.repr.c.pp`'s
  `sysroot-file`, ORS9 (a)'s `dbc9a76c2`; `tools/ccdump/sysroot.go` and its two config lines, `c60cb208c`) and the round
  trip's no-gcc judgment with its deferral of the classes only levels 3-4 give (ORS16's stand-in), now its default.
  `bin/lib/repr-c/sysroot.bash` is written anew: Arbace's target, or `ARBACE_C_SYSROOT=/` (the host's files) for gcc's.
- **Left on oracle-records-cpp2**: the packs (`c-headers-gcc15`, `c-musl-1.2.6-x86_64`, `c-roundtrip-orig-*`), the dg record
  `test/c/ref-dg-gcc15.edn` and `test/c/dgref.ae`, C23's `ref-syntax-gcc15.tsv` and `c23records.ae`, `bin/oracle-records` and
  `test/oracle-records/`, the planner's `:phase :oracle`. None is needed: on batch1 the oracle tier is gcc run live by hand
  (`ARBACE_C_TARGET=gcc ARBACE_C_JUDGE=gcc`), as before the records; gmp.h and pcre2.h come from the host's packages by
  `test/c/host/headers.sha256`'s digests.
- **New on batch1**: no gcc call at all in the default paths: `bin/lib/ci/dg` skips its gcc runs, `bin/lib/repr-c/check`
  asks gcc's include directory only for `--record`, `bin/repr-c` only for `host record|check`, `bin/lib/repr-c/c23check` judges
  C23's files by their own validity (`test/c/c23/expected.edn`'s judge-1 entries the reviewed rejections) with the objects
  and gcc's acceptance the oracle tier's (`arbace.repr.c.c23check`: an object not compiled is not judged); every C step's
  `:hosts` loses `:gcc`.
- **Rekeys** (embedded, gate_key): `arbace/repr/c/host.ae`, `pp.ae`, `ppcheck.ae`, `rtcheck.ae`, `c23check.ae`; stage 0
  evaluates them from the checkout (repr namespaces are tool sources).

## 2026-11-07: batch 2, LLVM IR, rsi, OCaml's and Rust's native lines (branch batch2, on batch1 0edacad6)

Merge plan batches 2 and 3 on one day (`.tmp/tag-v2/merge-plan-rehearsal-1105.md` §4 step 3), after `merge-check.sh --base mac/batch1`
on the day's heads (25 steps, 17 conflicting), each line at its newest head: `rust-re5` 495e5265b's ancestor e573fd86b, `gt9v-rust`
73b2848b, `rust-re6` 495e5265, `llvm-read` 5249bc5c, `cpp-lower-dbg` 1eb112ab, `llvm-full` f2a4dafe, `llvm-verify3` 93108c42,
`gt9v-llvm` 058168d9, `llvm-ll6` 153ac9e9 (with cpp-native, cpp-native2, cpp-sema, cpp-codegen, cpp-codegen2, cpp-g2fix, cpp-templates
and llvm-row inside), `llvm-90` b8aa3f16; `ocaml-native` 8f00636e, `ocaml-errors` 0e25bd25, `ocaml-errors2` b7fa3dd7, `crop-controls-ocaml`
52548f59, then `ocaml-t12` 394c7a2f and `ocaml-90` c0fdfca4 merged by an agent on `batch2-ocaml` (6742fb54) with ocaml-libs4's libraries,
OC180, OC181, OC183 and OC184 built there; `rust-native` 2038e0fd, `rust-unicode` d6efaadb, `rust-errors` 7efd8917, `rust-errors2` 668e0259,
`crop-controls-rust` 63f53b42, `rust-rf9b` 523640cd, `rust-rf3b` 2c4e8faa, `rust-row` e33985da, and crop-controls' 7bf1d1e4 picked. One
merge commit a branch.

- **Why the lowering took an agent.** `arbace/cpp/lower.ae` had two implementations of vectors and of ci's va_list: batch 1's (CX8,
  rust-re4/re5, cppi-deferred, cpp-lower-dbg) and ll6's (LL5, LL6). The ll6 merge took ll6's file, and `batch2-lower` (f703fceb, 3550feb7)
  integrated the rest: LL5's vectors and one bitcast path win, rustc's intrinsics and the slot unions kept, the two va_list schemes kept
  apart by `*ci*`; checked on the C++ lowering's samples, llvmi's features and rsi's native leg. Alternatives: ours' vectors (rsi's) over
  LL5's (llvmi's 522-of-562 record was made with LL5's), or holding llvm-ll6 for its own day (the LLVM row waits).
- **The keys.** rust-re6 changes C's embedded runtime (`go/arbace/{ci,crt}`, `arbace/c/build.ae`, `tools/cirun`: threads on ci) and moves
  all three keys; llvm-ll6's `arbace/llvm/` had to join `arbace_crop_paths` and embed.ae's exclusions (the keys commit missed it), which
  moves gate_key and the javac key. Batch 2's night is a rekey night again (AGENDA, "Proposed (batch 2)", 2).
- **LLS27-LLS30** (the user's decisions of 2026-11-07): the rule for a pinned input corpus in the crop's own language in doc/CROPS-SCORE.md;
  llvmi switched to C's own target (`c/target`) so its eval and AOT stages are Arbace's alone; LLVM IR's independence 68.3% at llvm-90,
  88.3% on batch 2. LLS28 could not be prepared as decided: the 282 modules' lists and corner sources were in the removed worktrees of
  llvm-read and llvm-full; LLS31 proposes rebuilding a pack from what can be rebuilt.
- **Scores** (batch1 → batch2): LLVM IR 0 / 4.2 / 40 → 95.9 / 93.3 / 88.3; Rust 0 / 8.3 / 20 → 75.0 / 95.8 / 20; OCaml 80.9 / 100 / 20 →
  80.9 / 100 / 87.4; the others unchanged. `bin/reap`: ten rows, 40 cells, 37 checked, 1 problem (deferred.cpp's, tonight's record).
- **Found:** `runtime/thread-throw.cpp` on ci terminates in `cx$thread_start` since re6's threads (C++'s class `threads` extended, the
  record tonight's); the host's clang moved to 23.1.5, so `test/cpp/runtime/ir.sha256` no longer matches (the `!ident` line).
- Not merged: `llvm-ll4` (LLS32), `rust-res34` (its own day: C's `arbace/c/`), `oracle-records-ocaml`/`-rust` (ORS12), `ocaml-libs1` (a doc
  commit already in libs4's doc).

## 2026-11-07: Rust's independence and maturity (branch rust-90)

Branch `rust-90` from `mac/rust-rf9b` (`523640cd8`), `mac/rust-row` (`e33985da`) and `mac/rust-re6` (`495e5265`) merged in (clean), by an
agent by day in the light tier, toward the brief's goal of Rust above 90% on `bin/crops-score`'s three measures (scored from
`mac/batch1`'s tool, `0edacad62`). Order: independence first, then maturity, then completeness, by what each day's work moves. Built
(doc/rust/RUST-EVAL-SURVEY.md and FRONTEND-SURVEY.md, "As built: rust-90"; EVAL.md §13; SPEC §11.3; NATIVE-READER.md):
- the native reader as the default of `bin/repr-rust dump`, `read` and `roundtrip`; the round trip's tree level from the native trees
  (`arbace.repr.rust.native tree`: `rsdump tree`'s texts written from the native tree, ported from Arbace's own helper's transcoder,
  `tools/rsdump/rsdump.rs`'s `Mode::Tree`/`Mode::Lines`; 68 of 68 files byte for byte the helper's, `native-check --trees`); rustc only
  for `--reader helper` and the rustc levels, which move to the oracle tier (the read cell's rustdoc level `:role :beside`);
- the round trips' negative controls (`test/rust/roundtrip-controls.edn`, seven, run last by every round trip);
- rsi's programs' IR from pinned packs (`bin/rsi ir-pack`, `test/rust/ir/`: samples and synthetic committed, the UI corpus's in `.tmp`,
  its every-40th pinned), deterministic (the shim's ModuleID without the build directory); the ci leg's library keyed without the C
  toolchains (`lowered_key`, `runtime-build --ci`, repr/c's reader alone in the keys; rust-re6's runtime copied and re-keyed after its old
  key was verified over rust-re6's tree but the clang --version line); `RSI_BATCH` (RES53 (a));
- the UI programs judged by their own verdicts, compiletest's run rule ported from rustc 1.99.1's `src/tools/compiletest/src/runtest/ui.rs`
  and `directives.rs` (the pinned `rustc-1.99.1-src.tar.xz`): 3,409 of 3,718; rustc's recorded runs meet 277 of the 281 they cover;
- Rust's gate steps in `test/gate/steps.edn` (three essential, three full, on `:stage0` and `:go`), validated by the planner.
Stub runs: rustc, rustdoc, cargo, cc, gcc, g++, c++, clang, clang++, clang-23, llvm-as, opt, lli, llc, ld and ar first on `PATH` exiting
99 for the round trips (std's 8 files, 82 UI files), the controls, `bin/rsi judge-test`, `runtime-ci`, musl's reread and `bin/rsi check
--essential`: the log empty after each. Scores at `rust-90` (completeness / maturity / independence): 75.0 / 66.7 / 20.0 before,
75.0 / 95.8 / 80.0 after (`bin/rsi check --essential` 13 of 13 from the packs; the builder peaked at 3.8 GB, over the day's line). Not reached: completeness (the AOT cell needs RES34: cf2g's translation of a program with its linked std, up to 7 GiB) and
independence above 80% (eval and AOT consume std's rustc-made IR until Tier S, RF12). Alternatives considered: committing the UI
pack (18 MB; RES56), the AOT cell on the samples alone at 7 GiB (RES55 (b)), counting std's pinned IR as the crop's own input (RES54
(b): against the user's rule). Proposed RES54-RES60.

## 2026-11-07: Haskell's independence and maturity (branch `haskell-90`, from `haskell-simpl` 2646694d5)

The brief: the Haskell crop above 90% on `bin/crops-score`'s three measures (doc/CROPS-SCORE.md on `mac/crops-score`), under
the user's decisions of 2026-11-07 (a test's own verdict a judge no toolchain made; mixed stages case-weighted; toolchain runs
in the oracle tier). What was done, in doc/haskell/FRONTEND-SURVEY.md, "As built: the crop's independence":

- Taken in: `mac/oracle-records-cpp2`'s oracle-records tooling and Haskell's ORS12 commits (cherry-picked with `-x`:
  `c3892e875`, `8e5ce6196`, `1bf41517c`, `54f5e5313`, `62e96e5d9`, `2b03a6bdd`; `test/oracle-records/oracle.bash` taken whole
  from that branch's head); `mac/haskell-cpp`'s net change over `53794ed6^` (unlit, the driver, the clean-room C preprocessor,
  mode `:full` natively, the helper's version 5) applied as one diff: its tree has no GPL code and no commit containing
  `53794ed6` is in this history. `mac/haskell-eval9` not merged (eleven conflicted files in hsi's runtime: both lines built
  HES60 and HES64; HFS-I4). Considered: merging all of `mac/oracle-records-cpp2` (rejected: it carries every crop's ORS work).
- Read: the native reader is the round trip's and `read --corpus`'s default on both sides; what the reads still consume of
  the host (GHC's installation data and headers, the configure products, hsc2hs's outputs) is the pinned pack
  `haskell-native-inputs-ghc-9.14.1` (`bin/repr-haskell record-native-inputs`, by hand), read under a sysroot by `cpp.ae`.
- Judges by their own verdict: the tree and layout levels compare the reread forms (`compare-forms`), the text level the
  bytes (`compare-text --bytes-only`); hsi's programs with shipped expectations equal to GHC's recorded run (580 of 908,
  `test/haskell/own-verdicts.tsv`, `bin/hsi own-verdicts`) by those files. Negative controls for the tree, text and
  interface levels (`test/haskell/roundtrip-controls.edn`).
- hsi without GHC: the programs' STG and foreign stubs re-recorded into `haskell-stg-progs` (908 programs); the RTS headers
  from the native-inputs pack; `pins --fetch` and `corpus fetch` no longer probe GHC. `haskell-stg-libs` is stale at this
  helper: the night's `bin/hsi dumps --record`.
- The gate: five steps in `test/gate/steps.edn` (two essential, three `--full`), none on `:ghc`; the oracle tier's four in
  `test/gate/crops/haskell.edn`. The REAP row's read and print sources name the native judges, `bin/reap --record`.
- Stub runs (ghc, ghc-pkg, runghc, ghci, hsc2hs, gcc, cc, clang exiting 99): `bin/repr-haskell test` 18 ok; the round trip of
  five packages, 113 of 113 at the tree, layout and text levels; `bin/hsi check` of 16 programs, all as recorded; no stub called.

## 2026-11-07: batch 2, second round: rust-90, haskell-90, cpp-90, LLS31 (b) and the user's decisions (branch batch2)

On `batch2` at 590a8588, one merge commit each, the earlier merges' rules (union for docs and the Makefile, the lines' `.tmp` diffs
applied, the three keys computed after each):

- **`mac/rust-90` 58ca6783** (`105c549d8`): native.ae kept rust-errors2's rejecting dump and added rust-90's `tree` verb beside `errors`;
  its `file-trees` and `read-error` were taught errors2's diagnostics (maps, not rust-90's `[msg pos]`), and a parse that recovered from
  an error rejects the file there too. The two lines' round-trip controls both used `test/rust/roundtrip-controls.edn` with different
  formats (GT9's crossed pairs, judged inside each level's compare; rust-90's edited subjects, a `controls` verb run last), and both used
  `REPR_RUST_CONTROLS` (a file path; an on/off switch): rust-90's moved to `test/rust/roundtrip-edit-controls.edn` and
  `REPR_RUST_EDIT_CONTROLS`. Alternative considered: one file with both kinds (each reader would see the other's entries). The gate:
  rust-90's six steps (no rustc) replace GT9's four of the same names; GT9's rustc levels are kept (`:rust-helper` for the helper's
  sources) until the planner has `:phase :oracle`, rather than dropped as LLVM's interim step was, since they are the only runs of the
  interface and behaviour levels; `rsi-essential`'s 4 GiB is over GTS215's essential cap, so it runs in `--full` (RES61 proposed).
- **`mac/haskell-90` 0fb4591b** (`b71b3e9be`): no commit containing 53794ed6 (checked with `merge-base --is-ancestor`); its CPP is the
  clean-room `cpp.ae` (haskell-90 applied `mac/haskell-cpp`'s net diff). The branch sits on old main d86cb874 (164 commits, with
  oracle-records-cpp2's tooling cherry-picked): `bin/arbace-go` kept batch 2's gate_key (its `arbace_not_crops` already excludes hsi's
  Go forms and scripts, so no key moved), `bin/gate --pins` runs both the new crops' pins and `bin/oracle-records pins`, the REAP reader
  `:corpus-known` was one method for Rust and Haskell (haskell-90's second `defmethod` would have replaced Rust's). The packs
  haskell-native-inputs (2 files) and haskell-stg-progs (2) were copied, no clobber, into the main checkout's `.tmp/oracle-packs/`.
- **`mac/cpp-90` 4e0a8fc5** (`ef0af119c`): mostly docs and scripts; `bin/cppi`'s moved usage blocks and verbs kept once; lower.ae
  auto-merged (its only hunk, the x86_fp80 decimal literal, already in batch2-lower's integration).
- **LLS31 (b)** (`f95e6c26b`, the user's decision): LLVM's four first-record families kept as `:role :beside` in `test/reap/cells.edn`
  (not deleted, so the records stay visible until the pack `rt` re-records them); the read fact 1 and the judge recounted
  (2,068 + 0.5 x 948) / 3,016 = 0.843, so LLVM's independence is 96.9 rather than the estimate's 94.2 (which held the judge at 0.711).
  LLS31 (a)'s plan written in the LLVM survey: the ids' builds, `bin/llvmi ir-make rt` with `rt-list`, the families roundtrip-rt,
  text-rt, verify-rt with controls, the night's clang commands.
- **Decisions recorded** (`ffbcc29cd`): Rust RES54 (a), RES55 (a), RES56-RES60 as recommended; Haskell HFS-I1 (a), HFS-I2 (a), HFS-I3
  (b), HFS-I4 (b); C++ CP240 (a)(b)(c) in that order, CP241 (b), CP243 (a); Java JCS162-JCS164 (a), JCS165-JCS166 (a), JCS167 (a)
  (recorded in batch 2's JAVA-DEFERRED-TRIAGE.md: the proposals' text is on batch 1's later head from java-complete); batch 2's proposal
  1 (a) (the C++ runtime's IR pin on clang 23.1.5, tonight's step 0 on batch1/next, not run here) and 2 (c) (the night over two nights).
- **Keys**: gate `1bfe8ffffe7e`, runtime `6b114cbcf8f0`, javac `54e889c46334` after every commit (unchanged).
- **Checks**: `gate.crops check` 0; `bin/gate --plan` 49 steps, `--full --plan` 319 (305 run, 8 after); `bin/reap` 41 of 44 checked, the
  one problem deferred.cpp's (the matrix recorded with its reference row held out, as batch 1 did); `bin/crops-score --judge --strict` 0.
  Light samples: rsi judge-test, repr-rust controls 7/7 and a two-file native round trip, repr-haskell test, hsi own-verdicts --check,
  oracle-records pins, repr-cpp roundtrip-corpus samples --judge own 9/9. A slip: `bin/hsi own-verdicts --check` downloaded GHC's source
  and testsuite tarballs and nofib into the worktree's `.tmp/` (the worktree had none; pins matched); proposal 8 in the agenda.
- **Scores** (batch 2 before → after): C++ 75.4 / 91.7 / 20 → 75.4 / 100 / 31.4; Haskell 0 / 8.3 / 20 → 96.3 / 100 / 74.9; Rust
  75.0 / 95.8 / 20 → 75.0 / 95.8 / 80.0; LLVM IR 95.9 / 93.3 / 88.3 → 96.0 / 100 / 96.9; the others unchanged.
- Not merged here: `res34` and `cpp-indep` (later, on these heads).

## 2026-11-05: HF4b, the deriving strategy keywords and DerivingVia (branch `haskell-deriving-via`)

HF4's proposal 3 (b), decided by the user. Strategies written in a deriving clause or a standalone declaration
(`stock`, `newtype`, `anyclass`, `via T`) now set the request's strategy in `arbace/repr/haskell/typecheck.ae`
(`strategy-of`, `rep-of`, `gnd-of`); `via T` is the newtype strategy's coercion with `T` as the representation. Source read:
GHC 9.14.1's `compiler/GHC/Tc/Deriv.hs` (strategy choice, `mkNewTypeEqn`, via) from the pinned tarball, as the specification;
nothing new ported (origin as HF4's, `LICENSE.md`, `doc/ARBACE.md`). Judge findings fixed: a one-method, no-superclass class's
coerced method is the dictionary; a superclass dictionary applying a dictionary function to the instance's parameters alone is
inlined; standalone deriving with a strategy binds implicit type variables at the strategy's span (`rename.ae`). Samples
T23Strategies, T24Via; results in the survey ("As built: HF4b"). Alternative considered: checking the via type's
coercibility now (needs a Coercible solver; left to HF5's Core).

## 2026-11-05: Haskell's HE5 (the libraries' C) and HE6's verbs, on branch haskell-he5

- Branch `haskell-he5` from `haskell-he4d`, light work by day (2 jobs, no gate, no lock): the dumps of the 41 packages
  rebuilt (`bin/hsi dumps`, 1,320 modules), the foreign symbols counted (434; 253 C functions the table lacked, 69 left after
  this work, all in classes or unreached), and the C of the libraries ported by need, proved on seven library samples
  (`test/haskell/eval/samples/Lib*.hs`) against GHC's recorded runs, all equal. Ported from GHC 9.14.1's source tarball
  (`libraries/bytestring/cbits`: `itoa.c`, `fpstring.c`, `shortbytestring.c`, `is-valid-utf8.c` by Go's `unicode/utf8`,
  `aligned-static-hs-data.c`'s tables generated by `bin/lib/hsi/cdata-gen`; `libraries/text/cbits`: `is_ascii.c`,
  `measure_off.c`, `reverse.c`, `utils.c`), licenses bytestring (BSD 3-Clause) and text (BSD 2-Clause) in LICENSE.md; the rest
  (POSIX layer over os and syscall, spawned processes and pipes over os/exec, signals, printf) is Arbace's own.
- Considered and not done: forking (no fork of a Go process: `:process`, a design class), wrappers and foreign exports (the
  helper's dump lacks the exported Haskell helper: HES60), the tests' own C (ci beside hsi: HES64), waiting on descriptors by
  poll (HES61). `bin/hsi divergences` and `slice` (HE6's first verbs) and nofib as a run corpus of `bin/hsi check`.
- Results: check of samples, synthetic and testsuite-core 661 of 763 (650 of 756 before),
  nofib 76 of 144 (57 not built by GHC either, 9 `:speed`, 2 `:callback`), every non-pass in a class
  (doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE5, HE6 (write-ahead)", the night commands there).

## 2026-11-05: Haskell's HE6, second half (a program's own C, callbacks, descriptors, reasons), on branch haskell-he6

- Branch `haskell-he6` from `haskell-he5`, main `d86cb874` merged first (the C crop's prebuilt runtime, `arbace.c.runtime`,
  which hsi's C builds link through); light work by day (2 jobs, no gate, no lock, the main checkout's stage 0), every check on
  lists of a few dozen tests. HES60, HES61, HES64 (a) and HES43 (b) built (doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE6's
  second half (write-ahead)").
- **HES64 (a):** a test's C read and checked by ci and linked with musl (`bin/hsi cnodes`, `test/haskell/eval/cnodes.ae`, hsi's
  glue `test/haskell/hsi/c/`) and loaded into `go/arbace/ci` over hsi's own `crt.Mem` and `crt.Sys` (`hsi run --c`,
  `go/arbace/hs/cprog.ae`); `go/arbace/ci` and `crt` unchanged.
- **HES60 (a), by the probe:** `tools/hsdump/probe/StubProbe.hs` (`bin/hsi stubs-build`) showed that GHC 9.14.1's `ModGuts`
  hold a module's foreign stubs and that the exported helpers (`$fstable...`) are in the STG the hook sees, so in the dumps
  already: HE5's journal entry ("the helper's dump lacks the exported Haskell helper") is corrected here, the missing part
  was the stub C. The stubs are written by the probe's `--stubs` mode and compiled by ci; the RTS's C API is hsi's
  (`go/arbace/hs/rtsapi.ae`), Arbace's own, written from the declarations of the installed GHC's `RtsAPI.h`, `StablePtr.h`,
  `Adjustor.h` and `DerivedConstants.h`'s offsets (GHC 9.14.1, Alpine's ghc package; no code copied). The helper is not
  changed (the dumps' key stays): folding the stubs into it is HES65.
- **HES61 (a):** `waitRead#`, `waitWrite#`, `fdReady` by poll(2), the minimal scheduler change (HE4f's work does not exist on
  the branch); `fcntl(F_GETFL)` of a pipe's write end fixed.
- **HES43 (b):** `hsi-reason:` lines, `:reason` in the results, classes' `:reasons`.
- Considered: a helper extension (HES60 as decided) against a separate stub writer (taken today, the dumps' key untouched);
  hsi's own `printf` and `malloc` beside musl's (taken: musl's whenever a C program is loaded, one buffer and one heap);
  changing `go/arbace/ci`'s loader to name statics (not needed: the node program's `static` commands carry names, and hsi
  rewrites the `at` of an undefined object it defines before ci loads it).
- Results (lists of the testsuite's run tests, 2 jobs): of HE5's 57 `:test-c`/`:test-c-output` tests 33 pass, of its 21
  `:callback` 18, T21969 passes; the 104 recorded passes of the core's `concurrent`, `lib`, `ffi` and the samples and synthetic
  corpora unchanged; `test/haskell/check-hsi.edn` rerecorded for those: 789 of 907 pass (737 before; the core 652 of 701),
  `bin/hsi divergences` 118 in a class, none unclassed. Proposed: HES65-HES68 (the survey's "Proposed (HES65 onward)").

## 2026-11-05: Haskell's HE8 (AOT) designed and its first slice, on branch haskell-he8

- Branch `haskell-he8` from `haskell-he5` (`a7fafe0b`), light work by day (2 jobs, no gate, no lock, each Go build of a
  program under `GOMEMLIMIT=1536MiB`). HE8's design written into doc/haskell/HASKELL-EVAL-SURVEY.md ("As built: HE8
  (write-ahead)"): STG compiled to Go as a second implementation of hsi's node trees' `eval` over the same machine (one Go
  function per closure body, per case continuation and per join point, returning the next code to hsi's trampoline; the
  explicit stack, update frames, blackholes, exceptions, masks, STM, continuations and green threads all hsi's), installed
  per closure by its tree's shape when first entered, every other closure interpreted.
- Alternatives considered: Go's goroutine stacks for Haskell's stack (HES8 (b) again: loses `<<loop>>`, `-K`, asynchronous
  exceptions' unwinding, `control0#` and the collector's walk); STG → Java forms → j2g (Eta's route, §7 (b)); whole library
  modules in one Go package (measured: 9.2 MB of Go for 21 modules of `ghc-internal` peaks at 5.2 GB in the Go compiler, so
  the slice compiles the closures a profile run entered, and HES70 proposes a package per module).
- Built: `go/arbace/hs/aot.ae` (generator and runtime half), hooks in `load.ae`, `nodes.ae`, `rts.ae`, `live.ae` (the kill
  lists sorted: images and shapes depended on a map's order), `hsi aot`, `bin/hsi aot build|run`, `bin/hsi check --aot`.
- Results: `bin/hsi check --aot` over the samples (but Loop, a night's 7 GB) and the synthetic corpus, 61 of 62 equal to GHC's
  runs, AsyncRerun in its class as on hsi, and GT8's essential list (73 testsuite run tests among its 123) 123 of 123, every
  run the same as on hsi (`test/haskell/check-hsi-aot.edn`, 139 programs); a new sample `Classes` (user classes and
  dictionaries) with GHC's reference; every module of the 41 packages generated (88,465 closures, none refused, 337 MB of Go);
  speed 1.05 to 1.23 times hsi on nfib and four nofib programs (the machine's frames and allocation, shared by design, are the
  rest: HES73). Proposed HES70-HES73 (HES65-HES69 left to HE6's branch).

## 2026-11-05: Haskell's line integrated (HE6's second half and HE7 onto HE8's REAP row), on branch haskell-integrate

- Branch `haskell-integrate` from `haskell-row2` (`5045c5c3`: `haskell-he8` merged onto the REAP row); `haskell-he6`
  (`40b58c2f`) merged, then `haskell-he7` (`19268f47`, HE7: the REPL, built on HE6; its own commits carry no journal
  entry: `tools/hsrepl/Repl.hs`, GHC 9.14.1's API session whose `hscCompileCoreExprHook` writes each statement's STG,
  `go/arbace/hs/repl.ae`, `hsi repl`, `bin/hsi repl` and `repl-session` over the seven sessions of `test/haskell/repl/`, the
  survey's "As built: HE7 (write-ahead)", HES69 decided (b)). Light work by day, the main checkout's stage 0.
- hsi's Go forms (`go/arbace/hs.ae`, `hs/nodes.ae`, `hs/rts.ae`, `test/haskell/hsi/cmd/main.ae`) merged as Go, the Python
  crop's method: the packages `arbace/hs` and the driver printed by repr/go from the merge base, ours and theirs, `git
  merge-file` file by file (he6: no conflict left; he7: the driver's usage lines and cases, `aot` and `repl`, both kept), the
  module built and `go vet` clean, read back by `bin/hsi runtime read`; the forms equal to one side wherever only one side
  changed a file. Considered: resolving the `.ae` conflicts by hand (positions and the package's `:files` and
  `:init-order` make the forms' hunks interleave where the Go's do not). Makefile: `hsi-check-aot`, `hsi-stubs-build` and a
  new `hsi-repl-session`; `bin/lib/hsi/check` exports both sides' variables; the survey's sections and the journal as unions,
  HE6's second half, HE7 and HE8 in the order of their proposals (HES65-HES68, HES69, HES70-HES73).
- `test/haskell/check-hsi.edn` reconciled: the 78 programs whose rows differ between HE8's record and HE6's (`bin/hsi check
  --only`, 2 jobs) rerun on the merged hsi, every row now HE6's, and HE8's sample `Classes` added (it passes): 790 of 908. The
  first run met a race: two jobs' first `bin/ci musl ensure` in a fresh worktree at once, twelve programs failing
  `:c-build`; rerun once the cache was made, all twelve as recorded by HE6.
- Results: `bin/hsi runtime check` the same forms; `bin/hsi check` (samples, synthetic) 62 of 63, none unclassed, the controls
  failing; `bin/hsi check --aot` on GT8's essential list 123 of 123, every run the same as on hsi; `bin/hsi repl-session` 7 of
  7 byte for byte, the control failing; `bin/reap --record`, `bin/reap` 20 of 20 cells, 0 problems.

## 2026-11-05: HES73 (a), the compiled code's typed locals and direct calls, on branch haskell-speed

- Branch `haskell-speed` from `haskell-integrate` (`7bda6b88`), light work by day, the main checkout's stage 0, the
  integration's `.tmp/hsi` caches copied (no dump rebuilt). The method: hsi's runtime printed once as Go (`bin/hsi runtime
  print`), edited and built as Go against the AOT (`bin/lib/hsi/aot`'s new `HSI_RT_MOD`, `HSI_RT_KEY` with `HSI_EXE`), each
  step measured on nfib, then read back into the forms by `bin/hsi runtime read` (`go/arbace/hs/aot.ae`, `aotapi.ae`,
  `machine.ae`, `hs.ae`'s init order); the printed Go of the committed forms equal to the edited files.
- Profiled first (Go's pprof on HE8's compiled nfib): the machine's frames and allocation dominate, and
  `GHC.Internal.Classes`, over `HSI_AOT_MAX_KB`, was interpreted whole. Built: an oversized library module compiled in part
  (the profile's closures, kept by their list's digest; `HSI_AOT_PARTS=0` the old way); typed Go locals for the inline
  primops' unlifted results, flushed into the frame only before something else reads it; call sites resolved once (a
  global's function, a class method of a global dictionary) and entered directly; direct Go calls between compiled code,
  bounded (`HSI_AOT_DEPTH`, default 4: 16 measured slower, none a little slower); the machine's case and update frames
  pushed and popped in place (hsi gains 4-11 % too). Not done: a frameless evaluation of a case's scrutinee (designed,
  estimated 1.15-1.3 times for 3-4 days, risky for exceptions, STM and `control0#`: HES74 (a)).
- Results: compiled now against HE8's hsi 1.45 times on nfib 32 (5.84 → 4.04 s), tak 1.34, rfib 1.45, fibheaps 1.12, sphere
  1.17 (against HE8's compiled 1.07-1.32); 3 times not reached: the survey's "As built: HES73", HES74 (recommended (c) then
  (a): `-O1` STG for the compiled level, then frameless evaluation) and HES75. `bin/hsi check --aot` on GT8's essential list
  123 of 123, all the same as hsi; samples and synthetic 62 of 63 (AsyncRerun in its class), all 63 the same as hsi; `bin/hsi
  check` on the essential list 123 of 123; `bin/hsi runtime check` the same forms.
- `bin/hsi cnodes`: the integration's race fixed, `bin/ci musl ensure` under a lock (`.tmp/hsi/cnodes-musl.lock`), a log per
  job; two runs at once gave byte-identical node programs. Considered: preparing musl once in `bin/lib/hsi/check` before
  the jobs (only the check would be safe; the lock covers every caller).

## 2026-11-05: HES74 (c), the compiled level on GHC's -O1 STG, on branch haskell-o1

- Decided by the user (HES74: (c) first, then (a)). The helper unchanged: `tools/hsdump stg --ghc -O1` dumps the program's
  modules with GHC's optimiser on, against the installed `-O2` interfaces; the libraries' dumps were already the installed
  units' `-O2` builds (HES6 (a)), so every worker and specialisation an `-O1` program names is there. `bin/hsi check --opt O1`
  (own dump cache, references, results, record, and `test/haskell/divergences-O1.edn` with `:eval-order`), `bin/hsi
  record-ref --opt O1` (`test/haskell/ref-ghc914-O1.edn`: the essential list and five benchmarks recorded), `HSI_OPT=O1` for
  `bin/hsi aot run`. hsi's own level stays on `-O0`. Considered: dumping `-O1` for hsi too (rejected by the decision: the
  interpreter is the reference); a second library dump at `-O1` (not needed: the installed libraries are `-O2`).
- Two runtime fixes the `-O1` code showed: `runSlice` ends the slice after `ret` when a compiled case continuation spent
  the budget (an inline `yield#` was lost: MVarOrder, ThrowMasked); `integer_gmp_gcd_word` in hsi's C table (the installed
  gmp backend's `gcdWord`, inlined at `-O1`). GHC's own `-O0`/`-O1` references differ on five synthetic programs, all
  constant folding (negative zero, `float2Int#` out of range), which the compiled level matches at `-O1`.
- Measured (one run each, the same host one after the other): compiled `-O1` against compiled `-O0` 6.7-10.3 times (nfib
  0.53 s, tak 2.23, rfib 2.98, fibheaps 3.97, sphere 2.39), 6.0-8.7 times HES73's numbers: the target of 3 met. `bin/hsi check
  --aot --opt O1` on the essential list: 123 of 123, all the same as hsi on the same dumps, the controls failing; after the
  machine's change `bin/hsi check --aot` (`-O0`) and `bin/hsi check` (hsi) on the essential list 123 of 123 each. Details: doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HES74 (c)".

## 2026-11-07: F10a, java.nio's selectors and socket channels over jrt's natives at sun.nio.ch's layer (branch java-complete)

Branch `java-complete` from `mac/batch1` 0edacad62, the main session's brief: raise Java's completeness (72.9% strict, 80.4%
without by design on batch 1) by the triage's next ranks not yet built. F16, F8, F7 and F11 are JC9b's (merged in batch 1,
write-ahead), F12, F6, F13 and F19 JC9c's, F3 JC9's; the next rank was F10 (selectors and channels, rank 13), built here as
F10a: selectors, socket and server socket channels, pipes and the sockets' adaptors (doc/classes/JAVA-DEFERRED-TRIAGE.md,
"As built: F10a (write-ahead)").

- **First, the main session's check of JC9's slices together**: JC9b's finding (F5's `Logger`/`LogRecord` calling
  `ResourceBundle.getBundle(String, Locale, ClassLoader)`, absent from jrt's overlay) is fixed on batch 1 (`b8a1951ce`). JC9b's
  joint javac check (`.tmp/keep/java-jc9b/jc9b/all9/run.sh`, which named the removed java-jc9b worktree) is now
  `.tmp/all9-run.sh CHECKOUT` in this worktree's scratch (default the `next` worktree; writes only `CHECKOUT/.tmp/jc9-check/`;
  exit 1 on a javac error or a missing file; KIND linux handled): batch 1's 2,215 files 0 errors; its negative control (the
  overload renamed) fails with F5's two errors.
- **The design**: jdk26u's channel Java unchanged (`SocketChannelImpl`, `ServerSocketChannelImpl`, `SelectorImpl`,
  `SelectionKeyImpl`, `EPollSelectorImpl`, the pipes, the adaptors: jdk26u `baf63fb`), and jrt's own native layer below it, as
  overlays keeping jdk26u's classes and signatures: `Net` (jdk26u's file, its natives given bodies by `.tmp/f10/gennet.py`),
  `IOUtil`, `NativeDispatcher`, `SocketDispatcher`, `UnixDispatcher`, `NativeThread`, `Poller`, `Util`, `UnixDomainSockets`,
  `EPoll`, `EventFD`, `FileDispatcherImpl`, `PollSelectorImpl` (derived, `genpoll.py`), `SocketOptionRegistry` (derived from the
  template, `genopts.py`, equal by its 17 options to the build's gensrc), all over `jdk.internal.jrt.HostChannels`' 31 natives,
  Go ports of jdk26u's `src/java.base/unix/native/libnio/ch/{Net,IOUtil,SocketDispatcher,UnixDispatcher,PollSelectorImpl}.c`,
  `src/java.base/linux/native/libnio/ch/{EPoll,EventFD}.c` and libnet's `NET_SetSockOpt`/`NET_GetSockOpt`/`NET_Bind`
  (`go/arbace/jrt/channels_linux.ae`; `channels_other.ae` off Linux). Alternatives considered: jrt's own channel classes over
  Go's `net` (as `HostSocketImpl` and F7's `FileChannelImpl`): rejected, the channels' state machines and close protocol are
  jdk26u's own code worth keeping; the poll selector as Linux's default (AIX's choice, one class fewer): rejected for jdk26u's
  Linux default, epoll, whose EPollSelectorImpl stays unchanged once EPoll's event array is a token over int arrays; the
  kernel's blocking descriptors: rejected, a thread blocked in the kernel cannot be woken by the JDK's dup2-and-signal close in
  Go, so every descriptor is non-blocking and a blocking one waits in Go's poller, which `preClose` wakes.
- **New**: KIND `linux` in `print-sources` and its consumers (`bin/jrt-convert`'s tree, origins and summary line, the originals'
  check, the round trip's origins); j2g's VarHandle table takes `ConstantBootstraps.fieldVarHandle`; the rename
  `sun/nio/ch/Util` → `Ch_Util`; variants of `SelectorProvider$Holder`, `SocketChannelImpl`, `PollSelectorProvider`,
  `EPollSelectorProvider`.
- **Checked by day** (light): the joint javac check of the 2,277 files 0 errors; `bin/repr-java read-jdk --check` of the 62
  new files 0 failed, `--no-javac` 64 native and identical; the variants applied and parsed; Go names (one collision, renamed);
  `go build`/`go vet` of `arbace/jrt` for amd64 and arm64 and `go test` on amd64, three new tests passing with every other.
  Not run: the conversion, j2g, the executable and the tests (the night's: the section's commands).
- **Expected**: 49 / 49 tests are F10a's with F7, F11 and F13 built; about 34 / 34 to pass, Java eval and AOT about +1.0
  point each. Java after tonight's records, all the write-ahead slices' expected yields together: about 80.6% strict, 89%
  without by design.
- **Findings**: `bin/jrt build` fails on batch 1 at `arbace/expat` (a crop's package in bin/jrt's module); its print needs
  `GOMEMLIMIT` under the light rules (3.9 GB without, 1.45 GB with 1400 MiB).
- **Proposed** (the triage's "Proposed (F10a)"): JCS162 Thread's blocker (interrupts), JCS163 F10b (DatagramChannel, then the
  asynchronous channels), JCS164 UNIX domain sockets with F9, JCS165 the joint javac check as a gate step, JCS166 the
  derivations committed as Arbace with a check, JCS167 serialization out of by design (the strict mean cannot pass 90% while
  10% of eval and AOT are by design; serialization is 168 / 117 of it).

## 2026-11-07: C++ without clang and g++ at check time, CP240 (a) and CP241 (b) (branch cpp-indep)

The user decided CP240 ((a), (b), (c) in that order) and CP241 (b) (doc/cpp/CPP-SURVEY.md, "Decided (the user, 2026-11-07): CP240,
CP241"). On `cpp-indep` from `mac/cpp-90` (4e0a8fc5b), the day's light tier.

- **CP240 (a).** From `mac/oracle-records-cpp2`: the shared tooling as `mac/haskell-90` took it (`a08c614e9`, its cherry-pick of
  `c3892e875`), then `4ec2da932`, `e63d37180` and `15a312026` with `-x`, each cut to C++'s eval and AOT (the IR packs `cpp-ir-samples`,
  pin `0730915b`, and `cpp-runtime-ir`, `ea55ad3c`, read unless `CPPI_LIVE=1`; `record-ir`, `--every`; the lowering's gcc half behind
  `CPPI_GCC`); left out: tools/cxxdump's records, the round trip's records mode, the cxxdump packs (the print is judged by its own verdict
  on this line). Conflicts: `Makefile` and `bin/gate` (both sides: the oracle-records targets beside batch 1's, the packs' pins check
  beside GTS211's crop pins), `bin/cppi` (cpp-90's codegen verbs and record-ir both kept; runtime-ci's key on `key.records` with
  cppi-deferred's reader key). The gate's C++ run steps and the runtime's build lose `:cxx` and `:gcc`; the live sampling and the packs'
  verify wait in `test/gate/crops/cpp-oracle.edn` for the oracle tier. Proved with the toolchain stubbed: `bin/cppi runtime-build` from
  the pack (198 of 198), `bin/cppi lower-essential` (6 of 6). `bin/cppi check` on ci was stopped (runtime-ci's read at 3.0 GB, over the
  day's 2 GiB): the night's. `runtime/deferred.cpp` is not in the pack (CP245).
- **CP241 (b).** `test/cpp/target.ae` and `cpp/target/libstdcxx.edn`: libstdc++'s installed tree (834 files) and gcc's own include
  directory (151) made from the pinned gcc-15.2.0 tarball (test/cpp/corpora.edn's `:gcc-15.2.0`, sha256 438fd996…) with no compiler:
  827 + 148 files laid out, the build's products by rules stated from inputs and outputs (no GPL makefile or configure ported):
  std.cc and std.compat.cc joined, the gthr headers renamed, c++config.h's switches and configure's 299 answers (173 probes of musl
  1.2.6's headers from its tarball, C's own target's; 126 facts with reasons), limits.h, omp.h and mm_malloc.h (Alpine's patch as four
  substitutions). `bin/repr-cpp target oracle`: 834 of 834 and 151 of 151 byte for byte. The readers read it through `ARBACE_C_SYSROOT`
  (`bin/lib/repr-cpp/sysroot.bash`; with musl's headers and fortify-headers' nine files at recorded digests, every search directory a
  root): pp-check's essential and a sampling of 949 files as with the host's headers, no host header opened (strace).
- **Alternatives**: for the headers, a pack of the installed tree (CP241 (a), 0.5) or a clean-room set (c, weeks, a clean-room runtime
  with it); taking all of `mac/oracle-records-cpp2` (its gate tier's planner changes rekey every step); musl's headers left in place
  (the token check would still read a library's installed tree: the first measure showed only fortify and the compiler's directory
  mattered, so both were made or taken at digests).
- **Scores** (`bin/crops-score --crop cpp`, facts in the worktree's `.tmp/independence.diff`): completeness 75.4, maturity 100,
  independence 31.4 to 54.3 (read 0.148, eval 0.5, AOT 0.5). The night re-records the packs on clang 23.1.5 by hand (the survey's
  commands) and compares the routes for CP240 (b). Proposed CP244-CP246.

## 2026-11-07: RES34 on rust-90's line (branch `res34`, from `mac/rust-90` 58ca67832, `mac/rust-res34` merged)

The user's decision RES55 (a): RES34 first, then Rust's AOT cell. `mac/rust-res34` (std's units translated once per key by cf2g,
`arbace.c.aotlib`, the link's provenance `*link-trace*`) merged into rust-90's line: the Makefile's target list and the survey's
sections conflicted, both resolved by keeping both sides; the code merged clean. RES40 (a) built in the scripts (the prebuilt cache
the main checkout's `.tmp/ci-aot-lib`, shared by the worktrees; `CI_AOT_LIB` exported to the builder), not in `aotlib.ae`, whose source
is in the units' key. `bin/rsi aot-lib` gained `RSI_AOT_LIB_CACHED=1` (the units whose fragments exist) and `compile-list` a line of
the translation's parts. C's embedded set is RES34's own four files (`arbace/c/{build,cf2g,runtime,aotlib}.ae`), unchanged here; their
merge rekeys every library's fragments (the day hard-linked rust-90's after checking fragments made at the new key byte for byte equal).

Proved by day under 2 GiB a process: std's 74 linked units translated once (19,843 functions, none refused, 69 MB of Go, 28 minutes,
1.37 GB); five synthetic programs AOT-built against them pass (2 to 5 minutes each, the builder at most 1.71 GB; Go's build 14 to 17 s
after the first shard's 324 s), against RE4's about 1,000 s and 7 GiB a program whole. The translation is now mostly the printing of
the glue (51 to 77 s: 11,343 `SetCompiled` and 7,786 data runs for `exit-code.rs`): RES64. `collections.rs`'s builder passed 2.3 GB:
RES66. Night commands, the expected scores (completeness 75 → 100, maturity 95.8 → 100) and RES64-RES66 in
doc/rust/RUST-EVAL-SURVEY.md, "As built: RES34 on rust-90's line". Alternatives considered: a per-crate Go package (RES34.1's
rejection stands), and changing `aotlib.ae`'s default root (rejected: it would retranslate every unit).

## 2026-11-07: HFS-I1 (a), hsi from the native front end's STG (branch `hfs-i1`, from `mac/haskell-90` 0fb4591bf)

The user's decision HFS-I1 (a). `bin/hsi check --front native` (`bin/lib/hsi/check`): each program the front end can take (a single
module of library imports, no C, CPP or foreign stubs, no deferred type errors or RULES: about 690 of 908) is compiled by
`arbace.repr.haskell.front` (one process a shard, each program with its own `-X` flags through `front.ae`'s new `--plan`, a program
that loops stopped after `HSI_FRONT_TIMEOUT`) and run from Arbace's STG; the rest from `haskell-stg-progs`; each result names its
`:stg`. The interfaces are a new pack, `haskell-ifaces-ghc-9.14.1` (440 members: the helper's iface closure of the run corpora's
imports and ghc-pkg's reexports, recorded by hand with GHC, `--record-ifaces`). `bin/hsi compare-front` judges a native run against
the GHC-STG record program by program; the default stays ghc (`default_front`) until a night's run shows no pass lost; the switch is
written (`.tmp/hfs-i1-switch.diff` in the worktree, and the survey).

By day (light caps, 2 GiB): 100 programs, 84 from native STG, 79 pass, no pass lost but the two that fail at the light cap from GHC's
STG too. The first slice found four lost passes; two were front-end bugs fixed by GHC's rules (exprIsConApp_maybe and a strict
field's argument, `cgrun051`; dsStrictBind's banged pattern binding, `T11601`), two programs outside the subset (deferred type errors,
RULES). Eval stays 0.5 a case: the libraries' STG and interfaces are GHC's; 1 needs HF9 and HF10. The libraries' pack is stale at
this helper (haskell-90's night item): the day ran with `HSI_LIBS` its unpacked members. doc/haskell/FRONTEND-SURVEY.md, "As built:
HFS-I1 (a)", HFS-I5-HFS-I7. Alternatives considered: run-check's one `-XHaskell2010` for every program (rejected: GHC compiled the
pack's STG in GHC2021 with each test's flags), eligibility by trying the front end on every program (kept as the fallback: a stop
runs from the pack).

## 2026-11-07: batch 2, third round: batch1's last head, cpp-indep, res34, hfs-i1, and the worktrees' Haskell inputs (branch batch2)

On `batch2` 93db65e83, by an agent by day in the light tier (no gate but `--plan`, no build, no heavy lock: the night shift ran on
`next`). One merge commit each, the keys computed after each (`bin/arbace-go --gate-key`, `arbace_runtime_key`, `arbace_javac_key`):

- `mac/batch1` 23d640f54 (java-complete's F10a) as `69d8704c2`: the journal and JAVA-DEFERRED-TRIAGE as unions; all three keys move
  (F10a's jrt natives and overlays): gate `6725b5801608`, runtime `cb6a54899d99`, javac `343e3c44918d`.
- `mac/cpp-indep` 605c62e69 (CP240 (a), CP241 (b)) as `2bc42fc1c`: `bin/gate`'s oracle-packs block (the same as haskell-90's, kept in
  place), the Makefile (the `oracle-records-*` targets already in), `kinds.edn` (both crops' kinds), CPP-SURVEY (batch 2's Decided kept,
  cpp-indep's As built after); its facts from its worktree's `.tmp/independence.diff` applied; C++ independence 31.4% to 54.3% (read 0.148,
  eval and AOT 0.5), `test/crops/scores.edn` recorded (`5fc449d4e`). Two sampling parts moved to `crops/cpp-oracle.edn` (`--full` 317
  steps). No key moved.
- `mac/res34` 5d85bff6e (RES34 on rust-90's line) as `b3de90d41`: `arbace/c/cf2g.ae` (batch 1's CS277 `apack`/`vplen` and RES34's
  placeholders both kept; the namespace loads), the Makefile, RUST-EVAL-SURVEY. res34's RES61-RES63 renumbered RES64-RES66, since batch 2
  had proposed its own RES61 (rsi-essential's memory) the same day; the alternative, renumbering batch 2's, would have moved a number the
  user had already read in batch 2's proposals. Gate `fc2cc71ebe1f`, javac `cc2abcd735f9` (C's embedded set), runtime unchanged.
- `mac/hfs-i1` ce89c3836 (HFS-I1 (a)) as `d0964c426`: FRONTEND-SURVEY and the journal; its facts applied (no score change); the switch
  diff left for the night's `compare-front`, as the main session said. No key moved.
- Proposal 8 (a) built (`2f9eb4fa4`, `2a124fbe9`): `bin/lib/repr-haskell/inputs.bash`, sourced by `bin/repr-haskell` and `bin/hsi`: in a
  linked worktree (git's common dir) or the clean clone (`.tmp/gate-clean/SHA`) the GHC source and testsuite tarballs are hard-linked from
  the main checkout's copies and nofib fetched from its checkout; with no copy there, a refusal: a worktree never downloads (the user's
  network rule). Scheme's SM9 shares the corpora directory itself; here only the immutable inputs are shared, each worktree extracting its
  own (the extraction gets configure products written into it). The main checkout had none: seeded by hard links from batch 2's `.tmp`
  (SHA-256 84aefc21 testsuite, 2a83779c source, the pins'; nofib d4750745 cloned, tree 1f5a8852). Checked by `bin/hsi pins --fetch` with
  batch 2's copies moved aside (all three taken from the main checkout, nothing fetched) and a missing file refused.
- Checks after each merge: `gate.crops check` 0 problems, both plans clean, `bin/reap` 41 of 44 with deferred.cpp's one problem (CP245),
  `bin/crops-score --judge --strict` 0; samples under 2 GiB (`bin/oracle-records pins`, `bin/repr-cpp pp-check --essential` 1.47 GB, `bin/rsi
  judge-test`, `bin/repr-haskell test`). Not run: C's and Rust's AOT after RES34 (the shared `.tmp/ci-aot-lib` is the night's first build).
- Found: repr-haskell's configure skip test is inverted in native mode (proposal 9 in the AGENDA).

## 2026-11-07: batch 4, the oracle-records line consolidated (branch batch4, on batch2 3ff5ad692)

Branch `batch4` from `mac/batch2` 3ff5ad692, the main session's request (consolidating the held branches first). The line
`oracle-records` (doc/ORACLE-RECORDS-SURVEY.md, ORS1-ORS28) is linear: `mac/oracle-records` 3f800b868 < `-c` 58829ed2e < `-scheme`
b8f7825b1 < `-haskell` c2d46e3a6 < `-rust` 3c7ceddf9 < `-ocaml` 056caff62 < `-cpp` 0c025a3f7 < `-cpp2` 557e7aeec (checked with `git
merge-base --is-ancestor`); each merged in that order, one merge commit a branch (589e229e9, 17c101e67, 6ac1674ed, 15a7e32fa, acba96cd7,
c4337a5ad, 453bb382f, df089a139), then the oracle tier's splice (d29725a4f).

- **Why most of it is batch 2's.** Since the line was written (2026-11-05/06) batch 2 took its shared tooling and Haskell's records
  (haskell-90's cherry-picks), C++'s IR packs (cpp-indep's) and C's sysroot (c-indep-b1's), and each crop moved to its own verdicts and own
  targets: C on Arbace's target judging each test by its own verdict, OCaml on Arbace's typer and native reader, Rust on its native reader
  with its own IR packs, LLVM IR on own verdicts with `ref-rt23.edn` and `ref-opt23.edn`, Scheme with Chibi's marks in the check's record
  and psyntax booted on si. The rule kept: batch 2's behaviour where the two differ, the line's additions where they add, every drop named
  with its reason in its merge message.
- **Taken.** The planner's `:phase :oracle` (`test/gate/plan.ae`: modes `oracle` and `full-oracle`, an oracle step whose host is none here
  `absent`), the runner's `absent` (`bin/lib/gate-run.bash`), `bin/gate --oracle`. C's gcc outcomes as records and packs, read under gcc's
  target and judge (`ARBACE_C_TARGET=gcc ARBACE_C_JUDGE=gcc`): `bin/lib/ci/dg` judges by the directives unless gcc's judge, which reads
  `test/c/ref-dg-gcc15.edn` (`--live` runs gcc); `bin/lib/repr-c/c23check`'s modes self (default), records, `--oracle`, `--record-ref`,
  `--verify-ref`; `bin/lib/repr-c/roundtrip`'s originals' packs under gcc's judge; `bin/lib/ci/musl`'s `pack`, `verify-pack`, gcc's target
  taking the products from the pack and `verify-derived` against the pack with no gcc; `bin/lib/repr-c/sysroot.bash`'s `c_gcc_sysroot`
  (the pack `c-headers-gcc15`) in place of `/` for gcc's target. Scheme's `scheme-chez-dumps` pack (SCS2 (a), the user 2026-11-07).
  C++'s selftest split (`test/cpp/selftest/toolchain.txt`, 30 of 34 tests) and ORS28 (b) (the user, 2026-11-06).
- **Spliced.** `test/gate/crops/ocaml-oracle.edn` (OC180 (a)'s), `cpp-oracle.edn` (cpp-indep's, with `cpp-selftest-oracle` and
  `cpp-lower-oracle` added), `haskell.edn`'s four oracle steps (haskell-90's), by `gate.crops splice` (steps, crops, controls; chains
  renamed CROP-oracle; cpp-oracle's crops entry `:cpp`'s); Rust's four `:rust-helper` steps set to `:phase :oracle` in place (and in
  `llvm-rust.edn`, `natives.edn`). The tier: 52 steps.
- **Alternatives considered.** Integrating Rust's corpus packs into batch 2's `bin/repr-rust` so native-check runs without rustc: not
  now, the entries are keyed by the line's helper hash and would all read STALE (ORS29 proposes a re-record); taking the C packs' `:pins`
  on every C step (the line's): no default C path reads them on batch 2, so they would rekey C's steps for nothing; si-boot-oracle and
  Chibi's records: superseded.
- **Keys.** gate `fc2cc71ebe1f` and runtime `cb6a54899d99` unchanged; javac `cc2abcd735f9` -> `73b4ec54ebcd` at the C merge (its
  commit message names `83c9dfbb1623`, a value taken while a conflict marker was still in `bin/lib/repr-c/check`, resolved before that
  commit: corrected in the Scheme merge's message).
- **Checks** (light, main's stage 0, after each merge): `gate.crops check` 0; `bin/gate --plan` 49 (48 after the splice), `--full --plan`
  317 (311), `--oracle --plan` 52, `--full --oracle --plan` 358; `bin/reap` 41 of 44, 1 problem (deferred.cpp's, CP245); `bin/crops-score
  --judge --strict` 0 (recorded at the Scheme merge); `bin/oracle-records pins` 13 packs after copying C's two from `.tmp/keep/oracle-records-c`
  into the main checkout's `.tmp/oracle-packs` (no clobber); C: dg essential 92/92, check --c23 59/3/0 in both modes, musl verify-derived
  equal to the pack; Scheme: the read check on the records with chez stubbed (1,358 + 6 files and probes, 7 scans, equal, no stub
  called); C++: selftest --no-toolchain 4 pass with the compilers stubbed.
- **Found.** `test/gate/crops-test.bash` (in the essential gate's `gate-tests`) failed on batch 2 already: batch 1's table was hardcoded
  (crop pins, chains, `:haskell` as the crop not gated, an injected `:llvm []` that duplicated batch 2's key); brought to batch 2's table.
- **Scores**: Scheme's independence 95.0 -> 97.5 (judge 0.752 -> 0.874, the read check's 1,371 cases at 0.5: recorded dumps; SCS2's
  "about 0.82" was an estimate); every other crop unchanged.
- Proposed: ORS29-ORS32 (the survey's "Proposed (batch 4)").

## 2026-11-07: batch 3, Haskell, C++ native and the small branches (branch `batch3`, on `batch2` 93db65e8)

Merge plan batches 4 and 5 (`.tmp/tag-v2/merge-plan.md` §3, the rehearsal's §4 steps 4-5) and the independent branches, one merge commit a
branch, the light checks after each (doc/AGENDA.md, "Batch 3", has the commits, conflicts, keys and numbers):

- **Haskell.** `haskell-errors3` ed461dd2 (with `-errors`, `-errors2`), `haskell-deriving-via` aa7d0b52, `haskell-o1` b76f992e (with
  `haskell-speed`), `gt9v-haskell` a9853476, `crop-controls-haskell` 72f76e22 merged. The GPL rule: `haskell-cpp`'s clean-room net diff over
  `53794ed6^` was already in through haskell-90; of `haskell-full2` only its commit after `haskell-cpp`, 4ed75d38, was cherry-picked (`-x`).
  No commit containing 53794ed6 is in the history. `parsers-speed` 855d6f49 was rebuilt rather than merged (plan item 32): its six commits
  re-applied by cherry-pick onto the merged tree, each conflict resolved for both lines (the OCaml LR driver's flat tables with the errors
  line's Menhir error strategy; Haskell's lexer slots renumbered `:acc 18 :tab-first 19 :tab-count 20`; the array driver with `lx/fatal!`;
  the fused dump writer for mode `:lines` only), and one semantic fault the textual merge left (`keyword-token` called with one argument in
  the new OCaml fast path, which made every errors-check case differ) found by the proof and fixed. The proof: the native readers' outputs
  before and after on samples, byte for byte (OCaml 44 files' dumps with positions, 8 files' tokens, errors-check 100 of 100; Haskell 42
  corpus modules' dumps, 166 rejected samples' messages, the `read` verb on 15 and 4 modules). `haskell-eval9` not merged: HFS-I4 (b)'s
  rebase of its HE9 commit conflicts in ten of hsi's files (proposal 1). Considered: merging `parsers-speed` and fixing by hand (rejected by
  the plan: a hand merge of two rewrites of the same drivers can drop either side silently).
- **C++.** `cpp-tree` f0e631c3, `cpp-cpr1` ee6f606f (with `cpp-print-review`), `crop-controls-cpp` 25a62973 merged; of `crop-controls`, the
  GT9.8 section 0d1ae970 picked and Haskell's round-trip pair 6dc54221 left out as superseded by haskell-90's controls in the same file
  (the same design over the own-verdict judges); the others were picked in batches 1 and 2. The LICENSE question of plan item 35 (clang's
  disambiguation reproduced or ported in `arbace/repr/cpp/{parse,scope,sema}.ae`) stays as doc/ARBACE.md records it; no new port came in.
- **The small ones.** `goi-budgets` 741e989e (with `goi-not-applicable`), `goi-full-forms` 53f076ea (with `goi-liveness`),
  `java-internal-pkgs` e3d5b57a, `c2j-pool-encode` 74e60353, `ci-lls21` cd76b572, `llvm-ll4` a9a95f40 (LLS32 (a): its records re-made by
  `bin/llvm-corpus verify-record` on batch 3, now judged by LL9a's references) merged; `trio-off` 2e86cdba superseded (its commit is main's
  d86cb874a). Then `mac/batch2` 3ff5ad69 (batch 2's third round) merged, and `bin/reap --record` and `bin/crops-score --record` with the
  night's pending records held out for the run (deferred.cpp, ci-run's two new programs' AOT rows, java-internal-pkgs' three references).
- Keys at the end: gate `bd0295a51879`, runtime `e55979557036`, javac `a7060fc7d33d` (moved by goi-full-forms, java-internal-pkgs,
  c2j-pool-encode, ci-lls21 and batch 2's third round). Scores: Go 94.9 to 97.0 completeness, LLVM 96.0 to 96.3, C++ independence 31.4 to
  54.3 (batch 2's cpp-indep); the rest unchanged. Proposed: HE9 by hand (1), LLVM's RUN-line substitutions in the own verdict (2), the GPL
  line's branches deleted (3), the worktrees' corpora from the main checkout (4).

## 2026-11-07: batch 3's proposals decided and built (branch `batch3`)

The user's decisions (relayed by the main session, about 13:45) on doc/AGENDA.md's "Batch 3, Proposed" and batch 2's proposal 9:

- **2 (a)+(b), built.** LLVM's own verdict takes lit's RUN-line substitution (`bin/repr-llvm`'s `own_verdicts`: the first RUN line's
  `sed 's/A/B/g' %s |`; `verify-check` reads sed's output for a module judged by its own verdict), and a `valid` claim from a RUN line
  reading the module with `--disable-verify` is no own verdict. valid-wide 413 -> 419 of 420; the one left classed in
  `test/llvm/valid-wide-known.tsv` until opt's record. A first version made every `--disable-verify` RUN line void the own verdict and
  lost reject-av's `Assembler-attribute-builtin` (an `invalid` claim from another RUN line): restricted to `valid` claims, the other
  lists' records came back unchanged. `check verify-llvmtest` re-recorded (11 fixed by LL4's messages). Alternative considered: running
  each RUN line's substitution and requiring every result to verify (lit runs each); one substitution suffices for a verdict of the
  module's text, and the corpora's tests substitute a type or an opcode alike in every line.
- **4 (a), built.** `bin/lib/crops/take.bash` (`crop_take`): a worktree's missing crop input taken from the main checkout by hard link,
  refused when the main checkout has none; used by `bin/repr-ocaml corpus fetch` and its opam set. The main checkout's
  `.tmp/repr-ocaml/src` seeded from ocaml-90's copies (hard links; pins checked). Proved under stubs: no download, no compiler call.
  Alternative considered: generalizing `bin/lib/repr-haskell/inputs.bash` in place (rejected: batch 2's proven code, and Haskell's git
  checkout of nofib has its own function).
- **Batch 2's 9 (a), built.** `bin/repr-haskell corpus fetch`'s configure test un-inverted; proved under stubs, with a control that calls
  the compiler stub when `REPR_HASKELL_CONFIGURE=1`. Found: `haskell-native-inputs` is STALE at the helper's new hash, so the native reads
  wait for the night's `record-native-inputs`.
- **1 (a)**: HE9 ported by hand the day after batch 3's night (doc/AGENDA.md, "The day after batch 3's night"); not started.
- **3 (a)**: the GPL line's branches deleted locally and on `mac` by the main session once batch 3 is on `next`.
- The REAP matrix and the scores re-recorded (LLVM's read cell after the verify re-record: completeness 96.4).

## 2026-11-07: batch 3 merged into batch 4 (branch batch4)

`mac/batch3` 90396a29c (batch 3 on batch2 3ff5ad692: Haskell's remaining lines, C++'s tree, CPR1 and controls, the small branches,
llvm-ll4; its second round: LLVM's RUN-line substitutions, `crop_take`, Haskell's corpus fetch without configure) merged into `batch4`, one
merge commit, the main session's request: `batch4` is the single head of everything after batch 1. Conflicts only in doc/AGENDA.md and
doc/JOURNAL.md (both sides kept, batch 4's sections first); `test/gate/steps.edn` and the fragments merged cleanly, the oracle tier's 26
`:phase :oracle` declarations intact, and batch 3 plans no toolchain step outside it but the three ORS31 names (`cpp-dg`, `cpp-roundtrip`,
`cpp-roundtrip-dg` on `:cxx`, as before). Keys: gate `bd0295a51879`, runtime `e55979557036` (batch 3's), javac `4414d352e77c` (batch 3's
plus batch 4's `bin/lib/ci`, `bin/lib/repr-c`). Checks: `gate.crops check` 0; `--plan` 48, `--full --plan` 311, `--oracle --plan` 52;
`bin/reap --record` with batch 3's hold-out (its `.tmp/reap-rec.sh`, run in batch4: deferred.cpp, ci-run's two programs, three JDK
references held) 0 problems, the matrix unchanged; `bin/crops-score --judge --strict --record` 0 with the rows held (unheld, the held rows'
9 reap problems and 1 score problem stay until the night's records, as on batch 3). Scores: Go 94.9 -> 97.0 completeness, LLVM IR 96.0 ->
96.4 (batch 3's), the rest as batch 4. The nights of batches 2-4 in one ordered list: the AGENDA's "Batches 2-4: the nights after batch 1".

## 2026-11-08: C's own target triaged (branch `c-triage`, on main 2a72c94aa)

The brief: C's records on its own target (c-indep-b1) committable, every non-pass in a real class, C at or above 95 / 100 / 99.7.
Inputs: night 7's own-target records and logs (the main checkout's `.tmp/night7/`), night 8's class `:ci-run-consolidation` (12
cases). Detail in doc/C23-SURVEY.md, "As built: the own target's triage" (T.1-T.9, CS281-CS283).

- **The consolidation's cause** was not a merge: stage 0, built from the seed `d5548e72` (2026-11-05), embeds ci compiled and RT.load
  takes the compiled loader; `RT.toolSource` reloads repr/c from the checkout but not `arbace/c/`, so LLS21 (a)'s and c-deferred's
  ci changes (2026-11-05/06), proved on their branches over the checkout's ci, ran in no C check of main. Shown on stage 0 (the
  embedded `arbace/c/build.ae` differs, its loader set) and by `bin/ci check --only`: ci-run's four fail as recorded, then pass with
  the checkout's ci. Fix: `bin/lib/ci/fresh.bash`'s `c_ci_fresh` (from `bin/ci`): the three variables PB.6 used when the checkout's ci
  differs from the host's. Alternatives: `-s`-loading ci's namespaces over the compiled ones (`arbace_tool -s`; rejected: compiled
  callers and redefined types would mix two ci's), RT.toolSource (needs a re-seed: CS281 (a)), a re-seed now (CS281 (b)).
- **c23-complit-4**: threads run since CS276 (b)'s scheduler, but a `static thread_local` compound literal was an ordinary static:
  `arbace.c.build`'s compound literals take `:tls`, cf2g reads thread-local statics through `ci.IStatic`. All twelve and
  binary-trees-5 pass at eval and AOT (`bin/ci check [--aot] --only`).
- **Annotations** (14): four pass with the checkout's checker; zlib's two read as ci reads (`*include-inside*` in
  `arbace.repr.c.annot`); the benchmarksgame six by Arbace's `mm_malloc.h` declaring `posix_memalign` (musl hides it under strict
  ISO C); 20040629-1, 95_bitfields and quickjs's unicode_gen.c classed `:item-placement` (their entries the night's patch
  `.tmp/worktrees/c-triage/.tmp/night/annot-known-item-placement.patch`, applied before `annotations --record`: the committed
  results still pass them, and a known entry the results pass is a REAP problem).
- **Round trip** (11): gcc-embed's four by rereading a print by its absolute name when the original was read so
  (`arbace.repr.c.rtcheck`); fprintf-1's two stale entries removed; c23-named-loops-1, 12.S and 13.S deferred to the oracle tier by
  their recorded rows (`bin/lib/repr-c/roundtrip`); limits-externdecl and c23-binary-constants-1 back as `:eval-limits`.
- **The token check's `--record`** runs gcc only under `ARBACE_C_JUDGE=gcc` and keeps the `:gcc` level otherwise (`ppcheck/record`,
  a test in test/c/judge_test.ae).
- `bin/reap --record` and `bin/crops-score --record` on the tree for the known edits (0 problems; C 95.1 / 100.0 / 99.7).
- The night's commands: doc/C23-SURVEY.md T.7 (the records in order, the class's removal by
  `.tmp/worktrees/c-triage/.tmp/night/remove-ci-run-consolidation.patch`, reap, the scores).

## 2026-11-08: HE9 ported by hand onto main (branch `he9-port`, from main 2a72c94aa)

By batch 3's proposal 1 (a) (the user, 2026-11-07): `haskell-eval9`'s 3f57a1d32 (HE9's allocation counters and limits, allocLimit1-4,
and the RTS's options from GHC 9.14.1's `rts/RtsFlags.c`, cgrun057 and T16916) ported onto main's hsi, which since moved (haskell-90's
native read and own verdicts, hfs-i1's `--front native`, b193d0de8/2aafa3a35/6cc1ea87f's records and the stubs' lock). The forms are a
read of their Go text (HES38), so the port was made on the text: 3f57a1d3's parent, 3f57a1d3 and main printed by `bin/repr-go print`
(`go/arbace/hs.ae`, `test/haskell/hsi/cmd.ae`), 3f57a1d3's Go diff (14 hunks; the forms' diff is mostly positions) applied to main's
print by `patch` (10 hunks, each with an offset or fuzz read again) and by hand (4: the driver's `--threaded` and `--with-rtsopts` beside
main's `--c-main`, `rtsSize` removed, the Machine's `AllocGrace` beside main's `inCFin`), `gofmt`, `go vet`, `bin/hsi runtime read`:
`hs.ae`'s `:files` and `:init-order` and `cmd.ae`'s made by the read. The three new files (`go/arbace/hs/alloclimit.ae`,
`test/haskell/hsi/cmd/rtsflags.ae`, `rtsusage.ae`) are 3f57a1d3's byte for byte; `bin/lib/hsi/check`'s hunk (the program's
`-threaded`, `-with-rtsopts`) re-applied. LICENSE.md and doc/ARBACE.md name the three files' GHC origins. Not taken: `haskell-eval9`'s
records (recorded at helper 6542fb8a3e38 and over HE9's own C route, which main replaced by batch 3's HES60 (a) and HES64 (a)).
Alternative considered: re-applying the hunks on the forms (rejected: every hunk shifts the positions of the forms after it, and the
forms must stay a read of their print, `bin/hsi runtime check`).

Checks (light, 2 jobs): the six programs 6 of 6 (main's hsi 0 of 6); 67 programs of the testsuite's core (the 13 with RTS options, a
third of the concurrent tests, every twentieth other pass) 66 of 67, main's hsi 62, no pass lost (T8726 out of memory on both at
`ARBACE_MEM_GIB=3`, passes alone); `bin/hsi runtime check` passes; hsi-essential's command 123 of 123, controls failing. Expected after the
night's `bin/hsi check ... --record`: the testsuite's core 658 of 701 (652), the record 796 of 908; the divergences' edits and the night's
commands in doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE9 on main (write-ahead)". HE9's 666 counted 11 programs whose references
main's record-ref cannot build (the C includes GHC's `_stub.h`): HES83 proposed.

## 2026-11-08: C++ and LLVM IR re-pinned to clang 23.1.5 (branch `cpp-repin`, from next bd33e27cc)

The user decided (2026-11-08) to follow the host: Alpine's `clang23` is 23.1.5-r0 (with `llvm23`, `llvm23-libs`, `clang23-libs`, `clang23-headers`), the pins
said 23.1.4, and `bin/gate --pins` failed on C++ and LLVM alone. The network was allowed for this task only; fetched:
- `https://github.com/llvm/llvm-project/releases/download/llvmorg-23.1.5/llvm-project-23.1.5.src.tar.xz`: 179,220,264 bytes, SHA-256
  `fb50b1eac93ddeb3aa8dab4cfe324dff74f4a9c2bdee6160c9e36188f8d06a6e` (equal to the digest of the release's assets API), SHA-512 `fae39af4...ab5880`
  (equal to the line of the APKBUILD), `xz -t` ok, 197,239 tar entries; kept in the main checkout's `.tmp/llvm/`. The `.sig` was not fetched (no gpg).
- `https://gitlab.alpinelinux.org/alpine/aports/-/raw/03cb9a15bb287ab788d73f542ce1c51d8a22609e/main/clang23/APKBUILD` ("main/clang23: upgrade to 23.1.5",
  found by the commits API of main/clang23/APKBUILD): SHA-256 `2f6e63a658b3ffb7a99f90916d00b39130df454962f169fd38588a70ac9104ce`, 15,153 bytes; pkgver 23.1.5,
  pkgrel 0, the six patches' SHA-512s as before; kept in the main checkout's `.tmp/cpp-pins/`.
- the release metadata (assets API) and the tag object `604d7ddfacefc074968a9ba0d88c667ab31b42bd` (commit `392dfff97d29b59d77e5cac6d4b6ef2585fe7117`, verified).
Changed: `test/cpp/corpora.edn` (keys `:llvmorg-23.1.5`, `:llvm-test-suite-23.1.5`; tag, commit, sizes, digests, `:alpine-clang23`'s commit, digest, version,
installed packages; the test-suite tarball's size and SHA-256 from the assets API, still not fetched), `bin/repr-cpp` (version checks, the pin keys; the
tarball and APKBUILD default to the main checkout's `.tmp/`, so worktrees need no copy), `bin/llvm-corpus`, `bin/llvmi`, `bin/lib/repr-cpp/dg`, the
Makefile line, the surveys' notes (CPP-SURVEY, LLVM-CROP-SURVEY), AGENDA's proposal 1. Not changed: the ports' origin comments in `arbace/` (they ported
23.1.4 sources; in `gate_key`), LICENSE.md's origin lines, and every record (below). `test/cpp/host/` already holds 23.1.5's answers.
`bin/gate --pins` passes. Records made on 23.1.4, to re-record at night: `test/llvm/{ref-lli23,ref-opt23}.edn`, `check-{text,roundtrip,verify}*-linux-amd64.edn`
(`bin/repr-llvm check ... --record`, as each file's header names), `test/llvm/llvm-test-counts.tsv` (`bin/llvm-corpus scan` against the new tarball) and the
lists under `.tmp/llvm/lists`/`test/llvm/essential*` against it, `test/llvm/ir/{c,tx}.cmd` and the IR packs (`bin/llvmi ir-make`), `test/cpp/check-dg-linux-amd64.edn`
and `divergences-dg.edn` (`bin/ci`-style dg run, `bin/repr-cpp dg --record`), `test/cpp/runtime/ir.sha256` and `units.edn` (`bin/cppi runtime-build --update`),
`test/rust/runtime/std.edn` (`:clang` line), and the oracle packs of cpp-ir-* (`bin/oracle-records`). The old 23.1.4 tarball in `.tmp/llvm/` stays until those are done.

## 2026-11-08: the round trip's `--record` without gcc, CS282 (a) (branch `c-rt-record`, from next bd33e27cc)

By the user's decision of 2026-11-08 (CS282 (a)): `bin/repr-c roundtrip --record` in the default mode records levels 1-2 and keeps
levels 3-4 and gcc's acceptance from the results file's recorded rows, as the token check's record keeps its `:gcc` level (c-triage's
T.5), so the night can record C's print on C's own target. Built in `arbace.repr.c.rtcheck` (`load-recorded`, `recorded-levels`,
`judge`'s recorded mode and the verb `judge-recorded`; `record` sorts each corpus's entries) and `bin/lib/repr-c/roundtrip` (`--record`
no longer implies gcc; `--no-gcc --record` allowed). Two rules found on the samples: a file with no recorded level 3 keeps a known
entry of a class only levels 3-4 give (zlib inflate.c, read only since CDF.8); a row known by such a class whose entry the known file
dropped since (reverse-complement-5.c, 808d08d60) loses its recorded levels 3-4. Alternatives considered: deferring the stale entries
as the no-gcc judge's awk does (rejected: the record would drop them from the known file); keeping `--record` the oracle tier's
(CS282 (b), not chosen). Test `roundtrip-record-keeps-oracle-levels-under-own-verdict` (with the round trip's and token record's
tests: 6 tests, 60 assertions, 0 failures); the record over tcc-pp, zlib, gcc-va-opt and benchmarksgame (exit 0, 98 pass, 3 known,
the known file unchanged; its rows not committed). fprintf-1's and fprintf-chk-1's rows tidied from an `--only` run judged by
`judge-recorded` (both pass), `bin/reap --record` (C print 4,770 passing) and `bin/crops-score --record`. Details and the night's
command: doc/C23-SURVEY.md, "As built: CS282".

## 2026-11-08: C++'s own target, the route's switch prepared, C++'s last `:cxx` steps off --full (branch `cpp-target`, from next bd33e27cc)

Three steps toward C++'s independence (doc/cpp/CPP-SURVEY.md, "As built: cpp-target"):

- **CP244 (b)** (recommended in "As built: cpp-indep", taken): the token check read g++'s recorded answers (test/cpp/host: predefined macros
  per standard, the option lists' deltas, `__has_` answers, the search list). `test/cpp/predef.ae` now computes them from C's ABI
  description (`c/target/abi.edn` through `c.target/macros`, the C standard of the same inline semantics, C's own macros dropped, the
  double constants in C++'s functional cast) and `cpp/target/dialect.edn` (`__cplusplus`, the 78 feature-test macros by year from SD-6,
  g++'s own macros, char8_t, the options' rules, the x86 ISA's implications, Table 21's attribute values, the GNU attribute and builtin
  lists), with `cpp/target/probes.tsv` (the 118 names the corpora ask) and `cpp/target/option-lists.txt` (334 lists harvested from the
  token check's samplings), into `cpp/target/host/` in test/cpp/host's layout. No GPL source was ported: the rules are stated from
  ISO/IEC 14882:2024 [cpp.predefined] and [cpp.cond], WG21's SD-6, the GCC 15.2 manual and musl 1.2.6's include/stdc-predef.h, and checked
  against the recorded outputs: `bin/repr-cpp target host-oracle`, 19 of 19 files equal (16 standards line for line, 118 answers, 324 option
  lists, the search list; the 10 lists g++ refused have no answer to compare). `REPR_CPP_TARGET=arbace` the default
  (`arbace.repr.cpp.host/gxx-dir`; `gxx` the record); the sysroot's roots from the target's search list; `cpp-read-essential` verifies the
  target first. Alternatives: computing the deltas at check time from the options (rejected for now: the generated file keeps load-host
  and the check unchanged, and verify proves it current); taking the 324 recorded keys as the list (rejected: harvested from the lists
  instead, `target option-lists`).
- **CP240 (b)** (decided 2026-11-07), prepared: `bin/cppi route-compare` (clang's route against auto on the same programs, judgments side
  by side, exit 1 when auto loses a pass) and the one-word switch `cppi_route_default` in bin/lib/cppi/route.bash. Found and fixed: the
  route's list was read with `IFS=$'\t'`, which collapses an empty options field, so every auto fallback to clang of a program with no
  options was STALE. Proved by day on the IR stage alone (the samples and the runtime programs, the packs for clang's half) and on made-up
  judgments; the full comparison needs runtime-ci (about 3 GB) and a disk under 80%: the night's.
- **ORS31 (a)** (batch 4's proposal): `cpp-roundtrip` and `cpp-roundtrip-dg` `--judge own` on stage 0; `cpp-dg` `--own` (a new option:
  preprocess's dg directives on the shared preprocessor, modules' round trip by its own levels) on stage 0; clang's dg-error part and the
  round trips' clang levels moved to the oracle tier (`cpp-dg-oracle`, `cpp-roundtrip-oracle`), as the user's decision of 2026-11-07
  moves the toolchain's runs. Alternative: keeping dg-error in `--full` on `:cxx` (rejected by that decision; CP247 proposes its packs to
  bring it back on records).

Checked with clang, clang++, clang-23, g++, gcc, cc, cpp, c++, cc1, cc1plus, ld and as stubs exiting 99 first on PATH (none called): the
target's verify and oracle; `pp-check --essential` (53 files as before, the controls failing as they must) and `--corpus gxx-dg --sample
20` (904 files, as test/cpp/host gives) under strace, no file of test/cpp/host or the installed headers opened; the dg check's preprocess
part and modules `--own --sample 20` (60 of 60); the samples' own round trip (9 of 9). `bin/crops-score --crop cpp`: 76.1 / 100 / 54.3 to
76.1 / 100 / 67.7 (read 0.148 to 0.648, judge 0.568 to 0.735), recorded in test/crops/scores.edn. Proposed CP247-CP249.

## 2026-11-08: HES83, the 11 programs of HE9's C route (branch `hs-hes83`, from next bd33e27cc)

By the user's decision of 2026-11-08 (HES83: (b) own verdicts where a program ships its `.stdout`, (a) for the rest). Source
studied: GHC 9.14.1's testsuite (`.tmp/hsi/ghc-9.14.1/testsuite/tests`, the pinned tarball): each of the 11 tests' `all.T` runs a
`pre_cmd` whose `Makefile` rule compiles the Haskell first (`-c NAME.hs`, so `NAME_stub.h` exists before the C is compiled; GHC
compiles a command line's C before its Haskell, checked by hand) or the `_ghci` tests' object (`-c NAME_c.c -o NAME_c.o`); and
`rts/sm/Storage.c`'s `rts_clearMemory` and `rts/include/RtsAPI.h` (GHC 9.14.1's source tarball), for the no-op's reasoning.

- `bin/lib/hsi/record-ref`: d539b644's order (`haskell-eval9`) ported: a program whose flags name C, Cmm, assembly or objects has
  its Haskell compiled first (`--make -no-link`, no `-stubdir`), and an object with its C beside it made by `ghc -c`, then the
  build. Alternative considered: d539b644's run_plan hunk (the object spelled as its C), rejected: the plan is shared with
  `bin/hsi check`, which maps objects already; running the Makefile's rule itself (needs make and the testsuite's variables).
- The references recorded by GHC 9.14.1 by hand (`bin/hsi record-ref --corpus testsuite-core --only` the 11 and conc059): all
  build, the same statuses and digests as d539b644's; `bin/hsi own-verdicts --record`: 586 of 908 (580), the six with a shipped
  `.stdout` GHC's run gives (T20735, the three `_ghci` tests, T5594, ffi002) judged by their own verdict.
- hsi (`go/arbace/hs/rtsapi.ae`, `rts.ae`, made on the printed Go and read back): `rts_clearMemory` a no-op (hsi's heap is Go's);
  the collector marks the threads in a foreign call that called back as roots before deciding the blocked ones (GHC's
  `markCapability` marks `suspended_ccalls`): ffi023's main had been resurrected with BlockedIndefinitelyOnMVar.
- `test/haskell/divergences.edn`: the 10 passing leave `:no-reference`; conc059 (only the threaded ways; GHC's normal-way run times
  out, hsi's `hs_exit` lets the forked threads print) in a new `:threaded-only`; rts_clearMemory stays in `:test-c` until the night.
- Checks (light): the 11, 10 pass (rts_clearMemory out of memory under the 2-3 GiB cap only: a 1,000,000-deep stack, its copies at
  100,000 and 300,000 pass at 294 and 714 MB); 54 of 55 with 44 passing callback, finalizer and deadlock programs (no pass lost);
  `--record` of the 10 and conc059; `bin/hsi divergences` 800 pass, 0 unclassed; the core 662 of 701 recorded (652), 669 expected
  after the night's record; `runtime check`, `own-verdicts --check`, hsi-essential's command 123 of 123.
- Proposed: HES84 (the threaded ways' references and `hs_exit`'s shutdown), HES85 (nofib's C in record-ref). Details and the
  night's commands: doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HES83 (write-ahead)".

## 2026-11-08: ocamllex ported, compiler-libs typed natively, OC185's switch prepared (branch `ocaml-lex`, from next bd33e27cc)

By the brief's recommended items (OC186 (a), OC188 (a), OC185 (a) decided 2026-11-07), depth first, under the day's light rules (doc/ocaml/
OCAML-SURVEY.md, "As built: ocamllex ported (OC186 (a)), compiler-libs typed natively (OC188 (a)), OC185's switch prepared").

- **ocamllex** (`arbace/repr/ocaml/ocamllex.ae`): OCaml 5.5.0's `lex/` (the release tarball https://caml.inria.fr/pub/distrib/ocaml-5.5/
  ocaml-5.5.0.tar.xz, SHA-256 fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba: lexer.mll, parser.mly, cset.ml, table.ml,
  lexgen.ml, compact.ml, common.ml, output.ml, main.ml) and stdlib/set.ml's AVL trees ported by hand, rule for rule (LGPL 2.1 with the
  linking exception: LICENSE.md, doc/ARBACE.md). The tree shapes of Set matter: a union keeps one of two equal identifiers by height, and its
  location is printed; OCaml's evaluation orders matter too (a constructor's arguments right to left, `let ... and` left to right). The
  parser is a shift-reduce on ocamlyacc's precedences, with ocamlyacc's default reduction of `regexp: Tident` before the lookahead (found by
  400 mutants against the host's ocamllex: 399 equal before, 400 after). `bin/repr-ocaml ocamllex`, `ocamllex-check` (29 .mll: the tarball's
  18, the opam set's 3, 8 samples of `test/ocaml/ocamllex/`; byte for byte the host's ocamllex 5.5.0's outputs and messages, recorded in
  `test/ocaml/ocamllex-expected.edn` by `--host --record`; a negative control), in `ocaml-read-essential`. `-ml` (outputbis.ml) not ported:
  its order is OCaml's Hashtbl's (OC191). Alternatives considered: committing ocamllex's `lexer.ml` (OC186 (c), a toolchain record), running
  lex/ on oi (OC186 (b), it needs a lexer generator to bootstrap).
- **bin/oi**: compiler-libs' `lexer.ml` from the native ocamllex (`OI_OCAMLLEX=host` the host's). The native library then built Lexer; the
  typer's fixes (typing/typecore.ml's disambiguate_by_ids, unify_kept, type_approx, Ppat_open; env.ml's normalize_module_path through
  applications; includemod.ml's add_signature of the implementation; datarepr.ml's inline records of exceptions, Record_extension, and of GADT
  constructors) took compiler-libs from 35 to 44 of 44 units (doc/ocaml/TYPER.md §25). Under stubs of the toolchain (none called): the five
  compiler-libs programs of the run corpus 5 of 5 natively (2 before), libs4's 18 with the essential list 75 of 76 run (the one other
  `-safer-matching`, classed), typer-check 79 of 79, repl-check 24 of 24.
- **OC185**: `bin/oi reap-switch [--apply]` (`test/ocaml/oc185.ae`): the four native records' passes against their oracle twins', and the
  cells' and the independence facts' edit when none has fewer (proved on a scratch copy). Not applied: the native records of the suite and
  the AOT level are the night's.
- **The by-design review**: eval's 205 (180 by a decided rule, 25 `c-api` deferred); 14 reachable cases their rules do not cover:
  Proposed OC192-OC197 (finalisers, max_domains, ocamlyacc, a test's second program, speed, the `.mll` tests), with OC191.
- Scores (crops-score, the records unchanged): OCaml 80.9 / 95.2 / 100 / 87.4, before and after; the night's commands in the survey's section.

## 2026-11-08: HFS-I2 (a), Haskell's read stage's inputs derived (branch `hs-inputs`, from `next` bd33e27cc)

The user's decision of 2026-11-07, HFS-I2 (a): the native reads' inputs, held until now by the pack
`haskell-native-inputs-ghc-9.14.1` (GHC's installation's files, recorded by hand), derived without GHC or a C compiler, data first,
then hsc2hs's C program on ci. Sources: GHC 9.14.1's source tarball (https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz,
SHA-256 `2a83779c...101e77`, the corpus's pin: hadrian's `src/Rules/Generate.hs` and `bindist/Makefile`, `rts/configure.ac`,
`rts/gen_event_types.py`, the packages' `configure.ac`s, `libraries/ghc-internal/aclocal.m4`, `utils/deriveConstants/Main.hs`,
`utils/hsc2hs/src/`, `libraries/Cabal/Cabal-syntax` 3.16.0.0) and musl 1.2.6's tarball (C's pin, test/c/corpora.edn).

- **Built.** `arbace.repr.haskell.target` (the tree in the pack's layout: the settings file in hadrian's order, the 47 package records
  through `arbace.repr.haskell.cabal`, a port of Cabal-syntax's field parser, conditions, free text and `showInstalledPackageInfo`, then
  the bindist's `patchpackageconf`; the installed headers by `install-includes`; ghcversion.h, ghcplatform.h, the event headers,
  HsIntegerGmp.h, ghc-gmp.h, musl's stdc-predef.h); `arbace.repr.haskell.configure` (the seven configure headers by config.status's
  rewrite over `test/haskell/target/answers.edn`, which `bin/repr-haskell target probe` computes on Arbace's C target: a check's kind
  from its autoheader comment or a rule of its configure.ac, headers in musl's tree and `c/include`, functions among libc.a's
  definitions, compile tests by ci's checker, the constants' and htypes' types by `bin/ci annot`, values by `bin/ci run`);
  `arbace.repr.haskell.constants` (deriveConstants: its `wanteds` read from Main.hs, the arrays' sizes from ci's checker's annotations
  in place of nm); `arbace.repr.haskell.hsc2hs` (hsc2hs 0.68.10's direct mode, its program built and run by `bin/ci run`);
  `test/haskell/target/installation.edn` (the installation's names and choices); `bin/repr-haskell target derive|hsc|probe|compare`,
  the derived tree the native reads' default (`REPR_HASKELL_INPUTS=pack` the pack, now the oracle tier's), what is not made yet taken
  from the pack and named in `BORROWED`; `haskell-roundtrip` runs `target hsc` first; the oracle tier's step `haskell-target-oracle`.
- **Proved.** `target compare`: 335 of the pack's 337 members byte for byte; the two left are GHCi/FFI.hsc's, whose C reads libffi's
  `ffi.h` (HFS-I11 proposed). The native dumps of seven packages (166 modules, 61 .hsc, 60 CPP views) byte for byte the same from the
  derived tree as from the pack; `roundtrip --text --layout` over them 166 of 166 at every level with ghc, ghc-pkg, runghc, ghci, hsc2hs,
  gcc, cc, clang, cpp, c99 and ld first on PATH as stubs exiting 99, none called.
- **Found.** ci's checker accepts a pointer in an integer constant expression and a double cast to a pointer (gcc rejects both): the
  probe decides FP_COMPUTE_INT's and FPTOOLS_CHECK_HTYPE's cases by the declarations' types instead; building the RTS's headers is
  refused (SpinLock.h's inline assembly), so deriveConstants reads sizes from the checker, as Main.hs reads them from an object file.
  ci's musl index leaves out `src/ldso` and the assembly sources (vfork, dlopen, dlinfo): the probe scans them.
- **Scores** (`bin/crops-score --crop haskell`): 96.3 / 100 / 74.9 (read 0.804) before, 96.3 / 100 / 78.8 (read 1.000) after;
  `test/crops/independence.edn`'s read facts and `test/crops/scores.edn` recorded.
- **Alternatives considered.** Porting Hadrian's unit-id hash (the ids stay recorded names, HFS-I10); committing the hsc2hs outputs
  or packing them (HFS-I8: made by key in `.tmp/` as built); a configure.ac interpreter (the templates' autoheader comments name the
  macros; the rest is 120 rules with their configure.ac's logic, each checked against the pack).

## 2026-11-08: SCS3, Scheme's suites judged by their own verdict alone (branch `scheme-scs3`, from next 7696e50da)

SCS3 (b), recommended in scheme-90's report under the user's own-verdict decision of 2026-11-07: `bin/si check` and `check --aot`
(`arbace.scheme.check`) judge each program by one judge (`judge-of`): Racket's 27 R6RS suite programs by their own report alone
(`suite-items`: each failing test an item, the report's count of tests against the program's new `:tests` in
`test/scheme/si/programs.edn`, taken from the first record, where si's and Chez's reports agree on all 25 that report: the blind spot
scheme-90 noted, a run that stops early with status 0, now fails); si's 10 programs by golden outputs of Arbace's own
(`test/scheme/si/golden/`, from si's runs, equal to Chez's recorded runs, reviewed line by line against R6RS and si's documented design:
doc/scheme/SCHEME-SURVEY.md, "As built: SCS3"); Larceny's 13 by Chez's recorded run, as before. The five programs Chez fails the same
way, which the recorded-output judge passed, are classed in `test/scheme/divergences.edn`: `chez-exact-zero` (racket/base's five
division and angle tests: Chez's exact-zero rules, si's tower by SM5), `chez-console-port` (racket/io/ports: the console's input/output
port), `chez-condition-message` (racket/exceptions: a lexical violation's message a format string), all `:design`, and `suite-ill-formed`
(`:na`: racket/run imports a library the suite lacks, racket/test calls an identifier its library does not export). Controls one a
judge (`test/scheme/si/controls.edn`, a third: larceny/run-benchmark against larceny/exceptions's Chez run), and `judge-test`, the judges
on made-up runs at every judgment. `judge --record --only` keeps the record's other entries (as SC182 (b)'s mats). Alternatives
considered: counting tests from the source (rejected: the suites' macros make tests in loops, test/output runs two), keeping the
recorded-output judge beside (SCS3 (a), rejected by the recommendation: it reads Chez's runs at check time), classing the ill-formed
programs `:design` (rejected: they are not programs of the language).

Checked: the essential interpreted subset (49 programs: 43 pass, 11 items all classed, no regression, the controls differ, judge-test ok),
larceny/fib alone, the essential AOT subset (11) and the nine other programs whose compiled verdict changes, `bin/si own` (37 of 49 judged
without Chez; the 5 classed are Chez's failures too), `bin/repr-scheme c2j-check` (18 of 18), `bin/reap --record`, `bin/crops-score --record`.
Scheme's scores: completeness 98.4 to 96.8 (by design out 98.9 to 98.8: three programs now fail their own tests by design, two leave the
denominator), maturity 100, independence 97.5 (judge 0.874 to 0.876; bin/si check's terms 0.77 to 0.87). Night: `bin/si check --record`,
`bin/si check --aot --record`. Proposed: SCS4 (the five tests: keep Chez's semantics), SCS5 (the read check's 1,249 probes judged by the
report's properties: the judge's largest term), SCS6 (Larceny's golden outputs).

## 2026-11-08: LLS31 (a), the pack rt's verb, mutants and families; ORS30 (a), LLVM IR's oracle tier (branch `llvm-rt-pack`, from next 7696e50da)

The user decided LLS31 (a) on 2026-11-07: rebuild LLVM IR's first-record lists (the 282 modules and their mutants, whose clang-made
inputs were lost with the removed worktrees). LLS31 (b) had retired the four families from the read cells, `:role :beside`, meanwhile.

**What the inputs were.** The ids of `test/llvm/check-roundtrip-linux-amd64.edn` were reread:
- 41 torture programs (the plan said 42). Five of them exist only in later gccs, so the source is gcc 15.2.0's
  `gcc.c-torture/execute`, from the tarball C's crop pins (`test/c/corpora.edn` `:gcc-15.2.0`), where all 41 are.
- 9 C++ samples, Rust's `shapes.rs` (three builds), 10 llvm-stress seeds, 21 corner modules (sources lost, dropped) and the four
  committed `hw-*`.

The mutants' names give their kinds and indices:
- fitted: 0-339 and 1000-1339 of the base family's 17 kinds;
- held-out: 500000-500599 and 600000-600599 of all 41 kinds.

`arbace.repr.llvm.mutate` today assigns exactly those kinds to those indices: `k mod |kinds|`, checked on the record's names. So the
mutator needed no new kinds, only these sets.

**Built.**
- `bin/llvmi rt-list` writes `test/llvm/rt-builds.tsv`: 257 modules.
- `bin/llvmi ir-make rt [--mutants]` builds the pack `test/llvm/ir/rt.{tar.xz,tar.xz.sha256,tsv,cmd}`: the builds by clang, clang++
  (Mach-O on `cpp_full_sysroot`'s headers), rustc and llvm-stress, all with prefix maps and `-fdebug-compilation-dir=.`. It then makes
  Arbace's mutant sets a, b, h1, h2, plus three single mutants of `hw.list` (ctl, ctlh, ctlu) as the controls' subjects.
- The mutator's cache is bounded to four modules. The output is unchanged and the peak is halved.
- `bin/repr-llvm check` has four new families, `roundtrip-rt`, `text-rt`, `verify-rt` and `verify-rt-heldout`, with crossed-pair
  controls. `bin/repr-llvm rt-lists` writes their lists.
- Reader fix: LLParser's `parseParameterList` gives a metadata argument no attributes ("expected metadata operand"). A held-out mutant
  of the subset found it against live opt.
- reap: the four families are `:until` sources; `matrix.edn` was re-recorded, 0 problems.

**Proved** on 17 modules and 83 mutants (light):
- two builds from scratch give the same pack byte for byte;
- roundtrip 17 of 17 and text 17 of 17;
- verify-rt 62 of 62 and heldout 42 of 42 against live opt 23.1.5 (after the fix);
- the controls fail as they must;
- llvm/test's four families are unchanged.

**ORS30 (a).**
- `bin/repr-llvm records-verify`: the records against the live tools, with `moved`, `address`, and a crossed pair.
- The oracle steps `llvm-live` (roundtrip-llvmtest and verify-probes live) and `llvm-records-verify`, from
  `test/gate/crops/llvm-oracle.edn`, spliced by hand. `bin/gate --oracle --plan` plans them.
- Not taken: verify-llvmtest live. Its record is the own verdicts, and opt's whole stderr differs on 30 of them.

**Alternatives considered.**
- Making the mutants at check time: not taken. Opt's record is keyed by the mutant's digest, so every change to the printer would make
  them stale.
- gcc 9.1.0's torture with the mirror snapshot: not taken. It covers 39 of the 41 programs, against all 41 in the pinned gcc 15.2.0.
- The host's libstdc++ headers for the Mach-O builds: not taken in favour of the tree made without g++.

**Left for the night.** The pack's full build, the opt and rt records, and the four families' `--record`. Then by day: the cells' switch,
independence, and the gate parts. The commands are in doc/llvm/LLVM-CROP-SURVEY.md, "As built: LLS31 (a)". The proposals there are
LLS33 and LLS34.

## 2026-11-08: RF10's first slice, MIR building from the native front end (branch `rust-rf10`, from next bd33e27cc)

The user's order of 2026-11-07 (RES54 (a): RFP20, RFP15, then RF10): RFP20 (captures in the helper's `--typeck`, upvar.rs ported in
full, 2-3 days) and RFP15 (`mod m;` loading, a few days) are not small and neither is read by a first slice on single files without
closures, so RF10's first slice came first; both stay next (doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (first slice)", RFP22).
Sources: rustc 1.99.1 (`58cf8f9c3`, Alpine's rust-src-1.99.1-r0 apk pinned by tools/rsdump/apks.sha256), read in
`compiler/rustc_hir_analysis/src/check/region.rs`, `rustc_middle/src/middle/region.rs`, `rustc_mir_build/src/thir/{cx,pattern}/`
and `rustc_mir_build/src/builder/{mod,cfg,scope,block,misc}.rs`, `builder/expr/`, `builder/matches/{mod,match_pair,buckets,test}.rs`,
ported by hand under MIT into `arbace/rust/front/build.ae` (region scope tree, THIR, scopes and drop trees, expressions, the match
lowering); typeck keeps its results by HIR id for it. Two fixes of earlier stages: a `while`'s `break` an expression statement (typeck
coerces it to `()`), path patterns peel references; typeck-check and hir-check unchanged. Judge: `bin/rust-front build-oracles`
(rustc's `-Zdump-mir=built` by the helper as rustc) and `build-check` (our pipeline's bodies printed by arbace.rust.front.mir against
rustc's text byte for byte): test/rust/front's seven no_std samples 47 of 54 bodies equal and one more but for rustc's allocations text
(a1, a2, a4, a5 whole); the corpus of 88 programs 92 of 510 equal and 4 more, five programs whole, the refusals classed (closures,
desugarings' lang item calls, overloaded operators, constants' bodies). Run: `bin/rust-front run-built` puts our bodies, after
CleanupPostBorrowck's changes and the abort strategy's unwinds, in place of the helper's `--mono-mir` record's bodies (the record keeps
the symbols, std's callees and the layouts; unit's, scalars' and scalar pairs' layouts it lacks computed after rustc_abi) and runs them
through RF3's codegen, rsi's lowering and gcc with rustc, rustdoc and cargo stubs exiting 99 on PATH: a1-a5 and a7 give rustc's
statuses (5, 212, 41, 17, 45, 21), every body ours, the stubs' log empty; the control (Add as Sub) differs. Alternatives considered:
building MIR from the helper's typeck data (RFP16 (b), rejected earlier: the pipeline is the goal), judging at runtime MIR only (rejected:
a difference far from its cause; built MIR is the builder's own output), running the std programs by run-native (rejected here: it
links through rustc as the linker driver, which the stubs forbid). `bin/crops-score` Rust 75.0 / 95.8 / 80.0 before and after: under
RES54 (a) eval and AOT stay 0.5 while std's records are consumed, until RF12. Proposed: RFP22 (the order after the slice), RFP23 (the run's
record as packs), RFP24 (no gate step yet).

## 2026-11-08: SCS4 (a), SCS5 (b), SCS6 (b), Scheme's probes by the report and Larceny's programs by golden outputs (branch `scheme-scs4`, from `mac/scheme-scs3` d026b47f1)

The user's decisions of 2026-11-08 (about 17:00) on scheme-scs3's proposals. SCS4 (a): si keeps Chez's semantics for the five Racket
tests it fails by design; nothing built, the three classes' `:decided` name it. SCS5 (b): `arbace.repr.scheme.report`, R6RS 4.1-4.3's
lexical and datum grammar written from the report rule for rule, apart from the reader (which reads as Chez does), its numbers by exact
arithmetic and its own correctly rounded binary64 conversion (`nearest-double`), its outcome the data, a syntax violation or
"undecided" where the report leaves the reading to the implementation (`#!<identifier>`, `#e` on an infinity, an exact ratio over zero,
a mantissa width below 53 or with `#e`, a complex of mixed exactness, a polar number but for an exact zero angle: 72 probes). The read
check judges each probe it decides by it (`check/judge-probe`, `report-diff`: data, offsets, count, error and its class, write then read
back), its dump in the pack unread: 1,140 of 1,249 probes. `bin/repr-scheme report-check` (by hand, from the recorded dumps) reads every
probe three ways: 1,140 agree, and the 37 where Chez's R6RS mode reads past the grammar are named in `test/scheme/report-chez.edn` (upper
case `+INF.0`, a mantissa width outside radix 10, a `#` after a prefixed number) and judged by their dump, as the reader stays Chez's
(SM1). Checks of the judge: `report-test` (17 numbers of known binary64 or exact value, at every check) and a control family
`:read-report` (6 controls and a sanity entry; `bin/si corpora controls-test` 35 ok). SCS6 (b): golden outputs of Larceny's 13 programs
(`test/scheme/si/golden/larceny/`, from si's runs, reviewed line by line against R6RS: doc/scheme/SCHEME-SURVEY.md, "As built:
SCS4-SCS6"), with `:status` and `:error` (the error's kind) for the five that end with an error; `golden-items` checks the kind too
(`error-differs`), `judge-test` three more cases; no program reads Chez's run at check time; the class `chez-conditions` left
(larceny/exceptions passes: its golden states si's R6RS conditions). Alternatives considered: the report's verdict on the 37 with
Arbace's reader changed to the report's (rejected: SM1 makes the reader Chez's, and SCS4 (a) keeps Chez's semantics), a regex in place
of the 37 ids (rejected: the regexes matched 12 more probes where both readings are errors), the corpora's files by the report too
(measured, 106 of 109 essential files agree, proposed as SCS7: SCS5's recommendation kept them on the dumps).

Checked: scheme-read-essential's command (1,358 files and probes, 1,140 by the report, controls, report-test, round trip, spec-check),
`bin/si check` and `--aot` on their essential subsets (no Chez run read, no regression, the controls differ) and on Larceny's 13 with
`--record --only`, `bin/si own` (49 of 49 judged without Chez), `bin/repr-scheme c2j-check` (19 of 19), `bin/reap --record`, `bin/crops-score
--record` and `--judge --strict` (0 problems). Scheme's scores: completeness 96.8 to 97.3, maturity 100, independence 97.5 to 99.6 (judge
0.876 to 0.979). Night: `bin/repr-scheme check --record`, `bin/si check --record`, `bin/si check --aot --record`. Proposed: SCS7 (the
corpora's files by the report), SCS8 (Larceny's earley and nboyer run as their authors meant, their own result checks).

## 2026-11-08: repr/go's printer groups names only of one type, the type metadata included (branch `go-tests`, from 1f397fd0e)

Found today by `bin/jrt test` (go test ./... over go/ printed in the gofmt layout): `go/arbace/expat` did not build. The printer
regroups consecutive names (N3: the converter keeps one entry a name) when `groupable?` (arbace/repr/go/print_code.ae) finds their
types equal, and it compared them with `=`, which ignores metadata; a func type's parameter types are the names' `:tag`, so
`(func [enc fromP fromLim toP toLim] ...)` with `toP *Ptr` and with `toP *Ptr16` were equal and `ENCODING`'s `utf8Convert` and
`utf16Convert` printed as one field `utf8Convert, utf16Convert func(... *Ptr, ... Ptr)`. Mode `:full` was never affected (a group
there also needs the types' one recorded position). Fix: `arbace.repr.go.types/same-type?`, the forms `=` and, at every level,
their type metadata equal (`:tag` compared again by same-type?, `:go/tag`, `:go/grouped`; positions, lines and comments not),
used by the printer's `groupable?` (struct fields, parameters, results, type parameters: every list `group-entries` groups) and by
the converter's `mark-ungrouped` (A1's `:go/grouped false` in generic declarations, which compared the tags with `=` too: there it
only added a redundant marker). `go/var` and `go/const` specs need none: a spec keeps the names its source spec declared. As
go/printer prints the source's own field groups, a regrouping may only join what the source could have written as one field.
Alternatives considered: comparing the printed text of the two types (exact but a print per pair), comparing all metadata but a
list of position keys (any new layout key would silently split groups); the whitelist names what is part of a type (SPEC §5).
Tests: case `test/repr/go/cases/s05_type_identity` (expat's two fields, named results, struct tags, parameters and results of a
function; each with its negative, equal types grouped) and test `type-identity` in `test/repr/go/print_test.ae` (same-type? on
the exact forms and the negatives; the gofmt layout equal to the original byte for byte); with the old printer the case fails
gocmp's tree, export and code levels. `bin/repr-go test print`: 281 assertions, 0 failures; `test read`: 174, 0 failures.
expat and pyexpat printed in the gofmt layout: go vet clean, go test passes (expat's tests; pyexpat has none), and oi's with its
testdata copied in; so `bin/jrt test`'s exclusion of 1f397fd0e is undone (`./...` again), print_module copying
`test/ocaml/oi/testdata` as `bin/oi test` does. Printed output: a scan of every pair of consecutive tagged names whose tags are
`=` but not same-type? (the only pairs whose grouping can change) over go/, overlay/go, box/, x509roots/, tamago/goroot, goos and
cmd, and the program's j2g forms (the main checkout's `target/arbace-go/j2g`, 4,157 files): 5 pairs, all in go/arbace/expat and
pyexpat (crops' paths); none in the runtime or the program. Ten std packages (strings, sort, bufio, encoding/json, go/ast,
text/template/parse, net/textproto, image/png, net/http/httptest, flag) printed in the full, gofmt and lines layouts by the old and
the new printer: identical, and the full layout equal to the originals byte for byte; slices, maps, sync/atomic and iter converted
by the old and new converter: identical forms. The whole runs are the night's (the report lists them).

## 2026-11-08: the essential memory lane (branch `gate-lane`, from next bd33e27cc)

By the user's decision of 2026-11-08, the essential gate's big steps are admitted by a memory lane rather than serialized.
On both of the nights 2026-11-07 and 11-08, diffcheck's j2g was OOM-killed when diffcheck, java-compiled, suite-aot-essential and
programs ran at once: peaks 21, 17, 14 and 10 GiB on the 62 GiB host. They are `:heavy :inner`, and the night's gate holds
the big lock with `GATE_HEAVY_CMD=env`.

Built (doc/GATE-SURVEY.md, "As built: the essential memory lane"):
- `bin/gate-lane`'s free mode. Each step's `:mem-gib` is set against MemAvailable less 2 GiB. The running steps hold back
  their declarations less their process trees' RSS. A step is admitted when it fits, or alone. A big step (16 GiB or more)
  is also admitted when no big step runs. The head big step holds room back when even the free memory cannot hold it. A
  small step that does not fit holds nothing back.
- `bin/gate`'s default without `--concurrent` and outside the clean run (`GATE_LANES=free`; `GATE_MEM_LANE=0` off).
- The runner's `step`: the lane first, then the heavy lock as before. The lane's wait is reported apart, and the summary
  flags a peak over its declaration.
- `test/gate/lanes.ae`: `decide-free`, and the free view in `bin/gate --plan`, with a serial `--full` simulated as run,
  printed against the big steps one at a time and against every step at once.

Alternatives considered:
- Serializing the big steps (the heavy lock around them whole): the essential checks take about 31 minutes against the
  lane's 24.
- GTS227 (a)'s fixed budget with one big step at a time: it cannot run diffcheck and java-compiled together (53 GiB), and
  it does not see other processes.
- Holding back the blocked small steps too, as GTS227 (a)'s conservative backfill does: in the simulation it stalls every
  light chain behind a waiting big step.

Fixed on the way: the runner's sampler. busybox awk stops at a `/proc/PID/stat` that vanished between the glob and its
read, which lost 611 of 3,848 samples' step trees in main's last gate. It now reads through `cat`, and so does the lane's
RSS. Proved on samples: `test/gate/lanes-test.bash`, 38 checks (17 new), passing plain and under strace; `bin/gate-tests`; and
`bin/gate --plan` and `--full --plan`. The essential plan simulates to 0:46 at 54 GiB free, against 0:53 with the big steps
one at a time and 0:42 at once; `--full` serially to 9:54, against 10:01 and 9:51. The runner's change rekeys every check:
each runs once on the next night. Proposed: GTS248-GTS251.

## 2026-11-08: GTS248 (a) and GTS249 built, GTS250 (a) and GTS251 (a) recorded (branch `gate-lane2`, from `gate-lane` 9da94ac0e)

Late on 2026-11-08 the user accepted "As built: the essential memory lane"'s four proposals: GTS248 (a), GTS249 (a) for
`--full --concurrent` and (b) for the clean run, GTS250 (a) and GTS251 (a). The last two need no code. Built as
write-ahead (doc/GATE-SURVEY.md, "Decided (2026-11-08, the user): GTS248-GTS251" and "As built: GTS248, GTS249"):

- **GTS248 (a)**, `bin/gate-lane`'s free mode with `GATE_LANE_HOST_DIR`. The state is kept for the host, in the main
  checkout's `.tmp/gate-lanes` beside the store of passes; a store of the tests' own gets its own directory. Each entry is
  named `GATE-ORDER-NAME.PID`, GATE being `%010d%07d` of the gate's start and its PID (`GATE_LANE_GATE`), so the earlier
  gate's steps sort first. The host's `events.tsv` carries the gate in a seventh column, and each run keeps its own
  six-column events. The step is not handed the host's state.
- **GTS248's switch.** `bin/gate` turns it on with `GATE_LANE_SHARED=1`. It is off by default, by the user's condition,
  until a night has proved the lane. Turning it on is one line, `${GATE_LANE_SHARED:-0}` becoming `:-1`, plus
  lanes-test's grep.
- **GTS249.** `bin/gate`'s `lane_rule`: `--concurrent` is free by default and exports `GATE_LANE_MODE=free` to the
  runner's unchanged GTS227 (a) branch; `GATE_LANE_RULE=budget` brings back the fixed budget. The clean run stays on the
  budget by default, and `GATE_LANE_RULE=free` (passed into the clone) switches it. Its record's `:lanes` names
  `:rule`. `--plan` prints the rule and the state.

Found and fixed on the way, in `gate-lane` itself: a step admitted by the lane inherits `GATE_LANE_MODE=free`,
`GATE_LANE_DIR` and the other lane variables, because `bin/gate-lane` execs the step. Run as `gate-tests`' step under
tonight's essential lane, `lanes-test` would have taken its budget cases for the free rule. Reproduced by hand with a
step's environment: 9 of 38 checks failed. `lanes-test` now unsets the lane variables first.

Also caught by `gate-reads-test` (the whole of `bin/gate-tests` under strace): lanes-test's check that `bin/gate` refuses a
third `GATE_LANE_RULE` first ran `gate_env`, which hashes the checkout for the gate's key. That read about 6,100
undeclared files. The refusal now comes before the environment, in 0.01 s, and reads nothing.

Alternatives considered:
- For the state's place, a variable naming the main checkout directly. Rejected for the store's own directory, which
  keeps the gate's tests out of the real state by construction.
- For the order, the gate's start alone. Its PID was added so that two gates started in the same second still sort
  apart.
- Changing the runner to pass the host's directory. Rejected: the runner is in every check's key, and the environment
  needed no runner change, so nothing is rekeyed beyond what `gate-lane` rekeys already.

Checked (light, no gate):
- `test/gate/lanes-test.bash`: 46 checks, 8 new, passing plain and with a lane step's inherited environment.
- `bin/gate --plan`: 0:46, unchanged, with either state.
- `bin/gate --full --plan`: 9:54.
- `bin/gate --full --concurrent --plan`: 6:22 on the free rule against 6:58 on the budget, at 48.7 GiB free.
- `--full --clean --concurrent --plan`: 7:02 on the budget, and 6:11 with `GATE_LANE_RULE=free`. That plan declares up
  to 73 GiB at 52 GiB free, which is GTS255.
- `bin/gate-tests`: 35 of 36 before the refusal's fix, gate-reads-test failing. After it, `gate-reads-test`, which runs
  the whole of `bin/gate-tests` under strace, passes in 6 minutes: the canary is seen and every read is declared.

Proposed: GTS252 to GTS255.

## 2026-11-08: GTS252-GTS255 decided, GTS255 (b) built (branch `gate-lane2`, from 2acb2463d)

The user accepted GTS252 (b) now and (a) with the next runner change, GTS253 (a), GTS254 (a) and GTS255 (b), as recommended
(doc/GATE-SURVEY.md, "Decided (2026-11-08, the user, later): GTS252-GTS255"). GTS252 (a), GTS253 (a) and GTS254 (a) wait (the
next runner change; the first clean run in the lanes). GTS255 (b) is built ("As built: GTS255 (b)"): `bin/gate-lane` and
`test/gate/lanes.ae` only, so no key moves. Under `--concurrent` (`GATE_LANES=1`, in the plan the view's title) a big waiter
must fit ROOM like every other step; the essential lane is unchanged. `GATE_LANE_STRICT=1|0` overrides, for the tests. The
brief's `GATE_LANE_MODE=free-strict` was not used: the runner tests the mode `= free` and is in every key. Alternatives: a
fourth mode in gate-lane (rejected, the same reason); strictness from `GATE_LANE_RULE` (rejected, it is bin/gate's, not
visible to the step). Checked: lanes-test 50 checks (4 new), 0 failed; `bin/gate --full --concurrent --plan` 6:41 at 53.8
GiB free (non-strict 6:48 at 51.7: free memory moved), peak declared 53 (the java-interp steps, alone).

## 2026-11-08: C++'s runtime by our route, libsupc++ (branch `cpp-runtime-own`, from next bd33e27cc; CP240 (c), CODEGEN.md G5c)

By the user's decision CP240 (c) (2026-11-07): libsupc++'s 63 C++ units of `test/cpp/runtime/units.edn` (gcc 15.2.0's
`libstdc++-v3/libsupc++` and `config/cpu/generic/atomicity_builtins/atomicity.h`, from the pinned tarball) compiled by
Arbace's route (the native parser, `arbace.cpp.sema`, `arbace.cpp.codegen`) instead of clang's IR. cpp-90 had 41 of 62
generating; a names level written first (each unit's external names against the oracle's IR from the pack
`cpp-runtime-ir`, both ways) showed "generated" meant little: eleven `eh_*` units generated empty (a quoted `"unwind.h"`
not found, the preprocessor's error ignored), no unit emitted its key function's vtable, type_info, D0 or C1/D1. The fixes,
each a rule of the standard, the Itanium ABI or clang's -O0 code generation as its IR shows it (doc/cpp/CODEGEN.md G5c.1's
table): using-directive cycles, restrict references, the `__atomic` builtins, the bit builtins, `__builtin_offsetof`,
const-int's casts and unsigned compares, preprocessor errors fatal, typedef names for linkage, GNU's `mode`, `used`,
`noreturn` and `aligned` (on a class), `thread_local`, qualified definitions of extern variables, a qualified member's body
looked up in its namespace, key functions (vtable, VTT, type_info strong where defined), C1/D1 and D0 of defined
constructors and destructors, out-of-line `= default`, the fundamental types' type_info, internal linkage of unnamed
namespaces' classes, no code after a terminator, `__cxa_end_catch` an invoke when it may throw (terminate pads while
unwinding), `__cxa_guard_abort`, `__cxa_bad_typeid`, a static member function template's call in a template argument and
its mangling. Three sema gaps of the branch's base on today's tree were fixed on the way (the native parser's
CXXDependentScopeMemberExpr, a reference dynamic_cast's value category, `:delegatingInit`: 11 codegen samples failed at
bd33e27cc).

`bin/cppi runtime-build` takes `CPPI_RUNTIME_ROUTE=ours|auto` (`CPPI_RUNTIME_OURS`, default `libsupc++/`), the oracle's IR
kept as `clang.ll`, the names level in `runtime-judge`, the route in the runtime's key; `bin/cppi runtime-swap` (new) proves
the runtime by our route unit by unit: codegen-check's programs (by clang's IR and by ours) linked with the runtime built
from clang's IR, then with each unit by our route alone swapped in, then all of them; every status and stdout the
baseline's. It found what the names level could not: `__cxa_refcounted_exception` 8 bytes short (GNU's `aligned` on
`_Unwind_Exception` ignored), harmless while every unit agreed, a crash when eh_ptr.cc by ours met eh_throw.cc by clang's.
Results (doc/cpp/CODEGEN.md G5c.4): 62 of 63 units by our route, names equal; runtime-build 198 of 198 with 62 by our route, also from the records with clang, gcc and the LLVM tools stubbed (their log empty); runtime-swap: no run of 102 programs changes with any one unit swapped, none of 298 with all; codegen-check on that runtime 148 of 149 (hdr-vector known); native-check samples 9 of 9, native 1 of 1.

Not by our route: `eh_alloc.cc` (C++17's `std::pair<std::string_view, int>` constructors: their enable_if conditions still
false; it keeps the oracle's IR under `auto`) and libstdc++-v3/src's 136 units (P22). Sources: the Itanium C++ ABI (§2.5,
§2.9.4-§2.9.5, §3.3.2-§3.3.3, §5.1, §5.2.3), ISO/IEC 14882:2024, GCC's manual (the attributes), libsupc++'s sources read as
the program compiled; clang's IR (the packs) as the oracle; no clang code ported. Alternatives considered: proving by the
IR's text against clang's (rejected: the routes' IR differs in names of temporaries and in clang's attributes everywhere;
the names level and the runs are the contract); the swap over every program for every unit (18,000 links; the per-unit
runs take the 102 programs (51 samples by both routes) of exceptions, RTTI, guards, allocation and headers, and the whole swap every program).

## 2026-11-08: C++'s completeness: the dg harness's DejaGnu rules and the own judge's failures (branch `cpp-compl`, from mac/cpp-runtime-own 9681152c4)

The brief: C++'s `bin/crops-score` completeness 76.1 (96.5 without by design); fix the largest deferred classes' causes, review the
by-design ones and propose. The gap's shape (test/reap/matrix.edn): read's deferred 77 were the dg harness's (prune-output 51,
gxx-disagrees 19); print's 1,665 by design are the clang judge's own blind spots (oracle-rejects 1,584, gxx-objects-location 81),
which the decided own judge (CP243 (a)) judges once its night records exist, so the print work was the own judge's failures.

The dg check (arbace.repr.cpp.dg), against gcc 15.2.0's testsuite library as the source of the rules (gcc/testsuite/lib/gcc-dg.exp
lines 398-403, gcc-dg-prune; lines 1226-1283, process-message; target-supports-dg.exp, dg-require-dll and -ifunc): dg-prune-output
applied as gcc-dg-prune's regsub after the directives took their lines (cpp0x/initlist19.C, `dg-prune-output "error"` with a passing
dg-error, decided the order; the first version pruned first and lost six passes); a line-numbered directive matching an error of any
file (DejaGnu's pattern names no file: cpp0x/deleted16a.C); a message's later top-level branches matched on any diagnostic at the line
(process-message splices the message as text: cpp0x/defaulted10.C's `previously|declared` on g++'s note); dg-require-dll and -ifunc
false here (g++ 15.2 on musl: "'ifunc' is not supported on this target"); `--embed-dir` for `#embed __FILE__`; the class musl-libc
(musl's NULL is nullptr, measured; musl's iconv writes `*` for an unencodable character, measured with a C program). g++'s
reference re-recorded for the 167 files affected and their rows recorded (`bin/repr-cpp dg --only --record-ref`, `--judge --record`):
10 more passes, prune-output 0, gxx-disagrees 2; the every-40th sample unchanged. Considered and not applied: dg.exp's directive grep
(white space before the closing brace), which cpp1z/fold1.C supports and cpp1y/udlit-char-template-sfinae-neg.C contradicts; dg.exp
itself is not in the tarball, so the rule stays as it was.

The own judge's runs (`roundtrip-corpus KEY --judge own --only-list`: the known file's deferred files of each key, the gxx-dg
gxx-objects-location files, every 5th oracle-rejected file of gxx-dg-every-10 and every 40th other, old-deja's alike): 13 failures,
each fixed in the printer or the converter by the grammar's rule (a bare switch's items, a unary operand a cast-expression, a pack
expansion's pattern an assignment-expression, `< ::` against C++98's digraph, a template's text from its outermost header, a
typedef's leading standard attributes, a macro-written for-init, attributes before `auto`, a statement's `;` after a comment, an
argumentless explicit specialization, a type-id's pack expansion); rtcheck lets a known file of a level the own judge does not run
take its own levels' class; `bin/repr-cpp dg --rt-judge own` records the modules keys (cpp-90's night list named
`roundtrip-corpus gxx-modules-all`, which has no corpus). After: 63 of 64 deferred files, 48 of 48 objects files, 176 of 176 and 24
of 24 of the samples, the essential list 33 of 33 (recorded), the samples 9 of 9 with the control failing.

Scores: completeness 76.1 to 76.2 on the records (read 80.6 to 80.7), about 88.5 expected after the night's own records (print to
about 98). Proposals CP260-CP265 in doc/cpp/CPP-SURVEY.md, "As built: cpp-compl" (UNSUPPORTED as :na; the token check by clang's
tokens where g++ -E is no reference; the dg check's clang-* classes; oracle-rejected under our route; threads; the macro extent).

## 2026-11-08: C++'s CP247 (a), CP248 (a), CP249 (a) built (branch `cpp-cp247`, from next 5af7d0058; write-ahead)

By the user's decisions of 2026-11-08 (~17:00), under the day's light rules (doc/cpp/CPP-SURVEY.md, "As built: cpp-cp247").

- **CP248 (a)**, the clang half of Arbace's C++ target: `test/cpp/clang.ae` ports clang 23.1.5's InitPreprocessor.cpp (the standard,
  feature-test and predefined macros with their helpers), X86.cpp's getTargetDefines (the baseline CPU) and OSTargets.h's Linux defines from
  the pinned llvm-project-23.1.5.src.tar.xz (SHA-256 fb50b1ea...6a6e, test/cpp/corpora.edn; Apache 2.0 with LLVM Exceptions, LICENSE.md),
  over C's c/target/abi.edn and the new cpp/target/clang.edn (TargetInfo's facts, Alpine's driver defaults, PickFP's tables, the __has_
  tables, the search list). `bin/repr-cpp target gen` writes predefined/clang/, search-clang.txt and has.tsv's clang column; host-oracle 37
  of 37 files equal to test/cpp/host (clang's 16 byte for byte; a mutated clang.edn fails). The native parser, ppcheck --clang and the
  sysroot read the target in place of clang's record (REPR_CPP_TARGET=gxx: the record). native-check samples 68 of 68 items as before.
  Alternative considered: CP248 (b), the native parser on g++'s answers (rejected by the decision: clang's AST was made with clang's macros).
  The proposal's estimate of "about 60 lines of differences per standard" was low: about 250; the port computes all of them.
- **CP247 (a)**, the dg-error part back in `--full`: tools/cxxdump's records mode (mac/oracle-records-cpp2's 4ec2da932 and e63d37180: in
  next's history, but their tools/cxxdump, bin/repr-cpp record-dumps and the roundtrip's records mode were lost from the files by a later
  merge; only test/oracle-records/cpp.bash's cpr_dump_dirs remained) restored, with the g++.dg tree keyed as @TREE@; `bin/repr-cpp
  record-dumps dg-error`, `verify-dumps`, `cxxdump-ensure`; the dg script reads the pack unless REPR_CPP_LIVE=1; cpp-dg gains the part
  dg-error (no pack yet: "no record yet", skipped), cpp-dg-oracle runs live. Proved on a 23-file sample pack (not committed): equal to a live
  run, tree-independent, a missing entry fails. The round trips' records mode was not restored: their clang levels are the oracle tier's since
  ORS31 (a), read live there.
- **CP249 (a)**: as now, nothing built.
- Scores unchanged (76.1 / 100 / 67.7; the dg cases a record either way, the native parser's record in no counted case); independence.edn's
  C++ read fact rewritten to say so. Proposed CP250 (clang's option deltas), CP251 (the dg pack's dependencies on the host's headers).

## 2026-11-08: C++'s accepted proposals CP260-CP265 and P20-P22 built (branch `cpp-props`, from mac/cpp-compl f070d5247; write-ahead parts)

The user accepted (2026-11-08, about 19:45) cpp-compl's CP260 (a), CP261 (a), CP262 (b), CP263 (a), CP265 and cpp-runtime-own's P20 (b),
P21 (b), P22 (b), and decided CP250 (a) when needed, CP251 (a) at the next re-record, CP264 a night task (one job). mac/cpp-cp247 (4ddab9d59)
merged first; cpp-cx12 (CX12, still running, `arbace/cpp/sema.ae` and `arbace/repr/cpp/dg.ae` its) not merged: CP262 (b)'s diagnostics are
its, the read cell's classification here. Details in doc/cpp/CPP-SURVEY.md, "As built: cpp-props", and doc/cpp/CODEGEN.md G5c.9.
- CP260 (a): dg-unsupported (C++'s and C's) and the token check's not-cxx and target-option `:kind :na`; bin/reap's per-file merge prefers
  a counted class to `:na`. Alternative left: as now (by design, in the strict denominator).
- CP261 (a): the token check judges a file g++ -E refuses (not for another target's option) or rewrites (-fmodules-ts) by clang's raw
  tokens alone (`clang-tokens`), its control `_control/pp-clang/empty15-vs-cpp-11`; 27 files pass, their known entries gone, their rows
  recorded by `pp-check --only --record-results` (new: a subset's rows replaced, each already recorded).
- CP262 (b), the read side: `arbace.repr.cpp.dgsyntax` lists the 234 dg-error files expecting only syntax errors
  (`test/cpp/dg-syntax-only.txt`); bin/reap's `:own-for` takes their verdicts from CX12's own record once it exists, a failure the class
  `syntax-own` (deferred). On cpp-cx12's sample record 27 of them are judged, 12 pass, 15 fail (the cell 10 passes lower): the whole record
  will lower the read cell by about 85 passes, the cost of judging them by the native front end. Alternative considered: taking an own
  pass only (CX12's CP271 (b)), which would not be CP262 (b)'s judgment by the native parser.
- CP263 (a), write-ahead: `ours-refused` (deferred) for a program our route refuses and clang rejects under CPPI_ROUTE=auto;
  route.bash keeps gen.refused across ir1. In effect once CP240 (b)'s switch makes auto the default.
- CP265: a macro's extent runs on through the arguments its expansion's unbalanced `(` takes from the text after it (the main file's
  definition read); delimited-escape-seq-5.c passes the own round trip; 82 macro files of the samples and the essential lists unchanged.
  A header's macro is not seen (the shared preprocessor's expansion would be the full answer).
- P20 (b): clang's resource directory made from the pinned llvm-project-23.1.5 tarball as clang/lib/Headers/CMakeLists.txt installs it
  (its set() lists and install(FILES) commands read as data, cpp/target/clang.edn's :resource-include): 285 of the 296 installed files,
  all equal byte for byte; the 11 TableGen products for ARM, AArch64, RISC-V and HLSL not made (named in :not-made). The runtime's units
  by our route read clang's unwind.h (no -iquote of gcc's): the 13 eh_* units our route generates have the oracle's names. Source read:
  llvm-project-23.1.5.src.tar.xz (sha256 fb50b1ea...), clang/lib/Headers (Apache-2.0 with the LLVM exception, LICENSE.md's LLVM entry), files
  copied into the generated tree under .tmp/, nothing committed of them.
- P21 (b), write-ahead: bin/cppi's `cppi_runtime_route_default` (clang until the night's runs hold, then auto); the oracle tier's
  `cpp-runtime-swap-oracle`.
- P22 (b), the first measure: `arbace.cpp.codegen gen-batch` over the 103 units of c++98/ and c++11/ (27 min, one process): 52 generate, 13 with the oracle's
  external names exactly (functexcept, functional, globals_io, cxx11-stdexcept, regex, mutex, snprintf_lite, parallel_settings and five
  with none), 4 with one or two extra names (a terminate pad clang does not need, two inlined chrono constructors), 35 without the explicit
  instantiations they exist for (G5b's); 51 refused (parse 11, name 10, type 7, string 6, the rest small). The measure's lists and logs in
  the worktree's .tmp/p20/; P22 (a) next.
- Checks on the affected cases only (stubs exiting 99 first on PATH, their log empty, but native-check's oracle): pp-check --only (29) and
  --essential; the own round trips gxx-dg-essential 33/33, samples 9/9, the 57 + 25 macro files; native-check samples 9 same, native 1;
  runtime-build of the eh_* units by our route (records mode); `gate.crops check` 0 problems; `bin/gate --oracle --plan`; bin/reap --record
  (0 problems); crops-score --record, --judge --strict (0 problems). Scores: C++ 76.15 / 100 / 67.7 to 76.4 / 100 / 67.7 (read 80.7 to 81.6,
  36 cases n/a), C 95.1 (8 n/a).

## 2026-11-08: C++'s CX12, first slice: Arbace's own front end's diagnostics for the dg-error part (branch `cpp-cx12`, from mac/cpp-cp247 4ddab9d59)

By the brief of 2026-11-08 (Sema's diagnostics natively, so that the dg-error cases are judged by Arbace's front end), under the day's light
rules: samples only (every 10th file of the part, 454 supported), no clang or gcc in the own runs (stubs exiting 99 first on PATH, their log
empty). doc/cpp/CPP-SURVEY.md, "As built: CX12 (first slice)".

- **Measured first**: the native parser on the sample with its declines as syntax errors, judged by the dg verdict: 38 of 454 pass (clang's
  record: 3,359 of 4,566 corpus-wide). The expected error lines grouped by the dg-error's message and, from clang -fsyntax-only's outputs on
  the same runs made by hand for the measure (the oracle tier's comparison), by clang's diagnostic on the line: "no matching function"
  109, "call to implicitly-deleted copy constructor" 86, "use of undeclared identifier" 51, ... 209 lines with no clang error at all
  (g++'s own). 400 of the 416 failures were missed errors, not extra ones: the front end accepted too much.
- **Built**: `arbace.repr.cpp.diag` (the diagnostics: the preprocessor's, the parser's syntax errors at the failing token through a new
  parser slot DIAGS, Sema's `undeclared` and `redefinitions` over the final tree, and the code generator's Sema, arbace.cpp.sema, as a
  checker through two hooks, `*at*` and `*on-refusal*`, a refused function or statement reported and the analysis going on, its refusals
  that are Sema's own errors mapped by `sema-rules`); `bin/repr-cpp dg --front native` and the dg verb `own` (each file by its own dg-error
  lines, the record test/cpp/check-dg-own-linux-amd64.edn, regressions and the dg control judged); the gate part `cpp-dg.dg-error-own`.
  The messages are clang's texts (DiagnosticSemaKinds.td) and the rules follow SemaExpr's DiagnoseEmptyLookup and SemaDecl's redeclaration
  checks, from the pinned llvm-project-23.1.5 tarball (LICENSE.md, doc/ARBACE.md; the file carries clang's notice).
- **Alternatives considered**: checks run during the parse (rejected: the tentative parses roll back, a check must see the committed tree);
  a checker of its own beside the code generator's Sema (rejected for now, CP272: the code generator's analysis already decides overloads,
  initialization and members, and a gap it shows is one the code generator has too); the declines alone as errors (CP242 (b): kept, as
  one source of four).
- **Checked**: the redeclaration rules against clang on two probe files (19 of 19 errors at clang's lines and columns); the sample by
  `bin/repr-cpp dg --corpus dg-error --front native --sample 10 --record` with the compiler stubs first on PATH: 64 of 454 own passes (38
  before), 6 of them passes clang's record fails (read: genuine), the dg control failing as it must on dr2082 and dr1423; `make
  check-makefile`, `gate.crops check`, `bin/gate --full --plan` (`cpp-dg.dg-error-own` planned).
- **The measure's commands** (by hand, the oracle tier's comparison): `arbace_tool -s arbace.c.dg -m arbace.repr.cpp.dg lists WORK
  $GXXDG dg-error - 10`; then in the g++.dg tree, for each `run.tsv` line (NAME STD G++ARGS CLANGARGS), `clang++ -fsyntax-only -x c++
  -std=STD CLANGARGS -ferror-limit=0 -fno-color-diagnostics -fno-caret-diagnostics -Wno-unknown-warning-option -iquote DIR NAME >
  CL/NAME.err` (NAME's / as __); `REPR_CPP_FRONT=native arbace_tool ... read WORK dg-error`; `arbace_tool ... own WORK --clang CL`.
- Scores (bin/crops-score --crop cpp): 76.1 / 100 / 67.7 before, 76.1 / 100 / 67.8 after (read 0.648 to 0.653: the sample record's 64 own-judged dg cases at 1; about 68.7 once the night records the whole part, about 650 own passes at the sample's rate); test/crops/scores.edn recorded. Proposed CP270 (clang's builtin table for the undeclared check), CP271 (the REAP read cell and the own record), CP272
  (the code generator's Sema as the checker, its gaps), CP273 (the next diagnostics by the measure), CP274 (the part's cost).

## 2026-11-08: C++'s CX12, second slice: the measured order's diagnostics and clang's identifier table (branch `cpp-cx12`, from 64e7b9758)

By the brief of 2026-11-08 (continue CX12 in CP273 (a)'s order) and the user's decisions of the same evening (CP270-CP274 as
recommended, relayed by the coordinator: CP270 (a) to be built in this slice), under the day's light rules: the every-10th sample only, the
compiler stubs first on PATH (their log empty). doc/cpp/CPP-SURVEY.md, "Decided (CP270-CP274)", "As built: CX12 (second slice)".

- **Built**, every rule on the checker's side of the code generator's Sema (`arbace.cpp.sema/checking?`, true only while the diag's
  `*on-refusal*` is bound): deleted functions as candidates, the call whose only viable function is deleted an error, the implicit copy
  members deleted by a declared move ([class.copy.ctor]/6) and the implicit moves declared ([class.copy.ctor]/8); reference binding with
  volatile and the cv inclusion order ([dcl.init.ref], [over.ics.rank]/3.2.6); member pointers in deduction (template.ae's port of
  SemaTemplateDeduction's MemberPointer case, the method qualifiers, [temp.deduct.call]/6.2) and in conversion and `.*`
  ([conv.mem], [expr.mptr.oper]/6); "no matching function" for templates only when each deduction failed surely; access control for a
  non-member function in a unit without friends; constexpr variables' initializers evaluated, an undefined operation the error at its
  line; a namespace-scope initializer's refusal reported and the analysis going on; init/ptrfn2's crash (the InitListChecker port's
  empty class). CP270 (a): `test/cpp/builtins.ae` reads the pinned llvm-project-23.1.5 tarball's `clang/include/clang/Basic/Builtins.td`,
  `BuiltinsBase.td`, `BuiltinsX86.td`, `BuiltinsX86Base.td`, `BuiltinsX86_64.td` (a TableGen subset: class, def, let ... in, foreach,
  # pasting, NAME) and names the builtins as `clang/utils/TableGen/ClangBuiltinsEmitter.cpp`'s collectBuiltins does, with `TokenKinds.def`
  and `TransformTypeTraits.def`'s keywords, into `cpp/target/clang-builtins.edn` (3,775 builtins, 395 keywords; `bin/repr-cpp target
  builtins gen|verify`); the undeclared check judges a `__` name by it (LookupBuiltin: no library builtin in C++).
- **Measured** on the sample, the same clang table as the first slice: own passes 64 → 68 (deleted, volatile, member pointers in
  deduction) → 75 (access, member function pointer qualifiers, constexpr, implicit moves, global recovery) → 75 (sure deductions only)
  → 76 (the identifier table). Expected lines with an own error: "call to implicitly-deleted copy constructor" 0 → 85 of 86, "no matching
  function" 38 → 81 of 109, "assigning ... incompatible type" 0 → 12 of 12, "is a private member" 0 → 7 of 17, constexpr 0 → 5 of 25,
  g++'s own 6 → 18 of 209.
- **No false credit**: the 12 new passes read one by one against their sources and clang's outputs, each a real error on every expected
  line (three with another reason than clang's); no first-slice pass lost. A measured run showed sure-looking template drops that were
  the slice's (pack-indexing11, bit-cast4): the drop rule was narrowed to the named sure reasons before recording.
- **The code generator unchanged**: `arbace.cpp.codegen gen-list` over test/cpp/codegen's 142 samples by 64e7b9758's tree and this
  branch's, 404 of 404 files byte for byte (no clang; codegen-check with clang live is the night's).
- **Alternatives considered**: the rules on for the code generator too (rejected for now: the brief keeps code generation unchanged;
  they are correct and CP280 proposes taking them where the samples show gaps); a deleted best function always an error (rejected:
  cpp1z/noexcept-type3's two overloads the slice merges); every template drop a sure one (rejected after the measure, above); `__`
  names judged by a hand list (rejected: CP270 (a)'s table from the pin).
- **Recorded**: test/cpp/check-dg-own-linux-amd64.edn (454 rows, 76 passes), the C++ read fact's cases (`:dg-own [76 1]
  :dg-clang [4521 0.5]`), test/crops/scores.edn (C++ 76.1 / 100 / 67.78, read 0.654; about 68.9 when the night records the part).
  `make check-makefile`, `gate.crops check`, `bin/gate --full --plan` pass; the dg control fails as it must. Proposed CP280 (the gaps the
  global initializers show), CP281 (constraints' satisfaction as a sure failure), CP282 (g++'s own lines: attributes, then OpenMP), CP283
  (the table's verify in the gate). Slips: one `<(...)` and one `2>/dev/null` in throwaway commands (the survey's "Slips").

## 2026-11-08: C++'s CX12, third slice: lookup, constraints, static_assert, access, constexpr, conversions; CP280-CP283 (branch `cpp-cx12`, from dd3f526dc)

- **Decided** (the user, 2026-11-08): CP280-CP283 (a) (doc/cpp/CPP-SURVEY.md, "Decided (the user, 2026-11-08: CP280-CP283)").
- **Built** (doc/cpp/CPP-SURVEY.md, "As built: CX12 (third slice)"), each rule the checker's (`arbace.cpp.sema/checking?`) unless
  said: an operator expression on classes no operator and no conversion function serves, with two-phase lookup's definition-point
  cutoff in function templates' specializations, and a call there nothing finds though a later declaration names it, both where the
  associated namespaces are sure ([over.match.oper]/3, [over.built], [temp.dep.candidate]); CP281 (a): satisfaction with a doubt flag
  (a refusal read as not satisfied), a false with no doubt a sure drop; a compound requirement's return-type-requirement evaluated
  ([expr.prim.req.compound]/1.3; the parser keeps `-> C<...>` as `:_ret`); `__is_same_as`; static_assert's conditions; access
  through private member typedefs; constexpr initializers' calls of non-constexpr functions, pointer-integer conversions and loads of
  unusable variables ([expr.const]/5.2, /5.9, /5.15); callees declared nowhere and sure-ADL undeclared calls; the explicit-specifier
  evaluated and direct-initialization's explicit conversions (CP280 (a)), "no viable conversion" from a class with only explicit
  conversion functions; C++20's returned-local lvalue retry; `typedef struct { ... } T` (the unnamed class named for linkage: every
  `T t;` was refused before, pr56961's real gap); C++20's parenthesized aggregates (CP280 (a), under the file's standard); CP282 (a)'s
  first rule, a standard attribute with no arguments and `...` ([dcl.attr.grammar]/4), a syntax error that declines nothing; CP283
  (a): the `--full` step `cpp-builtins`.
- **Measured** on the every-10th sample (454 files, clang's table from the first slice's outputs): own passes 76 → 77 → 82 → 87 → 86.
  Expected lines with an own error: "no matching function" 81 → 90 of 109, "use of undeclared identifier" 30 → 39 of 51, "is a
  private member" 7 → 10 of 17, "no viable conversion" 5 → 8 of 14, "cannot be used as an attribute pack" 0 → 7 of 7.
- **No false credit**: the eleven new passes read against their sources and clang's outputs; template/access19 passed by a wrong
  reason (the explicit-specifier's pattern without a word boundary made `explicit_t(...)` explicit) and was withdrawn by the fix;
  the ten others genuine. No earlier pass lost; extras 98 → 92 lines, none new.
- **The code generator unchanged**: `gen-list` over test/cpp/codegen's 142 samples by dd3f526dc's tree and this branch's, 404 of 404
  files byte for byte, before and after the fix (no clang; codegen-check live is the night's).
- **Alternatives considered**: generic lambdas for lookup/operator-3a's 37 lines (left: a code generator feature, CP290); unknown GNU
  attributes as tree nodes as CP282 (a) words it (not built: the tree is compared with clang's AST, CP294); every constraint false a
  sure failure (rejected: the requires-expression's refusals read as false); constexpr's sure rules inside called functions
  (rejected: gcc and clang place those errors at the inner call, lines the dg directives differ on).
- **Recorded**: test/cpp/check-dg-own-linux-amd64.edn (454 rows, 86 passes), the C++ read fact (`:dg-own [86 1] :dg-clang
  [4511 0.5]`), test/crops/scores.edn (C++ 76.1 / 100 / 67.80, read 0.655). `make check-makefile`, `gate.crops check`, `bin/gate
  --full --plan`, `bin/repr-cpp target builtins verify` pass; the dg control fails as it must. Proposed CP290 (generic lambdas), CP291
  (fold expressions), CP292 (the return-type-requirement for the code generator), CP293 (the file's standard in the code generator),
  CP294 (gcc's unknown attributes as data, not nodes).

## 2026-11-08: C++'s CP290-CP294 built: generic lambdas, fold expressions, the return-type-requirement and the standard in the code generator, gcc's unknown attributes as data (branch `cpp-cx12`, from 013681c7e)

- **Decided** (the user, 2026-11-08 evening): CP290-CP294 (a) (doc/cpp/CPP-SURVEY.md, "Decided (the user, 2026-11-08: CP290-CP294)").
- **Built** (doc/cpp/CPP-SURVEY.md, "As built: CP290-CP294"): CP290, a generic lambda's closure with its `operator()` a member
  function template (invented type parameters for each `auto`, a pack for `auto...`, after the explicit ones; declared through the
  templates' `:declare` hook, deduced and instantiated by arbace.cpp.template; the specialization's body analyzed in the lambda's
  context with the definition-point cutoff, [temp.res]; captures found at the lambda by a scan of the body for a capture-default, a
  later need refused); the native parser keeps the parts as `:_glam` data, its tree unchanged. Found with it and fixed: the code
  generator's Sema refused `CXXDependentScopeMemberExpr` (the native tree has it since a14d52531, 2026-11-04), so 10 of
  test/cpp/codegen's 142 samples were refused since; `template <class...>` (unnamed parameters) was not seen as a template by the
  parser's `in-template?`, so its deduced return type was taken from the body; the implicit copy constructor's and copy assignment's
  parameter is `X&` when a subobject has no const copy ([class.copy.ctor]/7, [class.copy.assign]/2). CP291, fold expressions: the
  comma among the fold-operators, the four forms parsed into `:_fold` data in the code generator's mode, expanded at instantiation
  ([temp.variadic]/11-12), each step the written operator's analysis. CP292, the return-type-requirement for the code generator (a
  false met with a doubt stays true there). CP293, `*cxx-year*` bound by `gen-file` (cxx-year moved to arbace.cpp.sema). CP294, GNU
  attributes clang drops kept in the parser's slot `GATTRS` and as `:_gattrs` on fields and variables, checked by
  arbace.repr.cpp.diag's `gattr-rules`: strict_flex_array's, written clean-room from the GCC 15.2 manual's "Common Variable
  Attributes" (gcc/doc/extend.texi of the pinned gcc-15.2.0.tar.xz, read; no GCC code), the messages Arbace's own.
- **Measured**: the every-10th sample's own passes 86 → 88 → 89 → 89 (lookup/operator-3a, diagnostic/pr79304, strict-flex-array-1),
  the extras' 92 lines the same; the whole part's 99 files with a generic lambda, a fold or strict_flex_array, base against branch:
  12 → 16 (operator-3a, operator-4, -4a, -5a, strict-flex-array-1; concepts-lambda19 lost).
- **No false credit**: every new pass read. Withdrawn: diagnostic/param-type-mismatch-3 (passed by "no matching function", which the
  slice would also say of a valid conversion of a generic closure to a pointer to function: such a call is now unsure in the
  checker); concepts-lambda19's earlier pass was the parser's decline of the comma fold on a valid line; parsed now, its real error (at
  an explicit instantiation the checker does not instantiate) is not judged.
- **The code generator**: `gen-list` over test/cpp/codegen's 142 samples, base 013681c7e and branch: 131 byte for byte; the 10
  refused since a14d52531 generated, their defined names equal clang's IR's of the same samples (cpp-runtime-own's codegen-check
  of today, `x.ll`) but hdr-vector's known one; rtti refused as before. Probes of every capture form, folds and constrained
  overloads generate the IR clang names. The live codegen-check (clang, gcc) is the night's.
- **Alternatives considered**: a generic lambda's specialization analyzed inside the lambda's expression (rejected: specializations
  are made by later calls; the closure object is built first, hence the capture scan and the sealed refusal); fold operands as
  tree nodes (rejected: the tree is compared with clang's, `:_fold` data as `:_ret` before); gcc's messages for the attribute
  rules (rejected: the dg verdict compares lines, the messages written anew); the generic-closure rule removed and the pass kept
  (rejected: false credit).
- **Recorded**: test/cpp/check-dg-own-linux-amd64.edn (454 rows, 89 passes), the C++ read fact (`:dg-own [89 1] :dg-clang
  [4508 0.5]`), test/crops/scores.edn unchanged (C++ 76.1 / 100 / 67.80, read 0.655). `make check-makefile`, `gate.crops check`,
  `bin/gate --full --plan`, `bin/crops-score --judge --strict` pass; the dg control fails as it must. One slip: a throwaway
  `diff <(...) <(...)` (process substitution, against the day's rules), redone with files. Proposed CP300 (templates differing by
  constraints), CP301 (a generic lambda's conversion to a pointer to function), CP302 (the code generator's light half in `--full`),
  CP303 (the next unknown GNU attributes), CP304 (implicit captures in unevaluated operands), CP305 (explicit instantiation
  definitions in the checker).

## 2026-11-08: C++'s CX12, fifth slice: "no matching function"'s rest, constexpr, access, conversions (branch `cpp-cx12`, from 1443ba1c5)

- **Built** (doc/cpp/CPP-SURVEY.md, "As built: CX12 (fifth slice)"), in CP273 (a)'s order, each rule the checker's unless said:
  non-deduced contexts ([temp.deduct.call]/1, /6: a braced list or an overload set of two or more functions for a bare template
  parameter, undeduced and named by no other parameter: a sure drop) with the checker's position kept at the call while deducing;
  C++11's null pointer constants (`false` none, CWG 903); a reinterpret_cast as a template argument ([temp.arg.nontype]/2) a sure
  drop; a requirement surely false (a member type of a scalar, `*` of a non-pointer); `constexpr-bodies` and `constexpr-redecls`
  in arbace.repr.cpp.diag after SemaDeclCXX.cpp's CheckConstexprFunctionBody, CheckConstexprDeclStmt, CheckConstexprMissingReturn
  and MergeCXXFunctionDecl (llvm-project-23.1.5, read from the pinned tarball; the texts clang's), the parser's `:_cexpr` on a
  constexpr lambda; if constexpr's condition by the evaluator's sure rules; a namespace-scope initializer's private constructors,
  template-ids' member types and `T()`'s, ambiguous bases (a qualified member through its naming class); a const object's conversion
  functions, explicit ones only yielding the type in direct-initialization ([over.match.conv]/1.1), enumerations' initialization;
  CTAD from no initializer by the defaults; for the code generator too, an implicit default constructor deleted by a member with
  none ([class.default.ctor]/2.5). None of CP300-CP305 built or needed.
- **Measured** on the every-10th sample (454 files, clang's table from the first slice's outputs): own passes 89 → 99 → 108; the
  extras' 92 lines the same. Lines with an own error: "no matching function" 90 → 95 of 109, "is a private member" 10 → 12 of 17,
  "no viable conversion" 8 → 12 of 14.
- **No false credit**: the 19 new passes read against their sources and clang's outputs, each the program's error on every expected
  line; probes of the valid forms give no diagnostic; no earlier pass lost.
- **The code generator**: `gen-list` over test/cpp/codegen's 142 samples by 1443ba1c5's tree and this branch's: 141 of 141 forms
  byte for byte (rtti refused as before).
- **Alternatives considered**: the conversion rules for the code generator too (left to CP312: a change of generated programs
  wants codegen-check live); access in units with friends by a friend-aware context (CP313); the in-call constexpr errors at the
  variable as clang places them (rejected: the dg lines are gcc's, which place them by kind, CP310); `auto x{e}` in C++11 (left).
- **Recorded**: test/cpp/check-dg-own-linux-amd64.edn (454 rows, 108 passes), the C++ read fact (`:dg-own [108 1] :dg-clang
  [4489 0.5]`), test/crops/scores.edn (C++ 76.1 / 100 / 67.82, read 0.656). `make check-makefile`, `gate.crops check`, `bin/gate
  --full --plan`, `bin/crops-score --judge --strict` pass; the dg control fails as it must. Two slips: a read of `/dev/null` as an
  empty grep argument and a `2>/dev/null` in a wait loop (the survey's "Slips"). Proposed CP310 (constexpr errors inside calls and
  immediate functions' addresses), CP311 (CTAD with initializers), CP312 (the conversion rules for the code generator), CP313
  (access in units with friends), CP314 (C++11's constexpr function body).

## 2026-11-08: HES84 (b) and HES85 (a), the threaded ways kept in their class and nofib's C in record-ref (branch `hs-hes84`, from next 5af7d0058)

By the user's decisions of 2026-11-08 (HES84 (b), HES85 (a); doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HES84 (b), HES85 (a)").
Sources studied: nofib at its pinned commit (`.tmp/hsi/nofib`, `test/haskell/eval-corpora.edn`'s `:nofib` pin): `mk/ghc-paths.mk`
(`ALL_SRCS`, `SRCS`, `EXCLUDED_SRCS`, `C_SRCS`), `mk/target.mk`, the Makefiles of the nine program directories with C;
GHC 9.14.1's testsuite (`test/haskell/eval/testsuite-run.edn`, the tests whose `only_ways` names only threaded ways).

- HES84 (b): no code; `:threaded-only`'s `:decided` names it. What it leaves judged against the normal way: 14 tests of the core,
  7 passing, four of them on the normal way's refusal or another output than the shipped one (T21651, ffi014, hs_try_putmvar003,
  numsparks001): HES87 proposed.
- HES85 (a): `bin/lib/hsi/record-ref`'s nofib plan passes `nofib_c_srcs`, the C nofib's make links (`C_SRCS`: the Makefile's
  `SRCS` when it sets one, else the directory's sources less `EXCLUDED_SRCS`): callback001, callback002 and fft2. Alternative
  considered: every `.c` of the directory, as `bin/hsi check`'s plan and the proposal's text had it, rejected: six Makefiles set
  `SRCS` to leave a C version out (paraffins' has its own `main`), so GHC would fail to link them at the next whole record.
  Recorded by GHC 9.14.1 by hand (`bin/hsi record-ref --corpus nofib --only nofib/smp/callback001,nofib/smp/callback002`): both
  status 0, callback001 its shipped `.faststdout`; with fft2 and paraffins added, every line as committed but its seconds.
  `bin/hsi own-verdicts --record`: 587 of 908. hsi: callback001 passes by its own verdict (recorded); callback002 passes by hand at
  3 GiB (0.71 GB resident at 300,000 callbacks), stopped by the light 2 GiB cap: it waits in `:no-reference` for the night's record.
- `test/crops/independence.edn`'s judge cases from the records (eval 587 own, 321 recorded); `bin/reap --record` (eval 801 / 64 / 43),
  `bin/crops-score --record`: Haskell 96.6 / 100 / 74.9 (0.9659 to 0.9661; independence 0.7492 to 0.7494). Independence stays near 75
  until the native front end's STG is the eval and AOT cells' source (HFS-I1).
- Checks: own-verdicts current, hsi-essential's command 123 of 123 with the controls, `bin/hsi divergences` 0 unclassed, `bin/reap`
  and `bin/crops-score --judge --strict` 0 problems.
- Proposed: HES86 (bin/hsi check's nofib C by the same rule, at the next haskell-stg-progs recording), HES87 (the four threaded-only
  passes on a refusal into a deferred class).

## 2026-11-08: Haskell's accepted proposals: HES86 (a), HES87 (a), HF9-P1 (a) + P8, HF9-P2 (b) + P5 (a), HF9-P6 (a), HF9-P7 (a) (branch `hs-props`, from mac/hs-hf9 7ed40299a, mac/hs-hes84 merged)

The user's decisions of 2026-11-08 (about 19:45), built by an agent on the main host, light (doc/haskell/HASKELL-EVAL-SURVEY.md,
"As built: HES86 (a), HES87 (a)"; doc/haskell/FRONTEND-SURVEY.md, "As built: HF9-P1, P2, P5, P6, P7, P8").

- HES87 (a): a rule, not a list: `bin/hsi own-verdicts` computes `test/haskell/threaded-way.tsv`, the tests all.T runs only in
  threaded ways whose recorded normal-way run is not their own verdict (10); `bin/lib/hsi/check` records a match of such a run
  `:threaded-way` (`:would :pass`), so exactly T21651, ffi014, hs_try_putmvar003 and numsparks001 leave the passes (T10414, T14346,
  T21969 keep theirs: GHC's run is their own verdict). The deferred class `:threaded-way` in `divergences.edn` (the four) and
  `divergences-aot.edn` (ffi014, the one the AOT level runs); the committed rows rewritten as the check writes them (checked by
  `bin/hsi check --only` the four and T14346: threaded-way 4, pass 1). Eval 801 to 797, AOT 138 to 137. Alternative: the four ids
  listed by hand in the class (rejected: the rule is the reason, and a re-record keeps it).
- HES86 (a): `nofib_c_srcs` moved into `bin/lib/hsi/records.bash`, record-ref and the check both use it; six programs' C goes
  (paraffins, primes, compress, fasta, reverse-complement, simple), their haskell-stg-progs entries STALE until the night records
  them again (write-ahead, as decided).
- HF9-P2 (b) + P5 (a): `tools/hsdump/iface2/Iface2.hs`, a second executable under its own version and key (`hsr_iface2_hash`), built
  with HsRn.hs's export list opened in a copy so its forms are the iface verb's and `hsr_helper_hash` does not move: hidden modules
  found among the units' hidden modules and read by getModuleInfo; every unexported type constructor from the external package
  state (not classes, implicit things or family-instance tycons); pattern synonyms as patSynSig's parts and the builder type (GHC's
  pretty-printed type left out: it carries session uniques). The elaborator takes imported pattern synonyms' signatures. Alternative
  considered: a verb inside tools/hsdump's main (rejected: the glob `tools/hsdump/*.hs` is every pack's key).
- HF9-P1 (a) + P8: lib-check reads the pack haskell-lib-ifaces-ghc-9.14.1 (pool and closures, keyed by the iface2 verb), `--live`
  the helper, `--record-pack`, `--verify-pack` (uniques renumbered, the crossed pair), `--record`, `--judge` (worse modules,
  missing ones, unclassed ones, the negative control Either judged against Maybe's interface); the `--full` step haskell-lib-check
  (2 GiB). The record `test/haskell/lib-check-ghc-internal.edn`: 266 modules, 261 pass; the pack is the night's (the whole package's
  closures by GHC), so the step is UNRECORDED until then.
- HF9-P6 (a), HF9-P7 (a): `test/haskell/divergences-lib-check.edn`: `:kinds` (Generics, Data.Typeable.Internal, Data.Dynamic),
  `:default-signatures` (Exception.Context, found today: the default method checked at the method's type, its default signature's
  Show not given; with slice 7), `:th-quotes` (TH.Lift). The derived Data/Generic/Lift methods wait for slice 7.
- lib-check on the 18 helper-limited modules with the iface2 verb: 16 pass (were 0), Exception.Context 5 to 1 error, Data.Dynamic
  4 to 2 (both now the elaborator's), Event.Internal 3 to 7 of 7 binders equal; the 18 hidden modules, never judged before, all pass.
  ghc-internal: 248 judged, 227 clean and 226 equal, to 266 judged, 261 clean and equal.

## 2026-11-08: HF9's first slice, chosen by measure (branch `hs-hf9`, from next 835891150)

The brief: Haskell's independence stays under 75% while hsi runs the boot libraries' STG GHC made; HF9 (the type-level tier) is
the first of the two milestones that remove it. Measured first, then built in the order of the measure. Detail in
doc/haskell/FRONTEND-SURVEY.md, "As built: HF9 (first slice)".

- **The measure** (`bin/repr-haskell lib-check`, `arbace.repr.haskell.libcheck`): ghc-internal's 266 modules natively read, renamed
  and elaborated, each against the interfaces of its imports (the helper's `iface` verb, GHC 9.14.1's installed interfaces: the
  oracle tier's), and its binders' types judged against GHC's own interface of the module. ghc-prim 9.14.1 has no Haskell but
  `Dummy.hs` (its modules moved into ghc-internal at GHC 9.10); base's modules re-export ghc-internal's.
- **Before → after** (the same closures and dumps; the branch's start with the judge's additions alone): ghc-internal's 248 judged
  modules (18 hidden: GHC-87110, the helper cannot load them) elaborate without an error 122 → 200, with every binder's type equal
  to GHC's interface 113 → 198; binders equal 4,164 of 4,473 → 4,937 of 4,992; stopped 5 → 0. base's sample (every 9th, 26
  judged): 23 → 26. The ranking (modules blocked, before): unboxed tuples in types 30, kind arguments of poly-kinded tycons 29,
  `HsDocTy` 28, untyped variables 19, Coercible 14, GADTs 13, derived superclasses 13, unknown constructors 12, type applications
  10, unboxed sums 8, ScopedTypeVariables 4 (with the metas they leave unsolved), type families 4, `(->)` as a tycon 4, implicit
  parameters 4, MultiWayIf 4, rank-n 2, type literals 4. After: GADT local equalities 12, pattern synonyms 7, unknown constructors
  7 (pattern synonyms' uses), names of hidden modules 8, derived Show/Eq contexts 6-7.
- **Built**: `arbace/repr/haskell/famenv.ae`, a port of GHC 9.14.1's `compiler/GHC/Core/FamInstEnv.hs` (reduceTyFamApp_maybe,
  lookupFamInstEnv's matching, chooseBranch, findBranch, apartnessCheck, compatibleBranches, computeAxiomIncomps) with
  `compiler/GHC/Core/Unify.hs`'s tcMatchTys and tcUnifyTysFG (the pinned tarball, SHA-256 `2a83779c...101e77`, BSD-3 Glasgow
  license, the notice in the file, LICENSE.md and doc/ARBACE.md updated); in `typecheck.ae`, after GHC's functions as the
  specification: kinds erased from interface types (each tycon's, promoted constructor's and class's invisible arity), the
  families read and reduced in `expand`, stuck equalities deferred, GADT constructors with rejigConRes's split and given
  equalities per alternative (imported constructors' split from their types), signatures checked (tcPolyCheck's order), data family
  instances as representation tycons, unboxed tuples and sums, ScopedTypeVariables and pattern-bound type variables, rank-n
  foralls, type applications, implicit parameters, view patterns, MultiWayIf, Coercible erased, Typeable of applications,
  KnownNat/KnownSymbol/KnownChar on literals, `(->)`/FUN as tycons; three bugs of the existing code the measure found (a documented
  deriving clause's classes dropped, standalone deriving's variables lost, simplify-pred's `first` of a keyword).
  `arbace/repr/haskell/libcheck.ae` and `bin/lib/repr-haskell/lib-check` (`bin/repr-haskell lib-check`, `make
  repr-haskell-lib-check`): the measure. `bin/repr-haskell`'s native reads in typecheck-check, rename-check, run-check's
  typecheck lists take `--libdir` (typecheck-check failed at `/settings` on next before this branch).
- **Checks**: typecheck-check 24 of 24, its control differing; run-check 59 of 63 pass, 62 of 63 as recorded, the one differing
  (`synthetic/primops-word`'s STG-stage StackOverflowError) the same at the branch's start.
- **Proposed**: HF9-P1 (the measure as a pack-backed `--full` step), HF9-P2 (a second helper verb for hidden modules and
  pattern synonyms' types), HF9-P3 (speed, then GADT implications, pattern synonyms, deriving), HF9-P4 (kinds erased for now).
- **Findings beside the slice**: the native dumps of the read stage hold control bytes (a `"..."#` literal's bytes), so the host's
  grep (ugrep) takes them for binary and prints nothing with `-o`: every tool that greps a dump needs `-a` (lib-check's closures
  were empty for 20 modules until then); the helper's `iface` verb cannot load ghc-internal's 18 hidden modules (GHC-87110) and
  writes a pattern synonym without its type; two modules (TH.Syntax, System.Posix.Types) take 5 to 10 minutes in the elaborator.
- Alternatives considered: judging against `tc` dumps (GHC compiling each module: GHC at check time for every module, and
  ghc-internal's modules compile only as the package itself); a clean reimplementation of type family reduction (rejected: the
  brief's rule for rule port, and the apartness check is subtle); kind inference instead of kind erasure (the elaborator needs no
  kinds to place evidence; Typeable's kind representations are the one consumer, left for a later slice).

## 2026-11-08: HF9's second slice: speed, implications, pattern synonyms, deriving (branch hs-hf9)

repr/haskell's type-level tier, the remaining items in HF9-P3 (a)'s order, measured with `bin/repr-haskell lib-check` on each
item's modules by day and on the whole of ghc-internal at the end; doc/haskell/FRONTEND-SURVEY.md, "As built: HF9 (second slice)".

- **Measure, first slice's end → this slice's end** (the same closures, dumps and judge): ghc-internal's 248 judged modules
  without an error 200 → 227, every binder's type equal to GHC's interface 198 → 226, binders 4,937 → 4,955 of 4,992; the whole
  run in one process 2,782 s → 592 s; base's sample 26 of 26. No module lost (none went from passing to failing).
- **Speed**: the renamer's interface cache was keyed by the list of interface directories, and lib-check gives each module its own
  directory of links into one pool, so every module parsed its closure's interfaces again and kept the copies: the process grew
  and its later modules ran 5 to 9 times slower (the corpus's last 53 modules 2,124 s → 221 s once keyed by the file). Then the
  instance table indexed by class, derived contexts by a simultaneous fixpoint until none changes (GHC's inferInstanceContexts:
  each round from the previous one's), the renamed tree kept in memory by lib-check: TH.Syntax 108 s → 32 s alone.
- **GADTs and implications** (written anew after GHC.Tc's rules): checking mode (case, if, let, lambda, multi-way if, list
  comprehensions; tcApp's result unified before the arguments are checked; tcSkolemise of an expected polytype), sub-patterns
  checked at the pushed type, equality givens of contexts, a pattern's givens over the statements after it, wanteds and failed
  equalities kept with their implication's givens and solved later, givens on still unknown types pending until their binding
  ends. Data.Data, Data.String, Data.Type.Equality, Err, TypeLits, TypeNats, Unsafe.Coerce pass.
- **Pattern synonyms**: signatures split as tcPatSynSig, declarations checked (tcCheckPatSynDecl) or inferred, uses as
  patterns (tcPatSynPat) and builders; Tuple, Natural, TypeLits, TypeNats, Exception, IO.Encoding.Types pass.
- **Deriving**: standalone deriving of imported types, derived Eq/Ord/Show on unlifted fields (`deriv.ae` takes GHC.Tc.Deriv.
  Generate's primOrdOps, boxConTbl and postfixModTbl, under its GHC notice), the newtype strategy only when eta-reducible, Generic's
  contexts empty, data instances' derived heads at the family, existential and GADT record patterns, RecordWildCards in
  expressions. Beside them: DataToTag, WithDict, implicit-parameter bindings and simplifyTop's defaulting of ?callStack and
  ?exceptionContext, variable constraints, data families generative, over-applied families, `e :: sig` checked, promoted lists,
  non-leading invisible kind binders, naughty selectors, default signatures apart from method types.
- **Left** (21 modules): 18 wait for the helper (hidden modules' names, a non-exported synonym `CodeBuffer#` the `iface` verb omits,
  the imported pattern synonym `Fun`), 2 for kinds (Generics' SingKind, Data.Typeable.Internal), 1 for Template Haskell quotes.
  The methods of derived Data, Generic and Lift are not generated (their heads and contexts are): HF10's STG needs them.
- **Checks**: typecheck-check 24 of 24 (control 13 of 24); run-check 59 of 63, 62 of 63 as recorded, the differing one the first
  slice's (`synthetic/primops-word`); base's sample 26 of 26.
- **Proposed**: HF9-P5 (the helper's second verb also writes non-exported tycons), HF9-P6 (kinds: keep erasure, 2 modules in a
  class), HF9-P7 (Data/Generic/Lift's code with slice 7), HF9-P8 (the measure as a recorded check, now 12 minutes).
- Alternatives considered: rebuilding the interface cache per module (rejected: the pool's files are shared, keying by the file is
  the cache's intent); a fresh process per module (15 s of start-up each); eager unification of a GADT given's unknown sides (the
  first slice's: wrong when a later argument fixes the scrutinee's type); generating Data/Generic/Lift's methods now (unmeasured by
  lib-check; with the desugaring slice instead).

## 2026-11-08: the C API on ci, OCaml's eval `c-api` from 25 deferred to 5 (branch `ocaml-compl`, from next 7744e70b4)

- **Why.** OCaml's completeness was 80.9%; its cells' deferred cases were eval's `c-api` 25, AOT's `c-api` 25, `:light-speed` 35 and
  `:light-depth` 4, print's `:string-bytes` 2, none pending. `c-api` (DEFERRED-TRIAGE's item 9, OT27 (a)) was the biggest cause; the
  light level's 39 wait for the compiled level's whole record (OT8 (a)).
- **Built.** A test's C stubs run on ci beside oi: Arbace's own clean-room `caml/*.h` and C runtime (`test/ocaml/capi/`, from OCaml's
  manual chapter 22 and, for BLAKE2b, RFC 7693's specification; no OCaml runtime source read for them), linked with the stubs and musl
  by `test/ocaml/capi/cnodes.ae` (repr/c, ci's checker, the prebuilt musl, Arbace's own C target) into a node program that
  `go/arbace/oicapi` loads into ci inside oi's light driver (`OI_CSTUBS`): OCaml's value layout in ci's memory, oi's values copied at
  the boundary (once a pass, identity kept, read back on return, raise and callback, written again after a callback), callbacks on a
  stack of no effect handlers, exceptions, named values, bigarrays, C custom blocks marshalled by their operations, getpid, kill and
  raise as the host's. oi's hooks in `go/arbace/oi/ext_capi.ae`; header.awk runs `modules = "X.c"` tests at eval (the AOT levels and
  native-only tests keep the class); two rules of OCaml's on the way (an allocation is a poll point; a stub's signal to the process
  is pending when it returns); the typer's Ptyp_open (doc/ocaml/TYPER.md §26). The compiled AOT level and its suite now take the native
  path too (for OC202), unrun: its builds exceed the day's memory.
- **Route.** OT27 (a) chose ci over a clean-room `caml/*.h` after oi's embedding in the shipped program, the one executable thought
  to hold both; the light driver links ci's back end itself, so it is built now (Proposed OC198 asks to confirm). Alternatives
  considered: handles for blocks (OT27's sketch; rejected: `Field(v, i)` and `&Field(args, 0)` are memory in the stubs, so the copies
  keep OCaml's layout), a subprocess to the tools' host (OT27 (b): a round trip a callback), the stubs ported to Go (OT27 (c): no C
  checked).
- **Checked** (light, by list, main's stage 0): the 25 programs on the native path and on the oracle path (`--ocdump`), each 17 pass,
  2 Marshal's Closures flag (`:marshal-closures`, OT7 (a), now in the run corpus's classes), 1 `:speed` (subarraystub), 5 still
  `:c-api`; both recorded by list (check-oi-native.edn, check-oi.edn; the known file's two pins attested); the essential list 58 of 58,
  six signal programs, `bin/oi test`, `repl-check --native` 24 of 24, `own-verdicts --check`, the four divergences judgments,
  gate-key-crops-test, `bin/gate --plan` and `--full --plan`.
- **Scores**: OCaml 80.9 / 95.2 / 100 / 87.4 to **81.5** / 96.3 / 100 / 87.4 (eval 441 / 205 / 25 to 458 / 208 / 5); `bin/reap` and
  `crops-score --judge --strict` 0 problems. Proposed OC198-OC202 (the route and the AOT mirror, native-only stubs, a Fortran stub, a
  test's C helper program, the AOT cell at the compiled level); the night's commands in the survey's section.

## 2026-11-08: OC199 (b), OC200 (a), OC201 (a) built, OC198 (a) and OC202 (a) recorded; OCaml's eval `c-api` empty (branch `ocaml-compl`, from 265bb7911)

- **Decided** (the user, about 20:30, relayed): OC198 (a), OC199 (b), OC200 (a), OC201 (a), OC202 (a) (doc/ocaml/OCAML-SURVEY.md,
  "Decided (the user, 2026-11-08, about 20:30)"). OC195 is undecided, so OC201 is built for C helpers alone.
- **Built.** OC199 (b), OC200 (a): the classes `:native-only` (int64-unboxing, unboxed-primitive-args; eval) and `:fortran`
  (bigarrfml; eval and both AOT levels), `:design`. OC201 (a): header.awk's helper mode reads ocamltest's first alternative (the
  helper's and the test's `ocamlc.byte` builds, each `run`, the source directory's `TEST.run` hook, as ocamltest's main.ml and
  actions_helpers.ml run them, ocamltest/ of the pinned OCaml 5.5.0 tarball read); bin/oi's `oi_helper` builds a C helper by
  `test/ocaml/capi/cnodes.ae --program` on Arbace's own C target and runs it by oi's driver (`oi cmain`, oicapi's `RunMain`), an
  OCaml helper with C stubs dumped and run by oi with its stubs on ci; `oi_one` runs the hook and the further runs. oicapi's host
  natives add `fstat` and `close` (a stub's descriptor is the process's); oi's driver opens Go's poller while descriptors 3 to 63 are
  held (found by strace: the poller's eventfd took the descriptor cloexec's `O_CLOEXEC` file had freed); Arbace's own
  `caml/unixsupport.h`; `Unix.openfile`'s `O_CLOEXEC` wins over `O_KEEPEXEC` (open_unix.c's rule).
- **Alternatives.** tools/cirun for the C helper (rejected: crt refuses a kill of another pid, by design for C's checks; changing it
  moves C's keys: Proposed OC205); a hook mode for every test (OC203, proposed: swapchan's sort); the OCaml-only second programs of
  OC195 (one condition away: OC204).
- **Checked** (light, by list): the 25 C API programs and six signal programs native (24 of 24 run pass) and `--ocdump` (19 pass, 6
  by design), both recorded by list; the essential list 58 of 58 with its control; 14 lib-unix programs; own-verdicts --check; the
  five divergences judgments; `bin/oi test`; gate-key-crops-test; `bin/gate --plan`, `--full --plan`.
- **Scores**: OCaml 81.5 / 96.3 / 100 / 87.4 to **81.6** / 96.6 / 100 / 87.4 (eval 460 / 211 / 0). The AOT light level's 39
  deferred: 29 pass compiled in the partial records, 2 by design, 3 deferred, 5 unrecorded; OC202's cell switch prepared as a patch in
  the worktree's `.tmp/oc202-cells.patch`, not applied. Proposed OC203-OC205.

## 2026-11-08: HF10's first slice: ghc-internal's modules desugared and made STG natively (branch `hs-hf10`, from `mac/hs-hf9` 7ed40299a)

Haskell's eval and AOT cases count 0.5 because every hsi run executes the boot libraries' STG GHC made (the pack
`haskell-stg-libs`). HF10 starts the native front end's compile of the libraries themselves; doc/haskell/FRONTEND-SURVEY.md,
"As built: HF10 (first slice)", holds the design (HF10-D1 to D6) and the measures.

- **Design.** The libraries take the programs' pipeline (desugar to untyped Core, SimpleOpt, the `-O0` Simplifier, CorePrep,
  CoreToStg, Unarise), in their own unit. The reference is GHC's `-O0` STG of the same packages compiled from the pinned tarball by
  `bin/hsi dumps`' recipe (`HSI_DUMP_OPT=-O0`, new): the pack's `-O2` STG cannot be equalled by a `-O0` front end. The judge
  (`stgcmp`) compares each exported binding with the internal top-level bindings it reaches, renamed together, and records the
  first difference of each, so the differences rank into rule classes. The slice: the 55 modules at levels 0 to 12 of
  ghc-internal's import graph, all elaborated by HF9's lib-check (`test/haskell/lib-stg.txt`).
- **Running.** Programs compiled against the helper's `-O2` interfaces name `-O2`-only bindings (`sameTypeRep`), so a coherent
  `-O0` library set runs 35 of 63; the slice runs on the pack's ABI instead, each module's own bindings that the judge finds equal
  in place of the pack's, the pack's other bindings kept beside them (`libcheck merge`): 595 exported bindings of 52 modules
  Arbace's own, 59 of 63 programs pass as on the pack, none lost. With every own binding (`LIB_STG_REPLACE=all`) three programs
  fail on the representations of clone binders (Base's `bindIO`): the bugs list.
- **Rules ported** (GHC 9.14.1, written from the source as before): CorePrep's speculative evaluation of dictionaries
  (mkNonRecFloat, exprOkForSpecEval's app_ok), its case floats out of scrutinees, cpeArgArity's eta expansion of local partial
  applications, CoreToStg's alternative types from the scrutinee, typePrimRep through synonyms; the match compiler's bang patterns
  (tidy_bang_pat, matchBangs) and strict variable bindings (the elaborator had dropped every bang); Core.Make's bindNonRec for an
  unlifted effect; getDFunTyKey's type variables; tagToEnum#'s tycon; strict unlifted fields. Equal bindings 435 of 1,004 at the
  first measure, 595 of 1,149 at the end.
- **Checks**: typecheck-check 24 of 24 (control 13); run-check 59 of 63, 62 as recorded (one intermediate state lost nine
  `primops-*` programs to a case on an unboxed tuple, caught by it and fixed); the judge's negative control 0 of 171.
- **Left**: deriving's tag code (GHC's `dataToTag#` and `Int#` primops against deriv.ae's boxed `eqInt`), the methods' eta
  expansion through casts, clone binders' representations, the Simplifier's `isTrue#` case rules, join points, foreign imports (49
  modules, two STG stops). Proposed: HF10-P1 (the `-O0` reference as a pack), P2 (lib-stg as a recorded check), P3 (the front
  end's own interfaces, bottom-up, the coherent `-O0` set), P4 (behaviour first), P5 (foreign imports).
- Alternatives considered: direct STG without Core (rejected: GHC's reference passes through Core, and one pipeline keeps programs
  and libraries checked together); the pack's `-O2` STG as the reference (no `-O0` binding can equal it); whole-module replacement
  in the pack (the `-O2` importers name workers and floated bindings the `-O0` module lacks); the `-O0` set (needs `-O0`
  interfaces: P3); replacing every own binding by default (three programs fail: the equal ones only, GHC's `-O0` code by
  construction).

## 2026-11-09 HF10 (second slice): the first slice's classes, behaviour first; lib-stg's list to level 30

Branch `hs-hf10` (from its first slice bd09e1192), an agent in the light tier. doc/haskell/FRONTEND-SURVEY.md, "As built: HF10
(second slice)", has the rules, the measures and HF10-P6 to P10. GHC 9.14.1's sources (the pinned tarball's `compiler/`) are the
spec; GHC itself only as the oracle (the first slice's `-O0` reference, and small probes by hand, `ghc -O0 -ddump-ds -ddump-simpl
-ddump-verbose-inlinings` on scratch modules, to find which rule makes a shape: the default methods' compulsory unfoldings, the
eta expansion of stable unfoldings, IP's evidence kept to CorePrep, all three found so).

- **Behaviour first** (HF10-P4 (a)): the binders the desugarer and the Simplifier make now carry types (GHC's exprType and
  dataConInstArgTys at the binding sites, `core.ae`'s `infer-binder-types!`, before and after the Simplifier), which fixed the
  representations class (148 → 20 first differences on the 55 modules) and with it Base's `bindIO` that broke programs under
  `LIB_STG_REPLACE=all`; Unarise's voidArgId; a `State#` returned by name as `(##)`; an unlifted top-level thunk (prepareRhs at the
  top level); the constructor wrappers (`$WSrcLoc`) unboxing the pack's unpacked fields, found by `synthetic/runtime-exceptions`
  failing on `not an integer: I#` in an intermediate state; primitive literal patterns, which had become wildcards.
- **Then by count**: deriving's untagged tag code as GHC's Generate.hs writes it (dataToTag#, ==#, /=#, tagToEnum#), the
  default methods' compulsory unfoldings (makeCorePair), dfuns CONLIKE, stable unfoldings eta-expanded, combineIdenticalAlts,
  litEq, the unboxed tuples' known constructors, join points (recursive ones let-no-escape, nullary ones' representations,
  is_more_evald), HasCallStack's push onto the given (EvCsPushCall), the superclass evidence bound once (dsEvBinds), non-exported
  top-level binders internal (Tidy), RULES' free variables kept alive, data constructors saturated as arguments (maybeSaturate),
  top-level string literals, PolyAlt for unlifted pointers, NOINLINE and OPAQUE pragmas, EmptyCase.
- **Measure**: the first slice's 55 modules 595 → 857 of 1,149 bindings equal to GHC's `-O0` STG, none lost; the list extended
  past level 12 to level 30 (`test/haskell/lib-stg.txt`, 112 modules, all made STG but the first slice's two foreign-import stops):
  2,473 of 3,310. Level 31's `GHC.Internal.IO.Encoding.UTF8` takes the Simplifier minutes an iteration (`utf8_decode`), past the
  900 s limit: the list stops before it (40 modules of levels 31 to 45 measured: 594 of 1,063).
- **Checks**: typecheck-check 24 of 24 (control 13); `lib-stg --run` over the 112 modules: 59 of 63 on the pack (62 of 63 as recorded), 59 of 63 with 2,451 own
  bindings of 109 modules, none lost; `LIB_STG_REPLACE=all` (5,944 own bindings) 28 of 63, 31 lost: the bugs list (the `-O2`
  ABI's arities, a lazily bound unlifted value, the GMP backend, an unsaturated primop, a lost local, an unboxed tuple's size).
- **Left**: constraint tuples' variable superclasses, unboxed sums in Unarise, the bignum backend of lib-check's dumps, the `-O2`
  ABI against the `-O0` reference (SrcLoc), the Simplifier's case-of-case into join points where GHC keeps the case, foreign
  imports. Proposed: HF10-P6 (the dumps with `-DBIGNUM_NATIVE`), P7 (the pack's data until P3), P8 (unboxed sums), P9
  (constraint tuples, recommended later), P10 (UTF8's Simplifier time, then levels 31 to 60).
- Alternatives considered: the judge against the reference's `-O0` data while the runs use the pack's (rejected for now, P7: an
  equal binding would not be the one that runs); renaming the constraint tuples' constructors at the output only (tried: it
  collided with the class's own worker, reverted); `ip` inlined by an unfolding (tried: GHC's `-O0` keeps `ip d` and `C:IP` to
  CorePrep, the dump shows it, reverted to that).

## 2026-11-08: SCS7 (c), SCS8 (b), Scheme's corpus files by the report and Larceny's benchmarks by their own checks (branch `scheme-props`, from `mac/next` 5ec43f7b7)

The user's decisions of 2026-11-08 (about 19:45) on scheme-scs4's proposals (doc/scheme/SCHEME-SURVEY.md, "As built: SCS7 (c), SCS8
(b)"). SCS7 (c): `arbace.repr.scheme.check/judge-file` judges each corpus file by R6RS's grammar (`arbace.repr.scheme.report`, SCS5's) where
it decides the whole text, its Chez dump unread: 112 of the 115 files; the 3 with a complex number of mixed exactness (Racket's base.sls and
io/ports.sls, Chibi's r7rs-tests.scm) by their dumps; `bin/repr-scheme report-check` reads the files three ways too (112 agree, 3
undecided). The scans stay on their dumps and were measured (`bin/repr-scheme report-scans`, `test/scheme/report-scans.edn`): the report's
character classes over Java's Unicode tables (jrt's, jdk26u, Unicode 17.0), each confirmed by the report's reading of the scan's text on
every differing value and a sample, against Chez 10.3.0's: whitespace equal; `ident-initial` 4,790 and `ident-subsequent` 4,803 code points
only the report's, none only Chez's: exactly Unicode 17.0's new characters (CJK Extension J 4,298, Tangut's supplements, Tai Yo, Beria Erfe,
Tolong Siki, Sidetic and 17.0's additions elsewhere); Chez's tables are 16.0's, no category differs. SCS8 (b): `split-libraries` puts the
leading library forms of Larceny's earley.sch and nboyer.sch onto a library path of the run's own (as Larceny loads that layout; R6RS 8.1
begins a program with `import`), the program after them keeps its line numbers; bin/si's `split_program` at both levels; inputs of our own
(`test/scheme/si/inputs/`: earley 1 iteration, nboyer size 0); the judge `:bench` (`bench-items`): status 0, run-benchmark's banner with the
benchmark's name, no "Error: Benchmark program returned wrong result" (earley's 1430 trees, nboyer's 95,024 rewrites); judge-test's five
cases, the control `_control/nboyer-vs-earley`; run-benchmark.sch stays refused (Larceny's `(primitives run-benchmark)`). Chez's reference
runs of the two re-recorded by hand (`--record-ref --only`, the same bytes as si's). Alternatives considered: the scans by the report now
(not decided: the measurement first, SCS9); reading all 3.3 million scan texts by the report (did not end in 45 minutes: the classes with a
confirming sample instead); keeping earley and nboyer's refusal entries beside new split ones (rejected: run-benchmark keeps the refusal
tested); a golden output for the benchmarks (not needed: their authors' result checks are the verdict).

Checked: scheme-read-essential's command (1,358 files and probes, 106 files and 1,140 probes by the report, controls, report-test, round
trip, spec-check, corpora files), `--corpus whimsical --corpus scans`, `bin/si check` and `--aot` on their essential subsets (no Chez run
read, no regression, the four controls differ), `--record --only` for the two at both levels, `bin/si own` (2 of 2 agree), `bin/repr-scheme
c2j-check` (19 of 19), `bin/si corpora controls-test`, `bin/reap --record`, `bin/crops-score --record`, `--judge --strict` (0 problems).
Scheme's scores: completeness 97.3 (unchanged), maturity 100, independence 99.6 to 99.8 (judge 0.979 to 0.989). Night: `bin/repr-scheme
check --record`, `bin/si check --record`, `bin/si check --aot --record`. Proposed: SCS9 (the scans by a pinned UCD 16.0, the reader's tables
from it), SCS10 (si's choices where R6RS defers stated and applied by the report), SCS11 (earley and nboyer at the corpus's sizes, compiled).

## 2026-11-08: SCS9 (c), SCS10 (b), SCS11 (b), Scheme's scans by Unicode's data, si's reading choices stated, Larceny's benchmarks at their sizes (branch `scheme-props`, from b05140c26)

The user's decisions of 2026-11-08 (about 21:00) on SCS9-SCS11 (doc/scheme/SCHEME-SURVEY.md, "As built: SCS9 (c), SCS10 (b), SCS11 (b)").
SCS9 (c), with the network allowed for this one fetch: the Unicode Character Database 16.0.0's `UnicodeData.txt` and
`extracted/DerivedGeneralCategory.txt` from https://www.unicode.org/Public/16.0.0/ucd/ and Unicode's license (https://www.unicode.org/license.txt,
Unicode License V3; the version directory has none) fetched into a new directory, the main checkout's `.tmp/scheme/ucd-16.0.0`, data only,
pinned by SHA-256 in `test/scheme/ucd.edn` (UnicodeData.txt `ff58e5823bd095166564a006e47d111130813dcf8bf234ef79fa51a870edb48f`,
DerivedGeneralCategory.txt `7676ab755a41ef82108460238569e60ad65c191ddafe61b36c6765ec1353f293`, license
`e7a93b009565cfce55919a381437ac4db883e9da2126fa28b91d12732bc53d96`), the license and the files it covers in LICENSE.md, the generated files'
origin in doc/ARBACE.md. PropList.txt was not fetched: R6RS 4.2.1's classes are by general category alone. `arbace.repr.scheme.ucd` reads
UnicodeData.txt (its First/Last ranges expanded, unlisted Cn), checks it against DerivedGeneralCategory.txt code point for code point (equal),
and generates `arbace/repr/scheme/category.ae` (3,368 runs: the report's categories, in place of `Character/getType`'s Unicode 17.0),
`arbace/repr/scheme/unicode.ae` and `go/arbace/scm/unicode.ae` (the readers' identifier ranges by R6RS's rule, stated in the generator apart from
the report: number for number the ranges SC21 took from Chez); `bin/repr-scheme ucd fetch|gen|check`, `pins check` covers the pins, the gate's
`scheme-read.scans` runs `ucd check`. The check judges the three reading scans by the report's classes (no dump read), the four printing scans
by Chez's dumps; `report-scans` is now a check (3 of 3 equal to Chez's; SCS7's 4,790 and 4,803 differences were Unicode 17.0's, gone over 16.0).
SCS10 (b): doc/scheme/SPEC.md 12 states si's six choices where R6RS defers (`:mixed-exactness`, `:ratio-over-zero`, `:exact-naninf`,
`:mantissa-width`, `:fold-case`, `:hash-bang`), applied by `arbace.repr.scheme.report` (`choose`, `*choices*`, `:choices`); the polar numbers and
an exponent beyond the limits stay undecided. 48 probes and 2 files move off Chez's dumps (the brief's "about 75" counted the 24 polar probes
that stay; Racket's base.sls reaches a polar number past its mixed complex), each agreeing with Chez's dump (`report-check`). Checked against
Chez 10.3.0 (dump.ss) and si's runtime reader (`bin/si run`) by hand on 18 cases of their own, `test/scheme/choices.edn`, held at every check by
`choices-test`: three differences classed (`:fold-char-names`, `:fold-full`, `:chezscheme-directive`; SCS12 proposes closing them).
Alternatives considered for SCS10: changing repr/scheme's reader to Chez's folding now (not decided: SCS12), stating the polar numbers' choice
(not decided: the last bit of cos and sin, SCS14). SCS11 (b): `larceny/earley-1000` and `larceny/nboyer-5` (the corpus's inputs) with
`:aot-only` (bin/si's program list, `arbace.scheme.check/programs` at the interpreted level, reap's `:same-cases-extra`): compiled, both pass by
their own result checks (earley 61 s, nboyer 5 98.5 s to run, peak 1.45 GB), recorded at the AOT level and Chez's runs beside (`--record-ref`).

Checked: scheme-read-essential's command (1,358 equal; 1,188 probes and 108 files by the report, 48 and 2 by the choices; report-test,
choices-test, controls, round trip, spec-check, corpora files), `--corpus whimsical`, `--corpus scans`, `report-check`, `report-scans --record`,
`ucd check`, `bin/si unit`, `test --si`, `check` and `check --aot` on their essential subsets (controls differ, no regression), `r7rs`, `srfi`,
`bin/si own --aot`, `bin/repr-scheme c2j-check` (21 of 21), `bin/gate --full --plan`, `bin/reap --record`, `bin/crops-score --record`, `--judge
--strict` (0 problems). Scheme's scores: completeness 97.31 to 97.41 (98.84 to 98.88 without by design), maturity 100, independence 99.78 to 99.88
(judge 0.989 to 0.994). Night: `bin/repr-scheme check --record`, `bin/si check --aot --record`, `bin/si check --record`. Proposed: SCS12 (the
readers' folding and `#!chezscheme` as Chez's, full case folding from the pin), SCS13 (the readers' whitespace from the pin), SCS14 (the polar
numbers by a correctly rounded reference).

## 2026-11-08: SCS12 (c), SCS13 (b), SCS14 (b), Scheme's readers fold and space by Unicode's data, the polar numbers by a correctly rounded reference (branch `scheme-props`, from ccc0eadb8)

Decided by the user (about 21:30; doc/scheme/SCHEME-SURVEY.md, "Decided" and "As built: SCS12-SCS14"). One more fetch, by the user's allowance:
https://www.unicode.org/Public/16.0.0/ucd/CaseFolding.txt (86,092 bytes, SHA-256 `6f1f9c58...599e09bb`, `CaseFolding-16.0.0.txt` of
2024-04-30) into the main checkout's `.tmp/scheme/ucd-16.0.0`, pinned in `test/scheme/ucd.edn`; Unicode License V3 as for SCS9's files (LICENSE.md,
doc/ARBACE.md extended). SCS12 (c): `arbace.repr.scheme.ucd` reads it (full folding C + F, the Turkic T left out; simple C + S) and generates
`arbace/repr/scheme/casefold.ae` (1,557 foldings) and si's `FoldCase`, `FoldRunes`, `FoldRune` in `go/arbace/scm/unicode.ae`; repr/scheme's reader,
the report and si's runtime reader fold identifiers and character names after `#!fold-case` by them (before `String.toLowerCase`, Go's
`strings.ToLower`) and take `#!chezscheme` with no effect on the reading; si's `char-foldcase` and `string-foldcase` use them too, replacing Chez
10.3.0's recorded answers (measured first: equal entry for entry, 1,484 simple and 1,557 full). Found and fixed: si's `Port` was no `FoldState`, so
`#!fold-case` was lost between two reads of a port. choices.edn's three classes are gone; 15 cases added (recorded by hand with Chez's dump.ss and
`bin/si run`); one narrower class, `:chezscheme-extensions` (an extension after `#!chezscheme`: Chez's and si's runtime's syntaxes, not R6RS's),
proposed as SCS15. SCS13 (b): the generator states R6RS 4.2.1's `<whitespace>` and `<intraline whitespace>` over the categories into the reader's
`unicode.ae` and si's file; `Character/getType` and Go's `unicode` tables are out of both readers (no reading changed). SCS14 (b): SPEC 12's
`:polar` (an exact zero magnitude the exact 0, `#e` otherwise no number, else m cos a + i m sin a with the cosine and the sine correctly rounded);
`arbace.repr.scheme.report`'s `cr-cos`, `cr-sin` by integer arithmetic (Machin's pi, reduction at a precision covering the exponent, truncated
Taylor series, each step's bound stated) and Ziv's rounding test. Alternatives considered: a property judge (within one ulp, SCS14 (c): hides
the last bit), BigDecimal's or a library's functions (no proven rounding on jrt), sharing the reference with the reader (would make the judge
circular; the reader keeps jrt's StrictMath and its last-bit differences are classed `:polar-last-bit`, verified to be one ulp and no more).
Validated against bc -l (70 digits, rounded by Python's float) on 2,008 angles: 4,016 of 4,016 equal; on the same angles StrictMath 95 values
off by one ulp, Chez (musl's libm) 87, si's runtime (Go's math) none; 16 of them held by report-test. The 24 polar probes and Racket's
base.sls are judged by the report now (report-check: `:polar` 25 agree with Chez, none undecided).

Checked: scheme-read-essential's command (1,358 equal; 1,212 probes and 109 files by the report, 37 probes by dumps; report-test, choices-test 33
cases, controls, round trip, spec-check, corpora files), `ucd check` 4 of 4, `--corpus whimsical --corpus scans`, `report-check`, `report-scans`,
`pins check`, `bin/si unit` (TestFoldAndWhitespace added), `test --si`, `check` and `check --aot` on their essential subsets (controls differ, no
regression), `r7rs`, `srfi`, `bin/si own`, `bin/repr-scheme c2j-check` (22 of 22), `bin/gate --full --plan`, `bin/reap --record`, `bin/crops-score
--record`, `--judge --strict` (0 problems). Scheme's scores: completeness 97.41 (98.88) unchanged, maturity 100, independence 99.88 to 99.92 (judge
0.994 to 0.996). Night: `bin/repr-scheme check --record`, `bin/si check --record`, `bin/si check --aot --record`. Proposed: SCS15 (`#!chezscheme`'s
extensions: leave), SCS16 (si's `(rnrs unicode)` from the pin), SCS17 (the printing scans by stated rules), SCS18 (the 37 probes where Chez reads
past the grammar refused as R6RS does).

## 2026-11-08: HFS-I11 (a), the system libffi derived; HFS-I9 (b), the probe as a --full step; HFS-I8 (a), HFS-I10 (a) decided (branch `hs-hfsi9`, from next 5ec43f7b7)

The user's decisions on hs-inputs' proposals (doc/haskell/FRONTEND-SURVEY.md, "Decided (the user, 2026-11-08: HFS-I8 to HFS-I11)"),
built as "As built: HFS-I9 (b), HFS-I11 (a)".

- **HFS-I11 (a)**. GHCi/FFI.hsc's C includes `<ffi.h>`, the system libffi's (rts.cabal's +use-system-libffi), so its hsc2hs output was the
  last BORROWED member of the derived installation. Fetched once (the user's one network allowance) the latest 3.x release, libffi
  3.8.0 from https://github.com/libffi/libffi/releases/download/v3.8.0/libffi-3.8.0.tar.gz into a new directory of the main checkout,
  `.tmp/repr-haskell/libffi/`; SHA-256 `7da3e2d9a171eb0a038f592ecad3ff2bb2550f3496d87b3b29ad0cf4430c0db4` (GitHub's release API lists the
  same digest), pinned in `test/haskell/target/libffi.edn` (with SHA-512, size, `:host x86_64-alpine-linux-musl`). The tarball is data:
  `arbace.repr.haskell.target`'s `libffi-vars` ports configure.ac's and configure.host's rules for ffi.h.in's six `@VAR@`s (3.8.0, X86_64,
  HAVE_LONG_DOUBLE 1, 30800, FFI_EXEC_TRAMPOLINE_TABLE 0; sizes from `c/target/abi.edn`) and `libffi-headers` writes ffi.h and copies
  src/x86/ffitarget.h, into the derived tree's `sysroot/usr/include`, listed in a new `SYSTEM` file that `target compare` leaves out.
  Both equal the host's libffi-dev 3.8.0-r0 headers byte for byte. `target hsc` puts that directory last on every module's ci run, and
  runs `bin/ci musl ensure` first (a fresh tree failed its ghci modules for want of the forms cache). Results:
  `REPR_HASKELL_ONLY_PACKAGES=ghci bin/repr-haskell target hsc` 3 of 3 on ci (1 GiB), GHCi/FFI.hs byte for byte the pack's (3abe5950...);
  the tree with the 90 modules made on ci against the pack: `target compare` 337 of 337 equal, nothing borrowed. libffi's MIT License
  in LICENSE.md, its origin in doc/ARBACE.md. Alternative (b), an `ffi.h` of Arbace's own written from the ABI, was not taken: the user
  chose the faithful one.
- **HFS-I9 (b)**. The probe and the tarball's extraction moved to `bin/repr-haskell-target` (`src`, `probe [--verify]`), so that its key is
  not bin/repr-haskell's (a `bin/` script: the planner's `:cmd` check reads `bin/NAME`); `bin/repr-haskell target probe` and `target_src`
  run it; `make repr-haskell-target-probe`. The new `--full` step `haskell-target-probe` (steps.edn, 2 GiB, `:stage0 :go`) is keyed on the
  script, configure.ae, answers.edn, the GHC tarball's and musl's pins and C's tools and target (186 files); its control waived (the
  fresh answers against the committed file); the oracle fragment's `haskell-target-oracle` keeps `target hsc && target compare`.
  `bin/repr-haskell target probe --verify` by day through the moved script: the answers as committed, 36 min, 0.9 GiB.
  `gate.crops check` 0 problems; `bin/gate --full --plan` runs it ("never passed"), among the chains beside the serial run;
  `bin/gate-tests`: crops-test's expected chains gained it, then 36 of 36 (gate-reads-test rerun alone).
- **HFS-I8 (a)** and **HFS-I10 (a)**: recorded as decided, nothing to change (both as built by HFS-I2 (a)).
- **Scores**: Haskell's read fact `:cases {:self 1345 :recorded 0}` (read 0.9996 to 1.000); `bin/crops-score --crop haskell` 96.6 / 100 /
  78.8 (0.78840 to 0.78847, `test/crops/scores.edn` unchanged at four places), `--judge --strict` 0 problems.
- Night: `bin/repr-haskell target hsc` whole (the key moved with libffi.edn: about an hour of ci), then `bin/repr-haskell target compare`
  (337 of 337 expected) and `bin/gate --full` (haskell-target-probe's first pass, haskell-roundtrip). Proposed: HFS-I12 (the probe's
  negative control), HFS-I13 (the clean run's two hours of ci).

## 2026-11-08: HFS-I12 (a) built, HFS-I13 (a) recorded (branch `hs-hfsi9`, from 88af58be2)

The user accepted HFS-I12 (a) and HFS-I13 (a) (doc/haskell/FRONTEND-SURVEY.md, "Decided (the user, 2026-11-08, later: HFS-I12,
HFS-I13)"; "As built: HFS-I12 (a)"). `haskell-target-probe`'s waiver is replaced by a control: two planted compile tests,
`test/haskell/target/controls/control-{no,yes}.c`, which `bin/repr-haskell-target controls` runs through `bin/ci diagnose` and
which the probe runs first; a flipped answer fails it in about 20 seconds. Wired in steps.edn (`:controls`, the directory in
`:inputs`). Checked by flipping each planted answer once by hand (REPR_HASKELL_CONTROLS): the probe's control fails each time
with the flipped test named. Alternatives: planting beside the 308 in the one diagnose run (rejected: a flip shows after 36
minutes); keeping the waiver. HFS-I13 (a): nothing to build. the controls passed first, then the 308 compile tests, types and values: "the answers as committed", 33 min wall (40 min CPU), one process.

## 2026-11-08: LLS33 (b), the pack rt's mutants judged by their own verdicts; LLS34 (b) (branch `llvm-lls33`, from next 5ec43f7b7)

The user's decisions of 2026-11-08 (about 22:00) on llvm-rt-pack's proposals. LLS33 (b): each rt-family mutant carries its own verdict,
written from the rule its kind breaks. LLS34 (b): the pack keeps the first records' set, with no `win` build for the four C++ samples.

What was built:
- **The verdict line.** `arbace.repr.llvm.mutate`'s 41 kinds attach what they changed to the mutant's forms (metadata). The new
  namespace `arbace.repr.llvm.expect` derives the verdict and message from LLParser's and the Verifier's rules, and `bin/repr-llvm
  mutants` writes them as the mutant's first line, `; MUTANT: KIND VERDICT LAYER: MESSAGE` (or `valid`, or `unknown: WHY`). The rules
  are ported from llvm-project 23.1.5 (the pinned `.tmp/llvm/llvm-project-23.1.5.src.tar.xz`: llvm/lib/AsmParser/LLParser.cpp,
  llvm/lib/IR/{Verifier,AutoUpgrade,Attributes,DiagnosticInfo}.cpp, llvm/include/llvm/IR/{Attributes.td,CallingConv.h}; Apache-2.0
  with LLVM exceptions; LICENSE.md, doc/ARBACE.md), each with its line:
  - LLParser's naming, as `parse-sim` (PerFunctionState's getVal, setInstName, defineBB, checkValueID, checkValidVariableType,
    finishFunction);
  - the Verifier's visiting order, and its dominance (none in unreachable blocks, none of a value wrapped in metadata);
  - verifyParameterAttrs, verifyFunctionAttrs and typeIncompatible, check by check;
  - visitFunction's convention switch, verifyMustTailCall, visitCallBase's bundle loop;
  - the personality and unwind checks, module flags, the DI REQUIRED fields, the debug records' checks;
  - AutoUpgrade's replacement of an intrinsic declaration's attributes, and the Debug Info Version message.
- **The judge.** `bin/repr-llvm`'s own_verdicts reads the line. verify-check holds the mutant to it; opt's record no longer judges it.
  `bin/repr-llvm mutants-oracle` checks the lines against opt, record or live: the oracle tier.
- **Two verifier bugs, found by the own verdicts.** `arbace.repr.llvm.verify` took a local wrapped in metadata as no use; LLParser
  resolves it (parseValueAsMetadata, LLParser.cpp:3352, 6750). Its walk of instruction locations counted old `llvm.dbg.*` calls, which
  are records by then, and functions without a subprogram (Verifier.cpp:3313-3335). Both fixed.
- **Not built.** No gate step changed. The pack, its records and the cells' switch stay LLS31 (a)'s night.

Proved by day on 41 modules:
- the sets: 1,872 mutants at the pack's set indices (680 of the base family, 1,192 of every kind);
- own verdicts: 1,696 (base family 99.4%, every kind 85.6%);
- against Arbace's verifier (verify-check in own mode, the stubs first on PATH, no LLVM tool called): all 1,696 met;
- against live opt 23.1.5 (mutants-oracle, by hand, the oracle tier's): 1,696 of 1,696, verdict and message;
- unchanged: verify-llvmtest 792 of 806, verify-probes 472 of 478, roundtrip-llvmtest and text-llvmtest 304 of 304,
  `bin/llvmi check --essential` 10 of 10, controls-test all ok.

The 176 unknowns (intr-decl and intr-rename 30 each, md-attach 28, di-retarget 20, di-drop 17, ...) stay on opt's record. LLVM IR's
scores are unchanged now (independence 96.9, judge 0.843: the rt families are not recorded). Projected on the night's records:
without LLS33, judge 0.728 and independence 94.6; with it, 0.878 and 97.6 (test/crops/independence.edn's comment).

Alternatives considered:
- the verdict in a sidecar table of the pack's manifest, rejected: the line keeps the verdict with the test, as llvm/test's RUN and
  CHECK lines do;
- the kind alone deciding the message, rejected: a kind's message depends on where it acts (hoist above a definition only used in
  metadata is valid, an `entrybr` in an unlabelled entry renumbers);
- `valid` claims left out, rejected: they are rules too (AutoUpgrade, the debug-intrinsic upgrade), and opt agrees on all.

Proposed: LLS35 (mutants-oracle as a part of the oracle tier's llvm-records-verify), LLS36 (port the unknown kinds' rules), LLS37 (a
mutant of an invalid base).

## 2026-11-08: GTS81 (a)'s controls for Clojure's and Go's last four families (branch `controls-cg`, from next 5ec43f7b)

`bin/crops-score` on `mac/next` gave Clojure's maturity item `:controls` 0.7778 (7 of 9 families) and Go's 0.7500 (6 of 8):
no negative control for "repr/clojure's one-way rules" (the read cell's source of `test/reap/cells.edn`, the rules' claims),
"repr/clojure's round trip" (print), "native-check's known differences" (Go's read, `:within` native-check) and "bin/repr-go
roundtrip --dirs linux/amd64" (Go's print). Each now has one, a crossed pair judged by the family's own judge (GTS81 (a)), a
control that passes failing the check; doc/GATE-SURVEY.md, "As built: GTS81 (a), Clojure's and Go's last families":
- the rules: `arbace.repr.clojure.rules/round-trip` (the samples' judge, read by the rules and printed back byte for byte) and
  `control-verdict` over `test/repr/clojure/rules-controls.edn`, one pair a rule, run by print_test's `deftest controls` in
  `bin/repr-clojure check`; a vacuous control (the rule does not rewrite its subject) fails too;
- the round trip: `arbace.repr.clojure/control-verdict` (the subject's text that comes back, `roundtrip`'s new `:texts` for
  `:keep`, against the reference's original, then `known-matches?`, run-roundtrip's known-entry judge factored out) over
  `test/repr/clojure/roundtrip-controls.edn`, one pair for each of the eight corpora, `--controls` from `bin/lib/repr-clojure/check`;
  `test/repr/clojure/judge-test.bash`'s case `controls`;
- native-check's known differences: the control level `:known` of `arbace.repr.go.annotate/control-verdicts`, the subject's crossed
  difference judged by `known-difference?` (check-corpus's `known?`, factored out) against the reference's own entries, listed
  by the four controls of `test/repr/go/native-controls.edn`;
- the directory tests' round trip: the kind `dirtest` of `bin/lib/repr-go/check --controls` (the subject's printed FILE.dir staged
  under the reference's `.go` names, gocmp -dirtest at the tree level), `test/repr/go/roundtrip-controls.txt`'s closure5.go and
  ddd2.go both ways; `test/repr/go/controls-test.bash` covers both Go controls.
Alternatives considered: pointing the two "within" families' `crops.edn` rules at the existing files (native-check's pairs,
the round trip's package lines), rejected as vacuous: those controls never reach the known-entry judge or gocmp -dirtest;
a control of a one-way rule's loss alone (a sample that must not come back), rejected as not a crossed pair.
Checked light (stage 0, a few files each, the suite and jars copied from the main checkout's `.tmp/`): every real control holds,
every fabricated one (a file against itself, a judge broken on purpose: `known-difference?` excusing anything) fails its check;
`bin/repr-clojure check --roundtrip` whole (61 s) passes. Found and fixed: that round trip failed on `mac/next` (arbace/'s
`:outside` 152, recorded 151: `genclass.ae`'s comment naming upstream's `clojure/genclass.clj`, from CDR3, a4258506f);
`roundtrip-known.edn` records 152. `crops.edn` names the four by `:match`; `bin/crops-score --record` (Clojure and Go maturity
96.3 and 95.8 to 100), `--judge --strict` 0 problems, `bin/reap` 0 problems. The gate's key moves (87234529dd27 to 8d0f7834608c,
three embedded namespaces) and javac's (34e3a3200c4e to e556b7de7898), not the runtime's. Night: the `repr-clojure`, `repr-go`
and `repr-go-native` steps of `bin/gate --full` run the controls over the whole corpora.

## 2026-11-08: C's completeness, the deferred classes on the checkout's ci (branch `c-compl`, from next 5ec43f7b7)

The brief: raise C's crops-score completeness (95.1%, 98.7% without by design) by its largest deferred and pending classes. Measured
first: of the 167 deferred items, sixty were the seed's ci in the records (T.1 of the own target's triage): the complex integers, the
VLA sizeofs, `__builtin_va_arg_pack`, signals, static and thread-local compound literals, threads and the twelve
`:ci-run-consolidation` cases pass with the checkout's ci (`bin/ci check --only` and `--aot --only` on 77 programs). Fixed, each after
gcc 15.2's own code (c-decl.cc declspecs_add_type and build_enumerator, c-typeck.cc c_alignof_expr, set_init_index, do_case and
output_init_element, c-common.cc c_add_case_label, libcpp's search_path_head and cpp_classify_number; no code copied): `__alignof__`
of a declaration reads its merged DECL_ALIGN (align-3), an overwritten initializer's side effects are dropped (embed-12), the bool
keyword specifiers, an enumeration redefinition's range, a non-integer array designator, a no-argument attribute's arguments, a case
range's conversions and overlap, and `#embed <...>` with no embed directory in gcc's words (eight `:wrong-reason` files), C23's
decimal suffixes d32, d64, d128 and d64x in the converter and the printer, with `DEC64X_` in Arbace's float.h (six dg and six
round-trip files). The `--only` runs' rows merged into the records (the night's whole records replace them), four stale entries of
`test/c/check-known.edn` and six of `test/repr/c/roundtrip-known.edn` removed. Alternatives considered: re-recording whole corpora by
day (against the day's rules; the night's T.7 list covers it), CS278 (a) for 95_bitfields (not built: the re-entered copies share one
CFile, so a pragma needs stream-order stamping, proposed as CS290), turning the converter's refusals in error tests into diagnostics
(CS291) and a dg-wording family for the parser's syntax-error wording (CS292), proposed rather than done. Checked: dg `--essential`
92 of 92, the essential eval and AOT lists 0 failing, `bin/repr-c check --c23` 59 / 3 / 0, `bin/ci judge-test`, `bin/reap --record`
0 problems, `bin/crops-score --record` and `--judge --strict`. C: 95.1% to 95.9% (98.7% to 99.6%), maturity 100, independence 99.7.
Details: doc/C23-SURVEY.md, "As built: C's completeness, the deferred classes measured on the checkout's ci".

## 2026-11-08: RF10's second slice, closures with their captures (RFP20 (a)), operators, constants and the format machinery's MIR (branch `rust-rf10b`, from next `5ec43f7b7`)

The brief: RF10's next items in their stated order (doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (first slice)", slices 1 then 2),
measured as the first slice measured (`bin/rust-front build-check`: our built MIR against rustc 1.99.1's `-Zdump-mir=built`, byte for
byte), the biggest group of failing bodies first; RFP22-RFP24 not built (they await the user). Measured on the first slice's corpus (88
programs, 510 bodies, 92 equal): closures were the biggest group (212 bodies), then format_args!'s lowering (75 refused), so slice 4's
built-MIR half was taken after slice 1 (it needs no PromoteTemps at the `built` phase), then slice 2's operators and constants, then
the tail. RFP20 (a) first, as decided: the helper's `--captures` (`tools/rsdump/front.rs`) and a port of `rustc_hir_typeck`'s
`upvar.rs` and `expr_use_visitor.rs` with `rustc_passes`' `upvars.rs` into `arbace/rust/front/build.ae`, judged by `bin/rust-front
capture-check` (`capture_check.ae`): 173 of 174 closures equal (kind, captures, fake reads; the one left inside a user macro's
expansion). Then closure bodies and expressions (`capture_upvar`, `UpvarRef`'s resolution, `insert_upvar_arg`, `limit_capture_mutability`),
calls through the Fn traits, overloaded operators, derefs and indexing (`overloaded_operator`, `overloaded_place`), slices' bounds
checks (`PtrMetadata`, fake borrows), drop-and-replace, repeats, floats, chars, byte strings, named constants, allocations
(`allocate_bytes_dedup`, `write_allocations`), canonical user type annotations (`write_user_type_annotation_from_args`,
`instantiate_method_args`, `TypeOf(.. at <impl ..>)`, `Ty(..)` lets with their locals' projections and ascriptions), std macro tokens'
places kept through expansion for those annotations' spans, lint scopes (`maybe_new_source_scope`), `super let` and lifetime extension
(`resolve_local`). The helper's `--std-iface` gained the lang item ADTs no public module reaches (`core::fmt::rt::Argument`) and
enums' repr and discriminants: `test/rust/front/std-iface.sha256` re-pinned (only additions). The judge counts a body equal but for its
allocations' ids apart (the ids are the session's interning order, which follows the order rustc builds bodies in, not the source's).

Results: build-check 92 → 361 of 510 bodies equal, 25 more but for the allocations' ids (4 before but for their text), 5 → 51
programs whole; the samples a1-a7 51 of 54 and 1 (47 and 1 before). Regressions checked: `typeck-check` 11,140 → 11,144 (`--ours`
11,138 → 11,142), `hir-check --ours` 90 and 1 but for whitespace (unchanged, after regenerating expand2's oracle, whose path length
changed with the worktree), `expand-check` 92 of 92 crates (unchanged), `run-built` 6 of 6 no_std samples from Arbace's MIR, stubs'
log empty (unchanged); `bin/crops-score` Rust 75.0 / 95.8 / 80.0 (RES54 (a): unchanged until RF12). Alternatives considered: slice 2
before format_args! (rejected: the format machinery blocked most of the remaining bodies, and the brief asks for the biggest group
first); capture analysis inside typeck replacing its closure-kind estimate (deferred: typeck-check depends on it and the port runs on
the resolved results, as rustc's runs after the main typeck); allocations judged by content only (kept as a class apart, RFP25).
Proposed: RFP25 (the allocations' ids), RFP26 (the helper's std interface facts), RFP27 (a gate step for the judges).

## 2026-11-08: the user's decisions on RFP22-RFP24, recorded (branch `rust-rf10b`)

RFP22 (a): RFP20's captures judged first, then RF10's slices 1-2 on the corpus, then slice 3; RFP23 (b) then (c): the run's symbols,
instances and layouts as packs first, then ported; RFP24 (a): no Rust front-end gate step until RF11, (b) at RFP23 (b). The second
slice (entry above) already followed RFP22 (a)'s order; recorded in doc/rust/FRONTEND-SURVEY.md, "Decided (2026-11-08, the user:
RFP22-RFP24)"; RFP27 rewritten to ride with RFP24 (b). Next by that order: slice 3.

## 2026-11-08: RF10's third slice: the rest of slice 2 and slice 3's first passes (branch `rust-rf10b`)

In the user's order (RFP22 (a)), continuing `rust-rf10b` from `d9a0245e2`. Sources: rustc 1.99.1 (`58cf8f9c3`, Alpine's
rust-src-1.99.1-r0 pinned by `tools/rsdump/apks.sha256`), MIT OR Apache-2.0, taken under MIT (LICENSE.md, doc/ARBACE.md).

Slice 2's rest, in `arbace/rust/front/build.ae`: the match lowering replaced by a port of `compiler/rustc_mir_build/src/builder/
matches/{mod,match_pair,buckets,test,util}.rs` rule for rule (FlatPat and squash_inter_pat, candidates with subcandidates for
or-patterns, expand_and_match_or_candidates, merge_trivial_subcandidates, test_remaining_match_pairs_after_or,
partition_candidates_into_buckets with choose_bucket_for_candidate's whole matrix, perform_test for switches, ranges, slice
lengths, string and scalar equality, lower_match_tree's false edges, bind_pattern, bind_and_guard_matched_candidate with
collect_fake_borrows, the guards' RefForGuard locals and guard_context, fixed_temps); patterns of `thir/pattern/{mod,const_to_pat}.rs`
(ranges, slices and arrays, `..` with enumerate_and_adjust, box, `&str` constants); let-else (`builder/block.rs`) and `if let`/`while
let` (`lower_let_expr`); construct_const for constants and statics, StaticRef with the statics' allocations (literal, array and
`&str` initializers, provenance printed as write_allocation_bytes prints it), anonymous constants of array lengths and repeat counts
(numbered per owner, those in macro calls after the rest as def collection meets them during expansion); functional update, union
aggregates, write_box_via_move, unit-variant casts (discriminant_def_for_variant), byte and unescaped string literals, the
coercions' kinds; in typeck the builtin DiscriminantKind projection, range endpoints typed, references peeled for range and slice
patterns, written arguments' elided regions canonical; std ADTs' needs_drop by their Drop impls; an inherent impl's own regions
printed in value paths. Slice 3's start: `arbace/rust/front/passes.ae` ports mir_built's passes after the `built` dump
(`compiler/rustc_mir_transform/src/{lib,lint_and_remove_uninhabited,simplify}.rs`: LintAndRemoveUninhabited, SimplifyCfg-initial with
remove_dead_blocks); `bin/rust-front build-oracles` dumps `-Zdump-mir=built|SimplifyCfg-initial` (`RF_DUMP_MIR`; the built dumps
regenerated byte for byte equal), and `BUILD_PASS=SimplifyCfg-initial bin/rust-front build-check` judges after the pass
(`make rust-front-pass-check`).

Results: build-check 361 → 415 of 510 bodies equal, 25 → 29 but for the allocations' ids, 51 → 58 programs whole, refused 26 → 13;
a1-a7 54 of 54; after SimplifyCfg-initial 379 of 475 and 29 but for the ids. typeck-check 11,144 (`--ours` 11,142), hir-check
--ours 90 + 1, expand-check 92/92 and 857/857, capture-check 173/174, run-built 6/6: unchanged. Alternatives considered: extending
the old match lowering case by case (rejected: or-patterns and guards change the candidate structure, so the port is the shorter
road and keeps the block numbering rustc's); PromoteTemps before SimplifyCfg-initial (rejected: rustc runs SimplifyCfg-initial in
mir_built, before mir_promoted); a structural needs_drop guess for private std ADTs from their arguments (tried, rejected: it
dropped JoinInner's Arc; kept "needs drop" and proposed RFP28). The user's decisions RFP25 (a), RFP26 (a), RFP27 (a) recorded in
doc/rust/FRONTEND-SURVEY.md, "Decided (2026-11-08, the user: RFP25-RFP27)". Proposed: RFP28 (the private std ADTs' fields in
`--std-iface`), RFP29 (the crate hash in MIR text).

## 2026-11-08: RF10's fourth slice: slice 3's passes from mir_promoted to runtime MIR's cleanup (branch `rust-rf10b`)

Continued `rust-rf10b` from `8052a77c0` (doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (fourth slice)"). Sources: rustc 1.99.1
(`58cf8f9c3`, Alpine's rust-src-1.99.1-r0 pinned by `tools/rsdump/apks.sha256`, which carries `compiler/`), MIT OR Apache-2.0,
taken under MIT; rustc_apfloat 0.2.3 (`rustc_apfloat-0.2.3+llvm-462a31f5a5ab`, the host's cargo registry, `src/ieee.rs`),
Apache-2.0 WITH LLVM-exception (LICENSE.md, doc/ARBACE.md).

`arbace/rust/front/passes.ae` gained the pipeline of `rustc_mir_transform/src/lib.rs` from PromoteTemps to
SimplifyCfg-pre-optimizations (`passes/pipeline`, 21 passes): PromoteTemps (`promote_consts.rs`' collector, Validator and Promoter over
`rustc_const_eval`'s qualifs; promoted bodies join at ImpossibleClauses), CleanupPostBorrowck, RemoveNoopLandingPads, the SimplifyCfg
variants with strip_nops and the runtime phases' simplify_branch, Derefer and ElaborateBoxDerefs over MirPatch's statement insertion,
AddCallGuards, PostAnalysisNormalize's opaque types, ElaborateDrops (`rustc_mir_dataflow`'s move paths, MaybeInitializedPlaces and
MaybeUninitializedPlaces to their fixpoint, drop_flag_effects, and `elaborate_drop.rs`' DropCtxt: static, conditional, open drops with
their ladders, enums' switch blocks, destructors, Box's contents), AbortUnwindingCalls over fn_can_unwind, EraseDerefTemps,
LowerIntrinsics, RemovePlaceMention. `build.ae`'s statements carry their source scope, type forms their typeck type, locals whether
they are user variables, constants their allocations' ids (a body's allocations recomputed after promotion). `bin/rust-front
build-oracles` dumps all 21 passes (oracles regenerated for the 88 programs; constructs, edition2015 and lowered_table still fail in
rustc after dumping, as before); `BUILD_PASS=NAME bin/rust-front build-check` judges each pass to NAME against its own dump, by its
first phase (`passes/pass-phase`), `BUILD_FIRST=1` names each body's first unequal pass. Slice 2's tail: rustc_apfloat's float Display,
coercion casts (a cast to its operand's type a Use), a derive's tuple-struct path its constructor.

Results: after SimplifyCfg-pre-optimizations 499 of 616 bodies equal and 46 but for the allocations' ids, 60 programs whole; no pass
loses a body (every unequal body is unequal at built, but two promoted bodies with typeck's `Option<_>` gap); built MIR 415 → 418 of
510, refused 13 → 12; a1-a7 whole at built and after every pass. expand-check 92/92 and 857/857, hir-check --ours 90 + 1,
typeck-check --ours 11,142 of 11,173, capture-check 173/174: unchanged. RFP28 holds back 2 bodies (float-f32's and float-f64's
main), RFP29 1 (drops' main); neither built. Alternatives considered: judging each pass on rustc's input to it (the `.before.mir`
dump) instead of our own previous pass's output (rejected: it needs a MIR text reader and would hide a pass's error in the next);
ElaborateDrops by a structural guess of drop flags (rejected: the flags and their block numbering follow the dataflow's states,
so only the dataflow gives rustc's text); a structural hash for opaque types (taken for now, RFP33 proposes typeck's def id).
Found on the way: a `swap!` whose update function swapped the same atom looped forever (Derefer, ElaborateBoxDerefs: now one
`swap!` after the walk). Proposed: RFP30 (the pass judge in RFP24 (b)'s essential step), RFP31 (the oracles' size), RFP32 (std's
function attributes), RFP33 (opaque types' identity).

## 2026-11-08: RFP23 (b) and RFP24 (b) with RFP27 (a): the front end's oracle facts as packs, its essential step (branch `rust-rf10b`)

The user's decision (2026-11-08): RFP23 (b) first, the run's symbols, instances and layouts recorded as packs so that `run-built`
needs no helper at check time, then RFP24 (b), `build-check` over `test/rust/front/a*.rs` an essential gate step with its oracle texts
pinned, and RFP27 (a), `capture-check` in it. doc/rust/FRONTEND-SURVEY.md, "As built: RFP23 (b), RFP24 (b)".

What was done. `bin/lib/repr-rust/front-records.bash` (sourced by `bin/rust-front`, out of every key): four packs in the
oracle-records format (doc/ORACLE-RECORDS-SURVEY.md §3.2, `test/oracle-records/oracle.bash`'s `oracle_make`, `oracle_unpack`), made
by hand with the helper as rustc (`bin/rust-front records make`, 31 s; `:rustc b789fd432132e37d`, rustc 1.99.1 Alpine r0; helper
`6378914b5a9b`): `rust-front-std` (std-names.edn and std-iface.edn, their digests `test/rust/front/std-*.sha256`'s; 514,512 bytes xz),
`rust-front-built` (rustc's `.after.mir` dumps of a1-a7 after mir_built and each of the 21 passes, 1,108 members, 24,580 bytes),
`rust-front-captures` (the closures program's and the seven test/rust/front programs' `--captures`, 840 bytes) and `rust-front-mono`
(a1-a7's `--mono-mir` record with rustc's executable's status and output, the checkout's path written `@ROOT@`, 11,108 bytes); pins
and manifests in `test/rust/records/`, the packs in the main checkout's `.tmp/oracle-packs/`. Keys: the kind, the program's path and
bytes, the helper's source digest, the flags (and the passes); UNRECORDED and STALE fail (checked). `records verify` reruns every
entry and each kind's crossed pair (all equal, the four controls differing); kinds `rust front-*` in `test/oracle-records/kinds.edn`.
`build-check --records`, `capture-check --records` and `run-built [--records]` read the packs; `run-built` runs the lowered C on ci
(`bin/ci run`) instead of gcc (`RF10_CC=gcc` by hand), so the run needs no C compiler either. a6 (the unwinding no_std sample) runs
now: codegen ports `rustc_codegen_llvm`'s `volatile_store`/`volatile_load` and `codegen_static` (an internal global of a static's bytes),
and `build-run` carries our bodies' static, string and byte-string allocations into the run (the static's symbol from the record by its
name, its bytes our own evaluation). `bin/rust-front essential` (with rustc, rustdoc, cargo, gcc, cc and clang stubbed exiting 99):
build-check 54 of 54 bodies, capture-check 30 of 31 closures, run-built 7 of 7 programs (statuses 5, 212, 41, 17, 45, 6, 21), the three
controls differing, the stubs' log empty, held to `test/rust/front/essential-known.txt`; 6 min 21 s, 1.39 GB. The gate step
`rust-front-essential` (essential, chain rust-essential, `:mem-gib 2`, `:hosts [:stage0 :go]`, the packs' pins, control
`test/rust/front/controls.edn`) and the oracle tier's `rust-front-records-verify`; `gate.crops check` 0 problems, `bin/gate --plan` and
`--oracle --plan` show them; `bin/crops-score --judge --strict` Rust unchanged (75.0 / 75.0 / 95.8 / 80.0: the eval stage stays 0.5,
each run still consuming the mono record; `test/crops/independence.edn`'s Rust eval and judge facts name the step).

Alternatives considered: the facts committed in the tree as plain text (`test/rust/records/essential/`, as Scheme's) instead of
packs (rejected: std's interface is 18 MB raw; the brief asks the pack format); one pack for the four kinds (rejected: the kinds change
at different times, std's with RFP26's regeneration, the dumps with the programs); keeping gcc for the run (rejected: ci removes the
last host tool of the run, at about 45 s a program); taking a static's initializer from the helper (would need a helper change and a
re-record of every pack: our own evaluation is used, as the run judges it). Found on the way: `bin/ci musl ensure` with
`CI_MUSL_FORMS` pointing at the main checkout's forms cache deleted that cache when its key differed (this branch's repr/c is not
main's) and began rereading it; stopped at once and reread in the worktree instead. The main checkout's `.tmp/ci-musl/forms` is left
partial without its key: its next `bin/ci musl ensure` rereads it (`bin/ci check`, `bin/rsi check` and now `bin/rust-front essential` run ensure before
reading the forms; `bin/ci run` alone does not). Proposed: RFP34 (closures
in macro_rules! bodies: Span::to across contexts), RFP35 (the packs' key and the helper), RFP36 (the essential step's runs batched in one
ci session).

## 2026-11-09: RFP23 (c): the run's facts ported, the runs off rustc's mono record (branch `rust-rf10b`)

The user's decision of 2026-11-08 (RFP23 (b) then (c)): after the packs, port the run's facts. Built on `rust-rf10b` from `88ad9637d`.
Sources: rustc 1.99.1 (https://github.com/rust-lang/rust at 58cf8f9c3, Alpine's rust-src-1.99.1-r0 pinned by tools/rsdump/apks.sha256):
`compiler/rustc_symbol_mangling/src/{lib,v0}.rs`, `rustc_span/src/def_id.rs` (StableCrateId::new), `rustc_middle/src/{mono.rs,
ty/print/mod.rs,ty/instance.rs,ty/layout.rs}`, `rustc_mir_transform/src/cross_crate_inline.rs`, `rustc_metadata/src/rmeta/{mod,encoder}.rs`,
`rustc_attr_ir/src/weak_lang_items.rs`, `rustc_monomorphize/src/collector.rs`, `rustc_abi/src/{layout.rs,layout/simple.rs,lib.rs,
wrapping_range.rs}`, `rustc_ty_utils/src/layout.rs`; rustc-stable-hash 0.1.2 (`src/sip128.rs`, `src/stable_hasher.rs`, the version
rustc's Cargo.lock pins, read from the host's Cargo registry copy). MIT OR Apache-2.0, taken under MIT (LICENSE.md).

- `arbace/rust/front/mono.ae`: SipHasher128 and StableCrateId (a1's `0x799ecbb0b009f827` and `__rustc`'s disambiguator as rustc's on
  the first try: the stream is the name, 0xFF, "metadata", "exe", cfg_version `1.99.1 (58cf8f9c3 2026-10-13) (Alpine Linux Rust
  1.99.1-r0)`); an rmeta reader of the header and CrateRoot's first fields for core's, alloc's and std's ids (Cargo's -C metadata makes
  them uncomputable), written to `test/rust/front/std-crates.edn` by `bin/rust-front std-crates` (`--check`: equal); the v0 mangler
  with backrefs, impl paths, punycode, internal symbols and ReifyShims; codegen attributes (local from the HIR's parsed attributes,
  core's from rust-src's library/core through the module tree, parsed by repr/rust's native parser, cfg_attr evaluated for core's
  build); instantiation_mode and cross_crate_inlinable's attribute rules; the collector's roots, calls and drops (drop_glue::<T> and the
  local Drop::drop); callees as the helper writes them; the DefIndex order (injected items, derive items deferred, discriminant
  constants); rustc_abi's layouts (univariant with repr(Rust)'s reordering and the end-biased retry, niches, enum niche-filling and
  tagged layouts, pattern types, fat pointers) over the type checker's types and the record's type forms.
- The builder keeps each call's span on its callee operand's metadata (`build.ae`), for the caller locations; `build-check`'s
  `build-program` takes a continuation called within the program's bindings.
- `arbace/rust/front/mono_check.ae`, `bin/rust-front mono-check [--control]`: every native fact against the record. a1-a7: symbols 47/47,
  items 188/188, callees 34/34, locs 33/34 (panic!'s expansion), fn-symbols 33/33, statics 1/1, langsyms 113/113, layouts 221/223
  (ArgumentType, Argument: not in std's interface), core's items reached directly 1/1 (3 more through core's bodies); the control 66 of
  160 entries differing. Layouts matched rustc's on the first run of the port for every type of the seven programs' own locals.
- `arbace.rust.front.build-run`'s native path (the default; `RF_MONO=pack` RFP23 (b)'s): the module from the collector's items, native
  facts, the record read for core's bodies (a6's four) and the refused facts only, our own type forms. `run-built --records`: 7 of 7;
  a1-a5 and a7 take no fact from the record (its status and output only, the judge's reference). The control: a1 156 for 5.
- `bin/rust-front essential` adds mono-check's summary line (held in `test/rust/front/essential-known.txt`, recorded) and its control
  (`test/rust/front/controls.edn`); `steps.edn` pins `tools/rsdump/apks.sha256` for rust-src. The step: 8 min 14 s, peak 1.44 GB,
  stub calls 0; `bin/gate --plan`: run rust-front-essential (never passed at the new key); `bin/crops-score --judge --strict`: 0
  problems, Rust 75.0 / 95.8 / 80.0 unchanged: the runs are not the eval cell's cases, so the eval fact keeps 0.5 (its why updated).
- Alternatives: computing core's StableCrateId from Cargo's metadata hash (rejected: Cargo's inputs are not ours); reading core's
  attributes and cross_crate_inlinable from the rmeta's tables (RFP39 (b), deferred: rustc's product); taking the record's type forms
  (dropped: no record DefIds in our bodies, the runs pass). Proposed RFP37-RFP41 in the survey's "As built: RFP23 (c)".

## 2026-11-09 rust-front-essential extracts rust-src itself

- `bin/rust-front essential` (mono-check, run-built) read rust-src's `library/` from the worktree's `.tmp/repr-rust/apks/rust-src`, which only
  rust-read-essential's round trip extracted: with that step skipped at its key and this one rerun in a fresh worktree or the clean clone it
  failed with "no libsrc". Fix: `bin/repr-rust src-take` (rust_src_take: the pinned apk from `REPR_RUST_APKS`, else the main checkout's
  `.tmp/repr-rust/apks` by crop_take, checked against `tools/rsdump/apks.sha256`, extracted; never downloaded; a clear refusal when the apk is
  absent) and `bin/rust-front`'s `rf_libsrc_take` calls it when `$libsrc` is missing (`RF_MONO_LIB` still names another tree). Declared inputs
  need no change: `:rust` holds bin/repr-rust, take.bash and apks.sha256, the step pins apks.sha256, `bin/rust-front` is listed.
  Alternative: extracting in the step's command (rejected: the other callers, run-built and mono-check by hand, would still fail).

## 2026-11-08: night 10's merge stage rehearsed; haskell-target-oracle spliced (branch `night10-fixes`, from `mac/hs-hfsi9` 11ecb9ffa)

The second dry run of night 10's merge stage (`.tmp/night10/merge.sh next|java --dry`, a scratch worktree detached at mac/next
5ec43f7b7 and then at mac/java-night 639d42ab9; nothing merged into next or main, nothing pushed but this branch) took the
finished branches as they were on mac late on 2026-11-08 (go-tests, gate-lane2, cpp-compl, cpp-cx12, hs-hes84, hs-hf9,
ocaml-compl, cpp-props, hs-props, hs-hf10, scheme-props, hs-hfsi9, llvm-lls33, controls-cg, c-compl, rust-rf10b into next; java-more,
java-older, java-classfile, java-http and next onto java-night). The conflicts and their resolutions are recorded beside the script
(`resolve-BRANCH-FILE.{ours,theirs,merged}`, used only for the exact two blobs they were made from). One by-day edit the branches
left: `test/gate/crops/haskell.edn`'s `haskell-target-oracle` (HFS-I2 (a)'s `target hsc && target compare`, the oracle tier's) was
never spliced into `test/gate/steps.edn`. Spliced here with `gate.crops splice` over a fragment holding that step alone (its step,
its `:crops :haskell` entry, its waiver), as batch 4 spliced the rest of the fragment: `gate.crops check` 0 problems,
`test/gate/crops-test.bash` ok, `bin/gate --oracle --plan` lists it (never passed). Alternatives: splicing the whole fragment again
(rejected: its other steps are in the table already and would be twice); leaving it to a later batch (rejected: the oracle tier's
next run would not compare the derived installation).

The same rehearsal found one interaction: on the merged tree `bin/gate --oracle --plan` stops with a NullPointerException in
`gate.lanes/hm` (test/gate/lanes.ae:148). gate-lane2 prints the lanes' schedule with every plan, and `simulate` scheduled only the
`:essential` and `:full` phases, so the oracle tier's steps had no start. On `mac/gate-lane2` alone it fails the same way; on next
(no lanes printed for that plan) it does not. Fixed in `simulate`: the `:oracle` phase scheduled after `:full`, its steps one
chain (the oracle tier runs one after another, ORS8 (a)). Checked on gate-lane2 with the fix merged: `bin/gate --oracle --plan`
passes, `test/gate/lanes-test.bash` 50 checks, 0 failed. Alternative: printing no lanes for `--oracle` (rejected: the plan's
budget lines are wanted there too).

## 2026-11-09: RFP30 (a), RFP31 (a), RFP36 (c), RFP40 (c): the front end's essential step, 8.5 to 3.5 minutes (branch `rust-props`)

The user's decisions of 2026-11-09 (~06:15, RFP28-RFP41 all as recommended; doc/rust/FRONTEND-SURVEY.md, "Decided (2026-11-09)"),
the night-relevant four first, on `rust-props` from `rust-rf10b` at `c0f226f3b`.
- RFP31 (a): `build-oracles` deletes the `.before.mir` dumps; the pack `rust-front-built` keeps the `.after.mir` files the judge reads
  (each pass at its first phase, `rfr_judged`, in the key): 1,050 of 1,108 members, every kept member's digest unchanged, remade by
  hand with rustc 1.99.1 (Alpine r0) and the helper at the pinned digest, pin `36526aa2...`; `records verify built` equal.
- RFP36 (c), RFP40 (c): `run-built`'s batch (`arbace.rust.front.build-run`'s `batch`, `native-run`; the new `arbace.rust.front.ci-batch`
  over `arbace.c.run`'s `read-unit` and `nodes`, nothing of C changed): one process for every program's pipeline, mono-check, codegen
  and lowering, one for ci's node programs, cirun a program; the run control and mono-check's crossed control in the same runs.
  Alternatives: ci's `nodes-list` (one file a program: the driver concatenated, rejected for changing the C read; extending it would
  move C's keys), one process for both halves (rejected: ci must run evaluated from the checkout while the host's embedded copy differs,
  `c_ci_fresh`, which would evaluate the rust half too).
- RFP30 (a): the pass judge (`BUILD_PASS=SimplifyCfg-pre-optimizations build-check --records`) in the step, its 21 lines held in
  `essential-known.txt` (recorded, `--record`), its crossed control `_control/pass/crossed`.
- The step: 3 min 22 s (`--record`) and 3 min 38 s, from 8 min 36 s, the same summary lines but the 21 new ones, the five controls
  differing, 0 stub calls; `steps.edn`'s summary 32 lines; `gate.crops check` 0 problems; `bin/gate --plan`: run rust-front-essential.

## 2026-11-09: the rest of RFP28-RFP41 built (branch `rust-props`)

The user's decisions of 2026-11-09 (all as recommended), after the night's four; doc/rust/FRONTEND-SURVEY.md, "As built: the rest of
RFP28-RFP41". Sources: rustc 1.99.1 (rust-lang/rust 58cf8f9c3, Alpine's rust-src-1.99.1-r0, `compiler/`), as LICENSE.md and
doc/ARBACE.md name them.
- RFP29 (b): `build-check`'s class "but for the crate hash" (local DefIds' index and hash made one, allocations renumbered canonically);
  the builder prints a byte-array static's `<static(DefId(0:? ~ CRATE[????]::PATH))>` (`pretty_print_const_scalar_ptr`) and builds a
  closure's fake reads (`as_rvalue.rs`; refused before). The 88 programs at built: 418 → 420 of 510, `structs`' main in the class.
  Alternative (a), the exact index and hash from `arbace.rust.front.mono`, waits for RF11 as decided.
- RFP28 (a), RFP37 (a): the helper's `--std-iface` adds the std ADTs a written field names, transitively (182 lines, additions only);
  `std-iface.sha256` and the four packs re-made by hand with rustc (std `fe79f73b`; the other three the same bytes). float-f32/f64's
  `main` equal through every pass; mono-check's layouts 223 of 223.
- RFP32 (a): `si/fn`'s `:const`, `:promotable`, `:nounwind` (is_const_fn, is_promotable_const_fn, NEVER_UNWIND) read by PromoteTemps,
  const-fn promotion in initializers and fn_can_unwind; the hand table (whose `<impl i8>` paths never met std's `{impl#N}`) removed.
  The packs re-made (std `5116eb09`). The 88 programs after every pass: 506 of 616 (499), 63 whole (60).
- RFP33 (a): `[:opaque CLAUSES ID ARGS]`, revealed by the definer's body with the use's args, function constants' types revealed too;
  `test/rust/front/opaque1.rs` in the packs built (`3f84e0a6`) and captures (`e41dfc4e`), not mono (its run is RF11's): 8 of 8 through
  every pass (4 before). Alternative: keeping the structural match with a tie-break by signature position (rejected: not rustc's).
- RFP34 (a), RFP38 (a): fragments' metavariable spans (`mk_delimited`) and caller locations at `expansion_cause`; capture-check 174 of
  174 over every program, mono-check's locations 34 of 34; hir-check and typeck-check unchanged. Not ported: `tt` metavariable spans and
  the different-context fallback of `prepare_to_combine` (no sample meets them).
- RFP35 (a), RFP41 (a), RFP39 (a) then (c): as built, recorded in the survey.
- The essential step re-recorded after each change that moved a line (`essential-known.txt`): 8 programs built (62 of 62, the passes
  60 of 60), capture-check 34 of 34, run-built 7 of 7, mono-check every fact but core's closure (0 of 3, RF12's); about 3.5 minutes.
  `bin/crops-score --judge --strict`: Rust unchanged.

## 2026-11-09: correction (the packs' digests of the entry above)

The entry "the rest of RFP28-RFP41 built" names `rust-front-built` `3f84e0a6` and `rust-front-captures` `e41dfc4e`: those were made while
the sample was `a8.rs`; renamed `opaque1.rs` (kept out of the mono kind) the packs were made again, `c7005a5a` and `19e9947c`, the pins
committed with RFP33 (a). `bin/oracle-records pins`: the four Rust front packs in the store as their manifests say.

## 2026-11-09: C++'s CP302 (a), the code generator's light half in the gate (branch `cpp-props2`, from f9b81f1e3)

- **Decided** by the user (2026-11-09, about 06:15 UTC): CP300-CP305 and CP310-CP314 as recommended (doc/cpp/CPP-SURVEY.md, "As
  built: CP290-CP294" and "As built: CX12 (fifth slice)", their "Proposed"); CP302 first, for the night.
- **Built** (doc/cpp/CPP-SURVEY.md, "As built: CP300-CP314", CP302 (a)): `bin/cppi codegen-gen` (gen-list over test/cpp/codegen's 142
  samples, no clang or gcc; `arbace.cpp.codegen/gen-judge`), the records `test/cpp/codegen/refusals.edn` (rtti) and
  `test/cpp/codegen/forms.sha256` (141 digests of forms.ae), two `:level :gen` negative controls in test/cpp/controls.edn, the
  `--full` step `cpp-codegen-gen` (`:mem-gib 2`, chain cpp-full) in test/gate/crops/cpp.edn and test/gate/steps.edn, `make
  cppi-codegen-gen`. Why: the ten samples refused from a14d52531 (2026-11-04) went unnoticed four days, since only codegen-check
  (clang and gcc live) generated them.
- **Alternatives considered**: (b) the step in the essential gate (5 minutes a gate, rejected by the decision); digests of gen.c too
  (left: the lowering has its own steps, and a lowering change would re-record every sample); comparing the refusal's message (left:
  the class is the decision, the message may name generated symbols).
- **Checked**: the recorded run and a second gen-list run byte for byte; the judge passes the record, fails a changed digest and an
  unrecorded refusal; both controls fail as they must; `make check-makefile`, `gate.crops check`, `bin/gate --full --plan`,
  `bin/crops-score --crop cpp` (unchanged). One slip: a throwaway `grep ... 2>/dev/null` over scratch logs (only read).

## 2026-11-09: C++'s CP314 (a), C++11's constexpr function body (branch `cpp-props2`)

- **Built** (doc/cpp/CPP-SURVEY.md, "As built: CP300-CP314", CP314 (a)): in `arbace.repr.cpp.diag/constexpr-bodies`, at C++11 a
  constexpr function's body is one return statement beside null statements, static_assert, typedefs, aliases and using-declarations
  and -directives, a constructor's nothing (clang's CheckConstexprFunctionStmt's C++11 cases, llvm-project-23.1.5 from the pinned
  tarball; the corpus's g++ errors placed at the closing brace), nested compound statements their own error and flattened, a class
  template's constructors judged too.
- **Measured**: the every-10th sample unchanged (108 own passes, 92 extras); the C++11 constexpr files of the whole part: four gained
  (cpp0x/constexpr-type-def1, -diag5, -ctor19, -compound), none lost, no new extra. Each line read; valid probes clean.
- **Alternatives considered**: judging function templates' bodies at their definition (left: the corpus shows g++ doing it only for
  a class template's constructor, cpp0x/constexpr-ctor19); clang's placement at the statement (rejected: the dg lines are g++'s).

## 2026-11-09: C++'s CP303 (a), the next unknown GNU attributes (branch `cpp-props2`)

- **Built** (doc/cpp/CPP-SURVEY.md, "As built: CP300-CP314", CP303 (a)): the parser keeps a function's and a typedef's unknown GNU
  attributes as `:_gattrs` (a variable's and a field's since CP294 (a)), those of a leading attribute-specifier-seq too, and marks
  `[[gnu::x(...)]]` as a standard attribute's syntax; `gattr-rules` gain warn_if_not_aligned (not on a variable, a function or a
  bit-field; no bit-field of a type declared with it) and no_dangling (its argument rules), written from the GCC 15.2 manual
  (gcc/doc/extend.texi of the pinned tarball) and the corpus's directives, clean-room.
- **Not built**, with reasons: musttail (the middle end's tail-call errors), transaction_safe/_unsafe/_wrap (the TM TS under
  -fgnu-tm, its keywords not parsed); no_dangling's template-dependent argument rules.
- **Measured**: the every-10th sample unchanged (108, 92 extras); the attributes' 38 dg-error files of the whole part: three gained
  (pr53037-4, other/bitfield7, ext/attr-no-dangling2), none lost, no new extra; each line read, valid probes clean.
- **Alternatives considered**: (b) alone (the failing files' attributes only: the same two built, musttail and TM left for the
  same reasons); placing warn_if_not_aligned's errors at the attribute as CP294's (rejected: the directives' columns are the
  declarator's).

## 2026-11-09: C++'s CP304 (a), no captures from unevaluated operands (branch `cpp-props2`)

- **Built**: the generic lambda's capture scan skips sizeof's, alignof's, noexcept's operands, requires-expressions and `sizeof...`;
  their analysis (`*unevaluated*`) reads an enclosing variable without capturing it, for generic and non-generic lambdas
  ([expr.prim.lambda.capture]/7-8).
- **Measured**: the probe's closures 24 → 4 and 8 → 1 bytes, as clang lays them out; the sample unchanged (108, 92 extras); the code
  generator's 142 samples as recorded.
- **Alternatives considered**: typeid's operand unevaluated too (left: a polymorphic glvalue's is evaluated, and the slice's
  analysis does not decide it before the scan).

## 2026-11-09: C++'s CP305 (a), explicit instantiation definitions in the checker, and C++23's P2266 (branch `cpp-props2`)

- **Built**: the checker instantiates a function template's explicit instantiation definition (its written arguments, then
  deduction from the declared type) and analyzes the definition there ([temp.explicit]/10); the code generator unchanged. Found
  with it, C++23's P2266: a returned implicitly movable entity is an xvalue whatever the return type (cpp23/elision7's
  instantiation reported a copy that P2266 makes a move; `int& f(int&& n) { return n; }` now the expected error).
- **Measured**: the 69 files with explicit instantiations of functions: passes 2 → 2, extras 9 → 6 (elision7's three); the 34 elision
  and implicit-move files: extras 5 → 2, none new; the code generator's 142 samples as recorded.
- **Alternatives considered**: (b) both, the code generator emitting explicit instantiations eagerly (left by the decision: clang's
  weak_odr definitions are a names-check matter); P2266's throw operand (left: the try-block scope rule needs the enclosing try
  blocks, which the slice does not track).

## 2026-11-09: C++'s CP301 (a), a captureless generic lambda's conversion to a pointer to function (branch `cpp-props2`)

- **Built**: the conversion function template of a generic lambda with no lambda-capture, specialized on use for the target
  function type (the call operator's specialization deduced from its parameter types, its type required to be the target's), with
  its static invoker; mangled as clang's specializations (`__invokeIiEEDaT_`, `cvPFDaT_EIiEEv`). With it: deduced return types with
  a declarator (`auto&`, `auto&&`, `auto*`) deduced from the return statement; CP290's unsure rule narrowed to `auto...` closures.
- **Measured**: the 51 generic-lambda dg-error files: 9 → 12 own passes (lambda-generic-cfun, -xcfun, param-type-mismatch-3, CP290's
  withdrawn pass now sure), none lost, no new extra; the code generator's 142 samples as recorded; the probe's invokers generated
  (their runs the night's codegen-check).
- **Alternatives considered**: deducing from the target's return type too ([temp.deduct.conv] on the conversion's `auto (*)(T)`:
  the specialization's deduced return type is compared instead, the same verdicts); `auto...` (left unsure, the closure's pack
  against a function type's parameters).

## 2026-11-09: C++'s CP300 (a), constraint subsumption (branch `cpp-props2`)

- **Built**: constraints' normal forms (concept-ids expanded with their parameter mappings, atoms by expression and mapping),
  clang's subsumption over DNF and CNF (SemaConcept.cpp at llvmorg-23.1.5, from the pinned tarball), the more constrained template
  as [temp.func.order]/6's tie-break, and the constraints in a redeclaration's identity ([temp.over.link]/7, by their spelling).
- **Measured**: the 263 concepts dg-error files: 47 → 50 own passes (concepts-ca107, -pr67427, concepts1), none lost, extras 145 →
  139, none new; the code generator's 142 samples as recorded; a probe's calls take the more constrained specializations.
- **Alternatives considered**: the redeclaration's identity by normal forms (rejected: two declarations' own atoms are never
  identical, so a declaration and its definition would be two templates); clang's caching of normal forms (left: the slice's
  candidate sets are small).

## 2026-11-09: C++'s CP310 (b), immediate functions' addresses and invocations (branch `cpp-props2`)

- **Built** (the checker): a constexpr variable initialized by an immediate function's address, directly or returned by an
  immediate invocation, is an error at the variable ([expr.const]/16-18); an immediate invocation of a consteval function whose body
  the checker found not constant is an error at the call.
- **Measured**: the 84 consteval and constexpr dg-error files of cpp2a and cpp23: 5 → 7 own passes (consteval24, consteval22),
  none lost, no new extra; the code generator unaffected (checker-only rules).
- **Left**: CP310 (a), the in-call errors by kind, until the night's whole-part record gives their counts (the decision's order).

## 2026-11-09: C++'s CP312 (a), the conversion rules for the code generator (branch `cpp-props2`)

- **Built**: `conv-for`'s const-object and explicit-conversion rules (CX12's fifth slice, the checker's) for the code generator too.
- **Measured**: a probe's const object now converts by the const conversion function, as clang's IR does; the code generator's 142
  samples as recorded; the checker unchanged.

## 2026-11-09: C++'s CP313 (a), first part: access with friends for non-member functions (branch `cpp-props2`)

- **Built** (the checker): classes record their friend functions, friend templates and friend classes; a member named in a
  non-member function is judged unless a friend declaration may grant it (by name; a derived class's friends for a protected one),
  no longer only in units without friends (SemaAccess.cpp's EffectiveContext and friend matching, llvmorg-23.1.5).
- **Measured**: the sample unchanged (109, 92 extras); the 22 access-with-friends files unchanged; a probe correct.
- **Left** for the second pass: member contexts, [class.protected]'s object rule, using-declarations', structured bindings' and the
  injected-class-name's access, constructor templates in members, the type-access gate.

## 2026-11-09: C++'s proposals of the day, recorded (branch `cpp-props2`)

- **Recorded**: test/cpp/check-dg-own-linux-amd64.edn from the sample's last run (454 rows, 109 passes: 108 and cpp2a/consteval24),
  the C++ read fact (`:dg-own [109 1] :dg-clang [4488 0.5]`), test/crops/scores.edn (unchanged at its precision: C++ 76.1 / 100 /
  67.8). `bin/cppi codegen-gen` on the branch's head: 142 of 142 as recorded, both controls failing as they must; the dg control
  fails as it must; `make check-makefile`, `gate.crops check`, `bin/gate --full --plan`, `bin/crops-score --judge --strict` pass.
- **Left** for the second pass (doc/cpp/CPP-SURVEY.md, "As built: CP300-CP314", "Not built in this pass"): CP311 (a), CP310 (a)
  after the night's record, CP313 (a)'s member contexts and the other access forms.
- **Slips**: three throwaway reads with `/dev/null` or a process substitution (the survey's "Slips"); nothing written there.

## 2026-11-09: LLS35 (a), LLS36 (a), LLS37 (b): the mutants' oracle in the oracle tier, the unknown kinds' rules (branch `llvm-props`, from `mac/llvm-lls33` 60026c61b)

The user decided LLVM IR's three proposals as recommended (2026-11-09 morning). LLS35 (a): `bin/repr-llvm records-verify` (the oracle
tier's `llvm-records-verify`) ends with `mutants-oracle` over the pack rt's two mutant lists against opt's record, when the pack exists;
the step's command line is unchanged (only a comment and its waiver), so the night's slot patch `llvm-cells.patch` still applies.
LLS36 (a): `arbace.repr.llvm.expect` ports, from llvm-project 23.1.5 (`llvm-project-23.1.5.src.tar.xz`, the pin; Apache-2.0 with LLVM
exceptions): AutoUpgrade.cpp's upgradeIntrinsicFunction1 name rules, UpgradeIntrinsicFunction and UpgradeIntrinsicCall's cases with
Intrinsics.cpp's remangleIntrinsicFunction, then the Verifier's order for intrinsic declarations (V:3846 at the first caller, V:5952's
ptrmask and is.fpclass cases, V:3297's isSignatureValid message through intr.ae's matcher); visitInstruction's attachment checks
(V:5779-5913 with verifyRangeLikeMetadata, visitProfMetadata, the alias scopes, align, dereferenceable, TBAA's first); the visitDI*
field rules (V:1074-1723), a declaration's `!dbg` (V:3202) and StripDebugInfo's reachability (a debug fault in a node kept after the
strip is a hard error). The mutator attaches the three kinds' changes as metadata (no random number drawn: the mutants' bodies are
llvm-lls33's byte for byte, 900 compared). The comparisons found four rules Arbace's verifier lacked (a local variable's non-DIScope
scope, an old debug intrinsic's call's metadata arguments as a record's, that call's attachments dropped, a declaration's distinct
`!dbg`), fixed. Measured light (stage 0, the stubs first; live opt by hand as the oracle): the day's 1,872 mutants 1,694 to 1,818 own
verdicts (97.1%), every one as Arbace's verifier and opt; a held-out set never looked at (h2, 900 of the intrinsics, metadata and
debug families at 800000) 458 to 815 own (90.6%), all 815 as both; a first held-out set (h1, at 700000) read once, which showed 10 own
verdicts of the first rules wrong against opt (fixed). The families the verifier's fixes touch as recorded (verify-llvmtest 792 of 806,
verify-probes 472 of 478, roundtrip- and text-llvmtest 304 of 304), `bin/llvmi check --essential` 10 of 10, controls-test all ok.
LLS37 (b): nothing. Alternatives considered: a new gate part for the mutants (a `:parts` line on llvm-records-verify; rejected, it
would change the slot patch's context), porting module flags' and visitIntrinsicCall's other cases now (proposed as LLS38, LLS39).
Details: doc/llvm/LLVM-CROP-SURVEY.md, "As built: LLS35 (a), LLS36 (a)".

## 2026-11-09: CS290 (a), CS291 (a)/(b), CS292 (a): pragmas in stream order, the lexer's rejections as diagnostics, a dg-wording family (branch `c-props`, from `mac/c-compl` 35744df5e)

The user decided C's three proposals as recommended (2026-11-09 morning). CS290 (a): under `*reentry*` the preprocessor keeps a
re-entered main file's `#pragma` texts and hands them to the parser with the next token (`stream`'s `:pragmas`), the parser records
them by token seq (moved with the tokens when `__func__`'s are injected), and ci's read (`*include-inside*`) writes `c/pragma` before
the body item that token starts; the checker runs a statement-level `c/pragma` (so a main-file `#pragma pack` in a body counts too)
and lays a record out by the pack state at its definition. 95_bitfields' packed and aligned tests then showed three layout rules
under `#pragma pack`, taken after gcc 15.2's stor-layout place_field and layout_decl (no code copied): no type-size window for
bit-fields when a maximum field alignment is set, a zero-width bit-field aligning to its type's full alignment, an aligned
attribute capped by the pack; and ci's back end (IBitLoad/IBitStore, aot's BitLoad/BitStore) reads and writes a packed bit-field's
bits past its 64-bit unit in the next byte. 95_bitfields passes at eval and AOT. CS291 (a): `(c/bad-lit T spelling message)` for
the constants gcc's lexer rejects (a binary prefix after libcpp's cpp_classify_number; a u8 character constant after
cpp_interpret_charconst), reported by the checker, printed back as written: c23-utf8char-3 and c23-binary-constants-3 pass dg and
the round trip's levels 1 and 2; (b): c23-auto-3 and c23-constexpr-3 keep their refusals. CS292 (a): the family
`:syntax-expected-before` in `test/c/dg-wording.edn`: c23-attr-syntax-7 passes; -6 keeps two lines in `:wrong-reason` (no syntax
error after a failed paste: proposed as CS293) and gnu23-attr-syntax-3 stays (a declaration diagnostic, not wording). Checked light
(stubs first, no gcc): the affected cases `--only`, dg `--essential` 92 of 92, the essential eval and AOT lists 0 failing, `bin/repr-c
check --c23` 59 / 3 / 0, `bin/ci judge-test` 0 failures; the `--only` rows merged into the records. The musl forms cache re-read for
the new key was cut by a timeout at 1,300 of 1,331 files; its 3,165 files equal c-compl's byte for byte, whose index and the new
key completed it (the night re-reads). C: completeness 95.92% to 95.96% (99.59% to 99.64% without by design), the read cell's deferred
36 to 33, eval and AOT 1 to 0, print 41 to 39. Alternatives considered: naming each re-entry `MAIN#again#N` (CS290 (b), rejected by
the decision), the parser's messages in gcc's words (CS292 (b): the parse check compares them with cc's). Details: doc/C23-SURVEY.md,
"As built: CS290 (a), CS291, CS292 (a)".

## 2026-11-09: SCS15 (a), SCS16 (b), SCS17 (c), SCS18 (b), si's Unicode and printer from the pinned UCD, the read judge without Chez's dumps (branch `scheme-props2`, from `mac/scheme-props` 1f64429fa)

The user accepted the four as recommended (about 06:15). doc/scheme/SCHEME-SURVEY.md, "As built: SCS15-SCS18", has the detail.

- SCS15 (a): `#!chezscheme`'s extensions stay classed (`:chezscheme-extensions`, choices.edn); nothing built.
- SCS16 (b): fetched once by the user's allowance from https://www.unicode.org/Public/16.0.0/ucd/ into the main checkout's
  `.tmp/scheme/ucd-16.0.0`: `SpecialCasing.txt` (16,809 bytes, SHA-256 8d5de354eef79f2395a54c9c7dcebbaf3d30fc962d0f85611ea97aa973a0c451),
  `DerivedCoreProperties.txt` (1,115,959, 39d35161f2954497f69e08bdb9e701493f476a3d30222de20028feda36c1dabd), `PropList.txt` (144,253,
  53d614508e2a0b2305a8aa21cd60d993de9326cdf65993660dfcce4503548583), pinned in test/scheme/ucd.edn; data only, nothing run. The generator
  `arbace.repr.scheme.ucd` writes si's categories, the properties R6RS's `(rnrs unicode)` names (Alphabetic, Numeric as UnicodeData.txt's
  numeric value, White_Space, Uppercase, Lowercase, Cased, Case_Ignorable), the simple mappings and SpecialCasing.txt's unconditional full
  ones into `go/arbace/scm/unicode.ae`; `prim_char.ae` takes them, dropping Go's `unicode` tables (17.0) and Chez's recorded mappings.
  Checked by a new portable R6RS program over every scalar value, `test/scheme/unicode/scan.sps`, run on Chez 10.3.0 and on si: 4,825 lines
  differed before, none after; `bin/si unicode-scan [--record]` holds si to Chez's recorded digest (test/scheme/unicode-scan.edn).
  Alternative not taken: leaving Go's tables (the 4,803 code points of 17.0 and Numeric's 8 ideographs would stay Chez differences).
- SCS17 (c): doc/scheme/SPEC.md 7 states `:print-string`, `:print-char`, `:print-symbol`; the generator writes the escape tables for both
  printers (repr/scheme's `print.ae`, si's `write.ae`); the report states the rules a second time and judges the four printing scans, so
  `bin/repr-scheme check` reads no scan dump. `report-scans` holds the rules to Chez's scans (equal) and reads the written texts back.
- SCS18 (b): both readers refuse `+INF.0`, `#x1|53` and end a prefixed number at `#` in R6RS's syntax; report-chez.edn is empty, its three
  rules are classes of choices.edn with their 37 probes, which report-check holds to differ from Chez. si's `string->number` is unchanged
  (SCS19 proposes it).
- Numbers: Scheme's independence 99.92 to 100.0 (judge 0.996 to 1.0: the read check reads no Chez dump), completeness 97.41 (98.88) and
  maturity 100 unchanged. Proposed: SCS19 (`string->number`'s syntax), SCS20 (the Unicode scan in `si-check`), SCS21 (titlecasing's word breaks
  from WordBreakProperty.txt).

## 2026-11-09: OC203 (a), OC204 (a) (= OC195 (a)), OC205 (a) built: ocamltest's hooks ported, the four second programs and signal run (branch `ocaml-props`, from `ocaml-compl`'s fb8a73f19)

The user's decisions of the morning (about 06:15, as recommended), relayed by the main session. Source studied: OCaml 5.5.0's
ocamltest (the pinned tarball's `ocamltest/main.ml` `test_file`'s `install_hook`, `actions.ml`'s `run`, `actions_helpers.ml`'s
`run_hook` and `check_output`, `builtin_actions.ml`'s `initialize_test_exit_status_variables`, `testsuite/Makefile`'s `SORT`), ported
as a rule into bin/oi's runners (doc/ocaml/OCAML-SURVEY.md, "As built: OC203-OC205"):

- OC203 (a): every run test's `TEST.run` and `TEST.check-program-output` hooks run as ocamltest runs them, `sh HOOK` in the build
  directory with ocamltest's variables, in oi's runner, the references', and the AOT light and compiled levels' (`hook_vars`,
  `prog_script`, `hook_run`, `hook_check`). The corpus takes the tests whose check is their hook (lib-threads/signal); swapchan's and tls's
  own-verdict exceptions left, their reference rows re-recorded through the hooks (`bin/oi record-ref --only-list` of swapchan, tls and
  signal with the host's OCaml 5.5.0, ocamlc and ocamlopt, by hand: each sorted output's sha256 is now its `.reference`'s, 2aba5ace for
  swapchan, d73f64ff for tls, where before swapchan's both runs and tls's ocamlopt run were unsorted interleavings; signal's row dropped,
  a helper test); tls passes at the AOT light level (was a known schedule failure); the compiled level's runner takes the hooks, its
  record not rerun here (tls's `aot-compiled-known.edn` line kept until the night's compiled run). `record-ref` skips the tests that
  build a helper.
- OC204 (a): header.awk's helper mode takes a helper of OCaml alone; quotecommand, redirections, wait_nohang and test_unix_cmdline
  run and pass by their own verdict at oi's level (both paths), the AOT levels keep `:ocamltest-tools`.
- OC205 (a): recorded; C helpers stay on `oi cmain`.

Alternatives considered: keeping bin/oi byte-identical (the native library's key hashes it) by overriding its functions from a file of
bin/lib/oi/: rejected as duplicated definitions; the library is rebuilt once (about 8.5 minutes, `OI_LIB_JOBS=1`) and the night's
`bin/oi stdlib` rebuilds it after the merge in any case. Applying a hook's response file's modifiers: not ported (no corpus test's hook
writes one), a non-empty response is a note of the row.

Checks: the nine by `--only-list` on both paths (9 of 9), `--aot` (2 pass, 7 classed), the essential list (58 of 58, control differs),
`own-verdicts --check`, the five judgments, `bin/oi test`, `bin/reap` and `crops-score --judge --strict` (0 problems), `bin/gate --plan`.
Scores: OCaml completeness 81.6 to **81.75** (eval 460 / 211 / 0 to 465 / 207 / 0; AOT 405 / 203 / 63 to 406 / 202 / 64). Proposed OC206
(the OCaml helpers at the AOT levels).

Also: `bin/oi suite rollup` failed on `ocaml-compl`'s records (27 files "run without a level's result": programs oi runs that the AOT
levels class by header); `test/ocaml/testsuite_check.ae` gives them their AOT class, a commit of its own (86d8b6c47) to pick onto `next`
before the night's `oi-check`. Checks by list on both paths, the essential list with its control, own-verdicts --check, the five judgments
and the rollup: 0 failing. Scores: OCaml completeness 81.58% to 81.75% (eval 460/211/0 to 465/207/0, AOT 405/203/63 to 406/202/64),
maturity 100%, independence 87.4%. Proposed OC206 (the AOT levels' helpers), OC207 (a control that runs a hook).

## 2026-11-09 HF10's accepted proposals built: P1, P2, P5, P6, P8, P10 (branch `hs-props2`, from `hs-hf10` 5e30516fb)

The user accepted HF10-P1 to P10 as recommended (~06:15). Built by an agent in the light tier on the main host; GHC 9.14.1
(`:ghc b9908216`) ran by hand only to record oracle packs. Sources: GHC 9.14.1's tarball as pinned by
`test/haskell/corpus-ghc-9.14.1.edn` (`.tmp/ghc-src/`), `compiler/GHC/HsToCore/Foreign/{Decl,C,Prim,Call}.hs`,
`compiler/GHC/Stg/Unarise.hs`, RepType's sum slots, `Core/Opt/Simplify/Env.hs`, `Core/Opt/Simplify.hs`, `Core/Opt/ConstantFold.hs`.

- `mac/next` (b15d4c73c, which holds hs-hf10) merged in first for HF9-P1's lib-check pack machinery; night10-fixes' key rule
  cherry-picked (d4d589c49) so the `-O2` pack stays keyed as recorded and `dump_package` unchanged.
- HF10-P1 (a): the pack `haskell-stg-libs-O0-ghc-9.14.1` (510 members, 1.9 MB xz, byte for byte the first slice's reference),
  `bin/hsi libs-pack`, the kind `stg-libs-O0`; lib-stg reads it. Alternative (b), GHC at each run, rejected by the user's decision.
- Two more packs by hand, because P2's step must run without GHC: `haskell-lib-ifaces-ghc-9.14.1` (HF9-P1's write-ahead pack, now
  recorded over the native-bignum read) and `haskell-ifaces-ghc-9.14.1` re-keyed (0cdc76cec's helper change had made it STALE; its
  members unchanged). All mirrored (`bin/oracle-records mirror --push`).
- HF10-P10 (a): the -O0 Simplifier's contOutArgs substitution was exponential (every saved substitution converted transitively);
  ported GHC's lazy getSubst. UTF8 from over 900 s to 9.8 s; then every module of ghc-internal's plan compiles (265, the slowest
  84 s), and the list became the whole package. Profiled with a call-counting driver in scratch (not committed).
- HF10-P6 (a): `REPR_HASKELL_BIGNUM=native` for lib-check's read (dump_plan.ae's rule); lib-check's record re-recorded (265 modules).
- HF10-P5 (a): foreign imports desugared after GHC's dsFImport/dsFCall/dsPrimCall/dsFLabel into Core with an FCallId-like global,
  StgOpApp's `ffi`/`primcall` in tostg; with it realWorld# kept as an argument, Solo#/Unit# reps, shortOutIndirections of top-level
  binders, ConstantFold's two-literal binary rules (the NPE behind the two STG stops). Alternative (b), the pack's bindings, rejected
  by the decision.
- HF10-P8 (a): unboxed sums in Unarise (ubxSumRepType, layoutUbxSum, mkUbxSum, unariseSumAlts); the construction's type taken from the
  binding's result type, a limitation recorded.
- HF10-P2 (a): `lib-stg --judge`/`--record`, the control file, the own set's recorded passes, the `--full` step `haskell-lib-stg`.
- P4 (a), P7 (a), P9 (b) recorded as orders; P3 (a) not started (its first step's gap written down in the survey).
- Measures: lib-stg over 265 modules 5,372 of 7,593 bindings equal to GHC's -O0 (the old list 2,473 → 2,560 of 3,421, none
  worse); runs 59 of 63 on both sets, 0 lost; typecheck-check 24 of 24; run-check 59 of 63 as before.

## 2026-11-09: C++'s CP311 (a), class template argument deduction with initializers (branch `cpp-props3`)

- **Built** (the checker and the code generator, `arbace/cpp/template.ae`'s `ctad`, `arbace/cpp/sema.ae`): clang's
  SemaTemplateDeductionGuide.cpp and SemaInit.cpp's DeduceTemplateSpecializationFromInitializer at llvmorg-23.1.5 (the pinned
  tarball), ported rule for rule: implicit guides from the declared constructors (the class's then the constructor template's
  parameters, the pattern's member typedefs substituted), `C()`, the copy deduction candidate, the user's deduction guides
  (recorded on their class template as declared), C++20's aggregate deduction candidate; deduced by `deduce-call`, chosen by
  `resolve-overload` with [over.match.best]/2.9-2.11's guide tie-breaks; copy-initialization's converting guides, list-initialization's
  two phases, the explicit guide in copy-list-initialization, no viable or ambiguous guides, a placeholder in a compound type, the
  name without arguments before C++17 and the alias template's C++20 extension under `-pedantic-errors` as the program's errors; for
  variables (the parser marks its own deduction's placeholder, `:_ctad`), `T(...)` and `T{...}`. Unsure drops (types the slice cannot
  build or substitute) keep the parser's deduction, so the code generator is never worse than before. Also two partial
  specializations differing only in their constraints kept as two (found once CTAD let concepts-class be analyzed).
- **Why (a)**: the proposal's recommendation, the user's decision of 2026-11-09; (b) the checker alone would leave the code generator's
  CTAD to the parser's approximation (no user guides, no packs, one guide), (c) the 11 sample lines unjudged.
- **Measured**: see the survey's "As built: CP311 (a) and the rest of CP313 (a)" (the sample's own passes, the 75 CTAD files of the
  whole part 6 → 18, the code generator's 142 samples as recorded, a probe whose deduced types' mangled names are clang's).
- **Left**: alias templates' guides in C++20 (BuildDeductionGuideForTypeAlias), C++23's inherited constructors' guides, `new T(...)`,
  class templates' static members' initializers in specializations.

## 2026-11-09: C++'s CP313 (a), the rest: access in every context the checker models (branch `cpp-props3`)

- **Built** (the checker, `arbace/cpp/sema.ae`; after clang's SemaAccess.cpp at llvmorg-23.1.5: EffectiveContext, GetFriendKind,
  HasAccess, IsAccessible's path walk): the context's classes (nested ones' enclosing classes) and function; friends matched by
  name (functions, now also classes by their recorded names); [class.protected]'s object rule; a member's access through the
  naming class's bases ([class.access.base]/5, an accessible base's members); applied to `o.m`, `o.f()`, members named in member
  functions, the constructor an initialization chooses (a constructor template's specialization with its template's access), the
  injected-class-name through a private base (`o.B::m`), class-scope using-declarations of enumerators and types (their own
  access), structured bindings' rules ([dcl.struct.bind]/4: one class holding the members, no anonymous member, no closure type,
  accessible members, the names' count); the member types' check gated per class instead of per unit.
- **Why**: the user's decision CP313 (a) of 2026-11-09, its second pass ("As built: CP300-CP314", "Not built in this pass").
- **Measured**: the every-10th sample 114 → 117 (access19, scoped15, decomp4), none lost, the extras the same 92 lines; the 90
  access files of the whole part 12 → 19, none lost; the code generator's 142 samples as recorded (checker-only rules).
- **Recorded**: test/cpp/check-dg-own-linux-amd64.edn from the final run (454 rows, 117 passes), the C++ read fact
  (`:dg-own [117 1] :dg-clang [4480 0.5]`), test/crops/scores.edn (C++ 76.1 / 100 / 67.84, read 0.657); `make check-makefile`,
  `gate.crops check`, `bin/gate --full --plan`, the dg control and `bin/crops-score --judge --strict` pass.
- **Found**: the dg read's order-dependence (lookup/scoped15 read once accepted in a 90-file list, otherwise rejected), not
  explained in this pass.
- **Proposed** (doc/cpp/CPP-SURVEY.md, "Proposed (CP320-CP324)"): alias templates' guides, static members' in-class initializers in
  the checker, the read's order-dependence, C++23's inherited constructors' guides, using-declarations' access for functions.
- **Slips**: three throwaway commands against the day's rules (a mistyped `sed -i` naming the null device, a wait loop with `pgrep`
  and a discarded output, a process substitution in an inspection command); listed in the survey's "Slips, scratch".

## 2026-11-09: C++'s CP322 (a), the dg read's order-dependence (branch `cpp-props4`, from `mac/next` 4be141f28)

- **Found**: the dg read's pool threads loaded the code generator's Sema concurrently: `arbace.repr.cpp.diag/sema-errors` used
  `requiring-resolve` (an unlocked resolve first) and a plain `require` of `arbace.cpp.template`, so a second thread could load
  the template namespace again while the first one's unit ran on it; a failure in Sema is caught by `sema-errors` and leaves the
  unit accepted. That fits the one flip (lookup/scoped15, the fourth file of the 90, accepted once); it was not reproduced in eleven
  runs. Also `arbace.cpp.sema/text-of`'s header-text cache keyed by an unresolved relative header name (another directory's
  header of the same name got the first one's text).
- **Fixed**: `diag/load-sema!` (one `delay`, the requires under `RT/REQUIRE_LOCK`), called by `sema-errors`, `cxx-year` and by
  `read-cmd` before its pool; the header texts keyed by the resolved path; `read-cmd` reads in the names' order, or shuffled by
  `REPR_CPP_DG_SHUFFLE=SEED`.
- **Proved**: the 90 access files in the names' order and shuffled, twice (seeds 7 and 3, the second on the commit's tree),
  and the every-10th sample's 454 in both orders: identical verdicts each time (doc/cpp/CPP-SURVEY.md, "As built: CP322 (a)").
- **Alternatives**: (b) a fresh process a file for the record (about twice the 80 minutes), not needed once the load is
  serialized; keying the shared tables by file was the other half of (a).


## 2026-11-09 HF10-P3 (a), first slice: the front end's own interfaces, ghc-internal bottom-up (branch `hs-p3`, from `mac/next` e6214cd13)

- Decided by the user 2026-11-09 (HF10-P3 (a)): the front end writes its own interfaces as it compiles the boot libraries
  bottom-up, so the `-O0` set is coherent without GHC's interfaces or the pack merge. Built in the light tier by day; no GHC run.
- `arbace/repr/haskell/ownface.ae`: the writer after GHC 9.14.1's `compiler/GHC/Iface/Make.hs` (mkIface_) and `Iface/Tidy.hs`
  (addExternal, trimId) at -O0 (`Driver/Config/Tidy.hs`'s ExposeNone, `IfaceToCore.hs`'s tcIdInfo ignoring all but compulsory
  unfoldings), `Types/Id/Make.hs`'s dataConSrcToImplBang, `HsToCore/Binds.hs`'s makeCorePair (every default method's compulsory
  unfolding, Note [INLINE and default methods] of `Tc/TyCl/Instance.hs`), the wired-in definitions of `Builtin/Types.hs` and the
  built-in families of `Builtin/Types/Literals.hs`, from the pinned tarball (SHA-256 2a83779c...), with GHC's notice (LICENSE.md,
  doc/ARBACE.md). The format is the helper's iface2 forms, so the readers did not change; kinds stay erased (HF9-P4 (a)), the readers'
  dropped kind arguments written back as placeholders. GHC.Internal.Prim stays GHC's wired-in interface (from the pack).
- Considered: a new format of our own (rejected: two readers to keep; the forms are already Arbace's data), judging against GHC's
  `-O0` `.hi` dumped afresh (rejected for this slice: GHC by hand; the iface2 pack's exports, fixities, things and instances do not
  depend on the level, so the judge reads the pack), writing no unfoldings at all (tried first: lib-stg 5,140; GHC's -O0 does expose
  the default methods' compulsory unfoldings, found reading makeCorePair; with them 5,280).
- `libcheck own-order`/`own`: the 265 modules and 24 hs-boot files in dependency order, closures as file lists (`*iface-files*` in the
  renamer and the elaborator: jrt has no symbolic links), resumable; `bin/repr-haskell lib-check --ifaces own`, `lib-stg --ifaces own
  [--run]`, run-check's `REPR_HASKELL_RUN_IFACES_OVER`; the interface judge (`compare-ifaces`, by heads) with its negative control.
- Rules met (GHC's): an hs-boot file's signatures as its values (renamer and elaborator), tcShortCutLit (`Tc/Utils/TcMType.hs`),
  standalone deriving on an empty data type, class fundeps by position, every own class written (Tidy keeps every TyCon), the closures
  with the transitive imports (the oracle's closures name the GMP backend).
- Measures: lib-check on own interfaces 258 of 265 (GHC's 260; the first own run 237), 20,681 of 20,891 interface facts equal to
  GHC's; lib-stg 5,280 of 7,627 (GHC's interfaces 5,372); ten programs: 8 on GHC's `-O0` libraries with the own interfaces (HF10-D5's
  coherent set was 35 of 63 on `-O2` interfaces), 8 with 70 modules Arbace's own STG whole, 0 lost; the whole own set 0 (krep$*, an
  unbound local). Records `test/haskell/lib-check-own-ghc-internal.edn`, `test/haskell/lib-stg-own-results.edn`. Proposals HF10-P11
  to P14 in the survey.

## 2026-11-09 OC194 (a): ocamlyacc ported (branch `ocaml-yacc`, from next e6214cd13)

The user accepted OC194 (a) (2026-11-09). Built by an agent in the light tier (one process at a time, no gate, no build).
Source: OCaml 5.5.0's release tarball (https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz, SHA-256
`fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`, the copy in `.tmp/repr-ocaml/ocaml-5.5.0`), its `yacc/`:
main.c, reader.c, symtab.c, error.c, lr0.c, closure.c, warshall.c, lalr.c, mkpar.c, output.c, skeleton.c, verbose.c (wstr.c, Windows'
wide strings, not), Berkeley yacc as OCaml adapted it ("Based on public-domain code from Berkeley Yacc", INRIA's LGPL 2.1 notice with
the linking exception). The reference: the host's `/usr/bin/ocamlyacc` 5.5.0.

- `arbace/repr/ocaml/ocamlyacc.ae` (`arbace.repr.ocaml.ocamlyacc`), a port by hand, rule for rule, evaluated on stage 0 as
  `ocamllex.ae` is: the reader with its C details kept (a signed `char`, so a 0xFF byte is `EOF`; the reused line buffer that
  `print_pos` reads under a stale position, modelled), LR(0), DeRemer and Pennello's LALR(1) as lalr.c writes it (`menhir.ae`'s
  sets consulted, not reused, to keep the gotos' numbering), mkpar.c's conflicts and default reductions, output.c's packing
  (`pack_vector`, `matching_vector`) and text, verbose.c's `.output`, main.c's command line and statuses (the `.output` left on an
  error, the `.ml` and `.mli` removed). Alternatives: a fresh LALR(1) generator emitting the same tables (rejected: the packing
  depends on byacc's state and goto numbering, so equality byte for byte needs its order anyway); Menhir's `--table` (another format).
- `bin/repr-ocaml ocamlyacc` (the native tool) and `ocamlyacc-check [--host [--record]]`: the tarball's eight ocamlyacc grammars and
  20 samples of Arbace's own (`test/ocaml/ocamlyacc/`, 13 of them error cases), plain and `-v`, and `cases.txt`'s 20 command lines:
  76 runs, the files and messages by SHA-256 against `test/ocaml/ocamlyacc-expected.edn` (recorded from the host), a negative
  control. `--host`: 76 of 76 equal byte for byte; the plain check 76 of 76, about 16 s, 440 MB. `make repr-ocaml-ocamlyacc-check`.
- Mutants in scratch (a byte deleted, a token inserted, a span cut; 14 grammars; every other one with `-v`): 1,004 of 1,004 equal to
  the host's (507 generate), with no fix needed.
- Docs: doc/ocaml/OCAML-SURVEY.md "As built: OC194 (a)", doc/ARBACE.md, LICENSE.md. Not a gate step (left to the main session);
  `tool-lexyacc/calc` can take `bin/repr-ocaml ocamlyacc -q calc_parser.mly` in place of the host's ocamlyacc.

## 2026-11-09 OCaml's accepted proposals built: OC192, OC193, OC194's wiring, OC196, OC197, OC206, OC207; OC191 (b) recorded (branch `ocaml-props2`, from next `e6214cd13`)

The user accepted OC191-OC197, OC206 and OC207 as recommended (~09:30; OC191 (b), the rest (a)). Built in the light tier (one
check process at a time, `OI_JOBS=1`, `O2J_JOBS=1`, `OI_TYPER_GOMEMLIMIT=1200MiB`, main's stage 0); the host's OCaml 5.5.0 ran only
the oracle path's ocdump dumps and the oracle-tier recording of ocamlyacc's record. Sources read: the OCaml 5.5.0 release tarball as
pinned by `test/ocaml/corpora.edn` (`runtime/finalise.c`, `runtime/startup_aux.c`'s `caml_parse_ocamlrunparam` and `scanmult`,
`runtime/domain.c`'s `caml_domain_spawn` and `domain_create`'s failure, `runtime/caml/domain.h`'s `Max_domains_def` and
`Max_domains_max`, `yacc/`, `testsuite/tests`' headers), ocamltest's actions as earlier entries cite them.

- OC194 (a) by a subagent (branch `ocaml-yacc`, `b59f68e79`, cherry-picked as `a0369db49`): `arbace/repr/ocaml/ocamlyacc.ae`, a
  port of `yacc/` (Berkeley yacc, public domain, under INRIA's LGPL 2.1 notice with the linking exception), `bin/repr-ocaml
  ocamlyacc` and `ocamlyacc-check` (76 of 76 runs equal to the host's ocamlyacc byte for byte, 1,004 of 1,004 mutants). Wired here:
  the gate step `ocaml-read-essential` runs `ocamlyacc-check`; the suite corpus generates a test's `.mly`/`.mll` modules natively
  (`tool-lexyacc/calc` runs and passes); `bin/repr-ocaml`'s generators read and write in the caller's directory.
- OC192 (a): finalisers at oi's level by Go finalisers (`go/arbace/oi/ext_final.ae`, resurrection as the collector's darkening) and
  in the AOT runtime by weak references with a block's field array kept (`jdk/ocaml/ocaml/rt/ExtGc.ae`); finalise.c's batch order;
  run at the forced collections and at the ticks. Alternatives considered: a mark of our own from the roots (impossible: the roots
  are Go's and the Java forms interpreter's), keeping values strongly and never running (the old behaviour), Go's `weak` package for
  the `first` table (no resurrection: the function needs the value).
- OC193 (a): `OCAMLRUNPARAM`'s `d` in oi and the AOT runtime; header.awk passes a `d`-only `ocamlrunparam`.
- OC196 (a): `:speed` deferred. OC197 (a): the four main `.mll` tests in the run corpus, ocamllex natively at check time;
  `warnings/mnemonics` a class of its own (`:compiler-of-the-tree`, OT3 (a)).
- OC206 (a): the OCaml helpers at the light level (translated and run with their test) and at the compiled level (a package of the
  test's shard; built, not run here). OC207 (a): the hook control and signal in the essential list.
- Measures (light, by list, both paths): run corpus 13 programs 12 pass at oi's level (mnemonics classed), essential list 59 of 59 with
  both controls, suite pair 2 of 2, AOT light 17 programs 12 pass, 2 slow, 3 classed (quotecommand and finaliser_handover beyond the
  light level's 120 s of CPU), AOT runtime regression sample 7 of 7; the AOT runtime fixed on the way: a created file's permissions
  (`caml_sys_open`) and `2>&1` merged in `create_process`. Scores: OCaml completeness 81.75% to 82.49%, eval 465/207/0 to 477/195/4,
  AOT 406/202/64 to 419/191/66, maturity 100%, independence 87.4%. Not run: the compiled level (the night's; OC206's compiled path built,
  not proved). Proposed OC208 (`Unix.execv` at the AOT levels). Process note: the harness made the subagent's worktree inside
  `/root/arbace/.tmp/mac-queue/.claude/worktrees/` (an isolation flag of the call); its commit was fetched from there.

## 2026-11-09: C++'s CP320 (a), an alias template's guides (branch `cpp-props4`)

- **Built** (`arbace/cpp/template.ae`; after clang's SemaTemplateDeductionGuide.cpp at llvmorg-23.1.5, the pinned tarball:
  getRHSTemplateDeclAndArgs, BuildDeductionGuideForTypeAlias, DeclareImplicitDeductionGuidesForTypeAlias,
  DeclareAggregateDeductionGuideForTypeAlias, buildIsDeducibleConstraint): an alias template's guides f' from its
  defining-type-id's class template's guides f (implicit, copy, user, aggregate), f's return type's arguments deduced from the
  defining-type-id, f''s parameters the alias's it names and f's undeduced, its signature built lazily; the is_deducible
  constraint and the alias's own constraints on the class a guide returns; from C++17 on.
- **Why**: the user's decision CP320 (a) of 2026-11-09 (doc/cpp/CPP-SURVEY.md, "Proposed (CP320-CP324)").
- **Measured**: probes `.tmp/p/al1-al3b.cpp` (seven deduced types right, the control's seven wrong ones refused); the 75 CTAD
  files (`.tmp/fctad.txt`) with CP323: 21 → 28 (class-deduction-alias1, -alias2, -alias3, -alias8, -alias12, -alias16, -alias19).
- **Left**: a member alias template named through its class (`T<double>::AFoo`), cpp1z/class-deduction-alias1's C++17-only
  GCC error (clang accepts).

## 2026-11-09: C++'s CP323 (a), C++23's inherited constructors' guides (branch `cpp-props4`)

- **Built** (from the standard's text, [over.match.class.deduct]/1.10 and [over.match.best.general]/2 per P2582R1; clang 23 has
  none): `inherited-guides`, the guides of the alias with C's parameters and the base as defining-type-id (CP320's), each returning
  C with the arguments deduced back; the tie-break for a guide not from inherited constructors; the dependent base's
  `UnresolvedUsingValueDecl` recognized. In the checker: a class's inherited constructors among its initializations' candidates
  (B's but copy, move and no-parameter ones), the chosen one's access as B's member, the initialization otherwise not analyzed.
- **Why**: the user's decision CP323 (a) of 2026-11-09; the checker's half because a class inheriting constructors gave a false
  "no viable function" at its construction (any standard).
- **Measured**: probe `.tmp/p/in1.cpp` (C<int>, C<char>, G<char, bool>; D and E errors), `in3`-`in5`; the every-10th sample
  117 → 120 with CP320, CP321 (cpp23/class-deduction-inherited1 among them); the using-declaration files (`.tmp/fusing.txt`):
  inh-ctor9 kept for the right reason now (A's protected constructor), inh-ctor35 lost (its expected error is
  `-fno-new-inheriting-ctors`'s, before P0136: the earlier pass was the false "no viable" error), pr97268's extra line gone.

## 2026-11-09: C++'s CP321 (a), static data members' in-class initializers in the checker (branch `cpp-props4`)

- **Built** (`arbace/cpp/sema.ae`, `template.ae`): a static data member's in-class initializer analyzed in its class's scope in the
  analysis' order (refusals reported), for a class of the main file, a class template's specialization only when an explicit
  instantiation definition names it (`:explicit-inst-def`; [temp.inst]/4 for the implicit ones); the code generator unchanged.
- **Why**: the user's decision CP321 (a) of 2026-11-09.
- **Measured**: using-enum-1 (lines 37-38) and class-deduction108 (eight lines) now own passes (the every-10th sample's two of the
  three gained); the 158 dg-error files with an in-class static member and a class (`.tmp/fstatic.txt`, in chunks of 20,
  `.tmp/ch-*` against the base `.tmp/chb-*`): no pass lost, no new extra line.

## 2026-11-09: C++'s CP324 (a), a using-declaration's access for member functions (branch `cpp-props4`)

- **Built** (`arbace/cpp/sema.ae`): a class's using-declaration of member functions gives its synonyms its own access in its class
  ([namespace.udecl]/19), judged at the call through the naming class (`cx-using-map`), and each function it names checked
  accessible at the using-declaration.
- **Why**: the user's decision CP324 (a) of 2026-11-09.
- **Measured**: probe `.tmp/p/u1.cpp` (three errors where the rules have them, none on a protected function made public);
  the 19 dg-error files with a class's using-declaration and an access message (`.tmp/fusing.txt`): parse/access6 now passes
  (with CP323's and CP321's: 5 → 6, inh-ctor35 lost as CP323 says).

## 2026-11-09: C++: the dg read's one endless file fixed, CP321's measure corrected (branch `cpp-props4`)

- **Found**: cpp0x/overflow1.C never ended in the dg read (exponential constant folding of `Fib<95>::value` through the
  specializations' static members; on next's tree too): tonight's whole-part record would have hung.
- **Fixed**: `arbace.cpp.sema/var-const-int`, a const integral variable's value kept once found; overflow1 in 7 s. And CP321's
  analysis restores the analysis position after a member's initializer (a stale line-1 error in
  template/non-type-template-argument-1).
- **Correction** of the CP321 entry above: its "158 files: no pass lost, no new extra" came from a run on the CP323 commit's tree,
  a mistake in assembling the commits. Measured again on the right tree: no pass lost, five gained (class-deduction108,
  using-enum-1, class-deduction-alias12, var-templ81, static4, each reviewed), one new extra found and fixed (the position)
  (doc/cpp/CPP-SURVEY.md, "As built: the dg read's one endless file, and CP321's measure corrected").

## 2026-11-09: RF11 (first slice): rsi's cases from the front end's own MIR (branch `rust-rf11`, from next `e6214cd13`)

The user's order (RES54 (a): RF11 after RF10; RFP41 (a): the eval cell moves only when rsi's own cases run from the front end's MIR).
Recorded in doc/rust/FRONTEND-SURVEY.md, "As built: RF11 (first slice)".

- Design: the switch is per case and replaces the program's codegen unit only (the pack's `x.ll`); the shim and std's lowered
  library stay rustc's, and std's instances' bodies (our module is a `-Zshare-generics=no` one) and the facts the native mono refuses
  come from a new record, `rust-front-rsi` (the helper's `--mono-mir` at rsi's plan's flags against rsi's std build's sysroot, whose
  StableCrateIds are recorded in `test/rust/front/std-crates-rsi.edn`; 73 programs, 2.5 MB xz, made by hand with rustc). The judge
  is rsi's own (status and outputs against `ref-rustc-1.99.1.edn`). Alternatives considered: merging our functions into the pack's
  share-generics `x.ll` (it references std's shared instances by symbols our native mono does not compute, and would mix two codegens
  in one unit); running the whole module with core's bodies from the host sysroot (the ci leg links rsi's std, whose crate ids differ).
- Built: `bin/rust-front rsi-build` (batches of 6, the ported passes to runtime MIR, the plan's edition and overflow checks, the
  program's crate and file names as rustc's), `bin/rsi check --front own` (`--record` refused with it until RFP42), `bin/rust-front
  rsi-native` (by hand: the native leg, since std's lowered library has never been built on this host), the Makefile's
  `rust-front-rsi-build`, `rust-front-rsi-native`, `rsi-check-front-own`. Ports from rustc 1.99.1 (`58cf8f9c3`, rust-src-1.99.1-r0):
  rustc_abi's `layout_of_union`, rustc_codegen_ssa's `project_subslice` and exposed-provenance casts, `Generics::own_args_no_defaults`
  in the v0 paths; core's integer constants and scalar promoted constants evaluated; Assert, method-call and overloaded-index caller
  locations as rustc's `fn_span`; codegen fixes found by the runs (i128 folding, widened unsigned constants, generic float
  intrinsics, ClosureFnPointer, a never-assigned scalar return, the front end's ADT names). The pack `rust-front-built`'s dumps now hold
  `@ROOT@` (they named `rust-rf10b`'s worktree, so the essential step's built judge gave 51 of 62 in any other checkout), made again.
- Results: 47 of the 67 synthetic programs own (0 of the six samples, measured once), 43 of the 47 give rustc's recorded run on the
  native leg; the four left: std's source paths of rsi's std build (3, RFP47) and a fault (`panic-vec-remove`). The 20 not own: vtables
  (virtual calls 5, unsizing to `dyn` 3), the repeat loop 2, float `pow` 2 (fixed after the run), CTFE, a transmute, an allocation kind,
  `tlsref`, `typed_swap_nonoverlapping`, three `main`s not ours. `bin/rust-front essential` passes against its known file; crops-score
  unchanged (Rust 75.0 / 75.0 / 95.8 / 80.0): an own case still consumes rustc's records, so the eval stage stays 0.5 (RES54 (a);
  `independence.edn` counts the own cases). Proposed: RFP42-RFP47.


## 2026-11-09: C++'s programs by Arbace's own route, the refusals' biggest groups closed and the switch prepared (branch `cpp-route`)

- **Measured** (the run corpora's 82 programs, bin/cppi check's lists: samples, runtime, gxx-dg-run's every-25th; `arbace.cpp.codegen
  gen-batch` as `cppi_ir_route` runs it, no clang): 45 generated at the day's start (e6214cd13), **55** on the branch; the refusals by
  cause in doc/cpp/CPP-SURVEY.md, "As built: cpp-route". The sampling by our route alone (`CPPI_ROUTE=ours bin/cppi check --corpus
  gxx-dg-run --sample 25`, no clang): 45 generated and run on ci, **44 pass by their own verdict**, 1 the oracle-text class as under
  clang, none runs wrong (two more built after, range-for14 and ucnid-1, pass alone); two of the passes (special/initp1.C, cpp0x/defaulted19a.C) are programs clang rejects. Under auto the
  sampling projects to 62 passes (clang's record 60), none lost. The samples' and runtime's runs are the night's (ci's builder and the
  runtime's read pass 2.3 GB with their iostream units).
- **Built** (doc/cpp/CODEGEN.md, G5d; arbace/cpp/sema.ae, codegen.ae, template.ae, arbace/repr/cpp/parse.ae): structured bindings;
  GNU's `x ?: y`; [expr.cond]'s class-to-scalar operands and same-type rule; aggregates' default member initializers in place with
  their `this`; value-initialized list fills; lists bound to references; deduction from a reference-typed expression's type; function
  template redeclarations by their non-type parameters' types; clang's type-parameter test (std::pair's C++17 constructors were declined
  by the parser); default construction and implicit conversions by constructor templates; explicit template arguments kept for
  argument-dependent lookup; classes through an ellipsis and va_arg of them (the psABI's §3.5.7); the implicit global allocation
  functions; `__decltype` at a declaration's start (std::sort); GNU's statement expressions and a range-for's init-statement; universal character names in identifiers (CP337 (b)); the
  unexpanded-pack diagnosis for a non-type parameter's type. Five samples of test/cpp/codegen (struct-bind, gnu-cond, va-class,
  agg-dmi, stmt-expr). Tooling: runtime-ci's incremental read no longer drops read.tsv
  when nothing changed, and `CPPI_CI_CHUNK` reads the runtime in chunks.
- **Why**: the user's CP240 (b), CP263 (a) and P21 (b) (2026-11-07, 2026-11-08): C++'s eval and AOT count 0.5 while the programs run from
  clang's recorded IR.
- **Checked**: codegen-gen: 148 of 149 forms a0c6ad347's byte for byte, the 149th (recursion's bool conditional, now clang's `phi i1`)
  intended; the record, stale on next since G5c's merges (120 of 149), recorded again: 154 of 154. The dg check's own front end on the
  every-10th sample: 117 own passes as recorded after cpp0x/variadic170.C's diagnosis (it had passed by a parser decline), 118 at
  the head (cpp2a/desig29.C). The sampling's AOT level by our route alone (`--aot`, cf2g and TamaGo's go): 44 of the 45 generated
  pass, as the interpreted level.
- **Not done** (proposed CP330-CP337 in the survey): inheriting constructors (std::unique_ptr), std::tuple's constructors, coroutines,
  GNU vector types, the checker's structured bindings, the eval and AOT facts' weighting after the switch, the generation's time.
- **Alternatives considered**: running the day's samples and runtime by gcc on the lowered C (the oracle tier's judge, not allowed for
  an own measure); counting the generated programs without running them (no credit without the run); taking the night8 run's whole ci
  library (its reader is older): only the 13 units no day process could read were taken, their lowered C byte for byte the checkout's.
- **Slips**: one process substitution in a throwaway comparison; one kill loop that matched its own shell. Recorded in the survey.
