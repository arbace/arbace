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
