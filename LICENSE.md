# Licenses

All licenses of the code in this repository are collected here.

Arbace itself, all code and documentation in this repository not listed below, is Copyright (c)
the Arbace authors and licensed under the Eclipse Public License 1.0, the same license as Clojure
(text below).

Arbace vendors code from the projects below; each part stays under its own license, reproduced in
full in this file. Source files keep the copyright and license notices they came with (EPL 1.0
§3 and the BSD 3-Clause License require it); this file is the one place that states which license
covers what.

- `arbace/`: the vendored Clojure in it (everything outside the tools `arbace/classes/`,
  `arbace/repr/`, `arbace/build/`, `arbace/j2g/` and `arbace/c2j/`, which are Arbace's own but
  for repr/java's native lexer and parser, ports of jdk26u's javac, below) is
  derived from Clojure, https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a`,
  with ASM, https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`: the frozen tree `clojure/` that this repository
  held until the freeze (it remains on the branch `arbace-for-java-26`), renamed to `arbace.*`,
  with its Java converted to Clojure Java forms (doc/VENDOR-NOTES.md). Each file stays under its
  source's license:
  - ASM, under the BSD 3-Clause License (the package renamed from `org.objectweb.asm`, by way
    of `clojure.asm`, to `arbace.asm`), was `arbace/asm/` and `arbace/asm.ae` until 2026-10-12,
    when it left main with the JVM's bytecode back end (doc/STANDALONE-NOTES.md, S4; it remains
    on the branch `arbace-for-java-26`). No file of main is derived from it now; its license
    stays below for the JVM jars main uses or used, which embed it (`seed/arbace-seed.jar`
    while it was committed, until S4, 2026-10-12, now on the branch `arbace-for-java-26`, and the
    frozen jar of `arbace-for-java-26-v4`, the oracle's reference, built from its tag), and for
    `jdk/edge.edn` while it holds ASM's declarations;
  - `arbace/lang/Murmur3.ae`: from Guava (Copyright (C) 2011 The Guava Authors), as included in
    Clojure, under the Apache License 2.0; the MurmurHash3 algorithm it implements was written
    by Austin Appleby and placed in the public domain;
  - `arbace/repl.ae` (Copyright (c) Chris Houser): it still carries an older header naming the
    Common Public License 1.0, the EPL's predecessor; upstream Clojure distributes it as part of
    Clojure under the EPL and ships no CPL text;
  - the rest: Clojure, under the Eclipse Public License 1.0.

  Files there that are Arbace's own, not derived from Clojure (they say so in their header, such
  as `arbace/lang/KeywordInvokeSite.ae`), are under the Eclipse Public License 1.0 as Arbace.
  The Go-build variants of vendored files (`arbace/lang/go/`, read by j2g only) are derived
  from the files they vary, under their licenses (since SA150, 2026-10-13, merged into their
  bases; `arbace/lang/go/` holds only the generated `CompilerOps.ae` and `CompilerAOTFn.ae`).
- `arbace/spec/` (`arbace/spec/alpha.ae`, `gen/alpha.ae`, `test/alpha.ae`): spec.alpha,
  https://github.com/clojure/spec.alpha at `v0.6.249`
  (`3d1efb353b8a95c699b4051ba8273568c21f874e`), Copyright (c) Rich Hickey, under the Eclipse
  Public License 1.0. Renamed from `clojure.spec.*` to `arbace.spec.*` (doc/VENDOR-NOTES.md).
