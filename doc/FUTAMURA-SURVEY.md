# Futamura's projections for Arbace — survey

> Names: since 2026-10-17 c2a is repr/clojure (`arbace.c2a.*` is `arbace.repr.clojure(.*)`,
> `bin/c2a` `bin/repr-clojure read`, `bin/c2a-check` `bin/repr-clojure check`, `test/c2a/`
> `test/repr/clojure/`, `doc/C2A.md` `doc/REPR-CLOJURE.md`) and the back end `arbace.aot` is c2j
> (`arbace/aot/` `arbace/c2j/`, its marks `:aot/*` `:c2j/*`, `test/aot/` `test/c2j/`); "AOT"
> stays the compiled pipeline's name (`ARBACE_AOT`, the packages `aot_arbace_*`, the tree `aot/`;
> doc/NAMING-SURVEY.md, "The second rename as built").

Status: survey, 2026-10-17, the user's choice (2026-10-16) of the agenda's Later item "Futamura's
projections (the user, 2026-10-14)". Nothing is adopted here; the decisions (§7, F1-F11) are the
user's. Measured on main at `d2e220e3` (the seed `b566d53a...`, its stage 0 at
`target/seed/amd64/arbace`), amd64, TamaGo's go1.27.1; the experiment's code is on the branch
`futamura-survey` only (`test/futamura/`, §5).

The user's note (doc/AGENDA.md, Later):

> Arbace's compiler is written by hand, not derived, but it has the pairs the projections relate:
> the evaluated build (the evaluator and the image) against the compiled one (S6), and the class
> forms' interpreter (`arbace/classes/interp`) against j2g, kept in step by the suite and the
> oracle, not by construction. To survey: fns as Go closures (SA156 (c), closure generation, also
> a Go type fewer per fn); code generation (T4) by partial evaluation of the evaluator, ahead of
> time, in the way of Truffle/Graal (speculating on vars, falling back when one is redefined);
> the second projection, j2g derived from the interpreter, one definition of each language's
> semantics (Clojure, the Java forms, later Go). The cost: binding times in a dynamic language
> (vars, `eval`, macros at load time).

## Executive summary

- **Arbace already has two hand-made first projections and one hand-made second.** The
  evaluator's closure compiler (`CodeCompiler`, B1a step 7b) is the evaluator specialized to one
  method's `Expr` tree, done at run time by hand (the "closure generation" of Feeley and Lapalme);
  the back end `arbace.c2j.emit` is the same specialization written out as Java forms ahead of
  time; j2g is a compiler of Java forms beside an interpreter of them (`Compiler$CF`), both
  after one shared analysis (`arbace.classes.analyze`). Each pair shares its front half (the
  analysis: what is static) and repeats its back half (what to do with it) by hand: about 1,530
  lines of `Code` nodes plus 1,670 generated against 1,470 of `emit.ae`; about 2,500 lines of
  `CF` nodes plus 1,435 of `interp.ae` against j2g's 2,227 of `code.ae` (§1, §3).
- **The pairs are kept equal by tests, not per form.** The oracle and Clojure's suite run the
  compiled build, whose own namespaces are compiled and whose scripts and test namespaces are
  evaluated, so both halves run, but no check compares the evaluator and the back end on the same
  form; the fixpoint (G2 = G3) proves determinism, not equivalence. The interpreter-against-
  compiled check of the Java forms (`test/classforms/jvm_check.ae`) lives on the frozen branch
  only (§1.3).
