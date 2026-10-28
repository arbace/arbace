# Arbace

Arbace is a Clojure derivative written entirely in symbolic expressions.

- **Extended to represent its own implementation.** Clojure gained the Java forms
  ([spec](doc/classes/SPEC.md)): every construct of a Java class file as idiomatic Clojure
  (`defclass`, `method`, `switch`, `label`, ...). With them, and the Java → Java forms converter
  repr/java, Clojure's Java half became Clojure too. Arbace is that vendored Clojure, all `.ae`,
  bootstrapping itself in stages that rebuild each other byte for byte and pass Clojure's whole
  test suite.
- **Modernized for Java 26.** `invokedynamic` call sites for reflection, keywords and `str`, the
  JDK's AOT cache (a launch in about 0.2 s), a self-contained `jlink` image, an opt-in
  virtual-thread executor, and every class checked by the JDK's verifier (benchmarks:
  [BENCHMARKS.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/BENCHMARKS.md),
  on the branch `arbace-for-java-26`).
- **Evolving to represent its runtime too, down to the bare metal.** Arbace is leaving the JVM
  and the `.class` format behind: a standalone ecosystem whose language is Clojure plus forms for
  Java (kept usable at the source level through repr/java) and later Go. Its runtime is already a
  static Go executable, translated from Arbace's own sources and a part of the JDK's. The goal
  is a self-sustaining REPL in a virtual sandbox, written in Arbace all the way down to the ISA,
  in the way of Go and TamaGo: /dev/kvm on amd64 and Hypervisor.framework on arm64.

The derivation is done once, by hand, and recorded as it goes ([journal](doc/JOURNAL.md)), so it
can be repeated. Prior art: [Clojure](https://github.com/clojure/clojure),
[jdk26u](https://github.com/openjdk/jdk26u), [Go](https://github.com/golang/go),
[TamaGo](https://github.com/usbarmory/tamago), [Joker](https://github.com/candid82/joker).

## Arbace for Java 26

The branch `arbace-for-java-26` preserves the final state of Arbace on the JVM, binary compatible
with the Java ecosystem, before main leaves the JVM behind. Its releases are tags:
`arbace-for-java-26-v1` (the freeze), `-v2` (a fix), `-v3` (for the server side: a runtime
image of `java.base` and `jdk.unsupported` only, and an Alpine package, `arbace-java26`, with
the commands `arbace` and `arb`) and `-v4` (`reduce` without an init over an
`IReduceInit`-only reducible, `bean`'s key order). [doc/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/FREEZE.md)
and [doc/ALPINE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/ALPINE.md) on
the branch describe what it holds, how to build, run and verify it, and its known limits.

Main no longer builds the JVM Arbace (since standalone S4, the milestone `arbace-seed-is-go-v1`). The
frozen jar of `arbace-for-java-26-v4`, built from its tag and pinned by hash, stays main's
reference for the Go build's differential tests ([doc/go/ORACLE.md](doc/go/ORACLE.md)) and the
JVM's records (j2g's edge, the data generators). repr/java's Java front end is javac itself, held
as Java forms and run by Arbace (javac on Arbace, a tool executable built from them): since the
milestone JN6 (2026-10-21) nothing of repr/java starts a JVM
([doc/classes/JAVA-FRONTEND-SURVEY.md](doc/classes/JAVA-FRONTEND-SURVEY.md)).

## Arbace for Golang

The branch `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`, 2026-10-11) holds Arbace as one
static executable for Linux, amd64 and arm64, with no JVM ([doc/go/B1-PLAN.md](doc/go/B1-PLAN.md),
milestone B1a): j2g (c2g on the branch) translates Arbace's runtime and the part of jdk26u it needs (`java.base`'s
collections, concurrency, time, text, regex, files, sockets, processes and security basics,
among others) into Go, built with TamaGo's go1.27.1; the evaluator analyses forms and runs them
as compiled closures; the core namespaces are prepared at build time, so it starts in about 0.2
s. On amd64 it is about 66 MB, passes Clojure's test suite as far as it runs (on its test build,
with the suite's Java fixtures: 76 namespaces, 20,506 of 20,520 assertions) and matches the JVM
on 22,232 of the oracle's 22,278 cases (42 of the rest need locale data beyond root and English,
which the default build leaves out); it leaves out `gen-class`, XML, Java serialization and
Swing. [doc/go/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/go/FREEZE.md)
on the branch describes what it holds, how to build, run and verify it, and its known limits.
Main builds it from its own seed (next section).

