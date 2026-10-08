# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

## 2026-10-06: Repository bootstrap

- The repo started from GitHub's profile-README template (`arbace/arbace`). Its `.gitignore` is
  GitHub's Leiningen template.
- Added `.claude/` (local Claude Code settings) and `.tmp/` (scratch space for temporaries) to
  `.gitignore`. Both patterns are unanchored. Created `.tmp/`.
- Defined the project: Arbace is a from-scratch reimplementation of Clojure in Clojure. Studying and
  vendoring prior art is always allowed. The work is a derivation done once by hand, and this journal
  exists so it could be repeated.
- Prior art:
  - https://github.com/clojure/clojure for the language.
  - https://github.com/openjdk/jdk26u for the native runtime.
  - https://github.com/golang/go as a slimmer, appealing runtime and anything else useful from
    it, except the Go language itself.
- Replaced the template README.md. Added CLAUDE.md, doc/AGENDA.md (the current state, rewritten
  freely) and doc/JOURNAL.md (this file, append-only).

## 2026-10-06: Seed from Clojure and ASM sources (SEED.bash)

- Added `SEED.bash`, which vendors upstream sources into a single tree, `clojure/`, compiles them in
  place, and smoke-tests the result. Its steps:
  - Shallow-clone https://gitlab.ow2.org/asm/asm and https://github.com/clojure/clojure into `_/`.
    The script does not pin revisions. The seed was taken at these revisions:
    - asm `0460f74ba64230e5b846967c81a52628b5dd0596`
    - clojure `98d735fab02f337cee654cb0629bddc09883a75a` (`1.13.0-master-SNAPSHOT`)
  - Take all of `asm/src/main/java/org/objectweb/asm`, plus `GeneratorAdapter`,
    `InstructionAdapter`, `LocalVariablesSorter`, `Method` and `TableSwitchGenerator` from
    asm-commons. Rename the package `org.objectweb` to `clojure` (giving `clojure.asm`), which
    replaces Clojure's own repackaged copy.
  - Take Clojure's `src/jvm` Java sources, except its bundled `clojure/asm`, and its `src/clj`
    sources, except `clojure/parallel`. Write `clojure/version.properties` from `pom.xml`.
  - Normalize the files: squeeze blank lines, retab to width 2, strip trailing whitespace.
  - Stub out clojure.spec, which is a separate library: comment out the `[clojure.spec.alpha :as
    spec]` require and the spec checks in macroexpansion, the `*explain-out*` use and the doc
    `describe` call. Replace the `clojure.spec.skip-macros` check with `false`.
  - Run `javac -g` over the tree. The result runs `clojure.main`:
    `(reduce + (map inc [1 2 3]))` evaluates to 9.
- The result is 183 `.java` files and 48 `.clj` files under `clojure/`. The compiled `.class`
  files are already gitignored.
- Licenses: Clojure is EPL-1.0 and ASM is BSD-3-Clause (INRIA, France Telecom).

## 2026-10-06: Freeze clojure/, drop SEED.bash, add LICENSE.md

- `clojure/` is the frozen reference baseline for the rewrite and is never modified. The rewrite
  lives outside it.
- `SEED.bash` was committed together with `clojure/` in `d21dc91` and then deleted from the
  worktree because it is fragile. It is kept only in git history. The upstream revisions are pinned
  in the previous entry.
- Added `LICENSE.md`, which concatenates the vendored licenses:
  - Clojure's EPL-1.0, converted to Markdown from `epl-v10.html` at clojure `98d735fab02f`.
  - ASM's BSD-3-Clause, verbatim from `LICENSE.txt` at asm `0460f74ba642`.

## 2026-10-06: javalisp, Java as Clojure-readable s-expressions

- Goal, set by the user: a Clojure tool under `arbace/` that transcribes the baseline's Java
  sources (any Java 26 syntax) into a form the Clojure reader accepts as data. Later, javac
  will be vendored and given a second parser that reads this form and compiles the same
  classes byte for byte. The inverse (javalisp → Java) was asked for as a bonus.
- Precedent studied: go-lisp, the same idea for Go, at `../go-lisp` commit `2483a496a80b`
  (`golisp/DESIGN.md`, `SPEC.md`, `README.md`). Its approach carried over: the same tree
  through another syntax, keyword heads for forms without a language keyword, a corpus
  round-trip test, then a compiler front end.
- Decisions taken with the user:
  - Java words, Lisp shape (go-lisp style), not Clojure idioms (`defn`, `^hints`, `.method`).
  - The text must be an EDN subset readable by both `clojure.edn` and `clojure.core`.
  - A line-preserving layout, so the class files' line numbers can be reproduced.
  - Keep all comments.
- Implementation: `arbace/javalisp/`, about 2,400 lines of Clojure run on the vendored
  `clojure/` by `bin/javalisp`. The forms are specified in `doc/javalisp/SPEC.md`. Parsing uses
  the running JDK's own javac (26.0.2.1, built from jdk26u `baf63fbe42b8`) through
  `jdk.compiler` internals.
- Findings that shaped the design. Each was checked in jdk26u's `jdk.compiler` sources:
  - Lines in class files come from node positions, not node starts (`Gen.statBegin`):
    - a statement's position
    - a call's `(`
    - a binary's operator, used for loop and `?:` conditions
    - a block's and a switch's `}` (`bracePos`), used for implicit returns and scope ends
    - a selection's position, which is its dot

    So every node position is kept as a line. When a default rule cannot place a node on its
    line, a marker does: the operator repeated in infix position, `.`, or a detached closing
    paren.
  - javac's parser folds a `-` into a following decimal int or long literal, but not into
    one that starts with 0, nor into hex or floating literals (`JavacParser.term3`). It folds
    adjacent string literals only when the `+` chain is the root of its binary expression
    (`foldStrings`).
  - Parentheses are semantic: `Attr.makeNullCheck` skips the null check only when the tree's
    tag is `NEWCLASS`, so `(new C())::m` and `new C()::m` compile differently. javalisp
    therefore keeps every `JCParens`. Parentheses Java needs are implicit, by one rule shared
    by both directions (`prec.clj`); the redundant ones become `(:paren x)`.
  - A same-line doc comment (`/** @deprecated */ void f()`, even on a parameter) sets the
    deprecated flag. Such comments are written inline as `#_"..."` discards.
  - Enum constant positions follow `enumeratorDeclaration`: the creation is at the token after
    the name. Declarators of one declaration (`int a, b[];`) share a modifiers tree and stay
    one form.
  - For a few lambda-heavy classes javac's own output is not deterministic: identity-hash
    names like `$1522236047`. The byte comparison skips the classes that differ between two
    compiles of the unchanged source.
- Spellings forced by the readers: `bit-xor`, `bit-not`, `bit-xor=` and `div=` for `^`, `~`,
  `^=` and `/=`; `(:ann A)` for `@A`; `(:ref T m)` for `T::m`; `(long 5)` and `(float 1.5)`
  for the suffixed literals; `(char 0xD800)` for surrogate characters.
- Verification. `check` parses, transcribes, reads the result with both Clojure readers,
  prints Java back, re-parses it, compares the two javac trees strictly (parentheses and
  lines included), and requires that transcribing again gives the same text.
  - `clojure/`: 183 of 183 files.
  - jdk26u `src/`: 14,202 of 14,202 files.
  - jdk26u `test/`: 37,582 of the 37,584 files javac parses. The other 213 are negative tests.
  - Class bytes, compiled with `-g`: `clojure/` gives 812 of 812 identical classes. Of
    jdk26u `test/`, 12,411 files compile on their own, giving 27,987 identical classes.
- Known limits, both javac edge-case tests:
  - comment delimiters written as unicode escapes (`unicode/UnicodeCommentDelimiter.java`)
  - stray `;` between members, which javac's tree lacks but its doc-comment handling sees
    (`depDocComment/DeprecatedDocComment3.java`)

## 2026-10-06: Transcriptions are not kept

- The user decided that transcribed `.clj` files (such as those of the 183 baseline sources) are
  preliminary and are not committed. They can be regenerated with
  `bin/javalisp -m arbace.javalisp.main transcribe clojure OUT`.

## 2026-10-06: javalisp files are `.jls`

- javalisp text now has its own suffix, `.jls`, so it is not mistaken for Clojure source.
  `.clj` stays for the Clojure code of the tool itself. `transcribe` writes `OUT/**.jls`,
  `check -o` writes `.jls` next to the `.rt.java` files, and the commands `java2clj` and
  `clj2java` are renamed `java2jls` and `jls2java`.

## 2026-10-06: Decisions for later phases

Recorded now, at the user's request, so that the current work does not block them:

- When `clojure/` is vendored into `arbace/`, everything is renamed to `arbace.*`: the Java
  packages (`clojure.lang` becomes `arbace.lang`), the namespaces (`clojure.core` becomes
  `arbace.core`), resource paths and the string references to all of them.
- `clojure/version.properties` (`version=1.13.0-master-SNAPSHOT`, read by `core.clj` to build
  `*clojure-version*`) is not vendored as a file. Its value is folded into the source (for
  example a literal in `arbace/core.clj`), or the versioning machinery is dropped, as versioning
  is not relevant to Arbace. The dropped value is `1.13.0-master-SNAPSHOT`.
- Replacing the `.java` files under `arbace/` with `.jls` includes the vendored javac itself.
  The end state is an `arbace/` tree with no `.java` at all, so building javac will need a
  bootstrap, for example a stage-0 compiler built from Java regenerated by `jls2java`, or taken
  from git history.

## 2026-10-06: Vendor javac as arbace.javac

