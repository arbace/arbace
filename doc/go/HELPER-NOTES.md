# g2c's front end helper: notes

`tools/godump` is the front end of g2c (Go as Arbace forms), milestone G1 of
[G2C-SURVEY.md](../G2C-SURVEY.md) §7. It is a Go program, Arbace's own code under the EPL like
the rest, using only Go's standard library (BSD-licensed APIs, no third-party modules). It was
seeded from the experiment `godump` of survey §2.4 and §10. It loads Go packages for one build
configuration, type-checks them with `go/types`, and writes per package one forms file. The
helper is dumb and complete: it writes the typed AST generically, plus what `go/types` decided.
The converter (`arbace.g2c`, in Clojure, to come) makes every decision about forms.

## Building and running

```sh
bin/g2c build                        # .tmp/godump/godump, built with TamaGo's go for the host
bin/g2c dump fmt runtime             # .tmp/godump/out/fmt.edn, .tmp/godump/out/runtime.edn
bin/g2c dump -goarch arm64 -o DIR std
bin/g2c dump -files prog.go a.go,b.go   # single-file programs; a.go,b.go is one program
bin/g2c read .tmp/godump/out         # read on Arbace (needs bin/build-arbace), print a summary
bin/g2c corpus [amd64|arm64|test]    # the coverage runs below, into .tmp/godump/corpus/
```

- **Toolchain.** `G2C_GOROOT` names TamaGo's Go tree (default `/root/tamago-go`). The system Go
  (plain go1.27.1) lacks `GOOS=tamago`. `bin/g2c` builds the helper with `$G2C_GOROOT/bin/go`
  for the host (it runs at conversion time, like javac for j2c). It then runs the helper with
  `-go $G2C_GOROOT/bin/go`, which the helper uses for `go env` and `go list`.
- **Pinned Go.** The helper checks that it was built with go1.27 (its `go/types` is the
  language) and that the go command is go1.27.1. It type-checks at the package's language
  version: `go1.27` for std and single files, the `go` line of a module otherwise. It refuses a
  module or file whose language version is newer than go1.27. Files whose build constraint
  needs a newer release (`//go:build go1.28`) are excluded by the configuration, as the go
  command does. Moving the pin is a recorded decision (journal, 2026-10-07).
- **Configuration.** There is one per run, written into every dump: `-goos` (default
  `tamago`), `-goarch` (`amd64` or `arm64`) and `-tags`. `CGO_ENABLED=0`, `GOFLAGS` is
  emptied, and `GOTOOLCHAIN=local`. `GOAMD64`/`GOARM64` and `GOEXPERIMENT` come from `go env`
  and are recorded.
- **Loading.** One `go list -e -json -export -deps` gives the files of each package for the
  configuration (`GoFiles`, `CgoFiles`) and the export data of every dependency. Each package
  is parsed (`parser.ParseComments|SkipObjectResolution`). It is checked by `types.Config` with
  `importer.ForCompiler(fset, "gc", lookup)`, which reads `go list`'s export data through the
  package's `ImportMap` (vendored std packages). `Sizes` is `types.SizesFor("gc", GOARCH)`, so
  `unsafe.Sizeof` folds as gc folds it. Packages are dumped in parallel (`-j`, default
  GOMAXPROCS), each with its own `FileSet` and importer. Single files (`-files`) are matched
  against the configuration with `go/build`'s `MatchFile` (build constraints and file name),
  and their imports are listed in one `go list`.
- **Output.** `-o DIR` (default `godump.out`; `bin/g2c` uses `.tmp/godump/out`). A package
  goes to `DIR/IMPORT/PATH.edn`, and a single file to `DIR/FILE.go.edn` (relative to
  `$GOROOT/test`, `$GOROOT` or the working directory). `.edn` follows `test/*.edn`: the
  format targets the Clojure reader, not strict EDN. Dumps are regenerated, not tracked
  (journal, 2026-10-07).
