# g2c's converter: notes

`arbace.g2c` turns the helper's typed syntax tree of a Go package ([HELPER-NOTES.md](HELPER-NOTES.md),
format `:godump 2`) into Go forms ([SPEC.md](SPEC.md)), milestone G1 of
[G2C-SURVEY.md](../G2C-SURVEY.md) §7. As in j2c ([classes/CONVERTER-NOTES.md](../classes/CONVERTER-NOTES.md)),
the front end is dumb and every decision about forms is made here, in Clojure. The forms are
printed back to Go by [the printer](PRINTER-NOTES.md); the round trip is judged by the oracle
([ROUNDTRIP.md](ROUNDTRIP.md)).

## Running

```sh
bin/g2c convert fmt sort              # dump (godump) and convert into .tmp/g2c/forms/pkgs
bin/g2c convert --full --tests fmt    # mode :full; with the _test.go files and fmt_test
bin/g2c convert --corpus amd64        # a corpus of `bin/g2c corpus` (amd64, arm64, test,
                                      # tests) into .tmp/g2c/forms/CORPUS, report in
                                      # .tmp/g2c/forms/CORPUS.report.edn
bin/g2c convert --corpus test --print DIR   # and print each package into the candidate DIR
bin/g2c roundtrip [amd64|arm64]       # convert + print std (and $GOROOT/test on amd64) into
                                      # .tmp/g2c/cand-ARCH, then bin/g2c-check
bin/g2c print ...                     # the printer (bin/g2c-print)
bin/g2c test                          # the converter's tests (bin/g2c-convert-tests)
```

The tools run on Arbace's `target/stage2` (`bin/lib/tools.bash`); `arbace.g2c` loads from
source. Conversion runs one package per task on a thread pool (`G2C_JOBS`, default half the
cores; heap `G2C_JVM_OPTS`, default `-Xmx12g`). Every file written is read back with Arbace's
reader (`LineNumberingPushbackReader`, `*read-eval*` false) and compared with the forms in
memory: data, every metadata key, and, in mode `:lines`, the line of every list (the reader's
`:line` or an explicit one) against the Go line it stands for. A difference fails the run.

## Structure

