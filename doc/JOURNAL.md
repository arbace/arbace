# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-reaps-3-crops-v2`, by the user's decision (2026-10-26). It
started over the same way at the two freezes and after the milestones `arbace-inside-itself-v1`,
`arbace-interprets-go1.27.1-v1` and `arbace-reaps-3-crops-v1`. The history before it is on the frozen branches, at four
tags, and in git history:

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

## 2026-11-01: the journal starts over

- **The user's decision** of 2026-10-26 (the milestone `arbace-reaps-3-crops-v2`, an annotated tag as v1's, main's
  journal starting over again), and the goal of 2026-10-28 ("reach milestone arbace-reaps-3-crops-v2"), whose last
  items were the tag and the journal reset: after the milestone `arbace-reaps-3-crops-v2` (a tag on main, not a
  freeze, so no branch) this journal starts over.
  - The earlier one, 871 lines and 34 entries from 2026-10-27 to the tag's entry, is at the tag (linked above).
  - CLAUDE.md's Records paragraph, the agenda's header and README's documentation list name the six places of the
    history.
- **Where things stand** (doc/AGENDA.md): the REAP matrix at the tag, 12 of 12 cells checked, none failing, every
  non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v2's divergences", with its caveat: Java's timeouts by
  design by the catch-all `speed` class, curated after the tag); the gate keyed (GT1-GT7), its clean record
  `test/gate/clean-arbace-reaps-3-crops-v2.edn`; the seed `73b0d637`; parked (the user, 2026-10-26): the box,
  repr/haskell, repr/rust, the macOS UI, the iOS app.
- **Next:** the merge window, in the order VM rehearsal 7 tested (`mac/112-merge-cp`: the gate's branches, the audits
  and their fixes, the crops C23, net/http, OCaml, Scheme and C++23, then JN10 with the provenance writers; one
  essential gate after the gate branches, one `bin/gate --full` at the end), then the batched re-seed, then C23's
  C3b-C9 toward `arbace-reaps-4-crops-v1`.

## 2026-11-01: the user's decisions after the tag

The open decision ids of the waiting branches (VM reports 097, 102 and 103, five sheets), asked after the tag. Each
branch's survey gets its "Decided" section when it merges (the surveys are on the branches, not on main yet).

- **The gate's audits:** GTS102 (mac/034-gts102): redone on the merged audit-075 and audit-070, the decision kept;
  GTS140-GTS143 (audit-060): 140 (a), 141 (b), 142 (b), 143 (a) now and (b) at the next record; GTS180 (a)-(e)
  (audit-075): the source's picks ((a) the bumps lowered after the next clean run, (b) CPU 20 s and wall 90 s if
  measured under 5 s, else 120 and 480, (c) `:timeout` recorded, (d) kept with `--save` copying
  `timeout-attempts.tsv`, (e) 137 left); GTS170-GTS173 (audit-070): ratified as built (170 (b), 171 (a), 172 (a), 173
  (b) after the merge with audit-041); GTS150 (mac/074-clean-judge): (a) kept; GTS130-GTS133 and GTS137 (audit-061):
  130 (a), 131 (c) then (a), 132 (b), 133 (b) at the next reader change, 137 (a) after the first record;
  GTS134-GTS136 and GTS138 (audit-065): 134 (a) and 138 (a) as built, 135 (a) regenerated on the host after the crops,
  136 (b).
- **C23** (c23-c7): CS236 (a), CS237 (a), CS238 (a); CS239 (a): `-I-` refused, a class of its own.
- **OCaml** (ocaml-o1): OC130 (b) with OC120 (b), OC131 (a), OC160 (a): a light oi-divergences step in the merge's
  essential gate.
- **C++** (cpp-cx7): CP140, CP141, CP152, CP154 (b) (154 once 140 is recorded); CP142 (b), CP143 (a), CP150 (a),
  CP151 (b), CP153 (a).
- **Scheme** (scheme-sm1): the 36 ids VM report 102 found decided by what was built (SC20-SC25, SC30-SC32, SC40,
  SC42-SC46, SC50, SC60, SC62-SC70, SC80, SC81, SC83-SC85, SC91, SC92, SC97, SC99, SC204) confirmed as built, SC66 as
  built ((b), not the earlier (a)); the 6 moot (SC26, SC47, SC48, SC201, SC202, SC205) superseded by SC112, SC211, SC210
  and SM5; SC61 (a), SC86 (a), SC93 (b), SC33 and SC49 following (si shipped through arbace.lang.SchemeInterp, at the
  heavy window); SC90 (b) with SC94 (b); SC95 (d) then (b); SC41 (b) with the next boot batch; SC82 (b), Chibi's
  srfi/1 suite first, pinned; SC96 (b) and SC98 (b); SC200 (b), the small identifiers. SC203 was not asked: VM task
  105 built its recommended (b), the larceny inputs pinned (`mac/105-sm1`).

## 2026-11-01: GTS81 (a) for the crops' checks (write-ahead, queue 064)

