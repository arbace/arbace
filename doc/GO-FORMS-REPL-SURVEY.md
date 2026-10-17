# Go forms at the REPL — survey

Status: survey, 2026-10-17, written at the user's request. The user, 2026-10-17: "go forms should be
first class citizens like arbace and java forms: interpreted in the repl. bring arbace/clojure,
java and go forms to the same level in repl when it is appropriate. haskell forms are good as
data for now". Nothing here is adopted. The decisions (§6, G1-G17) belong to the user. The proposals
are numbered G1, G2, ... as the request asked. They have nothing to do with the fixpoint's
stages G1-G3 (`bin/arbace-go --fixpoint`). A record that has to cite both can prefix them `GR`.

Everything was measured on main at `1000b3c2`, amd64, with TamaGo's go1.27.1
(`/root/tamago-go`) and the stage 0 of the main checkout (`target/seed/amd64/arbace`, seed
`56fabe20…`). The experiments' scratch is in `.tmp/go-forms-repl-survey/` and is not tracked. The
programs written for the experiments are described in Appendix A. yaegi v0.16.1 and x/tools
`v0.44.1-0.20260420230617-19499e7caabc` were copied from the host's module cache into the scratch
directory and built there. Nothing was fetched into the repo.

Names follow doc/NAMING-SURVEY.md ("The rename as built"). repr/go and repr/java are bridges. j2g
translates Java forms into Go forms, and build/go builds them. c2a will become `repr/clojure`, and
`arbace.aot` will become `c2j`. The text says "Arbace forms" for Clojure code in `.ae` files,
"Java forms" for the class forms (doc/classes/SPEC.md) and "Go forms" for doc/go/SPEC.md's.

## Executive summary

- **What runs at the REPL today.**
  - Arbace forms are analyzed by `Compiler` and run by the closure compiler (`Code` nodes) over
    the compiled program.
  - Java forms are analyzed by `arbace.classes` and interpreted by `Compiler$CF`. The classes they
    define are made at run time (`Dyn`).
  - Go forms do not run at the REPL at all. Their declarations name the namespace `arbace.go`,
    which does not exist, so `(require '[arbace.go :as go])` fails at the stage 0 REPL ("Could not
    locate arbace/go…", §1.1). They are data: repr/go reads and prints them, build/go builds them
    (`bin/build-go`), and in the box the tool guests compile them (`tamago/cmd/`).
- **The target (§1.2).** Go forms typed or loaded at the REPL would be interpreted, at the level of
  Arbace and Java forms:
  - their declarations defined and redefined in their package's namespace;
  - calls across dialects in both directions, and into the program's own compiled Go packages;
  - Go's panics and stack traces with the forms' lines;
  - Go values printed by the REPL.

  In builds they would compile as now: repr/go print, then gc. This is the pair the Java forms
  already have: interpreted at the REPL, translated by j2g in builds.
- **What it takes (§2).**
  - **A type checker in Arbace.** Converted forms carry go/types' non-local decisions (`:val`,
    `:inst`, `:go/via`, `^:assign`, `:tag`; over `tamago/goroot`'s 583,072 lines: 44,435
    `:tag`, 13,413 `:val`, 4,989 `:go/via`, 1,065 `:inst`). Every other type is derived "forward
    from declared signatures" (SPEC §11.2), and forms typed by hand carry none of these. So the
    checker has to compute what the converter copies: types, constant values, inference, embedded
    paths. Go's own checker is 24,879 lines (`go/types`, non-test) and yaegi's is about 7,000.
    Arbace's checker for Java forms (`arbace.classes.analyze`) is 4,375 lines of Arbace. **The
    estimate is 5,000-8,000 lines of Arbace, generics included.**
  - **Values.** The interpreter should hold scalars in typed slots and composites as real Go
    values made with `reflect`.
  - **Execution.** Real goroutines and channels.
  - **Compiled packages.** A **table of the program's compiled Go packages** generated at build
    time: `reflect.Value`s plus the declarations taken from the forms.