- `arbace/core/specs/alpha.ae`: core.specs.alpha, https://github.com/clojure/core.specs.alpha
  at `v0.6.133-alpha10` (`75875946b7e827a1cec91ce645d5e1dcfc475e49`), under the Eclipse Public
  License 1.0 (the repository's `LICENSE`; the file itself carries no notice). Renamed from
  `clojure.core.specs.alpha` to `arbace.core.specs.alpha`.
- `test/oracle/forms/harvest/` and the expected results recorded from it
  (`test/oracle/expected/forms/harvest/`): forms harvested by `bin/oracle harvest` from the
  assertions of Clojure's test suite, https://github.com/clojure/clojure at
  `98d735fab02f337cee654cb0629bddc09883a75a`, `test/clojure/test_clojure/`, renamed to
  `arbace.*`; under the Eclipse Public License 1.0, as Clojure.
- `test/java-fixtures/forms/` (test/java-fixtures/README.md): the Java forms of Clojure's
  suite's Java fixtures, `test/java` of https://github.com/clojure/clojure at
  `98d735fab02f337cee654cb0629bddc09883a75a` (the files carry no notice of their own), renamed
  by repr/clojure and converted by repr/java: Copyright (c) Rich Hickey (Clojure's readme.txt), under the Eclipse
  Public License 1.0, as Clojure. `test/java-fixtures/provenance.edn` and `README.md`: Arbace's
  own, under the Eclipse Public License 1.0.
- `overlay/go/runtime/proc.ae` and `overlay/go/runtime/runtime2.ae`: Go's runtime, the files
  `src/runtime/proc.go` and `runtime2.go` of TamaGo's go1.27.1 tree
  (https://github.com/usbarmory/tamago-go, `VERSION` "go1.27.1", 2026-08-28; the Go
  distribution with TamaGo's changes), converted to Go forms by `bin/repr-go read` and patched
  (doc/go/JRT-NOTES.md, phase 2a): Copyright 2009 The Go Authors, under Go's BSD 3-Clause
  License (text below). `overlay/go/runtime/arbace_local.ae` and `overlay/go/runtime.ae` are
  Arbace's own.
- `overlay/go/reflect/arbace_named.ae` (goi's run-time named types, doc/go/GOI-SPEC.md "As built:
  GR9a-c"): Arbace's addition to Go's package `reflect`, written after the GR9 survey's prototype
  (doc/go/GR9-SURVEY.md, Appendix B) on reflect's own types and helpers of TamaGo's go1.27.1
  tree (`src/reflect/type.go`; its blank-field `Equal` after `StructOf`'s), converted to Go
  forms by `bin/repr-go read`: Copyright 2009 The Go Authors (the parts it adapts) and Arbace,
  under Go's BSD 3-Clause License (text below). `overlay/go/reflect.ae` is Arbace's own.
- `overlay/go/syscall/fs_tamago.ae`: TamaGo's simulated file system, the file
  `src/syscall/fs_tamago.go` of the same TamaGo go1.27.1 tree (itself adapted from Go's
  `fs_nacl.go`), patched and converted to Go forms by `bin/repr-go read` (doc/go/BOX-NOTES.md,
  S7): Copyright 2019 The Go Authors, under Go's BSD 3-Clause License (text below).
  `overlay/go/syscall.ae` is Arbace's own.
- Excerpts of Go's standard library in repr/go's cases, tests and spec: `test/repr/go/cases/s14_2_utf8.go`
  (from `src/unicode/utf8/utf8.go`), `s14_3_sort.go` (from `src/sort/sort.go` and
  `zsortinterface.go`), `s02_search.go` (`sort.Search`, from `src/sort/search.go`), the Go forms
  of the same names (`.ae`) that hold them converted, `test/repr/go/convert/search.go`
  (`sort.Search`) and the expected forms `test/repr/go/convert/expected/s02_search.ae`,
  `s14_2_utf8.ae` and `s14_3_sort.ae`, and their code and forms in doc/go/SPEC.md §2
  (`sort.Search`), §14.2 and §14.3: from TamaGo's go1.27.1 tree, as above, Copyright 2009
  (`utf8.go`, `sort.go`), 2010 (`search.go`) and 2022 (`zsortinterface.go`) The Go Authors,
  under Go's BSD 3-Clause License (text below). Each of those files carries Go's notice
  ("Copyright 20XX The Go Authors. All rights reserved. Use of this source code is governed by
  a BSD-style license that can be found in the LICENSE file."), the forms as `;;` lines; SPEC.md's
  excerpts are covered by this entry.
- `tools/gocmp/errorcheck.go`'s judge of errorcheck programs (goi's check, doc/go/GOI-SPEC.md
  "As built: GR7b"): the functions `splitOutput`, `errorCheck`, `wantedErrors`, `matchPrefix`,
  `partitionStrings`, `replacePrefix` and the regexps of `// ERROR` comments, from TamaGo's
  go1.27.1 tree's `src/cmd/internal/testdir/testdir_test.go`, Copyright 2012 The Go Authors,
  under Go's BSD 3-Clause License (text below); the file carries Go's notice. The rest of
  `tools/gocmp/` is Arbace's own.
- `go/`: jrt's hand-written Go forms (`go/arbace/jrt.ae`, `go/arbace/jrt/`) and the Go
  build's main package (`go/arbace/cmd/arbace/`): Arbace's own, under the Eclipse Public License
  1.0, except the parts of `go/arbace/jrt/` listed below.
- `box/` (`box/go/arbace/box.../`, the packages `arbace/box/hv`, `vmm`, `abi`, `board` and
  `libsys`, with `board/boot_amd64.s` and `boot_arm64.s`; box/README.md, doc/go/BOX-NOTES.md;
  the launcher `arbace/box/cmd/arbace-box`, added in S1, `box/mac/`, added in S4, and the
  packages `arbace/box/tool` and `arbace/box/tree` (`box/go/arbace/box/tool.ae`, `tool/`,
  `tree.ae`, `tree/`), added in S7, are Arbace's own, under the Eclipse Public License 1.0;
  purego is the next entry):
  the box of go-whim, https://github.com/arbace/go-whim at
  `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, its `hv/`, `vmm/` (without `vmm/cmd/`),
  `guest/abi/`, `guest/tamago/board/` and `internal/libsys/`, converted to Go forms by
  `bin/repr-go read` (`bin/box-vendor`): under the Eclipse Public License 1.0, as Arbace.
  go-whim's repository-wide `LICENSE` is Vim's licence, which covers the modified vim that
  go-whim's README names as its product; these parts are not derived from vim. Their history
  in go-whim (`git log --format=%an -- hv vmm guest/abi guest/tamago/board internal/libsys`)
  has a single author, the user (`rhz`, robert.zawiasa@gmail.com), as has every commit of
  go-whim, and the files carry no other copyright or licence notice. The user stated on
  2026-10-11 that these parts are licensed to Arbace under the Eclipse Public License 1.0
  (decision D8, doc/go/BOX-SURVEY.md §10). go-whim keeps its own copy under its own terms.
  The test guests and their tests (`box/go/arbace/box/guest.ae`, `guest/`: the packages
  `arbace/box/guest`, `guest/demo`, `guest/cmd/demo`, `guest/cmd/faulty` and `guest/cmd/bare`,
  with `bare_amd64.s` and `bare_arm64.s`), added on 2026-10-16 when go-whim was vendored whole
  (doc/go/BOX-NOTES.md, "go-whim vendored whole"), adapt more of the same commit: its
  `guest/guest_test.go`, `smp_test.go`, `readahead_test.go`, `guest/tamago/main.go`,
  `guest/tamago/faulty/main.go` and `guest/rt/` (`hello.c`, `rt.c`, `entry_amd64.S`,
  `entry_arm64.S`, rewritten in Go's assembly), whose history in go-whim also has the user as
  its single author; under the Eclipse Public License 1.0 on the same terms as D8 (the user's
  decision of 2026-10-16 to vendor and adapt everything Arbace needs from go-whim).
  What these packages build with: on darwin, `hv` and `libsys` import purego (the next entry),
  and a guest built on `board` links TamaGo's `goos` module (https://github.com/usbarmory/tamago
  v1.27.1, BSD 3-Clause License, Copyright (c) The TamaGo Authors) and TamaGo's Go tree (Go's
  BSD 3-Clause License, below), both held in `tamago/` (below).
- `box/go/arbace/box/purego.ae` and `box/go/arbace/box/purego/` (the packages
  `arbace/box/purego`, `purego/internal/fakecgo` and `purego/internal/strings`, with their
  assembly and headers; box/README.md, doc/go/BOX-NOTES.md, S4, BX62): purego,
  https://github.com/ebitengine/purego v0.11.1 (go.sum
  `h1:2zpWRSQNVKN4eKsKO9eM1ILDgWfYMY9GwqRmK6XeQ/0=`), its files that build for darwin/arm64,
  their import paths renamed to `arbace/box/purego`, converted to Go forms by `bin/repr-go read`
  (`bin/box-vendor purego`): Copyright 2022 The Ebitengine Authors (each file's SPDX notice,
  kept as `;;` lines), under the Apache License 2.0 (section "purego" below). The box's launcher
  for macOS (`bin/box-mac-bundle`) contains it.
- `tamago/` (tamago/README.md, doc/go/BOX-NOTES.md, S6): the box guest's own Go as Go forms,
  converted by `bin/repr-go read` (`bin/tamago-vendor`), with the assembly, headers, embedded
  files and module files they came with, unchanged:
  - `tamago/goroot/`: the standard library of TamaGo's go1.27.1 tree
    (https://github.com/usbarmory/tamago-go, `VERSION` "go1.27.1", 2026-08-28; the Go
    distribution with TamaGo's changes), every package of `go list std` with Go files for
    `tamago/amd64` or `tamago/arm64` from its `src/` (but `internal/trace/traceviewer` and
    `encoding/json/internal/jsontest`, which embed third-party data: not held here), and the
    headers `src/runtime/cgo/abi_amd64.h` and `abi_arm64.h`: Copyright The Go Authors (and, for TamaGo's
    additions, their authors as the files state), under Go's BSD 3-Clause License (text below).
  - `tamago/goos/`: TamaGo's `goos` module, https://github.com/usbarmory/tamago v1.27.1 (go.sum
    `h1:Zl4w3hu5DaogGvcLzunS4gjwNEqdjFFJedMO3pDQa5k=`), its packages that build for
    `tamago/amd64` or `tamago/arm64` and its `go.mod`: Copyright (c) The TamaGo Authors, under the BSD 3-Clause
    License (text below).
  - `tamago/cmd/` (tamago/cmd/README.md, doc/STANDALONE-NOTES.md, S9): Go's toolchain, the 105
    packages of the same tree's `src/cmd/` that `cmd/compile`, `cmd/link` and `cmd/asm` build
    from (with `cmd/vendor/golang.org/x/telemetry/...`, vendored there from
    https://go.googlesource.com/telemetry at the version `src/cmd/go.mod` names), their
    assembly, and `src/cmd/go.mod`, `go.sum`, `vendor/modules.txt` and
    `compile/default.pgo`: Copyright The Go Authors, under Go's BSD 3-Clause License (text
    below).
  Each Go file's notice is kept, as `;;` lines of its forms.
- `go/arbace/repr/go/internal/syntax.ae` and `syntax/` (doc/go/GO-PARSER-SURVEY.md, "As built:
  GP7"): a renamed copy of `tamago/cmd/`'s forms of `cmd/compile/internal/syntax` (above: the
  same tree's `src/cmd/compile/internal/syntax/`), its namespace and import path renamed
  `arbace/repr/go/internal/syntax` by `arbace.repr.go.syntax/copy-texts`, nothing else changed:
  Copyright The Go Authors, under Go's BSD 3-Clause License (text below); each file's notice kept.
- `x509roots/` (`x509roots/go/arbace/x509roots/fallback/bundle.ae` and `bundle/`, with
  `bundle/bundle.der`; x509roots/README.md, doc/go/BOX-NOTES.md "TLS (option 2)"): the package
  `bundle` of the module `golang.org/x/crypto/x509roots/fallback`, https://go.googlesource.com/crypto
  at `3b414e3e8284a71c817c4d1009acdd5d3a464ff9` (version
  `v0.0.0-20261011195143-3b414e3e8284`, go.sum `h1:vbII1qQTqQ3ya1k/bfJWuHiUNzCkFD7LRbGiO3gSwVE=`,
  from Go's module cache), its import path renamed to `arbace/x509roots/fallback/bundle`, converted
  to Go forms by `bin/repr-go read` (`bin/x509roots-vendor`), its embedded `bundle.der` unchanged:
  Copyright 2023-2026 The Go Authors (each file's notice, kept as `;;` lines; the generated
  `bundle.go` carries none), under Go's BSD 3-Clause License (the module's `LICENSE` is the text
  below, "Go: BSD 3-Clause License", byte for byte). The certificates in `bundle.der` are the
  roots of Mozilla's NSS trust store, as the module's generator (`gen_fallback_bundle.go`, over
  `x509roots/nss`) extracted them from NSS's `certdata.txt`; the module distributes them under
  its own licence only. By the user's decision (2026-10-12), the certificate data is recorded here
  under NSS's licence as well: the Mozilla Public License 2.0 (text below, "Mozilla NSS's root
  certificates: Mozilla Public License 2.0"), distributed unmodified. The Go build's executables
  and the box's guest contain them.
- `tools/tlsserver/`: the TLS test server of the TLS checks: Arbace's own, under the Eclipse
  Public License 1.0.
- `jdk/` (jdk/README.md; doc/STANDALONE-SURVEY.md, S1, D2): the JDK's Java forms and resource
  data that the Go build reads, committed since 2026-10-12; `bin/jrt-convert` makes them (and
  `bin/jrt-convert verify` proves them) from https://github.com/openjdk/jdk26u at `baf63fb` and
  from jrt's own Java (`jdk/provenance.edn` records the commit; `jdk/sources.txt` lists each
  file's origin, which each form file's first line also names):
  - `jdk/forms/`: Java forms converted by repr/java from jdk26u's Java sources (the closure of
    java.base's files jrt translates, 755 of `src/java.base/share/classes`, 6 of
    `src/java.base/unix/classes`, 2 of `src/java.sql/share/classes`), from the 31 sources the JDK
    build generates (`CharacterData*`, java.nio's buffers, `ScopedMemoryAccess`, the CLDR locale
    data, `LocaleEquivalentMaps`: generated by `bin/jrt-convert` with jdk26u's build tools from
    jdk26u's data, as the build generates them) and from `jdk/internal/jrt/LocaleDataIndex`
    (generated by `test/jrt/jrt_sources.ae`): Copyright (c) Oracle and/or its affiliates, under
    the GNU General Public License version 2 with the Classpath Exception (text below), with the
    third-party data they hold under the licenses listed below ("The JDK's resource data": the
    Unicode Character Database's, ICU4J's and CLDR's under the Unicode License V3). The form files
    converted from jrt's own Java (the first line names `overlay/jdk/java.base/...`) are under
    `overlay/jdk/`'s licenses, given in the next item.
  - `jdk/data/`: the JDK's resource data, as listed below ("The JDK's resource data").
  - `jdk/edge.edn` (j2g's edge, recorded by `bin/j2g-edge`: doc/STANDALONE-NOTES.md, S2): the
    declarations (names, modifiers, signatures, constants, annotations) of the classes outside
    the forms, as the JVM reports them: of the JDK's classes (JDK 26.0.2.1, built from jdk26u at
    `baf63fb`) under the GNU General Public License version 2 with the Classpath Exception; of
    ASM's (Arbace's ASM, `arbace.asm`, until the file is recorded again without them: ASM left
    main on 2026-10-12) and Arbace's runtime classes, their sources' licenses (ASM's BSD 3-Clause
    License, the Eclipse Public License 1.0).
  - `jdk/sources.txt` and `jdk/provenance.edn` (lists made by `bin/jrt-convert`) and
    `jdk/README.md`: Arbace's own, under the Eclipse Public License 1.0.
  - `jdk/javac/` (javac's forms, doc/classes/JAVA-FRONTEND-SURVEY.md, JN2; `bin/javac-vendor`
    makes them and `bin/javac-vendor verify` proves them, from https://github.com/openjdk/jdk26u
    at `baf63fb`): `jdk/javac/forms/`, Java forms converted by repr/java from jdk26u's
    `src/jdk.compiler/share/classes`, `src/java.compiler/share/classes`,
    `src/jdk.internal.opt/share/classes/jdk/internal/opt/CommandLine.java`, the sources the JDK
    build generates for jdk.compiler (its `support/gensrc/jdk.compiler`; since JFS48 (b) generated
    by `bin/javac-vendor` from jdk.compiler's `.properties` with the build's langtools tools, as the
    build generates them, and since JFS52 (b) `FlagsEnum` too, from `Flags.java`) and 23 files of
    `src/java.base/share/classes` (`jdk/javac/sources.txt` lists them, and each form file's
    first line names its source): Copyright (c) Oracle and/or its affiliates, under the GNU
    General Public License version 2 with the Classpath Exception (text below); the form files
    converted from jrt's own Java for javac's world (the first line names
    `overlay/jdk/javac/...`) are under `overlay/jdk/`'s licenses (the next item).
    `jdk/javac/edge.edn` (the edge of javac's world beyond `jdk/edge.edn`'s, recorded by
    `bin/javac-vendor` on the frozen JVM): the declarations of the JDK's classes, as
    `jdk/edge.edn`'s, under the GNU General Public License version 2 with the Classpath
    Exception. `jdk/javac/sources.txt`, `jdk/javac/provenance.edn` and `jdk/javac/platform.sha256`
    (the pin of javac on Arbace's platform, JN3): Arbace's own, under the Eclipse Public License 1.0.
    Since JN7 (doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN7") `jdk/javac/forms/` also holds
    the forms of the JDK build's tools that `bin/jrt-convert`'s generate step runs, converted from
    jdk26u's `make/jdk/src/classes/build/tools/` (with the interim copies of
    `src/java.base/share/classes/java/time/zone/` in package `build.tools.tzdb`), and since JFS48 (b)
    of the langtools tools `bin/javac-vendor` runs on the tool, converted from jdk26u's
    `make/langtools/tools/compileproperties/`, `make/langtools/tools/propertiesparser/` and (since
    JFS52 (b)) `make/langtools/tools/flagsgenerator/`: Copyright (c)
    Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath
    Exception; and of java.xml's SAX parser and the classes it names
    (`overlay/jdk/javac/closure.txt`, from jdk26u's `src/java.xml/share/classes`): each form file
    keeps its source's notice, the GNU General Public License version 2 with the Classpath
    Exception (Copyright (c) Oracle and/or its affiliates) or, for the files derived from Apache
    Xerces (jdk26u's `src/java.xml/share/legal/xerces.md`; their notice "Licensed to the Apache
    Software Foundation (ASF)"), the Apache License 2.0 (text in "Murmur3 (Guava): Apache License
    2.0" below), with Oracle's copyright line where the file has one.
- `overlay/jdk/javac/java.base/java/util/zip/Deflater.java` (JN7): jrt's `Deflater` for javac's
  tool build, a port to Java of zlib 1.3.2's compressor as jdk26u bundles it
  (`src/java.base/share/native/libzip/zlib/`: `deflate.c`, `deflate.h`, `trees.c`; zlib by
  Jean-loup Gailly and Mark Adler, https://zlib.net): Copyright (C) 1995-2026 Jean-loup Gailly and
  Mark Adler, and the Arbace authors for the port, under the zlib License (section "zlib: zlib
  License" below); its public API follows jdk26u's documented `java.util.zip.Deflater`.
- `overlay/jdk/`: jrt's own Java sources and Go-build variants of JDK classes (doc/go/JRT-NOTES.md,
  "Phase 2C"; `overlay/jdk/javac/`, those of javac's tool build, doc/classes/JAVA-FRONTEND-SURVEY.md,
  JN2), written for Arbace in place of jdk26u's files of the same names (from their
  documented APIs, without their code): Arbace's own, under the Eclipse Public License 1.0. The
  jdk26u files that bin/jrt-convert translates are read from the jdk26u checkout, not kept here;
  their Java forms are, in `jdk/forms/` (above).
  Except: `overlay/jdk/java.base/java/util/Calendar.java` and `GregorianCalendar.java`
  (doc/go/JRT-NOTES.md, phase 2B "Dates") hold code transcribed from
  https://github.com/openjdk/jdk26u at `baf63fb` (`src/java.base/share/classes/java/util/Calendar.java`,
  `GregorianCalendar.java`; each file's header names the parts): Copyright (c) Oracle and/or
  its affiliates, under the GNU General Public License version 2 with the Classpath Exception
  (text below); the rest of those files is Arbace's own. Kept so by the user's decision
  (2026-10-09; doc/go/EVAL-NOTES.md, B7), rather than rewritten from the documented behaviour.
  Likewise, by the rule of B7, jrt's own Java of java.time's locale data (doc/go/JRT-NOTES.md,
  "Time"): `overlay/jdk/java.base/java/util/ResourceBundle.java` and, in
  `overlay/jdk/java.base/sun/util/locale/provider/`, `LocaleProviderAdapter.java`,
  `LocaleResources.java`, `CalendarDataUtility.java`, `TimeZoneNameUtility.java` and
  `JavaTimeDateTimePatternImpl.java` hold code transcribed from jdk26u at `baf63fb` (the files of
  the same names, `CalendarNameProviderImpl.java`, `TimeZoneNameProviderImpl.java`, and
  `src/java.base/share/classes/sun/util/cldr/CLDRCalendarDataProviderImpl.java`,
  `CLDRTimeZoneNameProviderImpl.java`, `CLDRLocaleProviderAdapter.java`; each file's header
  names the parts), under the GNU General Public License version 2 with the Classpath Exception;
  the rest of those files is Arbace's own. Until 2026-10-10 the overlay also held jrt's own
  `TimeZone.java`, `sun/util/calendar/ZoneInfo.java` and `jdk/internal/jrt/TimeText.java`;
  jdk26u's `TimeZone` and `ZoneInfo` are translated since, and `TimeText` is gone.
  Also except (doc/go/JRT-NOTES.md, "Sockets"; amendment NT3): `overlay/jdk/java.base/jdk/internal/jrt/HostSocketImpl.java`
  follows `src/java.base/share/classes/sun/nio/ch/NioSocketImpl.java` of jdk26u at `baf63fb`
  (its state checks, messages, streams and option handling transcribed, the I/O replaced by
  jrt's socket table), and the variants `overlay/jdk/variants/Socket.ae`, `ServerSocket.ae`, `InetAddress.ae`,
  `Inet4AddressImpl.ae`, `Inet6AddressImpl.ae`, `Inet6Address.ae`, `IPAddressUtil.ae` and `Exceptions.ae` keep
  the code of the jdk26u methods they replace (`java/net/Socket.java`, `ServerSocket.java`, `InetAddress.java`,
  `Inet6AddressImpl.java`, `Inet6Address.java`, `sun/net/util/IPAddressUtil.java`, `jdk/internal/util/Exceptions.java`) where they keep it: Copyright (c)
  Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath
  Exception (text below); the rest of those files is Arbace's own.
- Security (doc/go/JRT-NOTES.md, "Security"): `overlay/jdk/java.base/javax/crypto/JceSecurity.java`
  holds the `getInstance` methods of https://github.com/openjdk/jdk26u at `baf63fb`
  (`src/java.base/share/classes/javax/crypto/JceSecurity.java.template`),
  `overlay/jdk/java.base/sun/security/provider/SeedGenerator.java` its `getSystemEntropy` and
  `longToByteArray` (`sun/security/provider/SeedGenerator.java`), and the variants
  `overlay/jdk/variants/Provider.ae`, `Security.ae`, `ProviderConfig.ae`, `SunEntries.ae`,
  `SunJCE.ae`, `DigestBase.ae` and `NativePRNG.ae` keep the code of the jdk26u methods they
  replace (`java/security/Provider.java`, `Security.java`, `sun/security/jca/ProviderConfig.java`,
  `sun/security/provider/SunEntries.java`, `com/sun/crypto/provider/SunJCE.java`,
  `sun/security/provider/DigestBase.java`,
  `src/java.base/unix/classes/sun/security/provider/NativePRNG.java`) where they keep it:
  Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
  with the Classpath Exception (text below); the rest of those files, the digests' variants
  (`MD5.ae`, `SHA.ae`, `SHA2.ae`, `SHA5.ae`, `SHA3.ae`), `jdk/internal/jrt/HostRandom.java`,
  `sun/security/provider/JrtServices.java` and `com/sun/crypto/provider/JceServices.java` are Arbace's
  own.
- `overlay/jdk/java.base/jdk/internal/jrt/HostPath.java` (doc/go/JRT-NOTES.md, "Files") holds code
  transcribed from https://github.com/openjdk/jdk26u at `baf63fb`
  (`src/java.base/unix/classes/sun/nio/fs/UnixPath.java`: the path operations, its header names
  them), `overlay/jdk/java.base/java/nio/file/Path.java` its default methods `resolve` (the two
  varargs overloads), `resolveSibling(Path)` and `iterator` (`java/nio/file/Path.java`),
  `overlay/jdk/java.base/java/nio/file/Files.java` its `readAllLines(Path, Charset)` and
  `copy(Path, OutputStream)` (`java/nio/file/Files.java`), and `go/arbace/jrt/filesystem.ae` jrt's
  Go forms following `src/java.base/unix/native/libjava/canonicalize_md.c` (`JDK_Canonicalize`) and
  `path_util.c` (`collapse`) and the results of `UnixFileSystem_md.c`: Copyright (c) Oracle and/or
  its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text
  below); recorded by the rule of B7. The rest of those files is Arbace's own, and the other files
  of the change are written from the documented APIs, as the overlay's other files (but for the
  variants keeping jdk26u's code, listed below).
- Processes (doc/go/JRT-NOTES.md, "Processes"; recorded by the rule of B7): the variants
  `overlay/jdk/variants/ProcessImpl.ae`, `Process.ae`, `ProcessBuilder.ae` and
  `ProcessHandleImpl.ae` keep the code of the jdk26u methods they replace
  (https://github.com/openjdk/jdk26u at `baf63fb`: `src/java.base/unix/classes/java/lang/ProcessImpl.java`,
  `src/java.base/share/classes/java/lang/Process.java`, `ProcessBuilder.java`,
  `ProcessHandleImpl.java`) where they keep it; `overlay/jdk/java.base/jdk/internal/jrt/RuntimeExec.java`
  transcribes `Runtime.exec` (`src/java.base/share/classes/java/lang/Runtime.java`); and
  `go/arbace/jrt/process.ae` and `hostproc_linux.ae`, jrt's Go forms, follow
  `src/java.base/unix/native/libjava/ProcessImpl_md.c` (the exception texts, `IOE_FORMAT`),
  `childproc.c` (`JDK_execvpe`'s search), `ProcessHandleImpl_unix.c` and
  `src/java.base/linux/native/libjava/ProcessHandleImpl_linux.c` (`/proc`'s fields, the status
  encoding, the start-time checks): Copyright (c) Oracle and/or its affiliates, under the GNU
  General Public License version 2 with the Classpath Exception (text below); the rest of those
  files is Arbace's own. The errno texts of `process.ae` are musl's `strerror`'s (as this
  machine's JVM gives them), facts of its interface, not its code.
- `overlay/jdk/java.base/jdk/internal/jrt/ThreadPerTaskExecutor.java` (doc/go/JRT-NOTES.md,
  "Concurrency": its states and their transitions, `ensureNotShutdown`, `tryTerminate`,
  `tryShutdownAndTerminate`, `shutdown`, `shutdownNow`), the methods `DefaultThreadFactory.newThread`
  and `AutoShutdownDelegatedExecutorService`'s of `overlay/jdk/variants/Executors.ae`,
  `arrangeTimeout` of `overlay/jdk/variants/CompletableFuture.ae`, `doInvokeAny` of
  `overlay/jdk/java.base/jdk/internal/jrt/ForkJoinPools.java`, and the default methods of
  `Future` in `go/arbace/jrt/executor.ae` (`Future_ResultNow__O`, `Future_ExceptionNow__Throwable`,
  `Future_State__Future_State`) follow the code of https://github.com/openjdk/jdk26u at `baf63fb`
  (`src/java.base/share/classes/java/util/concurrent/ThreadPerTaskExecutor.java`,
  `Executors.java`, `CompletableFuture.java`, `AbstractExecutorService.java`, `Future.java`):
  Copyright (c) Oracle and/or
  its affiliates, under the GNU General Public License version 2 with the Classpath Exception
  (text below); recorded as ports to be safe, by the rule of B7. `Executors.java`,
  `CompletableFuture.java`, `AbstractExecutorService.java` and `Future.java` also carry the notice
  of their original version: "Written by Doug Lea with assistance from members of JCP JSR-166
  Expert Group and released to the public domain, as explained at
  http://creativecommons.org/publicdomain/zero/1.0/". The rest of the concurrency
  work's files (the other variants and overlay classes, `varhandle.ae`) is Arbace's own.
- `overlay/jdk/variants/TreeMap.ae` (doc/go/JAVA-BASE.md, amendment JB2): its two
  `buildFromIterator` methods are jdk26u's `buildFromSorted` (https://github.com/openjdk/jdk26u
  at `baf63fb`, `src/java.base/share/classes/java/util/TreeMap.java`) without the deserializing
  branch, and its other members keep the code of the members they replace: Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath
  Exception (text below), by the rule of B7; the rest of the file is Arbace's own.
- Other Go-build variants (J2G-SPEC §4.6; recorded by the rule of B7):
  `overlay/jdk/variants/AbstractChronology.ae`, `BitSet.ae`, `CRC32.ae`, `Collectors.ae`,
  `File.ae`, `FileHandler.ae`, `ParseUtil.ae`, `TimeZone.ae`, `TzdbZoneRulesProvider.ae`,
  `URI.ae`, `URL.ae`, `URLConnection.ae`, `ZoneInfoFile.ae` and `ZoneOffset.ae` keep the code
  of the jdk26u members they replace (https://github.com/openjdk/jdk26u at `baf63fb`:
  `src/java.base/share/classes/java/time/chrono/AbstractChronology.java`, `java/util/BitSet.java`,
  `java/util/zip/CRC32.java`, `java/util/stream/Collectors.java`, `java/io/File.java`,
  `src/java.base/unix/classes/sun/net/www/protocol/file/Handler.java`, `sun/net/www/ParseUtil.java`,
  `java/util/TimeZone.java`, `java/time/zone/TzdbZoneRulesProvider.java`, `java/net/URI.java`,
  `java/net/URL.java`, `java/net/URLConnection.java`, `sun/util/calendar/ZoneInfoFile.java`,
  `java/time/ZoneOffset.java`) where they keep it (each file's header names the members), and
  `overlay/jdk/java.base/jdk/internal/jrt/StandardStreams.java` keeps `System.newPrintStream`
  (`java/lang/System.java`): Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below); the rest of those files is
  Arbace's own.
- `go/arbace/jrt/fdlibm.ae` and the `FdLibm` part of `go/arbace/jrt/math.ae` (`Log`, `Cbrt`,
  `Pow`): jrt's hand-written Go forms porting, as written, https://github.com/openjdk/jdk26u at
  `baf63fb`, `src/java.base/share/classes/java/lang/FdLibm.java` (doc/go/JRT-NOTES.md, phase 1
  and "Phase 2B (step 5)"): Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below).
- `arbace/repr/java/lex.ae` and `arbace/repr/java/parse.ae` (repr/java's native lexer and parser,
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN8"): Arbace forms porting, method by method,
  https://github.com/openjdk/jdk26u at `baf63fb`, `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s
  `parser/UnicodeReader.java`, `JavaTokenizer.java`, `JavadocTokenizer.java` (its comments),
  `Tokens.java`, `JavacParser.java`, `tree/TreeInfo.java` (`getStartPos`, `getEndPos` and the
  helpers the parser calls) and `TreeMaker.java` (its factories' positions): Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath
  Exception (text below; each file carries the notice of the files it ports).
- `decodeUTF16` and `encodeUTF16` in `go/arbace/jrt/codec.ae`, and `indent`, `stripIndent` and
  `translateEscapes` in `go/arbace/jrt/string.ae`: jrt's hand-written Go forms following the code
  of https://github.com/openjdk/jdk26u at `baf63fb`
  (`src/java.base/share/classes/sun/nio/cs/UnicodeDecoder.java`, `UnicodeEncoder.java`,
  `java/lang/String.java`): Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below); recorded as ports to be
  safe, by the rule of B7 (step 5).
- The JDK's resource data in the Go build's executables (doc/go/JRT-NOTES.md, "The JDK's resource
  data"): kept in `jdk/data/` since 2026-10-12 (standalone S1); `bin/jrt-convert` makes them
  from https://github.com/openjdk/jdk26u at `baf63fb` (`bin/jrt-convert update` writes them
  there), and `bin/j2g --program` embeds them, so `bin/arbace-go`'s executables carry them:
  - `jdk/internal/icu/impl/data/icudata/nfc.nrm` and `nfkc.nrm`, copied from jdk26u's
    `src/java.base/share/classes/jdk/internal/icu/impl/data/icudata/`: ICU4J's normalization data
    (ICU 78.1, as jdk26u's `src/java.base/share/legal/icu.md` records), under the ICU4J License,
    the Unicode License V3 (text below);
  - `java/lang/uniName.dat`, generated as the JDK build generates it
    (`make/modules/java.base/Gendata.gmk`: jdk26u's build tool
    `make/jdk/src/classes/build/tools/generatecharacter/CharacterName.java` run on
    `src/java.base/share/data/unicodedata/UnicodeData.txt`): the character names of the Unicode
    Character Database 17.0.0, under the Unicode License V3 (text below, as jdk26u's
    `src/java.base/share/legal/unicode.md` states it);
  - `lib/tzdb.dat` (doc/go/JRT-NOTES.md, "Time"), the time-zone database, generated as the JDK
    build generates it (`make/modules/java.base/gendata/GendataTZDB.gmk`: jdk26u's build tool
    `make/jdk/src/classes/build/tools/tzdb/TzdbZoneRulesCompiler.java` run on
    `src/java.base/share/data/tzdata/`, tzdata2026b): the IANA time zone database, whose files
    are in the public domain (as each states: "This file is in the public domain"), in jdk26u's
    copies under jdk26u's GNU General Public License version 2 with the Classpath Exception
    (their headers; text below);
  - `conf/security/java.security` (doc/go/JRT-NOTES.md, "Security"), the security properties,
    generated as the JDK build generates them (`make/modules/java.base/Gendata.gmk`: jdk26u's
    build tool `make/jdk/src/classes/build/tools/makejavasecurity/MakeJavaSecurity.java` run on
    `src/java.base/share/conf/security/java.security` for linux): jdk26u's file, under its GNU
    General Public License version 2 with the Classpath Exception (text below).

  The executables also hold java.base's CLDR locale data of the root and English locales
  (doc/go/JRT-NOTES.md, "Time"), generated by `bin/jrt-convert` as the JDK build generates it
  (`make/modules/java.base/Gensrc.gmk`: jdk26u's build tool
  `make/jdk/src/classes/build/tools/cldrconverter/` run on `make/data/cldr/common`, CLDR 48) into
  Java sources that are translated with the rest (`sun.text.resources.cldr.FormatData`,
  `FormatData_en`, `sun.util.resources.cldr.TimeZoneNames`, `TimeZoneNames_en`, `CalendarData`,
  `sun.util.cldr.CLDRBaseLocaleDataMetaInfo`, `java.time.format.ZoneName`): the Unicode Common
  Locale Data Repository's data, under the Unicode License V3 (text below, as jdk26u's
  `src/java.base/share/legal/cldr.md` states it), in sources generated by jdk26u's tool under
  its GNU General Public License version 2 with the Classpath Exception.

  Likewise jdk.localedata's CLDR locale data of the locales `bin/jrt-convert` selects
  (doc/go/JRT-NOTES.md, "Text and locales"; `JRT_LOCALES`: none by default, the
  shipped executables' choice; `common`, the 27 locales of `test/jrt/jrt_sources.ae`'s
  `text-locales` with their parent locales; `all`), generated as
  `make/modules/jdk.localedata/Gensrc.gmk` generates it (the same `CLDRConverter`, on the same
  CLDR 48 data) into the bundles `sun.text.resources.cldr.ext.FormatData_*` and
  `sun.util.resources.cldr.ext.TimeZoneNames_*`, `CurrencyNames_*`, `LocaleNames_*`, and
  java.base's `CurrencyNames` and `LocaleNames` of the root and English locales: the Unicode
  Common Locale Data Repository's data, under the Unicode License V3 (SPDX `Unicode-3.0`; text
  below), in sources generated by jdk26u's tool under its GNU General Public License version 2
  with the Classpath Exception. With them, jdk26u's own files of that data: the JRE collation
  rules of jdk.localedata (`src/jdk.localedata/share/classes/sun/text/resources/ext/CollationData_*.java`)
  and java.base (`sun/text/resources/CollationData.java`, `sun/util/locale/provider/CollationRules.java`),
  under jdk26u's GNU General Public License version 2 with the Classpath Exception; and the
  resource data `java/util/currency.data` (generated from
  `src/java.base/share/data/currency/CurrencyData.properties` by
  `make/jdk/src/classes/build/tools/generatecurrencydata/`, GPLv2 with the Classpath Exception),
  `sun/text/resources/WordBreakIteratorData`, `LineBreakIteratorData` and
  `SentenceBreakIteratorData` (generated from `sun/text/resources/BreakIteratorRules.java` and
  the Unicode Character Database's `UnicodeData.txt` by
  `make/jdk/src/classes/build/tools/generatebreakiteratordata/`: GPLv2 with the Classpath
  Exception, the Unicode License V3 for the character data), and ICU4J's bidi properties
  `jdk/internal/icu/impl/data/icudata/ubidi.icu` (copied; ICU4J License, the Unicode License V3).

  `overlay/jdk/java.base/sun/util/resources/LocaleData.java`,
  `overlay/jdk/java.base/sun/util/cldr/CLDRParentLocales.java`, the additions of
  "Text and locales" to `ResourceBundle.java` (`Control`), `Calendar.java` (the week parameters
  and display names), `LocaleProviderAdapter.java`, `LocaleResources.java`,
  `CalendarDataUtility.java` and `ResourceBundleBasedAdapter.java` hold code transcribed from
  jdk26u at `baf63fb` (`java/util/ResourceBundle.java`, `java/util/Calendar.java`,
  `sun/util/resources/LocaleData.java`, `sun/util/locale/provider/LocaleProviderAdapter.java`,
  `JRELocaleProviderAdapter.java`, `LocaleResources.java`, `CalendarDataUtility.java`,
  `ResourceBundleBasedAdapter.java`, `sun/util/cldr/CLDRLocaleProviderAdapter.java`; each file's
  header names the parts): under the GNU General Public License version 2 with the Classpath
  Exception, by the rule of B7; the rest of those files is Arbace's own.
  `test/jrt/jrt_sources.ae`'s `default-list` and `cldr-candidates` (the candidate locales of
  the locale selection) follow `ResourceBundle.java`'s and `CLDRLocaleProviderAdapter.java`'s
  candidate lists likewise. `overlay/jdk/variants/BreakIteratorResourceBundle.ae` keeps the
  code of the method it replaces (`sun/util/resources/BreakIteratorResourceBundle.java`), likewise.

  The executables also hold translated jdk26u code derived from Unicode's and ICU's work (the
  `CharacterData*` tables generated from the Unicode Character Database, `jdk.internal.icu`'s
  normalizer, ported from ICU4J): those files carry their notices, and are covered by jdk26u's
  GPLv2 with the Classpath Exception and, for their Unicode and ICU parts, by the licenses below.
- The Go build's executables, `target/arbace-go/<arch>/arbace` (amd64 and arm64), which
  `bin/arbace-go --build` makes (doc/go/EVAL-NOTES.md, doc/go/EXEC-NOTES.md): `bin/j2g --program`
  translates the program into Go forms and `bin/build-go` builds them with TamaGo's go1.27.1
  toolchain (`ARBACE_GOROOT`) into static executables (`CGO_ENABLED=0`: no C code, no libc). Each
  combines works under several licenses, each covering its part:
  - Arbace: the `arbace/` namespaces the executable carries (the vendored Clojure, spec.alpha,
    core.specs.alpha and Arbace's own, translated by j2g and embedded as sources with the image
    of their analyzed forms; since S6 compiled to Go, the image only for what stays evaluated), jrt (`go/`, and `overlay/jdk/` translated with jdk26u's Java) and
    the main package (`go/arbace/cmd/arbace/` with what j2g writes): under the licenses given
    above for their files, the Eclipse Public License 1.0 but for `arbace/lang/Murmur3.ae`
    (the Apache License 2.0) and the parts derived from jdk26u (the GNU General Public License
    version 2 with the Classpath Exception). ASM is not in them.
  - jdk26u: the classes of java.base that `bin/jrt-convert` converts to Java forms from
    https://github.com/openjdk/jdk26u at `baf63fb` (committed in `jdk/forms/`; 835 files with the default locale set, 838 as of 2026-10-15, the
    sources the JDK build generates among them; with `JRT_LOCALES`, jdk.localedata's bundles of
    the locales selected) and j2g translates into Go, with the generic signatures j2g writes from
    them (`arbace/classes/generics.edn`, embedded): Copyright (c) Oracle and/or its affiliates,
    under the GNU General Public License version 2 with the Classpath Exception (text below).
    With them, the third-party data and code the JDK bundles that they hold or embed, under
    their own licenses as listed above ("The JDK's resource data"): the Unicode Character
    Database's (`CharacterData*`, `uniName.dat`, the break iterators' data), ICU4J's (the
    normalizer, `nfc.nrm`, `nfkc.nrm`, `ubidi.icu`) and CLDR's (the locale bundles) under the
    Unicode License V3; the IANA time zone database's (`tzdb.dat`), in the public domain (in
    jdk26u's copies under its license). Of the JDK's other bundled third-party code (jdk26u's
    `src/java.base/share/legal/`), none is in them: zlib's is native code that the Go build
    does not translate (Go's `compress/zlib` inflates instead), nor are the public suffix list,
    SipHash, libutl, GCC's or the Thai dictionary (jdk.localedata).
  - Go: the runtime and the packages of the standard library the program imports (among them
    `runtime`, `os`, `syscall`, `net`, `time`, `compress/zlib`, `hash/crc32`, `crypto/md5`,
    `crypto/sha1`, `crypto/sha256`, `crypto/sha512`, `crypto/sha3`, `math`, `reflect`, `embed`),
    statically linked from TamaGo's go1.27.1 tree (https://github.com/usbarmory/tamago-go,
    `VERSION` "go1.27.1", 2026-08-28), with jrt's overlay of the runtime (`overlay/go/`, above):
    Copyright 2009 The Go Authors, under Go's BSD 3-Clause License (text below); the tree's
    `PATENTS` grants Google's patent license for it. A few of its files keep the notice of the
    code they derive from (as `src/math/log.go`, `exp.go` and the others from FreeBSD's msun:
    Copyright (C) 1993 by Sun Microsystems, Inc., "Permission to use, copy, modify, and
    distribute this software is freely granted, provided that this notice is preserved").

  The test build, `target/arbace-go/<arch>/arbace-tests` (`bin/arbace-go --build-tests`), adds
  Clojure's suite's Java fixtures (`test/java` of https://github.com/clojure/clojure at
  `98d735fab02f337cee654cb0629bddc09883a75a`, renamed by repr/clojure and converted by repr/java, as committed in
  `test/java-fixtures/forms/`): under the Eclipse Public License 1.0, as Clojure.

  So a distribution of an executable carries this file (the texts of the Eclipse Public License
  1.0, the GNU General Public License version 2 with the Classpath Exception, Go's BSD 3-Clause
  License, the Apache License 2.0 and the Unicode License V3, and the notices above), as the BSD
  3-Clause License, the Apache License 2.0 (§4) and the Unicode License V3 require for binary
  copies, and makes the source available: under the GNU General Public License version 2 (§3),
  the complete corresponding source of jdk26u's parts, with the executable or by a written
  offer (jdk26u at `baf63fb` and this repository at the revision built, whose tools convert and
  translate it); under the Eclipse Public License 1.0 (§3), a statement that the source of
  Arbace's parts is available and how to obtain it. The Classpath Exception lets the executable
  as a whole be distributed under terms of the distributor's choice, provided that the license
  of each linked independent module is also met; jdk26u's parts stay under the GNU General
  Public License version 2 with the Classpath Exception.
- The box (doc/go/BOX-NOTES.md, S1), `target/arbace-go/box-<arch>/`, which `bin/arbace-go --build
  --box` makes: the guest image `arbace.elf`, the same program as the executables above built
  with `GOOS=tamago`, and the launcher `arbace-box` (the monitor, a static Linux program, with
  the guest image appended). The guest holds what the executables hold (above), and besides:
  `box/`'s `arbace/box/board` and `abi` (the Eclipse Public License 1.0, as above), and TamaGo's
  `goos` module, https://github.com/usbarmory/tamago v1.27.1 (go.sum
  `h1:Zl4w3hu5DaogGvcLzunS4gjwNEqdjFFJedMO3pDQa5k=`; its `goos` package, the `runtime/goos`
  the build names as `GOOSPKG`, printed from `tamago/goos/` since S6, or with
  `ARBACE_BOX_GO=orig` linked from Go's module cache): Copyright (c) The TamaGo Authors, under
  the BSD 3-Clause License (text below); its Go is TamaGo's go1.27.1 runtime and standard
  library built for `GOOS=tamago`, printed from `tamago/goroot/` since S6 (Go's BSD 3-Clause
  License). The launcher holds `box/`'s `hv`, `vmm`,
  `abi` and the launcher's own package (the Eclipse Public License 1.0) and Go's runtime and
  standard library (Go's BSD 3-Clause License). A distribution of the box carries this file, as
  for the executables above.
- `seed/arbace-seed-go.tar.xz` (since S4, 2026-10-13; doc/STANDALONE-NOTES.md, "S4: the
  Go-source seed"): the seed of the bootstrap, Arbace's Go program as the Go-hosted fixpoint
  printed it, the source of the Go build's executables (above): the main package with its
  embedded resources (`mod/cmd/arbace/`), j2g's translation of the runtime (`mod/lang/`) and of
  the compiled namespaces (`mod/aot_*/`), jrt with the translated JDK classes (`mod/jrt/`), the
  x509roots bundle (`mod/x509roots/`) and Go's runtime as `overlay/go` patches it
  (`overlay/runtime/`). Each part is under the licenses of what it was translated or printed
  from, as given above: `arbace/` (the Eclipse Public License 1.0; `Murmur3` the Apache License
  2.0), `go/` and `overlay/jdk/` (the Eclipse Public License 1.0, but for their parts derived
  from jdk26u), jdk26u's classes and resource data (the GNU General Public License version 2
  with the Classpath Exception; the Unicode License V3 for their Unicode, ICU4J and CLDR data),
  `overlay/go/` (Go's BSD 3-Clause License) and `x509roots/` (Go's BSD 3-Clause License; the
  certificates under the Mozilla Public License 2.0). `seed/arbace-seed-go.tar.xz.sha256` and
  `seed/goroot.sha256` are Arbace's own, under the Eclipse Public License 1.0.
- `seed/arbace-seed.jar` (until S4, 2026-10-12; on the branch `arbace-for-java-26`): the binary
  seed of the bootstrap, Arbace as built at the tag
  `arbace-for-java-26-v1` (doc/VENDOR-NOTES.md, "The binary seed"): the `arbace/` sources of
  that tag and the classes compiled from them. Each entry is under the license of the `arbace/`
  file it is or is compiled from, as above (`arbace/asm/...` classes under ASM's BSD 3-Clause
  License, `arbace/lang/Murmur3*` under the Apache License 2.0, and so on).

## Arbace, Clojure, spec.alpha and core.specs.alpha: Eclipse Public License 1.0

Copyright (c) Rich Hickey. All rights reserved.

**Eclipse Public License - v 1.0**

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.

**1. DEFINITIONS**

"Contribution" means:

a) in the case of the initial Contributor, the initial code and documentation distributed under this Agreement, and

b) in the case of each subsequent Contributor:

i) changes to the Program, and