- Source: https://github.com/openjdk/jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259`
  (local checkout `/root/jdk26u`), the commit the running JDK 26.0.2.1 was built from. License:
  GPLv2 with the Classpath Exception. `LICENSE.md` now carries jdk26u's `LICENSE`,
  `ADDITIONAL_LICENSE_INFO` and `ASSEMBLY_EXCEPTION` verbatim, and `arbace/javac/README.md`
  records the origin next to the code.
- Taken from `src/jdk.compiler/share/classes`, everything except `module-info.java` and
  `sun/tools/serialver` (a separate tool), 360 files:
  - `com/sun/tools/javac/**` to `arbace/javac/**`
  - `com/sun/source/**` to `arbace/javac/source/**`
  - `com/sun/tools/doclint/**` to `arbace/javac/doclint/**`
- Repackaged because the JDK's `jdk.compiler` module owns the original package names, so
  class-path copies would be shadowed (as ASM became `clojure.asm`). The scheme keeps all of
  javac under one root: `com.sun.tools.javac` → `arbace.javac`, `com.sun.tools.doclint` →
  `arbace.javac.doclint`, `com.sun.source` → `arbace.javac.source`. It is a textual
  replacement in every `.java` and `.properties` file, in that order, over code, strings and
  comments:
  `sed -i -e 's/com\.sun\.tools\.javac/arbace.javac/g' -e 's/com\.sun\.tools\.doclint/arbace.javac.doclint/g' -e 's/com\.sun\.source/arbace.javac.source/g'`.
  The strings matter: bundle names (`arbace.javac.resources.compiler`, `.ct`, `.version`,
  `.launcher`, `.javac`), the class names `Trees` and `JavacTask` check, and `DefinedBy`.
  Alternatives considered: `arbace.javac` for javac but `arbace.source` for the tree API
  (rejected: two roots for one module), or no renaming with `--patch-module jdk.compiler`
  (rejected: it would replace the JDK's javac, which the javalisp tool uses as the reference).
- Build-generated sources, vendored as generated (the generators stay upstream). The langtools
  build tools at jdk26u `make/langtools/tools` (`propertiesparser`, `flagsgenerator`) were
  compiled and run on the unrenamed upstream inputs, then their output was renamed by the same
  `sed`:
  - `java -cp TOOLS propertiesparser.PropertiesParser -compile .../resources/compiler.properties OUT/ -compile .../resources/launcher.properties OUT/`
    gives `resources/CompilerProperties.java` and `resources/LauncherProperties.java`.
  - `java -cp TOOLS flagsgenerator.FlagsGenerator .../code/Flags.java OUT/FlagsEnum.java`
    gives `code/FlagsEnum.java`.
  - All three are byte-identical (before renaming) to the JDK build's own output in
    `/root/jdk26u/build/linux-x86_64-server-release/support/gensrc/jdk.compiler`.
  - The message bundles are not compiled into `ListResourceBundle` classes as the JDK build
    does (`compileproperties`). They stay `.properties` resources, which `ResourceBundle` loads
    from the class path with the same content. `version.properties` (from
    `version.properties-template`) is written by the build script: `jdk=26.0.2.1`,
    `full=26.0.2.1-arbace`, `release=26.0.2.1`.
- `bin/build-javac` compiles `arbace/javac` with the system javac (`-g`, plus `--add-exports`
  for the JDK internals jdk.compiler uses: `java.base/jdk.internal.{javac,misc,module,jmod}`,
  `java.base/sun.reflect.annotation`, `jdk.internal.opt/jdk.internal.opt`) into `target/javac`
  (already gitignored as `/target/`). It copies the bundles and registers
  `arbace.javac.platform.JDKPlatformProvider` as a service, which `--release` needs. The
  `javax.tools.JavaCompiler` and `ToolProvider` registrations of the module are left out so the
  vendored javac never stands in for the JDK's. `bin/javac` runs `arbace.javac.Main`.
- Verified on the unmodified vendored javac:
  - `clojure/`'s 183 files compiled with `-g -nowarn` give 812 classes byte-identical to the
    system javac's, both without and with `--release 26`.
  - The vendored javac compiling its own 346 sources gives 1,588 classes byte-identical to the
    system javac's build of them.

## 2026-10-06: Drop the vendored javac; new direction: classes in idiomatic Clojure

- The user changed course: instead of a javac that reads javalisp, Arbace's own Clojure compiler
  should compile Java's classfile constructs, written as idiomatic Clojure forms in ordinary
  `.clj` sources (classes definable from the REPL). The end state has no `.java` and no `.jls`.
  The `.jls` pathfinder (javalisp, `arbace/javalisp/`) stays as a tool.
- The vendored javac (`600f4ed`) was removed from the tree: `arbace/javac/`, `bin/build-javac`,
  `bin/javac`, and jdk26u's license texts in `LICENSE.md`. It remains in git history, recorded
  in the entry above, should it be needed again. An unfinished, never committed `.jls` parser
  for it (`arbace/javac/parser/Javalisp{Parser,Prec,Reader}.java`, about 3,700 lines, a port of
  `reader.clj`, `prec.clj` and `l2j.clj`) was discarded.
- That supersedes the earlier decision that the vendored javac would also be rewritten. The
  `arbace.*` renaming and dropping `version.properties` stand.
- Decisions for the new language, taken with the user:
  - Goal: every classfile construct javac can produce is expressible and compiles to equivalent
    classes. Comments, line numbers, debug info and byte-identical output do not matter.
  - Idiomatic Clojure vocabulary (metadata for modifiers and type hints, interop forms, `set!`)
    extended with new forms the compiler knows: a class-defining special form, mutable locals,
    and the missing control flow.
  - Control flow: `return`, `break` and `continue` (with labels) exist, but the Java → Clojure
    converter prefers restructuring into `if`/`when`/`cond`/`loop` wherever that is
    straightforward.
  - Java primitive arithmetic is written with explicit Clojure operators (`unchecked-*-int`,
    `bit-*` and the like, adding the missing ones), so `+` never changes meaning.
  - The converter needs javac's attributed trees (name resolution, overloads, implicit
    conversions). It uses the JDK's own javac as a conversion-time tool only.
  - Bootstrap: the frozen `clojure/` (stage 0, plus a bootstrap library that compiles the new
    forms) compiles `arbace/**/*.clj` into stage 1, stage 1 compiles them again into stage 2,
    and stage 2 must reproduce itself. The `arbace.*` names keep the stages apart.

## 2026-10-06: Spec of the class forms (agenda step 1)

- Wrote `doc/classes/SPEC.md`, a proposal for review: Java's class file constructs written as
  idiomatic Clojure ("class forms"), within the decisions of the previous entry. No code yet.
- Method: enumerated the constructs from javac's `Tree.Kind`, the JVMS class file structure (access
  flags, attributes) and the attributes javac's `ClassWriter` writes, and javac's desugarings
  (`Lower`, `TransTypes`, `TransPatterns`, `LambdaToMethod`, `StringConcat`), all at jdk26u
  `baf63fbe42b8`, `src/jdk.compiler/share/classes`. Studied Clojure's existing forms and limits at
  `98d735fab02f` (`clojure/`): `Compiler.java` (special forms, `LocalBinding`, `LetExpr`,
  `QualifiedMethodExpr` and param-tags, array class symbols, `FISupport`, `NewInstanceExpr`),
  `Intrinsics.java`, `DynamicClassLoader.java`, `RT.makeClassLoader`, `core.clj`,
  `core_deftype.clj`, `genclass.clj`, `core_proxy.clj`. The spec ends with coverage tables
  (tree kinds, flags, attributes) and its sources.
