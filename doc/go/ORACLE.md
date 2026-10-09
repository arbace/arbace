# The oracle: differential tests against the JVM Arbace

B1a builds Arbace as a static Go executable ([B1-PLAN.md](B1-PLAN.md)); its steps 3-5 (jrt,
the c2g-translated runtime, the evaluator) are each checked against the JVM Arbace, the
oracle. This document describes the oracle's corpora, their formats, the runner `bin/oracle`
and how to extend it. It depends on nothing of the Go work: the expected results are recorded
from `bin/arbace` (built by `bin/build-arbace`) and, for regex, from its JDK.

## Contents

Under `test/oracle/`:

| part | sources | what | size |
|---|---|---|---|
| forms | `forms/*.clj` (hand-written, by topic) | Clojure forms, evaluated in order | 6,898 forms, 14 files |
| forms | `forms/harvest/*.clj` (generated) | the self-contained expressions of the assertions of Clojure's test suite | 3,189 forms, 29 files |
| classes | `classes/*.clj` | operation scripts on the runtime's classes, by reflection | 8,943 steps, 18 files |
| regex | `regex/*.clj` | patterns × flag sets × inputs for `java.util.regex` | 1,233 patterns × flag sets, 11,682 inputs, 11 files |

and `driver.clj` (sent to the implementation under test), `runner.clj` (the runner, on the JVM
Arbace), `harvest.clj` (the harvest), and the recorded results in `expected/<part>/<file>.edn`,
one per source file. Expected files total 5.3 MB (forms 1.2 MB, classes 2.5 MB, regex 1.6 MB).

Hand-written forms (`forms/`): `numbers` (overflow and promotion, ratios, BigInt, BigDecimal,
doubles, casts, bit operations, literals, `parse-*`, hashes), `strings` (UTF-16 and non-BMP
characters, `arbace.string`, `Character`, escapes), `printing` (the print functions, every
literal and collection, `*print-length*`, `*print-level*`, `*print-meta*`, `*print-dup*`,
`*print-readably*`, namespaced maps, `print-method`, pprint, cl-format), `format`, `reader`
(every literal, syntax-quote, `#()`, reader conditionals, tagged literals, `*data-readers*`,
`*read-eval*`, `arbace.edn`, reader errors), `hashing`, `sorting`, `inst` (`#inst`, `#uuid`,
`arbace.instant`), `collections`, `seqs` (laziness and chunking made visible by side effects,
transducers), `destructuring_macros`, `polymorphism` (multimethods and hierarchies, protocols,
records, `deftype`, `reify`; no `defclass` or `gen-class`, D6), `types` (types made at run
time: `definterface`, protocols on host types and nil, records as maps, mutable and primitive
fields, `IFn` and collection implementations, `proxy`, `bean`; EVAL-NOTES.md phase 2A),
`state_errors` (dynamic
vars and `binding`, atoms, refs, delays, exceptions and their messages, metadata, namespaces,
keywords and symbols, the regex functions, `eval`).

Class scripts (`classes/`): `PersistentVector` (across the 32 and 1,056 element boundaries,
transients, chunked seqs), `PersistentHashMap` (collisions, nil key, transients),
`PersistentArrayMap` (order, promotion), `PersistentTreeMap` (comparators, `seqFrom`), `Sets`
(`PersistentHashSet`, `PersistentTreeSet`), `PersistentList` (and `Cons`), `Seqs` (`LongRange`,
`Range`, `Repeat`, `Cycle`, `Iterate`, `ArraySeq`, `StringSeq`, chunks, `IteratorSeq`),
`Numbers` and `NumbersCompare` (operator × value tables over longs, ints, doubles with NaN and
-0.0, ratios, BigInt, BigInteger, BigDecimal), `RatioBigInt`, `Names` (`Symbol`, `Keyword`),
`VarNamespace`, `LispReader` (`RT.readString`, `LispReader.read`, `EdnReader`), `Printer`
(`RT.printString`, `RT.print`, `RT.format`), `Murmur3` (and `Util.hasheq`), `UtilRT` (`Util`'s
equality and comparison, `RT`'s collection functions and casts), `Strings` (`StringBuilder`,
`String`, number conversions, `Compiler.munge`/`demunge`), `BigNumbers` (every operation of
`BigInteger` and `BigDecimal` across the algorithms' thresholds, then `BigInt` and `Numbers`
over them; C2G-NOTES.md, phase 2C).

