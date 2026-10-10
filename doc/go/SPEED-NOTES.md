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

**With the suite's last fixes** (main at `5ced273`, EVAL-NOTES.md, SL1 and SL2): the compiled
paths clear locals as `evalIn` does: a use the analyzer marks `shouldClear` (and the local
`canBeCleared`, not primitive) reads and clears its slot (`CodeLocalClear`), or in a `^:once` fn
its closed-over value (`CodeClosedClear`); a fn's or reify's closed-over values are read through
its `closesExprs`, so their last uses clear too; a local in a `do`'s statement position is
neither read nor cleared. Hinted calls of methods not listed for `CodeOpN` take
`Evaluator.invokeResolved` (its invoker directly) when `Evaluator.directOk`, else Reflector, as
`evalIn` does; the closure compiler's own `call` native is gone (constructors keep
`Compiler_CodeRun_Construct`). Clojure's `clearing` namespace passes 31 of 31; the benchmarks
move within the noise.

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
- After merging main at `5ced273` (suite-last): the oracle 20,566 of 20,600, as recorded;
  Clojure's suite on Go 19,506 of 19,506 assertions (664 tests, `clearing` 31 of 31), no
  regressions; smoke and `bin/c2g-evalproof` pass.
- Clojure's suite on Go (`CLOJURE_TESTS_GO=... bin/clojure-tests -j 12`): 19,379 of 19,398
  assertions (646 tests, 0 errors), no regressions against `test/arbace-go-results.edn` (before
  that merge 19,255 of 19,280); `transducers` alone (not skipped) 108 of 108.
- After merging step 7b (`bfedb18`, main at `48ba45c`): `bin/jrt test` amd64 and
`bin/arbace-go --smoke` pass; Clojure's suite on Go has no regressions against
`test/arbace-go-results.edn`; the oracle matches 20,780 of 21,027, with 242 new mismatches
against `test/oracle/known-go-amd64.edn`, all in `defclass.clj` and `defclass_corpus.clj`
(`Unable to resolve classname: Compiler$CF$Node`): main's own executable of that commit fails
the same way (class forms at the REPL after the merges), so they are not this branch's.
`bin/gate` was not run: no source the bootstrap compiles changed (the variants and c2g are
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

## Amendments

Numbered EC. The user accepted EC1-EC6 on 2026-10-10 (folded where each says); EC7 was not
taken.

- **EC1 (EVAL-PLAN §2, §6; C2G-SPEC §10.1) Closure compilation.** Each method of an evaluated
  fn or deftype is compiled at its first call into `Code` nodes (`CompilerCode.clj`); `evalIn`
  stays the evaluation of top-level forms and of compat-mode nodes. `long` and `double` locals
  live unboxed in the frame's `prims`; `recur` sets the frame's flag.
  *Accepted 2026-10-10: EVAL-PLAN §2, §2.1, §6; C2G-SPEC §10.1, §16.*
- **EC2 (C2G-SPEC §10.1) Direct static calls generated.** `arbace/lang/go/CompilerOps.clj` is
  generated by `test/c2g/eval_ops.clj` from the JVM's classes and checked in (regenerated when
  `Numbers`, `RT`, `Util` or jrt's `Math` change their public static methods).
  *Accepted 2026-10-10: C2G-SPEC §10.1, §16.*
- **EC3 (EVAL-PLAN §2.2, Q3 as built) Calls of fixed arity without a seq**: `EvalFn.invoke` of
  0 to 5 arguments, `EvalMethod.invoke` of 1 to 4, `Dyn`'s dispatch by arity (`dynCallFast`).
  *Accepted 2026-10-10: EVAL-PLAN §2.2; C2G-SPEC §16.*
- **EC4 (EVAL-PLAN §2.1) Frames reused per thread by depth**, cleared when the call returns.
  *Accepted 2026-10-10: EVAL-PLAN §2.1; C2G-SPEC §16.*
- **EC5 (C2G-SPEC §10.7; jrt) Resolved members called through their invoker** by jrt's natives
  `Compiler_CodeRun_Call_Method_O_O1__O` and `..._Construct_Constructor_O1__O`, exceptions not
  wrapped; jrt's stack-trace mapping knows the `Compiler_Code*` frames.
  *Accepted 2026-10-10: C2G-SPEC §10.1, §10.7, §16 (with SL2, through `Evaluator.invokeResolved`).*
- **EC6 (EXEC-NOTES, U1) The image stores the analyzed trees; methods are compiled lazily at
  their first call**, the compiler's caches being skipped fields of the image.
  *Accepted 2026-10-10: EXEC-NOTES.md, "The image of prepared namespaces"; C2G-SPEC §16.*
- **EC7 (test/arbace-go-results.edn) `transducers` no longer skipped**: it passes in 574 s,
  under the runner's 900 s, but would add about 10 minutes of one process to the suite on Go
  in `bin/gate --full` (it is the longest namespace); the user's choice.
  *Not taken (the user, 2026-10-10): `seq-and-transducer` stays skipped in the Go reference (`:skipped-tests`); the namespace passed in full, 108 of 108, in 574 s when run alone, under the runner's 900 s.*

# Speed below the evaluator (B1a step 7a)

B1a step 7, first part ([B1-PLAN.md](B1-PLAN.md), decision D7 of
[C2G-NOTES.md](C2G-NOTES.md), phase 2D; branch `step7a`, 2026-10-09): the cost of the
translated runtime and of jrt under the evaluator, measured with profiles and fixed where it
paid. The evaluator itself (closure compilation, how `Expr`s call methods) is step 7b and is
not changed here. Summary:

- **The collector was most of the cost.** With Go's defaults (`GOGC=100`, a 4 MiB minimum
  heap) and 64 processors the mark workers took 60-70% of all CPU, and a program with a small
  live heap collected hundreds of times a second. jrt now starts programs with `GOGC=200` and a
  64 MiB minimum heap (both overridable). This alone makes the micro workloads 2-3 times faster
  and cuts the executable's CPU time by two thirds.