ii) additions to the Program;

where such changes and/or additions to the Program originate from and are distributed by that particular Contributor. A Contribution 'originates' from a Contributor if it was added to the Program by such Contributor itself or anyone acting on such Contributor's behalf. Contributions do not include additions to the Program which: (i) are separate modules of software distributed in conjunction with the Program under their own license agreement, and (ii) are not derivative works of the Program.

"Contributor" means any person or entity that distributes the Program.

"Licensed Patents" mean patent claims licensable by a Contributor which are necessarily infringed by the use or sale of its Contribution alone or when combined with the Program.

"Program" means the Contributions distributed in accordance with this Agreement.

"Recipient" means anyone who receives the Program under this Agreement, including all Contributors.

**2. GRANT OF RIGHTS**

a) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free copyright license to reproduce, prepare derivative works of, publicly display, publicly perform, distribute and sublicense the Contribution of such Contributor, if any, and such derivative works, in source code and object code form.

b) Subject to the terms of this Agreement, each Contributor hereby grants Recipient a non-exclusive, worldwide, royalty-free patent license under Licensed Patents to make, use, sell, offer to sell, import and otherwise transfer the Contribution of such Contributor, if any, in source code and object code form. This patent license shall apply to the combination of the Contribution and the Program if, at the time the Contribution is added by the Contributor, such addition of the Contribution causes such combination to be covered by the Licensed Patents. The patent license shall not apply to any other combinations which include the Contribution. No hardware per se is licensed hereunder.

