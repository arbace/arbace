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

## 2026-11-04: repr/ocaml's native reader (branch ocaml-native)

- OCaml text read into OCaml forms by Arbace alone (the survey's O6a half; doc/ocaml/OCAML-SURVEY.md, "As built: the native
  reader"). **Decision: port Menhir's construction and generate the tables from parser.mly**, not a hand-written recursive
  descent: OCaml's parser is the LALR(1) automaton of `parsing/parser.mly` with its conflicts resolved by 28 precedence
  levels and `%prec`, and its locations are Menhir's (`$symbolstartpos`, `%inline` spans); generating the automaton gets
  both by construction and makes the claim checkable. Alternatives: a hand-written parser (each resolution and location
  re-derived by hand, completeness unprovable); Menhir's own tables decoded from `boot/menhir/parser.ml` and used directly
  (the production-to-action mapping only in the generated code; kept as the cross-check); the actions translated by
  machine (needs this reader; OC174 (b)).
- Sources, all from the pinned tarball https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz (SHA-256
  `fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`): `parsing/parser.mly` (read by the generator; its
  611 distinct action texts translated by hand into `arbace/repr/ocaml/actions.ae`), `parsing/lexer.mll` (`lex.ae`, with
  stdlib/lexing.ml's position updates and utils/misc.ml's Utf8_lexeme), `parsing/docstrings.ml` (`docstrings.ae`),
  `parsing/ast_helper.ml` (`ast.ae`), `boot/menhir/menhirLib.ml` (the engine `parser.ae` follows, and the table format
  `menhir.ae` decodes) and `boot/menhir/parser.ml` (Menhir 20250912's tables, the cross-check). Menhir's standard library
  rules (`option`, `ioption`, `list`, `nonempty_list`, `preceded`, `terminated`, `rev`, `flatten`, `append`) written from
  standard.mly's definitions (inline-ness confirmed by the cross-check). LICENSE.md: the OCaml Core System's LGPL-2.1 with
  the linking exception; a new section for MenhirLib's terms.
- Checks (light, stage 0): `bin/repr-ocaml menhir --check`: 2,327 of 2,327 states, 1,024 of 1,024 productions, 238 of 238
  nonterminals paired with Menhir's, every action, goto and default reduction equal. `native-check` (the dump with
  positions against `ocdump --loc`, byte for byte): samples 8 of 8 (and their `:full` layouts), stdlib 148 of 150 (the 2
  templates rejected by both), the compiler's parsing/typing/lambda/utils/driver 248 of 248, 16 syntax-heavy testsuite
  directories 231 of 234 (3 rejected by both), 37 of 40 `#use` files; the rejected files' messages are the later step
  (OC172), their locations agree. `read --reader native` gives the helper path's forms exactly in `:lines` and `:full`.
  About 40 KB/s on stage 0, peak 1.0 GB. Not run: the gate, whole corpora, mutants. Proposals OC170-OC174, none decided.

## 2026-11-05: oi's native front end, the first slice (branch ocaml-typer)

OC4 (b)'s route begun: OCaml 5.5.0's typing/ and lambda/'s Translcore with Matching ported to Arbace, so the native
reader's Parsetree becomes Lambda without the host's ocamlc (doc/ocaml/TYPER.md). Sources: the pinned release tarball
(https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz, SHA-256 fcc6ae66...f015ba): typing/types.ml, btype.ml,
ctype.ml, predef.ml, printtyp.ml (out_type.ml, oprint.ml), typeopt.ml, env.ml, typetexp.ml, typedecl.ml, datarepr.ml,
patterns.ml, parmatch.ml, typecore.ml, typemod.ml's structure items; lambda/lambda.ml's helpers, switch.ml,
printlambda.ml's names, matching.ml, translcore.ml, translprim.ml, translmod.ml's bytecode structure; LGPL-2.1 with the
linking exception, the notices in the files, LICENSE.md and doc/ARBACE.md extended. Built under the day's light rules
on main's stage 0: `arbace/repr/ocaml/typer/` (types, env, parmatch, typecore, lambda, matching, translcore; 3,900
lines) and the driver `arbace.repr.ocaml.typer` (types, lambda, check), `bin/oi typer-check` and `run-native`
(bin/lib/oi/typer.bash), `make oi-typer-check`. The stdlib's interfaces are read by the native reader and translated
by the port (no .cmi). Judged against tools/ocdump: signatures as text, the raw Lambda (before Simplif) equal up to
identifier stamps and static exit numbers renamed in order of appearance, and oi's runs of the native Lambda against
the toplevel's: test/ocaml/typer/'s 36 samples (30 written first, 6 more after the port, passing on their first run)
36 of 36 at each level. A probe over testsuite/tests/basic/'s first 40 files (36 that ocamlc accepts): 15 equal, 20
refused with the slice's reasons (Printf formats, modules, open, GADTs), 1 printer difference (Format's breaking of a
long val line). Alternatives considered: a hand-written checker (rejected: Translcore and Matching need OCaml's
Typedtree as it is, and equality of Lambda needs their port), translating the Typedtree from the oracle (it keeps
ocamlc). Next (TYPER.md §6): Simplif (T7), the module system and the stdlib typed natively (T8). Proposals TY1-TY6,
none decided.

