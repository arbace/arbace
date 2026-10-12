# The standalone Arbace — notes

Working notes of the standalone stages ([STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), §9.1; the
user's decisions D1-D13 of 2026-10-12): what each stage did, how it is checked, and its
amendments (SA1...), proposed to the user, the recommended choice applied meanwhile. S1 (the
JDK's forms committed in `jdk/`) is recorded in [go/JRT-SOURCES.md](go/JRT-SOURCES.md), "The
committed forms", and `jdk/README.md`.

## S2: a2g without the JVM's reflection and ASM

### What a2g took from the JVM

a2g ran on the JVM's `target/stage2` and learned what it could not read from the class forms by
asking the running JVM (survey §4.3). Measured on main at `ff53e17`, by recording every such
question of a `bin/a2g --program` run and of the test build's (`--input` the suite's Java
fixtures):

- **the class environment** (`arbace.classes.env/info`): classes neither compiled from the
  forms nor declared were loaded (`RT/classForNameNonLoading`) and reflected
  (`getDeclaredMethods`, `getDeclaredConstructors`, the fields and their constants read from the
  class file with ASM's `ClassReader`, the methods' annotations likewise). 403 classes: jrt's
  hand-written ones (`java/lang/Object`, `String`, `Class`, `Thread`, `System` ...), the JDK's
  classes the world names without holding them (annotation types, `jdk/internal` classes behind
  cut code, `java/sql`, `org/xml` ...), and ASM's own classes, which `arbace/lang`'s compiler
  names and the Go variants erase (A2G-SPEC §4.6);
- **the analyzer's other questions** (`arbace.classes.analyze`): a class's existence, its type
  parameters, generic signatures (javac's bridges), an annotation type's `@Retention` and
  `@Target`, whether an interface is a `@FunctionalInterface` and its method (Clojure's
  `FISupport`), a record's components; and `FnInvokers`' invoker methods, by `getMethod` on the
  JVM's own `arbace.lang.FnInvokers`;
- **a2g's own questions**: an enum constant's ordinal (`Enum/valueOf`), whether an interface is
  functional (`fromfn`), the JVM's order of a class's overloads (`getDeclaredMethods` of the
  JDK's CDS-archived classes: the member tables follow it, as `Reflector` does), the public
  members and constants of the cut classes (their member tables), which classes the embedded
  namespaces name exist on the JVM and the types their members name (the cut candidates), and
  which JDK packages their modules do not export (the REPL's world);
- **ASM writing classes**: `arbace.a2g.access` undid javac's accessor methods (`access$NNN`) by
  writing each class's accessors with ASM's `ClassWriter` and reading back the one member
  instruction each holds.

On the Go executable none of this exists: its reflection lists only the public members of the
world's classes (CLASSFORMS-REPL.md §7), it has no class files, and ASM is cut (erased).

### The edge: the JVM's answers as data

`jdk/edge.edn` holds the answers (amendments SA2, SA3). One a2g run records them
(`bin/a2g --record-edge FILE`: `arbace.classes.env`'s `*recorded*` and `arbace.a2g.edge`'s
`*recording*`), `bin/a2g-edge update` merges the records of the builds' two worlds (the program,
`--program`; the test build, `--program --input FIXTURES`) into it, and every other a2g run reads
it (by default; `--edge FILE` names another) and binds `arbace.classes.env/*declared*` to its
classes. With it bound, `env/load-class` loads nothing, `env/info` answers from the declared
infos, and every other question goes through one function, `env/reflected` (a class's facet:
`:tparams`, `:generics`, `:retention`, `:targets`, `:fi-method`, `:record-components`,
`:functional`, `:enum-constants`), or, for a2g's own, `arbace.a2g.edge/jvm` and `jvm?`
(`:method-order`, `:cut`, `:exists`, `:member-types`, `:unexported`). A class neither compiled
nor declared does not exist; a declared class whose facet was never recorded is an error that
names it (`bin/a2g-edge update`). The class path's sources are not looked up either
(`analyze/*source-lookup*` false, as on the Go build), which no a2g run needed.

The file (Clojure-reader data, `arbace.a2g.edge/edge-text`):

```clojure
{:classes
 {"java/lang/Object"
  {:info {:fields [...] :flags 1 :interface? false :interfaces [] :methods [{:desc "()V" :flags 1 :name "<init>"} ...]
          :name "java/lang/Object" :nest-host "java/lang/Object" :outer nil :outer-instance? false :super nil}
   :exists true :tparams [] ...}
  ...}
 :cut {"arbace/asm/ClassReader" {:ctors [{:mods 1 :params ["[B"]} ...]
                                 :fields [{:const 8 :mods 25 :name "EXPAND_FRAMES" :type "I"} ...]
                                 :methods [...]} ...}
 :exists #{"arbace.asm.Opcodes" "java.io.File" ...}
 :member-types {"arbace/asm/ClassReader" ["java/io/InputStream" "java/lang/Class" ...] ...}
 :method-order {"java/io/BufferedWriter" {"write" [["[C" "int" "int"] ["java.lang.String" "int" "int"] ["int"]]} ...}
 :unexported #{"jdk.internal.misc" ...}}
```

`:info` is `env`'s class info in its own shape (`reflect-info`'s), less what is implied: the
owner of each member (the class), a field's nil constant, a method's empty `:throws`,
`:annotations` and `:param-annotations` (a constructor has neither); reading restores them, the
constants to their field's type, the annotations delayed. Maps are written with sorted keys,
sets sorted, strings with `\uXXXX` escapes outside printable ASCII, one member per line.
A class's methods keep the JVM's order when the class is in the JDK's CDS archive (a2g's output
depends on it: the order of an enum's `compareTo` forwarder among its methods follows
`java/lang/Enum`'s); for the others (ASM's, the JDK's outside the archive), whose order
HotSpot varies from one process to the next, they are sorted by name and descriptor.

Size: 403 classes with about 10,300 members (whole member lists: overload resolution and the
interfaces' method sets need every member, not only the ones used), 114 cut classes,
1.40 MB, 14,619 lines. The survey's 226 classes and 1,223 members counted the members the
translated JDK classes reference across the edge, not what a2g asks.

### Accessors as data

`arbace.classes.analyze/add-accessor!` keeps, beside each accessor's bytecode-writing function,
its member instruction as data in the class state's `:accessors` (`{:insn :getfield ... :owner
:name :desc :itf}`), and `arbace.a2g.access` reads it: no class is written. `FnInvokers`' invoker
is looked up in its class info (`env/info!`: its class forms in a2g's world, reflection on the
JVM's compiler) rather than by `Class/getMethod`.

### The id floor (SA4)

a2g's output names the analyzer's and the translator's temporaries by the runtime's id counter
(`gensym`, `RT/nextID`: `arr_10021`, `t_14172`), and the counter's value when a2g starts depends
on how much code the running Arbace compiled before: a2g's own namespaces load from source, so
any change to a2g renumbered every temporary of the program (S2's code, 458 ids more, would
have), and a2g on the Go executable (S3) would not reproduce the JVM's program. a2g now advances
the counter to a fixed value, `arbace.a2g.main/id-floor` (100000), before anything else; its
output depends on its inputs alone.

### Checks

- `bin/a2g-edge verify` records the edge afresh and compares it with `jdk/edge.edn` byte for
  byte; `bin/gate --full` runs it in its go chain, after `jrt-convert` (SA6).
- `bin/a2g-edge trace` runs `bin/a2g --program` under JFR's method tracing (JEP 520) of the
  JVM's reflection and class loading (`Class.forName`, `getDeclared*`, `getMethods`,
  `getFields`, `getConstructors`, `getTypeParameters`, the generic types, annotations, enum
  constants, record components, the nest host, the declaring class, `Field.get`, the class
  path's resources, the module layer, `RT.classForName`) and of ASM's classes (`ClassReader`,
  `ClassWriter`, the visitors, `Type`, `TypeReference`), and fails when a traced call is made
  inside a2g's run (a frame of `arbace.a2g.main$run` on its stack), but for the runtime's own
  member lookup (`arbace.lang.Reflector`, behind a2g's own untyped interop) and
  `TypeReference`'s arithmetic (below).
- What stays: the analyzer still computes type annotations' target values with ASM's
  `TypeReference` (integer arithmetic: no class file; the Go build substitutes
  `arbace.classes.go`'s), and a2g's and the analyzer's code use `Opcodes`' constants. Both leave
  with ASM in S4. a2g itself runs on the JVM until S3.

### Results (2026-10-12)

RESULTS

### Amendments

Proposed to the user; the recommended choice is applied on the branch `standalone-s2`.

- **SA1 Where S2's notes live.** (a) This file, `doc/STANDALONE-NOTES.md`, the standalone
  stages' notes from S2 on, with a paragraph in A2G-SPEC §4.1 pointing here; (b) a new section of
  A2G-SPEC.md (4,455 lines already); (c) A2G-NOTES.md. *Recommended: (a)*: S3 and S4 change the
  bootstrap, not only a2g.
- **SA2 The edge as a recorded file in `jdk/`.** (a) `jdk/edge.edn`, recorded from what a2g's
  runs for the builds ask the JVM (`bin/a2g --record-edge`, `bin/a2g-edge update`), verified by
  recording again (`bin/a2g-edge verify`, in `bin/gate --full`); (b) a closed definition (every
  class the world's forms name outside the world, with its supertypes), generated without
  running a2g: larger, and it would still miss a2g's own questions (cut classes, overload
  order); (c) the file beside jrt's manifest (`go/arbace/jrt/`) or in `arbace/a2g/` (which holds
  only `.ae`). *Recommended: (a)*, in `jdk/` beside the forms it completes: like them generated
  on the JVM, committed, verified, and hashed by the gate's key. The edge covers the builds'
  two worlds; a2g's other worlds (bin/a2g-check's fixtures, bin/a2g-evalproof's roots) gave the
  same program with the edge as with the JVM's reflection; another world that asks more names
  the missing class or facet, and `--jvm-reflection` runs it on the JVM.
- **SA3 Whole classes, the JVM's order kept where it is stable.** (a) every member of each class
  asked about (403 classes, about 10,300 members, 1.40 MB), the methods of the JDK's
  CDS-archived classes in the JVM's order, the others sorted; (b) only the members used (the
  survey's estimate, 1,223): overload resolution and interface method sets need them all, and
  the program changed. *Recommended: (a)*. ASM's 15 classes in it go with ASM in S4.
- **SA4 The id floor.** (a) a2g advances the runtime's id counter to 100000 before it starts, so
  that its output depends on its inputs alone (needed by S3, whose G2 = G3 compares a2g on the
  Go executable with a2g on the JVM); every temporary of the program is renumbered once
  (`arr_10021` becomes `arr_1xxxxx`); (b) the floor at main's value (9022), which kept the
  program's names: impossible, S2's a2g itself loads 458 ids more (9480); (c) no floor: every
  change to a2g renumbers the program's temporaries, as today, and S3's comparison fails;
  (d) a2g's own counter for the analyzer's names: a change of the analyzer the JVM's compiler
  shares. *Recommended: (a)*.
- **SA5 The exit criterion as met.** The survey's "the same program byte for byte" cannot hold
  literally: the Go executable embeds the class forms' analysis (`arbace/classes/env.ae`,
  `analyze.ae`, for the REPL's class forms, CLASSFORMS-REPL.md), which S2 changes, and SA4
  renumbers. Proven instead (Results): a2g's program tree equals main's a2g's, run with the same
  id floor, but for the two embedded sources; with S2's two sources on both sides, the
  executables (`--build` amd64 and arm64, `--build-tests`, `--build --box` amd64 and arm64) are
  byte-identical; the class forms compiler's output on the JVM (stage 2's `arbace/lang`,
  `arbace/asm`, `arbace/java/api`) is unchanged. *Recommended: accept*; the alternatives
  (freezing a copy of main's analyzer as the Go build's variant, or the analyzer's changes
  undone by a2g at run time) would cost more than they prove.
- **SA6 `bin/a2g-edge verify` in `bin/gate --full`.** (a) in the go chain after `jrt-convert`
  (two a2g runs, about 3.5 minutes, sequential for memory); (b) only by hand, like `trace`.
  *Recommended: (a)*: a change that asks the JVM something new must update the edge, and only
  the JVM can tell.