Regex (`regex/`): `basics`, `classes`, `unicode`, `anchors`, `quantifiers`, `groups`,
`lookaround`, `flags`, `split_replace`, `errors` (`PatternSyntaxException` descriptions, indexes
and messages) and `cross` (30 common patterns × 18 inputs × 2 flag sets).

## The runner

    bin/oracle record [-j JOBS] [--timeout S] [SEL...]
    bin/oracle check IMPL [-j JOBS] [--timeout S] [--expected FILE | --write-expected FILE] [SEL...]
    bin/oracle harvest [SUITE]

- `record` regenerates the expected files of the selected parts from the JVM Arbace
  (`bin/arbace -`). The result is byte-identical across runs (checked: two records, same
  files). It fails when a run does not complete or a class script step cannot be resolved.
- `check IMPL` runs every expected file against `IMPL` and compares; `jvm` stands for
  `bin/arbace -`, and `bin/oracle check jvm` passes (20,263 of 20,263 cases). It prints a line per file, the
  first mismatches of each, and writes all of them to `.tmp/oracle/check.txt`; the exit status
  is 0 when every case matches.
- `check IMPL --expected FILE` passes instead when the set of mismatching cases equals the one
  recorded in `FILE`, a reference of the implementation's known mismatches: it prints the new
  mismatches and the newly passing cases, and fails on either (or on a run that did not
  complete). A case is named by its corpus file and its line (forms) or index (classes, regex),
  as the report prints it: `test/oracle/forms/strings.clj:408`. Only the files the check ran
  count, so a selection compares the reference's cases in the selected files.
  `check IMPL --write-expected FILE` (re)writes `FILE` from the run: a recorded case of a file
  the check ran stays, in its group, while it still mismatches; a new one goes into a group
  with the placeholder reason `(new: give the reason)`, to be replaced by hand; cases of files
  not run stay. Rewriting an unchanged reference gives the same file. The format (read with
  `read`):

      ;; comment lines
      {:impl "target/arbace-go/amd64/arbace -"
       :about "what the reference is for"
       :mismatches
       [{:reason "why these cases differ, and where it is documented"
         :cases
         [{:case "test/oracle/regex/basics.clj:45" :form "\\N{LATIN SMALL LETTER E WITH ACUTE}"}
          ...]}
        ...]}

  `:form` is the case's text (shortened), for the reader; only `:case` is compared.
  `test/oracle/known-go-amd64.edn` is the Go build's on amd64 (32 cases in 5 groups,
  2026-10-09), against which `bin/gate --full` checks it (with `--timeout 900`: the reducers
  file takes about 285 s there).
- `SEL` selects parts (`forms`, `classes`, `regex`) or files by a part of their path
  (`forms/numbers`, `harvest`); `-j` the concurrent runs (default 16), `--timeout` the seconds
  allowed to one run (default 300).
- `harvest` regenerates `forms/harvest/` (below).

Times on this machine (64 cores): `record` of everything about 10 s with `-j 16` (6 CPU-minutes), `check jvm`
the same; one file takes 0.6-2 s, mostly the JVM's start.

### The implementation under test

`IMPL` is a shell command (run with `bash -c` in the repository root) that evaluates the forms
on its standard input in order, as `arbace.main -` does (a script read from stdin: `*ns*` is
`user`, the print and reader variables bound so that `set!` works), letting them print to its
standard output. The Go executable plugs in as, say, `bin/oracle check 'target/arbace-go -'`.

Each source file is one run of `IMPL`. Its input is the driver (`test/oracle/driver.clj`, namespace
`oracle.driver`, which ends with `(in-ns 'user)`), then one call per case, then
`(oracle.driver/done N)`:

    (oracle.driver/form 17 "test/oracle/forms/numbers.clj" 42 "(+ Long/MAX_VALUE 1)")
    (oracle.driver/step {:i 3 :op :invoke :class "arbace.lang.PersistentVector" :name "nth" ...})
    (oracle.driver/regex {:i 5 :pattern "a(b)?" :flags [:i] :inputs ["ab"] :replace ["[$1]"]})

The driver prints one line per case, `@@oracle ` and a record, written by its own small printer
(`emit`), in printable ASCII: every other character of a string as `\uXXXX`, so the channel keeps
UTF-16 strings exactly, lone surrogates included, whatever the implementation's stdout encoding.
Other lines on stdout are ignored (counted in the report). A case without a record (the run
died or timed out) is a mismatch (`:missing`). So the implementation needs, to run the driver:
`ns`, `defn`, `let`/`loop`/`cond`/`case`, `try`/`catch`, `binding`, `eval`, `read` from a
`LineNumberingPushbackReader` over a `StringReader`, `pr-str`, `StringWriter`, `StringBuilder`,
and for the scripts `java.lang.reflect` (`Class/forName`, `getMethods`, `getField`,
`Method.invoke`, `Array`), for regex `java.util.regex`.

