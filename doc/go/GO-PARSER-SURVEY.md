# repr/go without Go's toolchain: an Arbace-native Go reader — survey and plan

Status: survey and plan, 2026-10-20, at the user's request ("write a survey and plan for an
Arbace-native Go parser, so that repr/go reads Go without Go's own toolchain"; a plan only,
nothing built). Nothing here is adopted. The decisions are the user's, as proposals GPS1-GPS11
(§10). The milestones are GP1-GP8 (§8). Neither prefix is used elsewhere in the repository
(`#GP` in doc/go/PROCESS-SPEC.md is x86's general protection fault).

Base: branch `go-parser-survey`, from main `9a6a36e9`. The tools' host is that commit's stage 0
(`bin/seed --build`, `target/seed/amd64/arbace`, 143,749,280 bytes). The toolchain is TamaGo's
go1.27.1 (`/root/tamago-go`, `VERSION` go1.27.1, 2026-08-28). The machine is the shared 64-core
amd64 host, with a load average of about 15 during the runs. Native times are the best of 3 to 5
runs. The scratch is `.tmp/gps/` in the worktree, not tracked. Appendix A lists the experiments.
Nothing of repr/go, goi, the checker or the helper was changed.

## Summary

- **Today.** repr/go reads Go only through the helper `tools/godump`, a Go program of 2,801
  lines. It runs `go list -e -json -export -deps` to get the files and the dependencies' export
  data. Each package is parsed with `go/parser` and type-checked with `go/types`, then written
  as one dump. The dumps are big: std for tamago/amd64 is 19.6 MB of Go and 252.7 MB of dump.
  The converter (`arbace.repr.go.convert`) reads the dump back and makes the Go forms (§1).
  - The converter needs go/types' decisions: call or conversion, field, method or package
    member, constant values, `:tag`, `:inst`, embedded paths, `^:assign`, implicit import
    names, init order (SPEC §11.1).
  - In mode `:full` it also reads the Go files' bytes itself, for comments as written, white
    space, semicolons and trailing commas.
- **Where the helper is needed:** `bin/repr-go read`, `corpus` and `roundtrip`; the gate's
  round trips; goi's check and annotation check (their corpora are converted by it); the
  vendoring of `tamago/`, `tamago/cmd/`, `box/` and the x509 roots. The box cannot read Go
  today. It has no go command, no export data and no helper. It prints forms and runs gc from
  forms, but it never reads Go text (§1.5).
- **What fails today:** 134 of goi's errorcheck programs are unsupported.
  - 70 have syntax errors, so go/parser gives no tree and repr/go reads nothing.
  - 64 are invalid in a way the converter cannot represent (GC47).
- **Prior art (§2).** Two Go parsers are relevant, and each wins on one measure:
  - `go/parser` (3,821 lines with its resolver and interface) is the only one whose tree is the
    dump's. The dump is go/ast, field by field, and only go/parser keeps every comment and every
    go/ast position.
  - gc's `cmd/compile/internal/syntax` (7,533 lines) has gc's error messages and recovery. Run
    on the 70 syntax-error programs, under the test driver's judge, **syntax's errors pass 66 of
    70, go/parser's 20 of 70** (experiment E1).
  - Over all 3,400 `.go` files of `$GOROOT/test`, the two parsers disagree on 16 files. 14 are
    branch and label checks that go/types makes as well. In 2, syntax parses what go/parser
    rejects (E2).
  - Joker (a Clojure in Go) and yaegi are not Go parsers that Arbace could use; they confirm the
    shape of a hand-written reader and of go/parser's reuse.
- **Both parsers are already Arbace forms**, and goi runs them.
  - The two packages: `go/parser` in `tamago/goroot/go/go/parser/`, gc's `syntax` in
    `tamago/cmd/go/cmd/compile/internal/syntax/`.
  - Interpreted by goi on stage 0, each parses all of std (1,742 files, 19.6 MB) in 26.1 to
    26.2 s, 0.75 MB/s (E3). That is about 70 times slower than compiled (52.5 MB/s, 0.37 s).
  - **The interpreted go/parser gives the native one's trees.** The node counts are the same, file
    by file, over all of std (4,844,260 nodes).
  - A byte loop written in Arbace runs at 1.4 to 1.6 MB/s (E4). So a parser hand-written in
    Arbace would run at about the interpreted speed, with nothing gained in fidelity.
- **Recommendation (GPS1-GPS4).**
  - Read the tree with Go's own `go/parser`, run from `tamago/goroot`'s forms: first by goi,
    interpreted (no build change), then compiled into the executable through goi's table (GP7,
    estimated +0.36 MB; E5).
  - Use a dumper generated from go/ast's forms that builds the dump's tree as Arbace data, with
    no text in between.
  - Take syntax errors in gc's words from gc's `syntax`, also run from forms.
  - Take the annotations from Arbace's own checker, `arbace.go.check`. It already computes every
    one of them, and verifies them over 121,345 declarations of five corpora with 0 errors. The
    path:
    - The converter writes **raw forms**, which leave the typed decisions open.
    - The checker resolves them and records its answers.
    - The answers are put into the tree in the dump's own shape.
    - The converter, unchanged, makes the forms.
  - A hand-written Arbace parser (option (a), about 3,500 lines, 2 to 3 weeks) stays an
    alternative if the user wants the parser in Arbace's Clojure dialect. It reads no better.
- **Switching is checkable at three levels** over every corpus (§6):
  - E-tree: the trees are equal as data to the helper's dumps, annotations stripped;
  - E-ann: the annotations the converter reads are equal to go/types', types compared by
    structure;
  - E-forms: the `.ae` files are byte for byte the same, in mode `:lines` and mode `:full`.
  `bin/tamago-vendor verify` and `bin/toolchain-vendor verify` then hold by construction.
- **Speed (§7).** The helper itself is fast: 8.5 s alone for std, 1.0 s at `-j 64`. The cost is
  reading its text back: 7 min 54 s of CPU for std's 252.7 MB of dumps, about 60% of the
  conversion's 13 min 9 s.
  - The native path writes no text. It adds the checker (80 CPU-s for std) and a second
    conversion.
  - Estimated total: about 12 CPU-min interpreted, about 6 to 7 compiled with one conversion.
    Today it is about 13.
- **Milestones (§8):**

  | milestone | content | days |
  |---|---|---:|
  | GP1 | the tree | 3-4 |
  | GP2 | the loader | 3-4 |
  | GP3 | raw forms | 3-4 |
  | GP4 | the annotator | 5-7 |
  | GP5 | the forms equal, and the switch | 2-3 |
  | GP6 | syntax errors in gc's words, and invalid programs for goi | 2-3 |
  | GP7 | compiled | 3-4, and one re-seed |
  | GP8 | the box reads Go | 2-3 |

  About 23 to 32 days in all. The helper stays as the oracle in `bin/gate --full`, as the frozen
  jar stays for Java.

## 1. How repr/go reads Go today

### 1.1 The pipeline

```
Go files ──go list──▶ files, export data ──go/parser, go/types──▶ dump (.edn, text)
                                                                   │ arbace.repr.go.read
                                                                   ▼
             forms (.ae) ◀──layout── Go forms (data) ◀──convert── dump (data)
                                        ▲
                       mode :full: the Go files' bytes (source-text)
```

- **The helper** is `tools/godump`: `main.go`, `load.go`, `dirs.go`, `dump.go`, `types.go`,
  `view.go` and `write.go`, 2,801 lines (doc/go/HELPER-NOTES.md).
  - It is built with TamaGo's go for the host. It checks that it was built with go1.27 and that
    the go command is go1.27.1.
  - Loading: one `go list -e -json -export -deps` gives each package's files for the
    configuration (GOOS, GOARCH, tags, `CGO_ENABLED=0`) and every dependency's export data.
  - Parsing: each file with `parser.ParseComments|SkipObjectResolution`.
  - Checking: `types.Config` with `importer.ForCompiler(fset, "gc", lookup)` over go list's
    export data, and `types.SizesFor("gc", GOARCH)`.
  - Writing: one `.edn` per package (format `:godump 2`). `-tests` adds the test variants and the
    external test packages. `-files` handles single-file programs, `-dirs` the directory tests of
    `$GOROOT/test` (their action lines, local imports and runindir modules, read as Go's test
    driver reads them).
- **The reader and the converter** are `arbace/repr/go/read.ae`, `convert.ae`, `types.ae`,
  `lit.ae` and `layout.ae`, 2,713 lines (doc/go/CONVERTER-NOTES.md). They run on the tools' host:
  `read-dump` reads the text with `*read-eval*` false, `convert-package` makes the Go forms, and
  `layout` writes them as `.ae`.
  - Mode `:lines` (the default) keeps statements' lines.
  - Mode `:full` keeps every position plus the text, so that the printer gives the original files
    back byte for byte (SPEC §10.2).
    - It takes the comments, white space, semicolons, trailing commas and the tail from the Go
      file's bytes (`source-text`, convert.ae §"The source text").
    - It takes the literals' spellings from the dump, and the keyword `chan` of `<- chan T` from
      the source (`recv-chan-pos`).
- **Commands:**
  - `bin/repr-go read [--full] [--tests] [--goos G --goarch A] PKG...` dumps and converts.
  - `bin/repr-go read --corpus C` converts a corpus of `bin/repr-go corpus C` (std and
    `$GOROOT/test` for four configurations, std with tests, directory tests, error tests).
  - `bin/repr-go roundtrip` converts, prints and compares (doc/go/ROUNDTRIP.md).

### 1.2 What the dump carries

| part | from | used by the converter for |
|---|---|---|
| `:files` / `:ast`: every go/ast node as `(:kebab-type {fields})`, every position `"L:C"` (raw, not `//line`-adjusted, byte columns), and `:dot` (the `.` of selectors and type assertions, found by the helper in the source) | go/parser | the forms, and every position of mode `:full` |
| `:comments`: every comment group with its comments' positions and text; `:text` (go/ast's `CommentGroup.Text`) on doc groups; `:doc`/`:comment` as the group's first position | go/parser | doc comments, `;` comments, directives' attachment |
| `:directives`: every `//go:`, build, `//line`, `//export`, `//extern` and `ast.ParseDirective` comment | go/ast | `go/directive` and directive metadata |
| `:config`, the file lists (`:go-files`, `:s-files`, ...), `:embed-patterns`, `:embed-files` with SHA-256, `:imports`, per-file `:go-version`, `:dir-test`, `:local-imports` | go list, go/build | `go/package`, `go/file`'s `:lang` |
| annotations as reader metadata: `:mode`, `:t` (a type id), `:val`, `:def`/`:use` (objects), `:inst`, `:sel`, `:field`, `:implicit` | go/types | the decisions of SPEC §11.1 |
| `:init-order`, `:types` (named types: `:underlying`, method sets, layouts), `:type-table` (every type, structured, in dependency order) | go/types | `:init-order`, the type forms of `:tag`/`:inst`, constants' kinds; method sets and layouts are not used |
| `:errors` (go/types' errors, `file:L:C: msg`), `:warnings` | go/types, the helper | goi's check: `gocmp errorcheck -dump-errors` |

### 1.3 What the converter takes from go/types

The converter reads the annotations at about 30 sites (`(an n)` in convert.ae). They are the
decisions of SPEC §11.1:

| decision | annotation read | forms |
|---|---|---|
| a call, a conversion or a builtin | `:mode` `:type`/`:builtin` on the callee | `conv`, builtin heads, calls |
| index or instantiation | `:mode :type` on the index | `(inst ...)`, `aget` |
| field, method value or method expression | `:sel` `:kind` | `(.-f x)`, `(.M x ...)`, `method-expr` |
| embedded paths | `:sel`/`:field` `:via` | `:go/via` |
| package members, universe names, dot imports | `:use` `:kind :pkg`, `:universe` | `pkg/Name`, `go/id`, `go/call` |
| constant values and their kind | `:def`'s `:val` and `:t` | `:val` on constant names |
| untyped constants in code, non-constant shifts | `:mode`, `:t` | `:tag` (§8.3) |
| inferred type arguments | `:inst` | `:inst` |
| new or reused names in `:=` | `:def` vs `:use` | `^:assign` |
| implicit import names | `:implicit` on import specs | `[name "path"]` |
| init order | `:init-order` | `go/package`'s `:init-order` |

goi's checker computes every one of these itself, and its annotation check verifies them
(GOI-SPEC §3.6, §11.4). Re-measured here: `bin/goi annotations tamago-amd64` covers 371 packages
and 35,783 declarations with 0 errors, verified `{:assign 757, :conv 22534, :inst 1061, :tag 269,
:val 9084, :go/via 4969}`, in 17.5 s (80 s of CPU). Over the five corpora the count is 121,345
declarations (GOI-SPEC "As built: GR6").

### 1.4 Where the helper is needed

| user | command | what it asks of Go's toolchain |
|---|---|---|
| repr/go | `bin/repr-go dump`, `corpus`, `read`, `roundtrip` (and `--text`, `--dirs`, `--tests`) | the go command (go list, export data), go/parser, go/types; gc and the linker only for the round trip's judge (`go build`, `gocmp`) |
| the gate | `bin/gate --full`'s repr-go step (round trips, `bin/box-vendor`, `bin/tamago-vendor verify`, `bin/x509roots-vendor`) and goi step (`bin/goi annotations`, `check`) | as above, on fresh conversions |
| goi's check | `bin/goi check` and `--essential` (the essential gate) convert `$GOROOT/test`'s programs (corpora `linux-test`, `linux-dirtest`, `linux-errtest`) when they are not cached | the helper, once per cache; gc for the references |
| goi's table | `bin/goi table` converts linux's std (cached under `.tmp/goi-table/`); tamago's from `tamago/goroot` | the helper for linux |
| vendoring | `bin/tamago-vendor convert`/`verify` (TamaGo's GOROOT/src and goos module), `bin/toolchain-vendor convert`/`verify` (GOROOT/src/cmd's 105 packages, 3 configurations), `bin/box-vendor` (with tests; purego for darwin/arm64), `bin/x509roots-vendor` | the helper, then gc for their `check`s |
| the box | nothing: the box never reads Go text | — |

So the go command is needed for the configuration's file lists (go/build's rules), for the
modules' import resolution and for the export data. go/parser is needed for the tree, and
go/types for the annotations.

