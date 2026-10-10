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

## 2026-10-08: The Go forms spec's 28 amendments accepted

- The user accepted, in four groups, all 28 amendments proposed by the helper (7), the
  printer (A1-A10) and the converter (C1-C11): round-trip markers (`^:go/grouped`,
  `^:go/implicit`, `^:go/paren`, `:type-params` on local generic types); new spellings
  (`:go/label` for labeled declarations, `^:var (values a b)`, `(inst G T)` in `new` and
  method expressions, `(binary-float M E)`); directive and file rules (`//go:` lines attach
  across blank lines, as verified in gc; directives among imports; non-`//go:` directives
  verbatim; `/*line :N:C*/`; `x.go.clj` on name collisions); and the factual fixes and
  clarifications, folded in without separate questions. Two agents: one folds them into
  `doc/go/SPEC.md`, one closes the round trip (printer support, the helper's and the oracle's
  `$GOROOT/test` selections made to agree, a recorded reference).

## 2026-10-08: g2c's round trip closed (milestone G2)

- Agent, branch `g2c-close`. The printer prints the converter's markers as the accepted
  amendments say: local `const ( ... )` groups (`^:go/grouped`, `^:go/implicit`, C2),
  `^:go/paren` operands of `&&`/`||` (C3), `go/directive` among the imports (C4), tagged
  `(go/id "x")` names (C5), local generic types (C6), `:go/label` (C9); `new`'s operand by A5
  (a list with a plain head is a call); and it no longer rejects nested labels, the most
  negative integer literals (`-0x8000000000000000`), an empty `(go/const)` or a first clause
  on the `switch` line. A6 is the spec's: non-`//go:` directives stay free-standing forms. The
  converter writes `-0` (integer or exact float) as `(- 0)`: the literal `-0` reads as `0`.
- The lines layout follows the source more closely: the `}` before `else` on the else's
  line, interpreted strings only (a raw string's newlines would push what follows: the forms
  do not say how the source spelled it), labels and `const (` on the line before their
  statement when free, nothing broken inside a one-line body. Measured by compiling both
  sides from their own layouts (`gocmp -keep-positions`): the code equals the original's in
  371/372 std packages and 1,704/1,707 programs (amd64), from 364 and 1,701.
- One `$GOROOT/test` selection, in one place: `gocmp tests` (the test driver's directories and
  reading of the action line, `-goexperiment` left to other configurations, go/build's
  matching), used by `bin/g2c-check` and by the helper's corpus (`bin/g2c corpus test`,
  `test-arm64`). Before, the oracle took the programs `go list -export F.go` compiles (no
  build constraints on named files: 19 programs the helper excluded, such as `chanlinear.go`)
  and the helper missed `//run` without a space and could not be checked on the 18 `compile`
  programs with body-less functions and the two-file `cmplxdivide.go`. gocmp now compiles a
  program as the test driver does, without `-complete` (`-gcflags=-complete=false` after the
  go command's `-complete`; gc's flag only allows body-less functions, `noder/writer.go`),
  and takes programs of several files (`F.go,G.go`). Considered: excluding those programs
  from both corpora (losing body-less functions from the check), or running `go tool compile`
  directly (another build path than the go command's).
- `bin/g2c roundtrip [amd64|arm64] [--tests] [--record]` is a check: it exits non-zero on a
  regression (an entry and level passing in the reference and not now, or missing) against
  `test/g2c/roundtrip-amd64.edn`, `-arm64.edn`, `-tests.edn` (Clojure-reader format;
  `bin/g2c-check --record`, `--reference`, `--known`). arm64 now includes `$GOROOT/test`.
  Result: std 373/373 (amd64) and 372/372 (arm64) at the tree level, all but `unsafe` (no
  export data or code) at export and code; `$GOROOT/test` 1,707/1,707 and 1,702/1,702 at all
  three levels. About 40 s per architecture with a warm build cache (convert and print 29 s,
  check 11 s). Not added to `bin/gate`.
- The oracle supports packages with their tests (`gocmp -tests`, `bin/g2c-check
  --with-tests`): the `_test.go` files' trees, and the export data and code of every package
  `go list -test -export` compiles, paired by import path. std with tests (379 packages,
  test-only ones included): tree 379/379, export 378 (+ `unsafe`), code 369; the 9 others are
  known differences with their reason (`test/g2c/roundtrip-tests-known.edn`): TamaGo's
  `testdata_tamago_test.go` embeds the package's own Go sources, so the test binary holds the
  candidate's text. 46 s.
- The oracle's own candidates (identity, reprint, canon) pass the new corpus on amd64 (373 +
  1,707 trees). Tests: `bin/g2c-print-tests` 5 tests, 256 assertions (new cases
  `s14_5_amended`, SPEC §14.5, and `s15_roundtrip`); `bin/g2c-convert-tests` 6 tests, 150
  assertions; `go test` in `tools/gocmp`.
- The main session merged the branch and checked it: `bin/gate` passed (3m48s); `bin/g2c
  roundtrip amd64` reproduced the agent's result (std 373 trees, 372 export and code with
  `unsafe` skipped; `$GOROOT/test` 1,707 at all three levels; no regressions against
  `test/g2c/roundtrip-amd64.edn`) in 56 s.

## 2026-10-08: The g2c round trip in `bin/gate --full`

- At the user's decision, `bin/gate --full` runs `bin/g2c roundtrip amd64` concurrently with
  its other checks; the essential gate is unchanged. First run: passed in 7m06s (the round
  trip 1m49s under the gate's load; std 373 and `$GOROOT/test` 1,707 at all three levels, no
  regressions). CLAUDE.md says to run `--full` when the class forms compiler, j2c or g2c
  change.

## 2026-10-08: A plan for B1

