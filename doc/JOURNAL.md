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

## 2026-10-20: the go-lisp, ghc-lisp and rust-lisp checkouts removed; their patches kept

- **Why:** Arbace's surveys and specs cite the three forks as prior art for repr/go, repr/haskell
  and repr/rust (G2C-SURVEY, REPR-HASKELL-SURVEY, REPR-RUST-SURVEY, doc/go/SPEC.md,
  doc/haskell/SPEC.md, doc/rust/SPEC.md). No script, build or check of Arbace reads them.
- **Checked first:** each local checkout equalled its GitHub origin (every branch, a clean tree, no
  tags or stashes), checked with `git ls-remote` without writing to them.
- **The patches** are kept in a new private repository, github.com/arbace/forks (`a21d047`), by the
  user's choice. Each fork's own commits are there as `git format-patch BASE..HEAD`:
  - go-lisp: 27 patches on golang/go `f043823a`, head `2483a496`;
  - ghc-lisp: 25 on GHC's `c35096f5`, head `db7adf93`;
  - rust-lisp: 11 on rust-lang/rust `602727f2`, head `f28e0de8`.
  It has no merges or binary files, about 1.4 MB.
  - Each series applied to its base (with `git apply --cached` in a throwaway bare clone) gives
    the head's tree exactly.
  - Each base is a commit of its upstream (GitHub's and GHC's GitLab's APIs).
  - A fresh clone of arbace/forks equals the local files byte for byte.
  The repository's README says how to restore a fork and under which upstream license each
  series is.
  - Alternatives considered: a release asset on GitHub (rejected: it lives only on GitHub's
    servers, outside any clone) and the patches inside Arbace (rejected: three more upstream
    licenses for files Arbace does not use).
- **Removed:** `/root/go-lisp`, `/root/ghc-lisp` and `/root/rust-lisp` (about 3.6 GB), at the
  user's word. The forks themselves stay on GitHub (go-lisp and ghc-lisp public, rust-lisp
  private). The specs' citations keep their commits; their local paths now name what was there.

## 2026-10-20: the forks deleted from GitHub; their record is arbace/forks

- The user deleted github.com/arbace/go-lisp, arbace/ghc-lisp and arbace/rust-lisp. They had no
  watchers, forks or stars. In the user's words, these projects were pathfinders for Arbace's
  forms: Go, Haskell and Rust written as s-expressions and compiled beside their ordinary source,
  the prior art of repr/go, repr/haskell and repr/rust.
- Their contributions are kept in the private repository github.com/arbace/forks (`a21d047`; the
  entry above): each fork's patch series on its upstream base, checked to give the fork head's
  tree exactly. Its README says how to restore a fork.
- The citations in Arbace's surveys and specs (doc/G2C-SURVEY.md, doc/REPR-HASKELL-SURVEY.md,
  doc/REPR-RUST-SURVEY.md, doc/go/SPEC.md, doc/go/ROUNDTRIP.md, doc/haskell/SPEC.md,
  doc/rust/SPEC.md) name the forks by repository, branch and commit. Those commits are now
  reachable only by applying arbace/forks' patches to the upstream bases: golang/go `f043823a`,
  GHC `c35096f5`, rust-lang/rust `602727f2`. The citations are left as written, a dated record.