## The seed: Arbace builds itself

Since standalone S4 (2026-10-12; the milestone `arbace-seed-is-go-v1`, 2026-10-13) the bootstrap needs no JVM
([doc/STANDALONE-NOTES.md](doc/STANDALONE-NOTES.md), [doc/STANDALONE-SURVEY.md](doc/STANDALONE-SURVEY.md)).
Its seed is Arbace's own Go program as Arbace printed it, `seed/arbace-seed-go.tar.xz` (5.4 MB
as xz, 61 MB of Go source, pinned by its SHA-256; the Go toolchain that builds it, TamaGo's go1.27.1,
pinned by the hash of its sources). Go's toolchain alone builds it into stage 0, an Arbace
executable; stage 0 runs j2g and repr/go on Arbace's sources and prints and builds the program
again; that program does the same, and the third generation is the second byte for byte (the
fixpoint). The seed is that fixpoint's printed source, so the executable built from it rebuilds
itself exactly (`bin/seed --verify`). The JDK's classes Arbace needs are committed as class
forms (`jdk/`), converted once by repr/java. Since standalone S6 the program's namespaces are compiled
to Go (`ARBACE_AOT=1`, the default; `ARBACE_AOT=0` evaluates them from an image, as before).

```sh
bin/seed --check && bin/seed --build    # stage 0 from the seed: target/seed/amd64/arbace
bin/arbace-go --build --pgo             # target/arbace-go/{amd64,arm64}/arbace, built by stage 0
bin/arbace-go                           # a REPL (with rlwrap on a terminal); also -e '(+ 1 2)', a script
bin/arbace-go --fixpoint                # G1 by stage 0, G2 by G1, G3 by G2: G2 = G3
bin/seed --verify                       # the seed printed again from its commit, and identical
```

The same through `make` (`make help` lists every target, each one a script of `bin/`):
`make stage0`, `make build`, `make repl`, `make fixpoint`, `make seed-verify`, `make gate`.

## Arbace in the box

The tags `arbace-box-amd64-v1` and `arbace-box-arm64-v1` (2026-10-12, on main) mark the same
program built with TamaGo (`GOOS=tamago`) as the guest of a small monitor on /dev/kvm, with no
operating system inside: go-whim's monitor vendored as Go forms in `box/`, TamaGo's runtime and
standard library as Go forms in `tamago/` (one tree for amd64 and arm64), each guest built from
them byte-identical to one built from TamaGo's own sources. The REPL, `-e`, `-m` and scripts run
on one to four vCPUs; files live in memory; a content-addressed store (`arbace.box`, also natively)
and TCP streams proxied by the launcher under a policy are its only ways out
([doc/go/BOX-NOTES.md](doc/go/BOX-NOTES.md), [doc/go/BOX-SURVEY.md](doc/go/BOX-SURVEY.md)). amd64
runs on a Linux host with /dev/kvm; arm64 on KVM/arm64, checked here in an emulated aarch64 VM
(`bin/box-arm64-vm check`). The Mac's Hypervisor.framework has a launcher (purego vendored as Go
forms) and a bundle with a checklist (`bin/box-mac-bundle`), which passed on an M2 Max.

The tag `arbace-inside-itself-v1` (2026-10-15, on main) marks the box rebuilding Arbace inside
itself: a guest runs j2g, repr/go's printing, the plan and Go's compiler and linker as spawned tool
guests over the store, and the image it builds is the host's byte for byte, on amd64, in the
emulated arm64 VM and on the Mac (`bin/box-rebuild`; [doc/go/BOX-NOTES.md](doc/go/BOX-NOTES.md),
"S8").

