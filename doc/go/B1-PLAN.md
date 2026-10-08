# B1: Arbace in the box via gc — plan

Status: plan for the user's review (2026-10-08). B1 is the milestone after g2c's G0-G2
([G2C-SURVEY.md](../G2C-SURVEY.md) §5.2, §7; the user's decisions of 2026-10-07: G0-G2, then
B1; the box built by gc + TamaGo first, gc replaced layer by layer later).

**Goal.** A self-sustaining Arbace REPL in a virtual sandbox: an image built by Go's toolchain
(gc + TamaGo, go1.27.1) from sources that Arbace holds as forms, running on go-whim's monitor on
/dev/kvm (amd64, Alpine edge host) and Hypervisor.framework (arm64, macOS host). Every source
of the image, Go's runtime and TamaGo included, is visible and editable as forms in Arbace;
gc, the assembler and the linker are still Go's (survey §5.2, step b1).

## Where things stand

- **The box exists, outside Arbace.** `/root/go-whim`: the monitor `vmm/` (KVM and
  Hypervisor.framework, amd64 and arm64, SMP, a doorbell ABI of 17 host calls, seccomp), a
  TamaGo board `guest/tamago/board` answering TamaGo's runtime hooks through the doorbell, and
  Joker as a REPL in the box (23 MB image, 20-60 ms start; `doc/LISP-SANDBOX.md`), with a
  closure-compiling backend 2-5 times faster than its bytecode VM. KVM is available here
  (`/dev/kvm`); arm64 is tested here only under QEMU (go-whim `doc/GUEST.md`, "Testing arm64").
- **g2c closes the round trip** (std and `$GOROOT/test`, amd64 and arm64, tree, export data and
  object code; `bin/gate --full`). So any Go the image needs can be held as Go forms and
  printed back to the same program.
- **Arbace's runtime is Java.** `arbace/lang/` is 142 class-form files, about 36,000 lines,
  compiled to JVM classes. It leans on the JDK: `java.lang.reflect` (the Reflector, 77
  references), `java.util` (45), `java.io` (31), `java.util.concurrent` and atomics, regex,
  `java.math`, `java.lang.invoke` (the `invokedynamic` sites), `java.lang.ref`. Its compiler
  (`arbace.lang.Compiler`) analyses forms into `Expr` objects and emits JVM bytecode with ASM.
  `arbace/core.clj` and the other namespaces are Clojure on top.

So B1 is mostly not g2c work: it is getting Arbace's *runtime and evaluator* onto Go.

## The strategy (to decide: D1, D2)

1. **The runtime by translation, not by rewrite (D1).** A converter **c2g** turns class forms
   into Go forms, as j2c turned Java into class forms: classes become structs with method sets,
   interfaces stay interfaces, inheritance becomes embedding plus explicit virtual dispatch,
   exceptions become panics, static initialisers an init order, Java's arithmetic Go's with the
   needed wrapping and checks. Under it, **jrt**, a small Java runtime in Go forms covering
   exactly the JDK surface `arbace/lang` uses (measured first, step 3). The alternative is a
   hand-written runtime in Go forms, as Joker is: faster to a first REPL, but a second
   implementation of Clojure that drifts from Arbace's, and it gives up "Arbace represents its
   own implementation". Recommendation: c2g + jrt.
2. **Evaluation by interpretation first (D2).** In the box there is no JVM to load bytecode.
   Keep `Compiler`'s analyser (translated by c2g) and replace its bytecode back end with an
   evaluator over the `Expr` tree: Clojure's `Expr` classes already have `eval`, which
   Compiler uses for top-level forms; `FnExpr` and the rest get closures (Joker's closure
   backend is the model). Later, AOT: namespaces compiled to Go forms and built into the image
   (survey §5.2 (ii)), for start-up and speed. Recommendation: the evaluator first.
3. **Develop on the host first.** Every step below runs first as an ordinary `linux/amd64` Go
   program on the host, where it can be tested against the JVM Arbace side by side
   (differential testing: the same forms, the same printed results), and only then in the box.

## Steps

Each step has a gate; nothing advances on an open gate. Estimates are agent-assisted days at
the pace of the class forms and g2c work, rough, growing with the step number.