- **Summary.** On stdout, one line per package:
  `STATUS<TAB>KEY<TAB>FILES<TAB>BYTES<TAB>MS<TAB>TYPE-ERRORS[<TAB>FIRST-MESSAGE]`. STATUS is
  `ok`, `errors` (type errors; the dump is still written), `fail` (no dump: parse errors, `go
  list` errors, a newer language version) or `excluded` (a single file the configuration
  leaves out). On stderr, the totals. The exit status is 1 if anything has errors or failed.
- **Determinism.** The walk follows AST order. Method sets, init order and imports come out of
  `go/types` sorted or in a defined order. No map is iterated where order matters. Two runs
  give byte-identical dumps, whatever `-j`: checked over std on amd64 and arm64 and over the
  test programs.

## Output format

One form per package, read with `read` and `*read-eval*` false (`arbace.g2c.read/read-dump`):

```clojure
;; godump: the typed AST of a Go package for arbace.g2c (doc/go/HELPER-NOTES.md)
{:godump 1                                  ; format version
 :package "fmt" :name "fmt"                 ; import path ("command-line-arguments" for files)
 :config {:goos "tamago" :goarch "amd64" :goamd64 "v1" :tags [] :tool-tags [...]
          :goexperiment "" :cgo false :compiler "gc" :toolchain "go1.27.1"
          :go-version "go1.27" :word-size 8 :max-align 8}
 :dir "$GOROOT/src/fmt" :go-files [...] :s-files [...] :ignored-go-files [...]
 :embed-patterns [...] :embed-files [...]   ; from go list, when not empty
 :imports ["errors" "internal/fmtsort" ...] ; resolved paths, sorted
 :errors []                                 ; type errors, "file.go:L:C: message"
 :files [{:file "print.go" [:go-version "go1.21"]   ; when the file's differs (//go:build)
          :ast (:file {...})
          :comments [["L:C" "// text"] ...]}  ; every comment, outside the tree
         ...]
 :directives [["print.go:L:C" "//go:linkname ..."] ...]
 :init-order [{:lhs [["name" "file.go:L:C"] ...] :rhs "file.go:L:C"} ...]
 :types [{:name ... } ...]}
```

**The tree.** Every `go/ast` node is `(:kebab-type {:kebab-field value ...})`, with the fields
in `go/ast`'s order, for example `(:binary-expr {:x ... :op-pos "3:9" :op "+" :y ...})`. Values:

- nodes, and vectors of nodes;
- positions, `"LINE:COL"` in the file being written (raw positions, never adjusted by `//line`);
- tokens as their Go spelling (`"+="`, `"import"`, `"INT"`);
- `ast.ChanDir` as `:send`, `:recv` or `:both`;
- strings (identifiers, literals' source text) and `true`;
- `:doc` and `:comment` (comment groups) as the position of the group's first comment in
  `:comments`.

A field at its zero value is omitted (nil, false, empty string, no position, `ILLEGAL` token).
Not written: the parser's resolution (`Obj`, `Scope`, `Unresolved`), `File.Imports` (the
import specs again) and `File.Comments` (written beside the tree as `:comments`, every comment
with its position, so that a printer can restore them). Declarations start on their own line.

**Annotations** are reader metadata on the node's form, where `go/types` recorded something
(survey §2.2):

| key | on | value |
|---|---|---|
| `:mode` | expressions | `:const`, `:var` (addressable), `:mapindex`, `:value`, `:commaok`, `:type`, `:builtin`, `:void`, `:nil` |
| `:t` | expressions | the type, `types.TypeString` qualified by package path (none for the package dumped) |
| `:val` | constants | the exact value: integer (BigInt when large); float as integer or ratio (exact); `(:float "0x.8p+1")` beyond 2^±16384; string, or `(:bytes [b ...])` when not UTF-8; boolean; `(:complex RE IM)` |
| `:def`, `:use` | identifiers | the object (below) |
| `:inst` | identifiers of generic uses | `{:targs ["int" ...] :t "func(int) int"}` |
| `:sel` | selector expressions | `{:kind :field/:method/:method-expr :path [i ...] :indirect B :recv "T"}`: the index path through embedded fields (the last index is the field or method), and whether a pointer is followed |
| `:implicit` | import specs, type switch clauses, anonymous fields | the object declared implicitly |

