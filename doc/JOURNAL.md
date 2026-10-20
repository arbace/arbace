# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-interprets-go1.27.1-v1`, by the user's
decision (2026-10-20). It started over the same way at the two freezes and after the milestone
`arbace-inside-itself-v1`. The history before it is on the frozen branches, at two tags, and in
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

## 2026-10-20: the journal starts over

- **The user's decision**, after the milestone `arbace-interprets-go1.27.1-v1` (a tag on main, not
  a freeze, so no branch): this journal starts over.
  - The earlier one, 2,204 lines and 95 entries from 2026-10-15 to the tag's entry, is at the tag
    (linked above).
  - CLAUDE.md's Records paragraph, the agenda's header and README's documentation list now name the
    four places of the history.
- **The user cleared the goal** of 2026-10-19 ("reach arbace-interprets-go-v1, that is the next
  milestone"): the milestone is reached and tagged.
- **Still running at the reset:** goi's check in the emulated arm64 box over all of
  `test/goi/box.txt` (`GOI_BOX_ALL=1`, 584 programs), in a detached worktree at `ab8a41dc`.
  - It was started while the Mac run was awaited; the tag's claim counted the half, 291 of 292.
  - Its result is recorded here when it ends.
