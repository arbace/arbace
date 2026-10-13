# The standalone Arbace — notes

Working notes of the standalone stages ([STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), §9.1; the
user's decisions D1-D13 of 2026-10-12): what each stage did, how it is checked, and its
amendments (SA1-SA9 for S4's preparation, SA10... for S2, SA20... for S3's fixpoint, SA30... for S3's g2a), proposed to the user, the
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

## S4: the Go-source seed

Status: preparation, 2026-10-12, amendments SA1-SA9 accepted by the user the same day, branch `s4-seed` (beside S2 and S3, which it does not depend
on). The JVM bootstrap stays as it is; nothing here removes it. The tooling, a candidate seed
printed by today's JVM chain, and the proofs below.

### What the seed is (D3)

Stage 0 of the bootstrap becomes **Arbace's Go program as a2g and g2a print it**: the module
`arbace` that `bin/arbace-go --build` hands to `go build` (`target/arbace-go/work-<arch>/mod`),
with the Go runtime's files that `overlay/go` patches, and nothing else Arbace-made. Built by
Go's toolchain alone, it gives the executable `bin/arbace-go --build` gives, byte for byte. That
executable then runs the chain (S3: a2g, g2a, `go build`) that builds stage 1 from the
checkout's sources, and so on to the fixpoint.

Contents of the archive, under one top directory `arbace-seed-go/`:

| path | what | size |
|---|---|---:|
| `SEED.edn` | what the seed is: its format, the version map of the commit it was printed from (linked as `main.arbaceVersion`), Go's version, the build's settings, what printed it (read with `read`) | 1 KB |
| `mod/go.mod` | `module arbace`, `go 1.27`; no requirements (no `go.sum`, no module cache) | |
| `mod/cmd/arbace/` | the main package: `main.go`, the image's types and loader, `rlwrap_*.go`, the embedded namespaces' sources `res/` (47 namespaces, a2g's compressed `.ae.z`, SZ2); `image.bin` empty (SA3) | 0.6 MB |
| `mod/lang/` | a2g's translation of Clojure's runtime (`arbace.lang`) | |
| `mod/jrt/` | jrt's hand-written Go and the JDK classes a2g translated from `jdk/forms`, with their data files (`tzdb.dat`, `uniName.dat`, ICU's `nfc.nrm`, ...) | |
| `mod/x509roots/` | Mozilla's roots (`x509roots/`, BX72) | |
| `overlay/runtime/` | `arbace_local.go`, `proc.go`, `runtime2.go`: Go's runtime as `overlay/go` patches it (`bin/jrt overlay`) | 0.4 MB |
| `namespaces.txt` | the embedded namespaces the image prepares (a2g's `prog/namespaces.txt`) | |

1,148 files, 29.2 MB unpacked (1,070 Go files, 28.4 MB). It is the same for every architecture:
the work modules of amd64 and arm64 are identical (checked, `diff -r`), so one seed builds both.
It holds no tests, no box (the box's guest is built by a later stage from `box/`, `tamago/` and
the same a2g output), and no host tools (`tools/elfsyms`, `tools/tlsserver` are plain Go in the
checkout).

### Format and pin (SA1, SA2)

`seed/arbace-seed-go.tar.xz` beside `seed/arbace-seed-go.tar.xz.sha256` (sha256sum's format, as
the jar's). The tar is deterministic: POSIX format, members sorted by name, mtime 0, owner and
group 0 (numeric), modes 0644 and 0755, no atime or ctime (GNU tar 1.35: `--sort=name
--format=posix --pax-option=exthdr.name=%d/PaxHeaders/%f,delete=atime,delete=ctime --mtime=@0
--owner=0 --group=0 --numeric-owner`); compressed with `xz -9e -T1` (one thread, so one block:
the output depends only on xz's version, 5.8.4 here). The pin is the archive's hash, what
`--go-check` checks in a second; `--go-verify` compares the *tar* byte for byte (then reports
whether the archive is identical too), so a different xz release does not make a valid seed look
wrong.

Sizes of today's candidate (main `b0f5730` plus the tooling, `7498921`):

| form | bytes |
|---|---:|
| the tree | 29,155,346 (tar 30,074,880) |
| tar.xz, `-9e` (chosen) | 3,063,796 |
| tar.xz, `-9` | 3,143,856 |
| tar.zst, `-19` | 3,235,094 |
| tar.gz, `-9` | 4,461,467 |
| `image.bin`, if carried (SA3), xz alone | +1,320,412 (6,308,340 raw) |

The survey's 35 MB and 4.4 MB counted the work module with its image.

### Stage 0 from the seed (SA3, SA4, SA5)

`bin/seed --go-build [-o DIR] [--arch amd64,arm64]` checks the hash, unpacks into `DIR/src`
(default `target/seed-go`), writes the overlay file for the toolchain's GOROOT, and builds as
`bin/arbace-go --build` builds:

1. `go build -trimpath -buildvcs=false -overlay overlay.json arbace/cmd/arbace` for the host,
   with the empty image (the environment of `bin/arbace-go`'s `go_env`: `GOTOOLCHAIN=local`,
   `CGO_ENABLED=0`, `GOOS=linux`, `GOAMD64=v1` / `GOARM64=v8.0`, `GOPROXY=off`, no `GOFLAGS`,
   `GOWORK`, `GOEXPERIMENT`);
2. that executable prepares the image (`ARBACE_PREPARE`, requiring `namespaces.txt`'s
   namespaces), as `bin/arbace-go`'s `prepare` does;
3. each architecture linked again with the image and `-ldflags "-s -w -X
   'main.arbaceVersion=<SEED.edn's :version>'"`, into `DIR/<arch>/arbace`.

About 20 s with Go's build cache warm; minutes cold (gc needs 7-8 GB for `jrt`, survey §8.2).

- **SA3 The image is not in the seed.** The image of prepared namespaces (6.3 MB, 1.3 MB as xz)
  is derived data, a function of the seed's own sources and its executable; carrying it would
  put 6.3 MB of opaque bytes in a seed meant to be readable source. Stage 0 prepares it, as every
  `--build` does; the result is the same image, byte for byte (checked). Alternative: carry it
  (one `go build` fewer, about 11 s; the archive 4.4 MB).
- **SA4 The toolchain**, pinned by its version and its sources. The toolchain is TamaGo's Go
  distribution (`/root/tamago-go`, https://github.com/usbarmory/tamago-go, go1.27.1,
  2026-08-28), the one every Arbace build uses (`G2A_GOROOT`). `seed/goroot.sha256` pins it as
  `<SHA-256>  go1.27.1`, and `SEED.edn` records the same as `:go` and `:goroot-sha256`. The hash
  of GOROOT's sources is defined as: sha256sum's lines (`<SHA-256>  <path relative to GOROOT>`)
  of `VERSION` and every file under `src/`, sorted by path in the C locale's byte order, hashed
  again; `src/` must hold only regular files and directories (a symbolic link or other file is
  refused); `pkg/`, `bin/`, `lib/`, `misc/`, `test/`, caches and the rest of GOROOT do not count
  (they are built or not compiled into Arbace). TamaGo's go1.27.1: 12,175 files,
  `5745170fd3c83cb77ec88bc50aee8606f0436e718c222b48376b5943ac0cebe5`. `--go-make` refuses to
  pack with another toolchain; `--go-build` and `--go-verify` refuse a GOROOT whose version or
  hash differs from the pin or from `SEED.edn` (a seed without `:goroot-sha256`, as the first
  candidate, is refused). The check takes 0.65 s with the files in the page cache, 3.6 s cold.
  The toolchain's binaries are not hashed: they are built from these sources, and Go's builds are
  reproducible.
- **SA5 Stage 0's version** is the version map of the commit the seed was printed from
  (`SEED.edn`'s `:version`, `git describe` there), so stage 0 *is* that commit's Arbace: its
  banner and `*arbace-version*` name that commit, not the checkout building it. A seed made from
  a dirty checkout records `:dirty true` (and `--go-make` warns); a seed to commit is made from a
  clean one.

### `bin/seed`'s modes (SA6)

Beside the jar's `--check` and `--verify`, unchanged (the gate still runs `--check`):

| mode | what | time |
|---|---|---:|
| `--go-check` | the archive (`ARBACE_SEED_GO`, default `seed/arbace-seed-go.tar.xz`) has its pinned hash | <1 s |
| `--go-make [ARCHIVE]` | pack this checkout's last `bin/arbace-go --build` (the host's work module, the patched runtime, `namespaces.txt`, the version map) and pin its hash in `ARCHIVE.sha256` | 17 s (xz) |
| `--go-build [-o DIR] [--arch A,B]` | stage 0, as above | 20 s warm |
| `--go-verify [REV]` | in a temporary worktree at REV (default `SEED.edn`'s `:commit`): the commit's own build of its Go program (today `bin/build-arbace`, `bin/arbace-go --build`; from S3 the Go-hosted chain started from the previous seed), its `--go-make`; then compares the tar byte for byte and the archive, and builds stage 0 from the printed seed and compares it with the commit's own `--build` executable | ~10 min |

The code is `bin/lib/seed-go.bash` (sourced by `bin/seed`). At S4's end (SA6): the jar and its
modes leave main, the `--go-*` modes become `--check`, `--build`, `--verify` (and `--make`),
`bin/gate`'s first step checks the Go seed, and `bin/build-arbace` gives way to S3's chain started
from `bin/seed --build`.

### When the seed is committed (SA7)

The seed to commit is the **fixpoint's**: printed by the Go-hosted chain (S3, G2 = G3), at the
commit that is tagged `arbace-seed-is-go-v1`. Today's candidate is printed by the JVM chain; if
S3 holds its exit criterion (the Go-hosted chain prints what the JVM chain prints), the two are
the same tree, but that is S3's proof, not this one's. So the candidate stays in `.tmp/` and is
not committed: 3.1 MB per seed in git's history is cheap, but a seed that is replaced within days
is churn. Recommended: commit the seed once, at S4's end, with `--go-verify` run on it; re-seed
rarely (as the jar was pinned once, at a freeze), for instance when the toolchain moves.
Alternative (rejected): the tree committed as files (29 MB, readable and diffable on GitHub, but
every re-seed a diff of up to 29 MB, and 1,070 Go files of a module nobody edits inside the
repository; `jdk/` and `tamago/` are committed as forms because they are *sources*, which the
printed Go is not: its sources are the forms).

### Proofs (2026-10-12)

On the worktree `s4-seed` at `7498921` (main `b0f5730` plus the tooling), a clean checkout,
`bin/build-arbace` then `ARBACE_GO_ARCHES=amd64,arm64 bin/arbace-go --build`:

- **The module is architecture-neutral**: `work-amd64/mod` and `work-arm64/mod` identical
  (`diff -rq`).
- **Generation A**: `bin/seed --go-make` → 3,063,796 bytes, SHA-256
  `c4e00bf6c127568784aa307d41ddb2a565680f4fec5e1a02e474a6b8869b99a3`.
- **Stage 0 = `--build`, both architectures**: `bin/seed --go-build --arch amd64,arm64` from A
  (in a fresh directory) gives `amd64/arbace` (69,169,312 bytes, SHA-256
  `0793e34f…5c036e8a`) and `arm64/arbace` (66,781,344 bytes, `9e7a7cee…0fc837d`), each
  byte-identical to `bin/arbace-go --build`'s; the image it prepares is identical to the
  build's. `*arbace-version*`: `{:describe "arbace-box-arm64-v1-28-g7498921", :commit
  "7498921…", :dirty false}`.
- **Generation B**: `bin/seed --go-verify` (a fresh worktree at `7498921`: its own bootstrap,
  a2g, g2a, gc, `--go-make`; 4m50s): the same tar byte for byte (SHA-256
  `cc61aabbf4fcf5e050d58edb7f1ee735b2ff91eb0d7464dce720056ff294aca0`) and the same archive
  (`c4e00bf6…`); stage 0 built from B (amd64) is that worktree's own `--build` executable, byte
  for byte (`0793e34f…`). The seed is reproducible from its commit.
- An earlier generation from the same tree with a dirty checkout differed only in `SEED.edn`'s
  version map, and its stage 0 equalled that build's executables too (the version linked is the
  only input outside the tree).
- **Again with SA4's toolchain pin**, after main was merged (`3d3a13b`, clean; a fresh
  `ARBACE_GO_ARCHES=amd64,arm64 bin/arbace-go --build`): the candidate 3,063,232 bytes, SHA-256
  `311420be5f7fb60d195550d033f17b60b593dc94738caba8b389fd88b4aeb2a2` (its `SEED.edn` with
  `:goroot-sha256`), `--go-check` as pinned; stage 0 from it identical to the build's
  executables, amd64 `27b438c5…fe892ad`, arm64 `bfb2136b…c3f59a7`, built in 18 s; the oracle with
  main's reference on that executable: the 46 known mismatches, as recorded (with `files.edn`
  re-recorded). The first candidate, without `:goroot-sha256`, is now refused by `--go-build`;
  a GOROOT with one file of `src/runtime` changed is refused (checked).
- **After S2 and the test fixtures merged** (`26885c6`, clean): the essential `bin/gate` passed
  (6m06s); a fresh `--build` (amd64, arm64), the candidate 3,063,800 bytes (SHA-256
  `d70fd40604cafebba23fe93491e055f6a66cda75519c7d528398b2dfc72e0e58`), `--go-check` as pinned,
  stage 0 from it identical to the build's executables (below),
  and the oracle with main's reference: the 46 known, as recorded. Stage 0: amd64
  `c4fe6b02cac31d127ce0de1311130f306b6c218130a963a1443f01d6080e90cd`, arm64
  `dddeb311ca319dd17cb1e328b7e32b81900722a9cbd48ad8908c681c947824ad`.

### The amendments

Accepted by the user 2026-10-12: SA1-SA3 and SA5-SA9 as proposed, SA4 with the hash of GOROOT's
sources (as above, rather than the version alone).

- **SA1 The seed's form**: one deterministic `tar.xz` committed as `seed/arbace-seed-go.tar.xz`
  with `.sha256` beside it (3.1 MB), not the printed tree committed as files (29 MB) nor a gzip
  (4.5 MB).
- **SA2 Its determinism and pin**: the tar's settings above, `xz -9e -T1`; the pin is the
  archive's SHA-256; `--go-verify` compares the tar, so only the tree must reproduce.
- **SA3** The image of prepared namespaces not carried; stage 0 prepares it.
- **SA4** The toolchain pinned by version (TamaGo's go1.27.1) and by the SHA-256 of GOROOT's
  `VERSION` and `src/` (`seed/goroot.sha256`, `SEED.edn`'s `:goroot-sha256`), checked by
  `--go-make`, `--go-build` and `--go-verify`.
- **SA5** Stage 0 carries the version of the commit the seed was printed from.
- **SA6** `bin/seed`'s `--go-check`, `--go-make`, `--go-build`, `--go-verify` now, beside the
  jar's modes; renamed `--check`, `--make`, `--build`, `--verify` when the jar leaves (S4's end).
- **SA7** The seed committed once, at S4's end, printed by the Go-hosted fixpoint and verified;
  today's JVM-printed candidate stays in `.tmp/`.
- **SA8** The oracle's reference is the jar of `arbace-for-java-26-v4`, built from its tag and
  pinned by hash (`test/oracle/frozen-jar.sha256`), not `seed/arbace-seed.jar` (v1).
- **SA9** At S4 the expected files are recorded again from v4, and the Go build's known
  mismatches against them are `test/oracle/known-go-amd64-frozen.edn` (the 46 of
  `known-go-amd64.edn` plus main's two divergences from v4); `known-go-amd64.edn` stays the
  gate's until then, unchanged.

## S4: the oracle's reference (D4)

`bin/oracle` ran its runner, and `record` and the `jvm` implementation, on `bin/arbace-j`, that
is `target/arbace.jar` from the bootstrap, which leaves main at S4. It now takes
`ARBACE_ORACLE_REF`:

- `main` (the default until S4): as before.
- `frozen`: the frozen JVM Arbace, run by `java` alone (no `bin/build-arbace`, no
  `bin/arbace-j`): the runner (`java -cp JAR:.tmp/oracle-view:test arbace.lang.Main -m
  oracle.runner`; the frozen jar's loader reads `.clj` only, so `oracle/runner.clj` and
  `harvest.clj` are links to the `.ae` files), and the reference implementation for `record` and
  `check jvm` (`java -XX:+UseCompactObjectHeaders -XX:-OmitStackTraceInFastThrow -cp JAR
  arbace.lang.Main -`, through the runner's new `ARBACE_ORACLE_JVM`).
- `ARBACE_ORACLE_EXPECTED` names the expected files' directory (default
  `test/oracle/expected`), so a record from another reference can go elsewhere.

**SA8 The reference is the frozen branch's last release, v4, not the seed jar (v1).** The seed
jar is `arbace-for-java-26-v1`, which still compares NaN wrongly (`(< ##NaN 1)` true, `(long
##NaN)` throwing: the class forms compiler's `dcmpg`, fixed in v2, `398f01b`); recorded from it,
`forms/numbers` alone differs from the expected files in 8 cases. `bin/oracle` builds the jar of
`ARBACE_ORACLE_TAG` (default `arbace-for-java-26-v4`, `c4095d1`) once from the tag, with the
tag's own `bin/build-arbace` in a temporary worktree (about 2 minutes), into
`.tmp/oracle-frozen/arbace-for-java-26-v4.jar`, and checks it against
`test/oracle/frozen-jar.sha256` (pinned:
`4b6f031990ba61edc73030d17b827ad2e2fca7aa2c53c3ed3e178ae1c89d640a`; two builds identical).
`ARBACE_ORACLE_JAR` takes another jar unchecked (`seed/arbace-seed.jar` for v1). Alternatives:
commit the v4 jar (7.5 MB) as the reference, which spares the tag's build but is one more binary
in git; or keep `seed/arbace-seed.jar` and accept v1's bugs as recorded divergences (rejected:
they are bugs, not divergences).

### The expected files recorded from v4

`ARBACE_ORACLE_REF=frozen ARBACE_ORACLE_EXPECTED=.tmp/oracle-exp-frozen bin/oracle record`: every
file recorded (22,118 cases), 5 of 84 files differ from the committed expected files, 6 cases:

| case | committed (main's JVM) | v4 | what |
|---|---|---|---|
| `forms/state_errors.ae:542` | "Could not locate ...`__init.class`, `.ae`, `.clj` or `.cljc`" | without `.ae` | **divergence**: main's loaders take `.ae` (2026-10-11), the frozen jar does not |
| `forms/polymorphism.ae:176` | "... `java.io.Serializable` and `java.lang.Comparable` ..." | the two interfaces in the other order | **divergence**: the order of the ambiguous methods in the message (main's multimethod, or the JDK's interface order through the jar's build) |
| `forms/files.ae:123, 349-353` | the forms' text with `.ae` | with `.clj` | **stale expected file**: the `.ae` rename (`c76b468`) edited `expected/forms/files.edn` but not `forms/files.ae`; main's JVM records the same as v4 (checked). Not a divergence: re-recording on main fixes it |
| `forms/strings.ae:408` | `(hash (StringBuilder. "x"))` 1318227903 | 1293885429 | **noise**: an identity hash, different on every run (main's own record differs too) |
| `forms/concurrency.ae:201` | `CyclicBarrier`'s arrival indexes `[2 (0 1)]` | `[0 (1 2)]` | **noise**: thread timing |

### The native oracle with the v4 reference

The native executable (`target/arbace-go/amd64/arbace` at `7498921`) checked with the runner
and the expected files from v4 (`ARBACE_ORACLE_REF=frozen
ARBACE_ORACLE_EXPECTED=.tmp/oracle-exp-frozen bin/oracle check ... --expected
test/oracle/known-go-amd64.edn`): 22,229 of 22,278 cases, 49 mismatches: the 46 known and 3
new. The same executable with main's reference: 22,232, the 46 known, as recorded.

- `forms/concurrency.ae:201`: a noisy record (v4 recorded `[0 (1 2)]` once; three records
  again gave `[2 (0 1)]`, as the committed file and the Go build). Not a divergence; the expected
  file kept the stable value.
- `forms/polymorphism.ae:176`: main's MultiFn names two ambiguous classes in the order of their
  names (`ac8b971`, 2026-10-09, after the freeze); v4 in the JDK's order. A divergence of main.
- `forms/state_errors.ae:542`: main's loaders take `.ae` first (2026-10-11), so the not-found
  message names `.ae`. A divergence of main (D13).

The existing `test/oracle/known-go-amd64.edn` is unchanged. The two divergences go into a
separate reference, `test/oracle/known-go-amd64-frozen.edn` (48 cases: the 46 plus a group for
the two, with their reasons), for the check against v4's expected files (SA9); with it the check
passes (22,230 of 22,278 match, the 48 as recorded, the frozen jar matching its pin). The expected files from v4 are not committed (they are 5 of 84 files
different, of which `files.edn`'s was a stale file: re-recorded from main's JVM reference on
this branch, the diff only its six `.ae`-rename cases); recording them into
`test/oracle/expected` is S4's step, with `known-go-amd64-frozen.edn` becoming the gate's.

## S4: inventory for the JVM's removal

What S4 removes from main or moves off the JVM, with what replaces each (D4: the JVM stays only
as the oracle's reference, the frozen jar, and as j2a's front end). (Done at S4's end, the boxes
below left as planned: "S4: the JVM removed" records what was done and what is left.)
Tags: **[P]** the JVM product, leaves main (kept on `arbace-for-java-26`); **[T]** a tool that
runs on the JVM today and is hosted by the Go executable after S3; **[R]** stays on a JVM (the
oracle's reference or j2a). Counts at `b0f5730`.

### Scripts

- [ ] [P] `bin/build-arbace` (229 lines: the seed view, stages 1-3, AOT, `arbace.classes.verify`,
  `bin/native-tests`, the jar, the AOT cache, `--suite`, `--image`) → S3's chain from `bin/seed
  --build` (stage 0 from the Go seed, then G1 → G2 → G3).
- [ ] [P] `bin/arbace-j` (105) → the Go executable (`bin/arbace-go`); the oracle runs the frozen
  jar with `java` (done: `ARBACE_ORACLE_REF=frozen`).
- [ ] [P] `bin/arbace-image` (70, jlink and its AOT cache) → removed.
- [ ] [P] `bin/seed`'s jar modes → its `--go-*` modes renamed (SA6).
- [ ] [P] `bin/native-tests` (24, `test/native` on stage 2) → removed; its semantic tests
  (`defclass`, `deftype`, `fn`, `overload`, `nan`, `bridge`, `reduce`, `imports`,
  `nested_deftype`) into the oracle's corpus first.
- [ ] [P] `bin/class-forms-tests` (24, `test/classes` against javac's bytecode) → removed, or
  reduced to the front end's tests on Go.
- [ ] [R/P] `bin/class-forms-check` (13: converted forms compiled to bytecode and compared with
  javac's) → used by `bin/j2a-check` (steps 2-3) and `bin/jrt-convert check`: see open point O2.
- [ ] [P] `bin/java-surface` (32, ASM over stage 2) → retired; its output
  `doc/go/java-surface.edn` stays as data.
- [ ] [T] `bin/lib/tools.bash`'s `arbace_runtime` (`target/stage2`) → the Go executable
  (`ARBACE_PATH` to the checkout); `frozen_clojure` stays (git only).
- [ ] [T] `bin/a2g`, `bin/a2g-check`, `bin/a2g-race`, `bin/a2g-evalproof`; `bin/g2a` (read,
  convert, build's driver), `bin/g2a-print`, `-print-tests`, `-convert-tests`, `-build-tests`;
  `bin/c2a`, `bin/c2a-check`; `bin/box-vendor`, `bin/tamago-vendor`, `bin/x509roots-vendor` (g2a);
  `bin/jrt` build/test/print (g2a) → on the Go executable (S3).
- [ ] [T/R] `bin/clojure-tests` (369): c2a's translation and the listing/reports on stage 2 → on
  Go; its stage modes [P] removed; `--java-fixtures` (j2a) [R]: done, the fixtures are committed
  as forms in `test/java-fixtures/` (merged 2026-10-12), `--java-fixtures verify` on the JVM.
- [ ] [R] `bin/oracle` → the frozen jar (done, SA8); `ARBACE_ORACLE_REF=frozen` becomes the
  default and `main` goes.
- [ ] [R] `bin/jrt testdata`, `manifest`, `standins`; `bin/jrt-coverage`; `bin/net-check`'s JVM
  side → on the frozen jar (data generators: their outputs are committed or regenerated rarely).
- [ ] [R] `bin/j2a`, `bin/j2a-check`, `bin/jrt-convert` → stay on the JVM (javac), see O1, O2.
- [ ] `bin/arbace-go`: `build` (a2g, g2a on the JVM) [T] → S3's chain; `jvm_store_checks` and the
  JVM's `tls_checks` in `--smoke` [P] → removed; `gate_key` hashes `seed/arbace-seed.jar.sha256`,
  `bin/arbace-j`, `bin/build-arbace`, `java -version` → the Go seed's pin, the chain's scripts, Go
  only (plus `java` while a2g runs on it).

### Code

- [ ] [P] ASM: `arbace/asm.ae` and `arbace/asm/` (45 files, 14,747 lines) → removed, after
  `Opcodes`' access-flag constants (used by `classes/{parse,analyze,compiler}`,
  `lang/Intrinsics`, `a2g/{checks,decls,dyn,fromfn,model,out,reach}`) move into a constants
  namespace of Arbace's own (O3).
- [ ] [P] `arbace/lang/Compiler.ae`'s bytecode back end (8,717 lines; 105 `emit*` methods,
  `ObjExpr.compile`) → the analyzer stays; the emitters removed (the Go variant
  `lang/go/Compiler.ae` already erases them).
- [ ] [P] `arbace/lang/Compile.ae` (72, AOT's main) → removed.
- [ ] [P] `arbace/lang/DynamicClassLoader.ae` (78) and its uses (`RT.ae`, `main.ae`,
  `core/server.ae`, `repl/deps.ae`) → the Go variants (`lang/go/RT.ae`, `ns/main.subst.ae`,
  `ns/core/server.subst.ae`) become the sources.
- [ ] [P] `arbace/genclass.ae` (760, gen-class, gen-interface by ASM) → removed (D13); the Go
  variant `lang/go/ns/genclass.ae` (gen-interface at run time) becomes the one.
- [ ] [P] `arbace/core_proxy.ae` (403) → `lang/go/ns/core_proxy.ae`.
- [ ] [P] `arbace/core.ae`: `descriptor`, `add-annotation` (ASM `Type`), `ns`'s `:gen-class`,
  `compile`/`*compile-files*`, `(load "genclass")`; `core/specs/alpha.ae`'s `::ns-gen-class` →
  removed or no-ops, each a recorded divergence (D13).
- [ ] [P] `arbace/reflect/java.ae` (267, ClassReader) → `lang/go/ns/reflect/java.subst.ae`.
- [ ] [P] `arbace/core_classes.ae`'s `write-module!`, `write-package!` → removed.
- [ ] [P] the class forms compiler's back end: `classes/emit.ae` (2,187; first move a2g's
  `ctor-real-desc`, `this0-name`, used in 6 a2g files, O4), `compiler.ae` (169), `shape.ae` (161),
  `verify.ae` (102), `build.ae` (74), `boot.ae` (31) → removed; the front end (`parse`, `lower`,
  `analyze`, `env`, `types`, `native`) stays.
- [x] [T] ASM and reflection in a2g's run: `env.ae`'s class path reflection and ClassReader,
  a2g's `access.ae`, the accessors → S2 done (merged 2026-10-12): the edge as data
  (`jdk/edge.edn`, `bin/a2g-edge`, its `verify` on the JVM), accessors as data, 0 reflective calls
  by a2g. Left: the analyzer's accessor bytecode is still emitted for the JVM back end (goes with
  it), and `Opcodes`/`TypeReference` stay as the Go build's classes (O3).
- [ ] [P] the JVM namespaces a2g already leaves out (`repl/deps`, `java/basis`, `tools/deps`,
  `java/browse*`, `java/javadoc`, `inspector`, `xml`, `instant_timestamp`, `java/api`) → removed
  or ported, per D13.
- [ ] [P] `arbace/box/host.ae` (794, the store on the JVM, BX37), `arbace/tls/host.ae` (98,
  JSSE) → removed (the Go variants under `lang/go/ns/` stay).
- [ ] [R] `arbace/j2a/` → stays; `j2a/build.ae` (bytecode, j2a-check's step 4) see O2.

### Tests and data

- [ ] [P] `test/aot-training.ae`; `test/arbace-results.edn` (the suite on stages 1, 2) → removed;
  `test/arbace-go-results.edn` becomes the gate's.
- [ ] [R] `test/baseline-results.edn` → stays while `j2a-check --suite` runs on the JVM.
- [ ] [P] `test/native/` (19 files) → removed after the semantic ones move to the oracle.
- [ ] [P] `test/classes/` (17 files and fixtures) → removed, except `check_converted.ae` (O2).
- [ ] [P] `test/classforms/jvm_check.ae` → removed; `gen_corpus.ae`, `class_scripts.ae` [R] on
  the frozen jar.
- [ ] [R] `test/oracle/` (runner, harvest, expected) → on the frozen jar (done); the expected
  files re-recorded from v4 at S4.
- [ ] [R] `test/jrt/testdata*.ae`, `tables.ae`, `manifest.ae`, `standins*.ae`,
  `jrt_coverage.ae`, `jrt_sources.ae` → on the frozen jar; `java_surface.ae` [P] retired.
- [ ] [T] `test/a2g/eval_ops.ae` (generates `lang/go/CompilerOps.ae` on stage 2),
  `test/a2g/check.ae` → on Go.
- [ ] `test/c2a/c2a_test.ae` names `arbace.asm.commons.Method` as data → check after ASM goes.

### `bin/gate`

- [ ] `bin/seed --check` → the Go seed's hash.
- [ ] `bin/build-arbace` [P] → `bin/seed --build` and S3's chain (the fixpoint, printed Go
  compared).
- [ ] `suite-stage2` [P] → Clojure's suite on the Go test build (`bin/arbace-go --build-tests`,
  `CLOJURE_TESTS_GO`, `test/arbace-go-results.edn`): the gate's suite (survey S4's exit).
- [ ] `cft` (`bin/class-forms-tests`) [P] → removed or the front end's tests on Go.
- [ ] `go` (`bin/arbace-go --gate`) → keyed without the JVM; no JVM checks in the smoke test.
- [ ] `--full`: `suite-stage1` [P] removed; `j2a` (`j2a-check --suite`) and `jrt-convert verify`
  [R] stay on the JVM (O1, O2); `oracle-go` → the frozen reference; g2a, box, go-build,
  go-build-tests, suite-go, box-forms, box-smoke [T] → on the Go-hosted tools; the converters
  no longer wait for `suite-stage1`.

### Documents

- [ ] CLAUDE.md (Layout: the seed, `bin/build-arbace`, the stages, the jar, the AOT cache,
  `bin/arbace-j`, `bin/arbace-image`; the gate; "the tools run on `target/stage2`"), README.md
  (building and running), LICENSE.md (ASM's entries and section, the seed jar's entry: kept while
  any ASM-derived file or the frozen jar is in the tree).
- [ ] doc/VENDOR-NOTES.md (the bootstrap, the jar and AOT cache, the runtime image, the suite on
  stages 1 and 2, the binary seed: marked as the frozen branch's), doc/classes/{SPEC (§6, §8.3,
  §9.3, §9.4, §9.6), COMPILER-NOTES, CONVERTER-NOTES}.md, doc/ARBACE.md, doc/C2A.md,
  doc/go/{A2G-SPEC, EVAL-NOTES, CLASSFORMS-REPL, JAVA-SURFACE, BOX-NOTES (BX37, TLS on the JVM),
  EVAL-PLAN, EXEC-NOTES, ORACLE, A2G-NOTES, B1-PLAN, JRT-NOTES, JRT-SOURCES, SPEED-NOTES, BUILD,
  ...}.md; doc/AGENDA.md; a JOURNAL entry.

### Open points for S4

- **O1 Which JVM Arbace hosts j2a after S4.** j2a's Arbace side runs on `target/stage2` today.
  Options: (a) the frozen v4 jar with `arbace.j2a` loaded from the checkout's source (it reads
  `.clj` only: a view as the oracle's); (b) j2a's Arbace side on the Go executable, javac alone in
  a JVM (a process boundary inside j2a). Recommended: (a) at S4, (b) with D12's later stage.
- **O2 j2a's checks compile to bytecode.** `j2a-check` (steps 2-4) and `jrt-convert check`
  compile converted forms with the class forms back end (emit, ASM's ClassReader) and compare
  with javac. Options: (a) run them on the frozen jar's own compiler (at the freeze's state);
  (b) make them forms-level only (j2a's output compared with the committed forms, as `jrt-convert
  verify` does); (c) keep emit, shape and ASM on main as a j2a-check toolkit. Recommended: (b),
  with (a) for the suite step.
- **O3** `Opcodes`' constants need a home before ASM goes. **O4** a2g's two helpers from emit
  move first.
- **O5** Done: the Go suite's Java fixtures are committed as forms (`test/java-fixtures/`), so the
  test build needs no javac or j2a; `bin/clojure-tests --java-fixtures verify` (j2a on the JVM)
  is a `--full` check, as `bin/a2g-edge verify` is.

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

The user accepted SA30-SA32 as recommended (2026-10-12, after the merge into main).

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

## S3: the Go-hosted fixpoint

The other half of S3 (branch `s3-fixpoint`, 2026-10-12; g2a's half is "S3: g2a on Go" above):
a2g (`arbace/a2g`, with the class forms' analysis `arbace.classes.*`) run by the native Go
executable, so that the whole build of the Go program runs on Arbace's Go build: a2g, g2a's
printing and `bin/g2a build`'s driver on the executable, `go build` as a host process. The exit
criterion (survey §9.1): G1, built by today's JVM chain, builds G2; G2 builds G3; G2 = G3, and
a2g's program tree on Go equal to the JVM's.

### What kept a2g off the Go executable

S2 had removed a2g's questions to the JVM (the edge); what remained were names and classes the
Go build does not have, found by loading a2g's namespaces on the executable:

1. **The bytecode back end.** a2g required `arbace.classes.emit` (ASM's visitors, proxies of
   `arbace.asm` classes the Go world does not hold) for two functions of javac's rules:
   `ctor-real-desc` (a constructor's descriptor with its implicit parameters: the outer
   instance, the enum's name and ordinal, captured locals) and `this0-name` (the outer instance
   field, `this$N`). They are analysis, not emission: they move to `arbace.classes.analyze`
   (with `outer-level`), `emit` and a2g call them there, and a2g no longer requires `emit`
   (SA22). `analyze.ae` is embedded in the executable (the REPL's class forms), so its source
   and the image change; the JVM's class forms compiler compiles the same code.
2. **The JVM's module layer.** `repl-visible?` named `java.lang.Module` and `ModuleLayer` in a
   fallback only `--record-edge` and `--jvm-reflection` call; the Go build has neither class, and
   a hint or a class name the compiler cannot resolve fails the namespace's load. The question
   moves to `arbace.a2g.jvm`, loaded on demand (`requiring-resolve`) by that fallback (SA23).
3. **`java.util.zip.Deflater`.** a2g compresses the embedded resources (SZ2) with the JDK's
   Deflater, zlib's C code: the Go build has no Deflater (jrt inflates with Go's
   `compress/zlib`), and Go's compressor writes other bytes than zlib's, so a2g on Go could not
   write the JVM's tree. a2g now writes them with `arbace.a2g.zlib`, a DEFLATE compressor in
   Arbace (LZ77 over a 32 KB window with hash chains searched 64 deep and one step of lazy
   matching, dynamic Huffman blocks of 16,384 symbols, lengths limited by halving the
   frequencies, ties by symbol): the same bytes on every host, read by jrt's inflater and the
   JDK's. Its output is 0.5-0.7% larger than zlib's level 9 (the resources 666,535 bytes against
   662,014; the executable +8 KB); 35 ms for `core.ae` (286 KB) on the JVM (SA21).

With these, a2g on the executable translates the program exactly as on the JVM, from the first
run: every Go form identical (`diff -r` of a2g's output trees).

### Speed (and what the evaluator was doing)

a2g on the JVM: about 80 s (load 12-14 s, reach 4-7, translate 33-50, write 7-9). On Go, main's
executable: 34 min 46 s (load 228 s, reach 74, translate 995, write 265), 9.6 GB of RSS, 26 times
the JVM. Go's profiles (`ARBACE_CPUPROFILE`) showed two faults of the evaluator, not of a2g:

- **Calls through a seq compiled the method every time** (`Evaluator.invokeFn` of a variadic fn
  or `apply`): 15% of the CPU time. Fixed on `s3-g2a` as SA32 (the same fix was made here and
  dropped at the merge).
- **Static fields were looked up by name at every read** (`StaticFieldExpr` evaluated by
  `evalIn`: `Reflector.getStaticField` lists the class's fields, building a `Field` per member,
  at each read; a2g and the analyzer read `Opcodes/ACC_*` everywhere): 4%. The closure compiler
  now compiles it into `CodeStaticField`, which reads the analyzer's resolved `Field`, a static
  final field of a primitive type or `String` (a constant) once (SA24).

After both (the fixpoint's runs): a2g on Go 1,365 s (load 113, reach 56, translate 742-747, write
160), 9.6 GB of RSS at most (GOGC 400; a live heap of 1.1-1.6 GB), against 79 s on the JVM (12,
4.5, 33.5, 6.9): 17 times, the translation 22 times, the reach and the load 9-12. The rest is the evaluator itself, spread thin (frames, `Code` dispatch, calls
through member tables, lazy seqs, boxing); the collector marks on other cores (GOGC 400: about
105 collections of a 1.1 GB live heap, 25% of the CPU samples, under 1% of the wall time).
The ratio, 15-20x, is the evaluator's on this kind of code (SPEED-NOTES: 5-500x, 54x geometric
mean on benchmarks); what closes it is compiling Arbace to Go (S6): a2g, the analyzer and g2a
built into the executable as Go rather than evaluated.

### The build on Go: `ARBACE_GO_HOST`, `A2G_HOST`

- `bin/a2g` takes `A2G_HOST=jvm|go` (as `bin/g2a`'s `G2A_HOST`, SA30): `go` runs `-m
  arbace.a2g.main` on `A2G_GO_EXE`, else `target/arbace-go/<host arch>/arbace`, from a copy in
  `.tmp/a2g-host/`, the checkout first on `ARBACE_PATH` (a2g's namespaces are not embedded: they
  load from source; the analysis is the executable's own, embedded and prepared), with
  `GOGC=400` and `GOMEMLIMIT=12GiB` unless set (the JVM's heap is 12 GB) (SA20).
- `ARBACE_GO_HOST=go bin/arbace-go --build` sets both: the build's Arbace tools run on the
  executable of the previous build. The JVM stays the default until S4.
- `bin/arbace-go --fixpoint` runs the chain (amd64): G1 by the JVM chain, G2 by G1, G3 by G2,
  each a whole `--build` (a2g, `bin/g2a build`, the image, the final link) in
  `target/arbace-go/fixpoint/g{1,2,3}`; then it compares a2g's trees (`diff -r`, `report.edn`
  without its times) and the executables byte for byte, prints their hashes, and writes
  `g3/printed-by`. `ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3 bin/seed --go-make` packs
  G3's printed module as the seed (SA7), its `SEED.edn`'s `:printed-by` from that file (SA25).

### The fixpoint (2026-10-12)

On `s3-fixpoint` with main at `aea0b98` merged (commit `f293ba7`), `bin/arbace-go --fixpoint`,
48 min 46 s:

| | built by | a2g | `bin/g2a build` | the build |
|---|---|---:|---:|---:|
| G1 | the JVM chain (a2g and g2a on `target/stage2`) | 79 s | 9 s | 100 s |
| G2 | G1 (`A2G_HOST=go`, `G2A_HOST=go`) | 1,365 s | 31 s | 1,407 s |
| G3 | G2 | 1,377 s | 31 s | 1,419 s |

- a2g's program trees (`a2g/`: the generated forms, the program root with jrt's files, the
  embedded resources, `namespaces.txt`, `report.edn` but its times): G1 = G2 = G3 (`diff -r`). So
  a2g on Go writes the JVM's tree.
- The printed modules (`work-amd64/mod`, what the seed packs): G1 = G2 = G3 (`diff -r`).
- The executables: **G1 = G2 = G3 byte for byte**, SHA-256
  `2f19b3d1c64525db0b11e71c58a7aca750d95f35da8fb6a0ad3f9b6388540f8d`, 69,202,080 bytes.
- `ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3 bin/seed --go-make` packs G3: 3,069,608
  bytes, 1,148 files, 29,168,488 bytes unpacked, `:printed-by` the fixpoint (SHA-256
  `2471d77c…`, a dry run in `.tmp`, not committed: SA7).

Before the fixes of the speed section, a2g on main's executable (with the three changes above
loaded over it) already wrote the JVM's translation (every Go form; the resources then
uncompressed, the zlib not yet written).

### Checks

- `bin/arbace-go --smoke` (amd64, G1): 83 ok, none failed.
- The oracle on G3 (the runtime changed: SA24), `--expected test/oracle/known-go-amd64.edn`:
  22,232 of 22,278, the 46 recorded mismatches, 0 new, 0 now passing.
- `bin/build-arbace`: stage 2 = stage 3; stage 2's `arbace/lang`, `arbace/asm`, `arbace/java/api`
  byte-identical to main's (moving the two functions changes no class the compiler emits).
- `arbace.a2g.zlib` on the JVM: its streams inflate to their input (the JDK's `Inflater`, Python's
  zlib) for the sources, `tzdb.dat`, the empty input and runs; on Go the executable's image step
  reads every compressed source through jrt's inflater (`prepare`).
- The essential `bin/gate` (main at `aea0b98` merged): passed (5m19s).

### Left for S4

- The default: `ARBACE_GO_HOST=go` (and `A2G_HOST`, `G2A_HOST`) becomes the build's, the seed's
  executable as G1 (`bin/seed --build`, then `--fixpoint` from it, not from the JVM); the
  `--fixpoint`'s first step then is stage 0 instead of the JVM chain.
- The time: a Go-hosted `--build` takes about 24 minutes against 2 on the JVM (a2g 23 of them).
  `bin/gate`'s cached executable hides it when nothing changed; a change to `arbace/` costs a
  full a2g run. S6 (Arbace compiled to Go) is the remedy; until then, a2g's phases could run in
  parallel (translation per class is independent but for the shared literal pools).
- The edge (`bin/a2g-edge`) and its recording stay on the JVM (D4).
- `bin/arbace-go --build-tests` (the test build) and `--build --box` on Go: the same a2g and g2a,
  not separately proven here.

### Amendments

Provisional: the user, away, asked for the recommended choice applied and marked so (2026-10-12).

- **SA20 a2g's host chosen by `A2G_HOST`; `ARBACE_GO_HOST` for the build.** (a) `A2G_HOST=jvm|go`
  in `bin/a2g` as SA30's `G2A_HOST`, and `ARBACE_GO_HOST=go` in `bin/arbace-go` setting both;
  the JVM the default until S4; (b) one variable for both tools only; (c) Go the default now (24
  minutes a build). *Recommended: (a)*. Provisional.
- **SA21 The embedded resources compressed by a DEFLATE compressor in Arbace.** (a)
  `arbace.a2g.zlib`, the same bytes on every host (output 0.5-0.7% larger than zlib's; the
  executable +8 KB); (b) a2g writes the resources plain and the image step compresses them with
  Go's `compress/zlib` in the executable (a jrt native; a2g's tree then differs from today's
  shape, the build's steps change, the box's too); (c) no compression (+1.2 MB, SZ2 undone);
  (d) zlib's C algorithm ported to give the JDK's bytes (large, fragile). *Recommended: (a)*.
  Provisional.
- **SA22 javac's implicit constructor parameters in the analysis.** (a) `ctor-real-desc`,
  `this0-name` (and `outer-level`) move from `arbace.classes.emit` to `arbace.classes.analyze`;
  a2g no longer loads the bytecode back end; the embedded `analyze.ae` and the image change;
  (b) a copy in a2g (two definitions of javac's rule); (c) `emit` loadable on Go (ASM in the Go
  world). *Recommended: (a)*. Provisional.
- **SA23 The JVM-only question in `arbace.a2g.jvm`, loaded on demand.** (a) the module layer's
  exports asked through `requiring-resolve` when recording the edge or with `--jvm-reflection`;
  (b) reflection by name (`Class/forName` of `java.lang.ModuleLayer`). *Recommended: (a)*: S4's
  removal of the JVM paths then deletes one file. Provisional.
- **SA24 The evaluator's static field reads** (runtime, `arbace/lang/go/CompilerCode.ae`):
  `CodeStaticField` reads the resolved `Field` (`Reflector.prepRet` as `getStaticField` does), a
  primitive or `String` static final once. *Recommended: accept* (a fix, as SA32). Provisional.
- **SA25 `bin/arbace-go --fixpoint` and the seed from G3.** (a) the chain as a mode of
  `bin/arbace-go` into `target/arbace-go/fixpoint/`, and `bin/seed --go-make` packing a named
  build (`ARBACE_SEED_GO_BUILD`, its `printed-by`); (b) a separate `bin/fixpoint` script; (c)
  the chain in `target/arbace-go` itself, each build replacing the previous (no comparison of
  G1 and G2 after the fact). *Recommended: (a)*. Provisional.

## S4: the JVM removed

Status: done on the branch `s4-final` (2026-10-12/13), from main at `6cb552a`, for the milestone
`arbace-seed-is-go-v1`; the amendments SA40-SA88 below are provisional (the user, away, asked for
the recommended choices applied and marked so). The work ran as five branches merged into
`s4-final`: the coordinator's (the seed, the build, the gate, the JVM's products and the
JVM-only namespaces), `s4-a2gpar` (a2g in parallel), `s4-frozen` (what stays on a JVM, on the
frozen jar), `s4-tools` (the tools on the Go executable) and `s4-backend` (the bytecode back ends
and ASM). The inventory above ("S4: inventory for the JVM's removal") is done but for what
"Left" below names.

What changed, in one paragraph: the bootstrap starts from the Go-source seed
`seed/arbace-seed-go.tar.xz` (the fixpoint's printed program, committed with this stage); Go's
toolchain builds it into stage 0 (`bin/seed --build`), which hosts every Arbace tool (a2g, g2a,
c2a, the suite's runner) with their namespaces loaded from the checkout; `bin/arbace-go --build`
is a Go-hosted build by stage 0, `--fixpoint` builds G1 by stage 0, G2 by G1, G3 by G2. The JVM
runs only the frozen jar of `arbace-for-java-26-v4` (pinned by hash): the oracle's reference, j2a
(javac), and the recording or checking of what the JVM answered (the edge, the JDK's forms, the
fixtures' forms, data generators). The JVM Arbace's build and products, ASM, the bytecode back
ends, the class loader and the JVM-only namespaces left main (they are on `arbace-for-java-26`):
132 files and 25,474 lines deleted, `arbace/` 25,410 lines fewer (`Compiler.ae` 8,717 → 5,525).

### The bootstrap from the seed (the coordinator's)

- **`bin/seed`** (SA6 done): `--check` (the archive's pinned hash), `--build [-o DIR] [--arch A]`
  (stage 0 into `target/seed/<arch>/arbace`, `target/seed/seed.sha256` naming the seed; about 15 s
  with Go's build cache warm), `--make [ARCHIVE]` (packs a build: `ARBACE_SEED_GO_BUILD`, the
  fixpoint's G3 for a seed to commit), `--verify [REV]`. The jar's modes, `seed/arbace-seed.jar`
  and its pin left. `--verify` now reproduces the seed as it was made (SA41): in a worktree at
  REV (by default the commit `SEED.edn` names), stage 0 built from the seed being verified hosts
  `bin/arbace-go --fixpoint`, G3 is packed, the tars are compared byte for byte, and stage 0 =
  G1 = G2 = G3 byte for byte: the seed's executable rebuilds itself. `SEED.edn`'s `:printed-by`
  names the procedure, not the host (SA43), so the reprint is identical.
- **`bin/lib/host.bash`** (SA40): `arbace_host_exe`, the executable that hosts the tools:
  `ARBACE_HOST_EXE` when set, else stage 0, built from the seed when missing or when the seed
  changed. `bin/a2g` and `bin/lib/g2a-host.bash` take it (`A2G_GO_EXE`, `G2A_GO_EXE` name another,
  as the fixpoint does); `bin/lib/tools.bash`'s `arbace_tool` too (below). The executable runs
  from a copy (`.tmp/a2g-host`, `.tmp/g2a-host`) as in S3.
- **`bin/arbace-go`**: `--build` is Go-hosted (a2g and g2a on the host; `ARBACE_GO_HOST` left:
  there is no other host); `--fixpoint` (SA42) builds G1 by the host, G2 by G1, G3 by G2, and
  holds when G2 = G3 (a2g's trees and the executables); G1 = G2 and host = G1 are reported, and
  hold when the host is stage 0 of a seed printed from the same sources; `--gate-build` builds the
  executable and the test build (amd64) into `.tmp/arbace-go-gate`, again only when `gate_key`
  changed (Arbace's sources, `go/`, `overlay/`, `jdk/`, `x509roots/`, `bin/lib`, the fixtures'
  forms, the build's scripts, the seed's and the toolchain's pins, `ARBACE_HOST_EXE`'s hash when
  set, Go's version; no `java -version`); `--gate` adds the smoke test; the smoke test's JVM checks
  (`jvm_store_checks`, the JVM's TLS checks) left with the JVM.
- **Removed**: `bin/build-arbace`, `bin/arbace-j`, `bin/arbace-image`, `seed/arbace-seed.jar`
  and `.sha256`, `test/aot-training.ae`, `test/arbace-results.edn`; the namespaces the Go build
  left out, removed rather than ported (SA45, D13): `arbace.repl.deps`, `arbace.java.basis` (and
  `.impl`), `arbace.tools.deps.interop`, `arbace.java.browse`, `arbace.java.browse-ui`,
  `arbace.java.javadoc`, `arbace.inspector` (Swing), `arbace.xml` (SAX); `instant_timestamp.ae`
  folded into `instant.ae`.
- **The namespace variants without a JVM counterpart became the sources** (SA46; `git mv` or the
  substitutions applied, the embedded text unchanged but for headers): `instant`, `box/host`,
  `tls/host` (the JVM's java.nio and `javax.net.ssl` host layers left: BOX-NOTES.md, "After
  standalone S4"), `core_bean`, `java/io`; `s4-backend` did the same for `genclass`, `core_proxy`,
  `main`, `core/server`, `reflect/java`, `classes/native`, `go`, `interp` (SA83). Left as variants:
  `classes/{analyze,env,types}.subst.ae` (the front end still runs on the frozen JVM for j2a and
  the edge) and the class forms' variants `arbace/lang/go/*.ae` (SA82).

### a2g in parallel (`s4-a2gpar`, SA50-SA53)

On the Go executable a2g took 1,365 s (load 113, reach 56, translate 742, write 160), and a build
needs two runs (the program, the test build). The classes are now translated on `A2G_JOBS` threads
(default the processors, at most 16: SA51), each apart: its string literals named apart
(`Lit?i_k`), its additions to the package's sets (`Up_` conversions, lambda adapters, cut
classes) and to the race candidates recorded in order, its output captured. A merge in class order
then does what the sequential loop did: it appends the forms, replays the additions one by one,
and numbers the literals in the package's pool, the renames applied when the files are written
(`out/*renames*`): the output is the sequential translation's byte for byte (SA50). The printed
temporaries' ids are all taken by the analysis in the load; a2g's own gensyms are never printed;
the caches memoize pure functions. The registrations and the dynamic class's slot methods run the
same way; writing computes each file's text in parallel (a long file lays out its unlined forms in
parallel first); resources are compressed in parallel and `report.edn` is pprinted beside the
writing (SA52); the load reads its files and scans the embedded sources in parallel (SA53). Three
quadratic steps became linear (`operands` of an array initializer: `Character$UnicodeScript` 12 s
→ 2 s on the JVM; `package-private-overrides`; `collisions`' sort).

| a2g on Go | total | load | reach | translate | write | peak RSS |
|---|---:|---:|---:|---:|---:|---:|
| sequential (S3) | 1,365 s | 113 | 56 | 742 | 160 | 9.6 GB |
| 64 threads | 242 s | 108.5 | 57.5 | 26.0 | 37.0 | 12.2 GB |
| 16 threads (the default) | 258 s | 123 | 57.8 | 31.3 | 32.4 | 10.1 GB |

On the JVM 86 s → 29 s. `bin/arbace-go --build` (amd64, Go-hosted) 4 min 47 s against about 23.5;
its executable identical to the one built from the sequential tree (`06fe240c…`, at `e1bf825`).
What remains sequential is the load's analysis (it numbers anonymous classes and the
temporaries in order) and the reach (about 165 of the 242 s): S6's to close.

### The tools on the Go executable (`s4-tools`, [T], SA70-SA79)

`bin/lib/tools.bash`'s `arbace_runtime` (`target/stage2`) gave way to `arbace_tool [-p DIR]...
[-s NS]... [-D NAME=VALUE]... ARG...`: the tools' host run with the checkout, then the tool's
directories, on `ARBACE_PATH`; system properties as `-e (System/setProperty ...)` (SA70). The
tools' namespaces not embedded (g2a, a2g, j2a, test/'s) load from the checkout as on the JVM; c2a,
embedded, is loaded from the checkout over the executable's copy (`-s`, SA71). `G2A_HOST` and
`A2G_HOST` only guard against `jvm` now (SA73).

- c2a: `bin/c2a`, `bin/c2a-check` (unit 120 of 120; `--suite` as recorded; `--libs` against the
  JVM's output, `test/c2a/libs.out`, SA75). `bin/clojure-tests`' Go mode needs no JVM: c2a, the
  listing and the report on the host (SA74), the jars unzipped; the rename runs on the JVM stages
  (`CLOJURE_TESTS_RENAME`, `CLOJURE_TESTS_PRECOMPILED`) are gone; the renamed suite, runner and six
  libraries equal the JVM's (`diff -r`).
- g2a: `bin/g2a` (read, convert, build, roundtrip), `g2a-print`, `-print-tests` (268),
  `-convert-tests` (150), `-build-tests` (30) on Go; `roundtrip amd64` 248 s, `linux/amd64` 255 s
  (6.4-6.8 GB), no regressions.
- a2g: `bin/a2g-check` on Go (2,184 s): 8,958 of 9,022 per architecture, its programs and counts
  identical to a JVM run; its programs run without the host's `ARBACE_PATH` (SA79); the fixtures'
  results recorded on a JVM, `test/a2g/fixtures.edn` (`bin/a2g-check --record-fixtures`, on the
  frozen jar, SA72); `a2g-evalproof` and `a2g-race` as before; `test/a2g/eval_ops.ae` on Go writes
  `CompilerOps.ae` byte for byte (SA78).
- The vendoring tools through the Go-hosted g2a: `x509roots-vendor verify`, `check`;
  `box-vendor roundtrip` (20 of 20), `kvm-tests`, `purego-check`; `tamago-vendor verify` (383 s),
  `print`, `check` (both guests identical); `bin/jrt build`, `test`, `print`.
- `bin/net-check`'s client on the host (SA77); `bin/java-surface` retired, its output
  `doc/go/java-surface.edn` kept as data, `test/jrt/java_surface.ae` kept as the library
  `bin/jrt testdata` requires (SA76).

### What stays on a JVM: the frozen jar (`s4-frozen`, [R], SA60-SA69)

What stays on a JVM runs on the frozen jar of `arbace-for-java-26-v4` (`bin/lib/frozen.bash`:
built once from the tag, pinned by `test/oracle/frozen-jar.sha256`), never on a JVM build of the
checkout. The jar's loader reads `.clj` only, and loads a namespace from its source when the
source is newer than its `__init` class (the jar's entries are dated 2000-01-01): `frozen_view`
puts `.clj` links to the checkout's tool namespaces (j2a, a2g, c2a, g2a, the front end
`arbace.classes.{types,env,parse,lower,analyze}`, and every namespace the jar lacks: `opcodes`,
`go`, `jvm`, `interp`) *before* the jar on the class path; `arbace.core`, the runtime and the
jar's own back end (`native`, `emit`, `compiler` ...) stay v4's (SA62). On it:

- j2a (O1 (a)): `bin/j2a`, `bin/j2a-check`, `bin/jrt-convert`, `bin/clojure-tests
  --java-fixtures`. j2a's checks are forms-level (O2 (b), SA63): the baseline converted with
  `--check` against a SHA-256 manifest (`test/j2a/expected/baseline.sha256`, 189 files), the
  samples against their committed forms (`test/j2a/expected/java`, `module`), `--update` to
  rewrite them; and `--suite` (O2 (a)): the converted baseline compiled by the jar's own compiler
  (`test/j2a/build.ae`, SA67) and Clojure's suite run on it: 83 namespaces, 20,718 of 20,750, no
  regressions against `test/baseline-results.edn`. `bin/class-forms-check` and
  `test/classes/check_converted.ae` left; `jrt-convert`'s `check` step compares with the committed
  forms as a report (SA64); `jdk/provenance.edn` and the fixtures' provenance name j2a's host
  (SA65).
- a2g's edge (`bin/a2g-edge update`, `verify`, `trace`): a2g and the front end from the checkout;
  `--record-edge` reads class files through `arbace.classes.jvm` (ASM's ClassReader, in the jar);
  the fixtures default to the committed `test/java-fixtures/forms` (SA69).
- The oracle: the frozen reference only (`ARBACE_ORACLE_REF=main` and `bin/arbace-j` left); the
  expected files recorded again from v4 (SA60: v4's values where main diverged,
  `polymorphism.ae:176` and `state_errors.ae:542`; the stable values kept for the noisy
  `strings.ae:408` and `state_errors.ae:579`); `test/oracle/known-go-amd64-frozen.edn` the gate's
  reference of known mismatches and `known-go-amd64.edn` removed (SA61); `harvest`'s default the
  Go run's renamed suite.
- `bin/native-tests` and `test/native/` left after their semantic tests moved into the oracle's
  corpus, `test/oracle/forms/native.ae` (269 cases, recorded from v4; SA66): the nine named
  (defclass, deftype, fn, overload, nan, bridge, reduce, imports, nested_deftype) and the semantic
  parts of inline, reflect and virtual_threads; retired: aot, verify, version, box, the member
  shapes, opcodes and stack checks.
- The data generators: `bin/jrt` testdata, manifest, standins; `bin/jrt-coverage`;
  `bin/net-check`'s servers; `test/classforms/class_scripts.ae`. `gen_corpus.ae` retired with the
  tests it read, `test/oracle/forms/defclass_corpus.ae` kept as data (SA68).

Every committed output these tools make is reproduced byte for byte (`jdk/forms` and `data`,
`jdk/edge.edn`, `test/java-fixtures`, the standins, the coverage file), but
`go/arbace/jrt/reflect_tables.ae`: `bin/jrt manifest` gives another order of
`ForkJoinTask.adapt`'s overloads, on the frozen jar as on main's stage 2 (the committed file is
stale against the JDK's CDS order; not regenerated here, since it changes the Go build).

### The back end and ASM (`s4-backend`, [P], SA80-SA88)

ASM (`arbace/asm.ae`, `arbace/asm/`, 14,747 lines) and the JVM's bytecode back ends left main.

- O3 (SA80): the access flags in `arbace.classes.opcodes` (25 `^:const` `ACC_*` ints), required
  `:as Opcodes` so that `Opcodes/ACC_PUBLIC` reads as before, by `classes/{parse,analyze,env}`
  and a2g. `TypeReference` is `arbace.classes.go`, required `:as TypeReference` on every host
  (SA81).
- The class forms compiler keeps its front end (`types`, `env`, `parse`, `lower`, `analyze`,
  `native`) with `opcodes`, `go`, `interp` (the Go build's interpreter, no longer a variant) and
  `jvm` (env's class-file reading with ASM's ClassReader, loaded by `requiring-resolve` only for a
  class read from a class file, i.e. on the frozen jar; left out of the embedded tree; SA84).
  `emit`, `compiler`, `shape`, `verify`, `build`, `boot`, `arbace/j2a/build.ae`,
  `bin/class-forms-tests` with `test/classes/`, `test/classforms/jvm_check.ae` left. The analyzer's
  accessors are data only.
- `Compiler.ae` lost its 239 members that name ASM (the emit methods, ASM's fields and constants),
  `SourceDebugExtensionAttribute`, direct linking (`StaticInvokeExpr`, SA88) and the annotation
  hooks; what the Go variant replaced only to drop the back end is the base's now (`compile`,
  `writeClassFile`, `primInterface`, `buildArrayClassDescriptor`, `getCompiledClass`, the
  compiler options' initializer); the variant keeps the evaluator and erases nothing (SA82).
  `Compile.ae`, `DynamicClassLoader.ae`, `Intrinsics.ae` removed; `RT.ae` took its variant's
  `makeClassLoader` and `classForName`.
- `jdk/edge.edn`: ASM's, `DynamicClassLoader`'s, `Compile`'s and `Intrinsics`' 39 entries
  pruned (SA87), then recorded again on the frozen jar at integration (`bin/a2g-edge update`).

**The language's divergences (D13)**, each a recorded divergence of Arbace from Clojure on the
JVM (doc/VENDOR-NOTES.md, hand change 23, "Dialect divergences"):

- `gen-class` does nothing (nil); `ns`'s `:gen-class` is accepted and ignored (SA85).
- `gen-interface`, `definterface`, `defprotocol`'s interface and `proxy`'s class are made at run
  time (the evaluator's `Dyn`), as they were in the Go build; no class file.
- `compile` loads the namespace with `*compile-files*` true and writes nothing.
- `defmodule`, `defpackage` do nothing (SA86); `defclass` and the class forms are interpreted.
- Annotations in deftype or defrecord metadata are carried by no class file.
- `add-classpath` / `RT.addURL` throw; `*loader*` and `Compiler/LOADER` are nil; the REPL and
  prepl set no `DynamicClassLoader` as the thread's context loader.
- The REPL's `repl-requires` are `arbace.repl`'s only (no javadoc, pprint, repl.deps); an error
  report is printed with `prn`.
- `arbace.reflect`'s `AsmReflector` exists, its reflection throws; `JavaReflector` is the
  default.
- `:direct-linking` is ignored; `*compiler-options*` are not read from system properties.
- The namespaces removed (SA45): `arbace.repl.deps` (`add-lib`, `add-libs`, `sync-deps`),
  `arbace.java.basis`, `arbace.tools.deps.interop`, `arbace.java.browse` and `browse-ui`,
  `arbace.java.javadoc`, `arbace.inspector`, `arbace.xml`: requiring them fails, as it did in
  the Go build.

### The gate (SA44)

Essential (`bin/gate`): `bin/seed --check`; `bin/seed --build` (stage 0, the bootstrap from the
seed); `bin/arbace-go --gate-build` (the executable and the test build by stage 0, cached by key);
then concurrently the smoke test, Clojure's suite on the test build (against
`test/arbace-go-results.edn`) and the oracle on the executable against the frozen reference
(`test/oracle/known-go-amd64-frozen.edn`). `--full` adds, concurrently: on the frozen JVM
`j2a-check --suite`, `jrt-convert verify`, `a2g-edge verify`, `--java-fixtures verify`; the Go
build afresh with its smoke test, the box's guests from `tamago/` (`tamago-vendor check`) and the
box's smoke test, then `bin/arbace-go --fixpoint`; g2a's round trips, the box's vendoring checks,
`tamago-vendor verify`, `x509roots-vendor`. The gate ignores `ARBACE_HOST_EXE`: its host is stage
0. `bin/class-forms-tests` and the suite on stages 1 and 2 left with the JVM.

### Proofs

On `s4-final` (2026-10-13), the sources at `ed892a1`, the seed committed alone in `eaea7dd`:

- The fixpoint (`bin/arbace-go --fixpoint`, host S3's G3 `2f19b3d1…`, which embeds main's older
  analyzer, so host ≠ G1): G1 = G2 = G3 byte for byte,
  `f114b6bd943a508b8cb6e3b0bb8d7b0728d35b531fd612326ee4fb40073fce8a` (68,972,704 bytes), a2g's
  trees equal; 280, 274 and 273 s a build (a2g load 106-108 s, reach 55 s, translate 29 s, write
  19 s).
- The seed: `ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3 bin/seed --make`:
  `seed/arbace-seed-go.tar.xz`, 3,058,352 bytes, 1,145 files (29,069,147 bytes unpacked),
  SHA-256 `8ad2fb89b0ef7bf7923b59f9f6ec951d66791363f6c30c3c23ae8167a000e072`; `SEED.edn`'s
  `:commit` `ed892a1…`, `:dirty false`.
- `bin/seed --verify` (14m48s): the fixpoint from the seed's own stage 0 printed again at
  `ed892a1`, the same tree byte for byte (the tar `262d37e5…`), stage 0 = G1 = G2 = G3
  (`f114b6bd…`).
- The essential `bin/gate` on `eaea7dd` (13m29s): the seed as pinned; stage 0 in 15 s
  (`f114b6bd…`); the executables built by stage 0 in 587 s; the smoke test; Clojure's suite on
  the Go test build 20,506 of 20,520, no regressions; the oracle against the frozen v4 22,490 of
  22,547, the 57 known, 0 new.
- The box: `ARBACE_BOX_ARCHES=amd64,arm64 bin/arbace-go --build --box` (245 s); `bin/tamago-vendor
  check` (233 s): both guests, with and without symbols, identical to TamaGo's originals;
  `ARBACE_BOX_ARCHES=amd64 bin/arbace-go --smoke --box` (215 s): 111 ok, no failures.

### Left

- `go/arbace/jrt/reflect_tables.ae` is stale against `bin/jrt manifest` (the order of
  `ForkJoinTask.adapt`'s overloads): regenerate it deliberately (it changes the Go build).
- Two Go-build bugs found by the moved native tests (a top-level `do` of mutually referring
  `defclass`es; a class forms `lambda` called in a handed-over fn): fixed after S4 (below, "After
  S4: two Go-build bugs"). By design: eval fns implement no primitive `IFn$LL` interfaces
  (`native.ae:233`, `:234`, `:237`); no `java.lang.SecurityManager` (`:412`); jrt's
  `Method.invoke` wrong-receiver message (`:652`).
- `test/oracle/known-go-box-amd64.edn` has the new groups but was not run on the box.
- The class forms' Go variants (`arbace/lang/go/*.ae`) and the front end's `.subst` variants stay
  (SA82, SA83): merging them waits until nothing else reads the bases.
- JVM-only code paths kept in shared sources (the version resource in `arbace.core`,
  `RT.inArbacePath`'s class path case): harmless on Go, to trim with the variants' merge.
- The load's analysis and the reach in a2g stay sequential (S6).

### Amendments

Provisional (the user, away, asked for the recommended choices applied and marked so;
2026-10-12/13).

- **SA40 The tools' host.** (a) `bin/lib/host.bash`'s `arbace_host_exe`: `ARBACE_HOST_EXE`, else
  stage 0 from the committed seed, rebuilt when the seed changes; (b) the last build
  (`target/arbace-go/<arch>/arbace`), as the JVM's stage 2 was; (c) an executable built from the
  checkout before each tool run. *Recommended: (a)*: a build is a function of the commit (its
  seed and sources), and the gate's key covers the host through the seed's pin; (b) makes a build
  depend on what was built before, (c) costs a build per tool. Provisional.
- **SA41 `bin/seed --verify` reproduces the seed as it was made.** (a) The fixpoint at REV from
  the seed's own stage 0, G3 packed, the tars compared, and stage 0 = G1 = G2 = G3 (three
  Go-hosted builds, about 20 minutes); (b) one build by stage 0, the tars compared but for
  `:printed-by`; (c) S4's preparation's scheme (the commit's own JVM build). *Recommended: (a)*: it
  proves both that the seed is its commit's fixpoint and that its executable rebuilds itself.
  Provisional.
- **SA42 When `--fixpoint` holds.** (a) G2 = G3 (trees and executables), with G1 = G2 and host =
  G1 reported; (b) G1 = G2 = G3 required. *Recommended: (a)*: G1 is the host's translation, and
  differs legitimately when the host is not these sources' fixpoint (a seed older than a change
  of the analysis or the runtime); `--verify` requires all four equal for the seed's own commit.
  Provisional.
- **SA43 `SEED.edn`'s `:printed-by` names the procedure.** (a) "the Go-hosted fixpoint ...: G3,
  a2g and g2a on G2, G2 built by G1, G1 by the host executable", without the host's identity; (b)
  the host's hash. *Recommended: (a)*: `--verify` runs the same procedure from another host (the
  seed's stage 0) and must print the same tar; the fixpoint makes G3 independent of the host.
  Provisional.
- **SA44 The gate.** (a) As above: the essential gate builds both executables by stage 0
  (cached by key), then smoke test, suite and oracle; `--full` adds the frozen JVM's checks, the
  fresh build with the box, the fixpoint, g2a and the vendoring checks; (b) the fixpoint in the
  essential gate (three builds per change, about 15 minutes); (c) the suite and the oracle in
  `--full` only. *Recommended: (a)*: the survey's exit criterion makes the suite on Go the gate's
  suite, and a parallel a2g makes a cache miss about 10 minutes of builds. Provisional.
- **SA45 The JVM-only namespaces removed, not ported.** (a) `repl/deps`, `java/basis`,
  `tools/deps`, `java/browse`, `browse_ui`, `javadoc`, `inspector`, `xml` removed (D13; the Go
  build never had them), `instant_timestamp` folded into `instant`; (b) kept as sources that fail
  to load; (c) ported (a SAX parser in jrt for `arbace.xml`, a browser launcher...).
  *Recommended: (a)*; (c) when a use appears. Provisional.
- **SA46 The namespace variants become the sources.** (a) Where the variant has no JVM
  counterpart left (`instant`, `box/host`, `tls/host`, `core_bean`, `java/io`, and `s4-backend`'s
  SA83), the variant's text is the source, moved with `git mv` or the substitutions applied, the
  embedded text unchanged but for headers; (b) keep the variants until a general merge.
  *Recommended: (a)*. Provisional.
- SA50-SA53 (`s4-a2gpar`):
  - **SA50 The parallel translation's determinism.** (a) Each class translated apart, merged in
    class order (literals renamed to the sequential numbering, the sets' additions replayed): the
    output byte for byte today's; (b) per-class literal pools or a pre-pass (simpler, every file
    changes); (c) sequential until S6. *Recommended: (a)*. Provisional.
  - **SA51 The threads.** (a) the processors (242 s, 12.2 GB); (b) min(processors, 16) (258 s,
    10.1 GB); (c) fewer. *Recommended: (b)*. Provisional.
  - **SA52 `report.edn`'s times.** (a) pprinted beside the writing, `:times` (with `:steps`) put
    in at the end on one line; (b) pprinted at the end. *Recommended: (a)* (30 s less on Go).
    Provisional.
  - **SA53 The load's reading in parallel.** (a) The class forms files read and the embedded
    sources scanned in parallel (no reader construct in class forms takes ids; the scan takes the
    same number in any order); (b) sequential (8 s more). *Recommended: (a)*. Provisional.
- SA60-SA69 (`s4-frozen`):
  - **SA60 The expected files from v4.** (a) v4's record wholesale; (b) v4's values where main
    diverged, the stable values of noisy cases kept. *Recommended: (b)*. Provisional.
  - **SA61 The known-mismatch files.** (a) `known-go-amd64.edn` kept beside; (b) removed,
    `known-go-amd64-frozen.edn` the gate's; (c) the frozen file renamed to the old name.
    *Recommended: (b)*: 57 cases (the 48 and five groups for `native.ae`); the box's file
    likewise. Provisional.
  - **SA62 What the frozen view loads from the checkout.** (a) the front end with
    `arbace.classes.native`; (b) without `native` (the jar's compiler's hook into its own back
    end), plus the tools' namespaces and every namespace the jar lacks, the view before the jar.
    *Recommended: (b)*. Provisional.
  - **SA63 j2a-check's forms-level criterion.** (a) every expected form committed; (b) the
    samples' forms and a SHA-256 manifest of the 2.2 MB baseline; (c) hashes only.
    *Recommended: (b)*. Provisional.
  - **SA64 jrt-convert's check step.** (a) dropped; (b) a comparison with the committed
    `jdk/forms`, as a report; (c) the forms analysed by the front end. *Recommended: (b)*;
    `verify` stays the proof. Provisional.
  - **SA65 Provenance names j2a's host** (`:host "arbace-for-java-26-v4 <sha>"`), since the output
    depends on the runtime running j2a; alternative: the j2a sources' hash only. *Recommended:
    name it*. Provisional.
  - **SA66 What moved from `test/native`**: the nine named tests and the semantic parts of
    inline, reflect, virtual_threads; retired: aot, verify, version, box, shapes, opcodes and
    stack checks, the property opt-in; NPE messages compared by class only, `(shutdown-agents)`
    at the end. Provisional.
  - **SA67 j2a's build driver** in `test/j2a/build.ae`, on the jar's own compiler. Provisional.
  - **SA68 gen_corpus and class_scripts.** class_scripts kept on the frozen jar; gen_corpus
    retired with `test/classes` (at integration), its corpus kept as data. Provisional.
  - **SA69 a2g-edge's fixtures** default to the committed `test/java-fixtures/forms`.
    Provisional.
- SA70-SA79 (`s4-tools`):
  - **SA70 The tools' runner `arbace_tool`** in `bin/lib/tools.bash` (a); each script invoking
    the executable itself (b); a `bin/arbace-tool` (c). *Recommended: (a)*. Provisional.
  - **SA71 c2a loaded from the checkout over the embedded copy** (a, `-s`: `load-file`, then
    marked loaded); the embedded c2a (b: an edit takes effect only after a build); c2a left out of
    the embedded tree (c). *Recommended: (a)*. Provisional.
  - **SA72 a2g-check's fixtures recorded on a JVM and committed** (`test/a2g/fixtures.edn`, by
    `test/a2g/record_fixtures.ae` on the frozen jar) (a); recorded on the Go host at each run (b:
    not possible, the Go build's class forms cannot evaluate three of them); dropped (c).
    *Recommended: (a)*. Provisional.
  - **SA73 `G2A_HOST`/`A2G_HOST` kept as guards** (a: unset or `go`; `jvm` fails naming S4);
    removed (b); `jvm` kept (c). *Recommended: (a)*. Provisional.
  - **SA74 The suite's listing and report on the tools' host** (a), not on the build under test
    (b). *Recommended: (a)*; `CLOJURE_TESTS_RENAME` is no longer a setting. Provisional.
  - **SA75 `c2a-check --libs` against the JVM's recorded output** `test/c2a/libs.out` (a); the
    host against a Go build only (b); the host alone (c). *Recommended: (a)*. Provisional.
  - **SA76 `bin/java-surface` retired**, its output and the library `test/jrt/java_surface.ae`
    kept (a); moved to the frozen jar (b: meaningless there). *Recommended: (a)*. Provisional.
  - **SA77 net-check's client on the tools' host** (a); on the frozen jar (b). *Recommended:
    (a)*. Provisional.
  - **SA78 `eval_ops.ae` reflects the Go executable's own classes** (a: run on an executable
    built from the checkout; two builds when `Numbers`, `RT`, `Util` or `Math` change); listed
    from the class forms by the analysis (b, later). *Recommended: (a) now*. Provisional.
  - **SA79 a2g-check's programs run without the host's `ARBACE_PATH`** (a: check's `sh`
    removes it); (b) another way in `bin/a2g-check`. *Recommended: (a)*. Provisional.
- SA80-SA88 (`s4-backend`):
  - **SA80 Opcodes' home**: `arbace.classes.opcodes`, `^:const` ints required `:as Opcodes` (a);
    an interface class form translated into the Go world (b); flags as keywords (c).
    *Recommended: (a)*. Provisional.
  - **SA81 TypeReference**: `arbace.classes.go` the source, required as `TypeReference` on every
    host (a); split into a host-neutral and a Go-only namespace (b). *Recommended: (a)*.
    Provisional.
  - **SA82 The class forms' Go variants stay variants** (a; the members the base kept only to be
    replaced take the variant's text; `ObjExpr.compile` and `compileStub` throw in the base and
    stay replaced); merge every variant now (b, about 8,000 lines); only delete the emitters (c).
    *Recommended: (a)*. Provisional.
  - **SA83 Which namespace variants become sources**: `genclass`, `core_proxy`, `main`,
    `core/server`, `reflect/java`, `classes/native`, `go`, `interp` (a), the front end's `.subst`
    staying variants for the frozen JVM; all of them (b). *Recommended: (a)*. Provisional.
  - **SA84 env's JVM part**: the class-file reading with ASM in `arbace.classes.jvm`, loaded on
    demand on a JVM only, `getDeclared*` kept in env for the Go REPL's class forms (a); all
    reflection moved (b). *Recommended: (a)*. Provisional.
  - **SA85 gen-class** a no-op (a); throws, as the Go variant did (b); removed with the ns clause
    (c). *Recommended: (a)*. Provisional.
  - **SA86 defmodule and defpackage** do nothing (a); throw (b). *Recommended: (a)*.
    Provisional.
  - **SA87 edge.edn pruned by hand**, then recorded again at integration (a); left alone until
    recorded (b). *Recommended: (a)*. Provisional.
  - **SA88 Direct linking**: `StaticInvokeExpr` removed, `:direct-linking` ignored (a); a stub
    kept (b). *Recommended: (a)*. Provisional.

## After S4: two Go-build bugs

Status: on the branch `go-bugs` (2026-10-13), from main at `7f7a2b6` (the milestone
`arbace-seed-is-go-v1` and record commits). The two bugs S4's "Left" named, found when the native
tests moved into the oracle; the frozen v4 jar (the oracle's reference) answered them right.

### A top-level `do` of mutually referring `defclass`es

`(do (defclass Even ... (Odd/odd ...)) (defclass Odd ... (Even/even ...)))` failed on Go with `No
method user/Odd.odd(I)Z` (`native.ae:29`, `:32`, `:50`). The compiler evaluates the do's forms
one by one, each class form compiled with the others entered as declarations (SPEC §9.2:
`ClassFormsExpr/pushSiblings`, `compile-top`'s `siblings`), which worked: Even's code was
analyzed against Odd's declaration. The cause was in `arbace.classes.interp/define!`, the Go
build's class forms: it built every method's interpreter tree right after defining the classes
("as the JVM compiles them all"), and building a call resolves its target, the `Klass` of an
interpreted class or a reflective `Method`. Odd, declared but not yet defined (its form comes
next in the do), had neither, and the build failed. On the JVM the bytecode names Odd and the
JVM resolves the name when the code first runs, after the do has defined it.

The fix (`arbace/classes/interp.ae`): a class the compilation holds as a declaration only (a
sibling, `sibling?`) that a body refers to and that cannot be resolved throws a signal
(`pending!`, an `ex-info` with `::pending`), from `klass`, `desc->class` and `real-method`.
`define!`'s eager build catches it per method: the method keeps its builder, so its tree is built
on its first call (`Meth.ensure`, as the interpreter already did for lambdas' and lazily built
methods, with the compilation's class set rebound), when the do's later form has defined the
sibling. A static initializer that refers to a sibling becomes a static method `<clinit>` of its
own, built and called on the class's initialization. While `define!` builds (`*eager*`), a
sibling is pending even when a class of that name exists, so a do evaluated again binds the new
classes, as the JVM's lazy resolution does (case `native.ae:750`, `:753`). At a first call
where the sibling is still not defined (a later form of the do threw), the call throws "Class X
(a class form of the enclosing do) is not defined yet" where the JVM throws
`NoClassDefFoundError`. The member types the host needs at definition (parameters, return,
fields: `meth-classes`, `publish!`) take `Object` for a sibling not yet defined (`sig-class`,
SA90): calls and values are right, reflection reports `Object` for that type
(`native.ae:745`, a known mismatch).

### A `lambda` called by class forms code

`(.apply (lambda java.util.function.Function [s] (str s "!")) "hi")` at the top level (the form
handed over to the class forms as an eval fn) threw `ArrayIndexOutOfBoundsException -1`
(`native.ae:184`). The interpreter's virtual call `CF$CallV` (`arbace/lang/go/ClassForms.ae`),
finding the receiver's interpreted method by its class's vtable, stored the receiver in the
callee's frame at `(.-self m)` unconditionally; a lambda's (or method reference's) method has
no receiver slot (`self` -1; its object holds the captured values, which `Meth.invoke` copies
into the frame with `Meth/capture`). The existing cases called lambdas only from Clojure code or
from the world (`ArrayList.sort`), through `Meth.invoke`; no class forms code had called one.
The fix: `CallV.enter`, used by `eval`, `evalJ` and `evalD`, makes the callee's frame as
`Meth.invoke` does: the receiver when the method has a slot for it, then the captured values.
Without it a capturing lambda called this way would also have lost its captures.

### Cases and checks

New oracle cases (`test/oracle/forms/native.ae`, namespace `native.siblings`, before the final
`(shutdown-agents)`, so earlier cases keep their lines; recorded from the frozen jar, `bin/oracle
record forms/native`, which changed only those entries): a do whose classes use a sibling in a
static initializer, a field's and a method's type, a constructor and a call each way; the
reflected return type; a mutually recursive do evaluated twice with different bodies; lambdas
capturing locals and returning objects, longs and doubles, a bound and an unbound method
reference, called in a handed-over fn; a lambda and a bound method reference called inside class
methods. `known-go-amd64-frozen.edn` and `known-go-box-amd64.edn`: the two bugs' groups (four
cases) removed, the reflected type's group (one case) added.

- `bin/arbace-go --build` (Go-hosted from stage 0, 7m29s); amd64 executable 68,984,992 bytes.
- `ARBACE_GO_ARCHES=amd64 bin/arbace-go --smoke`: 60 ok, no failures.
- The oracle against the frozen reference, `--expected known-go-amd64-frozen.edn` before the
  edit: 22,504 of 22,558, `:29`, `:32`, `:50`, `:184` now passing, `:745` the one new; after it,
  as recorded.
- The essential `bin/gate`: see the branch's commit.

The runtime changed (`ClassForms.ae` is translated into the program), so the committed seed is no
longer the program's fixpoint print; it is remade only at a milestone.

### Amendments

For the user to decide.

- **SA90 A sibling's type in a member's signature.** The host gets each member's types when the
  class is defined, before a later form of the do defines the sibling. (a) `Object` for a
  sibling not yet defined (as done): calls, fields and values right, reflection on the member
  reports `java.lang.Object` (`native.ae:745`, a known mismatch); (b) the members published again
  when the sibling is defined (jrt's member tables replace an entry: a change to `CFGo` and its
  natives); (c) an error, as before the fix (a do of classes referring to each other in their
  types fails). *Recommended: (a)* now, (b) if reflection on such members matters.
- **SA91 When a body referring to a sibling is built.** (a) On its first call (as done), only
  the bodies that refer to a sibling not resolvable while `define!` builds, every other body built
  at definition as before; (b) every body of a compilation with siblings on its first call;
  (c) a deferred call node resolving its target at its first run, the rest of the body built at
  definition. *Recommended: (a)*: build errors other than the sibling's absence are found by the
  analysis, which has run on every body; (c) needs a new node kind in the runtime for the same
  effect.
- **SA92 The error when a sibling is never defined.** (a) The pending signal's `ExceptionInfo`
  ("Class X (a class form of the enclosing do) is not defined yet") at the first call, where the
  JVM throws `NoClassDefFoundError`; (b) a `NoClassDefFoundError` with the class's name.
  *Recommended: (a)* (as done): a compile error of the class forms, as the other build failures
  are; (b) when code needs to catch the JVM's error (no case covers it).

