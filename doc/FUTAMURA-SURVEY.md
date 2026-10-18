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

## F3 and the back end's limits as built (2026-10-17)

F3 (a) and the rest of F2.6 as decided, in `arbace.c2j` (`arbace/c2j/emit.ae`, `main.ae`) and
`arbace.classes.native`; on the branch `c2j-batch`. F4 (j2g's initialization guards) is another
branch's; the two are measured together after both merge, and the re-seed follows them.

### F3: Numbers' boxed operations with inline fast paths

A call of one of `Numbers`' boxed operations (an operand of type `Object`) is emitted with an
inline fast path (`numbers-intrinsic`, in `emit-static-method`): the operands are bound once, in
order; when the `Object` ones are `Long`s the operation is computed on longs, boxed with
`Long/valueOf`; for the `(Object, Object)` and `(Object)` operations, when they are `Double`s, on
doubles, boxed with `Double/valueOf`; anything else calls the operation itself. The operations:
`+ +' - -' * *'` and their `unchecked-` forms (`add addP minus minusP multiply multiplyP
unchecked_*`, signatures `(O,O)`, `(O,J)`, `(J,O)`), `inc inc' dec dec'`, negation, `quot rem`,
`bit-and bit-or bit-xor bit-shift-left bit-shift-right`, `< <= > >= ==`, `zero? pos? neg?`.

The fast path only ever answers what `Numbers` answers: it is `LongOps`' (`DoubleOps`') own
computation and boxing (`Numbers.num(long)` is `Long.valueOf`, with its cache of -128 to 127, so
boxing identity is the same), and every case where `Numbers` would not answer a long calls
`Numbers`: an overflow (which `+ - * inc dec` throw and the `'` forms promote to a `BigInt`;
tested as `Numbers.addP` and `multiplyP` test it), a subtraction of `Long/MIN_VALUE` (`Numbers`
adds the negation, which overflows), a negation of `Long/MIN_VALUE`, a zero divisor; and every
other operand (`Integer`, `Short`, `Byte`, ratios, bigints, `BigDecimal`, a `Long` mixed with a
`Double`, `nil`, a non-number, whose errors are `Numbers`' own). `==` stays `Numbers.equiv` (`=`
is `Util.equiv`, not an operation of `Numbers`, and is untouched); `*unchecked-math*` chooses
`unchecked_*` in the analysis, as before. Not intrinsics: the `(double, Object)` overloads (a cast
and `doubleValue` already), `max`/`min`, the primitive `(long, long)` operations (their cost is
the initialization guard, F4).

`fib`'s code (j2g's Go, abridged): `if jrt.Long_InstanceOf(n) { var n0 int64 = ...LongValue__J();
tmp1 = n0 < 2 } else { tmp1 = lang.Numbers_Lt_O_J__Z(n, 2) }`, and `+` as `nr := n0 + n1; if
(n0^nr)&(n1^nr) >= 0 { return jrt.Long_ValueOf_J__Long(nr).Ref() } else { return
lang.Numbers_Add_O_O__Number(a, b) }`.

Checked: Clojure's suite and the oracle (the essential gate); `bin/diffcheck` (all the numbers
corpora agree); and, once, a namespace of 26 values (0, ±1, 2, `Long/MAX_VALUE`,
`Long/MIN_VALUE` and their neighbours, values whose products overflow, 1.5, -0.0, 0.0, `##NaN`,
`##Inf`, 1/2, 3N, an `int`, a `short`, a `byte`, 1.5M, `nil`, a string, a `float`) through 37
unary and literal forms and, for each of the 676 pairs, 22 binary forms (and `identical?` of
results), each result or exception printed: compiled with the fast paths (61 sites) and evaluated
in the same program, 745,131 bytes of results, the same byte for byte (277 overflows, 182 zero
divisors among them).

**Measured** as SA139 measured T1 (`test/bench/eval-bench.ae` as the namespace `bench.eval`,
compiled with the runtime's namespaces into a measurement program by the gate's executable, j2g
and `bin/build-go`; before: main's `arbace/c2j`, after: this branch's; ms, the best of two runs
of each, each the benchmark's best over at least 1 s and 3 runs; amd64, the machine shared):

| benchmark | before | after | speed-up |
|---|---:|---:|---:|
| `fib` | 6.26 | 1.53 | 4.09 |
| `fib-prim` | 2.33 | 2.49 | 0.94 |
| `loop-long` | 12.20 | 12.58 | 0.97 |
| `loop-double` | 1.53 | 1.50 | 1.02 |
| `dotimes-array` | 11.60 | 11.03 | 1.05 |
| `reduce-range` | 63.08 | 37.46 | 1.68 |
| `reduce-fn` | 21.32 | 11.56 | 1.84 |
| `vector-conj` | 18.07 | 18.38 | 0.98 |
| `map-assoc-get` | 21.15 | 19.64 | 1.08 |
| `keyword-map` | 13.20 | 6.75 | 1.96 |
| `str-build` | 6.59 | 6.24 | 1.06 |
| `seq-ops` | 43.71 | 40.91 | 1.07 |
| `lazy-walk` | 28.22 | 25.31 | 1.11 |
| `transduce` | 19.44 | 16.11 | 1.21 |
| `into-xform` | 15.36 | 14.61 | 1.05 |
| `protocol` | 7.52 | 5.51 | 1.36 |
| `multimethod` | 8.94 | 8.24 | 1.08 |
| `destructure` | 30.22 | 23.27 | 1.30 |
| `closures` | 26.97 | 17.15 | 1.57 |
| geometric mean | 13.46 | 10.45 | 1.29 |

`fib` as the experiment predicted (§5: 6.40 → 1.44). The sequence and reduction benchmarks gain
through `arbace.core`'s own compiled arithmetic (`reduce +`, `range`'s steps, counters); the
benchmarks of primitive arithmetic (`fib-prim`, `loop-long`, `loop-double`, `dotimes-array`)
have no boxed operation and are unchanged within the noise (`loop-long`'s 3% in both runs is
code layout): their cost is F4's. The program: 1,235 sites with a fast path in the measurement
program's tree (the shipped namespaces' and the benchmarks'; 676 with a `Double` one); the gate's executable 103,837,856 → 104,673,440
bytes (+0.8%, the back end's own new code included); the constant pools unchanged (the fast
paths make no constants).

### One-segment namespaces

Code analyzed in a namespace of one segment (`user`) is compiled: its holder class (`user`,
nesting `user$eval1$fn__2`) and the loader of a source of one segment (`user__init`) are in
Java's unnamed package (`:package ""`, `e/unnamed-package`; a simple name would be read in the
tree's namespace, `arbace`, as `arbace.user`), and every file of the tree imports them (`(import
user)`, which the Java forms' front end records as a source import: Java could not name a class
of the unnamed package from a package, the Java forms can), so that the loader of
`dcf/state_errors.ae` names `user$eval1$fn__2`. The fns' classes keep their JVM names. The
shipped tree has no such namespace and does not change. `test/diffcheck/gen.ae` no longer renames
the corpora's `user` to `dcf.user`; the renaming hid cases: `dcf.user`, made bare by `in-ns`,
had no `arbace.core` referred, so a case such as `(identical? 'a 'a)` failed alike in both
runs. Run in `user` they show two disagreements, recorded: `(resolve 'map)` (compilation's
semantics, as the JVM's AOT: the loader interns `user/map`, which a later `(def map 1)` defines,
when its constants are made) and `(identical? 'a 'a)` (F3.3 below); and `(arbace.set/union #{1}
#{2})`, recorded as the check's own, agrees.

### Proxies made at run time before their analysis

A class implementing `IProxy` that no source's events made (`(get-proxy-class Object)` evaluated,
then `(proxy [Object] [] ...)` analyzed, which finds it made) is a class made at run time
(`runtime-class?`): compiled code reaches it by reflection, and the unit naming it makes it
again first, unless it exists, with `arbace.core`'s `define-proxy` by its own name (not by
`get-proxy-class`, whose name depends on the namespace current then), as an event's replay
does. `test/diffcheck/excluded.edn`'s case (`harvest/java_interop.ae:13`) is back and agrees.

### The Java forms compiler's classes

