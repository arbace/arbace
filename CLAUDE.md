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
  1,471 of 1,837 pass, none fail, the rest in the recorded classes of `bin/goi divergences`
  (doc/go/GOI-SPEC.md, "v1's divergences"); the same in the box on amd64 and in the emulated arm64
  VM. Now: doc/AGENDA.md.
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
  toolchain alone (`target/seed/<arch>/arbace`); `--make` packs a build (the fixpoint's G3, for a
  seed to commit, rarely: a re-seed); `--verify` prints it again from its commit (the fixpoint from
  the seed's own stage 0) and requires the same tar and stage 0 = G1 = G2 = G3. Until S4 the seed
  was a jar (`arbace-for-java-26-v1`'s), and before that the frozen reference tree `clojure/`
  (upstream Clojure and ASM): both on the branch `arbace-for-java-26`. The tools take what they
  need of that tree as data (repr/java's corpus, the baseline of Clojure's suite) from the tag,
  extracted into `.tmp/frozen/` by `bin/lib/tools.bash`.
- Stage 0 hosts Arbace's tools (`bin/lib/host.bash`'s `arbace_host_exe`: `ARBACE_HOST_EXE`, else
  stage 0, built when missing or when the seed changed): c2j, j2g, repr/go, repr/clojure, the suite's runner,
  compiled into it (S6, SA133), a tool's namespace whose source in the checkout differs from the
  embedded copy loaded from the checkout instead, evaluated (`bin/lib/tools.bash`'s
  `arbace_tool`, `ARBACE_PATH`). The JVM runs only the frozen jar of `arbace-for-java-26-v4` (built from its tag,
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
  tools' host).
- `bin/reap` (and `make reap`; REAP1, doc/FULL-SUPPORT-SURVEY.md, RPS3, RPS13): the REAP matrix,
  read, eval, AOT and print for Clojure, Java and Go, judged from the checks' recorded results
  alone (no runs): the cells and their sources in `test/reap/cells.edn`, the judging in
  `test/reap/main.ae`, each level's classes in `test/goi/divergences.edn`'s format
  (`test/clojure/divergences{,-read,-aot,-print}.edn`, `test/repr/go/divergences-{read,print}.edn`,
  goi's, `test/java/divergences-diffcheck.edn`; Go's read numbers `test/repr/go/native-check-results.edn`,
  `bin/reap record-native-check`); a cell whose check is not recorded yet prints "no check yet".
  `bin/clojure-tests --divergences [RESULTS]` judges a suite run per test against the Clojure classes.
- `LICENSE.md` holds all licenses: their full texts and which files each covers. Licenses are
  stated nowhere else, apart from the notices source files came with, which stay. Extend it when
  vendoring from a new source.
- `arbace/` holds Arbace's own Clojure code, namespaces `arbace.*`, all `.ae` (Arbace's source
  extension: Clojure plus forms Clojure cannot evaluate; the loaders take `.ae`, then `.clj`):
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
    still javac's trees', against `test/repr/java/attr-check-known.edn` ("As built: JN9a"); `bin/repr-java
    check --jdk` compares jdk26u's forms with their hashes, `test/repr/java/expected/jdk26u.sha256`
    (`--update` rewrites them, JFS72 (b)), and Java forms → Java (`print`, `arbace.repr.java.print`; the
    `.ae` writer is `arbace.repr.java.layout`) on the tools' host, with its round trip Java →
    forms → Java → forms at the levels forms, ae (the converted text) and classes (javac's, byte
    for byte; `bin/repr-java roundtrip`, corpora baseline, samples and jdk/forms, references in
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
    not embedded.
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
    state `arbace.go.repl` and program runs `arbace.go.run` (GR3; `bin/goi run`, `check`, `test`; `divergences`, GR10: every program of the check that does not pass in a recorded class, `test/goi/divergences.edn`; its corpora and the table's linux std read natively since GP5, the error tests' go/types errors from the helper, the oracle, GPS33; since GP6 the error tests judged on gc's syntax errors when one is a syntax error or go/parser rejects the file, and the invalid programs the converter cannot convert on their raw forms, `arbace.go.run --errorcheck --raw`; since GPS41 (c) every error test with gc's front end's errors beside the checker, `--gc-front`: gc's syntax's branch checks in place of goi's own, noder's pragmas', then its writer's declaration checks),
    all embedded (the table's generator `arbace.go.table` runs on the tools' host). Its back end is
    `go/arbace/goi` (hand-written Go forms; the operators' nodes and the factories' registry
    generated by `test/goi/gen_nodes.ae`); Go forms' API to Arbace is the package `arbace`
    (`go/arbace.ae`, listed in the table, GR5); repr/go's native reader's packages are go/'s too (`go/arbace/repr/`, listed, GP7); goi's cross-dialect oracle cases
    (`test/oracle/forms/go.ae`) are recorded by goi (`bin/oracle record --goi IMPL`).
  - `arbace/j2g/`: the Java forms → Go forms translator (spec `doc/go/J2G-SPEC.md`; `bin/j2g`),
    on the tools' host (`J2G_GO_EXE`), its classes translated in parallel (`J2G_JOBS`, default at
    most 16) with the sequential output byte for byte; the resources compressed by
    `arbace/j2g/zlib.ae` (SA21).
- The Go build (B1a, `doc/go/B1-PLAN.md`): Arbace as a static Go executable. j2g translates
  the runtime (with the generated Go-build variants in `arbace/lang/go/`), and jrt, the Java runtime on Go (`go/arbace/jrt`, hand-written Go forms;
  `overlay/jdk/`, its own Java and variants of JDK classes; `jdk/`, the JDK's Java forms and
  resource data committed (repr/java's output from jdk26u and `overlay/jdk/java.base`, provenance in
  `jdk/provenance.edn`; `bin/jrt-convert update` regenerates them, `verify` checks them; standalone
  S1); `jdk/edge.edn`, the JVM's answers j2g needs, so j2g uses no JVM reflection or ASM
  (`bin/j2g-edge update`, `verify`, `trace`; S2, doc/STANDALONE-NOTES.md); `bin/jrt`); `overlay/go/` patches Go's runtime.
  `jdk/javac/`: javac's Java forms (jdk26u's jdk.compiler with its generated sources,
  java.compiler, `CommandLine` and the java.base classes javac's world needs, unpatched; jrt's
  own Java and j2g variants for that world in `overlay/jdk/javac/`; since JN7 also the JDK build's
  tools `bin/jrt-convert`'s generate step runs on the tool, `make/jdk/src/classes/build/tools` as
  module `buildtools`, with java.xml's SAX parser and the JDK classes they need beyond the
  program's, `overlay/jdk/javac/closure.txt`, and jrt's own zlib `Deflater`; since JFS48 (b) the
  langtools tools `make/langtools/tools/{compileproperties,propertiesparser}`, which generate
  jdk.compiler's resource bundles and `CompilerProperties`/`LauncherProperties` on the tool in
  `convert`'s generate step, compared with the build tree's, and since JFS52 (b) `flagsgenerator`,
  `FlagsEnum`, through javax.tools' API: `ToolProvider`'s tool-world variant returns javac on
  Arbace's `JavaCompiler`, `SystemCompiler`, JavacTool's tasks over its file manager as a standard
  one, against the platform), its source list, provenance
  and edge (`jdk/javac/edge.edn`: what javac's world asks beyond `jdk/edge.edn`), made by
  `bin/javac-vendor convert`: since JN5 read by javac on Arbace (the fixpoint: javac's tool reads
  javac's own sources into these forms), the edge recorded on the frozen JVM (`verify` byte for
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
  `verify`), smoke-tests and runs it. Its namespaces are compiled to Go (`ARBACE_AOT=1`, the
  default since S6; `ARBACE_AOT=0`: evaluated from the image of prepared namespaces, as before):
  by the host when its runtime (`bin/lib/version.bash`'s `arbace_runtime_key`: `arbace/lang`,
  `go`, `overlay/jdk`, `jdk` but j2g's edge) is the checkout's, one j2g run, started as the host
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
- `doc/ALPINE.md`: the host's Alpine packages as one minimal `apk add` line (`bin/alpine-env
  world`, `check`, `simulate`), the subset Arbace uses, and what apk does not provide (jdk26u,
  TamaGo's Go, Go's module cache: only `bin/gate --full`'s vendoring checks need it). No build
  uses cgo (`CGO_ENABLED=0` everywhere, 2026-10-16).
- `Makefile`: a familiar interface for newcomers, nothing more (`make help`): each target runs a
  script of `bin/`, which holds the logic. Keep it in step with `bin/`: when a script's name or
  options change, or a script becomes an objective of the repo, update its target and `##` line;
  `make check-makefile` (the essential gate's first step) checks the scripts it runs exist.
- `bin/gate` is the check every change to main must pass: the Makefile's scripts (`make
  check-makefile`), the seed's hash, stage 0 built from
  it (`bin/seed --build`), the executable and the test build by stage 0 (`bin/arbace-go
  --gate-build`, cached in `.tmp/arbace-go-gate` and keyed by a hash of what they are built from:
  `arbace/`, `go/`, `overlay/`, `jdk/`, `x509roots/`, the fixtures' forms, the build's scripts, the
  seed's and Go's pins; seconds when unchanged, else two Go-hosted builds, about 8 minutes: the
  test build reuses the executable's compiled namespaces), then
  concurrently the smoke test, the per-form differential check (`bin/diffcheck`, Futamura F2, since
  2026-10-17 in the essential gate: the oracle's forms evaluated against compiled by c2j, the Java
  forms interpreted against translated by j2g, against `test/diffcheck/known.edn`; about 5
  minutes, 40 s cached), Clojure's suite on the test build (`test/arbace-go-results.edn`),
  the oracle against the frozen reference, and goi's essential check (`bin/goi check
  --essential`: `$GOROOT/test`'s programs of `test/goi/essential.txt` interpreted against gc), and the generated variants'
  check (`bin/generated-check`, F7.3: `arbace/lang/go/CompilerNodes.ae` and `CompilerAOTFn.ae`, and
  since GP7 repr/go's generated Go forms, the dumper `go/arbace/repr/go/godump` and the renamed copy of gc's
  syntax `go/arbace/repr/go/internal/syntax`, regenerated and compared with the committed files), and since 2026-10-24 `bin/j2g-edge verify` on the
  frozen JVM (j2g's edge recorded afresh: an embedded namespace's new JDK class makes `jdk/edge.edn`
  stale), and `bin/reap` (REAP1: the REAP matrix judged from the checks' recorded results, seconds). `bin/gate --full` adds, concurrently: the Go build afresh with its
  smoke test, then javac's forms and javac on Arbace (`bin/javac-vendor check`, `build`, `verify`:
  javac's forms read afresh by javac on Arbace, `classes baseline samples jdk java.base`), then,
  beside the rest of that chain, repr/java's checks on the tool just built (JN5, JFS9): `bin/repr-java
  check --suite`, `bin/repr-java roundtrip --corpus baseline,samples,jdk --text` (then mode `:full`
  printed back to the original Java, byte for byte; the classes level by javac on Arbace),
  `bin/jrt-convert verify` (`jdk/` converted afresh, byte for byte), `bin/clojure-tests
  --java-fixtures verify` (the verifies compare the provenance but its host, which names the tool
  that wrote it, JFS32), `bin/repr-java parse-check` (the native parser's trees equal to javac on
  Arbace's, JN8), `bin/repr-java front-check` (the converter's forms from the native trees equal to
  javac's trees', JFS66 (b)), `bin/repr-java attr-check` (the native provider's facts equal to javac on
  Arbace's, JN9a); the box's guests from `tamago/`'s forms byte-identical to TamaGo's originals'
  (`bin/arbace-go --build --box`, `bin/tamago-vendor check`), the box's smoke test, its oracle's
  subset and `bin/box-suite`, then
  `bin/arbace-go --fixpoint`; repr/go's round trips (`bin/repr-go roundtrip amd64`, `linux/amd64`, and
  `--text amd64`: mode `:full` printed back to the original text, byte for byte; then `bin/repr-go native-check --fresh`: the native reader's headers and trees equal to the helper's over every corpus, its raw forms resolved by the final ones, and the final forms of its dumps annotated by goi's checker equal to the helper's, as data and as `.ae` text in both modes), repr/haskell's
  (`bin/repr-haskell roundtrip --iface --text --layout`: GHC's trees and lines over GHC 9.14.1's
  boot libraries, in the braces and the layout layouts, then their interfaces compiled at `-O1`,
  then mode `:full` printed back byte for byte, and `test`; on the host's GHC), repr/clojure's
  (`bin/repr-clojure check`, `--suite`, `--roundtrip`), repr/rust's (`bin/repr-rust roundtrip --iface --text --pretty`: rustc's trees and
  lines over std's five crates, then rustdoc's JSON of the crates without spans, then the pretty
  layout's trees, then mode `:full` printed back byte for byte, and `test`; on the host's rustc;
  `REPR_RUST_CORPORA=all` adds RR4's corpora and the UI suite, `'all --run --std'` the behaviour level, `'all --code'` the lowered and code levels), the box's vendoring checks (`bin/box-vendor roundtrip`, `kvm-tests`, `purego-check`),
  `bin/tamago-vendor verify`, `bin/x509roots-vendor verify`, `check`; goi (`bin/goi
  annotations`, `test --gc`, `check --jobs 8`, after repr/go's round trips); Go's toolchain as forms
  (`bin/toolchain-vendor verify`, `check`). Run it when the Java forms'
  front end, repr/java, repr/go, j2g or jrt change (after a change to `overlay/jdk/java.base`, jrt's closure
  or repr/java, `bin/jrt-convert update` first; after a change to what j2g asks of the JVM,
  `bin/j2g-edge update`). Logs in `.tmp/gate/`. Every gate run and every `bin/arbace-go --build`
  first trims Go's build cache of entries unused for 90 minutes (`bin/lib/gocache.bash`,
  `ARBACE_GOCACHE_KEEP_MIN`). No build may use the host's `go`: only TamaGo's (`ARBACE_GOROOT`).

## Records

- `doc/AGENDA.md` holds the current state of the work. Keep it up to date: rewrite it freely so it
  always reflects where things stand and what comes next.
- `doc/JOURNAL.md` is append-only. Add a dated entry for every important decision or action: what
  was done, why, and what alternatives were considered. Name sources precisely, including
  repo, path and commit/tag for anything studied or vendored. Never edit or delete past entries.
  Correct them with a new entry. Write it so the progress could be recreated from it. At each
  freeze, and after the milestones `arbace-inside-itself-v1` and `arbace-interprets-go1.27.1-v1`,
  main's journal started over, by the user's decision: the history up to 2026-10-08 is on
  `arbace-for-java-26`, from then to 2026-10-11 on `arbace-for-golang`, from then to 2026-10-15 at
  the tag `arbace-inside-itself-v1`, from then to 2026-10-20 at the tag
  `arbace-interprets-go1.27.1-v1`, and all of it in git.
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
