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
