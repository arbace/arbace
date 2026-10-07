# Arbace

Arbace is a from-scratch reimplementation of Clojure, written in Clojure.

It is a fresh implementation, not a fork. We study prior art and may vendor parts of it, so in that
sense the result is derived from earlier work. The derivation is done once, by hand, and recorded as
we go so that it could be repeated.

## Prior art

- [clojure/clojure](https://github.com/clojure/clojure): the language (reader, compiler, core library, data structures)
- [openjdk/jdk26u](https://github.com/openjdk/jdk26u): the native runtime (JVM, class libraries)
- [golang/go](https://github.com/golang/go): a slimmer, appealing runtime (scheduler, GC, toolchain and the rest), and later a language to express in Arbace
- [usbarmory/tamago](https://github.com/usbarmory/tamago): Go on bare metal, without an OS
- [candid82/joker](https://github.com/candid82/joker): a Clojure dialect implemented in Go

## Direction

- Now: Arbace is excellent on the modern JVM (Java 26) and binary compatible with it.
- At the break: that state is frozen on the branch `arbace-for-java-26`, which will be the place
  for Arbace on the JVM.
- Later: a standalone ecosystem in the Arbace language. That is Clojure extended with idiomatic
  forms for Java and later Go, with Java source kept usable through j2c transcription. The
  target is a self-sustaining REPL in a virtual sandbox, written in Arbace down to the bare
  metal ISA: /dev/kvm on amd64 (Alpine Linux) and Hypervisor.framework on arm64 (macOS).

## Tools

- Class forms ([spec](doc/classes/SPEC.md)): every construct of a Java classfile written as
  idiomatic Clojure (`defclass`, `field`, `method`, `switch`, `label`, ...). The class forms
  compiler (`arbace/classes/`) compiles them; the converter (`arbace/j2c/`, `bin/j2c`) turns
  Java source into them. Arbace's own Java parts are class forms, and since stage 1 the forms are
  native to Arbace.

## Running

`bin/build-arbace` bootstraps Arbace into `target/` (about 1.5 minutes; see
[doc/VENDOR-NOTES.md](doc/VENDOR-NOTES.md)). Then `bin/arbace` runs it like `clojure.main`:
`bin/arbace` for a REPL, `bin/arbace -e '(+ 1 2)'`, `bin/arbace script.clj`. It runs
`target/arbace.jar` with the JDK AOT cache `target/arbace.aot` when the cache applies (it is tied
to the JDK build and the jar), and from the jar alone otherwise.

`bin/arbace-image` (or `bin/build-arbace --image`) makes `target/arbace-image`, a
self-contained Arbace needing no installed JDK: a JDK runtime image trimmed by `jlink` to the
modules Arbace uses (`java.base`, `java.desktop`, `java.sql`, `jdk.unsupported` and what they
require), the jar, an AOT cache trained by the image's own `java`, and `bin/arbace`. Copy the
directory anywhere and run `arbace-image/bin/arbace`. It takes 131 MB (42 MB as a `.tar.gz`)
against 401 MB for the JDK alone, and launches as fast as `bin/arbace`. Further modules go in
`ARBACE_IMAGE_MODULES` (e.g. `java.net.http`).

`send-off`, `future` and `pmap` run on a cached pool of platform threads, as in Clojure. With
`ARBACE_JAVA_OPTS=-Darbace.virtual-threads=true` they run on virtual threads instead, one per
task, which suits many blocking tasks (10,000 futures sleeping 100 ms: 120 ms against 230 ms,
and 72 platform threads against 5,500). At run time,
`(set-agent-send-off-executor! (arbace.lang.Agent/newVirtualThreadExecutor))` does the same.
Virtual threads are daemon threads: the JVM exits when the main thread ends, without waiting
for running futures or actions (nor for the pool's idle threads, which otherwise keep it
alive for a minute unless `shutdown-agents` is called).

## Documentation

- [doc/AGENDA.md](doc/AGENDA.md): the current state of the work and what comes next
- [doc/JOURNAL.md](doc/JOURNAL.md): an append-only log of important decisions and actions
- [doc/classes/SPEC.md](doc/classes/SPEC.md): the class forms
- [LICENSE.md](LICENSE.md): Arbace is under the Eclipse Public License 1.0, like Clojure; the
  file also holds the licenses of all vendored code
- [doc/ARBACE.md](doc/ARBACE.md): what `arbace/` holds, and the origin of the vendored Clojure
