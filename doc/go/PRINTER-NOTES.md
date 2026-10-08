# g2c's printer: Go forms → Go

The printer of milestone G2 ([SPEC.md](SPEC.md) §12): it reads Go forms with the Clojure reader
(`*read-eval*` false, line numbers on) and writes Go source that gc compiles as the original
(§3). It is the second half of the round trip *Go → converter → forms → printer → Go*; the
converter does not exist yet, so the printer is proven on hand-written forms (below).

```sh
bin/g2c-print [--layout lines|gofmt] [--no-line-directives] [--line-file] \
              [--src DIR [--build]] [--gofmt] FORMS.clj OUTDIR
bin/g2c-print-tests [REGEX]
```

`FORMS.clj` is a package file (`ns`, `go/package`, `load`s, §4.1) or one file holding
`go/package`, `go/file` and declaration forms. One `.go` file per `go/file` is written into
`OUTDIR`. `--src DIR` (the original package directory) copies `:other-files` and
`:embed-files` (SHA-256 checked) and writes `OUTDIR/overlay.json`; `--build` then runs
`go build -trimpath -overlay` in `DIR` under the forms' `:config` (§12.4). Both scripts run on
Arbace's `target/stage2` through `bin/lib/tools.bash`, like `bin/j2c`; `arbace.g2c` loads from
source.

## Structure

| namespace | file | role |
|---|---|---|
| `arbace.g2c.print` | `print.clj` | reading (`read-forms`, `collect` following `load`, `collect-forms` for forms in memory), files (§4.3: build lines, header directives, package doc, imports, declarations), the package build (§12.4), the CLI |
| `arbace.g2c.print-code` | `print_code.clj` | forms → tokens: classification by head (§4.4, §13.5), types (§5), declarations (§6), statements and expressions (§7), literals (§8), directives and doc comments (§9, §10.4) |
| `arbace.g2c.print-emit` | `print_emit.clj` | the emitter: tokens, Go's semicolon rule, token separation, indentation, the line of the cursor, placing forms on recorded lines (§10.1, §12.3), `//line` and `/*line*/` corrections |
| `arbace.g2c.print-text` | `print_text.clj` | literal spellings (strings, byte strings, runes, exact floats, §8.1, §12.2) and a port of `text/tabwriter` (gofmt's alignment) |

The printer resolves no names and needs no types: every form is classified by its head and
position, and the metadata for consumers (`:val`, `:inst`, `:go/via`, `:tag` on expressions)
is ignored, as §12.1 says.

**Spacing** follows go/printer (go1.27.1, `src/go/printer/nodes.go`): `binaryExpr` with
`cutoff`, `walkBinary`, `diffPrec` and the depth rules of call arguments, index and slice
expressions, assignments and composite literals (so `a + b*c`, `f(a+b, c)`, `s[x+1 : x+2]`);
`mayCombine` keeps tokens apart (`- -1`, `a / *p`, `a & ^b`, `x < -1`). Parentheses are added
only where the grammar needs them (§12.2): by precedence (a right operand of equal precedence
too), around operands of unary operators and `*`, around composite literals of type names in
`if`/`for`/`switch` headers (also instantiated generic types, `switch (P[int]{}) {`), around
conversions to `*`, `func`, `chan` and `<-chan` types, around a `func` type as callee. Never
around `&&`/`||` operands beyond precedence (ROUNDTRIP.md, known gaps).

**Alignment**: text is written with tabs for indentation and vertical tabs between the cells
go/printer aligns (struct fields, specs of a group with gofmt's `keepTypeColumn`, keys of
multi-line composite literals, consecutive one-line functions), and form feeds where
go/printer starts a new section; `print-text/align` is `text/tabwriter`'s `format`,
`writeLines` and `writePadding` for gofmt's configuration (minwidth 0, tabwidth 8, padding 1,
`DiscardEmptyColumns`, `TabIndent`). Raw strings and comments are escaped from it.

## Layout

