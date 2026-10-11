# Arbace for Golang: the freeze

Arbace on Go is one static executable for Linux, amd64 and arm64, with no JVM: Arbace's runtime
and the part of jdk26u it needs, translated into Go, with an evaluator in place of the JVM's
bytecode ([B1-PLAN.md](B1-PLAN.md), milestone B1a). Main moves on from it to the box (TamaGo,
B1b) and to Arbace's own language and tools. Before that, the state of B1a is frozen on a
well-known branch:

- **branch `arbace-for-golang`**, with the annotated tag **`arbace-for-go1.27.1-v1`** on the same
  commit: Arbace on Go as it was on main at the freeze, built with TamaGo's go1.27.1.

The tag names the pinned Go toolchain; fix releases on the same toolchain are `-v2`, `-v3` and so
on. The branch takes fixes only; new work lands on main.

**Releases on this branch:** `arbace-for-go1.27.1-v1` (the freeze, 2026-10-11). Use the newest
tag.

## What the branch holds

- **The Go executables**, `target/arbace-go/amd64/arbace` and `target/arbace-go/arm64/arbace`,
  static (`CGO_ENABLED=0`: no C code, no libc), built from the branch (they are not committed).
  Each is:
  - **c2g's translation** (`bin/c2g`, `arbace/c2g/`) of Arbace's runtime (`arbace/lang/`, the
    class forms of the vendored Clojure) and of the jdk26u classes `bin/jrt-convert` selects
    (835 files of `java.base` with the default locale set, converted to class forms by j2c), into
    Go forms, printed to Go by g2c's printer and built by gc;
  - **jrt**, the Java runtime under them (`go/arbace/jrt/`, `overlay/`): the VM's edge
    hand-written (UTF-16 strings, objects and monitors, reflection over c2g's member tables,
    threads over goroutines, the host interface to the OS);
  - **the evaluator**: `Compiler`'s analyser, translated, with each method compiled at its first
    call into a tree of closures ([EVAL-NOTES.md](EVAL-NOTES.md),
    [SPEED-NOTES.md](SPEED-NOTES.md));
  - **the image of prepared namespaces**: `arbace.core` and the shipped namespaces analysed at
    build time and linked in, with their sources embedded, so the executable starts in about
    0.2 s and needs no file beside it ([EXEC-NOTES.md](EXEC-NOTES.md));
  - **the class forms at the REPL**: `defclass` and the code forms, analysed by the embedded
    class forms compiler and interpreted ([CLASSFORMS-REPL.md](CLASSFORMS-REPL.md));
  - **rlwrap**: the interactive REPL re-executes itself under `rlwrap` when stdin and stdout are
    terminals and `rlwrap` is on the `PATH` (EXEC-NOTES.md, "Line editing: rlwrap").
- **What works**, beyond the language: the parts B1-PLAN's D6 first left out and later brought
  back: `proxy` (of `Object`, interfaces and a fixed list of classes), the fork-join pool and
  streams, `java.io.File`, the file system and a small `java.nio.file`, sockets and the socket
  REPL (`prepl`, `io-prepl`, `remote-prepl`), processes (`ProcessBuilder`, `Runtime.exec`,
  `arbace.java.shell`, `arbace.java.process`), `java.time` with the zone data,
  `java.util.concurrent`, java.security's basics (digests, HMACs, `SecureRandom`, `UUID`;
  providers SUN and SunJCE), `java.text` (formats, `Collator`, `BreakIterator`, `Bidi`,
  `Currency`) with the root and English locale data by default (`JRT_LOCALES=common` adds 27
  locales, `all` every CLDR bundle), `java.util.regex` ported whole, `\N{name}` and `CANON_EQ`
  included, resources and `URLClassLoader` over directories. Not there: below, "Known limits".
- **The numbers** (2026-10-11, the build with the default locale set, amd64):
  - the executable 66.4 MB (stripped; the symbols with `ARBACE_GO_SYMBOLS=1`); start to `-e nil`
    0.20-0.21 s;
  - Clojure's upstream test suite on the test build (below): 76 namespaces, 777 tests, 20,506 of
    20,520 assertions (11 failures, 3 errors, all recorded), 9 namespaces and 7 tests skipped,
    test.generative 27 of 27 specs (`test/arbace-go-results.edn`; the JVM: 20,750 of 20,750);
  - the oracle ([ORACLE.md](ORACLE.md)): 22,232 of 22,278 cases as on the JVM; the 46
    mismatches are recorded with their reasons in `test/oracle/known-go-amd64.edn`: 42 need
    locale data beyond root and English (`text.clj`, `time.clj`; they pass with
    `JRT_LOCALES=all`), and one each for the provider list (SUN and SunJCE only), a
    `StringBuilder`'s identity hash (HotSpot's, not reproducible), the identity of a boxed NaN
    (Go reuses one, so `Util/equiv` answers true) and Java serialization (not in the build);
  - java.base: 9,807 of its 17,546 exported members (55.9%), 93.0% of those Arbace's runtime
    and REPL namespaces reference ([JAVA-BASE.md](JAVA-BASE.md), `bin/jrt-coverage`).