c) Recipient understands that although each Contributor grants the licenses to its Contributions set forth herein, no assurances are provided by any Contributor that the Program does not infringe the patent or other intellectual property rights of any other entity. Each Contributor disclaims any liability to Recipient for claims brought by any other entity based on infringement of intellectual property rights or otherwise. As a condition to exercising the rights and licenses granted hereunder, each Recipient hereby assumes sole responsibility to secure any other intellectual property rights needed, if any. For example, if a third party patent license is required to allow Recipient to distribute the Program, it is Recipient's responsibility to acquire that license before distributing the Program.

d) Each Contributor represents that to its knowledge it has sufficient copyright rights in its Contribution, if any, to grant the copyright license set forth in this Agreement.

**3. REQUIREMENTS**

A Contributor may choose to distribute the Program in object code form under its own license agreement, provided that:

a) it complies with the terms and conditions of this Agreement; and

b) its license agreement:

i) effectively disclaims on behalf of all Contributors all warranties and conditions, express and implied, including warranties or conditions of title and non-infringement, and implied warranties or conditions of merchantability and fitness for a particular purpose;

ii) effectively excludes on behalf of all Contributors all liability for damages, including direct, indirect, special, incidental and consequential damages, such as lost profits;

iii) states that any provisions which differ from this Agreement are offered by that Contributor alone and not by any other party; and