Two layouts; the default is `lines` when the `go/file` form carries a `:line` (it was read
from text) and `gofmt` otherwise (forms built by a program):

- **`gofmt`**: lines are ignored and the code is laid out as go/printer lays out a tree
  without positions, except that function bodies with statements always span lines
  (go/printer would put small ones on one line) and top-level declarations are separated by
  a blank line. The result is gofmt's fixed point: `gofmt -l` lists none of the test outputs
  (one exception, a gofmt bug, below).
- **`lines`** (§10.1, §12.3): each form with a recorded line is put on that line. Recorded
  lines are `:line` on lists (the reader's), on symbols and vectors (written), a parent's
  `:go/breaks` for children that cannot carry metadata (literals; statements too, such as a
  final `nil` or `-1`), and `:go/end` for the closing brace of `go/func`, `go/method` and
  `fn`. Lines are the cursor's line as gc sees it.
  - Before a statement, declaration, spec, field, clause or element: line breaks up to the
    recorded line, or `;` (`,`) on the same line.
  - Inside an expression: line breaks where Go allows them (after an operator, `(`, `,`, ...);
    where it does not (after a token that ends a statement), a `/*line :N:1*/` directive.
  - A form that would land after its line (the forms leave too little room, typically a
    header longer than the original's: `import "a"; import "b"` is printed as one group) gets
    `//line :N:1` on a line of its own before it, or `/*line :N:1*/` inside a line. The
    column keeps the file name (see A3). `--no-line-directives` turns these off.
  - Closing brackets without a recorded line (blocks other than function bodies, composite
    literals, calls, groups) go on a line of their own when there is room before the next
    recorded line (`*next*`, the room rule), with a trailing comma where Go's semicolon rule
    needs one; otherwise they stay on the line (`f() }`, `x}`). Forms without a line follow
    the same rule, so a hand-written forms file gets a readable Go file with its own lines.
  - `--line-file` starts the file with `//line FORMS.clj:1`, so that gc's positions name the
    forms file with the forms' lines (§12.3's printer option for hand-written forms).

  The lines layout is gofmt's only when the forms leave gofmt's room (the converter's forms
  do: they have the lines of gofmt'd Go). Running gofmt over it is not safe in general: gofmt
  keeps lines only where the layout is already its own (A10).
- **`:full`** (§10.2, columns): not implemented; a package with `:positions :full` is printed
  in the lines layout.

## Coverage

Every form of the spec is printed; `test/g2c/cases/` covers each, and the test `spec-coverage`
checks that every head of §13.5, the metadata the printer reads and the `go/package` and
`go/file` options occur in a case.

| spec | forms | case |
|---|---|---|
| §2 | the first example (sort.Search), lines layout | `s02_search` |
| §4.1-4.2 | package file, `load` of one file per Go file, `:config`, `:files`, `:other-files`, `:embed-files`, `:init-order`, `:positions`, `:test-files` | `s04_multi`, `s09_directives`, `s04_testpkg` |
| §4.3 | `go/file` `:build`, `:directives`, `:doc`, `:package`, `:imports` (`[name "path"]`, `^:alias`, `_`, `.`) | `s04_files`, `s04_testpkg` |
| §4.4 | `pkg/Name`, dot-imported names, labels, `go/call` (`fn`, `len`), `go/id` (`true`), heads as plain symbols (`do`, `when`) | `s04_files` |
| §5.1-5.2 | `* slice array map chan func struct interface \| tilde inst`, `(G T...)`, `(array ... T)`, `chan (<-chan T)`, `unsafe/Pointer` | `s05_types`, `s07_exprs` |
| §5.3 | parameters, unnamed and variadic (`&`), single and named results, results vectors of function types | `s05_types`, `s06_decls` |
| §5.4 | fields, embedded (also `*T`, generic), `:go/tag`, `:doc`, interface methods, embedded interfaces, type sets | `s05_types`, `s10_positions` |
| §5.5 | `:type-params` (constraints, trailing comma `[P *int,]`), generic receivers, generic methods (Go 1.27), `inst`, `:inst` | `s05_types`, `s06_decls`, `s07_exprs`, `s12_printer` |
| §6 | `go/type` (alias, generic alias, groups, a group of one), `go/const` (`values`, implicit repetition, `:val`), `go/var` (no init, `nil` init, `values`, groups), `go/func`, `go/method` (unnamed receiver, `^:extern`, `init`, implicit return, `panic`), doc comments | `s06_decls`, `s09_directives` |
| §7.1-7.4 | `let` (`:=`, `values`, `^:assign`, `var` with `zero`, `^:var`, `^:const`), `let-type` (`^:alias`), `set!` (3 shapes, the 11 operators), `inc!`, `dec!`, `aset`, `do` | `s07_stmts` |
| §7.5 | every operator, n-ary chains, precedence and parentheses, unary combinations | `s07_exprs`, `s12_printer` |
| §7.6-7.7 | `.-f`, `.M`, field calls, method values and `method-expr`, `addr`, `@`, `lit` (positional, keyed, map, elided `_`, `&T{}`, `[...]T`, promoted keys), `aget`, `subslice` (all shapes), `conv`, `assert` (comma-ok), `spread`, every builtin, `new` of a value, `unsafe` | `s07_exprs`, `s12_printer` |
| §7.8 | `if` `when` `cond` (`else if`, `else { if }`), init vectors of every kind, `switch` (init, tag, `default` anywhere, `fallthrough`, empty), `type-switch` (guard forms, init), `select` (every comm, empty), `for` (every clause shape, `for true`), `while`, `range` (all shapes, `^:assign`), labels, `break` `continue` `goto`, `(label :L)` | `s07_stmts` |
| §7.9 | `fn` (results, immediately called, deferred), `go`, `defer`, `>!`, `<!`, `panic`, `recover` | `s07_stmts`, `s07_exprs` |
| §8 | integers (big), floats (double, `BigDecimal`, ratios as decimals and hex floats, `-0.0`), `imaginary`, runes (escapes, `(rune n)`), strings (escapes, raw when multi-line, `byte-string`), negative literals | `s08_literals` |
| §9 | attached directives (sorted, `//` after a doc), vectors of a directive, `go:embed` (also in a group), `go:linkname` pull and push, body-less functions with assembly, free-standing directives, `//line` in a body | `s09_directives` |
| §10 | `:line` on lists, symbols, vectors; `:go/breaks` on literals and statements; `:go/end`; header overflow corrected by `//line` | `s10_positions`, `s02_search`, `s14_*` |
| §12.4 | copies with hash check, overlay, `go build -overlay` | test `package-build` |
| §14 | 14.1 (both as the spec writes it and on Go's lines), 14.2 utf8 excerpts, 14.3 sort excerpts, all in the lines layout | `s14_1_sample`, `s14_1_sample_lines`, `s14_2_utf8`, `s14_3_sort` |
| errors | `values` in an expression, a statement in an expression, `let` without vector, bad `set!` operator, declarations before `go/file` | `e01`-`e05` |

Not covered: mode `:full`; directives named other than `//go:` (A6).

## Tests

`bin/g2c-print-tests` builds `tools/gocmp` and `test/g2c/golines` for the host into
`.tmp/g2c-print-tests/bin/` and runs `test/g2c/print_test.clj` on Arbace. Each case
`test/g2c/cases/NAME.clj` is printed in both layouts into `.tmp/g2c-print-tests/NAME/` and:

- `gofmt -l`: both outputs parse; the gofmt layout is gofmt's fixed point (`;; g2c-test:
  nogofmt` waives it, for `s12_printer` only, A10);
- when the original exists (`NAME.go`, the directory `NAME/`, or `original=FILE`), `gocmp`
  compares it with the printed files at the levels tree, export and code (`levels=`
  restricts them), for both layouts;
- for cases marked `lines`, `golines` compares the lines gc sees (after `//line` directives)
  of every declaration, spec, struct field, statement, clause and function closing brace.

Other tests: `package-build` (§12.4), `forms-in-memory` (the converter's path: forms without
lines through `collect-forms`, printed in the gofmt layout, gocmp all levels), `line-file`
(`--line-file`, golines), `spec-coverage`.

Result (2026-10-08, tamago/amd64, go1.27.1): 5 tests, 241 assertions, all pass. The 17 cases
with an original are equal to it at the tree, export and code levels in both layouts; the
gofmt layout of every case but `s12_printer` is gofmt's fixed point; the 5 `lines` cases (and
the `line-file` test) have the original's lines. In the lines layout `s14_3_sort` is
byte-identical to its original; `s02_search`, `s14_1_sample_lines` and `s14_2_utf8` differ
only in ordinary comments and literal spellings; `s10_positions` only in its imports (two
`import` declarations printed as one group, so the next declaration gets `//line :6:1`). A
forms file of 32,000 table elements and 3,000 statements prints in 2.5 s, the JVM's start
included.

## Findings: amendments to SPEC.md (accepted 2026-10-08)

Proposed by the printer; the user accepted all ten on 2026-10-08, with the helper's H1-H7 and
the converter's C1-C11, and they are folded into SPEC.md, marked "(amendment, accepted
2026-10-08)" (SPEC §15, "Amendments (2026-10-08)"). Where they disagreed, the converter's
wins over the printer's over the helper's; the cases are noted below.

- **A1. Field groups in generic declarations.** N3 drops the grouping of names (`a, b T`), but
  gc's output depends on it in generic declarations (ROUNDTRIP.md, known gaps: "must keep the
  field groups of generic declarations"). The printer can only guess (it groups consecutive
  names of equal type on one line, which is right when the source grouped what it could).
  Proposal: the converter marks each name declared together with the name before it with
  `^:go/grouped` (parameters, results, type parameters, struct fields), at least inside
  generic declarations; a file that uses the marker is printed with exactly those groups.
  Implemented (`s05_groups`). *Accepted 2026-10-08, folded into SPEC §5.3 (and §3.2 N3,
  §12.2, §13.1, §14.5).* The converter's form wins: the marker only inside generic
  declarations, plus `^{:go/grouped false}` for names of equal type declared apart; C2 reuses
  it for local `const` groups.
