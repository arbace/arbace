# Go forms: Go programs written in Clojure

Status: accepted (2026-10-08), milestone G0 of g2c ([G2C-SURVEY.md](../G2C-SURVEY.md) §7),
with the 28 amendments found while implementing the helper, the printer and the converter folded
in (2026-10-08, §15). Implemented by `tools/godump` (the helper), `arbace.g2c.convert` (the
converter) and `arbace.g2c.print` (the printer). The decisions of 2026-10-07 (survey §9) bind
it; the user accepted the recommendation of each of the 20 questions of §15 on 2026-10-08.

This spec defines the **Go forms**: how every construct of a Go package, as `go/parser` and
`go/types` see it, is written as Clojure data in ordinary `.clj` sources. It says what the forms
mean, what the converter writes out of the type checker's decisions, what a consumer derives by
itself, and what the printer needs to give the Go toolchain a program that `gc` compiles to the
same export data and object code. It is the counterpart of the class forms
([classes/SPEC.md](../classes/SPEC.md)) for Go, and follows it in structure and principles.

Words used throughout:

- **the helper**: the Go program under `tools/` (seeded from the survey's `godump`). It loads
  one package for one build configuration with `go/parser` and `go/types` and emits the typed
  syntax tree generically: every `go/ast` node with its position fields, plus the `types.Info`
  maps (survey §2.2). It decides nothing about forms.
- **the converter**: `arbace.g2c`, in Clojure. It reads the helper's output and writes Go forms.
  Every decision about forms is made here.
- **the printer**: Go forms → Go source (G2). With the converter it makes the round trip
  *Go → forms → Go* (§3).
- **a consumer**: anything that reads Go forms for their meaning: the printer, and later
  Arbace's own Go-forms compiler (survey §5.2, steps b2-b4).
- **Go forms**: the forms of this spec. **Go context**: the inside of a Go declaration form
  (§7.2), where the forms mean Go.
- **gc**: Go's compiler, `cmd/compile`, as driven by the `go` command of the pinned toolchain.

Contents: 1 Principles · 2 A first example · 3 Equivalence: the round trip · 4 Packages, files,
imports, names · 5 Types · 6 Declarations · 7 Code · 8 Constants and literals · 9 Directives,
`unsafe`, cgo, assembly, embedding · 10 Positions and comments · 11 What the converter writes,
what is derived · 12 The printer · 13 Coverage · 14 Worked examples · 15 Open questions ·
16 Sources.

## 1. Principles

1. **Clojure keeps its meaning outside Go context; inside, the forms mean Go.** Go forms live
   in namespaces of their own and are read by consumers that know them. Inside a Go declaration
   form, `+`, `<<`, `==` and the other operators are Go's operators at the operands' type, and
   `if`, `let`, `set!`, `range`, `for` are Go's statements (the user's decision of 2026-10-07,
   a deliberate exception to class forms principle 1). No Clojure code runs inside Go context:
   there are no vars, no seqs, no Clojure functions there.
2. **Declarations mirror Go.** One form per Go declaration, in Go's source order, in one file
   per Go file. Go's identifiers are written verbatim: capitalisation is export, so nothing is
   munged. Types are tags and type forms, as in the class forms.
3. **go/types' decisions are written out.** Whatever a consumer could only learn by running a
   Go type checker is in the forms: constant values, inferred type arguments, embedded-field
   paths of selections, the redeclared names of `:=`, initialisation order, the types of
   untyped constants where Go's rules are not local. A consumer needs the declared signatures
   of the package and its imports, never type inference (§11).
4. **What follows from declarations is not written.** Method sets, struct layout, zero values,
   implicit `&` and `*` on method receivers, implicit conversions to interface types, the
   per-clause variables of a type switch, `range` kinds: a consumer derives them from the
   declarations, as gc does.
5. **Plain Clojure syntax.** Everything reads with `clojure.core/read` (Arbace's reader,
   Clojure 1.12 syntax) without data readers or reader changes: metadata, qualified symbols,
   keywords, BigInts (`N`), BigDecimals (`M`) and ratios. Not EDN (CLAUDE.md, "Clojure reader
   over EDN"). Where Go syntax cannot be read by the reader (`^`, `~`, `&^`, `'x'`, `` `raw` ``,
   `.5`, hex floats), the forms use other spellings (§7.5, §8).
6. **Lossless for gc.** The forms keep everything gc's output depends on: file names and split,
   declaration order, every directive, initialisation semantics, and positions (§10). The
   printer can therefore give gc a program it compiles to the same export data and object code
   (§3). Differences that gc cannot see (parentheses, field grouping, literal spelling, ordinary
   comments) are normalised away and listed (§3.2); where gc does see them (parentheses around
   `&&` and `||` operands, field groups of generic declarations, local `const` groups), they
   are kept (amendment, accepted 2026-10-08).
7. **One configuration per conversion.** Build constraints are resolved by the helper. The forms
   of a package are for one GOOS, GOARCH and tag set, recorded in the forms (§4.2). The pinned
   configurations are `GOOS=tamago` with `GOARCH=amd64` and `arm64`, toolchain go1.27.1
   (`/root/tamago-go`).

## 2. A first example

`sort/search.go`, go1.27.1:

```go
func Search(n int, f func(int) bool) int {
	// Define f(-1) == false and f(n) == true.
	// Invariant: f(i-1) == false, f(j) == true.
	i, j := 0, n
	for i < j {
		h := int(uint(i+j) >> 1) // avoid overflow when computing h
		// i ≤ h < j
		if !f(h) {
			i = h + 1 // preserves f(i-1) == false
		} else {
			j = h // preserves f(j) == true
		}
	}
	// i == j, f(i-1) == false, and f(j) (= f(i)) == true  =>  answer is i.
	return i
}
```

As Go forms (the doc comment shortened):

```clojure
(go/func Search
  "Search uses binary search to find and return the smallest index i in [0, n) ..."
  ^int [^int n ^{:tag (func [int] [bool])} f]
  ;; Define f(-1) == false and f(n) == true.
  ;; Invariant: f(i-1) == false, f(j) == true.
  (let [(values i j) (values 0 n)]
    (while (< i j)
      (let [h (conv int (>> (conv uint (+ i j)) 1))]   ; avoid overflow when computing h
        ;; i ≤ h < j
        (if (not (f h))
          (set! i (+ h 1))                              ; preserves f(i-1) == false
          (set! j h))))                                 ; preserves f(j) == true
    ;; i == j, f(i-1) == false, and f(j) (= f(i)) == true  =>  answer is i.
    i))
```

- The declaration form is `go/func` (alias `go` for `arbace.go`); the name is verbatim.
- Types are tags. A single unnamed result is the tag of the parameter vector, as in `defn`.
- `i, j := 0, n` is a `let` whose binding target is `(values i j)`. A `let` scopes over the rest
  of the Go block, so the rest of the function is its body.
- `+`, `>>` and `<` are Go's operators at `int` and `uint`. `int(...)` is `(conv int ...)`.
- The function's last statement `return i` is the body's last value, `i`.
- The comments are `;` comments: they live in the text, not in the data (§10.4).

## 3. Equivalence: the round trip

The round trip is *Go → helper → converter → forms → printer → Go*. It is the check of
converter and printer (survey §6.1), and later of anything that writes Go forms.

### 3.1 Levels

For a package converted under a configuration, compile the original files and the printed files
with the same toolchain, configuration and flags (§12.4). They are **equivalent** when:

1. **Trees.** Each printed file parses to the same `go/ast` tree as its original, after the
   normalisations of §3.2, with the same comments where §3.2 keeps them. Positions are compared
   as the positions mode says (§10).
2. **Export data.** The two packages' export data describe the same package: the same objects
   with the same types, constant values, method sets and inlinable bodies. In positions mode
   `:full` the export data is byte-identical; in mode `:lines`, positions inside it are compared
   by line only.
3. **Object code.** Each function compiles to the same machine code, relocations and data, and
   the same symbols in the same order. In mode `:full` the line tables (pclntab) and DWARF are
   identical too; in mode `:lines`, line tables are compared for the lines of statements.
4. **Behaviour.** Programs built from printed sources behave as the originals: Go's test
   programs (`$GOROOT/test`, `// run`) and package tests, run with `GOOS=tamago` in the box
   (survey §6.3) or on the host configuration where the round trip is also run.

Level 3 implies the others for what gc emits; levels 1 and 2 localise a difference. The gate
of G2 is levels 1-3 over `$GOROOT/src` and `$GOROOT/test` (the tamago-go tree), and level 4
for the run programs.

### 3.2 Normalisations

The forms do not keep the following, because gc's output does not depend on them. The tree
comparison of level 1 applies them to both trees before comparing:

| N | difference | how it is normalised |
|---|---|---|
| N1 | parentheses | `ParenExpr` removed; the printer adds the parentheses Go's grammar needs (§12.2); except around operands of `&&` and `\|\|`, kept as `^:go/paren` (§7.5) |
| N2 | empty statements | `EmptyStmt` removed from statement lists, except as the target of a label |
| N3 | grouped names in fields, parameters, results, type parameters (`a, b int`) | one field per name; the printer may regroup; except in generic declarations, where the groups are kept as `^:go/grouped` (§5.3) |
| N4 | literal spelling | `BasicLit` compared by kind and exact value: radix, `_` separators, exponent form, raw against interpreted strings, escapes |
| N5 | import grouping | imports compared as one ordered list of specs per file |
| N6 | local declaration groups | `var (...)` and `type (...)` inside a function become one declaration per spec; a local `const (...)` group keeps its grouping as `^:go/grouped`, with its implicit repetition written out and marked `^:go/implicit` (§7.3) |
| N7 | `for ; c ; {}` | the same tree as `for c {}` (Go's parser already makes it one) |
| N8 | comments | only directives (§9.1), line directives (§10.3) and, when kept, doc comments are compared |
| N9 | positions | per mode (§10): all of them in `:full`; in `:lines`, the lines of statements and of the forms that start a Go line |
| N10 | `Incomplete`, `Implicit` flags, `Scope`, `Obj`, `Unresolved` | ignored |

Everything else is kept and must be equal: declaration and file order, `var x = e` against
`x := e`, `x op= e` against `x = x op e`, `i++` against `i += 1`, `for {}` against `for true {}`,
elided composite literal types, `any` against `interface{}`, `byte` against `uint8`, aliases,
single-spec groups at package level, the position of `default` in a `switch`, `else { if ... }`
against `else if`. Some of these are invisible to gc too, but keeping them costs nothing and
makes inlining costs, which count gc's IR nodes, follow without argument.

N1, N3 and N6 have exceptions because gc's output depends on what they would drop
(amendment, accepted 2026-10-08): gc removes dead branches of `&&` and `||` by syntax and does
not look through parentheses (`bytes.IndexRune` compiles differently without them); field
groups in generic declarations change gc's dictionaries; and iota and implicit repetition in a
local `const` group depend on the group. The oracle's tree level keeps the last two
([ROUNDTRIP.md](ROUNDTRIP.md), normalizations 4 and 5); the parentheses show at the export and
code levels.

### 3.3 What equivalence does not cover

- Files excluded by the configuration (they are not converted), cgo files (excluded by
  `CGO_ENABLED=0`, §9.4) and assembly (§9.5) are outside the forms; the printer copies the
  non-Go files of the package unchanged (§12.4).
- Programs that inspect their own column numbers in mode `:lines`; go-lisp excluded 15 such
  run programs (survey §1.2). Mode `:full` covers them.

## 4. Packages, files, imports, names

### 4.1 Namespaces and layout

A Go package is a Clojure namespace. Its name is `go.` followed by the import path, with `/`
replaced by `.` and, inside a path element, `.` and `-` replaced by `_`:

| import path | namespace | files |
|---|---|---|
| `unicode/utf8` | `go.unicode.utf8` | `go/unicode/utf8.clj`, `go/unicode/utf8/utf8.clj` |
| `math/rand/v2` | `go.math.rand.v2` | `go/math/rand/v2.clj`, `go/math/rand/v2/*.clj` |
| `vendor/golang.org/x/net/dns/dnsmessage` | `go.vendor.golang_org.x.net.dns.dnsmessage` | |
| `main` packages (`cmd/gofmt`) | `go.cmd.gofmt` | |
| the external test package of `fmt` (`fmt_test`, §15 Q14) | `go.fmt_test` | `go/fmt_test.clj`, `go/fmt_test/*.clj` |
| a single-file program `$GOROOT/test/fixedbugs/bug257.go` | `go.test.fixedbugs.bug257` | |

The path is kept exactly in `go/package` (§4.2). An external test package `p_test` has the
path `p_test` and the namespace `go.<path>_test`, `<path>` mapped as above (amendment, accepted
2026-10-08). A single-file program (`$GOROOT/test`) keeps the import path the go command gives
it, `command-line-arguments`, in `:path`; its namespace is `go.test.` followed by the file's
path relative to `$GOROOT/test` without `.go`, mapped as above (amendment, accepted
2026-10-08). Two paths of one conversion that map to one namespace are a converter error (none
exists in the standard library). Layout follows class forms §9.1: the package file holds the
`ns` form, the `go/package` form and one `load` per Go file in the package's file order; each
Go file `x.go` becomes `<pkg-dir>/x.clj`, or `<pkg-dir>/x.go.clj` when the package's source
directory has a subdirectory `x`, whose package's own file would be `<pkg-dir>/x.clj`
(amendment, accepted 2026-10-08; 9 files in std, such as `runtime/debug.go`):

```clojure
;; go/unicode/utf8.clj
(ns go.unicode.utf8
  (:require [arbace.go :as go]))

(go/package utf8
  :path "unicode/utf8"
  :config {:goos "tamago" :goarch "amd64"}   ; and the rest, §4.2
  :files ["utf8.go"])

(load "utf8/utf8")
```

```clojure
;; go/unicode/utf8/utf8.clj
(in-ns 'go.unicode.utf8)

(go/file "utf8.go"
  :doc "Package utf8 implements functions and constants to support text encoded in\n..."
  :imports [])

(go/const ...)
...
```

The forms of each configuration are a separate tree (for example under
`target/go/tamago_amd64/`). Converted output is regenerated, not tracked (decision 9).

### 4.2 `go/package`

```
(go/package name option*)
```

| option | value |
|---|---|
| `:path` | the import path, a string |
| `:config` | the build configuration (below) |
| `:files` | the Go files of the package in gc's order (sorted by name, as `go list` gives `GoFiles`), a vector of strings |
| `:other-files` | the non-Go files the build uses, copied by the printer: `.s`, `.h`, `.syso` (§9.5); one sorted vector, the union of the helper's separate lists |
| `:embed-files` | the files matched by `//go:embed` patterns, with their SHA-256 (§9.6), the test files' included |
| `:test-files` | when tests are converted, the package's `_test.go` files, loaded after `:files` (§15 Q14) |
| `:init-order` | go/types' `InitOrder` (§6.3) |
| `:positions` | `:lines` (the default) or `:full` (§10) |

`:config` is a map:

| key | example | meaning |
|---|---|---|
| `:goos`, `:goarch` | `"tamago"`, `"amd64"` | the target |
| `:goamd64` / `:goarm64` | `"v1"`, `"v8.0"` | the architecture level, which selects code and build tags |
| `:toolchain` | `"go1.27.1"` | `go version` of the toolchain (TamaGo's go1.27.1) |
| `:lang` | `"go1.27"` | the package's language version (`go.mod` or the standard library's) |
| `:tags` | `[]` | the `-tags` given; the implicit tags follow from the other keys |
| `:goexperiment` | `""` | `GOEXPERIMENT` |
| `:cgo` | `false` | `CGO_ENABLED` |
| `:compiler` | `"gc"` | always gc |

The helper gives go list's file lists apart (`:s-files`, `:h-files`, `:syso-files`, and the
cgo-only lists, empty for tamago); the converter merges them, sorted, into `:other-files`, since
the printer only copies them (amendment, accepted 2026-10-08). `:other-files`, `:embed-files`
and `:test-files` are omitted when empty. The package's name is the package clause's; a
package with test files only (no Go files for the configuration) is named by the last element
of its path, with `.` and `-` replaced by `_` (amendment, accepted 2026-10-08).