A deftype, defrecord, reify or fn whose code uses the Java forms' code forms is compiled by the
Java forms compiler (`arbace.classes.native`, SPEC §9.5), and so is a top-level `defclass`: the
class is the interpreter's, made at run time, unrecorded, so compiled code named a class the
program lacked. Now, while the program records the units it prepares (`ARBACE_COMPILE`),
`arbace.classes.native/compiled` keeps each compilation (the namespace, its class imports then,
the Java forms, the classes' names), and the back end writes them into its tree
(`aot_jf_NAME.ae`: `(in-ns 'NS)`, `(import ...)`, the `defclass` forms), each in its namespace's
Go package; the compiled code names them as before, and they are classes of the program. A
constant naming one is looked up where used (`*late-classes*`), so a class j2g leaves out (below)
fails the code that runs it, not the namespace's loader; an event of a class the analysis never
made (a reify's stub, its fn given to the Java forms compiler) is skipped. The shipped program's
namespaces have no such class: its tree does not change.

In `bin/diffcheck`, `native.ae` is in Clojure's pair too, as a copy (`dcf/native_c2j.ae`, its
cases named `c2j:test/oracle/forms/native.ae:LINE`): its `defclass` cases left out (the Java
forms' pair's subject, whose classes `jforms/` holds and the copy would define again), each case's
fn made inside the thunk (`(fn* [] ((fn* [] FORM)))`, `diffcheck.driver/case`'s `deferred`), so
that a class initialization's error is the case's. Of its 283 cases 225 agree; 58 disagree, all
j2g's F2.4 met in compiled code (recorded): the Java forms compiler's classes hold Clojure data
(`#'arbace.core/str`, keywords, quoted symbols), "j2g: Clojure constant ..." when initialized;
and five classes whose Clojure code names the namespace's vars or holds a `deftype*` j2g's
analysis leaves out ("not in the closed world"), as `jforms-excluded.edn`'s `dcf.defclass.Clj`
and `native.nested_deftype.Host`. They are the measure of F2.4's j2g side: each should agree once
it is built.

### The check

`bin/diffcheck`: 12,673 of 12,754 cases agree, 81 disagree, all recorded in
`test/diffcheck/known.edn` (before: 12,449 of 12,471, 22). Of the corpora checked before, 23
disagree (22, less `arbace.set/union`, plus the two found in `user`); the other 58 are
`native.ae`'s copy (283 cases new to Clojure's pair). `test/diffcheck/excluded.edn`: 4 cases (5
before: the proxy's is back; the 4 are j2g's Go-name collision, F2.6's, the other branch's).

The essential gate passed (the smoke test; Clojure's suite, no regressions; the oracle, 53
known mismatches, none new), and `bin/arbace-go --fixpoint` holds: G1 = G2 = G3 byte for byte
(104,706,208 bytes), stage 0 = G1 not, as expected until the re-seed (the back end and
`arbace.classes.native` changed).

### Proposals (the user's to decide)

- **F3.1 Where the fast paths stop.** (a) As built: `Long` operands, and two `Double`s for the
  `(Object, Object)` and `(Object)` operations, everything else `Numbers`; (b) also a `Long`
  mixed with a `Double` and the `(double, Object)` overloads; (c) also `Util.equiv` (`=`) on two
  `Long`s. *Recommended: (a)*: the measured gain is the `Long` case; (b) and (c) add code at every
  site for cases the benchmarks do not show.
- **F3.2 The primitive operations.** With F4's guards gone gc may inline `Numbers`' `(long, long)`
  operations (§5: `loop-long` 2.81 → 1.09 ms needs both); the back end could also emit them as
  the Java forms' operators directly (exact: `Math/addExact` for `+`, as `Numbers` does).
  *Recommended:* measure after F4 merges; emit them inline only if gc's inliner still leaves
  them out.
- **F3.3 Symbols in the constant pools.** (a) Kept as SA155 made them: a class's symbols without
  metadata made once, so `(identical? 'a 'a)` is true in compiled code (recorded); (b) a symbol
  per constant, as the JVM's `emitValue` and the evaluator make them (more objects at each
  class's initialization, every pool of the tree rewritten). *Recommended: (a)*: Clojure leaves
  symbols' identity unspecified, and no code of the runtime depends on it.
- **F3.4 `bin/diffcheck` in the essential gate** (F2.1's decision: once F3 and F4 change the back
  end): after both merge, with the re-seed. *Recommended:* then, as decided.

**Decided (2026-10-17, the user):** F3.1 (a), as built; F3.2 as proposed (measured after F4
merged: below); F3.3 (a), the symbols shared, `(identical? 'a 'a)` recorded as known; F3.4 as
proposed, `bin/diffcheck` in the essential gate after both merge, with the re-seed.

### F3 and F4 measured together

Main's j2g batch (`ca4d3eed`: F4's guard-free start classes, F2.2-F2.6's j2g fixes) merged into
`c2j-batch`. Three measurement programs as above, each with the merged tree's j2g and the gate's
executable of the merged tree: before, main at `30e9f246` (neither); F4, main's back end with
F4's j2g; F3 + F4, the merged tree. ms, the best of five interleaved rounds; the machine was
loaded this time (load average 15-25, other builds), so every column is slower than the quiet
measurement above (`fib` before: 6.95 against 6.26), the ratios within a round comparable:

| benchmark | before | F4 | F3 + F4 | speed-up |
|---|---:|---:|---:|---:|
| `fib` | 6.95 | 7.47 | 1.63 | 4.26 |
| `fib-prim` | 2.59 | 2.33 | 2.28 | 1.14 |
| `loop-long` | 13.31 | 4.77 | 3.90 | 3.41 |
| `loop-double` | 1.67 | 0.57 | 0.55 | 3.04 |
| `dotimes-array` | 13.56 | 10.02 | 10.33 | 1.31 |
| `reduce-range` | 77.13 | 70.12 | 40.23 | 1.92 |
| `reduce-fn` | 24.92 | 24.35 | 11.82 | 2.11 |
| `vector-conj` | 23.00 | 22.39 | 18.18 | 1.27 |
| `map-assoc-get` | 27.82 | 31.64 | 21.14 | 1.32 |
| `keyword-map` | 15.03 | 12.15 | 6.45 | 2.33 |
| `str-build` | 7.73 | 6.71 | 6.53 | 1.18 |
| `seq-ops` | 54.64 | 48.87 | 41.82 | 1.31 |
| `lazy-walk` | 31.81 | 30.50 | 24.45 | 1.30 |
| `transduce` | 21.65 | 21.59 | 15.47 | 1.40 |
| `into-xform` | 18.70 | 15.80 | 13.76 | 1.36 |
| `protocol` | 8.27 | 6.69 | 5.31 | 1.56 |
| `multimethod` | 9.83 | 8.54 | 7.95 | 1.24 |
| `destructure` | 35.71 | 32.94 | 25.09 | 1.42 |
| `closures` | 31.33 | 27.76 | 18.31 | 1.71 |
| geometric mean | 15.65 | 12.89 | 9.33 | 1.68 |

- **F4.2** (`fib` and `map-assoc-get` slower with F4 alone: here 6.95 → 7.47 and 27.82 → 31.64):
  gone with F3. The boxed generic calls gc inlined are now the fast paths' slow branches only
  (`Numbers_Add_O_O__Number_NoInit` and the like, reached by a non-`Long`); `fib` 1.63,
  `map-assoc-get` 21.14, both faster than before. *Recommended for F4.2:* nothing more (no
  out-of-line mark).
- **F3.2** (the primitive `(long, long)` operations): with F4 they inline. gc (`-m`) reports
  `Numbers_Lt_J_J__Z_NoInit` (cost 4), `Xor` (4), `Add_J_J__J_NoInit` (62),
  `Multiply_J_J__J_NoInit` (62), `Inc_J__J_NoInit` (74) inlinable and inlines them into the
  benchmarks' compiled code (`loop-long`: 13.31 → 3.90; `loop-double` 1.67 → 0.55). What is left
  of `loop-long` (the experiment's exact inline loop: 1.09 on a quiet machine) is
  `Math.*Exact`'s own code, which `Numbers` calls and the back end would emit the same.
  *Recommended for F3.2:* not emitting them inline in the back end; revisit `Math.*Exact`'s Go
  (jrt) if `loop-long` matters.

On the merged tree: `bin/diffcheck` 12,731 of 12,754 cases agree, 23 disagree, every one recorded
with its reason (`known.edn` and `excluded.edn` recorded afresh from the merged run; no case
excluded: the Go-name collision is fixed). F2.4 turned 44 of the `native.ae` copy's 58 into
agreements; 14 are left: the 11 of the classes j2g's analysis still leaves out (Clojure code
naming the namespace's vars, a `deftype*` in a handed-over fn: F2.4 made Clojure constants, not
these, which `jforms-excluded.edn` keeps excluded in the Java forms' pair too), and 3 where the
compiled code now answers as the JVM and the evaluator by design does not (a `^long` fn of the
Java forms compiler is an `IFn$LL` compiled, not interpreted: the oracle's known mismatch of the
same cases). The other 9: compilation's semantics (4, `(resolve 'map)` among them), F3.3 (1),
an identity hash, a message's position, the interpreter's two known limits.

## F4 and F2's j2g fixes as built (2026-10-17)

F4 (a) and the j2g-side fixes of F2's findings, as decided (F2.2, F2.3, F2.4, F2.5, F2.6's name
collision, and the abstract class's constructor); on the branch `j2g-batch`. The back end
(`arbace/c2j`) is untouched: F3 is built apart, and the two are measured together after both
merge, with one re-seed.

### F4: no initialization guards for the classes initialized at start

**The list.** `arbace/j2g/start.ae` (`arbace.j2g.start/classes`): the runtime's classes
(packages `arbace/lang` and `arbace/jrt`) that the program initializes before any user code, by
their Go names, in the order it initializes them: 82 classes, from `Boolean` ... `Main`, `RT`,
`Var`, `Keyword`, the collections, `Compiler`, `Util`, `LispReader` to `Numbers`,
`PersistentTreeMap`, `MultiFn`, `Ref`, `LockingTransaction`, `Compiler$C`. It is derived, not
written: `test/j2g/start_classes.ae EXE [--write]` runs the program three ways with a user
program that does nothing (`-e nil`, a script, the REPL on its standard input) with
`ARBACE_TRACE_INIT` set, under which jrt prints each class's initialization as it starts
(`jrt.ClassInit.Run`), and keeps the classes all three initialize, in the order of the first.

**What j2g does with it** (J2G-SPEC §6.2, "Classes initialized at start"). For a listed class
`C` (translated, with non-trivial initialization), each static method `C_m` becomes two
functions: `C_m_NoInit`, the body without the guard, and `C_m`, the guard (when the method has
one, as before) and a call of `C_m_NoInit`. Compiled code (arbace.c2j's classes) calls
`C_m_NoInit`, and reads and writes `C`'s statics without the guard; every other caller, the
runtime's own code above all, calls `C_m` as before, so the runtime's semantics do not change.
In the shipped program, 1,724 static methods have the two entries, and compiled code calls the
guard-free ones at 43,830 sites (`Compiler$AOT.fnClass`, `RT.intCast`, `RT.nth`, `Util.equiv`,
`Numbers.lt` ...). A stack trace leaves out `C_m`'s frame above `C_m_NoInit`'s (jrt's
`stackTrace`), so traces are as before.

**The check at start.** `jrt.StartCheck` runs at the start of a compiled loader's `load` and of
every compiled class's static initializer; the first call, before any compiled code runs,
initializes the listed classes whose initialization has not started, in the list's order, and
then requires each to be started (`jrt.ClassInit.Started`: done, or in progress by the current
thread, as JVMS 5.5 lets that thread see it half-initialized); otherwise the program stops
(status 70, naming the class and the stale list). j2g registers the list's classes with jrt
(`j2g_start.go`: their `ClassInit`s, `C_Init`s and names).

Why the first compiled code initializes some: the program starts compiled code early, inside
`RT`'s static initializer (`arbace.core`'s loader); 58 of the 82 classes have started by then,
and the other 24 the program initialized while loading the startup namespaces, `Numbers` among
them (from `arbace.core`'s first compiled unit). Restricting the list to the 58 would leave out
`Numbers`, the class the measurements are about; initializing the 24 a little earlier, at the
first compiled code instead of at their first use during the startup namespaces' loading, is
the one change of order (F4.1). Their static initializers make constants and intern vars; one
that needed a namespace the startup loads would fail when the program starts (loudly), and the
gate's checks (the smoke test, Clojure's suite, the oracle, `bin/diffcheck`) pass.

**Measured** (`test/bench/eval-bench.ae` compiled as a namespace into a build of the runtime's
namespaces, as SA139 measured T1: c2j, j2g `--program`, `bin/build-go`; the same procedure on
`30e9f246` and on this branch; the two builds run in turn three times, the best of each; ms,
amd64, the machine shared with other builds, load 7-13 on 64 threads):

| benchmark | before | after | after/before |
|---|---:|---:|---:|
| `fib` | 6.30 | 6.68 | 1.06 |
| `fib-prim` | 2.33 | 2.04 | 0.88 |
| `loop-long` | 12.20 | 4.37 | 0.36 |
| `loop-double` | 1.55 | 0.53 | 0.34 |
| `dotimes-array` | 11.60 | 9.54 | 0.82 |
| `reduce-range` | 62.86 | 64.12 | 1.02 |
| `reduce-fn` | 21.22 | 21.65 | 1.02 |
| `vector-conj` | 17.95 | 18.45 | 1.03 |
| `map-assoc-get` | 19.99 | 21.46 | 1.07 |
| `keyword-map` | 13.15 | 11.20 | 0.85 |
| `str-build` | 6.57 | 6.32 | 0.96 |
| `seq-ops` | 43.51 | 43.72 | 1.00 |
| `lazy-walk` | 29.15 | 26.74 | 0.92 |
| `transduce` | 19.30 | 20.15 | 1.04 |
| `into-xform` | 15.96 | 15.86 | 0.99 |
| `protocol` | 7.70 | 6.37 | 0.83 |
| `multimethod` | 9.03 | 8.54 | 0.95 |
| `destructure` | 29.53 | 30.23 | 1.02 |
| `closures` | 26.52 | 26.99 | 1.02 |

Geometric mean: 0.87 (13% faster). The primitive loops gain most (`loop-long` 2.8 times,
`loop-double` 2.9), as §5 predicted for the guard; `loop-long` stops at 4.37 ms rather than §5's
1.09 because gc inlines `Numbers`' `lt` and `xor` now but not `add`, `multiply` and `inc` (costs
139 to 151 with their overflow checks, over the budget of 80 with or without the guard): inline
exact arithmetic is F3's (the back end's fast paths). Calls through the runtime's statics gain
10-17% (`keyword-map`, `protocol`, `dotimes-array`, `fib-prim`). The boxed `fib` is 6-8% slower
in every run (measured again: 6.18-6.38 against 6.77-6.91), `map-assoc-get` 2-7%: their
compiled code calls `Numbers`' generic boxed operations, which gc now inlines into the fn's
code (they were over the budget with the guard); F3's fast paths replace exactly those calls,
so the two are to be measured together (F4.2). The shipped executable grows by 139 KB (0.13%).

### F2's j2g-side findings, fixed

- **F2.2, anonymous classes with constructor arguments.** One cause for the three classes: the
  analyzer (`arbace.classes.analyze/analyze-anon`) made the anonymous class's derived
  constructor before it knew the superclass constructor chosen, whenever choosing it asked
  whether the superclass's package-private constructors are accessible from the anonymous
  class (which made its members): the constructor kept no parameters, so `(anon K
  ^{:param-tags [int]} [9])` called `K_1_New` with an argument it did not take and `K`'s
  `Ctor_I` without one, and `(anon In [k] :outer o ...)` mixed the outer instance into the
  wrong constructors. The derived constructor now takes the chosen one's descriptor (flags and
  throws) when the members were made already. The analyzer is shared, so the interpreter's
  classes take the same constructor. `cfcorpus.c50.A2`, `c54.K` and `c57.PB` are translated.
  *A race behind two later flakes (2026-10-17, `bin/diffcheck`: a build error in
  `jf_cfcorpus_c50`, then `PB_1_New_PB` called with the wrong number of arguments).* j2g's
  second round (the late inputs: the compiled namespaces' tree, and diffcheck's Java forms)
  analyzes its classes in parallel tasks that shared the unit's order; `analyze-anon` takes the
  order's tail from where it stood (`process-classes! from`), so a task could resolve the
  members of, and analyze, another task's anonymous class declared meanwhile, before that task
  gave it its constructor: the F2.2 case again, by chance, and temporaries and literals numbered
  in the wrong task's id space. Reproduced on j2g alone over diffcheck's tree: the first two
  runs differed (the Go of `c57.PB`). Three causes, each fixed by construction: (1) the shared
  order (`arbace.j2g.world/load-world`): each task has an order of its own, as the first round's
  thread has, appended to the unit's in the tasks' order after each group; j2g prints how many
  classes the second round's analysis declared (77 there). (2) `arbace.classes.lower` numbered
  the names it makes (`Fn<N>`, `Reify<N>`, its gensyms) from a counter of its own shared by all
  threads, so in the order the tasks ran; now from `RT/nextID`, the calling task's id space
  (RT.ID_SPACE), as gensym's. (3) With both fixed, about one run in ten still differed, in the
  deftypes of `native.deftype` and `native.nested-deftype` alone: a class's Clojure constants
  (`const__N`, F2.4) were kept in a map from their keys, `[:var v]` or `[:const datum]`, which
  past eight entries iterates in its keys' hash order, a var's hash being its identity's; the
  static initializer and the fields' declarations followed that order, and with them the
  package's literal numbering (`Lit_N`, `j2g_strings`). The analyzer
  (`analyze/constant-field!`) now keeps them as a vector in the order made (const__0,
  const__1, ...) beside an index from key to field. Then the runs agreed at any number of jobs
  but one: at `J2G_JOBS=1` `native.nested-deftype` differed, its failures (expected ones,
  `test/diffcheck/known.edn`) with them, for two more reasons of the same kind, the tasks'
  order deciding what a task's analysis did. A member class that is a holder too (c2j's fn in a
  fn in a top-level form) was a task beside its own members: coming first, it analyzed them in
  place (in its id space, their failure its own), else found them analyzed. The tasks now go
  in groups by height in the nest, the classes without member classes first, then each holder
  in the group after its members' highest, so that no task analyzes another's class. And
  arbace.lang.Compiler loads `arbace.classes.native` on first use (`classForms`, for a deftype*
  whose methods use Java forms): the task that first used it loaded it in its own id space,
  and the others found its namespace already there, `compile-deftype` not yet bound ("unbound
  fn" for "Unknown type"); `load-world` loads it before the tasks start, in an id space of its
  own. (The check in `classForms` is racy in itself: any program's threads may meet it.)
  Proved by 29 runs over that tree, at the default jobs, `J2G_JOBS` 1, 2 and 32, and two at a
  time, all the same. The shipped program's translation does not change: its second round
  (only the compiled namespaces, which declare no local or anonymous classes) declares none,
  the runtime's classes hold no Clojure constants, and its j2g output differs from main's only
  in the packages of the namespaces changed.
- **F2.3, an unimplemented interface method**: j2g's dispatch method for a method without an
  implementation throws `AbstractMethodError` with HotSpot's message ("Receiver class C does
  not define or inherit an implementation of the resolved method 'abstract R m(P)' of
  interface I."), as the evaluator's classes (`dynAbstract`) and the JVM do
  (`decls/dispatch-forms`, `model/abstract-method-error-text`).
- **F2.4, Clojure constants in a class of Java forms**: the static initializer makes them as
  the interpreter does, `RT.var(ns, name)` for a var and `RT.readString(text)` for a datum,
  cast to the field's type (`decls/clj-const-store`); reachability already reached both. The
  runtime's classes hold none, so the shipped program does not change by it.
- **F2.5, the interpreter's lambdas**: a lambda's class made at run time takes its interfaces'
  default methods made at run time, as a class does (`interp/lambda-done!` calls `vtable!`):
  `(.negate p)` of an `FxPredicate` lambda works.
- **F2.6, the Go name collision**: a class of arbace.c2j's tree outside `arbace/` (a package
  `aot_...` in the tree's `packages.edn`: a user's namespace, the corpora's `dcf.*`) is named
  as the runtime's compiled namespaces are, by its whole binary name between `A_` and `_A`
  (`names/compiled-class?`), so a reify in an anonymous fn's class `X$fn__N$fn` and the static
  field `fn-class` of `X$fn__N` no longer meet; it is j2g's naming, the back end's field keeps
  its name. The shipped program's names do not change (its compiled classes are all under
  `arbace/`).
- **The abstract class's constructors**: j2g's member tables list an abstract class's public
  constructors (without a body: Go makes no object of it), so reflection finds them as on the
  JVM; `(Base.)` fails with `InstantiationError` (the evaluator's `NewExpr`), and
  `Constructor.newInstance` with `InstantiationException` (jrt). This one changes the runtime's
  reflection: `getConstructors` of the runtime's abstract classes (`AFn`, `ASeq` ...) now
  answers as the JVM's.

`bin/diffcheck`: 12,463 of 12,471 cases agree, 8 disagree, all recorded, 0 new (before: 22
known); `test/diffcheck/known.edn` keeps 8 cases (compilation's semantics 3, the check's own 3,
the interpreter's two known limits), `excluded.edn` 1 case (the proxy made at run time; 5
before), `jforms-excluded.edn` 16 classes (19 before). The back end's limits of F2.6 (one-segment
namespaces, reifys and deftypes using code forms, a proxy made at run time before its analysis)
are c2j's, left to its batch.

### Checks

The essential gate (`bin/gate`): passed (the smoke test; Clojure's suite, no regressions against
`test/arbace-go-results.edn`; the oracle, 53 known mismatches, 0 new). `bin/arbace-go
--fixpoint`: holds, G1 = G2 = G3 byte for byte (103,977,120 bytes, sha256 `898825d8...`), stage
0 different from G1 as expected until the re-seed. No re-seed: stage 0 still builds the program (the checkout's
j2g, jrt and runtime are loaded or compiled from the checkout), and the lead re-seeds after F3
and F4 merge. j2g asks nothing new of the JVM, so `jdk/edge.edn` does not change.

### Proposals (the user's to decide)

- **F4.1 Where the list ends.** (a) As built: the classes the program initializes before any
  user code, those not started when compiled code first runs (24, `Numbers` among them)
  initialized then, in the program's order; (b) only the classes already started when compiled
  code first runs (58), with no initialization moved, but without `Numbers` (no gain on the
  arithmetic benchmarks); (c) (b) plus `Numbers` named by hand. *Recommended: (a)*: derived,
  checked at start, and the order changes only for classes whose static initializers make
  constants.
- **F4.2 The boxed calls inlined.** `fib` (+6-8%) and `map-assoc-get` (+2-7%) are slower since
  gc inlines `Numbers`' generic boxed operations into compiled code. (a) Measured again with
  F3, whose fast paths replace those calls, before deciding anything; (b) keep the guard-free
  calls to `Numbers`' boxed generic operations out of line (`//go:noinline`-like, a j2g mark);
  (c) accept. *Recommended: (a)*.
- **F4.3 Threads during start.** A compiled namespace's code run by another thread while a
  listed class is still being initialized by the main thread (`RT`, during `arbace.core`'s
  loading) would see it half-initialized, where the JVM makes that thread wait. The startup
  starts no such thread (agents and futures run after it). (a) Accept, recorded as a deviation
  of §6.2; (b) `jrt.StartCheck` on every compiled loader's call to wait for the classes in
  progress by other threads. *Recommended: (a)*.

**Decided (2026-10-17, the user):** F4.1 (a) the 82 classes, as built; F4.2 (a) re-measured with F3
before deciding; F4.3 (a) recorded as a deviation of J2G-SPEC §6.2.

## F5 as built (2026-10-18)

F5 (a), guarded direct linking, as decided after F3 and F4; on the branch `f5-linking` (from main
`02e2b9d7`), in `arbace.c2j` (`arbace/c2j/emit.ae`, `main.ae`), the runtime's compiled fns
(`test/c2j/gen_fns.ae`, `arbace/lang/go/CompilerAOTFn.ae`) and one rule of j2g (`code.ae`).

### The design

- **Which sites.** A call `(f x ...)` of a var `f` with at most 20 arguments (more go through
  `applyTo`, as before) whose root at the end of the prepare run is a fn whose code the tree
  compiles: an evaluated fn (`Compiler$EvalFn`, whose `fe` is the `FnExpr` the back end makes a
  code class of) or, for the back end's own namespaces (which `compiled-back-end` loads compiled
  before it runs), a compiled one (`Compiler$AOTFn`, `$AOTRestFn`, named by its code class). Not
  linked: a var dynamic when compiled, `^:dynamic`, `^:redef` or a macro, a protocol's fn (its
  call site is `emitProto`'s), and a call of an arity the code has no fixed method for (a
  variadic call stays the generic one).