### 1.5 The box

The box runs Arbace as a TamaGo guest. It has no go command and no processes of the host's kind,
except the tool guests (`compile`, `link` and `asm` built from `tamago/cmd/`'s forms). In
`bin/box-rebuild`, it runs j2g and repr/go's *printing* inside itself, and plans builds with
`arbace.box.plan`, an Arbace go list over the store's trees (S8). It never reads Go text:
`tamago/goroot`, `tamago/cmd` and `box/` were all converted on the host by the helper. A reader
without the toolchain is what lets the box convert Go code itself, for example a new TamaGo
release, a vendored module, or Go text typed at a REPL.

### 1.6 What fails today

goi's check of `$GOROOT/test`'s 659 errorcheck programs, against `test/goi/check-linux-amd64.edn`
(GOI-SPEC "As built: GR7b" and the divergences' table): 337 pass, and 281 are unsupported.
Among the unsupported:

- **70 syntax errors.** go/parser rejects the file, so there is no dump and no forms. An example
  reason: `bombad.go:14:2: illegal byte order mark (and 4 more errors)`. Of the corpus, 73 are
  rejected and 70 reach this step; the others stop earlier, at an action flag.
- **64 that repr/go cannot convert** (GC47, decided (a): "the forms are for valid Go"):

  | count | the converter's failure |
  |---:|---|
  | 39 | `selector: :selector-expr`: a selection with no object |
  | 10 | `not a numeric value`: a constant without a value |
  | 4 | `type selector: :selector-expr` |
  | 2 | `x.(type) outside a type switch` |
  | 1 | `conversion arity` |
  | 1 | `assignment :=` |
  | 7 | exceptions in the converter (`import6.go`, `syntax/typesw.go`, `fixedbugs/bug351.go`, ...) |

Both classes are gc's errorcheck tests: gc must report the errors in the files' `// ERROR`
comments. For the first class gc's syntax errors are the whole output, since gc stops after
them (noder). For the second, they are types2's.

## 2. Prior art

### 2.1 go/scanner and go/parser (go1.27.1, `$GOROOT/src/go/`)

| file | lines |
|---|---:|
| `go/scanner/scanner.go`, `errors.go` | 999, 122 |
| `go/parser/parser.go`, `resolver.go`, `interface.go` | 2,956, 612, 253 |
| `go/ast/ast.go` (the nodes), `commentmap.go`, `directive.go` | 1,155, 327, 179 |
| `go/token/position.go`, `token.go` | 629, 341 |
| non-test totals: scanner 1,121; parser 3,821; ast 3,458; token 1,449 | |

- **Structure.** The scanner inserts semicolons and reports comments (`ScanComments`). The
  parser is recursive descent over the spec's grammar, with one token of lookahead. It builds
  go/ast, which has a position field for every token that matters: `Lparen`, `Rparen`, `OpPos`,
  `Lbrace`, `Rbrace`, `Colon`, `TokPos`, `ValueEnd` (go1.26), `FileStart`, `FileEnd`, and so on.
  The resolver (`resolver.go`) resolves identifiers syntactically. The helper turns it off
  (`SkipObjectResolution`).
- **Comments.** Every comment is in `File.Comments`, in groups (`consumeCommentGroup`: adjacent
  comments with no blank line between them). Lead comments become `Doc` and trailing ones
  `Comment` (`leadComment`, `lineComment`, parser.go:49-267). The dump's `:comments`, `:doc`,
  `:comment` and `:text` are exactly these.
- **Positions.** `token.Pos` offsets go through a `token.File`. The dump uses `PositionFor(p,
  false)`: lines and byte columns, not adjusted by `//line`.
- **Errors and recovery.** `p.error` and `p.errorExpected` ("expected X, found Y") add to a
  `scanner.ErrorList`.
  - Without `AllErrors`, an error on the same line as the last one is dropped, and parsing stops
    after more than 10 errors (`bailout`).
  - `advance(to)` skips to a synchronization set (`stmtStart`, `declStart`, `exprEnd`), with
    `syncPos`/`syncCnt` so that it cannot loop.
  - The tree after errors is partial, with `BadExpr`, `BadStmt` and `BadDecl`. Nesting is
    limited to 100,000 (`maxNestLev`).
  - Its messages are go/parser's own, not gc's.

### 2.2 gc's parser: `cmd/compile/internal/syntax` (go1.27.1, also `tamago/cmd/`'s forms)

| file | lines |
|---|---:|
| `scanner.go`, `source.go`, `tokens.go`, `token_string.go`, `operator_string.go` | 881, 218, 159, 70, 46 |
| `parser.go` | 2,908 |
| `nodes.go`, `pos.go`, `positions.go`, `walk.go`, `type.go`, `syntax.go` | 505, 223, 365, 346, 78, 94 |
| `branches.go` (labels, `goto`, `break`, `continue`: `CheckBranches`) | 339 |
| `printer.go`, `dumper.go`, `testing.go` | 1,020, 212, 69 |
| non-test total | 7,533 |

- **Imports.** Only the standard library: `fmt`, `go/build/constraint`, `io`, `os`, `reflect`,
  `strconv`, `strings`, `unicode`, `unicode/utf8`. A renamed copy builds as an ordinary package,
  as in E3. As forms it is `tamago/cmd/go/cmd/compile/internal/syntax/`: its 16 files and the
  package file, which `bin/toolchain-vendor verify` reconverts and compares and whose tools
  `check` proves byte-identical to the originals'.
- **The tree is gc's, not go/ast's.**
  - `Operation` covers unary and binary operators, `ListExpr` multiple indices, and
    `TypeSwitchGuard` type switches.
  - A node has one position (`Pos()`), plus a few end positions (`Rbrace`, the `Colon` of case
    clauses). A call has no `Rparen`, a binary operation has no separate operator position, and
    so on.
  - Positions carry a `PosBase`, which `//line` changes.
  - Over std it makes 4,534,662 nodes where go/ast has 4,844,260 (E3).
- **Comments are not kept.** The scanner can report them (mode `comments`), but the parser asks
  only for directives (`//go:`, `//line`) and gives them to a `PragmaHandler`. Mode `:full`
  needs every comment and every position, and syntax has neither.
- **Errors and recovery.** gc's messages come from here:
  - `syntaxErrorAt` writes "syntax error: unexpected X, expected Y" (also "in", "at" and "after"
    variants), naming the token as gc names it ("name x", "literal 1", "newline").
  - `advance(followlist)` skips to the follow set, plus the statement keywords' `stopset` inside
    a function (`fnest`).
  - After an error at EOF, follow-up errors are dropped.
  - gc's `base.ErrorfAt` keeps one syntax error per line. Without `-e`, gc stops after 10
    errors.
  - `CheckBranches` reports gc's label and jump errors.
- **Release.** It follows gc. go/parser follows go/types and gofmt. Both are maintained by the
  Go team and change at the same releases.

### 2.3 Type checkers: go/types, types2 and `arbace.go.check`

go/types (24,879 lines, non-test) is mostly generated from types2 (23,145 lines,
`cmd/compile/internal/types2`), gc's checker. `arbace.go.check` (`arbace/go/check.ae` and
`arbace/go/check/`, 8,071 lines) follows types2: its messages, its follow-on rules and its
inference (`infer.ae` after `unify.go` and `infer.go`). It checks Go forms, not Go text. It
loads imports from the table's digest (compiled packages) or from forms. As §1.3 measures, it
already decides everything the converter asks of go/types.

### 2.4 Others

- **Joker** (candid82/joker, v1.10.0 at `c87eb950`, 2026-10-05; the fork in go-whim at
  `80c49bf`, `guest/joker/joker/`, read only) is a Clojure dialect written in Go. Its reader,
  `core/read.go` (1,540 lines), is a hand-written recursive descent with line and column
  positions per form. A read error is a `ReadError` panic at its position, with no recovery.
  It is the inverse of the task (a Lisp read by Go). Its lesson for option (a) is that a reader
  with positions is about 1,500 lines. Joker has no Go parser.
- **yaegi** (traefik/yaegi, doc/GO-FORMS-REPL-SURVEY.md) parses with `go/parser` and type-checks
  with its own checker (`interp/cfg.go`), not go/types. That is the split recommended here: Go's
  parser, the interpreter's own checker.
- **Arbace's own readers.** `arbace.lang.LispReader` (the vendored Clojure reader, Java forms)
  and repr/clojure. The other bridges' surveys weighed a parser in Arbace and set it aside
  while the language's own compiler can run on the host:
  - doc/REPR-RUST-SURVEY.md §3.3 (e): "worth it only if Rust must be read where rustc cannot
    run";
  - doc/REPR-HASKELL-SURVEY.md (c).
  Go differs: its parsers are already Arbace forms that Arbace runs, and the box, where Go's
  toolchain cannot run, is a target.
- **Arbace's go list.** `arbace.box.plan` (346 lines) computes `go list -deps` from source trees.
  It handles GOOS and GOARCH in file names, `//go:build`, test and `_`/`.` files, files importing
  `"C"`, std's vendor directory, GOOSPKG's `runtime/goos`, language versions and embedded files
  (no glob patterns). `arbace.go.check.load` (166 lines) evaluates build constraints for goi.

### 2.5 E1: the 70 syntax-error programs, judged

Each of the 70 programs' errors, as gc prints them with `-C` (`FILE:LINE: MESSAGE`), judged by
`gocmp errorcheck` (Go's test driver's judge, vendored):

- from gc's `syntax` (a renamed copy, `CheckBranches`, one syntax error per line as
  `base.ErrorfAt` keeps them);
- from go/parser (`AllErrors`).

| parser | pass | fail |
|---|---:|---:|
| gc's syntax | **66** | 4 |
| go/parser | 20 | 50 |

- **syntax's 4 failures:** `fixedbugs/issue23586.go`, `fixedbugs/issue4468.go`,
  `fixedbugs/issue50372.go` and `slice3err.go`. Some or all of the expected errors are types2's
  (`must be function call`, `range clause permits at most two iteration variables`, `3-index
  slice of string`), reported after a parse.
  - syntax parses `issue23586.go` and `issue50372.go` without error, and types2 reports them.
  - In `issue4468.go` and `slice3err.go`, syntax reports some of the errors and types2 the rest.
  go/parser rejects all four in its own words, so they need a tree that go/parser does not give
  (GPS9).
- **go/parser's 50 failures** are its messages (`expected 'package', found 'func'` where gc says
  `package statement must be first`, `expected operand` where gc says `unexpected newline`),
  and its line filter.

So errors must be gc's syntax's, as goi's errors are types2's.

### 2.6 E2: what each parser accepts

All 3,400 `.go` files under `$GOROOT/test`, parsed by both:

| go/parser | syntax | files |
|---|---|---:|
| accepts | accepts | 3,306 |
| rejects | rejects | 78 |
| accepts | rejects | 14 |
| rejects | accepts | 2 |

- **The 14** are branch and label errors, which syntax checks (`CheckBranches`) and go/types
  checks after parsing: `goto.go`, `label.go`, `label1.go`, `switch4.go`, `syntax/typesw.go`,
  `fixedbugs/bug136.go`, `bug179.go`, `bug213.go`, `bug217.go`, `bug344.go`, `issue14540.go`,
  `issue19610.go`, `issue6500.go`, `issue7538a.go`.
- **The 2** are `fixedbugs/issue23586.go` and `issue50372.go`, two of E1's 4.

All 6,951 `.go` files of `$GOROOT/src` outside `testdata/` (89.9 MB) parse with both, with no
error.

### 2.7 Which to follow

| need | go/parser | syntax |
|---|---|---|
| the dump's tree (go/ast, field by field) | is it | another tree; 310,000 fewer nodes over std; positions missing |
| mode `:full` (every comment and position) | yes | no comments, one position per node |
| gc's syntax errors (goi's errorcheck) | 20 of 70 | 66 of 70 |
| gc's acceptance (what compiles) | 16 files differ in `$GOROOT/test` | is gc's |
| speed, compiled (E3) | 44.5 MB/s (std) | 47.3 MB/s (std) |

**Follow go/parser for the tree and syntax for the errors.** A program that parses is read with
go/parser's tree. A program that does not parse is reported with syntax's errors. Neither
parser's tree has to be adapted to the other's.

## 3. Implementation options

### 3.1 The options