- **Allocations removed** where jrt made two objects for one Java object (reference arrays,
  strings, `StringBuilder`), or where reflection copied the arguments on every call.
- **Fewer type assertions and deferred calls** on hot paths: `getClass()` of boxed numbers and
  strings, the nominal check of objects made at run time, `try` blocks without live catch
  clauses.
- **Profile-guided builds** (`bin/arbace-go --build --pgo`): gc devirtualizes and inlines the
  hot interface calls a training run shows; 5-15% more, opt-in (it doubles the build).

Overall, on linux/amd64: `bin/c2g-perf`'s workloads went from 3.7-49 times the JVM's time to
2.3-32 times (1.7 to 4.7 times faster); the executable's evaluator workloads run 1.4-1.65 times
faster with a third of the CPU. Before main's namespace image it started (`-e nil`) in 3.0 s
against 4.3 s (2.8 s with PGO); merged with main, 0.27 s against main's 0.35 s.

## Method

The machine: AMD EPYC 7763, 64 processors, 60 GB, shared with three to five other agents
(load averages from 3 to 120 during this work). Wall times are noisy (±20%), so:

- **Micro workloads at fixed work.** `bin/c2g-perf` (the JVM against Go, time-budgeted runs,
  ns per element) for the before/after table; for single changes, a harness over the same
  translated functions (`.tmp/c2g/perf/work/mod/cmd/xbench`, scratch: one workload, a fixed
  number of repetitions per process) under `perf stat -e cycles:u,instructions:u,task-clock`
  with `GOMAXPROCS=8`, the minimum of three runs. User cycles and instructions do not count
  time spent waiting for the processor, so they are stable under load where wall time is not.
- **The executable.** Scripts of evaluator-heavy workloads (below, `w.clj`), each timed inside
  the program (`System/nanoTime`), the minimum of two or three alternating runs of each build;
  CPU time by `perf stat -e task-clock`; `-e nil` for the start (loading `arbace.core`,
  `spec.alpha` and `arbace.main` through the evaluator).
- **Profiles.** `ARBACE_CPUPROFILE=FILE` and `ARBACE_MEMPROFILE=FILE` (new, jrt's
  `StartRuntime`) on any program run under `jrt.RunMain`, `C2G_PROF` on `bin/c2g-perf`'s,
  `perf record -g` on the executables; `go test -bench -cpu 1` over the translated packages for
  single operations.

The executable's workloads (`w.clj`):

```clojure
(bench "reduce-range" (reduce + (range 1e7)))
(bench "map-inc" (reduce + (map inc (range 1e6))))
(bench "into-xf" (count (into [] (comp (map inc) (filter even?)) (range 1e6))))
(bench "hashmap" (count (reduce #(assoc %1 %2 %2) {} (range 2e5))))
(bench "vec-conj" (count (reduce conj [] (range 1e6))))
(bench "str" (count (apply str (map str (range 2e5)))))
(bench "fib" (letfn [(fib [n] (if (< n 2) n (+ (fib (- n 1)) (fib (- n 2)))))] (fib 25)))
(bench "loop" (loop [i 0 acc 0] (if (< i 3000000) (recur (inc i) (+ acc i)) acc)))
(bench "binding" (binding [*d* 2] (loop [i 0 acc 0] (if (< i 500000) (recur (inc i) (+ acc *d*)) acc))))
(bench "sort" (count (sort (map #(mod (* % 7919) 10007) (range 2e5)))))
(bench "keyword-map" (let [m {:a 1 :b 2 :c 3}] (loop [i 0 acc 0] (if (< i 1000000) (recur (inc i) (+ acc (:b m))) acc))))
(bench "atom-swap" (let [a (atom 0)] (dotimes [i 500000] (swap! a inc)) @a))
(bench "lazy-seq-str" (count (arbace.string/join "," (range 100000))))
```

