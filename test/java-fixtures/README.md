# test/java-fixtures/

The class forms of Clojure's suite's Java fixtures, the input the Go build's test build adds to
the program (doc/go/EVAL-NOTES.md, "The suite's Java fixtures"; amendment FX1): `bin/arbace-go
--build-tests` translates them with the program into `target/arbace-go/<arch>/arbace-tests`, the
executable Clojure's suite runs on (`CLOJURE_TESTS_GO`). Committed as the JDK's forms are in
`jdk/` (standalone stage S1, [doc/STANDALONE-SURVEY.md](../../doc/STANDALONE-SURVEY.md) §4.2, D2),
so that the test build needs neither javac nor j2a. They are generated, not edited:
`bin/clojure-tests --java-fixtures update` writes them, `bin/clojure-tests --java-fixtures
verify` (run by `bin/gate --full`, before the test build) converts afresh and compares, byte for
byte.

| path | what | from |
|---|---|---|
| `forms/` | one class forms file (`.ae`) per Java source, at its package's path, and one namespace file per package (`arbace/test.ae`, `compilation.ae`, `reflector.ae`), as j2a writes them (`bin/j2a -m arbace.j2a.main convert`, javac's class path Arbace's `target/stage2`, for `arbace.lang.IFn` and the Java API); the first line names the source (`;; Converted from clojure/clojure test/java/...`) | `test/java` of Clojure's repository (12 files: `compilation.TestDispatch`, `JDK8InterfaceMethods`, `ClassWithFailingStaticInitialiser`, `reflector.IBar`, and `clojure.test`'s `AdapterExerciser`, `ConcreteClass`, `FIConstructor`, `FIStatic`, `FunctionalTester`, `GenericInterface`, `ReflectorTryCatchFixture`, `SwissArmy`), renamed by c2a (`clojure.*` to `arbace.*`) as `bin/clojure-tests` renames the suite |
| `provenance.edn` | Clojure's repository and commit; the JDK whose javac attributes the sources; the converters' versions (SHA-256 over `arbace/j2a` and `bin/j2a`, as `jdk/provenance.edn`; over `arbace/c2a`); the counts | `bin/clojure-tests --java-fixtures` |

Origin: https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a`, the
revision `bin/clojure-tests` fetches for the suite (the upstream source of the frozen tree
`clojure/` on `arbace-for-java-26`, tag `arbace-for-java-26-v1`, which kept no `test/`), path
`test/java`; converted on 2026-10-12 by j2a, on the JDK 26.0.2.1 built from jdk26u, as of
Arbace's main at `b0f5730` (the commit that first added this tree records the rest).

When to regenerate (`bin/clojure-tests --java-fixtures update`, then commit this tree with the
change): after a change to what the conversion reads: j2a (`arbace/j2a`, `bin/j2a`), c2a's
renaming (`arbace/c2a`), the suite's revision (`rev` in `bin/clojure-tests`), the classes of
Arbace or the JDK the fixtures name (javac attributes against them). `verify` fails until then.
`bin/arbace-go --build-tests DIR` builds the test executable from other forms, for instance a
fresh conversion (`bin/clojure-tests --java-fixtures DIR`).

Licence: Clojure's test files, and the forms converted from them, under the Eclipse Public
License 1.0 (Copyright (c) Rich Hickey, as Clojure's readme.txt states); `provenance.edn` and this file,
Arbace's own: [LICENSE.md](../../LICENSE.md).