- On `mac/064-gts81` (from `mac/054-merge`, `a69c1dfc`), in the light tier: negative controls, GTS81's crossed pairs, for the
  crops' checks gate-post-v2 left: `bin/si check` (`test/scheme/si/controls.edn`), `bin/ci check` and `check --aot`, `bin/ci
  dg` (`test/c/controls.edn`, `:run` and `:dg`), `bin/oi check`, `check --aot`, `suite check`, `suite expect`
  (`test/ocaml/controls.txt`), `bin/cppi lower-check` (`test/cpp/controls.edn`), `bin/goi check --corpus nethttp`
  (`test/goi/controls.txt`, LEVEL nethttp). Each judge's control code tested on copies (joker for the `.ae` judges, the shell
  functions extracted and run alone) over the real data files; no check run (no Arbace executable on the VM). The pairs from
  the records; the C dg pair's directives read in gcc 15.2's testsuite (gcc-mirror/gcc, `releases/gcc-15.2.0`,
  `gcc/testsuite/gcc.dg/bitint-75.c`, `c23-attr-syntax-3.c`), the C++ pair's outputs by g++ 15.2 on the VM.
- The design table, the families still without a control and their proposals: doc/GATE-SURVEY.md, "As built: GTS81 (a), the
  crops' controls". Alternatives considered: a deliberately failing case of its own per family (a file or program added to
  each corpus) instead of a crossed pair: rejected as GTS81 did (a case of its own needs its own run, reference and
  upkeep, and goes through a code path of its own).

## 2026-11-02: the waiting branches merged, C8, C9's line, `--full` at 504ab60d

- **The merge window** (journal 2026-11-01, "Next"): one merge commit `7d9c84d6` brought in VM rehearsal 7's tested line
  (`mac/112-merge-cp` `56fbb8cf`: the gate's branches and audits, the crops C23 to `c23-c7`, net/http, OCaml, Scheme and
  C++23, JN10 with the provenance writers), then C8 (`c23-c8`, `8c61bd89`: C's row of the REAP matrix, 16 of 16 cells;
  doc/C23-SURVEY.md, "As built: C8, the fourth row") merged at `619f7335`. `bin/gate --full` at `619f7335` failed in 15
  steps (log `.tmp/merge-gate/full-619f7335.log`); the branch `full-fix` (`504ab60d`, ten commits) fixed them, among them
  `bin/goi check` in Go's test driver's environment (`1ebd4ee6`; issue21317 and linkobj pass, `c430b01e`) and
  `:unstable` classes accepted in Java's levels judgment (`a3bd7d31`; the user kept `bin/goi`'s `run_env` and accepted it).
  `bin/gate --full` passed at `504ab60d` (12:15:08; 85 steps run, 3 skipped, 368m55s).
- **The user's decisions (2026-11-02), all as recommended:** CS240 (a) at C9 (C's checks write results files at the
  window), (b) until then; CS241 (a); `:runtime-source` by design (`clojure/bad_def_test.clj`, written at run time, loads
  from source); the `time_zones` form's relative offsets; `-buildvcs=false` for `tools/gateaudit` at the window; CS250 (a)
  (the window's order), CS251 (a), CS252 (a), CS253 (b) for v1 and (a) after (musl compiled), CS254 (a) (`062de7d1`); the
  re-seed for `arbace-reaps-4-crops-v1` embeds C only (`d6051d79`); GTS102's 14 lines set by the audited CPU at 2.5x
  (`05c2b424`); CS255-CS259 (the re-seed's guards) and CS260-CS264 (C's classes' decisions) all (a) (`6fd02a6e`); VM audit
  149: `print-classes` narrowed to `test/java/print.ae`'s named kinds, the 72 others the deferred `print-classes-unnamed`
  (`818ff165`), `speed` keeping its caveat for this tag; VM report 140: C's full checks as heavy `--full` steps, C's pins in
  `--pins` (`c23-full`, merged `1c78baee`); W1 (the window takes C's line alone), W2 (the window starts when the host is
  idle), W14 (amd64 only at step 3); the frozen jar and the reference JDK on Java 26 explicitly, from jdk26u's own build
  (`24585ae8`, `1830be87`; the system default had become OpenJDK 25), the edge recorded again (`1c8e55e2`: 8 classes);
  go-whim's commit and ccorpus2's module hash in `--pins` (`bc4e4c5a`); the round trip's levels 1-2 control and the C steps'
  narrowed keys after the tag; `torture-execute-mirror` collapsed to its 101 files (VM report 156, `1a385469`); CS265 (a),
  CS266 (a), no record writes `:unclassified` (`b7e8e8f6`); the next REAP section is "REAP v3's divergences" (the rounds
  numbered, not the crops).
- **C9's line** (`c9-line`, `e56a017d`: C9's preparation, queues 132-139's fixes) and `c23-full` merged on main
  (`1c78baee`); the essential gate failed on the JVM's version and an intermittent read of the audit (`2a39402a`: fchdir
  traced), then passed at `2a39402a` (14:46:56, 26 steps run, 11m02s).

## 2026-11-02: the test budget (the user's decision)

- **Decided:** `arbace-reaps-4-crops-v1` is tagged on C's full steps in a keyed `bin/gate --full` overnight and the audit of
  C's new steps; **no all-crops clean run** for this tag: it waits for compressed corpora. The next milestone's main thread is
  **GT8, the test budget**: tiers essential at most 15 minutes, nightly at most 6 hours, clean at most 24 hours, for all ten
  planned crops (Clojure, Java, Go, C, C++, OCaml, Scheme, Haskell, Rust, Python), by deduplication, coverage subsets,
  rotation and synthetic corpora; heavy runs at night, development by day; the Mac peer takes small development tasks
  (154-156, then up to 165).
- **Why:** v2's clean run took 190,621 s over eight runs for three crops; C's seven full steps add about 7 to 8 hours to
  `--full` serial; six more crops would multiply that past any night or day. C9.2's row 12 (`--full --clean --tag`, VM report
  153 F2) is superseded.
- **Alternatives considered:** the clean run as v2's for this tag (rejected: a day or more of the host for every tag, and the
  next crops' corpora larger still); C's full checks left out of the gate, recorded by hand only (rejected on VM report 140:
  §11's C9 asks for `--full` with C's full checks).
- **Coverage subsets** (VM reports 160, 161): `torture-execute`'s 59 of 1,532 programs keep every back-end block (6% of
  the wall time); 229 programs over C's run corpora, the subset check 939 to 242 s. The user: optimise for the back end's
  blocks, the kept classes, the controls and one per class; keep every deferred and gap program; a `:subset` per corpus.
  Held until after the tag.

## 2026-11-02: C9.2, the re-seed window

- **What:** C9.2's steps (doc/C23-SURVEY.md, "As built: C9.2, the re-seed window", with each step's time) from 14:47:25 on
  main `2a39402a`. Steps 1-5 passed (the executable built from the merged tree, `aot: 156 sources compiled, 14886 classes; 0
  failed`; `bin/ci check` on it, the 15 run corpora judged, 36 min). Step 7 (`bin/ci annotations --record`) wrote 77
  `:unclassified` entries: no regression (45 the old writer's, the judge's own classes; 30 error tests gcc rejects at their
  options, CS265; 2 the checker's, `transparent_union`); step 7a's tree check wrote 335 by the same writer. Main moved to
  `5de0939b` (the writers fixed, the mirror collapsed, the pins, Java 26 from jdk26u), and 7a ran again (7a2): 3, 276 and 2
  files of no class, classified by hand (`14873a86`: the 276 C7's decided grammar, the class `:gcc-syntax`, CS267 proposed;
  `255c9d54`: `:c23-colons`, `:xid-identifier`, `:run-address`, `:bad-specs-print` proposed), merged `b2a4911f`.
- **The seed:** the fixpoint at `5de0939b` in the clean worktree `seed-c9` (G2 = G3, 35 min; stage 0 = G1 = G2 = G3 is `--verify`'s, below), `bin/seed --make` from G3
  (the seed `a59cc441…`, commit `1fe1063f`), `bin/seed --verify` passed (the same archive, stage 0 = G1 = G2 = G3
  `606d45e6…`, 1,724 s). The VM's review of the seed skipped (no VM work overnight, as told the user): `--verify` is the check.
- **After the seed** (step 11, the night): stage 0 from the new seed, `bin/reap` 16 of 16 with 0 problems, `bin/ci dg
  --essential` ok; `bin/ci judge-test` failed (3 assertions) and step 11a (`bin/ci check --aot`) found two regressions and one
  improvement (the next entry). The keyed `bin/gate --full` (step 12) started at 22:59:38 on main `b2a4911f`.

## 2026-11-03: the night's three fixes

On branch `night-fix` (from `b2a4911f`), not merged while `--full` ran on main:

- `04947f0e`: judge-test's two host-dependent tests (the in-process back end is cinterp's var on the new stage 0; the
  compiled parser reads 30,000 nested parentheses, so the panic is stood in by `with-redefs`); `arbace/c/repl.ae` left alone
  (stage 0 runs its embedded copy; a source fix waits for the next re-seed). The AOT record's pass of `20101011-1.c` (in
  `torture-execute` and its mirror) was stale: it fails with status 136 on the previous seed's stage 0 too (a worktree at
  `5de0939b`); re-recorded as known `signals`, as at the interpreted level; `c23-float-6.c` now passes. C AOT 2,058 / 115 / 87.
- `c14a5eca`: the gate's `c-c2j` failed at 02:16 with 0 sources: the host embeds C's namespaces compiled since the re-seed,
  and their compiled loaders record no units; `bin/ci c2j-check` runs with `ARBACE_NO_AOT=1`, as `bin/lib/program.bash`'s
  `host_compiles`: 25 sources, 0 failed.
- `d16ae499`: the classes as the user decided in the morning (the next entry).
- The night's script was stopped by hand at 05:21 (the user: C's full steps rerun the next night), so the audit of C's steps
  did not run; the `--full` itself went on with its other steps, stopped by a watcher (`.tmp/window/stop-before-c.sh`) when
  its first heavy C step started. TODO(tag): the time it stopped and the other steps' outcome.

## 2026-11-03: the user's decisions, and the schedule to the tag

- **CS267 (a)**: `:gcc-syntax` by design (the tree check's constructs cc's parser lacks or reads otherwise, read as gcc does;
  CS40 (a)'s rule). **CS268 (a)**: the `--record` writers guess `:pp-errors` only when the differing errors are the
  preprocessor's and never fall back to another check's classes; built **after the tag** (repr/c is embedded: a change reruns
  the whole `--full`; VM task 167). **`:bad-specs-print` deferred for v1**, CS221 (a) with CS232 (a) after the tag (VM task
  168). `:c23-colons` and `:xid-identifier` reworded to what still differs since CS143 (a)'s lexer. All in `d16ae499` and
  doc/C23-SURVEY.md's new section.
- **Alternatives considered:** CS268 built before the tag (rejected: a whole `--full` again for a writer the tag's records do
  not depend on, since they were classified by hand); CS221 (a) with CS232 (a) now (rejected for the same cost).
- **The schedule:** `night-fix` merged into main; C's full steps rerun keyed on the night of 2026-11-03 (the other steps
  skip at the keys they passed), with the audit of C's new steps; the tag `arbace-reaps-4-crops-v1` on 2026-11-04, its
  documentation on the branch `tag-docs` ("REAP v3's divergences", doc/v3-record/, the C9.2 section, CLAUDE.md, README, the
  agenda). The Mac VM's branches 154-165 held until after the tag (doc/AGENDA.md, "After the tag").

## 2026-11-03: GT8 and the backlog before the tag (the user's decision)

- **Decided (the user, 2026-11-03, superseding the schedule of the entry above and the 2026-11-02 test budget's "no
  all-crops clean run"):** `arbace-reaps-4-crops-v1` waits for GT8, the test budget, and for the whole backlog: the Mac
  VM's held branches (154, 164, 155, 157, 158, 159, 161, 162, 163, 165, then 167, CS268 (a), and 168, CS221 (a) with
  CS232 (a)), Java's speed curation (GTS102's per-test limits, the `speed` catch-all's timeouts judged one by one) and the
  round trip's negative control at levels 1 and 2 (VM report 159). The tag is made on an audited clean run of all four
  crops in a fresh clone over GT8's compressed corpora, within GT8's budget.
- **Why:** the held branches change ci, which is embedded (`gate_key`): merged after the tag they would rerun the whole
  `--full` anyway, and the tag's caveats (Java's `speed`, the round trip's levels 1-2, the coarse C keys, no clean run)
  are the backlog's own items. Putting them first makes the tag's record carry fewer caveats and a clean run.
- **Alternatives considered:** the tag on the next morning on a keyed `--full` (the entry above; rejected by the user:
  the backlog and GT8 would follow it at once and invalidate its runs); GT8 after the tag (the plan of 2026-11-02).
- **Done at once:** the keyed `--full` stopped by hand in goi's AOT check (05:4x; its passes stay in the store at their
  keys); `night-fix` merged into main (`d16ae499`) with the parts of `tag-docs` that hold before the tag (the journal, the C9.2
  section, the AGENDA, the class texts); CLAUDE.md, README and "REAP v3's divergences" stay on `tag-docs` until the tag.

## 2026-11-03: GT8's survey and its decisions

- doc/GATE-SURVEY.md, "GT8, the test budget": the tiers against today's times (essential 11 min; `--full` about 11.8 h
  serial with C's full checks; v2's clean run 190,621 s for three crops), where the time goes (Java about 258 min, C about
  300, Go about 75, Clojure about 33), the methods GT8.1-GT8.7 and proposals GTS200-GTS209. **Decided by the user,
  2026-11-03: all (a).** The Mac VM took GT8.1's cost table (task 170) and GT8.2's duplicate finder (task 171).
- **Alternatives considered:** per-crop shares by weight (GTS201 (b)), a weekly long run instead of a nightly slice
  (GTS202 (b)), the full corpora at the clean run (GTS204 (b)), Java's speed curation after GT8 (GTS208 (b)): see the
  survey's options.

## 2026-11-03: Haskell and Rust unparked; Python's survey

- **Decided (the user, 2026-11-03):** repr/haskell and repr/rust, parked since 2026-10-26, are unparked: crops in progress
  again, as OCaml, Scheme and C++ are (evaluated from the checkout, out of `gate_key`, no gate step until their row of the
  matrix fits GT8's budget). An initial survey of Python as the tenth crop starts (doc/python/PYTHON-SURVEY.md).
- The host still has the pinned toolchains: GHC 9.14.1 (`tools/hsdump/GHC_VERSION`), rustc 1.99.1 `58cf8f9c3`
  (`tools/rsdump/RUSTC_VERSION`); Python 3.14.10 is the host's (Alpine's) Python.

## 2026-11-03: Python's proposals decided; the Mac VM takes the crop

- doc/python/PYTHON-SURVEY.md (merged `4658fe21`): Python as the tenth crop, milestones PY1-PY11 (about 57 to 98 working
  days), proposals PYS1-PYS20. **Decided by the user:** all as recommended (CPython 3.14.10 pinned, a dump helper first and
  a pegen-generated native reader later, pyi on its own IR in `go/arbace/py` with Go's GC and a GIL, the stdlib as forms
  with about 25 Go ports, AOT to Go forms, `PYTHONHASHSEED=0`, subsets from day one, out of the gate until its row fits
  the budget), and PYS19: **the Mac VM peer does all of Python**, PY1 to PY11 task by task, from now, reviewed and merged
  here. The first tasks are 174 (PY1) and 175 (PY2) in the queue.
- Also today: Haskell and Rust checked after unparking (nothing broke; tests pass, 40 s and 58 s; sample round trips
  clean; whole text levels about 1 to 1.5 h and 40 to 60 min, night jobs); their eval needs GHC's Core/STG and a lazy
  runtime, and Rust's MIR and a Miri-like runtime, each a survey of its own.

## 2026-11-03: Haskell's and Rust's eval surveys decided; Scheme, C++ and OCaml rows on branches

- doc/haskell/HASKELL-EVAL-SURVEY.md (merged `9bd523bd`): eval interprets GHC 9.14.1's final STG obtained by a GHC API
  driver, an eval/apply machine `hsi` with green threads, the boot libraries compiled as installed with the pure-Haskell
  bignum, AOT STG → Go forms; HE1-HE11, 54-88 days. doc/rust/RUST-EVAL-SURVEY.md (`465473b8`): eval runs rustc's -O0 LLVM
  IR through C++'s lowering on ci ("rustc compiles, Arbace runs", a recorded divergence), std built from the pinned source
  with its IR kept, AOT by cf2g; RE1-RE9, 33-58 days. **Decided by the user: all as recommended (HES1-HES21, RES1-RES16);
  the trials HE1-HE3 and RE1-RE2 start now**, by agents here.
- On held branches until after the tag `arbace-reaps-4-crops-v1`: Scheme's row (`scheme-sm9`, 20 of 20 cells with it),
  C++'s row (`cpp-cx10`, with CX9 on `cpp-cx9`; CX8 running), OCaml's row (O8, running). The user decided: C++ and Scheme
  embed at the first re-seed after the tag (CP180, SC230); CP181-CP184, CP210-CP216, SC231-SC235 as recommended.
- Python on the Mac VM: PY1-PY3 done (spec and pins; read and print over 2,800 files; `:full` byte for byte with
  `marshal` equal on 2,795), the user's decisions: two parses for type comments, 20 pinned site-packages apks,
  `:code-lines` by design for `:lines`, `:charset` by design; PY4 running.

## 2026-11-04: the day and the night; the staging branch `next`

- The user pinned the night shift to 12 hours, 19:00-07:00 UTC, for gating, testing, details and the bugs checks find; the
  day for development and design with sample checks and occasional light gating. Asked for my opinion, I proposed: plan 8
  of the 12 hours (4 of slack for fixes and reruns), the clean run in one night (two at most, resumed by `--clean --only`,
  `main` frozen between), and day merges through a staging branch rather than an essential gate per merge (the alternative
  considered: one or two batched essential gates a day). The user decided all three, with the staging branch:
  doc/GATE-SURVEY.md "Decided (2026-11-04, the user): the day and the night" (GTS224-GTS226, amending GTS209), doc/AGENDA.md's
  tier table.
- `bin/lib/gate-cache.bash`'s `arbace_main_checkout` no longer writes `${TMPDIR:-.}/gate-cache-git.err`: unset TMPDIR left an
  empty file in the checkout's root; git's error now joins the output (not a directory: the fallback).

## 2026-11-04: the day's development, held for after the tag

The user asked for deep-thinking development by day (the tokens otherwise lost) with sample checks only, no corpus runs
and no gates: "bringing all the crops' readers and printers on par". Agents (Opus at max or high effort, Sonnet for the
smaller parts), each on a branch of its own, held for after the tag (the merge plan: `.tmp/tag-v2/merge-plan.md`):

- **Native readers.**
  - OCaml (`ocaml-native`): Menhir's LALR construction ported, its tables generated from OCaml 5's `parser.mly` and
    checked state for state against the tarball's `boot/menhir/parser.ml` (2,327 states); `lexer.mll` ported.
  - Haskell (`haskell-native`): Happy's construction ported, the tables from GHC 9.14.1's `Parser.y` equal to the
    tarball's `Parser.hs` (1,537 states); `Lexer.x` hand-ported with the layout rule; PostProcess and the Haddock pass.
  - Rust (`rust-native`): `rustc_lexer` and `rustc_parse` of rustc 1.99.1 ported rule for rule; the identifier tables
    from the `unicode-ident` and `unicode-properties` crates rustc pins (`rust-unicode`).
  - C++ (`cpp-native`, `cpp-native2`, `cpp-sema`, `cpp-tree`): CX11, the standard's grammar with clang's disambiguation,
    clang's AST as data, a minimal Sema (defaulted noexcept, init-list forms, CTAD, dependent members), a node-by-node tree
    check against tools/cxxdump.
  - LLVM IR (`llvm-read`, `llvm-full`, `llvm-verify2`, `llvm-verify3`): the reader's gaps (debug records, structured DI
    nodes, callbr, Windows EH, summaries, ptrtoaddr), mode `:full`, and a verifier ported from LLVM 23's Verifier.cpp and
    LLParser's checks, judged against `opt -passes=verify`'s whole stderr; its messages taken from libLLVM-23's strings
    (LLVM's Apache-2.0 with the LLVM exception: LICENSE.md at the merge).
- **Error messages equal to the compilers':** OCaml's (`ocaml-errors`, `ocaml-errors2`), Haskell's (`haskell-errors`,
  `-errors2`, `-errors3`: GHC's pretty-printer and its 123 parser codes ported), Rust's first error (`rust-errors`,
  `-errors2`).
- **Haskell's preprocessing:** native unlit and a traditional cpp. The first cpp was a port of GCC 15.2's (GPL-3.0); the
  user decided (2026-11-04) to reimplement it clean, written from the documented behaviour and judged against gcc's
  `cpp -traditional` as a black box (`haskell-cpp`); haskell-pp and haskell-cpp merge as one squashed commit so the port
  never enters main's history.
- **The C++ lowering takes -g IR** (`cpp-lower-dbg`); the embedding of C++, Scheme and OCaml's oi prepared (`embed-crops`,
  `embed-gaps`; CP191 decided (a) by the user: ci's `arbace/c` reloaded by RT.toolSource); negative controls for the
  crops' read and print checks (`crop-controls*`); the evaluator's speed (`eval-speed`, doc/go/EVAL-SPEED.md: calls,
  arrays, reflection, protocols; a runtime change, held for the re-seed window).
- **goi:** its classes reduced (`goi-classes`: gc's build flags in the reference, gc's type-size checks, `//line`
  directives, types2's go/defer check) and the frames class (`goi-frames`: runtime.Caller and gc's traces from the
  interpreted stack): 1,620 → 1,664 of 1,837; both merged into `next` for night 4's gate. Its checker now rejects
  `for _ := range x` ("no new variables", main 01d3ce9f).
