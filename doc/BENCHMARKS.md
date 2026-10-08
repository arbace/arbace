# Benchmarks

Arbace against upstream Clojure 1.12 and the frozen baseline `clojure/`, all on the same JDK 26,
with `bin/arbace-bench` (pre-freeze item 4). Measured on 2026-10-08 at Arbace `fc77cba` with
the AOT cache trained without method profiles (the change of that day, committed as `fb8d58e`;
see "The AOT cache's method profiles"); the first run, on 2026-10-07 at `e311a30` with
profiles, is summarized there.

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
  - *Load time*: `(require LIB)` timed inside a fresh JVM (a script), three times as many forks
    per library as steady state has (15 in the run below); median and spread. Libraries that
    the launcher already loads (`spec.alpha`, `string`, `edn`, `walk`, ...) are not measured.
  - *Steady state*: each benchmark in its own fresh JVM per fork (`--forks`, default 3; 5 in
    the run below). In a fork the benchmark's thunk runs for 3 s of warm-up, while the calls
    per sample are doubled until a sample takes at least 20 ms, then samples are taken for 3 s
    (at least 10). A sample's value is its time divided by its calls; the fork's value is its
    median sample. Reported: the median of the fork values, and their spread (min, max). Every
    result goes into a volatile, so the JIT cannot drop the work.
  - Forks are interleaved: round *r* runs every implementation once, in a rotating order, so
    changes of machine load fall on all of them alike. The 1-minute load average is recorded
    with every launch.
