# g2c: Go as Arbace forms — survey and plan

Status: survey, 2026-10-07. The user decided its open questions (§9) the same day. Two
decisions differ from the survey's recommendations: phase a (G3-G6) is skipped, and the build
configuration is `GOOS=tamago`. The decisions are in
[JOURNAL.md](JOURNAL.md#2026-10-07-decisions-on-g2cs-open-questions) and noted in §9; the survey
is otherwise left as written. Nothing here is implemented in `arbace/`. The experiments it cites
ran in `.tmp/g2c/` (§10).

g2c is a Go → Clojure mechanism, the counterpart of j2c (`arbace/j2c/`,
[classes/CONVERTER-NOTES.md](classes/CONVERTER-NOTES.md)). It would let Arbace take in what the
Go ecosystem has solved without Go's syntax: first libraries, later the runtime itself in the
way of Go plus TamaGo. This document asks what reading Go takes, how Go's constructs could
be written as idiomatic forms ("Go forms"), and what their semantics cost on the JVM (phase a)
and on Arbace's own bare-metal runtime (phase b). It also covers how to verify the result and in
what order to build it.

## Executive summary

**Reading Go is the easy part.** A small Go helper on `go/parser` + `go/types` emits the fully
typed tree as Clojure-readable forms. It plays the part javac's attributed trees play for j2c.
The experiment (§2.4, §10) type-checks and dumps any standard package, `runtime` included (2 s,
31 MB, no type errors). The output is deterministic, and `clojure.core/read` reads it back
(3 s for `runtime`). There is no reason to write a Go type checker in Clojure.

**Writing Go down as forms is moderate work.** Go is a smaller language than Java. go-lisp
(`/root/go-lisp`) already maps every Go syntax node to s-expressions without loss, over all
11,588 files of `$GOROOT/src` and `$GOROOT/test`. The Go forms should borrow its coverage, but
not its surface: class-forms style (Clojure reader, metadata for types, Clojure's code
vocabulary where it fits, and what go/types infers written out).

**Go's semantics are the hard part, and the two phases are hard in different ways.**

- **Phase a, Go forms compiled to JVM classes.** A useful subset is realistic: "pure" Go
  without `unsafe`, `go:linkname`, assembly or cgo, plus goroutines, channels,
  `defer`/`recover`, generics and interfaces. That covers `unicode/utf8`, `math/bits`,
  `strconv`, `sort`, `slices`, `container/*`, `encoding/{hex,base64,binary}`, `regexp`,
  `math/big` (pure-Go build) and, with a hand-written `reflect`, `fmt` and `encoding/json`.
  What it costs:
  - Structs are values and must be copied.
  - Pointers to scalars, fields and elements need reference objects.
  - Strings are bytes, not UTF-16, so Go strings cannot be `java.lang.String`.
  - Unsigned and narrow integers need masking.
  - Interface calls are structural, so they go through `invokedynamic` or itab lookups.

  Goroutines on virtual threads cost about what Go's do (measured, §4.6). Channels and
  `select` need Arbace's own implementation: the JDK's queues are no match under contention.

  What cannot be done on the JVM:
  - general `unsafe` (pointer arithmetic, reinterpreting memory);
  - the `runtime` package itself, which is written against the machine;
  - `go:linkname` into runtime internals;
  - assembly, cgo;
  - exact finalizer and stack semantics.

  These need a hand-written runtime shim, as GopherJS and TinyGo both have (§1.3).
- **Phase b, Go forms as Arbace's systems layer.** The Go runtime is written in a dialect of
  Go: `nosplit`, write-barrier rules, `systemstack`, the register ABI, and 55k lines of
  assembly. To transcribe it, Arbace would need a native compiler that keeps the same
  guarantees: stack maps for a precise GC, stack-growth prologues, write barriers and the ABI.
  That is most of `cmd/compile`'s back end and `cmd/link`, years of work. **But there is a
  shortcut.** Go forms can be printed back as Go (or as go-lisp's `.lgo`, which the user's
  go-lisp fork compiles directly). Then Go's own toolchain plus TamaGo is a ready native back
  end, and go-whim already boots TamaGo guests on the user's own monitor, on KVM amd64 and
  Hypervisor.framework arm64 (`/root/go-whim/doc/GUEST.md`).

**Recommendation.**

1. **Build g2c's front end and the Go forms spec first.** Make *Go → forms → Go* the first
   back end, verified as go-lisp verifies itself: the round trip over `$GOROOT`, then Go's own
   test programs and package tests built from the round-tripped sources with the real
   toolchain. This is cheap, exact, and needed by both phases.
2. **For phase a,** compile Go forms to JVM classes through the class forms compiler. Start
   with pure-Go packages and a hand-written runtime shim; grow it package by package against
   Go's own tests (§6). Type-check for `GOOS=wasip1 GOARCH=wasm`, whose system interface is 36
   WASI calls and which has the fewest assembly files (§2.3).
3. **For phase b, do not start by writing a native Go compiler.** Make the bare-metal REPL
   real first: Arbace emits Go forms, Go + TamaGo compiles them, and go-whim's monitor runs
   them. Then replace pieces of Go's toolchain with Arbace's own, one verified layer at a
   time, the way stage 0's frozen `clojure/` was replaced. The Go runtime is kept as the
   runtime, transcribed into Go forms and so readable and editable in Arbace, long before
   Arbace compiles it itself.
4. **Do not:**
   - transcribe Go's runtime onto the JVM;
   - emulate `unsafe` memory on the JVM;
   - port types2 to Clojure before it is needed;
   - CPS-transform goroutines as GopherJS must (the JVM has virtual threads);
   - fork TamaGo deeply;
   - promise "all of Go on the JVM".

The realistic scope on the JVM is what GopherJS reached in JavaScript: most of the standard
library except `runtime`, `unsafe`-heavy and OS-heavy packages.

## 1. Context and precedents

### 1.1 What j2c taught

j2c works because javac does the hard reasoning. It works on attributed trees (`Attr` + `Flow`),
and the converter writes out what javac inferred: erasure casts, narrowing, overload pins, constant
values. The class forms compiler then only derives what javac derives from declarations
(SPEC §1, principles 3-4). Verification compares class shapes with javac's and runs Clojure's
suite on the converted baseline (CONVERTER-NOTES "Results"). g2c should keep all three parts:

- an authoritative front end (go/types);
- explicit inferences in the forms;
- verification against the original toolchain's output and the original test suite.

There is one difference. For Java, *equivalence of class files* was a strong, cheap check, because
both javac and Arbace emit the same format. Go has no such shared artifact with the JVM. The
analogue is behavioural: Go's test suites, run on both. The structural check is still available
for the Go → forms → Go back end (§6.1).

### 1.2 go-lisp (`/root/go-lisp`, commit `2483a496`)

go-lisp is the user's fork of golang/go that adds a second front end to `gc`. `.lgo` files
(Go as EDN-flavoured s-expressions) are parsed by `cmd/compile/internal/syntax/lisp_*.go`
(5,701 lines with tests) into the same `*syntax.File` as Go source. So types2, SSA, the linker
and the go command treat both alike. What it established:

- **Coverage and losslessness.** Every node of `syntax/nodes.go` has a form (`golisp/SPEC.md`).
  Go → Lisp → Go reproduces every valid file of `$GOROOT/src` and `$GOROOT/test` (11,588
  files, compared as trees after normalising field groups and redundant parentheses).
  966 `$GOROOT/test` run programs behave identically when built from `.lgo`. 15 programs
  that inspect their own line numbers are excluded (F12).
- **Real-program check.** go-whim's 52,657-line generated editor converts, builds and passes
  its suites as `.lgo` (`/root/go-whim/doc/GO-LISP.md`).
- **Design choices g2c can reuse.** Every form keeps Go's source order (I1), so positions grow
  monotonically. Vectors are never expressions (I4). Field groups are explicit (D13).
  Composite literals are explicit (D5). Directives are comments (D7). Its §4.1 mapping table
  is a ready checklist for coverage.
- **Choices g2c should not reuse:**
  - Its reader is its own and is *not* Clojure-reader compatible, by design (Non-goal in
    DESIGN.md): `'x'` runes, backtick raw strings, Go escapes and Go number syntax.
  - Its names are kebab-cased with an export convention (D18).
  - It is purely syntactic: positions and heads, never types (I2). That is right for a second
    front end to gc, whose type checker runs afterwards. It is wrong for g2c, whose consumer
    (Arbace's compiler) needs types.

So go-lisp is the *syntactic* precedent and the proof that a lossless s-expression form of Go
is practical. g2c is the *semantic* layer that j2c is for Java. For phase b, go-lisp's
toolchain is also a ready "Go forms → native" back end (§5.2).

### 1.3 Other prior art (web research, sources in §11)

| project | what it is | relevance |
|---|---|---|
| GopherJS (v1.21.0, master `490705b`) | Go → JS, Go 1.21 | Closest analogue of phase a. Goroutines by a whole-program "blocking" colouring and resumable state machines. Pointers to fields and elements are getter/setter objects. Structs are JS objects copied explicitly. `int64` is high/low halves. A hand-written runtime plus about 8.3k lines of overlays for reflect, sync, syscall, time and os. Tags `purego`, `math_big_pure_go`, `netgo`. Tested with a fork of Go's `test/run.go` and a `knownFails` table (about 70 entries), plus `gopherjs test` over std minus 21 packages. `unsafe` is unsupported. |
| TinyGo (v0.42.0, Go 1.27, LLVM 22) | Go via go/ssa → LLVM | Own runtime, reflect, task and bytealg replace upstream. Upstream's assembly is not used. GCs: leaking, conservative, precise, boehm. Schedulers: tasks (stack switching), asyncify, threads, cores. Of 161 std packages, about 95 pass tests and 66 fail, many on reflect gaps. Bare metal, now UEFI amd64. |
| Yaegi (`fcb76d1`) | Go interpreter in Go | go/ast plus its own checker, values as `reflect.Value`. No assembly or cgo. Slow on computation. Runs in a TamaGo guest (go-whim LISP-SANDBOX: `fib 27` 0.57 s against about 0.003 s compiled). |
| llgo (xgo-dev, v1.0.6), gollvm (dormant) | LLVM Go compilers | Both use conservative GCs and fixed or native stacks. Evidence that Go's precise GC and growable stacks are the expensive part of a native Go. |
| Go on the JVM | JGo (2011, abandoned: structs, interfaces, concurrency and defer never done; `Ptr<T>`, `VarPtr`, typed slices, `@Unsigned`), JGolangCompiler (an educational subset) | **No Go → JVM project reached goroutines plus pointers.** g2c would be the first serious one. |
| Valhalla (JEP 401) | value objects | Integrated for **JDK 28** as a preview, so not in 26. Value objects are immutable, and Go structs are mutable in place, so they would fit only unaddressed structs. Not a basis for phase a. |
| TamaGo (v1.27.1, `7af6ac9`) | `GOOS=tamago` | The fork adds about 3.8k lines to go1.27.1. Board hooks (`goos/goos.go`): `RamStart`, `RamSize`, `RamStackOffset`, `CPUInit`, `Hwinit0` (assembly), `Hwinit1`, `InitRNG`, `GetRandomData`, `Nanotime`, `Printk`, and optionally `Bloc`, `Exit`, `Idle`, `ProcID`, `Task`, `Wake`. The module has about 34k lines of Go and 4k of assembly. Upstreaming: golang/go#73608 is open, with CL 821260 (`GOOSPKG`, custom GOOS) in review as of 2026-10. |
| Joker (v1.10.0) and jcburley's gostd | Clojure dialect in Go | gostd generates Clojure wrappers of std with go/types. It hits the addressability problem: pointer-receiver methods on non-addressable values fail. Joker runs as a TamaGo guest REPL on go-whim's monitor. |
| go-whim (`80c49bf`) | C → Go/Java/Clojure/… | Its Java backend's memory model: `BytePtr` (array plus offset), `Ptr<T>`, struct copies `a.set(b)`, unsigned via the JDK's `*Unsigned` methods, struct members as owned objects. Its Clojure backend lowers to basic blocks where control does not nest. It measured Clojure at about 6-7 times slower than Go on its workload (`doc/CLOJURE.md`, `CLAUDE.md`). And it has the monitor, the TamaGo boards and the SMP guest that phase b needs. |

## 2. Front end

### 2.1 Choice: a Go helper, not a parser in Clojure

| | Go helper on go/types, emitting forms | Parse and type-check in Clojure |
|---|---|---|
| fidelity | exactly Go's rules: untyped constants, generic inference, method sets, embedding, init order | a reimplementation of types2's 23k lines; years to reach parity |
| maintenance | follows Go releases for free | every language change ported by hand |
| dependency | the Go toolchain at conversion time only, as javac is for j2c | none |
| later self-hosting | g2c of `go/types` itself gives a Clojure checker, *if* ever wanted | — |

The helper is the counterpart of `arbace/j2c/javac.clj`, which drives javac in-process. Go has
no JVM API, so the helper is a separate Go program. It writes one forms file per package (or a
stream), and `arbace.g2c` reads it with Arbace's reader. go-lisp cannot serve as the front end:
its parser lives in `cmd/compile/internal/syntax`, which Go's `internal` rule keeps from being
imported outside `cmd/compile` (go-whim GO-LISP.md). And g2c needs types, which `go/types`
gives through a public API.

Alternative inside Go: `golang.org/x/tools/go/ssa` (in the module cache,
`tools@v0.44.1-0.20260420230617-19499e7caabc`). It is what TinyGo uses, and it lowers
`defer`, `range`, closures and multiple assignment for you. It is right for a code generator
and wrong for idiomatic forms, because the structure is gone. Recommendation: forms from the
typed AST. Keep go/ssa in mind for a "lowered" mode, should the native back end ever want one.

### 2.2 What the helper emits

Following the project's convention (CLAUDE.md) and SPEC §1, principle 5, everything targets
**`clojure.core/read` (Arbace's reader), not EDN**. The helper may use metadata (`^{...}`), symbols with dots and
slashes, ratios and BigInts, and tagged literals where a data reader is registered.

- **The tree.** One form per AST node, in source order: `(:binary-expr {...})` and so on,
  generically from `go/ast`.
- **Types and decisions as metadata** on each node form:
  - `:t`, the type: a string from `types.TypeString` for now, later a type form;
  - `:mode` (`:const`, `:var` (addressable), `:value`, `:type`, `:builtin`, `:commaok`,
    `:void`);
  - `:val`, the exact constant value;
  - `:def` and `:use`, the object: kind, package, name, declaring position and type;
  - `:sel`, the selection: field, method or method-expr; the **index path through embedded
    fields**; and whether it is indirect;
  - `:inst`, the inferred type arguments of a generic use;
  - `:implicit`, the objects of type-switch clauses and anonymous imports.
- **The package view.** `:init-order` is go/types' `InitOrder`, the spec's dependency order
  of package-level variables. A full helper adds:
  - method sets;
  - each named type's interface satisfactions within the program (for an itab cache, §4.5);
  - struct layouts from `types.SizesFor("gc", GOARCH)`, for `unsafe.Sizeof` and
    `Offsetof`;
  - the build configuration (GOOS, GOARCH, tags, Go version), and every `//go:` directive
    with its position.

Over the typed tree, g2c's converter (in Clojure, like j2c) makes the Go forms of §3. The
split mirrors j2c: the helper is dumb and complete, and all decisions about forms are in
Clojure, where they can be tested against the compiler.

### 2.3 Build configuration

Go packages are a different set of files per GOOS, GOARCH and tags. g2c must pick one
configuration, as GopherJS (`js/ecmascript`) and TinyGo do. Measured with `go list std`,
go1.27.1:

| configuration | packages with `.s` files | `.s` files | cgo packages | system interface |
|---|---:|---:|---:|---|
| linux/amd64 | 38 | 64 | 7 | Linux syscalls (hundreds) |
| tamago/amd64 | 37 | 62 | 0 | board hooks (§1.3) |
| js/wasm | 17 | 27 | 0 | `syscall/js` (a JS host) |
| **wasip1/wasm** | **16** | **26** | **0** | **36 `wasi_snapshot_preview1` imports** |

Under wasip1 most remaining `.s` files are empty stubs (`stub.s`, `empty.s`). What is left
outside runtime, reflect and atomics is `internal/bytealg` (3), `math` floor, `math/big` arith
(and the `math_big_pure_go` tag removes it), and `internal/cpu`. **Recommendation:** type-check
for `GOOS=wasip1 GOARCH=wasm` with tags `purego,math_big_pure_go`. (Decided otherwise:
`GOOS=tamago`, open question 5 in §9.)

- `int` and `uintptr` are 64-bit there, which suits `long`.
- The OS layer is a small, documented interface. It is implementable on the JVM and later in
  the bare-metal runtime as host calls.
- The alternative is a custom GOOS (`arbace`) through an overlay, as GopherJS and TinyGo do.
  That is more control and more work. With CL 821260 (custom GOOS, `GOOSPKG`) it may become
  an upstream mechanism.

### 2.4 Feasibility experiment

`.tmp/g2c/godump/main.go`, 349 lines of Go, is described in §10. It loads a package's
files for the current configuration (`go list`), type-checks them with `go/types` (importing
dependencies from export data through `importer.ForCompiler(fset, "gc", nil)`), and prints
the AST generically by reflection with the annotations above. With `-meta` the annotations
are reader metadata.

| package | time | output | type errors |
|---|---:|---:|---:|
| `unicode/utf8` | 43 ms | 0.35 MB | 0 |
| `internal/strconv` | 122 ms | 1.6 MB | 0 |
| `sort` | 188 ms | 0.52 MB | 0 |
| `fmt` | 659 ms | 1.5 MB | 0 |
| `sync` | 322 ms | 0.47 MB | 0 |
| `reflect` | 809 ms | 4.3 MB | 0 |
| **`runtime`** | **1.9 s** | **31 MB** | **0** |

- **Determinism:** two runs on `runtime` are byte-identical. The walk follows AST order and
  never iterates a map.
- **Readable:** `clojure.core/read` (the frozen `clojure/`) reads the `-meta` dump of
  `runtime` in 3.0 s, with 186,447 forms carrying metadata. `clojure.edn/read` reads the
  plain variant in 1.9 s.
- **What the decisions look like** (from `sample.go`, §10):
  - `n.Move(Small)` with `const Small = Big >> 98` (`Big = 1 << 100`): the argument carries
    `:mode :const :t "int32" :val "4"`, so the untyped constant is already converted to its
    context type.
  - `p.X` in a method on `*Point`: `:sel {:kind :field :path [0] :indirect true :recv "*Point"}`.
  - Uses of the generic `Stack` carry `:inst {:targs [...]}`.
  - The type-switch's per-clause variables appear as `:implicit` objects.
- **Line info:** every node carries `line:col`. The converter keeps positions as `:line` and
  `:column` metadata (what Clojure's `LineNumberingPushbackReader` attaches anyway). The
  compiler writes Go's lines into `LineNumberTable`, with `SourceFile` set to the `.go` name,
  so stack traces and `runtime.Caller` show Go positions. That matters: go-lisp had to exclude
  15 test programs that check their own line numbers.

Conclusion: the front end is a solved problem. Its cost is the converter, not the reading.

## 3. Go forms: a sketch

Spirit: as the class forms are for Java (SPEC §1):

- one form per Go declaration, in Go's order;
- Go's names verbatim (capitalisation *is* export, so no munging, unlike go-lisp's D18);
- types as tags and type forms;
- Clojure's code vocabulary where its meaning fits;
- what go/types infers written out where Arbace's compiler would otherwise need a full checker;
- the Clojure reader only.

All names below are **tentative** (open question 4; adopted as proposed, §9). Top-level Go
declarations are written with the alias `go` (for `arbace.go`): `go/type`, `go/func`,
`go/method`, `go/var`, `go/const`. That way they never shadow `clojure.core`'s `type` or the `var` special form.

### 3.1 Packages, files, imports, build tags

- A Go package is a namespace: `net/http` becomes `go.net.http`, with the package name kept
  as metadata. An import path with dots or dashes (`golang.org/x/sys/unix`) is munged by one
  fixed rule: `go.golang_org.x.sys.unix`, with the path kept as `:go/path` metadata.
- Layout follows SPEC §9.1: `go/strconv.clj` holds `(ns go.strconv ...)` and one
  `(load "strconv/atoi")` per Go file. Each file holds `(in-ns 'go.strconv)` and its forms.
- Imports become `(:require [go.unicode.utf8 :as utf8])`. A use is `utf8/RuneLen`, like a
  static member in the class forms. Dot imports and blank imports (`_`) are `(:refer :all)`
  and a plain `:require`.
- Build tags are resolved by the helper: the forms are for one configuration. The
  configuration is recorded in the package namespace's metadata. Files excluded by tags are
  not converted. A multi-configuration source, where one form set covers several GOOS, is out
  of scope (open question 7; so decided, §9).

### 3.2 Types

| Go | type form |
|---|---|
| `int32`, `string`, `error`, `any` | `int32`, `string`, `error`, `any` (predeclared names, never Java's) |
| `*T`, `[]T`, `[4]T`, `map[K]V` | `(* T)`, `(slice T)`, `(array 4 T)`, `(map K V)` |
| `chan T`, `<-chan T`, `chan<- T` | `(chan T)`, `(chan :recv T)`, `(chan :send T)` |
| `func(int) (int, error)` | `(func [int] [int error])` |
| `struct{...}` (anonymous) | `(struct ^int32 X ^int32 Y)` |
| `interface{ M() int; io.Reader }` | `(interface (M ^int []) io/Reader)` |
| `Pair[string, int]` | `(Pair string int)` (as class forms write generics, SPEC §4.3) |
| constraint `~int \| ~float64` | `(\| (~ int) (~ float64))` |

As in SPEC §4.3, a symbol type is a tag (`^int32 x`) and a compound one needs the map form
(`^{:tag (slice byte)} b`). Declarations:

```go
type Point struct{ X, Y int32 }
type Named struct {
	Point          // embedded
	Name string `json:"name"`
}
type Celsius float64
type Shape interface{ Area() float64 }
type Stack[T any] struct{ items []T }
type IntAlias = int
```

```clojure
(go/type Point (struct ^int32 X ^int32 Y))
(go/type Named (struct ^:embedded Point ^{:tag string :go/tag "json:\"name\""} Name))
(go/type Celsius float64)
(go/type Shape (interface (Area ^float64 [])))
(go/type Stack :type-params [T] (struct ^{:tag (slice T)} items))
(go/type ^:alias IntAlias int)
```

Embedding is metadata on a field whose name is its type's, which is what Go makes of it.
Struct tags are strings in metadata.

### 3.3 Functions, methods, multiple results

```go
func (p *Point) Move(dx int32) { p.X += dx }
func (p Point) String() string { return fmt.Sprintf("(%d,%d)", p.X, p.Y) }
func Divmod(a, b int) (q, r int) { q = a / b; r = a % b; return }
func Map[T, U any](xs []T, f func(T) U) []U { ... }
```

```clojure
(go/method Move [^{:tag (* Point)} p ^int32 dx]
  (set! (.-X p) (+ (.-X p) dx)))

(go/method String ^string [^Point p]
  (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))

(go/func Divmod [^int a ^int b] :results [^int q ^int r]
  (set! q (quot a b))
  (set! r (rem a b))
  (return))

(go/func Map :type-params [T U] ^{:tag (slice U)} [^{:tag (slice T)} xs ^{:tag (func [T] [U])} f]
  ...)
```

- The receiver is the first parameter, as in `deftype` and class-form methods. Its tag decides
  value or pointer receiver.
- A single unnamed result is the parameter vector's tag, as in `defn`. Several or named results
  go in `:results [...]`. Named results are mutable locals, and `(return)` returns them.
- Multiple values are bound by **a list as binding target**. It is an error in Clojure today,
  so by SPEC principle 1 it is free for a new meaning:
  `(let [(values q r) (Divmod 7 2)] ...)`. Underscore discards a value:
  `(let [(values _ err) (f)] ...)`. Multiple assignment uses the same shape,
  `(set! (values a b) (values b a))`, with Go's evaluate-all-then-assign order.
- Variadics are `[& ^{:tag (slice T)} xs]`, and spreading is `(f a (spread xs))`.

### 3.4 Code

Go bodies reuse the class forms' code vocabulary (SPEC §5): `let` with `^:mutable`, `set!`,
`if`/`when`/`cond`, `loop`/`recur`, `label`, `break`, `continue`, `return`, `switch`. The
converter restructures as j2c does (CONVERTER-NOTES "How it works" 4-6). Go-specific forms:

| Go | form |
|---|---|
| `x := e`, `var x T` | `(let [x e] ...)`, `(let [^T x (zero T)] ...)`; `^:mutable` if assigned |
| `p.f`, `p.M(a)`, `pkg.F(a)` | `(.-f p)`, `(.M p a)`, `(pkg/F a)` |
| `&x`, `*p`, `*p = v` | `(addr x)`, `@p`, `(set! @p v)` |
| `T{X: 1}`, `[]int{1, 2}`, `map[K]V{k: v}` | `(lit T {:X 1})`, `(lit (slice int) [1 2])`, `(lit (map K V) {k v})` |
| `a[i]`, `a[i] = v`, `s[lo:hi:max]`, `len(s)` | `(aget a i)`, `(aset a i v)`, `(subslice s lo hi max)`, `(len s)` |
| `T(x)` conversion, `x.(T)`, `v, ok := x.(T)` | `(conv T x)`, `(assert T x)`, `(let [(values v ok) (assert T x)] ...)` |
| `for i := 0; i < n; i++`, `for cond`, `for {}` | `loop`/`recur` or `while` as in j2c; `(while true ...)` |
| `for k, v := range x` (slice, string, map, chan, int, func) | `(range [k v x] ...)` |
| `switch`, `fallthrough` | `(switch e ...)` (SPEC §5.8); a fall-through repeats the next arm, as j2c does |
| `switch x := v.(type)` | `(type-switch [x v] nil ... fmt/Stringer ... [int int64] ...)` |
| `goto L` | restructured; otherwise `(go/blocks ...)` (§3.6) |
| `go f(x)`, `defer f(x)` | `(go (f x))`, `(defer (f x))`; arguments evaluated at once, as in Go |
| `panic(v)`, `recover()` | `(panic v)`, `(recover)` |
| `ch <- v`, `<-ch`, `v, ok := <-ch` | `(>! ch v)`, `(<! ch)`, `(let [(values v ok) (<!? ch)] ...)` |
| `select` | `(select (>! ch i) body (<! done) body :default body)` |
| `func(x int) int {...}` | `(fn ^int [^int x] ...)` with Go capture semantics (below) |

The channel names are core.async's, which a Clojure reader already knows. Three semantic
points decide the design:

- **Operators mean Go's operators in Go bodies.** Go has no mixed-type arithmetic: both
  operands of `+` have one type, apart from shift counts. So `(+ a b)` on `uint8` operands is
  `uint8` wraparound, on `string` operands concatenation, and on `float32` single precision.
  The alternative would be class forms' explicit `unchecked-*` operators plus explicit
  narrowing after every narrow operation. That is faithful to principle 1 but unreadable.
  Recommendation: a `go/func` body is a **Go context**, the way a class body changes name
  resolution (SPEC §5.2). It is the one place where Clojure's `+` (overflow-checked) does not
  apply. This is a conscious exception to principle 1 (open question 3; so decided, §9).
- **Closures capture variables, not values.** A Go `func` literal captures by reference, and
  since Go 1.22 each loop iteration has its own variable. Class forms forbid capturing a
  `^:mutable` local (SPEC §5.3), which makes that an error today and so free for Go's meaning:
  a captured mutable local becomes a shared cell. Only mutable captured locals pay.
- **Zero values and copies are implicit, as in Go.** Writing `(copy x)` at every struct
  assignment would bury the code. The compiler knows each expression's type (§3.7), so it
  copies where Go copies: assignment, passing, return, storing in an interface, and range
  values.

Example, the sample's `Safe` and `Pipeline` (Go in §10):

```clojure
(go/func Safe [^{:tag (func [] [])} f] :results [^error err]
  (defer ((fn []
            (let [r (recover)]
              (when (some? r)
                (set! err (fmt/Errorf "recovered: %v" r)))))))
  (f)
  (return nil))

(go/func Pipeline ^int [^int n]
  (let [ch (make (chan int))
        done (make (chan (struct)))]
    (go ((fn []
           (defer (close ch))
           (range [i n]
             (select
               (>! ch i) nil
               (<! done) (return))))))
    (let [^:mutable total 0]
      (range [v ch] (set! total (+ total v)))
      total)))
```

### 3.5 Constants, `iota`, untyped values, literals

The converter writes each constant's **value**, folded by go/types, with its type. Untyped
constants stay untyped (`^:untyped`), because their exact values matter in further constant
expressions in importing packages. The Clojure reader has the number types this needs:

```clojure
(go/const ^Weekday Sunday 0)             ; iota; the source expression in :go/src metadata
(go/const ^:untyped Big 1267650600228229401496703205376N)   ; 1 << 100, a BigInt
(go/const ^:untyped Third 1/3)                              ; exact untyped float, a ratio
(go/const ^:untyped Pi 3.14159265358979323846264338327950288419716939937510582097494459M)
```

Floats are kept exact (a ratio or BigDecimal), because Go's untyped float constants are exact.
In code, constants used in a typed context are written as typed literals: the experiment
already gives `:t "int32" :val "4"`.

Strings and runes:

- A Go string is bytes. A literal that is valid UTF-8 is a Clojure string, and the compiler
  encodes it as UTF-8 bytes.
- An invalid one is `(go/bytes-str "ÿ..." [0xff 0x00])`, or a tagged literal
  `#go/bytes [...]` if the reader registers it.
- A rune is an `int32` constant. A rune in the Basic Multilingual Plane can be written `\a`
  (a Clojure char literal). Above it the rune is written as a number, because Clojure chars
  are UTF-16 units and metadata cannot attach to a number. `complex128` constants are
  `(complex 1.0 2.0)`.

### 3.6 Directives, `unsafe`, cgo, assembly, `init`, `goto`

- **`init`:**
  - Package-level variables are written in go/types' `InitOrder`, not in source order. Then
    loading the namespace runs them in Go's order, and the compiler does not have to compute
    it.
  - `func init` (there may be several per file) becomes `(go/init ...)`, run in file order
    after the variables.
  - Package initialisation is the namespace's load. Imports load first, which is Go's order
    as long as there are no import cycles, and Go forbids them.
- **`//go:` directives** are metadata on the declaration: `^{:go/noinline true}`,
  `^{:go/linkname "runtime.nanotime"}`, `^{:go/embed ["data.txt"]}`, `^{:go/nosplit true}`.
  Each back end honours the ones it can. The JVM back end resolves `linkname` only against
  its runtime shim (§4.8). `go:embed` is resolved at conversion time into the data.
- **`unsafe`:** the forms are just `(unsafe/Pointer p)`, `(unsafe/Sizeof x)` and so on.
  Whether they can be compiled is the back end's business (§4.4, §5).
- **cgo:** out of scope for the JVM. For phase b it is irrelevant: there is no C on bare
  metal, and TamaGo has none.
- **Assembly (`.s`):** g2c does not translate Go's assembler. A body-less `go/func` (Go's
  declaration of an assembly function) is a hole the back end fills: by a pure-Go fallback
  (most packages have one behind `purego`), by a hand-written shim (JVM), or, in phase b, by
  Arbace's own assembler forms (§5.3).
- **`goto`:** the stdlib has 402 `goto` lines in 82 files, 30 of the files in `runtime`; the
  rest are mostly parsers and `compress/flate`. Go's `goto` cannot jump into blocks or over
  declarations, so most uses become `label`/`break`/`continue` (go-whim's Clojure backend did
  this for C, `doc/CLOJURE.md`). The rest are a `(go/blocks [:L1 ...] [:L2 ...])` form: basic
  blocks in a `loop` over a state keyword, which the compiler emits as real jumps.

### 3.7 Who infers what

As in SPEC §6-§7: the compiler derives what follows from declarations, and the converter writes
what follows from type inference. Proposed split:

| decided by | what |
|---|---|
| converter (from go/types) | constant values and their types; the type of each `:=` local whose initializer's type is not obvious (generic results, untyped constants); type arguments of generic calls `((inst slices/Sort int) xs)`; method expressions; `range` kinds; which `switch` is a type switch; init order; implicit `&`/`*` in method calls on addressable values; conversions to interface types at assignments |
| compiler (from declarations) | struct layout, zero values, copies, embedding promotion (paths follow from the struct declarations), method sets, interface satisfaction, comparability, the types of the remaining expressions by forward propagation |

So the Go-forms compiler is a *forward* type propagator over fully declared signatures, like
the class forms compiler, not a Go type checker. Hand-written Go forms at the REPL
need the same: declared signatures, with `:=` inferred forward. Generic inference at the REPL
would need part of types2. That is the place to use g2c on `go/types` itself, later and only if
wanted.

## 4. Phase a: Go semantics on the JVM through Arbace

The Go-forms compiler lowers to class forms (one Java class per Go package for functions and
globals, one per struct type) and reuses the class forms compiler's code generation. The table
says what runs well, what is slow, and what is impossible.

### 4.1 Integers, floats, strings

| Go | JVM | cost |
|---|---|---|
| `int`, `int64`, `uint64`, `uintptr` | `long`; unsigned ops via `Long.divideUnsigned`, `remainderUnsigned`, `compareUnsigned`, `>>>` | native speed; HotSpot intrinsifies them |
| `int32`, `uint32`, `rune` | `int`; `Integer.*Unsigned`, `toUnsignedLong` | native |
| `int8`, `int16`, `uint8`, `uint16` | `int` with `i2b`/`i2s`/`& 0xff`/`& 0xffff` after operations that can overflow; `byte[]` and `short[]`/`char[]` in arrays | a mask per operation; cheap |
| shifts | Go: counts ≥ width give 0 (or -1), a negative count panics. JVM: counts are masked. | a compare per non-constant shift |
| `/`, `%` by zero; `MinInt64 / -1` | `ArithmeticException` mapped to a runtime-error panic; the same wrap | free |
| `float32`, `float64` | `float`, `double` (strict IEEE since Java 17) | native; float→int conversion of out-of-range values is implementation-defined in Go and saturates on the JVM, so it is fine |
| `complex64/128` | a final class of two floats or doubles (a value class with Valhalla) | allocation per operation unless scalar-replaced |
| `string` | **not `java.lang.String`**: a final class over `byte[]` (offset and length, or always exact). `len` is bytes, `s[i]` a byte, `range` decodes UTF-8, `==` and `<` compare bytes. Literals are constant byte arrays (condy, MODERN-COMPILER.md). | conversions at every Java interop boundary; `fmt` and strconv-heavy code allocates more than Go's |

### 4.2 Structs, arrays, copying

Go structs are values: assignment, passing and returning copy. A struct is a final class with
mutable fields, a no-arg constructor for the zero value, `copy()` and `set(other)`. A field of
struct type is an *owned* inner object, created with its parent and copied deeply. That is
go-whim's Java choice too: `a.set(b)`, "a struct member of a struct is its own object". Arrays
`[N]T` are Java arrays, copied on assignment, with struct elements filled at creation.

- **Runs well:** code that passes pointers to structs, which most Go code does for large ones.
- **Slow:** small structs passed by value in hot loops, such as `Point` and `time.Time` (3
  words), and `[]Point` slices. Each copy allocates unless C2's escape analysis scalar-replaces
  it, which it does only when the copy does not escape and the code inlines. Slices of small
  structs become arrays of pointers: worse locality, and GC load that Go does not have.
  Mitigations:
  - skip the copy when the source is dead after the copy, or when neither side is ever
    mutated or addressed (a per-type "immutable after construction" analysis: many Go structs
    are);
  - later, Valhalla value classes for such types (JDK 28 preview).
- **Struct equality** (`==` is field-wise) and use as map keys get generated `equals` and
  `hashCode` with Go's semantics: `+0.0 == -0.0`, and NaN is never equal, so NaN keys never
  match.

### 4.3 Pointers, interior pointers, slices

| pointer to | representation | note |
|---|---|---|
| a struct (`&T{}`, `new(T)`, `&x` of a struct variable) | the struct object itself | the common case, free. `*p = v` is `p.set(v)`. |
| a struct-typed field or array element (`&n.Point`) | the owned inner object | free, thanks to owned inner objects |
| an address-taken local scalar | a cell object (`IntRef`, `LongRef`, `Ref<T>`); the local lives in it | escape analysis removes it when the pointer does not escape |
| a scalar field (`&p.X`) | a field reference: base object plus a `VarHandle` (or a generated accessor class per field) | allocation per `&`; cached per (object, field) to make `==` work |
| an array or slice element (`&a[i]`) | (backing array, index) | as GopherJS's `$indexPtr` |

**Pointer equality** must be identity of the *location*. So reference objects compare by (base
identity, field or index), and `p == q` on pointer types is a static call, not `if_acmpeq`.
Pointers to struct objects keep plain identity.

**Slices** are an object (backing array, offset, length, capacity), with one class per element
kind (`ByteSlice`, `IntSlice`, ..., `ObjSlice`; JGo did the same). Aliasing is exact, because
two slices share the array. `append` follows Go's growth rule (`runtime/slice.go`'s
`nextslicecap`) so that capacities, and with them aliasing behaviour, match Go's. Code that
relies on that (`append` to a sub-slice overwriting the parent) is legal Go and is tested by
Go's tests. Bounds checks are explicit (`0 ≤ i < len`). The JVM's own check covers only the
backing array.

### 4.4 `unsafe`

| use | JVM |
|---|---|
| `unsafe.Sizeof`, `Alignof`, `Offsetof` | constants from gc's layout for the configuration (the helper computes them) |
| `*T` ↔ `unsafe.Pointer` ↔ `*T` of the same `T`, `unsafe.Pointer` as an opaque handle (`atomic.Pointer`, `sync.Pool`) | fine: the reference itself |
| `unsafe.String`, `StringData`, `Slice`, `SliceData` over `[]byte`/`string` | possible over the byte-array representation, with copying or sharing rules |
| `(*[8]byte)(unsafe.Pointer(&x))`, `*(*uint64)(unsafe.Pointer(&f))` (reinterpretation) | **impossible in general.** Recognised idioms can be rewritten (`math.Float64bits` is `Double.doubleToRawLongBits`), and the rest fail at compile time. |
| `uintptr` arithmetic on addresses | **impossible** |

The stdlib has 8,185 `unsafe.Pointer` lines in 574 files, nearly all in `runtime`, `reflect`,
`sync`, `syscall`, `os` and `internal/*`, the packages the shim replaces. Leaf packages rarely
use it: `strconv`, `sort`, `container/*`, `encoding/*` and `regexp` have none, and `strings`
and `bytes` use it in one file (§10, table).

### 4.5 Interfaces, method calls, generics

- **Interface values** are references to objects that know their Go type. Struct objects carry
  it in their class. A value of a named non-struct type in an interface (`Celsius(3)`) is boxed
  in a per-type box class, so its dynamic type is `Celsius`, not `Double`. Predeclared types
  box in Arbace's own boxes, because `int` and `int64` must stay distinct dynamic types. A
  struct stored in an interface is copied.
- **Calls through interfaces** use structural satisfaction: a type implements interfaces it
  never names. Options:
  1. `invokedynamic` per call site, bootstrapped to look up the receiver's method by Go name
     and signature, with an inline cache keyed by class. This is the JVM's own answer to
     dynamic dispatch (MODERN-COMPILER.md ranks indy for keyword and reflective call sites,
     and both have since been adopted). It works incrementally at the REPL.
  2. Whole-program: generate a Java interface per Go interface and make each class implement
     every interface its method set satisfies. This is fastest (`invokeinterface`), but it
     breaks when a new interface appears after the type was compiled, which is the REPL case.

  Recommendation: 1, with 2 as an optimisation for closed-world builds. Type assertions and
  type switches use the same per-type descriptors.
- **Embedding:** promoted methods get forwarding methods on the outer type (Go's compiler
  generates wrappers too). Promoted fields are paths, which the compiler derives (§3.7).
- **Method values** (`f := x.M`) bind the receiver, copied if it is a value. Method
  expressions (`T.M`) are functions.
- **Generics:** gc uses GC-shape stenciling with dictionaries. On the JVM the simplest correct
  route is **monomorphisation per instantiation**: every type argument becomes concrete, so
  zero values, copying, `==` and arithmetic on `~int | ~float64` all compile directly. It costs
  code size, and Go's generic code is small. Dictionaries for reference-shaped type arguments
  can come later.

### 4.6 Goroutines, channels, `select` — measured

Goroutines map to **virtual threads**: cheap, preemptible at blocking points, with growable
heap stacks, and since JDK 24 (JEP 491) not pinned by `synchronized`. They are daemon threads,
so returning from `main` ends the program, as in Go. An unrecovered panic in any goroutine must
end the whole program (an uncaught-exception handler that exits with Go's status 2).
`runtime.Gosched` is `Thread.yield`, and `GOMAXPROCS` is the scheduler's parallelism.

Measured on this machine (64 cores, `.tmp/g2c/chbench`, §10), Go 1.27.1 against JDK 26
virtual threads with the scheduler's parallelism set like `GOMAXPROCS`. The driver runs on a
virtual thread, as Go's `main` is a goroutine.

| | Go, 1 P | Go, 8 P | JVM, parallelism 1 | JVM, parallelism 8 |
|---|---:|---:|---:|---:|
| unbuffered ping-pong, round trip | 400-560 ns | 450-540 ns | 485-490 ns (`SynchronousQueue`), 540-620 ns (lock + conditions) | 530-780 ns (`SynchronousQueue`), **4,750-7,760 ns** (lock + conditions) |
| spawn 1M goroutines/threads that finish at once | 1.2-1.6 s | 0.67-0.71 s | 0.15-0.20 s | 0.57-0.94 s |
| buffered channel (cap 128), per message | 41-66 ns | 61-64 ns | 27-40 ns (`ArrayBlockingQueue`) | **230-330 ns** |

Reading:

- **Creation and the uncontended path are as cheap as Go's**, sometimes cheaper. (Go's spawn
  figures on this VM are slower than usual, so treat the comparison as order of magnitude.)
- **The JDK's blocking queues and a naive lock-based channel fall apart under parallel
  scheduling** (5-15 times worse). Go's channels hand a value straight to a parked receiver
  (`runtime/chan.go`'s `send` to a waiting `sudog`) and spin briefly.
- So channels and `select` must be **Arbace's own implementation**. The sensible one is
  Go's: `chan.go` plus `select.go` (1,615 lines) transcribed through g2c onto a small
  park/unpark shim (`LockSupport`). `select` locks every involved channel in a global order
  (Go uses addresses; on the JVM, a per-channel sequence number) and randomises the order of
  ready cases, which Go programs and tests rely on for fairness.
- Not available on the JVM: deadlock detection ("all goroutines are asleep") needs a global
  count of goroutines blocked in Arbace's own primitives (approximable), goroutine ids, and
  per-goroutine stack size limits. Go stacks grow to 1 GB. Virtual thread stacks grow in the
  heap but are bounded by `-Xss`-like limits, so very deep recursion can hit
  `StackOverflowError`, which must become Go's fatal "stack overflow".

### 4.7 `defer`, `panic`, `recover`

- `defer` pushes a closure (with its arguments already evaluated) on a per-call list. A
  function with defers compiles to `try { body } catch (Throwable t) { ... } finally { run
  defers }`. Defers in loops are a dynamic list. Go 1.14's "open-coded defers" (static ones
  inlined at each exit) are the obvious optimisation, and the class forms' `finally` gives
  them almost for free.
- `panic(v)` throws `GoPanic(v)`. Runtime errors map from JVM exceptions to Go's
  `runtime.Error` panics with Go's messages, which tests compare:
  - `NullPointerException` → nil dereference;
  - index exceptions → index out of range, which Arbace checks itself to get Go's message;
  - `ArithmeticException` → integer divide by zero;
  - `ClassCastException` → interface conversion.
- `recover()` returns the current panic only when called *directly* by a deferred function
  while panicking. Implementation: the frame's catch sets a per-goroutine "panicking" record
  before running its defers. `recover` in a function that is itself running as a deferred call
  (known statically: the closure is marked when deferred) clears it and returns the value. If
  it was cleared, the function returns normally with its named results. Results are therefore
  cells when a deferred closure can assign them (the `Safe` example).
- `runtime.Goexit` is a non-catchable throwable that runs defers.
- Cost: a `try` region is free on the JVM until it throws. Panics are exceptions, so they are
  expensive, but Go uses them rarely (67 `recover()` lines in std).

### 4.8 Maps, finalizers, the `runtime` package, reflection

- **Maps:**
  - Use Arbace's own hash map with Go semantics: keys hash and compare by Go's `==`. NaN keys
    never match, so `m[NaN] = 1` twice gives two entries. Interface keys hash their dynamic
    value, and keys whose dynamic type is not comparable panic.
  - Iteration order is randomised per `range`, from a random start position, because tests
    check that code does not depend on it.
  - Deletion during iteration is allowed, and insertion may or may not be seen.
  - `clear(m)`; `maps.Keys` and the like are iterators (Go 1.23 range-over-func).
  - Go 1.24's Swiss-table implementation (`internal/runtime/maps`, 5,005 lines) uses
    `unsafe`, so it is a source of ideas, not of code, on the JVM.
  - "concurrent map writes" detection is best effort.
- **Finalizers:**
  - `runtime.SetFinalizer` maps to `java.lang.ref.Cleaner` only approximately. Go runs a
    finalizer once, with the object resurrected and in dependency order; a cleaner cannot see
    the object.
  - `runtime.AddCleanup` (Go 1.24) maps exactly to `Cleaner`.
  - `weak.Pointer` maps to `WeakReference`, and `unique.Make` to a weak interning map.
  - `runtime.KeepAlive` is `Reference.reachabilityFence`.
- **The `runtime` package:** hand-written (the shim), as GopherJS and TinyGo do:
  - `Caller` and `Callers` through `StackWalker`, with Go positions from the line tables
    (§2.4); PCs are synthetic ids, and `FuncForPC` maps them back.
  - `NumGoroutine`, `GC` (`System.gc()`), `ReadMemStats` (approximate, from MXBeans),
    `GOOS`, `GOARCH`, `Version`.
  - `LockOSThread` is a no-op: virtual threads cannot own carriers.
  - The internal linkname targets that std expects: `sync`'s `runtime_Semacquire` and
    `runtime_notifyList*`, `time`'s timers, `sync/atomic`'s operations
    (`VarHandle`, sequentially consistent, which matches the Go memory model's atomics),
    `internal/godebug`, and `poll`'s runtime hooks.

  With that shim, `sync` itself (mutex, RWMutex, WaitGroup, Once, Pool, Map, Cond) can be
  converted from Go's source rather than rewritten. It is the first test of the shim boundary.
- **Reflection:** `reflect` is about 8.8k lines over `unsafe` and gc's type descriptors, so it
  cannot be converted. It must be written for Arbace's representation:
  - type descriptors that the compiler emits per Go type (kind, size, fields with names, tags
    and offsets, method tables with names for `MethodByName`, element types);
  - `reflect.Value` over objects, cells and field references, with `MethodHandle`s for `Call`;
  - `StructOf` and `FuncOf` (Yaegi needs them) are optional at first.

  `fmt`, `encoding/json`, `text/template` and `testing` depend on it. TinyGo's
  reflect gaps are the main reason about 66 of its 161 std packages fail tests, which shows
  how central reflect is. **Reflect is on the critical path for verification**: `testing` needs
  `fmt`, which needs `reflect` (§6.2).
- **Data races:** Go's memory model lets a race on multiword values (interfaces, slices,
  strings) corrupt memory. On the JVM those are objects, published by reference, so races are
  safer than in Go and racy programs at worst behave differently, never crash. Atomics and
  `sync` give the happens-before edges the Go memory model promises (DRF-SC).

### 4.9 Summary for phase a

- **Runs well:** integer and float code (`strconv`, `math/bits`, `unicode/*`, hashing,
  `compress/*`), pointer-linked structures (`container/*`, trees), interfaces with inline
  caches, goroutines, `defer`/`recover`, generics by monomorphisation.
- **Slow (2-5 times Go, estimated):**
  - value-struct-heavy code;
  - string-heavy code (byte strings without the JDK's String intrinsics);
  - channel-heavy code until Arbace's channels are tuned;
  - `fmt` and reflection.
- **Impossible without an own runtime:** `runtime` itself; `unsafe` beyond the safe idioms;
  assembly (use `purego`); cgo; `go:linkname` into anything but the shim; exact finalizers;
  goroutine stack semantics; `os/signal` to the extent the JVM hides signals; `plugin`.

## 5. Phase b: Go forms on Arbace's own runtime

### 5.1 What transcribing Go's runtime means

Measured in go1.27.1 (`/usr/lib/go/src`, non-test lines):

| part | Go lines | `.s` lines |
|---|---:|---:|
| all of `runtime` | 121,454 | 54,596 (all OS/arch variants; about 150k Go and 64k `.s` with `internal/runtime` at master) |
| memory allocator (`malloc*`, `mheap`, `mcache`, `mcentral`, `mpagealloc*`, ...) | 11,731 | |
| collector (`mgc*`, `mbitmap*`, `mwbbuf`) | 13,280 | |
| scheduler, stacks, locks (`proc`, `runtime2`, `stack`, `preempt`, `lock_*`, `sema`) | 13,634 | |
| channels, select | 1,615 | |
| maps (`internal/runtime/maps`) | 5,005 | |
| tracebacks, symbol tables, panics | 5,120 | |
| TamaGo's runtime files (`*_tamago*`) | 1,371 | in the above |
| TamaGo module (boards, SoCs, `kvm/`, `amd64/`) | 33,779 | 4,131 |

The runtime is written in **runtime Go**, a dialect with obligations the compiler checks or
the authors uphold:

- `//go:nosplit`: 1,220 lines in std (stack-check-free functions, bounded frame size);
- `//go:nowritebarrier`, `nowritebarrierrec`, `go:systemstack`, `go:uintptrescapes`;
- `go:linkname`: 1,420 lines across std, 709 in `runtime`;
- no allocation in signal, GC and scheduler paths;
- `systemstack(func(){...})` switches to the g0 stack;
- `getg()` reads the current goroutine from a register (R14 on amd64).

Compiling it correctly requires the compiler to provide:

1. **Precise stack maps and pointer bitmaps** at every safepoint, plus register maps for
   asynchronous preemption, because the GC is precise and moves stacks.
2. **Stack growth:** every non-`nosplit` function's prologue compares SP with `g.stackguard0`
   and calls `morestack`. Stacks are contiguous and copied on growth, with pointers into
   them adjusted. Frames therefore need exact layout metadata.
3. **Write barriers:** every pointer store into the heap goes through the hybrid barrier
   while marking (`writeBarrier.enabled` checks plus `wbBuf`).
4. **The ABIs:** ABIInternal (registers, unstable, per architecture) for Go code, ABI0
   (stack) for assembly, and wrappers between them. The runtime's assembly (`asm_amd64.s`,
   `memmove_*`, `memclr_*`, `sys_*`) assumes them.
5. **The object file and linker contract:** `pclntab` (function tables used by tracebacks,
   GC and preemption), `funcdata` and `pcdata`, `moduledata`, type descriptors in gc's layout
   (`internal/abi`), itabs, `go:linkname` resolution, and the init task graph.
6. **Assembly:** Go's assembler dialect (Plan 9 syntax), 54k lines across arches. Only
   amd64 and arm64 (the two targets), and only the files TamaGo's build uses, are needed:
   about 60 files.

This is the substance of `cmd/compile`'s back end (`ssagen` 15k, `ssa` passes, `liveness` 3.5k,
`abi`, `amd64` 7k, `arm64` 2.8k lines, plus the generated rewrite rules) and `cmd/link`. A
reimplementation in Arbace is a multi-year project, and that is the main reason not to put it
first.

### 5.2 The shortcut: Go's toolchain as Arbace's first native back end

The user's existing work already composes into a path to the bare-metal REPL:

1. **Go forms → Go.** Print the forms back as Go source, or as go-lisp `.lgo`, which the
   go-lisp toolchain compiles directly. Either is a pure printer, and its correctness check is
   the round trip (§6.1).
2. **Go + TamaGo → an image.** `GOOS=tamago` with a board package. go-whim's
   `guest/tamago/board` already answers TamaGo's hooks through its monitor's doorbell.
3. **The monitor runs it.** go-whim's `vmm` runs it on KVM amd64 and on Hypervisor.framework
   arm64, with SMP. It has run Joker as a REPL in the box (17.7 MB image, 20-60 ms start;
   LISP-SANDBOX.md), with GC pauses measured and tuned.

What Arbace then still needs is to **run Clojure on that runtime**: Arbace's runtime (the
`arbace.lang` classes: persistent collections, vars, namespaces, the reader, the compiler)
expressed in Go forms instead of class forms, plus a back end that turns compiled Arbace code
into Go forms. There are three possibilities, increasingly ambitious:

- **(i) An interpreter or bytecode VM written in Go forms.** Joker's approach. Its REPL in the
  box already works, so this is the baseline to beat.
- **(ii) Ahead-of-time Arbace → Go forms → gc.** Each namespace becomes Go code. The REPL
  compiles new forms to Go forms, and needs either an interpreter for them (Yaegi-like; Yaegi
  runs in the box) or to rebuild and restart an image. go-whim LISP-SANDBOX "A stack, sketched"
  discusses a compiler from forms to Go closures (two to five times an interpreter's speed).
- **(iii) Arbace's own code generator for the box,** emitting machine code into an executable
  arena the guest owns. The REPL is then native. The hard constraint is Go's GC: generated code
  must give stack maps or keep no Go pointers in registers across safepoints (LISP-SANDBOX:
  "the hard part is Go's GC").

In all three, the Go runtime and TamaGo stay what they are: compiled by gc, but **visible and
editable as Go forms in Arbace**, because g2c transcribes them (it type-checks `runtime` today,
§2.4). The self-sustaining property ("written in Arbace all the way down") is then reached by
replacing gc piece by piece, as stage 0's frozen `clojure/` was replaced:

| step | Arbace owns | still Go's toolchain |
|---|---|---|
| b1 | the sources of everything, as Go forms and Arbace code | gc, the assembler, the linker |
| b2 | the front end: Go forms are type-checked and lowered to an SSA-like IR in Arbace | gc's back end, fed through generated Go (or later through `go/ssa`-shaped data) |
| b3 | the back end for one architecture (amd64), verified against gc on Go's tests | the linker |
| b4 | the linker and object layout, `pclntab` | none: Arbace builds its image in the box |

Each step has the previous one as an oracle: same tests, same behaviour, and for b4 the same
runtime tables. This is the j2c discipline (compare against the reference toolchain) carried
to native code.

### 5.3 What Arbace's native compiler will need (b3-b4)

- An SSA IR with liveness for stack maps and register allocation. gc's `ssa` package is the
  model and can be studied through g2c's own output.
- The Go ABI for the chosen architectures (`src/cmd/compile/abi-internal.md`), or Arbace's own
  ABI with wrappers at the assembly boundary as gc has. Owning the ABI is the chance to add
  what the user wants and Go lacks, such as **proper tail calls**, which the JVM never gave
  (JOURNAL 2026-10-07).
- Stack-growth prologues and `morestack`, or a different stack strategy (segmented or fixed
  plus guard pages) with a matching runtime change. Changing the strategy forks Go's runtime
  semantics, so keep Go's.
- Write barriers emitted per Go's rules, and preemption points (loop back-edges, or
  asynchronous signals in TamaGo's case: interrupts).
- **Assembly forms:** a Clojure form for Go's assembler, roughly
  `(asm/TEXT runtime·memmove :nosplit [0 24] (MOVQ (FP to+0) DI) ...)`, transcribed from the
  `.s` files by a small converter. The Go assembler's own parser (`cmd/asm`) gives the tree,
  as go/types does for Go. About 60 files for the two targets.
- TamaGo's board support (`amd64/`, `kvm/` virtio, pvclock) is ordinary Go plus a few `.s`
  files. g2c converts it like any package, so it costs nothing extra once b1 works.

## 6. Verification

j2c's three checks (structure, compile back, suite) carry over, with Go's toolchain as the
oracle.

### 6.1 Round trip through Go (structure)

**Go → forms → Go** must give the same program, compared as go-lisp compares (trees after
normalising groups and parentheses) and, stronger, as **gc's export data and object code**.
Compile both with the same toolchain and compare the export data (types, method sets,
constants) and the generated code per function (`go build -gcflags=-S`, or the object files
with positions stripped). This is the analogue of SPEC §3's class equivalence. It is cheap,
needs no runtime, and validates converter and printer over all of `$GOROOT/src` (2,970 files
outside `cmd`) from the first week. Corpus: go-lisp's 11,588 files, of which the tamago-go and
go-lisp trees hold the tests. The system Go at `/usr/lib/go` is Alpine's and ships no
`_test.go` files and no `test/` directory, so the corpus must come from `/root/tamago-go`
(go1.27.1, 1,557 non-cmd test files, `test/` with 392 entries) or `/root/go-lisp`.

### 6.2 Behaviour on the JVM

1. **`$GOROOT/test` run programs:** single-file programs with `// run` and expected output
   (`.out` files). go-lisp ran 966 of them. Use the same harness as GopherJS, a fork of Go's
   `test/run.go` with a `knownFails` table, each entry with a reason (`usesUnsafe`,
   `needsRuntimeInternals`, `inspectsLineNumbers`, ...). A test that newly passes fails the
   run until it is taken off the list, as in GopherJS.
2. **Package tests with `go test` semantics:** run `TestXxx`, `ExampleXxx` (compare output)
   and, timed, `BenchmarkXxx` on the JVM. Compare with `go test -json` of the same package and
   configuration on real Go. Record results as `test/baseline-results.edn` does for Clojure's
   suite: per package, the set of passing tests, which must never shrink.
3. **Order**, by dependency closure under wasip1 and the shim's growth (sizes from §10):

| tier | packages | needs |
|---|---|---|
| 0 | `unicode/utf8` (578 lines), `math/bits` (918), `container/list`, `container/ring`, `unicode` (10.8k, tables) | core forms; a minimal test harness without `fmt` (`t.Errorf` with `%d`, `%s`, `%v` on basic types) |
| 1 | `strconv` (+ `internal/strconv`), `sort`, `slices`, `maps`, `container/heap`, `encoding/hex`, `encoding/base64`, `encoding/binary` (byte order part), `hash/*`, `strings`, `bytes` | generics, iterators, `errors` |
| 2 | `fmt`, `reflect`, `testing` itself, `encoding/json`, `text/template` | hand-written `reflect`; then the real `testing` package converted |
| 3 | `sync`, `sync/atomic`, `context`, `time` (timers), channel-heavy tests | the shim's semaphores, timers, channels (Go's `chan.go` converted) |
| 4 | `regexp`, `math/big` (pure Go), `compress/*`, `crypto/*` (purego), `bufio`, `io` | performance work |
| 5 | `os`, `io/fs`, `path/filepath`, `net` (wasip1 subset) | the 36 WASI calls on the JVM |
| 6 | runtime pieces as *libraries*: `internal/runtime/maps` algorithms (not code), `chan.go`, `select.go`, `sema.go` | phase a's own runtime, written in Go forms |

