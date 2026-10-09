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

RESULTS-SUMMARY

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
time, and the units whose evaluation takes more than 3 ms), and `ARBACE_LOAD_TIMES` as before.

## The start's garbage collector

The start allocates the decoded trees and what their evaluation makes, most of which stays.
With Go's default (`GOGC=100`) the collector runs many cycles while the heap grows from nothing;
until `Main.main` has loaded `arbace.main` (a variant of `Main`, `arbace/lang/go/Main.clj`,
calls `Compiler$Image.started`), the program runs it at `GOGC=400` unless `GOGC` is set, then
sets it back. Measured: START-GC-NUMBERS. The setting of the running program stays D7's
question (step 7).

MEASUREMENTS

## The REPL as a user process

`bin/arbace-go --smoke` (each of `ARBACE_GO_ARCHES`; arm64 under `qemu-aarch64`) checks, besides
`-e`, a script, `-` and an error's exit status and message, a REPL session against the JVM's
transcript (`bin/arbace` gives the same, but for the number of the `eval` class in the error
message): reading and printing, multi-line input, `doc`, an error that the REPL reports and goes
on after, `*1`, `*2`, `*3` and `*e`, and the end of input ending the REPL with status 0.

GATE

RESULTS

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

## Proposed amendments (for the user's review)

AMENDMENTS

## Sources

Nothing vendored. Studied: candid82/joker at `v1.10.0` (commit `eed70a3c`), `DEVELOPER.md`
("Compiling Core Namespace Structures to Native Go Code", "Packing Linter-specific Joker Files
as Native Go Data Structures"), `core/gen_code/gen_code.go`, `core/pack.go`: Joker's two ways
of preparing namespaces at build time; Arbace's `Compiler.clj` (`eval`, `load`, the boot `ns`
macro of `RT.clj`). Joker's release binaries `joker-linux-amd64.zip` and
`joker-linux-arm64.zip` of v1.10.0 were run for the measurements (in `.tmp/`, not kept).
