# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

The journal up to the freeze (2026-10-06 to 2026-10-08: the class forms, j2c, the vendored and
self-hosting Clojure, the work for Java 26, the g2c survey) is on the branch
[`arbace-for-java-26`](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
tag `arbace-for-java-26-v1`, and in git history. This journal starts over there, by the user's
decision (2026-10-08).

## 2026-10-08: The freeze; the repository made public

- `bin/freeze --run-gate --yes` ran the gate locally on main `38a652d` and passed:
  - stages 1-3 identical at 5,756 classes, the verifier clean at class file version 70;
  - native tests 47/2,780;
  - Clojure's suite 20,750/20,750 on stages 1 and 2;
  - `bin/class-forms-tests` 64/133;
  - `bin/j2c-check --suite` without regressions.
- It then created and pushed the branch `arbace-for-java-26` at `38a652d` and the annotated tag
  `arbace-for-java-26-v1` (`2fbde83`), whose message records the commit and the gate. The push
  started the branch's CI run (actions run 37749231098). `doc/FREEZE.md` describes the branch.
- At the user's request the GitHub repository `arbace/arbace` was made public, after a scan of
  the tracked files and history for credentials (none). Commit author lines carry the user's
  name and email.
- Main now moves toward the standalone Arbace (`doc/AGENDA.md`). Next on main, as decided:
  `clojure/` is replaced by a binary seed (the jar built from the freeze tag, pinned by hash).

## 2026-10-08: `clojure/` replaced by a binary seed

- The user decided where the seed lives: committed in the repo (alternatives: a GitHub release
  asset, which would tie main's build to GitHub; rebuilding it from the tag at every build).
- The seed (agent, then merged): `seed/arbace-seed.jar`, the jar built at the tag
  `arbace-for-java-26-v1`, SHA-256
  `561b4cc5f5a340b9da5ee07f7d2c193017890e1a5799396dbb901131b1239f4d`, pinned in
  `seed/arbace-seed.jar.sha256`. Rebuilt in a fresh worktree of the tag it gives exactly this
  hash: the jar is reproducible. `bin/seed --check` checks the hash, `bin/seed --verify`
  rebuilds it from the tag (about 1.6 minutes). LICENSE.md says its entries carry the licenses
  of the files they come from.
- The bootstrap: `bin/build-arbace` checks the seed's hash, then compiles stage 1 with
  `java -cp seed/arbace-seed.jar arbace.lang.Main`. The jar alone on the class path makes its
  own compiled namespaces load (its sources and classes carry fixed dates, classes the newer);
  with the checkout first, `RT.load` would compile the current sources instead. Extracting it
  into `target/stage0` with fresh dates was considered and not taken: the jar alone runs exactly
  the bytes the hash covers. Stage 2 = stage 3 stays the hard check; stage 1 = stage 2 is now
  only reported, since stage 1 is built by the freeze's compiler and legitimately differs once
  the compiler's output changes. The seed's compiler must be able to compile the current class
  forms; when it no longer can, the seed moves to a new tag. Stage 1's class forms compile took
  13-15 s, against 21-23 s on `clojure/`.
- The tools (agent, then merged): j2c, the class forms checks, the suite runner's renaming, the
  bench driver and the jdk26u check run on Arbace, through one helper `bin/lib/tools.bash`:
  `target/stage2` (freshly built, so its namespaces are newer than their sources) plus the
  checkout, so the tools' own namespaces and any edited ones load from source; the jar was not
  used because it carries its own sources and would hide edits. Their namespaces were ported from
  `clojure.*` to `arbace.*`; the converter's output is unchanged apart from a header's path
  (checked on the baseline, the samples and java.logging). What needs the frozen Clojure as
  data (j2c's corpus and javac classes, the class forms tests' comparisons, the baseline run of
  Clojure's suite, the bench's baseline) is extracted from the tag with `git archive` into
  `.tmp/frozen/`, once per tag commit.
- Removed from main (all remain on `arbace-for-java-26`): `clojure/`; `bin/vendor-arbace`,
  `bin/vendor-spec` and their parts of `arbace.j2c.rename`; the stage-0 path of
  `arbace.classes.boot` (12 classes fewer per stage: 5,744); and, at the user's choice after a
  search for what the freeze left unneeded, `bin/freeze`, `.github/workflows/gate.yml`,
  `doc/FREEZE.md` and `doc/MODERN-COMPILER.md`. Kept: the benchmarks, since main still runs on
  the JVM. Links to removed docs now point to the branch; CLAUDE.md, LICENSE.md, README.md,
  doc/ARBACE.md, SPEC §9.6 and the notes follow.
- The agent's check with `clojure/` deleted: stages 1-3 identical (5,744 classes), verified;
  native tests 47/2,780; the suite 20,750/20,750 on stages 1 and 2; `bin/class-forms-tests`
  64/133; `bin/j2c-check --suite` all shape-identical, no regressions; `bin/arbace-bench
  --quick` runs.
- The main session's gate on the integrated main, one check after the other: `bin/seed --check`
  as pinned; `bin/build-arbace --suite` stages 1-3 identical (5,744 classes), verified, native
  tests 47/2,780, the suite 20,750/20,750 on stages 1 and 2; `bin/class-forms-tests` 64/133;
  `bin/j2c-check --suite` no regressions. The new jar differs from the seed, as expected (boot's
  stage-0 path is gone).

## 2026-10-08: A faster gate; the non-native fallbacks of the class forms' names removed

- At the user's request ("more concurrency, or a reduced gate with the essentials, the full one
  optional"), both: `bin/gate` runs `bin/seed --check` and `bin/build-arbace` (the stages, the
  verifier, the native tests, the jar, the cache), then concurrently Clojure's suite on stage 2
  and `bin/class-forms-tests`; `bin/gate --full` adds, concurrently, the suite on stage 1 and
  `bin/j2c-check --suite`. Each suite run uses 24 test JVMs (`GATE_TEST_JOBS`). Logs in
  `.tmp/gate/`. The suite on stage 1 is not essential: the build requires stage 2 = stage 3
  and reports stage 1 = stage 2, and when they are equal it tests the same classes twice.
  `bin/build-arbace --suite` stays, sequential, for a build with the suite.
- Concurrent runs share two things, now locked with `flock`: the test libraries
  (`.tmp/clojure-tests/lib`, `lib-arbace`) in `bin/clojure-tests`, and the extraction of the
  frozen tree (`.tmp/frozen`) in `bin/lib/tools.bash`. Each suite run has its own directory.
- Measured: `bin/gate --full` 6m25s at `3ef663a` on a quiet machine (build 1m28s, then suite on
  stage 2 3m24s, class forms tests 7 s, suite on stage 1 3m27s, j2c-check 4m57s), against about
  13 minutes for the same checks one after the other; `bin/gate` 3m40s with the change below,
  under load average 95 from three g2c agents (build 1m28s, suite on stage 2 2m12s).
- `arbace/core_classes.clj`: with stage 0 the seed, nothing runs the class forms on a compiler
  without their special forms, so `class-forms-native?` and the non-native branches of
  `defclass`, `defclasses` and `defop` (whose plain-Clojure operator bodies were the frozen
  runtime's fallback) were removed; the operators are unchanged (inlined `arbace.lang.Numbers`
  calls). Stages identical at 5,740 classes; `bin/gate` passed.

## 2026-10-08: g2c G1, the Go front end helper

- The user started g2c's G0-G2 (front end and round trip), with up to 4 agents. Three ran in
  parallel: the Go forms spec, the helper, the round-trip comparator.
- The helper (agent, `b19f2fe`, merged): `tools/godump/` (Go, standard library only, EPL as
  Arbace's own code), seeded from the experiment `.tmp/g2c/godump` (survey §2.4). Built with
  TamaGo's go1.27.1 (`/root/tamago-go`, overridable with `G2C_GOROOT`); it requires go1.27.1
  and rejects newer language versions (the user's pin). One `go list -e -json -export -deps`
  call gives the files and the dependencies' export data; packages are type-checked by
  go/types with gc sizes for the target GOARCH, in parallel. Output per package, for the
  Clojure reader: a header with the configuration (GOOS, GOARCH, GOAMD64/GOARM64, tags,
  GOEXPERIMENT, toolchain, word size), the files' trees as `(:kebab-type {...})` in go/ast
  field order with the go/types annotations as metadata (`:mode`, `:t`, `:val` as exact
  numbers, `:def`/`:use`, `:inst`, `:sel` with embedding paths, `:implicit`), each file's
  comments as a separate positioned list, and the package view (`:directives`,
  `:init-order`, `:types` with method sets and gc layouts). Details: `doc/go/HELPER-NOTES.md`.
- `bin/g2c` (`build`, `dump`, `read`, `corpus`) and `arbace.g2c.read`, running on Arbace
  (`bin/lib/tools.bash`, which now treats `arbace/g2c/` as a tool, like `classes/` and `j2c/`).
- Coverage: std for tamago/amd64, 379 packages, 1,742 files, and tamago/arm64, 378, 0 type
  errors; `$GOROOT/test`, 1,705 of 1,745 single-file programs (35 excluded by build
  constraints, 5 need another GOEXPERIMENT). Dumps are byte-identical across runs. All 2,462
  read on Arbace; runtime, the largest (27 MB, 233,403 nodes), in 2.0-2.7 s.
- LICENSE.md and doc/ARBACE.md now name `arbace/g2c/` among Arbace's own tools.