### 4.3 `go/file`

Each Go file starts with one `go/file` form, after `in-ns`. It holds the file's package clause
and everything gc reads from the file header:

```
(go/file "name.go" option*)
```

| option | value |
|---|---|
| `:package` | the package clause's name, when it differs from `go/package`'s (never for a valid package; kept for test packages, §15 Q14) |
| `:build` | the build constraint lines, verbatim, in order: `["//go:build !purego"]` |
| `:directives` | other directives before the package clause, verbatim (`//go:debug ...`) |
| `:lang` | the file's language version when it differs from the package's (`types.Info.FileVersions`, from a `//go:build go1.N` line) |
| `:doc` | the package doc comment on this file's package clause (§10.4) |
| `:imports` | the import specs, in order, and the directives among them (below) |

An import spec is a vector `[name "path"]`:

| Go | spec |
|---|---|
| `import "unicode/utf8"` | `[utf8 "unicode/utf8"]`: `name` is the imported package's name (go/types' implicit `PkgName`) |
| `import r "math/rand"` | `^:alias [r "math/rand"]`: an explicit name |
| `import _ "embed"` | `[_ "embed"]` |
| `import . "math"` | `[. "math"]` |

A directive after the package clause and before the end of the last import declaration is a
`(go/directive "...")` element of `:imports`, in source order with the specs (amendment,
accepted 2026-10-08). `crypto/internal/fips140/mlkem`'s `mlkem768.go` has a `//go:generate`
line between its package clause and its imports, so its `go/file` has
`:imports [(go/directive "//go:generate go run generate1024.go ...") [bytes "bytes"] ...]`
(the text shortened). The printer writes the directive where it stands.

Imports are file-scoped, as in Go: `utf8/RuneLen` resolves through the imports of the file it is
in, so two files may import different packages under one name. The forms do not use Clojure's
`:require` aliases for Go packages.

### 4.4 Names

Go identifiers are symbols, verbatim: `RuneLen`, `rune1Max`, `_`, `ñ`. Other names:

| Go | form |
|---|---|
| `pkg.Name` (qualified identifier) | `pkg/Name`, `pkg` being the file's import name |
| a name brought in by a dot import | the bare symbol |
| a label `L` | the keyword `:L` (labels are never confused with locals) |
| the predeclared `true`, `false`, `nil` | the literals `true`, `false`, `nil` |
| an identifier spelled `true`, `false` or `nil` that names something else (a shadowing declaration) | `(go/id "true")`, in any symbol position; as a declared name (parameter, field, binding target) it is tagged like a symbol: `^bool (go/id "true")` |

A tagged `(go/id "x")` in a parameter, field or binding position is a name like a tagged
symbol (amendment, accepted 2026-10-08): `text/template/parse` has a parameter named `true`.

**Resolution.** An unqualified symbol in Go context means what Go's scopes make of it: locals and
parameters innermost first, then the package's declarations (from all its files), then the
file's dot imports, then Go's universe. The forms keep Go's blocks (§7.3), so this is Go's
resolution. Type names and value names share Go's scopes as in Go.

