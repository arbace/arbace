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

- Class forms ([spec](doc/classes/SPEC.md)): every construct of a Java classfile written as
  idiomatic Clojure (`defclass`, `field`, `method`, `switch`, `label`, ...). The class forms
  compiler (`arbace/classes/`) compiles them; the converter (`arbace/j2c/`, `bin/j2c`) turns
  Java source into them. Arbace's own Java parts are class forms, and since stage 1 the forms are
  native to Arbace.

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): the current state of the work and what comes next
- [doc/JOURNAL.md](doc/JOURNAL.md): an append-only log of important decisions and actions
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the class forms
- [LICENSE.md](LICENSE.md): Arbace is under the Eclipse Public License 1.0, like Clojure; the
  file also holds the licenses of all vendored code
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and the origin of the vendored Clojure