- At the user's request, `doc/go/B1-PLAN.md`: ten steps with gates, from bringing up the box
  (go-whim's monitor and TamaGo board) through the image's own Go as round-tripped forms, a
  measured JDK surface, a class forms → Go forms converter (c2g) over a Java runtime subset in
  Go forms (jrt), an evaluator over `Compiler`'s `Expr` tree, to the REPL in the box on amd64
  and arm64, and speed. Measured for it: `arbace/lang` is 142 class-form files, about 36,000
  lines, using `java.lang.reflect`, `java.util`, `java.io`, concurrency, regex, `java.math`,
  `java.lang.invoke`. Six decisions are the user's (D1-D6), each with a recommendation.

## 2026-10-08: B1 replanned: a native Go executable first; D1-D6 decided

- The user proposed to postpone the box and arm64 and to reach first a standalone static
  `linux/amd64` Go executable of Arbace, testable as an ordinary user process here, without
  TamaGo. Agreed: it removes the monitor, the image and the second architecture from the
  critical path, and lets every step be tested against the JVM Arbace as plain processes.
  `doc/go/B1-PLAN.md` was rewritten: B1a (the executable, steps 0-7: g2c for `linux/amd64`,
  the measured Java surface, the c2g spec, jrt, the c2g converter, the evaluator, the
  executable, speed) and B1b (the box, later; jrt's OS use kept behind one host interface so
  the move stays small). This changes, for now, g2c's configuration of 2026-10-07
  (`GOOS=tamago`) to `linux/amd64` for B1a; `tamago` returns with B1b.
- The user's decisions: D1 c2g + jrt (not a hand-written runtime); D2 an evaluator over
  `Compiler`'s `Expr` tree first, AOT later; D4 Java's UTF-16 string semantics; D5 a port of
  `java.util.regex` (against the recommendation of Go's RE2 first: exact Java semantics); D6 a
  first REPL without `defclass`/the class forms, `gen-class`, `proxy`, JVM interop beyond
  jrt's classes. D3 (go-whim) moves to B1b.

## 2026-10-08: B1a ends with a freeze, and includes linux/arm64

- The user: the static Linux executable may be the next milestone frozen on a branch, as the
  Java 26 state was this morning; and since it is a Go program, cross-compiling to arm64 is
  trivial and belongs in the milestone. The plan now says so: step 0 covers `linux/amd64` and
  `linux/arm64`, step 6 builds both, and a step 8 makes the freeze (branch and tag, name to
  choose, on the user's confirmation). The arm64 executables are tested here under QEMU's
  user-mode emulation (`/usr/bin/qemu-aarch64`, installed).
- Step 0 started with two agents: the round trip for linux, and building Go forms into static
  executables.

## 2026-10-08: B1a step 0: Go forms → static executables

- Agent, `493567c`, merged: `bin/g2c build [--arch amd64|arm64] [-o OUT] DIR` (logic in
  `arbace.g2c.build`, on Arbace): a program held as Go forms (`DIR/go/` in SPEC §4.1's
  layout, optional `DIR/program.edn` with `:module` and `:go`) printed into a temporary
  module and built with `CGO_ENABLED=0 GOOS=linux go build -trimpath -buildvcs=false`, the
  environment pinned (`GOTOOLCHAIN=local`, `GOFLAGS=`, `GOWORK=off`, `GOPROXY=off`,
  `GOAMD64=v1`/`GOARM64=v8.0`). Without arguments `bin/g2c build` still builds the helper.
  Details: `doc/go/BUILD.md`.
- Tests (`bin/g2c-build-tests`, 30 checks): a hello program and a three-package program
  (embedding, goroutines, channels, `select`, stdin, a panic) written by hand as Go forms,
  built for linux/amd64 and linux/arm64: statically linked (no interpreter, no dynamic
  section), byte-identical across directories and fresh caches, every run's stdout, stderr
  and exit code as expected; arm64 run under `qemu-aarch64`. Hello is about 2.35 MB. The main
  session reran the tests (30 passed) and `bin/gate` (passed).
- Toolchain: TamaGo's go1.27.1 by default (`G2C_GOROOT`); Alpine's go1.27.1 passes too, with
  different but equally reproducible executables.
- Four proposed spec amendments (programs and modules; files of hand-written packages; the
  `//line` file name; `:config` overridden by the build target), collected for the user.

## 2026-10-08: B1a step 0 done: the round trip for linux, in the full gate

- Agent, `8f6b3e2`, merged: the configuration is a parameter end to end; the round trip passes
  for linux/amd64 (std 376, `$GOROOT/test` 1,713) and linux/arm64 (374, 1,707) at all three
  levels, `unsafe` skipped at export and code, no known differences; references
  `test/g2c/roundtrip-linux-{amd64,arm64}.edn`; tamago unchanged. The helper, converter and
  printer needed no change; `bin/g2c` and `bin/g2c-check` gained `GOOS/ARCH` and `--goos`,
  and both set `CGO_ENABLED=0` (gcc is on this host, and `go` turns cgo on for linux).
  TamaGo's go1.27.1 builds linux too (its std differs from upstream only in tamago's files and
  the GOOS tables), so one tree and one build cache serve the four configurations. Warm round
  trip about 40 s per configuration, 93-98 s cold.
- An oracle bug found and fixed by the agent: the go command replays a cached compile's `-S`
  listing from the build cache and prints nothing when that entry is missing, so two programs'
  code level passed vacuously; gocmp now recompiles under a fresh cache key when a listing is
  empty, and reports an error if it stays empty.
- `bin/gate --full` runs the tamago and the linux round trips on amd64, one after the other in
  one job. Run concurrently, the second converter was killed by the kernel's out-of-memory
  killer (two 12 GB heaps beside three suite runs and j2c-check on 62 GB), and the round trip
  then reported one missing package instead of failing: `bin/g2c roundtrip` now stops when the
  converter dies by a signal, and `bin/g2c build` builds the helper under a lock into a
  temporary file moved into place, so concurrent runs never overwrite a running helper.
  `bin/gate --full` passed in 6m54s.
- Five more proposed spec amendments (the configurations `tamago` and `linux`, each amd64 and
  arm64, cgo off everywhere; one tree per configuration), collected for the user with the build
  agent's four.

## 2026-10-08: Nine more Go forms amendments accepted; step 1 started

- The user accepted the nine amendments from step 0: the pinned configurations are
  `GOOS=tamago` and `GOOS=linux`, each amd64 and arm64, cgo off everywhere, TamaGo's go1.27.1
  the one toolchain, one form tree per configuration; a hand-written program states its module
  in `program.edn` (not a `go/module` form); a hand-written package's other and embedded
  files live in its forms directory; `//line` names the forms file relative to it; the build
  target overrides a program's `:goos`/`:goarch`. An agent folds them into `doc/go/SPEC.md`.
- B1a step 1 started (agent): the Java surface of `arbace/lang` and the namespaces, measured
  from the compiled bytecode, with a treatment per JDK class (translate from the JDK's source
  with j2c, shim, cut, or rework) and the decisions for the user.

## 2026-10-08: B1a step 1: the Java surface measured

- Agent, `67e56cd`, merged: `bin/java-surface` (`test/g2c/java_surface.clj`, 15 s,
  deterministic) reads the bytecode of all 5,740 classes of `target/stage2` with
  `java.lang.classfile`, resolving members to their declaring JDK class; a `javap` cross-check
  on the runtime agrees exactly (573 member references). Results in
  `doc/go/JAVA-SURFACE.md`, raw data in `doc/go/java-surface.edn`.
- The runtime and the REPL's namespaces use 278 JDK classes, 970 members (the runtime alone
  224 and 682). Proposed treatments: translate 150 classes (j2c from jdk26u's source), shim 57,
  compile directly in c2g 2 (string `+`, lambdas), rework 7 (`java.lang.invoke`,
  `ClassLoader`), cut 62. The translation closure is 422 classes from 184 source files, of
  which j2c converts all 176 it covers to javac's shapes; 8 are generated in the JDK build.
  `java.util.regex` (9,592 lines) converts exactly. Reflection cannot be cut: the analyzer
  resolves every interop form with it. The `invokedynamic` sites become plain evaluation, ASM
  is not needed, class loading becomes source loading plus a name registry. Estimate 13-19
  agent-days in all (jrt 10-15 against the plan's 6-10). Seven decisions for the user.
- The user took the seven decisions, each as recommended: reflection hand-written over member
  tables c2g generates (`Compiler` and `Reflector` translated unchanged); all plain Java of the
  closure translated with j2c, the VM's edge hand-written (so `BigInteger`/`BigDecimal` are
  translated, not built on `math/big` as the plan first said); a hand-written UTF-16 `String`;
  concurrency mixed (hand-written atomics, locks, executors over Go; `ConcurrentHashMap` and
  the queues translated over a small `Unsafe`); thread identity by a goroutine-local slot added
  to the Go runtime held as forms (the goroutine id from `runtime.Stack` as the stopgap); a
  trimmed REPL start; `#inst` over a small `Date` on Go's `time`. Recorded in JAVA-SURFACE.md
  and B1-PLAN.md (jrt now 10-15 days).

## 2026-10-08: A name for B1a's freeze

- The user proposed `arbace-for-go1.27.1` for the branch that will freeze B1a, after the
  pinned Go toolchain, as `arbace-for-java-26` names the JDK. Recorded in B1-PLAN and the
  agenda.
- Revised by the user the same day: the branch `arbace-for-golang`, the tag
  `arbace-for-go1.27.1`. The branch names the platform and can take later fixes; the tag names
  the exact toolchain.

## 2026-10-08: B1a step 2: the c2g spec, draft for review

- Agent, `3022054`, merged as a draft: `doc/go/C2G-SPEC.md` (about 2,300 lines), class forms →
  Go forms. c2g reuses the class forms analyzer unchanged and translates its typed nodes;
  Go-build differences go in variant files. Two Go packages (`arbace/jrt`, `arbace/lang`;
  Java's packages import each other in cycles), module `arbace`. Classes as structs embedding
  their superclass with an 8-byte header (identity hash, thin lock); interfaces and classes
  with subclasses as Go interfaces with marker methods; leaf classes as plain pointers (326 of
  352 in `arbace.lang`); `Object` as `any`; names mangled with the erased descriptor; lazy
  per-class initialization; exceptions as panics with `try` bodies as function literals;
  monitors in the header; volatile fields as `sync/atomic` types; reflection over generated
  member tables; evaluator-made objects as one `Dyn` type; thread identity by a goroutine-
  local slot patched into the Go runtime. Arithmetic rules found by a prototype: Go rejects
  constant conversions that overflow, Go's evaluation order differs, and gc fuses
  multiply-add on arm64 (an explicit conversion prevents it).
- Checked with a prototype (`.tmp/c2g-proto/`, scratch): translated `Murmur3`, `Reduced`,
  `Util`'s class init and a class hierarchy build for linux/amd64 and arm64 and give the JVM's
  hashes. Measured (ns): interface call 3.5 (megamorphic 10.2), direct call 1.9, entering a
  `try` 9.0, throw and catch 305-795, the runtime slot 3.7 (the `runtime.Stack` stopgap about
  3,000), a thin lock 5.0. 23 open questions with recommendations, for the user's review.
- The user accepted all 23 recommendations of the c2g spec's §16, asked in groups: interface
  values for references, `Object` as `any`, leaf classes as pointers; two Go packages, names
  mangled with descriptors, `F_` fields; lazy class init, header monitors, exceptions as
  panics; torn two-word fields accepted under races with `-race` testing, reference CAS by
  striped locks; one `Dyn` type, partial member tables, variant files; the evaluator counts
  stack depth, fused multiply-add prevented; `jrt.Concat`, one adapter per functional
  interface; jrt's forms under `go/arbace/jrt` with an overlay option in `bin/g2c build`,
  module `arbace`; Java stack traces, receiver null checks, conservative temporaries. The
  spec's status is accepted.

## 2026-10-08: The JVM oracle for differential tests

- Agent, `521bd0a`, merged: `test/oracle/` and `bin/oracle` (`record`, `check IMPL`,
  `harvest`); docs in `doc/go/ORACLE.md`. 15,854 cases, about 4 MB, Clojure-reader format:
  6,473 hand-written forms and 3,179 harvested from Clojure's test suite (printed value and
  class, output, or the exception chain), 4,969 steps of operation scripts over the runtime's
  core classes (as data), and 1,233 regex patterns with 11,682 inputs (Java's exact results
  for matches, finds with all groups, split, replace, named groups, syntax errors). An
  implementation plugs in as a command reading forms on stdin; the driver prints results in
  ASCII so UTF-16 strings, lone surrogates included, survive. Exclusions (time, locale,
  randomness, concurrency, identity hashes, file loading) in ORACLE.md. LICENSE.md: the
  harvested forms and their results come from Clojure's test suite (EPL-1.0).
- The main session's recheck found 2 mismatches the agent had not: NPE messages turned nil
  under load, the JVM's fast throw of preallocated exceptions once C2 compiles a hot path (the
  native tests met it before). The runner now starts the JVM with
  `-XX:-OmitStackTraceInFastThrow`; two checks then matched 15,854 of 15,854 (about 10 s each).

## 2026-10-08: jrt's JDK sources through j2c

- Agent, `070c969`, merged: `bin/jrt-convert [--twice]` (`test/g2c/jrt_sources.clj`) converts
  the 184 source files of the measured closure (176 from jdk26u's `share/classes`, 8
  generated) with j2c into `.tmp/jrt/` (regenerated, not tracked), compiles them with javac
  under java.base's options, and checks every file with the class forms compiler: 184 of 184
  files, 591 of 591 classes shape-identical to javac's; deterministic; about 85 s with
  `--twice`. Notes in `doc/go/JRT-SOURCES.md`.
- The 8 generated sources (`CharacterData*`, `CaseFolding`, `IndicConjunctBreak`) are made by
  the JDK build's own generators, built and run as its makefiles do; byte-identical to the
  build's `support/gensrc`. They must run on the boot JDK 25: `GenerateCaseFolding` filters
  by the running JDK's `Character`, and on JDK 26 (Unicode 17) it drops 28 entries the build
  (Unicode 16) kept.
- Three bugs fixed on the way, reviewed by the main session: j2c's printer laid out deeply
  nested forms in exponential time (memoized; output unchanged); the class forms compiler
  wrote a folded NaN with the hardware's bits instead of javac's canonical NaN; and the shape
  comparison compared float constants with `=`, so NaN never equalled itself and javac's own
  `Float`/`Double` were silently skipped, which had hidden the second bug.
  `bin/gate --full` passed (7m21s).
- The edge jrt hand-writes, over whole files: 188 classes, 678 members (shims 51 classes: the
  largest `String` 49 members, `Unsafe` 39, `Math` 34, `StringBuilder` 31; rework 9, cut 47);
  5 natives; 70 intrinsic candidates, all with Java bodies. Translating whole files pulls in
  80 plain-Java classes outside the 184 files: a decision for the user.
- The user's decision: c2g translates the methods reached from Arbace (step 1's closure) and
  stubs the rest of each file (throwing `UnsupportedOperationException` if called), so the
  closure stays at 184 files; c2g needs a reachability pass. Not taken: a whole-file closure
  grown to a fixed point.

## 2026-10-08: B1a step 3, phase 1: jrt's core

- Agent, branch `jrt-core` (ten commits), squash-merged because its early commits carried 12 MB
  of JVM test data it later untracked. jrt's hand-written Go forms under `go/arbace/jrt/`
  (package `arbace/jrt`, module `arbace`; about 4,700 lines of forms, 13,600 of Go): the object
  header and monitors (thin lock and identity hash in one word, inflation on contention,
  recursion and `wait`), classes, arrays, class-init guards, the UTF-16 `String`,
  `StringBuilder`, `StringBuffer`, codecs, number formatting, `Math` and `StrictMath` (FdLibm
  ports, equal to StrictMath bit for bit), `Throwable` and the try/catch machinery with Java
  stack traces from a frame table c2g will register, `Enum`, `Record`, `Volatile`; stand-ins for
  32 exception classes, 5 interfaces and the parts of `Character` that `String` needs, until
  c2g translates them; a manifest derived from the forms. `bin/jrt` (`build`, `test`,
  `testdata`, `standins`, `manifest`, `print`); `bin/g2c build --overlay` (BUILD.md B5,
  accepted with C2G-SPEC q19). Notes: `doc/go/JRT-NOTES.md`.
- Tests: 28, on linux/amd64, on arm64 under qemu-aarch64, and with `-race`; about 295,000 cases
  compared with values the JVM computed (strings, number formatting, math, exception messages)
  plus the case mappings of every code point; regenerated test data (12 MB, not tracked). The
  main session reran `bin/jrt test` (both architectures pass).
- Thread identity uses the `runtime.Stack` stopgap until phase 2 (monitor enter+exit 8.9 µs
  for now). Helpful NPE messages are not reproduced. Proposed amendments for the user:
  C2G-SPEC A1-A10, BUILD.md B6 (in JRT-NOTES).

## 2026-10-08: B1a step 3, phase 2b: reflection and the remaining shims

- Agent, `d7ac556`, merged: reflection over member tables (`reflect.clj`: `Class` lookups
  merged as the JDK does, `Method.invoke` with Java's unboxing, widening and messages,
  `InvocationTargetException`, fields, constructors; `reflect_array.clj`), a minimal
  `ClassLoader`, three charsets, a root-data `Locale`, and `Date` (Julian before 1582-10-15,
  Gregorian after, `Date.UTC` as jdk26u normalizes) for `#inst`. The member-table format is
  normative in `doc/go/JRT-NOTES.md`: per class its public members (plus listed declared ones),
  raw JVM modifiers, one-call invokers over plain Go values, set in `init`. Generated tables
  for 38 jrt classes; stand-ins for the wrapper classes until c2g.
- Tests on amd64, arm64 under qemu and `-race`; 1,423 cases against the JVM (reflection,
  charsets, locales, 883 date and `#inst` cases). The main session reran `bin/jrt test`.
- Proposed amendments A11-A19 (value convention for invokers, the table format, no
  `Proxy.newProxyInstance` with `jrt.AdaptFn` instead, API names, the declared-members list,
  plain generic reflection, an en_US default locale with root data, three charsets, the
  stand-in), collected for the user with phase 1's.

## 2026-10-08: B1a step 3, phase 2a: threads, concurrency, the host; step 3 done

