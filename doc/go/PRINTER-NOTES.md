# repr/go's printer: Go forms → Go

> Names: since 2026-10-17 g2a is repr/go and j2a repr/java (doc/NAMING-SURVEY.md, "Decided"):
> `arbace.g2a.*` is `arbace.repr.go(.*)`, its build `arbace.g2a.build` is `arbace.build.go`;
> `bin/g2a convert` is `bin/repr-go read` (`bin/g2a read`: `read --dump`), `bin/g2a build`
> `bin/build-go`, `bin/g2a-print` `bin/repr-go print`, `bin/g2a-check` `bin/repr-go check`,
> `bin/g2a-convert-tests` and `-print-tests` `bin/repr-go test read` and `test print`,
> `bin/g2a-build-tests` `bin/build-go test`; `G2A_GOROOT` is `ARBACE_GOROOT`, the other `G2A_*`
> `REPR_GO_*`, `J2A_*` `REPR_JAVA_*`; `bin/j2a-check` is `bin/repr-java check`. This document
> uses the current names throughout, its dated passages included; branch names keep theirs.

The printer of milestone G2 ([SPEC.md](SPEC.md) §12): it reads Go forms with the Clojure reader
(`*read-eval*` false, line numbers on) and writes Go source that gc compiles as the original
(§3). It is the second half of the round trip *Go → converter → forms → printer → Go*. It is
proven on hand-written forms (below) and on the converter's forms of all of std and
`$GOROOT/test` ([CONVERTER-NOTES.md](CONVERTER-NOTES.md), "The round trip").

```sh
bin/repr-go print [--layout lines|gofmt|full] [--no-line-directives] [--line-file] \
              [--src DIR [--build]] [--gofmt] FORMS.ae OUTDIR
bin/repr-go test print [REGEX]
```

`FORMS.ae` is a package file (`ns`, `go/package`, `load`s, §4.1) or one file holding
`go/package`, `go/file` and declaration forms. One `.go` file per `go/file` is written into
`OUTDIR`. `--src DIR` (the original package directory) copies `:other-files` and
`:embed-files` (SHA-256 checked) and writes `OUTDIR/overlay.json`; `--build` then runs
`go build -trimpath -overlay` in `DIR` under the forms' `:config` (§12.4). Both scripts run on
Arbace's Go executable (`bin/lib/repr-go-host.bash`: `REPR_GO_EXE`, else the tools' host; on
`target/stage2` until standalone S4); `arbace.repr.go` loaded from the checkout's sources until S6,
compiled into the executable since (SA133), a source in the checkout that differs from the
embedded copy loaded from it instead, evaluated.

## Structure

