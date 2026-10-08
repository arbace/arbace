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
bin/g2c dump -tests fmt              # fmt.edn with fmt's _test.go files, and fmt_test.edn
bin/g2c dump -files prog.go a.go,b.go   # single-file programs; a.go,b.go is one program
bin/g2c read .tmp/godump/out         # read on Arbace (needs bin/build-arbace), print a summary
bin/g2c corpus [amd64|arm64|test|test-arm64|tests]  # the coverage runs below, into .tmp/godump/corpus/
bin/g2c corpus linux-amd64 linux-test linux-arm64 linux-test-arm64   # the same for GOOS=linux
bin/g2c dump -goos linux -goarch arm64 -o DIR std
```

- **Toolchain.** `G2C_GOROOT` names TamaGo's Go tree (default `/root/tamago-go`). The system Go
  (plain go1.27.1) lacks `GOOS=tamago`. TamaGo's go also builds `GOOS=linux`, and is used for it
  too: one tree and one build cache for every configuration; its std differs from upstream's
  only by `tamago` build constraints, its own files and the GOOS tables (ROUNDTRIP.md). `bin/g2c` builds the helper with `$G2C_GOROOT/bin/go`
  for the host (it runs at conversion time, like javac for j2c). It then runs the helper with
  `-go $G2C_GOROOT/bin/go`, which the helper uses for `go env` and `go list`.
- **Pinned Go.** The helper checks that it was built with go1.27 (its `go/types` is the
  language) and that the go command is go1.27.1. It type-checks at the package's language
  version: `go1.27` for std and single files, the `go` line of a module otherwise. It refuses a
  module or file whose language version is newer than go1.27. Files whose build constraint
  needs a newer release (`//go:build go1.28`) are excluded by the configuration, as the go
  command does. Moving the pin is a recorded decision (journal, 2026-10-07).
