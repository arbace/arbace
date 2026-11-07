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

## 2026-11-04: repr/haskell's native reader (branch `haskell-native`)

- **What:** Haskell text read into GHC 9.14.1's parsed tree by Arbace code alone, no GHC at read time,
  written as the helper's dump of mode `:lines` so that the converter makes the same forms
  (doc/haskell/NATIVE.md). Ported by hand from the corpus's pinned tarball,
  https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`), read in place:
  `compiler/GHC/Parser/Lexer.x.source` (with `Lexer/String.x.source`, `Lexer/Interface.hs`, `String.hs`,
  `CharClass.hs`) into `arbace/repr/haskell/lex.ae`; `GHC/Parser/Header.hs` and the extension flags of
  `GHC/Driver/{DynFlags,Flags,Session}.hs` into `header.ae`; `GHC/Parser.y.source`'s actions into
  `rules_module.ae`, `rules_decl.ae`, `rules_expr.ae`; `GHC/Parser/PostProcess.hs` with the builders of
  `GHC/Hs/{Utils,Type}.hs` into `post.ae`, `post_decl.ae`; `GHC/Parser/PostProcess/Haddock.hs` and
  `GHC/Parser/HaddockLex.x.source` into `haddock.ae`, `haddocklex.ae`. Licenses: GHC's BSD-style
  Glasgow license, its text added to LICENSE.md; origins in doc/ARBACE.md.
- **Decided: a generated LALR(1) parser**, not a hand-written one following `Parser.y`. `happy.ae`
  ports Happy's construction (LR(0), DeRemer-Pennello lookaheads, `%shift`, the default actions) and
  `tables.ae` is its output for the pinned grammar, proven equal to Happy 1.20.1.1's tables in the
  tarball's `Parser.hs`, state for state (907 productions, 1,537 states; `bin/repr-haskell tables
  --check`). Why: the layout's parse-error(t) rule is Parser.y's `close : error {% popContext }`,
  decided by where the automaton has no action; the 264 `%shift`-resolved conflicts and the one
  grammar of expressions, patterns and commands stay GHC's; a new GHC regenerates the tables and
  shows its new rules as actions missing. The alternative, decoding `Parser.hs`'s tables, would keep
  generated data with no generator; it is the check instead. The lexer is hand-ported with Alex's
  semantics rather than a port of Alex's DFA construction (small regular expressions, code actions).
- **CPP, unlit, hsc2hs** stay GHC's driver's: the helper's new verb `cpp` writes the text GHC parses
  (`FILE.hspp`) and its configuration (`FILE.hspp.edn`); the native reader parses that text with its
  line markers as GHC does. A native preprocessor is a later step, as are rejected files' messages.
- **Wired:** `bin/repr-haskell read --reader native` (dumps, then the converter's forms),
  `native-check [LIST]` (default `test/haskell/native-check.txt`), `tables [--check]`; the Makefile's
  `repr-haskell-native-check`, `repr-haskell-tables-check`.
- **Results:** `native-check` 54 of 54 equal (the crop's 17 samples and 37 boot-library modules of 22
  packages, 12 preprocessed), trees, pragmas and forms, 7.6 s natively, 21 s with the helper's runs.
  While building: 119 more modules without preprocessing, 88 preprocessed, the 25 `.hs-boot` files and
  three modules of rare syntax, equal (the converter rejects the three, the helper's dumps too:
  arrows' `HsProc`, `{-# ANN module #-}`, a quasi-quote in a type: the converter's gaps, not the
  reader's). The declarations' port was also run over the corpus's existing dumps, 1,316 of 1,345
  modules, trees equal: wider than the light check intended for this step.

## 2026-11-05: the front end's trial HF1-HF2 (branch `haskell-hf12`, from `haskell-frontend-survey`)

Done by an agent in one session (09:36 to about 11:00 UTC), light on the host (no gate, no build of stage 0, the host's
GHC 9.14.1 and main's stage 0 `/root/arbace/target/seed/amd64/arbace`), after the user decided HFS1-HFS9 as
recommended (doc/haskell/FRONTEND-SURVEY.md, "Decided"). Source: `ghc-9.14.1-src.tar.xz` (SHA-256 `2a83779c...101e77`,
the corpus's pin), read in place: `compiler/GHC/Rename/*.hs`, `compiler/GHC/Hs/*.hs`, `compiler/Language/Haskell/Syntax/*.hs`,
`compiler/GHC/Types/{Fixity,Avail,Name/Reader}.hs`; BSD-3 (the Glasgow license, LICENSE.md's entry extended, doc/ARBACE.md).

- HF1: `tools/hsdump/HsRn.hs`, the helper's verbs `rn` (GhcRn reduced to the resolved names, operator trees,
  binding groups, imports, exports) and `iface` (the libraries' interfaces as forms); `bin/repr-haskell rn-dump`, `iface`.
  Decision: judge sets (each name at each span once), not the tree's multiset, and the groups' order not at all (it
  depends on GHC's uniques); the names GHC gives no location (`SyntaxExprRn`) are in the facts with the span `nil`.
  Alternative considered: dump the whole `HsGroup GhcRn` as forms (a reduction would still be needed to compare;
  the exact tree would force Arbace's renamer to reproduce every extension field). Instances of package modules are
  not dumped (GHC's API refuses), types are strings: left to HF3.
- HF2: `arbace/repr/haskell/rename.ae` (1,397 lines), `bin/repr-haskell rename-check` over `test/haskell/rename-check.txt`:
  101 modules equal (47 samples in `test/haskell/rename/`, 33 modules of ghc-internal, 21 nofib/spectral programs).
  Decision: the renamer writes the facts of HF1 (judged by data equality) and does not yet build a renamed tree; HF3's
  first step is to (survey, "As built: HF1-HF2", Proposed 1).
- Wider sweeps (ghc-internal alone, mtl/parsec/containers..., 36 more nofib programs) found and fixed a dozen cases; see the survey's "As built". Instances and structured types were added to `iface` afterwards.

## 2026-11-05: HF3's start, the renamed tree, the interfaces and the elaborator's first slice (branch `haskell-hf3`, from `haskell-hf12`)

Done by an agent in one session (from 13:26 UTC), light on the host (samples and single-module nofib programs, the
host's GHC 9.14.1 for the helper at `-O0`, main's stage 0 as the tools' host, no gate, no build). The user decided the
four proposals of "As built: HF1-HF2" as recommended; recorded in doc/haskell/FRONTEND-SURVEY.md, "Decided".

- Proposal 1 (a), the renamed tree inside `rename.ae`: the walk records the resolutions, operator shapes, implicit
  names and binding groups keyed by the parse tree's objects (an `IdentityHashMap`), and `rtree` substitutes them after
  the walk. Alternative considered and not taken (the user's decision): a second pass joining the facts to the tree by
  span (spans collide: puns, wildcards). The facts became the tree's projection: `rename-check` also requires the
  tree's located names to be among the facts; 101 of 101 modules equal, none outside, none absent.
- Proposal 2: the helper's `iface` gains roles, arity, synonyms, family equations, fundeps, default methods, bangs,
  arity and strictness of ids, family instances; type variables carry their uniques (GHC's inferred types repeat `t`);
  the new verb `tc` dumps a module's typed things and its desugared Core (`desugarModule`, `-O0`) with types erased. The
  renamer writes a module's own things as far as it knows them; the elaborator writes the typed interface.
- The elaborator, `arbace/repr/haskell/typecheck.ae` (1,531 lines, Arbace's own; GHC's `compiler/GHC/Tc/` of
  `ghc-9.14.1-src.tar.xz` and Jones's "Typing Haskell in Haskell" read as the specification, nothing ported, so no
  LICENSE.md change): HM by levels, groups by the renamer's SCCs, the monomorphism restriction, classes and instances
  as dictionaries with GHC's names, superclasses, defaulting, `HasCallStack` at the call site. Judge decided: types
  (canonical forms) and evidence (applications of names to dictionaries after a little of GHC's simple optimiser), full
  Core reported only (the match compiler is HF5's); the alternative, judging Core now, would need GHC's match compiler
  first. A negative control (defaulting reversed, superclass-implied constraints kept) must differ.
- Results: `bin/repr-haskell typecheck-check` 16 of 16 samples (`test/haskell/typecheck/`), types 194 of 194, evidence 202
  of 202; nofib/spectral's 21 programs by hand, 17 of 21; the four others in two recorded classes (GHC inlines `INLINE`
  library default methods; the order of two constraints of one class). Proposals 1-5 in the survey (the judge, the
  `INLINE` unfoldings, the constraint order, the gate step of decision 3, HF3's next steps).

## 2026-11-05: HF3's start, later the same session: the judge widened (correcting the entry above)

The evidence judge of the entry above recognised only dictionary *parameters*: after normalisation a global dictionary
(`$fShowInt`) was text and escaped the recogniser, so the numbers above were too kind. Fixed, then what the wider judge
found was matched: every default method has a compulsory unfolding GHC inlines into an instance's missing method even at
`-O0` (GHC.Tc.TyCl.Instance, Note [INLINE and default methods]), now dumped by the helper's `iface` verb
(`hsiface/unfoldings`) and inlined by the elaborator (the survey's proposal 2, done because no instance of a base class
passes without it; the alternative, leaving it to HF5, kept as the proposal's option (b)); tidyNPat's literal patterns;
superclass bindings `$cpNCls`; an instance's own dictionary as a given; pattern bindings bound once; `MonadFail` in `do`;
dictionary functions' names; and in the judge, GHC's `simpleOptPgm` inlining of once-used unexported top-level bindings
and `:Main.main`. Results now: 17 of 17 samples (types 194 of 194, evidence 220 of 220), nofib/spectral 17 of 21, 36 more
nofib programs 31 of 36 (with foreign imports, `~` and expression pragmas added), every non-pass in the survey's three recorded classes ("As built: HF3 (start)"). `typecheck.ae`
is about 1,650 lines.
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

## 2026-11-05: HF5-HF6's first slice, the match compiler and Core to STG (branch `haskell-hf56`, from `haskell-hf3`)

- What: the front end's elaborated program (HF3) made STG in hsi's format and run on hsi. `arbace/repr/haskell/desugar.ae`
  (HF5: GHC's match compiler for Haskell 2010's patterns, guards, pattern guards, list comprehensions, `refineDefaultAlt`,
  lambdas collected), `arbace/repr/haskell/tostg.ae` (HF6: CorePrep's A-normal form, saturation, string literals, the
  data constructors' workers, strict fields, top-level floats; CoreToStg's right-hand sides; Stg/FVs's free variables;
  Unarise a no-op on Haskell 2010; the module's `stg/data`, dictionary selectors, newtypes and newtype classes erased,
  `:Main.main`), `arbace/repr/haskell/stgcmp.ae` (the STG judge), `bin/repr-haskell stg-check` (`make
  repr-haskell-stg-check`), `test/haskell/stg/` (nine programs with a `main`). `typecheck.ae` exposes its constructor,
  type and class tables (tags, strict marks, flavours) and its record selectors' case no longer scrutinises an unbound
  variable. Details in doc/haskell/FRONTEND-SURVEY.md, "As built: HF5-HF6 (first slice)".
- Sources: GHC 9.14.1's `compiler/GHC/HsToCore/Match.hs`, `Match/Constructor.hs`, `Match/Literal.hs`, `GuardedRHSs.hs`,
  `ListComp.hs`, `compiler/GHC/Core/Utils.hs` (`refineDefaultAlt`), `compiler/GHC/CoreToStg/Prep.hs`, `CoreToStg.hs`,
  `compiler/GHC/Stg/Unarise.hs`, `Stg/FVs.hs`, `compiler/GHC/Types/Id/Make.hs` (https://downloads.haskell.org/ghc/9.14.1/
  ghc-9.14.1-src.tar.xz, SHA-256 `2a83779c...101e77`, BSD-3 Glasgow license), followed for their algorithms and output's
  shape, not translated line by line (a deviation from HFS5 (a), proposed for the user: the survey's proposal 1); the
  files carry GHC's notice, LICENSE.md and doc/ARBACE.md record them. STG's format: doc/haskell/STG-SPEC.md and
  `go/arbace/hs/load.ae` of `haskell-integrate`.
- `haskell-integrate` (7bda6b88b) merged in for hsi; conflicts in the Makefile, `bin/repr-haskell` (the helper's ghc
  line), this journal and `tools/hsdump/HsDump.hs`, each resolved by keeping both sides.
- Results: the programs of `test/haskell/stg/` compiled from source by Arbace's front end and run on hsi: 9 of 9 with GHC's
  `-O0` output byte for byte, the negative control failing; desugared Core equal to GHC's tc dump in 152 of 220 bindings of
  the typecheck samples (107 before the match compiler); STG equal to GHC's `-O0` dumps in a part of the bindings, the
  classes in the survey (GHC's simple optimiser, AbsBinds' tuples, the failure join points). Light: hsi's light driver
  built in the worktree, the libraries' dumps of `haskell-integrate`'s worktree (`HSI_LIBS`), at most 3 GiB a run.
- Alternatives: a line-by-line port first (HFS5 (a)) was weighed against getting the pipeline end to end in this session;
  the end-to-end slice was chosen so the run judge exists, and the port is proposed as a choice (port now, or close the
  judges' classes function by function).

## 2026-11-05: HF5-HF6's first slice, later the same session (completing the entry above)

- The same branch, after the entry above: lazy patterns (the elaborator keeps `~p`, the match compiler binds each variable
  by a selector), a scrutinised variable's case binder in its alternatives, inlined unfoldings beta-reduced, the library
  classes' dictionary constructors saturated, CorePrep's floats (out of a constructor's arguments, out of a case's
  scrutinee, a CAF's arguments to the top level), shared failure handlers as join points (`letne`, `:j`, `(##)`), the STG
  judge comparing free variables as sets. Final numbers: 9 of 9 programs run on hsi with GHC's stdout, the control failing;
  Core 152 of 220 bindings of the typecheck samples; STG 62 of 83 bindings of the programs equal to GHC's `-O0` dumps;
  `bin/repr-haskell typecheck-check` still 17 of 17 with the elaborator's changes, its control differing.

## 2026-11-05: HF4, deriving (branch `haskell-hf4`, from `haskell-hf3`)

`arbace/repr/haskell/deriv.ae` (about 500 lines) ports the generators of GHC 9.14.1's `compiler/GHC/Tc/Deriv/Generate.hs`
(Eq, Ord, Show, Read, Enum, Bounded, Ix) and `Deriv/Functor.hs` (Functor, Foldable, Traversable, with tuples and nested
type constructors) from `ghc-9.14.1-src.tar.xz` (the corpus's pin, SHA-256 `2a83779c...101e77`), by hand, as renamed-tree
bindings the elaborator types like any instance's methods; `Deriv/Infer.hs`, `Deriv/Utils.hs`, `Deriv.hs` read as the
specification of the contexts (a fixpoint over the module's derived instances, the last occurrence of a duplicate kept as
GHC's solver leaves it) and of the strategies (newtype for a newtype's Eq, Ord, Ix, Bounded as GHC chooses without
extensions, every class but Show and Read with `GeneralizedNewtypeDeriving`, `DeriveAnyClass`, standalone deriving).
`LICENSE.md` and `doc/ARBACE.md` record the origin (BSD-3, the Glasgow license). Primops (`dataToTag#`, `tagToEnum#`,
`Int#` comparisons) became three nodes of the elaborator and `GHC.Classes`' `eqInt`/`ltInt`... since the elaborator has
no unboxed types; the auxiliary `$tag2con_T`/`$maxtag_T` are inlined (alternative: generate them, proposal 1).
The rename tree carries the module's extensions now (`:exts`); `bin/repr-haskell typecheck-check` takes a list
(`test/haskell/typecheck-check.txt`, HF3's decision 5: the nofib sweeps as a check list with their recorded non-passes).
Judge: HF3's evidence judge over the derived bindings (the tc dump's Core of each `$c` binding; `-ddump-deriv` read, not
compared). Results: 22 of 22 samples equal (T18-T22 are new), the list 48 of 57 with 9 known non-passes in HF3's classes.
Alternatives considered: judging `-ddump-deriv`'s text (needs a printer of GHC's pretty layout for the generated trees,
for less than the Core comparison gives); generating source text and renaming it (the derived code names `GHC.Classes`'
functions whatever the module imports, so it would need the renamer's wired-in scope). Proposals 1-4 in the survey.


## 2026-11-06: HF5-HF6's second slice, HF7's first light, HF3's second slice (branch `haskell-hf7`, from `haskell-hf56`)

- What: the user's decisions on HF5-HF6 (2026-11-05: (1) (b) the match compiler and CorePrep kept, GHC's functions ported
  where a judge needs them, amending HFS5; (2) (a) GHC's simple optimiser ported; (3) (a) the run corpus a recorded check
  list after HF4; (4) the gate step once `haskell-integrate` is on main) carried out, and the front end's next stage, HF3's
  extensions in the order the run corpus asked for them. `haskell-hf4` (08a4354e7) merged first (decision 3's "after
  HF4"; conflicts in `typecheck.ae` and the docs, both sides kept). New: `arbace/repr/haskell/simpleopt.ae` (a port of
  GHC's `Core/SimpleOpt.hs` with OccurAnal's information and `Types/Id/Make.hs`'s compulsory unfoldings),
  `constfold.ae` (a port of `Core/Opt/ConstantFold.hs`'s `Float#`/`Double#` rules), `front.ae` (the front end as one
  driver), `bin/repr-haskell run-check` with `test/haskell/run-check.edn` and `test/haskell/run_record.ae`
  (`make repr-haskell-run-check`). Changed: `tostg.ae` (representations from types, primops, unlifted case floats,
  Unarise's unboxed tuples), `typecheck.ae` (MagicHash, UnboxedTuples, Typeable with GHC's fingerprints, existentials,
  fundeps' improvement, `default` declarations, deriving's tags as `dataToTag#`, ...), `desugar.ae`, `rename.ae`
  (reexported modules), `tools/hsdump/HsRn.hs` (the iface closure follows types' modules and orphans). Details in
  doc/haskell/FRONTEND-SURVEY.md, "As built: HF5-HF6's second slice, HF7 (first light), HF3's second slice".
- Sources: GHC 9.14.1's `compiler/GHC/Core/SimpleOpt.hs`, `Core/Opt/OccurAnal.hs`, `Core/Opt/ConstantFold.hs`,
  `Types/Literal.hs`, `Types/Id/Make.hs`, `Core/Opt/Pipeline.hs` (`getCoreToDo`), `CoreToStg/Prep.hs`, `Stg/Unarise.hs`,
  `Tc/Instance/Typeable.hs` with `GHC.Internal.Fingerprint` (https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz,
  SHA-256 `2a83779c...101e77`, BSD-3), read in the extracted tree of the frontend survey's worktree
  (`.tmp/keep/wt/haskell-frontend-survey/src/ghc-9.14.1`); simpleopt.ae and constfold.ae carry GHC's notice, LICENSE.md and
  doc/ARBACE.md record them. References: `test/haskell/ref-ghc914.edn` (GHC's recorded `-O0` runs),
  `test/haskell/check-hsi.edn` (hsi on GHC's STG), the oracle pack's GHC STG of the programs
  (`.tmp/oracle-packs/unpacked/576184f2.../haskell-stg-progs`) for the shapes.
- Finding: `-O0` is not "the desugarer's `simpleOptPgm` and nothing else" (the survey's §2.3): `getCoreToDo` keeps
  `simplify "final"`, one Simplifier run with the built-in rules; the floating-point rules change results (GHC computes
  on Rationals), so they are ported; the rest of that run changes shapes only (the STG judge's classes).
- Results: run-check 60 of 63 (16 before: 12 samples, 4 synthetic), the 3 others in hsi's classes (they fail on GHC's
  own STG under the light cap); stg-check 9 of 9 runs, Core 171 of 227 bindings (158), STG 65 of 83 (62);
  typecheck-check 22 of 22. Light: about 2 minutes, at most 1.6 GB a process. Beyond it, `run-check --nofib`: the 53
  single-module nofib programs of the check list (the pinned tarball's nofib/, references recorded once with the host's
  GHC at -O0 in `test/haskell/ref-nofib.edn`), 40 of 53 with GHC's output, 12 in the class `hsi` (hsi fails on GHC's own
  STG of them under the light caps), 1 `space` (the -O0 Simplifier's inlining of once-used recursive CAFs); the nofib
  programs found five front-end bugs the run corpus had not (derived `==` on tags, `default` declarations, top-level
  pattern bindings' selectors, a quantified superclass, MonadFail's message), each fixed.
- Alternatives: a line-by-line port of CorePrep and CoreToStg (decision 1 (a), declined by the user); porting the whole
  `-O0` Simplifier for the STG judge's equality (a proposal, below the survey's section); running the run corpus with
  GHC's interfaces regenerated per program (the iface closure's fix keeps one dump for the corpus).

## 2026-11-06: the front end's -O0 Simplifier (branch `haskell-simpl`, from `haskell-hf7`)

- What: the user's decisions on HF5-HF6's second slice and HF7 (2026-11-06; recorded first in
  doc/haskell/FRONTEND-SURVEY.md, "Decided (2026-11-06, the user: HF5-HF6's second slice and HF7's proposals)"): (1) (a)
  run-check essential and `--nofib` in `--full` once `haskell-integrate` is on main, prepared here as
  `test/gate/haskell-front-steps.edn` (not in main's steps); (3) (a) the tarball's nofib kept; (4) (a) run-check takes
  `ARBACE_MEM_GIB` and `HSI_TIMEOUT` (`bin/repr-haskell`); (2) the full port of GHC 9.14.1's `-O0` Simplifier, done:
  `arbace/repr/haskell/simplify.ae` (Simplify.hs, Simplify/Iteration.hs, Utils.hs, Env.hs, Inline.hs, the class-op
  rule), `occanal.ae` (OccurAnal.hs), `core.ae` (Core/Utils.hs, Opt/Arity.hs, Unfold.hs, Unfold/Make.hs,
  SimpleOpt.hs's exprIsConApp_maybe), `primops.ae` (generated by `tools/hsprimops/primops.awk` from
  `compiler/GHC/Builtin/primops.txt.pp`), and the judge `simplcmp.ae` (our simplified Core against GHC's after its
  Simplifier, the helper's `stg --core`), in `bin/repr-haskell stg-check`. Behind `REPR_HASKELL_SIMPL=1` first, the
  default once the judges agreed (`REPR_HASKELL_SIMPL=0`: the simple optimiser alone). The Core it reads extended as
  the decision allows: the newtypes' casts with their coercions' kinds, GHC's failure handlers `\void ->`,
  dsAbsBinds' letrec for one unsigned self-recursive binder, the simple optimiser's dead code, DFun unfoldings, record
  selectors with recSelError's DEFAULT; tostg's CorePrep eta expansion to the binders' arity, top-level string
  literals, join points as let-no-escape, a strict constructor's non-atomic arguments case-bound. Details and the
  measures in the survey's "As built: the -O0 Simplifier".
- Sources: GHC 9.14.1 (https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz, SHA-256 `2a83779c...101e77`,
  BSD-3), read in the frontend survey's extraction (`.tmp/keep/wt/haskell-frontend-survey/src/ghc-9.14.1`; the tarball
  was not needed, so not fetched): `compiler/GHC/Core/Opt/Simplify.hs`, `Simplify/{Iteration,Utils,Env,Inline,Monad}.hs`,
  `Core/Opt/{OccurAnal,Arity,Pipeline,ConstantFold}.hs`, `Core/{Unfold,Utils,SimpleOpt,Make,TyCon}.hs`,
  `Core/Unfold/Make.hs`, `Types/Id/Make.hs`, `HsToCore/Binds.hs` (dsAbsBinds), `Driver/{DynFlags,Flags}.hs`,
  `Driver/Config/Core/Opt/Simplify.hs`, `IfaceToCore.hs` (tcIdInfo under -fignore-interface-pragmas),
  `Builtin/primops.txt.pp`, `Builtin/PrimOps.hs`. GHC's notice in each ported file; LICENSE.md and doc/ARBACE.md record
  them. References: the helper's `stg --core` and `core` verbs at `-O0` (GHC's own simplified Core), `ghc -O0
  -ddump-simpl`/`-ddump-ds` to read GHC's choices.
- Findings: `-O0` keeps the built-in rules (the class-op rule resolves the module's own instances' methods; the
  default methods' compulsory unfoldings come from the interfaces even with -fignore-interface-pragmas); imported Ids
  have arity 0 and no unfolding; integrate's `space` class was the desugarer's (rtotals' AbsBinds letrec inlined by
  simpleOptPgm), and GHC's own STG of it needs the same memory (fails at 2 GiB, passes at 4).
- Alternatives: piece by piece (proposal 2 (c), declined by the user); a typed Core throughout (HFS4 (b)): the
  Simplifier's type needs met by binder types from the elaborator and coercions by their kinds instead.

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
