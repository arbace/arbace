# Vendoring `clojure/` as `arbace/`, and self-hosting (agenda step 4)

The frozen baseline `clojure/` is vendored under `arbace/`, renamed `clojure.*` → `arbace.*`, all
of it `.clj`: the Java files as class forms (doc/classes/SPEC.md), Clojure's own `.clj` renamed.
The vendored tree is the source of truth from now on and is maintained by hand; how it was
derived is recorded below and can be replayed with `bin/vendor-arbace`. The bootstrap of SPEC
§9.6 works: stage 0 compiles it into stage 1, stage 1 into stage 2, stage 2 into stage 3, and
stage 1, 2 and 3 are byte-identical. Clojure's test suite gives the baseline's result on stage 1
and stage 2.

## Layout

| path | what |
|---|---|
| `arbace/*.clj`, `arbace/{core,java,pprint,reflect,repl,test,tools}/**.clj` | Clojure's namespaces, renamed: `arbace.core` (with `core_print.clj` and the other files it loads), `arbace.main`, `arbace.pprint`, `arbace.java.io`, ... |
| `arbace/lang.clj`, `arbace/lang/*.clj` | namespace `arbace.lang` (Java package `clojure.lang`), one file per Java file: `RT.clj`, `Compiler.clj`, ... |
| `arbace/asm.clj`, `arbace/asm/**.clj` | `arbace.asm`, `arbace.asm.commons`, `arbace.asm.signature` (ASM, BSD 3-Clause) |
| `arbace/java/api.clj`, `arbace/java/api/Clojure.clj` | `arbace.java.api` |
| `arbace/lang/Main.clj` | the class `arbace.lang.Main`, Clojure's main class `clojure.main` (`clojure/main.java`); vendored as `arbace.main` in `arbace/main_class.clj` with the package file `arbace.clj`, moved after vendoring (below) |
| `arbace/classes/`, `arbace/j2c/` | the tools (javalisp, `arbace/javalisp/`, was dropped later), unchanged in place; no vendored name clashes with them |
| `doc/ARBACE.md` (was `arbace/README.md`) | what `arbace/` holds and its origin (licenses: `LICENSE.md`) |
| `bin/vendor-arbace` | replays the derivation into `.tmp/vendor/` (never into `arbace/`) and checks it |
| `bin/build-arbace` | builds stages 1 to 3 into `target/` and checks the fixpoint; `--suite` runs the test suite on stages 1 and 2 |
| `target/stage1`, `target/stage2`, `target/stage3` | build output (gitignored, `/target/`): each stage's classes, those of the class forms under `arbace/` (815) and the AOT-compiled `arbace.classes` namespaces (1,289) |

`clojure/version.properties` is not vendored (below). Nothing under `clojure/` changed.

The class-form files of a package are ordinary `.clj` files on the class path; the Clojure
runtime never loads them (it loads `arbace/core.clj` and the other namespaces), only the class
forms compiler reads them, as sources (SPEC §9.2).

## How it was produced

On 2026-10-07, on the `vendor` branch from `7229dd9`, with the converter `arbace.j2c` of that
commit (unchanged since) and JDK 26.0.2.1. Inputs: `clojure/` as tracked (Clojure
`98d735fab02f337cee654cb0629bddc09883a75a` and ASM `0460f74ba64230e5b846967c81a52628b5dd0596` as
seeded, with the two seed fixes of the journal). `bin/vendor-arbace` replays exactly these steps:

1. `bin/j2c -m arbace.j2c.main convert --rename clojure=arbace OUT/converted . clojure`: the 183
   Java files, all classes, packages and namespaces renamed by the converter (189 files: one per
   Java file plus the package namespaces).