- **The guard.** `Compiler$AOTFn/linked r C/fn-class` (`$AOTRestFn`'s for variadic code): the
  body of root `r` when `r` is a compiled fn whose class is the code class's fn class, else nil;
  then `(.invoke (cast C body) r x ...)`, the code's method called directly with the fn as `self`,
  as `AOTFn.invoke` calls it; else the generic call T1 made. The root is read once, the generic
  path casts it to `IFn` before the arguments, as before. The compare is a pointer compare of the
  fn's class: `Compiler$AOT/fnClass` makes one `Class` per code class, by name, so the compare
  holds exactly for the fns of that code (every closure of it, `with-meta`'s copies) and for no
  other, needs no state published by the defining code, and cannot hold falsely: a fn evaluated
  at the REPL is no `AOTFn`, a fn of another code has another class. This is the pointer compare
  of the proposal ("the var's root against the fn the compiled code expects") made per code
  rather than per object, which is the same test for a top-level `defn` and stays exact for the
  rest. j2g casts to a fn's code (no `C_Cast`, SA159) by Go's assertion (J2G-SPEC's code-class
  paragraph).
- **Across namespaces.** A direct call names the code's Go type, so its package imports the
  code's, and the compiled namespaces' Go packages must not import each other in a cycle
  (`arbace.core` names `arbace.core.protocols`' interfaces, whose fns call `arbace.core`'s). The
  back end resolves the sites after every class is made (`link-plan`): the packages' graph from
  the classes each top-level Java form names (symbols, tags, param-tags), then the sites in the
  order emitted, a site in another package linked only when the graph does not reach the
  caller's package from the code's, its edge then added. In the shipped tree 42,852 of 46,345
  sites are linked, 3,493 generic (13,304 when only sites within a package were linked, the
  first version measured below).
- **Redefinition.** Nothing is assumed after the prepare run: a `defn` evaluated at run time, a
  `with-redefs`, `alter-var-root`, a `def` the loader runs again with another fn, a `binding` of
  a `^:dynamic` var (never linked) all leave a root the compare rejects or that is the code's
  own, and the generic path runs. Unlike Clojure's direct linking (F5 (b)), callers see it.

### Measured

As F3 and F4 were (SA139's way): `test/bench/eval-bench.ae` as `bench.eval` (and
`test/c2j/link_redef.ae`), compiled with the runtime's namespaces into a measurement program by
one executable (the branch's gate executable, for both), with each tree's c2j, j2g and
`bin/build-go`; before: main `02e2b9d7`, after: the branch; ms, the best of five interleaved
rounds, each the benchmark's best over at least 1 s and 3 runs; amd64, the machine shared (load
2-20):

| benchmark | before | after | speed-up |
|---|---:|---:|---:|
| `fib` | 1.42 | 0.96 | 1.48 |
| `fib-prim` | 1.99 | 1.49 | 1.34 |
| `loop-long` | 3.74 | 3.73 | 1.00 |
| `loop-double` | 0.54 | 0.55 | 0.98 |
| `dotimes-array` | 9.03 | 8.98 | 1.01 |
| `reduce-range` | 38.22 | 37.27 | 1.03 |
| `reduce-fn` | 11.14 | 10.84 | 1.03 |
| `vector-conj` | 17.57 | 16.64 | 1.06 |
| `map-assoc-get` | 19.01 | 18.47 | 1.03 |
| `keyword-map` | 5.96 | 6.11 | 0.98 |
| `str-build` | 5.98 | 6.08 | 0.98 |
| `seq-ops` | 39.97 | 31.00 | 1.29 |
| `lazy-walk` | 23.99 | 17.36 | 1.38 |
| `transduce` | 14.82 | 13.00 | 1.14 |
| `into-xform` | 13.61 | 11.26 | 1.21 |
| `protocol` | 4.90 | 4.71 | 1.04 |
| `multimethod` | 7.22 | 7.71 | 0.94 |
| `destructure` | 22.26 | 19.08 | 1.17 |
| `closures` | 16.45 | 16.64 | 0.99 |
| geometric mean | 8.66 | 7.89 | 1.10 |

`fib` 1.42 → 0.96 (§5's experiment: 1.44 → 0.78 with the call itself inlined; gc inlines no
recursive call, and the generic path stays beside it). The sequence benchmarks gain through
`arbace.core`'s own calls (`seq`, `first`, `next`, `cons` inside `map`, `filter`, `reduce`).
`multimethod` is 6-7% slower in every round (7.2 against 7.7), not investigated (its dispatch is
`MultiFn`'s, which has no linked site; code layout is the likely cause, F5.2). Earlier variants,
measured the same way in rounds of their own: sites within a package only, with an inline guard
(geometric mean 8.28 against 8.72, the program 168.3 MB); all sites, the guard inline (7.88,
186.0 MB); all sites, the guard in `linked` as built (7.97 in the same rounds, 176.1 MB).

**Size.** The measurement program 157,147,572 → 176,139,366 bytes (+12.1%): each linked site
holds both calls and the guard. The shipped executable is 119,226,528 bytes (G1 = G2 = G3).

### Checks

- **Redefinition** (`test/c2j/link_redef.ae`, run in the measurement program compiled and,
  `ARBACE_NO_AOT` set, evaluated: the same 20 lines, all `ok`): a linked call and `fib`; the
  fixed arities of a variadic fn; a `def` the loader runs again (the first call sees the first
  fn, the second the second); a closure's code; `with-redefs` to an evaluated fn and to another
  compiled fn, and back; `with-redefs` of `arbace.core/vec` seen by a caller in another
  namespace; `binding` of a `^:dynamic` var and its root; `alter-var-root` of a `^:redef` var;
  an arity the code lacks (the generic path's `ArityException`); `alter-var-root` to `with-meta`
  (the same code, still direct) and to other fns; a `defn` evaluated at run time in the namespace
  (the REPL's); and the compiled `fib` after `fib` is redefined at run time, whose own recursive
  calls then reach the new definition (`[8 8]`: Clojure's direct linking would answer 5).
- **`bin/diffcheck`** (in the essential gate): 12,731 of 12,754 cases agree, 23 disagree, all
  recorded, 0 new (the corpora's redefinition cases, `state_errors.ae`'s `with-redefs`,
  `alter-var-root` and `eval`'d `defn`, among the agreeing).
- **The essential gate** passed: the smoke test; Clojure's suite, no regressions; the oracle, 53
  known mismatches, 0 new; diffcheck as above. **`bin/arbace-go --fixpoint`** holds: G1 = G2 =
  G3 byte for byte (119,226,528 bytes, sha256 `6000241f...`), stage 0 different from G1 as
  expected until the re-seed. No re-seed here: the back end, j2g and `CompilerAOTFn.ae` are the
  program's, the re-seed is the lead's.

### Proposals (the user's to decide)

- **F5.1 Code size.** (a) As built: every site whose code is known linked, +12% of the program;
  (b) only sites within a Go package (a third of the sites, about a third of the growth, the
  geometric mean 1.06 instead of 1.10); (c) a site linked only when its code is small or hot (a
  profile, as `--pgo` takes one). *Recommended: (a)*: the gain is spread over `arbace.core`'s
  own calls, which (b) keeps and (c) needs a profile for; revisit with SA156 (c)'s closures if
  size matters.
- **F5.2 `multimethod`'s 6%.** (a) Investigated with gc's `-m` and a profile before F6; (b)
  accepted as layout. *Recommended: (a)*, small, with F6's measurements.

**Decided (2026-10-18, the user):** F5.1 (a), every known call linked (as built); F5.2 (a), the
`multimethod` slowdown investigated before F6.

## F5.2 as found (2026-10-18)

F5.2 (a): why `multimethod` is 6-7% slower with F5 (7.22 → 7.71 ms, "F5 as built"). Found: code
layout only. Nothing to fix; *recommended: accept it* (F5.2 (b)).

- **No linked site, no guard on its path.** The benchmark's call `(marea m)` is the generic
  call in both trees, byte for byte the same Go (`IFn_Cast(var.GetRawRoot()).Invoke_O__O`): the
  var's root at the end of the prepare run is a `MultiFn`, neither an `EvalFn` nor an `AOTFn`, so
  `link-plan` never links it and no `linked` guard runs. The methods (`defmethod`'s fns, keyword
  lookups and `*`) and the loop are the same Go in both; `MultiFn`'s Go (`lang/MultiFn.go`) is
  the same but for a temporary's name. The runtime's package `lang` differs only by F5's two
  `Compiler$AOTFn/linked`, `$AOTRestFn/linked` functions and their reflection table entries.
- **The same machine code.** Of the 72 functions in the profile (Go's pprof, `ARBACE_CPUPROFILE`,
  8 s of `multimethod` in each build), the 63 with samples of their own (`MultiFn.invoke`,
  `getFn`, `getMethod`, `Keyword.invoke`, `PersistentArrayMap`'s and `PersistentHashMap`'s
  lookups, `Var.deref`, the `Cast`/`InstanceOf` helpers, `Numbers`, the methods, `runtime`'s)
  disassemble to the same instructions in both (`go tool objdump`, addresses relative to the
  function); the 9 that differ are the start-up chain (`arbace.main`, `load`, `apply`), run once.
  So gc inlined nothing differently on the hot path (no `-gcflags=-m` difference to find).
- **Where it moved.** F5's two functions, early in `lang`'s text, shift the package by 0x520
  bytes: `MultiFn.invoke`, `getMethod`, `getFn`, `Keyword.invoke`, `Var.deref` and
  `ILookup_InstanceOf` go from 32 mod 64 to 0 mod 64, `IFn_Cast` and `AOTFn.invoke` the other
  way. The profiles differ by noise but for `getMethod`'s line `(.deref hierarchy)` (320 → 550
  ms, the same instructions), the kind of cost alignment and branch-predictor aliasing make.
- **The layout perturbed.** The two measurement programs (`.tmp/f5-linking/`'s, the printed
  modules rebuilt by `go build` byte for byte: same sha256), linked again with
  `-ldflags=-funcalign=64` and `=128` (the code unchanged, only where it lies); five interleaved
  rounds of the whole set as in "F5 as built", the machine shared (load 13-34):

| benchmark | before | F5 | | before, align 64 | F5, align 64 | | before, align 128 | F5, align 128 |
|---|---:|---:|---|---:|---:|---|---:|---:|
| `multimethod` | 7.29 | 7.80 | 0.93 | 7.47 | 7.18 | 1.04 | 8.12 | 7.74 | 1.05 |
| `protocol` | 4.90 | 4.71 | 1.04 | 4.61 | 5.11 | 0.90 | 4.69 | 4.80 | 0.98 |
| `closures` | 16.17 | 16.65 | 0.97 | 16.34 | 15.85 | 1.03 | 15.80 | 15.16 | 1.04 |
| `str-build` | 5.97 | 6.16 | 0.97 | 6.10 | 6.15 | 0.99 | 6.08 | 6.28 | 0.97 |
| `fib` | 1.52 | 0.96 | 1.58 | 1.48 | 0.98 | 1.51 | 1.47 | 0.99 | 1.48 |
| `seq-ops` | 39.48 | 30.83 | 1.28 | 39.79 | 30.94 | 1.29 | 39.00 | 31.20 | 1.25 |
| `lazy-walk` | 23.54 | 17.29 | 1.36 | 22.69 | 17.40 | 1.30 | 23.71 | 17.58 | 1.35 |
| geometric mean (19) | 8.70 | 7.88 | 1.10 | 8.58 | 7.83 | 1.10 | 8.59 | 7.85 | 1.10 |

  The same machine code runs `multimethod` in 7.18 to 8.12 ms depending on where the linker put
  it, and F5's 7% loss becomes a 4-5% gain under either alignment, every round (F5, align 64:
  7.18-7.26; before, align 64: 7.47-7.66). `protocol` (and `closures`) swing the same way, by
  the same cause. F5's gains (`fib`, the sequence benchmarks, `destructure`) and the geometric
  mean, 1.10, hold under all three layouts.
- **Not proposed.** Building with `-funcalign=64` is no remedy: it moves other benchmarks
  (`loop-long` 3.74 → 4.06 in both trees) and the program by 2%; layout effects of ±5% on a
  single small benchmark are within what any change to the runtime's text causes. A profile
  (`--pgo`, which orders and inlines by the hot paths) is the tool if they matter, with F6.

Scratch: `.tmp/f5-multimethod/` (the profiles, the rebuilt programs, the rounds `r-*.txt`).

## F6 as built (2026-10-18)

F6 (a) as decided: primitive fns first, then speculative unboxed workers, since `fib` still showed
the gap; on the branch `f6-prim` (from main `80294e9a`), in `arbace.c2j` (`arbace/c2j/emit.ae`,
`main.ae`), the compiled fns' run-time classes (`test/c2j/gen_fns.ae`,
`arbace/lang/go/CompilerAOTFn.ae`) and one rule of j2g (`code.ae`). The analysis is not changed:
`FnMethod.primInterface` still answers nil (EVAL-PLAN Q1), no call site is an `.invokePrim` and
an evaluated fn is no `IFn$LL`; everything below is the back end's, below the analysis, and
answers only what the boxed path answers.

### Step 1: primitive fns' unboxed entries

- **The entry.** A fn method with `^long` or `^double` parameters or return, at most 4
  parameters and not variadic (the JVM's `primInterface`: the signatures `IFn$LL`, `IFn$DD`,
  `IFn$OL`, `IFn$LO` ...), gets `invokePrim(self, long/double/Object...)` returning its return's
  class: the body as the boxed method had it, its primitive parameters taken as they are, its
  return unboxed (`ret-body`). Its boxed `invoke` converts the arguments as before
  (`RT.longCast`, `doubleCast`) and boxes the entry's value as before (`Long.valueOf`,
  `Double.valueOf`), so generic callers, evaluated code and `apply` see the same fn. The code
  class records its entries' signatures (`:prims`).
- **Which code a site expects** is read from the analysis: the fn of the var's last top-level
  `def` in the recorded units (`var-fns`), not the root's `.-fe`, so that the back end decides
  the same when its own namespaces and `arbace.core` run compiled (their roots `AOTFn`s,
  SA164's `compiled-back-end`: the fixpoint's G2) as when they run evaluated (G1); a fn a `def`
  makes inside a `let` (a closure) gets its entries, and its calls stay F5's (the boxed
  `invoke` called directly). The link plan still checks the code class's own `:prims` (and `:specs`).
- **A linked call (F5) of it** binds its arguments first, each once and in order: an argument
  of the parameter's primitive class that is a local or a resolved static call, unboxed (boxed
  for the generic call as `emit` boxes it); a constant, the literal (the pool's object for the
  generic call); anything else boxed and converted by the boxed invoke's own conversion
  (`RT.longCast`) after all are evaluated, where the boxed invoke would convert it. Then under
  the guard the entry is called directly. Its value is boxed as the boxed invoke boxes it, or,
  where the consumer converts the call's value to the return's class (an argument of a
  primitive parameter: `typed-args`; a primitive return: `ret-body`), left unboxed, the generic
  path converting what it gets as before (so a redefinition to a fn of a double, a ratio, a
  string still answers 2, 3, `ClassCastException` as it did).

`fib-prim`'s body becomes one Go function on `int64`s, its recursive calls
`linked.(*A_bench_eval_fib_prim_A).InvokePrim_O_J__J(root, n-1)` under the guard.

### Step 2: speculative workers

- **Which fns.** A fixed-arity fn method without primitive hints (at most 4 parameters, not
  variadic, no recur to its parameters) whose parameters an F3 operation of `Numbers` takes as
  Object operands, and which passes one of them, or such an operation of them, to a call of a
  var (`spec-mask`; a fn whose parameters only meet operations, `arbace.core`'s `+`, would pay
  the dispatch for nothing: measured, `reduce-range` 5% slower with every such fn given one).
- **The worker**, `invokeSpec(self, long n, Object nbox ...)`, takes those parameters as longs,
  each with its box: the caller's object when it had one, else nil and made once when the body
  needs it (`Long.valueOf`, the same box the generic call would have passed), so a parameter
  returned, stored or compared by `identical?` is the caller's object, and one box per
  parameter. In its body an F3 operation on such a parameter takes the long with no test; any
  other use takes the box. Entered from the boxed `invoke` when those arguments are `Long`s
  (else the boxed body, unchanged), and from linked calls under the guard plus the arguments'
  type tests; an argument that is the calling worker's own long parameter, a primitive long, or
  an F3 operation of such parameters (`spec-intrinsic`: its `Long` case computed as a long, not
  boxed; Numbers' own answer otherwise) needs no test. It returns the boxed value, so a failed
  guard needs no deoptimization: there is no unboxed state to hand over (§4 (g)'s risk), the
  generic call is T1's.
- **The guard inlined.** `Compiler$AOTFn/linked` was a call at every site (gc would not inline
  it: the `Cast` it makes may throw). The calls of entries and workers use `linkedFast`, the same
  test written so that j2g binds `if-instance`'s pattern by Go's assertion for the compiled fns'
  classes (`assert-classes`: the value is known non-nil and of the type, so `C_Cast` could
  neither answer nil nor throw), which gc inlines; F5's other sites keep the out-of-line `linked`
  (inlined at all 44,600 sites the program grew by 4%).

### Measured

As F5 was (SA139's way): `test/bench/eval-bench.ae` as `bench.eval` (with
`test/c2j/link_redef.ae` and `test/c2j/prim_fns.ae`) compiled with the runtime's namespaces into
a measurement program by stage 0 (the seed's runtime is main's), with each tree's c2j, j2g and
`bin/build-go`; before: main `80294e9a`; step 1: the unboxed entries alone, as first built (the
guard `linked`, out of line); F6: the branch at `08d5423a` (the final `ea676188` differs by
`var-fns` alone, one site fewer in the program: `fib` 0.51, `fib-prim` 0.36 in a spot check); run compiled
(`(Compiler$AOT/load "bench/eval" ...)`); ms, the best of five interleaved rounds, each the
benchmark's best over at least 1 s and 3 runs; amd64, the machine shared (load 5-20):

| benchmark | before | step 1 | F6 | speed-up |
|---|---:|---:|---:|---:|
| `fib` | 0.98 | 0.98 | 0.53 | 1.85 |
| `fib-prim` | 1.51 | 0.47 | 0.36 | 4.19 |
| `loop-long` | 3.74 | 3.74 | 3.74 | 1.00 |
| `loop-double` | 0.53 | 0.54 | 0.54 | 0.98 |
| `dotimes-array` | 8.98 | 8.96 | 8.96 | 1.00 |
| `reduce-range` | 38.16 | 37.72 | 37.48 | 1.02 |
| `reduce-fn` | 10.97 | 10.91 | 10.94 | 1.00 |
| `vector-conj` | 16.57 | 16.20 | 16.33 | 1.01 |
| `map-assoc-get` | 18.23 | 18.20 | 18.36 | 0.99 |
| `keyword-map` | 6.11 | 6.12 | 6.10 | 1.00 |
| `str-build` | 6.02 | 6.16 | 6.08 | 0.99 |
| `seq-ops` | 30.49 | 31.37 | 31.31 | 0.97 |
| `lazy-walk` | 17.42 | 17.63 | 17.60 | 0.99 |
| `transduce` | 12.69 | 12.99 | 13.01 | 0.98 |
| `into-xform` | 10.92 | 11.33 | 11.23 | 0.97 |
| `protocol` | 4.72 | 4.69 | 4.73 | 1.00 |
| `multimethod` | 7.44 | 7.35 | 7.30 | 1.02 |
| `destructure` | 19.20 | 20.41 | 20.29 | 0.95 |
| `closures` | 15.74 | 15.34 | 14.95 | 1.05 |
| geometric mean | 7.83 | 7.41 | 7.06 | 1.11 |

- `fib-prim` 1.51 → 0.36 (the JVM 0.16): 0.47 with the entries alone, 0.35-0.36 once the guard
  is inlined. `fib` 0.98 → 0.53: with the worker alone 0.88, the guard inlined 0.78-0.82, the
  arguments `(- n 1)` passed unboxed 0.53; what is left is the boxed return (a worker answers an
  Object; fib's values are small, so `Long.valueOf` mostly hits its cache) and the call itself
  (a root read, the guard, a method call per call: gc inlines no recursion, where HotSpot
  inlines a few levels).
- The rest is unchanged within the noise and layout (F5.2): the runtime has 78 unboxed entries
  (`arbace.math`'s 43, `j2g.zlib`'s 8, `repr.go`'s text, `arbace.core`'s few) and 51 workers
  (mostly `repr.go`'s printer), none on these benchmarks' paths. `destructure` and `seq-ops`
  moved by as much in step 1, whose code on their paths is the same.
- **Variants measured and not built** (rounds of their own): the worker for every fn whose
  parameters meet an F3 operation (420 linked worker calls, `reduce-range` 40.0 against 38.0);
  j2g's assertion binding for every translated class (F6.1): `fib` 0.54, `fib-prim` 0.35 as
  built, but `loop-long` 3.74 → 4.37 with byte-identical code (layout) and 3-5% on four sequence
  benchmarks, the program +0.6%; and with `linked` itself inlined everywhere, +3.8% (185.9 MB).
- **Size.** The measurement program 177,447,903 → 178,880,946 bytes (+0.8%).

### Checks

- `test/c2j/prim_fns.ae` (new; 72 checks), run in the measurement program compiled
  (`Compiler$AOT/load`) and evaluated (`ARBACE_NO_AOT`): the same lines, all `ok`, and the same
  in main's program (the 52 checks of the primitive fns as first written): hints honored (a double, a ratio,
  an int, a bigint in and out of range, a string, nil as a `^long` argument; returns of a ratio
  and of a bigint), overflow (thrown by `inc` and `*`, wrapped by `unchecked-inc`, promoted by
  `inc'` and `*'`), `^double` fns, `IFn$LO`- and `$OL`-shaped fns, 4 and 5 parameters, a
  multi-arity fn with primitive arities and a variadic one, recur on primitive parameters, a
  primitive closure, callers in primitive and boxed contexts, boxing identity; a primitive fn
  redefined by `with-redefs` (to evaluated fns of a long, a double, a ratio, a string; to
  another compiled primitive fn), `alter-var-root` and a `defn` evaluated at run time, seen by
  its compiled callers, `fib-prim`'s own recursion included; the workers: a parameter returned
  is the caller's object, one box per parameter, a fresh argument, overflow, promotion, ratios,
  doubles, bigints, ints, nil and strings through the boxed path, two parameters, a closure over
  a parameter, a worker calling a redefined fn, `fib` redefined at run time.
- `test/c2j/link_redef.ae`: all 20 `ok`, compiled and evaluated.
- **`bin/diffcheck`** (in the essential gate): 12,731 of 12,754 cases agree, 23 disagree, all
  recorded, 0 new.
- **The essential gate** passed on `HEAD` before this record (the smoke test; Clojure's suite,
  20,506 of 20,520, no regressions; the oracle, 53 known mismatches, 0 new; diffcheck as above).
  **`bin/arbace-go --fixpoint`** holds: G1 = G2 = G3 byte for byte (120,578,208 bytes, sha256
  `5a6a8f30...`), stage 0 different from G1 as expected until a re-seed (the back end, j2g and
  `CompilerAOTFn.ae` are the program's). A first version decided from the root (`.-fe` of an
  evaluated fn, nothing for a compiled one) gave G1 ≠ G2 (G2 = G3): the reason for `var-fns`.
  The shipped executable 119,660,704 → 120,578,208 bytes (+0.8%). No re-seed here.

### Proposals (the user's to decide)

- **F6.1 j2g's assertion binding for every class.** `if-instance` binds by Go's assertion for
  the compiled fns' classes only (as built); for every translated class that is no interface
  it is as exact and lets gc inline more (measured: the geometric mean 7.33 against 7.45 for the
  rest equal, `protocol` and `destructure` 5-6% faster, but `loop-long` 16% and four sequence
  benchmarks 3-5% slower on the same machine code, layout; the program +0.6%). (a) As built,
  the two classes; (b) every class. *Recommended: (a)*; (b) with a profile (`--pgo`) that
  settles the layout, if it is ever measured so.
- **F6.2 A worker's unboxed return.** A worker answers a box (`fib`'s 0.53 against
  `fib-prim`'s 0.36 is mostly that, and the type tests on it); answering a long needs a second
  result for the cases that are no long (a promotion, a redefined callee's value): a long and a
  box in a holder the caller passes, or a sentinel with the object kept per thread. (a) Left
  as built; (b) a two-result worker for fns whose every return is an F3 operation, a long
  parameter or a worker's call. *Recommended: (a)* until a workload beyond `fib` asks for it;
  the call itself (a root read, the guard, a method call per call) is what is left of the gap to
  the JVM's 0.16 for both, which inlining recursion would take, and gc does not.
- **F6.3 Workers' reach.** (a) As built: a worker only for a fn passing a parameter, or an
  operation of one, to a call of a var (51 in the runtime); (b) also fns whose parameters only
  meet operations (measured: `reduce-range` 5% slower, nothing faster); (c) also the
  parameters' loops (a recur to them, with a fallback to the boxed method at the recur when a
  new value is no `Long`). *Recommended: (a)*; (c) when a benchmark of loops over boxed
  parameters asks for it.

Scratch: `.tmp/f6-prim/` (`bench.bash` and `rounds.bash`, the programs `b-*`, the rounds
`rounds*/` and `r-*.txt`, `table-final.md`).

## F7 as built (2026-10-18)

F7 (a) as decided, the test of F1 (a): on the branch `f7-defnode` (from main `7fb90fc8`), a
`defnode` form for the five kinds the proposal named, each defined once and generating both the
evaluator's `Code` nodes and c2j's emitter, which replace the hand-written pairs of those kinds.

### The design

- **Where it lives.** `arbace/c2j/node.ae` (`arbace.c2j.node`): the `defnode` macro and the two
  readings of a definition. The definitions themselves are in `arbace/c2j/emit.ae`, where the
  emitters were (a section "node kinds defined once"). The emitter's reading is the macro's
  expansion: `emit-KIND`, an ordinary fn of the back end, compiled with it. The evaluator's
  reading is `node/code-forms`, run by `test/c2j/gen_nodes.ae`, which writes
  `arbace/lang/go/CompilerNodes.ae` (a Go-build variant of `Compiler`, as `CompilerOps.ae` and
  `CompilerAOTFn.ae` were): the kinds' `Code` classes and `CodeNodes/compile`, their
  construction, which `CodeCompiler.compile` asks first. The evaluator half has to be generated
  text: the runtime's Java forms are read by j2g, not evaluated, so a macro cannot run inside
  `Compiler.ae`. The back end is the right owner: its namespace is evaluated (macros run), and
  it already held the templates the two halves now share (F3's fast paths). `gen_nodes.ae`
  replaces `test/j2g/eval_ops.ae` (its method listing kept), and `CompilerNodes.ae`
  `CompilerOps.ae`.
- **A definition** is a binding vector and a template. The bindings are the node's children
  (`child`, `children` with the largest count the evaluator makes a class for) and its statics
  (`static T form`: made when the node is, by the same form in both halves: a Java form in the
  evaluator's construction, a Clojure form in the emitter, over the node's fields; it sees a node
  through `Compiler$AST`, the analysis' public face, since `Compiler$Expr` is not public:
  `AST/literal` was added for it). The template is a Java form, the node's value in channel
  `ch`, with a few staged forms: `(run CH x)`, `(static-if s a b)`, `(static-case ch ...)`,
  `(call-site line [s init ...] body)`, `(eval-only ...)`, `run-all`/`run-args` with `splice`
  and `array-of`, `box-as`, `call-static`, `get-place`/`set-place`, `let`. The emitter quotes
  the template (children become `emit`/`emit-unboxed` of them, statics `lift`ed into literals or
  pool constants); the evaluator runs it (children become `(.run x f)`, `.runLong` ..., statics
  fields; a `static-if` on a static boolean, `many?` or a place's clearing becomes one class per
  case, Truffle's specializations; the channels become `run`, `runLong`, `runDouble`, `runBool`).
  This works without translating anything because both halves are Java forms: the evaluator is
  written in them and c2j emits them, so the template is literally the code of both.
- **The five kinds** (66 lines with their docstrings):

| kind | definition | the evaluator's classes | the emitter's own |
|---|---|---|---|
| `const` (8 literal Exprs) | `v`, its long and double statics; `(static-case ch :o v :j (static-if num lv ...) ...)` | `CodeConst`, `CodeConstNum` | `lift` (Java forms' spelling of nil, strings, booleans, empty collections) |
| `local` | `(static-if (clears? at) (let [t (get-place at :o)] (set-place at nil) t) (get-place at ch))` | 8 (`CodeLocal`, `CodeLocalClear`, `CodeLocalLong` ...): the template over `eval-places`, a table of the frame's places | `local-place` (fields, `this`, F6's workers' boxes, locals) |
| `if` | `(if (static-if z (run :z test) (run :o test)) (run ch thn) (run ch els))` | `CodeIf`, `CodeIfZ` | none |
| `static-call` | the arguments bound in order, the line, then `(box-as ch m (call-static m as))` | `CodeOp0`-`3`: a switch per arity over the 579 listed methods (a static of finite domain, enumerated: "the trick") | `direct-static?` (which methods), `typed-args` (conversions) |
| `invoke` | `(call-site line [g (cast IFn (run :o fx)) as (run-all :o xs)] (eval-only (when clear (CodeRun/clearThis f))) (static-if (many? xs) (.applyTo ...) (.invoke g (splice as))))` | `CodeInvoke0`-`20`, `CodeInvokeN` | `invoke-linked` (protocol calls, F5/F6's linked calls), tried first |

`call-static` is one builder for both halves: `numbers-fast`, F3's fast paths, now called by the
generator too, so the evaluator's calls of `Numbers`' boxed operations got them by construction.

### Checks

- **The emitted code.** The shipped namespaces' tree compiled by main's c2j and by the branch's,
  on the same executable: the same byte for byte but for c2j's own namespaces (their source
  changed) and the constant pools of five namespaces of j2g (`dyn`, `dyncf`, `embed`, `main`,
  `out`): a quoted form inside a macro that copies its body (`for`'s chunked and plain paths)
  was one constant object for both copies, and is now one per copy, as the evaluator always had
  it. The shared static showed the halves held different constants: c2j emitted the analyzer's
  form (`ConstantExpr.v`), the evaluator `Evaluator/constant` of it (rebuilt as the JVM's
  `emitValue` makes it), each copy its own; they are now the evaluator's in both (F7.4). The
  benchmarks' and the F5/F6 corpora's code (`bench.eval`, `prim_fns.ae`, `link_redef.ae`, and a
  corpus of the five kinds' shapes) is the same byte for byte.
- **The evaluator** changes by what the shared definitions brought: F3's fast paths in
  `CodeOp1`/`CodeOp2`, direct `invoke` for 9 to 20 arguments (was `applyTo` past 8), one class
  per arity, `if` and constant variants without the run-time flags, `RT.afterLoad` listed (the
  committed `CompilerOps.ae` predated it), booleans boxed by `Boolean/valueOf`.
- `bin/diffcheck` (in the essential gate): 12,731 of 12,754 cases agree, 23 disagree, all
  recorded, 0 new, 0 now passing; the essential gate passed (the smoke test; Clojure's suite
  20,506 of 20,520, no regressions; the oracle, 53 known mismatches, 0 new; goi's 266 programs).
  `bin/arbace-go --fixpoint`: G1 = G2 = G3 byte for byte (132,059,296 bytes, sha256
  `77c5703f...`), stage 0 different from G1 until a re-seed.
- One bug found on the way, by the first build: the static call's domain index asked
  `CodeRun/op` of an unresolved method (nil): the evaluator's NullPointerException in the
  closure compiler, at the compile of the first reflective static call.

### Measured

As F6 (SA139's way, `.tmp/f7-defnode/bench.bash`): `test/bench/eval-bench.ae` as `bench.eval`
with the F5/F6 corpora compiled with the runtime's namespaces into a measurement program by one
executable (the branch's), with each tree's c2j, j2g and `bin/build-go`; before: main
`7fb90fc8`, after: the branch; ms, the best of five interleaved rounds (two sets, the same to
1-2%), amd64, the machine shared (load 6-19). Compiled (`Compiler$AOT/load`), and evaluated
(`load-file` of the source: the closure compiler's `Code` nodes), the best of three rounds:

| benchmark | compiled before | after | | evaluated before | after | speed-up |
|---|---:|---:|---|---:|---:|---:|
| `fib` | 0.56 | 0.59 | 0.95 | 13.88 | 7.66 | 1.81 |
| `fib-prim` | 0.35 | 0.35 | 1.00 | 8.90 | 8.46 | 1.05 |
| `loop-long` | 4.37 | 4.37 | 1.00 | 71.31 | 66.34 | 1.07 |
| `loop-double` | 0.53 | 0.53 | 1.00 | 13.63 | 11.94 | 1.14 |
| `dotimes-array` | 9.07 | 9.03 | 1.00 | 24.04 | 23.38 | 1.03 |
| `reduce-range` | 37.81 | 37.66 | 1.00 | 37.87 | 38.15 | 0.99 |
| `reduce-fn` | 11.53 | 11.08 | 1.04 | 42.66 | 29.89 | 1.43 |
| `vector-conj` | 16.95 | 16.61 | 1.02 | 26.36 | 26.48 | 1.00 |
| `map-assoc-get` | 18.49 | 18.42 | 1.00 | 23.87 | 22.68 | 1.05 |
| `keyword-map` | 7.15 | 6.23 | 1.15 | 25.86 | 18.26 | 1.42 |
| `str-build` | 6.01 | 6.01 | 1.00 | 11.90 | 11.79 | 1.01 |
| `seq-ops` | 32.29 | 32.39 | 1.00 | 34.25 | 35.03 | 0.98 |
| `lazy-walk` | 17.96 | 17.75 | 1.01 | 25.22 | 22.89 | 1.10 |
| `transduce` | 14.14 | 13.17 | 1.07 | 21.42 | 17.70 | 1.21 |
| `into-xform` | 11.82 | 11.17 | 1.06 | 11.91 | 11.09 | 1.07 |
| `protocol` | 4.86 | 4.76 | 1.02 | 131.21 | 123.14 | 1.07 |
| `multimethod` | 7.73 | 7.59 | 1.02 | 20.13 | 18.69 | 1.08 |
| `destructure` | 20.28 | 20.07 | 1.01 | 68.03 | 56.65 | 1.20 |
| `closures` | 15.85 | 17.07 | 0.93 | 54.65 | 47.08 | 1.16 |
| geometric mean | 7.34 | 7.24 | 1.01 | 27.31 | 24.03 | 1.14 |

Compiled: the benchmarks' and `arbace.core`'s Java forms and Go are the same in both programs
(the Go differs in the runtime's `lang` package, j2g's temporaries' numbering and c2j's own
packages), so `fib`'s 5% and `closures`' 7% (both sets) and `keyword-map`'s 15% the other way
are layout, as in F5.2. Evaluated: 14% faster in the geometric mean, `fib` 1.8 times (F3's fast
paths), nothing slower. The measurement program 194,981,934 → 196,764,393 bytes (+0.9%: the
switch cases' fast paths, 22 invoke classes, `arbace.c2j.node` compiled).

### Lines

| | before | after |
|---|---:|---:|
| the five kinds, both halves, by hand: their `Code` classes and construction (`Compiler.ae`), `eval_ops.ae`'s templates, their emitters (`emit.ae`) | about 370 | |
| the definitions (`defnode`s) | | 66 |
| each half's own parts the definitions name (`lift`, `prim-chan`, `local-place`, `direct-static?`, `call-static`; `eval-places`) | | about 90 |
| the machinery: `arbace.c2j.node` but its place table (the macro, the evaluator's reading), and `gen_nodes.ae`'s printer | | about 610 |

In all `git diff --numstat` counts 961 lines added and 489 removed by hand (generated files
apart: `CompilerOps.ae` 1,670 lines out, `CompilerNodes.ae` 7,230 in, pretty-printed): 472
more. The machinery is a fixed cost; per kind a definition is 8-16 lines where the pair was 30
to 110. It would pay back after some 10 more kinds of the size of these.

### What was awkward

- **Byte-identical emitted code** (the cheapest proof of the emitter half) constrains the
  expansion: values made in the hand-written order (the pool's constants and the temporaries are
  numbered in order of creation), laziness where the hand-written code was lazy (`invoke`'s
  arguments), and `run-args` delayed so that F3's temporaries are named before the operands are
  emitted. A definition free of that history would not need it.
- **Locals** are the binding-time boundary itself: where a local lives is each half's own (the
  frame's slots, prims, closed-over arrays and `Dyn` fields; Java locals, fields, `this`, F6's
  boxes). The definition shares four lines (read, or read and clear); the rest is two tables,
  as long as the classes were. The choice of a place stays hand-written in `CodeCompiler.local`
  and `localUse`, because fns' closed-over values (`fetchers`) need it without an Expr.
- **What only one half does** needs escapes: `eval-static` and `eval-only` (the evaluator's
  frame line and tail-call clearing), `:emit-first` (c2j's linked and protocol calls, F5/F6:
  speculation the evaluator does not have, still checked by F2 only), per-half statics (which
  static methods are direct: every public one for c2j, a finite listed domain for the evaluator,
  which cannot call a method it has no case for).
- **The static forms** must be the common subset of Clojure and Java forms, and see the node
  through `Compiler$AST` (`Expr` is not public): `AST/literal`.
- **The generated file** has to be regenerated by hand after a definition changes, on an
  executable with the checkout's `Numbers`, `RT`, `Util`, `Math` (the committed `CompilerOps.ae`
  had gone stale that way: `RT.afterLoad`); nothing checks it (F7.3).

### The remaining kinds

- **Expressions without storage or control** (about 15): var reads, `the-var`, keyword
  invokes, `with-meta`, the collection literals, `instance?`, `throw`, monitors, `str` concat,
  static fields, `import`, `new`, instance fields and methods. They go as `if` and `invoke` did,
  10-15 lines each, the hand-written pairs some 450 lines. Two would change a half: a var read
  (c2j decides dynamic or not when compiling, SA138; the evaluator at each read: one definition
  makes the evaluator decide it when the node is made, as the compiled code does), and the
  instance calls (the evaluator calls through member tables' invokers, not directly: a per-half
  `call-method` like `call-static`'s domain).
- **Storage and control** (`let`, `loop`, `recur`, `letfn`, `fn`, `reify`, `deftype`, `try`,
  `case`, `def`, `set!`): the evaluator's frames, recur flag and jump-free loops against Java
  forms' locals, `loop`/`recur` and exceptions. Like locals, each would be a few shared lines
  over per-half primitives, and the shapes differ for good reasons (the survey's §4 (f) risk):
  little gained by construction.

### Recommendation

- **F1: (a) for the expression kinds, (c) for storage and control.** The evidence: the
  definitions made the two halves of five kinds one text, checked as equal by diffcheck,
  the suite and the oracle; the emitted code stayed byte for byte (but for the constants'
  identity the shared static exposed); the evaluator gained what one half had (F3's fast paths:
  evaluated `fib` 1.8 times, the geometric mean 14%) with nothing slower, and a divergence
  between the halves came to light (the constants). Against it: the machinery costs more lines
  than five kinds save, locals did not stage, and what is half-specific (speculation, frames)
  stays hand-written behind escapes, with F2 as its only check. Staging pays where the halves
  compute the same thing in the same shape; F2 remains the check for the rest.
- **F8: (b), the Java forms' pair kept as two implementations with F2's check.** F7 worked
  because the evaluator and c2j's output are the same language (Java forms), so a template is
  the code of both. In the Java forms' pair the interpreter (`CF`, Java forms) and j2g's output
  (Go forms) are not: a shared template would need j2g to translate it, which makes j2g's code
  translation derived from the interpreter through j2g itself, the second projection's shape,
  a design study rather than a defnode (F8 (a) stays later, as decided).

### Proposals (the user's to decide)

- **F7.1 Merge.** (a) Merge the branch (the five kinds staged, the evaluator's gains), and
  convert the expression kinds above as their code is touched; (b) merge and convert them all
  now; (c) keep the branch as the experiment, main's pairs hand-written. *Recommended: (a)*.
- **F7.2 The evaluator's changes that came with it** (F3's fast paths, direct `invoke` to 20
  arguments, `RT.afterLoad` listed, `CompilerOps.ae`'s generator replaced). (a) Kept with the
  merge; (b) the evaluator's half restricted to what it did before. *Recommended: (a)*: they are
  the point of one definition.
- **F7.3 A check that `CompilerNodes.ae` is current.** (a) `gen_nodes.ae` run by the essential
  gate on the gate's executable (seconds) and compared with the committed file; (b) by hand, as
  `CompilerOps.ae` was. *Recommended: (a)*.
- **F7.4 Constants' identity.** (a) As built: each constant node's value the evaluator's
  (`Evaluator/constant`), so a quoted form a macro copies is one object per copy, compiled and
  evaluated alike; (b) the analyzer's form shared by the copies in both halves (the evaluator
  would no longer rebuild constants as `emitValue` makes them). *Recommended: (a)*.

Scratch: `.tmp/f7-defnode/` (`bench.bash`, `rounds.bash`, `rounds-eval.bash`, `table.py`,
`emitcmp.bash` and `tree.bash` for the emitted code's comparison, the rounds `r-*`, `re-*`,
`r1/`).
