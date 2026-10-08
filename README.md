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

The branch `arbace-for-java-26` (tag `arbace-for-java-26-v1`) preserves the final state of
Arbace on the JVM, binary compatible with the Java ecosystem, before main leaves the JVM behind.
[doc/FREEZE.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/FREEZE.md) on the branch describes what it holds, how to build, run and verify it, and its
known limits.

```sh
bin/build-arbace         # bootstrap into target/: the stages, target/arbace.jar and its AOT cache
bin/arbace               # a REPL; also bin/arbace -e '(+ 1 2)', bin/arbace script.clj
```

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): where the work stands and what comes next
- [doc/JOURNAL.md](doc/JOURNAL.md): every decision and action, append-only
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the class forms
- [doc/VENDOR-NOTES.md](doc/VENDOR-NOTES.md): how Clojure became Arbace, and every change since
- [doc/BENCHMARKS.md](doc/BENCHMARKS.md): Arbace against Clojure 1.12
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and its origin
- [LICENSE.md](LICENSE.md): Eclipse Public License 1.0, like Clojure, and the licenses of all
  vendored code