### Normalization

The runner normalizes every string of a record, the same way when recording and checking,
where the text varies between runs or implementations without meaning: identity hashes
(`#object[C 0x1f2e3d4c` to `0xN`, `C@1f2e3d` to `C@N`), auto-gensyms (`x__123__auto__`),
`G__123`, the `$eval123` and `fn__123`-style counters in generated class and local names
(`runner.clj`, `normalizers`). Nothing else is normalized.

### Helpful NullPointerException messages (V11)

HotSpot describes the null of an implicit `NullPointerException` (JEP 358): `Cannot invoke
"String.length()" because "s" is null`, naming locals and parameters as the bytecode has them.
The Go build's implicit `NullPointerException`s have a null message (accepted deviation V11). By
the user's decision (2026-10-09) such an exception is compared by class only:

- `record` (and `check`, on what the implementation printed) marks every exception chain entry
  (`:ex`, `:print-ex`, `:throws`, `:error`) of class `java.lang.NullPointerException` whose
  message is a helpful one with a third element: `[class message :helpful-npe]`. Helpful means
  the whole message matches HotSpot's grammar (`runner.clj`, `helpful-npe-re`): one of the
  failed actions of `NullPointerExceptions::print_NPE_failed_action` (`Cannot invoke "M"`,
  `Cannot read field "F"`, `Cannot assign field "F"`, `Cannot load from T array`, `Cannot store
  to T array`, `Cannot read the array length`, `Cannot throw exception`, `Cannot enter
  synchronized block`, `Cannot exit synchronized block`), optionally followed by `because ... is
  null`. An explicit `NullPointerException` (a message the code chose, or none) is not marked
  and is compared as everything else.
- `check` compares a marked entry by class only: when the expected chain and the actual one have
  the same length, an actual entry at the position of a marked one, of the same class, counts
  as equal whatever its message. A case that matches only so is counted, per file (`N by V11`)
  and in the summary.

The expected files hold 54 marked entries (52 forms, 2 class script steps). On the Go build
(2026-10-09) the 52 forms all match by the rule; `bin/oracle check jvm` matches all cases with
none of them needing it.

## Formats

All files are read with the Clojure reader (not strict EDN). An expected file is

    ;; Written by bin/oracle record from <source> (doc/go/ORACLE.md); do not edit.
    {:part :forms :source "test/oracle/forms/numbers.clj" :count 822
     :cases
     [{...}
      {...}]}

one case per line, keys in a fixed order; strings are UTF-8 with control characters and lone
surrogates as `\uXXXX`. The cases carry their inputs, so a harness (a Go test, for example) can
use the expected files alone.

### Forms

Source: a `.clj` file; each top-level form is a case, with its text and first line. Forms run in
order in one process per file, in `user`: definitions carry over. The form is read by the
implementation (`read` with `{:read-cond :allow}`, its line set to the source line, `*file*`
the source path, so compiler messages name the source) and evaluated with `eval`, `*out*` and
`*err*` captured. Invalid literals must be inside `read-string`: the runner reads the source
file whole.