```sh
bin/arbace-go --build && bin/arbace-go --build --box   # target/arbace-go/box-amd64/arbace-box
target/arbace-go/box-amd64/arbace-box                  # a REPL in the box (rlwrap on a terminal)
B=target/arbace-go/box-amd64/arbace-box
$B --store st put hello.ae                             # prints the blob's hash
$B --store st ref hello HASH; $B --store st ls         # a ref names it
ARBACE_BOX_STORE=st $B ref:hello                       # run the script from the store
ARBACE_BOX_NET=loopback $B                             # with a network: loopback only
ARBACE_BOX_ARCHES=amd64,arm64 bin/arbace-go --build --box && bin/box-arm64-vm check
```

## Three crops: read, eval, AOT and print

The tag `arbace-reaps-3-crops-v1` (2026-10-27, on main) marks REAP for Clojure, Java 26 and Go 1.27.1:
each language read into Arbace's forms, evaluated at the REPL, compiled ahead of time into the static
executable, and printed back to its source, checked against the language's own reference (Clojure's suite,
jdk26u's jtreg tests and the JDK's runs, `$GOROOT/test` against gc). `bin/reap` prints the matrix; none of
its 12 cells fails, and every case that does not pass is in a recorded class
([doc/FULL-SUPPORT-SURVEY.md](doc/FULL-SUPPORT-SURVEY.md), "REAP v1's divergences").

## Go forms at the REPL

The tag `arbace-interprets-go1.27.1-v1` (2026-10-20, on main) marks goi, Arbace's own interpreter
of Go forms, complete at the REPL for Go 1.27.1 ([doc/go/GOI-SPEC.md](doc/go/GOI-SPEC.md)). Go is
written as forms beside Arbace's own code, type-checked as go/types does, and run by goi:
functions, methods, interfaces, goroutines and channels, generics (a generic function's var
infers its type arguments at each call), and the standard library, partly compiled into the
executable and partly interpreted from its own Go forms. Interpreted named types are real runtime
types to compiled code and to reflect. Go values and Arbace values cross in both directions;
a Go panic is an Arbace exception.

Over `$GOROOT/test`, run against gc, 1,471 of its 1,837 programs pass, none fail. The other 366
fall into recorded classes, 358 of them by design: programs that are not Go forms (syntax errors,
assembly, cgo), gc's own diagnostics (optimizer flags, `-race`, its later phases), and goi's
frames in traces, timing and speed (`bin/goi divergences`; GOI-SPEC, "v1's divergences"). goi
passes the same check in the box: on amd64, and on arm64 in the emulated VM.

```sh
target/arbace-go/amd64/arbace
user=> (ns go.demo (:require [arbace.go :as go]))
go.demo=> (go/import [strings "strings"] [fmt "fmt"])
go.demo=> (go/func Greet ^string [^string name] (+ "hello, " (strings/ToUpper name)))
go.demo=> (Greet "go")
"hello, GO"
go.demo=> (go/type Point (struct ^int X ^int Y))
go.demo=> (go/method String ^string [^Point p] (fmt/Sprintf "(%d,%d)" (.-X p) (.-Y p)))
go.demo=> (go/eval (fmt/Println (lit Point 1 2)))
(1,2)
bin/goi check          # $GOROOT/test's programs interpreted against gc
bin/goi divergences    # every program that does not pass, in its recorded class
```

## Clojure code on Arbace

