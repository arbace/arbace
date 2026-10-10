# The evaluator: closure compilation (B1a step 7b)

B1a step 7 ([B1-PLAN.md](B1-PLAN.md), D7: "closure compilation of `Expr`s, then AOT of
namespaces to Go forms"), part b, branch `step7b`, 2026-10-10: the evaluator's speed. (Part a,
the runtime below the evaluator, c2g's code and jrt, is described under its own heading.)
Before this step each node of an analyzed `Expr` tree interpreted itself at every call
(`evalIn(Frame)`, [EVAL-NOTES.md](EVAL-NOTES.md)); hinted static calls such as
`Numbers.add(long, long)` went through `Reflector.invokeMatchingMethod`, boxing every
argument and result, and every fn call through `RestFn`'s seq of arguments.

Summary: each method of an evaluated fn, deftype, defrecord or reify is now compiled once, at
its first call, into a tree of `Code` nodes. On the benchmarks below the Go executable is 5 to
69 times faster than before (geometric mean 12.8x), now 6 to 500 times slower than the JVM's
compiled code (geometric mean 54x, from about 700x). Start to `-e nil`: 0.33 s → 0.21 s; a
load from source (`ARBACE_NO_IMAGE`) 3.5 s → 1.8 s; a namespace's `refer` of `arbace.core`
18.6 → 5.7 ms. Clojure's suite on Go: no regressions (19,379 of 19,398 assertions, on main
with `java.io.File`), and its
slowest namespaces run 4 to 5 times faster (reducers 345 → 67 s, parse 306 → 78 s);
`transducers`, skipped for timing out at 900 s, now passes in full in 574 s. The oracle on Go:
20,566 of 20,600, exactly its 34 known mismatches.

## The design

`arbace/lang/go/CompilerCode.clj` (a second variant file of `Compiler`, read by c2g only) and
`arbace/lang/go/CompilerOps.clj` (generated):

- **`Code`**, an abstract class: `run(Frame)` is the node's value as an `Object`, boxed as the
  bytecode boxes it; a node whose analyzed type is primitive (`Compiler.maybePrimitiveType`:
  `long`, `int`, `double`, `boolean`) also answers `runLong`, `runDouble`, `runBool` without
  boxing, as `emitUnboxed` does on the JVM. Parents pick the channel from the child's analyzed
  type; converters (`CodeUnbox`, `CodeL2I`, `CodeL2D`) carry `MethodExpr.emitTypedArgs`'
  conversions (`RT.longCast` of a boxed argument, `RT.intCast(long)`, a char widened, NPE for
  null), so the semantics stay the evaluator's (EVAL-NOTES.md, "The bytecode's semantics").
- **`CodeCompiler`** translates one method's tree: a local becomes its slot index, or its index
  in the frame's `prims` (a `long[]`; a `double` as its raw bits) when its type is `long` or
  `double`, or its closed-over index (the fn's values, the deftype's fields); constants are made
  once (`CodeConst`, with their `long` and `double` values); a Var node holds its `Var`, a
  keyword call its `Keyword`.
