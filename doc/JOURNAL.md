# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-reaps-3-crops-v1`, by the user's decision (the
goal of 2026-10-24). It started over the same way at the two freezes and after the milestones
`arbace-inside-itself-v1` and `arbace-interprets-go1.27.1-v1`. The history before it is on the frozen branches, at two tags, and in
git history:

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

## 2026-10-27: the journal starts over

- **The user's decision**, the goal of 2026-10-24 ("reach arbace-reaps-3-crops-v1 milestone, then reset the
  journal"): after the milestone `arbace-reaps-3-crops-v1` (a tag on main, not a freeze, so no branch) this
  journal starts over.
  - The earlier one, 2201 lines and 101 entries from 2026-10-20 to the tag's entry, is at the tag (linked
    above).
  - CLAUDE.md's Records paragraph, the agenda's header and README's documentation list name the five places
    of the history.
- **Where things stand** (doc/AGENDA.md): the REAP matrix at the tag, 12 of 12 cells checked, none failing,
  every non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v1's divergences"); the seed
  `b4ac198d`; parked (the user, 2026-10-26): the box, repr/haskell, repr/rust, the macOS UI, the iOS app. Next,
  in the user's order: the scripts' sweep of `/dev/null` and process substitution; the milestone
  `arbace-reaps-3-crops-v2` (the efficient gate, REAP's small and medium leftovers); then C23 at parity with
  the three crops (prior art: go-whim's C front end). Subagents on Sonnet for as much as possible, Opus for the
  hard parts (the user, 2026-10-25/26). The user asked to settle after this entry, for a restart of the
  harness.

## 2026-10-27: the scripts' sweep started; then `arbace-reaps-3-crops-v2` (the user)

- The user: do the scripts' sweep, then start `arbace-reaps-3-crops-v2`. The sweep (decided 2026-10-24):
  `/dev/null`, `/dev/stdin`, `/dev/stdout`, `/dev/fd`, `/dev/zero` and process substitution out of the
  committed scripts, one commit per script, each script's check rerun. Scope: the scripts of the three crops
  and the build (what the gate runs), 29 files, about 90 uses, in three batches on Sonnet in parallel
  (`sweep-build`: arbace-go, seed and bin/lib's build helpers, alpine-env, x509roots-vendor; `sweep-java`:
  java-check, javac-vendor, jrt-convert, jrt, j2g-edge, repr-java and its lib; `sweep-go`: repr-go's lib,
  generated-check, diffcheck, clojure-tests, repr-clojure's lib). Left as they are, with the parked
  sub-projects (the user, 2026-10-26), to be swept when each is revisited: the box's scripts (`box-*`,
  `tamago-vendor`, `box-vendor`, `net-check`, `bin/lib/{store,tls}-checks.bash`), `repr-haskell` and
  `repr-rust` with their lib files (their checks need KVM or the host's GHC and rustc); `tools/rsdump/rsdump.rs`
  is Rust source.
- Corrected (the user, the same day): the parked sub-projects' scripts are swept too, a fourth batch
  (`sweep-parked`, on Sonnet): the box's 11 files and repr/haskell's and repr/rust's 5, checked where the
  host can run them (KVM tests, the box's smoke test and suite, `bin/tamago-vendor verify`, `bin/repr-haskell
  test`, `bin/repr-rust test`), the rest by syntax (the box's rebuild, the arm64 VM, the Mac bundle).
  `tools/rsdump/rsdump.rs`'s `<(` is Rust's syntax, not a process substitution.
