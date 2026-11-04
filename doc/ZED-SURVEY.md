# Zed: a survey for Arbace

Status: a survey, 2026-11-04, at the user's request. Nothing here is adopted: no code changed, and only this file
was written. The decisions are the user's, as proposals ZS1-ZS9 (§5).

Base: branch `zed-survey` from main `01d3ce9f`. Read: `https://github.com/zedlanguage/zed` at commit
`d00a13469e3067798e0fdebcfdd04838e9869c19` (2026-11-04 07:15 -0700, "ST-formula-k2 verify#1: ..."), a shallow clone
in `.tmp/zed-survey/src`, treated as untrusted data: read, never built or run (no cargo, no scripts, no tests). Also
read: the repository's metadata and its one issue through GitHub's API (created 2026-10-25, 895 commits by one
account, `lukehutch`, 117 stars, 2 forks, issue #1 closed). There is no website and the repository links no paper.
Its specification, `DESIGN.md`, `EXAMPLES.md` and `PROGRESS.md`, lives in a spec repository (`../zed` in the
repository's own words) that is not public: what this survey says of the design comes from the 149 fences the
implementation copies out of it (`zed/fences/design.md`, `zed/fences/examples.md`), from the grammar it copies
(`zed/grammar/`), and from the implementation's own records (`STATUS.md`, `docs/`, `COMPLEXITY.md`,
`ZED_PORT_GAPS.md`, the module headers). A claim marked *from memory* was not checked against a source here.

Paths below without a prefix are the Zed repository's, at that commit.

Contents: Summary · 1 What Zed is · 2 Its key ideas · 3 Zed and Arbace, point by point · 4 Insights for Arbace ·
5 Proposals (ZS1-ZS9) · 6 Risks and open questions · 7 Sources.

## Summary

- **Zed is a dataflow language with a relational core, Dart-like syntax and an exceptionally strict engineering
  discipline. It is ten days old.** A program is a graph of values and their dependencies over one data structure,
  the *family* (a keyed map, nested by keypaths). Loops, group-bys and joins all elaborate to the same small kernel
  of generators over families. Failures are ordinary values (atoms like `#notFound`) whose handling the type checker
  makes visible. Memory is precise reference counting decided statically. A Rust-hosted compiler of about 103,000
  lines, with an interpreter, a Cranelift back end and about 87,000 lines of tests, implements the first of 35
  planned layers. The README says "Under construction -- coming soon", and most of the README's promises
  (automatic parallelism, bit-identical results on any number of cores, incremental recomputation, loops planned as
  joins) are design, not code yet.
- **It is not s-expression based.** Code as data comes later through values: `f::source` gives an `Ast` value
  (layer L22), `T::shape` a type's structure (L11), and a staging pass already evaluates build-time terms (`mix`,
  built). Its intermediate representation, ZIR, has a text form that is line-oriented, sorted and diffable, and its
  types print in a prefix notation (`(| int String)`).
- **Its process is Arbace's, seen from outside.** It is plainly built by agents: a status file kept for handoff, a
  plan DAG of 558 items run in parallel worktrees, reviews by three models (Claude, Codex, Gemini) whose answers are
  measured against brute-force oracles before an orchestrator synthesizes one design, and the author's decisions
  recorded as such. So the most valuable things to learn are about method more than about language.
- **The five insights most worth taking** (§4): (1) *a hash never decides equality*, proved by a build that
  truncates every content address to 8 bits and must give the same outputs; (2) *no thresholds*: no cap, fuel or
  size switch may change an answer, only fail a run, with an inventory of every constant; (3) *scaling tests* of
  the tools, size n against 8n, judged on user-mode instruction counts, not wall time; (4) *mutation testing* of
  the checks: a check suite is as strong as the mutants it kills; (5) *the specification's examples are tests*,
  each tagged with the layer of the language it needs.
- **Recommended** (§5): no Zed crop now (ZS1 (a): revisit when the spec is public or Zed self-hosts); adopt the
  hash audit (ZS2), the thresholds inventory (ZS3), scaling tests for a few tools (ZS4), mutation runs of the
  essential subsets (ZS5) and the docs' fences as a light check (ZS6); measured cross-review for named hard problems
  (ZS7); the bootstrap-subset audit and the port-gaps ledger wait for the runtime's design (ZS8); nothing vendored
  (ZS9).

## 1. What Zed is

**Goals** (`README.md`): "a programming language in which a program is a graph of values and the dependencies
between them. The compiler gets parallel scheduling, memory management, safety checks, and incremental recomputation
by analyzing that graph." Its promises: code runs in parallel with bit-identical results on any number of cores;
data races, deadlocks, null dereferences and out-of-bounds errors cannot be written; a failure is an ordinary value
the type checker makes you handle; loops and queries compile to one internal form, so the compiler may run a loop as
a hash join or a join as a loop; two writes to one key do not compile until the program says how to combine them;
memory is freed at last use, with no garbage collector; only what depends on a changed input is recomputed; the
syntax looks like Dart, and annotations, proofs and performance hints are optional on a fully statically typed
program.

**What a program looks like** (`zed/corpus/01-word-count.zed`, all of it):