- Agent, `5dcf84f`, merged: the goroutine-local slot as a patch of Go's runtime held as forms
  (`overlay/go/runtime/`: TamaGo go1.27.1's `proc.go` and `runtime2.go` converted by g2c with
  two marked lines, plus `arbace_local.go`), built through `go build -overlay` (`bin/jrt
  overlay [--check]`; without it the link fails, so a build cannot miss the patch);
  `LICENSE.md` gains Go's BSD license for the converted runtime files. `Thread` and
  `currentThread` over the slot (3.4 ns; the stopgap was 2,900), `ThreadLocal`, interrupts,
  executors and futures, `CountDownLatch`, atomics, `ReentrantLock` and read-write locks with
  conditions, `LockSupport`, `Unsafe` (JVM array offsets, field offsets by registered Go
  types, striped locks for two-word slots), weak references over `weak.Pointer` (soft
  references held strongly), `System`, `Runtime`, and the host interface `jrt.Host` (`OSHost`
  over `os`/`time`, swappable for B1b). Uncontended monitor enter+exit 14.4 ns (target ~10).
- The main session merged it onto phase 2b: the shared lists (`jrt.clj`, the stand-in and
  manifest generators, JRT-NOTES) conflicted and were resolved as unions, keeping phase 2b's
  manifest logic (a superset); the generated stand-ins and manifest were regenerated. `bin/jrt
  build`, `bin/jrt test` (amd64, arm64 under qemu) and `--race` pass.
- Step 3 (jrt's hand-written part) is done. The translated JDK classes join with c2g (step 4),
  which then deletes the stand-ins. Proposed amendments from the three phases, for the user.
- The user accepted all of jrt's proposals: the deviations (implicit NPEs with a null message,
  helpful messages possible later from c2g; `SoftReference` held strongly; about 14 ns per
  uncontended monitor); the implemented APIs and formats as amendments to C2G-SPEC (phase 1
  A1-A10, phase 2b A11-A19, phase 2a A11-A16: class info and tables in `init`, constructors,
  arrays, the frame table, the lock word, the member-table format and invoker values, threads
  with `RunMain`, the host interface, `RegisterGoType`, the runtime patch under
  `overlay/go/runtime/`); the reduced scope (no `Proxy.newProxyInstance`, `jrt.AdaptFn`
  instead; plain generic reflection, no annotations; en_US with root data; three charsets);
  and BUILD.md B6 (`--print-only`, `--tests`, `--module`). An agent folds them into the specs.

## 2026-10-08: A NaN comparison bug in the class forms compiler, fixed

- Found by c2g's differential check (step 4): on the JVM Arbace every ordered comparison with
  NaN was true (`(< 1.0 ##NaN)`, `(>= ##NaN 1.0)`, ...); Clojure 1.12.6 gives false, as Java
  does; `==` was right. Cause: `arbace.classes.emit/emit-cond` chose `dcmpg`/`dcmpl` (`fcmpg`/
  `fcmpl`) from the comparison it jumps on, which is the negation when the code jumps past the
  true branch; NaN then made the negation false and the code fell into the true branch. Since
  `arbace.lang.Numbers` is class forms, Clojure's `<` and friends inherited it. Fixed: the
  choice follows the comparison as written. Clojure's test suite never checks this, which is
  why the gate passed throughout; the frozen `arbace-for-java-26` has the bug too.