- **Configuration.** There is one per run, written into every dump: `-goos` (default
  `tamago`; g2c's configurations are `tamago` and `linux`: amendments L1 and L2, accepted
  2026-10-08, folded into SPEC §1 item 7 and §4.2), `-goarch` (`amd64` or `arm64`) and
  `-tags`. `CGO_ENABLED=0`, `GOFLAGS` is
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
- **Tests** (`-tests`, doc/go/SPEC.md §15 Q14). `go list` gets `-test` as well, and each
  package named is dumped as `go test` compiles it: with its in-package `_test.go` files (go
  list's test variant `p [p.test]`, whose files are `GoFiles` then `TestGoFiles`), under its own
  import path, and its external test package `p_test` (`p_test [p.test]`, files
  `XTestGoFiles`) as a dump of its own, key and path `p_test`. Both are type-checked through
  the variants' `ImportMap`, so `p_test` imports the `p` with test files, and packages that
  the tests make go list rebuild (`q [p.test]`) are imported as rebuilt. The generated test
  main (`p.test`) is not dumped. A package without in-package test files is dumped as without
  `-tests`. The first run builds every test variant's export data (about 15 s for std).
- **Output.** `-o DIR` (default `godump.out`; `bin/g2c` uses `.tmp/godump/out`). A package
  goes to `DIR/IMPORT/PATH.edn` (an external test package to `DIR/IMPORT/PATH_test.edn`), and
  a single file to `DIR/FILE.go.edn` (relative to `$GOROOT/test`, `$GOROOT` or the working
  directory). `.edn` follows `test/*.edn`: the format targets the Clojure reader, not strict
  EDN. Dumps are regenerated, not tracked (journal, 2026-10-07).
- **Summary.** On stdout, one line per package:
  `STATUS<TAB>KEY<TAB>FILES<TAB>BYTES<TAB>MS<TAB>TYPE-ERRORS[<TAB>FIRST-MESSAGE]`. STATUS is
  `ok`, `errors` (type errors; the dump is still written), `fail` (no dump: parse errors, `go
  list` errors, a newer language version) or `excluded` (a single file the configuration
  leaves out). On stderr, the totals. The exit status is 1 if anything has errors or failed.
- **Determinism.** The walk follows AST order. Method sets, init order and imports come out of
  `go/types` sorted or in a defined order. Type table ids are given in the order of first
  mention in the walk. No map is iterated where order matters. Two runs give byte-identical
  dumps, whatever `-j`: checked over std on amd64 and arm64, std with tests, and the test
  programs.

## Output format

Format version 2 (2026-10-08; version 1 had `types.TypeString` strings where it now has type
ids, a flat comment list, and floats as plain numbers). One form per package, read with `read`
and `*read-eval*` false (`arbace.g2c.read/read-dump`):

```clojure
;; godump: the typed AST of a Go package for arbace.g2c (doc/go/HELPER-NOTES.md)
{:godump 2                                  ; format version
 :package "fmt" :name "fmt"                 ; import path ("command-line-arguments" for files)
 :config {:goos "tamago" :goarch "amd64" :goamd64 "v1" :tags [] :tool-tags [...]
          :goexperiment "" :cgo false :compiler "gc" :toolchain "go1.27.1"
          :go-version "go1.27" :word-size 8 :max-align 8}
 :dir "$GOROOT/src/fmt"
 :for-test "fmt"                            ; with -tests: the package tested (both dumps)
 :go-files [...] :s-files [...] ...         ; go list's file lists (below), when not empty
 :embed-patterns [...] :embed-files [["path" "sha256"] ...]
 :imports ["errors" "internal/fmtsort" ...] ; resolved paths, sorted
 :errors []                                 ; type errors, "file.go:L:C: message"
 :files [{:file "print.go" [:go-version "go1.21"]   ; when the file's differs (//go:build)
          :ast (:file {...})
          :comments [{:list [["L:C" "// text"] ...] [:text "..."]} ...]}  ; comment groups
         ...]
 :directives [["print.go:L:C" "//go:linkname ..."] ...]
 :init-order [{:lhs [["name" "file.go:L:C"] ...] :rhs "file.go:L:C"} ...]
 :types [{:name ... } ...]
 [:warnings ["file.go:L:C: ..." ...]]       ; what the dump cannot represent, when any
 :type-table [{:kind ...} ...]}             ; every type mentioned, indexed by id
```

**File lists**, relative to `:dir`, as `go list` gives them for the configuration, each
written when not empty: `:go-files` (gc's order: sorted, the package's files), `:cgo-files`,
`:c-files`, `:cxx-files`, `:m-files`, `:h-files`, `:f-files`, `:s-files`, `:swig-files`,
`:swig-cxx-files`, `:syso-files`, `:ignored-go-files`, `:ignored-other-files`,
`:embed-patterns` and `:embed-files`. Each embedded file is `["path" "sha256"]`, the SHA-256
of its contents in lower-case hex (SPEC §9.6). The dump of a package with tests also has
`:test-go-files` (its `:files` are `:go-files` then these), `:test-embed-patterns` and
`:test-embed-files`; an external test package has only `:go-files`, `:embed-patterns` and
`:embed-files`, its own. Single files have `:go-files` only.

**The tree.** Every `go/ast` node is `(:kebab-type {:kebab-field value ...})`, with the fields
in `go/ast`'s order, for example `(:binary-expr {:x ... :op-pos "3:9" :op "+" :y ...})`. Values:

- nodes, and vectors of nodes;
- positions, `"LINE:COL"` in the file being written: raw positions (`PositionFor(p, false)`),
  never adjusted by `//line`; columns count bytes from 1;
- tokens as their Go spelling (`"+="`, `"import"`, `"INT"`);
- `ast.ChanDir` as `:send`, `:recv` or `:both`;
- strings (identifiers, literals' source text) and `true`;
- `:doc` and `:comment` (comment groups) as the position of the group's first comment, which
  is the first position of a group in `:comments`.

One field is not `go/ast`'s: **`:dot`**, the position of the `.` of a `selector-expr` and of a
`type-assert-expr` (gc positions both at their dot; SPEC §10.2), written after `:x`. The helper
finds it in the source after the operand, skipping white space and comments.

A field at its zero value is omitted (nil, false, empty string, no position, `ILLEGAL` token).
Not written: the parser's resolution (`Obj`, `Scope`, `Unresolved`), `File.Imports` (the
import specs again) and `File.Comments` (written beside the tree as `:comments`). Declarations
start on their own line. Every position gc uses is a field: end positions (`:rbrace`,
`:rparen`, `:rbrack`, `:closing`, the `:colon` of case clauses) and the file's `:file-start`
and `:file-end` included.

**Comments.** `:comments` holds every comment group of the file (`File.Comments`), in order,
as `{:list [["L:C" "text"] ...]}`, each comment with its position and its text as written
(`//...` or `/*...*/`). A group that is some node's `:doc` (package clause, declaration, spec,
field) also has `:text`, `go/ast`'s `CommentGroup.Text`: markers, directives, leading and
trailing blank lines removed (SPEC §6.5). `:comment` groups (line comments after a node) are
groups like the others.