- **GT9** (`gate-crops`, doc/GATE-SURVEY.md "GT9: the crops' gate"): the seven ungated crops' gating plan; GTS210-GTS223
  decided (a) by the user, implemented after the tag.
- **The Zed survey** (`zed-survey`, zedlanguage/zed at d00a13469e30): parked by the user, "let Zed reach maturity".
- **Python on the Mac VM:** PY6 done (104 modules, 14,132 methods, 0 failing); queue 201 done as one branch,
  `mac/201-python-py7-py11` 45ce1e92 (PY6-PY10, PY11 prepared); the user's decisions: finalization's 47 methods by design
  (PYS9 (a)), no re-seed before Python's tag, the compiled loop judged in its clean run, expat ported to Go forms
  (pyexpat (a)); queue 202 next.
- **Housekeeping:** the disk from 83% to 47% (merged and old worktrees removed, the user's OK for the older lines;
  night 3's raw coverage and the speed reruns' work directories; `bin/gate --prune`); a stray `/.tmp/tmp` (an empty root
  in a TMPDIR, 2026-11-03) removed. The account switched (`/login`): both hosts switch together next time.

## 2026-11-05: the re-seed before `arbace-reaps-4-crops-v1` (night 5); GTS228; the audit's reducer race

- **Night 4's coverage run (2026-11-04, `.tmp/night4/`):** the cover build ran Java's compiled level (20:28-22:57,
  passed) and Clojure's AOT level (failed only on test.generative's "no result", the cover build's slowness; every other
  namespace equal to the plain build's). Clojure's AOT run takes about 13 minutes, so it needs no subset.
