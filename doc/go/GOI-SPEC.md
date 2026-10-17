# Go forms at the REPL: goi, the interpreter of Go forms

Status: draft for the user's review, 2026-10-17: milestone GR0 of Go forms at the REPL
([GO-FORMS-REPL-SURVEY.md](../GO-FORMS-REPL-SURVEY.md) §6.2), written at the user's decision of
2026-10-17. It is bound by the survey's proposals G1-G17, all decided as recommended on
2026-10-17; the choices made while writing it are S1-S55 (§13.1), the questions for the user
GS1-GS15 (§13.2), each with the recommendation this text follows. The experiments that validate
its key choices (Appendix A) were run with TamaGo's go1.27.1 (`/root/tamago-go`) on amd64, the
machine shared; their programs are in `.tmp/go-forms-spec/` (not tracked). GR1-GR6 (§12) come
after the coming re-seed.

**The name.** The survey called this document `doc/go/REPL-SPEC.md`. It is `GOI-SPEC.md`
instead: the REPL also holds Arbace forms and Java forms, which this text does not specify, and
what it does specify is one component with one name throughout the code, `goi` (the Go package
`arbace/goi` of decision G2 and the namespaces `arbace.go.*` that drive it), as
[J2G-SPEC.md](J2G-SPEC.md) is named after j2g.

This spec defines how **Go forms** ([SPEC.md](SPEC.md)) run at Arbace's REPL: the forms a user
types or loads, how a Go package becomes a namespace, how the forms are type-checked, how the
checked forms become an interpreter's nodes, how Go values are represented and cross into Arbace
and into the program's compiled Go, and how the interpreter is kept equal to gc. In builds Go
forms keep their compiled path (repr/go's printer, then gc: [BUILD.md](BUILD.md)); this is the
pair the Java forms already have (interpreted at the REPL, [CLASSFORMS-REPL.md](CLASSFORMS-REPL.md);
translated by j2g in builds). Names follow [NAMING-SURVEY.md](../NAMING-SURVEY.md): repr/go,
repr/java, repr/clojure and repr/haskell are bridges, c2j and j2g translators, build/go the build
of Go forms.

Words used throughout:

- **goi**: the interpreter as a whole. Its parts: the macros (`arbace.go`), the REPL's state
  (`arbace.go.repl`), the checker (`arbace.go.check`), the builder (`arbace.go.build`), the
  back end (the Go package `arbace/goi`), the table of compiled packages (`arbace/gotable`) and
  the Go API for Go forms (the Go package `arbace`).
- **the checker**: `arbace.go.check`, a Go type checker in Arbace (G3). Its input is Go forms
  and the declarations digest of the compiled packages; its output is **the checked tree**
  (§3.7).
- **the builder**: `arbace.go.build`, in Arbace. It turns the checked tree into **nodes**
  through the back end's natives (§4.1).
- **the back end**: the Go package `arbace/goi` (`go/arbace/goi.ae`, `go/arbace/goi/*.ae`),
  hand-written Go forms printed with the program as jrt is (G2): node factories, frames,
  threads, cells, type descriptors, proxies, the boundary conversions.
- **nodes**: typed Go closures (`func(*goi.Frame) int64` and its kin, §4.2).
- **the table**: the generated Go package `arbace/gotable` (`go/arbace/gotable/`, written at
  build time), which gives goi the program's compiled Go packages of a listed set (G5, §6).
- **the digest**: the declarations of the table's packages as Go forms without bodies, embedded
  in the program (§6.5), read by the checker as it reads any Go forms.
- **interpreted** and **compiled**: a Go package (function, type, value) is interpreted when goi
  runs its forms, compiled when gc compiled it into the program (the std packages, jrt, the
  table, `arbace`). A compiled package is reached only through the table.
- **RT(T)**: the `reflect.Type` that represents values of the Go type `T` in Go memory (§5.2).
- **the REPL mode** and **the program mode**: how goi treats `os.Exit` and uncaught panics
  (§2.7, §10.7): the REPL's process survives in the first; the second behaves as a program built
  by gc does, for `bin/goi run` and the check (§11).
- **gc**: Go's compiler as build/go drives it; its behaviour is the definition (§1.2, 4).

Contents: 1 Scope and principles · 2 The REPL surface · 3 The checker · 4 The builder and the
nodes · 5 Values · 6 The table of compiled packages · 7 Calls across · 8 Goroutines, channels,
select · 9 Generics · 10 Errors and traces · 11 The check · 12 Milestones · 13 Decisions and
open questions · 14 Sources · Appendix A: the experiments · Appendix B: a worked example.

## 1. Scope and principles

### 1.1 Scope

In scope: every construct of Go forms (SPEC.md §4-§8, go1.27.1, language version go1.27 and the
earlier versions a file's `:lang` names) for the configurations the program runs in (`linux` and
`tamago`, `amd64` and `arm64`, cgo off), typed or loaded at the REPL, checked, interpreted, and
crossing into Arbace, Java forms and the program's compiled Go. Generics from GR6 (§9).

Out of scope, as the survey's §2.8 has it: `unsafe` beyond `Sizeof`, `Alignof`, `Offsetof` (as
constants) and `unsafe.Pointer` conversions between pointer types and `uintptr` (§5.4); assembly
(a body-less `go/func ^:extern` is an error at the REPL unless the table provides the function),
cgo, `go:linkname` (an error naming it), `go:embed` (an error at GR1-GR6, GS14), the other `//go:`
directives (accepted and ignored: `nosplit`, `noinline` ... change no meaning); exact allocation
and escape behaviour; `runtime.Callers` and `runtime.Caller` with gc's program counters;
finalizer timing; Go's own deadlock detection (§8.5); `reflect` of interpreted named types
beyond what §5.2 gives (G8). Package tests (`:test-files`, `_test.go`) are not loaded by goi in
GR1-GR6 (S45).

### 1.2 Principles

1. **Forms in, forms all the way** (G1). goi reads Go forms as data, never Go text: no printing,
   no reparsing at the REPL. Positions come from the forms' metadata (SPEC.md §10). Forms typed
   by hand and forms converted by repr/go are the same language and take the same path.
2. **The checker is Arbace's and it is complete** (G3). Every form is type-checked before it
   runs, by `arbace.go.check`, which computes what the converter writes (`:val`, `:inst`,
   `:go/via`, `^:assign`, `:tag`, `:init-order`) and verifies the converter's annotations where
   they are present (§3.6). The builder never infers a type.
3. **Analysis in Arbace, execution in Go** (G2), the Java forms' split: the checker and the
   builder are Arbace namespaces compiled into the program by c2j; the nodes are Go closures of
   hand-written Go forms, printed with the program.
4. **gc is the definition; the check is the method** (G14, survey §4.3). Arbace does not own Go's
   compiler, so goi cannot be equal to gc by construction. Every program of the corpus is run by
   goi and by gc from the same forms, and their outputs compared (§11). A difference is a bug of
   goi or an entry of the known file with its reason.
5. **Go values are Go values** (G4). Values that compiled code may see are real Go values of the
   right type; scalars live in typed slots; goi's descriptors give what `reflect` cannot make
   (named types, methods, interfaces: §5).
6. **The REPL's meaning is late binding** (G9). A package is a namespace, its declarations vars;
   calls go through cells, so a redefinition is seen by every later call. Builds keep Go's static
   binding. Nothing else in Go's meaning changes at the REPL, except where §2.7 and §10.7 say
   (process exit, uncaught panics in REPL mode).
7. **Go context stays closed** (SPEC.md principle 1, G11). No Clojure code runs inside Go context;
   Arbace and Go forms meet at declarations (vars), values and calls, never inside one body.

### 1.3 The parts and where they live

| part | where | language | in the program | milestone |
|---|---|---|---|---|
| `arbace.go`: the macros `go/package`, `go/file`, `go/type`, `go/const`, `go/var`, `go/func`, `go/method`, `go/directive`, and the REPL's `go/import`, `go/eval`, `go/run-main`, `go/run-compiled` (§2) | `arbace/go.ae` | Arbace | embedded, compiled by c2j | GR1 (the table's namespaces), GR3 |
| `arbace.go.repl`: packages, files, declarations, generations, the load hook (§2) | `arbace/go/repl.ae` | Arbace | compiled | GR3 |
| `arbace.go.check`: the checker (§3) | `arbace/go/check.ae`, `arbace/go/check/*.ae` | Arbace | compiled | GR2 |
| `arbace.go.build`: the builder (§4.1) | `arbace/go/build.ae` | Arbace | compiled | GR3 |
| `arbace.lang.GoInterp`: the natives the builder and the REPL call; `GoFn`, `GoVar`, `GoType`, `GoPanic`, `GoExit` (§4.11, §5.5, §10.4) | `arbace/lang/GoInterp.ae` | Java forms | translated by j2g | GR1 |
| `arbace/goi`: frames, threads, nodes, cells, descriptors, proxies, conversions, the natives' Go side (§4, §5) | `go/arbace/goi.ae`, `go/arbace/goi/*.ae` | Go forms, hand-written | printed with jrt | GR1 (conversions), GR3 |
| `arbace/gotable`: the table (§6) | `target/arbace-go/gotable/` (generated, not tracked) | Go forms, generated | printed with the program | GR1 |
| the digest (§6.5) | the program's resource `arbace/go/digest.zz` | Go forms (data), generated | embedded, zlib (SA21) | GR1 |
| `arbace`: Go forms' API to Arbace (§7.3) | `go/arbace.ae`, `go/arbace/api.ae` | Go forms, hand-written | printed, in the table | GR5 |
| `arbace.go.table`: the table's generator (§6.3) | `arbace/go/table.ae` | Arbace | on the tools' host | GR1 |
| jrt's `GoValue` rule (§5.5), the trace hook (§10.6), the goroutine's goi state (§4.3) | `go/arbace/jrt/object.ae`, `throwable.ae`, `thread.ae` | Go forms | printed | GR1, GR5 |
| `bin/goi`: `run`, `check`, `annotations`, `table` (§11) | `bin/goi`, `test/goi/` | bash, Arbace | tools | GR1-GR3 |

The import graph of the Go packages: `arbace/lang` imports `arbace/goi` (the natives of
`GoInterp`) and `arbace/gotable`; `arbace/gotable` imports `arbace/goi` and the listed
packages; `arbace/goi` imports `arbace/jrt` and std, never `arbace/lang` (it calls Arbace through
interfaces it declares itself, which `lang`'s types satisfy structurally: §7.3); the package
`arbace` imports `arbace/lang` and `arbace/jrt` and is listed in the table. So there is no cycle.

### 1.4 A first session

```clojure
user=> (ns go.demo (:require [arbace.go :as go]))
nil
go.demo=> (go/import [strings "strings"] [fmt "fmt"])
nil
go.demo=> (go/func Greet ^string [^string name] (+ "hello, " (strings/ToUpper name)))
#'go.demo/Greet
go.demo=> (Greet "go")                              ; Arbace calls the Go func: a var, an IFn
"hello, GO"
go.demo=> (go/type Point (struct ^int X ^int Y))
#'go.demo/Point
go.demo=> (go/method String ^string [^Point p] (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))
#'go.demo/Point.String
go.demo=> (go/eval (fmt/Println (lit Point 1 2)) (lit Point :X 3))
(1,2)
#go "demo.Point{X:3, Y:0}"
go.demo=> (go/func Div ^int [^int a ^int b] (/ a b))
#'go.demo/Div
go.demo=> (Div 1 0)
Execution error (GoPanic) at go.demo/Div (REPL:7).
runtime error: integer divide by zero
go.demo=> (go/func Greet ^string [^string name] (+ "hi, " name))   ; redefined: callers see it
#'go.demo/Greet
```

`fmt.Println` calls the interpreted `String` method through a proxy (§5.3); the REPL prints a Go
value by its `%#v`, named as gc names it (§5.7); the panic leaves Go code as a `GoPanic` with gc's
message (§10.4).

## 2. The REPL surface

### 2.1 Packages are namespaces

A Go package is the namespace SPEC.md §4.1 names (`go.` and the import path, `/` to `.`, `.` and
`-` inside an element to `_`). Its package-level declarations are vars in it (G9):

| declaration | the var's root | Arbace sees |
|---|---|---|
| `go/func F` | an `arbace.lang.GoFn` (an `AFn`): the function's cell (§4.6) and its signature | `(go.x/F args...)`, arguments and results converted (§5.6) |
| `go/method M` on `T` | a `GoFn` of the method expression `T.M` (receiver first), named `T.M` (`(*T).M` for a pointer receiver is the var `T.M` too: one var per method name, §2.5) | `(go.x/T.M recv args...)` |
| `go/var V` | an `arbace.lang.GoVar`: the variable's cell and type, `IDeref` | `@go.x/V` reads it, `(go/assign! go.x/V v)` writes it, converted |
| `go/const C` | the constant's value converted (§5.6; untyped integers exact as `Long` or `BigInt`) | `go.x/C` |
| `go/type T` | an `arbace.lang.GoType`: the descriptor (§5.2), an `IFn` | `(go.x/T v)` converts (`T(v)`), `(go.x/T {:X 1})` makes a struct value (§7.1) |

The var's metadata: `:go/decl` (the declaration's object, for the checker), `:go/kind`, `:doc`
(the doc string, SPEC.md §6.5), `:file` and `:line` (the forms'), `:arglists` (the Go parameter
names). `doc`, `meta`, `ns-publics` and `find-var` work as on any var. Unexported Go names are
vars too, marked `:private` (Go's export rule is capitalization; Arbace's `:private` follows it).
The blank `_` and `init` declare no var (`init` functions run at initialization, §2.6).

A compiled package of the table has a namespace too, made from the table when it is required
(§6.1): `(require '[go.strings :as strings])`, then `(strings/ToUpper "abc")` (GR1). Its vars
are made on first resolution (a namespace of every listed member is not interned eagerly: S9).

**The namespace's kind is fixed when it is made** (S1): a namespace made from the table is
compiled, one made by `go/package` (or by the first declaration in a `go.*` namespace) is
interpreted. An interpreted package may not take the path of a listed compiled package (an
error naming the path), except the generic std packages that the table lists as interpretable
(§9.3). `(remove-ns ...)` of an interpreted package forgets it; a later `require` loads it again.