2. `java -cp . clojure.main -m arbace.j2c.rename vendor OUT/converted . OUT/tree`
   (`arbace/j2c/rename.clj`, new):
   - converted files: the converter's first line (an absolute path and "regenerate rather than
     edit") is replaced by the Java file's leading comment block (its license notice, as `;;`
     lines, verbatim) and a line naming the Java file, the Clojure revision and the tools; the
     rest is renamed by the rules below (what is left of `clojure` in it are string literals);
   - `clojure/**/*.clj` (48 files): path and text renamed by the same rules, then two hand edits
     (each checked to apply exactly once): the `version.properties` fold and the server
     property prefix (below).
3. Copied into the worktree: `cp -r OUT/tree/* .` (giving `arbace.clj`, since removed, and the vendored files
   under `arbace/`).

Check (step 3 of `bin/vendor-arbace`): the Java of `clojure/` renamed by the same rules (plus
`package clojure;` → `package arbace;` in `main.java`, a single-segment package the rules leave
alone) and compiled by javac gives 812 classes; the vendored class forms compiled at stage 0 give
812 classes of the same names, all with javac's class shapes (SPEC §3: flags, signatures,
attributes, members, symbolic content of code). Since the strings of the renamed Java are what the
rules make of the original strings, this also checks that the converted files' strings are renamed
as the Java would be. `diff -r .tmp/vendor/tree/arbace arbace` shows only the tools and the README.

## What is renamed

One function, `arbace.j2c.rename/rename`, a textual rewrite of `clojure` to `arbace` where it
starts one of the names below. It is used for the vendored sources and for the renamed copy of
the test suite and its libraries (below), so both sides agree. Names count written dotted
(`clojure.lang.RT`), as paths (`clojure/lang/RT`, `clojure/core.clj`), in descriptors
(`Lclojure/lang/Var;`), array names (`[Lclojure.lang.Var;`), regex literals (`clojure\.lang\.`),
qualified symbols and keywords (`clojure.core/map`, `:clojure.core/x`), munged class names
(`clojure.core$map`, `clojure/core__init`) and `-D` options.

Renamed:

1. **Java packages**, with whatever follows: `clojure.lang`, `clojure.asm` (and `.commons`,
   `.signature`), `clojure.java.api`; and the class `clojure.main`. In code (by the converter's
   `--rename`) and in strings: class names for `Class.forName` and printing, internal names and
   descriptors in `Compiler` (`"clojure/lang/RestFn"`, `"(Ljava/lang/String;)Lclojure/lang/Keyword;"`),
   the method strings of `Intrinsics` (`"public static long clojure.lang.Numbers.add(long,long)"`,
   which must match `Method.toString()`).
2. **The vendored namespaces**, exactly: `clojure.core`, `.core.protocols`, `.core.reducers`,
   `.core.server`, `.data`, `.datafy`, `.edn`, `.inspector`, `.instant`, `.java.basis`,
   `.java.basis.impl`, `.java.browse`, `.java.browse-ui`, `.java.io`, `.java.javadoc`,
   `.java.process`, `.java.shell`, `.main`, `.math`, `.pprint`, `.reflect`, `.repl`,
   `.repl.deps`, `.set`, `.stacktrace`, `.string`, `.template`, `.test`, `.test.junit`,
   `.test.tap`, `.tools.deps.interop`, `.uuid`, `.walk`, `.xml`, `.zip`, the files loaded into
   them (`core_print`, `core_proxy`, `core_deftype`, `genclass`, `gvec`, `pprint/cl_format`, ...,
   `reflect/java`), and classes defined in their packages (`clojure.core.VecNode`,
   `clojure.core.protocols.InternalReduce`). As symbols, keywords, strings (`RT.var("clojure.core",
   ...)`, `"clojure.core"` in `Compiler`, `main.clj`'s list of core namespaces, `pprint`'s
   dispatch) and resource paths (`RT.load("clojure/core")`).
3. **The keyword namespace `clojure.error`** (`:clojure.error/phase` and the rest, in `Compiler`,
   `main.clj`, `repl.clj`).
