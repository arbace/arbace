# Benchmarks

Arbace against upstream Clojure 1.12 and the frozen baseline `clojure/`, all on the same JDK 26,
with `bin/arbace-bench` (pre-freeze item 4). Measured on 2026-10-07 at Arbace `e311a30`.

## Method

- **One workload file, run unchanged by all three**: `test/bench/workload.clj`. It uses only the
  core names referred into `user` and reaches other libraries through a helper that picks
  `clojure.string` or `arbace.string`, so the same source is read, compiled and run by each
  implementation as a script.
- **A small own harness**, no external libraries. The driver `test/bench/driver.clj` (run by the
  frozen `clojure/`, never measured itself) launches every measured JVM, one at a time:
  - *Startup*: the wall time of a whole launch (`-e 1`, and a 17-form REPL session on stdin,
    `test/bench/repl-session.clj`), 3 warm-up launches, then 20 timed ones; median and the
    spread (min, max) around it.
  - *Load time*: `(require LIB)` timed inside a fresh JVM (a script), 15 forks per library;
    median and spread. Libraries that the launcher already loads (`spec.alpha`, `string`, `edn`,
    `walk`, ...) are not measured.
  - *Steady state*: each benchmark in its own fresh JVM per fork (5 forks). In a fork the
    benchmark's thunk runs for 3 s of warm-up, while the calls per sample are doubled until a
    sample takes at least 20 ms, then samples are taken for 3 s (at least 10). A sample's
    value is its time divided by its calls; the fork's value is its median sample. Reported:
    the median of the 5 fork values, and their spread (min, max). Every result goes into a
    volatile, so the JIT cannot drop the work.
  - Forks are interleaved: round *r* runs every implementation once, in a rotating order, so
    changes of machine load fall on all of them alike. The 1-minute load average is recorded
    with every launch.
- **JVM options**: steady state uses `-Xms2g -Xmx2g -XX:+UseCompactObjectHeaders` for all three
  (Arbace's launcher uses compact headers by default). Startup and load use each
  implementation as shipped: `bin/arbace` (the jar, compact headers, the AOT cache), and
  `java -cp ... clojure.main` for Clojure.
- **Ratios** are Arbace / Clojure 1.12.6 and baseline / Clojure 1.12.6: below 1 is faster than
  Clojure 1.12.6, above 1 slower.

Run it with `bin/arbace-bench` (about 75 minutes; `--quick` for a smoke test in about 8;
`--forks`, `--warmup`, `--measure`, `--sections`, `--only`), after `bin/build-arbace`. It
writes raw results to `.tmp/bench/results-DATE.edn`, and `bin/arbace-bench report FILE` prints
the tables below from them.

## What was measured

- **Arbace** `e311a30`: `bin/arbace`, i.e. `target/arbace.jar` with the JDK AOT cache
  `target/arbace.aot`, both made by `bin/build-arbace`. Its namespaces are AOT-compiled with
  direct linking. Also `arbace-nocache`: the same jar with `ARBACE_AOT=off`.
- **Clojure 1.12.6**, the latest 1.12.x release (2026-09-02; Maven Central's latest overall is
  1.13.0-alpha8), with the dependencies its pom names, from Maven Central into `.tmp/bench/lib/`:

  | artifact | SHA-256 |
  |---|---|
  | `org.clojure/clojure 1.12.6` | `21b578db389a8e74b286ce79c4a7f86ec641e26d88b12f18d69030916b0e18ca` |
  | `org.clojure/spec.alpha 0.5.238` | `94cd99b6ea639641f37af4860a643b6ed399ee5a8be5d717cff0b663c8d75077` |
  | `org.clojure/core.specs.alpha 0.4.74` | `eb73ac08cf49ba840c88ba67beef11336ca554333d9408808d78946e0feb9ddb` |

  Also `clojure-aot`: the same jars with a JDK AOT cache trained like Arbace's (on
  `test/aot-training.clj` renamed to `clojure.*`, without its `defclass`), to tell the JDK's
  share of Arbace's startup from Arbace's own.
- **The frozen baseline** `clojure/` (upstream 1.13 master `98d735f`, spec stubbed out):
  `baseline` has its 35 namespaces AOT-compiled with direct linking into
  `.tmp/bench/baseline-classes/`, as upstream's release build does, so it compares with the
  two jars. `baseline-src` is the tree as it is, compiling `clojure.core` from source at every
  launch (startup only).

