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
bin/repr-java print [--index DIR]... [--quiet] FORMS... OUT
bin/repr-java roundtrip [--corpus baseline,samples,jdk] [--no-classes] [--record]
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
| `arbace/repr/java/roundtrip.ae` | `arbace.repr.java.roundtrip` | the round trip's comparator: forms, text and class files, results, references, known differences with their reasons |
| `arbace/repr/java/layout.ae` | `arbace.repr.java.layout` | the converter's writer of `.ae` text (until 2026-10-17 `arbace.repr.java.print`, renamed by R1) |
| `bin/repr-java` | | the verbs `print` (on the tools' host, `bin/lib/tools.bash`'s `arbace_tool`) and `roundtrip` |
| `bin/lib/repr-java/roundtrip` | | the round trip's driver (its header) |
| `test/repr/java/roundtrip/` | | per corpus: `CORPUS.edn`, the reference (results without reasons), and `CORPUS-known.edn`, the differences with their reasons |

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
| samples | 17 | 17 | 17 | 11 of 14 | 42 s |
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
- **The class files** (baseline 40, samples 3, jdk 326 Java files): the forms do not record the
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
  - the order of code or of the constant pool (13), a type annotation on an array creation's
    dimension the converter does not keep (`new @A String @B [3]`, samples' `TypeAnns`).

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
copying, not by printing. Not built (proposal JP2).

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