- **GTS228 (a), branch `java-compiled-subset` (2c939eec):** java-compiled-full's nightly slices now run the compiled
  level's coverage subset, `test/java/corpora.edn`'s `:subset-compiled` per corpus (`:subset-key :subset-compiled` in
  `test/gate/steps.edn`). It holds 1,905 of the 3,322 tests: every covered block, one test per class, the kept classes'
  tests and the controls' subjects (`bin/cover subset`, GTS205 (a)'s rule). The sub parts: jdk-run 1,190, jdk-other 520,
  langtools-run 89, hotspot-compiler 75, hotspot-runtime 22, jls 9. The clean run still runs every test.
- **The reducer race, branch `audit-race` (344dcb07):** night 5's first gate of `next` failed at 23:35 on gate-tests
  ("files the step reads outside its :inputs: 1, e.g. go/arbace/goi/ops.ae"), a step that had passed at 20:01 on the same
  commit. Cause: strace can print a new child's first calls before its parent's clone result. A relative open by such a
  child was resolved against the checkout's root, not the parent's directory, and generated-check-test's fabricated tree
  opens `ops.ae` by its bare name. Fix, in both reducers (`test/gate/audit.awk`, `tools/gateaudit`): the path of a relative
  call by a process not yet born is the result's own path when `-y` names it (`= N</abs/path>`). `audit-trace-test.bash`'s
  new case `earlyrel` failed on the old awk and passes on the fix (19 of 19). The relaunched gate passed (23:57).
  Alternatives: a retry of gate-tests (it hides the race); buffering an unborn child's lines until its birth (heavier in
  both reducers, and the result path makes it unnecessary for opens).