**(a) A parser hand-written in Arbace, following go/parser.** A scanner of about 700 lines and a
parser of about 2,200, after go/parser's 999 and 2,956. Comment grouping, the go/ast node shapes
and every position of go/ast, about 600 more. About 3,500 lines of `.ae` in all.
- Pros: it is Arbace's Clojure dialect throughout. It is an ordinary embedded namespace,
  evaluated from the checkout until the next re-seed, with no table or build change. It runs in
  the box.
- Cons: a second Go parser that must equal go/parser on every file of every corpus. Each Go
  release's grammar changes come by hand. Its speed is the interpreted parser's or less
  (E4: Arbace's byte loops run at 1.4 to 1.6 MB/s; go/parser spends about 2.6 times its
  scanner's time). gc's errors need syntax's recovery copied as well, or GP6 runs syntax
  anyway. 2 to 3 weeks.

**(b) gc's syntax from `tamago/cmd/`'s forms, interpreted by goi or compiled through the table.**
- Pros: gc's parser and gc's errors by construction (E1: 66 of 70).
- Cons: its tree is not the dump's, and it keeps neither comments nor go/ast's positions. Reading
  forms with it means changing its parser to record what go/ast records. Those are changes to a
  vendored tree that `bin/toolchain-vendor check` keeps byte-identical, so the result would be a
  fork. On top of that comes an adapter from its tree to go/ast's shapes, whose equality must be
  proven like (a)'s.
- Compiling it into the executable means a renamed copy. The table lists std's packages and
  user packages, and Go's rule forbids importing `cmd/.../internal/`.
- **Good for the errors (GP6), not for the tree.**

**(c) go/parser from `tamago/goroot`'s forms, run by Arbace.** go/parser, go/scanner, go/ast and
go/token are std, already Go forms in `tamago/goroot/go/go/` (133 KB, 39 KB, 128 KB and 53 KB of
forms), already interpretable by goi (GR8a: `go/parser` loads at the REPL in 2.28 s). A dumper
turns `*ast.File` into the dump's tree as Arbace data. It is generated from go/ast's forms: the
node types and their fields, in go/ast's order (GPS5).
- Two ways to run it:
  - (c1) interpreted by goi (no build change, 0.75 MB/s);
  - (c2) compiled into the executable, by listing the four packages in
    `go/arbace/gotable.edn` and putting the dumper in `go/` as hand-written Go forms (native
    speed; one re-seed).
- Pros: the tree is go/parser's by construction. E3 shows the interpreted parser's trees equal
  the native ones over all of std, so tree differences can only be goi's bugs or the dumper's,
  and E-tree finds both. No grammar to maintain: moving the Go pin reconverts `tamago/goroot` as
  it does now. It runs in the box. It is Go's own code as Arbace forms, as `tamago/` and
  `tamago/cmd/` are: the S1 and S9 way, "the prior art as forms, run by Arbace".
- Cons: Arbace depends on goi to read Go (c1), or on the table and a re-seed (c2). The dumper
  crosses from Go values to Arbace data, which costs (§7).

**(d) The helper in process: go/parser and go/types compiled in.** godump's tree and annotations
by construction, with no go command:
- the files from Arbace's loader (§5);
- imports type-checked from source, a source importer in place of export data;
- the dumper building Arbace data, as in (c2).

Pros: annotation equality for free (§4.3 (c)). Cons:
- about +2.6 MB of executable (E5, with go/importer);
- a second type checker in the executable beside `arbace.go.check`;
- imported objects' positions differ: export data's against the source's (`:decl` of imported
  local types; HELPER-NOTES "What is left");
- it keeps go/types, not Arbace's checker, as the authority. Arbace's checker would remain the
  verifier, as now.

### 3.2 E3: Go's parsers run by goi

For each parser, a program parses its arguments and counts the nodes. It was converted by
`bin/repr-go read --goos linux --goarch amd64` (the parser package a renamed copy, `gps/syntax`,
or std's go/parser) and run by `bin/goi run` on stage 0. The native times are the same packages
built by TamaGo's go (Appendix A).

| run | file(s) | interpreted (goi) | native | ratio |
|---|---|---:|---:|---:|
| syntax | `fmt/print.go` (33,315 bytes, 11,020 nodes) | 0.056 s | 0.002 s | 28× |
| syntax | `runtime/proc.go` (244,820 bytes, 46,354 nodes) | 0.287-0.298 s | 0.008 s | 36× |
| go/parser | `runtime/proc.go` (50,314 nodes) | 0.288-0.305 s | 0.006 s | 50× |
| syntax | all of std, tamago/amd64 (1,742 files, 19.6 MB, 4,534,662 nodes) | 26.14 s, **0.75 MB/s** | 0.373 s, 52.5 MB/s | 70× |
| go/parser | the same (4,844,260 nodes) | 26.24 s, **0.75 MB/s** | 0.441 s, 44.5 MB/s | 59× |

- One process loads the parser package and runs: 3.5 s before the first file for syntax, 2.3 s
  for go/parser (GR8a's load).
- The interpreted go/parser's node counts equal the native ones file by file over std.
- Over all of `$GOROOT/src` (6,951 files, 89.9 MB), native: go/scanner 0.77 s (117 MB/s),
  go/parser 1.79 s (50.2 MB/s), syntax 1.62 s (55.4 MB/s). Interpreted it would be about 120 s
  of CPU, which the files' parallelism divides.

### 3.3 E4: Arbace's own loops

On stage 0, over `runtime/proc.go`'s bytes (best of 5):

- `arbace.repr.go.convert/source-text` (a compiled byte loop of the converter, `^bytes`, transient
  vectors): 0.156 s, 1.6 MB/s;
- an evaluated lexer of identifiers, numbers, strings, comments and operators (30,526 tokens):
  0.178 s, 1.4 MB/s.

go/scanner does the same work at 115 MB/s. go/parser spends about 2.6 times its scanner's time,
so a hand-written Arbace parser (a) would run at roughly 0.5 MB/s: about 40 s of CPU for std and
3 minutes for `$GOROOT/src`. It would be no faster than goi interpreting go/parser.

### 3.4 E5: sizes, compiled

A program with `encoding/json`, `math/big`, `regexp`, `fmt`, `os` and `strings` (stripped,
TamaGo's go for linux/amd64): 2,932,896 bytes. Adding `go/parser` (with `go/scanner`, `go/ast`
and `go/token`) makes it 3,289,248 bytes, **+356,352**. Adding `go/types` and `go/importer`'s
source importer as well makes it 5,566,624, **+2,633,728**. GR8-SURVEY §3.3 measured `go/ast`
alone through the table at +335,872. The executable is 143.7 MB.

### 3.5 Side by side

| | (a) hand-written | (b) syntax | (c1) go/parser, interpreted | (c2) go/parser, compiled | (d) helper in process |
|---|---|---|---|---|---|
| tree = the dump's | to prove, every corpus | needs a fork and an adapter, to prove | by construction (goi's bugs aside) | by construction | by construction |
| gc's syntax errors | by copying syntax's recovery | 66 of 70 | with syntax beside (b) | with syntax beside | with syntax beside |
| annotations | Arbace's checker (§4) | the same | the same | the same | go/types |
| speed over std (parse) | about 40 CPU-s | 26 CPU-s | 26 CPU-s | 0.44 CPU-s | 0.44 + type-checking about 8 CPU-s |
| executable | +the namespace | +0 or a copy | +0 | about +0.36 MB | about +2.6 MB |
| re-seed | at the next one, batched | — | none | one | one |
| maintenance per Go release | the grammar by hand | the fork's patches | none (reconverted with `tamago/goroot`) | none | none |
| in the box | yes | yes | yes | yes | yes, with the loader |
| "Arbace all the way down" | Arbace's Clojure | Go forms (gc's) | Go forms (Go's) | Go forms (Go's) | Go forms, but go/types beside Arbace's checker |

**Recommended: (c), first (c1) then (c2), with (b) for the errors only, and annotations from
Arbace's checker** (GPS1, GPS2, GPS4). (a) is the choice if the parser must be written in
Arbace's Clojure dialect for its own sake. It reads no better and no faster.

## 4. Annotations

### 4.1 Who can answer

| decision (§1.3) | syntactic? | Arbace's checker |
|---|---|---|
| package names, locals and package members | yes, with scopes (go/parser's resolver does it) | yes |
| new names in `:=` | yes, with scopes | yes (`^:assign` verified) |
| a call or a conversion; index or instantiation | only for locally declared types | yes (`conv` verified) |
| field or method; method expression; embedded paths | no (types) | yes (`:go/via` verified) |
| constant values, their kinds; `:tag` | no | yes (`:val`, `:tag` verified) |
| inferred type arguments | no | yes (`:inst` verified, GR6) |
| implicit import names | the imported package's clause | yes |
| init order | no | yes (`:init-order` verified) |
| per-file language version | yes (`//go:build`) | used, not computed |

So the annotations must come from a checker: Arbace's (§4.2), or go/types kept (§4.3).

### 4.2 The checker as the annotator

The checker checks Go forms, and the forms need the answers. The circle is broken by **raw
forms**, a private stage of the conversion:

1. The parser and dumper give the tree, without annotations.
2. The converter's raw mode writes forms where every decision of §1.3 that needs types is left
   open. `(F x)` is a call whose head the checker classifies, `(go/select x f)` a selector of
   unknown kind, there is no `:val`, `:tag`, `:go/via` or `:inst`, and `:=` targets are marked.
   Raw forms always carry mode `:full`'s positions.
3. The checker in a resolve mode checks the raw forms against the dependencies' checked states,
   in dependency order, as `bin/goi annotations` does. It accepts the open heads and records its
   answers by position: the kind of each selector and callee, constant values, the types it would
   tag, inferred instances, embedded paths, implicit names and the init order.
4. The answers are put into the tree as the dump's annotations, in go/types' shape: `:mode`,
   `:t`, `:val`, `:def`/`:use`, `:sel`, `:field`, `:inst`, `:implicit`. A writer adds a
   `:type-table` (and `:types`' `:underlying`) from the checker's types, in the table's
   structure.
5. The converter, unchanged, makes the forms from the annotated tree.

- **The converter does not change.** The forms it makes are the helper's if the tree and the
  annotations are, so the switch is checkable at each level (§6).
- **Errors.** Step 3 reports the program's type errors as types2 does, in place of the dump's
  `:errors`, and in raw forms, which also exist for the invalid programs GC47 left out. A
  selection without an object is `(go/select x f)`, which the checker reports as `x.f undefined`.
  So most of GC47's 64 can be checked (GPS9).
- **Cost.** Two conversions: the raw one and the final one. If the raw conversion is measured to
  dominate, the checker's resolve mode can write the final forms itself, in one conversion (GPS3
  (b)). The converter's typed half then moves into the checker, and equality must be proven
  again.

### 4.3 Alternatives

- (b) one conversion, the checker finishing the forms (above);
- (c) go/types compiled in (§3.1 (d)): its annotations by construction, at +2.6 MB and a second
  checker;
- (d) the helper kept for annotations only: it keeps the go command, the export data and the
  dump's text, so nothing is gained.

### 4.4 What changes for the dump's consumers

| consumer | today | native |
|---|---|---|
| `arbace.repr.go.convert` | the dump read from text | the same data, from the dumper and the annotator; no change |
| mode `:full`'s text (`source-text`, `recv-chan-pos`) | the Go file's bytes | the same bytes (the loader reads them anyway) |
| `bin/repr-go read --dump`, `corpus` | read the dumps | kept for the helper (the oracle) |
| goi's check (`gocmp errorcheck -dump-errors`, "go/types reports none either") | the dump's `:errors` | the checker's errors from step 3 (GC46's rule restated: errors the checker does not report either) |
| goi's annotation check | verifies the converter's annotations against the checker | in native conversions, true by construction; it becomes meaningful against the helper's conversions (the oracle) |
| `:types`' method sets and layouts, `:warnings` | written, not used | not produced; `:warnings` (a `/*line*/` inside an expression) by the dumper |

## 5. The loader: go list's part

What the helper gets from the go command, and where Arbace has it:

| need | helper | Arbace |
|---|---|---|
| a package's files for a configuration (file names, `//go:build`, `_test.go`, `_`/`.`, cgo off), go list's other lists (`.s`, `.h`, `.syso`, ignored) | go list | `arbace.box.plan` (S8), `arbace.go.check.load` |
| std's packages (`go list std`), cmd's (`cmd/vendor`) | go list | the plan's walk, to generalize |
| imports: std's vendor, a module's paths, `ImportMap` | go list | the plan (std, GOOSPKG); modules from `go.mod`, to add |
| language versions (`go` lines, `//go:build go1.N`) | go list, go/version | the plan |
| `//go:embed` patterns, files and SHA-256 | go list | the plan, without glob patterns; the go command's resolution (`cmd/go/internal/load`) to follow |
| test variants (`-tests`), external test packages | go list `-test` | to add (file lists only: no export data is needed any more) |
| directory tests (`-dirs`: action lines, grouping by package clause, local imports, runindir modules) | `dirs.go`, 452 lines | to port to Arbace (data only: no compilation is needed any more) |
| the dependencies' export data | go list `-export` | **not needed**: the checker reads the dependencies' forms or the table's digest |