**Reserved heads.** In Go context the head of a list is first one of the form names of this
spec (§13.5 lists them), whatever Go names are in scope. That keeps classification by head and
position, never by scope (go-lisp's invariant I2): the printer never resolves names. Go's
keywords cannot be identifiers, but Go's predeclared names and ordinary names can coincide with
form names (`fn`, `do`, `when`, `len` when shadowed). A **call** whose callee is a symbol that
is a reserved head is written `(go/call f args...)`: `fn(x)` with a local `fn` is
`(go/call fn x)`. Elsewhere such a symbol is a plain symbol (`(g fn)` passes the local `fn`).

**Namespace-qualified names in data.** Types the converter writes that the source did not spell
(inferred type arguments, §5.5; expression types, §8.3) may name types of packages the file does
not import. Those are qualified by the package's namespace: `go.internal.abi/Type`. A qualifier
with a dot is a namespace; one without is an import name of the file (Go import names have no
dots, and every Go-forms namespace has one).

## 5. Types

### 5.1 Type forms

| Go | type form |
|---|---|
| `int`, `string`, `error`, `any`, `byte`, `comparable` | `int`, `string`, `error`, `any`, `byte`, `comparable` (as written: `byte` stays `byte`) |
| `T`, `pkg.T`, a type parameter `E` | `T`, `pkg/T`, `E` |
| `*T` | `(* T)` |
| `[]T` | `(slice T)` |
| `[4]T`, `[N]T`, `[...]T` (composite literals only) | `(array 4 T)`, `(array N T)`, `(array ... T)` |
| `map[K]V` | `(map K V)` |
| `chan T`, `<-chan T`, `chan<- T` | `(chan T)`, `(chan :recv T)`, `(chan :send T)` |
| `func(int, ...any) (int, error)` | `(func [int & (slice any)] [int error])` (§5.3) |
| `struct{...}` | `(struct field*)` (§5.4) |
| `interface{...}`, `interface{}` | `(interface element*)`, `(interface)` (§5.4) |
| `Pair[string, int]`, `pkg.List[T]` | `(Pair string int)`, `(pkg/List T)` |
| the same, when the generic type's name is a reserved type head (`slice`, `array`, `tilde`) | `(inst slice int)` |
| `~int` (a term) | `(tilde int)` |
| `A \| B` (a union) | `(\| A B)` |

The reserved type heads are `*`, `slice`, `array`, `map`, `chan`, `func`, `struct`,
`interface`, `|`, `tilde` and `inst`. (`~` cannot be a symbol: the reader reads `~x` as unquote.)
Type forms appear only in type positions (§5.2), so `(* T)` is never multiplication and
`(func ...)` never a literal.

A type is written as the source wrote it: an alias stays the alias (`byte`, `rune`, `any`, a
declared alias), a parenthesised type loses its parentheses (N1). Array lengths are expressions:
a literal, a constant name or a constant expression form (§8.2).

### 5.2 Where types are written

| position | written as |
|---|---|
| parameter, result, field, local, package-level variable or constant | tag on the name: `^int32 x`; a compound type in the map form: `^{:tag (slice byte)} p` |
| single unnamed result | tag on the parameter vector: `(go/func Len ^int [...] ...)` |
| type of a type declaration | the last argument of `go/type` (§6.1) |
| constraint of a type parameter | tag on the parameter: `:type-params [^comparable K ^any V]` |
| conversion, composite literal, `make`, `new`, type assertion, type switch case, method expression | the type operand: `(conv T x)`, `(lit T ...)`, `(make T n)`, `(new T)`, `(assert T x)`, `(case [T U] ...)`, `(method-expr T M)` |
| type arguments | `(inst F T...)`, `(G T...)`, `:inst [T...]` metadata (§5.5) |
| expression type written by the converter | `:tag` on the expression form (§8.3) |

A tag is a declaration, never a conversion: `^{:tag (slice T)} xs` declares `xs`'s type.

### 5.3 Signatures

A function's signature is written as in `defn`, with Go's results:

```
params   = [param* (& variadic)?]
param    = ^T name                 ; named
         | T                       ; unnamed: a type form without :tag
variadic = ^{:tag (slice T)} name  ; ...T: the parameter's type inside the function is []T
         | (slice T)
results  = ^T params                         ; one unnamed result: tag on the parameter vector
         | params :results [^T r ...]       ; named results
         | params :results [T U ...]        ; several unnamed results
```

- The rule for vectors of parameters, results and struct fields: **a symbol with a `:tag` is a
  name with its type; any other element is a type** (an unnamed parameter, an embedded field).
  A blank name is `^T _`. A tagged `(go/id "x")` counts as a symbol (§4.4).
- **Field groups in generic declarations** (amendment, accepted 2026-10-08). Inside a generic
  declaration (a `go/type`, `go/func` or `go/method` with `:type-params`, a method with a
  generic receiver, a local generic type) gc's output depends on how names were grouped
  (`a, b T` against `a T, b T`: each written type is its own object in the generic
  dictionary), so N3 does not apply there. A name declared in one field with the name before it
  carries `^:go/grouped`; a name declared apart although its type equals the previous name's
  carries `^{:go/grouped false}`. This holds for type parameters, parameters, results and
  struct fields: `(go/func Max :type-params [^cmp/Ordered T] ^T [^T a ^:go/grouped ^T b] ...)`
  is `func Max[T cmp.Ordered](a, b T) T`. The printer prints exactly the groups the markers
  say; elsewhere it groups consecutive names of equal type or not, as it likes (N3).
- `func f()` has `[]` and no result tag. A Go function's results cannot be inferred: no tag
  and no `:results` means no results.
- In a **function type** the results are a second vector: `(func [params] [results])`, and
  `(func [params])` when there are none. Named parameters and results may appear there too:
  `(func [^int x] [^int n ^error err])`.
- In an **interface method** the element is `(Name params)` with the same result syntax as a
  declaration: `(Read [^{:tag (slice byte)} p] :results [^int n ^error err])`,
  `(String ^string [])`.

### 5.4 Struct and interface types

```clojure
(struct ^int32 X ^int32 Y)                                  ; struct{ X, Y int32 }
(struct Point ^{:tag string :go/tag "json:\"name\""} Name)  ; Point embedded; a struct tag
(struct (* pkg/T) ^{:tag (slice E)} items)                  ; *pkg.T embedded
(struct)                                                    ; struct{}
```

- Fields are in declaration order, one per name (N3). A field's struct tag is `:go/tag`, its
  doc comment `:doc` (§10.4).
- **Embedded fields** are written as their type, without a name: `Point`, `(* pkg/T)`,
  `(List int)`. The field's name is the type name, as Go defines it. Metadata on the type form
  holds the embedded field's `:go/tag` and `:doc`.

```clojure
(interface (Area ^float64 []) io/Reader)              ; methods and embedded interfaces
(interface (tilde int) (tilde float64))               ; a constraint: two lines are an intersection
(interface (| (tilde int) (tilde float64) string))    ; ~int | ~float64 | string
```

An interface element is a **method** when it is a list whose head is a plain symbol and whose
second element is a vector; anything else is an embedded type or a type set term. Elements are
in source order.

### 5.5 Generics

- **Type parameters** are `:type-params [P ...]` on `go/type`, `go/func` and `go/method`, each
  `P` a symbol tagged with its constraint: `^any T`, `^comparable K`,
  `^{:tag (tilde (slice E))} S`, `^cmp/Ordered E`. An untagged parameter means `any`.
- **Receivers** of methods on generic types name the receiver's type parameters in the
  receiver's type, as Go does: `^{:tag (* (Stack T))} s` declares `T` for the method. A generic
  method (Go 1.27) has its own `:type-params` as well.
- **Explicit instantiation** in source is written: `(inst slices/Sort (slice int))` for the
  function value `slices.Sort[[]int]`, `(Stack int)` for the type `Stack[int]`. A partial
  instantiation `F[A]` is `(inst F A)`. When the operand of `new` or of `method-expr` is itself
  a generic type's instance, it is written `(inst G T...)`, which means `G[T...]` in any
  position: there `(G T)` would be ambiguous for a consumer without declarations, since `new`'s
  operand may be a value (Go 1.26: `new(f(x))` is a call), and a list with a plain head there
  is a call (amendment, accepted 2026-10-08). `(new (inst atomic/Pointer T))` is
  `new(atomic.Pointer[T])`, `(new (f x))` is `new(f(x))`, `(method-expr (inst List int) Len)`
  is `List[int].Len`; inside a type form (`(method-expr (* (List T)) Push)`) the position is a
  type position and `(G T)` stays.
- **Inferred type arguments** (go/types `Instances` without explicit arguments in source) are
  `:inst` metadata on the symbol that names the generic function or method, with *all* type
  arguments: `(^{:inst [(slice int) int]} slices/Sort x)` for `slices.Sort(x)`, and
  `(^{:inst [string]} .Apply l f)` for a generic method. On a partial instantiation the symbol
  in `(inst F A)` carries the full list. The printer ignores `:inst`; a consumer reads it
  instead of inferring.
- A use of a generic *type* always has its arguments in source (Go infers none for types), so
  it needs no metadata.

## 6. Declarations

### 6.1 `go/type`

```
(go/type Name doc? (:type-params [P ...])? type-form)       ; type Name[P ...] T
(go/type ^:alias Name doc? (:type-params [P ...])? type-form) ; type Name[P ...] = T
(go/type doc? [spec] [spec] ...)                              ; type ( ... ), a group
spec = ^:alias? Name (:type-params [P ...])? type-form         ; inside the vector
```

```clojure
(go/type Point (struct ^int32 X ^int32 Y))
(go/type Celsius float64)
(go/type Shape (interface (Area ^float64 [])))
(go/type Stack :type-params [T] (struct ^{:tag (slice T)} items))
(go/type ^:alias IntAlias int)
(go/type ^:alias Set :type-params [^comparable K] (map K bool))   ; generic alias, Go 1.24
```

Methods are separate `go/method` forms, as in Go. A **group** is a `go/type` whose arguments,
after an optional doc string, are all vectors; each vector is one spec, with the spec's doc as
`:doc` on its name. A group of one spec is kept (`type (A int)` is `(go/type [A int])`).

Local type declarations are `let-type` (§7.3).

### 6.2 `go/const`

```
(go/const target init)               ; const X = e, const X T = e
(go/const doc? [target init?] ...)   ; const ( ... ), a group
target = ^{:tag T? :val v :doc s?} Name
       | (values ^{...} A ^{...} B)  ; const A, B = e1, e2
init   = expr | (values expr ...)
```

```clojure
(go/const ^{:val 4} UTFMax 4)
(go/const
  [^{:tag Weekday :val 0} Sunday iota]
  [^{:val 1} Monday]                    ; implicit repetition of `Weekday = iota`
  [^{:val 2} Tuesday])
(go/const ^{:val 1267650600228229401496703205376N} Big (<< 1 100))
```

- The init is the source expression, so `iota`, implicit repetition and the group survive
  (a group's iota is the spec's index in it). A spec without init repeats the previous spec's
  type and init list, as in Go.
- **`:val` on every constant name** is go/types' value of that constant, represented as §8.2
  says (exact, at the constant's type). A consumer never needs to fold a constant
  declaration; the printer ignores `:val`.
- `:tag` is the type the spec spells, if any. A constant without `:tag` is typed when its
  init is typed (`X = int32(5)`) and untyped otherwise; `:val`'s representation says the
  untyped kind (§8.2).
- Local constants are `let` bindings marked `^:const` (§7.3).

### 6.3 `go/var`

```
(go/var target init?)               ; var X T, var X = e, var X T = e
(go/var doc? [target init?] ...)    ; var ( ... ), a group
target = ^{:tag T? :doc s?} Name | (values A B ...)
```

```clojure
(go/var ErrEmpty (errors/New "empty"))
(go/var ^{:tag (array 256 uint8)} first (lit (array 256 uint8) as as ...))
(go/var (values ^int a ^int b))                  ; var a, b int
(go/var (values r w) (os/Pipe))                  ; var r, w = os.Pipe()
(go/var ^{:tag io/Reader} _ (conv (* T) nil))    ; var _ io.Reader = (*T)(nil)
```

Package-level variables stay in **source order**. Their initialisation order is go/types'
`InitOrder`, recorded in `go/package`'s `:init-order` as a vector with one entry per
initializer: the variable's symbol, `(values a b)` for an initializer of several variables, and
for a blank variable `["file.go" line col]` (the position of its `_`), also as an element of
`(values ...)` (amendment, accepted 2026-10-08). A consumer runs the
initializers in that order, then each file's `init` functions in file order and, within a file,
in source order (Go's rules); it does not compute the order itself.

### 6.4 `go/func` and `go/method`

```
(go/func Name doc? (:type-params [P ...])? signature body*)
(go/method Name doc? (:type-params [P ...])? [receiver param*] results? body*)
```

```clojure
(go/func Divmod [^int a ^int b] :results [^int q ^int r]
  (set! q (/ a b))
  (set! r (% a b))
  (return))

(go/method Move [^{:tag (* Point)} p ^int32 dx]
  (set! (.-X p) + dx))

(go/method String ^string [^Point p]
  (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))

(go/method Apply :type-params [F] ^{:tag (List F)} [^{:tag (List E)} l ^{:tag (func [E] [F])} f]
  nil)
```

- The **receiver** is the first parameter, as in `deftype` methods. Its tag (`T` or `(* T)`)
  decides value or pointer receiver. An unnamed receiver is its type alone: `[(* T)]`.
- **Named results** are variables of the function, zero at entry; `(return)` returns them.
- **Bodies.** A function without a body (implemented in assembly or by `//go:linkname`) has
  `^:extern` on its name and no body forms: `(go/func ^:extern ^:go/noescape memmove [...])`.
  Without `^:extern`, no body forms is the empty body `{}`.
- `init` and `main` are ordinary `go/func` forms named `init` and `main`; a package may have
  several `init`s, which nothing can call, as in Go.
- Methods may appear in any file of the package and before their type, as in Go.
- **The return value.** In a function with results, when the last form of the body, looking
  through `let` and `let-type` bodies, is an expression form other than a call of `panic`, it is
  the function's `return` statement: `(go/func Len ^int [...] (len x))` is
  `func ... int { return len(x) }`. The converter writes a final `return e` (one expression,
  which may yield several values) that way, and `return a, b` as `(return a b)`. A final
  `(return)` stays. The same holds for function literals.

### 6.5 Doc comments

Doc comments are kept as data, as Clojure keeps docstrings (§10.4): a doc string after the name
of `go/func`, `go/method` and a single `go/type`; `:doc` metadata on the name of a constant,
variable, field, interface method or spec in a group; a group's doc string right after the head;
`:doc` in `go/file` for the package doc. The text is the comment's text as `go/ast`'s
`CommentGroup.Text` gives it, without the directives: exactly that, its final newline included
(`"Numbers fundamental to the encoding.\n"`) (amendment, accepted 2026-10-08). Only a node's
`Doc` group is a doc comment; a line comment after a field or spec (go/ast's `Comment`, as in
`lo uint8 // lowest value for second byte.`) is an ordinary comment (§10.4) (amendment,
accepted 2026-10-08).

## 7. Code

### 7.1 Overview

| Go | Go form |
|---|---|
| `x := e`, `a, b := f()` | `(let [x e] ...)`, `(let [(values a b) (f)] ...)` |
| `x, err := g()` with `err` already declared in the block | `(let [(values x ^:assign err) (g)] ...)` |
| `var x T`, `var x T = e`, `var x = e`, `var a, b = f()` | `(let [^T x (zero T)] ...)`, `(let [^T x e] ...)`, `(let [^:var x e] ...)`, `(let [^:var (values a b) (f)] ...)` |
| `const N = 10`, `type T struct{}` in a function | `(let [^:const ^{:val 10} N 10] ...)`, `(let-type [T (struct)] ...)` |
| `x = e`, `a, b = b, a`, `_ = x` | `(set! x e)`, `(set! (values a b) (values b a))`, `(set! _ x)` |
| `x += e`, `x &^= m` | `(set! x + e)`, `(set! x bit-and-not m)` |
| `i++`, `i--` | `(inc! i)`, `(dec! i)` |
| `a[i] = v` | `(aset a i v)` |
| `{ ... }` | `(do ...)` |
| `if c {...}`, `if c {...} else {...}`, `if x := f(); c {...}` | `(when c ...)`, `(if c then else)`, `(when [x (f)] c ...)` |
| `if a {} else if b {} else {}` | `(cond a ... b ... :else ...)` |
| `switch`, type switch, `select` | `(switch ...)`, `(type-switch ...)`, `(select ...)` (§7.8) |
| `for c {}`, `for {}` | `(while c ...)`, `(while true ...)` |
| `for i := 0; i < n; i++ {}` | `(for [i 0] (< i n) (inc! i) ...)` |
| `for k, v := range x {}` | `(range [k v x] ...)` |
| `L:`, `break L`, `continue L`, `goto L`, `fallthrough` | `(label :L stmt)`, `(break :L)`, `(continue :L)`, `(goto :L)`, `(fallthrough)` |
| `L: x := e` (a labeled declaration) | `(let [^{:go/label :L} x e] ...)` |
| `return`, `return a, b` | `(return)`, `(return a b)`; a final `return e` is `e` (§6.4) |
| `go f(x)`, `defer f(x)` | `(go (f x))`, `(defer (f x))` |
| `ch <- v`, `<-ch` | `(>! ch v)`, `(<! ch)` |
| `x.f`, `x.M(a)`, `x.f(a)` (a func-typed field), `pkg.F(a)` | `(.-f x)`, `(.M x a)`, `((.-f x) a)`, `(pkg/F a)` |
| method value `x.M`, method expression `T.M`, `(*T).M` | `(.-M x)`, `(method-expr T M)`, `(method-expr (* T) M)` |
| `&x`, `*p`, `*p = v` | `(addr x)`, `@p`, `(set! @p v)` |
| `T{X: 1}`, `[]int{1, 2}`, `map[K]V{k: v}`, `[]P{{1, 2}}` | `(lit T :X 1)`, `(lit (slice int) 1 2)`, `(lit (map K V) [k v])`, `(lit (slice P) (lit _ 1 2))` |
| `a[i]`, `s[lo:hi]`, `s[lo:hi:max]`, `s[:n]` | `(aget a i)`, `(subslice s lo hi)`, `(subslice s lo hi max)`, `(subslice s _ n)` |
| `T(x)`, `x.(T)`, `v, ok := x.(T)` | `(conv T x)`, `(assert T x)`, `(let [(values v ok) (assert T x)] ...)` |
| `f(a, xs...)` | `(f a (spread xs))` |
| `func(x int) int { ... }` | `(fn ^int [^int x] ...)` |
| operators | §7.5 |
| literals | §8 |

### 7.2 The Go context

Go context is the inside of `go/type`, `go/const`, `go/var`, `go/func` and `go/method` forms:
their types, initializers and bodies, and everything nested in them. There:

- The forms of this spec are the only forms. A list is a form named by its head (§4.4,
  reserved heads) or else a call. Vectors are never expressions: they are binding vectors,
  parameter lists, init statements, case lists, key-value pairs. Maps and sets do not occur
  except as metadata.
- Every Go variable is a variable: assignable, addressable, captured by reference by function
  literals (Go 1.22 loop variables are per iteration). `^:mutable` is not written (§15 Q3).
- **Statement forms and expression forms.** The statement heads are `let`, `let-type`, `set!`,
  `aset`, `inc!`, `dec!`, `>!`, `go`, `defer`, `return`, `break`, `continue`, `goto`,
  `fallthrough`, `label`, `do`, `if`, `when`, `cond`, `switch`, `type-switch`, `select`, `for`,
  `while`, `range`. Every other form is an expression; in a statement position it is an
  expression statement (a call or a receive, as Go allows).
- **Bodies and branches.** A *body* (of a function, `when`, `let`, `do`, a `case` clause, a
  loop) is a statement list. A *branch* (the `then` and `else` of `if`, the values of `cond`)
  is one statement, or `(do ...)` whose forms are the branch's statement list: `(if c (f) (g))`
  and `(if c (do (f)) (do (g)))` are the same Go. A nested Go block in a branch is
  `(do (do ...))`. A `label` wraps one statement.

### 7.3 Blocks, scopes, local declarations

A Go block is a statement list. A declaration in it (`:=`, `var`, `const`, `type`) scopes from
the declaration to the end of the block. In the forms, the declaration is a `let` (or
`let-type`) whose body is **the rest of the block**; so a `let` is always the last form of its
statement list, and consecutive declarations share one `let`:

```clojure
(let [ch (make (chan int))
      done (make (chan (struct)))]   ; ch := ...; done := ...: two statements
  (go ...)
  (let [total 0]                     ; total := 0, after the go statement
    ...))
```

- A binding pair is one Go statement. `x e` is `x := e`; `(values a b) (values 1 2)` is
  `a, b := 1, 2`; `(values v ok) (aget m k)` is the comma-ok form `v, ok := m[k]` (two targets
  and an index, assertion or receive init; Go decides comma-ok by the same shape).
- **Redeclaration.** In `x, err := g()` with `err` declared earlier in the same block, `err` is
  assigned, not declared (go/types records it in `Uses`, not `Defs`). Such a target is marked
  `^:assign`. At least one target of a `:=` is new, as Go requires.
- **`var` statements.** `^T x e` is `var x T = e`; `^T x (zero T)` is `var x T`, and
  `(values ^T a ^T b) (zero T)` is `var a, b T`; `^:var x e` is `var x = e`, and
  `^:var (values a b) (f)`, the marker on the `values` form, is `var a, b = f()` (amendment,
  accepted 2026-10-08). `(zero T)` occurs only there.
- **Local constants** are bindings marked `^:const`, with `:val` as in §6.2:
  `(let [^:const ^{:tag int :val 10} N 10] ...)`. The implicit repetition and `iota` of a local
  `const` group are written out per constant (N6), each `iota` meaning its spec's index as in
  the source group. The group itself is kept (amendment, accepted 2026-10-08): in a group of
  several specs, the target of every spec after the first carries `^:go/grouped`, and the
  target of a spec without init in the source (its type and init written out) also
  `^:go/implicit`, so that the printer prints the group as written:

  ```clojure
  (let [^:const ^{:val 0} a iota                          ; const (a = iota
        ^:const ^:go/grouped ^:go/implicit ^{:val 1} b iota]  ;        b)
    ...)
  ```

- **Local types**: `(let-type [Name type-form ...] body...)`, with `^:alias` on aliases. A
  local type's name is in scope in its own type form (recursive types), as in Go. A local
  generic type (in `$GOROOT/test` and std's tests) carries its type parameters as
  `:type-params [P ...]` metadata on its name, the vector as in §5.5, so the binding vector
  keeps its name and type pairs (amendment, accepted 2026-10-08):
  `(let-type [^{:type-params [^any T]} Box (struct ^T v)] ...)`.
- **Labeled declarations.** `L: x := 1` (legal Go, 20 in std, such as
  `internal/runtime/maps`) is `^{:go/label :L}` on the first target of the `let` binding that
  holds the declaration: `(let [^{:go/label :L} x 1] ...)` (amendment, accepted 2026-10-08).
  `(label :L (let ...))` would make the `let`'s body the label's statement.
- **Explicit blocks** `{ ... }` are `(do ...)`. In a body, `(do ...)` always means a Go block.

### 7.4 Assignment

```
(set! place value)                   ; place = value
(set! (values place ...) init)       ; p1, p2 = e1, e2 or p1, p2 = f()
(set! place op value)                ; place op= value
(inc! place)  (dec! place)           ; place++  place--
(aset a i v)                         ; a[i] = v, the same as (set! (aget a i) v)
```

- A **place** is a variable symbol, `_`, `(.-f x)`, `(aget a i)` (arrays, slices, maps,
  pointers to arrays) or `@p`.
- `op` in `(set! place op value)` is one of `+ - * / % << >> bit-and bit-or bit-xor
  bit-and-not`, for Go's `+= -= *= /= %= <<= >>= &= |= ^= &^=`. Go evaluates the place's operands
  once; so does the form.
- In `(set! (values a b) (values b a))` all right-hand values and all index and pointer operands
  of the places are evaluated first, then assigned left to right, as Go specifies.
- The converter writes `(aset a i v)` for a single `a[i] = v` and `(set! (aget a i) ...)` in
  the other forms.

### 7.5 Operators

Operators mean Go's operators at the operands' type: `+` on `uint8` wraps, on strings
concatenates, on `float32` rounds to `float32`; `/` truncates on integers and panics on zero;
`<<` by a count of at least the width gives 0. Both operands of a binary operator have one type
(Go's rule), apart from shift counts.

| Go | form | arity |
|---|---|---|
| `a + b`, `a - b`, `a * b`, `a / b` | `(+ a b)`, `(- a b)`, `(* a b)`, `(/ a b)` | n-ary, left to right |
| `a % b`, `a << n`, `a >> n` | `(% a b)`, `(<< a n)`, `(>> a n)` | binary |
| `a & b`, `a \| b`, `a ^ b`, `a &^ b` | `(bit-and a b)`, `(bit-or a b)`, `(bit-xor a b)`, `(bit-and-not a b)` | n-ary, left to right |
| `a && b`, `a \|\| b` | `(and a b)`, `(or a b)` | n-ary, short-circuit |
| `a == b`, `a != b`, `a < b`, `a <= b`, `a > b`, `a >= b` | `(== a b)`, `(!= a b)`, `(< a b)`, `(<= a b)`, `(> a b)`, `(>= a b)` | binary only |
| `-x`, `+x`, `^x`, `!x` | `(- x)`, `(+ x)`, `(bit-not x)`, `(not x)` | unary |
| `&x`, `*p`, `<-ch` | `(addr x)`, `@p`, `(<! ch)` | unary |

- The names follow Go where the reader reads Go's spelling, and Clojure's names (which mean the
  same on Go's types) where it does not: `^` is the reader's metadata character and `&^`,
  `~` are not symbols, so the bit operations are Clojure's `bit-*` names and the logic operators
  Clojure's `and`, `or`, `not`.
- `@p` reads as `(clojure.core/deref p)` (or Arbace's `arbace.core/deref`); in Go context that
  list is pointer indirection, whatever the qualifier.
- **n-ary forms** are left-nested chains of one operator: `(+ a b c)` is `(a + b) + c`. The
  converter flattens a left-nested chain of the same operator into one form and keeps any other
  nesting: `a + (b + c)` is `(+ a (+ b c))`. A string chain `a + b + c` therefore stays one
  concatenation for gc, as in the source.
- **Parenthesized operands of `and` and `or`** (amendment, accepted 2026-10-08). gc removes
  dead branches of `&&` and `||` by syntax and does not look through parentheses, so they
  change its output (`bytes.IndexRune`, `strings.IndexRune`). An operand of `and` or `or` that
  the source parenthesized carries `^:go/paren`, and the printer parenthesizes it; a
  parenthesized operand of the same operator is not merged into the chain:
  `a && (b && c)` is `(and a ^:go/paren (and b c))`, `(a || b) && c` is
  `(and ^:go/paren (or a b) c)`. A literal operand cannot carry metadata, so the parentheses
  of `(true) && x` are lost. Other parentheses are dropped (N1).
- Comparisons are strictly binary: Clojure's `(< a b c)` means something else.
- A negative numeric literal is the unary minus of the literal, as Go parses it: `-1` is Go's
  `-1`, and `(- 1)` is never written. `-0.0` is the constant 0 (Go constants have no negative
  zero). This holds for Go's integer and float literals (`-1`, `-1.5`, `-0x10`, `-1E+400M`,
  `-1/4` for `-0x1p-2`); the minus of a rune or imaginary literal stays `(- x)`: `-'a'` is
  `(- \a)`, `-2i` is `(- (imaginary 2))` (amendment, accepted 2026-10-08).

### 7.6 Selectors, calls, methods

- `(.-name x)` is Go's selector `x.name` when it is not called: a field (`FieldVal`) or a
  method value (`MethodVal`); Go forbids a field and a method of one name at one depth, so the
  declarations decide which. `(.name x args...)` is a method call. A call of a func-typed
  field is `((.-f x) args...)`. Both calls print as `x.name(args)`.
- A qualified identifier `pkg/Name` is a package member: `(pkg/F a)` a call, `pkg/V` a variable,
  `(.-f pkg/V)` its field, `pkg/T` a type.
- **Embedded paths.** When a selection goes through embedded fields (go/types' `Selection.Index`
  longer than one), the selector form carries `:go/via`, the names of the embedded fields
  traversed, outermost first:

  ```clojure
  ^{:go/via [Point]} (.-X n)          ; n.X, X promoted from the embedded Point
  ^{:go/via [Point]} (.Move n Small)  ; n.Move(Small), Move promoted from *Point
  ```

  The metadata goes on the `(.-f x)` or `(.M x ...)` list. Whether a step dereferences a
  pointer follows from the embedded fields' types. The names are the helper's `:via` (the
  embedded fields of `Selection.Index()` but its last index, by name, outermost first; for
  literal keys, of the key's field path), copied as symbols (amendment, accepted 2026-10-08).
  A composite literal key naming a promoted field (Go 1.27) gets the same path in the
  literal's `:go/via` map (§7.7).
- **Receivers.** Implicit `&x` and `*p` on a method's receiver (`n.Move()` on an addressable
  `Named`, `p.String()` on a `*Point`) are not written; they follow from the method's receiver
  type and the operand's addressability.
- **Method expressions**: `(method-expr T M)` is `T.M`, `(method-expr (* T) M)` is `(*T).M`,
  `(method-expr io/Reader Read)` is `io.Reader.Read`.
- **Calls.** `(f args...)` calls `f`: a function, a function value, an instantiated generic
  `((inst F int) x)`, a method value. Variadic spreading is `(spread xs)` as the last argument:
  `(f a (spread xs))` is `f(a, xs...)`, `(append b (spread "abc"))` is `append(b, "abc"...)`.
  `(go/call f args...)` is a call whose callee's name is a reserved head (§4.4).

### 7.7 Builtins, conversions, assertions, composite literals, indexing

- **Builtins** are heads of their own name, with Go's argument rules: `(len x)`, `(cap x)`,
  `(append s a b)`, `(copy dst src)`, `(delete m k)`, `(clear x)`, `(close ch)`,
  `(make (slice int) n c)`, `(make (map K V))`, `(make (chan T) n)`, `(new T)`,
  `(new 42)` (Go 1.26: `new` of a value; the declarations decide whether a symbol operand is a
  type or a value, as in Go; a generic type's instance there is `(inst G T)`, §5.5),
  `(complex re im)`, `(real z)`, `(imag z)`, `(min a b)`, `(max a b)`, `(panic v)`,
  `(recover)`, `(print ...)`, `(println ...)`. `unsafe`'s are package members:
  `(unsafe/Sizeof x)`, `(unsafe/Add p n)`, `(unsafe/Slice p n)`, `(unsafe/String p n)`,
  `(unsafe/SliceData s)`, `(unsafe/StringData s)`, `(unsafe/Offsetof (.-f x))`,
  `(unsafe/Alignof x)`.
- **Conversions** `(conv T x)`, for every `T(x)` where go/types says the callee is a type,
  `unsafe.Pointer(p)` included: `(conv unsafe/Pointer p)`.
- **Type assertions** `(assert T x)`; comma-ok by a two-target binding or assignment (§7.3).
- **Composite literals**:

  ```
  (lit T element*)
  element = expr            ; positional
          | [key value]     ; arrays, slices, maps: an index or a key (any expression)
          | :field value    ; structs: a field name (Go 1.27: any selector of a promoted field)
  T       = a type form | _ ; _: the type is elided (inside an outer literal), also for &T
  ```

  ```clojure
  (lit Point 1 2)                                ; Point{1, 2}
  (lit Point :X 1 :Y 2)                          ; Point{X: 1, Y: 2}
  (lit (array 16 acceptRange) [0 (lit _ locb hicb)] [1 (lit _ 0xA0 hicb)])
  (lit (map string int) ["a" 1] ["b" 2])
  (addr (lit Point))                             ; &Point{}
  ^{:go/via {:X [P]}} (lit N :X 1 :Name "a")     ; N{X: 1, Name: "a"}, X promoted from P
  ```

  Elements keep source order. A Clojure map is never used: its order is not kept and the reader
  rejects duplicate keys.
- **Indexing** `(aget x i)` for arrays, pointers to arrays, slices, strings and maps; on a map
  in a two-target binding it is the comma-ok form. **Slicing**
  `(subslice x lo? hi? max?)`: omitted trailing parts are dropped, an omitted inner part is
  `_`: `s[i:]` is `(subslice s i)`, `s[:n]` is `(subslice s _ n)`, `s[:]` is `(subslice s)`,
  `s[:n:m]` is `(subslice s _ n m)`.

### 7.8 Control flow

**Init statements.** `if`, `when`, `switch` and `type-switch` take an optional **init vector**
as first argument, and `for` always has one:

```
init = []                  ; none (for only)
     | [target expr]       ; a := declaration: [x (f)], [(values v ok) (aget m k)]
     | [stmt]              ; any other simple statement: [(set! x 0)], [(inc! i)], [(f)]
```

Since vectors are never expressions, the init cannot be mistaken for a condition or a tag. Its
declarations scope over the whole statement (all branches and clauses), as in Go.

**if.**

```
(if init? cond then else?)        ; then, else: branches (§7.2)
(when init? cond body*)           ; if without else
(cond test branch test branch ... :else branch)
```

`(if c then)` is the same as `(when c then)`. An else branch that is an `if`, `when` or `cond`
form prints as `else if`; `else { if ... }` is `(do (if ...))`. The converter writes an
`else if` chain of two or more tests without init statements as `cond`, with `:else` for a final
`else`; it does so only when no `if` of the chain has an init statement, so that the chain
ends in a plain `else` block or in nothing. A chain with an init statement in one of its
`else if`s stays nested `if` and `when` forms, each printing as `else if` (amendment, accepted
2026-10-08).

**switch.**

```
(switch init? tag? clause*)
clause = (case [expr ...] stmt*) | (default stmt*)
```

```clojure
(switch (.-kind t)                  ; switch t.kind {
  (case [Int Uint] (f))             ; case Int, Uint: f()
  (default (g))                     ; default: g()
  (case [String] (h) (fallthrough)))
(switch [i (conv uint32 r)]         ; switch i := uint32(r); {
  (case [(<= i rune2Max)] ...))
```

- Without a tag the case expressions are conditions (`switch true`). The tag is any expression
  form (never a vector, never a `case`/`default` list). Clauses stay in source order, `default`
  where it was; `(fallthrough)` is the last statement of a clause that falls through.
- Each clause body is an implicit block, so a `let` in it scopes to the clause.

**type-switch.**

```
(type-switch init? guard clause*)
guard  = [x expr]       ; switch x := expr.(type)
       | expr           ; switch expr.(type)
clause = (case [type ...] stmt*) | (default stmt*)    ; nil is a type here: (case [nil] ...)
```

If the argument after the first vector is a clause, that vector is the guard; otherwise it is
the init and the guard follows. The per-clause variable `x` has the clause's type when the case
lists one type, and the guard's type otherwise (go/types' `Implicits`, derived).

**select.**

```
(select clause*)
clause = (case comm stmt*) | (default stmt*)
comm   = (>! ch v)                       ; case ch <- v:
       | (<! ch)                         ; case <-ch:
       | [target (<! ch)]                ; case x := <-ch:, case x, ok := <-ch: with (values x ok)
       | (set! target (<! ch))           ; case x = <-ch:, case x, ok = <-ch:
```

**Loops.**

```
(while cond body*)                     ; for cond {}; (while true ...) is for {}
(for init cond post body*)             ; for init; cond; post {}
(range [x] body*)                      ; for range x {}
(range [k x] body*)                    ; for k := range x {}
(range [k v x] body*)                  ; for k, v := range x {}
^:assign (range [k v x] body*)         ; for k, v = range x {} (k, v any places)
```

- In `for`, `cond` and `post` are `_` when omitted; `post` is a simple statement. A loop with
  only a condition is `while`. Go's `for true {}`, which is not `for {}`, is
  `(for [] true _ ...)`.
- `range` ranges over what Go ranges over (arrays, pointers to arrays, slices, strings, maps,
  channels, integers, functions); the kind follows from `x`'s type. `_` is a blank key.
- Each iteration has its own loop variables (Go 1.22), and the per-file `:lang` decides that
  for files at an older language version.

**Labels and jumps.** `(label :L stmt)` labels one statement; `(label :L)` is a label on an
empty statement. `(break)`, `(break :L)`, `(continue)`, `(continue :L)` mean Go's (`break` leaves
the innermost `for`, `switch` or `select`), `(goto :L)` and `(fallthrough)` likewise. `goto` is
kept as written (§15 Q9): Go forbids jumps into blocks and over declarations in scope at the
label, and with `let` scoping over the rest of a block that is exactly "into a `let` body from
outside it".

### 7.9 Function literals, goroutines, defer, panics, channels

- `(fn params results? body*)` is a function literal, with the signature syntax of §5.3:
  `(fn ^int [^int x] (* x 2))`, `(fn [] :results [^int n ^error err] ...)`. It captures
  variables by reference. A `fn` with a vector after its head is a literal; a call of a Go
  value named `fn` is `(go/call fn ...)`.
- `(go call)` and `(defer call)`: the callee and arguments are evaluated at once, the call runs
  later, as in Go. The operand is any call form: `(defer (.Unlock mu))`, `(go ((fn [] ...)))`.
- `(panic v)`, `(recover)`: Go's. `recover` returns the panic value only when called directly
  by a deferred function.
- `(>! ch v)` sends, `(<! ch)` receives; `(<! ch)` in a two-target binding or assignment is
  `v, ok := <-ch`. `(close ch)` closes. (The comma-ok receive needs no form of its own, unlike
  the survey's `<!?`: Go decides it by the shape of the assignment, and so do the forms.)

## 8. Constants and literals

### 8.1 Literals

Go's literals are written as Clojure literals with the same exact value. The forms text may keep
the source's radix; after reading, only the value remains (N4).

| Go | form | kind |
|---|---|---|
| `42`, `0x2A`, `0o52`, `052`, `0b101010`, `1_000` | `42`, `0x2A`, `052`, `052`, `2r101010`, `1000` | untyped int (long, or BigInt beyond 64 bits) |
| `1.5`, `1e10`, `.5`, `1e400`, `0x1p-2` | `1.5`, `1.0E10`, `0.5`, `1E+400M`, `1/4` | untyped float |
| `2i`, `1.5e3i` | `(imaginary 2)`, `(imaginary 1500.0)` | untyped complex |
| `'a'`, `'\n'`, `'é'`, `'\x00'`, `'😀'` | `\a`, `\newline`, `\é`, `\u0000`, `(rune 0x1F600)` | untyped rune |
| `"abc"`, `` `a\b` `` (raw) | `"abc"`, `"a\\b"` | untyped string |
| `"\xff\x00a"` (not valid UTF-8) | `(byte-string 0xff "\u0000a")` | untyped string |
| `true`, `false`, `nil` | `true`, `false`, `nil` | |

- **Integers** read as `long`, or as `BigInt` when they do not fit (the reader does that by
  itself; `N` may be written).
- **Floats are exact.** A Clojure `double` literal in Go context denotes the exact decimal value
  of its shortest representation (`Double/toString`), not its binary value: `0.1` is 1/10, as
  Go's untyped `0.1`. The converter writes a Go float literal as a double when that reads back to
  the literal's exact value, and otherwise as a `BigDecimal` (`1E+400M`,
  `0.1000000000000000055511151231257827021181583404541015625M`) or, when the value has no finite
  decimal expansion of reasonable length, a ratio. Hex floats are always exact as a ratio or
  `BigDecimal`. Integer-valued float literals keep a decimal point or exponent (`1.0`), so their
  kind stays float. Go's `.5` must be written `0.5`: the reader reads `.5` as a symbol.
  Precisely (amendment, accepted 2026-10-08): a decimal literal is a double when
  `Double/toString` of its nearest double denotes exactly its value, else a `BigDecimal`. A
  hexadecimal float is a ratio (`0x1p-2` is `1/4`), or a `BigDecimal` when integral (`0x1p4`
  is `16.0M`, since a ratio that is an integer reads as an integer). A non-integral value
  whose decimal expansion would be longer than 80 characters is a ratio.
- **Runes** in the Basic Multilingual Plane outside the surrogates are Clojure characters;
  others are `(rune n)`. `(rune n)` takes an integer literal and is a literal itself, not a
  conversion (`rune(x)` is `(conv rune x)`). The converter spells characters that do not
  print (controls, format characters, U+FFF0-U+FFFF) as `\uXXXX`, and uses the reader's names
  (`\newline`, `\space`, ...) where it has them.
- **Strings** are bytes in Go. A Clojure string denotes the UTF-8 encoding of its characters and
  must not contain unpaired surrogates. A string that is not valid UTF-8 is
  `(byte-string part ...)`: each part is a Clojure string (its UTF-8 bytes) or an integer 0-255
  (one byte), concatenated. The converter splits a literal into maximal valid UTF-8 runs and
  single invalid bytes.
- `(imaginary x)` takes a numeric literal and is Go's imaginary literal `xi`, not a call.

### 8.2 Constant values

`:val` (§6.2, §7.3) holds a constant's exact value at its type:

| kind | representation |
|---|---|
| boolean | `true`, `false` |
| string | a string, or `(byte-string ...)` |
| integer (typed integers, untyped int) | `long` or `BigInt` |
| rune (untyped rune) | a character or `(rune n)` |
| float (typed floats, untyped float) | as float literals in §8.1: a double when exact by that rule, else `BigDecimal` or ratio; a typed `float32` or `float64` value is the rounded value, so always a double; an integral untyped float is written with a decimal point or exponent: `1.0E100` denotes exactly 10^100 by §8.1's rule, so it is a double (amendment, accepted 2026-10-08: this text called it inexact); an untyped float beyond a rational's range is `(binary-float M E)` (below) |
| complex | `(complex re im)`, each part a float as above |

**Huge floats** (amendment, accepted 2026-10-08). go/types holds an untyped float whose
binary exponent is beyond ±2^14 (`1e1000000`; only in `$GOROOT/test`) as a `big.Float`, not a
rational, and its decimal or ratio would have up to hundreds of millions of digits. Its `:val`
is `(binary-float M E)`, meaning M·2^E with M an odd integer and E an integer, each a literal
(`(binary-float 3 4000000)` is 3·2^4000000). Like `(rune n)`, it is data, not a call.

**The kind comes from the type** (amendment, accepted 2026-10-08). The representation follows
the constant's type (the helper's `:t`), not the kind of go/constant's value, which can be
narrower. go/constant keeps a value in its smallest kind: `const C = 1 + 0i` is an
`untyped complex` constant with the integer value 1, so its `:val` is `(complex 1.0 0.0)`;
`const F float64 = 2` has the integer value 2 and the `:val` `2.0`; untyped runes have integer
values and get characters. A named type's kind is its underlying type's (the dump's `:types`);
for a named type of another package, whose underlying type is in that package's dump, the
value's kind decides.

The representation alone tells an untyped constant's kind; a typed constant's type is its
`:tag` or its init's type. go/types folds untyped floats exactly while they fit a rational and
with 512-bit precision beyond; `:val` is whatever it computed, so a consumer that folds the init
itself must agree with it, and the converter's value wins.

Array lengths in type forms are written as the source wrote them (§5.1): a literal, a
constant name or a constant expression. A consumer takes a constant name's value from its
declaration's `:val` and folds a length expression itself (lengths are small integer constant
expressions).

### 8.3 Untyped constants in code

Go converts an untyped constant to the type its context requires: an assignment's or
parameter's type, the other operand of a binary operator, a result type, the default type
otherwise. These rules are local, and a consumer applies them, as gc does: `(.Move n Small)`
converts `Small` to `int32` because `Move`'s parameter is `int32`.

One rule is not local, and there the converter writes the type: in a **non-constant shift**
whose left operand is an untyped constant, the operand takes the type the whole shift
expression would take in its context (`var b uint8 = 1 << s` shifts a `uint8`). The converter
puts go/types' type of the shift on the shift form as `:tag`:

```clojure
(let [^uint8 b ^{:tag uint8} (<< 1 s)] ...)
```

A `:tag` on an expression form is always go/types' type of that expression (a statement of
fact, never a conversion). The converter writes it where this spec asks; more cases may be
added as the compiler is built (§15 Q17).

## 9. Directives, `unsafe`, cgo, assembly, embedding

### 9.1 `//go:` directives

Every `//go:` directive (and every other `//name:` directive go/ast recognises, such as
`//export` and `//line`, §10.3) is kept. gc reads two kinds:

1. **Attached** to the declaration that follows: `nosplit`, `noinline`, `noescape`, `norace`,
   `nocheckptr`, `systemstack`, `nowritebarrier`, `nowritebarrierrec`, `yeswritebarrierrec`,
   `registerparams`, `uintptrkeepalive`, `uintptrescapes`, `cgo_unsafe_args`, `wasmimport`,
   `wasmexport`, `fix`, `embed` (before a `var` spec), `linkname` naming the declaration that
   follows, and any other `//go:` directive not listed under 2. They are metadata on the
   declared name, keyed `:go/<name>`, valued `true` when the directive has no arguments and
   otherwise the argument text verbatim (a vector of such strings when one directive occurs
   several times):

   ```clojure
   (go/func ^:go/nosplit ^:go/noinline f [] ...)
   (go/func ^:extern ^{:go/linkname "nanotime runtime.nanotime"} nanotime ^int64 [])
   (go/var ^{:go/embed ["a.txt" "b.txt"] :tag embed/FS} files)
   ```

   **Which declaration** (amendment, accepted 2026-10-08): the next one, as gc decides it, *also
   across blank lines and other comments*. gc's parser accumulates pragmas until the next
   top-level declaration takes them, and drops what is left after each declaration
   (`cmd/compile/internal/syntax/parser.go`, `takePragma` and `clearPragma`;
   `noder/noder.go`), so a blank line does not detach a directive; the text of 2026-10-08
   required "no blank line or other code between". Inside a `var`, `const` or `type` group a
   directive attaches to the next spec of the group. A directive after the last declaration
   of a file is free-standing.

   The printer writes them, one per line, between the doc comment and the declaration, sorted
   by name (gc does not depend on their order). After a doc comment it first writes a `//`
   line, as gofmt separates a doc comment's text from its directives (amendment, accepted
   2026-10-08); the converter's line accounting counts that line.
2. **Free-standing**: directives that gc applies by name or to the file (`linkname` naming
   another declaration, `cgo_import_dynamic`, `cgo_import_static`, `cgo_export_*`,
   `cgo_ldflag` and every other `cgo_*`, `linknamestd`, `generate`, `build` out of the header),
   and every directive that is not a `//go:` line (`//export f`, `//extern`, `//line`,
   `//tool:name args` such as `//gcassert:inline`). They are top-level forms in source order,
   where they stand: `(go/directive "//go:cgo_import_dynamic libc_read read \"libc.so\"")`,
   the text verbatim. A non-`//go:` directive is never `:go/<name>` metadata, which would print
   as a `//go:` line (amendment, accepted 2026-10-08).

Directives before the package clause are in `go/file` (§4.3): `//go:build` and `// +build` in
`:build`, the others (`//go:debug`) in `:directives`. Directives between the package clause and
the end of the imports are elements of `:imports` (§4.3). Directives inside function bodies
are not attached to anything gc reads; they are kept as statement-level `(go/directive "...")`
forms in the statement list where they stand, `//line` included (§10.3). A `//line` comment
that does not start at column 1 is not a directive for gc (`cmd/compile/doc.go`) and is an
ordinary comment (amendment, accepted 2026-10-08).

### 9.2 `go:linkname`

`linkname` is a directive like the others: attached when its first argument names the
declaration that follows it, free-standing otherwise (§9.1). A
`go/func ^:extern` with a `:go/linkname` (pull) has no body; one with a body and a one-argument
`linkname` (push) is an ordinary function. Resolving names is the linker's business.

### 9.3 `unsafe`

`unsafe` is an imported package: `unsafe/Pointer` is a type, `(conv unsafe/Pointer p)` a
conversion, `(unsafe/Add p 8)` and the others builtins (§7.7). Nothing more is needed in the
forms: gc compiles them, and phase b's runtime transcription needs them verbatim.
`uintptr` arithmetic is ordinary integer arithmetic.

### 9.4 cgo

Out of scope by configuration: `GOOS=tamago` builds with `CGO_ENABLED=0`, so files that import
`"C"` are excluded and no package has cgo files. The `//go:cgo_*` directives of `runtime`,
`syscall` and others are ordinary free-standing directives (§9.1).

### 9.5 Assembly and other files

Assembly (`.s`), C headers for it (`.h`) and `.syso` files are not translated. A body-less
`go/func ^:extern` is Go's declaration of an assembly function. `go/package`'s `:other-files`
lists the files, and the printer copies them next to the printed Go files (§12.4). Assembler
forms (survey §5.3) are a later spec.

### 9.6 `go:embed`

The directive is kept on its variable (§9.1). The embedded files are not data in the forms:
`go/package`'s `:embed-files` lists each matched file as `["path/in/package" "sha256"]`, and
the printer copies them from the original package directory, checking the hashes.

## 10. Positions and comments

gc's output depends on positions: line tables, DWARF, panic messages, `runtime.Caller` and the
inlinable bodies in export data. The forms keep them in one of two modes, recorded as
`go/package`'s `:positions`.

### 10.1 Mode `:lines` (the default)

Positions are lines, and they are carried by the layout of the forms text:

- **The forms file has the Go file's lines.** The converter writes each form that begins a Go
  line on that line number of the `.clj` file. Go's lines are never fewer than the forms need:
  Go puts one statement per line, and a closing `}` line becomes a blank line or a comment line.
  The `in-ns` and `go/file` forms take the place of the package clause and the imports: both
  start on the package clause's line, and comments before it (a copyright) are `;;` lines
  above (amendment, accepted 2026-10-08).
- The reader attaches `:line` (and the `.clj` column, `:column`) to every list it reads. In this
  mode `:line` is the Go line; `:column` is meaningless and ignored.
- **Symbols and vectors** that begin a Go line, and so carry no `:line` from the reader, get it
  written: `^{:line 87} rune2Max`; so do `@` forms. A **literal** or keyword that begins a Go
  line (it cannot carry metadata) is recorded in its parent's `:go/breaks`, a vector of the
  child indexes that begin a new line. An index is the child's index as `nth` gives it, the
  head being 0, and `:go/breaks` applies to every list, statement lists included: a final
  `nil`, `-1` or `"?"` that begins its Go line as `return nil` is a break of its function or
  `let` form (amendment, accepted 2026-10-08). The rows of `utf8`'s `first` table begin with
  the symbol `as` and need nothing; a table of numbers with 16 to a row, each row on a line of
  its own, gets `^{:go/breaks [2 18 34 50]} (lit (array 64 uint8) ...)` (`lit` is 0, the type
  1, the first element 2).
- **Lists placed after their line.** When the forms must put a list after its Go line (in
  `(assert T x)` the type precedes `x`, and a `T` that spans lines pushes `x` down), the list
  carries an explicit `:line`, which the reader keeps over its own (amendment, accepted
  2026-10-08; 8 lists in std, all type assertions to multi-line interface types).
- **Lists inside metadata** carry the reader's `:line` too, type forms in tags included
  (`^{:tag (func [int] [bool])} f`), and a consumer takes them as recorded lines. So the
  converter writes a declaration's doc string, name and signature on the declaration's line;
  a doc string on a forms line of its own before a signature would push the signature's types
  to later Go lines (amendment, accepted 2026-10-08).
- **End lines.** gc uses the line of a function's closing brace (the implicit return, deferred
  calls at exit). Every `go/func`, `go/method` and `fn` form carries `:go/end`, that line:
  `^{:go/end 73} (go/func Search ...)`.

The converter keeps the forms in memory with the same metadata (`:line` on lists, symbols and
vectors that begin lines, `:go/breaks`, `:go/end`); writing them out as text and reading them
back gives them again. Forms written by hand have the lines of their own text, which is what
source lines mean: printed Go then reports the `.clj` file's lines (§12.3).

What `:lines` does not keep: columns, and the lines of operators and brackets that gc uses as
node positions when they are not on the line where the node begins (gc's syntax tree positions
a binary expression at its operator, a call at its `(`, a selector at its `.`). For Go's usual
style (an operator ends its line) these coincide; the difference shows only in the line tables
of a few broken-up expressions, and it is what level 3 of §3.1 tolerates in this mode.

### 10.2 Mode `:full`

Every position of the syntax tree is kept, for byte-identical export data and line tables:

- Every list and symbol carries `:go/pos`, a map from the node's position fields (go/ast's
  field names, kebab-cased as the helper writes them) to `[line column]`:
  `^{:go/pos {:op-pos [24 9]}} (+ a b)`, `^{:go/pos {:name-pos [12 2]}} x`,
  `^{:go/pos {:lparen [31 7] :rparen [31 19]}} (f x)`. A selector also has `:dot`, the position
  of its `.` (go/ast has none; the helper computes it from the source); so does a type
  assertion `(assert T x)`, which gc also positions at its dot (amendment, accepted
  2026-10-08).