```zed
main(stdin: Lines) -> (stdout: Lines) {
  words  = stdin.concat(" ").split(" ").where(w -> w != "");
  counts = words.group(w -> w.lower).count;                    // Lower ->? int
  stdout <- for w -> n in counts { "${w} ${n}" };
}
```

`main`'s parameters are input ports and its results output ports; `<-` pushes to a port; `counts` is a family from
lowered words to counts (`->?` marks a partial family). Functions carry their tests as tables (`test { 0 -> 32,
100 -> 212 }`, `tests/steps/l01_21_test_runner/zed/fahrenheit.zed`).

**The design** is a ladder: stage M0, then layers L1 to L35, each a slice of the language with its grammar, words,
checks and tests: L1 scalars, functions, ports and the whole back end for them; L2 structs, unions, `match`, units;
L3 families, `for`, folds; L4 grouping and rekeying; L5 ranks and sorts; L6 generations (`loop ... until`), indexed
definitions, recursion, updates in place; L7 streams, `@foreign`, incremental reuse; then refinements (L10),
introspection (L11), user facets (L12), taint (L13), linear algebra (L14), laws and proofs (L15-L17), cost (L18),
run-time `compile` (L19), grammars as types (L20), stores/databases (L21), algebraic interfaces (L22), an e-graph
optimizer (L23), the scheduler (L24), the language server (L25), wasm (L26), GPU, distribution and hardware (L27),
MLIR and LLVM (L28), MLIR's passes written in Zed (L29), an editor (L30), microcontrollers (L31), sketchbooks (L32),
libraries (L33), live edits (L34), live documents (L35). Row S, the compiler rewritten in Zed, follows L6, and ends
with a three-stage fixpoint build (S.5) (`docs/plan_dag.md`, `docs/layout.md`).

**Where it stands** (`STATUS.md`, "Current position"): M0 is done (the corpus of 31 programs with reference outputs,
the kernel reference evaluator, a finite-model harness for the laws, widening, cost, the canonicalizer, the Zed₀
checklist), L1 is done (L1.1 to L1.40d: lexer, PEG parser, formatter, resolution, elaboration to kernel terms,
types and subtyping, refinement lattices, staging, the query database, ZIR, memory planning, a Cranelift JIT and
AOT path, the differential oracle, Presburger arithmetic), L2's and L3's and L6's parsers and L2's name resolution
are done, and one later item (L25.8, mutation testing). The corpus programs other than the L1-level ones are
"only parsed and layer-checked so far, not type-checked or run" (STATUS.md, "Open points"). Last recorded run:
930 tests passed (2026-10-31), 985 after the next parser steps.