- **The re-seed (`.tmp/night5/reseed.sh`, 23:37-03:14 UTC), every step passed:**
  - `next` gated and main fast-forwarded;
  - the pre-seed C checks passed, with `bin/ci check` judged against the record (no `--record`);
  - the build: 157 sources, 15,267 classes, 0 failed;
  - `--fixpoint` held (G2 = G3);
  - `bin/seed --make` packed G3 as seed `d5548e72…`, committed as 496d33485;
  - `bin/seed --verify`: stage 0 = G1 = G2 = G3, `d503777b…`;
  - stage 0 rebuilt from the new seed;
  - the PATH_FIRST trio dropped from the nine C steps (d86cb874a);
  - C's light checks and `bin/ci check --aot` passed without the trio;
  - the essential gate passed on the re-seed (26 steps run, 31 min);
  - main and `next` advanced to d86cb874a, main pushed.

  Before the launch, Go's build cache was trimmed from 39 GB to 3 GB (`gocache_trim`, ARBACE_GOCACHE_KEEP_MIN=20) to meet
  the precondition of 60 GiB free.
- **The keyed `bin/gate --full --concurrent` (GTS230) started at 03:14 on main,** with a deadline of 16:00 (GTS231-234 (a):
  overflow into the day allowed).
- **Merged into `next` (2026-11-05 morning):** `java-compiled-subset` and `audit-race`, for tonight's gate before the
  clean run.

