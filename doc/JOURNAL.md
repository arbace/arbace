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

## 2026-10-08: g2c G0 draft and G2's round-trip oracle

- The Go forms spec (agent, `724edaa`, merged as a draft for the user's review):
  `doc/go/SPEC.md`, about 1,950 lines, modelled on the class forms spec. Round trip first (the
  forms keep everything gc's output depends on), readable Clojure (Go names verbatim, the
  agreed heads, nested `let`, Go's operators in Go context), go/types' decisions as metadata
  (`:val`, `:inst`, `:go/via`, `:init-order`, `:tag` on untyped-constant shifts). Reader
  findings changed the survey's sketch: `~` is unquote (so `(tilde T)`), `.5` reads as a
  symbol, `1e400` as `##Inf`. 20 open questions with recommendations, published for review as
  an artifact (https://claude.ai/artifact/DuQmxZxEHAXV8S5R3GAYME).
- The round-trip oracle (agent, `c529817`, `7ad506e`, merged): `tools/gocmp` (Go, standard
  library only) compares an original package or test program with a candidate at three
  levels: tree (go/ast by reflection, go-lisp's method from `lisp_roundtrip_test.go` @
  `2483a496`, with documented normalisations: parentheses, literal spelling, import order,
  declaration and field grouping, empty statements, elided literal types), export data (the
  candidate compiled in the original's place through `go list -export -overlay`, the export
  data decoded generically and compared as graphs), and object code (every symbol's `-S`
  listing, positions removed, both sides compiled from a canonical token layout since gc's
  scheduling, inline marks and stack slots depend on positions). `bin/g2c-check` runs it over
  std (373 packages for tamago/amd64, 372 for arm64) and 1,705 `$GOROOT/test` programs, with
  results for the Clojure reader in `.tmp/g2c-check/`. Details: `doc/go/ROUNDTRIP.md`.
- Proof without forms: identity, a go/printer reprint and a "canon" respelling of every
  normalisation pass all three levels on both architectures; seeded mutations (operand swaps,
  changed constants, dropped statements; about 4,750 per seed) are all caught by the tree
  level. A full run takes about 10 s with a warm build cache, 56 s from an empty one.
- Known gaps for the printer: line numbers are never compared (they would match only with
  `//line` directives); parentheses around `&&`/`||` operands change gc's dead-branch removal
  (11 packages), and expanding field groups in generic declarations changes dictionaries, so
  the printer must avoid both.
- Incident: the agent's bisection script ran `go tool compile -o /dev/null`; on a compile
  error gc removed `/dev/null` and another process recreated it as a regular file. The agent
  restored the character device (`mknod c 1 3`, mode 666) within minutes, about 10:19 UTC;
  checked afterwards. Lesson: never give a compiler `/dev/null` as an output path.

## 2026-10-08: The Go forms spec accepted

- The user answered the 20 open questions of `doc/go/SPEC.md` §15, asked one by one, and
  accepted every recommendation: positions `:lines` by default and `:full` for the gate; doc
  comments and directives as data, other comments dropped by reading; no `^:mutable`; nested
  `let`; `(set! place op v)`, `inc!`/`dec!`, `aset`; init statements as a leading vector; `for`
  for Go's three-clause loop; implicit return of the last expression; `goto` and
  `fallthrough` verbatim; package variables in source order with `:init-order`; constants as
  source expression plus `:val`; embedded paths by name in `:go/via`; composite literal
  elements positional, `[k v]` and `:field v`; test files converted behind an option; form
  names win, with `go/call`, `go/id` and `inst` as escapes and `fn` for function literals;
  the renamings of §15 question 16; only shifts of untyped constants get a `:tag`, more by
  amendment; the helper extended now (§11.3) before the converter; embedded files by path and
  hash; separate trees per architecture. The spec's status is now accepted.

## 2026-10-08: The helper's format 2 (spec §11.3)

- As the user decided (spec q18), the helper was extended before the converter (agent,
  `037c8ad`, merged): `:godump 2`. Types are data: one `:type-table` per dump in dependency
  order (named types, aliases and type parameters are leaves), and every annotation refers to
  it by id; local types are told apart by their `:decl` position. New: `:dot` positions of
  selectors and type assertions; comment groups with doc text; every `go/ast` directive;
  `:warnings` (a `/*line*/` inside an expression); constant values with go/constant's kind
  (`(:float R)`, `(:float M E)`, `(:complex ...)`); `:via` names of embedded fields on
  selections and on struct literal keys; all of `go list`'s file lists and embedded files
  with SHA-256; a `-tests` option dumping in-package tests and external `p_test` packages.
  Details: `doc/go/HELPER-NOTES.md`.
- Corpus: std tamago/amd64 252.7 MB (was 266.7), dump 1.8 s, read on Arbace 5.9 s, 55,001
  types; arm64 alike; `$GOROOT/test` 52.2 MB; std with tests 554 dumps, 451 MB, read 7.4 s.
  0 type errors, identical across job counts.
- The agent proposes seven spec amendments (dots on type assertions; whether gc attaches
  `//go:` lines across blank lines, to verify; the type table as §11.3's shape; values
  narrower than their type; `:go/via` from the helper; separate file lists; the `p_test`
  path). Collected for the user with the printer's.

## 2026-10-08: g2c's printer, Go forms → Go

- Agent, `7e28713` .. `1666677`, merged: `arbace.g2c.print` (reading, headers, imports,
  declarations, the §12.4 package build with `go build -trimpath -overlay`),
  `arbace.g2c.print-code` (forms → tokens), `arbace.g2c.print-emit` (Go's semicolon rule,
  indentation, lines), `arbace.g2c.print-text` (literal spellings, a port of
  `text/tabwriter`); spacing ported from go/printer (go1.27.1 `nodes.go`); parentheses only
  where the grammar needs them. Two layouts: `gofmt` and `lines` (forms placed on their
  recorded Go lines, with `//line` corrections where a line cannot be reached); `--line-file`.
  Not yet: mode `:full`. `bin/g2c-print`, `bin/g2c-print-tests`; notes in
  `doc/go/PRINTER-NOTES.md`.
- Tests: hand-written forms for every spec section and the §14 examples (real `unicode/utf8`
  and `sort` excerpts), 5 tests, 241 assertions: both layouts parse; the 17 cases with an
  original Go file equal it at gocmp's tree, export and code levels in both layouts; the
  line-aligned cases have the original's lines (checked by a small Go tool,
  `test/g2c/golines`); `s14_3_sort` prints byte-identical in the lines layout.
- Ten proposed spec amendments (A1-A10 in PRINTER-NOTES: `^:go/grouped` for generic field
  groups; `:go/breaks` indexes; `/*line :N:C*/`; conversion parentheses; `(inst G T)` for
  instances; non-`//go:` directives verbatim; doc text and directives; lines inside metadata;
  labeled `:=`; gofmt not safe on the output), collected for the user with the helper's seven.

## 2026-10-08: g2c's converter, helper dumps → Go forms

- Agent, branch `g2c-converter`: `arbace.g2c.convert` (files, declarations, statements with
  §7.3's `let` scoping, expressions, types from the source), `arbace.g2c.types` (type forms
  from the type table, for `:inst` and `:tag`), `arbace.g2c.lit` (§8's literals and constant
  values, exact), `arbace.g2c.layout` (the forms text: mode `:lines` puts every form on its Go
  line, mode `:full` writes `:go/pos`/`:go/apos`), `arbace.g2c.main` (command line, a check
  that every file read back with Arbace's reader equals the forms in memory, metadata and the
  lines of lists included). `bin/g2c convert`, `bin/g2c roundtrip`, `bin/g2c print`,
  `bin/g2c test` (`bin/g2c-convert-tests`: the spec's §2 and §14 examples and one Go file per
  spec section, 6 tests, 150 assertions). Notes: `doc/go/CONVERTER-NOTES.md`.
- Corpus (64 cores, 32 threads): std amd64 379 packages → 30.7 MB of forms in 8.7 s, arm64
  alike, `$GOROOT/test` 1,705 programs in 2.6 s, std with tests 554 dumps in 10.6 s, mode
  `:full` 86.4 MB; 0 failures, every file reads back exactly, every node of the dumps
  consumed, two runs byte-identical.
- Verified in gc (`cmd/compile/internal/syntax/parser.go`, `clearPragma`/`takePragma`;
  `noder/noder.go`, `pragma`) that `//go:` directives attach to the next declaration across
  blank lines: the converter attaches them so (C1).
- Round trip with the printer (merged from main): std amd64 336/373 equal at the tree level,
  355 at the code level; `$GOROOT/test` 1,660/1,705; arm64 337/372. The remaining
  differences are forms the printer does not print yet (local const groups, labeled `:=`,
  local generic types, `&&` parentheses, ...), and 19 programs the oracle compiles but the
  helper excludes by build constraints.
- Eleven proposed amendments (C1-C11 in CONVERTER-NOTES): directives across blank lines;
  local const groups kept (`^:go/grouped`, `^:go/implicit`; N6 against the oracle and gc);
  `^:go/paren` on `&&`/`||` operands (gc's dead-code removal); directives among the imports;
  tagged `go/id` names; local generic types; `x.go.clj` on file-name collisions (9 in std);
  `(binary-float M E)` for huge constants; `:go/label`; `^:var (values ...)`; the positions
  of spliced nodes in `:full`. Also the spec's text: `1.0E100` is exact by §8.1's rule;
  §14.2's `runeErrorByte0` is an untyped rune.