- **JVM options**: steady state uses `-Xms2g -Xmx2g -XX:+UseCompactObjectHeaders` for all three
  (Arbace's launcher uses compact headers by default). Startup and load use each
  implementation as shipped: `bin/arbace` (the jar, compact headers, the AOT cache), and
  `java -cp ... clojure.main` for Clojure.
- **Ratios** are Arbace / Clojure 1.12.6 and baseline / Clojure 1.12.6: below 1 is faster than
  Clojure 1.12.6, above 1 slower.

Run it with `bin/arbace-bench` (about 75 minutes with `--forks 5`; `--quick` for a smoke test
in about 8; `--forks`, `--warmup`, `--measure`, `--sections`, `--only`), after
`bin/build-arbace`. It writes raw results to `.tmp/bench/results-DATE.edn`, and
`bin/arbace-bench report FILE` prints the tables below from them.

## What was measured

- **Arbace** `fc77cba` with the training change of `fb8d58e`: `bin/arbace`, i.e.
  `target/arbace.jar` with the JDK AOT cache `target/arbace.aot`, both made by
  `bin/build-arbace`. Its namespaces are AOT-compiled with direct linking. Also
  `arbace-nocache`: the same jar with `ARBACE_AOT=off`.
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

The run below (`--forks 5`, 2026-10-08 05:34-06:50 UTC) was taken in a quiet period: the load
average stayed between 1 and 7. Ratios below 1 are faster than Clojure 1.12.6.

Startup: wall time of a launch, median of 20 runs and its spread (min, max); load average min / median / max 3 / 4 / 6.

| implementation | `-e 1` | spread | REPL session | spread |
|---|---:|---:|---:|---:|
| `arbace` | 201 ms | -8/+16 % | 457 ms | -7/+12 % |
| `arbace-nocache` | 554 ms | -4/+9 % | 989 ms | -6/+6 % |
| `clojure` | 558 ms | -5/+9 % | 1039 ms | -5/+9 % |
| `clojure-aot` | 202 ms | -6/+18 % | 501 ms | -4/+11 % |
| `baseline` | 470 ms | -4/+6 % | 818 ms | -6/+11 % |
| `baseline-src` | 2085 ms | -6/+6 % | 2613 ms | -3/+6 % |

Load time: `(require LIB)` in a fresh JVM (a script), median over forks and its spread; load average 5 / 5 / 6.

| library | `arbace` | `clojure` | `clojure-aot` | `baseline` |
|---|---:|---:|---:|---:|
| pprint | 17.9 ms -5/+7 % | 69.6 ms -1/+4 % | 15.8 ms -7/+8 % | 70.8 ms -5/+5 % |
| spec.test.alpha | 36.4 ms -8/+12 % | 86.6 ms -4/+9 % | 33.8 ms -5/+16 % | n/a |
| test | 8.2 ms -9/+45 % | 23.7 ms -7/+6 % | 7.5 ms -6/+67 % | 24.6 ms -5/+21 % |
| reflect | 30.2 ms -7/+8 % | 29.2 ms -10/+7 % | 33.6 ms -7/+10 % | 25.3 ms -6/+16 % |
| data | 5.6 ms -10/+38 % | 13.4 ms -8/+17 % | 5.4 ms -6/+48 % | 13.7 ms -8/+13 % |
| xml | 12.4 ms -6/+3 % | 8.9 ms -2/+32 % | 13.5 ms -16/+20 % | 8.2 ms -6/+6 % |

Steady state: time of one call, the median over forks of each fork's median sample, and the spread of the forks' medians; load average 1 / 2 / 7.

| benchmark | `arbace` | `clojure` | `baseline` | arbace / clojure | baseline / clojure |
|---|---:|---:|---:|---:|---:|
| `vector-conj` | 1.48 ms -1/+8 % | 1.53 ms -0/+3 % | 1.51 ms -0/+1 % | 0.97 | 0.98 |
| `vector-transient` | 628 µs -19/+5 % | 629 µs -0/+4 % | 604 µs -1/+1 % | 1.00 | 0.96 |
| `vector-nth` | 775 µs -0/+0 % | 850 µs -1/+1 % | 796 µs -1/+0 % | 0.91 | 0.94 |
| `vector-assoc` | 724 µs -0/+3 % | 727 µs -0/+2 % | 738 µs -2/+4 % | 1.00 | 1.02 |
| `map-assoc` | 1.01 ms -0/+3 % | 1.02 ms -0/+1 % | 1.03 ms -1/+0 % | 0.99 | 1.01 |
| `map-transient` | 646 µs -0/+9 % | 647 µs -1/+2 % | 650 µs -0/+1 % | 1.00 | 1.01 |
| `map-get` | 333 µs -1/+34 % | 449 µs -18/+1 % | 270 µs -10/+15 % | 0.74 | 0.60 |
| `map-update` | 1.20 ms -0/+1 % | 1.19 ms -1/+3 % | 1.02 ms -1/+4 % | 1.01 | 0.86 |
| `set-into-contains` | 1.04 ms -0/+8 % | 1.17 ms -13/+22 % | 1.01 ms -4/+6 % | 0.88 | 0.86 |
| `lazy-seq-walk` | 2.39 ms -2/+1 % | 2.40 ms -1/+1 % | 1.73 ms -0/+0 % | 1.00 | 0.72 |
| `lazy-for` | 143 µs -1/+1 % | 141 µs -1/+2 % | 144 µs -0/+5 % | 1.01 | 1.02 |
| `seq-ops` | 5.80 ms -0/+1 % | 5.72 ms -3/+0 % | 5.60 ms -0/+0 % | 1.02 | 0.98 |
| `reduce-vector` | 337 µs -0/+2 % | 339 µs -1/+0 % | 324 µs -0/+1 % | 0.99 | 0.96 |
| `transduce` | 819 µs -0/+0 % | 838 µs -1/+0 % | 816 µs -0/+0 % | 0.98 | 0.97 |
| `into-xform` | 736 µs -22/+6 % | 562 µs -2/+5 % | 748 µs -0/+0 % | 1.31 | 1.33 |
| `keyword-map` | 3.56 ms -9/+1 % | 4.66 ms -0/+8 % | 5.06 ms -3/+3 % | 0.76 | 1.08 |
| `keyword-record` | 3.24 ms -0/+1 % | 3.24 ms -0/+1 % | 3.85 ms -0/+2 % | 1.00 | 1.19 |
| `protocol` | 11.1 ms -9/+26 % | 15.3 ms -18/+2 % | 10.4 ms -1/+0 % | 0.72 | 0.68 |
| `multimethod` | 2.70 ms -1/+1 % | 2.90 ms -0/+7 % | 3.12 ms -11/+1 % | 0.93 | 1.07 |
| `str-concat` | 1.33 ms -0/+2 % | 14.2 ms -1/+0 % | 14.0 ms -0/+1 % | 0.09 | 0.99 |
| `str-apply` | 3.05 ms -0/+1 % | 2.63 ms -0/+9 % | 2.57 ms -1/+0 % | 1.16 | 0.98 |
| `interop-reflective` | 750 µs -0/+0 % | 437 ms -3/+0 % | 438 ms -1/+1 % | 0.00 | 1.00 |
| `interop-hinted` | 480 µs -0/+0 % | 477 µs -0/+0 % | 460 µs -1/+0 % | 1.00 | 0.96 |
| `prim-long-loop` | 4.71 ms -13/+2 % | 4.12 ms -0/+14 % | 4.70 ms -12/+0 % | 1.14 | 1.14 |
| `prim-double-loop` | 2.82 ms -0/+0 % | 2.82 ms -0/+0 % | 2.82 ms -0/+0 % | 1.00 | 1.00 |
| `prim-array` | 441 µs -0/+4 % | 441 µs -0/+0 % | 441 µs -0/+3 % | 1.00 | 1.00 |
| `atom-swap` | 5.44 ms -0/+0 % | 5.81 ms -1/+0 % | 5.48 ms -0/+1 % | 0.93 | 0.94 |
| `atom-contended` | 233 ms -7/+1 % | 217 ms -2/+4 % | 205 ms -6/+14 % | 1.07 | 0.94 |
| `future` | 3.72 ms -4/+1 % | 3.49 ms -0/+10 % | 3.60 ms -1/+9 % | 1.07 | 1.03 |
| `pmap` | 457 µs -1/+4 % | 452 µs -4/+2 % | 453 µs -1/+5 % | 1.01 | 1.00 |
| `regex` | 3.13 ms -2/+2 % | 3.13 ms -1/+2 % | 3.31 ms -0/+2 % | 1.00 | 1.06 |
| `string-lib` | 1.32 ms -1/+1 % | 1.33 ms -2/+0 % | 1.34 ms -0/+9 % | 0.99 | 1.01 |
| `eval-macros` | 8.33 ms -1/+3 % | 8.26 ms -0/+0 % | 7.00 ms -1/+4 % | 1.01 | 0.85 |

Geometric mean of the ratios: arbace / clojure 0.75, baseline / clojure 0.96
Without `interop-reflective` and `str-concat` (the two that Arbace's call sites change by 10x or more): arbace / clojure 0.98, baseline / clojure 0.96

## Reading the results

**Startup.** `bin/arbace -e 1` takes 201 ms against 558 ms for Clojure 1.12.6, and the REPL
session 457 ms against 1,039 ms. That gain is the JDK's AOT cache (JEP 483, 514), not
Arbace's code: given a cache trained the same way, Clojure 1.12.6 starts as fast (202 ms,
501 ms), and Arbace without its cache is as slow as Clojure without one (554 ms). What Arbace
adds is that the cache is built, checked and used by default (`bin/build-arbace`,
`bin/arbace`). The frozen tree as it is (no compiled namespaces) takes 2.1 s.

**Load time.** The same holds for loading libraries after startup: with the cache, `pprint`,
`spec.test.alpha`, `test` and `data` load 2.4 to 4 times faster, for Arbace and for Clojure
with a cache alike, because the cache holds their classes already loaded and linked.
Libraries outside the training workload (`reflect`, `xml`) gain nothing; `xml` is about 40 %
slower with either cache.

**Steady state**, by what each benchmark exercises:

- *Where Arbace's own changes show*: un-hinted interop is about 580 times faster
  (`interop-reflective`, 0.75 ms against 437 ms for 300,000 calls: `invokedynamic` sites
  caching `Reflector`'s choice, `doc/VENDOR-NOTES.md` hand change 10); `str` with several
  arguments about 10 times (`str-concat`, `StringConcatFactory`, hand change 13); keyword
  lookups on maps 24 % (`keyword-map`, `invokedynamic` keyword sites, hand change 12).
- *Equal within the noise* (ratios 0.9 to 1.1): vectors (`vector-conj`, `-transient`, `-nth`,
  `-assoc`), persistent maps, lazy seqs, `seq-ops`, `reduce-vector`, `transduce`, keyword
  lookups on records, hinted interop, primitive double and array loops, atoms, futures, `pmap`,
  `multimethod`, `regex`, `string-lib` and `eval-macros` (the compiler itself).
- *Slower than Clojure 1.12.6*:
  - `into-xform` (`into []` through a transducer) 1.31x. It is bimodal: one Arbace fork ran at
    Clojure's speed (576 µs), the others at 736-783 µs, and the frozen baseline, the same Java
    code compiled by javac 26, is as slow (1.33x). Clojure 1.12.6's jar (class files for Java 8)
    is fast in every fork. So this is not the class forms compiler's doing; why HotSpot settles
    differently for the newer class files is open.
  - `str-apply` (`apply str` over a seq) 1.16x, steady over forks (3.05 ms against 2.63 ms; the
    baseline 2.57 ms). With the profiled cache it was 1.00; the cause is open.
  - `prim-long-loop` 1.14x: bimodal for all three (forks at 4.1 or 4.7 ms), not a difference.
- *Faster than Clojure 1.12.6 without a known Arbace change*: `protocol` 0.72x, `map-get`
  0.74x, `set-into-contains` 0.88x. The frozen baseline (1.13 master) is as fast or faster on
  these too, so they reflect Clojure's code or JIT luck rather than Arbace. `protocol` is
  bimodal: separate forks of the same implementation settle at about 10.7, 15.6 or 20 ms.

Taken together, without the two benchmarks that Arbace's call sites change by 10x or more,
Arbace runs at Clojure 1.12.6's speed (geometric mean 0.98, baseline 0.96).

## The AOT cache's method profiles

The first run (2026-10-07, Arbace `e311a30`) used a cache trained with method profiles, the
default of `-XX:AOTCacheOutput` (JEP 515). Three benchmarks were slower in all its runs:
`into-xform` 1.61x, `reduce-vector` 1.17x, `vector-transient` 1.15x. The call sites' bytecode
matched Clojure's, the runtime methods differed from javac's by a few bytes, and HotSpot
inlined the same methods.

The cause was the cache's profiles. In a focused check (`into-xform`, `reduce-vector`,
`vector-transient`, 4 s of warm-up, 4 interleaved rounds), Arbace with the profiled cache took
about 930, 390 and 855 µs; the same cache with the profiles ignored at run time
(`-XX:+UnlockDiagnosticVMOptions -XX:-AOTReplayTraining`) took about 745, 380 and 680 µs, as
Arbace without a cache did, and close to Clojure 1.12.6. The JIT trusts profiles gathered in
the short training session, which do not fit long hot loops. A cache trained without profiles
(`-XX:-AOTRecordTraining`) loads as fast (0.2 s) and is 2 MB smaller; it costs a short REPL
session about 40 ms of warm-up (450 against 410 ms). At the user's decision (2026-10-08) the
cache is now made that way (`bin/arbace --aot-train`), and so is the `clojure-aot` cache here.
This run shows the result: `reduce-vector` 0.99x and `vector-transient` 1.00x. The profiles
had helped a few benchmarks, which are now even with Clojure: `keyword-record` (0.80x then),
`str-apply` (1.00x then, 1.16x now) and `keyword-map` (0.67x then, 0.76x now).

The first run's tables, for reference (steady state, ratio to Clojure 1.12.6, where it differs
from this run by more than 0.1): `into-xform` 1.61, `reduce-vector` 1.17, `vector-transient`
1.15, `keyword-record` 0.80, `map-get` 1.05, `str-apply` 1.00. Startup was 184 ms (`-e 1`) and
417 ms (REPL session), the latter about 40 ms faster with the profiles. Its raw results are
not tracked (`.tmp/bench/full2.edn`).

## Noise

- The machine is a shared 64-core VM; another agent's builds raised the load average to over
  300 at times. Under load the spread of fork medians reached +150 % and single benchmarks
  moved by 2x; the quiet run above has spreads of a few % for most benchmarks. Compare only
  runs with a similar load average (it is printed for each section).
- Wide spreads that remain in the quiet run (`map-get` +34 %, `protocol` +26 %, `into-xform`
  -22 %, `vector-transient` -19 %; `set-into-contains`, `transduce` and `pmap` in other runs)
  come from forks whose JIT took another path, not from load. Five forks and medians tame them
  but do not remove them.
- `atom-contended`, `future` and `pmap` depend on thread scheduling and are the most sensitive
  to other load on the machine.
- Startup and load times include the bash launcher for `bin/arbace` (a few ms).