4. **System properties**: `clojure.compile.path`, `clojure.compile.warn-on-reflection`,
   `clojure.compile.unchecked-math`, `clojure.compiler.*` (`direct-linking`, `elide-meta`,
   `disable-locals-clearing`; `Compiler` reads them by the prefix `"clojure.compiler."`),
   `clojure.read.eval`, `clojure.main.report`, `clojure.server.*` (socket servers; `server.clj`
   compares the first segment to `"clojure"`, a hand edit), `clojure.basis`. So
   `-Darbace.compiler.direct-linking=true`, `-Darbace.compile.path=...`.
5. `clojure.jar` in `main.clj`'s usage text (`java -cp arbace.jar arbace.main ...`, now
   `arbace.lang.Main`).

Left alone (with the reason):

- Names of other libraries: `clojure.spec.alpha` (and `.gen.alpha`, `.test.alpha`,
  `:clojure.spec.alpha/problems`, `RT.load("clojure/spec/alpha")` in `Compiler`'s macro check,
  the property `clojure.spec.check-asserts`), `clojure.core.specs.alpha`, `clojure.tools.deps`
  (functions of the CLI's tools.deps that `repl.deps` asks the CLI to run), `:clojure.exec/*`
  (the CLI's `-X`/`-T` protocol), `"clojure"` (the CLI command `tools.deps.interop` runs),
  `~/.clojure/deps.edn`; in the test libraries `clojure.test.check`, `clojure.test.generative`,
  `clojure.tools.namespace`, `clojure.tools.reader`, `clojure.data.generators`,
  `clojure.java.classpath`. Spec is stubbed out in the baseline already; the others are not part
  of Clojure.
- Identifiers that merely contain the word: `clojure-version`, `*clojure-version*`,
  `refer-clojure`/`:refer-clojure`, `clojure-fn?` (locals), `CLOJURE_NS` and other Java
  constants, the generated proxy field `__clojureFnMap`, the thread names
  `clojure-agent-send-pool-%d`/`clojure-agent-send-off-pool-%d`, the temp file prefix
  `clojure-` of `main.clj`'s error reports. Renaming them would change Clojure's API or nothing
  that names a package or namespace.
- Prose and URLs: "Clojure", `clojure.org`, `code.google.com/p/clojure`, comments with old paths
  (`/Users/rich/dev/clojure/src/zip.clj`), `clojure.contrib` examples, `(require '(clojure zip
  [set :as s]))` in `require`'s docstring (a prefix list the rules cannot see), "Cannot remove
  clojure namespace" (`Namespace`), `clojure/replace` in `walk.clj`'s docstrings.
- The license notices and the "rich Mar 25, 2006" comments at the head of converted files are
  the Java files' own text, verbatim (one says "placed under clojure.lang namespace").

## Hand edits

1. `arbace/core.clj`, the version: `*clojure-version*` was built from the resource
   `clojure/version.properties`; the value is folded in as a literal
   (`version-string "1.13.0-master-SNAPSHOT"`) and the parsing kept, so `*clojure-version*` and
   `(clojure-version)` are unchanged. Dropping the machinery was the alternative; the test suite
   and tools use `*clojure-version*`, so it stays. The REPL banner reads
   `Clojure 1.13.0-master-SNAPSHOT`.
2. `arbace/core/server.clj`: `(= k1 "clojure")` → `(= k1 "arbace")`, the prefix of the
   `arbace.server.*` properties.

## The bootstrap

`bin/build-arbace` (about one minute):

- **Stage 0 → stage 1.** The frozen `clojure/` loads `arbace.classes` through
  `arbace.classes.boot` and runs `arbace.classes.build` over the packages
  (`arbace` until the main class moved, below), `arbace/lang`, `arbace/asm`, `arbace/asm/commons`, `arbace/asm/signature`, `arbace/java/api`:
  183 files → 812 classes in `target/stage1` (20 s). Every class those files declare is entered
  from its source (SPEC §9.2): none exists at stage 0.
- **Stage 1** is `target/stage1` plus the `.clj` sources: `java -cp target/stage1:. arbace.lang.Main`
  starts a REPL (`Clojure 1.13.0-master-SNAPSHOT`, `(class [])` is
  `arbace.lang.PersistentVector`, `#'arbace.core/map`); `arbace.lang.RT` loads `arbace/core.clj`
  with `arbace.lang.Compiler`. With `-verbose:class` no `clojure.*` class is loaded.
- **Stage 1 → stage 2.** `java -cp target/stage1:. arbace.lang.Main -e "(require 'arbace.classes.boot)
  (arbace.classes.build/-main ...)"`: the compiler, plain Clojure, is compiled by
  `arbace.lang.Compiler` against `arbace.core`, `arbace.string`, `arbace.lang` and `arbace.asm`,
  and compiles the same files into `target/stage2`.
- **Stage 2 → stage 3** likewise; `target/stage3` must equal `target/stage2` byte for byte.

Result (2026-10-07): stage 1, stage 2 and stage 3 are byte-identical, all 812 classes. Stage 1
already equals stage 2: the compiler is deterministic, and every stage sees the same class
environment (below), so the runtime it runs on does not show in its output. There was no
nondeterminism to fix.

Running a stage: `java -cp target/stageN:. arbace.lang.Main` (any `arbace.main` option, e.g. `-e`,
`-m`, a script). The class path needs the repository root for the `.clj` sources.

## Clojure's test suite on stages 1 and 2

`bin/clojure-tests` has a rename mode, `CLOJURE_TESTS_RENAME=arbace`:

- the fetched suite (`test/`, `src/script/`, and the checkout's top-level files, as
  `sequences.clj` reads `readme.txt`) is copied to `.tmp/clojure-tests/RUN/suite/` and renamed
  with `arbace.j2c.rename` (`.clj`, `.cljc`, `.java`); the test namespaces
  (`clojure.test-clojure.*`, `clojure.test-helper`) keep their names, so the reference
  `test/baseline-results.edn` applies unchanged;
- the six test libraries are extracted from their jars into `.tmp/clojure-tests/lib-arbace/` and
  renamed the same way (their own namespaces keep their names, their uses of `clojure.core`,
  `clojure.string`, `clojure.lang.*` ... are renamed);
- the runner `test/run_clojure_tests.clj` is renamed into the run directory; the harness uses
  `arbace.lang.Main`, `arbace.lang.Compile`, `-Darbace.compiler.direct-linking=true`,
  `-Darbace.compile.path`, and AOT-compiles the renamed core namespaces;
- one renaming artifact is adjusted: `test_pretty.clj`'s expected layouts align continuation
  lines under `[clojure.pprint :only (`; `arbace` is one character shorter, so those lines lose
  one space (a `sed` in the harness, two assertions);
- since the renames after vendoring (below), `test-proxy-method-order` lists the `IProxy` methods
  as `__initArbaceFnMappings` and so on (another `sed`).

```
CLOJURE_TESTS_RENAME=arbace CLOJURE_TESTS_RUN=stage1 CLOJURE_SRC=$PWD/target/stage1:$PWD bin/clojure-tests
CLOJURE_TESTS_RENAME=arbace CLOJURE_TESTS_RUN=stage2 CLOJURE_SRC=$PWD/target/stage2:$PWD bin/clojure-tests
```

(or `bin/build-arbace --suite`). Results, 2026-10-07:

| run | namespaces | tests | assertions passed | test.generative | regressions |
|---|---:|---:|---:|---:|---|
| baseline (reference) | 83 | 809 | 20,718 / 20,750 | 27 / 27 | |
| stage 1 | 83 | 809 | 20,718 / 20,750 | 27 / 27 | none |
| stage 2 | 83 | 809 | 20,718 / 20,750 | 27 / 27 | none |

The 32 failing assertions are the baseline's: they expect spec's error messages.

Before the regex-literal and `readme.txt` handling, stage 1 showed 10 more failures, all from the
renaming of the suite (regexes `#"clojure\.lang\.Agent"`, the pprint layout, a relative file).

## Changes to arbace.classes

The compiler had only run on the frozen runtime and had compiled converted code whose classes
already existed (the baseline's own, under `clojure.*`), so the class environment took them from
the class path. Under `arbace.*` nothing exists at stage 0, and at stage 1 the classes being
compiled are the running runtime's own. Changes:

1. **Stage-neutral sources.** The compiler names its runtime as Arbace's everywhere:
   `arbace.string`, `arbace.java.io`, `arbace.set`, `arbace.lang.*`, `arbace.core/...`
   (previously `clojure.*`; ASM already was `arbace.asm`). Strings that named `clojure/lang/...`
   (descriptors and owners of `Var`, `IFn`, `RT`, `Keyword`, `Symbol`, `Reflector`, the
   collection interfaces, for Clojure code in class bodies) are computed from
   `arbace.classes.types/lang`, the package of the running `arbace.lang.RT`.
2. **`arbace.classes.boot` works at every stage.** At stage 0 it reads the compiler's sources
   mapping every `arbace.X` symbol to `clojure.X` (it mapped only `arbace.asm` before), except the
   tools' own `arbace.classes.*`, `arbace.j2c.*` (and then `arbace.javalisp.*`). From stage 1 on it just
   requires them. It interns the class forms' names into the running core namespace
   (`clojure.core` or `arbace.core`). The file itself names neither runtime (it finds the running
   core as `(namespace `ns)`).
3. **`arbace.classes.build`** (new): compiles trees of class forms files into class files at any
   stage; `bin/build-arbace` and `bin/vendor-arbace` use it. It binds
   `arbace.classes.env/*from-source*` to the packages being built: their classes are never taken
   from the class path, even when loaded, so a stage compiling itself sees the classes of its
   sources, exactly as stage 0 does (new dynamic var, honoured by `env/load-class`).
4. **Imports of classes that do not exist (yet).** `import-classes!` recorded such imports as
   failures, and names imported from another package (`ClassVisitor` in `arbace.asm.commons`)
   did not resolve. They now go into `env/source-imports` (namespace → simple name → class), which
   `resolve-class-sym` consults after the namespace's mappings, then finds the class's source.
5. **Java's order of imports.** `resolve-class-sym` looked at the namespace's mappings before
   the class's own package, and every namespace maps Clojure's default imports
   (`RT/DEFAULT_IMPORTS`: `Compiler`, `RT`, `Var`, ...), so at stage 0 `Compiler$FISupport` in
   `arbace.lang.Reflector` resolved to `clojure.lang.Compiler`. Now: explicit imports, the own
   package, then the default imports and `java.lang`, as Java's single-type imports, package and
   on-demand imports.
6. **Source lookup for `p/C_class.clj`** (SPEC §9.1's `_class` rule): `enter-from-source!` tries
   `arbace/main_class.clj` before `arbace/main.clj` (which is the namespace `arbace.main`).
7. **Source lookup crashed on classes with options and members**: `declares?` read the options
   with `(apply hash-map (rest (drop-while (complement keyword?) cf)))`, which fails on any class
   with `:extends`/`:implements` followed by members ("No value supplied for key"). It uses
   `parse/split-options` now.
8. **Bridges of classes entered from source.** Only compiled classes got their derived bridge
   methods; classes entered as declarations did not, so a subclass compiled against a
   source-entered superclass re-derived the superclass's bridges (38 classes had extra
   `cons`/`withMeta`/`assoc` bridges, e.g. `ArraySeq` re-bridging `ASeq.cons`). Entered classes
   now get their bridges as well (`add-bridges!` runs once per class, `:bridges-done`).
9. `arbace.classes.shape/diff-dirs` (new): compares the shapes of two directories of classes.

After rebasing on `f240a04`, `bin/class-forms-tests` passes (60 tests, 112 assertions) and
`bin/j2c-check` is clean (the converted baseline and all 5 sample files shape-identical to
javac's). One environment note: javac, which the tests run in-process with the repository on
its class path, prefers a `.java` source to its `.class` when the source is newer, so in a fresh
worktree where git wrote `clojure/**/*.java` after the `.class` files a test compiled
`clojure/lang/IDeref.java` itself; `touch`ing the class files fixes it.

## Changes to arbace.j2c

- `arbace.j2c.rename` (new): the renaming rules, `vendor!`, and a command line (`files`,
  `stdin`, `tree DIR EXT...`, `vendor CONV SRC OUT`). The converter itself is unchanged.

## Decisions taken, and alternatives

- **Committed sources, not regenerated.** `arbace/` is committed and is the source of truth
  (the user's intent); `bin/vendor-arbace` only reproduces the derivation for audit, into
  `.tmp/vendor/`. The alternative, regenerating `arbace/` from `clojure/` on every build, would
  keep `clojure/` the real source, against the point of the step.
- **Strings renamed after conversion** with one rule set shared with the test suite, rather than
  renaming the Java text before converting (equivalent; the user asked for
  `--rename clojure=arbace`, and the javac check covers both).
- **Exact name lists** rather than renaming every `clojure.` prefix: other libraries keep their
  names (`clojure.spec.alpha`, `clojure.test.check`, `clojure.tools.namespace`), so their jars
  work unrenamed apart from their uses of Clojure, and the suite's namespace names, and so the
  reference results, stay.
- **System properties renamed** (`arbace.compile.path`, ...): they are Clojure's
  configuration names in the `clojure.` namespace of names.
- **Build output in `target/`** (already gitignored, as the vendored javac's build used it), one
  directory per stage.
- **A stage compiles from sources only** (`*from-source*`), rather than reflecting on its own
  loaded classes: the class environment is then the same at every stage, which is what makes
  stage 1 equal stage 2.

## After vendoring: hand changes

The user's decisions on the open points of step 4 (2026-10-07), applied by hand to `arbace/`
(so `bin/vendor-arbace`'s replay no longer equals `arbace/` in these places):

1. **The main class is `arbace.lang.Main`.** The class `clojure.main`, vendored as `arbace.main`
   in the single-segment package `arbace` (`arbace/main_class.clj`, with the package file
   `arbace.clj` at the repository root), moved into the package `arbace.lang`, next to
   `arbace.lang.Compile`, `Repl` and `Script`: `arbace/lang/Main.clj`, loaded by
   `arbace/lang.clj`. `arbace.clj` is gone; nothing remains at the repository root. The
   namespace `arbace.main` keeps its name. References updated: `Repl.clj` and `Script.clj`
   (`Main/legacy_repl`, `Main/legacy_script`), the usage texts of `arbace/main.clj`,
   `bin/build-arbace` (stage launcher, and the package list loses `arbace`), `bin/clojure-tests`
   (rename mode runs `arbace.lang.Main`), `CLAUDE.md`, `arbace/README.md`. Launch a stage with
   `java -cp target/stageN:. arbace.lang.Main`.
2. **Runtime-visible `clojure` names renamed to `arbace`:**

   | where | before | after |
   |---|---|---|
   | `lang/Agent.clj`, agent thread names | `clojure-agent-send-pool-%d`, `clojure-agent-send-off-pool-%d` | `arbace-agent-send-pool-%d`, `arbace-agent-send-off-pool-%d` |
   | `core_proxy.clj`, generated proxy field | `__clojureFnMap` | `__arbaceFnMap` |
   | `lang/IProxy.clj`, `core_proxy.clj`, the methods every proxy class has | `__initClojureFnMappings`, `__updateClojureFnMappings`, `__getClojureFnMappings` | `__initArbaceFnMappings`, `__updateArbaceFnMappings`, `__getArbaceFnMappings` |
   | `main.clj`, error report temp files | prefix `clojure-` | prefix `arbace-` |
   | `core/server.clj`, socket server thread names | `Clojure Server <name>`, `Clojure Connection <name> <id>` | `Arbace Server ...`, `Arbace Connection ...` |
   | `java/process.clj`, process I/O thread names | `Clojure Process IO <n>` | `Arbace Process IO <n>` |

   Kept: `clojure-version` and `*clojure-version*` (libraries call them), the REPL banner
   (`Clojure 1.13.0-master-SNAPSHOT`, printed from `clojure-version`), `refer-clojure`, the class
   `arbace.java.api.Clojure` (an API class name), the JSR-45 stratum `Clojure` in
   `SourceDebugExtension` (debuggers look for it), error messages and docstrings naming the
   language, and `CLOJURE_*` constants (not visible). The suite's `test-proxy-method-order`
   expects the `IProxy` method names; the harness adjusts it (above).

3. **The class forms native** (SPEC §9.5; `doc/classes/COMPILER-NOTES.md`, "Native class
   forms"): `arbace/core.clj` loads the new `arbace/core_classes.clj` (the class forms' names in
   `arbace.core`), and `arbace/lang/Compiler.clj` knows their special forms (`class*`,
   `switch*`, ...), handing them to `arbace.classes` through `arbace.classes.native`: new parser
   `Compiler$ClassFormsExpr`, the signal it throws caught by `FnExpr/parse`, `eval` and
   `compile1`, top-level `do` siblings, and signals instead of three errors (a primitive tag on a
   local with a primitive initializer, `set!` of a local, `new` of an array class). Stage 1 needs no `arbace.classes.boot`.
4. **The §5.4 operators inlined** (SPEC §9.5; `doc/classes/COMPILER-NOTES.md`, "Native class
   forms"): `arbace/lang/Numbers.clj` gains the static methods the operators' `:inline`
   expansions call: `andInt`, `orInt`, `xorInt`, `notInt` (`int`), `unchecked_float_add`,
   `_subtract`, `_multiply`, `_divide`, `_remainder`, `_negate` (`float`), and
   `unchecked_divide`, `unchecked_remainder` with the nine `long`/`double`/`Object` overloads of
   Clojure's `add`. `arbace/lang/Intrinsics.clj` maps the primitive ones to `IAND`, `IOR`,
   `IXOR`, `ICONST_M1 IXOR`, `FADD` ... `FNEG`, `LDIV`, `DDIV`, `LREM`, `DREM`.
5. **Reproducible AOT output** (needed since `bin/build-arbace` AOT-compiles `arbace.classes`
   into the stages, which must be byte-identical): `Compiler$LocalBinding` (in
   `arbace/lang/Compiler.clj`) has a `hashCode` from its index and name (equality stays
   identity), so a fn's closed-over locals, kept in a hash map, come out as fields and
   constructor parameters in the same order in every JVM (it was the identity hash); and
   `generate-proxy` (`arbace/core_proxy.clj`) emits the superclass's constructors sorted by
   parameter types, not in reflection order (which varies between JVM runs; methods were
   sorted already).
6. **Deftypes with class forms** (SPEC §9.5; COMPILER-NOTES, "Native class forms"):
   `Compiler$NewInstanceExpr$DeftypeParser/parse` catches the class forms' signal from a
   method body of `deftype*` and analyzes `nil` in its place after
   `arbace.classes.native/compile-deftype` has compiled and defined the class.

## Open decisions for the user

1. (decided, above) `arbace.clj` and the class `arbace.main`.
2. (decided, above) the leftover `clojure` identifiers.
3. **`CLAUDE.md`'s layout section** still describes `arbace/` as the tools only; it could name
   the vendored tree and `bin/build-arbace` (left to the main session).
4. (done, item 3 of "After vendoring") the class forms native at stage 1.
