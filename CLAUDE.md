# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

Arbace is a Clojure derivative written entirely in symbolic expressions. Studying prior art and vendoring
parts of it is always allowed. The work is a derivation done once by hand, so it must stay
reproducible from the record we keep.

Direction:
- Two states of the art are frozen, each on a well-known branch advertised in the README, which
  takes fixes only (tags `-v2`, `-v3`...):
  - the JVM: `arbace-for-java-26` (tag `arbace-for-java-26-v1`, 2026-10-08), Arbace on Java 26,
    binary compatible with it;
  - Go: `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`, 2026-10-11), Arbace as one static
    executable for Linux, its runtime and the part of jdk26u it needs translated into Go.
- The box, Arbace built with TamaGo as a guest of its own monitor (vendored from go-whim, a pilot
  Arbace no longer depends on), is two milestones on main
  (not freezes: no branch): on amd64 with /dev/kvm (tag `arbace-box-amd64-v1`, 2026-10-12) and
  on arm64 (tag `arbace-box-arm64-v1`, 2026-10-12): KVM/arm64, checked in an emulated aarch64
  VM, and Hypervisor.framework on an M2 Max (the bundle of `bin/box-mac-bundle`, 42 of 42).
- The standalone Arbace (doc/STANDALONE-SURVEY.md, D1-D13 decided), S1-S9 done, two milestones
  on main (tags, not freezes): the Go-hosted bootstrap (S1-S4): the seed is Arbace's Go program
  as Arbace printed it, the Go executable rebuilds itself, and the JVM stays only as the oracle's
  reference (the frozen v4 jar) and repr/java's front end (`arbace-seed-is-go-v1`, 2026-10-13; since
  JN6, 2026-10-21, repr/java needs no JVM: javac on Arbace); the box
  rebuilding Arbace inside itself (S5-S8), its namespaces compiled to Go (S6), byte for byte on
  amd64, in the emulated arm64 VM and on the Mac (`arbace-inside-itself-v1`, 2026-10-15); then S9, Go's
  toolchain as forms (`tamago/cmd/`, 2026-10-15).
- goi, the interpreter of Go forms at the REPL, complete for Go 1.27.1 (GR1-GR10), a milestone on
  main (tag `arbace-interprets-go1.27.1-v1`, 2026-10-20): `$GOROOT/test`'s programs against gc,
  1,471 of 1,837 pass at the tag (1,664 in `test/goi/check-linux-amd64.edn` since, 2026-11-04), none fail, the rest in the recorded classes of `bin/goi divergences`
  (doc/go/GOI-SPEC.md, "v1's divergences"); the same in the box on amd64 and in the emulated arm64
  VM. Then REAP, read, eval, AOT and print for Clojure, Java and Go, a milestone on main (tag
  `arbace-reaps-3-crops-v1`, 2026-10-27): the matrix of `bin/reap`, 12 of 12 cells checked, none failing,
  every non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v1's divergences"). Then its second round, a
  milestone on main (tag `arbace-reaps-3-crops-v2`, 2026-11-01): the same matrix, 12 of 12 cells checked, none failing,
  every non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v2's divergences"), with the efficient gate
  (doc/GATE-SURVEY.md, GT1-GT7), its clean run in a fresh clone recorded in
  `test/gate/clean-arbace-reaps-3-crops-v2.edn`. Then REAP for four crops, C joining (doc/C23-SURVEY.md, C8, C9), a
  milestone on main (tag `arbace-reaps-4-crops-v1`, 2026-11-07): the matrix's 16 of 16 cells checked, none
  failing, every non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v3's divergences"), on the seed that
  embeds C (`d5548e72`, verified 2026-11-05) and an audited clean run of the four crops over GT8's compressed corpora
  within its budget (doc/GATE-SURVEY.md, GT8), recorded in `test/gate/clean-arbace-reaps-4-crops-v1.edn`. Parked (the user,
  2026-10-26): the box, the macOS UI, the iOS app (repr/haskell and repr/rust parked then too, unparked by the user
  2026-11-03: crops in progress again, as OCaml, Scheme and C++; Python surveyed). Now: doc/AGENDA.md.
- Long term: Arbace is a standalone ecosystem, not binary compatible with Java. Java stays
  usable at the source level through repr/java. The language is Clojure extended with an idiomatic
  representation of Java (the Java forms), and later of Go: the Arbace language. The goal is a
  self-sustaining REPL inside a virtual sandbox, written in Arbace all the way down to the bare
  metal ISA, in the way of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host),
  and Hypervisor.framework on arm64 (macOS host).

Prior art:
- https://github.com/clojure/clojure covers the language.
- https://github.com/openjdk/jdk26u covers the JVM runtime, the near-term target.
- https://github.com/golang/go is the model for a slimmer runtime of Arbace's own, and later a
  language to project into Arbace, as Java was.
- https://github.com/usbarmory/tamago: Go on bare metal, the model for running in a sandbox
  without an OS.
- https://github.com/candid82/joker: a Clojure dialect implemented in Go.

## Layout