iv) states that source code for the Program is available from such Contributor, and informs licensees how to obtain it in a reasonable manner on or through a medium customarily used for software exchange.

When the Program is made available in source code form:

a) it must be made available under this Agreement; and

b) a copy of this Agreement must be included with each copy of the Program.

Contributors may not remove or alter any copyright notices contained within the Program.

Each Contributor must identify itself as the originator of its Contribution, if any, in a manner that reasonably allows subsequent Recipients to identify the originator of the Contribution.

**4. COMMERCIAL DISTRIBUTION**

Commercial distributors of software may accept certain responsibilities with respect to end users, business partners and the like. While this license is intended to facilitate the commercial use of the Program, the Contributor who includes the Program in a commercial product offering should do so in a manner which does not create potential liability for other Contributors. Therefore, if a Contributor includes the Program in a commercial product offering, such Contributor ("Commercial Contributor") hereby agrees to defend and indemnify every other Contributor ("Indemnified Contributor") against any losses, damages and costs (collectively "Losses") arising from claims, lawsuits and other legal actions brought by a third party against the Indemnified Contributor to the extent caused by the acts or omissions of such Commercial Contributor in connection with its distribution of the Program in a commercial product offering. The obligations in this section do not apply to any claims or Losses relating to any actual or alleged intellectual property infringement. In order to qualify, an Indemnified Contributor must: a) promptly notify the Commercial Contributor in writing of such claim, and b) allow the Commercial Contributor to control, and cooperate with the Commercial Contributor in, the defense and any related settlement negotiations. The Indemnified Contributor may participate in any such claim at its own expense.

For example, a Contributor might include the Program in a commercial product offering, Product X. That Contributor is then a Commercial Contributor. If that Commercial Contributor then makes performance claims, or offers warranties related to Product X, those performance claims and warranties are such Commercial Contributor's responsibility alone. Under this section, the Commercial Contributor would have to defend claims against the other Contributors related to those performance claims and warranties, and if a court requires any other Contributor to pay any damages as a result, the Commercial Contributor must pay those damages.

**5. NO WARRANTY**

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, THE PROGRAM IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Each Recipient is solely responsible for determining the appropriateness of using and distributing the Program and assumes all risks associated with its exercise of rights under this Agreement , including but not limited to the risks and costs of program errors, compliance with applicable laws, damage to or loss of data, programs or equipment, and unavailability or interruption of operations.

**6. DISCLAIMER OF LIABILITY**

EXCEPT AS EXPRESSLY SET FORTH IN THIS AGREEMENT, NEITHER RECIPIENT NOR ANY CONTRIBUTORS SHALL HAVE ANY LIABILITY FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING WITHOUT LIMITATION LOST PROFITS), HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OR DISTRIBUTION OF THE PROGRAM OR THE EXERCISE OF ANY RIGHTS GRANTED HEREUNDER, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

**7. GENERAL**

If any provision of this Agreement is invalid or unenforceable under applicable law, it shall not affect the validity or enforceability of the remainder of the terms of this Agreement, and without further action by the parties hereto, such provision shall be reformed to the minimum extent necessary to make such provision valid and enforceable.

If Recipient institutes patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Program itself (excluding combinations of the Program with other software or hardware) infringes such Recipient's patent(s), then such Recipient's rights granted under Section 2(b) shall terminate as of the date such litigation is filed.

All Recipient's rights under this Agreement shall terminate if it fails to comply with any of the material terms or conditions of this Agreement and does not cure such failure in a reasonable period of time after becoming aware of such noncompliance. If all Recipient's rights under this Agreement terminate, Recipient agrees to cease use and distribution of the Program as soon as reasonably practicable. However, Recipient's obligations under this Agreement and any licenses granted by Recipient relating to the Program shall continue and survive.

Everyone is permitted to copy and distribute copies of this Agreement, but in order to avoid inconsistency the Agreement is copyrighted and may only be modified in the following manner. The Agreement Steward reserves the right to publish new versions (including revisions) of this Agreement from time to time. No one other than the Agreement Steward has the right to modify this Agreement. The Eclipse Foundation is the initial Agreement Steward. The Eclipse Foundation may assign the responsibility to serve as the Agreement Steward to a suitable separate entity. Each new version of the Agreement will be given a distinguishing version number. The Program (including Contributions) may always be distributed subject to the version of the Agreement under which it was received. In addition, after a new version of the Agreement is published, Contributor may elect to distribute the Program (including its Contributions) under the new version. Except as expressly stated in Sections 2(a) and 2(b) above, Recipient receives no rights or licenses to the intellectual property of any Contributor under this Agreement, whether expressly, by implication, estoppel or otherwise. All rights in the Program not expressly granted under this Agreement are reserved.

This Agreement is governed by the laws of the State of New York and the intellectual property laws of the United States of America. No party to this Agreement will bring a legal action under this Agreement more than one year after the cause of action arose. Each party waives its rights to a jury trial in any resulting litigation.