## 2026-11-05: oi's native front end, T7 and T8 (branch ocaml-typer2)

The next steps of TYPER.md in the decided order (TY1-TY6, the user, 2026-11-05), on the branch `ocaml-typer2` from
`ocaml-typer`, under the day's light rules (main's stage 0, samples, the basic/ probe and the stdlib's units one
process at a time). Ported from the same pinned tarball (ocaml-5.5.0.tar.xz, SHA-256 fcc6ae66...f015ba; LGPL-2.1 with
the linking exception, the notices in the files, LICENSE.md and doc/ARBACE.md extended): lambda/simplif.ml (T7),
lambda/tmc.ml, lambda/translmod.ml, typing/typemod.ml, includemod.ml, mtype.ml, subst.ml, env.ml's module components,
stdlib/format.ml's engine and typing/oprint.ml's printers (T8). TY1: the raw Lambda judged against -drawlambda, Simplif's
against -dlambda. TY2: the samples' oracle dumps committed in test/ocaml/typer/oracle/ (516 KB for 48 samples), dumped
afresh and compared when ocdump is present, judging alone otherwise (`OI_TYPER_NO_OCDUMP=1`). TY5 (c): `bin/oi
stdlib-check` (make oi-stdlib-check) types bin/oi stdlib's units natively, each coerced to its interface, against bin/oi
stdlib's Lambda; bin/oi stdlib stays oi's library. Results: test/ocaml/typer/ 48 of 48 (types, raw and simplified Lambda,
oi's runs; 12 new samples for Simplif, modules, functors, signatures, the stdlib's functors, local modules, first-class
and recursive modules, inline records, lazy, [@tail_mod_cons]); the stdlib 48 of 71 units equal (the others wait on
Printf's formats, GADTs, objects, effects, recursive values: T9, T10); the probe 17 of 36 (12 of the 19 others are
formats). The judge's alternative for the stdlib, recording its 71 units' dumps (TY5 (b)), stays rejected. Proposals
TY7-TY10 (TYPER.md §10), none decided.

## 2026-11-05: oi's native front end, T7 and T8 continued (branch ocaml-typer2)

Later the same day, on the same branch: the stdlib's units left after the module system were blocked by Printf's
formats and the GADTs they rest on, so these parts of T9 and T10 were ported from the same tarball (TYPER.md §9,
"Pulled forward from T9 and T10"): GADTs with local equations and explicit polymorphism (typing/typecore.ml's
type_newtype and its pattern mode, typedecl.ml's GADT constructors), type extensions (typedecl.ml's
transl_type_extension, translmod.ml's extension constructors), the format parser of stdlib/camlinternalFormat.ml and
typecore.ml's type_format (arbace/repr/ocaml/typer/fmt.ae), type_argument's elimination of optional arguments, lazy
and array patterns from lambda/matching.ml, immutable arrays, atomics, Bigarray's specialization, the effect
primitives. Results replacing the entry above's: test/ocaml/typer/ 52 of 52 at every level (types, raw and simplified
Lambda, oi's runs); the stdlib 64 of 71 units equal natively (the 7 others: polymorphic variants, objects, GADT-aware
exhaustiveness, recursive values); the probe 31 of 36. The alternative of stopping at the module system was weighed:
T8's exit criterion is the whole stdlib, and T11, decided next, needs corpus runs the day's rules exclude.