- **A2. `:go/breaks` indexes** are not defined precisely (the example `[17 33 49]` fits no
  obvious counting). Proposal: the index of the child as `nth` gives it, the head being 0;
  and `:go/breaks` applies to every list, statement lists included (a final `nil`, `-1` or
  `"?"` begins its Go line as `return nil`). Implemented so. *Accepted 2026-10-08, folded
  into SPEC §10.1.*
- **A3. `/*line :N*/`** (§12.3) records an empty file name: a line directive without a column
  takes the previous file name only when it has a column (`cmd/compile/doc.go`;
  `syntax/parser.go`, `updateBase`). Proposal: `/*line :N:C*/` (or the file name). The
  printer writes `/*line :N:1*/` and `//line :N:1`. *Accepted 2026-10-08, folded into SPEC
  §12.3.*
- **A4. Conversions** (§12.2): a type beginning with `[` needs no parentheses (`[]byte(s)`;
  go/printer adds them only for function types); channel types are safer with them. Proposal:
  "around conversions to types that begin with `*`, `<-`, `func` or `chan`". *Accepted
  2026-10-08, folded into SPEC §12.2.*
- **A5. `new`'s operand.** `(new (G T))` is either `new(G[T])` (a generic type's instance) or
  Go 1.26's `new(G(T))` (a call's value); the printer cannot tell without declarations. It
  takes such a list as a type (std has `new(atomic.Pointer[T])`, and `new` of a call is new
  and rare). Proposal: a generic type instance in `new`'s operand is written `(inst G T)`
  (which prints `G[T]` in any position) and a list with a plain head there is a call.
  *Accepted 2026-10-08, folded into SPEC §5.5 and §7.7*, extended by the converter to
  `method-expr`'s operand.
