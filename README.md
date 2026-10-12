# Arbace

Arbace is a Clojure derivative written entirely in Clojure.

- **Extended to represent its own implementation.** Clojure gained the class forms
  ([spec](doc/classes/SPEC.md)): every construct of a Java class file as idiomatic Clojure
  (`defclass`, `method`, `switch`, `label`, ...). With them, and the Java → class forms converter
  j2a, Clojure's Java half became Clojure too. Arbace is that vendored Clojure, all `.ae`,
  bootstrapping itself in stages that rebuild each other byte for byte and pass Clojure's whole
  test suite.
- **Modernized for Java 26.** `invokedynamic` call sites for reflection, keywords and `str`, the
  JDK's AOT cache (a launch in about 0.2 s), a self-contained `jlink` image, an opt-in
  virtual-thread executor, and every class checked by the JDK's verifier (benchmarks:
  [BENCHMARKS.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/BENCHMARKS.md),
  on the branch `arbace-for-java-26`).
- **Evolving to represent its runtime too, down to the bare metal.** Arbace is leaving the JVM
  and the `.class` format behind: a standalone ecosystem whose language is Clojure plus forms for
  Java (kept usable at the source level through j2a) and later Go. Its runtime is already a
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

Main still builds the JVM Arbace, as the bootstrap and the reference the Go build is checked
against:

```sh
bin/build-arbace         # bootstrap into target/: the stages, target/arbace.jar and its AOT cache
bin/arbace-j             # a REPL (with rlwrap on a terminal); also -e '(+ 1 2)', a script
bin/arbace-image         # target/arbace-image: a self-contained jlink image (java.base, jdk.unsupported)
```

## Arbace for Golang

The branch `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`, 2026-10-11) holds Arbace as one
static executable for Linux, amd64 and arm64, with no JVM ([doc/go/B1-PLAN.md](doc/go/B1-PLAN.md),
milestone B1a): a2g (c2g on the branch) translates Arbace's runtime and the part of jdk26u it needs (`java.base`'s
collections, concurrency, time, text, regex, files, sockets, processes and security basics,
among others) into Go, built with TamaGo's go1.27.1; the evaluator analyses forms and runs them
as compiled closures; the core namespaces are prepared at build time, so it starts in about 0.2
s. On amd64 it is about 66 MB, passes Clojure's test suite as far as it runs (on its test build,
with the suite's Java fixtures: 76 namespaces, 20,506 of 20,520 assertions) and matches the JVM
on 22,232 of the oracle's 22,278 cases (42 of the rest need locale data beyond root and English,
which the default build leaves out); it leaves out `gen-class`, XML, Java serialization and
Swing. [doc/go/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/go/FREEZE.md)
on the branch describes what it holds, how to build, run and verify it, and its known limits.
Main still builds it: the commands below, now leading to what comes next.

```sh
bin/build-arbace               # the JVM Arbace first: the bootstrap and the tools run on it
bin/jrt-convert                # the JDK sources jrt needs, converted to class forms
bin/arbace-go --build --pgo    # target/arbace-go/{amd64,arm64}/arbace, profile-guided
bin/arbace-go                  # a REPL (with rlwrap on a terminal); also -e '(+ 1 2)', a script
```

## Arbace in the box

The tag `arbace-box-amd64-v1` (2026-10-12, on main) marks the same program built with TamaGo
(`GOOS=tamago`) as the guest of a small monitor on /dev/kvm, with no operating system inside:
go-whim's monitor vendored as Go forms in `box/`, TamaGo's runtime and standard library as Go
forms in `tamago/`, the guest built from them byte-identical to one built from TamaGo's own
sources. The REPL, `-e`, `-m` and scripts run on one to four vCPUs; files live in memory; a
content-addressed store (`arbace.box`, also natively and on the JVM) and TCP streams proxied by
the launcher under a policy are its only ways out ([doc/go/BOX-NOTES.md](doc/go/BOX-NOTES.md),
[doc/go/BOX-SURVEY.md](doc/go/BOX-SURVEY.md)). amd64 only, on a Linux host with /dev/kvm; arm64
and the Mac's Hypervisor.framework are the next milestone.