- **Where the 16x against the JVM goes (measured, §5).** The back end's own code for `fib` and
  `loop-long`, rebuilt by hand in the seed's program (6.40 and 11.87 ms against T1's recorded
  6.36 and 12.21 ms), then changed one step at a time:
  - on `fib` (boxed), the call path (var, `IFn` cast, `AOTFn`, the arity's assertion) costs
    13%, while `Numbers`' generic boxed operations cost about 78%: a fast path for `Long`
    inline gives 6.40 → 1.44 ms. Speculating on the var as well gives 0.78 ms, and adding the
    argument's type (an unboxed worker behind guards) gives **0.168 ms, the JVM's 0.16**;
  - on `loop-long` (unboxed longs), **the class initialization guard pushes even `x < y` over
    gc's inlining budget** (`Numbers_Lt_J_J__Z` costs 81, the budget is 80; `Numbers_Init` alone
    is 75): the operations stay calls, 11.87 ms; inlined with their guards 2.81 ms, without the
    guards 1.09 ms (the JVM 0.41, Go's unchecked loop 0.31-0.62);
  - **fns as Go closures change nothing in speed** (6.46 against 6.40 ms): SA156 (c) is about
    size only (7,915 code types in the seed).
- **So most of the speed needs no partial evaluator.** Two template-level changes to the back
  end come first: arithmetic intrinsics, and no initialization guard for the classes
  initialized at start. Var speculation follows, cheap because a var is already an indirection:
  the guard is one compare against the root the prepare run left, its fallback the call T1 makes
  today, and no frame state needs to move. Only type specialization across calls (S2) needs what
  partial evaluation and deoptimization give, and it is also the step that reaches the JVM.
- **Binding times** (§3.4): static at build time are the analysis (macros expanded, symbols
  resolved to vars, members resolved by hints, constants) and the final var roots after the
  prepare run, which is a snapshot in the way of weval and Wizer: 3,161 vars in the loaded
  namespaces, 2,601 holding fns, 178 macros. Dynamic are the roots under `def`, `alter-var-root`
  and `with-redefs`, the 108 dynamic vars, protocol extensions, multimethods, `eval` and `load`,
  and classes redefined. Clojure's own precedent is direct linking: vars taken as static except
  `^:dynamic` and `^:redef`, with no fallback. The JVM Arbace's jar used it; the Go build does
  not.
- **The second projection, by self-application, is not the route.** A self-applicable
  specializer for a language with Java's semantics, mutable frames and exceptions remains a
  research problem: no production system derives its compiler that way. The practical form of
  "one definition of semantics" is staging: each node kind's semantics written once and run
  either to compute (the interpreter) or to emit code (the compiler). This is LMS, "collapsing
  towers", Truffle's DSL and Deegen, and Clojure macros make it natural (§2, §4).
- **Recommendation** (§7): first the measurable template changes (F3, F4), then the per-form
  differential checks that make any later derivation safe (F2), then guarded var speculation (F5)
  and type specialization (F6) on the numbers. The staged single definition (F7, F8) is the
  design of T4 and of the Java forms' pair, prototyped on a small subset of node kinds before
  any commitment. No self-applicable mix (F1), no run-time specializer for now (F10), closures
  only for size (F9).

## 1. The projections, stated for Arbace

### 1.1 The general statement

For an interpreter `int` of a language L (written in a language M), a program `p` of L, its input
`d`, and a specializer `mix` for M (`[[mix]](q, s) = q_s` with `[[q_s]](d) = [[q]](s, d)`):

| projection | equation | result |
|---|---|---|
| first | `target = [[mix]](int, p)` | `p` compiled into M |
| second | `comp = [[mix]](mix, int)` | a compiler from L to M |
| third | `cogen = [[mix]](mix, mix)` | a compiler generator: interpreter → compiler |

The second and third need `mix` to be **self-applicable** (written in M, effective on itself);
the cogen approach writes the generating extension by hand instead (Holst and Launchbury;
Birkedal and Welinder), which is what staging systems do.

### 1.2 Arbace's artifacts in that frame

| | interpreter `int` | program `p` | the specializer's role | today's hand-made counterpart |
|---|---|---|---|---|
| Clojure | the evaluator: `Expr.evalIn` and the closure compiler's `Code` nodes (`arbace/lang/Compiler.ae`, `Evaluator`, `Code*`, `CompilerOps.ae`) | a namespace's analyzed units (`Compiler$AST/UNITS`, the prepare run's) | specialize `Code.run` to the tree, ahead of time, with the vars' roots as speculated static data | `arbace.c2j.emit` (T1) → Java forms → j2g → Go |
| Java forms | `Compiler$CF` (about 70 node classes) built by `arbace.classes.interp` | a class's analyzed typed nodes (`arbace.classes.analyze`) | specialize `CF$Node.eval` to the tree | j2g's `code.ae` (Java forms → Go forms) |
| Go (later) | an interpreter of Go forms (none today) | Go forms (`repr/go`'s) | specialize to the package | gc (Go's own compiler) |

The evaluator itself runs on Go (j2g's translation of `Compiler`), so M is Go for the run-time
part and Java forms for what the back end emits. A specializer in Arbace would work on the
Java forms of the evaluator (`Compiler.ae`'s `Code` classes are Java forms) and produce class
forms, which j2g translates as it does today: the projection would sit between the analysis and
j2g, in `emit.ae`'s place.

What each projection would replace:

- **The first**, `[[mix]](evaluator, units)`: `emit.ae`'s templates, so that compiled code
  follows from the evaluator's own definition (what `Code` nodes do is what compiled code does).
- **The second**, `[[mix]](mix, evaluator)` = a back end: `emit.ae` and the closure compiler as
  one generated pair; for the Java forms, `[[mix]](mix, CF)` = j2g's `code.ae` (not j2g's model,
  declarations, reachability or the world, which are not about code).
- **The third**: a generator taking each language's interpreter (Clojure's, the Java forms', a
  Go interpreter) to its compiler. It is the "one definition of each language's semantics" of
  the note.

### 1.3 How the pairs are kept equal today

| check | what it compares | the pair it covers |
|---|---|---|
| the oracle (`bin/oracle`, 22,558 cases) | the Go build against the frozen JVM Arbace's recorded results | both halves at once: forms are evaluated, `arbace.core` and the others compiled |
| Clojure's suite (`bin/clojure-tests`, 20,506 of 20,520) | the test build against its own record | test namespaces evaluated, the runtime's namespaces compiled |
| the fixpoint (`--fixpoint`, `bin/seed --verify`) | G2 = G3, stage 0 = G1 = G2 = G3 | j2g and the back end are deterministic functions of the sources, not equivalence |
| `bin/j2g-evalproof` (31 forms) | the translated evaluator against a recorded output | the evaluator only |
| the oracle's `defclass` forms (213) and corpus (131) | Java forms interpreted on Go against the JVM's compiled results | the interpreter (against the JVM's compiled classes, not against j2g) |
| `test/classforms/jvm_check.ae` | the interpreter against compiled classes, per form | interp against emit, on the JVM, on `arbace-for-java-26` only since S4 |

What is not checked per form: one form compiled by the back end against the same form
evaluated, and one Java form interpreted against the same Java form translated by j2g. A
mismatch in one half shows only when a test happens to depend on it. S6 found three causes this
way (STANDALONE-NOTES.md, "S6": a fn argument adapted as a functional interface, a `^long` return
not converted, two fns given one class), each caught by the oracle only because `arbace.core`'s
own code went through the back end.

## 2. Prior art

| work | what it is | applies to Arbace? |
|---|---|---|
| Futamura, "Partial evaluation of computation process — an approach to a compiler-compiler" (Systems, Computers, Controls 2(5), 1971; reprinted in Higher-Order and Symbolic Computation 12(4):381-391, 1999) | the three projections | the frame (§1) |
| Jones, Gomard, Sestoft, *Partial Evaluation and Automatic Program Generation* (Prentice Hall, 1993; the authors' PDF) | offline partial evaluation: binding-time analysis (BTA), then specialization; self-application; "the trick"; congruence; termination | the vocabulary and the method; BTA is what §3.4 does by hand |
| mix (Jones, Sestoft, Søndergaard, 1985 and 1989) | the first self-applicable partial evaluator (first-order, a subset of Lisp) | shows self-application works for small, pure, first-order languages, which Arbace's are not |
| Similix (Bondorf and Danvy, 1991), Schism (Consel, 1993) | self-applicable, higher-order (Scheme subsets), with side effects in Similix | the closest in language family; still a subset, and kept research tools |
| cogen by hand (Holst and Launchbury 1991; Birkedal and Welinder, "Hand-writing program generator generators", 1994) | write the generating extension directly instead of self-applying | the realistic form of the second and third projections |
| PyPy / RPython (Bolz, Cuni, Fijałkowski, Rigo, "Tracing the meta-level", ICOOOLPS 2009) | a meta-tracing JIT: the interpreter is written once (RPython), with hints; the JIT traces the interpreter running the program | the principle (one interpreter, the compiler derived) applies; the mechanism (tracing at run time, a JIT that emits machine code) does not fit a static Go executable without its own code generator (b3) |
| Truffle and Graal (Würthinger et al., "Self-optimizing AST interpreters", DLS 2012; "One VM to rule them all", Onward! 2013; "Practical partial evaluation for high-performance dynamic language runtimes", PLDI 2017) | AST interpreters that rewrite their nodes to specialized ones (types, call targets), partial evaluation of the node tree at run time when hot, `Assumption`s invalidated (a redefined global) with deoptimization back to the interpreter (Hölzle, Chambers, Ungar, PLDI 1992) | the closest model: the `Code` nodes are an AST interpreter; Truffle's `Assumption` is exactly var speculation; but Truffle specializes at run time with Graal as the compiler, where Arbace would do it ahead of time with gc (§4 (b), (e)) |
| Truffle DSL (Humer et al., GPCE 2014) | node specializations written once as annotated methods; the DSL generates the node classes and their rewriting | "one definition" at the node level, by generation: the model for §4 (f) |
| LMS (Rompf and Odersky, "Lightweight modular staging", GPCE 2010; CACM 2012) | staging by types: the same code computes or builds IR depending on whether a value is `Rep[T]` | one definition, two uses, without self-application |
| Amin and Rompf, "Collapsing towers of interpreters" (POPL 2018; Pink and Purple) | a stage-polymorphic evaluator: one interpreter that, given staged input, emits code; a tower of interpreters collapses into one compiler | the second projection made practical, in a Lisp; the nearest prior art to "j2g derived from the interpreter" |
| weval (Fallin, Bytecode Alliance; "Partial Evaluation, Whole-Program Compilation", arXiv 2411.10559, PLDI 2025) | the first projection ahead of time on an existing interpreter (SpiderMonkey's, in Wasm): a snapshot (Wizer) plus intrinsics marking the program and the specialization context; reported 2-5x over the interpreter | the closest to what the note asks (ahead of time, an existing interpreter, a snapshot of the loaded state) |
| Deegen (Xu and Kjolstad, arXiv 2411.11469, OOPSLA 2026) | an interpreter and a baseline JIT generated from one definition of each bytecode's semantics (C++) | "one definition, two tiers", by a generator |
| tools.analyzer (github.com/clojure/tools.analyzer, `.jvm`) and tools.emitter.jvm | Clojure's analysis as data (maps with `:op`), with passes; an emitter from it | `Compiler$AST/node` is the same idea (SA131); the passes model (type inference, locals) is how a BTA would be added |
| Clojure's compiler (`Compiler.java`) | `Expr.eval` and `Expr.emit` side by side, kept equal by hand; `eval` of anything non-trivial compiles a fn; direct linking (1.8): calls of non-dynamic, non-`^:redef` vars bound at compile time, no fallback | the original of Arbace's two pairs; direct linking is Clojure's binding-time decision for vars |
| SCI (github.com/babashka/sci) | a Clojure interpreter in Clojure, AOT-compiled by GraalVM native-image | an evaluator in the language itself, AOT: Arbace's position, without specialization |

What applies: the vocabulary of offline partial evaluation (BTA, static and dynamic, guards),
Truffle's speculation and invalidation model, weval's ahead-of-time setting, and staging
(LMS, Pink, Truffle DSL, Deegen) for "one definition". What does not: self-applicable mix for the
full language, and tracing or run-time partial evaluation while Arbace has no code generator of
its own (b3).

## 3. Arbace's design today

### 3.1 The evaluator

- **Analysis**: `Compiler`'s `analyze` makes `Expr` trees (the JVM's classes, with the Go
  build's fields); macros are expanded here, symbols resolved to `Var`s or locals, interop
  members resolved by hints (else reflection), primitive types inferred
  (`maybePrimitiveType`).
- **Evaluation**: top-level forms by `Expr.evalIn(Frame)`; each method of a fn, deftype or reify
  compiled lazily, at its first call, by `CodeCompiler` into `Code` nodes (Compiler.ae lines
  10198-11727): typed channels (`run`, `runLong`, `runDouble`, `runBool`), locals in slots or
  unboxed `prims`, constants made once, a `CodeVar` holding its `Var` and calling `deref` at each
  read, `CodeInvoke` calling `IFn.invoke` by arity, direct static calls to 578 methods of
  `Numbers`, `RT`, `Util` and `Math` by a generated `switch` (`CompilerOps.ae`), frames reused by
  depth (SPEED-NOTES.md, "The evaluator").
- **The image**: the prepare run stores the analyzed trees of the embedded namespaces; since S6
  it is empty in the shipped build (everything compiled), and still the mode of `ARBACE_AOT=0`.

This is the classic "closure compilation" step: the interpretive overhead of dispatching on the
node kind is paid once per method, not once per evaluation. In the vocabulary of §1, it is the
first projection done by hand, at run time, with the tree as static and the frame as dynamic.

### 3.2 The back end (S6)

`arbace/c2j/emit.ae` (1,470 lines) turns the same analyzed units, read as maps through
`Compiler$AST/node`, into Java forms, following the JVM's `emit` methods one to one (SA130):
a fn is a code class implementing `Compiler$AOTF<n>`/`$AOTR<k>` (SA141), held by a runtime
`Compiler$AOTFn`; var reads are `getRawRoot` (or `get` for a var dynamic when compiled, SA138);
calls are `IFn.invoke` on the root; arithmetic is `Numbers`' overloads chosen by the analysis's
types; protocol calls test the target's interface first (`emitProto`, without the JVM's class
cache). j2g translates the Java forms with Java's semantics (casts, class initialization
guards, null checks). For `(defn fib [n] ...)` the emitted Go is:

```go
func (t *A_fexp_bench_fib_A) Invoke_O_O__O(self, n any) any {
	if lang.Numbers_Lt_O_J__Z(n, 2) { return n }
	return lang.Numbers_Add_O_O__Number(
		lang.IFn_Cast(const_7.GetRawRoot__O()).Invoke_O__O(lang.Numbers_Minus_O_J__Number(n, 1)),
		lang.IFn_Cast(const_7.GetRawRoot__O()).Invoke_O__O(lang.Numbers_Minus_O_J__Number(n, 2)))
}
```

where `IFn_Cast` is a Go type assertion plus `jrt.IsDynamic`'s header check, and `AOTFn`'s
`Invoke_O__O` asserts its body to `Compiler$AOTF1` (two more assertions) before the call. Counted
in the seed's 100 compiled packages (their Go, with grep): 42,383 calls through
a var's root, 54,722 `IFn_Cast`s, 991 reads of dynamic vars, 746 boxed and 2,113 primitive
`Numbers` calls, 469 reflective calls, 17,686 class initialization guards (`X_Init()`), 7,915 fn
code types.

### 3.3 Speed (T1, T3) and SA156

- T1 against the JVM (STANDALONE-NOTES.md, "Benchmarks and T3 (SA139)"): compiled 3.2 times
  faster than all evaluated, **16 times slower than the JVM** in the geometric mean (the
  evaluator 54 times). T3 (trampolines) was 8% slower and was rejected; T4 (a tail call is a
  jump) waits for Arbace's own back end. The note's suspects: boxed arithmetic through
  `Numbers`, j2g's class initialization guards, the casts and interface assertions of each call.
  §5 measures them.
- The JVM's numbers come from the frozen jar, whose `arbace.core` is compiled **with direct
  linking** (VENDOR-NOTES.md) and runs under HotSpot's inlining JIT. The Go build has no direct
  linking (EVAL-PLAN.md §2.7, "direct linking: off").
- SA156 (STANDALONE-NOTES.md, "S6: the compiled program's size"): (a) built: a fn's code class
  is no Java object in Go (no `Object` protocol, 121.8 → 101.3 MB with SA158-SA159); (c) not
  done: a fn's code as a Go closure behind one adapter per arity (J2G-SPEC §7.11's lambdas), with
  no Go type per fn. It needs `^:once`'s clearing, `letfn`'s mutual references, captured loop
  locals, and stack traces that name the fn.

### 3.4 The Java forms' pair

`arbace.classes.analyze` (4,375 lines) makes typed nodes from Java forms; then either
`arbace.classes.interp` (1,435 lines) builds `Compiler$CF` nodes (Compiler.ae from line 6108,
about 2,500 lines: typed `eval`, `evalJ`, `evalD`, `exec`, `test`; jumps as pending codes;
inline-cached virtual calls) for classes made at run time at the REPL (CLASSFORMS-REPL.md), or
j2g's `code.ae` (2,227 lines) writes Go statements and expressions for the build. The
interpreter is 14-40 times faster than the evaluator and 5-200 times slower than HotSpot's
compiled code (CLASSFORMS-REPL.md §6). Both halves implement Java's semantics separately: the
order of evaluation, `int` wrap-around, `try`/`finally` with jumps, the class initialization of
JLS 12.4.2, lambdas.

### 3.5 Binding times

For a specializer, each datum is **static** (known when the code is made) or **dynamic** (known
only when it runs). In Arbace's build:

| datum | binding time | notes |
|---|---|---|
| the forms, macros expanded | static at analysis | a macro redefined later does not change code already analyzed, on the JVM as here |
| a symbol's resolution (local, `Var`, class, member) | static at analysis | the `Var` object is static; its root is not |
| interop members (with hints) | static at analysis | else reflection at run time |
| primitive types of locals and returns | static (inferred, hinted) | the evaluator's `prims`, the back end's typed locals |
| constants, keywords, quoted data | static | the back end's pools (SA155) |
| `*unchecked-math*`, `*warn-on-reflection*` | static per unit | recorded with each unit (SA132) |
| **a var's root after loading** | static *by speculation*: known at the end of the prepare run, which loads every namespace | 3,161 vars, 2,601 holding fns, 178 macros (the namespaces stage 0 loads for `test/futamura/vars.ae`) |
| a var's root later | dynamic: `def` again at the REPL, `alter-var-root`, `with-redefs`, `intern`, `:reload` | rare in running code, common at the REPL and in tests |
| a dynamic var's value | dynamic per thread (`binding`) | 108 dynamic vars; `^:redef` and `^:dynamic` metadata on 85 |
| a protocol's implementations | dynamic: `extend-type` at any time | 10 protocols; their fns' caches |
| a multimethod's table | dynamic: `defmethod` at any time | 9 multimethods |
| `eval`, `load`, `require` of new code | dynamic | the evaluator runs it |
| classes made at run time (deftype, defrecord again, proxies, Java forms at the REPL) | dynamic | they shadow compiled classes by name (SA136) |
| the type of a value in a boxed local | dynamic | a speculation target (S2 in §5) |

Two points come out of the table. The prepare run is a snapshot of the loaded program, as
Wizer's is for weval: speculating on the roots it leaves is speculating on the state a build
already computes. And the speculation needs no frame transfer when the guard sits at a call site
(the fallback is T1's own call); deoptimization in Truffle's sense is needed only once
specialized code carries state in a form the generic code cannot resume from (unboxed values in
the middle of a computation).

## 4. Options

Each with what it buys, its cost and risks, and a staged plan. "Speed" refers to §5's
measurements.

### (a) The hand-written pairs kept, the equivalence checked per form

The oracle's forms (and the suite's harvested ones) run three ways on one build: evaluated, as
today; compiled, the forms wrapped as a namespace and compiled by the back end into the test
build (as SA139's measurement build did); and, for the Java forms, interpreted against j2g's
translation (the oracle's `defclass` forms and corpus compiled into the test build). The results
are compared with each other, not only with the JVM's record.

- Buys: a mismatch between the halves found where it is, independently of the JVM; the safety
  net any later derivation (b, c, f) needs, since a derived back end is checked against the
  evaluator it was derived from.
- Cost: a test build with the forms compiled (the back end's failures are reported per source;
  forms that need the evaluator, such as `eval` and run-time classes, stay evaluated); j2g's
  time on more classes; a few minutes in the gate.
- Risks: small; the harness exists (`arbace.c2j.main/run` on a source list, `--build-tests
  OUT`).
- Plan: one corpus file first (`numbers`), then all; then the Java forms' pair; then in
  `bin/gate` or `--full`.

### (b) The first projection offline: the evaluator specialized on a namespace's trees

A specializer reads the `Code` classes' definitions (Java forms) and a method's analyzed tree,
unfolds `run` over the static tree, and leaves the dynamic parts (frame values) as residual
code: compiled code that is the evaluator's semantics by construction. Truffle's speculation
would be added by marking var roots as static under an assumption (a guard plus fallback), and
values' types as static under a type guard.

- Buys: one definition of Clojure's semantics for both builds; speed from the speculation (§5:
  up to the JVM's on `fib`).
- Cost: a partial evaluator for Java forms, which have mutable fields, loops, `switch`, `try`,
  monitors and Java's object model: a BTA over them, congruence, unfolding and memoization
  rules, termination (`loop` nodes, recursive fns), and code-size control. The `Code` nodes would
  need binding-time annotations or a rewrite into a form the specializer understands. The
  evaluator's laziness (compile at first call, the compat mode, `evalIn` fallbacks) is dynamic
  structure a specializer must see through.
- Risks: high. The residual code is only as good as the evaluator's node design. Today's `Code`
  nodes box at `invoke` (no primitive fns, EVAL-PLAN Q1), so specializing them gives T1's
  shapes, not S2's. Speed comes from speculation, which can also be added to `emit.ae` directly
  (d, e). Size grows with unfolding.
- Plan: only as (f): staged definitions rather than a specializer of arbitrary Java forms.

### (c) The second projection: j2g (or the back end) derived from the interpreter by a specializer

`[[mix]](mix, CF)`: needs (b)'s specializer to be self-applicable.

- Buys: in principle one definition of the Java forms' semantics, j2g's `code.ae` derived.
- Cost and risks: self-application has worked for small first-order or Scheme-subset languages
  (mix, Similix, Schism), not for a Java-semantics interpreter with mutable frames, and no
  production system derives its compiler this way. j2g's `code.ae` is also more than a code
  generator: Java's evaluation order with temporaries, the typed-nil rule, two-word races, Go's
  `defer` for `finally`, the line layout. A specializer would have to produce that Go from the
  interpreter's Go.
- Plan: not by self-application; by (f).

### (d) Template-level speed in the back end (no specializer)

Changes to `emit.ae` and j2g that §5 shows carry most of the gap:

1. **Arithmetic intrinsics**: `Numbers`' boxed overloads emitted with an inline fast path for
   `Long` (and `Double`) operands, exact Clojure semantics (overflow throws, as `Numbers.add`
   does), the generic call otherwise (`fib`: 6.40 → 1.44 ms).
2. **No class initialization guard for the classes initialized at start**: `Numbers`, `RT`,
   `Util`, `Var` are initialized before any compiled namespace runs. j2g could drop their guards
   in compiled code (a list, or a mark like SA158's `:c2j/nn`), or give `Numbers`' primitive
   operations guard-free variants, so that gc inlines them (`loop-long`: 11.87 → 1.09 ms; with
   the guards inlined, 2.81).
3. Protocol call sites with the JVM's inline class cache (`protocol` 7.93 ms against the JVM's
   0.96), keyword call sites, `case` on longs without boxing (SPEED-NOTES "Next").

- Buys: most of the measured gap at the cost of templates; no change to the language's binding
  times.
- Cost: small (emit templates, j2g's guard rule); the compiled program grows by the fast paths
  (to measure: 746 boxed `Numbers` sites, a few hundred bytes each).
- Risks: low; the oracle's `numbers` corpus covers overflow and promotion.
- Plan: (1) and (2) measured on `eval-bench.ae` compiled (SA139's measurement build), the
  oracle and the suite; then (3).

### (e) Var speculation in the back end: guarded direct linking

A call `(f x)` of a var whose root at the end of the prepare run is a compiled fn of the program
becomes `if root(v) == F { F_body.invoke(x) } else { IFn_Cast(root(v)).invoke(x) }`, a direct Go
call of the code's method under one pointer compare. `^:dynamic` and `^:redef` vars are excluded,
as for Clojure's direct linking, but redefinition stays correct because the guard falls back to
T1's call, which needs no deoptimization.

- Buys: the call path (13% of `fib` alone, more with intrinsics: 1.44 → 0.78 ms). It also lets gc
  inline small fns across namespaces, and it is the precondition of (g).
- Cost: the back end must know, per call site, the root's code class (the prepare run's state);
  j2g must know a code class's Go method; arity selection; variadic fns stay generic.
- Risks: the roots of the prepare run differ from those of the running program when a namespace
  is reloaded or redefined; the guard catches that. Code size: one branch per call site (42,383
  sites).
- Plan: after (d); measured; then the default if the oracle and the suite pass unchanged.

### (f) One definition per node kind, staged (the projections in practice)

The node kinds' semantics written once, as Arbace code that is either run (an interpreter node)
or run with static inputs to emit Java forms (the back end): LMS's `Rep[T]`, Pink's
stage-polymorphic evaluator, Truffle DSL's generated nodes. In Arbace the natural vehicle is
macros: a `defnode` form per kind generates both the `Code` (or `CF`) class and the `emit`
method. For example, `if`, a local, `let`, `loop`/`recur`, a `Numbers` static call, `invoke`,
each a few lines that today exist twice.

- Buys: the second projection's benefit (one definition, two halves equal by construction for
  the nodes defined so), without a self-applicable specializer; a path for the Java forms (CF and
  `code.ae` from one definition) and later for Go forms (an interpreter of Go forms at the REPL
  and their compilation through gc).
- Cost: a redesign of the `Code` nodes (Compiler.ae) and `emit.ae`, node kind by node kind, with
  (a)'s checks to keep both outputs as they are; the Go build's `Compiler` is part of the runtime
  (a change rebuilds it, the build's two-run case).
- Risks: the two halves' shapes differ for good reasons (the interpreter's frames and jump codes,
  the compiler's Go control flow); a shared definition that cannot express one half's needs
  becomes a constraint. Start where the halves are closest (expressions without control).
- Plan: a prototype on a small subset (constants, locals, `if`, a `Numbers` call, `invoke`) on a
  branch, measured against (a)'s checks, before any commitment; then the design of T4 (Arbace's
  own back end, survey §7 b3), where the emit half targets Arbace's own IR instead of Java forms.

### (g) Type specialization across calls (the step that reaches the JVM)

Per fn, an unboxed worker for the argument types the analysis or a profile suggests (`long`,
`double`), entered from the boxed entry when the arguments have those types, calling itself (or
other workers) under (e)'s var guard; overflow throws as Clojure's `+` does (no promotion, so
this is no guard failure); a failed var guard in the middle of the worker needs a fallback with
the computation's state boxed (a deoptimization to the generic code at a call boundary).

- Buys: `fib` 6.40 → 0.168 ms (the JVM 0.16): the gap closed where boxing was the cost.
- Cost: a type inference over fns (or profiles from a training run, as `--pgo` takes them), the
  workers' code, and the fallback at call boundaries. Code size: a second body per specialized
  fn.
- Risks: correctness of the fallback (side effects before a failed guard must not repeat);
  `^long` hints give a weaker form today (Clojure's primitive fns, absent from the Go build:
  EVAL-PLAN Q1, S6 "primitive fns (open)").
- Plan: primitive fns first (the language's own mechanism, `invokePrim`); then speculation on
  unhinted fns, if the numbers justify it.

### (h) Fns as Go closures (SA156 (c))

- Buys: one Go type fewer per fn (7,915 in the seed, about 1 KB each with their function table:
  some 8 MB of the 101 MB). **No speed** (§5: 6.46 against 6.40 ms).
- Cost: `^:once` clearing, `letfn`, captured loop locals, stack traces' names.
- Risks: stack traces (the closure's Go name is its enclosing method's).
- Plan: independent of the projections; when size matters (SA156's own recommendation).

### (i) Run-time specialization at the REPL (Truffle-like)

Nodes that rewrite themselves (types, call targets, inline caches) are within reach of the
`Code` nodes today. Partial evaluation at run time, though, needs a compiler at run time. In a
static Go executable that means generating Go and running gc (seconds, in the box a spawned tool
guest: S7) or Arbace's own code generator (b3).

- Buys: the REPL's code as fast as the compiled build's; speculation on the REPL's own
  definitions.
- Cost: self-specializing nodes are cheap (an inline cache per `CodeInvoke`); run-time
  compilation is the b3 project.
- Plan: inline caches in the `Code` nodes when the REPL's speed matters; run-time compilation
  with b3.

### Side by side

| option | one definition | speed (§5) | cost | risk | when |
|---|---|---|---|---|---|
| (a) checks per form | safety net for all | none | small | low | first |
| (b) first projection, specializer | yes | via speculation | very large | high | not as such |
| (c) second projection, self-applied | yes | none by itself | research | very high | no |
| (d) intrinsics, guards | no | most of the gap | small | low | first |
| (e) guarded direct linking | no | the call path | medium | low | after (d) |
| (f) staged definitions | yes, per node kind | as the templates | large, incremental | medium | prototype, then T4 |
| (g) type specialization | no | to the JVM on numeric code | large | medium | after primitive fns |
| (h) closures | no | none | medium | medium | for size |
| (i) run-time specialization | no | the REPL's code | small (caches) to b3 | medium | later |

## 5. Experiment: what a specializer's output would gain over T1

`test/futamura/` on the branch (`run.bash`, `fexp.go`, `bench.ae`, `vars.ae`): `bench.ae`'s
`fib` and `loop-long` (`eval-bench.ae`'s) compiled by stage 0's back end into Java forms, and
their T1 shapes written in Go as j2g translates them, in a file added to the seed's main package
(run when `FEXP` is set), against the seed's real `jrt` and `lang`. Then hand-specialized variants
of the kind a partial evaluator with speculation would produce. Built with TamaGo's go1.27.1 and
the seed's flags and overlay; run as `eval-bench.ae` runs (best run over at least 1 s and 3
runs); three runs, amd64, the machine shared, ms:

| variant | `fib 22` (boxed) | what changes |
|---|---:|---|
| T1 (the back end's code) | 6.34-6.44 | (recorded in SA139: 6.36) |
| T1 as a Go closure behind one adapter (SA156 (c)) | 6.42-6.51 | the code's Go type |
| S1: the var speculated (guard, direct call) | 5.56 | no `IFn_Cast`, `AOTFn`, `AOTF1` assertions |
| T1b: T1 with `Long` fast paths for `<`, `-`, `+` | 1.40-1.49 | no `Numbers` dispatch; values still boxed and allocated |
| S1b: S1 with the fast paths | 0.78 | both |
| S2: the var and the argument's type speculated (an unboxed worker, guards) | 0.168 | no boxing |
| S2 after the var is redefined (the guard falls back) | 5.56 | the generic path |
| Go's own `int64` fib | 0.091 | the floor |
| the JVM (SA139's record) | 0.16 | |

| variant | `loop-long` (1M iterations, unboxed longs) | what changes |
|---|---:|---|
| T1 (the back end's code: `Numbers`' `long` operations called) | 11.87-11.91 | (recorded: 12.21) |
| the operations inline, their class initialization guards kept | 2.80-2.81 | the guard alone |
| the operations inline, exact (overflow throws), no guards | 1.09 | |
| Go's unchecked loop | 0.31-0.62 | the floor (the range is code layout) |
| the JVM (SA139's record) | 0.41 | |

Why T1 calls the `long` operations: gc's inliner (`-gcflags=arbace/lang=-m=2` on the seed)
reports `Numbers_Init` inlinable at cost 75, and then `Numbers_Lt_J_J__Z` (`x < y` after the
guard) at 81, `Numbers_Xor_J_J__J` 81, `Numbers_Add_J_J__J` 139, `Numbers_Inc_J__J` 151, all
over the budget of 80; `IFn_Cast` is 179. The guard is what keeps the smallest operations from
being inlined.

Findings:

1. The call path is not where `fib`'s time goes: var, cast and dispatch together are 13%.
   `Numbers`' generic boxed operations (`ops(x).combine(ops(y))`, `GetClass`, the `Number`
   casts) are about 78% (T1 → T1b), and allocation of the boxes is less than the dispatch
   (S1b's 0.78 ms still allocates).
2. Speculation pays most when it removes boxing (S2); it reaches the JVM's time, which HotSpot
   reaches by inlining and escape analysis at run time.
3. For unboxed code the cost is the initialization guard, through the inliner's budget, not the
   guard's own instructions (2.81 against 1.09 ms).
4. Closures do not change the speed.
5. The fallback works without deoptimization machinery when the guard is at the call (S2 after
   redefinition takes the generic path and gives the same value). S2's worker reruns the call
   generically, which is valid only because `fib` is pure: general type specialization needs the
   state handed over at the guard.

## 6. Plan

| stage | content | check | depends on |
|---|---|---|---|
| P1 | (a) evaluated against compiled per form, for the oracle's forms; then interp against j2g for the Java forms | the halves agree; the JVM's record as now | — |
| P2 | (d1) arithmetic intrinsics, (d2) no initialization guards for classes initialized at start | `eval-bench.ae` compiled; oracle, suite; size | — |
| P3 | (e) guarded direct linking from the prepare run's roots | as P2; a redefinition corpus (`def` again, `with-redefs`, `alter-var-root`) | P2 |
| P4 | primitive fns (`invokePrim`) in the Go build, then (g) if the numbers justify it | as P2 | P3 |
| P5 | (f) prototype: a staged `defnode` for a subset of node kinds producing both the `Code` node and the emit template | P1's checks pass with the derived halves | P1 |
| P6 | (f) for the Java forms' pair, as the design of how j2g's code translation and `CF` relate | P1's Java forms checks | P5 |
| P7 | T4: Arbace's own back end (b3) designed with (f)'s definitions, its emit half targeting Arbace's IR; then (i) | the oracle, the fixpoint | P5, b3 |

P1 and P2 are independent of each other and of the projections. P5 can be judged after its
prototype: if the shared definitions cost more than the duplication, the pairs stay hand-written
with P1's checks.

## 7. Recommendation and proposals

Recommended: take the projections as a direction (one definition of each language's semantics,
by staging), not as a mechanism to build now. Do first what the measurements show pays (the back
end's templates, the initialization guards), and make the pairs checkable per form, so that a
later derivation can be proven against what exists. The proposals:

- **F1 The approach to "one definition of semantics".** (a) Staging: each node kind defined once
  and generating both halves (§4 (f)); (b) a self-applicable specializer (the literal second
  projection, §4 (c)); (c) the hand-written pairs kept for good, with checks. *Recommended:
  (a)* as the direction, judged on F7's prototype, with (c) as the fallback; (b) is research
  with no production precedent for such a language.
- **F2 Differential checks per form.** (a) The oracle's forms compiled by the back end and
  compared with their evaluation, and the Java forms interpreted compared with j2g's
  translation, in the test build (§4 (a)); (b) the JVM's record only, as now. *Recommended:
  (a)*, in `bin/gate --full` first, in the essential gate if it costs minutes.
- **F3 Arithmetic intrinsics in the back end.** (a) `Numbers`' boxed operations emitted with an
  inline `Long`/`Double` fast path and Clojure's exact semantics (§4 (d1)); (b) left to
  `Numbers`. *Recommended: (a)*: about 4.4x on `fib` in the experiment, measured on the whole
  benchmark set before adoption.
- **F4 Class initialization guards of the classes initialized at start.** (a) j2g omits the
  guard in compiled code for a fixed list (`Numbers`, `RT`, `Util`, `Var`, ...), checked
  initialized when the program starts; (b) guard-free variants of `Numbers`' primitive
  operations called by the back end; (c) the guards kept. *Recommended: (a)*: about 4x on
  `loop-long` with the guards alone (2.81 → 1.09 once inlined; 11.87 → 1.09 in all), and it
  helps every class on the list.
- **F5 Var speculation.** (a) Guarded direct linking: a call of a var whose root after the
  prepare run is a compiled fn becomes a direct call under a pointer compare, the generic call
  otherwise; `^:dynamic` and `^:redef` excluded (§4 (e)); (b) Clojure's direct linking (no
  guard: redefinition not seen by callers, as on the JVM's jar); (c) none. *Recommended: (a)*
  after F3 and F4, on the numbers: it keeps the REPL's redefinition semantics that (b) gives up.
- **F6 Type specialization.** (a) Primitive fns (`invokePrim`) in the Go build first, then
  speculative unboxed workers with fallbacks at call boundaries if the benchmarks still show the
  gap (§4 (g)); (b) speculative workers at once; (c) neither. *Recommended: (a)*.
- **F7 A prototype of staged definitions.** (a) On a branch: a `defnode` form for a few node
  kinds (constants, locals, `if`, a static `Numbers` call, `invoke`), generating the `Code` node
  and the emit template, checked by F2; (b) not now. *Recommended: (a)* after F2, as the test of
  F1 (a).
- **F8 The Java forms' pair.** (a) The same staged approach for `CF` and j2g's code
  translation, after F7, as a design study first; (b) kept as two implementations with F2's
  check. *Recommended: (b)* now, (a) after F7's result.
- **F9 Fns as Go closures (SA156 (c)).** (a) Left as SA156 recommends: later, if size matters;
  not part of this work (no speed gain, §5); (b) now. *Recommended: (a)*.
- **F10 Run-time specialization at the REPL.** (a) Inline caches in the `Code` nodes when the
  REPL's speed matters; run-time compilation with Arbace's own back end (b3); (b) a run-time
  path through gc now. *Recommended: (a)*.
- **F11 Records.** (a) This survey kept as `doc/FUTAMURA-SURVEY.md`; the agenda's Later item
  replaced by the decided proposals; a journal entry; the experiment's code (`test/futamura/`)
  kept on the branch only, or merged as a measurement tool. *Recommended: (a)*, the experiment
  not merged (it patches the seed's printed program).

## 8. Sources

- Y. Futamura, "Partial evaluation of computation process — an approach to a compiler-compiler",
  Systems, Computers, Controls 2(5):45-50, 1971; reprinted in Higher-Order and Symbolic
  Computation 12(4):381-391, 1999 (doi:10.1023/A:1010095604496).
- N. D. Jones, C. K. Gomard, P. Sestoft, *Partial Evaluation and Automatic Program Generation*,
  Prentice Hall, 1993; https://www.itu.dk/people/sestoft/pebook/
- N. D. Jones, P. Sestoft, H. Søndergaard, "An experiment in partial evaluation: the generation
  of a compiler generator", RTA 1985; "Mix: a self-applicable partial evaluator for experiments
  in compiler generation", LISP and Symbolic Computation 2(1), 1989.
- A. Bondorf, O. Danvy, "Automatic autoprojection of recursive equations with global variables
  and abstract data types", Science of Computer Programming 16(2), 1991 (Similix).
- C. Consel, "A tour of Schism", PEPM 1993.
- C. K. Holst, J. Launchbury, "Handwriting cogen to avoid problems with static typing", 1991;
  L. Birkedal, M. Welinder, "Hand-writing program generator generators", PLILP 1994.
- M. Feeley, G. Lapalme, "Using closures for code generation", Computer Languages 12(1), 1987.
- C. F. Bolz, A. Cuni, M. Fijałkowski, A. Rigo, "Tracing the meta-level: PyPy's tracing JIT
  compiler", ICOOOLPS 2009 (doi:10.1145/1565824.1565827); https://rpython.readthedocs.io
- T. Würthinger et al., "Self-optimizing AST interpreters", DLS 2012; "One VM to rule them all",
  Onward! 2013; "Practical partial evaluation for high-performance dynamic language runtimes",
  PLDI 2017 (doi:10.1145/3062341.3062381); C. Humer et al., "A domain-specific language for
  building self-optimizing AST interpreters", GPCE 2014
  (https://lafo.ssw.uni-linz.ac.at/pub/papers/2014_GPCE_TruffleDSL.pdf);
  https://github.com/oracle/graal/tree/master/truffle
- U. Hölzle, C. Chambers, D. Ungar, "Debugging optimized code with dynamic deoptimization",
  PLDI 1992.
- T. Rompf, M. Odersky, "Lightweight modular staging", GPCE 2010; CACM 55(6), 2012.
- N. Amin, T. Rompf, "Collapsing towers of interpreters", POPL 2018 (doi:10.1145/3158140);
  https://github.com/namin/pink
- C. Fallin et al., weval: https://github.com/bytecodealliance/weval; "Partial Evaluation,
  Whole-Program Compilation", https://arxiv.org/abs/2411.10559 (PLDI 2025);
  https://cfallin.org/pubs/pldi2025_weval_slides_public.pdf
- H. Xu, F. Kjolstad, "Deegen: A JIT-Capable VM Generator for Dynamic Languages",
  https://arxiv.org/abs/2411.11469 (OOPSLA 2026).
- https://github.com/clojure/tools.analyzer, https://github.com/clojure/tools.analyzer.jvm,
  https://github.com/clojure/tools.emitter.jvm; Clojure's `src/jvm/clojure/lang/Compiler.java`
  (https://github.com/clojure/clojure), direct linking (Clojure 1.8 release notes,
  `changes.md`); https://github.com/babashka/sci
- Arbace: doc/STANDALONE-NOTES.md ("S6: Arbace compiled to Go", "Benchmarks and T3 (SA139)",
  "S6: the compiled program's size", SA138, SA141, SA156, SA158); doc/STANDALONE-SURVEY.md
  (§5.4 T1-T4, D6, §7); doc/go/SPEED-NOTES.md ("The evaluator: closure compilation");
  doc/go/EVAL-PLAN.md (§2.7, Q1); doc/go/CLASSFORMS-REPL.md; doc/go/ORACLE.md;
  doc/VENDOR-NOTES.md (direct linking on the JVM); `arbace/lang/Compiler.ae` (`CF` from line
  6108, `Evaluator`, `AST`, `Code*` from 10198), `arbace/lang/go/CompilerOps.ae`,
  `arbace/c2j/emit.ae`, `arbace/c2j/main.ae`, `arbace/classes/interp.ae`, `arbace/j2g/code.ae`;
  the seed `seed/arbace-seed-go.tar.xz` (`b566d53a...`), its compiled packages' Go.

## Decided (2026-10-17)

The user: "start the Futamura proposals F1–F11 when you see fit as your suggestion". The lead's
suggestion, so decided: F1 (a) staging as the direction, judged on F7's prototype, the
hand-written pairs as the fallback; F8 (b) the Java forms' pair kept as two implementations with
F2's check, the staged approach after F7; F9 (a) closures later, if size matters; F10 (a)
inline caches when the REPL's speed matters; F11 (a) as done. The order of the work, after the
rename of 2026-10-17 (j2g and the bridges `repr/java`, `repr/go`; it touches the same files):
1. F2 (a): per-form differential checks (the oracle's forms compiled by the back end against
   their evaluation; the Java forms interpreted against j2g's translation), in `bin/gate
   --full` first; no change to the program, no re-seed.
2. F3 (a) and F4 (a) together: arithmetic fast paths in the back end with Clojure's exact
   semantics, and no initialization guards for the classes initialized at start (a fixed list,
   checked at start); measured on the whole benchmark set, checked by F2; one re-seed.
3. F5 (guarded direct linking) decided on the numbers after F3 and F4; F6 (primitive fns, then
   unboxed workers) and F7 (the `defnode` prototype) after that.

## F2 as built (2026-10-17)

F2 (a) as decided: each language's two hand-written halves run on the same forms, in one
program, and compared case by case. `bin/diffcheck` (`make diffcheck`), in `bin/gate --full`;
on the branch `f2-diff`.

### The design: one program built for the check

The cheapest sound shape is the one §4 (a) sketched: a program that carries both halves of each
pair, so that a case runs twice in the same executable and only the half under test differs.
It is neither the shipped program nor the test build (Clojure's suite keeps its own); it is built
from the gate's executable, whose runtime is the checkout's:

1. `test/diffcheck/gen.ae` writes each corpus file as a namespace (`dcf.STEM`): one top-level
   form per case, `(diffcheck.driver/case ID FILE LINE TEXT)`. `case` is a macro
   (`test/diffcheck/driver.ae`): it reads the case's text when the form is analyzed, by the
   evaluator when the namespace is evaluated and by the compiling program's evaluator when the
   back end compiles it, and expands to `(form* ID FILE (fn* [] FORM))`. The case is a fn's
   body, so the evaluated run takes the evaluator's closure compiler (`Code` nodes) and the
   compiled run the back end's Java forms translated by j2g; `form*` records it as the oracle's
   driver does (value and class, exception chain, `*out*`, `*err*`). A case that cannot be read
   or analyzed is found by a trial analysis first (of the text read again; its events but a
   proxy's not recorded) and expands to its exception chain, alike in both runs.
2. The gate's executable prepares the runtime's namespaces and the corpora's with
   `ARBACE_COMPILE` (as `bin/arbace-go`'s host compile) and `arbace.c2j.main/run` compiles them
   all into one tree: each source in its own id space, so the runtime's part is the shipped
   build's tree.
3. j2g translates the program with that tree and the Java forms' classes (below), and
   `bin/build-go` builds it: `.tmp/diffcheck/arbace-diff` (280 MB: 153 sources, 21,370 compiled
   classes, against the shipped 100 and 8,141).
4. `test/diffcheck/main.ae` runs each corpus twice on it, 16 runs at a time, and compares the
   two runs' records (normalized as the oracle normalizes, and the numbers of fn classes),
   against the known disagreements `test/diffcheck/known.edn` (the oracle's format and rules:
   `check-known`, `--write-expected`).

The pairs:

| pair | run A | run B | corpora | cases |
|---|---|---|---|---:|
| Clojure's | the namespace loaded from source: evaluated | `Compiler$AOT/load`: compiled | 53 (the oracle's forms but three, the harvest) | 11,758 |
| the Java forms' | evaluated, the classes interpreted (`arbace.classes.interp`; defined again at run time, they shadow the compiled ones by name, SA136) | evaluated, each defining case replaced by `classes*`: the classes j2g translated into the program, imported as `defclass` imports them | `defclass.ae`, `defclass_corpus.ae`, `native.ae`, j2g's fixtures (`test/j2g/fixtures`, their 79 recorded calls) | 713 |

134 top-level classes of 86 defining cases are given to j2g (`jforms/`, a Go package per Java
package, `jforms/packages.edn`: in `lang` two simple names of two packages collide). A class
defined twice (`Again`) or listed in `test/diffcheck/jforms-excluded.edn` stays interpreted in
both runs; `native.ae` is in the Java forms' pair only, since the back end cannot compile its
code forms in fns, deftypes and reifys (below). Cases the back end or j2g cannot compile at
all, so that the rest of their file compiles, are left out of both runs with their reasons
(`test/diffcheck/excluded.edn`, 5 cases), and the oracle's namespace `user` is named
`dcf.user` in Clojure's pair (below).

### What it found

Run on 2026-10-17: 12,449 of 12,471 cases agree, 22 disagree, all recorded with their reasons.

Fixed in `arbace.c2j.emit` (small and clearly right; the only changes to the shipped program,
below):

- **`import`'s value**: compiled `(import ...)` was nil, where the evaluator (`ImportExpr.evalIn`)
  and the JVM's `ImportExpr.emit` leave the class: 21 cases, `definterface` among them (it
  expands to an import).
- **A deftype in a fn's code**: its methods' `this` was emitted as the enclosing fn's `self`
  (`emit-deftype-class!` kept the fn's `:self-sym`). A top-level `defrecord` is such a deftype
  (its expansion is a `let`, which a compiled loader runs as a fn), so every compiled record
  failed j2g's analysis (`Unable to resolve symbol: self`). The runtime's namespaces have no
  deftype that names `this` there, so their tree did not change.

Known, recorded in `test/diffcheck/known.edn`:

| kind | cases | what |
|---|---:|---|
| j2g, a real divergence | 5 | an interface method a deftype or reify leaves unimplemented throws `UnsupportedOperationException` ("j2g: not translated: abstract ...") where the evaluator and the JVM throw `AbstractMethodError` (F2.3) |
| j2g, a real divergence | 1 | reflection finds no constructor of a translated abstract class (`(Base.)`: "No matching ctor found" where the interpreted class fails with `InstantiationError`) |
| j2g, a limit | 7 | Clojure data in a class body (`:k`, `#'arbace.core/map`): "j2g: Clojure constant ..." when the class is initialized (F2.4) |
| the interpreter, a real divergence | 1 | a lambda of an interface made at run time lacks its default method (`negate`: `AbstractMethodError`) (F2.5) |
| the interpreter, known limits | 2 | a class made at run time cannot extend `StringReader` nor reach `MethodHandles` (why j2g's fixtures are recorded on the JVM) |
| compilation's semantics | 3 | compiled code interns the vars it names when its constants are made, at the loader's start (as the JVM's AOT) |
| the check's own | 3 | the compiling program had loaded `arbace.set`; an identity hash; `eval`'s error position (a compiled loader sets no `Compiler/LINE`, as the JVM's `__init`) |

Found by the build itself, recorded with their reasons in the two exclusion files (they stop
the build, so they cannot be disagreements):

- **j2g bugs**: anonymous classes with constructor arguments (`(anon K ^{:param-tags [int]}
  [9])`, `(anon In [k] :outer o ...)`: the arguments dropped or passed to the wrong
  constructor, Go does not compile; `(anon In [2] ...)` of a member class reading the outer
  field: "no Go type for nil") (F2.2).
- **j2g's Go names collide** for a reify inside an anonymous fn inside a fn: `X$fn__N$fn` (the
  reify's declaring class) gets `X_fn__N_fn_class`, the Go name of the static field `fn-class`
  of the code class `X$fn__N` (SA154); j2g reports it (J2G-SPEC §4.4) and stops (4 cases) (F2.6).
- **The back end's limits**: no class for a reify or deftype whose methods use code forms (the
  Java forms compiler makes it at run time, unrecorded): `native.ae` is in the Java forms' pair
  only; code analyzed in a namespace of one segment (`user`) cannot be compiled, its holder
  class being in Java's default package, which Java forms of the package `arbace` cannot name
  (the corpora's `user` is named `dcf.user`); a proxy whose class was made at run time before
  its form is analyzed records no event, and compiled code names a class the program lacks (1
  case).
- **Clojure in class bodies** that names the namespace's vars or defines a deftype at run
  time, classes the Java forms compiler's tests expect not to compile, classes naming
  test files the corpus does not keep, a native method: interpreted in both runs
  (`jforms-excluded.edn`, 19 classes).

The halves agree on all else: numbers, strings, printing, the reader, sequences and
transducers, collections, destructuring and macros, protocols and multimethods, types,
concurrency, files, processes, sockets, java.time and java.text, the harvest of Clojure's suite;
the Java forms' arithmetic, control flow, arrays, statics, inheritance, nested and anonymous
classes, lambdas, switch, records, enums and exceptions, as far as the classes are defined.

### Cost, and the gate

| step | time |
|---|---:|
| the corpora's sources | 5 s |
| prepared and compiled (all 153 sources; the corpora are evaluated once here) | 125-185 s |
| j2g | 80-90 s |
| `bin/build-go` (Go's build cache and repr/go's print cache warm) | 20 s |
| the runs (114 runs, 16 at a time; reducers.ae 30 s alone) | 30-45 s |
| in all, from the gate's executable | about 4.5-6 minutes |

Steps 1-3 are cached (`.tmp/diffcheck`, keyed by the gate's executable and the check's sources):
unchanged, `bin/diffcheck` takes the runs' 30-45 s. It is a step of `bin/gate --full`
(`diffcheck`, concurrently with the other chains, after the essential gate's builds, whose
executable it reuses).

### Changes to the shipped program

`arbace/c2j/emit.ae` only, the two fixes above. The back end is part of the program, so the
shipped executable changes; its compiled namespaces change in their import units alone (45
files of the tree, each only by `(do (.importClass ...) nil)` becoming `(.importClass ...)`,
checked against the tree of the unchanged back end). The seed stays as it is: stage 0 still
builds the program, compiling the namespaces with the checkout's back end (its source differs
from stage 0's embedded one, so it is loaded from the checkout: `compiled-back-end`); the
fixpoint's stage 0 = G1 is then reported different until a re-seed, and the change rides with
F3 and F4's.

### Proposals (the user's to decide)

- **F2.1 Where the check runs.** (a) In `bin/gate --full` only (as built); (b) also in the
  essential gate, concurrently with the suite and the oracle (about 5 minutes on a change to the
  sources, 40 s when cached); (c) only by hand. *Recommended: (b)* once F3 and F4 change the back
  end: they are the changes it is for.
- **F2.2 j2g's anonymous classes with constructor arguments.** (a) Fixed in j2g (the arguments
  of a `(anon C [args] ...)` and of `:param-tags`, and the outer instance of `:outer`), the
  three classes taken off `jforms-excluded.edn`; (b) left recorded. *Recommended: (a)*, with F3
  and F4's re-seed (j2g is in the program). The runtime has no such anonymous classes.
- **F2.3 An unimplemented interface method.** (a) j2g's stub throws `AbstractMethodError`, as the
  JVM and the evaluator; (b) kept as `UnsupportedOperationException`, recorded. *Recommended:
  (a)*, with the re-seed.
- **F2.4 Clojure constants in a class of Java forms outside the runtime.** (a) j2g makes them
  as the back end makes a namespace's constants (a pool read at class initialization); (b) left
  to the interpreter (Clojure in class bodies stays a REPL feature). *Recommended: (b)* now: no
  compiled class needs it yet.
- **F2.5 The interpreter's lambdas and default methods.** (a) A lambda of an interface made at
  run time gets its default methods (as `Dyn`'s classes fall back to them); (b) recorded.
  *Recommended: (a)*: small, and the interpreter is the REPL's Java.
- **F2.6 The back end's names and limits found here.** (a) The `fn-class` field renamed so no
  class name can collide (a reify in a nested anonymous fn), a namespace of one segment given a
  holder in a package, a reify or deftype with code forms compiled by the Java forms' own
  compiler into the tree; each with the corpora's cases put back; (b) recorded as they are: the
  runtime's namespaces need none of them. *Recommended: (a) for the name collision* (small, with
  the re-seed), *(b) for the others* until a user namespace is compiled (T4, the box's
  compiled user code).

**Decided (2026-10-17, the user), F2's proposals:** F2.1 (a): `bin/diffcheck` in `bin/gate --full`
now, in the essential gate once F3 and F4 change the back end; F2.2, F2.3 and F2.6's name
collision: fixed with the next re-seed (j2g's anonymous classes with constructor arguments, its
stub throwing `AbstractMethodError`, the Go-name collision of a reify in an anonymous fn); F2.5:
the interpreter's lambdas get their interface's default methods; F2.4 and the rest of F2.6,
against the recommendation ("leave recorded"): fixed too (Clojure constants in Java forms
classes outside the runtime; the back end's limits: one-segment namespaces, reifys and deftypes
using code forms, proxies made at run time before their analysis).
