# B1: Arbace on Go — plan

> Names: since 2026-10-17 g2a is repr/go and j2a repr/java (doc/NAMING-SURVEY.md, "Decided"):
> `arbace.g2a.*` is `arbace.repr.go(.*)`, its build `arbace.g2a.build` is `arbace.build.go`;
> `bin/g2a convert` is `bin/repr-go read` (`bin/g2a read`: `read --dump`), `bin/g2a build`
> `bin/build-go`, `bin/g2a-print` `bin/repr-go print`, `bin/g2a-check` `bin/repr-go check`,
> `bin/g2a-convert-tests` and `-print-tests` `bin/repr-go test read` and `test print`,
> `bin/g2a-build-tests` `bin/build-go test`; `G2A_GOROOT` is `ARBACE_GOROOT`, the other `G2A_*`
> `REPR_GO_*`, `J2A_*` `REPR_JAVA_*`; `bin/j2a-check` is `bin/repr-java check`. This document
> uses the current names throughout, its dated passages included; branch names keep theirs.

Status: plan, revised at the user's proposal (2026-10-08); the user decided D1, D2, D4, D5 and D6
the same day (below); D6 was partly reversed on 2026-10-09 and 2026-10-10 (`proxy`, the fork-join
pool and streams, dates, `defclass`, files, sockets, processes, java.time, java.util.concurrent,
java.security's basics, java.text: D6 below). Steps 0-8 are done: B1a ended with its freeze on
2026-10-11 (the branch `arbace-for-golang`, the tag `arbace-for-go1.27.1-v1`). B1b, the box,
started the same day (below): S0, S1, S2 and S6's amd64 half done, S3 and S5 done on amd64
(amendments BX30-BX36 and BX40-BX44 accepted). Since done: the box on amd64 and on arm64
(KVM/arm64, the Mac's Hypervisor.framework), the milestones `arbace-box-amd64-v1` and
`arbace-box-arm64-v1` (2026-10-12, [BOX-NOTES.md](BOX-NOTES.md)); then the standalone Arbace's
S1-S8 ([../STANDALONE-NOTES.md](../STANDALONE-NOTES.md), BOX-NOTES.md): the milestones
`arbace-seed-is-go-v1` (2026-10-13, S1-S4) and `arbace-inside-itself-v1` (2026-10-15, S5-S8, the
box rebuilding Arbace inside itself byte for byte). B1 is the milestone after repr/go's G0-G2
([G2C-SURVEY.md](../G2C-SURVEY.md) §5.2, §7). The user's decisions of 2026-10-07 put the box built
by gc + TamaGo first; on 2026-10-08 the user proposed to postpone the box and arm64, and to reach
first **a standalone static `linux/amd64` Go executable of Arbace**, testable as an ordinary user
process on this machine. B1 is split accordingly:

- **B1a (now; done, frozen 2026-10-11): Arbace as a native Go executable.** A static binary
  (`CGO_ENABLED=0`, go1.27.1, `GOOS=linux GOARCH=amd64`) running Arbace's REPL, built by gc from
  sources Arbace holds as forms. B1a is a milestone that ends with a freeze, as the JVM state was
  frozen on `arbace-for-java-26`: a well-known branch and tag, advertised in the docs (the user's
  intent, 2026-10-08). Proposed names (the user, 2026-10-08): the branch `arbace-for-golang`, the
  tag `arbace-for-go1.27.1-v1` after the pinned Go toolchain, with `-v2`... for fix releases on the
  same toolchain, as `arbace-for-java-26-v1`/`-v2` (the user, 2026-10-10). It includes
  `linux/arm64`: Go cross-compiles the same program, and the arm64 executable is tested here under
  QEMU's user-mode emulation (`qemu-aarch64`).
- **B1b (later; done: the milestones `arbace-box-amd64-v1` and `arbace-box-arm64-v1`,
  2026-10-12): the box.** The same program built with TamaGo (`GOOS=tamago`) on go-whim's
  monitor, amd64 on /dev/kvm, then arm64 on Hypervisor.framework.

## Where things stood (2026-10-08)

- **repr/go closes the round trip** for `tamago` (std and `$GOROOT/test`, amd64 and arm64, tree,
  export data and object code; `bin/gate --full`). Any Go the executable needs can be held as
  Go forms and printed back to the same program. The helper and the oracle take the
  configuration as a parameter.
- **Arbace's runtime is Java.** `arbace/lang/` is 142 class-form files, about 36,000 lines,
  compiled to JVM classes. It leans on the JDK: `java.lang.reflect` (the Reflector, 77
  references), `java.util` (45), `java.io` (31), `java.util.concurrent` and atomics, regex,
  `java.math`, `java.lang.invoke` (the `invokedynamic` sites), `java.lang.ref`. Its compiler
  (`arbace.lang.Compiler`) analyses forms into `Expr` objects and emits JVM bytecode with ASM.
  `arbace/core.ae` and the other namespaces are Clojure on top.

So B1a is mostly not repr/go work: it is getting Arbace's *runtime and evaluator* onto Go.

## The strategy (to decide: D1, D2)

1. **The runtime by translation, not by rewrite (D1).** A converter **a2g** turns class forms
   into Go forms, as repr/java turned Java into class forms: classes become structs with method sets,
   interfaces stay interfaces, inheritance becomes embedding plus explicit virtual dispatch,
   exceptions become panics, static initialisers an init order, Java's arithmetic Go's with the
   needed wrapping and checks. Under it, **jrt**, a small Java runtime in Go forms covering
   exactly the JDK surface `arbace/lang` uses (measured first, step 1). The alternative is a
   hand-written runtime in Go forms, as Joker is: faster to a first REPL, but a second
   implementation of Clojure that drifts from Arbace's, and it gives up "Arbace represents its
   own implementation".
2. **Evaluation by interpretation first (D2).** A Go executable cannot load JVM bytecode. Keep
   `Compiler`'s analyser (translated by a2g) and replace its bytecode back end with an
   evaluator over the `Expr` tree: Clojure's `Expr` classes already have `eval`, which Compiler
   uses for top-level forms; `FnExpr` and the rest get closures (go-whim's closure backend for
   Joker is the model). Later, AOT: namespaces compiled to Go forms and built into the
   executable (survey §5.2 (ii)), for start-up and speed (since done: standalone S6, the default).
3. **Differential testing against the JVM.** The JVM Arbace is the oracle: the same forms
   evaluated by both, the same printed results; Clojure's test suite run by both, recorded per
   namespace as `test/arbace-results.edn` is (removed from main in S4, 2026-10-12, with the JVM
   build: the baseline is `test/baseline-results.edn`, the Go build's `test/arbace-go-results.edn`).
4. **The OS in one place.** jrt's use of the operating system (files, stdin/stdout, time,
   environment, threads' OS side) goes through one small host interface, so that B1b swaps it
   for the monitor's host calls without touching the rest.

## Steps of B1a

Each step has a gate; nothing advances on an open gate. Estimates are agent-assisted days at
the pace of the class forms and repr/go work, rough, growing with the step number.

| step | content | gate | estimate |
|---|---|---|---|
| **0 repr/go for linux** | the round trip and its references for `linux/amd64` and `linux/arm64` (std has more files there: syscalls, `os`, `net`); the build of a Go-forms program into a static executable for either architecture (`bin/build-go`). *Done* | the round trip passes for both at all three levels; a hello program written as Go forms builds statically for both and runs (arm64 under `qemu-aarch64`) | 1 |
| **1 Measure the Java surface** | every JDK class, method and field `arbace/lang` and the namespaces use (from the class forms compiler's resolution, not grep), grouped by package; what each needs in Go. *Done ([JAVA-SURFACE.md](JAVA-SURFACE.md))* | a table in the a2g spec; the user's review of what to translate, shim, or cut (reflection, `invokedynamic`, class loading) | 1 |
| **2 a2g spec** | `doc/go/A2G-SPEC.md`: class forms → Go forms (types, objects and dispatch, exceptions, statics, primitives and arithmetic, strings (D4), arrays, threads and locks, reflection's subset). *Done; amended since (A2G-SPEC §16)* | the user's review, as for the class forms and Go forms specs | 2 |
| **3 jrt** | the Java runtime subset in Go forms: `Object` protocol, `String`/`StringBuilder`, boxed numbers, `Math`, per [JAVA-SURFACE.md](JAVA-SURFACE.md) and its decisions: the plain Java of the measured closure translated from jdk26u's source with repr/java (collections, `BigInteger`/`BigDecimal`, `Character`, `Formatter`, `java.util.regex` (D5), `ConcurrentHashMap`), hand-written the VM's edge (UTF-16 `String`/`StringBuilder`, reflection over a2g's member tables, atomics, locks, executors and threads over Go's `sync` and goroutines, `ThreadLocal` over a goroutine-local slot in the Go runtime, a small `Date` on Go's `time`); the host interface. *Done* | its own tests (regex: Java's behaviour, checked against the JVM on a corpus of patterns); the round trip of its forms | 10-15 |
| **4 a2g converter** | class forms → Go forms, in dependency order: `Util`, `Murmur3`, `Numbers`, the persistent collections, `Symbol`/`Keyword`/`Var`/`Namespace`, seqs, `LispReader`, printing, `RT`. *Done* | per class, differential tests against the JVM Arbace: the same operations, the same results | 6-10 |
| **5 The evaluator** | `Compiler`'s analyser through a2g, with an `Expr` evaluator in place of bytecode emission (closures for `fn*`, `loop`/`recur`, `try`, `letfn`; `deftype`/`reify` through jrt's dispatch). *Done 2026-10-09 (phase 2B)* | `arbace/core.ae` loads from source; a growing part of Clojure's test suite passes, recorded per namespace | 6-10 |
| **6 The executable** | `bin/arbace-go` (name to choose): static binaries for `linux/amd64` and `linux/arm64` with `arbace.main`'s REPL, `-e`, scripts; the core namespaces prepared at build time (pre-read or pre-analysed) to start fast. *Done 2026-10-09: pre-analysed, the image of prepared namespaces (EXEC-NOTES.md, amendment U1); start 4.2 s to 0.32 s on amd64* | a REPL as a user process: read, eval, print, `doc`, errors; start time and size measured against the JVM launcher (`bin/arbace`, then `bin/arbace-j`; removed from main in S4, on `arbace-for-java-26` as `bin/arbace`) and Joker; a smoke test in `bin/gate`; the arm64 binary passes the same under `qemu-aarch64` | 2-3 |
| **7 Speed** | closure compilation of `Expr`s, then AOT of namespaces to Go forms built into the executable. *Done 2026-10-10: 7a the running program's speed (amendments O1, O2, O4, O5; O3 dropped), 7b the evaluator's closure compilation (EC1-EC6) ([SPEED-NOTES.md](SPEED-NOTES.md)); AOT of namespaces to Go not scheduled (the user, 2026-10-10; since done: standalone S6, the default)* | benchmarks (`bin/arbace-bench`'s workload, on `arbace-for-java-26`) against the JVM Arbace; the suite still passes | open |
| **8 The freeze** | the docs, the gate, and a branch and tag for the static executables, as for `arbace-for-java-26`; the READMEs diverge as at that freeze: main's gains a section "Arbace for Golang" after "Arbace for Java 26" (the branch `arbace-for-golang`, its tags, a link to the branch's freeze document), and the branch's README describes it locally (its own freeze document, how to build and run the executables, the releases); main's journal starts over, as at the JVM freeze: the history up to the freeze stays on `arbace-for-golang`, its tag and in git, and main's new journal opens with a pointer to them. *Done 2026-10-11: the branch `arbace-for-golang` and the tag `arbace-for-go1.27.1-v1`* | the user's confirmation | 1 |

The `.ae` rename (Arbace's sources from `.clj` to `.ae`, one self-contained change; the user's
decision of 2026-10-09) was step 8, the last before the freeze; the user moved it on 2026-10-10 to
right after the freeze, as the first task (AGENDA.md), and it was done then: VENDOR-NOTES.md,
hand change 19, and A2G-SPEC.md §10.3.

Rough total (as planned): 25-43 days.

**Checks.** Step 0 adds the `linux/amd64` round trip to `bin/gate --full`; steps 3 and 4 add
jrt's tests and the differential tests; step 6 adds the executable's smoke test to `bin/gate`
and its suite run to `--full`. The essential gate's smoke test (amendment U5, accepted
2026-10-09) runs on an executable cached by a hash of its inputs (`bin/arbace-go --gate`,
`.tmp/arbace-go-gate`), built again, beside the suite, only when they changed. The smoke test also runs the REPL on a
pseudo-terminal under rlwrap on amd64 (amendment RL3, accepted 2026-10-10; about 5 s).
`bin/gate --full`'s suite on Go runs on the test build (amendment FX1, accepted 2026-10-10):
`bin/arbace-go --build-tests` builds `arbace-tests`, the program with the suite's Java fixtures
converted by repr/java and translated in (the shipped executable does not carry them), beside the
shipped one (about 3 more minutes).
**D7** (performance, deferred to step 7; A2G-SPEC §13.4): the start's collector setting is step
6's (amendment U4: `GOGC=400` while the program starts); the running program's is step 7a's.
*Closed by amendment O1 (accepted 2026-10-10, SPEED-NOTES.md):* `GOGC=200` and a 64 MiB
minimum heap set by jrt's initialization, reference arrays and strings in one allocation (O2).

## B1b, after the freeze: the box

The focus right after the `arbace-for-golang` freeze and the `.ae` rename (the user,
2026-10-10).

From the executable: the configuration becomes `GOOS=tamago` (TamaGo's go1.27.1), jrt's host
interface is answered by go-whim's doorbell through its TamaGo board, and the image runs on its
monitor. Then: the image's own Go (board, TamaGo's runtime, std) as round-tripped forms, the image
byte-identical to one built from the originals; amd64 on /dev/kvm here; arm64 under QEMU here and
on Hypervisor.framework on the user's Mac. How Arbace depends on go-whim (its checkout pinned by
commit, or vendored) is decided then. The survey and its stages S0-S6:
[BOX-SURVEY.md](BOX-SURVEY.md). Decided 2026-10-11 (D3, D8-D14, all as recommended): vendored as Go
forms. S0 done 2026-10-11: go-whim's box copied unchanged as Go forms under `box/`, proved by the
round trip's oracle, byte-identical builds of go-whim's monitor and guests and go-whim's tests on
/dev/kvm ([BOX-NOTES.md](BOX-NOTES.md)). S1 done 2026-10-11: the same program built with
`GOOS=tamago` runs its REPL, `-e`, `-m` and scripts in the box on /dev/kvm, on one and four vCPUs,
under Arbace's own launcher `arbace-box` (`bin/arbace-go --build --box`, `--smoke --box`;
BOX-NOTES.md, S1; amendments BX1-BX10 accepted). S2 done 2026-10-11 (BOX-NOTES.md, S2): the image
mapped (BX11), the guest's symbol table cut to the monitor's symbols, a memory limit in the box,
the watchdog kept off, the suite's file tests and reducers passing in the box (the evaluator's tail
calls clear the method's object, embedded namespaces load once), `fold` scaling on 4 vCPUs (the
evaluator's yield counted per thread); start and suite times against native (amendments BX12-BX17
proposed; accepted 2026-10-11, BOX-NOTES.md, S2). S6's amd64 half done 2026-10-11 (BOX-NOTES.md,
S6): the guest's own Go, TamaGo's runtime and standard library and its `goos` module, held as Go
forms in `tamago/` (`bin/tamago-vendor`), the box's guest built from them printed by default,
byte-identical to the guest built from the originals (without build IDs; amendments BX20-BX24
accepted). S3 done on amd64 2026-10-11 (BOX-NOTES.md, S3): the store, blobs by SHA-256 and refs
with compare-and-set, in a host directory (`ARBACE_BOX_STORE`, `arbace-box --store DIR`), the
namespace `arbace.box` over it in the box (the monitor's calls) and natively (jrt's directory
store, the same format, one directory shareable), scripts and namespaces loaded from refs
(`ref:NAME`, `ARBACE_PATH` entries `store:PREFIX`), the in-memory file system's snapshot as a tar
(`arbace.box/save-fs!`, `ARBACE_BOX_FS`), the launcher's store tool (amendments BX30-BX36
accepted). S5 done on amd64 2026-10-11 (BOX-NOTES.md, S5; D12 decided: a stream proxy): TCP streams
through the monitor's calls under the launcher's `ARBACE_BOX_NET` policy, jrt's `NetHost` in the
box, the sockets' oracle corpus and `bin/net-check` in the box, the socket REPL from the host
(amendments BX40-BX44 accepted).