- Main decisions proposed:
  - Guiding rule: existing Clojure forms keep their meaning; new meaning only for new names (vars
    in `arbace.core` over new starred special forms such as `class*`, `label*`, `switch*`) and
    for forms that are errors today (`^int` on a local with a primitive initializer, `set!` of a
    local, list `:tag`s, `recur` out of tail position, `new` of an array class).
  - One `defclass` for all kinds (`^:interface`, `^:enum`, `^:record`, `^:annotation`), header
    options `:extends`, `:implements`, `:permits`, `:type-params`, and member forms `field`,
    `method`, `constructor`, `initializer`, `static-initializer`, nested `defclass`,
    `constants` (enums). Modifiers and annotations are metadata (Clojure's annotation syntax);
    Java's defaults apply (no modifier is package access). Explicit receiver parameter as in
    `deftype`. Generic types as data in `:tag` (`^{:tag (List T)}`), used only for `Signature`;
    the compiler works on erasures.
  - Code: `^:mutable` locals with `set!`, exact primitive locals by tag, `^:const` locals;
    Java arithmetic with `unchecked-*`/`bit-*` operators plus new ones (`-float` family,
    `unchecked-divide`, `unchecked-remainder`, `bit-*-int`, `bit-shift-*-int`); keyword labels with
    `label`, `break`, `continue` (non-tail `recur`), `return`; `switch` with Java constants and
    patterns, `if-instance`/`when-instance`; `for-each`, `with-resources`, `java-str`,
    `java-assert`, `anon`, `letclass`, `lambda`, `method-ref`; `Outer/this`, `super`,
    `(super. ...)`, `(this. ...)`, `(.new o Inner ...)`; arrays as `(new int/1 n)`.
  - The compiler derives what javac derives (bridges, enum and record members, captures, nest
    attributes, switch translations, lambda methods, string concatenation by `invokedynamic`),
    with javac's names. The converter writes erasure casts, narrowing, overload pins
    (param-tags, decided by running the compiler's own resolution), evaluation-order temporaries
    and implicit qualifications.
  - Equivalence is defined over class shapes plus the symbolic content of code (§3).
  - Compilation: a class environment entered from the current top-level form and from sources on
    a source path (javac's `-sourcepath` behaviour) handles the cyclic references of Java
    classes; one namespace per Java package with one loaded file per Java file. REPL: one class
    loader per Java package, with a new generation on redefinition, so package access works
    across forms.
- Alternatives considered are recorded in the spec next to each choice (§12 lists the names
  considered for every new form).
- Open questions for the user, each with a recommendation in §12: explicit vs implicit `this`;
  default access; one `defclass` vs a macro per kind; the spelling of generic types; namespace
  layout; handling of cycles; reflection as an error; derived vs explicit bridges; switch
  fall-through; the names of the new forms; implicit boxing; REPL loaders; `float` operators;
  keyword labels; `deftype`-style signature inference; reusing `cast`; one or two
  implementations of the class forms across the stages.

## 2026-10-06: Reference run of Clojure's test suite; two seed fixes

- `bin/clojure-tests` (helper `test/run_clojure_tests.clj`) runs upstream Clojure's test suite at
  `98d735fab02f` against a Clojure (the baseline by default) and checks the results against
  `test/baseline-results.edn`. It writes only under `.tmp/clojure-tests/`.
  - The suite is fetched into `.tmp/` from `test/clojure`, `test/java` and `src/script`.
  - Test libraries come from Maven Central, checked by SHA-1: test.generative 1.1.1,
    tools.namespace 1.5.1, java.classpath 1.1.1, tools.reader 1.6.0, data.generators 1.1.1 and
    test.check 1.1.3. spec.alpha and core.specs.alpha are left out because the baseline stubs
    spec, and jaxws-api because only Java 8 to 10 use it.
  - Adaptations: the Clojure under test is AOT-compiled with direct linking, as upstream does,
    since the AOT test fixtures need it. Each namespace runs in its own JVM, in parallel. Each
    JVM first loads every test namespace in upstream's order, because several test namespaces
    rely on others having loaded `clojure.test-helper` or `clojure.pprint`.
  - Control run: upstream's own build of the same revision, on the same JDK 26, passes all of
    it. That is 83 namespaces, 809 tests, 20,750 assertions and 27 test.generative specs. To
    reproduce it, with `C=.tmp/clojure-tests/control`:
    `CLOJURE_TESTS_RUN=upstream CLOJURE_SRC="$C/build:$C/clj:$C/lib/spec.alpha-0.6.249.jar:$C/lib/core.specs.alpha-0.6.133-alpha10.jar" bin/clojure-tests`
- The run found two baseline bugs from the seed. The user approved fixing them in `clojure/`:
  - Upstream ASM (`0460f74ba642`) caps stack-map frame computation per method at 10 MB and 100M
    operations. Clojure's bundled ASM has no cap. Compiling `data_structures.clj:1586`
    (`missing-directive`) threw `LimitExceededException`. Fix: `Compiler.classWriter()`, through
    which every ClassWriter is made (`core_proxy.clj` and `genclass.clj` included), calls
    `setComputeLimits(Integer.MAX_VALUE, Long.MAX_VALUE)`. The vendored ASM stays untouched.
  - `clojure.lang.Compile` still did `RT.load("clojure/core/specs/alpha")`, which the spec stub
    removed, so AOT compilation with it failed. Fix: the line is dropped.
- With both fixes the baseline matches upstream apart from the 32 assertions that expect spec's
  error messages: 83 namespaces, 809 tests, 20,718 of 20,750 assertions pass, and 27 of 27
  test.generative specs pass. The harness's empty `clojure.core.specs.alpha` stand-in is gone.

## 2026-10-06: Class forms spec settled

- The user reviewed `doc/classes/SPEC.md` and accepted all 17 recommendations of its §12. In
  brief:
  - Receiver: an explicit `this` parameter.
  - Default access: Java's package access.
  - Declarations: one `defclass`, with the kind as metadata.
  - Generic types: written `^{:tag (List T)}`.
  - Converted code: one namespace per Java package.
  - Cyclic references: a class environment plus a source path.
  - Reflection in class bodies: an error, with a `:warn` escape.
  - Bridge methods: derived by the compiler.
  - `switch` fall-through: the shared code is repeated.
  - Names of the new forms: the spec's.
  - Boxing: implicit, as javac does it.
  - REPL: one class loader per Java package, a new generation when a class is redefined.
  - Arithmetic: new `-float` operators.
  - Labels: keywords.
  - Untyped methods: the signature is inferred from a unique inherited method.
  - `cast`: compiled to `checkcast`.
  - Implementation: one, in plain Clojure, used at stage 0 and compiled into stage 1.

## 2026-10-06: Track the baseline's compiled classes

- With the user's consent, the 812 classes of `clojure/` (1.8 MB) are now tracked, through a
  `!/clojure/**/*.class` exception in `.gitignore`. Fresh checkouts and agent worktrees under
  `.tmp/worktrees/` then run the baseline Clojure without building it first.
- They were rebuilt from scratch exactly as `SEED.bash` built them, `javac -g` over every
  `.java` under `clojure/`, including the two seed fixes. Rebuild them after any further fix.

## 2026-10-07: Class forms compiler and Java → class forms converter (agenda steps 2 and 3)

Both were built by background agents, each in its own worktree under `.tmp/worktrees/` (the
user's rule for agents). They were checked by the main session on `b1bf2c8`.

- Compiler: `arbace/classes/` (`arbace.classes.*`). It is one plain-Clojure implementation
  (§12 q17), run at stage 0 on the frozen `clojure/` through `arbace.classes.boot`, which
  rewrites `arbace.asm` to `clojure.asm`.
  - It covers every class kind, every member kind, bridges, generic signatures, annotations and
    type annotations, sealed types, modules and package annotations.
  - Code: exact primitive arithmetic, mutable locals, `label`/`break`/`continue`/`return`,
    `switch` with patterns, lambdas and method references (serializable ones too), `java-str`
    by `invokedynamic`, and nested, inner, local and anonymous classes.
  - Class environment: AOT, and REPL class loaders per package with generations.
  - `bin/class-forms-tests`: most tests compare class shapes with javac's output for the
    equivalent Java; others run the classes, among them a REPL session with redefinition.
  - Details, stage-0 limits and §8 coverage: `doc/classes/COMPILER-NOTES.md`.
- Converter: `arbace/j2c/` (`arbace.j2c.*`, `bin/j2c`). It runs the JDK's javac (`parse` and
  `analyze`) and converts the attributed trees. `--rename clojure=arbace` serves step 4.
  Details are in `doc/classes/CONVERTER-NOTES.md`.
- Results:
  - All 183 baseline files convert. They compile with the class forms compiler to 812 classes
    whose shapes match javac's: flags, signatures, attributes, members, and the symbolic
    content of the code.
  - Clojure boots from those 812 classes. Clojure's test suite on them gives the same result
    as on the baseline: 809 tests, 20,718 of 20,750 assertions, 27 of 27 generative specs.
    Checked with `bin/j2c-check --suite`.
  - jdk26u: all 12,730 files of `src/*/share/classes` convert. About 91 to 94% of them compile
    to javac's class shapes (the agent's numbers, not rechecked).
  - Language samples (`test/j2c/java`): one difference from javac is left, integer constants in
    `d + (z ? 1 : 2)`.
- The agents propose 13 (compiler) and 15 (converter) spec amendments, in their notes, and
  await the user's decision. Known gaps:
  - Stage 0 rejects `fn`, `letfn` and `case` inside class bodies.
  - The new code forms work only inside class bodies at stage 0.
  - No form yet for `o.new Inner() { ... }`.
  - The converter drops 6 type annotations in the JDK.
  - The converter predicts overloads conservatively instead of asking the compiler.

## 2026-10-07: Spec amendments folded into the class forms spec

The user accepted all 28 spec amendments that implementing the compiler (13, COMPILER-NOTES.md)
and the converter (15, CONVERTER-NOTES.md) proposed. They are now normative text in
`doc/classes/SPEC.md`, at the sections they concern, not an appended list; overlapping ones
were merged (compiler 6 with converter 13, compiler 2 with converter 15, compiler 10 with
converter 12, compiler 11 with converter 3). The notes keep one line per amendment pointing to
the SPEC section; the original texts are in commit `7229dd9`. §12 records the amendments next
to the settled questions.

- Syntax and reading:
  - Class-form files are read by `clojure.core/read` (Clojure 1.12+), not EDN (§1.5).
  - `C/name` may declare a member of `C` whose name is no symbol, as `List/nil` (§4.6).
  - Param-tags on constructor calls: `(^[int] super. x)`, `(^[int] this. x)`, `.super`, `.new`
    on the head; `(anon C ^[int] [x] ...)` and `(NAME ^[int] [x])` on the argument vector
    (§4.7, §4.8, §4.10, §5.6).
  - `(anon Inner [args] :outer o ...)` is `o.new Inner(args) { ... }`; anonymous subclasses of
    inner classes pass the outer instance as javac does (§4.8).
  - `switch` labels are literals, constant symbols or lists of them; other constant expressions
    are written folded (§5.8).
- Meaning in class bodies:
  - `this` names the instance in field initializers and `initializer` bodies (§4.5, §4.7, §5.2).
  - An enclosing method's receiver is the enclosing instance (`this$0`), the way to reach an
    enclosing anonymous class (`this1`); members of anonymous classes are named without the class
    or by javac's binary name (§4.8).
  - Reference tags on `let` bindings are a `checkcast` when not already assignable (§5.3).
  - Locals shared by `switch` cases are bound mutable around the `switch` (§5.3, §7).
  - Clojure's `+ - * inc dec` on `long` are `Math.*Exact`, `quot`/`rem` `ldiv`/`lrem`; on
    `double` IEEE instructions; in the Java subset (§5.4, §5.13).
  - All-literal conditionals are `int`: they narrow like literals, and are `int` operands of any
    primitive operator (§5.4).
  - Branches of different primitive types are boxed each by its own type, as Clojure's `if`
    (§5.5).
  - Overload resolution is JLS 15.12.2 on erased types (strict, loose, variable arity), then a
    phase narrowing integer literals; the converter decides pins with this resolution (§5.6,
    §7.3).
  - Signature polymorphic calls take their descriptor from param-tags (or argument types) and a
    tag on the call (§5.6).
  - Array `clone()` is followed by a `checkcast` (§5.6); `for-each` with a primitive binding over
    an `Iterable` casts to the wrapper and unboxes (§5.7).
  - Nested `java-str` forms flatten into one call site (§5.10).
- What javac derives and the converter writes:
  - Enum switches use ordinals directly only for enums of the same top-level class, otherwise
    the `$SwitchMap$` holder, numbered after the anonymous classes (§5.8, §6).
  - Erasure casts as javac's `TransTypes`: qualifiers, arguments (instantiated parameter types),
    values (§5.5, §7).
  - Casts of `null` and to array types stay `checkcast`s; intersection casts list only the
    missing bounds (§5.5).
  - Statics by simple name are qualified by the current class when a member of it, else by the
    declaring class (§7.1).
- Compilation:
  - Equivalence is relative to javac's options; the compiler has `-XDstringConcat=inline` and
    `-parameters` counterparts (§3, §5.10).
  - A converted file gets `_class` appended when a `.clj` of that name exists; a single-segment
    package is a single-segment namespace (§9.1).
  - `defclasses` compiles class forms together at stage 0, where a top-level `do` cannot (§9.2,
    §9.5, §10).

Where the implementation lags the amended spec (listed in AGENDA.md): the compiler does not yet
read constructor-call param-tags on `super.`, `this.`, `.super`, `.new`, `anon` and enum
constants, nor `:outer` in `anon`; it types all-literal conditionals as `int` only for `int`
contexts, not under `long`, `float` and `double` operators; the converter still predicts pins
instead of calling `arbace.classes`'s resolution.

## 2026-10-07: Vendor clojure/ as arbace/, all .clj, and self-host (agenda step 4)

- Done by a background agent (`af29cc6`); the full record is `doc/VENDOR-NOTES.md`.
- Derivation:
  - The baseline's Java was converted with `arbace.j2c` at `7229dd9`, with
    `--rename clojure=arbace`.
  - `arbace.j2c.rename` renamed the rest: string literals, and the `.clj` sources.
  - Two hand edits: `version.properties` folded into `arbace/core.clj` as
    `"1.13.0-master-SNAPSHOT"`, and `server.clj`'s system property prefix.
  - The rename rules cover:
    - the packages and the class `arbace.main`
    - the vendored namespaces
    - `clojure.error` keywords
    - the `clojure.compile.*`, `clojure.server.*` and similar system properties
  - Other libraries' names (spec, tools.deps, test libraries), identifiers merely containing
    the word, prose and URLs were left alone.
  - `bin/vendor-arbace` replays the derivation into `.tmp/vendor/`.
  - From now on `arbace/` is hand-maintained.
- Bootstrap (`bin/build-arbace`):
  - Stage 0 compiles 183 files to 812 classes (`target/stage1`).
  - Stage 1 loads `arbace.classes` as plain Clojure, against `arbace.core` and `arbace.asm`,
    and rebuilds everything as stage 2. Stage 2 rebuilds it as stage 3.
  - Stages 1, 2 and 3 are byte-identical. Stage 1 boots without loading any `clojure.*` class.
- Clojure's test suite, renamed by the new `CLOJURE_TESTS_RENAME=arbace` mode of
  `bin/clojure-tests`: on stages 1 and 2, 809 tests, 20,718 of 20,750 assertions and 27 of 27
  generative specs pass. That is the baseline's result, and the main session checked it.
- Fixes made along the way to `arbace/classes`:
  - its sources name `arbace.*`, and runtime class names come from the running runtime
  - `boot` works at every stage
  - the packages being built always come from source (`env/*from-source*`)
  - Java's import order is used for name resolution
  - `_class` file lookup, and a crash in source lookup
  - bridge methods for classes entered from source
- Next: make the class forms native (SPEC §9.5). Open for the user:
  - `arbace.clj`, the package file of the class `arbace.main`
  - the leftover `clojure` identifiers

## 2026-10-07: Spec gaps closed; decisions on step 4's leftovers

- A background agent closed the four places where the code lagged the amended spec. The work
  is `f240a04`, `90ba9ae` and `13ef825`; details are in the notes files.
  - Param-tags on constructor calls: `super.`, `this.`, `.super`, `.new`, and the argument
    vectors of `anon` and enum constants.
  - `(anon Inner [args] :outer o ...)`, compiled as javac compiles `o.new Inner(args) {...}`.
  - All-literal conditionals are `int` operands of every primitive operator. All 6 language
    samples are now identical to javac.
  - The converter decides pins with the compiler's own resolution (`arbace/j2c/resolve.clj`).
    Param-tags went from 534 to 4 in the baseline and from 14,182 to 1,440 in the JDK.
  - All checks stay green: `bin/class-forms-tests`, `bin/j2c-check --suite` and
    `bin/build-arbace --suite` (the fixpoint, and the suite on stages 1 and 2).
- The user's decisions on step 4's open points:
  - The class `arbace.main` moves into a package, so nothing remains at the repo root.
    `arbace.clj` goes away, and the `arbace.main` namespace stays.
  - Runtime-visible leftovers are renamed to `arbace`: the `clojure-agent-*` thread names,
    `__clojureFnMap` and the `clojure-` temp-file prefix. `clojure-version` and
    `*clojure-version*` stay, because libraries call them.
  - Next step, started now: make the class forms native (SPEC §9.5).

## 2026-10-07: Native class forms (SPEC §9.5); step 4 cleanups

- A background agent did both; the work is `08e2167`, `0290f14` and `5081e57`. Details are in
  `doc/VENDOR-NOTES.md` ("After vendoring: hand changes") and `doc/classes/COMPILER-NOTES.md`
  ("Native class forms (stage 1 on)", "Clojure in class bodies").
- Cleanups, as the user decided:
  - The class `arbace.main` is now `arbace.lang.Main` (`arbace/lang/Main.clj`, next to
    `Compile`, `Repl` and `Script`), and `arbace.clj` at the repo root is gone. The
    `arbace.main` namespace keeps its name. Launch: `java -cp target/stageN:. arbace.lang.Main`.
  - Renamed to `arbace`: the agent pool thread names, `__clojureFnMap` (now `__arbaceFnMap`),
    the `IProxy` methods `__{init,update,get}ArbaceFnMappings`, the error-report temp-file
    prefix, and the socket server and process IO thread names. Kept: `clojure-version`,
    `*clojure-version*`, the REPL banner, `refer-clojure`, `arbace.java.api.Clojure` and the
    `Clojure` debug stratum. `bin/clojure-tests` adjusts the one suite test that names the
    `IProxy` methods (`test-proxy-method-order`).
- Native class forms:
  - The macros and the §5.4 operators are `arbace.core` vars (`arbace/core_classes.clj`, loaded
    by `core.clj`); `arbace/classes/core.clj` is removed. `arbace.lang.Compiler` has the
    special forms `class*`, `label*`, `break*`, `continue*`, `return*`, `switch*`, `lambda*`,
    `method-ref*`, `java-str*`, `java-assert*`, `for-each*`, `with-resources*` and
    `if-instance*`.
  - One implementation: `arbace.classes`, loaded on first use through `arbace.classes.native`.
    `defclass`/`defclasses` are compiled during analysis and replaced by imports and the
    class. A fn using code forms (or Clojure errors that are class forms, such as `set!` of a
    local) is handed over whole to `arbace.classes`, which emits the fn class the compiler
    would have made. A top-level `do` enters its class forms as siblings (§9.2).
  - `arbace/classes/lower.clj` rewrites `fn*`, `reify*`, `letfn*`, `case*`, `def` and `var`
    into class forms, so class bodies accept them, also at stage 0.
  - Narrowed from the spec, and SPEC §9.5 now says so: `recur` stays an error out of tail
    position and across `try` in code the compiler compiles itself, because Clojure's suite
    asserts those errors. Class forms inside `deftype` method bodies are not supported.
  - SPEC changes: §9.5 ("One implementation"), §9.2 (top-level `do`), §9.6 (stage 1), §5.13
    (Clojure semantics outside the Java subset).
- Checks (the main session reran them on `5081e57`):
  - `bin/build-arbace --suite`: stages 1, 2 and 3 identical, now 815 classes. The new
    `bin/native-tests` on stage 1 runs 14 tests and 96 assertions with no failures. The suite on
    stages 1 and 2 gives the baseline's result.
  - `bin/class-forms-tests`: 61 tests, 125 assertions, all pass.
  - `bin/j2c-check --suite`: clean, samples 6 of 6, no regressions.
  - At a stage-1 REPL with no boot step: `defclass` with a constructor, `for-each` over a
    mutable `^int` local in a `defn`, and `label`/`switch`/`break` in an anonymous fn all work.
- Left open: the §5.4 operators have no `:inline` expansions yet; `reify` signatures are
  inferred, not chosen from hints; the first class form costs about 2 s to load
  `arbace.classes`; `bin/vendor-arbace` no longer reproduces `arbace/` where hand changes apply.

## 2026-10-07: arbace/README.md moved to doc/ARBACE.md

- At the user's request, `arbace/` now holds only `.clj` files: its README (what the tree holds,
  and the origin and licenses of the vendored Clojure) moved to `doc/ARBACE.md`.
- CLAUDE.md's vendoring rule now says that for `arbace/` the origin record next to the code is
  `doc/ARBACE.md`, plus each converted file's notice kept as a comment. The root README links it.

## 2026-10-07: All licenses in LICENSE.md

- At the user's request, `LICENSE.md` is the one place that states licenses: their full texts and
  which files each covers. `doc/ARBACE.md` no longer names licenses, and CLAUDE.md now says
  origins go to the journal and next to the code, licenses to `LICENSE.md`.
- Source files keep the copyright and license notices they came with. They were not removed,
  because EPL 1.0 §3 ("may not remove or alter any copyright notices") and the BSD 3-Clause
  License (retain the notice in source redistributions) require them.
- Survey of the notices in `clojure/` (`98d735fab02f`): 186 Clojure files under EPL 1.0, 41 ASM
  files under BSD 3-Clause, and two gaps in the old `LICENSE.md`:
  - `clojure/lang/Murmur3.java` (→ `arbace/lang/Murmur3.clj`) is Guava's, Copyright (C) 2011
    The Guava Authors, under the Apache License 2.0; MurmurHash3 itself is Austin Appleby's,
    public domain. The Apache License 2.0 text (terms and conditions, without the appendix) was
    added, taken from `APACHE-LICENSE-2.0` of github.com/sergi/go-diff
    v1.3.2-0.20230802210424-5b0b94c5c0d3 in the local Go module cache.
  - `clojure/repl.clj` (Copyright (c) Chris Houser) still carries an older header naming the
    Common Public License 1.0, the EPL's predecessor. Upstream Clojure ships it under the EPL
    and includes no CPL text; `LICENSE.md` records the fact.
- The stale `arbace/` paths in `LICENSE.md` (`arbace.clj`, `arbace/main_class.clj`) were
  replaced by a rule: everything outside `arbace/classes/`, `arbace/j2c/` and `arbace/javalisp/`
  is vendored, under its source file's license.

## 2026-10-07: Arbace is licensed under EPL 1.0

- The user chose the Eclipse Public License 1.0 for Arbace's own code and docs (Copyright (c) the
  Arbace authors), the same license as Clojure. Recorded in `LICENSE.md`, whose EPL section now
  covers both Arbace and Clojure, and in the root README. No per-file headers were added.
