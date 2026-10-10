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
- **Evolving to represent its runtime too, down to the bare metal.** Next, Arbace leaves the JVM
  and the `.class` format behind: a standalone ecosystem whose language is Clojure plus forms for
  Java (kept usable at the source level through j2c) and later Go. The goal is a self-sustaining
  REPL in a virtual sandbox, written in Arbace all the way down to the ISA, in the way of Go and
  TamaGo: /dev/kvm on amd64 and Hypervisor.framework on arm64.

The derivation is done once, by hand, and recorded as it goes ([journal](doc/JOURNAL.md)), so it
can be repeated. Prior art: [Clojure](https://github.com/clojure/clojure),
[jdk26u](https://github.com/openjdk/jdk26u), [Go](https://github.com/golang/go),
[TamaGo](https://github.com/usbarmory/tamago), [Joker](https://github.com/candid82/joker).

## Arbace for Java 26

The branch `arbace-for-java-26` preserves the final state of Arbace on the JVM, binary
compatible with the Java ecosystem, before main leaves the JVM behind. Its releases are tags:
`arbace-for-java-26-v1` (the freeze), `-v2` (a fix: comparisons with NaN), `-v3` (for the
server side: the runtime image holds `java.base` and `jdk.unsupported` only, `bean` and `#inst`
need nothing else, and an Alpine package) and `-v4` (fixes: `reduce` without an initial value
over an eduction, `iteration` or a `reify` of `IReduceInit`; `bean`'s keys in name order). Use the newest. [doc/FREEZE.md](doc/FREEZE.md)
describes what the branch holds, how to build, run and verify it, and its known limits.

```sh
bin/build-arbace         # bootstrap into target/: the stages, target/arbace.jar and its AOT cache
bin/arbace               # a REPL; also bin/arbace -e '(+ 1 2)', bin/arbace script.clj
bin/arbace-image         # target/arbace-image: a self-contained jlink image (java.base, jdk.unsupported)
```

**On Alpine Linux** (edge, x86_64 and aarch64): the package `arbace-java26`
([dist/alpine/](dist/alpine/), [doc/ALPINE.md](doc/ALPINE.md)) installs that image, with its own
JDK 26, in `/usr/lib/arbace-java26`, with the commands `arbace` and `arb` (the REPL with line
editing through `rlwrap`, as `clj` is to `clojure`). It needs no Java package. `bin/alpine-package`
builds and tests it from the release tag.

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): where the work stands and what comes next
- [doc/JOURNAL.md](doc/JOURNAL.md): every decision and action, append-only
- [doc/FREEZE.md](doc/FREEZE.md): Arbace on the JVM: build, run, verify, the frozen branch
- [doc/ALPINE.md](doc/ALPINE.md): the Alpine package
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the class forms
- [doc/VENDOR-NOTES.md](doc/VENDOR-NOTES.md): how Clojure became Arbace, and every change since
- [doc/BENCHMARKS.md](doc/BENCHMARKS.md): Arbace against Clojure 1.12
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and its origin
- [LICENSE.md](LICENSE.md): the Eclipse Public License 1.0, like Clojure, and the licenses of
  all vendored code (ASM's BSD-3-Clause, and in the runtime image the JDK's GPLv2 with the
  Classpath Exception and its third-party notices)