The standard of success per tier is GopherJS's list of known failures, not perfection.

### 6.3 Phase b

The same test runs in the box, on the TamaGo image:

- **b1:** the Go-printed forms built by gc must pass what the original sources pass. TamaGo's
  own CI runs std tests on a userspace target.
- **b2-b4:** Arbace's front end, back end and linker in turn, compared with gc on the same
  tests and, where meaningful, the same runtime tables (`pclntab` lookups, stack maps checked
  by `GODEBUG` GC stress modes such as `gcstoptheworld`, `gccheckmark`, `clobberfree`).

## 7. Plan

Estimates are agent-assisted calendar weeks at the pace of the class-forms work. For scale: the
class forms compiler is 7.6k lines and j2c 4.3k lines of Clojure, and spec, compiler,
converter and self-hosting took about two days with agents. They are rough, and the uncertainty
grows down the table.

| milestone | content | gate | estimate |
|---|---|---|---|
| **G0 Spec** | `doc/go/SPEC.md`: Go forms for every go/ast node and go/types decision (go-lisp's §4.1 table as checklist), naming, reader rules (Clojure reader), what the converter writes and what the compiler derives, open questions | user review, as for the class forms | 1 |
| **G1 Helper + converter** | `bin/g2c`: the Go helper (from `.tmp/g2c/godump`) and `arbace.g2c` converting to forms | all of `$GOROOT/src` (wasip1 config) converts; forms read by Arbace's reader; deterministic | 1-2 |
| **G2 Back to Go** | printer Go forms → Go | round trip over `$GOROOT/src` and `$GOROOT/test` equal by tree, and by export data and object code (§6.1); `test/` run programs pass from round-tripped sources | 1-2 |
| **G3 JVM core** | Go-forms compiler onto class forms: scalars, strings, structs, pointers, slices, maps, interfaces (indy), closures, defer/panic, monomorphised generics; minimal runtime shim and test harness | tier 0 and tier 1 package tests; `$GOROOT/test` run programs with a known-fails list | 4-8 |
| **G4 reflect + fmt + testing** | hand-written `reflect`; real `testing` | tier 2 | 3-6 |
| **G5 concurrency** | goroutines, Go's `chan.go`/`select.go` converted onto the shim, `sync` converted, timers | tier 3; channel benchmarks within 2 times Go's | 2-4 |
| **G6 breadth** | tiers 4-5, WASI on the JVM, performance (copy elision, string intrinsics) | GopherJS-class coverage of std | open-ended |
| **B1 Arbace in the box via gc** | Arbace's runtime and compiled namespaces as Go forms → gc + TamaGo → go-whim's monitor; a REPL in the box (interpreter or AOT, §5.2) | a self-hosted REPL on KVM amd64 and HVF arm64 | 6-12, mostly the Clojure runtime in Go forms, not g2c |
| **B2-B4 own native toolchain** | front end, SSA back end for amd64 then arm64, assembler forms, linker | Go's tests in the box against gc's; then the image built in the box | years; decide after B1 |

**Risks:**

- **Semantics creep on the JVM.** Every Go detail (copy points, `append` capacity rules, map
  NaN keys, shift counts, panic messages) is a test somewhere. The mitigation is the
  known-fails discipline and a narrow first tier.
- **Performance of value structs and strings on the JVM** may disappoint for some packages.
  It is acceptable if phase a is about *reusing solutions*, not speed. Valhalla (JDK 28+) helps
  later but is past the JVM freeze point (`arbace-for-java-26`).
- **`reflect` is large and central.** It gates `fmt` and `testing`, and so most verification.
  The minimal harness of tier 0 is the hedge.
- **Go releases move:** a new language feature about every year (range-over-func 1.23,
  generic aliases 1.24, core types removed 1.25). Pin a Go version per Arbace release, as
  TamaGo does (go1.27.1 locally), and let the helper reject newer language versions.
- **Phase b depends on TamaGo** (a fork) and, if it lands, upstream's custom GOOS (CL 821260).
  Keep the board code at the hook interface (`goos/goos.go`) so either works.
- **Scope confusion between the phases.** Phase a needs a *safe* subset of Go and a shim.
  Phase b needs the *unsafe* runtime dialect and a native compiler. Only the front end, the
  forms and the round trip are shared. The plan keeps those first so that neither phase
  blocks the other.

**What not to do:**

- Don't translate `runtime`, `reflect` or `internal/runtime/*` to the JVM. Write the shim, as
  every successful non-gc Go does.
- Don't emulate memory with a big `byte[]` heap to support general `unsafe`. That is
  go-whim's C model, right for C and wrong for Go, whose code is safe to 99%.
- Don't CPS-transform functions for goroutines. Virtual threads exist, and GopherJS's
  transform spreads through every indirect call.
- Don't write a Go type checker in Clojure. Use go/types through the helper. Revisit only for
  REPL generics, and then by g2c of `go/types`.
- Don't adopt go-lisp's surface syntax for Go forms. It is the right tool for its job (a
  second front end to gc), but it is not Clojure-reader compatible by design. Do reuse its
  coverage tables and its round-trip test.
- Don't begin phase b with an own native back end. Reach the REPL in the box through gc
  first.
- Don't promise Go binary compatibility (export data, ABI) in phase a.

## 8. Summary table

| Go feature | Go forms | phase a (JVM) | phase b (own runtime) |
|---|---|---|---|
| packages, imports, init order | ns per package; init order from go/types | good | good |
| build tags, GOOS/GOARCH | resolved by the helper (wasip1/wasm proposed) | good | per board (tamago) |
| integers incl. unsigned, wraparound | Go operators in Go context | good (masks, `*Unsigned`) | native |
| strings (bytes), runes | byte strings; `\a` or numbers | moderate (own string class) | native |
| structs as values, arrays | `go/type`, implicit copies | slow-ish (copies, allocation) | native |
| pointers to structs | `addr`, `@p` | good | native |
| interior pointers (fields, elements, locals) | `addr` | moderate (reference objects) | native |
| slices, `append` aliasing | `subslice`, `append` | good | native |
| maps (Go equality, random order) | `lit`, `range` | good (own map) | Go's map (transcribed) |
| interfaces, structural satisfaction | `interface`, `assert`, `type-switch` | good (indy caches) | itabs as Go |
| embedding, promotion | `^:embedded` | good (forwarders) | native |
| generics | `:type-params`, explicit `inst` | good (monomorphised) | stenciling as gc |
| multiple results | `values` binding targets | good (tuples, scalar-replaced) | registers (ABI) |
| closures (by-reference capture) | `fn` in Go context | good (cells for mutated captures) | native |
| defer / panic / recover | `defer`, `panic`, `recover` | good | native |
| goroutines | `go` | good (virtual threads) | Go's scheduler |
| channels, select | `<!`, `>!`, `select` | needs Go's algorithm (measured) | Go's chan.go |
| labeled break/continue, goto | `label`, `go/blocks` | good | native |
| const, iota, untyped constants | folded values, BigInt and ratios | good | good |
| `unsafe` (layout, opaque pointers) | `unsafe/...` | partial | full |
| `unsafe` (reinterpretation, uintptr math) | `unsafe/...` | **impossible** | full |
| `runtime` package | hand-written shim | shim only | transcribed Go runtime |
| `reflect` | hand-written | large but doable | Go's reflect |
| finalizers, weak, unique | | approximate / exact / exact | exact |
| `go:linkname` | metadata | shim targets only | full |
| assembly `.s` | holes | purego fallbacks, shim | assembler forms (about 60 files) |
| cgo | out of scope | no | no (no C in the box) |
| stack growth, precise GC, ABI | not in the forms | the JVM's | Arbace's native compiler (years) or gc first |

## 9. Open questions for the user

The user answered all ten on 2026-10-07 ("Decisions on g2c's open questions" in
[JOURNAL.md](JOURNAL.md#2026-10-07-decisions-on-g2cs-open-questions)). Each answer is noted
after its question.

1. **Priority of the phases.** Is phase a, Go libraries on the JVM before the freeze, worth
   G3-G6? Or should g2c go straight from G2 (round trip) to B1 (the box via gc)? The front
   end, spec and round trip serve both. Everything after differs.
   *Decided:* G0-G2, then B1. Phase a (G3-G6) is skipped, since the freeze would leave it behind.
2. **The box via gc.** Is it acceptable that the first bare-metal Arbace is built by Go's
   toolchain plus TamaGo, with "Arbace all the way down" reached by replacing gc step by step
   (§5.2)? The alternative is that Arbace's own native compiler is a precondition.
   *Decided:* yes, with gc as the reference while it is replaced layer by layer.
3. **Go context for operators.** In Go bodies, should `+`, `-`, `<<` and the others mean Go's
   operators at the operands' type (recommended, §3.4)? The alternative keeps Clojure's
   meaning everywhere and writes Go's arithmetic with explicit `unchecked-*` and narrowing
   forms, as class forms do for Java.
   *Decided:* the Go context, a deliberate exception to "Clojure keeps its meaning".
4. **Names.** The `go/type`, `go/func` and `go/method` heads; `(addr x)` against go-lisp's
   `(& x)`; core.async's `<!` and `>!` for channels; `(values ...)` binding targets; Go
   identifiers verbatim (recommended) against go-lisp's kebab-case with `-` for unexported.
   *Decided:* the survey's proposal, as listed.
5. **The build configuration** for phase a: `wasip1/wasm` with `purego,math_big_pure_go`
   (recommended), or an own `GOOS=arbace` overlay as GopherJS and TinyGo have.
   *Decided:* neither: `GOOS=tamago` for amd64 and arm64, the box's own configuration.
6. **Go version pinning:** go1.27.1, the version of the local toolchain and of TamaGo, for the
   first g2c. Who moves it, and when?
   *Decided:* pinned to TamaGo's release (go1.27.1 now). Claude proposes moves, the user
   approves, and the journal records them.
7. **One configuration per conversion,** or forms that keep build-tag alternatives (as source
   does) so that one Arbace package serves the JVM and the box?
   *Decided:* one configuration per conversion, recorded in the forms.
8. **The Go helper as a tool dependency:** g2c needs the Go toolchain at conversion time, as
   j2c needs javac. Is a small Go program under `bin/` or `tools/` acceptable in this
   repository (BSD-licensed Go APIs, Arbace's own code under EPL)? If yes, the experiment's
   `godump` becomes its seed. Converted Go code is BSD-3-Clause (Go, TamaGo), and
   `LICENSE.md` would need that license when the first converted package is kept.
   *Decided:* yes, under `tools/`, seeded from `godump`, under EPL. BSD-3-Clause goes into
   `LICENSE.md` once converted Go code is kept.
9. **Converted output in the repository?** j2c's output is regenerated, not tracked (the
   user's decision for j2c). The same for g2c? For phase b the transcribed runtime would
   eventually *become* source, as `arbace/` did for Clojure.
   *Decided:* regenerated, not tracked, until a package becomes hand-maintained source by a
   recorded decision.
10. **go-lisp's role:** keep it as a separate tool and use its toolchain as the `.lgo` back end
    (§5.2), or fold its round-trip corpus tests into Arbace's checks?
    *Decided:* its coverage tables, round-trip method and corpus are reused; no `.lgo` back end,
    and go-lisp stays a separate tool.

## 10. Experiments (reproducible; scratch in `.tmp/g2c/`, not tracked)

All on 2026-10-07: Go 1.27.1 (`/usr/lib/go`, Alpine package, no test files), OpenJDK
26.0.2.1, 64 cores.

1. **`godump`** (`.tmp/g2c/godump/main.go`, module `godump`, `go 1.27`, standard library only):
   - Usage: `godump [-meta] -o OUT.edn PKGDIR IMPORTPATH`.
   - It lists the package's `GoFiles` with `go list` in PKGDIR and parses them with
     `parser.ParseComments|SkipObjectResolution`.
   - It type-checks with `types.Config{Importer: importer.ForCompiler(fset, "gc", nil)}` and
     the `Info` maps `Types`, `Defs`, `Uses`, `Selections`, `Instances`, `Implicits`.
   - It prints every `ast.Node` by reflection as `(:kebab-node-name {:field value ...})`,
     skipping `Obj`, `Scope`, `Comments`, `Doc`, nil fields and most zero positions. Then it
     prints the annotations of §2.2, either after the map (plain, EDN) or, with `-meta`, as
     `^{...}` metadata on the form (Clojure reader).
   - Then `:init-order`.
   - Reading: `readedn.clj` (`clojure.edn/read`) and `readclj.clj` (`clojure.core/read` with
     `*read-eval*` false) on the frozen `clojure/`: `java -cp /root/arbace clojure.main
     readclj.clj FILE`.
   - Sample package `.tmp/g2c/sample/sample.go`: structs with embedding, pointer and value
     receivers, a generic `Stack[T]`, `iota`, `1 << 100`, uint8 wraparound, `defer`/`recover`
     with a named result, a goroutine with `select`, a type switch, an interior pointer
     `&n.Point.Y`. 41 kB of output, read in 18 ms.
2. **Package measurements:** per package, non-test lines, test lines (from `/root/tamago-go`),
   assembly files, `go list -deps` count, files importing `unsafe`, `go:linkname` lines:

| package | lines | test lines | `.s` | deps | unsafe files | linkname |
|---|---:|---:|---:|---:|---:|---:|
| unicode/utf8 | 578 | 1,027 | 0 | 1 | 0 | 0 |
| math/bits | 918 | 1,766 | 0 | 2 | 1 | 3 |
| strconv | 1,910 | 1,962 | 0 | 35 | 0 | 0 |
| sort | 2,174 | 1,848 | 0 | 36 | 0 | 0 |
| slices | 1,801 | 2,898 | 0 | 33 | 1 | 0 |
| strings | 2,444 | 4,441 | 0 | 42 | 1 | 0 |
| container/list | 235 | 372 | 0 | 1 | 0 | 0 |
| encoding/binary | 1,228 | 1,591 | 0 | 47 | 0 | 0 |
| encoding/json | 6,055 | 14,507 | 0 | 74 | 0 | 0 |
| fmt | 3,544 | 4,080 | 0 | 59 | 0 | 0 |
| sync | 1,711 | 3,550 | 0 | 34 | 8 | 3 |
| regexp | 2,777 | 2,994 | 0 | 49 | 0 | 0 |
| math/big | 10,219 | 11,826 | 12 | 65 | 2 | 8 |
| time | 6,593 | 7,051 | 0 | 43 | 5 | 13 |
| os | 11,765 | 17,793 | 0 | 53 | 28 | 12 |
| reflect | 8,844 | 12,157 | 14 | 43 | 9 | 44 |
| runtime | 112,843 | 42,270 | 182 | 30 | 293 | 709 |

   Even `strconv` has 35 dependencies, because every package implicitly depends on `runtime`
   and its internals. That is why the shim boundary, not the dependency closure, defines what
   phase a must provide. In go1.27, `encoding/json` is built on `encoding/json/v2` (its
   `GoFiles` are `v2_*.go`).
3. **Std-wide counts** (non-`cmd`, non-vendor, 2,970 files):

| construct | lines | files |
|---|---:|---:|
| `goto` | 402 | 82 |
| `fallthrough` | 106 | 58 |
| `select {` | 159 | 47 |
| `go func` | 68 | 45 |
| `defer` | 1,395 | 363 |
| `recover()` | 67 | 43 |
| `unsafe.Pointer` | 8,185 | 574 |
| `go:linkname` | 1,420 | 279 |
| `go:nosplit` | 1,220 | 230 |

   The tree has 852,765 non-test Go lines outside `cmd` and vendor, and 134,332 `.s` lines.
4. **Channels** (`.tmp/g2c/chbench`: `main.go` and `Bench.java`): ping-pong of 1M round trips
   over two unbuffered channels, spawn of 1M goroutines that each call `wg.Done`, and 10M
   messages through a channel of capacity 128. Go with `GOMAXPROCS` 1 and 8. Java with virtual
   threads, `-Djdk.virtualThreadScheduler.parallelism` 1 and 8, using `SynchronousQueue`, a
   `ReentrantLock` + `Condition` rendezvous channel, `CountDownLatch` and
   `ArrayBlockingQueue(128)`. Three or four repetitions each; the ranges are in §4.6.

## 11. Sources

Local:

- go1.27.1 (`/usr/lib/go`): `src/go/{ast,parser,types,constant,importer}`,
  `src/cmd/compile/internal/{syntax,types2,ssa,ssagen,liveness,abi}`,
  `src/cmd/compile/abi-internal.md`, `src/runtime/` (files in §5.1),
  `src/internal/runtime/maps`, `src/syscall/*_wasip1.go`.
- tamago-go `/root/tamago-go` (tamago1.27.1, `src/runtime/*tamago*`) and the module
  `github.com/usbarmory/tamago@v1.27.1` in `/root/go/pkg/mod` (`goos/`, `board/`, `amd64/`,
  `kvm/`). BSD-3-Clause ("Copyright (c) The TamaGo Authors").
- go-lisp `/root/go-lisp`, commit `2483a496`: `CLAUDE.md`, `golisp/{README,DESIGN,SPEC}.md`,
  `src/cmd/compile/internal/syntax/lisp_*.go`, `lisp_run_test.go`.
- go-whim `/root/go-whim`, commit `80c49bf`: `CLAUDE.md`, `doc/{GO-LISP,CLOJURE,JAVA,GUEST,LISP-SANDBOX}.md`,
  `crefactor/togo/`.
- golang.org/x/tools `v0.44.1-0.20260420230617-19499e7caabc` (module cache): `go/ssa`.
- Arbace: `doc/classes/SPEC.md`, `doc/classes/CONVERTER-NOTES.md`, `doc/MODERN-COMPILER.md`,
  `doc/JOURNAL.md` (2026-10-07 entries), `arbace/j2c/`.

Web (checked 2026-10-07):

- GopherJS, master `490705b`, v1.21.0: https://github.com/gopherjs/gopherjs, `compiler/functions.go`,
  `compiler/internal/analysis/info.go`, `compiler/expressions.go`, `compiler/prelude/types.js`,
  `compiler/natives/src/`, `tests/gorepo/run.go`, `doc/packages.md`.
- TinyGo, dev `f6d269f`, v0.42.0: https://github.com/tinygo-org/tinygo,
  https://tinygo.org/docs/concepts/compiler-internals/pipeline/,
  https://tinygo.org/docs/reference/lang-support/stdlib/ (counts approximate).
- Yaegi, master `fcb76d1`: https://github.com/traefik/yaegi.
- llgo: https://github.com/xgo-dev/llgo (v1.0.6); gollvm: https://go.googlesource.com/gollvm
  (`605d1b6`).
- JGo: https://github.com/thomasmodeneis/jgo; JGolangCompiler: https://github.com/Rybchits/JGolangCompiler.
- JEP 401 Value Objects (Preview), JDK 28: https://openjdk.org/jeps/401; Kotlin inline classes:
  https://kotlinlang.org/docs/inline-classes.html.
- TamaGo, master `7af6ac9`, v1.27.1: https://github.com/usbarmory/tamago (`goos/goos.go`);
  go-boot: https://github.com/usbarmory/go-boot; bare-metal proposal
  https://github.com/golang/go/issues/73608 and https://go.dev/cl/821260; `cmd/link -D`
  https://github.com/golang/go/issues/74945.
- Joker v1.10.0: https://github.com/candid82/joker; gostd: https://github.com/jcburley/joker/blob/gostd/GOSTD.md.
- Go: memory model https://go.dev/ref/mem (2022-06-06); spec https://go.dev/ref/spec (go1.27);
  loopvar https://go.dev/blog/loopvar-preview; Go 1.23 range-over-func https://go.dev/doc/go1.23;
  Go 1.24 generic aliases and Swiss maps https://go.dev/doc/go1.24; core types
  https://go.dev/blog/coretypes; contiguous stacks https://go.dev/doc/go1.4; register ABI
  https://go.dev/doc/go1.17; assembler https://go.dev/doc/asm; JEP 491 (virtual threads
  without pinning on `synchronized`, JDK 24).