- `seed/arbace-seed-go.tar.xz` is the seed of the bootstrap: Arbace's Go program as the Go-hosted
  fixpoint printed it (j2g's translation printed by repr/go, the module `go build` compiles, with Go's
  runtime as `overlay/go` patches it; since S6 the program with its namespaces compiled to Go,
  doc/STANDALONE-NOTES.md, "S6: the compiled build as the default"), one deterministic tar.xz pinned by
  `seed/arbace-seed-go.tar.xz.sha256`; the Go toolchain that builds it (TamaGo's go1.27.1) pinned
  by `seed/goroot.sha256` (doc/STANDALONE-NOTES.md, "S4: the Go-source seed", "S4: the JVM
  removed"). `bin/seed --check` checks the hash; `--build` builds stage 0 from it with Go's
  toolchain alone (`target/seed/<arch>/arbace`, and when its runtime is the checkout's the checkout's namespaces compiled by it into `target/seed/aot`, `--no-aot` not); `--make` packs a build into the seed and its pin (the last `bin/arbace-go --build` by default; a seed to commit
  is the fixpoint's G3, `ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3`, rarely: a re-seed); `--verify` prints it again from its commit (the fixpoint from
  the seed's own stage 0) and requires the same tar and stage 0 = G1 = G2 = G3. Until S4 the seed
  was a jar (`arbace-for-java-26-v1`'s), and before that the frozen reference tree `clojure/`
  (upstream Clojure and ASM): both on the branch `arbace-for-java-26`. The tools take what they
  need of that tree as data (repr/java's corpus, the baseline of Clojure's suite) from the tag,
  extracted into `.tmp/frozen/` by `bin/lib/tools.bash`.
- Stage 0 hosts Arbace's tools (`bin/lib/host.bash`'s `arbace_host_exe`: `ARBACE_HOST_EXE`, else
  the checkout's own stage 0, `target/seed/<arch>/arbace`, built when missing or when the
  seed changed by `bin/seed --build`, which also compiles the checkout's namespaces into `target/seed/aot` (RPS95 (b),
  about 3 minutes beyond the Go build, under no heavy lock (a per-checkout `.tmp/seed-build.lock` only keeps two tools from building at once); in a linked worktree its own when `bin/seed --kept` keeps it,
  else the main checkout's hard-linked into it when its pins line is the worktree's (`bin/seed --link`), else a refusal:
  `ARBACE_SEED_BUILD=1` builds, GTS82 (a), review 001's fixes; in a worktree set `ARBACE_HOST_EXE` to the main checkout's stage 0
  to skip all of it; `bin/gate` unsets it and uses the checkout's stage 0, built `--no-aot`, and `gate_key` hashes the
  bytes of `ARBACE_HOST_EXE` when set, so a `bin/arbace-go --gate-build` by hand under it misses the gate's builds;
  `ARBACE_SEED_GO` names another seed for the host as for `bin/seed`): c2j, j2g, repr/go, repr/clojure, the suite's runner,
  compiled into it (S6, SA133), a namespace of a tool or of goi (`arbace/go/`;
  RT.toolSource) whose source in the checkout differs from the embedded copy loaded from the checkout instead, evaluated (`bin/lib/tools.bash`'s
  `arbace_tool`, `ARBACE_PATH`). The JVM (Java 26 explicitly, the JDK of the jdk26u checkout's own build, `${JDK:-/root/jdk26u}/build/linux-x86_64-server-release/images/jdk`, or `ARBACE_FROZEN_JAVA_HOME`, never the system default: `bin/lib/java26.bash`) runs only the frozen jar of `arbace-for-java-26-v4` (built from its tag,
  pinned by `test/oracle/frozen-jar.sha256`; `bin/lib/frozen.bash`, its tools' namespaces from the
  checkout as `.clj` links before the jar, repr/java's not among them since JN6): the oracle's
  reference, the back end of `bin/repr-java check --suite` (the converted baseline compiled by the
  jar's own compiler, Clojure's suite run on it), the edges' recordings (`bin/j2g-edge`,
  `bin/javac-vendor edge`) and the data generators (`bin/jrt testdata|manifest|standins`,
  `jrt-coverage`); a JDK is the reference javac `bin/javac-vendor classes` compares javac on Arbace
  with. Nothing of repr/java starts a JVM (JN6, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built:
  JN6"), and since JN7 ("As built: JN7") nothing of `bin/jrt-convert` either: the JDK build's
  tools of its generate step run as Java forms on javac's tool
  executable (jdk/javac's `buildtools`, with java.xml's SAX parser), and its edge step
  (`jrt.jrt-edge`) on the tools' host, reading class files with `jrt.classfile`, Arbace's own reader;
  since JFS48 (b) ("As built: JFS48 (b)") jdk.compiler's generated sources are generated likewise,
  by the build's langtools tools on the tool (`bin/javac-vendor convert`), since JFS52 (b) ("As built:
  JFS52 (b)") `FlagsEnum` too, flagsgenerator compiling `Flags.java` through javax.tools' API on javac
  on Arbace (`jdk.internal.jrt.javac.SystemCompiler`): no generated source is copied.
- `bin/clojure-tests` runs Clojure's upstream test suite against a Clojure, by default the
  frozen baseline on the JVM (`test/baseline-results.edn`), or with `CLOJURE_TESTS_GO` the Go
  build's test executable (`test/arbace-go-results.edn`; repr/clojure's translation and the report on the
  tools' host); `--aot` (REAP3, doc/FULL-SUPPORT-SURVEY.md, "As built: REAP3"): Clojure's AOT level, the
  suite and its libraries compiled by c2j with the runtime's namespaces on the gate's test build, j2g and
  `bin/build-go` making their program with the Java fixtures (`.tmp/clojure-tests/aot/aot-build`, cached
  by key), each namespace loaded through its compiled loader (`test/suite_aot_hook.ae`), against
  `test/arbace-go-aot-results.edn` (classes `test/clojure/divergences-aot.edn`), the two levels'
  disagreements listed (`bin/gate --full`'s `suite-aot`).
- `bin/reap` (and `make reap`; REAP1, doc/FULL-SUPPORT-SURVEY.md, RPS3, RPS13): the REAP matrix,
  read, eval, AOT and print for Clojure, Java, Go, C, C++, Scheme, OCaml and Python (eight rows, 32 cells since merge plan batch 1,
  2026-11-07, the rows' branches merged: four rows, 16 cells since C8, doc/C23-SURVEY.md §10; C++'s since CX10,
  doc/cpp/CPP-SURVEY.md, "As built: CX10 (write-ahead)", "As built: cppi-check": C++'s read the token check's `test/repr/cpp/pp-check-known.edn` with its results
  `pp-check-results.edn` (`:c-known`'s `:id-sep "/"`) and CX9's dg record, eval and AOT CX8's `test/cpp/check{,-aot}-linux-amd64.edn` and the sampling's
  `check{,-aot}-every-25-linux-amd64.edn` (cppi-check; the lowering's `test/cpp/lower-results.edn` beside, reader `:cpp-lower`), print the round trip's `test/repr/cpp/roundtrip-results/`
  and known file (reader `:cpp-roundtrip`), classes `test/cpp/divergences{,-read,-aot,-print,-dg,-lower}.edn`), judged from the
  checks' recorded results alone (no runs): the cells and their sources in `test/reap/cells.edn`, the judging in
  `test/reap/main.ae`, each level's classes in `test/goi/divergences.edn`'s format
  (`test/clojure/divergences{,-read,-aot,-print}.edn`, `test/repr/go/divergences-{read,print}.edn`,
  goi's, `test/java/divergences-diffcheck.edn`; C's: `test/c/divergences{,-aot,-dg,-read,-annot,-print}.edn`, over the
  records `test/c/check{-linux-amd64,-aot-linux-amd64,-dg-linux-amd64}.edn` and the known files of `test/repr/c/`, the reader
  `:c-known`; Go's read numbers `test/repr/go/native-check-results.edn`,
  `bin/reap record-native-check`); the C known files' results files (`test/repr/c/{pp-check,parse-check,annot,roundtrip}-results.edn`,
  CS240 (a)), written by the full runs' `--record` since C9.2's window (2026-11-02), give C's read and print cells their passes; a cell whose check
  is not recorded yet prints "no check yet".
  Scheme's row since SM9 (doc/scheme/SCHEME-SURVEY.md, "As built: SM9 (write-ahead)"): its records one map a case
  (`test/scheme/check{,-aot,-r7rs,-srfi,-chibi-tests,-ecraven,-mats}-linux-amd64.edn`, the reader `:scheme`, the class an item records
  held to the matrix's), repr/scheme's read and print records `test/scheme/{check-read,roundtrip}-results.edn` (`bin/repr-scheme check
  --record`, `roundtrip --record`, whole runs), classes `test/scheme/divergences{,-aot,-corpora,-read,-print}.edn`; the mats and Chibi's
  own (chibi test) beside; the row counts as many rows as `cells.edn` has.
  OCaml's row since O8 (doc/ocaml/OCAML-SURVEY.md, "As built: O8 (write-ahead)"): the reader `:ocaml` (rows
  `[ID RESULT REASON ...]`, RESULT a string), eval `test/ocaml/check-oi.edn` and `check-suite-oi.edn` (oi against ocamlc's recorded
  runs), AOT `check-aot.edn` and `check-suite-aot.edn` (the light level, OC87 (a)), read and print `test/ocaml/{read,print}-results.edn`
  (repr/ocaml's round trip over the 16 corpora against ocamlc's own parser, `bin/repr-ocaml roundtrip --record-results`, a corpus's rows
  replaced, from a passing run), classes `test/ocaml/divergences{,-suite,-aot,-suite-aot,-read,-print}.edn` (`:corpus` by design,
  `:light` deferred).
  Python's row since PY10 (doc/python/PYTHON-SURVEY.md §10.1, "As built: PY10"; the Mac VM's line): read repr/python's `:lines`
  round trip and the native reader's token and tree checks (`test/python/{roundtrip,lex-check,parse-check}-known.edn` with their
  results files), eval `bin/pyi check` over Lib/test (`test/python/check-lib-test-linux-amd64.edn`, against CPython 3.14.10's
  `ref-cpython314-lib-test*.edn`, reader `:results` with `:py-ref`) and pyi's compiled loop, AOT `bin/pyi check --aot`
  (`check-aot-linux-amd64.edn`, reader `:pyi-aot`), print the `:full` round trip; eval and AOT "no check yet" until Python's amd64
  records night writes them.
  `bin/clojure-tests --divergences [RESULTS]` judges a suite run per test against the Clojure classes.
- `bin/crops-score` (and `make crops-score`; doc/CROPS-SCORE.md): each of the eleven crops' completeness (the REAP cells'
  pass / (pass + by design + deferred + pending) from a tree's `test/reap/matrix.edn`), maturity (a checklist measured on the
  tree: cells recorded, none unclassed, negative controls, gate steps, a REAP row, records without a host path) and
  independence (per stage, read, eval, AOT, print and judge, the reviewed facts of `test/crops/independence.edn`
  cross-checked with the tree's gate steps' toolchain hosts), from the records alone: the checkout, `--ref REF` (git show)
  or `--newest` (each crop's newest held branch on the remote mac); `crops.score` (`test/crops/score.ae`), its crops
  `test/crops/crops.edn`; the scores of the checkout recorded in `test/crops/scores.edn` (`--record`; any difference fails),
  completeness also without by design (CSP4); the essential step `crops-score` (`--judge --strict`, beside `reap`; CSP1 (b),
  the user, 2026-11-06).
- `c/`: Arbace's own C target, x86_64-linux-musl (c/README.md; doc/C23-SURVEY.md, "As built: C's own target"): `c/include/`
  (Arbace's headers, clean-room from C23 and the AMD64 ABI) and `c/target/` (the description repr/c reads: predefined macros
  and `__has_` answers made by `test/c/target.ae` from `abi.edn` and `dialect.edn`, `bin/repr-c target gen|verify|oracle`),
  with musl 1.2.6's headers from its tarball, assembled into a sysroot by `bin/lib/repr-c/sysroot.bash` (`ARBACE_C_SYSROOT`,
  read by repr/c's preprocessor and tools/ccdump); the default (`ARBACE_C_TARGET=gcc`: gcc's recorded `test/c/host/` and the
  host's headers, the oracle tier's). C's checks judge each test by its own verdict (the user, 2026-11-07; `test/c/selfref.ae`,
  `bin/ci selfref`; `ARBACE_C_JUDGE=gcc`: gcc's runs and records, by hand); no C step runs gcc.
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.ae` (Arbace's source
  extension: Clojure plus forms Clojure cannot evaluate; the loaders take `.ae`, then `.clj`, then `.cljc`):
  - the vendored Clojure, renamed from `clojure.*` (`arbace.core`, `arbace.lang` and the rest;
    the Java parts are Java forms). It is hand-maintained source; its derivation is recorded in
    `doc/VENDOR-NOTES.md` (replayable on `arbace-for-java-26`). Since S4 it has no bytecode back
    end, no ASM and no class loader (on `arbace-for-java-26`, with the JVM's three-stage
    bootstrap, the jar, its AOT cache, the jlink image and `bin/arbace-j`): the Go build
    translates it. The Go build's variants of `RT`, `Main` and `Compiler` are merged into their
    bases (SA150); `arbace/lang/go/` keeps the generated ones (`CompilerNodes.ae`,
    `CompilerAOTFn.ae`, regenerated by `test/c2j/gen_nodes.ae`, `test/c2j/gen_fns.ae`;
    `CompilerNodes.ae` holds the evaluator's half of the node kinds `arbace.c2j.emit` defines with
    `defnode`, `arbace.c2j.node`, Futamura F7). The divergences this makes
    (gen-class a no-op, no class files, ...) are recorded in doc/VENDOR-NOTES.md and
    doc/STANDALONE-NOTES.md (D13).
  - `arbace/classes/`: the Java forms' front end (spec `doc/classes/SPEC.md`): parse, lower,
    analyze, env, types, native, the access flags (`opcodes`), `go`, the interpreter (`interp`);
    `jvm`, env's class-file reading, only for the frozen jar.
  - `arbace/c2j/`: c2j, the back end compiling namespaces to Java forms (`arbace.c2j.*`;
    standalone S6, doc/STANDALONE-NOTES.md, "S6: Arbace compiled to Go"): the analyzed units of
    a program's prepare run, `Compiler`'s `Expr` trees, become classes j2g translates like the
    runtime's (the compiled pipeline, AOT: c2j, j2g, repr/go's print, Go's compiler).
  - `arbace/repr/`: the bridges between a language's source text and its forms, each one
    namespace with its parts below it and one command with the verbs `read` and `print`:
    `arbace.repr.java` (`arbace/repr/java{.ae,/}`), Java → Java forms (`bin/repr-java read`,
    `check`, `check --jdk`: jdk26u's 68 modules) on javac's tool executable (javac on Arbace, the
    reader compiled into the tool; JN4-JN6, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN4",
    "As built: JN5", "As built: JN6"; its only host since JN6, `REPR_JAVA_EXE`, the tool's key
    beside it, `arbace-javac.key`, `bin/lib/javac-key.bash`, named by the provenance files it writes,
    `bin/lib/repr-java/host.bash`; the frozen JVM's read, `REPR_JAVA_HOST=jvm`, and `host-check`
    left with JN6); `bin/repr-java javac`: javac's command line on javac on Arbace; `bin/repr-java
    declarations`: the runtime's declarations, javac's class path for the suite's fixtures; since JN8
    the native parser, `arbace.repr.java.parse` with its lexer `arbace.repr.java.lex`: Java 26's lexer
    and parser in Arbace, ports of javac's, giving javac's trees as data (tags, positions with javac's
    start and end, stored end positions, doc comments, every field), `bin/repr-java parse` (on the
    tools' host, evaluated from the checkout; `--javac`: javac on Arbace's tree, `arbace.repr.java.jtree`,
    on the tool; `--time`: the speed); `bin/repr-java parse-check`: the tree level, every file of the
    samples, the baseline, the jdk corpus's and javac's sources and jdk26u's 68 modules, 8,000
    mutants (javac's error recovery) and jdk26u's tests parsed by both, the trees equal, against `test/repr/java/parse-check-known.edn`;
    doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN8"); since JFS66 (b) the converter reads the tree through
    `arbace.repr.java.tree` (javac's JCTrees or the native trees as data) and asks javac's attribution through
    `arbace.repr.java.attr` (for the native trees by position: each native node paired with javac's attributed
    node of its tag and position), `read --front javac|native|both` (`--split`: the attribution facts asked and
    what each output list needed, `arbace.repr.java.split`); `bin/repr-java front-check`: the samples, the
    baseline, the jdk corpus and javac's sources (`--corpus all`: jdk26u's 68 modules) converted from both
    trees, the forms equal byte for byte, the split summed ("As built: JFS66 (b)"); since JN9a a native
    attribution provider behind `attr` for the scope and declarations tiers, `arbace.repr.java.nattr` over
    `arbace.repr.java.decls` (classes, members, flags, declared types, names, annotation values: Enter,
    TypeEnter, MemberEnter, Resolve's lookups, ConstFold, Annotate; the platform's class files read by
    `arbace.repr.java.classfile`, no javac) and `arbace.repr.java.scope` (locals, pattern bindings, jump
    targets), `read --front native|both --attr native`, each native answer checked against javac's;
    `bin/repr-java attr-check` (front-check `--attr native`): the facts equal fact by fact and the forms
    still javac's trees', against `test/repr/java/attr-check-known.edn` ("As built: JN9a"); since JN9b the
    members and types tiers too (`arbace.repr.java.expr`, `arbace.repr.java.types`: names in code, every
    expression's type; the known file's `:bridges`, "As built: JN9b"); since JN9c the overload tier
    (`arbace.repr.java.overload`: Resolve's three phases and most specific, every call that needs no
    inference; `attr.md` sums the declines by reason, "As built: JN9c"); since JN9d the inference tier
    (`arbace.repr.java.infer`: Infer's inference with InferenceContext, DeferredAttr's and ArgumentAttr's
    poly arguments, lambdas, method references, diamonds, signature polymorphic calls; the switch facts;
    the symbols' type in `arbace.repr.java.sym`; `REPR_JAVA_INFER_TRACE` traces it, "As built: JN9d"); `bin/repr-java
    check --jdk` compares jdk26u's forms with their hashes, `test/repr/java/expected/jdk26u.sha256`
    (`--update` rewrites them, JFS72 (b)), and Java forms → Java (`print`, `arbace.repr.java.print`; the
    `.ae` writer is `arbace.repr.java.layout`) on the tools' host, with its round trip Java →
    forms → Java → forms at the levels forms, ae (the converted text) and classes (javac's, byte
    for byte; `bin/repr-java roundtrip`, corpora baseline, samples and jdk, references in
    `test/repr/java/roundtrip/`; doc/classes/PRINTER-NOTES.md; mode `:full`, `read --full`,
    keeps the Java text, printed back byte for byte: `roundtrip --text`); `arbace.repr.go`
    (`arbace/repr/go{.ae,/}`), Go as Go forms and back (spec `doc/go/SPEC.md`; mode `:full` keeps
    the text: comments, spellings, layout, printed back byte for byte; helpers `tools/godump`, `tools/gocmp`;
    `bin/repr-go read`, `print`, `roundtrip`, `check`, `test`; `read` reads natively since GP5 (GPS7; doc/go/GO-PARSER-SURVEY.md): Go's go/parser and the dumper, `go/arbace/repr/go/godump` (generated from go/ast's forms, committed), GP1, compiled since GP7 (go/parser, go/scanner, go/ast, go/token and the dumper listed in goi's table; on an executable whose table does not list them, the tools' host until the re-seed that carries GP7, run by goi from the digest and the checkout), the loader `arbace.repr.go.load` with go list's part in `arbace.box.plan`, GP2, raw forms (the decisions that need types left open, GP3; `read --reader native --raw`), goi's checker in its resolve mode answering them as go/types does, its answers written into the native dumps as go/types' annotations by `arbace.repr.go.annotate`, GP4, converted as the helper's dumps are: no helper and no go command; `read --reader helper` (or `REPR_GO_READER=helper`): the helper `tools/godump`, the oracle; `read` writes `OUT.tsv`, the summary in the helper's format, the vendoring tools' and goi's; `read --reader native --trees`: the native dumps alone; `native-check`: each dump read once, E-tree, the raw forms resolved by the final ones, E-ann, E-forms (the `.ae` text of both readers byte for byte in modes `:lines` and `:full`); `native-check --headers`: the loader's headers alone; `syntax-errors`: syntax errors in gc's words, gc's own parser `cmd/compile/internal/syntax` (its renamed copy `go/arbace/repr/go/internal/syntax`, from `tamago/cmd`'s forms) by the driver `go/arbace/repr/go/gcsyntax`, listed in goi's table since GP7, `arbace.repr.go.syntax`, GP6, also the native read's message for a file go/parser rejects; since GPS41 (c) the driver ports gc's noder's pragma handling, its errors among the parse's, and `--decls` adds the declarations' checks of noder's writer; a listed driver of another `Revision` than the checkout's is replaced by the checkout's, interpreted), on the tools' host
    (`REPR_GO_EXE` names another executable); `arbace.repr.haskell` (`arbace/repr/haskell{.ae,/}`),
    Haskell as Haskell forms and back (spec `doc/haskell/SPEC.md`, notes
    `doc/haskell/HASKELL-NOTES.md`; helper `tools/hsdump` on the host's GHC, pinned by
    `tools/hsdump/GHC_VERSION`; `bin/repr-haskell read`, `print`, `roundtrip` (`--iface`: GHC's
    interfaces too; `--text`: mode `:full` printed back to the original text, byte for byte;
    `--layout`: printed by Haskell's layout rule, as a person writes it), `test`; corpus GHC
    9.14.1's boot libraries, `test/haskell/corpus-ghc-9.14.1.edn`), evaluated from the checkout,
    not embedded; `arbace.repr.rust` (`arbace/repr/rust{.ae,/}`), Rust as Rust forms and back
    (spec `doc/rust/SPEC.md`, notes `doc/rust/RUST-NOTES.md`; helper `tools/rsdump`, a
    `rustc_private` driver on the host's rustc, pinned by `tools/rsdump/RUSTC_VERSION`, built
    against `rustc-dev` fetched as an apk into `.tmp/` and pinned by `tools/rsdump/apks.sha256`;
    `bin/repr-rust read`, `print`, `roundtrip` (rustc's trees and lines; `--iface`: rustdoc's JSON
    of the crates, without spans; `--text`: mode `:full` printed back to the original text, byte
    for byte; `--pretty`: the forms without positions in the pretty layout, their trees), `test`;
    corpus std's five crates of `rust-src` 1.99.1, `test/rust/corpus-rustc-1.99.1.edn`, and since
    RR4 the rest of `library/`, the compiler and a pinned crate set (`-library`, `-compiler`,
    `corpus-crates-2026-10-18.edn`, its `.crate` files pinned by `test/rust/crates-2026-10-18.sha256`;
    `roundtrip ... all`), and since RR5 rustc's UI suite (`corpus-rustc-1.99.1-ui.edn`, from
    `rustc-1.99.1-src.tar.xz` pinned by SHA-256); `--run`: the behaviour level, the UI suite's
    run-pass programs built and run from the originals and from the printed files; `--std`: std
    built from its printed files, the same programs run against it; since RR6 `--code`: the
    lowered and code levels, the HIR, MIR and objects of the library corpora's crates and of the UI
    suite's programs built by the helper as rustc from the originals and from the printed files;
    `include!`'s expression files as `(rs/expr e)` forms), evaluated from the checkout,
    not embedded; `arbace/repr/c/` (`arbace.repr.c.*`: `lex`, `pp`, `host`, `parse`, `tree` and the checks), C's bridge
    (doc/C23-SURVEY.md, CS1-CS63; spec doc/c/SPEC.md): ports of cc's lexer, preprocessor and parser, checked
    token for token against cc run by the helper `tools/ccdump` over `test/c/corpora.edn` and against gcc
    (`bin/repr-c check`, `parse-check`; the parse level's reference is gcc's acceptance, CS61; the Makefile's
    `repr-c-*`), embedded and compiled with the program since C9 (CS100 (b), doc/C23-SURVEY.md "As built: C9 (preparation)"), with ci's `arbace/c` and
    `arbace.lang.CInterp`, so in `gate_key` (the seed embeds them compiled since C9.2's re-seed, `1fe1063f`: a change to them reruns
    the whole `--full`, and stage 0 evaluates a changed one from the checkout until the next re-seed); `arbace/repr/c/` also holds `convert`, `print`, `layout` and
    `annot` (`bin/repr-c read`, `print`, `roundtrip`). ci, C's eval (doc/C23-SURVEY.md §6; `bin/ci`: `run`, `check`, `check --aot`, `dg`,
    `annotations`, `repl-session`, `c2j-check`, `cinterp-check`, `judge-test`; `arbace/c.ae` and `arbace/c/`: the checker, the builder,
    musl 1.2.6 as C forms, cf2g's Go; `go/arbace/{ci,cinterp,crt}`, its back end; `tools/cirun`, the light path; the Makefile's `ci-*`).
    C's essential subsets are gate steps (C8, CS19:
    `c-dg-essential`, `c-check-essential`, `c-aot-essential`, `c-c23` in the essential gate, and `c-c2j` (C9 preparation: `bin/ci c2j-check`,
    ci's and repr/c's namespaces compiled by c2j alone), `c-judge-test` in `--full`; the
    subsets `test/c/essential-{dg,check,aot}.txt`) and its row of `bin/reap` is judged by the `reap` step. C's full runs (`bin/ci check`,
    `check --aot`, `dg`, `annotations`, `bin/repr-c check --tokens`, `parse-check`, `roundtrip`) are heavy `--full` steps since 2026-11-02,
    which judge the records and never `--record`: the records are recorded by hand (`--record`), and `bin/reap` judges them.
  - The other crops, repr/ocaml (`bin/repr-ocaml`, `bin/oi`), repr/scheme (`bin/repr-scheme`, `bin/si`), repr/cpp and repr/llvm
    (`bin/repr-cpp`, `bin/repr-llvm`, `bin/cppi`), repr/python and pyi (`bin/repr-python`, `bin/pyi`; doc/python/PYTHON-SURVEY.md, the
    Mac VM's line, merged with batch 1): their namespaces (`arbace/repr/{ocaml,scheme,cpp,llvm,python}`, `arbace/scheme/`, `arbace/cpp/`,
    `arbace/python/`) are evaluated from the checkout, not embedded (`arbace/j2g/embed.ae`'s exclusions; pyi's embedding waits for the
    re-seed window W, PYS18 (a)), and out of every key: since the keys commit (PYS30 (a), the user 2026-11-04; batch 1, 2026-11-07)
    one list, `bin/lib/version.bash`'s `arbace_crop_paths` (the crops' namespaces, their Go forms `go/arbace/{oi,scm,scminterp,py,
    expat,pyexpat,hs,libm}` and pyi's lazy modules `go/arbace/{clib,pybz2,pylzma,pyzstd,pyssl,pysqlite3}`, OCaml's `jdk/ocaml/`, the
    crops' script libraries `bin/lib/{oi,repr-cpp,cppi,rsi,hsi,si,pyi,repr-*,crops}`), is left out by `gate_key`, `gate_rest_key`,
    `arbace_runtime_key`, `arbace_embedded_sources`, `arbace_javac_key` and `arbace_javac_rest_key`, and the audit's hosts take it
    (`test/gate/audit.ae`'s `crop-rules`; `test/gate/gate-key-crops-test.bash`): a crop's commit under it moves none of the keys.
    Since batch 1, C++'s, Scheme's and OCaml's steps are in the gate (GT9, doc/GATE-SURVEY.md, "As built: GT9 (write-ahead)",
    GTS210-GTS223, GTS240-GTS247): spliced into `test/gate/steps.edn` from their fragments `test/gate/crops/{cpp,scheme,ocaml}.edn`
    (`gate.crops splice`, `test/gate/crops.ae`; `gate.crops check`: every new crop's step declares `:mem-gib`, its essential checks
    at most 3 GiB, every check a negative control in `:controls` or a waiver), their hosts as data (`:host-ids` `:cxx :chez :chibi
    :ocaml`, beside `:gcc`; `bin/gate`'s `gate_host_id`), their pins `:crop-pins` (`bin/gate --pins`), their light `:full` chains
    beside the serial run (`:night-concurrent`, GTS216; `bin/gate --plan` names them), their budgets `:new-crops` and `:shares`
    (`bin/gate --costs`); the rotations take GT9's verbs (`--only-list`, `--cmi-only`, `--no-expect`; test/{ocaml,cpp}/subsets.edn).
    The fragments of later batches (llvm-rust, natives, haskell, cpp-native, python) wait in `test/gate/crops/`.
    C++'s since CX10 (doc/cpp/CPP-SURVEY.md, "As built: CX10 (write-ahead)"), on stage 0 with the host's g++
    and clang++ (the host `:cxx`; `bin/gate --pins` runs `bin/repr-cpp pins check` and `host check`, `:crop-pins :cpp`):
    essential `cpp-read-essential` (`bin/repr-cpp pp-check --essential`, `test/cpp/essential-read.txt`; `--only FILE`, CORPUS/NAME a line,
    `--record-results`), `cpp-print-essential` (`bin/repr-cpp roundtrip-corpus gxx-dg-essential --judge own` and `samples --judge own`: a recorded list made by its
    key, `test/cpp/essential-print.txt` held to `roundtrip-results/own/gxx-dg-essential.edn`; since cpp-90 judged by the round trip's own levels 1 and 5 on the
    native parser, no clang, `--judge clang` the oracle tier's; the run checks by each test's own verdict, `test/cpp/selfref.ae`, `CPPI_JUDGE=gxx` g++'s runs), `cpp-lower-essential` (`bin/cppi lower-essential`,
    `test/cpp/essential-lower.txt`), about 2 minutes warm; `--full` `cpp-selftest`, `cpp-tokens`, `cpp-roundtrip` and `cpp-lower` (`bin/cppi
    lower-corpus KEY`), a part a recorded list; `make cpp-essential`. Since cppi-check (2026-11-05; "As built: cppi-check") CX8's run checks too:
    essential `cpp-check-essential` (`bin/cppi check --only test/cpp/essential-check.txt`; cppi-check's `cpp-aot-essential`, 4 GiB, is not a step: GTS215's 3 GiB), `--full` `cpp-check` and
    `cpp-aot`, parts the recorded corpora samples and runtime (`test/cpp/check{,-aot}-linux-amd64.edn`) and the sampling `--sample 25` of gxx-dg-run
    (`test/cpp/check{,-aot}-every-25-linux-amd64.edn`: a `--sample N` run is held to and `--record`ed into its own record), against g++ 15.2's runs
    `test/cpp/ref-gxx15.edn` (reap's `:ref-corpora`); CX9's `cpp-dg` (`bin/repr-cpp dg`), `cpp-runtime` (the lowered runtime, a build step), `cpp-roundtrip-dg` (rotated) and `cpp-c2j` with GT9's fragment.
    Since cpp-indep (2026-11-07; doc/cpp/CPP-SURVEY.md, "As built: cpp-indep", CP240 (a), CP241 (b)) the run checks and the runtime's build
    read clang's IR from the packs `cpp-ir-samples` and `cpp-runtime-ir` (`test/cpp/records/`, mac/oracle-records-cpp2's tooling
    `test/oracle-records/`, `bin/oracle-records`; `bin/cppi record-ir` by hand, `CPPI_LIVE=1` live, no gcc unless `CPPI_GCC=1`), and the readers
    read libstdc++'s headers and gcc's include directory as `test/cpp/target.ae` makes them from the pinned gcc tarball and `cpp/target/`
    (`bin/repr-cpp target tree|answers|oracle`, with musl's headers from its tarball: `bin/lib/repr-cpp/sysroot.bash`, `REPR_CPP_HEADERS=arbace|libstdcxx|host`);
    the live parts wait in `test/gate/crops/cpp-oracle.edn` for the oracle tier.
    Scheme's since SM9: repr/scheme against Chez 10.3.0 run live (the host `:chez`) and si's light driver
    (`go/arbace/scm` built alone by TamaGo's go, not Arbace's executable) against Chez's and Chibi's recorded runs, the corpora fetched
    by pin into the main checkout's `.tmp/scheme/corpora` (a worktree's and the clean clone's too); `--corpus ID` (`bin/repr-scheme
    check`, `roundtrip`) and `--only LIST` (`bin/si check`, `ecraven`, `mats`) select parts, a partial run judged on what it ran.
    OCaml's since O8: repr/ocaml against the host's OCaml 5.5.0 (the host `:ocaml`: ocamlc, ocamlopt, compiler-libs,
    the oracle `tools/ocdump` built live) and oi's light driver (`go/arbace/oi` built alone by TamaGo's go) against ocamlc's and
    ocamlopt's recorded runs, the corpora fetched by pin into the checkout's `.tmp/repr-ocaml`; `--corpus ID` (`bin/repr-ocaml
    roundtrip`; `opam`, `all`) and `--only-list FILE` (`bin/oi check [--aot]`, `suite check [--aot]`) select parts.
  - `arbace/repr/clojure{.ae,/}`: `arbace.repr.clojure`, Clojure's bridge (`doc/REPR-CLOJURE.md`;
    `bin/repr-clojure read`, `print`, `roundtrip`, `check`): Clojure source into Arbace's, the
    names of the vendored Clojure, `.clj`/`.cljc` into `.ae`, a table of rules for the dialects'
    divergences, and back (`print`, each rule reversed; its two one-way rules documented: a
    `.cljc`'s conditionals resolved, `SecurityManager`'s spelling; `read --full` records a
    `.cljc`'s conditionals in a comment block at the `.ae`'s end, printed back as the `.cljc`),
    Clojure → Arbace → Clojure byte for byte over the frozen `clojure/`, the suite and its
    libraries (`check --roundtrip`, `test/repr/clojure/roundtrip-known.edn`); `bin/clojure-tests`
    translates Clojure's suite with it.
  - `arbace/build/`: `arbace.build.go`, the build of a program held as Go forms: printed by
    repr/go into a module with its overlay, then `go build` (`bin/build-go`, `doc/go/BUILD.md`).
  - `arbace/go.ae`, `arbace/go/`: goi, the interpreter of Go forms (spec `doc/go/GOI-SPEC.md`):
    `arbace.go` (the table's namespaces, GR1; the macros of Go forms, GR3), the checker
    `arbace.go.check` (§3, GR2; `bin/goi annotations`, `test`), Go forms type-checked after
    go/types, the converter's annotations verified, and its resolve mode (§3.10, GP4: repr/go's raw
    forms, their open decisions answered by position, go/types' spellings kept); the builder `arbace.go.build`, the REPL's
    state `arbace.go.repl` and program runs `arbace.go.run` (GR3; `bin/goi run`, `check`, `test`; `divergences`, GR10: every program of the check that does not pass in a recorded class, `test/goi/divergences.edn`; its reference gc's build and run of the originals since REAP2 (RPS12); `check --aot`, REAP2, Go's AOT level: the same corpora's forms built by `arbace.build.go` (the lines layout, the action's flags: `--go-flag`) and run, directory tests as modules, errorcheck programs printed from mode `:full` and compiled by gc, against gc on the originals, `test/goi/check-aot-linux-amd64.edn`, its classes `test/goi/divergences-aot.edn`, `divergences --aot`; its corpora and the table's linux std read natively since GP5, the error tests' go/types errors from the helper, the oracle, GPS33; since GP6 the error tests judged on gc's syntax errors when one is a syntax error or go/parser rejects the file, and the invalid programs the converter cannot convert on their raw forms, `arbace.go.run --errorcheck --raw`; since GPS41 (c) every error test with gc's front end's errors beside the checker, `--gc-front`: gc's syntax's branch checks in place of goi's own, noder's pragmas', then its writer's declaration checks),
    all embedded (the table's generator `arbace.go.table` runs on the tools' host). Its back end is
    `go/arbace/goi` (hand-written Go forms; the operators' nodes and the factories' registry
    generated by `test/goi/gen_nodes.ae`); Go forms' API to Arbace is the package `arbace`
    (`go/arbace.ae`, listed in the table, GR5); repr/go's native reader's packages are go/'s too (`go/arbace/repr/`, listed, GP7); goi's cross-dialect oracle cases
    (`test/oracle/forms/go.ae`) are recorded by goi (`bin/oracle record --goi IMPL`); `bin/goi check --corpus nethttp`
    runs `test/nethttp/`'s programs (a module, a package main each) the same way.
  - `arbace/j2g/`: the Java forms → Go forms translator (spec `doc/go/J2G-SPEC.md`; `bin/j2g`),
    on the tools' host (`J2G_GO_EXE`), its classes translated in parallel (`J2G_JOBS`, default at
    most 16) with the sequential output byte for byte; the resources compressed by
    `arbace/j2g/zlib.ae` (SA21); `bin/j2g-check` (the oracle's class scripts translated, built for amd64 and arm64
    and compared with the JVM's recorded steps), `bin/j2g-evalproof` (the evaluator boundary's proof, doc/go/EVAL-PLAN.md) and
    `bin/net-check` (the socket server against the JVM's) are its older differential harnesses, in no gate step.
- The Go build (B1a, `doc/go/B1-PLAN.md`): Arbace as a static Go executable. j2g translates
  the runtime (with the generated Go-build variants in `arbace/lang/go/`), and jrt, the Java runtime on Go (`go/arbace/jrt`, hand-written Go forms;
  `overlay/jdk/`, its own Java and variants of JDK classes; `jdk/`, the JDK's Java forms and
  resource data committed (repr/java's output from jdk26u and `overlay/jdk/java.base`, provenance in
  `jdk/provenance.edn`; `bin/jrt-convert update` regenerates them, `verify` checks them, the vendoring tools reading
  without javac by default since JN9f (`bin/lib/repr-java/host.bash`'s `repr_java_vendor_read`; `REPR_JAVA_VENDOR_READ=javac`
  the old read; `bin/repr-java read --no-javac`); standalone S1); `jdk/edge.edn`, the JVM's answers j2g needs, so j2g uses no JVM reflection or ASM
  (`bin/j2g-edge update`, `verify`, `trace`; S2, doc/STANDALONE-NOTES.md); `bin/jrt`); `overlay/go/` patches Go's runtime.
  `jdk/javac/`: javac's Java forms (jdk26u's jdk.compiler with its generated sources,
  java.compiler, `CommandLine` and the java.base classes javac's world needs, unpatched; jrt's
  own Java and j2g variants for that world in `overlay/jdk/javac/`; since JN7 also the JDK build's
  tools `bin/jrt-convert`'s generate step runs on the tool, `make/jdk/src/classes/build/tools` as
  module `buildtools`, with java.xml's SAX parser and the JDK classes they need beyond the
  program's, `overlay/jdk/javac/closure.txt`, and jrt's zlib ports (`Deflater` since JN7, in the shipped world since JC6b with `Inflater`); since JFS48 (b) the
  langtools tools `make/langtools/tools/{compileproperties,propertiesparser}`, which generate
  jdk.compiler's resource bundles and `CompilerProperties`/`LauncherProperties` on the tool in
  `convert`'s generate step, compared with the build tree's, and since JFS52 (b) `flagsgenerator`,
  `FlagsEnum`, through javax.tools' API: `ToolProvider`'s tool-world variant returns javac on
  Arbace's `JavaCompiler`, `SystemCompiler`, JavacTool's tasks over its file manager as a standard
  one, against the platform), its source list, provenance
  and edge (`jdk/javac/edge.edn`: what javac's world asks beyond `jdk/edge.edn`), made by
  `bin/javac-vendor convert`: since JN5 read by javac on Arbace (the fixpoint: javac's tool reads
  javac's own sources into these forms; javac's read left to the cases `--no-javac` falls back on, JN9f), the edge recorded on the frozen JVM (`verify` byte for
  byte, `check`: j2g translates javac's world; doc/classes/JAVA-FRONTEND-SURVEY.md, JN2, JN5); the input of javac's tool build,
  `bin/arbace-go --build-java` (`target/arbace-go/<arch>/arbace-javac`, not shipped). javac runs on
  it (JN3) through its command line on Arbace, `jdk.internal.jrt.javac.Main` with an Arbace file
  manager (jrt's own Java in `overlay/jdk/javac/jdk.compiler`), against the platform: the JDK build
  tree's exploded modules with their jmods' module descriptors (since JN6 all 68 of the image, as
  the JVM's javac sees them; the provenance files' `:jdk` names the platform by its pin), pinned by
  `jdk/javac/platform.sha256` (`bin/javac-vendor platform-pin`); `bin/javac-vendor classes`
  compiles the baseline, the samples and java.base's closure (`-g:none`) by javac on Arbace and by
  the JVM's javac and compares the class files byte for byte (`java.base`: java.base as the JDK
  build compiles it, against the build's classes). repr/java's reader runs on the tool (JN4; the
  default since JN5): `--build-java` embeds `arbace/repr/java` and compiles it by c2j on a
  first tool build (`target/arbace-go/aot-reader`, kept by key; j2g's `--embed`); `read-jdk`'s
  `--patch-module` against the same platform. `bin/arbace-go` builds
  (`--build`, `--build-tests`, `--pgo`, by the tools' host; the test build's Java
  fixtures committed as forms in `test/java-fixtures/`, `bin/clojure-tests --java-fixtures update`,
  `verify`), smoke-tests and runs it; `--build-program DIR` (REAP7, doc/go/BUILD.md, "A user's
  program"; `make program DIR=...`) builds a user's program: `DIR/program.edn`'s entry (`:main NS`, its
  `-main`; `:main-class CLASS`, its `main`; neither: `go/` a Go program, `arbace.build.go` alone),
  `src/`'s namespaces compiled by c2j and embedded (its `.clj` and `.cljc` translated into `.ae` by
  repr/clojure first, REAP7b), `java/`'s Java forms and the compiled namespaces translated by j2g with
  the shipped build's compiled namespaces (the shared library `bin/lib/program.bash`, RPS97 (b); its `program_base` takes the first of `ARBACE_PROGRAM_BASE`, `target/arbace-go`, the gate's build at the checkout's key (`ARBACE_GATE_EXES/KEY20/amd64`, computed as the gate does by `bin/lib/gate-cache.bash`'s `arbace_gate_cache`; `ARBACE_GATE_CACHE` overrides) and stage 0's `target/seed`, which `bin/seed --build` makes, whose `aot.key` is the checkout's: `aot.runtime` beside it names their runtime; else stage 0's compiled again, else a
  `--build` first; j2g's `--entry`, `--entry-arg`, `--resources`, `--java-namespaces`: the packages'
  namespaces no-op loaders), `go/`'s Go packages compiled (a package goi's checker refuses is a build error, RPS102 (b); REAP7b: listed in a goi table of the
  program's own, `bin/goi table --program-list FILE --program-root DIR`, and printed into the module by
  `bin/build-go --root DIR`, a package outside the module `arbace` a module of its own under
  `_modules/`), into
  `target/arbace-go/programs/NAME/ARCH/NAME`, cached by key; `--build-program --check` (`make
  programs-check`) builds and runs the samples of `test/programs/` (Clojure, Java forms, Go forms,
  mixed) against their runs (`bin/gate` the `mixed` sample and `bad-go`, the one that must not build, `make programs-essential`; `--full` all). Its namespaces are compiled to Go (`ARBACE_AOT=1`, the
  default since S6; `ARBACE_AOT=0`: evaluated from the image of prepared namespaces, as before):
  by the host when its runtime (`bin/lib/version.bash`'s `arbace_runtime_key`: `arbace/lang`,
  `go`, `overlay/jdk`, `jdk` but j2g's edge, `jdk/javac/`, `jdk/provenance.edn`, `overlay/jdk/javac/` and `arbace_crop_paths`) is the checkout's, one j2g run, started as the host
  compiles them (SA160), about 2.5 to 3 minutes; else by a first build of the program, two runs, about
  5.5 minutes (after a change to the runtime). The test build reuses the shipped build's compiled namespaces when made from the same sources and
  runtime (`target/arbace-go/aot.key`); `bin/build-go` keeps a cache of printed packages
  (`--print-cache`, `.tmp/repr-go-print-cache`); `--fixpoint` builds G1 by the host, G2 by G1, G3 by G2 into
  `target/arbace-go/fixpoint/` and requires G2 = G3. The oracle (`bin/oracle`, `test/oracle/`,
  `doc/go/ORACLE.md`) checks it against the frozen JVM Arbace (the v4 jar's expected results,
  `test/oracle/expected`), against its known mismatches `test/oracle/known-go-amd64-frozen.edn`.
  Notes in `doc/go/`.
- The box (B1b, `doc/go/BOX-SURVEY.md`, `doc/go/BOX-NOTES.md`): the same program built with
  TamaGo (`GOOS=tamago`) as a guest of a monitor on /dev/kvm (amd64) and Hypervisor.framework
  (arm64; KVM/arm64 too). `box/` holds the hypervisor API, the monitor, the doorbell ABI and the
  TamaGo board, vendored as Go forms from go-whim (at `80c49bf`) and Arbace's own since, with
  their KVM tests on the box's own test guests (`arbace/box/guest`: a runtime-less guest in Go's
  assembly, a demo and a faulty Go guest; 2026-10-16, nothing of go-whim needed), and purego for
  the launcher on darwin/arm64 (`box/README.md` records the origins; `bin/box-vendor` checks
  them: `roundtrip`, `kvm-tests`, `purego-check`); import paths `arbace/box/*`. `tamago/` holds the guest's own Go as Go
  forms (S6): TamaGo's runtime and standard library (`tamago/goroot/`, GOROOT/src's layout) and
  its `goos` module (`tamago/goos/`), for tamago/amd64 and tamago/arm64 as one tree, converted by `bin/tamago-vendor`
  (repr/go's native reader, as every vendoring tool since GP5; the goos module's packages those that
  go build builds for the architecture, GPS32; `convert`, `verify`; `check` proves the guest image built from them byte-identical to one
  built from the originals); the box's guest is built from them printed (`ARBACE_BOX_GO=orig`:
  from the toolchain's tree). `tamago/cmd/` holds Go's toolchain as Go forms (S9): the 105
  packages of GOROOT/src/cmd that `compile`, `link` and `asm` build from, for linux/amd64,
  tamago/amd64 and tamago/arm64 as one tree, converted by `bin/toolchain-vendor` (`convert`,
  `verify`; `check` proves the nine tools built from them byte-identical to the originals'); the
  box's tool guests (`bin/box-tools`) are built from them printed (`BOX_TOOLS_GO=orig`: from the
  toolchain's tree). `bin/arbace-go --build --box` builds the
  guest image and Arbace's launcher `arbace-box` (`--smoke --box`, which runs `bin/box-smoke`;
  `bin/box-suite`; arm64 with `ARBACE_BOX_ARCHES`). `bin/box-arm64-vm` runs an emulated aarch64
  VM with KVM/arm64 for the arm64 box's checks (`check`: its smoke test, `box/`'s KVM tests);
  `bin/box-mac-bundle` packs the Mac's launcher, guest and checklist (`box/mac/`; `--convert`: the conversion alone, `./mac.sh convert`, its expected digests from `bin/box-convert --arch arm64 prepare host`). `bin/box-rebuild`
  rebuilds Arbace's guest inside the box (S8); `bin/box-convert` converts `tamago/goroot` inside it
  (GP8: repr/go's native reader in spawned guests, `arbace.box.convert`'s assembling and merging,
  also `bin/tamago-vendor`'s), byte for byte the committed forms (on amd64; in the emulated arm64
  VM a part of std, `--packages`, file for file against a whole run). The store (S3):
  blobs and refs in a host directory (`ARBACE_BOX_STORE`, `arbace-box --store DIR`, which is also
  its tool), the namespace `arbace.box` over it in the box and natively.
- `test/java/`, `bin/java-check`: Java's conformance check (doc/classes/JAVA-CONFORMANCE-SURVEY.md,
  JC1-JC8, "As built: JC1", "As built: JC2"), as goi's `bin/goi check` is Go's: jdk26u's jtreg tests read in place at the
  platform's pin (corpora by area and category, `test/java/corpora.edn`: `jdk/java/*` but AWT and beans,
  since JC6 ("As built: JC6") the rest of `jdk/` but its desktop parts and `hotspot/jtreg/runtime`,
  `langtools/tools/javac`, `hotspot/jtreg/compiler`'s plain, `@compile` and JIT-option runs) and Arbace's
  own JLS programs (`test/java/jls/`), each test's files put in a package of their own (`package tNNNNN; `
  before the first line), the JDK's runs recorded as data (`test/java/ref-jdk26.edn`, `--record-ref` with
  the system's JDK, `--only` keeping and checking every other line; the check needs no JVM), the levels `read` (repr/java on javac's tool executable), `nread` (JFS123 (b), outside the gate: the read without javac, `bin/repr-java read --no-javac`, its forms against `read`'s byte for byte, `test/java/check-nread.edn`, `make java-check-nread`),
  `interp` (each package required file by file as the REPL loads it, `--load do` the one-`do` mode, and
  `main` run on the executable, `test/java/run.ae`; since JCV1 (the user's decision of 2026-11-07, "As built: JCV1")
  a test `test/java/verdicts.edn` marks `:status` (`bin/java-check verdicts`: its source set can fail by itself, or it prints
  nothing) judged by its own verdict, jtreg's (status 0, no other thread's exception uncaught), the rest (`:stdout`) by
  status and stdout's digest, also with identity hash codes masked, against the JDK's), `compiled` (JC2, "As built: JC2": the tests translated by j2g in
  shards of programs, `bin/j2g --program` with the build's compiled namespaces (`program_base`'s, RPS161 (b); `JAVA_CHECK_AOT` overrides) and `bin/build-go`, a
  test j2g refuses or whose Go does not compile taken out, each `main` run on its program) and `classes`
  (javac on Arbace's class files against the JDK javac's; since JCV1 javac's acceptance, the digest the oracle tier's), results
  against `test/java/check-LEVEL.edn` (`--record`; the JDK's record's judgment, rows `LEVEL.jdk`, is the oracle tier's, by hand at a tag:
  `bin/java-check --judge --oracle [--latest-saved]` against `test/java/oracle/`),
  every test not passing in a class of `test/java/divergences.edn`; the namespace `java.check`
  (`test/java/check.ae`) on the tools' host; the essential subset's forms committed in `test/java/forms/`
  (`--forms verify|update`); `diagnostics` (JC3, "As built: JC3"): langtools' golden-diagnostics tests
  (compile-fail-ref, the corpus `langtools-diagnostics`), run in place by javac on Arbace as jtreg runs
  them, the output against the golden file line by line (`test/java/check-diagnostics.edn`); `print` (REAP5,
  doc/FULL-SUPPORT-SURVEY.md, "As built: REAP5"): each test's forms printed back to Java by repr/java's
  printer (`test/java/print.ae`), javac on Arbace accepting it, read again to the same forms, mode `:full`
  printed back byte for byte, its class files against the original's (`test/java/check-print.edn`).
- `doc/ALPINE.md`: the host's Alpine packages as one minimal `apk add` line (`bin/alpine-env
  world`, `check`, `simulate`), the subset Arbace uses, and what apk does not provide (jdk26u,
  TamaGo's Go, Go's module cache: only `bin/gate --full`'s vendoring checks need it). No build
  uses cgo (`CGO_ENABLED=0` everywhere, 2026-10-16).
- `Makefile`: a familiar interface for newcomers, nothing more (`make help`): each target runs a
  script of `bin/`, which holds the logic. Keep it in step with `bin/`: when a script's name or
  options change, or a script becomes an objective of the repo, update its target and `##` line;
  `make check-makefile` (the essential gate's first step) checks the scripts it runs exist.
- `bin/gate` is the check every change to main must pass. Since GT1-GT3 (doc/GATE-SURVEY.md,
  "As built: GT1-GT3") its steps are data, `test/gate/steps.edn` (each step's command, the steps it
  needs, its declared inputs: files, hosts, pins, environment), and after stage 0 the planner
  `test/gate/plan.ae` (on stage 0) keys each check over them, never the commit, and skips one that
  passed at its key in the store of passes (`ARBACE_GATE_STORE`, default the main checkout's
  `.tmp/gate-pass/`, shared by the worktrees: each pass with its commit, worktree, date, seconds,
  peak memory, log); the builds (`go-build`, `javac-tool`) run when a check that runs needs them;
  `bin/gate [--full] --plan` prints each step's decision and why, `bin/gate --why STEP` what changed
  since its latest pass, `--rerun` runs every check; since GT4-GT6 (doc/GATE-SURVEY.md, "As built: GT4, GT6")
  `bin/gate --audit [STEP...]` runs a check under strace and fails on a file of the checkout it reads
  that its declared inputs do not cover (the declarations are the audit's; `make gate-audit`), one skipped
  light check (the oldest pass) reruns each gate as the canary (`GATE_CANARY=0` not), `--prune` trims the
  store (the last 5 passes and 30 days per step) and the executables' directories but the newest 3, when older than a day, the planner fails on a table that names no file, step or
  host, a step over three times its latest passes is flagged, the `:heavy true` steps (measured peaks above
  8 GiB, `:mem-gib`) run each under `GATE_HEAVY_CMD` (default `bin/heavy-lock`: the host's big-job lock
  `ARBACE_HEAVY_LOCK`, by default the main checkout's `.tmp/big.lock`, taken in turn, a FIFO of tickets in
  front of the flock that plain `flock big.lock` users still exclude; GTS32 (a), `make gate-queue`) and the
  gate itself does not (a caller that holds the lock sets `GATE_HEAVY_CMD=env`, else the gate waits for it),
  the `:heavy :inner` ones not: their tools take the lock around their heavy process alone (since GT5,
  GTS31 (b), `bin/lib/heavy.bash`: `bin/j2g`, repr/go's native reader and converter, repr/java's corpus
  reads, under `ARBACE_HEAVY_CMD` unless `ARBACE_HEAVY_HELD`; outside the gate `ARBACE_HEAVY_CMD` is unset and the tools take
  no lock: run a heavy command by hand as `bin/heavy-lock CMD`, or with `ARBACE_HEAVY_CMD="$PWD/bin/heavy-lock"`, an absolute path, as the tools run it from their own directory), their light work beside the other steps;
  since GT5 (doc/GATE-SURVEY.md, "As built: GT5") the inner keys: a step's `:parts` are one check each
  (native-check and repr/java's tree checks per corpus, `NAME.CORPUS`; the parked sub-projects' corpora
  `:clean-only`, run with `GATE_ALL_PARTS=1`), a step's `:outputs` (`GATE_OUT`) are kept with its pass and put
  back when it is skipped, a step's `:uses` (`GATE_OUTS`) put its producers' outputs' digests in its key (an
  early cutoff, decided once they ran: the plan's `after`), Java's check is one step per level with its
  judgment apart (`bin/java-check --save DIR`, `--judge RESULTS...`: a classes file reruns the judgment
  alone), the program hosts `:prog` and `:javac-prog` know the gate's executables and javac's tool by the
  units of the program j2g made (`bin/lib/prog-units.bash`, `units.tsv` beside the build) less a step's
  `:never-loads` (`arbace.j2g`: a j2g change that leaves the translations alone reruns only j2g's steps), the
  builds all run before the checks and the plan is made again after them, `bin/gate --audit` records the
  namespaces a step loads (`ARBACE_LOADED_LOG`, RT.load) and fails on one of its `:never-loads`, its reducer
  `tools/gateaudit` (Go, GTS30 (a)), and every `--full` ends with `--prune` (GTS34 (a)); since GT7
  (doc/GATE-SURVEY.md, "As built: GT7 (tooling)") `bin/gate [--full] --clean [--tag TAG] [--only STEP,...|failed|changed]` (`make
  gate-full-clean`, `make gate-full-clean-only`: a selection of steps rerun and merged into the tag's record) is the clean run at a tag: a fresh `git clone --shared` of HEAD's commit in
  `.tmp/gate-clean/SHA` with its own empty `.tmp/`, `target/` and `GOCACHE`, stage 0, the executables and
  javac's tool built fresh, no pass read, serial, every step run under the audit, every part, Java's levels
  on the read's cache forced (`JAVA_CHECK_FORMS_ONLY=1`), the externals checked by their pins first (`bin/gate
  --pins`), the record `test/gate/clean-TAG.edn` written into the invoking checkout and the passes (marked
  `:clean`) and the builds published into the shared store and executables' directory (a pass there that is empty or
  does not read replaced, GTS80 (a); `record_pass` writes each file through a temporary name); `--clean --only
  STEP,...|failed|changed` reruns some steps into the record, a program host not built compared by its build's key
  (GTS79 (b)); since GTS81 (a) each check family has a negative control, a case's run judged against another case's
  reference by the family's own judge, that must fail (`test/java/controls.edn`, `test/goi/controls.txt`,
  `test/clojure/controls.edn`, `test/oracle/controls.edn`, `test/diffcheck/controls.edn`): one that passes fails the
  step (doc/GATE-SURVEY.md, "As built: GTS72, GTS79-GTS82 (write-ahead)"; since GTS84-GTS86 also Java's diagnostics and
  print levels and goi's errorcheck and directory tests, every control judged from its subject's own run: "As built:
  GTS83-GTS87 (write-ahead)"; since GTS91 (a) also the read level's forms at `bin/java-check --forms verify`); `--full
  --concurrent` runs the chains at once under the big lock held once, the heavy steps on a lock of the
  gate's own (GTS37 (a)); `bin/gate-demos` repeats GTS42 (a)'s two demonstrations as plans (`bin/gate
  --full --plan --builds`); the executables and javac's tool are kept by key in
  `ARBACE_GATE_EXES` (default the main checkout's `.tmp/gate-exe`), shared by the worktrees;
  the programs of goi, Java's interpreted level, the suite and the oracle run under CPU-time limits with
  a wall cap and a quiet retry (`bin/lib/limit.bash`); the runner's logic is `bin/lib/gate-run.bash`, in
  every key with the planner and `bin/gate`'s header and summary not; the essential step `provenance`
  (`bin/provenance-check`) compares the provenance files' reader hash with repr/java's; each step's wall and CPU seconds and peak RSS
  (busybox `time -v`) in `.tmp/gate/NAME.mem`, a 5 s memory sampler's `.tmp/gate/mem.tsv`. The
  steps: the Makefile's scripts (`make check-makefile`), the seed's hash, stage 0 built from
  it (`bin/seed --build --no-aot`, kept by the seed's and Go's pins and its scripts' digest, GTS53 (a); in a linked worktree
  without a current one of its own the main checkout's when its pins are the worktree's, `bin/seed --kept`, hard-linked into
  the worktree's `target/seed`, `bin/seed --link`, GTS83 (a)), the executable and the test build by stage 0 (`bin/arbace-go
  --gate-build`, cached by key in `ARBACE_GATE_EXES/KEY20` (`KEY20` the first 20 characters of `bin/arbace-go --gate-key`; the one function `bin/lib/gate-cache.bash`'s `arbace_gate_cache` computes it for the gate and for the scripts run by hand; `ARBACE_GATE_CACHE` overrides, and a miss replaces only a directory of its own making, built beside it and swapped in under a lock, so a second run waits and then hits), and keyed by a hash of what they are built from,
  `gate_key`: `arbace/` less the namespaces no executable embeds (`bin/lib/version.bash`'s `arbace_embedded_sources`:
  repr/java and `arbace_crop_paths`' namespaces), `go/` less the crops' Go forms (`arbace_crop_paths`'; ci's, cinterp's and crt's are in it since C9: j2g copies them into the program), `overlay/` and `jdk/` less
  javac's world, `jdk/platform/`, OCaml's `jdk/ocaml/` and the provenance files, `x509roots/`, the fixtures' forms,
  `bin/lib` (less the repr checks', C's `ci/`, `arbace_crop_paths`' libraries and `gate-run.bash`; queue 111: no build
  reads them), since GT7 `tamago/goroot` and `tools/godump` (goi's table), the build's scripts, the
  seed's and Go's pins, Go's `VERSION` and `ARBACE_HOST_EXE`'s bytes when set; seconds when unchanged, else two Go-hosted builds, about 8 minutes: the
  test build reuses the executable's compiled namespaces), then
  concurrently the smoke test, the per-form differential check (`bin/diffcheck`, Futamura F2, since
  2026-10-17 in the essential gate: the oracle's forms evaluated against compiled by c2j, the Java
  forms interpreted against translated by j2g, against `test/diffcheck/known.edn`; about 5
  minutes, 40 s cached; run by hand, `bin/diffcheck`, `bin/generated-check` without EXE and `bin/clojure-tests --aot` without
  `CLOJURE_TESTS_AOT_EXES` call `bin/arbace-go --gate-build` themselves and take its executables from that same
  `ARBACE_GATE_EXES/KEY20`, a hit after a gate run of the checkout's key, else two Go-hosted builds), Clojure's suite on the test build (`test/arbace-go-results.edn`),
  the oracle against the frozen reference, and goi's essential check (`bin/goi check
  --essential`: `$GOROOT/test`'s programs of `test/goi/essential.txt` interpreted against gc), Java's
  essential check (`bin/java-check --essential`: the jtreg tests of `test/java/essential.txt`
  interpreted from their committed forms against the JDK's recorded runs), and the generated variants'
  check (`bin/generated-check`, F7.3: `arbace/lang/go/CompilerNodes.ae` and `CompilerAOTFn.ae`, and
  since GP7 repr/go's generated Go forms, the dumper `go/arbace/repr/go/godump` and the renamed copy of gc's
  syntax `go/arbace/repr/go/internal/syntax`, regenerated and compared with the committed files), and since 2026-10-24 `bin/j2g-edge verify` on the
  frozen JVM (j2g's edge recorded afresh: an embedded namespace's new JDK class makes `jdk/edge.edn`
  stale), and `bin/reap` (REAP1: the REAP matrix judged from the checks' recorded results, seconds), and since REAP8 the new levels' essential subsets (doc/FULL-SUPPORT-SURVEY.md, "As built: REAP8"): `bin/goi check --aot --essential` (`test/goi/essential-aot.txt`), `bin/java-check --essential-compiled` (the essential tests that pass compiled, one program cached by key) and `bin/clojure-tests --aot --essential` (`test/clojure/essential-aot.txt`'s namespaces compiled alone, cached by key), and since REAP7b `bin/arbace-go --build-program --check mixed` (a user's program of the three languages, its Go package compiled through its own goi table, built on the gate's executable, cached by key, and run; `--full` all of `test/programs/`), and since SM9 Scheme's essential subsets, about 1.5 minutes in two chains (`si-essential`: `bin/si unit`, `test --si`, `check --only test/scheme/essential-check.txt` with the negative controls, `r7rs`, `srfi`; `si-aot-essential`: `check --aot --only test/scheme/essential-aot.txt`; `scheme-read-essential`: repr/scheme's read check and round trip over every corpus but whimsical, `spec-check`, `bin/si corpora files`; the hosts `:chez` and `:chibi`, Chez's and Chibi's identities, in `bin/lib/gate-run.bash`), and since O8 OCaml's essential subsets, about 1.5 minutes in two chains (`oi-essential`: `bin/oi test`, `repl-check`, `check --only-list test/ocaml/essential-check.txt`, OC67 (a), with the negative control; `oi-divergences`: the four judgments of the records, OC160 (a); `ocaml-read-essential`: `bin/repr-ocaml corpus check`, `roundtrip --cmi samples stdlib`, `--spec`, `spec-check`, `embed-check`; no AOT step, OC95 (a); the host `:ocaml` in `bin/lib/gate-run.bash`), the heavy steps, the whole-program j2g runs among them, one at a time under the heavy lock. `bin/gate --full` adds, one after another (concurrently by chain with
  `GATE_FULL_PARALLEL=1`): javac's tool (`bin/javac-vendor tool`, built once per `arbace_javac_key`
  and kept by that key in `ARBACE_GATE_EXES/javac-KEY`, j2g on the gate's executable) and javac on Arbace (`bin/javac-vendor
  check --from`: the tool build's own j2g run, javac's world translated once; `verify`:
  javac's forms read afresh by javac on Arbace, `classes baseline samples jdk java.base`), then
  repr/java's checks on the tool (JN5, JFS9): `bin/repr-java
  check --suite`, `bin/repr-java roundtrip --corpus {part} --text` per corpus part (then mode `:full`
  printed back to the original Java, byte for byte; the classes level by javac on Arbace),
  `bin/jrt-convert verify` (`jdk/` converted afresh, byte for byte), `bin/clojure-tests
  --java-fixtures verify` (the verifies compare the provenance but its host, which names the tool
  that wrote it, JFS32), `bin/repr-java parse-check` (the native parser's trees equal to javac on
  Arbace's, JN8), `bin/repr-java front-check` (the converter's forms from the native trees equal to
  javac's trees', JFS66 (b)), `bin/repr-java attr-check` (the native provider's facts equal to javac on
  Arbace's, JN9a), `bin/repr-java nojavac-check` (since JN9f: the corpora read with no javac, every answer native,
  the forms equal to javac's; these four by corpus since GT5), `bin/java-check --levels L --save` for each of
  read, interp, classes, diagnostics, compiled and print (Java's conformance check, every corpus, a step a level
  since GT5), then `--forms verify` (the essential tests' forms read afresh) and `bin/java-check --judge` (the
  levels' results against the classes; its essential step `java-check-judge` too); then
  `bin/arbace-go --fixpoint --g1 "$GATE_CACHE"` (G1 the gate's build, `ARBACE_GATE_EXES/KEY20`, G2 and G3 built; `GATE_FRESH_G1=1`: G1 built afresh; a `--g1` directory not at the checkout's `gate_key` builds G1 too, three Go-hosted builds), then `bin/clojure-tests --aot` (Clojure's suite compiled, REAP3), then `bin/arbace-go --build-program --check` (every sample, REAP7b); repr/go's round trips (`bin/repr-go roundtrip amd64`, `linux/amd64`, and
  `--text amd64`, each again with `--dirs`: mode `:full` printed back to the original text, byte for byte; then `bin/repr-go native-check --fresh`: the native reader's headers and trees equal to the helper's over every corpus, its raw forms resolved by the final ones, and the final forms of its dumps annotated by goi's checker equal to the helper's, as data and as `.ae` text in both modes), `bin/x509roots-vendor verify`, `check` (in the `repr-go` step); goi (`bin/goi
  annotations`, `test --gc`, `check --jobs 8`, then `check --aot --jobs 8`, after repr/go's round trips); then repr/clojure's
  (`bin/repr-clojure check`, `--suite`, `--roundtrip`); and last C's: `bin/ci judge-test`, then since 2026-11-02 its full checks as heavy steps, a part a corpus, judged against the records and never `--record` (`bin/ci check` on the gate's executable, `check --aot`, `dg`, `annotations`, `bin/repr-c check --tokens`, `parse-check`, `roundtrip`; doc/C23-SURVEY.md, "As built: C's full checks in the gate"; C's pins, gcc's and musl's tarballs and `test/c/host/`, in `bin/gate --pins`); then Scheme's (SM9, light, about 25 minutes nightly and 55 whole): `scheme-read`, `scheme-roundtrip` a part a corpus (the probes, the scans, the SRFIs as one), `si-check` (with `bin/si test`, `fixpoint`, `check-suite`, `vendor verify`, `boot --check` on Chez), `si-aot` (with `srfi --aot`), `si-corpora` (R7RS's, the SRFIs, Chibi's tests on Chibi, `corpora files|divergences`), `si-ecraven` (`SI_TIMEOUT=300`) and `si-mats`, these two with GT8.4's `:rotate` over `test/scheme/subsets.edn`. Then OCaml's (O8, light, the chain `ocaml-full`): `ocaml-roundtrip` a part a corpus (`--cmi`: the trees, `:full` byte for byte, the `.cmi`; the opam set one part), `ocaml-dumps`, `oi-check` (with `suite check`, `suite rollup`), `oi-aot` and `oi-suite-aot` (the light level, GT8.4's `:rotate` over `test/ocaml/subsets.edn`), `ocaml-judges` (`bin/oi judges-test`: OC120 (b)'s tests, OC130 (b), OC112 (b)'s expect check). Parked by the user, 2026-10-26, and out
  of `--full` (kept as they are, run by hand with their scripts and Makefile targets, revisited later
  piece by piece): the box (`bin/arbace-go --build --box` with `bin/tamago-vendor check`, the box's smoke test, oracle
  subset, `bin/box-suite`, `bin/goi check --box`, `bin/box-vendor roundtrip`, `kvm-tests`, `purego-check`),
  `tamago/` and `tamago/cmd/` (`bin/tamago-vendor verify`, `bin/toolchain-vendor verify`, `check`); repr/haskell
  and repr/rust (`bin/repr-haskell`, `bin/repr-rust` round trips), unparked 2026-11-03, stay out of `--full` until their
  rows fit GT8's budget. Run it when the Java forms'
  front end, repr/java, repr/go, j2g or jrt change (after a change to `overlay/jdk/java.base`, jrt's closure
  or repr/java, `bin/jrt-convert update` first; after a change to what j2g asks of the JVM,
  `bin/j2g-edge update`). `--full` is serial by default since GT3 (GTS13 (a); at once its chains overload a 62 GB host:
  REAP9's runs, 2026-10-26). Logs in `.tmp/gate/`. Every gate run and every `bin/arbace-go --build`
  first trims Go's build cache of entries unused for 90 minutes (`bin/lib/gocache.bash`,
  `ARBACE_GOCACHE_KEEP_MIN`). No build may use the host's `go`: only TamaGo's (`ARBACE_GOROOT`).

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it. At each
  freeze, and after the milestones `arbace-inside-itself-v1`, `arbace-interprets-go1.27.1-v1`,
  `arbace-reaps-3-crops-v1`, `arbace-reaps-3-crops-v2` and `arbace-reaps-4-crops-v1`,
  main's journal started over, by the user's decision: the history up to 2026-10-08 is on
  `arbace-for-java-26`, from then to 2026-10-11 on `arbace-for-golang`, from then to 2026-10-15 at
  the tag `arbace-inside-itself-v1`, from then to 2026-10-20 at the tag
  `arbace-interprets-go1.27.1-v1`, from then to 2026-10-27 at the tag `arbace-reaps-3-crops-v1`, from then to
  2026-11-01 at the tag `arbace-reaps-3-crops-v2`, from then to 2026-11-07 at the tag `arbace-reaps-4-crops-v1`, and
  all of it in git.
- When vendoring code from prior art, record its origin (repo, path, revision) in the journal and
  next to the vendored code (for `arbace/`, which holds only `.ae` files: `doc/ARBACE.md`), and
  its license in `LICENSE.md`.

## Working conventions

- Data and source formats target the Clojure reader (`read`), not strict EDN; the two differ
  slightly, and where in doubt the Clojure reader wins.
- The tools' names: the bridges between a language's text and its forms are the `repr` family
  (read/print, representation), repr/java, repr/go and repr/clojure (c2a before), and the
  translators are c2j (the back end, Arbace's namespaces → Java forms; `arbace.aot` before) and
  j2g (Java forms → Go forms), since 2026-10-17 (doc/NAMING-SURVEY.md; g2a's build is
  `arbace.build.go` since then, and class forms are called Java forms; "AOT" names the whole
  compiled pipeline: `ARBACE_AOT`, the packages `aot_arbace_*`); j2a, g2a and a2g before, as dated
  records and the tags name them; j2c, g2c and c2g before 2026-10-11, as the frozen branches and
  dated records name them.
- The tools (c2j, j2g, repr/go, repr/clojure, the suite runner, `bin/jrt-convert`'s `jrt.jrt-sources`
  and, since JN7, `jrt.jrt-edge`)
  run on the tools' Go host, stage 0 by default (`bin/lib/host.bash`, `bin/lib/tools.bash`);
  repr/java's read and javac on javac's tool executable (its only host since JN6), and the JVM's
  records on the frozen jar (`bin/lib/frozen.bash`). Nothing builds or runs a JVM Arbace of the checkout.
- Put temporary and scratch files in `.tmp/`, which is gitignored. The scripts of `bin/` set `TMPDIR` to `.tmp/tmp`
  when it is unset (`bin/lib/tmpdir.bash`), so Arbace's error reports stay out of /tmp.
- Never create or modify anything under `.claude/`.