- Literals and keywords get theirs in the parent's `:go/apos`, a map from child index to
  `[line column]` (`:value-pos` positions).
- End positions (`:rbrace`, `:rparen`, `:rbrack`, `:colon` of case clauses, the file's end)
  are among the fields.
- Columns count bytes from 1, as Go's do; tabs count one.
- **Spliced nodes** (amendment, accepted 2026-10-08). Where a form stands for several go/ast
  nodes, it carries the positions of the nodes it absorbs: a `go/func`, `go/method` or `fn`
  form its `func` keyword and its body's braces (`:func`, `:lbrace`, `:rbrace`); a parameter
  or result vector its field list's `:opening` and `:closing`; `when`, the loops, `switch`,
  `type-switch` and `select` their body's `:lbrace` and `:rbrace`; `if` its then block's and
  the else block's (`:else-lbrace`, `:else-rbrace`); a selector also its name's `:name-pos`,
  and a qualified symbol `pkg/Name` the package name's `:x-pos`; a binding target its
  statement's `:tok-pos`. Not kept: the positions of parentheses (N1), of the `return` keyword
  of an implicit return (§6.4), of the inner operators of a merged chain (§7.5), and of a
  literal's parts beyond its own position (`:go/apos` holds one position per child).

This mode is for the G2 gate and for tools; it is not meant to be read.

### 10.3 Line directives

`//line` and `/*line*/` directives are kept as `(go/directive "//line foo.y:12")` at the
statement or declaration where they occur. Positions in the forms are the file's raw positions
(`token.FileSet.PositionFor(p, false)`); the printed directive adjusts them again in gc, as in
the original. A `/*line*/` directive inside an expression is not representable; the helper
warns, and the converter reports it and does not write it (none occurs in std; two programs of
`$GOROOT/test`, `issue29504.go` and `issue38698.go`, have some).

### 10.4 Comments

- **Doc comments** are data (§6.5), so they survive reading and printing. The converter keeps
  them by default.
- **Directives** are data (§9).
- **Other comments** are written by the converter as `;` comments in the forms text, at their
  Go line where the layout allows, and are lost when the forms are read: the printer does not
  reproduce them. They do not count for equivalence (N8). A comment is written at the end of
  its Go line (`; text`) or on a line of its own (`;; text`), each line of a block comment on
  its line. Line comments after fields and specs are among them (§6.5).

## 11. What the converter writes, what is derived

### 11.1 Written by the converter (from go/types)

| decision | go/types source | in the forms |
|---|---|---|
| which call is a conversion, which a builtin call | `Types[fun]` mode `type` / `builtin` | `conv`, builtin heads |
| constant values | `Types[e].Value`, `Const.Val()` | `:val` on constant names (§6.2, §7.3) |
| expression types the local rules do not give | `Types[e].Type` | `:tag` on the expression (§8.3) |
| inferred type arguments | `Instances` | `:inst` on the generic function's or method's symbol (§5.5) |
| embedded-field paths of selections | `Selections[e].Index()` (helper: `:sel`'s `:via`) | `:go/via` names (§7.6) |
| promoted keys of struct literals | the key's field object (helper: `:field`'s `:via`) | `:go/via` map on `lit` |
| field selection, method value, method expression | `Selections[e].Kind()` | `(.-f x)`, `(.M x ...)`, `((.-f x) ...)`, `method-expr` |
| new against reused names in `:=` | `Defs` / `Uses` | `^:assign` |
| package members, dot-imported names, universe names | `Uses` | `pkg/Name`, bare symbols, `go/call`, `go/id` |
| implicit import names | `Implicits[ImportSpec]` | `[name "path"]` |
| package initialisation order | `InitOrder` | `:init-order` |
| per-file language versions | `FileVersions` | `:lang` in `go/file` |
| comma-ok forms | `Types[e]` mode `commaok` | two-target bindings (the shape suffices) |
| constant kinds of `:val` | `Types[e].Type` of the constant (helper: `:t`), not the value's kind | `:val`'s representation (§8.2) |

The converter also writes, from the syntax tree rather than go/types, what gc's output depends
on beyond the normalisations (amendment, accepted 2026-10-08): `^:go/paren` on parenthesized
operands of `and` and `or` (§7.5), `^:go/grouped` in generic declarations (§5.3) and local
`const` groups with `^:go/implicit` (§7.3), and `:go/label` on labeled declarations (§7.3).

### 11.2 Derived by a consumer (from declarations)

| what | from |
|---|---|
| types of all other expressions | forward from declared signatures, `:inst`, `:tag`, and Go's local rules for untyped constants |
| implicit `&`/`*` of method receivers, implicit dereference in `p.f` and `p[i]` | receiver types, addressability, operand types |
| implicit conversions to interface types | assignability |
| per-clause variables of type switches | the case types (`Implicits[CaseClause]`) |
| `range` kinds, map and channel element types | the operand's type |
| struct layout, sizes, alignment, `unsafe.Sizeof` values | `types.SizesFor("gc", GOARCH)`'s rules |
| method sets, interface satisfaction, comparability | declarations, embedding |
| the meaning of `iota` | the spec's index in its group (and `:val` anyway) |
| scopes | the nesting of the forms (`types.Info.Scopes` is not written) |

### 11.3 What the helper must give the converter

The helper (developed in parallel under `tools/`) emits the typed tree generically. For this
spec the converter needs from it:

1. Every node with all its position fields, raw (not adjusted by line directives), and the
   position of the `.` of each selector and each type assertion (`:dot`, §10.2).
2. All comment groups with positions, so the converter can attach doc comments, directives and
   `;` comments.
3. `Types` (mode, type, value), `Defs`, `Uses`, `Implicits`, `Selections` (kind, index path,
   indirect), `Instances` (type arguments), `InitOrder`, `FileVersions`.
4. **Types as structured data**, not `types.TypeString` strings: the converter writes type
   forms in `:inst` and `:tag`, and must qualify named types by package *path* (to map them to
   import names or namespaces, §4.4) and keep aliases (`types.Alias`) and instances
   (`Named.TypeArgs`) apart. The helper gives a **type table** (amendment, accepted
   2026-10-08): one `:type-table` vector per dump, in dependency order, every annotation
   referring to a type by its id (its index). Named types, aliases and type parameters are
   leaves `{:kind :named :name N :pkg P}` (`:alias` with its `:actual`, `:type-param` with its
   `:index`), qualified by package path; an instance names its `:origin` and `:args`; other
   entries are interned by structure. A **local type** (declared in a function) and a **type
   parameter** are identified by `:decl`, their declaring position `"file.go:L:C"`, so two
   local types `T` of one package are two entries; the converter writes them by name, which
   Go's scopes resolve (§4.4). The table's full shape is in
   [HELPER-NOTES.md](HELPER-NOTES.md), "Types".
5. Constant values exactly, with go/constant's kind: the helper writes integers, strings,
   `(:float R)` with R an exact rational, `(:float M E)` for M·2^E beyond a rational's range,
   and `(:complex ...)`. That kind can be narrower than the constant's type, so the converter
   builds §8.2's representation from the type (`:t`), not from the kind (amendment, accepted
   2026-10-08).