The loader generalizes `arbace.box.plan` from the store's trees to directories as well. It is
checked by E-tree's header comparison: `:config`, file lists, `:imports`, `:embed-files`,
`:go-version`, `:dir-test` and `:local-imports` equal to the helper's.

## 6. The output and its proof

The native reader must give the forms the helper's path gives, byte for byte, over every corpus.
There are three levels, each checked against the helper as the oracle:

1. **E-tree.** The dumper's tree and header equal the helper's dump with the annotations stripped
   (`:mode`, `:t`, `:val`, `:def`, `:use`, `:inst`, `:sel`, `:field`, `:implicit` metadata;
   `:types`, `:type-table`, `:init-order`, `:errors`). Equality is Arbace data equality.
2. **E-ann.** The annotations the converter reads equal go/types'. Types are compared by
   structure, not by id (the ids are numbered by first mention), and only what the converter
   reads is compared: `:t` where it is read (shifts, constants), not on every expression.
3. **E-forms.** The `.ae` files are byte for byte the same in mode `:lines` and mode `:full`.
   The round trips' references and the vendored trees then hold unchanged.

The corpora (HELPER-NOTES "Coverage", ROUNDTRIP.md):

| corpus | size | today's check |
|---|---|---|
| std tamago/amd64, tamago/arm64, linux/amd64, linux/arm64 | 379, 378, 382, 380 packages; 1,742-1,834 files | `bin/repr-go roundtrip` |
| std with tests, tamago/amd64 | 554 packages, 2,935 files | `roundtrip --tests` |
| `$GOROOT/test`'s programs, four configurations | 1,702-1,713 programs | `roundtrip` |
| directory tests, four configurations | 559-562 dumps | `roundtrip --dirs` |
| error tests, linux/amd64 | 659 programs (586 dumped) | goi's check |
| `tamago/goroot`, `tamago/goos` | 2,416 `.ae` | `bin/tamago-vendor verify` |
| `tamago/cmd` | 758 `.ae`, 3 configurations | `bin/toolchain-vendor verify` |
| `box/`, purego (darwin/arm64), x509roots | 136 `.ae` and the rest | `bin/box-vendor`, `bin/x509roots-vendor verify` |

The vendoring tools' `verify` converts afresh and compares with the committed forms, so with the
native reader as the converter they are E-forms over the vendored trees.

## 7. Speed

Measured over std for tamago/amd64: 1,742 files, 578,418 lines, 19.6 MB.

| step | today | native, interpreted (c1) | native, compiled (c2) |
|---|---|---|---|
| loading | go list, 0.3 s cached (about 6 s the first time) | the loader: no compilation | the same |
| parsing | go/parser in the helper, 0.44 s | 26.2 CPU-s (E3) | about 0.44 CPU-s |
| type checking | go/types in the helper | the checker: 80 CPU-s, 17.5 s wall (§1.3) | the same |
| the dump | the helper writes 252.7 MB of text; all of it 8.5 s at `-j 1`, 1.0 s at `-j 64` | the dumper builds data: not measured, estimated the parse's order | estimated a few CPU-s |
| reading the dump | **7 min 54 s CPU** (28.5 s wall, 64 threads) | none | none |
| converting | 13 min 9 s CPU with reading (45.3 s wall, 32 threads; mode `:full` 24 min 17 s, 85.1 s) | twice, raw then final: about 2 × 5 min CPU (estimated: today's less reading) | once with GPS3 (b): about 5 min |
| total, CPU | about 13 min | about 12 min (estimated) | about 6 to 7 min (estimated) |

- The helper's throughput is 2.3 MB/s of Go per thread with type checking and writing, and 44.5
  MB/s for parsing alone.
- The native path's cost is the converter's, not the parser's. Even interpreted, parsing is about
  4% of it. Not writing and reading 252.7 MB of text pays for the checker and most of the second
  conversion.
- At the REPL (one file, one package) the interpreted parser's load is 2.3 s once per process,
  then 0.3 s for a file as large as `runtime/proc.go`. Compiled, both are negligible.
- Over all of `$GOROOT/src` (89.9 MB), parsing is 1.8 s compiled, about 120 CPU-s interpreted,
  and about 3 CPU-min estimated for (a).

## 8. Milestones

| milestone | content | exit criteria | effort (days) |
|---|---|---|---:|
| **GP1** the tree | go/parser run by goi from `tamago/goroot`'s forms in the tools' host (`arbace.repr.go.parse`); the dumper, generated from go/ast's forms, building the dump's tree, comments (`:text`), directives, `:dot`, `:warnings`; `bin/repr-go read --reader native` without annotations, for E-tree | E-tree for every file of every corpus of §6 (trees, comments, directives); the parse and dump times over std recorded | 3-4 |
| **GP2** the loader | `arbace.box.plan` generalized: directories and store trees, all of std and cmd, modules, test variants, directory tests (`dirs.go`'s rules), embed patterns with globs, language versions | E-tree's header (§5) for every corpus; `tamago/goroot`'s and `tamago/cmd`'s file lists equal to go list's for their configurations | 3-4 |
| **GP3** raw forms | the converter's raw mode (§4.2 step 2), private (GPS10) | every corpus converted raw; the raw forms read back; their count of open decisions recorded per kind | 3-4 |
| **GP4** the annotator | the checker's resolve mode; answers by position; the annotations, `:type-table`, `:types`' underlying and `:init-order` written in the dump's shape | E-ann for every corpus; `bin/goi annotations` over the native conversions: zero errors, the same counts | 5-7 |
| **GP5** the forms, the switch | `bin/repr-go read` native by default (GPS7); the helper as the oracle; the vendoring tools and goi's corpora on the native reader | E-forms for every corpus, `:lines` and `:full`; `bin/repr-go roundtrip` (and `--text`, `--dirs`, `--tests`) without regression; the vendoring tools' `verify`; goi's check as recorded | 2-3 |
| **GP6** errors | gc's `syntax` run from `tamago/cmd`'s forms for files go/parser rejects, its errors as gc prints them; goi's errorcheck on the raw forms of invalid programs (GPS9) | goi's check: the 70 syntax-error programs re-measured (66 pass expected, E1); GC47's 64 re-measured; the reference recorded | 2-3 |
| **GP7** compiled | go/parser, go/scanner, go/ast, go/token listed in the table; the dumper as Go forms in `go/`; syntax as a renamed copy; GPS3 (b) if measured worthwhile | the parse over std under 1 s; the executable's growth recorded (`bin/goi table --size`; about +0.36 MB expected); the essential gate; batched into a re-seed | 3-4 |
| **GP8** the box reads Go | `arbace.repr.go` read in the box from the store's trees (the plan, the parser, the checker against the box's digest) | `tamago/goroot`'s conversion in the box byte for byte equal to the committed forms, on amd64 and the emulated arm64 VM (as S8's rebuild) | 2-3 |

- Total: about 23 to 32 days. GP1-GP6 change only tools' namespaces, which are evaluated from the
  checkout until a re-seed. GP7 changes the program (the table, `go/`) and needs one re-seed,
  batched with other runtime changes.
- With option (a) instead of (c), GP1 becomes the hand-written parser (10-15 days) and GP7 loses
  its parser part.

## 9. The checks and the gate

| check | what | where |
|---|---|---|
| `bin/repr-go native-check [CORPUS...]` (new, GP1-GP5) | E-tree, E-ann, E-forms against the helper's conversions, every corpus of §6; known differences with reasons in `test/repr/go/native-known.edn` (expected empty) | `bin/gate --full`, beside the round trips; by hand at each milestone |
| the round trips | unchanged references, now from native conversions | `bin/gate --full` (as now) |
| `bin/tamago-vendor verify`, `bin/toolchain-vendor verify`, `bin/box-vendor`, `bin/x509roots-vendor verify` | the vendored trees reconverted natively, equal to the committed forms | `bin/gate --full` (as now) |
| goi's check, essential and whole | its corpora converted natively; syntax errors in gc's words (GP6) | essential gate (`--essential`), `bin/gate --full` (as now) |
| the box's reading (GP8) | `tamago/goroot` converted in the box | by hand at milestones, as `bin/box-rebuild` |

The essential gate gains nothing new (GPS8). Its goi check reads its corpus natively after GP5,
so it needs no helper and no go command.

## 10. Proposals for the user (GPS1-GPS11)

- **GPS1 The tree's parser.**
  - (a) hand-written in Arbace, following go/parser (§3.1 (a));
  - (b) gc's syntax from `tamago/cmd`'s forms, forked to record comments and positions, with an
    adapter to go/ast's shapes;
  - (c) Go's go/parser from `tamago/goroot`'s forms, run by Arbace, with a dumper to the dump's
    tree.

  *Recommended: (c)*: the dump's tree by construction (E3), no grammar to maintain, in the box,
  the S1 and S9 way. (a) if the parser must be Arbace's Clojure for its own sake.
- **GPS2 Syntax errors.**
  - (a) go/parser's messages (E1: 20 of 70);
  - (b) gc's syntax run for files go/parser rejects (66 of 70);
  - (c) none: the 70 stay unsupported (GC47 as decided).

  *Recommended: (b)*, at GP6.
- **GPS3 The annotations.**
  - (a) Arbace's checker over raw forms, its answers put into the tree, the converter unchanged
    (two conversions; §4.2);
  - (b) the checker finishing the forms itself (one conversion);
  - (c) go/types compiled in (§3.1 (d));
  - (d) the helper kept for annotations.

  *Recommended: (a)*, then (b) at GP7 if the second conversion is measured to matter. (a)
  makes go/types the oracle, as the frozen jar is Java's.
- **GPS4 Interpreted or compiled.**
  - (a) interpreted only (c1);
  - (b) interpreted from GP1, compiled at GP7 (c2);
  - (c) compiled from the start.

  *Recommended: (b)*: no build change until the reader is proven, then one re-seed.
- **GPS5 The dumper.**
  - (a) generated from go/ast's forms (`tamago/goroot/go/go/ast/`: node types and fields in
    order), as Go forms;
  - (b) hand-written Go forms;
  - (c) a port of `tools/godump/dump.go`'s reflection-based walk.

  *Recommended: (a)*: go/ast's field order is the dump's (HELPER-NOTES "The tree"), and a
  generator follows go/ast's changes at the pin's moves. (c) depends on reflect over goi's
  interpreted values.
- **GPS6 The interface between the reader and the converter.**
  - (a) the dump's data shape (`:godump 2`, positions as `"L:C"`), in memory;
  - (b) a new shape (positions as vectors, no type ids).

  *Recommended: (a)*: the converter does not change, and E-tree and E-ann compare like with
  like.
- **GPS7 After the switch.**
  - (a) native by default, the helper kept as the oracle in `bin/gate --full` (`bin/repr-go read
    --reader helper`);
  - (b) the helper by default, native in the box only;
  - (c) the helper removed.

  *Recommended: (a)*, as the JVM stays only as the oracle's reference.
- **GPS8 The gate.**
  - (a) `bin/repr-go native-check` over every corpus in `bin/gate --full`; the essential gate
    unchanged;
  - (b) also a subset in the essential gate.

  *Recommended: (a)*: no change to the bootstrap is checked by it, and the essential gate's goi
  check already reads natively after GP5.
- **GPS9 Invalid programs (GC47 revisited).**
  - (a) as decided, unsupported;
  - (b) goi's errorcheck on the raw forms of the 64 programs the converter cannot convert, and
    E1's 4 (syntax accepts, go/parser rejects) kept known with their reason;
  - (c) (b), plus syntax's tree for those 4.

  *Recommended: (b)*: raw forms exist for them at no extra cost (§4.2).
- **GPS10 Raw forms.**
  - (a) private to repr/go and the checker, documented in CONVERTER-NOTES, not a dialect of
    SPEC;
  - (b) a public "untyped Go forms" dialect, typed by hand at the REPL without annotations.

  *Recommended: (a)* now; (b) is a later question for goi's REPL.
- **GPS11 Go text at the REPL** (once GP7 or GP8 is done).
  - (a) later, when asked;
  - (b) `go/read-string` at goi's REPL, Go text read into forms in the session.

  *Recommended: (a)*: it is a use, not this plan's goal.

## 11. Risks

- **goi's bugs in the parser.** An interpreted go/parser that differs from the native one is
  goi's bug. E-tree finds it on 10 million lines, which also makes it a large test of goi. E3
  found none over std.
- **The checker's coverage.** E-ann requires the checker's answers on every declaration of
  every corpus. The annotation check already covers five corpora with 0 errors, but not
  `tamago/cmd` or the box's trees. They may find checker bugs (GP4's effort range).
- **The loader.** go list's corner cases: glob embed patterns, runindir modules, `cmd/vendor`,
  test variants' import maps. E-tree's header comparison is the check.
- **Speed.** The second conversion (§7) is an estimate. If it is too slow, GPS3 (b).
- **Re-seed.** GP7 changes the program. It is batched, and nothing before it does.

## 12. Sources