Case: `:id` (1, 2, ...), `:line`, `:form` (the text), then the result:
- `:value` the `pr-str` of the value, `:class` its class name (`"nil"` for nil);
- or `:ex`, the exception and its causes, each `[class message]` or `[class message
  printed-ex-data]`, or `[class message :helpful-npe]` (below, "Helpful NullPointerException
  messages");
- or `:print-ex`, when printing the value threw;
- `:out`, `:err` what the form printed, when it printed something.

### Class scripts

Source: a `.clj` file of steps, read as data (not evaluated):

- `(def NAME EXPR)` binds the result; a bare `EXPR` only records it.
- `EXPR`: `Class/FIELD` (static field), `(Class/method args...)` (static method),
  `(.method recv args...)` (instance method, `recv` a bound name; `^Class recv` names the class
  whose public methods are searched, by default the receiver's runtime class),
  `(new Class args...)` or `(Class. args...)`, `(.-field recv)`.
- Short class names resolve in `arbace.lang`, `java.lang`, `java.util`, `java.math`,
  `java.util.regex`; arrays as `T[]`.
- Arguments: `nil`, booleans, longs, doubles (`##NaN`, `##Inf`), strings, characters; a symbol
  (a bound name); `Class/FIELD` (a static field); `(int 3)`, `(long 3)`, `(short 3)`, `(byte 3)`, `(char 65)`, `(float 1.5)`,
  `(double 2)`, `(boolean true)`, boxed primitives of that type; `(biginteger "123")`,
  `(bigdecimal "1.50")`; `(array T x...)`, an array of `T`.
- The overload is chosen from the argument types (applicable, then most specific, then most
  exact; an ambiguity is an error); `^{:sig [int Object]} (...)` gives it explicitly.
- `(each [x [1 2] y [3 4]] STEP...)` repeats the steps for every combination.

Case (a step): `:i`, `:src` (its source text), `:op` (`:new`, `:invoke`, `:static`, `:get`,
`:get-static`), `:class` (where the member is looked up), `:name`, `:target` (the receiver's
binding), `:args` (as data: the literals; `{:ref name}`, `{:static class :name field}`,
`{:char n}` (a surrogate, which has no character literal; results print it so too), `{:box "int" :value 3}`,
`{:biginteger s}`, `{:bigdecimal s}`, `{:array T :items [...]}`), `:sig` (the parameter types,
Java names, resolved at recording), `:bind`, then the result:
- `:ret` the declared return type (or field type) and `:result`: `{:type T}` plus `:value` for
  nil, booleans, numbers of the primitive wrappers, strings, characters (BigInteger and
  BigDecimal as their string), or `:pr` (`RT.printString`) for collections, seqs, keywords,
  symbols and other numbers; lazy seqs (`LazySeq`, `Iterate`, `Cycle`, `Repeat`) are not
  printed, so observing never realizes them; `{:type "void"}`, `{:type "nil"}`;
- or `:throws`, the exception chain the call threw (a bound name then holds nil).

A Go harness runs the steps with the class's translated methods: `:class`, `:name` and `:sig`
identify the method exactly; values flow through the bindings.

### Regex

Source: a `.clj` file of maps: `:pattern` or `:patterns`, `:flags` (one set) or `:flag-sets`,
`:inputs`, optional `:replace` (replacement strings). Flags: `:i` CASE_INSENSITIVE, `:m`
MULTILINE, `:s` DOTALL, `:u` UNICODE_CASE, `:x` COMMENTS, `:d` UNIX_LINES, `:literal`, `:U`
UNICODE_CHARACTER_CLASS, `:canon-eq`. Each pattern × flag set is one case (`:count` counts
these); each input of it a sub-case.

Case: `:i`, `:pattern`, `:flags`, `:inputs`, `:replace`, then `:groups` (group count),
`:named` (name → group), `:results`, one per input: `:matches`, `:match-groups` (each group's
`[start end]` when it matched, -1 for an unmatched group), `:hit-end` (after `matches`),
`:looking-at`, `:find` (every `find` from the start, each with all group spans; at most 1,000),
`:split`, `:split-1` (limit -1), `:replace` (`[replaceAll replaceFirst]` per replacement, or
`[:ex class message]`); or `:error [class description index message]` for a pattern that does
not compile. Positions are in UTF-16 units.

## Adding cases

- Forms: add forms to a file under `test/oracle/forms/` (or a new file), then
  `bin/oracle record forms/<name>`, read the expected file (an unresolved symbol or a typo
  shows up as an `:ex`), and `bin/oracle check jvm forms/<name>`. Commit source and expected
  file together.
- Class scripts and regex: the same, under `classes/` and `regex/`; a class step that cannot be
  resolved is recorded as `:error` and fails `record`.
- The harvest: `bin/clojure-tests` in rename mode (run by `bin/build-arbace --suite` or
  `bin/gate`) leaves the renamed suite in `.tmp/clojure-tests/stage2/suite`; `bin/oracle
  harvest [SUITE]` then rewrites `forms/harvest/` (about 5 minutes), and `bin/oracle record
  harvest` records it. The harvest takes the expressions of `is` (the non-literal arguments of
  `=`, `==`, `not=`; the body of `thrown?`, `thrown-with-msg?`; otherwise the assertion) and of
  `are` (each row substituted), prints them back as source (type hints kept), prepends the
  test file's `arbace.*` aliases and `java.*` imports, and keeps those that run twice with the
  same result without a resolution error (the test file's own definitions are not loaded),
  identity-dependent output or a result over 3,000 characters.
- Text in the sources: write non-ASCII characters as `\uXXXX` escapes (some editors and agent
  tools turn `\uXXXX` in written text into the characters themselves).

## Exclusions

Everything recorded is deterministic; left out:

- Time and the host: the current time, `System` properties and environment, files, the default
  time zone and locale (`(str (java.util.Date. 0))`, `%s` of a Date, `%,d`).
- Randomness: `rand`, `shuffle`, `random-uuid`, `random-sample`.
- Concurrency and timing: futures, agents, `pmap`, threads; refs and atoms only single-threaded.
- Identity: identity hashes and the printing of objects without value semantics (fns, atoms,
  transients, arrays, `PersistentQueue`'s `#object` form, regex patterns' hashes, `deftype`s
  without `hashCode`); what remains in messages and `#object[...]` is normalized (above).
- The class forms (`defclass`), `gen-class` (D6), and loading or compiling files. `proxy` is
  covered (amendment X4, accepted 2026-10-09; C2G-SPEC §16): the Go build has it since the
  user's decision reversing D6 for it (C2G-SPEC §10.4), and `forms/types.clj` checks it (proxies
  of `Object`, of interfaces, of `Writer`, `PushbackReader`, `InputStream` and `OutputStream`,
  `proxy-super`, `update-proxy`, `bean`, pprint's writers). The harvest takes the forms naming
  `proxy` too (since 2026-10-09; before, `harvest.clj`'s `excluded` and `unsafe` left them out,
  from when D6 cut it): the suite's proxy tests are in `java_interop.clj` (its `proxy`
  directory only defines the AOT-compiled classes they use), a namespace left out whole, of
  which the harvest keeps the assertions naming `proxy`, `proxy-super`, `update-proxy`,
  `get-proxy-class`, `construct-proxy`, `init-proxy`, `proxy-mappings` or
  `proxy-call-with-super` (`harvest.clj`, `proxy-forms`): `forms/harvest/java_interop.clj`,
  3 forms after the 7 of its preamble (the others use the test's locals or its `proxy.examples` namespace, and do not stand
  alone). On the Go build 2 of the 3 pass; the third serializes a proxy
  (`java.io.ObjectOutputStream`, cut by D6). `protocols.clj`'s one proxy form needs the test's
  own protocol and is not kept; pprint's proxies are in `pprint/`, which the harvest does not
  read (it takes the top-level test files).
- In the harvest, whole test namespaces: `agents`, `annotations`, `clearing`, `compilation`,
  `errors`, `genclass`, `generators`, the `generated_*` adapters, `java_interop` (but for its
  proxy forms, above), `main`,
  `method_thunks`, `ns_libs`, `parallel`, `param_tags`, `reflect`, `refs`, `repl`, `rt`,
  `run_single_test`, `serialization`, `server`, `streams`, `tap`, `test`, `test_fixtures`
  (`harvest.clj`, `excluded`); and any form mentioning time, randomness, threads, files,
  loading, `gensym`, `promise`, `locking` and the like (`unsafe`).
- In regex: inputs that could exhaust the stack (deep recursion in `java.util.regex` throws
  `StackOverflowError` depending on the stack size); backtracking-prone patterns get inputs of
  14 characters or fewer.

## What the Go side should know

- Some recorded messages are the JVM's own: `ClassCastException` messages name modules and
  loaders (`... in unnamed module of loader 'app'`, `module java.base of loader 'bootstrap'`),
  helpful `NullPointerException` messages name locals and parameters (`because "s" is null`,
  `"<parameter1>"`), and compiler exceptions carry spec's explain data. jrt either reproduces
  them or the mismatches are listed and accepted case by case; the helpful
  `NullPointerException` messages are compared by class only (V11, above).
- Class names recorded for values (`:class`, `:type`) are the JVM's (`java.lang.Long`,
  `arbace.lang.PersistentVector`, `user.R` for records, `user$evalN$fn__N` for fns): `class`
  in the Go build is expected to answer the same names for jrt's and c2g's classes.
- Class scripts record the runtime types of results, among them anonymous classes
  (`arbace.lang.PersistentVector$2`, an iterator); c2g's names for them should match.
- Comparison is of the printed records (but for helpful `NullPointerException` messages,
  above), so `-0.0` differs from `0.0` and `##NaN` matches itself.
- The format cannot express, in class scripts: nested calls as arguments (bind them first),
  literal keywords, symbols or collections as arguments (build them with steps), a literal
  receiver, functions (so `LazySeq` and fn-taking methods are reached only through the forms
  corpus), thread bindings across steps (each step runs on its own).
- The forms of a file depend on each other in order; a case that fails in the Go build can
  make later ones fail too: read the report from the first mismatch of each file.
