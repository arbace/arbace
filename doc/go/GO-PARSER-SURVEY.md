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