An object is `{:kind K :name N ...}`, with K one of `:var`, `:field`, `:func`, `:const`, `:type`,
`:pkg`, `:builtin`, `:label` or `:nil`. It may carry:

- `:pkg`, the path of its package, when it is not the package dumped;
- `:decl`, its position (`"L:C"` in the same file, `"file.go:L:C"` otherwise), for objects of
  the package dumped. The position is the object's identity;
- `:t`, its type;
- `:pkg-level true` for package-scope objects and `:universe true` for predeclared ones;
- `:recv`, a method's receiver type;
- `:embedded true` on embedded fields;
- `:alias true` and `:type-param true` on type names;
- `:path`, a package name's import path;
- `:val`, a constant's value, on definitions only. Uses carry it as the expression's `:val`.

**The package view:**

- `:init-order`: `go/types`' `InitOrder`, the spec's dependency order of package-level
  variables. Each entry has its variables (name and declaring position) and the position of its
  initialiser.
- `:types`: every named type declared in the package, at package level or local (`:local
  true`), in source order. Each has `:t` and `:underlying`, `:type-params` (as `"T constraint"`),
  or `:alias true :rhs R`. It has `:method-set` and, except for interfaces, `:ptr-method-set`:
  `types.NewMethodSet` of T and *T, each method as `{:name N [:pkg P] :t SIG :recv R :path [i ...]
  :indirect B}`. Without type parameters it also has the layout under
  `types.SizesFor("gc", GOARCH)`: `:size`, `:align` and, for structs, `:fields` with
  `:offset`, `:size`, `:align` and `:embedded`.
- `:directives`: every comment that is a directive (`//go:`, build constraints `//go:build`
  and `// +build`, `//line` and `/*line`, `//export`, `//extern`), with its position.

### Example

`p.go`:

```go
package p

type P struct{ X, Y int32 }

//go:noinline
func (p *P) Move(dx int32) { p.X += dx * K } // scale

const K = 1 << 3
```

`godump -files p.go` writes, besides the header (config, files, imports), the method's body:

```clojure
(:assign-stmt
  {:lhs [^{:mode :var :t "int32" :sel {:kind :field :path [0] :indirect true :recv "*P"}}
         (:selector-expr
           {:x ^{:mode :var :t "*P" :use {:kind :var :name "p" :decl "6:7" :t "*P"}}
               (:ident {:name-pos "6:30" :name "p"})
            :sel ^{:use {:kind :field :name "X" :decl "3:16" :t "int32"}}
                 (:ident {:name-pos "6:32" :name "X"})})]
   :tok-pos "6:34" :tok "+="
   :rhs [^{:mode :value :t "int32"}
         (:binary-expr
           {:x ^{:mode :var :t "int32" :use {:kind :var :name "dx" :decl "6:18" :t "int32"}}
               (:ident {:name-pos "6:37" :name "dx"})
            :op-pos "6:40" :op "*"
            :y ^{:mode :const :t "int32" :val 8
                 :use {:kind :const :name "K" :decl "8:7" :t "untyped int" :pkg-level true}}
               (:ident {:name-pos "6:42" :name "K"})})]})
```

The use of the untyped constant `K` already has its context type (`:t "int32"`). The package
view ends:

```clojure
 :comments [["5:1" "//go:noinline"] ["6:46" "// scale"]]}]
 :directives [["p.go:5:1" "//go:noinline"]]
 :init-order []
 :types [{:name "P" :decl "p.go:3:6" :t "P" :underlying "struct{X int32; Y int32}"
   :method-set []
   :ptr-method-set [{:name "Move" :t "func(dx int32)" :recv "*P" :path [0] :indirect true}]
   :size 8 :align 4 :fields [{:name "X" :t "int32" :offset 0 :size 4 :align 4}
                             {:name "Y" :t "int32" :offset 4 :size 4 :align 4}]}]}
```