6. For every key of a struct composite literal, the field's index path (go/types records only
   the field object), and for keys and selections the names of the embedded fields traversed
   (`:via`), which become `:go/via` (§7.6).
7. The configuration (§4.2), the file lists (`GoFiles` order, `SFiles`, `HFiles`, `SysoFiles`,
   `EmbedFiles`) and the embed patterns. The helper gives each of go list's lists apart; the
   converter merges the non-Go ones into `:other-files` (§4.2).

## 12. The printer

The printer turns Go forms into Go source that gc compiles as it compiles the original (§3).

### 12.1 Input

It reads the package file and the files it loads with `read` on a line-numbering reader (so
`:line` metadata is present), never evaluating anything. It needs no name resolution: every form
is classified by its head and position (§4.4). It needs no type information except what the
forms carry; the metadata the printer ignores (`:val`, `:inst`, `:go/via`, `:tag` on
expressions) is for consumers.

### 12.2 Syntax

- One `.go` file per `go/file`, named as recorded, with: build lines, header directives, the
  package doc and clause, the imports (one grouped `import (...)` when there are several), then
  the declarations in order.
- **Parentheses** where Go's grammar needs them, and only there (N1): by precedence (Go's five
  binary levels: `* / % << >> & &^`, then `+ - | ^`, then comparisons, then `&&`, then `||`;
  unary operators bind tighter), around a right operand of the same precedence
  (`a - (b - c)`), around composite literals whose type is a bare name in `if`, `for` and
  `switch` headers (`if x == (T{}) {`, also for instantiated generic types:
  `switch (P[int]{}) {`), around conversions to types that begin with `*`, `<-`, `func` or
  `chan` (`(*T)(x)`, `(<-chan int)(c)`, `(func())(f)`, `(chan int)(c)`), and for
  `chan (<-chan T)`. A type that begins with `[` needs none (`[]byte(s)`) (amendment, accepted
  2026-10-08). Beyond the grammar, an operand of `and` or `or` marked `^:go/paren` is
  parenthesized (§7.5).