- Go 1.27.1 as TamaGo's tree, `/root/tamago-go` (`VERSION` go1.27.1, 2026-08-28):
  `src/go/scanner/`, `src/go/parser/` (`parser.go`: `consumeCommentGroup`, `advance`, `error`,
  `bailout`, `maxNestLev`), `src/go/ast/` (`ast.go`, `commentmap.go`, `directive.go`),
  `src/go/token/`, `src/go/types/`, `src/go/build/` (`build.go`, `constraint/`),
  `src/cmd/compile/internal/syntax/` (`parser.go`: `syntaxErrorAt`, `advance`, `stopset`;
  `scanner.go`'s `comments`/`directives` modes), `src/cmd/compile/internal/types2/`,
  `src/cmd/compile/internal/base/print.go` (`ErrorfAt`: one syntax error per line),
  `src/cmd/internal/testdir/testdir_test.go` (the errorcheck judge, vendored in
  `tools/gocmp/errorcheck.go`).
- Arbace at `9a6a36e9`: `tools/godump/` (doc/go/HELPER-NOTES.md), `arbace/repr/go/`
  (doc/go/CONVERTER-NOTES.md, doc/go/SPEC.md §10.2, §11), doc/go/ROUNDTRIP.md, `arbace/go/check/`
  (doc/go/GOI-SPEC.md §3.6, §11.4, "As built: GR6", "As built: GR7b", the divergences' table,
  GC46-GC49),
  `test/goi/check-linux-amd64.edn`, `arbace/box/plan.ae`, `arbace/go/check/load.ae`,
  `go/arbace/gotable.edn`, `tamago/goroot/go/go/{parser,scanner,ast,token}/`,
  `tamago/cmd/go/cmd/compile/internal/syntax/`, doc/go/GR8-SURVEY.md §3.3, doc/go/GR8-STD.edn,
  doc/REPR-RUST-SURVEY.md §3.3, doc/GO-FORMS-REPL-SURVEY.md (yaegi).
- Joker: candid82/joker v1.10.0, commit `c87eb9502fd6ea623dbf8f48aebb4ccd6ea1d1a8`, as forked
  in go-whim at `80c49bf5` (`guest/joker/joker/`, `core/read.go`), read only.

## Appendix A: the experiments

All in the worktree's `.tmp/gps/`, not tracked:

- `bench/`: a module `bench` with a renamed copy of `cmd/compile/internal/syntax`
  (`bench/syntax`, the 16 non-test files) and `main.go`:
  - `-mode goscanner|goparser|syntax -n N FILES|DIR`: parse throughput, best of N (§3.2);
  - `-mode errs-syntax|errs-goparser FILE`: a file's syntax errors as gc prints them with `-C`.
    syntax keeps one syntax error per line, as `base.ErrorfAt`; go/parser runs with
    `AllErrors`. Sorted by position.
  - `-mode accept FILES`: whether each parser accepts each file (E2).

  Built with `GOROOT=/root/tamago-go /root/tamago-go/bin/go build` (`CGO_ENABLED=0`).
- `gocmp`: `tools/gocmp` built the same way. E1 runs `gocmp errorcheck FILE OUT` in each
  program's directory, as goi's check does. Results are in `errs/results.tsv`.
- `goimod/`: a module `gps` with `syntax/` (the same copy) and `cmd/psyntax`, `cmd/pgoparser`
  (parse the arguments, count the nodes with `syntax.Inspect`/`ast.Inspect`, print the time).
  It was converted by `bin/repr-go read --goos linux --goarch amd64 --out goiforms` and moved
  under a `b_dir` directory so that goi's program mode finds the imported package as a
  directory test's. It was run as `GOI_RUN_EXE=target/seed/amd64/arbace bin/goi run goiroot
  go.b_dir.gps.cmd.psyntax FILES...` (E3). The same programs built natively are `psyntax`
  and `pgoparser`.
- `lex.ae`: E4, run as `target/seed/amd64/arbace .tmp/gps/lex.ae proc.go`.
- `size/`: E5's three programs.
- `std-files.txt`: `go list -f '{{range .GoFiles}}...'` std for tamago/amd64 (1,742 files).
  `dump1/` and `dump64/`: the helper over std at `-j 1` and `-j 64`. `corpus-amd64.log`,
  `read-amd64.log` and `read-amd64-full.log`: `bin/repr-go corpus amd64`, `read --corpus amd64`
  and `read --full --corpus amd64`, timed. `annot.log`: `bin/goi annotations tamago-amd64`.

## Decided (2026-10-20)

The user decided GPS1-GPS11 as recommended:
- GPS1 (c): Go's go/parser from `tamago/goroot`'s forms, run by Arbace.
- GPS2 (b): syntax errors from gc's syntax.
- GPS3 (a): annotations from Arbace's checker over raw forms; (b) at GP7 if the second conversion
  is measured to matter.
- GPS4 (b): interpreted from GP1, compiled at GP7.
- GPS5 (a): the dumper generated from go/ast's forms.
- GPS6 (a): the dump's data shape.
- GPS7 (a): native by default, the helper as the oracle.
- GPS8 (a): `--full` only.
- GPS9 (b): the 64 invalid programs checked on their raw forms.
- GPS10 (a): raw forms private.
- GPS11 (a): Go text at the REPL later.
The milestones are GP1-GP8 (§8).

## As built: GP1 (2026-10-20)

GP1, the tree (§8), is built on branch `go-parser-gp1` from main `26816f48`, as decided: GPS1 (c),
GPS4 (b) (interpreted), GPS5 (a), GPS6 (a). Only tools' namespaces and scripts changed: nothing of
`go/`, the table, goi, the checker, the converter or the helper; no re-seed. The tools' host was
the worktree's stage 0 (`target/seed/amd64/arbace`, 143,749,280 bytes), on the shared 64-core amd64
machine.

**What exists.**

| part | where | as built |
|---|---|---|
| the parser | `arbace/repr/go/parse.ae`, `arbace.repr.go.parse` (583 lines) | go/parser, go/scanner, go/ast and go/token interpreted by goi in the tools' host, from the executable's digest (below: equal to `tamago/goroot`'s forms); each file `parser.ParseFile(fset, path, src, ParseComments\|SkipObjectResolution)`, as the helper |
| the dumper | generated by `dumper-forms` from `tamago/goroot/go/go/ast/ast.ae` at load time | a Go package of Go forms (`arbace/repr/go/godump`, one file), checked and defined by goi once per process (`load!`, 2.2-2.6 s, mostly go/parser's own load). go/ast's node types are the structs whose pointer has `Pos` and `End` (57); each is a case of one type switch, its fields in go/ast's order, each of the helper's kinds (`fieldKind`: position, token, channel direction, string, bool, comment group, node, list of nodes), zero values left out, `Obj`, `Scope`, `Unresolved`, `Imports`, `Comments` not written; a type with a field of no kind (`ast.Package.Files`) is a panic when met, as the helper's `infoOf` (never: go/parser makes none). Hand-written beside the generated cases, after `tools/godump/dump.go`: `:dot` (`dotAfter`), positions `"L:C"` (`PositionFor(p, false)`), strings not valid UTF-8 as `(:bytes [...])`, the comment groups with `:text` on doc groups, the directives (`isDirective`, `ast.ParseDirective`) and the `/*line*/` warnings (`inExpr`). The tree is built directly as Arbace data through the package `arbace` (`arbace.Call` of a node maker, `vector`, `array-map`; keywords made once, as package variables) |
| the native dump | `native-dump` | a helper dump's header (§5's keys: package, configuration, directory, file lists, `:dir-test`, `:local-imports`) and its files parsed and dumped natively: `:name` (go/types' rule: the first package clause that is not `_`, `""` without files), `:files` (`:file`, `:go-version` by go/types' `FileVersions` rule: max(the file's `//go:build` version, go1.21), written when it differs from the package's), `:ast`, `:comments`, `:directives`, `:warnings` (when any). No annotations, `:imports`, `:errors`, `:init-order`, `:types`, `:type-table` (GP4) |
| E-tree | `check-dump`, `diff` | the helper's dump read (`read-dump`) and compared with the native one: `:name`, each file's `:file`, `:go-version`, `:ast` and `:comments`, the `:directives` and `:warnings`; annotations are metadata, ignored; strictly (a list, a vector and a map are different kinds, unlike `=`), the first difference reported with its path |
| the commands | `bin/repr-go` | `read --reader native [--tests] [--goos G --goarch A] [--out DIR] PKG...` and `read --reader native --corpus C`: the helper dumps (the files' lists: GPS14), the native reader parses, E-tree checks, the native dumps are written as text under `.tmp/repr-go/native/` (they read back with `read-dump`); `--reader helper` is the default. `native-check [--fresh] [CORPUS...]`: E-tree over every corpus (default all 22), the helper's dumps made when missing (`--fresh`: afresh), against `test/repr/go/native-known.edn` (empty), report `.tmp/repr-go/native-check.report.edn`; `corpus` split into `corpus_dump` (no change of behaviour) |
| the vendored trees' dumps | `bin/lib/repr-go/native-corpus` | the helper's dumps of the sources the vendoring tools convert, as they dump them, their pins read from the tools: TamaGo's goos module (`goos-amd64`, `goos-arm64`, `./...`), `GOROOT/src/cmd`'s 105 packages (`cmd-linux-amd64`, `cmd-tamago-amd64`, `cmd-tamago-arm64`), `box/` printed (`bin/box-vendor print`) for its five configurations with tests (`box`), purego renamed for darwin/arm64 (`purego`), the x509 bundle renamed with tests (`x509roots`) |
| the gate | `bin/gate --full` | a step `repr-go-native` after the `repr-go` step in its chain: `bin/repr-go native-check --fresh` (GPS8 (a), GPS15); the essential gate unchanged |
| the Makefile | `make repr-go-native-check` | `bin/repr-go native-check` |

**go/parser's forms.** goi interprets std's interpretable packages from the executable's digest
(GR8a), its own configuration's conversion: on the tools' host linux/amd64's, converted from the
same GOROOT/src as `tamago/goroot` (`bin/goi table`). `native-check` checks, at every run, that the
digest's go/parser, go/scanner, go/ast and go/token are `tamago/goroot`'s forms: 135, 50, 291 and 88
declarations, equal in order, metadata aside (`forms-check`). So the parser run is `tamago/goroot`'s
forms' code; in the box (GP8) the digest is tamago's, made from `tamago/goroot` itself (GPS12).

**E-tree (the exit criterion).** `bin/repr-go native-check` over the 22 corpora, every file of
every corpus of §6 that the helper dumps:

| corpus | dumps | files | nodes | Go | equal |
|---|---:|---:|---:|---:|---|
| std tamago/amd64 (`amd64`) | 379 | 1,742 | 2,308,308 | 19.6 MB | all |
| std tamago/arm64 | 378 | 1,738 | 2,304,970 | 19.6 MB | all |
| std linux/amd64 | 382 | 1,834 | 2,357,789 | 20.1 MB | all |
| std linux/arm64 | 380 | 1,829 | 2,355,930 | 20.1 MB | all |
| std with tests, tamago/amd64 (`tests`) | 554 | 2,935 | 4,299,718 | 32.2 MB | all |
| `$GOROOT/test`'s programs (`test`, `test-arm64`, `linux-test`, `linux-test-arm64`) | 1,707, 1,702, 1,713, 1,707 | 1,708, 1,703, 1,714, 1,708 | 396,632-398,760 each | 5.2-5.3 MB each | all |
| directory tests (`dirtest`, `-arm64`, `linux-`, `linux-…-arm64`) | 561, 559, 562, 559 | 586, 584, 587, 584 | 41,470-41,593 each | 0.3 MB each | all |
| error tests, linux/amd64 (`linux-errtest`) | 586 | 586 | 443,163 | 1.9 MB | all |
| TamaGo's goos module (`goos-amd64`, `goos-arm64`) | 94, 92 | 287, 288 | 85,959, 86,642 | 0.7, 0.8 MB | all |
| `tamago/cmd`'s sources (`cmd-linux-amd64`, `cmd-tamago-amd64`, `cmd-tamago-arm64`) | 105 each | 647 each | 3,197,904, 3,197,245, 3,197,245 | 21.2 MB each | all |
| `box/` printed, five configurations with tests | 41 | 217 | 160,362 | 1.0 MB | all |
| purego (darwin/arm64), x509roots (linux/amd64, tests) | 3, 1 | 27, 5 | 11,381, 1,954 | 0.1 MB, 37 KB | all |
| **total** | **12,275** | **22,603** | **25,763,858** | | **all equal** |

(Nodes are the dump's node forms, `arbace.repr.go.read/nodes`; E3's 4,844,260 for std counted
`ast.Inspect`'s calls, which include one `nil` call after each node's children.) `tamago/goroot` is
std tamago/amd64 and arm64 (the first two rows) and the goos module; `tamago/cmd` the three `cmd-`
corpora. The error tests are the 586 the helper dumps; the 73 that go/parser rejects have no dump
and are GP6's. The first run found one difference, fixed: `blank1.go` (`package _`), whose
package name go/types leaves `""` (`Checker.initFiles`: `invalid package name _`), and six
packages without files in std (`""` as well). `test/repr/go/native-known.edn` is empty. The whole
check took 17 min 18 s wall (126 CPU-min, 32 jobs), most of it reading the helper's dumps (1,951
CPU-s summed over the 22 corpora), the dumps being made beforehand.

**File lists (until GP2).** The native reader takes each package's files from the helper's dump
header (`:dir`, the `:files` entries in order): the helper (go list, `-files`, `-dirs`) stays the
loader, and E-tree compares only what the parser and the dumper make (GPS14). `:dir` is resolved
as the converter resolves it: `$GOROOT/...`, an absolute directory, or relative to `$GOROOT/test`.

**Times over std (tamago/amd64: 1,742 files, 19.6 MB).** `REPR_GO_JOBS=1 bin/repr-go read --reader
native --corpus amd64`, one thread, the machine otherwise lightly loaded:

| step | CPU, one thread | rate |
|---|---:|---:|
| parse (go/parser interpreted by goi; E3: 26.2 s) | 25.2 s | 0.78 MB/s |
| dump (the generated dumper, interpreted, building Arbace data) | 27.3 s | 0.72 MB/s |
| the native reader, total | 52.5 s | 0.37 MB/s |
| for comparison: reading the helper's 252.7 MB of dumps (E-tree's own cost) | 96.7 s | |
| the dumper's load (generated, checked, defined; go/parser's load within) | 2.2-2.6 s, once per process | |

