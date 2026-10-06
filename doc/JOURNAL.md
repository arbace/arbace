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