repr/clojure translates Clojure source into Arbace's: the names of the Clojure Arbace vendors
(`clojure.core` → `arbace.core`, `clojure.lang` → `arbace.lang`, ...), `.clj` and `.cljc` files
into `.ae` files (a `.cljc` file's reader conditionals resolved for `:clj`), and the source
changes the dialects' few divergences call for, by a table of rules; layout and comments are
kept ([doc/REPR-CLOJURE.md](doc/REPR-CLOJURE.md)). Clojure's test suite and its libraries run on Arbace through
it.

```sh
bin/repr-clojure read -o lib/ae path/to/library/src          # .clj/.cljc -> .ae, into lib/ae
ARBACE_PATH=lib/ae bin/arbace-go -e "(require 'my.lib)"
```

The translators are c2j (Arbace's namespaces → Java forms: the back end, derived from Clojure's
compiler) and j2g (Java forms → Go forms), a chain that compiles Arbace to Go for the
executable. The bridges between a language's text and its forms are the `repr` family, one
command each with the verbs `read` and `print`: repr/java (Java → Java forms, `bin/repr-java`),
repr/go (Go → Go forms, and back, `bin/repr-go`) and repr/clojure (Clojure → Arbace, and back,
`bin/repr-clojure`); the build of a program held as Go forms is `bin/build-go` (`arbace.build.go`).
From 2026-10-11 to 2026-10-17 the bridges were j2a and g2a (the build `bin/g2a build`), j2g
was a2g (Java forms were "class forms"), repr/clojure was c2a (`bin/c2a`) and c2j the namespace
`arbace.aot`; and until 2026-10-11 j2c, g2c and c2g, as the frozen branches, the journals and the
dated records name them (doc/NAMING-SURVEY.md).

## Arbace's version

Arbace names the commit it was built from: the REPL's banner says `Arbace <git describe>
(<short commit>)`, and `*arbace-version*` holds `{:describe ... :commit ... :dirty ...}`
(`(arbace-version)` the describe); `*clojure-version*` stays 1.13 for the libraries that test
it.

```sh
bin/arbace-go -e '(prn *arbace-version*)'
```

In a checkout it is `git describe --tags --always --dirty` at build time (the Go executable's
link); in a tarball made by `git archive` (GitHub's archives) the file `VERSION`, which `git
archive` fills with the commit and its describe. Stage 0 carries the version of the commit its
seed was printed from (the seed's `SEED.edn`).

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): where the work stands and what comes next
- [doc/go/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/go/FREEZE.md)
  (on `arbace-for-golang`): Arbace for Golang: build, run, verify, the frozen branch
- [doc/JOURNAL.md](doc/JOURNAL.md): every decision and action, append-only (since 2026-10-27;
  the earlier history at the tags `arbace-reaps-3-crops-v1`, `arbace-interprets-go1.27.1-v1` and
  `arbace-inside-itself-v1` and on the frozen branches)
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the Java forms
- [doc/go/B1-PLAN.md](doc/go/B1-PLAN.md): the Go executable's plan; [doc/go/J2G-SPEC.md](doc/go/J2G-SPEC.md),
  its translation; [doc/go/EXEC-NOTES.md](doc/go/EXEC-NOTES.md), the executable;
  [doc/go/JAVA-BASE.md](doc/go/JAVA-BASE.md), how much of `java.base` it implements;
  [doc/go/SPEED-NOTES.md](doc/go/SPEED-NOTES.md), its speed; [doc/go/BOX-SURVEY.md](doc/go/BOX-SURVEY.md)
  and [doc/go/BOX-NOTES.md](doc/go/BOX-NOTES.md), the box (B1b); the rest of `doc/go/`, the notes of each part
- [doc/REPR-CLOJURE.md](doc/REPR-CLOJURE.md): repr/clojure, Clojure source translated into Arbace's
- [doc/C23-SURVEY.md](doc/C23-SURVEY.md): the survey for C23 at parity with the three crops (C forms, repr/c, ci, AOT; proposals CS1-CS24)
- [doc/G2C-SURVEY.md](doc/G2C-SURVEY.md): the survey for repr/go (then g2c, from 2026-10-11 to 2026-10-17 g2a), Go as Arbace forms
- [doc/STANDALONE-SURVEY.md](doc/STANDALONE-SURVEY.md) and [doc/STANDALONE-NOTES.md](doc/STANDALONE-NOTES.md):
  the standalone Arbace, the Go-hosted bootstrap and the seed
- [doc/VENDOR-NOTES.md](doc/VENDOR-NOTES.md): how Clojure became Arbace, and every change since
- [doc/BENCHMARKS.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/BENCHMARKS.md)
  (on the branch `arbace-for-java-26`): Arbace on the JVM against Clojure 1.12
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and its origin
- [LICENSE.md](LICENSE.md): the Eclipse Public License 1.0, like Clojure, and the licenses of
  all vendored and translated code (ASM's BSD-3-Clause, the JDK's GPLv2 with the Classpath
  Exception, Unicode's data license, and the rest)