| namespace | file | role |
|---|---|---|
| `arbace.repr.go.print` | `print.ae` | reading (`read-forms`, `collect` following `load`, `collect-forms` for forms in memory), files (§4.3: build lines, header directives, package doc, imports, declarations), the package build (§12.4), the CLI |
| `arbace.repr.go.print-code` | `print_code.ae` | forms → tokens: classification by head (§4.4, §13.5), types (§5), declarations (§6), statements and expressions (§7), literals (§8), directives and doc comments (§9, §10.4) |
| `arbace.repr.go.print-emit` | `print_emit.ae` | the emitter: tokens, Go's semicolon rule, token separation, indentation, the line of the cursor, placing forms on recorded lines (§10.1, §12.3), `//line` and `/*line*/` corrections |
| `arbace.repr.go.print-text` | `print_text.ae` | literal spellings (strings, byte strings, runes, exact floats, §8.1, §12.2) and a port of `text/tabwriter` (gofmt's alignment) |

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

Three layouts; the default is `full` for a package of `:positions :full`, else `lines` when
the `go/file` form carries a `:line` (it was read from text) and `gofmt` otherwise (forms built
by a program):

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
    A closing brace followed by `else` goes to the line of the `else if` (its recorded line)
    or, before a plain `else` block, to the line before the block's first statement, when
    free. Inside a function body kept on one line nothing breaks the line.
  - Name lists (2026-10-12, BOX-NOTES S4): each name of a parameter, result, type parameter
    or struct field list, and of a spec, goes on its recorded line (a name that begins a Go
    line carries `:line`; Go allows a break after the comma), and a name that begins a line
    starts a group of its own unless `:go/grouped` joins it, since gc declares each name at its
    line (DWARF's `decl_line`). Until then a parameter list's names all went on the list's
    first line and only their types reached their lines, by `/*line*/` directives: code and
    line tables were the original's, DWARF was not (purego's `collectStackArgs`). Outside
    generic declarations names of equal type, doc and tag are grouped (the converter gives a
    field's doc to each of its names), so `h1, h2 T` under a doc comment prints as one field.
    The parameters' `)` stays on the line of the results (the line they record, else the last
    parameter's); the body of a function whose signature spans lines is indented from the
    line `func` began on. The values of a spec or return after the first follow their
    `:go/breaks` too.
  - Expression lists (2026-10-12, BOX-NOTES S4): a case clause's values or types, a return's
    results, a `:=`'s targets, a range's variables and a spec's values keep their recorded
    breaks (`:go/breaks`, or their own `:line`), as calls and composite literals did; a case
    list that begins on the line after `case` goes there. A break (`:go/breaks` index) is the
    line after the last one a form began on (the converter's count, SPEC §10.1), not after
    the cursor's: a closing bracket the layout put on a line of its own does not count, so
    `}, nil` stays one line (before, `nil` went a line further where a return's breaks were
    followed). A call's `)` after a function literal follows its `}` (`})`), as gofmt writes.
    Found by `bin/tamago-vendor check` on the TLS branch: `net/url`'s `validUserinfo`, whose
    `case` list spans two lines, was printed on one, which moved a line of `.gopclntab` (the
    function is inlined into `parseAuthority`).
  - Strings with line breaks are interpreted strings (`"a\nb"`): the forms do not say whether
    the source spelled them raw, and a raw string would push what follows to later lines.
    (The gofmt layout writes them raw where Go allows it.)
  - The label of a labeled declaration (`:go/label`), the keyword of a local `const ( ... )`
    group, and a label before a label, go on the line before their statement when it is
    free.
  - `--line-file` starts the file with `//line FORMS.ae:1`, so that gc's positions name the
    forms file with the forms' lines (§12.3's printer option for hand-written forms).
    The build (`bin/build-go`, [BUILD.md](BUILD.md)) names the forms file relative to the
    package's forms directory, which keeps
    `-trimpath` builds reproducible (amendment B3, accepted 2026-10-08, folded into SPEC
    §12.3).

  The lines layout is gofmt's only when the forms leave gofmt's room (the converter's forms
  do: they have the lines of gofmt'd Go). Running gofmt over it is not safe in general: gofmt
  keeps lines only where the layout is already its own (A10).
- **`full`** (§10.2, §12.3): every token at its recorded line and column; the original text
  (below, "Mode :full as built").

## Coverage

Every form of the spec is printed; `test/repr/go/cases/` covers each, and the test `spec-coverage`
checks that every head of §13.5, the metadata the printer reads and the `go/package` and
`go/file` options occur in a case.

| spec | forms | case |
|---|---|---|
| §2 | the first example (sort.Search), lines layout | `s02_search` |
| §4.1-4.2 | package file, `load` of one file per Go file, `:config`, `:files`, `:other-files`, `:embed-files`, `:init-order`, `:positions`, `:test-files` | `s04_multi`, `s09_directives`, `s04_testpkg` |
| §4.3 | `go/file` `:build`, `:directives`, `:doc`, `:package`, `:imports` (`[name "path"]`, `^:alias`, `_`, `.`, a `go/directive` among them: C4) | `s04_files`, `s04_testpkg`, `s15_roundtrip` |
| §4.4 | `pkg/Name`, dot-imported names, labels, `go/call` (`fn`, `len`), `go/id` (`true`; tagged as a parameter name: C5), heads as plain symbols (`do`, `when`) | `s04_files`, `s15_roundtrip` |
| §5.1-5.2 | `* slice array map chan func struct interface \| tilde inst`, `(G T...)`, `(array ... T)`, `chan (<-chan T)`, `unsafe/Pointer` | `s05_types`, `s07_exprs` |
| §5.3 | parameters, unnamed and variadic (`&`), single and named results, results vectors of function types | `s05_types`, `s06_decls`, `s05_groups` |
| §5.4 | fields, embedded (also `*T`, generic), `:go/tag`, `:doc`, interface methods, embedded interfaces, type sets | `s05_types`, `s10_positions` |
| §5.5 | `:type-params` (constraints, trailing comma `[P *int,]`), generic receivers, generic methods (Go 1.27), `inst`, `:inst`; field groups of generic declarations (`^:go/grouped`, A1) | `s05_types`, `s06_decls`, `s07_exprs`, `s12_printer`, `s05_groups` |
| §6 | `go/type` (alias, generic alias, groups, a group of one), `go/const` (`values`, implicit repetition, `:val`), `go/var` (no init, `nil` init, `values`, groups), `go/func`, `go/method` (unnamed receiver, `^:extern`, `init`, implicit return, `panic`), doc comments | `s06_decls`, `s09_directives` |
| §7.1-7.4 | `let` (`:=`, `values`, `^:assign`, `var` with `zero`, `^:var`, `^:const`; a local `const` group, `^:go/grouped`, `^:go/implicit`: C2; `:go/label`: C9), `let-type` (`^:alias`; a local generic type's `:type-params`: C6), `set!` (3 shapes, the 11 operators), `inc!`, `dec!`, `aset`, `do` | `s07_stmts`, `s14_5_amended`, `s15_roundtrip` |
| §7.5 | every operator, n-ary chains, precedence and parentheses, unary combinations, `^:go/paren` operands of `and` and `or` (C3) | `s07_exprs`, `s12_printer`, `s14_5_amended`, `s15_roundtrip` |
| §7.6-7.7 | `.-f`, `.M`, field calls, method values and `method-expr`, `addr`, `@`, `lit` (positional, keyed, map, elided `_`, `&T{}`, `[...]T`, promoted keys), `aget`, `subslice` (all shapes), `conv`, `assert` (comma-ok), `spread`, every builtin, `new` of a value, `unsafe` | `s07_exprs`, `s12_printer` |
| §7.8 | `if` `when` `cond` (`else if`, `else { if }`), init vectors of every kind, `switch` (init, tag, `default` anywhere, `fallthrough`, empty), `type-switch` (guard forms, init), `select` (every comm, empty), `for` (every clause shape, `for true`), `while`, `range` (all shapes, `^:assign`), labels (nested too), `break` `continue` `goto`, `(label :L)` | `s07_stmts`, `s15_roundtrip` |
| §7.9 | `fn` (results, immediately called, deferred), `go`, `defer`, `>!`, `<!`, `panic`, `recover` | `s07_stmts`, `s07_exprs` |
| §8 | integers (big), floats (double, `BigDecimal`, ratios as decimals and hex floats, `-0.0`), `imaginary`, runes (escapes, `(rune n)`), strings (escapes, raw when multi-line in the gofmt layout, `byte-string`), negative literals | `s08_literals` |
| §9 | attached directives (sorted, `//` after a doc), vectors of a directive, `go:embed` (also in a group), `go:linkname` pull and push, body-less functions with assembly, free-standing directives, `//line` in a body | `s09_directives` |
| §10 | `:line` on lists, symbols, vectors; `:go/breaks` on literals and statements; `:go/end`; header overflow corrected by `//line` | `s10_positions`, `s02_search`, `s14_*` |
| §12.4 | copies with hash check, overlay, `go build -overlay` | test `package-build` |
| §14 | 14.1 (both as the spec writes it and on Go's lines), 14.2 utf8 excerpts, 14.3 sort excerpts, all in the lines layout; 14.5 the amended forms | `s14_1_sample`, `s14_1_sample_lines`, `s14_2_utf8`, `s14_3_sort`, `s14_5_amended` |
| errors | `values` in an expression, a statement in an expression, `let` without vector, bad `set!` operator, declarations before `go/file` | `e01`-`e05` |

Mode `:full` is covered by the converter's test `positions-full-text` (the spec's sample and
search and the unit files of each section, test/repr/go/convert/, converted with `--full` and
printed back byte for byte) and by the text level of the round trip (below). Directives not
spelled `//go:` are free-standing `go/directive` forms (A6 as accepted), covered by
`s09_directives`.

## Tests

`bin/repr-go test print` builds `tools/gocmp` and `test/repr/go/golines` for the host into
`.tmp/repr-go-print-tests/bin/` and runs `test/repr/go/print_test.ae` on Arbace. Each case
`test/repr/go/cases/NAME.ae` is printed in both layouts into `.tmp/repr-go-print-tests/NAME/` and:

- `gofmt -l`: both outputs parse; the gofmt layout is gofmt's fixed point (`;; repr-go-test:
  nogofmt` waives it, for `s12_printer` only, A10);
- when the original exists (`NAME.go`, the directory `NAME/`, or `original=FILE`), `gocmp`
  compares it with the printed files at the levels tree, export and code (`levels=`
  restricts them), for both layouts;
- for cases marked `lines`, `golines` compares the lines gc sees (after `//line` directives)
  of every declaration, spec, embedded struct field, statement, clause and function closing
  brace, and of every name a field list or a spec declares (since 2026-10-12: case
  `s10_names`, the names of lists that span lines), and of every element of a case clause's
  list, a return's results and an assignment's sides (case `s10_breaks`).

Other tests: `package-build` (§12.4), `forms-in-memory` (the converter's path: forms without
lines through `collect-forms`, printed in the gofmt layout, gocmp all levels), `line-file`
(`--line-file`, golines), `spec-coverage`.

Result (2026-10-08, tamago/amd64, go1.27.1): 5 tests, 256 assertions, all pass (with the cases
`s14_5_amended` and `s15_roundtrip` of the round trip's forms; `spec-coverage` also checks the
metadata of the amendments: `:go/grouped`, `:go/implicit`, `:go/label`, `:go/paren`,
`:type-params`). The 19 cases with an original are equal to it at the tree, export and code levels
in both layouts; the gofmt layout of every case but `s12_printer` is gofmt's fixed point; the 5
`lines` cases (and the `line-file` test) have the original's lines. In the lines layout `s14_3_sort`
is byte-identical to its original; `s02_search`, `s14_1_sample_lines` and `s14_2_utf8` differ only
in ordinary comments and literal spellings; `s10_positions` only in its imports (two `import`
declarations printed as one group, so the next declaration gets `//line :6:1`). (Since
2026-10-11 the originals of `s02_search` and `s14_3_sort` carry Go's copyright notice, as
`s14_2_utf8`'s did, which the forms hold as `;;` lines (SPEC §10.1) and the printer does not
print: `s14_3_sort` now differs from its original in that comment only.) A forms file of
32,000 table elements and 3,000 statements prints in 2.5 s, the JVM's start included.

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

## The round trip

Closed (milestone G2, 2026-10-08): `bin/repr-go roundtrip` converts std and `$GOROOT/test`, prints them
in the lines layout and checks them with `bin/repr-go check` at the tree, export and code levels against
a recorded reference; std (373 packages on amd64, 372 on arm64) and the `$GOROOT/test` programs
(1,707 and 1,702) pass every level on both architectures, and so does std with its tests apart from
nine known differences (embedded sources). For `GOOS=linux` (B1a, 2026-10-08) std (376 and 374
packages) and the programs (1,713 and 1,707) pass every level too (`bin/repr-go roundtrip linux/amd64`,
`linux/arm64`), with no change to the printer. The table, times and what it took are in
[CONVERTER-NOTES.md](CONVERTER-NOTES.md), "The round trip".

What remains:

1. Mode `:full` is printed since 2026-10-16 (below). The oracle's levels still compare no
   positions; with the full layout they could (`gocmp -keep-positions`).
2. In mode `:lines` comments other than doc comments and directives are not reproduced
   (§10.4, N8); go/printer reformats doc comments (lists, code blocks), which only matters for
   the doc text, which no level compares. Mode `:full` reproduces all of them.

## Mode :full as built (2026-10-16)

At the user's request (2026-10-16): Go forms converted with `--full` print back to the original
Go text. `bin/repr-go read --full ...` then `bin/repr-go print FORMS.ae OUT` (the full layout is the
default for such a package) gives each file byte for byte.

**What the converter adds.** The node positions of §10.2 do not hold the whole text, so mode
`:full` writes more, only in that mode (SPEC §10.2, "As built", lists the keys): every comment
as the source spells it, the source's semicolons and trailing commas, its white space beyond
gofmt's rule, the file's tail; literal spellings where the printer would spell the value
otherwise; parentheses; the inner operators of chains; `cond`'s branches; and the positions
the forms spliced away (implicit returns, struct and interface braces, receivers' and type
parameters' brackets, labels, key colons, the type switch's guard, qualified names, local and
import declarations' keywords and parentheses). The converter reads the Go files themselves
for the text (`:text-dir`, from `arbace.repr.go`: the package's directory, or `$GOROOT/test`'s
for a program). Two shapes differ from `:lines`' in this mode: a chain does not continue
through parentheses, and `-(1)` stays a negation of a parenthesized literal. The `:lines`
output is unchanged.

**What the printer does** (`print-file-full`, `print-emit`'s full mode). The forms are read as
in the other layouts; `print-code/wrap-full` replaces each literal child that has a position
by a record of its value, position, spelling and parentheses. The forms are then printed by
the same code as the other layouts, with each token given its position (`e/at!`; names,
keywords, brackets, operators); the emitter collects the tokens instead of writing them, with
separators it cannot decide (`e/soft`: a `;` between statements, a trailing comma) and
whether gofmt would put a blank before each. Comment-lines, doc comments, directives and
`//line` corrections are not written: the file's comments are data. At the end
(`full-result`) the comments, semicolons and trailing commas are merged in by position, the
printer's own semicolons and trailing commas dropped, and the tokens laid out: a positioned
token at its line and column, white space as tabs at the start of a line and spaces elsewhere
unless `:go/space` records the source's run, and an unpositioned token (a comma between
elements, a spec's `=`, `else`, a slice's colon, `type` in `.(type)`) after the token before it
as gofmt spaces it; when the next positioned token is on its line, the room between them is
shared: blanks beyond gofmt's go before the first token gofmt separates by a blank (an aligned
`=`), missing ones are dropped from the left, and a run goes after a block comment when there
is no room before it (`f(x /* c */, y)`). Grammar parentheses are not added (the source's
are recorded), names of equal type are grouped only when they share the type node, and a local
`var ( ... )` or `type ( ... )` group is printed as one.

**The text level** (SPEC §3.1): `bin/repr-go roundtrip --text [[GOOS/]ARCH] [--tests]` converts
the round trip's corpora with `--full`, prints them into `.tmp/repr-go/cand-text-...` and compares
each printed file with its original byte for byte (`arbace.repr.go --text`, no build); the
known differences, with their reasons, are test/repr/go/roundtrip-text-known.edn, shared by the
configurations. Results (2026-10-16, go1.27.1):

| configuration | std (packages, files: byte-identical) | `$GOROOT/test` programs (files: byte-identical, known) | time |
|---|---|---|---|
| tamago/amd64 | 379, 1,742: 1,742 | 1,708: 1,705, 3 | 156 s |
| tamago/arm64 | 378, 1,738: 1,738 | 1,703: 1,700, 3 | 168 s |
| linux/amd64 | 382, 1,834: 1,834 | 1,714: 1,711, 3 | 168 s |
| tamago/amd64 with tests (`--tests`) | 554, 2,935: 2,935 | | 179 s |

The forms of mode `:full` are about 3.4 times those of `:lines` (std on tamago/amd64: 103.9 MB
against 30.7 MB), and converting std takes about twice as long (124 s against 61 s). The `:lines` output is unchanged: `bin/repr-go roundtrip amd64` and
`linux/amd64` show no regression against their references, the converter's and printer's tests
pass, and the Go printed from `tamago/`'s, `tamago/cmd/`'s and `box/`'s forms (2,504 files) and
a2g's program (`bin/build-go --print-only`, 1,572 files) is the same byte for byte before and
after.

The 3 known differences are the 3 programs with an empty declaration group (`import ( )` in
`test/empty.go`, a local `var ()` in `fixedbugs/issue49611.go` and `typeparam/issue49611.go`):
an empty group declares nothing, and the forms have no element to hold it (G2F6).

Found on the way: ordinary semicolons are in no node (`x := 1;` in the old tests, `if ; c`),
nor are trailing commas (`[]int{1, 2, }`, `int(1.0,)`); a comment between a token and an
unpositioned comma (`crypto.Hash(0) /* no pre-hashing */, false`); labeled declarations'
labels; a negative literal written `-(1e9)` (`encoding/json/v2`'s tests).

**Proposals** (the user's decisions, built as recommended; not yet accepted):

- **G2F1. Comments as data in mode `:full`.** Every comment of a file is `go/file`'s
  `:go/comments [[line col text] ...]`, as the source spells it (doc comments and directives
  too), and the printer takes all comments from there; the `;` lines stay for reading.
  Alternative: read the `;` lines from the forms text, which lose columns, `/* */` against
  `//`, and the spelling of doc comments. Recommended: data.
- **G2F2. The source's text beyond positions.** White space runs other than gofmt's
  (`:go/space`), the tail (`:go/tail`), semicolons (`:go/semis`) and trailing commas
  (`:go/trails`), read from the source by the converter. Alternative: record every comma's
  position too (no placement rule for commas; larger forms), or have the helper emit the token
  stream. Recommended: as built (the corpora need nothing more).
- **G2F3. The converter reads the Go files** in mode `:full` (`:text-dir`), since the dump has
  no source text. Alternative: the helper (`tools/godump`) adds the text's data to the dump.
  Recommended: the converter (the helper stays dumb; without the files, comments come from the
  dump and the rest is missing, and the text is not the original's).
- **G2F4. Shapes that differ from `:lines`.** In mode `:full` a chain stops at parentheses
  (`(a + b) + c` is `(+ ^{:go/parens ...} (+ a b) c)`) and `-(1)` is `(- 1)` with its
  parentheses. Alternative: keep `:lines`' shapes and record the parentheses of sub-chains
  apart. Recommended: as built (gc's tree is the same; the forms stay simple).
- **G2F5. Literal spellings only where they differ** from the printer's spelling of the value
  (`:go/atext`, `:go/text`); struct tags always (`:go/tag-text`). Alternative: always.
  Recommended: as built (most literals need nothing).
- **G2F6. Empty declaration groups** (`import ( )`, a local `var ()`) stay unrepresented and are
  known differences (3 programs). Alternative: a form for them (`(go/var)` with no specs as a
  statement, an empty `:imports` decl marker). Recommended: as built.
- **G2F7. The text level stays outside the gate.** `bin/repr-go roundtrip --text` takes about 3
  minutes per configuration and no build. Alternative: add `bin/repr-go roundtrip --text` (amd64)
  to `bin/gate --full`, beside the round trip's checks. Recommended: add it to `--full`.
- **G2F8. Keys and names.** The new metadata keys of SPEC §10.2 ("As built"), the layout name
  `full` and `bin/repr-go roundtrip --text`. Recommended: as built.

**Decided (2026-10-17, the user):** G2F1-G2F6 and G2F8 (a), as built; G2F7 against "stays
outside": `bin/repr-go roundtrip --text amd64` in `bin/gate --full` (its repr/go step), as recommended.
