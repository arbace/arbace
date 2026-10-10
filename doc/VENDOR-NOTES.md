# Vendoring `clojure/` as `arbace/`, and self-hosting (agenda step 4)

The frozen baseline `clojure/` is vendored under `arbace/`, renamed `clojure.*` → `arbace.*`, all
of it `.clj`: the Java files as class forms (doc/classes/SPEC.md), Clojure's own `.clj` renamed.
The vendored tree is the source of truth from now on and is maintained by hand; how it was
derived is recorded below and can be replayed with `bin/vendor-arbace`. The bootstrap of SPEC
§9.6 works: stage 0 compiles it into stage 1, stage 1 into stage 2, stage 2 into stage 3, and
stage 1, 2 and 3 are byte-identical. Clojure's test suite gives the baseline's result on stage 1
and stage 2, and, since spec was vendored too (below, "Spec"), passes in full.

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
| `arbace/spec/**.clj`, `arbace/core/specs/alpha.clj` | spec.alpha and core.specs.alpha, vendored later and renamed alike (below, "Spec") |
| `bin/vendor-spec` | replays the vendoring of spec into `.tmp/vendor-spec/` |
| `test/arbace-results.edn` | the suite's reference for Arbace's stages (`test/baseline-results.edn` is the frozen baseline's) |
| `bin/build-arbace` | builds stages 1 to 3 into `target/` and checks the fixpoint; `--suite` runs the test suite on stages 1 and 2 |
| `target/stage1`, `target/stage2`, `target/stage3` | build output (gitignored, `/target/`): each stage's classes, those of the class forms under `arbace/` and the stage's AOT-compiled namespaces (below, "Compiled namespaces"; 5,756 classes per stage on 2026-10-07) |

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
3. Copied into the worktree: `cp -r OUT/tree/* .` (giving `arbace.clj`, since removed, and the
   vendored files under `arbace/`).

Check (step 3 of `bin/vendor-arbace`): the Java of `clojure/` renamed by the same rules (plus
`package clojure;` → `package arbace;` in `main.java`, a single-segment package the rules leave
alone) and compiled by javac gives 812 classes; the vendored class forms compiled at stage 0 give
812 classes of the same names, all with javac's class shapes (SPEC §3: flags, signatures,
attributes, members, symbolic content of code). Since the strings of the renamed Java are what the
rules make of the original strings, this also checks that the converted files' strings are renamed
as the Java would be. `diff -r .tmp/vendor/tree/arbace arbace` showed only the tools and the
README (now `doc/ARBACE.md`); since then the hand changes below differ too.

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
  `clojure.java.classpath`. Spec is stubbed out in the baseline already (it has since been
  vendored and renamed as well: below, "Spec"); the others are not part of Clojure.
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

`bin/build-arbace` (about 1.5 minutes):

- **Stage 0 → stage 1.** The frozen `clojure/` loads `arbace.classes` through
  `arbace.classes.boot` and runs `arbace.classes.build` over the packages (`arbace` until the
  main class moved, below), `arbace/lang`, `arbace/asm`, `arbace/asm/commons`,
  `arbace/asm/signature`, `arbace/java/api`: 183 files → 812 classes in `target/stage1` (20 s).
  Every class those files declare is entered from its source (SPEC §9.2): none exists at
  stage 0.
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

### Compiled namespaces, the jar and the AOT cache (2026-10-07)

The first recommendation of `doc/MODERN-COMPILER.md` (§1, §3.1, §4.15), in `bin/build-arbace`:

- **Each stage holds its namespaces AOT-compiled by its own runtime.** After a stage's class
  forms are built, one `arbace.lang.Compile` run on `-cp target/stageN:.` with
  `-Darbace.compile.path=target/stageN -Darbace.compiler.direct-linking=true` compiles, in
  order, the 35 namespaces of upstream `build.xml`'s `compile-clojure` (renamed; the list
  `bin/clojure-tests` also uses: `arbace.core` ... `arbace.repl.deps`), then
  `arbace.classes.boot` (the class forms compiler). Direct linking, as Clojure's release jar.
  4,276 classes per stage (2,941 of them the 35 namespaces, 1,335 `arbace.classes`), in about
  6 s. `arbace.j2c` stays source (a stage-0 tool, run by the frozen `clojure/`); the package
  files `arbace/lang.clj`, `arbace/asm.clj`, `arbace/java/api.clj` are not namespaces to compile
  (they load the class forms). The next stage is built by these classes.
- **The fixpoint covers them**: stages 1, 2 and 3 are byte-identical, 5,091 classes each (815
  class-form classes, 4,276 namespace classes). No further nondeterminism had to be fixed:
  the two fixes made for `arbace.classes` (`LocalBinding.hashCode`, sorted proxy
  constructors, "After vendoring" item 5) were enough, and three runs of the compile into fresh
  directories gave identical bytes. Gensym and fn-class numbers (`RT.nextID`) depend only on
  the load order, which is the same in every stage: each compile starts from a stage without
  namespace classes, so `arbace.core` loads from source first in all of them.
- **`target/arbace.jar`**: stage 2's classes plus every `arbace/**/*.clj` (what
  `-cp target/stage2:.` gives), manifest `Main-Class: arbace.lang.Main`. It is reproducible: the
  build makes the jar of stage 3 too and requires the two to be equal byte for byte. Entries in
  byte order, no directory entries, a fixed manifest written as a file (`jar --no-manifest`),
  times fixed through the files' mtimes in a staging copy under `TZ=UTC`: sources at
  2000-01-01T00:00:00Z and classes ten seconds later. They must differ: `RT.load` takes a
  namespace's `__init` class only when it is strictly newer than its source, and `jar --date`
  gives every entry the same time (with it all namespaces loaded from source).
