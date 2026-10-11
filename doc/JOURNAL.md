# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over at the second freeze, by the user's decision (2026-10-10), as it did at
the first. The history before it is on the frozen branches and in git history:

- 2026-10-06 to 2026-10-08 (the class forms, j2c, the vendored and self-hosting Clojure, the work
  for Java 26, the g2c survey): on
  [`arbace-for-java-26`](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
  tag `arbace-for-java-26-v1`;
- 2026-10-08 to 2026-10-11 (milestone B1a: g2c, c2g, jrt, the evaluator, the Go executable and
  its feature completion, the freeze of the Go build): on
  [`arbace-for-golang`](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/JOURNAL.md),
  tag `arbace-for-go1.27.1-v1`.

## 2026-10-11: the arbace-for-golang freeze

- On the user's confirmation: the branch `arbace-for-golang` and the annotated tag
  `arbace-for-go1.27.1-v1` on main's `e358a78`, pushed. The tag's message names the commit, the
  full gate's result (19m34s on `e09139d`; documents and `bin/gate`'s default test JVMs since),
  the toolchains (JDK 26.0.2.1 built from jdk26u `baf63fb`, TamaGo's go1.27.1) and the release
  executables (PGO; amd64 66,371,744 bytes, arm64 64,225,440 bytes; not published). The branch's
  `doc/go/FREEZE.md` describes it.
- After the tag, on main (the user's decisions): this journal starts over; `doc/go/FREEZE.md` is
  removed from main and linked on the branch, as the JVM's FREEZE.md; the README's "Arbace for
  Golang" links it there; CLAUDE.md's direction names both frozen branches and what comes next,
  and its Records paragraph both journal restarts. The Go build stays on main and is built and
  checked as before (`bin/gate`, `bin/gate --full`).
- Next (the agenda): the `.ae` rename; c2a and the tools renamed j2a, g2a, a2g; Arbace's own
  version; then B1b, the box (doc/go/BOX-SURVEY.md).

## 2026-10-11: after the freeze: the .ae rename, c2a, Arbace's version

- The user's decisions: do the agenda's items 1-3 (the `.ae` rename; c2a and the tools renamed
  j2a, g2a, a2g; Arbace's own version); no `bin/gate --full` until the next milestone (targeted
  checks and the essential `bin/gate` instead, as before the freeze).
- The `.ae` rename first, alone (branch `ae`, an agent): it touches the loaders, the bootstrap
  (the seed reads `.clj` only), every tool, the oracle's case ids and the documents. c2a with
  the tool renames and the version follow on the renamed tree, in parallel.
