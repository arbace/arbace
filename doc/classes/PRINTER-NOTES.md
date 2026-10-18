# repr/java's printer: Java forms → Java

The second verb of repr/java, Java's bridge (doc/NAMING-SURVEY.md, R1 and R5, decided
2026-10-17): `read` turns Java into the Java forms of [SPEC.md](SPEC.md) (the converter,
[CONVERTER-NOTES.md](CONVERTER-NOTES.md)), `print` turns Java forms back into Java source that
javac compiles and that `read` converts to the same forms. It runs on the tools' host (stage 0,
like repr/go's printer, [../go/PRINTER-NOTES.md](../go/PRINTER-NOTES.md)): it needs no javac and no
JVM. The reading side stays on the frozen JVM, since javac is its front end. SPEC §14 states
the printer's contract; these notes say how it is built, what the round trip checks, and what
it found.

```sh
bin/repr-java print [--index DIR]... [--layout full|lines] [--anew] [--quiet] FORMS... OUT
bin/repr-java roundtrip [--corpus baseline,samples,jdk] [--no-classes] [--text | --text-only] [--record]
```

`FORMS` are converted files or directories of them (`read`'s output). Every file holding Java
forms (`defclass`, `defpackage`, `defmodule`) becomes `OUT/<package path>/<Name>.java`, the name
taken from the file's header (`;; Converted from .../Name.java`); a package's namespace file
(`ns` and `load`s) has no Java. `--index DIR` adds converted files whose declarations the
printer consults without printing them (which classes are generic, the methods' signatures);
the round trip passes `jdk/forms`.

## Layout

| file | namespace | role |
|---|---|---|
| `arbace/repr/java/print.ae` | `arbace.repr.java.print` | the printer: reading converted files, names and imports, the declarations index and the expressions' types, types, annotations, code, declarations, files, the CLI |
| `arbace/repr/java/roundtrip.ae` | `arbace.repr.java.roundtrip` | the round trip's comparator: forms, text and class files, results, references, known differences with their reasons; the text level (`text`) |
| `arbace/repr/java/full.ae` | `arbace.repr.java.full` | mode `:full`'s record: spans, the canonical text and hash of a form, `record-hashes` (the reader's), `strip` (below, "Mode :full as built") |
| `arbace/repr/java/layout.ae` | `arbace.repr.java.layout` | the converter's writer of `.ae` text (until 2026-10-17 `arbace.repr.java.print`, renamed by R1) |
| `bin/repr-java` | | the verbs `print` (on the tools' host, `bin/lib/tools.bash`'s `arbace_tool`) and `roundtrip` |
| `bin/lib/repr-java/roundtrip` | | the round trip's driver (its header) |
| `test/repr/java/roundtrip/` | | per corpus: `CORPUS.edn`, the reference (results without reasons), and `CORPUS-known.edn`, the differences with their reasons; the text level's `CORPUS-text.edn`, `CORPUS-text-known.edn` |
| `test/repr/java/full/` | | the samples' edited forms of mode `:full` as the full layout prints them |

`arbace/repr/java/` is not embedded in the executable (`arbace.j2g.embed`'s exclusions: repr/java
needs javac), so the host loads the printer from the checkout's sources, evaluated; no re-seed is
involved.

## How it works

The forms are read with the core's reader (metadata included). The printer inverts what the
converter makes explicit and restructures (SPEC §7), so that `read` gives the same forms back.
Where the forms leave a choice open (Java's `?:` or an `if` returning, a statement or expression
`switch`, `var` or a declared type), the printer picks the reading that converts back to the
same forms, and among those the one the baseline's style suggests.

**Names.** A file's `import` gives simple and member-class names (`Map$Entry`); classes of the
package (by the corpus), Clojure's default imports (`RT/DEFAULT_IMPORTS`, imported in Java where
not `java.lang`) and member classes in scope resolve as SPEC §5.2 says. Java imports are written
for the classes the text names by simple name; a member class is written through its outer
class (`Map.Entry`) when Java can name that, else by its canonical name. A member class that a
class in scope inherits under the same name from a supertype is qualified (Java would find the
inherited one first; the forms see declared members only).

**The declarations and types.** The printer indexes the classes of the corpus and of `--index`
(type parameters, supertypes, fields and methods with erased types, whether a method's
parameters or result use type variables), and falls back on the host's own classes
(`arbace.classes.env/info`: reflection over the classes of the executable) for the JDK classes
neither holds. From these it types expressions as Arbace types them (erased, SPEC §5.5): an
untagged local is its initializer's type (§5.3), written as that class, as `long` for an
integer literal, as the functional interface of a lambda, or `var` where Java needs javac's type
arguments (a generic local whose methods get lambdas) or the printer cannot tell.