## ASM: BSD 3-Clause License

```
ASM: a very small and fast Java bytecode manipulation framework
Copyright (c) 2000-2011 INRIA, France Telecom
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the copyright holders nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.
```

## Go: BSD 3-Clause License

From the Go distribution's `LICENSE` (TamaGo's go1.27.1 tree):

```
Copyright 2009 The Go Authors.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

   * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
   * Neither the name of Google LLC nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## TamaGo's goos module: BSD 3-Clause License

From the `LICENSE` of https://github.com/usbarmory/tamago v1.27.1 (Go's module cache):

```
Copyright (c) The TamaGo Authors. All Rights Reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

   * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
   * Neither the name of Reversec Consulting AB nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## Murmur3 (Guava): Apache License 2.0

Copyright (C) 2011 The Guava Authors

```
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS
```

## purego: Apache License 2.0

Copyright 2022 The Ebitengine Authors (https://github.com/ebitengine/purego, v0.11.1). purego's
`LICENSE` is the Apache License, Version 2.0, January 2004, whose text is given in full in the
section "Murmur3 (Guava): Apache License 2.0" above (purego's file also carries the license's
appendix, "How to apply the Apache License to your work", which states no further terms).

## Mozilla NSS's root certificates: Mozilla Public License 2.0

The root certificates in `x509roots/go/arbace/x509roots/fallback/bundle/bundle.der` come from
Mozilla's NSS (`certdata.txt`, https://hg.mozilla.org/projects/nss, by way of
`golang.org/x/crypto/x509roots`), distributed unmodified. NSS is under the Mozilla Public License
Version 2.0:

```
Mozilla Public License Version 2.0
==================================

1. Definitions
--------------

1.1. "Contributor"
    means each individual or legal entity that creates, contributes to
    the creation of, or owns Covered Software.

1.2. "Contributor Version"
    means the combination of the Contributions of others (if any) used
    by a Contributor and that particular Contributor's Contribution.

1.3. "Contribution"
    means Covered Software of a particular Contributor.

1.4. "Covered Software"
    means Source Code Form to which the initial Contributor has attached
    the notice in Exhibit A, the Executable Form of such Source Code
    Form, and Modifications of such Source Code Form, in each case
    including portions thereof.

1.5. "Incompatible With Secondary Licenses"
    means

    (a) that the initial Contributor has attached the notice described
        in Exhibit B to the Covered Software; or

    (b) that the Covered Software was made available under the terms of
        version 1.1 or earlier of the License, but not also under the
        terms of a Secondary License.

1.6. "Executable Form"
    means any form of the work other than Source Code Form.

1.7. "Larger Work"
    means a work that combines Covered Software with other material, in
    a separate file or files, that is not Covered Software.

1.8. "License"
    means this document.

1.9. "Licensable"
    means having the right to grant, to the maximum extent possible,
    whether at the time of the initial grant or subsequently, any and
    all of the rights conveyed by this License.

1.10. "Modifications"
    means any of the following:

    (a) any file in Source Code Form that results from an addition to,
        deletion from, or modification of the contents of Covered
        Software; or

    (b) any new file in Source Code Form that contains any Covered
        Software.

1.11. "Patent Claims" of a Contributor
    means any patent claim(s), including without limitation, method,
    process, and apparatus claims, in any patent Licensable by such
    Contributor that would be infringed, but for the grant of the
    License, by the making, using, selling, offering for sale, having
    made, import, or transfer of either its Contributions or its
    Contributor Version.

1.12. "Secondary License"
    means either the GNU General Public License, Version 2.0, the GNU
    Lesser General Public License, Version 2.1, the GNU Affero General
    Public License, Version 3.0, or any later versions of those
    licenses.

1.13. "Source Code Form"
    means the form of the work preferred for making modifications.

1.14. "You" (or "Your")
    means an individual or a legal entity exercising rights under this
    License. For legal entities, "You" includes any entity that
    controls, is controlled by, or is under common control with You. For
    purposes of this definition, "control" means (a) the power, direct
    or indirect, to cause the direction or management of such entity,
    whether by contract or otherwise, or (b) ownership of more than
    fifty percent (50%) of the outstanding shares or beneficial
    ownership of such entity.

2. License Grants and Conditions
--------------------------------

2.1. Grants

Each Contributor hereby grants You a world-wide, royalty-free,
non-exclusive license:

(a) under intellectual property rights (other than patent or trademark)
    Licensable by such Contributor to use, reproduce, make available,
    modify, display, perform, distribute, and otherwise exploit its
    Contributions, either on an unmodified basis, with Modifications, or
    as part of a Larger Work; and

(b) under Patent Claims of such Contributor to make, use, sell, offer
    for sale, have made, import, and otherwise transfer either its
    Contributions or its Contributor Version.

2.2. Effective Date

The licenses granted in Section 2.1 with respect to any Contribution
become effective for each Contribution on the date the Contributor first
distributes such Contribution.

2.3. Limitations on Grant Scope

The licenses granted in this Section 2 are the only rights granted under
this License. No additional rights or licenses will be implied from the
distribution or licensing of Covered Software under this License.
Notwithstanding Section 2.1(b) above, no patent license is granted by a
Contributor:

(a) for any code that a Contributor has removed from Covered Software;
    or

(b) for infringements caused by: (i) Your and any other third party's
    modifications of Covered Software, or (ii) the combination of its
    Contributions with other software (except as part of its Contributor
    Version); or

(c) under Patent Claims infringed by Covered Software in the absence of
    its Contributions.

This License does not grant any rights in the trademarks, service marks,
or logos of any Contributor (except as may be necessary to comply with
the notice requirements in Section 3.4).

2.4. Subsequent Licenses

No Contributor makes additional grants as a result of Your choice to
distribute the Covered Software under a subsequent version of this
License (see Section 10.2) or under the terms of a Secondary License (if
permitted under the terms of Section 3.3).

2.5. Representation

Each Contributor represents that the Contributor believes its
Contributions are its original creation(s) or it has sufficient rights
to grant the rights to its Contributions conveyed by this License.

2.6. Fair Use

This License is not intended to limit any rights You have under
applicable copyright doctrines of fair use, fair dealing, or other
equivalents.

2.7. Conditions

Sections 3.1, 3.2, 3.3, and 3.4 are conditions of the licenses granted
in Section 2.1.

3. Responsibilities
-------------------

3.1. Distribution of Source Form

All distribution of Covered Software in Source Code Form, including any
Modifications that You create or to which You contribute, must be under
the terms of this License. You must inform recipients that the Source
Code Form of the Covered Software is governed by the terms of this
License, and how they can obtain a copy of this License. You may not
attempt to alter or restrict the recipients' rights in the Source Code
Form.

3.2. Distribution of Executable Form

If You distribute Covered Software in Executable Form then:

(a) such Covered Software must also be made available in Source Code
    Form, as described in Section 3.1, and You must inform recipients of
    the Executable Form how they can obtain a copy of such Source Code
    Form by reasonable means in a timely manner, at a charge no more
    than the cost of distribution to the recipient; and

(b) You may distribute such Executable Form under the terms of this
    License, or sublicense it under different terms, provided that the
    license for the Executable Form does not attempt to limit or alter
    the recipients' rights in the Source Code Form under this License.

3.3. Distribution of a Larger Work

You may create and distribute a Larger Work under terms of Your choice,
provided that You also comply with the requirements of this License for
the Covered Software. If the Larger Work is a combination of Covered
Software with a work governed by one or more Secondary Licenses, and the
Covered Software is not Incompatible With Secondary Licenses, this
License permits You to additionally distribute such Covered Software
under the terms of such Secondary License(s), so that the recipient of
the Larger Work may, at their option, further distribute the Covered
Software under the terms of either this License or such Secondary
License(s).

3.4. Notices

You may not remove or alter the substance of any license notices
(including copyright notices, patent notices, disclaimers of warranty,
or limitations of liability) contained within the Source Code Form of
the Covered Software, except that You may alter any license notices to
the extent required to remedy known factual inaccuracies.

3.5. Application of Additional Terms

You may choose to offer, and to charge a fee for, warranty, support,
indemnity or liability obligations to one or more recipients of Covered
Software. However, You may do so only on Your own behalf, and not on
behalf of any Contributor. You must make it absolutely clear that any
such warranty, support, indemnity, or liability obligation is offered by
You alone, and You hereby agree to indemnify every Contributor for any
liability incurred by such Contributor as a result of warranty, support,
indemnity or liability terms You offer. You may include additional
disclaimers of warranty and limitations of liability specific to any
jurisdiction.

4. Inability to Comply Due to Statute or Regulation
---------------------------------------------------

If it is impossible for You to comply with any of the terms of this
License with respect to some or all of the Covered Software due to
statute, judicial order, or regulation then You must: (a) comply with
the terms of this License to the maximum extent possible; and (b)
describe the limitations and the code they affect. Such description must
be placed in a text file included with all distributions of the Covered
Software under this License. Except to the extent prohibited by statute
or regulation, such description must be sufficiently detailed for a
recipient of ordinary skill to be able to understand it.

5. Termination
--------------

5.1. The rights granted under this License will terminate automatically
if You fail to comply with any of its terms. However, if You become
compliant, then the rights granted under this License from a particular
Contributor are reinstated (a) provisionally, unless and until such
Contributor explicitly and finally terminates Your grants, and (b) on an
ongoing basis, if such Contributor fails to notify You of the
non-compliance by some reasonable means prior to 60 days after You have
come back into compliance. Moreover, Your grants from a particular
Contributor are reinstated on an ongoing basis if such Contributor
notifies You of the non-compliance by some reasonable means, this is the
first time You have received notice of non-compliance with this License
from such Contributor, and You become compliant prior to 30 days after
Your receipt of the notice.

5.2. If You initiate litigation against any entity by asserting a patent
infringement claim (excluding declaratory judgment actions,
counter-claims, and cross-claims) alleging that a Contributor Version
directly or indirectly infringes any patent, then the rights granted to
You by any and all Contributors for the Covered Software under Section
2.1 of this License shall terminate.

5.3. In the event of termination under Sections 5.1 or 5.2 above, all
end user license agreements (excluding distributors and resellers) which
have been validly granted by You or Your distributors under this License
prior to termination shall survive termination.

************************************************************************
*                                                                      *
*  6. Disclaimer of Warranty                                           *
*  -------------------------                                           *
*                                                                      *
*  Covered Software is provided under this License on an "as is"       *
*  basis, without warranty of any kind, either expressed, implied, or  *
*  statutory, including, without limitation, warranties that the       *
*  Covered Software is free of defects, merchantable, fit for a        *
*  particular purpose or non-infringing. The entire risk as to the     *
*  quality and performance of the Covered Software is with You.        *
*  Should any Covered Software prove defective in any respect, You     *
*  (not any Contributor) assume the cost of any necessary servicing,   *
*  repair, or correction. This disclaimer of warranty constitutes an   *
*  essential part of this License. No use of any Covered Software is   *
*  authorized under this License except under this disclaimer.         *
*                                                                      *
************************************************************************

************************************************************************
*                                                                      *
*  7. Limitation of Liability                                          *
*  --------------------------                                          *
*                                                                      *
*  Under no circumstances and under no legal theory, whether tort      *
*  (including negligence), contract, or otherwise, shall any           *
*  Contributor, or anyone who distributes Covered Software as          *
*  permitted above, be liable to You for any direct, indirect,         *
*  special, incidental, or consequential damages of any character      *
*  including, without limitation, damages for lost profits, loss of    *
*  goodwill, work stoppage, computer failure or malfunction, or any    *
*  and all other commercial damages or losses, even if such party      *
*  shall have been informed of the possibility of such damages. This   *
*  limitation of liability shall not apply to liability for death or   *
*  personal injury resulting from such party's negligence to the       *
*  extent applicable law prohibits such limitation. Some               *
*  jurisdictions do not allow the exclusion or limitation of           *
*  incidental or consequential damages, so this exclusion and          *
*  limitation may not apply to You.                                    *
*                                                                      *
************************************************************************

8. Litigation
-------------

Any litigation relating to this License may be brought only in the
courts of a jurisdiction where the defendant maintains its principal
place of business and such litigation shall be governed by laws of that
jurisdiction, without reference to its conflict-of-law provisions.
Nothing in this Section shall prevent a party's ability to bring
cross-claims or counter-claims.

9. Miscellaneous
----------------

This License represents the complete agreement concerning the subject
matter hereof. If any provision of this License is held to be
unenforceable, such provision shall be reformed only to the extent
necessary to make it enforceable. Any law or regulation which provides
that the language of a contract shall be construed against the drafter
shall not be used to construe this License against a Contributor.

10. Versions of the License
---------------------------

10.1. New Versions

Mozilla Foundation is the license steward. Except as provided in Section
10.3, no one other than the license steward has the right to modify or
publish new versions of this License. Each version will be given a
distinguishing version number.

10.2. Effect of New Versions

You may distribute the Covered Software under the terms of the version
of the License under which You originally received the Covered Software,
or under the terms of any subsequent version published by the license
steward.

10.3. Modified Versions

If you create software not governed by this License, and you want to
create a new license for such software, you may create and use a
modified version of this License if you rename the license and remove
any references to the name of the license steward (except to note that
such modified license differs from this License).

10.4. Distributing Source Code Form that is Incompatible With Secondary
Licenses

If You choose to distribute Source Code Form that is Incompatible With
Secondary Licenses under the terms of this version of the License, the
notice described in Exhibit B of this License must be attached.

Exhibit A - Source Code Form License Notice
-------------------------------------------

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.

If it is not possible or desirable to put the notice in a particular
file, then You may include the notice in a location (such as a LICENSE
file in a relevant directory) where a recipient would be likely to look
for such a notice.

You may add additional accurate notices of copyright ownership.

Exhibit B - "Incompatible With Secondary Licenses" Notice
---------------------------------------------------------

  This Source Code Form is "Incompatible With Secondary Licenses", as
  defined by the Mozilla Public License, v. 2.0.
```

