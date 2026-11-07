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
