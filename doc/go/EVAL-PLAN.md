# The evaluator: plan (B1a step 5)

Status: proposal (2026-10-09, c2g phase 2B); the user decided Q1-Q7 as recommended (§9), and
step 5 built it ([EVAL-NOTES.md](EVAL-NOTES.md), whose "Deviations from EVAL-PLAN" list where
the evaluator differs from this plan; its amendments M1-M8, X1-X4 and S1-S8, accepted
2026-10-09, are folded into C2G-SPEC and noted below where they touch this plan). B1a step 5
([B1-PLAN.md](B1-PLAN.md), decision D2): Arbace on Go evaluates the `Expr` trees of its
translated `Compiler` instead of emitting bytecode. This document fixes the evaluator's design
over the translated tree, records what phase 2B built and proved, estimates the rest, and asks
the questions the user should decide (§9).

## 1. Where things stand (phase 2B)

- **`Compiler` translates as a whole.** `bin/c2g --root 'arbace.lang.Compiler$*'` translates 94
  of `Compiler`'s 96 classes (its analyzer: `analyze`, macroexpansion, resolution, every `Expr`
  class and its parser, `eval`, `load`), about 17,400 lines of Go; the two left are the back
  end's (`Compiler$2`, a `ClassWriter`; `SourceDebugExtensionAttribute`). The variant
  (`arbace/lang/go/Compiler.clj`, 356 lines) erases ASM (`(c2g/erase "arbace/asm/")`: every
  value of an ASM type is nil, its computation skipped, its stores dropped; members whose
  signatures name ASM do not exist in Go, so the `emit` methods disappear), replaces
  `ObjExpr.compile` (no class is generated), `getCompiledClass`, `ObjExpr.eval` (a `fn*` is an
  `EvalFn`), `StaticInvokeExpr.parse` (no direct linking), the array descriptor of
  `HostExpr` and the stream code of `QualifiedMethodExpr` (loops), and makes AOT compilation
  throw. With it, small variants of `RT` (`makeClassLoader`, `baseLoader`, `classForName` over
  jrt's registry) and `Reflector` (no method handle for `canAccess`).
- **What remains missing in `Compiler`** (operations that throw `UnsupportedOperationException`
  when reached): `loadFile` and the `eval-file` name in `eval` (`java.io.File`, files: phase 2C
  or jrt), `NewInstanceExpr.compileStub` (deftype's stub class: step 5, on `Dyn`), and
  `StaticMethodExpr.isBoxedMath` (annotations' values; only with `:warn-on-boxed`).
- **The evaluator's interface** is in the variant, as class forms translated like the rest:
  `Compiler$Frame` (the slots of one invocation), `Compiler$EvalFn extends RestFn` (a `fn*`
  value: its `FnExpr` and closed-over values), `Compiler$Evaluator` (the walk:
  `eval(Expr, Frame)`, `invokeFn`), `Compiler$Dyn` (classes made at run time, natives c2g
  writes).
- **The proof**, `bin/c2g-evalproof` (`test/c2g/eval/`): a Go program reads forms with the
  translated `LispReader`, analyzes them with the translated `Compiler`, prints their `Expr`
  trees from Go (a type switch over the translated `Expr` types) and evaluates them, by
  `Expr.eval` and by `Compiler.eval` through `EvalFn`; it builds and gives the recorded output on
  linux/amd64 and, under `qemu-aarch64`, linux/arm64 (25 MB static executables). Excerpt:

  ```
  form   (let* [x 1 y 2] (if x (arbace.lang.Numbers/add x y) nil))
  tree   (Invoke (Fn arbace.core$fn__9 closes [] (Method [] (Body (Let [x#1 (Number 1) y#2 (Number 2)]
           (Body (If (Local x #1) (StaticMethod arbace.lang.Numbers add {public static long
           arbace.lang.Numbers.add(long,long)} (Local x #1) (Local y #2)) (Nil))))))) )
  eval   3
  form   (loop* [i 0 acc 0] (if (arbace.lang.Numbers/lt i 5) (recur ...) acc))      eval 10
  form   (((fn* [x] (fn* [y] (arbace.lang.Numbers/add x y))) 40) 2)                  eval 42
  form   (def twice (fn* [&form &env x] (arbace.lang.RT/list (quote ...) x 2)))
  form   (. (var twice) (setMacro))
  form   (twice 21)       tree (StaticMethod arbace.lang.Numbers multiply ...)       eval 42
  form   ((fn* f [n] (if (... lte n 1) 1 (... multiply n (f (... dec n))))) 10)       eval 3628800
  form   ((fn* [& xs] xs) 1 2 3)                                                     eval (1 2 3)
  form   (Math/abs -3)    tree (StaticMethod java.lang.Math abs {... abs(long)} ...)  eval 3
  ```

  and a class made at run time (`user.Foo` implementing `Counted` and `ILookup` with an evaluated
  fn as `count`): `RT.count` gives 42 through Go's interface call, `instance?` of
  `IPersistentVector` is false although Go's assertion succeeds, a cast to `Seqable` throws
  `ClassCastException`, an unset method throws `AbstractMethodError`, and evaluated forms reach
  it by reflection (`(. foo count)`, `(. foo -a)`); and an evaluated fn adapted to
  `java.util.function.Function` by `jrt.AdaptFn` (`FromFn`).

## 2. The evaluator over the translated tree

The design follows what Clojure's `Compiler` already decides; the evaluator adds only the
run-time side the bytecode had.

### 2.1 Frames and locals

- The analyzer numbers locals (`LocalBinding.idx`, `NEXT_LOCAL_NUM`, `ObjMethod.maxLocal`) as
  JVM slots: no two live locals share a number within a method. A `Frame` is `Object[maxLocal +
  2]` per invocation of a `FnMethod`, indexed by `idx`; slot 0 is the fn itself (`this`, the
  named fn's own name) unless the fn is direct (`canBeDirect`, where the analyzer shifts the
  parameters down by one).
- A local is in the frame when the running method declares it (`ObjMethod.locals`), otherwise
  it is closed over: `ObjExpr.closes` lists the fn's closed-over bindings, and `EvalFn` holds
  their values in that order, captured from the creating frame when the `FnExpr` is evaluated
  (an inner fn's capture of an outer closed-over local reads the outer fn's value: the
  analyzer's `closeOver` adds it to every fn in between).
- Primitive locals (`^long`, `^double`, loop locals with primitive inits) hold boxed values;
  the analyzer has chosen the primitive overloads, and the invokers convert (§2.6).

### 2.2 Functions

- `fn*` evaluates to `Compiler$EvalFn` (`extends RestFn`, required arity 0): every `invoke`
  arrives at `doInvoke` with its arguments as a seq; `Evaluator.invokeFn` picks the
  `FnMethod` of the arity (fixed first, then the variadic one), binds parameters by their
  `idx`, the rest parameter as an `ArraySeq`, and runs the body. Translated code calls an
  evaluated fn with Go's interface call `(.Invoke_O__O f x)`, through `RestFn`'s dispatch.
- Metadata (`MetaExpr` over a `FnExpr`): `AFunction.withMeta`, as on the JVM.
- `ArityException` names the fn by its analyzed name.
- **As built** (amendment M2, accepted 2026-10-09; C2G-SPEC §10.2): each `FnExpr` has a class
  made at run time, a subclass of `EvalFn` named as the JVM names the fn's class
  (`arbace.core$map`), which `getClass` answers through `EvalFn`'s field `c2g$class`; `EvalFn`
  binds the arguments from `RestFn`'s seq, the rest parameter unrealized (EVAL-NOTES.md,
  "Functions").
- Step 7 replaces `doInvoke`'s seq with per-arity `invoke` methods (Q3) and the tree walk with
  closures.

### 2.3 Control

- `if`, `do`, `let` (`LetExpr`), `loop` (`LetExpr.isLoop`) and `recur` are cases of
  `Evaluator.eval` (done in phase 2B). `recur` assigns the loop's (or the fn method's
  parameter) slots and returns a sentinel (`Evaluator.RECUR`) that the loop or `invokeFn`
  restarts on; the analyzer guarantees tail position, so the sentinel cannot escape into a
  value. No exception and no Go `goto` is needed.
- `try`/`catch`/`finally` (`TryExpr`): a Java `try` in the evaluator's class forms (c2g makes
  it Go's panics and recovers), each `catch` matching by `Class.isInstance`, its binding stored
  in its slot; `finally` as Java's. `throw` (`ThrowExpr`) throws.
- `letfn*` (`LetFnExpr`): create the `EvalFn`s first with their closed-over values pending,
  store them in their slots, then fill the closed values (they may close over each other), as
  the bytecode's `emitLetFnInits` does.
- `case*` (`CaseExpr`): the analyzer's tests, shift and mask, `int`/hash/identity modes,
  looked up in the `thens` map as `CaseExpr.emit` does (a direct translation of its switch).
- `monitor-enter`/`monitor-exit`: `jrt.MonitorEnter`/`Exit` through `locking` in class forms
  (the analyzer pairs them in `try`/`finally`).

### 2.4 Vars, `def`, `set!`, constants

- `DefExpr`, `VarExpr`, `TheVarExpr`, `AssignExpr` with a var target: their own `eval` works
  when their sub-expressions have no locals; the evaluator's cases evaluate `init` and the
  meta map in the frame and then do what `eval` does (`bindRoot`, `setMeta`, `setDynamic`).
- `set!` of a local (`deftype` mutable fields, the only assignable locals) through the `Dyn`
  object's field (§2.5); of a field or static field through reflection (`Reflector`).
- Constants, keywords, numbers, strings, `nil`, booleans, `quote`, `var`: their `eval` (done).
  Collection literals (`VectorExpr`, `MapExpr`, `SetExpr`): evaluate the elements in the frame,
  then build as their `eval` does.
- `KeywordInvokeExpr`: `RT.get`-like, `Keyword.invoke`; `InstanceOfExpr`: `Class.isInstance`.

### 2.5 deftype, defrecord, reify: `Dyn`

- `NewInstanceExpr.build` (deftype, defrecord through deftype) and `reify` keep the analyzer's
  parse; `compileStub` (the stub class giving `this` a type for the method bodies) becomes
  `Compiler$Dyn.defineClass(name, interfaces, fields)`: a `jrt.Class` made at run time
  (`jrt.DefineDynamic`, which a deftype evaluated again may replace), registered for
  `Class.forName`, with its fields as reflection's `FieldInfo`. Its methods are
  `NewInstanceMethod`s whose bodies are evaluated with `this` in slot 0 and the fields read
  through `Compiler$Dyn.getField` (the analyzer turns field references into
  `LocalBindingExpr`s of the fields, which the evaluator maps to field indexes); each becomes
  an `EvalFn`-like method object set by `Compiler$Dyn.setMethod(class, name, params, ret,
  impl)`. `deftype` constructors (`NewExpr` of a run-time class) call `newInstance`.
- c2g's Go type `Dyn` has a method for every method of every interface of the closed world (76
  interfaces, 252 methods in the proof's world) and every marker, so translated code calls a
  run-time object exactly as it calls a translated one; a method dispatches to its slot's fn,
  else to the interface's default method, else throws `AbstractMethodError`. `instance?` and
  casts of interfaces add the nominal check (`jrt.Dynamic.DynImplements`) after Go's assertion.
- Covariant overrides and bridges: `gatherMethods`' `covariants` give the evaluator every
  signature a method answers to; it sets each (`setMethod` per descriptor).
- Protocols: `defprotocol`'s interface is made at run time too (`gen-interface` is
  `defineClass` of an interface kind, Q5); inline protocol implementations are methods in the
  `Dyn` class's member table, called by reflection (`Reflector`), and `extend` works on
  `Class` objects as on the JVM.
- Record fields: `defrecord`'s `ILookup`, `IPersistentMap` and the rest are the evaluated
  methods `defrecord` writes, as on the JVM.
- **`proxy`** (amendments X1-X3, accepted 2026-10-09; the user's decision reversing D6 for it;
  C2G-SPEC §5.12, §10.4): a proxy of `Object` is a `Dyn`; a proxy of a class of a fixed list
  (`Writer`, `Reader`, `PushbackReader`, `InputStream`, `OutputStream`, `APersistentMap`, jrt's
  `ThreadLocal`) is a `DynSub_C`, which c2g writes ahead of time; the namespace variant
  `core_proxy.clj` replaces only `generate-proxy`. A class made at run time that defines a
  method twice throws `ClassFormatError`, as on the JVM.

### 2.6 Interop

- Hinted calls (`StaticMethodExpr`, `InstanceMethodExpr` with a `method`), fields
  (`StaticFieldExpr`, `InstanceFieldExpr`) and constructors (`NewExpr`): their `java.lang
  .reflect` objects come from jrt's member tables at analysis; evaluation calls
  `Reflector.invokeMatchingMethod` as their `eval` does (done for the two method kinds), which
  converts arguments by `boxArgs` and calls the table's invoker. Step 7 may call the invoker
  directly with Go values (§10.1).
- Un-hinted calls: `Reflector` at run time, as on the JVM.
- Functional interfaces: `Reflector.boxArg` adapts an `IFn` with `jrt.AdaptFn`, which is the
  interface's `ClassInfo.FromFn` that c2g now generates (phase 2B, §7 of the notes); inside
  class bodies the analyzer's `:fi-adapter` is translated.
- What the REPL can name is the closed world: every public member of every class built in must
  be translated, not only what the runtime reaches, since reflection reaches it (Q2).

### 2.7 Loading `arbace.core` from source

- `RT`'s initialization loads `arbace/core.clj` and the REPL namespaces through `RT.load`
  (`Compiler.load` over a reader of the source): the sources are embedded in the executable
  (`//go:embed`, jrt's host `Resource`) and read through `RT.resourceAsStream`'s variant
  (phase 2A/C's `RT` work). `Compiler.load` itself is translated unchanged (its class loader
  binding is nil).
- The order of work: `arbace.core`'s first forms (`ns`, `in-ns`, `def`, `fn*`, the macros
  `defn`, `let`, `fn`, `loop`) need §2.1-2.4 and `try`; `defprotocol`/`deftype` (core
  `reduce`, `print-method`'s multimethods, `IKVReduce`) need §2.5; `gen-interface` is used
  by `defprotocol` (Q5).
- `*compiler-options*`, direct linking: off (the variant); `:static` fns are ordinary fns.
- **As built** (amendments M1, M3, M5, accepted 2026-10-09; C2G-SPEC §10.3, §4.1): `bin/c2g
  --program` embeds the namespaces' sources with the Go build's namespace variants applied
  (`arbace/lang/go/ns/`); `RT.load` reads them, then the directories of `ARBACE_PATH`; the
  classes the namespaces name outside the world are cut classes whose members throw.
- **Since step 6** (amendment U1, accepted 2026-10-09; C2G-SPEC §10.3, EXEC-NOTES.md): the
  embedded namespaces are analyzed at build time; `RT.load` replays them from the program's
  image (the `Expr` trees and the events of their analysis), skipping reading and analysis.

### 2.8 Recursion depth and errors

- C2G-SPEC §10.5: Go's stack overflow is fatal, so `invokeFn` counts depth per thread (a field
  of the frame chain, or a jrt goroutine-local counter) and throws `StackOverflowError` beyond a
  limit (Q4).
- Errors: `CompilerException` with phase `:execution` around invokes and interop, as the
  bytecode's line tables give it on the JVM (done for invoke and the method calls). Stack
  traces show the evaluator's Go frames, not the Clojure fns (Q6).
- **As built** (EVAL-NOTES.md, deviation 5; amendment M6, accepted 2026-10-09, C2G-SPEC §10.7):
  exceptions are not wrapped in `CompilerException` inside fns, as compiled code does not wrap
  them; stack traces show the evaluated Clojure frames (Q6's (a)), which jrt's frame mapping
  puts in place of the evaluator's Go frames.

## 3. Primitive fns

`(fn [^long x] ...)` makes the analyzer pick a primitive interface (`IFn$LL`) for the fn and
`(.invokePrim ...)` calls for its callers (`InvokeExpr.parse`), which an `EvalFn` cannot answer:
Go has no type for each of the 322 interfaces made at run time, and `Dyn` covers only the
interfaces the world uses. Proposed: the Go build's variant makes `FnMethod.primInterface`
return nil (no primitive fns: callers call `invoke`), and `invokeFn` converts a `^long` or
`^double` parameter with `RT.longCast`/`doubleCast` on entry and a primitive return on exit, so
the arithmetic stays the same; `IFn$LL` and the rest stay in the closed world for translated
code (Q1).

## 4. What the evaluator needs from the others

- RT (phase 2A): `*out*`/`*err*` writers, `print-method` once `arbace.core` loads, the reader's
  data readers, `RT.load` from embedded sources.
- Reflector (2A): `boxArg` through `jrt.AdaptFn` (c2g now writes `FromFn`), method selection
  without streams (`(. foo (valAt :k))` on a run-time class reaches `getMethods`' stream code
  today).
- jrt (2C): files (`Compiler.loadFile`), the rest of the JDK closure the namespaces name.
- c2g (2D): the whole-program `--main` (every public member as a root, Q2), line positions for
  stack traces, unused locals when an operation is missing (seen with `removeIf` on a world
  without `Predicate`: `declared and not used`).

## 5. Estimates

Agent-assisted days, at phase 2B's pace, for step 5 (B1-PLAN: 6-10):

| part | content | days |
|---|---|---|
| evaluator cases | the node kinds of §2.3-2.4 not yet done (try, throw, letfn, case, collections, def/set! with locals, keyword invoke, instance?, new, fields, monitors), each with a proof form | 2 |
| deftype/reify on `Dyn` | `compileStub`, `NewInstanceMethod` evaluation, fields, covariants, `defineClass` of interfaces (`gen-interface`), protocols | 2-3 |
| loading `arbace.core` | embedded sources, the core namespaces in order, fixing what each reaches (with 2A's RT and Reflector) | 2-3 |
| Clojure's suite | running it on the evaluator per namespace, recorded as `test/arbace-results.edn` is; the differences | 2-3 |
| depth, errors | `StackOverflowError`, `CompilerException` positions, stack traces | 0.5-1 |
| **all** | | **8.5-12** |

Slightly above B1-PLAN's 6-10: the whole-program world (every public member translated, Q2)
and protocols at run time (Q5) were not in its estimate.

## 6. Performance (for step 7)

A tree walk per form with seq-packed arguments and boxed locals: expect Joker's order of speed
(10-50x slower than the JVM's compiled code on loops), acceptable for a first REPL. The step-7
plan stays: per-arity `invoke`, closure compilation of the tree (each `Expr` to a Go closure
once, at `fn*` evaluation), invokers called with Go values on hinted interop, then AOT of the
core namespaces to Go forms.

## 7. Checks

- `bin/c2g-evalproof`: grows with each node kind (a form per case, its tree and value),
  amd64 and arm64.
- Differential: the same forms evaluated by the JVM Arbace (`bin/oracle`'s form scripts),
  compared as `bin/c2g-check` compares class scripts.
- Clojure's suite per namespace once `arbace.core` loads.

## 8. Amendments found (listed with reasons in C2G-NOTES.md, phase 2B)

B1 nested-class variants, B2 erased packages, B3 the evaluator's classes in the `Compiler`
variant, B4 `Dyn` as built (its API, `jrt.Dynamic`, `jrt.DefineDynamic`), B5 `FromFn` from
c2g in `arbace/lang`, B6 natives in their class's package, B7 the root spec `CLASS$*`, B8
`clj-fi-method` for classes from source. Accepted 2026-10-09 and folded into C2G-SPEC as
E1-E8, with the decisions of §9 that touch its §10 (C2G-SPEC §16).

## 9. Open questions, with recommendations

**Decided (2026-10-09):** the user accepted the recommendation of each of Q1-Q7.

- **Q1 Primitive fns.** (a) No primitive fns in the Go build: `primInterface` returns nil and
  the evaluator converts `^long`/`^double` parameters and returns (§3) (recommended: small,
  keeps arithmetic); (b) `EvalFn` subclasses per primitive interface used (only those the
  closed world names; more code, invokePrim then works for translated callers); (c) prim
  interfaces through `Dyn` (its method set would grow by the 322 interfaces).
- **Q2 The REPL's world.** (a) Every public member of every class in the build is translated
  (all of `arbace.lang`, the JDK closure's public API), so that reflection finds real code
  (recommended; measure the size: phase 1's "all of arbace.lang" was 811 classes, 6993
  methods); (b) only what the runtime reaches, the rest stubs that throw (smaller, but
  `(.foo x)` at the REPL fails unpredictably).
- **Q3 `EvalFn`'s shape.** (a) One class over `RestFn` with seq-packed arguments now, per-arity
  `invoke` in step 7 (recommended); (b) an `AFunction` with the 21 `invoke` arities now
  (faster calls, more code before anything runs); (c) separate `EvalFn`/`EvalRestFn` as
  C2G-SPEC §10.2's example (`(instance? RestFn f)` would then tell variadic fns as on the JVM;
  nothing in the namespaces asks).
- **Q4 Recursion limit.** (a) A per-thread depth counter in `invokeFn` with a fixed limit
  (recommended: 10,000 evaluator frames, about the JVM's default on simple recursion; Go's
  stack, 1 GB by default on 64-bit, is far from it); (b) Go's `debug.SetMaxStack` and no
  counter (the overflow is a fatal Go error, not a catchable `StackOverflowError`).
- **Q5 Interfaces made at run time** (`definterface`, `defprotocol`'s `gen-interface`):
  (a) `Compiler$Dyn.defineClass` of kind interface, implemented by `Dyn` classes through their
  member tables and called by reflection (recommended: protocols work; inline protocol methods
  are reflective calls until step 7); (b) protocols only through `extend` (no inline
  implementations: breaks `deftype` with protocols, used by `arbace.core` itself).
- **Q6 Stack traces.** (a) The evaluator records the Clojure fn and line per frame (a field of
  `Frame`) and `StackTraceElement`s come from the frame chain (recommended for step 5's
  REPL); (b) Go frames only (the evaluator's functions, unreadable to the user).
- **Q7 ASM's erasure.** (a) Erasure as a c2g rule (`c2g/erase`, done: no line of `Compiler`
  copied into the variant) (recommended); (b) cut each back-end member by name in the variant
  (about 150 methods and 120 fields listed, drifting with `Compiler`).