- **The JVM Arbace**, still on the branch as on main: the bootstrap from the binary seed
  (`bin/build-arbace`; stages 1-3, `target/arbace.jar`), the tools' runtime (`target/stage2`) and
  the reference the Go build is checked against (`bin/arbace-j`).
- **The tools**: c2g (class forms → Go forms, `bin/c2g`, `bin/c2g-check`); g2c (Go ↔ Go forms:
  the helper `tools/godump`, the round trip `bin/g2c roundtrip`, `bin/g2c build`); j2c (Java →
  class forms, `bin/j2c`); `bin/jrt-convert` (jrt's jdk26u sources through j2c, checked
  against javac); `bin/jrt` (jrt's own tests); the differential oracle `bin/oracle` (forms,
  class scripts and regex cases recorded from the JVM); `bin/jrt-coverage`.
- **The records**: `doc/JOURNAL.md` (from the JVM freeze to this one; the history before it is
  on `arbace-for-java-26`), `doc/AGENDA.md`, and in `doc/go/`: [C2G-SPEC.md](C2G-SPEC.md) (the
  translation, every accepted amendment, the deviations §3.2), [B1-PLAN.md](B1-PLAN.md) (the
  plan, decisions D1-D7), [EXEC-NOTES.md](EXEC-NOTES.md), [EVAL-NOTES.md](EVAL-NOTES.md),
  [JRT-NOTES.md](JRT-NOTES.md), [SPEED-NOTES.md](SPEED-NOTES.md),
  [JAVA-BASE.md](JAVA-BASE.md), [BOX-SURVEY.md](BOX-SURVEY.md) (the box, for after the freeze);
  `LICENSE.md`. Its entry "The Go build's executables" states the licences an executable
  combines (Arbace's EPL-1.0 code, jdk26u's translated code under GPLv2 with the Classpath
  Exception, the Unicode, ICU and CLDR data under the Unicode License V3, the time zone data,
  Go's runtime and standard library under its BSD 3-Clause License) and what a distribution of
  one must carry: `LICENSE.md` itself (the licence texts and notices), jdk26u's corresponding
  source (`baf63fb`) and this repository's revision, with the executable or by a written
  offer, and a statement that Arbace's source is available and how to obtain it.

## Build, run, verify

Needs, on Linux (Alpine edge here; amd64 hosts the build and the checks):

- **A JDK 26** (`java`, `javac`, `jar`, `jlink` on the `PATH`) for the bootstrap and the tools.
  `bin/jrt-convert` checks the converted classes against javac's and attributes the sources
  with `--patch-module` against the running JDK, so that JDK is best built from the pinned
  jdk26u (here 26.0.2.1 from `/root/jdk26u`). Its generators run on `GEN_JDK` (default the
  JDK build's boot JDK, else `/usr/lib/jvm/java-25-openjdk`; JRT-SOURCES.md, "The generated
  sources").
- **jdk26u's checkout** at `baf63fb` (https://github.com/openjdk/jdk26u,
  `baf63fbe42b8758448fee570a9d9bb914272d259`), for `bin/jrt-convert`: `JDK` (default
  `/root/jdk26u`). Its `build/*/support/gensrc` and `jdk/modules`, when present, are what the
  generated sources and data are compared with byte for byte.
- **TamaGo's go1.27.1** (https://github.com/usbarmory/tamago-go, `VERSION` go1.27.1,
  2026-08-28): `G2C_GOROOT` (default `/root/tamago-go`), read by `bin/g2c`, `bin/arbace-go`,
  `bin/jrt` and the gate. It builds `GOOS=linux` as upstream go1.27.1 does (ROUNDTRIP.md, "The
  corpus and the configuration"); Alpine's go1.27.1 (`G2C_GOROOT=/usr/lib/go`) builds too, into
  other bytes. The helper refuses any go command but go1.27.1.
- **qemu-aarch64** (`/usr/bin/qemu-aarch64`, QEMU's user-mode emulation) to run and check the
  arm64 executable; **python3** and **rlwrap** for the REPL's checks on a pseudo-terminal
  (`test/arbace-go-rlwrap.py`; skipped without them); bash, git, curl.
- **Memory and cores**: c2g's JVM takes 12 GB (`C2G_JVM_OPTS`), `bin/jrt-convert` one 8 GB j2c
  JVM then four 3 GB check JVMs; the suites run many JVMs or processes at once (the essential
  gate peaked at 25 GB when it built the executable, EXEC-NOTES.md). The checks were run on
  64 cores.

In order, from a fresh checkout of the tag:

```sh
bin/build-arbace                 # the bootstrap: stages 1-3, the verifier, target/arbace.jar
bin/jrt-convert                  # jrt's jdk26u sources into class forms, .tmp/jrt
                                 # (JRT_LOCALES=none|common|all|tags; default none)
bin/arbace-go --build --pgo      # target/arbace-go/{amd64,arm64}/arbace, profile-guided (the
                                 # release build); without --pgo the ordinary build
bin/arbace-go --build-tests      # target/arbace-go/<arch>/arbace-tests: the suite's Java
                                 # fixtures translated in (not shipped)
bin/arbace-go --smoke            # -e, a script, the REPL, an error's status; both
                                 # architectures (ARBACE_GO_ARCHES), arm64 under qemu-aarch64
CLOJURE_TESTS_GO=target/arbace-go/amd64/arbace-tests bin/clojure-tests -j 12
                                 # Clojure's suite against test/arbace-go-results.edn
bin/oracle check 'target/arbace-go/amd64/arbace -' --timeout 900 \
  --expected test/oracle/known-go-amd64.edn   # the oracle against its known mismatches
```

`ARBACE_GO_ARCHES=amd64` limits the build and the smoke test to amd64 (the PGO training run
needs the host's executable). The build takes about 5 minutes per pass on the development
machine; `--pgo` adds a training run of `test/arbace-go-pgo.clj` (about 15 s) and a second
build. On arm64 the suite runs with `CLOJURE_TESTS_GO="qemu-aarch64
target/arbace-go/arm64/arbace-tests"`, in hours.

**The gate**: `bin/gate` (the seed's hash, the bootstrap, then concurrently Clojure's suite on
stage 2, `bin/class-forms-tests` and the Go smoke test on a cached executable; about 4 minutes,
about 7 when the Go executable must be built again) and `bin/gate --full` (adding the suite on
stage 1, `bin/j2c-check --suite`, the g2c round trip for tamago/amd64 and linux/amd64, and the
Go chain on amd64: `bin/jrt-convert`, the build, the smoke test, the test build, the suite on
it and the oracle; about 21 minutes). Logs in `.tmp/gate/`.

**Running.**

```sh
bin/arbace-go                    # a REPL (under rlwrap on a terminal); built first if missing
bin/arbace-go -e '(+ 1 2)'       # any arbace.main option: -e, -m NS, a script, - for stdin
ARBACE_GO_ARCH=arm64 bin/arbace-go   # the arm64 executable, under qemu-aarch64
target/arbace-go/amd64/arbace script.clj   # the executable alone: copy it anywhere
```

- `ARBACE_RLWRAP=off` keeps the REPL out of rlwrap (`wrapped` marks the re-executed child);
  the history is `~/.arbace_history`.
- `JAVA_TOOL_OPTIONS` gives system properties (`-Dname=value`, split as HotSpot splits it), as
  the JVM reads it: e.g. `-Duser.language=fr -Duser.country=FR` (with the locale data built in),
  or the socket server, `JAVA_TOOL_OPTIONS='-Darbace.server.repl="{:port 5555 :accept
  arbace.core.server/repl}"'`.
- `ARBACE_PATH` (directories separated by `:`) is where `RT.load` and the resources look after
  what the executable embeds: the JVM's class path for namespaces and resources.

## Known limits

- **Out for good** (the user's decision, 2026-10-10): XML (SAX, `arbace.xml`), `gen-class`,
  Java serialization, Swing/AWT (`java.desktop`, `arbace.inspector`), and loading classes from
  jars or class files: the closed world is the classes translated in plus those the evaluator
  defines (`deftype`, `defrecord`, `reify`, `proxy`, `definterface`, `defclass`).
- **Not in the build**: the rest of java.base (JAVA-BASE.md): `java.nio`'s channels,
  `java.lang.invoke`, `java.lang.classfile`, most of
  `java.lang.foreign`; java.security beyond SUN and SunJCE (signatures, ciphers, key stores,
  TLS); `javax.*` mostly; locale data beyond root and English unless built with `JRT_LOCALES`.
- **Deviations from the JVM** (C2G-SPEC §3.2, V1-V16): no recoverable `StackOverflowError`
  (the evaluator counts its own depth) or `OutOfMemoryError`; identity hashes and the order of
  identity-keyed maps differ; one lambda class per functional interface; stack traces show
  translated frames; a static initializer running on another thread is not waited for by
  instance code; a racy non-volatile field of interface type can tear; no `finalize`, soft
  references never cleared by the collector; thread priorities, groups and daemon status
  without effect; implicit `NullPointerException`s without the helpful message; monitors about
  14 ns uncontended; providers SUN and SunJCE only; the locale data as selected; no module
  encapsulation of resources.
- **Speed**: evaluated, not compiled. On compute the executable is about 40-55 times slower than
  the JVM's compiled code (step 7b's benchmarks: 6 to 500 times, geometric mean 54x;
  SPEED-NOTES.md); start-up is alike (about 0.2 s). AOT of namespaces to Go is not scheduled.
- **The suite's 11 failures and 3 errors** (`test/arbace-go-results.edn`): `compilation` 10
  (primitive fns evaluated boxed: no "Cannot coerce" or recur-mismatch diagnostics; a few line
  numbers in stack traces) and `java-interop` 1 failure and 3 errors (a fn adapted to an
  interface `definterface` makes at run time; a proxy of `java.util.BitSet`, not one of the
  proxy superclasses). Skipped: the namespaces `clojure-xml`, `genclass`, `genclass.examples`,
  `serialization`, `java.javadoc`, `metadata`, `repl.deps` (out of the build) and two upstream
  fixtures; the tests `clj-1208` (AOT class files), `test-bean` and `test-dot` (`java.awt`),
  `test-proxy-method-order` (ASM on class files), `test-proxy-non-serializable`,
  `invoke-checks-accessibility` (`javax.xml.stream`) and `seq-and-transducer` (200,000
  test.check trials, beyond the 900 s a namespace may take).
- **arm64 is checked only under emulation** (`qemu-aarch64`), and by `bin/gate --full` not at
  all: its smoke test and, by hand, the suite and the oracle.
- `bin/arbace-go` does not notice an executable stale against edited sources: build again.

## Making the freeze

B1-PLAN's step 8, as done for `arbace-for-go1.27.1-v1`; to repeat for a fix release, the same
from the branch:

1. On main, clean and equal to `origin/main`: `bin/gate --full` passes on the commit.
2. The release executables: `bin/jrt-convert`, then `bin/arbace-go --build --pgo` (amd64 and
   arm64), `bin/arbace-go --smoke` on both, Clojure's suite on the amd64 test build and the
   oracle against its known mismatches (the commands above), with the numbers recorded.
3. `bin/jrt-coverage` regenerates `doc/go/java-base-coverage.edn`, and `doc/go/JAVA-BASE.md`
   takes its numbers.
4. The READMEs diverge: the freeze commit's README describes the frozen executables (this
   document, how to build and run them, the releases); after the tag, main's README gains a
   section "Arbace for Golang" after "Arbace for Java 26", naming the branch and its tags and
   linking this document on the branch
   (https://github.com/arbace/arbace/blob/arbace-for-golang/doc/go/FREEZE.md).
5. The branch and the tag on that commit, pushed:

   ```sh
   git branch arbace-for-golang main
   git tag -a arbace-for-go1.27.1-v1 -m 'Arbace for Golang v1: ...' arbace-for-golang
   git push origin arbace-for-golang arbace-for-go1.27.1-v1
   ```

   The tag message names the commit, the gate's result and the toolchains (JDK, go1.27.1).
6. Main's journal starts over, as at the JVM freeze: `doc/JOURNAL.md` opens with a paragraph
   pointing to `arbace-for-golang`, its tag and git for the history up to the freeze;
   CLAUDE.md's Records paragraph names both freezes; the agenda records the freeze.

A fix release commits the fix on the branch, passes `bin/gate --full` there, rebuilds and
checks the executables as in 2, and tags `arbace-for-go1.27.1-vN`, with a line in "Releases"
above and in main's README.

## What comes next on main

Main goes on, and the branch takes fixes only. First the `.ae` rename: Arbace's sources, which
hold forms Clojure cannot evaluate, move from `.clj` to `.ae` (the frozen branch keeps `.clj`).
Then c2a, a Clojure → Arbace translator (first the renames `clojure.*` → `arbace.*`, later the
dialects' divergences), with the tools renamed after their target (j2c → j2a, g2c → g2a, c2g →
a2g if still needed); Arbace's own version in the REPL's banner and `*arbace-version*`; and B1b,
the box: the same program built `GOOS=tamago` and run on go-whim's monitor, on /dev/kvm on amd64
and later Hypervisor.framework on arm64 ([BOX-SURVEY.md](BOX-SURVEY.md), stages S0-S6).
