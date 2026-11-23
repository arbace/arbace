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
  for repr/java's native lexer and parser, ports of jdk26u's javac, repr/c's lexer and
  preprocessor, ports of cc, repr/rust's native lexer and parser, ports of rustc, and repr/haskell's native
  reader, ports of GHC, below) is
  derived from Clojure, https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a`,
  with ASM, https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`: the frozen tree `clojure/` that this repository
  held until the freeze (it remains on the branch `arbace-for-java-26`), renamed to `arbace.*`,
  with its Java converted to Clojure Java forms (doc/VENDOR-NOTES.md). Each file stays under its
  source's license:
  - ASM, under the BSD 3-Clause License (the package renamed from `org.objectweb.asm`, by way
    of `clojure.asm`, to `arbace.asm`), was `arbace/asm/` and `arbace/asm.ae` until 2026-10-12,
    when it left main with the JVM's bytecode back end (doc/STANDALONE-NOTES.md, S4; it remains
    on the branch `arbace-for-java-26`). Since 2026-11-10 `arbace/asm/` holds the assembly crop's
    assembler instead (Apache-2.0 WITH LLVM-exception, below), nothing of ASM. No file of main is derived from it now; its license
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
- `go/arbace/repr/go/internal/gc/` (doc/go/GOI-SPEC.md, "Since v1: gc in the executable", 2026-11-22): a renamed
  copy of `tamago/cmd/`'s forms of `cmd/compile`'s closure (`cmd/compile/internal/...`, `cmd/internal/...` and
  `cmd/vendor/golang.org/x/telemetry/...`: above, the same tree's `src/cmd/`) and of `tamago/goroot/`'s forms of
  the standard library's internal packages it imports (`internal/abi`, `internal/buildcfg` ... : the same tree's
  `src/internal/`), their namespaces and import paths renamed under `arbace/repr/go/internal/gc/` by
  `arbace.go.gc/copy-texts` (its docstring lists the few other changes: test assembly and `internal/abi`'s
  `funcpc.go` left out, two configuration-dependent files' `:val` annotations removed): Copyright The Go Authors,
  under Go's BSD 3-Clause License (text below); each file's notice kept. `go/arbace/repr/go/gccompile/compile.ae`'s
  `Compile` is `src/cmd/compile/main.go`'s `main` of the same tree, ported as forms (its notice kept): Copyright
  2015 The Go Authors, under Go's BSD 3-Clause License (text below).
- `wasm/web/arbace.js`'s part 1 (the gojs imports and the `Go` class; doc/wasm/WEB.md; doc/wasm/WASM-SURVEY.md, "As built:
  WA11, WA12 (begun)") and wai's headless world `go/arbace/wai/gojs.ae` (the same imports, the reference table and the run
  written in Go after it): derived from TamaGo's go1.27.1 tree's `lib/wasm/wasm_exec.js` and `lib/wasm/wasm_exec_node.js`
  (above: `VERSION` "go1.27.1"), Copyright 2018 and 2021 The Go Authors, under Go's BSD 3-Clause License (text below); the
  notice is kept at the top of `arbace.js`. The rest of `arbace.js`, `wasm/web/index.html` and wai's `js*.ae`, `gojsnode.ae`
  and `gojspage.ae` are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/go/escape.ae` (doc/go/GOI-SPEC.md, "Since v1: gc's escape analysis and inliner"): a port to Arbace of
  gc's escape analysis and inliner from TamaGo's go1.27.1 tree (above: `VERSION` "go1.27.1"):
  `src/cmd/compile/internal/escape/` (`escape.go`, `graph.go`, `solve.go`, `expr.go`, `stmt.go`, `assign.go`,
  `call.go`, `utils.go`, `leaks.go`), `inline/inl.go`, `inline/interleaved/interleaved.go`,
  `devirtualize/devirtualize.go`, `ir/scc.go`, `ir/reassignment.go`, `ir/fmt.go`, `types/fmt.go`,
  `loopvar/loopvar.go`, `typecheck/stmt.go`, `typecheck/subr.go` (`convertOp`), `walk/expr.go`,
  `noder/writer.go` and `ssagen/intrinsics.go` (the amd64 entries of its table, as data), written anew in
  Arbace after them: Copyright The Go Authors, under Go's BSD 3-Clause License (text below).
- `go/arbace/repr/go/gcsyntax/errors.ae` (doc/go/GO-PARSER-SURVEY.md, "As built: GPS41 (c)"):
  besides Arbace's own driver, a port of gc's noder's pragma handling from TamaGo's go1.27.1 tree
  (above: `VERSION` "go1.27.1"): `src/cmd/compile/internal/noder/noder.go` (`pragma`,
  `checkUnusedDuringParse`, `isCgoGeneratedFile`, `safeArg`, `parseGoEmbed`, `checkEmbed`),
  `lex.go` (`pragmaFlag`, `pragcgo`, `pragmaFields`, `isSpace`, `isQuoted`), `writer.go`
  (`collectDecls`' and `checkPragmas`' checks, `funcExt`'s of a function's directives) and
  `src/cmd/compile/internal/ir/node.go` (the `PragmaFlag` bits), written in Go, adapted (gc's
  configuration fixed: package `p`, linux/amd64, outside the standard library and the runtime)
  and read into Go forms by `bin/repr-go read`; and since 2026-11-17 (doc/go/GOI-SPEC.md, "Since v1: types2's range
  and slice checks") a port, written in Go forms, of `src/cmd/compile/internal/types2`'s `range.go` (`rangeStmt`'s
  checks of the iteration variables, `rangeKeyVal`), `stmt.go` (the range clause's variables), `index.go` (`sliceExpr`,
  `index`) and `operand.go` (the operand's string): Copyright The Go Authors, under Go's BSD 3-Clause License (text below).
- `arbace/python/scope.ae` (pyi's scope analysis, doc/python/PYI-SPEC.md §2; doc/python/PYTHON-SURVEY.md, "As built:
  PY5a"): a port to Arbace of CPython 3.14.10's `Python/symtable.c` (the symbol table's two passes, its mangling and its
  messages) and of `Python/future.c`'s `future_parse` and `future_check_features`, from the release tarball
  https://www.python.org/ftp/python/3.14.10/Python-3.14.10.tar.xz (SHA-256
  `de414a57119abef52784d8993e341d53aed635fc2b05c2607d9f98a2c5b72032`), by hand on 2026-11-03: Copyright (c) 2001 Python
  Software Foundation, All Rights Reserved, under the PSF License Agreement for Python 3.14 (text below, the tarball's
  `LICENSE` whole). `arbace/python.ae` and the rest of `arbace/python/` are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/repr/python/lex.ae` (repr/python's native lexer; doc/python/PYTHON-SURVEY.md, "As built: PY9a"): a port to Arbace
  of CPython 3.14.10's C tokenizer, `Parser/lexer/lexer.c` and `state.c`, the errors of `Parser/tokenizer/helpers.c`,
  `Parser/tokenizer/readline_tokenizer.c`'s underflow, `Python/Python-tokenize.c`'s iterator and `Lib/tokenize.py`'s
  `detect_encoding`, from the same release tarball, by hand on 2026-11-03: Copyright (c) 2001 Python Software Foundation,
  All Rights Reserved, under the PSF License Agreement for Python 3.14 (text below). Likewise since PY9b ("As built: PY9b"):
  `arbace/repr/python/pegen.ae` (a port of `Tools/peg_generator/pegen`'s metagrammar, `grammar.py`, `parser_generator.py`,
  `sccutils.py` and `c_generator.py`'s code shapes), `arbace/repr/python/peg.ae` (of `Parser/pegen.c` and
  `Parser/action_helpers.c`) and `arbace/repr/python/parser.ae` (generated from `Grammar/python.gram`, its actions
  translated), from the same tarball; since queue 202 (202c) `arbace/repr/python/ucn.ae` (of `Modules/unicodedata.c`'s
  `_getcode` and its DAWG lookup, from the same tarball). Since queue 236 (2026-11-08; doc/python/PY315-SURVEY.md, "As
  built: 236") also from CPython 3.15.2's release tarball (https://www.python.org/ftp/python/3.15.2/Python-3.15.2.tar.xz,
  SHA-256 8b5536d280142ecf6cdfe15d12a21ba1c65e300bbf7b28a8d0cdb849575797de, its LICENSE the same bytes as 3.14.10's):
  `arbace/repr/python/parser315.ae` (generated by pegen.ae from its `Grammar/python.gram`), the 3.15 helpers of `peg.ae`
  (`Parser/action_helpers.c`'s `_PyPegen_checked_from_import`, `_warn_relative_import_of_lazy` and
  `_PyPegen_raise_error_for_missing_comma`) and the 3.15 message of `lex.ae` (`Parser/lexer/lexer.c`), under the same
  agreement. The rest of `arbace/repr/python/` and `tools/pydump/` are Arbace's own, under the Eclipse Public License 1.0.
- `go/arbace/py/builtins_csv.ae` and `builtins_mmap.ae` (queue 204, PYS22 (a): pyi's modules `_csv` and `mmap`;
  doc/python/PYTHON-SURVEY.md, "As built: 204"): ports to Go of CPython 3.14.10's `Modules/_csv.c` with
  `Modules/clinic/_csv.c.h`, and of `Modules/mmapmodule.c`'s UNIX paths with `Modules/clinic/mmapmodule.c.h`, from the same release
  tarball, by hand on 2026-11-05: Copyright (c) 2001 Python Software Foundation, All Rights Reserved, under the PSF
  License Agreement for Python 3.14 (text below).
- `go/arbace/py/builtins_faulthandler.ae`, `builtins_lsprof.ae`, `builtins_termios.ae`, `builtins_tracemalloc.ae` and
  `monitoring.ae` (queue 220, PYS22 (a): pyi's modules `faulthandler`, `_lsprof`, `termios`, `_tracemalloc` and
  sys.monitoring's call-family events; doc/python/PYTHON-SURVEY.md, "As built: 220"): ports to Go of CPython 3.14.10's
  `Modules/faulthandler.c` with `Python/traceback.c`'s dump functions, `Modules/_lsprof.c` with
  `Modules/clinic/_lsprof.c.h`, `Modules/termios.c` with `Modules/clinic/termios.c.h`, `Modules/_tracemalloc.c` with
  `Python/tracemalloc.c`'s API, and `Python/instrumentation.c`'s and `Python/legacy_tracing.c`'s call events, from the
  same release tarball, by hand on 2026-11-05: Copyright (c) 2001 Python Software Foundation, All Rights Reserved,
  under the PSF License Agreement for Python 3.14 (text below).
- `go/arbace/py/sentinel.ae`, `frozendict.ae` and `builtins_unicodedata_table315.ae`, and the Python 3.15 parts of
  `builtins_math.ae`, `bytearray.ae`, `exc.ae`, `class.ae`, `attr.ae`, `sys.ae` and `builtins_unicodedata.ae` (queue 237,
  2026-11-08; doc/python/PY315-SURVEY.md, "As built: 237": pyi's runtime under REPR_PYTHON_VERSION=3.15): ports to Go of
  CPython 3.15.2's `Objects/sentinelobject.c`, `Objects/dictobject.c`'s frozendict, `Modules/mathintegermodule.c`,
  `Objects/bytearrayobject.c`'s `take_bytes`, `Objects/exceptions.c`'s `ImportError_repr`, `Objects/typeobject.c`'s
  slots and shared descriptors, `Python/preconfig.c`'s UTF-8 mode and `Modules/unicodedata.c`'s 3.15 parts, and the
  tables of its generated `Objects/unicodetype_db.h`, `Modules/unicodedata_db.h` and `Modules/unicodename_db.h` (UCD
  17.0.0) taken by `tools/pyi/unicodedata_table.py --suffix 315`, from CPython 3.15.2's release tarball (SHA-256
  8b5536d280142ecf6cdfe15d12a21ba1c65e300bbf7b28a8d0cdb849575797de), by hand: Copyright (c) 2001 Python Software
  Foundation, All Rights Reserved, under the PSF License Agreement for Python 3.15 (its LICENSE the same bytes as
  3.14.10's, text below); the Unicode data under the Unicode License v3 as CPython distributes it.
- `go/arbace/py/lazyimport.ae`, and the Python 3.15 parts of `import.ae`, `eval.ae`, `aot.ae`, `func.ae`, `sys.ae`,
  `registry.ae`, `load.ae` and `bytecode.ae` (queue 239, 2026-11-09; doc/python/PY315-SURVEY.md, "As built: 239": lazy
  imports under REPR_PYTHON_VERSION=3.15), and the 3.15 parts of `arbace/python/scope.ae` and `compile.ae`: ports of
  CPython 3.15.2's `Objects/lazyimportobject.c`, `Python/import.c`'s lazy imports, `Python/ceval.c`'s
  `_PyEval_LazyImportName` and the reification of `_PyEval_LoadGlobalStackRef` and `_PyEval_LoadName`,
  `Objects/moduleobject.c`'s getattr, `Python/sysmodule.c`'s lazy-import API, `Python/initconfig.c`'s
  `config_init_lazy_imports`, `Python/bltinmodule.c`'s `__lazy_import__`, `Lib/types.py`'s fallback as `_types`, and
  `Python/symtable.c`'s, `Python/codegen.c`'s, `Python/compile.c`'s and `Python/ast_preprocess.c`'s 3.15 deltas, from the
  same release tarball, by hand: Copyright (c) 2001 Python Software Foundation, All Rights Reserved, under the PSF
  License Agreement for Python 3.15 (text below).
- `go/arbace/py/builtins_ssl.ae`, `builtins_ssl_cert.ae` and `builtins_ssl_conn.ae` (queue 209, PYS22 (a): pyi's module
  `_ssl` over Go's crypto/tls; doc/python/PYTHON-SURVEY.md, "As built: 209"): written after CPython 3.14.10's
  `Modules/_ssl.c`, `Modules/clinic/_ssl.c.h`, `Modules/_ssl/cert.c` and `Modules/_ssl/distrusted_spki.h` (its digests
  copied), from the same release tarball, by hand on 2026-11-05: Copyright (c) 2001 Python Software Foundation, All
  Rights Reserved, under the PSF License Agreement for Python 3.14 (text below). The OIDs, NIDs and names of
  `sslObjects` and the alert reasons are the values OpenSSL 3.5 gives for them (facts of its interface, read through
  CPython's `_ssl.txt2obj`; queue 217 added the alerts of the verification codes, the security levels' bits and the
  cipher-string rules as OpenSSL 3.5 shows them through CPython); the pipe adapter, the cipher-string evaluator and the DER walker are Arbace's own, under the
  Eclipse Public License 1.0, as are the test certificates `test/python/pyi/ssl/` (made on 2026-11-05 with
  `openssl req`/`x509`; `ssl/levels/` by queue 217 the same way).
- `go/arbace/oi.ae` and `go/arbace/oi/` (oi, the interpreter of OCaml's Lambda; doc/ocaml/OCAML-SURVEY.md, "As
  built: O5 (write-ahead)"): the interpreter's core (values, the compiler of Lambda, application, exceptions,
  effects, the toplevel) is Arbace's own, under the Eclipse Public License 1.0; the externals are ports to Go
  of the OCaml Core System's runtime, `runtime/*.c` of OCaml 5.5.0 (the release tarball
  https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz, SHA-256
  `fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`, Copyright INRIA and its contributors):
  `ext_num.ae` and `ext_str.ae` (ints.c, floats.c, str.c), `compare.ae` (compare.c), `hash.ae`
  (hash.c), `ext_array.ae` (array.c), `marshal.ae` (extern.c, intern.c), `ext_io.ae` (io.c), `ext_sys.ae`
  (sys.c, signals.c, gc_ctrl.c, weak.c, finalise.c, backtrace.c, memprof.c, domain.c), `ext_digest.ae`
  (md5.c, blake2.c, bigarray.c's minimal part, the LXM generator of random.ml's runtime), `ext_lex.ae`
  (lexing.c, parsing.c), `ext_bigstring.ae` (bigarray.c), and the parts of `ext.ae`, `ext_domain.ae`,
  `exn.ae`, `run.ae` and `thread.ae` that follow obj.c, sync.c, domain.c, fiber.c and printexc.c, and `ext_unix.ae`
  (the Unix library's core, otherlibs/unix's `unixsupport.c`'s error table and the C stubs of the externals unix.ml
  declares; doc/ocaml/DEFERRED-TRIAGE.md, "As built: OT9's second fix"), `ext_unix_fd.ae` (otherlibs/unix's C stubs of
  descriptors, files and directories), `ext_thread.ae` (otherlibs/systhreads' `st_stubs.c`; "As built: OT9's next
  three fixes"), `ext_unix_sock.ae` (otherlibs/unix's C stubs of sockets, the host database and select) and `ext_signal.ae`
  (runtime/signals.c and otherlibs/unix's signals.c, kill.c, spawn.c, wait.c, itimer.c; "As built: OT9's fixes 6-8"), `ext_str_re.ae`
  (otherlibs/str's `strstubs.c`) and `ext_bigarray.ae` (bigarray.c's compare, serialization, float16 and reshape, otherlibs/unix's
  `mmap_unix.c`; "As built: OT21-OT26 and fixes 9-11"): under the
  GNU Lesser General Public License version 2.1 with the OCaml LGPL linking exception (below). The Unix library's
  OCaml units (`otherlibs/unix/unix.mli`, `unix_unix.ml`, `unixLabels.ml{,i}` of the same tarball, same license) are not
  copied into the tree: `bin/oi stdlib` compiles them from the pinned tarball into `.tmp/oi/unixlib/`, and the threads
  library's (`otherlibs/systhreads/thread.ml{,i}`, `event.ml{,i}`) into `.tmp/oi/threadlib/`, and compiler-libs' units of
  `utils/` the triage's programs need (`config.fixed.ml`, `config.common.ml.in`, `build_path_prefix_map`, `format_doc`, `misc`,
  `identifiable`, `numbers`, `strongly_connected_components`, `stable_matching`, `compression`; since OT21 (b) all of ocamlcommon's
  `utils/` but `domainstate` and `parsing/`, with `boot/menhir/parser.ml` and `menhirLib.ml`; since queue 480 `typing/`, `file_formats/`'s
  `cmi_format` and `cmt_format`, `lambda/`'s `debuginfo` and `lambda`, and `driver/`'s `pparse`, `compenv`, `main_args` and `compmisc`;
  since queue 529 the rest of `lambda/` (`runtimedef.ml` generated by the tarball's own `generate_runtimedef.sh` and
  `runtime/gen_primitives.sh`), `file_formats/`'s `cmo_format`, `cmx_format` and `cmxs_format` and `bytecomp/`'s `instruct`, `meta`,
  `bytesections`, `dll` and `symtable`) into `.tmp/oi/complib/`, the toplevel's `toplevel/` units and `toplevel/byte/topmain.ml`
  with `tools/ocamltex.ml{,i}` (queue 529, doc/ocaml/OCAML-SURVEY.md "As built: queue 529": unmodified) into `.tmp/oi/toplevel/`,
  ocamlc itself and dumpobj (queue 532, doc/ocaml/OCAML-SURVEY.md "As built: queue 532": unmodified): the rest of `comp_SOURCES`
  (`driver/makedepend.ml`, `driver/compile_common.ml`, `bytecomp/opcodes.ml` generated by the tarball's `tools/make_opcodes.mll`),
  `ocamlbytecomp_SOURCES` (`bytecomp/` and `driver/errors.ml`, `compile.ml`, `maindriver.ml`; `bytecomp/byterntm.ml` generated by the
  native ocamllex), `driver/main.ml` and `tools/dumpobj.ml{,i}` with `tools/opnames.ml{,i}` (generated by `make_opcodes`) into
  `.tmp/oi/bytecomp/`,
  and ocamldoc's `ocamldoc/` (queue 480, doc/ocaml/OCAML-SURVEY.md "As built: queue 480": unmodified, its
  lexers and parsers generated by the native ocamllex and ocamlyacc) into `.tmp/oi/ocamldoc/`, and the Str
  library's `otherlibs/str/str.ml{,i}` into `.tmp/oi/strlib/`. The test
  data `test/ocaml/oi/testdata/` and the tables in `go/arbace/oi/lexdata*_test.ae` and `parsedata_test.ae`
  are outputs of the host's OCaml 5.5.0 (ocaml, ocamllex, ocamlyacc) over Arbace's own test programs.
- oi's C API on ci (doc/ocaml/OCAML-SURVEY.md, "As built: the C API on ci"): `go/arbace/oicapi.ae`, `go/arbace/oicapi/`,
  `go/arbace/oi/ext_capi.ae` and `test/ocaml/capi/` (the headers `include/caml/*.h`, the C runtime `runtime.c`, `main.c`,
  `cnodes.ae`) are Arbace's own, under the Eclipse Public License 1.0: clean-room, written from OCaml's manual (chapter 22,
  "Interfacing C with OCaml": the value layout, the macros' and functions' names and meanings) without OCaml's runtime headers
  or sources; `runtime.c`'s BLAKE2b is written from RFC 7693's specification (the initialization vector and the message schedule
  are the RFC's constants). Two exceptions (queue 478, doc/ocaml/OCAML-SURVEY.md "As built: queue 478"): the headers
  `test/ocaml/capi/include/caml/runtime_events.h` and `runtime_events_consumer.h` take their enumerations (the ring's numbering)
  and their functions' declarations from OCaml 5.5.0's `runtime/caml/runtime_events.h` and
  `otherlibs/runtime_events/caml/runtime_events_consumer.h` (the tarball above), under the GNU Lesser General Public License
  version 2.1 with the OCaml LGPL linking exception (below); `runtime.c`'s consumer over oi's ring is Arbace's own.
- `bin/oi depend`'s program `test/ocaml/depend/ocamldep.ml` (queue 478, doc/ocaml/OCAML-SURVEY.md "As built: queue 478") is OCaml
  5.5.0's `driver/makedepend.ml` (the tarball above, Copyright INRIA; its notice kept) with its reading of a source and its
  arguments' parsing put in place of `driver/pparse.ml`'s and `driver/compenv.ml`'s: under the GNU Lesser General Public License
  version 2.1 with the OCaml LGPL linking exception (below). `go/arbace/oi/ext_runtime_events.ae` (OC219) follows the behaviour of
  `runtime/runtime_events.c` and `otherlibs/runtime_events/runtime_events_consumer.c` of the same tarball, under the same license.
- `test/ocaml/objinfo/oiobjinfo.ml` (queue 511, doc/ocaml/OCAML-SURVEY.md "As built: queue 511"), ocamltest's `ocamlc -c` and
  `ocamlobjinfo` on oi, is modified from OCaml 5.5.0's `tools/objinfo.ml` (the tarball above, Copyright INRIA and Mehdi Dogguy; its
  notice kept; the .cmi and .cmt printing kept, since queue 532 the .cmo printing too, the other kinds left out) and
  `driver/compile_common.ml`'s steps up to typing: under
  the GNU Lesser General Public License version 2.1 with the OCaml LGPL linking exception (below).
- `test/ocaml/toplevel/topeval.ml` and `test/ocaml/toplevel/trace.ml` (queue 529, doc/ocaml/OCAML-SURVEY.md "As built: queue 529"),
  the toplevel's evaluator and #trace on oi, are modified from OCaml 5.5.0's `toplevel/byte/topeval.ml` (its loading of a phrase's
  Lambda through oi's Dynlink externals in place of Bytegen, Symtable and Meta, the Lambda written as oi's forms after
  `tools/ocdump/ocdump.ml`'s printer, Arbace's own) and `toplevel/byte/trace.ml` (no code pointer), the tarball above (Copyright INRIA;
  their notices kept): under the GNU Lesser General Public License version 2.1 with the OCaml LGPL linking exception (below).
- `test/ocaml/ocamldoc/oicmi.ml` (queue 480, doc/ocaml/OCAML-SURVEY.md "As built: queue 480"), `ocamlc -c FILE.mli` on oi over
  compiler-libs' units (the standard library's .cmi files ocamldoc types against), is Arbace's own, under the Eclipse Public
  License 1.0, written against the interfaces of OCaml 5.5.0's compiler-libs (driver/compile_common.ml's interface step read for
  its sequence: Pparse, Typemod.type_interface, Env.save_signature).
- repr/ocaml's native reader (doc/ocaml/OCAML-SURVEY.md, "As built: the native reader"): `arbace/repr/ocaml/lex.ae`
  (parsing/lexer.mll, the parts of stdlib/lexing.ml that update the positions, utils/misc.ml's Utf8_lexeme),
  `docstrings.ae` (parsing/docstrings.ml), `ast.ae`'s constructors and docstring attributes (parsing/ast_helper.ml,
  docstrings.ml, location.ml's mkloc), `actions.ae` (parsing/parser.mly's header and its semantic actions), `report.ae` (parsing/location.ml's
  batch-mode report printer, utils/format_doc.ml's pp_two_columns, the break rule of stdlib/format.ml; parse.ml's and
  lexer.mll's error messages are in `actions.ae` and `lex.ae`) and the
  generated `tables.ae` (parser.mly's automaton, with its actions' texts) are ports of OCaml 5.5.0 (the release
  tarball https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz, SHA-256
  `fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`, Copyright INRIA), by hand on 2026-11-04, the
  files carrying the notices: under the GNU Lesser General Public License version 2.1 with the OCaml LGPL linking
  exception (below). `parser.ae`'s engine follows MenhirLib's (boot/menhir/menhirLib.ml of the same tarball: Engine,
  TableInterpreter) and `menhir.ae` follows Menhir 20250912's documented construction, reproduces nine rules of its
  standard library (standard.mly) and decodes MenhirLib's table format (PackedIntArray, RowDisplacement): under
  MenhirLib's terms ("Menhir and MenhirLib", below). The rest of them (`ast.ae`'s dump, which ports Arbace's own
  tools/ocdump/ocdump.ml, `native.ae`, the LALR(1) construction of `menhir.ae`) is Arbace's own, under the Eclipse
  Public License 1.0.
- The read level's own verdicts by the build (doc/ocaml/OCAML-SURVEY.md, "As built: the build's own reads", 2026-11-22):
  `arbace/repr/ocaml/prep.ae`'s `mucppo` ports mucppo (Copyright 2023-2024 Marek Kubica, https://github.com/Leonidas-from-XIV/mucppo,
  as vendored in yojson 3.0.0's `lib/mucppo/mucppo.ml`, the opam tarball pinned in test/ocaml/corpora.edn), released by its notice
  under the CC0 1.0 Universal Public Domain Dedication (https://creativecommons.org/publicdomain/zero/1.0/); its `sync-stdlib-docs`
  ports the perl substitutions of OCaml 5.5.0's `tools/sync_stdlib_docs` (Copyright 2020 INRIA, John Whitington; the release tarball
  above) under the GNU Lesser General Public License version 2.1 with the OCaml LGPL linking exception (below); the rest of the file
  and `test/ocaml/own-build-controls.txt` are Arbace's own, under the Eclipse Public License 1.0.
- ocamllex ported (doc/ocaml/OCAML-SURVEY.md, "As built: ocamllex ported (OC186)"): `arbace/repr/ocaml/ocamllex.ae` ports OCaml
  5.5.0's lex/ (lexer.mll, parser.mly, cset.ml, table.ml, lexgen.ml, compact.ml, common.ml, output.ml, main.ml) and the parts of
  stdlib/set.ml it relies on, from the same release tarball (SHA-256 `fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`,
  Copyright INRIA), by hand on 2026-11-08, the file carrying the notice: under the GNU Lesser General Public License version 2.1 with
  the OCaml LGPL linking exception (below). Its `check` verb, `bin/repr-ocaml ocamllex-check`, `test/ocaml/oc185.ae` and the samples of
  `test/ocaml/ocamllex/` are Arbace's own (Eclipse Public License 1.0); `test/ocaml/ocamllex-expected.edn` records the host's ocamllex
  5.5.0's outputs' digests.
- ocamlyacc ported (doc/ocaml/OCAML-SURVEY.md, "As built: OC194 (a)"): `arbace/repr/ocaml/ocamlyacc.ae` ports OCaml 5.5.0's yacc/
  (main.c, reader.c, symtab.c, error.c, lr0.c, closure.c, warshall.c, lalr.c, mkpar.c, output.c, skeleton.c, verbose.c) from the same
  release tarball (SHA-256 `fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`), by hand on 2026-11-09, the file carrying
  the notice. yacc/ is Berkeley yacc as OCaml adapted it: each file says "Based on public-domain code from Berkeley Yacc" (Berkeley
  yacc itself is in the public domain), and each carries INRIA's notice (Copyright 1996 INRIA) placing OCaml's adaptation under the GNU
  Lesser General Public License version 2.1 with the OCaml LGPL linking exception (below); the port is under those terms. Its `check`
  verb, `bin/repr-ocaml ocamlyacc-check` and the samples of `test/ocaml/ocamlyacc/` are Arbace's own (Eclipse Public License 1.0);
  `test/ocaml/ocamlyacc-expected.edn` records the host's ocamlyacc 5.5.0's outputs' and messages' digests.
- oi's native front end (doc/ocaml/TYPER.md): `arbace/repr/ocaml/typer/types.ae` (typing/types.ml, btype.ml, ctype.ml's
  levels, unification, generalization and instances, predef.ml, printtyp.ml with out_type.ml and oprint.ml),
  `env.ae` (typing/env.ml, typetexp.ml, typedecl.ml, datarepr.ml), `parmatch.ae` (typing/patterns.ml, parmatch.ml),
  `typecore.ae` (typing/typecore.ml, typemod.ml's structure items), `lambda.ae` (lambda/lambda.ml's helpers,
  switch.ml, printlambda.ml's names), `matching.ae` (lambda/matching.ml), `translcore.ae` (lambda/translcore.ml,
  translprim.ml, translmod.ml's bytecode structure), `simplif.ae` (lambda/simplif.ml with lambda.ml's traversals,
  T7), `tmc.ae` (lambda/tmc.ml), `typemod.ae` (typing/typemod.ml, includemod.ml, mtype.ml's nondep_supertype),
  `translmod.ae` (lambda/translmod.ml), `format.ae` (stdlib/format.ml's engine) and `printtyp.ae` (typing/oprint.ml's
  printers), with `env.ae` extended by env.ml's module components, subst.ml and mtype.ml (T8), and their polymorphic
  variants' parts of the same files (types.ml, btype.ml, ctype.ml, typetexp.ml, typecore.ml, parmatch.ml, out_type.ml,
  matching.ml, switch.ml, translcore.ml, translprim.ml; T9), `typeclass.ae` (typing/typeclass.ml with ctype.ml's class
  signatures and objects, T9) and `translclass.ae` (lambda/translobj.ml, translclass.ml, T9) with the object parts of
  the files above (types.ml, ctype.ml, typetexp.ml, typecore.ml, out_type.ml, oprint.ml, translcore.ml, translprim.ml,
  simplif.ml), and `valuerec.ae` (typing/value_rec_check.ml's classify_expression, lambda/value_rec_compiler.ml, T10)
  with parmatch.ml's exhaust and typecore.ml's counter-example check in `parmatch.ae` and `typecore.ae`, and (T10's
  rest, 2026-11-06) lambda/debuginfo.ml's Scoped_location in `translcore.ae` with the effect, let-operator, open,
  recursive-module, scope and modular-explicit (Tfunctor) parts of the files above, are ports of
  the same OCaml 5.5.0 tarball (SHA-256
  `fcc6ae665d1ec51d52510eaac7834a86a9806bf5a258bb7cca78733fccf015ba`, Copyright INRIA), by hand on 2026-11-05 and 2026-11-06, the
  files carrying the notices: under the GNU Lesser General Public License version 2.1 with the OCaml LGPL linking
  exception (below). `arbace/repr/ocaml/typer.ae` (the driver and the judge) and `bin/lib/oi/typer.bash` are Arbace's own,
  under the Eclipse Public License 1.0; test/ocaml/typer/'s samples are Arbace's own, their `.out` files outputs of the
  host's OCaml 5.5.0 toplevel, the dumps of `test/ocaml/typer/oracle/` outputs of tools/ocdump on the host's
  compiler-libs.
- `jdk/ocaml/` (OCaml's runtime as Java forms, the package `ocaml.rt`; doc/ocaml/OCAML-SURVEY.md, "As built: O6
  (write-ahead)") and `arbace/o2j.ae` (o2j, Lambda to Java forms): the values, the application, the program's
  start (`Block`, `Closure`, `Pap`, `Native`, `Code`, `Exn`, `Exit`, `Fatal`, `I64`, `NInt`, `Custom`, `Rt`, `Main`, `Gil`,
  `Dom`, `TState`) and o2j are Arbace's own, under the Eclipse Public License 1.0; the externals are ports to Java
  forms of oi's Go ports (above) of the OCaml Core System's runtime, `runtime/*.c` of the same OCaml 5.5.0 tarball:
  `ExtNum`, `ExtStr` (ints.c, floats.c, str.c), `Compare` (compare.c), `Hash` (hash.c), `ExtArray` (array.c), `Marshal`
  (extern.c, intern.c), `ExtIo`, `Channel`, `Fd` (io.c, sys.c's open and close), `ExtSys` (sys.c, signals.c), `ExtGc`
  (gc_ctrl.c, weak.c, finalise.c, backtrace.c, memprof.c), `ExtDigest`, `Blake2` (md5.c, blake2.c), `ExtLxm`
  (the LXM generator), `ExtBigarray`, `BigArray` (bigarray.c's part oi has), `ExtLex` (lexing.c, parsing.c), and the
  parts of `ExtCore`, `Effects` and `Fiber` that follow obj.c, alloc.c, lazy.c, sync.c, domain.c and fiber.c, and `ExtUnix`
  (oi's `ext_unix.ae`, above), `ExtSignal` and `ExtProc` (oi's `ext_signal.ae`'s part Java allows; "As built: OT9's fixes 6-8"),
  `ExtStrRe` and `BaOps` (oi's `ext_str_re.ae` and `ext_bigarray.ae`'s part but the file mapping):
  under the GNU Lesser General Public License version 2.1 with the OCaml LGPL linking exception (below).
- The opam set's library either (test/ocaml/corpora.edn, the pin `:opam-either` of queue 534, the user's decision of
  2026-11-22 on 528-15): the opam package either 1.0.0 (Craig Ferguson), which containers 3.18's library needs, at the
  version opam-repository's master 5c9b817c546fcd5cb2be1cb52b08ea5c36320181 pins by checksum, fetched into `.tmp/` and read
  in place, **not distributed** (it keeps its own license and notices), under the MIT License, as its opam file states.
- The opam set of the OCaml crop (test/ocaml/corpora.edn, the pins `:opam-NAME`; doc/ocaml/OCAML-SURVEY.md, "As built: O7"):
  sources of ten opam packages, fetched at pinned versions into `.tmp/` and read in place by the checks, **not distributed**
  (nothing of them is in the tree; each keeps its own license and notices): sexplib0 v0.17.0 (MIT), fmt 0.11.0, astring
  0.8.5, uutf 1.0.4, cmdliner 2.1.1 and base64 3.5.2 (ISC), re 1.14.0 (LGPL-2.1-or-later with the OCaml LGPL linking
  exception), pprint 20230830 (LGPL-2.0-only with the OCaml LGPL linking exception), yojson 3.0.0 (BSD-3-Clause),
  containers 3.18 (BSD-2-Clause), as their opam files state. `test/ocaml/opam/load.ml` is Arbace's own.
- The opam set's test libraries (test/ocaml/corpora.edn, the pins `:opam-NAME` of queue 531, the user's decision of 2026-11-22
  on 528-15): the packages five of the set's tests need (sexplib0's sexplib0_test, re's expect, base64's test, fmt's test_fmt,
  cmdliner's test_arg), at the versions opam-repository's master 5c9b817c546fcd5cb2be1cb52b08ea5c36320181 pins by checksum,
  fetched into `.tmp/` and read in place, **not distributed** (each keeps its own license and notices), as their opam files
  state: under the MIT License, Jane Street's base v0.17.3, base_quickcheck v0.17.1, bin_prot v0.17.0-1, capitalization,
  expect_test_helpers_core, fieldslib, jane-street-headers, jst-config, parsexp, sexp_pretty, sexplib, splittable_random,
  stdio, time_now and variantslib (v0.17.0), ocaml-compiler-libs v0.17.0, ocaml_intrinsics_kernel v0.17.3, typerep v0.17.1,
  ppxlib_jane v0.17.4 and the rewriters ppx_assert, ppx_base, ppx_cold, ppx_compare, ppx_custom_printf,
  ppx_disable_unused_warnings, ppx_enumerate, ppx_fields_conv, ppx_fixed_literal, ppx_hash, ppx_here,
  ppx_ignore_instrumentation, ppx_jane, ppx_log, ppx_module_timer, ppx_optional, ppx_pipebang, ppx_sexp_message,
  ppx_sexp_value, ppx_stable_witness, ppx_string and ppx_string_conv (v0.17.0), ppx_bench, ppx_bin_prot, ppx_inline_test,
  ppx_let, ppx_optcomp, ppx_sexp_conv, ppx_stable, ppx_tydi, ppx_typerep_conv and ppx_variants_conv (v0.17.1),
  ppx_globalize v0.17.2, ppx_expect v0.17.3, and ppxlib 0.39.0 (Jane Street Group, LLC and the ppxlib authors); under the
  ISC License, alcotest 1.10.1, bos 0.3.0, fpath 0.7.3, logs 0.10.0 and rresult 0.7.0; b0 0.0.6 under the ISC License and
  the BSD 2-Clause License (its opam file lists both); ppx_derivers 1.2.1 under the BSD 3-Clause License.
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
    java.base's files jrt translates, 815 of `src/java.base/share/classes` (since JC6b; 1,150 since
    F1, the JCA, doc/classes/JAVA-DEFERRED-TRIAGE.md, once `bin/jrt-convert update` has run), 6 of
    `src/java.base/unix/classes`, 2 of `src/java.sql/share/classes`), from the 31 sources the JDK
    build generates (`CharacterData*`, java.nio's buffers, `ScopedMemoryAccess`, the CLDR locale
    data, `LocaleEquivalentMaps`; 41 since F1, the JCA's fields of `sun.security.util.math.intpoly`
    made by the build's `FieldGen`: generated by `bin/jrt-convert` with jdk26u's build tools from
    jdk26u's data, as the build generates them) and from `jdk/internal/jrt/LocaleDataIndex`
    (generated by `test/jrt/jrt_sources.ae`): Copyright (c) Oracle and/or its affiliates, under
    the GNU General Public License version 2 with the Classpath Exception (text below), with the
    third-party data they hold under the licenses listed below ("The JDK's resource data": the
    Unicode Character Database's, ICU4J's and CLDR's under the Unicode License V3). The form files
    converted from jrt's own Java (the first line names `overlay/jdk/java.base/...`) are under
    `overlay/jdk/`'s licenses, given in the next item.
  - `jdk/data/`: the JDK's resource data, as listed below ("The JDK's resource data").
  - `jdk/provides.txt` (A4 of the Ghidra case study's G1 plan, 2026-11-10): the world's module provides, generated by
    `bin/jrt provides` from jdk26u's `module-info.java` files: the names of services and providers (Copyright (c)
    Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception, as the
    files they are read from).
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
    build generates them, and since JFS52 (b) `FlagsEnum` too, from `Flags.java`) and 22 files of
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
    Since JCS52 (a) (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS52 (a)") `jdk/javac/forms/` also holds jdk.javadoc's
    doclint, converted from jdk26u's `src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclint/` (Checker, DocLint, Env,
    Messages), `.../html/HtmlAttr.java`, `.../html/HtmlTag.java` and `.../tool/AccessLevel.java`, and its resource bundles as the
    build generates them (`support/gensrc/jdk.javadoc/jdk/javadoc/internal/doclint/resources/`, generated by `bin/javac-vendor` from
    the `doclint*.properties` with the build's CompileProperties): Copyright (c) Oracle and/or its affiliates, under the GNU General
    Public License version 2 with the Classpath Exception (text below).
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
    The own platform (doc/classes/JAVA-FRONTEND-SURVEY.md, "Own platform", 2026-11-17): `jdk/javac/platform-gensrc.tar.xz`
    (the generated sources of jdk26u's 16 modules that have some, made by the JDK build's own tools on javac's tool from
    jdk26u's templates and data: each file keeps its template's notice, the GNU General Public License version 2 with the
    Classpath Exception, Copyright (c) Oracle and/or its affiliates, and for jdk.localedata's CLDR-derived files the Unicode
    License, jdk26u's `src/java.base/share/legal/cldr.md`, as jdk/data's CLDR data below), `jdk/javac/platform-records/`
    (`java.base/jdk/internal/lang/CaseFolding.java`, the JDK build's generated file, GNU General Public License version 2
    with the Classpath Exception, Copyright (c) Oracle and/or its affiliates; `java.base/lib/security/cacerts`, the JDK
    build image's, as `jdk/data/lib/security/cacerts`), and `jdk/javac/platform-own-known.edn`,
    `platform-gensrc-known.edn`, `platform-descriptors-known.edn` and `platform-build.edn`, `platform-gensrc.edn`
    (Arbace's own, under the Eclipse Public License 1.0).
- `overlay/jdk/java.base/java/util/zip/Deflater.java` (JN7, for javac's tool build, then in
  `overlay/jdk/javac/java.base/`; since JC6b, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built:
  JC6b", in the shipped program's world): jrt's `Deflater`, a port to Java of zlib 1.3.2's
  compressor as jdk26u bundles it (`src/java.base/share/native/libzip/zlib/`: `deflate.c`,
  `deflate.h`, `trees.c`; zlib by Jean-loup Gailly and Mark Adler, https://zlib.net), and
  `overlay/jdk/java.base/java/util/zip/Inflater.java` (JC6b): jrt's `Inflater`, a port to Java of
  zlib 1.3.2's decompressor (`inflate.c`, `inflate.h`, `inftrees.c`, `inftrees.h`): Copyright (C)
  1995-2026 Jean-loup Gailly and Mark Adler, and the Arbace authors for the ports, under the zlib
  License (section "zlib: zlib License" below); their public APIs follow jdk26u's documented
  `java.util.zip.Deflater` and `Inflater`.
- `go/arbace/py/zlib_zutil.ae`, `zlib_deflate.ae`, `zlib_trees.ae`, `zlib_inflate.ae` (queue 200, PY6d: pyi's module
  `zlib`): zlib 1.3.2's deflate and inflate ported to Go from the two Java ports above and, for the paths they leave
  out (deflate_rle, Z_FIXED, the gzip wrapper, inflate's gzip header), zlib's C (deflate.c, trees.c, inflate.c,
  inftrees.c): Copyright (C) 1995-2026 Jean-loup Gailly and Mark Adler, and the Arbace authors for the port, under the
  zlib License (section "zlib: zlib License" below).
- `go/arbace/expat.ae`, `go/arbace/expat/` (queue 202e: expat for pyi's module `pyexpat`): Expat 2.9.0 (`Modules/expat/`
  of the CPython 3.14.10 release tarball, https://www.python.org/ftp/python/3.14.10/Python-3.14.10.tar.xz, SHA-256
  `de414a57...b72032`; xmlparse.c's first line names Expat's commit `e3ca8454...` "2.9.0+"), its `xmlparse.c`,
  `xmltok.c`, `xmltok_impl.c`, `xmltok_ns.c`, `xmlrole.c`, `xcs.c`, `siphash.h` and the headers they need ported to Go
  statement for statement; and `go/arbace/pyexpat.ae` and `go/arbace/pyexpat/` (package `arbace/pyexpat`), a port of the same tarball's `Modules/pyexpat.c`
  (under the PSF License Agreement, section "CPython: the PSF License Agreement for Python 3.14" below). Expat:
  Copyright (c) 1998-2000 Thai Open Source Software Center Ltd and Clark Cooper, Copyright (c) 2001-2025 Expat
  maintainers, each file with its own list of copyright holders, and the Arbace authors for the port, under the MIT
  License (section "Expat: MIT License" below); but `go/arbace/expat/siphash.ae`, siphash.h's port (SipHash-2-4 by
  William Ahern after Jean-Philippe Aumasson's and Daniel J. Bernstein's reference implementation), under the CC0 1.0
  Universal Public Domain Dedication its notice states (https://creativecommons.org/publicdomain/zero/1.0/).
- `go/arbace/clib/bzip2.ae`, `go/arbace/clib/bzip2/` (queue 208: the C library of pyi's module `_bz2`, generated by
  `bin/pyi-clib build bzip2`): bzip2 1.0.8's library (`blocksort.c`, `huffman.c`, `crctable.c`, `randtable.c`,
  `compress.c`, `decompress.c`, `bzlib.c` and their headers, of
  https://github.com/python/cpython-source-deps/archive/refs/tags/bzip2-1.0.8.tar.gz, SHA-256 `ab8d1b0c...7427e2`, the
  tarball CPython 3.14.10's `Misc/externals.spdx.json` pins), built by ci's builder into one node program with the part
  of musl 1.2.6 it reaches (https://musl.libc.org/releases/musl-1.2.6.tar.gz, test/c/corpora.edn's pin) and
  `tools/pyi/clib/bzip2/pybz2.c` (Arbace's own): bzip2 Copyright (C) 1996-2019 Julian R Seward, under bzip2's license
  (section "bzip2: its license" below); musl Copyright (C) 2005-2020 Rich Felker, et al., under the MIT License (section
  "musl: MIT License" below). `go/arbace/pybz2.ae` and `go/arbace/pybz2/` (package `arbace/pybz2`), a port of the
  CPython 3.14.10 tarball's `Modules/_bz2module.c` (under the PSF License Agreement, section "CPython: the PSF License
  Agreement for Python 3.14" below); `go/arbace/clib.ae`, `go/arbace/clib/clib.ae` and `clib_test.ae`, the host they
  run on, Arbace's own.
- `go/arbace/clib/xz.ae`, `go/arbace/clib/xz/` (queue 208: the C library of pyi's module `_lzma`, generated by
  `bin/pyi-clib build xz`): XZ Utils 5.2.5's liblzma (`src/liblzma`'s `liblzma.la` files and `src/common/tuklib_physmem.c`, of
  https://github.com/python/cpython-source-deps/archive/refs/tags/xz-5.2.5.tar.gz, SHA-256 `a15c168e...ad282`, the tarball
  CPython 3.14.10's `Misc/externals.spdx.json` pins), built by ci's builder with the part of musl 1.2.6 it reaches and
  `tools/pyi/clib/xz/pylzma.c` (Arbace's own; `tools/pyi/clib/xz/config.h` is the tarball's configure's output): liblzma
  is in the public domain (section "XZ Utils: its licensing" below), musl under the MIT License (section "musl: MIT
  License" below). `go/arbace/pylzma.ae` and `go/arbace/pylzma/` (package `arbace/pylzma`), a port of the CPython
  3.14.10 tarball's `Modules/_lzmamodule.c` (under the PSF License Agreement, section "CPython: the PSF License
  Agreement for Python 3.14" below).
- `go/arbace/clib/zstd.ae`, `go/arbace/clib/zstd/` (queue 208: the C library of pyi's module `_zstd`, generated by
  `bin/pyi-clib build zstd`): zstd 1.5.7's library (`lib/common`, `lib/compress`, `lib/decompress`, `lib/dictBuilder`, of
  https://github.com/python/cpython-source-deps/archive/refs/tags/zstd-1.5.7.tar.gz, SHA-256 `f24b5247...499c8e`, the
  tarball CPython 3.14.10's `Misc/externals.spdx.json` pins), built by ci's builder with the part of musl 1.2.6 it reaches
  and `tools/pyi/clib/zstd/pyzstd.c` (Arbace's own): Copyright (c) Meta Platforms, Inc. and affiliates, under the BSD
  License of its `LICENSE` (zstd is dual licensed, BSD or GPLv2; taken under the BSD License, section "zstd: BSD
  License" below), musl under the MIT License (section "musl: MIT License" below). `go/arbace/pyzstd.ae` and
  `go/arbace/pyzstd/` (package `arbace/pyzstd`, queue 215), a port of the CPython 3.14.10 tarball's `Modules/_zstd/`
  (under the PSF License Agreement, section "CPython: the PSF License Agreement for Python 3.14" below);
  `tools/pyi/clib/refmod.bash`, Arbace's own, builds that tarball's `Modules/_zstd` unchanged for the reference (not kept).
- `go/arbace/clib/sqlite.ae`, `go/arbace/clib/sqlite/` (queue 215: the C library of pyi's module `_sqlite3`, generated by
  `bin/pyi-clib build sqlite`; 215's DECISION 1 (a), the user 2026-11-05): SQLite 3.50.4's amalgamation (`sqlite3.c`,
  `sqlite3.h` of https://github.com/python/cpython-source-deps/archive/refs/tags/sqlite-3.50.4.0.tar.gz, SHA-256
  `fb5ab81f...31e8d0`, the tarball CPython 3.14.10's `Misc/externals.spdx.json` pins), built by ci's builder with the part of
  musl 1.2.6 it reaches and `tools/pyi/clib/sqlite/pysqlite.c` (Arbace's own): SQLite is in the public domain (section
  "SQLite: public domain" below), musl under the MIT License (section "musl: MIT License" below). `go/arbace/pysqlite3.ae`
  and `go/arbace/pysqlite3/` (package `arbace/pysqlite3`), a port of the CPython 3.14.10 tarball's `Modules/_sqlite/`
  (Copyright (C) 2004-2010 Gerhard Häring, under the zlib-style notice its files carry, and the Python Software Foundation,
  under the PSF License Agreement, section "CPython: the PSF License Agreement for Python 3.14" below).
- `go/arbace/clib/ctypestest.ae`, `go/arbace/clib/ctypestest/` (queue 244: the C world of pyi's ctypes, generated by
  `bin/pyi-clib build ctypestest`): CPython 3.14.10's `Modules/_ctypes/_ctypes_test.c` and
  `_ctypes_test_generated.c.h` and, since queue 262, its `Modules/_testlimitedcapi.c` and `Modules/_testlimitedcapi/`
  (all of it since queue 268, `heaptype_relative.c` and `vectorcall_limited.c` too), and since queue 269 its
  `Modules/_testcapimodule.c` (with `Modules/_testcapi_feature_macros.inc` inlined) and parts of `Modules/_testcapi/` in
  the full API's mode, and since queue 456 its `Modules/_testclinic.c` (with `Modules/clinic/_testclinic.c.h` and
  `_testclinic_depr.c.h`), `Modules/_testclinic_limited.c` (with its clinic header) and `Modules/_testbuffer.c`, and
  since py-capi-1122 its `Modules/_testmultiphase.c` (with `Modules/clinic/_testmultiphase.c.h`) and
  `Modules/_testsinglephase.c` (their export tables `tools/pyi/lib-dynload/*.arbace-314.so` list the files' PyInit_
  functions), compiled against its `Include/` in the limited API's mode
  (from the release tarball test/python/corpora.edn pins, `:cpython-3.14.10`, SHA-256 `de414a57...b72032`; Copyright
  (c) 2001 Python Software Foundation, All Rights Reserved, under the PSF License Agreement, section "CPython: the PSF
  License Agreement for Python 3.14" below), built by ci's builder with the part of musl 1.2.6 it reaches (under the
  MIT License, section "musl: MIT License" below) and `tools/pyi/clib/ctypestest/pyctypes_test.c` and `pycapi.c`
  (Arbace's own; `testcapi_parts.c`, the stubs of the parts not built, removed by queue 274b once all 38 were; `pycapi.c`'s `_Py_ctype_table`, `_Py_ctype_tolower` and `_Py_ctype_toupper` since
  queue 270 the tables of CPython 3.14.10's `Python/pyctype.c`, and its `_Py_ascii_whitespace` since queue 481 the table of `Objects/unicodeobject.c`, under the PSF License Agreement). `tools/pyi/capi/include/pyconfig.h`: Arbace's own configuration for CPython's `Include/` (the clean-room
  `Python.h` of queue 244 was removed by queue 262), under the Eclipse Public License 1.0. `go/arbace/pyctypes.ae` and
  `go/arbace/pyctypes/` (package `arbace/pyctypes`): libffi's part over ci, Arbace's own (written from libffi's
  documented interface, `ffi_prep_cif`, `ffi_call`, `ffi_prep_closure_loc`; no libffi code), under the Eclipse Public
  License 1.0; the rest a port of CPython 3.14.10's `Modules/_ctypes/` (queue 244) and, in `capi_limited.ae`,
  `capi_objects.ae` (queue 262), `capi_types.ae` and `capi_more.ae` (queue 268), `capi_mirror.ae` (queue 269: the
  layouts of `Include/cpython/`, `Objects/typeslots.inc`'s offsets, `PyType_Ready`'s part), `capi_unicode.ae` and `capi_getargs.ae` (queue 270: `Objects/unicodeobject.c`'s compact layout's
  functions and the writer, `Python/getargs.c`'s units, `Objects/abstract.c`'s buffer protocol), `capi_heaptype.ae`,
  `capi_parts.ae` and `capi_port.ae` (queue 270b: `Objects/typeobject.c`'s metaclasses, tokens and module lookups, the
  managed dict, `Objects/funcobject.c`, `Python/codecs.c`, `Python/pytime.c`, `Python/errors.c`, `Objects/abstract.c`,
  `Objects/longobject.c`, `Objects/dictobject.c`, `Python/ceval.c`'s `PyEval_EvalCodeEx`), `capi_watchers.ae` (queue 274:
  the watchers of `Objects/dictobject.c`, `Objects/typeobject.c`, `Objects/funcobject.c`, `Objects/codeobject.c` and
  `Python/context.c`), `capi_monitoring.ae` (queue 274b: sys.monitoring's C API of `Python/instrumentation.c`), of the
  C API's functions in its `Objects/` and `Python/`, under the PSF License
  Agreement (section above).
- `tools/pyi/clib/pytokenizer/` and `go/arbace/clib/pytokenizer.ae`, `go/arbace/clib/pytokenizer/` (queue 489, lex-check's own
  reference): the node program of CPython 3.14.10's C tokenizer, its `Parser/lexer/` (`lexer.c`, `state.c`, `buffer.c`),
  `Parser/tokenizer/helpers.c` and `readline_tokenizer.c`, `Parser/token.c`, `Python/Python-tokenize.c` (with
  `Python/clinic/Python-tokenize.c.h`), `Objects/unicodectype.c` (with `unicodetype_db.h`) and `Python/pyctype.c`, unmodified,
  compiled against its `Include/` (from the release tarball test/python/corpora.edn pins, `:cpython-3.14.10`, SHA-256
  `de414a57...b72032`; Copyright (c) 2001 Python Software Foundation, All Rights Reserved, under the PSF License Agreement,
  section "CPython: the PSF License Agreement for Python 3.14" below), built by ci's builder with the part of musl 1.2.6 it
  reaches (under the MIT License, section "musl: MIT License" below) and `tools/pyi/clib/pytokenizer/tokharness.c`, Arbace's
  own under the Eclipse Public License 1.0 but for its copies of `Objects/unicodeobject.c`'s `_PyUnicode_ScanIdentifier` and
  `_Py_ascii_whitespace` and `Parser/pegen.c`'s three `_PyPegen_byte_offset_to_character_offset*` functions (under the PSF
  License Agreement, marked in the file).
- `tools/pyi/clib/pyparser/` and `go/arbace/clib/pyparser.ae`, `go/arbace/clib/pyparser/` (queue 494, parse-check's own
  reference): the node program of CPython 3.14.10's parser, its `Parser/parser.c`, `pegen.c`, `pegen_errors.c`,
  `action_helpers.c`, `string_parser.c` and `peg_api.c`, `Python/Python-ast.c`, `asdl.c`, `pyarena.c`, `future.c`,
  `ast_preprocess.c` and `mystrtoul.c`, and the tokenizer's files of the entry above with `Parser/tokenizer/string_tokenizer.c`
  and `utf8_tokenizer.c`, unmodified, compiled against its `Include/` (the same tarball and license as the entry above),
  built by ci's builder with the part of musl 1.2.6 it reaches (under the MIT License) and
  `tools/pyi/clib/pyparser/parseharness.c`, Arbace's own under the Eclipse Public License 1.0 but for its copies of
  `Modules/unicodedata.c`'s normalization and name lookup (NFKC, the DAWG, the derived names; Copyright (c) Corporation for
  National Research Initiatives, under the PSF License Agreement; the harness reads their tables from the tarball's
  `Modules/unicodedata_db.h` and `unicodename_db.h` at run time), of
  `Objects/unicodeobject.c`'s `_PyUnicode_DecodeUnicodeEscapeInternal2`, `_PyUnicode_ScanIdentifier` and
  `_Py_ascii_whitespace`, `Objects/bytesobject.c`'s `_PyBytes_DecodeEscape2`, `Objects/longobject.c`'s `_PyLong_DigitValue`,
  `Python/ast.c`'s `_PyAST_GetDocString`, `Python/pystrcmp.c`'s `PyOS_mystrnicmp` and `Python/codegen.c`'s `_Py_CArray_*` (under the PSF License Agreement, marked
  in the file).
- `overlay/jdk/`: jrt's own Java sources and Go-build variants of JDK classes (doc/go/JRT-NOTES.md,
  "Phase 2C"; `overlay/jdk/javac/`, those of javac's tool build, doc/classes/JAVA-FRONTEND-SURVEY.md,
  JN2; `overlay/jdk/javac/buildtools/` and `buildtools-world/`, the tool-world variants of the JDK build's tools and of
  the API they call that `bin/javac-vendor gensrc` runs on javac's tool, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built:
  JCS359 stages 2-3 (day)"), written for Arbace in place of jdk26u's files of the same names (from their
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
- Generic reflection (F25, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F25 (write-ahead)"):
  `overlay/jdk/java.base/jdk/internal/jrt/ReflectSupport.java` holds the bodies of the generic members of
  https://github.com/openjdk/jdk26u at `baf63fb` (`src/java.base/share/classes/java/lang/Class.java`,
  `java/lang/reflect/Executable.java`, `Method.java`, `Constructor.java`, `Field.java`; the file's header names them),
  their receiver a parameter: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
  with the Classpath Exception (text below); the rest of that file, the variant `overlay/jdk/variants/TypeVariableImpl.ae`
  and `go/arbace/jrt/classdata.ae` (whose Class members follow the same `Class.java`'s rules) are Arbace's own, under
  the Eclipse Public License 1.0.
- Serialization (JCS167, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 (write-ahead)"; doc/go/JRT-NOTES.md,
  "Serialization"): `overlay/jdk/java.base/java/io/ObjectStreamClass.java` and `ObjectInputStream.java` are jdk26u's
  files of the same names (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.base/share/classes/java/io/`) edited
  by `test/jrt/serialization/variants.py` (each file's header says so): Copyright (c) Oracle and/or its affiliates, under
  the GNU General Public License version 2 with the Classpath Exception (text below), the edits Arbace's own;
  `overlay/jdk/java.base/java/io/ClassCache.java` and `jdk/internal/jrt/SerialAlloc.java` are Arbace's own. Since
  serialization's slice 4, JCS169 and JCS170 (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: serialization slice 4",
  "As built: JCS169 (write-ahead)", "As built: JCS170 (write-ahead)"): `overlay/jdk/variants/SerializedLambda.ae` keeps
  the code of jdk26u's `src/java.base/share/classes/java/lang/invoke/SerializedLambda.java` where it keeps it (its
  `toString`'s format; the kinds' names of `MethodHandleInfo.java`), and the `readObject`s the variants `DESKey.ae`,
  `DESedeKey.ae`, `PBEKey.ae` and `PasswordCallback.ae` restore are jdk26u's as repr/java converts them, less the
  Cleaner's registration: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
  with the Classpath Exception (text below), the rest Arbace's own; `overlay/jdk/java.base/jdk/internal/jrt/SerialLambdas.java`
  is Arbace's own; `arbace/lang/Compiler.ae`'s `Compiler$ProxySupport` and `Compiler$ProxyFn` are Arbace's own code
  following the rules of jdk26u's `java/lang/reflect/Proxy.java` and `ProxyGenerator.java` (their messages quoted).
- Security (doc/go/JRT-NOTES.md, "Security"): `overlay/jdk/java.base/javax/crypto/JceSecurity.java`
  holds the `getInstance` methods of https://github.com/openjdk/jdk26u at `baf63fb`
  (`src/java.base/share/classes/javax/crypto/JceSecurity.java.template`),
  `overlay/jdk/java.base/sun/security/provider/SeedGenerator.java` its `getSystemEntropy` and
  `longToByteArray` (`sun/security/provider/SeedGenerator.java`), and the variants
  `overlay/jdk/variants/Provider.ae`, `Security.ae`, `ProviderConfig.ae`, `SunEntries.ae`,
  `DigestBase.ae` and `NativePRNG.ae` keep the code of the jdk26u methods they
  replace (`java/security/Provider.java`, `Security.java`, `sun/security/jca/ProviderConfig.java`,
  `sun/security/provider/SunEntries.java`,
  `sun/security/provider/DigestBase.java`,
  `src/java.base/unix/classes/sun/security/provider/NativePRNG.java`) where they keep it, and since
  the JCA (doc/classes/JAVA-DEFERRED-TRIAGE.md, F1; `SunJCE.ae` removed, jdk26u's `SunJCE` whole) the
  variants `PBES2Core.ae`, `HmacPKCS12PBECore.ae`, `PBMAC1Core.ae`, `HKDFKeyDerivation.ae`,
  `ConstructKeys.ae`, `DESKey.ae`, `DESedeKey.ae`, `PBEKey.ae`, `PBKDF2KeyImpl.ae`
  (`com/sun/crypto/provider/`), `KeyUtil.ae`, `ECUtil.ae` (`sun/security/util/`) and `PKCS8Key.ae`
  (`sun/security/pkcs/PKCS8Key.java`) keep the members they replace as repr/java converts them, and
  `overlay/jdk/java.base/javax/crypto/JceSecurityManager.java` the names and signatures of jdk26u's
  `javax/crypto/JceSecurityManager.java`: Copyright (c) Oracle and/or its affiliates, under the GNU
  General Public License version 2 with the Classpath Exception (text below); the rest of those
  files, the digests' variants (`MD5.ae`, `SHA.ae`, `SHA2.ae`, `SHA5.ae`, `SHA3.ae`), the JCA's
  variants that only cut or set (`SecretKeySpec.ae`, `EncodedKeySpec.ae`, `SealedObject.ae` until serialization's slice 4,
  `Signature.ae`; `GaloisCounterMode.ae` until F23, below), `jdk/internal/jrt/HostRandom.java`,
  `jdk/internal/jrt/CryptoAccess.java`, `sun/security/provider/JrtServices.java`,
  `com/sun/crypto/provider/JceServices.java` and `sun/security/rsa/RsaServices.java` (their service
  class names are jdk26u's providers') are Arbace's own.
- `go/arbace/jrt/filesystem.ae` (doc/go/JRT-NOTES.md, "Files") holds jrt's Go forms following jdk26u's
  (https://github.com/openjdk/jdk26u at `baf63fb`) `src/java.base/unix/native/libjava/canonicalize_md.c`
  (`JDK_Canonicalize`) and `path_util.c` (`collapse`) and the results of `UnixFileSystem_md.c`: Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below);
  recorded by the rule of B7. The rest of it is Arbace's own. (jrt's own `HostPath.java`, `Path.java`, `Files.java`
  and `GlobPatterns.java`, which held code transcribed from jdk26u's UnixPath, Path and Files, left with F9 below.)
- Round 54 (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: n-java-defer (round 54), the fourth pass"): `go/arbace/jrt/date.ae`'s
  `dateParse`, `dateWords` and `dateActions` port jdk26u's (https://github.com/openjdk/jdk26u at `baf63fb`)
  `src/java.base/share/classes/java/util/Date.java` `parse(String)` and its tables, and the variant
  `overlay/jdk/variants/Util.ae` keeps the code of jdk26u's `sun/rmi/server/Util.java` methods it replaces: Copyright (c)
  Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below).
  The rest of them, and the variants `overlay/jdk/variants/FileInputStream.ae` and `HostFiles.ae` (jrt's own Java), are
  Arbace's own.
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
- java.io, zip and jar (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"; recorded by the
  rule of B7): `overlay/jdk/java.base/java/util/zip/ZipFile.java` and
  `overlay/jdk/java.base/java/util/jar/JarFile.java` are jdk26u's files of the same paths
  (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.base/share/classes/`) transcribed with
  the changes their headers name, `overlay/jdk/java.base/java/io/FileDescriptor.java` holds
  jdk26u's `attach` and `closeAll`, and the variants `overlay/jdk/variants/RandomAccessFile.ae`,
  `Adler32.ae`, `CRC32C.ae` and `URLJarFile.ae` keep the code of the jdk26u methods they replace
  (`java/io/RandomAccessFile.java`, `java/util/zip/Adler32.java`, `CRC32C.java`,
  `sun/net/www/protocol/jar/URLJarFile.java`) where they keep it: Copyright (c) Oracle and/or its
  affiliates, under the GNU General Public License version 2 with the Classpath Exception (text
  below); the rest of those files, `overlay/jdk/java.base/java/io/FileCleanable.java`,
  `jdk/internal/jrt/ZipAccess.java`, `Utf8Coder.java`, the variants `ZipCoder.ae`, `ZipUtils.ae`,
  `JarFileFactory.ae` and `go/arbace/jrt/checksum.ae` are Arbace's own (zlib's adler32 algorithm
  in `checksum.ae` written from RFC 1950).
  Since P1 (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: P1 (by day)", 2026-11-10) jrt's
  JarVerifier and ManifestEntryVerifier are gone (jdk26u's are translated) and `JarFile.java` holds
  jdk26u's verifier code again.
- java.lang.ref and thread groups (P1, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: P1 (by day)"):
  `go/arbace/jrt/threadgroup.ae` transcribes jdk26u's `java/lang/ThreadGroup.java` (at `baf63fb`) into
  Go: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with
  the Classpath Exception (text below); the PhantomReference and the thread "Reference Handler" in
  `go/arbace/jrt/reference.ae` and the thread groups' members of `thread.ae` are Arbace's own.
- Records' reflection and the platform logging bean (P2, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: P2 (by
  day)", 2026-11-11): `go/arbace/jrt/record.ae` transcribes jdk26u's `java/lang/reflect/RecordComponent.java` (at
  `baf63fb`) into Go, and `overlay/jdk/java.base/sun/management/ManagementFactoryHelper.java` now also holds jdk26u's
  `PlatformLoggingImpl` (its calls made directly): Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below); `go/arbace/jrt/record_test.ae` is Arbace's own.
- java.util.logging (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F5 (write-ahead)"; recorded by the
  rule of B7): the variants `overlay/jdk/variants/LogManager.ae`, `Logger.ae`, `Level.ae`,
  `LogRecord.ae`, `SimpleFormatter.ae` and `LoggingFileHandler.ae` keep the code of the jdk26u members
  they replace (https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/java.logging/share/classes/java/util/logging/LogManager.java`, `Logger.java`, `Level.java`,
  `LogRecord.java`, `FileHandler.java`, and `src/java.base/share/classes/jdk/internal/logger/SimpleConsoleLogger.java`,
  whose `Formatting.getSimpleFormat` and `isFilteredFrame` `SimpleFormatter.ae` and `LogRecord.ae`
  transcribe) where they keep it: Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below); the rest of those files and
  `overlay/jdk/java.base/sun/util/logging/internal/LoggingProviderImpl.java` (jrt's own, with jdk26u's
  names and signatures of its access interface) are Arbace's own.
- `overlay/jdk/variants/TreeMap.ae` (doc/go/JAVA-BASE.md, amendment JB2): its two
  `buildFromIterator` methods are jdk26u's `buildFromSorted` (https://github.com/openjdk/jdk26u
  at `baf63fb`, `src/java.base/share/classes/java/util/TreeMap.java`) without the deserializing
  branch, and its other members keep the code of the members they replace: Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath
  Exception (text below), by the rule of B7; the rest of the file is Arbace's own.
- Permissions, JAAS's core and PKI (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F4 (write-ahead)",
  "As built: F2 (write-ahead)"; recorded by the rule of B7): the variants
  `overlay/jdk/variants/Password.ae`, `PasswordCallback.ae`, `LoginContext.ae`, `Configuration.ae`,
  `FilePermission.ae` (F4), `SignatureAccess.ae`, `X500Principal.ae`, `X500Name.ae`,
  `SignatureUtil.ae`, `JceKeyProtector.ae`, `PKCS12KeyStore.ae`, `Sun.ae` and `SunEntriesPki.ae` (F2)
  keep the jdk26u members they replace or add (https://github.com/openjdk/jdk26u at `baf63fb`, the
  classes' files of `src/java.base/share/classes`) as repr/java converts them, with the changes they
  name: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
  with the Classpath Exception (text below); the rest of those files, the variants `Subject.ae`,
  `PortConfig.ae`, `RegisteredDomain.ae` and `OCSP.ae`, and
  `overlay/jdk/java.base/jdk/internal/event/EventHelper.java` (with jdk26u's names and signatures),
  `jdk/internal/jrt/PkiAccess.java` and `sun/security/provider/PkiServices.java` (its service class
  names are jdk26u's providers') are Arbace's own. Their jdk26u sources join `jdk/forms/` (the
  closure's files above) once `bin/jrt-convert update` has run: 86 files of permission-sources (69
  of `src/java.base/share/classes`, 1 of `src/java.base/unix/classes`, 15 of
  `src/jdk.security.auth/share/classes`, 1 of `src/java.logging/share/classes`) and the 2 security
  message bundles the build's `CompileProperties` generates, and 219 of pki-sources (218 of
  `src/java.base/share/classes`, 1 of `src/jdk.security.auth/share/classes`).
- The JCA's tail and SASL (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F23 (write-ahead)"; recorded by the rule
  of B7): the variants `overlay/jdk/variants/GaloisCounterMode.ae` (since F23 its `overlapDetection` and `GCMDecrypt.doFinal`)
  and `ProviderConfig.ae`'s SunSASL case (`ProviderConfigSasl.ae` until JCS232 (a) folded it in) keep the jdk26u members they replace (https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/java.base/share/classes/com/sun/crypto/provider/GaloisCounterMode.java`, `sun/security/jca/ProviderConfig.java`) as
  repr/java converts them, with the changes they name: Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below); the rest of those files and the cases F23 adds to
  `overlay/jdk/java.base/com/sun/crypto/provider/JceServices.java` and `sun/security/provider/JrtServices.java` are Arbace's
  own. Their jdk26u sources join `jdk/forms/` once `bin/jrt-convert update` has run: jca-tail-sources' 3 files of
  `src/java.base/share/classes` (`CtrDrbg`, `MD4`, `GCMParameters`) and sasl-sources' 34 (28 of
  `src/java.security.sasl/share/classes`, 5 of `src/java.base/share/classes/com/sun/security/ntlm`, 1 of
  `src/jdk.security.auth/share/classes`), under the same license.
- JC9b's small members, charsets, file channels and datagrams (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built:
  F16 (write-ahead)", "As built: F8 (write-ahead)", "As built: F7 (write-ahead)", "As built: F11 (write-ahead)";
  recorded by the rule of B7): jdk26u's files (https://github.com/openjdk/jdk26u at `baf63fb`) transcribed with
  the changes their headers name, `overlay/jdk/java.base/java/util/concurrent/atomic/AtomicIntegerFieldUpdater.java`,
  `AtomicLongFieldUpdater.java`, `AtomicReferenceFieldUpdater.java` (F16) and `java/nio/channels/FileChannel.java`
  (F7, until F9: jdk26u's file again); `java/nio/DirectByteBuffer.java` and `DirectByteBufferR.java` (F7), derived from the JDK build's generated
  `HeapByteBuffer.java` and `HeapByteBufferR.java` (`make/modules/java.base/gensrc/GensrcBuffer.gmk` over
  `src/java.base/share/classes/java/nio/Heap-X-Buffer.java.template`); `java/nio/MappedByteBuffer.java` and
  `sun/nio/ch/FileChannelImpl.java` (F7), which keep jdk26u's public members' documented behaviour, checks,
  messages and some of their code, and `java/io/FileInputStream.java` and `FileOutputStream.java`, jdk26u's
  `getChannel` and the channel's close; the variants `overlay/jdk/variants/Timer.ae`, `PropertyResourceBundle.ae`
  (F16), `Charset.ae`, `CsStandardCharsets.ae`, `CsStringUTF16.ae`, `GB18030.ae` (F8),
  `AbstractInterruptibleChannel.ae`, `RandomAccessFile.ae`'s close (F7) and `DatagramSocket.ae` (F11), which keep
  the jdk26u members they replace as repr/java converts them: Copyright (c) Oracle and/or its affiliates, under
  the GNU General Public License version 2 with the Classpath Exception (text below); the rest of those files,
  `overlay/jdk/java.base/jdk/internal/jrt/FieldUpdaters.java`, `java/sql/DriverManager.java` (with jdk26u's
  names and signatures), the variant `Scanner.ae` (F16), `sun/nio/cs/JrtCharsets.java` (its class names are
  jdk26u's charsets'), `jdk/internal/jrt/CharsetStrings.java` (F8), `jdk/internal/jrt/HostFileChannels.java`
  (F7), `java/net/HostDatagramSocketImpl.java`, `jdk/internal/jrt/HostDatagram.java` and the variants
  `NetworkInterface.ae`, `InterfaceAddress.ae` (F11) are Arbace's own. Their jdk26u sources join `jdk/forms/`
  once `bin/jrt-convert update` has run: 27 files of small-members-sources (19 of `src/java.base/share/classes`,
  5 of `src/java.sql/share/classes`, 3 of `src/java.sql.rowset/share/classes`), 42 of charset-sources and the 51
  charset sources the build's `Spp` and charsetmapping tool generate (charset-gensrc), 23 of channel-sources and
  35 generated buffers (added-gensrc), and 12 of datagram-sources (11 of `src/java.base/share/classes`, 1 of
  `src/java.base/unix/classes`); the data `uprops.icu` (ICU's, under the Unicode License V3 as jdk26u's
  `legal/icu.md` says, beside `ubidi.icu`) and `sun/net/www/content-types.properties`.
- F10a's selectors and socket channels (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10a (write-ahead)";
  recorded by the rule of B7): jdk26u's files (https://github.com/openjdk/jdk26u at `baf63fb`) transcribed or
  derived with the changes their headers name, in `overlay/jdk/java.base/sun/nio/ch/`: `Net.java` (derived from
  `share/classes/sun/nio/ch/Net.java`), `PollSelectorImpl.java` (from `unix/classes/sun/nio/ch/PollSelectorImpl.java`),
  `SocketOptionRegistry.java` (from `share/classes/sun/nio/ch/SocketOptionRegistry.java.template`), `NativeDispatcher.java`,
  `UnixDispatcher.java`, `EventFD.java`, `NativeThread.java`, `Util.java`'s `ungrowableSet`, and the variant
  `overlay/jdk/variants/SelectorProvider.ae`, which keeps the jdk26u member it replaces, and `go/arbace/jrt/channels_linux.ae`,
  a port to Go forms of jdk26u's `src/java.base/unix/native/libnio/ch/{Net,IOUtil,SocketDispatcher,UnixDispatcher,
  PollSelectorImpl}.c`, `src/java.base/linux/native/libnio/ch/{EPoll,EventFD}.c` and
  `src/java.base/unix/native/libnet/net_util_md.c`'s `NET_SetSockOpt`, `NET_GetSockOpt` and `NET_Bind` (their system
  calls, flags, conversions and results): Copyright (c) Oracle and/or its affiliates, under the GNU General Public
  License version 2 with the Classpath Exception (text below); the rest of those files, `IOUtil.java`,
  `SocketDispatcher.java`, `Poller.java`, `EPoll.java`, `FileDispatcherImpl.java` (jdk26u's
  names and signatures, jrt's code), `jdk/internal/jrt/HostChannels.java`, the variants `SocketChannelImpl.ae`,
  `PollSelectorProvider.ae` and `EPollSelectorProvider.ae`, `go/arbace/jrt/channels_other.ae` and `channels_test.ae`
  are Arbace's own.
  Their jdk26u sources join `jdk/forms/` once `bin/jrt-convert update` has run: 47 files of selector-sources (40 of
  `src/java.base/share/classes`, 4 of `src/java.base/unix/classes`, 3 of `src/java.base/linux/classes`).
- F10b's datagram channels and multicast (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F10b (write-ahead)";
  recorded by the rule of B7): jdk26u's files (https://github.com/openjdk/jdk26u at `baf63fb`) transcribed with the
  changes their headers name, `overlay/jdk/java.base/sun/nio/ch/NativeSocketAddress.java` (from
  `share/classes/sun/nio/ch/NativeSocketAddress.java`) and `DatagramDispatcher.java` (from
  `unix/classes/sun/nio/ch/DatagramDispatcher.java`), `Net.java`'s multicast natives, the variant
  `overlay/jdk/variants/DatagramChannelImpl.ae`, which keeps the jdk26u members it replaces, and the part of
  `go/arbace/jrt/channels_linux.ae` that ports `src/java.base/unix/native/libnio/ch/DatagramChannelImpl.c`,
  `DatagramDispatcher.c` and `Net.c`'s `joinOrDrop4/6`, `blockOrUnblock4/6`, `setInterface4/6`, `getInterface4/6` and
  `initGroupSourceReq`: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with
  the Classpath Exception (text below); the rest of those files, the variants `DatagramSocketAdaptor.ae` and
  `DatagramSocket.ae` (since F10b only an added accessor), `NetworkInterface.ae`'s accessors and
  `go/arbace/jrt/datagrams_test.ae` are Arbace's own. Their jdk26u sources join `jdk/forms/` once `bin/jrt-convert
  update` has run: 7 files of datagram-channel-sources (`src/java.base/share/classes`).
- F9's java.nio.file and JCS162's interrupts (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F9 (write-ahead)", "As
  built: JCS162"; recorded by the rule of B7): jdk26u's files (https://github.com/openjdk/jdk26u at `baf63fb`) derived
  with the changes their headers name (`.tmp/f9/derive.py`'s asserted replacements), in `overlay/jdk/java.base/`:
  `java/nio/file/Files.java`, `FileSystems.java`, `FileChannelLinesSpliterator.java`, `spi/FileSystemProvider.java`,
  `sun/nio/fs/UnixNativeDispatcher.java`, `LinuxNativeDispatcher.java`, `LinuxWatchService.java`, `UnixFileSystem.java`,
  `UnixChannelFactory.java`, `UnixPath.java`, `NativeBuffer.java`, `NativeBuffers.java` and
  `UnixUserDefinedFileAttributeView.java`; `sun/nio/ch/FileChannelImpl.java`'s `setUninterruptible` (jdk26u's);
  `go/arbace/jrt/nio_fs_linux.ae`, a port to Go forms of jdk26u's
  `src/java.base/unix/native/libnio/fs/{UnixNativeDispatcher,UnixFileSystem}.c` and
  `src/java.base/linux/native/libnio/fs/{LinuxNativeDispatcher,LinuxWatchService}.c` (their system calls, flags,
  results and errors), and `go/arbace/jrt/thread.ae`'s `Thread.interrupt` and `blockedOn` (jdk26u's
  `java/lang/Thread.java`'s): Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
  with the Classpath Exception (text below); the rest of those files, `go/arbace/jrt/nativemem.ae`, `nio_fs_other.ae`,
  `nio_fs_linux_amd64.ae`, `nio_fs_linux_arm64.ae`, `nio_fs_test.ae`, `jdk/internal/jrt/NioStrings.java`,
  `jdk/internal/jrt/HostThreads.java`, `HostFiles.java`'s `adopt` and the variant `AbstractInterruptibleChannel.ae`
  are Arbace's own. Their jdk26u sources join `jdk/forms/` once `bin/jrt-convert update` has run: 91 files of
  nio-file-sources (70 of `src/java.base/share/classes`, 15 of `src/java.base/unix/classes`, 6 of
  `src/java.base/linux/classes`) and the generated `sun/nio/fs/UnixConstants.java`.
- JCS164's UNIX domain sockets (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS164 (write-ahead)"; recorded by the
  rule of B7): jdk26u's `src/java.base/share/classes/sun/nio/ch/UnixDomainSockets.java` (https://github.com/openjdk/jdk26u
  at `baf63fb`) derived with the changes its header names (`.tmp/unix/derive.py`'s asserted replacements) into
  `overlay/jdk/java.base/sun/nio/ch/UnixDomainSockets.java`, and `go/arbace/jrt/channels_linux.ae`'s UNIX section, a port
  to Go forms of jdk26u's `src/java.base/unix/native/libnio/ch/UnixDomainSockets.c` (its system calls, its sockaddr_un's
  layout and lengths, results and errors): Copyright (c) Oracle and/or its affiliates, under the GNU General Public
  License version 2 with the Classpath Exception (text below); the rest of those files, `HostChannels.java`'s five
  `unix` natives, `channels_other.ae`'s and `channels_test.ae`'s UNIX parts are Arbace's own. Their jdk26u sources join
  `jdk/forms/` once `bin/jrt-convert update` has run: the 2 files of unix-socket-sources (`java/net/UnixDomainSocketAddress.java`
  of `src/java.base/share/classes`, `sun/nio/ch/UnixDomainSocketsUtil.java` of `src/java.base/unix/classes`).
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
- TLS (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F3 (write-ahead)"; recorded by the rule of B7): the
  variants `overlay/jdk/variants/SSLContext.ae`, `SNIHostName.ae`, `SSLSocketImpl.ae`,
  `InetAddressAccess.ae` and `ProviderConfig.ae`'s SunJSSE case (`ProviderConfigJsse.ae` until JCS232 (a)) keep the jdk26u members they replace or add
  (https://github.com/openjdk/jdk26u at `baf63fb`, the classes' files of `src/java.base/share/classes`)
  as repr/java converts them, with the changes they name, and `overlay/jdk/java.base/sun/security/ssl/SSLLogger.java`
  is jdk26u's `SSLLogger.java` transcribed with the changes its header names: Copyright (c) Oracle and/or
  its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text
  below), as its notice says; the rest of the variants, `overlay/jdk/java.base/sun/security/ssl/JsseServices.java`
  (its service class names are jdk26u's SunJSSE's) and `jdk/internal/jrt/NetAccess.java` are Arbace's own.
  Their jdk26u sources join `jdk/forms/` (the converted closure, under the same license as above): 205 of
  tls-sources (`src/java.base/share/classes`); the resource data `sun/net/idn/uidna.spp` joins `jdk/data/`
  (the resource data's item below).
- URL and HTTP's classes (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F12 (write-ahead)"; recorded by the
  rule of B7): the variants `overlay/jdk/variants/DefaultProxySelector.ae`, `HttpClient.ae`,
  `HttpConnectSocketImpl.ae`, `HttpCookieAccess.ae` and `HttpURLConnectionImpl.ae` keep the jdk26u members
  they replace (https://github.com/openjdk/jdk26u at `baf63fb`, the classes' files of
  `src/java.base/share/classes`) as repr/java converts them, with the changes they name, and
  `overlay/jdk/java.base/sun/util/logging/PlatformLogger.java` is jdk26u's `PlatformLogger.java` transcribed
  with the changes its header names, its `JulBridge` from jdk26u's
  `src/java.logging/share/classes/sun/util/logging/internal/LoggingProviderImpl.java`: Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text
  below), as its notice says; the rest of the variants (`ProxySelector.ae`, `NetProperties.ae` too),
  `overlay/jdk/java.base/jdk/internal/misc/InnocuousThread.java` (written after jdk26u's documented
  factories) and NetAccess's cookie access are Arbace's own. Their jdk26u sources join `jdk/forms/` (the
  converted closure, under the same license as above): 82 of http-sources (`src/java.base/share/classes`);
  `conf/net.properties` joins `jdk/data/` (the resource data's item below).
- javax.script's API and JNDI's names (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F19 (write-ahead)"; recorded by
  the rule of B7): the variants `overlay/jdk/variants/ScriptEngineManager.ae`, `VersionHelper.ae` and
  `NamingManager.ae` keep the jdk26u members they replace (https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/java.scripting/share/classes`, `src/java.naming/share/classes`) as repr/java converts them, with the changes
  they name: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with the
  Classpath Exception (text below); the rest of the variants is Arbace's own. Their jdk26u sources join `jdk/forms/`
  (the converted closure, under the same license): 63 of naming-script-sources.
- jdk.httpserver (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F24 (write-ahead)"; recorded by the rule of B7):
  `overlay/jdk/java.base/sun/net/httpserver/{ServerImpl,ExchangeImpl,HttpConnection,HttpContextImpl,ServerConfig}.java`
  are jdk26u's files of the same names (https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/jdk.httpserver/share/classes/sun/net/httpserver/`) with the exact replacements `test/jrt/derive.ae` makes (each
  file's header says so): Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
  with the Classpath Exception (text below), the derivation Arbace's own; `overlay/jdk/java.base/jdk/internal/jrt/SystemLoggers.java`
  holds jdk26u's `java.lang.System.Logger` interface (`src/java.base/share/classes/java/lang/System.java`) and the
  System.Logger half of `LoggingProviderImpl.JULWrapper` (`src/java.logging/share/classes/sun/util/logging/internal/`)
  under the same license, the rest Arbace's own; the variants `overlay/jdk/variants/HttpServerProvider.ae` and
  `LogManager.ae`'s `LoggingProviderAccess` keep the jdk26u members they replace as repr/java converts them (the same
  license), with the changes they name. Their jdk26u sources join `jdk/forms/` (the converted closure, under the same
  license): 38 of httpserver-sources (`src/jdk.httpserver/share/classes`).
- java.home, the crypto policy and the trust anchors (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS233"; recorded
  by the rule of B7): since JCS233 (a) `overlay/jdk/java.base/javax/crypto/JceSecurity.java` also holds jdk26u's
  `setupJurisdictionPolicies`, its static initializer, `getDefaultPolicy`, `getExemptPolicy` and `isRestricted`
  (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.base/share/classes/javax/crypto/JceSecurity.java.template`, its
  `@@JCE_DEFAULT_POLICY@@` as `unlimited`), and `JceSecurityManager.java` jdk26u's `getCryptoPermission` (but the code
  base's walk) and `getDefaultPermission` (`javax/crypto/JceSecurityManager.java`): Copyright (c) Oracle and/or its
  affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below), the rest Arbace's
  own; jca-sources' `javax/crypto/CryptoPermissions.java` and `CryptoPolicyParser.java` join `jdk/forms/` under the same
  license. The resource data `jdk/data/conf/security/policy/` (README.txt and the limited and unlimited policies,
  `src/java.base/share/conf/security/policy/`) and `jdk/data/lib/security/cacerts` (the JDK build's image file, made by
  `make/modules/java.base/Gendata.gmk`'s GenerateCacerts from `src/java.base/share/data/cacerts/`, jdk26u's CA list:
  public certificates, under the same license as jdk26u's tree) are jdk26u's; `go/arbace/jrt/javahome.ae` and its test
  are Arbace's own (Eclipse Public License 1.0).
- Kerberos's permissions (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS230"; recorded by the rule of B7):
  kerberos-sources' 2 files, jdk26u's (https://github.com/openjdk/jdk26u at `baf63fb`)
  `src/java.security.jgss/share/classes/javax/security/auth/kerberos/DelegationPermission.java` and
  `ServicePermission.java`, join `jdk/forms/` (the converted closure): Copyright (c) Oracle and/or its affiliates, under
  the GNU General Public License version 2 with the Classpath Exception (text below).
- java.net.http's client (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS254 (b)"; recorded by the rule of B7): the 15
  overlays `overlay/jdk/java.base/jdk/internal/net/http/{HttpClientImpl,ExchangeImpl,Exchange,HttpConnection,Response,AltSvcProcessor,Http1Response,WindowController,RawChannelTube,HttpClientFacade}.java`,
  `common/{Log,Logger,DebugLogger,Utils}.java` and `hpack/HPACK.java` are jdk26u's files of the same names
  (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.net.http/share/classes/jdk/internal/net/http/`) with the exact
  replacements and cuts `test/jrt/derive.ae` makes (each file's header says so), and `test/jrt/derive.ae`'s
  `http-derivations` hold text of those files as its OLD strings: Copyright (c) Oracle and/or its affiliates, under the
  GNU General Public License version 2 with the Classpath Exception (text below), the derivation Arbace's own. Their
  jdk26u sources join `jdk/forms/` (the converted closure, under the same license): 140 of http-client-sources
  (`src/java.net.http/share/classes`).
- jrt's derivations as data (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS251 (a), JCS253"; recorded by the rule
  of B7): `test/jrt/derive.ae`'s tables hold, as the OLD and NEW strings of its replacements, text of jdk26u's files
  (https://github.com/openjdk/jdk26u at `baf63fb`: `src/java.base/share/classes/sun/nio/ch/Net.java`,
  `SocketOptionRegistry.java.template`, `UnixDomainSockets.java`, `src/java.base/unix/classes/sun/nio/ch/PollSelectorImpl.java`,
  the thirteen files of F9's table under `src/java.base/{share,unix,linux}/classes/`, and the JDK build's generated
  `java/nio/HeapByteBuffer.java` and `HeapByteBufferR.java`): Copyright (c) Oracle and/or its affiliates, under the GNU
  General Public License version 2 with the Classpath Exception (text below); the rest of `test/jrt/derive.ae`, the
  bodies and helpers it writes and `test/jrt/vet.ae` are Arbace's own. The overlays it makes are covered above with
  their features (F7, F9, F10a, F10b, JCS164, F24).
- JMX's core, AccessFlag and type annotations (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JMX-1 (write-ahead)", "As built:
  JCS273 (write-ahead)", "As built: JCS271 (b) (write-ahead)"; recorded by the rule of B7): the derived overlays
  `overlay/jdk/java.base/{com/sun/jmx,javax/management}/...` (21 files), `java/lang/management/ThreadInfo.java`,
  `java/lang/reflect/AccessFlag.java` and `sun/reflect/annotation/{TypeAnnotationParser,AnnotationType,AnnotationSupport}.java`
  are jdk26u's files of the same paths (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.management/share/classes/`,
  `src/java.base/share/classes/`) with the exact replacements `test/jrt/derive.ae` makes, whose tables hold their replaced
  text (each file's header says so); `overlay/jdk/java.base/jdk/internal/jrt/{AccessFlagSupport,TypeAnnotationSupport}.java`
  hold jdk26u's bodies of `Class.java`'s, `java/lang/reflect/Executable.java`'s, `Method.java`'s, `Constructor.java`'s,
  `Field.java`'s and `jdk/internal/reflect/ReflectionFactory.java`'s members their headers name, and
  `jdk/internal/jrt/ClassFileAccess.java` the names and values of `java/lang/classfile/ClassFile.java`'s access flags; and
  the jdk26u sources the closure adds (`test/jrt/jrt_sources.ae`'s `jmx-sources`, `accessflag-sources`,
  `type-annotation-sources`) join `jdk/forms/` (the converted closure): Copyright (c) Oracle and/or its affiliates, under the
  GNU General Public License version 2 with the Classpath Exception (text below), the derivations and the rest of those
  files Arbace's own; `jdk/internal/jrt/TypeAnnotationData.java`, `jdk/internal/reflect/ConstantPool.java` (jrt's own, in
  place of jdk26u's), the variant `overlay/jdk/variants/TypeVariableImpl.ae`, `arbace/classes/annot.ae`'s type annotation
  records and `go/arbace/jrt/classdata.ae`'s natives are Arbace's own, under the Eclipse Public License 1.0. F13's
  overlays `LockInfo.java`, `MonitorInfo.java`, `MemoryUsage.java`, `javax/management/ObjectName.java` and
  `openmbean/CompositeData.java` are removed (jdk26u's in their place).
- java.lang.invoke (F15, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F15 (java.lang.invoke)"; recorded by the rule of
  B7): the derived overlays `overlay/jdk/java.base/java/lang/invoke/{MethodType,MethodTypeForm,LambdaMetafactory}.java` and
  `sun/invoke/util/VerifyAccess.java` are jdk26u's files of the same paths (https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/java.base/share/classes/`) with the exact replacements and cuts `test/jrt/derive.ae` makes (`invoke-derivations`,
  whose tables hold their replaced text; each file's header says so); `java/lang/invoke/MethodHandleStatics.java` and
  `MethodHandleNatives.java` hold jdk26u's bodies of the members of the files of the same names their headers name, and
  `MethodHandle.java`, `MethodHandles.java` and `CallSite.java`, `ConstantCallSite.java`, `MutableCallSite.java`,
  `VolatileCallSite.java` jdk26u's text of the members their headers name (the argument checks and their messages, the
  lookup modes, `in`, `dropLookupMode`, `toString`, the varargs collector's `asTypeUncached`); and the jdk26u sources the
  closure adds (`test/jrt/jrt_sources.ae`'s `invoke-sources`) join `jdk/forms/`: Copyright (c) Oracle and/or its
  affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below), the derivations and
  the rest of those files Arbace's own; `java/lang/invoke/DirectMethodHandle.java`, `MethodHandleImpl.java`,
  `ProxyLambdaMetafactory.java` (jrt's own, in place of jdk26u's), `jdk/internal/jrt/Invokers.java`, the analyzer's
  rewrite in `arbace/classes/analyze.ae` and `go/arbace/jrt/natives.ae`'s two natives are Arbace's own, under the Eclipse
  Public License 1.0. StringConcatFactory (JCS353 (a), doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS350, JCS351,
  JCS353"; recorded by the rule of B7): `overlay/jdk/java.base/java/lang/invoke/StringConcatFactory.java` holds jdk26u's text
  (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java`) of
  the members its header names (makeConcat, makeConcatWithConstants' checks and messages, parseRecipe, argumentMismatch,
  constantMismatch, makeSimpleConcat, unaryConcat) and of `StringConcatHelper.stringOf`: Copyright (c) Oracle and/or its
  affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below); the adapters in
  place of the hidden class and the rest of the file, and `test/jrt/scf-diff/`, are Arbace's own, under the Eclipse Public
  License 1.0.
- java.lang.reflect.Parameter and Runtime.Version.parse (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS280 (a)",
  "As built: JCS304 (b), JCS293"; recorded by the rule of B7): the derived overlay `overlay/jdk/java.base/java/lang/reflect/Parameter.java`
  is jdk26u's file of the same path (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.base/share/classes/`) with the
  exact replacements `test/jrt/derive.ae` makes; `overlay/jdk/java.base/jdk/internal/jrt/ParameterSupport.java` transcribes
  jdk26u's `Executable.getAllGenericParameterTypes` (its branch without real parameter data), and
  `AccessFlagSupport.parameterAccessFlags` `Parameter.accessFlags`; `go/arbace/jrt/system.ae`'s `Version.parse` ports
  jdk26u's `Runtime.java` (`Version.parse`, `isSimpleNumber`, `VersionPattern`, `toString`, `compareTo`, `equalsIgnoreOptional`,
  `compareToIgnoreOptional`): Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with
  the Classpath Exception (text below); the rest of those files, the sample `test/j2g/unit/typeann/` and `test/j2g/typeann.ae`
  are Arbace's own, under the Eclipse Public License 1.0.
- Java RMI (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F27 (write-ahead)"; recorded by the rule of B7): the derived
  overlays `overlay/jdk/java.base/java/rmi/server/RMIClassLoader.java` and
  `overlay/jdk/java.base/sun/rmi/{transport/GC,runtime/Log,runtime/RuntimeUtil}.java` are jdk26u's files
  of the same paths (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.rmi/share/classes/`) with the exact
  replacements `test/jrt/derive.ae` makes, whose tables hold their replaced text (each file's header says so); and the
  jdk26u sources the closure adds (`test/jrt/jrt_sources.ae`'s `rmi-sources`: 89 files of `src/java.rmi/share/classes`
  and `src/java.base/share/classes/java/lang/NoSuchMethodError.java`) join `jdk/forms/` (the converted closure); likewise
  JMX-2's, its connectors: the derived overlays `overlay/jdk/java.base/javax/management/remote/{JMXConnectorFactory,
  JMXConnectorServerFactory}.java`, `com/sun/jmx/remote/internal/{ClientCommunicatorAdmin,ServerCommunicatorAdmin,
  ClientNotifForwarder}.java` (`src/java.management/share/classes/`) and `javax/management/remote/rmi/RMIConnector.java`
  (`src/java.management.rmi/share/classes/`), and `jmx-remote-sources`' 41 files of those two trees: Copyright
  (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text
  below), the derivations Arbace's own; `go/arbace/jrt/rmi.ae` (GC.maxObjectInspectionAge's native, after librmi's
  `GC.c`, written anew over Go's runtime/debug) and its test, `overlay/jdk/java.base/jdk/internal/jrt/JmxProviders.java` (the
  providers java.management.rmi's module-info names), jrt's Thread context class loader (`go/arbace/jrt/thread.ae`)
  and the throws records (`arbace/classes/annot.ae`) are Arbace's own, under the Eclipse Public License 1.0.
- Java's older proposals (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: Java's older proposals (JCS191-JCS213)";
  recorded by the rule of B7): `go/arbace/jrt/serial_locks.ae` holds the serial forms and serialization hooks of jdk26u's
  `java/util/concurrent/locks/{ReentrantLock,ReentrantReadWriteLock,AbstractQueuedSynchronizer,AbstractQueuedLongSynchronizer,AbstractOwnableSynchronizer}.java`,
  `java/util/concurrent/Semaphore.java` and `ForkJoinTask.java` (https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/java.base/share/classes`; written by Doug Lea with members of JCP JSR-166 and released to the public domain, as their
  headers say, in files under Oracle's notice): their serialVersionUIDs, fields and hooks' bodies over jrt's state, under the
  GNU General Public License version 2 with the Classpath Exception (text below) as far as Oracle's notice covers them, the
  rest Arbace's own. `overlay/jdk/java.base/sun/nio/ch/InheritedChannel.java`, `overlay/jdk/java.base/jdk/net/ExtendedSocketOptions.java`
  and `LinuxSocketOptions.java` are jdk26u's files (`src/java.base/unix/classes/sun/nio/ch/InheritedChannel.java`,
  `src/jdk.net/share/classes/jdk/net/ExtendedSocketOptions.java`, `src/jdk.net/linux/classes/jdk/net/LinuxSocketOptions.java`)
  with the exact replacements `test/jrt/derive.ae` makes (their headers say so), and `go/arbace/jrt/channels_linux.ae`'s
  inherited-channel and extended-options sections port jdk26u's `src/java.base/unix/native/libnio/ch/InheritedChannel.c` and
  `src/jdk.net/linux/native/libextnet/LinuxSocketOptions.c` (their system calls, options and results): Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below); the
  derivations' tables are covered as above; the rest of those files, `HostChannels.java`'s inherited and extended-option
  natives, `channels_other.ae`'s and the tests' parts, `annotation.ae`'s invocation handler (jdk26u's
  `sun/reflect/annotation/AnnotationInvocationHandler.java`'s invoke, as F14's port) and `system.ae`'s Runtime.Version rules
  (jdk26u's `java/lang/Runtime.java`'s Version) are as F14 and the rest of jrt are covered. Their jdk26u sources join
  `jdk/forms/` once `bin/jrt-convert update` has run: jdk-net-sources (`src/jdk.net/share/classes/jdk/net/{NetworkPermission,Sockets,UnixDomainPrincipal}.java`).
- java.lang.management without JMX (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F13 (write-ahead)";
  recorded by the rule of B7): `overlay/jdk/java.base/java/lang/management/{ManagementFactory,ThreadInfo,LockInfo,MonitorInfo,MemoryUsage}.java`
  `overlay/jdk/java.base/sun/management/{ManagementFactoryHelper,VMManagementImpl}.java` and
  `overlay/jdk/java.base/com/sun/management/internal/OperatingSystemImpl.java` are jdk26u's files
  (https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.management/share/classes`, and
  `src/jdk.management/unix/classes` for the last) transcribed with the changes their headers name: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with
  the Classpath Exception (text below), as their notices say; the variants `overlay/jdk/variants/ThreadImpl.ae` and
  `BaseOperatingSystemImpl.ae`, `com/sun/management/internal/JrtPlatformBeans.java`, the
  skeletons `overlay/jdk/java.base/javax/management/ObjectName.java` and `openmbean/CompositeData.java` (written for
  JMX's documented signatures), `jdk/internal/jrt/HostManagement.java` and `go/arbace/jrt/management{,_linux,_tamago}.ae` are Arbace's
  own (written for jdk26u's documented natives and Linux's /proc and syscalls). Their jdk26u sources join `jdk/forms/` (the converted closure, under the same license): 29 of management-sources.
- `go/arbace/jrt/annotation.ae` (F14, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F14 (first slice,
  write-ahead)"): `AnnotationObj`'s `Equals_O__Z`, `HashCode__I`, `ToString__String`, `Element` and their helpers
  (`memberValueEquals`, `memberValueHashCode`, `memberValueToString`, the `*SourceString` functions, `quoteChar`,
  `cloneMember`, `memberMethods`), `withDefaults` and `hashMapOrder` (AnnotationParser's memberValues over
  AnnotationType's memberDefaults), `classAnnotations` (Class.createAnnotationData), `directlyAndIndirectlyPresent`,
  `containerFirst` and `containerType` (AnnotationSupport), the `AnnotatedElement_*` default functions, and the
  Field primitive accessors of `go/arbace/jrt/reflect.ae` (`getMessage`, `setPrimMessage`, `getPrim`, `setPrim`, the
  `Get*`/`Set*` methods) follow the code of https://github.com/openjdk/jdk26u at `baf63fb`
  (`src/java.base/share/classes/sun/reflect/annotation/{AnnotationInvocationHandler,AnnotationParser,AnnotationType,
  AnnotationSupport}.java`, `java/lang/Class.java`, `java/lang/reflect/AnnotatedElement.java`, `java/util/HashMap.java`'s
  iteration order, `jdk/internal/reflect/FieldAccessorImpl.java`): Copyright (c) Oracle and/or its affiliates, under the
  GNU General Public License version 2 with the Classpath Exception (text below); recorded as ports, by the rule of B7.
  The data format, its decoder, the per-type annotation classes, `annotation_test.ae`, `arbace/classes/annot.ae` and
  j2g's annotation types are Arbace's own.
- `go/arbace/jrt/fdlibm.ae` and the `FdLibm` part of `go/arbace/jrt/math.ae` (`Log`, `Cbrt`,
  `Pow`): jrt's hand-written Go forms porting, as written, https://github.com/openjdk/jdk26u at
  `baf63fb`, `src/java.base/share/classes/java/lang/FdLibm.java` (doc/go/JRT-NOTES.md, phase 1
  and "Phase 2B (step 5)"): Copyright (c) Oracle and/or its affiliates, under the GNU General
  Public License version 2 with the Classpath Exception (text below).
- `go/arbace/jrt/serial_classes.ae` (JCS167's second slice, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167
  slices 2-3 (write-ahead)"): the serialization hooks of jrt's hand-written classes port the bodies of
  https://github.com/openjdk/jdk26u at `baf63fb`, `src/java.base/share/classes/java/lang/{Throwable,StringBuilder,StringBuffer}.java`'s
  and `java/util/Date.java`'s `writeObject` and `readObject` (Throwable's validation of what it reads), with their
  classes' serialVersionUIDs and serial fields, and `StackTraceElement`'s `toString`, `equals` and `hashCode` in
  `go/arbace/jrt/throwable.ae` port `java/lang/StackTraceElement.java`'s at the same commit: Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below);
  the rest of these files is Arbace's own, under the Eclipse Public License 1.0. `arbace/classes/suid.ae`
  (JCS168) ports `src/java.base/share/classes/java/io/ObjectStreamClass.java`'s `computeDefaultSUID` and the parts of
  `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/Lower.java` that name a serializable class's synthetic
  members (`outerThisName`, `shouldEmitOuterThis`, `assertFlagTest`) at the same commit, under the same license;
  the samples of `test/jrt/serialization/suid/` and the recorders `test/jrt/serialization/{SerialRecord,DefaultSUIDs}.java`
  and `suid/runner/` are Arbace's own (Eclipse Public License 1.0).
- `arbace/repr/c/` (repr/c, C's bridge; doc/C23-SURVEY.md, "As built: C2"): `lex.ae` and `pp.ae`,
  repr/c's lexer and preprocessor, are Arbace forms porting, function by function,
  modernc.org/cc/v4 v4.29.7 as go-whim carries it, https://github.com/arbace/go-whim at
  `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, `crefactor/cc/scanner.go`, `cpp.go` and the parts of
  `etc.go` and `cc.go` they use: Copyright (c) 2014 The CC Authors, under the BSD 3-Clause License
  ("cc (modernc.org/cc/v4) and go-whim's C23 delta" below; each file carries cc's notice); what
  derives from go-whim's own C23 delta to cc (the scanner's digit separators and `u8'a'`, marked
  `go-whim` there), licensed to Arbace by its author under the Eclipse Public License 1.0 (CS37 (a),
  2026-10-28), and Arbace's changes (marked `Arbace:`) are under the Eclipse Public License 1.0. The
  rest of `arbace/repr/c/` (`host.ae`, `ppcheck.ae`) and the helper `tools/ccdump/`, which imports
  go-whim's cc (or upstream's, its build tag `upstream`) and copies none of it, are Arbace's own,
  under the Eclipse Public License 1.0. Since C3 (doc/C23-SURVEY.md, "As built: C3"), `parse.ae`,
  repr/c's parser, ports likewise `crefactor/cc/parser.go` with go-whim's `c23.go` (its productions
  followed line for line, marked `go-whim`, Eclipse Public License 1.0 by CS37 (a)) and the parts of
  `ast.go`, `ast2.go`, `cc.go` (`Builtin`) and `etc.go` the parser uses; `tree.ae` holds the node types'
  table derived from cc's generated `ast.go` and the kinds' names of `stringer.go` (BSD 3-Clause, cc's
  notice), the rest of it Arbace's; `parsecheck.ae` is Arbace's own (Eclipse Public License 1.0).
  Since C3b (doc/C23-SURVEY.md, "As built: C3b"), `arbace/repr/c.ae`, `convert.ae`, `print.ae`,
  `layout.ae` and `rtcheck.ae` (the converter, the printer and the round trip): the converter's and the
  printer's rules port those of go-whim's `crefactor/cemit` (the canonical spelling: one declarator a
  declaration, braces on every body, the source's parentheses kept, a macro invocation recovered from the
  source where it made a whole node) and `crefactor/clisp` (declarators read from the name outward, labels
  as items), at the same commit, licensed to Arbace by their author under the Eclipse Public License 1.0
  (CS37 (a), 2026-10-28); the rest of them is Arbace's own, under the Eclipse Public License 1.0.
  Since C4 (doc/C23-SURVEY.md, "As built: C4"), `check.ae` and `annot.ae` (the checker over C forms and its
  check against cc's checker, `bin/ci`) are Arbace's own (CS8 (a): written from N3220 and gcc 15.2's
  behaviour, nothing of cc's `check.go`, `type.go`, `value.go` or `abi.go` ported), under the Eclipse Public
  License 1.0, as is `tools/ccdump/types.go`, which imports cc.
- `test/c/c23/` (C23's conformance files, doc/C23-SURVEY.md, "As built: C3b", CS38 (a)): the 49 files of
  go-whim's `crefactor/c23conf/testdata/` at `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, copied unchanged
  (each file's git blob as `test/c/corpora.edn` records it), licensed to Arbace by their author under the
  Eclipse Public License 1.0 (CS37 (a)); the files `pp_*.c`, `lib_*.c` and `pp_embed.dat` beside them are
  Arbace's own, under the Eclipse Public License 1.0.
- `test/c/run/` (ci's own run programs) and `test/c/judge/` (the small files of ci's judges' tests, doc/C23-SURVEY.md,
  "As built: review 006's fixes"): Arbace's own, under the Eclipse Public License 1.0.
- `test/c/synth/` (synth-c23, C23's synthetic programs construct by construct, their table and generator, queue 172,
  doc/GATE-SURVEY.md GT8.6): Arbace's own, under the Eclipse Public License 1.0.
- `arbace/repr/java/lex.ae` and `arbace/repr/java/parse.ae` (repr/java's native lexer and parser,
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN8"): Arbace forms porting, method by method,
  https://github.com/openjdk/jdk26u at `baf63fb`, `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s
  `parser/UnicodeReader.java`, `JavaTokenizer.java`, `JavadocTokenizer.java` (its comments),
  `Tokens.java`, `JavacParser.java`, `tree/TreeInfo.java` (`getStartPos`, `getEndPos` and the
  helpers the parser calls) and `TreeMaker.java` (its factories' positions): Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath
  Exception (text below; each file carries the notice of the files it ports).
- `arbace/repr/rust/lex.ae`, `parse.ae`, `parse/expr.ae`, `parse/pat.ae`, `parse/item.ae` and the meta
  items, literals and identifiers' names of `native.ae` (repr/rust's native reader, doc/rust/NATIVE-READER.md):
  Arbace forms porting, function by function, rustc 1.99.1, https://github.com/rust-lang/rust at
  `58cf8f9c3` (the sources of Alpine's `rust-src-1.99.1-r0`, pinned by `tools/rsdump/apks.sha256`):
  `compiler/rustc_lexer/src/lib.rs`, `compiler/rustc_parse/src/lexer/mod.rs`, `tokentrees.rs` and (two entries of `UNICODE_ARRAY`) `unicode_chars.rs`,
  `compiler/rustc_parse/src/parser/`'s `mod.rs`, `attr.rs`, `path.rs`, `generics.rs`, `ty.rs`, `expr.rs`,
  `stmt.rs`, `pat.rs`, `item.rs`, `function.rs` and the paths of `diagnostics.rs` that accept,
  `compiler/rustc_ast/src/`'s `token.rs`, `ast.rs` (the tree's shapes), `util/classify.rs`, `util/parser.rs`,
  `util/literal.rs` and `attr/mod.rs`, and `compiler/rustc_span/src/symbol.rs` (the keywords, `Ident`'s
  Debug), with the escapes' checks of the crate rustc-literal-escaper 0.0.8 that rustc calls
  (https://github.com/rust-lang/literal-escaper; its `.crate`, pinned by rustc's `Cargo.lock` by SHA-256
  `bfe6f213fb658c8fb95baabd5420393438cf5a98d707f5dd701d9197c705f71e`): copyright the Rust project's
  contributors (The Rust Project Developers), dual-licensed under the MIT License or the Apache License
  2.0 at the licensee's option (`rustc_lexer`'s and the escaper's `license = "MIT OR Apache-2.0"`), taken
  here under the MIT License ("Rust (rustc 1.99.1): MIT License or Apache License 2.0" below).
  `arbace/repr/rust/unicode.ae` (generated by `bin/rust-unicode` and `test/rust/unicode_gen.ae`, Arbace's own
  code) holds data from the tables of two crates that rustc 1.99.1's `rustc_lexer` links, from
  https://crates.io, pinned by `tools/rsdump/unicode-crates.sha256` and rustc's `Cargo.lock`: unicode-ident
  1.0.24 (`.crate` SHA-256 `e6e4313cd5fcd3dad5cafa179702e2b244f760991f45397d14d4ebf38247da75`),
  `src/tables.rs`'s XID_Start and XID_Continue ranges, `license = "(MIT OR Apache-2.0) AND Unicode-3.0"`
  (the code MIT OR Apache-2.0, taken under the MIT License; the Unicode data under the Unicode License V3, copyright
  1991-2023 Unicode, Inc., text below), and unicode-properties 0.1.4 (`.crate` SHA-256
  `7df058c713841ad818f1dc5d3fd88063241cc61f49f5fbea4b951e8cf5a8d71d`), `src/tables.rs`'s `EMOJI_STATUS`,
  `license = "MIT/Apache-2.0"` (copyright the Rust project's developers; the MIT License below); the file
  carries the notice. The rest of `native.ae` (the facts, after `tools/rsdump`'s visitor, the dump and the check) and
  `test/rust/native-check.txt` are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/mir.ae` (the printer), `arbace/rust/back/codegen.ae` and `collect.ae` (the first slice of RF3, doc/rust/FRONTEND-SURVEY.md,
  "As built: RF1-RF3 (trial)"), and the helper's `tools/rsdump/mirdata.rs`: Arbace forms and Rust written after rustc 1.99.1,
  https://github.com/rust-lang/rust at `58cf8f9c3` (the sources of Alpine's `rustc-dev-1.99.1-r0` and `rust-src-1.99.1-r0`,
  pinned by `tools/rsdump/apks.sha256`), reading `compiler/rustc_middle/src/mir/{pretty,syntax,terminator,consts,statement}.rs`
  (the text of MIR: `write_mir_fn`, the `Debug` impls of places, operands, rvalues, statements and terminators, the labels of
  successors, `fmt_assert_args`) and `compiler/rustc_codegen_ssa/src/mir/{mod,analyze,block,operand,place,rvalue,statement}.rs`
  (the SSA analysis of locals, the order and merging of blocks, operands, places, rvalues, terminators), `compiler/rustc_mir_transform/src/
  remove_noop_landing_pads.rs`, `compiler/rustc_middle/src/mir/{traversal,mod}.rs` (mono reachability, `get_caller_location`'s walk),
  `compiler/rustc_monomorphize/src/collector.rs` (the collector's roots and uses) and `compiler/rustc_codegen_llvm/src/{intrinsic,consts}.rs`
  (the intrinsics' lowering, volatile loads and stores among them since 2026-11-08; `codegen_static`): copyright the Rust
  project's contributors, dual-licensed under the MIT License or the Apache License 2.0 at the licensee's option, taken here
  under the MIT License ("Rust (rustc 1.99.1): MIT License or Apache License 2.0" below). The rest (the forms' shapes,
  `doc/rust/MIR.md`, the judges, `bin/rust-front`) is Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/expand.ae`, `expand_format.ae`, `expand_derive.ae` and `resolve.ae` (RF4-RF5, doc/rust/FRONTEND-SURVEY.md, "As built:
  RF4-RF5 start"), and the helper's `tools/rsdump/front.rs`: Arbace forms and Rust written after rustc 1.99.1, the same revision and
  sources, reading `compiler/rustc_expand/src/{expand,config}.rs` and `mbe/{macro_parser,macro_rules,quoted,transcribe}.rs` (macro_rules!'
  arms, matching, transcription and its spacing, the statement macros' semicolon), `compiler/rustc_builtin_macros/src/{format,assert,
  edition_panic,source_util,concat,cfg,standard_library_imports}.rs` and `deriving/` (the builtins' and derives' expansions),
  `compiler/rustc_parse_format/src/lib.rs` (format strings), `compiler/rustc_resolve/src/{build_reduced_graph,imports,ident,late}.rs`
  (the def collector, imports, globs, preludes, ribs) and `compiler/rustc_middle/src/metadata.rs`, `compiler/rustc_metadata/src/creader.rs`
  (the helper's modes): copyright the Rust project's contributors, dual-licensed under the MIT License or the Apache License 2.0 at the
  licensee's option, taken here under the MIT License ("Rust (rustc 1.99.1): MIT License or Apache License 2.0" below). The rest (the
  judges `expand_check.ae` and `resolve_check.ae`, the samples `test/rust/front/expand1.rs` and `resolve1.rs`) is Arbace's own, under
  the Eclipse Public License 1.0.
- `arbace/rust/front/pp.ae`, `lower.ae` and `hir_pretty.ae` (RF6, doc/rust/FRONTEND-SURVEY.md, "As built: RF6"): Arbace forms written
  after rustc 1.99.1, the same revision and sources, reading `compiler/rustc_ast_pretty/src/pp.rs`, `pp/ring.rs` and
  `pprust/state.rs` (Oppen's printer as rustc keeps it, the comments copied forward, PrintState, token streams, macro definitions),
  `compiler/rustc_ast_lowering/src/{lib,item,expr,pat,block,format}.rs` (AST to HIR: the desugarings of `for`, `while`, `while let`,
  `?`, ranges and `format_args!`'s bytecode, use trees, generics, implicit self, elided lifetimes), `compiler/rustc_hir_pretty/src/lib.rs`
  (the HIR printer), `compiler/rustc_hir/src/hir.rs` (expression precedence), `compiler/rustc_ast/src/util/literal.rs` (literals'
  display) and `compiler/rustc_macros/src/print_attribute.rs` (parsed attributes' printing): copyright the Rust project's contributors,
  dual-licensed under the MIT License or the Apache License 2.0 at the licensee's option, taken here under the MIT License ("Rust (rustc
  1.99.1): MIT License or Apache License 2.0" below). The rest (the judge `hir_check.ae`, the sample `test/rust/front/lower1.rs`) is
  Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/collect.ae` (RF7, doc/rust/FRONTEND-SURVEY.md, "As built: RF7") and the helper's RF7 additions to
  `tools/rsdump/front.rs`'s `--std-names` (defaults, aliases, supertraits, lang items, doc(hidden)): Arbace forms and Rust written after
  rustc 1.99.1, the same revision and sources, reading `compiler/rustc_resolve/src/def_collector.rs` (definitions and their def path
  data), `compiler/rustc_hir_analysis/src/collect/{generics_of,resolve_bound_vars,type_of,clauses_of}.rs` (own parameters,
  is_late_bound_map, gather_explicit_clauses_of), `constrained_generic_params.rs` (setup_constraining_clauses), `hir_ty_lowering/{mod,
  bounds,dyn_trait}.rs` (type lowering, generic arguments with their defaults, poly trait refs, implicit sizedness bounds, associated
  item constraints), `compiler/rustc_metadata/src/rmeta/decoder/cstore_impl.rs` (visible_parent_map),
  `compiler/rustc_middle/src/ty/print/{mod,pretty}.rs` (the type printer: def paths, visible paths, impl paths, regions, binders' names,
  trait objects, opaque types, clauses) and `compiler/rustc_middle/src/ty/generics.rs` (own_args_no_defaults): copyright the Rust
  project's contributors, dual-licensed under the MIT License or the Apache License 2.0 at the licensee's option, taken here under the
  MIT License ("Rust (rustc 1.99.1): MIT License or Apache License 2.0" below). The rest (the judge `collect_check.ae`, the samples
  `test/rust/front/collect1.rs` and `collect2.rs`) is Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/typeck.ae` (RF9's first slice with RF8's part, doc/rust/FRONTEND-SURVEY.md, "As built: RF9a") and the helper's
  `--typeck` and `--std-iface` modes in `tools/rsdump/front.rs`: Arbace forms and Rust written after rustc 1.99.1, the same revision and
  sources, reading `compiler/rustc_hir_typeck/src/{expr,expectation,coercion,callee,op,place_op,pat,fallback,writeback,closure}.rs` and
  `method/{probe,confirm}.rs` (expressions with expectations, coercions, calls, operators, indexing, patterns and default binding
  modes, closures' signature deduction, the method probe), `compiler/rustc_infer/src/infer/` (type, integer and float variables),
  `compiler/rustc_trait_selection/src/traits/{select,project,fulfill}.rs` (selection of impls, normalization of projections, pending
  obligations) and `compiler/rustc_middle/src/ty/{typeck_results,adjustment}.rs` (the facts' shapes), and since RF9b ("As built: RF9b")
  `rustc_hir_typeck/src/{upvar,expr_use_visitor,cast,fn_ctxt/checks,gather_locals}.rs` (closure kinds, argument checking, casts, locals'
  initializers), `rustc_infer/src/infer/relate/{generalize,lattice}.rs` and `region_constraints/mod.rs` (generalization, LUB and GLB of
  regions) and `rustc_infer/src/traits/util.rs` with `rustc_type_ir/src/elaborate.rs` (the parameter environment's elaboration): copyright the Rust project's
  contributors, dual-licensed under the MIT License or the Apache License 2.0 at the licensee's option, taken here under the MIT License
  ("Rust (rustc 1.99.1): MIT License or Apache License 2.0" below). The rest (the judge `typeck_check.ae`) is Arbace's own, under the
  Eclipse Public License 1.0.
- `arbace/rust/front/build.ae` (RF10's first slice, doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (first slice)"): Arbace forms written
  after rustc 1.99.1, the same revision and sources, reading `compiler/rustc_hir_analysis/src/check/region.rs` and
  `compiler/rustc_middle/src/middle/region.rs` (the region scope tree, temporary scopes), `compiler/rustc_mir_build/src/thir/cx/{mod,expr,block}.rs`
  and `thir/pattern/mod.rs` (THIR), `compiler/rustc_mir_build/src/builder/{mod,cfg,scope,block,misc}.rs`,
  `builder/expr/{into,stmt,as_operand,as_temp,as_rvalue,as_place,as_constant,category}.rs` and
  `builder/matches/{mod,match_pair,buckets,test}.rs` (MIR building: scopes and drop trees, expressions, the match lowering), and the
  passes `CleanupPostBorrowck` and `AbortUnwindingCalls`' effect in `arbace/rust/front/build_run.ae`, with the layouts of scalars and
  scalar pairs after `compiler/rustc_abi/src/layout.rs`: copyright the Rust project's contributors, dual-licensed under the MIT License or
  the Apache License 2.0 at the licensee's option, taken here under the MIT License ("Rust (rustc 1.99.1): MIT License or Apache License
  2.0" below). The rest (the judge `build_check.ae`, the rest of `build_run.ae`) is Arbace's own, under the Eclipse Public License 1.0.
  RF10's second slice (doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (second slice)") adds to `build.ae`, from the same sources and
  under the same terms, the closures' capture analysis after `compiler/rustc_hir_typeck/src/{upvar,expr_use_visitor}.rs` and
  `compiler/rustc_passes/src/upvars.rs`, the canonical user type annotations after `compiler/rustc_hir_typeck/src/fn_ctxt/_impl.rs`,
  `method/confirm.rs` and `compiler/rustc_middle/src/ty/{typeck_results,print/pretty}.rs` (with `typeck.ae`'s part), lint scopes
  after `builder/scope.rs`' `maybe_new_source_scope`, allocations after `compiler/rustc_middle/src/mir/pretty.rs`' `write_allocations`;
  the judge `arbace/rust/front/capture_check.ae` and the helper's `--captures` are Arbace's own (the helper's terms are its own, above).
  RF10's third slice (doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (third slice)") adds to `build.ae`, from the same sources and under
  the same terms, the rest of `compiler/rustc_mir_build/src/builder/matches/{mod,match_pair,buckets,test,util}.rs` and `builder/block.rs`'
  let-else, `thir/pattern/{mod,const_to_pat}.rs`' ranges, slices and string constants, `builder/mod.rs`' `construct_const` and
  `builder/expr/into.rs`' `write_box_via_move` and functional update; and `arbace/rust/front/passes.ae`, Arbace forms written after
  `compiler/rustc_mir_transform/src/{lib,lint_and_remove_uninhabited,simplify}.rs` and `compiler/rustc_middle/src/mir/{terminator,traversal}.rs`,
  the same terms.
  RF10's fourth slice ("As built: RF10 (fourth slice)") adds to `passes.ae`, from the same sources and under the same terms,
  `compiler/rustc_mir_transform/src/{promote_consts,cleanup_post_borrowck,remove_noop_landing_pads,deref_separator,add_call_guards,
  post_analysis_normalize,elaborate_drops,elaborate_drop,abort_unwinding_calls,erase_deref_temps,elaborate_box_derefs,lower_intrinsics,
  remove_place_mention,patch}.rs`, `compiler/rustc_const_eval/src/check_consts/qualifs.rs`, `compiler/rustc_mir_dataflow/src/{move_paths/
  {mod,builder}.rs,un_derefer.rs,drop_flag_effects.rs,impls/initialized.rs,framework/direction.rs}` and `compiler/rustc_middle/src/ty/
  layout.rs`' `fn_can_unwind`; and to `build.ae` a port of rustc_apfloat 0.2.3's (`rustc_apfloat-0.2.3+llvm-462a31f5a5ab`,
  `src/ieee.rs`, `impl Display for IeeeFloat`) float formatting, a port of LLVM's APFloat::toString, under the Apache License 2.0
  with LLVM Exceptions (the text of section "LLVM (clang): Apache License 2.0 with LLVM Exceptions" below).
- `arbace/rust/front/mono.ae` (RFP23 (c), doc/rust/FRONTEND-SURVEY.md, "As built: RFP23 (c)"): Arbace forms porting, function by
  function, rustc 1.99.1 (https://github.com/rust-lang/rust at 58cf8f9c3, the sources of Alpine's rust-src-1.99.1-r0):
  `compiler/rustc_symbol_mangling/src/{lib.rs,v0.rs}` (symbol_name_from_attrs, compute_symbol_name, the v0 mangler, punycode
  identifiers, mangle_internal_symbol), `compiler/rustc_span/src/def_id.rs` (StableCrateId::new), `compiler/rustc_middle/src/{mono.rs,
  ty/print/mod.rs,ty/instance.rs,ty/layout.rs}` (instantiation_mode, default_print_def_path, requires_inline, requires_caller_location,
  discr_range_of_repr), `compiler/rustc_mir_transform/src/cross_crate_inline.rs`, `compiler/rustc_metadata/src/rmeta/{mod.rs,
  encoder.rs}` (the rmeta header and CrateRoot's first fields, should_encode_mir), `compiler/rustc_attr_ir/src/weak_lang_items.rs`,
  `compiler/rustc_monomorphize/src/collector.rs` (roots, the uses of calls and drops), `compiler/rustc_abi/src/{layout.rs,
  layout/simple.rs,lib.rs,wrapping_range.rs}` and `compiler/rustc_ty_utils/src/layout.rs` (layout_of for x86_64); and
  rustc-stable-hash 0.1.2 (https://github.com/rust-lang/rustc-stable-hash, the version rustc's Cargo.lock pins: `src/sip128.rs`,
  `src/stable_hasher.rs`, SipHasher128 and StableHasher's writes): copyright the Rust project's contributors, dual-licensed under
  the MIT License or the Apache License 2.0 at the licensee's option, taken here under the MIT License ("Rust (rustc 1.99.1): MIT
  License or Apache License 2.0" below). The judge `mono_check.ae` and `test/rust/front/std-crates.edn` are Arbace's own, under the
  Eclipse Public License 1.0.
- RF11's first slice (2026-11-09, doc/rust/FRONTEND-SURVEY.md, "As built: RF11 (first slice)"), from rustc 1.99.1, the same revision
  and sources and under the same terms (MIT): `mono.ae`'s `layout_of_union` after `compiler/rustc_abi/src/layout.rs` (with
  `BackendRepr::to_union`, `compiler/rustc_abi/src/lib.rs`), `arbace/rust/back/codegen.ae`'s Subslice places after
  `compiler/rustc_codegen_ssa/src/mir/place.rs` (`project_subslice`) and its PointerExposeProvenance and PointerWithExposedProvenance
  casts after `compiler/rustc_codegen_ssa/src/mir/rvalue.rs`; `build_run.ae`'s values of core's integer constants follow
  `library/core/src/num/{int_macros,uint_macros}.rs`' definitions of MIN, MAX and BITS. `eval-promoted` and the rsi switch
  (`bin/rust-front rsi-build`, `bin/lib/rsi/check`'s `--front own`) are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/core_check.ae` and RF12's first slice's edits (2026-11-09, doc/rust/FRONTEND-SURVEY.md, "As built: RF12 (first
  slice)"), from rustc 1.99.1, the same revision and sources and under the same terms (MIT): `compiler/rustc_expand/src/module.rs` (the
  module tree), `compiler/rustc_expand/src/mbe/{metavar_expr,transcribe,macro_rules}.rs` and `compiler/rustc_expand/src/expand.rs`
  (metavariable expressions, macros 2.0's arms, the expansion's order) in `expand.ae`, `compiler/rustc_builtin_macros/src/cfg_select.rs`,
  `compiler/rustc_resolve/src/late.rs` and `compiler/rustc_span/src/hygiene.rs` (a trait's self rib, def-site hygiene) in `resolve.ae`,
  `compiler/rustc_mir_build/src/builder/mod.rs` (`#[rustc_inherit_overflow_checks]`) in `build.ae`. The driver's judge, core's index and
  its native interface, `bin/lib/repr-rust/core-build.bash` and `test/rust/front/core-modules.txt` are Arbace's own, under the Eclipse
  Public License 1.0.
- RF16 (2026-11-10, doc/rust/FRONTEND-SURVEY.md, "As built: RF16 (RFP58 (a))"), from rustc 1.99.1, the same revision and sources and
  under the same terms (MIT): `mono.ae`'s SIMD layouts after `compiler/rustc_ty_utils/src/layout.rs` (layout_of_uncached's repr(simd))
  and `compiler/rustc_abi/src/layout.rs` (`LayoutCalculator::simd_type`, univariant's newtype ABI), `arbace/rust/back/codegen.ae`'s
  generic SIMD intrinsics after `compiler/rustc_codegen_llvm/src/intrinsic.rs` (`generic_simd_intrinsic`), its vectors' indirect
  passing after `compiler/rustc_target/src/callconv/mod.rs` (`adjust_for_rust_abi`) and its thread-local references after
  `compiler/rustc_codegen_ssa/src/mir/rvalue.rs` (ThreadLocalRef); `mono.ae`'s reading of stdarch's `types!` follows
  `library/stdarch/crates/core_arch/src/macros.rs`. The source reader of private ADTs and `build_run.ae`'s promoted constants'
  additions are Arbace's own, under the Eclipse Public License 1.0.
- The proposals RFP28-RFP41 (2026-11-09, doc/rust/FRONTEND-SURVEY.md, "As built: the rest of RFP28-RFP41"), from rustc 1.99.1, the same
  revision and sources and under the same terms (MIT): `build.ae`'s byte-array static constants after `compiler/rustc_middle/src/ty/
  print/pretty.rs` (`pretty_print_const_scalar_ptr`) and a closure's fake reads after `compiler/rustc_mir_build/src/builder/expr/
  as_rvalue.rs`, its caller spans after `compiler/rustc_middle/src/mir/consts.rs` (`span_as_caller_location`) and
  `compiler/rustc_span/src/hygiene.rs` (`ExpnId::expansion_cause`); `passes.ae`'s opaque types revealed by their definition after
  `compiler/rustc_mir_transform/src/post_analysis_normalize.rs`, with `collect.ae`'s opaque types as rustc represents them (an alias of
  the OpaqueTy's DefId and its parent's identity args); `expand.ae`'s metavariable spans after `compiler/rustc_expand/src/mbe/transcribe.rs` (`mk_delimited`) and
  `compiler/rustc_span/src/lib.rs` (`Span::to`, `prepare_to_combine`); the helper's `tools/rsdump/front.rs` additions (field-reached
  ADTs, function attributes) written against `rustc_middle`'s queries. The judges' changes (`build_check.ae`'s class, `build_run.ae`'s
  batch, `ci_batch.ae`) and `test/rust/front/opaque1.rs` are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/back/shim.ae` (RIND2 (a), 2026-11-17, doc/rust/FRONTEND-SURVEY.md, "As built: RIND (stage 1)"), Arbace forms written
  after rustc 1.99.1, the same revision and under the same terms (MIT): the allocator shim of `compiler/rustc_codegen_llvm/src/
  allocator.rs` with `compiler/rustc_ast/src/expand/allocator.rs`' methods, and the allocator module's CGU name after
  `compiler/rustc_middle/src/mir/mono.rs` and `compiler/rustc_data_structures/src/base_n.rs`, held to rustc's recorded shims (the
  IR packs of `test/rust/ir/`); the check (`check`, `emit-list`) and `bin/rsi shim-check` are Arbace's own, under the Eclipse Public
  License 1.0.
- `arbace/rust/front/lib_unit.ae` (C6's first slice, 2026-11-20, doc/rust/FRONTEND-SURVEY.md, "As built: C6's first slice"): its v0
  decoder written after rustc 1.99.1, the same revision and under the same terms (MIT): the grammar `compiler/rustc_symbol_mangling/
  src/v0.rs` prints (paths, impl paths, types, constants, back-references, identifiers, disambiguators). The census, the keys and the
  judge, build-run's `lib-unit-run` and `bin/rust-front lib-unit` are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/std_names.ae` (C4-1 (a), 2026-11-20, doc/rust/FRONTEND-SURVEY.md, "As built: C4-1 (a)"): its scan and
  import resolution written after rustc 1.99.1, the same revision and under the same terms (MIT): `compiler/rustc_resolve/src/
  {build_reduced_graph,imports}.rs` (the definitions' namespaces and visibilities, a constructor's narrowed visibility, macro_rules!
  bindings, single and glob imports, an import binding's visibility) and `tools/rsdump/front.rs`' traversal (Arbace's own); the
  builtin macros' kinds after `compiler/rustc_builtin_macros/src/lib.rs`. The assembly, the compare and `bin/rust-front
  std-names-native` are Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/back/inline.ae` (C6-4, 2026-11-20, doc/rust/FRONTEND-SURVEY.md, "C6-4's next slice: LLVM's AlwaysInliner over our
  forms"): Arbace forms porting LLVM 23's always-inliner, the llvm-project of the pinned rustc-1.99.1-src.tar.xz
  (`src/llvm-project`): `llvm/lib/Transforms/IPO/AlwaysInliner.cpp` (AlwaysInlineImpl), `llvm/lib/Transforms/Utils/
  InlineFunction.cpp` (InlineFunctionImpl) and `llvm/lib/Transforms/Utils/CloneFunction.cpp` (CloneAndPruneIntoFromInst,
  PruningFunctionCloner::CloneBlock), with InstSimplify's phi rule: copyright the LLVM Project contributors, under the Apache
  License 2.0 with the LLVM Exceptions (section "LLVM (clang)" below). Its pure cases (`test/rust/front/inline_test.ae`) are
  Arbace's own, under the Eclipse Public License 1.0.
- `arbace/rust/front/cargo_meta.ae` (C6-2 (b), 2026-11-20, doc/rust/FRONTEND-SURVEY.md, "As built: C6-2"): Arbace forms porting
  Cargo 0.100.0 (cargo 1.99.1, https://github.com/rust-lang/cargo at 5f94df478, the pinned rustc-1.99.1-src.tar.xz's
  `src/tools/cargo`): `src/compiler/build_runner/compilation_files.rs` (compute_metadata, hash_rustc_version),
  `src/workspace/{package_id,source_id,profiles,manifest}.rs`, `src/compiler/{build_config,lto,compile_kind,crate_type}.rs`,
  `crates/cargo-util-schemas` (SourceKind, TomlDebugInfo, TomlTrimPaths); semver 1.0.28's `Version` hash and rustc-stable-hash
  0.1.2's `StableSipHasher128` writes (the same tarball's `vendor/`): copyright the Cargo, semver and Rust project contributors,
  dual-licensed under the MIT License or the Apache License 2.0 at the licensee's option, taken here under the MIT License ("Rust
  (rustc 1.99.1): MIT License or Apache License 2.0" below). Its JSON reader, `test/rust/front/std-build-units.edn` (the library
  build's units as data), `bin/rsi std-build --units` and `bin/rust-front std-crates --native|--units` are Arbace's own, under the
  Eclipse Public License 1.0.
- The own-verdict judge of `arbace/rust/check.ae` (`own-verdict`, `run-class`, `verdict-met?`, `directives`; rust-90,
  doc/rust/EVAL.md §13): Arbace forms written after rustc 1.99.1's compiletest, the same revision and sources (the pinned
  `rustc-1.99.1-src.tar.xz`'s `src/tools/compiletest/src/runtest/ui.rs`: `run_ui_test`'s RunResult of the run and the
  run-pass, run-fail and run-crash checks; `directives.rs`: the `//@` and `//@[REV]` directive lines and the output
  directives): copyright the Rust project's contributors, dual-licensed under the MIT License or the Apache License 2.0
  at the licensee's option, taken here under the MIT License ("Rust (rustc 1.99.1): MIT License or Apache License 2.0"
  below). The rest of `check.ae`, `native.ae`'s tree texts (after Arbace's own helper `tools/rsdump`'s transcoder) and
  `test/rust/roundtrip-controls.edn` are Arbace's own, under the Eclipse Public License 1.0.
- The non-javac branches of `skip-parens`, `tree-name`, `full-name`, `diamond?` and `class-kind` in
  `arbace/repr/java/tree.ae` (repr/java's interface to the tree, doc/classes/JAVA-FRONTEND-SURVEY.md,
  "As built: JFS66 (b)"): Arbace forms reimplementing, for the native parser's trees,
  https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/tree/TreeInfo.java`'s `skipParens`, `name`,
  `fullName` and `isDiamond`, and `tree/JCTree.java`'s `JCClassDecl.getKind` (its test of the
  modifiers' flags): Copyright (c) Oracle and/or its affiliates, under the GNU General Public
  License version 2 with the Classpath Exception (text below; the file carries their notice).
- `arbace/repr/java/decls.ae` and `arbace/repr/java/scope.ae` (repr/java's native attribution, its
  declarations and scope tiers, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN9a"): Arbace forms
  reimplementing, for the native parser's trees, https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s `comp/Check.java` (`checkFlags`,
  `implicitEnumFinalFlag`), `comp/Resolve.java` (`findType`, `findTypeVar`, `findImmediateMemberType`,
  `findInheritedMemberType`, `findMemberType`, `findGlobalType`, `findVar`, `findField`,
  `findIdentInPackage`, `isAccessible`), `comp/TypeEnter.java` (`attribSuperTypes`, `baseEnv`,
  `getCanonicalConstructorDecl`), `comp/MemberEnter.java` (`visitMethodDef`, `visitVarDef`), `comp/Enter.java`
  (`visitClassDef`'s flags and enclosing type), `comp/ConstFold.java`, `comp/Annotate.java`
  (`attributeAnnotationValue` and its helpers), `code/Types.java` (`asSuper`, `asOuterSuper`,
  `asEnclosingSuper`, the substitution of type arguments), `code/Symbol.java` (`markAbstractIfNeeded`),
  `code/Flags.java` (the flags and masks), `comp/Attr.java` (the environments of its visit methods, the
  match bindings' scopes, `findJumpTarget`, `checkIdInternal`'s type of a class name) with
  `comp/MatchBindingsComputer.java`, and `comp/Flow.java`'s `AliveAnalyzer` as far as the bindings'
  scopes need it: Copyright (c) Oracle and/or its affiliates, under the GNU General Public License
  version 2 with the Classpath Exception (text below; each file carries the notice).
- `arbace/repr/java/expr.ae` and `arbace/repr/java/types.ae` (repr/java's native attribution, its
  members and types tiers, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN9b"), and the generated
  members and members' clones of `arbace/repr/java/decls.ae` and the methods' `isMemberOf` of
  `arbace/repr/java/nattr.ae`: Arbace forms reimplementing, for the native parser's trees,
  https://github.com/openjdk/jdk26u at `baf63fb`, `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s
  `comp/Attr.java` (`visitIdent`, `visitSelect`, `selectSym`, `checkIdInternal`, `visitApply`,
  `adjustMethodReturnType`, `checkMethod`'s unchecked erasure, `visitIndexed`, `visitParens`, `visitAssign`,
  `visitAssignop`, `visitUnary`, `visitBinary`, `visitTypeCast`, `visitTypeTest`, `visitConditional`,
  `isBooleanOrNumeric`, `condType`, `visitSwitchExpression`, `enumConstant`, `visitNewClass`'s and
  `visitNewArray`'s types, `visitLiteral`), `comp/Operators.java` (the operator helpers, `unaryPromotion`,
  `binaryPromotion`, `stringPromotion`), `code/Types.java` (`unboxedType`, `boxedClass`, `isSameType`,
  `isSubtype`, `containsType`, `isAssignable`'s constants, `memberType`, `capture`, `glb`, `lub`,
  `skipTypeVars`), `comp/TypeEnter.java` (`addEnumMembers`, `addRecordMembersIfNeeded`,
  `BasicConstructorHelper`, `RecordConstructorHelper`), `comp/Resolve.java` (`findVar`'s and
  `findFun`'s clones of imported members), `code/Symbol.java` (`MethodSymbol.isInheritedIn`,
  `hiddenInInternal`, `isMemberOf`) and `code/Symtab.java` (`lengthVar`, `arrayCloneMethod`): Copyright (c)
  Oracle and/or its affiliates, under the GNU General Public License version 2 with the Classpath
  Exception (text below; each file carries the notice).
- `arbace/repr/java/overload.ae` (repr/java's native attribution, its overload tier,
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN9c"), with the overload tier's choice in
  `arbace/repr/java/expr.ae` and ClassReader's method flags and inner constructors in
  `arbace/repr/java/decls.ae`: Arbace forms reimplementing, for the native parser's trees,
  https://github.com/openjdk/jdk26u at `baf63fb`, `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s
  `comp/Resolve.java` (`isAccessible` of classes and members, `isProtectedAccessible`, `isInnerSubClass`,
  `notOverriddenIn`, `rawInstantiate` without inference, `resolveMethodCheck`'s `argumentsAcceptable` and
  `checkArg`, `MethodCheckContext.compatible`, `MethodResultInfo.check`, `selectBest`, `mostSpecific`,
  `signatureMoreSpecific`, `adjustArgs`, `MostSpecificCheck`, `ambiguityError`, `mergeAbstracts`,
  `findMethodInScope`, `findMethod` with `InterfaceLookupPhase` and `superclasses`, `findFun`,
  `lookupMethod`, `shouldStop`, `mergeResults`, `resolveMethod`, `resolveQualifiedMethod`,
  `resolveConstructor`, `findConstructor`), `comp/Attr.java` (`visitApply`'s and `visitNewClass`'s
  resolution sites), `comp/ArgumentAttr.java` (which arguments are standalone), `code/Types.java`
  (`isSubtypeUnchecked`, `isConvertible`, `cvarUpperBound`, `cvarLowerBound`, `hasSameArgs`,
  `isSubSignature`, `overrideEquivalent`, `returnTypeSubstitutable`, `resultSubtype`,
  `covariantReturnType`, `mergeAbstracts`, `implementation`, `isDerivedRaw`, `rank`, `closure`, `insert`,
  `union`), `code/Symbol.java` (`MethodSymbol.overrides`, `isOverridableIn`, `TypeSymbol.precedes`),
  `comp/Check.java` (`intersect`, `importAccessible`), `code/Scope.java` (the order of a static import's
  members), `comp/TypeEnter.java` (`AnonClassConstructorHelper`, `staticImportFilter`) and
  `jvm/ClassReader.java` (`readMethod`'s default methods and inner constructors): Copyright (c) Oracle
  and/or its affiliates, under the GNU General Public License version 2 with the Classpath Exception
  (text below; each file carries the notice).
- `arbace/repr/java/infer.ae` (repr/java's native attribution, its inference tier,
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN9d"), with the inference tier's poly expressions,
  lambdas, method references, record patterns and local class names in `arbace/repr/java/expr.ae`, its
  diamonds, method references and functional descriptors in `arbace/repr/java/overload.ae`, and
  ClassReader's synthetic members and deprecation flags in `arbace/repr/java/decls.ae`: Arbace forms
  reimplementing, for the native parser's trees, https://github.com/openjdk/jdk26u at `baf63fb`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s `comp/Infer.java` (`instantiateMethod`,
  `PartiallyInferredMethodType.check`, `generateReturnConstraints`, `needsEagerInstantiation`,
  `generateReferenceToTargetConstraint`, `instantiateAsUninferredVars`, `instantiateFunctionalInterface`,
  `instantiatePatternType`, `instantiatePolymorphicSignatureInstance`, the incorporation actions and
  engine, the inference steps, the graph solver), `comp/InferenceContext.java`, `comp/DeferredAttr.java`
  (`DeferredType.check`, the stuck policies, `DeferredAttrContext.complete`, `pickDeferredNode`,
  `StructuralStuckChecker`), `comp/ArgumentAttr.java` (the argument types: `ResolvedMethodType`,
  `ResolvedConstructorType`, `ConditionalType`, `SwitchExpressionType`, `ExplicitLambdaType`, the
  reference's overload kind), `comp/Attr.java` (`checkMethod`'s instantiation and unchecked erasure,
  `visitLambda`, `getTargetInfo`, `lambdaBodyResult`, `checkLambdaCompatible`, `visitReference`,
  `checkReferenceCompatible`, `visitNewClass`'s diamonds and qualified creations,
  `visitAnonymousClassDefinition`, `visitRecordPattern`, `visitBindingPattern`, `visitTypeUnion`,
  `visitForeachLoop`'s element, `condType`, `checkExConstraints`, `handleSwitch`'s pattern switch and
  unconditional pattern), `comp/Flow.java` (`AliveAnalyzer`'s case liveness, `isExhaustive`),
  `comp/Resolve.java` (`resolveDiamond`, `findDiamond`, `resolveMemberReference`, `getMemberReference`,
  the reference lookup helpers and choosers, `ReferenceLookupResult`, `findPolymorphicSignatureInstance`,
  `MethodReferenceCheck`, `arityMethodCheck`, `nilMethodCheck`), `comp/Check.java` (`checkLocalVarType`,
  `localClassName`, `checkType`, `isChecked`), `comp/TypeEnter.java` (`BasicConstructorHelper`,
  `getDefaultConstructorHelper`), `code/Type.java` (`UndetVar`), `code/Types.java` (`isSubtype`,
  `isSuperType`, `isSameType`, `containsType`, `containedBy`, `asSuper`, `supertype`, `interfaces`,
  `closure`, `intersect`, `merge`, `closureMin`, `lub`, `glb`, `upward` with `TypeProjection`, `captures`,
  `findDescriptorType`, `findDescriptorSymbol`, `removeWildcards`, `interfaceCandidates`, `prune`,
  `overridesObjectMethod`, `membersClosure`, `newInstances`), `code/Symbol.java` (`TypeSymbol.precedes`),
  `jvm/ClassReader.java` (`enterMember`, the @Deprecated flags), `tree/TreeInfo.java`
  (`isExpressionStatement`) and `util/GraphUtils.java` (`tarjan`): Copyright (c) Oracle and/or its
  affiliates, under the GNU General Public License version 2 with the Classpath Exception (text below;
  each file carries the notice).
- The native attribution's read without javac and its last declines (doc/classes/JAVA-FRONTEND-SURVEY.md,
  "As built: JN9e"), in `arbace/repr/java/expr.ae` (a lambda's thrown types through a try statement's
  resources, catch clauses and finally block, a call returned by a lambda its outer call checks, a
  type variable named as a qualifier, a multi-catch parameter's intersection lub), `overload.ae` (an
  intersection site's non-public members), `decls.ae` (a package's top-level class names, a record
  accessor's deprecation), `attr.ae` (a record component's annotations taken off its field) and
  `model.ae` (a `var` local's constant): Arbace forms reimplementing, for the native parser's trees,
  https://github.com/openjdk/jdk26u at `baf63fb`, `src/jdk.compiler/share/classes/com/sun/tools/javac/`'s
  `comp/Flow.java` (`FlowAnalyzer.visitTry`, `visitThrow`'s precise rethrow), `comp/Check.java`
  (`validateAnnotation`'s record members, `getApplicableTargets`, `subset`, `incl`, `intersect`),
  `code/ClassFinder.java` (`fillIn`, `includeClassFile`), `code/Types.java` (`makeIntersectionType`),
  `code/Symbol.java` (`isInheritedIn`, `packge`), `comp/Attr.java` (`visitIdent`, `visitTypeUnion`,
  `visitVarDef`'s lazy constant, `attribLazyConstantValue`), `comp/TypeEnter.java` (the record
  members' original annotations) and `comp/MemberEnter.java` (`handleDeprecatedAnnotations`):
  Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with the
  Classpath Exception (text below; each file carries the notice).
- `arbace/repr/haskell/lex.ae`, `header.ae`, `post.ae`, `post_decl.ae`, `rules_module.ae`,
  `rules_decl.ae`, `rules_expr.ae`, `haddock.ae` and `haddocklex.ae` (repr/haskell's native reader,
  doc/haskell/NATIVE.md): ports of GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz
  (SHA-256 `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin):
  `compiler/GHC/Parser/Lexer.x` (with `Lexer/String.x`, `Lexer/Interface.hs`, `String.hs`,
  `CharClass.hs`), `GHC/Parser/Header.hs`, the language extensions of `GHC/Driver/{DynFlags,Flags,Session}.hs`,
  `GHC/Parser.y`'s actions, `GHC/Parser/PostProcess.hs`, `GHC/Parser/PostProcess/Haddock.hs`,
  `GHC/Parser/HaddockLex.x` and the builders of `GHC/Hs/{Utils,Type}.hs` they use (the tarball names
  the Alex and Happy sources `*.x.source` and `Parser.y.source`), by hand on 2026-11-04, each file
  carrying GHC's notice: Copyright 2002, The University Court of the University
  of Glasgow, under the Glasgow Haskell Compiler License, a BSD 3-Clause License ("GHC: the Glasgow
  Haskell Compiler License" below). `arbace/repr/haskell/tables.ae` is generated from that tarball's
  `compiler/GHC/Parser.y.source` (its grammar, the same Glasgow license) by `happy.ae`, Arbace's own port of
  Happy's LALR(1) construction (no Happy code: Happy is a tool, its tables in GHC's generated
  `Parser.hs` the check); `happy.ae`, `ast.ae`, `parse.ae`, `native.ae` and `mutate.ae` are Arbace's own,
  under the Eclipse Public License 1.0. `arbace/repr/haskell/doc.ae`, `errors.ae` and `pprhs.ae` (the
  native reader's messages, doc/haskell/NATIVE.md, "Messages") port from the same tarball
  `compiler/GHC/Utils/Ppr.hs` (GHC's copy of the pretty library's HughesPJ printer) and the combinators of
  `GHC/Utils/Outputable.hs`, `GHC/Parser/Errors/Ppr.hs`, `GHC/Types/Hint/Ppr.hs`, the parser's codes of
  `GHC/Types/Error/Codes.hs`, the printing of `GHC/Types/Error.hs`, `GHC/Utils/Error.hs`,
  `GHC/Driver/Errors.hs`, `GHC/Utils/Logger.hs` and the Outputable instances of
  `GHC/Hs/{Expr,Pat,Type,Lit,Binds,Decls}.hs` and `GHC/Parser/Types.hs`, by hand on 2026-11-04, each file
  carrying GHC's notice (the Glasgow license, below). `arbace/repr/haskell/fileargs.ae` (the driver's header
  phase, 2026-11-05) ports `GHC/Parser/Header.hs` (getOptions', toArgs, checkExtension,
  checkProcessArgsResult), `GHC/Driver/CmdLine.hs` (processArgs), `GHC/Driver/Session.hs` and
  `GHC/Driver/Pipeline/Execute.hs` (getFileArgs) with GHC's notice; `arbace/repr/haskell/flags.ae` is generated
  by the helper's `flags` verb from the names of `flagsDynamic` and `supportedLanguagePragmas` of the same
  tarball's GHC (the Glasgow license).
- `arbace/repr/haskell/famenv.ae` (the elaborator's type families, HF9's first slice, doc/haskell/FRONTEND-SURVEY.md, "As built:
  HF9 (first slice)"): a port of GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin): `reduceTyFamApp_maybe`,
  `lookupFamInstEnv`'s matching, `chooseBranch`, `findBranch`, `apartnessCheck`, `compatibleBranches` and `computeAxiomIncomps` of
  `compiler/GHC/Core/FamInstEnv.hs` with `tcMatchTys` and `tcUnifyTysFG` of `compiler/GHC/Core/Unify.hs`, by hand on 2026-11-08,
  the file carrying GHC's notice (the Glasgow Haskell Compiler License, "GHC: the Glasgow Haskell Compiler License" below).
  `arbace/repr/haskell/libcheck.ae` and `bin/lib/repr-haskell/lib-check` are Arbace's own; `typecheck.ae`'s HF9 additions (kinds
  erased, GADTs' givens, ScopedTypeVariables, the implicit parameters, the type applications, the data family instances; the
  second slice's checking mode, implications, pattern synonyms, DataToTag, WithDict, the implicit parameters' bindings and
  defaulting) are Arbace's own, GHC's `compiler/GHC/Tc/` read as their specification; `deriv.ae`'s second-slice tables
  (primOrdOps' ordOpTbl, boxConTbl, postfixModTbl of `compiler/GHC/Tc/Deriv/Generate.hs`) are under the file's GHC notice.
- `arbace/repr/haskell/ownface.ae` (the front end's own interfaces, HF10-P3, doc/haskell/FRONTEND-SURVEY.md, "As built: HF10-P3
  (first slice)"): written after GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin): what `mkIface_` of
  `compiler/GHC/Iface/Make.hs` writes and `addExternal`/`trimId` of `compiler/GHC/Iface/Tidy.hs` expose at -O0 (with
  `compiler/GHC/Driver/Config/Tidy.hs`'s ExposeNone and `compiler/GHC/IfaceToCore.hs`'s tcIdInfo), `dataConSrcToImplBang` of
  `compiler/GHC/Types/Id/Make.hs`, the wired-in definitions of `compiler/GHC/Builtin/Types.hs` (multiplicityTyCon's constructors,
  heqClass's, eqClass's and coercibleClass's superclasses, the kinds' invisible binders of unboxedSoloTyCon, coercibleTyCon,
  eqTyCon, heqTyCon, anyTyCon) and the built-in families of `compiler/GHC/Builtin/Types/Literals.hs`, by hand on 2026-11-09, the
  file carrying GHC's notice (the Glasgow Haskell Compiler License, "GHC: the Glasgow Haskell Compiler License" below); the
  judge (`compare-ifaces`) and `libcheck.ae`'s HF10-P3 verbs (`own-order`, `own`, `rank-own`) are Arbace's own; HF10-P12's
  `helper-core` too. HF10-P12 and P14 (2026-11-09): `simplify.ae`'s stable-unfolding mode (updModeForStableUnfoldings of
  `compiler/GHC/Core/Opt/Simplify/Utils.hs`) and `rename.ae`'s `integral-fl` (generalizeOverLitVal of
  `compiler/GHC/Rename/Pat.hs`) from the same tarball, under the notices those files carry; `libcheck.ae`'s `dm-unfoldings`,
  `grow-closure` and `cabal-reexports` are Arbace's own.
- `arbace/repr/haskell/primiface.ae` (GHC.Internal.Prim's interface from `primops.txt.pp`, HF10-P18 (a), doc/haskell/FRONTEND-SURVEY.md,
  "As built: HF10-P18"): ports of GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin): genprimopcode's lexer and parser
  (`utils/genprimopcode/Lexer.x`, `Parser.y`), `AccessOps.hs`, `Main.hs`'s desugarVectorSpec, tvsIn and ppTyVarBinders,
  `compiler/GHC/Builtin/Types/Prim.hs`'s template variables and exposedPrimTyCons, `compiler/GHC/Builtin/Utils.hs`'s
  ghcPrimExports and ghcPrimFixities and `compiler/GHC/Types/Id/Make.hs`'s ghcPrimIds, by hand on 2026-11-10, under the notices
  those files carry (the Glasgow Haskell Compiler License, "GHC: the Glasgow Haskell Compiler License" below). `typecheck.ae`'s
  HF10-P20 Typeable bindings of the wired-in things follow `compiler/GHC/Tc/Instance/Typeable.hs` (mkPrimTypeableTodos,
  builtInKindReps, mkKindRepRhs, todoForTyCons) of the same tarball, under its notice.
- `arbace/repr/haskell/rename.ae` (repr/haskell's renamer, HF2, doc/haskell/FRONTEND-SURVEY.md, "As built: HF1-HF2"):
  ports of GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin): `mkOpAppRn`,
  `mkNegAppRn`, `mkConOpPatRn` and `mkHsOpTyRn` of `compiler/GHC/Rename/HsType.hs` and `compareFixity` of
  `compiler/GHC/Types/Fixity.hs`, by hand on 2026-11-05, the file carrying GHC's notice (Copyright 2002, The
  University Court of the University of Glasgow, the Glasgow Haskell Compiler License, a BSD 3-Clause License,
  "GHC: the Glasgow Haskell Compiler License" below); the rest of `compiler/GHC/Rename/` read as the
  specification of the scoping and of the dependency analyses. The helper's `rn` and `iface` verbs,
  `tools/hsdump/HsRn.hs`, are Arbace's own, written against GHC's API.
- `arbace/repr/haskell/desugar.ae` and `arbace/repr/haskell/tostg.ae` (the front end's match compiler, HF5, and its
  CorePrep, CoreToStg and Unarise, HF6; doc/haskell/FRONTEND-SURVEY.md, "As built: HF5-HF6"): written on 2026-11-05
  after GHC 9.14.1's algorithms (the same tarball and pin: `compiler/GHC/HsToCore/Match.hs`, `Match/Constructor.hs`,
  `Match/Literal.hs`, `GuardedRHSs.hs`, `ListComp.hs`, `compiler/GHC/Core/Utils.hs`'s `refineDefaultAlt`,
  `compiler/GHC/CoreToStg/Prep.hs`, `CoreToStg.hs`, `compiler/GHC/Stg/Unarise.hs`, `Stg/FVs.hs`,
  `compiler/GHC/Types/Id/Make.hs`'s dictionary selectors), the files carrying GHC's notice (the Glasgow Haskell
  Compiler License, "GHC: the Glasgow Haskell Compiler License" below); `stgcmp.ae`, the STG judge, is Arbace's own.
  Since HF10's first slice (2026-11-08) they also follow `compiler/GHC/CoreToStg/Prep.hs`'s speculative evaluation and
  cpeArgArity, `compiler/GHC/Core/Utils.hs`'s exprOkForSpecEval, `compiler/GHC/CoreToStg.hs`'s mkStgAltType,
  `compiler/GHC/HsToCore/Match.hs`'s tidy_bang_pat and matchBangs and `compiler/GHC/Core/Make.hs`'s bindNonRec, under the same notice.
  Since HF10-P5 and HF10-P8 (2026-11-09) `typecheck.ae` also follows `compiler/GHC/HsToCore/Foreign/Decl.hs`, `Foreign/C.hs`,
  `Foreign/Prim.hs` and `Foreign/Call.hs` (dsFImport, dsCImport, dsFCall, dsPrimCall, dsFLabel, unboxArg, boxResult, resultWrapper,
  mk_alt) and `tostg.ae` `compiler/GHC/Types/RepType.hs`'s unboxed sums' slots (SlotTy, primRepSlot, slotPrimRep, fitsIn,
  ubxSumRepType, layoutUbxSum) with `compiler/GHC/Stg/Unarise.hs`'s mkUbxSum, ubxSumRubbishArg, unariseSumAlts and
  mapSumIdBinders, and `simplify.ae` (HF10-P10) `Core/Opt/Simplify/Env.hs`'s getSubst laziness, under the same notice.
- `arbace/repr/haskell/simpleopt.ae` and `arbace/repr/haskell/constfold.ae` (the front end's simple optimiser and the
  floating-point primops' built-in rules, HF5-HF6's decision 2 (a); doc/haskell/FRONTEND-SURVEY.md, "As built: HF7 (first
  light)"): ports of GHC 9.14.1 (the same tarball and pin) by hand on 2026-11-06: `compiler/GHC/Core/SimpleOpt.hs`
  (`simpleOptPgm`, `simple_opt_expr`, `simple_app`, `simple_opt_bind`, `simple_bind_pair`, `simple_out_bind_pair`,
  `safe_to_inline`, `exprIsConApp_maybe`'s constructor case), the occurrence information of
  `compiler/GHC/Core/Opt/OccurAnal.hs`, the compulsory unfoldings of `compiler/GHC/Types/Id/Make.hs`, and
  `compiler/GHC/Core/Opt/ConstantFold.hs`'s `Float#`/`Double#` rules with `compiler/GHC/Types/Literal.hs`'s
  conversions, the files carrying GHC's notice (the Glasgow Haskell Compiler License, "GHC: the Glasgow Haskell
  Compiler License" below). `front.ae` (the driver) and `test/haskell/run_record.ae` are Arbace's own.
- `arbace/repr/haskell/simplify.ae`, `arbace/repr/haskell/occanal.ae`, `arbace/repr/haskell/core.ae` and
  `arbace/repr/haskell/primops.ae` (the front end's -O0 Simplifier, the user's decision of 2026-11-06;
  doc/haskell/FRONTEND-SURVEY.md, "As built: the -O0 Simplifier"): ports of GHC 9.14.1 (the same tarball and pin) by
  hand on 2026-11-06: `compiler/GHC/Core/Opt/Simplify.hs`, `Simplify/Iteration.hs`, `Simplify/Utils.hs`,
  `Simplify/Env.hs`, `Simplify/Inline.hs`, `compiler/GHC/Core/Opt/OccurAnal.hs`, `compiler/GHC/Core/Opt/Arity.hs`,
  `compiler/GHC/Core/Unfold.hs`, `Unfold/Make.hs`, `compiler/GHC/Core/Utils.hs`, `compiler/GHC/Core/SimpleOpt.hs`'s
  `exprIsConApp_maybe` and `compiler/GHC/Types/Id/Make.hs`'s class-op rule; `primops.ae` is generated by
  `tools/hsprimops/primops.awk` (Arbace's own) from `compiler/GHC/Builtin/primops.txt.pp`; the files carrying GHC's
  notice (the Glasgow Haskell Compiler License, "GHC: the Glasgow Haskell Compiler License" below). `simplcmp.ae`, the
  simplified Core's judge, is Arbace's own.
- `arbace/repr/haskell/deriv.ae` (repr/haskell's deriving, HF4, doc/haskell/FRONTEND-SURVEY.md, "As built: HF4", HFS5 (a)):
  ports of GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin): the generators of
  `compiler/GHC/Tc/Deriv/Generate.hs` (`gen_Eq_binds`, `gen_Ord_binds`, `gen_Enum_binds`, `gen_Bounded_binds`,
  `gen_Ix_binds`, `gen_Read_binds`, `gen_Show_binds`) and of `compiler/GHC/Tc/Deriv/Functor.hs` (`gen_Functor_binds`,
  `gen_Foldable_binds`, `gen_Traversable_binds`, `functorLikeTraverse`), by hand on 2026-11-05, the file carrying GHC's
  notice (Copyright 2002, The University Court of the University of Glasgow, the Glasgow Haskell Compiler License, a
  BSD 3-Clause License, "GHC: the Glasgow Haskell Compiler License" below); the context inference and the choice of
  strategy of `compiler/GHC/Tc/Deriv/{Infer,Utils}.hs` and `Deriv.hs` read as the specification, written anew in
  `typecheck.ae`.
- `arbace/repr/haskell/target.ae`, `configure.ae` and `hsc2hs.ae` (the native reads' inputs derived, HFS-I2 (a),
  doc/haskell/NATIVE.md, "The native reads' inputs: derived"): ports of GHC 9.14.1,
  https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c9af86554a3289f2787a38d6aa83d00d136aa9f920361dd693c101e77`, the corpus's pin), by hand on 2026-11-08: hadrian's
  `src/Rules/Generate.hs` (generateSettings' order, templateRules) and `bindist/Makefile` (lib/settings, patchpackageconf),
  `rts/configure.ac`'s ghcplatform.h and ghcautoconf.h steps, `rts/gen_event_types.py`'s two outputs, the packages'
  `configure.ac`s' defines and `aclocal.m4`'s FPTOOLS_CHECK_HTYPE and FP_CHECK_CONST, and `utils/hsc2hs/src/{HSCParser,C,
  DirectCodegen,UtilsCodegen}.hs` (hsc2hs 0.68.10's direct mode), under the Glasgow Haskell Compiler License (below);
  autoconf's config.status header rewrite written from its documented behaviour. `arbace/repr/haskell/cabal.ae`: a port of
  the same tarball's `libraries/Cabal/Cabal-syntax` 3.16.0.0 (`Distribution/Fields/{Parser,Pretty}.hs`'s layout,
  `Distribution/FieldGrammar/{Parsec,Pretty}.hs`'s free text, `Distribution/Types/InstalledPackageInfo/FieldGrammar.hs`'s
  ipiFieldGrammar, `Distribution/Types/Condition.hs`), under Cabal's BSD 3-Clause License ("Cabal: BSD 3-Clause License"
  below).
- `arbace/repr/haskell/target.ae`'s `libffi-vars` and `libffi-headers` (the system libffi's two headers GHCi/FFI.hsc's hsc2hs run
  includes, HFS-I11 (a), 2026-11-08): the substitution rules of libffi 3.8.0's `configure.ac` (AC_INIT's version, FFI_VERSION_NUMBER,
  HAVE_LONG_DOUBLE, FFI_EXEC_TRAMPOLINE_TABLE, the ffitarget.h copy) and `configure.host` (x86_64's TARGET and TARGETDIR), ported by
  hand from https://github.com/libffi/libffi/releases/download/v3.8.0/libffi-3.8.0.tar.gz (SHA-256
  `7da3e2d9a171eb0a038f592ecad3ff2bb2550f3496d87b3b29ad0cf4430c0db4`, `test/haskell/target/libffi.edn`'s pin); the headers they make,
  `include/ffi.h.in` substituted and `src/x86/ffitarget.h`, are libffi's text, made in `.tmp/` and not committed: libffi, Copyright (c)
  1996-2026 Anthony Green, Red Hat, Inc and others, under the MIT License ("libffi: MIT License" below).
- `arbace/repr/haskell/unlit.ae` (repr/haskell's native unlit, doc/haskell/NATIVE.md): a port of the
  same tarball's `utils/unlit/unlit.c`, under the Glasgow Haskell Compiler License (below), on
  2026-11-04; `arbace/repr/haskell/driver.ae` (GHC's driver's preprocessing: `GHC/SysTools/Cpp.hs`'s
  doCpp, the package database's units as `GHC/Unit/State.hs` closes them) is Arbace's own, written
  from that tarball's sources, under the Eclipse Public License 1.0.
- `arbace/repr/haskell/cpp.ae` (repr/haskell's native C preprocessor in GCC's traditional mode,
  doc/haskell/NATIVE.md) is Arbace's own, under the Eclipse Public License 1.0: a clean-room
  implementation (2026-11-04) written from the documented behaviour of traditional mode and judged
  against Alpine's gcc 15.2.0 `cpp -traditional` run as a black box, no code of GCC's (it replaced a
  port of GCC's libcpp, under the GPL version 3, made on the branch `haskell-pp` and never merged).
- `tools/ppref/` (the token checks' reference, doc/cpp/CPP-SURVEY.md, "As built: pp-ref"; the user's CP413 (a) of
  2026-11-20): `ppref.cc`, the harness, and `shim.c` are Arbace's own, under the Eclipse Public License 1.0, no line of GCC's
  in them. `bin/repr-cpp ppref build` links the harness, in `.tmp` only, with GCC 15.2.0's libcpp (GPL-3.0-or-later) and
  libiberty's files (LGPL-2.0-or-later) from the pinned gcc-15.2.0 tarball, unmodified, built by Arbace's own C++ and C (or,
  `--native`, by the host's g++, the oracle tier's); the result, the lowered C and Go made from those files and the executable,
  stays in `.tmp` and the gate's local caches: it is run, never committed, packed or pushed (GPLv3 section 2: a covered work one
  makes and runs without conveying it). No GPL code enters the tree.
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
  - `sun/net/idn/uidna.spp` (doc/classes/JAVA-DEFERRED-TRIAGE.md, F3: java.net.IDN), copied from
    jdk26u's `src/java.base/share/classes/sun/net/idn/`: ICU4J's StringPrep profile for IDNA (RFC 3491),
    under the ICU4J License, the Unicode License V3 (text below), as jdk26u's
    `src/java.base/share/legal/icu.md` records ICU's data;
  - `conf/net.properties` (doc/classes/JAVA-DEFERRED-TRIAGE.md, F12: sun.net.NetProperties' defaults), as
    `make/modules/java.base/Copy.gmk` makes it: jdk26u's `src/java.base/share/conf/net.properties` followed
    by `src/java.base/unix/conf/net.properties`, Copyright (c) Oracle and/or its affiliates, under the GNU
    General Public License version 2 with the Classpath Exception (text below), as their notices say;
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
    `src/java.base/share/legal/`), zlib's is in them as jrt's ports of its compressor and
    decompressor to Java (`overlay/jdk/java.base/java/util/zip/Deflater.java`, `Inflater.java`,
    above; since JC6b), under the zlib License; none of the others is: not the public suffix list,
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
- `arbace/repr/asm.ae` and `arbace/repr/asm/` (`arbace.repr.asm.*`, repr/asm, the assembly crops' bridge, doc/asm/SPEC.md, AX1):
  the lexer's token rules (`lex.ae`), the GNU expression precedence and statement shapes (`gnu.ae`) and the AT&T operand
  grammar (`amd64/att.ae`) follow LLVM's `llvm/lib/MC/MCParser/AsmLexer.cpp`, `AsmParser.cpp` (`getGNUBinOpPrecedence`,
  `parseStatement`, `parseMacroLikeBody`) and `llvm/lib/Target/X86/AsmParser/X86AsmParser.cpp` (`ParseMemOperand`,
  `ParseOperand`) from `llvm-project-23.1.5.src.tar.xz` (the pin of `test/cpp/corpora.edn` and `test/asm/corpora.edn`), rule
  for rule, under the Apache License 2.0 with the LLVM Exceptions (section "LLVM (clang)" below). Its test corpora
  (`llvm/test/MC/{X86,AsmParser}`, extracted into `.tmp/`, never committed) are under the same licence; musl 1.2.6's
  `.s` files, read as a corpus, under musl's MIT licence (section "musl: MIT License").
- `arbace/repr/wasm.ae` and `arbace/repr/wasm/` (`arbace.repr.wasm.*`, repr/wasm, WebAssembly's bridge, doc/wasm/SPEC.md, WA1,
  WA2): `ops.ae`, the instruction table, is generated by `test/wasm/gen_ops.ae` from the WebAssembly specification's reference
  interpreter, https://github.com/WebAssembly/spec at the tag `wg-3.0` (`9d36019973201a19f9c9ebb0f10828b2fe2374aa`, the pin of
  `test/wasm/corpora.edn`), its `interpreter/binary/decode.ml` (each opcode, its immediates' reading) and
  `interpreter/text/lexer.mll` (the instructions' text names); `binary.ae`'s decoder follows `decode.ml`'s order of reads, its
  checks and its messages, rule for rule, and `lex.ae` and `text.ae` read the text format of the specification's chapter 6 with
  the reference interpreter's messages where the testsuite states them: under the Apache License 2.0 (section "The WebAssembly specification's reference interpreter and testsuite"
  below). The testsuite's scripts (https://github.com/WebAssembly/testsuite, the corpus of WA2 on, extracted into `.tmp/`, never
  committed) are under the same licence. The rest is Arbace's own, under the Eclipse Public License 1.0. `test/wasm/go/hello/`
  is Arbace's own; the Go-built corpus's other programs are read in place from `$GOROOT/test` (Go's BSD 3-Clause License).
- `arbace/asm/` (`arbace.asm.tablegen`, `arbace.asm.amd64.*`: the assembler of the assembly crops, AX2, doc/asm/AMD64.md):
  the TableGen reader and evaluator follows LLVM's `llvm/lib/TableGen/TGLexer.cpp`, `TGParser.cpp`, `Record.cpp` and
  `llvm/docs/TableGen/ProgRef.rst`, the table's generator `llvm/utils/TableGen/AsmMatcherEmitter.cpp` and
  `utils/TableGen/Common/CodeGenInstruction.cpp` (`FlattenAsmStringVariants`) and `CodeGenRegisters.cpp` (`TopoOrderRC`), the matcher
  `llvm/lib/Target/X86/AsmParser/X86AsmParser.cpp`, `X86Operand.h` and `X86AsmParserCommon.h`, the encoder
  `llvm/lib/Target/X86/MCTargetDesc/X86MCCodeEmitter.cpp`, `X86BaseInfo.h`, `X86MCTargetDesc.cpp` and
  `X86EncodingOptimization.cpp`, the printer `X86ATTInstPrinter.cpp`, `X86InstPrinterCommon.cpp`, `X86MCAsmInfo.cpp`,
  `llvm/lib/MC/MCInstPrinter.cpp`, `MCExpr.cpp`, `MCSymbol.cpp` and `MCAsmInfo.cpp` with the table's printer fields after
  `llvm/utils/TableGen/AsmWriterEmitter.cpp` and `Common/AsmWriterInst.cpp`, from the same `llvm-project-23.1.5.src.tar.xz`, rule for rule, under the Apache License 2.0 with
  the LLVM Exceptions (section "LLVM (clang)" below); no code is copied. `asm/amd64/table.edn`, generated from that tarball's
  `llvm/lib/Target/X86/*.td` and `llvm/include/llvm/{Target,CodeGen,IR}/*.td` (extracted into `.tmp/`, never committed), is a work
  derived from them, under the same licence. AX4's first slice: `arbace/asm/amd64/obj.ae` (the object assembler) follows
  `llvm/lib/MC/MCObjectStreamer.cpp`, `MCELFStreamer.cpp`, `MCAssembler.cpp`, `MCDwarf.cpp` (FrameEmitterImpl),
  `MCParser/ELFAsmParser.cpp`, `MCParser/AsmParser.cpp`, `X86AsmBackend.cpp`, `X86ELFObjectWriter.cpp` and
  `X86EncodingOptimizationForImmediate.def`, and `arbace/asm/elf.ae` (the ELF64 writer and reader) `llvm/lib/MC/ELFObjectWriter.cpp`
  and `StringTableBuilder.cpp`, the same tarball, licence and manner; no code is copied. x86-print (2026-11-22): `arbace/asm/amd64/lfi.ae`
  (the LFI rewriter) follows `llvm/lib/Target/X86/MCTargetDesc/X86MCLFIRewriter.cpp`, `llvm/lib/MC/MCLFI.cpp` and `MCLFIRewriter.cpp` of
  the same tarball, licence and manner. x86-objs (2026-11-22, AXS99 (b)): `arbace/asm/macho.ae` and `arbace/asm/amd64/macho.ae` (the
  Mach-O writer and reader, x86's relocations and compact unwind) follow `llvm/lib/MC/MachObjectWriter.cpp`, `MCMachOStreamer.cpp`,
  `MCParser/DarwinAsmParser.cpp`, `MCSectionMachO.cpp`, `MCObjectFileInfo.cpp`, `MCAsmInfoDarwin.cpp`, `MCDwarf.cpp` and
  `llvm/lib/Target/X86/MCTargetDesc/X86MachObjectWriter.cpp` and `X86AsmBackend.cpp`; `arbace/asm/coff.ae` and
  `arbace/asm/amd64/coff.ae` (the COFF writer and reader, x86's relocations and Win64 unwind tables) follow `WinCOFFObjectWriter.cpp`,
  `MCWinCOFFStreamer.cpp`, `MCParser/COFFAsmParser.cpp`, `MCWin64EH.cpp`, `MCStreamer.cpp`'s emitWinCFI* and
  `X86WinCOFFObjectWriter.cpp`, `X86WinCOFFStreamer.cpp`; `arbace/asm/amd64/objview.ae` (the views) follows
  `llvm/tools/llvm-objdump/{llvm-objdump,MachODump}.cpp`, `llvm/tools/llvm-readobj/{MachODumper,COFFDumper}.cpp`,
  `llvm/lib/Object/{MachOObjectFile,COFFObjectFile,FaultMapParser,RelocationResolver}.cpp` and llvm-nm's `llvm-nm.cpp`: the same
  tarball, licence and manner; no code is copied. The records `test/asm/records/obj-*.edn` hold
  llvm-mc 23.1.5's output for musl's files (MIT), Arbace's own tests and `llvm/test/MC/X86`'s (Apache 2.0 WITH LLVM-exception). AX4's
  second slice: `arbace/asm/link.ae` (the static linker) follows lld's `lld/ELF/` from the same tarball (`Driver.cpp`, `InputFiles.cpp`,
  `SymbolTable.cpp`, `Symbols.cpp`, `LinkerScript.cpp`, `Writer.cpp`, `OutputSections.cpp`, `SyntheticSections.cpp`, `InputSection.cpp`,
  `Relocations.cpp`, `Arch/X86_64.cpp`), the same licence and manner; no code is copied. `test/asm/run/` is Arbace's own; its
  `musl:` sources are read from musl 1.2.6's pinned tarball (MIT), never committed. AX4's third slice: `arbace/asm/amd64/decode.ae`
  (the decoder) follows `llvm/lib/Target/X86/Disassembler/X86Disassembler.cpp`, `X86DisassemblerDecoder.h`,
  `llvm/include/llvm/Support/X86DisassemblerDecoderCommon.h` and the disassembler tables' generator `llvm/utils/TableGen/X86RecognizableInstr.cpp`,
  `X86DisassemblerTables.cpp` and `X86ModRMFilters.h`, and `arbace/asm/amd64/decode_check.ae` `llvm/tools/llvm-mc/Disassembler.cpp`,
  the same tarball, licence and manner; no code is copied. `llvm/test/MC/Disassembler/X86` is read from the tarball as test data
  (extracted into `.tmp/`, never committed). AX5.2 (x86i): `arbace/asm/amd64/dgen.ae` writes the disassembler's decisions (the same
  X86DisassemblerTables.cpp rules, through `decode.ae`) as `go/arbace/x86i/dectab.ae`, a work derived from the table, and
  `go/arbace/x86i/decode.ae` and `tables.ae` follow `X86Disassembler.cpp`'s decoding for 64-bit mode's legacy and REX encodings, the
  same tarball, licence and manner; no code is copied. The rest of `go/arbace/x86i` (the families, the flags, the lockstep) and
  `go/arbace/emu` are Arbace's own, written from the Intel 64 and IA-32 Architectures Software Developer's Manual (vol. 2) and the AMD64
  Architecture Programmer's Manual (vol. 3) as references, nothing copied from them, and from the System V AMD64 psABI and the Linux
  man pages (getauxval(3), ptrace(2), personality(2)), not from the kernel's source. AX5.3 and AX5.4 (the step mode, the signal
  frames, the families rcl to movbe) likewise; the signal frame's layout from the psABI and musl 1.2.6's `arch/x86_64/bits/signal.h`
  (MIT, section "musl: MIT License"), nothing copied; `test/asm/x86i/gen.ae` and the programs it writes are Arbace's own.
- The same namespaces' arm64 part (AR1, doc/asm/ARM64-SURVEY.md): `arm64/gnu.ae`'s operand grammar follows LLVM's
  `llvm/lib/Target/AArch64/AsmParser/AArch64AsmParser.cpp` and `lex.ae`'s AArch64 dialects its
  `MCTargetDesc/AArch64MCAsmInfo.cpp` (the same tarball and licence); `plan9.ae`'s operand grammar follows Go 1.27.1's
  `src/cmd/asm/internal/asm/parse.go` (TamaGo's GOROOT, pinned by `seed/goroot.sha256`; section "Go: BSD 3-Clause License").
  No code is copied. Read as corpora, never committed: `llvm/test/MC/AArch64` (LLVM's licence), musl 1.2.6's aarch64 `.s`
  (MIT), Go's `cmd/asm` testdata (BSD-3-Clause), the box's arm64 `.s` (its own files' licences, box/README.md), and gcc
  15.2.0's `libgcc/config/aarch64/*.S` from the pinned tarball (GPL-3.0-or-later with the GCC Runtime Library Exception 3.1:
  read and printed back as test data, never ported).
- The same namespaces' A64 encoder (AR2, doc/asm/ARM64-SURVEY.md, "As built: AR2 (first slice)"): `arm64/table.ae` is generated
  data from Go 1.27.1's GOROOT (TamaGo's, pinned by `seed/goroot.sha256`): `src/cmd/vendor/golang.org/x/arch/arm64/arm64asm/`'s
  `tables.go` (the rows), `condition.go` (the alias conditions, translated into Arbace), `inst.go` (`sysInstsAttrs`) and `inst.json`
  (the should-be-one bits), golang.org/x/arch at `v0.27.1-0.20260521044007-9c1a596a2c97` as GOROOT's `cmd/vendor/modules.txt`
  names it, and `src/cmd/internal/obj/arm64/sysRegEnc.go` (the system registers); `arm64/bits.ae` ports arm64asm's
  `condition_util.go`; `arm64/enc.ae` inverts `decode.go`'s `decodeArg` kind by kind (written by hand, no code copied).
  golang.org/x/arch's `LICENSE` is Go's BSD 3-Clause License word for word but its first line, `Copyright 2015 The Go Authors.`
  (section "Go: BSD 3-Clause License"). Its check reads `llvm/test/MC/AArch64` as a corpus, never committed (LLVM's licence).
- AR2's second slice (doc/asm/ARM64-SURVEY.md, "As built: AR2 (second slice)"; ARS24 (a)): `asm/arm64/table.edn`, generated by
  `arbace/asm/arm64/gen.ae` from `llvm-project-23.1.5.src.tar.xz`'s `llvm/lib/Target/AArch64/*.td` with
  `llvm/include/llvm/{Target,CodeGen,IR,TableGen}/*.td` (read by Arbace's TableGen) and the literal messages of
  `llvm/lib/Target/AArch64/AsmParser/AArch64AsmParser.cpp`'s `showMatchError` and its `ExtensionDefs` table (read as text; all
  extracted into `.tmp/`, never committed), is a work derived from them, under the Apache License 2.0 with the LLVM Exceptions
  (section "LLVM (clang)" below). `arbace/repr/asm/arm64/lenc.ae` (the matcher and encoder over that table) follows
  `AArch64AsmParser.cpp` (`parseSysAlias`, `parseSyspAlias`, `setRequiredFeatureString`, `validateInstruction`'s MOPS rules,
  `MatchAndEmitInstruction`'s operand twiddles and messages), `MCTargetDesc/AArch64MCCodeEmitter.cpp` (the encoder methods),
  `MCTargetDesc/AArch64InstPrinter.cpp` (`printLogicalImm`, `printSIMDType10Operand`, ARS25's spelling) and
  `llvm/utils/TableGen/AsmMatcherEmitter.cpp` (MatchInstructionImpl's choice of diagnostic) from the same tarball, rule for rule; no
  code is copied. `test/asm/table_oracle_arm64.ae` reads llvm-tblgen's own output by hand (the oracle tier), never committed.
- `test/cpp/clang.ae` (`cpp.clang`, the clang half of Arbace's C++ target, doc/cpp/CPP-SURVEY.md, "As built: CP248 (a)"):
  a port of parts of clang, the LLVM Project, from `llvm-project-23.1.5.src.tar.xz` (the pin of `test/cpp/corpora.edn`,
  SHA-256 `fb50b1eac93ddeb3aa8dab4cfe324dff74f4a9c2bdee6160c9e36188f8d06a6e`): `clang/lib/Frontend/InitPreprocessor.cpp`'s
  predefined macros and their helpers, `clang/lib/Basic/Targets/X86.cpp`'s `getTargetDefines` (the baseline CPU's part),
  `Targets/OSTargets.h`'s `LinuxTargetInfo::getOSDefines` and `Targets.cpp`'s `DefineStd` and `defineCPUMacros`, under
  the Apache License 2.0 with the LLVM Exceptions (section "LLVM (clang)" below), the file carrying clang's notice;
  `cpp/target/clang.edn`, the facts it reads (PickFP's float strings among them, from the same file), likewise.
- `arbace/repr/cpp/diag.ae` (`arbace.repr.cpp.diag`, Arbace's own C++ front end's diagnostics, doc/cpp/CPP-SURVEY.md, "As built:
  CX12 (first slice)"): its diagnostics' texts are clang's (`clang/include/clang/Basic/DiagnosticSemaKinds.td`) and its rules follow
  clang's Sema (`clang/lib/Sema/SemaExpr.cpp`'s `DiagnoseEmptyLookup`, `SemaDecl.cpp`'s `MergeVarDecl`, `MergeFunctionDecl`,
  `ActOnTag`, `CheckRedeclaration`, `ActOnParamDeclarator`, `ActOnField`), the LLVM Project from `llvm-project-23.1.5.src.tar.xz` (the
  pin of `test/cpp/corpora.edn`), under the Apache License 2.0 with the LLVM Exceptions (section "LLVM (clang)" below), the file
  carrying clang's notice. Its second slice (doc/cpp/CPP-SURVEY.md, "As built: CX12 (second slice)") adds the checker's rules to the
  code generator's Sema, `arbace/cpp/sema.ae` (Arbace's own, written after the standard's rules), and to `arbace/cpp/template.ae`'s
  port of `SemaTemplateDeduction.cpp` (its MemberPointer case and the method qualifiers' mismatch, under that file's LLVM notice).
  Its rules for GNU attributes clang does not know (`gnu-attrs`, `gattr-rules`; CP294 (a), doc/cpp/CPP-SURVEY.md, "As built:
  CP290-CP294") are Arbace's own, written clean-room from the GCC 15.2 manual's description of each attribute (no GCC code read or
  ported; the messages Arbace's own). Its fifth slice (doc/cpp/CPP-SURVEY.md, "As built: CX12 (fifth slice)") follows
  `clang/lib/Sema/SemaDeclCXX.cpp`'s `CheckConstexprFunctionBody`, `CheckConstexprDeclStmt`, `CheckConstexprMissingReturn` and
  `MergeCXXFunctionDecl` of the same release (`constexpr-bodies`, `constexpr-redecls`), its texts clang's.
- `test/cpp/builtins.ae` (`cpp.builtins`) and `cpp/target/clang-builtins.edn` (CX12, CP270 (a)): the table is computed from the same
  tarball's `clang/include/clang/Basic/Builtins.td`, `BuiltinsBase.td`, `BuiltinsX86.td`, `BuiltinsX86Base.td`, `BuiltinsX86_64.td`
  (read as data, the names as `clang/utils/TableGen/ClangBuiltinsEmitter.cpp` makes them) and `TokenKinds.def`,
  `TransformTypeTraits.def` (the keywords), under the Apache License 2.0 with the LLVM Exceptions (section "LLVM (clang)" below);
  the reader carries clang's notice, the table names its sources.
- `test/java/` (bin/java-check, Java's conformance check, doc/classes/JAVA-CONFORMANCE-SURVEY.md,
  "As built: JC1"): its tools, data and the hand-curated programs of `test/java/jls/` are Arbace's
  own, under the Eclipse Public License 1.0. jdk26u's jtreg tests are read in place from the
  jdk26u checkout, not kept here, but for `test/java/forms/`: the essential subset's tests
  (`test/java/essential.txt`) as Java forms, converted by repr/java from
  https://github.com/openjdk/jdk26u at `baf63fb` (`test/`, each file put in a package of its own,
  `package tNNNNN; ` before its first line; `test/java/forms/provenance.edn`; each form file's
  first line names its source), under the GNU General Public License version 2 (the text below,
  "OpenJDK (jdk26u)"), without the Classpath Exception, which jdk26u's `test/` does not carry.
  They are inputs of the check, in no executable.
- `arbace/cpp/template.ae` and the template parts of `arbace/cpp/abi.ae`'s mangling (C++'s code generator,
  doc/cpp/CODEGEN.md §G3, by the user's decision P6 (b), 2026-11-05): ports of parts of clang, the LLVM Project,
  https://github.com/llvm/llvm-project at `llvmorg-23.1.4` (commit `7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`,
  the tarball pinned in `test/cpp/corpora.edn`): `clang/lib/Sema/SemaTemplateDeduction.cpp`'s
  `DeduceTemplateArgumentsByTypeMatch`, `AdjustFunctionParmAndArgTypesForDeduction`, `DeduceTemplateSpecArguments`,
  `DeduceTemplateBases`, `checkDeducedTemplateArguments`, `isForwardingReference`,
  `hasInconsistentOrSupersetQualifiersOf` and `isAtLeastAsSpecializedAs`, and `clang/lib/AST/ItaniumMangle.cpp`'s
  `mangleTemplatePrefix`, `mangleTemplateArgs`, `mangleTemplateParameter`, `mangleIntegerLiteral`, the return-type
  rule of `mangleFunctionEncodingBareType` and the `sr` form of `mangleUnresolvedPrefix`, and since CP311 (a)
  (doc/cpp/CPP-SURVEY.md, "As built: CP311 (a) and the rest of CP313 (a)", 2026-11-09) `arbace/cpp/template.ae`'s
  class template argument deduction, after `clang/lib/Sema/SemaTemplateDeductionGuide.cpp`'s
  `DeclareImplicitDeductionGuides`, `transformConstructor`, `buildSimpleDeductionGuide` and
  `DeclareAggregateDeductionGuideFromInitList` and `clang/lib/Sema/SemaInit.cpp`'s
  `DeduceTemplateSpecializationFromInitializer` at `llvmorg-23.1.5` (the tarball pinned in `test/cpp/corpora.edn`), under the Apache License
  2.0 with the LLVM Exceptions (section "LLVM (clang)" below), the files carrying clang's notice; and
  `arbace/cpp/inline.ae` (doc/cpp/CODEGEN.md G5, the user's decision P16 (a), 2026-11-05): a port of LLVM's
  AlwaysInliner at the same commit, `llvm/lib/Transforms/IPO/AlwaysInliner.cpp`'s `AlwaysInlineImpl`,
  `llvm/lib/Transforms/Utils/InlineFunction.cpp`'s `InlineFunctionImpl` (the parts -O0 IR uses),
  `HandleInlinedLandingPad`, `HandleCallsInBlockInlinedThroughInvoke`, `LandingPadInliningInfo::forwardResume` and
  `getInnerResumeDest`, and `llvm/lib/Analysis/InlineCost.cpp`'s `isInlineViable`, under the same license, the file
  carrying LLVM's notice. The rest of `arbace/cpp/` is Arbace's own, under the Eclipse Public License 1.0.
- `go/arbace/libm.ae` and `go/arbace/libm/` (the package `arbace/libm`, musl's libm in Go, shared by hsi and pyi;
  also `go/arbace/hs/prim_scalar.ae`'s `fmaf`, musl 1.2.6's `src/math/fmaf.c` ported likewise, HE4e;
  doc/haskell/HASKELL-EVAL-SURVEY.md, HES37 (a), "As built: HE4b, HE4c (write-ahead)", "As built: HES38's :full
  forms"; until then `go/arbace/hs/libm.ae` and `libm_data.ae`): ported to Go by hand, then converted to Go forms, from musl 1.2.6's
  `src/math` (https://musl.libc.org/releases/musl-1.2.6.tar.gz, SHA-256
  `d585fd3b613c66151fc3249e8ed44f77020cb5e6c1e635a616d3f9f82460512a`, the pin of `test/c/corpora.edn`; the files the
  first file's header names): musl, Copyright (c) 2005-2020 Rich Felker, et al., under the MIT License below ("musl");
  its exp, log, pow, expf, logf and powf code and their tables Copyright (c) 2017-2018 Arm Limited, MIT; the rest from
  FreeBSD's msun, Copyright (C) 1993 Sun Microsystems, Inc. ("Permission to use, copy, modify, and distribute this
  software is freely granted, provided that this notice is preserved"), the float kernels and the argument reductions
  as optimized by Bruce D. Evans and David Schultz (Copyright (c) 2003-2011, "extremely permissive terms", musl's
  COPYRIGHT). `go/arbace/hs/` and `test/haskell/hsi/libm-ref.c` are Arbace's own, under the Eclipse Public
  License 1.0.
- `go/arbace/hs/cbits.ae` and `go/arbace/hs/cdata.ae` (hsi's C for the packages bytestring and text, doc/haskell/HASKELL-EVAL-SURVEY.md,
  "As built: HE5 (write-ahead)"): bytestring's `cbits/itoa.c`, `fpstring.c`, `shortbytestring.c`, `is-valid-utf8.c` (its
  result by Go's `unicode/utf8`) and the tables of `cbits/aligned-static-hs-data.c` (`cdata.ae`, generated by
  `bin/lib/hsi/cdata-gen`) ported to Go, and text's `cbits/is_ascii.c`, `measure_off.c`, `reverse.c` and `utils.c`, from GHC
  9.14.1's source tarball (`libraries/bytestring`, `libraries/text`; the pin of `tools/repr-haskell`'s corpus): bytestring,
  Copyright (c) Don Stewart 2005-2009, Duncan Coutts 2006-2015, David Roundy 2003-2005, Simon Meier 2010-2011, Koz Ross 2021,
  under the BSD 3-Clause License below ("bytestring"); text, Copyright (c) 2008-2009 Tom Harper (the cbits files' own notices
  too, Andrew Lelechenko 2021), under the BSD 2-Clause License below ("text"). The rest of `go/arbace/hs/` (`posix.ae`,
  `cformat.ae` and the rest) is Arbace's own, under the Eclipse Public License 1.0.
- The Hackage packages of Haskell's eval corpora (hs-pkgs-1122; the user's decision of 2026-11-22): `test/haskell/hackage.edn` pins
  the source tarballs (and Hackage's revised `.cabal` files) of old-locale 1.0.0.7, old-time 1.1.1.0, splitmix 0.1.3.2, random 1.3.1,
  parallel 3.3.0.0, primitive 0.9.1.0 and unboxed-ref 0.4.0.0 from https://hackage.haskell.org, which `bin/hsi hackage fetch`
  extracts into `.tmp/` and the native front end compiles as own libraries (nothing of them is committed; the records name their
  modules and outputs): old-locale, old-time and random under the Glasgow Haskell Compiler License with the Haskell 98 Report's
  notice, parallel under the Glasgow Haskell Compiler License, splitmix (Copyright (c) 2017 Oleg Grenrus), primitive (Copyright (c)
  2008-2009 Roman Leshchinskiy) and unboxed-ref (Copyright (c) 2017 winter) under the BSD 3-Clause License, each text below ("The
  Hackage packages of Haskell's eval corpora").
- `arbace/repr/haskell/cmm.ae` and `test/haskell/eval/cmm/Cmm.h`, `MachDeps.h` (queue 332, hsi's Cmm, doc/haskell/HASKELL-EVAL-SURVEY.md,
  "HES67's design: Cmm on hsi"): `cmm.ae` ports `compiler/GHC/Cmm/Lexer.x.source`, `compiler/GHC/Cmm/Parser.y.source` and
  `compiler/GHC/Cmm/MachOp.hs`'s machOpResultType, the headers copy definitions of `rts/include/Cmm.h`, `rts/include/MachDeps.h` and
  `rts/include/rts/storage/ClosureTypes.h`, from GHC 9.14.1's source tarball (the pin of `tools/repr-haskell`'s corpus): GHC, Copyright
  The University Court of the University of Glasgow, under the Glasgow Haskell Compiler License below ("GHC: the Glasgow Haskell
  Compiler License"). The Cmm forms, their elaboration and hsi's Cmm back end `go/arbace/hs/cmm.ae` are Arbace's own, under the Eclipse
  Public License 1.0.
- `go/arbace/hs/alloclimit.ae` and hsi's driver's `test/haskell/hsi/cmd/rtsflags.ae` and `rtsusage.ae` (HE9's allocation
  limits and the RTS's options, doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE9 on main"): `alloclimit.ae`'s rules from GHC
  9.14.1's `rts/Schedule.c` (schedulePostRunThread) and `rts/PrimOps.cmm` (stg_{get,set}ThreadAllocationCounterzh);
  `rtsflags.ae` ports `rts/RtsFlags.c`'s option parsing (procRtsOpts, decodeSize, parseDouble, read_trace_flags, bad_option,
  errorUsage) and `rtsusage.ae` transcribes its `usage_text[]`, from GHC 9.14.1's source tarball (the pin of
  `tools/repr-haskell`'s corpus): GHC, Copyright The University Court of the University of Glasgow, under the Glasgow Haskell
  Compiler License below ("GHC: the Glasgow Haskell Compiler License"). The rest of these files is Arbace's own, under the
  Eclipse Public License 1.0.
- `scheme/psyntax/` (Scheme's expander, doc/scheme/SCHEME-SURVEY.md, "As built: SM4"; origins in
  `scheme/provenance.edn`): `expander.ae`, `library-manager.ae`, `builders.ae` and `config.ae` are
  psyntax, the R6RS library and syntax-case system of Abdulaziz Ghuloum and R. Kent Dybvig, as Ikarus
  Scheme carried it (https://github.com/lambdaconservatory/ikarus at
  `1f2b250ed9e8cb1c2466c939a9a305461db71739`, `scheme/psyntax.*.ss`, each file's notice kept), as Scheme
  forms; `psyntax.pp.ae` is generated from them (psyntax expanded by itself), and `tools/psyntax/boot.ss` and
  `tools/psyntax/boot-si.sps` (its program on si itself, SMR7b) follow the structure of `psyntax-buildscript.ss` of the portable psyntax-r6rs (revision 4, 2007, by the
  same authors): all under the MIT License below ("psyntax"). `compat.ae`, `internal.ae`, `patches.edn`,
  `tools/psyntax/chez/`, `tools/psyntax/run.ss` and `tools/psyntax/expand.ss` are Arbace's own, under the
  Eclipse Public License 1.0 (Ikarus's own `psyntax.compat.ss`, under the GPL version 3, is not vendored).
- `scheme/lib/srfi/` (the SRFIs' reference implementations ported as R6RS libraries, their tests in
  `test/scheme/srfi/`): each SRFI's section below, "SRFI 1" to "SRFI 133", names its repository, its
  revision and its license.
- `scheme/vendor/chibi/` (Chibi Scheme 0.12's `(chibi test)` and the libraries it imports, with three of
  their tests; `scheme/vendor/chibi/provenance.edn`) and `scheme/lib/chibi/optional.sls`, derived from
  it: under the BSD 3-Clause License below ("Chibi Scheme").
- `arbace/repr/scheme/category.ae`, `arbace/repr/scheme/unicode.ae`, `arbace/repr/scheme/casefold.ae` and
  `go/arbace/scm/unicode.ae` (SCS9 (c), SCS12 (c), SCS13 (b), doc/scheme/SCHEME-SURVEY.md, "As built: SCS9 (c), SCS10 (b),
  SCS11 (b)", "As built: SCS12-SCS14"): generated by `arbace.repr.scheme.ucd` from the Unicode Character Database 16.0.0
  (https://www.unicode.org/Public/16.0.0/ucd/ `UnicodeData.txt`, `extracted/DerivedGeneralCategory.txt` and `CaseFolding.txt`,
  pinned by SHA-256 in `test/scheme/ucd.edn`, fetched 2026-11-08; since SCS16 (b) and SCS17 (c), "As built: SCS15-SCS18", also
  `SpecialCasing.txt`, `DerivedCoreProperties.txt` and `PropList.txt`, fetched 2026-11-09; since SCS21 (b) also
  `auxiliary/WordBreakProperty.txt`, fetched 2026-11-09; since SCS23 (b) also `emoji/emoji-data.txt`'s Extended_Pictographic, fetched
  2026-11-09, and since SCS24 (b) `auxiliary/WordBreakTest.txt`, the cases si's unit test `go/arbace/scm/wordbreak_test.ae` reads, not
  copied into the tree): the general categories, the identifier,
  whitespace and printer escape ranges derived from them, the case foldings, si's properties (Alphabetic, Numeric, White_Space,
  Uppercase, Lowercase, Cased, Case_Ignorable, Extended_Pictographic), its simple and full case mappings and its Word_Break classes are Unicode's data, under the Unicode License V3 below ("The
  Unicode Character Database 16.0.0 (repr/scheme's Unicode data)"); the generator and the files' code are Arbace's own,
  under the Eclipse Public License 1.0.
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
- `test/programs/junit4/patches/`: two variants of pinned third-party sources (bin/java-project's `:patches`, A10 of the
  Ghidra case study's G1 plan), whose context lines are theirs: `hamcrest-core-1.3-javac.patch`, of hamcrest-core 1.3
  (`org.hamcrest:hamcrest-core:1.3`'s sources jar, http://hamcrest.org), under the BSD License below ("Hamcrest");
  `junit-4.13.2-jdk18.patch`, of JUnit 4.13.2 (`junit:junit:4.13.2`'s sources jar, https://github.com/junit-team/junit4
  at `r4.13.2`), under the Eclipse Public License 1.0 (text below). The artifacts themselves are fetched by pin
  (`test/java/project/maven-pins.edn`) into `.tmp/maven`, never committed.

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

## cc (modernc.org/cc/v4) and go-whim's C23 delta: BSD 3-Clause License

From the `LICENSE` of `crefactor/cc` in https://github.com/arbace/go-whim at
`80c49bf53cd6340487ac623f3d6d135b77ac1db6` (upstream modernc.org/cc/v4 v4.29.7's, unchanged), covering
what `arbace/repr/c/lex.ae` and `pp.ae` port from cc; go-whim's delta and Arbace's changes in them are
under the Eclipse Public License 1.0 (above):

```
Copyright (c) 2014 The CC Authors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

   * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
   * Neither the names of the authors nor the names of the
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

## psyntax: MIT License

From the head of each file of psyntax in https://github.com/lambdaconservatory/ikarus at
`1f2b250ed9e8cb1c2466c939a9a305461db71739` (`scheme/psyntax.expander.ss`, `psyntax.library-manager.ss`,
`psyntax.builders.ss`, `psyntax.config.ss`), the same as the `README.txt` of psyntax-r6rs revision 4; it covers
`scheme/psyntax/expander.ae`, `library-manager.ae`, `builders.ae`, `config.ae`, `psyntax.pp.ae` and
`tools/psyntax/boot.ss` (above):

```
Copyright (c) 2006, 2007 Abdulaziz Ghuloum and Kent Dybvig

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
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

## LLVM (clang): Apache License 2.0 with LLVM Exceptions

The LLVM Project's `LICENSE.TXT` (in `llvm-project-23.1.4.src/clang/LICENSE.TXT`, sha256
`ebcd9bbf783a73d05c53ba4d586b8d5813dcdf3bbec50265860ccc885e606f47`) states: "The LLVM Project is under the Apache
License v2.0 with LLVM Exceptions". The Apache License, Version 2.0, January 2004, is given in full in the section
"Murmur3 (Guava): Apache License 2.0" above. The exceptions, verbatim:

```
---- LLVM Exceptions to the Apache 2.0 License ----

As an exception, if, as a result of your compiling your source code, portions
of this Software are embedded into an Object form of such source code, you
may redistribute such embedded portions in such Object form without complying
with the conditions of Sections 4(a), 4(b) and 4(d) of the License.

In addition, if you combine or link compiled forms of this Software with
software that is licensed under the GPLv2 ("Combined Software") and if a
court of competent jurisdiction determines that the patent provision (Section
3), the indemnity provision (Section 9) or other Section of the License
conflicts with the conditions of the GPLv2, you may retroactively and
prospectively choose to deem waived or otherwise exclude such Section(s) of
the License, but only in their entirety and only with respect to the Combined
Software.
```

The file's further sections (software from third parties, which the ported functions are not, and the legacy
University of Illinois/NCSA license of code contributed before the relicensing) state no terms for the ported parts.

## purego: Apache License 2.0

Copyright 2022 The Ebitengine Authors (https://github.com/ebitengine/purego, v0.11.1). purego's
`LICENSE` is the Apache License, Version 2.0, January 2004, whose text is given in full in the
section "Murmur3 (Guava): Apache License 2.0" above (purego's file also carries the license's
appendix, "How to apply the Apache License to your work", which states no further terms).

## The WebAssembly specification's reference interpreter and testsuite: Apache License 2.0

The WebAssembly specification, https://github.com/WebAssembly/spec (tag `wg-3.0`, commit
`9d36019973201a19f9c9ebb0f10828b2fe2374aa`): its `LICENSE` states that `interpreter/` and `test/` are under the Apache License
2.0 (`document/` under the W3C Software and Document Notice and License, read only; `papers/` under CC-BY-4.0, not used). The
WebAssembly testsuite, https://github.com/WebAssembly/testsuite (commit `92a3d806c7d70f5b63f125e6155b8d76304509f1`): its `LICENSE`
is the Apache License, Version 2.0, January 2004. Copyright the WebAssembly Community Group's contributors. The license's text
is given in full in the section "Murmur3 (Guava): Apache License 2.0" above. Covered here: `arbace/repr/wasm/ops.ae` (generated
from `interpreter/binary/decode.ml` and `interpreter/text/lexer.mll`), the decoder's and the text reader's rules and messages
after the reference interpreter (`arbace/repr/wasm/binary.ae`, `lex.ae`, `text.ae`), and the testsuite's scripts as test data
(read from `.tmp/`, not distributed; any later committed copy of an essential subset is under this license).

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

## The Unicode Character Database 16.0.0 (repr/scheme's Unicode data): Unicode License V3

Covers the data of `arbace/repr/scheme/category.ae`, `arbace/repr/scheme/unicode.ae`, `arbace/repr/scheme/casefold.ae` and
`go/arbace/scm/unicode.ae`, generated from the Unicode Character Database 16.0.0 (`UnicodeData.txt`,
`extracted/DerivedGeneralCategory.txt`, `CaseFolding.txt`, and since SCS16 (b) `SpecialCasing.txt`, `DerivedCoreProperties.txt`,
`PropList.txt`, since SCS21 (b) `auxiliary/WordBreakProperty.txt`, and since SCS23 (b) and SCS24 (b) `emoji/emoji-data.txt` and
`auxiliary/WordBreakTest.txt` (the latter read by si's unit test, not copied); https://www.unicode.org/Public/16.0.0/ucd/, pinned by `test/scheme/ucd.edn`). The license, the text of
https://www.unicode.org/license.txt as fetched on 2026-11-08 (SHA-256
`e7a93b009565cfce55919a381437ac4db883e9da2126fa28b91d12732bc53d96`, pinned in `test/scheme/ucd.edn`):

```
UNICODE LICENSE V3

COPYRIGHT AND PERMISSION NOTICE

Copyright © 1991-2026 Unicode, Inc.

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

## zlib: zlib License

Covers `overlay/jdk/java.base/java/util/zip/Deflater.java` and `Inflater.java` (ports of zlib's
compressor and decompressor; the compressor in `overlay/jdk/javac/java.base/` until JC6b) and the
form files converted from them (`jdk/forms/java/util/zip/Deflater.ae`, `Inflater.ae`; until JC6b
`jdk/javac/forms/java/util/zip/Deflater.ae`). The license, as
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

## The OCaml Core System: GNU Lesser General Public License 2.1 with the OCaml LGPL linking exception

The text of OCaml 5.5.0's `LICENSE`, as distributed in its release tarball:

```
In the following, "the OCaml Core System" refers to all files marked
"Copyright INRIA" in this distribution.

The OCaml Core System is distributed under the terms of the
GNU Lesser General Public License (LGPL) version 2.1 (included below).

As a special exception to the GNU Lesser General Public License, you
may link, statically or dynamically, a "work that uses the OCaml Core
System" with a publicly distributed version of the OCaml Core System
to produce an executable file containing portions of the OCaml Core
System, and distribute that executable file under terms of your
choice, without any of the additional requirements listed in clause 6
of the GNU Lesser General Public License.  By "a publicly distributed
version of the OCaml Core System", we mean either the unmodified OCaml
Core System as distributed by INRIA, or a modified version of the
OCaml Core System that is distributed under the conditions defined in
clause 2 of the GNU Lesser General Public License.  This exception
does not however invalidate any other reasons why the executable file
might be covered by the GNU Lesser General Public License.

----------------------------------------------------------------------

GNU LESSER GENERAL PUBLIC LICENSE

Version 2.1, February 1999

Copyright (C) 1991, 1999 Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
Everyone is permitted to copy and distribute verbatim copies
of this license document, but changing it is not allowed.

[This is the first released version of the Lesser GPL.  It also counts
 as the successor of the GNU Library Public License, version 2, hence
 the version number 2.1.]

Preamble

The licenses for most software are designed to take away your freedom to share and change it. By contrast, the GNU General Public Licenses are intended to guarantee your freedom to share and change free software--to make sure the software is free for all its users.

This license, the Lesser General Public License, applies to some specially designated software packages--typically libraries--of the Free Software Foundation and other authors who decide to use it. You can use it too, but we suggest you first think carefully about whether this license or the ordinary General Public License is the better strategy to use in any particular case, based on the explanations below.

When we speak of free software, we are referring to freedom of use, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software (and charge for this service if you wish); that you receive source code or can get it if you want it; that you can change the software and use pieces of it in new free programs; and that you are informed that you can do these things.

To protect your rights, we need to make restrictions that forbid distributors to deny you these rights or to ask you to surrender these rights. These restrictions translate to certain responsibilities for you if you distribute copies of the library or if you modify it.

For example, if you distribute copies of the library, whether gratis or for a fee, you must give the recipients all the rights that we gave you. You must make sure that they, too, receive or can get the source code. If you link other code with the library, you must provide complete object files to the recipients, so that they can relink them with the library after making changes to the library and recompiling it. And you must show them these terms so they know their rights.

We protect your rights with a two-step method: (1) we copyright the library, and (2) we offer you this license, which gives you legal permission to copy, distribute and/or modify the library.

To protect each distributor, we want to make it very clear that there is no warranty for the free library. Also, if the library is modified by someone else and passed on, the recipients should know that what they have is not the original version, so that the original author's reputation will not be affected by problems that might be introduced by others.

Finally, software patents pose a constant threat to the existence of any free program. We wish to make sure that a company cannot effectively restrict the users of a free program by obtaining a restrictive license from a patent holder. Therefore, we insist that any patent license obtained for a version of the library must be consistent with the full freedom of use specified in this license.

Most GNU software, including some libraries, is covered by the ordinary GNU General Public License. This license, the GNU Lesser General Public License, applies to certain designated libraries, and is quite different from the ordinary General Public License. We use this license for certain libraries in order to permit linking those libraries into non-free programs.

When a program is linked with a library, whether statically or using a shared library, the combination of the two is legally speaking a combined work, a derivative of the original library. The ordinary General Public License therefore permits such linking only if the entire combination fits its criteria of freedom. The Lesser General Public License permits more lax criteria for linking other code with the library.

We call this license the "Lesser" General Public License because it does Less to protect the user's freedom than the ordinary General Public License. It also provides other free software developers Less of an advantage over competing non-free programs. These disadvantages are the reason we use the ordinary General Public License for many libraries. However, the Lesser license provides advantages in certain special circumstances.

For example, on rare occasions, there may be a special need to encourage the widest possible use of a certain library, so that it becomes a de-facto standard. To achieve this, non-free programs must be allowed to use the library. A more frequent case is that a free library does the same job as widely used non-free libraries. In this case, there is little to gain by limiting the free library to free software only, so we use the Lesser General Public License.

In other cases, permission to use a particular library in non-free programs enables a greater number of people to use a large body of free software. For example, permission to use the GNU C Library in non-free programs enables many more people to use the whole GNU operating system, as well as its variant, the GNU/Linux operating system.

Although the Lesser General Public License is Less protective of the users' freedom, it does ensure that the user of a program that is linked with the Library has the freedom and the wherewithal to run that program using a modified version of the Library.

The precise terms and conditions for copying, distribution and modification follow. Pay close attention to the difference between a "work based on the library" and a "work that uses the library". The former contains code derived from the library, whereas the latter must be combined with the library in order to run.

TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. This License Agreement applies to any software library or other program which contains a notice placed by the copyright holder or other authorized party saying it may be distributed under the terms of this Lesser General Public License (also called "this License"). Each licensee is addressed as "you".

A "library" means a collection of software functions and/or data prepared so as to be conveniently linked with application programs (which use some of those functions and data) to form executables.

The "Library", below, refers to any such software library or work which has been distributed under these terms. A "work based on the Library" means either the Library or any derivative work under copyright law: that is to say, a work containing the Library or a portion of it, either verbatim or with modifications and/or translated straightforwardly into another language. (Hereinafter, translation is included without limitation in the term "modification".)

"Source code" for a work means the preferred form of the work for making modifications to it. For a library, complete source code means all the source code for all modules it contains, plus any associated interface definition files, plus the scripts used to control compilation and installation of the library.

Activities other than copying, distribution and modification are not covered by this License; they are outside its scope. The act of running a program using the Library is not restricted, and output from such a program is covered only if its contents constitute a work based on the Library (independent of the use of the Library in a tool for writing it). Whether that is true depends on what the Library does and what the program that uses the Library does.

1. You may copy and distribute verbatim copies of the Library's complete source code as you receive it, in any medium, provided that you conspicuously and appropriately publish on each copy an appropriate copyright notice and disclaimer of warranty; keep intact all the notices that refer to this License and to the absence of any warranty; and distribute a copy of this License along with the Library.

You may charge a fee for the physical act of transferring a copy, and you may at your option offer warranty protection in exchange for a fee.

2. You may modify your copy or copies of the Library or any portion of it, thus forming a work based on the Library, and copy and distribute such modifications or work under the terms of Section 1 above, provided that you also meet all of these conditions:

    a) The modified work must itself be a software library.
    b) You must cause the files modified to carry prominent notices stating that you changed the files and the date of any change.
    c) You must cause the whole of the work to be licensed at no charge to all third parties under the terms of this License.
    d) If a facility in the modified Library refers to a function or a table of data to be supplied by an application program that uses the facility, other than as an argument passed when the facility is invoked, then you must make a good faith effort to ensure that, in the event an application does not supply such function or table, the facility still operates, and performs whatever part of its purpose remains meaningful.

    (For example, a function in a library to compute square roots has a purpose that is entirely well-defined independent of the application. Therefore, Subsection 2d requires that any application-supplied function or table used by this function must be optional: if the application does not supply it, the square root function must still compute square roots.)

These requirements apply to the modified work as a whole. If identifiable sections of that work are not derived from the Library, and can be reasonably considered independent and separate works in themselves, then this License, and its terms, do not apply to those sections when you distribute them as separate works. But when you distribute the same sections as part of a whole which is a work based on the Library, the distribution of the whole must be on the terms of this License, whose permissions for other licensees extend to the entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest your rights to work written entirely by you; rather, the intent is to exercise the right to control the distribution of derivative or collective works based on the Library.

In addition, mere aggregation of another work not based on the Library with the Library (or with a work based on the Library) on a volume of a storage or distribution medium does not bring the other work under the scope of this License.

3. You may opt to apply the terms of the ordinary GNU General Public License instead of this License to a given copy of the Library. To do this, you must alter all the notices that refer to this License, so that they refer to the ordinary GNU General Public License, version 2, instead of to this License. (If a newer version than version 2 of the ordinary GNU General Public License has appeared, then you can specify that version instead if you wish.) Do not make any other change in these notices.

Once this change is made in a given copy, it is irreversible for that copy, so the ordinary GNU General Public License applies to all subsequent copies and derivative works made from that copy.

This option is useful when you wish to copy part of the code of the Library into a program that is not a library.

4. You may copy and distribute the Library (or a portion or derivative of it, under Section 2) in object code or executable form under the terms of Sections 1 and 2 above provided that you accompany it with the complete corresponding machine-readable source code, which must be distributed under the terms of Sections 1 and 2 above on a medium customarily used for software interchange.

If distribution of object code is made by offering access to copy from a designated place, then offering equivalent access to copy the source code from the same place satisfies the requirement to distribute the source code, even though third parties are not compelled to copy the source along with the object code.

5. A program that contains no derivative of any portion of the Library, but is designed to work with the Library by being compiled or linked with it, is called a "work that uses the Library". Such a work, in isolation, is not a derivative work of the Library, and therefore falls outside the scope of this License.

However, linking a "work that uses the Library" with the Library creates an executable that is a derivative of the Library (because it contains portions of the Library), rather than a "work that uses the library". The executable is therefore covered by this License. Section 6 states terms for distribution of such executables.

When a "work that uses the Library" uses material from a header file that is part of the Library, the object code for the work may be a derivative work of the Library even though the source code is not. Whether this is true is especially significant if the work can be linked without the Library, or if the work is itself a library. The threshold for this to be true is not precisely defined by law.

If such an object file uses only numerical parameters, data structure layouts and accessors, and small macros and small inline functions (ten lines or less in length), then the use of the object file is unrestricted, regardless of whether it is legally a derivative work. (Executables containing this object code plus portions of the Library will still fall under Section 6.)

Otherwise, if the work is a derivative of the Library, you may distribute the object code for the work under the terms of Section 6. Any executables containing that work also fall under Section 6, whether or not they are linked directly with the Library itself.

6. As an exception to the Sections above, you may also combine or link a "work that uses the Library" with the Library to produce a work containing portions of the Library, and distribute that work under terms of your choice, provided that the terms permit modification of the work for the customer's own use and reverse engineering for debugging such modifications.

You must give prominent notice with each copy of the work that the Library is used in it and that the Library and its use are covered by this License. You must supply a copy of this License. If the work during execution displays copyright notices, you must include the copyright notice for the Library among them, as well as a reference directing the user to the copy of this License. Also, you must do one of these things:

    a) Accompany the work with the complete corresponding machine-readable source code for the Library including whatever changes were used in the work (which must be distributed under Sections 1 and 2 above); and, if the work is an executable linked with the Library, with the complete machine-readable "work that uses the Library", as object code and/or source code, so that the user can modify the Library and then relink to produce a modified executable containing the modified Library. (It is understood that the user who changes the contents of definitions files in the Library will not necessarily be able to recompile the application to use the modified definitions.)
    b) Use a suitable shared library mechanism for linking with the Library. A suitable mechanism is one that (1) uses at run time a copy of the library already present on the user's computer system, rather than copying library functions into the executable, and (2) will operate properly with a modified version of the library, if the user installs one, as long as the modified version is interface-compatible with the version that the work was made with.
    c) Accompany the work with a written offer, valid for at least three years, to give the same user the materials specified in Subsection 6a, above, for a charge no more than the cost of performing this distribution.
    d) If distribution of the work is made by offering access to copy from a designated place, offer equivalent access to copy the above specified materials from the same place.
    e) Verify that the user has already received a copy of these materials or that you have already sent this user a copy.

For an executable, the required form of the "work that uses the Library" must include any data and utility programs needed for reproducing the executable from it. However, as a special exception, the materials to be distributed need not include anything that is normally distributed (in either source or binary form) with the major components (compiler, kernel, and so on) of the operating system on which the executable runs, unless that component itself accompanies the executable.

It may happen that this requirement contradicts the license restrictions of other proprietary libraries that do not normally accompany the operating system. Such a contradiction means you cannot use both them and the Library together in an executable that you distribute.

7. You may place library facilities that are a work based on the Library side-by-side in a single library together with other library facilities not covered by this License, and distribute such a combined library, provided that the separate distribution of the work based on the Library and of the other library facilities is otherwise permitted, and provided that you do these two things:

    a) Accompany the combined library with a copy of the same work based on the Library, uncombined with any other library facilities. This must be distributed under the terms of the Sections above.
    b) Give prominent notice with the combined library of the fact that part of it is a work based on the Library, and explaining where to find the accompanying uncombined form of the same work.

8. You may not copy, modify, sublicense, link with, or distribute the Library except as expressly provided under this License. Any attempt otherwise to copy, modify, sublicense, link with, or distribute the Library is void, and will automatically terminate your rights under this License. However, parties who have received copies, or rights, from you under this License will not have their licenses terminated so long as such parties remain in full compliance.

9. You are not required to accept this License, since you have not signed it. However, nothing else grants you permission to modify or distribute the Library or its derivative works. These actions are prohibited by law if you do not accept this License. Therefore, by modifying or distributing the Library (or any work based on the Library), you indicate your acceptance of this License to do so, and all its terms and conditions for copying, distributing or modifying the Library or works based on it.

10. Each time you redistribute the Library (or any work based on the Library), the recipient automatically receives a license from the original licensor to copy, distribute, link with or modify the Library subject to these terms and conditions. You may not impose any further restrictions on the recipients' exercise of the rights granted herein. You are not responsible for enforcing compliance by third parties with this License.

11. If, as a consequence of a court judgment or allegation of patent infringement or for any other reason (not limited to patent issues), conditions are imposed on you (whether by court order, agreement or otherwise) that contradict the conditions of this License, they do not excuse you from the conditions of this License. If you cannot distribute so as to satisfy simultaneously your obligations under this License and any other pertinent obligations, then as a consequence you may not distribute the Library at all. For example, if a patent license would not permit royalty-free redistribution of the Library by all those who receive copies directly or indirectly through you, then the only way you could satisfy both it and this License would be to refrain entirely from distribution of the Library.

If any portion of this section is held invalid or unenforceable under any particular circumstance, the balance of the section is intended to apply, and the section as a whole is intended to apply in other circumstances.

It is not the purpose of this section to induce you to infringe any patents or other property right claims or to contest validity of any such claims; this section has the sole purpose of protecting the integrity of the free software distribution system which is implemented by public license practices. Many people have made generous contributions to the wide range of software distributed through that system in reliance on consistent application of that system; it is up to the author/donor to decide if he or she is willing to distribute software through any other system and a licensee cannot impose that choice.

This section is intended to make thoroughly clear what is believed to be a consequence of the rest of this License.

12. If the distribution and/or use of the Library is restricted in certain countries either by patents or by copyrighted interfaces, the original copyright holder who places the Library under this License may add an explicit geographical distribution limitation excluding those countries, so that distribution is permitted only in or among countries not thus excluded. In such case, this License incorporates the limitation as if written in the body of this License.

13. The Free Software Foundation may publish revised and/or new versions of the Lesser General Public License from time to time. Such new versions will be similar in spirit to the present version, but may differ in detail to address new problems or concerns.

Each version is given a distinguishing version number. If the Library specifies a version number of this License which applies to it and "any later version", you have the option of following the terms and conditions either of that version or of any later version published by the Free Software Foundation. If the Library does not specify a license version number, you may choose any version ever published by the Free Software Foundation.

14. If you wish to incorporate parts of the Library into other free programs whose distribution conditions are incompatible with these, write to the author to ask for permission. For software which is copyrighted by the Free Software Foundation, write to the Free Software Foundation; we sometimes make exceptions for this. Our decision will be guided by the two goals of preserving the free status of all derivatives of our free software and of promoting the sharing and reuse of software generally.

NO WARRANTY

15. BECAUSE THE LIBRARY IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY FOR THE LIBRARY, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE LIBRARY "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE LIBRARY IS WITH YOU. SHOULD THE LIBRARY PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

16. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE LIBRARY AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE LIBRARY (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE LIBRARY TO OPERATE WITH ANY OTHER SOFTWARE), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
END OF TERMS AND CONDITIONS

How to Apply These Terms to Your New Libraries

If you develop a new library, and you want it to be of the greatest possible use to the public, we recommend making it free software that everyone can redistribute and change. You can do so by permitting redistribution under these terms (or, alternatively, under the terms of the ordinary General Public License).

To apply these terms, attach the following notices to the library. It is safest to attach them to the start of each source file to most effectively convey the exclusion of warranty; and each file should have at least the "copyright" line and a pointer to where the full notice is found.

one line to give the library's name and an idea of what it does.
Copyright (C) year  name of author

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Also add information on how to contact you by electronic and paper mail.

You should also get your employer (if you work as a programmer) or your school, if any, to sign a "copyright disclaimer" for the library, if necessary. Here is a sample; alter the names:

Yoyodyne, Inc., hereby disclaims all copyright interest in
the library `Frob' (a library for tweaking knobs) written
by James Random Hacker.

signature of Ty Coon, 1 April 1990
Ty Coon, President of Vice

That's all there is to it!

--------------------------------------------------

## Menhir and MenhirLib: GNU Library General Public License version 2 with a special exception on linking

MenhirLib, Menhir's runtime library, as OCaml 5.5.0's release tarball carries it (`boot/menhir/menhirLib.ml`), states:

```
Menhir

Copyright Inria. All rights reserved. This file is distributed under
the terms of the GNU Library General Public License version 2, with a
special exception on linking, as described in the file LICENSE.
```

Menhir's standard library (`standard.mly`) is distributed under the same terms. In the tarball that carries
MenhirLib, the file LICENSE is OCaml's: the GNU Lesser General Public License text and the linking exception of
the section "The OCaml Core System" above (the Lesser GPL 2.1 is the Library GPL 2's successor). Arbace's files
that follow them are named in the list at the top (repr/ocaml's native reader).

## SRFI 14 (char-sets): MIT Scheme license

Covers `scheme/lib/srfi/%3a14/char-sets.sls` (the port of Olin Shivers' reference implementation `srfi-14.scm`, himself derived from
MIT Scheme's runtime; srfi-14 repository at d6e98129d1e075c03bfe4dcb30e1b5847727e26e, `scheme/lib/srfi/provenance-14.edn`) and
the tests `test/scheme/srfi/srfi-14.sps` derived from `srfi-14-tests.scm`. The license, as `srfi-14.scm` states it at its end:

```
Copyright (c) 1988-1995 Massachusetts Institute of Technology

This material was developed by the Scheme project at the Massachusetts
Institute of Technology, Department of Electrical Engineering and
Computer Science.  Permission to copy and modify this software, to
redistribute either the original software or a modified version, and
to use this software for any purpose is granted, subject to the
following restrictions and understandings.

1. Any copy made of this software must include this copyright notice
in full.

2. Users of this software agree to make their best efforts (a) to
return to the MIT Scheme project any improvements or extensions that
they make, so that these may be included in future releases; and (b)
to inform MIT of noteworthy uses of this software.

3. All materials developed as a consequence of the use of this
software shall duly acknowledge such use, in accordance with the usual
standards of acknowledging credit in academic research.

4. MIT has made no warrantee or representation that the operation of
this software will be error-free, and MIT is under no obligation to
provide any services, by way of maintenance, update, or otherwise.

5. In conjunction with products arising from the use of this material,
there shall be no use of the name of the Massachusetts Institute of
Technology nor of any adaptation thereof in any advertising,
promotional, or sales literature without prior written consent from
MIT in each case.
```

## SRFI 128 (comparators): MIT license

Covers `scheme/lib/srfi/%3a128/comparators.sls` and `scheme/lib/srfi/%3a128.sls` and `test/scheme/srfi/srfi-128.sps` (John Cowan's
reference implementation and tests, github.com/scheme-requests-for-implementation/srfi-128).

```
Copyright (C) John Cowan (2015). All Rights Reserved.

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
```

## SRFI 125 (intermediate hash tables): William D Clinger's notice

Covers `scheme/lib/srfi/%3a125/hash-tables.sls`, `scheme/lib/srfi/%3a125.sls` and `test/scheme/srfi/srfi-125.sps` (William D
Clinger's reference implementation and tests, github.com/scheme-requests-for-implementation/srfi-125).

```
Copyright 2015 William D Clinger.

Permission to copy this software, in whole or in part, to use this
software for any lawful purpose, and to redistribute this software
is granted subject to the restriction that all copies made of this
software must include this copyright and permission notice in full.

I also request that you send me a copy of any improvements that you
make to this software so that they may be incorporated within it to
the benefit of the Scheme community.
```

## SRFI 158 (generators and accumulators): MIT license

Covers `scheme/lib/srfi/%3a158/generators-and-accumulators.sls`, `scheme/lib/srfi/%3a158.sls` and `test/scheme/srfi/srfi-158.sps`
(Shiro Kawai, John Cowan and Thomas Gilray's reference implementation and tests,
github.com/scheme-requests-for-implementation/srfi-158; the notice is the SRFI document's).

```
Copyright (C) Shiro Kawai, John Cowan, Thomas Gilray (2015). All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
```

## SRFI 1 (lists): Olin Shivers' notice and the MIT license

Covers `scheme/lib/srfi/%3a1/lists.sls` (the port of Olin Shivers' reference implementation `srfi-1-reference.scm`; srfi-1 repository at 72442d67896181afccd4519c36140ba65ccfd0ca, `scheme/lib/srfi/provenance-1.edn`), which keeps the notice below at its top, and the tests `test/scheme/srfi/srfi-1.sps`. The notice of the reference file, then the MIT license text of the repository's `LICENSES/MIT.txt`:

```
Copyright (c) 1998, 1999 by Olin Shivers. You may do as you please with
this code as long as you do not remove this copyright notice or
hold me liable for its use. Please send bug reports to shivers@ai.mit.edu.
    -Olin

MIT License

Copyright (c) <year> <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## SRFI 26 (cut): public domain and the MIT license

Covers `scheme/lib/srfi/%3a26/cut.sls` (the port of `cut.scm`, which its authors Sebastian Egner and Al Petrofsky place in the public domain: "placed in the public domain"; srfi-26 repository at 2102d4d0db3ab74744a2765d9237027a55c5f58b, `scheme/lib/srfi/provenance-26.edn`) and the tests `test/scheme/srfi/srfi-26.sps` derived from `check.scm`. The SRFI document is under the MIT license, as its Copyright section states:

```
Copyright (C) Sebastian Egner (2002). All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## SRFI 39 (parameters): MIT license

Covers `scheme/lib/srfi/%3a39/parameters.sls` (the reference implementation printed in the SRFI document; srfi-39 repository at 840e8d7e8b3bc4fbbc0cf8c78ce52dde42211702, `scheme/lib/srfi/provenance-39.edn`) and the tests `test/scheme/srfi/srfi-39.sps`. The license, as the SRFI document states it:

```
Copyright (C) Marc Feeley 2002. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## SRFI 69 (basic hash tables): MIT license

Covers `scheme/lib/srfi/%3a69/basic-hash-tables.sls` (the reference implementation printed in the SRFI document; srfi-69 repository at c9c3604c1818f3f27cf54b43e77c327a02a6c53f, `scheme/lib/srfi/provenance-69.edn`) and the tests `test/scheme/srfi/srfi-69.sps`. The license, as the SRFI document states it:

```
Copyright (C) Panu Kalliokoski (2005). All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## SRFI 98 (OS environment variables): MIT license

Covers `scheme/lib/srfi/%3a98/os-environment-variables.sls` (written for the SRFI's specification; srfi-98 repository at 8f28224c77be945bf5fd4d5e750831979f23adc4, `scheme/lib/srfi/provenance-98.edn`) and the tests `test/scheme/srfi/srfi-98.sps`. The specification's license, as the SRFI document states it:

```
Copyright (C) Taro Minowa (Higepon) (2008). All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## SRFI 13 (strings): MIT Scheme and Scsh licenses

Covers `scheme/lib/srfi/%3a13/strings.sls` (the port of Olin Shivers' reference implementation `srfi-13.scm`, srfi-13 repository at
e608d1280b73944cfc96a05d8da6003cf296f09f, `scheme/lib/srfi/provenance-13.edn`) and `test/scheme/srfi/srfi-13.sps`. The prefix, suffix and
comparison routines are under the MIT Scheme license (the text is in the section "SRFI 14 (char-sets): MIT Scheme license"
above), the remainder under the Scsh license. `srfi-13.scm` states them at its end:

```
Copyright details

The prefix/suffix and comparison routines in this code had (extremely
distant) origins in MIT Scheme's string lib, and was substantially
reworked by Olin Shivers (shivers@ai.mit.edu) 9/98. As such, it is
covered by MIT Scheme's open source copyright. See below for details.

The KMP string-search code was influenced by implementations written
by Stephen Bevan, Brian Dehneyer and Will Fitzgerald. However, this
version was written from scratch by myself.

The remainder of this code was written from scratch by myself for scsh.
The scsh copyright is a BSD-style open source copyright. See below for
details.
    -Olin Shivers

MIT Scheme copyright terms

This material was developed by the Scheme project at the Massachusetts
Institute of Technology, Department of Electrical Engineering and
Computer Science.  Permission to copy and modify this software, to
redistribute either the original software or a modified version, and
to use this software for any purpose is granted, subject to the
following restrictions and understandings.

1. Any copy made of this software must include this copyright notice
in full.

2. Users of this software agree to make their best efforts (a) to
return to the MIT Scheme project any improvements or extensions that
they make, so that these may be included in future releases; and (b)
to inform MIT of noteworthy uses of this software.

3. All materials developed as a consequence of the use of this
software shall duly acknowledge such use, in accordance with the usual
standards of acknowledging credit in academic research.

4. MIT has made no warrantee or representation that the operation of
this software will be error-free, and MIT is under no obligation to
provide any services, by way of maintenance, update, or otherwise.

5. In conjunction with products arising from the use of this material,
there shall be no use of the name of the Massachusetts Institute of
Technology nor of any adaptation thereof in any advertising,
promotional, or sales literature without prior written consent from
MIT in each case.

Scsh copyright terms

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the authors may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## SRFI 133 (vectors): MIT License

Covers `scheme/lib/srfi/%3a133/vectors.sls` (the port of John Cowan's `vectors/vectors-impl.scm`, srfi-133 repository at
12923135eeebedcff84972a4b5140b0f97306160, `scheme/lib/srfi/provenance-133.edn`; the code is Taylor Campbell's SRFI 43 code, placed in the
public domain, with corrections by Will Clinger and changes by John Cowan, public domain or under this license) and
`test/scheme/srfi/srfi-133.sps` (derived from `vectors/vectors-test.scm`). The license, as the repository's `LICENSE` states it:

```
Copyright (C) John Cowan (2016). All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Chibi Scheme: BSD 3-Clause License

Covers `scheme/vendor/chibi/` (Chibi-Scheme 0.12's `(chibi test)`, `(chibi diff)`, `(chibi optional)` and `(chibi term ansi)`, their `.sld` files and the
`.scm` files they include, and the three libraries' own tests `diff-test.sld`, `optional-test.sld` and `term/ansi-test.sld` (SC142), unchanged:
byte for byte the files of `lib/chibi/` in https://github.com/ashinn/chibi-scheme at the tag `0.12`, commit
`08e23be118e7de8cf29d606bc4793371c0ace680`, as the host's chibi-scheme package installs them; `scheme/vendor/chibi/provenance.edn`). The license is
the repository's `COPYING` at that tag, quoted below verbatim (SHA-256 `86805fe7af1bd20d5f5050d86c61d6fe038452cb08c6106a08761e010d37da43`, the same
file test/scheme/corpora.edn pins for the chibi-r7rs corpus; the package does not install it). The files keep their own notices: `test.scm`'s
"Copyright (c) 2010-2025 Alex Shinn. All rights reserved. BSD-style license: http://synthcode.com/license.txt" (that page gives the same text with
2000-2015), `ansi.scm`'s 2010-2014. `scheme/lib/chibi/optional.sls` (SC150) is `optional.scm`'s definitions (Alex Shinn, the same license) with
`let-optionals*` written afresh for si. `scheme/lib/chibi/test.sls` is Arbace's own, written from the description in `r7rs-tests.scm`'s header (SM8),
under the Eclipse Public License 1.0.

```
Copyright (c) 2009-2021 Alex Shinn
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## Expat: MIT License

The `COPYING` of Expat 2.9.0 as CPython 3.14.10 vendors it (`Modules/expat/COPYING`); it covers `go/arbace/expat.ae`
and `go/arbace/expat/` (above):

```
Copyright (c) 1998-2000 Thai Open Source Software Center Ltd and Clark Cooper
Copyright (c) 2001-2025 Expat maintainers

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## bzip2: its license

The `LICENSE` of bzip2 1.0.8 (the tarball above); it covers bzip2's part of `go/arbace/clib/bzip2.ae` and
`go/arbace/clib/bzip2/` (above):

```
--------------------------------------------------------------------------

This program, "bzip2", the associated library "libbzip2", and all
documentation, are copyright (C) 1996-2019 Julian R Seward.  All
rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. The origin of this software must not be misrepresented; you must 
   not claim that you wrote the original software.  If you use this 
   software in a product, an acknowledgment in the product 
   documentation would be appreciated but is not required.

3. Altered source versions must be plainly marked as such, and must
   not be misrepresented as being the original software.

4. The name of the author may not be used to endorse or promote 
   products derived from this software without specific prior written 
   permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Julian Seward, jseward@acm.org
bzip2/libbzip2 version 1.0.8 of 13 July 2019

--------------------------------------------------------------------------
```

## XZ Utils: its licensing

The `COPYING` of XZ Utils 5.2.5 (the tarball above), whole; liblzma, the part `go/arbace/clib/xz.ae` and
`go/arbace/clib/xz/` hold, is in the public domain:

```
XZ Utils Licensing
==================

    Different licenses apply to different files in this package. Here
    is a rough summary of which licenses apply to which parts of this
    package (but check the individual files to be sure!):

      - liblzma is in the public domain.

      - xz, xzdec, and lzmadec command line tools are in the public
        domain unless GNU getopt_long had to be compiled and linked
        in from the lib directory. The getopt_long code is under
        GNU LGPLv2.1+.

      - The scripts to grep, diff, and view compressed files have been
        adapted from gzip. These scripts and their documentation are
        under GNU GPLv2+.

      - All the documentation in the doc directory and most of the
        XZ Utils specific documentation files in other directories
        are in the public domain.

      - Translated messages are in the public domain.

      - The build system contains public domain files, and files that
        are under GNU GPLv2+ or GNU GPLv3+. None of these files end up
        in the binaries being built.

      - Test files and test code in the tests directory, and debugging
        utilities in the debug directory are in the public domain.

      - The extra directory may contain public domain files, and files
        that are under various free software licenses.

    You can do whatever you want with the files that have been put into
    the public domain. If you find public domain legally problematic,
    take the previous sentence as a license grant. If you still find
    the lack of copyright legally problematic, you have too many
    lawyers.

    As usual, this software is provided "as is", without any warranty.

    If you copy significant amounts of public domain code from XZ Utils
    into your project, acknowledging this somewhere in your software is
    polite (especially if it is proprietary, non-free software), but
    naturally it is not legally required. Here is an example of a good
    notice to put into "about box" or into documentation:

        This software includes code from XZ Utils <https://tukaani.org/xz/>.

    The following license texts are included in the following files:
      - COPYING.LGPLv2.1: GNU Lesser General Public License version 2.1
      - COPYING.GPLv2: GNU General Public License version 2
      - COPYING.GPLv3: GNU General Public License version 3

    Note that the toolchain (compiler, linker etc.) may add some code
    pieces that are copyrighted. Thus, it is possible that e.g. liblzma
    binary wouldn't actually be in the public domain in its entirety
    even though it contains no copyrighted code from the XZ Utils source
    package.

    If you have questions, don't hesitate to ask the author(s) for more
    information.
```

## SQLite: public domain

The notice every file of SQLite 3.50.4's amalgamation (the tarball above) carries, as `sqlite3.c` opens `sqliteInt.h`:

```
** The author disclaims copyright to this source code.  In place of
** a legal notice, here is a blessing:
**
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
```

## zstd: BSD License

The `LICENSE` of zstd 1.5.7 (the tarball above), whole; it covers zstd's part of `go/arbace/clib/zstd.ae` and
`go/arbace/clib/zstd/`:

```
BSD License

For Zstandard software

Copyright (c) Meta Platforms, Inc. and affiliates. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

 * Neither the name Facebook, nor Meta, nor the names of its contributors may
   be used to endorse or promote products derived from this software without
   specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## musl: MIT License

The `COPYRIGHT` of musl 1.2.6 (https://musl.libc.org/releases/musl-1.2.6.tar.gz, test/c/corpora.edn's pin), whole; it
covers musl's part of the node programs in `go/arbace/clib/` and hsi's libm, `go/arbace/libm` (above; merged at batch 2 with
haskell-90's section of musl's `COPYRIGHT`, its part for the whole and for `src/math`, the same text):

```
musl as a whole is licensed under the following standard MIT license:

----------------------------------------------------------------------
Copyright © 2005-2020 Rich Felker, et al.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
----------------------------------------------------------------------

Authors/contributors include:

A. Wilcox
Ada Worcester
Alex Dowad
Alex Suykov
Alexander Monakov
Andre McCurdy
Andrew Kelley
Anthony G. Basile
Aric Belsito
Arvid Picciani
Bartosz Brachaczek
Benjamin Peterson
Bobby Bingham
Boris Brezillon
Brent Cook
Chris Spiegel
Clément Vasseur
Daniel Micay
Daniel Sabogal
Daurnimator
David Carlier
David Edelsohn
Denys Vlasenko
Dmitry Ivanov
Dmitry V. Levin
Drew DeVault
Emil Renner Berthing
Fangrui Song
Felix Fietkau
Felix Janda
Gianluca Anzolin
Hauke Mehrtens
He X
Hiltjo Posthuma
Isaac Dunham
Jaydeep Patil
Jens Gustedt
Jeremy Huntwork
Jo-Philipp Wich
Joakim Sindholt
John Spencer
Julien Ramseier
Justin Cormack
Kaarle Ritvanen
Khem Raj
Kylie McClain
Leah Neukirchen
Luca Barbato
Luka Perkov
Lynn Ochs
M Farkas-Dyck (Strake)
Mahesh Bodapati
Markus Wichmann
Masanori Ogino
Michael Clark
Michael Forney
Mikhail Kremnyov
Natanael Copa
Nicholas J. Kain
orc
Pascal Cuoq
Patrick Oppenlander
Petr Hosek
Petr Skocik
Pierre Carrier
Reini Urban
Rich Felker
Richard Pennington
Ryan Fairfax
Samuel Holland
Segev Finer
Shiz
sin
Solar Designer
Stefan Kristiansson
Stefan O'Rear
Szabolcs Nagy
Timo Teräs
Trutz Behn
Will Dietz
William Haddon
William Pitcock

Portions of this software are derived from third-party works licensed
under terms compatible with the above MIT license:

The TRE regular expression implementation (src/regex/reg* and
src/regex/tre*) is Copyright © 2001-2008 Ville Laurikari and licensed
under a 2-clause BSD license (license text in the source files). The
included version has been heavily modified by Rich Felker in 2012, in
the interests of size, simplicity, and namespace cleanliness.

Much of the math library code (src/math/* and src/complex/*) is
Copyright © 1993,2004 Sun Microsystems or
Copyright © 2003-2011 David Schultz or
Copyright © 2003-2009 Steven G. Kargl or
Copyright © 2003-2009 Bruce D. Evans or
Copyright © 2008 Stephen L. Moshier or
Copyright © 2017-2018 Arm Limited
and labelled as such in comments in the individual source files. All
have been licensed under extremely permissive terms.

The ARM memcpy code (src/string/arm/memcpy.S) is Copyright © 2008
The Android Open Source Project and is licensed under a two-clause BSD
license. It was taken from Bionic libc, used on Android.

The AArch64 memcpy and memset code (src/string/aarch64/*) are
Copyright © 1999-2019, Arm Limited.

The implementation of DES for crypt (src/crypt/crypt_des.c) is
Copyright © 1994 David Burren. It is licensed under a BSD license.

The implementation of blowfish crypt (src/crypt/crypt_blowfish.c) was
originally written by Solar Designer and placed into the public
domain. The code also comes with a fallback permissive license for use
in jurisdictions that may not recognize the public domain.

The smoothsort implementation (src/stdlib/qsort.c) is Copyright © 2011
Lynn Ochs and is licensed under an MIT-style license.

The x86_64 port was written by Nicholas J. Kain and is licensed under
the standard MIT terms.

The mips and microblaze ports were originally written by Richard
Pennington for use in the ellcc project. The original code was adapted
by Rich Felker for build system and code conventions during upstream
integration. It is licensed under the standard MIT terms.

The mips64 port was contributed by Imagination Technologies and is
licensed under the standard MIT terms.

The powerpc port was also originally written by Richard Pennington,
and later supplemented and integrated by John Spencer. It is licensed
under the standard MIT terms.

All other files which have no copyright comments are original works
produced specifically for use as part of this library, written either
by Rich Felker, the main author of the library, or by one or more
contibutors listed above. Details on authorship of individual files
can be found in the git version control history of the project. The
omission of copyright and license comments in each file is in the
interest of source tree size.

In addition, permission is hereby granted for all public header files
(include/* and arch/*/bits/*) and crt files intended to be linked into
applications (crt/*, ldso/dlstart.c, and arch/*/crt_arch.h) to omit
the copyright notice and permission notice otherwise required by the
license, and to use these files without any requirement of
attribution. These files include substantial contributions from:

Bobby Bingham
John Spencer
Nicholas J. Kain
Rich Felker
Richard Pennington
Stefan Kristiansson
Szabolcs Nagy

all of whom have explicitly granted such permission.

This file previously contained text expressing a belief that most of
the files covered by the above exception were sufficiently trivial not
to be subject to copyright, resulting in confusion over whether it
negated the permissions granted in the license. In the spirit of
permissive licensing, and of not having licensing issues being an
obstacle to adoption, that text has been removed.
## libffi: MIT License

The `LICENSE` of libffi 3.8.0 (https://github.com/libffi/libffi/releases/download/v3.8.0/libffi-3.8.0.tar.gz,
`test/haskell/target/libffi.edn`'s pin), whole; it covers what `arbace/repr/haskell/target.ae` ports of its configure and the
two headers it makes from the tarball (above):

```
libffi - Copyright (c) 1996-2026  Anthony Green, Red Hat, Inc and others.
See source files for details.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
``Software''), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## GHC: the Glasgow Haskell Compiler License

The `LICENSE` of GHC 9.14.1's source tarball (above), covering what repr/haskell's native reader
ports from GHC:

```
The Glasgow Haskell Compiler License

Copyright 2002, The University Court of the University of Glasgow.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

- Neither name of the University nor the names of its contributors may be
used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY COURT OF THE UNIVERSITY OF
GLASGOW AND THE CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
UNIVERSITY COURT OF THE UNIVERSITY OF GLASGOW OR THE CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
DAMAGE.
```

## Cabal: BSD 3-Clause License

The license of Cabal-syntax 3.16.0.0 (GHC 9.14.1's tarball, `libraries/Cabal/Cabal-syntax/LICENSE`), covering
`arbace/repr/haskell/cabal.ae`:

```
Copyright (c) 2003-2025, Cabal Development Team.
See the AUTHORS file for the full list of copyright holders.

See */LICENSE for the copyright holders of the subcomponents.


All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Isaac Jones nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

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

## CPython: the PSF License Agreement for Python 3.14

The `LICENSE` of the CPython 3.14.10 release tarball, whole (its history and the licenses it carries):

```
A. HISTORY OF THE SOFTWARE
==========================

Python was created in the early 1990s by Guido van Rossum at Stichting
Mathematisch Centrum (CWI, see https://www.cwi.nl) in the Netherlands
as a successor of a language called ABC.  Guido remains Python's
principal author, although it includes many contributions from others.

In 1995, Guido continued his work on Python at the Corporation for
National Research Initiatives (CNRI, see https://www.cnri.reston.va.us)
in Reston, Virginia where he released several versions of the
software.

In May 2000, Guido and the Python core development team moved to
BeOpen.com to form the BeOpen PythonLabs team.  In October of the same
year, the PythonLabs team moved to Digital Creations, which became
Zope Corporation.  In 2001, the Python Software Foundation (PSF, see
https://www.python.org/psf/) was formed, a non-profit organization
created specifically to own Python-related Intellectual Property.
Zope Corporation was a sponsoring member of the PSF.

All Python releases are Open Source (see https://opensource.org for
the Open Source Definition).  Historically, most, but not all, Python
releases have also been GPL-compatible; the table below summarizes
the various releases.

    Release         Derived     Year        Owner       GPL-
                    from                                compatible? (1)

    0.9.0 thru 1.2              1991-1995   CWI         yes
    1.3 thru 1.5.2  1.2         1995-1999   CNRI        yes
    1.6             1.5.2       2000        CNRI        no
    2.0             1.6         2000        BeOpen.com  no
    1.6.1           1.6         2001        CNRI        yes (2)
    2.1             2.0+1.6.1   2001        PSF         no
    2.0.1           2.0+1.6.1   2001        PSF         yes
    2.1.1           2.1+2.0.1   2001        PSF         yes
    2.1.2           2.1.1       2002        PSF         yes
    2.1.3           2.1.2       2002        PSF         yes
    2.2 and above   2.1.1       2001-now    PSF         yes

Footnotes:

(1) GPL-compatible doesn't mean that we're distributing Python under
    the GPL.  All Python licenses, unlike the GPL, let you distribute
    a modified version without making your changes open source.  The
    GPL-compatible licenses make it possible to combine Python with
    other software that is released under the GPL; the others don't.

(2) According to Richard Stallman, 1.6.1 is not GPL-compatible,
    because its license has a choice of law clause.  According to
    CNRI, however, Stallman's lawyer has told CNRI's lawyer that 1.6.1
    is "not incompatible" with the GPL.

Thanks to the many outside volunteers who have worked under Guido's
direction to make these releases possible.


B. TERMS AND CONDITIONS FOR ACCESSING OR OTHERWISE USING PYTHON
===============================================================

Python software and documentation are licensed under the
Python Software Foundation License Version 2.

Starting with Python 3.8.6, examples, recipes, and other code in
the documentation are dual licensed under the PSF License Version 2
and the Zero-Clause BSD license.

Some software incorporated into Python is under different licenses.
The licenses are listed with code falling under that license.


PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
--------------------------------------------

1. This LICENSE AGREEMENT is between the Python Software Foundation
("PSF"), and the Individual or Organization ("Licensee") accessing and
otherwise using this software ("Python") in source or binary form and
its associated documentation.

2. Subject to the terms and conditions of this License Agreement, PSF hereby
grants Licensee a nonexclusive, royalty-free, world-wide license to reproduce,
analyze, test, perform and/or display publicly, prepare derivative works,
distribute, and otherwise use Python alone or in any derivative version,
provided, however, that PSF's License Agreement and PSF's notice of copyright,
i.e., "Copyright (c) 2001 Python Software Foundation; All Rights Reserved"
are retained in Python alone or in any derivative version prepared by Licensee.

3. In the event Licensee prepares a derivative work that is based on
or incorporates Python or any part thereof, and wants to make
the derivative work available to others as provided herein, then
Licensee hereby agrees to include in any such work a brief summary of
the changes made to Python.

4. PSF is making Python available to Licensee on an "AS IS"
basis.  PSF MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PSF MAKES NO AND
DISCLAIMS ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS
FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF PYTHON WILL NOT
INFRINGE ANY THIRD PARTY RIGHTS.

5. PSF SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF PYTHON
FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS AS
A RESULT OF MODIFYING, DISTRIBUTING, OR OTHERWISE USING PYTHON,
OR ANY DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.

6. This License Agreement will automatically terminate upon a material
breach of its terms and conditions.

7. Nothing in this License Agreement shall be deemed to create any
relationship of agency, partnership, or joint venture between PSF and
Licensee.  This License Agreement does not grant permission to use PSF
trademarks or trade name in a trademark sense to endorse or promote
products or services of Licensee, or any third party.

8. By copying, installing or otherwise using Python, Licensee
agrees to be bound by the terms and conditions of this License
Agreement.


BEOPEN.COM LICENSE AGREEMENT FOR PYTHON 2.0
-------------------------------------------

BEOPEN PYTHON OPEN SOURCE LICENSE AGREEMENT VERSION 1

1. This LICENSE AGREEMENT is between BeOpen.com ("BeOpen"), having an
office at 160 Saratoga Avenue, Santa Clara, CA 95051, and the
Individual or Organization ("Licensee") accessing and otherwise using
this software in source or binary form and its associated
documentation ("the Software").

2. Subject to the terms and conditions of this BeOpen Python License
Agreement, BeOpen hereby grants Licensee a non-exclusive,
royalty-free, world-wide license to reproduce, analyze, test, perform
and/or display publicly, prepare derivative works, distribute, and
otherwise use the Software alone or in any derivative version,
provided, however, that the BeOpen Python License is retained in the
Software, alone or in any derivative version prepared by Licensee.

3. BeOpen is making the Software available to Licensee on an "AS IS"
basis.  BEOPEN MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, BEOPEN MAKES NO AND
DISCLAIMS ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS
FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF THE SOFTWARE WILL NOT
INFRINGE ANY THIRD PARTY RIGHTS.

4. BEOPEN SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF THE
SOFTWARE FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS
AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE SOFTWARE, OR ANY
DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.

5. This License Agreement will automatically terminate upon a material
breach of its terms and conditions.

6. This License Agreement shall be governed by and interpreted in all
respects by the law of the State of California, excluding conflict of
law provisions.  Nothing in this License Agreement shall be deemed to
create any relationship of agency, partnership, or joint venture
between BeOpen and Licensee.  This License Agreement does not grant
permission to use BeOpen trademarks or trade names in a trademark
sense to endorse or promote products or services of Licensee, or any
third party.  As an exception, the "BeOpen Python" logos available at
http://www.pythonlabs.com/logos.html may be used according to the
permissions granted on that web page.

7. By copying, installing or otherwise using the software, Licensee
agrees to be bound by the terms and conditions of this License
Agreement.


CNRI LICENSE AGREEMENT FOR PYTHON 1.6.1
---------------------------------------

1. This LICENSE AGREEMENT is between the Corporation for National
Research Initiatives, having an office at 1895 Preston White Drive,
Reston, VA 20191 ("CNRI"), and the Individual or Organization
("Licensee") accessing and otherwise using Python 1.6.1 software in
source or binary form and its associated documentation.

2. Subject to the terms and conditions of this License Agreement, CNRI
hereby grants Licensee a nonexclusive, royalty-free, world-wide
license to reproduce, analyze, test, perform and/or display publicly,
prepare derivative works, distribute, and otherwise use Python 1.6.1
alone or in any derivative version, provided, however, that CNRI's
License Agreement and CNRI's notice of copyright, i.e., "Copyright (c)
1995-2001 Corporation for National Research Initiatives; All Rights
Reserved" are retained in Python 1.6.1 alone or in any derivative
version prepared by Licensee.  Alternately, in lieu of CNRI's License
Agreement, Licensee may substitute the following text (omitting the
quotes): "Python 1.6.1 is made available subject to the terms and
conditions in CNRI's License Agreement.  This Agreement together with
Python 1.6.1 may be located on the internet using the following
unique, persistent identifier (known as a handle): 1895.22/1013.  This
Agreement may also be obtained from a proxy server on the internet
using the following URL: http://hdl.handle.net/1895.22/1013".

3. In the event Licensee prepares a derivative work that is based on
or incorporates Python 1.6.1 or any part thereof, and wants to make
the derivative work available to others as provided herein, then
Licensee hereby agrees to include in any such work a brief summary of
the changes made to Python 1.6.1.

4. CNRI is making Python 1.6.1 available to Licensee on an "AS IS"
basis.  CNRI MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR
IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, CNRI MAKES NO AND
DISCLAIMS ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS
FOR ANY PARTICULAR PURPOSE OR THAT THE USE OF PYTHON 1.6.1 WILL NOT
INFRINGE ANY THIRD PARTY RIGHTS.

5. CNRI SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF PYTHON
1.6.1 FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS AS
A RESULT OF MODIFYING, DISTRIBUTING, OR OTHERWISE USING PYTHON 1.6.1,
OR ANY DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.

6. This License Agreement will automatically terminate upon a material
breach of its terms and conditions.

7. This License Agreement shall be governed by the federal
intellectual property law of the United States, including without
limitation the federal copyright law, and, to the extent such
U.S. federal law does not apply, by the law of the Commonwealth of
Virginia, excluding Virginia's conflict of law provisions.
Notwithstanding the foregoing, with regard to derivative works based
on Python 1.6.1 that incorporate non-separable material that was
previously distributed under the GNU General Public License (GPL), the
law of the Commonwealth of Virginia shall govern this License
Agreement only as to issues arising under or with respect to
Paragraphs 4, 5, and 7 of this License Agreement.  Nothing in this
License Agreement shall be deemed to create any relationship of
agency, partnership, or joint venture between CNRI and Licensee.  This
License Agreement does not grant permission to use CNRI trademarks or
trade name in a trademark sense to endorse or promote products or
services of Licensee, or any third party.

8. By clicking on the "ACCEPT" button where indicated, or by copying,
installing or otherwise using Python 1.6.1, Licensee agrees to be
bound by the terms and conditions of this License Agreement.

        ACCEPT


CWI LICENSE AGREEMENT FOR PYTHON 0.9.0 THROUGH 1.2
--------------------------------------------------

Copyright (c) 1991 - 1995, Stichting Mathematisch Centrum Amsterdam,
The Netherlands.  All rights reserved.

Permission to use, copy, modify, and distribute this software and its
documentation for any purpose and without fee is hereby granted,
provided that the above copyright notice appear in all copies and that
both that copyright notice and this permission notice appear in
supporting documentation, and that the name of Stichting Mathematisch
Centrum or CWI not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

STICHTING MATHEMATISCH CENTRUM DISCLAIMS ALL WARRANTIES WITH REGARD TO
THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS, IN NO EVENT SHALL STICHTING MATHEMATISCH CENTRUM BE LIABLE
FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

ZERO-CLAUSE BSD LICENSE FOR CODE IN THE PYTHON DOCUMENTATION
----------------------------------------------------------------------

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
```

## LLVM (llvm/test files in test/llvm/essential/, repr/llvm's verifier and tables): Apache License 2.0 with LLVM Exceptions

The modules of `test/llvm/essential/` are files of `llvm/test` of llvm-project at `llvmorg-23.1.4` (commit 7a941dfb2877a0b8cf30323fefa85ad019e0b0f5, `llvm-project-23.1.4.src.tar.xz`, pinned in `test/cpp/corpora.edn`), copied unchanged as test data; the rest of `llvm/test`, read in place from the pinned tarball, is not stored. The same release's `llvm/lib/IR/Verifier.cpp`, `Intrinsics.cpp`, `AutoUpgrade.cpp`, `Type.cpp`, `Attributes.cpp`, `AsmWriter.cpp`, `DebugInfoMetadata.cpp` and `llvm/lib/AsmParser/LLParser.cpp`, `LLLexer.cpp` are reimplemented in part by `arbace/repr/llvm/verify.ae`, `read.ae`, `intr.ae` and `diag.ae` (doc/ARBACE.md), and those of release 23.1.5 (`llvm-project-23.1.5.src.tar.xz`, the same `llvm/LICENSE.TXT`) with `Attributes.td`, `CallingConv.h`, `DiagnosticInfo.cpp` and (LLS36 (a)) `Intrinsics.cpp` and `DebugInfo.cpp` and (LLS38) `Module.cpp` by `arbace/repr/llvm/expect.ae` (LLS33 (b)), and `arbace/repr/llvm/intr_table.ae` and `di_table.ae` are generated from its `llvm/include/llvm/IR/Intrinsics*.td`, `DebugInfoFlags.def`, `llvm/include/llvm/BinaryFormat/Dwarf.def`, `Dwarf.h`, `LLParser.cpp` and `AsmWriter.cpp` (`bin/llvm-corpus intrinsics`, `di-table`). The text of `llvm/LICENSE.TXT` of that release follows.

The LLVM Project is under the Apache License v2.0 with LLVM Exceptions:
==============================================================================

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

    APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

    Copyright [yyyy] [name of copyright owner]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


---- LLVM Exceptions to the Apache 2.0 License ----

As an exception, if, as a result of your compiling your source code, portions
of this Software are embedded into an Object form of such source code, you
may redistribute such embedded portions in such Object form without complying
with the conditions of Sections 4(a), 4(b) and 4(d) of the License.

In addition, if you combine or link compiled forms of this Software with
software that is licensed under the GPLv2 ("Combined Software") and if a
court of competent jurisdiction determines that the patent provision (Section
3), the indemnity provision (Section 9) or other Section of the License
conflicts with the conditions of the GPLv2, you may retroactively and
prospectively choose to deem waived or otherwise exclude such Section(s) of
the License, but only in their entirety and only with respect to the Combined
Software.

==============================================================================
Software from third parties included in the LLVM Project:
==============================================================================
The LLVM Project contains third party software which is under different license
terms. All such code will be identified clearly using at least one of two
mechanisms:
1) It will be in a separate directory tree with its own `LICENSE.txt` or
   `LICENSE` file at the top containing the specific license and restrictions
   which apply to that software, or
2) It will contain specific license and restriction terms at the top of every
   file.

==============================================================================
Legacy LLVM License (https://llvm.org/docs/DeveloperPolicy.html#legacy):
==============================================================================
University of Illinois/NCSA
Open Source License

Copyright (c) 2003-2019 University of Illinois at Urbana-Champaign.
All rights reserved.

Developed by:

    LLVM Team

    University of Illinois at Urbana-Champaign

    http://llvm.org

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal with
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimers.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimers in the
      documentation and/or other materials provided with the distribution.

    * Neither the names of the LLVM Team, University of Illinois at
      Urbana-Champaign, nor the names of its contributors may be used to
      endorse or promote products derived from this Software without specific
      prior written permission.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
SOFTWARE.


## gcc's testsuite (the IR of gcc.c-torture/execute in test/llvm/ir/tx.tar.xz): GNU General Public License version 3 or later

`test/llvm/ir/tx.tar.xz` holds clang 23's IR of the files of gcc 9.1.0's `gcc/testsuite/gcc.c-torture/execute/` and its `ieee/`
(as packaged by modernc.org/ccorpus2 v1.6.0, `test/c/corpora.edn`'s `:torture-execute`), compiled as test data by `bin/llvmi ir-make`
(doc/llvm/LLVM-CROP-SURVEY.md, "Decided (2026-11-06, the user: the torture corpora's IR pinned)"); gcc's testsuite is distributed with
gcc under the GNU General Public License version 3 or later (gcc's `COPYING3`). Its text follows, gcc 15.2.0's `COPYING3`, out of the
tarball pinned in `test/c/corpora.edn` (`:gcc-15.2.0`; the same version 3 text gcc 9.1.0 ships; doc/llvm/LLVM-CROP-SURVEY.md's LLS26).
The IR of `test/llvm/ir/c.tar.xz` is made from
Arbace's own test programs (`test/c/synth`, `test/c/run`, `test/cpp/samples`, `test/cpp/runtime/samples`), under Arbace's license.

```
                    GNU GENERAL PUBLIC LICENSE
                       Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

                            Preamble

  The GNU General Public License is a free, copyleft license for
software and other kinds of works.

  The licenses for most software and other practical works are designed
to take away your freedom to share and change the works.  By contrast,
the GNU General Public License is intended to guarantee your freedom to
share and change all versions of a program--to make sure it remains free
software for all its users.  We, the Free Software Foundation, use the
GNU General Public License for most of our software; it applies also to
any other work released this way by its authors.  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
them if you wish), that you receive source code or can get it if you
want it, that you can change the software or use pieces of it in new
free programs, and that you know you can do these things.

  To protect your rights, we need to prevent others from denying you
these rights or asking you to surrender the rights.  Therefore, you have
certain responsibilities if you distribute copies of the software, or if
you modify it: responsibilities to respect the freedom of others.

  For example, if you distribute copies of such a program, whether
gratis or for a fee, you must pass on to the recipients the same
freedoms that you received.  You must make sure that they, too, receive
or can get the source code.  And you must show them these terms so they
know their rights.

  Developers that use the GNU GPL protect your rights with two steps:
(1) assert copyright on the software, and (2) offer you this License
giving you legal permission to copy, distribute and/or modify it.

  For the developers' and authors' protection, the GPL clearly explains
that there is no warranty for this free software.  For both users' and
authors' sake, the GPL requires that modified versions be marked as
changed, so that their problems will not be attributed erroneously to
authors of previous versions.

  Some devices are designed to deny users access to install or run
modified versions of the software inside them, although the manufacturer
can do so.  This is fundamentally incompatible with the aim of
protecting users' freedom to change the software.  The systematic
pattern of such abuse occurs in the area of products for individuals to
use, which is precisely where it is most unacceptable.  Therefore, we
have designed this version of the GPL to prohibit the practice for those
products.  If such problems arise substantially in other domains, we
stand ready to extend this provision to those domains in future versions
of the GPL, as needed to protect the freedom of users.

  Finally, every program is threatened constantly by software patents.
States should not allow patents to restrict development and use of
software on general-purpose computers, but in those that do, we wish to
avoid the special danger that patents applied to a free program could
make it effectively proprietary.  To prevent this, the GPL assures that
patents cannot be used to render the program non-free.

  The precise terms and conditions for copying, distribution and
modification follow.

                       TERMS AND CONDITIONS

  0. Definitions.

  "This License" refers to version 3 of the GNU General Public License.

  "Copyright" also means copyright-like laws that apply to other kinds of
works, such as semiconductor masks.

  "The Program" refers to any copyrightable work licensed under this
License.  Each licensee is addressed as "you".  "Licensees" and
"recipients" may be individuals or organizations.

  To "modify" a work means to copy from or adapt all or part of the work
in a fashion requiring copyright permission, other than the making of an
exact copy.  The resulting work is called a "modified version" of the
earlier work or a work "based on" the earlier work.

  A "covered work" means either the unmodified Program or a work based
on the Program.

  To "propagate" a work means to do anything with it that, without
permission, would make you directly or secondarily liable for
infringement under applicable copyright law, except executing it on a
computer or modifying a private copy.  Propagation includes copying,
distribution (with or without modification), making available to the
public, and in some countries other activities as well.

  To "convey" a work means any kind of propagation that enables other
parties to make or receive copies.  Mere interaction with a user through
a computer network, with no transfer of a copy, is not conveying.

  An interactive user interface displays "Appropriate Legal Notices"
to the extent that it includes a convenient and prominently visible
feature that (1) displays an appropriate copyright notice, and (2)
tells the user that there is no warranty for the work (except to the
extent that warranties are provided), that licensees may convey the
work under this License, and how to view a copy of this License.  If
the interface presents a list of user commands or options, such as a
menu, a prominent item in the list meets this criterion.

  1. Source Code.

  The "source code" for a work means the preferred form of the work
for making modifications to it.  "Object code" means any non-source
form of a work.

  A "Standard Interface" means an interface that either is an official
standard defined by a recognized standards body, or, in the case of
interfaces specified for a particular programming language, one that
is widely used among developers working in that language.

  The "System Libraries" of an executable work include anything, other
than the work as a whole, that (a) is included in the normal form of
packaging a Major Component, but which is not part of that Major
Component, and (b) serves only to enable use of the work with that
Major Component, or to implement a Standard Interface for which an
implementation is available to the public in source code form.  A
"Major Component", in this context, means a major essential component
(kernel, window system, and so on) of the specific operating system
(if any) on which the executable work runs, or a compiler used to
produce the work, or an object code interpreter used to run it.

  The "Corresponding Source" for a work in object code form means all
the source code needed to generate, install, and (for an executable
work) run the object code and to modify the work, including scripts to
control those activities.  However, it does not include the work's
System Libraries, or general-purpose tools or generally available free
programs which are used unmodified in performing those activities but
which are not part of the work.  For example, Corresponding Source
includes interface definition files associated with source files for
the work, and the source code for shared libraries and dynamically
linked subprograms that the work is specifically designed to require,
such as by intimate data communication or control flow between those
subprograms and other parts of the work.

  The Corresponding Source need not include anything that users
can regenerate automatically from other parts of the Corresponding
Source.

  The Corresponding Source for a work in source code form is that
same work.

  2. Basic Permissions.

  All rights granted under this License are granted for the term of
copyright on the Program, and are irrevocable provided the stated
conditions are met.  This License explicitly affirms your unlimited
permission to run the unmodified Program.  The output from running a
covered work is covered by this License only if the output, given its
content, constitutes a covered work.  This License acknowledges your
rights of fair use or other equivalent, as provided by copyright law.

  You may make, run and propagate covered works that you do not
convey, without conditions so long as your license otherwise remains
in force.  You may convey covered works to others for the sole purpose
of having them make modifications exclusively for you, or provide you
with facilities for running those works, provided that you comply with
the terms of this License in conveying all material for which you do
not control copyright.  Those thus making or running the covered works
for you must do so exclusively on your behalf, under your direction
and control, on terms that prohibit them from making any copies of
your copyrighted material outside their relationship with you.

  Conveying under any other circumstances is permitted solely under
the conditions stated below.  Sublicensing is not allowed; section 10
makes it unnecessary.

  3. Protecting Users' Legal Rights From Anti-Circumvention Law.

  No covered work shall be deemed part of an effective technological
measure under any applicable law fulfilling obligations under article
11 of the WIPO copyright treaty adopted on 20 December 1996, or
similar laws prohibiting or restricting circumvention of such
measures.

  When you convey a covered work, you waive any legal power to forbid
circumvention of technological measures to the extent such circumvention
is effected by exercising rights under this License with respect to
the covered work, and you disclaim any intention to limit operation or
modification of the work as a means of enforcing, against the work's
users, your or third parties' legal rights to forbid circumvention of
technological measures.

  4. Conveying Verbatim Copies.

  You may convey verbatim copies of the Program's source code as you
receive it, in any medium, provided that you conspicuously and
appropriately publish on each copy an appropriate copyright notice;
keep intact all notices stating that this License and any
non-permissive terms added in accord with section 7 apply to the code;
keep intact all notices of the absence of any warranty; and give all
recipients a copy of this License along with the Program.

  You may charge any price or no price for each copy that you convey,
and you may offer support or warranty protection for a fee.

  5. Conveying Modified Source Versions.

  You may convey a work based on the Program, or the modifications to
produce it from the Program, in the form of source code under the
terms of section 4, provided that you also meet all of these conditions:

    a) The work must carry prominent notices stating that you modified
    it, and giving a relevant date.

    b) The work must carry prominent notices stating that it is
    released under this License and any conditions added under section
    7.  This requirement modifies the requirement in section 4 to
    "keep intact all notices".

    c) You must license the entire work, as a whole, under this
    License to anyone who comes into possession of a copy.  This
    License will therefore apply, along with any applicable section 7
    additional terms, to the whole of the work, and all its parts,
    regardless of how they are packaged.  This License gives no
    permission to license the work in any other way, but it does not
    invalidate such permission if you have separately received it.

    d) If the work has interactive user interfaces, each must display
    Appropriate Legal Notices; however, if the Program has interactive
    interfaces that do not display Appropriate Legal Notices, your
    work need not make them do so.

  A compilation of a covered work with other separate and independent
works, which are not by their nature extensions of the covered work,
and which are not combined with it such as to form a larger program,
in or on a volume of a storage or distribution medium, is called an
"aggregate" if the compilation and its resulting copyright are not
used to limit the access or legal rights of the compilation's users
beyond what the individual works permit.  Inclusion of a covered work
in an aggregate does not cause this License to apply to the other
parts of the aggregate.

  6. Conveying Non-Source Forms.

  You may convey a covered work in object code form under the terms
of sections 4 and 5, provided that you also convey the
machine-readable Corresponding Source under the terms of this License,
in one of these ways:

    a) Convey the object code in, or embodied in, a physical product
    (including a physical distribution medium), accompanied by the
    Corresponding Source fixed on a durable physical medium
    customarily used for software interchange.

    b) Convey the object code in, or embodied in, a physical product
    (including a physical distribution medium), accompanied by a
    written offer, valid for at least three years and valid for as
    long as you offer spare parts or customer support for that product
    model, to give anyone who possesses the object code either (1) a
    copy of the Corresponding Source for all the software in the
    product that is covered by this License, on a durable physical
    medium customarily used for software interchange, for a price no
    more than your reasonable cost of physically performing this
    conveying of source, or (2) access to copy the
    Corresponding Source from a network server at no charge.

    c) Convey individual copies of the object code with a copy of the
    written offer to provide the Corresponding Source.  This
    alternative is allowed only occasionally and noncommercially, and
    only if you received the object code with such an offer, in accord
    with subsection 6b.

    d) Convey the object code by offering access from a designated
    place (gratis or for a charge), and offer equivalent access to the
    Corresponding Source in the same way through the same place at no
    further charge.  You need not require recipients to copy the
    Corresponding Source along with the object code.  If the place to
    copy the object code is a network server, the Corresponding Source
    may be on a different server (operated by you or a third party)
    that supports equivalent copying facilities, provided you maintain
    clear directions next to the object code saying where to find the
    Corresponding Source.  Regardless of what server hosts the
    Corresponding Source, you remain obligated to ensure that it is
    available for as long as needed to satisfy these requirements.

    e) Convey the object code using peer-to-peer transmission, provided
    you inform other peers where the object code and Corresponding
    Source of the work are being offered to the general public at no
    charge under subsection 6d.

  A separable portion of the object code, whose source code is excluded
from the Corresponding Source as a System Library, need not be
included in conveying the object code work.

  A "User Product" is either (1) a "consumer product", which means any
tangible personal property which is normally used for personal, family,
or household purposes, or (2) anything designed or sold for incorporation
into a dwelling.  In determining whether a product is a consumer product,
doubtful cases shall be resolved in favor of coverage.  For a particular
product received by a particular user, "normally used" refers to a
typical or common use of that class of product, regardless of the status
of the particular user or of the way in which the particular user
actually uses, or expects or is expected to use, the product.  A product
is a consumer product regardless of whether the product has substantial
commercial, industrial or non-consumer uses, unless such uses represent
the only significant mode of use of the product.

  "Installation Information" for a User Product means any methods,
procedures, authorization keys, or other information required to install
and execute modified versions of a covered work in that User Product from
a modified version of its Corresponding Source.  The information must
suffice to ensure that the continued functioning of the modified object
code is in no case prevented or interfered with solely because
modification has been made.

  If you convey an object code work under this section in, or with, or
specifically for use in, a User Product, and the conveying occurs as
part of a transaction in which the right of possession and use of the
User Product is transferred to the recipient in perpetuity or for a
fixed term (regardless of how the transaction is characterized), the
Corresponding Source conveyed under this section must be accompanied
by the Installation Information.  But this requirement does not apply
if neither you nor any third party retains the ability to install
modified object code on the User Product (for example, the work has
been installed in ROM).

  The requirement to provide Installation Information does not include a
requirement to continue to provide support service, warranty, or updates
for a work that has been modified or installed by the recipient, or for
the User Product in which it has been modified or installed.  Access to a
network may be denied when the modification itself materially and
adversely affects the operation of the network or violates the rules and
protocols for communication across the network.

  Corresponding Source conveyed, and Installation Information provided,
in accord with this section must be in a format that is publicly
documented (and with an implementation available to the public in
source code form), and must require no special password or key for
unpacking, reading or copying.

  7. Additional Terms.

  "Additional permissions" are terms that supplement the terms of this
License by making exceptions from one or more of its conditions.
Additional permissions that are applicable to the entire Program shall
be treated as though they were included in this License, to the extent
that they are valid under applicable law.  If additional permissions
apply only to part of the Program, that part may be used separately
under those permissions, but the entire Program remains governed by
this License without regard to the additional permissions.

  When you convey a copy of a covered work, you may at your option
remove any additional permissions from that copy, or from any part of
it.  (Additional permissions may be written to require their own
removal in certain cases when you modify the work.)  You may place
additional permissions on material, added by you to a covered work,
for which you have or can give appropriate copyright permission.

  Notwithstanding any other provision of this License, for material you
add to a covered work, you may (if authorized by the copyright holders of
that material) supplement the terms of this License with terms:

    a) Disclaiming warranty or limiting liability differently from the
    terms of sections 15 and 16 of this License; or

    b) Requiring preservation of specified reasonable legal notices or
    author attributions in that material or in the Appropriate Legal
    Notices displayed by works containing it; or

    c) Prohibiting misrepresentation of the origin of that material, or
    requiring that modified versions of such material be marked in
    reasonable ways as different from the original version; or

    d) Limiting the use for publicity purposes of names of licensors or
    authors of the material; or

    e) Declining to grant rights under trademark law for use of some
    trade names, trademarks, or service marks; or

    f) Requiring indemnification of licensors and authors of that
    material by anyone who conveys the material (or modified versions of
    it) with contractual assumptions of liability to the recipient, for
    any liability that these contractual assumptions directly impose on
    those licensors and authors.

  All other non-permissive additional terms are considered "further
restrictions" within the meaning of section 10.  If the Program as you
received it, or any part of it, contains a notice stating that it is
governed by this License along with a term that is a further
restriction, you may remove that term.  If a license document contains
a further restriction but permits relicensing or conveying under this
License, you may add to a covered work material governed by the terms
of that license document, provided that the further restriction does
not survive such relicensing or conveying.

  If you add terms to a covered work in accord with this section, you
must place, in the relevant source files, a statement of the
additional terms that apply to those files, or a notice indicating
where to find the applicable terms.

  Additional terms, permissive or non-permissive, may be stated in the
form of a separately written license, or stated as exceptions;
the above requirements apply either way.

  8. Termination.

  You may not propagate or modify a covered work except as expressly
provided under this License.  Any attempt otherwise to propagate or
modify it is void, and will automatically terminate your rights under
this License (including any patent licenses granted under the third
paragraph of section 11).

  However, if you cease all violation of this License, then your
license from a particular copyright holder is reinstated (a)
provisionally, unless and until the copyright holder explicitly and
finally terminates your license, and (b) permanently, if the copyright
holder fails to notify you of the violation by some reasonable means
prior to 60 days after the cessation.

  Moreover, your license from a particular copyright holder is
reinstated permanently if the copyright holder notifies you of the
violation by some reasonable means, this is the first time you have
received notice of violation of this License (for any work) from that
copyright holder, and you cure the violation prior to 30 days after
your receipt of the notice.

  Termination of your rights under this section does not terminate the
licenses of parties who have received copies or rights from you under
this License.  If your rights have been terminated and not permanently
reinstated, you do not qualify to receive new licenses for the same
material under section 10.

  9. Acceptance Not Required for Having Copies.

  You are not required to accept this License in order to receive or
run a copy of the Program.  Ancillary propagation of a covered work
occurring solely as a consequence of using peer-to-peer transmission
to receive a copy likewise does not require acceptance.  However,
nothing other than this License grants you permission to propagate or
modify any covered work.  These actions infringe copyright if you do
not accept this License.  Therefore, by modifying or propagating a
covered work, you indicate your acceptance of this License to do so.

  10. Automatic Licensing of Downstream Recipients.

  Each time you convey a covered work, the recipient automatically
receives a license from the original licensors, to run, modify and
propagate that work, subject to this License.  You are not responsible
for enforcing compliance by third parties with this License.

  An "entity transaction" is a transaction transferring control of an
organization, or substantially all assets of one, or subdividing an
organization, or merging organizations.  If propagation of a covered
work results from an entity transaction, each party to that
transaction who receives a copy of the work also receives whatever
licenses to the work the party's predecessor in interest had or could
give under the previous paragraph, plus a right to possession of the
Corresponding Source of the work from the predecessor in interest, if
the predecessor has it or can get it with reasonable efforts.

  You may not impose any further restrictions on the exercise of the
rights granted or affirmed under this License.  For example, you may
not impose a license fee, royalty, or other charge for exercise of
rights granted under this License, and you may not initiate litigation
(including a cross-claim or counterclaim in a lawsuit) alleging that
any patent claim is infringed by making, using, selling, offering for
sale, or importing the Program or any portion of it.

  11. Patents.

  A "contributor" is a copyright holder who authorizes use under this
License of the Program or a work on which the Program is based.  The
work thus licensed is called the contributor's "contributor version".

  A contributor's "essential patent claims" are all patent claims
owned or controlled by the contributor, whether already acquired or
hereafter acquired, that would be infringed by some manner, permitted
by this License, of making, using, or selling its contributor version,
but do not include claims that would be infringed only as a
consequence of further modification of the contributor version.  For
purposes of this definition, "control" includes the right to grant
patent sublicenses in a manner consistent with the requirements of
this License.

  Each contributor grants you a non-exclusive, worldwide, royalty-free
patent license under the contributor's essential patent claims, to
make, use, sell, offer for sale, import and otherwise run, modify and
propagate the contents of its contributor version.

  In the following three paragraphs, a "patent license" is any express
agreement or commitment, however denominated, not to enforce a patent
(such as an express permission to practice a patent or covenant not to
sue for patent infringement).  To "grant" such a patent license to a
party means to make such an agreement or commitment not to enforce a
patent against the party.

  If you convey a covered work, knowingly relying on a patent license,
and the Corresponding Source of the work is not available for anyone
to copy, free of charge and under the terms of this License, through a
publicly available network server or other readily accessible means,
then you must either (1) cause the Corresponding Source to be so
available, or (2) arrange to deprive yourself of the benefit of the
patent license for this particular work, or (3) arrange, in a manner
consistent with the requirements of this License, to extend the patent
license to downstream recipients.  "Knowingly relying" means you have
actual knowledge that, but for the patent license, your conveying the
covered work in a country, or your recipient's use of the covered work
in a country, would infringe one or more identifiable patents in that
country that you have reason to believe are valid.

  If, pursuant to or in connection with a single transaction or
arrangement, you convey, or propagate by procuring conveyance of, a
covered work, and grant a patent license to some of the parties
receiving the covered work authorizing them to use, propagate, modify
or convey a specific copy of the covered work, then the patent license
you grant is automatically extended to all recipients of the covered
work and works based on it.

  A patent license is "discriminatory" if it does not include within
the scope of its coverage, prohibits the exercise of, or is
conditioned on the non-exercise of one or more of the rights that are
specifically granted under this License.  You may not convey a covered
work if you are a party to an arrangement with a third party that is
in the business of distributing software, under which you make payment
to the third party based on the extent of your activity of conveying
the work, and under which the third party grants, to any of the
parties who would receive the covered work from you, a discriminatory
patent license (a) in connection with copies of the covered work
conveyed by you (or copies made from those copies), or (b) primarily
for and in connection with specific products or compilations that
contain the covered work, unless you entered into that arrangement,
or that patent license was granted, prior to 28 March 2007.

  Nothing in this License shall be construed as excluding or limiting
any implied license or other defenses to infringement that may
otherwise be available to you under applicable patent law.

  12. No Surrender of Others' Freedom.

  If conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot convey a
covered work so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you may
not convey it at all.  For example, if you agree to terms that obligate you
to collect a royalty for further conveying from those to whom you convey
the Program, the only way you could satisfy both those terms and this
License would be to refrain entirely from conveying the Program.

  13. Use with the GNU Affero General Public License.

  Notwithstanding any other provision of this License, you have
permission to link or combine any covered work with a work licensed
under version 3 of the GNU Affero General Public License into a single
combined work, and to convey the resulting work.  The terms of this
License will continue to apply to the part which is the covered work,
but the special requirements of the GNU Affero General Public License,
section 13, concerning interaction through a network will apply to the
combination as such.

  14. Revised Versions of this License.

  The Free Software Foundation may publish revised and/or new versions of
the GNU General Public License from time to time.  Such new versions will
be similar in spirit to the present version, but may differ in detail to
address new problems or concerns.

  Each version is given a distinguishing version number.  If the
Program specifies that a certain numbered version of the GNU General
Public License "or any later version" applies to it, you have the
option of following the terms and conditions either of that numbered
version or of any later version published by the Free Software
Foundation.  If the Program does not specify a version number of the
GNU General Public License, you may choose any version ever published
by the Free Software Foundation.

  If the Program specifies that a proxy can decide which future
versions of the GNU General Public License can be used, that proxy's
public statement of acceptance of a version permanently authorizes you
to choose that version for the Program.

  Later license versions may give you additional or different
permissions.  However, no additional obligations are imposed on any
author or copyright holder as a result of your choosing to follow a
later version.

  15. Disclaimer of Warranty.

  THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY
APPLICABLE LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT
HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY
OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM
IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

  16. Limitation of Liability.

  IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS
THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY
GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE
USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF
DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD
PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS),
EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGES.

  17. Interpretation of Sections 15 and 16.

  If the disclaimer of warranty and limitation of liability provided
above cannot be given local legal effect according to their terms,
reviewing courts shall apply local law that most closely approximates
an absolute waiver of all civil liability in connection with the
Program, unless a warranty or assumption of liability accompanies a
copy of the Program in return for a fee.

                     END OF TERMS AND CONDITIONS

            How to Apply These Terms to Your New Programs

  If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

  To do so, attach the following notices to the program.  It is safest
to attach them to the start of each source file to most effectively
state the exclusion of warranty; and each file should have at least
the "copyright" line and a pointer to where the full notice is found.

    <one line to give the program's name and a brief idea of what it does.>
    Copyright (C) <year>  <name of author>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

Also add information on how to contact you by electronic and paper mail.

  If the program does terminal interaction, make it output a short
notice like this when it starts in an interactive mode:

    <program>  Copyright (C) <year>  <name of author>
    This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c' for details.

The hypothetical commands `show w' and `show c' should show the appropriate
parts of the General Public License.  Of course, your program's commands
might be different; for a GUI interface, you would use an "about box".

  You should also get your employer (if you work as a programmer) or school,
if any, to sign a "copyright disclaimer" for the program, if necessary.
For more information on this, and how to apply and follow the GNU GPL, see
<http://www.gnu.org/licenses/>.

  The GNU General Public License does not permit incorporating your program
into proprietary programs.  If your program is a subroutine library, you
may consider it more useful to permit linking proprietary applications with
the library.  If this is what you want to do, use the GNU Lesser General
Public License instead of this License.  But first, please read
<http://www.gnu.org/philosophy/why-not-lgpl.html>.
```
## Rust (rustc 1.99.1): MIT License or Apache License 2.0

The Rust project's sources are dual-licensed under the MIT License or the Apache License 2.0, at
the licensee's option. The MIT License's text, as the Rust project's crates carry it (the
`LICENSE-MIT` of rustc-literal-escaper 0.0.8, the `.crate` above, whole):

```
Permission is hereby granted, free of charge, to any
person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the
Software without restriction, including without
limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions
of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
```

The Apache License 2.0's text (the escaper's `LICENSE-APACHE`, the same terms) is given in full in
the section "Murmur3 (Guava): Apache License 2.0" above.
## bytestring: BSD 3-Clause License

The license of the Haskell package bytestring (libraries/bytestring/LICENSE of GHC 9.14.1's source tarball), which covers hsi's ports of its cbits:

```
Copyright (c) Don Stewart 2005-2009
          (c) Duncan Coutts 2006-2015
          (c) David Roundy 2003-2005
          (c) Simon Meier 2010-2011
          (c) Koz Ross 2021

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the author nor the names of his contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
```

## text: BSD 2-Clause License

The license of the Haskell package text (libraries/text/LICENSE of GHC 9.14.1's source tarball), which covers hsi's ports of its cbits:

```
Copyright (c) 2008-2009, Tom Harper
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

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

## The Hackage packages of Haskell's eval corpora: the Glasgow Haskell Compiler License and BSD 3-Clause Licenses

Each package's `LICENSE`, from its pinned tarball (`test/haskell/hackage.edn`).

old-locale 1.0.0.7, old-time 1.1.1.0 and random 1.3.1 (the same text in each):

```
This library (libraries/base) is derived from code from two
sources: 

  * Code from the GHC project which is largely (c) The University of
    Glasgow, and distributable under a BSD-style license (see below),

  * Code from the Haskell 98 Report which is (c) Simon Peyton Jones
    and freely redistributable (but see the full license for
    restrictions).

The full text of these licenses is reproduced below.  Both of the
licenses are BSD-style or compatible.

-----------------------------------------------------------------------------

The Glasgow Haskell Compiler License

Copyright 2004, The University Court of the University of Glasgow. 
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.
 
- Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.
 
- Neither name of the University nor the names of its contributors may be
used to endorse or promote products derived from this software without
specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY COURT OF THE UNIVERSITY OF
GLASGOW AND THE CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
UNIVERSITY COURT OF THE UNIVERSITY OF GLASGOW OR THE CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
DAMAGE.

-----------------------------------------------------------------------------

Code derived from the document "Report on the Programming Language
Haskell 98", is distributed under the following license:

  Copyright (c) 2002 Simon Peyton Jones

  The authors intend this Report to belong to the entire Haskell
  community, and so we grant permission to copy and distribute it for
  any purpose, provided that it is reproduced in its entirety,
  including this Notice.  Modified versions of this Report may also be
  copied and distributed for any purpose, provided that the modified
  version is clearly presented as such, and that it does not claim to
  be a definition of the Haskell 98 Language.

-----------------------------------------------------------------------------
```

parallel 3.3.0.0:

```
This library (libraries/parallel) is derived from code from
the GHC project which is largely (c) The University of
Glasgow, and distributable under a BSD-style license (see below).

-----------------------------------------------------------------------------

The Glasgow Haskell Compiler License

Copyright 2004, The University Court of the University of Glasgow. 
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.
 
- Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.
 
- Neither name of the University nor the names of its contributors may be
used to endorse or promote products derived from this software without
specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY COURT OF THE UNIVERSITY OF
GLASGOW AND THE CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
UNIVERSITY COURT OF THE UNIVERSITY OF GLASGOW OR THE CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
DAMAGE.

-----------------------------------------------------------------------------
```

splitmix 0.1.3.2:

```
Copyright (c) 2017, Oleg Grenrus

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Oleg Grenrus nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

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

primitive 0.9.1.0:

```
Copyright (c) 2008-2009, Roman Leshchinskiy
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.
 
- Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.
 
- Neither name of the University nor the names of its contributors may be
used to endorse or promote products derived from this software without
specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY COURT OF THE UNIVERSITY OF
GLASGOW AND THE CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
UNIVERSITY COURT OF THE UNIVERSITY OF GLASGOW OR THE CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
DAMAGE.
```

unboxed-ref 0.4.0.0:

```
Copyright (c) 2017, winter

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of winter nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

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

## Hamcrest: BSD License

```
BSD License

Copyright (c) 2000-2006, www.hamcrest.org
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of
conditions and the following disclaimer. Redistributions in binary form must reproduce
the above copyright notice, this list of conditions and the following disclaimer in
the documentation and/or other materials provided with the distribution.

Neither the name of Hamcrest nor the names of its contributors may be used to endorse
or promote products derived from this software without specific prior written
permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
DAMAGE.
```