**Machine**: AMD EPYC 7763, 64 cores (a VM), 62 GB, Alpine Linux edge (kernel 6.18.52). The
machine is shared with other agents' builds. **JDK**: OpenJDK 26.0.2.1 built from jdk26u
(`26.0.2.1-internal-adhoc.root.jdk26u`), default G1 collector.

## Results

The run below (`--forks 5`, 21:33-22:38 UTC) was taken in a quiet period: the load average
stayed between 1 and 8. An earlier run with 3 forks under load averages of 8 to 222 (another
agent compiling jdk26u) agreed on the large differences but had spreads up to +150 %, and it
put `protocol` at 2x slower for Arbace; see "Noise" below.

Startup: wall time of a launch, median of 20 runs and its spread (min, max); load average min / median / max 4 / 5 / 7.

| implementation | `-e 1` | spread | REPL session | spread |
|---|---:|---:|---:|---:|
| `arbace` | 184 ms | -5/+12 % | 417 ms | -6/+9 % |
| `arbace-nocache` | 573 ms | -5/+5 % | 1007 ms | -5/+9 % |
| `clojure` | 571 ms | -5/+9 % | 1070 ms | -5/+6 % |
| `clojure-aot` | 190 ms | -7/+8 % | 460 ms | -7/+10 % |
| `baseline` | 478 ms | -4/+6 % | 834 ms | -6/+11 % |
| `baseline-src` | 2219 ms | -11/+5 % | 2792 ms | -4/+7 % |

Load time: `(require LIB)` in a fresh JVM (a script), median over forks and its spread; load average 4 / 5 / 6.

| library | `arbace` | `clojure` | `clojure-aot` | `baseline` |
|---|---:|---:|---:|---:|
| pprint | 15.4 ms -13/+21 % | 74.1 ms -5/+7 % | 12.3 ms -15/+57 % | 74.4 ms -6/+16 % |
| spec.test.alpha | 30.3 ms -7/+10 % | 88.9 ms -7/+10 % | 28.7 ms -9/+26 % | n/a |
| test | 6.4 ms -6/+140 % | 25.1 ms -13/+15 % | 6.0 ms -7/+50 % | 25.5 ms -3/+19 % |
| reflect | 27.5 ms -9/+16 % | 28.7 ms -6/+42 % | 28.4 ms -7/+16 % | 27.7 ms -10/+12 % |
| data | 4.8 ms -10/+48 % | 14.1 ms -7/+39 % | 4.4 ms -11/+49 % | 14.6 ms -12/+9 % |
| xml | 10.2 ms -13/+38 % | 9.2 ms -12/+34 % | 9.7 ms -9/+27 % | 8.8 ms -9/+47 % |

Steady state: time of one call, the median over forks of each fork's median sample, and the spread of the forks' medians; load average 1 / 2 / 8.

