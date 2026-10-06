# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Arbace is a from-scratch reimplementation of Clojure in Clojure. Studying prior art and vendoring
parts of it is always allowed. The work is a derivation done once by hand, so it must stay
reproducible from the record we keep.

Prior art:
- https://github.com/clojure/clojure covers the language.
- https://github.com/openjdk/jdk26u covers the native runtime.
- https://github.com/golang/go is a model for a slimmer runtime and anything else useful from it.
  The Go language itself is out of scope.

## Layout

- `clojure/` is the frozen reference baseline: upstream Clojure and ASM sources as seeded by
  `SEED.bash` (now only in git history, commit `d21dc91`). Never modify it. The rewrite is derived
  from it but lives elsewhere.
- `LICENSE.md` holds the licenses of all vendored code. Extend it when vendoring from a new source.

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it.
- When vendoring code from prior art, record its origin (repo, path, revision) and license, both in
  the journal and next to the vendored code.

## Working conventions

- Put temporary and scratch files in `.tmp/`, which is gitignored.
- Never create or modify anything under `.claude/`.