## 2026-11-05: oi's native front end, T9's polymorphic variants (branch ocaml-polyvar)

TY9 (b), decided by the user the same day: polymorphic variants before T11. On the branch `ocaml-polyvar` from
`ocaml-typer2`, under the day's light rules (main's stage 0, samples and single units, one process under 1.5 GB). Ported
from the same pinned tarball (ocaml-5.5.0.tar.xz, SHA-256 fcc6ae66...f015ba; LGPL-2.1 with the linking exception, the
notices in the files, LICENSE.md and doc/ARBACE.md extended): typing/types.ml's rows and row fields, btype.ml's row
helpers and hash_variant, ctype.ml's unify_row, unify_row_field, copy_row, build_subtype, enlarge_type and the
occur check through rows, typetexp.ml's Ptyp_variant, typecore.ml's variant expressions and patterns (solve_Ppat_variant,
build_or_pat, finalize_variant, type_coerce), parmatch.ml's full_match, close_variant and pressure_variants,
out_type.ml's and oprint.ml's rows and aliases, lambda/matching.ml's divide_variant and combine_variant, switch.ml's
test_sequence, translcore.ml's Texp_variant; with them `|>` and `@@` typed as applications, `%loc_` primitives,
declarations' constraints, memorized abbreviation expansions (recursive types through abbreviations) and GADT
constructors filtered out of a match's completeness when their result type cannot be the matched one (TYPER.md §11).
Results: test/ocaml/typer/ 58 of 58 at every level (6 new samples, t53-t58, their oracle dumps committed); the stdlib 66
of 71 units equal natively (CamlinternalFormatBasics and Format new); the basic/ probe 32 of 36 (switch_opts new).
Alternatives weighed: a row representation of our own (rejected: Matching and the printer need OCaml's rows as they
are); porting Ctype.subtype and scopes now (deferred: TY12, TY13, no accepted program's output depends on them).
Proposals TY11-TY13 (TYPER.md §12), none decided.

## 2026-11-05: oi's native front end, T9's polymorphic variants continued (branch ocaml-polyvar)

Later the same day, correcting the entry above: Ctype.subtype was ported after all (typing/ctype.ml's subtype_rec and
subtype_row, without objects, functors and universal variables), since the brief named it: `(e : t1 :> t2)` is checked,
a ground coercion that one unification cannot make is made by its constraints. So the proposals are TY11 (sharing in
unification) and TY12 (scopes), TYPER.md §12; the entry above's TY12 (subtype) is withdrawn and its TY13 is TY12. Also
`let p : 'a. t = e`, and two more samples (t59, t60): test/ocaml/typer/ 60 of 60 at every level; the stdlib still 66 of
71, the basic/ probe 32 of 36.

## 2026-11-05: oi's native front end, T9's objects and classes (branch ocaml-objects)

The rest of T9, on the branch `ocaml-objects` from `ocaml-polyvar`, under the day's light rules (main's stage 0,
samples and single units, one process under 1.5 GB). Ported from the same pinned tarball (ocaml-5.5.0.tar.xz,
SHA-256 fcc6ae66...f015ba; LGPL-2.1 with the linking exception, the notices in the files, LICENSE.md and doc/ARBACE.md
extended): typing/typeclass.ml (class_structure, type_object, class_expr, class_type, class_declarations,
class_type_declarations) into the new `arbace/repr/ocaml/typer/typeclass.ae`, with ctype.ml's class signatures,
object fields (unify_fields, filter_method, filter_method_row), instance_class, limited_generalize, subtype_fields and
build_subtype on objects; types.ml's Tobject, Tfield, field kinds, Tpoly and Tunivar; typetexp.ml's Ptyp_object,
Ptyp_class and transl_poly_type; typecore.ml's object expressions (type_send, instance variables, self, overrides,
Pexp_poly); out_type.ml's and oprint.ml's object types and class declarations; lambda/translobj.ml and translclass.ml
into the new `typer/translclass.ae` (shared constants, oo_wrap, the object and class initialisers, builtin methods,
cached class tables, Value_rec_compiler's dummy blocks for static classes); translcore.ml's object expressions and
fuse_method_arity, translprim.ml's %send primitives, simplif.ml's Lifused. Along the way: Ctype.unify2's expansion of an
abbreviation that drops a parameter, unify3's link of an object to the abbreviation it meets, `[@@unboxed]` variants,
an include renaming an exception's constructor, and a structure's let bindings translated before the rest (the order
of Translobj's shared constants). Results (TYPER.md §13): test/ocaml/typer/ 68 of 68 at every level (8 new samples,
t61-t68, their oracle dumps committed); the stdlib 69 of 71 units equal natively (CamlinternalOO, Oo and Dynarray new;
CamlinternalFormat and CamlinternalMod wait for T10); the basic/ probe 34 of 36 (objects and includestruct new); a probe
of 23 run tests with objects 12 of 23 equal in Lambda. Alternatives weighed: OCaml's `Tpoly (t, [])` for every method
type (deferred, TY14: no output found to depend on it); porting Includeclass now (deferred, TY13: only rejected
programs differ). Proposals TY13-TY15 (TYPER.md §14), none decided.

## 2026-11-05: oi's native front end, T9's objects and classes continued (branch ocaml-objects)

Later the same day, correcting the numbers of the entry above: more of typeclass.ml and translclass.ml (transl_class_rebind
for classes that apply or re-export another, abbreviate_class_type, optional class parameters with defaults, class
constraints' fields, self coercions to the class being defined), Typecore.is_nonexpansive for objects and `new`,
Ctype.normalize_type's object names, the class parameters' `constraint 'a = ...` (Ctype.unalias, tree_of_constraints),
Btype.link_type's passing of a variable's name, polytypes' pre-univars. Results (TYPER.md §13): test/ocaml/typer/ 71 of
71 at every level (t69-t71 new); the stdlib still 69 of 71; the basic/ probe 34 of 36; the objects probe, widened to 29
run tests, 18 of 29 equal in Lambda (16 in their signatures). Proposals TY13-TY15 unchanged.

## 2026-11-05: oi's native front end, T10's two stdlib blockers (branch ocaml-t10)

TY15 (a), from `ocaml-objects`. Ported by hand from OCaml 5.5.0's release tarball (SHA-256
fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba): typing/value_rec_check.ml's classify_expression and
lambda/value_rec_compiler.ml into the new `arbace/repr/ocaml/typer/valuerec.ae` (a recursive binding's kind, computed
by type_let; compile_letrec with pre-allocation, backpatching, constants and lifted functions, used by transl_let), and
typing/parmatch.ml's exhaust with build_other and typing/typecore.ml's check_counter_example_pat and partial_pred into
`parmatch.ae` and `typecore.ae` (a match's counter-examples typed with their GADT equations; total when none types).
Results (TYPER.md §15): the stdlib 71 of 71 units equal natively (CamlinternalFormat and CamlinternalMod new);
test/ocaml/typer/ 73 of 73 at every level (t72, t73 new, their oracle dumps committed); the basic/ probe 36 of 36 in
Lambda (recvalues, tailcalls new); the objects probe 19 of 29 (class_2 new). Not ported, by choice: Value_rec_check's
validity check (rejected programs, TY16), check_counter_example_pat's Refine_or and explosion (TY17); the counter-
examples are checked only when the usefulness check finds a match with GADT constructors partial. Alternatives weighed:
replacing the usefulness check by exhaust and the counter-example check for every match, as OCaml does (deferred,
TY17 (b): the same answers, more work per match). Proposals TY16-TY18 (TYPER.md §16), none decided.