**Directives.** `:directives` lists every comment that is a directive, with its position
(`"file.go:L:C"`) and text, in file and source order: `//go:` lines, build constraints
(`//go:build`, `// +build`), line directives (`//line`, `/*line`), `//export`, `//extern`, and
every other comment that `go/ast.ParseDirective` takes for one (`//tool:name args`, such as
`//gcassert:inline`). Where a directive belongs (a declaration, the file, a statement) follows
from its position; the converter decides (SPEC §9.1).

**Warnings.** `:warnings` (only when there are any) lists what the dump cannot represent for
the forms. Now: a `/*line*/` directive strictly inside an expression (SPEC §10.3), as
`"file.go:L:C: /*line*/ directive inside an expression"`. None occurs in std; 2 programs of
`$GOROOT/test` have some.

**Annotations** are reader metadata on the node's form, where `go/types` recorded something
(survey §2.2). A type is always an id into `:type-table` (below):

| key | on | value |
|---|---|---|
| `:mode` | expressions | `:const`, `:var` (addressable), `:mapindex`, `:value`, `:commaok`, `:type`, `:builtin`, `:void`, `:nil` |
| `:t` | expressions | the type's id |
| `:val` | constants | the exact value (below) |
| `:def`, `:use` | identifiers | the object (below) |
| `:inst` | identifiers of generic uses | `{:targs [T ...] :t T}`: all type arguments (inferred or not) and the instantiated type |
| `:sel` | selector expressions | `{:kind :field/:method/:method-expr :path [i ...] :indirect B :recv T [:via ["F" ...]]}`: the index path through embedded fields (the last index is the field or method), whether a pointer is followed, the receiver's type, and the names of the embedded fields traversed (the path but its last index), outermost first |
| `:field` | key-value expressions of struct literals | `{:path [i ...] [:via ["F" ...]]}`: the index path of the field the key names (go/types records only the field object), and the embedded fields traversed, as for `:sel` (SPEC §7.7, Go 1.27's promoted keys) |
| `:implicit` | import specs, type switch clauses, anonymous fields | the object declared implicitly |

**Constant values** (`:val`) are exact, and their representation tells go/constant's kind:

| kind | value |
|---|---|
| bool | `true`, `false` |
| string | a string, or `(:bytes [b ...])` when not valid UTF-8 |
| int (also untyped runes) | an integer, BigInt when large |
| float | `(:float R)`, R the exact value as an integer or a ratio; `(:float M E)`, meaning M·2^E with M odd, when the binary exponent is beyond ±2^14 (go/constant's `big.Float`, as for `1e100000`) |
| complex | `(:complex (:float RE) (:float IM))` |
| unknown | `nil` |

The kind is go/constant's, which can be narrower than the expression's type: in `1 + 2i` the
operand `1` has `:t` `untyped complex` but the value `1`. A consumer goes by `:t` for the type
and by `:val` for the value; SPEC §8.2's representation (doubles, BigDecimals) is the
converter's to choose.

An object is `{:kind K :name N ...}`, with K one of `:var`, `:field`, `:func`, `:const`, `:type`,
`:pkg`, `:builtin`, `:label` or `:nil`. It may carry:

- `:pkg`, the path of its package, when it is not the package dumped;
- `:decl`, its position (`"L:C"` in the same file, `"file.go:L:C"` otherwise), for objects of
  the package dumped. The position is the object's identity;
- `:t`, its type's id;
- `:pkg-level true` for package-scope objects and `:universe true` for predeclared ones;
- `:recv`, a method's receiver type (an id);
- `:embedded true` on embedded fields;
- `:alias true` and `:type-param true` on type names;
- `:path`, a package name's import path;
- `:val`, a constant's value, on definitions only. Uses carry it as the expression's `:val`.

**The package view:**

- `:init-order`: `go/types`' `InitOrder`, the spec's dependency order of package-level
  variables. Each entry has its variables (name and declaring position) and the position of its
  initialiser.
- `:types`: every named type declared in the package, at package level or local (`:local
  true`), in source order. Each has `:t` and `:underlying` (ids), `:type-params` (as in the
  table: `[{:t TP :constraint C} ...]`), or `:alias true :rhs R`. It has `:method-set` and,
  except for interfaces, `:ptr-method-set`: `types.NewMethodSet` of T and *T, each method as
  `{:name N [:pkg P] :t SIG :recv R :path [i ...] :indirect B}`. Without type parameters it
  also has the layout under `types.SizesFor("gc", GOARCH)`: `:size`, `:align` and, for structs,
  `:fields` with `:offset`, `:size`, `:align` and `:embedded`.
- `:directives`, `:warnings`: above.

### Types

`:type-table` is a vector: the type with id N is its Nth entry. It holds every type the dump
mentions, each once, as structured data, so that types can be written as type forms (SPEC
§5) and told apart. Component types are ids of earlier entries: the table is in dependency
order, and `arbace.g2c.read/check-type-table` checks it. A named type, an alias and a type
parameter are leaves, named and not expanded (a named type's underlying type is in the `:types`
of the dump of its package), which also ends recursive types.

| entry | type |
|---|---|
| `{:kind :basic :name "int"}` | a basic type, by `go/types`' name: `"byte"` and `"rune"` as written, `"untyped int"` ... `"untyped nil"`, `"invalid type"` |
| `{:kind :basic :name "Pointer" :pkg "unsafe"}` | `unsafe.Pointer` |
| `{:kind :pointer :elem T}`, `{:kind :slice :elem T}`, `{:kind :array :len N :elem T}` | `*T`, `[]T`, `[N]T` |
| `{:kind :map :key K :elem T}`, `{:kind :chan :dir D :elem T}` | `map[K]T`, `chan T` (D `:both`, `:send`, `:recv`) |
| `{:kind :func [:type-params [TP ...]] :params [V ...] [:results [V ...]] [:variadic true]}` | a signature; the last parameter of a variadic one has the slice type |
| `{:kind :struct :fields [{:name F [:pkg P] :t T [:embedded true] [:tag "..."]} ...]}` | a struct |
| `{:kind :interface [:methods [{:name M [:pkg P] :t SIG} ...]] [:embeddeds [T ...]] [:implicit true]}` | an interface as declared: explicit methods and embedded types (unions too); `:implicit` for the interface of a constraint written `[T ~int]` |
| `{:kind :union :terms [{:t T [:tilde true]} ...]}` | a union of terms (constraints) |
| `{:kind :tuple :vars [V ...]}` | the type of a call with several results (and `[]` of a void call) |
| `{:kind :named :name N [:pkg P] [:decl POS]}` | a named type; without `:pkg` a universe one (`error`, `comparable`) |
| `{:kind :named :name N :pkg P :origin G :args [T ...]}` | an instance of the generic named type G |
| `{:kind :alias :name N [:pkg P] [:decl POS] [:origin G :args [T ...]] :actual T}` | an alias (`any` too), kept apart from what it denotes, `:actual` (`types.Unalias`) |
| `{:kind :type-param :name N :index I [:pkg P] [:decl POS]}` | a type parameter |

with V = `{[:name N] :t T}` (a parameter, result or tuple element) and TP = `{:t TYPE-PARAM
:constraint T}`. Every entry ends with `:str`, `types.TypeString` (the package dumped
unqualified, others by path), for people: it is not part of the identity.

- **Qualification.** Named types, aliases and type parameters carry their package's path
  (`:pkg`), unexported fields and methods theirs, since they are distinct per package.
- **Identity.** A named type or alias that is not an instance is one entry per type name
  object, a type parameter one per `go/types` object. A local named type (declared in a
  function) also has `:decl`, its declaring position `"file.go:L:C"` (for an imported one,
  export data's), so two local types `T` are two entries that differ. A type parameter always
  has its `:decl` when known. Other entries are interned by structure, so equal entries are
  one id. Parameter names are part of a signature's entry (as `types.TypeString` prints them),
  though not of its type identity.
- Instances (`List[int]`) and generic aliases' instances name their origin and type
  arguments; the origin entry is the generic type itself (`:str "List[T any]"`).

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
  {:lhs [^{:mode :var :t 1 :sel {:kind :field :path [0] :indirect true :recv 3}}
         (:selector-expr
           {:x ^{:mode :var :t 3 :use {:kind :var :name "p" :decl "6:7" :t 3}}
               (:ident {:name-pos "6:30" :name "p"})
            :dot "6:31"
            :sel ^{:use {:kind :field :name "X" :decl "3:16" :t 1}}
                 (:ident {:name-pos "6:32" :name "X"})})]
   :tok-pos "6:34" :tok "+="
   :rhs [^{:mode :value :t 1}
         (:binary-expr
           {:x ^{:mode :var :t 1 :use {:kind :var :name "dx" :decl "6:18" :t 1}}
               (:ident {:name-pos "6:37" :name "dx"})
            :op-pos "6:40" :op "*"
            :y ^{:mode :const :t 1 :val 8
                 :use {:kind :const :name "K" :decl "8:7" :t 5 :pkg-level true}}
               (:ident {:name-pos "6:42" :name "K"})})]})
```

The use of the untyped constant `K` already has its context type (`:t 1`, `int32`; `K` itself
is `untyped int`, 5). The file and the package view end:

```clojure
  :comments [{:list [["5:1" "//go:noinline"]] :text ""}   ; Move's :doc: only a directive
             {:list [["6:46" "// scale"]]}]}]
 :directives [["p.go:5:1" "//go:noinline"]]
 :init-order []
 :types [{:name "P" :decl "p.go:3:6" :t 0 :underlying 2
   :method-set []
   :ptr-method-set [{:name "Move" :t 4 :recv 3 :path [0] :indirect true}]
   :size 8 :align 4 :fields [{:name "X" :t 1 :offset 0 :size 4 :align 4}
                             {:name "Y" :t 1 :offset 4 :size 4 :align 4}]}]
 :type-table [{:kind :named :name "P" :pkg "command-line-arguments" :str "P"}
  {:kind :basic :name "int32" :str "int32"}
  {:kind :struct :fields [{:name "X" :t 1} {:name "Y" :t 1}] :str "struct{X int32; Y int32}"}
  {:kind :pointer :elem 0 :str "*P"}
  {:kind :func :params [{:name "dx" :t 1}] :str "func(dx int32)"}
  {:kind :basic :name "untyped int" :str "untyped int"}]}