| benchmark | `arbace` | `clojure` | `baseline` | arbace / clojure | baseline / clojure |
|---|---:|---:|---:|---:|---:|
| `vector-conj` | 1.50 ms -2/+2 % | 1.54 ms -1/+1 % | 1.56 ms -1/+2 % | 0.97 | 1.01 |
| `vector-transient` | 725 µs -0/+1 % | 632 µs -1/+2 % | 597 µs -0/+0 % | 1.15 | 0.95 |
| `vector-nth` | 815 µs -1/+3 % | 849 µs -0/+0 % | 793 µs -2/+0 % | 0.96 | 0.93 |
| `vector-assoc` | 742 µs -2/+3 % | 737 µs -3/+4 % | 716 µs -1/+13 % | 1.01 | 0.97 |
| `map-assoc` | 1.02 ms -1/+2 % | 1.03 ms -1/+1 % | 1.04 ms -2/+4 % | 0.99 | 1.01 |
| `map-transient` | 658 µs -7/+1 % | 666 µs -10/+4 % | 671 µs -3/+6 % | 0.99 | 1.01 |
| `map-get` | 426 µs -14/+22 % | 406 µs -26/+15 % | 288 µs -11/+1 % | 1.05 | 0.71 |
| `map-update` | 1.06 ms -2/+1 % | 1.16 ms -2/+10 % | 1.07 ms -5/+14 % | 0.92 | 0.93 |
| `set-into-contains` | 1.03 ms -2/+61 % | 1.24 ms -23/+19 % | 1.02 ms -2/+3 % | 0.83 | 0.82 |
| `lazy-seq-walk` | 2.40 ms -1/+4 % | 2.37 ms -1/+6 % | 1.77 ms -2/+2 % | 1.01 | 0.75 |
| `lazy-for` | 130 µs -2/+2 % | 144 µs -3/+1 % | 141 µs -2/+3 % | 0.91 | 0.98 |
| `seq-ops` | 5.82 ms -1/+1 % | 5.65 ms -1/+3 % | 5.62 ms -3/+0 % | 1.03 | 0.99 |
| `reduce-vector` | 398 µs -0/+0 % | 342 µs -1/+7 % | 328 µs -0/+0 % | 1.17 | 0.96 |
| `transduce` | 838 µs -1/+1 % | 834 µs -1/+1 % | 820 µs -1/+1 % | 1.00 | 0.98 |
| `into-xform` | 913 µs -0/+0 % | 566 µs -3/+13 % | 743 µs -0/+1 % | 1.61 | 1.31 |
| `keyword-map` | 3.23 ms -0/+9 % | 4.84 ms -4/+6 % | 5.18 ms -4/+6 % | 0.67 | 1.07 |
| `keyword-record` | 2.61 ms -0/+2 % | 3.25 ms -1/+1 % | 3.88 ms -1/+3 % | 0.80 | 1.19 |
| `protocol` | 10.7 ms -2/+47 % | 15.5 ms -6/+0 % | 10.1 ms -1/+4 % | 0.69 | 0.65 |
| `multimethod` | 2.71 ms -0/+10 % | 3.03 ms -5/+2 % | 2.89 ms -7/+11 % | 0.89 | 0.95 |
| `str-concat` | 1.38 ms -4/+2 % | 14.2 ms -0/+0 % | 14.0 ms -1/+1 % | 0.10 | 0.99 |
| `str-apply` | 2.62 ms -0/+5 % | 2.63 ms -0/+2 % | 2.57 ms -1/+2 % | 1.00 | 0.98 |
| `interop-reflective` | 739 µs -0/+1 % | 432 ms -1/+5 % | 442 ms -1/+1 % | 0.00 | 1.02 |
| `interop-hinted` | 484 µs -1/+5 % | 478 µs -0/+6 % | 457 µs -0/+1 % | 1.01 | 0.96 |
| `prim-long-loop` | 4.71 ms -0/+2 % | 4.71 ms -0/+1 % | 4.14 ms -0/+14 % | 1.00 | 0.88 |
| `prim-double-loop` | 2.82 ms -0/+0 % | 2.82 ms -0/+0 % | 2.82 ms -0/+0 % | 1.00 | 1.00 |
| `prim-array` | 442 µs -1/+3 % | 441 µs -0/+3 % | 454 µs -3/+2 % | 1.00 | 1.03 |
| `atom-swap` | 5.80 ms -0/+1 % | 5.80 ms -6/+0 % | 5.50 ms -1/+0 % | 1.00 | 0.95 |
| `atom-contended` | 207 ms -21/+6 % | 217 ms -2/+4 % | 207 ms -7/+9 % | 0.95 | 0.95 |
| `future` | 3.62 ms -3/+3 % | 3.59 ms -2/+3 % | 3.61 ms -1/+2 % | 1.01 | 1.00 |
| `pmap` | 523 µs -1/+2 % | 486 µs -4/+2 % | 480 µs -2/+3 % | 1.08 | 0.99 |
| `regex` | 2.83 ms -1/+2 % | 3.15 ms -3/+1 % | 3.30 ms -9/+4 % | 0.90 | 1.05 |
| `string-lib` | 1.41 ms -1/+0 % | 1.30 ms -1/+3 % | 1.33 ms -2/+4 % | 1.08 | 1.02 |
| `eval-macros` | 8.59 ms -2/+3 % | 8.22 ms -2/+3 % | 7.72 ms -12/+16 % | 1.04 | 0.94 |

Geometric mean of the ratios: arbace / clojure 0.75, baseline / clojure 0.96
Without `interop-reflective` and `str-concat` (the two that Arbace's call sites change by 10x or more): arbace / clojure 0.98, baseline / clojure 0.96

## Reading the results

**Startup.** `bin/arbace -e 1` takes 184 ms against 571 ms for Clojure 1.12.6, and the REPL
session 417 ms against 1,070 ms. That gain is the JDK's AOT cache (JEP 483, 514, 515), not
Arbace's code: given a cache trained the same way, Clojure 1.12.6 starts as fast (190 ms,
460 ms), and Arbace without its cache is as slow as Clojure without one (573 ms). What Arbace
adds is that the cache is built, checked and used by default (`bin/build-arbace`,
`bin/arbace`). The frozen tree as it is (no compiled namespaces) takes 2.2 s.

