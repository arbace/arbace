# The executable: B1a step 6

B1a step 6 ([B1-PLAN.md](B1-PLAN.md), branch `step6`, 2026-10-09): `bin/arbace-go` builds
static executables of Arbace for `linux/amd64` and `linux/arm64`, running `arbace.main`'s REPL,
`-e` and scripts; the core namespaces are prepared at build time, so that the executable starts
in about 0.3 s instead of 4.2 s. This document describes the image of prepared namespaces (the
step's main lever), the build, the measurements against the JVM's `bin/arbace` and against
Joker, the REPL as a user process, the gate question, the results, and the amendments proposed.

```
$ bin/arbace-go --build                    # c2g, the Go builds, the image, the links
$ target/arbace-go/amd64/arbace -e '(+ 1 2)'
3
$ echo '(doc inc)' | target/arbace-go/amd64/arbace
Clojure 1.13.0-master-SNAPSHOT
user=> -------------------------
arbace.core/inc
...
```

Summary (details below). Start to `-e nil` done on amd64: **4.2 s before, 0.32 s now** (the
JVM's `bin/arbace` 0.17 s, Joker under 0.01 s); under `qemu-aarch64` 44 s before, 5.2 s now.
The executable is 63.5 MB on amd64 (58.4 before; the image is 3.8 MB of it), 60.9 MB on arm64.
`bin/arbace-go --smoke` passes on both architectures, now with a REPL session compared with
the JVM's transcript. Clojure's suite on the Go build: 18,781 of 18,806 assertions, no regression
against `test/arbace-go-results.edn`; the oracle: 20,220 of 20,253, the one change being
`polymorphism.clj:176`, whose order of two interfaces is now deterministic and not the JVM's
(amendment U2).

## Where the start went

Before this step the executable read, macroexpanded, analyzed and evaluated `arbace.core` and the
namespaces `arbace.main` requires from their embedded sources at every start: 4.2 s on amd64
(EVAL-NOTES.md, "Measurements"), 1.9 s of it `arbace.core`. Measured at the start of the step
(amd64, `GOGC` default, the machine shared):

| | |
|---|---|
| reading (`LispReader`), all forms of the start | about 0.1 s |
| macroexpansion and analysis, the macros being evaluated fns (and, once `spec.alpha` is loaded, `arbace.core.specs`' checks of every macro call: most of `main.clj`'s 1.4 s) | most of the rest |
| evaluating the top-level forms (defs make fns; `ns` forms refer `arbace.core`) | small: about 20 ms for `spec.alpha`'s 227 forms against 0.7 s to analyze them |
| the garbage collector | 60% of the CPU time (1.7 GB allocated during the start) |

So pre-reading would save little, and the work to skip is the analysis, macroexpansion included.

## The image of prepared namespaces

**What it holds.** For every namespace source embedded in the program, the *units* its load
evaluates, in order: a unit is a top-level form, or each form of a top-level `do` (as
`Compiler.eval` splits them), after macroexpansion and analysis: its `Expr` tree (as
`Compiler.eval` would evaluate it: the `def`'s analysis with `C/EVAL`, or a non-`def` form wrapped
in a `fn*` and invoked), its line and column, and the *events* of its analysis: what the
analysis did besides building the tree, which must happen again before the tree is evaluated:

| event | made by | replayed by |
|---|---|---|
| `stub` | `NewInstanceExpr.compileStub` (deftype's stub class) | the same call |
| `type` | `ObjExpr.compile` (a deftype's, defrecord's or reify's class, with its methods) | `Evaluator.defineType`, the class stored in the `NewInstanceExpr` |
| `iface`, `imethod` | `gen-interface` (`definterface`, `defprotocol`; `genclass.clj`) | `Dyn.defineInterface`, `addInterfaceMethod` |
| `proxy` | `proxy`'s expansion makes the proxy class (`get-proxy-class`, `core_proxy.clj`) | `get-proxy-class` |
| `ns` | `*ns*` set while the unit is analyzed: the boot `ns` macro (`RT.bootNamespace`, used by `core.clj`'s first form) sets it when it expands | `*ns*` set again |

Var interning at analysis (`def`, `declare`) happens again when the unit's `Var`s are decoded
(found, else interned), and namespaces named by the tree are found or created.

**At run time** `RT.load` of an embedded source the image holds does not read it:
`Compiler$Image.replayLoad` makes the bindings `Compiler.load` makes, and for each unit decodes
it, makes its events again, and evaluates its tree as `Compiler.eval` does (with `*line*` and
`*column*` bound, and the class loader binding). Evaluation is unchanged: the evaluator runs the
same trees as after a load from source; only the analysis is skipped, as the JVM skips it for
its AOT-compiled namespaces. A load from source (`ARBACE_PATH`, `load-file`, the REPL) is as
before; so is a load when the program has no image.

**Recording.** `bin/arbace-go --build` runs the built executable once with `ARBACE_PREPARE=FILE`,
requiring every embedded namespace (a source whose first form is `ns`; the others are loaded by
them). Each embedded source `RT.load` evaluates is then recorded: `Compiler.load` hands each
top-level form to `Compiler$Image.evalUnit`, a `Compiler.eval` that records the unit between
its analysis and its evaluation, with the events gathered while the recorder is bound (during
the analysis only: what the evaluation does happens again when the unit is evaluated). The
program writes the image at exit; the build embeds it (the main package's `image.bin`,
`//go:embed`) and links the executables again.

**The encoding** (`go/arbace/cmd/arbace/image.clj`, the main package, which knows every type of
the program: c2g generates its table, `image_types.go`). A source's units are one stream; its
objects are numbered in order of first encounter, so the graph's sharing (a `LocalBinding` used
by many nodes, a form's metadata) and cycles survive, also between units. Each object is:

- a struct of the program (every `Expr`, `LocalBinding`, the persistent collections and their
  nodes, `ArrayList`, ...): its Go type, its identity hash as it was (0 if none was asked for),
  and its fields in declaration order (references, `bool`, integers, floats), by Go's
  reflection; decoding writes the fields in place (`unsafe`, the offsets computed once per type;
  an interface field's method table made once per pair of types);
- by name: `Keyword`, `Var` (its namespace and name), `Namespace`, `Class` (decoded with
  `Class.forName`'s registry, classes made at run time included), `Method`, `Constructor` and
  `Field` (the class's declared member of that name and signature); a class (or member) that the
  unit's own events make is resolved only after its event, since an older class of that name
  may still exist (a namespace loaded again);
- by value: strings, boxed numbers and characters (`valueOf`: the caches' identity), booleans
  (`Boolean.TRUE`/`FALSE`), arrays, `Pattern` (its source and flags, compiled again), atomics,
  a realized `LazySeq` (its seq; a `LazySeq` in a form is realized when it is encoded);
- canonical objects shared with the running program: the empty collections (`PersistentList`,
  `PersistentVector` and its `EMPTY_NODE`, maps, sets, the queue), `Compiler.NIL_EXPR`,
  `TRUE_EXPR`, `FALSE_EXPR`, `RT.EMPTY_ARRAY`, `PersistentHashMap.NOT_FOUND`.

Hashed collections keep their layout when decoded because every key hashes as it did when the
image was made: an object of the graph keeps its identity hash (the header's), a class's identity
hash is its name's (amendment U2), and the content hashes (strings, symbols, keywords, numbers)
are the same in every run. The two kinds of keys that do not, `Var` and `Namespace`, make a map
holding them be encoded as its entries and built again (`ObjExpr.vars`). Cached hashes
(`_hash`, `_hasheq`) are not encoded. Fields only the bytecode back end reads are not encoded
either: `ObjExpr.src` (the fn's or type's whole form) and the locals clearing paths
(`LocalBindingExpr.clearPath`, `clearRoot`).

Decoding an object of a class runs the class's static initialization first, as making an
instance does on the JVM (`StrConcatExpr.STR_VAR` was nil otherwise).

**Determinism.** Making the image twice gives the same bytes, as does making it with an
executable that already holds one (preparing ignores it). It does not depend on the
architecture: the amd64 executable makes it, and the arm64 executables embed the same.

**With the closure compiler** (amendment EC6, accepted 2026-10-10; SPEED-NOTES.md, "The
evaluator: closure compilation"): the image still holds the analyzed `Expr` trees; each method is
compiled to `Code` at its first call, not when the image is replayed (most of core's fns are not
called at start). The compiler's caches, `ObjMethod.evalCM` and `FnExpr.evalArities`, are among
the image's skipped fields. Start went from 0.33 s to 0.21 s, preparing the image from 32 s to
11 s.

## Building

`bin/arbace-go --build` (amd64 and arm64 by default, `ARBACE_GO_ARCHES`):

1. `bin/c2g --program` writes the program: the main package now has `image.go` (copied from
   `go/arbace/cmd/arbace/image.clj`), `image_types.go` (generated) and an empty `image.bin`;
2. `bin/g2c build` for each architecture, the host's first;
3. the host's executable prepares the namespaces (`ARBACE_PREPARE`; about 35 s, all 41
   embedded sources, 27 namespaces);
4. each executable is linked again with the image (`go build` in the printed module: only the
   main package changes, the rest comes from Go's build cache; a few seconds).

`bin/arbace-go --image` repeats 3 and 4 with the executables built.

Environment of the executable: `ARBACE_NO_IMAGE` (load from the sources, as before),
`ARBACE_PREPARE=FILE` (record the image into FILE), `ARBACE_IMAGE_STATS` (with
`ARBACE_PREPARE`: each source's objects by type), `ARBACE_IMAGE_TIMES` (each source's decoding
time, and the units whose evaluation takes more than 3 ms), and `ARBACE_LOAD_TIMES` as before;
`ARBACE_RLWRAP` (`off`: the REPL never under rlwrap; "Line editing: rlwrap").

## The start's garbage collector

The start allocates the decoded trees and what their evaluation makes, most of which stays.
With Go's default (`GOGC=100`) the collector runs many cycles while the heap grows from nothing;
until `Main.main` has loaded `arbace.main` (a variant of `Main`, `arbace/lang/go/Main.clj`,
calls `Compiler$Image.started`), the program runs it at `GOGC=400` unless `GOGC` is set, then
sets it back. Measured: amd64, best of 10 runs, `-e nil`: 0.32 s with the start's `GOGC=400`, 0.41 s with
`GOGC=100` throughout; with the collector off the start takes 0.28 s (and 280 MB). The setting of the running program stays D7's
question (step 7).

## Measurements

On this machine (64 cores, shared with other agents' builds; best of 5 to 15 runs), go1.27.1,
JDK 26 for the JVM:

| | start to `-e nil` done | REPL to end of input | REPL, one `defn` | resident | size |
|---|---:|---:|---:|---:|---:|
| JVM `bin/arbace` (jar and AOT cache) | 0.17 s | 0.28 s | 0.33 s | 96 MB | jar 7.7 MB + AOT cache 39 MB + the JDK (401 MB; a `jlink` image of Arbace 133 MB) |
| Go, before (step 5, main's executable) | 4.18 s | | | 117 MB | 58.4 MB |
| Go, this step, from the sources (`ARBACE_NO_IMAGE`) | 3.47 s | | | 244 MB | |
| **Go, this step** | **0.32 s** | **0.37 s** | **0.56 s** | 134 MB | **63.5 MB** (static, not stripped) |
| Joker v1.10.0 (release binary) | < 0.01 s | | < 0.01 s | 20 MB | 29.0 MB |
| Go arm64 under `qemu-aarch64`, before / now | 44.1 s / 5.2 s | | | 211 MB | 60.9 MB |

The first `defn` at the REPL costs 0.2 s more than the start: the first macroexpansion checking
specs loads `arbace.spec.alpha` again and `arbace.core.specs.alpha` (`Compiler.ensureMacroCheck`,
as on the JVM), both replayed from the image.

Where the 0.32 s go now (amd64, `ARBACE_IMAGE_TIMES`, `GODEBUG=inittrace=1`): the Go packages'
initialization 11 ms; decoding the 16 sources of the start (239,000 objects) about 125 ms
(core.clj 82 ms); evaluating their units about 150 ms, most of it the `ns` forms (each `refer`
of `arbace.core`'s 700 public vars, evaluated: 15 to 25 ms per namespace) and `arbace.main`'s
and `spec.alpha`'s top-level defs; the rest the collector. Joker starts from Go-initialized
data structures with nothing evaluated; the JVM from classes already loaded and linked (the AOT
cache) with compiled code. The remaining levers are step 7's: closure compilation (the `refer`s
and the other evaluated start code), then namespaces compiled to Go (no decoding at all).

Before this step's decoding was made direct (field offsets, cached method tables, member lists
cached per class) it took 580 ms; with `ObjExpr.src` and the clearing paths encoded the image
was 5.1 MB and 380,000 objects for the start.

## The REPL as a user process

`bin/arbace-go --smoke` (each of `ARBACE_GO_ARCHES`; arm64 under `qemu-aarch64`) checks, besides
`-e`, a script, `-` and an error's exit status and message, a REPL session against the JVM's
transcript (`bin/arbace` gives the same, but for the number of the `eval` class in the error
message): reading and printing, multi-line input, `doc`, an error that the REPL reports and goes
on after, `*1`, `*2`, `*3` and `*e`, and the end of input ending the REPL with status 0.

## Line editing: rlwrap

The user's decision (2026-10-10): the executable's interactive REPL runs under `rlwrap`, as
Alpine's `clj` runs `clojure` and the JVM package's `arb` runs `arbace`
(`exec rlwrap -m -r -q '\"' -b "(){}[],^%#@\";:'" "$bin_dir/clojure" "$@"`; the quote
characters are `\` and `"`). Branch `gorl`; amendments RL1-RL3
below.

**When.** The main package's `main` calls `execRlwrap` first, before jrt's host is set and the
image is replayed (`rlwrap_linux.go`, hand-written forms `go/arbace/cmd/arbace/rlwrap_linux.clj`,
copied by `bin/c2g --program` beside `image.clj`). It replaces the process with rlwrap only when
all of these hold, else it returns and the program runs as before, silently:

1. `ARBACE_RLWRAP` is not `off` (the user's switch) and not `wrapped` (the marker the
   executable sets for its child: no loop); any other value, or none, means the default;
2. `TERM` is set and not `dumb` (Emacs' shells; rlwrap warns without `TERM`);
3. the arguments start arbace.main's REPL (`startsREPL`, read off `arbace.main/main`): no
   arguments, or init options (`-i`/`--init`, `-e`/`--eval`, `--report`, each with its
   argument) followed by `-r`/`--repl`. Init options alone (null-opt), `-m`, `-h`/`-?`/`--help`,
   a script and `-` do not start a REPL, so `-e` and scripts never see rlwrap;
4. standard input and output are terminals (`ioctl` `TCGETS` on fds 0 and 1), and standard
   input's terminal has a width (`TIOCGWINSZ`: rlwrap refuses a width of 0, and the REPL would
   then not start at all);
5. the parent process is not rlwrap (`/proc/PPID/comm`: `rlwrap arbace` typed by hand, which
   does not set the marker);
6. `rlwrap` is a regular executable file in one of `PATH`'s absolute directories;
7. `os.Executable` (`/proc/self/exe`, resolved) names the executable.

Then `syscall.Exec` (execve, no lingering parent) of rlwrap with clj's flags, the executable's
resolved path and the original arguments, the environment unchanged but for
`ARBACE_RLWRAP=wrapped`. rlwrap names its history after the command's base name:
`~/.arbace_history` (the reason for the resolved path rather than `/proc/self/exe`, which would
give `~/.exe_history`). `GOGC` and the other settings reach the child as they were; the start's
collector setting (U4) is made after `execRlwrap`, in the process that runs. If the exec fails
the program goes on without rlwrap.

**Cost.** The checks are a few system calls and one small file read, made only when 1 to 3
hold; when rlwrap applies, the first process has done only the Go packages' initialization
(about 11 ms) before the exec, none of the image's replay.

**No message when rlwrap is missing.** clj prints "Please install rlwrap for command editing
or use "clojure" instead." and exits 1: it is only a front for `clojure`, which stays the way
without rlwrap. This executable is the only way to its REPL and a complete REPL without
rlwrap, so it runs it; a line at every start would be noise for those who do not want rlwrap. Documented here and in RL1 instead. arbace.main's `--help` text is the JVM's too
(`arbace.main/main`'s doc string, shared by both builds), so it is not changed.

**Other systems.** `rlwrap_other.go` (`//go:build !linux`) has an empty `execRlwrap`: TamaGo's
programs (B1b) have no rlwrap, no `execve` and no terminal of this kind.

**The check** (`test/arbace-go-rlwrap.py`, run by `bin/arbace-go --smoke` on amd64 when
`python3` and `rlwrap` are installed). Python's `pty` runs the executable on a pseudo-terminal
of 24x80 (`TIOCSWINSZ`), `TERM=xterm`, a scratch `HOME`: with no arguments and with
`-e ... -r` the process on the pty is rlwrap and its one child `arbace`, the REPL sees
`ARBACE_RLWRAP` `"wrapped"`, the up arrow recalls the last input (`42` twice), the end of input
ends it with status 0 and nothing about rlwrap is printed; with `ARBACE_RLWRAP=off`,
`TERM=dumb` and `PATH=/nonexistent` the process on the pty is the executable itself, without
child; `-e` alone on the pty prints its value, without rlwrap. The smoke test's other checks run
with pipes, so without rlwrap. arm64 is not checked on a terminal: under `qemu-aarch64`, rlwrap
would execute the arm64 executable directly.

## The smoke test in the essential gate

The smoke test itself is fast: `ARBACE_GO_ARCHES=amd64 bin/arbace-go --smoke` takes 3.5 s. Getting
an executable is not. After the bootstrap, the chain is `bin/jrt-convert` (about 95 s; its output
`.tmp/jrt/conv` is what `bin/c2g` translates), `bin/c2g --program` (about 75 s), `bin/g2c build`
amd64 (60 s of CPU with Go's build cache warm, minutes cold), the image (32 s) and the link (a
few seconds): 4 to 5 minutes, against the 3.2 minutes the suite on stage 2 runs concurrently. It
would add 1 to 2 minutes to the essential gate, and load the machine beside the suite. So, by the
rule given, `bin/gate` is not changed. Options for the user:

1. **The chain in the essential gate**, concurrently with the suite and the class forms tests:
   +1 to 2 minutes, about 10 GB more at its peak.
2. **A cached executable**: the gate keys the executable by the hash of its inputs (the git tree
   hashes of `arbace/`, `go/`, `overlay/`, `test/jrt`, the scripts `bin/c2g`, `bin/g2c`,
   `bin/jrt-convert`, `bin/arbace-go`, and the toolchain's version) and builds only when the key
   changed; a change to docs or tests costs seconds, a change to Arbace's sources the chain of 1.
   (`bin/jrt-convert`'s output can be cached the same way by its own inputs, saving 95 s of
   most rebuilds.)
3. **The smoke test in `--full` only**, beside the suite and the oracle on Go that `--full`
   gains (the Go checks the gate-go branch adds): the essential gate stays JVM-only, as it is.

Recommended: 2, with 3's checks in `--full`: the essential gate then tests the executable on
every change that can change it, and costs nothing on the others.

**As built** (the user's decision, 2026-10-09: option 2; amendment U5). The essential gate's
check `go` runs `bin/arbace-go --gate`, concurrently with the suite on stage 2 and the class
forms tests (not with `--full`, whose Go chain builds and smoke-tests anew). It computes a key,
the SHA-256 of the contents of `arbace/`, `go/`, `overlay/` and `bin/lib/`, of
`doc/go/java-surface.edn`, the seed's hash and the scripts of the build (`bin/arbace-go`, `c2g`,
`g2c`, `j2c`, `jrt`, `jrt-convert`, `arbace`, `build-arbace`), and of the toolchains (TamaGo's
`VERSION`, `java -version`, jdk26u's commit): about 0.1 s. When `.tmp/arbace-go-gate/key` holds
that key, it smoke-tests `.tmp/arbace-go-gate/amd64/arbace` (`--smoke` on amd64); otherwise it
runs `bin/jrt-convert`'s steps `sources,generate,convert` (what `bin/c2g` reads: the javac
comparison, the check and the report are `--full`'s) and `--build` for amd64, copies the
executable and the key into the cache, and smoke-tests it. Measured on this machine (shared):

| | the check `go` | the essential gate |
|---|---:|---:|
| a miss | 5m13s and 5m37s (built in 310 s, 334 s) | 6m46s, 7m06s |
| a hit | 0m04s | 3m38s |
| before (no check `go`) | | about 4m20s |

At the first miss the machine's peak memory use was 25 GB of 62 (the suite's 24 JVMs, the class
forms tests, and the build's JVMs and `go build` beside them); during the second pair of runs,
with other agents' work beside, it reached 40 GB, 6 GB left available.
## Results

- **Oracle on Go** (`bin/oracle check 'target/arbace-go/amd64/arbace -'`), on this branch before
  the merge: 20,220 of 20,253 (main then: 20,221). The forms corpus 10,074 of 10,077: besides main's two (`deftype Foo/2`'s error source,
  the `StringBuilder`'s identity hash), `polymorphism.clj:176`: `class-ambig`'s message names
  `java.io.Serializable` before `java.lang.Comparable`, where the JVM's names them in the other
  order. The order is that of a hash map keyed by the two classes (the multimethod's method
  table); main passed it by the luck of identity hashes (EVAL-NOTES.md, phase 2A), and with
  classes hashed by their names (amendment U2, which the image needs) the order is now fixed,
  and not the JVM's. The class scripts 8,942 of 8,943 and the regex corpus 1,204 of 1,233, as on
  main. Merged into main (`bc586b1`) with `MultiFn`'s order by class name (`ac8b971`), the case
  passes there: the message no longer depends on a hash.
- **Clojure's suite on Go** (`CLOJURE_TESTS_GO=... bin/clojure-tests -j 16`): 18,781 of 18,806
  assertions (588 tests; 19 failures, 6 errors), 61 of 64 namespaces load: no regression
  against `test/arbace-go-results.edn`. (A first run found one: a library requiring
  `arbace.core` loads `core.clj` again from another namespace, and the boot `ns` macro sets
  `*ns*` while it expands, which the image did not replay: the `ns` event.)
- `bin/arbace-go --smoke`: amd64 and arm64 (`qemu-aarch64`) pass.
- `bin/jrt test` (amd64) passes; `bin/c2g-evalproof` (amd64): as expected (31 forms).
- `bin/gate` was not run: this step changes no source the bootstrap compiles (the Go build's
  variants `arbace/lang/go/` and c2g are read by c2g only), and by the user's rule (2026-10-09)
  `--full` is for the freeze.

## Alternatives considered

- **Pre-read forms** (the forms as data, no reader at start): the reader is about 0.1 s of the
  4.2 s; macroexpansion and analysis would remain.
- **Macroexpanded forms** (the forms after expansion, analyzed at start): saves the evaluated
  macros but not the analysis, and needs a `macroexpand-all` that follows the analyzer's
  decisions (locals shadowing macros, `&env`); recording the expansions by identity of the
  forms would avoid that, at the cost of the forms' size.
- **A heap snapshot** (the state after loading, restored at start; Joker's `gen_code` does this
  for its core namespaces, as Go code): fastest (no evaluation at start), but it captures
  values the load computed (system properties, `*command-line-args*`, the architecture: an
  image made on amd64 would be wrong on arm64), and every runtime structure (thread-locals,
  locks, agents' executors, jrt's registries) would need its own restore rule.
- **Go code instead of data** (the trees as Go composite literals): faster decoding, but a
  larger program and a slower build; the image is 3.8 MB of data.
- **Lazy decoding** (a fn's body decoded when it is first called; most of core's 1,262 fns are
  not called at start): the encoding would have to keep each body a closed subgraph; left for
  later, with step 7's code generation as the better remedy.

Chosen: pre-analyzed units with their events (like Joker's packed linter forms, evaluated at
start), because evaluation stays exactly what a load from source does and the image is
independent of the architecture and of the environment at build time.

## Amendments

Numbered U (a letter not used in `doc/`). All accepted by the user on 2026-10-09 and folded
into their home documents, as noted under each.

- **U1 (EVAL-PLAN §2.7, C2G-SPEC §10.3) The image of prepared namespaces.** `RT.load` of a
  source embedded in the program replays it from the program's image (its units: the analyzed
  `Expr` trees with the events of their analysis) instead of reading and analyzing it;
  `bin/arbace-go --build` records the image with the built executable (`ARBACE_PREPARE`) and
  links the executables again with it; `Compiler$Image` in the `Compiler` variant, the variants of
  `RT` (`load`, the boot `ns` macro's event) and `Main` (`started`), `genclass.clj` and
  `core_proxy.clj` recording their classes. A load from source is unchanged. (B1-PLAN's step 6
  said "pre-read or pre-analysed": pre-analysed, measured above.) *Accepted 2026-10-09, folded
  into C2G-SPEC §10.3 and §16, EVAL-PLAN §2.7, B1-PLAN's step 6.*
- **U2 (C2G-SPEC §5.8; jrt) A class's identity hash is its name's** `String.hashCode` (31 bits,
  never 0), set when the `Class` is made (`jrt.presetClassHash`: `Define`, `DefineDynamic`, the
  primitive and array classes), instead of the next value of the global sequence. Hashed
  collections keyed by classes keep their layout in the image, and `(hash SomeClass)` and such
  orders are the same in every run (the AGENDA's "determinism issue"). Consequence on this
  branch: the oracle's `polymorphism.clj:176` mismatched in every run. *Accepted 2026-10-09,
  folded into C2G-SPEC §5.8 and §16.* The case is moot since main's `ac8b971`: `MultiFn` names
  two ambiguous classes in the order of their names, on both builds, so the message no longer
  depends on any hash and `polymorphism.clj:176` passes (re-recorded there).
- **U3 (C2G-SPEC §10.6) The main package's files**: besides
  `main.go` and `res/`, `image.go` (hand-written forms, `go/arbace/cmd/arbace/image.clj`, copied
  by `bin/c2g --program`), `image_types.go` (generated: every exported non-generic struct type of
  `arbace/lang` and `arbace/jrt`, by name) and the embedded `image.bin`; jrt's `ImageHooks`, set
  by the main package, through which `Compiler$Image`'s natives reach it. *Accepted 2026-10-09,
  folded into C2G-SPEC §10.6 and §16.*
- **U4 (D7, for step 7) The start's collector**: `GOGC=400` while the program starts, unless
  `GOGC` is set, back to the previous value when `Main.main` has loaded `arbace.main`.
  *Accepted 2026-10-09, folded into C2G-SPEC §13.4 (step 7's plan, D7) and §16, and B1-PLAN's
  "Checks" paragraph (D7).*
- **U5 (B1-PLAN, "Checks") The smoke test and the gate**: the smoke test gains the REPL session;
  its place in `bin/gate` is the user's choice among the options above (recommended: a cached
  executable in the essential gate). *Accepted 2026-10-09 with option 2 (the user's decision):
  implemented as the essential gate's check `go` ("The smoke test in the essential gate"
  above), folded into B1-PLAN's "Checks", C2G-SPEC §16 and CLAUDE.md.*

### The REPL under rlwrap (RL)

Numbered RL (a prefix not used in `doc/`); proposed on branch `gorl` (2026-10-10), for the
user's acceptance.

- **RL1 (C2G-SPEC §10.6; the executable's environment) The interactive REPL under rlwrap.**
  The program's `main` first calls `execRlwrap`, which replaces the process (execve) with
  `rlwrap -m -r -q '\"' -b "(){}[],^%#@\";:'" EXE ARG...` (clj's flags, the executable's resolved
  path, the original arguments, `ARBACE_RLWRAP=wrapped` added) when the arguments start
  arbace.main's REPL, standard input and output are terminals with a width, `TERM` is set and
  not `dumb`, the parent is not rlwrap, `ARBACE_RLWRAP` is neither `off` nor `wrapped`, and
  `rlwrap` is on `PATH`; otherwise the program runs as before. New environment variable:
  `ARBACE_RLWRAP` (`off`: never; `wrapped`: set by the executable for its child). No message
  when rlwrap is missing ("Line editing: rlwrap" above, for why).
- **RL2 (C2G-SPEC §10.6, U3) The main package's files**: besides `main.go`, `image.go`,
  `image_types.go`, `image.bin` and `res/`, `rlwrap_linux.go` and `rlwrap_other.go`
  (`//go:build linux` and `!linux`), from the hand-written forms
  `go/arbace/cmd/arbace/rlwrap_linux.clj` and `rlwrap_other.clj`, copied by
  `bin/c2g --program` (`arbace.c2g.embed/rlwrap-forms`). The terminal checks are the main
  package's, over `syscall`: they are the launcher's decision, before jrt is set up, so jrt's
  `Host` and its optional interfaces (§9.4) are unchanged and gain none.
- **RL3 (B1-PLAN, "Checks") The smoke test on a terminal**: `bin/arbace-go --smoke` runs
  `test/arbace-go-rlwrap.py` on amd64 when `python3` and `rlwrap` are installed (skipped with a
  line otherwise): the REPL under rlwrap on a pseudo-terminal, and not under it when turned off,
  with `TERM=dumb`, without rlwrap on `PATH`, and for `-e`. It adds about 5 s to the smoke test
  on amd64 (now about 13 s in all, most of it the agents' checks of 5 s and the pty's checks).

## Sources

Nothing vendored. Studied: candid82/joker at `v1.10.0` (commit `eed70a3c`), `DEVELOPER.md`
("Compiling Core Namespace Structures to Native Go Code", "Packing Linter-specific Joker Files
as Native Go Data Structures"), `core/gen_code/gen_code.go`, `core/pack.go`: Joker's two ways
of preparing namespaces at build time; Arbace's `Compiler.clj` (`eval`, `load`, the boot `ns`
macro of `RT.clj`). Joker's release binaries `joker-linux-amd64.zip` and
`joker-linux-arm64.zip` of v1.10.0 were run for the measurements (in `.tmp/`, not kept).

The REPL under rlwrap: Alpine's `clojure` package's `clj` script (`/usr/bin/clj` on this host,
its rlwrap command line and flags) and the rlwrap installed here (`rlwrap` 0.48, Alpine's
package; its manual for `-m`, `-r`, `-q`, `-b` and the history file `~/.COMMAND_history`).