```

(The real output has no indentation inside a declaration.) More, from a program with an
embedded struct `N{P; Name string}`, a generic function and two local types `T`:

```clojure
;; n := N{X: 1, Name: "a"}: X promoted from the embedded P (Go 1.27)
^{:field {:path [0 0] :via ["P"]}} (:key-value-expr {:key ... (:ident {... :name "X"}) ...})
^{:field {:path [1]}} (:key-value-expr {:key ... (:ident {... :name "Name"}) ...})
;; n.Move(2): a promoted method
^{:mode :value :t 4 :sel {:kind :method :path [0 0] :indirect false :recv 8 :via ["P"]}}
(:selector-expr {...})
;; _ = f(3) with func f[T any](x T) T
^{:mode :value :t 20 :use {:kind :func :name "f" :decl "25:6" :t 14 :pkg-level true}
  :inst {:targs [17] :t 20}} (:ident {:name-pos "34:6" :name "f"})
;; constants
:val (:float 2)                      ; const F = 2.0
:val (:complex (:float 1) (:float 2)) ; const C = 1 + 2i
:val (:float 5874...6824125 331682)  ; const Huge = 1e100000 (go/types' 512-bit value)
;; in the type table
{:kind :type-param :name "T" :index 0 :pkg "command-line-arguments" :decl "p.go:25:8" :str "T"}
{:kind :interface :str "interface{}"}
{:kind :alias :name "any" :actual 12 :str "any"}
{:kind :func :type-params [{:t 11 :constraint 13}] :params [{:name "x" :t 11}] :results [{:t 11}] :str "func[T any](x T) T"}
{:kind :named :name "T" :pkg "command-line-arguments" :decl "p.go:28:7" :str "T"}
{:kind :named :name "T" :pkg "command-line-arguments" :decl "p.go:40:7" :str "T"}
```

## Coverage

Measured on 2026-10-08 with `bin/g2c corpus`: TamaGo go1.27.1 (`/root/tamago-go`), 64 cores,
helper `-j 64`, Arbace stage 2, reading with 64 threads (which includes checking each type
table). Dump times exclude `go list`: 0.3 s when the export data is cached, about 6 s the
first time per GOARCH, when it builds std for the configuration (about 15 s for std with
tests).

| corpus | packages / programs | files | type errors | failed | output | largest | dump | read on Arbace |
|---|---:|---:|---:|---:|---:|---|---:|---:|
| std, tamago/amd64 | 379 | 1,742 | 0 | 0 | 252.7 MB | runtime, 24.8 MB | 1.8 s | 5.9 s (2,308,308 nodes, 55,001 types) |
| std, tamago/arm64 | 378 | 1,738 | 0 | 0 | 252.3 MB | runtime, 24.8 MB | 1.5 s | 5.4 s (2,304,970 nodes, 54,967 types) |
| `$GOROOT/test`, tamago/amd64 | 1,705 of 1,745 | 1,706 | 0 | 0 | 52.2 MB | fixedbugs/bug257.go, 12.2 MB | 0.7 s | 2.4 s (396,469 nodes, 31,024 types) |
| std, linux/amd64 | 382 | 1,834 | 0 | 0 | 257.7 MB | runtime, 26.2 MB | 1.3 s | 7.3 s (2,357,789 nodes, 56,256 types) |
| std, linux/arm64 | 380 | 1,829 | 0 | 0 | 257.5 MB | runtime, 26.2 MB | 1.3 s | 6.4 s (2,355,930 nodes, 56,229 types) |
| `$GOROOT/test`, linux/amd64 | 1,713 | 1,714 | 0 | 0 | 52.4 MB | fixedbugs/bug257.go, 12.2 MB | 1.5 s | 2.5 s (398,760 nodes, 31,239 types) |
| `$GOROOT/test`, linux/arm64 | 1,707 | 1,708 | 0 | 0 | 52.3 MB | fixedbugs/bug257.go, 12.2 MB | 1.1 s | 2.5 s (397,654 nodes, 31,151 types) |
| std with tests (`-tests`), tamago/amd64 | 554 (379 + 175 `_test` packages) | 2,935 | 0 | 0 | 451.0 MB | runtime, 25.8 MB | 1.1 s | 7.4 s (4,299,718 nodes, 99,678 types) |

Format version 1 (same day, same machine) wrote 266.7 MB, 266.0 MB and 53.9 MB for the first
three, in 1.4 s, 1.3 s and 0.8 s, read in 5.7 s, 5.5 s and 2.5 s: the type ids make the dumps
5% smaller than the type strings did; dumping takes 0.1 to 0.4 s longer, reading the same.

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

  Since 2026-10-08 (closing the round trip) the list is the oracle's, `bin/g2c-check
  --list-tests` (`gocmp tests`, [ROUNDTRIP.md](ROUNDTRIP.md)): one rule, in one place, for
  both corpora. It is the rule above with the test driver's reading of the action line
  (`//run` without a space counts: 2 more programs) and go/build's matching of build
  constraints and file names: 1,707 programs for tamago/amd64 (1,708 files), 1,702 for
  tamago/arm64 (`bin/g2c corpus test-arm64`), all dumped, 0 excluded by the helper, 0 type
  errors.
- **linux** (2026-10-08, B1a's configuration, `bin/g2c corpus linux-amd64 linux-test ...`):
  std has 382 packages on amd64 (380 on arm64), three more than tamago's
  (`internal/cgrouptest`, `internal/runtime/syscall/linux`, `runtime/race/internal/amd64v1` on
  amd64), and 1,834 files (173 that tamago's configuration leaves out: system calls and their
  generated tables, `os`, `net`, `internal/poll`, `internal/syscall/unix`, the runtime's linux
  files; 81 the other way). `$GOROOT/test` has 1,713 programs on amd64 (`chanlinear.go`,
  `maplinear.go`, `recover4.go`, `fixedbugs/issue15002.go`, `issue79874.go`, `issue8606b.go`
  are linux or unix only) and 1,707 on arm64. Everything type-checks and reads; the helper
  needed no change (cgo is off, `CGO_ENABLED=0`, so `CgoFiles` are empty: the packages' cgo
  variants, `net`'s and `os/user`'s, are not this configuration; amendment L3, accepted
  2026-10-08, folded into SPEC §9.4). The linux std dumps in 1.3 s,
  the first time 5 to 10 s (go list builds its export data).
- **std with tests** (`bin/g2c corpus tests`, not run by default): every std package with its
  in-package test files, and the 175 external test packages. All type-check for tamago/amd64.
- **Reading on Arbace:** every dump of the corpora reads with `arbace.core/read`
  (`*read-eval*` false) as exactly one form, and its type table is in dependency order.
  Constants exercise every value encoding, `(:float M E)` included (3 dumps).

## Spec amendments (accepted 2026-10-08)

Writing format 2 raised seven amendments to [SPEC.md](SPEC.md) (journal, 2026-10-08, "The
helper's format 2"). The user accepted all seven on 2026-10-08, with the printer's A1-A10 and
the converter's C1-C11; where they disagreed, the converter's wins over the printer's over the
helper's (SPEC §15, "Amendments (2026-10-08)").

- **H1. `:dot` also on type assertions.** gc positions `x.(T)` at its dot as it does a
  selector, so the helper writes `:dot` on `type-assert-expr` too. Accepted 2026-10-08, folded
  into SPEC §10.2 (and §11.3 item 1, §13.1).
- **H2. Do `//go:` lines attach across blank lines?** Left open for verification. Accepted
  2026-10-08, settled by the converter's C1 (gc attaches them across blank lines), folded into
  SPEC §9.1. C1, the later and more specific, is the rule.
- **H3. The type table as §11.3 item 4's shape.** Types are ids into `:type-table`; local
  types and type parameters are told apart by `:decl`. Accepted 2026-10-08, folded into SPEC
  §11.3 item 4.
- **H4. Values narrower than their type.** go/constant's kind can be narrower than the
  expression's type (`1` in `1 + 2i`); §8.2's representation is built from `:t`. Accepted
  2026-10-08, folded into SPEC §8.2 (and §11.1, §11.3 item 5, §13.3), with the converter's
  refinement (named types of other packages: the value's kind decides).
- **H5. `:go/via` names from the helper's `:via`** on `:sel` and `:field`. Accepted
  2026-10-08, folded into SPEC §7.6 (and §11.1, §11.3 item 6, §13.3).
- **H6. Separate file lists.** The helper writes each of go list's lists apart; the converter
  merges the non-Go ones into §4.2's `:other-files`. Accepted 2026-10-08, folded into SPEC §4.2
  (and §11.3 item 7).
- **H7. The external test package's path** is `p_test`, its namespace `go.<path>_test`.
  Accepted 2026-10-08, folded into SPEC §4.1.

## What is left

- **The converter** `arbace.g2c` (G1's other half, per `doc/go/SPEC.md`): the forms of survey
  §3 from these dumps.
- **Not emitted:** each named type's interface satisfactions within the program (survey §2.2,
  for an itab cache), `go/types`' scopes (`Info.Scopes`, not needed: SPEC §11.2), a
  signature's receiver and receiver type parameters in the type table (the method objects carry
  `:recv`), and the underlying types of imported named types (in their packages' dumps).
- **Directory tests** of `$GOROOT/test` (`rundir`, `compiledir`, ...): several packages per
  test, importing each other by local path. They need a small driver over the `.dir`
  directories.
- **Positions of imported objects** are not written. An imported object is identified by
  `:pkg` and `:name` (and, for fields and methods, its receiver), and found in that package's
  dump. Imported local types and type parameters have their export data position in the type
  table.
