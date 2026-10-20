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
  keep, SPEC.md §10.1). As built (GR5.1, GC26 (b)): only a statement that can panic or call
  stores it; one that can do neither leaves its predecessor's line, which no trace sees. A panic leaves the chain as it was (§4.3), so it is the panic's trace.
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
| the generator | `arbace/go/table.ae` (`arbace.go.table`), `bin/goi table` | on the tools' host, before j2g in `bin/arbace-go`'s `build` (kept when its inputs are unchanged: `DIR/key`), on goi's checker since the merge with GR2 (amendment 5): 155 s for the four configurations on stage 0, linux's std converted once by `bin/repr-go read` and cached (`.tmp/goi-table/KEY/linux-ARCH`, KEY repr/go's, godump's and Go's hashes: S44) |
| the table | `target/arbace-go/gotable/` → j2g `--gotable DIR` | the package `arbace/gotable`, imported (blank) by the main package; for linux/amd64: 829 functions, 347 variables, 244 types, 997 methods; 782 distinct signatures (functions' and methods' without the receiver), 235 typed invokers (233 with the stand-in) |
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
| the executable after GR1, before GC14 (the generator `arbace.go.table` embedded and compiled) | 110,555,296 (+5,709,824) |
| of which the table, all invokers (the first build: one per signature, 763) | 4,247,552 |
| of which the invokers in it | 1,630,208 |
| the table with `:min-sharing 2` (233 invokers), before GC14 | 3,477,504 |
| of which the invokers in it | 860,160 |
| **the executable as built, since GC14** (the generator and the checker not embedded: they run on the tools' host) | **108,822,688 (+3,977,216, 3.8%)** |
| of which the table (235 invokers) | 3,485,696 |
| of which the invokers in it | 864,256 |
| goi, GoInterp's classes and `arbace.go` (the executable without the table, but for its image) | 491,520 |

The invokers of every signature cost 1.63 MB, above GS4's 1 MB: so GS4 (b), the threshold
variant, as decided: a typed invoker for each signature at least two members share (in one
configuration), 235 of 782 (233 before GC14); the other members (most of them methods of one type) are called by
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
5. **The declaration layer is goi's checker's** (GC14, decided: at GR2's merge). Built first
   as a stand-in in `arbace.go.table` while GR2 was being built in parallel, it was replaced at
   the merge by `arbace.go.check`'s: the configuration's forms read and their build constraints
   evaluated by `arbace.go.check.load` (`package-source`: the root, tamago's `arch/ARCH/` laid
   over it, `:build` lines and file names, gc's tags with go1.27.1's default experiments), the
   listed packages checked with their imports in one session per configuration (so linux's whole
   std is converted, `bin/repr-go read ... std`, cached under `.tmp/goi-table/`), and their
   objects' types, method sets (the checker's `lookup`: Go's depth rules, ambiguity, pointer
   receivers, promotion through embedded fields) and constants' types the checker's. The
   generator itself still reads syntax: which declaration forms the digest keeps and their
   bodies stripped. A signature with a type the table cannot name (internal, unexported, a
   generic type's name, an invalid type) gets no invoker. **The table as the checker makes it,
   against the stand-in's** (all four configurations, the same members and signatures: 829
   functions, 347 variables, 244 types, 997 methods and 782 signatures on linux/amd64): (a) more
   signatures get an invoker (233 → 235): the stand-in could not tell the kind of a type of
   a package it had not read (`hash.Hash` of `md5.New` and the other hashes' `New`,
   `syscall.RawConn` of the `SyscallConn` methods), the checker reads every import, and so every
   later invoker's number moves (`invN`, `minvN`): the other differences of the package files are
   these numbers only; (b) the digest: constants whose type the stand-in's typer could not
   compute (`compress/zlib`'s levels, defined by `compress/flate`'s, `runtime`'s from
   `internal/...`) lost their `:go/digest-init` initializer, the checker giving their type; and
   `runtime`'s `mgcmark_greenteagc.go` is in it (the `goexperiment.greenteagc` tag is set by
   default in go1.27.1, which the stand-in had off: the checker's tags are gc's). The generator
   takes 155 s for the four configurations on stage 0, std's conversion cached (the stand-in 31 s).
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

**Decided (2026-10-17, the user).** GC10 (a), GC11 (a), GC12 (a), GC13 (a), as built; GC14 (a),
now, at GR2's merge: the stand-in removed, the generator on `arbace.go.check` (amendment 5).
Since the merge, j2g's embedded sources leave out exactly the build's tools of `arbace/go/`, the
checker (`arbace/go/check.ae`, `arbace/go/check/`) and the table's generator
(`arbace/go/table.ae`), which run evaluated from the checkout on the tools' host; `arbace.go`
(`arbace/go.ae`), which the program runs, is embedded (GR2's list had left out all of
`arbace/go.ae` and `arbace/go/`).

**Proposals for the user** (each built as recommended; decided above):

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

## As built: GR3 (2026-10-18)

GR3, the interpreter's core, is built on branch `goi-gr3` (from main `80294e9a`, seed `910b4b8f`).
Where building it found otherwise than this text, the text below is the record; proposals for the
user are GC15-GC18.

**What exists.**

| part | where | as built |
|---|---|---|
| the macros | `arbace/go.ae` | `go/package`, `go/file`, `go/func`, `go/method`, `go/type`, `go/var`, `go/const`, `go/directive`, `go/import`, `go/eval`; the functions `run-main`, `check` (with GR1's `new`, `zero`, `str`, `spread`, `assign!`). Each macro hands its form, quoted, to `arbace.go.repl`, required on first use (`requiring-resolve`) |
| the REPL's state | `arbace/go/repl.ae` (`arbace.go.repl`) | packages by import path (atoms: files, declarations, cells, generations), checking against the digest and the interpreted packages, `define!`, loading (§2.3), declaring at the REPL (§2.5), `go/eval`, `go/run-main`, `go/check` |
| the builder | `arbace/go/build.ae` (`arbace.go.build`) | the checked tree to nodes: descriptors, slots, cells, jump codes, calls (§4.1) |
| program mode | `arbace/go/run.ae` (`arbace.go.run`), `bin/goi run` | §2.7: checked, built eagerly, initialized, run; `goi: unsupported: ...` (status 125), `goi: error: ...` (126) |
| the back end | `go/arbace/goi/{frame,func,stmt,expr,types}.ae` | threads and frames, functions and calls, closures, defer, panic, recover, the program's runner and gc's report; statements; expressions, builtins, conversions; descriptors, RT, loads and stores |
| generated | `go/arbace/goi/{ops,registry}.ae` by `test/goi/gen_nodes.ae` | 387 operator factories (per operator and type: §4.2, S19); the registry of the 588 factories the builder calls by name |
| the natives | `arbace/lang/GoInterp.ae` | GR1's and `op` (amendment 1) |
| jrt | `go/arbace/jrt/thread.ae` | `Thread.Interp`, the goroutine's goi thread (S22) |
| the core | `arbace/lang/RT.ae`, `arbace/core.ae`, `arbace/lang/Compiler.ae` | VENDOR-NOTES.md, hand change 27: the after-load hooks (S6), `toolSource` takes `arbace/go/`, S4's alias |
| embedded | `arbace/j2g/embed.ae` | the checker, the builder, the REPL and the runner are embedded and compiled (c2j); the table's generator stays a tool of the build |
| the check | `bin/goi check`, `test/goi/check-linux-amd64.edn`, `test/goi/check-known.edn`, `test/goi/essential.txt` | §11 (below) |
| tests | `test/goi/run/go/t/*.ae` (10 programs and gc's outputs: `.out`, `.err`, `.exit`), `test/goi/repl.ae` (33 checks; its package in `test/goi/repl-pkgs/`) | in `bin/goi test` (`--gc`: the expected outputs are gc's run of the printed forms; `--regen`) and `bin/arbace-go --smoke` |
| the gate | `bin/gate` | the essential gate runs `bin/goi check --essential` (GS12); `--full` adds `bin/goi test --gc` and `bin/goi check` on the gate's executable |

**The check (§11, the GR3 exit criterion).** Over the 1,051 programs of `$GOROOT/test` whose action
is `run` (repr/go's corpus `linux-test`; gc's references are the forms printed by repr/go, built
by gc and run, cached in `.tmp/goi-check/ref/`), recorded in `test/goi/check-linux-amd64.edn`:

| result | programs |
|---|---:|
| pass | 532 |
| fail | 0 |
| error | 0 |
| known (`test/goi/check-known.edn`, each with its reason) | 24 |
| unsupported: GR4's (methods, interface methods, type switches and assertions, goroutines, channels, select, method values and expressions, range over functions) | 280 |
| unsupported: GR6's (generics: every declaration that uses them) | 164 |
| unsupported: an import neither listed nor interpreted (syscall, regexp, testing, text/tabwriter, runtime/trace, math/big, maps, iter, html/template, runtime/pprof) | 29 |
| unsupported: recursive types reflect cannot make (amendment 3) | 6 |
| unsupported: a type the table does not hold (GR5's growth: `encoding/binary`'s unexported `bigEndian`, `hash.Hash` of an unlisted package) | 3 |
| skipped: the action asks for gc's flags (as the round trip leaves them out) | 13 |

So every program within GR3's subset passes or is known, and the rest is unsupported with its
reason. The known differences are by design: `runtime.Caller`, `Callers` and `FuncForPC` name
goi's nodes (12 programs), the collector's and the compiler's own behaviour (stack objects'
liveness, finalizers: 3), an interpreter's speed beyond the check's 10 s (fib-like recursion of
hundreds of millions of calls: 4), unsafe arithmetic over gc's layout (2), `reflect` seeing a
proxy (§5.3: 1), programs that run the go command (2). The whole check takes 37 s with 16 jobs
(gc's references cached; their first build about 4 minutes), the essential subset, 266 programs
(the passing ones outside `fixedbugs/` and every third of `fixedbugs/`), 17 s with 8 jobs.

**Measurements (GR3's criterion: within twice X2's sketch).** `test/goi/run/go/t/fib.ae`, run by
`bin/goi run test/goi/run go.t.fib bench` (best of 5, amd64, the machine shared), through the real
checker and builder:

| | goi | X2's sketch | criterion | gc |
|---|---:|---:|---:|---:|
| fib 22 | 2.41-2.54 ms | 2.87 ms | 5.74 ms | 0.23 ms |
| sumSq 1e6 | 19.1-20.2 ms | 22 ms | 44 ms | 0.33 ms |
| sieve 1e6 | 66 ms | 55 ms (the survey's sketch) | | 2.6-2.9 ms |

The call path: a static call loads the callee's code from its cell (S27), takes the next frame of
the thread (frames are reused by depth) and its windows of the thread's stacks before the
arguments are evaluated (amendment 2), and restores the stacks' tops after the body. The builder
picks the variants of a local and a constant operand (`n - 1`, `n < 2` read the slot directly:
`SubLK64`, `LtLKI`). A run of a small program costs about 0.5 s, mostly loading the checker and
checking the digest's packages the program imports.

**The executable (GS4, GS5).** 119,775,392 bytes before GR3 (main `80294e9a`), 129,949,856 after
(+10,174,464, 8.5%): almost all of it the compiled checker, builder and REPL (c2j's Go of their 8,300
lines of Arbace: GC15) and the back end; the table grows by 90,112 bytes to 3,575,808
(`bin/goi table --size`: GR1's 3,485,696) with GR3's three packages (amendment 13), its invokers
to 905,216.

**Amendments (as built).**

1. **The natives** (§4.11): one native, `GoInterp.op(name, args)`, makes every node, descriptor
   and function by a factory's name (`registry.ae`), its arguments adapted (Longs, Strings,
   Booleans, Doubles, byte and Object arrays, nodes as Go values); scalar results come back as
   Java objects. The spec's `node`, `func`, `setCode`, `defineType`, `newVar`, `call` are factories
   (`FuncCell`, `SetCode`, `TNamed`/`TSetUnder`, `NewGlobal`, `CallInterp` ...); an interpreted
   function's `GoFn` is GR1's over a `Callable` that calls it (§7.1).
2. **Frames** (§4.3): the thread keeps a frame per depth, reused; a call takes the next depth and
   its windows (only of the kinds the callee uses) before evaluating the arguments, so that calls
   in them take frames above it, and restores the thread's depth and tops after a normal return
   (a panic leaves them; the frame that recovers restores its own). The depth limit is checked
   only when the frames grow. `Frame.Line` is not kept yet (traces' lines are GR5's).
3. **Two representations** (§5.1, §5.2): a value is held as Go memory holds it, but for two kinds
   of types: a function type, whose frame value may be an interpreted closure (in Go memory a
   `reflect.MakeFunc` adapter of its type, made once and mapped back to the closure: GR5's typed
   adapters come later), and a pointer to a recursive named type, erased in Go memory to an
   `unsafe.Pointer` inside composites and variables (reflect has no recursive types: `type node
   struct{v int; next *node}` and `[]*node` work, their %v gc's). Recursion through a slice, an
   array, a map or a function (`type F func() F`, `type S []S`) and recursive named pointer types
   are unsupported.
4. **Calls of several results** write the caller's temporaries (S28); a deferred call (§4.7) runs
   with a frame of its own holding its evaluated arguments and its temporaries.
5. **Assignments** (§4.5): the places' pointer, slice and index operands are evaluated into
   temporaries first, then the values, then each assignment with Go's nil and bounds checks
   (`fixedbugs/issue23017.go`); a map element assigned by a range is assigned at each iteration.
6. **Runtime errors** (§10.3) are made by the operation gc would make wherever goi can: bounds
   by indexing and slicing a slice of empty structs of the right length and capacity, nil
   dereferences by a real one, division by Go's division at the type; `goi.RuntimeError` only for
   `makeslice`, `makemap`, the conversion of a slice to an array and unsafe's builtins.
7. **Program mode's report** (§10.7): gc's `printpanics` over goi's chain of replaced panics
   (`[recovered]`, `[recovered, repanicked]`), the signal line of a nil dereference, then
   `goroutine 1 [running]:` and the interpreted frames' names. `print` and `println` are Go's own
   builtins at the operand's kind (S24).
8. **Interfaces** (§5.3, GR3's part): an interface's content is the value of its Go type, or a
   `goi.Proxy` (its descriptor and value, `fmt.Formatter` delegating to the value) for a named type
   without an RT of its own; no method calls on interfaces (GR4). Methods of compiled types are
   called through the table's method expressions (§7.4), the receiver an R (its address for a
   pointer method on an addressable value).
9. **The REPL** (§2.5): a declaration checks the whole package with it in place of its names'
   earlier declarations; its own errors reject it, nothing changed; a dependent that no longer
   checks keeps its definition, with a warning naming it (`goi: warning: ...`). A function keeps
   its cell while its signature is the same, else gets a new one; every function's code is built
   again on its next call. A variable defined again runs its initializer (S10, S11). Unused
   imports are not errors at the REPL (GC16). Check errors are an `ExceptionInfo` with
   `:go/errors` (data, §3.8), not a `CompilerException`; their file is the forms' source, `REPL`
   for typed forms.
10. **`go/eval`** (§2.4): the statements become `(go/func goi_evalN ^any [] ...)` in the package
    (`:results [any ...]` for a call of several results, no result for a statement, looking
    through `let` bodies), checked, built, called; the value goes to Arbace by its dynamic type.
11. **The digest's constants** (§6.5, GR2's checker): a constant of the digest has its `:val` and
    no init; the checker gives it its `:tag` (else the untyped kind its `:val`'s representation
    says) and that value, trusted. Without it every constant of a listed package was invalid.
12. **The checked tree's result** (§3.7): `check-package` with `:keep-trees` also returns the
    declarations (`:decl-infos`: their forms, objects, trees, generics' marks), the initializers'
    order (`:order`) and the `init` functions (`:inits`), which the REPL and the runner define from.
13. **The listed set** (§6.6, GS5): `io/ioutil`, `log` and `math/rand`, which 28 programs of the
    check imported (+90,112 bytes). The check's other missing imports are packages goi could only
    interpret with GR4 (methods) or not at all (syscall, testing, runtime/trace).
14. **Generics** (§9): a declaration that uses generics (a generic declaration, an instance, a
    type parameter, `:inst`) is not built: its program is unsupported (GR6), as the annotation
    check counts it apart (S53).

**Proposals for the user** (each built as recommended):

- **GC15 The checker in the program.** The REPL needs the checker (§3.1), which is about 6,000
  lines of Arbace; compiled by c2j with the builder and the REPL it costs about 10 MB of the
  executable. (a) as built, compiled into the program as the other embedded namespaces are; (b)
  embedded as source and evaluated on first use (the executable smaller by most of that, the
  first check of a session seconds slower). *Recommended: (a)*: a REPL whose first Go form takes
  seconds is not the REPL the survey asked for; revisit with the program's size budget.
- **GC16 Unused imports at the REPL.** (a) as built, not errors at the REPL (the REPL's file
  collects imports for declarations still to come; package loads report them as gc does); (b)
  gc's error everywhere. *Recommended: (a)*.
- **GC17 The listed set's growth.** (a) as built, `io/ioutil`, `log` and `math/rand` (+90 KB),
  asked for by the check; (b) GR1's 41 only. *Recommended: (a)*.
- **GC18 The check in the essential gate.** (a) as built, the 266 programs of
  `test/goi/essential.txt` (17 s with 8 jobs); (b) the whole check (37 s with 16 jobs, gc's
  references cached); (c) `--full` only. *Recommended: (a)*, GS12's subset; (b) costs little more
  and may replace it once GR4 grows the passing set.

## As built: GR4 (2026-10-18)

GR4, methods, interfaces and concurrency, is built on branch `goi-gr4` (from main `7fb90fc8`, which
merges GR3, seed `910b4b8f`). Where building it found otherwise than this text, the text below is
the record; proposals for the user are GC19-GC22.

**What exists.**

| part | where | as built |
|---|---|---|
| methods and interfaces at run time | `go/arbace/goi/method.ae` | method sets (`TSetMethods`), interfaces' methods (`TSetIfaceMethods`), dispatch with an inline cache per call site (`CallIface*`), compiled values' methods (`CompiledDispatch*`, `ErrorCall`), method values (`Bound`: `MethodValue`, `IfaceMethodValue`, `BoundCompiled`), type assertions and switches with gc's messages (`TypeAssertionError`), fmt's handling of proxies (`Proxy.Format`), the wrappers of `error` and `fmt.Stringer`, `CallProxy` and `RegisterWrapper` for the table's wrappers |
| goroutines, channels, select, range over functions | `go/arbace/goi/chan.ae` | `GoStmt`, channel operations by reflect, `Select` by `reflect.Select`, `RangeChan`, `RangeFunc` (§4.10) |
| descriptors | `go/arbace/goi/types.ae` | interned (amendment 3); wrappers in and out of Go memory (§5.3 rule 3) |
| the builder | `arbace/go/build.ae` | methods and their receivers, promotion through embedded fields, method sets and their thunks, interface calls, method values and expressions, assertions, type switches, `go`, send, receive, `select`, `close`, `make` of channels, range over channels and functions |
| the REPL's state | `arbace/go/repl.ae` | a cell per method `T.M` or `(*T).M` (gc's names), kept while its signature is, built on first call or eagerly as the functions |
| program runs | `arbace/go/run.ae` | a directory test's packages loaded, checked and defined in dependency order, initialized in Go's order (amendment 12); assembly and `go:linkname` reported unsupported (§1.1) |
| the table | `arbace/go/table.ae` | the wrappers of the listed interfaces (§6.4): 63 for linux/amd64 |
| the check | `bin/goi check` | the directory tests (rundir, buildrundir, runindir: repr/go's corpus `linux-dirtest`), amendment 12 |
| tests | `test/goi/run/go/t/{methods,ifaces,chans}.ae` | 13 programs in `bin/goi test` (GR3's 10 and these three, gc's outputs recorded) |

**The check (§11, the GR4 exit criterion).** Over the 1,051 run programs of `$GOROOT/test` and the
127 directory tests of `linux-dirtest`, recorded in `test/goi/check-linux-amd64.edn`:

| result | programs (directory tests among them) |
|---|---:|
| pass | 832 (58) |
| fail, error | 0 |
| known (`test/goi/check-known.edn`, each with its reason) | 51 (2) |
| unsupported: GR6's (generics) | 217 |
| unsupported: an import neither listed nor interpreted (syscall, regexp, testing, runtime/trace, math/big, ...) | 30 |
| unsupported: reflect's method lookups, not in the table (S39), and types the table does not hold | 17 |
| unsupported: assembly, `go:linkname` (§1.1) | 9 |
| unsupported: GR3's recursive types (its amendment 3) | 7 |
| skipped: the action asks for gc's flags | 15 |

Of the 280 programs GR3 left unsupported for GR4, 241 pass, 26 are known and 13 are unsupported
for the reasons above (none GR4's); no program passing at GR3 regresses. The new known
differences are by design: `runtime.Caller`, `Callers` and the traces of panics (7 programs), the
collector's and the compiler's own behaviour (finalizers' timing, allocations, `uintptrescapes`,
profiling: 10), an interpreter's speed beyond the check's 10 s (2: 41 s and 32 s), and what
compiled `reflect` and the runtime see of interpreted types (§5.2, §5.3, G8: 9). The whole check
takes 38-46 s with 16 jobs. The essential subset (`test/goi/essential.txt`, GC21) is 376 programs:
the passing ones outside `fixedbugs/` (directory tests among them) and every fourth passing one of
`fixedbugs/`, 23-30 s with 8 jobs on the shared machine.

**Measurements.** GR3's figures are unchanged (`bin/goi run test/goi/run go.t.fib bench`, best of
three, amd64, the machine shared): fib 22 2.46 ms, sumSq 1e6 19.9 ms, sieve 1e6 62 ms (GR3: 2.41-2.54,
19.1-20.2, 66). The executable is 132,137,120 bytes against 130,883,744 built from main `7fb90fc8`
(+1,253,376, 1.0%: the back end's new files and the table's wrappers).

**Amendments (as built).**

1. **Receivers** (§4.6): a method's receiver is laid after its parameters and results, so the
   arguments of a call through an interface (§5.3) take the signature's slots whatever the
   dynamic type's receiver kind; a static call writes the receiver last but evaluates it first.
2. **Method sets** (§5.3): when a type goes into an interface (`iface-content`), and when it is an
   assertion's or a type switch's target, the builder gives its descriptor its method set once
   per build context: for each method of the set (go/types' lookup with the checker's objects) its
   key (the name, `path.name` unexported), its function and the receiver's slot. The function is
   the method's own cell when the receiver is the type itself, else a **thunk** the builder makes
   from a checked tree of its own (a method of receiver D calling the method through its path of
   embedded fields, pointers and embedded interfaces), built eagerly in program mode. Compiled
   types never get one; an entry whose signature names a type the digest does not hold is left
   out.
3. **Descriptors are interned** (§5.2): one per type (composites by their parts' descriptors, named
   types by path, name and generation, local types by a counter shared by every build), so that
   type identity is descriptor identity across build contexts and packages (a proxy made in one
   package, asserted in another).
4. **Proxies** (§5.3 rule 2): a value of a type with methods goes into an interface as its proxy;
   an unfaithful composite (`[]Celsius`) does not (GR3's representation: compiled `reflect` sees
   its RT). The content of a proxy whose type has `Error() string` is its `error` wrapper, else with
   `String() string` its `fmt.Stringer` wrapper (`contentOf`), so that compiled code given it as
   an `any` sees an error or a Stringer (`fmt.Errorf`'s `%w`, `errors.Is`); every operation finds
   the proxy through its wrapper (`proxyOf`).
5. **Raw values** (GC20): runtime's and reflect's compiled functions are given a proxy's value
   itself for their empty-interface parameters (`runtime.SetFinalizer`, `reflect.ValueOf`), and a
   raw value of an interpreted faithful type with methods that compiled code gives back (a
   `reflect.Value`'s `Interface`) is dispatched and asserted through the registry by its RT.
6. **Interface calls** (§5.3): the content is evaluated into a temporary; a proxy calls its type's
   entry (an inline cache keyed by the descriptor); any other content calls the compiled method:
   the static compiled interface's method expression from the table, `error`'s `Error`, or for an
   interpreted interface the table's method of the dynamic type, found by its reflect type among
   the table's types (`Error` and `String` by the compiled interfaces when the type is not
   listed). A value method called through a nil pointer panics with gc's panicwrap message.
7. **Method values and expressions** (§4.6): `x.M` binds the receiver evaluated at once (a
   `goi.Bound`, called as a closure, a real Go func in Go memory by `reflect.MakeFunc`); `T.M` and
   `(*T).M` are thunks of the receiver first.
8. **Assertions and type switches** (§5.3, §10.3): concrete targets compare descriptors (a proxy's)
   or RTs (a faithful type's value); interface targets check the method set (keys and the
   signatures' descriptors), compiled values by `reflect.Type.Implements` or the table; failures
   are gc's `TypeAssertionError` messages (`interface conversion: interface {} is string, not
   int`, `missing method M`, `(types from different packages)`). No inline cache per node yet.
9. **Wrappers** (§6.4): the table generates one per listed interface whose methods it can write
   (63 for linux/amd64), each method calling `goi.CallProxy` by reflect (GC22); `error` and
   `fmt.Stringer`'s are goi's own. A proxy stored in Go memory of a compiled interface type is
   wrapped, loaded back unwrapped (`toMem`, `fromMem`); a compiled call's arguments and results of
   such types are converted (`MemR`, `FromMemR`).
10. **Goroutines and channels** (§8): `GoStmt` evaluates the call as a deferred one is (its own
    frame) and runs it on a new goroutine and goi thread; an uncaught panic is reported as gc's
    and, in program mode, exits with status 2. Channels' operations, `close`, `len` and `cap` are
    reflect's (Go's own rules and messages); the fast paths of §4.9 and §8.2 (S31) are not built:
    the check needs no speed there. A deferred or `go` interface call evaluates its receiver at
    once, nil a nil dereference as gc. A channel's frame value keeps its channel's type through a
    conversion between channel types; it goes into an interface as its own type.
11. **range over functions** (§4.10): the yield is a closure of goi whose code copies its
    parameters into temporaries of the enclosing frame, binds the iteration's variables (new cells
    each iteration) and runs the body there; a break, return or outer jump makes it return false
    and is performed when the function returns; a call after that panics with gc's messages.
12. **Directory tests** (§11.1, §11.2; GC19): `bin/goi run` of a directory test's main package
    loads the packages of its directory by their import paths (`:local-imports`, module paths),
    each checked and defined before its importers, and initializes them in Go's order (go1.21: the
    first package by import path whose imports are initialized). The check compares goi's
    standard output and error combined, and status 0, with the test's golden `.out` (empty when
    there is none), as Go's test driver does, rather than with gc's run of the printed forms
    (repr/go's round trip at its level `run` checks that they give it).
13. **`recover`** (§4.7): a deferred call of `recover` itself recovers the panic its deferring
    function runs in (that function a deferred call), unless that function's own deferred calls
    run for a panic not yet recovered, as gc (`recover1.go`).
14. **Fixes of GR3's core found by the check**: the checker took `(switch false ...)` for a tagless
    switch (`code_stmt.ae`); range targets that are places (`x[i]`) are assigned as one assignment
    (their operands first, §4.5); `==` of structs with blank fields compares the others; a
    conversion between function types keeps the closure; `[0]T(s)` of a nil slice; gc's
    `makeslice` and `growslice` messages for sizes beyond its largest allocation; the main
    package's names in RTs have gc's path `main`; `bin/goi`'s reports no longer end the script at
    their first `head` (pipefail).

**Proposals for the user** (each built as recommended):

- **GC19 The directory tests' reference.** (a) as built, the golden output (the driver's own
  criterion), the round trip having checked the printed forms against it; (b) gc's run of the
  printed forms packaged as the driver does (gocmp's `-dirtest` layout), as for single-file
  programs. *Recommended: (a)*: no second build of 127 tests, and the same verdict where the round
  trip passes.
- **GC20 What compiled code sees of an interpreted value with methods.** (a) as built, its proxy
  (fmt honours its methods; `%T` and `reflect` see the proxy, but runtime's and reflect's
  functions get the value itself, amendment 5); (b) the value itself everywhere (reflect exact,
  fmt's `Stringer` and `error` of interpreted types lost). *Recommended: (a)*: the check's programs
  print their errors and Stringers far more often than they reflect on them (9 known against
  dozens passing by fmt).
- **GC21 The essential subset.** (a) as built, 376 programs (every fourth passing `fixedbugs/`
  program, the directory tests in), about 25 s; (b) GR3's rule, every third, 426 programs, about 30
  s; (c) the whole check, 38-46 s with 16 jobs. *Recommended: (a)*.
- **GC22 The wrappers' calls.** (a) as built, each wrapper method calls the interpreted method by
  reflect (`CallProxy`: simple, about a microsecond a call, enough for `sort.Interface` and
  `io.Reader` in the check); (b) typed adapters per signature, as §6.4's invokers (GR5's adapters).
  *Recommended: (a)* until GR5 measures them.

## As built: GR5 (2026-10-18)

GR5, the crossings and the REPL, is built on branch `goi-gr5` (from main `ec47d8af`, which merges
GR4 and F7, seed `910b4b8f`). Where building it found otherwise than this text, the text below is
the record; proposals for the user are GC23-GC26.

**What exists.**

| part | where | as built |
|---|---|---|
| Arbace fns as Go funcs (§7.5) | `go/arbace/goi/adapt.ae`, `convert.ae` | an Arbace `IFn` given for a parameter of func type (of an interpreted function, of compiled Go, a `GoType`'s conversion) is a real Go func of that type (`FromArbace`): each call converts its arguments to Arbace by their types, applies the fn through the bridge and converts its results back (§5.6; several results: the elements of the sequential the fn returns) |
| typed adapters (§6.4, GC22) | `go/arbace/goi/adapt.ae`, `arbace/go/table.ae` | a **target** (`goi.Target`: an interpreted closure, an interpreted method value, an Arbace fn) is entered as a frame of its signature's layout (`Enter`, `RunCall`, `Leave`, `SetArgR`, `ResR`); the table writes a typed adapter (`adaptN`, registered by `RegisterAdapter`) per unnamed function type of its members' parameters and results and a fixed list of common ones (§6.4's "fixed list": 14 types), 22 for linux/amd64; a closure or method value stored in Go memory (§4.6, S29) and an Arbace fn given to Go use it, else `reflect.MakeFunc`. The wrappers' methods (§5.3 rule 3) are the same typed calls through the proxy's interpreted method (`EnterMethod`), not `CallProxy` by reflect (GC22 (b), as the user decided) |
| the bridge to Arbace | `arbace/lang/GoInterp.ae` (`GoBridge`) | goi cannot import `arbace/lang` (§1.3): GoInterp's initialization gives goi an instance of `GoBridge` (a new native, `init1`), whose methods goi calls through an interface of its own with j2g's names (`Apply_O_O1_I__O1`, `IsFn_O__Z`, `Keyword_String__O` ...): applying an Arbace fn (a GoPanic goi made coming back as its Go value, S50; a `GoExit` as os.Exit's panic), keywords, symbols, vars, deref, numbers, truth, seqs, `Reflector`'s calls |
| the package `arbace` (§7.3) | `go/arbace.ae`, `go/arbace/api.ae`; listed in `go/arbace/gotable.edn` | §7.3's functions (`Call`, `Invoke`, `Str`, `GoStr`, `Kw`, `Sym`, `Var`, `Deref`, `Fn`, `Long`, `Float64`, `Bool`, `Seq`), Go values given to them converted to Arbace by their dynamic types; it imports std only and calls Arbace through `arbace.Hooks` (an interface it declares, `Bridge`; goi sets it), not `arbace/lang` and `arbace/jrt` (amendment 3, GC24). j2g copies it into the program beside goi; the table's generator reads it from `go/` (its other packages from the configuration's root) |
| `GoPanic` (§10.4) | `arbace/lang/GoInterp.ae`, `go/arbace/goi/call.ae`, `trace.ae` | its message gc's text after `panic: ` (`PanicText`: gc's `printpanicval`, `+2.500000e+000`, `panics.Celsius(+3.660000e+001)`, an interpreted `Error`'s or `String`'s result); `ex-data` `{:go/value :go/type :go/trace}`, the trace the interpreted frames innermost first as `[go.demo/Div "REPL" 1]`; a run-time error's Java exception (jrt's `FromRuntimeError`) as its cause (GS11 (a)); its stack trace begins with the interpreted frames; the Go value itself kept (`goValue`) for S50. The thread's stacks are restored when a panic leaves (`guarded`) |
| traces (§10.6) | `go/arbace/goi/stmt.ae` (`SeqL`), `func.ae` (`FuncSource`), `trace.ae`; `go/arbace/jrt/throwable.ae` (`GoiTrace`) | statements store their lines in the frame (S51; amendment 1); a function's cell knows its namespace and file (the Go file, the forms' source for the REPL's implicit file, `REPL` at the REPL); jrt captures the interpreted frames when an exception is made (`jrt.GoiTrace`, goi's) and puts each in place of the Go frame of goi's `run` that ran it, leaving goi's and the table's other frames out |
| the REPL's report | `arbace/main.ae` (hand change 28, doc/VENDOR-NOTES.md) | `ex-triage` reports a `GoPanic` of the cause chain, at its innermost interpreted frame, rather than its cause (GC23): `Execution error (GoPanic) at go.demo/Div (REPL:1).` |
| printing (§5.7) | `go/arbace/goi/value.ae` (`GoString`), `method.ae` (`Proxy.Format`) | `%#v` with interpreted types named by their descriptors (`demo.Point{X:3, Y:0}`; the tagged RTs of §5.2 replaced by their names), `#n` after an old generation's name (`cross.P#1{X:5, Y:6}`), a proxy's `T(v)` or `T{...}`, its `GoString` method when it has one; `Proxy.Format`'s `%#v` the same, so interpreted code's `fmt.Printf("%#v")` of a proxy prints gc's names |
| methods as vars (§2.1) | `arbace/go/repl.ae`, `arbace/go/build.ae` (`method-callable`) | `go/method` defines the var `T.M` (for `(*T).M` too), a `GoFn` of the receiver first (`InterpMethodCallable`): GR3 and GR4 had the method cells but no vars, and `go/method` returned `nil` |
| GS10: interop on Go values | `arbace/lang/Reflector.ae` (hand change 28), `GoInterp` | `(.M v)`: an interpreted type's method (its method set, else its var `T.M`), a compiled type's from the table by the value's reflect type and `T.M` (S39: no lookup by reflect); `(.-F v)` and `(.F v)` without a method: an exported field |
| GS7: collection protocols | `arbace/lang/RT.ae` (hand change 28), `GoInterp` | read-only: `count`, `nth` (slices, arrays), `get` and keyword lookup (structs' exported fields by keyword, maps by key converted by the key type), `seq` (elements; a map's entries as `MapEntry`s), so `vec`, `into` and the sequence functions work; elements converted by their types (§5.6) |
| `go/run-compiled` (G15) | `arbace/go.ae`, `arbace/go/repl.ae` | the package and the interpreted packages it imports written as a forms root (their files' forms with their metadata), built by `arbace.build.go` (embedded) with `ARBACE_GOROOT`'s toolchain (default `/root/tamago-go`) in a temporary directory, the executable run with the arguments (`arbace.java.shell/sh`): `{:out :err :exit}`. Not in the box (no toolchain there) |
| `go/eval`, `go/run-main` | `arbace/go/repl.ae` | GR3's, now with GR5's conversions, traces and `GoPanic` |
| the oracle (S54) | `test/oracle/forms/go.ae`, `test/oracle/expected/forms/go.edn`; `test/oracle/runner.ae`, `bin/oracle record --goi IMPL` | 112 cross-dialect cases (§1.4's session first; Arbace calling Go forms and compiled Go, Go forms calling Arbace through `arbace`, Arbace fns as Go funcs, Java forms calling Go forms through vars and Go forms calling Java forms' objects, panics, printing, GS7, GS10); a corpus file whose first line says `;; oracle: recorded by goi` is recorded from the Go build with `--goi` and reviewed (ORACLE.md, "Adding cases"), skipped by `record` without it and by `check jvm`; the Go build's check runs it as any other |
| tests | `test/goi/repl.ae` (63 checks, 30 of them GR5's), `test/goi/session.in`, `.expected` | GR5's crossings in `bin/goi test` and the smoke test; §1.4's session typed at the REPL against its transcript, in both |

**§1.4's session** runs as written, its transcript `test/goi/session.expected`: but for the line
numbers, which are the REPL's (Arbace's REPL numbers each input's lines from 1, so `Div`'s panic
is at `(REPL:1)`, not §1.4's illustrative `(REPL:7)`).

**The check (§11).** No regression: `bin/goi check` over the 1,051 run programs and the 127
directory tests, against `test/goi/check-linux-amd64.edn`: 832 pass, 0 fail or error, 51 known, 280
unsupported, 15 skipped, as at GR4 (the reference unchanged; 280 s with 16 jobs on the loaded machine).

**Measurements.** `bin/goi run test/goi/run go.t.fib bench` (best of three, amd64, the machine
shared): fib 22 2.57-2.64 ms, sumSq 1e6 20.1-20.8 ms, sieve 1e6 68.6-69.0 ms (GR4: 2.46, 19.9, 62; another
agent's builds sharing the machine): the lines
statements store cost a few percent (a store per statement, and a closure for a list of one
statement whose line it sets). The executable is 133,836,960 bytes against 133,308,576 built from
main `ec47d8af` (+528,384, 0.4%: the bridge, the adapters, the package `arbace`, the typed
wrappers). The table: 45 packages, 808 signatures, 255 invokers, 22 adapters.

**Amendments (as built).**

1. **Lines** (§10.6, S51): a statement list's node (`SeqL`, the builder's `body-node`) stores each
   statement's line in the frame before it runs; a frame's line is reset when a call enters it,
   so a frame whose body has not started (its arguments being evaluated) is not in a trace.
   `Frame.Line` is GR5's (GR3's amendment 2 left it).
2. **Traces' files**: the namespace and file of a frame come from its function's cell
   (`FuncSource`); a function literal's are its enclosing function's; a method thunk's (GR4) the
   package's namespace, without a file.
3. **The package `arbace`** imports std only (§1.3 and §7.3 have it import `arbace/lang` and
   `arbace/jrt`): its declarations' digest and checking then need no Go forms of `lang` (which
   are j2g's output, not tracked forms); its `Hooks` (an interface value of its type `Bridge`,
   excluded from the table) are goi's, which holds the bridge (GC24). `Call` also takes a String
   naming a var; `Long` and `Float64` take Go numbers too (each argument goes to Arbace by its
   dynamic type first).
4. **The bridge** (§1.3): goi reaches Arbace only through `arbace.lang.GoBridge`, given at
   GoInterp's initialization; an Arbace exception thrown into Go code is a Go panic whose value is
   the `Throwable` (§10.5), a `GoPanic` goi made is its Go value again (S50).
5. **`GoPanic`'s report** (§1.4, GS11): with the cause GS11 (a) gives it, `arbace.main`'s report
   would name the cause (`ArithmeticException`, `/ by zero`); `ex-triage` names the `GoPanic`
   instead (hand change 28; GC23).
6. **Typed adapters** (§6.4): one per unnamed function type the table's members take or return,
   and 14 common ones (`func()`, `func(int, int) bool`, `func(rune) rune` ...); a named function
   type (`http.HandlerFunc`) and the others get `reflect.MakeFunc`. An adapter's frame is the
   target's own (an interpreted closure's at the thread's next depth; an Arbace fn's a frame of
   its own), so the adapters serve every target alike.
7. **Methods' vars** (§2.1): `go/method` returns the var `T.M` and Arbace calls it with the
   receiver first; a pointer receiver's method given a value from Arbace is an error, a value
   receiver's given a pointer dereferences it (Go's implicit `*`).
8. **GS9 not built**: the check finds `%T` of interpreted values in 1 of 1,178 programs (a known
   difference, `fixedbugs/issue15528.go`), which GS9's condition ("if the check finds it frequent")
   does not meet; `%T` stays a known difference (G8).
9. **The box's oracle** leaves `forms/go.ae` out (`bin/gate --full`'s box-oracle step,
   `--exclude forms/go.`): `go/run-compiled` has no toolchain in the box, and goi's crossings
   there are not checked yet (GC25).
10. **The differential check** (`bin/diffcheck`, Futamura F2) leaves `forms/go.ae` out of Clojure's
   pair (`test/diffcheck/gen.ae`): its Go forms are calls of `arbace.go.repl` at run time whether
   evaluated or compiled by c2j, and its `go.*` namespaces are not the pair's corpus namespaces;
   goi's own pair is the check (§11).

**Proposals for the user** (each built as recommended):

- **GC23 The report of a Go panic.** (a) as built, `arbace.main` reports the `GoPanic` (its
  message gc's text) at its innermost interpreted frame, its cause the run-time error's Java
  exception (GS11 (a)) for those who catch it; (b) report the root cause, as for any exception
  (`Execution error (ArithmeticException) ... / by zero`). *Recommended: (a)*: §1.4's report, and
  gc's message is the one a Go programmer knows.
- **GC24 The package `arbace`'s imports.** (a) as built, std only, its calls through `Hooks`
  (goi's bridge); (b) §7.3's `arbace/lang` and `arbace/jrt` imports, which would need `lang`'s Go
  forms (j2g's output) for the table's checker. *Recommended: (a)*.
- **GC25 goi's crossings in the box.** (a) as built, `forms/go.ae` left out of the box's oracle
  until checked there (with the check in the box, GS13); (b) in the box's oracle now, but for
  `go/run-compiled`. *Recommended: (a)*.
- **GC26 Lines in frames.** (a) as built, every statement stores its line (a few percent on
  fib, sumSq and sieve); (b) only statements that can panic or call (fewer stores, a statement
  without a call or a check shows its predecessor's line). *Recommended: (a)*: exact traces,
  within GR3's speed criterion.
  *Decided: (b)*, by the user on 2026-10-19 (not the recommended (a)); built as GR5.1, below.

## As built: GR5.1 (2026-10-19)

GR5.1 builds the user's decision on GC26, (b), on branch `goi-gr5-1` (from main `8ed8d6cb`, seed
`215d3a1b`, which has GR5). Proposals for the user are GC27.

**The rule.** goi's builder (`arbace/go/build.ae`: `quiet-expr?`, `quiet-place?`,
`quiet-header?`, `stored-line`, `hoisted-line`) decides statically, as it builds a statement
list's node (`body-node`, the `SeqL` and its lines), whether each statement's own part can
panic or call; only such a statement stores its line in the frame before it runs (§10.6, S51).
A statement's own part is the statement without its nested statement lists, which store their
own: an `if`'s init, condition and else-if chain (its blocks' `let` declarations too), a `for`'s
init, condition and post, a `range`'s operand and targets, a switch's init, tag and case values,
a type switch's init and operand. The decision is conservative: an expression is **quiet** only
when it is one of these kinds, its operands quiet:

- a constant, a variable, a function, `nil`, a method expression, a function literal (its body
  is its own frame's), a comma-ok's `ok`, a type (`new`'s argument);
- `+ - not bit-not`; `and`, `or`; arithmetic of basic types but integer `/` and `%` by a
  non-constant (floats' and complex numbers' are quiet), shifts but by a non-constant signed
  count; comparisons of basic types, pointers and channels, or with `nil`;
- a conversion between basic types, or between a string and bytes or runes (not a slice to an
  array, not of a type parameter), a conversion to an interface;
- a field selection with no pointer on its path (the checker's `:derefs`), an array's index by a
  constant (checked statically), a map read by a key of a basic type (no hash of an interface), a
  composite literal of quiet elements (a map's keys basic), `&x` of a quiet `x`;
- the builtins `len`, `cap`, `min`, `max`, `real`, `imag`, `complex`, `new`.

Every other kind can panic or call: any call and method call, `go`, `defer`, a crossing into
Arbace (a call), slice and string indexing, slicing, a dereference, a type assertion (but
comma-ok), a receive, a send, `select`, a map write, `append`, `make`, `copy`, `delete`,
`panic`, `print`, a method value, a range over a function, a channel or a pointer to an array,
a switch on a tag not of a basic type, and any kind not listed. Places: a local or global variable
is quiet, a field without a pointer on its path and an array element by a constant are quiet,
the rest not. A `return` stores its line in a function that defers (its deferred calls run
there). `break`, `continue`, `goto`, `fallthrough`, local types and directives are quiet.

**Loops' hoisted line** (amendment, GC27): a `for` or `range` whose own part is quiet and whose
body is one statement without statement lists of its own (an expression, an assignment, a
declaration, a send, `go`, `defer`) that can panic or call stores that statement's line once,
before the loop, and the body stores nothing at each iteration: the frame's line is the same at
every point that can raise (sieve's inner loop `composite[j] = true`).

**Traces.** Correct wherever a statement can raise: such a statement stores its line, and
whatever ran after it stored a subset of what GR5 stored. A probe of 17 panics (in a callee and
its caller, in a loop, an else-if, a switch's clause, a deferring function's return, a function
literal, a nil dereference, a nil map's write, a slice-to-array conversion, a negative shift, a
failed assertion, an uncomparable comparison, an explicit panic, hoisted loops, a labeled one, a
range) gives the same `:go/trace` on GR5's executable and GR5.1's. `test/goi/repl.ae` (63 checks)
and §1.4's session (`test/goi/session.in`) pass unchanged.

**The check (§11).** `bin/goi check`: 833 pass, 0 fail or error, 50 known, 280 unsupported, 15
skipped, no regression against `test/goi/check-linux-amd64.edn` (271 s, 16 jobs): the one more
pass is `gc2.go`, known for `runtime.ReadMemStats` and the check's 10 s, which depends on the
collector and the machine's load (the reference is left as it is).

**Measurements.** `bin/goi run test/goi/run go.t.fib bench`, best of five per run, three runs
alternating GR5's executable (stage 0 of seed `215d3a1b`) and GR5.1's on the shared machine
(load about 12):

| | GR4 | GR5 (its record) | GR5, this session | GR5.1 |
|---|---|---|---|---|
| fib 22 | 2.46 ms | 2.57-2.64 ms | 2.57-2.60 ms | 2.56-2.58 ms |
| sumSq 1e6 | 19.9 ms | 20.1-20.8 ms | 20.96-21.29 ms | 17.99-18.76 ms |
| sieve 1e6 | 62 ms | 68.6-69.0 ms | 67.95-70.54 ms | 65.02-66.26 ms |

sumSq's loop is all quiet (no store: faster than GR4); sieve keeps a store for `!composite[i]`
and the inner loop's hoisted one; fib's `if n < 2 { return n }` is quiet, but its call's
statement stores (GR5's remaining cost there is the frame's entry, not the lines). Without the
hoisting, sieve was 69.3-70.8 ms against GR5's 68.8-71.4 in the same session. The executable is
133,951,648 bytes (+114,688 against GR5's 133,836,960: the builder's predicates, compiled).

**Proposals for the user** (built as recommended):

- **GC27 Loops' hoisted line.** (a) as built, a loop whose own part is quiet and whose body is
  one simple statement that can panic or call stores that line once before the loop (the same
  traces; sieve 65-66 ms against 69-71); (b) GC26 (b)'s letter: the body's statement stores its
  line at each iteration. *Recommended: (a)*.

## As built: GR6 (2026-10-19)

GR6, generics, is built on branch `goi-gr6` (from main `a1a50c4c`, seed `1d5c35b4`, which has GR5
and GR5.1: GC26 (b), GC27 as built). Where building it found otherwise than this text, the text
below is the record; proposals for the user are GC28-GC34.

**What exists.**

| part | where | as built |
|---|---|---|
| type parameters, type sets | `arbace/go/check/types.ae` | a type parameter's `:bound` (its constraint's type, set once its list is declared) and `:iface` (`tparam-iface`, go/types' `iface`: the bound's interface, else an implicit one); an interface's third element its type set, `{:terms :comparable}` (terms a set of `[tilde? T]`, normalized after `termlist.go`, their order kept for messages); unions `[:union terms]`; `typeset-pairs`, `under-is`, `all-*` (go/types' `allInteger` ...), `common-under`; comparability strict and dynamic (`comparableType`); `satisfies-cause` (go/types' `implements` of a constraint) |
| constraints, declarations | `arbace/go/check/code.ae`, `decl.ae`, `code_stmt.ae` | `tparam-scope` (go/types' `collectTypeParams`), unions and `~T` terms (`union-type`), interfaces' type sets (`computeInterfaceTypeSet`); generic types (their type parameters known before their constraints, which may name them), generic aliases (go1.24), local generic types, a generic type's methods (the receiver's type parameters constrained as the type's, the receiver the instance by them, `_` allowed), generic methods (go1.27: a method's own `:tparams`, the receiver's `:rtparams`) |
| inference | `arbace/go/check/infer.ae` (new, 387 lines) | go1.27.1's `unify.go` (`nify`, joined handles, inexact and assign modes, interface inference) and `infer.go` (function arguments, constraints' core terms and methods while they make progress, untyped constants' default types, simplification, cycles killed), `renameTParams`, `isParameterized` |
| instantiation, calls | `arbace/go/check/code_call.ae`, `code_expr.ae` | a generic function's value (`:generic-fn`, `:gtype`, `:tparams`, partial `:ptargs`) instantiated by its call (`call-args`: the callee's and the generic arguments' type parameters inferred together, go/types' `arguments`), by its assignment to a function type (reverse inference, go1.21: `func-inst-target`), or by `(inst F T...)` (partial: its constraints then infer the rest); instances' signatures substituted and their type arguments verified after the declaration (`verify-later!`); operations on type parameters by their type sets (operators, comparisons, shifts, conversions, assignability, indexing, slicing, composite literals by the common underlying type, `len`, `cap`, `append`, `copy`, `delete`, `clear`, `close`, `make`, `min`, `max`, receive, send, `*p`, `range`, calls of a type parameter of function type); method selections on type parameters (their constraints' methods); unsafe's sizes of types depending on type parameters not constant |
| `:inst` | `code_call.ae` (`inst-say`, `record-inst!`) | the converter's `:inst` read as types and compared with the type arguments the checker computed (§3.6): verified or an error; in annotated forms an inferred instance without `:inst` is an error |
| the annotation check | `arbace/go/check.ae` | S53 ends: every declaration checked, counted and its annotations verified, `:inst` included (amendment 6) |
| instances | `arbace/go/build.ae` (`inst-tree`, `instance-cell`, `build-instance`, `isubst`) | §9.2 (amendments 1-4) |
| generic types' descriptors | `arbace/go/build.ae` (`named-desc`, `inst-gen`, `inst-type-name`) | an instance is an interpreted named type `pkg.T[args]` (gc's runtime name, type arguments named by package path), a generation of its own per origin and type arguments' descriptors; a compiled package's generic type's instance too (§9.3: `atomic.Pointer[T]`) |
| the interpretable packages | `go/arbace/gotable.edn` (`:interpret`, `:replace`), `arbace/go/table.ae` (`interpret-forms`), `arbace/go/repl.ae` (`interpretable?`, `load-digest-package!`) | §9.3 (amendment 5) |
| the REPL | `arbace/go/repl.ae` | a generic function or method has no cell and no var (its instances have cells, made by their callers' builds); every other declaration defined as before, those that use generics included |
| tests | `test/goi/run/go/t/generics.ae`, `test/goi/repl.ae`, `test/goi/errors/generics.ae` | 21 lines of output against gc's (written by hand: no `:inst`, the checker infers every type argument): generic functions, inference, reverse and partial instantiation, generic types and methods, a generic method, type sets, slices, maps, cmp, `atomic.Pointer`, `sync.OnceValue`; five checks of generics at the REPL (a generic function redefined, its instances' callers seeing the new code; `go/eval` inferring); seven errors of generic code with gc's messages (`bin/goi test --gc`) |

**The annotation check (§11.4, the GR6 exit criterion).** `bin/goi annotations`: zero errors in
every package of the five corpora, every declaration checked (none left unchecked), every
annotation verified, `:inst` among them:

| corpus | declarations checked (using generics among them) | `:inst` verified | `:val` | `:tag` | `:go/via` | `^:assign` | `conv` | time |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `tamago-amd64` | 35,783 (1,258) | 1,061 | 9,084 | 269 | 4,969 | 757 | 22,534 | 35-40 s |
| `tamago-arm64` | 35,700 (1,258) | 1,061 | 8,989 | 270 | 4,956 | 757 | 22,515 | 32-38 s |
| `linux-amd64` | 38,366 (1,289) | 1,091 | 10,972 | 282 | 4,909 | 793 | 24,185 | 39-42 s |
| `linux-test` | 9,770 (1,234) | 422 | 633 | 48 | 163 | 37 | 3,555 | 26-29 s |
| `linux-dirtest` | 1,726 (478) | 218 | 30 | 0 | 12 | 0 | 198 | 22 s |

The declarations counted apart at GR5.1 (1,263, 1,263, 1,294, 1,290, 498) are now checked and
counted (`tamago-amd64` 34,520 + 1,263 = 35,783); their annotations (`:val` 63, `:tag` 26, `:go/via`
1,125, `^:assign` 86, `conv` 1,416 in `tamago-amd64`) verified with the rest. A declaration "uses
generics" when checking it meets a generic declaration, an instance or a type parameter (5 fewer
than GR2 marked in `tamago-amd64`: GR2 also marked a declaration whose operand a generic one left
invalid).

**The check (§11, the GR6 exit criterion).** `bin/goi check` over the 1,051 run programs of
`$GOROOT/test` and the 127 directory tests, against `test/goi/check-linux-amd64.edn` (recorded):

| result | GR5.1 | GR6 |
|---|---:|---:|
| pass | 832 (833) | 1,036 |
| fail, error | 0 | 0 |
| known (`test/goi/check-known.edn`) | 51 | 59 |
| unsupported | 280 | 68 |
| skipped (gc's flags) | 15 | 15 |

Of the 217 programs GR5.1 left unsupported for GR6, 202 pass, 8 are known and 7 are unsupported for
GR3's recursive types (a type recursive through a slice, a map or a function: `typeparam/cons.go`,
`issue47901.go`, `issue48030.go`, `issue48137.go`, `issue48185b.go`, `issue48617.go`,
`fixedbugs/issue72063.go`); the two that imported `maps` and `iter` pass (§9.3). Of
`$GOROOT/test/typeparam`'s 200 run programs (directory tests among them), 186 pass, 6 are known, 8
unsupported (the 6 recursive types above, and `encoding/json` and `encoding/xml`, not listed: GS5).
The new known differences are by design: a finalizer's timing (`fixedbugs/issue54343.go`,
`typeparam/chans.go`, `chansimp.go`: their `_Ranger` waits for one), `%T` of interpreted values
(`genmeth1.go`, `typeparam/issue49547.go`: G8, GS9 not built, amendment 9) and reflect's names
and identities of interpreted instances (`typeparam/issue54456.go`, `mdempsky/17.go`, `nested.go`:
G8). No program passing at GR5.1 regresses. The whole check takes 42-48 s with 16 jobs on the
compiled executable (88 s with the namespaces evaluated from the checkout). The essential
subset, `test/goi/essential.txt`, is 566 programs (GC28), 30 s with 8 jobs.

**Measurements (GR3's criterion).** `bin/goi run test/goi/run go.t.fib bench`, best of five per run,
three runs alternating GR5.1's executable (stage 0 of seed `1d5c35b4`) and GR6's on the shared
machine (load about 16-18):

| | GR5.1 (its record) | GR5.1, this session | GR6 |
|---|---|---|---|
| fib 22 | 2.56-2.58 ms | 2.55-2.72 ms | 2.55-3.01 ms (best 2.55) |
| sumSq 1e6 | 17.99-18.76 ms | 18.91-20.74 ms | 17.87-21.07 ms (best 17.87) |
| sieve 1e6 | 65.02-66.26 ms | 63.55-90.56 ms | 65.52-71.17 ms (best 65.52) |

An instance runs as fast as the function written for its type arguments (its nodes are typed by
them, G7 (a)): a generic `fib[T ~int | ~int32 | ~int64]` instantiated with `int`, 2.50-2.61 ms
against `fib`'s 2.48-2.67 in the same runs. The executable is 135,504,032 bytes against GR5.1's
133,951,648 (+1,552,384, 1.2%): the checker's generics and inference, the builder's instances
(c2j's Go of their Arbace), and the digest's interpretable packages (linux/amd64's digest 336,715
bytes compressed against 324,205: +12,510).

**Amendments (as built).**

1. **Instances are built from checked trees substituted** (§9.2). An instance's code is built from
   its generic declaration's checked tree with every type substituted (`inst-tree`): its local
   variables and local named types copied per instance (gc makes a local type per instance), each
   node copied once (a call of several results is shared by its results' nodes), method selections
   on a type parameter resolved for the instance's type (an interface method of the constraint
   becomes the type's method, its call a method call), constants represented at their instance
   types (a `float32`'s rounded; a constant converted to a type parameter constant again),
   unsafe's `Sizeof`, `Alignof` and `Offsetof` that depended on type parameters constant. So the
   builder builds an instance as any function, its nodes typed by the type arguments; it never
   sees a type parameter. The checker keeps a generic declaration's tree for this, also of the
   digest's packages (§9.3), which it checks whole.
2. **Instances' cells** (§9.2): one per generic function or method object and type arguments'
   descriptors (interned, so equal across build contexts), built by the build context of the
   generic declaration's package (an interpreted package's last, a compiled package's made for its
   digest's declarations: `ctx-of`), on first call or at once when building eagerly (program
   mode), named as gc names them in traces (`F[...]`, `(*List[...]).Push`). A redefinition at the
   REPL checks the package again: the callers' code, built again on their next call, reaches new
   instances.
3. **A generic type's instance** is an interpreted named type of the instance's name and a
   generation of its own (the interning key of §5.2 per instance; its RT's tag
   `goi:"main.List[int]#N"`), its methods instantiated per instance: a method's cell for a
   selection on an instance is its instance's (`method-target`), in method sets too (an instance
   in an interface: §5.3). A generic method has no method set entry (go1.27: it implements no
   interface method).
4. **What the checker makes explicit for instances** (§3.7): a type parameter's value assigned to an
   interface is a `:convert-iface` (an instance's type may not be an interface); method selections
   carry their receiver's `:recv-type` (the instance), generic function values their `:targs`.
5. **The interpretable packages** (§9.3): `cmp`, `iter`, `maps`, `slices` and `internal/race`
   (which `iter` imports; its functions are empty but in race builds), their forms whole in each
   configuration's digest (`:interpret`), loaded by goi as interpreted packages of their std paths
   when first imported (`load-digest-package!`, as a package from `ARBACE_PATH`: checked, defined,
   initialized). A declaration naming a package neither listed nor interpretable is left out of
   the digest with that import (`internal/race`'s two functions of `internal/abi`'s types). The
   list's `:replace` gives declarations in Go forms for those the runtime implements through
   `go:linkname`: `maps.Clone` (through `maps.clone`), a copy by `range` (GC29). `iter.Pull` stays
   unsupported (its coroutines are the runtime's).
6. **The annotation check's records** (§11.4): `test/goi/annotations-CORPUS.edn`'s `:declarations`
   gains `:unsupported` (declarations whose code goi's checker does not check: none) and `:generic`
   counts the declarations using generics among those checked; `:generic-errors` became
   `:unsupported-errors`, `:in-generic` `:in-unsupported`, both zero; a corpus passes only with no
   declaration unchecked.
7. **A compiled package's unexported type** met in an instance of its generic type
   (`sync/atomic.noCopy` in `atomic.Pointer[T]`) is described as an interpreted named type: the
   table holds exported types only (GC30).
8. **Program mode** (§2.7): a function built on its first call (an instance, a method thunk) that
   goi does not support ends the run as `goi: unsupported: REASON`, status 125, as one found
   before the run does (GC33).
9. **`%T`, reflect** (§9.4, G8): an interpreted instance's value in an interface is its proxy, or for
   a struct its RT; `%T` and reflect's names show those, not gc's `main.T[int]` (GS9 not built: 3
   programs of 1,178).
10. **Fixes found by the check**, outside generics: `((.M x))` calls the method's result (GR2 made
    the callee invalid); a method's receiver is not checked as a parameter (its type is the
    declaration's); a range over values assigned to interface targets converts them; a select's
    comma-ok receive into interface targets; assertion messages name unnamed interfaces as gc's
    runtime does (`interface { T() main.T }`); a struct literal keyed `[name value]` (the
    converter's form for a literal of a type parameter's type).
11. **Errors** (§3.8): an operand of a type parameter's type is written with its constraint
    (`variable of type T constrained by any`), a named basic type's with its kind (`int type
    Celsius`), a generic type used without instantiation with its type parameters (`List[T any]`),
    type set terms in their written order, as types2 writes them; "cannot infer" names the type
    parameter's file and line (gc adds its column).

**Proposals for the user** (each built as recommended):

- **GC28 The essential subset with generics.** (a) as built, GR5.1's 379 programs and the programs
  GR6 made pass by the same rule (every one outside `fixedbugs/`, every fourth new one of
  `fixedbugs/`): 566, 30 s with 8 jobs; (b) GC21's rule applied anew to the new reference (565,
  its `fixedbugs/` selection shifting by the new passes); (c) GR5.1's 379 (no generic program in
  the essential gate). *Recommended: (a)*: the rule's subset, stable.
- **GC29 Declarations the runtime implements for an interpretable package.** (a) as built, the
  list's `:replace` writes them in Go forms (`maps.Clone`); (b) §9.3's letter: unsupported (any
  program calling `maps.Clone` unsupported). *Recommended: (a)*: one short declaration, and
  `maps.Clone` is common.
- **GC30 Unexported types of compiled packages in instances.** (a) as built, described as
  interpreted named types (their values made by interpreted code only); (b) listed in the table
  (their RTs compiled; the table grows by every unexported type a listed generic type names).
  *Recommended: (a)*.
- **GC31 Instances' names.** (a) as built, gc's runtime names (`main.List[int]`, type arguments by
  package path) for `%#v`'s printing and traces; (b) go/types' type strings (`List[int]`, package
  names). *Recommended: (a)*: what a Go programmer sees from gc.
- **GC32 Listed packages' generic functions over internal packages.** `reflect.TypeFor`,
  `reflect.TypeAssert`, `runtime.AddCleanup` and `math/rand/v2`'s `N` reach `internal/abi` or
  unexported functions the table does not hold. (a) as built, unsupported when called; (b) goi's
  overrides for them (`reflect.TypeFor[T]` the RT of `T`'s descriptor). *Recommended: (a)* now,
  (b) when a program of the check needs one.
- **GC33 Code built late in program mode.** (a) as built, an unsupported form met by a function
  built on its first call ends the run as unsupported (status 125); (b) build every instance of
  the program before it runs (instances found by a closure over the call graph: not their
  arguments' types computed at run time, so impossible in general). *Recommended: (a)*.
- **GC34 Generic functions at the REPL.** (a) as built, a generic function has no var (Arbace
  cannot name its type arguments; Go functions calling it are the way); (b) a var whose `GoFn`
  infers the type arguments from its arguments' Go types at each call. *Recommended: (a)*, (b) to
  reconsider with GS2's auto-import after GR6.

## As built: GR7a (2026-10-19)

GR7a, a part of GR7 (§12's row "GR7 (later)"), is built on branch `goi-gr7a` (from main `90ec844d`,
which has GR1 to GR6): the check in the box (GS13 as decided: a subset run by the TamaGo guest in
`bin/gate --full`), GC25's exclusion lifted (goi's crossings in the box's oracle), and GR3's limit
on recursive types lifted. GR7b (errorcheck programs, `reify`) is built beside it and numbers its
proposals from GC45; GR7a's are GC35-GC42. Where building it found otherwise than this text, the
text below is the record.

**What exists.**

| part | where | as built |
|---|---|---|
| the check in the box | `bin/goi check --box`, `test/goi/box.txt`, `test/goi/check-box-amd64.edn`, `test/goi/check-box-known.edn` | the programs of `box.txt` (GC36) each run by the amd64 guest (`target/arbace-go/box-amd64/arbace-box`, `GOI_BOX_EXE`) in a box of its own on /dev/kvm: the guest's standard input writes the program's forms (a directory test's packages too) into its in-memory file system under `/goi`, then calls `arbace.go.run/-main` on `/goi`; the result against gc's run on linux/amd64 (the check's reference, `.tmp/goi-check/ref`, shared), the box's console one stream (GC35); `check-known.edn`'s and `check-box-known.edn`'s programs known; the results against the box's own reference, as the check's (a program passing there and not now fails). 8 boxes at a time, each of the launcher's 4 vCPUs |
| the gate | `bin/gate --full` (`box-goi`), `Makefile` (`box-goi`) | after every chain (it reads repr-go's fresh conversions of `$GOROOT/test`, and gc's reference the `goi` step shares), when the box was built |
| goi's crossings in the box's oracle | `bin/gate --full`'s box-oracle step, `Makefile`'s `box-oracle`, `test/oracle/known-go-box-amd64.edn` | `forms/go.ae` no longer left out (GC25 (a) ended); it holds no `go/run-compiled` case (that is `test/goi/repl.ae`'s, natively), so nothing of it is excluded: 111 of its 112 cases match, the other the box's nil dereference (GC40) |
| shaped types | `arbace/go/build.ae` (`shaped?`, `shape-type`), `go/arbace/goi/types.ae` (`TShape`, `reshape`, `Shaped`) | a named type recursive other than through pointers is held in Go memory at its shape (GC38, amendment 1) |
| interfaces' methods | `arbace/go/build.ae` (`*iface-pending*`, `desc`) | given once the outermost descriptor is built (GC41, amendment 2) |
| tests | `test/goi/run/go/t/recursive.ae` | 16 lines of output against gc's (converted by repr/go from Go source): a slice, a function, a channel and a map recursive through themselves, a struct through a slice of itself with methods in interfaces and a type assertion, mutual recursion (`A` through `[]B`, `B` through a field `A`), a pointer type recursive through its element (`type Peano *Peano`) and through an array (`T19`), generic ones (`Src[T] func() (T, Src[T])`, `GChan[T] chan GChan[T]`), and an interface naming a struct that names it (`typeparam/cons.go`'s `List[T]`, `Cons[T]`) |

**The check in the box (GS13, the exit criterion).** `bin/goi check --box`, on the box's guest of
this branch (amd64, /dev/kvm, nested KVM):

| | programs | pass | fail, error | unsupported | known | time (8 boxes) |
|---|---:|---:|---:|---:|---:|---:|
| the essential subset as GR6 left it (the first run comparing the console as one stream) | 566 | 555 | 9 | 2 | 0 | 262 s |
| `box.txt` at GR6's programs (recorded on GR6's program) | 556 | 555 | 0 | 0 | 1 | 263 s |
| `box.txt` with GR7a's ten (GC37), GR7a's program | 566 | 565 | 0 | 0 | 1 | 279-284 s |

Eight of that run's nine failures are the box's by design and are left out of `box.txt` (GC36):
seven run the go command or gc's tools (`const7.go`, `linkmain_run.go`, `linkx_run.go`,
`fixedbugs/issue14636.go`, `issue21576.go`, `issue22662b.go`, `issue52127.go`: the box has no
processes and no toolchain), `env.go` reads `PATH` (the box's environment holds none); the two
unsupported are `chanlinear.go` and `maplinear.go`, whose build constraints (`darwin || linux`)
leave out `GOOS=tamago`. The ninth failure is known (`check-box-known.edn`): `typeparam/issue50109.go`: its output ends without a
newline, and the box's console ends the last line with one (the board's line buffer). Compared
stream by stream (before GC35), 25 more failed only because a Go program's standard output reaches the launcher's standard
error in the box; and with 2 vCPUs `mallocfin.go` ends with the board's `fatal error: task: no
idle vCPU` (it sets `GOMAXPROCS(4)`: TamaGo's runtime asks the board for an M per P, and the board
has one per vCPU), so each box has the launcher's 4.

**The check (§11).** `bin/goi check` over the 1,051 run programs and 127 directory tests, against
`test/goi/check-linux-amd64.edn` (recorded):

| result | GR6 | GR7a |
|---|---:|---:|
| pass | 1,036 | 1,049 |
| fail, error | 0 | 0 |
| known | 59 | 60 |
| unsupported | 68 | 54 |
| skipped (gc's flags) | 15 | 15 |

The 14 programs GR6 left unsupported for GR3's recursive types (its amendment 3) run: 13 pass
(`ddd.go`, `reorder2.go`, `fixedbugs/bug294.go`, `bug336.go`, `issue17039.go`, `issue4316.go`,
`issue72063.go`, and of `typeparam/` `cons.go`, `issue47901.go`, `issue48030.go`, `issue48137.go`,
`issue48185b.go`, `issue48617.go`), and `peano.go` is known: its `fact(9)` in Peano numbers
recurses 362,880 interpreted calls deep, past goi's depth limit (S23, 100,000; GC42). No program
recurses through a type goi cannot make now. `$GOROOT/test/typeparam`'s 200 run programs: 192 pass
(GR6: 186), 6 known, 2 unsupported (`encoding/json` and `encoding/xml`, not listed: GS5). No
program passing at GR6 regresses. 125 s with 16 jobs on a loaded machine (51 s on the second run).

**The essential subset** (`test/goi/essential.txt`): GR6's 566 and the ten GR7a made pass by GC28's
rule (every one outside `fixedbugs/`, every fourth new one of `fixedbugs/`: `bug294.go` and
`issue72063.go`, the first and the fifth of five): 576 (GC37).

**Measurements (GR3's criterion).** `bin/goi run test/goi/run go.t.fib bench`, best of five per run,
seven runs alternating GR6's executable (main `90ec844d`'s, 135,504,032 bytes) and GR7a's on the
shared machine (load 7-19):

| | GR6 (its record) | GR6, this session | GR7a |
|---|---|---|---|
| fib 22 | 2.55-3.01 ms (best 2.55) | 2.52-4.42 ms (best 2.52) | 2.55-3.09 ms (best 2.55) |
| sumSq 1e6 | 17.87-21.07 ms (best 17.87) | 18.85-19.29 ms (best 18.85) | 19.46-21.32 ms (best 19.46) |
| sieve 1e6 | 65.52-71.17 ms (best 65.52) | 65.03-68.25 ms (best 65.03) | 68.50-71.36 ms (best 68.50) |

None of the three programs reaches GR7a's code (I and slice nodes, no shaped type, no interface):
`perf` shows the same hot functions, placed 2,976 bytes further in the executable; the 3-5% on
sumSq and sieve is that placement's, within GR3's criterion (twice X2's sketch: fib 22 2.87 ms,
sumSq 1e6 22 ms) and GR6's own spread. The executable is 135,557,280 bytes against GR6's
135,504,032 (+53,248): the builder's shapes and the deferred interfaces, `TShape` and `reshape`.

**Amendments (as built).**

1. **Shaped types** (§5.1, §5.2; GR3's amendment 3 amended). A named type that reaches itself
   other than as a pointer's element (`shaped?`: through a slice, an array, a map, a channel, a
   function's parameter or result, or a struct field of another named type) is *shaped*: before
   its underlying type is built (whose descriptor asks for the type's Go memory type), it gets its
   shape as its Go memory type (`TShape`): the checker type of its layout and pointer words
   (`shape-type`: every pointer, map, channel and function an `unsafe.Pointer`, a slice a
   `[]unsafe.Pointer`, an array and a struct their elements' shapes, a recursive interpreted
   named type its underlying type's shape, another named type itself), a struct's shape with goi's
   tag of its own on its first field (`goi:"path.Name#gen shape"`) so that shapes of different
   types differ. Its RT, the frame's type, is made after as any named type's, from its parts' Go
   memory types (the shape where it recurses): `type S []S` is `[][]unsafe.Pointer` in frames and
   `[]unsafe.Pointer` in Go memory, `type F func(int) F` `func(int) unsafe.Pointer` and an
   `unsafe.Pointer`, `type Tree struct{Name string; Kids []Tree}` the tagged struct of a string and
   a `[]struct{...}` of the shape. The two are the same bytes: `toMem` and `fromMem` reinterpret one
   as the other (`reshape`, a copy through `reflect.NewAt`) after and before their other
   conversions (an interpreted closure is its Go func first, then that func's word), and both are
   the identity on a value already of the target type. An interface's content of a shaped type is
   its Go memory value, as an erased pointer's is (so `hasType`'s Go memory type stays the test);
   a shaped struct's shape is registered beside its RT (`interpOf`). A type that reaches itself
   only through pointers keeps GR3's erasure; a named pointer type recursive through its element
   (`type Peano *Peano`) is that erasure too, its Go memory type set before its RT (`*Mem`), so GR3's
   "recursive pointer type" is no longer unsupported. Where a frame value of a composite type is
   made or converted, the type's RT is used, no longer its Go memory type (they differ only for
   shaped and erased types): `make` of slices, maps and channels, literals, `append`, conversions,
   `unsafe.Slice`, slicing an array, a closure's or method value's Go func (`goFunc`,
   `adaptTarget`), a variadic call's slice from Arbace. Allocations stay at the parts' Go memory
   types, so the collector sees every pointer word: a shape's slice header is a `[]unsafe.Pointer`
   whose array was allocated at its real element type.
2. **Interfaces' methods after the types they name** (§5.3). An interface descriptor's methods'
   signatures (`TSetIfaceMethods`) are given when the outermost `desc` call returns, not while its
   types are being built: `typeparam/cons.go`'s `Cons[a]` has a field `List[a]` whose method names
   `Function[Cons[a], any]`, whose method names `Cons[a]` before it is complete (its Go func type
   cannot be made yet). `desc` keeps the pending interfaces (`*iface-pending*`) and gives each its
   methods once the type that started it is complete; a signature built then may add more.
3. **What compiled code sees of shaped values** (§5.7, G8): a shaped value given to compiled code
   is its RT, its recursive parts their shapes: `fmt`'s `%v` of a `Tree` prints by its `String`
   method as gc's, but of a `[]S`'s inner elements their shapes' words (no program of the check
   prints one; GC39).
4. **The box's console** (§11.5): TamaGo's runtime writes a program's file descriptors 1 and 2 to
   the board's console (`write1`, `goos.Printk`), which the board sends to the launcher's standard
   error (`abi.Message`), so in the box a Go program's standard output and error are one stream on
   the launcher's standard error; jrt's own streams (Arbace's `*out*`) are the launcher's two. The
   check in the box compares that stream with gc's standard output followed by its standard error
   (exact for a program writing one stream, as almost all do; GC35).
5. **A nil dereference in the box** is the board's `sigpanic` (TamaGo has none of its own), whose
   run-time error is `board.memoryError`, gc's message; on Linux it is a `runtime.errorString`.
   Interpreted code sees the same message and `recover`s it the same; `%T` and a `GoPanic`'s
   `:go/type` name the board's type (the box oracle's one known case of `forms/go.ae`; GC40).

**Proposals for the user** (each built as recommended):

- **GC35 Go's standard output in the box.** (a) as built, the check compares the box's console as
  one stream against gc's standard output then error (amendment 4); (b) the box's runtime overlay
  (`bin/lib/box-overlay.bash`, as it patches `syscall/fs_tamago.go`) patches TamaGo's `write1` to
  send file descriptor 1 to the launcher's standard output, through a hook the box's jrt host sets
  (the board's `Printk` has no descriptor), so that the box's Go programs' streams are Linux's.
  *Recommended: (a)*: the box runs no Go program but Arbace, whose streams are right; (b) when one
  does (a tool guest's output, goi's programs at the box's REPL).
- **GC36 The box's subset.** (a) as built, `test/goi/box.txt`: the essential subset but the
  programs the box cannot run by design (processes, the toolchain, the host's environment, a
  build constraint without tamago), 566 programs, about 5 minutes with 8 boxes; (b) every program
  of the check but the same exclusions (about 1,100, about 10 minutes); (c) a part of the
  essential subset (about 200, 2 minutes). *Recommended: (a)*: the essential subset's rule, every
  milestone's new programs with it, at the cost of the box's oracle and suite together.
- **GC37 The essential subset with GR7a's programs.** (a) as built, GC28's rule applied to the ten
  GR7a made pass (576 programs); (b) unchanged (566). *Recommended: (a)*.
- **GC38 Recursive types' representation.** (a) as built, shapes: the type's Go memory type its
  shape, its frames' type its RT, the bytes reinterpreted where values cross (amendment 1); (b)
  GR3's erasure at the container (a `[]T` held as an opaque header, its RT made when first
  asked, as an erased pointer's): every operation allocating through the container's type would
  need the element's RT, and the elements of `type S []S` have none; (c) frames hold shapes too (no
  reinterpretation): `append`, `copy` and conversions by reflect would see a slice's elements at
  the shape's element type (`unsafe.Pointer`) and refuse or misplace them. *Recommended: (a)*.
- **GC39 Shaped values in compiled code.** (a) as built, compiled code sees a shaped value's RT,
  its recursive parts at their shapes (amendment 3: `%v` of nested elements prints their words), a
  known difference no program of the check meets; (b) G8's runtime overlay for named types (GS8),
  when built, gives them their own `rtype`s. *Recommended: (a)*, (b) with GS8.
- **GC40 The box's nil dereference.** (a) as built, the box oracle's known case (amendment 5); (b)
  the board's `sigpanic` panics with the runtime's own error (`runtime.panicmem`'s, by a
  linkname), so `%T` is `runtime.errorString` in the box too. *Recommended: (a)*: a box change,
  for the box's work; goi's programs and Arbace's catch see the same message and class.
- **GC41 Interfaces naming types being built.** (a) as built, interfaces' methods given after the
  outermost descriptor (amendment 2); (b) every function descriptor's Go type made when first
  asked (a lazy `TFunc`, as an erased pointer's RT): more checks on every call path.
  *Recommended: (a)*.
- **GC42 Deep recursion.** (a) as built, `peano.go` known (S23's limit of 100,000 interpreted
  calls, `ARBACE_GOI_DEPTH`); (b) a higher default in program mode (a million: gc's 1 GB stack
  holds `fact(9)`'s 362,880 frames; goi's Go stack per interpreted call is larger, so a run that
  deep would need `debug.SetMaxStack` raised too). *Recommended: (a)*: one program of 1,178.

**Checks** (this branch, amd64): the essential `bin/gate` passed (14 m 58 s; goi's essential
check 576 of 576, the suite, the oracle 22,620 of 22,673 against its 53 known, the differential
check, the generated variants); `bin/arbace-go --fixpoint` holds (G1 = G2 = G3, 135,557,280
bytes); `bin/goi check`, `bin/goi test --gc` (15 programs, 68 REPL checks, §1.4's session, the
checker's 19 assertions and its 10 files of invalid forms, 68 messages, gc's), `bin/goi annotations` (the five corpora as GR6's:
zero errors, every declaration checked); the box: `bin/goi check --box` (565 of 566, one known),
the box's oracle (22,166 of 22,220, its 54 known: GR6's 53 and GC40's), its smoke test (113
checks).

**Remaining** (for GR7's other parts, or the box's): the check in the arm64 box (KVM/arm64 in the
emulated aarch64 VM, `bin/box-arm64-vm`, and Hypervisor.framework on the Mac: out of GR7a's
scope; `bin/goi check --box` takes `GOI_BOX_EXE`, but the VM's runs would need its own driver and
the emulation's time, about 30 times the host's); GC35 (b) if a box Go program's output matters;
the board's `task: no idle vCPU` when a program sets `GOMAXPROCS` above the box's vCPUs (a box
limit: TamaGo's runtime asks for an M per P); GR7b's errorcheck programs and `reify`; staging
(G16), the runtime overlay for named types (G8, GS8) and speed (F10) as §12 lists.

## As built: GR7b (2026-10-19)

GR7b, part of GR7 (§12: `errorcheck` programs; `reify` of listed Go interfaces, G13), is built on
branch `goi-gr7b` (from main `90ec844d`, seed `1d5c35b4`, which has GR1 to GR6; GR7a merged in at
`81cd54ed`). GR7a (goi in the box, recursive types) was built beside it; its proposals are GC35-GC42,
this part's GC45-GC54.
Where building it found otherwise than this text, the text below is the record.

### Errorcheck programs (§11.6)

**The corpus.** `$GOROOT/test`'s error tests of one program, `linux-errtest`: the files of Go's
test driver's directories whose action is `errorcheck` or `errorcheckwithauto` (whatever their
flags), with no other GOEXPERIMENT and matched by go/build (`gocmp tests -errorcheck`, `bin/repr-go
check --list-errorchecks`): 659 programs. `bin/repr-go read --corpus linux-errtest` dumps them
(`godump -files`: a program with type errors is dumped with them, its go/types errors in the
dump's `:errors`) and converts them in mode `:lines`: 586 dumped (139 without type errors, 447
with), 73 that go/parser rejects not dumped, 522 converted, 64 that the converter cannot convert
(an invalid program: a selection without an object, a conversion's arity, `x.(type)` outside a
type switch, a constant without a value). godump's change: an import path the go command cannot
list (`test/import6.go`'s `"\x00"`, `""`) is not given to `go list` (`listablePath`).

**Checking.** `arbace.go.run --errorcheck [--lang goV] ROOT NS` checks the package as program
mode does (`repl/check-source`, the table's digest for listed packages) without building or
running it, and prints its errors as gc does with `-C` (`base.FlushErrors`): `FILE:LINE: MESSAGE`,
by position, an error equal to the one before it once; a language-version error with the version's
origin as gc's noder adds it (`(file declares //go:build go1.21)`, `(-lang was set to go1.12;
check go.mod)`). Status 1 with errors, 0 without; 125 unsupported, 126 goi's error, as a run's.

**The judge (GC45).** `tools/gocmp errorcheck [-auto] FILE.go OUTPUT` is Go's test driver's
judge, vendored from TamaGo's go1.27.1 `src/cmd/internal/testdir/testdir_test.go` (`splitOutput`,
`errorCheck`, `wantedErrors`, `matchPrefix`, `partitionStrings`, `replacePrefix`, the `// ERROR`
and `// ERRORAUTO` regexps; Go's BSD license, LICENSE.md): line by line, the errors of each
`// ERROR "re"...` line must match each regexp (Go's `regexp`, on the message after
`FILE:LINE: `), and no error may be left unmatched; a test that expects the compilation to fail
must have an error. `-dump-errors DUMP` prints a dump's go/types errors in the same format.

**Per program** (`bin/goi check`'s `check_err`):

1. the action's flags: `-t N`, `-e`, `-d=panic` and `-lang=goV` (given to the checker) are taken;
   one of gc's later phases makes the program unsupported with its reason: `-m`, `-l`, `-live`,
   `-wb`, `-N`, `-smallframes` (escape analysis, inlining, liveness), `-d=...` (the SSA back end's
   debug flags), `-race`, `-std`, `-+`, `-complete`, `-asmhdr`, `-spectre`, `-goexperiment`;
2. a file with line directives (`//line`, `/*line`) is unsupported: gc's positions follow them;
3. gc's reference: the original compiled as the test driver compiles an errorcheck test (`go tool
   compile -p=p -d=panic -C -e -importcfg=STD FLAGS -d=ssa/check/on FILE`, the standard library's
   importcfg from `go list -export std`), judged; cached by the file's and the toolchain's hashes
   (`.tmp/goi-check/eref/`). A program gc's own errors do not pass is skipped (3: their messages
   changed since);
4. no forms: unsupported, syntax errors (go/parser's) or the converter's failure, with its message;
5. goi's checker (`--errorcheck`), judged: pass; else, when go/types (the dump's `:errors`)
   reports no error either, unsupported: the expected errors are gc's phases' after type checking
   (its noder's, the SSA back end's: `stack frame too large`, `misplaced compiler directive`),
   which goi does not have (GC46); else fail.

**Results** (`bin/goi check`, the 659 errorcheck programs with the 1,178 run programs and
directory tests; the reference `test/goi/check-linux-amd64.edn` recorded):

| result | errorcheck programs (659) |
|---|---:|
| pass | 337 |
| unsupported | 281: 109 a flag of gc's later phases (`-m` 59, `-d=...` 20, `-l` 9, `-race` 5, `-live` 5, `-std` 3, `-N` 3, `-+` 3, `-e=0`, `-complete`), 70 syntax errors, 64 repr/go cannot convert the invalid program, 24 errors of gc's phases after type checking, 7 line directives, 7 an import neither listed nor interpretable |
| known | 13 |
| fail | 23 |
| error | 2 |
| skip | 3 (gc's own errors no longer pass the judge) |

The run programs and directory tests are unchanged (1,049 pass, 60 known, 54 unsupported, 15
skipped, as GR7a recorded them); the reference gains the 659 errorcheck programs and nothing else.
The whole check takes 54 s with 16 jobs on the compiled executable. `test/goi/essential.txt`
(GC50) gains 123 errorcheck programs: 699 programs with GR7a's, 39 s with 8 jobs.
`bin/goi run`'s action line now skips blank lines before it, as Go's test driver does (a
`//go:build` line followed by a blank line and the action).

**What the corpus made the checker do** (types2's behaviour and messages, as gc prints them):

- **Lines of literals** (SPEC.md §10.1): a literal that begins a Go line carries no `:line`; its
  line is its parent's `:go/breaks` (`e/child-line`: the line after its predecessor's last, or
  the line recorded), computed only when an error needs it (`e/*child-at*`, `with-elem`): for
  statements of a list (an implicit return's value), composite literals' elements, keys and
  fields, call arguments; struct fields, type specs of a group and select clauses at their own
  lines.
- **Follow-on errors** (types2's `report`): after a first error, one whose message names an
  invalid operand or an invalid type (not at its beginning) is not reported.
- **Declarations**: a name repeated on the left of `:=` is a dummy, not declared; a redeclared
  local is not reported unused; `init` and `main` (of package main) that are not functions,
  `import init "fmt"`, a bodiless `func init`; a package-level object named as a file's import or
  a dot-imported name (`already declared through import of package fmt ("fmt")`); duplicate
  parameters of function types and interface methods; a blank interface method; duplicate
  methods through embedding (at the interface); a duplicate struct field not added; `const x =
  nil`.
- **Expressions**: conversion errors at the operand, with the interface's missing-method cause;
  a pointer to an interface's cause; incomparable causes (`struct containing []int cannot be
  compared`); `nil == nil`; a switch on an incomparable tag invalidates it; struct literals
  (`too many values` at the value, `mixture of field:value and value elements`, blank fields
  never found, unknown keys the converter wrote as `[k v]`, duplicate keys only of fields found,
  no hint for a field's elided type: `missing type in composite literal`, its elements used);
  duplicate constant keys of an interface key type by their types; complex array lengths with a
  zero imaginary part; `append` with `...` through the arguments' check (`not enough arguments
  in call to append`), `copy`'s and `append`'s go1.27 messages (`invalid copy: argument must be
  a slice; have untyped nil`); a builtin's arguments checked before their count, `invalid
  operation: ` prefixed; unsafe's builtins discard results (`is not used`, `defer discards
  result of`); `new(len)`; a package used alone (`use of package fmt not in selector`, the import
  then unused); a variable named where a type is expected is used; `range` causes (`receive from
  send-only channel`, `requires go1.22 or later`, `func must be func(yield func(...) bool):
  ...`, a variadic yield); method expressions `(*T).M`, `struct{}`, `(*T)(x)` printed as gc
  does; go/constant's float strings (`1e+200`, `0.9999999`, beyond float64 `1e+646456992`).
- **Branches** (gc's `syntax.checkBranches`): a label named by `break` or `continue` is used even
  when not a valid target; `invalid break label` for a label not enclosing; the block of an
  `else` (and of an `else if`, holding the rest of the chain) starts at its own line.
- **Type switches**: `(type-switch [t x])` without clauses is a guard, not an init; `_ :=` is
  `no new variable on left side of :=`.

None of these changes the run programs' results or the annotation check (§11.4, every count as
recorded).

**Known differences (GC49)**, in `test/goi/check-known.edn` with their reasons: `byte` and `rune`
named `uint8` and `int32` (goi's types identify them; 3 programs), aliases named by the type they
denote (1), literals' spellings, which mode `:lines` does not keep (`1e200` is read as `1.0E200`;
4), a position's column in a message (1), the line of a composite literal's closing brace (1), a
type spanning lines in a tag (1), a function's final expression statement, which the forms write
as its return (SPEC.md §6.4; `test/return.go`), repr/go dropping a conversion's `...` (1).

**Remaining failures**, recorded as such in the reference (a regression is a program passing
there and not now): invalid recursive types and cycles goi's checker does not detect or words
otherwise (`bug195`, `bug224`, `bug251`, `bug256`, `bug398`, `bug464`, `issue21273`, `issue23823`,
`issue41575`, `issue48301`, `issue75022`, `initloop.go`'s cycle detail, `typeparam/issue48711.go`'s
instantiation cycle); interfaces with type constraints used as types (`issue10975`, `issue11614`);
`bug217`'s receiver errors, `issue28079b`, `issue28079c` (a shift in `complex`), `issue48471`
(missing-method causes with `have`/`want` of a differently cased method), `issue8183` (errors of
an implicitly repeated constant at its spec, go/types' `errpos`), `goto.go`'s last case (an else
block's line the forms do not keep), `interface/explicit.go`, `typeparam/issue51232.go`; and two
crashes of the checker on huge constants and sizes (`issue20227`: a complex division by
1e-600000000; `issue59190`: sizes overflowing int64, go/types' `too large`).

### reify of Go interfaces (G13)

G13 (a), decided at the survey: `reify` of a listed Go interface through its wrapper, after GR5.

**From Arbace.** `reify` (`arbace.core`, hand change 29 of doc/VENDOR-NOTES.md) takes among its
specs a Go interface of goi's table: a symbol naming a `GoType`'s var, its namespace required
(`go.io/Reader`, `io/Reader` with an alias):

```clojure
(require '[go.io] '[go.sort])
(defn chunks [cs]
  (let [left (atom cs)]
    (reify go.io/Reader
      (Read [_ p] (if-let [c (first @left)]
                    (do (swap! left rest) [(go.demo/Put p c) nil])
                    [0 @go.io/EOF])))))
(go.demo/ReadAllString (chunks ["ab" "cd"]))          ;=> "abcd"
(go.sort/Sort (reify go.sort/Interface (Len [_] ...) (Less [_ i j] ...) (Swap [_ i j] ...)))
```

Such a spec's methods are fns of the object and the Go arguments (converted to Arbace by their
Go types, §5.6), their results converted back by the result types (several as a vector); in a
method's body `recur` passes the arguments, as for a Java method. Other specs (Java
interfaces, `Object`, protocols) are reify's as before, and `this` is the same object for all.
The object also implements `arbace.lang.GoReify`, whose `goiReify` is `[T f0 f1 ...]`: T the
reify type of its Go interfaces (`arbace.go/reify-type`, one per list) and fi the fn of T's
method i (`arbace.go/reify-data`, which requires every method of the interfaces and none
else: `reify: method Less of sort.Interface is missing`).

**In Go.** The reify type (`arbace/goi` `ReifyType`) is an interpreted named type
`arbace.reify[I, ...]` (`Reify` set) whose method set is the interfaces' methods (by name, one
signature each), each an interpreted function whose code (`reifyMethodCode`) reads the receiver
(the Arbace object) and the arguments from its frame, asks the bridge for the object's fns
(`GoBridge.reifyOf`), calls fn i with the object and the arguments converted to Arbace
(`callArbace`), and writes its results. So every path that calls an interpreted method works
unchanged: compiled code through the interface's wrapper (`EnterMethod`, GC22), interpreted
code's method calls, method values, type assertions and switches (the method set). At the
boundary (`FromArbace`, §5.6) an Arbace reify for a parameter or variable of a compiled interface
type I is a proxy of T: in an `any`, the proxy (`contentOf`: fmt sees a `Stringer`, an `error`);
in I, I's wrapper of the table (the object must have I's methods, by name and signature;
`error` and `fmt.Stringer` goi's own); an interface without a wrapper is an error. A proxy of a
reify type going to Arbace is the object itself (`ContentToArbace`), so `(identical? r (Id r))`.

**Tests.** `test/goi/repl.ae`, 8 checks (78 in all pass): a `Stringer` through `fmt.Sprint`, an
interpreted function taking a `fmt.Stringer`, an assertion in interpreted code, the object back
as itself, `this` with an Arbace interface too, an `io.Reader` read by compiled `io.ReadAll` (the fn
writing the Go slice through a Go forms function), `sort.Sort` of an Arbace vector, a missing
method's error; oracle cases at the end of `test/oracle/forms/go.ae`
(17, recorded by `bin/oracle record --goi 'target/arbace-go/amd64/arbace -'`, reviewed: 129 of 129
match).

### Measurements

- **Size.** The executable is 136,446,112 bytes against main's 135,557,280 (with GR7a): +888,832
  (0.66%): the checker's additions (c2j's Go of their Arbace), goi's reify code, the table's
  `RegisterIfaceMethods` calls. A first version asked reflect for the interfaces' methods
  (`reflect.Type.Method`), which made the linker keep every exported method of the program (S39):
  +18.8 MB; the methods' names and types now come from the table (GC54's note).
- **The annotation check** (§11.4): every corpus as recorded, in 44.6, 37.7, 41.9, 29.9 and 23.9 s
  (GR6's 35-42 s, 26-29 s, 22 s, at a lower load).
- **Speed** (GR3's criterion): `bin/goi run test/goi/run go.t.fib bench`, best of five per run,
  three runs alternating main's executable (GR7a merged) and GR7b's, load about 5:

  | | main (GR7a) | GR7b |
  |---|---|---|
  | fib 22 | 2.56-2.62 ms | 2.55-2.65 ms |
  | sumSq 1e6 | 19.47-21.17 ms | 19.84-21.34 ms |
  | sieve 1e6 | 69.74-71.10 ms | 66.71-67.50 ms |

  against GR6's 2.55, 17.9 and 65.5 ms (at load 16-18): within the runs' noise; the interpreter's
  core is unchanged.

**Checks** (this branch, amd64, after GR7a's merge): the essential `bin/gate` passed (7 m 21 s;
goi's essential check 699 of 699); `bin/arbace-go --fixpoint`: G1 = G2 = G3 (136,446,112 bytes);
`bin/goi check` as recorded, `--essential`, `bin/goi test` (the checker's tests, 15 programs, 78 REPL
checks, §1.4's session), `bin/goi annotations`, `make check-makefile`, `bin/generated-check`, and
the oracle's `forms/go.ae` (129 of 129). Not run: `bin/gate --full` (its box oracle now has the
17 reify cases too: GR7a recorded 111 of 112 matching in the box).

### Proposals for the user (each built as recommended)

- **GC45 The errorcheck judge.** (a) as built, Go's test driver's own code vendored in
  `tools/gocmp` (Go's regexp, its partition by line, its unmatched errors), goi printing its
  errors as gc does with `-C`, gc's own run the reference; (b) a judge in Arbace
  (`java.util.regex`, not RE2: a few of the patterns differ). *Recommended: (a)*.
- **GC46 Errors of gc's phases goi does not have.** (a) as built, unsupported: an action flag of
  those phases, or errors go/types does not report either (decided per program from the dump's
  go/types errors); (b) emulate gc's noder checks (directives, `go:linkname`, import paths) in
  goi's checker. *Recommended: (a)*, (b) when goi needs them for its own programs.
- **GC47 Invalid programs repr/go cannot convert** (64, and 73 with syntax errors). (a) as built,
  unsupported with the converter's or the parser's message; (b) teach the converter forms for
  invalid code (a selection with no object, a conversion of several arguments). *Recommended:
  (a)*: the forms are for valid Go.
- **GC48 The converter's annotations in an invalid program.** (a) as built, their disagreements
  are not errors in errorcheck mode (they are go/types' types of an invalid program); (b)
  reported. *Recommended: (a)*; `bin/goi annotations` keeps verifying them on valid programs.
- **GC49 What the forms of mode `:lines` and goi's types cannot say** (spellings, columns,
  closing braces, `byte`, aliases). (a) as built, known, with reasons; (b) errorcheck over mode
  `:full`'s forms (spellings, columns) and `byte`/`rune`/aliases as types of their own in the
  checker, a later step. *Recommended: (a)* now.
- **GC50 The essential subset with errorcheck programs.** (a) as built, the passing errorcheck
  programs by GC28's rule (every one outside `fixedbugs/`, every fourth of `fixedbugs/`); (b)
  none. *Recommended: (a)*.
- **GC51 The spelling of a Go reify.** (a) as built, Arbace's `reify` takes Go interfaces among
  its specs (the survey's `(reify go.io/Reader (Read [_ p] ...))`); (b) a macro of its own,
  `go/reify`, leaving the vendored `reify` as upstream's. *Recommended: (a)*: one reify, mixing
  Go and Java interfaces.
- **GC52 What a reify is in Go.** (a) as built, a proxy of a reify type per list of interfaces,
  the object as its value, its methods interpreted functions calling the object's fns (every
  path of interpreted methods unchanged, the object back to Arbace as itself); (b) a Go value made
  when the reify is (a GoValue of the interface): Arbace would hold a Go value, not its object.
  *Recommended: (a)*.
- **GC53 Interfaces of Go forms (interpreted).** (a) as built, only listed compiled interfaces
  (G13's letter; a signature naming an interpreted type has no reflect type); (b) interpreted
  interfaces through their descriptors. *Recommended: (a)*, (b) when asked for.
- **GC54 Method calls of a reify cross the bridge.** (a) as built, each call of a reify's method
  asks the bridge for the object's fns (one Java interface call) and converts the arguments by
  their Go types; (b) the fns cached in the proxy. *Recommended: (a)*: a proxy's value must stay
  the object itself for Go's `==` and for the way back to Arbace.

## As built: GR7c (2026-10-19)

GR7c, part of the plan to the milestone `arbace-interprets-go-v1` (doc/AGENDA.md: every program
of `bin/goi check` passes or is a divergence by design with its reason), is built on branch
`goi-gr7c` (from main `0ca375c8`, GR7a and GR7b merged). Its proposals are GC85-GC94. GR7b left
23 errorcheck programs failing and 2 crashing the checker; GR7c ports go/types' (types2's, go1.27.1)
behaviour and messages for them, from TamaGo's go1.27.1 `src/cmd/compile/internal/types2`
(`decl.go`, `cycles.go`, `validtype.go`, `initorder.go`, `mono.go`, `gcsizes.go`,
`builtins.go`, `lookup.go`, `typexpr.go`, `typeset.go`, `literals.go`, `errors.go`) and gc's
noder (`cmd/compile/internal/noder/irgen.go`, cycleFinder). Where building it found otherwise
than this text, the text below is the record.

### Cycles

- **cycleError** (`code/cycle-error!`): a cycle of one is `invalid recursive type: T refers to
  itself` (`invalid cycle in declaration: x refers to itself` for values); a longer one is
  `invalid recursive type T` with a line per reference, from the cycle's first object in the
  source around: `\n\tFILE:LINE: T refers to U` (types2's `addf` lines; gc prints
  `FILE:LINE:COL`, the forms keep lines: GC85).
- **directCycles** (go1.27's `cycles.go`, `decl/direct-cycles!`): before the declarations are
  checked, package-level type declarations whose RHS is a type name of the package scope are
  followed; a cycle (`type a b; type b c; type c b`, `type T T`) is reported and its closing type
  made invalid (its object black, never declared).
- **validType** (`code/valid-type!`): a named type containing itself without indirection,
  through array elements, struct fields, union terms, embedded interfaces and instances' type
  arguments (`nest` and `path` as go/types keeps them: `A refers to T[A]`), for package-level
  and local types; the first type of the cycle made invalid. The checks are queued in the order
  the type declarations began (go/types' `later` at the start of `typeDecl`) and run after the
  bodies, before the declarations' other laters (GC86). An interface type carries its embedded
  types as metadata, `:embeddeds`, types being compared by value (GC87). An instance records
  the position of its first instantiation, for its line in a cycle.
- **Initialization cycles**: `findPath` and `reportCycle` (initorder.go): `initialization cycle
  for a` with `a refers to b` lines.
- **Locals and functions**: a local alias is in scope at its name (`type T = T`: invalid
  recursive type); a function named while its signature is checked is no cycle (types2 sets
  the signature first as a guard); where a type is wanted, an object that is no type name is
  not declared and named with its kind (go1.27: `foo (function) is not a type`).
- **Interfaces embedding a type in progress** (a cycle): the element is left out (go/types'
  `computeInterfaceTypeSet` ignores an invalid element) and the interface is marked
  `:incomplete`, which implements and is implemented (go/types' `hasAllMethods` with invalid
  embedded fields), so the cycle's error is the only one (`bug251`; GC88).
- **Anonymous interface cycles** (gc's noder, go.dev/issue/56103): after a check without
  errors, an anonymous interface embedding a type in progress whose methods reach it again
  through unnamed types is `invalid recursive type: anonymous interface refers to itself (see
  https://go.dev/issue/56103)`, once per outermost interface literal (GC89).
- **Instantiation cycles** (mono.go, `decl/monomorph!`): the graph of type parameters' flows,
  recorded by every satisfied instantiation (its explicit type arguments' lines), receivers'
  type parameters canonical to their type's, local types parameterized by the type parameters
  in scope; checked when no error was found: `instantiation cycle:` with `T instantiated as
  []T` lines (GC90).

### Constants, sizes and the rest

- **Constants** (GC91): a binary float's exponent is big.Float's (MinExp, MaxExp: int32):
  below, the value underflows to 0; above, it is go/constant's Unknown (an infinite
  big.Float); an Unknown operand makes an operation Unknown. go/types' check of a complex
  divisor whose parts' squares underflow: `division by zero` (`issue20227`, which crashed).
- **Sizes** (GC92): gcSizes' -1 for sizes and offsets beyond int64 (`Sizeof`, `Offsetsof`,
  `align` overflowing), `unsafe.Sizeof` and `Offsetof`'s `X is too large` (`issue59190`, which
  crashed).
- **Assignability** is go/types' static `implements`: an interface value assigned to an
  interface whose methods it lacks is an error (GR2's check let it pass); a conversion's cause
  is assignableTo's (`: need type assertion`, `: I does not implement I2 (missing method N)`);
  a method found with another case is the `missing method M` cause with `have m(int)` and
  `want M(int)` (`wrongName`), `unexported method M` when the name is the same.
- **errpos**: the errors of a constant with an inherited initializer are at its name's line.
- **An instance with a wrong count of type arguments** is still an instance, its underlying
  invalid (go/types' `expandUnderlying`; GC93), so further messages name it (`cannot use
  &concreteF[RCT]{…} (value of type *concreteF[RCT]) as F[RCT] value`); a composite literal of an
  invalid underlying type uses its elements and is a value of the type; `G[T]{…}` in messages.
- `complex()` of untyped arguments one of which is not constant converts both to float64 (the
  shift's `shifted operand 1 (type float64) must be integer`); `array length X must be
  constant` with the operand once; the version errors of embedding a non-interface type or an
  interface element before go1.18; a method without a receiver is an error and a function;
  rune literals print as `strconv.QuoteRune` does (`'\\'`, `'\n'`).

### Results

GR7b's 25 programs:

| program | GR7c |
|---|---|
| `bug195`, `bug224`, `bug251`, `bug256`, `bug398`, `bug464`, `issue10975`, `issue20227`, `issue21273`, `issue23823`, `issue28079b`, `issue28079c`, `issue41575`, `issue48301`, `issue48471`, `issue59190`, `initloop.go`, `interface/explicit.go`, `typeparam/issue48711.go`, `typeparam/issue51232.go` | pass (20) |
| `bug217` | known: two receivers `(a b, c d)` are written `[^b a ^d c]`, a receiver and a parameter (SPEC.md §6.4): the forms say another program (its line 12, a method without a receiver, passes) |
| `issue11614` | known: `(interface{int})`'s parentheses, which mode `:lines` does not keep (and goi abridges a type literal in an expression as go/types' ExprString does) |
| `issue75022` | known: an alias named by the type it denotes in an instance (`T[B]` printed `T[C]`), GC49's class; its three other cycles pass |
| `issue8183` | known: `byte` named `uint8`, GC49's class; its lines (errpos) pass |
| `goto.go` | known: the line of an else block's brace (`} else {`, a line without a statement), which mode `:lines` does not keep |

Three programs that were unsupported pass: `issue16369`, `issue49368` and `issue56103` (the
anonymous interface cycles). GR7b's 13 known errorcheck programs were reviewed: `issue43762`
(rune literals' spelling) now passes, its entry removed; the 12 others stay known for the
reasons GC49 decided, each checked to fail only for its reason (`byte`, `rune` and aliases'
names: `alias`, `convert1`, `copy1`, `alias2`; spellings mode `:lines` does not keep:
`issue19947`, `issue56220` (also its parentheses), `issue31747`; a column: `issue68292`; a
closing brace's line: `issue23732`; a type spanning lines in a tag: `funcdup2`; the forms of
an invalid program: `return.go`, `ddd1.go`).

**The check** (`bin/goi check`, 1,837 programs), before main's GR8a and GR9d were merged in:
1,410 pass, 0 fail, 0 error, 332 unsupported, 77 known, 18 skipped (GR7b: 1,386, 23, 2, 335, 73,
18), 55 s with 16 jobs. After the merge (`2fda8588`), recorded from a fresh run
(`test/goi/check-linux-amd64.edn`, its diff against main's exactly the 29 programs above: the
25, `issue43762`, the 3 unsupported): 1,438 pass, 2 fail, 0 error, 296 unsupported, 83 known, 18
skipped; the 2 fails are GR8a's, recorded as such there (`issue30116`, `issue30116u`, goi's
bounds messages of run programs, not the checker's). With 16 jobs on a loaded machine (load
20-40) `fixedbugs/bug358.go` (an errorcheck program importing interpreted std packages, about
8 s alone, main's executable as GR7c's) and `nosplit.go` exceeded the check's 10 s; the
reference was recorded with `--jobs 8` (119 s). `test/goi/essential.txt` gains, by GC50's rule,
the 9 errorcheck programs GR7c made pass outside `fixedbugs/` (4) and every fourth of
`fixedbugs/` (5): 721 programs with GR8a's, 721 pass. The checker's tests gain
`test/goi/errors/cycles.ae`, `mono.ae` and `ordered.ae` (gc agrees, `bin/goi test --gc`); a
message's lines of positions are left out of their comparison, as `gc_errors.ae` leaves gc's
out. The oracle's `forms/go.ae` case of `cmp.Compare(true, false)` is recorded again (GC144 (b),
reviewed: the only change, 150 cases).

### Measurements

- **Size.** Before the merge the executable was 136,941,728 bytes against main's 136,446,112
  (`0ca375c8`): +495,616 (0.36%), the checker's additions; after it 143,007,904 against main's
  142,508,192: +499,712.
- **The annotation check** (§11.4): every corpus as recorded, 0 errors, in 42.7, 37.2, 43.3,
  28.2 and 27.0 s (after the merge).
- **Speed** (GR3's criterion): `bin/goi run test/goi/run go.t.fib bench`, three runs
  alternating main's executable (`0ca375c8`) and GR7c's, load 11-18:

  | | main | GR7c |
  |---|---|---|
  | fib 22 | 2.58-2.91 ms | 2.54-2.84 ms |
  | sumSq 1e6 | 19.47-22.24 ms | 20.33-21.31 ms |
  | sieve 1e6 | 69.92-70.32 ms | 69.52-70.10 ms |

  within the runs' noise; the interpreter's core is unchanged.

**Checks** (this branch after the merge, amd64): the essential `bin/gate` passed (4 m 19 s, its
build cached; goi's essential check 721 of 721, the oracle as recorded, `bin/generated-check`,
`make check-makefile`); `bin/arbace-go --fixpoint`: G1 = G2 = G3 (143,007,904 bytes); `bin/goi
check` as recorded, `--essential`, `bin/goi test` (22 checker assertions, 17 programs, 101 REPL
checks, std's 183, §1.4's session) and `--gc`, `bin/goi annotations`. Not run: `bin/gate --full`.

### Proposals for the user (each built as recommended)

- **GC85 Positions in a cycle's lines.** (a) as built, `FILE:LINE:` (the forms keep lines; the
  judge's patterns accept them); (b) columns from mode `:full`'s forms. *Recommended: (a)*.
- **GC86 When validType runs.** (a) as built, go1.27's directCycles first, then validType queued
  in the order the type declarations began, after the bodies (which cycle is reported, and from
  which type, as types2); (b) a later of each declaration in source order (GR7b's: another type
  of the cycle may be reported). *Recommended: (a)*.
- **GC87 Embedded types for validType.** (a) as built, an interface type's `:embeddeds` as
  metadata (types still compared by value, nothing else changes); (b) a fourth element of the
  interface vector, every match of interface types changed. *Recommended: (a)*.
- **GC88 An interface embedding a type in progress.** (a) as built, the element left out, the
  interface `:incomplete` and implementing both ways (only cycles, all reported, lead there);
  (b) lazy type sets as go/types computes them, a larger change of goi's types. *Recommended:
  (a)*, (b) when a valid program needs it.
- **GC89 gc's anonymous interface cycles.** (a) as built, the noder's cycleFinder ported, after
  a check without errors as gc runs it (three programs pass, `issue56103` among them, which
  GC46 had made unsupported); (b) unsupported, GC46's rule. *Recommended: (a)*: goi's
  structural types cannot represent such an infinite type; GC46 stays for gc's other phases.
- **GC90 The monomorphization check.** (a) as built, mono.go ported, when no error was found;
  (b) none: an instantiation cycle accepted, and goi's interpreter, which instantiates at run
  time, would instantiate without end. *Recommended: (a)*.
- **GC91 Constants beyond big.Float's range.** (a) as built, go/constant's (underflow to 0,
  overflow Unknown); (b) unbounded exponents (GR7b's; the complex divisor underflows to no zero
  and the check differs from gc's). *Recommended: (a)*.
- **GC92 Sizes beyond int64.** (a) as built, gcSizes' -1 and go/types' `is too large`; (b)
  arbitrary-precision sizes. *Recommended: (a)*: gc's sizes are int64.
- **GC93 An instance with a wrong count of type arguments.** (a) as built, an instance with an
  invalid underlying type, as go/types; (b) an invalid type (GR6's: fewer follow-on errors than
  gc, `issue51232` fails). *Recommended: (a)*.
- **GC94 The five programs left.** (a) as built, known with their reasons, GC49's classes
  (the forms of mode `:lines`, `byte` and aliases in goi's types); (b) errorcheck over mode
  `:full`'s forms and `byte`, `rune` and aliases as types of their own (GC49 (b)), a later step.
  *Recommended: (a)*.
- **GC144 (b), decided (GR9d's proposal, built here).** A constraint's terms in messages in their
  written order for the digest's packages too: `terms-union` and `terms-intersect` go through
  terms-norm's `:order`, not their set's (`bool missing in ~int | ~int8 | ... | ~string`, as
  types2 writes cmp.Ordered's); `test/goi/errors/ordered.ae` compares the message with types2's.

## Decided (2026-10-19): GR9d, generic functions at the REPL

GC34 is amended from (a) to (b): a generic function has a var, whose fn infers its type
arguments at each call. GC72 (a), GC73 (a) and GC74 (a) of doc/go/GR9-SURVEY.md are decided as
recommended, under the user's goal of 2026-10-19 (the milestone `arbace-interprets-go-v1`,
doc/AGENDA.md's plan): Arbace scalars are untyped constants of their kinds and Go values keep
their Go types for inference (GC72); `(go/inst f T...)` gives explicit type arguments, partial
instantiation allowed (GC73); instances are cached per var, by the arguments' types, and shared
with the builder's instance cells (GC74).

## As built: GR9d (2026-10-19)

GR9d, the survey's Part 2 (doc/go/GR9-SURVEY.md §4), is built on branch `goi-gr9d` (from main
`04ac458b`, with the GR8 and GR9 surveys). GR9a (runtime types of interpreted named types) and
GR8a are built at the same time from the same base; GR9d keeps to the var, inference from Arbace
values and the cache. Where building it found otherwise than the survey, the text below is the
record; proposals for the user are GC135-GC144.

**What exists.**

| part | where | as built |
|---|---|---|
| the var's fn | `arbace/lang/GoInterp.ae` (`GoGenericFn`) | an `AFn` holding the function's name, its description (`data`) and `resolve`; `keyOf` keys each argument (a `GoValue` its descriptor, `GoInterp/valueType`; a `Long`, `Integer`, `Short`, `Byte`, `BigInt` or `BigInteger` `:untyped-int`; a `Double`, `Float`, `Ratio` or `BigDecimal` `:untyped-float`; a `Character` `:untyped-rune`; a `Boolean` `:untyped-bool`; a `String` `:untyped-string`; `nil` `:untyped-nil`; a collection `:coll`; a keyword or a symbol `:any`; another fn, a `GoFn` among them, `:fn`; any other Java object `:any`); `instanceFor`: the last key's instance (compared element by identity: descriptors are interned), else a `ConcurrentHashMap` by the key as a vector, else `resolve`, kept; the call is the instance `GoFn`'s (`GoInterp/call`, its arguments converted by the instance's parameter types, §5.6); `instanceCount` |
| inference | `arbace/go/generic.ae` (`arbace.go.generic`, new) | `resolve-instance`: the key's operands (`operand`: an untyped kind an untyped constant, `:fn`, `:coll` and nil an untyped nil, which inference skips, `:any` the type `any`, a descriptor its checker type), the parameters for the arguments (a variadic one's elements expanded; an arity error in types2's words), the checker's `rename-tparams` and `infer` (go1.27.1's `infer.go`, GR6) with go/inst's type arguments, `verify!` of the constraints; errors thrown as the REPL's check errors (`Go check errors:` and `:go/errors`, each message `REPL: ...`); then the instance's `GoFn` over `instance-callable` |
| descriptors as checker types | `arbace/go/generic.ae` (`desc->type`, `rehome`), `arbace/go/build.ae` (`desc-types`), `go/arbace/goi/natives.ae` (`GoInterp_ValueType0`, `GoInterp_TypeInfo0`) | a Go value's descriptor: a proxy's type, an interpreted struct's by its RT, else the compiled type's (`descOfRT`); a descriptor's parts (`typeInfo`: kind, named, name, package, element, key, length, parameters, results, variadic, direction, fields, method count); an interpreted named type's checker type is the builder's registry's (`desc-types`: descriptor to the type it was last made for, filled by `named-desc`), taken to its package's last check by `rehome` (a named type by package and name, an instance instantiated again), so that a value made under an older check unifies with the generic declaration's types; a compiled named type the digest's; composites by their parts |
| instances | `arbace/go/build.ae` (`instance-callable`) | the `Callable` of a generic function's or method's instance: `instance-cell` (the cell interpreted callers reach, GR6's amendment 2), its signature substituted, `InterpCallable` or, for a method, `InterpMethodCallable` with the receiver's instance type |
| the vars | `arbace/go/repl.ae` (`define!`, `named-type`, `generic-env`) | every generic function `F` and generic type's method `T.M` (and a generic method of go1.27) of an interpreted package gets a var whose root is a new `GoGenericFn` at each definition of the package (metadata `:go/kind`, `:go/generic true`, `:go/package`, `:line`, `:file`, `:doc`); a method's var takes the receiver first and infers the type's arguments from the receiver's instance type (`List[int]`: `T = int`), its own type parameters from the rest |
| `go/inst` | `arbace/go.ae` (`inst`), `arbace/go/repl.ae` (`inst!`, `resolve-types`), `arbace/go/generic.ae` (`instantiate`, `type-instance`) | `(go/inst f T...)`: `f` a generic function's or method's var, or a generic type of an interpreted or listed package by name (`go.main/List`: a generic type has no var); the type forms resolved by a check of the namespace's package (go.user's outside go.*) with a struct type of fields of those types added (`resolve-types`, as go/eval does), then rehomed; all type arguments: the instance's `GoFn` (a function, a method with its receiver first) or `GoType` (a type); fewer: a `GoGenericFn` with those fixed (Go's partial instantiation); kept per `GoGenericFn` and forms (`:insts`) |
| the interpretable packages from Arbace | `arbace/lang/GoInterp.ae` (`loadTable`), `arbace/go.ae` (`interpretable-ns!`) | `(require '[go.slices :as slices])` loads `slices` (`cmp`, `iter`, `maps`) from the digest as goi does for an import (§9.3), so that its vars exist: RT.load's hook asks `arbace.go` for a `go/` resource of one element that is no listed package |
| the listed packages' generics | `arbace/go.ae` (`digest-info`, `table-ns!`), `arbace/go/repl.ae` (`listed-generic!`) | a listed package's generic function or generic type's method (`sync.OnceValue`, `atomic.Pointer.Load`) gets its var on first use (S9) as a `GoGenericFn` over the digest's declaration, its instances interpreted from the digest's forms (§9.3); not among the vars `ns-publics` makes |
| interop | `arbace/lang/GoInterp.ae` (`methodVar`) | `(.Push l 3)` on an instance's value finds the var `List.Push` (the instance's name without its type arguments), a `GoGenericFn` |
| tests | `test/goi/repl.ae`, `test/oracle/forms/go.ae` | the survey's §4.7 at the REPL (below); 21 oracle cases recorded by goi |

**Arbace values as operands (§4.3 of the survey, as built).** As the survey's table, with three
differences: a keyword or a symbol is typed `any` (a Java object); a `GoFn` (a Go func value in
Arbace) infers nothing, as an Arbace fn (its Go type is in its `Callable`, not asked: GC137);
`(go/spread xs)` is not recognized as a variadic spread (GC138). Values matter only through
conversion: `((go/inst go.cmp/Compare int8) 300 1)` throws §5.6's error
`argument 1 of cmp.Compare[int8]: 300 overflows int8`.

**The REPL's checks** (`test/goi/repl.ae`, the section "generic functions' vars"): the var is a
`GoGenericFn`; `slices.Index` over a Go `[]int` and a `[]int64` (the untyped argument takes the
element type), `slices.Contains`, `slices.Max`; `cmp.Compare` of `Long`s (`int`), of a `Long`
and a `Double` (`float64`), of strings, and `int64` by go/inst; `slices.SortFunc` with an Arbace
fn; the program's own `Map[T, U any]` with go/inst (its `U` cannot be inferred from a fn);
partial instantiation `(go/inst go.slices/Index (slice int64))`; the cache (three calls of two
types, two instances); a generic type's methods by their vars (`List.Push`, `List.Len`), by
interop (`.Push`) and by go/inst (`List.Push string`), the instances shared with an interpreted
caller's; go/inst of a generic type (its zero value); a listed package's generic,
`sync.OnceValue`; a redefinition seen by the var (a new `GoGenericFn`); the errors in the
checker's words: `in call to gvar.Map, cannot infer U (declared at repl.go:?)`, `bool does not
satisfy cmp.Ordered (bool missing in ...)`, `in call to slices.Index, S (type []string) does
not satisfy ~[]E`, `not enough arguments in call to slices.Index` with types2's have and want,
and the overflow above.

**Amendments (as built).**

1. **§9, GR6's REPL row and GC34.** A generic function or method still has no cell (its instances
   have); it now has a var, a `GoGenericFn` (GC34 (b)). §2.1's table of vars gains it: `(F a b)`
   infers, `(go/inst F T...)` instantiates.
2. **A generic type has no var** (GC135): `go/inst` finds it by its namespace and name in its
   package's last check (`named-type`). `(go.main/List int)` stays meaningless, as the survey
   wanted; the survey's "a generic type's var" is not built.
3. **Types of older checks** (GC136). The survey keyed inference by descriptors and assumed their
   checker types. A package is checked anew at every declaration, so the type a descriptor was
   made for may belong to an older check than the generic declaration's: `rehome` takes every
   interpreted named type to its package's last check by package and name, instances
   instantiated again (the checker's instances are interned by origin and type arguments).
4. **The var is made anew at each definition of its package** (GC139), as GR3's function vars
   are: its cache starts empty after any declaration in the package, and the instances it then
   makes are new cells (the instance cells are keyed by the generic object, which a check makes
   anew). The survey's "a redefinition of F" is any definition of F's package.
5. **The interpretable packages from Arbace** (GC142): the survey's
   `(require '[go.slices :as slices])` needed RT.load's hook to know them; it asks `arbace.go`
   for any `go/X` resource of one element that is no listed package.
6. **Messages** (GC140): the checker's words (`cannot infer U`, `S (type []string) does not
   satisfy ~[]E`, types2's arity message), each as `REPL: ...`, an argument named by its literal
   for a scalar, else `argument N`. Their positions are the checker's: `declared at repl.go:?`
   for a type parameter typed at the REPL, whose forms carry no line. cmp.Ordered's terms are
   listed in the checker's set order, not as written (GC144): GR6's amendment 11 holds for
   interpreted constraints, not for the digest's.

**Measurements** (linux/amd64, the executable of this branch, best of five runs of 10^5 calls
after 2 * 10^4 warm-up calls, two runs; the machine shared, load about 6):

| | value |
|---|---|
| a function var's call (`GoFn`): `IndexInts(xs, 5)`, a Go function calling `slices.Index[[]int,int]` | 1.46-1.49 µs |
| a generic var's call, cached: `(slices/Index xs 5)` | 1.66-1.79 µs |
| a generic var's call, cached: `(cmp/Compare 1 2)` | 1.45-1.53 µs |
| the instance by go/inst: `((go/inst go.slices/Index (slice int) int) xs 5)` | 1.34-1.36 µs |
| a miss (a new key: operands, `infer`, verification, the instance's cell and `GoFn`; its code built on its first call) | 1.7-1.9 ms |
| the executable (linux/amd64) | 137,044,128 bytes against 136,446,112 at `04ac458b` (+598,016, 0.44%: `arbace.go.generic`, the class, the natives) |

The survey's target, about 1.5 µs per call once cached, holds: a cached call costs its key (one
native call per Go value argument, `instanceof` tests for the rest) and one identity comparison
over the instance `GoFn`'s call, 0.0-0.3 µs above a function var's.

**Checks** (in the worktree, before the merge): `bin/goi check` 1,837 programs, 1,386 pass,
23 fail, 2 error, 335 unsupported, 73 known, 18 skipped, no regression against
`test/goi/check-linux-amd64.edn` (the fails and errors are errorcheck programs of the reference);
`--essential` 699 of 699; `bin/goi test` the checker's tests (3 tests, 19 assertions), 15 of 15
programs, the REPL's 101 of 101 checks, §1.4's session as its transcript; `bin/goi annotations`
zero errors in the five corpora (counts unchanged: GR9d does not touch the checker);
`make check-makefile`; `bin/generated-check`; the oracle's `forms/go.ae` 150 cases (21 new)
recorded by goi and reviewed; the essential `bin/gate` passed (9m45s: the smoke test, Clojure's suite on the test build, the oracle 22,658 of 22,711 cases with its 53 known mismatches as recorded, the differential check, goi's essential check, the generated variants); `bin/arbace-go --fixpoint` holds (G1 = G2 = G3 byte for byte).

**Proposals for the user** (each built as recommended):

- **GC135 Generic types from Arbace.** (a) as built, no var: `go/inst` names a generic type by
  namespace and name; (b) a var per generic type holding a generic type object whose `go/inst`
  is its instance. *Recommended: (a)*: a value nothing but go/inst can use.
- **GC136 Values of types checked before the generic declaration.** (a) as built, interpreted
  named types rehomed to their packages' last check by package and name; (b) the builder's
  registry's type as it is (an inference after any later declaration in the type's package
  fails: `type *List[int] of argument 1 does not match`). *Recommended: (a)*.
- **GC137 A `GoFn` argument.** (a) as built, it infers nothing, as an Arbace fn; (b) typed by its
  `Callable`'s func type (`slices.SortFunc(xs, strings.Compare)` would infer from it).
  *Recommended: (a)* now, (b) when asked for.
- **GC138 `(go/spread xs)` in a generic call.** (a) as built, not recognized (the spread value
  is typed as a Go value, inference fails); (b) the last argument spread as `call-args` does.
  *Recommended: (a)* now: rare at the REPL.
- **GC139 The cache's lifetime.** (a) as built, a new `GoGenericFn` at each definition of the
  package (as GR3's function vars); (b) kept while the generic declaration's signature is, its
  instances rebuilt. *Recommended: (a)*: one rule for every var.
- **GC140 Errors of Arbace's calls.** (a) as built, the checker's messages as `REPL: ...`,
  arguments named by their literal or `argument N`; (b) with the caller's file and line from the
  Arbace frame. *Recommended: (a)*.
- **GC141 Resolving go/inst's type forms.** (a) as built, a check of the namespace's package
  with a struct type of those fields (about one go/eval's cost, once per forms and var); (b) an
  entry of the checker resolving type expressions in a file's scope without a package check.
  *Recommended: (a)* now, (b) if go/inst is found in loops with changing forms.
- **GC142 Interpretable packages required from Arbace.** (a) as built, RT.load's hook loads
  `cmp`, `iter`, `maps`, `slices` from the digest; (b) only once interpreted code imports them.
  *Recommended: (a)*: the survey's `(require '[go.slices :as slices])`.
- **GC143 The listed packages' generics and `ns-publics`.** (a) as built, their vars are made
  on first use only (S9), not by `ns-publics` or `refer :all` (which would check the package's
  digest); (b) made with the others. *Recommended: (a)*.
- **GC144 Constraint terms in messages of digest packages.** (a) as built, unchanged by GR9d (the
  checker's set order: `bool missing in ~float64 | ~uintptr | ...`); (b) the written order, as
  GR6's amendment 11 does for interpreted constraints, a fix of the checker. *Recommended: (b)*,
  outside GR9d (GR8's or a later step).

## As built: GR8a (2026-10-19)

GR8a, std at the REPL (doc/go/GR8-SURVEY.md §6.1-§6.3, its milestone GR8a; step 2 of the plan to
the milestone `arbace-interprets-go-v1`, doc/AGENDA.md), is built on branch `goi-gr8a` (from main
`04ac458b`, GR1 to GR7b, with the GR8 and GR9 surveys merged in). GR8b (reflect's methods: GC60)
and GR9 (GC61, the runtime types of interpreted named types, generic vars) are other branches'.
Where building it found otherwise than the survey, the text below is the record; proposals for
the user are GC95-GC104.

**Decided as recommended, under the user's goal of 2026-10-19** (reaching
`arbace-interprets-go-v1`), each built so, for the user to revisit: **GC55 (a)** the interpretable
packages' forms in each configuration's digest (lines kept, docs kept); **GC56 (a)** the 34
packages that must be compiled added to the table; **GC57 (a)** `math/big` compiled; **GC58 (a)**
the internal packages interpreted std needs interpreted, their runtime hooks `:replace`d; **GC59
(a)** `go:linkname` pulls between interpreted packages bound by goi, bad-linkname aliases
`:drop`ped; **GC62 (a)** `reflect.TypeFor` (go1.21's body) and the reflect-driven encoders
`encoding/json`, `encoding/xml`, `encoding/gob` compiled; **GC63 (a)** the table's members over
`iter.Seq` typed by the interpreted `iter`; **GC64 (a)** the speed set compiled (`regexp`,
`bufio`, `compress/gzip`, `encoding/base64`, `encoding/csv`).

**What exists.**

| part | where | as built |
|---|---|---|
| the list | `go/arbace/gotable.edn` | `:packages` 89 (45 at GR7b): GC56's 34, GC62's three encoders and `encoding/json/jsontext`, `encoding/json/v2` with them (GC98), GC64's five; `:interpret` no longer a hand list (computed, below); `:out` the 10 user packages out of scope with their reasons; `:replace` grown by `internal/godebug`'s 4 hooks (with an `:imports` map: `os`), `internal/cpu`'s 7 (amd64's 3, arm64's 4), `internal/abi`'s 2, `reflect.TypeFor` and its `goiNilType` (GC97); `:drop` (bad-linkname aliases of `go/types` and `net/http`; `internal/race`'s two functions of `internal/abi`'s types, so that `iter`, `slices` and `maps` do not import `internal/abi`); `:bodyless` (`iter`'s `newcoro`, `coroswitch`: `iter.Pull` unsupported, GR6's); `:api-interpreted` (GC96) |
| the classification | `arbace/go/table.ae` (`classify`) | per configuration, from the forms (not checked by the generator: goi checks a package when a program imports it): every package of the configuration's std but the listed ones, `:out`'s, `unsafe` and `cmd/`'s, whose body-less declarations are all `:replace`d, `:drop`ped, `:bodyless` or `go:linkname` pulls of an interpretable package's function (GC59), and whose imports (std's vendored ones by `vendor/`) are listed, interpretable or `unsafe`, to a fixpoint; then the user packages among them, `:api-interpreted`'s, and what they import and pull, transitively. The report (`report.edn`): `:interpret` (134 per configuration, 81 user packages), `:refused` (user packages neither listed, interpretable nor out: none in the four configurations), `:api-interpreted` |
| the digest | `arbace/go/table.ae` (`interpret-forms`, `digest-forms`) | the interpretable packages whole (GR6's), lines kept (`:line`, for traces: GC55), docs kept (GC12), `:drop`ped declarations left out, `:replace`d ones replaced with their `:imports`, imports no declaration of the file uses left out; the listed packages' digests: a generic function's `:replace` (`reflect.TypeFor`), replace forms naming no declaration added to the package's first file (`goiNilType`), a package without kept declarations keeps its first file (`time/tzdata`), an untagged variable typed by the checker and its initializer dropped (GC99), an interpretable package not imported but `:api-interpreted`'s (GC96); a last chunk `goi:out`, the out-of-scope packages' reasons; the members: an exported variable's unexported named type of the package described by the variable (`(.Elem (reflect/TypeOf (addr binary/LittleEndian)))`) |
| the REPL | `arbace/go/repl.ae` | the digest's session checks the interpretable packages too, with their trees, once per process (GC104); an interpreted package's session delegates them to it (`:local`), so a listed member naming `iter.Seq[string]` is the type interpreted code names (GC63); goi defines an interpretable package from that check (`load-digest-package!`), its interpretable imports first, after the importer's check (`define-digest-imports!`; not in errorcheck); `refusal` (GC101): an out-of-scope package (`goi: embed: go:embed is out of scope (GS14)`), an internal or vendored std package imported outside std (`use of internal package internal/godebug not allowed`, `package golang.org/x/net/idna is std's vendored copy, not importable`); `linkname-cell` (GC59); `intern-go` (a Go name shadows a class or a core var without a warning: `go/ast.Package`, `regexp/syntax.Error`); the build contexts' `:table-type?` and `:pkg-scope` |
| the builder | `arbace/go/build.ae` | F1 (a labeled declaration a goto target); F4 (`free-locals`: a type switch clause's variable is declared by its clause); a method of an unexported compiled type through a listed interface of its package with that method and signature, which the type implements (`via-iface`: `binary.LittleEndian.Uint32` as `ByteOrder.Uint32`); a compiled package's unexported type the table describes is compiled (`TCompiled`); `reflect.TypeFor` of an interpreted interface (named, or unnamed with methods) is `goiNilType`'s instance (nil, GC97) |
| the checker | `arbace/go/check/code_stmt.ae`, `check.ae`, `check/decl.ae`, `check/code_call.ae` | F1's `:decl-label`; a session's `:refuse` (the import's error `could not import embed (goi: embed: ...)`) and `:keep-trees` (a predicate); the initialization order by a heap (GC100); `lookup-error`'s local `o` shadowed the function `o` (a crash on a misspelled selection of a package-level object) |
| tests | `test/goi/run/go/t/labels.ae`, `stdlib.ae`, `test/goi/std.ae`; `bin/goi test` | `labels.ae` (F1; goi printed `0 0 0`, `0`, `0 0`, `[]` before the fix); `stdlib.ae`, 41 lines of gc's output: the compiled `regexp`, `math/big`, `encoding/json` over an interpreted struct, `bufio`, `compress/gzip`, `encoding/base64`, `encoding/csv`, `hash.Hash32`, `net/url`; the interpreted `regexp/syntax`, `container/*` over the program's own types, `hash/fnv`, `text/tabwriter`, `text/template` (parsed), `flag` with an interpreted `flag.Value`, `go/parser`, `go/ast`, `go/format`, `html`; `encoding/binary`'s byte orders; `reflect.TypeFor`; `strings.SplitSeq` and `strings.Lines` ranged; `std.ae` (183 checks, about 30 s, not in the smoke test: GC103): every user std package of the configuration imported at the REPL in a package of its own and a member named by Go code (88 compiled, 81 interpreted), the 10 out of scope refused with their reasons, internal and vendored packages refused outside std |

**The exit criteria (the survey's §6.2).**

- **No program of `bin/goi check` unsupported for lack of a std package**: the run programs and
  directory tests unsupported are 22, all GR8b's or out of scope: 13 for reflect's method lookups
  (`reflectmethod1`, `4`-`7`, `recover`, `bug424`, `issue16331`, `issue19028`, `issue25897b`,
  `issue27695`, `issue27695b`, `issue27695c`: GR8b, 8 of them GR9's, GC61) and 9 assembly or
  `go:linkname` (§1.1). The survey's 29 std-package programs: 22 pass, 5 known (`issue7690`,
  `issue8606b`, `issue4618`, `issue16037_run`, `typeparam/issue48318`, below), 2 fail by goi's core
  (`issue30116`, `issue30116u`: GC95); the 3 `encoding/binary` programs: 2 pass (`issue19201`,
  `issue44823`), `issue11656` known.
- **Every user std package importable**: `test/goi/std.ae` on linux/amd64, 169 of the 180 user
  packages imported (88 compiled, 81 interpreted), `unsafe` the checker's, the other 10 refused with
  their reasons. The four configurations classify alike (134 interpretable, none refused); the box
  was not built or checked on this branch (GC102's note).
- No program passing at GR7b regresses; the essential gate and the fixpoint (below).

**The check (§11).** `bin/goi check` over the 1,178 run programs and directory tests and the 659
errorcheck programs, against `test/goi/check-linux-amd64.edn` (recorded):

| result | GR7b | GR8a |
|---|---:|---:|
| pass | 1,386 | 1,414 |
| fail | 23 | 25 |
| error | 2 | 2 |
| known | 73 | 79 |
| unsupported | 335 | 299 |
| skip | 18 | 18 |

Of the 36 that leave unsupported: 28 pass (among them `regexp`'s `issue26411`, `issue36437`,
`issue9355`, `syscall`'s `issue15002`, `issue79874`, `recover4`, `sigchld`, `testing`'s `bug295`,
`issue4667`, `issue56990`, `math/big`'s `abi/idata`, `issue62360`, `runtime/trace`'s `issue73748a`,
`b`, `runtime/pprof`'s `issue9321`, `flag`'s `chan/doubleselect`, `container/heap`'s `chan/sieve2`,
`go/ast`'s `bug206`, `go/build`'s `alias3`, `hash`'s `bug257`, `encoding/json`'s
`typeparam/issue48317`, `encoding/binary`'s `issue19201`, `issue44823`, `nosplit`, and the
errorcheck programs `bug358` (`net/http`), `import1`, `issue5089`, `issue6428`); 6 are known by design (`test/goi/check-known.edn`): runtime.Stack's
frames (`issue7690`), runtime.Callers and FuncForPC (`issue11656`), the order of an interface
comparison of structs over protected memory (`issue8606b`), `testing.AllocsPerRun`
(`issue4618`), `encoding/xml` of an interpreted generic type's unnamed struct RT (`issue48318`:
G8, GR9), `html/template`'s Execute asking `reflect.Value.MethodByName` (`issue16037_run`:
GR8b); 2 fail, recorded so (GC95): `issue30116` and `issue30116u`, goi's bounds messages for
slicing an array (`with capacity 3` where gc says `with length 3`) and for unsigned indices beyond
2^63 (`[-1]` where gc says `[18446744073709551615] with length 3`). The unsupported errorcheck
programs change only in their reasons (`embed`'s refusal). `bug385_64` is no longer over the
check's 10 s (GC100). The whole check takes 79-83 s with 12 jobs on the loaded machine (GR7b: 54 s
with 16). The essential subset (`test/goi/essential.txt`) gains 13 by GC28's rule (every one
outside `fixedbugs/`, every fourth new one of `fixedbugs/`; `nosplit.go` left out, it runs the go
command): 712 programs.

**Found while building.**

- **`text/template`'s Execute needs `reflect.Value.MethodByName`** even for fields
  (`evalField` asks a method of the field's name first): over a map or a struct it panics with the
  table's `not supported by goi yet: reflect.Value.MethodByName is not in the table` (the survey's
  §2.5 had it working). Parsing works. GR8b's (GC60).
- **go1.27's `encoding/json` is `encoding/json/v2`'s** (GOEXPERIMENT `jsonv2`, a default
  experiment): `json.Marshaler` is an alias of `jsonv2.Marshaler`, `json.RawMessage` of
  `jsontext.Value`. Interpreted, those would be other types than the compiled `encoding/json`'s
  (`reflect.TypeFor[json.Marshaler]()` was `interface {}`): so jsontext and v2 are compiled with it
  (GC98), and their options' internal interface `jsonopts.Options` interpreted for their API
  (GC96).
- **A listed package's digest naming an interpretable package** (the survey's §5.1): 8 on
  linux/amd64, mostly unexported declarations (`regexp`'s fields of `regexp/syntax`'s types,
  `runtime`'s and `reflect`'s of `internal/abi`). Resolved, they would type compiled members by
  the interpreted twin's types, and every import of `regexp` would check `regexp/syntax` (1.2 s):
  so their imports are left out of the listed digests but `:api-interpreted`'s (GC96).
- **The checker's initialization order was quadratic** (the minimum of the pending objects by a
  scan): `syscall`'s digest, 1,880 constants, took 1.1 s to check, now listed (`os` imports it).
  A heap as go/types' (GC100): 0.2 s; `bin/goi annotations` unchanged (2,560 initializers of
  `linux-amd64` in the same order).
- **The digest's untagged variables kept their initializers** for their types (GR1's
  `:go/digest-init`): `unicode`'s range tables took 0.9 s to check at every first import of
  `strings` or `unicode`; typed by the generator's checker now (GC99): 0.14 s.
- **Unexported compiled types.** `binary.LittleEndian`'s type was described as an interpreted named
  type (its compiled value not its RT); the table now describes it by its variable's type, and a
  method call on it goes through `ByteOrder` (the survey's §4.4, no size).

**Measurements.**

- **Speed** (GR3's criterion): `bin/goi run test/goi/run go.t.fib bench`, best of five per run,
  five runs alternating main's executable (`04ac458b`, GR7b, 136,446,112 bytes) and GR8a's on the
  shared machine (load 12-15):

  | | main (GR7b) | GR8a |
  |---|---|---|
  | fib 22 | 2.52-3.19 ms (best 2.52) | 2.61-3.19 ms (best 2.61) |
  | sumSq 1e6 | 19.46-24.82 ms (best 19.46) | 18.81-22.10 ms (best 18.81) |
  | sieve 1e6 | 66.26-85.25 ms (best 66.26) | 62.17-80.75 ms (best 62.17) |

  The interpreter's core is unchanged; the runs' noise. The speed set runs compiled (GC64): the
  survey's 150-430× of interpreted `regexp`, `math/big`, `bufio` becomes a compiled call each.
- **Size.** The executable is 141,906,080 bytes against main's 136,446,112: **+5,459,968
  (4.0%)**, within the survey's expected +3-5 MB plus GR8a's code. `bin/goi table --size`: the
  table 9,170,944 bytes (GR7b's about 3.7 MB), of which the invokers 1,519,616 (389, above GS4's
  1 MB: GC11's threshold is 2 sharing members; GC102), 89 packages, 1,307 signatures, 28 adapters;
  linux/amd64's members 1,391 functions, 391 variables, 510 types, 1,685 methods (GR7b's 886, 348,
  249, 1,035). The digest: 10,394,893 bytes, **2,489,120 compressed** (GR7b's 1,404,911 and
  336,715): +2.15 MB, the survey's 1.4-2.4 MB.
- **Load time at the REPL** (a fresh process, the first Go form, `math`'s, made before: 0.09 s, GR7b
  0.03 s; then the import and a member named, as `test/goi/std.ae` does), on the loaded machine:

  | package | GR8a | survey (interpreted) |
  |---|---:|---:|
  | `container/list`, `math/big` (compiled), `text/tabwriter`, `hash/fnv` | 0.03-0.15 s | 0.17-0.5 s |
  | `syscall`, `regexp` (compiled), `strings` | 0.27-0.42 s | `regexp` 3.1 s |
  | `encoding/json` (compiled), `os`, `go/token`, `image/png` | 0.59-0.71 s | |
  | `log/slog`, `flag`, `regexp/syntax`, `archive/zip`, `database/sql`, `testing`, `text/template` | 1.05-1.76 s | `flag` 2.0-2.9 s |
  | `go/parser`, `html/template` | 2.28, 2.75 s | |
  | `go/types`, `net/http` | 4.85, 8.48 s | |

  `strings` against main's 1.09 s (GC99); `os` against main's 0.32 s (`syscall` listed, checked).
  `test/goi/std.ae` imports the 169 in 29-32 s in one process (each package checked once, GC104).

**Checks** (this branch, amd64): the essential `bin/gate` passed (14 m 42 s: the smoke test, Clojure's
suite as recorded, the oracle 22,637 of 22,690 against its 53 known, the differential check
12,731 of 12,754 against its known, goi's essential check 712 of 712, the generated variants);
`bin/arbace-go --fixpoint`: G1 = G2 = G3 (141,906,080 bytes); `bin/goi check` as recorded (above),
`--essential` (712 of 712); `bin/goi test` and `--gc` (the checker's 19 assertions and its invalid
forms, 17 programs, 78 REPL checks, 183 std checks, §1.4's session); `bin/goi annotations` (the
five corpora as GR6's: zero errors, every declaration checked, the same counts); `make
check-makefile`; `bin/generated-check`. Not run: `bin/gate --full` and the box (GC102).

**Proposals for the user (GC95-GC104; each built as recommended).**

- **GC95 The programs GR8a's packages reach that fail by goi's core.** `issue30116`,
  `issue30116u`: goi's bounds messages of slicing an array and of unsigned indices (above). (a) as
  built, recorded as failures in the reference, the essential subset by GC28's rule without them;
  (b) fixed in goi's back end now (an index check at the index's unsigned type, an array's slice
  bounds checked as a string's, gc's `panicSliceAlen`), a change of the back end that GR9a and
  GR9d share. *Recommended: (a)* now, (b) with the next change of the back end.
- **GC96 The listed digests' imports of interpretable packages.** (a) as built, left out but the
  list's `:api-interpreted` (`iter`, `encoding/json/internal/jsonopts`): the members naming the
  others are unusable, as before GR8a; (b) all resolved: compiled members typed by interpreted
  twins (the survey's one-type-twice), and slower imports of compiled packages. *Recommended:
  (a)*.
- **GC97 `reflect.TypeFor` of an interpreted interface.** (a) as built, nil (`goiNilType`):
  `Implements(nil)` panics where it is asked (`database/sql/driver`'s `valuerReflectType`,
  `html/template`'s), so those packages import and work but for that check; (b) unsupported at the
  instance's build: `database/sql`, `database/sql/driver`, `net/rpc` and their importers
  unimportable (they ask it at initialization). *Recommended: (a)*, until G8 (GR9) gives
  interpreted interfaces reflect types.
- **GC98 `encoding/json`'s v2 packages.** (a) as built, `encoding/json/jsontext` and
  `encoding/json/v2` compiled with `encoding/json` (their types are in its API; linked already);
  (b) interpreted (the survey's class): `json.RawMessage`, `json.Marshaler` of interpreted code
  other types than compiled `encoding/json`'s. *Recommended: (a)*.
- **GC99 The digest's untagged variables.** (a) as built, typed by the generator's checker, their
  initializers dropped, when the type names only the file's imports (`unicode`'s tables: 0.9 s →
  0.14 s at each first import); (b) GR1's initializer kept for typing. *Recommended: (a)*.
- **GC100 The checker's initialization order.** (a) as built, go/types' heap (`syscall`'s
  digest 1.1 s → 0.2 s, the order unchanged); (b) the scan, with `syscall`'s cost on every
  program importing `os`. *Recommended: (a)*.
- **GC101 What the REPL refuses.** (a) as built, the out-of-scope packages with their reasons
  (`unsafe` the checker's, importable), internal and vendored std packages outside std (Go's
  rule), as a check error of the import; (b) the internal ones importable at the REPL (a
  programmer's exploration). *Recommended: (a)*: Go's rule.
- **GC102 The box and the invokers' threshold.** The four configurations' tables and digests are
  generated (the classification alike); the box's executable (tamago's digests, 2.46-2.47 MB
  compressed) was not built or checked here. (a) `bin/goi check --box` and the box's smoke test at
  the merge, `bin/gate --full` (the agenda's next full gate); (b) now on this branch.
  The invokers cost 1.52 MB (GS4's 1 MB): (a) `:min-sharing 2` kept; (b) 3. *Recommended: (a)*
  both.
- **GC103 `test/goi/std.ae` where.** (a) as built, in `bin/goi test` (about 30 s), not in the smoke
  test; (b) in `bin/arbace-go --smoke` too. *Recommended: (a)*.
- **GC104 Interpretable packages checked once per process.** (a) as built, in the digest's
  session (GR6 checked `slices` again for each importer's session); (b) GR6's per importer.
  *Recommended: (a)*.

## Decided (2026-10-19): GC65-GC71

GC65 (a), GC66 (a), GC67 (a), GC68 (a), GC69 (a), GC70 (a), GC71 (a) of doc/go/GR9-SURVEY.md
§7, as recommended, under the user's goal of 2026-10-19 (reaching the milestone
`arbace-interprets-go-v1`, doc/AGENDA.md's plan): the reflect overlay; typed stubs per method
signature and slot, a panic stub for the others; 256 slots for `func() string` and `func()
error`, 16 for every other family; a new generation when a type's method set changes; proxies
and wrappers removed; GR7a's shapes kept as a temporary layout only; interpreted interface types
`any` in Go memory. GC72-GC74 (generic vars, GR9d) are built apart.

## As built: GR9a-c (2026-10-19)

GR9a (the overlay and named types), GR9b (values without proxies) and GR9c (names, embedding,
recursion, `Equal`) of the GR9 survey (doc/go/GR9-SURVEY.md, §3, §6) are built on branch
`goi-gr9a` (from main `04ac458b`, with the GR8 and GR9 surveys merged in; main merged in again
with GR9d at `e4621b5d`, GR8a at `104d2827` and GR7c at `6295e93b`), in that order and as one change of the
interpreter's representation. Where building them found otherwise than the
survey's §3 (the spec draft), the text below is the record; proposals for the user are
GC125-GC134.

**What exists.**

| part | where | as built |
|---|---|---|
| the reflect overlay | `overlay/go/reflect.ae`, `overlay/go/reflect/arbace_named.ae`; `bin/jrt` (`make_overlay`, `overlay_pkgs`), `bin/tamago-vendor` (`overlay_json`), `bin/lib/seed-go.bash` | `arbace_named.go` added to `$GOROOT/src/reflect` in every build's overlay (linux and tamago, amd64 and arm64; the box's guests through the same overlay): `ArbaceNamed`, `ArbaceSetLayout`, `ArbaceSetUnderlying`, `ArbaceSetMethods` (§3.1) and `ArbaceDirectIface`; written in Go, converted to forms by `bin/repr-go read` in a copy of the toolchain's tree with the file added (amendment 1). The seed's archive carries `overlay/reflect/` beside `overlay/runtime/` from its next remaking (`seed_go_make`); `seed_go_build` maps every `overlay/PKG/F.go` to `GOROOT/src/PKG/F.go`, so the current seed (runtime only) builds as before |
| descriptors | `go/arbace/goi/types.ae` | `TNamed` makes the runtime type and its pointer type (`ArbaceNamed`), `TSetUnder` completes it once (`ArbaceSetUnderlying`), `TShape` gives a shape's layout (`ArbaceSetLayout`); one RT per type in frames and in Go memory (`Mem` is `RT`); the registry (`register`, `descOfRT`) |
| method tables and stubs | `go/arbace/goi/stubs.ae`, `method.ae` (`TSetMethods`) | `setStubs`: a type's RT gets its method table once, each method a stub of its signature's family (`takeStub`), `EnterSlot` the stubs' entry into the interpreted method; `RegisterStubs`, the panic stub |
| the stubs | `arbace/go/table.ae` (`stub-forms`, `data-type`), `go/arbace/gotable.edn` (`:stubs`) | per family `stubSlotsN` (its slots), `stubCallN` (the typed call through goi's frame, an adapter's), `stubN_k`; the families: the listed interfaces' method signatures and `:common`'s |
| dispatch | `go/arbace/goi/method.ae` | `ifaceMethod`: an inline cache per call site keyed by the content's reflect type; assertions and type switches compare RTs; `missingMethod` by the descriptor's method set |
| the builder | `arbace/go/build.ae` | `named-desc` (the method counts, `reflect-kind`, `layout-needed?`), the method sets given at the outermost `desc` (`*methods-pending*`, `set-named-methods!`), `coerce` (a value of another composite type assigned: converted), runtime names (`sym-name`, `rt-name`, `inst-type-name`) |
| the checker | `arbace/go/check.ae` (`number-local-types!`), `check/code_stmt.ae` | function-scoped defined types numbered as gc's noder does (`:goi/vargen`) |
| the REPL | `arbace/go/repl.ae` (`define!`) | generations by method set (GC68), to a fixpoint over the package's types; each checked type object carries its generation (`:goi/gen`) |
| removed | `method.ae`, `types.ae`, `value.ae`, `adapt.ae`, `expr.ae`, `func.ae`, `trace.ae`, `natives.ae`, `convert.ae`, `call.ae`, `table.ae` | `goi.Proxy`, `contentOf`, `proxyOf`, `proxied`, `Proxy.Format` and its `formatGuarded`, `errorWrapper`, `stringerWrapper`, `CallProxy`, `RegisterWrapper`, `wrapProxy`, `EnterMethod`, `RawR` (and the builder's `*raw-any*`), the tags of S37 (`renameTypes`, `tagTypes`, `proxyGoSyntax`), GR3's erased pointers (`convPtr`, `Erased`), GR7a's shaped Go memory (`Shaped`, `reshape`, `toMem1`), `interpOf`, `unfaithful`; the table's wrappers (the table keeps `RegisterIfaceMethods` for reify) |
| tests | `test/goi/run/go/t/named.ae` (`.out` gc's), `test/goi/repl.ae` (GR9a-c's checks), `test/goi/run/go/t/fib.ae` (two method benchmarks) | X9's checks as a program goi and gc run alike; at the REPL `%T`, a method added making a new generation, a body redefined reached through the same stub, `sort.Sort` of an interpreted `sort.Interface` |

**The known entries (the exit criterion).** The survey's eleven (`fixedbugs/bug177.go`,
`bug468.go`, `issue15528.go`, `issue30041.go`, `issue77779.go`, `recover2.go`, `genmeth1.go`,
`typeparam/issue49547.go`, `issue54456.go`, `mdempsky/17.go`, `nested.go`) and
`fixedbugs/issue4585.go` pass and have left `test/goi/check-known.edn`. `issue4585.go`'s structs
are named, so GS8's `Equal` (§3.6 of the survey) is enough: the unnamed struct types `StructOf`
makes keep comparing their blank fields (reflect unchanged, GC132).

**The check (§11).** `bin/goi check`, against `test/goi/check-linux-amd64.edn` (recorded after
the merges, from a fresh run; its diff against main's is the thirteen programs, known → pass):

| result | main (GR7c, `6295e93b`) | GR9a-c |
|---|---:|---:|
| pass | 1,438 | 1,451 |
| fail | 2 | 2 |
| known | 83 | 70 |
| unsupported | 296 | 296 |
| skipped | 18 | 18 |

The thirteenth is GR8a's `typeparam/issue48318.go` (`encoding/xml` of an interpreted generic
type's value, known for G8). No program passing on main regresses. Under a load of 30-45 two
programs passing on main exceed the check's 10 s on both executables, main's and this branch's
(`fixedbugs/bug358.go`, `nosplit.go`): they pass run alone; the reference, recorded from a run
with 8 jobs (136 s), keeps their passes. 65-92 s with 16 jobs. The essential subset gains GR9's eight programs by GC28's
rule (every one outside `fixedbugs/`, every fourth new one of `fixedbugs/`: `bug177.go` and
`issue4585.go`, the first and the fifth of six): with GR8a's and GR7c's, 729 programs. `bin/goi check --box` (amd64, the guest of this
branch, before the merges): `test/goi/box.txt` with the same eight, 574 programs, 573 pass, 1
known (`typeparam/issue50109.go`, the console's last newline), no regression, 271 s with 8 boxes;
`test/goi/check-box-amd64.edn` recorded with the eight.

**Amendments (as built).**

1. **The overlay's source** (§3.1): Go first (`arbace_named.go`, from Appendix B), checked by
   building `reflect` with it for linux/amd64, linux/arm64, tamago/amd64 and tamago/arm64 and by
   X9's prototype against it (34 of 34), then converted to forms by `bin/repr-go read` in a copy
   of the toolchain's tree (hard links) with the file added; the forms are what the repository
   holds and the builds print (GC125). Beyond Appendix B: a mutex around its map (one map, the
   type's uncommon type and room by descriptor), the pointer type made with the type
   (`ArbaceNamed` gives both) and its room checked by `ArbaceSetMethods`, the method names of
   unexported methods with their package paths (`abi.Name`'s bit 2), sorted as gc sorts them
   (exported first, by name, then by package path), the named struct's `Equal` skipping blank
   fields and `TFlagRegularMemory` cleared for them, and `ArbaceDirectIface` (the runtime's
   `IsDirectIface`, which decides what a method's code is given as its receiver).
2. **Blank fields** (§3.6): go1.27's `StructOf` takes several `_` fields (it refuses duplicate
   names but `_`), so goi no longer numbers them (`_1`, `_2`: S37's era); `%+v` prints `_` as
   gc, and the named type's `Equal` finds them by `Name.IsBlank`.
3. **The stubs' calls** (§2.3): every method of a table has a slot of its own (GC130: the
   pointer type's entries are the builder's thunks, gc's panicwrap message for a nil pointer
   theirs, as GR4's), and `EnterSlot` makes the receiver the interface value the itab's call is
   of, the slot's type word and the word it is given (a pointer-shaped value, else a pointer to
   the interface's copy, which nothing writes): no copy, no reflect. The method set's entry is
   cached in the slot while the type's method set is the one it was found in (`mver`). A first
   version shared the value methods' slots between the two tables as §2.3 has it, loading a copy
   of the value through reflect at each call: `sort.Sort` of 10^5 took 371-472 ms against main's
   wrappers' 225-246 ms; as built 174-176 ms.
4. **When method sets are given** (§3.2, GR4's amendment 2): an interpreted named type's method
   set and its pointer type's are given when its descriptor is made, at the end of the outermost
   `desc` (`*methods-pending*`, as GR7a's interfaces), no longer when a value first goes into an
   interface: compiled code may meet a value of the type without goi seeing it go into one
   (`fmt` printing a slice's elements, `reflect.New`), and a failed itab is cached for good. The
   method table's room is the count of the checker's method set (`method-count`); an entry left
   out (a signature the digest cannot describe) leaves room unused. A thunk the table cannot
   build at once (a promoted compiled method the table leaves out, S39: `struct{ reflect.Value }`'s
   `Method`) is built when first called, in program mode too (`typeparam/issue48185b.go`).
5. **Stubs' families** (§2.3): the table's families are the listed interfaces' method signatures
   and the twelve of `go/arbace/gotable.edn`'s `:stubs :common` (`String`/`Error`/`GoString`,
   `Unwrap`/`Close`/`Err`, `errors.Is`, `errors.As`, `Unwrap() []error`, `MarshalJSON`,
   `UnmarshalJSON`, `Timeout`, `Len`, `Less`, `Swap`, `Format`), each family with the method
   names compiled code calls it by: before GR8a's merge 77 families, 1,712 stubs on linux/amd64;
   with GR8a's 89 listed packages 118 families and 2,416 stubs in the four configurations' union
   (two families of 256, `Read`'s and `Write`'s of 64, the others 16). A stub is `stubN_k(p unsafe.Pointer, args...) results`, calling
   the family's `stubCallN(&stubSlotsN[k], p, args...)`, which enters the method's frame
   (`goi.EnterSlot`) and writes and reads the slots as the typed adapters do (GC22); `RegisterStubs`
   takes the stubs' code by `reflect.Value.Pointer` (a Go function's ABIInternal entry, what an
   itab calls). A method whose signature has no family gets the panic stub (GC134).
6. **Exhaustion** (GC67, GC126): a method takes a slot only when compiled code can call it by an
   interface, its name one of its family's (a listed interface's method, a common one), else the
   panic stub, silently: GR8a's 81 interpreted std packages exhausted `func()`, `func() bool`,
   `Read` and `func() int` with methods no compiled interface has. goi warns at the REPL only; a program's run (bin/goi run, the
   check) keeps its standard error the program's (`ken/embed.go` defines more than 16 types with
   `func() int` methods that compiled code never calls). `SetOSArgs`, now before the program's
   packages are built, sets program mode.
7. **The registry** (§3.2): every descriptor whose RT is its own enters it when interned
   (`intern`, `register`), composites of interpreted types too (`[]Celsius`); an unnamed struct
   type with embedded fields and a method set of its own enters it at `TSetMethods`. `descOfRT`
   is the one lookup (dispatch, assertions' messages, `ContentToArbace`, the natives, GR9d's
   `ValueType0`): the registry, then composites compiled code made of interpreted types
   (`reflect.SliceOf(T)`) made from their parts, else the compiled type's; its answers cached.
8. **Assignment between a named and an unnamed composite type** (GC128): their RTs are two now,
   so the builder converts a value assigned to a variable, parameter, result or element of
   another struct, array, slice, map or pointer type (`coerce` in `ex`, `ConvR`), and compares two
   operands of such types after converting one (`fixedbugs/issue23545.go`: an `OutputID` against
   a `[32]interface{}`); pointers and channels are compared by their words (a channel's frame
   value keeps its own direction: `fixedbugs/issue67190.go`). `ConvR` reinterprets the bytes
   (`retype`) when reflect refuses the conversion: an unnamed struct type of plain fields (S36's,
   still) to a named one whose fields are embedded.
9. **Generations** (§3.7, GC68): a type's key is its underlying type's and its declared methods'
   names, receivers and signatures; the keys are computed to a fixpoint over the package's types,
   so that a type naming another whose generation changed changes too (before GR9, a dependent
   type kept its generation and its descriptor's underlying type was set again, which a runtime
   type cannot be). Each checked type object carries its generation (`:goi/gen`), so that a
   dependent kept with its old definition (§2.5 item 6) builds with the old type's descriptor
   (GC129). `test/goi/repl.ae`'s `cross.P#1` is now `cross.P#3`: two methods were added before the
   value was made.
10. **Names** (§3.5): gc's runtime names. A function-scoped defined type is numbered as gc's noder
    numbers them (`writer.go`'s `collectDecls`: the package's typegen, source order): the checker
    records each with its package-level declaration's id (collection order is source order) and
    numbers them after the bodies (`number-local-types!`, GC131); the copies an instance makes
    of a generic function's local types carry the instance's type arguments (`:goi/implicits`),
    and an instance of a local generic type is of the instance's copy (`isubst` takes the origin
    too). `sym-name` is noder's `mangle`: the name, `[implicits;own]`, then `·N`; the type's own
    runtime name leaves the trailing `·N` out (`types/fmt.go`), a type argument's keeps it
    (`LinkString`): `main.T[int;main.U[int;int]·3]`.
11. **Recursive types** (§3.4, GC70): `layout-needed?` decides which types get their shape's
    layout first: a type that is a struct's field, an array's element or a map's key or element
    among the types made while it is (GR7a's `A` through `[]B`, `B` holding an `A`: `StructOf` of
    `B` reads `A`'s layout); a type recursive through pointers, slices, channels and functions
    only needs nothing (its RT and `PointerTo`, `SliceOf`, `ChanOf`, `FuncOf` of it exist before
    it is complete). GR3's erased pointers and GR7a's shaped Go memory are gone.
12. **Reify** (G13, GR7b): a reify type is a named struct `arbace.reify[I...]` holding the Arbace
    object (`struct{O any}`), its methods the interfaces' through stubs; a reify given for a
    compiled interface is a value of it (the runtime's itab), and back in Arbace its object
    (`reifyObj`). `RegisterIfaceMethods` stays the table's, without the wrappers.
13. **What compiled code sees** (G8, §9.4): `%T`, `%v`, `%#v`, `reflect`'s names, kinds,
    `Implements`, `AssignableTo`, `Field(i).Anonymous` of named struct types, the runtime's
    messages, `fmt`'s `Stringer`, `error`, `Formatter` and `GoStringer`, `errors.Unwrap`, `Is`
    and `As`, `sort.Interface`, `io.Reader`: gc's (`test/goi/run/go/t/named.ae`, gc's output). The
    known differences left: interpreted interface types are `interface {}` (GC71); unnamed
    struct types with embedded fields keep plain fields (GC127); a method of a signature without
    a stub family panics when compiled code calls it (GC134).
14. **goString** (§5.7): `%#v` as gc prints it (a named float's `%#v` is its number, as gc's),
    `#n` after an old generation's name at its first occurrence.
15. **The merge with GR9d** (main `e4621b5d`): GR9d's `descOfRT` (natives.ae: the registry of
    named structs and their pointers, GR4's `interpOf`) gave way to GR9a-c's (types.ae), and
    `ValueType0` asks it alone (a value's type is its RT's descriptor: no proxy); `TypeInfo0`,
    `desc-types` (kept in `named-desc`, around GR9a-c's body, `named-desc1`) and `rehome` are
    unchanged.

16. **The merge with GR8a** (main `104d2827`): no textual conflict but `test/goi/essential.txt`
    (both added programs: their union, sorted). GR8a's `via-iface`, `TCompiled` of a compiled
    package's unexported types and the digest session's interpretable packages are unchanged; its
    `reflect.TypeFor` of an interpreted interface stays `goiNilType`'s nil (GC97: an interpreted
    interface's RT is `any`, GC71 (a); GC134). Its interpreted std made two things of GR9a-c
    visible: the method sets now given when a descriptor is made reach methods of types goi
    describes but has no cells for (`internal/bisect`'s unexported `Matcher` methods through
    `expvar`, a compiled package's unexported type, GC30): such an entry is left out, as one
    whose signature the digest cannot describe; and their many methods exhausted families
    (amendment 6). `bin/goi test`'s std section: 183 of 183.

**Proposals for the user** (each built as recommended):

- **GC125 The overlay's source.** (a) as built, written in Go, built and checked against the
  prototype, then converted to forms by `bin/repr-go read` (the forms are what the repository
  holds; amendment 1); (b) written in Go forms by hand. *Recommended: (a)*: the conversion is
  repr/go's, checked by its round trip, and the Go was compiled for the four configurations
  before it became forms.
- **GC126 Which methods take slots, and exhaustion.** (a) as built, only a method whose name
  compiled code calls with its signature (a listed interface's, the list's common names), others
  the panic stub silently; a family exhausted: goi warns at the REPL, a program's run stays
  silent (its standard error is the program's); (b) every method of a family's signature takes
  a slot (the survey's: GR8a's interpreted std exhausts `func()`, `Read`, `func() int`), and
  warns in program mode too (`ken/embed.go` would fail the check); (c) larger counts. *Recommended:
  (a)*.
- **GC127 Unnamed struct types with embedded fields.** (a) as built, S36 for unnamed struct
  types only: reflect makes them with plain fields (`StructOf` refuses embedded fields with
  methods), goi dispatches their promoted methods by its descriptor, compiled code sees no
  methods; (b) the overlay's `ArbaceStructOf`: `StructOf`'s type copied with its fields marked
  embedded and the promoted methods' stubs, one per struct type (canonical by the plain one).
  *Recommended: (a)*: no program of the check meets them, (b) when one does.
- **GC128 Assignment across a named and an unnamed composite type.** (a) as built, the builder
  converts the value to the target's type where the checker found it assignable (`coerce`,
  amendment 8; one reflect conversion at run time); (b) the checker records an implicit
  conversion node, which the builder builds as any conversion. *Recommended: (a)*: the builder
  has both types at hand; (b) if a later program needs the node elsewhere.
- **GC129 Generations of kept dependents.** (a) as built, each checked type object carries its
  generation: a dependent kept with its old definition builds with the old type; (b) the
  generation by name (GR3's): the kept dependent built with the new generation's descriptor and
  the old underlying type, which a runtime type cannot change (amendment 9). *Recommended: (a)*.
- **GC130 A value method's two tables.** (a) as built, a slot each: the receiver is the
  interface value itself (no copy), the pointer table's entries the builder's thunks
  (amendment 3); (b) §2.3's one shared slot: half the slots, but a copy of the value at each
  call (the pointer table's word points to a variable): `sort.Sort` 2.1-2.7 times slower.
  *Recommended: (a)*: with the families' names (amendment 6) slots are no longer scarce.
- **GC131 The numbering of local types.** (a) as built, by the package-level declaration's id
  (collection order, the source's) and the order the body declares them; (b) by file and line
  (mode `:lines` keeps no columns, and a literal on its parent's line has none).
  *Recommended: (a)*: exact where gc's typegen is, `typeparam/nested.go`.
- **GC132 Blank fields in unnamed struct types' `==`.** (a) as built, reflect's `StructOf`
  unchanged: an unnamed struct type with blank fields compares them in compiled code (map keys
  of such a type); goi's own `==` (`goEq`) skips them; (b) the overlay replaces `StructOf`'s
  `Equal` (a change of reflect's own file, not an added one). *Recommended: (a)*: no program
  of the check meets it.
- **GC133 Stubs for signatures naming interpreted types.** (a) as built, the panic stub (a
  compiled interface cannot name an interpreted type, so only `reflect`'s `Value.Method` and
  `Call` could reach one); (b) shape families: a stub typed by the signature's shape (every
  pointer-shaped parameter an `unsafe.Pointer`), registered for every signature of that shape
  (the method type stays the exact one). *Recommended: (a)*, (b) with GR8b's reflect entries if
  they need it.
- **GC134 Interface types made by reflect.TypeFor.** GR8a's `goiNilType` (GC97) stays: an
  interpreted interface type's RT is `any` (GC71), so `reflect.TypeFor` of one is nil. (a) as
  built; (b) with GC71 (b), the overlay making named interface types (an `interfaceType` with
  methods), `TypeFor` then their RT. *Recommended: (a)* until GC71 (b).

**Measurements.**

- **Size.** The executable (amd64, stripped) is 143,564,960 bytes against main's 143,007,904
  (`6295e93b`, built for the comparison; at `104d2827`, 143,065,248 against 142,508,192): +557,056
  (0.39%), under the exit criterion's 1 MB: the
  stubs (118 families, 2,416 stubs in the four configurations' union; about 225 bytes each, as
  X9 measured), goi's new code, the overlay's file. Before the merges, against main `04ac458b`'s
  136,446,112: +430,080 (77 families).
- **Speed** (GR3's criterion; `bin/goi run test/goi/run go.t.fib bench`, best of five per run,
  three runs alternating main's executable and this branch's, load 18-24; `fib.ae` gained two
  benchmarks of compiled code calling interpreted methods through compiled interfaces):

  | | main (`104d2827`) | GR9a-c |
  |---|---|---|
  | fib 22 | 2.75-3.63 ms | 2.55-2.63 ms |
  | sumSq 1e6 | 18.81-21.24 ms | 19.34-20.80 ms |
  | sieve 1e6 | 68.90-75.67 ms | 67.32-69.28 ms |
  | `sort.Sort` of 10^5 ints (`Len`, `Less`, `Swap` by `sort.Interface`) | 229.20-239.31 ms (the table's wrappers) | 174.09-175.93 ms (the stubs) |
  | 10^5 `String` calls by `fmt` (`fmt.Stringer`) | 125.68-151.96 ms (the proxies' `stringerWrapper`) | 91.27-92.44 ms |

  fib, sumSq and sieve meet no GR9 code (their noise is the load's); compiled code's calls of
  interpreted methods are 24-27% faster on `sort.Sort` and about 30% on `fmt`, the receiver now
  the interface's value itself.

**Checks** (this branch, amd64, after the merges of GR9d, GR8a and GR7c; `ee80eaf5` and the
reference's commit): the essential `bin/gate` passed (15 m 44 s; goi's essential check 729 of
729, Clojure's suite as recorded, the oracle 22,658 of 22,711 against its 53 known, the
differential check, the generated variants); `bin/arbace-go --fixpoint`: G1 = G2 = G3
(143,564,960 bytes); `bin/goi check` as recorded above, `--essential`; `bin/goi check --box`
(amd64): 573 of 574, the one known, no regression; `bin/arbace-go --smoke --box`: 113 checks
ok; `bin/goi test` (the checker's 19 assertions and its invalid forms, 18 programs, 106 REPL
checks with GR9d's and GR9a-c's, GR8a's std 183 of 183, §1.4's session); `bin/goi annotations`
(the five corpora as recorded: zero errors, every declaration checked); `make check-makefile`;
the oracle's `forms/go.ae` 150 of 150. The overlay builds for every configuration: the box's
guests built for tamago/amd64 and tamago/arm64 (`bin/arbace-go --build --box`, both arches,
from `tamago/`'s forms), and reflect with the overlay compiled for linux/amd64, linux/arm64,
tamago/amd64 and tamago/arm64. The guests had stopped linking on main: GR8a's listed `syscall`
lists `RawSyscall`, `RawSyscall6` and `Syscall6`, which TamaGo declares without implementing,
and since GR1 tamago/arm64's runtime table listed `CallOnG0` (amd64's assembly only): the list's
new `:exclude-in` leaves them out of those configurations' tables (`go/arbace/gotable.edn`,
`arbace/go/table.ae`'s `package-members`).

**Remaining**: GC127 (b), GC133 (b), GC134 (b) when a program needs them; the arm64 box's check
(GR7a's remaining); GR8b's `reflect.Value.Method`, `MethodByName` (reflect's view of interpreted
methods: their `Tfn` is -1 for a value receiver of a type not pointer-shaped, §2.3).