- **Direct static calls**: `test/c2g/eval_ops.clj` (run on the JVM's stage 2) lists the 578
  public static methods of `Numbers`, `RT`, `Util` and jrt's `Math` with at most three
  parameters of the kinds `long`, `int`, `double`, `boolean` or a reference, and writes
  `CodeOp0` .. `CodeOp3`: per arity one class whose `run`, `runLong`, `runDouble`, `runBool`
  are a `switch` over the methods, each case computing its arguments in their primitive types,
  setting the frame's line (stack traces) and calling the method, which c2g makes a direct Go
  call (`Numbers_Add_J_J__J`). `CodeRun.op` finds a method by its key (`class.name(desc)`).
- **Other resolved members** (`CodeHostStatic`, `CodeHostInstance`, `CodeHostNew`): the
  argument conversions of `Evaluator.typedArgs`, then jrt's natives
  `Compiler_CodeRun_Call/Construct` call the member table's invoker directly, without
  `Reflector`'s matching, the `LinkedList`, `getParameterTypes`' copy, or the
  `InvocationTargetException` round trip. Unresolved calls stay reflective.
- **Control**: `do` and a `let`'s stores are chains of pairs (`CodeSeq`, `CodeStore`); `loop`
  (`CodeLoop`) runs its body again while the frame's `recur` flag is set; `recur` computes its
  values (through temporaries in the frame when there are several) and sets the flag, so a
  `long` loop recurs without boxing. `if` tests a boolean child with `runBool`.
- **Calls**: `EvalFn` answers `invoke` of arities 0 to 5 itself: the compiled method of that
  fixed arity (`FnExpr.evalArities`) is called with the arguments bound in a frame, without a
  seq; other calls (variadic, `apply`) go through `RestFn` to `doInvoke` as before.
  `EvalMethod` (a deftype's method) answers `invoke` of 1 to 4 arguments, and c2g's `Dyn`
  dispatch (`dynCallFast`, `arbace/c2g/dyn.clj`) calls `invoke` of the arity instead of
  `applyTo`.
- **Frames are reused**: calls nest, and no frame outlives its call (a fn closes over values,
  copied when it is made), so each thread's `EvalState` keeps its frames by depth; a call takes
  the frame of its depth and clears its slots when it returns (nothing it held is kept). The
  arguments of a call through a seq are taken from the seq before the frame is taken (realizing
  it may run evaluated code at the same depth).
- `(str a b ...)` (`StrConcatExpr`) concatenates as the bytecode's `StringConcatFactory` call
  does, without calling the `str` fn.
- **Compat mode**: a method holding a node kind the compiler does not know (none today; the
  class forms at the REPL may bring some) is compiled with its `long` and `double` locals boxed
  in the slots, and the unknown node evaluated by its `evalIn`.

Stack traces are unchanged: the try literal of `Evaluator.invokeFn` and `invokeMethod` is still
the Go frame jrt replaces by the Clojure frame (`isEvalCall`); the `Compiler_Code*` frames are
internal to jrt's mapping, and `CodeHost*` frames hide reflective frames above them, as the
`EvalIn` frames of the host nodes did (`go/arbace/jrt/throwable.clj`).

**The image (step 6)** keeps storing the analyzed `Expr` trees; methods are compiled lazily, at
their first call, not when the image is replayed: most of core's 1,262 fns are not called at
start, and code that runs once (the `ns` forms' `refer`, top-level defs) is compiled once
anyway. The new caches (`ObjMethod.evalCM`, `FnExpr.evalArities`) are among the image's skipped
fields. Preparing the image takes 11 s (32 s before), the image is unchanged (3.8 MB).

## Measurements

Method: `test/bench/eval-bench.clj`, run unchanged by the JVM's `bin/arbace` (jar and AOT cache)
and by the Go executable; each benchmark runs once, then repeatedly for at least a second and 3
runs; the best run's time is reported (ms). Go: amd64, go1.27.1, `GOGC` default after the start
(step 6); "before" is main's executable at `bc586b1`. The machine (64 cores) was shared with
other agents' builds; differences under 10% are noise.

| benchmark | Go before | Go after | speedup | JVM | before / JVM | after / JVM |
|---|---:|---:|---:|---:|---:|---:|
| `fib` (fib 22), boxed | 123.6 | 14.3 | 8.6x | 0.16 | 772x | 90x |
| `fib-prim`, `^long` | 119.8 | 9.8 | 12.2x | 0.09 | 1,331x | 109x |
| `loop-long`, 1M iterations | 3,733.7 | 72.5 | 51.5x | 0.43 | 8,683x | 169x |
| `loop-double`, 200k | 810.5 | 11.7 | 69.3x | 0.55 | 1,474x | 21x |
| `dotimes-array`, 200k `aset`/`aget` | 1,058.1 | 24.2 | 43.8x | 0.09 | 11,757x | 268x |
| `reduce-range`, `(reduce + (range 1e6))` | 1,339.1 | 146.8 | 9.1x | 3.14 | 426x | 47x |
| `reduce-fn`, 200k | 406.8 | 41.1 | 9.9x | 7.32 | 56x | 6x |
| `vector-conj`, 100k | 350.6 | 52.4 | 6.7x | 1.43 | 245x | 37x |
| `map-assoc-get`, 20k | 184.9 | 36.5 | 5.1x | 4.03 | 46x | 9x |
| `keyword-map`, 200k | 531.9 | 23.9 | 22.2x | 0.68 | 782x | 35x |
| `str-build`, 20k | 526.2 | 11.6 | 45.4x | 0.41 | 1,284x | 28x |
| `seq-ops`, 20k | 1,292.7 | 204.5 | 6.3x | 5.22 | 248x | 39x |
| `lazy-walk`, 100k | 1,331.5 | 193.5 | 6.9x | 2.55 | 522x | 76x |
| `transduce`, 100k | 793.6 | 94.4 | 8.4x | 0.81 | 980x | 117x |
| `into-xform`, 100k | 743.5 | 96.5 | 7.7x | 0.98 | 759x | 98x |
| `protocol`, 100k calls | 987.8 | 129.0 | 7.7x | 1.06 | 932x | 122x |
| `multimethod`, 50k calls | 317.0 | 21.2 | 14.9x | 1.36 | 233x | 16x |
| `destructure`, 50k calls | 780.4 | 87.5 | 8.9x | 3.29 | 237x | 27x |
| `closures`, 100k | 692.0 | 96.3 | 7.2x | 0.19 | 3,642x | 507x |
| geometric mean | | | 12.8x | | ~700x | 54x |

Other measurements (amd64, best of 10 or 3 runs):

| | before | after | JVM |
|---|---:|---:|---:|
| start to `-e nil` done (with the image) | 0.33 s | 0.21 s | 0.17 s |
| start from the sources (`ARBACE_NO_IMAGE`) | 3.54 s | 1.82 s | |
| `refer` of `arbace.core` into a new namespace | 18.6 ms | 5.7 ms | 2.1 ms |
| preparing the image (`bin/arbace-go --build`) | 32 s | 11 s | |
| the oracle's `(reduce + 0 (r/map identity (range 1.0E8)))` | about 280 s | 53 s | |
| Clojure's suite on Go: `reducers` / `parse` (the slowest) | 345 s / 306 s | 67 s / 78 s | |
| Clojure's suite on Go: `transducers` | over 900 s (skipped) | 574 s, 108 of 108 | |

Profiles (perf; Go's frame pointers give full stacks): before, a `long` loop spent most of its
time in `Reflector.invokeMatchingMethod` and allocating (boxes, argument arrays,
`getParameterTypes`' copies), and the collector took 60% of the CPU. After, a `long` loop
allocates nothing; what remains of a call is the frame's setup, the per-thread state's
`ThreadLocal` lookup (about 1.5%), the try's defer, and, for boxed arithmetic, the runtime's
`Numbers` ops (`GetClass`, `Number` casts: part a). On allocation-heavy benchmarks the
collector still takes most of the CPU, the live heap (all of core) being large for `GOGC=100`:
with `GOGC=400` `str-build`, `closures` and `protocol` take 20 to 30% less; the collector's
settings are part a's (D7).

## Checks

- `bin/arbace-go --smoke` (amd64) passes.
- `bin/c2g-evalproof`: as expected on amd64 and arm64 (31 forms).
- The oracle on Go (`bin/oracle check 'target/arbace-go/amd64/arbace -' --timeout 900
  --expected test/oracle/known-go-amd64.edn`): 20,566 of 20,600, the 34 recorded mismatches, 0
  new, 0 now passing (main merged at `b20b577`, with `java.io.File`; before that merge 20,243 of
  20,276, the 33 then recorded).
- Clojure's suite on Go (`CLOJURE_TESTS_GO=... bin/clojure-tests -j 12`): 19,379 of 19,398
  assertions (646 tests, 0 errors), no regressions against `test/arbace-go-results.edn` (before
  that merge 19,255 of 19,280); `transducers` alone (not skipped) 108 of 108.
- `bin/gate` was not run: no source the bootstrap compiles changed (the variants and c2g are
  read by c2g only).

## Tried and dropped

- **Children in `Code[]` arrays**: in Go every element read from an `Object[]` is an
  interface assertion to `Code`, a lookup of its method table (`itabTableType.find` was 6% of a
  loop's profile): the hot nodes hold their children in fields (`CodeSeq` pairs, `CodeRecur`'s
  first four values, `CodeInvoke`'s first six arguments).
- **A class per signature shape for direct calls** (`CodeJJ_J` ...): more classes for the same
  calls; one class per arity with a `switch` on the method is as fast (gc compiles the switch
  to a jump table and inlines the small `Numbers` methods).
- **Typed function values in c2g's member tables** (an invoker per method taking Go values):
  general, but a change to c2g's tables and jrt's `MethodInfo` where part a works; the generated
  `CodeOpN` cover the hot methods without it.
- **Compiling the image's trees at replay**: start would compile code that never runs; lazy
  compilation at the first call made start faster (0.33 → 0.21 s).
- **A new frame per call**: with a `Frame` and its slot array allocated per call, the collector
  took 60% of the CPU on `reduce`; reusing frames by depth removed most of it (`fib` 19.8 →
  14.3 ms, `reduce-range` 327 → 147 ms).
- Not done (measured small): the per-thread state in a field of jrt's `Thread` instead of a
  `ThreadLocal` (1.5% of `fib`).

## Next

- AOT of namespaces to Go forms built into the executable (B1-PLAN step 7's second half): the
  `Code` tree is a direct guide (each node kind a Go statement form); it would remove the
  remaining dispatch per node and the frames' boxing at calls.
- Protocol call sites (`InvokeExpr.isProtocol`: the JVM's inline cache of the target's class),
  `Keyword` call sites, `case` on ints with a primitive `long` key without boxing.
- The collector's settings for the running program (part a, D7).

## Proposed amendments (for the user's review)

Numbered EC.

- **EC1 (EVAL-PLAN §2, §6; C2G-SPEC §10.1) Closure compilation.** Each method of an evaluated
  fn or deftype is compiled at its first call into `Code` nodes (`CompilerCode.clj`); `evalIn`
  stays the evaluation of top-level forms and of compat-mode nodes. `long` and `double` locals
  live unboxed in the frame's `prims`; `recur` sets the frame's flag.
- **EC2 (C2G-SPEC §10.1) Direct static calls generated.** `arbace/lang/go/CompilerOps.clj` is
  generated by `test/c2g/eval_ops.clj` from the JVM's classes and checked in (regenerated when
  `Numbers`, `RT`, `Util` or jrt's `Math` change their public static methods).
- **EC3 (EVAL-PLAN §2.2, Q3 as built) Calls of fixed arity without a seq**: `EvalFn.invoke` of
  0 to 5 arguments, `EvalMethod.invoke` of 1 to 4, `Dyn`'s dispatch by arity (`dynCallFast`).
- **EC4 (EVAL-PLAN §2.1) Frames reused per thread by depth**, cleared when the call returns.
- **EC5 (C2G-SPEC §10.7; jrt) Resolved members called through their invoker** by jrt's natives
  `Compiler_CodeRun_Call_Method_O_O1__O` and `..._Construct_Constructor_O1__O`, exceptions not
  wrapped; jrt's stack-trace mapping knows the `Compiler_Code*` frames.
- **EC6 (EXEC-NOTES, U1) The image stores the analyzed trees; methods are compiled lazily at
  their first call**, the compiler's caches being skipped fields of the image.
- **EC7 (test/arbace-go-results.edn) `transducers` no longer skipped**: it passes in 574 s,
  under the runner's 900 s, but would add about 10 minutes of one process to the suite on Go
  in `bin/gate --full` (it is the longest namespace); the user's choice.
