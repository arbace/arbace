# repr/go's converter: notes

> Names: since 2026-10-17 g2a is repr/go and j2a repr/java (doc/NAMING-SURVEY.md, "Decided"):
> `arbace.g2a.*` is `arbace.repr.go(.*)`, its build `arbace.g2a.build` is `arbace.build.go`;
> `bin/g2a convert` is `bin/repr-go read` (`bin/g2a read`: `read --dump`), `bin/g2a build`
> `bin/build-go`, `bin/g2a-print` `bin/repr-go print`, `bin/g2a-check` `bin/repr-go check`,
> `bin/g2a-convert-tests` and `-print-tests` `bin/repr-go test read` and `test print`,
> `bin/g2a-build-tests` `bin/build-go test`; `G2A_GOROOT` is `ARBACE_GOROOT`, the other `G2A_*`
> `REPR_GO_*`, `J2A_*` `REPR_JAVA_*`; `bin/j2a-check` is `bin/repr-java check`. a2g is j2g
> (`arbace.a2g.*`, `bin/a2g*`, `A2G-SPEC.md`, `A2G_*`, the printed `a2g_*`, `A2g_*`, `a2g/*`: `j2g`
> likewise), and class forms are Java forms (the front end keeps `arbace.classes.*`). This document
> uses the current names throughout, its dated passages included; branch names keep theirs.

`arbace.repr.go` turns the helper's typed syntax tree of a Go package ([HELPER-NOTES.md](HELPER-NOTES.md),
format `:godump 2`) into Go forms ([SPEC.md](SPEC.md)), milestone G1 of
[G2C-SURVEY.md](../G2C-SURVEY.md) §7. As in repr/java ([classes/CONVERTER-NOTES.md](../classes/CONVERTER-NOTES.md)),
the front end is dumb and every decision about forms is made here, in Clojure. The forms are
printed back to Go by [the printer](PRINTER-NOTES.md); the round trip is judged by the oracle
([ROUNDTRIP.md](ROUNDTRIP.md)).

## Running

```sh
bin/repr-go read fmt sort              # dump (godump) and convert into .tmp/repr-go/forms/pkgs
bin/repr-go read --full --tests fmt    # mode :full; with the _test.go files and fmt_test
bin/repr-go read --goos linux --goarch arm64 os   # for another configuration (default
                                      # tamago/amd64)
bin/repr-go read --corpus amd64        # a corpus of `bin/repr-go corpus` ([linux-]amd64, arm64,
                                      # test, test-arm64, tests) into .tmp/repr-go/forms/CORPUS,
                                      # report in .tmp/repr-go/forms/CORPUS.report.edn
bin/repr-go read --corpus test --print DIR   # and print each package into the candidate DIR
bin/repr-go roundtrip [amd64|arm64]       # convert + print std and $GOROOT/test into
                                      # .tmp/repr-go/cand-ARCH, bin/repr-go check against the
                                      # reference test/repr/go/roundtrip-ARCH.edn (tamago)
bin/repr-go roundtrip linux/amd64         # the same for linux (also --goos linux arm64):
                                      # .tmp/repr-go/cand-linux-ARCH, reference
                                      # test/repr/go/roundtrip-linux-ARCH.edn
bin/repr-go roundtrip --tests             # std amd64 with its tests (.tmp/repr-go/cand-tests)
bin/repr-go roundtrip ... --record        # the same, recording the results as the reference
bin/repr-go print ...                     # the printer (bin/repr-go print)
bin/repr-go test read                          # the converter's tests (bin/repr-go test read)
```

The tools run on Arbace's Go executable (`bin/lib/repr-go-host.bash`: `REPR_GO_EXE`, else the
tools' host, `bin/lib/host.bash`; on `target/stage2` until standalone S4, with a heap
`REPR_GO_JVM_OPTS`, default `-Xmx12g`); `arbace.repr.go` loaded from the checkout's sources until S6,
compiled into the executable since (SA133), a source in the checkout that differs from the
embedded copy loaded from it instead, evaluated. Conversion
runs one package per task on a thread pool (`REPR_GO_JOBS`, default half the cores). Every file written is read back with Arbace's
reader (`LineNumberingPushbackReader`, `*read-eval*` false) and compared with the forms in
memory: data, every metadata key, and, in mode `:lines`, the line of every list (the reader's
`:line` or an explicit one) against the Go line it stands for. A difference fails the run.

## Structure