| step | content | gate | estimate |
|---|---|---|---|
| **1 Box bring-up** | `bin/box`: build a TamaGo image from a Go program and run it on go-whim's monitor (D3: how Arbace depends on go-whim). First program: hello, written as Go forms and printed by g2c | prints and exits on KVM amd64; the arm64 image boots under QEMU | 1-2 |
| **2 The image's own Go as forms** | the board package, TamaGo's runtime and std as used by the image, converted by g2c and printed; the image built from the printed sources | the image is byte-identical to one built from the originals (gc is reproducible), amd64 and arm64 | 1-2 |
| **3 Measure the Java surface** | every JDK class, method and field `arbace/lang` and the namespaces use (from the class forms compiler's resolution, not grep), grouped by package; what each needs in Go | a table in the c2g spec; the user's review of what to translate, shim, or cut (reflection, `invokedynamic`, class loading) | 1 |
| **4 c2g spec** | `doc/go/C2G-SPEC.md`: class forms → Go forms (types, objects and dispatch, exceptions, statics, primitives and arithmetic, strings (D4), arrays, threads and locks, reflection's subset) | the user's review, as for the class forms and Go forms specs | 2 |
| **5 jrt** | the Java runtime subset in Go forms: `Object` protocol, `String`/`StringBuilder`, boxed numbers, `Math`, `BigInteger`/`BigDecimal` over `math/big`, the `java.util` collections used, atomics and locks over `sync`, threads over goroutines, `ThreadLocal`, regex (D5), minimal reflection | its own tests on the host; the round trip of its forms | 4-8 |
| **6 c2g converter** | class forms → Go forms, in dependency order: `Util`, `Murmur3`, `Numbers`, the persistent collections, `Symbol`/`Keyword`/`Var`/`Namespace`, seqs, `LispReader`, printing, `RT` | per class, differential tests against the JVM Arbace on the host: the same operations, the same results | 6-10 |
| **7 The evaluator** | `Compiler`'s analyser through c2g, with an `Expr` evaluator in place of bytecode emission (closures for `fn*`, `loop`/`recur`, `try`, `letfn`, `deftype`/`reify` through jrt's dispatch, `defclass` out of scope at first) | `arbace/core.clj` loads from source on the host; a growing part of Clojure's test suite passes, recorded per namespace as `test/arbace-results.edn` is | 6-10 |
| **8 REPL in the box** | `arbace.main`'s REPL on the monitor's console; the core namespaces loaded at build time (pre-read or pre-analysed) to start fast | a REPL on KVM amd64: read, eval, print, `doc`, errors; start time and image size measured against Joker's guest | 2-3 |
| **9 arm64** | the same image for arm64: QEMU here, Hypervisor.framework on the Mac (the user runs it) | the REPL on HVF arm64 | 1-2 |
| **10 Speed** | closure compilation of `Expr`s (Joker's 2-5x), then AOT of namespaces to Go forms built into the image | benchmarks against the JVM Arbace and Joker's guest; the suite still passes | open |

Rough total: 25-45 days, consistent with the survey's 6-12 weeks.

**Checks.** Steps 2, 5 and 6 add to `bin/gate --full`: the image round trip (byte-identical),
jrt's tests and the differential tests. Step 8 adds a box smoke test (boot, evaluate, exit) when
`/dev/kvm` is present.

## Decisions for the user

- **D1 The runtime.** c2g + jrt (recommended), or a hand-written runtime in Go forms.
- **D2 Evaluation.** An evaluator over `Compiler`'s `Expr` tree first, AOT later
  (recommended), or AOT to Go forms first (fast code, but no `eval` in the box until an
  evaluator exists anyway).
- **D3 go-whim.** Use `/root/go-whim`'s monitor and board from its checkout, pinned by commit
  (recommended for now: it is the user's own project and still moving), or vendor them into
  Arbace (then they become Arbace's forms too, which "all the way down" eventually wants).
- **D4 Strings.** Java's UTF-16 `String` semantics in jrt (recommended: `count`, `subs`,
  `.charAt` and the suite behave as on the JVM), or Go's byte strings (simpler and faster, but
  Clojure's string functions change meaning).
- **D5 Regex.** Go's `regexp` (RE2) with recorded differences from `java.util.regex`
  (recommended first: no backreferences or lookaround), or a port of `java.util.regex` later.
- **D6 Scope of the first REPL.** Leave out at first: `defclass` and the class forms (their
  compiler emits JVM classes), `gen-class`, `proxy`, JVM interop beyond jrt's classes, agents'
  executors beyond goroutines. Recommended; each comes back as its Go-side meaning is defined.

## Risks

- **jrt's size.** Reflection-driven interop (`Reflector`) has no Go equivalent; Clojure code
  that calls Java methods by name works only on jrt's classes. Measure (step 3) before
  committing to scope.
- **Semantic drift between JVM and Go builds.** Mitigated by differential testing on the host
  for every step and by Clojure's suite.
- **Performance of an interpreter.** Joker's numbers are the baseline; closure compilation and
  AOT (step 10) are the remedy, and a native back end is B2-B4.
- **go-whim and TamaGo move.** Pin both; TamaGo's go1.27.1 is the pinned toolchain.