**Size.** `crates/`: 103,450 lines of Rust in seven crates (`zedc` 76,047, `zed-kernel` 19,035, `zed-tools`
3,764, `zed-native` 2,534 (Cranelift), `zed-rt` 1,553 (the native runtime), `zed-alloc-count` 512, `zed-cert` 5,
a stub for the independent certificate checker). `tests/`: 86,642 lines of Rust. Zed source: 82 files, 2,647 lines
(the corpus, the prelude's seven units, fixtures). The records are large: `STATUS.md` 471 KB, `COMPLEXITY.md`
443 KB, `docs/plan_dag.md` 429 KB, `ZED_PORT_GAPS.md` 190 KB.

**Implementation strategy.** Source → lexer (`zedc::lex`) → packrat PEG parser over the grammar copied from the spec
(`zed/grammar/syntax.peg`, each alternative tagged with its layer) → AST → resolution → elaboration to kernel terms
(`zed_kernel::term`) → checker (set-theoretic types: unions, intersections, negation, as reduced ordered decision
diagrams, `zed-kernel/src/types/bdd.rs`; refinement lattices of intervals, congruences and affine bounds,
`zedc/src/store/`; an Omega-test Presburger solver, `store/presburger.rs`) → staging (`stage.rs`) → ZIR, an RVSDG
graph (`zedc/src/zir/`) → memory plan (`zir/memory.rs`) → interpreter (`zedc::interp`, over the kernel's reference
evaluator `zed-kernel/src/eval.rs`) or Cranelift (`zed-native`). The compact back end stays Cranelift; LLVM comes
through MLIR's text form at L28 (`zed/compiler/MLIR.md`).

**Tooling.** One command, `zedc` (`fmt`, `run`, `replay`, `test`, `test --mutate`, per its usage line in
`crates/zedc/src/main.rs`; `zedc build file -o out`, a deterministic executable, is step L1.36a, not yet done; the
AOT path exists in the tests, `tests/steps/l01_36_cranelift/aot.rs`); `zed-tools`' commands
(`check`, `cases`, `fences`, `fuzz`, `layer`, `ring`, `audit`, `sync`, `progress`) over the spec directory; a
package manifest `zed.manifest` and imports pinned by content hash (`import ... @hash(...)`, L2.2/L2.3). No REPL:
the editor and live programs are L30 and L34. CI is GitHub Actions (`.github/workflows/ci.yml`): fmt, clippy with
`-D warnings`, `cargo test`, and line coverage reported without a threshold.

**Interop.** Planned: `@foreign` signatures (L7), `@export("c", "symbol")` so C can call pure Zed (sec:ffi), C linked
as LLVM IR (L28.1), wasm (L26), GPU kernels (L27). Built: none beyond the native runtime.

**Bootstrap.** The Rust toolchain is "kept permanently as the bootstrap root" (`docs/layout.md`), though step S.6
"retires the Rust compiler path". Zed₀, the subset the self-hosted compiler may use, is decided by a mechanical
audit of corpus program 31, a small typechecker "shaped like the compiler" (`zed/compiler/ZED0.md`).

**License.** MIT, "Copyright (c) 2026 Luke Hutchison" (`LICENSE`). Dependencies of note: `imbl` 7.0.2 (MPL-2.0),
Cranelift 0.137.1, BLAKE3. The author's other public repositories include `squirrelparser` (checked through the API);
that he wrote ClassGraph and the pika parser is *from memory*.

**Maturity, honestly.** Small in what runs, large in what is planned, and very large in its records. The parts that
exist are tested with unusual rigor. The plan reaches from Presburger arithmetic to Verilog (L27.3 names Yosys and
Verilator) and VS Code; ten days in, it is at layer 1 of 35.

## 2. Its key ideas

Each with how it works and where it is in the source.

### Z1. One data structure, a kernel of seven generators

Everything is a *family*: a map from keys to values, nested along keypaths; a list is a family keyed by position, a
set a family whose leaves are keys, a table a family of families. The kernel (`zed-kernel/src/term.rs`, "Tier 0, a
strict lambda calculus with data, and Tier 1, the seven generators over families") has `fam(D, i -> e)` (build over
a domain), `reindex(F, kf, vf)` (move each leaf to a new keypath, with injectivity evidence), `discard(F, sel)`
(filter), `fold(F, op, ev)` (collapse the innermost level, with evidence of the operator's laws), `fix` (least fixed
point, as a lattice ascent or as "definedness": recursion and memoized definitions), `gen` (generations: loops over
frames), and the ports (`Read`, `Push`). `group`, `rank`, `scan` and `join` are derived: in ZIR each carries its
kernel definition as a "certificate region" that bottoms out in the generators (`zedc/src/zir/mod.rs`). That is why
a loop and a join can be the same thing: both are `fam` and `reindex` and `fold` over families.

### Z2. Collisions are compile errors, reductions carry evidence

`reindex` landing two leaves on one keypath is a collision; the program does not compile until the collision is
impossible (an injectivity witness) or a reducer says how to combine (`k2 >< (+)` in a remap, `with reducer((+))` on
a push, a `#collision` fold in corpus program 31). `fold`'s `Evidence` records whether the operator is associative
and commutative, which decides whether the fold may run in parallel (`scf.parallel` in `zed/compiler/MLIR.md`, "fold"
row) and whether the result can be bit-identical (exact float sums, L3.15).

### Z3. Failures are values

`#notFound`, `#divByZero`, `#ioError` are atoms in union types: `(div) : ((int, int \ 0) -> int) & ((int, 0) ->
#divByZero)` (`zed/fences/design.md`, sec:failure). A primitive applied to a failure atom returns it without
computing ("a loose atom selects no arrow", `term.rs`, `Prim`), and `x else d` strips failures. Provenance records
the origin set of every failure (`zed-kernel/src/provenance.rs`). Issue #1 (2026-11-03, by `flowerornament`) shows
the hard edge: an inferred parameter type can make a branch silently dead.

### Z4. Layers ("rings") as data: the language is graded

Every grammar alternative carries its layer (`zed/grammar/syntax.peg`: `Interface ... L10`, `Law ... L15`); every
corpus program starts with its label (`// layer L4`); the tool `ring` gives the lowest layer at which a file parses,
`layer` the highest word it uses, and both must agree with the label (`zed/corpus/LEDGER.md`). The resolver refuses a
form "with the step that compiles it" (L2.1). The bootstrap subset is then a measured object: Zed₀ is exactly the
117 grammar alternatives and 13 words corpus program 31 needs ("an alternative is needed when program 31's units stop
parsing, or parse to a different tree, without it"), with negative controls (an entry deleted makes the audit fail;
a stream program audits above Zed₀) and a Part B listing what the compiler needs beyond program 31, each with the
step that provides it (`zed/compiler/ZED0.md`).

### Z5. The specification's examples are tests

The sync tool copies all 149 `zed` fences of DESIGN.md and EXAMPLES.md into `zed/fences/`, each with its source line,
layer, ring and the `✗` marks of the variants that must be rejected (`zed/fences/rejections.txt`); every fence is a
test, and a separate check fails when the copies drift from the spec (`docs/layout.md`, "The design document's
fences"). The step tests are written "as a small real-world example, simplified until it tests just the thing it is
about", because "the user documentation is to be built from them" (`docs/layout.md`, "Tests").

### Z6. A hash never decides equality

Definitions are content-addressed: the BLAKE3-256 of a definition's canonical ZIR (`zedc/src/address.rs`, after the
canonicalizer `zedc/src/canon/`), which keys caches and imports. But "a key is its inputs, not a hash of them: a hash
never decides equality (DESIGN.md sec:theories, the trusted base)" (`zedc/src/query.rs`). The proof is a build:
the feature `short-addresses` keeps 8 bits of every address and digest, so distinct values collide constantly, and
that build must give the same snapshots as the full one, "every enabled fence's verdict ... every corpus program;
every step program on every corpus input" (`tests/steps/l01_37a_short_addresses/mod.rs`; STATUS.md's L1.37a row:
"600 sums give 601 nodes under 8-bit keys; a map trusting the key keeps at most 256"). Its control, run once: the
lowering trusting its key gave 232 nodes and failed 4 oracle tests.