- **`target/arbace.aot`**: a JDK AOT cache (JEP 483, 514; 36 MB at first, 39.4 MB on
  2026-10-08), made by a training run `bin/arbace --aot-train < test/aot-training.clj`
  (`-XX:AOTCacheOutput`, one step): a REPL session requiring the common libraries, a
  `defclass`, deftype/defrecord/protocols, `arbace.test`, futures, `pmap`, agents, `doc` and
  `source`. The cache refuses directories on the
  class path at creation, hence the jar. It is tied to the JDK build, the jar (path, size, mtime)
  and the JVM options (e.g. compact headers), so every build remakes it; it is not compared
  between stages (it is not reproducible and holds nothing of Arbace's own making).
  Since 2026-10-08 it holds no method profiles: the training run passes
  `-XX:-AOTRecordTraining`. With the profiles (JEP 515) the JIT trusted those of the short
  training session, and long hot loops (`into` with a transducer, `conj!`) ran 15-30% slower;
  without them a short REPL session warms up about 40 ms slower, and startup is unchanged.
- **`bin/arbace`** runs `arbace.lang.Main` from the jar with `-XX:+UseCompactObjectHeaders`
  (JEP 519; also used for the training run, since the cache must match), and with
  `-XX:AOTCache=target/arbace.aot` when the cache is newer than the jar. If the JVM rejects the
  cache (another JDK, other options) it runs without it; the launcher turns the JVM's `aot` and
  `cds` logging off, which would otherwise print the rejection on stdout. `ARBACE_CLASSPATH` is
  appended to the class path (the cache allows appending), `ARBACE_JAVA_OPTS` adds JVM options,
  `ARBACE_AOT=off` skips the cache, `ARBACE_JAR` names another jar.
- **The verifier** (added later the same day): `bin/build-arbace` runs
  `arbace.classes.verify` (the JDK's `ClassFile/verify`, see `doc/classes/COMPILER-NOTES.md`)
  on stages 1 and 2 with the stage's own runtime, and fails on any verify error or a class
  whose major version is not the running JDK's. `test/native/verify_test.clj` verifies the
  classes of a sample namespace compiled at run time (fns, `case`, `letfn`, `try`, deftype,
  defrecord, protocols, `reify`, `proxy`, `gen-class`, `gen-interface`, a multimethod, a
  `defclass`: 50 classes) and checks that a broken class is rejected.
- **The suite** (`bin/build-arbace --suite`) runs against the stages' own compiled namespaces,
  as upstream's runs against its build: `bin/clojure-tests` got `CLOJURE_TESTS_PRECOMPILED=1`,
  which skips its step 3 (compiling the namespaces into the run's `classes/`). Result on stages
  1 and 2: 83 namespaces, 809 tests, 20,718 / 20,750 assertions, test.generative 27 / 27, no
  regressions (before spec was vendored; below, "Spec").

Measured on 2026-10-07 (JDK 26.0.2.1, 64 cores shared with other agents, load average 2 to 7
during the runs; hyperfine, 3 warmups, 20 runs; noise about ±5 %):

| `-e 1` launch | mean | min |
|---|---:|---:|
| stage from sources (before: `-cp target/stage2:.`, no namespace classes) | 2.14 s | 2.04 s |
| `-cp target/stage2:.` with the compiled namespaces | 0.48 s | 0.46 s |
| `-cp target/arbace.jar` | 0.49 s | 0.46 s |
| the jar, `-XX:+UseCompactObjectHeaders` | 0.49 s | 0.46 s |
| the jar + AOT cache | 0.16 s | 0.15 s |
| the jar + AOT cache, compact headers | 0.16 s | 0.15 s |
| `bin/arbace -e 1` (the last, through bash) | 0.17 s | 0.16 s |
| frozen `clojure/` (`java -cp . clojure.main`) | 2.11 s | 1.95 s |

- First class form of a session (a `defclass` in a script, the `eval` timed): 255 ms before,
  about 200 ms from `-cp target/stage2:.`, about 55 ms from `bin/arbace` (the cache has
  `arbace.classes` loaded and linked). The whole script: 2.33 s → 0.78 s → 0.26 s.
- Compact object headers: no launch difference, and no throughput difference within the noise
  (`(into #{} (map #(update % :a inc) v))` over 2 million small maps, about 1.0 s either way),
  but the retained heap of those maps is 245 MB instead of 277 MB (-11 %). Hence the launcher
  default.
- `bin/build-arbace` without `--suite`: 82.8 s before, 82.5 s after (the namespace compile adds
  about 6 s per stage; stages 2 and 3, built by a stage with compiled namespaces, start faster;
  the jars and the training run add about 6 s). With `--suite`: 5 min 43 s.

### The runtime image (2026-10-07)

`doc/MODERN-COMPILER.md` §4.11 (the `jlink` half; Arbace stays on the class path, no JPMS
module): `bin/arbace-image`, also run by `bin/build-arbace --image`, makes
`target/arbace-image` from `target/arbace.jar` in about 14 s:

- **Modules**: `jdeps --print-module-deps` over the jar gives `java.base`, `java.desktop` (the
  `java.beans` of `bean`, Swing in `arbace.inspector` and `arbace.java.browse-ui`, AWT in
  `arbace.java.browse`), `java.sql` (`resultset-seq`, `java.sql.Timestamp` in `arbace.instant`)
  and `jdk.unsupported` (`sun.misc.Signal` in `arbace.repl`); `jlink` adds what they require
  (`java.xml`, `java.logging`, `java.datatransfer`, `java.prefs`, `java.transaction.xa`).
  `ARBACE_IMAGE_MODULES` adds more. `jlink --strip-debug --no-header-files --no-man-pages`.
- **Layout**: the image's `bin/arbace` is the repository's `bin/arbace`, which, when it finds
  `bin/java` and `lib/arbace/arbace.jar` beside it, runs that `java` and that jar (and
  `lib/arbace/arbace.aot`). The jar is copied unchanged.
- **The AOT cache** is trained by the image's own `java` (`bin/arbace --aot-train` inside the
  image, the same `test/aot-training.clj`), then the image is launched once with it. A JDK
  26.0.2 problem showed up here: with AOT class linking (the default of `-XX:AOTCacheOutput`),
  the cache of a `jlink` image of some module sets stops the JVM at startup ("Unexpected
  exception when loading aot-linked classes", `InternalError` from
  `ClassLoader.registerAsParallelCapable` in `ClassLoaders$AppClassLoader.<clinit>`). It
  reproduces with a hello-world class and `jlink --add-modules java.base,java.sql` (also
  `java.base,java.compiler`, and Arbace's set), not with `java.base` alone, `java.base,
  java.desktop`, all modules or the full JDK, nor with JDK 25's `jlink`. In the failing
  images the cache records `ClassLoaders$AppClassLoader` among the classes of the archived
  `ArchivedClassLoaders` subgraph, to be initialized at run time. Adding one of a dozen small
  modules (`jdk.unsupported.desktop`, `jdk.attach`, ...) avoids it, so the image adds
  `jdk.unsupported.desktop` (a few classes; it requires only `java.desktop`). As a safeguard,
  if the launch with the cache fails, the script trains again with `-XX:-AOTClassLinking`
  (classes still come from the cache, not linked; about 45 ms slower to launch) and writes
  that option to `lib/arbace/aot-train-options`, which `bin/arbace --aot-train` reads in the
  image.
- **Relocation**: the image runs from any directory and needs no JDK on the `PATH`
  (`env -i image/bin/arbace`). The cache still applies after a move: the JVM matches the
  recorded class path by its common prefix (`-Xlog:class+path`: "Longest common prefix
  substitution in boot/app classpath matching: yes").
- **Tested**: all native tests pass on the image (`ARBACE_CLASSPATH=test`, 30 tests).

Measured on 2026-10-07 (load average 50 to 160 from other agents; hyperfine, 5 warmups, 40
runs):

| | size | `-e 1` mean | min |
|---|---:|---:|---:|
| `bin/arbace` (the JDK, 401 MB, plus `target/arbace.jar` and `.aot`) | 444 MB | 179 ms | 158 ms |
| `target/arbace-image/bin/arbace` | 131 MB | 174 ms | 152 ms |
| the image, cache trained with `-XX:-AOTClassLinking` | 128 MB | 220 ms | 195 ms |
| the image without its cache (`ARBACE_AOT=off`) | | 562 ms | 520 ms |
| `bin/arbace` without the cache | | 508 ms | 475 ms |

The image's 131 MB: `lib/modules` 54 MB, `libjvm.so` 30 MB, the cache 35 MB, the jar 6.5 MB,
the rest 5 MB; 42 MB as a `.tar.gz`. Without `java.desktop` the JDK part would be 67 MB
instead of 89 MB, but `bean` and the inspector need it. Without the cache the image is slower
than the full JDK, which has its default CDS archive (`lib/server/classes.jsa`); `jlink
--generate-cds-archive` would add one, but the image always has its own cache.

### The runtime image: `java.base` and `jdk.unsupported` (2026-10-10)

By the user's decision (Arbace targets the server side), `bin/arbace-image` no longer takes
jdeps' module list: its modules are `java.base` and `jdk.unsupported` (`sun.misc.Signal` for the
REPL's interrupt handler), plus `ARBACE_IMAGE_MODULES`. Hand changes 14 to 16 made the rest
optional (`bean` without `java.beans`, `#inst` without `java.sql`, clear errors for
`arbace.xml`, `arbace.inspector` and `arbace.java.browse-ui`). With the new jar, jdeps finds
`java.desktop` only in `arbace.inspector` and `arbace.java.browse-ui`, `java.sql` only in
`resultset-seq` and the fns of `arbace/instant_timestamp.clj`, `java.xml` only in `arbace.xml`
and `arbace.lang.XMLHandler`. The AOT class-linking problem above does not occur with this set (the
cache keeps class linking, no extra module); it does with `java.base,java.sql,jdk.unsupported`,
where adding `jdk.attach` avoids it and the script's fallback still applies. Measured with the
development JDK, two rounds of 20 launches of `-e nil`: the image 133 → 100 MB (`lib/modules`
54 → 25 MB, `.tar.gz` 43 → 32 MB), with the cache 186-188 → 180-186 ms, without 613-626 →
607-622 ms. Details and the Alpine package: `doc/ALPINE.md`.

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
| stage 1, 2 with spec (below, "Spec") | 83 | 809 | 20,750 / 20,750 | 27 / 27 | none (against `test/arbace-results.edn`) |

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
   core as ``(namespace `ns)``).
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
  `stdin`, `tree DIR EXT...`, `vendor CONV SRC OUT`; later also `vendor-lib SRC OUT REPO REV`,
  below, "Spec"). The converter itself is unchanged.

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
  reference results, stay. (Spec has since been vendored and renamed: below, "Spec".)
- **System properties renamed** (`arbace.compile.path`, ...): they are Clojure's
  configuration names in the `clojure.` namespace of names.
- **Build output in `target/`** (already gitignored, as the vendored javac's build used it), one
  directory per stage.
- **A stage compiles from sources only** (`*from-source*`), rather than reflecting on its own
  loaded classes: the class environment is then the same at every stage, which is what makes
  stage 1 equal stage 2.

## After vendoring: hand changes

Changes applied by hand to `arbace/` after vendoring (so `bin/vendor-arbace`'s replay no longer
equals `arbace/` in these places). Items 1 and 2 are the user's decisions on the open points of
step 4 (2026-10-07); the others came with later work, as each says:

1. **The main class is `arbace.lang.Main`.** The class `clojure.main`, vendored as `arbace.main`
   in the single-segment package `arbace` (`arbace/main_class.clj`, with the package file
   `arbace.clj` at the repository root), moved into the package `arbace.lang`, next to
   `arbace.lang.Compile`, `Repl` and `Script`: `arbace/lang/Main.clj`, loaded by
   `arbace/lang.clj`. `arbace.clj` is gone; nothing remains at the repository root. The
   namespace `arbace.main` keeps its name. References updated: `Repl.clj` and `Script.clj`
   (`Main/legacy_repl`, `Main/legacy_script`), the usage texts of `arbace/main.clj`,
   `bin/build-arbace` (stage launcher, and the package list loses `arbace`), `bin/clojure-tests`
   (rename mode runs `arbace.lang.Main`), `CLAUDE.md`, `arbace/README.md` (now `doc/ARBACE.md`).
   Launch a stage with `java -cp target/stageN:. arbace.lang.Main`.
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
   local with a primitive initializer, `set!` of a local, `new` of an array class). Stage 1
   needs no `arbace.classes.boot`.
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

7. **The Clojure compiler emits the running JDK's class file version**
   (`doc/MODERN-COMPILER.md` §1 item 3, §4.9): `Compiler/JVM_BYTECODE_VERSION` in
   `arbace/lang/Compiler.clj` was `V17` (major 61, upstream `Compiler.java:347`); it is now
   `(unchecked-add-int 44 (.feature (Runtime/version)))`, so 70 on JDK 26, as
   `arbace.classes.emit/*version*`. The field is no longer a compile-time constant: it is set in
   `Compiler`'s `<clinit>`, and every user reads it at run time (`getstatic`): fn classes,
   deftype stubs and compile stubs (`Compiler.clj`), `gen-class` and `gen-interface`
   (`genclass.clj`), proxies (`core_proxy.clj`). No other classfile version constant is used
   outside `arbace/asm` (the vendored ASM knows up to `V26`). Frames are still ASM's
   `COMPUTE_FRAMES` with `getCommonSuperClass` answering `java/lang/Object`; the JDK's verifier
   accepts all classes at 70 as at 61 (no classfile feature or verifier rule changed between
   61 and 70). The AOT-compiled namespaces of each stage (4,276 classes) and everything compiled
   at run time now have major version 70; the fixpoint holds as before (5,091 classes).
8. **(Reverted.)** `Atom` briefly held its state in a volatile field updated through a
   `VarHandle` (`298ad15`): 16 bytes less per atom and slightly faster uncontended, but one atom
   swapped by 8 threads became about 25% slower. At the user's decision it was reverted, so
   `Atom` keeps upstream's `final AtomicReference state`.

9. **An opt-in virtual-thread executor for `send-off`, `future` and `pmap`**
   (`doc/MODERN-COMPILER.md` §4.12): in `arbace/lang/Agent.clj`, `soloExecutor` (the executor of
   `send-off`, and through `future-call` of `future`, `pmap`, `pcalls` and `pvalues`) is made by
   the new private `createSoloExecutor`: upstream's cached pool of platform threads named
   `arbace-agent-send-off-pool-N`, or, when the system property `arbace.virtual-threads` is
   `true` at class initialization, `Agent.newVirtualThreadExecutor()`. That new public static
   method returns `Executors.newThreadPerTaskExecutor` over virtual threads named
   `arbace-agent-send-off-virtual-N`; `(set-agent-send-off-executor!
   (arbace.lang.Agent/newVirtualThreadExecutor))` switches at run time. `send` keeps its fixed
   pool of platform threads (CPU-bound actions gain nothing). The default is unchanged.
   Behaviour with virtual threads (tested in `test/native/virtual_threads_test.clj`, in process
   and in a forked JVM with the property): binding conveyance (`binding-conveyor-fn`) and the
   sends an action makes (held until it ends) work as before; `shutdown-agents` shuts the
   executor down and later `future` calls throw `RejectedExecutionException`, as with the pool.
   The difference: virtual threads are daemon threads, so the JVM exits when the main thread
   ends, without waiting for running futures or `send-off` actions, and without the cached
   pool's 60 seconds of idle threads keeping it alive when `shutdown-agents` is not called
   (`bin/arbace -e '@(future 1)'` takes 60.2 s by default, 0.2 s with the property). Measured
   (jar, `-XX:+UseCompactObjectHeaders`, five rounds in one JVM, first and last round):

   | workload | platform pool | virtual threads |
   |---|---|---|
   | 10k futures sleeping 10 ms | 243 → 88 ms, 1,640 threads, 338 MB RSS | 159 → 37 ms, 72 threads, 201 MB |
   | 10k futures sleeping 100 ms | 653 → 231 ms, 5,501 threads, 814 MB | 249 → 120 ms, 72 threads, 238 MB |
   | 100k futures sleeping 10 ms | 904 → 477 ms, 2,249 threads, 569 MB | 587 → 152 ms, 72 threads, 770 MB |
   | 10k futures sleeping 1 s | 2,501 → 1,064 ms, 10,006 threads, 1.36 GB | 1,149 → 1,014 ms, 72 threads, 207 MB |
   | `pmap` of 20k CPU-bound items (sum of 20k) | 522–660 ms | 467–529 ms |

   (thread counts are the JVM's peak of platform threads; the virtual threads run on the
   carrier pool.) The JDK AOT cache still applies with the property set.

10. **Reflective calls are `invokedynamic` inline caches** (`doc/MODERN-COMPILER.md` §2.2, §4.1,
    §4.8). The Clojure compiler emitted a call of `Reflector` for each call it could not resolve,
    and `Reflector` searched the class's methods on every call. Now:
    - `arbace/lang/Compiler.clj`: the unresolved emissions of `InstanceMethodExpr` (upstream
      `Compiler.java` `invokeInstanceMethod`/`invokeInstanceMethodOfClass`), `InstanceFieldExpr`
      (`invokeNoArgInstanceMember`, `requireField` or not), `StaticMethodExpr`
      (`invokeStaticMethod`) and `NewExpr` (`invokeConstructor`) emit the target and arguments as
      before (each as an `Object`, in the same order), then `invokedynamic "invoke"
      (Object...)Object` with the bootstrap `ReflectorCallSite.bootstrap` and the static arguments
      member name, class name (the qualifying class, or the class of a static method or
      constructor; `""` for none) and kind (`METHOD`, `MEMBER`, `FIELD`, `STATIC`, `NEW`). The
      new `Compiler/emitReflectorSite` and `Compiler/isSystemClass` do it. The old emission stays
      for more than 20 arguments, for a static method named `new`, and when the class named in
      the site is not of the boot or platform loader (a user class: its name can denote a newer
      class after a REPL redefinition, which `RT.classForName` per call finds and a cached class
      would not); unqualified instance calls are guarded on the receiver's class and need no
      name. Field assignment (`setInstanceField`) keeps `Reflector`: it is rare, and a cached
      setter would be the place where JEP 500's final-field rules bite. Reflection warnings are
      made at analysis, unchanged; `eval` of top-level forms uses `Reflector` as before.
    - `arbace/lang/ReflectorCallSite.clj` (new, Arbace's own class form): a `MutableCallSite`
      per site. Its fallback goes through `Reflector`'s unchanged path for the first call of a
      site (a site run once never links), and from the second call on it first links an entry:
      it finds the member `Reflector` would call with `Reflector`'s own code, unreflects it from
      its own lookup (in `arbace.lang`, as `Reflector`, so the same access rules and the same
      caller for caller-sensitive methods), and adapts it with `Reflector`'s argument conversion
      (a `boxArg` filter, after `widenBoxedArgs` where `Reflector` widened; a plain cast or unboxing
      where the guarded argument class makes `boxArg` one) and return conversion (`prepRet` for a
      `Boolean` result). The entry is guarded on the receiver's class, and on the argument
      classes (nil as `Void`) when there was more than one candidate, so the choice, which then
      depends on them, is `Reflector`'s for every call it takes. Entries are prepended to the
      site's target as `guardWithTest` (8 at most); past that the site is megamorphic and its
      fallback scans the entries (64 at most), then goes through `Reflector`. The call that links
      still goes through `Reflector`. Whatever is not cached goes through `Reflector` too, which
      so throws its own exceptions with its own messages: a nil receiver, a call `Reflector`
      refuses, a member a method handle cannot reach (after 8 failed links a site stops
      trying). Exceptions of the called member pass through unwrapped, as `Reflector` unwraps
      `InvocationTargetException`; an exception of `boxArg` is unwrapped as `Reflector` does.
      No field is written through it (JEP 500). A site keeps the classes of its entries
      reachable, as a protocol call site's `__cached_class__` field does: a site in a long-lived
      class that saw a REPL-defined receiver class keeps that class's loader alive.
    - `arbace/lang/Reflector.clj`: the selection is split from the invocation, unchanged, so
      both paths share it: `selectMatchingMethod` (what `invokeMatchingMethod` chose and checked;
      it returns the widened arguments through a one-element array), `instanceMethods` (the
      candidates of `invokeInstanceMethodOfClass`), `selectConstructor` and `constructors` (of
      `invokeConstructor`). The public methods behave as before.
    - `test/native/reflect_test.clj` compares each kind of site, on its first calls and cached,
      with `Reflector`'s results and exceptions: mono-, poly- and megamorphic receivers (80
      classes), threads, overloads chosen by runtime argument types (widened `Integer`, `Short`,
      `Byte`, `Float`; `Ratio`, `BigInt`, nil), nil receivers and arguments, error messages, fields
      and no-argument members, canonical `Boolean`s, functional-interface adaptation, qualified
      calls, static methods and constructors; and checks by the stack (no `Method.invoke` frame)
      that cached calls bypass `Reflector`. `test/aot-training.clj` runs a few reflective sites,
      so the AOT cache holds what they load.

    Measured (JDK 26, one JVM per row, after warm-up, best of five runs of 10⁶-2·10⁶ calls, the
    arguments un-hinted):

    | call | `Reflector` | call site |
    |---|---:|---:|
    | `(.length s)` (a member, monomorphic) | 1,053 ns | 2.0 ns |
    | `(.get m k)` (`HashMap`) | 492 ns | 8 ns |
    | `(.append sb x)` (overloads, `Long`) | 1,972 ns | 14-17 ns |
    | `(.indexOf s "c" 1)` (overloads) | 1,443 ns | 7 ns |
    | `(.size c)`, 2 / 4 receiver classes | 521 / 611 ns | 6 / 6 ns |
    | `(.size c)`, 10 classes (megamorphic) | 690 ns | 14-21 ns |
    | `(.contains c x)`, 3 classes | 920 ns | 6 ns |
    | `(Math/abs x)`, `(Math/max x y)` | 2,041 / 2,123 ns | 1.2 / 3.3 ns |
    | `(ArrayList. x)`, `(StringBuilder. x)` | 427 / 465 ns | 9 / 12 ns |

    The cost is once per site. A site's first call (the JVM's linkage of the `invokedynamic` and
    the bootstrap, then `Reflector`) and its second (the link) cost more than `Reflector`'s
    calls; from the third call on it is cheaper. In a cold JVM (the jar with its AOT cache, 300
    fresh sites, each called thrice): 108 / 91 / 3 µs per site against 64 / 20 / 12 µs; with the
    JIT warm, 8-20 / 5-28 / 1 µs against 3.5-4 / 2 / 2 µs. So a site called fewer than about 20
    times in a cold JVM costs more than before, by at most about 0.1 ms; a script of 300
    reflective sites each run once took 1.07 → 1.18 s, each run 100 times 1.52 → 1.50 s.
    Startup is unchanged (`bin/arbace -e '(+ 1 2)'` 213 ms → 214 ms, the AOT training session
    802 → 779 ms): no reflective site runs at startup. The JDK AOT cache does not pre-resolve
    custom bootstraps, so every launch pays the linkage again. The stages hold 31 classes with
    such sites (`ReflectorCallSite` itself not counted).

11. **`SecurityManager` is no longer a default import** (`doc/MODERN-COMPILER.md` §4.17): the
    entry `SecurityManager` of `RT/DEFAULT_IMPORTS` (`arbace/lang/RT.clj`, upstream
    `RT.java:119`) is gone, since JEP 486 (Java 24) disabled the Security Manager for good.
    Upstream still imports it (`clojure/clojure` master `4278bcea`, 2026-10-07); nothing in
    `arbace/`, its tools or Clojure's test suite names the class unqualified. The class still
    exists in JDK 26, so `java.lang.SecurityManager` resolves; the class forms compiler's own
    `java.lang` fallback (SPEC §9.1) is unaffected. Test: `test/native/imports_test.clj`.

12. **Keyword invoke sites are `invokedynamic`** (`doc/MODERN-COMPILER.md` §4.1; 2026-10-07):
    `Compiler$KeywordInvokeExpr/emit` (`arbace/lang/Compiler.clj`) emitted Clojure's hand-written
    inline cache, a `__site__N` (`KeywordLookupSite`) and a `__thunk__N` (`ILookupThunk`) static
    field per site, set in `<clinit>`, a call of the thunk, an identity test for a miss, and on a
    miss `fault` and a store of the new thunk (upstream `Compiler.java:3825-3847`). It now emits
    the target and `invokedynamic invoke (Object)Object`, bootstrapped by
    `arbace.lang.KeywordInvokeSite/bootstrap` with the keyword's namespace (if any) and name as
    static arguments. The fields, their `<clinit>` code, `registerKeywordCallsite`,
    `ObjExpr/emitKeywordCallsites` and the site/thunk name helpers are gone;
    `KEYWORD_CALLSITES` stays bound as before (it marks code inside a fn, where keyword invokes
    are sites). The new class `arbace/lang/KeywordInvokeSite.clj` (Arbace's own) holds a
    `MutableCallSite` whose value is always `(get target :k)`: for its first 256 calls it calls
    `RT.get`; then it links an inline cache of up to four class guards (`guardWithTest` on the
    exact class of the target), each bound to what `KeywordLookupSite` would cache for that
    class: a record's `IKeywordLookup` thunk (its field read), `ILookup.valAt`, or `RT.get`. At a
    fifth class it becomes `RT.get` for good; `nil` is never cached. `KeywordLookupSite`,
    `ILookupSite` and `ILookupThunk` stay (records implement `getLookupThunk`, and classes
    compiled elsewhere may use them). The warm-up threshold keeps launches from paying for
    method handles: the JDK spins LambdaForm classes for the first `guardWithTest`,
    `insertArguments` and bootstrap invocation, so linking at the first call made 8 more spun
    classes at `bin/arbace -e 1` (9 against 1) and the launch 5-8% slower; with the threshold, 1
    as before. Measured (2026-10-07, loaded 64-core machine): AOT-compiled namespaces 9,449,063
    → 8,822,445 bytes (-6.6%), static fields 20,568 → 14,432, 1,979 sites; `bin/arbace -e 1`
    median 301 ms → 301 ms with the AOT cache, 717 → 696 ms without (noise ±15 ms); loading 15
    test namespaces from source unchanged (5.44 s → 5.54 s median, noise ±0.3 s); a lookup
    loop (`(:a x)` over 1,000 values, min of 7) records 7.8 → 5.1 ns, array maps 10.6 → 7.3 ns,
    hash maps 11.4 → 8.2 ns, six classes (records, maps, `java.util.HashMap`, `nil`) at one site
    18 → 11.5 ns.

13. **`str` with two or more arguments is `StringConcatFactory`** (`doc/MODERN-COMPILER.md`
    §4.7; 2026-10-07): `Compiler$InvokeExpr/parse` (`arbace/lang/Compiler.clj`) turns a call of
    `#'arbace.core/str` with 2 to 99 arguments, inside a fn, into the new
    `Compiler$StrConcatExpr`, an intrinsic like an `:inline` (a redefinition of `str` does not
    reach it, as with other inlined fns). It emits `invokedynamic makeConcatWithConstants`
    (JEP 280) with exactly `str`'s result: constants whose text is fixed (strings, characters,
    booleans, longs, doubles, keywords, `nil`) are folded into the recipe (passed as recipe
    constants when they hold `\u0001` or `\u0002`); primitive arguments are passed as such (the
    factory prints them as `toString` of their box does); any other argument is converted
    inline: `nil` to `""`, else its `toString`. A `null` from `toString` gives `"null"` (as
    `StringBuilder.append` does in `str`), except for the first argument, where `str`'s
    `new StringBuilder` throws, and the intrinsic throws the same `NullPointerException` from
    the same constructor. The arguments are evaluated in order before any conversion, as for a
    call: when an argument with effects (anything but a folded constant or an immutable local) follows
    an object argument, every argument goes through a temporary local first (reserved below the
    arguments' own locals, cleared after use). All-constant calls make `new String(text)`, so
    each evaluation still gives a new string. `eval` (top level, outside a fn) calls `str`.
    Stack traces of an exception from a `toString` lack the `arbace.core$str` frame. Measured
    (2026-10-07, machine under heavy load): 515 sites in the AOT-compiled namespaces, classes
    8,822,445 → 8,989,918 bytes (+1.9%: recipes and bootstrap entries); calls 1.5 to 2.5 times
    faster (min of 7 rounds: `(str "n=" x)` 105-119 → 66-69 ns, five arguments 186-260 → 89-134
    ns, with a `nil` 132-199 → 60-94 ns, with a primitive 162-199 → 68-108 ns); `bin/arbace -e
    1` unchanged (median 393 → 391 ms with the AOT cache, which pre-resolves these sites; 833 →
    842 ms without, noise ±20 ms).

14. **`bean` without `java.beans`** (2026-10-10, the user's decision that Arbace's runtime
    images hold `java.base` and `jdk.unsupported` only; `java.beans` is in `java.desktop`):
    `bean` (`arbace/core_proxy.clj`) found the properties with
    `java.beans.Introspector/getBeanInfo`; it now finds them by reflection, as the Introspector
    does when no explicit `BeanInfo` exists, in the new private fns `bean-decapitalize`
    (`Introspector/decapitalize`), `bean-accessible-method` (`com.sun.beans.finder.MethodFinder/
    findAccessibleMethod`: for a class that is not public, the public interface method a getter
    implements), `bean-class-methods` (`com.sun.beans.introspect.MethodInfo/get`: the public
    methods a class declares, then its interfaces' default methods, `AutoCloseable`, `Cloneable`,
    `Closeable` and `Comparable` ignored, in MethodInfo's order), `bean-class-reads`
    (`PropertyInfo/get`: `isX()` returning `boolean` first, else the most specific `getX()`,
    default methods not replacing; static methods ignored) and `bean-properties` (Introspector's
    merge down the superclass chain: a subclass's getter replaces its superclass's, except an
    `isX()` of another name). Only getters without parameters are considered, the only ones
    `bean` ever used; the value conversion (`Reflector/prepRet` on the getter's type) is
    unchanged. Ported from main's Go build (`arbace/lang/go/ns/core_proxy.clj`, whose simpler
    version took `getMethods` as a whole) and extended to the Introspector's rules above. Checked
    against the Introspector on the full JDK (`.tmp/bean/beancmp.clj`, the read method of every
    property compared): 77 classes, 583 properties (JDK values: strings, numbers, files, dates,
    collections and their non-public iterators and views, threads, URIs, URLs, `java.time`,
    NIO buffers and paths, charsets, process handles, calendars, patterns, executors, futures,
    MXBeans, loggers, reflection objects, exceptions; Arbace's: vectors, maps, atoms, agents,
    refs, vars, namespaces, symbols, keywords, lazy seqs, ranges, fns, a record, a deftype, a
    proxy, a reify): equal for 76; the one difference is `javax.swing.JLabel`, whose superclass
    `java.awt.Component` has an explicit `BeanInfo` (`com.sun.beans.infos.ComponentBeanInfo`)
    that limits the Introspector to a few of Component's properties; `bean` now lists all of
    them. Explicit `BeanInfo` classes and `@JavaBean` annotations are not consulted; outside
    `java.desktop` the JDK has none. Since v3, the properties come sorted by name, as the Introspector returns them,
    so that `bean`'s map keeps upstream's key order (v3 inserted them unsorted: e.g. the keys of
    `(bean (java.net.URI. "http://a/b"))` came in hash order).

15. **`#inst` on `java.base`; `java.sql.Timestamp` optional** (2026-10-10, same decision):
    `core.clj` loaded `instant.clj` only when `java.sql.Timestamp` exists, and
    `default-data-readers` held `'inst` only then, so without `java.sql` there was no `#inst`.
    Now `instant.clj` is always loaded and `'inst` is always `read-instant-date`; the parts that
    need `java.sql` (`print-method` and `print-dup` of `java.sql.Timestamp`, its formatter,
    `construct-timestamp` and `read-instant-timestamp`) moved to the new file
    `arbace/instant_timestamp.clj` (`(in-ns 'arbace.instant)`, so the vars keep their names),
    which `instant.clj` loads when `java.sql.Timestamp` exists. On a full JDK everything is as
    before. `resultset-seq`'s `^java.sql.ResultSet` hint needs no change: the class is resolved
    only when `resultset-seq` runs (checked on a `java.base` image).

16. **Clear errors for the namespaces that need an optional module** (2026-10-10, same
    decision): `arbace.xml` (`java.xml`), `arbace.inspector` and `arbace.java.browse-ui`
    (`java.desktop`) start with a check, before their `ns` form, that throws an
    `UnsupportedOperationException` naming the module and `ARBACE_IMAGE_MODULES` when the
    runtime lacks it, instead of a `NoClassDefFoundError` from deep in the namespace's
    initialization. `arbace.java.browse/browse-url` already did without `java.awt.Desktop`
    (reflection, `ClassNotFoundException` caught); it reaches `arbace.java.browse-ui`, and so
    the error, only when there is no `xdg-open` either.

17. **`reduce` without an init on an `IReduceInit` that is not an `IReduce`** (2026-10-10):
    `(reduce + (eduction (map inc) (range 10)))` threw `ClassCastException: Eduction cannot be
    cast to IReduce` in some runs and returned 55 in others. `reduce` without an init goes to
    `coll-reduce` (`arbace/core/protocols.clj`) for anything not an `IReduce`, and
    `CollReduce`'s implementation for `IReduceInit` cast its argument to `IReduce` in its
    two-argument arity, as upstream's does (`clojure/core/protocols.clj`, also in Clojure
    1.12.6). An `Eduction` is an `IReduceInit`, an `Iterable` and a `Sequential`; when no class
    on its superclass chain has an implementation, `find-protocol-impl` picks among the matching
    interfaces with `pref` over the set `(supers c)`, a hash set of classes ordered by their
    identity hashes, so `Iterable` (the iterator, which works) or `IReduceInit` (the cast,
    which throws) won by chance: with the JDK's CDS or AOT archive the classes' hashes come
    from the archive and are stable, without it they follow the run (upstream Clojure 1.12.6
    returns 55 with its default CDS archive and throws with `-Xshare:off`; Arbace's jar returned
    55 with `target/arbace.aot` and threw without it, as on the seed). `iteration` (a `reify`
    of `IReduceInit` and `Seqable` only) threw every time, upstream too. The two-argument arity
    now calls `IReduce.reduce` only on an `IReduce`, and otherwise reduces through
    `IReduceInit.reduce` from a fresh sentinel object standing for no value yet: the first item
    replaces it, `f` sees the first two items first, an empty reducible gives `(f)` and a single
    item is returned without calling `f`, as `reduce` without an init promises; `reduced`
    stops it as before. So the result no longer depends on which implementation the hashes
    pick; the choice itself (`find-protocol-impl`, upstream's) is unchanged. Regression test:
    `test/native/reduce_test.clj` (run by the bootstrap on stage 1). (Main: hand change 18.)

    **A dialect divergence, not a bug fix** (recorded 2026-10-10, the user's decision): upstream
    does not support `reduce` without an init on an `IReduceInit` that is not an `IReduce`, by
    design (Alex Miller on clojure-dev, 2017: the `ClassCastException` is "your clue that you
    should implement IReduce or use an init value"; ask.clojure.org #11138, 2021: "eduction has
    always been implemented only with IReduceInit"). The run-to-run difference is upstream's
    CLJ-2656, "Protocol dispatch via interfaces is nondeterministic" (open; its patch, sorting
    the interfaces by name, would make the eduction case always throw). Arbace gives `reduce`
    without an init its documented meaning on every reducible: code relying on it works on
    Arbace from v4 and not reliably on Clojure. Listed under "Dialect divergences" below.

## Dialect divergences

Where Arbace behaves differently from upstream Clojure on purpose, beyond the renaming
(`clojure.*` → `arbace.*`) and Java 26: code written for one may not behave the same on the
other.

- **`reduce` without an init on an `IReduceInit`-only reducible** (hand change 17, from v4):
  Arbace reduces it, seeded with the first item; upstream throws `ClassCastException` by
  design, or, for `eduction`, works or throws depending on the JVM's class archive (CLJ-2656).

## Spec (2026-10-07)

The seed stubbed clojure.spec out of the baseline (journal, 2026-10-06), so 32 assertions of
the suite, which expect spec's messages, failed on the baseline and on the stages. Arbace now
vendors spec and behaves as upstream Clojure with it: macro calls are checked against
core.specs at macroexpansion, `ex-triage` and the REPL print spec's explanations, `doc` shows
specs. The frozen `clojure/` keeps the stub.

**Vendored** (`bin/vendor-spec`, which replays it into `.tmp/vendor-spec/`): the versions
upstream Clojure `98d735fab02f` depends on in its `pom.xml`, from their repositories at those
tags (the `.clj` sources equal those in the Maven jars, checked with `cmp`):

| library | tag (commit) | files | vendored as |
|---|---|---|---|
| https://github.com/clojure/spec.alpha | `v0.6.249` (`3d1efb353b8a95c699b4051ba8273568c21f874e`) | `src/main/clojure/clojure/spec/alpha.clj`, `gen/alpha.clj`, `test/alpha.clj` | `arbace/spec/alpha.clj`, `gen/alpha.clj`, `test/alpha.clj`: `arbace.spec.alpha`, `arbace.spec.gen.alpha`, `arbace.spec.test.alpha` |
| https://github.com/clojure/core.specs.alpha | `v0.6.133-alpha10` (`75875946b7e827a1cec91ce645d5e1dcfc475e49`) | `src/main/clojure/clojure/core/specs/alpha.clj` | `arbace/core/specs/alpha.clj`: `arbace.core.specs.alpha` |

Both are EPL-1.0 (`LICENSE.md` says which files). `arbace.j2c.rename/vendor-lib!` renames path
and text and adds an origin comment after each file's notice. The renaming rules gained
`clojure.spec` as a prefix (`clojure.spec.alpha`, `.gen.alpha`, `.test.alpha`,
`:clojure.spec.alpha/problems`, `clojure/spec/alpha`, the system properties
`clojure.spec.check-asserts`, `clojure.spec.skip-macros`, `clojure.spec.compile-asserts`) and
the namespace `clojure.core.specs.alpha`; the suite is renamed with the same rules, so it
expects `arbace.spec.alpha`'s keywords and `arbace.core/let`'s spec errors. `clojure.test.check`
(spec's optional generator library, loaded lazily by `arbace.spec.gen.alpha` as upstream) keeps
its name.

**One hand edit** in the vendored files: `arbace/spec/gen/alpha.clj` excludes `return` from
`arbace.core` (`:refer-clojure :exclude`), since `arbace.core` has the class form `return` and
`gen/return` would replace it with a warning on every load. (test.check's
`clojure.test.check.generators` gives the same warning; it is not vendored and stays as it is.)

**Un-stubbed** (upstream's code, renamed; compare upstream `98d735fab02f` with `clojure/`):

- `arbace/main.clj`: `(:require [arbace.spec.alpha :as spec])`; the REPL binds
  `arbace.spec.alpha/*explain-out*`; `ex-str` prints the explanation of a spec error
  (`with-out-str` of `spec/explain-out`) for macro syntax errors and instrumented calls; the
  core namespace list names `arbace.spec.*`.
- `arbace/repl.clj`: `(:require [arbace.spec.alpha :as spec])`; `doc` prints a fn's spec
  (`spec/get-spec`) and documents a spec given a keyword (`spec/describe`).
- `arbace/lang/RT.clj`: `instrumentMacros` is `(not (Boolean/getBoolean
  "arbace.spec.skip-macros"))`, not `false`; `checkSpecAsserts` reads
  `arbace.spec.check-asserts`.
- `arbace/lang/Compiler.clj`: `ensureMacroCheck` loads `arbace/spec/alpha` and
  `arbace/core/specs/alpha` and finds `arbace.spec.alpha/macroexpand-check`; `SPEC_PROBLEMS` is
  `:arbace.spec.alpha/problems` (these strings had been left unrenamed).
- `arbace/lang/Compile.clj`: `(RT/load "arbace/core/specs/alpha")` before compiling, as upstream
  `Compile.java` ("force load to avoid transitive compilation during lazy load"; one of the two
  seed fixes had dropped it from `clojure/`).

**Build** (`bin/build-arbace`): after the stage's namespaces, a second `arbace.lang.Compile`
run compiles `arbace.spec.alpha`, `arbace.spec.gen.alpha`, `arbace.spec.test.alpha` with
`-Darbace.spec.skip-macros=true`, as spec.alpha's own `pom.xml` compiles its jar, and then
`arbace.core.specs.alpha`, which upstream ships as source: compiled, the first macro call of a
session costs about 40 ms instead of loading it from source. A separate run because `Compile`
loads core.specs (and so spec) before compiling; compiling spec in that JVM would reload it and
redefine its protocols under the specs core.specs has registered. The main run so compiles the
core namespaces with macro checking on, as upstream's build does. Each stage: 5,625 classes
(815 class-form classes, 4,810 namespace classes, 534 of them spec's), stages 1, 2 and 3
byte-identical, all verified; the jar has 5,890 entries, the AOT cache is 39 MB.
`bin/arbace -e 1` launches as before (about 0.18 s, the time within noise; `arbace.main` now
loads `arbace.spec.alpha`, as upstream's does); `-e "(let [a 1] a)"`, the first macro, takes
about 40 ms more (core.specs and the check).

Running a stage after editing `arbace/spec/**` or `arbace/core/specs/alpha.clj` without
rebuilding loads the edited namespace from source with macro checking on, which can trip
`Cyclic load dependency` (spec's `ns` form triggers the check, which loads core.specs, which
requires spec): rebuild, or run with `-Darbace.spec.skip-macros=true`. Upstream has the same
property; it ships spec compiled.

**The suite**: `test/arbace-results.edn` is the reference for Arbace's stages, and
`bin/clojure-tests` uses it by default in rename mode (`CLOJURE_TESTS_RENAME=arbace`): all
passing, 83 namespaces, 809 tests, 20,750 / 20,750 assertions, test.generative 27 / 27, as
upstream's control run. `test/baseline-results.edn` stays the frozen baseline's reference (the
default run of `bin/clojure-tests`), with its 32 spec failures. Result on stages 1 and 2:
20,750 / 20,750, no regressions. spec.alpha's own tests (its `src/test/clojure`, renamed, with
the renamed test.check 1.1.3) pass on stage 2: 13 tests, 174 assertions.

## Open decisions for the user

1. (decided, above) `arbace.clj` and the class `arbace.main`.
2. (decided, above) the leftover `clojure` identifiers.
3. (done, `9cfc9b9`) `CLAUDE.md`'s layout section names the vendored tree and
   `bin/build-arbace`.
4. (done, item 3 of "After vendoring") the class forms native at stage 1.