## OpenJDK (jdk26u): GNU General Public License version 2 with the Classpath Exception

The text of https://github.com/openjdk/jdk26u `LICENSE` at `baf63fb`:

```
The GNU General Public License (GPL)

Version 2, June 1991

Copyright (C) 1989, 1991 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

Everyone is permitted to copy and distribute verbatim copies of this license
document, but changing it is not allowed.

Preamble

The licenses for most software are designed to take away your freedom to share
and change it.  By contrast, the GNU General Public License is intended to
guarantee your freedom to share and change free software--to make sure the
software is free for all its users.  This General Public License applies to
most of the Free Software Foundation's software and to any other program whose
authors commit to using it.  (Some other Free Software Foundation software is
covered by the GNU Library General Public License instead.) You can apply it to
your programs, too.

When we speak of free software, we are referring to freedom, not price.  Our
General Public Licenses are designed to make sure that you have the freedom to
distribute copies of free software (and charge for this service if you wish),
that you receive source code or can get it if you want it, that you can change
the software or use pieces of it in new free programs; and that you know you
can do these things.

To protect your rights, we need to make restrictions that forbid anyone to deny
you these rights or to ask you to surrender the rights.  These restrictions
translate to certain responsibilities for you if you distribute copies of the
software, or if you modify it.

For example, if you distribute copies of such a program, whether gratis or for
a fee, you must give the recipients all the rights that you have.  You must
make sure that they, too, receive or can get the source code.  And you must
show them these terms so they know their rights.

We protect your rights with two steps: (1) copyright the software, and (2)
offer you this license which gives you legal permission to copy, distribute
and/or modify the software.

Also, for each author's protection and ours, we want to make certain that
everyone understands that there is no warranty for this free software.  If the
software is modified by someone else and passed on, we want its recipients to
know that what they have is not the original, so that any problems introduced
by others will not reflect on the original authors' reputations.

Finally, any free program is threatened constantly by software patents.  We
wish to avoid the danger that redistributors of a free program will
individually obtain patent licenses, in effect making the program proprietary.
To prevent this, we have made it clear that any patent must be licensed for
everyone's free use or not licensed at all.

The precise terms and conditions for copying, distribution and modification
follow.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License applies to any program or other work which contains a notice
placed by the copyright holder saying it may be distributed under the terms of
this General Public License.  The "Program", below, refers to any such program
or work, and a "work based on the Program" means either the Program or any
derivative work under copyright law: that is to say, a work containing the
Program or a portion of it, either verbatim or with modifications and/or
translated into another language.  (Hereinafter, translation is included
without limitation in the term "modification".) Each licensee is addressed as
"you".

Activities other than copying, distribution and modification are not covered by
this License; they are outside its scope.  The act of running the Program is
not restricted, and the output from the Program is covered only if its contents
constitute a work based on the Program (independent of having been made by
running the Program).  Whether that is true depends on what the Program does.

1. You may copy and distribute verbatim copies of the Program's source code as
you receive it, in any medium, provided that you conspicuously and
appropriately publish on each copy an appropriate copyright notice and
disclaimer of warranty; keep intact all the notices that refer to this License
and to the absence of any warranty; and give any other recipients of the
Program a copy of this License along with the Program.

You may charge a fee for the physical act of transferring a copy, and you may
at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Program or any portion of it, thus
forming a work based on the Program, and copy and distribute such modifications
or work under the terms of Section 1 above, provided that you also meet all of
these conditions:

    a) You must cause the modified files to carry prominent notices stating
    that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in whole or
    in part contains or is derived from the Program or any part thereof, to be
    licensed as a whole at no charge to all third parties under the terms of
    this License.

    c) If the modified program normally reads commands interactively when run,
    you must cause it, when started running for such interactive use in the
    most ordinary way, to print or display an announcement including an
    appropriate copyright notice and a notice that there is no warranty (or
    else, saying that you provide a warranty) and that users may redistribute
    the program under these conditions, and telling the user how to view a copy
    of this License.  (Exception: if the Program itself is interactive but does
    not normally print such an announcement, your work based on the Program is
    not required to print an announcement.)

These requirements apply to the modified work as a whole.  If identifiable
sections of that work are not derived from the Program, and can be reasonably
considered independent and separate works in themselves, then this License, and
its terms, do not apply to those sections when you distribute them as separate
works.  But when you distribute the same sections as part of a whole which is a
work based on the Program, the distribution of the whole must be on the terms
of this License, whose permissions for other licensees extend to the entire
whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest your
rights to work written entirely by you; rather, the intent is to exercise the
right to control the distribution of derivative or collective works based on
the Program.

In addition, mere aggregation of another work not based on the Program with the
Program (or with a work based on the Program) on a volume of a storage or
distribution medium does not bring the other work under the scope of this
License.

3. You may copy and distribute the Program (or a work based on it, under
Section 2) in object code or executable form under the terms of Sections 1 and
2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable source
    code, which must be distributed under the terms of Sections 1 and 2 above
    on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three years, to
    give any third party, for a charge no more than your cost of physically
    performing source distribution, a complete machine-readable copy of the
    corresponding source code, to be distributed under the terms of Sections 1
    and 2 above on a medium customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer to
    distribute corresponding source code.  (This alternative is allowed only
    for noncommercial distribution and only if you received the program in
    object code or executable form with such an offer, in accord with
    Subsection b above.)

The source code for a work means the preferred form of the work for making
modifications to it.  For an executable work, complete source code means all
the source code for all modules it contains, plus any associated interface
definition files, plus the scripts used to control compilation and installation
of the executable.  However, as a special exception, the source code
distributed need not include anything that is normally distributed (in either
source or binary form) with the major components (compiler, kernel, and so on)
of the operating system on which the executable runs, unless that component
itself accompanies the executable.

If distribution of executable or object code is made by offering access to copy
from a designated place, then offering equivalent access to copy the source
code from the same place counts as distribution of the source code, even though
third parties are not compelled to copy the source along with the object code.

4. You may not copy, modify, sublicense, or distribute the Program except as
expressly provided under this License.  Any attempt otherwise to copy, modify,
sublicense or distribute the Program is void, and will automatically terminate
your rights under this License.  However, parties who have received copies, or
rights, from you under this License will not have their licenses terminated so
long as such parties remain in full compliance.

5. You are not required to accept this License, since you have not signed it.
However, nothing else grants you permission to modify or distribute the Program
or its derivative works.  These actions are prohibited by law if you do not
accept this License.  Therefore, by modifying or distributing the Program (or
any work based on the Program), you indicate your acceptance of this License to
do so, and all its terms and conditions for copying, distributing or modifying
the Program or works based on it.

6. Each time you redistribute the Program (or any work based on the Program),
the recipient automatically receives a license from the original licensor to
copy, distribute or modify the Program subject to these terms and conditions.
You may not impose any further restrictions on the recipients' exercise of the
rights granted herein.  You are not responsible for enforcing compliance by
third parties to this License.

7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues), conditions
are imposed on you (whether by court order, agreement or otherwise) that
contradict the conditions of this License, they do not excuse you from the
conditions of this License.  If you cannot distribute so as to satisfy
simultaneously your obligations under this License and any other pertinent
obligations, then as a consequence you may not distribute the Program at all.
For example, if a patent license would not permit royalty-free redistribution
of the Program by all those who receive copies directly or indirectly through
you, then the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under any
particular circumstance, the balance of the section is intended to apply and
the section as a whole is intended to apply in other circumstances.

It is not the purpose of this section to induce you to infringe any patents or
other property right claims or to contest validity of any such claims; this
section has the sole purpose of protecting the integrity of the free software
distribution system, which is implemented by public license practices.  Many
people have made generous contributions to the wide range of software
distributed through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing to
distribute software through any other system and a licensee cannot impose that
choice.

This section is intended to make thoroughly clear what is believed to be a
consequence of the rest of this License.

8. If the distribution and/or use of the Program is restricted in certain
countries either by patents or by copyrighted interfaces, the original
copyright holder who places the Program under this License may add an explicit
geographical distribution limitation excluding those countries, so that
distribution is permitted only in or among countries not thus excluded.  In
such case, this License incorporates the limitation as if written in the body
of this License.

9. The Free Software Foundation may publish revised and/or new versions of the
General Public License from time to time.  Such new versions will be similar in
spirit to the present version, but may differ in detail to address new problems
or concerns.

Each version is given a distinguishing version number.  If the Program
specifies a version number of this License which applies to it and "any later
version", you have the option of following the terms and conditions either of
that version or of any later version published by the Free Software Foundation.
If the Program does not specify a version number of this License, you may
choose any version ever published by the Free Software Foundation.

10. If you wish to incorporate parts of the Program into other free programs
whose distribution conditions are different, write to the author to ask for
permission.  For software which is copyrighted by the Free Software Foundation,
write to the Free Software Foundation; we sometimes make exceptions for this.
Our decision will be guided by the two goals of preserving the free status of
all derivatives of our free software and of promoting the sharing and reuse of
software generally.

NO WARRANTY

11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR
THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.  EXCEPT WHEN OTHERWISE
STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE
PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND
PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE,
YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL
ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE
PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY
GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR
INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA
BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A
FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER
OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

END OF TERMS AND CONDITIONS

How to Apply These Terms to Your New Programs

If you develop a new program, and you want it to be of the greatest possible
use to the public, the best way to achieve this is to make it free software
which everyone can redistribute and change under these terms.

To do so, attach the following notices to the program.  It is safest to attach
them to the start of each source file to most effectively convey the exclusion
of warranty; and each file should have at least the "copyright" line and a
pointer to where the full notice is found.

    One line to give the program's name and a brief idea of what it does.

    Copyright (C) <year> <name of author>

    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation; either version 2 of the License, or (at your option)
    any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
    more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this when it
starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author Gnomovision comes
    with ABSOLUTELY NO WARRANTY; for details type 'show w'.  This is free
    software, and you are welcome to redistribute it under certain conditions;
    type 'show c' for details.

The hypothetical commands 'show w' and 'show c' should show the appropriate
parts of the General Public License.  Of course, the commands you use may be
called something other than 'show w' and 'show c'; they could even be
mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your school,
if any, to sign a "copyright disclaimer" for the program, if necessary.  Here
is a sample; alter the names:

    Yoyodyne, Inc., hereby disclaims all copyright interest in the program
    'Gnomovision' (which makes passes at compilers) written by James Hacker.

    signature of Ty Coon, 1 April 1989

    Ty Coon, President of Vice

This General Public License does not permit incorporating your program into
proprietary programs.  If your program is a subroutine library, you may
consider it more useful to permit linking proprietary applications with the
library.  If this is what you want to do, use the GNU Library General Public
License instead of this License.


"CLASSPATH" EXCEPTION TO THE GPL

Certain source files distributed by Oracle America and/or its affiliates are
subject to the following clarification and special exception to the GPL, but
only where Oracle has expressly included in the particular source file's header
the words "Oracle designates this particular file as subject to the "Classpath"
exception as provided by Oracle in the LICENSE file that accompanied this code."

    Linking this library statically or dynamically with other modules is making
    a combined work based on this library.  Thus, the terms and conditions of
    the GNU General Public License cover the whole combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent modules,
    and to copy and distribute the resulting executable under terms of your
    choice, provided that you also meet, for each linked independent module,
    the terms and conditions of the license of that module.  An independent
    module is a module which is not derived from or based on this library.  If
    you modify this library, you may extend this exception to your version of
    the library, but you are not obligated to do so.  If you do not wish to do
    so, delete this exception statement from your version.
```