```sh
bin/arbace-go --build && bin/arbace-go --build --box   # target/arbace-go/box-amd64/arbace-box
target/arbace-go/box-amd64/arbace-box                  # a REPL in the box (rlwrap on a terminal)
B=target/arbace-go/box-amd64/arbace-box
$B --store st put hello.ae                             # prints the blob's hash
$B --store st ref hello HASH; $B --store st ls         # a ref names it
ARBACE_BOX_STORE=st $B ref:hello                       # run the script from the store
ARBACE_BOX_NET=loopback $B                             # with a network: loopback only
```

## Clojure code on Arbace

c2a translates Clojure source into Arbace's: the names of the Clojure Arbace vendors
(`clojure.core` → `arbace.core`, `clojure.lang` → `arbace.lang`, ...), `.clj` and `.cljc` files
into `.ae` files (a `.cljc` file's reader conditionals resolved for `:clj`), and the source
changes the dialects' few divergences call for, by a table of rules; layout and comments are
kept ([doc/C2A.md](doc/C2A.md)). Clojure's test suite and its libraries run on Arbace through
it.

```sh
bin/c2a -o lib/ae path/to/library/src                       # .clj/.cljc -> .ae, into lib/ae
ARBACE_CLASSPATH=lib/ae bin/arbace-j -e "(require 'my.lib)"  # the JVM build
ARBACE_PATH=lib/ae bin/arbace-go -e "(require 'my.lib)"      # the Go build
ARBACE_PATH=lib/ae bin/arbace-j -e "(require 'my.lib)"       # the JVM build reads it too
```

The translators name Arbace (`a`) as their target or source: c2a (Clojure → Arbace), j2a
(Java → class forms), g2a (Go → Go forms, and back) and a2g (Arbace's class forms → Go, for
the Go executable). Until 2026-10-11 the last three were j2c, g2c and c2g, as the frozen branches,
the journals and the dated records name them.

## Arbace's version

Arbace names the commit it was built from: the REPL's banner says `Arbace <git describe>
(<short commit>)`, and `*arbace-version*` holds `{:describe ... :commit ... :dirty ...}`
(`(arbace-version)` the describe), on both builds; `*clojure-version*` stays 1.13 for the
libraries that test it.

```sh
bin/arbace-j -e '(prn *arbace-version*)'   # or bin/arbace-go -e ...; java -cp target/stage2:. arbace.lang.Main -e ...
unzip -p target/arbace.jar arbace/version.edn
```

In a checkout it is `git describe --tags --always --dirty` at build time (the jar's resource
`arbace/version.edn`, the Go executable's link); in a tarball made by `git archive` (GitHub's
archives) the file `VERSION`, which `git archive` fills with the commit and its describe.

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): where the work stands and what comes next
- [doc/go/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/go/FREEZE.md)
  (on `arbace-for-golang`): Arbace for Golang: build, run, verify, the frozen branch
- [doc/JOURNAL.md](doc/JOURNAL.md): every decision and action, append-only
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the class forms
- [doc/go/B1-PLAN.md](doc/go/B1-PLAN.md): the Go executable's plan; [doc/go/A2G-SPEC.md](doc/go/A2G-SPEC.md),
  its translation; [doc/go/EXEC-NOTES.md](doc/go/EXEC-NOTES.md), the executable;
  [doc/go/JAVA-BASE.md](doc/go/JAVA-BASE.md), how much of `java.base` it implements;
  [doc/go/SPEED-NOTES.md](doc/go/SPEED-NOTES.md), its speed; [doc/go/BOX-SURVEY.md](doc/go/BOX-SURVEY.md)
  and [doc/go/BOX-NOTES.md](doc/go/BOX-NOTES.md), the box (B1b); the rest of `doc/go/`, the notes of each part
- [doc/C2A.md](doc/C2A.md): c2a, Clojure source translated into Arbace's
- [doc/G2C-SURVEY.md](doc/G2C-SURVEY.md): the survey for g2a (then g2c), Go as Arbace forms
- [doc/VENDOR-NOTES.md](doc/VENDOR-NOTES.md): how Clojure became Arbace, and every change since
- [doc/BENCHMARKS.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/BENCHMARKS.md)
  (on the branch `arbace-for-java-26`): Arbace on the JVM against Clojure 1.12
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and its origin
- [LICENSE.md](LICENSE.md): the Eclipse Public License 1.0, like Clojure, and the licenses of
  all vendored and translated code (ASM's BSD-3-Clause, the JDK's GPLv2 with the Classpath
  Exception, Unicode's data license, and the rest)
