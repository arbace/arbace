# repr/clojure: Clojure ↔ Arbace

> Names: this bridge is repr/clojure (`arbace/repr/clojure.ae`, `arbace/repr/clojure/`,
> `bin/repr-clojure read`, `print` (since 2026-10-18), `roundtrip`, `bin/repr-clojure check`, this file `REPR-CLOJURE.md`) since 2026-10-17
> (the user's decision; doc/NAMING-SURVEY.md, "The second rename as built"); from 2026-10-11 it
> was c2a (`arbace.c2a.*`, `bin/c2a`, `bin/c2a-check`, `test/c2a/`, `C2A.md`), as the records of
> those days name it. This document uses the current names throughout, also where it records
> earlier work.

repr/clojure is Clojure's bridge, one of the `repr` family (read and print, as repr/java and
repr/go). Its `read` translates Clojure source into Arbace source: `.clj` and `.cljc` files in,
`.ae` files out, their layout and comments kept; its `print`, Arbace back to Clojure (`arbace.*`
to `clojure.*`, `.ae` to `.clj`), reverses each rule, so that Clojure read and printed back is
the same text byte for byte (below, "Print"). Clojure and Arbace share the reader, so the bridge runs between two dialects' texts. Arbace is a Clojure derivative ([VENDOR-NOTES.md](VENDOR-NOTES.md)):
its core and the rest of Clojure's namespaces are named `arbace.*`, its sources are `.ae` files,
and it differs from Clojure in a few places on purpose (VENDOR-NOTES.md, "Dialect divergences").
repr/clojure makes the source changes these call for, by a table of rules
(`arbace/repr/clojure/rules.ae`), so later divergences of the dialects are new rules.

It began as `arbace.j2c.rename`, the renaming rules that derived `arbace/` from Clojure once
(2026-10-07) and renamed Clojure's test suite for every run since; those rules are repr/clojure's rule
`:rename`, unchanged. `bin/clojure-tests` translates the suite and its test libraries with
repr/clojure.

## Use

```sh
bin/repr-clojure read -o OUT SRC...                # translate files and trees into the directory OUT
bin/repr-clojure read --in-place SRC...            # translate them where they are (x.clj becomes x.ae)
bin/repr-clojure read --in-place --keep-ext SRC... # rename in place, names kept (bin/clojure-tests' mode)
bin/repr-clojure read --stdin [--kind cljc --ae]   # standard input to standard output
bin/repr-clojure read --rules                      # the rules, what print cannot restore, the divergences
bin/repr-clojure print -o OUT SRC...               # print Arbace files and trees back as Clojure into OUT
bin/repr-clojure print --in-place SRC...           # print them where they are (x.ae becomes x.clj)
bin/repr-clojure print --stdin [--kind cljc --ae]  # standard input to standard output
bin/repr-clojure roundtrip [--keep-ext] SRC...     # read then print in memory, each file back byte for byte
bin/repr-clojure roundtrip --from-arbace SRC...    # print then read
```

- A directory `SRC` is translated with its relative paths: its contents land in `OUT`, so
  `bin/repr-clojure read -o lib/ae jar-a/ jar-b/` merges two libraries' trees.
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
- `bin/repr-clojure` runs on Arbace's Go executable, the tools' host (`bin/lib/tools.bash`'s
  `arbace_tool`, `bin/lib/host.bash`: `ARBACE_HOST_EXE`, else stage 0 built from the Go seed;
  standalone S4; `target/stage2` until then). repr/clojure is embedded in the executable, whose embedded
  sources come before `ARBACE_PATH`: `bin/repr-clojure` loads its four namespaces from the checkout
  (`load-file`, then marked loaded: `repr_clojure_sources`), so the checked-out repr/clojure runs, not the one
  the executable was built with.
- `print` takes `read`'s options (`--keep-ext` aside: it keeps the names `read` kept) and its
  plan reversed (below, "Print"). `roundtrip` takes `--known EDN --corpus NAME` (the
  differences expected, `{corpus {path reason}}`), `--from-arbace` and `-v`.
