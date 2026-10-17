# c2a: Clojure → Arbace

c2a translates Clojure source into Arbace source: `.clj` and `.cljc` files in, `.ae` files out,
their layout and comments kept. Arbace is a Clojure derivative ([VENDOR-NOTES.md](VENDOR-NOTES.md)):
its core and the rest of Clojure's namespaces are named `arbace.*`, its sources are `.ae` files,
and it differs from Clojure in a few places on purpose (VENDOR-NOTES.md, "Dialect divergences").
c2a makes the source changes these call for, by a table of rules
(`arbace/c2a/rules.ae`), so later divergences of the dialects are new rules.

It began as `arbace.j2c.rename`, the renaming rules that derived `arbace/` from Clojure once
(2026-10-07) and renamed Clojure's test suite for every run since; those rules are c2a's rule
`:rename`, unchanged. `bin/clojure-tests` translates the suite and its test libraries with
c2a.

## Use

```sh
bin/c2a -o OUT SRC...                # translate files and trees into the directory OUT
bin/c2a --in-place SRC...            # translate them where they are (x.clj becomes x.ae)
bin/c2a --in-place --keep-ext SRC... # rename in place, names kept (bin/clojure-tests' mode)
bin/c2a --stdin [--kind cljc --ae]   # standard input to standard output
bin/c2a --rules                      # the rules and the dialect divergences
```

- A directory `SRC` is translated with its relative paths: its contents land in `OUT`, so
  `bin/c2a -o lib/ae jar-a/ jar-b/` merges two libraries' trees.
- `.clj` and `.cljc` files become `.ae` files. A `.cljc` file's reader conditionals are
  resolved, since the reader reads a `.ae` file without them. When `x.clj` and `x.cljc` are
  both there, `x.clj` is translated, as Clojure's reader prefers it, and `x.cljc` is skipped
  with a message. An `x.ae` that is already there is kept, and `x.clj` is skipped, since
  Arbace's loader takes the `.ae` first.