| namespace | role |
|---|---|
| `arbace.repr.go.convert` | the converter: nodes → forms. Files (`convert-file`: imports, header directives, directive placement, comments), declarations (`func-decl`, `gen-decl`), statements (`stmts-forms`: the `let` scoping of §7.3, `stmt`, `if-form`, clauses), expressions (`expr`, `call`, `selector-form`, `composite`, `instantiation`), types from the source (`type-form`, `fields`, `signature`, `type-params`), and the package form |
| `arbace.repr.go.types` | type forms from the helper's type table (for `:inst` and `:tag`), qualification by import name or namespace (§4.4), constant kinds |
| `arbace.repr.go.lit` | literals (§8.1) and constant values (§8.2): integers with their radix, exact floats (double, `BigDecimal`, ratio), runes, strings and `byte-string`, Go's UTF-8 validity |
| `arbace.repr.go.layout` | the forms text: mode `:lines` places every form on its Go line (§10.1), mode `:full` writes `:go/pos`/`:go/apos` (§10.2); ordinary comments as `;` comments |
| `arbace.repr.go` | the command line, the read-back check, coverage counts, the `--print` hook into `arbace.repr.go.print` |
| `arbace.repr.go.read` | reading dumps (the helper's, extended) |

In memory a form is Clojure data: lists, vectors and symbols carry the spec's public metadata
and two private keys, `:arbace.repr.go.convert/l` (the Go line the form begins on) and
`.../pos` (its node's position fields, for `:full`). Literals are `arbace.repr.go.lit/Lit`
records: the text written (`0x2A`, `2r1010`, `1E+400M`) and the value read back. The layout
first decides the breaks (a form whose Go line is after everything written before it starts
that line), then writes the text; `bin/repr-go read` checks that reading the text gives the
forms back.

## Decisions

Beyond what the spec fixes:

- **Layout (`:lines`).** The `in-ns` and `go/file` forms share the package clause's line;
  comments before it (copyright) are `;;` lines above. A form on a later Go line than
  everything before it breaks; lists get their line from the reader, symbols, vectors and `@`
  forms an explicit `^{:line N}`, literals and keywords their parent's `:go/breaks` (nth
  index, head 0: A2). When the forms must put a list after its Go line (the type of
  `x.(T)` precedes `x`, and `T` spans lines), the list gets an explicit `:line` (8 lists in
  std, 700 in `$GOROOT/test`, all type assertions to multi-line interface types).
  Indentation is two spaces per open bracket. Doc strings, names and signatures stay on the
  declaration's line (A8).
- **Comments.** Doc comments kept as data (§6.5) and directives are not repeated as
  comments; every other comment is written at the end of its Go line (`; text`) or on its own
  line (`;; text`); the lines of a block comment each on their line. Line comments after
  struct fields (`lo uint8 // lowest ...`) are comments, not `:doc`.
- **Doc text** is `CommentGroup.Text` exactly, with its final newline.
- **Directives (§9.1).** As gc does (`syntax/parser.go`: pragmas accumulate until the next
  declaration takes them or `clearPragma` drops them; `noder.go`), a `//go:` directive
  between two top-level declarations attaches to the next one *across blank lines*, unless it
  is `build`, `linknamestd`, `generate`, `cgo_*`, or a `linkname` naming another declaration;
  those, and non-`//go:` directives (`//export`, `//line`, `//tool:x`), are free-standing
  `(go/directive "...")` forms where they stand. Inside a group, a directive attaches to the
  next spec. Header directives (before the package clause) go to `go/file`'s `:build`
  (`//go:build`, `// +build`) and `:directives`. Directives after the package clause and
  before the end of the imports are `(go/directive ...)` elements of `:imports` in source
  order (proposal C4). Directives in function bodies are statement-level forms in the
  statement list where they stand. A `//line` comment not at column 1 is not a directive for
  gc (`cmd/compile/doc.go`) and is kept as an ordinary comment. Two `/*line*/` directives
  inside expressions (`issue29504.go`, `issue38698.go` of `$GOROOT/test`) cannot be placed
  (§10.3; the helper warns): they are reported, not written.
- **Files.** `x.go` is `<pkg-dir>/x.ae`, except when the package's source directory has a
  subdirectory `x` (a package whose own file would be `x.ae`): then `x.go.ae` (proposal
  C7; 9 files in std, e.g. `runtime/debug.go`).
- **Single-file programs** (`$GOROOT/test`) keep their import path
  `command-line-arguments` in `:path`; their namespace is `go.test.` plus the file's path
  (`fixedbugs/bug257.go` → `go.test.fixedbugs.bug257`).
- **Package form.** `:config` from the dump (`:go-version` as `:lang`); `:other-files` the
  sorted union of the helper's non-Go lists (`:s-files`, `:h-files`, `:syso-files`, and the
  cgo-only lists, empty with cgo off, so in every configuration: L2, accepted 2026-10-08,
  folded into SPEC §4.2); `:embed-files` and the tests' embed files; `:test-files`
  with `-tests`. A test-only package without Go files gets its last path element as name.
- **`:init-order`.** One entry per initializer; a blank variable `["file.go" line col]`,
  also inside `(values ...)`.