- The namespaces are `arbace.repr.clojure` (the command line, its verbs `read`, `print` and
  `roundtrip`, and the trees), `arbace.repr.clojure.rules`
  (the table, each rule with its reversal), `arbace.repr.clojure.text` (the lexical reader) and `arbace.repr.clojure.rename` (the rename's
  patterns).

## The rules

They apply in this order. Each is a map in `arbace.repr.clojure.rules/rules` with `:id`, `:doc`,
`:applies` (whether the rule applies to a file, given its kind `:clj`, `:cljc`, `:java` or
`:text`, whether it becomes a `.ae` file, and the reader's features), `:rewrite` (text to
text), and for `print` `:print` (the rewrite reversed), `:way` (`:reversible` or `:one-way`)
and `:loss` (what `print` cannot restore; below, "Print").

| rule | applies to | what it rewrites |
|---|---|---|
| `:reader-conditionals` | a `.cljc` file becoming `.ae` | Each `#?(...)` becomes the text of the branch the reader selects on Arbace: the first whose feature is `:clj` or `:default`, as LispReader chooses. A `#?@(...)` becomes the contents of that branch's list or vector. With no branch selected, the conditional becomes nothing. The branch's text is kept verbatim, nested conditionals resolved. The rule fails as the reader does on a splice at top level, an odd number of forms or a feature that is not a keyword. |
| `:rename` | every file | The names of the vendored Clojure, as `arbace.repr.clojure.rename` renames them (VENDOR-NOTES.md, "What is renamed"): its namespaces exactly (`clojure.core` and the files loaded into it, `clojure.string`, ..., spec), the Java packages `clojure.lang`, `clojure.asm` (with `commons`, `signature`) and `clojure.java.api`, the keyword namespace `clojure.error`, the system properties `clojure.*` and the source paths `clojure/<ns>.clj`, which become `arbace/<ns>.ae`. These are written dotted, as paths, in descriptors, in regex literals, as qualified symbols and keywords, as munged class names, and in strings and comments too. Left alone: other libraries' names (`clojure.test.check`, `clojure.tools.*`, `clojure.core.async`), `refer-clojure`, `*clojure-version*`, prose and URLs. |
| `:proxy-methods` | every file | The methods every proxy class has and its field: `__initClojureFnMappings`, `__updateClojureFnMappings` and `__getClojureFnMappings` become `__initArbaceFnMappings` and the rest; `__clojureFnMap` becomes `__arbaceFnMap` (VENDOR-NOTES.md, hand change 2). This applies in strings too, where code names them for reflection. |
| `:core-names` | `.clj`, `.cljc` | Names `arbace.core` has and `clojure.core` has not are those marked `{:added "arbace"}`: the Java forms' macros and operators, such as `return`, `switch`, `label`, `lambda` and `bit-and-int`. When a file defines one of them (`def`, `defn`, `defmacro`, ... at any depth), the name joins its `ns` form's `(:refer-clojure :exclude [...])`, as a library does for a core name it redefines. The name is added to an `:exclude` already there. If there is none, `:exclude [...]` is added to the `:refer-clojure`, or `(:refer-clojure :exclude [...])` to the `ns` form. Without the rule the library still works, but every load warns `return already refers to: #'arbace.core/return` (test.check's `generators` defines `return`). A file with `:only`, or without an `ns` form, is left as it is. |
| `:security-manager` | `.clj`, `.cljc` | An unqualified `SecurityManager` (as a symbol, `SecurityManager/x`, `SecurityManager.` or a hint) becomes `java.lang.SecurityManager`, because Arbace does not import it by default (hand change 11). A file that imports it is left as it is. |

The table in `arbace.repr.clojure.rules/divergences` says, for eight of the divergences of
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
| Source files are `.ae` | repr/clojure's output names, and `:reader-conditionals`. The vendored namespaces' source paths are renamed by `:rename`. |
| Names `arbace.core` adds | `:core-names` |
| Runtime-visible names | `:proxy-methods`. Thread names (`arbace-agent-send-pool-N`, ...), the error reports' file prefix and the socket server's thread names are data in strings, and are left alone (below, "Limits"). |

### Adding a rule

A later divergence that a source rewrite can answer becomes:

1. an entry in `rules` at its place in the order, with its `:print`, `:way` and `:loss`;
2. its divergence in `divergences`, and in VENDOR-NOTES.md's list;
3. unit tests in `test/repr/clojure/read_test.ae`, and of its reversal in `print_test.ae`;
4. a row above.

If the rule changes what the suite or its libraries become, `bin/repr-clojure check --suite`'s digest
changes. Record the new digest once the suite passes on it. `bin/clojure-tests` translates the
libraries again on its own: it keeps a hash of repr/clojure's sources beside them.

## How it reads

repr/clojure does not read forms into data and print them back, which would lose layout, comments and
the reader's sugar. It rewrites text, guided by a lexical reader (`arbace.repr.clojure.text`) that knows
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

## Print

`print` is `read` reversed: Arbace text back to Clojure's, file by file (`print-text`), and
the trees with read's plan reversed (`print-plan`). Built 2026-10-18, as the user decided on
2026-10-17 (doc/NAMING-SURVEY.md, "Added (2026-10-17, the user)": "rename c2a at the next
re-seed, print later", then "both next"). As `read` it rewrites text and keeps everything
else; the rules' `:print` functions apply in the reverse order of their `:rewrite`s.

### Each rule, reversed

| rule | way | `print` |
|---|---|---|
| `:reader-conditionals` | one way | Nothing to undo: the branches not selected are gone. A `.ae` made from a `.cljc` is printed as a `.clj` holding the `.cljc`'s `:clj` reading. |
| `:rename` | reversible | `arbace.repr.clojure.rename/unrename`: the same patterns, in the same contexts, finding `arbace` instead of `clojure` (`arbace.core` → `clojure.core`, `arbace.lang.RT`, `Larbace/lang/Var;`, `:arbace.error/phase`, ...), and the source paths `arbace/<ns>.ae` → `clojure/<ns>.clj`. |
| `:proxy-methods` | reversible | `__initArbaceFnMappings` and the rest, `__arbaceFnMap`, back to Clojure's names. |
| `:core-names` | reversible | `uncore-names`: what `core-names` added is one of three texts, by what the `ns` form had (a `(:refer-clojure :exclude [...])` at its end, an `:exclude [...]` at its `:refer-clojure`'s end, the names at its `:exclude`'s end). Each is tried, and kept only when `core-names` gives the text back from what is left, so `print` inverts `read` exactly; with none, the text is left as it is (Arbace source that excludes the names itself). |
| `:security-manager` | one way | `java.lang.SecurityManager` outside imports, in a file that does not import it, unqualified again (Clojure imports it by default). A Clojure file that wrote it qualified comes back unqualified: the same class. |

### The one-way rules

Decided here (the brief left it to this work): **the losses are documented, not recorded.**

- `:reader-conditionals`. Recording what `read` drops would mean keeping the whole conditional
  (every branch's text, and where it stood) beside the `.ae`: as metadata the `.ae` would no
  longer be the text it is (a `.ae` is read without conditionals, so they cannot stay in it),
  and as a side file a second source to keep in step. Neither is needed: the lossless path
  exists already, `--keep-ext`, which keeps a `.cljc` as a `.cljc` (Arbace's reader selects its
  `:clj` branches, as Clojure's does) and is what `bin/clojure-tests` uses; `print` then gives
  it back whole. What a `.ae` loses is exactly the other platforms' code, which Arbace cannot
  run. The round trip checks the loss is only that: a `.cljc` read into a `.ae` must come back
  as a `.clj` whose text is the `.cljc` with its conditionals resolved, byte for byte. A
  `--full` mode recording the conditionals is proposal CP1 (below), if wanted.
- `:security-manager`. Which uses were written qualified is lost, and nothing in the source of
  Clojure's sources, suite or libraries writes `SecurityManager` at all. `print` writes the
  short name, which is what Clojure code writes and means the same.

### Plans

| Arbace file | becomes |
|---|---|
| `x.ae` | `x.clj` (as `read` cannot tell, a `.ae` made from a `.cljc` too) |
| `data_readers.ae` | `data_readers.clj`; `data_readers.cljc` when a `data_readers.clj` is beside it (Clojure merges the two); left out when both are |
| `x.clj`, `x.cljc` | printed, keeping their names (read kept them: `--keep-ext`, `data_readers` files, `project.clj`); left out beside an `x.ae`, which is printed instead, as `read` left a `.cljc` beside a `.clj` (in place it stays as it was) |
| `.java`, `--text` files | printed, keeping their names |
| class files | left out |
| anything else | copied (with `-o`) |

A directory's names are taken before any of its files is renamed (`run`'s listing, for `read`
too), so the plan does not depend on the order files are written in place.

### Where print is not read's inverse

`read` maps Clojure's names onto Arbace's; it does not touch an `arbace.*` that is already
there. So `print` inverts it on what `read` makes, but an Arbace text that names `clojure.*`
itself (a provenance comment, "Converted from clojure/lang/Var.java"; a docstring about
upstream) or that defines `return` with no exclusion is not something `read` makes: printed
and read again, its `clojure.*` become `arbace.*`. The same holds the other way for a Clojure
text naming `arbace.*` (none of the corpora does). Such texts are outside `read`'s image; the
round trip from Arbace counts them apart and requires that their printed text makes the round
trip from Clojure.

### The round trip

`bin/repr-clojure check --roundtrip` (in `bin/gate --full`) runs `bin/repr-clojure roundtrip`
over each corpus, in memory: every file `read` translates must come back from `print` under
its name, byte for byte, except the known differences, `test/repr/clojure/roundtrip-known.edn`
(none; a new difference, or a known one that comes back equal, fails). On 2026-10-18:

| corpus | way | files | equal | apart |
|---|---|---|---|---|
| the frozen tree's `clojure/` (`frozen_clojure`: Clojure's `.clj` and `.java`, ASM's `.java`) | Clojure → Arbace → Clojure | 231 | 231 | |
| Clojure's suite (`98d735f`: `test/`, `src/script/`, the top-level files), `--keep-ext` | Clojure → Arbace → Clojure | 103 | 103 | |
| the suite into `.ae` files | Clojure → Arbace → Clojure | 103 | 102 | 1 `.cljc` (`reader.cljc`) back as its `:clj` reading |
| the suite's six test libraries (the jars of `--libs`), `--keep-ext` | Clojure → Arbace → Clojure | 33 | 33 | |
| the libraries into `.ae` files | Clojure → Arbace → Clojure | 33 | 21 | 12 `.cljc` (test.check, tools.reader) back as their `:clj` readings |
| `arbace/` | Arbace → Clojure → Arbace | 283 | 133 | 150 outside `read`'s image (they name `clojure.*` themselves: the Java forms' provenance lines, "Converted from clojure/lang/Var.java", spec's "Vendored from" lines, code and docstrings naming both dialects, as repr/clojure's own; `core.ae` defines `return` without excluding it), each printed text making the round trip from Clojure |

0 differences in all. `bin/repr-clojure check --suite` also prints the translated suite back in
place with the command (`print --in-place`) and compares the tree with Clojure's, byte for byte;
`bin/repr-clojure check` holds `print`'s unit tests (`test/repr/clojure/print_test.ae`: each
rule reversed, the samples of `read`'s tests read and printed back byte for byte, the plans, a
tree printed with `-o` and in place, the round trip of a tree).

The round trip found one fault of `read`, fixed with it: a `#?@` nested in a spliced branch
(the suite's `reader.cljc`, `[#?@(:clj [:a #?@(:clj [:b ...])])]`) failed with "splicing not
allowed at the top level", as the branch's text was resolved as if at the top level; it is now
resolved at the conditional's depth. The suite's digest (`--keep-ext`) is unchanged.

From Arbace the round trip is meaningful in that sense only: `arbace/` is hand-maintained since
the derivation, not `read`'s output, so most of its vendored namespaces are outside `read`'s
image; the 133 that are not come back byte for byte.

### Proposals

- **CP1** A `read --full` mode keeping a `.cljc`'s conditionals for `print` (a side file
  `x.cljc.ae-conditionals`, or the original `.cljc` kept beside the `.ae`), so that `.cljc` →
  `.ae` → `.cljc` is byte for byte too; not built, as `--keep-ext` is that path already.

Until the next re-seed the executable embeds the repr/clojure before `print`: `bin/repr-clojure`
loads the checkout's namespaces (`repr_clojure_sources`), so `print` runs on today's stage 0.

## Checks

```sh
bin/repr-clojure check             # the unit tests (test/repr/clojure/read_test.ae, print_test.ae): the reader, each rule and its reversal, the file plans, trees
bin/repr-clojure check --suite     # Clojure's suite translated, against the digest test/repr/clojure/suite.sha256, then printed back byte for byte
bin/repr-clojure check --roundtrip # the round trips (above, "The round trip"), against test/repr/clojure/roundtrip-known.edn
bin/repr-clojure check --libs      # the suite's test libraries as .ae, run on the tools' host and a Go build
```

`bin/gate --full` runs the first three (its step `repr-clojure`, about 1 minute on the tools'
host: 51 s on 2026-10-18).

- `--suite`: the suite at the revision `bin/clojure-tests` pins (`98d735f`: `test/`,
  `src/script/` and the top-level files) is translated in place with `--keep-ext`. Its digest
  must equal the recorded one. On 2026-10-11 repr/clojure's tree was byte for byte the one
  `arbace.j2c.rename` plus `bin/clojure-tests`' `java_interop.clj` `sed` made before repr/clojure (the
  `sed` is now the rule `:proxy-methods`). The six libraries' trees were too, apart from
  `:core-names` adding `return` to test.check's `generators` exclusions. `bin/clojure-tests`
  then ran on repr/clojure's trees with the reference results (below).