- **A6. Directives other than `//go:`** (§9.1 mentions `//export`): keyed `:go/<name>`, they
  would print as `//go:<name>`. Proposal: keep them verbatim, e.g. `:go/directives
  ["//export f"]`. (cgo is excluded for tamago, so none occurs in the corpus.) *Accepted
  2026-10-08, folded into SPEC §9.1* in the converter's spelling, which wins: every directive
  other than a `//go:` line is a free-standing `(go/directive "...")` form where it stands,
  never metadata; the text stays verbatim.
- **A7. Doc comment and directives** (§9.1): gofmt separates a doc comment's text from the
  directives after it by a `//` line, and the converter's line accounting must count it.
  Proposal: "between the doc comment and the declaration, after a `//` line when there is a
  doc comment, as gofmt writes them". Implemented so. *Accepted 2026-10-08, folded into SPEC
  §9.1.*
- **A8. Lines of lists in metadata** (§10.1): the reader gives every list the forms text's
  line, including type forms in tags (`^{:tag (func [int] [bool])} f`), which the printer
  takes as recorded lines; a doc string on its own forms line before a signature therefore
  pushes the signature's types to later lines. The converter must write a declaration's doc
  string, name and signature on the declaration's line (the test forms do). Alternative: lists
  inside metadata never carry Go lines (only explicit `:line` on symbols and vectors would).
  *Accepted 2026-10-08, folded into SPEC §10.1* as the converter does it (doc string, name and
  signature on the declaration's line); the alternative was not taken.
- **A9. A labeled declaration** (`L: x := 1`, legal Go) has no form: `(label :L (let ...))`
  makes the let's body the label's. Rare (none seen); proposal: `(let [^{:go/label :L} x 1]
  ...)` if one turns up. *Accepted 2026-10-08, folded into SPEC §7.1 and §7.3* in the
  converter's C9 form, which wins: `:go/label` on the first target of the `let` binding that
  holds the declaration (`:=` or `var`), 20 in std.
- **A10. gofmt over printed output** (§12.3, §14.4 "comes from running gofmt on the output,
  which keeps the lines"): gofmt keeps lines only where the layout is already gofmt's, and
  gofmt 1.27.1 itself breaks `switch (P[int]{}) {`: its `stripParens` does not count an
  instantiated type as a type name and drops parentheses the parser needs (the output does
  not parse). The printer does the spacing and alignment itself and needs no gofmt pass.
  *Accepted 2026-10-08, folded into SPEC §12.3 and §14.4.*
- The spec's §14.1 forms are illustrative: their lines are not the Go lines (off by one, and
  `:go/end 15` where its own `Move` is on line 14); `s14_1_sample_lines` has the forms on
  Go's lines. *Accepted 2026-10-08 (with the converter's same note), folded into SPEC §14.*

## What remains for the round trip

Once the converter writes forms (§11), the round trip over `$GOROOT/src` and `$GOROOT/test`:

1. A driver: convert each corpus entry, print it into a candidate tree laid out like GOROOT
   (`CANDIDATE/src/<dir>/`, `CANDIDATE/test/`), and run `bin/g2c-check CANDIDATE` (tree,
   export, code). `print-package` and `collect-forms` are the entry points; the overlay and
   `build` are §12.4's for single packages.
2. The converter's layout must follow §10.1 and A2, A7, A8 so that the lines layout gives
   gc the original's lines: then `golines`-style equality holds, and the code level can be
   run with `gocmp -keep-positions` as well.
3. Field groups of generic declarations: A1 (or accept the heuristic's misses, which the tree
   level reports).
4. Mode `:full` (columns, `:go/pos`, `:go/apos`) for byte-identical export data and line
   tables, if the G2 gate keeps it.
5. Comments other than doc comments and directives are not reproduced (§10.4, N8); go/printer
   reformats doc comments (lists, code blocks), which only matters for the doc text, which no
   level compares.