## 2026-11-05: the day's deep-thinking work, held for after the tag; the user's decisions

The user asked for high-thinking work by day: the crops more mature, more complete, less dependent on the host's packages,
with no heavy load on the host. Seven agents (four on Opus, three on Sonnet) worked on branches with sample checks only;
everything is held for after `arbace-reaps-4-crops-v1` and pushed to `mac`.

- **OCaml without `ocamlc`** (`ocaml-typer`, OC4 (b)'s route): OCaml 5.5.0's typing/ and lambda/ (Matching, Switch) ported,
  about 3,900 lines, `doc/ocaml/TYPER.md`. On 36 samples the native Parsetree's types equal the toplevel's, the raw Lambda
  equals `-drawlambda`, and oi's output equals the toplevel's. T7-T12 are left, about 31-49 days.
- **C++ without clang** (`cpp-codegen`): `arbace/cpp/{abi,sema,codegen}.ae`, written from the standard and the Itanium
  ABI, `doc/cpp/CODEGEN.md`. 38 samples run on cppi like clang's IR, clang accepts our IR, and the mangled names are
  clang's. Templates and the library come next (G2-G6).
- **Java's deferred cases triaged** (`java-deferred-triage`, `doc/classes/JAVA-DEFERRED-TRIAGE.md`): 24 root causes, ranked by
  tests per day. The JCA alone blocks 176 eval and 168 AOT tests.
- **Go's by-design cases reviewed** (`go-design-review`, `doc/go/GOI-DESIGN-REVIEW.md`): AOT already passes 156 of goi's
  168 non-passes. `-race`, `GOROOT` and speed budgets bring eval to 1,673 / 164 on sample runs; the whole check is the
  night's.
- **Rust's eval, RE5** (`rust-re5`): the synthetic corpus (67 programs), the judge's self-test and the essential list. No
  program ran on ci by day: there is no UI source or rust-src on the host, and std for ci is a night job.
- **Haskell's hsi, HE5-HE7** (`haskell-he5`): running.
- **GT9 built** (`gt9-build`): the hosts as data, the crops' pins, night concurrency for light crop chains, the costs per
  crop, the crop rules, and per-batch step fragments. Also the keys commit, PYS30 (a): one exclusion list
  (`arbace_crop_paths`) for gate_key, the runtime key and the javac key, so later crop batches no longer rekey the heavy
  steps.
- **The merge plan rehearsed** (`.tmp/tag-v2/merge-plan-rehearsal-1105.md`): 26 of 44 steps conflict. The night cost
  drops sharply once the keys commit lands in batch 1.

**Decided by the user (2026-11-05), all as recommended:**

| area | proposals | where |
|---|---|---|
| Go | GOI-DESIGN-REVIEW's six | `doc/go/GOI-DESIGN-REVIEW.md` |
| Java | JCS128-JCS135 (JC9, then JC9b) | `doc/classes/JAVA-DEFERRED-TRIAGE.md` |
| OCaml | TY1-TY6 | `doc/ocaml/TYPER.md` |
| Rust | RES35-RES39 | `doc/rust/RUST-EVAL-SURVEY.md` |
| C++ | P1-P7 (clang's code ported only for templates and the ABI classification) | `doc/cpp/CODEGEN.md` |
| the gate | GTS240-GTS245 | `doc/GATE-SURVEY.md`, "As built: GT9 (write-ahead)" |

Each branch's doc records it.

**The keyed `--full` (03:14-08:39):** 201 steps passed and 9 failed, on three causes:
- **GT8.4's change to repr/clojure** (a11cf22a, 11-03) broke `repr-clojure`'s test and `java-fixtures`' provenance.
- **HugeToString needs memory beyond the check's 24 GiB cap**, failing `java-judge` and `java-held`; it is now classed in
  `:memory-limit`.
- **C's five steps:** the new seed's compiled parser reads gcc's `limits-*.c`, which the evaluated parser refused. The
  converter then overflowed Go's 1 GB stack, a fatal error that ended the corpus's run. A nesting bound in the parser
  (4,000) now fails such a file the old way. Until the next re-seed, stage 0 evaluates the changed parser from the
  checkout: torture-compile's parse check took 178 s, against 132 s compiled.

## 2026-11-06: C's deferred classes: variably modified structures, complex integers, synchronous signals (branch `c-deferred`)

- **What.** C's deferred cases ranked by cases per fix across the four cells (doc/C23-SURVEY.md, "As built: C's deferred
  classes closed"): `vla` (9 programs at eval and at AOT, all GNU's structures with VLA members) and `complex-int` (9, of
  which 3 were `_Float16`/`_Float128`/`_Float64x` complexes misclassed) first, then `signals` (20101011-1 twice). Built in
  `arbace/c/check.ae` and `arbace/c/build.ae` (a record's run-time layout where its definition stands, sizeof, offsetof,
  members, copies, varargs by reference; complex integer parts and gcc's wide division) and in `go/arbace/crt` and
  `go/arbace/ci` (crt's `Raise`/`SyncHook`, `Sys.SyncSignal`: a fault delivered to the installed handler where it is raised).
- **Why this way.** Only existing node kinds, so cf2g needs nothing; the signal at the fault's point (not after unwinding)
  so a handler's `siglongjmp` reaches its `sigsetjmp` frame. Alternatives: a run-time layout node in the back end (more Go,
  both back ends); delivering after the panic unwound (breaks siglongjmp). Clean-room: gcc's behaviour observed, none of its
  code ported. CS81 (b) was found not to hold (a one-line hand-written body keeps gcc -O0's nop; only one expansion's
  location drops it).
- **Proof.** The checkout's checker and builder `load-file`d over main's stage 0 (its `arbace.c.*` compiled; RT's
  `toolSource` does not reload `arbace/c/`), cirun by `bin/ci cirun-build`: `bin/ci check --only` on 124 programs, no failure,
  19 recorded failures pass; three new ci-run programs (`vla-struct.c`, `complex-int.c`, `signal-sync.c`) equal to gcc -O0 here.
  Deferred at eval and AOT 33 to 13 each; read 51 and print 78 unchanged.
- **Proposed** CS274-CS277 (a multi-item invocation printed as itself; self-include; threads over goroutines; va_arg_pack).
- **Left for the night (main host):** `bin/ci record-ref --corpus ci-run --only`, `bin/ci check --record`, `check --aot --record`,
  `bin/ci annotations --only` over the touched files, `bin/ci judge-test`, `bin/reap`.
- **Later the same day: CS275 (b)** (doc/C23-SURVEY.md, CDF.5): ci's read binds the converter's `*include-inside*` and the
  preprocessor's new `*reentry*` (the re-entered main file a CFile of its own, `MAIN#again`); a body keeps the items an
  `#include` inside it brought in under `*include-inside*`. Five self-including programs and 18_include pass (25 recorded
  failures pass on the 133-program sample, exit 0); 95_bitfields left in `read` (a packed attribute from a macro the copy
  defines is lost). Eval and AOT deferred 33 to 7 each. Alternatives: CS275 (a), the print's statement-level include, two days.
- **The user's decisions of the day (CS274 (a), CS275 (a), CS276 (b) by rust-re6's scheduler, CS277 (a)) recorded; CS277 built**:
  `__builtin_va_arg_pack` as a call's argument node `(apack)` forwarding the calling frame's variadic arguments (`Frame.VArgs`,
  both back ends, cf2g's `ci.Pack`), `__builtin_va_arg_pack_len` as `(vplen)`; va-arg-pack-1 passes. Alternative: inlining
  always_inline functions in the builder (CS277 (b)), two days.
- **CS274 (a) built, and CS275 (a)'s first half** (doc/C23-SURVEY.md, CDF.7): the print writes an invocation that made several
  top-level items or statements as itself (`:c/macro-stmt` on the first form, `:c/macro-part` on the rest); integer imaginary
  constants read as integers (`2i`); a declaration holding `#include`s between its statements is no longer refused at the print.
  `bin/repr-c roundtrip --only`: 24 of the 26 `objects-macro-body` files and 18_include and infback pass; a 131-file sample passes
  but its known; print deferred 78 to 51. Left: CS275 (a)'s self-including files at the print (a family's inactive groups as text).
- **CS275 (a)'s second half** (doc/C23-SURVEY.md, CDF.8): every `read-c` binds the preprocessor's `*reentry*`, the converter no
  longer refuses a self-including file, and an executed conditional family's groups the main pass skipped are kept as their text
  (`c/directive`), so the print's own second pass takes them. The six self-including files pass the round trip's four levels;
  print deferred 51 to 45. 95_bitfields at eval left (its copy's `#pragma pack` is no form): CS278 proposed.