**Load time.** The same holds for loading libraries after startup: with the cache, `pprint`,
`spec.test.alpha`, `test` and `data` load 3 to 5 times faster, for Arbace and for Clojure
with a cache alike, because the cache holds their classes already loaded and linked.
Libraries outside the training workload (`reflect`, `xml`) gain nothing; `xml` is about 10 %
slower with Arbace (within its spread).

**Steady state**, by what each benchmark exercises:

- *Where Arbace's own changes show*: un-hinted interop is about 580 times faster
  (`interop-reflective`, 0.74 ms against 432 ms for 300,000 calls: `invokedynamic` sites
  caching `Reflector`'s choice, `doc/VENDOR-NOTES.md` hand change 10); `str` with several
  arguments about 10 times (`str-concat`, `StringConcatFactory`, hand change 13); keyword
  lookups 20-33 % (`keyword-map`, `keyword-record`, `invokedynamic` keyword sites, hand change
  12).
- *Equal within the noise* (ratios 0.9 to 1.1, spreads of a few %): persistent maps
  (`map-assoc`, `map-transient`, `map-get`, `map-update`), `vector-conj`/`-nth`/`-assoc`,
  lazy seqs, `seq-ops`, `transduce`, `str-apply`, hinted interop, primitive long/double/array
  loops, atoms (one thread and contended), futures, `multimethod`, `regex`, and `eval-macros`
  (the compiler itself: macroexpansion, analysis, ASM emission and class definition, +4 %).
- **Where Arbace is slower** than Clojure 1.12.6, in all three runs made (this one, the loaded
  3-fork run, and a 1-fork `--quick` run):
  - `into-xform` (`into []` through a transducer): **1.61x** here, 1.48x and 1.18x in the two
    other runs. A micro check (`.tmp`, not kept) found Arbace 10-25 % slower on the
    whole family: `(into [] v)`, `reduce conj!` into a transient, `reduce` with a fn.
  - `reduce-vector` (`(reduce + v)`): **1.17x** (1.20x and 1.13x).
  - `vector-transient` (`conj!` 100,000 times): **1.15x** (1.01x and 1.58x).
  - `string-lib` 1.08x, `pmap` 1.08x: within or near the noise.

  The cause is not found. The emitted bytecode of the hot paths was compared: the call sites
  (protocol, `reduce`) are identical, and `PersistentVector.reduce`, `TransientVector.conj`
  and `Numbers.ops` differ from javac's by a few bytes only (the class forms compiler writes
  `iload/iadd/istore` where javac writes `iinc`, and a branch for a boolean `instanceof`:
  `RT.isReduced` is 13 bytes against javac's 5). HotSpot inlined the same methods in both
  (`-XX:+PrintInlining` on `(reduce + v)`). Closing this is left to the class forms compiler
  work (comparing its method bodies with javac's beyond the class shapes).
- *Faster than Clojure 1.12.6 without a known Arbace change*: `protocol` 0.69x,
  `set-into-contains` 0.83x, `lazy-for` and `map-update` 0.91-0.92x. The frozen baseline (1.13 master) is as
  fast or faster on these too (`protocol` 0.65x, `map-get` 0.71x, `lazy-seq-walk` 0.75x), so
  they reflect Clojure's code or JIT luck rather than Arbace. `protocol` is bimodal: separate
  forks of the same implementation settled at about 10.7, 15.6 or 20 ms depending on the JIT's
  profile, which made it look 2x slower for Arbace in the loaded run.

Taken together, without the two benchmarks that Arbace's call sites change by 10x or more,
Arbace runs at Clojure 1.12.6's speed (geometric mean 0.98, baseline 0.96).

## Noise

- The machine is a shared 64-core VM; another agent's builds raised the load average to over
  300 at times. Under load the spread of fork medians reached +150 % and single benchmarks
  moved by 2x; the quiet run above has spreads of a few % for most benchmarks. Compare only
  runs with a similar load average (it is printed for each section).
- Wide spreads that remain in the quiet run (`set-into-contains` +61 %, `protocol` +47 %,
  `map-get`, `transduce` and `pmap` in other runs) come from forks whose JIT took another
  path, not from load. Five forks and medians tame them but do not remove them.
- `atom-contended`, `future` and `pmap` depend on thread scheduling and are the most sensitive
  to other load on the machine.
- Startup and load times include the bash launcher for `bin/arbace` (a few ms).
