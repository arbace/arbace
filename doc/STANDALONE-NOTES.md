# The standalone Arbace — notes

Working notes of the standalone stages ([STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), §9.1; the
user's decisions D1-D13 of 2026-10-12): what each stage did, how it is checked, and its
amendments (SA1-SA9 for S4's preparation, SA10... for S2, SA30... for S3), proposed to the user, the
recommended choice applied meanwhile. S1 (the
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

`jdk/edge.edn` holds the answers (amendments SA11, SA12). One a2g run records them
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

### The id floor (SA13)

a2g's output names the analyzer's and the translator's temporaries by the runtime's id counter
(`gensym`, `RT/nextID`: `arr_10021`, `t_14172`), and the counter's value when a2g starts depends
on how much code the running Arbace compiled before: a2g's own namespaces load from source, so
any change to a2g renumbered every temporary of the program (S2's code, 458 ids more, would
have), and a2g on the Go executable (S3) would not reproduce the JVM's program. a2g now advances
the counter to a fixed value, `arbace.a2g.main/id-floor` (100000), before anything else; its
output depends on its inputs alone.

### Checks

- `bin/a2g-edge verify` records the edge afresh and compares it with `jdk/edge.edn` byte for
  byte; `bin/gate --full` runs it in its go chain, after `jrt-convert` (SA15).
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

On the branch `standalone-s2`, with main at `2035f7b` (TLS merged) merged in; "main+floor" is
main's a2g with the same `id-floor` added to its `run` (eight lines, nothing else), in a second
worktree.

- **The translation.** a2g with the edge against main+floor (JVM reflection, ASM): the program
  trees (`bin/a2g --program`, and with `--input` the suite's Java fixtures) identical but for the
  two embedded sources the stage changes (`res/arbace/classes/analyze.ae.z`, `env.ae.z`); with
  S2's two sources on main+floor's side as well, identical, `report.edn` too (but the times).
  a2g in `--record-edge` mode and with the edge give the same tree; a2g-check's fixture world
  and a2g-evalproof's roots give the same tree with the edge as with `--jvm-reflection`.
- **The executables** (`--build` amd64, arm64; `--build-tests`; `--build --box` amd64, arm64),
  S2 against main+floor with S2's two sources, both linked with one version string (the -X
  version is the checkout's): byte-identical, sha256 `656fa77e…` (amd64), `574dc30c…` (arm64),
  `8b581a03…`/`1aa47d7f…` (tests), `5ef3ba3c…`/`1fc238c7…` (box amd64 launcher/guest),
  `e1384a59…`/`0b591ba5…` (box arm64). Two builds of the branch are identical (RP1): amd64
  `ed6c6de0…`, arm64 `8712ab5c…`.
- **The JVM's class forms compiler** is unchanged: stage 2's `arbace/lang` (774 classes),
  `arbace/asm` (45) and `arbace/java/api` byte-identical to main's; stage 2 = stage 3.
- **The trace** (`bin/a2g-edge trace`): 305,188 traced calls, 25,763 inside a2g's run: 8,466
  `TypeReference` objects, 17,162 `getInterfaces` of protocol dispatch, 15 by `Reflector`'s
  lookup, 120 by the JVM's linkage, stack traces and class constants; by a2g: **0**. With
  `--jvm-reflection` the same trace counts 13.3 million calls by a2g (4.4 million class loads by
  `env/load-class` alone).
- **The edge's drift is caught**: main's TLS merge made the embedded namespaces name
  `java.io.IOException` and `java.net.InetSocketAddress`, two more cut candidates;
  `bin/a2g-edge update` added them (`:exists`), and `verify` then passed.
- `bin/arbace-go --smoke`: 117 ok (amd64, arm64), none failed; `--smoke --box` (amd64): 111 ok;
  the oracle on amd64: 22,232 of 22,278, the 46 known mismatches, 0 new;
  `bin/jrt-convert verify` passed (`edge.edn` left to `bin/a2g-edge`); `bin/a2g-edge verify`
  passed (3 min 14 s).
- **Time** (`bin/a2g --program`, three runs each, alternated): main 93.1-95.8 s (analysis
  15.1-16.2 s); S2 78.6-89.2 s (reading the edge 0.2 s, analysis 11.4-12.5 s: no class loading
  nor reflection).

Left for S3: a2g, its namespaces and the analyzer loaded and run by the Go executable (the edge
read there as here; `Opcodes` and `TypeReference` are the Go build's already); the analyzer's
own reflective interop on its values (`arbace.classes.types/parse-method-desc`) works on Go's
`Reflector` but is slow; `bin/a2g-edge` itself and the edge's recording stay on the JVM (D4).
`jrt-convert`'s step `edge` (the members jrt hand-writes, `.tmp/jrt/edge.edn`) is another list,
of the translated JDK classes' references across the edge.

### Amendments

Proposed to the user; the recommended choice is applied on the branch `standalone-s2`.

The user accepted SA10-SA15 as recommended (2026-10-12). S2 numbered them SA1-SA6 on its
branch; renumbered at the merge, since S4's preparation had taken SA1-SA9.

- **SA10 Where S2's notes live.** (a) This file, `doc/STANDALONE-NOTES.md`, the standalone
  stages' notes from S2 on, with a paragraph in A2G-SPEC §4.1 pointing here; (b) a new section of
  A2G-SPEC.md (4,455 lines already); (c) A2G-NOTES.md. *Recommended: (a)*: S3 and S4 change the
  bootstrap, not only a2g.
- **SA11 The edge as a recorded file in `jdk/`.** (a) `jdk/edge.edn`, recorded from what a2g's
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
- **SA12 Whole classes, the JVM's order kept where it is stable.** (a) every member of each class
  asked about (403 classes, about 10,300 members, 1.40 MB), the methods of the JDK's
  CDS-archived classes in the JVM's order, the others sorted; (b) only the members used (the
  survey's estimate, 1,223): overload resolution and interface method sets need them all, and
  the program changed. *Recommended: (a)*. ASM's 15 classes in it go with ASM in S4.
- **SA13 The id floor.** (a) a2g advances the runtime's id counter to 100000 before it starts, so
  that its output depends on its inputs alone (needed by S3, whose G2 = G3 compares a2g on the
  Go executable with a2g on the JVM); every temporary of the program is renumbered once
  (`arr_10021` becomes `arr_1xxxxx`); (b) the floor at main's value (9022), which kept the
  program's names: impossible, S2's a2g itself loads 458 ids more (9480); (c) no floor: every
  change to a2g renumbers the program's temporaries, as today, and S3's comparison fails;
  (d) a2g's own counter for the analyzer's names: a change of the analyzer the JVM's compiler
  shares. *Recommended: (a)*.
- **SA14 The exit criterion as met.** The survey's "the same program byte for byte" cannot hold
  literally: the Go executable embeds the class forms' analysis (`arbace/classes/env.ae`,
  `analyze.ae`, for the REPL's class forms, CLASSFORMS-REPL.md), which S2 changes, and SA13
  renumbers. Proven instead (Results): a2g's program tree equals main's a2g's, run with the same
  id floor, but for the two embedded sources; with S2's two sources on both sides, the
  executables (`--build` amd64 and arm64, `--build-tests`, `--build --box` amd64 and arm64) are
  byte-identical; the class forms compiler's output on the JVM (stage 2's `arbace/lang`,
  `arbace/asm`, `arbace/java/api`) is unchanged. *Recommended: accept*; the alternatives
  (freezing a copy of main's analyzer as the Go build's variant, or the analyzer's changes
  undone by a2g at run time) would cost more than they prove.
- **SA15 `bin/a2g-edge verify` in `bin/gate --full`.** (a) in the go chain after `jrt-convert`
  (two a2g runs, about 3.5 minutes, sequential for memory); (b) only by hand, like `trace`.
  *Recommended: (a)*: a change that asks the JVM something new must update the edge, and only
  the JVM can tell.

## S3: g2a on Go

The part of S3 that is g2a's (branch `s3-g2a`, 2026-10-12): g2a's printer, `bin/g2a build`'s
driver (printing a program's packages into a module, then `go build` as a host process through
`ProcessBuilder`) and its converter run on the native Go executable, with the same output byte
for byte. a2g on Go is the other half of S3 (branch `s3-fixpoint`).

### The host: `G2A_HOST`

`bin/lib/g2a-host.bash` runs g2a's namespaces for `bin/g2a build`, `bin/g2a print`
(`bin/g2a-print`) and `bin/g2a convert` (so the round trip) on the host `G2A_HOST` names:

- `jvm`, the default: Arbace on the JVM, `target/stage2` plus the checkout, as before.
- `go`: the native executable, `G2A_GO_EXE` or else `target/arbace-go/<host arch>/arbace` (built by
  `bin/arbace-go --build`), run as `arbace -m NS ARGS` with the checkout first on `ARBACE_PATH`.
  `arbace.g2a` is not embedded in the executable (a2g's `embed.ae` leaves the tools out), so it
  loads from the checkout's sources, as on the JVM. The executable runs from a copy in
  `.tmp/g2a-host/`, refreshed (by a rename, under a lock) when it differs: `bin/arbace-go
  --build` replaces `target/arbace-go/<arch>/arbace` while its own printing runs.

So `G2A_HOST=go bin/arbace-go --build` builds the Go executable with the Go executable, and
`G2A_HOST=go bin/arbace-go --build --box` the box (`bin/tamago-vendor print`, the guest and the
launcher through `bin/g2a build`). The JVM stays the default until S3's fixpoint replaces it.

### Byte for byte

Proofs (main at `329af1a` merged; `.tmp/proof/`, removed after):

- `bin/arbace-go --build` and `--build --box` for amd64 and arm64, once with `G2A_HOST=jvm` and
  once with `G2A_HOST=go`: the printed modules (`work-amd64/mod`, `work-arm64/mod`: a2g's
  program with `x509roots/`; `work-box-<arch>/mod`, the guests with `box/`;
  `work-box-launcher-<arch>/mod`; the printed `tamago/` tree, `target/arbace-go/tamago`) equal by
  `diff -r`, and the eight executables identical: `amd64/arbace`, `arm64/arbace`, and for amd64
  and arm64 the guest `arbace.elf` and the launcher `arbace-box`.
- `tamago/`'s std and goos module, each architecture's forms root (`tamago/goroot/go` with
  `arch/<arch>/go` over it, as `bin/tamago-vendor` lays it): printed on Go, on the JVM, and on
  the JVM by main's printer (`329af1a`), all equal, for amd64 and for arm64.
- a2g's program printed by main's printer on the JVM equals the new printer's (both hosts);
  `box/` and `x509roots/` printed alone on both hosts and by main's printer, equal.
- `bin/tamago-vendor check`: both guests identical to TamaGo's originals' (linked and cut).
- `bin/g2a roundtrip amd64` and `linux/amd64`: no regressions; `G2A_HOST=go bin/g2a roundtrip
  amd64` (converter and printer on Go) too.

### Speed

Wall times on the shared 64-core host (other agents' builds running; differences under 10-20%
are noise). "Before" is main's printer (`329af1a`); on Go, main's executable of the time.
The program's line includes `go build` from a warm cache (a few seconds).

| tree | JVM before | Go before | JVM after | Go after |
|---|---:|---:|---:|---:|
| a2g's program, amd64 (`bin/g2a build`, 34 MB of forms, 1,070 files) | 23.8 s | 378 s (6.2 GB) | 12.1 s | 32.3 s (4.6 GB) |
| `tamago/` std, amd64 (print only, 371 packages) | 20.1 s | 235 s | 6.2 s | 21-24 s (3.7 GB) |
| `tamago/` std, arm64 | 18.1 s | | 6.6 s | 24.1 s |
| `tamago/` goos module | 2.5 s | 4.4 s | 2.2 s | 2.7 s |
| `box/` (print only) | 2.3 s | 4.4 s | 2.2 s | 2.4 s |
| `x509roots/` (print only) | 1.9 s | 3.5 s | 2.0 s | 2.4 s |
| `net/http` alone (`bin/g2a-print`, 29 files) | 2.7 s | 10.1 s | | 3.8 s |
| `bin/arbace-go --build`, amd64 and arm64 (a2g on the JVM in both) | | | 137 s | 153 s |
| `bin/arbace-go --build --box`, amd64 and arm64 | | | 85 s | 97 s |
| the round trip's conversion and printing (`bin/g2a roundtrip amd64`) | | | 36.7 s | 207 s (13 GB) |

Go after is about 3 times the JVM's wall time on the large trees (CPU: about 2 times the JVM's
on the program, 488 s against 237 s), against 16 times before; on small trees the
executable's start (no JVM to start, but the printer loaded from source, 1.6 s) evens them.
The converter was not tuned (left for S3's fixpoint, which needs only the printer and the driver).

### What changed

The survey's measure (§8.3, about 7 times slower) was of one package; the whole program was
16 times slower, for reasons in g2a, mostly sequential work the evaluator makes long:

1. **Reading once, in parallel.** `bin/g2a build` read every forms file twice (`package-file?`,
   then `collect`), one at a time: 42 s of the 97 s left on Go once printing was parallel (the
   reader is Go code, but slower than the JVM's: `LispReader` through four `Reader` layers and
   jrt's regex for each symbol). Now `arbace.g2a.build/read-all` reads the files once, in
   parallel (`pmap`), and `print/collect` takes a reading function: 7 s.
2. **Printing in parallel.** Packages were printed in parallel, but each package's files one
   after another, and a file whole: jrt's 910 files in a row, and `a2g_classes.ae` (4 MB, 8,249
   declarations) alone took 44 s on Go. Now `print-forms` prints a package's files in parallel,
   and `print-file` a long file's declarations in chunks (mode `:lines`, about two per
   processor): each chunk is printed in an emitter state of its own, started at the line its
   first declaration is placed on (`print-emit/start-at!`), then joined to the file's state in
   order (`join!`) when the file's printing reaches that line in the state the chunk assumed: at
   the start of a line, indentation 0, no pending blank or open bracket, the mark below the line
   and the chunk's own line never below it (`:min-line`, which `set-line!` keeps); at the start
   of a line the emitter reads nothing written before (the last token and the semicolon rule
   only between tokens of a line; the mark only as `(max (inc mark) line)`). Otherwise the chunk
   is printed again in the file's state: the text is the sequential printing's in every case.
   gofmt's alignment (`print-text/align`) aligns a long text's sections (form feeds end them)
   in parallel too.
3. **Binary chains in linear time.** An n-ary operator form `(+ a b c ...)` is printed as the
   left-nested chain go/printer sees; the printer made each prefix with `butlast` and walked it
   again for its cutoff (go/printer's `walkBinary`) at each level: cubic in the operands.
   x/text's normalization tables (a long concatenation) took 178 s on Go and 11 s on the JVM,
   the whole std's critical path. Now `walk-prefixes` walks all prefixes in one pass and
   `binary-expr` prints the chain in one loop, doing what the recursion did at each level (the
   repeated `break-to!` of f's line, each prefix's cutoff): 8.9 s and 2.6 s.
4. **Smaller things**: `go-printable?` made a set of 22 character types at each code point (now
   once); `escape`/`unescape` mapped every character of the file through a map (now
   `String.replace`); `sp` and `vtab` compared with `(char 11)` through a reflective
   `Util.equiv` (the only reflection warnings of the printer's hot path; on Go each call listed
   `Util`'s methods); `parse-lines` (alignment) compares chars as ints and shares the empty
   indentation cell; the emitter's accessors (`lines?`, `line`, `bol?`, `cur-indent`,
   `target`) are `definline` (measured: no gain, kept as harmless).
5. **The evaluator (runtime, `arbace/lang/go/Compiler.ae`)**: `Evaluator.invokeFn` of a call
   through a seq (variadic fns, `apply`, `RestFn`) compiled the method to `Code` at every call
   (`CodeRun/cmethod` unconditionally, with a binding of `*ns*`), where the calls of a fixed
   arity take the cached `ObjMethod.evalCM`: 9% of std's printing on Go. It now takes the cache
   too. The one runtime change; the oracle (`known-go-amd64.edn`: 22,232 of 22,278, the 46
   recorded mismatches, 0 new) and `--smoke` pass.

Profiles (Go's pprof, `ARBACE_CPUPROFILE`) attribute the rest to the evaluator itself, spread
thin (frames, `Code` dispatch, boxing, allocation; the collector 7-16%): no single hot spot
remains in g2a. Tried and not kept: `GOGC=400` for g2a's runs (13% less CPU time, the same wall
time, a third more memory: the executable's own setting, 200, is used); per-function
instrumentation (wrapped vars: its cost hid the costs measured).

### Left for S3

- a2g on the Go executable (S2's a2g, `s3-fixpoint`), then the chain G1 → G2 → G3 with
  `G2A_HOST=go`.
- g2a embedded in the executable and prepared in its image would save its load from source (1.6
  s per run); compiling the tools' namespaces to Go (B1-PLAN step 7's second half, AOT to Go
  forms) is what would close most of the remaining 2-3x: it needs a2g's embedding (`embed.ae`
  excludes `arbace/g2a/`).
- The converter on Go (5.6x the JVM's time, 13 GB) is untuned; the round trip is not part of
  the build.
- The reader on Go (`LispReader` through `LineNumberingPushbackReader`, jrt's regex per token)
  is the slowest part left of reading; a faster path would serve every tool.

### Amendments

Proposed to the user; the recommended choice is applied on the branch `s3-g2a`.

- **SA30 g2a's host chosen by `G2A_HOST`.** (a) `G2A_HOST=jvm|go` in `bin/lib/g2a-host.bash`
  for `bin/g2a build`, `print`, `convert` (and everything built on them: `bin/arbace-go`,
  `bin/jrt`, `bin/tamago-vendor`), the JVM the default until the fixpoint; the executable
  `G2A_GO_EXE`, else the host architecture's `target/arbace-go`, run from a copy; (b) a separate
  `bin/g2a-go`; (c) Go the default now. *Recommended: (a)*: one switch for every caller, which
  S3's chain sets; the default moves with the fixpoint (S3/S4).
- **SA31 The printer parallel within a package and a file.** (a) files of a package in
  parallel, a long file's declarations in chunks joined when the state matches, else printed
  again (exact by construction, proven on every tree above); (b) files only (the critical path
  then a2g's `a2g_classes.ae`, 44 s on Go); (c) splitting a2g's large files in a2g instead.
  *Recommended: (a)*; the JVM gains too (the program 23.8 → 12.1 s).
- **SA32 The evaluator's method cache on calls through a seq** (runtime, `Compiler.ae`): a fix,
  recorded as an amendment because it changes the Go build's runtime. *Recommended: accept.*