## Decisions (the user's, 2026-10-08)

- **D1 The runtime.** a2g + jrt (recommended), or a hand-written runtime in Go forms.
  *Decided: a2g + jrt.*
- **D2 Evaluation.** An evaluator over `Compiler`'s `Expr` tree first, AOT later
  (recommended), or AOT to Go forms first (fast code, but no `eval` until an evaluator exists
  anyway). *Decided: the evaluator first.*
- **D4 Strings.** Java's UTF-16 `String` semantics in jrt (recommended: `count`, `subs`,
  `.charAt` and the suite behave as on the JVM), or Go's byte strings (simpler and faster, but
  Clojure's string functions change meaning). *Decided: Java's UTF-16 semantics.*
- **D5 Regex.** Go's `regexp` (RE2) with recorded differences from `java.util.regex`
  (recommended first: no backreferences or lookaround), or a port of `java.util.regex`.
  *Decided: a port of `java.util.regex`* (exact Java semantics; a larger part of jrt, step 3).
- **D6 Scope of the first REPL.** Leave out at first: `defclass` and the class forms (their
  compiler emits JVM classes), `gen-class`, `proxy`, JVM interop beyond jrt's classes, agents'
  executors beyond goroutines (recommended); each comes back as its Go-side meaning is defined.
  *Decided: left out at first.* **Partly reversed (2026-10-09, B1a step 5):** `proxy` is back,
  over the evaluator's `Dyn` (the user's decision; for `Object`, interfaces and a fixed list of
  non-leaf classes: A2G-SPEC §5.12, §10.4, amendments X1, X2), and the fork-join pool is real,
  goroutine workers in jrt, so reducers' `fold` and parallel streams run in parallel (A2G-SPEC
  §8.4, amendment S5). **Reversed for sockets (2026-10-10, the user's decision of 2026-10-09;
  amendments NT1-NT5):** `java.net`'s sockets and addresses are translated over jrt's
  `HostSocketImpl` and the host's optional `NetHost`, and `arbace.core.server` (the socket REPL,
  `prepl`, `io-prepl`, `remote-prepl`) is in the executable, its servers started from the
  `arbace.server.*` properties (from `JAVA_TOOL_OPTIONS`) as on the JVM (A2G-SPEC §4.1, §9.4,
  §10.6; JRT-NOTES.md, "Sockets (go-net)"). `gen-class` and JVM interop beyond the closed world
  stay out.
  **Reversed for `defclass` (2026-10-10, amendment CF3):** `defclass` and the code forms work at
  the REPL, analyzed by the embedded class forms compiler and interpreted
  ([CLASSFORMS-REPL.md](CLASSFORMS-REPL.md), A2G-SPEC §10.4).
  **Reversed for processes (2026-10-10, the user's decision of that day; amendments PR1-PR7):**
  jdk26u's `Process`, `ProcessBuilder` (with `Redirect`), `ProcessHandle`, `ProcessHandleImpl`,
  `ProcessImpl` and `ProcessEnvironment` are translated over jrt's natives and the host's optional
  `HostProc` (fork and exec, pipes, reaping, signals; `/proc` for the handles), `Runtime.exec`
  is in `Runtime`'s member table, and `arbace.java.shell` and `arbace.java.process` are in the
  executable (A2G-SPEC §4.1, §9.4; JRT-NOTES.md, "Processes").
  **Reversed for files, time, concurrency, security and text (2026-10-09 to 2026-10-10):** dates
  and `java.util.stream` (phase 2B), `Semaphore` (Y3); `java.io.File`, the file system and a
  small `java.nio.file` (FS1-FS7; FS6: files in the closed world); `java.time` translated with
  the zone data (TM1-TM9); `java.util.concurrent` translated (JC1-JC10); java.security's basics,
  digests, HMACs and `SecureRandom` (SC1-SC7; SC4 puts them in the closed world);
  `java.text`'s formats (TL1-TL8, the `gotext` branch); the suite's Java fixtures in a test
  build (FX1) (JRT-NOTES.md; A2G-SPEC §16).
  **Not to be done (the user, 2026-10-10):** XML (SAX, `arbace.xml`), `gen-class`, Java
  serialization: they stay out of the Go build for good, as do Swing/AWT and loading classes
  from jars.

(D3, how Arbace depends on go-whim, moves to B1b.)

## Risks

- **jrt's size.** Reflection-driven interop (`Reflector`) has no Go equivalent; Clojure code
  that calls Java methods by name works only on jrt's classes. Measure (step 1) before
  committing to scope.
- **Semantic drift between the JVM and Go builds.** Mitigated by differential testing for
  every step and by Clojure's suite.
- **Performance of an interpreter.** Joker's numbers are the baseline; closure compilation (step
  7b) is the remedy taken, AOT (step 7's second half) not scheduled (since done: standalone S6, the
  default), and a native back end is B2-B4 ([G2C-SURVEY.md](../G2C-SURVEY.md) §7).
- **The box later.** Keeping the OS behind jrt's host interface is what keeps B1b small.