- `--libs`: test.check, test.generative, tools.namespace and data.generators, with
  tools.reader and java.classpath on which they depend (the jars `bin/clojure-tests`
  downloads), and a small library the check writes with a `data_readers.cljc` (`#repr-clojure/point`,
  its reader selected by a conditional) and a `.cljc` namespace, become `.ae` files under
  `.tmp/repr-clojure-check/ae`, and no `.clj` or `.cljc` is left.
  `test/repr/clojure/libs.ae` then runs on them on the tools' host and on a Go build
  (`target/arbace-go/<arch>/arbace` or the argument, `ARBACE_PATH`), each output equal to
  `test/repr/clojure/libs.out`, the JVM's (recorded on main's last `target/stage2`, 2026-10-12, when the
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

What repr/clojure cannot translate mechanically:

- **Class files.** AOT output (`foo__init.class`, `gen-class` and `deftype` classes in a jar)
  is bytecode compiled against `clojure.lang`. repr/clojure skips it, so the library loads from source.
  A jar that ships only classes cannot be translated.
- **Java libraries.** Java code compiled against `clojure.lang` (a `.jar` of `.class` files)
  needs its `.java` sources renamed (repr/clojure does that) and compiled again, or converted by repr/java
  for the Go build. `bin/clojure-tests --java-fixtures` does this for the suite's fixtures.
- **`gen-class` on Go.** The Go build has no `gen-class` (go/B1-PLAN.md, D6; out for good,
  go/JAVA-SURFACE.md): a library using it loads only on the frozen JVM Arbace
  (`arbace-for-java-26`). repr/clojure cannot write Java forms for it. A `defclass` by hand is the way.
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
  string is not rewritten: such a string is data, and repr/clojure cannot tell it from a message.
- **The `clojure.main` class.** Java that calls `clojure.main.main(...)` (a launcher) needs
  `arbace.lang.Main`. The rename makes it `arbace.main`, the namespace's name.
- **Other platforms' code.** `.cljs` files are copied, not translated. Arbace's features are
  `#{:clj}`, so `:cljs`, `:cljr` and other branches are dropped.

**Decided (2026-10-18, the user):** CP1 against the recommendation: a `read --full` mode to record
a `.cljc`'s reader conditionals, so `.cljc` → `.ae` → `.cljc` is byte for byte too (to build).
