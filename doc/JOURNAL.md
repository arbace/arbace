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