- **Groups.** In a file whose forms carry `:go/grouped`, names are grouped exactly as marked
  (§5.3); local `const` bindings marked `^:go/grouped` print as one `const ( ... )` group,
  those marked `^:go/implicit` without their type and init (§7.3). A binding with `:go/label`
  prints with its label (`L: x := 1`). A `go/directive` element of `:imports` prints between
  the import declarations where it stands (§4.3).
- `(- -1)` prints `- -1` (or `-(-1)`): two `-` never touch. A type parameter list with one
  parameter whose constraint begins with `*` or `(` gets a trailing comma (`[P *T,]`).
- A n-ary form prints as a left-associative chain without parentheses.
- `(byte-string ...)` prints as an interpreted string with `\x` escapes for the single bytes;
  other strings print as interpreted strings (printable characters as they are, `\n`, `\t`,
  `\"`, `\\`, other controls as `\x` or `\u`), or as raw strings when that is valid and the
  string has a newline (N4 makes both equal).
- Float values print as Go literals with the same exact value (§8.1): a double as its
  shortest representation, a `BigDecimal` as its decimal string, a ratio as a decimal when its
  expansion is finite and short, and otherwise as a hex float (a ratio in code comes from a Go
  literal, whose value has a finite decimal or binary expansion).

### 12.3 Layout

- **Mode `:lines`**: before a form with a `:line` greater than the current line, the printer
  ends the line (and adds a trailing comma where Go's semicolon rule needs one, before a `)` or
  `}` that follows on a later line). `:go/breaks` and `:go/end` likewise. It never puts a form
  on a later line than recorded. When a line break is not allowed at that point, it writes a
  `/*line :N:1*/` directive instead, and when a form would land after its line (the header
  took more lines than the original's), a `//line :N:1` line before it. The column is needed:
  a line directive without a column keeps the previous file name only when it has one
  (`cmd/compile/doc.go`; `syntax/parser.go`, `updateBase`), so `/*line :N*/` would record an
  empty file name (amendment, accepted 2026-10-08). Indentation is tabs, as gofmt's.
- **gofmt** is not run over the output (amendment, accepted 2026-10-08). gofmt keeps lines
  only where the layout is already its own, and gofmt 1.27.1 breaks `switch (P[int]{}) {`: its
  `stripParens` does not count an instantiated type as a type name and drops parentheses the
  parser needs. The printer does gofmt's spacing and alignment itself (go/printer's rules and
  `text/tabwriter`); with no positions, its output is gofmt's fixed point.
- **Mode `:full`**: every token at its recorded line and column, with newlines, spaces, and
  `/*line :L:C*/` directives where a position cannot be reached by layout (a token whose
  column is left of the cursor). The output is not gofmt-formatted.
- **No positions** (forms built by a program): the printer lays the code out as gofmt would.
- Forms written by hand in an Arbace source: the printer may also emit
  `//line file.clj:N` directives, so that gc's positions name the forms file (a printer
  option; not used by the round trip).

### 12.4 Building the printed package

For levels 2-4 of §3.1 the printed package is compiled exactly as the original:

- The same toolchain (`/root/tamago-go/bin/go`, `GOROOT=/root/tamago-go`), environment
  (`GOOS`, `GOARCH`, `GOAMD64`/`GOARM64`, `CGO_ENABLED=0`, `GOEXPERIMENT`), tags and flags, with
  `-trimpath`.
- The printed files replace the originals through `go build -overlay`, which keeps every path,
  file name and the package's import path unchanged; the non-Go files (`:other-files`, embedded
  files) are those of the original directory.
- Comparison of export data and object files is the comparator's (`tools/`, developed in
  parallel).

## 13. Coverage

These tables check the spec against the authoritative lists of go1.27.1: `go/ast`'s node types
(`src/go/ast/ast.go`), `go/token`'s operators, `go/types`' `Info` maps, type kinds and object
kinds, and the universe's builtins.

### 13.1 `go/ast` nodes

| node | form |
|---|---|
| `File` | the file's `go/file` form and its top-level forms (§4.3) |
| `Package` (deprecated) | not used: a package is `go/package` (§4.2) |
| `Comment`, `CommentGroup` | doc strings and `:doc`, directives (§9), `;` comments (§10.4) |
| `GenDecl` `import` | `go/file :imports`, with `go/directive` elements among the specs (§4.3) |
| `GenDecl` `const`, `var`, `type` | `go/const`, `go/var`, `go/type` (groups as vectors); in functions `let`, `let-type` (§7.3), a local `const` group as `^:go/grouped` and `^:go/implicit` bindings |
| `ImportSpec` | `[name "path"]`, `^:alias`, `_`, `.` |
| `ValueSpec` | a target and init (§6.2, §6.3) |
| `TypeSpec` | a `go/type` or a spec vector; `^:alias` for `Assign` set; in a function a `let-type` pair, `:type-params` metadata on a generic one's name |
| `FuncDecl` | `go/func`, `go/method` |
| `Field`, `FieldList` | tagged symbols and types in parameter, result and field lists (§5.3, §5.4); interface elements; `^:go/grouped` for the field groups of generic declarations (§5.3) |
| `BadDecl`, `BadStmt`, `BadExpr` | none: the converter rejects invalid packages |
| `Ident` | a symbol; `pkg/Name`; `true`, `false`, `nil`; `(go/id "...")`, tagged as a declared name (§4.4) |
| `BasicLit` | Clojure literals, `(rune n)`, `(imaginary x)`, `(byte-string ...)` (§8.1); with a unary `-`, a negative Clojure number (§7.5) |
| `CompositeLit` | `(lit T element*)` |
| `KeyValueExpr` | `[key value]`, `:field value` |
| `FuncLit` | `(fn ...)` |
| `ParenExpr` | none (N1); `^:go/paren` on an operand of `and` or `or` (§7.5) |
| `SelectorExpr` | `(.-f x)`, `(.M x ...)`, `pkg/Name`, `(method-expr T M)` |
| `IndexExpr`, `IndexListExpr` | `(aget x i)`; `(inst F T...)`, `(G T...)` |
| `SliceExpr` | `(subslice x lo hi max)` |
| `TypeAssertExpr` | `(assert T x)`; in a type switch, the guard; its `:dot` in mode `:full` (§10.2) |
| `CallExpr` | a call, `(conv T x)`, a builtin, `(go/call f ...)`; `Ellipsis` set: `(spread xs)` |
| `StarExpr` | `@p`; `(* T)` in type positions |
| `UnaryExpr` | `(- x)`, `(+ x)`, `(bit-not x)`, `(not x)`, `(addr x)`, `(<! ch)`; `(tilde T)` in constraints |
| `BinaryExpr` | §7.5; `(\| A B)` in constraints |
| `Ellipsis` | `&` in parameters, `(array ... T)` |
| `ArrayType`, `StructType`, `FuncType`, `InterfaceType`, `MapType`, `ChanType` | type forms (§5.1) |
| `DeclStmt` | `let`, `let-type`, `^:const` bindings, `^:var` on a binding or a `values` target |
| `EmptyStmt` | none (N2); `(label :L)` |
| `LabeledStmt` | `(label :L stmt)`; on a declaration, `:go/label` on the first target of its `let` (§7.3) |
| `ExprStmt` | the expression form |
| `SendStmt` | `(>! ch v)` |
| `IncDecStmt` | `(inc! x)`, `(dec! x)` |
| `AssignStmt` | `set!` (3 shapes), `aset`, `let` for `:=` |
| `GoStmt`, `DeferStmt` | `(go call)`, `(defer call)` |
| `ReturnStmt` | `(return e*)`; the body's last expression (§6.4) |
| `BranchStmt` | `(break :L?)`, `(continue :L?)`, `(goto :L)`, `(fallthrough)` |
| `BlockStmt` | a body; `(do ...)` |
| `IfStmt` | `if`, `when`, `cond`, init vectors |
| `CaseClause` | `(case [...] ...)`, `(default ...)` |
| `SwitchStmt`, `TypeSwitchStmt` | `switch`, `type-switch` |
| `CommClause`, `SelectStmt` | `(case comm ...)`, `(default ...)`, `select` |
| `ForStmt` | `while`, `for` |
| `RangeStmt` | `range`, `^:assign` |

### 13.2 Operators and assignment tokens

| tokens | forms |
|---|---|
| `+ - * / %` | `+ - * / %` |
| `& \| ^ &^` | `bit-and bit-or bit-xor bit-and-not` |
| `<< >>` | `<< >>` |
| `&& \|\| !` | `and or not` |
| `== != < <= > >=` | `== != < <= > >=` |
| unary `- + ^ & * <-` | `(- x)`, `(+ x)`, `(bit-not x)`, `(addr x)`, `@p`, `(<! ch)` |
| `~` (constraint term) | `tilde` |
| `=`, `:=`, `op=` (11), `++`, `--` | `set!`, `let`, `(set! place op v)`, `inc!`, `dec!` |
| `...` | `&`, `spread`, `...` (array length) |
| `<-` in send, `chan<-`, `<-chan` | `>!`, `(chan :send T)`, `(chan :recv T)` |

### 13.3 `types.Info`

