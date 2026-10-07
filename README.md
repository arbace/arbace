# Arbace

Arbace is a from-scratch reimplementation of Clojure, written in Clojure.

It is a fresh implementation, not a fork. We study prior art and may vendor parts of it, so in that
sense the result is derived from earlier work. The derivation is done once, by hand, and recorded as
we go so that it could be repeated.

## Prior art

- [clojure/clojure](https://github.com/clojure/clojure): the language (reader, compiler, core library, data structures)
- [openjdk/jdk26u](https://github.com/openjdk/jdk26u): the native runtime (JVM, class libraries)
- [golang/go](https://github.com/golang/go): a slimmer, appealing runtime (scheduler, GC, toolchain and the rest), though not the Go language itself

## Tools

- [javalisp](doc/javalisp/SPEC.md) (`arbace/javalisp/`) transcribes Java 26 source into
  s-expressions that Clojure reads as data, and back. The round trip keeps javac's tree and
  every line javac records, so the classes compile byte for byte the same.

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): the current state of the work and what comes next
- [doc/JOURNAL.md](doc/JOURNAL.md): an append-only log of important decisions and actions
- [doc/javalisp/SPEC.md](doc/javalisp/SPEC.md): the javalisp forms, layout rules and tool
- [LICENSE.md](LICENSE.md): the licenses of all code in this repository
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and the origin of the vendored Clojure