At 32 jobs on the loaded machine the same corpus sums to 52.9 s of parse and 78.6 s of dump CPU (45 s
wall with E-tree). The dump costs about what the parse does, as §7 estimated ("the parse's order");
the parse and the dump together are about a sixth of today's reading of the dump's text for the
same packages (7 min 54 s CPU, §7). The native dumps written as text are 132 MB for std against the
helper's 252.7 MB (no annotations, no type table).

**Checks.** `bin/repr-go native-check`: 12,275 of 12,275 dumps equal (after the fix; `linux-errtest`
re-run, 586 of 586); `bin/repr-go read --reader native` on packages (`fmt`, `go/parser` for
linux/amd64) and on the corpus `amd64`, its native dumps read back by `read-dump`; `make
check-makefile`; the essential `bin/gate` passed (20 m 03 s, its build anew: the smoke test, Clojure's
suite with no regressions, the oracle 22,658 of 22,711 against its 53 known, the differential check
12,731 of 12,754 against its 23 known, goi's essential check 740 of 740, the generated variants).
Not run: `bin/gate --full`. The new namespace is embedded and compiled by the next build of the
executable as the other tools' are (`arbace.j2g.embed`); stage 0 loads it from the checkout.

**Proposals (GPS12-GPS15; each built as recommended).**

- **GPS12 Where goi takes go/parser's forms.**
  - (a) the executable's digest, its configuration's conversion of the same Go sources, checked
    equal to `tamago/goroot`'s declaration for declaration at every `native-check` (as built);
  - (b) `tamago/goroot`'s forms loaded in place of the digest's: goi's interpretable packages come
    only from the digest (`arbace.go.repl/load-import`), so a change to goi.

  *Recommended: (a)*: the same forms, checked; in the box the digest is `tamago/goroot`'s anyway.
- **GPS13 The dumper's forms.**
  - (a) generated in memory from go/ast's forms at each load (as built): no generated file, always
    in step with `tamago/goroot`;
  - (b) a committed generated file, checked as `bin/generated-check` checks the generated variants.

  *Recommended: (a)* until GP7, which puts the dumper in `go/` as Go forms: then (b).
- **GPS14 The files' lists in GP1.**
  - (a) the helper's dump headers (as built), the helper the loader until GP2;
  - (b) go list called by the native reader itself.

  *Recommended: (a)*: E-tree compares the trees only; (b) would be thrown away at GP2.
- **GPS15 `native-check` in `bin/gate --full`** (GPS8 (a)).
  - (a) `native-check --fresh`, a step after the `repr-go` step in its chain, so that it shares no
    dumps' directory with a running round trip or vendoring check (as built; about 17 minutes, of
    which about 3 the helper's dumps);
  - (b) without `--fresh`, on the dumps the chain left (the vendored trees' and some corpora's then
    possibly stale).

  *Recommended: (a)*.

## Decided (2026-10-20): GPS12-GPS15

The user decided GPS12-GPS15 as built (a):
- GPS12: go/parser's forms from the digest, checked against `tamago/goroot`'s.
- GPS13: the dumper generated in memory at each load, a committed and checked file at GP7.
- GPS14: file lists from the helper's headers until GP2.
- GPS15: `native-check --fresh` after the round trips in `--full`'s repr-go chain.

## As built: GP2 (2026-10-20)

GP2, the loader (§5, §8), is built on branch `go-parser-gp2` from main `c8287cf1`. The native
reader now takes its packages, their files and every dump's header from Arbace's own loader, not
from the helper: GPS14's interim is over, and `bin/repr-go read --reader native` runs without the
helper and without the go command. The tools' host was the worktree's stage 0
(`target/seed/amd64/arbace`, built from the seed by `bin/seed --build`), on the shared 64-core
amd64 machine.

**What exists.**

| part | where | as built |
|---|---|---|
| go list's part | `arbace/box/plan.ae`, `arbace.box.plan` (S8's planner, 346 lines before, 1,020 now) | generalized from the store's trees to directories: a tree is a store hash or a `java.io.File`. Ported: go/build's `matchTag`, `goodOSArchFile`, `shouldBuild` and `parseFileHeader` (`//go:build` by go/build/constraint's parser, `// +build` lines when there is none, `multiple //go:build comments`), `readGoInfo` (the import reader: the header through the imports, the package clause, each import spec's name and path, `isValidImport`; `//go:embed` patterns from every `//` comment of a file that imports embed, strings, raw strings and runes skipped, `ast.ParseDirective`'s arguments), `readComments` for the other files, and `Import` (`import-dir`: every list go list gives, `GoFiles` to `IgnoredOtherFiles`, `TestGoFiles`, `XTestGoFiles`, imports and embed patterns per kind, `documentation` packages, files importing `"C"` ignored with cgo off, `.S`/`.sx` ignored, `MultiplePackageError`, `NoGoError`); cmd/go's `resolveEmbed` (`resolve-embed`: `all:`, `path.Match` and `filepath.Glob`, `validEmbedPattern`, `isBadEmbedName` after `module.CheckFilePath`, nested modules, hidden files, sorted lists); `go.mod` (module, go line, local replacements); imports located in a module, GOROOT/src, std's `vendor/` for the standard library and `cmd/vendor/` for cmd (`locate`, `resolve-import`); `load-package` (a package as `go list -e` sees it, its imports resolved); `match-pattern` (`std`, `cmd`, `P/...`, as cmd/go's `matchPackages` walks: testdata, `_` and `.` directories, nested modules and modules' vendor directories pruned, `builtin` and, without cgo, `runtime/cgo` ignored, wildcards dropping directories without Go files); `list-packages` (`go list -e [-deps]`, go list's order) |
| the loader | `arbace/repr/go/load.ae`, `arbace.repr.go.load` (435 lines) | the helper's three modes over `arbace.box.plan`, each giving the dumps' keys, headers, directories and files (`jobs`): packages (`packageJobs`: patterns, `./...` and `.` in the working directory's module, `-tests`: the variant `p [p.test]` as `load.TestPackagesFor` makes it, of a package with in-package tests or of a main package with external ones, and `p_test`; `-deps` with a path prefix, for `tamago/cmd`), single-file programs (`fileJobs`: `MatchFile`, `FILE,FILE`, `fileKey`) and directory tests (`dirs.go` ported: the action line, `splitQuoted`, the flags, `MatchFile` on the test, grouping by package clause, `-s`, builddir's `.s` files, local imports resolved by gc's `-D`, runindir's module named by the directory with go line `-gomodversion` or 1.14, its packages in `go list -deps ./...`'s order). The header: `:config` (the configuration's tags; the tool tags as godump's: the architecture level's and go1.27.1's baseline experiments for the host the helper runs on, GPS20), `:dir` (`$GOROOT/...` or as given), `:for-test`, go list's lists, `:embed-files` with their SHA-256, `:dir-test`, `:local-imports`, the language version (std and cmd go1.27, a module's go line, runindir's), and `:imports` as go/types makes `Package.Imports`: each spec's path when `validatedImportPath` takes it (not renamed `init`, `"C"` not renamed), resolved as the importer would map it, as written when it does not resolve |
| the native reader | `arbace/repr/go/parse.ae` | `native-dump` takes a loader's job: its header, directory and files. E-tree (`tree-of`) compares the header (`header-keys`: `:godump`, `:package`, `:config`, `:dir`, `:for-test`, the file lists, the embedded files, `:dir-test`, `:local-imports`, `:imports`) as well as the name, the trees, comments, directives and warnings; the corpus check compares the sets of dumps too: a helper dump without a job is missing, a job without a helper dump is extra (accepted only when the helper's summary says it failed), a job go/parser rejects without a helper dump is rejected by both. `check --headers`: the headers and the sets alone, reading only the helper dumps' headers, no parse (GPS19). `read`: the native reader alone, no helper |
| the commands | `bin/repr-go` | a corpus is a spec (`$work/corpus/NAME.load.edn`: `{:name :dumps :tsv :loads [LOAD ...]}`, the helper's arguments as data; `native_spec`, and `bin/lib/repr-go/native-corpus` for the vendored trees, which also has `--src-only`); `read --reader native [--tests] [--goos G --goarch A] PKG...` and `read --reader native --corpus C` without the helper; `native-check [--fresh] [--headers] [CORPUS...]`, the known differences in `test/repr/go/native-known.edn` (empty) |
| the box's planner, checked | `bin/lib/repr-go/plan-check [--base REV]` | the plan of Arbace's program (`arbace/cmd/arbace`, tamago/amd64 and tamago/arm64) from store trees of GOROOT/src, TamaGo's goos module and the seed's program with `box/` printed into it, against tools/goplan's (go list -deps, as `bin/box-rebuild plan-check` compares) and against the planner of commit REV byte for byte |

**go list's part on the tools' host until a re-seed.** `arbace.box.plan` is embedded in the
program, and `arbace/box/` is not among RT's tool sources (`RT/toolSource`: j2g, repr, build, c2j,
go), so stage 0 would run its own copy, which has none of GP2's functions. The loader therefore
loads `arbace/box/plan.ae` from the checkout (ARBACE_PATH) when the embedded copy differs from it
(GPS16), as RT does for the tools' sources. After the next re-seed the copies are the same and
nothing is loaded.

**E-tree with the headers (the exit criterion).** `bin/repr-go native-check --fresh`, all 22
corpora, the helper's dumps made afresh:

| corpus | helper's dumps | native dumps | headers and trees equal | extra (the helper's failures) | rejected by both | load (s) |
|---|---:|---:|---|---:|---:|---:|
| std tamago/amd64, tamago/arm64 | 379, 378 | 379, 378 | all | 0 | 0 | 2.8, 3.0 |
| std linux/amd64, linux/arm64 | 382, 380 | 382, 380 | all | 0 | 0 | 2.6, 3.2 |
| std with tests, tamago/amd64 | 554 | 554 | all | 0 | 0 | 4.3 |
| `$GOROOT/test`'s programs, four configurations | 1,707, 1,702, 1,713, 1,707 | the same | all | 0 | 0 | 1.4-1.5 each |
| directory tests, four configurations | 561, 559, 562, 559 | the same | all | 0 | 0 | 2.2-2.5 each |
| error tests, linux/amd64 | 586 | 586 | all | 0 | 73 | 1.0 |
| TamaGo's goos module, amd64, arm64 | 94, 92 | 100, 100 | all 94, 92 | 6, 8 | 0 | 0.7, 1.0 |
| `tamago/cmd`'s sources, three configurations | 105 each | 105 each | all | 0 | 0 | 7.1, 9.1, 9.7 |
| `box/` printed, five configurations with tests | 41 | 41 | all | 0 | 0 | 2.3 |
| purego (darwin/arm64), x509roots (linux/amd64, tests) | 3, 1 | 3, 1 | all | 0 | 0 | 0.4, 0.1 |
| **total** | **12,275** | **12,289** | **12,275 of 12,275** | **14** | **73** | **about 66** |

No dump of the helper is missing, none differs, `test/repr/go/native-known.edn` stays empty.
22,603 files, 25,763,858 nodes, as GP1. The first comparisons found three differences of the
loader, fixed before this run: `builtin` and `runtime/cgo` listed by `std` (cmd/go's walk ignores
them); a main package without test files given a test variant (go list's `-test` makes one only for a
package with tests); `:imports` of `import6.go` and `fixedbugs/issue4517d.go` (go/types takes
each spec on its own, where go/build drops a file's imports at its first invalid path, and leaves
out an import named `init`).

**The file lists against go list.** The helper writes go list's own lists into its headers
(`fileLists`: `GoFiles` ... `XTestEmbedFiles`, verbatim), so every equal header is the loader's
lists equal to go list's for that configuration. `tamago/goroot` is std for tamago/amd64 and
tamago/arm64 (379 and 378 packages) and TamaGo's goos module (94 and 92); `tamago/cmd` the three
`cmd-` corpora, 105 packages each, whose package set the loader computes itself (`-deps` of
cmd/compile, cmd/link and cmd/asm, those of cmd) where the helper takes go list's: the sets are
equal, and so are all 315 headers. The 14 extra dumps of the goos module are packages for other
architectures that go list's `-export` cannot build for tamago/amd64 or arm64 (an `.s` file
gc's assembler rejects for the architecture, `undefined: reg.Out8`, ...): the helper fails them;
the native reader, which needs no export data, dumps them (GPS17). The 73 of `linux-errtest` are
the error tests go/parser rejects, the helper's failures too (GP6's).

