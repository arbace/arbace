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
  strings, `StringBuilder`), or where reflection copied arrays on every call.
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

### 6. Reflective calls: no copies of the parameter classes or the arguments — kept

The evaluator calls static and instance methods through `Method.invoke` (step 7b will change
that); each call asked `getParameterTypes()` twice (the evaluator's `typedArgs`, the
Reflector's `boxArgs`), and Java's contract is a fresh array each time; `convertArgs` copied
the arguments into a new slice. Now `getParameterTypes()` returns one array per `Method`
(made on first use; a deviation, amendment O3) and `convertArgs` passes the arguments' own
slice unless a primitive parameter converts one. This removed 21% of the bytes the evaluator
workloads allocated.

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

## Proposed amendments to C2G-SPEC (for the user's review)

Letter O: every letter appears somewhere in doc/; O only as C2G-SPEC §4.4's `O1` (the Go name of an `Object[]` parameter), never for amendments.

- **O1 (§13.4, D7) The collector's settings.** jrt's package initialization sets `GOGC=200`
  unless `GOGC` is set, and a 64 MiB minimum heap (a ballast without pointers, never written)
  unless `ARBACE_MIN_HEAP_MB` sets another (0: none); the main package's start-up setting
  (U4: `GOGC=400` until `arbace.main` is loaded) then raises it and restores jrt's. D7's
  "planned" paragraph becomes this rule, with the measurements above.
- **O2 (§5.9, §13.2, D7) Reference arrays and strings in one allocation.** A reference array of
  up to 32 slots and a `String` of up to 108 code units are one Go object, the slots or units
  after the header (`unsafe.Slice` over a trailing array, the length rounded to Go's size
  classes); `new StringBuilder()` holds its first 16 units in itself. The Go types stay
  (`*jrt.RefArray`, `*jrt.String`, a slice `A`/`value`); c2g's output does not change.
- **O3 (§3.2, §5.11) Shared parameter classes (new deviation V14).**
  `Method.getParameterTypes()` and `Constructor.getParameterTypes()` return the same array on
  every call (Java: a fresh copy). Nothing in the closed world writes to it; a program that
  does would change the method's reported parameters. Alternative: keep Java's contract and let
  step 7b's evaluator stop asking for the array on every call (then this deviation can go).
- **O4 (§5.5, §5.7, §5.8, §5.12) The dynamic flag.** Bit 31 of the header's low word marks
  objects of classes made at run time (set by `MarkDynamic` when c2g's `Dyn` and `DynSub_C`
  objects are made; the identity hash keeps its 31 bits); interface `instanceof` and checkcast
  ask `jrt.Dynamic` only for flagged objects (`jrt.IsDynamic`, which reads the header through
  the interface's data word: every Java object is a pointer to a struct whose first field is
  the header, §5.2).
- **O5 (§13.4, §13.6, BUILD.md) Profile-guided builds.** `bin/g2c build --pgo FILE` and
  `bin/arbace-go --build --pgo` (a training run of `test/arbace-go-pgo.clj` with
  `ARBACE_CPUPROFILE`, then `go build -pgo`). Proposed: the freeze's executables are built so;
  the default build stays without, for build time.
- **O6 (§7.9.2, §7.9.4) A `try` with nothing to catch** (no catch clause naming a class of the
  world, no normal-completion code) has no function literal of its own: its body is in place,
  inside the `finally`'s literal when there is one.
- **O7 (§9.4) Profiles.** `ARBACE_CPUPROFILE=FILE` and `ARBACE_MEMPROFILE=FILE` write Go's CPU
  and heap profiles of any program run under `jrt.RunMain`; allocation sampling is off
  otherwise.

## For step 7b (the evaluator)

What the profiles show above jrt, for the evaluator's step: a static or instance method call
goes through `Reflector.invokeMatchingMethod` and `Method.invoke` on every evaluation (the
matching, `boxArgs`' array, the invoker's `[]any`, the result boxed), about 10 allocations per
call of `(+ acc i)`; an `EvalFn` invocation allocates its `Frame` and an argument array; the
`(loop ...)` of `w.clj` costs 1.6 µs per iteration against the JVM's 3 ns. Direct invokers per
resolved method (the member tables' `Invoke` functions called with the evaluated arguments,
without `Method.invoke`) and frames sized at analysis would remove most of it.

Also found: a script that sends to an agent and does not call `(shutdown-agents)` ends, after
the pool's 60 s keep-alive, with Go's `fatal error: all goroutines are asleep - deadlock!`
(the JVM exits once its pool threads time out): `jrt.WaitNonDaemon` waits for the pool's
non-daemon workers, which block on their queue forever (step 6's executable).

## Sources

Go 1.27.1 (`/root/tamago-go`): `runtime/mgc.go` and `mgcpacer.go` (the heap goal from `GOGC`
and the 4 MiB minimum), `runtime/debug.SetGCPercent`, `runtime.MemProfileRate`,
`cmd/compile`'s PGO (`-pgo`, devirtualization of hot interface calls); the ballast technique
as Twitch described it (2019) for Go before `GOMEMLIMIT`.