- Alternatives considered: MIT (the user's recent choice elsewhere) and 0BSD. Both would have
  covered only the tools (`arbace/classes/`, `arbace/j2c/`, `arbace/javalisp/`, `bin/`), while
  the vendored Clojure in `arbace/` stays EPL 1.0 regardless. New code is written into those
  hand-maintained EPL files, so one license spares tracking a boundary that blurs over time.

## 2026-10-07: javalisp dropped

- At the user's request javalisp is removed: `arbace/javalisp/`, `bin/javalisp` and
  `doc/javalisp/SPEC.md`. It was the pathfinder for the class forms; nothing depends on it (the
  converter `arbace.j2c` has its own javac access). Its last tree is `959d114`; its results
  stay recorded in this journal (exact round trip on `clojure/` and on jdk26u's src and tests).
- What is given up: a comment- and layout-preserving Java transcription. The class forms keep
  only each file's leading notice, which is all the rewrite needs; javalisp can be restored from
  history if exact transcription is ever wanted again.
- References updated: README (the Tools section now describes the class forms), CLAUDE.md
  (javalisp's layout entry and its check rule), AGENDA, LICENSE.md, `doc/ARBACE.md`,
  `doc/VENDOR-NOTES.md`, `bin/vendor-arbace`, and `arbace/classes/boot.clj` (the stage-0 list of
  tool namespaces that keep their names).
- Checked: `bin/class-forms-tests` passes; `bin/build-arbace` gives stages 1, 2 and 3 identical
  (815 classes) and the native tests pass.

## 2026-10-07: Survey of modern classfile and platform features (doc/MODERN-COMPILER.md)

- At the user's request a background agent surveyed how Arbace's compilers could use the
  features added since Java 9, targeting Java 26 (jdk26u `baf63fbe42b8`). The survey is
  `doc/MODERN-COMPILER.md` (`86befe9`), with sources (JEPs, jdk26u paths) and measurements.
- Baseline found: Clojure's compiler emits classfile version 61 (Java 17,
  `clojure/lang/Compiler.java` `JVM_BYTECODE_VERSION = V17`), not pre-Java 9 bytecode. Its only
  `invokedynamic` is Clojure 1.12's functional-interface adapter. The class forms compiler
  already emits version 70 with javac's bootstraps. The stages hold no compiled namespaces, so
  `arbace.core` compiles from source at every launch.
- Ranked recommendation: first, AOT-compile the namespaces into the stages and use the JDK AOT
  cache (measured launch 3.27 s → 0.45 s), add `ClassFile.verify` to the checks, emit version
  70. Next, measured one by one: condy for constants, `invokedynamic` for keyword sites and
  reflective calls, an opt-in virtual-thread executor. Defer `invokedynamic` Var calls and
  protocol caches, replacing ASM with `java.lang.classfile`. Avoid `LambdaMetafactory` and
  hidden classes for fns, preview APIs, `ScopedValue` for bindings, JVM records for `defrecord`.
- No decision taken yet; it is the user's.

## 2026-10-07: Step 5's open ends closed

- Two background agents, in parallel. The main session reran the full gate on the combined tree
  (`2f2a5f2`).
- Operators and load time (`2dd9cba`, `367c83d`, `ea25842`):
  - The §5.4 operators have `:inline` expansions to new `arbace.lang.Numbers` methods. Where the
    context is primitive, `Intrinsics` makes them bare instructions (`IAND`, `FADD`, `LDIV`,
    ...). Boxed operands convert as for `unchecked-add-int` (RT casts), with no reflection.
    Behaviour changes: float overflow gives Infinity (the old function threw); a shift count
    that does not fit an `int` throws.
  - In class bodies, when no method applies after a Clojure `:inline` expansion, the single
    method of that name and arity is called with RT casts, as Clojure does. This also fixes
    Clojure's own `unchecked-add-int` on Object operands there. Float `%` constant folding gave
    0.0 and is fixed.
  - Each stage AOT-compiles `arbace.classes` into itself. The first class form in a session
    went from about 1.05 s to 0.26 s; the time was nearly all compiling those namespaces from
    source. For reproducible AOT output, `Compiler$LocalBinding` got a `hashCode` from index and
    name, and `generate-proxy` sorts superclass constructors.
- `deftype`/`defrecord` and `reify` (`2f2a5f2`):
  - A `deftype*` whose method bodies use class forms is handed over whole to
    `arbace.classes.native/compile-deftype`. That builds a class form with Clojure's deftype
    shape (fields, mutability flags, constructors, the record extras, `getBasis`). Deftypes
    without class forms compile as before.
  - `reify` and handed-over deftype methods pick the interface method as Clojure's compiler
    does (name and arity, then hints), with Clojure's error messages.
  - Left open: `deftype*` inside a class body or a handed-over fn (use `defclass`); small
    differences in covariant bridges; the arity fallback is not Clojure's full
    `getMatchingParams`.
- SPEC §9.5 and §9.6 were amended by the agents to match (see the notes files).
- Gate on `2f2a5f2`, rerun by the main session:
  - `bin/build-arbace --suite`: stages 1, 2 and 3 identical, now 2,150 classes. The native tests
    (25 tests, 2,337 assertions) pass. The suite on stages 1 and 2 gives the baseline's result.
  - `bin/class-forms-tests`: 61 tests, 126 assertions pass. `bin/j2c-check --suite` is clean.
- Pitfall both agents hit: in a fresh worktree checkout leaves `clojure/**/*.java` newer than
  their tracked classes, so in-process javac recompiles them and `bin/class-forms-tests` fails.
  The fix is `find clojure -name '*.class' -exec touch {} +`; CLAUDE.md now says so.
- On the user's question about tail calls: Java 26 has no tail-call support (no bytecode, no
  HotSpot elimination; the MLVM patch and Loom's stated goal never landed). Self-calls compile to
  jumps via `recur`. A group of mutually recursive fns known at compile time could be fused
  into one method with a dispatch switch. Full Scheme-style tail calls are a point for an own
  runtime.

## 2026-10-07: Direction: excellent on the JVM now, standalone on bare metal later

- The user set Arbace's direction:
  - Short term: make Arbace excellent on the modern JVM (Java 26). The first step was started
    now: AOT-compile all namespaces into the stages and use the JDK AOT cache (survey item 1).
  - Long term: Arbace will not be binary compatible with the Java ecosystem. Java source stays
    usable through j2c transcription.
  - The Arbace language: Clojure extended with an idiomatic representation of Java (the class
    forms), and later a projection of Go into the same dialect.
  - The goal: a self-sustaining REPL inside a virtual sandbox, written in Arbace all the way
    down to the bare metal ISA, much like Go plus TamaGo. The JVM is overkill for this; the Go
    runtime is the promising inspiration.
  - Two target environments: /dev/kvm on amd64 with an Alpine edge Linux host, and
    Hypervisor.framework on arm64 with a macOS host.
  - Before breaking away from the `.class` format and the JVM, freeze the JVM state of the art
    on a well-known branch, `arbace-for-java-26`, advertised in the docs. It is not created yet,
    because the JVM work is still going on.
- New prior art: https://github.com/usbarmory/tamago (Go on bare metal) and
  https://github.com/candid82/joker (a Clojure dialect in Go). Go changes role: before, only the
  runtime was in scope; now the language too, as a later projection into Arbace.
- Recorded in CLAUDE.md ("Direction", prior art), README and AGENDA.

## 2026-10-07: Formats target the Clojure reader

- The user's rule, now in CLAUDE.md: data and source formats target the Clojure reader (`read`),
  not strict EDN; where in doubt, the Clojure reader wins.
- Checked against the tools. j2c writes Clojure 1.12 syntax (param-tags, array class
  symbols). Its `--check` counts a file as unreadable only when `clojure.core/read` fails, and
  lists `clojure.edn/read` failures only as information. The class forms compiler and the boot
  read sources with `read`.
- For uniformity, `arbace.j2c.coverage` now reads `j2c-report.edn` files with `read-string`
  (with `*read-eval*` off) instead of `clojure.edn/read-string`.

## 2026-10-07: Survey and plan for g2c (Go as Arbace forms)

- At the user's request a background agent surveyed a Go → Clojure mechanism analogous to j2c:
  `doc/G2C-SURVEY.md` (`379a363`, `f5330af`). Research only; nothing under `arbace/` changed.
- Recommendation:
  - The front end is a small Go helper on `go/parser` and `go/types` that emits the typed tree
    for `clojure.core/read`. An experiment (`.tmp/g2c/`, not committed) dumped every package
    tried, `runtime` included, deterministically.
  - The first back end prints Go forms back to Go, verified by round-tripping `$GOROOT`.
  - Phase a (JVM): pure-Go packages plus a hand-written runtime shim, type-checked for
    `wasip1/wasm`.
  - Phase b (bare metal): first Go plus TamaGo compiles Arbace-emitted Go forms inside go-whim's
    KVM/HVF monitor. Then gc is replaced layer by layer.
- Hard parts: on the JVM, value structs, pointers, byte strings, unsigned arithmetic,
  structural interfaces, `reflect`, `unsafe`. On an own runtime, what Go's runtime demands of its
  compiler.
- Measured: JDK 26 virtual threads match Go for spawning and uncontended channel use but degrade
  under parallel scheduling, so Go's own channel algorithm would be converted.
- Ten open questions for the user are in §9 of the document. No decision taken yet.

## 2026-10-07: Compiled namespaces, a reproducible jar, the JDK AOT cache and bin/arbace

- First item of the modern-compiler survey, done by a background agent (`8c9f2d3`). Details and
  measurements are in `doc/VENDOR-NOTES.md` ("Compiled namespaces, the jar and the AOT cache").
- Every stage AOT-compiles, with its own compiler and with direct linking (as Clojure's release
  build does), the 35 namespaces of upstream's `build.xml` list plus `arbace.classes`. `arbace.j2c`
  stays source. Stages 1, 2 and 3 are byte-identical at 5,091 classes; no new nondeterminism
  turned up.
- `target/arbace.jar` (stage 2 plus all `arbace/**/*.clj`) is reproducible, and the build checks
  it against stage 3's jar. Sources are dated before their classes, because `RT.load` takes a
  class only when it is strictly newer. `target/arbace.aot` (JEP 514 one-step training over
  `test/aot-training.clj`) is the JDK AOT cache. It is not reproducible, so it is not compared.
- New launcher `bin/arbace`: the jar, `-XX:+UseCompactObjectHeaders` (11% less retained heap
  for small maps, same speed), and the cache when it is newer than the jar. It falls back
  cleanly when the cache is missing, stale or rejected.
- Clojure's suite now runs against each stage's compiled namespaces
  (`CLOJURE_TESTS_PRECOMPILED=1`), as upstream's does.
- Launch of `-e 1` (agent's hyperfine): from sources 2.14 s, compiled classes 0.48 s, jar with
  cache 0.16 s; frozen `clojure.main` 2.11 s. The main session measured `bin/arbace -e` at 0.21 s
  wall time against 2.0 s for the frozen Clojure. The first `defclass` takes about 55 ms.
- Gate rerun by the main session on `8c9f2d3`: `bin/build-arbace --suite` (fixpoint 5,091
  classes, jars equal, native tests, suite on stages 1 and 2 without regressions),
  `bin/class-forms-tests` and `bin/j2c-check --suite` all pass.
- Left open: the survey's other two "do first" items (`ClassFile.verify` in the checks,
  classfile version 70 in the Clojure compiler). `bin/arbace` does not notice a jar that is
  stale against edited sources.

## 2026-10-07: The JDK verifier in the build; classfile version 70

- The survey's other two "do first" items, done by a background agent (`e8a5e30`).
- `arbace.classes.verify` runs `java.lang.classfile.ClassFile/verify` (JEP 484) over a class
  tree, in parallel. It answers hierarchy questions from the tree itself, then the JDK. It also
  fails any class whose major version is not the running JDK's. `bin/build-arbace` runs it on
  stages 1 and 2 and fails the build on any error. The new native test `verify_test` verifies
  the 50 classes of a compiled sample namespace (fns, protocols, records, `proxy`, `gen-class`,
  `defclass`, ...) and checks that a broken class is rejected.
- `arbace/lang/Compiler.clj`: `JVM_BYTECODE_VERSION` is the running JDK's (`44 + feature`, so
  70), no longer `V17`. Fn classes, deftype and compile stubs, `gen-class`/`gen-interface` and
  proxies follow it. It is hand change 7 in `doc/VENDOR-NOTES.md`.
- Gate rerun by the main session on `e8a5e30`:
  - stages 1 and 2 verify, 5,091 classes each with 0 errors (about 0.9 s each), and all are
    version 70;
  - stages 1, 2 and 3 are identical, and the jar equals stage 3's;
  - native tests: 27 tests, 2,345 assertions;
  - the suite on stages 1 and 2 shows no regressions;
  - `bin/class-forms-tests` and `bin/j2c-check --suite` pass.
- With this, all three "do first" items of `doc/MODERN-COMPILER.md` are done.
- The user's question on class loading: a REPL `defn` is compiled to bytes and defined by
  `DynamicClassLoader` in memory. Class files are written only under `*compile-files*`. Hidden
  classes stay rejected (survey §4.5).

## 2026-10-07: Decisions on the modern-compiler survey

The user decided, on the main session's recommendations:
- Start all four "do next" items. Each is measured on its own and kept only if it pays off:
  - constant dynamic for the compiler's constants;
  - `invokedynamic` keyword sites;
  - `invokedynamic` reflective calls with Reflector's exact choice and messages;
  - an opt-in virtual-thread executor for `send-off`, `future` and `pmap`.
- Keep the vendored ASM. The `java.lang.classfile` API is not adopted: ASM is portable Arbace
  code, and the JDK's verifier already gives the API's main benefit. Vendoring the JDK's
  implementation through j2c stays a later option.
- Keep the Var indirection for calls. No `invokedynamic` for Var calls, and no direct linking by
  default for user code; REPL redefinition stays free.
- Fold three smaller items into this round: VarHandles in `Atom`, `StringConcatFactory` for
  `str`, and a `jlink` runtime image.

## 2026-10-07: Decisions on g2c's open questions

The user answered the ten open questions of `doc/G2C-SURVEY.md` §9, all as the main session
recommended:
1. **Phases:** first the front end, the Go forms spec and the round trip back to Go (G0-G2).
   Then the box (B1). Phase a, the Go standard library on the JVM (G3-G6), is skipped, since
   the `arbace-for-java-26` freeze would leave it behind.
2. **The box via gc:** yes. The first bare-metal Arbace is built by Go's toolchain plus TamaGo
   and run by go-whim's monitor. "Arbace all the way down" is reached by replacing gc layer by
   layer, with gc as the reference.
3. **Operators:** inside Go function bodies, `+`, `<<` and the rest mean Go's operators at the
   operands' type (a "Go context"). This is a deliberate exception to "Clojure keeps its
   meaning".
4. **Names:** the survey's proposal:
   - `go/type`, `go/func`, `go/method`, `go/var`, `go/const`;
   - `(addr x)` and `@p`;
   - core.async's `<!` and `>!`;
   - `(values ...)` binding targets;
   - Go identifiers verbatim.
5. **Build configuration:** `GOOS=tamago` for amd64 and arm64, the box's own (not wasip1).
6. **Go version:** pinned to TamaGo's release, go1.27.1 now. Claude proposes moves, the user
   approves, and the journal records them.
7. **Build tags:** one configuration per conversion. The helper resolves tags and the forms
   record the configuration.
8. **The Go helper:** lives in this repository under `tools/`, seeded from the survey's
   `godump`, under EPL. BSD-3-Clause goes into `LICENSE.md` once converted Go code is kept.
9. **Converted output:** regenerated, not tracked, as for j2c, until a package becomes
   hand-maintained source by a recorded decision.
10. **go-lisp:** its coverage tables, round-trip method and corpus are reused. No `.lgo` back
    end; go-lisp stays a separate tool.

## 2026-10-07: Pre-freeze work chosen

- Asked what remains before freezing `arbace-for-java-26`, the user chose all four proposals:
  1. compile the converted jdk26u and compare it with javac (j2c had only converted it);
  2. restore clojure.spec, which the seed stubbed out and which accounts for the suite's 32
     failing assertions;
  3. close the remaining todo rows;
  4. benchmarks against upstream Clojure, CI, and a freeze kit.
- The first started at once. The others wait for a free agent slot, at about four at a time.
  Item 4 goes last, so that it measures the final state.

## 2026-10-07: Atom VarHandle (reverted), opt-in virtual threads, the jlink image

- Three of the survey decisions, done by a background agent and measured:
  - **`Atom` on a `VarHandle`** (`298ad15`), with the state a volatile field of the atom. It
    saved 16 bytes per atom and made uncontended `swap!` slightly faster (67 → 64 ms per 10M).
    But one atom swapped by 8 threads became about 25% slower (1.85 s → 2.3-2.5 s); the
    suspected cause is the CAS sharing a cache line with `validator`/`watches`. **Reverted at
    the user's decision** (`689bafa`): atoms exist for concurrent use, and few exist.
  - **Opt-in virtual-thread executor** (`3a3e9d1`, hand change 9): `-Darbace.virtual-threads=true`
    or `arbace.lang.Agent/newVirtualThreadExecutor` with `set-agent-send-off-executor!`. It
    covers `send-off`, `future`, `pmap`, `pcalls` and `pvalues`; `send` stays on platform
    threads. Example: 10k futures sleeping 100 ms took 231 → 120 ms and 814 → 238 MB RSS, with
    no CPU-bound regression. Virtual threads are daemons, so the JVM does not wait for pending
    futures (documented in the README).
  - **`jlink` image** (`9c33091`): `bin/arbace-image` or `bin/build-arbace --image` builds
    `target/arbace-image`. It holds the JDK trimmed to the jar's modules, the jar, an AOT cache
    trained by the image's own java, and `bin/arbace`. It is 131 MB (42 MB as `.tar.gz`), runs
    without a JDK, and launches as fast as `bin/arbace` (about 0.17 s).
- JDK 26.0.2 problem found: with AOT class linking, the cache of a jlink image with certain
  module sets (e.g. `java.base,java.sql`) stops the JVM at startup ("Unexpected exception when
  loading aot-linked classes"; `InternalError` from `ClassLoader.registerAsParallelCapable` in
  `AppClassLoader.<clinit>`). `java.base` alone, all modules and JDK 25 are fine. Workaround: the
  image adds `jdk.unsupported.desktop`, and the script retrains with `-XX:-AOTClassLinking` if a
  test launch fails. It may be worth reporting upstream.
- Checked after the revert by the main session: `bin/build-arbace` (fixpoint 5,091 classes,
  verifier, native tests 30/2,367 pass). The full gate runs again once the parallel agents have
  pushed.

## 2026-10-07: invokedynamic reflective call sites (kept)

- Done by a background agent (`e81cf41`, hand change 10 in `doc/VENDOR-NOTES.md`).
- How it works:
  - Unresolved interop calls emit `invokedynamic` to `arbace.lang.ReflectorCallSite`, a
    `MutableCallSite`. This covers instance methods, no-argument members, static methods and
    constructors on JDK classes.
  - Each site caches the member `Reflector` itself selects, guarded on the receiver class, and
    on the argument classes where the choice depends on them. It holds up to 8 entries, then
    turns megamorphic.
  - Everything else, including every error, goes through `Reflector` unchanged. The selection
    logic was moved inside `Reflector`, not changed, so both paths share it.
  - User classes, field writes and calls with more than 20 arguments keep the old path, so
    REPL redefinition and JEP 500 are unaffected.
- Measured by the agent, per un-hinted call: `.length` 1,053 → 2 ns, `.get` 492 → 8 ns,
  `Math/abs` 2,041 → 1.2 ns. Megamorphic `.size` over 10 classes went 690 → 14-21 ns.
  Startup is unchanged. The cost is the link of each site on every launch, since the AOT cache
  does not pre-resolve custom bootstraps: up to about 0.1 ms more for a site run only a few
  times in a cold JVM.
- The main session's check: 2M un-hinted `.length`/`.get` calls take about 4 ms on Arbace,
  against about 3.6 s on the frozen Clojure. `bin/build-arbace` gives a fixpoint at 5,093
  classes, and the native tests (38 tests, 2,722 assertions) pass.

## 2026-10-07: clojure.spec restored (pre-freeze item 2)

- Done by a background agent (`d9ecda7`). The full record is `doc/VENDOR-NOTES.md`, "Spec".
- **Versions.** Upstream Clojure `98d735fab02f`'s `pom.xml` depends on
  `org.clojure/spec.alpha 0.6.249` and `org.clojure/core.specs.alpha 0.6.133-alpha10`. Both were
  fetched from their repositories at those tags, and their `.clj` files equal those in the Maven
  jars.
  - https://github.com/clojure/spec.alpha tag `v0.6.249`, commit
    `3d1efb353b8a95c699b4051ba8273568c21f874e`: `src/main/clojure/clojure/spec/alpha.clj`,
    `spec/gen/alpha.clj` and `spec/test/alpha.clj` became `arbace/spec/alpha.clj`,
    `arbace/spec/gen/alpha.clj` and `arbace/spec/test/alpha.clj`.
  - https://github.com/clojure/core.specs.alpha tag `v0.6.133-alpha10`, commit
    `75875946b7e827a1cec91ce645d5e1dcfc475e49`: `src/main/clojure/clojure/core/specs/alpha.clj`
    became `arbace/core/specs/alpha.clj`.
  - Both are EPL-1.0, recorded in `LICENSE.md`, `doc/ARBACE.md` and an origin comment in each
    file.
- **Derivation.** The new `bin/vendor-spec` clones the two tags into `.tmp/vendor-spec/`,
  checks the commits, and runs the new command `arbace.j2c.rename vendor-lib`. The renaming
  rules gained `clojure.spec` and `clojure.core.specs.alpha`; `clojure.test.check` keeps its
  name. One hand edit: `arbace.spec.gen.alpha` excludes `arbace.core/return`, which is a class
  form.
- **Un-stubbed in `arbace/`, as upstream has it** (the frozen `clojure/` keeps its stub):
  - `main.clj` and `repl.clj` require spec, for `*explain-out*`, spec explanations in errors,
    and `doc`;
  - `RT.instrumentMacros` reads `arbace.spec.skip-macros`, and `checkSpecAsserts` reads
    `arbace.spec.check-asserts`;
  - the Compiler's macro check loads `arbace.spec.alpha` and `arbace.core.specs.alpha`;
  - `Compile` loads core.specs before compiling, which a seed fix had dropped.
- **Build.** Each stage compiles the spec namespaces in a second `Compile` run with
  `-Darbace.spec.skip-macros=true` (as spec.alpha's own build does), then
  `arbace.core.specs.alpha`. It must be a separate JVM, because `Compile` already loads
  core.specs, and recompiling spec in that JVM would reload its protocols.
- **Suite.** The new `test/arbace-results.edn` (everything passing) is the reference in rename
  mode. `test/baseline-results.edn` stays the frozen baseline's reference, with its 32 spec
  failures. spec.alpha's own tests pass on stage 2 too (13 tests, 174 assertions).
- **Main session's check on `d9ecda7`:** stages 1, 2 and 3 are identical at 5,627 classes, and
  all verify at version 70. The native tests pass (38 tests, 2,722 assertions). **Clojure's
  suite on stages 1 and 2: 809 tests, 20,750 of 20,750 assertions, test.generative 27 of 27.**
  `(let [a] a)` fails with spec's "failed: even-number-of-forms?" message.
- Correction to the class loading answer above: the "temporary external file" the user saw is
  most likely the error report a non-REPL run writes on an uncaught error (`Full report at:
  /tmp/arbace-NNN.edn`, Clojure 1.10+'s `clojure.main/report-error`; renamed from `clojure-`).
  It is a report of the error, not a class file. Compiled fns still never touch the disk.

## 2026-10-07: The remaining todo rows closed (pre-freeze item 3)

- Done by a background agent (`6c62f61`, `e06e81a`, `28e24d5`, `b75b87e`):
  - `deftype*` (and `import*`) now work inside class bodies and handed-over fns: Clojure's
    compiler defines the class during analysis.
  - Handed-over reify/deftype classes get Clojure's covariant bridges (`ACC_BRIDGE` only).
  - Clojure-meaning code chooses overloads as Clojure's compiler does (`getMatchingParams`,
    its errors, reflection otherwise).
  - `SecurityManager` was dropped from `RT`'s default imports (hand change 11; upstream still
    imports it).
- SPEC §9.5 was amended to match. The main session renumbered the hand change, which had
  clashed with the reverted Atom entry 8.
- Checked by the main session: `bin/build-arbace --suite` passes (see the log of this commit).

## 2026-10-07: Keyword sites and `str` as invokedynamic (kept); condy constants (not kept)

- Done by a background agent and measured. The machine was heavily loaded; medians come from
  interleaved runs.
  - **Condy constants: not kept, not pushed.** `<clinit>` methods went from 4,237 to 1,238 and
    the classes got 0.8% smaller. Launch and namespace loading did not change, with or without
    the AOT cache, which does not pre-resolve app condys. Condys in `__init` classes were
    slower. The experiment is kept on the local branch `condy-item1-experiment` (`bdd773b`).
  - **Keyword invoke sites** (`7fb2da7`, hand change 12): `invokedynamic` to the new
    `arbace.lang.KeywordInvokeSite`.
    - It always computes `(get x :k)`. After 256 calls it links a cache of up to 4 exact-class
      guards, and becomes `RT.get` from the fifth class on.
    - Lookups got faster: records 7.8 → 5.1 ns, array maps 10.6 → 7.3 ns. Classes are 6.6%
      smaller and launch is unchanged.
    - The 256-call threshold avoids spinning LambdaForms at launch.
  - **`str` with 2-99 arguments** (`65120ab`, hand change 13): `StringConcatFactory`, with
    constants folded into the recipe. Behaviour is identical: nil gives "", and a null
    `toString` on the first argument throws as before. It is 1.5-2.5x faster, and launch is
    unchanged because the AOT cache pre-resolves these sites. It acts like an `:inline`, so a
    redefinition of `str` does not reach compiled calls. A stack trace from a throwing
    `toString` no longer has the `core$str` frame.
  - `LICENSE.md` now states that Arbace's own files under `arbace/` are EPL-1.0.
- The main session checked `65120ab`:
  - `bin/build-arbace --suite`: stages 1-3 identical at 5,656 classes, the verifier clean,
    native tests 47/2,780, and the suite 20,750/20,750 on stages 1 and 2;
  - `(str "a" nil 1 \c (:x p) :k)` gives `"a1c1:k"`;
  - 1M record lookups plus `str` calls take about 12 ms warm.
- Pre-freeze item 4 (benchmarks, CI, freeze kit) started at the user's request. The freeze
  kit is only prepared: the branch and tag are created after the user confirms.

## 2026-10-07: CI only for the frozen branch

- The user's decision: main's progress is not tied to GitHub's infrastructure yet. The gate
  workflow (`.github/workflows/gate.yml`, added in `e68c480`) runs automatically only on pushes
  to `arbace-for-java-26`; it can be started by hand on any ref (`workflow_dispatch`)
  (`44cf328`). The in-progress run on main was cancelled. main is gated by running the checks
  locally, as before.

## 2026-10-07: Plan for clojure/ after the freeze

- The user asked whether `clojure/` is still needed. It still serves as:
  - stage 0 of the bootstrap and of `bin/class-forms-tests`;
  - j2c's regression corpus (`bin/j2c-check`);
  - the reference for `bin/clojure-tests` (`test/baseline-results.edn`);
  - the source for `bin/vendor-arbace` and `bin/vendor-spec`.
- It is 5.4 MB, 1,044 files with 812 tracked classes. It differs from upstream `98d735fab02f`
  by the spec stub, the separately pinned ASM and the two approved fixes.
- Agreed plan (in AGENDA): keep it until the freeze and on `arbace-for-java-26` for good. On main
  after the break, replace it, preferably by a Go-style binary seed (the freeze tag's jar,
  pinned by hash), otherwise by a pinned upstream fetch plus patches. Decide at the break.

## 2026-10-07: The converted jdk26u compiled and compared with javac (pre-freeze item 1)

- Done by a background agent (`4261ec6` .. `ee37d61`). Details and the per-module table are in
  `doc/classes/CONVERTER-NOTES.md`, "The converted JDK".
- New `bin/j2c-check --jdk [MODULE...]` (about 10 minutes, not in the default gate):
  - it converts each module of jdk26u `src/*/share/classes` (`baf63fbe42b8`);
  - it compiles the originals twice with javac as the reference, with the JDK build's options;
  - it compiles the forms with the class forms compiler and compares every class's shape;
  - the report is `.tmp/j2c-jdk/report.md`.
- Before → after: shape-identical files 11,699 → **12,376 of 12,444 (99.5%)**, differing 512 →
  65, compile errors 173 → 3, classes compiled 22,119 → 24,127.
- Fixed:
  - compiler gaps in constants and literals, branch types and lub, the class environment,
    bridges, InnerClasses, null checks, JEP 513 prologues and `java.lang.Object` itself,
    lambda, anonymous and local class details, and switches and records;
  - converter gaps, including a real semantic bug: a `try` used as a value returned the wrong
    value;
  - type annotations, which now round-trip (new sample `TypeAnns.java`).
- Remaining (68 files):
  - `switch` fall-through duplicating a lambda or anonymous class;
  - javac's anonymous-class numbering in chained calls;
  - InnerClasses entries for classes named only in javac's frames;
  - `this.k` on an inherited constant;
  - three compile errors (`PackageBuilder`, `Attr`, `ModuleDescriptor`);
  - module-infos, which are not converted yet but are feasible.
- Spec amendments 16-19 (§4.8 `^:diamond`, §5.12 `^:method-ref` on a `T[]::new` lambda, §4.11
  `&` in variable arity records, §4.4 type annotations in code) were **accepted by the user**
  and are marked so in SPEC.md.
- The main session checked `ee37d61` with the full gate:
  - `bin/build-arbace --suite`: stages 1-3 identical at 5,756 classes, the verifier clean,
    native tests 47/2,780, and the suite 20,750/20,750 on stages 1 and 2;
  - `bin/class-forms-tests`: 64/133;
  - `bin/j2c-check --suite`: baseline and samples identical, no regressions.

## 2026-10-07: Benchmarks, CI and the freeze kit (pre-freeze item 4)

- Done by a background agent (`e68c480` .. `3179442`); the main session reviewed it.
- Benchmarks (`5fe500e`): `bin/arbace-bench`, `test/bench/{workload,driver,repl-session}.clj`,
  results in `doc/BENCHMARKS.md`.
  - Compared on JDK 26.0.2.1: Arbace `e311a30` (`bin/arbace`: the jar plus its AOT cache);
    Clojure 1.12.6 from Maven Central (spec.alpha 0.5.238, core.specs.alpha 0.4.74; SHA-256
    sums in BENCHMARKS.md, downloaded into `.tmp/bench/lib`, not vendored); the frozen
    `clojure/` AOT-compiled with direct linking; and, for startup only, Clojure 1.12.6 with a
    JDK AOT cache trained like Arbace's.
  - Own harness, no libraries: one workload file run unchanged by all three, a fresh JVM per
    measurement, interleaved forks, load average recorded. Alternatives considered: JMH and
    criterium, rejected because the same source had to run on Arbace's renamed namespaces.
  - Results (quiet run, load 1-8): `-e 1` 184 ms vs 571 ms; a REPL session 417 vs 1,070 ms;
    un-hinted interop about 580x faster; multi-argument `str` about 10x; keyword lookups
    20-33% faster. The startup gain is the JDK AOT cache: Clojure with the same cache starts
    in 190 ms. Geometric mean of the ratios 0.75; without the interop and `str` outliers 0.98
    (the frozen baseline: 0.96).
  - Slower on Arbace in every run: `into-xform` (1.2-1.6x), `reduce-vector` (1.13-1.20x),
    `vector-transient` (1.0-1.6x). Cause not found: the call sites' bytecode is identical, the
    hot runtime methods differ from javac's by a few bytes (`iload/iadd` for `iinc`, a branch
    for a boolean `instanceof`), and `-XX:+PrintInlining` shows the same inlining. A lead for
    the class forms compiler.
  - Raw results: `.tmp/bench/full2.edn` (quiet) and `full1.edn` (under load), not tracked.