| namespace | role |
|---|---|
| `arbace.g2c.convert` | the converter: nodes → forms. Files (`convert-file`: imports, header directives, directive placement, comments), declarations (`func-decl`, `gen-decl`), statements (`stmts-forms`: the `let` scoping of §7.3, `stmt`, `if-form`, clauses), expressions (`expr`, `call`, `selector-form`, `composite`, `instantiation`), types from the source (`type-form`, `fields`, `signature`, `type-params`), and the package form |
| `arbace.g2c.types` | type forms from the helper's type table (for `:inst` and `:tag`), qualification by import name or namespace (§4.4), constant kinds |
| `arbace.g2c.lit` | literals (§8.1) and constant values (§8.2): integers with their radix, exact floats (double, `BigDecimal`, ratio), runes, strings and `byte-string`, Go's UTF-8 validity |
| `arbace.g2c.layout` | the forms text: mode `:lines` places every form on its Go line (§10.1), mode `:full` writes `:go/pos`/`:go/apos` (§10.2); ordinary comments as `;` comments |
| `arbace.g2c.main` | the command line, the read-back check, coverage counts, the `--print` hook into `arbace.g2c.print` |
| `arbace.g2c.read` | reading dumps (the helper's, extended) |

In memory a form is Clojure data: lists, vectors and symbols carry the spec's public metadata
and two private keys, `:arbace.g2c.convert/l` (the Go line the form begins on) and
`.../pos` (its node's position fields, for `:full`). Literals are `arbace.g2c.lit/Lit`
records: the text written (`0x2A`, `2r1010`, `1E+400M`) and the value read back. The layout
first decides the breaks (a form whose Go line is after everything written before it starts
that line), then writes the text; `bin/g2c convert` checks that reading the text gives the
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
- **Files.** `x.go` is `<pkg-dir>/x.clj`, except when the package's source directory has a
  subdirectory `x` (a package whose own file would be `x.clj`): then `x.go.clj` (proposal
  C7; 9 files in std, e.g. `runtime/debug.go`).
- **Single-file programs** (`$GOROOT/test`) keep their import path
  `command-line-arguments` in `:path`; their namespace is `go.test.` plus the file's path
  (`fixedbugs/bug257.go` → `go.test.fixedbugs.bug257`).
- **Package form.** `:config` from the dump (`:go-version` as `:lang`); `:other-files` the
  sorted union of the helper's non-Go lists (`:s-files`, `:h-files`, `:syso-files`, and the
  cgo-only lists, empty for tamago); `:embed-files` and the tests' embed files; `:test-files`
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
  `-2i` stay `(- ...)`.
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

## Deviations from the spec: proposed amendments

The converter follows these; none is accepted yet. The printer's A1-A10 are in
[PRINTER-NOTES.md](PRINTER-NOTES.md) (the converter applies A1, A2, A5, A8, A9). The helper's
author's notes, applied: type assertions carry `:dot`; constant representation from `:t`;
`:go/via` from the helper's `:via`; file lists merged into `:other-files`; `p_test` →
`go.<path>_test`; gc attaches `//go:` lines across blank lines (verified, C1).

- **C1. Directives attach across blank lines** (§9.1 says "no blank line or other code
  between"): gc's parser keeps pragmas from the end of one declaration to the next
  (`syntax/parser.go`, `clearPragma` after each top-level declaration, `takePragma` at the
  next), so a blank line does not detach them. The forms follow gc.
- **C2. Local const groups** (N6 says local groups become one declaration per spec): gc and
  the oracle keep a `const ( ... )` group of several specs (iota and implicit repetition
  depend on it; ROUNDTRIP.md, normalization 4). The bindings of a local group after the
  first carry `^:go/grouped`, and a spec without init (its type and init written out, as N6
  says) `^:go/implicit`; the printer can then print the group as written. 79 groups in std.
- **C3. Parentheses around `&&` and `||` operands** (N1 removes all parentheses): gc removes
  dead branches by syntax and does not look through parentheses (ROUNDTRIP.md, "logical";
  `bytes.IndexRune` and `strings.IndexRune` compile differently without them). The operand
  form gets `^:go/paren`, and a parenthesized operand is not merged into the chain.
- **C4. Directives among the imports** are `(go/directive "...")` elements of `:imports`,
  in order (`crypto/internal/fips140/mlkem`'s `//go:generate` between the package clause and
  the imports).
- **C5. `(go/id "x")` as a name.** In parameter, field and binding positions a tagged
  `(go/id "true")` is a name like a tagged symbol (`text/template/parse`'s parameter `true`).
- **C6. Local generic types** (`$GOROOT/test`, and std's tests): a `let-type` name carries
  `:type-params [...]` as metadata, keeping the vector's pairs.
- **C7. File-name collisions**: `x.go.clj` when the package directory has a subdirectory
  `x` (above).
- **C8. `(binary-float M E)`** for constant values beyond a rational's range (above).
- **C9. `:go/label` (the printer's A9)**: a labeled `:=` or `var` (`L: x := 1`, 20 in std,
  e.g. `internal/runtime/maps`) is `^{:go/label :L}` on the first target of its `let`.
- **C10. Multi-name `var` without type**: `var a, b = f()` in a function is
  `^:var (values a b) (f)`, the marker on the `values` form.
- **C11. `:full` positions of spliced nodes.** A function form carries its `func` keyword
  and its body's braces (`:func`, `:lbrace`, `:rbrace`); a parameter or result vector its
  field list's `:opening`/`:closing`; `when`, loops, `switch`, `type-switch` and `select`
  their body's braces; `if` its then block's and `:else-lbrace`/`:else-rbrace`; a selector
  also its name (`:name-pos`) and a qualified symbol `:x-pos`. A binding target carries its
  statement's `:tok-pos`. Lost: the positions of parentheses (N1), of the `return` keyword of
  an implicit return, of a merged chain's inner operators, and of literals beyond their own
  (`:go/apos` holds one position per child).
- **The spec's text.** §8.2 calls `1.0E100` inexact, but by §8.1's rule the double text
  `1.0E100` denotes exactly 10^100, so it is written as a double. §14.2 shows `:val 239` for
  `runeErrorByte0`, an untyped rune (`t3 | RuneError>>12`), which §8.2 writes as a character
  (`\ï`), and `:doc` for acceptRange's line comments. §14.1's `:go/end` and lines are
  illustrative (the printer's notes say the same). The examples' docs are shortened.

## Coverage

Every `go/ast` node kind of §13.1 that occurs is converted (`Bad*` never occurs: the helper
reports type errors and there are none). `bin/g2c convert` counts the nodes it consumes per
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
| `$GOROOT/test`, tamago/amd64 | 1,705 | 1,706 | 9.8 MB | 2.6 s | 0 | 0 |
| std with tests (`-tests`), amd64 | 554 | 2,935 | 49.2 MB | 10.6 s | 0 | 0 |
| std amd64, mode `:full` | 379 | 1,742 | 86.4 MB | 10.3 s | 0 | 0 |
| `$GOROOT/test`, mode `:full` | 1,705 | 1,706 | 20.2 MB | 3.2 s | 0 | 0 |

The JVM's start adds about 2 s. Measured with `-Xmx16g`, the process peaked at about 14 GB
(the heap grows to its allowance; runtime's dump alone is 24.8 MB of text); the default is
now `-Xmx12g`.

Two runs give byte-identical forms trees (checked on std amd64).

## The round trip

`bin/g2c roundtrip` converts, prints with `arbace.g2c.print/print-package` (lines layout)
into a candidate tree and runs `bin/g2c-check` (tree, export, code):

| corpus | entries | tree pass/fail/err | export pass/fail/err/skip | code pass/fail/err/skip |
|---|---:|---|---|---|
| std, tamago/amd64 | 373 | 336 / 37 / 0 | 359 / 13 / 0 / 1 | 355 / 17 / 0 / 1 |
| `$GOROOT/test`, tamago/amd64 | 1,705 | 1,660 / 18 / 27 | 1,667 / 11 / 27 / 0 | 1,666 / 12 / 27 / 0 |
| std, tamago/arm64 | 372 | 337 / 35 / 0 | 358 / 13 / 0 / 1 | 354 / 17 / 0 / 1 |

(2026-10-08; `unsafe` has no export data or code.)

What fails is, as far as the first difference of each entry shows, on the printer's side:
it does not yet print the converter's markers or some forms.

| first difference (amd64, std and tests) | entries | side |
|---|---:|---|
| a local `const ( ... )` group printed as separate declarations (C2) | 35 | printer: `:go/grouped`, `:go/implicit` on `let` bindings |
| a labeled `:=`/`var` printed without its label (C9, A9) | 11 | printer: `:go/label` |
| a local generic type without its type parameters (C6) | 4 | printer: `:type-params` on `let-type` names |
| `&&`/`\|\|` operands without their parentheses: gc's code differs (C3) | 2 | printer: `:go/paren` |
| `//go:generate` among the imports (C4) | 1 | printer: `go/directive` in `:imports` |
| a parameter named `true` (C5) | 1 | printer: tagged `go/id` |
| `switch {; case ...}` clauses on one line do not parse | 3 | printer |
| not printed: nested labels `(label :A (label :B s))` (4), `-0x8000000000000000` and similar ("long overflow", 3), an empty `(go/const)` group (1) | 8 | printer |
| not dumped: excluded by build constraints for the helper, compiled by the oracle | 19 | corpus lists (helper/oracle) |

## What remains

- The printer's support of C2-C6, C9 (and the nested labels, `-0x8000000000000000`, empty
  groups and one-line clause lists it rejects), then the round trip to zero failures;
  `gocmp -keep-positions` for the lines.
- The 19 `$GOROOT/test` programs that `bin/g2c-check` compiles but the helper excludes by
  their build constraints (`chanlinear.go`: `darwin || linux`; `go list F.go` does not apply
  constraints to named files), and 6 more the helper leaves to other configurations: the two
  corpus lists should agree.
- Mode `:full` is written but not consumed by the printer yet.
- The spec decisions above (C1-C11).