### Z7. No thresholds: a limit may fail a run, never change an answer

"Nothing in Zed may rest on an arbitrary threshold, cap or budget whose crossing changes what the compiler does"
(`docs/thresholds.md`, the author's direction of 2026-10-30). The file inventories every such constant (search
budgets, fuel, piece limits, widening delays, `i64` ends) with what crossing it changed and the parameter-free
algorithm that replaced it: a monotone propagation whose lattice height the program fixes, or a fragment decided
exactly. What remains undecidable is *refused* with its own message naming the cause, never cut by a count
(`docs/reviews/xs.md`: "Count- or size-based refusal of a large SCC: a threshold" is rejected). Every remaining
precision tradeoff carries a comment beginning `Complexity tradeoff:` naming what is given up, why, the trigger, the
test and its row (`docs/reviews/tradeoffs.md`, 43 sites confirmed). Machine resources stay outside the rule:
"running out of them is a build failure, never a different answer".

### Z8. Complexity is tested, by instructions

`COMPLEXITY.md` holds the "Six standing rules" audit: O(n log n) or better, O(n²) only with a recorded reason. An
item closes when "a scaling test (size n against 8n, ratio at most 24 in instructions and, since SCALE-bytes, in
bytes allocated) passes". The measure is user-mode instructions from `perf_event` (the least of two runs), or the task
clock on a VM with no counter, failing only on three measurements over in a row (`STATUS.md`, the complexity audit;
`tests/src/`'s `scaling::check`, `check_order` for work of order n^k). The reason for bytes: an old resolver took
10.2 times the instructions at 8 times the size but 51.8 times the processor time, because vector moves count as few
instructions (STATUS.md, "Open points").

### Z9. Several independent implementations checked against each other

The kernel's reference evaluator (M0.9, `zed-kernel/src/eval.rs`), the interpreter, the memory plan's counting
machine and the Cranelift code run the same programs; the differential oracle (L1.37) compares their port writes
byte for byte, and the native run must also make exactly the machine's increments, decrements and allocations
(`tests/steps/l01_36_cranelift/corpus.rs`, `tests/steps/l01_37_oracle/`). The finite-model harness (M0.10) checks
laws 1-28 by enumeration on finite models. `zedc test --mutate` (L25.8) changes one operator or integer constant at
a time, skips mutants the optimizer proves equal, and reports the mutants no test kills. Negative controls are
listed in every step's row ("Negative controls: no memo of reads, lines from 0, `\r` kept, ...", L1.19), run once
and not committed.

### Z10. Precise reference counting decided statically

`zir/memory.rs`: each value edge is owned or borrowed (borrow inference), a dup before each owned use but the last,
a drop at the last use, and fusion: a `match` that owns its scrutinee frees the cell without a count write when the
count is 1 and moves the binders out. Reuse in place at count 1 (`with`, a one-key `patch`, a `map` over a dying
family) is L6.13. This is the lineage of Lean 4's and Koka's Perceus (*from memory*; the file cites neither). It
works because Zed values are immutable and acyclic until L6's `mutable`.

### Z11. Determinism, ports and replay

Ports are the only outside world. A wave reads its inputs once, buffers its writes and commits them when it ends; a
second push to one port in a wave is an error. A `Record` holds every input a wave read, and `zedc replay` runs the
program again from it alone (`zedc/src/run.rs`; L1.19). Bit-identity across cores (L3.15) and targets (L26.2's
"cross-target determinism suite") is planned.

### Z12. Staging inside the compiler

`mix` evaluates every demanded build-stage term (one that reads no port and no parameter) to a literal, but only when
its form shows it ends, with no fuel; "the Trick" specializes a function on a parameter of finite domain, only on the
values static calls pass, "at most one per call site, so the code grows at most linearly" (`zedc/src/stage.rs`).
`T::shape`'s `match` in `encode` runs at build, so a serializer specializes to string concatenations
(`zed/fences/design.md`, sec:introspection).

### Z13. Checking written in the language

Facets are user-written abstract domains: a lattice, a `holds` predicate and transfer rules (`parity` in
`zed/fences/design.md`, sec:checking_in_zed); proofs are values of a union type `Proof` (`#eval`, `#domain`,
`#rewrite`, `#induction`, ... `#assume(why)`), discharged on a ladder of rungs. The prelude's lattices are already Zed
(`zed/prelude/lattice.zed`), with the Rust copies checked by the tests until the Zed runs.

### Z14. The compiler as queries, ZIR as diffable text

Each stage is a query memoized under a key made of what it reads, with early cutoff ("an edit that leaves those
definitions alone ... reuses the row's result. This cutoff is the point of the structure", `zedc/src/query.rs`). ZIR's
text form is "flat, sorted, line-oriented": a line per region header, node or result, keyed by path (`r.0/0.0`), read
in any order, "so the form can be sorted, diffed and merged line by line" (`zedc/src/zir/text.rs`).

### Z15. The process

`docs/plan_dag.md`: 558 items, 2,092 edges, 80 levels, each edge justified by kind (code, measure, files-heavy; edges
of kind order, docs or files-light dropped on 2026-11-02 so items run in parallel worktrees). `STATUS.md`: "so that the
work can be restarted from it". `ZED_PORT_GAPS.md`: every step adds what its Rust relied on that Zed lacks, so row S
meets no surprises (ten gaps found, each a step now). Campaigns of cross-review (`docs/reviews/xe-history.md`,
`xs.md`): the same open problem given to "gpt-6.1-sol ... gemini-4-argon ... claude-opus-5-5", each critiquing the
others, then cross-check agents rebuilding every candidate against brute-force oracles ("6 generators × 200,000
systems"), then an orchestrator's synthesis; proposals that switch behaviour at a size are rejected on principle
(X7-X10).

## 3. Zed and Arbace, point by point

| | Zed | Arbace |
|---|---|---|
| Language | A new dataflow language, Dart-like syntax, statically typed by inference with set-theoretic and refinement types | Clojure extended with Java forms and Go forms; dynamic, its crops' languages each with their own semantics |
| Code as data | Not homoiconic; `::source` gives `Ast` values (L22, planned); `T::shape` (L11, planned); staging built | Homoiconic by construction: every crop's program is forms, the reader's data |
| Other languages | One language; interop planned through `@foreign`, `@export("c")`, LLVM IR, wasm | Ten crops as forms (Clojure, Java, Go, C, C++, OCaml, Scheme, Haskell, Rust, Python, with LLVM IR), read, eval, AOT and print for each |
| Core | Tier 0 lambda calculus plus seven generators over families; derived nodes carry certificates | `arbace.lang`'s `Compiler` and evaluator; per crop an evaluator (goi, ci, pyi, ...) over its forms |
| Compilers | Interpreter, reference evaluator, Cranelift JIT/AOT; MLIR and LLVM at L28 | c2j (namespaces → Java forms), j2g (Java forms → Go forms), Go's toolchain; static executables |
| Differential checks | Interpreter = native, byte for byte and in RC counts; oracle in CI | `bin/diffcheck` (evaluated = compiled, interpreted = translated), the oracle against the frozen jar, every crop against its reference compiler's recorded runs |
| Judges | insta snapshots, step tests, Zed `test` tables | Recorded results and classes of divergences, `bin/reap` from the records alone |
| Controls | Listed per step, run once, not committed | Committed per family, judged by the family's judge, must fail (GTS81 onward) |
| Complexity | Scaling tests n vs 8n by instructions, a standing rule | Wall-time budgets per tier (GT8), steps flagged at three times their passes; no complexity tests of the tools |
| Caching | Content addresses of canonical ZIR, query DB with cutoff; hashes confirmed by contents | `gate_key` and step keys over declared inputs, audited by strace; early cutoff by `:uses`; keys are SHA-256 digests |
| Gate | GitHub CI: fmt, clippy, tests, coverage report | `bin/gate`, keyed, audited, tiered, clean runs in a fresh clone |
| Bootstrap | Rust host as root; Zed₀ audited; row S with a three-stage fixpoint planned | Go-source seed; stage 0 = G1 = G2 = G3 verified; the JVM only as a frozen oracle |
| Bare metal | None; microcontrollers a study (L31), hardware (L27.3) planned | The box: TamaGo guests on KVM and Hypervisor.framework (parked) |
| Memory | Static RC, no GC | Go's GC (the runtime's own design open, AGENDA "Long term") |
| Records | STATUS.md rewritten; PROGRESS.md in the spec; plan DAG | JOURNAL.md append-only; AGENDA.md rewritten; surveys with numbered proposals |
| Age, size | 10 days, 895 commits, about 190,000 lines of Rust | About a month, ten crops, much larger |

What the two share is striking: a written plan of numbered steps, negative controls as a rule, independent
implementations compared, a three-stage fixpoint, records detailed enough to replay the work, the author's decisions
kept apart from the agents' proposals. Where they differ is the object: Zed designs one language from first
principles; Arbace represents many existing languages faithfully in one homoiconic substrate. So Zed's language
features (families, set-theoretic types, failures as values) mostly do not transfer, and its methods mostly do.

## 4. Insights for Arbace

### Worth adopting

1. **A hash never decides equality, and a short-hash build proves it** (Z6). Arbace leans on digests everywhere:
   `gate_key` and the store of passes, `aot.key`, `bin/build-go`'s print cache, `units.tsv`'s program units,
   `test/repr/java/expected/jdk26u.sha256`, the digests of stdout in Java's interpreted level, goi's table digests.
   They are SHA-256, so an accidental collision is not the risk. The risk Zed's rule catches is different: a cache
   whose *key* omits an input, or a comparison done on a digest of a *lossy* form, so two different things share a
   key by construction. Truncating digests to 8 bits makes such code visible because then every cache must still
   give the same answers, which only code that confirms by contents (or recomputes) can do. Where Arbace's design is
   "a hit skips the work", a short-key run turns false hits into wrong outputs or failing checks, which is the
   point. Cheap where a cache is local (the print cache); for the gate's store a false hit is a skipped check, so
   the honest form there is the audit (GT4's strace audit already covers the omitted-input half).
2. **No thresholds** (Z7). The rule fits Arbace's own standard ("every non-pass in a recorded class") and sharpens it:
   a limit may fail a run (a resource, recorded as such), never change an answer. Arbace's CPU limits and timeouts are
   already of the allowed kind (a timeout is a classed non-pass). What would need an inventory are fallbacks that
   switch the path an answer takes at some size or condition (an evaluator's recursion caps, a reader that falls back
   to another reader, a namespace loaded from the checkout instead of the embedded copy when they differ), each either
   made path-independent or recorded with its condition and its test.
3. **Scaling tests measured in instructions** (Z8). Arbace's budgets are wall time on a host whose load swings (GT8);
   the 3× flag catches a step that got slower, not a tool that went quadratic on the next, larger corpus. A handful of
   n-vs-8n tests on the tools that see the largest inputs (repr/go's native reader, repr/java's parser, j2g on one
   class, goi's checker, c2j on a namespace), measured by user-mode instructions, are robust to load. The host allows
   it in principle (`kernel.perf_event_paranoid` is 2, which permits user-mode counting of one's own processes, and a
   `cpu` event source exists; whether the VM exposes hardware counters is to verify, else the task clock with Zed's
   three-in-a-row rule). Zed's lesson about bytes (vector copies are cheap in instructions) applies to Go too.
4. **Mutation testing of the checks** (Z9). GT8 chooses essential subsets by coverage; coverage says a block ran, not
   that a wrong block would be caught. Arbace's controls (GTS81) prove a judge can fail; mutation proves a subset
   can. One operator or constant changed at a time in an evaluator's Go forms (`go/arbace/goi`, ci's back end) or in
   c2j's emitter, the essential subset run, the surviving mutants listed: a direct measure of what the 15-minute tier
   buys, and a guide for the subsets.
5. **The documentation's examples as checks** (Z5). Arbace's docs hold 144 `clojure` and 50 `forms` fences (most in
   `doc/classes/SPEC.md`, `doc/c/SPEC.md`, the surveys). A light check that each reads (and, where a fence states a
   result, evaluates to it) keeps the specs honest as the forms change, at seconds of cost.

### Worth adapting

6. **The bootstrap subset as a measured object** (Z4). Arbace's long-term goal ("written in Arbace all the way down")
   and the box both need to know what part of Arbace the self-hosting tools use. Zed's audit, which removes each
   grammar alternative and sees whether program 31 still parses the same, transfers as: which vars of `arbace.core`,
   which special forms, which Java forms and which JDK classes c2j, j2g, repr/go and `arbace.build.go` reach (RT.load's
   log, GT4's `ARBACE_LOADED_LOG`, already records the namespaces; vars are the next grain). Its use comes with the
   runtime's design.
7. **A port-gaps ledger** (Z15). `ZED_PORT_GAPS.md` lists what the host-language code relies on that the target lacks.
   Arbace's equivalent is the hand-written Go under `go/arbace/` (jrt, goi's back end, ci's runtime): what Go
   constructs it relies on (goroutines and channels, `unsafe`, assembly, the GC, `reflect`) that Arbace forms
   cannot yet express. That ledger is the list the "all the way down" goal must close, and grows cheaply if each
   change adds its line.
8. **Measured cross-review** (Z15). For a hard algorithmic piece (repr/java's inference tier, goi's checker's
   corner cases, a crop's native parser generator), two or three independent attempts in separate worktrees, each
   critiqued, then measured against a brute-force or reference oracle with controls before one is synthesized, is
   stronger than one implementation and one review. Within the user's rules: local agents only (no cloud sessions),
   and the Mac VM as one of the reviewers.
9. **Content-addressed units for c2j** (Z6, Z14). Zed keys compiled code by the canonical form of each definition,
   so an edit recompiles what changed. Arbace's AOT is keyed by the whole runtime (`aot.key`) and costs 2.5 to 5.5
   minutes a build. A per-namespace (or per-top-level-form) key over the analyzed form would cut that, but j2g's
   whole-program translation and Go's package build limit the gain; a study, not a proposal now.
10. **Replay records** (Z11). For the box and the REPL, a session's inputs (stdin, files read, time, randomness)
    recorded and replayed would make a box run reproducible byte for byte. When the box is unparked.

### Worth rejecting, with the reason

11. **Zed's syntax and type system for Arbace's core.** Arbace is s-expressions and Clojure's dynamic semantics by
    design; its crops' static semantics are their own languages' (go/types, javac, gcc), checked against those. A
    set-theoretic type layer over Arbace would be a new language, not a representation.
12. **Static reference counting as Arbace's memory model.** It works in Zed because values are immutable and acyclic.
    Clojure has cycles through vars, atoms, closures and lazy sequences, and every crop brings its own (Java's object
    graphs, Go's pointers, Python's reference cycles, which CPython itself collects with a tracing pass). Arbace's
    runtime design should know the option and its precondition; it is not a fit for the shared runtime.
13. **MLIR as a back end.** Arbace compiles to Go and treats LLVM IR as a crop. Zed's MLIR decisions (lower into
    upstream dialects rather than write a dialect, the text interface, certificates stopping at the lowering) are a
    sound record if Arbace ever targets LLVM directly; until then they are reading.
14. **A plan DAG of hundreds of items.** It serves Zed's many parallel worktrees; Arbace's agenda, surveys and the
    user's decisions already order its work, and the user prefers a brief agenda. The useful part, that an ordering
    edge needs a stated technical reason, is a habit, not a file.

## 5. Proposals (ZS1-ZS9)

Each with options, the recommended one first.

- **ZS1 A crop for Zed.**
  - What it would be. *Zed forms*: Zed units as Arbace forms after its AST (`zedc::ast`'s node kinds: `Function`,
    `Binding`, `Push`, `For`, `Match`, ...), with spans and comments kept in mode `:full`. *read*: repr/zed, a port of
    the PEG grammar (`zed/grammar/{lexical,syntax}.peg`, 357 lines with the layer tags, MIT) into an Arbace packrat
    parser, judged by zedc's parser through a helper dumping its trees (as `tools/godump` or `tools/hsdump` do); the
    319 bracketing cases of `zed/grammar/cases.txt` and the 149 fences as the first corpus. *eval*: zi, the kernel's
    seven generators and Tier 0 interpreted from the elaborated kernel terms, judged by `zedc run` on the corpus
    inputs (the `.out` files). *AOT*: kernel terms to Go forms. *print*: forms to text, `:full` byte for byte, and
    zedc's canonical layout (`zedc fmt`) as the lines mode.
  - Options: (a) not now; revisit when DESIGN.md is public or Zed reaches row S (its own compiler in Zed, a stable
    Zed₀ to target), the trigger checked at each survey round; (b) read and print only, at a pinned commit, the L1-L6
    grammar, as an experiment of a few days outside the matrix and the gate; (c) a full row of the REAP matrix now.
  - *Recommended: (a).* The language changes by about ninety commits a day, its specification is private, most of its
    semantics (L3 onward) is not yet executable, and its corpus is 31 programs: a row would freeze an early draft and
    judge it against a moving oracle. Building zedc as an oracle also means running its code, which this survey did
    not do and which needs the user's decision.
- **ZS2 Where a digest decides equality.**
  - Options: (a) an audit listing every place an Arbace tool takes a digest or key match for equality (the gate's
    store, `aot.key`, the print cache, `units.tsv`, `jdk26u.sha256`, the stdout digests, goi's tables), each with
    what its key covers and whether a hit is confirmed by contents or recomputed, then a short-key run (keys
    truncated to 8 bits by an environment variable) for the local caches only, which must give the same outputs;
    (b) the audit alone; (c) nothing: SHA-256 and GT4's input audit suffice.
  - *Recommended: (b)*, then (a) for the print cache if the audit finds a key that omits an input.
- **ZS3 The thresholds inventory.**
  - Options: (a) a section (in GATE-SURVEY.md or a crop's survey) listing every constant or condition whose crossing
    changes an answer rather than failing a run: evaluators' depth or step caps, fallbacks between readers or hosts,
    the embedded-versus-checkout switch, each made path-independent or recorded with its condition and a test that
    crosses it; (b) Zed's rule adopted whole as a standing rule for new code; (c) nothing.
  - *Recommended: (a)*, and (b) for new code once the inventory shows the cost.
- **ZS4 Scaling tests for the tools.**
  - Options: (a) a small runner (`bin/scaling`, outside the essential tier, in the nightly) with n-vs-8n cases for
    four or five tools (repr/go's native reader, repr/java's parser, j2g on one class, goi's checker, c2j on a
    namespace), each case's input generated (a file of n declarations, n nested blocks), judged by user-mode
    instructions (`perf_event`, with Zed's ratio of 24 for linear work), bytes allocated beside it (Go's
    `runtime.MemStats`), the task clock with three failures in a row where no counter opens, and a control (a
    deliberately quadratic case) that must fail; (b) the same in the essential tier; (c) nothing: GT8's budgets and the
    3× flag suffice.
  - *Recommended: (a)*, after checking that the host's VM exposes a hardware instruction counter.
- **ZS5 Mutation runs of the essential subsets.**
  - Options: (a) `bin/mutate` (by hand and on quiet nights, light tier), one operator or constant of a chosen Go-forms
    file of an evaluator (`go/arbace/goi`, ci's back end) or of c2j's emitter changed at a time, the program rebuilt
    and the matching essential subset run, the surviving mutants written as a record; the survivors feed GT8's subset
    choices; (b) mutate the subject programs instead (Zed's way, for user code; for Arbace a weaker measure); (c)
    nothing.
  - *Recommended: (a)*, starting with one evaluator and a budget of a few hundred mutants; each mutant costs a rebuild,
    so a light driver (the goi back end interpreted, or a package-level build) matters.
- **ZS6 The docs' fences as a check.**
  - Options: (a) a light step reading every `clojure` and `forms` fence of `doc/` with the reader, the fences marked as
    deliberately invalid skipped by a marker, and a fence ending in a stated result (`;=> ...`) evaluated on stage 0;
    (b) reading only; (c) nothing.
  - *Recommended: (b)* first (seconds, no new conventions), (a) when a spec's examples are worth running.
- **ZS7 Measured cross-review for named hard problems.**
  - Options: (a) for a problem the coordinator names, two or three independent attempts by local agents in separate
    worktrees (the Mac VM one of them), each critiquing the others, every attempt measured against a brute-force or
    reference oracle with controls, the synthesis recorded with its measurements; (b) the current practice, one
    implementation and a review; (c) Zed's form with models of other vendors.
  - *Recommended: (a)*, opt-in per problem; (c) is outside the user's rules (no cloud sessions) and needs the user.
- **ZS8 The bootstrap subset and the port-gaps ledger.**
  - Options: (a) when the runtime's design opens (AGENDA, "Long term"), an audit of the vars, special forms, Java forms
    and JDK classes the self-hosting tools reach, and a ledger of the Go constructs `go/arbace/` relies on that Arbace
    forms lack, each change adding its line; (b) the ledger now, the audit then; (c) neither.
  - *Recommended: (a)*: both serve a decision not yet open, and the box is parked.
- **ZS9 Vendoring.**
  - Options: (a) nothing vendored; if ZS1 is ever taken, the PEG grammar and the corpus under MIT with the notice in
    LICENSE.md and the origin in doc/ARBACE.md and the journal (`imbl`, MPL-2.0, is a dependency of Zed's, not
    something to vendor); (b) vendor the grammar and corpus now as test data.
  - *Recommended: (a).*

## 6. Risks and open questions

- **What Zed is may change.** Ten days and 895 commits; the plan has been rewritten several times in that span (the
  "Corrections to the supplied graph" in `docs/plan_dag.md`; thresholds removed on 2026-10-30). This survey describes
  one commit. Its promises beyond L1 (parallel scheduling, bit-identity, incremental recomputation, joins planned by
  cost) are not built and may not survive contact with the code.
- **The specification is private.** Everything about the design beyond its fences, the grammar and the
  implementation's own records is inference. Whether DESIGN.md will be published is open.
- **Scale of ambition.** A plan from Presburger arithmetic to GPUs, Verilog, databases and an editor, at layer 1 of
  35, is a warning as much as an inspiration; Arbace's own ten crops face the same question of budget, which GT8 and
  GT9 answer with tiers and shares. Zed has no equivalent of GT8 yet: its record shows tests failing on time at load
  40 to 59 (STATUS.md, "Test baseline").
- **Agent-written records.** Zed's records (STATUS.md at 471 KB) show the cost of keeping everything: a handoff file
  that size is hard to read for a person. Arbace's split (an append-only journal, a short agenda, surveys) is the
  better shape; nothing here argues for changing it.
- **Counters on the host.** ZS4 depends on the VM exposing hardware counters; without them the task clock under
  Arbace's load is noisy, and Zed's three-in-a-row rule is the fallback, at three times the cost of a failing case.
- **Mutation cost.** ZS5's mutants each need a rebuild of Go code; without a light path (interpreting the mutated
  back end, or building one package) a few hundred mutants are hours.
- **Licenses.** MIT allows vendoring with the notice; nothing is vendored by this survey. Running zedc as an oracle
  (ZS1 (b) or (c)) would mean building a Rust workspace with Cranelift from an untrusted repository, which needs the
  user's decision and a pin.
- **Open:** whether Zed's ZIR text (sorted, path-keyed lines) or its kernel (seven generators) would be worth a
  representation in forms on its own, as a small dataflow IR beside LLVM IR's, if Arbace ever wants one for its own
  optimizer; no current Arbace need asks for it.

## 7. Sources

- `https://github.com/zedlanguage/zed` at `d00a13469e3067798e0fdebcfdd04838e9869c19` (2026-11-04), shallow clone in
  `.tmp/zed-survey/src` (kept as scratch): `README.md`, `LICENSE`, `Cargo.toml`, `STATUS.md`, `COMPLEXITY.md`,
  `ZED_PORT_GAPS.md`, `docs/layout.md`, `docs/plan_dag.md`, `docs/thresholds.md`, `docs/reviews/{tradeoffs,xs,
  xe-history}.md`, `zed/compiler/{ZED0,MLIR}.md`, `zed/fences/{design,examples}.md`, `zed/grammar/*.peg`,
  `zed/corpus/` (`01-word-count.zed`, `02-histogram.zed`, `31-typechecker.zed`, `LEDGER.md`), `zed/prelude/lattice.zed`,
  `crates/zed-kernel/src/{term,print}.rs`, `crates/zedc/src/{address,query,stage,run}.rs`,
  `crates/zedc/src/zir/{mod,text,memory}.rs`, `crates/*/Cargo.toml`, `tests/steps/l01_21_test_runner/zed/`,
  `tests/steps/l01_36_cranelift/corpus.rs`, `tests/steps/l01_37_oracle/mod.rs`,
  `tests/steps/l01_37a_short_addresses/mod.rs`, `.github/workflows/ci.yml`.
- GitHub's API, 2026-11-04: `repos/zedlanguage/zed` (created 2026-10-25), its contributors (one, 895 commits), issue
  #1, `orgs/zedlanguage/repos` (one public repository), `users/lukehutch/repos`.
- Arbace: CLAUDE.md, doc/AGENDA.md, doc/GATE-SURVEY.md (GT8; GT9 on the branch `gate-crops`),
  doc/python/PYTHON-SURVEY.md (§12's form), doc/FUTAMURA-SURVEY.md (headings); the host's
  `/proc/sys/kernel/perf_event_paranoid` (2) and `/sys/bus/event_source/devices/`.