**The box's planner.** `plan` keeps its output byte for byte. The one change of order the port
would have made, a directory pattern's embedded files sorted as cmd/go sorts them where the
planner listed them in its walk's order (`res/arbace/box.ae.z` before `res/arbace/box/build.ae.z`),
is kept as before for `plan` (`resolve-embed`'s `:walk-order`, GPS18); go list's `EmbedFiles` and
the headers are sorted. Checked with `bin/lib/repr-go/plan-check --base c8287cf1`: 325 packages
(tamago/amd64) and 324 (tamago/arm64), equal to tools/goplan's for every package and field, and
the plans byte for byte the planner's of `c8287cf1` (`:src` tree hashes included), in 22 s; the
planner itself takes 2.5 s where it took 4.2 s. What the box's plan now accepts that it refused:
glob patterns in `//go:embed` (cmd/go's resolution); what it now refuses beyond the old list:
`.cxx`, `.for`, `.f90`, `.swigcxx` and `.sx` files (go/build's lists; none in the box's trees).
`bin/box-rebuild plan-check` was not run (it needs a box run, `box-steps.edn`).

**The program changes.** `arbace.box.plan` and the new `arbace.repr.go.load` are embedded
namespaces (`arbace/box/` and `arbace/repr/` in the program's resources, compiled since S6), so
GP2 changes the program: the next re-seed carries it, to be batched (no re-seed here).

**Times.** The loader over each corpus is 0.1 to 9.7 s on one thread (about 66 s over
the 22, `load` above; cmd's `-deps` the longest, std 2.6 to 4.3 s), against the helper's go list,
0.3 s cached and about 6 s the first time for std. The whole `native-check --fresh` took 19 min
51 s wall (136 CPU-min, 32 jobs), the helper's dumps made afresh first (about 3 min), most of the
rest reading the helper's dumps (E-tree's own cost) and the interpreted parse and dump, as in
GP1. `native-check --headers` over the 22 corpora with the helper's dumps present: 1 min 16 s.
`read --reader native --goos linux fmt go/parser`: 5.8 s, of which 2.2-2.6 s the dumper's load.

**Checks.** `bin/repr-go native-check --fresh`: 12,275 of 12,275 dumps equal, headers and trees,
0 not known (before the work, the same at `c8287cf1`'s GP1: 12,275 of 12,275 trees, 18 min 36 s);
`native-check --headers`: 12,275 of 12,275; `bin/repr-go read --reader native` on packages
(`fmt`, `go/parser` for linux/amd64, from outside the checkout) and on the corpus `x509roots`,
without the helper; `bin/lib/repr-go/plan-check --base c8287cf1` (above); `make check-makefile`;
the essential `bin/gate` passed (18 m 52 s, its build anew since `arbace/` changed: the smoke
test, Clojure's suite with no regressions, the oracle 22,658 of 22,711 against its 53 known, the
differential check 12,731 of 12,754 against its 23 known, goi's essential check 740 of 740, the
generated variants). Not run: `bin/gate --full`, `bin/box-rebuild`.

**What is not ported.** Symbolic links (the trees read have none; jrt has no
`Files.isSymbolicLink`), workspaces (`go.work`), modules from the module cache (`require`
without a local replacement: no corpus imports one), modules' `vendor/` directories (only std's
and cmd's), go.mod's `ignore`, cgo on, `-mod`, binary-only packages, import comments, go list's
errors that need more than go/build (import cycles, `use of internal package not allowed`,
`-export`'s build failures: GPS17). `:imports` of a package (not of the single-file programs or
directory tests) is taken from go/build's imports, which leave out a file whose import path is
invalid: such a package is go list's error and the helper's failure anyway.

**Proposals (GPS16-GPS20; each built as recommended).**

- **GPS16 Where the tools' host takes the generalized planner from, until a re-seed.**
  - (a) the loader loads `arbace/box/plan.ae` from the checkout when the embedded copy differs
    (as built): no runtime change, the copy loaded is the one the next program embeds;
  - (b) `arbace/box/` among RT's tool sources: a change to the runtime, and the box's own
    namespaces would load from a checkout where one is on ARBACE_PATH;
  - (c) go list's part moved to a tools' namespace (`arbace.repr.go.list`) that the box's
    planner requires: the box would depend on repr/go's namespaces.

  *Recommended: (a)*.
- **GPS17 Packages the helper cannot dump.** go list's `-export` fails to build some packages of
  a corpus (TamaGo's goos module for another architecture: 6 for amd64, 8 for arm64), so the
  helper fails them, while the native reader needs no export data.
  - (a) the loader lists them as go list does, the native reader dumps them, and native-check
    reports them as extra, accepted because the helper's summary says it failed (as built);
  - (b) the loader drops them: it would have to compile them, or to model gc's assembler.

  *Recommended: (a)*: which of them the vendoring converts is GP5's question, as now.
- **GPS18 The order of a directory pattern's files in the box's plan.**
  - (a) the walk's order, as before GP2 (as built): the box's plans byte for byte the same;
  - (b) sorted as cmd/go sorts `Internal.Embed`'s lists: the box's `-embedcfg` would be go
    build's, the objects the same (gc sorts the files itself, `staticdata.embedFileList`), the
    compile's action key of `arbace/cmd/arbace` changed once.

  *Recommended: (a)* now; (b) when the box's driver changes for another reason.
- **GPS19 A check of the loader alone.**
  - (a) `native-check --headers`: the headers and the sets of dumps, no parse, about a minute
    over the 22 corpora with the helper's dumps present (as built), by hand;
  - (b) the same in the essential gate: it needs the helper's dumps, so the go command.

  *Recommended: (a)*: the full check (GPS15) covers it in `bin/gate --full`.
- **GPS20 The experiments' tool tags in `:config`.** The helper writes go/build's
  `Default.ToolTags` of its own process, the baseline experiments of the host it runs on
  (linux/amd64: `goexperiment.dwarf5` and the rest), also for a corpus of darwin/arm64
  (purego), where the go command's would leave out `dwarf5`.
  - (a) the loader computes them as the helper does, for the host (as built): E-tree compares
    like with like;
  - (b) the target's, in the helper and the loader: a change to the dumps and the round trips'
    references, for a tag that only `internal/goexperiment`'s `exp_dwarf5_on.go` and
    `exp_dwarf5_off.go` test, in std, which no darwin corpus has.

  *Recommended: (a)* until GP5 makes the native reader the default; then (b) can be decided
  with the helper as the oracle.

## Decided (2026-10-20): GPS16-GPS20

The user decided GPS16-GPS20 as built, each (a):
- GPS16: the checkout's `plan.ae` used until the re-seed.
- GPS17: the helper's failures dumped natively, reported as extra.
- GPS18: walk order for the box plan's embedded files.
- GPS19: `native-check --headers` by hand.
- GPS20: `:config`'s experiment tags as the helper's until GP5.

## As built: GP3 (2026-10-20)

GP3, raw forms (§4.2 step 2, §8), is built on branch `go-parser-gp3` from main `02d20ed1`, as
decided: GPS3 (a) (raw forms, then the checker, then the converter unchanged) and GPS10 (a)
(private, documented in doc/go/CONVERTER-NOTES.md, "Raw forms", not a dialect of SPEC). The tools'
host was the worktree's stage 0 (`target/seed/amd64/arbace`, 143,749,280 bytes, built by `bin/seed
--build`), on the shared 64-core amd64 machine (load average 15 to 25 during the runs).

**What exists.**

| part | where | as built |
|---|---|---|
| the raw mode | `arbace/repr/go/convert.ae` (`*raw*`, option `:raw` of `convert-package`) | `an` reads no annotation in raw mode; at each decision of SPEC §11.1 that needs types the converter writes an open form instead (`raw-call`, `raw-select`, `go/index`, `go/star`, `go/id`, `^:go/define`, `^:go/key`, no `:val`, `:tag`, `:inst`, `:go/via`, implicit import name or `:init-order`), marked in memory with the private `::open` (its kind); `open-counts` counts them; mode `:full`'s positions always. Invalid programs' constructs: `go/type-guard`, `go/define`, `go/literal`, a non-name where a name is required as its expression |
| the shape | doc/go/CONVERTER-NOTES.md, "Raw forms" | what is open and how each decision is marked (12 kinds), what stays decided from the syntax, the invalid programs' marks |
| the commands | `arbace/repr/go/raw.ae`, `arbace.repr.go.raw` (new, 535 lines); `bin/repr-go` | `read --reader native --raw [--corpus C \| PKG ...]`: the loader's jobs, the native dumps, the raw forms written as `.ae` (`arbace.repr.go/convert-data`, the read-back check against the forms in memory), the open decisions counted per kind; `raw-check [--fresh] [CORPUS ...]` (`make repr-go-raw-check`): the raw forms against the final ones (below), known differences `test/repr/go/raw-known.edn` (empty) |
| `arbace.repr.go` | `arbace/repr/go.ae` | `convert-dump` split into `dump-context` (a dump's namespace, its directory test's namespaces, its source directory and text) and `convert-data` (a dump in memory converted, written and read back), so that the native reader's dumps convert as the helper's do; the output unchanged |
| the statement lists | `convert.ae`, `stmts-forms` | a loop over the statements that do not declare, recurring only into a `let`'s body (GPS24): the recursion per statement overflowed the stack on `fixedbugs/bug385_64.go` (16,480 statements) when the converter is evaluated, as it is until a re-seed |

**Every corpus converted raw, read back (the exit criterion).** `bin/repr-go read --reader native
--raw --corpus C` over the 22 corpora of native-check: every dump the loader gives and go/parser
accepts converts raw, and every file reads back with Arbace's reader as the forms in memory. The
open decisions per kind and per corpus (`:init-order`: one per package):

| corpus | dumps | files | MB | `:call` | `:select` | `:index` | `:star` | `:id` | `:define` | `:key` | `:val` | `:shift` | `:any` | `:import` | `:init-order` | invalid | total |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| amd64 | 379 | 1,742 | 108.2 | 125,373 | 148,710 | 14,394 | 3,998 | 31,156 | 38,950 | 22,393 | 9,143 | 4,821 | 146 | 5,523 | 379 | 0 | 404,986 |
| arm64 | 378 | 1,738 | 108.0 | 125,186 | 148,515 | 14,382 | 3,988 | 31,188 | 38,881 | 22,330 | 9,048 | 4,712 | 146 | 5,518 | 378 | 0 | 404,272 |
| linux-amd64 | 382 | 1,834 | 110.4 | 129,032 | 150,679 | 14,684 | 4,131 | 31,773 | 39,878 | 22,296 | 10,972 | 4,891 | 147 | 5,743 | 382 | 0 | 414,608 |
| linux-arm64 | 380 | 1,829 | 110.3 | 128,954 | 150,590 | 14,706 | 4,127 | 31,818 | 39,816 | 22,237 | 11,063 | 4,824 | 147 | 5,748 | 380 | 0 | 414,410 |
| tests | 554 | 2,935 | 187.8 | 276,867 | 281,248 | 21,029 | 6,188 | 62,777 | 89,765 | 58,004 | 11,015 | 6,489 | 230 | 12,409 | 554 | 0 | 826,575 |
| test | 1,707 | 1,708 | 22.1 | 33,782 | 7,773 | 4,382 | 782 | 1,998 | 5,267 | 687 | 629 | 1,576 | 521 | 1,068 | 1,707 | 0 | 60,172 |
| test-arm64 | 1,702 | 1,703 | 22.0 | 33,763 | 7,767 | 4,379 | 782 | 1,997 | 5,263 | 687 | 629 | 1,571 | 521 | 1,067 | 1,702 | 0 | 60,128 |
| linux-test | 1,713 | 1,714 | 22.1 | 33,950 | 7,851 | 4,414 | 784 | 2,017 | 5,346 | 699 | 633 | 1,600 | 521 | 1,087 | 1,713 | 0 | 60,615 |
| linux-test-arm64 | 1,707 | 1,708 | 22.1 | 33,862 | 7,831 | 4,383 | 784 | 2,008 | 5,327 | 699 | 633 | 1,573 | 521 | 1,084 | 1,707 | 0 | 60,412 |
| dirtest | 561 | 586 | 1.8 | 2,189 | 1,713 | 160 | 73 | 275 | 582 | 63 | 30 | 1 | 195 | 415 | 561 | 0 | 6,257 |
| dirtest-arm64 | 559 | 584 | 1.8 | 2,186 | 1,712 | 160 | 73 | 275 | 581 | 63 | 30 | 1 | 195 | 414 | 559 | 0 | 6,249 |
| linux-dirtest | 562 | 587 | 1.8 | 2,197 | 1,719 | 161 | 73 | 275 | 584 | 63 | 30 | 1 | 195 | 419 | 562 | 0 | 6,279 |
| linux-dirtest-arm64 | 559 | 584 | 1.8 | 2,186 | 1,712 | 160 | 73 | 275 | 581 | 63 | 30 | 1 | 195 | 414 | 559 | 0 | 6,249 |
| linux-errtest | 586 | 586 | 17.4 | 5,098 | 1,721 | 668 | 575 | 524 | 1,890 | 118 | 578 | 49,950 | 30 | 155 | 586 | 4 | 61,897 |
| goos-amd64 | 100 | 324 | 4.2 | 5,217 | 7,378 | 79 | 80 | 649 | 1,043 | 434 | 2,841 | 502 | 0 | 759 | 100 | 0 | 19,082 |
| goos-arm64 | 100 | 322 | 4.2 | 5,217 | 7,378 | 79 | 80 | 649 | 1,043 | 434 | 2,841 | 502 | 0 | 759 | 100 | 0 | 19,082 |
| cmd-linux-amd64 | 105 | 647 | 139.2 | 175,232 | 314,686 | 39,750 | 1,197 | 43,021 | 93,914 | 111,638 | 22,890 | 6,722 | 5 | 2,949 | 105 | 0 | 812,109 |
| cmd-tamago-amd64 | 105 | 647 | 139.2 | 175,178 | 314,615 | 39,747 | 1,195 | 43,000 | 93,898 | 111,632 | 22,889 | 6,720 | 5 | 2,935 | 105 | 0 | 811,919 |
| cmd-tamago-arm64 | 105 | 647 | 139.2 | 175,178 | 314,615 | 39,747 | 1,195 | 43,000 | 93,898 | 111,632 | 22,889 | 6,720 | 5 | 2,935 | 105 | 0 | 811,919 |
| box | 41 | 217 | 6.6 | 11,023 | 12,944 | 1,011 | 220 | 3,080 | 4,399 | 893 | 1,249 | 380 | 0 | 903 | 41 | 0 | 36,143 |
| purego | 3 | 27 | 0.5 | 1,064 | 915 | 118 | 105 | 57 | 176 | 49 | 23 | 35 | 0 | 36 | 3 | 0 | 2,581 |
| x509roots | 1 | 5 | 0.1 | 18 | 34 | 0 | 0 | 4 | 8 | 482 | 0 | 0 | 0 | 10 | 1 | 0 | 557 |
| **total** | 12,289 | 22,674 | 1,170.8 | 1,482,752 | 1,892,106 | 218,593 | 30,503 | 331,816 | 561,090 | 487,596 | 130,085 | 103,592 | 3,725 | 52,350 | 12,289 | 4 | 5,306,501 |

The kinds are CONVERTER-NOTES' table's: `:call` calls of unknown kind, `:select` selectors,
`:index` index or instantiation, `:star` indirection or pointer type, `:id` the names `true`,
`false`, `nil`, `:define` names of `:=`, `:key` identifier keys of composite literals, `:val`
constant names, `:shift` shifts, `:any` constraints `any`, `:import` implicit import names; the
column invalid counts the invalid programs' marks, all in `linux-errtest`: 2 `x.(type)` outside a
type switch, 1 `:=` in a post statement, 1 malformed literal.

**Invalid programs (GPS9 (b)).** Of `linux-errtest`'s 659 error tests, the loader gives 586
programs that go/parser accepts and 73 it rejects (GP6's, by gc's syntax). All 586 get raw forms,
which read back. Among them are GC47's 64, which the final conversion cannot convert; `raw-check`
lists exactly these 64 as raw only, the same names as `test/goi/check-linux-amd64.edn`'s "repr/go
cannot convert". Their raw forms: a selection without an object is a `go/select` like any other
(39), a constant without a value a name without `:val` (10), an import path that is not a name
`["path"]`, `(x) := 0` and `main() :=` their expressions' forms (6), `x.y` in a type context
`x/y` (4), `x.(type)` outside a type switch `(go/type-guard x)` (2), a conversion of two arguments
a call (1), `for ;; x := 1` `(go/define x 1)` (1), `6e5518446744` `(go/literal "FLOAT"
"6e5518446744")` (1). `fixedbugs/bug340.go`'s `case 0:` in a type switch is the literal's form.

**The check against the final forms (`raw-check`).** For each dump, its native dump converted raw
and the helper's annotated dump converted as ever (mode `:full`) walk in parallel
(`arbace.repr.go.raw/conform`): every form, symbol, literal and metadata key (`::l`, `::pos`,
`::full` included) equal, but at the open decisions, where the final form must be one of that
kind's resolutions, which are counted. Erasing go/types' decisions from the final forms thus gives
the raw forms exactly. Over the 22 corpora:

- 12,158 dumps resolved, 0 differ, 0 failed; 117 raw only: GC47's 64 and 53 packages of TamaGo's
  goos module for other boards (24 for amd64, 29 for arm64: the helper dumps them with type
  errors, the final conversion stops at `selector`); 14 without the helper's dump (GPS17), raw
  only too; 73 rejected by go/parser.
- The resolutions, summed: `:call` 542,581 calls, 535,915 method calls, 207,896 builtin calls,
  192,589 conversions (1 dropping an invalid `...`, GPS25); `:select` 959,460 fields or method
  values, 535,915 methods called, 391,171 package members, 1,242 method expressions; `:index`
  214,169 indexes, 4,030 instantiations, 365 generic types; `:star` 20,378 indirections, 10,021
  pointer types; `:id` 331,359 the universe's, 101 declared; `:define` 554,398 new, 5,848
  reused; `:key` 474,025 fields, 12,931 expressions; `:val` 127,470 values, 64 none; `:shift`
  2,294 tagged, 100,865 not; `:any` 3,665 the universe's, 60 declared; `:import` 51,631; besides
  them the final forms' `:inst` (12,726) and `:go/via` (72,963), and 44 `for true {}`.
- A negative check: the final forms of `strings` with one symbol renamed, or one form's positions
  moved, do not conform; the raw forms against themselves do not either (the final forms carry no
  `::open`).

The first runs found five differences, fixed: a `^:go/key` vector compared without dropping its
mark; a generic type instance's count; the operand of an instantiation, which the final forms
write without its parentheses and positions (accepted, GPS25); the final `...` of a conversion
and of `nil` (accepted, GPS25); `r.true` (a field named `true`), whose raw name was the symbol
`true`, which reads back as a boolean (now `(go/id "true")`, as the final forms name it).

**The normal conversion is unchanged.** The raw mode is additive: it is a binding the normal path
never sets, and `an` is the annotations when it is not set. Checked byte for byte: `bin/repr-go
read --corpus C` and `read --full --corpus C` of the 14 corpora of repr/go's own (std for four
configurations, std with tests, `$GOROOT/test`'s programs and directory tests for four
configurations each, the error tests), 28 conversions, before (main `02d20ed1`, a pristine copy
of its tree on the same stage 0 and the same helper's dumps) and after: every `.ae` file equal
(62,878 files, 0 differ; the error tests' 64 failures the same). The round trips' references are
functions of these forms (`bin/repr-go roundtrip` prints and judges them), so they hold
unchanged; run as well: `bin/repr-go roundtrip --dirs amd64` (no regressions against its
reference), `roundtrip --text --dirs amd64` (586 of 586 files byte-identical) and `roundtrip
--text amd64` (std 1,742 of 1,742, `$GOROOT/test` 1,705 of 1,708, the 3 known). GPS24's loop changes no output: it is among the 28.

**Times.** The raw conversion runs on stage 0, which evaluates `arbace.repr.go.convert`,
`arbace.repr.go` and the new `arbace.repr.go.raw` from the checkout until a re-seed (their
compiled copies are main's). `read --reader native --raw` over the 22 corpora: 42 min 4 s wall
(24 jobs, 270 CPU-min), of which the interpreted parse 526 CPU-s, the dump 701 CPU-s, and the raw
conversion with its layout and read-back check 11,532 CPU-s (std tamago/amd64: 56, 75 and
1,375 CPU-s, 124 s wall; the helper's path converts the same corpus in mode `:full`, compiled, in
about 112 s wall at 24 jobs). `raw-check` over the 22 corpora: 43 min 25 s wall (300
CPU-min, 32 jobs), the three `cmd-` corpora 9 to 10 min each (the long pole their largest
package, `cmd/compile/internal/ssa`, 12.4 MB of Go, one job), std 1.7 to 2.2 min each.

**The program changes.** `arbace.repr.go.convert` and `arbace.repr.go` are embedded namespaces,
and `arbace.repr.go.raw` is a new one under `arbace/repr/`: the next re-seed carries them, to be
batched (no re-seed here). Until then stage 0 loads them from the checkout.

**Checks.** `bin/repr-go raw-check` (above); `bin/repr-go read --reader native --raw` over the
22 corpora (above) and on packages (`fmt`, `strings` for linux/amd64); the 28 conversions before
and after; `make check-makefile`; the essential `bin/gate` passed (20 m 50 s, its build anew since
`arbace/` changed: the smoke test, Clojure's suite with no regressions, the oracle 22,658 of 22,711
against its 53 known, the differential check 12,731 of 12,754 against its 23 known, goi's
essential check 740 of 740, the generated variants). Not run: `bin/gate --full`.

**For GP4.** The checker's resolve mode takes the raw forms in memory (GPS26) and answers by
position: an open form carries its node's positions (`::pos`, `:go/pos` in the text), a name its
`:name-pos`. It must accept the heads `go/select`, `go/index`, `go/star` and `go/id` (resolved by
the checker's own rules for `pkg/Name`, `(.-f x)`, `(.M x)`, `method-expr`, `aget`, `inst`,
`deref` and `*`, and scopes), calls whose head is any form (`expr-or-type` decides conversion,
builtin, method or function), `^:go/define` names (new or reused, as go/types' Defs and Uses),
`^:go/key` vectors (field or key, by the literal's type), constant names without `:val`, shifts
without `:tag`, `^any` constraints, `["path"]` imports and a `go/package` without `:init-order`.
Its answers are then the annotations the converter reads, and `raw-check`'s resolutions are
what E-ann will compare kind by kind.

**Proposals (GPS21-GPS26; each built as recommended).**

- **GPS21 How raw forms mark what is open.**
  - (a) heads of their own for the open forms (`go/select`, `go/index`, `go/star`, `go/id`), public
    keys for the open names and keys (`^:go/define`, `^:go/key`), the typed metadata left out
    (`:val`, `:tag`, `:inst`, `:go/via`, an implicit name, `:init-order`), a call as `(F a ...)`;
    and the private `::open` in memory (as built);
  - (b) one wrapper `(go/open :kind form)` around each open form: uniform, but 5.3 million
    wrappers over the corpora (1.5 million calls, 1.9 million selectors), each to unwrap;
  - (c) only the private `::open`, no mark in the text: the text would not say what is open
    (`(len x)`, a `[k v]` key), so it could not be checked or read as raw forms.

  *Recommended: (a)*: the raw forms keep the final forms' shape wherever the syntax decides, and
  each open decision is visible in the text and countable in memory.
- **GPS22 Calls of names that are builtins' or heads of the syntax.**
  - (a) a raw call is never classified: `(len x)` calls the identifier `len`, whatever it denotes,
    and only a name that is a head of the forms' syntax (`fn`, `do`, `slice`, `assert`, ...) is
    written `(go/call h ...)` (as built; `raw-reserved-heads`);
  - (b) every raw call `(go/call F ...)`: no reserved names, but 1.5 million extra heads, and the
    final forms' builtin calls would differ in shape from their raw ones.

  *Recommended: (a)*.
- **GPS23 Where `raw-check` runs.**
  - (a) by hand at milestones (as built; 43 min over the 22 corpora with the helper's dumps
    present);
  - (b) a step of `bin/gate --full` after native-check: 43 more minutes, the helper's dumps read a
    second time;
  - (c) merged into native-check at GP4: one read of the helper's dumps for E-tree, the raw forms'
    check and E-ann, which compares the checker's answers with go/types' at the same open
    decisions.

  *Recommended: (a)* now, (c) at GP4.
- **GPS24 The statement lists' recursion.** `stmts-forms` recurred once per statement: on stage
  0, which evaluates the converter until a re-seed, `fixedbugs/bug385_64.go` (16,480 statements)
  overflowed the stack, raw and final alike (the compiled converter of main converts it).
  - (a) a loop over the statements that do not declare, recurring only into a `let`'s body (as
    built): the same forms, byte for byte over the 28 conversions;
  - (b) unchanged, the program known in `raw-known.edn` until a re-seed compiles the converter.

  *Recommended: (a)*: GPS9 (b) needs raw forms for every error test, and the loop is no
  slower.
- **GPS25 What the final forms write less precisely than the raw ones.** Three cases, which
  `conform` accepts as the final's: an instantiation's operand without its parentheses, and as a
  field's selector or a method expression without the name's position (`instantiation` builds its
  operand apart from `selector-form`; 4,030 instantiations, the few with such operands); a
  conversion's argument without an invalid `...` (`unsafe.Pointer(&x...)`, `ddd1.go`, already a
  known errorcheck divergence of goi); a `...` after the universe's `nil` without a line
  (`f(nil...)`, 2 error tests).
  - (a) accepted by `conform`, the final converter unchanged (as built: GP3 additive, the round
    trips' references unchanged);
  - (b) the final converter corrected now: the forms of those files change, and with them, for the
    first, mode `:full`'s positions.

  *Recommended: (a)* now; (b) at GP5, when E-forms makes the native path's forms the reference.
- **GPS26 How GP4's checker takes the raw forms.**
  - (a) in memory, from the converter in the same process: the forms, their `::open` and their
    positions as data; the `.ae` text for the read-back check and for reading by hand (as built:
    `read --raw` writes it, `raw-check` and GP4 take the forms in memory);
  - (b) through the `.ae` files, read back: the text carries the marks and positions, not `::open`,
    and costs a write and a read per package.

  *Recommended: (a)*.