- New regression test `test/native/nan_test.clj` (class bodies, both jump shapes, `and`/`or`,
  floats, and Clojure's comparison functions on doubles, longs, BigInts and ratios). The native
  tests now run on stage 2, the stage the jar ships: stage 1 is compiled by the seed's compiler
  and keeps the old bug until the seed moves (stages 1 and 2 now differ in `Numbers`,
  `Numbers$DoubleOps`, `RT` and ASM's `GeneratorAdapter`, reported, not a failure).
- The oracle's expectations, recorded from the buggy JVM, were re-recorded: 67 cases in 6 files
  changed, every one involving NaN. `bin/gate --full` passed (7m13s).

## 2026-10-09: B1a step 4, phase 1: c2g's core

- Agent, branch `c2g-core` (`36b5ba8` .. `1a23143`), merged: `arbace/c2g/` (11 namespaces,
  about 5,000 lines: names, the scan of jrt's forms, the world analyzed exactly as the class
  forms compiler does, the model, reachability with stubs, code, declarations, generated files
  and tables), `bin/c2g`, `bin/c2g-check` (`test/c2g/check.clj`, fixtures), variants in
  `arbace/lang/go/` (`RT` without loading `arbace.core`, `Compiler`), jrt additions (`vm`,
  `natives`, `forkjoin`, `access`). Every analyzer node kind of C2G-SPEC §14 is translated but
  `:fi-adapter` (stubbed for now) and serialization; the member, frame and class tables are
  generated; 38 of jrt's 64 stand-ins are replaced by translated JDK classes. All of
  `arbace.lang` translates (441 classes reached; 322 unused primitive `IFn` interfaces and a few
  unreached classes left out) and builds for both architectures; about 48 s to translate, 26 s
  to build, executables about 26 MB. Notes: `doc/go/C2G-NOTES.md`; amendments A1-A8 proposed;
  the two C2G-SPEC corrections (§5.8, §7.9.5) made.
- The agent's differential check found the NaN bug (previous entry). The main session reran
  `bin/c2g-check` on main after the fix and the re-recording, both architectures: 4,938 of 5,005 on each
  oracle steps pass (`NumbersCompare` 784 of 784 now), the rest wait for `arbace.core` loaded
  in RT (printer, reader, vars: phase 2), a `Reflector` variant, and
  `String.CASE_INSENSITIVE_ORDER`. `bin/c2g` now stops when `.tmp/jrt/conv` is missing (run
  `bin/jrt-convert` first): on a checkout without it the JDK's types came out as `any` and the
  Go did not build. `bin/gate` passed.

## 2026-10-09: arbace-for-java-26-v2, a fix release

- At the user's decision, the NaN comparison fix (`a6b2893`) and its regression test went to the
  frozen branch as a fix release: `arbace-for-java-26` at `398f01b`, annotated tag
  `arbace-for-java-26-v2` (v1 unchanged). The branch's gate, run locally, passed (stages 1-3
  identical, native tests 49/2,847, Clojure's suite 20,750/20,750 on stages 1 and 2,
  class-forms-tests, j2c-check); the push runs the branch's CI. The branch's FREEZE.md lists
  both releases and its journal records v2. The README names both tags. The seed stays the v1
  jar: stage 1, compiled by its compiler, keeps the old NaN code, and stage 2, compiled by the
  fixed compiler, is what ships.

## 2026-10-09: B1a step 4, phase 2A: Reflector, RT's runtime services

- Agent, `0da8d70`, merged: a `Reflector` variant (no `MethodHandles`; `boxArg` adapts Clojure
  functions through `jrt.AdaptFn`, which waits for c2g's `FromFn` adapters), RT's variant binding
  `*out*`/`*err*`/`*in*` over jrt's host streams (stopgap writer and reader classes until jrt has
  the JDK's stream classes) and looking classes up without a `DynamicClassLoader` (§10.3), small
  fixes in c2g (cut members with their signatures; a constant null stored into a field outside
  the closed world), a fixture for interning across GC, Reflector by name and the stream vars;
  `test/c2g/needs-core.edn` lists the steps that need `arbace.core` loaded, with reasons.
- `bin/c2g-check`: 4,953 of 5,009 steps pass on each architecture, 0 fail, 10 unavailable, 46
  need core (core's printer, namespaced maps, data readers, core's vars): step 5's work.
- Proposed amendments P2A-1..4 (native methods and member classes in variants; programs that
  call by name must root what they may call, since reachability does not follow reflection; a
  field outside the closed world dropped; RT's streams and class lookup).
- Process note: the agent used `git stash` (shared by all worktrees) and a system-wide `pkill`
  pattern; the other agents were told to check their state and to avoid both.

## 2026-10-09: The evaluator's questions decided; c2g's amendments accepted

- Part B of step 4's phase 2 (`9ffd5e5` .. `0dd7675`, merged; conflicts with part A in the
  `RT` and `Reflector` variants and `arbace.c2g.code` resolved: A's variants kept, B's
  `makeClassLoader` added, both "dropped store" conditions combined): `Compiler` translates
  through c2g with ASM erased by package (`c2g/erase`), the evaluator's interface
  (`Compiler$Frame`, `EvalFn`, `Evaluator`, `Dyn`), `Dyn` for run-time classes with the nominal
  interface check, `:fi-adapter` and generated `FromFn` adapters, `bin/c2g-evalproof` (25 forms
  read, analyzed and evaluated in Go, identical on amd64 and arm64), and `doc/go/EVAL-PLAN.md`
  (step 5's design, 8.5-12 days).
- The user accepted EVAL-PLAN's Q1-Q7 as recommended: primitive fns evaluated boxed
  (`primInterface` nil in the Go build); the REPL's world roots every public member of every
  built-in class; one `EvalFn` over `RestFn` now; a per-thread depth counter with
  `StackOverflowError` at about 10,000 frames; run-time interfaces by `defineClass` of an
  interface kind; Clojure fn and line per frame in stack traces; ASM erased by package.
- The user accepted all of c2g's proposed amendments so far (core A1-A8, part A P2A-1..4, part
  B B1-B8); they will be folded into C2G-SPEC together with parts C's and D's.
- Merging B onto A broke the build of c2g's output: A's natives for its `RT` and `Reflector`
  variants are hand-written in jrt, while B made natives calls into the class's own package
  (B6). The main session reconciled them in `arbace.c2g.decls`: a native call is jrt's,
  qualified, when jrt defines the function, else in the class's own package (where c2g writes
  `Compiler$Dyn`'s). The spec fold states the same rule (P1 with E6). After the fix:
  `bin/c2g-evalproof` as expected on both architectures; `bin/c2g-check` 4,957 of 5,013 steps on
  each, 0 fail, 10 unavailable, 46 need core; `bin/gate --full` passed (7m58s).

## 2026-10-09: B1a step 4, phase 2C: the JDK closure complete, jrt's streams and files

- Agent, `5591868` .. `07f6c78`, merged (conflicts with phase 2B in `arbace.c2g.main`'s roots
  and jrt's file list resolved as unions): all 64 of jrt's stand-ins are now translated from
  jdk26u (31 more files in the closure; jrt's own Java for files and the standard streams in
  `overlay/jdk/java.base/`, converted and checked like the JDK's; JDK Go-build variants in
  `overlay/jdk/variants/`); `System.in`/`out`/`err` through the translated `PrintStream`, files
  through the host; `String.CASE_INSENSITIVE_ORDER`; String's regex methods over the translated
  `Pattern`. A class forms compiler bug fixed with a test: `resolve-class-sym` ignored source
  imports of member classes (`CodePointTrie$Fast16`), so c2g's world now analyzes without
  failures. A c2g bug fixed: a private method of a non-leaf class evaluated its receiver
  twice. New oracle script `BigNumbers` (3,974 steps over BigInteger/BigDecimal across their
  algorithm thresholds); `bin/c2g-regex` runs the regex corpus on the translated
  `java.util.regex`.
- Checked by the main session after the merge: `bin/c2g-check` 8,953 of 9,000 steps on each
  architecture, 0 fail, 47 need core; `bin/c2g-regex` 1,204 of 1,233 cases (11,365 of 11,682
  inputs; the rest need JDK resource data: CANON_EQ's normalizer, `\N{name}`);
  `bin/c2g-evalproof` as expected; `bin/jrt test`; `bin/gate --full` passed (7m28s).
- Proposed amendments P2C-1..3 (jrt's own Java and JDK variants under `overlay/jdk`; the jrt
  members c2g writes; all stand-ins translated), for the user.

## 2026-10-09: B1a step 4, phase 2D: quality and the whole program; step 4 done

- Agent, `7f7cfe9`, merged (conflicts with 2B/2C in `arbace.c2g.code`, `main` and `world`
  resolved: the race note goes into phase 2A's `set-field!`, both sides' requires and world
  keys kept): line positions (forms laid out on the class forms' lines, `--line-file` builds;
  Java stack traces and Go tracebacks show the source file and line); pattern switches as one
  `if` chain in a labeled block (guards had made code grow exponentially) and javac's
  `MatchException` wrapping; checks for duplicate Go names and package-private overrides (none);
  race candidates reported and `bin/c2g-race` (only one-word hash-cache races, which Java
  allows); `bin/c2g-perf` (8 workloads: the translated runtime is 4-53x slower than the JVM,
  mostly allocation, boxing and interface assertions, not c2g's code; `GOGC=400` gains 20-40%;
  class-init guards elided where initialization is benign, e.g. `Murmur3.hashLong` 9.4 -> 3.7
  ns); `bin/c2g --program`, the whole program rooted at `arbace.lang.Main#main` (668 classes,
  23.5 MB on amd64, 22.3 on arm64, about 7 ms to start; it stops where `arbace.core` must load,
  step 5's work).
- Checked by the main session after the merge: `bin/c2g-check` 8,955 of 9,002 steps on each
  architecture, 0 fail, 47 need core; `bin/c2g-regex` 1,204 of 1,233 cases; `bin/c2g-evalproof`
  as expected; `bin/jrt test`; `bin/gate --full` passed (7m25s).
- Step 4 (c2g) is done: the runtime and the JDK closure translate and pass the oracle's class
  scripts except what needs `arbace.core` loaded. Proposed amendments D1-D7 and P2C-1..3 for
  the user.
- The user accepted P2C-1..3 and D1-D6 (to be folded into C2G-SPEC) and put D7's performance
  work (GOGC at start, reference arrays as one allocation) into step 7, after correctness.

## 2026-10-09: `bin/gate --full` trims Go's build cache

- The disk reached 80% again: Go's build cache had grown to 47 GB (c2g's check programs, the
  round trips, jrt's builds), and Go itself keeps entries five days. Trimmed by hand to 8.8 GB
  (entries unused for two hours). At the user's request `bin/gate --full` now first deletes
  cache entries unused for a day (Go refreshes an entry's time when it uses it, at most hourly,
  so runs in progress keep theirs) and prints the size before and after.

## 2026-10-09: Arbace's sources move to the `.ae` extension (planned)

- The user proposed an Arbace extension in place of `.clj`, which misleads since the sources
  hold class forms and Go forms that Clojure cannot evaluate: `.æ` and/or `.ae`. Decided: `.ae`
  only. `.æ` (U+00E6, without decomposition, so safe under macOS's NFD) works in Go, the JVM,
  jars and git, but costs typing and scripting everywhere; accepting both would double every
  loader rule and glob.
- Planned as one change after step 5 phase 1 merges (it would collide with the evaluator
  agent's work on loading) and before the `arbace-for-golang` freeze. Constraint: the frozen seed
  only knows `.clj`, so `bin/build-arbace` gives it a `.clj` view of the sources for stage 1
  rather than moving the seed.

## 2026-10-09: B1a step 5, phase 1: Arbace runs as a Go executable

- Agent, branch `eval-core` (`43a59ff` .. `4b368a9`), merged: the evaluator in the Go build's
  `Compiler` variant (each node kind evaluates itself, `evalIn(Frame)`, rather than one central
  `Evaluator.eval`: one interface call per node; frames as slot arrays; one `EvalFn` over
  `RestFn` per fn with a run-time class of the JVM's name, binding variadic arguments lazily;
  boxed primitive fns, a depth counter at 10,000 frames, Clojure frames in stack traces, per
  EVAL-PLAN's decisions; where `Expr.eval` differs from compiled code, the evaluator follows the
  compiled code: no `CompilerException` wrapping, typed conversions, JVM checkcast messages);
  deftype/reify/defrecord/definterface over `Dyn`. `bin/c2g --program` embeds 41 namespace
  sources (825 KB, `//go:embed`; then `ARBACE_PATH`) and translates the whole REPL world (1,376
  classes); namespace variants in `arbace/lang/go/ns/` (proxy expanding to a throw per D6,
  `arbace.instant` over `Date`, the trimmed REPL start). `bin/arbace-go` runs, builds and
  smoke-tests the executable like `bin/arbace`. Notes: `doc/go/EVAL-NOTES.md`; amendments
  V1-V8 proposed.
- Checked by the main session: `bin/arbace-go --build` gives static executables of 37.4 MB
  (amd64) and 35.5 MB (arm64); `-e` and the smoke test pass on both (arm64 under qemu); a
  launch takes about 4.2 s, core's load through the interpreter about 1.9 s of it (step 7's
  work). The oracle's forms corpus on the amd64 executable: 9,364 of 9,652 (97.0%); the agent
  reports all 9,002 class-script steps passing with core loaded on both architectures. The 288
  failures: `arbace.math` (jrt's `Math` lacks the trigonometric functions) 145, the JVM's
  helpful NPE messages (an accepted deviation) 52, `Calendar`/`Timestamp`/`Instant` 37, `proxy`
  (cut by D6; pprint's `cl-format` needs it) 30, the rest small. `bin/gate` passed.
- The user's decisions: `proxy` comes back, over `Dyn` (D6 had left it out; pprint's `cl-format`
  needs it); the oracle accepts a NullPointerException whose JVM message is a helpful one
  (deviation V11) by comparing its class only; phase 1's amendments V1-V8 accepted (to be renamed
  when folded, to avoid C2G-SPEC's V deviations); the `.ae` rename after step 5 phase 2.
- Step 5 phase 2 started with three agents: run-time types (`proxy` over `Dyn`; deftype, reify,
  protocols checked); jrt's surface for the REPL (`Math`'s functions, `Calendar`/`Timestamp`/
  `Instant`, sequenced collections, charsets, the small items); Clojure's test suite on the Go
  build, with the oracle's NPE rule.

## 2026-10-09: B1a step 5, phase 2C: the oracle's NPE rule; Clojure's suite on the Go build

- Agent, `54c2623`, merged: the oracle marks a NullPointerException whose JVM message follows
  HotSpot's helpful-NPE grammar and compares it by class only (deviation V11, the user's
  decision); 54 entries re-marked, nothing else changed; `check jvm` stays 19,828/19,828; on the
  Go build all 52 forms pass by the rule (forms 9,416 of 9,652, the whole oracle 19,562 of
  19,828).
- Clojure's suite on the Go build (`CLOJURE_TESTS_GO=... bin/clojure-tests`, each namespace in
  its own Go process with the renamed suite on `ARBACE_PATH`; reference
  `test/arbace-go-results.edn` with a cause per failing namespace): 46 of 64 namespaces load;
  1,852 of 1,906 assertions pass; 40 namespaces pass as on the JVM; 19 skipped with reasons
  (Java fixtures, gen-class, D6's cuts). Blocked mostly by jrt's surface (`java.util.Random`'s
  init, missing `Unsafe.objectFieldOffset(Field)`, blocks 12,786 assertions; `Collectors`,
  `URI`, `Math.sin`, `IntSupplier`, `CyclicBarrier`, `Semaphore`, `File`) and by `proxy` (1,319);
  forwarded to the two running agents. Evaluator fixes on the way (method values' namespace,
  `def`'s dynamic flag in embedded sources, `case`'s warning, `isBoxedMath` without
  annotations). test.generative needs `Random` and `java.util.jar`. A full suite run takes
  about 4.5 minutes at 16 processes (about 10 GB with `GOGC=400`). A proposal for
  `bin/gate --full` is in EVAL-NOTES. `bin/gate` passed; `bin/oracle check jvm` passed.

## 2026-10-09: B1a step 5, phase 2A: proxy over Dyn; run-time types checked

- Agent, `605b2b3` .. `f2bc564`, merged: `proxy` over `Dyn`, as the user decided (a proxy of
  `Object` is a `Dyn`; for the listed proxyable classes, `Writer`, `Reader`, `PushbackReader`,
  `InputStream`, `OutputStream`, `APersistentMap` and jrt's `ThreadLocal`, c2g writes a
  `DynSub_C` that embeds C, dispatching each method to its slot's fn or to C's), upstream's
  `proxy` macros with only `generate-proxy` replaced, `bean` by reflection; pprint and
  `cl-format` work (pprint loads in about 7.9 s on the evaluator, so the REPL does not require it
  at start). Evaluator fixes: a method defined twice is a `ClassFormatError`, `(set! (.x this) v)`
  in deftype methods, arity counting as `AFn.applyToHelper`. New oracle file
  `test/oracle/forms/types.clj` (425 forms on run-time types: deftype, defrecord, reify,
  protocols, mutable and primitive fields, `Object` overrides, proxy, bean, pprint).
- Results: forms 9,871 of 10,077 on amd64 and arm64, file for file; the whole oracle on Go
  20,017 of 20,253; Clojure's suite on Go 1,874 assertions passing (from 1,852), the test.check
  namespaces now stopping at `Math/exp` (phase 2B's). Executables about 4 MB larger (41.6 MB
  amd64). Main session: `bin/gate` passed; `bin/oracle check jvm` 20,253 of 20,253.
- A determinism issue to fix: `polymorphism.clj:176` names two ambiguous interfaces in an order
  that comes from identity hashes of `Class` objects; classes made while `core_proxy` loads
  shift them. Proposed amendments A1-A4 (EVAL-NOTES).

## 2026-10-09: B1a step 5, phase 2B: jrt's surface for the REPL; step 5 done

- Agent, `b686c4a` .. `8bad4b7` (two forks merged into it: `eval-2b-math`, `eval-2b-time`),
  merged as a fast-forward:
  - `Math` and `StrictMath` complete in jrt (109 public members each; the rest of jdk26u's
    FdLibm ported, `go/arbace/jrt/fdlibm.clj`). jrt's `Math` is `StrictMath`, bit for bit with
    the JVM's `StrictMath` on amd64 and arm64; HotSpot's `Math` on amd64 uses Intel libm stubs
    that differ in the last bit for 1.8-19% of arguments (measured), so tests allow 1 ulp (2 for
    sinh, cosh, tanh).
  - Dates: `Instant`, `java.sql.Timestamp` and `java.sql.Date` (Go name `Sql_Date`) translated
    from jdk26u; `Calendar`, `GregorianCalendar`, `TimeZone`, `ZoneInfo` as jrt's own Java (about
    2,300 lines, fixed-offset zones only; translating the JDK's, about 14,000 lines plus tzdb.dat
    and locale providers, was rejected); jrt's `Date` non-leaf.
  - `java.util.stream` translated, parallel streams on a real `ForkJoinPool` in jrt (goroutine
    workers; a fork without a free worker runs in the forking thread); `Stream.gather` and
    `Gatherers.mapConcurrent` left out (they throw). `URI`, `CyclicBarrier`, `EnumSet`/`EnumMap`,
    `Random` (a variant for its initialization), `RandomGenerator`, sequenced collections,
    `Constable`, UTF-16 charsets, `AtomicInteger`/`AtomicLong` as `Number`s, resources from
    `ARBACE_PATH`, `RT.baseLoader` (so `source` works).
  - Evaluator: primitive locals boxed at each use (NaN identity); `:arglists` and constant
    metadata of the embedded namespaces' top-level defs rebuilt as their compiled classes build
    them (the JVM loads those namespaces AOT-compiled).
  - Outside jrt: the class forms compiler gives an erased supertype method's type variables the
    subclass's bounds (`EnumMap` lacked its `put(Object, Object)` bridge in c2g's world; stages
    unchanged); c2g: casting bridges for narrowed generic returns, HotSpot's array cast message,
    default-method forwarders on jrt's hand-written classes, jrt members naming translated
    classes; j2c/`bin/jrt-convert`: `J2C_PATCH_ALL` (chunks see each other's sources) and
    `known-differences` (four files whose shapes differ from javac's in known ways: captured
    variable order in `Collectors`, `Gatherers`, `MatchOps`; an `InnerClasses` entry in `Nodes`).
  - Not done: `Semaphore` (c2g leaves `AbstractQueuedSynchronizer$ConditionObject` untranslated
    while writing members that name it; pprint's suite namespace does not load), `java.io.File`
    (D6). `transducers` loads but times out (evaluator speed, step 7); skipped with that reason.
  - Proposed amendments B1-B8 (EVAL-NOTES), among them B7, the user's call: the overlay's
    `Calendar`, `GregorianCalendar`, `TimeZone` and `TimeText` transcribe parts of jdk26u (GPL v2
    with the Classpath Exception; LICENSE.md says so), against rewriting them from documented
    behaviour.
- Checked by the main session on the merged main: `bin/jrt-convert`, `bin/gate` (3m47s) and
  `bin/oracle check jvm` (20,253 of 20,253) pass; `bin/arbace-go --build` gives 58.4 MB (amd64)
  and 55.8 MB (arm64) executables (from 41.6 MB: streams and the functional interfaces); the
  smoke test passes; the oracle on the amd64 executable 20,221 of 20,253 (the forms 10,075 of
  10,077; the rest the NaN `equiv` class script and 29 regex cases needing JDK resource data,
  e.g. `java.text.Normalizer`). Agent's figures: Clojure's suite on Go 18,781 of 18,806
  assertions (from 1,874; 61 of 64 namespaces load; `test/arbace-go-results.edn`),
  `bin/c2g-check -- --program` 9,010 of 9,010, `bin/jrt test` on amd64, arm64 and `--race`.
- Step 5 is done. Open before the `.ae` rename: the amendments (A1-A4, B1-B8; V1-V8 to fold),
  the Go checks in `bin/gate --full`, and the `polymorphism.clj:176` determinism issue.

## 2026-10-09: the step 5 decisions; amendments folded

- The user's decisions on step 5's open items, all as recommended: B7, the transcribed jdk26u
  parts of the overlay's `Calendar`, `GregorianCalendar`, `TimeZone` and `TimeText`, are kept as
  recorded in LICENSE.md (not rewritten); all amendments accepted and folded; the Go checks join
  `bin/gate --full` (amd64; the essential gate unchanged); the ambiguous-interface message at
  `test/oracle/forms/polymorphism.clj:176` lists the interfaces sorted by class name. The last
  two went to an agent (branch `gate-go`).
- Agent, `0655138`, merged: the amendments folded, renamed to avoid clashes (V1-V8 → M1-M8,
  A1-A4 → X1-X4, B1-B8 → S1-S8; the scheme and the placements in C2G-SPEC §16) into C2G-SPEC,
  B1-PLAN (D6 partly reversed: `proxy` back, the fork-join pool real), JAVA-SURFACE, EVAL-PLAN,
  ORACLE, JRT-SOURCES, BUILD, classes/SPEC §6 (S8, COMPILER-NOTES amendment 15). Inaccuracies
  fixed on the way: wrong section references in B5 and B8, stale ORACLE.md and C2G-SPEC §4.1
  figures (340 closure files), JRT-NOTES' "Dates" amendments numbered W1-W4 (clashing with
  C2G-SPEC's). Open in ORACLE.md: `test/oracle/harvest.clj` still leaves out the forms that name
  `proxy`.
- LICENSE.md completed: the agent added jrt's FdLibm port (`fdlibm.clj`, part of `math.clj`) and
  `LocalDate.java` as a source of `TimeText`; the main session added `codec.clj`'s UTF-16 coders
  (after `sun.nio.cs.UnicodeDecoder`/`UnicodeEncoder`) and `string.clj`'s `indent`,
  `stripIndent`, `translateEscapes` (after `String`), recorded as ports by B7's rule, to be safe.

## 2026-10-09: the `.ae` rename moves to the end of B1a

- The user's decision: the `.ae` rename, still one self-contained change, is the last step before
  the `arbace-for-golang` freeze, no longer right after step 5. B1-PLAN's steps: 6 the
  executable, 7 speed, 8 the `.ae` rename (new), 9 the freeze (was 8). So steps 6 and 7 work on
  the `.clj` names, and the rename lands once the Go build's shape is settled.

## 2026-10-09: a multimethod's ambiguity message names classes in name order

- The oracle case `test/oracle/forms/polymorphism.clj:176`, `(class-ambig "s")` with methods for
  `Comparable` and `java.io.Serializable`, throws "Multiple methods ... match dispatch value:
  class java.lang.String -> A and B, and neither is preferred", A and B in the order of
  `MultiFn`'s method table, a hash map keyed by the classes, whose hashes are identity hashes:
  the order varied between runs and between the JVM and Go builds (it matched on Go by luck).
- The user's decision: name the two in the order of their class names, in Arbace's source.
  Done in `MultiFn.findAndCacheBestMethod` (`arbace/lang/MultiFn.clj`): when both dispatch
  values are classes they are ordered by `Class.getName`; other dispatch values (keywords and
  the like, which hash by value) keep the table's order, so no other message changes.
  Recorded as hand change 14 in `doc/VENDOR-NOTES.md`. Considered: ordering any two values by
  their printed form (changes the keyword cases' messages for no gain); keeping the method
  table sorted (it needs an order over any dispatch value, and only the message needs one).
- Re-recorded with `bin/oracle record forms/polymorphism`: only case 169 (line 176) changed,
  now "... -> interface java.io.Serializable and interface java.lang.Comparable, ...".
  `bin/oracle check jvm` passes 20,253 of 20,253; the Go build (amd64) gives the same message,
  and its oracle run still mismatches only its 32 known cases. The bootstrap passes (stage 2 =
  stage 3).

## 2026-10-09: the Go checks in `bin/gate --full`

- The user's decision (2026-10-09): the proposal of `doc/go/EVAL-NOTES.md` ("For the gate"),
  amd64 only; the essential `bin/gate` unchanged. `bin/gate --full` gains a chain: `bin/jrt-convert`,
  `ARBACE_GO_ARCHES=amd64 bin/arbace-go --build`, then concurrently `--smoke`, Clojure's suite on
  the Go build (`CLOJURE_TESTS_GO`, `-j 12`, against `test/arbace-go-results.edn`) and the oracle
  on it. Each step logs to `.tmp/gate/NAME.log` and has its summary line (`jrt-convert`,
  `go-build`, `go-smoke`, `suite-go`, `oracle-go`); a step after a failed one reports "not run".
- The oracle gains `bin/oracle check IMPL --expected FILE` (`test/oracle/runner.clj`,
  `check-known`): it passes when the set of mismatching cases (named `file:line` for forms,
  `file:index` for class scripts and regex) equals the reference's, over the files run, and
  names the new mismatches and the newly passing cases otherwise. `--write-expected FILE`
  rewrites the reference, keeping the reasons of cases still recorded (new ones get a
  placeholder reason). Considered: counting mismatches only (a fix hiding a regression would
  pass); comparing whole records (a reference as large as the expected files, rewritten on
  every message change). The reference `test/oracle/known-go-amd64.edn`, read with `read`:
  main's 32 mismatches in five groups with reasons and where they are documented (`deftype
  Foo/2`'s error source; the `StringBuilder` identity hash; the JVM's recorded `dcmpg` bug; 5
  `\N{name}` and 24 `CANON_EQ` regex cases needing the JDK's resource data). The gate runs it
  with `--timeout 900`: the reducers file takes about 285 s of the default 300 on Go.
- Memory: the first two runs with the chain concurrent from the start failed: the OOM killer
  took g2c's converter (12 GB heap) both times, once with jrt-convert's beside it, and the suite
  on stage 1 crashed once; the three suites alone (24 JVMs each) reach 40-59 GB. Also found:
  `suite-go` ran the executable by a relative path, which `bin/clojure-tests` does not resolve
  (exit 127); it is passed absolute. So g2c and the Go chain now start, concurrently, when the
  three suites (stages 1 and 2, j2c's) are done. Considered: starting them when the stage suites
  are done, beside j2c's suite (passed in 22m02s, but peaked at 55 of 62 GB, which the
  morning's 46 GB host would not have held); chaining g2c before jrt-convert (longer, the Go
  suite being the long pole anyway).
- `bin/gate --full` passes in 21m24s (was about 7 minutes): bootstrap 1m31s, suites 3m37s and
  3m36s, j2c 5m12s, then g2c 1m27s, jrt-convert 1m32s, go-build 2m58s, go-smoke 31 s,
  suite-go 10m08s (18,781 of 18,806 assertions, no regressions), oracle-go 5m22s (20,221 of
  20,253, the 32 as recorded). Memory used at most 59 GB in the suites' phase (as without the
  Go checks), at most 26 GB after. Docs: CLAUDE.md, `bin/gate`'s header, `bin/oracle`'s usage,
  ORACLE.md (the option and the format), EVAL-NOTES.md (proposal -> done).

## 2026-10-09: Semaphore in jrt; proxy forms in the oracle; the regex cases analysed

- Agent, `a8352a8` (branch `smalls`), merged:
  - c2g named a class that jrt provides and c2g does not translate by its Java name; jrt's
    `ReentrantLock_ConditionObject` registers as `AbstractQueuedSynchronizer$ConditionObject`, so
    translated code named an undefined Go type. Now such a class takes the Go name jrt registers
    (`arbace/c2g/model.clj`, `go-name`).
  - `Semaphore` hand-written in jrt (`go/arbace/jrt/executor.clj`), like jrt's locks: the full
    public API, Java's messages, no fairness (as `ReentrantLock`); 21 ns an uncontended
    acquire/release. Considered and measured: `AbstractQueuedSynchronizer` translated: it builds
    but needs `Unsafe`'s `putIntOpaque`, `getAndBitwiseAndInt`, `weakCompareAndSetReference`,
    `park`, and its `Node.waiter` is a racy two-word field in Go.
  - `Dyn` (reify, deftype) also implements jrt's hand-written interfaces that have a cast function
    (`Future`, `ExecutorService`, `Executor`, `Lock`, `Condition`), with a nominal check
    (`jrt.dynNominal`): on main `(instance? java.util.concurrent.Future (future 1))` was false.
  - The harvester keeps `java_interop`'s assertions that name proxy functions (new file
    `test/oracle/forms/harvest/java_interop.clj`, 10 cases); the suite's `proxy/` directory only
    defines classes.
  - Results: Clojure's suite on Go 19,251 of 19,280 assertions (62 of 64 namespaces load; pprint
    470 of 474, its 4 errors a proxy of `java.io.BufferedWriter`, a leaf class in c2g).
  - Analysed, not done: the 5 `\N{name}` regex cases (`uniName.dat`, zlib) and the 24 `CANON_EQ`
    ones (`java.text.Normalizer`, ICU data through `java.nio` buffers). Amendments Y1-Y4 proposed
    (EVAL-NOTES, "Phase 2B follow-up").
- Main session: the one new Go mismatch, `java_interop.clj:14` (a proxy serialized through
  `ObjectOutputStream`, cut by D6), added to `test/oracle/known-go-amd64.edn` with its reason.
  `bin/oracle check jvm` (agent) 20,263 of 20,263; Go (main) 20,230 of 20,263, as recorded.
- The user's decisions: do both regex groups, `\N{name}` and `CANON_EQ` (agent, branch
  `regex-res`), and pprint's 4 errors, a proxy of `BufferedWriter` (agent, branch `pprint-bw`).
  `bin/gate` passed on the merge (3m55s).

## 2026-10-09: feature completion of the Go build; the gate until the freeze

- The user's decisions: until the `arbace-for-golang` freeze, `bin/gate --full` is not run (it
  runs at the freeze); changes are checked by short targeted tests, the essential `bin/gate` when
  the bootstrapped sources change. The work focuses on the Go build's features and speed, with
  more agents. Started alongside step 6, step 7a, the regex resources and pprint's proxy:
  class forms in the Go build's REPL (B1-PLAN had a first REPL without them); `java.io.File` and
  the file system, and sockets with the socket REPL (`arbace.core.server`), both reversing parts
  of D6; the suite's last failures (`clearing`, `api`, `transducers`' time) with periodic arm64
  checks.

## 2026-10-09: amendments Y1-Y4 accepted and folded

- The user accepted the small items' amendments Y1-Y4 (EVAL-NOTES.md, "Phase 2B follow-up"),
  folded by the main session: Y1 (c2g names a jrt-provided class by jrt's registered Go name)
  into C2G-SPEC §4.4; Y2 (`Dyn` implements jrt's hand-written interfaces with a cast function,
  with the nominal check; narrows E4) into §5.12; Y3 (`Semaphore` hand-written in jrt, AQS
  measured and not taken) into §8.4 and JAVA-SURFACE.md decision 4; Y4 (the harvest keeps
  `java_interop`'s proxy assertions) into ORACLE.md; all listed in C2G-SPEC §16.

## 2026-10-09: B1a step 6, the executable: an image of prepared namespaces

- Agent, branch `step6` (4 commits on `42552a0`), merged: measured first, start (4.18 s) was
  macroexpansion and analysis, not reading (about 0.1 s), with GC at 60% of CPU. Now
  `bin/arbace-go --build` runs the built executable once with `ARBACE_PREPARE`, requiring every
  embedded namespace (about 32 s), and records per top-level form the analyzed `Expr` tree plus
  the side effects of its analysis (deftype stubs and classes, `gen-interface`'s interfaces,
  proxy classes, the boot `ns` macro's `*ns*`): an image of 3.8 MB, reproducible, the same for
  both architectures, linked into the executables (`go/arbace/cmd/arbace/image.clj`, the type
  table `image_types.go` from `bin/c2g --program`; `Compiler$Image` in the Compiler variant). At
  run time `RT.load` replays an embedded source from the image. A class's hash comes from its
  name (the image needs the same hashes in every run). `GOGC=400` during start unless `GOGC`
  is set (a new `Main` variant). The smoke test adds a REPL session checked against the JVM's
  transcript. Considered (EXEC-NOTES.md): pre-read forms, macroexpanded forms, a heap snapshot
  (as Joker), Go code instead of data, lazy decoding.
- Start to `-e nil` on amd64: 4.18 s → 0.32 s (JVM `bin/arbace` 0.17 s, Joker 1.10.0 under
  0.01 s); arm64 under qemu 44 s → 5.2 s; size 58.4 → 63.5 MB (Joker 29 MB). The rest is mostly
  each `ns`'s `refer` of `arbace.core`'s vars (step 7).
- The class hash by name reordered `polymorphism.clj:176`'s message on the agent's branch; on
  main, MultiFn's sort by class name (hand change 14) makes it moot: the case passes.
- Main session on the merge: Go build, `--smoke`, the Go oracle 20,230 of 20,263 as recorded
  (`--expected`), Clojure's suite on Go 19,251 of 19,280 with no regressions; start 0.35 s.
- The user's decisions: amendments U1-U5 accepted (to fold); the smoke test joins the
  essential `bin/gate` with the executable cached by a hash of its inputs (rebuilt only when
  they change). Step 7b (the evaluator's closure compilation) can start.

## 2026-10-09: a proxy of BufferedWriter; pprint passes on Go

- Agent, branch `pprint-bw` (`38cd59a`, `88aaca9`), merged: c2g's proxy list moves to
  `arbace.c2g.model` and gains `java/io/BufferedWriter`; a translated non-final class on it is
  not a leaf (its `DynSub_C` extends it), while hand-written classes keep jrt's leafness. 13 new
  forms at the end of `test/oracle/forms/types.clj` (proxy-super, buffering, prn, pprint and
  cl-format through a proxied BufferedWriter, a write after close). The suite's pprint namespace
  passes 474 of 474; the suite on Go 19,255 of 19,280. Executable +981 KB. Amendment Z1
  proposed (EVAL-NOTES.md).
- Main session on the merge (over step 6): Go build, `--smoke`, the Go oracle's forms 10,097 of
  10,100 as recorded, Clojure's suite on Go with no regressions.

## 2026-10-09: step 6's follow-up: the smoke test in the essential gate; U1-U5 folded

- Agent, branch `step6` (`2ed50fd`, `e380a5e`, `e9f296b`), merged: `bin/gate` runs a check `go`
  (`bin/arbace-go --gate`, not with `--full`) beside the suite on stage 2 and the class forms
  tests: the smoke test on amd64 of an executable cached in `.tmp/arbace-go-gate`, keyed by a
  SHA-256 of `arbace/`, `go/`, `overlay/`, `bin/lib/`, the Java surface, the seed's hash, the
  build's scripts and the toolchains (TamaGo's VERSION, `java -version`, jdk26u's commit). A miss
  runs `bin/jrt-convert`'s steps sources, generate, convert and the amd64 build with the image.
  Measured: the essential gate 3m38s on a hit, about 7 minutes on a miss (the check itself 5m13s
  to 5m37s). Alternative considered: always building (the user chose the cache). CLAUDE.md's
  gate paragraph describes it.
- Amendments U1-U5 folded: C2G-SPEC §5.8 (a class's hash from its name), §10.3 (the prepared
  namespaces), §10.6 (the main package and jrt's hooks), §13.4 (`GOGC` at start), §16; B1-PLAN
  (step 6 done, the cached check, a D7 note); EVAL-PLAN §2.7; EXEC-NOTES.
- Correction to the step 6 entries: while cleaning up after the pprint merge the main session
  force-removed this agent's worktree while it worked, losing its uncommitted edits; the
  worktree was recreated on the branch and the agent redid them. Worktrees are now removed only
  after their agent has reported and stopped.
- Main session on the merge: scripts only and docs; the agent's two essential gate runs (a miss
  and a hit) passed with no regressions; `--full` not run, by the user's policy until the freeze.

## 2026-10-10: amendment Z1 accepted and folded

- The user accepted Z1 (EVAL-NOTES.md, "Proxies of BufferedWriter"): a translated, non-final
  class on c2g's proxy list is not a leaf, since its `DynSub_C` subclasses it; the list lives in
  `arbace.c2g.model/proxy-supers` and includes `java.io.BufferedWriter`. Folded into C2G-SPEC
  §5.3, §5.12 and §16; it records what main already does since the pprint merge. Alternative
  considered: keeping leafness a condition on the list (a listed class must already be
  non-leaf), which excluded BufferedWriter.

## 2026-10-10: java.io.File and the file system on Go

- Agent, branch `go-file` (`6bb5b0a`, `6172f24`), merged: jdk26u's `File`, `FileSystem`,
  `UnixFileSystem` (`src/java.base/unix/classes`, a new source kind `unix` in `bin/jrt-convert`),
  `FileReader`, `FileWriter`, `DeleteOnExitHook` translated; `overlay/jdk/variants/UnixFileSystem.clj`
  routes its 15 natives to `go/arbace/jrt/filesystem.clj` (canonicalize from jdk26u's C, in
  LICENSE.md) over an optional host interface `HostFS` (`hostfs.clj`, `hostfs_linux.clj`; `Host`
  unchanged). Shutdown hooks run when main returns. A small `java.nio.file` of jrt's own (`Path`,
  `Files`, `HostPath`, path arithmetic from jdk26u's `UnixPath`) instead of jdk26u's provider
  layer (about a hundred more natives for what Arbace and the suite use: temp files, a buffered
  reader). `java.net.URL` translated with the `file:` connection (`slurp` tries `URL.` first);
  `http:`, `https:`, `jar:` parse only. `RandomAccessFile` not done (unused). Amendments FS1-FS7
  in JRT-NOTES.md "Files" (to review). JDK closure 340 → 389 files; executable +2.1 MB.
- Merge conflicts (main session): jrt's file list and load order (union with step 6's
  `image`), the suite reference's header.
- Main session on the merge: `bin/jrt-convert`, the amd64 build, `--smoke`; the Go oracle 20,566
  of 20,600, its 34 mismatches as recorded (a new one: a `localhost` URL's hash, which the JDK
  takes from the resolved address); Clojure's suite on Go 19,379 of 19,398 assertions, 19
  failures, 0 errors (`method-thunks`, `reader`, `sequences` and `pprint` pass), no regressions.
  `java.io` still fails to load on `ServerSocket` (the sockets agent).

## 2026-10-10: amendments FS1-FS7 accepted and folded

- The user accepted FS1-FS7 (JRT-NOTES.md, "Files"). The agent folded them (`ef9e0c6`): FS1
  JRT-SOURCES.md (the `unix` source kind); FS2 stays a decision in JRT-NOTES.md, referred to
  from C2G-SPEC §4.1; FS3 C2G-SPEC §9.4 (an optional `HostFS`); FS4 §4.4's rename table; FS5
  §8.4 (`RunMain` runs the shutdown hooks); FS6 §4.1 (files in the closed world) and §10.3
  (namespace variants); FS7 LICENSE.md, kept. C2G-SPEC §16 has a "Files" entry.

## 2026-10-10: the suite's last failures on Go

- Agent, branch `suite-last` (9 commits to `5072bae`), merged (`6a33be8`). Amendments SL1-SL7 in
  EVAL-NOTES.md, "The suite's last failures".
- SL1 locals clearing in the evaluator, as compiled code does (`arbace/lang/go/Compiler.clj`:
  `closesExprs` as the JVM's `compile` builds it, a slot cleared after a `shouldClear` use,
  closed-overs cleared in a `^:once` fn, statement-position locals skipped, a fn's class declares
  its closed-overs as fields through `Dyn.defineFnField`): `clearing` 12 → 31 of 31; a lazy
  seq's head is no longer retained (5e6 elements: 619 → 121 MB resident).
- SL2 a hinted call of a resolved public method goes through jrt's invoker
  (`Evaluator.invokeResolved`), skipping `Reflector`'s selection and wrapping: transducers' trial
  11.5 → 8.6 ms (JVM 0.11). SL3 the Go reference skips single tests (`:skipped-tests`; only
  `seq-and-transducer`). SL6 `arbace.java.api.Clojure` is in the program (`api` loads). SL7 c2g
  cuts a fixed list of JDK classes the test libraries import (`embed/library-cuts`), so
  test.generative runs on Go (26 specs pass; about 3 more minutes in the suite on Go).
- arm64 under qemu (before the merges): the oracle's forms 10,079 of 10,082, the same 3
  mismatches as amd64; 14 suite namespaces (14,904 assertions) as on amd64.
- Not done, with costs (EVAL-NOTES.md): SL4 the suite's Java fixtures as a test build (about 450
  assertions), SL5 embedding D6's `metadata`/`javadoc` namespaces, `ProcessBuilder`, SAX,
  serialization.
- The user's decisions: SL1, SL2, SL3's skip, SL6, SL7 accepted (to fold); of the proposals,
  only SL3's per-namespace timeouts, for after step 7; SL4, SL5 and `ProcessBuilder` not now.
- Main session on the merge: `bin/gate` (the runner changed) passed in 6m39s, the JVM suite
  20,750 of 20,750, the Go check built afresh (308 s); the Go oracle 20,566 of 20,600 as
  recorded; Clojure's suite on Go 19,506 of 19,506 assertions, 0 failures, 0 errors (65
  namespaces, `java.io` fails to load on `ServerSocket`), no regressions.

## 2026-10-10: the user's decisions on step 7a, 7b and class forms; java.base's assessment

- Class forms at the Go REPL (branch `cf-repl`): CF1-CF6 accepted, merge as is (executable
  63.1 → 74.0 MB; size tuning later, with step 7); CF3 reverses D6 for `defclass`.
- Step 7a: O1 (collector settings, closing D7), O2, O4, O6, O7 accepted; O3 (a shared
  `getParameterTypes` array, a deviation from Java's contract) dropped; O5 accepted: the
  freeze's executables are built with `--pgo`.
- Before the freeze, an assessment of how much of `java.base` jrt implements (the user's
  request): an agent measures it reproducibly (branch `jbase`, `doc/go/JAVA-BASE.md`).

## 2026-10-10: step 7b: the evaluator's closure compilation

- Agent, branch `step7b` (`5754115` to `5561d9e`), fast-forwarded into main over `5ced273`. Each
  method of an evaluated fn, deftype, defrecord or reify is compiled at its first call into a
  tree of `Code` nodes (class forms translated by c2g; `arbace/lang/go/CompilerCode.clj`):
  locals as slot indexes, `long`/`double` locals unboxed in a `prims` array, unboxed paths for
  primitive-typed nodes (as the bytecode back end's `emitUnboxed`), constants made once, 578
  public static methods of `Numbers`, `RT`, `Util` and jrt's `Math` called directly (generated
  by `test/c2g/eval_ops.clj` into `CompilerOps.clj`), `recur` as a frame flag, fixed-arity calls
  without `RestFn`'s seq, frames reused per thread by depth, `str` concatenated directly. The
  image keeps the analyzed Expr trees; methods compile lazily (compiling at replay started
  slower). Node kinds it does not know fall back to their `evalIn`.
- Integrated with suite-last (SL1's locals clearing in the compiled paths: `CodeLocalClear`,
  `CodeClosedClear`, captures through `closesExprs`; SL2's `invokeResolved` replaces the
  agent's own call native).
- Speed (amd64, 19 benchmarks): geometric mean 12.8× faster; Go against the JVM from about 700×
  to about 54× slower. `-e nil` 0.33 → 0.21 s; loading from source 3.5 → 1.8 s; `refer` per
  namespace 18.6 → 5.7 ms; image preparation 32 → 11 s; suite `reducers` 345 → 67 s, `parse`
  306 → 78 s; `transducers`' skipped test passes in 574 s. Tried and dropped (SPEED-NOTES.md):
  child nodes in arrays (an interface assertion per read), one class per call signature, typed
  function values in c2g's tables (step 7a's ground), compiling at replay, a new frame per call.
- Amendments EC1-EC7 in SPEED-NOTES.md, for the user.
- The agent's incidents: a `pkill -f` by pattern killed the sockets agent's queued build (re-run
  since); disk at 86%, the shared Go build cache trimmed to entries used in the last 6 hours.
- Checks (the agent's, on exactly this tree): smoke, `bin/c2g-evalproof` on amd64 and arm64,
  the Go oracle 20,566 of 20,600 as recorded, Clojure's suite on Go 19,506 of 19,506, `clearing`
  31 of 31.
- Merge order changed (main session): the cf-repl merge (`51f5e3d`) broke class forms on Go
  (`arbace.classes.interp` fails to load: a proxy of `arbace.asm.ClassVisitor`; 271 new oracle
  mismatches), found before pushing. Local main was reset to the pushed `5ced273`; the cf-repl and
  jbase merges are kept on the local branch `main-cf-pending` (`8ace8b1`) until the class forms
  agent fixes the cause.

## 2026-10-10: the Go freeze's tag gets a -v1 suffix

- The user's decision: the tag of the B1a freeze is `arbace-for-go1.27.1-v1` (was
  `arbace-for-go1.27.1`), as the JVM freeze's `arbace-for-java-26-v1`/`-v2`: fixes on the same
  Go toolchain are released as `-v2`, `-v3`..., and the name before the suffix still names the
  exact toolchain. The branch stays `arbace-for-golang`. B1-PLAN and the agenda updated.

## 2026-10-10: jlinked images are headless

- The user's decision: every jlinked image (`bin/arbace-image`, on main and on the
  `arbace-for-java-26` line, and the Alpine package built from it) is headless: Arbace targets
  the server side only. Today's image (2026-10-08) holds java.base, java.datatransfer,
  java.desktop, java.logging, java.prefs, java.sql, java.transaction.xa, java.xml,
  jdk.unsupported and jdk.unsupported.desktop (133 MB): `jdeps --print-module-deps` of the jar
  (java.desktop through `arbace.inspector` and `arbace.java.browse`/`browse_ui`) plus
  jdk.unsupported.desktop, added for the AOT cache's class linking on JDK 26.0.2. The desktop
  modules leave the image; the desktop namespaces stay in the jar and work on a full JDK. The
  Alpine package agent (branch `apk26`) makes the change, re-examines the AOT linking workaround
  and measures; main's `bin/arbace-image` (identical to the frozen branch's) takes the same
  change after. The Go build already leaves Swing/AWT out (D6).
- `java.beans` is in the java.desktop module and `arbace.core/bean` (core_proxy.clj) uses its
  `Introspector`, so a headless image would break `bean`. The user's choice (of: port the Go
  build's reflection-based `bean`; keep java.desktop run headless; accept a broken `bean`): port
  the Go build's variant (`arbace/lang/go/ns/core_proxy.clj`: Introspector's decapitalize and
  readable properties by reflection) into the JVM core, a hand change (VENDOR-NOTES). The
  projected headless modules: java.base, java.sql (`#inst`'s `Timestamp` guard and printing,
  `resultset-seq`) with java.logging, java.transaction.xa and java.xml, and jdk.unsupported. The
  Alpine package agent does it on `apk26`; main follows.
- Revised by the user the same day: the default jlinked image holds `java.base` only; every
  other module is optional (used when the runtime provides it). Besides java.desktop (inspector,
  browse, `bean`), the jar reaches java.sql (`core.clj`'s `when-class "java.sql.Timestamp"`
  guards of `#inst`, `instant.clj`'s `Timestamp` printing and reader, `resultset-seq`'s hint),
  java.xml (`arbace.xml`, `arbace.lang.XMLHandler`) and jdk.unsupported
  (`arbace.repl/set-break-handler!`'s `sun.misc.Signal`). `#inst` with `java.util.Date` stays on
  java.base; the `Timestamp` support loads when java.sql is there; `arbace.xml` and the desktop
  namespaces fail cleanly without their modules; the break handler degrades. Hand changes
  (VENDOR-NOTES), done on `apk26` first; `bin/arbace-image`'s default modules: `java.base`, plus
  `ARBACE_IMAGE_MODULES`.

## 2026-10-10: main gains sockets, regex resources, class forms, java.util, step 7a, the monitor fix

- Merged into main after step 7b (in this order): `go-net` (sockets and the socket REPL;
  NT1-NT5), `regex-res` (`\N{name}`, `CANON_EQ`, the JDK's resource data embedded; RD1-RD5),
  `cf-repl` (class forms at the REPL; CF1-CF6), the java.base assessment (`jbase` `b3cfd60`:
  `bin/jrt-coverage`, `doc/go/JAVA-BASE.md`), `suite-last`'s folds and per-namespace timeouts,
  step 7b's folds, `monfix`, `jbase` (java.util completed; JB1-JB5), `step7a` (O1, O2, O4-O7),
  `cf-repl`'s fixes. Conflicts (main session): jrt's file lists, c2g's rename table and
  `main.clj`'s embedding (`:embedded-data` and `generics.edn`), `jrt_sources.clj`'s lists,
  `natives.clj`, and the spec documents' §16 entries (each kept).
- The monitor race (`monfix` `f0db06d`): test.generative deadlocked on Go after step 7b (about
  65 goroutines on jrt's `monMu`, none holding it). `monitorExit` freed a monitor, released its
  mutex and then called `tryDeflate`; meanwhile another thread could take, exit and deflate it
  and the object be locked again, so the late `tryDeflate` reset another thread's lock (mutual
  exclusion lost) and freed the index twice; a header then named a missing monitor,
  `lookupMonitor` dereferenced nil under `monMu`, and `jrt.Catch` made it a Java
  NullPointerException a Java `catch` swallowed, leaving `monMu` locked. Fixed: `tryDeflate`
  deflates only the live monitor; every `monMu` section unlocks by `defer`; a missing monitor
  is a Go bug panic. Two regression tests (a deterministic replay, a contention test).
- Class forms broke twice on the combined tree, both fixed by the cf-repl agent: step 6's
  image recorded a proxy event before `defineProxyClass` succeeded (a proxy the Go build cannot
  make was replayed at load; `0fb2b35`); the evaluator resolves some hints at run time in the
  current `*ns*`, so `arbace.classes.interp`'s imported short names failed from `user` (full
  names now, `0b2b2f4`; the evaluator should resolve such tags at analysis, as the JVM does: an
  open item); step 7a's header flag for objects made at run time was not set on interpreted
  classes' objects (`81a6ffc`).
- Checks on the merged tree (the cf-repl agent's, on `81a6ffc`, which differs from main only
  in the journal, agenda and B1-PLAN): smoke; the Go oracle 21,180 of 21,188, the 8 recorded
  mismatches; Clojure's suite on Go 19,628 of 19,632 assertions, 66 namespaces all loading, 4
  errors (`java.io`'s class loader cases), test.generative 26 of 26, no regressions. Before it,
  the main session's essential `bin/gate` on `48ba45c` passed (8m39s).
- Corrected by the user the same day: the baseline of every Arbace image is `java.base` plus
  `jdk.unsupported` (for `sun.misc.Signal`, the REPL's break handler, which stays as it is);
  java.sql, java.xml and java.desktop are optional. `bin/arbace-image`'s default modules:
  `java.base,jdk.unsupported`, plus `ARBACE_IMAGE_MODULES`.

## 2026-10-10: the evaluator resolves hints in the defining namespace

- Agent, branch `evaltags` (`6e64164`), merged. The Go evaluator asked an expression's type
  (`getJavaClass`, `maybePrimitiveType`, which resolve a hint's short name in the current
  namespace and cache it) at run time in four places the JVM asks at analysis:
  `Evaluator.caseKey` (`case` with keyword or string tests), `CaseExpr.evalIn`,
  `Evaluator.result` (a deftype method returning a primitive), and step 7b's closure compiler
  (a method compiled at its first call). A fn whose hints use imported short names then failed
  when first called from another namespace. Fixed: each fn or type records its defining
  namespace (`ObjExpr.evalNs`), methods compile with `*ns*` bound to it; `CaseExpr` keeps its
  test's primitive type from analysis (`evalPc`); `Evaluator.result` takes `CMethod.bodyPrim`.
  New oracle forms `hint_namespaces.clj` (35 cases; 5 failed before).
- Checks (the agent's, on exactly this tree plus the journal): smoke; the Go oracle 21,215 of
  21,223, the 8 recorded mismatches; Clojure's suite on Go 19,628 of 19,632, no regressions;
  benchmarks unchanged.

## 2026-10-10: java.time on Go

- Agent, branch `go-time` (to `e61da9a`), merged. Translated from jdk26u: `java.time`,
  `.temporal`, `.format`, `.zone` (not the serialization proxies), `.chrono`'s ISO chronology
  only (Hijrah, Japanese, Minguo, Thai Buddhist cut); jdk26u's `TimeZone`, `SimpleTimeZone`,
  `ZoneInfo`, `ZoneInfoFile` and `sun.util.calendar` replace jrt's fixed-offset ones; `Date`
  uses the default zone. `tzdb.dat` generated as the JDK build makes it (byte-identical to the
  image's) and embedded; java.base's CLDR data (root and English) generated by the build's
  `CLDRConverter` (7 files, byte-identical); the locale providers and a minimal `ResourceBundle`
  as jrt's own Java (0 differences in 6,677 lookups against the JDK). The default zone from
  `TZ`, `/etc/localtime` (a new optional host interface `HostLinks`), then GMT, as the JDK on
  Linux. Amendments TM1-TM9 (JRT-NOTES.md, "Time"); TM8 affects all builds (an instance-method
  root is a virtual call; reachability indexed, 58-68 s → 3.6 s).
- Numbers: jrt-convert 580 files (475); java.base coverage 35.9% → 44.1% of members, java.time
  86.5%; new oracle files `time.clj` (273) and `time_zones.clj` (57), 327 of 330 on Go (the 3:
  `Locale.FRANCE`/`UK` from root/`en` data, a TM4 deviation, recorded). Executable +11.4 MB
  (88.2 → 99.6 MB): reflection closures 2.0 MB, reflection tables 0.84 MB, `Dyn` types 1.0 MB,
  CLDR as code 1.0 MB, translated code 0.94 MB.
- The user's decision: TM1-TM9 accepted, and the size is to be trimmed before the freeze.
- Main session on the merge: `bin/jrt-convert`, `bin/jrt testdata`, `bin/jrt test`, the amd64
  build, smoke; the Go oracle 21,542 of 21,553, the 11 recorded mismatches; Clojure's suite on Go
  19,628 of 19,632, no regressions.

## 2026-10-10: java.util.concurrent on Go

- Agent, branch `go-juc` (20 commits to `bd2c2e2`), merged. JC1: c2g compiles `VarHandle`s
  statically (the `findVarHandle`/`MhUtil.findVarHandle`/`arrayElementVarHandle` constants of
  static initializers; each access mode as the atomic operation on the field or element, the
  field made volatile in Go; `arbace/c2g/vh.clj`, jrt's `varhandle.clj`; `java.lang.invoke`
  stays out). With it, translated from jdk26u: the concurrent collections and queues,
  `Exchanger`, `Phaser`, `CompletableFuture`, `ThreadPoolExecutor`,
  `ScheduledThreadPoolExecutor`, `Executors`, `FutureTask`, `ThreadLocalRandom`, the atomic
  arrays, adders and accumulators, `StampedLock`, AQS, `Flow`, `SubmissionPublisher`; jrt's
  hand-written pools and `FutureTask` removed (`ReentrantLock`, `ReentrantReadWriteLock`,
  `Semaphore`, `CountDownLatch` stay hand-written: `LazySeq` and `Delay` lock on every use, and
  the JDK's lock allocates per acquire). Variants make the two-word fields read across threads
  volatile (LinkedTransferQueue's `waiter` was read half-written under test.generative: an NPE in
  `LockSupport.unpark`). JC9: `RunMain` keeps a ticker pending, so a program ends as on the JVM
  (a `send` without `shutdown-agents` keeps running, a `future`-only one exits after 60 s; Go's
  "all goroutines are asleep" crash is gone; three new smoke checks). Amendments JC1-JC10
  (JRT-NOTES.md, "Concurrency"), accepted by the user (to fold).
- Coverage: java.util.concurrent's family 29.0% → 89.2% of members. New oracle forms
  `concurrency.clj` (247, all match); the 3 `nextProbablePrime` cases now pass (removed from the
  known mismatches). Executable +7.4 MB.
- Main session on the merge (over java.time): conflicts in jrt's file list and JRT-NOTES;
  `bin/jrt-convert`, `bin/jrt manifest` (no change), `bin/jrt testdata`, `bin/jrt test`, the
  amd64 build (107.1 MB), smoke; the Go oracle 21,792 of 21,800, the 8 recorded mismatches;
  Clojure's suite on Go 19,628 of 19,632, no regressions.
- The JVM line: `arbace-for-java-26` gained the Alpine package and the base image and was
  tagged `arbace-for-java-26-v3` (`71ffc9f`), then the APKBUILD bumped to the tag (`ec49a67`);
  both pushed (the user's decisions: merge and tag v3, the name `arbace-java26`, Temurin
  26.0.2.1 as the build JDK, publishing later).

## 2026-10-10: the .ae rename after the freeze; rlwrap in the Go executable

- The user's decision: the `.ae` rename is no longer B1a's step 8, the last before the freeze;
  it is the first task right after the `arbace-for-golang` freeze. The freeze becomes step 8.
  B1-PLAN and the agenda updated. The frozen branch then keeps `.clj` sources, as
  `arbace-for-java-26` does.
- The user's decision: the Go executable runs its interactive REPL under `rlwrap` when stdin is
  a terminal (as Alpine's `clj` and the JVM package's `arb`). An agent designs and builds it
  (branch `gorl`): only for the interactive REPL, with a marker against re-wrapping and a way to
  turn it off.
- The user's decision: on main, `bin/arbace` is renamed `bin/arbace-j` (the JVM launcher, beside
  the Go executable) and runs its interactive REPL under `rlwrap` as the Go executable does. One
  convention for both: `ARBACE_RLWRAP=off` disables it, `ARBACE_RLWRAP=wrapped` marks the
  child; wrap only for the interactive REPL with stdin and stdout terminals, `rlwrap` on PATH and
  TERM not `dumb`; `clj`'s flags. Done with the base-image port (branch `baseimage`); historical
  records keep the old name. The `arbace-for-java-26` line keeps `bin/arbace`.

## 2026-10-10: the executable trimmed, 107.1 → 61.9 MB

- Agent, branch `gosize` (to `8716900`, over main `d97e0f9`), merged; the user accepted SZ1-SZ4
  (JRT-NOTES.md, "Size"). SZ1: `-ldflags=-s -w` (no symbol table or DWARF; Go's function table
  stays, so panics, Java stack traces and profiles are unchanged, checked;
  `ARBACE_GO_SYMBOLS=1` keeps them): −23.5 MB. SZ4: `Dyn` and the 18 `DynSub_C` types embed one
  `dynCore` holding the world's interface methods, where each defined about 1,300: −11.3 MB.
  SZ3: each class's reflective members as a data table decoded at first use (`EnsureMembers`,
  jrt/members.clj), replacing about 22,600 closures and 5 MB of start-up code: −9.2 MB. SZ2:
  embedded resources zlib-compressed when a quarter smaller (`P.z`): −1.15 MB. Not done, with
  numbers: compressing the image of prepared namespaces (6.0 → 1.7 MB, +40 ms start), fewer REPL
  roots (changes behaviour), CLDR arrays by a helper, jrt's reflect_tables in the new format.
- Also folded: TM1-TM9 (C2G-SPEC §4.1, §4.4, §9.4, §10.3, §10.6, §12, §16 "Time"; JRT-SOURCES).
- Checks (the agent's, on the merged program; main adds only documents): the Go oracle 21,792
  of 21,800, the 8 recorded mismatches; smoke; Clojure's suite on Go, no regressions; `bin/jrt
  test` on amd64 and arm64; start 0.20-0.21 s unchanged, memory after start 162 → 158 MB.
- Earlier the same day (main session): JC1-JC10 folded and `doc/go/JAVA-BASE.md` measured again
  on `d97e0f9` (`b8584d8`): java.base members 50.1% (8,792 of 17,546), Arbace's own use 89.8%.

## 2026-10-10: narrowing the Go build's gaps before the freeze

- Compared (main session): the JVM build passes Clojure's suite whole (809 tests, 20,750
  assertions); the Go build runs 66 of 85 namespaces, 19,628 of 19,632 assertions (94.6% of the
  JVM's), the oracle 21,792 of 21,800; java.base 50.1% of members (Arbace's own use 89.8%);
  start-up alike (0.17-0.20 s); throughput about 40-55× slower (no AOT to Go yet). The gaps,
  by cost, offered to the user: (1) the suite's Java fixtures as a test build (SL4), (2) class
  loader resources (java.io's 4 errors), (3) small oracle differences and more CLDR locales, (4)
  processes, (5) java.security basics, (6) java.text formats; larger: (7) AOT of namespaces to
  Go, (8) XML, (9) gen-class, (10) serialization.
- The user's choice: 1-6 before the freeze; 7-10 after. Four agents: `gofix` (1-3 without the
  locales), `gotext` (6 with the locales), `goproc` (4, reversing D6 for processes), `gosec`
  (5).

## 2026-10-10: the Go executable's REPL under rlwrap

- Agent, branch `gorl` (`6608250`), merged; the user accepted RL1-RL3 (EXEC-NOTES.md, "Line
  editing: rlwrap"), folded by the main session into C2G-SPEC §10.6 and §16 and B1-PLAN's
  "Checks". `main` first calls `execRlwrap` (before jrt's host and the image; about 11 ms of
  Go's package initialization in the first process): when the arguments start the REPL, fds 0
  and 1 are terminals with a width, `TERM` is set and not `dumb`, the parent is not rlwrap,
  `rlwrap` is on `PATH` and `ARBACE_RLWRAP` is neither `off` nor `wrapped`, it execs `rlwrap`
  with `clj`'s flags around itself (`ARBACE_RLWRAP=wrapped` for the child; history in
  `~/.arbace_history`); otherwise silently on. The smoke test gains 24 checks on a
  pseudo-terminal (`test/arbace-go-rlwrap.py`). Considered: a message when rlwrap is missing, as
  `clj` (not taken: the executable is the only way to its REPL and works without rlwrap).
- Also folded by the main session: SZ1-SZ4 into C2G-SPEC §16 ("Size").
- Main session on the merge (over the size trimming): `bin/jrt-convert`, the amd64 build (61.9
  MB), the smoke test 36 of 36 with the terminal checks, the Go oracle 21,792 of 21,800 as
  recorded.

## 2026-10-10: the READMEs

- The user's request: both READMEs up to date, and the divergence prepared for the next freeze.
  `arbace-for-java-26`'s README and FREEZE.md now name v1-v3, the base image and the Alpine
  package (`b07fdf9`, pushed). Main's README follows the `baseimage` merge (the Go executable,
  v3, `bin/arbace-j`, the licences). At the Go freeze (B1-PLAN step 8) the READMEs diverge as at
  the JVM freeze: main's gains a section "Arbace for Golang" after "Arbace for Java 26",
  pointing to the branch `arbace-for-golang`, its tags and the branch's freeze document; the
  branch's README describes the frozen executables locally.
- The user's decision: after the Go freeze main's journal starts over, as after the JVM freeze
  (2026-10-08): the journal up to the freeze stays on `arbace-for-golang`, its tag
  `arbace-for-go1.27.1-v1` and in git; main's new journal opens with a paragraph pointing to
  them, as this one points to `arbace-for-java-26`. Part of B1-PLAN step 8; CLAUDE.md's Records
  paragraph is updated then to name both freezes.

## 2026-10-10: what comes after the freeze

- The user's decisions: XML (SAX, `arbace.xml`), `gen-class` and Java serialization will not be
  done: they stay out of the Go build for good (D6; the suite's `clojure-xml`, `genclass` and
  `serialization` namespaces stay skipped, the oracle's serialization case stays a known
  mismatch). After the freeze, the `.ae` rename comes first, then the focus is TamaGo: B1b, the
  box. AOT of namespaces to Go (the throughput gap) is not scheduled.
- The user's decision, the same day: after the `.ae` rename, a Clojure → Arbace translator,
  c2a: first the mechanical renames (`clojure.*` namespaces, the `clojure.lang`, `clojure.asm`
  and `clojure.java.api` packages, the `:clojure.error` keyword, the `clojure.*` system
  properties; what `arbace.j2c.rename` does for the test tools today), later more as the Clojure
  and Arbace dialects diverge. With it the tools are renamed to name Arbace as their target:
  j2c → j2a, g2c → g2a, and c2g → a2g if it is still needed then. Arbace runs Clojure source
  unchanged only when it names nothing but `clojure.core` (measured: `(require 'clojure.string)`
  fails on both builds; `#?(:clj ...)` reads); a measured corpus of libraries is deferred.

## 2026-10-10: main's image on java.base; bin/arbace-j with rlwrap

- Agent, branch `baseimage` (to `4a57d5d`), merged (`7d17154`). The `arbace-for-java-26-v3`
  changes ported to main as VENDOR-NOTES hand changes 15-17 (main's 14 is MultiFn's order):
  15 `bean` by reflection in a new `arbace/core_bean.clj` (properties sorted by name, as
  upstream), so main's Go variant of core_proxy drops its own `bean` and keeps only
  `core_bean.subst.clj` (`bean-exported?` true: jrt has no modules); 16 `#inst` without
  java.sql (`arbace/instant_timestamp.clj`, left out of the Go embed, whose `instant` variant
  has its own); 17 `arbace.xml`, `arbace.inspector`, `arbace.java.browse-ui` fail with an error
  naming their module. `bin/arbace-image`: `java.base,jdk.unsupported` plus
  `ARBACE_IMAGE_MODULES`; 133 → 100 MB, start with the cache 180-190 ms. The launcher uses a
  cache not older than the jar.
- `bin/arbace` renamed `bin/arbace-j` (git mv; live callers, CLAUDE.md's layout mention, README,
  ORACLE.md and the notes updated; historical records keep the old name); its interactive REPL
  runs under rlwrap with the Go executable's convention (`ARBACE_RLWRAP`), tested on a
  pseudo-terminal (history recall, `off`, `-e` unwrapped, 15 argument patterns).
- Found on the way: `(reduce + (eduction (map inc) (range 10)))` throws ClassCastException on
  every JVM release (v1-v3; upstream Clojure 1.12.6 and the Go build give 55): an agent fixes it
  on both lines. And v3's `bean` returned its keys in hash order (fixed on `apk26`, `84da2dc`,
  not yet released).
- Main session on the merge: `bin/gate` passed (6m51s); the Go oracle as recorded (8);
  Clojure's suite on Go 19,628 of 19,632, no regressions.
- Main's README brought up to date (the user's request): the third bullet says the runtime is
  already a static Go executable; "Arbace for Java 26" names v1-v3 and the Alpine package and
  links FREEZE.md and ALPINE.md on the branch; main's JVM commands with `bin/arbace-j` and
  `bin/arbace-image`; a new section "The Go executable" (what it is, its numbers, what it leaves
  out, the coming freeze, how to build and run it), which becomes "Arbace for Golang" at the
  freeze; the documentation list gains doc/go/; the licences beyond EPL-1.0.
