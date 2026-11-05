# Clojure's by-design classes, reviewed (2026-11-05)

Brief: the Clojure row of the REAP matrix is read 908 / 2 / 0, eval 21,053 / 81 / 1, AOT 809 / 30 / 0, print 1,031+ / 0 / 0
(`bin/reap`, `test/reap/matrix.edn`; classes in `test/clojure/divergences{,-read,-aot,-print}.edn` over
`test/arbace-go-results.edn`, `test/arbace-go-aot-results.edn`, `test/oracle/known-go-amd64-frozen.edn`,
`test/diffcheck/known.edn`). As `doc/go/GOI-DESIGN-REVIEW.md` (branch `go-design-review`) did for Go, each class is read here
for what it is, whether it is truly by design or reachable by a runtime or c2j change, what that costs (days of one
person's work, rough) and how many cases it unlocks. Reading and analysis only; the probes were run on main's stage 0 and on
the executables already built (`target/arbace-go/amd64/arbace-javac`), nothing was built.

Counting: the suite's cases are tests (a namespace with no tests counts 1); the oracle's are forms. "23 out-for-good" is
therefore 21 tests of the skipped namespaces and tests plus two namespaces with no tests (`genclass.examples`, `repl.deps`).
The same suite case counts at both the eval and the AOT level, so a fix of one is worth two cases there.

## Findings in one table

Ranked by cases per day (a case counted at both levels where both gain). "by design" means the user decided it so and nothing
reachable changes the answer; "reachable" means a named change does.

| # | class | cell | n | verdict | reachable by | cases | days | cases/day |
|---|---|---|---|---|---|---|---|---|
| 1 | locales (root and English only) | eval | 42 | **reachable, a build selection**: the data are generated and committed as a switch | `JRT_LOCALES=common`: `bin/jrt-convert update`, +113 bundles, +8.9 MB (JRT-NOTES.md, "Size") | 42 (+ some of Java's 20/22 `charsets-locales`) | 0.5-1, then a re-seed | 40-80 |
| 2 | `seq-and-transducer` (speed) | eval (AOT passes) | 1 | **not a limit**: 574 s alone (SPEED-NOTES.md) or 1,700 s (the case's note) against the runner's 900 s; AOT runs it in 75 s | a per-test limit, or recorded as an evaluator speed class | 1 | 0.1-0.2 | 5-10 |
| 3 | `java.sql.Connection` (`javadoc-url-test`) | eval, AOT | 1 | **reachable**: the test only needs the interface and its module name | java.sql's interfaces into the shipped world (`jdk/sources.txt`), `Class.getModule` for `java.sql` | 2 | 0.5-1 | 2-4 |
| 4 | `java.awt.Point`, `Color` (`test-dot`, `test-bean`) | eval, AOT | 2 | **reachable**: a slice of java.desktop, no toolkit | `overlay/jdk` variants: `Point`, `Point2D`, `Color` (sRGB only), `Transparency`, a `ColorSpace` stand-in | 4 | 1-1.5 | 3-4 |
| 5 | the evaluator's boxed primitive fns, Clojure-level (`primitive-return-decl`, `incorrect-primitive-type-hint-throws`) | eval 2, AOT 2 | 4 | **reachable, analysis only**: both tests are compile-time checks that need the call's primitive type, not a primitive interface (probe below) | a typed invoke node in `InvokeExpr.parse`, no `invokePrim` | 4 | 1.5-2.5 | 2 |
| 6 | `gen-class` at run time | eval, AOT | 9 | **reachable for most**: gen-interface, proxy and deftype already make classes at run time over `Dyn`; `gen-class` is a no-op | gen-class as a `Dyn` class with delegating methods; 6-7 of 9 tests (see the class) | 12-14 (+ 54 assertions) | 4-6 | 2-3 |
| 7 | `javax.xml.stream` (`invoke-checks-accessibility`) and SAX (`clojure-xml`) | eval, AOT | 2 | **reachable**: the SAX parser and StAX's parts are Java forms in javac's world already and run (probe below) | the xerces closure into the shipped world, `DefaultHandler` among the proxy supers; StAX's factory after | 4 (+ Java's `xml` 22 and 4) | 2-3 | 1.5-2 |
| 8 | Java serialization (`serialization`, `test-proxy-non-serializable`) | eval, AOT | 10 | **reachable, large**: `ObjectOutputStream` and `ObjectInputStream` translated, with their reflection on fields | jrt: `ObjectStreamClass` over jrt's reflection, `serialVersionUID`, `readResolve`/`writeReplace` on Clojure's types | 20 (+ 77 assertions; Java's `serialization` 168 + 117) | 5-8 | 2.5-4 (cross-cell: far more) |
| 9 | `clj-1208` (AOT) | AOT | 1 | **reachable at AOT only**: a deftype compiled by c2j whose static initializer loads its namespace; the evaluator cannot | c2j's deftype class init for a namespace the run has not loaded | 1 | 1-2 | 0.5-1 |
| 10 | primitive interfaces on evaluated fns (native.ae's three, diffcheck's three) | eval 3, AOT 3 | 6 | **reachable, costly**: Q1 (b), an `EvalFn` subclass per primitive interface | `EvalFn` per `IFn$LL` and kin | 6 | 3-5 | 1-2 |
| 11 | `compiled-loader-line` | AOT | 1 | by design as the JVM's `__init`; reachable | the loader sets `Compiler/LINE` per form | 1 | 0.5 | 2, not recommended |
| 12 | `security-manager` | eval | 1 | by design (JEP 486), reachable | a class stub in the world | 1 | 0.2 | not recommended |
| 13 | `one-way-rules` (2), `identity-hash` (2), `shared-symbols`, `interned-at-load` (4), `runtime-source`, `proxy-method-order`, `main-since-v4` (2), `method-invoke-message`, `security-providers` | all | 15 | **truly by design** (see each) | - | 0 | - | - |

Read and print: nothing reachable (read's two are the one-way rules; print has none).

## The AOT level's 30, each named

The cell is `Clojure's suite compiled` (30 not passing, 809 passing); beside it `diffcheck: Clojure's pair` has 10 more known
disagreements (not in the 30, listed after). Where the AOT level differs from the eval level is stated.

**out-for-good (23): XML, gen-class, Java serialization, Swing/AWT, classes from jars** (`doc/go/B1-PLAN.md` D6, the user, 2026-10-10)
1. `clojure.test-clojure.genclass` (8 tests, 54 assertions upstream): gen-class is a no-op, so `ExampleClass` cannot be imported.
2. `clojure.test-clojure.genclass.examples` (0 tests): the fixture of 1.
3. `clojure.test-clojure.serialization` (9 tests, 77 assertions): `ObjectOutputStream`.
4. `clojure.test-clojure.clojure-xml` (1 test): SAX.
5. `clojure.test-clojure.repl.deps` (0 tests): `arbace.repl.deps` (tools.deps' jars).
6. `java-interop/test-bean`: `java.awt.Color`.
7. `java-interop/test-dot`: `java.awt.Point`.
8. `java-interop/test-proxy-non-serializable`: `arbace.inspector` (Swing) and `ObjectOutputStream`.
(Counted: 8 + 9 + 1 + 3 = 21, plus the two empty namespaces = 23.)

**class-files (2)**
9. `compilation/clj-1208`: reachable at AOT only (class 9 above); by design at the eval level.
10. `java-interop/test-proxy-method-order`: reads the proxy's class file with ASM; truly by design.

**q1-evaluated (2)**
11. `compilation/primitive-return-decl` (2 assertions) and 12. `compilation/incorrect-primitive-type-hint-throws` (1): at the
    AOT level too, as they evaluate code at run time (`eval-in-temp-ns`, `load-string`): class 5.

**javadoc-url (1)**
13. `java.javadoc/javadoc-url-test` (1 error: `java.sql.Connection`): class 3.

**xml-stream (1)**
14. `reflect/invoke-checks-accessibility`: `javax.xml.stream.XMLInputFactory`: class 7.

**runtime-source (1)**
15. `not-compiled clojure/bad_def_test.clj`: `CLJ-1184-do-in-non-list-test` spits a source and `compile`s it while it runs; no
    compiled loader can exist for it, so it loads from source and the test passes. By design: not a failure at all.

(Total: 23 + 2 + 2 + 1 + 1 + 1 = 30 cases.)

**Beside the 30: diffcheck's pair (10)**: `interned-at-load` (4, `state_errors.ae:453,454,470,480`: a compiled loader interns the
vars it names when it starts, as the JVM's AOT), `q1-compiled-prims` (3, `native.ae:233,234,237`: class 10),
`compiled-loader-line` (1, `harvest/vars.ae:5`: class 11), `shared-symbols` (1, `state_errors.ae:597`: SA155's shared
symbols), `identity-hash` (1, `strings.ae:408`).

## Class by class

### locales (42 oracle cases, eval)

`test/oracle/forms/text.ae` and neighbours: `Locale/FRANCE`, `GERMANY`, `JAPAN`, `(Locale/of "es" "MX")` through
`NumberFormat`, `DecimalFormatSymbols`, `DateFormatSymbols`, `SimpleDateFormat`, `MessageFormat`, `Currency`. The shipped build
holds java.base's root and English data only (the user, 2026-10-11, for the shipped executables and the freeze;
`JRT_LOCALES` unset = `none`, `bin/jrt-convert` line 484). This is not a design limit: the CLDR bundles are generated
byte-identical to the JDK build's (JRT-NOTES.md, "Text and locales"), `JRT_LOCALES=common` (27 locales and parents, 113 bundles) was
the default until that decision and "makes these pass" (the same note), at 7.7 MB over `none` (63.1 to 70.8 MB, +12%); `all`
is 130 MB. The cost is not code but a decision and a conversion: `bin/jrt-convert update` with the selection (the committed
`jdk/` forms carry the bundles; `jdk/provenance.edn` records `:locales`), the gate's executable and the seed rebuilt (a
re-seed: the seed holds the printed program), then the oracle re-recorded. Unmeasured here: that all 42 pass under `common`
(the 2026-10-11 note says the locale cases do; some may name a locale outside the 27, e.g. `hi-IN` is in `hi`, `fr-BE` is not).
Measure on one build before the decision. Also Java eval's `charsets-locales` (20) and AOT's (22) may shrink.

### `seq-and-transducer` (1, deferred: speed)

Two figures disagree: the case's note says 8.6 ms a trial, 1,700 s; SPEED-NOTES.md (EC7) says the namespace passes in 574 s
alone. Either way the AOT level runs it in 75 s and passes (RPS43). The evaluator is about 23 times slower than the compiled
code on this test (8.6 ms against 0.37 ms), 78 times slower than the JVM. Reachable by (a) a per-namespace limit of 1,800 s in
the runner (the nightly takes one more process of up to 30 minutes, light) or (b) recording it by design at the eval level
"passes compiled" (the form Go's review gave the AOT-passing classes). (c) Speeding the evaluator is open-ended and not priced
here: the ratio to compiled code says where the room is (the evaluator interprets `Expr` trees; a profile of this one test is
the first step, 1 day to look). Recommended: (a) once, to know whether the evaluated run passes at all, then (b) if the
night's cost is not wanted.

### java.sql (1 case twice: `javadoc-url-test`)

`java.sql.Connection` is an interface the test names through `Class/forName` and `Class.getModule`. `jdk/forms/java/sql/` holds
only `Date` and `Timestamp`. java.sql's interfaces (`Connection`, `Statement`, `PreparedStatement`, `ResultSet`, `DatabaseMetaData`,
`SQLException` and the rest, 57 files in jdk26u's `java.sql/share/classes/java/sql`, interface-only but for `DriverManager`'s
service lookups) added to `jdk/sources.txt`, and jrt's `Module` table naming them java.sql. About 40 files; the size is small
(no implementations). Also takes Java's `sql` class (4 interpreted, 1 compiled). 0.5-1 day.

### java.desktop (test-dot, test-bean)

`Point` is 236 lines and `Point2D` 431, pure Java (imports `java.beans.Transient`, `Serial`: cut by a variant). `Color`
(1,225 lines) imports `ColorSpace`, `AffineTransform`, `Rectangle2D`, `ColorModel` and `ConstructorProperties`; `test-bean`
needs only `Color/black` and its getters (`red`, `green`, `blue`, `RGB`, `alpha`, `transparency`, `class`). A variant of `Color`
with the sRGB path, `Transparency` and a `ColorSpace` stand-in whose `getInstance` answers a plain sRGB object is a day of
`overlay/jdk` work; `Point` with `Point2D` half a day. This is a decision to reopen D6 for two classes, not for java.desktop.
`arbace.inspector`'s Swing stays cut (`test-proxy-non-serializable` also needs serialization, class 8).

### the evaluator's boxed primitive fns (q1)

Two different things share the name. Probe, on main's stage 0 (`$ARBACE_HOST_EXE -e`), `*warn-on-reflection*` true:
`(defn primfn (^long []) (^double [a]))`, `(fn [] (loop [k (primfn)] (recur :foo)))` prints no
"recur arg for primitive local: k is not matching primitive"; `(defn rl ^long [] 1) (load-string "(Integer/bitCount ^int (rl))")`
answers 1 where Clojure throws "Cannot coerce long to int, use a cast instead". The cause is in
`arbace/lang/Compiler.ae`: `InvokeExpr.parse` (line 3185 on) rewrites a call of a var with a `^long`/`^double` arglist to
`(.invokePrim ...)` only when `FnMethod/primInterface` (line 3714) answers a class; it answers nil (Q1 (a)), so the call stays a plain
`invoke` and carries no primitive type for `RecurExpr` (the check at line 4584) or `HostExpr`/`retType` (line 5914) to compare.
Both tests are compile-time checks; no primitive interface is needed at run time.

- **Reachable (analysis only, 1.5-2.5 days)**: when `primInterface` would have answered, build the call as a typed invoke
  (an `InvokeExpr` whose `getJavaClass` is the primitive, `canEmitPrimitive` true and `emit`/`eval` doing `invoke` then
  `RT.longCast` or `doubleCast`), not as `.invokePrim`. It is a new node kind: `defnode` in `arbace.c2j.emit`, the generated
  `CompilerNodes.ae` and `CompilerAOTFn.ae` regenerated (`test/c2j/gen_nodes.ae`), the generated-check, and a sample check on
  the recur and the cast. Risk: arithmetic's fast paths (`Numbers/add` of two prims) could now be chosen for a call result that
  the evaluator returns boxed; the cast on return keeps it right. Two tests at each level.
- **Not reachable without Q1 (b) (3-5 days)**: native.ae's three cases and diffcheck's three call `.invokePrim` or test
  `instance? IFn$LL` on a fn made at run time; they need a real interface on the evaluated fn. These stay by design at Q1 (a).

The probe is the evidence, not a build: a small, certain item was not found in this class (the node kind is generated code with
two checks behind it), so nothing was built.

### gen-class (9 cases twice, 54 assertions)

Today `gen-class` is `(defmacro gen-class [& options] nil)` (`arbace/genclass.ae`) with `gen-interface` made at run time
(`Compiler$Image/defineInterface`) and proxy and `reify` and deftypes over `Compiler$Dyn`
(`defineProxyClass`, `setMethod`, `fillProxySlots`). Of genclass's eight tests, three (`interface-array-type-hints`,
`gen-interface-source-file`, `array-descriptors->class`) test `definterface`/`gen-interface`, which exist; they fail to run
only because the namespace requires `genclass.examples`, which imports gen-class's classes (the case is the namespace). A
`gen-class` over `Dyn` for `:name`, `:implements`, `:methods`, `:prefix`, `:state`, `:init`, `:constructors`, `:main`,
`:factory`, `:exposes-methods`, as the delegating methods `proxy` makes (the fns of `prefix-method` vars looked up when called),
unlocks `arg-support` (ExampleClass), `genclass-option-validation` (the private `validate-generate-class-options`, portable from
upstream: 0.2 day on its own), and the three of gen-interface. Remaining at risk: `name-munging` (needs public static fields
`foo_Object_int__var`: `Dyn` has instance slots only), `protected-final-access` (`:extends java.lang.ClassLoader`: not among the
proxy supers, `ClassLoader` is not in the world) and the annotations (commented out upstream). Realistic: 6 of 8 tests, 4-6
days, the fixture compiled at each level. Value beyond the suite: libraries that gen-class a Java-callable class at load time
(not `-main` alone, which `:gen-class` in `ns` already tolerates). A first step of 0.5 day (the validation function, and
`gen-class` defining an `:implements` class) makes `genclass` load, which is most of the namespace.

### javax.xml.stream and SAX (clojure-xml, invoke-checks-accessibility)

Probe on the existing `target/arbace-go/amd64/arbace-javac` (the tool build, whose world holds jdk/javac's 158 xerces files, 42
javax.xml and 26 org.xml.sax): `SAXParserFactory/newInstance`, `newSAXParser`, `getXMLReader`, `setContentHandler` with a `reify`
of `ContentHandler` parse `<a><b x="1"/></a>` and print `:start a 0`, `:start b 1` in 0.2 s. So SAX works in Arbace, in a
different world from the shipped one; `proxy [org.xml.sax.helpers.DefaultHandler]` (what `arbace.xml` uses) fails with
"proxy of org.xml.sax.helpers.DefaultHandler is not in the Go build" (the proxy-supers list, `arbace.j2g.dyn`). Reaching the
shipped build: the closure (jdk/javac's java.xml part is 1.6 MB of forms, a few MB of executable, to be measured) moved from
`overlay/jdk/javac/closure.txt` into the program's `jdk/sources.txt`, `DefaultHandler` among the proxy supers, JAXP's
`ServiceLoader` lookups working in the shipped world, and `arbace.xml`'s disabled SAX path enabled. 1.5-2 days for `clojure-xml`
(its XXE test passes by not resolving the entity: the parser must not read `file:///etc/hostname`), 0.5-1 more for StAX
(`com.sun.xml.internal.stream`: `XMLInputFactoryImpl`, `XMLStreamReaderImpl`, over the same scanners) and
`Paths/get`'s default method. Reopens D6 for XML. Also Java's `xml` class (22 interpreted, 4 compiled).

### Java serialization (10 cases, 77 assertions)

`ObjectOutputStream`'s closure translated (jdk26u `java.io`: `ObjectStreamClass`, `ObjectStreamField`, `ObjectInputStream`,
`SerialCallbackContext`) with jrt's reflection giving fields, `serialVersionUID`, constructors and the `writeReplace` /
`readResolve` / `readObject` hooks, and `Unsafe` or jrt's own field access in place of `ReflectionFactory`. Clojure's
collections are `Serializable` through `SerializedProxy`-like writeReplace (`arbace.lang` Java forms already carry them: the
class files' hooks are source). Several days (EVAL-NOTES.md says "several days"; 5-8 here with the Dyn classes' fields as the
risk: a proxy is not serializable by design upstream too). The decision is cross-cell: Java's `serialization` is the second
largest class (168 interpreted, 117 compiled) and J2G-SPEC §12 cuts it "for good". A spike of 2 days (a round trip of a vector
and a map through `ObjectOutputStream` on the tool build) would settle the size before a decision.

### clj-1208 (AOT only)

The test calls `(.f (clojure.test_clojure.compilation.load_ns.x.))` expecting the deftype's static initializer to load its
namespace. At the AOT level the class is c2j's, translated by j2g; whether c2j emits a loader-calling initializer for a deftype
class whose namespace the run has not loaded is not read here (compiled `deftype`s are classes of the program, `suite_aot_hook`
loads each namespace through its compiled loader and `load-ns` is excluded as a fixture). 1-2 days to find out and fix, one
case, one level. By design at the eval level.

### Truly by design

- `test-proxy-method-order`: reads a class file with ASM; no class file exists, nothing could be handed to ASM that is honest.
- `runtime-source` (15 above): a file made after the program was built cannot have a compiled loader.
- `interned-at-load` (4), `shared-symbols`, `compiled-loader-line`: the JVM's own AOT semantics (decided RPS20 (a), F3.3 (a));
  `compiled-loader-line` could be made to agree in half a day and would make the compiled run differ from the JVM's `__init`
  class, so it is not recommended.
- `identity-hash` (2): a StringBuilder's identity hash differs run to run.
- `main-since-v4` (2): main's two deliberate changes against the frozen v4 jar (the MultiFn names, `.ae` first): the reference
  is the old one; a re-recorded reference would remove it (the jar is the pinned oracle: not proposed).
- `method-invoke-message`: JDK 26's wording is the real one; aligning to the JVM Arbace's would make it worse.
- `security-providers`: ten providers of java.security not in the world; each is a module of its own.
- `one-way-rules` (read, 2): `.cljc` conditionals and `SecurityManager`'s spelling. Both are one-way because the `.ae` has one
  reading; `read --full` already records the conditionals and prints them back. Making print two-way for the spelling needs a
  mark per qualified name for no semantic gain (the same class in Clojure). Keep.

## Proposed

**CDR1. Locales: `JRT_LOCALES=common` in the shipped build, or a build for the oracle.** (a) Flip the default to `common`
(0.5-1 day: `bin/jrt-convert update`, committed bundles, the seed re-made at the next re-seed; +8.9 MB, 63 to 71 MB; the oracle's
42 re-recorded; Java's `charsets-locales` shrinks); (b) keep `none` in the shipped build and run the oracle on a build
with `common` (the test build, `arbace-tests`: 1 day, a second key in the gate, 42 cases pass at no shipped cost, but the oracle
then answers for a build that is not the shipped one); (c) keep as decided 2026-10-11. Recommended: measure first (one
`common` build, the oracle against it, the count that passes), then (a) if the 42 pass: the largest gain per day here, and
the reason for `none` (the freeze) is past. Needs the user: it reverses a decision.

**CDR2. The evaluator's primitive typing (class 5, analysis only).** (a) A typed-invoke node so that the recur mismatch warning
and "Cannot coerce long to int" fire in the evaluator and for the compiled run's `eval` (4 cases, 1.5-2.5 days); (b) also
Q1 (b)'s primitive interfaces on `EvalFn` (6 more cases, 3-5 more days, EVAL-PLAN.md Q1 reopened); (c) keep Q1 (a).
Recommended (a), with `:q1-boxed-prims`'s text narrowed to the three cases that really need an interface. Q1 (a) is the
user's decision of 2026-10-09; (a) here keeps it (no primitive fns at run time).

**CDR3. gen-class at run time (class 6).** (a) first step only: the option validation and `gen-class` defining the `:implements`
class over `Dyn`, so `genclass` loads (0.5-1 day, about 5 of 8 tests); (b) the whole of the listed options, 4-6 days, 6 of 8 tests
and the ecosystem's gen-class users; (c) keep it out (the user, 2026-10-10). Recommended (a) now, (b) after.

**CDR4. The small dependencies of the suite: java.sql's interfaces, `Point` and `Color`, in the shipped world (classes 3, 4).**
(a) java.sql's interface set (0.5-1 day, 2 cases); (b) both with `Point`, `Point2D` and an sRGB `Color` variant (1.5-2.5 days, 6
cases); (c) keep out (D6). Recommended (b): both are tiny against the executable, neither is a desktop port.

**CDR5. XML in the shipped world: SAX and StAX (class 7).** (a) SAX (`clojure-xml`, 1.5-2 days) then StAX (0.5-1), 4 cases and
Java's `xml` class; (b) keep out (D6, the user, 2026-10-10). The executable's growth is the cost to measure first (the
xerces closure compiled; the tool build is 301 MB against the shipped 206 MB, much of it javac). Recommended (a) if the
measured growth is under 10 MB.

**CDR6. Java serialization: a spike (class 8).** (a) A 2-day spike on the tool build's world (round trip of a vector, a map and a
record), then decide; (b) keep out for good (J2G-SPEC §12). The class is the largest cross-cell one still cut by decision
(Clojure's 20 cases, Java's 168 + 117 tests). Recommended (a), after CDR1-CDR4.

**CDR7. `seq-and-transducer`.** (a) A per-namespace limit of 1,800 s once, to settle whether it passes evaluated (0.1-0.2 day,
one process of up to 30 minutes); (b) record it as by design at the eval level ("passes compiled", the case run at the AOT level
since RPS43); (c) keep it deferred. Recommended (a) once; then (b) or the evaluator's profile.

**CDR8. `clj-1208` at the AOT level.** (a) read c2j's output for the deftype class (0.5 day), fix if the initializer is missing
(up to 2); (b) record the class as eval-by-design, AOT-deferred. Recommended (b) now: one case.

**CDR9. The classes' records.** The matrix's counts are cases of different units (tests, namespaces, assertions-free forms);
the `:out-for-good` class's 23 mixes ten reasons (SAX, gen-class, serialization, tools.deps, desktop). Split it into
`:xml`, `:gen-class`, `:serialization`, `:desktop`, `:tools-deps` so each verdict above is one class and one decision, as
Go's review split its classes; and move the `:decided` of each to the review's proposal when decided. A data change in two
`divergences*.edn` files and `test/reap/cells.edn`'s kind maps: 0.3 day, no case moves. Recommended.

## Order, by cases per day

CDR1 (42, 0.5-1), CDR7 (1, 0.2), CDR4 (6, 1.5-2.5), CDR2 (4, 2), CDR3 (12-14, 4-6; its first step 5 cases in 1 day), CDR5 (4, 2.5-3
and Java's 26), CDR6 (20, spike 2). After all of it the Clojure row's by-design cases would be 15 (the list under "Truly by
design") plus `proxy-method-order`, with the eval cell at about 21,13x passing and AOT at 8xx, subject to the unmeasured
points: CDR1's count, CDR3's six, CDR5's size.

## Sources

Read: `test/clojure/divergences*.edn`, `test/reap/matrix.edn`, `bin/reap --entries` (output kept in `.tmp/reap-entries.txt`),
`test/arbace-go-results.edn`, `test/arbace-go-aot-results.edn`, `test/diffcheck/known.edn`, `test/oracle/known-go-amd64-frozen.edn`,
`arbace/genclass.ae`, `arbace/core_proxy.ae`, `arbace/lang/Compiler.ae` (lines 3135, 3185-3205, 3714, 4584, 5914),
`doc/go/EVAL-PLAN.md` (§3, Q1), `doc/go/EVAL-NOTES.md` ("The suite's last failures"), `doc/go/JRT-NOTES.md` ("Text and locales", "Size"),
`doc/go/SPEED-NOTES.md` (EC7), `doc/go/J2G-SPEC.md` (§12, serialization), `doc/VENDOR-NOTES.md` (hand changes 11, 23),
`doc/go/GOI-DESIGN-REVIEW.md` on branch `go-design-review` (the model), upstream's tests at `/root/clojure/test/clojure/test_clojure/`
(`compilation.clj`, `genclass.clj`, `genclass/examples.clj`, `java_interop.clj`, `reflect.clj`, `java/javadoc.clj`, `clojure_xml.clj`),
jdk26u's `java.desktop` and `java.sql` sources for sizes. Probes: stage 0 `-e` (primitive typing, above) and
`target/arbace-go/amd64/arbace-javac -e` (SAX parse, above); no build was started.