## ICU4J, the Unicode Character Database and CLDR (in jdk26u): Unicode License V3

The ICU4J License, the text of https://github.com/openjdk/jdk26u
`src/java.base/share/legal/icu.md` at `baf63fb` (International Components for Unicode (ICU4J)
v78.1):

```
UNICODE LICENSE V3

COPYRIGHT AND PERMISSION NOTICE

Copyright © 2016-2025 Unicode, Inc.

NOTICE TO USER: Carefully read the following legal agreement. BY
DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.

Permission is hereby granted, free of charge, to any person obtaining a
copy of data files and any associated documentation (the "Data Files") or
software and any associated documentation (the "Software") to deal in the
Data Files or Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, and/or sell
copies of the Data Files or Software, and to permit persons to whom the
Data Files or Software are furnished to do so, provided that either (a)
this copyright and permission notice appear with all copies of the Data
Files or Software, or (b) this copyright and permission notice appear in
associated Documentation.

THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
THIRD PARTY RIGHTS.

IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall
not be used in advertising or otherwise to promote the sale, use or other
dealings in these Data Files or Software without prior written
authorization of the copyright holder.

SPDX-License-Identifier: Unicode-3.0

----------------------------------------------------------------------

Third-Party Software Licenses

This section contains third-party software notices and/or additional
terms for licensed third-party software components included within ICU
libraries.

----------------------------------------------------------------------

ICU License - ICU 1.8.1 to ICU 57.1

COPYRIGHT AND PERMISSION NOTICE

Copyright (c) 1995-2016 International Business Machines Corporation and others
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, and/or sell copies of the Software, and to permit persons
to whom the Software is furnished to do so, provided that the above
copyright notice(s) and this permission notice appear in all copies of
the Software and that both the above copyright notice(s) and this
permission notice appear in supporting documentation.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
HOLDERS INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY
SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER
RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

Except as contained in this notice, the name of a copyright holder
shall not be used in advertising or otherwise to promote the sale, use
or other dealings in this Software without prior written authorization
of the copyright holder.

All trademarks and registered trademarks mentioned herein are the
property of their respective owners.

```


The Unicode Character Database's license, the text of https://github.com/openjdk/jdk26u
`src/java.base/share/legal/unicode.md` at `baf63fb` (The Unicode Standard, Unicode Character
Database, Version 17.0.0), up to its Terms of Use, which are Unicode's website's:

```

UNICODE LICENSE V3

COPYRIGHT AND PERMISSION NOTICE

Copyright © 1991-2025 Unicode, Inc.

NOTICE TO USER: Carefully read the following legal agreement. BY
DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.

Permission is hereby granted, free of charge, to any person obtaining a
copy of data files and any associated documentation (the "Data Files") or
software and any associated documentation (the "Software") to deal in the
Data Files or Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, and/or sell
copies of the Data Files or Software, and to permit persons to whom the
Data Files or Software are furnished to do so, provided that either (a)
this copyright and permission notice appear with all copies of the Data
Files or Software, or (b) this copyright and permission notice appear in
associated Documentation.

THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
THIRD PARTY RIGHTS.

IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall
not be used in advertising or otherwise to promote the sale, use or other
dealings in these Data Files or Software without prior written
authorization of the copyright holder.

```

CLDR's license, the text of https://github.com/openjdk/jdk26u `src/java.base/share/legal/cldr.md`
at `baf63fb` (Unicode Common Local Data Repository (CLDR) v48), its license block:

```
UNICODE LICENSE V3

COPYRIGHT AND PERMISSION NOTICE

Copyright © 1991-2025 Unicode, Inc.

NOTICE TO USER: Carefully read the following legal agreement. BY
DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.

Permission is hereby granted, free of charge, to any person obtaining a
copy of data files and any associated documentation (the "Data Files") or
software and any associated documentation (the "Software") to deal in the
Data Files or Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, and/or sell
copies of the Data Files or Software, and to permit persons to whom the
Data Files or Software are furnished to do so, provided that either (a)
this copyright and permission notice appear with all copies of the Data
Files or Software, or (b) this copyright and permission notice appear in
associated Documentation.

THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
THIRD PARTY RIGHTS.

IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall
not be used in advertising or otherwise to promote the sale, use or other
dealings in these Data Files or Software without prior written
authorization of the copyright holder.

SPDX-License-Identifier: Unicode-3.0

————————————

Unicode® Copyright and Terms of Use
For the general privacy policy governing access to this site, see the  Unicode Privacy Policy.

Unicode Copyright
Copyright © 1991-2025 Unicode, Inc. All rights reserved.
Definitions
Unicode Data Files ("DATA FILES") include all data files under the directories:
https://www.unicode.org/Public/
https://www.unicode.org/reports/
https://www.unicode.org/ivd/data/

Unicode Data Files do not include PDF online code charts under the directory:
https://www.unicode.org/Public/

Unicode Software ("SOFTWARE") includes any source code published in the Unicode Standard
or any source code or compiled code under the directories:
https://www.unicode.org/Public/PROGRAMS/
https://www.unicode.org/Public/cldr/
http://site.icu-project.org/download/
Terms of Use
Certain documents and files on this website contain a legend indicating that "Modification is permitted." Any person is hereby authorized, without fee, to modify such documents and files to create derivative works conforming to the Unicode® Standard, subject to Terms and Conditions herein.
Any person is hereby authorized, without fee, to view, use, reproduce, and distribute all documents and files, subject to the Terms and Conditions herein.
Further specifications of rights and restrictions pertaining to the use of the Unicode DATA FILES and SOFTWARE can be found in the Unicode Data Files and Software License.
Each version of the Unicode Standard has further specifications of rights and restrictions of use. For the book editions (Unicode 5.0 and earlier), these are found on the back of the title page.
The Unicode PDF online code charts carry specific restrictions. Those restrictions are incorporated as the first page of each PDF code chart.
All other files, including online documentation of the core specification for Unicode 6.0 and later, are covered under these general Terms of Use.
No license is granted to "mirror" the Unicode website where a fee is charged for access to the "mirror" site.
Modification is not permitted with respect to this document. All copies of this document must be verbatim.
Restricted Rights Legend
Any technical data or software which is licensed to the United States of America, its agencies and/or instrumentalities under this Agreement is commercial technical data or commercial computer software developed exclusively at private expense as defined in FAR 2.101, or DFARS 252.227-7014 (June 1995), as applicable. For technical data, use, duplication, or disclosure by the Government is subject to restrictions as set forth in DFARS 202.227-7015 Technical Data, Commercial and Items (Nov 1995) and this Agreement. For Software, in accordance with FAR 12-212 or DFARS 227-7202, as applicable, use, duplication or disclosure by the Government is subject to the restrictions set forth in this Agreement.
Warranties and Disclaimers
This publication and/or website may include technical or typographical errors or other inaccuracies. Changes are periodically added to the information herein; these changes will be incorporated in new editions of the publication and/or website. Unicode, Inc. may make improvements and/or changes in the product(s) and/or program(s) described in this publication and/or website at any time.
If this file has been purchased on magnetic or optical media from Unicode, Inc. the sole and exclusive remedy for any claim will be exchange of the defective media within ninety (90) days of original purchase.
EXCEPT AS PROVIDED IN SECTION E.2, THIS PUBLICATION AND/OR SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND EITHER EXPRESS, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. UNICODE, INC. AND ITS LICENSORS ASSUME NO RESPONSIBILITY FOR ERRORS OR OMISSIONS IN THIS PUBLICATION AND/OR SOFTWARE OR OTHER DOCUMENTS WHICH ARE REFERENCED BY OR LINKED TO THIS PUBLICATION OR THE UNICODE WEBSITE.
Waiver of Damages
In no event shall Unicode, Inc. or its licensors be liable for any special, incidental, indirect or consequential damages of any kind, or any damages whatsoever, whether or not Unicode, Inc. was advised of the possibility of the damage, including, without limitation, those resulting from the following: loss of use, data or profits, in connection with the use, modification or distribution of this information or its derivatives.
Trademarks & Logos
The Unicode Word Mark and the Unicode Logo are trademarks of Unicode, Inc. “The Unicode Consortium” and “Unicode, Inc.” are trade names of Unicode, Inc. Use of the information and materials found on this website indicates your acknowledgement of Unicode, Inc.’s exclusive worldwide rights in the Unicode Word Mark, the Unicode Logo, and the Unicode trade names.
The Unicode Consortium Name and Trademark Usage Policy (“Trademark Policy”) are incorporated herein by reference and you agree to abide by the provisions of the Trademark Policy, which may be changed from time to time in the sole discretion of Unicode, Inc.
All third party trademarks referenced herein are the property of their respective owners.
Miscellaneous
Jurisdiction and Venue. This website is operated from a location in the State of California, United States of America. Unicode, Inc. makes no representation that the materials are appropriate for use in other locations. If you access this website from other locations, you are responsible for compliance with local laws. This Agreement, all use of this website and any claims and damages resulting from use of this website are governed solely by the laws of the State of California without regard to any principles which would apply the laws of a different jurisdiction. The user agrees that any disputes regarding this website shall be resolved solely in the courts located in Santa Clara County, California. The user agrees said courts have personal jurisdiction and agree to waive any right to transfer the dispute to any other forum.
Modification by Unicode, Inc. Unicode, Inc. shall have the right to modify this Agreement at any time by posting it to this website. The user may not assign any part of this Agreement without Unicode, Inc.’s prior written consent.
Taxes. The user agrees to pay any taxes arising from access to this website or use of the information herein, except for those based on Unicode’s net income.
Severability.  If any provision of this Agreement is declared invalid or unenforceable, the remaining provisions of this Agreement shall remain in effect.
Entire Agreement. This Agreement constitutes the entire agreement between the parties.
```

## zlib: zlib License

Covers `overlay/jdk/javac/java.base/java/util/zip/Deflater.java` (a port of zlib's compressor) and
the form file converted from it (`jdk/javac/forms/java/util/zip/Deflater.ae`). The license, as
zlib 1.3.2's `zlib.h` states it (jdk26u's `src/java.base/share/native/libzip/zlib/zlib.h`):

```
  Copyright (C) 1995-2026 Jean-loup Gailly and Mark Adler

  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.

  Jean-loup Gailly        Mark Adler
  jloup@gzip.org          madler@alumni.caltech.edu
```
