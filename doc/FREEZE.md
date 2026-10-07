# Arbace for Java 26: the freeze

Arbace runs on the JVM today: a Clojure, written in Clojure, that compiles itself to `.class`
files for Java 26. Its long-term direction leaves the JVM and the `.class` format behind (see
the README's "Direction"). Before that break, the JVM state of the art is frozen on a
well-known branch:

- **branch `arbace-for-java-26`**, with the annotated tag **`arbace-for-java-26-v1`** on the
  same commit: Arbace on the JVM as it was on main at the freeze.

`bin/freeze` creates both, at the user's confirmation (see "Making the freeze" below). Until it
has run, this document describes what the branch will hold. After it, the branch is where
Arbace on the JVM lives; main moves on.

## What the branch holds

- **Arbace on Java 26**: the vendored Clojure (`arbace.*`, all `.clj`, the Java parts written as
  class forms), bootstrapped in three stages that rebuild each other byte for byte. Each stage
  holds its namespaces AOT-compiled with direct linking; every class passes the JDK's class
  file verifier at class file version 70. Clojure's upstream test suite passes in full on
  stages 1 and 2 (809 tests, 20,750 of 20,750 assertions, test.generative 27 of 27), clojure.spec
  included.
- **The modern-JVM work** (`doc/MODERN-COMPILER.md`): the reproducible `target/arbace.jar`, the
  JDK AOT cache (launch in about 0.2 s), `invokedynamic` reflective call sites, `invokedynamic`
  keyword sites, `StringConcatFactory` for `str`, an opt-in virtual-thread executor, and a
  self-contained `jlink` image (`bin/arbace-image`). Tried and not kept: `Atom` on a
  `VarHandle`, condy constants.
- **The class forms** (`doc/classes/SPEC.md`): every construct of a Java class file as
  idiomatic Clojure, native to Arbace (`defclass`, `method`, `switch`, `label`, ...), with their
  compiler `arbace/classes/` and the Java → class forms converter `arbace/j2c/` (`bin/j2c`). The
  converted `clojure/` compiles to javac's class shapes and passes Clojure's suite.
- **The frozen reference** `clojure/` (upstream Clojure 1.13 master `98d735f` and ASM): stage 0
  of the bootstrap, j2c's regression corpus and the suite's reference. It stays on this branch
  for good, so the branch builds and verifies from its own checkout.
- **The records**: `doc/JOURNAL.md` (how everything was derived), `doc/AGENDA.md`,
  `doc/VENDOR-NOTES.md` (the bootstrap, the jar, the cache, the image, every hand change),
  `doc/BENCHMARKS.md`, `LICENSE.md`.

## Build, run, verify

Needs a JDK 26 (`java`, `javac`, `jar`, `jlink` on the `PATH`), bash, git, curl, cpio and
unzip, on Linux. About 64 GB of memory and many cores make the checks fast, but a 2-core,
7 GB GitHub runner passes them too.

```sh
find clojure -name '*.class' -exec touch {} +   # once, in a fresh checkout
bin/build-arbace                 # stages 1-3, verifier, native tests, target/arbace.jar + .aot
bin/arbace                       # a REPL; also bin/arbace -e '(+ 1 2)', bin/arbace script.clj
bin/arbace-image                 # target/arbace-image: a self-contained runtime (131 MB)
```

**The gate**, the checks every change had to pass, run one after the other:

```sh
bin/build-arbace --suite         # the bootstrap, plus Clojure's suite on stages 1 and 2
bin/class-forms-tests            # the class forms compiler on the frozen clojure/
bin/j2c-check --suite            # the converter on clojure/ and its samples, and Clojure's
                                 # suite on the converted, recompiled clojure/
```

They take about 10 to 40 minutes on the development machine (64 cores, shared), and about 45
minutes on a 2-core GitHub runner. The gate
needs no path outside the checkout; it downloads the clojure/clojure test sources (a shallow
git fetch) and six test libraries from Maven Central, checked by SHA-1. The larger checks over
jdk26u (`bin/j2c` on `/root/jdk26u`, see `doc/classes/CONVERTER-NOTES.md`) need a jdk26u
checkout and are not part of the gate.

**CI**: `.github/workflows/gate.yml` runs the gate on Temurin JDK 26 (actions/setup-java) on
every push to `arbace-for-java-26`, and by hand on any ref (`gh workflow run gate.yml --ref
REF`). By the user's decision it does not run automatically on main: main's progress is not
tied to GitHub's infrastructure, and main is gated by running the checks locally. The
workflow caches the test libraries and uploads the suite logs when a step fails.

**Benchmarks**: `bin/arbace-bench` compares Arbace with upstream Clojure 1.12.6 and the frozen
`clojure/` (`doc/BENCHMARKS.md`).

## Benchmark summary

From `doc/BENCHMARKS.md` (2026-10-07, Arbace `e311a30`, JDK 26.0.2.1, a quiet 64-core
machine; ratios against Clojure 1.12.6 on the same JDK, below 1 is faster):

- **Startup**: `bin/arbace -e 1` 184 ms against 571 ms, a REPL session 417 ms against
  1,070 ms. The gain is the JDK AOT cache that Arbace builds and uses by default; Clojure 1.12.6
  given a cache trained the same way starts as fast (190 ms).
- **Steady state**, 33 benchmarks: un-hinted interop about 580x faster (`invokedynamic`
  reflective sites), multi-argument `str` about 10x, keyword lookups 20-33 % faster. Most of
  the rest is equal within the noise. **Slower**: `into []` through a transducer 1.61x,
  `(reduce + v)` over a vector 1.17x, `conj!` into a transient vector 1.15x; the cause is not
  found (the bytecode of the hot paths matches javac's but for a few bytes). Without the two
  10x outliers, the geometric mean is 0.98 (the frozen baseline: 0.96).

## Known limits and open ends at the freeze

- **jdk26u through the class forms compiler**: the converted jdk26u compiles to javac's class
  shapes for 12,376 of 12,444 files (99.5 %, `bin/j2c-check --jdk`, which needs a jdk26u
  checkout and is not part of the gate). The remaining 68 files (switch fall-through
  duplicating a lambda, javac's anonymous-class numbering in chained calls, a few InnerClasses
  entries, three compile errors, module-infos not converted) are listed in
  `doc/classes/CONVERTER-NOTES.md`, "The converted JDK".
- The class forms compiler lags its amended spec in a few places (`doc/AGENDA.md`, step 3 of the class forms):
  constructor-call param-tags, `(anon Inner [args] :outer o ...)`, `int` typing of all-literal
  conditionals under `long`/`float`/`double` operators, and the converter deciding pins with
  the compiler's resolution.
- `recur` stays an error out of tail position and across `try` in code Clojure's compiler
  compiles itself (Clojure's suite asserts it); class forms lift that only in class bodies.
- `bin/arbace` does not notice a jar that is stale against edited sources: rerun
  `bin/build-arbace`.
- JDK 26.0.2: the AOT cache of a `jlink` image with some module sets stops the JVM at startup
  (`doc/VENDOR-NOTES.md`, "The runtime image"); the image works around it. Not yet reported
  upstream.
- Virtual threads (opt-in) are daemon threads: the JVM does not wait for pending futures.
- `bin/vendor-arbace` no longer reproduces `arbace/` where hand changes apply; the hand changes
  are recorded one by one in `doc/VENDOR-NOTES.md`.
- Performance: `doc/BENCHMARKS.md`, "Reading the results", lists where Arbace is slower than
  Clojure 1.12.6 (vector reduction and transient building, 15-60 %).

## What changes on main after the break

- Main leaves binary compatibility with Java: the `.class` format, the JVM as the runtime, and
  with them the jar, the JDK AOT cache, the `jlink` image, `invokedynamic` and the other
  JVM-specific work stop being goals there. Java stays usable at the source level through j2c.
- The language becomes the Arbace language (Clojure plus the class forms, later forms for Go
  via g2c, `doc/G2C-SURVEY.md`), aimed at a self-sustaining REPL in a virtual sandbox, written
  in Arbace down to the bare-metal ISA (/dev/kvm on amd64, Hypervisor.framework on arm64).
- On main, `clojure/` is replaced after the break, preferably by a Go-style binary seed (the
  freeze tag's jar as stage 0, pinned by hash), otherwise by a pinned upstream fetch plus the
  recorded patches; decided at the break (`doc/AGENDA.md`).
- The branch takes fixes only where they keep Arbace on Java 26 working (a later tag
  `arbace-for-java-26-v2` and so on); new features land on main.

## Making the freeze

```sh
bin/freeze                       # dry run: checks, and prints what it would do
gh workflow run gate.yml --ref main   # the gate on the main commit (or bin/freeze --run-gate)
bin/freeze --yes                 # creates arbace-for-java-26 and arbace-for-java-26-v1, pushes
```

`bin/freeze` requires a clean tree on main equal to `origin/main`, a branch and tag that do
not exist yet, the README's "Arbace for Java 26" section, and a passing gate on that very
commit: a successful `gate.yml` run whose head is the commit, or the gate run locally with
`--run-gate`. The tag message records the commit, the gate run and the JDK.
