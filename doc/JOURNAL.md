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