- CI (`e68c480`): `.github/workflows/gate.yml` runs the gate (`bin/build-arbace --suite`,
  native tests, `bin/class-forms-tests`, `bin/j2c-check --suite`) on Temurin 26.0.2.1 via
  actions/setup-java@v5, with `-Xmx4g`/`-Xmx5g` for the 2-core 7 GB runner; about 45 minutes.
  Its trigger is pushes to `arbace-for-java-26` and `workflow_dispatch` only (`44cf328`, the
  user's decision not to tie main to GitHub). Manual runs passed at `e311a30`
  (actions run 37684486653) and `3179442` (run 37697838281).
- `ffdb37f`: `bin/native-tests` runs with `-XX:-OmitStackTraceInFastThrow`. Under load,
  `native.reflect-test/overloads-by-runtime-types` failed about once in five runs: C2 threw a
  preallocated NullPointerException without a message, and the test compares messages.
- Freeze kit (`3179442`): `doc/FREEZE.md` (what the branch holds, how to build, verify and run
  it, CI, benchmarks, known limits, what changes on main after the break) and `bin/freeze`. It
  is a dry run by default; `--yes` creates the branch `arbace-for-java-26` and the annotated tag
  `arbace-for-java-26-v1` and pushes them, after checking a clean tree at `origin/main`, the
  README section, that neither exists, and a successful gate run on that commit (GitHub, or
  `--run-gate` locally). Tested as a dry run in a throwaway clone.
- The README section `## Arbace for Java 26` is commit `c57969f` on the local branch
  `freeze-readme`, kept off main until the branch exists, since it describes it as existing.
- The main session checked `3179442` with the full local gate: stages 1-3 identical at 5,756
  classes, the verifier clean, native tests 47/2,780, the suite 20,750/20,750 on stages 1 and
  2, `bin/class-forms-tests` 64/133, `bin/j2c-check --suite` without regressions.
- Not created: the branch and the tag wait for the user's confirmation.

## 2026-10-08: "A Clojure derivative", and a compact README

- The user corrected the project's definition: Arbace is not "a from-scratch reimplementation of
  Clojure in Clojure" (the first entry's words), since much of it is vendored; it is "a Clojure
  derivative written entirely in Clojure". CLAUDE.md and the README now say so. Past entries
  keep their words.
- The README was rewritten as a compact abstract, at the user's request, around the arc:
  Clojure extended to represent its own implementation (the class forms, j2c, the vendored
  Clojure as all `.clj`), modernized for Java 26, and evolving to represent its runtime too,
  down to the bare metal. It keeps the "Arbace for Java 26" section that `bin/freeze` requires,
  worded to hold before and after the freeze, two commands to build and run, and the
  documentation list.
- Moved out: the running details (the cache, the image's modules, the virtual-thread option) to
  `doc/FREEZE.md`'s "Build, run, verify"; their full accounts were already in
  `doc/VENDOR-NOTES.md`. Prior art is down to one line of links (CLAUDE.md keeps the
  annotated list).
- The README section prepared by the freeze agent (`c57969f`, local branch `freeze-readme`) is
  superseded by the new README's section, and the branch was deleted.

## 2026-10-08: After the freeze, `clojure/` is replaced by a binary seed

- The user decided the open question of the 2026-10-07 entry on `clojure/`: on main after the
  break, `clojure/` is replaced by a Go-style binary seed, the jar built from the freeze tag
  `arbace-for-java-26-v1`, pinned by hash, as stage 0 of the bootstrap.
- The alternative, a pinned fetch of upstream `98d735fab02f` plus the recorded patches, is not
  taken. `clojure/` stays on `arbace-for-java-26` for good.
