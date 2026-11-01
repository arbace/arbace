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