| map | use |
|---|---|
| `Types` | conversions and builtins by mode; constant values (`:val`), represented by the constant's type (§8.2); `:tag` on shifts (§8.3); comma-ok by mode; types for `:inst` |
| `Instances` | `:inst`, and `inst` for explicit instantiation |
| `Defs` | declared objects; new names of `:=` (others get `^:assign`) |
| `Uses` | package members, dot imports, universe, `go/call`; reused names of `:=` |
| `Implicits` | import names (`ImportSpec`); type-switch clause variables (derived, not written); unnamed parameters (nothing) |
| `Selections` | `.-` against `.M` against field calls, `method-expr`; `:go/via` (the helper's `:via`) |
| `Scopes` | not written: the forms' nesting is Go's block structure |
| `InitOrder` | `:init-order` |
| `FileVersions` | `:lang` |

### 13.4 Types and objects

| `types.Type` | form |
|---|---|
| `Basic` (typed) | `int` ... `uintptr`, `string`, `bool`, `unsafe/Pointer` |
| `Basic` (untyped kinds) | never a type form: the kind is in `:val`'s representation (§8.2), `(binary-float M E)` included |
| `Pointer`, `Slice`, `Array`, `Map`, `Chan` | `(* T)`, `(slice T)`, `(array n T)`, `(map K V)`, `(chan ...)` |
| `Signature` | `(func [...] [...])`; signatures in declarations (§5.3) |
| `Struct`, `Interface`, `Union` (and terms) | `(struct ...)`, `(interface ...)`, `(\| ...)`, `(tilde T)` |
| `Named` (and instances) | `T`, `pkg/T`, `(T args...)`, `(inst T args...)` |
| `Alias` (and generic alias instances) | the alias's name, `(A args...)` |
| `TypeParam` | its name |
| `Tuple` | `:results`, `(values ...)` |

| `types.Object` | form |
|---|---|
| `Var` (package, local, parameter, result, field) | `go/var`, `let` binding, parameter, result, field |
| `Const` | `go/const`, `^:const` binding |
| `TypeName` | `go/type`, `let-type`, type parameters |
| `Func` (function, method, generic method) | `go/func`, `go/method` |
| `PkgName` | import spec |
| `Builtin` | builtin heads (§7.7) |
| `Label` | keywords |
| `Nil` | `nil` |

### 13.5 Reserved heads in Go context

- Statements: `let let-type set! aset inc! dec! >! go defer return break continue goto
  fallthrough label do if when cond switch type-switch select case default for while range`.
- Expressions: `+ - * / % << >> == != < <= > >= and or not bit-and bit-or bit-xor bit-not
  bit-and-not addr <! lit conv assert aget subslice spread values inst method-expr fn zero
  rune imaginary byte-string`, and `deref` (from `@`).
- Builtins: `append cap clear close complex copy delete imag len make max min new panic print
  println real recover`.
- Types (in type positions): `* slice array map chan func struct interface | tilde inst`.
- Qualified, at top level or anywhere: `go/package go/file go/type go/const go/var go/func
  go/method go/directive go/call go/id`.
- Data in `:val` only (§8.2), never code: `binary-float`, besides the literal heads `rune`,
  `byte-string` and `complex`.

A Go identifier equal to an unqualified reserved head is written as itself everywhere except
as a callee, where it is `(go/call name ...)` (§4.4). In the standard library outside tests
this concerns mostly locals named `fn` (about 170 calls).

## 14. Worked examples

The examples show mode `:lines` without the metadata the layout carries; `:go/end` is shown
once. Their layout is illustrative: the converter puts each form on its Go line (§10.1), with
`in-ns` and `go/file` on the package clause's line, which these excerpts do not all do
(amendment, accepted 2026-10-08: the text said §14.1's forms were on Go's lines, but they are
one line off, and `:go/end 15` is `Move`'s Go line, not its forms line). Doc strings ending in
`...` are shortened.

### 14.1 The survey's sample (`.tmp/g2c/sample/sample.go`)

```go
package sample

import (
	"errors"
	"fmt"
)

type Point struct{ X, Y int32 }

type Named struct {
	Point // embedded
	Name  string
}

func (p *Point) Move(dx int32) { p.X += dx }

func (p Point) String() string { return fmt.Sprintf("(%d,%d)", p.X, p.Y) }

type Stack[T any] struct{ items []T }

func (s *Stack[T]) Push(x T) { s.items = append(s.items, x) }

func (s *Stack[T]) Pop() (T, bool) {
	var zero T
	if len(s.items) == 0 {
		return zero, false
	}
	x := s.items[len(s.items)-1]
	s.items = s.items[:len(s.items)-1]
	return x, true
}

const (
	A = iota * 10
	B
	Big = 1 << 100
	Small = Big >> 98
)

var ErrEmpty = errors.New("empty")

func Sum(xs []uint8) (s uint8) {
	for _, x := range xs {
		s += x // wraps
	}
	return
}

func Safe(f func()) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("recovered: %v", r)
		}
	}()
	f()
	return nil
}

func Pipeline(n int) int {
	ch := make(chan int)
	done := make(chan struct{})
	go func() {
		defer close(ch)
		for i := range n {
			select {
			case ch <- i:
			case <-done:
				return
			}
		}
	}()
	total := 0
	for v := range ch {
		total += v
	}
	return total
}

func Describe(v any) string {
	switch x := v.(type) {
	case nil:
		return "nil"
	case fmt.Stringer:
		return x.String()
	case int, int64:
		return fmt.Sprint(x)
	}
	return "?"
}

func Use() int32 {
	n := Named{Point{1, 2}, "n"}
	n.Move(Small)
	p := &n.Point.Y
	*p++
	return n.X + *p
}
```

`go/sample.clj`:

```clojure
(ns go.sample
  (:require [arbace.go :as go]))

(go/package sample
  :path "sample"
  :config {:goos "tamago" :goarch "amd64" :goamd64 "v1" :toolchain "go1.27.1"
           :lang "go1.27" :tags [] :goexperiment "" :cgo false :compiler "gc"}
  :files ["sample.go"]
  :init-order [ErrEmpty]
  :positions :lines)

(load "sample/sample")
```

`go/sample/sample.clj` (laid out for reading; the blank lines stand for Go's closing braces):

```clojure
(in-ns 'go.sample)
(go/file "sample.go"
  :imports [[errors "errors"]
            [fmt "fmt"]])


(go/type Point (struct ^int32 X ^int32 Y))

(go/type Named (struct
  Point                                    ; embedded
  ^string Name))

^{:go/end 15} (go/method Move [^{:tag (* Point)} p ^int32 dx] (set! (.-X p) + dx))

(go/method String ^string [^Point p] (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))

(go/type Stack :type-params [T] (struct ^{:tag (slice T)} items))

(go/method Push [^{:tag (* (Stack T))} s ^T x] (set! (.-items s) (append (.-items s) x)))

(go/method Pop [^{:tag (* (Stack T))} s] :results [T bool]
  (let [^T zero (zero T)]
    (when (== (len (.-items s)) 0)
      (return zero false))

    (let [x (aget (.-items s) (- (len (.-items s)) 1))]
      (set! (.-items s) (subslice (.-items s) _ (- (len (.-items s)) 1)))
      (return x true))))


(go/const
  [^{:val 0} A (* iota 10)]
  [^{:val 10} B]
  [^{:val 1267650600228229401496703205376N} Big (<< 1 100)]
  [^{:val 4} Small (>> Big 98)])


(go/var ErrEmpty (errors/New "empty"))

(go/func Sum [^{:tag (slice uint8)} xs] :results [^uint8 s]
  (range [_ x xs]
    (set! s + x))                          ; wraps

  (return))

(go/func Safe [^{:tag (func [])} f] :results [^error err]
  (defer ((fn []
            (when [r (recover)] (!= r nil)
              (set! err (fmt/Errorf "recovered: %v" r))))))


  (f)
  nil)

(go/func Pipeline ^int [^int n]
  (let [ch (make (chan int))
        done (make (chan (struct)))]
    (go ((fn []
           (defer (close ch))
           (range [i n]
             (select
               (case (>! ch i))
               (case (<! done)
                 (return)))))))




    (let [total 0]
      (range [v ch]
        (set! total + v))

      total)))

(go/func Describe ^string [^any v]
  (type-switch [x v]
    (case [nil]
      (return "nil"))
    (case [fmt/Stringer]
      (return (.String x)))
    (case [int int64]
      (return (fmt/Sprint x))))

  "?")

(go/func Use ^int32 []
  (let [n (lit Named (lit Point 1 2) "n")]
    ^{:go/via [Point]} (.Move n Small)
    (let [p (addr (.-Y (.-Point n)))]
      (inc! @p)
      (+ ^{:go/via [Point]} (.-X n) @p))))
```

Notes:

- `p.X += dx` is `(set! (.-X p) + dx)`; with `p` a `*Point`, the dereference is derived.
- `Small` is untyped; `Move`'s parameter makes it `int32` (derived, §8.3). go/types' `:val "4"`
  at `int32` is what a consumer computes.
- `n.Move(Small)` goes through the embedded `Point` (`Selection.Index` `[0 0]`: field 0, then
  method 0), so `:go/via [Point]`; `&n.Point` for the pointer receiver is derived.
- `&n.Point.Y` spells the path itself and needs no metadata.
- `Safe`'s final `return nil` is the body's last value `nil`; `Sum`'s bare `return` stays.
- The type switch's clause variables (`x` as `any`, `fmt.Stringer`, `any`) are derived.
- The blank lines are the closing braces of the Go source.

### 14.2 `unicode/utf8` (excerpts)

```clojure
(go/const
  "Numbers fundamental to the encoding.\n"
  [^{:val \uFFFD} RuneError \uFFFD]   ; the "error" Rune or "Unicode replacement character"
  [^{:val 128} RuneSelf 0x80]                ; characters below RuneSelf are represented as ...
  [^{:val (rune 0x10FFFF)} MaxRune (rune 0x10FFFF)]   ; Maximum valid Unicode code point.
  [^{:val 4} UTFMax 4])                      ; maximum number of bytes of a UTF-8 encoded ...

(go/const
  [^{:val 0} t1 2r00000000]
  [^{:val 128} tx 2r10000000]
  ...
  [^{:val 127} rune1Max (- (<< 1 7) 1)]
  ...)

(go/const
  [^{:val \ï} runeErrorByte0 (bit-or t3 (>> RuneError 12))]
  [^{:val \¿} runeErrorByte1 (bit-or tx (bit-and (>> RuneError 6) maskx))]
  [^{:val \½} runeErrorByte2 (bit-or tx (bit-and RuneError maskx))])

(go/var ^{:doc "first is information about the first byte in a UTF-8 sequence.\n"} first
  (lit (array 256 uint8)
    ;;   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
    as as as as as as as as as as as as as as as as   ; 0x00-0x0F
    ...
    s5 s6 s6 s6 s7 xx xx xx xx xx xx xx xx xx xx xx)) ; 0xF0-0xFF

(go/type acceptRange
  "acceptRange gives the range of valid values for the second byte in a UTF-8\nsequence.\n"
  (struct ^uint8 lo     ; lowest value for second byte.
          ^uint8 hi))   ; highest value for second byte.

(go/var acceptRanges
  (lit (array 16 acceptRange)
    [0 (lit _ locb hicb)]
    [1 (lit _ 0xA0 hicb)]
    [2 (lit _ locb 0x9F)]
    [3 (lit _ 0x90 hicb)]
    [4 (lit _ locb 0x8F)]))
```

`RuneError` and `MaxRune` are rune constants, and `:val` keeps the kind: a character, or
`(rune n)` (data here, spelled as the literal) above the Basic Multilingual Plane. So are
`runeErrorByte0`-`2`: `t3 | RuneError>>12` combines an untyped int and an untyped rune, which
gives an untyped rune, so their `:val`s are the characters U+00EF, U+00BF and U+00BD, not the
integers 239, 191 and 189 (amendment, accepted 2026-10-08: the text showed integers). The
comments after `acceptRange`'s fields are line comments, not doc comments, so they are `;`
comments, not `:doc` (§6.5; amendment, accepted 2026-10-08). Doc strings end in a newline, as
`CommentGroup.Text` gives them (§6.5). The row
symbols of `first` begin their lines and get `^{:line ...}` in the text (§10.1), not shown.

```go
func RuneLen(r rune) int {
	switch {
	case r < 0:
		return -1
	case r <= rune1Max:
		return 1
	case r <= rune2Max:
		return 2
	case surrogateMin <= r && r <= surrogateMax:
		return -1
	case r <= rune3Max:
		return 3
	case r <= MaxRune:
		return 4
	}
	return -1
}
```

```clojure
(go/func RuneLen
  "RuneLen returns the number of bytes in the UTF-8 encoding of the rune. ..."
  ^int [^rune r]
  (switch
    (case [(< r 0)]
      (return -1))
    (case [(<= r rune1Max)]
      (return 1))
    (case [(<= r rune2Max)]
      (return 2))
    (case [(and (<= surrogateMin r) (<= r surrogateMax))]
      (return -1))
    (case [(<= r rune3Max)]
      (return 3))
    (case [(<= r MaxRune)]
      (return 4)))
  -1)
```

```go
func encodeRuneNonASCII(p []byte, r rune) int {
	// Negative values are erroneous. Making it unsigned addresses the problem.
	switch i := uint32(r); {
	case i <= rune2Max:
		_ = p[1] // eliminate bounds checks
		p[0] = t2 | byte(r>>6)
		p[1] = tx | byte(r)&maskx
		return 2
	case i < surrogateMin, surrogateMax < i && i <= rune3Max:
		...
	default:
		_ = p[2] // eliminate bounds checks
		p[0] = runeErrorByte0
		p[1] = runeErrorByte1
		p[2] = runeErrorByte2
		return 3
	}
}
```

```clojure
(go/func encodeRuneNonASCII ^int [^{:tag (slice byte)} p ^rune r]
  ;; Negative values are erroneous. Making it unsigned addresses the problem.
  (switch [i (conv uint32 r)]
    (case [(<= i rune2Max)]
      (set! _ (aget p 1))                                   ; eliminate bounds checks
      (aset p 0 (bit-or t2 (conv byte (>> r 6))))
      (aset p 1 (bit-or tx (bit-and (conv byte r) maskx)))
      (return 2))
    (case [(< i surrogateMin) (and (< surrogateMax i) (<= i rune3Max))]
      ...)
    (default
      (set! _ (aget p 2))                                   ; eliminate bounds checks
      (aset p 0 runeErrorByte0)
      (aset p 1 runeErrorByte1)
      (aset p 2 runeErrorByte2)
      (return 3))))
```

`switch i := uint32(r); {` has an init and no tag; `byte(r)&maskx` binds tighter than `|`, which
the nesting shows and the printer's precedence rules restore. The untyped constants `t2`, `tx`,
`maskx` become `byte` from the other operand (derived).

```go
func DecodeRuneInString(s string) (r rune, size int) {
	// Inlineable fast path for ASCII characters; see #48195.
	// ...
	if s != "" && s[0] < RuneSelf {
		return rune(s[0]), 1
	} else {
		r, size = decodeRuneInStringSlow(s)
	}
	return
}
```

```clojure
(go/func DecodeRuneInString
  "DecodeRuneInString is like [DecodeRune] but its input is a string. ..."
  [^string s] :results [^rune r ^int size]
  ;; Inlineable fast path for ASCII characters; see #48195.
  (if (and (!= s "") (< (aget s 0) RuneSelf))
    (return (conv rune (aget s 0)) 1)
    (set! (values r size) (decodeRuneInStringSlow s)))
  (return))
```

The `else` stays (the source has it; a restructured version would change the tree and,
as the comment says, the inlining cost). From `decodeRuneInStringSlow`:

```clojure
(let [mask (>> (<< (conv rune x) 31) 31)]                        ; Create 0x0000 or 0xFFFF.
  (return (bit-or (bit-and-not (conv rune (aget s 0)) mask)
                  (bit-and RuneError mask))
          1))
```

### 14.3 `sort` (excerpts)

```go
func insertionSort(data Interface, a, b int) {
	for i := a + 1; i < b; i++ {
		for j := i; j > a && data.Less(j, j-1); j-- {
			data.Swap(j, j-1)
		}
	}
}

func siftDown(data Interface, lo, hi, first int) {
	root := lo
	for {
		child := 2*root + 1
		if child >= hi {
			break
		}
		if child+1 < hi && data.Less(first+child, first+child+1) {
			child++
		}
		if !data.Less(first+root, first+child) {
			return
		}
		data.Swap(first+root, first+child)
		root = child
	}
}

type IntSlice []int

func (x IntSlice) Len() int           { return len(x) }
func (x IntSlice) Less(i, j int) bool { return x[i] < x[j] }
func (x IntSlice) Swap(i, j int)      { x[i], x[j] = x[j], x[i] }

func Ints(x []int) { slices.Sort(x) }
```

```clojure
(go/func insertionSort [^Interface data ^int a ^int b]
  (for [i (+ a 1)] (< i b) (inc! i)
    (for [j i] (and (> j a) (.Less data j (- j 1))) (dec! j)
      (.Swap data j (- j 1)))))

(go/func siftDown [^Interface data ^int lo ^int hi ^int first]
  (let [root lo]
    (while true
      (let [child (+ (* 2 root) 1)]
        (when (>= child hi)
          (break))

        (when (and (< (+ child 1) hi) (.Less data (+ first child) (+ first child 1)))
          (inc! child))

        (when (not (.Less data (+ first root) (+ first child)))
          (return))

        (.Swap data (+ first root) (+ first child))
        (set! root child)))))

(go/type IntSlice (slice int))

(go/method Len ^int [^IntSlice x] (len x))
(go/method Less ^bool [^IntSlice x ^int i ^int j] (< (aget x i) (aget x j)))
(go/method Swap [^IntSlice x ^int i ^int j]
  (set! (values (aget x i) (aget x j)) (values (aget x j) (aget x i))))

(go/func Ints [^{:tag (slice int)} x] (^{:inst [(slice int) int]} slices/Sort x))
```

- `for {}` is `(while true ...)`; `break` leaves it.
- `Ints` calls the generic `slices.Sort[S ~[]E, E cmp.Ordered]`; the inferred `S = []int`,
  `E = int` are `:inst` on `slices/Sort`. `Ints` has no results, so `(slices/Sort x)` is a
  statement, not a return.
- `x[i], x[j] = x[j], x[i]` is one assignment with two places.

### 14.4 Printing

The printer's output for `encodeRuneNonASCII`'s first clause, from the forms above:

```go
	case i <= rune2Max:
		_ = p[1]
		p[0] = t2 | byte(r>>6)
		p[1] = tx | byte(r)&maskx
		return 2
```

`(bit-or tx (bit-and (conv byte r) maskx))` needs no parentheses (`&` binds tighter than `|`);
`(bit-and (bit-or a b) c)` would print `(a | b) & c`. gofmt's spacing (`r>>6`, `byte(r)&maskx`)
is cosmetic; the printer computes it itself, by go/printer's rules, and does not run gofmt on
its output (§12.3; amendment, accepted 2026-10-08: the text said the spacing came from running
gofmt).

### 14.5 The amended forms

The amendments of 2026-10-08 in one file (`p.go`, checked with go1.27.1 and gofmt):

```go
package p

import "cmp"

//go:noinline

func Clamp[T cmp.Ordered](x, lo, hi T) T {
	return min(max(x, lo), hi)
}

func Count(s []byte, c byte) int {
	const (
		none = iota - 1
		one
	)
	n := none + one
retry:
	i := 0
	for i < len(s) && (s[i] == c || c == 0) {
		n++
		i++
	}
	if n < 0 {
		n = 0
		goto retry
	}
	return n
}
```

```clojure
(go/func ^:go/noinline Clamp :type-params [^cmp/Ordered T]
  ^T [^T x ^:go/grouped ^T lo ^:go/grouped ^T hi]
  (min (max x lo) hi))

(go/func Count ^int [^{:tag (slice byte)} s ^byte c]
  (let [^:const ^{:val -1} none (- iota 1)
        ^:const ^:go/grouped ^:go/implicit ^{:val 0} one (- iota 1)
        n (+ none one)]
    (let [^{:go/label :retry} i 0]
      (while (and (< i (len s)) ^:go/paren (or (== (aget s i) c) (== c 0)))
        (inc! n)
        (inc! i))
      (when (< n 0)
        (set! n 0)
        (goto :retry))
      n)))
```

- `//go:noinline` attaches to `Clamp` across the blank line, as gc attaches it (§9.1).
- `Clamp` is generic, so its parameter group `x, lo, hi T` is kept: `lo` and `hi` are declared
  with the name before them (§5.3). `Count`'s parameters are not in a generic declaration and
  carry no marker.
- The local `const` group keeps its grouping; `one` repeats `iota - 1` implicitly, written out
  and marked (§7.3). The group and `n := ...` share one `let`.
- `retry: i := 0` is a labeled declaration: it starts a `let` of its own, the label on its
  first target (§7.3).
- The parentheses around `s[i] == c || c == 0` are needed by precedence anyway; `^:go/paren`
  records that the source had them, which matters where they are not needed:
  `a && (b && c)` (§7.5).

## 15. Open questions

Each with the recommendation the spec follows. **Settled (2026-10-08):** the user accepted all
20 recommendations; they are part of the spec.

**Amendments (2026-10-08).** Implementing the helper, the printer and the converter raised 28
proposed amendments: seven by the helper's author (H1-H7, [HELPER-NOTES.md](HELPER-NOTES.md)),
ten by the printer (A1-A10, [PRINTER-NOTES.md](PRINTER-NOTES.md)) and eleven by the converter
(C1-C11, [CONVERTER-NOTES.md](CONVERTER-NOTES.md)), with the converter's corrections of the
spec's text and the documented choices of its notes that fix the spec. The user accepted all of
them on 2026-10-08. They are folded into the text above, marked "(amendment, accepted
2026-10-08)" where they apply:

- H1 `:dot` on type assertions too: §10.2, §11.3 (1), §13.1. H2 whether a `//go:` line
  attaches across blank lines: settled by C1, §9.1. H3 the type table with ids, local types
  and type parameters identified by `:decl`: §11.3 (4). H4 values narrower than their type,
  `:val` built from `:t`: §8.2, §11.1, §11.3 (5), §13.3. H5 `:go/via` from the helper's `:via`:
  §7.6, §11.1, §11.3 (6), §13.3. H6 the helper's file lists merged into `:other-files`: §4.2,
  §11.3 (7). H7 `p_test` as `go.<path>_test`: §4.1.
- A1 `^:go/grouped` for the field groups of generic declarations: §3.2 (N3), §5.3, §12.2,
  §13.1, §14.5. A2 `:go/breaks` indexes by `nth`, on every list: §10.1. A3 `/*line :N:1*/`
  and `//line :N:1`: §12.3. A4 conversion parentheses for `chan`, not `[`: §12.2. A5
  `(inst G T)` in `new`'s operand: §5.5, §7.7. A6 directives other than `//go:` kept
  verbatim: §9.1. A7 a `//` line between a doc comment and its directives: §9.1. A8 lines of
  lists inside metadata: §10.1. A9 labeled declarations: §7.1, §7.3 (as C9). A10 no gofmt over
  the printer's output: §12.3, §14.4.
- C1 directives attach across blank lines: §9.1. C2 local `const` groups kept
  (`^:go/grouped`, `^:go/implicit`): §3.2 (N6), §7.3, §12.2, §13.1, §14.5. C3 `^:go/paren` on
  operands of `and` and `or`: §3.2 (N1), §7.5, §11.1, §12.2, §13.1, §14.5. C4 directives among
  the imports: §4.3, §9.1, §12.2, §13.1. C5 a tagged `(go/id "x")` as a name: §4.4, §5.3,
  §13.1. C6 local generic types: §7.3, §13.1. C7 `x.go.clj` on file-name collisions: §4.1. C8
  `(binary-float M E)`: §8.2, §13.4, §13.5. C9 `:go/label` on the first target: §7.1, §7.3,
  §12.2, §13.1, §14.5. C10 `^:var (values a b)`: §7.1, §7.3, §13.1. C11 `:full` positions of
  spliced nodes: §10.2.
- The spec's own text, corrected: `1.0E100` is exact by §8.1's rule (§8.2); `runeErrorByte0`
  is an untyped rune, so its `:val` is a character (§14.2); `acceptRange`'s field comments are
  line comments, not `:doc` (§6.5, §14.2); §14.1's lines are illustrative (§14); the spacing of
  §14.4 is the printer's, not gofmt's.
- The converter's documented choices that fix the spec: single-file programs'
  namespaces (§4.1); the name of a package with test files only (§4.2); blank variables inside
  `(values ...)` in `:init-order` (§6.3); doc text with its final newline (§6.5); negative
  literals (§7.5); `cond` only for chains without init statements (§7.8); the float literal
  rule and character spellings (§8.1); `linkname` attached only when it names the next
  declaration, directives in groups, header and bodies, `//line` off column 1 (§9.1, §9.2);
  `in-ns` and `go/file` on the package clause's line, explicit `:line` on lists placed after
  their line, `:line` on `@` forms (§10.1); `/*line*/` inside expressions reported, not
  written (§10.3); where ordinary comments go (§10.4).

Where two amendments disagreed, the later and more specific won (the converter's over the
printer's over the helper's):

- H2 left open whether gc attaches a `//go:` line across a blank line; C1 verified that it does
  (`syntax/parser.go`, `clearPragma`/`takePragma`) and is the rule.
- A6 proposed attached metadata such as `:go/directives ["//export f"]`; the converter writes
  every directive other than a `//go:` line as a free-standing `(go/directive "...")` form
  where it stands, and that is the rule. Both keep the text verbatim.
- A9 proposed `^{:go/label :L}` on the binding; C9 fixes it on the first target of the `let`
  binding that holds the declaration, `var` declarations included, starting a `let` of its own.
- A1 proposed `^:go/grouped` "at least inside generic declarations"; the converter writes it
  only there, adds `^{:go/grouped false}` for names of equal type declared apart, and C2 reuses
  the marker for local `const` groups.
- H4 said to build `:val` from `:t`; the converter refines it for named types of other
  packages, whose underlying type is not in the dump: there the value's kind decides (§8.2).
- A5 asked for `(inst G T)` in `new`'s operand; the converter writes it in `method-expr`'s
  operand as well (§5.5).

1. **Positions.** Mode `:lines` carries Go lines in the layout of the forms text (no metadata
   on most forms; `:line` on line-starting symbols, `:go/breaks`, `:go/end`), and `:full`
   writes every position as metadata for the gate. The alternative is explicit position
   metadata on every form always, which makes the forms unreadable, or no positions at all,
   which changes line tables and `runtime.Caller`. *Recommendation: `:lines` by default,
   `:full` for the G2 gate; level 3 of §3.1 compares line tables only in `:full`.*
2. **Comments.** Doc comments as data (doc strings, `:doc`), directives as data, other comments
   as `;` text comments that reading drops. Alternatives: no comments at all, or every comment
   as data (`(go/comment ...)` forms between statements), which clutters the code forms.
   *Recommendation: as specified; doc comments on by default.*
3. **Mutability.** Every Go variable is assignable, so `^:mutable` (the survey's sketch, and
   the class forms' rule) is not written; captured variables are shared, as in Go.
   *Recommendation: no `^:mutable` in Go context.*
4. **Local declarations.** Nested `let` scoping over the rest of the block (the survey's choice,
   idiomatic Clojure), or flat Go-style declaration statements (`(var x e)` scoping to the end of
   the list), which keep indentation flat in long Go functions. *Recommendation: nested `let`,
   consecutive declarations merged into one `let`.*
5. **Assignment forms.** `(set! place op value)` for `op=`, `(inc! x)`/`(dec! x)`, and `aset` as
   the converter's spelling of `a[i] = v`. The alternative `(set! x (+ x v))` evaluates the
   place twice and changes gc's IR (and so inlining costs). *Recommendation: as specified.*
6. **Init statements** as a leading vector of `if`, `when`, `switch`, `type-switch` and `for`.
   Alternative: a `let` around the statement with a marker. *Recommendation: the vector.*
7. **Go's `for` as `for`.** `(for init cond post body*)` takes Clojure's `for` name for Go's
   three-clause loop inside Go context; `while` and `range` cover the rest. Alternatives: a new
   name (`for3`, `loop-for`), or `loop`/`recur` as j2c restructures Java, which loses the tree
   and the round trip. *Recommendation: `for` (Go context already gives Go meanings to names).*
8. **Implicit return.** The body's last expression is returned (as in `defn` and the class
   forms); `return a, b` stays explicit. *Recommendation: as specified.*
9. **`goto` and `fallthrough` verbatim.** The survey proposed restructuring `goto` (or
   `go/blocks`) and repeating arms for fall-through, which serve a JVM compiler (phase a, now
   skipped) and lose the round trip. *Recommendation: verbatim; a native compiler handles
   them as gc does.*
10. **Package variables in source order** with `:init-order` as data, instead of the survey's
    proposal to write variables in initialisation order (which reorders declarations and so
    gc's output). *Recommendation: source order plus `:init-order`.*
11. **Constants: source expression plus `:val`**, instead of the value with the expression as
    metadata (the survey's sketch). The expression keeps `iota`, implicit repetition and the
    readable `1 << 100`; `:val` spares consumers constant folding. *Recommendation: as
    specified.*
12. **Embedded paths as names** (`:go/via [Point]`) rather than go/types' indexes (`[0 0]`)
    or explicit `(.-X (.-Point n))` (which changes the tree). *Recommendation: names, on
    selections and promoted literal keys; implicit `&`/`*` of receivers derived.*
13. **Composite literal elements**: positional, `[key value]` and `:field value`, with `_` for
    elided types, instead of the survey's Clojure maps, which lose order and reject duplicate
    keys. *Recommendation: as specified.*
14. **Test files.** In-package `_test.go` files join their package's conversion (`:test-files`
    in `go/package`, loaded after `:files`); an external test package `p_test` is its own
    package, with the namespace `go.<path>_test` and `:package` in its `go/file`. The
    alternative is not converting tests, but the round trip over `$GOROOT/src` needs them.
    *Recommendation: as described, behind a converter option, on for the G2 gate.*
15. **Reserved heads and escapes.** Form names win over Go names in head position; calls of
    clashing Go names are `(go/call f ...)`, unreadable identifiers `(go/id "true")`, generic
    types named like type heads `(inst slice int)`. Alternatives: resolve heads by Go scope
    first (the printer would need name resolution), or a different literal head (`func`
    instead of `fn`, which no Go name can clash with since `func` is a keyword).
    *Recommendation: as specified, with `fn`; reconsider `func` if `(go/call fn ...)` proves
    frequent in converted code.*
16. **Renamings from the survey's sketch**, forced by the reader or by the round trip:
    `(tilde T)` for `~T` (`~` is unquote), the comma-ok receive by shape instead of `<!?`,
    `init` as an ordinary `go/func` instead of `go/init`, clause lists for `switch` and
    `select` instead of label/body pairs (Go's `default` may stand anywhere and clauses hold
    statement lists), `(byte-string ...)` for invalid UTF-8 instead of `go/bytes-str`, and Go's
    `/` and `%` instead of `quot` and `rem`. *Recommendation: accept them.*
17. **Expression types written by the converter.** Only non-constant shifts of untyped
    constants get a `:tag` now (§8.3); everything else is local or derived. The compiler (b2)
    may find more places where go/types reasons non-locally. *Recommendation: add them as
    amendments, as for the class forms.*
18. **Helper output.** Types as structured data, not `TypeString` strings; raw positions and the
    selector dot; paths for promoted literal keys (§11.3). *Recommendation: ask the helper for
    these now, before the converter is written.*
19. **Embedded files.** Recorded by path and hash and copied by the printer, not stored in the
    forms (the survey proposed resolving them into data). Data in the forms would make the
    forms self-contained but large (`unicode` tables are Go code, but `//go:embed` users embed
    whole trees). *Recommendation: path and hash for the round trip; revisit for the box (B1),
    where the forms are the source.*
20. **One tree per architecture.** `tamago/amd64` and `tamago/arm64` are two conversions with
    mostly identical forms (decision 7). *Recommendation: keep them separate; sharing is a
    later optimisation (a diff of the trees shows what differs).*

## 16. Sources

- go1.27.1, TamaGo's toolchain `/root/tamago-go` (`go version go1.27.1`, language version
  go1.27 of 2026-05-26):
  - `src/go/ast/ast.go` (node types and position fields), `src/go/ast/directive.go`;
  - `src/go/types/api.go` (`Info`: `Types`, `Instances`, `Defs`, `Uses`, `Implicits`,
    `Selections`, `Scopes`, `InitOrder`, `FileVersions`), `src/go/types/universe.go`
    (predeclared types and builtins, `new` of a value), `assignments.go` (redeclaration in
    short variable declarations is recorded in `Uses`);
  - `src/cmd/compile/internal/syntax/parser.go` (gc positions: a selector at its `.`, a call at
    its `(`, an operation at its operator; pragmas kept across blank lines, `takePragma` and
    `clearPragma`; line directives' file names, `updateBase`),
    `src/cmd/compile/internal/noder/noder.go` (pragma names);
  - `src/go/printer/nodes.go` (spacing and parentheses), `src/cmd/gofmt` (`stripParens`);
  - `src/cmd/compile/doc.go` (line directives, function directives);
  - `doc/go_spec.html` ("Language version go1.27": generic methods, function type inference in
    assignment contexts, selector keys in struct literals; Go 1.24 generic aliases).
- Directive counts: `grep` over `$GOROOT/src` outside `cmd` and `testdata` (`//go:linkname`
  1,342, `//go:nosplit` 1,232, `//go:cgo_import_dynamic` 1,673, `//go:fix` 312, `//go:embed`
  76 ...); calls of identifiers that are reserved heads (`fn` 169).
- Experiments in `.tmp/g2c/` (survey §10): `godump/main.go`, `sample/sample.go` and
  `sample-meta.edn`; `out/sort.edn` (`:inst` of `slices.Sort`); a probe package in
  `.tmp/g2c-spec/t/` (redeclared `err` in `Uses`; `1 << s` typed `uint8`; promoted literal key
  without path; `new(42)`; generic method `Apply` with inferred `F`; method path `[0 0]`).
- Arbace's reader (`arbace/lang/LispReader.clj`, `MetaReader`): explicit `:line`/`:column`
  metadata is kept over the reader's; `~` is unquote, `^` metadata, `.5` a symbol, `1e400`
  `##Inf`, and `|`, `%`, `&`, `&&`, `||`, `!=`, `<<`, `>>`, `...`, `<!`, `>!`, `inc!` symbols
  (checked with `bin/arbace`).
- go-lisp `/root/go-lisp` (`2483a496`): `golisp/SPEC.md` (invariants I1-I4, the node tables,
  directives, round-trip normalisations, end positions F10), `golisp/DESIGN.md` §4.1.
- The amendments of 2026-10-08: `doc/go/HELPER-NOTES.md`, `doc/go/PRINTER-NOTES.md`,
  `doc/go/CONVERTER-NOTES.md`, and the oracle's normalizations in `doc/go/ROUNDTRIP.md`.
- Arbace: `doc/classes/SPEC.md` (structure, principles, tags, labels, `switch`),
  `doc/G2C-SURVEY.md`, the journal entry "Decisions on g2c's open questions" (2026-10-07, on
  the branch `arbace-for-java-26`).