- **`else if` chains** become `cond` when there are two or more tests without init and the
  chain ends in a plain `else` block or nothing; a chain ending in an `else if` with an init
  stays nested `if` forms (each prints as `else if`).
- **Untyped constant shifts (§8.3).** A non-constant shift whose left operand is an untyped
  constant expression (a literal, a constant of untyped type, or unary/binary combinations of
  them) gets `:tag`, go/types' type of the shift.
- **Constant values (§8.2)** are built from the constant's type (`:t`), not from the value's
  kind (the helper's note: `1` in `1 + 2i` is an integer value of type `untyped complex`). A
  named type's kind comes from its underlying type in the dump's `:types`; for a named type of
  another package (whose underlying type is in that package's dump) the value's kind decides.
  Typed floats are doubles. Values go/types holds as a `big.Float` beyond a rational's range
  (`(:float M E)`: `1e1000000` and the like, only in `$GOROOT/test`) are
  `(binary-float M E)`, M·2^E (proposal C8): their decimal would have up to hundreds of
  millions of digits.
- **Float literals (§8.1).** A decimal literal is a double when `Double/toString` of its
  nearest double denotes exactly its value, else a `BigDecimal` (always exact; `1E+400M`). A
  hexadecimal float is a ratio (`0x1p-2` → `1/4`), or a `BigDecimal` when integral
  (`0x1p4` → `16.0M`). Typed float `:val`s are doubles. Integral values keep `.0` or an
  exponent. Non-integral values whose decimal expansion is long (over 80 characters) are
  ratios.
- **Negative literals.** `-1`, `-1.5`, `-0x10` are Clojure literals (§7.5); `-'a'` and
  `-2i` stay `(- ...)`, and so does `-0` of an integer or an exact float (`time.Unix(-0, 0)`
  in `archive/tar`'s tests): `-0` would read as `0`; a double keeps its sign (`-0.0`).
- **Strings.** A rune is a character in the BMP outside the surrogates, else `(rune 0x...)`;
  characters that do not print (controls, format characters, U+FFF0-U+FFFF) are `\uXXXX`.
  Byte strings split into maximal valid UTF-8 runs by Go's `utf8.DecodeRune`.
- **Reserved heads.** `go/call` for calls of every unqualified head of §13.5 (statement,
  expression, builtin and type heads, `deref`): in std 334 calls, mostly `fn` and locals named
  `assert`, `do`, `print`.
- **Field groups (A1).** Inside generic declarations (type params, generic receivers, generic
  types, and local generic types), a name declared in one field with the name before it gets
  `^:go/grouped`; a name declared apart although of the same type as the name before it gets
  `^{:go/grouped false}`, so the printer, which groups equal types when no marker says
  otherwise, keeps them apart.
- **Instances in `new` and `method-expr`** are written `(inst G T...)` (A5), so that they
  read the same in any position.

## Deviations from the spec: amendments (accepted 2026-10-08)

The converter follows these. The user accepted all eleven on 2026-10-08, with the printer's
A1-A10 ([PRINTER-NOTES.md](PRINTER-NOTES.md); the converter applies A1, A2, A5, A8, A9) and
the helper's author's H1-H7 ([HELPER-NOTES.md](HELPER-NOTES.md), applied: type assertions
carry `:dot`; constant representation from `:t`; `:go/via` from the helper's `:via`; file
lists merged into `:other-files`; `p_test` → `go.<path>_test`; gc attaches `//go:` lines
across blank lines, verified as C1). All are folded into SPEC.md, marked "(amendment, accepted
2026-10-08)" (SPEC §15, "Amendments (2026-10-08)"). Where two disagreed, the converter's won
over the printer's over the helper's: C1 settles H2; free-standing `go/directive` forms for
non-`//go:` directives over A6's metadata; C9 over A9; the converter's use of `:go/grouped`
(generic declarations only, `false` for names declared apart, local const groups) over A1's;
the value's kind for named types of other packages refines H4. The choices under "Decisions"
above that fix the spec are folded too: single-file namespaces (§4.1), test-only package
names (§4.2), blank variables in `:init-order` (§6.3), doc text (§6.5), negative literals
(§7.5), `cond` (§7.8), float literals and characters (§8.1), directive placement (§9.1,
§9.2), layout (§10.1), `/*line*/` in expressions (§10.3), comments (§10.4).

- **C1. Directives attach across blank lines** (§9.1 says "no blank line or other code
  between"): gc's parser keeps pragmas from the end of one declaration to the next
  (`syntax/parser.go`, `clearPragma` after each top-level declaration, `takePragma` at the
  next), so a blank line does not detach them. The forms follow gc. *Accepted 2026-10-08,
  folded into SPEC §9.1.*
- **C2. Local const groups** (N6 says local groups become one declaration per spec): gc and
  the oracle keep a `const ( ... )` group of several specs (iota and implicit repetition
  depend on it; ROUNDTRIP.md, normalization 4). The bindings of a local group after the
  first carry `^:go/grouped`, and a spec without init (its type and init written out, as N6
  says) `^:go/implicit`; the printer can then print the group as written. 79 groups in std.
  *Accepted 2026-10-08, folded into SPEC §7.3 (and §3.2 N6, §12.2, §13.1, §14.5).*
- **C3. Parentheses around `&&` and `||` operands** (N1 removes all parentheses): gc removes
  dead branches by syntax and does not look through parentheses (ROUNDTRIP.md, "logical";
  `bytes.IndexRune` and `strings.IndexRune` compile differently without them). The operand
  form gets `^:go/paren`, and a parenthesized operand is not merged into the chain.
  *Accepted 2026-10-08, folded into SPEC §7.5 (and §3.2 N1, §11.1, §12.2, §13.1, §14.5).*
- **C4. Directives among the imports** are `(go/directive "...")` elements of `:imports`,
  in order (`crypto/internal/fips140/mlkem`'s `//go:generate` between the package clause and
  the imports). *Accepted 2026-10-08, folded into SPEC §4.3 (and §9.1, §12.2, §13.1).*
- **C5. `(go/id "x")` as a name.** In parameter, field and binding positions a tagged
  `(go/id "true")` is a name like a tagged symbol (`text/template/parse`'s parameter `true`).
  *Accepted 2026-10-08, folded into SPEC §4.4 and §5.3 (and §13.1).*
- **C6. Local generic types** (`$GOROOT/test`, and std's tests): a `let-type` name carries
  `:type-params [...]` as metadata, keeping the vector's pairs. *Accepted 2026-10-08,
  folded into SPEC §7.3 (and §13.1).*
- **C7. File-name collisions**: `x.go.ae` when the package directory has a subdirectory
  `x` (above). *Accepted 2026-10-08, folded into SPEC §4.1.*
- **C8. `(binary-float M E)`** for constant values beyond a rational's range (above).
  *Accepted 2026-10-08, folded into SPEC §8.2 (and §13.4, §13.5).*
- **C9. `:go/label` (the printer's A9)**: a labeled `:=` or `var` (`L: x := 1`, 20 in std,
  e.g. `internal/runtime/maps`) is `^{:go/label :L}` on the first target of its `let`.
  *Accepted 2026-10-08, folded into SPEC §7.3 (and §7.1, §12.2, §13.1, §14.5); it wins over
  the printer's A9.*
- **C10. Multi-name `var` without type**: `var a, b = f()` in a function is
  `^:var (values a b) (f)`, the marker on the `values` form. *Accepted 2026-10-08,
  folded into SPEC §7.3 (and §7.1, §13.1).*
- **C11. `:full` positions of spliced nodes.** A function form carries its `func` keyword
  and its body's braces (`:func`, `:lbrace`, `:rbrace`); a parameter or result vector its
  field list's `:opening`/`:closing`; `when`, loops, `switch`, `type-switch` and `select`
  their body's braces; `if` its then block's and `:else-lbrace`/`:else-rbrace`; a selector
  also its name (`:name-pos`) and a qualified symbol `:x-pos`. A binding target carries its
  statement's `:tok-pos`. Lost: the positions of parentheses (N1), of the `return` keyword of
  an implicit return, of a merged chain's inner operators, and of literals beyond their own
  (`:go/apos` holds one position per child). *Accepted 2026-10-08,
  folded into SPEC §10.2.*
- **The spec's text.** §8.2 calls `1.0E100` inexact, but by §8.1's rule the double text
  `1.0E100` denotes exactly 10^100, so it is written as a double. §14.2 shows `:val 239` for
  `runeErrorByte0`, an untyped rune (`t3 | RuneError>>12`), which §8.2 writes as a character
  (`\ï`), and `:doc` for acceptRange's line comments. §14.1's `:go/end` and lines are
  illustrative (the printer's notes say the same). The examples' docs are shortened.
  *Accepted 2026-10-08, folded into SPEC §8.2 (`1.0E100`), §14.2 and §6.5 (`runeErrorByte0`
  as a character, `acceptRange`'s comments as `;` comments, doc text with its newline) and §14
  (the lines of §14.1 illustrative).*

## Coverage

Every `go/ast` node kind of §13.1 that occurs is converted (`Bad*` never occurs: the helper
reports type errors and there are none). `bin/repr-go read` counts the nodes it consumes per
kind against the dumps' nodes: equal for every kind in every corpus (std amd64: 2,308,308
nodes). Notes counted (std amd64): implicit returns 12,435, attached directives 1,218,
free-standing 131, `cond` 500, `go/call` 334, shift tags 269, negative literals 1,873,
labeled declarations 20, local const groups 79, file-name collisions 9, `go/id` 2.

## Corpus

Tamago go1.27.1, 64 cores (32 conversion threads), Arbace stage 2, each file read back and
checked (times include reading the dumps and the check):

| corpus | dumps | Go files | forms | convert + check | failed | not read back |
|---|---:|---:|---:|---:|---:|---:|
| std, tamago/amd64 | 379 | 1,742 | 30.7 MB | 8.7 s | 0 | 0 |
| std, tamago/arm64 | 378 | 1,738 | 30.7 MB | 8.4 s | 0 | 0 |
| std, linux/amd64 | 382 | 1,834 | 31.3 MB | 19.7 s | 0 | 0 |
| std, linux/arm64 | 380 | 1,829 | 31.3 MB | 19.7 s | 0 | 0 |
| `$GOROOT/test`, tamago/amd64 | 1,707 | 1,708 | 9.8 MB | 2.6 s | 0 | 0 |
| `$GOROOT/test`, tamago/arm64 | 1,702 | 1,703 | 9.8 MB | 2.2 s | 0 | 0 |
| `$GOROOT/test`, linux/amd64 | 1,713 | 1,714 | 9.9 MB | 3.8 s | 0 | 0 |
| `$GOROOT/test`, linux/arm64 | 1,707 | 1,708 | 9.8 MB | 3.9 s | 0 | 0 |
| std with tests (`-tests`), amd64 | 554 | 2,935 | 49.2 MB | 10.6 s | 0 | 0 |
| std amd64, mode `:full` | 379 | 1,742 | 86.4 MB | 10.3 s | 0 | 0 |
| `$GOROOT/test`, mode `:full` | 1,705 | 1,706 | 20.2 MB | 3.2 s | 0 | 0 |

The linux rows were measured later the same day (2026-10-08) with the conversion's printing into the
candidate included (`bin/repr-go roundtrip`, `--print`); the tamago corpora measured that way take the
same time as linux's (std about 20 s). Every node kind of the linux corpora is converted (nodes
consumed equal the dumps' per kind; std linux/amd64: 2,357,789 nodes), with no node kind, directive
or marker that tamago's corpora lacked. The JVM's start added about 2 s. Measured with `-Xmx16g`, the
process peaked at about 14 GB (the heap grew to its allowance; runtime's dump alone is 24.8 MB of
text); the default became `-Xmx12g` (the JVM host's, until standalone S4).

Two runs give byte-identical forms trees (checked on std amd64).

## The round trip

`bin/repr-go roundtrip` converts, prints with `arbace.repr.go.print/print-package` (lines layout) into a
candidate tree and runs `bin/repr-go check` (tree, export, code; [ROUNDTRIP.md](ROUNDTRIP.md)) against a
recorded reference, `test/repr/go/roundtrip-ARCH.edn` for tamago and `test/repr/go/roundtrip-linux-ARCH.edn`
for linux (Clojure-reader format, one entry per line): it exits non-zero on a regression, an entry
and level passing in the reference and not now (or missing). `--record` rewrites the reference. The
`$GOROOT/test` programs are the oracle's list (`bin/repr-go check --list-tests`, `gocmp tests`), dumped
by the helper as they are: one rule for both. Result (2026-10-08, closing milestone G2):

| corpus | entries | tree pass/fail/err | export pass/fail/err/skip | code pass/fail/err/skip |
|---|---:|---|---|---|
| std, tamago/amd64 | 373 | 373 / 0 / 0 | 372 / 0 / 0 / 1 | 372 / 0 / 0 / 1 |
| `$GOROOT/test`, tamago/amd64 | 1,707 | 1,707 / 0 / 0 | 1,707 / 0 / 0 / 0 | 1,707 / 0 / 0 / 0 |
| std, tamago/arm64 | 372 | 372 / 0 / 0 | 371 / 0 / 0 / 1 | 371 / 0 / 0 / 1 |
| `$GOROOT/test`, tamago/arm64 | 1,702 | 1,702 / 0 / 0 | 1,702 / 0 / 0 / 0 | 1,702 / 0 / 0 / 0 |
| std with tests (`--tests`), amd64 | 379 | 379 / 0 / 0 | 378 / 0 / 0 / 1 | 369 / 9 / 0 / 1 |
| std, linux/amd64 | 376 | 376 / 0 / 0 | 375 / 0 / 0 / 1 | 375 / 0 / 0 / 1 |
| `$GOROOT/test`, linux/amd64 | 1,713 | 1,713 / 0 / 0 | 1,713 / 0 / 0 / 0 | 1,713 / 0 / 0 / 0 |
| std, linux/arm64 | 374 | 374 / 0 / 0 | 373 / 0 / 0 / 1 | 373 / 0 / 0 / 1 |
| `$GOROOT/test`, linux/arm64 | 1,707 | 1,707 / 0 / 0 | 1,707 / 0 / 0 / 0 | 1,707 / 0 / 0 / 0 |

`unsafe` has no export data or code (skip).

The linux rows (2026-10-08, B1a step 0) needed no change to the helper, the converter or the
printer: the configuration was already a parameter of the helper (`-goos`), written into every
dump and every package's `:config` (§4.2), and the printer's build takes its environment from
there. What changed is the plumbing (`bin/repr-go corpus linux-…`, `convert --goos/--goarch`,
`roundtrip linux/ARCH`, `bin/repr-go check --goos`, `CGO_ENABLED=0` everywhere: on a linux host the
go command turns cgo on for `GOOS=linux` by default; amendments L1 and L5, accepted
2026-10-08, folded into SPEC §1 item 7 and §12.4) and one fix in the oracle: two programs
passed the code level vacuously, their listings empty from the build cache
([ROUNDTRIP.md](ROUNDTRIP.md), "Known gaps"). The linux std's extra files (system calls and
their generated tables, `os`, `net`, `internal/poll`, the runtime's linux files and vDSO
code; 173 files not in tamago's configuration) hold no construct tamago's lacked; their
assembly, like tamago's, is not converted (`:other-files`).

The nine code differences of std with its tests are known and explained in
`test/repr/go/roundtrip-tests-known.edn`: TamaGo's `testdata_tamago_test.go` files embed the packages'
own Go sources (`//go:embed *.go` and the like), and the oracle compiles the candidate's sources in
the original's place, so the test binaries embed other text. With `--tests` the oracle compares the
`_test.go` files' trees and the export data and code of the package and its test variants (`gocmp
-tests`).

Times (64 cores, warm build cache): `bin/repr-go roundtrip` amd64 about 40 s (convert and print
std and `$GOROOT/test` 29 s, of which std 21 s; the check 11 s); arm64 the same; linux/amd64
and linux/arm64 39 s each (28 s and 10 to 11 s; the first linux runs, dumping with a cold go
list and checking with a cold build cache for the configuration, 93 s and 98 s: convert and
print 44 s and 52 s, check 49 s and 46 s); `--tests`
29 s and 17 s. The check from an empty build cache takes about a minute more (ROUNDTRIP.md).

The lines are close to the original's: compiled from their own layouts (`gocmp
-keep-positions`, so that positions are compared through the code: instruction order, inline
marks, frame slots), the candidates' code equals the original's in 371 of 372 std packages and
1,704 of 1,707 programs on amd64 (369 of 371 and 1,699 of 1,702 on arm64; linux: 374 of 375
and 1,710 of 1,713 on amd64, 371 of 373 and 1,704 of 1,707 on arm64, the same packages and
programs as tamago's). The rest differ by
columns (mode `:full`'s business) and by `/*line*/` directives inside expressions the forms
cannot place (`issue29504.go`, §10.3).

What it took (the first differences of the earlier run, 2026-10-08): the printer's support of
the markers C2 (local `const` groups, 35 entries), C9 (labeled declarations, 11), C6 (local
generic types, 4), C3 (`&&`/`||` parentheses, 2), C4 (a directive among the imports), C5 (a
parameter named `true`); printer fixes for one-line clause lists (`switch {; case`), nested
labels, `-0x8000000000000000` and an empty `(go/const)`; and one selection of `$GOROOT/test`
programs for the helper and the oracle: 19 programs the oracle took but the helper's build
constraints or configuration excluded; 18 `compile` programs with body-less functions and the
two-file `cmplxdivide.go` that the helper took but the oracle could not build (it now compiles
programs without `-complete`, as Go's test driver does, and takes programs of several files);
2 programs whose action line is `//run` without a space, which the helper's old reading
missed.

### Directory tests (2026-10-17)

`bin/repr-go roundtrip --dirs [[GOOS/]ARCH]` takes `$GOROOT/test`'s directory tests: the corpus
`[linux-]dirtest[-arm64]` (`godump -dirs`, HELPER-NOTES.md "Directory tests"; one dump per
package of each test), converted and printed into `.tmp/repr-go/cand-dirtest-[GOOS-]ARCH`, then
`bin/repr-go check --dirtests` (each test compiled package by package as Go's test driver
compiles it, and run: ROUNDTRIP.md "Directory tests") against the reference
`test/repr/go/roundtrip-dirtest-[GOOS-]ARCH.edn` and the known differences
`test/repr/go/roundtrip-dirtest-known.edn` (none). `--text --dirs` is their text level. What the
converter needed (amendments D1 and D2, proposed 2026-10-17, SPEC §4.1-§4.3, §10.2): a dump with
`:dir-test` is a program like a single-file one, its namespace from its dump's path
(`go.test.fixedbugs.issue10066_dir.a`), the other packages of its test mapped to theirs
(`types/*namespaces*`, for the names it qualifies by namespace); a file's local import paths are
resolved through the dump's `:local-imports` for the names it qualifies by import (`a/T` for a
type of `test/a` imported as `"./a"`), and the map is the package's `:local-imports`; in mode
`:full`, a receive-only channel type spelled `<- chan T` gets the position of its `chan`, found
in the source (go/ast has the arrow's only), so that the printer keeps the space (2 files of
`import2.dir`). The `:lines` output of every other corpus is unchanged.

| configuration | tests (packages, files) | tree pass/fail/err | export pass/fail/err/skip | code pass/fail/err/skip | run pass/fail/err/skip | text (files: byte-identical) |
|---|---:|---|---|---|---|---|
| tamago/amd64 | 253 (561, 586) | 253 / 0 / 0 | 253 / 0 / 0 / 0 | 253 / 0 / 0 / 0 | 0 / 0 / 0 / 253 | 586: 586 |
| tamago/arm64 | 251 (559, 584) | 251 / 0 / 0 | 251 / 0 / 0 / 0 | 251 / 0 / 0 / 0 | 0 / 0 / 0 / 251 | 584: 584 |
| linux/amd64 | 254 (562, 587) | 254 / 0 / 0 | 254 / 0 / 0 / 0 | 254 / 0 / 0 / 0 | 127 / 0 / 0 / 127 | 587: 587 |
| linux/arm64 | 251 (559, 584) | 251 / 0 / 0 | 251 / 0 / 0 / 0 | 251 / 0 / 0 / 0 | 124 / 0 / 0 / 127 | 584: 584 |

The level run is skip for tamago (it does not run on this machine) and for the tests that only
compile (125 compiledir, 2 builddir); linux/arm64 runs through qemu-aarch64. Each configuration
takes about 10 s (dump under a second, convert and print 3 s, check 3 to 9 s).

## Raw forms (GP3, 2026-10-20)

Raw forms are a private stage of the native reader (doc/go/GO-PARSER-SURVEY.md §4.2 step 2,
"As built: GP3"): the converter's raw mode (`arbace.repr.go.convert/*raw*`, option `:raw` of
`convert-package`) writes the forms of an un-annotated tree, the native reader's, with every
decision of SPEC §11.1 that needs types left open and marked. They are for repr/go and the
checker only (GPS10 (a)): not a dialect of SPEC, not printed, not read by goi. GP4's checker
resolves them; its answers become the dump's annotations and the converter, unchanged, makes the
final forms.

**No annotation is read.** In raw mode `an` returns nil, so nothing of go/types reaches the
forms; what the converter decided from the tree alone it decides as before. Raw forms always
carry mode `:full`'s positions (`::pos`, `:go/pos` in the text): an open form keeps its node's
positions, so the checker's answers are keyed by position.

**What is open, and how each open decision is marked:**

| decision (SPEC §11.1) | final forms | raw forms | kind |
|---|---|---|---|
| a call, a conversion, a builtin call, a method call | `(conv T x)`, `(len x)`, `(.M x a)`, `(f a)`, `(go/call h a)` | `(F a ...)`, F the callee's raw form; `(go/call h a ...)` only when `h` is a head of the forms' syntax that is not a builtin's name (`raw-reserved-heads`): a raw call is never classified, so `(len x)` calls the identifier `len` | `:call` |
| field, method value, method expression, package member | `(.-f x)`, `(method-expr T M)`, `pkg/Name`, the head of `(.M x ...)` | `(go/select x f)`: x the operand's raw form, f the selected name (a symbol with its position; `(go/id "true")` for a name the reader would read as a literal, `r.true`) | `:select` |
| index or instantiation | `(aget x i)`, `(inst F T ...)`, `(G T ...)` (a generic type) | `(go/index x i ...)`, for `x[i]` and `x[A, B]` | `:index` |
| indirection or pointer type | `@x`, `(* T)` | `(go/star x)` | `:star` |
| universe names | `true`, `false`, `nil`, or `(go/id "true")` when declared | `(go/id "true")` always: a name the checker looks up | `:id` |
| new or reused names of `:=` | `x`, `^:assign x` | `^:go/define x` (every name but `_`) | `:define` |
| keys of composite literals | `:f v` (a field) or `[k v]` (a map's key, an index) | `^:go/key [k v]` when k is an identifier; other keys are expressions, as before | `:key` |
| constant values | `^{:val v}` on constant names | no `:val`; each constant name counted | `:val` |
| untyped constant shifts (§8.3) | `^{:tag T} (<< 1 s)` | no `:tag`; each `<<` and `>>` counted | `:shift` |
| an `any` constraint | left out when it is the universe's `any` | `^any T` | `:any` |
| implicit import names | `[fmt "fmt"]` | `["fmt"]` | `:import` |
| package initialization order | `:init-order [...]` | left out of `go/package` | `:init-order` (one per package) |
| inferred type arguments, embedded paths | `:inst`, `:go/via` | none: they belong to the calls, selectors, index forms and keys above | |

Decided from the syntax in raw mode as before: everything else. In particular a selector in a
type context is `pkg/T` (a TypeName is a qualified identifier), `a[i] = v` is `aset` (an
instance is not assignable), the cases of a type switch and the types of declarations, fields,
signatures and composite literals are type forms, and `for {}`, labels, `let` scoping, `cond`,
directives, comments and `:lang` are the converter's own.

**Invalid programs.** Raw forms exist wherever go/parser accepts a file, also for the invalid
programs the final conversion cannot represent (GC47's 64 of `$GOROOT/test`'s error tests; GPS9
(b)). Their constructs that no valid program has are marked too:

| construct | raw form | kind |
|---|---|---|
| `x.(type)` outside a type switch | `(go/type-guard x)` | `:type-guard` |
| `:=` where no declaration may stand (a `for`'s post statement) | `(go/define targets init)` | `:define-stmt` |
| a literal the forms cannot hold (`6e5518446744`) | `(go/literal "FLOAT" "6e5518446744")` | `:literal` |
| a non-name where a name is required (`(x) := 0`, `switch main() := ...`) | the expression's raw form | |
| a case of a type switch that is not a type's syntax (`case 0:`) | the expression's raw form | |

A selection with no object, a conversion of several arguments, a constant without a value and
an import whose path is not a name are ordinary raw forms (`go/select`, a call, a name without
`:val`, `["path"]`): the checker reports them.

**In memory.** Each open form, symbol or vector also carries the private key
`arbace.repr.go.convert/open` (its kind), which the text does not have:
`arbace.repr.go.convert/open-counts` counts them, and `arbace.repr.go.raw` checks them.

**Commands.** `bin/repr-go read --reader native --raw [--corpus C | PKG ...]` writes the raw
forms of the native reader's dumps as `.ae` files (mode `:full`), reads each back with Arbace's
reader against the forms in memory, and counts the open decisions by kind (report
`DIR/NAME.report.edn`, `:open`). `bin/repr-go native-check [CORPUS ...]` (`make
repr-go-native-check`; GP3's `raw-check`, merged into it at GP4 as GPS23 (c) decided) converts
each dump twice, the native dump raw and the helper's annotated dump as ever (mode `:full`), and
walks the two in parallel (`arbace.repr.go.raw/conform`): equal everywhere but at the open
decisions, where the final form must be one of that decision's resolutions (the table above). So
erasing go/types' decisions from the final forms gives the raw forms exactly. The resolutions are
counted (`[:call :conversion]`, `[:select :package]`, `[:define :reused]`, ...); known differences
in `test/repr/go/native-known.edn`.

**Resolved (GP4).** goi's checker answers the open decisions (doc/go/GOI-SPEC.md §3.10, "The
resolve mode"), `arbace.repr.go.annotate` writes the answers into the native dump as go/types'
annotations, and the converter, unchanged, makes the final forms of it, which `native-check`
compares with the helper's (E-ann, doc/go/GO-PARSER-SURVEY.md "As built: GP4"). `bin/repr-go read
--reader native --forms [--full]` writes them as `read --corpus` writes the helper's.

Three things the final forms wrote less precisely than the raw ones, which `conform` accepted
as the final's until GP5 (GPS25 (a)), are corrected in the converter since GP5 (GPS25 (b), the
native reader the default): the operand of an instantiation is written as the expression it is
elsewhere, inside its parentheses (mode `:full`'s `:go/parens`), a field's or method value's
selector with its name's position, a method expression's name with its line and position
(`instantiation`); a conversion keeps an invalid `...`, `(conv T (spread x))` (`unsafe.Pointer(&x...)`,
`ddd1.go`; the printer prints it back as written); and a `(spread x)` takes its operand's line,
a literal's too (`f(nil...)`, `append(b, "s"...)`; `spread`). `conform` compares them exactly.

**The switch (GP5).** `bin/repr-go read` reads natively by default (GPS7; `--reader helper`, or
`REPR_GO_READER=helper`, the helper, its oracle): `arbace.repr.go.annotate -main read` writes the
final forms as `arbace.repr.go -main read` writes the helper's, the same summary
(`arbace.repr.go/report`) and a summary file `OUT.tsv` in the helper's format (the checker's
errors where the helper has go/types'). `native-check` adds E-forms: the `.ae` text of both paths
in modes `:lines` and `:full`, byte for byte, and, where the helper's conversion fails, the native
one failing with its message (doc/go/GO-PARSER-SURVEY.md, "As built: GP5").

## What remains

- Mode `:full` is written but not consumed by the printer yet: columns would make the
  positions (and `-keep-positions`' last differences) exact.