**The package of a namespace** is created by `go/package` (in files: SPEC.md §4.2) or, at the
REPL, implicitly by the first Go declaration in a namespace whose name starts with `go.` (S2): its
name the namespace's last segment, its path the segments after `go.` joined by `/`, its
`:config` the program's own (`:goos`, `:goarch`, `:lang` the toolchain's `go1.27`). In a
namespace whose name does not start with `go.`, a Go declaration is an error ("Go declarations
belong in a go.* namespace"); `go/eval` works anywhere (§2.4) and runs in the implicit package
`go.user` (S3).

### 2.2 `arbace.go`'s macros

`arbace.go` is an ordinary namespace, loaded by `(:require [arbace.go :as go])` (the converted
forms' own `ns` form, SPEC.md §4.1) or on the first Go form in a `go.*` namespace without the
alias: the compiler, meeting an unresolvable `go/` head in a namespace named `go.*`, requires
`arbace.go` and aliases it `go` in that namespace before it analyzes the form (S4, as
`Compiler.classForms` loads `arbace.classes` on the first Java form). It is compiled into the
program (embedded as `arbace.classes` is), so loading it costs no evaluation.

Every declaration head of SPEC.md is a macro. Each expands to one call of `arbace.go.repl`
with the form quoted, so the forms are data from the reader to the checker and nothing in them
is evaluated as Arbace (principle 7):

```clojure
(go/func Greet ^string [^string name] ...)
;; expands to
(arbace.go.repl/declare! (quote (go/func Greet ^string [^string name] ...)) {:ns go.demo :file ... :line 3})
```

| macro | at the REPL | in a package being loaded (§2.3) |
|---|---|---|
| `go/package` | creates or updates the namespace's package (`:path`, `:config`, `:lang`; `:files` noted) | starts the load (§2.3) |
| `go/file` | makes the named file the namespace's current file, its imports replaced by `:imports` (S5) | starts the file: its imports, `:build` evaluated as gc does (a file excluded by its constraints declares nothing), `:lang` |
| `go/type`, `go/const`, `go/var`, `go/func`, `go/method` | checks and defines at once (§2.5); returns the var (the first of a group) | queued, checked when the package completes |
| `go/directive` | accepted, ignored but for the out-of-scope ones (§1.1) | the same |
| `go/import` (the REPL's) | adds import specs `[name "path"]` (SPEC.md §4.3) to the current file | an error: a file's imports are its `go/file`'s |
| `go/eval` | §2.4 | an error |
| `go/run-main`, `go/run-compiled`, `go/check`, `go/assign!`, `go/str`, `go/zero` | functions, §2.6, §7.1 | |

The current file at the REPL is the one the last `go/file` named, else the implicit file
`repl.go` (S5). A file's imports are file-scoped (SPEC.md §4.3): a declaration resolves `pkg/X`
through the imports of the file it was declared in, so changing the current file's imports
later re-resolves nothing already defined.

### 2.3 Loading files

`(require 'go.bench)` and `(load "go/bench")` load the package file of SPEC.md §4.1 from
`ARBACE_PATH` like any namespace: its `ns`, its `go/package` and one `load` per Go file, each
file starting with `in-ns` and `go/file`. Go checks a package as a whole (its declarations refer to
each other across files, in any order), so during the load the declaration macros only queue
their forms, and the package is checked, built and initialized when the load of its package
file ends (S6):

- `go/package` registers the package as loading, with its `:files` (and the path of the package
  file);
- each declaration of a file of a loading package is queued with its file;
- `arbace.core/load` calls the hooks of `arbace.lang.RT`'s after-load list when the load of a
  resource ends, normally or not; `arbace.go.repl`'s hook, given the package file's path,
  completes the package: checks it (§3), defines its vars, builds its initializers and runs
  the package's initialization (§2.6). A check error ends the load with a `CompilerException` that
  lists every error (§3.8), and the package's vars are not defined. This hook is the one change
  goi makes to the vendored core (recorded in doc/VENDOR-NOTES.md when built).

Imports of a package are loaded first, recursively, as Go requires (an import cycle is an
error, "import cycle not allowed", with the cycle). An import path resolves:

1. to a compiled package when the table lists it (§6.6);
2. to an interpreted package already loaded;
3. else to a namespace loaded from `ARBACE_PATH` by the path's namespace (`go/x/y.ae`), the
   program's module rules of SPEC.md §4.1 (B1) and local paths (`:local-imports`, D1) applied;
4. else an error, "package P is not in the table and has no Go forms on ARBACE_PATH".

### 2.4 `go/eval`

`(go/eval stmt ...)` runs Go statements in the current Go namespace's package (in a namespace not
named `go.*`, in `go.user`), with the current file's imports. The statements are the body of an
implicit function without parameters, checked and run at once; the value of the last statement,
when it is an expression with a value, is the call's value, converted to Arbace (§5.6); several
values are a vector; none is `nil` (S7). It is Go context: `(go/eval (let [x 1] (+ x 2)))` is
`3`. Locals of one `go/eval` do not survive it (a later `go/eval` does not see them; package-level
state is `go/var`'s: GS3). A `go/eval` of a single symbol or call needs no special form:
`(go/eval Pi)`, `(go/eval (strings/Repeat "ab" 3))`.

`go/eval` takes values from Arbace only through declarations (vars of the package): there is no
syntax for an Arbace local inside Go context (principle 7).

### 2.5 Redefinition and generations

At the REPL each declaration form defines its names at once (outside a package load):

1. The form is checked against the package as it stands (its other declarations, imports,
   digest). An error leaves everything as it was.
2. **Functions and methods**: the declaration's cell gets the new code; every later call sees it
   (calls go through cells, §4.6; G9). A method redefined on a type changes the type's method set
   in place: the type keeps its generation (S8).
3. **Variables**: a `go/var` defined again with the same type keeps its Go variable (its cell, so
   pointers taken to it still alias it) and runs the new initializer; with another type it is a
   new variable (S10).
4. **Constants**: a new value.
5. **Types**: a `go/type` defined again with an identical underlying type and the same type
   parameters only replaces its doc; otherwise it starts a **new generation** (G9; as Java forms'
   classes, classes/SPEC.md §10): a new descriptor and a new RT (§5.2), so values of the old
   generation keep the old type, are not of the new one, and are printed with their generation
   (`demo.Point#1{X:1, Y:2}`, §5.7). Methods are not carried over: `go/method` forms define them
   for the new generation.
6. **Dependents.** The declarations of the package that refer to a redefined object (the
   checker records the references of each declaration, §3.7) are checked again in dependency
   order. Those that still check are rebuilt (their nodes made again on their next call, §4.1);
   those that no longer check keep their old definition and are reported as a warning that
   names them and their errors (G9), until they are defined again. Interpreted packages that
   import the package are dependents too, through its exported objects.
7. The var of a declaration is the same var across redefinitions (`#'go.demo/Greet`); its root
   is replaced (a `GoFn` holds the cell, so Arbace code that captured the fn sees the new code;
   a `GoType` is replaced by the new generation's).

A redefinition inside a package being loaded is Go's: two declarations of one name in a package
are an error ("Greet redeclared in this block").

### 2.6 Initialization, `main`

A package is initialized once, when its definition completes (a load, §2.3) or at its first REPL
declaration: package variables in `:init-order` (the converter's, verified; computed by the checker
for hand-typed forms with go/types' algorithm, §3.4), then each file's `init` functions in file
order and source order (SPEC.md §6.3). At the REPL a `go/var` typed later is initialized when it is
defined, and an `init` typed at the REPL runs at once (S11).

`(go/run-main 'go.bench)` runs a main package's `main` in program mode semantics for exit (§2.7)
but in the REPL's process and its stdout: it returns `nil`, or throws `GoExit` with the status when
`os.Exit` was called. `(go.bench/main)` calls `main` as any function (REPL mode).

### 2.7 The two modes

| | REPL mode (the default) | program mode (`bin/goi run`, the check, `go/run-main`) |
|---|---|---|
| `os.Exit(n)` in interpreted code | throws `arbace.lang.GoExit` (status `n`) out of the goi call, through Go frames as a panic that `recover` does not stop (deferred calls run, as Goexit's) (S12) | in `bin/goi run`: the process exits with `n` without running deferred calls, as gc; in `go/run-main`: `GoExit` |
| an uncaught panic in the calling goroutine | crosses into Arbace (§10.4) | gc's report on stderr (§10.7), status 2 |
| an uncaught panic in another goroutine | gc's report on stderr; that goroutine ends; the process lives (S13) | gc's report, status 2 (the process ends) |
| `main` returning | `go/run-main` returns; goroutines still running keep running | `bin/goi run` exits 0 at once, as gc does (goroutines are not waited for) |

`os.Exit` and the other members whose meaning goi must own are the table's overrides (§6.7).

### 2.8 Summary of the commands

| command | effect |
|---|---|
| `(go/import [n "p"] ...)` | imports for the current REPL file |
| `(go/eval stmt ...)` | runs Go statements, returns their value |
| `(go/run-main 'go.x)` | runs package `go.x`'s `main` |
| `(go/run-compiled 'go.x & args)` | prints `go.x`'s program with repr/go, builds it with build/go and runs it as a child process (in the box a spawned guest), returns `{:out :err :exit}` (G15, GR5) |
| `(go/check 'go.x)` | checks the package again, returns the errors (data) without defining anything |
| `(go/assign! var v)` | assigns a package variable from Arbace |
| `(go/zero T)`, `(go/str v)` | the zero value of a Go type (a `GoType` or a type form), `fmt.Sprint(v)` as a String |
| `bin/goi run ROOT NS [args]` | runs an interpreted program from a forms root in program mode (`arbace -m arbace.go.run`) |

## 3. The checker

### 3.1 Role and inputs

`arbace.go.check` type-checks Go forms as `go/types` checks Go: one package at a time, all its
files, against the declarations of its imports. Its inputs:

- the package's forms (the `go/package` options, each file's `go/file` and declarations), read
  with `:line` metadata (SPEC.md §10.1), as data;
- for each import: an interpreted package's checked declarations, or a compiled package's digest
  (§6.5), which is Go forms too and is read by the same code;
- the configuration (`:goos`, `:goarch`, the file's `:lang`) and gc's sizes for `GOARCH`
  (`types.SizesFor("gc", GOARCH)`: word 8, max alignment 8 on amd64 and arm64).

At the REPL it checks one declaration (or `go/eval`'s body) against the package's checked state;
in a load, the package whole. It runs on the tools' host too (`bin/goi annotations`, §11.5), with
no Go toolchain: a Go forms checker usable beyond the REPL (a linter, j2g's output checked, the box,
survey §5 (a)).

### 3.2 Its relation to go/types

The checker follows **the Go specification as go/types implements it in go1.27.1**
(`src/go/types`), and where the specification leaves a choice open (error wording, which of two
errors is reported, the precision of huge untyped floats, the order of initialization of
independent variables), go/types' choice. Its structure follows go/types' where that serves
agreement: objects and scopes (`object.go`, `scope.go`), the package's declarations collected
first and checked lazily by dependency with go/types' colouring (`decl.go`, `objDecl`), constant
arithmetic as go/constant's, initialization order as `initorder.go`, method sets and lookup as
`lookup.go`, `methodset.go`, type inference as `infer.go` and `unify.go` (GR6). It is written anew in
Arbace (S14), go/types its model and its judge: everything the converter copies from go/types is a
test of the checker (§3.6, §11.5).

Size: 5,000-8,000 lines of Arbace (survey §2.1): scopes, objects, types, assignability,
conversions, constants, method sets, embedding, interfaces, statements and their rules, about
4,000-5,500; generics 1,500-2,500.

### 3.3 Objects, scopes, types

The checker's data model is normative for the checked tree (§3.7) and the builder; its
representation inside the checker is the checker's.

- **Objects** are maps with identity (records, compared by identity): `{:kind :var|:const|
  :type|:func|:pkg|:label|:builtin|:nil, :name, :type, :pkg, :decl {:file :line}, ...}`, a
  `:var` with `:local?`, `:param?`, `:result?`, `:field?`, `:embedded?`, and, computed for the
  builder, `:captured?` (referenced from a function literal nested in its scope),
  `:addressed?` (operand of `addr`, receiver of a pointer method through implicit `&`, sliced
  when an array) and `:per-iteration?` (a loop variable of a file at `:lang` ≥ go1.22).
- **Scopes**: the universe, the package scope (all files), one file scope per file (its imports,
  dot imports), and the blocks the forms nest (SPEC.md §7.3: a `let` scopes over its body, which is
  the rest of the Go block). Resolution is SPEC.md §4.4's.
- **Types**: basic types are keywords (`:bool`, `:int8` ... `:uint64`, `:int`, `:uint`,
  `:uintptr`, `:float32`, `:float64`, `:complex64`, `:complex128`, `:string`,
  `:unsafe-pointer`; untyped kinds `:untyped-bool`, `:untyped-int`, `:untyped-rune`,
  `:untyped-float`, `:untyped-complex`, `:untyped-string`, `:untyped-nil`; `byte` and `rune` are
  `:uint8` and `:int32`, their spelling kept for messages). Composite types are values compared by
  structure (`=`): `[:pointer T]`, `[:slice T]`, `[:array n T]`, `[:map K V]`, `[:chan dir T]`,
  `[:func params results variadic?]`, `[:struct fields]` (each field its name, type, tag,
  embedded flag and, for an unexported name, its package), `[:interface methods embeddeds]`,
  `[:tuple ...]`, `[:union terms]`. **Named types**, aliases (materialized, Go 1.24) and type
  parameters are objects with identity: a named type has its type name, its underlying type, its
  methods, its generation, and for an instance its origin and type arguments. So `=` on types is
  Go's identity: structural for unnamed types, by identity for named ones.

### 3.4 What it checks

Everything the Go specification requires a compiler to reject, as go/types rejects it, for the
constructs of SPEC.md §4-§8. By area, with the go/types file each follows:

| area | rules | go/types |
|---|---|---|
| declarations | redeclaration, unused imports (an error, as gc), unused variables (an error, as gc), the blank identifier, `init` (no references, no signature), `main` in package `main`, labels (defined, used, no jumps into blocks or over declarations: SPEC.md §7.8) | `decl.go`, `resolver.go`, `labels.go`, `stmt.go` |
| types | valid recursive types (`invalid recursive type`), array lengths (constant, non-negative, representable as `int`), map keys comparable, embedded fields (a type name or pointer to a non-interface type name), struct field and method name clashes, interface embedding, method declarations (receiver base type declared in the package, not a pointer or interface) | `typexpr.go`, `struct.go`, `interface.go`, `signature.go`, `validtype.go` |
| constants | exact arithmetic (§3.5), representability (`cannot use 300 (untyped int constant) as int8 value in assignment (overflows)`), division by zero, shifts (counts non-negative, untyped shifts in non-constant context, SPEC.md §8.3), `iota`, implicit repetition, `len`/`cap` constants of arrays, `unsafe.Sizeof`/`Alignof`/`Offsetof` with gc's sizes | `const.go`, `expr.go`, `builtins.go`, `sizes.go` (gc's `gcSizes`) |
| expressions | operand modes (value, variable, constant, type, builtin, no value, comma-ok, map index), untyped conversion to the context's type, operators per type, comparison (comparable, ordered, `nil` against the six kinds), conversions (the specification's rules, constant conversions), selectors with embedded depth search and ambiguity (`ambiguous selector`), implicit `&`/`*` of receivers, method values and expressions, index and slice (bounds of constants, three-index slices on strings rejected), composite literals (keys, duplicate keys and indexes as gc rejects them, elided types), function literals, type assertions (impossible ones rejected), builtins (all of SPEC.md §7.7, `min`, `max`, `clear`, `new` of a value, Go 1.26) | `expr.go`, `call.go`, `selection.go`, `lookup.go`, `index.go`, `conversions.go`, `builtins.go`, `literals.go` |
| assignability | identical types, identical underlying types with one unnamed, interfaces implemented (method sets, pointer receivers), channels by direction, `nil`, untyped constants representable | `operand.go` `assignableTo`, `lookup.go` `implements`, `missingMethod` |
| statements | assignments (`:=` with at least one new name: the targets the converter marks `^:assign` computed), `op=`, `++`, `--` on addressable or map index operands, `return` (count and types; named results shadowed: "result parameter x not in scope at return"), missing return (terminating statements), `switch` (duplicate constant cases as gc), `type-switch` (clause variables, duplicate types, impossible cases, unused clause variable), `select` (communication clauses), `for` and `range` (each range kind, the language version's: integers from go1.22, functions from go1.23), `go` and `defer` operands (calls, not conversions or some builtins), `break`/`continue`/`goto`/`fallthrough` placement | `stmt.go`, `assignments.go`, `return.go`, `range.go` |
| initialization | the order of package variables (go/types' `initorder.go`: dependencies through functions and methods, ties by declaration order), initialization cycles (`initialization cycle or refers to itself`) | `initorder.go` |
| language versions | features by the file's `:lang` (`go1.22` loop variables and integer ranges, `go1.23` range over functions, `go1.24` generic aliases, `go1.26` `new` of values, `go1.27` generic methods, inference in assignment contexts, selector keys in literals) | `version.go` |
| generics (GR6) | §9.1 | |

The checker never lets the builder meet an unchecked form: what it does not support yet (by
milestone, §12) is an error of its own, "not supported by goi yet: X (GRn)", so the check's
corpus can tell an unsupported program from a failing one (§11.3).

### 3.5 Constants

Constant values are Arbace's exact numbers, with go/constant's semantics (S15):

- integers: `Long`, or `BigInt` beyond 64 bits; no limit but go/constant's (an untyped integer
  of more than 512 bits after an operation is an error, "constant overflow", go/types' `overflow`
  with `prec = 512`);
- untyped floats: exact ratios (`Ratio`, `BigInt`, `BigDecimal` normalized to ratios) while
  numerator and denominator stay within go/constant's limit (`maxExp = 4 << 10` bits); beyond it
  a binary float of 512 bits of precision, written in `:val` as `(binary-float M E)`
  (SPEC.md §8.2), as go/constant switches from `big.Rat` to `big.Float`;
- typed floats rounded to `float32` or `float64` at every conversion to the type (go/types'
  `roundFloat32`, `roundFloat64`); complex as pairs;
- strings as UTF-8 byte strings (an Arbace string or `(byte-string ...)`, SPEC.md §8.1); booleans.

The value is `:val`'s representation of SPEC.md §8.2, chosen by the constant's type (H4): what
the checker computes for a constant name is directly comparable with the converter's `:val`.

### 3.6 The converter's annotations and their verification

Converted forms carry go/types' decisions (SPEC.md §11.1). The checker computes every one of them
itself and, where the forms carry it, compares (G3):

| annotation | the checker computes | on disagreement |
|---|---|---|
| `:val` on a constant name | the constant's value at its type (§3.5) | an error |
| `:tag` on an expression | the expression's type (SPEC.md §8.3's non-constant shifts; any expression the converter tags) | an error |
| `:inst` | the inferred type arguments (GR6) | an error |
| `:go/via` | the embedded path of the selection or literal key | an error |
| `^:assign` on a `:=` target | that the name is declared in the same block | an error |
| `(conv T x)` against a call | that `T` is a type | an error (SPEC.md §4.4 classifies by head; a `conv` of a non-type or a call of a type is an error) |
| `:init-order` | the order of package initialization | an error |
| `:lang` of a file | used, not computed (it comes from the file's build line) | |

An error here is "annotation A on X disagrees with goi's checker: forms say V, the checker says W"
(S16): either the checker is wrong or the forms are stale, and both are bugs to fix, never to
ignore. The annotations are never used in place of the checker's own answer: a hand-typed form
without them and a converted one with them check alike. The verification costs a comparison
per annotation and doubles as the checker's widest test (§11.5).

### 3.7 The checked tree

The checker's output, the builder's input: a tree of maps, one per form, each with `:op`, the
form's `:line` (and `:file` on declarations), and for expressions `:type` (§3.3) and `:mode`
(`:value`, `:variable` (addressable), `:mapindex`, `:constant` with `:val`, `:commaok`,
`:novalue`, `:type`, `:builtin`). Everything the forms leave implicit is explicit here:

- every **implicit conversion** is a node: `{:op :convert-iface :to T :x e}` (a value assigned,
  passed, returned or compared as an interface), `{:op :convert-const ...}` folded into the
  constant's value at its final type (an untyped constant meets its context's type: SPEC.md
  §8.3);
- every **selection** is resolved: `{:op :field :path [i j k] :derefs [...] :x e}` with the
  embedded path as field indexes and the implicit dereferences; a method call
  `{:op :call :callee {:op :method :method m :recv e :recv-adjust :addr|:deref|nil}}` with the
  method object (interpreted, compiled, or an interface method);
- every **call** says its kind: `:static` (a package function of an interpreted package: its
  cell), `:compiled` (a table member, with its invoker key, §6.4), `:method`, `:iface` (an
  interface method), `:closure` (a function value), `:builtin`, and its arguments with `:spread`;
- **places** of assignments: `{:op :place-local|:place-global|:place-field|:place-index|
  :place-map|:place-deref ...}`;
- **statements** as SPEC.md §7's forms resolved: `:block`, `:decl` (new locals with their objects),
  `:assign`, `:op-assign`, `:incdec`, `:expr`, `:send`, `:go`, `:defer`, `:return` (with the named
  results), `:if`, `:switch`, `:type-switch` (each clause's variable object and types), `:select`,
  `:for`, `:range` (`:over :int|:array|:ptr-array|:slice|:string|:map|:chan|:func`, the key and
  value objects or places), `:label`, `:break`, `:continue`, `:goto` (their targets resolved to
  the statement nodes), `:fallthrough`, `:local-type`;
- **declarations**: `{:op :func :obj f :params [...] :results [...] :body ... :refs #{...}}`,
  methods with `:recv`, `{:op :var-init :lhs [...] :rhs e}` in initialization order, types with
  their descriptors' data (fields, embedded flags, tags, methods, generation), constants with
  their values. `:refs` is the set of package-level objects the declaration refers to, for
  redefinition (§2.5).

Adding keys is not a change of contract; changing a key's meaning is. GR2's tests fix the shape
for the builder (`test/goi/checked/*.edn`, golden trees of small programs).

### 3.8 Errors

An error is reported as go/types words it (S17), with the forms' position:
`FILE:LINE: MESSAGE`, `FILE` the forms file (`go/bench/main.ae`) for files, `REPL` for typed
forms, `LINE` the form's `:line`; columns are not kept in mode `:lines` (SPEC.md §10.1) and not
reported. A package's errors are all reported (up to go/types' default of 10 per package, then
"too many errors"), each as gc's `cmd/compile` (types2) words it where it differs from go/types,
since gc's wording is the one users see (`declared and not used: x`). At the REPL they are thrown as one `CompilerException` whose message
lists them and whose `ex-data` holds them as data (`{:go/errors [{:file :line :msg :code}]}`,
`:code` go/types' `errors.Code` name, `UnusedVar`, so tools need not parse messages).

### 3.9 Configuration and language versions

The checker evaluates `go/file`'s `:build` lines against the package's configuration as gc does
(build tags of `:goos`, `:goarch`, the architecture level, `:tags`, `go1.N` release tags), so a
file of the wrong configuration declares nothing; `:lang` of a file (or the package's) gates
features (§3.4) and decides loop variables' scope. The configuration of the REPL's implicit
packages is the program's (§2.1).

## 4. The builder and the nodes

### 4.1 The builder

`arbace.go.build` walks the checked tree (§3.7) and makes nodes by calling the back end's
factories through the natives of `arbace.lang.GoInterp` (§4.11), as `arbace.classes.interp`
makes `Compiler$CF` nodes from the Java forms' analysis. It:

- numbers each function's slots per kind (§4.3), the parameters and results first;
- decides which variables are **cells** (§4.3): the checker's `:captured?` and `:addressed?`, every
  variable of a struct or array type, and every package variable;
- assigns each function's jump codes (§4.5) and lines;
- chooses each node's typed variant by the operands' types (an `int8` `+` is `AddI8`, a `string`
  `+` is `ConcatS`, an `==` on an interface type compares dynamic types: §4.4);
- resolves calls: a static call to the callee's cell, a compiled call to the table's member and
  its typed invoker (§6.4), a method to the method's cell or the table's method expression;
- builds a function's body **on its first call** (S18), as the Java forms' interpreter builds a
  method's tree: `goi.Func` holds the checked declaration until then (through an `IFn` the
  builder gives it), so loading a package checks everything and builds only what runs. A
  redefinition drops the built body (§2.5). `bin/goi check` builds every body eagerly, so the
  builder's own errors show (§11).

The builder makes no decision the checker has not made: no type, no constant, no resolution.

### 4.2 The nodes

Nodes are typed Go closures over one frame (G2, survey §2.7). Their Go types, in `arbace/goi`:

```clojure
(go/type I (func [(* Frame)] [int64]))      ; bool, int8 ... int64, uint8 ... uint64, int, uint, uintptr
(go/type F (func [(* Frame)] [float64]))    ; float32, float64
(go/type S (func [(* Frame)] [string]))     ; string
(go/type R (func [(* Frame)] [any]))        ; every other type: §5.1
(go/type B (func [(* Frame)] [bool]))       ; conditions (an I of type bool, made a test)
(go/type P (func [(* Frame)] [unsafe/Pointer])) ; addresses: a cell, a field, an element (places)
(go/type T (func [(* Frame)]))              ; a call of several results, written to the frame's result slots
(go/type Stmt (func [(* Frame)] [Ctl]))     ; a statement
(go/type Ctl int32)                         ; 0 next, 1 return, 2 + k the jump code k (§4.5)
```

- **Integers are normalized.** An `I` of a signed type of width w holds the value sign-extended
  from w bits, of an unsigned type zero-extended, `uint64` and `uint` and `uintptr` its bits; a
  `bool` 0 or 1. Every operation of a narrow type normalizes its result (`int8` `+` is
  `int64(int8(a+b))`), as CF keeps Java's `int` in `long`. Division and remainder by zero panic
  as Go does (the node performs Go's operation at the type: `int8(a) / int8(b)`, so gc's
  semantics, `MinInt8 / -1` included, come from gc itself).
- **Floats**: an `F` of `float32` holds the value rounded to `float32` after every operation.
- `R` nodes return the representations of §5.1; `P` nodes the address of a place, from which
  typed loads and stores are made (§4.4).

The node set is closed and listed in `go/arbace/goi/nodes.ae` with one factory per kind and type
variant, about 250 factories (estimate), generated in part from a table of operators and types
by `test/goi/gen_nodes.ae` (as `CompilerOps.ae` is generated: S19), written by hand for control
flow, calls, defer and select.

### 4.3 Frames, slots, threads, cells

```clojure
(go/type Frame (struct ^{:tag (slice int64)} I ^{:tag (slice float64)} F ^{:tag (slice string)} S
                       ^{:tag (slice any)} R ^{:tag (slice unsafe/Pointer)} C
                       ^{:tag (* Thread)} Th ^{:tag (* Func)} Fn ^int32 Line ^{:tag (* Frame)} Caller
                       ^{:tag (slice deferred)} defers ^{:tag (* panicking)} rec))
```

- **Slots** are numbered per kind by the builder: `I` for the I-types, `F`, `S`, `R` for values,
  `C` for cells. A variable that is not a cell is a slot of its kind; its value is the slot.
- **Cells** are addressable Go variables of the variable's own Go type, allocated with
  `reflect.New(RT(T))` (or a typed `new` for the basic types): a variable whose address is taken,
  a variable captured by a function literal, every struct or array variable (their parts are
  assigned in place), every package variable. The frame holds the cell's address in a `C` slot;
  reads and writes are typed loads and stores through it (§4.4). So `(addr x)` is a real `*T` that
  compiled code can use (X1: `fmt.Sscan` into a cell), and captured variables are shared by
  reference, as in Go.
- **Per-iteration loop variables** (files at go1.22 or later): a loop variable that is a cell gets a
  fresh cell each iteration, initialized from the previous one before the post statement, as the
  specification says; one that is not a cell needs nothing (S20).
- **Threads.** A `goi.Thread` is goi's state of one goroutine: a stack per slot kind, from which
  each call's frame takes a window (the slots of one call), and `Top`, the innermost interpreted
  frame. A call pushes the callee's windows and pops them when it returns normally; a panic
  leaves them (and `Top`), so the trace of a panic is still there when it is recovered or reported
  (§10.6). Taking slots from the thread's stack instead of allocating each frame halves the cost of
  a call (X2: `fib 22` 2.87 ms against 4.95 ms; gc 0.23 ms). A stack that must grow takes a new
  array; older frames keep their windows of the old one (S21).
- **Whose thread.** A goroutine's thread is found through jrt's goroutine-local slot: jrt's
  `Thread` gains one field for goi's state (`Interp any`), set on the goroutine's first
  interpreted call (S22). So compiled code calling back into interpreted code (a `sort.Slice` less)
  continues the same thread: one stack, one trace.
- **Depth.** A thread counts its interpreted calls; at goi's limit (`ARBACE_GOI_DEPTH`, default
  100,000) a call panics with gc's fatal text as a value (`runtime: goroutine stack exceeds
  1000000000-byte limit`; in program mode reported as gc's `fatal error: stack overflow` with
  status 2), since Go's own stack overflow is fatal and would end the REPL (S23).

### 4.4 Expressions

| form | nodes |
|---|---|
| literals, constants | `ConstI`, `ConstF`, `ConstS`, `ConstR` (the value made once at build time, at its final type) |
| locals, parameters | `LocalI` ... `LocalR` (a slot); a cell: `Load<k>` of `CellP` (§4.3) |
| package variables | `Load<k>` of the variable's cell (fixed address) |
| `(+ a b)` and the other operators (SPEC.md §7.5) | per operator and type: `AddI8` ... `AddU64`, `AddF32`, `AddF64`, `ConcatS`, `AddC`; n-ary chains folded left at build time into binary nodes |
| comparisons | `EqI`, `LtI` ..., `EqS`, `EqF` (Go's float `==`: NaN unequal), `EqR` for the other comparable types (pointers, channels, interfaces with Go's dynamic rule, structs and arrays by their Go `==` on the real values: a runtime panic for an uncomparable dynamic type, as gc) |
| `and`, `or`, `not` | `AndB`, `OrB` (short-circuit), `NotB` |
| shifts | per type, Go's semantics (count ≥ width gives 0 or the sign; a negative count panics: `runtime error: negative shift amount`) |
| `conv` | per source and target type: integer widths (truncation and extension), integer to float and back (Go's conversions at the types), `string(rune)`, `string([]byte)`, `[]byte(s)`, `[]rune(s)`, `string([]rune)`, slice to array and array pointer (with gc's panic), between named types of one underlying type (no-op or RT change, §5.2), to interfaces (§5.3), `unsafe.Pointer` to and from pointers and `uintptr` |
| `(.-f x)` | `FieldAddr` (the base's address plus the field's offset, the embedded path folded into one offset when no pointer is crossed, a load per pointer crossed with gc's nil check), then a typed load; method values make a bound closure (§4.6) |
| `(aget x i)` | arrays and slices: the element's address with gc's bounds check (`runtime error: index out of range [5] with length 3`), then a typed load; strings: `IndexS` (a byte); maps: `MapGet` (§5.4), comma-ok `MapGet2` |
| `(subslice x lo hi max)` | per base kind, gc's checks (`slice bounds out of range [:5] with capacity 3`) |
| `@p`, `(addr x)` | `Deref` (gc's nil check: `runtime error: invalid memory address or nil pointer dereference`), `Addr` of a cell or place |
| `(lit T ...)` | a new value of RT(T) built in place (structs, arrays, slices, maps; `&T{}` a new cell) |
| `(assert T x)` | §5.3; comma-ok as `T` (two results) |
| `(<! ch)` | §8.2 |
| `(fn ...)` | `MakeClosure`: the literal's code and the captured cells (§4.6) |
| builtins | `len`, `cap`, `append` (`reflect.Append` or typed fast paths for the common element types), `copy`, `delete`, `clear`, `close`, `make`, `new`, `complex`, `real`, `imag`, `min`, `max` (Go's NaN and signed-zero rules), `panic`, `recover` (§4.7), `print`, `println` (gc's runtime formatting, to stderr: `+1.000000e+000` for floats, S24) |

**Loads and stores** through a `P` are typed by the place's Go type: `*(*int8)(p)` for an `int8`,
`*(*string)(p)`, `*(*any)(p)` for an interface type whose RT is `any` (empty interfaces and every
interpreted interface type, §5.2), and for other types, a compiled non-empty interface (`error`,
`io.Reader`: its memory holds an itab, not a type) among them, `reflect.NewAt(RT(T), p).Elem()`'s
typed copy (`Set`), never an untyped copy, so Go's garbage collector's write barriers apply to
every pointer stored (S25). A field read from a struct value
held in an `R` (a box, §5.1) reads through the box's address and copies nothing. Reflect's field
access is not used for fields: it costs 7.8 ns against 0.6 ns for an offset (X2), and it refuses
unexported fields (X1).

### 4.5 Statements and control flow

A statement node returns a `Ctl`: 0 to go on, 1 for a `return` (the results are in their slots),
2 + k for the jump code k. The builder gives each jump target of a function its codes: a loop its
break and continue codes, a `switch`, `type-switch` or `select` its break code, a label its goto
code. No Go panic is used for control flow (as CF, CLASSFORMS-REPL.md §2.2).

- **Statement lists** (bodies, branches, `do`): `Seq` runs its statements, returning the first
  non-zero code that is not one of its own labels' goto codes; a goto code of one of its labels
  resumes the list at that label (S26). This is all `goto` needs: Go forbids jumps into blocks and
  over declarations in scope at the label, and with `let` scoping over the rest of its block a
  legal `goto` is a jump to a label of the same list or of an enclosing one (SPEC.md §7.8). A
  backward `goto` is a loop.
- **`let`** evaluates its initializers into the new variables' slots or cells, in Go's order
  (all right-hand sides first, then assignment), then runs its body list.
- **`if`, `when`, `cond`**: `If` with a `B` test.
- **`switch`**: tag switches over constant cases of an `I` or `S` tag are a Go `switch` on a map
  from value to clause index when the cases are many (≥ 8) and constant, else tests in source
  order (Go's order of evaluation of non-constant cases); `(fallthrough)` returns the
  fallthrough code, which the switch node turns into running the next clause.
- **`type-switch`**: §5.3's dynamic type of the guard, compared with each case's type in source
  order (an inline cache per node keyed by the dynamic type), the clause's variable bound in a new
  slot.
- **Loops**: `While`, `For` (init, test, post, body; the per-iteration copy of §4.3), `Range` per
  kind (§3.7's `:over`): an integer range counts at the operand's type; arrays and slices by
  index over the operand evaluated once (Go's semantics: an array is copied, a slice's header
  kept); strings by `utf8.DecodeRuneInString`; maps by `reflect.Value.MapRange` (Go's random
  order, Go's rules for entries added or deleted during the loop); channels by receive until
  closed; functions (go1.23) by §4.10.
- **Assignments**: `(set! (values a b) (values b a))` evaluates every right-hand side and every
  index and pointer operand of the places first, then stores left to right (SPEC.md §7.4); a map
  place stores with `SetMapIndex`; `(set! x op v)` evaluates the place's operands once.
- **`return`**: stores the results in their slots (named results are slots of the frame) and
  returns 1. The implicit return of a body's last expression (SPEC.md §6.4) is a `return`.

### 4.6 Calls and cells

```clojure
(go/type Func (struct ^string Name ^string File ^{:tag (* Code)} code ^{:tag (* Type)} Sig ...))
(go/type Code (struct ^int32 NI ^int32 NF ^int32 NS ^int32 NR ^int32 NC
                      ^{:tag (slice slotRef)} Params ^{:tag (slice slotRef)} Results
                      ^bool HasDefer ^Stmt Body ...))
(go/type Closure (struct ^{:tag (* Func)} F ^{:tag (slice unsafe/Pointer)} Cells))
```

- **A package function is a cell**: its `*Func`, whose `code` the REPL replaces on redefinition
  (G9). A static call node holds the `*Func` and reads `code` at every call, so later calls see a
  redefinition (one load: S27). Built lazily on the first call (§4.1).
- **A call** evaluates the callee (for a function value), then the arguments in the caller's
  frame, in Go's order, takes the callee's windows from the thread, writes the arguments into
  the parameter slots (or new cells), runs the body, reads the result: a single-result call node
  is typed by its result (`CallI`, `CallF`, `CallS`, `CallR`); a call of several results is a `T`
  writing them into the caller's result slots for the assignment that follows (S28).
- **Function values** in a frame are `*Closure` values (an interpreted function or literal and its
  captured cells: the literal's cells are put in its frame's `C` slots at entry), real compiled
  Go funcs, or `nil`. Stored into Go memory (a struct field, a slice element, an interface, an
  argument of compiled code) a closure becomes a real Go func of its type: a typed adapter of the
  table for a common signature (§6.4: 1.6 ns of overhead per call against `MakeFunc`'s 319 ns,
  X2), else `reflect.MakeFunc`; the adapter is made once per closure and recorded, so loading the
  func from Go memory gives the closure back (goi keeps a weak map from the func's address to its
  closure) (S29).
- **Method calls** on a static type: the method's `*Func` (interpreted) with the receiver adjusted
  (`&x`, `*p` as the checker resolved: §3.7), or the table's method expression (compiled). On an
  interface: §5.3. Method values (`(.-M x)`) are closures binding the receiver, evaluated when the
  method value is made, as Go does.
- **Compiled calls** go through the table's typed invoker for the signature, else
  `reflect.Value.Call` (§6.4).
- **`go/call`** (SPEC.md §4.4) is an ordinary call.

### 4.7 `defer`, `panic`, `recover`

- **Panics are Go panics** (G12): `(panic v)` is Go's `panic(v)` with the converted value; runtime
  errors come from the Go operations themselves where goi performs them (an `int` division, a
  slice index), and are made with gc's messages where goi checks first (bounds, nil, conversions
  to arrays, closed channels: §10.3).
- **`(defer call)`** evaluates the callee and the arguments at once and pushes a record on the
  frame's defer list (SPEC.md §7.9). A function whose body contains a `defer` (the checker marks
  it) runs its body inside a Go function with one Go `defer` of goi's `unwind` (S30); a function
  without one pays nothing (a Go defer costs 5 ns per call, X2).
- **`unwind`**, the deferred Go function, calls Go's `recover()` itself (the one place Go allows
  it), which gives the panic in progress, or `nil` for a normal return or `runtime.Goexit`, and
  records the interpreted trace if it is the panic's first recovery (§10.6). It then runs the
  records last in, first out, each guarded by a Go `defer` of its own, so that a record that panics
  replaces the panic in progress (and the rest still run), as in Go. While a panic is in progress,
  the record's call gets the panic in its frame's `rec`.
- **`(recover)`** returns the panic's value and stops the panic when its own frame's `rec` is set
  (the frame is the deferred function's own: Go's "called directly by a deferred function"), the
  panic not already recovered; else `nil`. After the records, `unwind` returns normally when the
  panic was recovered (the function then returns its result slots' values, as Go does), else
  panics again with the same value; the chain of values replaced (`panic: A [recovered]`,
  `panic: B`) is kept for the report (§10.7).
- A deferred call of a compiled function or of a builtin (`(defer (close ch))`, `(defer (.Unlock
  mu))`) is called as it is; `(defer (recover))` recovers nothing, as in Go.

### 4.8 Labels and `goto`

`(label :L stmt)` gives `stmt` its codes: a labeled loop, `switch`, `type-switch` or `select` the
break code of `(break :L)` and, for a loop, the continue code of `(continue :L)`; any label a goto
code. `(goto :L)` returns `L`'s goto code; the statement list that holds `L` (§4.5) resumes at it.
A labeled declaration (`^{:go/label :L}` on a `let` target, SPEC.md §7.3) labels the `let`'s
place in its list.

### 4.9 `select`

`(select clause ...)` (SPEC.md §7.8) evaluates every channel operand and every value to send in
source order, then:

- with at most two communication clauses on channels whose Go types are of a fast-path list
  (`chan int`, `chan bool`, `chan string`, `chan struct{}`, `chan any`, `chan error`, their
  directional types), a Go `select` on the asserted channels (S31);
- otherwise `reflect.Select` over the cases (X2: 202 ns for two cases, against 43 ns for a typed
  send and receive);
- the chosen clause's receive result is bound (comma-ok as two results), and its body runs; a
  `default` clause makes it non-blocking. A `select` with no clauses blocks forever (§8.5).

### 4.10 `range` over functions

`(range [k v f] body)` with `f` a function of type `func(yield func(K, V) bool)` (and its one- and
zero-argument forms) calls `f` with a yield function made by goi: a typed adapter of the yield type
(a real Go func, since compiled iterators such as `maps.Keys` call it) that runs the body **in the
enclosing frame** (so `defer` in the body belongs to the enclosing function, as Go specifies) with
`k` and `v` bound, and returns `true` to continue, `false` for a `break`. A `return`, an outer
`break` or `continue`, or a `goto` out of the body is recorded in the range node and the yield
returns `false`; when `f` returns, the range node performs it. A yield called again after it
returned `false` panics with gc's message (`runtime error: range function continued iteration
after function for loop body returned false`); a panic in the body propagates through `f`.

### 4.11 The natives: `arbace.lang.GoInterp`

The builder and the REPL reach the back end through one class of Java forms, `arbace.lang.GoInterp`,
whose methods are `^:static ^:native`. j2g translates each to a call of a Go function of
`arbace/goi` (`goi.GoInterp_<name>..._native`), an extension of J2G-SPEC §9.1's rule for
natives: a native of `arbace.lang.GoInterp` is `goi`'s, as one jrt defines is jrt's (S32; an
amendment of J2G-SPEC §9.1 when built). They take and return what goi can name: primitives,
`String` (`*jrt.String`), `Object` (`any`) and `Object[]`:

| native | does |
|---|---|
| `node(String kind, Object[] args) Object` | makes a node; `args` are nodes, descriptors, codes, slots (as `Long`) and constants; the node is an opaque `Object` (a Go value, `GoValue` to Arbace) |
| `func(String name, String file, Object sig, IFn builder) Object` | a `*Func`, its body built by `builder` on the first call |
| `setCode(Object fn, Object code)`, `dropCode(Object fn)` | (re)defines a function's code, forgets it |
| `defineType(Object[] spec) Object`, `setMethods(Object type, Object[] methods)` | descriptors (§5.2) |
| `newVar(Object type) Object` | a package variable's cell |
| `call(Object fn, Object[] args) Object` | Arbace calling Go (§7.1): conversions, the thread, the boundary for panics (§10.4) |
| `toArbace(Object v, Object type)`, `fromArbace(Object v, Object type)` | §5.6 |
| `member(String path, String name) Object`, `digest() byte[]` | the table (§6.1), the digest (§6.5) |
| `typeOf(Object v) Object`, `goString(Object v) String`, `equals(Object a, Object b) boolean`, `hash(Object v) int` | `GoValue`'s protocol (§5.5) |

Each crossing of the natives from Arbace costs a translated call and a conversion; the builder
calls them once per node, at build time, and the hot paths (a call of a Go func from Arbace)
take one native call each.

## 5. Values

### 5.1 Representations

Every Go type has one representation in a frame (its slot kind, §4.3) and one in Go memory, its
RT (§5.2). The second is normative: whatever compiled code sees is a real Go value of RT(T). The
first is the back end's and may be refined at GR3 by measurement (S33), provided converting
between them is exact and keeps aliasing (a slice keeps its array, a pointer its pointee).

| Go type | in a frame | in Go memory | to Arbace (§5.6) | from Arbace, for a parameter of the type (§5.6) |
|---|---|---|---|---|
| `bool` | `I` 0/1 | `bool` | `Boolean` | `Boolean` only |
| `int8` ... `int64`, `int`, `uint8` ... `uint32`, `uintptr` | `I`, normalized (§4.2) | the type | `Long` | `Long`, `Integer`, `Short`, `Byte`, `BigInt` and `Character` (a code point) in range; out of range an error, never a wrap |
| `uint64`, `uint` | `I`, the bits | the type | `Long` when below 2^63, else `BigInt` | `Long` ≥ 0, `BigInt` in range |
| `float32`, `float64` | `F` (`float32` rounded) | the type | `Float`, `Double` | `Double`, `Float`, `Long` and `BigInt` when exact |
| `complex64`, `complex128` | `R` (`complex128`) | the type | a Go value | a Go value |
| `string` | `S` | `string` | `String` (UTF-8 decoded, copied; invalid bytes as U+FFFD) | `String` (UTF-16 encoded as UTF-8; an unpaired surrogate as U+FFFD, as Go's conversion of invalid UTF-16) |
| pointers | `R` (the pointer) | `*T` | a Go value | the same Go value |
| slices, maps, channels | `R` (the real value) | `SliceOf`, `MapOf`, `ChanOf` | a Go value (aliasing kept) | the same Go value; a Java primitive array for a slice of its element type, copied (S34) |
| arrays, structs | a cell (variables, §4.3) or an `R` box (values in transit: a copy) | `ArrayOf`, `StructOf` (§5.2) | a Go value (a copy) | the same Go value; a map of keywords to field values for a struct, converted per field (§7.1) |
| functions | `R`: a `*Closure`, a compiled func or `nil` (§4.6) | `FuncOf` (adapters, §4.6) | a `GoFn` (an `IFn`) around the func and its type, unwrapped when it goes back to Go (S35) | a `GoFn`; any other `IFn`, adapted (§7.5); a Go func value |
| interfaces | `R`: the content (§5.3) | `any`, or the compiled interface type | by the content: `nil`, a Java object itself, a Go scalar or string converted as its type, else a Go value | `nil`; a Java object as itself; Arbace's `Long`, `Double`, `Boolean`, `String`, `Character` as Go's `int`, `float64`, `bool`, `string`, `rune` for `any` (G10); for another interface, a value implementing it |
| `unsafe.Pointer` | `R` | the type | a Go value | a Go value |

"A Go value" is the value itself handed to Arbace as an `any` that is not a Java object, which
jrt gives the class `arbace.lang.GoValue` (§5.5). Go values keep their identity in Arbace and go
back to Go unchanged.

### 5.2 Types at run time: descriptors and RT

Every type goi meets has a descriptor, `goi.Type` (kind, name, package, generation, RT, the
underlying type, elements, fields with their offsets, parameters, methods). The descriptor is
what interpreted code uses for everything `reflect` cannot represent; RT is what Go memory holds.

- **Basic types and compiled types**: RT is the real type (`reflect.TypeFor[int]()`, the
  table's `reflect.Type` of `time.Duration`), its descriptor made from RT and the digest on first
  use.
- **Unnamed composite types**: RT is made by `reflect`'s constructors from the components' RTs,
  which intern them (X1: `StructOf` twice gives one type).
- **Struct fields** in RT have their Go names; an unexported name carries the package's path as
  its `PkgPath` (X1: `StructOf` takes them, `%+v` prints the names, `encoding/json` leaves them
  out as for gc's types); an embedded field is an ordinary field of the type's name, since
  `StructOf` refuses embedded fields with methods (X1: "embedded type with methods not
  implemented if there is more than one field"); promotion is goi's (the checker's paths). gc
  prints the same (`%+v` shows `{Buffer:... X:0}` either way); `reflect`'s `Anonymous` differs
  (a known difference: S36).
- **Named struct types** (and their generations): RT is `StructOf` of the fields with the first
  field's tag extended by `goi:"<path>.<Name>#<generation>"` (S37). Go's type identity counts tags,
  so every named struct type and generation has an RT of its own, while conversion between
  struct types ignores tags as Go's does (X1: `P#1`, `Q#1` and `P#2` three types, `P#1`
  convertible to `Q#1`; `%v` `{1 2}`, `%+v` `{X:1 Y:2}`, `json` `{"X":1,"Y":2}` unchanged). `%T`
  and `reflect.Type.String` show the tag (`struct { X int "goi:\"main.P#1\""; Y int }` for gc's
  `main.P`): a known difference (G8). A named empty struct has no field to tag: its RT is
  `struct {}`.
- **Other named types** (`type Celsius float64`, `type IDs []int`): RT is the underlying type's,
  since `reflect` cannot make a named type (golang/go#16522).
- **Interface types** of interpreted packages: RT is `any` (reflect cannot make interface types,
  golang/go#4146); compiled interface types keep theirs.
- **Faithful types.** A type is faithful when its RT belongs to it alone: basic and compiled types,
  named struct types, and unnamed composites of faithful types. A named type whose RT is its
  underlying type's, a named empty struct, and composites containing such a type (`[]Celsius`,
  whose RT is `[]float64`) are not. goi keeps a registry from RT to descriptor for every faithful
  interpreted type.

### 5.3 Interface values and proxies

An interface value is its content, an `any`, in a frame and in Go memory alike. For a dynamic type
D, the content is:

1. D compiled, or interpreted, faithful and without methods: **the real value** of RT(D).
2. Otherwise (D has methods, or D is not faithful): a **proxy**, a struct value
   `goi.Proxy{T *Type; V any}`: D's descriptor and the real value of RT(D) (S38). A proxy is
   compared by Go's `==` field by field, so two proxies of equal values are equal and work as
   map keys, and an uncomparable `V` panics as Go does (X1); `goi.Proxy` implements
   `fmt.Formatter` with fmt's own rules over D's interpreted methods (`Format`, then `GoString`
   for `%#v`, then `Error`, then `String` for `%v %s %q %x %X`), else formats `V` with the same
   verb and flags, so `fmt.Println` of an interpreted `Stringer` prints its `String()` (X1).
3. In Go memory whose static type is a **compiled interface** I other than `any` (a field of type
   `error`, an `io.Reader` argument), a proxy is of I's **wrapper type**: a type of the table that
   embeds `goi.Proxy` and has I's methods, each calling D's interpreted method (G8; X1:
   `io.ReadAll` over a wrapper of `io.Reader`). The table generates one wrapper per listed
   interface (§6.4); storing a D in a compiled interface I without a wrapper is an error at
   check time ("goi cannot give compiled code an interpreted T as io.WriterTo: the interface has
   no wrapper in the table"). A value of a faithful methodless D needs none (rule 1), and an
   empty method set makes every interface's wrapper trivial.

Inside interpreted code:

- the dynamic type of a content is D for a proxy, else the registry's descriptor of the value's
  reflect type (§5.2), else the compiled type's;
- a type assertion or type switch compares descriptors (an inline cache per node); an assertion
  to an interface type checks D's method set against it (cached per pair);
- a method call on an interface value calls D's interpreted method on `V` for a proxy, else the
  table's method of the compiled dynamic type, found by its reflect type and the method's name in
  the table (an inline cache per call node); a compiled type whose methods the table does not hold
  is an error naming it ("goi cannot call String on *x.T: the type is not in the table"). goi never
  looks methods up by name through `reflect` (`MethodByName` with a name not constant keeps every
  exported method of every reachable type alive in the linked program, X3: +1.1 MB for 35 small
  packages; on the program's jrt, 47,445 exported methods: S39);
- `==` on interface values compares dynamic types (descriptors) and values; for proxies the two
  descriptors and `V`s, whatever wrapper holds them.

What compiled code sees of an interpreted value through an interface (known differences, G8):
`%T` and `reflect` show RT or `goi.Proxy` and its wrappers; an assertion to a compiled
interface succeeds only for the interface the value was stored as and for `fmt.Formatter`; `==`
between a wrapper and a plain proxy of one value is false; encoders that walk a proxy
(`encoding/json` of an interpreted struct type that has methods, passed as `any`) see the
proxy's fields. The runtime overlay that would make named types with method sets (ixgo's
`reflectx` route, survey §2.2) is the later decision G8 left open (GS8).

### 5.4 Maps, channels, functions, strings in detail

- **Maps** are real Go maps of RT(K) to RT(V): `reflect.Value`'s `MapIndex`, `SetMapIndex`,
  `MapRange`, with typed fast paths for the maps whose Go type is a fixed one (`map[string]int`,
  `map[int]int`, `map[string]string`, `map[string]any`, `map[any]any`, `map[string]bool`,
  `map[int]bool`, by assertion: S40). Keys of interface type hold contents (§5.3).
- **Channels** are real Go channels of RT(T) (§8.2).
- **Strings** are Go strings; `(aget s i)` is a byte, `range` decodes runes, conversions as Go's.
- **`unsafe.Pointer`** values convert to and from pointers and `uintptr` as Go allows; pointer
  arithmetic through `uintptr` is out of scope (§1.1), `unsafe.Add`, `Slice`, `String`,
  `SliceData`, `StringData` are in (they are typed).

### 5.5 Go values in Arbace: `arbace.lang.GoValue` (G10)

A Go value in Arbace is an `any` whose dynamic type is not one of jrt's `Object_I` types. jrt's
`Object` protocol (`jrt.GetClass`, `ToString`, `Equals`, `HashCode`, J2G-SPEC §5.8) fails on such
a value today (`notObject`: "not a Java object"). The rule (GR1):

- jrt holds two hooks, set at initialization: `jrt.GoValueClass` (the class
  `arbace.lang.GoValue`, a final class of Java forms with no constructor, made by `arbace/lang`)
  and `jrt.GoValueOps` (`String`, `Equals`, `Hash`, made by `arbace/goi`, which imports jrt).
  `notObject` returns through them instead of throwing when they are set; `nil` stays
  `NullPointerException`.
- **class**: `GoValue`; `(instance? arbace.lang.GoValue v)` is true of every non-nil Go value.
  `(type v)` is `GoValue`; `(go/type-of v)` gives the Go type's descriptor (a `GoType`).
- **`toString`**: the value's `%#v`, as gc prints it, with goi's descriptor names for interpreted
  types (`demo.Point{X:3, Y:0}`, §5.7).
- **`equals`**: Go's `==` when the type is comparable (structs, arrays, pointers, channels,
  interfaces' contents); identity otherwise (slices: the same array, length and capacity; maps
  and funcs: the same map or func). A panic of Go's `==` (an uncomparable dynamic type in a
  struct) is `false`.
- **`hashCode`**: consistent with `equals`: `hash/maphash.Comparable` with one seed per process
  for comparable values, the address for the others (S41).
- **Printing** at the REPL: `print-method` of `GoValue` prints `#go "<toString>"` (S42); not
  readable back (no data reader).

### 5.6 The boundary conversions

The conversions of §5.1's last two columns are made **by the static Go type** at the boundary:
a parameter's or result's declared type (Arbace → Go func, Go func → Arbace), a variable's
(`go/assign!`, `@var`), a constant's. For an interface type the content's dynamic type decides
(§5.1). They are goi's `toArbace` and `fromArbace` (§4.11), one per type kind, and they never
leak a raw Go scalar into Arbace: an `int` reaching Arbace is always a `Long`.

- **Named types.** Scalars and strings convert by their kind whatever their type
  (`Celsius(36.6)` is `36.6`). A value of a named composite type that is not faithful (§5.2:
  `type IDs []int`) goes to Arbace as its proxy, so it prints as its type and comes back as it;
  a faithful one (a named struct) goes as itself, its type found by its RT.
- **Several results** are a vector, in order; no result is `nil`.
- **Errors** are values: a function returning `(T, error)` returns `[v err]` to Arbace (`err`
  `nil` or a Go value); nothing throws (S43).
- **Out of range** arguments throw `IllegalArgumentException` naming the parameter, the value
  and the type (`argument n of go.x/f: 300 overflows int8`); a wrong kind throws
  `ClassCastException` (`argument p of go.x/f: expected []int, got arbace.lang.PersistentVector`).
- **Variadic** parameters take the trailing Arbace arguments, each converted to the element type;
  a last argument `(go/spread xs)`, `xs` a Go value of the slice type, is passed as the slice
  (`(go.x/f a (go/spread xs))`, Go's `f(a, xs...)`).

### 5.7 Printing Go values

goi's `goString(v)` is fmt's `%#v` of `v` where `v`'s types are compiled, and for interpreted
types the same format with the descriptor's qualified name (`pkgname.Name`, as gc's `%#v` names
types by package name) and, for a value whose generation is not its type's newest, `#n` after
the name (`demo.Point#1{X:1, Y:2}`). gc's `%v`, `%+v` and `%s` of an interpreted value printed by
interpreted code (`fmt.Println(p)`) are exact (X1); `%T` of an interpreted value shows its RT or
its proxy's type (a known difference, §5.3) unless goi formats `%T` itself (GS9).

## 6. The table of compiled packages

### 6.1 What it holds

For every listed package (§6.6), generated at build time (G5):

- **members**: each exported function (not generic) as its Go func value, each exported variable
  as its address, each exported named type (not generic) as its `reflect.Type`, each with its
  invoker key (§6.4); constants are not in the table (the digest has their `:val`, which the
  checker uses as any constant: their values are folded at build time);
- **methods**: for each listed type, its exported methods and those of its pointer type as method
  expressions (`(method-expr (* bytes/Buffer) WriteString)`, a func with the receiver first), each
  with its invoker key, and for each listed interface its methods' expressions
  (`(method-expr io/Reader Read)`); the table so names exactly what interpreted code may call, and
  the linker keeps nothing else (§5.3);
- **wrappers** of the listed interfaces (§5.3, §6.4);
- **typed invokers and adapters** (§6.4);
- the package's **digest** (§6.5), embedded apart.

At run time `goi.Lookup(path)` gives a package's table (registered by its Go `init`), and
`require` of the package's namespace (§2.1) makes vars from it on demand.

### 6.2 Its format

The table is Go forms, one file per listed package plus the invokers' files, in the generated
package `arbace/gotable` (`target/arbace-go/gotable/`, printed with the program as j2g's output
is; not tracked):

```clojure
;; gotable/strings.ae (generated): one package's table
(in-ns 'go.arbace.gotable)
(go/file "strings.go" :imports [[goi "arbace/goi"] [reflect "reflect"] [strings "strings"]])

(go/func init []
  (goi/Register "strings"
    (lit goi/Package
      :Funcs (lit (slice goi/Member)
               (lit _ "Compare" strings/Compare inv_SS_I)
               (lit _ "ToUpper" strings/ToUpper inv_S_S)
               ...)
      :Types (lit (slice goi/TypeMember)
               (lit _ "Builder" (reflect/TypeFor (strings/Builder)) ...)
               ...)
      :Methods (lit (slice goi/Member)
                 (lit _ "(*Builder).WriteString" (method-expr (* strings/Builder) WriteString) inv_PBuilder_S_IE)
                 ...))))
```

(`reflect.TypeFor[strings.Builder]()` is written `((inst reflect/TypeFor strings/Builder))` in
real Go forms; shortened here.)

### 6.3 Generation

`arbace.go.table` runs on the tools' host as part of the program's build (`bin/arbace-go --build`,
`--build-tests`, `--box`), after j2g and before build/go, for the build's configuration
(`linux` or `tamago`, the architecture):

1. it reads the list, `go/arbace/gotable.edn` (committed: packages, interfaces to wrap,
   overrides, the interpretable generic packages: §6.6);
2. it reads the forms of the listed packages for the configuration: `tamago/goroot/`'s for
   `tamago` (committed), `bin/repr-go read` of the listed packages for `linux` (converted at
   build time, cached by the toolchain's and the list's hashes under `.tmp/goi-table/`; S44), and
   `go/`'s for the program's own `arbace` package;
3. it checks their declarations with the checker's declaration layer (type expressions,
   signatures, method sets, constants' values: §12, GR1) and writes the table (§6.2) and the digest
   (§6.5).

The table is deterministic (sorted by name) and per configuration, like the printed program.

### 6.4 Typed invokers, adapters, wrappers

- **Invokers.** A call of a compiled function by `reflect.Value.Call` costs about 290 ns over the
  call itself (X2: 331 ns against 38 ns for `strings.ToUpper`, which costs 38 ns). For every
  signature of the listed functions and methods (527 distinct among 736 functions and 278 among
  697 methods of 35 of the packages, X3) the generator writes an invoker: a node factory that takes
  the func value and the argument nodes and returns the call node, the func asserted to its exact
  type once, at build time (X2: 39 ns, the call's own cost). A member whose signature has no
  invoker (none, with GS4 (a); the rarer signatures, with GS4 (b)) is called by
  `reflect.Value.Call`. The size of the invokers is measured at GR1 (GS4).
- **Adapters.** For every function type that is a parameter or result type of a listed member
  (`func(int, int) bool` of `sort.Slice`, `func(rune) rune` of `strings.Map`, the yield types of
  `iter`), and for the fixed list of common function types, the generator writes an adapter: a
  constructor of a real Go func of that type around a goi callable (a closure, or an Arbace `IFn`),
  which enters goi's thread directly (X2: `sort.Slice` of 10^5 with an adapted less 13.9 ms,
  against 11.1 ms native and 550 ms through `reflect.MakeFunc`). Other function types use
  `reflect.MakeFunc`.
- **Wrappers.** For every listed interface I, a struct type `W_<path>_<I>` embedding `goi.Proxy`
  with I's methods, each calling the proxy's descriptor's method through an adapter (§5.3).

### 6.5 The declarations digest

The digest of a listed package is Go forms (S55): its `go/package` (path, name, `:config`), one
`go/file` and its declarations with **bodies and initializers dropped** (functions and methods
`^:extern`, variables without initializers, constants with their `:tag` and `:val` but no init),
the exported declarations and every unexported declaration they reach (an unexported field's type,
an unexported result type, a promoted method's embedded type), struct types whole (their
unexported fields too, for comparability, sizes and conversions), and the **generic declarations
with their bodies**, which goi interprets (§9.3). The checker reads it as any Go forms; it trusts
the digest's `:val` without verifying it (the digest is generated from checked forms). All the
digests are one resource of the program, `arbace/go/digest.zz`, zlib-compressed as the
program's resources are (SA21), read on first use.

### 6.6 The listed set and how it grows

At GR1, the std packages the program imports directly (survey E2: 48 in the printed module),
less the seven goi cannot use as compiled packages: `unsafe` (the checker's), `embed`
(`go:embed`), `syscall`, `runtime/pprof`, `runtime/goos`, and `iter` and `weak`, whose API is
generic (§9.3). So 41: `archive/tar`, `bytes`, `compress/zlib`, `context`, `crypto/md5`,
`crypto/rand`, `crypto/sha1`, `crypto/sha256`, `crypto/sha3`, `crypto/sha512`, `crypto/tls`,
`crypto/x509`, `encoding`, `encoding/binary`, `encoding/hex`, `errors`, `fmt`, `hash/crc32`, `io`,
`io/fs`, `math`, `math/bits`, `math/rand/v2`, `net`, `net/netip`, `os`, `os/exec`, `path`,
`path/filepath`, `reflect`, `runtime`, `runtime/debug`, `sort`, `strconv`, `strings`, `sync`,
`sync/atomic`, `time`, `unicode`, `unicode/utf16`, `unicode/utf8`; at GR5 the package `arbace`
(§7.3).

The list grows by a commit to `go/arbace/gotable.edn` with the size it costs recorded (the
program's executable before and after, `bin/goi table --size`). The check's corpus will ask for
more (`bufio`, `regexp`, `container/*`, `encoding/json`, `slices` ...): a package is added when the
check gains programs by it and its size is acceptable (GS5). A package the program does not link
otherwise costs its whole exported API: X3 measured the table of 35 of the listed packages (1,081
functions and variables) at +3.9 MB on a program that links nothing of them (2.8 MB to 6.7 MB, +4.1
MB with their 202 types); for the program, which links most of these packages already, the cost
is lower and is GR1's measurement.

### 6.7 Overrides

A few members must mean what goi needs rather than what gc compiled; the list names them and the
table points them to goi's functions:

| member | goi's |
|---|---|
| `os.Exit` | §2.7 |
| `runtime.Goexit` | Go's own (the interpreted defers run through `unwind`: §4.7), listed to document it |
| `runtime.Caller`, `runtime.Callers`, `runtime.FuncForPC` | goi's frames where it can (file, line, function name of interpreted frames), else gc's: a known difference |
| `os.Args` | the program's arguments in program mode; at the REPL the REPL's (`*command-line-args*`) |
| `runtime.NumGoroutine`, `runtime.GC`, `runtime.KeepAlive`, `runtime.SetFinalizer` | Go's own (finalizers of reflect-made values work; timing unspecified) |

## 7. Calls across

| from \ to | Arbace fn | Java forms class | Go forms func | compiled Go |
|---|---|---|---|---|
| **Arbace** | — | interop, as today | the var's `GoFn` (§7.1) | the table's vars (§7.2, GR1) |
| **Java forms** | `IFn`, as today | — | through the var, an `IFn` (§7.6) | through Arbace (the table's vars) |
| **Go forms** | the package `arbace` (§7.3) or a Go func value made from an `IFn` (§7.5) | as Arbace objects, through `arbace.Invoke` (§7.3) | direct, through cells (§4.6) | the table (§7.4) |
| **compiled Go** | a Go func value or a listed interface handed in (§7.5) | through a compiled Java interface (`Dyn`), as today | a Go func value or a listed interface handed in (§7.5) | — |

### 7.1 Arbace calling Go forms

`(go.x/f args...)` invokes the var's `GoFn`, whose `invoke` calls `GoInterp.call` (§4.11): goi takes
the calling goroutine's thread (§4.3), converts each argument by its parameter's type (§5.6),
calls the function's cell, converts the results back and maps a panic leaving Go (§10.4). The
arity is Go's: a wrong count throws `ArityException`-like `IllegalArgumentException` ("go.x/f takes
2 arguments (func(int, int) int), got 3"). A method is its var `T.M` with the receiver first
(`(go.x/Point.String p)`), the receiver converted like an argument; Go's implicit `&` applies when
the receiver is a Go value of `T` and the method's receiver `*T` and the value is addressable
(never for a value from Arbace: an error asks for a pointer, `(go/new go.x/Point)` gives one).

A `GoType` is an `IFn` (S46): `(go.x/T v)` is Go's conversion `T(v)` of `v` converted from Arbace
by `T`'s underlying type; `(go.x/T {:X 1 :Y 2})` makes a value of a struct type `T` from a map of
keywords to field values (missing fields zero; an unknown key an error); `(go.x/T)` is the zero
value. `(go/new T)` allocates a `*T` (Go's `new`).

### 7.2 Arbace calling compiled Go (GR1)

`(require '[go.strings :as strings])` then `(strings/ToUpper "abc")`: the namespace of a listed
package (§2.1) has a var per member, made on demand; its `GoFn` is the same as an interpreted
function's, over the table's member: the call node is made once per member from its invoker with
parameter nodes reading a small frame (S47), so Arbace's calls of compiled Go and of Go forms are
one mechanism. Variables (`go.os/Args`) are `GoVar`s over the variable's address, types `GoType`s.
Constants come from the digest's `:val`.

This needs no interpreter (no builder, no checker beyond the declaration layer): it is GR1, Go's
standard library as a library for Arbace code, and it builds what everything else needs (the
table, the conversions, `GoValue`).

### 7.3 Go forms calling Arbace: the package `arbace` (G6)

A small hand-written Go package, import path `arbace` (`go/arbace.ae`, `go/arbace/api.ae`),
compiled into the program and listed in the table; it imports `arbace/lang` and `arbace/jrt`, and
Go forms use it like any package (`(go/file "x.go" :imports [[arbace "arbace"]])`):

```clojure
(go/func Call "Call invokes f (an IFn, a Var, or a Symbol naming a var) with args, each converted
to Arbace by its dynamic Go type (§5.6); the result is Arbace's value, unconverted.\n"
  ^any [^any f & ^{:tag (slice any)} args] ...)
(go/func Invoke "Invoke calls method name of the Java object obj by Arbace's reflection (Reflector).\n"
  ^any [^any obj ^string name & ^{:tag (slice any)} args] ...)
(go/func Str "Str is a Go string as an Arbace String.\n" ^any [^string s] ...)
(go/func GoStr "GoStr is an Arbace String, or any object's toString, as a Go string.\n" ^string [^any x] ...)
(go/func Kw "Kw is the keyword ns/name or name.\n" ^any [^string name] ...)
(go/func Sym "Sym is the symbol ns/name or name.\n" ^any [^string name] ...)
(go/func Var "Var is the var ns/name, resolved (requiring ns when needed), or panics.\n" ^any [^string name] ...)
(go/func Deref ^any [^any x] ...)
(go/func Fn "Fn is an Arbace fn calling f.\n" ^any [^{:tag (func [& (slice any)] [any])} f] ...)
(go/func Long ^int64 [^any x] ...)                ; Arbace's numbers unboxed; a ClassCastException otherwise
(go/func Float64 ^float64 [^any x] ...)
(go/func Bool "Bool is Arbace's truth of x.\n" ^bool [^any x] ...)
(go/func Seq "Seq iterates an Arbace collection.\n" ^{:tag (func [(func [any] [bool])])} [^any coll] ...)
```

Arbace's values are already legal Go `any`s (J2G-SPEC §5.1), so Go forms hold, store and pass
them unchanged. `lang`'s and `jrt`'s own Go API (`(.Invoke_O__O f x)`) stays unlisted (G6 (b) as an
unlisted escape: not reachable from Go forms at the REPL).

### 7.4 Go forms calling compiled Go

Through the table (§6): functions and methods by their invokers (§6.4), variables by address,
types by descriptor and RT, fields of compiled structs by offset (from RT), constants folded
from the digest, interface methods by the interface's method expressions. An import of a package
that is not listed and has no forms on `ARBACE_PATH` (§2.3) is a check error naming the package
and `go/arbace/gotable.edn`. An unlisted pure-Go package (`bufio`) whose forms are on
`ARBACE_PATH` is interpreted, its imports compiled where listed (GS5).

### 7.5 Compiled Go calling interpreted code

- **Functions.** An interpreted function value given to compiled code (an argument, a field of
  func type) is a real Go func of its type (§4.6): a typed adapter, else `MakeFunc`. An Arbace
  `IFn` given for a parameter of func type (`(go.sort/Slice xs (fn [i j] ...))`) is adapted the
  same way, each call converting the arguments to Arbace and the result back by the func type
  (§5.6). Panics propagate through compiled frames as Go panics.
- **Interfaces.** An interpreted value given to compiled code as an interface is its content
  (§5.3): the value itself, a proxy (`any`; `fmt` honours its methods), or the wrapper of a listed
  interface. An Arbace object given for a Go interface must implement it as a Go type (a Java
  object implements no Go interface of std; G13's `reify` of Go interfaces comes after GR5).

### 7.6 Java forms and Go forms

At the REPL they meet through Arbace (survey §4.2): Java forms call a Go forms function through
its var as an `IFn` (`(.invoke (RT/var "go.x" "f") a b)`, or Clojure code in a class body,
classes/SPEC.md §7.14), with §5.6's conversions; Go forms call Java forms' objects as Arbace
objects (`arbace.Invoke`, `arbace.Call`). Closer coupling (Go forms naming an interpreted Java
class's fields) needs a shared type model, the later study of G16.

## 8. Goroutines, channels, select

### 8.1 Goroutines

`(go call)` evaluates the callee and the arguments in the current goroutine, then starts a Go
goroutine (Go's `go`, not `jrt.Go`) that makes its goi thread and runs the call. It is a real
goroutine: compiled code it calls runs there, and Arbace code it calls adopts a jrt `Thread` on
first use (J2G-SPEC §8.4); dynamic bindings are not conveyed (as for Go code in builds). Its
uncaught panic: §2.7.

### 8.2 Channels

`(make (chan T) n)` is `reflect.MakeChan(reflect.ChanOf(dir, RT(T)), n)`, a real channel that
compiled code can use. Send, receive (comma-ok), `close`, `len`, `cap` are direct operations for
the fixed-type channels of §4.9's list (asserted once per node), else `reflect.Value`'s `Send`,
`Recv`, `Close` (X2: 157 ns for a send and a receive by reflect, 43 ns typed). Go's rules hold
because the operations are Go's: a send on a closed channel panics, a nil channel blocks forever,
`close` of a nil or closed channel panics, with gc's messages (§10.3).

### 8.3 `select`

§4.9.

### 8.4 `sync`, `time`, `context`

Compiled, through the table (§6.6): an interpreted struct with a `sync.Mutex` field holds a real
`sync.Mutex` in its RT (a field of the compiled type), locked through the table's method
expression `(*sync.Mutex).Lock` with the field's address. `sync.WaitGroup`, `sync.Once`,
`time.Sleep`, `time.After`, `time.Ticker`, `context.WithCancel` work alike, on the runtime's
scheduler.

### 8.5 Deadlocks and interrupts

The program keeps a ticker pending (`jrt.RunMain`, JC9), so Go's runtime never reports "all
goroutines are asleep": an interpreted program that blocks forever blocks (at the REPL, its call
blocks the REPL, as an Arbace `@(promise)` does; in the check, its timeout ends it: §11.3). The
REPL's interrupt (`Ctrl-C` to the REPL's thread) reaches interpreted code blocked in a channel
operation or a `select` on the REPL's thread: there goi's blocking operations also wait on the jrt
thread's interrupt token and panic with `InterruptedException` when it comes (S48); elsewhere they
are Go's own.

### 8.6 The memory model

Go's. Interpreted goroutines share real Go memory (cells, heap values) and each has its own slots;
data races are the program's, as with gc. goi's own shared state (the registry of descriptors, the
inline caches, the weak map of adapters, generations) is synchronized (caches by atomic pointers).

## 9. Generics (GR6)

### 9.1 The checker

Type parameters and constraints (type sets, `comparable`, core types), the validity of
instantiations, unification and inference as go1.27's go/types does them (`infer.go`, `unify.go`:
untyped constant arguments, inference from function arguments, reverse inference in assignment
contexts of go1.27), generic methods (go1.27), generic aliases (go1.24); the converter's `:inst`
verified (§3.6), `(inst F T)` checked.

### 9.2 Execution

Instantiation **per type-argument list** (G7), when a node referring to the instance is built,
lazily and cached by the type arguments' descriptors: a generic function instance is a `Func`
whose body is built with the type parameters bound to the concrete types, so its nodes are typed
as any other (an `int8` `+` in `Max[int8]`); a generic type's instance is a named type of its own
(`List[int]`: its descriptor, its RT tagged `goi:"main.List[int]#1"`), its methods instantiated
per instance on first use. A body is never shared between type-argument lists (no dictionaries:
G7 (a)).

### 9.3 Generics of compiled packages

A generic function or type of a compiled package has no `reflect.Value` until it is instantiated,
and the table can only hold what gc compiled. So:

- the digest keeps the **generic declarations with their bodies** (§6.5), and goi interprets them,
  their non-generic callees compiled (`atomic.Pointer[T]`'s methods call compiled
  `atomic.LoadPointer`);
- whole generic packages without assembly (`slices`, `maps`, `cmp`, `iter` but `iter.Pull`) are
  listed as interpretable (`:interpret` in `go/arbace/gotable.edn`): their forms are in the digest
  whole, and they are interpreted packages with their std paths (the exception to S1);
- a generic declaration that needs what goi cannot run (`iter.Pull`'s coroutines, through
  `go:linkname`) is unsupported, unless the list names instances of it, which the table then holds
  compiled (`:instances`).

### 9.4 What compiled code sees

An interpreted instance passes to compiled code as any interpreted type (§5.3). `reflect` names
show the tags (`%T` of a `List[int]` value).

## 10. Errors and traces

### 10.1 Check errors

§3.8: one `CompilerException` with every error, as gc words them.

### 10.2 Panics stay Go panics (G12)

§4.7. A panic in interpreted code unwinds interpreted and compiled frames alike; `recover` sees the
value whatever raised it: a Go value, a `runtime.Error`, or an Arbace exception (its `Throwable`,
since Java exceptions are Go panics: J2G-SPEC §7.9).

### 10.3 Runtime errors

Where goi performs Go's operation itself (integer division, a map write to a nil map, a send on a
closed channel, a typed load through a nil pointer), Go raises the error. Where goi checks before
(index and slice bounds, nil dereferences it makes explicit, conversions of slices to arrays,
type assertions, `make` sizes, comparing uncomparable types, negative shifts, a nil func call),
it raises a `goi.RuntimeError` (a `runtime.Error`: `Error()`, `RuntimeError()`) with gc's message,
for interpreted types with the descriptors' names:

| check | gc's message |
|---|---|
| index | `runtime error: index out of range [5] with length 3` |
| slice | `runtime error: slice bounds out of range [:5] with capacity 3` (and gc's other forms: `[5:3]`, `[::7] with capacity 5` ...) |
| nil | `runtime error: invalid memory address or nil pointer dereference` |
| assertion | `interface conversion: interface {} is string, not int`; `interface conversion: main.T is not main.I: missing method M` |
| division | `runtime error: integer divide by zero` |
| shift | `runtime error: negative shift amount` |
| map | `assignment to entry in nil map` |
| channels | `send on closed channel`, `close of closed channel`, `close of nil channel` |
| make | `runtime error: makeslice: len out of range`, `makeslice: cap out of range`, `makechan: size out of range` |
| comparison, hashing | `runtime error: comparing uncomparable type []int`, `runtime error: hash of unhashable type []int` |
| array conversion | `runtime error: cannot convert slice with length 2 to array or pointer to array with length 3` |

No `reflect` panic escapes goi: every reflect operation is guarded by goi's own check raising gc's
error, so a reflect message reaching a user is a goi bug (S49). The check compares these messages
(§11.3).

### 10.4 Panics leaving Go into Arbace

At goi's boundary to Arbace (`GoInterp.call`, the `GoFn`s, `go/eval`, `go/run-main`), a panic is:

- a `Throwable`: rethrown as it is;
- `GoExit`: passed on;
- anything else: an `arbace.lang.GoPanic`, an `ExceptionInfo` whose message is the panic's text as
  gc prints it after `panic: ` (an `error`'s `Error()`, a `Stringer`'s `String()`, a basic value
  as `print` prints it, else `(T) address` as gc's `printpanicval`), whose `ex-data` is
  `{:go/value v :go/type "T" :go/trace [...]}` (the value converted by its dynamic type, §5.6), and,
  for a `runtime.Error`, whose cause is the Java exception jrt maps it to (`ArithmeticException`,
  `NullPointerException`, `ArrayIndexOutOfBoundsException`, J2G-SPEC §7.9.5) (G12; GS11).

A `GoPanic` that goi made, thrown by Arbace code into Go code again (Go forms calling an Arbace fn
that called Go forms), is unwrapped at that crossing: Go code panics with the original value, so
an outer `recover` sees what was panicked (S50).

### 10.5 Arbace exceptions in Go code

They are Go panics whose value is the `Throwable`; deferred calls run, `recover` returns the
`Throwable` (a Java object, which Go forms can test with `arbace.Invoke` or rethrow with `panic`).

### 10.6 Traces

- **Interpreted frames.** Each goi thread keeps its chain of frames (`Top`, each frame's `Fn` and
  `Line`); a statement node that starts a new line stores it (S51: lines are statements' lines; gc
  attributes an expression spread over lines to its operator's line, which `:lines` forms do not
  keep, SPEC.md §10.1). A panic leaves the chain as it was (§4.3), so it is the panic's trace.
- **In Java's traces.** jrt's frame mapping asks the evaluator for evaluated frames through the
  hook `jrt.EvalTrace` (J2G-SPEC §10.7); goi registers a second hook, and jrt replaces each Go
  frame of goi's call body (`goi.(*Code).run`) by the interpreted frame it ran, innermost first,
  leaving goi's node frames out. An element's class is the Go package's namespace (`go.demo`), its
  method the Go function's name as gc names it (`Greet`, `(*Point).Move`, `Greet.func1`), its file
  the one gc would name for the printed program (the Go file for converted forms; for forms written
  by hand the forms file, amendment B3), its line the frame's. So `arbace.main` reports
  `Execution error (GoPanic) at go.demo/Div (REPL:7)`.
- **In program mode** goi prints gc's format (§10.7) from the same chains.

### 10.7 Uncaught panics, exit status

In program mode, and for a goroutine's uncaught panic in REPL mode (§2.7), goi prints on stderr
what gc's runtime prints: the chain of panics (`panic: A [recovered]`, then `\tpanic: B`, gc's
`printpanics`), for a nil dereference the line `[signal SIGSEGV: segmentation violation code=0x1
addr=0x0 pc=0x0]` (addresses goi cannot know: zero), a blank line, and each goroutine's trace in
gc's layout (`goroutine 1 [running]:`, `main.f(...)`, `\t/path/file.go:12 +0x0`) from the
interpreted frames; then the process exits with status 2 (program mode). `os.Exit(n)` exits with
`n`; goi's stack overflow (§4.3) with gc's `fatal error: stack overflow` and status 2.

## 11. The check (G14)

### 11.1 Per program

For each program of the corpus (§11.2), from the same forms:

1. **gc's reference**: the forms printed by repr/go and built by gc as the round trip's level
   `run` builds them (ROUNDTRIP.md, "Directory tests"; for single-file programs as Go's test driver
   runs `run` programs), run with the action's arguments; its stdout, stderr and status recorded,
   cached by the forms' and the toolchain's hashes (`.tmp/goi-check/ref/`), and checked against the
   program's golden `.out` where there is one (S52);
2. **goi's run**: `bin/goi run` in program mode, in a fresh process of the gate's executable, with
   the same arguments, a timeout of 10 s (or the action's), 16 programs at a time;
3. **the comparison**: the exit status; stdout byte for byte; stderr up to the first line that
   starts with `goroutine ` (so `println`'s output and the `panic:` lines are compared, the traces
   are not), with `[signal ...]` lines' addresses normalized.

### 11.2 The corpus

repr/go's conversions for `linux/amd64` (the program's configuration on the host): the single-file
programs of `$GOROOT/test` whose action is `run` (1,037 in the toolchain's tree, survey §4.3;
`bin/repr-go read --corpus linux-test`), and the directory tests whose action is `rundir`,
`buildrundir` or `runindir` (127, `--corpus linux-dirtest`). The forms are regenerated, not
tracked (SPEC.md decision 9). Programs whose action needs gc's flags (`-gcflags`, `-race`,
`GOEXPERIMENT`) are left out by the list itself, as the round trip's are.

### 11.3 Results

Each program is `pass`, `fail` (the outputs differ), `error` (goi crashed, a reflect panic, a
timeout), `unsupported` (the checker's "not supported by goi yet" or an out-of-scope feature:
assembly, `go:linkname`, `unsafe` arithmetic, an import that is neither listed nor in forms; with
the reason), or `known`: listed in `test/goi/check-known.edn` with its reason, a program that
tests what goi does not reproduce by design (gc's escape analysis or inlining as observed by
`testing.AllocsPerRun` or `runtime.ReadMemStats`, `runtime.Callers`' program counters,
finalizer timing, `%T` of interpreted types (G8), `GODEBUG` behaviour). The reference
`test/goi/check-linux-amd64.edn` records every program's result; the run fails on a regression (a
program passing there and not now) and `--record` writes a new reference, as `bin/repr-go
roundtrip` does.

### 11.4 The annotation check

`bin/goi annotations [CORPUS...]` checks, without running anything, every package of
`tamago/goroot` (tamago, amd64 and arm64: one tree) and of the converted `linux-amd64` std and
`$GOROOT/test` corpora: zero errors on these valid packages, and every annotation of §3.6 verified.
It reports the counts verified per kind (survey §2.1's 44,435 `:tag`, 13,413 `:val`, 4,989 `:go/via`,
1,065 `:inst`, 765 `^:assign` over `tamago/goroot`) against `test/goi/annotations-*.edn`. Until GR6,
declarations whose bodies use generics are skipped and counted apart (S53).

### 11.5 Where it runs

In `bin/gate --full`: `bin/goi annotations` over every corpus, `bin/goi check` over the whole
corpus (estimated minutes: 1,164 programs of which most run in well under a second, 16 at a time;
gc's references cached). In the essential gate, if measured in minutes at GR3, a subset: a fixed
list of fast programs (`test/goi/essential.txt`, about 200) and the annotation check of a few
packages (GS12). The cross-dialect cases of GR5 (`test/oracle/forms/go.ae`) are in the oracle,
their expected results recorded by goi and reviewed, since the frozen jar has no Go forms (S54).

### 11.6 Later

`errorcheck` programs (the checker's errors against their `// ERROR` patterns: they need mode
`:full`'s comments) after GR6; the check in the box (tamago) after GR4 (GS13).

## 12. Milestones

From the survey's plan (§6.2), refined. Each milestone ends with its check green and its records
(journal, agenda; this spec amended where building it found otherwise, as SPEC.md's amendments
were). All come after the coming re-seed; GR1, GR3, GR5 and GR6 change the program (jrt, goi, the
table, the embedded namespaces), each at most one re-seed, batched with other runtime changes.

| milestone | content | exit criteria | effort (days) |
|---|---|---|---:|
| **GR1** Arbace calls compiled Go | `go/arbace/gotable.edn` (§6.6); the generator `arbace.go.table` and the digest (§6.3, §6.5); the checker's declaration layer (type expressions, signatures, method sets, constants from `:val`); `arbace/goi`'s descriptors of compiled types, conversions (§5.6), invokers (§6.4); `GoInterp`'s natives for it, `GoFn`, `GoVar`, `GoType`; jrt's `GoValue` hooks (§5.5) and `print-method`; `require` of a listed package's namespace (§7.2); `bin/goi table --size` | `test/goi/calls.ae`: about 200 calls of the listed packages (`strings`, `strconv`, `math`, `bytes`, `unicode/utf8`, `time`, `sort`, `errors`, `fmt.Sprintf`, methods of `strings.Builder` and `bytes.Buffer`) against expected values written by hand, in the smoke test; the executable's size before and after recorded (GS4, GS5); the essential gate green | 3-4 |
| **GR2** the checker | `arbace.go.check` without generics (§3): scopes, objects, types, constants (§3.5), assignability, conversions, method sets, embedding, statements, initialization order, language versions, errors (§3.8); the annotation verification (§3.6); `bin/goi annotations` | over `tamago/goroot` and the `linux-amd64` std and `$GOROOT/test` corpora: zero errors and every annotation verified in every declaration that uses no generics, the others counted (S53); `test/goi/checked/` golden trees; `test/goi/errors/` (hand-written invalid forms, gc's messages) | 5-7 |
| **GR3** the interpreter's core | `arbace.go` and `arbace.go.repl` (§2): namespaces, macros, loading, redefinition, initialization; the builder (§4.1); the back end: frames, threads, cells, nodes for scalars, strings, slices, arrays, maps, structs, pointers, functions and closures, all control flow with `goto` and labels, `defer`, `panic`, `recover`, runtime errors (§10.3); interface values without methods (§5.3 rule 1, `any` to `fmt`); program mode, `bin/goi run`; the check's driver (§11) | `bin/goi check` over the corpus: every program within GR3's subset passes or is known, the rest `unsupported`; the reference recorded; `fib 22` and `sumSq 1e6` through the real builder within twice X2's sketch | 6-8 |
| **GR4** methods, interfaces, concurrency | methods, method values and expressions, interfaces, proxies and wrappers (§5.3), type switches and assertions, embedding and promotion at run time, goroutines, channels, `select` (§8), `range` over functions (§4.10) | the check with the directory tests (rundir, buildrundir, runindir); more programs pass, none regresses | 3-5 |
| **GR5** the crossings and the REPL | Arbace fns as Go func values (§7.5), `GoPanic` (§10.4), Go frames in traces (§10.6), the REPL's printing (§5.7), `go/eval`, `go/run-main`, `go/run-compiled` (G15), the package `arbace` (§7.3), Java forms and Go forms through vars (§7.6); GS7, GS9, GS10 if decided so | `test/oracle/forms/go.ae`: cross-dialect cases in the oracle (S54); §1.4's session as a smoke test | 3-4 |
| **GR6** generics | the checker's generics (§9.1), instantiation per type-argument list (§9.2), compiled packages' generics from the digest and the interpretable packages (§9.3) | `$GOROOT/test/typeparam`'s run programs in the check; the annotation check over every declaration, `:inst` included | 4-6 |
| GR7 (later) | `reify` of listed Go interfaces from Arbace (G13); `errorcheck` programs; the check in the box (GS13); staging shared with CF after Futamura's F7 (G16); the runtime overlay for named types (G8, GS8); speed (F10's inline caches, specialization) | as above | |

**Effort to GR6: 24-34 working days**, against the survey's 21-31 for GR1-GR6: GR1 gains the
checker's declaration layer (the table needs signatures and method sets before the checker is
complete), GR3 the program mode and the check's driver, GR6 the digest's generic bodies and the
interpretable packages.

## 13. Decisions and open questions

### 13.1 Decisions taken while writing (S1-S55)

Each on the terms of G1-G17; the user may revise any (§13.2 asks about the ones with real
alternatives).

| S | decision | reason |
|---|---|---|
| S1 | A namespace's kind (compiled from the table, or interpreted) is fixed when it is made; an interpreted package may not take a listed path, except the interpretable generic packages (§2.1, §9.3) | one package per path, as in a Go program |
| S2 | At the REPL the first Go declaration in a `go.*` namespace makes its package implicitly, named by the last segment, its path the segments joined by `/` (§2.1) | the REPL needs no `go/package`; SPEC.md §4.1's mapping read backwards |
| S3 | `go/eval` outside `go.*` runs in the package `go.user` (§2.1) | Go context needs a package |
| S4 | An unresolvable `go/` head in a `go.*` namespace loads `arbace.go` and aliases it (§2.2) | as `Compiler.classForms` loads the Java forms; converted forms require it anyway |
| S5 | A current file per namespace, `repl.go` by default, its imports set by `go/file` or extended by `go/import` (§2.2) | Go's imports are file-scoped |
| S6 | In a load, declarations are queued and the package checked when its package file's load ends, through an after-load hook of `arbace.core/load` (§2.3) | Go checks a package whole; the hook is goi's one change to the vendored core |
| S7 | `go/eval` returns its last expression's value, a vector for several, `nil` for none (§2.4) | as a body's value |
| S8 | A method defined again keeps its type's generation (§2.5) | RT has no methods; only a new underlying type needs a new RT |
| S9 | A listed package's namespace makes its vars on demand (§2.1) | 41 packages' members interned at once would cost start-up |
| S10 | A `go/var` defined again with its type keeps its Go variable (§2.5) | pointers to it keep aliasing it, as `def` keeps its var |
| S11 | At the REPL, a `go/var` is initialized and an `init` runs when defined (§2.6) | the REPL has no program start |
| S12 | `os.Exit` in REPL mode throws `GoExit`, deferred calls running (§2.7) | the REPL's process must live; program mode keeps gc's exit |
| S13 | An uncaught panic of another goroutine in REPL mode ends that goroutine only (§2.7) | gc would end the process, and the REPL with it |
| S14 | The checker is written anew in Arbace after go/types, not translated (§3.2) | go/types is Go over `go/ast`; the forms are Arbace data |
| S15 | Constants are Arbace's exact numbers with go/constant's limits and rounding (§3.5) | agreement with the converter's `:val` |
| S16 | An annotation that disagrees with the checker is an error (§3.6) | either is a bug; trusting would hide one |
| S17 | Errors as gc words them, the forms' file and line, `ex-data` with go/types' codes (§3.8) | users know gc's messages; tools need data |
| S18 | A function's nodes are built on its first call; `bin/goi check` builds all (§4.1) | loading checks everything and builds only what runs, as the Java forms' interpreter |
| S19 | The operator and type variants of the nodes are generated from a table, control flow written by hand (§4.2) | about 250 factories, most mechanical |
| S20 | Per-iteration copies only for loop variables that are cells (§4.3) | the difference is visible only through captures and addresses |
| S21 | Frames are windows of a per-goroutine stack per slot kind (§4.3) | X2: `fib 22` 2.87 ms against 4.95 ms |
| S22 | jrt's `Thread` gains a field for goi's thread (§4.3) | compiled code calling back continues the same thread and trace |
| S23 | An interpreted call depth limit, gc's text (§4.3) | Go's stack overflow is fatal |
| S24 | `print` and `println` with gc's runtime formatting, to stderr (§4.4) | the check compares stderr |
| S25 | Typed stores, and `reflect`'s typed copy for other types; never an untyped copy (§4.4) | the garbage collector's write barriers |
| S26 | `goto` as a jump code that the statement list holding the label resumes at (§4.5) | Go's rules for `goto` make it enough |
| S27 | A static call loads its `Func`'s code at each call (§4.6) | redefinition is seen; one load |
| S28 | Calls of several results write the caller's result slots (§4.6) | no allocation per call |
| S29 | A closure stored in Go memory becomes an adapter made once and recorded (§4.6) | speed (X2) and getting the closure back |
| S30 | A Go `defer` only in functions with `defer` statements (§4.7) | 5 ns per call otherwise (X2) |
| S31 | `select` of at most two clauses on fixed-type channels is a Go `select` (§4.9) | `reflect.Select` costs 202 ns (X2) |
| S32 | `GoInterp`'s natives are `arbace/goi`'s, an amendment of J2G-SPEC §9.1 (§4.11) | goi cannot import `arbace/lang` |
| S33 | The frame representation is the back end's, refinable at GR3; the Go-memory representation is normative (§5.1) | measured choices, a fixed boundary |
| S34 | A Java primitive array is accepted for a slice of its element type, copied (§5.1) | Arbace's byte arrays to Go's `[]byte` |
| S35 | A Go func reaching Arbace is a `GoFn` around it, unwrapped back to Go (§5.1) | callable from Arbace, unchanged for Go |
| S36 | Embedded fields are plain fields of the type's name in RT (§5.2) | `StructOf` refuses embedded types with methods (X1) |
| S37 | A named struct type's RT has `goi:"path.Name#gen"` on its first field's tag (§5.2) | an RT per named type and generation; conversion, `fmt`, `json` unchanged (X1) |
| S38 | Interpreted values with methods or without a faithful RT are proxies in interfaces, struct values with `Format` (§5.3) | Go's `==` and map keys right, `fmt` honours their methods (X1) |
| S39 | goi never looks methods up by a name that is not constant (§5.3) | the linker's dead code elimination (X3: +1.1 MB on 35 packages) |
| S40 | Typed fast paths for maps of fixed Go types (§5.4) | `reflect`'s map access is slow |
| S41 | `GoValue`'s hash by `maphash.Comparable` with one seed per process (§5.5) | consistent with Go's `==` |
| S42 | Go values print as `#go "%#v"` (§5.5) | readable, clearly not data |
| S43 | `error` results are values in the result vector (§5.6) | Go's style; Arbace code decides |
| S44 | `linux`'s listed packages are converted at build time and cached (§6.3) | std's linux forms are not tracked |
| S45 | Package tests are not loaded in GR1-GR6 (§1.1) | not needed by the REPL or the check |
| S46 | A `GoType` is an `IFn`: conversion, a struct from a map, the zero value (§7.1) | Go values made from Arbace without Go context |
| S47 | Arbace calls compiled Go through the table's invokers over a small frame (§7.2) | one mechanism for both callees |
| S48 | On the REPL's thread, blocking channel operations also wait for its interrupt (§8.5) | `Ctrl-C` must reach blocked Go code |
| S49 | No `reflect` panic escapes goi (§10.3) | users see gc's messages, never reflect's |
| S50 | A `GoPanic` re-entering Go is unwrapped to its value (§10.4) | an outer `recover` sees what was panicked |
| S51 | Frames' lines are the lines of statements (§10.6) | what `:lines` forms keep |
| S52 | The check's reference is gc's run of the printed forms, cached, checked against `.out` (§11.1) | the same forms on both sides |
| S53 | The annotation check skips generic declarations until GR6, counted (§11.4) | GR2's checker has no generics |
| S54 | GR5's cross-dialect oracle cases are recorded by goi and reviewed (§11.5) | the frozen jar has no Go forms |
| S55 | The digest is Go forms without bodies, generic bodies kept (§6.5) | the checker reads it as any forms; no second format |

### 13.2 Open questions for the user (GS1-GS15)

Each with the recommendation this text follows.

- **GS1 Compiled packages' namespaces.** (a) `require`d as any namespace (§2.1); (b) also made
  automatically when a qualified symbol names one (`(go.strings/ToUpper "x")` without `require`).
  *Recommended: (a)*: Arbace's rule for namespaces.
- **GS2 The REPL's imports.** (a) explicit, `go/file` and `go/import` (S5); (b) also listed
  packages imported automatically by their name on first use, at the REPL only (goimports' way).
  *Recommended: (a)* now, (b) to reconsider after GR5.
- **GS3 `go/eval`'s locals.** (a) local to one `go/eval` (§2.4); (b) kept across calls as yaegi
  does (they would become package variables of `go.user`). *Recommended: (a)*: `go/var` is the
  REPL's state, and (b) changes `:=`'s meaning.
- **GS4 Invokers.** (a) one per signature of every listed member (§6.4); (b) only the commonest
  signatures, `reflect.Value.Call` for the rest. *Recommended: (a)*, unless GR1 measures the
  invokers above 1 MB of the executable, then (b) with a threshold.
- **GS5 Growing the listed set.** (a) by need, each addition with its size recorded, and unlisted
  pure-Go packages interpreted from their forms when on `ARBACE_PATH` (§7.4); (b) the list only.
  *Recommended: (a).*
- **GS6 Arbace collections for slices and maps.** (a) explicit, `(go/slice T coll)` and
  `(go/map K V m)` copying; (b) vectors and maps converted implicitly when a parameter is a slice or
  map type. *Recommended: (a)*: an implicit copy breaks Go's aliasing silently.
- **GS7 Go values in Arbace's collection protocols.** (a) at GR5, read-only: `ILookup` for struct
  fields (keywords) and map keys, `Indexed`, `Counted`, `Seqable` for arrays, slices, strings and
  maps, elements converted (§5.6); (b) none. *Recommended: (a).*
- **GS8 The runtime overlay for named types** (G8's later decision). (a) decided after GR6, with the
  check's record of the differences it would remove; (b) at GR4. *Recommended: (a).*
- **GS9 `%T` of interpreted values.** (a) at GR5, goi's override of `fmt`'s `Printf` family when
  called from interpreted code, formatting `%T` (and `%v` of proxies' types) by descriptors; (b)
  left a known difference. *Recommended: (a)*, if the check finds it frequent.
- **GS10 Interop on Go values from Arbace.** (a) at GR5, `(.Len buf)` on a Go value resolved by
  Reflector's fallback through the table; (b) only `go/` functions (`(go.bytes/Buffer.Len buf)`,
  the method's var). *Recommended: (a).*
- **GS11 Runtime errors in Arbace.** (a) a `GoPanic` with jrt's Java exception as its cause
  (§10.4); (b) the Java exception itself (`(catch ArithmeticException e)` catches Go's division by
  zero). *Recommended: (a)*: one class for Go's panics, the cause for those who want it.
- **GS12 The check in the essential gate.** (a) a subset (about 200 programs and a few packages'
  annotations) if it runs in minutes, measured at GR3; (b) `--full` only. *Recommended: (a).*
- **GS13 The check in the box.** (a) after GR4, a subset run by the tamago guest in `--full`; (b)
  not. *Recommended: (a).*
- **GS14 `go:embed` at the REPL.** (a) after GR5, the embedded files read from the package's forms
  directory (amendment B2) when the package loads; (b) out of scope. *Recommended: (a).*
- **GS15 Go's standard output at the REPL.** (a) interpreted Go writes to the process's own
  `os.Stdout` (fd 1), apart from Arbace's `*out*`, a known difference; (b) while the REPL runs Go
  code, `os.Stdout` is a pipe copied into `*out*`. *Recommended: (a)*: (b) changes the process's
  `os.Stdout` for compiled code too.

## 14. Sources

- go1.27.1, TamaGo's toolchain `/root/tamago-go`: `src/go/types` (`api.go`, `object.go`,
  `scope.go`, `decl.go`, `resolver.go`, `typexpr.go`, `struct.go`, `interface.go`, `signature.go`,
  `validtype.go`, `const.go`, `expr.go`, `call.go`, `selection.go`, `lookup.go`, `methodset.go`,
  `index.go`, `conversions.go`, `builtins.go`, `literals.go`, `operand.go`, `stmt.go`,
  `assignments.go`, `return.go`, `range.go`, `labels.go`, `initorder.go`, `version.go`,
  `sizes.go`, `infer.go`, `unify.go`), `src/go/constant` (`value.go`: `maxExp`, the switch from
  `big.Rat` to `big.Float`), `src/reflect/type.go` (`StructOf`: unexported fields with `PkgPath`,
  embedded fields with methods refused; `runtimeStructField`), `src/reflect/value.go`
  (`MakeFunc`, `Select`, `NewAt`), `src/runtime/error.go` (`boundsError` formats,
  `TypeAssertionError`, `printpanicval`), `src/runtime/panic.go` (`printpanics`, "[recovered]"),
  `src/runtime/print.go` (`print`'s formats), `src/hash/maphash` (`Comparable`),
  `doc/go_spec.html` (go1.27), `src/cmd/internal/testdir` (actions and outputs).
- The survey, [GO-FORMS-REPL-SURVEY.md](../GO-FORMS-REPL-SURVEY.md) (G1-G17, its measurements and
  prior art: yaegi v0.16.1, gomacro, ixgo, `go/ssa/interp`), decided 2026-10-17.
- Arbace: [SPEC.md](SPEC.md) (§4, §6-§8, §10, §11, §13.5), [J2G-SPEC.md](J2G-SPEC.md) (§5.1,
  §5.8, §5.12, §7.9, §8.4, §9.1, §9.3, §10.7), [CLASSFORMS-REPL.md](CLASSFORMS-REPL.md),
  [ROUNDTRIP.md](ROUNDTRIP.md) ("Directory tests", the corpora), [BUILD.md](BUILD.md),
  [FUTAMURA-SURVEY.md](../FUTAMURA-SURVEY.md) (F2 as built, F7, F10),
  [NAMING-SURVEY.md](../NAMING-SURVEY.md), [haskell/SPEC.md](../haskell/SPEC.md) (this text's
  form); `arbace/lang/Compiler.ae` (`ClassFormsExpr`, `classForms`, the natives of
  `Compiler$CFGo`), `arbace/classes/interp.ae`, `go/arbace/jrt/object.ae` (`notObject`),
  `arbace/core.ae` (`load`, `load-one`), the seed's printed module (`seed/arbace-seed-go.tar.xz`,
  extracted by the survey: the std imports, `jrt`'s `MethodByName("Load")`).

## Appendix A: the experiments

Scratch in `.tmp/go-forms-spec/` (not tracked): `env.sh` (TamaGo's go1.27.1, `CGO_ENABLED=0`,
`GOTOOLCHAIN=local`, a build cache of its own, no network), `x1/`, `x2/`, `x3/`. Builds of `x3`
ran through `.tmp/heavy`. Best of 5, amd64, the machine shared.

**X1: reflect-made values** (`x1/main.go`, `go run .`). What the spec's value model (§5) rests on:

| probe | result | used in |
|---|---|---|
| `StructOf` of the same fields twice | one type | §5.2 |
| `struct{X, Y int}` with `goi:"main.P#1"` on `X`'s tag, against the untagged type, against `Q#1` and `P#2` | four distinct types; `P#1` made again is `P#1` | S37 |
| `P#1` `ConvertibleTo` `Q#1`, `AssignableTo` | true, false (Go's: conversion ignores tags) | §4.4 `conv` |
| `%v`, `%+v`, `json.Marshal` of a `P#1` value | `{1 2}`, `{X:1 Y:2}`, `{"X":1,"Y":2}` (gc's for `main.P`) | S37 |
| `%T` | `struct { X int "goi:\"main.P#1\""; Y int }` | §5.2 (known difference) |
| a `P#1` value as `any` asserted to `struct{X, Y int}` | fails; `reflect.TypeOf` is `P#1` | §5.2 faithful types |
| an unexported field `x` with `PkgPath "main"` | accepted; `%+v` `{x:7 Name:a}`; `json` `{"Name":"a"}`; `reflect`'s `SetInt` on it panics ("using value obtained using unexported field"); a store through its offset works | §5.2, §4.4 |
| an embedded `*bytes.Buffer` with another field | panics: "embedded type with methods not implemented if there is more than one field"; alone it works (28 methods); as a plain field named `Buffer`, `%+v` `{Buffer:hi X:0}` | S36 |
| `goi.Proxy{T, V}` values: two of `36.6` | equal; one finds the other as a map key; not equal to the bare `36.6`; an uncomparable `V` as a key panics with Go's "hash of unhashable type []int" | S38 |
| `Proxy.Format` delegating | `%v` `36.6`, `%.2f` `36.60`, `%#v` `main.Celsius(36.6)`, `Println` `36.6 [36.6 1]` | S38 |
| a proxy with an interpreted `String` (through `Format`, fmt's order) | `Println` `36.6°C`, `%v` `36.6°C`, `%s` `36.6°C`; without the `Format` rule a wrapper's `String` is not called (fmt prefers `Formatter`) | §5.3 |
| a wrapper of `io.Reader` | `io.ReadAll` reads through the interpreted `Read` | §5.3 rule 3 |
| `MakeFunc` of `func(rune) rune` to `strings.Map` | `ABC` | §4.6 |
| `sort.Slice` over a reflect-made `[]P` | sorted | §7.5 |
| `map[P]string`, `chan P` (a goroutine sends), `[3]P` | work as Go's; `%T` of the channel shows the tag | §5.4, §8.2 |
| `*P` against `*struct{X, Y int}` | distinct | §5.2 |
| `fmt.Sscan` into `reflect.New(int)` | 42 | §4.3 cells |

**X2: costs** (`x2/main.go`, `go build`, `./x2.exe`):

| | ms | ns per operation |
|---|---:|---:|
| `fib 22`, typed closures, frames allocated per call (the survey's sketch) | 4.95 | |
| `fib 22`, typed closures, slots on the goroutine's stack (S21) | 2.87 | |
| `fib 22`, gc (a func value) | 0.23 | |
| a field `X += 1` of a reflect-made struct by `reflect`'s `Field(i)` (10^6) | 7.83 | 7.8 |
| the same by its offset | 0.64 | 0.6 |
| `strings.ToUpper("abc")` by `reflect.Value.Call` (10^6) | 330.8 | 330.8 |
| the same by a typed invoker (the func asserted once) | 39.2 | 39.2 |
| the same called directly | 37.8 | 37.8 |
| `sort.Slice` of 10^5 ints, 1,723,501 calls of `less` by `reflect.MakeFunc` | 549.6 | 319 per call |
| the same, `less` a typed adapter around a goi body | 13.9 | |
| the same, native `less` | 11.1 | |
| a send and a receive on a buffered `chan int` by `reflect` (10^6) | 156.7 | 156.7 |
| the same, typed | 43.5 | 43.5 |
| `reflect.Select` of a send and a receive (10^6) | 201.7 | 201.7 |
| a call wrapped in a Go `defer` with `recover` (10^6) | 5.48 | 5.5 |
| the same call without | 0.57 | 0.6 |

**X3: the table's size and its signatures** (`x3/gen/main.go`, a generator of a table of the
exported API by go/types' source importer, as goi's generator will write it; `x3/sigs/main.go`).
For 35 of the listed packages (the 41 less `crypto/rand`, `crypto/sha3`, `encoding`, `reflect`,
`runtime`, `runtime/debug`), `go build -trimpath -ldflags=-s`:

| program | bytes |
|---|---:|
| the 35 packages imported blank (`import _ "..."`) | 2,789,536 |
| with a table of their 1,081 exported functions and variables (`reflect.ValueOf` of each) | 6,725,792 (+3,936,256) |
| with their 202 named types as well (`reflect.TypeFor`) | 6,852,768 (+4,063,232) |
| the functions' table and a `reflect.Value.MethodByName` with a name from the command line | 7,827,616 (+1,101,824 over the functions' table) |

The same 35 packages: 736 exported non-generic functions with 527 distinct signatures (the 50
commonest cover 220 functions), and 697 exported methods of their types (pointer method sets) with
278 distinct signatures without the receiver (the 50 commonest cover 445). The program links much
of these packages already, so the table's real cost to it is lower than this upper bound: GR1
measures it (§6.6).

## Appendix B: a worked example

The survey's `sumSq`, typed at the REPL (survey Appendix A):

```clojure
(go/func sumSq ^int64 [^int32 n]
  (let [^int64 s (zero int64)]
    (for [i (conv int32 0)] (< i n) (inc! i)
      (set! s + (* (conv int64 i) (conv int64 i))))
    s))
```

**The checked tree** (§3.7), abridged; `n` is a parameter, `s` and `i` locals, none a cell (not
captured, not addressed); `i` is per-iteration, which changes nothing for a variable that is not a
cell (S20):

```clojure
{:op :func :obj sumSq :params [n] :results [{:type :int64}] :line 1
 :body {:op :block
        :stmts [{:op :decl :objs [s] :init [{:op :const :type :int64 :val 0}]
                 :body {:op :block
                        :stmts [{:op :for :line 3
                                 :init {:op :decl :objs [i] :init [{:op :const :type :int32 :val 0}]}
                                 :cond {:op :binary :go/op :< :type :untyped-bool
                                        :x {:op :local :obj i :type :int32} :y {:op :local :obj n :type :int32}}
                                 :post {:op :incdec :inc true :place {:op :place-local :obj i :type :int32}}
                                 :body {:op :block
                                        :stmts [{:op :op-assign :go/op :+ :line 4
                                                 :place {:op :place-local :obj s :type :int64}
                                                 :value {:op :binary :go/op :* :type :int64
                                                         :x {:op :conv :type :int64 :x {:op :local :obj i :type :int32}}
                                                         :y {:op :conv :type :int64 :x {:op :local :obj i :type :int32}}}}]}}
                                {:op :return :line 5 :values [{:op :local :obj s :type :int64}]}]}}]}}
```

**The nodes** the builder makes (§4), with `I` slots `n` 0, `s` 1, `i` 2 and the result 3, as Go
the factories return (written as Go for brevity; they are Go forms in `arbace/goi`):

```go
n, s, i := LocalI(0), LocalI(1), LocalI(2)
body := Seq(
	SetI(1, ConstI(0)),                                   // s := int64(0)
	For(SetI(2, ConstI(0)),                               // i := int32(0)
		LtI(i, n),                                        // i < n, at int32 (I values normalized)
		SetI(2, IncI32(i)),                               // i++: int64(int32(i + 1))
		OpSetI(1, AddI64, MulI64(ConvI32toI64(i), ConvI32toI64(i)))), // s += int64(i) * int64(i)
	ReturnI(3, s))                                        // the implicit return of s
```

`ConvI32toI64` is the identity on a normalized `I` (an `int32` held sign-extended is already its
`int64` value), so the builder drops it; `IncI32` normalizes. These are the survey's typed-closure sketch's nodes (its Appendix A), which ran
`sumSq 1e6` in 22 ms (survey §5.6) against Arbace's Java forms interpreter's 95-127 ms.

## Decided (2026-10-17, the user)

GS1-GS15 as recommended: compiled packages' namespaces `require`d (GS1); explicit imports,
auto-import reconsidered after GR5 (GS2); `go/eval`'s locals local (GS3); an invoker per listed
signature unless GR1 measures them above 1 MB (GS4); the listed set grown by need, unlisted
pure-Go packages interpreted from their forms (GS5); explicit `go/slice` and `go/map` copies
(GS6); Go values read-only in Arbace's collection protocols at GR5 (GS7); the runtime overlay
decided after GR6 (GS8); goi's own `%T` at GR5 if frequent (GS9); `(.M v)` interop through the
table at GR5 (GS10); `GoPanic` with jrt's exception as cause (GS11); an essential-gate subset if
minutes (GS12); the check in the box after GR4 (GS13); `go:embed` after GR5 (GS14); interpreted
Go writing to the process's own stdout (GS15).

## As built: GR1 (2026-10-17)

GR1, Arbace calling compiled Go, is built on branch `goi-gr1` (from main `73e35434`, seed
`cf89f8a8`). Where building it found otherwise than this text, the text below is the record (as
SPEC.md's amendments were); proposals for the user are GC10-GC14 (GC1-GC9 are GR2's).

**What exists.**

| part | where | as built |
|---|---|---|
| the list | `go/arbace/gotable.edn` | §6.6's 41 packages; `:exclude` (reflect's `Value.Method`, `MethodByName`, `Methods`, `Type.Method`, `MethodByName`, `Methods`: S39), `:overrides` (`os.Exit` → `goi.Exit`), `:min-sharing 2` (GS4 (b), below), `:resolve` (empty) |
| the generator | `arbace/go/table.ae` (`arbace.go.table`), `bin/goi table` | on the tools' host, before j2g in `bin/arbace-go`'s `build` (kept when its inputs are unchanged: `DIR/key`); 31 s for the four configurations on stage 0, the linux forms converted once by `bin/repr-go read` and cached (`.tmp/goi-table/KEY/linux-ARCH`, KEY the list's, repr/go's, godump's and Go's hashes: S44) |
| the table | `target/arbace-go/gotable/` → j2g `--gotable DIR` | the package `arbace/gotable`, imported (blank) by the main package; for linux/amd64: 829 functions, 347 variables, 244 types, 999 methods; 782 distinct signatures (functions' and methods' without the receiver), 233 typed invokers |
| the digest | `gotable/digest_GOOS_GOARCH.edn.z` | one per configuration, embedded by that configuration's `register_sN.go`, 1,305,312 bytes (317,430 zlib-compressed) for linux/amd64 |
| goi | `go/arbace/goi.ae`, `go/arbace/goi/` | `frame` (Frame, I F S R T, LocalI ..., As, B2I), `table` (Member, Package, Register, Lookup, the digest), `convert` (§5.6 both ways), `call` (Callable: a member's call node from its invoker over a small frame, S47; reflect.Value.Call without one; the boundary for panics), `value` (GoValue's protocol), `natives` (GoInterp's, Type) |
| `arbace.lang` | `arbace/lang/GoInterp.ae` | `GoInterp` (natives and their Arbace side), `GoFn`, `GoVar`, `GoType`, `GoValue`, and GR1's minimal `GoPanic`, `GoExit` |
| `arbace.go` | `arbace/go.ae` | `table-ns!` (a listed package's namespace, vars on demand: S9), `digest-forms`, `packages`, `new`, `zero`, `type-of`, `str`, `spread`, `assign!` |
| jrt | `go/arbace/jrt/object.ae`, `string.ae` | `GoValueClass`, `GoValueOps`, `IsGoValue`; `Equals`, `HashCode`, `ToString`, `GetClass`, `IdentityHash` and `String.valueOf` answer for Go values (their fast paths unchanged, the Go value's case out of line) |
| the core | `arbace/lang/RT.ae`, `Namespace.ae`, `arbace/core_print.ae` | VENDOR-NOTES.md, hand change 26: RT.load's table first; a namespace's resolver; `print-method` of GoValue |
| j2g | `arbace/j2g/{decls,main,out}.ae` | GoInterp's natives are goi's (J2G-SPEC §9.1, amendment GR1-1); GoValue's `instanceof` is `jrt.IsGoValue`; goi copied into the program; `--gotable` |
| the test | `test/goi/calls.ae`, in `bin/arbace-go --smoke` | 289 calls of 31 of the listed packages, expected values written by hand |

**The sizes (GS4, GS5).** Measured on linux/amd64 by `bin/goi table --size` (the work module
linked as the build links it, stripped, without the image of prepared namespaces), and the
executables of `bin/arbace-go --build`:

| | bytes |
|---|---:|
| the executable before GR1 (main `73e35434`, = stage 0 of seed `cf89f8a8`) | 104,845,472 |
| the executable after GR1 (`bin/arbace-go --build`) | 110,555,296 (+5,709,824, 5.4%) |
| of which the table, all invokers (the first build: one per signature, 763) | 4,247,552 |
| of which the invokers in it | 1,630,208 |
| the table with `:min-sharing 2` (233 invokers), as built | 3,477,504 |
| of which the invokers in it | 860,160 |
| goi, GoInterp's classes, `arbace.go` and `arbace.go.table` compiled (the executable without the table, but for its image) | 2,232,320 |

The invokers of every signature cost 1.63 MB, above GS4's 1 MB: so GS4 (b), the threshold
variant, as decided: a typed invoker for each signature at least two members share (in one
configuration), 233 of 782; the other members (most of them methods of one type) are called by
`reflect.Value.Call`. The thresholds measured with linux/amd64's table alone: shared by 2, 231
invokers, 856,064 bytes (860,160 as built, the four configurations' table); by 3, 121, 667,648 bytes; by 5, 54, 512,000 bytes. The table's members without the
invokers cost 2.6 MB: the program links most of the listed packages already, but not every
exported function and method. **Found while measuring:** go1.27's `reflect.Value.Methods` and
`reflect.Type.Methods` (iterators over methods) are, like `Method` and `MethodByName`, marked
ReflectMethod; listed, they made the linker keep every exported method of every reachable type:
the table cost 21.5 MB, not 4.2 (the list excludes them).

**Amendments (as built).**

1. **The table's files** (§6.2): one file per listed package and set of configurations whose
   table is alike (`strings_s15.go`, `s15` the bits of the configurations, never a GOOS or GOARCH
   suffix, which gc would read as a constraint), with the set's build line when it is not all
   four; `invokers_sN.go` likewise; `register_sN.go` per configuration registers the packages
   and the digest. The linux tables of amd64 and arm64 are alike but `runtime`'s; tamago's differ
   in `net` and `runtime`. A member is `(lit _ "Name" value invoker-or-nil)`; a package's table is
   a package-level `goi.Package` variable, registered by address at init.
2. **Method invokers are generic over the receiver** (§6.4): one per signature without the
   receiver, `(go/func minvN :type-params [Rcv] ...)`, its receiver an R slot, instantiated per
   receiver type in the member (`(inst minvN (* strings/Builder))`; gc shares the code of pointer
   receivers' instances by shape). So 288 method signatures, not one per method.
3. **A method is listed once**, as its declared receiver gives it (§6.1): a value-receiver
   method as `(method-expr T M)`, a pointer-receiver one as `(method-expr (* T) M)`, promoted
   methods (through embedded fields, Go's depth rules and ambiguity) by the method set's
   rule (a pointer method promoted through a pointer embedding is in T's set). The var is
   `T.M` either way (§2.1); from Arbace a pointer to T given for a value receiver is
   dereferenced (Go's implicit `*`).
4. **The digest** (§6.5) is not one program resource but one file per configuration,
   embedded by the table's file of that configuration (only the configuration's own is linked;
   inflated by `goi.Digest` on first use): its first line indexes the packages' texts (`{"strings"
   [start end] ...}`, offsets in characters after the line), each package `in-ns`,
   `go/package` (`:path`, `:config`, `:files`), then **one `go/file` per source file** (its
   imports, pruned to the ones used, so that no import names clash) and its kept declarations.
   Constants are one form each, without init: `:val`, `:tag` (the source's, else the type the
   stand-in computes: `Microsecond`'s `Duration`), and `:go/underlying` (the basic kind, for
   consumers that need no type resolution). A variable without `:tag` keeps its initializer,
   marked `:go/digest-init`, so that a checker can type it. Doc strings are kept (they make
   `doc` work on the table's vars; 212 KB of the compressed digest).
5. **The declaration layer is a stand-in in `arbace.go.table`** (GR2 builds the checker in
   parallel, `arbace/go/check*`): it resolves type forms through files' imports and aliases,
   finds underlying types and slot kinds, method sets with promotion (through the listed
   packages' types: no embedding was left unresolved), the types of constants (a small typer:
   literals, names, `conv`, operators), and the build constraints of the tamago tree (`:build`
   lines and file name suffixes, gc's tags: GOOS, GOARCH, `unix`, `gc`, `go1.N`, the
   architecture level; cgo, `purego` and experiments off). A signature with a type the table
   cannot name (internal, unexported, a generic instance whose kind the stand-in cannot tell:
   `iter.Seq[string]`) gets no invoker. GR2's checker is its successor (GC14).
6. **Go values whose `==` can panic are boxed** (§5.1, §5.5): Arbace compares references with
   Go's `==` on `any` (`identical?`, the fast path of `=`, the collections), which panics on a
   slice, a map, a func, or a struct or array holding one or an interface. Such a value reaches
   Arbace as a `*goi.Boxed` around it: still of the class `GoValue`, printed, compared and hashed
   as the value, unwrapped whenever it goes back to Go; two crossings of one slice are `=` (same
   array) but not `identical?`.
7. **Interface contents of a named type with methods stay Go values** (§5.1's "a Go scalar or
   string converted as its type"): `hex.DecodeString`'s error `hex.InvalidByteError(120)` would
   otherwise reach Arbace as `120`; it stays the error (GC10).
8. **nil** (§5.1): a nil pointer, map, slice, channel, func or `unsafe.Pointer` reaches Arbace
   as `nil`, and `nil` goes back as the parameter's zero value.
9. **BigInt and maps cross by GoInterp** (goi cannot make or read `arbace.lang`'s objects,
   §1.3): a `BigInt` or `BigInteger` argument goes to goi as its decimal text, a uint64 of 2^63 or
   more comes back as a marker GoInterp makes a BigInt; a map of keywords given for a struct (a
   parameter or a `GoType`'s call, §7.1) goes as its names and values; a Go func comes back as a
   marker GoInterp wraps in a GoFn (its calls by the invoker of its type when a table function
   has it, else reflect). An Arbace fn for a func parameter is GR5's (§7.5).
10. **The natives** (§4.11) are GR1's: `init0`, `packagePath0`, `paths0`, `members0` (a
    package's members as `[kind name handle ...]`), `call0` (`[0 results...]`, `[1 message type
    value]` for a panic, `[2 status]` for `os.Exit`), `load0`, `store0`, `typeOf0`, `typeName0`,
    `convert0`, `zero0`, `new0`, `structArg0`, `bigArg0`, `spread0`, `marker0`, `bigText0`,
    `funcCallable0`, `funcValue0`, `goString0`, `str0`, `digest0`, `complex0`. GR3's (`node`,
    `func`, `defineType` ...) come with the builder. Natives skip j2g's class initialization
    guard, so GoInterp's public methods call them and its static initializer gives jrt
    `GoValueClass`.
11. **Panics leaving Go at GR1** (§10.4): a `GoPanic` with gc's text after `panic: ` (an
    `error`'s `Error()`, a `Stringer`'s `String()`, a string, else `fmt.Sprint`), `ex-data`
    `{:go/value v :go/type "T"}`; no `:go/trace` and no cause yet (GR5). A Java exception thrown
    inside (an argument's conversion) goes on as it is. `os.Exit` throws a `GoExit`
    (`{:go/status n}`, `.status`), the deferred calls run (S12).
12. **Namespaces on demand** (S9): `Namespace` holds a resolver that makes a var when it is
    first resolved (`findInternedVar`, `getMapping`: `strings/ToUpper`, `resolve`, `find-var`)
    and all of them when the mappings are enumerated (`ns-publics`, `refer`), which drops it.
    Var metadata: `:go/kind` (`:func`, `:method`, `:var`, `:type`, `:const`), `:go/package`,
    `:arglists` and `:doc` from the digest.
13. **`GoValue`** (§5.5): `(instance? arbace.lang.GoValue v)` and `(class v)` as specified;
    `(cast arbace.lang.GoValue v)` is not special (no instance of the class exists).
14. **The box**: tamago/amd64 and tamago/arm64 have their tables from `tamago/goroot`'s forms
    (§6.3); `runtime.GOOS` is `"tamago"` there. The box's smoke test does not run
    `test/goi/calls.ae` (GR1's test is the native executable's).

**Proposals for the user** (each built as recommended):

- **GC10 Named scalars with methods in interfaces.** (a) as built, they stay Go values
  (amendment 7); (b) §5.1's rule as written, converted by their kind. *Recommended: (a)*.
- **GC11 The invokers' threshold.** (a) `:min-sharing 2` (860 KB on linux/amd64); (b) 3
  (668 KB); (c) every signature again once GR3's interpreter calls compiled code in loops and
  needs them. *Recommended: (a)* now, revisited at GR3 with the check's profile.
- **GC12 Docs in the digest.** (a) kept (212 KB of the compressed digest per executable;
  `doc` on the table's vars); (b) dropped (`bin/goi table --no-docs`). *Recommended: (a)*.
- **GC13 Boxing.** (a) as built, values whose `==` can panic boxed (amendment 6); (b) j2g
  translating reference `==` on `any` through a safe comparison (every `identical?` slower).
  *Recommended: (a)*.
- **GC14 The stand-in's end.** (a) at GR2's merge the generator calls `arbace.go.check`'s
  declaration layer and the stand-in goes; (b) the stand-in stays as the table's own (the
  checker reads the digest it writes). *Recommended: (a)*.

## As built: GR2 (2026-10-17)

GR2, the checker, was built first, before GR1 (the user's choice of 2026-10-17), on the tools'
host. What this text says of the checker (§3) holds, with the choices and findings below.

**What was built.** `arbace.go.check` (`arbace/go/check.ae`) and its parts in
`arbace/go/check/`: `types.ae` (types, predicates, gc's sizes, field and method lookup after
`lookup.go`), `const.ae` (constants, §3.5: exact rationals, big.Float's 512 bits beyond
`maxExp`, float32 and float64 rounding), `env.ae` (objects, scopes, the universe, `unsafe`,
errors, the annotations verified), `code.ae` with `code_expr.ae`, `code_call.ae`,
`code_stmt.ae`, `code_branch.ae` (type expressions, expressions, calls and builtins,
statements, labels and branches), `decl.ae` (a package's declarations: collection, `objDecl`
with go/types' colouring, lazy named types, methods, `validType`, initialization order after
`initorder.go`) and `load.ae` (forms roots, build constraints and file names evaluated for the
configuration, import paths): 5,941 lines. `bin/goi annotations` (§11.4) and `bin/goi test`
(`test/goi/check_test.ae`: `test/goi/errors/`, 9 files of invalid forms with 61 messages;
`test/goi/annotations/`, converted forms whose `:val`, `:tag`, `:go/via`, `^:assign` and
`:init-order` are wrong, the checker's disagreements (S16); `test/goi/checked/`, three programs
and their checked trees; `--gc` prints the invalid forms with repr/go, builds them with gc and
requires gc's messages to be the expected ones: `test/goi/gc_errors.ae`); `make
goi-annotations`, `make goi-test`; `bin/gate --full` runs both after repr-go's round trips
(§11.5), on their fresh conversions.

**Imports from forms, not the digest.** Before GR1 there is no digest (§6.5): an imported
package is checked from its forms, whole (bodies included), once per session and in parallel
(each package's imports first), in a forms root: `tamago/goroot` (with `arch/ARCH/` laid over
it file by file; the merged tree's `:build` lines and file names evaluated for
`tamago/ARCH`), or repr/go's conversions of the linux corpora (`.tmp/repr-go/forms/CORPUS`;
the programs and directory tests import `linux-amd64`'s std; a directory test's packages by
their `:path` and `:local-imports`). The digest is Go forms too (S55), so GR1 adds it as one
more root of the same reader (GC1).

**Not in the program yet.** `arbace/j2g/embed.ae` leaves `arbace/go.ae` and `arbace/go/` out of
the embedded sources, as repr/haskell's: the checker runs evaluated from the checkout on stage
0, and the program changes only by that list's text. The program carries goi from GR1/GR3, after
the coming re-seed.

**The corpora** (`bin/goi annotations`, 24 threads, shared machine): zero errors in every
package, every annotation of a declaration without generics verified, the counts recorded in
`test/goi/annotations-CORPUS.edn`:

| corpus | packages | declarations checked | using generics (apart) | `:val` | `:tag` | `:go/via` | `^:assign` | `conv` | `:init-order` | time |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|---:|
| `tamago-amd64` | 371 | 34,520 | 1,263 | 9,021 | 243 | 3,844 | 671 | 21,118 | not written (merged tree) | 45 s |
| `tamago-arm64` | 370 | 34,437 | 1,263 | 8,926 | 244 | 3,831 | 671 | 21,099 | not written | 41 s |
| `linux-amd64` (std) | 376 | 37,072 | 1,294 | 10,905 | 256 | 3,779 | 702 | 22,642 | 376 packages, 2,560 initializers | 45 s |
| `linux-test` (`$GOROOT/test`) | 1,713 | 8,480 | 1,290 | 627 | 45 | 143 | 36 | 3,080 | 1,713 packages, 932 initializers | 29 s |
| `linux-dirtest` (directory tests) | 562 | 1,228 | 498 | 27 | 0 | 12 | 0 | 147 | 562 packages, 132 initializers | 23 s |

A declaration is a spec: a function, a method, a type, a variable or a constant (a group's
specs one each). The survey's 44,435 `:tag` (§2.1) counted every `:tag` of `tamago/goroot`,
declarations included; the converter writes it on expressions only for non-constant shifts
(SPEC §8.3): 269 there, 243 of them in declarations without generics. `:inst` occurs only in
declarations that use generics (1,061 in `tamago/goroot`), so none is verified before GR6.

**Generics (S53) as built.** A declaration uses generics when checking it meets a generic
declaration, an instance, a type parameter or `:inst`. Such a declaration is checked all the
same as far as its types go: instances are computed by substitution (with the converter's
`:inst` for inferred type arguments: the one place an annotation stands in for the checker,
only in declarations counted apart), so its dependents (a variable `var h = sync.OnceValue(f)`,
a struct with an `atomic.Pointer[T]` field) have their right types, and its references, the
imports it uses and its dependencies for initialization order are recorded; what GR2 does not
check is marked unsupported, the operand invalid, and checking goes on. Its errors are counted
apart (`:generic-errors`), never as errors. The unsupported constructs met in `tamago-amd64`:
selectors (27), `len` (35), `range` (15), indexing (4), slicing (5), `append` (1) and composite
literals (2) of type parameters, and type set terms (7).

**Choices and findings.**

- **gc's branch checks.** Labels, `goto`, `break`, `continue` and `fallthrough` are checked as
  gc checks them before types2 (`cmd/compile/internal/syntax/branches.go`), with its messages
  (`label L defined and not used`, `goto L jumps over declaration of x at FILE:LINE`, `goto L
  jumps into block starting at ...`); go/types' `labels.go` words some of them otherwise.
  Messages elsewhere are types2's (§3.8), checked against gc by `bin/goi test --gc`.
- **Shift counts stay untyped**, as go/types leaves them (an untyped non-constant count is
  integer and is not converted): `(<< x (<< 1 s))`'s count has no `:tag`. The `:tag` check found
  the checker's first answer (the default type) wrong.
- **Huge float literals** are big.Float's: its decimal scanning (math/big `floatconv.go`: the
  mantissa exact, times 2^k, times or divided by 5^|k| computed at 576 bits, `pow5`) is followed
  step by step, so `1E+646456992M` and its quotients have go/constant's `:val` exactly
  (`fixedbugs/issue11326b.go`).
- **A `let` that is not the last form of its statement list** is a block of its own, as the
  printer prints it (SPEC §7.3 has every `let` last).
- **The converter's typed float values were lossy**: `double-lit` converted a ratio with
  `double`, which rounds through 16 decimal digits, so `const _ float32 = real(2.1)` had `:val
  2.099999904632568` instead of float32(2.1)'s `2.0999999046325684` (`fixedbugs/issue11945.go`;
  S16: "both are bugs"). Fixed in `arbace/repr/go/lit.ae` (the ratio divided exactly); no
  tracked forms change: `tamago/goroot`'s typed float values are all verified exact, and those of
  `tamago/cmd`, `box/` and `go/` (π, e, 2π, 2e9) read the same either way (GC2).
- **Names spelled `true`, `false` or `nil`** (a directory test's package-level variables,
  `fixedbugs/issue4252.dir`) were written bare in `:init-order`, which the reader reads as the
  literals. Since GC3 the converter writes such a name `(go/id "true")` wherever it would
  otherwise write the bare symbol (SPEC §4.4): `:init-order` entries, `:go/via` names, the
  names of `:inst` and `:tag` type forms (types, type parameters, fields, interface methods),
  import names and package names; the printer and the checker read that spelling, and the
  checker no longer takes a literal in `:init-order` for a variable
  (`test/goi/annotations/literals.ae`; `test/repr/go/convert/units/s4_names.go`). No tracked
  forms change (`bin/tamago-vendor verify`, `bin/toolchain-vendor verify`, `bin/x509roots-vendor
  verify`: byte for byte). Blank variables' entries (`["file.go" line col]`) are compared by file
  and line: mode `:lines` keeps no columns.
- **Method expressions carry no `:go/via`** in the converter's forms (SPEC §7.6 asks it of
  selectors and literal keys): the checker computes the path and compares where present.
- **Errors**: a package's errors are all reported, as data `{:file :line :msg :code}` (§3.8);
  the REPL's `CompilerException` with at most ten of them, and the check of a single REPL
  declaration against the package's state, come with GR3.

**Decided (2026-10-17, the user).** GC1 (a): imported packages from their forms when a forms
root has them, the digest one more root at GR1, as built. GC2 (a): repr/go's `double-lit`
exact, with GR2, as built (its tests' expected value corrected: `float32(0.1)` is
`0.10000000149011612`). GC3 (a), now rather than later: the converter writes `(go/id "...")`
for these names everywhere a bare symbol would read as a literal, the printer and the checker
read it, and the checker accepts no literal there (above).