(The real output has no indentation inside a declaration.)

## Coverage

Measured on 2026-10-08 with `bin/g2c corpus`: TamaGo go1.27.1 (`/root/tamago-go`), 64 cores,
helper `-j 64`, Arbace stage 2, reading with 64 threads. Dump times exclude `go list`: 0.3 s
when the export data is cached, about 6 s the first time per GOARCH, when it builds std for the
configuration.

| corpus | packages / programs | files | type errors | failed | output | largest | dump | read on Arbace |
|---|---:|---:|---:|---:|---:|---|---:|---:|
| std, tamago/amd64 | 379 | 1,742 | 0 | 0 | 266.7 MB | runtime, 27.0 MB | 1.6 s | 5.7 s (2,308,308 nodes) |
| std, tamago/arm64 | 378 | 1,738 | 0 | 0 | 266.0 MB | runtime, 26.9 MB | 1.2 s | 5.5 s (2,304,970 nodes) |
| `$GOROOT/test`, tamago/amd64 | 1,705 of 1,745 | 1,706 | 0 | 0 | 53.9 MB | fixedbugs/bug257.go, 12.2 MB | 0.8 s | 2.7 s (396,469 nodes) |

- **std** is `go list std` for the configuration: `$GOROOT/src` outside `cmd`, with the
  vendored packages. arm64 lacks `internal/runtime/startlinetest`, which is amd64-only.
- **`$GOROOT/test`**: the directories of Go's test driver (`cmd/internal/testdir`), taking the
  files whose action compiles one program: `run` (1,072), `compile` (528), `asmcheck` (87),
  `build` (34), `runoutput` (22) and `buildrun` (2). `cmplxdivide.go` is dumped with the
  `cmplxdivide1.go` its action line names. Of the 1,745, 1,705 are dumped. 35 are excluded by
  their build constraints or file names (linux-only, cgo, `simd`, ...). 5 need another
  `GOEXPERIMENT` (`arenas`, `fieldtrack`, ...) and are left out as another configuration. Not
  taken: the 290 directory tests (`*dir` actions, several packages each) and the 678 error
  tests (`errorcheck*`, which must not compile).
- **Reading on Arbace:** every dump of the three corpora reads with `arbace.core/read`
  (`*read-eval*` false) as exactly one form. The largest, `runtime` (27.0 MB, 233,403 nodes,
  176,864 annotated), reads in 2.0 to 2.7 s in a fresh JVM, single-threaded (the frozen
  Clojure took 3.0 s for the experiment's 31 MB). Constants exercise every value encoding:
  ratios in 90 dumps, integers of 20 digits or more in 154, `(:complex ...)` in 52,
  `(:bytes ...)` in 31 and `(:float ...)` in 3.

## What is left

- **The converter** `arbace.g2c` (G1's other half, per `doc/go/SPEC.md`): the forms of survey
  §3 from these dumps.
- **Type strings** are `types.TypeString`, enough for now (survey §2.2: "later a type form").
  Local named types print by their bare name, so two local types `T` in different functions
  read alike in `:t`. Their identifiers carry `:decl`, which tells them apart. A structured type
  form, with identities, would remove the ambiguity.
- **Not emitted yet:** each named type's interface satisfactions within the program (survey
  §2.2, for an itab cache), `go/types`' scopes (`Info.Scopes`), and test files (`_test.go`,
  in-package and external) for the package test runs of survey §6.2.
- **Directory tests** of `$GOROOT/test` (`rundir`, `compiledir`, ...): several packages per
  test, importing each other by local path. They need a small driver over the `.dir`
  directories.
- **Positions of imported objects** are not written. An imported object is identified by
  `:pkg` and `:name` (and, for fields and methods, its receiver), and found in that package's
  dump.