## Where the time went (before)

`perf record` of the executable on `loop`, `map-inc` and `hashmap` (default settings): the
collector's functions (`tryDeferToSpanScan`, `scanObjectsSmall`, `scanSpan`, `gcDrain` ...)
took about 60% of the cycles, allocation (`mallocgc*`) another 8%; the evaluator's own code
and the translated runtime shared the rest, no function above 1%. With `GOGC=off` the same
script was *slower* (page faults), so the mutator, not the collector, sets the wall time: the
collector's cost was CPU taken from the other processors and the mutator's assists. `gctrace`
showed 73 collections for `-e nil` (a 37 MB live heap), each using 150-180 ms of CPU over the
64 processors.

On `bin/c2g-perf`'s workloads the live heap is tiny: Go's 4 MiB minimum made
`arrayMapSmall` collect 743 times in 2 s, the collector running half of the time.

The allocation profile of the executable's evaluator workloads (`alloc_space`): reference
arrays 66% of the bytes, two thirds of them made by reflective calls (the parameter classes
copied by `Method.getParameterTypes` twice per call, the arguments boxed and copied), the
evaluator's frames and argument arrays most of the rest.

Single operations (`go test -bench -cpu 1`, default `GOGC`, before):

| operation | ns |
|---|---:|
| `Var.deref`, root binding | 4.5 |
| `Var.deref`, thread-bound | 89 |
| `AReference.meta` (synchronized) | 19 |
| `MonitorEnter`/`MonitorExit`, uncontended | 15.3 |
| an entered `try` (literal, `defer`, `recover`) | 7.5 |
| `Thread.currentThread()` (the goroutine-local slot) | 3.6 |
| `Util.hasheq` of a `String` / a `Keyword` | 15.1 / 12.5 |
| `Util.equiv` of two `Long`s (500) | 41.6 |
| `RT.get` of a keyword in an array map / the keyword invoked | 19.5 / 19.5 |
| `Numbers.add` of two boxed longs | 55 |
| `Long.valueOf` (outside the cache) | 28 |
| `new Object[2]` | 66 |
| `String.equals` (11 units) / `concat` | 8.5 / 84 |
| `IFn` instanceof (assertion and the nominal check) / the assertion alone | 4.5 / 2.2 |

The goroutine-local slot, monitors and `try` cost what C2G-SPEC §13 measured; the outliers
were the thread-bound deref (a `MapEntry` allocated per lookup), equivalence of numbers
(`getClass` through `Object_I` four times), and everything that allocates.

## The changes, each measured

### 1. The collector's settings (D7) — kept

jrt's package initialization (`go/arbace/jrt/tuning.clj`, `init`, so before the main
package's own start-up setting, amendment U4, which raises `GOGC` to 400 until `arbace.main` is
loaded and then restores this one): `GOGC` is 200 unless the environment sets `GOGC`, and a
64 MiB minimum heap unless `ARBACE_MIN_HEAP_MB` sets another (0: none). The minimum heap is a
ballast: a 64 MiB byte slice, never written, without pointers, so neither scanned nor resident
(its pages are never touched); it raises the collector's goal as a JVM's initial heap (`-Xms`)
does. Go has no minimum-heap setting (`GOMEMLIMIT` only lowers the goal).

The executable, `ev.clj` (`loop` to 1M, `map-inc` to 300k, `hashmap` to 100k; ms, wall and
CPU, maximum resident set):

