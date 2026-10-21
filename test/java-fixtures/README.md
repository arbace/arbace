# test/java-fixtures/

The Java forms of Clojure's suite's Java fixtures, the input the Go build's test build adds to
the program (doc/go/EVAL-NOTES.md, "The suite's Java fixtures"; amendment FX1): `bin/arbace-go
--build-tests` translates them with the program into `target/arbace-go/<arch>/arbace-tests`, the
executable Clojure's suite runs on (`CLOJURE_TESTS_GO`). Committed as the JDK's forms are in
`jdk/` (standalone stage S1, [doc/STANDALONE-SURVEY.md](../../doc/STANDALONE-SURVEY.md) §4.2, D2),
so that the test build needs neither javac nor repr/java. They are generated, not edited:
`bin/clojure-tests --java-fixtures update` writes them, `bin/clojure-tests --java-fixtures
verify` (run by `bin/gate --full`, before the test build) converts afresh and compares, byte for
byte.

| path | what | from |
|---|---|---|
| `forms/` | one Java forms file (`.ae`) per Java source, at its package's path, and one namespace file per package (`arbace/test.ae`, `compilation.ae`, `reflector.ae`), as repr/java writes them (`bin/repr-java read`, javac's class path the frozen JVM Arbace's jar since S4, `bin/lib/frozen.bash`, until then Arbace's `target/stage2`, for `arbace.lang.IFn` and the Java API; repr/java itself on the frozen jar since S4; with `REPR_JAVA_HOST=arbace`, repr/java on javac's tool executable and javac's class path the checkout's runtime declarations, `bin/repr-java declarations`, the same forms: doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN4"; so by default since JN5, and only so since JN6: no JVM); the first line names the source (`;; Converted from clojure/clojure test/java/...`) | `test/java` of Clojure's repository (12 files: `compilation.TestDispatch`, `JDK8InterfaceMethods`, `ClassWithFailingStaticInitialiser`, `reflector.IBar`, and `clojure.test`'s `AdapterExerciser`, `ConcreteClass`, `FIConstructor`, `FIStatic`, `FunctionalTester`, `GenericInterface`, `ReflectorTryCatchFixture`, `SwissArmy`), renamed by repr/clojure (`clojure.*` to `arbace.*`) as `bin/clojure-tests` renames the suite |
| `provenance.edn` | Clojure's repository and commit; `:jdk`, what javac attributes the sources against (`java -version`'s JDK until JN6, since the platform of javac on Arbace by the SHA-256 of `jdk/javac/platform.sha256`); the converters' versions (SHA-256 over repr/java's reader and `bin/repr-java`, as `jdk/provenance.edn`; over `arbace/repr/clojure{.ae,/}`) and repr/java's host (the frozen jar's name and SHA-256 from S4, javac's tool executable by the key of its sources since JN5); the counts | `bin/clojure-tests --java-fixtures` |

Origin: https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a`, the
revision `bin/clojure-tests` fetches for the suite (the upstream source of the frozen tree
`clojure/` on `arbace-for-java-26`, tag `arbace-for-java-26-v1`, which kept no `test/`), path
`test/java`; converted on 2026-10-12 by repr/java, on the JDK 26.0.2.1 built from jdk26u, as of
Arbace's main at `b0f5730` (the commit that first added this tree records the rest).

When to regenerate (`bin/clojure-tests --java-fixtures update`, then commit this tree with the
change): after a change to what the conversion reads: repr/java (`arbace/repr/java{.ae,/}`, `bin/repr-java`), repr/clojure's
renaming (`arbace/repr/clojure{.ae,/}`), the suite's revision (`rev` in `bin/clojure-tests`), the classes of
Arbace or the JDK the fixtures name (javac attributes against them). `verify` fails until then.
`bin/arbace-go --build-tests DIR` builds the test executable from other forms, for instance a
fresh conversion (`bin/clojure-tests --java-fixtures DIR`).

Licence: Clojure's test files, and the forms converted from them, under the Eclipse Public
License 1.0 (Copyright (c) Rich Hickey, as Clojure's readme.txt states); `provenance.edn` and this file,
Arbace's own: [LICENSE.md](../../LICENSE.md).