- **Prior art (§3).**
  - **yaegi** runs a closure tree over `reflect.Value`s with its own type checker, and exposes
    compiled packages through tables that `yaegi extract` generates. It has had no release since
    v0.16.1 (2024-04), and its tables stop at Go 1.22.
  - **ixgo** (formerly igop) interprets `go/ssa` built from `go/types`. It is current (v1.2.3,
    2026-10-13, Go 1.25-1.27), but it makes named types with methods only through `reflectx`'s
    stub tables or a patched Go runtime.
  - **gomacro** emulates named types.
  - **`go/ssa/interp`** is a test adjunct ("never … production-quality"). On go1.27.1 it no longer
    runs the real standard library: a program importing `runtime` panics during initialization,
    and it runs only against x/tools' fake one.
  - **Go's `plugin`** needs cgo and dynamic linking, so a static Arbace cannot `dlopen`.
  - **The reason all of these are complicated** is the same: stock `reflect` can make struct,
    func, slice, map and chan types, but no named types, no methods and no interfaces (golang/go
    #16522, #4146, #15924).
- **Measured (§5.6; best of 5, ms).** The four benchmarks are `fib 22`, `sumSq 1e6` (an `int32`
  loop with an `int64` sum), `sieve 1e6` and `objs 1e5`:

  | run by | fib 22 | sumSq 1e6 | sieve 1e6 | objs 1e5 |
  |---|---:|---:|---:|---:|
  | gc | 0.091-0.15 | 0.33 | 2.6-2.9 | 0.06-0.13 |
  | typed Go closures, hand-built (option (a), Appendix A) | 5.5 | 22 | 55 | |
  | Arbace's Java forms interpreter (`test/classforms/bench.ae`) | 6.8-7.0 | 95-127 | 415-488 | 47-63 |
  | Arbace's evaluator (a Clojure fn) | 8.4-8.6 | 56-58 | | |
  | yaegi v0.16.1 | 38.8 | 191-211 | 199-204 | 258-284 |
  | `go/ssa/interp` (fake std) | 75 | 845 | 1,466 | 509 |

  - yaegi's interpreter adds 5.8 MB to a static binary, and its standard library table adds
    20.9 MB on top of that.
  - Go forms written by hand at the stage 0, printed and built by `bin/build-go`, take 1.7 s.
    Printing alone takes 1.5 s, and yaegi then runs the printed Go (option (c)'s path) in 60 ms.
  - Relinking the whole Arbace program takes about 5 s (STANDALONE-NOTES.md).
- **Options (§5).**
  - (a) Arbace's own Go forms interpreter. The front end is in Arbace, as `arbace.classes` is.
    The back end is typed Go closures written as hand-written Go forms in the program, as `jrt` is.
  - (b) A `go/ssa`-like IR built from the forms, then interpreted.
  - (c) yaegi embedded (as Go forms, converted like `box/`), fed the forms that repr/go prints.
  - (d) The compiled path only: print, build a separate program and run it as a child process or
    a tool guest; or rebuild and restart the program; or an in-process loader of gc's objects
    (goloader-like).
- **Recommendation (§6).**
  - **Go forms first-class.** Option (a), built in stages. First the table of compiled packages,
    with Arbace calling Go (GR1). Then the checker, then the interpreter's core, then concurrency
    and interfaces, then the crossings and the REPL's printing, then generics.
  - **The equality check.** One check per program keeps the interpreter equal to gc: each Go
    program interpreted and also printed and compiled. Its corpus is `$GOROOT/test`'s programs,
    which repr/go already converts (1,713 single-file programs on linux/amd64, 1,037 of them
    `// run`, and 127 runnable directory tests). It extends Futamura's F2 to Go forms.
  - **Not staging.** The Futamura survey's staging (F7) does not apply as for Clojure. Arbace does
    not own Go's compiler, so gc is the definition and the check is the method. Staging could
    later share node definitions between the Java forms' interpreter and Go's.
  - **Excluded:** yaegi as the interpreter (stale, its own checker, a ceiling on speed); an IR first
    (more machinery for no gain at the REPL); the compiled path as the REPL's main path (no shared
    heap, seconds per form). (d1) is kept as a tool (`go/run-compiled`).
  - **Effort.** By the record of similar work (the Java forms at the REPL took two days on top of
    an existing analysis; repr/go G0-G2 took two days), about 22-33 working days to GR6.

## 1. What "first-class at the REPL" means

### 1.1 The three dialects today

| | Arbace forms | Java forms | Go forms |
|---|---|---|---|
| reader | Arbace's | Arbace's | Arbace's (SPEC principle 5) |
| unit | a namespace, top-level forms | a class (`defclass`), code forms in fns (classes/SPEC §9.5) | a package = a namespace `go.<path>` (SPEC §4.1): `go/package`, `go/file`, declarations |
| front end at the REPL | `Compiler.analyze` (`Expr` trees) | `arbace.classes` (parse, lower, analyze, env, types), loaded on first use (CF2) | none |
| run at the REPL | the closure compiler (`CodeCompiler`, `Code` nodes, `Frame`, `EvalFn`; `arbace/lang/Compiler.ae` from line 10198) | `Compiler$CF` (~70 node kinds, typed `eval`/`evalJ`/`evalD`) built by `arbace.classes.interp`; classes made at run time (`Compiler$CFGo`, `Dyn`, J2G-SPEC §5.12, §16) | none: `(require '[arbace.go :as go])` fails, "Could not locate arbace/go__init.class, arbace/go.ae…" |
| compiled path (builds) | `arbace.aot` (to become c2j) → Java forms → j2g → Go → gc | j2g → Go → gc | repr/go print → gc (`bin/build-go`; in the box `tamago/cmd/`'s tool guests) |
| define again | `def` again, vars | a new generation of the class (classes/SPEC §10) | — |
| call the others | Java: interop (`.`, `new`, reflection); Go: none | Clojure: vars, fns as `IFn`, Clojure in class bodies (§7.14) | jrt's hand-written Go forms call translated Java (in builds only) |
| errors | Java exceptions; evaluated frames in traces (`jrt.EvalTrace`) | Java exceptions; no interpreted frames in traces (CLASSFORMS-REPL §7) | — |
| values at the REPL | Arbace's | Java objects (`Dyn` for interpreted classes) | — |

Two facts shape everything below.

- **The compiled program is itself Go.** j2g's output, `jrt`, Go's runtime and std run in one
  address space. Its 100 packages are `aot_*` (the namespaces), `lang` (205,890 lines of printed
  Go, 5,526 exported functions, 13,867 exported methods), `jrt` (1,079,576 lines, 15,407 and
  47,445) and `x509roots`, and they import 46 std packages directly (counted in the seed's
  tar, §5.6). So a Go forms interpreter in the program can call the program's own Go functions
  with no foreign function boundary. The only boundary is Go's static typing at run time
  (`reflect`).
- **Arbace values are Go values.** `java.lang.Object` is Go's `any`, a class instance is a pointer
  to its struct, `String` is `*jrt.String` (UTF-16 units) and `Long` is a struct (J2G-SPEC §5.1).
  An Arbace value is therefore already a legal Go `any`, and Go code can hold it, store it and pass
  it back unchanged. What does need converting is Go's scalars and strings in the one direction
  and Java's boxes and strings in the other (§2.2).

### 1.2 The target

The user's sentence asks for two things. Go forms "interpreted in the repl" is the mechanism. The
three dialects "at the same level … when it is appropriate" is the goal, and it comes with a
limit. Concretely:

1. **Define and redefine.**
   - `(load "go/bench")` of a forms tree, or a `go/func` typed at the REPL in a `go.*`
     namespace, defines the declarations. Defining one again replaces it, and later calls see the
     new definition.
   - A type defined again starts a new generation, as Java forms' classes do. Old values keep the
     old type.
2. **Calls across, both ways.**
   - Arbace calls a Go func as `(go.bench/fib 22)`.
   - Go calls an Arbace fn as a Go func value or through `lang.IFn`.
   - Java forms and Go forms call each other.
   - All three call the program's compiled Go packages.
3. **Errors.** A Go panic inside Go code is a panic: `recover` sees it, and gc's runtime errors
   have gc's messages. When it crosses into Arbace it becomes an exception the REPL reports, with
   Go frames (file and line from the forms' `:line`) in the trace.
4. **Printing.** Go values returned to the REPL print readably. Scalars and strings become Arbace
   numbers and strings; composites are printed as Go's `%#v` prints them, tagged.
5. **The compiled path stays.** Forms that ran at the REPL build unchanged with repr/go print and
   gc. The interpreter is checked against that path (§4.3).

### 1.3 The level, dialect by dialect (target)

| | Arbace forms | Java forms | Go forms (proposed) |
|---|---|---|---|
| typed at the REPL | yes | yes (`defclass`, code forms in fns) | declarations in a `go.*` namespace; `go/eval` for statements (G11) |
| loaded from files | `load`, `require` | `load` (`.ae` with `defclass`) | `load`, `require` of `go/<path>.ae` (the forms' layout, SPEC §4.1) |
| interpreted by | closure compiler | `Compiler$CF` | a Go forms interpreter (§2, option (a)) |
| redefinition | vars | class generations | per declaration; types by generation (§2.6) |
| inside another dialect's code | — | code forms inside Clojure fns | no: Go context is closed (SPEC principle 1: "No Clojure code runs inside Go context") |
| calls into the compiled program | Java interop (member tables) | the same | the package table (§2.5) |
| compiled in builds | c2j → j2g → gc | j2g → gc | repr/go print → gc |
| the check of the pair | F2 (evaluated against compiled, per form) | F2 (interpreted against j2g) | interpreted against printed and compiled, per program (§4.3) |

"When it is appropriate" is read as follows. Go forms are not mixed into Clojure code the way Java
code forms are, because Go context is closed by principle 1 and Go's scoping is per block, not per
expression. Haskell forms stay data (the user). Go's static binding of functions becomes late
binding at the REPL, as it does in yaegi and as vars are in Clojure: it is the REPL's meaning, and
builds keep Go's.

## 2. The design of a Go forms interpreter

### 2.1 The front end: what the forms carry, what an interpreter needs

SPEC principle 3: "Whatever a consumer could only learn by running a Go type checker is in the
forms". Principle 4: "What follows from declarations is not written". So a converted package
needs no type inference. It still needs **type derivation** (SPEC §11.2) for everything that is
not written:

- the types of all other expressions, forward from the declared signatures;
- the implicit `&` and `*` of receivers;
- implicit conversions to interfaces;
- the variables of each type-switch clause;
- `range` kinds, struct layout and method sets.

The interpreter must know the static type of every expression in order to choose its node: an
`int8` `+` wraps at 8 bits, an `==` on interfaces compares dynamic types. So a typed pass over the
forms is needed in any case.

Forms typed by hand at the REPL carry none of the converter's annotations. A user writes
`(let [x (f)] …)` and no `:tag`. `(set! x + 1)` gets no `:val`. A generic call gets no `:inst`, a
promoted field no `:go/via`, and a redeclaring `:=` no `^:assign`. In a hand-written `go/const`,
`:val` is missing too. How much the converter writes, over `tamago/goroot` (2,152 files,
583,072 lines of forms; grep):

| annotation | occurrences | what computing it takes |
|---|---:|---|
| `:tag` on expressions | 44,435 | untyped constants' default types, where the local rules do not give them (§8.3) |
| `:val` | 13,413 | constant evaluation with Go's exact arithmetic (go/constant: big integers, rationals, `iota`) |
| `:go/via` | 4,989 | embedded field search by depth (Go spec "Selectors") |
| `:inst` | 1,065 | type argument inference (unification, core types, untyped arguments) |
| `^:assign` | 765 | scope lookup |
| `(conv …)` | 22,640 | conversions told from calls: name resolution |

So an interpreter of hand-typed forms needs **a Go type checker in Arbace**, and it must agree
with go/types wherever the converter would have written something. Converted forms are then checked
the same way, and the converter's annotations are verified rather than trusted, which costs little
and doubles as a test of the checker (§4.3). Sizes for scale:

| checker | language | lines (non-test) |
|---|---|---:|
| `go/types` (go1.27.1) | Go | 24,879 |
| `cmd/compile/internal/types2` | Go | 23,145 |
| yaegi: `typecheck.go`, `cfg.go`, `type.go`, `gta.go` | Go | about 7,000 |
| `arbace.classes.analyze` (Java forms, with javac's derivations) | Arbace | 4,375 |
| proposed: `arbace.go.check` | Arbace | 5,000-8,000 (estimate) |

The estimate puts the core (scopes, objects, types, assignability, conversions, constant
arithmetic, method sets, embedding, interfaces, switch and select rules) at about 4,000-5,500
lines. Generics (type sets, unification, inference, instantiation) add about 1,500-2,500. Go's
spec is smaller than Java's: no overloading, no inheritance, no checked exceptions, no inner
classes. Its checker is mostly constants, untyped values and generics. **Error messages** should be
go/types' wording where practical. The check (§4.3) can compare the errors on `$GOROOT/test`'s
`errorcheck` programs later.

**Declarations of imported packages.**
- Today: the checker needs the exported declarations of every package the forms import. They are
  already Go forms at build time: std as `tamago/goroot`'s forms (for tamago; for linux,
  `bin/repr-go read` of the program's configuration), jrt's hand-written forms in `go/`, and j2g's
  output.
- Proposed: the build writes a **declarations digest** (exported types, signatures, constants with
  `:val`, method sets) for every package in the table (§2.5), embedded and compressed as the
  program's resources are (zlib, SA21).
- Rejected: reading gc's export data, which would need a Go-side importer and would tie the
  digest to gc's format.

**Where it runs.** The checker and the builder run in Arbace, compiled by c2j into the program as
`arbace.classes` is. The interpreter's own nodes run in Go (§2.7). This is the Java forms' split:
analysis in Arbace, execution in nodes translated to Go.

### 2.2 Values

| Go | in the interpreter (proposed) | to Arbace | from Arbace (for a Go parameter of that type) |
|---|---|---|---|
| `bool` | `int64` slot (0/1) | `Boolean` | `Boolean` |
| `int8` … `int64`, `uint8` … `uint32`, `int`, `uintptr` | `int64` slot, kept normalized to the width after every operation (as CF keeps `int` in `long`) | `Long` | `Long`, `Integer`, … range-checked (an error, not a wrap) |
| `uint64`, `uint`, `uintptr` above 2^63 | `int64` slot (bits) | `Long` when it fits, else `BigInt` | `BigInt` or `Long` ≥ 0 |
| `float32`, `float64` | `float64` slot, `float32` rounded after each operation | `Float`, `Double` | `Double`, `Float`, `Long` (exact only) |
| `complex64`, `complex128` | reference slot (`complex128`) | a Go value (below) | a Go value |
| `string` | reference slot holding a Go `string` | `String` (UTF-8 → UTF-16, copied) | `String` (UTF-16 → UTF-8; unpaired surrogates as U+FFFD, as Go's conversion of invalid UTF-16) |
| pointers, slices, arrays, maps, chans, funcs | reference slot holding the **real Go value** (`reflect`'s `SliceOf`, `MapOf`, `ChanOf`, `ArrayOf`, `PointerTo`, `FuncOf` + `MakeFunc`) | the value itself, as an `any` (a "Go value") | the same value; an Arbace fn for a func type (§4.2) |
| structs of the interpreted package | a real struct made with `reflect.StructOf`, plus the interpreter's type descriptor (name, methods, generation) | a Go value | the same |
| named types with methods, interfaces of interpreted packages | the interpreter's descriptor beside the value (below) | a Go value | the same |
| interfaces | a pair (dynamic type descriptor, value) inside the interpreter; an `any` outside | the value itself | the value, checked against the method set |
| `nil` | `nil` | `nil` | `nil` |

**Typed slots, not `reflect.Value` everywhere.** yaegi holds every value as a `reflect.Value`,
and that is a large part of its 38.8 ms on `fib`. Arbace's Java forms interpreter keeps
primitives in `long` and `double` slots and is 5.7 times faster on the same work (6.8 ms). The
closure sketch with typed slots takes 5.5 ms (§5.6). Composites must still be real Go values,
because compiled code receives them: a `[]byte` passed to `strings.Builder.Write` has to be a
`[]byte`.

**Named types and methods: the hard part.** `reflect` cannot make a named type, give a type
methods, or make an interface type (golang/go#16522 "reflect: NamedOf", #4146 "reflect:
MakeInterface"). `StructOf` does not promote the methods of embedded fields (#15924, #38783). The
consequences for the interpreter:

1. Within interpreted code there is no problem. The interpreter keeps its own descriptor per named
   type, and an interface value inside the interpreter is a (descriptor, value) pair. Method calls,
   type switches and assertions use the descriptors, as gomacro and yaegi do.
2. An interpreted value given to compiled code as an interface (an `io.Reader` written in Go forms
   passed to `bufio.NewReader`) needs a compiled type with those methods.
   - yaegi's answer: a **wrapper struct per compiled interface**, generated with the tables
     (`_io_Reader{IValue; WRead func(…)}`).
   - Arbace already answers this once for Java: `Dyn` is one Go type with a method for every
     interface method of the closed world, dispatching by slot (J2G-SPEC §5.12). That works
     because j2g's mangled names (`Invoke_O__O`) never clash. Go's method names do clash across
     interfaces (`Read(p []byte)` and `Read() rune`), so one universal type is not possible. The
     closest is **one generated wrapper type per group of non-clashing method signatures**,
     with yaegi's per-interface wrappers as the simple first version.
   - Either way the wrappers are generated at build time, for the interfaces of the packages in the
     table.
3. `%T`, `reflect.TypeOf` and `fmt`'s `%v` of an interpreted named type show the reflect-made
   unnamed struct (`struct { X int; Y int }`) and not `main.P`. yaegi's README lists the same
   difference. For gc's behaviour, the interpreter's own `fmt` paths could print through the
   descriptor. The general remedy belongs to the runtime: Arbace already patches Go's runtime
   (`overlay/go/`, the goroutine-local slot, J2G-SPEC §9.3), and ixgo's `reflectx` shows that
   named types with method sets are possible with a runtime patch (`iface_patch`) or pre-allocated
   method stubs (`icall`). That is a later, separate decision (G8).

**Arbace values inside Go code.** They need no conversion. An `any` parameter receives a
`*lang.PersistentVector` as it is, and Go code can type-switch on it or call it through the
program's Go interfaces (`lang.IFn`'s `Invoke_O__O`, §4.2). **Go values inside Arbace** are a
new case for jrt. A Go `[]int` held in an Arbace var is an `any` that is not a Java object, and
jrt's `Object` protocol (`getClass`, `toString`, `hashCode`, `equals`) must not fail on it. The
proposal is a jrt rule: a value of no Java class has the class `arbace.lang.GoValue`, its
`toString` is `%#v`, its `equals` is Go's `==` where the type is comparable and identity
otherwise, and its `hashCode` is consistent with that (G10).

### 2.3 Goroutines, channels, select, defer, panic

- **Goroutines are goroutines.** jrt's threads already are (J2G-SPEC §8.4: "a goroutine jrt did
  not start (Go's own) gets a `Thread` created on first use"), so an interpreted `(go (f x))` runs
  `f` on a new goroutine, and Arbace code it calls sees a thread with Arbace's state. Binding
  conveyance (`bound-fn`) is not implied, as with Go code in builds.
- **Channels are real channels of the element's Go type** (`reflect.MakeChan(ChanOf(…))`), so they
  can cross to compiled code. Send, receive and close go through `reflect.Value`. Typed fast paths
  for `chan int`, `chan any` and `chan struct{}` avoid reflect for the common cases. `select`
  uses `reflect.Select`, with direct `select` statements for one or two cases of the fast-path
  types (yaegi does this through `reflect.Select` only).
- **`defer`, `panic`, `recover`** use the frame's defer list and Go's own `panic`. `recover`
  returns the value only in a deferred call made by the interpreter's frame-exit path, which
  follows Go's rule ("called directly by a deferred function"). Runtime errors (index, nil
  dereference, division by zero, a failed type assertion, closing a closed channel, a negative
  `make` length) are raised as `runtime.Error` values with gc's messages
  (`runtime error: index out of range [5] with length 3`). Where the interpreter uses the real Go
  operation, Go raises them itself. Where it emulates one, the message has to be gc's.
  §4.3's check compares them.
- **Panics across dialects.** Inside Go code a Java exception is already a Go panic whose value is
  the `Throwable`: j2g's translated code throws that way (J2G-SPEC §7.9). So `recover` in Go forms
  sees an Arbace exception as its `Throwable`, which is correct. A Go panic that leaves Go code into
  an Arbace frame becomes an exception: the `Throwable` itself when the value is one, otherwise an
  `arbace.lang.GoPanic` (an `ExceptionInfo` with `:value` and, for a `runtime.Error`, the Java
  exception jrt already maps it to: `ArithmeticException`, `NullPointerException`,
  `ArrayIndexOutOfBoundsException`, J2G-SPEC §7.9.5). The trace gets the interpreted Go frames
  through the evaluator's hook (`jrt.EvalTrace`, J2G-SPEC §10.7). Each Go frame records its
  function and line (`:line` metadata, SPEC §10.1), as `Compiler$Frame` does.
- **Deadlocks.** `jrt.RunMain` keeps a ticker so that Go's runtime never declares "all goroutines
  are asleep" (JC9). An interpreted program that blocks forever at the REPL therefore blocks the
  REPL, as an Arbace `@(promise)` does today. An interrupt (`Ctrl-C` to the REPL's thread) is the
  remedy. Go's own deadlock detection is not reproduced.

### 2.4 Generics

- **The checker** carries most of the cost: type sets, core types, unification and inference,
  about 1,500-2,500 lines (§2.1).
- **Execution.** The proposal is to **instantiate per type-argument list when the node tree is
  built**, lazily and cached, the way `go/ssa`'s `InstantiateGenerics` and gc's stenciling
  produce one body per shape. The rejected alternative is to run one generic body with type
  dictionaries. The interpreter's nodes are typed (an `int8` `+` is not an `int64` `+`), so a
  body per instance is what typed nodes require, and the builder already runs once per function.
  Code size at the REPL is no concern.
- **Generic types from compiled packages** (`slices`, `maps`, `sync/atomic.Pointer[T]`,
  `iter.Seq`) cannot be called through a reflection table: a generic function has no
  `reflect.Value` until it is instantiated, and the table can only hold instances compiled in. Two
  ways remain:
  - interpret those packages from their forms (most of `slices`, `maps`, `cmp`, `iter` is plain
    Go without assembly);
  - list the instances the program needs in the table.

  The first is the general answer and matches what yaegi cannot do (generics of imported
  packages: gomacro's documented limit too).

### 2.5 Packages: interpreted against compiled, and the table

A package at the REPL is one of three kinds:

| kind | example | how it runs |
|---|---|---|
| interpreted | a `go.*` namespace typed or loaded at the REPL | the interpreter |
| compiled, in the table | `strings`, `strconv`, `math`, `bytes`, `sort`, `unicode/utf8`, `time`, `sync`, `io`, `os`, `fmt`, jrt's Go API (G6) | calls through the table |
| compiled, not in the table | `runtime` internals, `internal/*`, packages with no exported API worth calling | not reachable (an error naming the package) |
| interpretable from forms | generic std packages without assembly (`slices`, `maps`, `cmp`, `iter`) | the interpreter, from the forms of the build's configuration (§2.4) |

**The table** is generated by the build, as j2g's member tables are for Java. For every listed
package it holds:

- a Go map from member name to a `reflect.Value` (functions, pointers to variables);
- the `reflect.Type`s of its named types, its constants (also in the digest);
- the wrapper types of its interfaces (§2.2);
- the declarations digest (§2.1).

It is written as Go forms (`go/arbace/gotable/`, generated, like `reflect_tables_j2g.go`) and
printed with the program. yaegi's generator (`yaegi extract`) is the model.

**Its costs.**
- **Size.** A table keeps alive every function it names, which defeats the linker's dead code
  elimination. In the experiment, yaegi's interpreter alone made an 8.2 MB static binary against
  2.4 MB for the plain benchmark (+5.8 MB), and its tables of the whole std (Go 1.22's API, 206,206
  generated lines) made it 29.1 MB (+20.9 MB).
  For Arbace the cost is smaller where the program already links a package's API and larger for
  std packages it links only in part. The table should start with a list (as j2g's world has
  `class-supers`) and be measured: the 46 std packages the program imports directly, with their
  exported API, and not all of std.
- **Crossing.** A `reflect.Value.Call` boxes its arguments into a `[]reflect.Value` and allocates.
  It costs on the order of 100-300 ns, against a few ns for a direct call. For the common
  signatures (`func(string) string`, `func(int) int`, `func([]byte) (int, error)`) the generator
  writes **typed invokers**, as jrt's member tables have direct invokers, and reflection stays as
  the fallback.
- **Configuration.** The table is per program and per configuration (linux, tamago), like the
  printed program.

**Arbace calling compiled Go without any interpreter** is a by-product: `(go.strings/ToUpper
"abc")` from Arbace through the table, converting by §2.2's rules. It is useful on its own (Go's
std as a library for Arbace code) and it is the first milestone (GR1), because it builds the
table, the conversions and the `GoValue` rule that everything else needs.

### 2.6 Redefinition, namespaces, the REPL

- **A Go package is a namespace** (SPEC §4.1). Each package-level declaration becomes a var in it.
  A `go/func`'s var holds an `IFn` adapter, so Arbace calls it like a fn and `doc`, `meta` and
  `ns-publics` work. A `go/var` holds a reference to the Go variable, and `@` or `deref` reads it.
  A `go/type`'s var holds the type descriptor. A `go/const`'s var holds its value.
- **Calls between interpreted functions go through the callee's cell** (one indirection, as
  the closure sketch's `*fn`), so a redefinition is seen. A redefinition with a different signature
  is checked against its callers. The proposal is to re-check the dependent declarations of the
  package and report the ones that no longer type-check, leaving them bound to the old definition
  until they are defined again. The Java forms' generations have the same rule ("linked to the
  newest class of a name", CLASSFORMS-REPL §2.3).
- **Types defined again** are a new generation: a new descriptor, with old values keeping the old
  one, as `defclass` does (classes/SPEC §10). Functions using the type are re-checked as above.
- **`go/package` and `go/file`** set up the namespace, its imports and its per-file language
  version (`:lang`). At the REPL a namespace created with `(ns go.x (:require [arbace.go :as
  go]))` and `(go/package x :path "x")` is enough. `go/file` is optional at the REPL (one implicit
  file).
- **Initialization.** Package `var` initializers run in `:init-order` (written by the converter,
  computed by the checker for hand-typed forms), `init` functions after them. At the REPL a newly
  defined `go/var` is initialized when it is defined.
- **`main`.** `(go.bench/main)` runs a main package's `main` and returns. `os.Exit` inside
  interpreted code must not end the REPL's process. The interpreter maps it to an exception
  carrying the status (yaegi and ssa/interp do the same, by panic).

### 2.7 The closure technique, and where the interpreter lives

The evaluator's closure compiler (`CodeCompiler`) and the Java forms' `CF` nodes are trees of
objects with a `run`/`eval` method, Java forms translated by j2g. CLASSFORMS-REPL §3 records why
CF did not become Go closures: "closures cannot be written in Java forms". A Go forms interpreter
does not have that limit if its back end is **written as hand-written Go forms**, as `jrt` is
(`go/arbace/jrt/*.ae`, printed with the program):

- nodes are typed Go closures, `func(*frame) int64`, `func(*frame) float64`,
  `func(*frame) any`, `func(*frame) ctl` for statements;
- frames are an `[]int64` and an `[]any`;
- jumps are a returned code, as CF's pending jump is;
- calls go through cells.

The builder (Arbace, compiled by c2j) walks the checked tree and calls the back end's node
factories through natives, as `arbace.classes.interp` builds CF nodes. Appendix A's sketch shows
the speed: 5.5 ms on `fib 22` against CF's 6.8 ms and yaegi's 38.8 ms, and 22 ms on `sumSq`
against CF's 95-127 ms. The gap to gc stays large (60x on `fib`). It is the price of an
interpreter, and Futamura's F10 (run-time specialization) is where it would be closed, much later.

The alternative is a back end in Java forms like CF, translated by j2g. It is rejected: Java forms
have no unsigned arithmetic, no Go channels, structs or pointers. Every Go operation would be a
native call to Go forms anyway, and nodes could not be closures.

### 2.8 Out of scope

- `unsafe` (beyond `unsafe.Sizeof`/`Alignof`/`Offsetof` as constants, which the checker computes
  from `types.SizesFor("gc", GOARCH)`'s rules), assembly, cgo, `go:linkname`, `go:embed`, and most
  directives at the REPL. These stay the compiled path's. A package that needs them is used
  compiled, through the table.
- Exact allocation and escape behaviour, `runtime.Callers` and `runtime.Caller` with gc's frames,
  finalizer timing, and `reflect` details of interpreted named types (§2.2 item 3).

## 3. Prior art

| | approach | values | compiled packages | interpreted types seen by compiled code | generics | status |
|---|---|---|---|---|---|---|
| **yaegi** (github.com/traefik/yaegi) | `go/parser` → its own node tree; `gta` (global declarations, bodies skipped) and `cfg` (control flow, its own type checker, `typecheck.go`), not go/types; nodes run as a threaded chain of closures, `type bltn func(f *frame) bltn` (`interp/run.go:15`) | `reflect.Value`; types `itype` with an `rtype reflect.Type`; structs and funcs via `reflect.StructOf`/`FuncOf` | `Exports map[string]map[string]reflect.Value`, `Use`, generated by `yaegi extract` (`stdlib/`: 206,206 lines) | pre-generated wrapper structs with func fields (`_io_Reader{IValue; WRead}`); README: "Interfaces to be used from the pre-compiled code can not be added dynamically" | partial (`generic.go`; generics issues open) | Apache-2.0; interp/ 21,534 lines; v0.16.1 (2024-04-03), no release since; README: Go 1.21 and 1.22; 190 open issues and PRs (58 bugs) |
| **gomacro** (github.com/cosmos72/gomacro) | `classic` (tree walking) and `fast` (a faster reimplementation); macros as Go functions over `go/ast` | reflect | on Linux/macOS/BSD a Go **plugin** built from the imported package's exports; elsewhere generated files and a rebuild | named types emulated: "it actually creates the unnamed type `struct { A, B int }`" | experimental (C++-style, contracts-are-interfaces) | MPL-2.0; last release v2.7 (2018), commits until 2026-09 |
| **ixgo**, formerly igop and gossa (github.com/goplus/ixgo) | `go/types` → `golang.org/x/tools/go/ssa` → SSA interpreted | reflect, with `github.com/goplus/reflectx` | `pkg/` export tables (67 std directories, `ixgo export`) | `reflectx`: `NamedTypeOf`, `SetMethodSet`, `InterfaceOf`; needs pre-allocated method stubs (`icall`, "each installed method needs a unique ifn stub") or a **patched Go runtime** (`iface_patch`, tag `goplus.ifacefuncval`) | "all Go language features, including generics and generic methods" | Apache-2.0; v1.2.3 (2026-10-13); Go 1.25-1.27; REPL `ixgo repl` |
| **go/ssa/interp** (golang.org/x/tools/go/ssa/interp) | SSA interpreted, frames as `map[ssa.Value]value` | boxed `any`, `iface{t, v}` | none: everything interpreted from source, 39 std functions native (`external.go`) | n/a | with `InstantiateGenerics` | "an adjunct for testing the SSA construction algorithm … not, and will never be, a production-quality Go interpreter"; unsafe impossible, reflect partial, "Mutexes are currently broken"; 3,803 lines. **On go1.27.1 the real std does not run** (a nil dereference when the runtime initializes, §5.6); its tests use a fake std (`testdata/src`) |
| go-interpreter/ssainterp | a fork of the above | | | | | archived (2017) |
| Go's `plugin` | `dlopen` of a `-buildmode=plugin` `.so` | real | real | real | real | `//go:build (linux && cgo) \|\| (darwin && cgo) \|\| (freebsd && cgo)`: needs cgo and dynamic linking; same toolchain, flags and dependencies required. Unusable for a static, cgo-free Arbace (or the box) |
| goloader (github.com/pkujhd/goloader) | loads gc's object files into a running process, relocating against its symbols and registering module data with the runtime | real | real | real | | tied to runtime internals per Go version; the in-process route of option (d3) |
| **Arbace's Java forms** (doc/go/CLASSFORMS-REPL.md) | the compiler's own analysis (`arbace.classes.analyze`), its typed trees turned into CF nodes (`arbace.classes.interp`), classes made at run time (`Dyn`, `CFGo`) | typed: `long`, `double`, objects; frames `Object[]` + `long[]` | the world's member tables (reflection) | `Dyn`: one Go type with every interface method of the world, by slot | erased (Java) | built 2026-10-09/10; 212 of 213 oracle cases, 131 of 131 corpus; 14-40x faster than the evaluator was then |

The lessons for Arbace:

1. **Every production Go interpreter calls compiled packages through generated tables of
   `reflect.Value`s.** No other mechanism exists in a static Go binary. Arbace has the advantage
   of building its own program: the table is one more generated file, and the declarations come
   from the forms.
2. **Every one of them hits `reflect`'s missing named types and methods**, and each solves it
   differently: yaegi with pre-generated wrappers, gomacro with emulation, ixgo with stub tables or
   a runtime patch. Arbace's `Dyn` is the same problem solved for Java with generated code.
   Arbace also patches the runtime already, which makes ixgo's route possible later.
3. **The type checker is where they differ.**
   - yaegi's own checker is the source of much of its issue list.
   - ixgo uses `go/types`.
   - The `go/types` route is closed to Arbace: it is a Go library, and using it at the REPL would
     mean the interpreter's front end is Go and not Arbace, with no forms on the way in. The forms
     would have to be printed first, and that is option (c).
   - An Arbace checker can be checked against go/types: the converter's annotations are go/types'
     answers, and `$GOROOT/test`'s programs are the corpus (§4.3).
4. **Speed comes from typed slots, not from the IR.** On the same machine, yaegi's closures over
   `reflect.Value` are 5-7 times slower than Arbace's typed CF nodes, and `ssa/interp` is 2-10
   times slower again (§5.6).

## 4. The three dialects at the same level

### 4.1 Shared machinery

| | shared already | to add for Go forms |
|---|---|---|
| reading | Arbace's reader, all three | none (SPEC principle 5) |
| units and names | namespaces; Java forms' classes registered by name (`Class.forName`) | Go packages as namespaces `go.<path>` (SPEC §4.1), declarations as vars (§2.6) |
| loading | `load`, `require`, `ARBACE_PATH` | `arbace.go`: `go/package`, `go/file`, `go/func`, … as macros expanding to the interpreter's `define!`; loaded on the first Go form, as `Compiler.classForms` loads `arbace.classes` |
| the REPL's printing | `print-method` | `GoValue`'s (§2.2) |
| errors | `arbace.main`'s triage, `jrt.EvalTrace` | Go frames in the hook; `GoPanic` (§2.3) |
| builds | j2g's program, `bin/build-go` | unchanged: the forms print and build as today |

### 4.2 Calls across

| from \ to | Arbace fn | Java forms class | Go forms func | compiled Go |
|---|---|---|---|---|
| **Arbace** | — | interop (`.`, `new`; the class's member tables, `Dyn`) | the var's `IFn` adapter: `(go.bench/fib 22)`; arguments converted by §2.2 from the declared parameter types | the table: `(go.strings/ToUpper "abc")` (GR1) |
| **Java forms** | `IFn` (as today) | — | through the var as an `IFn` (`arbace.lang.Var`'s `invoke`), the Java forms' own path to Clojure (J2G-SPEC §7.14); a static-method-like spelling later | Java forms cannot name Go types: through Arbace (the table's vars) |
| **Go forms** | a Go func value: `reflect.MakeFunc` of the parameter's func type around the fn (each call converts per §2.2); or directly `(.Invoke_O__O f x)` on `lang/IFn`, a compiled Go interface of the program; a small hand-written helper package `arbace` (`arbace.Call(f, args...)`, `arbace.Str`, `arbace.Kw`) instead of the mangled names (G6) | as any Arbace object: `any`, called through the helper's reflection (`arbace.Invoke(obj, "name", args...)`), or through a compiled Java interface it implements (a `Dyn` satisfies them all) | direct (cells) | the table (direct invokers, reflection) |
| **compiled Go** | through a Go func value or `lang.IFn` handed in | through a compiled Java interface (`Dyn`) | through a Go func value (`MakeFunc`-made, or the interpreter's closure) or a compiled interface (the wrappers, §2.2) | — |

Three cases deserve attention.

- **An Arbace fn as a Go func value** is exact and cheap enough. `reflect.MakeFunc` gives a real
  func of the declared type, so compiled code like `sort.Slice` or `strings.Map` can call Arbace
  fns. Each call converts the arguments and the result.
- **An Arbace object as a Go interface.** Only Go interfaces whose methods are in the wrappers can
  be satisfied: `fmt.Stringer`, `io.Reader` and `error` are the useful ones. An Arbace `reify` of a
  Go interface (`(reify go.io/Reader (Read [_ p] …))`) is an extension of `reify` that this survey
  only notes (G13).
- **Java forms and Go forms.** Both are statically typed, and in builds they already meet: j2g
  translates Java forms into Go, and jrt's Go forms call that Go directly (`lang.RT_...`). At the
  REPL they meet through Arbace's values (vars, `IFn`, `any`), which costs a conversion per call.
  Closer coupling (Go forms naming an interpreted Java class's fields as a Go struct) would need
  the two interpreters to share a type model. That is the long-term unification of §4.3, not a
  first step.

### 4.3 One definition, or one check: Futamura's F2 and F7 for Go forms

The Futamura survey (doc/FUTAMURA-SURVEY.md §4 (f), F7, F8) proposes **staging** for the pairs
Arbace owns: each node kind defined once, generating both the interpreter's node and the
compiled path's template. For Clojure the compiled half is `arbace.aot`/c2j's emit, and for the
Java forms it is j2g's `code.ae`. Both are Arbace's code. **For Go forms the compiled half is gc**,
which Arbace does not write: repr/go print is the identity on meaning. Staging therefore cannot
make the Go interpreter equal to gc by construction, and the definition of Go's semantics stays
gc's. The method is the **check**, F2 extended:

- **Per program, interpreted against printed and compiled.** For each Go forms program: run it in
  the interpreter, print it with repr/go, build it with gc, run it, and compare stdout, stderr's
  panic message and the exit status.
- **The corpus exists.** repr/go already converts `$GOROOT/test`: 1,713 single-file programs on
  linux/amd64 (1,037 of them `// run` in the toolchain's tree, many with golden `.out` files),
  and 127 runnable directory tests (`bin/repr-go roundtrip --dirs`, level `run`). Go's own
  conformance suite, as forms, with gc's behaviour recorded: the oracle for the Go interpreter, as
  the frozen jar is for Arbace.
- **The checker's own check.** The converter's annotations (`:val`, `:inst`, `:go/via`, `:tag`,
  `^:assign`) are go/types' answers. Stripping them from converted forms and having the checker
  recompute them is a check of the checker over all of std and `$GOROOT/test`, with no running at
  all. Later, the `errorcheck` programs compare its errors with gc's.
- **Where it runs.** In `bin/gate --full` first; a subset (a few hundred programs) in the
  essential gate when it takes minutes.

Where staging can apply is **between the two interpreters Arbace does own**. CF (Java forms) and
the Go forms interpreter are both typed-slot interpreters of statically typed imperative code.
Their arithmetic, control flow, frames and jumps are the same machinery. A later step could
define those node kinds once (F7's `defnode`) and generate CF's Java forms classes and the Go
interpreter's closures from one definition. There is a further, more radical unification:
interpret Java forms by translating them with j2g into Go forms at the REPL and running the Go
interpreter on them. It would need j2g's closed-world model at run time, and `Dyn` exists
precisely because classes are open at the REPL. It is noted, and not proposed (G16).

## 5. Options

### (a) Arbace's own Go forms interpreter (front end in Arbace, back end in hand-written Go forms)

§2's design: `arbace.go` (the declaration macros), `arbace.go.check` (the checker),
`arbace.go.build` (the node builder), all compiled by c2j into the program; the back end
`go/arbace/goi/` (typed closures, frames, cells, descriptors, the `GoValue` rule), printed with
jrt; the table `go/arbace/gotable/` generated per program.

- **Buys:** forms in, forms all the way. No printing or reparsing; positions come from the forms'
  metadata. It has the Java forms' architecture, so the same people and checks apply. It has
  typed slots (§5.6: 5-7x yaegi on scalar code). Its checker is Arbace's own, usable beyond the
  REPL: a Go forms linter, j2g's output checked, the box with no Go toolchain needed to type-check
  forms. It follows the Go version Arbace pins (go1.27.1 today), with no outside project to wait
  for.
- **Cost:** the checker (5,000-8,000 lines), the back end (estimated 3,000-4,500 lines of Go forms),
  the builder (1,500-2,000), the table generator (500-800), the crossings and printing (about
  1,000). In time: §6.3, about 22-33 working days.
- **Risks:**
  - **Checker fidelity.** Untyped constants and inference are where hand-written Go checkers
    diverge; the annotation check (§4.3) bounds this.
  - **reflect's named-type gap** (§2.2).
  - **Size.** The table plus the back end might add 10-25 MB unless the table is restricted; to be
    measured at GR1.
- **Staging:** §6.2.

### (b) An SSA-like IR from the forms, interpreted

ixgo's and `ssa/interp`'s route, with the IR made by Arbace from checked forms.

- **Buys:** a uniform, small instruction set to interpret; a basis for optimizations and, later,
  for Arbace's own code generator (STANDALONE-SURVEY b3, Futamura's T4).
- **Cost:** everything in (a)'s front end, plus SSA construction: dominators, phi placement, and
  closure conversion for captured variables (Go captures by reference). `go/ssa` is about 15,000
  lines of Go.
- **Risks:** speed is not better for an interpreter. `ssa/interp`, the plain realization, is the
  slowest here (§5.6), and ixgo's speed comes from its value representation, not from SSA. Error
  positions and stack traces need the mapping back to forms.
- **Verdict:** an IR is a compiler's tool. If Arbace builds its own code generator, the checked
  tree of (a) is what it starts from, and the IR can come then.

### (c) yaegi embedded, fed printed Go

yaegi converted to Go forms (as `box/` vendors go-whim) and linked into the program. Go forms at the
REPL are printed by repr/go (`print` with `:lines`, keeping lines) and given to yaegi's `Eval`.

- **Buys:** working quickly. In the experiment, forms typed by hand were printed by `bin/build-go
  --print-only` in 1.5 s (most of it the tool's start-up) and run by yaegi in 60 ms. Its REPL
  model (incremental `Eval`) exists, and its table generator exists.
- **Cost:** tables regenerated for go1.27 (yaegi's stop at 1.22: its tables were built from go1.22
  symbols even on go1.27.1). Its interfaces with Arbace still to write (values cross as
  `reflect.Value`). 5.8 MB of interpreter plus the tables (§5.6).
- **Risks:** a stale upstream: no release since 2024-04, Go 1.21-1.22 in its README, 58 open bugs.
  Its own type checker (bugs Arbace would inherit and fix in someone else's code). Speed: 5.7x
  slower than CF on `fib`, with no way to improve it short of rewriting its value model. It is a
  second analysis of Go beside the one Arbace would want anyway for its forms. Positions and stack
  traces map through printed text. The forms would not be first-class but printed text.
- **Verdict:** a useful spike to explore the REPL's interface design in a few days. As the
  architecture it contradicts "first-class".

### (d) Only compiled Go forms at the REPL

Go has no `dlopen` in a static, cgo-free build (`plugin` requires cgo, §3). What is possible:

- **(d1) A separate program.** Print the forms, build them with `bin/build-go` (1.7 s for a small
  program with the std cached) and run it as a child process: on Linux `ProcessBuilder` and jrt's
  spawn; in the box a spawned guest (S7). Values cross as text (EDN or Go's `%#v`) over pipes. There
  is no shared heap, no calls into the REPL's Arbace values, and state lives only as long as the
  child does.
- **(d2) Rebuild and restart.** Add the package to the program, relink (the link alone about 5 s,
  the compiled packages cached: STANDALONE-NOTES.md, "the build's speed"), restart, and replay the
  session's loads. The REPL's live state is lost; only what loads again comes back.
- **(d3) An in-process loader** of gc's object files: goloader's technique (relocate against the
  running binary's symbols, register module data, type links and itabs with the runtime). It is
  exact gc semantics and gc's speed. But it is deep in runtime internals that change each Go
  version: pclntab, moduledata, the GC's bitmaps, itab tables. Arbace strips its executable
  (SZ1-SZ4), so the symbols would have to be kept or tabled. In the box, executable memory is not
  an obstacle (TamaGo maps it). It is a research project, the run-time half of Futamura's F10/b3.
- **Buys:** gc's semantics exactly, gc's speed (d1, d3).
- **Cost and risks:** latency in seconds per change (d1, d2); no shared values (d1); lost state
  (d2); fragility (d3).
- **Verdict:** not the REPL's main path. (d1) is worth having as a command beside the interpreter
  (`(go/run-compiled 'go.bench)`), the same forms run by gc, which is also §4.3's check at the
  REPL.

### (e) Printing to Go and running on the host's Go toolchain from the REPL, with `go run`

A variant of (d1) without build/go's structure. It is excluded: CLAUDE.md forbids the host's
`go`, and TamaGo's Go is what build/go already drives.

### Side by side

| option | first-class (forms in, values shared) | fidelity to gc | speed (fib 22) | cost | risk | when |
|---|---|---|---|---|---|---|
| (a) own interpreter | yes | by the check (§4.3) | ~5.5 ms (sketch) | large, staged | medium (checker, reflect) | recommended |
| (b) IR first | yes | by the check | ≥ (a) | larger | medium | if a code generator comes |
| (c) yaegi | no (printed text) | yaegi's checker | 38.8 ms | small to start, then upkeep | high (stale) | a spike at most |
| (d1) child program | no (no shared heap) | exact | gc's | small | low | as a tool |
| (d2) rebuild, restart | no (state lost) | exact | gc's | small | low | no |
| (d3) in-process loader | yes | exact | gc's | research | very high | with b3, if ever |

### 5.6 Measurements

**E1: speed.** The same four functions (Appendix A) were run by gc, by yaegi v0.16.1, by
`go/ssa/interp` and by a hand-built typed-closure sketch of option (a)'s back end. The two Arbace
interpreters ran `test/classforms/bench.ae` at stage 0. Best of 5 runs (Arbace: two passes),
amd64, the machine shared, ms:

| | fib 22 | sumSq 1e6 | sieve 1e6 | objs 1e5 |
|---|---:|---:|---:|---:|
| gc (go1.27.1) | 0.091-0.150 | 0.326-0.339 | 2.58-2.93 | 0.061-0.132 |
| typed Go closures (sketch, Appendix A) | 5.47-5.61 | 21.97-22.26 | 55.46-55.73 | — |
| Arbace: Java forms (`CF`) | 6.8-7.0 | 95.4-127.1 | 415.3-487.6 | 47.1-62.6 |
| Arbace: the evaluator (Clojure fns in a script) | 8.4-8.6 | 55.7-58.1 | — | — |
| yaegi v0.16.1 (with its std tables) | 38.77-38.79 | 191.3-210.8 | 199.5-204.2 | 257.6-284.3 |
| `go/ssa/interp` (x/tools' fake std; 3 runs ÷ 3) | 75 | 845 | 1,466 | 509 |

Notes:
- CF's `sieve` marks from `2i` and gc's and the others' from `i*i`, and CF's `objs` sums a field
  getter over `new` objects, not a method on a pointer. These are comparable in kind, not exactly.
- The Clojure `sum-sq` is a `^long` loop. Clojure's `fib` is boxed, and its 8.4 ms reflects the
  closure compiler's work since CLASSFORMS-REPL's measurement (120 ms then).
- **On go1.27.1, `go/ssa/interp` does not run real programs**: with the real std it panics (a nil
  dereference) while interpreting the runtime's initialization. It ran only after the package was
  built against x/tools' fake std (`go/ssa/interp/testdata/src`).
- yaegi's std tables are its go1.22 ones (`//go:build go1.22`) on go1.27.1.

**E2: size.** Static binaries, `CGO_ENABLED=0`: the plain benchmark 2,371,517 bytes; with yaegi's
interpreter 8,156,413 (+5.8 MB); with yaegi's std tables as well 29,088,569 (+20.9 MB). In the
seed's program, `jrt` is 914 files and 1,079,576 lines of printed Go (15,407 exported functions,
47,445 exported methods, 2,655 exported types), `lang` 146 files and 205,890 lines (5,526, 13,867,
997). 46 std packages are imported directly (grep over the printed module).

**E3: the compiled path.** The benchmark written by hand as Go forms (Appendix A), at stage 0:

- `bin/build-go` printed and built it in 1.69 s, and its `fib 22` ran in 0.12 ms;
- `--print-only` took 1.51 s, and yaegi ran the printed `main.go` in 60.5 ms, its `fib 22` taking
  47.9 ms;
- the printed Go was what a person would write (Appendix A).

**E4: today.** At the stage 0, `(require '[arbace.go :as go])` fails ("Could not locate
arbace/go__init.class, arbace/go.ae, arbace/go.clj or arbace/go.cljc"), and a `go/func` in a
namespace without it is "No such namespace: go".

**E5: the forms' annotations** (§2.1's table): grep over `tamago/goroot`'s 2,152 `.ae` files.

## 6. Recommendation, plan, proposals

### 6.1 Recommendation

Option (a): an interpreter of Go forms of Arbace's own. The front end goes in Arbace, as the Java
forms' analysis is. The back end is typed Go closures in hand-written Go forms, as jrt is. The
compiled packages are called through a generated table. The interpreter is kept equal to gc by a
per-program check over `$GOROOT/test`'s forms. Build it in stages, each useful by itself, starting
with what everything else needs: the table and Arbace calling compiled Go. Keep (d1) as a command.
Spike (c) only if the REPL's interface needs exploring before GR3. Take staging (F7) between CF
and the Go interpreter after F7's own prototype, not before.

### 6.2 The staged plan

| milestone | content | check | after |
|---|---|---|---|
| **GR0** | the spec: `doc/go/REPL-SPEC.md` (the forms at the REPL, the values' mapping, the table, the namespaces, errors), from this survey's §2 and the user's decisions | — | the decisions |
| **GR1** | the table of compiled packages (generator, typed invokers, the declarations digest), the `GoValue` rule in jrt, §2.2's conversions; **Arbace calling compiled Go**: `(go.strings/ToUpper "abc")` | a corpus of calls of the listed packages against expected values; size measured | GR0 |
| **GR2** | `arbace.go.check` without generics: scopes, types, constants (exact), assignability, method sets, embedding; the annotation check | the converter's annotations recomputed over std and `$GOROOT/test` (§4.3) | GR1 (the digest) |
| **GR3** | the interpreter's core: `arbace.go` macros, the builder, the back end's nodes for scalars, strings, slices, arrays, maps, structs, pointers, functions and closures, control flow, `defer`/`panic`/`recover`; package vars and init; redefinition | `$GOROOT/test`'s `run` programs within the subset, interpreted against gc (§4.3) | GR2 |
| **GR4** | methods, interfaces, type switches and assertions, embedding at run time, goroutines, channels, `select`; wrappers of compiled interfaces | more of `$GOROOT/test`; the directory tests | GR3 |
| **GR5** | the crossings: Arbace fns as Go func values, `GoPanic`, Go frames in traces, the REPL's printing, `go/eval`, `go/run-compiled` (d1); Java forms ↔ Go forms through vars | an oracle file of cross-dialect forms (`test/oracle/forms/go.ae`) | GR4 |
| **GR6** | generics: the checker's inference, instantiation per type arguments, std's generic packages interpreted from forms | `$GOROOT/test/typeparam` | GR5 |
| GR7 (later) | staging shared with CF (after F7), reflect's named types through the runtime overlay (G8), speed (F10's inline caches) | as above | F7, GR6 |

This follows the agenda's order. The build comes after F2-F4 and the renames (c2a to
repr/clojure, `arbace.aot` to c2j), since the checker and builder are compiled by c2j and the check
is F2's extension.

### 6.3 Effort

The estimates go by the record of comparable work done here. The Java forms at the REPL were built
in two days (2026-10-09/10), but on an existing analysis; this needs its analysis written. repr/go
reached its round trip (G0-G2) in two days. j2g's first program took about three days.

| milestone | estimate (working days) |
|---|---:|
| GR0 spec | 1-2 |
| GR1 table, Arbace → compiled Go | 2-3 |
| GR2 checker (no generics) | 5-7 |
| GR3 core interpreter | 5-7 |
| GR4 methods, interfaces, concurrency | 3-5 |
| GR5 crossings, REPL | 3-4 |
| GR6 generics | 3-5 |
| **total to GR6** | **22-33** |

Re-seeds: GR1, GR3, GR5 and GR6 change the program (jrt, the back end, the embedded namespaces),
one re-seed each at most. They can be batched with other runtime changes.

### 6.4 Proposals

- **G1 The approach.**
  - Options:
    - (a) Arbace's own interpreter: front end in Arbace, back end in hand-written Go forms (§5 (a));
    - (b) an IR first (§5 (b));
    - (c) yaegi embedded, fed printed forms (§5 (c));
    - (d) the compiled path only (§5 (d)).
  - *Recommended: (a).*
- **G2 Where the back end lives.**
  - Options:
    - (a) hand-written Go forms in the program (`go/arbace/goi/`), nodes as typed Go closures (§2.7);
    - (b) Java forms like CF, translated by j2g.
  - *Recommended: (a).* Closures and Go's own types are what Java forms cannot express.
- **G3 The checker.**
  - Options:
    - (a) `arbace.go.check` in Arbace, checking converted and hand-typed forms alike, the converter's
      annotations verified against it (§2.1);
    - (b) trust the annotations, checking only hand-typed forms;
    - (c) `go/types` in the program, the forms printed first.
  - *Recommended: (a).* (c) gives up forms-first and puts a Go library at the front.
- **G4 Values.**
  - Options:
    - (a) typed slots for scalars, real Go values (reflect-made) for composites, the interpreter's
      own descriptors for named types and interface values (§2.2);
    - (b) `reflect.Value` everywhere (yaegi);
    - (c) Arbace's own representation (Java objects) for everything.
  - *Recommended: (a).* (c) cannot cross to compiled Go without copying, and (b) is the slowest.
- **G5 The table of compiled packages.**
  - Options:
    - (a) generated by the build for a listed set (the std packages the program imports, their
      exported API), with typed invokers for common signatures, reflection otherwise, a declarations
      digest from the forms, and wrappers for the listed interfaces (§2.5);
    - (b) all of std;
    - (c) none (Go forms only call Go forms).
  - *Recommended: (a)*, with its size measured at GR1 before the list grows.
- **G6 The Arbace runtime's Go API for Go forms.**
  - Options:
    - (a) a small hand-written Go package `arbace` (`Call`, `Invoke`, `Str`, `GoStr`, `Kw`, `Sym`,
      `Var`) in the table, with the mangled names of `lang`/`jrt` not listed;
    - (b) `lang` and `jrt` in the table as they are (mangled names: `Invoke_O__O`);
    - (c) none.
  - *Recommended: (a).* (b) can stay as an advanced escape, unlisted.
- **G7 Generics.**
  - Options:
    - (a) instantiation per type-argument list when nodes are built, std's generic packages
      interpreted from their forms (§2.4);
    - (b) dictionaries over one body;
    - (c) no generics at the REPL.
  - *Recommended: (a)*, at GR6.
- **G8 Interpreted named types seen by compiled code and reflect.**
  - Options:
    - (a) wrappers for the listed compiled interfaces, `%T` and reflect differences recorded as
      known (yaegi's limit), and the runtime-overlay route (named types and method sets made at run
      time, as ixgo's `reflectx` patch does) left for a later decision;
    - (b) the runtime overlay now.
  - *Recommended: (a).*
- **G9 Namespaces and redefinition.**
  - Options:
    - (a) a Go package is a namespace, its declarations vars (funcs as `IFn` adapters); calls through
      cells, seeing redefinition; dependents re-checked; types by generation (§2.6);
    - (b) Go's static binding at the REPL too (a redefinition seen only by later definitions).
  - *Recommended: (a).*
- **G10 Go values in Arbace.**
  - Options:
    - (a) jrt's rule: a value of no Java class is an `arbace.lang.GoValue` (`toString` as `%#v`,
      `equals` Go's `==` or identity, a consistent `hashCode`), scalars and strings converted at the
      boundary by §2.2;
    - (b) every Go value wrapped in a box object.
  - *Recommended: (a).* It keeps identity, and Go values stay passable to Go unchanged.
- **G11 The REPL's surface.**
  - Options:
    - (a) declarations in `go.*` namespaces through `arbace.go`'s macros, loaded on the first Go
      form; `go/eval` for statements and expressions in the current Go namespace (a REPL line,
      yaegi-like), its value returned to Arbace; no Go code inside Clojure fns (SPEC principle 1);
    - (b) Go code forms inside Clojure fns as Java code forms are.
  - *Recommended: (a).*
- **G12 Errors and traces.**
  - Options:
    - (a) Go panics stay panics in Go code; a panic leaving into Arbace becomes the `Throwable` or a
      `GoPanic`; Go frames (function, file, line) in traces through `jrt.EvalTrace`; gc's runtime
      error messages (§2.3);
    - (b) panics become Java exceptions as soon as they occur.
  - *Recommended: (a).*
- **G13 Arbace objects implementing Go interfaces.**
  - Options:
    - (a) later: `reify` of a listed Go interface through its wrapper, after GR5;
    - (b) now, at GR4.
  - *Recommended: (a).*
- **G14 The check.**
  - Options:
    - (a) per program, interpreted against printed and gc-compiled, over `$GOROOT/test`'s forms
      (single-file and directory tests), plus the annotation check of the checker, in `bin/gate
      --full`, a subset in the essential gate when it is minutes (§4.3);
    - (b) a hand-written corpus only.
  - *Recommended: (a).*
- **G15 The compiled path at the REPL.**
  - Options:
    - (a) `go/run-compiled`: (d1), the forms printed, built by `bin/build-go` and run as a child
      program (in the box a spawned guest), output returned;
    - (b) none;
    - (c) (d3) an in-process loader.
  - *Recommended: (a)* at GR5; (c) not now.
- **G16 Unification with the Java forms' interpreter.**
  - Options:
    - (a) after F7's prototype, a study of shared node definitions for CF and the Go interpreter (the
      typed-slot machinery); Java forms interpreted through j2g → Go forms noted, not pursued
      (§4.3);
    - (b) now.
  - *Recommended: (a).*
- **G17 Records.**
  - Options:
    - (a) this survey kept as `doc/GO-FORMS-REPL-SURVEY.md`; the agenda's item replaced by the
      decided proposals and the plan's milestones; a journal entry; the experiments' code left in
      `.tmp/` (not merged);
    - (b) the experiments merged as a benchmark tool.
  - *Recommended: (a).*

## 7. Sources

- yaegi: https://github.com/traefik/yaegi (README, "Limitations"), v0.16.1, `interp/run.go`,
  `interp/cfg.go`, `interp/gta.go`, `interp/type.go`, `interp/use.go`, `stdlib/` (the copy
  studied: the module cache's `github.com/traefik/yaegi@v0.16.1`).
- gomacro: https://github.com/cosmos72/gomacro (README, `doc/features-and-limitations.md`,
  `fast/README.md`).
- ixgo (formerly igop, gossa): https://github.com/goplus/ixgo (README); `reflectx`:
  https://github.com/goplus/reflectx (README).
- golang.org/x/tools/go/ssa/interp: https://pkg.go.dev/golang.org/x/tools/go/ssa/interp
  (package doc); the copy studied `v0.44.1-0.20260420230617-19499e7caabc` (`interp.go`,
  `external.go`, `interp_test.go`, `testdata/src`).
- go-interpreter/ssainterp: https://github.com/go-interpreter/ssainterp (archived).
- goloader: https://github.com/pkujhd/goloader.
- Go's `plugin`: https://pkg.go.dev/plugin; `/root/tamago-go/src/plugin/plugin_dlopen.go` (build
  constraint).
- reflect's limits: https://go.dev/issue/16522 (NamedOf), https://go.dev/issue/4146
  (MakeInterface), https://go.dev/issue/15924 (StructOf and embedded methods),
  https://go.dev/issue/38783; `reflect.StructOf`'s doc.
- Go's checkers: `/root/tamago-go/src/go/types`, `src/cmd/compile/internal/types2` (go1.27.1).
- Joker (a Clojure in Go, not a Go interpreter): https://github.com/candid82/joker.
- Arbace: doc/go/SPEC.md (§1, §4.1, §7, §10, §11), doc/go/CLASSFORMS-REPL.md, doc/go/J2G-SPEC.md
  (§5.1, §5.12, §7.9, §8.4, §9.3, §10.7, §16), doc/go/EVAL-NOTES.md, doc/go/ROUNDTRIP.md,
  doc/go/BUILD.md, doc/FUTAMURA-SURVEY.md (§4 (f), (i), F2, F7, F8, F10),
  doc/STANDALONE-NOTES.md (the build's speed: the link), doc/NAMING-SURVEY.md,
  doc/REPR-HASKELL-SURVEY.md; `arbace/lang/Compiler.ae` (`CF` from line 6108, `Code*` from
  10198), `arbace/classes/interp.ae`, `arbace/classes/analyze.ae`, `test/classforms/bench.ae`,
  `test/build/go/hello/`; the seed `seed/arbace-seed-go.tar.xz` (`56fabe20…`).

## Appendix A: the experiments

All of them live in `.tmp/go-forms-repl-survey/` (not tracked). Builds used TamaGo's go1.27.1 with
`CGO_ENABLED=0`, `GOTOOLCHAIN=local`, a module cache of the scratch directory's own, filled from
the host's local cache (`GOPROXY=file://…`, no network) and `GOSUMDB=off`.

- `bench/prog/bench.go`: `fib`, `sumSq` (an `int32` loop, an `int64` sum), `sieve` (a
  `[]bool`), `objs` (a pointer method `(*P).add` over `&P{i, 1}`), each timed best of 5 by
  `time.Now`.
- `bench/yaegi/main.go`: `interp.New`, `Use(stdlib.Symbols)`, `Eval` of the benchmark's source
  (its `main` runs). `bench/yaegi0/` is the same without the std tables, for E2's size.
- `bench/ssa2/main.go`: the benchmark type-checked (`go/types`, the source importer over
  x/tools' fake std `go/ssa/interp/testdata/src`, plus `runtime/consts.go`), built into SSA with
  the fake `runtime` importable, and run by `interp.Interpret`; one program per benchmark
  (`bench/ssaprog/*`), 3 iterations each. `bench/ssa/main.go`, the same over the real std through
  `go/packages`, is the run that panics.
- `bench/closures/main.go`: option (a)'s back end sketched. Typed closures (`func(*frame) int64`,
  `func(*frame) bool`, `func(*frame) ctl`), frames of `[]int64` and `[]any`, calls through a cell
  (`**fn`, late bound), jumps as returned codes, the `int32` loop variable normalized after each
  increment. `fib`, `sumSq` and `sieve` were built by hand as a builder would build them from
  checked forms.
- `formsbench/go/bench.ae`, `formsbench/go/bench/main.ae`: the benchmark written by hand as Go
  forms, without any converter annotation (none was needed for this code: no untyped constant
  whose type the local rules leave open, no generics, no promoted fields), built with
  `bin/build-go -o bench.exe formsbench` and printed with `--print-only`, through the stage 0
  of the main checkout (`ARBACE_HOST_EXE`, `REPR_GO_EXE`). The function `sumSq` as typed:

  ```clojure
  (go/func sumSq ^int64 [^int32 n]
    (let [^int64 s (zero int64)]
      (for [i (conv int32 0)] (< i n) (inc! i)
        (set! s + (* (conv int64 i) (conv int64 i))))
      s))
  ```

  printed as

  ```go
  func sumSq(n int32) int64 {
  	var s int64
  	for i := int32(0); i < n; i++ {
  		s += int64(i) * int64(i)
  	}
  	return s
  }
  ```

- The Arbace rows: `target/seed/amd64/arbace test/classforms/bench.ae` (two passes; both shown as
  ranges).
- Counts: grep over `tamago/goroot/**/*.ae` (E5) and over the seed's extracted module (E2).