| setting | loop | map-inc | hashmap | wall | CPU | RSS |
|---|---:|---:|---:|---:|---:|---:|
| `GOGC=100` (Go's default) | 2,426 | 2,174 | 547 | 9.60 s | 34.5 s | 155 MB |
| `GOGC=200` | 2,247 | 1,778 | 425 | 8.35 s | 20.8 s | 202 MB |
| `GOGC=400` | 2,006 | 1,621 | 419 | 7.73 s | 14.6 s | 315 MB |
| `GOGC=400`, 32 MiB minimum | 1,925 | 1,558 | 358 | 7.28 s | 10.6 s | 479 MB |
| `GOGC=400`, 64 MiB minimum | 1,867 | 1,525 | 370 | 7.19 s | 9.5 s | 684 MB |
| **`GOGC=200`, 64 MiB minimum (chosen)** | 1,937 | 1,617 | 385 | 7.28 s | 11.7 s | 404 MB |
| `GOGC=200`, 128 MiB minimum | 1,939 | 1,521 | 360 | 7.18 s | 10.0 s | 559 MB |
| `GOGC=800` | 2,160 | 1,600 | 382 | 7.63 s | 11.0 s | 520 MB |

`-e nil`: 4.71 s and 13.8 s of CPU with Go's defaults; 3.42 s, 4.7 s of CPU and 303 MB with
the chosen settings. `GOGC=400` with the 64 MiB minimum is a little faster, at 1.7 times the
memory. The suite's runner sets `GOGC=400` for its processes and still does; the minimum
heap applies there too.

The micro workloads (fixed work, user Mcycles, `GOMAXPROCS=8`):

| workload | `GOGC=100` | `GOGC=400` | 400, 16 MiB | 400, 64 MiB | 100, 64 MiB |
|---|---:|---:|---:|---:|---:|
| vecConj | 10,450 | 4,562 | 3,363 | 2,662 | 3,163 |
| arrayMapSmall | 9,397 | 3,784 | 3,238 | 2,659 | 2,974 |
| numbers | 4,704 | 3,353 | 2,901 | 2,677 | 2,864 |
| hashMapAssoc | 12,658 | 5,034 | 3,459 | 2,610 | 3,876 |

Tried and dropped: the collector without Green Tea (`GOEXPERIMENT=nogreenteagc`): 18-42%
more cycles. `GOMAXPROCS` lower than the processor count: no gain in wall time.

### 2. Reference arrays in one allocation (D7) — kept

`jrt.NewRefArray` made a `RefArray` (header, component class, slice) and the slice's array: two
objects. Now an array of up to 32 slots is one object, `refArrayBuf[[N]any]`: the `RefArray`
followed by its slots, its slice pointing into the same object (`unsafe.Slice`), N rounded up
to Go's size classes (1-13, then odd sizes to 29, then 32); longer arrays keep two objects.
`Copy` (clone) uses the same allocation. The 16-byte slots stay (C2G-SPEC §13.2's thin
pointers would halve them, a change of the whole object model, not taken).

Fixed work, user Mcycles, `GOGC=100` (the default then): vecConj 10,680 → 10,005 (-6%),
hashMapAssoc 13,318 → 11,566 (-13%), vecEquiv 1,099 → 956 (-13%), the others within 3%.

### 3. `getClass()` of boxed numbers and strings — kept

`jrt.GetClass(x any)` asserted `x` to `Object_I` (an itab lookup) and called `GetClass__Class`
through the interface. A type switch on `*Long`, `*Double` and `*String` first (a compare of
the type word each) answers the classes `Numbers.ops`, `Numbers.category` and `Util.hasheq`
ask for most: numbers 4,716 → 4,392 Mcycles (-7%), vecEquiv 956 → 846 (-11%), seqWalk -9%.

### 4. `try` without live catch clauses — kept (c2g)

A `try` whose catch clauses catch no class of the closed world (and that has no
normal-completion code) needs no function literal of its own: its body is translated in place,
inside the `finally`'s literal when there is one, else straight into the enclosing block
(`arbace.c2g.code/translate-try`). Before, `try`/`finally` (locks, `binding`'s push and pop,
`LazySeq.sval`) made two nested literals, each with a `defer` and a `recover`, about 7.5 ns
each. Measured with item 5 (one build): `-e nil` 3.45 → 2.98 s, the `w.clj` workloads 8-15%
faster (`loop` 5,442 → 4,809 ms, `map-inc` 5,227 → 4,750, `keyword-map` 1,694 → 1,499,
`lazy-seq-str` 426 → 361).

### 5. A thread-bound var's deref without a `MapEntry` — kept (Go variant)

`Var.getThreadBinding` looked the var up with `entryAt`, which allocates a `MapEntry` (the
JVM's escape analysis removes it, Go's cannot across the interface call): 89 ns per deref of a
bound var, most of it the allocation. The Go build's variant `arbace/lang/go/Var.clj` uses
`valAt` (the bindings map vars to `TBox`es, never to nil, so the result is the same). `binding`
in `w.clj`: 939 → 791 ms with items 4 and 6.

### 6. Reflective calls: no copy of the arguments — kept (the shared parameter classes dropped)

The evaluator calls static and instance methods through `Method.invoke` (step 7b changes
that); `convertArgs` copied the arguments into a new slice on every call. It now passes the
arguments' own slice to the member table's invoker unless a primitive parameter converts one
(the invokers only read it; nothing of it reaches the caller). Measured with it, and decided
against by the user: `getParameterTypes()` returning one shared array per `Method` (O3, a
deviation from Java's fresh copy; it removed 21% of the bytes the evaluator workloads
allocated, the copies the evaluator's `typedArgs` and the Reflector's `boxArgs` ask for on
every call). jrt keeps Java's contract; step 7b's direct invocation removes those calls.
`convertArgs`' slice alone removes about 3% of those bytes.

### 7. The dynamic flag — kept

c2g's `instanceof` and checkcast of an interface follow a successful Go assertion with a
second one, to `jrt.Dynamic`, because an object of a class made at run time (`Dyn`, proxies)
has every interface's methods and must be checked nominally. Bit 31 of the header's low word
(the identity hash is 31 bits) now marks those objects (`MarkDynamic`, set by c2g's `Dyn`
constructors), and `jrt.IsDynamic(x)` (inlined: a load through the interface's data word, every
Java object's header being its first word) skips the second assertion for every other object:
`IFn` instanceof 4.5 → about 2.5 ns. In the executable: `fib` 427 → 407 ms, `into-xf` 5,308 →
5,119, `keyword-map` 1,499 → 1,458, the others within 1%.

### 8. Strings and `StringBuilder` in one allocation — kept

As for arrays: `jrt.allocString(n)` makes a `String` of up to 108 code units in one object
(`stringBuf[[N]uint16]`, N rounded to size classes); `NewStringUTF16` (`StringBuilder.toString`,
`substring`), `Concat` (`str` of several values) and the Latin-1 strings of numbers use it.
`new StringBuilder()` keeps its first 16 units in the object (`init`), and `append(int)` /
`append(long)` write the digits without making a `String`. `bin/c2g-perf`'s `strings` is
in the results table (178 → 93 ns per element with the other items). The evaluator's string workloads did not move (they are dominated by the
evaluator).

### 9. Allocation sampling off — kept

Linking `runtime/pprof` (for the profiles above) turns on Go's allocation sampling, whose
stack walks over the evaluator's deep stacks cost 1.8% of the `str` workload; jrt's `init`
sets `runtime.MemProfileRate` to 0 unless `ARBACE_MEMPROFILE` is set.

### 10. Profile-guided optimization — kept, opt-in

`bin/arbace-go --build --pgo` builds as before (with the namespaces' image), runs the host's executable on
`test/arbace-go-pgo.clj` (a training workload in the spirit of `test/aot-training.clj`: the
common libraries, collections, seqs and transducers, numbers, strings, regexes, printing,
protocols, dynamic vars, exceptions, futures and agents; about 15 s) with a CPU profile, and
builds every executable again in its work module with `go build -pgo` (as the image's link
does; `bin/g2c build --pgo FILE` is new too, for other programs). gc then
devirtualizes the hot interface calls (a type test and a direct, inlinable call) and inlines
more on hot paths. Micro workloads (fixed work, user Mcycles, a profile of the same
workloads): hashing -37%, numbers -15%, vecEquiv -20%, hashMapAssoc -10%, the others -1 to
-5%. The executable (an earlier build, its profile from `-e nil`, `ev.clj` and `w.clj`): the
`w.clj` total 46.3 → 43.2 s (-7%), `hashmap` -18%, `fib` -9%. Opt-in because the second build
is a full one (about 11 minutes of CPU for the c2g step, both builds and the training run, against
about 8 without) and the training run needs the amd64 executable; proposed for the
freeze's builds (amendment O5).

### Tried and dropped

- `GOEXPERIMENT=nogreenteagc` (item 1).
- `GOGC=off` with a memory limit: slower (page faults on fresh memory), and unsafe for large
  live heaps.
- A larger `Long` cache (`Long.valueOf` caches -128..127, as Java's default): Java allows more
  (`AutoBoxCacheMax`), but `identical?` on boxed numbers would differ from the JVM's in the
  oracle, and the workloads' numbers are mostly outside any reasonable cache.
- Rewriting `Numbers`' dispatch (`ops(x).combine(ops(y))`, four interface calls per
  operation): the remaining cost of `numbers` is allocation (60% of its profile: three boxes
  per element, which the JVM's escape analysis removes); PGO devirtualizes the calls.

## Results

### `bin/c2g-perf` (ns per element; the JVM's column from the same run)

| workload | JVM | Go before | Go after | Go/JVM before | Go/JVM after | Go speedup |
|---|---:|---:|---:|---:|---:|---:|
| vecConj | 34-46 | 755.7 | 220.3 | 22.0 | 4.8 | 3.4 |
| hashMapAssoc | 340-494 | 7,399 | 1,560 | 21.8 | 3.2 | 4.7 |
| arrayMapSmall | 144-165 | 3,509 | 1,108 | 24.3 | 6.7 | 3.2 |
| hashing | 17-18 | 91.4 | 53.7 | 5.3 | 2.9 | 1.7 |
| numbers | 4.6-5.4 | 261.6 | 145.2 | 48.9 | 31.7 | 1.8 |
| seqWalk | 8.7-10.7 | 121.3 | 58.3 | 11.3 | 6.7 | 2.1 |
| vecEquiv | 16,000-26,900 | 100,000 | 51,898 | 3.7 | 3.2 | 1.9 |
| strings | 40-62 | 178.4 | 92.9 | 2.9 | 2.3 | 1.9 |

Before: main at `42552a0`; after: this branch, without PGO (`bin/c2g-perf` builds without a
profile). The JVM's time varies between runs (it is time-budgeted and shares the machine);
the ratios are each run's own.

### The executable (linux/amd64)

ms unless noted; minimum of two (`w.clj`) or three (`-e nil`) alternating runs:

| | before (`42552a0`) | after | after, `--pgo` | speedup (after) |
|---|---:|---:|---:|---:|
| `-e nil` (start) | 4.26 s | 3.04 s | 2.83 s | 1.40 |
| `-e nil` CPU | 13.2 s | 4.16 s | 4.04 s | 3.17 |
| `-e nil` resident | 116 MB | 300 MB | 302 MB |  |
| reduce-range | 14,796 | 10,568 | 10,021 | 1.40 |
| map-inc | 6,604 | 4,771 | 4,469 | 1.38 |
| into-xf | 7,288 | 5,216 | 4,804 | 1.40 |
| hashmap | 1,144 | 740 | 702 | 1.55 |
| vec-conj | 1,881 | 1,347 | 1,264 | 1.40 |
| str | 2,228 | 1,471 | 1,405 | 1.51 |
| fib | 583 | 430 | 374 | 1.36 |
| loop | 7,162 | 4,988 | 4,502 | 1.44 |
| binding | 1,193 | 792 | 714 | 1.51 |
| sort | 6,469 | 4,729 | 4,249 | 1.37 |
| keyword-map | 2,228 | 1,508 | 1,386 | 1.48 |
| atom-swap | 2,103 | 1,512 | 1,414 | 1.39 |
| lazy-seq-str | 528 | 368 | 349 | 1.43 |
| `w.clj`, the whole run | 58.9 s | 41.9 s | 38.9 s | 1.41 |
| `w.clj` CPU | 230 s | 68.0 s | 65.4 s | 3.38 |

The JVM's `bin/arbace` on the same script, for scale: reduce-range 156, map-inc 83, into-xf 41, hashmap 78, vec-conj 51, str 25, fib 14, loop 8, binding 25, sort 90, keyword-map 32, atom-swap 17, lazy-seq-str 16 ms: the evaluator, not the runtime below it, is now the factor (step 7b).

### The suite's slow namespaces

Each namespace in its own process through the suite's runner (`bin/clojure-tests
NAMESPACE` with `CLOJURE_TESTS_GO`, which sets `GOGC=400`; the time includes the renaming and
the JVM's report, about 10 s), two runs each, before the namespaces' image:

| namespace | before | after | after, `--pgo` |
|---|---:|---:|---:|
| `clearing` | 76.7 / 78.7 s | 69.3 / 69.7 s | 66.1 / 67.0 s |
| `transducers` | the runner's 900 s timeout | the same | the same |

`transducers` times out in every build (its reference says so: 200,000 test.check trials
through the evaluator); it needs step 7b.

### After merging main (the namespaces' image, U4)

Main's executable (`ad06cda`, with the image: `arbace.core` and the namespaces decoded, not
read and analyzed) against this branch merged with it; same method:

| | main | this branch | speedup |
|---|---:|---:|---:|
| `-e nil` | 0.35 s | 0.27 s | 1.30 |
| `-e nil` CPU | 0.57 s | 0.33 s | 1.74 |
| `-e nil` resident | 152 MB | 208 MB | |
| reduce-range | 15,645 | 10,464 | 1.50 |
| map-inc | 6,976 | 4,741 | 1.47 |
| into-xf | 7,273 | 5,228 | 1.39 |
| hashmap | 1,155 | 700 | 1.65 |
| vec-conj | 1,989 | 1,332 | 1.49 |
| str | 2,237 | 1,491 | 1.50 |
| fib | 620 | 400 | 1.55 |
| loop | 7,302 | 4,838 | 1.51 |
| binding | 1,268 | 788 | 1.61 |
| sort | 6,815 | 4,764 | 1.43 |
| keyword-map | 2,376 | 1,456 | 1.63 |
| atom-swap | 2,303 | 1,583 | 1.45 |
| lazy-seq-str | 549 | 357 | 1.54 |
| `w.clj` total | 58.1 s | 38.7 s | 1.50 |
| `w.clj` CPU | 174 s | 56.3 s | 3.09 |

### After merging step 7b (closure compilation)

Main at `48ba45c` (step 7b's compiled evaluator, cf-repl, jbase, go-net, regex-res) against this
branch merged with it (`bfedb18`); same method, ms unless noted. With the evaluator 6.8 times
faster on this script (58.1 s to 8.6 s on main), the runtime's share grew; this branch's part is now 1.25 times on the whole script
(up to 1.7 on maps and vectors) and half the CPU:

| | main | this branch | speedup |
|---|---:|---:|---:|
| `-e nil` | 0.22 s | 0.20 s | 1.10 |
| `-e nil` CPU | 0.46 s | 0.20 s | 2.30 |
| `-e nil` resident | 124 MB | 154 MB |  |
| reduce-range | 2,420 | 2,088 | 1.16 |
| map-inc | 1,215 | 954 | 1.27 |
| into-xf | 1,131 | 943 | 1.20 |
| hashmap | 583 | 344 | 1.69 |
| vec-conj | 603 | 381 | 1.58 |
| str | 414 | 342 | 1.21 |
| fib | 61 | 54 | 1.13 |
| loop | 144 | 144 | 1.00 |
| binding | 118 | 85 | 1.39 |
| sort | 908 | 707 | 1.28 |
| keyword-map | 131 | 126 | 1.04 |
| atom-swap | 329 | 268 | 1.23 |
| lazy-seq-str | 118 | 88 | 1.34 |
| `w.clj` total | 8.58 s | 6.89 s | 1.25 |
| `w.clj` CPU | 17.1 s | 8.6 s | 1.99 |

The executable: 86,085,515 bytes on main, 86,338,259 here (+0.3%).

### Size

The amd64 executable: 58,378,956 bytes before, 58,733,769 after (+0.6%: the size-class
instantiations of the array and string buffers, the profiling hooks), 58,925,445 with PGO
(+0.9%, which inlines more).

## Checks

Before merging main (the branch at `14a9553`, linux/amd64 unless said):

| check | result |
|---|---|
| `bin/jrt test` amd64, arm64 (qemu), `--race` | pass (arm64 after the test's fix above) |
| `bin/c2g-check -- --program` | 9,010 of 9,010 steps on each architecture |
| `bin/c2g-regex` | 1,204 of 1,233 cases, 11,365 of 11,682 inputs, as on main |
| `bin/c2g-evalproof` | as expected on amd64 and arm64 |
| the oracle on the Go build | 20,221 of 20,253, as on main (the same 32 mismatches) |
| Clojure's suite on the Go build | no regressions against `test/arbace-go-results.edn` (18,781 of 18,806) |
| `bin/arbace-go --smoke` | pass (the `--pgo` build) |

After merging main (`ad06cda`): `bin/jrt test` amd64 passes; the oracle 20,243 of 20,276
against `test/oracle/known-go-amd64.edn`: 33 known, 0 new, 0 now passing; `bin/arbace-go --smoke` passes.
`bin/gate` was not run: no source the bootstrap compiles changed (`arbace/c2g`, `arbace/g2c`
and `arbace/lang/go` are read by the tools, not compiled into the stages).

Found on the way: gc for linux/arm64 (go1.27.1) reports a *constant* negative index into a
slice whose length it does not know as index 0 (`index out of range [0] with length 0`; a
computed -5, or a known length, gives -5; amd64 is right). jrt's `TestRuntimeErrorMessages`
indexed a `new Object[0]` with a constant -5 and passed only because the array's length was
known at compile time; with the arrays of item 2 it is not, so the test now computes the index.
A Java program indexing with a negative constant would show `Index 0` in the message on arm64.

## Amendments to C2G-SPEC (decided by the user 2026-10-10)

Letter O: every letter appears somewhere in doc/; O only as C2G-SPEC §4.4's `O1` (the Go name of an `Object[]` parameter), never for amendments.

- **O1 (§13.4, D7) The collector's settings.** jrt's package initialization sets `GOGC=200`
  unless `GOGC` is set, and a 64 MiB minimum heap (a ballast without pointers, never written)
  unless `ARBACE_MIN_HEAP_MB` sets another (0: none); the main package's start-up setting
  (U4: `GOGC=400` until `arbace.main` is loaded) then raises it and restores jrt's. D7's
  "planned" paragraph becomes this rule, with the measurements above. *Accepted 2026-10-10, folded into C2G-SPEC §13.4 (closing D7, B1-PLAN.md).*

- **O2 (§5.9, §13.2, D7) Reference arrays and strings in one allocation.** A reference array of
  up to 32 slots and a `String` of up to 108 code units are one Go object, the slots or units
  after the header (`unsafe.Slice` over a trailing array, the length rounded to Go's size
  classes); `new StringBuilder()` holds its first 16 units in itself. The Go types stay
  (`*jrt.RefArray`, `*jrt.String`, a slice `A`/`value`); c2g's output does not change. *Accepted 2026-10-10, folded into C2G-SPEC §5.9 and §7.5.*

- **O3 (§3.2, §5.11) Shared parameter classes (new deviation V14).**
  `Method.getParameterTypes()` and `Constructor.getParameterTypes()` return the same array on
  every call (Java: a fresh copy). Nothing in the closed world writes to it; a program that
  does would change the method's reported parameters. Alternative: keep Java's contract and let
  step 7b's evaluator stop asking for the array on every call (then this deviation can go). *Not taken (the user, 2026-10-10): `getParameterTypes()` keeps Java's contract, a fresh copy; reverted.*

- **O4 (§5.5, §5.7, §5.8, §5.12) The dynamic flag.** Bit 31 of the header's low word marks
  objects of classes made at run time (set by `MarkDynamic` when c2g's `Dyn` and `DynSub_C`
  objects are made; the identity hash keeps its 31 bits); interface `instanceof` and checkcast
  ask `jrt.Dynamic` only for flagged objects (`jrt.IsDynamic`, which reads the header through
  the interface's data word: every Java object is a pointer to a struct whose first field is
  the header, §5.2). *Accepted 2026-10-10, folded into C2G-SPEC §5.7 and §5.8.*

- **O5 (§13.4, §13.6, BUILD.md) Profile-guided builds.** `bin/g2c build --pgo FILE` and
  `bin/arbace-go --build --pgo` (a training run of `test/arbace-go-pgo.clj` with
  `ARBACE_CPUPROFILE`, then `go build -pgo`). Proposed: the freeze's executables are built so;
  the default build stays without, for build time. *Accepted 2026-10-10 (the freeze's executables are built with `--pgo`; ordinary builds stay without), folded into C2G-SPEC §13.4.*

- **O6 (§7.9.2, §7.9.4) A `try` with nothing to catch** (no catch clause naming a class of the
  world, no normal-completion code) has no function literal of its own: its body is in place,
  inside the `finally`'s literal when there is one. *Accepted 2026-10-10, folded into C2G-SPEC §7.9.2.*

- **O7 (§9.4) Profiles.** `ARBACE_CPUPROFILE=FILE` and `ARBACE_MEMPROFILE=FILE` write Go's CPU
  and heap profiles of any program run under `jrt.RunMain`; allocation sampling is off
  otherwise. *Accepted 2026-10-10, folded into C2G-SPEC §13.4.*

## For step 7b (the evaluator)

What the profiles show above jrt, for the evaluator's step: a static or instance method call
goes through `Reflector.invokeMatchingMethod` and `Method.invoke` on every evaluation (the
matching, `boxArgs`' array, the invoker's `[]any`, the result boxed), about 10 allocations per
call of `(+ acc i)`; an `EvalFn` invocation allocates its `Frame` and an argument array; the
`(loop ...)` of `w.clj` costs 1.6 µs per iteration against the JVM's 3 ns. Direct invokers per
resolved method (the member tables' `Invoke` functions called with the evaluated arguments,
without `Method.invoke`) and frames sized at analysis would remove most of it.

## Open problems

- A script that sends to an agent and does not call `(shutdown-agents)` ends, after the
  pool's 60 s keep-alive, with Go's `fatal error: all goroutines are asleep - deadlock!` (the
  JVM exits once its pool threads time out): `jrt.WaitNonDaemon` waits for the pool's
  non-daemon workers, which block on their queue forever (step 6's executable).
- gc for linux/arm64 reports a constant negative index into a slice of unknown length as index
  0 (above).

## Sources

Go 1.27.1 (`/root/tamago-go`): `runtime/mgc.go` and `mgcpacer.go` (the heap goal from `GOGC`
and the 4 MiB minimum), `runtime/debug.SetGCPercent`, `runtime.MemProfileRate`,
`cmd/compile`'s PGO (`-pgo`, devirtualization of hot interface calls); the ballast technique
as Twitch described it (2019) for Go before `GOMEMLIMIT`.