**Declarations.** `defclass` and its kinds, record components, enum constants (with bodies),
annotation types with `default`, sealed types, member, local (`letclass`) and anonymous classes
(`anon`, `:outer`, `^:diamond`), modules and package annotations. Annotations print in the order
they were written (the reader merges outer metadata after inner); a type annotation on a
declared type's base class is written on its qualified name (`java.lang.@A String`,
`Outer<T>.@A Inner`), so that it does not read as a declaration annotation; a receiver's
annotations give Java's receiver parameter. `^:deprecated` is a `/** @deprecated */`.

**Code.** Statements print in contexts: a statement, the method's or lambda's value (`return`),
a switch expression arm's value (`yield`), and the tail of a `loop`/`recur` body (where `recur`
falls to the update and a path without one leaves the loop). What the converter restructured
comes back as Java:

- an `if` whose branches return is an `if`/`else`; a statement ending in an explicit jump that
  more statements follow is put in a block, which the converter does not fold into an `if` over
  the rest (`(do (when c (return x)) ...)` stays as written);
- `loop`/`recur` is a `for` (all declarators of one type; the updates ordered so that Java's
  sequential update gives recur's parallel values), `(loop [] ... (when c (recur)))` a
  `do`-`while` (`(break :do-body)` its `continue`), a pattern condition a `while`; a `while`
  whose last statements hold increments the converter keeps unhoisted is a `for` with them as
  its update, and one whose condition's temporaries were made after its body's a `for` too
  (javac's order, which the temporaries' numbers record);
- `(break)` inside a Java `switch` leaves its loop by a label; a labeled switch's
  `(break :switch v)` is `yield`;
- the converter's temporaries become Java's operators again: postfix increments
  (`(let [old-N g] ...)`), compound assignments on places with impure parts, boolean `&` and
  `|`, increments hoisted before a `return`; an increment the converter moved after a statement
  on a local returns into it (`a[i++] = x`); the rebinding of captured locals disappears;
- conversions written out are Java's casts, those the converter made of implicit ones go
  (the `float` promotion of comparisons; `(long 1)` of an `int` `?:` in a compound assignment;
  the boxing of a `?:` branch); integer literals are written as longs where Java needs them (an
  operand of a `long` operator, a reference target, a parameter only a long overload takes);
- a local declared without initializer comes back so when its tag shows it (a `char`,
  `boolean`, `long`, `float` or `double` with Java's default value would have none) or when it
  is assigned before every read (definite assignment over the forms), unless the converter
  would then fold the next `if` into its initializer; a conditional initializer whose branches
  have other types than the local, or a let that would otherwise be merged with the next, is
  Java's `T x; if (c) x = a; else x = b;`;
- Java's casts that the forms erase are written back where javac needs them: to a type variable
  (`(T) x`, `throw (T) t`), to a generic type of the same erasure (`(Comparator<T>) ...`,
  `(T[]) ...`), to a collection type for a typed `for` over a raw one (`(Collection<T>) xs`), to
  a non-generic functional interface for a lambda passed to overloaded methods; the converter's
  hints on conditional and `clone()` arguments are left to it; an erasure cast of a generic
  value passed to a generic method is left to javac;
- `switch` arms are Java's arrows, `MatchException`'s default (javac's) is left out where javac
  adds it; patterns, `when` guards, record patterns, `case null, default`;
- `with-resources` is `try` with resources, joined with a `try` around it.

A form Java cannot write in an expression position falls back to a switch expression of one
arm, which compiles and converts back to another form (none in the corpora).

**Literals.** The reader keeps no radix (`0x7f` reads as 127). The printer scans each converted
file for integer tokens written in another radix and uses a value's radix when the file writes
that value so everywhere; otherwise decimal (the text level's known differences).

## The round trip

`bin/repr-java roundtrip` converts the corpus's Java into forms F1 (`read`, on the frozen JVM),
prints them (`print`, on the tools' host), reads the printed Java into F2 and judges every
converted file at three levels (`arbace.repr.java.roundtrip`):

| level | compares | |
|---|---|---|
| `forms` | F1's and F2's files as data, metadata included | the printer's requirement: the two forms equal, no normalizations |
| `ae` | the same files as text, byte for byte (after the first line, which names the Java file read) | the forms and the spellings the converter keeps (radixes) |
| `classes` | javac's class files of the original Java and of the printed Java, `-g:none` (no `LineNumberTable`, `LocalVariableTable`, `SourceFile`), byte for byte, per Java file (its classes and their nested ones) | the same javac output |

The javac of the class level is the frozen JVM's (jdk26u's, 26.0.2.1), with the options the
corpus is built with (the JDK's java.base: `--patch-module`, `-XDstringConcat=inline`). Printed
files javac rejects are replaced by their originals and compiled again, so that the other files'
classes are compared; the rejected ones fail the class level (their forms are then missing too).

Corpora: `baseline`, Clojure's Java (the frozen `clojure/`, 183 Java files, 189 converted files
with the packages' namespace files), as `bin/repr-java check` converts it; `samples`,
`test/repr/java/java` and `module` (14 Java files, 17 converted files); `jdk`, `jdk/forms`
(java.base's closure as `bin/jrt-convert` converts it, 838 Java files, 911 converted files), F1
being the committed forms and the originals made by `bin/jrt-convert`'s `sources` and `generate`
steps (into `.tmp/repr-java-roundtrip/jdk-originals`, once).

Each corpus's results are compared with its reference, `test/repr/java/roundtrip/CORPUS.edn`
(an entry and level passing there and not now fails the run); `--record` writes the reference
and the differences with their reasons, `CORPUS-known.edn`.

### Results (2026-10-17)

| corpus | files | forms | ae | classes (Java files) | time |
|---|---:|---:|---:|---:|---:|
| baseline | 189 | 189 | 182 | 143 of 183 | 61 s |
| samples | 17 | 17 | 17 | 12 of 14 (11 before JP3) | 42 s |
| jdk | 911 | 874 | 816 | 473 of 838 | 162 s |

Times on 64 cores, the three runs one after the other; the JDK's originals are made once more
(about a minute) when `.tmp` lacks them. Printing alone takes 18 s for the baseline and 44 s for
the JDK (the declarations of `jdk/forms` are indexed first, about 4 s); reading the printed Java
14 s and 31 s. `bin/gate --full` runs the three corpora (about 5 minutes, on the JVM's chain).

### The differences and their reasons

Every difference is listed with its reason in `CORPUS-known.edn`. By kind:

- **javac rejects the printed Java** (jdk: 39 Java files; 30 forms files missing, 6 package
  namespace files that load them differ): Java's type arguments in code are not in the forms
  (SPEC §1, principle 4), and where the original's typing needed them (inference of generic
  methods from wildcard-typed arguments, `(Map.Entry<K,V>)` casts of captured types, explicit
  type arguments `Gatherer.<A, R>defaultFinisher()`, a lambda's parameter types through several
  generic calls) the printer's casts do not recover them. The baseline and the samples compile
  whole.
- **The overload of a literal argument** (jdk: `java/util/Random.ae`): `fields.get("seed", -1L)`
  on `ObjectInputStream.GetField`, a class neither the corpus, `jdk/forms` nor the host declares,
  so the printer cannot tell that the literal is a long.
- **An integer's radix** (baseline 7, jdk 57 at the `ae` level; the forms are equal): the file
  writes the value in two radixes (`0x01` and `1`).
- **The class files** (baseline 40, samples 2, jdk 326 Java files): the forms do not record the
  source's choice of
  - a value's control flow (323 of them): `return c ? a : b` or `return switch (...)` in the
    original where the printer writes an `if` returning or a statement `switch`, or the other
    way round; the conditional's own conversions keep the printer to `if` where they would
    change the forms (an experiment printing `?:` where the forms allowed it: jdk 469 classes,
    the baseline 115, against 473 and 143),
  - a local's initializer (13): Java's default value written or left out,
  - a `switch` with fall-through (8): the converter repeats the following arms' code (SPEC §12,
    question 9), which the printer writes as separate arms,
  - an increment's place (4): `i++` inside an expression or as its own statement,
  - the order of code or of the constant pool (13). (Until JP3, below, also a type annotation on
    an array creation's dimension the converter did not keep, `new @A String @B [3]`, samples'
    `TypeAnns`.)

### The text level

repr/go's `:full` mode prints the original text back byte for byte because its forms record the
text: comments, spellings, layout. Java forms record none of these: the converter drops comments
and layout, normalizes spellings (`5L` is `5`, text blocks are strings, parentheses go), and
restructures the code (loops into `loop`/`recur`, folded `if`s, repeated fall-through,
temporaries). A Java text level would take a `:full` mode of `read` that records, per
declaration and statement, javac's source positions with the comments and tokens between them,
and a printer that lays a form out from its record when the form is the one recorded (and
prints it anew otherwise). Since the converter's restructurings are not one-to-one with the
text, the record would in effect be the original text per member: byte-identical, but by
copying, not by printing. Not built (JP2): the open end of repr/java, a `:full` mode of `read`
later. (Built on 2026-10-18 as that: "Mode :full as built", below.)

## Mode :full as built (2026-10-18)

At the user's decision (JP2 and JP4 (b), "a `:full` mode of `repr/java read` later", then
2026-10-17 "both next"): `bin/repr-java read --full` records what the Java forms lose, and
`print` gives every Java file back byte for byte (the full layout, `--layout full`, the default
for such forms); javac then accepts every printed file and compiles it to the original's class
files.

```sh
bin/repr-java read --full OUT ROOT [PATH...]          # also read-jdk --full
bin/repr-java print [--layout full|lines] [--anew] FORMS... OUT
bin/repr-java roundtrip [--corpus ...] --text | --text-only [--record]
```

**What the reader records** (`arbace.repr.java.full`; SPEC §14, 6). The forms are mode `:lines`'
with two metadata keys more, written on lines of their own before the form:

- `:java/at [start end hash]` on every declaration of javac's tree that a form stands for whole:
  each top-level class, module and package declaration, and each member of a top-level or member
  class (field, method, constructor, initializer, member class; an enum's constants as one, their
  `constants` form). `start` and `end` are javac's positions (`TreeInfo.getStartPos`, `getEndPos`
  with the unit's `EndPosTable`, which javac keeps since repr/java passes it a diagnostic
  listener): UTF-16 offsets into the file's text, from the first modifier or annotation to the
  closing brace or semicolon. `hash` is the form's: of a member the whole form, of a class its
  skeleton (the class with its positioned members as their starts), a 32-bit `String.hashCode`
  of a canonical text of the forms as the core's reader reads them (metadata included, but the
  `:java/` keys; maps and sets sorted), the same on the frozen JVM and on the tools' host. The
  reader writes each file with the forms numbered, reads it back and writes the hashes in.
- `:java/file {:text t :at [start ...]}` on the file's first form: the Java file's text (as javac
  read it: `getCharContent`) and the starts of its top-level forms in their order.

So the record holds the source's comments, layout, spellings (`5L`, `0x7f`, text blocks,
parentheses, Unicode escapes), the type arguments it wrote or left to javac's inference, and the
choices the converter restructures away (`?:` or `if`, statement or expression `switch`,
fall-through, `var`, initializers, `i++`'s place, the loops): as the text of the declarations
they are in. Positions below a member (statements, expressions) are not recorded (JF2).

**What the printer does** (`java-text-full`, `class-text-full`). A file whose top-level forms are
the ones its record names (their starts, in order) is its text, with each positioned form in its
span: a form that is the one recorded (its hash the recorded one) is the text of its span, a
class through its own text (between its members' spans) with each member in turn; a form that
is not is printed anew by the lines layout's code, as the text at its span (its lines indented
as the line it starts on), its own members that are the ones recorded again their text.
Overlapping spans (Java's `int a, b;`, two fields over one text) are one group, printed from the
record only when all of it is. A member printed anew leaves its doc comment to the record (no
`/** @deprecated */` of its own), and an enum's constants their `;`; a class it names by simple
name that the file does not import is imported after the file's `package` line, and a simple
name the file imports as another class is written qualified. A file whose top-level forms are
not the recorded ones is printed anew, its members from their record where they are the ones
recorded, with the file's imports. `--anew` prints every member anew within the record's text (a
check of printing anew in the full layout, below). Printing the JDK's 838 files takes 29 s in the
full layout (the index of `jdk/forms` included), 55 s with `--anew`.

**The text level** (`bin/repr-java roundtrip --text`, `--text-only`; the round trip's driver
`text_level`, `arbace.repr.java.roundtrip text`): each corpus's Java read with `--full` (the
jdk corpus from its originals, `.tmp/repr-java-roundtrip/jdk-originals`, as `jdk/forms` were read
from them), printed in the full layout and judged per converted file at four levels:

| level | compares |
|---|---|
| `record` | the forms of mode `:full` without their record (`full/strip`) and the forms of mode `:lines` (`jdk/forms` for the JDK), as data, metadata included |
| `text` | the printed Java file and the original, byte for byte |
| `classes` | javac's class files of the original and of the printed Java, `-g:none`, byte for byte |
| `anew` | javac accepts the Java of `print --anew` (every member printed anew within the record's text) |

against `test/repr/java/roundtrip/CORPUS-text.edn` (the reference) and `CORPUS-text-known.edn`
(the differences with their reasons). And the samples' Java with two edits made in its forms of
mode `:full` (a string literal of `Square`'s compact constructor changed, `Shapes.area` renamed)
prints as `test/repr/java/full/Square-edited.java` and `Shapes-edited.java`, which javac
accepts: the edited member printed anew, the rest of the file the original's.

### Results (2026-10-18)

| corpus | converted files | record | text (Java files) | classes | anew (javac accepts) |
|---|---:|---:|---:|---:|---:|
| baseline | 189 | 189 | 183 of 183 | 183 of 183 | 183 of 183 |
| samples | 17 | 17 | 14 of 14 | 14 of 14 | 14 of 14 |
| jdk | 911 | 911 | 838 of 838 | 838 of 838 | 799 of 838 |

Every Java file of the three corpora is printed back byte for byte (1,035 files; the JDK's 18.4 MB
of its originals' tree hold 838 of them), and javac compiles each to the original's class
files. No known difference at the levels `record`, `text`, `classes`. The 39 JDK files the
`--anew` print does not compile are the 39 the lines layout's (above, "javac rejects the printed
Java"): Java's type arguments in code, which the record holds as text but the printer of the
lines layout does not use (JF3). javac's first errors, now that every pass's log is kept for the
reasons: 36 of typing (27 incompatible types, the rest inference of type arguments and of
generic methods' applicability, and an ambiguous `makeRef`), one `cannot find symbol` and two of
definite assignment (`Socket`'s local `result`, another file's `clazz`); `jdk-known.edn` gives
these files the general reason, javac's first pass's log not naming them. The baseline's `Compiler.java` and the JDK's
`FileURLConnection.java` were rejected at first: an import the printer added had gone after the
file's last `import` line, inside a comment of commented-out imports, and a simple name the
member printed anew used (`URLConnection`, `java.net`'s) was imported by the file as another
class (`sun.net.www.URLConnection`); fixed as above.

The forms of mode `:full` of the JDK are 30.6 MB against `jdk/forms`' 10.4 MB (2.9 times), with
28,150 positioned forms; reading them takes 32 s (the hashes included). `:lines` is unchanged:
its forms are mode `:full`'s without the record (the `record` level), `bin/jrt-convert verify`
gives `jdk/forms` byte for byte, `bin/repr-java check` passes, and the round trip's levels
`forms`, `ae`, `classes` match their references. Times on 64 cores, shared: the text level
64 s for the baseline, 56 s for the samples, 216 s for the JDK (its read 31 s, the two prints 29
s and 55 s, javac of the printed and of the `--anew` trees the rest); `bin/gate --full`'s step
(`roundtrip --corpus baseline,samples,jdk --text`) 790 s, about 13 minutes, the text levels 336 s
of it (the three corpora's other levels took 454 s in that run, 265 s on 2026-10-17's quieter
host).

### Choices for the user (JF1-JF5; built as recommended)

- **JF1 Printing from the record's text.** (a) A declaration that is the one recorded prints as
  its span of the recorded text, one that is not is printed anew (as built): byte for byte by
  construction, and an edit prints the edited declaration anew in the printer's style; (b)
  token-level printing as repr/go's and repr/haskell's `:full`: every token printed from the
  forms and laid out at recorded positions. (b) needs the printer to reproduce every source choice
  the converter restructures (loops as `loop`/`recur`, repeated fall-through, the temporaries,
  folded `if`s, `?:`, switch expressions, `var`, parameterized local types, type arguments,
  casts, parentheses, spellings, imports), each recorded per construct, where the forms of the
  JDK give javac's classes back today for 473 of 838 files: a project of its own. *Recommended:
  (a)*, the notes' expectation of 2026-10-17 ("byte-identical, but by copying").
- **JF2 The unit of printing anew.** (a) Declarations: top-level forms, and the members of
  top-level and member classes (a change anywhere in a method prints the method anew); (b) also
  statements and expressions, whose forms the converter restructures (a `let` holds the rest of
  its block, a `loop` its update after its body), each needing its own rule of span. *Recommended:
  (a)*; (b) when a tool edits code inside methods and wants the rest kept.
- **JF3 The source's choices as data** (JP2 (b), JP4 (b): `?:`, a switch expression, `var`,
  `:java-type` on casts, explicit type arguments of calls) for the printer of the lines layout
  and the members printed anew: (a) not built, the record holding them as text (as built; the 39
  JDK files stay rejected when printed anew); (b) recorded as keys and used by the printer.
  *Recommended: (a)* until forms edited by tools must print as the source would.
- **JF4 The text level in the gate.** (a) `bin/gate --full`'s `repr-java-roundtrip` step runs
  `--text` (about 13 minutes on a shared host, the text levels about 6 of them); (b) outside the gate (`make
  repr-java-text`). *Recommended: (a)*, as repr/go's and repr/haskell's text levels.
- **JF5 Keys and the hash.** (a) `:java/at [start end hash]`, `:java/file {:text :at}`, the
  hash a 32-bit `String.hashCode` of the canonical text; (b) a SHA-256 (no practical collision,
  slower in the host's evaluated code). *Recommended: (a)*: the hash only tells an edited form
  from the recorded one.

## Proposals

- **JP1** `jdk/provenance.edn`'s converter hash covers `arbace/repr/java/` whole, the printer and
  the comparator included, so a change to them makes `bin/jrt-convert verify` (in
  `bin/gate --full`) fail until `bin/jrt-convert update`, though the forms do not change. (On
  main it is already stale: `6a2483...` recorded, `9c8304...` the sources at `82345a08`.)
  *Recommendation:* hash only the reader's sources (`java.ae`, `convert`, `jtypes`, `forms`,
  `names`, `layout`, `javac`, `resolve`) and `bin/repr-java`, then `bin/jrt-convert update` once;
  not done here.
- **JP2** The source's choices the forms do not record (`?:` or `if`, a switch expression,
  `var`, an initializer of Java's default value, a literal's spelling, parentheses, comments):
  (a) leave them out, the printer choosing one style (as built); (b) a `:full` mode of `read`
  recording them as metadata, for the class level to match and a text level to exist, as
  repr/go's. *Recommendation:* (a) now; (b) when Java's text matters (it is source of truth only
  for the JDK's own files).
- **JP3** The converter loses the type annotation of an array creation's dimension
  (`new @A String @B [3]` is `(new (array ^{A true} String) 3)`). *Recommendation:* fix it in
  `read` (forms and `test/repr/java/expected` change), separately.
- **JP4** Java's type arguments in code (casts to parameterized types, explicit type arguments
  of calls), which javac needs for the 39 rejected JDK files: (a) known differences (as built);
  (b) the converter records them as metadata (`^{:java-type (Comparator T)}` on a `cast` form, as
  `:type-args` already does for annotated ones). *Recommendation:* (a), with (b) under JP2's (b).

### Decided (2026-10-17, the user)

All as recommended:

- **JP1** the provenance files' converter hash (`jdk/provenance.edn`, and
  `test/java-fixtures/provenance.edn`, computed alike) covers only the reader's sources, what
  converts Java into forms: `bin/lib/frozen.bash`'s `repr_java_reader_hash` (`arbace/repr/java.ae`
  and `convert`, `forms`, `javac`, `jtypes`, `layout`, `names`, `resolve`, and `bin/repr-java`);
  `bin/jrt-convert update` and `bin/clojure-tests --java-fixtures update` run once (the forms
  unchanged, the provenance rewritten), their `verify` passing.
- **JP2** and **JP4** one printer style now; the 39 JDK files javac rejects and the source's
  choices the forms do not record stay known differences (`CORPUS-known.edn`). The open end: a
  `:full` mode of `repr/java read` recording comments, spellings, layout and those choices (and
  Java's type arguments in code), for a Java text level and exact class files, later.
- **JP3** fixed in `read` (a commit of its own): `new String @B [3]` is
  `(new ^{B true} (array String) 3)`, the dimensions' annotations from javac's tree
  (`dimAnnotations`); `test/repr/java/expected` and the samples' references updated (`TypeAnns`'
  classes now javac's: samples 12 of 14).


**Decided (2026-10-18, the user): JF1-JF5** as built: unedited declarations printed as their
recorded text, edited ones anew (JF1); the declaration the unit printed anew (JF2); source choices
held as text only (JF3); the text level in `bin/gate --full` (JF4); a 32-bit `String.hashCode` of
a canonical text for edits (JF5).
