# The translators' names — survey

Status: survey, 2026-10-16, at the user's request ("Survey first, decide later"), from the
user's two notes of that day (doc/JOURNAL.md, "a note on the translators' names" and "the naming
note refined"; doc/AGENDA.md, Later). Nothing is renamed here. The decisions (§6, N1-N13) are
the user's. Counts were taken on main at `a8431399` (`git grep`, case-insensitive, a name not
preceded or followed by a letter or digit, so `a2g_classes.go` and `A2G_JOBS` count), and on
the seed of that commit (`seed/arbace-seed-go.tar.xz`, `b566d53a...`, unpacked into `.tmp/`).

The user's notes:

> "g2a" name is unfortunate and misleading, maybe "g2g" would be better, or how to name these
> converters/compilers?

> translation is always between forms, so j2a or j2c might be better jaf read / jaf print
> instead. the earlier renaming stemmed from the confusion around the letter 'c' that starts
> "clojure" and also "classes"

The principle drawn from them: **one bridge per language** between its source text and its
forms (one tool, two verbs: read and print); **translators run between forms dialects only**;
**no bare `c`**.

## Executive summary

- **The names say less than the tools do.** Of the four `x2a`/`a2x` names, one is right
  (c2a: Clojure into Arbace), one is half right (j2a: Java into *class* forms, read only), one
  is wrong (g2a: Go to Go forms *and back*, plus the build, while the name reads "Go into
  Arbace"), and one is ambiguous (a2g: "Arbace to Go", while its input and its output are both
  Arbace forms, class forms and Go forms). `a` names three things (Arbace's core forms, class
  forms, Go forms); `c` named two (Clojure, classes) until 2026-10-11 and still names Clojure
  (§1, §2).
