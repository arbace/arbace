# Arbace

Arbace is a Clojure derivative written entirely in Clojure.

- **Extended to represent its own implementation.** Clojure gained the class forms
  ([spec](doc/classes/SPEC.md)): every construct of a Java class file as idiomatic Clojure
  (`defclass`, `method`, `switch`, `label`, ...). With them, and the Java → class forms converter
  j2c, Clojure's Java half became Clojure too. Arbace is that vendored Clojure, all `.clj`,
  bootstrapping itself in stages that rebuild each other byte for byte and pass Clojure's whole
  test suite.
- **Modernized for Java 26.** `invokedynamic` call sites for reflection, keywords and `str`, the
  JDK's AOT cache (a launch in about 0.2 s), a self-contained `jlink` image, an opt-in
  virtual-thread executor, and every class checked by the JDK's verifier
  ([benchmarks](doc/BENCHMARKS.md)).
- **Evolving to represent its runtime too, down to the bare metal.** Arbace is leaving the JVM
  and the `.class` format behind: a standalone ecosystem whose language is Clojure plus forms for
  Java (kept usable at the source level through j2c) and later Go. Its runtime is already a
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
`arbace-for-java-26-v1` (the freeze), `-v2` (a fix) and `-v3` (for the server side: a runtime
image of `java.base` and `jdk.unsupported` only, and an Alpine package, `arbace-java26`, with
the commands `arbace` and `arb`). [doc/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/FREEZE.md)
and [doc/ALPINE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/ALPINE.md) on
the branch describe what it holds, how to build, run and verify it, and its known limits.

Main still builds the JVM Arbace, as the bootstrap and the reference the Go build is checked
against:

```sh
bin/build-arbace         # bootstrap into target/: the stages, target/arbace.jar and its AOT cache
bin/arbace-j             # a REPL (with rlwrap on a terminal); also -e '(+ 1 2)', a script
bin/arbace-image         # target/arbace-image: a self-contained jlink image (java.base, jdk.unsupported)
```

## The Go executable

Arbace as one static executable for Linux, amd64 and arm64, with no JVM
([doc/go/B1-PLAN.md](doc/go/B1-PLAN.md)): c2g translates Arbace's runtime and the part of
jdk26u it needs (`java.base`'s collections, concurrency, time, regex, files and sockets, among
others) into Go, built with go1.27.1; the evaluator analyses forms and runs them as
compiled closures; the core namespaces are prepared at build time, so it starts in about 0.2 s.
On amd64 it is about 62 MB, passes Clojure's test suite as far as it runs (19,628 of 19,632
assertions) and matches the JVM on 99.96% of the oracle's 21,800 forms; it leaves out
`gen-class`, XML, Java serialization and Swing. It is to be frozen as the branch
`arbace-for-golang` (tag `arbace-for-go1.27.1-v1`).

```sh
bin/jrt-convert          # the JDK sources jrt needs, converted to class forms (after bin/build-arbace)
bin/arbace-go --build    # target/arbace-go/{amd64,arm64}/arbace
bin/arbace-go            # a REPL (with rlwrap on a terminal); also -e '(+ 1 2)', a script
```

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): where the work stands and what comes next
- [doc/JOURNAL.md](doc/JOURNAL.md): every decision and action, append-only
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the class forms
- [doc/go/B1-PLAN.md](doc/go/B1-PLAN.md): the Go executable's plan; [doc/go/C2G-SPEC.md](doc/go/C2G-SPEC.md),
  its translation; [doc/go/EXEC-NOTES.md](doc/go/EXEC-NOTES.md), the executable;
  [doc/go/JAVA-BASE.md](doc/go/JAVA-BASE.md), how much of `java.base` it implements
- [doc/VENDOR-NOTES.md](doc/VENDOR-NOTES.md): how Clojure became Arbace, and every change since
- [doc/BENCHMARKS.md](doc/BENCHMARKS.md): Arbace against Clojure 1.12
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and its origin
- [LICENSE.md](LICENSE.md): the Eclipse Public License 1.0, like Clojure, and the licenses of
  all vendored and translated code (ASM's BSD-3-Clause, the JDK's GPLv2 with the Classpath
  Exception, Unicode's data license, and the rest)