- `data_readers.clj` and `data_readers.cljc` become `data_readers.ae` by the same rules (a
  `.cljc`'s conditionals resolved for `:clj`), since Arbace reads `data_readers.ae` at the
  class path's roots before the other two (VENDOR-NOTES.md, hand change 21). Arbace reads and
  merges all three, so a file that would collide keeps its name rather than being left out:
  a `data_readers.clj` beside a `data_readers.ae`, a `data_readers.cljc` beside either. Its text
  is renamed, and a kept `.cljc` keeps its conditionals. The rule is per directory: two
  libraries merged into one `OUT` by `-o` still overwrite each other's `data_readers.ae`, as
  any two files at the same path do.
- `project.clj` keeps its name, because Leiningen finds it by that name. Its text is renamed.
- `.java` files are renamed as text and keep their names, since repr/java reads them
  ([classes/CONVERTER-NOTES.md](classes/CONVERTER-NOTES.md)): `import clojure.lang.IFn;`
  becomes `import arbace.lang.IFn;`.
- With `-o`, other files are copied as they are, except class files. In place, they stay as
  they are.
- `--keep-ext` keeps the `.clj` and `.cljc` names (`data_readers` files' too), and with them a
  `.cljc` file's conditionals. Arbace's reader selects their `:clj` branches, as Clojure's does.
  `bin/clojure-tests` uses it, because the reference results name the suite's files.
- `--text EXT` renames files ending in `EXT` as text too, keeping their names (`--text .edn`).
- `--skip ID,...` leaves rules out. `-q` drops the summary line, which counts the files
  translated, those turned into `.ae`, those unchanged, copied and skipped, and the errors.
- `bin/c2a` runs on Arbace's Go executable, the tools' host (`bin/lib/tools.bash`'s
  `arbace_tool`, `bin/lib/host.bash`: `ARBACE_HOST_EXE`, else stage 0 built from the Go seed;
  standalone S4; `target/stage2` until then). c2a is embedded in the executable, whose embedded
  sources come before `ARBACE_PATH`: `bin/c2a` loads its four namespaces from the checkout
  (`load-file`, then marked loaded: `c2a_sources`), so the checked-out c2a runs, not the one
  the executable was built with.
- The namespaces are `arbace.c2a.main` (the command line and the trees), `arbace.c2a.rules`
  (the table), `arbace.c2a.text` (the lexical reader) and `arbace.c2a.rename` (the rename's
  patterns).

## The rules

They apply in this order. Each is a map in `arbace.c2a.rules/rules` with `:id`, `:doc`,
`:applies` (whether the rule applies to a file, given its kind `:clj`, `:cljc`, `:java` or
`:text`, whether it becomes a `.ae` file, and the reader's features) and `:rewrite` (text to
text).

| rule | applies to | what it rewrites |
|---|---|---|
| `:reader-conditionals` | a `.cljc` file becoming `.ae` | Each `#?(...)` becomes the text of the branch the reader selects on Arbace: the first whose feature is `:clj` or `:default`, as LispReader chooses. A `#?@(...)` becomes the contents of that branch's list or vector. With no branch selected, the conditional becomes nothing. The branch's text is kept verbatim, nested conditionals resolved. The rule fails as the reader does on a splice at top level, an odd number of forms or a feature that is not a keyword. |
| `:rename` | every file | The names of the vendored Clojure, as `arbace.c2a.rename` renames them (VENDOR-NOTES.md, "What is renamed"): its namespaces exactly (`clojure.core` and the files loaded into it, `clojure.string`, ..., spec), the Java packages `clojure.lang`, `clojure.asm` (with `commons`, `signature`) and `clojure.java.api`, the keyword namespace `clojure.error`, the system properties `clojure.*` and the source paths `clojure/<ns>.clj`, which become `arbace/<ns>.ae`. These are written dotted, as paths, in descriptors, in regex literals, as qualified symbols and keywords, as munged class names, and in strings and comments too. Left alone: other libraries' names (`clojure.test.check`, `clojure.tools.*`, `clojure.core.async`), `refer-clojure`, `*clojure-version*`, prose and URLs. |
| `:proxy-methods` | every file | The methods every proxy class has and its field: `__initClojureFnMappings`, `__updateClojureFnMappings` and `__getClojureFnMappings` become `__initArbaceFnMappings` and the rest; `__clojureFnMap` becomes `__arbaceFnMap` (VENDOR-NOTES.md, hand change 2). This applies in strings too, where code names them for reflection. |
| `:core-names` | `.clj`, `.cljc` | Names `arbace.core` has and `clojure.core` has not are those marked `{:added "arbace"}`: the class forms' macros and operators, such as `return`, `switch`, `label`, `lambda` and `bit-and-int`. When a file defines one of them (`def`, `defn`, `defmacro`, ... at any depth), the name joins its `ns` form's `(:refer-clojure :exclude [...])`, as a library does for a core name it redefines. The name is added to an `:exclude` already there. If there is none, `:exclude [...]` is added to the `:refer-clojure`, or `(:refer-clojure :exclude [...])` to the `ns` form. Without the rule the library still works, but every load warns `return already refers to: #'arbace.core/return` (test.check's `generators` defines `return`). A file with `:only`, or without an `ns` form, is left as it is. |
| `:security-manager` | `.clj`, `.cljc` | An unqualified `SecurityManager` (as a symbol, `SecurityManager/x`, `SecurityManager.` or a hint) becomes `java.lang.SecurityManager`, because Arbace does not import it by default (hand change 11). A file that imports it is left as it is. |

The table in `arbace.c2a.rules/divergences` says, for eight of the divergences of
VENDOR-NOTES.md (not the REPL's banner, `data_readers.ae`, the store or "Without the JVM", which
it does not name; `gen-class` is under "Limits", below), which rule answers each, or why no
source rewrite can:

| divergence | rule |
|---|---|
| `reduce` without an init on an `IReduceInit`-only reducible | None: Arbace reduces what upstream rejects, so code that runs on Clojure runs the same. |
| `SecurityManager` is not a default import | `:security-manager` |
| A multimethod's ambiguity message | None: only the message's wording differs. A test comparing the message must be edited. |
| `bean` ignores explicit `BeanInfo` classes | None: this is what `bean` returns at run time, and nothing in the source names it. |
| Optional modules (`java.sql`, `java.xml`, `java.desktop`) | None: this depended on the JVM's runtime image, not the source (on `arbace-for-java-26`; on main since S4 the namespaces are removed, VENDOR-NOTES.md, "Without the JVM"). |
| Source files are `.ae` | c2a's output names, and `:reader-conditionals`. The vendored namespaces' source paths are renamed by `:rename`. |
| Names `arbace.core` adds | `:core-names` |
| Runtime-visible names | `:proxy-methods`. Thread names (`arbace-agent-send-pool-N`, ...), the error reports' file prefix and the socket server's thread names are data in strings, and are left alone (below, "Limits"). |

### Adding a rule

A later divergence that a source rewrite can answer becomes:

1. an entry in `rules` at its place in the order;
2. its divergence in `divergences`, and in VENDOR-NOTES.md's list;
3. unit tests in `test/c2a/c2a_test.ae`;
4. a row above.

If the rule changes what the suite or its libraries become, `bin/c2a-check --suite`'s digest
changes. Record the new digest once the suite passes on it. `bin/clojure-tests` translates the
libraries again on its own: it keeps a hash of c2a's sources beside them.

## How it reads

c2a does not read forms into data and print them back, which would lose layout, comments and
the reader's sugar. It rewrites text, guided by a lexical reader (`arbace.c2a.text`) that knows
where the Clojure reader's tokens and forms begin and end:

- strings, regex literals (`#"..."`), character literals (`\(`, `\newline`, `A`);
- comments (`;`, `#!`) and discarded forms (`#_`);
- collections, dispatch macros (`#{`, `#(`, `#'`, `#=`, `#?`, `#?@`, `##Inf`, `#:ns{`) and
  tagged literals (`#inst "..."`);
- metadata (`^`, `#^`), the quoting macros and tokens. A token runs until whitespace or a
  terminating macro character, as LispReader's do, so `a'b` and `foo#` are single tokens.

`:reader-conditionals`, `:core-names` and `:security-manager` use it, to work on whole forms
and on tokens outside strings and comments. `:rename` and `:proxy-methods` are regular
expressions over the whole text, comments and strings included, as names in strings
(`(require 'clojure.string)` built at run time, `"clojure.lang.RT"`) must change too.

## Checks

```sh
bin/c2a-check             # the unit tests (test/c2a/c2a_test.ae): the reader, each rule, the file plans, trees
bin/c2a-check --suite     # Clojure's suite translated, against the digest test/c2a/suite.sha256
bin/c2a-check --libs      # the suite's test libraries as .ae, run on the tools' host and a Go build
```

- `--suite`: the suite at the revision `bin/clojure-tests` pins (`98d735f`: `test/`,
  `src/script/` and the top-level files) is translated in place with `--keep-ext`. Its digest
  must equal the recorded one. On 2026-10-11 c2a's tree was byte for byte the one
  `arbace.j2c.rename` plus `bin/clojure-tests`' `java_interop.clj` `sed` made before c2a (the
  `sed` is now the rule `:proxy-methods`). The six libraries' trees were too, apart from
  `:core-names` adding `return` to test.check's `generators` exclusions. `bin/clojure-tests`
  then ran on c2a's trees with the reference results (below).
- `--libs`: test.check, test.generative, tools.namespace and data.generators, with
  tools.reader and java.classpath on which they depend (the jars `bin/clojure-tests`
  downloads), and a small library the check writes with a `data_readers.cljc` (`#c2a/point`,
  its reader selected by a conditional) and a `.cljc` namespace, become `.ae` files under
  `.tmp/c2a-check/ae`, and no `.clj` or `.cljc` is left.
  `test/c2a/libs.ae` then runs on them on the tools' host and on a Go build
  (`target/arbace-go/<arch>/arbace` or the argument, `ARBACE_PATH`), each output equal to
  `test/c2a/libs.out`, the JVM's (recorded on main's last `target/stage2`, 2026-10-12, when the
  host's was the same byte for byte; until standalone S4 the JVM build ran beside the Go one).
  It runs:
  - test.check: a property that holds; one that fails and shrinks to `[0 0 0 0]`; values
    generated from a fixed seed; a `defspec` under `arbace.test`;
  - data.generators on a fixed `Random`;
  - tools.reader and its EDN reader, with syntax-quote resolving to `arbace.core`;
  - tools.namespace: `ns` declarations parsed, a dependency graph sorted, a tracker;
  - test.generative: a spec's inputs from a fixed seed, and a short `run-suite`;
  - the `data_readers.ae`: its tag known at start, read once its namespace is loaded.

  Every check must hold, and the two builds' standard outputs must be equal. On 2026-10-11
  they were, 14 of 14 checks on each, and 15 of 15 with the `data_readers` library. Standard error is shown, not compared: the Go build
  warns once more, about reflection on `JarFile.getName` in tools.namespace's `find`, which
  is a gap in its reflection data, not a translation problem.

## Limits

What c2a cannot translate mechanically:

- **Class files.** AOT output (`foo__init.class`, `gen-class` and `deftype` classes in a jar)
  is bytecode compiled against `clojure.lang`. c2a skips it, so the library loads from source.
  A jar that ships only classes cannot be translated.
- **Java libraries.** Java code compiled against `clojure.lang` (a `.jar` of `.class` files)
  needs its `.java` sources renamed (c2a does that) and compiled again, or converted by repr/java
  for the Go build. `bin/clojure-tests --java-fixtures` does this for the suite's fixtures.
- **`gen-class` on Go.** The Go build has no `gen-class` (go/B1-PLAN.md, D6; out for good,
  go/JAVA-SURFACE.md): a library using it loads only on the frozen JVM Arbace
  (`arbace-for-java-26`). c2a cannot write class forms for it. A `defclass` by hand is the way.
- **Names built at run time.** `(symbol (str "clojure." "core"))`, `(str "clojure.lang." n)`
  and resource paths assembled from parts escape the rename.
- **Sources found by extension.** Code that looks for `.clj`/`.cljc` files (tools.namespace's
  `find` and `dir`, test.generative's runner over a directory) does not see `.ae` files. That
  is the library's own logic, which a rule could change only by knowing the library.
- **Layout that depends on name lengths.** `arbace` is one character shorter than `clojure`.
  Text laid out around a renamed name, such as a docstring's table or a test's expected
  pprint output, is not re-aligned. `bin/clojure-tests` fixes the one case in the suite
  (`test_pretty.clj`) with a `sed`.
- **A resolved conditional keeps its branch's indentation.** The selected branch is kept
  verbatim, so code that sat under `#?(:clj` keeps that indentation. The text means the same.
- **Strings that are data.** Thread names, the error reports' file prefix and the socket
  server's thread names say `arbace` (VENDOR-NOTES.md, hand change 2). Code matching them by
  string is not rewritten: such a string is data, and c2a cannot tell it from a message.
- **The `clojure.main` class.** Java that calls `clojure.main.main(...)` (a launcher) needs
  `arbace.lang.Main`. The rename makes it `arbace.main`, the namespace's name.
- **Other platforms' code.** `.cljs` files are copied, not translated. Arbace's features are
  `#{:clj}`, so `:cljs`, `:cljr` and other branches are dropped.