- **Three schemes compared** (§3): (A) the user's principle with short names: bridges `jaf`
  and `gaf` (verbs `read`, `print`, and for Go `build`), translators named by the pair of
  dialects they join, from unambiguous codes (`cf2gf` for a2g, `clj2ae` for c2a); (B) plain
  long names (`java-forms`, `go-forms`, `classes-to-go`, `clojure-to-arbace`); (C) the x2y
  scheme kept, with unambiguous codes everywhere (`j2cf`, `g2gf`/`gf2g`, `cf2gf`, `clj2ae`),
  or minimally (only g2a renamed, the user's `g2g`).
- **The rename is about 7,000 occurrences in 1,300 files** (§4): 6,959 occurrences of the four
  names, 225 paths named after them; without the generated `jdk/forms` headers (923), the
  journal and the historical survey, 5,974 in 386 files, of which 3,138 in 43 documents.
  a2g alone is 3,977 (57%). The precedent (2026-10-11, §5) changed 198 paths and 308 files
  (3,352 lines) in two commits on one agent's branch.
- **What is new since the precedent is the seed.** On 2026-10-11 the seed was a jar; today it is
  Arbace's printed Go program, which embeds the tools' sources (33 files under
  `res/arbace/{a2g,g2a,c2a}`), compiles them (33 `aot_arbace_{a2g,g2a,c2a}_*` packages), and
  carries a2g's prefix in its generated names (`a2g_*.go` 303 files, `A2g_*`, `a2g_header`:
  951 of the seed's 1,692 files). `RT.toolSource` names the tools' directories, so the rename
  changes the runtime: the first build after it runs the renamed tools evaluated on today's
  stage 0, then the fixpoint and a re-seed follow (§4.3).
- **Recommendation** (§6): scheme A. `jaf` (Java ⇄ class forms), `gaf` (Go ⇄ Go forms, with
  `build`), `cf2gf` (class forms → Go forms, today's a2g), `clj2ae` (Clojure → Arbace, today's
  c2a); `arbace.aot` kept as the compiler's back end (`ae → cf`); the printed program's names
  moved to a prefix tied to no tool, once; `G2A_GOROOT` becomes `ARBACE_GOROOT`; no shims,
  as in the precedent; one branch, after `g2a-full` is merged, ending with a re-seed.

## 1. The tools and what each does

### 1.1 The representations

| name | what it is | where | spec |
|---|---|---|---|
| Java (text) | Java source | jdk26u, `overlay/jdk/java.base`, `test/java-fixtures`' sources | — |
| Go (text) | Go source | GOROOT, TamaGo, go-whim, the printed program | — |
| Clojure (text) | Clojure source, `.clj`/`.cljc` | Clojure's suite and libraries | — |
| **Arbace forms** (core) | Clojure plus Arbace's forms, `.ae` | `arbace/` | Clojure, doc/VENDOR-NOTES.md |
| **class forms** | "Java's class files written in Clojure": classes, fields, methods, typed code | `arbace/lang`, `jdk/forms`, `overlay/jdk`, the back end's output | doc/classes/SPEC.md |
| **Go forms** | "Go programs written in Clojure": Go's syntax tree with what go/types infers | `go/arbace/jrt`, `tamago/`, `box/`, `x509roots/`, a2g's output | doc/go/SPEC.md |
| godump's dump | the typed tree of a Go package as EDN, from `go/parser` and `go/types` | `.tmp/godump/out` | doc/go/HELPER-NOTES.md |

The three forms dialects are all read by Arbace's reader and written as `.ae`. Class forms and
Go forms are both *Arbace's* forms, which is why a letter `a` cannot tell them apart.

Later dialects, by CLAUDE.md's direction: **Go as a language projected into Arbace**, "as Java
was" (an idiomatic representation of Go in the Arbace language, as class forms are for Java,
beside or above today's syntax-level Go forms), and, at the ISA horizon, the forms of Arbace's
own code generator (doc/STANDALONE-SURVEY.md §7). A scheme should leave room for both.

### 1.2 The tools

| tool | from → to | direction | kind | host | code |
|---|---|---|---|---|---|
| **j2a** | Java text → (javac's attributed trees) → class forms → `.ae` text | one way; no class forms → Java | bridge, read half | the frozen JVM (javac is its front end) | `arbace/j2a/` (9 ns), `bin/j2a`, `bin/j2a-check`, `bin/jrt-convert` |
| **g2a** | Go text → godump's dump → Go forms → `.ae` text (`convert`); Go forms → Go text (`print`); Go forms → executable (`build`: print, then `go build`) | both ways, plus the build | bridge, both halves, and a build driver | the tools' Go host | `arbace/g2a/` (11 ns), `tools/godump`, `tools/gocmp`, `bin/g2a` (verbs `build`, `dump`, `read`, `corpus`, `convert`, `print`, `roundtrip`, `test`), `bin/g2a-*` (6), `bin/lib/g2a-host.bash` |
| **a2g** | class forms (the runtime, `jdk/forms`, the overlays, the back end's output) → Go forms, a whole program: reachability, analysis, translation, the resources | one way | translator between forms dialects; a whole-program compiler | the tools' Go host (its edge recorded on the frozen JVM) | `arbace/a2g/` (19 ns), `bin/a2g`, `bin/a2g-check`, `bin/a2g-edge`, `bin/a2g-evalproof` |
| **c2a** | Clojure text → Arbace text, by a rule table, text-level (layout and comments kept) | one way | translator between dialects (of one reader) | the tools' Go host | `arbace/c2a/` (4 ns), `bin/c2a`, `bin/c2a-check` |
| **arbace.aot** | Arbace namespaces (the prepare run's analyzed units, `Compiler`'s `Expr` trees) → class forms | one way | the back end of Arbace's compiler; no command of its own | inside the build (`ARBACE_AOT=1`) | `arbace/aot/` (3 ns); packages `aot_*` in the program |

Two observations follow:

- **g2a and a2g overlap in letters, not in work.** g2a's `print` is "Go forms → Go", which in
  the x2y scheme reads `a2g`, the name of another tool, which turns class forms into Go forms
  (and has g2a print them). The whole path to an executable is
  `arbace.aot` (ae → cf) → a2g (cf → gf) → g2a print (gf → Go) → Go's toolchain.
- **j2a's `print` is not a Java printer.** `arbace.j2a.print` writes class forms as `.ae` text,
  as `arbace.g2a.layout` writes Go forms. In the user's sense, `jaf print` (class forms → Java)
  does not exist yet; a2g's edge, the oracle and javac do not need it. It would close a round
  trip for j2a as g2a's closes for Go (Java → forms → Java, compiled by javac and compared).

### 1.3 Where the letters came from

The first names, on `arbace-for-java-26` and `arbace-for-golang`: j2c (Java → class forms,
2026-10-06, `arbace.j2c`), g2c (Go → Clojure forms, 2026-10-07), c2g (class forms → Go): `c` meant Clojure in g2c and classes in c2g, j2c sitting between. The 2026-10-11 rename
(§5) put Arbace's `a` in every name, and c2a, the Clojure translator, took `c` for Clojure.

## 2. What a name must cover

Every scheme below names, for each tool:

- the tool and its verbs (`gaf print`...);
- the namespaces (`arbace.g2a.*`, 41 namespaces in four directories) and the test namespaces
  (`g2a.*`, `a2g.*` in `test/`);
- the scripts: `bin/j2a`, `bin/j2a-check`; `bin/g2a`, `bin/g2a-build-tests`, `-check`,
  `-convert-tests`, `-print`, `-print-tests`, `bin/lib/g2a-host.bash`; `bin/a2g`, `-check`,
  `-edge`, `-evalproof`; `bin/c2a`, `-check` (15 files);
- the environment variables (34 distinct names): `J2A_JVM_OPTS`, `J2A_PATCH_ALL`,
  `J2A_THREADS`, `J2A_TEST_JOBS`, `J2A_CP`, `J2A_DEBUG`; `G2A_GOROOT` (79 occurrences, the
  toolchain of every build), `G2A_HOST`, `G2A_GO_EXE`, `G2A_HOST_ID`, `G2A_GOARCH`, `G2A_GOOS`,
  `G2A_GOCMP_FLAGS`, `G2A_DUMPS`, `G2A_OP`, `G2A_SEED`, `G2A_LEVELS`, `G2A_JOBS`,
  `G2A_JVM_OPTS`; `A2G_GO_EXE`, `A2G_JOBS`, `A2G_HOST`, `A2G_OUT`, `A2G_ARCH`, `A2G_SHOW`,
  `A2G_ANALYSIS_TIMES`, `A2G_JVM_OPTS`, `A2G_EDGE_WORK`, `A2G_EDGE_TRACE_ARGS`, `A2G_PROF`,
  `A2G_FIXTURES`, `A2G_CLASS_TIMES`, `A2G_TRACE`, `A2G_JDK_REACH`, `A2G_EMBED_ONLY`; c2a none;
- the printed program's names, which are a2g's: the shared files of every package (`a2g_classes.go`,
  `a2g_strings.go`, `a2g_up.go`, `a2g_init.go`, `a2g_support.go`, `a2g_cut.go`, `a2g_dyn.go`,
  `a2g_frames.go`, `a2g_lambdas.go`, `a2g_cf.go`, `a2g_fromfn.go`, `reflect_tables_a2g.go`),
  the support names jrt's hand-written Go forms define and the program calls (`A2g_Truth`,
  `A2g_RefP`, `A2g_ObjectClone`... 18 names), the object header field `a2g_header`, the
  overlays' directives (`a2g/variant` 90, `a2g/cut` 57, `a2g/add` 45, `a2g/nth`, `a2g/out`)
  and metadata (`:a2g/line`, `:a2g/nth`); the compiled namespaces' packages
  (`aot_arbace_a2g_reach`, ...: a namespace's name, so they follow the namespaces);
- the documents named after a tool: `doc/go/A2G-SPEC.md`, `doc/go/A2G-NOTES.md`, `doc/C2A.md`,
  `doc/G2C-SURVEY.md` (a record), and those about one: `doc/classes/CONVERTER-NOTES.md` (j2a),
  `doc/go/CONVERTER-NOTES.md`, `PRINTER-NOTES.md`, `HELPER-NOTES.md`, `ROUNDTRIP.md`,
  `BUILD.md` (g2a);
- the Makefile's targets: `g2a-roundtrip`, and the `##` lines of `jdk-verify` (j2a) and
  `edge-verify` (a2g);
- the generated headers: each of `jdk/forms`' 911 files begins "... by arbace.j2a. Regenerate
  rather than edit." (`arbace/j2a/names.ae`), also `test/j2a/expected` and
  `test/java-fixtures`; `jdk/edge.edn`'s first lines name a2g; `jdk/provenance.edn` hashes
  `arbace/j2a` and `bin/j2a`;
- the caches named after a tool: `.tmp/a2g-host`, `.tmp/g2a-print-cache` (gitignored; renamed
  or left to expire).

## 3. Naming schemes

### 3.1 Dialect codes

The schemes share one table of codes, chosen so that no letter stands for two things:

| code | representation | why |
|---|---|---|
| `java`, `j` | Java text | — |
| `go`, `g` | Go text | — |
| `clj` | Clojure text | the file extension; no bare `c` |
| `ae` | Arbace forms (core) | the file extension, already Arbace's mark |
| `cf` | class forms | the spec's own name |
| `gf` | Go forms | the spec's own name |

A later idiomatic Go dialect would take its own code (for instance `gl`, "Go as language", or
whatever its spec calls it), and translators `gf2gl`, `gl2gf` would follow without a new
scheme. Arbace's own code generator would be `cf2<isa>` or `ae2<isa>`.

### 3.2 Scheme A: bridges and translators (the user's principle)

A **bridge** is one tool per foreign language, with verbs; a **translator** runs between two
forms dialects and is named by them.

| today | A | verbs | namespaces | scripts | env | docs |
|---|---|---|---|---|---|---|
| j2a | **`jaf`** (Java as forms) | `read` (today's `convert`), `check`, `coverage`; `print` later | `arbace.jaf.*` | `bin/jaf`, `bin/jaf-check` | `JAF_*` | `doc/classes/CONVERTER-NOTES.md` kept (or `JAF-NOTES.md`) |
| g2a | **`gaf`** (Go as forms) | `read` (today's `dump` + `convert`), `print`, `build`, `roundtrip`, `corpus`, `test`; today's `read` (a dump into data) becomes `read --dump` | `arbace.gaf.*` | `bin/gaf`, `bin/gaf-check`, `-build-tests`, `-read-tests`, `-print-tests`, `bin/lib/gaf-host.bash` (`bin/g2a-print` folded into `bin/gaf print`) | `GAF_*`; `G2A_GOROOT` → `ARBACE_GOROOT` | doc/go/'s CONVERTER (→ READER), PRINTER, HELPER, ROUNDTRIP, BUILD notes: names unchanged |
| a2g | **`cf2gf`** | — | `arbace.cf2gf.*` | `bin/cf2gf`, `-check`, `-edge`, `-evalproof` | `CF2GF_*` (`CF2GF_JOBS`) | `doc/go/CF2GF-SPEC.md`, `CF2GF-NOTES.md` |
| c2a | **`clj2ae`** | — | `arbace.clj2ae.*` | `bin/clj2ae`, `-check` | — | `doc/CLJ2AE.md` |
| arbace.aot | kept (`ae → cf` in the docs) | — | `arbace.aot.*` | — | `ARBACE_AOT` | doc/STANDALONE-NOTES.md, S6 |

Makefile: `g2a-roundtrip` → `gaf-roundtrip`; the `##` lines follow.

Trade-offs:
- + Each name says what the tool is. Bridges and translators are told apart by their shape
  (three letters with verbs, `x2y` without). The round trip reads as itself: `gaf read`, then
  `gaf print`. A future `jaf print` and a later Go dialect fit without renaming anything.
- + `gaf build` keeps the build where its users look for it today (`bin/g2a build`); the
  alternative, a separate driver (`bin/gobuild`, `arbace.gobuild`), is cleaner in principle
  (the build is print plus Go's toolchain, not a bridge) but splits the box's and jrt's callers.
- − Two shapes of name instead of one. `jaf` is also the JavaBeans Activation Framework's
  acronym (`javax.activation`), a minor clash outside Arbace; `gaf` has no common meaning.
- − `cf2gf` is five characters in prose that says "a2g" about 2,150 times; the docs grow a
  little. `clj2ae` likewise (c2a is the smallest of the four).
- Variant A': translators keep their present short names where they are not wrong (a2g stays:
  "a" is read as "class forms, Arbace's", "g" as "Go forms"). Cheaper (a2g is 57% of the
  rename), but it keeps the overlap of §1.2 (`gaf print` and `a2g` both "to Go") and the
  ambiguous `a`.

### 3.3 Scheme B: plain long names

| today | B | namespaces | env |
|---|---|---|---|
| j2a | `java-forms` (`read`, `print`) | `arbace.java-forms.*` | `JAVA_FORMS_*` |
| g2a | `go-forms` (`read`, `print`, `build`) | `arbace.go-forms.*` | `GO_FORMS_*` |
| a2g | `classes-to-go` (or `go-backend`) | `arbace.classes-to-go.*` | `CLASSES_TO_GO_*` |
| c2a | `clojure-to-arbace` | `arbace.clojure-to-arbace.*` | — |
| arbace.aot | kept | | |

Trade-offs:
- + Nothing to learn; reads well in the README and for newcomers (the Makefile's audience).
- − Long in commands, prose and generated names: the compiled packages become
  `aot_arbace_classes_to_go_reach`, the env vars `CLASSES_TO_GO_JOBS`. The docs say these
  names thousands of times.
- − `arbace.classes-to-go` sits beside `arbace.classes.*` (the class forms' front end) and its
  `arbace.classes.go`: a reader takes one for part of the other. `go-backend` avoids that but
  says less (the back end is aot plus a2g plus the printer plus Go's toolchain).
- − It drops the house style every record and frozen branch uses; mixing short and long names
  across the history makes the journal harder to follow.

### 3.4 Scheme C: x2y kept, unambiguous codes

C-full: every tool is `x2y` over §3.1's codes.

| today | C-full | C-min |
|---|---|---|
| j2a | `j2cf` | j2a |
| g2a | `g2gf` (and its print direction `gf2g`: two names for one tool, or one name with a `print` verb, against the scheme) | `g2g` (the user's first idea) or `g2f` |
| a2g | `cf2gf` | a2g |
| c2a | `clj2ae` | c2a |
| arbace.aot | `ae2cf` (or kept) | kept |

Trade-offs:
- + C-full is uniform and as precise as A's translators; it is A with the bridges written as
  their read direction.
- − A bridge has two directions; x2y names one. g2a would need two names or a lie. This is the
  defect the user's note points at.
- − C-min changes the least (g2a's 1,191 occurrences and 128 paths) but `g2g` reads as Go to Go,
  an identity: true of the round trip, not of either half; and it leaves `a` meaning three
  things and `c` meaning Clojure.

### 3.5 Side by side

| | j2a | g2a | a2g | c2a | aot | shapes | paths renamed | occurrences changed |
|---|---|---|---|---|---|---|---|---|
| today | j2a | g2a | a2g | c2a | aot | 1 | — | — |
| A | jaf | gaf | cf2gf | clj2ae | aot | 2 | 225 | ~7,000 |
| A' | jaf | gaf | a2g | clj2ae | aot | 2 | 184 | ~3,000 |
| B | java-forms | go-forms | classes-to-go | clojure-to-arbace | aot | 1 | 225 | ~7,000 |
| C-full | j2cf | g2gf/gf2g | cf2gf | clj2ae | ae2cf | 1 | 228+ | ~7,100 |
| C-min | j2a | g2g | a2g | c2a | aot | 1 | 128 | ~1,200 |

## 4. The rename's impact

### 4.1 Occurrences on main (`a8431399`)

Occurrences / files, per area:

| name | arbace/ | bin/ + Makefile | test/ | go/ + overlay/ | jdk/ | tools, box, tamago, x509roots | docs (doc/, *.md) | total | paths named |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| j2a | 52 / 16 | 118 / 9 | 87 / 38 | 1 / 1 | 923 / 913 | — | 303 / 29 | 1,484 / 1,006 | 45 |
| g2a | 123 / 18 | 386 / 28 | 79 / 31 | 5 / 3 | — | 25 / 13 | 573 / 27 | 1,191 / 120 | 128 |
| a2g | 641 / 41 | 310 / 18 | 345 / 29 | 491 / 132 | 18 / 2 | 18 / 4 | 2,154 / 34 | 3,977 / 260 | 41 |
| c2a | 36 / 6 | 103 / 8 | 40 / 17 | — | — | — | 128 / 13 | 307 / 44 | 11 |
| all | | | | | | | | **6,959 / 1,299** | **225** |

Without what is generated or kept (the `jdk/forms` headers, doc/JOURNAL.md, doc/G2C-SURVEY.md):
5,974 occurrences in 386 files; of them 3,138 in 43 documents and 2,836 in 343 files of code,
scripts, tests and data. By kind: namespace references `arbace.j2a` 1,015 (mostly the jdk
headers), `arbace.a2g` 214, `arbace.g2a` 137, `arbace.c2a` 38; script references `bin/g2a*` 355,
`bin/a2g*` 250, `bin/j2a*` 85, `bin/c2a*` 38; environment variables `G2A_*` 166, `A2G_*` 86,
`J2A_*` 28; overlay directives `a2g/*` 194; printed file names `a2g_*` 127 and support names
`A2g_*` 107 in the sources. The documents with most: STANDALONE-NOTES (546 lines), JRT-NOTES and
A2G-SPEC (338 each), A2G-NOTES (176), BOX-NOTES (142), EVAL-NOTES (141); CLAUDE.md 34 lines,
README 15, LICENSE.md 24, the Makefile 9. `arbace.aot` (53) and `aot_` (55) are untouched by
every scheme but C-full.

### 4.2 In the seed (`b566d53a...`, 1,692 files, 64.5 MB unpacked)

- the tools' embedded sources: 33 files, `mod/cmd/arbace/res/arbace/{a2g,g2a,c2a}/*.ae.z`
  (j2a is not in the program: it runs on the frozen JVM);
- the tools' compiled packages: 33 of the 100 `aot_*` packages (`aot_arbace_a2g_*` 18,
  `aot_arbace_g2a_*` 11, `aot_arbace_c2a_*` 4), their identifiers carrying the namespace
  (`a2g_reach_A_const_N`...: tens of thousands of occurrences);
- a2g's own names in every translated package: 303 `a2g_*.go` files, and `A2g_*` or
  `a2g_header` in 951 files (`A2g_Truth` alone 10,821 times);
- `SEED.edn`'s `:printed-by` names a2g and g2a.

Every scheme changes the first two (the namespaces are in the program); only a rename of the
printed names (N9 (a) or (b)) changes the third, which is most of the seed's files. The seed is
regenerated either way; the cost of the third is in the sources that write or call those names
(a2g's emitter, `go/arbace/jrt` 143 occurrences, the overlays 348), not in the seed.

### 4.3 What a rename forces

1. **The runtime changes.** `RT.toolSource` (`arbace/lang/RT.ae`) names the tools' directories
   (`arbace/a2g/`, `g2a/`, `c2a/`, `j2a/`, `aot/`), so the runtime key
   (`bin/lib/version.bash`'s `arbace_runtime_key`) changes, and so does it with `jdk/` (the
   headers) and `go/` (`A2g_*`, if renamed). The build takes the two-run path (about 5.5
   minutes, not 2.5 to 3).
2. **The first build runs the renamed tools evaluated.** Today's stage 0 embeds and compiles
   `arbace.a2g.*`; it has no `arbace.cf2gf.*`, so it loads them from the checkout and evaluates
   them, while the class forms' front end (`arbace.classes.*`, not renamed) stays compiled. Not
   measured; before S6, with every tool evaluated, a2g's load and reach took 106 s and 55 s and
   an evaluated build 5:40 (doc/STANDALONE-NOTES.md, "The build's time"), so expect minutes
   more than a usual build. Once, until the next seed.
3. **The fixpoint, then a re-seed.** G1 by stage 0, G2 by G1, G3 by G2, G2 = G3; the seed packed
   from G3 (`bin/seed --make`) and printed again from its commit (`bin/seed --verify`: stage 0 =
   G1 = G2 = G3, about 6 minutes). Until the re-seed every fresh checkout pays step 2.
4. **The JVM's records regenerated.** `bin/jrt-convert update` (the 911 headers name
   `arbace.<java bridge>`; `jdk/provenance.edn`'s converter hash), `bin/clojure-tests
   --java-fixtures update`, `bin/a2g-edge update` (its file's header), on the frozen jar. The
   jar itself needs nothing: it carries `arbace.j2c` and has loaded main's `arbace.j2a` from the
   checkout since 2026-10-11 (`bin/lib/frozen.bash` links the tools' directories, a list there
   that the rename edits).
5. **Checks.** The essential gate (about 8 minutes cold), and the `--full` parts the rename
   touches, run alone: `bin/j2a-check --suite`, `bin/jrt-convert verify`, `bin/a2g-edge
   verify`, the fixtures' verify, g2a's print, convert and build tests, `bin/g2a roundtrip
   amd64`, `bin/tamago-vendor verify` and `bin/toolchain-vendor verify` (they call `bin/g2a
   convert`), the box's build and smoke test.
6. **Caches.** The gate's cache key (it hashes `arbace/`, `go/`, `overlay/`, `jdk/`, the scripts)
   misses once; `.tmp/a2g-host`, `.tmp/g2a-print-cache` are left to expire or renamed.

### 4.4 What keeps the old names

- The frozen branches and every tag: `arbace-for-java-26` (j2c), `arbace-for-golang` (j2c,
  g2c, c2g), the milestones' tags (j2a, g2a, a2g, c2a). They take fixes only.
- doc/JOURNAL.md's dated entries (append-only) and the journals at the tags; commit messages.
- CLAUDE.md's convention line ("The translators name Arbace (`a`) ... (until 2026-10-11 j2c,
  g2c, c2g ...)"): extended, not replaced, with the new names and "until 2026-10-NN j2a, g2a,
  a2g, c2a". CLAUDE.md is the user's to approve.
- doc/G2C-SURVEY.md (its name and text, a record), this survey, and the provenance lines that
  say "derived by arbace.j2c" (the vendored files' records, kept in 2026-10-11 too).
- Living documents use the new names throughout, with a "Names:" note where they record earlier
  work, as doc/go/SPEC.md and A2G-SPEC.md do for 2026-10-11.

### 4.5 Compatibility shims

- (a) None, as on 2026-10-11: one change, every caller updated, the old command fails with
  "No such file". Users of the repo are the user and the agents, both following main.
- (b) Old scripts as one-line aliases (`exec bin/gaf "$@"`) for a while, and old variables
  read as fallbacks. Kinder to habits and to old notes, but every alias is a script the Makefile
  check, the docs and the gate must then know about, and they never get removed on schedule.
- (c) None, but a guard for the one variable likely set in a shell or a profile: scripts stop
  with a message when `G2A_GOROOT` is set and `ARBACE_GOROOT` is not (one line in
  `bin/lib/tools.bash`), so a stale environment fails loudly rather than building with another Go.

### 4.6 The work

The precedent (§5): one agent's branch, within a day, alongside c2a: 198 paths moved, 308 files
and 3,352 lines changed, the checks of §4.3.5 (then: the gate 7m04s). Today, for scheme A:
225 paths (and 3 documents), about 6,000 hand-relevant occurrences in about 390 files, most of
them mechanical (a scripted `git mv` and an ordered list of `sed` rewrites, longest names
first: `A2G_` before `a2g`, `arbace.a2g` before `a2g`), then the prose read over (verbs such as
"g2a's converter" become "gaf's reader"; `bin/g2a convert` becomes `bin/gaf read`, which is a
change of interface, not of spelling). Plus §4.3's compute: the evaluated first build, the
gate, the fixpoint, the re-seed and its verify, the JVM's regenerations and the targeted
checks: about an hour of machine time. In all about one agent-day, as in 2026-10-11, with a
re-seed more. Scheme B costs the same; A' about half; C-min a fifth.

## 5. The precedent: the 2026-10-11 renaming

- **The decision** (2026-10-10, on main before the second freeze; doc/JOURNAL.md on
  `arbace-for-golang`, "what comes after the freeze"; commit
  `42ef0e75`): with c2a, the Clojure → Arbace translator, "the tools are renamed to name Arbace
  as their target: j2c → j2a, g2c → g2a, and c2g → a2g if it is still needed then". The cause
  (the user, 2026-10-16): `c` began both "clojure" and "classes".
- **The order** (the journal at `arbace-inside-itself-v1`, 2026-10-11, "after the freeze" and
  "c2a; the tools renamed j2a, g2a, a2g"): the `.ae` rename first, alone (branch `ae`: `8913648`
  the `git mv` of 688 files, `c76b468` the callers, `d36a19d`); then, in parallel on the renamed
  tree, c2a with the tool renames (branch `c2a`) and Arbace's version (branch `version`).
- **The commits** (branch `c2a`, 7 commits, fast-forwarded into main):
  - `043300bb` "The tool renames: j2c -> j2a, g2c -> g2a, c2g -> a2g (git mv only)": the
    directories `arbace/{j2c,g2c,c2g}`, `test/{j2c,g2c,c2g}`, the commands `bin/{j2c,g2c,c2g}*`,
    `doc/go/C2G-SPEC.md` and `C2G-NOTES.md`; 198 paths, no content change, so history follows
    each file;
  - `511c30f6` "callers, commands, environment variables and documents": `bin/`, `test/`,
    `tools/`, `go/`, `overlay/`, `arbace/` and the documents: the namespaces, the test
    namespaces, the commands, `J2A_*`, `G2A_*`, `A2G_*`, the overlay directives, the generated
    program's `a2g_*.go` and `A2g_*`; 308 files, 3,352 lines;
  - `f95d8bc0`: g2a's test properties; `arbace.j2c.rename` moved to `arbace.c2a.rename` (the
    historical derivation keeps the name `arbace.j2c` in its records);
  - `5dc49419`, `1b344b45`: the journal (the rename's open points, the result);
  - the `version` branch then merged main and followed (`b0bbc07d`: "the version's names
    follow: a2g, A2G-SPEC").
- **The rules it set**: no shims for the old command names; living documents use the new names,
  dated passages included, each with a note of the old ones; the journal, the vendored files'
  provenance lines and doc/G2C-SURVEY.md keep the old names.
- **The checks**: `bin/gate` (7m04s); `bin/j2a-check` 183 of 183; g2a's print and convert tests
  and `bin/g2a roundtrip amd64` without regressions; `bin/jrt-convert`; the Go build, its smoke
  test and the test build; the Go oracle as recorded (46 known); Clojure's suite on Go 20,506 of
  20,520; `bin/oracle check jvm`; `bin/jrt test`.
- **What was easier then**: the seed was the jar of `arbace-for-java-26-v1`, which knew none of
  the tools' names, and the tools ran on the JVM's stages from the checkout's sources: nothing
  embedded, no re-seed. Today §4.3 applies.

## 6. Recommendation and proposals

Recommended: scheme A, all tools in one change, after the branch `g2a-full` (g2a's printer
mode `:full`, running) is merged, so that no open branch is left on the old names; the printed
program's names moved once to a prefix tied to no tool; then a re-seed. The proposals:

- **N1 The principle.** (a) Bridges per language (one tool, verbs `read` and `print`),
  translators between forms dialects named by the pair, no bare `c` (the user's); (b) the x2y
  scheme for all (C-full); (c) plain names (B). *Recommended: (a)*: it is the only scheme in
  which g2a's two directions have one honest name.
- **N2 The scope.** (a) All of j2a, g2a, a2g, c2a at once; (b) the bridges only (A'); (c) g2a
  only; (d) none: the principle recorded, the names kept. *Recommended: (a)*: one re-seed, one
  set of notes; (b) leaves "a2g" beside "gaf print"; (c) and (d) leave the user's objection.
- **N3 The dialect codes** (§3.1): `ae`, `cf`, `gf`, `clj`, with `java`/`j` and `go`/`g` for the
  texts; a later Go dialect takes its own. *Recommended: adopt*, in the translators' names and
  in the docs' prose ("cf → gf").
- **N4 Java's bridge.** (a) `jaf` (`read`; `print` when written); (b) `jform`; (c) j2a kept.
  *Recommended: (a)*, the user's name; the JavaBeans Activation Framework's acronym is no
  conflict inside Arbace.
- **N5 Go's bridge.** (a) `gaf`, with `read` (today's `dump` + `convert`), `print`, `build`,
  `roundtrip`; (b) `gaf` for read and print, the build as its own tool (`gobuild`); (c) `g2g`.
  *Recommended: (a)*: the build is print plus Go's toolchain, and its callers (jrt, the box,
  the vendoring scripts) already go through one command.
- **N6 The class forms → Go forms translator.** (a) `cf2gf`; (b) a2g kept (A'); (c) a word name
  (`gogen`). *Recommended: (a)*: it is the largest part of the rename, but the one whose name
  overlaps another tool's work.
- **N7 Clojure → Arbace.** (a) `clj2ae`; (b) c2a kept. *Recommended: (a)*: it is right but for
  the bare `c`, and it is cheap (307 occurrences).
- **N8 The back end.** (a) `arbace.aot` kept, described as `ae → cf`; (b) `ae2cf`.
  *Recommended: (a)*: it is part of Arbace's compiler, not a tool; `aot_*` names the compiled
  packages and `ARBACE_AOT` the build's switch, and neither misleads.
- **N9 The printed program's names** (`a2g_*.go`, `A2g_*`, `a2g_header`, the directives
  `a2g/variant`, `a2g/cut`, `a2g/add`..., `:a2g/line`). (a) Follow the translator (`cf2gf_*`);
  (b) a prefix tied to no tool, chosen once (for instance `ae_classes.go`, `Ae_Truth`,
  `ae_header`, directives `go/variant`, `go/cut`, `go/add`); (c) a2g's prefix kept as a fixed
  mark, documented as historical. *Recommended: (b)*: it was renamed once already (c2g → a2g),
  and a third rename of the translator would otherwise reach into every generated package
  again; (c) is cheapest but leaves the old name in 951 of the seed's files and jrt's sources.
- **N10 The environment variables.** (a) Renamed with their tools (`JAF_*`, `GAF_*`,
  `CF2GF_*`), and `G2A_GOROOT` → `ARBACE_GOROOT` (it names Arbace's toolchain for every build,
  not g2a's); (b) kept. *Recommended: (a)*.
- **N11 Shims** (§4.5). (a) None; (b) aliases and fallbacks; (c) none, with a guard for
  `G2A_GOROOT`. *Recommended: (c)*.
- **N12 The documents.** (a) `A2G-SPEC.md` → `CF2GF-SPEC.md`, `A2G-NOTES.md` → `CF2GF-NOTES.md`,
  `C2A.md` → `CLJ2AE.md`; the others keep their names (they name a topic, not a tool);
  G2C-SURVEY.md and the journal kept; a "Names:" note in each living document that records
  earlier work; CLAUDE.md's convention line extended (the user's approval). *Recommended: (a)*.
- **N13 The procedure.** (a) The precedent's: one branch; a `git mv`-only commit, then a
  content commit, then the regenerated records (`bin/jrt-convert update`, the fixtures, the
  edge); the evaluated first build, the gate and the targeted `--full` checks (§4.3.5); merged;
  the fixpoint and the re-seed from the merge's G3, `bin/seed --verify`; the journal. (b) Two
  steps: the tools first, the printed names (N9) in a later change with its own re-seed.
  *Recommended: (a)*: one evaluated build and one re-seed instead of two.

## 7. Sources

- doc/JOURNAL.md, 2026-10-16: "a note on the translators' names (the user)", "the naming note
  refined (the user)"; doc/AGENDA.md, Later.
- The journal on `arbace-for-golang` (`e358a786`, tag `arbace-for-go1.27.1-v1`), 2026-10-10:
  the decision (c2a and the renames); the journal at the tag `arbace-inside-itself-v1`,
  2026-10-11: "after the freeze: the .ae rename, c2a, Arbace's version", "c2a; the tools renamed
  j2a, g2a, a2g".
- Commits `42ef0e75`, `043300bb`, `511c30f6`, `f95d8bc0`, `5dc49419`, `1b344b45`, `b0bbc07d`
  (`git log --all --grep=renam -i`).
- doc/classes/SPEC.md, doc/go/SPEC.md, doc/go/A2G-SPEC.md, doc/C2A.md,
  doc/classes/CONVERTER-NOTES.md, doc/go/HELPER-NOTES.md, doc/STANDALONE-NOTES.md (S6, the
  build's time), doc/G2C-SURVEY.md, doc/STANDALONE-SURVEY.md (the form of this survey).
- `arbace/lang/RT.ae` (`toolSource`), `bin/lib/frozen.bash`, `bin/lib/version.bash`,
  `arbace/j2a/names.ae` (the jdk headers), the scripts' headers in `bin/`.
- The seed `seed/arbace-seed-go.tar.xz` at `a8431399` (`SEED.edn`: printed from `51300c07`).

## Decided (2026-10-17, the user)

The user, in place of the survey's `jaf`/`gaf`: "i prefer something, like \"repr/java\" and
\"repr/go\", where \"repr\" stands fo read/print and prefixes \"representation\"". Decided:

- **The bridges** are the `repr` family: `repr/java` (today j2a) and `repr/go` (today g2a), as
  spoken and in the docs; namespaces `arbace.repr.java` and `arbace.repr.go`; two commands,
  `bin/repr-java` and `bin/repr-go`, each with the verbs `read` and `print` (and the bridge's
  checks beside them).
- **The translators keep their names:** a2g (class forms → Go forms) and c2a (Clojure →
  Arbace); so N6 and N7 are not taken, and the printed program's `a2g_*` names (N9) stay.
- **g2a's `build`** (running `go build` on a printed module with its overlay) is not a
  representation: it becomes its own namespace, `arbace.build.go`.
- **Environment variables:** `G2A_GOROOT` becomes `ARBACE_GOROOT` (every build's Go); the
  bridges' own become `REPR_GO_*` and `REPR_JAVA_*`; a2g's and c2a's are unchanged.
- **No shims;** a guard stops a script with a message when `G2A_GOROOT` is set and
  `ARBACE_GOROOT` is not.
- **All at once** on a branch, as the 2026-10-11 rename (a `git mv` commit, a content commit,
  the regenerated records, the gate, the fixpoint), then one re-seed; then `repr/java print`.
- What keeps the old names: the frozen branches, the tags, the dated journal entries, G2C-SURVEY,
  the provenance lines.

**Added (2026-10-17, the user):** "the term \"class forms\" might be better as \"java forms\", i
propose renaming a2g to j2g for now, then we may revisit both j2g and c2a later, much later".
Decided with it: in the same rename and re-seed; the printed program's names follow (`a2g_*.go`
→ `j2g_*.go`, `A2g_*` → `J2g_*`, the `a2g/*` directives); "class forms" → "Java forms" in docs
and prose, the front end's namespaces `arbace.classes.*` kept for now; `A2G_*` → `J2G_*` with the
same guard. c2a unchanged; j2g and c2a to revisit later.

## The rename as built (2026-10-17)

On the branch `repr-rename` (from main at `11bf490e`), as the precedent (§5): a `git mv`-only
commit, a content commit for every caller, then the regenerated records; the gate and the
fixpoint after. The re-seed and the journal are the lead's, after the merge.

The user added on the same day (binding): a2g becomes **j2g**, the translator from Java forms to
Go forms, the printed program's names with it (`j2g_*.go`, `J2g_*`, `j2g_header`, the
directives `j2g/*`), its variables `J2G_*` with a guard for the old `A2G_*`; and the term
**class forms becomes Java forms** in documents, comments, messages and prose, while the front
end keeps `arbace.classes.*` and doc/classes/SPEC.md its name (to revisit with j2g and c2a).
c2a keeps its name. Built on the same branch as two more commits of the same kinds (a `git mv`,
then the content), before the gate and the fixpoint.

### Names

| before | after |
|---|---|
| j2a: `arbace.j2a.main`, `arbace.j2a.*` (`arbace/j2a/`) | repr/java: `arbace.repr.java` (`arbace/repr/java.ae`), `arbace.repr.java.*` (`arbace/repr/java/`) |
| g2a: `arbace.g2a.main`, `arbace.g2a.*` (`arbace/g2a/`) | repr/go: `arbace.repr.go` (`arbace/repr/go.ae`), `arbace.repr.go.*` (`arbace/repr/go/`) |
| `arbace.g2a.build` (`arbace/g2a/build.ae`) | `arbace.build.go` (`arbace/build/go.ae`) |
| `bin/j2a -m arbace.j2a.main convert` / `jdk`, `-m arbace.j2a.coverage` | `bin/repr-java read`, `read-jdk`, `coverage` (`main ARGS...`: arbace.lang.Main's arguments) |
| `bin/j2a-check` | `bin/repr-java check` (`bin/lib/repr-java/check`) |
| `bin/g2a convert`, `read`, `build` (no argument: the helper) | `bin/repr-go read`, `read --dump`, `helper` |
| `bin/g2a build ARGS` | `bin/build-go ARGS` |
| `bin/g2a dump`, `corpus`, `roundtrip` | `bin/repr-go dump`, `corpus`, `roundtrip` |
| `bin/g2a print`, `bin/g2a-print` | `bin/repr-go print` |
| `bin/g2a-check` | `bin/repr-go check` (`bin/lib/repr-go/check`) |
| `bin/g2a test`, `bin/g2a-convert-tests`; `bin/g2a-print-tests` | `bin/repr-go test read`; `test print` (`bin/lib/repr-go/test-read`, `test-print`) |
| `bin/g2a-build-tests` | `bin/build-go test` (`bin/lib/build-go/test`) |
| `bin/lib/g2a-host.bash` (`g2a_run`, `g2a_exec`) | `bin/lib/repr-go-host.bash` (`repr_go_run`, `repr_go_exec`), shared by `bin/repr-go` and `bin/build-go` |
| `test/g2a/`, `test/g2a/build/`, `test/j2a/` | `test/repr/go/`, `test/build/go/`, `test/repr/java/` |
| test namespaces `g2a.convert-test`, `g2a.print-test`, `j2a.build`; properties `g2a.*` | `repr.go.convert-test`, `repr.go.print-test`, `repr.java.build`; `repr.go.*` |
| the print tests' directive `;; g2a-test:` | `;; repr-go-test:` |
| the program's data marker `res/.g2a-data` | `res/.build-go-data` (the build reads it) |
| `G2A_GOROOT` | `ARBACE_GOROOT` |
| `G2A_GO_EXE`, `G2A_HOST_ID`, `G2A_DUMPS`, `G2A_JOBS`, `G2A_GOOS`, `G2A_GOARCH`, `G2A_GOCMP_FLAGS`, `G2A_OP`, `G2A_SEED`, `G2A_LEVELS` | `REPR_GO_EXE`, `REPR_GO_HOST_ID`, `REPR_GO_DUMPS`, `REPR_GO_JOBS`, `REPR_GO_GOOS`, `REPR_GO_GOARCH`, `REPR_GO_GOCMP_FLAGS`, `REPR_GO_OP`, `REPR_GO_SEED`, `REPR_GO_LEVELS` |
| `G2A_HOST` (only `go` allowed since S4) | dropped |
| `J2A_JVM_OPTS`, `J2A_CP`, `J2A_THREADS`, `J2A_PATCH_ALL`, `J2A_TEST_JOBS`, `J2A_DEBUG` | `REPR_JAVA_JVM_OPTS`, `REPR_JAVA_CP`, `REPR_JAVA_THREADS`, `REPR_JAVA_PATCH_ALL`, `REPR_JAVA_TEST_JOBS`, `REPR_JAVA_DEBUG` |
| `.tmp/g2a/`, `g2a-host`, `g2a-print-cache`, `g2a-check`, `g2a-*-tests`, `j2a-check`, `j2a-jdk`, `j2a-patch`; `j2a-report.edn` | `.tmp/repr-go/`, `repr-go-host`, `repr-go-print-cache`, `repr-go-check`, `repr-go-read-tests`, `repr-go-print-tests`, `build-go-tests`, `repr-java-check`, `repr-java-jdk`, `repr-java-patch`; `repr-java-report.edn` |
| the Makefile's `g2a-roundtrip`, `g2a-roundtrip-text` | `repr-go-roundtrip`, `repr-go-roundtrip-text` |
| the gate's steps `j2a`, `g2a` (logs `.tmp/gate/`) | `repr-java`, `repr-go` |
| provenance key `:converter {:j2a ...}` (jdk/, the fixtures) | `:converter {:repr-java ...}`, over `arbace/repr/java.ae`, `arbace/repr/java/` and `bin/repr-java` |
| a2g: `arbace.a2g.*` (`arbace/a2g/`), `test/a2g/` (`a2g.*`) | j2g: `arbace.j2g.*` (`arbace/j2g/`), `test/j2g/` (`j2g.*`) |
| `bin/a2g`, `a2g-check`, `a2g-edge`, `a2g-evalproof` | `bin/j2g`, `j2g-check`, `j2g-edge`, `j2g-evalproof` |
| `doc/go/A2G-SPEC.md`, `A2G-NOTES.md` | `doc/go/J2G-SPEC.md`, `J2G-NOTES.md` |
| the printed program: `a2g_*.go`, `reflect_tables_a2g.go`, `A2g_*` (jrt's support names), `a2g_header`; the overlays' `a2g/variant`, `a2g/cut`, `a2g/add`...; `:a2g/line` | `j2g_*.go`, `reflect_tables_j2g.go`, `J2g_*`, `j2g_header`; `j2g/variant`, `j2g/cut`, `j2g/add`...; `:j2g/line` |
| `aot_arbace_a2g_*` (the compiled namespaces' packages) | `aot_arbace_j2g_*` |
| `target/arbace-go/a2g/`, `.tmp/a2g-host` | `target/arbace-go/j2g/`, `.tmp/j2g-host` |
| `A2G_JOBS`, `A2G_GO_EXE`, `A2G_OUT`, `A2G_ARCH`, `A2G_SHOW`, `A2G_ANALYSIS_TIMES`, `A2G_CLASS_TIMES`, `A2G_JVM_OPTS`, `A2G_EDGE_WORK`, `A2G_EDGE_TRACE_ARGS`, `A2G_PROF`, `A2G_FIXTURES`, `A2G_TRACE`, `A2G_JDK_REACH`, `A2G_EMBED_ONLY` | `J2G_*` likewise |
| `A2G_HOST` (only `go` since S4) | dropped |
| class forms (the term) | Java forms (`arbace.classes.*`, doc/classes/SPEC.md's name, and hyphenated identifiers and data keys such as `class-forms` kept) |

Kept: c2a and its names; the frozen branches and the tags; doc/JOURNAL.md, doc/G2C-SURVEY.md, this survey's sections above; the
branch names the notes cite (`s3-g2a`, `speed2-g2a`, `g2a-params`, `g2a-cases`, `speed2-a2g`,
`s4-a2gpar`, `a2g-core`, `a2g-2a`...`a2g-2d`); `doc/go/java-surface.edn`, a record measured on
2026-10-11 (on the JVM's stages, which S4 removed), and its treatment key `:a2g` in
`test/jrt/java_surface.ae`, which writes and reads it; the provenance
lines of the vendored derivation (`arbace/lang.ae`, `arbace/java/api.ae`: "written by
arbace.j2a from Clojure ..."); doc/AGENDA.md (the lead's).

### What changed besides the spelling

- `RT.toolSource` names `arbace/repr/` and `arbace/build/` (beside `arbace/j2g/`, `c2a/`,
  `aot/`): the bridges' main namespaces are files beside their directories
  (`arbace/repr/go.ae` next to `arbace/repr/go/`), so the prefixes are the parents'. a2g's
  `embed/excluded` leaves out `arbace/repr/java.ae` and `arbace/repr/java/` (javac);
  `bin/lib/frozen.bash`'s tools view links `arbace/repr/{java,go}`, `arbace/build`.
- The print cache's host identity (`REPR_GO_HOST_ID`) hashes `arbace/repr/go.ae`,
  `arbace/repr/go/*.ae` and `arbace/build/*.ae` (g2a's directory held all three before); the
  vendoring scripts' tree keys hash the same and both commands.
- The guard (N11 (c)): `bin/lib/renamed-env.bash`, sourced by every script that reads
  `ARBACE_GOROOT` or `J2G_*` (`bin/arbace-go`, `build-go`, `repr-go`, `j2g`, `j2g-*`, `seed`,
  `gate`, `jrt`, `jrt-coverage`, `jrt-convert`, `clojure-tests`, `repr-java`, the box's and
  the vendoring scripts, the bridges' checks), stops (exit status 2) with a message naming the
  new variable when `G2A_GOROOT` is set and `ARBACE_GOROOT` is not, or when any other `G2A_*`,
  `J2A_*` or `A2G_*` is set (R2 below; until then only `G2A_GOROOT` and `A2G_*`).
- The mains' verbs: `arbace.repr.go`'s `convert` is `read`; `arbace.repr.java`'s `convert` and
  `jdk` are `read` and `read-jdk`.
- The first build after the rename runs the renamed tools evaluated on today's stage 0
  (§4.3.2): its seed embeds and compiles `arbace.g2a.*` and `arbace.a2g.*`, not
  `arbace.repr.go.*` and `arbace.j2g.*`.
- The term's change caught three uses of the class `Class` in code (`^Class form`,
  `(instance? Class form)`, `(cast Class form)` in `arbace/classes/types.ae` and
  `arbace/lang/Compiler.ae`), restored at once (the edge's recording failed on the first).

### Regenerated

- `jdk/` (the 911 headers name `arbace.repr.java`; `provenance.edn`'s `:converter
  {:repr-java ...}`): `bin/jrt-convert update`, run twice (after the bridges' commit, and again
  after the term's change touched repr/java's comments, which its provenance hash covers). Only
  the header lines and the provenance changed.
- `test/java-fixtures/` (headers, provenance): `bin/clojure-tests --java-fixtures update`, twice
  likewise; `test/repr/java/expected/` (headers): `bin/repr-java check --update`. The baseline's
  new SHA-256 list equals the old one with the headers spelled back (checked).
- `jdk/edge.edn`: `bin/j2g-edge update`, then `verify`. Besides its header naming j2g, its
  `:exists` gained seven `java.util` classes (`ArrayList`, `HashMap`, `HashSet`,
  `LinkedHashMap`, `LinkedHashSet`, `concurrent.ConcurrentHashMap`, `atomic.AtomicInteger`)
  that j2g's reachability has named since `2352fb2b` (2026-10-16): main's edge was stale since
  then, which only `bin/gate --full`'s edge verify would have shown.
- Not regenerated, edited with their generators: the reader tests' expected units' headers,
  the round trip's references' first lines, `arbace/lang/go/`'s generated files,
  `test/oracle/forms/defclass_corpus.ae`'s header.

### Commits

`789b8534` the bridges' `git mv`; `42737178` their callers; `1e242417` the records; for j2g its
`git mv` (`cc9e0b73`), then `ae758429` the callers, the printed names, the
variables, the term; `b4a7b38c` the edge, and three `Class` hints the term
had caught; `58428c77` the provenance again.

### Checks (all on the branch, amd64)

- `bin/gate`: passed in 10m18s (stage 0 from the seed, the executables built in 490 s with the
  renamed tools evaluated on stage 0; Clojure's suite 20,506 of 20,520, no regressions; the
  oracle 53 known mismatches, 0 new).
- `bin/arbace-go --fixpoint`: holds, G1 = G2 = G3 byte for byte (`425c4369...`, 103,760,032
  bytes; j2g's program G1 = G2 = G3 too), 6m19s.
- `bin/repr-go roundtrip amd64`: no regressions; `--text amd64`: 1,705 of 1,708 test files and
  1,742 of 1,742 std files byte-identical, the 3 differences known. `bin/repr-go test read` 7
  tests, 164 assertions; `test print` 5 tests, 268 assertions; `bin/build-go test` 26 passed.
- `bin/repr-java check` (its default checks, on the frozen JVM): baseline 189 files, java 11,
  module 6, as expected.
- `bin/j2g-edge verify`, `bin/box-vendor roundtrip`, `bin/toolchain-vendor verify`: pass.
  `bin/tamago-vendor verify` fails, as it does on main at `11bf490e` with the same 20-line diff
  (checked in a worktree of that commit): `:go/label-line` on four labeled declarations of
  `tamago/goroot`, which `53c65f69` (the printer's mode `:full`) made the reader write and
  `tamago/` was not converted again after; not the rename's (`bin/tamago-vendor convert` fixes
  it, a change of its own).
- `make check-makefile`; the guard: `G2A_GOROOT=/x bin/repr-go helper`, `G2A_DUMPS=/x bin/repr-go
  helper`, `J2A_JVM_OPTS=... bin/repr-java check`, `A2G_JOBS=2 bin/j2g` stop with exit status 2
  and a message naming `ARBACE_GOROOT`, `REPR_GO_DUMPS`, `REPR_JAVA_JVM_OPTS`, `J2G_JOBS`;
  `G2A_GOROOT` with `ARBACE_GOROOT` set passes.
- Not run, as asked: the re-seed, `bin/gate --full`.

### What still says g2a, j2a or a2g

The "Names:" notes of the living documents (they give the old names on purpose); the dated
records and the branch names above; doc/JOURNAL.md, doc/G2C-SURVEY.md, doc/AGENDA.md (the
lead's) and this survey's sections before this one; `bin/lib/renamed-env.bash` (the guard's
old names) and the guard's comment in each script; `doc/go/java-surface.edn` and its key in
`test/jrt/java_surface.ae`; `arbace/lang.ae` and `arbace/java/api.ae`'s first lines (the
derivation's provenance); the seed (until the re-seed).

### Proposals of the rename as built

- **R1** `arbace.repr.java.print`, which writes Java forms as `.ae` text, renamed
  `arbace.repr.java.layout` (as repr/go's layout), so that `repr/java print` (Java forms → Java)
  can take the name.
- **R2** The guard extended to every old `G2A_*` and `J2A_*`, not only `G2A_GOROOT`.
- **R3** `doc/go/java-surface.edn`'s treatment key `:a2g` kept (a record of the JVM's stages,
  which S4 removed), or measured again.
- **R4** `tamago/` converted again (`bin/tamago-vendor convert`), in a commit of its own, for
  the `:go/label-line` drift since `53c65f69`.
- **R5** The bridges' checks as verbs of their commands (`bin/repr-java check`, `bin/repr-go
  check`, `test read|print`, `bin/build-go test`), not separate scripts.

### Decided (2026-10-17, the user)

- **R1** yes, with `repr/java print` (not in this rename).
- **R2** yes, now: done on this branch (`bin/lib/renamed-env.bash`, above).
- **R3** accepted: `:a2g` stays in `java-surface.edn` and `test/jrt/java_surface.ae`.
- **R4** yes, as a separate commit after the merge (the lead's), not on this branch.
- **R5** accepted.

**Added (2026-10-17, the user):** c2a becomes `repr/clojure` ("yes, rename c2a at the next
re-seed, print later"): `arbace.repr.clojure`, `bin/repr-clojure read` (today's c2a), at the
next re-seed (with F3 and F4's); `repr/clojure print` (Arbace → Clojure: `arbace.*` back to
`clojure.*`, `.ae` back to `.clj`, a text round trip, as repr/go's `:full`) later. Clojure and
Arbace share the reader, so this bridge runs between two dialects' texts; the few one-way rules
(a `.cljc`'s reader conditionals resolved) are recorded or documented when `print` is built.

**Added (2026-10-17, the user):** the back end becomes `c2j` ("yes, rename aot to c2j with
repr/clojure"): `arbace.aot` → `arbace.c2j` (Arbace forms, Clojure's semantics, → Java forms;
derived from Clojure's compiler), its scripts, docs and "the back end" in prose; the two
translators then read as a chain, c2j then j2g. "AOT" stays the name of the whole compiled
pipeline (c2j → j2g → `repr/go print` → Go's compiler): `ARBACE_AOT` and the printed packages
`aot_arbace_*` are kept. Done with c2a → `repr/clojure`, after F2 merges and before F3 and F4
(which change `emit.ae`), all under the one re-seed of F3 and F4.

## The second rename as built (2026-10-17)

On the branch `rename-c2j` (from main at `80974421`, after F2's merge), the user's two additions
above, by the first rename's procedure: a `git mv`-only commit, a content commit for every
caller, then the regenerated records; the gate, the fixpoint and the targeted checks after. The
re-seed (with F3 and F4), the journal and the agenda are the lead's, after the merge.

### Names

| before | after |
|---|---|
| c2a: `arbace.c2a.main` (`arbace/c2a/main.ae`) | repr/clojure: `arbace.repr.clojure` (`arbace/repr/clojure.ae`), its `-main` taking the verb `read` |
| `arbace.c2a.rename`, `rules`, `text` (`arbace/c2a/`) | `arbace.repr.clojure.rename`, `rules`, `text` (`arbace/repr/clojure/`) |
| `bin/c2a [OPTION...] -o OUT SRC...`, `--in-place`, `--stdin`, `--rules` | `bin/repr-clojure read` with the same options |
| `bin/c2a-check [--suite \| --libs [GO-EXE]]` | `bin/repr-clojure check [--suite \| --libs [GO-EXE]]` (`bin/lib/repr-clojure/check`) |
| `doc/C2A.md` | `doc/REPR-CLOJURE.md` |
| `test/c2a/` (`c2a_test.ae`, ns `c2a.c2a-test`; `libs.ae`, `libs.out`, `suite.sha256`) | `test/repr/clojure/` (`read_test.ae`, ns `repr.clojure.read-test`; the rest by name) |
| the unit tests' property `c2a.work`; `.tmp/c2a-check/` | `repr.clojure.work`; `.tmp/repr-clojure-check/` |
| the library check's sample library (`c2a-readers.point`, tag `#c2a/point`) and summary line `c2a libs:` | `repr-clojure-readers.point`, `#repr-clojure/point`; `repr-clojure libs:` |
| `bin/lib/tools.bash`'s `c2a_sources` | `repr_clojure_sources` |
| `bin/clojure-tests`' `c2a`, `c2a_tree`, `c2a_stamp`, `lib-arbace/NAME.c2a` | `repr_clojure`, `repr_clojure_tree`, `repr_clojure_stamp` (now over `arbace/repr/clojure.ae` too), `NAME.repr-clojure` |
| the fixtures' headers "renamed by c2a"; provenance `:converter {... :c2a ...}` over `arbace/c2a` | "renamed by repr/clojure"; `:repr-clojure` over `arbace/repr/clojure{.ae,/}` |
| `C2A_*` (none was read) | `REPR_CLOJURE_*`, the guard extended |
| the back end: `arbace.aot.emit`, `main`, `check` (`arbace/aot/`) | c2j: `arbace.c2j.emit`, `main`, `check` (`arbace/c2j/`) |
| its marks on the Java forms it writes: `:aot/line`, `:aot/nn`, `:aot/code`, `:aot/source`, `:aot/parts`; the continuations `(aot/continue NAME k ...)` | `:c2j/line`, `:c2j/nn`, `:c2j/code`, `:c2j/source`, `:c2j/parts`; `(c2j/continue NAME k ...)` (read by j2g's `world`, `out`, `code`, `model`) |
| the compiled tree's header ";; Compiled by arbace.aot" | ";; Compiled by arbace.c2j" |
| `test/aot/` (`gen_fns.ae`, `merge_variants.ae`) | `test/c2j/` (`CompilerAOTFn.ae`'s header names it: regenerated by it, identical) |
| `RT.toolSource`'s `arbace/c2a/`, `arbace/aot/` | `arbace/c2j/` (repr/clojure is under `arbace/repr/`, already listed) |
| `bin/lib/frozen.bash`'s tools view: `c2a` | `repr/clojure` |

The printed packages of the renamed namespaces follow their names, as every compiled
namespace's does (`aot_arbace_repr_clojure_rules`, `aot_arbace_c2j_emit`, ...).

### Kept, and why

- **The pipeline's name, AOT** (the user: "AOT" stays the whole compiled pipeline's name):
  `ARBACE_AOT`, `ARBACE_NO_AOT`; the printed packages `aot_arbace_*`; the compiled tree
  `target/arbace-go/aot/` (and `aot-tests`, `aot-host`, `.tmp/diffcheck/aot`, the box's step
  `:aot` in `arbace.box.rebuild` and `box-steps.edn`), its files `aot_NAME.ae`, `aot_NAME.k.ae`,
  its report `aot.edn` (which j2g's `--await` waits for) and key `aot.key`; the log lines `aot:`
  of `bin/arbace-go`, the box's rebuild and `bin/diffcheck`; the run-time side in the runtime,
  `Compiler$AOT` (the compiled loaders), `AOTFn`/`AOTRestFn` and the generated
  `arbace/lang/go/CompilerAOTFn.ae`. These are the pipeline's output or its run-time side, not
  c2j's own; c2j's own marks (`:aot/*`) were renamed, as `:a2g/line` was in the first rename.
- **The JDK's AOT cache** (`target/arbace.aot`, `lib/arbace/arbace.aot`, `aot-train-options`,
  `test/aot-training.ae`) in VENDOR-NOTES and SPEED-NOTES: the JVM's, a different thing.
- **"AOT output"** (class files) in `arbace.repr.clojure`'s plan and in `bin/clojure-tests`'
  steps 3 and 4 (the baseline's AOT-compiled Clojure): Clojure's sense.
- The frozen branches, the tags, doc/JOURNAL.md, doc/G2C-SURVEY.md, doc/AGENDA.md (the lead's),
  this survey's sections before this one; the "Names:" notes (which give the old names on
  purpose) and the guard's old names (`bin/lib/renamed-env.bash`); the quoted user's note in
  doc/REPR-HASKELL-SURVEY.md ("j2g and c2a to revisit later"); the seed (until the re-seed).

### What changed besides the spelling

- `bin/repr-clojure` takes verbs as `bin/repr-java` does: `read` passes its options to
  `arbace.repr.clojure`'s `-main` (`read ...`, the verb checked there too), `check` runs
  `bin/lib/repr-clojure/check`; anything else prints the usage (exit 2). No `print` yet.
- The guard (`bin/lib/renamed-env.bash`) also stops on any `C2A_*` (exit 2, naming
  `REPR_CLOJURE_*`); every script that sources it says so in its comment, and
  `bin/repr-clojure` and its check source it now.
- The living documents (README, CLAUDE.md, LICENSE.md, doc/ARBACE.md, VENDOR-NOTES,
  STANDALONE-NOTES and -SURVEY, FUTAMURA-SURVEY, EVAL-NOTES, BOX-NOTES, J2G-SPEC,
  CONVERTER-NOTES, REPR-CLOJURE, the fixtures' README) use the new names throughout, with a
  "Names:" note where they record earlier work. README and CLAUDE.md describe c2j and j2g as
  the translators (a chain, c2j then j2g) and repr/clojure as the third bridge.

### Regenerated

- `test/java-fixtures/` (the 12 headers, `provenance.edn`): `bin/clojure-tests --java-fixtures
  update`. Its `:repr-java` hash changed too, as did `jdk/provenance.edn`'s (`bin/jrt-convert
  update`: 911 of 911 forms equal, only the provenance line): both cover `bin/repr-java`, whose
  guard comment now names `C2A_*`.
- `test/repr/clojure/libs.out`: recorded from the host's output (its header's procedure); the
  host's and the Go build's outputs equal, the summary line the only change.
- `arbace/lang/go/CompilerAOTFn.ae`: edited with its generator `test/c2j/gen_fns.ae`, then
  regenerated by the gate's executable: identical.
- `jdk/edge.edn`: unchanged (`bin/j2g-edge verify` passes).

### Commits

`65b4344b` the `git mv`; `7c8bb9b9` the callers, the marks, the guard, the docs; `a9d4c0b2` the
fixtures' and jdk/'s provenance; `5fac017b` `libs.out`.

### Checks (all on the branch, amd64)

- `bin/gate`: passed (stage 0 from the seed; the executables built in 392 s with the renamed
  tools evaluated on stage 0; Clojure's suite 20,506 of 20,520, no regressions; the oracle 53
  known mismatches, 0 new). A first run failed only because the records were regenerated
  while it ran (its executables rebuilt under the suite and the oracle); run again alone.
- `bin/arbace-go --fixpoint`: holds, G1 = G2 = G3 byte for byte (`e122ade3...`, 103,837,856
  bytes; j2g's program G1 = G2 = G3), 6m19s.
- `bin/repr-clojure check`: 10 tests, 120 assertions, 0 failures; `--suite`: as recorded
  (`c72863e4...`); `--libs` (the host and the gate's Go build): 15 checks, both outputs equal
  `libs.out`.
- `bin/diffcheck`: 12,449 of 12,471 cases agree, the 22 known, 0 new.
- `bin/repr-go roundtrip amd64`: no regressions. `bin/j2g-edge verify`: passes. `make
  check-makefile`: passes. The guard: `C2A_FOO=1 bin/repr-clojure read --rules` stops with exit
  status 2, naming `REPR_CLOJURE_FOO`.
- Not run, as asked: the re-seed, `bin/gate --full`.

### What still says c2a or arbace.aot

The "Names:" notes and the dated passages that give the old names with a date
("c2a until 2026-10-17": `arbace/repr/clojure.ae`'s and `rename.ae`'s doc strings,
`bin/repr-clojure`'s and its check's headers, README, CLAUDE.md's convention line,
doc/ARBACE.md); `bin/lib/renamed-env.bash`; doc/JOURNAL.md, doc/G2C-SURVEY.md, doc/AGENDA.md and
this survey's sections before this one; the quote in doc/REPR-HASKELL-SURVEY.md; the seed (it
embeds and compiles `arbace.c2a.*` and `arbace.aot.*`, until the re-seed: the first build after
the merge runs the renamed tools evaluated on stage 0, as the gate and the fixpoint did here).
Hex constants that spell `C2A` (jdk/, tamago/) are not names.

### Proposals of the second rename as built

- **R6** `test/c2j/merge_variants.ae` merges the Go build's variants of `RT`, `Main` and
  `Compiler` into their bases (SA150), which is not c2j's; it moved with `test/aot/` as asked.
  To `test/lang/go/` (beside what it merges into)? *Recommended: leave it with `gen_fns.ae`
  until a `test/lang/` exists.*
- **R7** The runtime's side of the pipeline keeps "AOT" (`Compiler$AOT`, `AOTFn`, `AOTRestFn`,
  `CompilerAOTFn.ae`, `ARBACE_NO_AOT`), as the pipeline's name. *Recommended: keep.*
- **R8** c2j's marks renamed `:c2j/*` (and `c2j/continue`) rather than kept as `:aot/*`, as the
  first rename renamed `:a2g/line`: they are what c2j writes and j2g reads. *Recommended: keep
  the rename (done).*

**Decided (2026-10-17, the user):** R6-R8 accepted as built (`test/c2j/merge_variants.ae` stays
where it moved; "AOT" kept on the runtime's side as the pipeline's name; c2j's marks `:c2j/*`).
