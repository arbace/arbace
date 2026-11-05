# Python as the tenth crop: survey and plan

Status: initial survey and plan, 2026-11-03, at the user's request (doc/JOURNAL.md, "2026-11-03: Haskell and Rust
unparked; Python's survey"). Nothing here is adopted: no code under `arbace/`, `go/`, `bin/`, `tools/` or `test/`
changed, and only this file was written. The decisions are the user's, as proposals PYS1-PYS20 (§12); the
milestones are PY1-PY11 (§11), named by PYS2.

Base: branch `python-survey` from main `422b7960`. The work is reading (the crop surveys, CLAUDE.md, GATE-SURVEY.md's
GT8) and probes of a few seconds each with the host's Python 3.14.10 (`ast`, `tokenize`, `symtable`, `dis`,
`marshal`, `compile` on files of a few lines and on the host's installed stdlib; scratch in the worktree's
`.tmp/python/`, Appendix A). No Arbace executable was built or run, no lock taken, nothing installed, nothing
downloaded. What the host does not hold (CPython's source tarball, `Lib/test`, pyperformance) is named from memory
of the projects, every such size, URL or license marked **to verify**, verified at the fetch the user decides (PYS1).

Words used throughout:

- **Python forms**: Python modules written as Arbace forms (§4), the counterpart of Go forms, Java forms, C forms
  and OCaml forms.
- **repr/python**: the bridge between Python text and Python forms (`arbace.repr.python`, `bin/repr-python read`,
  `print`, `roundtrip`, `check`), as repr/go, repr/c and repr/ocaml are.
- **pyi**: the evaluator of Python forms at the REPL and for programs (the name is PYS2's), as goi is Go's, oi
  OCaml's, si Scheme's, ci C's.
- **The oracle**: the host's CPython 3.14.10 (Alpine `python3-3.14.10-r1`), through a helper script
  `tools/pydump` (§5) that dumps what its `ast`, `tokenize`, `symtable` and `compile` see. **The judge** of runs is
  the same CPython, its runs recorded as data (§8.1).
- **The IR**: pyi's own intermediate form (§6.3), compiled by Arbace from Python forms, executed by pyi's frame
  machine and translated by the AOT (§7). It is Arbace's, not CPython's bytecode.

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 Measurements · 4 Python forms ·
5 repr/python: read and print · 6 eval: pyi · 7 AOT · 8 The oracle, the references and the corpora · 9 Licenses ·
10 The row and GT8's budget · 11 Plan (PY1-PY11) · 12 Proposals (PYS1-PYS20) · 13 What to decide first · 14 Risks ·
15 Sources · Appendix A: the scratch.

## Summary

- **Python is the first crop whose meaning is (almost) in its syntax.** Unlike OCaml (types, OC4), C++ (Sema,
  CP4) or Haskell, a Python program's semantics needs no type checker: the AST plus the scope analysis (`symtable`:
  local, global, nonlocal, free, cell; about 3,000 lines of C in CPython, to verify) decide every name, and
  everything else happens at run time through the data model. So **eval and AOT need no oracle dumps**: pyi
  can run from the forms alone, Arbace's own scope analysis checked against the oracle's `symtable` (§6.2). The
  oracle is needed for read (the parser) and as the reference of runs, and a native parser (§5.5) removes it from
  read too: Python can be the first crop that is standalone at every level in its first round.
- **The difficulty is breadth, not depth.** The data model (about 100 special methods, descriptors, metaclasses,
  the C3 MRO), the builtin types' surface (`str` alone has 47 methods), generators, coroutines and async
  generators, exception groups, `match`, f-strings and 3.14's t-strings, the import system, and a standard library
  of 595 Python modules (300,465 lines on the host, E2) over about 110 C modules (37 built in, 72 shared objects,
  E2). CPython's own test suite tests CPython's implementation details (reference counts, `gc`, the C API).
- **The host has everything for the oracle and nothing to install.** `python3` 3.14.10 (GIL build, `siphash13`,
  Unicode 16.0.0), with the stdlib as source (`/usr/lib/python3.14/*.py`), parses that stdlib in 2.8 s (1,082,411
  nodes, E2). `tokenize`'s full token stream round-trips byte for byte (80 of 80 stdlib files, E5), so mode
  `:full` needs no lexer of Arbace's at first, unlike OCaml's (OCAML-SURVEY §5.3). `compile()` is deterministic and
  `marshal` of a code object compiled from text equals that of the same AST (E7): a print level as strong as
  javac's class files byte for byte. `Lib/test` is the apk `python3-tests` (158 MiB installed, not installed) or the
  source tarball (PYS1).
- **Much of the stdlib's C has pure-Python twins in the stdlib itself** (E8): `_pyio` for `_io`, `_pydecimal` for
  `_decimal`, `_pydatetime` for `_datetime`, `_pylong`, `_py_abc`, `_py_warnings`, `re`'s compiler (only the
  matcher `_sre` is C), `json`'s and `pickle`'s pure paths, `importlib._bootstrap` for the import system. Read as
  Python forms, they shrink the Go ports to a list of about 25 modules (§6.6), as Scheme's crop runs psyntax and
  OCaml's runs its stdlib.
- **Recommended shape.** Python forms after the `ast` module's node names (PYS5); repr/python on the helper
  `tools/pydump` first, a native reader generated from CPython's own `Grammar/python.gram` by an Arbace port of
  pegen later (PYS3, PYS4); pyi as an IR compiled by Arbace and run by a frame machine in Go with heap frames, so
  generators and coroutines are suspended frames (PYS7); its runtime `go/arbace/py` in hand-written Go forms
  (PYS8); tracing GC with refcount timing a design class (PYS9); a GIL (PYS10); AOT as the IR translated to Go
  forms, a compiled interpreter at first, 1.5 to 3 times pyi (PYS13).
- **Plan (§11): PY1-PY11, about 57 to 98 working days**, of which the native reader (PY9) is 8 to 15; read and print
  alone (PY1-PY3, 9 to 14 days) give a round trip with no runtime commitment. The long poles are pyi's object model
  (PY5) and the stdlib's ports (PY6), as goi's GR3-GR10 and C5 were.
- **GT8 from day one (§10):** synthetic corpora by grammar rule and by special method, coverage subsets of
  `Lib/test` keyed per test method, and a measured cost per level before any corpus enters a tier. `Lib/test` whole
  under an interpreter on Go is far over 35 minutes a night (§10.2).
- **To decide first (§13):** the pin (PYS1), the oracle and the native reader's order (PYS3, PYS4), eval's
  representation (PYS7), finalization (PYS9), the stdlib's strategy (PYS11), the crop's place in the order
  (PYS19).

## 1. What parity means

Under the crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions" and "REAP v1's divergences"; C23-SURVEY
§1; OCAML-SURVEY §1): on the host (linux/amd64), forms only, each level's results recorded as data, `bin/reap`
judging from the records with no runs, none failing, every non-pass in a recorded class.

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **Python read** | repr/python `read` (the helper's dump converted; later the native reader, PY9) | the stdlib, `Lib/test`, the site-packages set, the samples, synthetic (§8.2) | CPython 3.14.10's acceptance (`ast.parse`), its `SyntaxError` (class, message, position) and its `ast` (`ast.dump` with attributes) | trees equal to the helper's as data; the print compiled by CPython gives the original's code object (E7); a rejected file rejected with the same message and position |
| **Python eval** | pyi (the IR on the frame machine) | the run programs: synthetic, `Lib/test` per test method, benchmarks | CPython 3.14.10's runs of the originals, recorded (status, stdout, stderr's last line; §8.1) | byte for byte after the recorded masks (addresses); per test method for `Lib/test` |
| **Python AOT** | the IR → Go forms → `arbace.build.go` (PYS13) | as eval | as eval | as eval; pyi and compiled compared (diffcheck's kind) |
| **Python print** | repr/python `print` (`:lines`, `:full`) | every read corpus | the original text; the forms read back; CPython's AST and code objects of the print | `:full` byte for byte; `:lines` forms → Python → forms equal, `ast.dump` equal, code objects equal without positions (E7) |

What differs from the other crops:

- **No semantic oracle for eval.** The read level needs CPython's parser until PY9; eval and AOT need only CPython's
  recorded runs. The scope analysis is Arbace's, checked against `symtable` (PY5).
- **Two tiers of rejection.** `ast.parse` accepts what the compiler then rejects (E6: `case [1, *rest] | {"k": _}`
  parses; `compile` raises "alternative patterns bind different names"; `return` outside a function, `nonlocal`
  errors, `await` outside `async`, likewise). The read level judges `ast.parse`'s acceptance; the compiler's and
  `symtable`'s errors are eval's (pyi's compiler reports them before a run, as CPython does), judged as runs (the
  `SyntaxError` at compile time is a run's status and stderr).
- **CPython's own suite is a test of CPython.** `Lib/test` uses `@cpython_only`, `support.check_impl_detail()`,
  `sys.getrefcount`, `gc.get_referrers`, `_testcapi` and C-level modules; PyPy runs it with
  `sys.implementation.name == 'pypy'` and a skip list (to verify). pyi does the same (PYS15).

## 2. The host and prior art

### 2.1 The host

- `python3-3.14.10-r1` (Alpine edge main, `PSF-2.0`, 22 MiB installed): `Python 3.14.10 (main, Oct 27 2026,
  09:53:04) [GCC 15.2.0]`, musl, **the GIL build** (`sys._is_gil_enabled()` true), `hash_info.algorithm` `siphash13`,
  `unicodedata.unidata_version` 16.0.0, `int_max_str_digits` 4300. `python3-pyc` installed.
- Available, not installed: `python3-tests` (158 MiB installed: `Lib/test`), `python3-dev` (101 MiB: headers, the
  static library, `config/`), `python3-doc` (22 KiB, a stub). No `Lib/test`, no `Grammar/`, no `Parser/`, no
  `Tools/peg_generator` on the host: those need the source tarball (PYS1).
- `/usr/lib/python3.14/site-packages`: Alpine's `py3-*` packages, 1,180 `.py` files, 461,735 lines, 4 shared
  objects (`attrs`, `certifi`, `charset_normalizer`, `idna`, `jinja2`, `jsonschema`, `packaging`, `pip`,
  `pyparsing`, `requests`, `setuptools`, `urllib3`, `yaml` with `_yaml`, `typing_extensions`, `wcwidth`, `pyte`,
  `coverage`, `markupsafe`, `cloudinit`, ...): a read corpus already on the host, pinned by the apks' versions (§8.2).

### 2.2 What 3.14 adds (the facts the forms must carry; to verify against the release notes)

- PEP 750 template strings: `t"..."` gives `TemplateStr` with `Interpolation` nodes (present in the probe's `ast`,
  E3); mixing them with plain literals in an implicit concatenation is a syntax error (E6).
- PEP 758: `except A, B:` without parentheses, the same tree as `except (A, B):` (E4).
- PEP 649 / PEP 749: annotations evaluated lazily (`__annotate__`, `annotationlib`): eval's semantics, not the tree's.
- PEP 765: `return`/`break`/`continue` leaving a `finally` block warns (`SyntaxWarning`): a compile-time warning
  on stderr, judged as runs.
- PEP 779: the free-threaded build supported (not the host's); PEP 734: `concurrent.interpreters`; PEP 784:
  `compression.zstd` (not among the host's shared objects); the incremental GC; the tail-calling interpreter and
  the experimental JIT (implementation details, invisible to the judge).
- Since 3.12: PEP 701 (f-strings parsed by the grammar, tokenized in a mode of their own: the hardest part of a
  native lexer, §5.5), PEP 695 (type parameters, `type X = ...`), PEP 709 (comprehensions inlined: visible to
  `symtable`, to tracebacks and to `locals()`).

### 2.3 Prior art (from memory, to verify)

- **CPython** (github.com/python/cpython, tag `v3.14.10`): `Grammar/python.gram` (the PEG grammar, of the order of
  1,500 lines, with `invalid_*` rules that give the error messages in a second pass), `Parser/` (the generated
  `parser.c`, `action_helpers.c`, the lexer `Parser/lexer/` and `Parser/tokenizer/`), `Tools/peg_generator/` (pegen,
  in Python, with C and Python back ends), `Python/symtable.c`, `Python/compile.c`, `Python/codegen.c`,
  `Python/flowgraph.c`, `Python/ceval.c` with `Python/bytecodes.c`, `Objects/` (the types), `Modules/`, `Lib/`.
- **gpython** (github.com/go-python/gpython, BSD-3-Clause): a Python 3.4 interpreter in Go (frames, a bytecode VM
  compatible with 3.4's, a partial stdlib). The closest design in Go; too old to vendor, a reference for the
  object model's Go shape.
- **starlark-go** (github.com/google/starlark-go, BSD-3-Clause): a Python dialect in Go (no classes, no
  exceptions); a reference for a tree compiler and a small VM in Go.
- **grumpy** (github.com/google/grumpy, Apache-2.0, abandoned): Python 2.7 transpiled to Go: the AOT idea of
  §7 (generic ops called from generated Go), with its known limits (no `exec`/`eval` of new code).
- **PyPy** (MIT): `lib_pypy/` holds pure-Python versions of C modules (`_functools`, `_collections`, `_csv`,
  `_structseq`, `_sqlite3` over cffi, ...), a vendoring source for §6.6; and its way of running `Lib/test`
  (`sys.implementation`, `check_impl_detail`).
- **RustPython** (MIT): a Python 3 interpreter in Rust with a compiler of its own and a partial stdlib; a
  reference for which C modules a non-CPython must provide first.
- **MicroPython** (MIT), **GraalPy** (UPL), **Jython** (PSF, 2.7), **IronPython** (Apache-2.0): references only.
- **hypothesmith** (MPL-2.0): programs generated from the grammar, prior art for §8.3's synthetic corpus.

### 2.4 What Arbace already has

- The repr family's helper pattern: `tools/hsdump` (GHC), `tools/rsdump` (rustc), `tools/ocdump` (compiler-libs),
  `tools/ccdump`, `tools/cxxdump`, `tools/schemedump`; for Python the helper is a script on the host's interpreter,
  with no build (§5.1).
- repr/c's native lexer, preprocessor and parser (ports checked token for token and tree for tree,
  `bin/repr-c parse-check`), repr/java's JN8 parser checked with 8,000 mutants for error recovery, repr/go's GP5: the
  pattern of PY9.
- goi's check machinery (`--record-ref`, `divergences`, controls, CPU limits `bin/lib/limit.bash`, essential
  subsets), oi's and si's runtimes in Go forms (`go/arbace/oi`, `go/arbace/scm`) with their AOT (o2j; Scheme's IR
  → Go forms, SC9 (a)), si's heap frames (SCHEME-SURVEY §5.2 (a)), OCaml's effects by goroutine hand-off (OC6).
- ci's runtime `go/arbace/crt` (libc's system calls in Go) and musl 1.2.6 as C forms with cf2g, and jrt's zlib ports
  (`Deflater`, `Inflater`), `x509roots/`: bases for `posix`, `math` (musl's libm: the host CPython's `math` is musl's,
  so bit-identical results come from the same C), `zlib`, `_ssl` (§6.6).

## 3. Measurements

Probes (seconds each; Appendix A):

- **E1, the host.** `python3 -VV`, `apk info`, `sys.flags`, `sys.hash_info`: as §2.1.
- **E2, the stdlib on the host.** 595 `.py` files outside `site-packages`, 300,465 lines, 11.0 MB; all parsed by
  `ast.parse` in 2.8 s, 1,082,411 nodes, no syntax error. 37 built-in modules (`_abc`, `_ast`, `_codecs`,
  `_collections`, `_contextvars`, `_datetime`, `_functools`, `_imp`, `_io`, `_locale`, `_opcode`, `_operator`,
  `_signal`, `_sre`, `_stat`, `_string`, `_suggestions`, `_symtable`, `_sysconfig`, `_thread`, `_tokenize`,
  `_tracemalloc`, `_types`, `_typing`, `_warnings`, `_weakref`, `atexit`, `builtins`, `errno`, `faulthandler`, `gc`,
  `itertools`, `marshal`, `posix`, `pwd`, `sys`, `time`) and 72 shared objects in `lib-dynload` (14 of them test
  modules: `_testcapi`, `_testinternalcapi`, `xxlimited`, ...).
- **E3, the `ast` module.** 132 node classes, 119 leaves, 6 of them deprecated aliases (`AugLoad`, `AugStore`,
  `ExtSlice`, `Index`, `Param`, `Suite`): about 113 live node kinds, of which 37 are operator and context
  singletons. Smaller than OCaml's Parsetree and Go's `go/ast` together.
- **E4, the tree is lossy.** On `x = (1 + 2) * 0x1F  # trailing`: the comment gone; the parentheses gone (the inner
  `BinOp` spans columns 5-10, the outer 4-18: parentheses survive only as a gap between positions); `0x1F` is
  `Constant(31)` (the spelling recoverable from the positions); `'a' "b" f"{x!r:>{w}}"` one `JoinedStr`; `except
  ValueError, TypeError:` the same tree as the parenthesized form; a backslash continuation and `;` invisible.
  `ast.unparse` re-spells all of them (`f'ab{x!r:>{w}}'`, `except (ValueError, TypeError):`). Column offsets are
  **UTF-8 byte offsets** (`y = é`: 15 to 17).
- **E5, tokens round-trip.** `tokenize.untokenize` of the full token stream (5-tuples, `COMMENT` and `NL`
  included) equals the source bytes on a file with non-ASCII names, `;`, a backslash continuation and a bracket
  continuation, and on 80 of 80 stdlib files (0.58 s).
- **E6, two tiers of rejection.** `ast.parse` rejects `'a' t"t{x}"` ("cannot mix t-string literals with string or
  bytes literals", with a caret span) and accepts `case [1, *rest] | {"k": _} if rest:` which `compile` rejects
  ("alternative patterns bind different names").
- **E7, code objects as a print level.** `marshal.dumps(compile(src))` equals `marshal.dumps(compile(ast.parse(src)))`
  (1,379 bytes) and two compiles are equal: compilation is deterministic. The source `ast.unparse` prints gives an
  equal `ast.dump` and code objects equal without positions (`co_code`, constants, names, local names,
  recursively), but different `marshal` bytes (the line and column table). So: `:lines` judged at code without
  positions, `:full` at `marshal` bytes. Bytecode is 3.14's own (`LOAD_FAST_BORROW`, `LOAD_SMALL_INT`): a reason
  not to interpret it (PYS7).
- **E8, pure-Python twins.** `_pyio` 2,764 lines, `_pydecimal` 6,503, `_pydatetime` 2,734, `_pylong` 729, `_py_abc` 152,
  `_py_warnings` 869, `re/_compiler.py` 782 and `re/_parser.py` 1,066, `importlib/_bootstrap.py` 1,570 and
  `_bootstrap_external.py` 1,566. Module kinds: `collections`, `functools`, `operator`, `json`, `re`, `io`,
  `decimal`, `datetime`, `heapq`, `bisect`, `random`, `struct`, `pickle`, `enum`, `dataclasses`, `typing`, `abc`,
  `copy`, `unittest`, `asyncio`, `contextlib`, `weakref`, `hashlib`, `os` are `.py` (some over a C accelerator);
  `itertools`, `time` built in; `math`, `array`, `unicodedata` shared objects.
- **E9, hash order.** `list({'apple','pear','fig','kiwi','plum'})` is the same under `PYTHONHASHSEED=0` twice, another
  order under seed 1 and another unseeded: recorded runs need a fixed seed and pyi needs CPython's `siphash13` with
  the same key derivation to print sets of strings in the same order (§8.1). `object()`'s repr carries an address
  (`<object object at 0x7fd3b3644200>`): masked, as Java's identity hash codes are.

## 4. Python forms

### 4.1 Principles

The other crops' (doc/go/SPEC.md, doc/ocaml/SPEC.md §1): data readable by the Clojure reader, one form per node,
`(head field...)` with an optional trailing metadata map, keywords for flags, vectors for lists, positions in mode
`:full` only, names spelled as the source. The heads are the `ast` classes' names in Arbace's spelling (PYS5 (a)):
`FunctionDef` → `function-def`, `AsyncFor` → `async-for`, `BinOp` → `bin-op`, `MatchClass` → `match-class`,
`TemplateStr` → `template-str`; the operator singletons are symbols (`+`, `//`, `@`, `**`, `not in`, `is not`,
`and`), so the converter is near the identity, as OCaml's is with the Parsetree (OC8). One top form per file:
`(python/module ...)`; the other `ast.parse` modes `(python/interactive ...)` (the REPL), `(python/expression ...)`
(`eval`), `(python/function-type ...)` (type comments).

### 4.2 What the forms keep and drop

- **Dropped and checked:** `ctx` (`Load`, `Store`, `Del`): the slot decides it; the converter checks the helper's
  value against the slot's, so nothing is lost (a test, not a field).
- **Kept beyond the `ast`:** the literal's spelling (`0x1F`, `1_000`, `1e3`, `'it''s'` concatenations, `r"\d"`, the
  string prefixes and quotes) always, as OCaml's forms keep `Pconst_integer`'s text (its SPEC's S6): the value
  is computed from the spelling by the reader, and the helper checks the two agree. Implicit concatenation keeps its
  parts (`(str-concat "a" "b" (f-string ...))`) where `ast` merges them.
- **Positions** (`lineno`, `col_offset`, `end_lineno`, `end_col_offset`, UTF-8 bytes, E4) in `:full`'s metadata, so
  the print can give `compile`'s line table (E7).
- **Type comments** (`# type: int`, `ast.parse(type_comments=True)`) as metadata of their node; `TypeIgnore` in the
  module's metadata.
- **Docstrings** are ordinary `Expr(Constant(str))` statements; the forms keep them so (the compiler's choice to
  store them as `__doc__` is eval's).

Examples (proposed, to fix at PY1):

```
(function-def f [(type-param T)] (arguments {:posonly [(arg a T)] :vararg (arg b) :kwonly [(arg k)]
              :kw-defaults [1] :kwarg (arg kw)})
  [(return (if-exp a a k))] {:returns T})                          ; def f[T](a: T, /, *b, k=1, **kw) -> T: return a if a else k
(assign [x] (bin-op * (bin-op + 1 2) (int "0x1F")))                ; x = (1 + 2) * 0x1F
(match x [(match-case (match-sequence [(match-value 1) (match-star rest)]) :guard rest [(pass)])])
```

### 4.3 Layout of the `.ae` files

One `.ae` per `.py`, the forms of `:lines` pretty-printed by the family's layout (`arbace.repr.java.layout`'s
widths, as repr/go's and repr/ocaml's), mode `:full`'s token stream in a trailing block as repr/clojure keeps a
`.cljc`'s conditionals; packages as directories (`__init__.ae`), as Go forms' packages are.

## 5. repr/python: read and print

### 5.1 The pipeline

`bin/repr-python read FILE` → helper `tools/pydump/pydump.py` run as `python3 -I -S -B` (isolated: no user site,
no current directory on the path, no `.pyc` written; the script reads files given as arguments, the convention for
untrusted inputs) → forms written by the helper in this spec's vocabulary (as `ocdump` writes OCaml forms) →
the converter `arbace.repr.python.convert` (checks, literal spelling, `.ae`) → Python forms. `print` → the printer
`arbace.repr.python.print` (precedence-aware parenthesization, indentation) → Python text.

The helper's modes: `tree` (`ast.parse(..., type_comments=True)` with attributes, the literals' source segments),
`tokens` (`tokenize.tokenize`, E5), `error` (`SyntaxError`'s class, `msg`, `lineno`, `offset`, `end_lineno`,
`end_offset`, `text`), `symtable` (scopes and each name's flags, for pyi's check, PY5), `code` (the `marshal`
digest of `compile`, and the positionless signature of E7, for print's levels). Unlike `hsdump` and `rsdump` it is
**not built**: its pin is the interpreter's (`tools/pydump/PYTHON_VERSION` with the apk's name and the
interpreter's `sys.version`, checked at each run as `GHC_VERSION` is).

### 5.2 Modes

`:lines` (forms; text → forms → text → forms equal) and `:full` (the text byte for byte from the token stream, E5,
with the comments and `NL` tokens woven by position). The print's levels, from repr/go's and repr/haskell's:

1. **forms**: Python → forms → Python → forms equal;
2. **ast**: `ast.dump` (without attributes) of the print equal to the original's;
3. **code**: the print's code objects equal to the original's without positions (E7), recursively;
4. **text**: `:full` printed byte for byte;
5. **marshal**: `:full`'s print compiled gives `marshal` bytes equal to the original's (positions included; with
   level 4 this is redundant but checks the helper);
6. **behaviour**: the run programs (§8.2) run from the print, as from the original (eval's references).

### 5.3 Errors

A file `ast.parse` rejects is a corpus case judged on the exception's class (`SyntaxError`, `IndentationError`,
`TabError`), message and span (E6), as goi's error tests are judged on gc's syntax errors. CPython's messages come
from the grammar's `invalid_*` rules, so a native reader generated from the same grammar (§5.5) produces them
without a table.

### 5.4 Encodings and lines

PEP 263 coding cookies, a UTF-8 BOM, `\r\n` and `\r` line ends, form feeds in indentation, tabs (`TabError`): `:full`
keeps the bytes (E5's round trip is in bytes); the forms hold the decoded text. Files that are not UTF-8 (a
`latin-1` cookie in `Lib/test`'s data) are corpus cases of their own.

### 5.5 The native reader (PY9; PYS4)

The JN8/GP5/C2 step: a lexer port of `Parser/lexer/` (indentation, the f-string and t-string modes of PEP 701,
numeric literals, the `type_comments` mode) and a parser **generated from `Grammar/python.gram` by an Arbace port of
pegen** (`Tools/peg_generator/pegen`: the grammar's grammar, the first-sets and left-recursion analysis, a back end
emitting Arbace with memoization), the grammar's C actions (`_PyAST_*` constructors and `action_helpers.c`'s
helpers, about 1,700 lines, to verify) translated into form constructors. Its check is `parse-check`: every corpus
file and N mutants parsed by both, the forms and the errors equal, against a known file. A native reader makes
read, eval and AOT need no CPython at all, the host's Python remaining only as the reference of recorded runs.

## 6. eval: pyi

### 6.1 What eval means for Python

A REPL (`bin/pyi`, the `python3` REPL's behaviour for expression statements: `repr` of a non-`None` value, `_`), and
program runs (`bin/pyi run FILE ARGS`, `-c`, `-m MODULE`) with `sys.argv`, the exit status (`SystemExit`, uncaught
exceptions: 1, the traceback on stderr), stdout and stderr. Python forms evaluated at Arbace's REPL as goi's are
(PYS20).

### 6.2 The parts

1. **The scope analysis** `arbace.python.scope`: CPython's `symtable` (module, class, function, lambda,
   comprehension and annotation scopes; PEP 709's inlined comprehensions; PEP 695's type-parameter scopes; PEP
   649's annotation scopes), with its errors. Checked name by name against the helper's `symtable` dump over every
   corpus (a check as goi's checker is checked against go/types).
2. **The compiler** `arbace.python.compile`: forms → the IR (§6.3), with the compiler's errors (E6) and warnings.
3. **The runtime** `go/arbace/py` (hand-written Go forms, in goi's table like `go/arbace/scm` and `go/arbace/oi`): the
   object model (§6.4), the frame machine, the built-in modules (§6.6).
4. **The library** `python/Lib/`: the stdlib's Python modules as Python forms, vendored and embedded (§6.6).
5. **The tools** `bin/pyi run|check|test|divergences|record-ref`, `arbace.python.check` on the tools' host, as goi's.

### 6.3 The IR and the frame machine (PYS7)

Python's semantics lives in frames: generators, coroutines and async generators are suspended frames; tracebacks
are a chain of frames with their positions; `sys._getframe`, `f_locals` (PEP 667's write-through proxies),
`sys.settrace` and `sys.setprofile` (which `coverage`, `pdb` and parts of `Lib/test` use) inspect them; recursion is
bounded by `sys.getrecursionlimit()` (1,000 by default), not by memory. So pyi compiles each code object to an IR of
its own (a register or stack code with an exception table, CPython-like in shape, Arbace's in detail and stable
across CPython releases) and runs it on **heap frames** in a loop in Go: a generator's `yield` returns from the loop
keeping the frame, `send` resumes it; `await` is the same; an exception unwinds through the exception table; the Go
stack never holds Python frames, so a deep Python recursion is a `RecursionError` exactly where CPython raises it.

The alternative (b), goi's closure tree on Go's stack with generators on goroutines (OC6's hand-off), is faster for
plain calls and pays a goroutine switch (hundreds of nanoseconds or more) at every `yield`, which Python code does in
every `for` over a generator; and it cannot give `f_lineno`, `settrace` or exact `RecursionError` without the same
bookkeeping a frame machine has. Interpreting CPython's bytecode (c) ties pyi to 3.14's opcodes (E7) and needs the
oracle to compile.

### 6.4 The object model

- **Objects**: a Go interface over structs with a pointer to their type; **types are objects** with C3 MROs, the
  slots CPython's `tp_*` caches (`__getattribute__`, `__call__`, `__hash__`, `__eq__`, the number and sequence
  protocols) filled from the class dictionary and invalidated on assignment, as CPython's type version tags do.
- **The data model**: attribute lookup with data and non-data descriptors, `__slots__`, `__getattr__`, properties,
  `classmethod`/`staticmethod`, `super()` (the `__class__` cell), metaclasses with `__prepare__`, `__init_subclass__`,
  `__set_name__`, `__class_getitem__`, `__mro_entries__`; the binary operators' reflected-operand rule and
  `NotImplemented`; rich comparison; `__index__`, `__format__`, `__hash__` = `None`; the context manager, iterator and
  async protocols; `__match_args__`.
- **Numbers**: `int` arbitrary precision (an `int64` fast path, `math/big` beyond; CPython's results for `//`, `%`,
  `**` with negatives, `pow(a, b, m)`, `int.to_bytes`, the 4,300-digit limit on `str(int)`); `float` with CPython's
  `repr` (the shortest round-trip digits, `1e+16`, `inf`, `nan`; Go's `strconv` shortest digits formatted by
  CPython's rules), `float.__format__`'s mini-language, `round` with ties to even, `math` from musl's libm (§2.4);
  `complex`; `bool` as an `int` subclass; hashing of numbers by the modulus `2**61 - 1` (E1).
- **Strings**: code-point sequences with O(1) indexing (PEP 393's Latin-1/UCS-2/UCS-4 storage, or `[]rune`), lone
  surrogates allowed (`surrogateescape`, `surrogatepass`), Unicode 16.0.0's data for `str.isidentifier`,
  `casefold`, `upper` (from the UCD files, pinned: Go's `unicode` tables are of another version, to verify); `bytes`,
  `bytearray`, `memoryview` with the buffer protocol.
- **Containers**: `dict` insertion-ordered (CPython's compact layout's iteration order, and its errors on mutation
  during iteration), `set` and `frozenset` (their iteration order follows the hash and the table's growth: CPython's
  probing reproduced, with `siphash13`, E9), `list` (Timsort, stable, the same comparisons in the same order:
  `sort` with a key that prints is observable), `tuple`, `range`, `slice`.
- **Exceptions**: the built-in hierarchy (about 70 classes), `__context__`, `__cause__`, `__suppress_context__`,
  `__traceback__`, `add_note`, `BaseExceptionGroup`/`ExceptionGroup` with `except*`; tracebacks printed with 3.11's
  carets (`~~~^^^`), judged on their last line by default (§8.1).
- **Functions**: defaults, keyword-only and positional-only parameters, `*args`/`**kwargs`, closures (cells),
  `__wrapped__`, `functools.partial`; bound methods; `__code__` attributes as far as the IR can answer.
- **Identity**: `id()` a stable number per object (not an address), `is` on interned small ints (-5 to 256) and
  interned strings as CPython interns them (observable through `is`: a class where it differs).

### 6.5 Memory, finalization, threads

- **Finalization (PYS9).** CPython frees an object when its count reaches zero: `__del__` runs, a weak reference's
  callback fires, an unclosed file is flushed and warns (`ResourceWarning`) at that point; programs and tests rely
  on it (`del x; assert wr() is None`). On Go's tracing GC that timing cannot be had without counting. v1: Go's GC, a
  finalizer queue drained at `gc.collect()` and at safe points, `sys.getrefcount` a class; the size of the class
  measured at PY7 before deciding whether pyi counts references (b).
- **Threads (PYS10).** `threading` over goroutines with a GIL (one Python thread runs at a time, switched every
  `sys.getswitchinterval()` at the IR's back edges and calls), which is the host build's semantics and makes the
  object model's structures need no locks; the free-threaded semantics a later option.
- **Signals**: `signal` handlers run between IR instructions on the main thread, as CPython runs them;
  `KeyboardInterrupt`.

### 6.6 The standard library (PYS11)

- **Python modules as forms**: the 595 stdlib modules read by repr/python into `python/Lib/` (committed like
  `jdk/`, `tamago/goroot/`, by a `bin/python-vendor convert|verify`), with the pure twins (E8) taking the C modules'
  places: `_pyio` as `io`'s implementation, `_pydecimal`, `_pydatetime`, `_pylong`, `_py_abc`, `_py_warnings`,
  `json`'s Python scanner and encoder, `pickle`'s Python `Pickler`, `heapq`'s, `bisect`'s, `functools`' and
  `collections`' pure parts, `asyncio`'s pure `Task` and `Future`, `ElementTree`'s Python parser front; and
  `importlib._bootstrap` and `_bootstrap_external` as **the import system** (PYS12): frozen into the executable as
  CPython freezes them, a source-only loader (no `.pyc`), `sys.path` naming the embedded `python/Lib`.
- **Go ports** (a list to fix at PY6; sizes of CPython's C to verify): `builtins`, `sys`, `_io`'s minimum under
  `_pyio` (`FileIO` over `crt`), `posix` (on `go/arbace/crt`), `_thread`, `_weakref`, `_imp`, `marshal` (for its
  format, `importlib`'s needs), `_codecs` and the encodings' C parts, `_sre` (the matcher, `Modules/_sre/sre_lib.h`,
  of the order of 3,000 lines; Go's `regexp` is RE2 and cannot do back references), `math` and `cmath` (musl's libm
  as C forms through cf2g, or Go's `math` with a class for the last bit), `itertools`, `_struct`, `array`,
  `_random` (Mersenne Twister, bit-exact for seeded runs), `unicodedata` (UCD 16.0.0), `binascii`, `zlib` (jrt's ports
  or Go's `compress/flate`; byte-exact `compress` output needs zlib's own deflate), `_hashlib`/`_md5`/`_sha1`/`_sha2`/
  `_sha3`/`_blake2` (Go's `crypto`), `time`, `select`, `_socket`, `errno`, `_stat`, `_string`, `_locale` (the C
  locale), `_contextvars`, `_tokenize` (the native lexer of PY9, or a class until then), `_ast` (Python forms ↔ `ast`
  objects: the bridge that makes `ast.parse` inside pyi the native reader), `_symtable`, `atexit`, `gc`, `faulthandler`
  (a class).
- **Deferred, classes in v1**: `_ctypes`, `_sqlite3`, `_ssl` (until `x509roots` and Go's TLS are wired), `_curses`,
  `readline`, `_tkinter`, `_lzma`, `_bz2` (Go has no compressor), `_zstd`, the CJK codecs, `_interpreters`,
  `_remote_debugging`, `_lsprof`, `_tracemalloc`, the `_test*` modules.
- **C modules through the C crop** (PYS11 (b), later): self-contained C cores (`sre_lib.h`'s matcher, libmpdec,
  `_randommodule.c`'s generator, expat, HACL*) read as C forms by repr/c and translated by cf2g, with the glue
  written by hand; a general C API emulation (as PyPy's cpyext) is out of scope.

## 7. AOT

Python's dynamism makes the first AOT a **compiled interpreter**: each code object's IR translated to a Go function
that calls the runtime's generic operations (`py.Add(a, b)`, `py.GetAttr(o, "x")`) with the dispatch loop, the
operand stack and the instruction decoding gone; generators as Go state machines over a heap frame (a `switch` on
the resume point); exceptions as returned statuses, as goi does for Go's panics; `exec`, `eval` and `compile` at run
time link pyi into the program. That is Nuitka's and grumpy's shape; realistic gains are 1.5 to 3 times pyi on
call- and loop-heavy code, little on code that spends its time in `dict` and `str`. Type feedback (an int loop
specialized under a guard, as CPython 3.11's specializing interpreter and its JIT do) is a later step
(FUTAMURA-SURVEY's F10 kind).

The path (PYS13): the IR → Go forms printed by repr/go and built by `arbace.build.go` over `go/arbace/py`, as
Scheme's AOT (SC9 (a)): the runtime is Go forms already, and Java forms (OCaml's o2j path, OC7) would add j2g's
translation of closures and statuses for nothing. The check: `bin/pyi check --aot` over the run corpora against the
same references, and pyi against compiled per program (diffcheck's kind).

## 8. The oracle, the references and the corpora

### 8.1 The references

- **Runs recorded as data** (`test/python/ref-cpython314.edn`, `bin/pyi record-ref`, as `bin/java-check
  --record-ref` and ci's `ref-gcc15`): each program run by the host's `python3` under a fixed environment
  (`PYTHONHASHSEED=0`, `-I`, `PYTHONDONTWRITEBYTECODE=1`, `LC_ALL=C.UTF-8`, `TZ=UTC`, `PYTHONIOENCODING=utf-8`, no
  terminal), its status, stdout's digest and text, stderr's last line and digest. The check needs no Python.
- **Masks**: addresses in default reprs (`at 0x...`), `id()` values, timings, and the traceback's text beyond its
  last line (a second, stricter judge on the full traceback where the programs print positions, PYS14).
- **`Lib/test`**: run by its own runner (`python3 -m test -v MODULE` or `unittest -v`), each test method a case
  (`ok`, `FAIL`, `ERROR`, `skipped 'reason'`, `expected failure`), as Clojure's suite is judged per test
  (PYS15); a module that does not import is one case.

### 8.2 The corpora

| corpus | source | size | license | use |
|---|---|---|---|---|
| the stdlib | `Lib/` of the pinned tarball (the host's 595 files as a first measure, E2) | about 600 modules, 300k lines (host); the tarball's `Lib/` without tests to verify | PSF-2.0 | read, print; the library pyi runs (§6.6); AOT |
| `Lib/test` | the pinned tarball's `Lib/test/` (or apk `python3-tests`, 158 MiB installed) | of the order of 2,000 `.py` files and 800k lines, about 500 `test_*` modules, to verify | PSF-2.0 | read, print (with its deliberately bad files: `badsyntax_*`, `bad_coding*`, `tokenizedata/`); eval and AOT per test method (§8.1) |
| `test_grammar`, `test_syntax`, `test_exceptions`, `test_patma`, `test_fstring`, `test_tstring`, `test_type_params`, `test_generators`, `test_coroutines`, `test_descr`, `test_class` | inside `Lib/test` | a few dozen modules | PSF-2.0 | the language core: the first eval corpus |
| site-packages | the host's `py3-*` apks (§2.1), each pinned by name and version | 1,180 files, 461,735 lines | MIT, BSD, Apache-2.0, PSF (each package's, to verify) | read, print; later eval of pure ones (`attrs`, `idna`, `packaging`, `pyparsing`, `jinja2`) |
| pyperformance | github.com/python/pyperformance, a pinned release (to verify) | about 80 benchmarks, of which the pure-Python ones (`nbody`, `richards`, `chaos`, `deltablue`, `fannkuch`, `float`, `go`, `hexiom`, `nqueens`, `pidigits`, `raytrace`, `spectral_norm`, `unpack_sequence`, `json_dumps`, `pyflate`, ...) | MIT (to verify) | eval and AOT, with their outputs; speed |
| benchmarksgame | the benchmarks game's Python programs (C's crop pins only its C programs, from `modernc.org/ccorpus2@v1.6.0` in `test/c/corpora.edn`; the Python ones a fetch, to verify) | tens of programs | BSD-3-Clause (revised, to verify) | eval, AOT, speed |
| synthetic | generated (§8.3) | hundreds of small programs | Arbace's | every level; the essential subsets |
| samples | `test/python/samples/`, one file per construct of §4 | tens | Arbace's | every level |

### 8.3 Synthetic corpora (GT8.6, GTS207 (a))

From the start, as GT8 asks of a new crop: (1) one program per rule and alternative of `python.gram`, including its
`invalid_*` rules (an error each), for read, print and the native reader; (2) one program per special method and per
built-in type's method (the data model's table and `builtins`' surface), printing its results, for eval and AOT; (3)
per statement and expression kind, the generator, coroutine and exception paths (`yield` inside `try`/`finally`,
`return` in a generator, `throw`, `close`, `aclose`, `except*` splits). Each is run by the same CPython and recorded
(GTS207 (a)); generated by an Arbace generator from the grammar and the data model's list, committed with its seed.

## 9. Licenses

`LICENSE.md` gains Python: CPython's **PSF License Agreement for Python 3.14** (PSF-2.0, "Python Software Foundation
License Version 2", permissive, with its own text and the history of the BeOpen, CNRI and CWI licenses it carries),
covering `Lib/`, `Grammar/`, `Parser/`, `Tools/peg_generator/` and the C sources read or ported; and the licenses of
what CPython bundles that a port touches: libmpdec (BSD-2-Clause), expat (MIT), HACL* (MIT or Apache-2.0), zlib
(zlib), mimalloc (MIT), the Unicode data (Unicode-3.0), `Lib/test`'s data files under the same PSF terms (all to
verify against `LICENSE` and `Doc/license.rst` of the pinned tarball). Vendoring `Lib/` as Python forms keeps the
files' notices (PSF's terms ask that the license and its notice be retained) and lists the files in `LICENSE.md`.
gpython (BSD-3-Clause), PyPy (MIT) and RustPython (MIT) are read as references; a vendored piece of PyPy's
`lib_pypy` would add MIT. pyperformance's and the site-packages' licenses per package (to verify).

## 10. The row and GT8's budget

### 10.1 The row

`test/reap/cells.edn` gains `:python` with four cells and the classes files
(`test/python/divergences{,-read,-aot,-print}.edn`); the Makefile gains `repr-python-*` and `pyi-*` targets
(`make check-makefile`). As the other crops since OC120 (b), its steps enter the gate only when its row fits GT8's
budget (the journal, 2026-11-03).

### 10.2 The budget (GTS201 (a): about 35 minutes nightly, about 2.4 hours clean, a share of the essential 15 minutes)

| tier | Python's content (proposed) | estimate (to measure at PY2, PY5, PY7) |
|---|---|---|
| essential (a share of 15 min, about 1 min) | the samples at every level; the synthetic core (a coverage subset of §8.3); 50 stdlib files read and printed `:full`; one AOT program of the synthetic core | under 60 s, on stage 0 and the gate's executable |
| nightly (35 min) | read and print of every corpus file (stdlib, site-packages, `Lib/test`'s files), deduplicated (GT8.2); eval and AOT over the coverage subset of `Lib/test`'s test methods (GTS205 (a): every block of `go/arbace/py` and of the compiler reached, every class with one case, the controls, every deferred case) plus the rotation's seventh (GTS203 (a)); the native reader's `parse-check` over the corpus and a seventh of the mutants | read: CPython parses the stdlib in 2.8 s (E2); the helper's dump, the converter and the print at 50 to 100 times that is 3 to 5 min for all read corpora; eval: see below |
| clean (2.4 h) | every part over the compressed corpora, audited; the rotation's record complete | within 2.4 h if the nightly is within 35 min |

**Why `Lib/test` whole cannot be nightly.** CPython runs its whole suite in roughly 10 to 30 minutes serially (to
verify; `-j` shortens it); an interpreter on Go's runtime is likely 3 to 20 times slower than CPython's specializing
interpreter at first (gpython's and RustPython's reported factors, to verify), so the whole suite is hours under pyi.
The coverage subset (GTS205/GTS206: `go build -cover` of the program, the test methods that reach every block) is
the nightly's, the rest rotates over seven nights, and the slow modules (`test_math`'s exhaustive cases,
`test_decimal`'s `decimaltestdata`, `test_unicodedata`'s full check, the `test_asyncio` package) get per-test CPU
limits as Java's `speed` curation does (GTS102, GTS208). Each level's measured cost is recorded with its first record
(GT8.1) and a level over its share compresses before its corpus grows (GTS201 (a)).

## 11. Plan (PY1-PY11)

Estimates in working days, as the other surveys give them. Each milestone ends with its check green and its records.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **PY1** the spec and pins | `doc/python/SPEC.md`: Python forms for every `ast` node of 3.14 (§4), the literal spelling, `:full`'s tokens, the checklist against `Parser/Python.asdl`; pins (`tools/pydump/PYTHON_VERSION`, the tarball's sha256, the site-packages apks, `test/python/corpora.edn`); `Lib/test`'s counts measured; the samples | the spec accepted; every corpus pinned and counted | 2-3 |
| **PY2** repr/python `:lines` | `tools/pydump` (tree, error, symtable, code), `arbace.repr.python.convert`, `.print`, `bin/repr-python read|print|roundtrip|check` | stdlib, site-packages, `Lib/test`'s files read; levels 1-3 of §5.2; rejected files judged on the message; costs recorded | 4-6 |
| **PY3** `:full` | the token mode, comments and layout woven, levels 4-5 | byte for byte over every read corpus; `marshal` equal | 3-5 |
| **PY4** synthetic corpora and references | the generator of §8.3, `bin/pyi record-ref`, `test/python/ref-cpython314.edn`, `Lib/test`'s per-method references | references for the synthetic, the language-core modules and the benchmarks | 2-3 |
| **PY5** pyi's core | scope analysis checked against `symtable`; the compiler to the IR; the frame machine; the object model (§6.4) for the built-in types, classes, descriptors, exceptions, generators, coroutines, `match`, f-strings and t-strings, `with`; `builtins`, `sys`; the REPL | the synthetic corpus and the language-core modules of `Lib/test` passing or classed; `symtable` equal over every corpus | 15-25 |
| **PY6** the stdlib | `python/Lib/` vendored as forms (`bin/python-vendor`), the import system from `importlib._bootstrap`, the Go ports of §6.6, threads with the GIL, `asyncio` over `selectors` | `Lib/test`'s per-method check: none failing, every non-pass classed; `pyperformance`'s pure benchmarks run | 10-18 |
| **PY7** the check over `Lib/test` and the classes | `bin/pyi check`, `divergences`, controls (GTS81 (a)), the coverage subset and rotation slices (GT8), finalization's class measured (PYS9) | every case in a result or a class; the nightly within its share | 4-6 |
| **PY8** AOT | the IR → Go forms, generators as state machines, `bin/pyi check --aot`, pyi against compiled | none failing; every refusal with a reason; speed recorded | 8-14 |
| **PY9** the native reader | the lexer port, pegen ported to Arbace with an Arbace back end, the actions; `parse-check` with mutants; `_ast` and `_tokenize` in pyi on it | trees and errors equal to the helper's over every corpus and the mutants | 8-15 |
| **PY10** the row | `cells.edn` `:python`, classes files, `bin/reap`, essential subsets, steps (`:parts` per corpus), Makefile | `bin/reap`: Python's four cells checked, none failing; `bin/gate --costs` within the shares | 1-2 |
| **PY11** the milestone | `bin/gate --full --clean` with Python's steps within budget, the embedding and re-seed if decided (PYS18), the tag | the tag | 1-2 |

**Total: about 57 to 98 working days** (PY1 2-3, PY2 4-6, PY3 3-5, PY4 2-3, PY5 15-25, PY6 10-18, PY7 4-6, PY8 8-14,
PY9 8-15, PY10 1-2, PY11 1-2); without the native reader (PYS4 (c)) about 49 to 83. PY1-PY3 (9 to 14 days) are a
trial with no runtime commitment; PY9 can run beside PY5-PY8 (it needs only PY2's helper as its judge).

**Sonnet and Opus** (the user's split): Sonnet for PY1's spec from `Python.asdl`, the helper (a short Python
script), the converter and printer once the heads are fixed, PY3's token weaving, PY4's generator, PY6's vendoring
tool and the mechanical ports (`binascii`, `_struct`, `_random`, hashes), PY7's classing, PY10. Opus for the IR and
frame machine, the object model and its slots, generators and coroutines, exception groups, finalization, the GIL,
`_sre`, pegen's port, the AOT.

**Re-seed needs.** PY1-PY4 change tools' namespaces only (evaluated from the checkout, out of `gate_key`, as the
other crops' are). `go/arbace/py` is evaluated by goi until listed in goi's table and compiled; embedding pyi in the
shipped program waits for a re-seed batched with the other crops' (CS250's decided line).

## 12. Proposals (PYS1-PYS20)

Each with options, the recommended one first.

- **PYS1 The reference and its pins.**
  - Options: (a) CPython 3.14.10: the host's Alpine `python3-3.14.10-r1` as the oracle (the helper and the recorded
    runs; its apk's sha256 pinned in the style of `tools/rsdump/apks.sha256`), and the source tarball
    `Python-3.14.10.tar.xz` from python.org (`https://www.python.org/ftp/python/3.14.10/`, sha256 pinned, to verify)
    for `Lib/test`, `Grammar/`, `Parser/`, `Tools/peg_generator/`, `Modules/` (references for ports) and `LICENSE`;
    the apk `python3-tests` not used (it is the same `Lib/test`, but a second artefact to pin); (b) the tarball alone,
    CPython built from it into `.tmp/` as the oracle (independent of Alpine's moves, a build of minutes per pin);
    (c) 3.13.x (the previous release; no t-strings, no PEP 758, the old annotations).
  - *Recommended: (a)*, the pattern of gcc and GHC (the host's compiler, its package pinned); (b) if Alpine edge
    moves to 3.15 before PY11.
- **PYS2 Names.**
  - Options: (a) `pyi` (`bin/pyi`), repr/python (`arbace.repr.python`, `bin/repr-python`), `tools/pydump`, the runtime
    `go/arbace/py`, the forms' top heads `python/...`, the vendored library `python/Lib/`, milestones PY1-PY11,
    proposals PYS (PYS20 onward after PY1); (b) `pi` (clashes with the constant); (c) `cpyi`.
  - *Recommended: (a)*.
- **PYS3 The read level's oracle.**
  - Options: (a) the helper `tools/pydump` on the host's interpreter (`ast`, `tokenize`, `symtable`, `compile`), a
    script with no build; (b) the native reader first (PY9 before PY2: no judge on its first day, the helper still
    needed to judge it); (c) `python3 -m ast` and `-m tokenize`'s text scraped (lossy: no symtable, no code digests).
  - *Recommended: (a)*, as hsdump, rsdump and ocdump.
- **PYS4 The native reader.**
  - Options: (a) in the plan (PY9), generated from `python.gram` by an Arbace port of pegen with the C actions
    translated, the lexer hand-ported, beside PY5-PY8; (b) a hand-written recursive descent parser (no generator to
    port, but the `invalid_*` rules' messages rewritten by hand and kept in step at each release); (c) not in v1 (the
    host's CPython stays needed for read).
  - *Recommended: (a)*: the grammar is the specification CPython itself is generated from, so the messages and the
    release-to-release diffs come for free, and with it every level of the row is standalone.
- **PYS5 The forms' vocabulary.**
  - Options: (a) the `ast` classes as heads (§4), `ctx` dropped and checked, operators as symbols, literal spelling
    and implicit concatenation kept; (b) a desugared core (the IR) as the user-facing form (loses the round trip);
    (c) a concrete syntax tree (lib2to3's kind: every token a node; heavier, and `:full`'s tokens give the same).
  - *Recommended: (a)*, as OCaml's forms mirror the Parsetree.
- **PYS6 `:full`.**
  - Options: (a) the helper's token stream (E5) woven into the forms by position, comments and `NL` kept; (b) the
    source text's slices between nodes stored (simpler, but the forms then carry text rather than structure).
  - *Recommended: (a)*, the native lexer replacing the helper's tokens at PY9.
- **PYS7 Eval's representation.**
  - Options: (a) an IR of Arbace's own compiled from the forms, run by a frame machine on heap frames (§6.3);
    (b) a closure tree on Go's stack (goi's), generators by goroutine hand-off (OC6's); (c) CPython's bytecode from
    the oracle's `marshal` (version-locked, E7, and the oracle needed to run).
  - *Recommended: (a)*: generators, coroutines, tracebacks, `settrace` and `RecursionError` are frames.
- **PYS8 The runtime's base.**
  - Options: (a) `go/arbace/py`, hand-written Go forms (as `go/arbace/scm` and `go/arbace/oi`), objects as Go structs
    with a type pointer and slots; (b) Arbace's Clojure runtime values (persistent collections against Python's
    mutable ones: a mismatch at every operation); (c) Java forms over jrt, translated by j2g.
  - *Recommended: (a)*.
- **PYS9 Finalization and reference counts.**
  - Options: (a) Go's tracing GC, `__del__` and weak-reference callbacks run from a finalizer queue drained at
    `gc.collect()` and at safe points, the timing a design class, measured at PY7; (b) reference counting
    emulated (an increment and decrement at every store, cycles left to Go's GC: CPython's timing exactly, at a
    cost of perhaps 1.5 to 2 times, to measure); (c) (a) with counting for the few types whose finalization is
    commonly relied on (files, generators' `close`), as a hybrid.
  - *Recommended: (a)*, then (c) or (b) on PY7's measured class.
- **PYS10 Threads.**
  - Options: (a) goroutines under a GIL with CPython's switch interval; (b) free-threaded semantics (3.14t: per-object
    locks, no GIL); (c) no threads in v1 (`threading` a class).
  - *Recommended: (a)*.
- **PYS11 The standard library.**
  - Options: (a) the Python modules as forms with the pure twins in the C modules' places, and Go ports for the
    list of §6.6; (b) (a) plus the self-contained C cores (`sre_lib.h`, libmpdec, expat, mt19937) read as C forms and
    translated by cf2g with hand-written glue, where a port would be large; (c) every C module ported to Go.
  - *Recommended: (a)*, (b) studied at PY6 for `_sre` and `_decimal`'s speed.
- **PYS12 The import system.**
  - Options: (a) `importlib._bootstrap` and `_bootstrap_external` run as forms, frozen into the executable,
    source-only; (b) an import system written in Go (faster at start, a second implementation of PEP 302/451's rules).
  - *Recommended: (a)*, as Scheme's crop runs psyntax.
- **PYS13 AOT's path.**
  - Options: (a) the IR → Go forms over `go/arbace/py` (a compiled interpreter; Scheme's SC9 (a)); (b) the IR → Java
    forms → j2g (OCaml's OC7 (a)); (c) Python → Arbace's Clojure → c2j (a semantic mismatch at every operation).
  - *Recommended: (a)*.
- **PYS14 The run judge.**
  - Options: (a) status, stdout byte for byte, stderr's last line, with addresses and `id()` masked, `PYTHONHASHSEED=0`
    and pyi's `siphash13` keyed as CPython keys it for seed 0 (E9); the full traceback a second judge where positions
    matter; (b) stdout only; (c) sets and dicts compared as sorted (hides real divergences).
  - *Recommended: (a)*.
- **PYS15 `Lib/test`'s judge and `sys.implementation`.**
  - Options: (a) per test method from the runner's verbose output, `sys.implementation.name == "arbace"` so
    `@cpython_only` and `check_impl_detail()` tests skip (their skips counted as a design class), as PyPy runs it;
    (b) per module (coarse: one failing method hides the rest); (c) `sys.implementation.name == "cpython"` (internals'
    tests run and fail).
  - *Recommended: (a)*.
- **PYS16 Corpora and GT8 from the start.**
  - Options: (a) the synthetic corpora (§8.3) and coverage subsets of `Lib/test` from PY4/PY7, the read corpora whole
    (minutes), costs recorded per level from the first record; (b) `Lib/test` whole nightly (hours: over the share);
    (c) the synthetic corpora alone (misses the stdlib's real use).
  - *Recommended: (a)*.
- **PYS17 The site-packages and PyPI corpus.**
  - Options: (a) the host's `py3-*` apks for read and print at PY2 (pinned by version), a small pinned PyPI set of
    pure packages for eval later; (b) a PyPI set fetched and pinned at PY1; (c) none.
  - *Recommended: (a)*.
- **PYS18 Embedding.**
  - Options: (a) repr/python and pyi evaluated from the checkout, out of `gate_key`, until the row fits the budget,
    then embedded at a re-seed batched with the other crops'; (b) embedded from PY5.
  - *Recommended: (a)*, as the crops since CS250.
  - When (PY10's DECISION 1, decided: the user, 2026-11-04, (a)): Python's tag does not wait for the re-seed; pyi is
    embedded at the first re-seed after the tag, batched with C++'s and Scheme's. The eval level's synthetic source is the
    compiled loop in every tier, pyi on goi rotated a seventh a night (PY10's DECISION 2, decided: the user, 2026-11-04, (a)).
- **PYS19 The crop's place in the order.**
  - Options: (a) after the tag `arbace-reaps-4-crops-v1` and GT8's compressed corpora, PY1-PY3 (light: a script,
    a converter, a printer) any time beside Haskell's and Rust's eval surveys, PY5 onward when the night budget has
    room; (b) Python first among the unparked crops (its eval needs no semantic oracle: the shortest way to a
    standalone row); (c) after Haskell and Rust's rows.
  - *Recommended: (a)*; (b) if the user wants the next row soonest.
- **PYS20 Python at Arbace's REPL.**
  - Options: (a) later: a package `python` of Arbace's API to Python forms (as `go/arbace.ae` is Go's, GR5), Python
    objects and Arbace values crossing by explicit conversion; (b) in v1.
  - *Recommended: (a)*.

### After PY11 (PYS21-PYS31; queue 202d, 2026-11-04)

The reasoning, measures and probes are in [AFTER-PY11.md](AFTER-PY11.md), its tags run, probed or read. All decided by the user, 2026-11-04: (a) for every one (PYS30 a request to the main host).

- **PYS21 Third-party C extensions.** *(decided: the user, 2026-11-04, (a))* (a) None for now: `:extension-modules` stays design, packages run on their pure
  fallbacks, the C API revisited on a named need; (b) the limited API (735 functions, 133 data) over pyi's objects as
  proxies in ci's memory, through ci and cf2g, from sdists only (no cgo, no `dlopen`: no binary wheel loads), 25-45 days;
  (c) the full API as PyPy's cpyext, 55-105 days, never complete; (d) HPy. *Recommended: (a).*
- **PYS22 The stdlib's C modules.** *(decided: the user, 2026-11-04, (a))* (a) By module: Go ports for small glue modules (`_csv` first: pip imports csv; `mmap`),
  self-contained C libraries through cf2g with Go-forms glue (`_sqlite3`, `_lzma`, `_bz2`, `_zstd`; libmpdec for `_decimal`
  only if its speed matters), `_ssl` over Go's TLS; the twins of PY6g's DECISION 1 stay; (b) every one ported by hand;
  (c) all classes. *Recommended: (a).*
- **PYS23 Packages beyond the 20 apks.** *(decided: the user, 2026-11-04, (a))* (a) Pinned pure-Python wheels from PyPI (name, version, SHA-256), the references
  CPython's runs with the same wheels on the main host; (b) sdists built by pyi; (c) Alpine's apks only. *Recommended: (a).*
- **PYS24 The installer.** *(decided: the user, 2026-11-04, (a))* (a) Arbace's own wheel installer (`bin/pyi install`, PEP 427/491), pip on pyi an eval target;
  (b) pip on pyi; (c) the host CPython's pip. *Recommended: (a).*
- **PYS25 Where packages live.** *(decided: the user, 2026-11-04, (a))* (a) A site directory built from the pins (`.tmp/cache/python/site-KEY/` for the checks,
  the program's work directory for `--build-program`'s `:python {:wheels ...}`); (b) committed as forms; (c) a user site.
  *Recommended: (a).*
- **PYS26 When to move to Python 3.15.** *(decided: the user, 2026-11-04, (a))* (a) After Python's tag, when Alpine edge's python3 is 3.15 (on 2026-11-04 it is
  3.14.10-r1; python.org has 3.15.0 since 2026-10-09 and 3.15.2), a round of its own, about 12-24 days; (b) now, CPython
  built from the tarball as the oracle (PYS1 (b)); (c) both versions supported. *Recommended: (a).*
- **PYS27 3.14.10's oracle once edge moves.** *(decided: the user, 2026-11-04, (a))* (a) The oracle's apk, its libraries' apks and the 20 py3-* apks kept in the
  shared cache, pinned, installable offline; (b) CPython built from the tarball then; (c) nothing (a forced move).
  *Recommended: (a).*
- **PYS28 pyi in the executable (the re-seed of PYS18).** *(decided: the user, 2026-11-04, (a))* (a) Listed in goi's table (`arbace/py`, `arbace/libm`; PY5a's
  DECISION (a)); (b) copied into the program as ci is, with a runtime class; (c) stay as today, the compiled loop and the
  compile server on stage 0. *Recommended: (a).*
- **PYS29 The embedded pyi's stdlib.** *(decided: the user, 2026-11-04, (a))* (a) Not embedded at the first re-seed: a configured `Lib` directory (the pinned
  tarball's); (b) embedded compressed (4.0 MB xz); (c) committed as Python forms and embedded (the box's end).
  *Recommended: (a).*
- **PYS30 The crops out of `arbace_runtime_key` and `arbace_javac_key`.** *(decided: the user, 2026-11-04, (a))* Both hash `go/arbace/py` today, and the javac
  key `arbace/python/` too (run): a pyi commit makes the builds take the two-run path and javac's tool rebuild. (a) One
  exclusion list in `bin/lib/version.bash` for `gate_key` and both keys, now; (b) leave. *Recommended: (a).*
- **PYS31 The eval cell after the embedding.** *(decided: the user, 2026-11-04, (a))* (a) pyi on the executable judged in every tier, once a night measures
  its 151 programs under 10 minutes; (b) PY10's DECISION 2 (a) kept. *Recommended: (a).*

## 13. What to decide first

PYS1 (the pin and the tarball's fetch), PYS3 and PYS4 (the oracle, and whether the native reader is in v1), PYS7
(eval's representation), PYS9 (finalization), PYS11 (the stdlib's strategy), then PYS19 (when). The rest follow. A
cheap trial is PY1-PY3: a round trip at five levels with no runtime.

## 14. Risks

- **Breadth.** The object model and the builtins are wide and every corner is observable; `Lib/test` exercises them
  all. PY5's range (15-25) is the widest and is re-estimated after the synthetic corpus runs.
- **Finalization timing** (PYS9): code relying on prompt `__del__`, weak-reference callbacks and `ResourceWarning`
  is common in `Lib/test`; the class may be large under (a).
- **Hash order** (E9): set and dict-of-set outputs differ unless `siphash13`, its seed-0 key and CPython's table
  probing and resizing are reproduced exactly; a slip shows as many stdout diffs at once.
- **Speed**: an interpreter on Go may be several times slower than CPython 3.14's specializing interpreter; the
  nightly share then holds fewer test methods, and the subsets carry more of the weight.
- **CPython-specific tests**: `@cpython_only`, `_testcapi`, `gc`, `sys.getrefcount`, `dis` of CPython's bytecode,
  `test_ast`'s positions of CPython's optimizer: a large design class, to keep honest (each with its reason).
- **The C modules' tail**: `ctypes`, `sqlite3`, `ssl`, `lzma`, `bz2`, `curses` and the CJK codecs as classes; a
  program importing one is a class, not a failure.
- **Unicode and floats**: Unicode 16.0.0's tables (Go's own are another version, to verify), `float` repr and
  formatting by CPython's rules, `math` to the last bit (musl's libm through the C crop, or a class).
- **The f-string/t-string lexer** (PY9): PEP 701's nested modes are the hardest part of a native lexer; until PY9
  the helper's tokens carry `:full`.
- **Moving pins**: Alpine edge may move to 3.15 (October 2026's 3.15 alpha cycle, to verify) and drop 3.14's apk;
  the apk's sha pinned and PYS1 (b) as the fallback.
- **Estimates**: the plan's ranges follow goi's, oi's and si's experience; unmeasured inputs (`Lib/test`'s size, the
  share of its methods pyi can run, the slowdown factor) are measured at PY1, PY5 and PY7 and the plan re-estimated.

## 15. Sources

- The host: `/usr/bin/python3` 3.14.10 (Alpine `python3-3.14.10-r1`, PSF-2.0, 22 MiB; `python3-tests` 158 MiB,
  `python3-dev` 101 MiB available, not installed), `/usr/lib/python3.14/` (595 `.py`, `lib-dynload/` 72 shared
  objects), `/usr/lib/python3.14/site-packages/` (1,180 `.py`).
- Upstream (not downloaded; to verify at PY1): github.com/python/cpython at tag `v3.14.10`: `Grammar/python.gram`,
  `Grammar/Tokens`, `Parser/Python.asdl`, `Parser/action_helpers.c`, `Parser/lexer/`, `Parser/tokenizer/`,
  `Tools/peg_generator/pegen/`, `Python/symtable.c`, `Python/compile.c`, `Python/codegen.c`, `Python/bytecodes.c`,
  `Objects/`, `Modules/_sre/`, `Modules/_decimal/libmpdec/`, `Lib/`, `Lib/test/`, `LICENSE`, `Doc/license.rst`,
  `Doc/reference/` (the Language Reference), `Doc/whatsnew/3.14.rst`; the tarball `Python-3.14.10.tar.xz` at
  python.org; PEPs 263, 393, 649, 667, 695, 701, 709, 734, 749, 750, 758, 765, 779, 784.
- Prior art (references, not vendored; licenses to verify): go-python/gpython (BSD-3-Clause), google/starlark-go
  (BSD-3-Clause), google/grumpy (Apache-2.0), PyPy's `lib_pypy/` (MIT), RustPython (MIT), MicroPython (MIT),
  python/pyperformance (MIT), HypothesisWorks/hypothesmith (MPL-2.0).
- Arbace: CLAUDE.md; doc/FULL-SUPPORT-SURVEY.md (the matrix); doc/C23-SURVEY.md; doc/ocaml/OCAML-SURVEY.md and
  doc/ocaml/SPEC.md (this file's shape and the forms' conventions); doc/scheme/SCHEME-SURVEY.md (§5.2's frames, §6's
  AOT); doc/cpp/CPP-SURVEY.md; doc/REPR-HASKELL-SURVEY.md and doc/REPR-RUST-SURVEY.md (the helper pattern);
  doc/GATE-SURVEY.md, "GT8, the test budget" and its decisions; doc/go/GOI-SPEC.md; `tools/hsdump`, `tools/rsdump`,
  `tools/ocdump`; `go/arbace/{oi,scm,crt}`.

## Appendix A: the scratch

In the worktree `.tmp/worktrees/python-survey`, `.tmp/python/` (gitignored): `t.py` (E4, E6, E7: a module with a
comment, `0x1F`, an f-string, a t-string, a generic function, `match`, PEP 758's `except`, `type`, a lambda with
`yield`), `u.py` (E4's UTF-8 offsets, E5's round trip), `sy.py` (a `nonlocal` for `symtable`). The probes were
one-line `python3 -c` programs over these and over `/usr/lib/python3.14/` (E2, E5, E8), each under a few seconds.

## Decided (2026-11-03, the user): PYS1-PYS20

PYS1-PYS18 and PYS20 all (a), as recommended. PYS19: the Mac VM peer does all of Python, step by step (PY1 through
PY11, a standing line of queue tasks, each reviewed and merged on the main host), beginning now, beside GT8; the main host
keeps GT8, the tag `arbace-reaps-4-crops-v1` and the other crops. Python stays out of `gate_key` and the gate (PYS18)
until its row fits GT8's budget.

## As built: PY1 (2026-11-03, the Mac VM, queue 174)

- **The spec**, `doc/python/SPEC.md`: Python forms for every node of `Parser/Python.asdl` (3.14.10: 19 types, 113
  constructors, 196 fields, 8 types with position attributes), the literals' spellings (§3: numbers, string and bytes
  bodies as written with `:prefix` and `:quote`, `str-concat` for implicit concatenation, f-strings with `:debug` for
  `=`, t-strings), `elif` kept, positions and `:full`'s tokens (§11), the error form (§12), the checklist (§13) and the
  choices S1-S20 (§14). The operators the reader cannot read (`@`, `//`, `^`, `~`) take the Go and C forms' names
  (S1). `ast.parse(type_comments=True)` rejects one file of `Lib/test` the plain parse accepts, so type comments come
  from a second parse (S6).
- **The pins**: the oracle in `tools/pydump/PYTHON_VERSION` (version, `sys.version`, the apk and its APKINDEX
  checksum per architecture); the tarball in `test/python/corpora.edn` (SHA-256 checked against python.org's
  published value and its Sigstore bundle's digest, SHA-512 against Alpine's APKBUILD), with five of its reference
  files; the site-packages set as 20 Alpine apks fetched into `.tmp/` (not installed), pinned per architecture in
  `tools/pydump/apks.sha256`, their `.py` files identical on x86_64 and aarch64.
- **The corpora** (`test/python/corpora.edn`): `stdlib` (`Lib/` less `Lib/test`: 720 files, 335,637 lines),
  `lib-test` (1,153 `.py` files, 667,670 lines; 433 test modules, 3,468 classes with 31,427 test methods counted
  statically; 4 files `ast.parse` rejects, 1 more that only `type_comments=True` rejects), `language-core` (the 11
  modules of §8.2: 28,302 lines, 1,140 test methods), `site-packages` (921 files, 330,603 lines), `samples` (6 files,
  every node class of the `ast` covered).
- **The checks**, `bin/repr-python pins --fetch|--count|--check`, `spec-check`, `samples` (the Makefile's
  `repr-python-*`), light, out of `gate_key` and the gate (PYS18).

## As built: PY2 (2026-11-03, the Mac VM, queue 175)

- **The helper** `tools/pydump/pydump.py` (no build; run `python3 -I -S -B`): `tree` writes each file as Python forms in
  the spec's vocabulary (the literals' spellings scanned in the source and evaluated back to their values; a target's
  context marked for the converter), or `(python/error ...)`, with a TSV per file of the levels' digests (`ast.dump`;
  the code without positions) and its seconds and peak RSS; `symtable` (PY5's reference) and `code`. Type comments
  from a second parse, a file only `type_comments=True` rejects marked `:type-comments :rejected` (queue 174's
  DECISION (a), decided by the user, 2026-11-03).
- **The converter and the printer**, `arbace.repr.python.convert` and `.print`, and the command `arbace.repr.python`
  (`convert`, `print`, `compare`), evaluated on stage 0 from the checkout (`ARBACE_PATH`; not embedded and out of the
  keys: `arbace/j2g/embed.ae`, `bin/lib/version.bash`, the audit's hosts, PYS18 (a)).
- **The round trip** `bin/repr-python roundtrip [--record]` (and `read`, `print`, `check`): levels 1-3 of §5.2 over
  samples, stdlib, site-packages (the 20 pinned apks, queue 174's DECISION (a), decided by the user, 2026-11-03) and `Lib/test`, judged
  against `test/python/roundtrip-known.edn` (two classes, both by rule: `:rejected`, the 4 files CPython rejects,
  judged on its message; `:code-lines`, the code differing with the ast equal: the positions'). On the VM (aarch64, 8
  CPUs), 2,800 files: level 1 2,796 of 2,796 read, level 2 all, level 3 all but 30 (3 stdlib, 21 site-packages, 6
  `Lib/test`); about 4.5 minutes, the costs per corpus and stage in `test/python/roundtrip-costs.edn`.
- **E7 corrected**: CPython 3.14's code without positions is not independent of the lines: a class body stores
  `__firstlineno__`, the compiler fuses `STORE_FAST`/`LOAD_FAST` pairs only within a line, inserts `NOP`s by line and
  splits exception ranges and match statements' `SWAP`s by line; the level's digest normalizes the first ones
  (`code_digest`), the rest are `:code-lines`.

## As built: PY3 (2026-11-03, the Mac VM, queue 176)

- **Mode `:full`** (doc/python/SPEC.md §11): the helper's `tree --full` writes every node with attributes in its long
  spelling with `:pos`, and the module's map with `:mode :full`, `:tokens` (the token stream as one flat vector, the
  text between tokens as `:ws`; the texts concatenated are the decoded file), `:encoding` (when not UTF-8) and `:bom`.
- **The converter** checks the forms against the tokens (every position on a token's edge, every leaf's spelling its
  token's text: 4.1 million positions and 2.07 million leaves over the corpora, 9 non-ASCII names whose tokens differ
  from the ast's by NFKC, not compared) and, without positions, against the `:lines` read of the same file.
- **The round trip** `bin/repr-python roundtrip --text [--record]`: level 4, the print's bytes equal to the original's;
  level 5, `marshal` of the code compiled from each equal, positions included; against `test/python/full-known.edn`.
  On the VM: samples 6, stdlib 720, site-packages 921 of 921, `Lib/test` 1,148 of 1,149 read (its 4 rejections as at
  PY2), all at both levels; the one other, `Lib/test/encoded_modules/module_koi8_r.py`, is `:charset` (the runtime has
  no KOI8-R charset: by design, queue 176's DECISION (a), the user, 2026-11-03: jrt's charsets are the root/English set of the locales decision of 2026-10-11). The 30 `:code-lines` files of PY2 pass both levels in `:full`. About
  8.5 minutes, the costs in `test/python/roundtrip-text-costs.edn`.
- Type comments and the site-packages set as decided for queue 174, `:code-lines` as decided for 175 (the user,
  2026-11-03).

## As built: PY4 (2026-11-03, the Mac VM, queue 177)

- **The synthetic corpus** (§8.3, GT8.6): `test/python/gen.ae`, run on stage 0 by `bin/pyi gen`, writes 145 programs into
  `test/python/synth/`: `ops/` (38: every binary, comparison and augmented operator over every pair of 15 operands, the
  unary and boolean operators), `datamodel/` (45: one special method or protocol each, every binary operator's own,
  reflected and in-place method among them), `builtins/` (17: every public method of str, bytes, bytearray, list, tuple,
  dict, set, frozenset, int, float, complex, bool, range, memoryview and slice, and the builtin functions), `constructs/`
  (32: together every node class of the ast), `paths/` (13: generators' close, throw and delegation, coroutines,
  async generators, context managers, `except*`, `finally`, recursion, an uncaught exception, `sys.exit`). Each prints
  reprs and exception names only. `bin/pyi gen --check` regenerates them equal to the committed files and checks the
  coverage. The grammar's `invalid_*` rules (§8.3 (1)'s error programs) are not generated yet.
- **The references** (GTS207 (a), PYS14 (a), PYS15 (a)): `bin/pyi record-ref` runs every program (the synthetic corpus and
  the samples) by CPython 3.14.10 as `python3 -s -P -B` (not `-I`, which would ignore `PYTHONHASHSEED`) with
  `PYTHONHASHSEED=0`, `LC_ALL=C.UTF-8`, `TZ=UTC`, `PYTHONIOENCODING=utf-8`, HOME and TMPDIR under `.tmp`, twice, into
  `test/python/ref-cpython314.edn` (151 references, none excluded), and `Lib/test`'s 11 language-core modules by regrtest
  (`-u none`: no network or other resource), each test method's outcome, into `test/python/ref-cpython314-lib-test.edn`
  (1,149 methods: 1,137 ok, 8 skipped, 4 excluded as the runner's: subprocesses run `-I` cannot import the tarball's test
  package). An entry the two runs disagree on, or whose output names an address, the architecture, the C library or a
  size, is written `:excluded`. `--check` reproduces every reference twice (7.7 s). Seconds per program and module in
  `test/python/ref-costs.edn` and `ref-costs-lib-test.edn` (all 151 programs in 1.1 s, the 11 modules in 2.6 s).
- pyperformance is not pinned: the pin is the user's (queue 177's DECISION).

## Decided (2026-11-03, the user): pyperformance pinned (queue 177's DECISION (a))

pyperformance 1.14.0's sdist (`pyperformance-1.14.0.tar.gz`, 8,796,350 bytes, SHA-256 `91f74393...a238a8aa`, PyPI's published
digest) is the speed reference for pyi and its AOT: pinned in `test/python/corpora.edn` (`:pyperformance-1.14.0`), fetched into
`.tmp/` and read in place by `bin/repr-python pins --fetch`, checked by `pins --check`; its pure-Python benchmarks to run as plain
programs with fixed loop counts, their output recorded as references (pyperf's timing runner not used).

## As built: PY5a (2026-11-03, the Mac VM, queue 179a)

PY5 split into steps reviewed one by one (queue 179's brief); PY5a is the scope analysis and the compiler, specified in
[PYI-SPEC.md](PYI-SPEC.md):

- **The scope analysis** `arbace.python.scope`: a port of CPython 3.14.10's `Python/symtable.c` (and `future.c`'s
  `__future__` check), its table printed in the form of the helper's new mode `pydump.py scopes` (CPython's own
  `_symtable`). `bin/pyi scope-check` over every read corpus: 2,980 files (samples 6, synthetic 145, errors 75, stdlib
  720, site-packages 921, lib-test 1,153), every table equal block for block and flag for flag, every rejection with
  CPython's message; `test/python/scope-known.edn` empty. The corpus `errors` (`test/python/errors/`, Arbace's): one
  program per message of `symtable.c`, and one per message of `codegen.c` the parser lets through (`compile-*.py`).
- **The compiler** `arbace.python.compile` (after `codegen.c`) to pyi's IR `arbace.python.ir` (§3 of the spec: code
  objects as maps, a stack machine with an exception table built from a handler stack as CPython's flowgraph builds
  it), and its verifier (stack depths by a worklist, handlers, indices, returns). `bin/pyi compile-check` over the same
  corpora: every file CPython compiles compiles and verifies (about 5.3 million instructions), every file CPython's
  compiler rejects is rejected with its message; `test/python/compile-known.edn` empty.
- **The keys** (PYS18 (a)): `arbace/python.ae`, `arbace/python/` and `go/arbace/py` out of `gate_key`, the embedded
  sources and the audit's tables, as the other crops'; `test/gate/gate-key-crops-test.bash` covers them.
- Not yet: running the IR (PY5b), so its behaviour is unchecked; lines come only from forms of mode `:full`.

## Decided (2026-11-03, the user): PY5a's DECISIONs and the split of PY5 (queue 179a)

- **Where pyi's runtime runs before a re-seed: (a).** `go/arbace/py` runs interpreted by goi on stage 0 (loaded from
  `ARBACE_PATH` as any interpreted package of Go forms), out of `gate_key`, and is compiled by a later batched re-seed,
  when it is also listed in goi's table (`go/arbace/gotable.edn` is in `gate_key`: listing it is the re-seed's step).
- **Traceback lines: (a).** pyi compiles the forms of mode `:full` (their `:pos`) when it runs a program; the checks'
  `:lines` dumps stay as they are.
- **The split of PY5** as 179a proposed it: PY5b (the machine and the core types), PY5c (the data model), PY5d
  (suspension and exceptions), PY5e (builtins, `sys`, the REPL and `Lib/test`'s core). By the user's order, PY5b0 first: the
  interfaces and the skeleton, small, reported and merged early; then the rest of PY5b, and PY5c, PY5d and PY5e
  concurrently (c and d independent; e alongside, its dunder-dependent part and the `Lib/test` core after c), one owner
  per shared file (doc/python/PYI-SPEC.md §9), rebased often, each sub-branch reported in turn.

## As built: PY5b (2026-11-03, the Mac VM, queue 186)

PY5b0's skeleton (PYI-SPEC.md §7-§9) and then the rest of PY5b, the core types, in `go/arbace/py` (Go forms of mode `:full`,
PY5b0's DECISION (a), decided by the user, 2026-11-03):

- **The types**: `int` of arbitrary precision; `float` (CPython's repr, divmod, a pow correctly rounded in double-double);
  `complex` (3.14's mixed-mode arithmetic, `_Py_c_quot`, `c_powi`); `str` as code points with lone surrogates (WTF-8);
  `bytes` with the utf-8, ascii and latin-1 codecs; `tuple`, `list` (a stable merge sort), `dict` with its views;
  `set` and `frozenset` with CPython's table, so iteration order is CPython's; `range`, `slice`; printf-style `%`;
  CPython 3.14's exception hierarchy with its fields; functions', code objects', frames' and tracebacks' attributes; the
  default exception hook's chains and source lines. Hashes are CPython's under `PYTHONHASHSEED=0` (siphash13, the
  numeric modulus, xxHash's tuple rounds), checked against the oracle's values.
- **The loop**: every operation PY5b owns; hooks for the other sub-milestones (generators, bound methods, subclasses'
  values, `OnTypesReady`, C3 for preset bases).
- **The compiler**: decorators applied nearest first (PY5c's finding); modes `:eval` and `:single` (`print-expr`); compile
  errors located; `bin/pyi compile-check --full` over every corpus.
- **The exit**: of `ops/` (38), `constructs/` (32) and the samples (6), with a scratch stand-in for PY5e's builtins,
  52 run equal to the references; the other 24 are classed: 2 by the last digit of `math`'s transcendental functions
  (Go's, not musl's: PY6's `math` port), 22 by an operation or builtin of PY5c, PY5d or PY5e.

## As built: PY8a (2026-11-03, the Mac VM, queue 198)

PY8's first part, the AOT itself, specified in [PYI-SPEC.md](PYI-SPEC.md) §10:

- **The IR straight to Go forms** (PYS13 (a)), `arbace.python.aot`, as Scheme's AOT: a compiled interpreter, each code
  object a Go function over the machine's heap frame, its entry a switch on the frame's PC (every instruction a label),
  its instructions calls of `go/arbace/py`'s helpers (`aot.go`) with constant arguments, its jumps gotos; the operations
  the helpers do not spell out (PY5c's, PY5d's and PY5e's registered ones among them) by `AotExec`, the loop's own
  implementation without its decoding. The loop runs a compiled code object in place of `step` (`Code.Native`), so calls
  push heap frames as before (RecursionError where CPython raises it), exceptions unwind by the same tables, and
  **generators, coroutines and async generators are state machines** on their resume point over the frame they keep.
- **A program** is its main module and the modules its module-level code imports, found statically on its `sys.path` and
  compiled ahead into packages of their own (keyed by their IR's hash, so `collections`' 9 modules compile once for
  every program that imports them); `exec`, `eval`, `compile` and other imports go at run time to the host's compiler
  (a compile server on stage 0) and run on the loop. Built by TamaGo's go alone (never `bin/arbace-go`), out of
  `gate_key` (PYS18 (a)).
- **`bin/pyi check --aot`**: the synthetic corpus and the samples (151, 177's references) compiled and run, each judged
  against CPython's reference, the same executable on the loop (`PYI_AOT_LOOP`) and pyi's run on goi: on the VM **123
  pass, 28 classed, 0 failing, 0 refused**; every classed one by the rule `:as-pyi` (its compiled run equal to the loop's
  and to pyi's: PY5's own classes, the C modules of PY6 among them, `test/python/divergences-aot.edn` holding no
  class of its own); a negative control (two passing programs judged against each other's references) fails as it must.
  About 1.2 s to compile a program, 17 minutes for the check with pyi's runs (pyi on goi about 100 times the compiled).
- **Speed** (`bin/pyi bench`, pyperformance 1.14.0's pure benchmarks with a pyperf stand-in, `test/python/bench`;
  `test/python/bench-aot-linux-arm64.edn`, the VM's): 13 run as CPython does, the 9 others classed `:module-import`
  (`_sre`, `_io`, `_random`, `array`: PY6's C modules). Compiled 21.9 s, the loop 22.7 s, CPython 3.3 s on a quiet VM (the
  recorded run, under a load of 8 from parallel tasks: 42.2, 42.3 and 3.8 s): the compiled
  interpreter is **1.04 times the loop**, not §7's 1.5 to 3, because the loop's dispatch is about 3% of the time (Go's
  profile of `deltablue`): the runtime's attribute lookup (`lookupAttr` 31 to 35%, an MRO walk of dictionary lookups per
  access), dictionaries and allocation dominate. The str hash kept and the name strs made once (PY5b's files) took
  `richards` from 6.0 to 4.2 s on both; a type attribute cache (prototyped, not committed: PY5c's) about 20% more.

## Decided (2026-11-03, the user): PY6b's DECISIONs (queue 200)

- **`.pyc` files carry pyi's own cache tag and magic number, amending PYS12 (a)'s "source-only loader"**: PY6b's (a).
  `sys.implementation.cache_tag` is `"arbace-314"`, so cached code is `X.arbace-314.pyc`. The magic number is 'a' plus a
  byte of the IR's digest. `dont_write_bytecode` stays True by default, as CPython's `-B`. Neither interpreter reads the
  other's code: code of another IR is refused at load. test_importlib's `test_magic_number` (it expects CPython's 3627)
  is a design class.
- **An empty built-in `_testmultiphase`**, so test_importlib's util imports and its methods run. A method that needs the
  extension's contents fails into the class that names it (`test/python/lib-test-known.edn`'s `:extension-modules`).
  pyi loads no extension modules (§6.6, the `_test*` modules deferred). Built by PY6-core,
  `go/arbace/py/builtins_testmultiphase.ae`.

## Decided (2026-11-04, the user): PY6e's and PY6g's DECISIONs (queue 200)

- **No `os.fork`** (PY6e's DECISION (a)). Go's runtime is multithreaded, so a forked child could not go on running
  Python. pyi's processes are created by `_posixsubprocess.fork_exec`, `os.posix_spawn`/`posix_spawnp` and `os.spawn*`,
  all over Go's `syscall.ForkExec`. `test.support`'s `has_fork_support` is False. The tests that need a fork skip and
  are classed by design (`test/python/lib-test-known.edn`'s `:fork`). subprocess's `preexec_fn` raises RuntimeError
  (no Python can run between fork and exec).
- **The pointer system calls are a design class** (PY6e2's proposal, `:pointer-syscall`). Go forms take no pointer for
  a raw `syscall.Syscall`, so the posix functions that need one are absent and their tests skip where CPython runs
  them:
  - `sched_setaffinity`;
  - `openpty`;
  - `memfd_create`;
  - the xattr functions;
  - `prlimit`;
  - `tcgetpgrp`/`tcsetpgrp`;
  - `unshare`/`setns`.
- **`_decimal` and `_datetime` stay absent** (PY6g's DECISION 1). decimal and datetime run as their pure-Python twins
  (`_pydecimal`, `_pydatetime`), as pickle and json do (§6.6, PYS11). The tests of their C variants are classed by
  design (`:c-accelerators`), as are those of `_pickle` and `_json`.

## As built: PY7 (the check, 2026-11-04, the Mac VM, queue 201)

PY7's check over Lib/test, on `mac/201-python-py7check` (base 9e41dc00, PY6 closed). The VM ran samples only (the user,
2026-11-04); the record and the cover run are the main host's night.

**The files** (PY10's row builds on them):

| file | what | written by |
|---|---|---|
| `test/python/divergences.edn` | the classes, GR10's format: a vector of `{:id :title :kind :owner :why :decided :match :tests}` | by hand |
| `test/python/check-lib-test-linux-amd64.edn` | the record: every method `["MODULE/TEST-ID" :RESULT "REASON" :CORPUS]` | `bin/pyi check --record`, the main host |
| `test/python/controls.edn` | the negative controls, crossed pairs `{:id :subject :reference :why}` | by hand |
| `test/python/corpora.edn` | each lib-test corpus's `:subset` (method ids), lib-test-all's in the top-level map `:lib-test-all` | `bin/pyi cover subset --write` |

- **A row.** RESULT is `:pass` when pyi's outcome is CPython's, else the first word of the method's line (`import`,
  `stopped-in`, `crash`, `not-run`, `ok`, `skipped`, `failure`, `error`, `timeout`) and REASON the rest. A non-pass's
  class is the first class of the vector that names it in `:tests` or one of whose `:match` finds "RESULT REASON":
  `test/reap/main.ae`'s, `test/cover/classes.ae`'s and `java.check`'s rule, so `bin/reap`'s `:results` reader reads the
  record as it is and `bin/cover classes` reads a run's `results.tsv`.
- **The corpora** are the record's fourth field: `language-core`, `stdlib-tests`, `lib-test-asyncio` (the families' 104
  modules, ref-cpython314-lib-test.edn's and -asyncio's) and `lib-test-all` (the 425 modules of
  ref-cpython314-lib-test-all.edn that no family owns): 529 modules in all.

**Why divergences.edn replaces the known file** (not one generated from the other). The known file held the classes and a
method-to-class map; the record holds every method's result. With both, the classes' patterns and the record decide every
method's class, and the map is redundant but for the methods no pattern finds: those are the classes' `:tests`, as Java's.
A generated copy would be a second truth to keep in step, and the REAP row needs the classes in GR10's shape, which only
the replacement gives. So the 64 classes moved in the known file's writer's order (alphabetical: the order its record was
made in), and the 4,393 methods became 2,422 found by their class's patterns and 1,971 `:tests` names. One method's class
changed, from `:generators` to `:coroutines`, because its line is `:coroutines`' pattern. Two overlaps that would have
taken `:testcapi`'s methods were taken out: `:c-modules-deferred`'s `_testinternalcapi` and `:extension-modules`'
`_test[a-z_]*`. The known file's guard, that a method newly not equal fails even when a pattern would class it, is now
the record's: a method the record passes that does not pass is a regression. A new method no pattern finds fails, as
before.

**The verbs.**
- `bin/pyi check [--corpus ID,...] [--only FILE] [--record] [--judge DIR] [--oracle]` runs the universe's modules through
  PY6i's restartable runner (`test/python/libtest_run.py` for every corpus now). It judges each method against CPython's
  references, divergences.edn and the record, prints `   failing: ID` lines (the gate's `:failed-re`), then judges the
  controls.
  - `--only FILE` takes MODULE or MODULE/TEST-ID lines; the driver runs only the listed methods (`PYI_LIB_TEST_ONLY`).
  - `--judge DIR` judges a saved run again with no rerun: `RUN/results.tsv` holds the rows.
- `bin/pyi divergences [--table|--methods]` classifies the record with no run. A non-pass in no class fails, and so does
  a `:tests` name that another class takes first.
- `bin/pyi lib-test` and `lib-test-all` judge with the same judge (`test/python/libtest_judge.ae`).

**The negative controls** (GTS81 (a)) are judged as before: the subject's run is judged as the reference module with no
classes, and must fail. `--only` adds the subjects to every selection, so every rotated run judges them. Control 2 is
now test_getopt against test_filecmp's references (about 50 s each). test_list's 692 s, with two 300 s timeouts, were
too dear for a module every selection runs.

**The oracle's control of the driver on the families' modules** was run, by python3 (`check --oracle --corpus
language-core,stdlib-tests,lib-test-asyncio`). Result: 12,399 of 12,400 methods equal. The one left is
test_selectors' `test_modify_unregister` (`:ref-empty-skip`: regrtest records `skipTest("")` as a pass). So the
families can move from libtest.py to libtest_run.py.

**PY6i's 8,179 unclassed methods.** They were measured before PY6e's `_socket` and `os.chmod` merged, and 18,214 of that
run's non-passes stopped at those two. A sample was rerun at 9e41dc00's runtime: 17 of PY6i's modules (6 that stopped at
`_socket`, 4 at `os.chmod`, 2 at `str.__doc__`, `_compare_digest`, test_userdict, and the controls' four).
- Before: 315 methods, 115 equal.
- Now: 230 equal and 85 classed, none failing.
- New classes, added at the vector's end so that the converted `:tests` keep their methods: `:doctests`,
  `:module-annotations`, `:pickle-iterators` and `:function-constructor`.
- Patterns added to `:int-core`, `:eval-source`, `:pickle-core`, `:data-model`, `:testcapi`, `:tracing` and
  `:remote-debugging`, and `:tests` names where a line is generic.
- PY6i's proposed classes, added at PY6-core's integration, keep their owners. `:str-doc` should match nothing at the
  next record (round 5's fix).
- The rest of the 437 modules is the main host's record. Its `--record` lists every method of no class (exit 3).

**Finalization (PYS9), measured.**
- **pyi today.** It has no finalizer at all: `weakref.ref` holds its referent and `gc.collect()` returns 0.
- **The families' record.** Of its 14,132 methods, 97 were in `:finalization`:
  - 50 call `gc.collect()` or `support.gc_collect()` before they look, so a finalizer queue drained at `gc.collect()`
    (PYS9 (a)) would serve them. They are the new class `:finalization-collect` (deferred, the runtime's work);
  - 47 look right after the last reference goes, which needs reference counting's timing. They stay `:finalization`,
    about 0.3% of the methods.
- **Statically over all of Lib/test.** 431 test methods use weak references, `__del__`, `getrefcount` or `ResourceWarning`:
  174 with `gc.collect`, 226 without, 31 `cpython_only`.
- **Decided (the user, 2026-11-04): (a).** The 47 are classed by design: `:finalization` is `:kind :design`, with no
  reference counting, (c) or (b). Its methods stay few: CPython's own refcount tests are `cpython_only`. The 50 of
  `:finalization-collect` stay deferred, for PY7-runtime's finalizer queue (PYS9 (a): `gc.collect()` drains it).

**The coverage subset and the rotation (GT8.3, GT8.4, GTS205 (a)).**
- **Why the AOT route.** Under goi, Go's `-cover` sees goi and not the interpreted runtime. PY8's AOT compiles the driver
  over go/arbace/py printed as Go, so the runtime's blocks are Go's there.
- **`bin/pyi cover run`.** It builds the driver by the AOT with `-cover -covermode=atomic` over `arbace/py` and its main
  package, then runs the check through it. The cover build's runtime gains `tools/pyi/libtest_cover.go` (`_pyi_cover`,
  never in go/arbace/py), whose `flush` writes each test's counters into a directory of its own and clears them. So each
  method has its own block set: the module's import counts with its first test, and the meta-data is hard-linked.
- **`bin/pyi cover blocks`** turns them into block sets (`tools/covsubset blocks`).
- **`bin/pyi cover subset [--write]`** selects the subset (`bin/cover classes` and `subset`). The subset holds: every
  method of a `:deferred` class, the controls' subjects and references, one method per class, then by greedy set cover
  every covered block. With `--write` it goes into corpora.edn.
- **The rotation's universe is the record** (`:id-re` with the corpus as group 2, the ids bare). Slices are by module:
  REQUEST cc463c8d, plan.ae's `:slice-re "^([^/]+)/"`. A night's slice then starts a seventh of the 529 modules whole,
  where slicing by method would start nearly all of them.
- **Measured on a sample on the VM** (24 modules, the 17 above with test_grammar, test_patma, test_fstring, test_tstring,
  test_type_params, test_fractions and test_struct):
  - 1,021 methods ran on the cover driver, each with its own counters, at 2 to 5 s a module (on goi: 20 to 160 s).
  - They cover 11,224 of the runtime's 27,448 blocks (41%).
  - The subset is 351 methods, which reach all 11,224: 195 kept (the deferred classes' and the controls'), 4 for their
    classes, 152 by the set cover.
  - On the cover driver only, 8 methods err with `Permission denied: ''`: a child started from `sys.executable`, which the
    AOT executable leaves empty. They are judged in that run alone, never recorded.
- The cover run itself is the main host's.

## As built: PY10 (2026-11-04, the Mac VM, queue 201)

The row, as §10.1 planned it and C's was added (doc/C23-SURVEY.md §10); the steps and the milestone's preparation in
[PY11-PREP.md](PY11-PREP.md) (not applied to the gate: the main host merges and gates them).

- **`bin/reap`'s fifth row** (`test/reap/cells.edn`'s `:python`, `test/reap/main.ae`): read, the round trip's `:lines` levels
  (`roundtrip-known.edn`) and the native reader's `lex-check` and `parse-check --full` (PY9's known files), each file counted
  once, the mutants not counted (C's rule, queue 132 F3); eval, `bin/pyi check` over Lib/test (PY7's record
  `check-lib-test-linux-amd64.edn`, its classes `divergences.edn`, its methods held to the references of the modules it holds)
  and pyi on goi over the synthetic corpus and the samples (`check --aot`'s record read at its pyi runs: the reader
  `:pyi-aot`, `:column :pyi`; a record made with `--no-pyi` is a problem there), `scope-check` and `compile-check` beside;
  AOT, `check --aot`'s compiled runs (a run the rule `:as-pyi` takes, the interpreter's own divergence, classed by the eval
  classes of the same program); print, the `:full` round trip (`full-known.edn`). The checks not recorded on the branch are
  `:until` sources (the main host's records, PY9's merge). The known files count no passes until their results files exist
  (GTS219 (a)).
- **The classes files**: `test/python/divergences-read.edn`, `-print.edn` (the known files' classes in GR10's format) and
  `-synth.edn` (the synthetic corpus's 13 programs the VM's sample run found `:as-pyi`, each classed by diffing its output
  against python3's: bytes' methods, `from_number`, `reversed(dict)`, range's and memoryview's methods, `__buffer__`,
  `__bytes__`, generator `close()`'s value, `~bool`'s warning, finalization; all deferred to the runtime); PY8's
  `divergences-aot.edn` read as it is (`{:classes [...]}`, `:ids`).
- **The essential subset** `test/python/essential-aot.txt` (20 programs) and `make python-essential`: 40 s warm on the VM
  (the read part 13 s, the 20 programs compiled and on the loop 27 s; cold, the runtime's print and the stdlib's modules,
  6.6 min more). `make pyi-check`, `pyi-check-record`, `pyi-divergences` for PY7's verbs.
- **Measured on the VM** (samples, not records): `bin/pyi check --aot --no-pyi` over the 151 programs, 138 pass, 13 classed,
  0 failing, 6 min 39 s cold; `scope-check` and `compile-check` over the stdlib, 28 s and 43 s; pyi on goi 10 s of startup
  a process, then 4 to 15 times the compiled loop (PY11-PREP.md §4).
- **Decided (the user, 2026-11-04): both of PY10's DECISIONs (a).** (1) The tag does not wait for the re-seed: pyi is
  embedded at the first re-seed after it, batched with C++'s and Scheme's (PYS18). (2) The eval level's synthetic source is
  the compiled loop in every tier: `cells.edn`'s source reads `check --aot`'s loop column (`:column :loop`, `test/reap/main.ae`),
  pyi on goi's column beside it; pyi on goi's 35 min run rotates a seventh a night (PY11-PREP.md's `pyi-check-aot-goi`)
  and stays in the main host's record.

## As built: PY9a (2026-11-03, the Mac VM, queue 199a)

The first part of PY9 (§5.5, PYS4 (a)): Python's lexer in Arbace, equal to CPython's tokenizer over every corpus and the
mutants. The parser (pegen ported, its back end generating Arbace, the actions) is PY9b's.

- **The lexer**, `arbace.repr.python.lex` (`arbace/repr/python/lex.ae`), a port of CPython 3.14.10's C tokenizer:
  `Parser/lexer/lexer.c` whole (the indentation and alternate-tab stacks, the parentheses' stack and its messages, the
  numbers' paths with `verify_end_of_number`'s keywords, the string prefixes, the f-string and t-string modes of PEP 701
  and PEP 750 with their nesting, format specs and `=` debug texts, `set_ftstring_expr`'s metadata, type comments and
  `TYPE_IGNORE`), `state.c`'s token setup, `helpers.c`'s errors (CPython's messages, columns and texts),
  `readline_tokenizer.c`'s underflow (the readline path of `tokenize.tokenize`: lines split at `\n`, a fake last
  new-line, `implicit_newline`), `Python/Python-tokenize.c`'s iterator (its columns in characters, computed as the C code
  computes them, its extra-token translations, the trailing tokens, the done codes' errors) and `Lib/tokenize.py`'s
  `detect_encoding` (the BOM, PEP 263's cookie, its errors). The text is an int array of code points with each one's
  UTF-8 byte offset: the C pointers are indices, the C byte columns are kept as C keeps them for the parser's positions.
  Evaluated from the checkout on stage 0 (out of `gate_key`, PYS18 (a); `gate-key-crops-test` passes).
- **The reference**, `tools/pydump/tokdump.py`: CPython's own tokenizer, `_tokenize.TokenizerIter` over the lines
  `tokenize.tokenize` gives it, in both modes: with extra tokens (tokenize's stream) and without (the parser's: exact
  operator types, identifiers checked by PEP 3131, numbers' ends checked); each token's type, positions and string, the
  `SyntaxError` (class, message, lineno, offset, end, text) that ends a stream.
- **The check**, `bin/repr-python lex-check [--record] [--mutants N|--no-mutants] [CORPUS...]` (`make
  repr-python-lex-check`): both dumps of every file line for line, `arbace.repr.python.lexcheck`, against
  `test/python/lex-check-known.edn`, a negative control (each file against another's dump), then the mutants (2,000 files
  drawn by a seeded shuffle from the corpora checked, two mutants each of 3 edits: a token deleted, duplicated, swapped,
  a token's text or one of 24 characters inserted). On the VM (aarch64, 3 jobs): samples 6, stdlib 720, site-packages 921,
  `Lib/test` 1,152 of 1,153 equal (its four rejected files included), the other the `:charset` class (KOI8-R, by design,
  queue 176's DECISION (a)); `test/python/lex-errors/` (62 files, one a tokenizer message or path: null bytes, encodings,
  the indentation errors, every literal's message, the prefixes, the f-string messages, the limits) 62 of 62; the 3,846
  mutants 3,846 of 3,846, 2,221 of them ending in an error (35 messages). About 6.5 minutes in all, the helper's dumps
  included.
- **Not checked yet** (the parser's, PY9b): the type-comment tokens (tokenize has no such mode; the parser's second parse
  with `type_comments=True` checks them), and `ast.parse`'s own input path (the string tokenizer: `\r\n` translated, the
  decoding errors' messages), which the parser's check judges with the trees.

## As built: PY9b (2026-11-03, the Mac VM, queue 199b)

The second part of PY9 (§5.5, PYS4 (a)): Python's parser generated by pegen ported to Arbace, its trees equal to
`ast.parse`'s over every corpus. The errors of rejected files (the second pass's `invalid_` rules, `pegen_errors.c`) are
PY9c's.

- **pegen in Arbace**, `arbace.repr.python.pegen` (`arbace/repr/python/pegen.ae`): the metagrammar (a hand-written parser
  of `metagrammar.gram`'s rules over the native lexer's tokens), `grammar.py`'s items and their strings (the artificial
  rules' keys), `parser_generator.py`'s analyses (the nullable items with pegen's quirk for lookaheads, the first graph,
  its strongly connected components and their leaders: the left-recursive rules and leaders pegen finds, `attr` leading
  `{attr, name_or_attr}`), the artificial rules of groups, repeats and gathers, the keywords (numbered from 500) and soft
  keywords, and a back end in the shape of `c_generator.py`'s C: each alternative's items in order with c_generator's
  variable names and default actions, cuts, lookaheads, forced tokens, the `invalid_` alternatives guarded by the second
  pass's flag, the memo of `(memo)` rules and the left-recursion loop of the leaders. The grammar's C actions are read as
  C expressions and translated: casts and `CHECK*` dropped, `_PyAST_*` into nodes over `Parser/Python.asdl`'s fields (with
  `EXTRA`'s positions), `a->v.Name.id` into field access, the helpers into `arbace.repr.python.peg`'s functions.
- **The parser**, `arbace.repr.python.parser` (`arbace/repr/python/parser.ae`), generated and committed: `bin/repr-python
  pegen` writes it from the tarball's grammar, `pegen --check` generates it again and compares (`make
  repr-python-pegen-check`).
- **The runtime**, `arbace.repr.python.peg`: `pegen.c`'s token buffer over the native lexer (keywords typed,
  `TYPE_IGNORE` recorded), the memo, the token functions (names NFKC-normalized), and `action_helpers.c`'s helpers (the
  sequences, contexts, arguments, decorators, strings and f-/t-strings' fields with their debug texts and conversions);
  a Constant keeps its kind of value (the forms keep the spelling). The error helpers of the second pass raise "PY9c's".
- **The reader**, `arbace.repr.python.native`: ast.parse's input path (new-lines translated, a last one added, UTF-8
  checked when no encoding is declared, with CPython's message), the parse, a second parse with type comments when the
  text has one (S6), and `tools/pydump/pydump.py`'s Writer ported, in modes `:lines` and `:full` (positions, tokens), so
  the native dump is the helper's text byte for byte; `bin/repr-python read --reader native`.
- **The check**, `bin/repr-python parse-check [--record] [--full] [--mutants N|--no-mutants] [CORPUS...]`
  (`arbace.repr.python.parsecheck`, `make repr-python-parse-check`), against `test/python/parse-check-known.edn`:
  the helper's dumps (`pydump.py tree`, `--full` with `tree --full`) and the native ones byte for byte. On the VM (aarch64,
  3 jobs), in both modes: samples 6, stdlib 720, site-packages 921 of 921 equal; `Lib/test` 1,149 of 1,153 (its 4 rejected
  files: 1 equal, `badsyntax_3131.py`, the tokenizer's message; 3 `:error-message`; and `:charset`'s KOI8-R file);
  `test/python/lex-errors/` 46 equal (the accepted files, and the rejections whose message is the lexer's or the UTF-8
  check's, `bad-utf8.py` among them) and 16 `:error-message`; the 3,846 mutants (`lex-check`'s set) 656 equal (in `:full` 655, and one `:helper-error`:
  the helper's own failure on a lone CR), the other 3,190 rejected by both, `:error-message`. No file differs and none
  crashes. About 11.5 minutes in `:lines`, 13 in `:full`, the helper's dumps included. `lex-check` again after the lexer's
  fix of a non-ASCII first character's column (the parser's byte columns): unchanged, every file equal.
- **Classes** (`test/python/parse-check-known.edn`): `:error-message` (both reject, the message or position not CPython's:
  the first pass's failure reported as "invalid syntax" at the last token, the `invalid_` rules' and `pegen_errors.c`'s
  messages PY9c's), `:charset`, `:helper-error`.
- **Not yet** (PY9c): the second pass's messages (`invalid_` rules with their `RAISE_*` helpers, `pegen_errors.c`: the
  tokenizer's errors mapped, "'(' was never closed", the generic error's location), `decode_str`'s other errors (an
  unknown or wrong encoding are tokenize's messages so far), the start rules other than `file` untried; then `_ast` and
  `_tokenize` in pyi on the native reader.

## As built: PY9 (errors, pyi; 2026-11-04, the Mac VM, queue 201)

PY9's rest (§5.5, PYS4 (a)): the native parser's syntax errors equal to CPython's, `_ast` and `_tokenize` in pyi on the
native reader, and `read --reader native` the default.

- **The errors** (`arbace.repr.python.peg`, `native`): `pegen_errors.c` ported: `_PyPegen_run_parser`'s second pass
  (the memo cleared, the `invalid_` rules on; an error raised in the first pass ends it at once, as `error_indicator`
  does), `_Pypegen_set_syntax_error` (the generic "invalid syntax" at the first pass's last token, unexpected
  indent/unindent, the EOF cases), `_PyPegen_tokenize_full_source_to_check_for_errors` (a bracket left open on an
  earlier line, a later tokenizer error, outside an f-string), `_Pypegen_tokenizer_error` (the done codes the
  tokenizer sets without raising), `_PyPegen_raise_error` and `_raise_error_known_location` (the error's line from
  the string tokenizer's buffers, byte offsets made characters as `_PyPegen_byte_offset_to_character_offset` does);
  pegen.h's `RAISE_*` macros (the generator now passes the parser, pegen.ae) and `action_helpers.c`'s error helpers
  (`_PyPegen_get_expr_name`, `get_invalid_target`, `arguments_parsing_error`, `nonparen_genexp_in_call`, ...);
  `string_parser.c`'s decoding errors ("(unicode error) 'unicodeescape' codec ...", "(value error) invalid \x
  escape ...", bytes' non-ASCII characters, f-string parts at their constant); `decode_str` (the coding specs of the
  first two lines, BOM conflicts, unknown encodings and strict decoding errors as `_PyPegen_raise_tokenizer_init_error`
  makes them, UTF-8 checked otherwise; NUL as `_Py_SourceAsString`'s error); the barry_as_FLUFL future (the lexer's
  `<>`) and `future.c`'s feature check that compile()'s AST preprocessing runs. The lexer reads in the string
  tokenizer's way for the parser (tok->buf moved only on a line read).
- **The check**: `parse-check` judges an error by its line (class, message, position, text); the class
  `:error-message` is gone. A new corpus `syntax-errors`: CPython's own syntax-error cases (`tools/pydump/syntax_cases.py`
  from the tarball's Lib/test: test_syntax's doctests, the sources test_exceptions', test_fstring's and test_grammar's
  error checks compile; 874 files). Samples on the VM (aarch64), both modes: lex-errors 62 of 62, syntax-errors 872 of
  874 (2 `:charset`: cp1251 and iso8859-7 coding specs), Lib/test's rejected files 4 of 4, stdlib 720 and
  site-packages 921 of 921 with 292 and 284 mutants (seed 26, 150 files' pools), all equal.
- **pyi** (`arbace.repr.python.astjson`): compile()'s PyCF_ONLY_AST answered from the native tree in
  `tools/pyi/ast_json.py`'s JSON (literal values decoded, strings' parts folded as `_build_concatenated_str`,
  Python.asdl's fields, positions and sequences emitted by pegen into parser.ae), the modes exec, eval, single
  (one statement) and func_type, incomplete input; `_tokenize.TokenizerIter` (go/arbace/py's `builtins_tokenize.go`)
  over the native lexer through the compiler's mode `tokenize:EXTRA`; pyi's compiler reads with the native reader.
  `bin/repr-python ast-check` compares the answers with CPython's compile() (`tools/pydump/astdump.py`) byte for
  byte: samples 6, lex-errors 58, syntax-errors 864, stdlib's and Lib/test's first 150 files (148 UTF-8) all equal.
  `test/python/pyi/f-ast-tokenize.py` runs equal to CPython. arbace.python.run's change is a REQUEST to PY7-runtime.
- `feature_version` holds the grammar's CHECK_VERSIONs and the numbers' underscores (compile()'s `_feature_version`).
- **Not yet**: optimize > 0 is ignored; compile() of an AST object has no `PyAST_Validate` (test_ast: 113 of 217 equal); a codec other than UTF-8, ASCII and
  Latin-1 has no strict decoding errors; TokenizerIter reads its readline to the end first; `\N{}` names are the
  runtime's Unicode data.

## As built: PY7-runtime (2026-11-04, the Mac VM, queue 201)

The runtime causes PY6's close and PY6i left open, each with a program of `test/python/pyi/` (`p7-*.py`) equal to
CPython 3.14.10:

- **The C level's recursion check.** CPython 3.14 guards its recursive built-in calls with `_Py_EnterRecursiveCall`
  and measures the machine's stack; pyi's built-ins recurse on Go's stack (a goroutine's at most 1 GB), which a deep
  structure overflowed (a fatal Go error, PY6i's 10 driver crashes). pyi counts the guarded levels per thread
  (`Thread.cdepth`, 10,000 of them): comparison, repr, str, a tuple's hash, isinstance's and issubclass's recursions,
  and Go's calls into the machine (`Thread.run`, the eval loop entered from C). The message is CPython's form,
  "Stack overflow (used N kB)WHERE", N a nominal 100 bytes a level (CPython's own N is the host's). The Python
  frames' limit (`sys.setrecursionlimit`) is unchanged. test_re's test_stack_overflow is left: sre.go's matcher
  recurses per repetition.
- **Constants and docstrings:** `\N{NAME}` escapes the compiler's host does not know (aliases) resolved by the
  runtime's Unicode data; docstrings of implicitly concatenated strs; a lambda's and a generator expression's lines.
- **The data model's gaps:** `pow(a, b, m)` as `ternary_op`; an unbound built-in method called with no argument;
  `dict.fromkeys` a class method; `dict.__reversed__`; `range.count`/`index`; bytes' `center`, `expandtabs`,
  `istitle`; `float`/`complex.from_number`; memoryview's 1-D attributes, `count`, `index`, `toreadonly`; `__bytes__`
  and `__buffer__` (PEP 688); `~bool`'s DeprecationWarning; `next()` of a generator with its return value;
  `_operator._compare_digest`.
- **The system calls:** `os.waitid`'s `WNOWAIT` without the pointer system call (`/proc/PID/stat`'s zombie status and
  a pidfd waited on by epoll), so asyncio's subprocesses end; `os.sendfile` releases the GIL.
- **The GIL's mutex** is a struct field again (goi takes a field's address as gc does, main 35b713ae).
- **Finalization (PYS9 (a)) is not built yet:** goi does not run a `runtime.SetFinalizer` finalizer (a probe:
  an object dropped, `runtime.GC()` twice, a second's wait, the finalizer never ran), so the finalizer queue
  `:finalization-collect` needs goi's finalizers first, or a mark phase of pyi's own at `gc.collect()`.

## As built: 202a, PY6's open items (2026-11-04, the Mac VM, queue 202)

- **_sre's matcher is iterative, as sre_lib.h's** (`go/arbace/py/sre.go`). The continuation-passing matcher recursed
  on Go's stack once per repetition (test_re's test_stack_overflow, `:recursion-limit`). SRE(match)'s DO_JUMP contexts
  are now an explicit stack of backtracking points and its DO_JUMP0 bodies (asserts, atomic groups, possessive
  repeats' iterations) a stack of frames; a repetition costs heap. The port keeps sre's bookkeeping exactly:
  LASTMARK_SAVE and MARK_PUSH where sre does them (marks only inside a repeat for most ops), MARK clearing the
  skipped marks, GROUPREF and GROUPREF_EXISTS seeing only marks below lastmark, POSSESSIVE_REPEAT going on past its
  body's SUCCESS (the old matcher accepted there), the case-insensitive ops on the lower case alone, LOCALE's
  `char_loc_ignore` and `charset_loc_ignore`, and the case mappings and categories of CPython's Unicode database.
  A differential harness (the printed runtime's package with a Go test running sre.go on code compiled by CPython's
  `re._compiler`, against CPython's spans and lastindex) found 0 differences in about 2.2 million cases; the old
  matcher differed on possessive repeats and case-insensitive sets.
- **_sre's objects as sre.c's** (`sre_obj.go`): a mutable subject read again by `group()`, `match_getindex`'s
  `__index__`, `pattern_repr`'s `%.200R`. test_re: 140 equal and 12 unclassed at the base, 151 equal and 0 unclassed
  after (with the mappingproxy REQUEST); `:recursion-limit` emptied.
- **The three `:run-hangs` tests** pass alone and in their modules' runs (test_asyncio.test_sock_lowlevel 45 of 45,
  test_asyncio.test_subprocess 86 of 86). test_huge_content's cause: `bytes(x)` did not call `x.__bytes__`, so
  wsgiref's `send_headers` (`bytes(self.headers)`, `wsgiref.headers.Headers.__bytes__`) raised in the test server's
  thread after `headers_sent`; no response came. 8013d5bb's bytes.go hunk fixed it (bisected by file).
- **Three-argument pow** holds on test_decimal's power tests; PyCoverage.test_implicit_context then met
  `sys.set_int_max_str_digits`, a no-op (REQUEST for the core owner, with the mappingproxy comparison).
- **lib-test-all's unclassed methods** on a sample (`bin/pyi check --only`, 2 jobs): the families' 15 modules and
  the two asyncio modules above, 861 methods, 824 equal, 37 classed, 5 of no class before (test_ucn's 4 `\N{NAME}`
  names, `:ucn-names`, the native reader's; test_queue's one timeout, `:slow-on-goi`); 16 other modules, 859 methods,
  401 equal, 458 classed after: test_configparser does not import under the native reader (`'\N{RS}'`, an alias,
  `:ucn-names`, 383 methods); `_heapq` and `readline` absent (`:c-modules-deferred`); abc's, super's and
  rlcompleter's data-model gaps (`:data-model`); a dict subclass's `__getitem__` that skipped `__missing__` (a
  REQUEST, which clears test_http_cookies' 3 and test_wsgiref's 2); a signal that cannot interrupt Go's `send()`
  (`:signal-timing`).

## As built: 202c (the reader, `_ast`; 2026-11-04, the Mac VM, queue 202)

The native reader stays `bin/repr-python read`'s default (PY9, queue 201): on the VM's samples (a few files of each
read corpus, lex-errors and syntax-errors whole, 92 mutants of seed 26) its trees and errors are CPython's in both modes;
the whole-corpus `parse-check` with the mutants is the main host's night run. Then `_ast`'s object model:

- **compile() of an AST object** (go/arbace/py's `builtins_ast_parse.go`): converted as Python-ast.c's `obj2ast_*`
  functions convert it (Python.asdl's fields and attributes from `astTable`: required fields, lists, identifiers,
  strings, ints, the sums' "expected some sort of ..." and the constructors' "field 'X' is required for Y"), checked by a
  port of Python/ast.c's `_PyAST_Validate` (contexts, positions, empty bodies, patterns, constants), the mode's node
  (`PyAst_CheckMode`); with `PyCF_ONLY_AST` the tree is preprocessed and made objects again (`PyAST_mod2obj`), else
  unparsed and compiled as before (PY6h's design).
- **Python/ast_preprocess.c** over the same tree: `ast.parse(optimize=1)` (`PyCF_OPTIMIZED_AST`) folds `__debug__`, a
  str's `%` with a tuple of simple `s`/`r`/`a` units into an f-string, a pattern's `-C` and `C1 +/- C2`; optimize 2
  removes docstrings (a lone one becomes `pass`).
- **The node classes** (`builtins_ast.go`): `ast_type_init`'s 3.14 rules (a field not given: optional None, a list [],
  `expr_context` Load(), else the DeprecationWarning; a keyword neither field nor attribute warned; "missing from
  _field_types"), `__replace__` (`ast_type_replace`), `__reduce__` (`ast_type_reduce`), the asdl signatures as
  `__doc__`, `ast_repr_max_depth`, AST's methods bound as method descriptors, `ast.AST`'s tp_name in messages, the
  parser's identifiers interned.
- **SyntaxError._metadata**: the second pass records the last compound statement's start
  (`_PyPegen_register_stmts`), the SyntaxError carries `(line, column, source)` (`_PyPegen_set_syntax_error_metadata`;
  the compiler's answer kind `m`), and a module `_suggestions` (Python/suggestions.c): traceback's keyword typos
  ("invalid syntax. Did you mean 'for'?") as CPython's. `ast-check`'s reference writes CPython's `_metadata` too:
  lex-errors 58 and syntax-errors 864 equal with it.
- **\N{NAME}** (`arbace.repr.python.ucn`, a follow-up from 202a): the native reader resolved names by the host's
  `Character/codePointOf`, which knows neither the aliases nor CPython's derived names (test_configparser's `'\N{RS}'`
  did not import). It now ports `unicodedata.c`'s `_getcode` over pyi's own tables (`ucdBlob`, read once from the
  checkout's Go forms): names, aliases, Hangul syllables, CJK and Tangut derived names; named sequences refused.
  test_configparser 381 of 383 (2 `:recursion-limit`), test_ucn 18 of 18.
- Samples (the VM, 2 jobs): test_ast 122 -> 201 of 217 methods equal (`bin/pyi lib-test-all test_ast`; the rest
  classed `:ast-objects` and `:cpython-only`); test_syntax's doctest 25 -> 1 example failing of 510 (all 23 keyword
  suggestions pass; left: a symtable error printed with its file, "no binding for nonlocal"); `bin/pyi lib-test
  test_syntax test_exceptions` unchanged, 117 of 157 equal, 40 classed.
- **Not yet**: the code objects' constants are not interned (`node.id is 'x'` inside a function), a `**{object(): ...}`
  keyword is refused by the call before `__init__` sees it, `b''`'s identity, AST's garbage collection, the int digits
  limit at parse time (`ast.literal_eval('3'*4001)`), test_pickling's time; compile()'s mode `single` of several
  statements gives the parser's error, not "multiple statements found".

## As built: pyexpat (2026-11-04, the Mac VM, queue 202e)

The user's decision on PY7-runtime's DECISION (2026-11-04): (a), expat ported to Go forms. Built:

- **`go/arbace/expat`** (package `arbace/expat`, about 17,300 lines of Go with its comments and tables): Expat 2.9.0 as CPython 3.14.10 vendors it
  (`Modules/expat/`; configuration of its `expat_config.h`: XML_NS, XML_DTD, XML_GE, XML_CONTEXT_BYTES 1024),
  `xmlparse.c`, `xmltok.c`, `xmltok_impl.c`, `xmltok_ns.c`, `xmlrole.c`, `xcs.c`, `siphash.h` and the headers, statement for
  statement under C's names. C's integer types are kept (int is int32, size_t uint64, ...). A `char *` is a `Ptr`, an
  allocation and an offset, so C's pointer arithmetic, comparisons and NULL survive. The allocation tracker
  (`expat_malloc`) accounts C's LP64 sizes (measured with gcc), so its amplification limits refuse what C's refuse.
  The encodings (UTF-8, UTF-16LE/BE, Latin-1, ASCII, an unknown encoding by its map) are expat's tables.
  `xmltok_impl.c`, which C instantiates three times, is one copy whose macros dispatch on the encoding's kind. One C
  undefined behaviour is modelled: epilogProcessor's `NULL - s`, which the tracker tolerates in C, is computed with a
  heap-like address. Not in any executable: out of `gate_key` and the audit's `:gate`, as `go/arbace/libm`.
- **`go/arbace/pyexpat`** (package `arbace/pyexpat`): `Modules/pyexpat.c` over it. Includes the xmlparser type with its 22 handlers,
  `buffer_text`, the ordered and specified attributes, `intern`, the reentrancy guard, a handler's exception stopping
  the parser, and the unknown encoding handler from a codec. Also `ErrorString`, `ExpatError`, the `errors` and `model`
  submodules, `features`, `version_info`, and the `expat_CAPI` capsule (C's `PyExpat_CAPI`, the functions `_elementtree`
  would call: a comment lists them; `_elementtree` is not ported, ElementTree runs its Python).
- **Loaded on the first `import pyexpat`** (the user's decision on 202e's DECISION, (a)): `arbace/pyexpat` is a package of
  its own over `arbace/py` and `arbace/expat`, not linked into py, so a run that never imports pyexpat never loads
  expat's forms (they cost goi about 2.8 s of every start). py names it in `lazyModules`
  (`builtins_pyexpat_api.go`): its first import calls the host's `SetPackageLoader` callback (arbace.python.run
  requires `go.arbace.pyexpat`), whose init registers the module (`RegisterBuiltinModule`); py then readies the types
  the package registered (`readyLoadedTypes`, readyTypes' passes over the late ones). The package reaches py's
  helpers through exported `Ext*` wrappers. A compiled program imports it blank (arbace.python.aot's main), so AOT
  links it as before.
- **Checks** (samples, run on the VM):
  - Go against C: the vendored C compiled by gcc and the port, through one driver each, every event with its line,
    column and byte index, the errors' codes and positions. 14,385 parses of 3,638 documents equal: the tarball's XML
    files (`Lib/test/xmltestdata`, `Modules/_xxtestfuzz`'s etree corpus, PCbuild, Tools/msi), 80 hand-written
    documents (DTDs, entities, namespaces, encodings, errors), and 3,300 seeded mutants, whole, in 7-byte chunks and
    byte by byte, with and without namespaces.
  - pyi against CPython: the same 638 documents of the first corpus through pyexpat in 5 configurations, every
    handler's event with the parser's position, the errors and the external entity parsers. 3,057 runs equal to
    CPython 3.14.10 with the tarball's own pyexpat and expat 2.9.0 built by gcc (the host's python3 links expat 2.10.0,
    equal on this corpus too); on goi and through pyi's AOT.
  - Lib/test, 2 jobs: test_pyexpat, test_xml_etree (ElementTree's Python on pyexpat), test_minidom and test_sax: 644
    methods, from 0 equal (453 in `:pyexpat`, test_sax's 191 skipped "no XML parsers available") to 632 equal, 3
    classed, 9 failing. No failing method is pyexpat's: list.remove and slice assignment with an `__eq__` or
    `__index__` that mutates the list panic in pyi's list (6 of test_xml_etree's BadElementTest), weak references'
    callbacks after `del` (test_weakref, test_cyclic_gc: PYS9's finalization), 500,000 nested SubElements past the
    300 s timeout (test_deeply_nested_deepcopy), and test_error_path_no_crash, which needs `_testcapi`'s memory-error
    injection as its class's other two tests do.

## As built: pyi's speed (2026-11-04, the Mac VM, queue 202b)

EVAL-SPEED's lessons (origin/eval-speed, doc/go/EVAL-SPEED.md: a call's cost, allocations, inline caches of the
lookups a call site repeats) applied where Go's profile (`PYI_CPUPROFILE` on a compiled benchmark, `go tool pprof`)
put pyi's time. Before them, on richards compiled: Go's collector 25-30%, a call's frame (the frame, its locals, a
stack grown by append from nothing, the arguments copied twice) 15-20%, attribute lookup 17% (lookupAttr's map by
name, then the instance dict's Go map), the dispatch through AotExec for load-attr and store-attr; and every compiled
program spent about 0.6 s of its start in the host's compiler, started only to read its own IR cache. The changes,
each measured before and after on the sample (least CPU seconds of 3 alternated runs) and kept only when it paid:

| change | where | measured on the VM (compiled) |
|---|---|---|
| a compiled program reads the host's IR cache itself (`compiler.sh`'s `# ir-cache: DIR DIGEST`) | aot.go, bin/pyi | -0.6 s every run |
| frames: locals and stack one buffer (`Code.stackHint`); a returned frame nothing can reach reused (per code, at most 4; escaped by `sys._getframe`, tracebacks, `gi_frame`, `f_back`, signal handlers) | frame.go, eval.go | 25-37% |
| inline caches of load-attr/store-attr/delete-attr by (type, typeEpoch) per instruction; `co_names` interned | attr.go, load.go | 3-11% |
| calls bind a Python function's arguments where they are on the stack (self in the callable's slot), keyword names made once | eval.go (`callStack`), aot.go | 17% (richards, deltablue) |
| exact int/float operations, int/float comparisons, list/tuple items without the slots' lookups | abstract.go, eval.go | nbody 22% |
| Go's collector at GOGC 400 under a 2 GiB soft limit (GOGC, GOMEMLIMIT override) | aot.go | 20-30%, peak RSS about 2x (64 -> 120 MB) |
| load-attr/store-attr compiled to `AotLoadAttr`/`AotStoreAttr`, not through AotExec | aot.ae, aot.go | raytrace 8% |
| dict: the index a Go map to each hash's first entry, the rest chained | dict.go | goi-neutral (see below) |

The dict was first an open-addressing table with no index up to 8 entries (4b51f741): 3-11% on the compiled sample,
but deltablue on goi about 15% slower (goi interprets the scans and probes, where a Go map's lookup is one native
call); the chained index (05453791) is goi-neutral and 2-14% behind open addressing compiled. Open addressing is the
better choice once pyi is compiled into stage 0 (the re-seed after Python's tag).

**The table** (`bin/pyi bench`, the process's milliseconds, two runs each; aarch64 VM shared with other tasks, load
5-30: differences under 10-15% are noise; CPython 3.14.10; pyi on goi: the benchmark's own seconds of one loop,
`time.perf_counter` around it, deltablue 5 loops, the base and this branch alternated):

| benchmark | loops | CPython | compiled before | compiled after | loop before | loop after | pyi on goi before | pyi on goi after |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| richards | 10 | 324-331 | 3,132-3,186 | 1,245-1,249 | 3,481-3,497 | 1,429-1,479 | 23.5-25.6 s | 23.5-27.9 s |
| deltablue | 50 | 130-134 | 1,829-2,030 | 670-691 | 1,924-2,015 | 686-705 | 11.3-12.7 s | 11.3-13.4 s |
| nbody | 3 | 302-311 | 1,658-1,801 | 534-567 | 1,747-1,821 | 563-568 | 53.7-60.2 s | 39.3 s |
| float | 4 | 223-232 | 1,793-1,819 | 640-689 | 1,797-2,062 | 690-695 | 31.5-32.8 s | 30.8 s |
| raytrace | 1 | 208-210 | 2,412-2,458 | 900-914 | 2,413-2,561 | 933-946 | 134.7-153.3 s | 126.4 s |

Compiled, the sample went from 5.5-9.6 times CPython to 1.8-4.3 times (the process, the start included); the loop
(`PYI_AOT_LOOP`) gained as much, the runtime being the same. On goi (this branch's head; the pairs alternated, so a range is the load's) nbody gained about 30% and raytrace a little,
richards, float and deltablue are within the noise: goi's own costs
(its calls, its closures) dominate what pyi's runtime does, and its start (about 20-26 s, stage 0 loading the runtime's
forms) is untouched; there GOMEMLIMIT is what binds (`PYI_GOMEMLIMIT=3GiB` took nbody's start from 26-28 s to 19-21 s,
peak RSS 1.5 -> 2.3 GB: 202b's DECISION, decided: the user, 2026-11-04, (b): bin/pyi's `PYI_GOMEMLIMIT` defaults to
3GiB since the integration, each pyi process peaking at about 2.3 GB, times `PYI_LIB_TEST_JOBS`).

The main host's table (amd64): `bin/pyi bench bm_richards bm_deltablue bm_nbody bm_float bm_raytrace` on this branch and
on its base 45ce1e92 (a worktree), and `bin/pyi bench --pyi ...` for pyi's whole process on goi; `bin/pyi bench --record`
over all 71 rewrites `test/python/bench-aot-linux-amd64.edn` (the night's).

Not taken (the profile's next items, for later): a load-method form in the IR (a method call still makes a bound method
for the call to unwrap; CPython's LOAD_ATTR method variant), global loads cached by the dicts' keys version, a fused
to-bool/jump (AotToBool pushes a bool AotTruthy pops), frame reuse for generator frames.

## As built: 203, 202's lows (2026-11-05, the Mac VM, queue 203)

- **compile()'s mode single is parsed as CPython parses it.** `arbace.python.run`'s compiler (modes `single` and
  `single-complete`) asks the native reader's single parse, `arbace.repr.python.astjson/single-error` (the start
  rule `interactive`, then Parser/pegen.c's `bad_single_statement`). Its "multiple statements found while compiling a
  single statement" comes ahead of codeop's rule, because CPython's parser stops after the first statement and the
  rest may be broken or unfinished. Its other errors come after that rule, for example a compound statement followed
  by another (invalid syntax at the second). The location is the first statement's NEWLINE token, as CPython gives
  it. The reader's port gained two of CPython's single-input rules:
  - `_PyPegen_fill_token`'s end of input: the ENDMARKER is read as a NEWLINE once parsing has started, and the open
    blocks are dedented unless `PyPARSE_DONT_IMPLY_DEDENT` (`peg/set-single`, `lex/imply-dedent!`). Without it,
    `statement_newline`'s `compound_stmt NEWLINE` never matched at the end of the source.
  - The string tokenizer adds no new-line to non-exec input (`lex/lexer`'s `no-fake`), so
    `compile('def f(): pass', f, 'single')` is CPython's invalid syntax at (1, 0).

  The AST mode passes `PyCF_DONT_IMPLY_DEDENT`. Checked against `python3 -I` on 34 sources in mode single
  (message, lineno, offset, end, text), all equal, and on mode single's AST with and without `PyCF_DONT_IMPLY_DEDENT`.
  With `PyCF_ALLOW_INCOMPLETE_INPUT` (codeop's flags), pyi's approximated incomplete-input rule still differs; see the
  lows in 203's report. test_compile's test_bad_single_statement passes. test_codeop and test_compile otherwise
  fail the same methods as at the base.
- **test_dict: 10 failing methods at the base, 0 now.** Seven are fixed in the runtime:
  - setdefault hashes the key and looks it up once;
  - update from a dict raises "dict mutated during update" when a key's `__eq__` changes the other dict;
  - update from a sequence raises PySequence_Fast's "object is not iterable", with `__notes__`;
  - the views are compared as `dictview_richcompare` compares them: the sizes, then containment item by item, with no
    set made;
  - the views' `mapping` is a mappingproxy of the dict;
  - getattr and the other attribute builtins take an instance of a str subclass;
  - the dict iterators count the items they still expect (`di->len`): "keys changed" is raised only on an item past
    that count, and a reverse iterator ends past the entries of a dict refilled since. Before, any change of the keys
    raised. A compaction since the iterator was made still counts as a keys change.

  Three are classed. test_container_iterator and test_free_after_iterating go in `:finalization-collect` (they call
  gc.collect before they look). test_oob_indexing_dictiter_iternextitem goes in `:finalization`: it needs `__del__` to
  run when the last reference goes.
- **test_list's test_list_index_modifing_operand**: an extended slice's length is taken after the assigned value is
  converted (`list_ass_subscript`, gh-120384).
- **The sample**: `bin/pyi lib-test-all --fresh test_dict test_list`, 190 methods: 163 equal, 27 classed, 0 failing
  (the base: 155, 24, 11).

## As built: 204, PYS22 (a)'s first Go ports, `_csv` and `mmap` (2026-11-05, the Mac VM, queue 204)

The user's decision PYS22 (a) (2026-11-04): the stdlib's C modules by module, Go ports first. Both modules are files of
pyi's runtime (`go/arbace/py`, loaded with it; together about 1,900 lines of Go), ported by hand from the release tarball
`Python-3.14.10.tar.xz` (SHA-256 `de414a57119abef52784d8993e341d53aed635fc2b05c2607d9f98a2c5b72032`, the survey's pin),
under the PSF License Agreement (LICENSE.md's entry for them). `Lib/csv.py` and the tests run unchanged.

- **`_csv`** (`go/arbace/py/builtins_csv.ae`, from `Modules/_csv.c` and `Modules/clinic/_csv.c.h`): function for function
  under C's names. `dialect_new` with its `_set_*` converters, `dialect_check_*`, the reuse of a Dialect instance and
  getargs' unnamed-format messages. The reader's `parse_process_char` state machine, with `parse_save_field`'s
  QUOTE_NONNUMERIC/STRINGS/NOTNULL fields and `parse_add_char`'s field limit. The writer's `join_append_data`, which
  counts and then copies, `join_check_rec_size` and the empty-field rules. Also the registry, `field_size_limit`,
  `_csv.Error`, `Reader`/`Writer` not instantiable, and `Dialect.__reduce_ex__` refusing to pickle. A Py_UCS4 is a
  rune; NOT_SET and EOL are -1 and -2.
  - *Deviation:* the module state is one per process. The types are runtime built-ins, not heap types made per module
    object.
- **`mmap`** (`go/arbace/py/builtins_mmap.ae`, from `Modules/mmapmodule.c`'s UNIX paths and its clinic file): over Go's
  `syscall` package, no cgo, no `unsafe`.
  - **Mapping and access:** `syscall.Mmap`; the fd duplicated with `F_DUPFD_CLOEXEC`; the regular file's size checks.
  - **Methods:** reading, readline, write, write_byte, find/rfind (`_PyBytes_Find`'s offsets), seek, move, size (fstat),
    and the context manager.
  - **flush:** `msync(MS_SYNC)` at the slice's address, obtained with `reflect`'s `Pointer`. So `flush(1, n)` is EINVAL,
    as on Linux.
  - **madvise:** `syscall.Madvise`.
  - **Subscripts and assignment:** `__index__` and `__buffer__` run before the bounds are checked, as gh-157335's fix
    has it.
  - **Iteration:** over sq_item, yielding length-1 bytes.
  - **Constants:** Linux's (amd64 and arm64 alike); `PAGESIZE` is `syscall.Getpagesize()`.
  - **Deviations:**
    - *resize:* Go's syscall has no mremap. Shrinking shortens the window in place. Growing makes a new mapping;
      a private mapping's bytes are copied, a shared one's come from the file. Linux's refusals are kept (a shared
      anonymous mapping cannot grow; a size of 0 is EINVAL after the ftruncate).
    - *Unmapping:* a mapping is never unmapped while the process runs. `close()` and a growing resize put an
      anonymous read-only zero mapping over the old pages (`MAP_FIXED`). The file's pages are released (the
      program's `/proc/self/maps` check), and a Go slice of them that a caller still holds reads zeros instead of
      faulting the process.
    - *Buffers:* pyi's buffer protocol has no export count, so `close()` and `resize()` never raise BufferError
      (decided: the user, 2026-11-05, 204's DECISION (a)). A memoryview used after its mmap was closed raises ValueError
      "mmap closed or invalid": memoryview's accessors ask the exporter's `BufferValid()` (`MemoryView.unusable`, the
      REQUEST commit for PY7's owner). `bytes(view)` then gives `b''`, because `BytesLike` cannot raise.
    - No `sys.audit` event.
  - **Runtime hook:** writableBuffer's last case asks an exporter's `WritableBytes()` (the REQUEST commit in
    `builtins_binascii.ae`), so `memoryview(m)[i] = v` writes into the mapping.
  - **Tamago:** pyi's runtime is read for linux (`runtime read --goos linux`) and does not run on tamago. A tamago
    build of it would need these syscalls (mmap, msync, madvise, ftruncate) from the board, and has none.

Samples (run on the VM, `bin/pyi lib-test-all --fresh test_csv test_mmap`, 2 jobs; no record written):

| module | before | after |
|---|---|---|
| test_csv | 0 equal, 134 classed (`:c-modules-deferred`, "No module named '_csv'") | 132 equal, 2 classed (cpython-only), 0 failing |
| test_mmap | 0 equal, 53 failing (SkipTest: no module) | 52 equal, 1 classed (`test_weakref`: `:finalization-collect`), 0 failing |

The programs `test/python/pyi/d-csv.py`, `d-csv-fuzz.py` (700 generated reader and writer cases) and `d-mmap.py` print
the same as CPython's under `bin/pyi run`. `:c-modules-deferred` no longer names `_csv`.

## As built: 205 (pinned wheels and pyi's installer; PYS23-PYS25 (a); 2026-11-05, the Mac VM, queue 205)

### The design

- **The pins** (PYS23 (a)): `test/python/wheels.edn`, read with the Clojure reader, one map per wheel: `:name` (the
  distribution, PEP 503-normalized), `:version`, `:file` (the wheel's file name, PEP 427), `:sha256` and `:bytes`
  (PyPI's published digest and size, from `https://pypi.org/pypi/NAME/VERSION/json` on the day the pin was made),
  `:url` (`files.pythonhosted.org`), `:pinned` (that day), `:sample` (the program of `test/python/wheels/` that exercises
  it). A pin moves only by a recorded decision, as the tarball's and the apks' do. Only `py3-none-any` wheels are pinned
  (a compressed tag set such as `py2.py3-none-any` counts: it includes `py3-none-any`, PEP 425); nothing of a wheel is
  committed but its pin.
- **The records rule** (as the apks, GTS212 (a)): `bin/pyi wheels fetch` is the only network step: each pinned wheel
  into the shared cache, `${REPR_PYTHON_SRC:-.tmp/repr-python}/wheels/FILE`, through a temporary name, kept only when
  its SHA-256 and size are the pin's; a file already there with the pin's digest is not fetched again. Every other verb
  reads the cache and fails, naming `wheels fetch`, on a missing or different file: no network in the gate or a clean
  run. CPython's references for programs that use the wheels are its runs with the same site, on the main host (amd64);
  the VM records nothing.
- **The installer** (PYS24 (a)): `arbace.python.wheels` (`arbace/python/wheels.ae`), evaluated from the checkout on
  the tools' host like `arbace.python`, no Python and no pip. It implements the wheel format alone (PEP 427, PEP 491's
  `.data` schemes, the binary distribution format spec): the zip read by jrt's `java.util.zip.ZipFile` (the program's
  zlib, jrt's Inflater port); the tag checked first (a wheel that is not `py3-none-any` refused with its tags named);
  `WHEEL`'s `Wheel-Version` 1.x and `Root-Is-Purelib: true`; `RECORD` parsed as CSV, every file of the archive in it
  (but `RECORD` and its signatures), every row's file in the archive, each digest (sha256, sha384 or sha512, urlsafe
  base64 unpadded; md5 and sha1 refused) and size equal; no absolute path, no `..`, no file outside the scheme
  directories. Then the purelib files (the root, and `NAME.data/purelib|platlib`) laid out under the site's `lib/`,
  `NAME.data/scripts` under `bin/`, `headers` under `include/NAME/`, `data` at the site's root; `*.dist-info` written as
  the wheel has it plus `INSTALLER` (`arbace-pyi`) and `RECORD` rewritten with the installed paths and their digests, as
  pip writes them; `entry_points.txt`'s `console_scripts` as launchers in `bin/`: a shell script that puts its own
  `../lib` in `PYI_SITE` and runs pyi's `python3` (`tools/pyi/python3`) on the entry point.
- **The site** (PYS25 (a)): `${REPR_PYTHON_SRC:-.tmp/repr-python}/site-KEY/` (`lib/`, `bin/`), KEY the first 20
  characters of the SHA-256 of the pins file and the installer's source, so the worktrees share one site per pin set
  and installer; built in a temporary directory beside it and renamed into place, so a reader sees it whole or not at
  all. `bin/pyi wheels install` builds it (when absent) and prints its path; `wheels verify` checks every installed
  file against the site's `RECORD`s and every wheel against its pin.
- **`sys.path`**: `PYI_SITE` (directories, colon-separated) is appended after the stdlib by `bin/pyi run`, `python`
  and `repl` (`arbace.python`'s `--site`), as CPython's `site` appends site-packages after the stdlib; the environment
  carries it to pyi's children (subprocess's `sys.executable`). `.pth` files are not processed (pyi starts with no
  `site`, as `-S`); the installer refuses a wheel that ships one, so a site never depends on them.
- **The check**: `bin/pyi wheels check` installs, verifies, compares the layout with the host CPython's pip
  (`pip install --no-index --no-deps --no-compile --target`, the same wheel: every file byte for byte but the
  `dist-info`'s `INSTALLER`, `RECORD`, `REQUESTED` and `direct_url.json`), runs each pin's sample by pyi and by
  `python3 -I -S` with the same site appended to `sys.path` (stdout, status, stderr's last line equal), a console
  script through its launcher, and two negative controls: a wheel tagged for a platform refused, a wheel whose
  `RECORD` digest is wrong refused.

### As built

- `test/python/wheels.edn` pins five wheels from PyPI's JSON API on 2026-11-05: attrs 26.1.0, idna 3.20, packaging
  26.3, pyflakes 4.1.0 (`py2.py3-none-any`, console script `pyflakes`) and six 1.17.0 (`py2.py3-none-any`); 344 KB in
  all. attrs, idna and packaging are also among the 20 apks at the same versions: the wheels are their upstream
  builds. Fetched once on the VM (`bin/pyi wheels fetch`, 2026-11-05) into `.tmp/cache/python/wheels/`.
- `arbace/python/wheels.ae` as designed; `bin/pyi wheels fetch|install|verify|path|run|check|pip-probe`;
  `arbace.python`'s `--site` and `PYI_SITE`. `verify` also fails on a file of the site no `RECORD` lists (the first
  check run found CPython's reference run had written `__pycache__` into the shared site: the oracle runs `-B` since).
  Installing the five takes well under a second on stage 0.
- `bin/pyi wheels check` on the VM (aarch64, 1.5 minutes): the layout equal to `pip install --target`'s (110 `RECORD`
  rows, both console scripts), the five samples (`test/python/wheels/use_*.py`) and pyflakes' console script equal to
  CPython's, both controls refused.
- The check found two gaps of pyi's runtime, fixed (go/arbace/py, printed, edited and read back by `bin/pyi runtime`):
  `typing.Union[X, ...]` had no `__class_getitem__` (packaging failed at import, `tuple[Union[int, str], ...]`), and a
  subclass of `module` could not be instantiated (six failed at import, `_MovedItems`); `Module` carries its class
  (`Cls`), a subclass's instance is made empty and filled by `module.__init__`, its attributes looked up generically
  (its class's descriptors around the dict) and set likewise. Program `test/python/pyi/w205-union-module.py`.
- pip on pyi (`bin/pyi wheels pip-probe`, the apks' pip 26.2.1): `pip --version` and `pip install --no-index` both stop
  at import, before csv: pip's vendored rich defines `class ColorTriplet(NamedTuple)`, whose annotations
  annotationlib's FORWARDREF evaluates through `types.FunctionType(code, globals, ...)`, the recorded class "types.FunctionType
  and types.CellType called". Any `class X(NamedTuple)` with annotations stops there on pyi.
- Not done: `.data/scripts`' `#!python` rewritten but no pin exercises it; `bin/pyi aot` does not take `PYI_SITE`;
  `--build-program`'s `:python {:wheels ...}` (PYS25 (a)'s second half) waits for pyi in the executable (PYS28).
## As built: 206, the re-seed window's light check (2026-11-05, the Mac VM, queue 206)

`bin/pyi c2j-check` (AFTER-PY11.md §4.4, the REQUEST to `bin/pyi`'s owner; `make pyi-c2j-check`): the 19 namespaces of
`arbace/python{.ae,/}` and `arbace/repr/python{.ae,/}` compiled by c2j alone on stage 0, as `bin/ci c2j-check` does for C
(the units `ARBACE_COMPILE` records, `arbace.c2j.main/run`; no j2g, no Go build), under `ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1
ARBACE_PATH_FIRST=1`, so the check keeps counting the sources after PYS28's re-seed embeds them compiled (C9's trap: 0
sources). It prints `c2j-check: 19 namespaces: 19 compiled, 0 failed; 2080 classes`, a failure as `c2j-check: not
compiled: SOURCE MESSAGE` (c2j's), and fails on a namespace that does not load, on a count short of the 19, or on any
failure. Run: every namespace compiles as committed; nothing in the Python namespaces needed a change, and no c2j limit
was met. 20 s wall, 30 s CPU, 2.9 GiB peak on the VM (arm64, 8 cores).

Its negative control (GTS81 (a)), in the same run: `test/python/c2j/pyi/c2j_control.ae` (namespace `pyi.c2j-control`, on
`ARBACE_PATH` after the checkout), a macro expanding to an evaluated fn that closes over the macro's parameter, a constant
the evaluator takes and c2j refuses ("a fn closing over locals as a constant", emit.ae's `constant`). It is judged apart
from the 19: compiled ("control passed") or refused with another message ("control not judged") fails the check. An
unreadable object as a constant (a macro returning `(Object.)`) was tried first and is not a control: c2j refuses it
in `pool-encode`, after the per-source compile, as an exception that ends the whole run (print-dup's "No method in
multimethod" before c2j's own "can't embed unreadable object"), not as one source's failure (a low of c2j, the main
host's).

The proposed gate step `pyi-c2j` (essential, chain `python-essential`, stage 0, its `:inputs` as strace found them) is in
PY11-PREP.md §2's list, not applied to `test/gate/steps.edn`.

## As built: 205f (types.FunctionType's and types.CellType's constructors; 2026-11-05, the Mac VM, queue 205f)

The user's decision on 205's DECISION (a), 2026-11-05: the constructors now, as a follow-up of 205
(`mac/205-python-functiontype`, from 205's head 87deec00).

- **FunctionType(code, globals, name=None, argdefs=None, closure=None, kwdefaults=None)** (`go/arbace/py/func.ae`'s
  `functionNew`): CPython 3.14.10's `Objects/funcobject.c` `func_new`. The argument parsing and the checks come in
  the clinic's order, then `func_new_impl`'s, with their messages (code, globals, name, defaults, closure, kwdefaults,
  the closure's length as a ValueError, its items as cells). Then `PyFunction_New`: the name, qualname and docstring
  come from the code, `__module__` from the globals' `__name__`, and the builtins from the globals' `__builtins__`
  (a module gives its dict; without one, the running frame's builtins).
- **CellType([contents])**: `cellobject.c`'s `cell_new` (no keywords, at most one argument), `cell_repr`
  (`<cell at 0x..: empty>`, `<cell at 0x..: int object at 0x..>`) and `cell_richcompare` (the contents compared; an
  empty cell before anything). Cells are unhashable, as CPython's.
- **A dict subclass as globals.** `Function.GlobalsMap` and `Frame.GlobalsMap` hold the globals when they are a dict
  subclass's instance; `Globals` stays its dict.
  - LOAD_GLOBAL and LOAD_FROM_DICT_OR_GLOBALS go through its `__getitem__`, as CPython's
    `PyMapping_GetOptionalItem` does on non-exact globals, so annotationlib's `_StringifierDict.__missing__` answers.
  - LOAD_NAME's globals step uses the dict itself (`_PyEval_LoadName`'s `PyDict_GetItemRef`).
  - A function made where the globals are such a mapping keeps them (`__globals__` is the mapping).
  - exec and eval take a dict subclass as globals or locals (the locals through `Frame.LocalsMap`, as a class body's
    `__prepare__` namespace). eval's globals messages are now `builtin_eval_impl`'s.
- **What the FunctionType path made visible, also fixed:**
  - Assigning or deleting a function's `__annotations__` left `__annotate__` set (the attribute slot wrote
    `__dict__`). It now sets it to None and checks for a dict, as `func_set_annotations`.
  - A union's arguments are deduplicated as `unionobject.c`'s builder does: a hashable argument by hash, then
    equality; an unhashable one among the unhashable. Before, `Union[str, undefined]` lost the `_Stringifier`, whose
    `__eq__` answers anything.
- Program `test/python/pyi/w205f-functiontype.py` (66 lines, equal to `python3 -I -B`). It covers:
  - the constructor with closures and every error;
  - cells;
  - a `__missing__` globals;
  - an annotated `typing.NamedTuple`;
  - annotationlib's FORWARDREF and STRING formats on functions, closures and classes;
  - `__annotations__` assignment;
  - exec and eval with mapping namespaces;
  - the union's deduplication.
- The class `:function-constructor` (divergences.edn) is narrowed to what is left:
  - exec's and eval's locals given as a mapping that is not a dict subclass (its match is now
    `^error locals must be a mapping`);
  - the audit event `function.__new__` (pyi's `sys.audit` calls no hook);
  - function's `__text_signature__`.
- pip (`bin/pyi wheels pip-probe`): `pip --version` runs. `pip install --no-index` stops at urllib3's
  `class RecentlyUsedContainer(typing.Generic[_KT, _VT], typing.MutableMapping[_KT, _VT])` with "duplicate base class
  Generic". The cause: pyi's `Generic.__class_getitem__` (`typing.ae`) returns a `types.GenericAlias`, not
  `typing._generic_class_getitem`'s `typing._GenericAlias`, so its `__mro_entries__` keeps Generic where CPython's
  returns `()` before a later generic alias. This is not chased here (the brief). It is in no class yet: every
  `class X(Generic[T], SomeABC[T])` stops there.

## As built: 205g (typing.Generic's subscription as CPython's; 2026-11-05, the Mac VM, queue 205g)

The user's decision on 205f's decision 1, 2026-11-05: the small fix now (`mac/205-python-generic`, from the
integration head c3488342, 205f merged), then the pip probe again and where pip stops classed.

- **Generic** (`go/arbace/py/typing.ae`), as CPython 3.14.10's `Objects/typevarobject.c`:
  - `__class_getitem__` and `__init_subclass__` call typing's `_generic_class_getitem` and `_generic_init_subclass`
    with the class first and the arguments as given (`call_typing_args_kwargs`; `callTyping`). `Generic[K, V]` is a
    `typing._GenericAlias`, whose `__mro_entries__` gives `()` before a later generic base, so
    `class X(Generic[K, V], MutableMapping[K, V])` builds; `__parameters__` comes from `__orig_bases__` as typing
    computes it, with typing's errors (plain Generic, Generic twice, type variables not listed, ...).
  - A class statement's type parameters (`class A[T, *Ts]`, the intrinsic SUBSCRIPT_GENERIC) are
    `_Py_subscript_generic`: a TypeVarTuple unpacked as `typing.Unpack[Ts]`, then `typing._GenericAlias(Generic, params)`.
  - What typing's `_GenericAlias` asks of the type variables, which pyi's lacked: `__typing_subst__` (TypeVar's and
    ParamSpec's typing's `_typevar_subst` and `_paramspec_subst`; TypeVarTuple's "Substitution of bare TypeVarTuple is
    not supported") and `__typing_prepare_subst__` (a TypeVar's own, with its default and "Too few arguments for ...";
    ParamSpec's and TypeVarTuple's typing's). `*Ts` is `typing.Unpack[Ts]` (`unpack_iter`).
- **types.GenericAlias' iterator** (`genericaliasobject.c`'s `ga_iter`): `*tuple[int]` is one starred copy, its repr
  after a star with its arguments and its equality comparing the star first. Before, `iter(tuple[int])` fell back on
  the sequence protocol over `__getitem__` and never ended: test_genericalias hung at import.
- Program `test/python/pyi/w205g-generic.py` (42 lines, equal to `python3 -I -B`): urllib3's container shape, ABC
  subscripts, Protocol, PEP 695 classes with a TypeVarTuple, `__orig_bases__` and `__parameters__`, substitution with
  defaults, typing's error messages, starred aliases.
- pip (`bin/pyi wheels pip-probe`): `pip install --no-index` now gets past urllib3's import and stops with
  `TypeError: _idfunc() takes exactly one argument`: typing.NewType's `__call__ = _idfunc` is called by pyi's slot with
  the instance first. CPython's slot functions bind only a method descriptor (a builtin function has no `__get__`);
  pyi's `descrGet` keeps that rule (`methodDescrs`), but `callBound`, which the slots call, has no owner and binds every
  unbound `Builtin`. New class `:builtin-function-special-method` (divergences.edn); not chased here.
- Samples (`bin/pyi lib-test-all --fresh`, the VM, before c3488342 / after): test_annotationlib 109 / 112 equal
  (test_syntax_error, test_pep695_generic_class_with_future_annotations and test_stringized_annotations_with_star_unpack,
  a timeout before, now pass; 422 s / 27 s); test_type_params 91 / 94 ok (test_types_new_class_no_callback,
  test_traditional_01, _02); test_genericalias hung at import / 18 equal, 7 classed, 12 failing of 37 (types.GenericAlias'
  rest: `__unpacked__`, parameters' deduplication and chaining, pickling, a union's `__parameters__`); test_typing still
  does not import (`cannot create 'typing.TypeAliasType' instances`, the class `:type-params`). No method that passed
  before fails.

## As built: 207 (pip on pyi as an eval target, PYS24 (a); 2026-11-05, the Mac VM, queue 207)

From 205g's head (ddc8d58a), the user's decision on 205g's DECISION: (a), callBound given the owner.

- **A built-in function as a class's special method** (`go/arbace/py/attr.ae`'s `callBound`, its 21 callers in
  `class.ae`, `format.ae`, `bytes.ae`, `math_extra.ae`): `callBound(d, owner, self, args, kwnames)` takes the type of
  self's MRO the method was found on (`lookupAttr`'s second value; at the `pyDefined` callers, which know it is a class
  statement's, self's type) and applies `descrGet`'s rule: an unbound `Builtin` found on a class statement's type that is
  no method descriptor (`methodDescrs`) is called with the arguments alone, as CPython's `lookup_maybe_method` binds
  only what has `__get__`. `typing.NewType`'s `__call__ = _idfunc` (where pip install stopped since 205g), `class C:
  __call__ = len`, `__getitem__ = abs`, `__init__ = print` now behave as CPython's; `object.__repr__` or `list.__len__`
  put in a class still bind. `_typing`'s `ParamSpecArgs` and `ParamSpecKwargs`, classes made in Go with Go methods,
  register those as method descriptors (as AST's base does). Program `test/python/pyi/w207-builtin-special.py`. The
  class `:builtin-function-special-method` is removed.
- **pip** (`bin/pyi wheels pip-probe`, now a check): with that fix pip 26.2.1 (the apks' copy) runs on pyi with no
  other change. The check runs pip by pyi (`PYI_SITE`) and by `python3 -I -S -B` (runpy) with the same argv and
  directories: `--version`, `list`, `install --no-index --no-deps --no-compile --target DIR WHEEL` for each of the five
  pins, `install --no-index --find-links CACHE --no-compile --target DIR` for their five names, `list --path DIR`.
  Each run's status, stdout and stderr equal CPython's and each installed tree equals file for file (every file's
  digest, `RECORD`, `INSTALLER`, `REQUESTED`, `direct_url.json`), but two classed differences: stderr's `WARNING:
  Disabling truststore since ssl support is missing` (pyi has no `_ssl`, class `:ssl`, queue 209's module; no offline
  verb uses the trust store), and a console script's first line, `#!` and the run's `sys.executable`, with its `RECORD`
  row's digest and size (csv's `\r\n` rows). `--no-compile` on both sides: pyi compiles to
  `__pycache__/NAME.arbace-314.pyc`, its own code (class `:pyc-magic`); its `RECORD` row is otherwise pip's (no digest,
  as CPython's). Two negative controls: CPython's tree with one file changed and CPython's launcher judged against
  pyi's executable both refused. On the VM (aarch64) about 7 minutes: each pip run takes 30-60 s on pyi.
- Not done: pip with the network (by the brief), pip's wheel building from an sdist (no pin is an sdist), `pip
  uninstall` and installing into a site rather than `--target`.
## As built: 208, the C library cores through ci (PYS22 (a)'s route C): the design, and `_bz2` (2026-11-05, the Mac VM, queue 208)

PYS22 (a) (decided: the user, 2026-11-04) puts the stdlib modules whose core is a C library (`_bz2`, `_lzma`, `_zstd`,
`_sqlite3`) on Arbace's C crop: the library itself as C, built by ci, and the CPython module's glue in Go over pyi's
objects. This section is the design (asked first), then what was built: `_bz2` end to end; `_lzma` and `_zstd` probed
(their libraries through ci), `_sqlite3` designed.

### The design

**The pins.** The versions are CPython 3.14.10's own: `PCbuild/get_externals.bat` names them, and
`Misc/externals.spdx.json` pins each tarball (python/cpython-source-deps' tags of the upstream releases) by SHA-256.
They were fetched once into the main checkout's `.tmp/cache/clib/` (`bin/pyi-clib fetch LIB`, the one network step), and
each digest equals the SPDX pin:

| library | version | tarball (github.com/python/cpython-source-deps/archive/refs/tags/) | SHA-256 | license | size |
|---|---|---|---|---|---|
| bzip2 | 1.0.8 | `bzip2-1.0.8.tar.gz` | `ab8d1b0cc087c20d4c32c0e4fcf7d0c733a95da12cedc6d63b3f0a9af07427e2` | bzip2's (BSD-like, 4 clauses) | 7 library files, 5,200 lines |
| liblzma (xz) | 5.2.5 | `xz-5.2.5.tar.gz` | `a15c168e39e87d750c3dc766edc7f19bdda57dacf01e509678467eace91ad282` | public domain (liblzma; COPYING) | 69 files |
| zstd | 1.5.7 | `zstd-1.5.7.tar.gz` | `f24b52470d12f466e9fa4fcc94e6c530625ada51d7b36de7fdc6ed7e6f499c8e` | BSD 3-Clause (dual with GPLv2: BSD taken) | 30 files, 33,200 lines |
| SQLite | 3.50.4 | `sqlite-3.50.4.0.tar.gz` | `fb5ab81f27612b0a7b4861ba655906c76dc85ee969e7a4905d2075aff931e8d0` | public domain | the amalgamation, 262,899 lines, 9.3 MB |

libmpdec (`mpdecimal-4.0.0`, SHA-256 `338fac3fb8cdd60f406b6326431338756f58a8af94229ffd9bf1e7c2b1ad71ca` in the same SPDX file) stays out (PYS22 (a): only if decimal's
speed is measured to matter). The tests do not pin the versions: test_zstd checks `zstd_version_info` against
`zstd_version`, nothing else.

**The layers.** For `_bz2`:

- Python: `Lib/bz2.py` and `_compression.py` (BZ2File, `bz2.open`) are the tarball's.
- `go/arbace/pybz2` (package `arbace/pybz2`): `Modules/_bz2module.c` ported statement for statement over pyi's
  objects. It is a lazy package as pyexpat's (202e): py's `builtins_bz2_api.go` names it in `lazyModules`, so a run that
  never imports bz2 loads none of it.
- `go/arbace/clib` (package `arbace/clib`): the host of a C library inside the Python process. `Open(name, nodes)` makes
  one `ci.Prog` over a crt address space per library, once per process. It loads the node program, runs its static
  initializers, seals its read-only data, and initializes musl as `__libc_start_main` would (`__init_libc` with an
  initial stack laid out as ci's `RunMain` does: the thread pointer, the auxiliary vector). `Call(name, args...)` calls a
  function by name on the program's one C thread. `Malloc`, `Free`, `Realloc`, `Read`, `Write` and the loads and stores
  move bytes between Go and C memory.
- `go/arbace/clib/bzip2` (package `arbace/clib/bzip2`, generated, committed): `Nodes`, the library's node program as
  text. That is ci's builder's output (`arbace.c.build`, CS120's "node program"): bzip2's seven files, the module's C
  helper `tools/pyi/clib/bzip2/pybz2.c`, and the part of musl 1.2.6 they reach, linked by `arbace.c.musl`.
- The back end, one of ci's two, chosen by what the process links:
  - **interpreted**: ci's nodes (the Loader's `code` commands), on goi (`bin/pyi run`, lib-test);
  - **compiled**: pyi's AOT links `arbace/clib/bzip2/aot`, cf2g's Go of the same node program
    (`bin/pyi-clib aot`). Its init calls `clib.RegisterCompiled`, so `Open` sets cf2g's functions as the Funcs'
    natives. The data and the static initializers are the same text, so both lay memory out alike (CS120 (a)).

**How the glue meets C.**

- **Memory.** The `bz_stream` and the decompressor's input buffer live in the library's C heap (musl's malloc).
  The glue reads and writes their fields at their offsets; `pybz2_layout` gives the offsets, and the glue checks
  them once.
- **Bytes.** A Py_buffer's bytes are copied into C memory for the call. CPython hands libbzip2 the caller's buffer
  directly; the copy is the one cost the address space adds.
- **Output.** The output buffer is CPython's `_BlocksOutputBuffer`: blocks of its sizes, the current one in C
  memory, copied out when the next is allocated. So libbzip2 sees CPython's `avail_out`s, and `max_length` is
  consumed as CPython consumes it.
- **Allocator.** libbzip2's default allocator is used (bzalloc NULL, musl's malloc). CPython's is PyMem_RawMalloc;
  the difference is invisible to Python.
- **Locks.** The caller holds pyi's GIL around every call: one C thread, and the calls never overlap. CPython's
  per-object lock and its `Py_BEGIN_ALLOW_THREADS` have no counterpart.
- **Freeing.** A finalizer (`runtime.SetFinalizer`; goi has no `AddCleanup`, GOI-SPEC GC32) queues
  `BZ2_bz*End` and the frees on the library (`Lib.Defer`). They run before its next call, under the GIL.
- **Faults.** A C fault or exit inside the library ends the process as it would end CPython's: 128 + the signal,
  with a "Fatal Python error" line naming the C frames. `bz_internal_error` writes bzip2's first line and `_exit(3)`s,
  as the stdio build's assertion does.

**The build, its cost and its keys** (`bin/pyi-clib`, a script of its own; `tools/pyi/clib/LIB/lib.conf`: the URL,
SHA-256, files and flags):

| step | what | cost on the VM | cached by |
|---|---|---|---|
| musl's forms | `bin/ci musl read` (shared with bin/ci) | 8 min once, 3 threads | `.tmp/ci-musl/forms/key` (repr/c, the libc reader, the host, configure's headers) |
| the node program | `bin/pyi-clib build LIB`: bin/ci nodes, the checkout's builder evaluated (`ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1`: a stage 0 runs the `arbace/c` it embeds, which differs), written as `go/arbace/clib/LIB` | bzip2 33 s (10 s embedded) | `Key` (in the package): lib.conf, the glue, the pin, repr/c, `arbace/c/{build,check,run,musl}.ae`, the host, `bin/pyi-clib`, musl's forms key; `build` keeps a committed package of the same key |
| check | `bin/pyi-clib verify LIB`: built afresh, compared byte for byte | the same | |
| cf2g | `bin/pyi-clib aot LIB OUT`: the committed node program translated, Main made `setup` and registered | bzip2 3 s, 797 KB of Go, 246 functions | the node program's digest (`OUT/nodes.key`) |
| AOT's module | bin/pyi's `aot_module` copies ci, crt, clib, clib/bzip2, pybz2 and runs `bin/pyi-clib aot bzip2` | once per runtime key | the runtime key (now with ci, crt, clib, pybz2, bin/pyi-clib) |

**Why the node program is committed.** goi loads a package's forms from the checkout. So a node program made at the
first import would need musl's forms cache (8 minutes) and the tarball on every machine. Committed, it is 558 KB of
forms for bzip2; `verify` keeps it honest, as `bin/jrt-convert verify` keeps `jdk/`. cf2g's Go is not committed:
it is made at the AOT module's build, from the committed text. For SQLite the committed size does not fit (below,
DECISION 2).

**Speed** (compressing with bzip2 at level 9; gcc 15 -O0 on this aarch64 VM, one run each):

| input | gcc -O0 | cf2g compiled (pyi's AOT) | ci's interpreter compiled (tools/cirun) | ci interpreted by goi (pyi on goi) |
|---|---:|---:|---:|---:|
| 20 KB of text | 0.004 s | 0.028 s | 0.16 s | 20.9 s |
| 200 KB random | 0.030 s | 0.32 s | 2.1 s | (about 200 s) |

On goi, ci itself is interpreted: `arbace/ci` is not in goi's table. That double interpretation is the cost:
about 1 KB/s. With `arbace/ci` and `arbace/crt` listed in the table (a re-seed carries it), the interpreted path would
run at cirun's speed, about 130 times faster (DECISION 1).

### As built

- `bin/pyi-clib` (fetch, build, verify, key, aot), `tools/pyi/clib/bzip2/` (lib.conf; `pybz2.c`: `pybz2_layout`,
  `bz_internal_error`, the `main` that names `__init_libc` so the link keeps it). bzip2 is built with `BZ_NO_STDIO`
  (the module calls only the six stream functions).
- `go/arbace/clib/bzip2` (522,914 bytes of node program, 246 functions with code; 558 KB of forms), `go/arbace/clib`
  (the host, its test: bzip2's stream functions against bzip2(1)'s output), `go/arbace/pybz2` (the glue).
- py: `builtins_bz2_api.go` (`lazyModules["_bz2"]`). `arbace.python.aot`'s main imports `arbace/pybz2` and
  `arbace/clib/bzip2/aot`. bin/pyi's runtime print, read, vet and check and its keys cover clib and pybz2; print also
  prints ci, crt and clib/bzip2 beside them.
- Out of every executable (gate_key, the audit's `:gate`, gate-key-crops-test), as pyexpat. LICENSE.md: bzip2's
  license and musl's COPYRIGHT (musl's code is in the node program).
- Divergences: `:pyio-size-type` (5 BZ2FileTest reads: `_pyio`'s BufferedReader.read takes a float size unchecked,
  which `_io`'s C refuses first; CPython's own `_pyio` does the same), and `:clib-on-goi` (testThreading and
  testDecompressLimited, which compress megabytes, past the driver's time on goi only).
- A REQUEST for 210 (class.ae): "not an acceptable base type" names the type by its tp_name
  (`'_bz2.BZ2Decompressor'`, as CPython).

**Samples (the VM).**

- Probes against CPython 3.14.10 (`python3 -I -B`): `t2.py` exercises compress and decompress, max_length,
  unused_data, needs_input, every error message, pickling, flush twice, `bz2.open` in text mode, and subclassing.
  It is equal on goi and compiled.
- bzip2 under ci (`tools/cirun`) against gcc: 5 inputs (text, random, repetitive, 1 MB of zeros, runs) at levels
  1, 5 and 9, compressed and decompressed. All 20 are equal.
- test_bz2, 102 methods:
  - on goi: 95 equal, 7 classed, 0 failing (32 minutes, 3 driver runs);
  - compiled (`bin/pyi aot test/python/libtest_run.py test_bz2 SKIP`, 30 s): 97 equal, the 5 `:pyio-size-type`
    errors. testThreading's 50 MB and testDecompressLimited pass.

### `_lzma` and `_zstd` (probed), `_sqlite3` (designed)

- **liblzma 5.2.5 builds and runs under ci.** The tarball's `configure` was run once
  (`--disable-shared --disable-threads --disable-nls`, the tools off) for `config.h`. The 69 files of
  `src/liblzma`'s `liblzma.la` build into a 1.93 MB node program (3 min 45 s, the checkout's builder). A driver
  (`lzma_easy_buffer_encode` with CRC64, decoded back) gives the same bytes as the gcc build of the same files at
  presets 0, 6 and 9 on 4 inputs: 12 of 12. Left for `_lzma`:
  - the glue, `_lzmamodule.c`'s 1,664 lines: the filter specs to `lzma_filter` chains of option structs in C memory,
    `_encode_filter_properties` and `_decode_filter_properties`, the formats XZ, ALONE, RAW and AUTO, memlimit;
  - lib.conf with the configure step (or its `config.h` committed as the glue's).
  On goi, preset 6's encoder initializes about 94 MB of match finder, which is hopeless interpreted twice: so
  `_lzma` waits on DECISION 1 for goi, and runs compiled as is.
- **zstd 1.5.7.**
  - Its `ZSTD_cpuid` is inline assembly under `__x86_64__` (ci's host is x86_64), and ci refuses a program holding
    an asm function even when it is never called: `-U__x86_64__ -U__amd64__` takes cpu.h's portable branch, with
    `ZSTD_NO_INTRINSICS`, `ZSTD_DISABLE_ASM` and `ZSTD_LEGACY_SUPPORT=0`.
  - The seed-1fe1063f stage 0's embedded builder then refuses `inBuffer_forEndFlush`'s structure-valued conditional
    ("a conversion from struct ZSTD_inBuffer_s to struct ZSTD_inBuffer_s"). The checkout's builder takes it
    (queue 178's fix): the reason `bin/pyi-clib` runs the checkout's builder.
  - The glue is `Modules/_zstd/`'s 2,567 lines: ZstdCompressor, ZstdDecompressor, ZstdDict, the parameters'
    bounds, `get_frame_info`.
  - Multithreaded compression (`nbWorkers`) needs pthreads in ci: built without `ZSTD_MULTITHREAD`, as test_zstd
    expects of such a build ("zstd lib doesn't support MT compression" skips).
- **`_sqlite3`, designed only.**
  - Size: the amalgamation is 50 times bzip2. Its node program would be 25-30 MB, cf2g's Go about 40 MB, and one
    package's `go build` several GB (C6.3: a quarter of musl, 4.3 MB of Go, took 0.8 GB). That is the heavy tier:
    the main host's, cf2g's Go split into packages (CS122 (a)'s split by directory, here by function groups). It is
    not committed as forms (DECISION 2) and not interpreted on goi.
  - The glue: `Modules/_sqlite/`'s 6,563 lines (connection, cursor, statement, row, blob, the adapters' module
    state). Its callbacks (create_function, aggregates, window functions, collations, the authorizer, the progress
    and trace handlers) are C calling Go. ci's natives are Go functions the node program calls by name: the C glue
    declares them extern and the Go glue sets them on the Prog before the first call. They run Python on the same
    goroutine, under the GIL already held.
  - The thread model is the open point: SQLite's busy handler sleeps (unixSleep: `usleep`, crt's nanosleep) inside
    `sqlite3_step`. With one C thread and the GIL held, a second Python thread waiting on a lock the first holds
    deadlocks. The design: one `ci.Thread` per Python thread, each with its own C stack in the shared address space
    (ci's Prog has one stack today: a change for the main host), the GIL released only around the library's sleeps
    (a native for nanosleep), so C never runs in two goroutines at once. SQLite is built `SQLITE_THREADSAFE=1` with
    its mutexes uncontended, so `sqlite3.threadsafety` is CPython's 3.

## Decided (2026-11-05, the user): 208's DECISIONs

- **DECISION 1 (a), decided:** arbace/ci and arbace/crt are listed in goi's table at the re-seed after Python's tag.
  This is the main host's REQUEST, given in 208's report. Until then the tests of megabytes stay `:clib-on-goi`.
- **DECISION 2 (a), decided:** the node programs of bzip2, liblzma and zstd are committed as forms and checked by
  `bin/pyi-clib verify`. SQLite is built on the main host into a keyed cache, and runs compiled only.

## After the decisions: `_lzma`, and zstd's library (2026-11-05, the Mac VM, queue 208)

- **liblzma committed** (`go/arbace/clib/xz`):
  - The build: `tools/pyi/clib/xz/` holds lib.conf (the 69 files of `src/liblzma`'s `liblzma.la`), `config.h` (the
    tarball's configure output, run once: no threads, every filter, the three checks) and `pylzma.c`.
  - `pylzma.c` holds the layouts, the filter chains, and the option structures of the LZMA, delta and BCJ filters,
    made and read field by field, so their layout stays liblzma's.
  - The result: 1.93 MB of node program, 2.05 MB of forms, 3 min 30 s with the checkout's builder.
  - `bin/pyi-clib` takes lib.conf's `includes`, with the glue's directory first on the include path (its config.h).
- **`_lzma`** (`go/arbace/pylzma`, lazy, as `_bz2`): `Modules/_lzmamodule.c` ported statement for statement:
  - `LZMACompressor`: the formats XZ, ALONE and RAW; a preset or a filter chain.
  - `LZMADecompressor`: the formats AUTO, XZ, ALONE and RAW; memlimit; `check`, `eof`, `needs_input`, `unused_data`.
  - `is_check_supported`, `_encode_filter_properties`, `_decode_filter_properties`, `LZMAError` and the constants.
  - The unsigned converters' messages ("Cannot convert negative int", "Python int too large for C uint32_t" and
    the like).
  - Filter specifiers: a key outside the options, or a refused value, raises the "Invalid filter specifier"
    ValueError.
  - A dict given as the filter chain is "dict is not a sequence".

  pyi's AOT links it with liblzma compiled by cf2g (`arbace/clib/xz/aot`).
- **Samples (the VM).**
  - `l1.py` against `python3 -I -B`: every format and check, a delta+LZMA2 chain, max_length, the properties
    codecs, 21 error cases, pickling, the constants. It is equal on goi (22 s, the start included) and compiled.
  - test_lzma, 121 methods:
    - compiled: 121 equal (120 ok, plus the refleak skip CPython's reference also skips);
    - on goi: 120 equal, 1 `:clib-on-goi` (test_decompress_limited's 2 MB of zeros), 0 failing; 13 minutes, 150 s
      cap per test.
- **zstd's library committed** (`go/arbace/clib/zstd`, decision 2 (a)):
  - The build: the four library directories; `-U__x86_64__ -U__amd64__` (cpu.h's portable branch, its
    `ZSTD_cpuid` asm avoided), with `ZSTD_NO_INTRINSICS`, `ZSTD_DISABLE_ASM` and `ZSTD_LEGACY_SUPPORT=0`, and no
    threads.
  - `pyzstd.c` holds the buffers' layouts and wrappers for the calls that pass a structure by value (`ZSTD_bounds`,
    `ZDICT_params_t`).
  - The result: 4.12 MB of node program, 4.36 MB of forms, 3 min 54 s.
  - Checked: `ZSTD_compress` at levels 1, 3 and 19 on 4 inputs, through arbace/clib with ci's interpreter
    compiled, gives the same bytes as gcc's build of the same files and decompresses back (12 of 12).
  - `_zstd`'s glue (`Modules/_zstd/`, 2,567 lines plus the clinic's 1,176) is not written yet: the next step.
## As built: 209, PYS22 (a)'s `_ssl` over Go's crypto/tls (2026-11-05, the Mac VM, queue 209)

The user's decision PYS22 (a) (2026-11-04) names `_ssl` over Go's TLS. Built: `go/arbace/py/builtins_ssl.ae` (the
module, the context), `builtins_ssl_cert.ae` (certificates, OIDs) and `builtins_ssl_conn.ae` (SSL objects, MemoryBIO,
SSLSession), about 3,000 lines of Go in pyi's runtime, written after CPython 3.14.10's `Modules/_ssl.c`, its clinic
file and `Modules/_ssl/cert.c` (the survey's pinned tarball; PSF License, LICENSE.md's entry for the Python runtime's
ports). Go's `crypto/tls`, `crypto/x509` and their dependencies are compiled packages of goi's table, so the TLS itself
runs at native speed under goi; pyi's own Go around it is interpreted. `Lib/ssl.py` runs unchanged.

### The design

- **The context** (`_SSLContext`) keeps OpenSSL's settings as `_ssl.c` reads and writes them, with CPython's defaults
  and errors. These are: the options; `verify_mode` and `check_hostname`, with their mutual rules; `verify_flags`;
  `_host_flags`; the minimum and maximum versions (PY_SSL_MIN_PROTOCOL TLS 1.2 for the TLS protocols); the cipher
  list; ALPN; one certificate chain a key type; the trust store; CRLs; the curves; `num_tickets`; the session
  statistics; `keylog_filename`; `sni_callback`. Each connection gets a fresh `tls.Config` made from these settings:
  - **Versions:** the protocol, the bounds and `OP_NO_TLSv1*` give `MinVersion`/`MaxVersion`. An empty range is
    OpenSSL's NO_PROTOCOLS_AVAILABLE.
  - **Ciphers:** `set_ciphers` evaluates OpenSSL's cipher-string language (`:`, `+`, `!`, `-`, `+X`, `@STRENGTH`,
    `@SECLEVEL`) over the TLS 1.0-1.2 suites Go implements, under OpenSSL's names and attributes (`get_ciphers`'
    dicts as `cipher_to_dict`'s). The result goes to `CipherSuites`. TLS 1.3's three suites are always on, as with
    OpenSSL's separate list.
  - **Tickets and resumption:** `OP_NO_TICKET` (and `num_tickets` 0 on a server) disables tickets. A server context's
    ticket key is shared by its connections, so a session resumes against the same context.
  - **ALPN, SNI, curves, key log:** ALPN goes to `NextProtos`. On a server without a protocol in common, the
    connection gets none (CPython's callback answers NOACK; Go's server would refuse). `server_hostname` gives the SNI
    `ServerName`. `set_ecdh_curve` sets `CurvePreferences`. `keylog_filename` is Go's `KeyLogWriter` on the file,
    with OpenSSL's header line.
- **Verification** is `_ssl`'s own: `InsecureSkipVerify` is set and `VerifyConnection` does the work, so CERT_OPTIONAL
  and the codes keep OpenSSL's meaning:
  - Go's `x509.Verify` runs with the context's store, plus the capath directories read at verification (OpenSSL's
    lookup by hash; the anchor found there then joins the store, as `get_ca_certs` shows). The peer's other
    certificates are the intermediates. The purpose is serverAuth or clientAuth.
  - Then the distrusted SPKIs (`Modules/_ssl/distrusted_spki.h`, code 28), `VERIFY_X509_STRICT`'s missing AKI (85),
    `VERIFY_CRL_CHECK_LEAF` (3, 8, 23) and the host check run. The host check is X509_check_host's rules:
    NO_PARTIAL_WILDCARDS, the CN only without DNS names and without NEVER_CHECK_SUBJECT, IP addresses (62, 64).
  - Go's errors map to OpenSSL's codes and texts: 18, 19, 20, 9, 10, 24, 25, 26, 47.
  - A server's CERT_REQUIRED is `RequireAnyClientCert`, and the certificate is verified the same way.
- **Certificates** are decoded from their DER by a walker of `_ssl`'s own, not by crypto/x509, which refuses some
  certificates OpenSSL reads (test_ssl's NUL-byte and CVE-2019-5010 certificates):
  - `_decode_certificate`'s dict: names with OpenSSL's long names, ASN1_STRING_to_UTF8's decodings,
    i2a_ASN1_INTEGER's serial, ASN1_TIME_print's dates;
  - `subjectAltName`'s GeneralNames as `_get_peer_alt_names` prints them;
  - OCSP, caIssuers and crlDistributionPoints.
  The OIDs are the part of OpenSSL 3.5's objects table that `_ssl` names, with its NIDs (`txt2obj`, `nid2obj`).
  Keys are PKCS #1, SEC 1 or PKCS #8. Encrypted PEM is read both ways: OpenSSL's legacy encryption by Go's
  `DecryptPEMBlock`, and PKCS #8's PBES2 with PBKDF2 and AES/3DES-CBC by the port (RFC 8018; PBKDF2 by hand, because
  `crypto/pbkdf2.Key` is generic and goi's table calls no generic function). The password follows CPython's rules: a
  str, bytes or a callable called only when needed, at most 1,024 bytes.
- **The pipe adapter (SSL objects over sockets and MemoryBIOs).** Go's `tls.Conn` wants a `net.Conn` it may block on.
  OpenSSL's callers want SSL_ERROR_WANT_READ and an operation that the next call resumes (ssl.py over a non-blocking
  socket, asyncio's SSLProtocol over MemoryBIOs). The adapter works as follows:
  - Each SSL object runs its `tls.Conn` in a goroutine of its own (the engine), over an in-memory transport. The
    engine's Read takes the bytes the Python side fed it. When there are none, Read signals that it wants input and
    waits. The engine's Write appends to the bytes to send.
  - Control alternates strictly. The Python thread hands the engine an operation (handshake or read), or resumes the
    one in flight, and waits for its event with the GIL released; the engine waits for the Python side. So the
    engine never touches a Python object, and the buffers have one owner at a time.
  - When the engine wants input, the Python side first sends the pending bytes (into the outgoing BIO, or to the
    socket under its timeout). Then it feeds the next bytes of the current TLS record (its header, then its length,
    never more), read from the incoming BIO or the socket:
    - a blocking socket blocks with the GIL released;
    - a socket with a timeout waits on select(2) to the operation's deadline, then raises CPython's TimeoutError
      text;
    - a non-blocking socket or an empty BIO raises SSLWantReadError, and the operation stays in flight for the
      next call.
    A byte past the record is never read, so after `unwrap()` the clear bytes behind close_notify stay in the
    socket, as with OpenSSL (STARTTLS).
  - Writes and close_notify never wait for input. After the handshake they are made on the Python thread directly
    (tls.Conn's write side has its own lock); a write before the handshake drives it first. A non-blocking socket
    that cannot take the records raises SSLWantWriteError. It keeps them, and the retried write returns the accepted
    count (SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER's contract).
  - A callback the engine needs (the SNI callback inside `GetConfigForClient`) is a third event. It runs on the
    Python thread with the GIL; the SNI callback may switch `sslsocket.context`, and the configuration is made from
    the context then in force.
  - So the runtime needs no decision on blocking or the GIL. Python code runs only on Python threads, holding the GIL,
    and every wait releases it.
- **Errors** are `fill_and_set_sslerror`'s:
  - The message is `[LIB: REASON] text (_ssl.c:LINE)`, with CPython 3.14.10's line numbers. The exception carries
    `reason` and `library`, and `verify_code` and `verify_message` on SSLCertVerificationError, whose bases are SSLError
    and ValueError.
  - A peer's alert gives OpenSSL's reason by its number (`SSLV3_ALERT_*`, `TLSV1_ALERT_*`). Go's own handshake errors
    give theirs by text (WRONG_VERSION_NUMBER, NO_SHARED_CIPHER, UNSUPPORTED_PROTOCOL,
    PEER_DID_NOT_RETURN_A_CERTIFICATE, ...).
  - The transport's EOF inside a record or a handshake is SSLEOFError UNEXPECTED_EOF_WHILE_READING
    (OP_IGNORE_UNEXPECTED_EOF: a clean EOF). close_notify is the zero return: `read()` gives `b''`.
- **What the module reports:** `OPENSSL_VERSION` is "OpenSSL 3.5.0 (compatible; Go crypto/tls)" with
  `OPENSSL_VERSION_INFO` (3, 5, 0, 0, 0): the API level the port follows. ssl.py and the tests branch on it, and 3.5 is
  the level CPython's references ran with. HAS_SNI, HAS_ECDH, HAS_ALPN, HAS_TLS_UNIQUE (Go's `TLSUnique` for TLS 1.2)
  and HAS_TLSv1 through 1_3 are True; HAS_PHA, HAS_PSK, HAS_NPN and HAS_SSLv2/3 are False. `get_default_verify_paths`
  is OpenSSL's Linux defaults (`/etc/ssl/cert.pem`, `/etc/ssl/certs`, `$SSL_CERT_FILE`, `$SSL_CERT_DIR`).
  `set_default_verify_paths` loads the file now and looks in the directory at verification, as OpenSSL does.
- **By design** (class `:ssl-go-tls`): what Go's TLS does not have:
  - TLS 1.3 post-handshake authentication (`verify_client_post_handshake` raises NotImplementedError, as CPython
    without PHA);
  - TLS-PSK (`set_psk_*_callback` raise NotImplementedError);
  - `_msg_callback` (kept, never called);
  - the TLS 1.3 exporter secret in the key log (Go logs four secrets, OpenSSL five);
  - the alert of a refusing SNI callback (Go sends internal_error);
  - the alert of a failed verification (Go sends bad_certificate where OpenSSL sends unknown_ca: the peer's reason
    differs);
  - finite-field DHE (`load_dh_params` reads the file and keeps nothing);
  - SSLv3 and compression;
  - the cipher each side picks (Go's preference order, not OpenSSL's).
- **Deferred** (class `:ssl-finalization`, PYS9's finalizer queue): an SSL object freed by `gc.collect()`.
- **Not in this slice:** an engine goroutine
  is not ended when its SSL object dies (it waits on its channels for ever: one goroutine an abandoned connection);
  tamago (pyi's runtime is read for linux; Go's TLS would run there, the sockets would not).

### The first slice and its samples (run on the VM)

- `test/python/pyi/d-ssl.py` with its own test CA and leaf (`test/python/pyi/ssl/`: EC P-256, localhost and
  127.0.0.1, valid until 2525). It runs a client and a server in one process over MemoryBIOs and over a socket pair
  with a thread, and covers:
  - the certificate's decoding, the handshake, `version`, `cipher`'s protocol, `getpeercert` (dict and DER);
  - the verified and unverified chains, reads and writes (20,000 bytes in pieces), `pending`, `unwrap` on both sides;
  - the verification failures (20, 62, 64) with their texts;
  - a handshake timeout, a non-blocking WANT_READ, then SSLEOFError, and ALPN with and without a protocol in common.
  Its output is equal to `python3 -I -B`'s under `bin/pyi run` (goi) and `bin/pyi aot` (the runtime compiled by Go).
- `bin/pyi lib-test-all --fresh test_ssl` (2 jobs, 49 s): 206 methods, from 0 equal (all in `:ssl`, "No ssl module")
  to 176 equal, 30 classed (20 `:ssl-go-tls`, 4 `:ssl-finalization`, 5 cpython-only, 1 buffer-exports), 0 failing.
- `bin/pyi lib-test-all --fresh test_poplib test_ftplib test_httplib test_imaplib test_urllib2_localnet` (their TLS
  tests ran for the first time): 570 methods, 564 equal, 3 classed, 3 failing. The failures are imaplib's
  `Time2Internaldate`, "date_time not of a known type": not `_ssl`'s, and in no class.
- `bin/pyi lib-test --corpus lib-test-asyncio test_asyncio.test_ssl test_asyncio.test_sslproto` (asyncio's
  SSLProtocol over MemoryBIOs, its starttls and shutdown paths): 64 methods, 56 equal, 8 classed (`:ssl-finalization`),
  0 failing. On the first run, `test_remote_shutdown_receives_trailing_data` hung. The cause: a read after this side's
  `unwrap()` had sent close_notify returned `b''` on the peer's close_notify, where OpenSSL raises
  SSLZeroReturnError once both shutdowns are seen. Fixed.
- `test_ssl`'s `test_session` failed once in three runs: a resumed session's id changed when the server renewed the
  ticket. Fixed: a resumed session keeps its id and time, and the renewed ticket only replaces its state.

### After the decisions (the user, 2026-11-05: (a) for both of 209's DECISIONs)

- **DECISION 2, (a):** `OPENSSL_VERSION` stays "OpenSSL 3.5.0 (compatible; Go crypto/tls)", as built.
- **DECISION 1, (a): the fallback store.** `set_default_verify_paths` (so `load_default_certs` and
  `create_default_context`) keeps CPython's system store. When that store holds nothing (no certificate in the default
  file, no certificate file in the default directories, after `$SSL_CERT_FILE` and `$SSL_CERT_DIR`), it takes the
  checkout's x509roots/ bundle: Mozilla's 120 roots, each with its distrust-after date as a constraint on the chain
  (Go's `CertPool.AddCertWithConstraint`; the bundle's rule: no certificate of the chain issued after the date). A chain
  the rule refuses fails with code 28, "certificate rejected", as `_ssl`'s distrusted SPKIs do.
  - The bundle reaches pyi's runtime through `go/arbace/pyssl` (package `arbace/pyssl` over `arbace/py` and
    `arbace/x509roots/fallback/bundle`), a package apart as `arbace/pyexpat` is. Its `init` registers the roots
    (`py.SetSSLFallbackRoots`). The interpreted runtime loads it on first need through the host's package loader, so a
    run with a system store never loads the bundle's forms. goi interprets the bundle's forms, its `go:embed` of
    `bundle.der` and its range-over-func iterator included. A compiled program links it (arbace.python.aot's main
    imports it blank).
  - bin/pyi: `x509roots/` is a source root of the interpreted runs (`ARBACE_PATH`), and the runtime's print, the AOT
    module and its key carry `pyssl` and the bundle with `bundle.der`. `runtime check` reads `pyssl` back. This was
    the REQUEST commit for the integration.
  - Tests: `PYI_SSL_FALLBACK_ROOTS` names a PEM file that stands in for the bundle (no bundle root signs a test leaf).
    A block's `Distrust-After: RFC 3339` header is its constraint. `_ssl._x509roots()` lists the fallback's roots as
    (subject, distrust-after).
  - Proof, `test/python/pyi/ssl/fallback.py` (its output in `fallback.out`, the same under `bin/pyi run` and
    `bin/pyi aot`). It points the store at an empty directory and shows:
    - the bundle's 120 roots, one with a date (Izenpe.com, 2026-04-15);
    - the test leaf through the fallback, accepted unconstrained and with a later date, then refused with a date
      before its issue (28);
    - with a store again, the fallback not consulted.
## As built: 210, pyi's speed, round two (2026-11-05, the Mac VM, queue 210)

Profile first: `PYI_CPUPROFILE` on the compiled benchmarks of `bin/pyi bench` (20 of the 71, the slowest against
CPython and the call-, attribute- and generator-heavy ones), read with TamaGo's `go tool pprof`; each change measured
on the benchmarks it was for, back to back with the build before it (the least CPU seconds of 3 alternated runs), and
kept only when it paid. What the profiles showed, and what was done:

| profile's finding | change | where | measured (compiled, VM) |
|---|---|---|---|
| bm_pprint: 46% in memmove, a bytearray's slice assignment copying the whole buffer (`_pyio`'s BytesIO writes `b[pos:pos+n]` at its end: io.StringIO quadratic) | in place when the length is kept or the tail replaced | bytearray.go | pprint 34 s -> 7.5 s |
| bm_mdp: 47% in hashTuple (nested namedtuple states as dict keys, rehashed at every lookup) | a tuple's hash kept once computed, as CPython 3.14's `ob_hash` | containers.go | mdp 6.9 -> 3.5 s |
| richards, deltablue, go, raytrace: attribute loads 20-38% (the instance dict's Go map; a monomorphic cache missing on richards' four task classes, lookupAttr's string map behind it), globals 5-10% | load-attr/store-attr caches of four types, the instance dict's entry found by identity (interned names) before the index; load-global's per-instruction cache (the globals' entry, or the builtins' while the globals' version holds); a dict's 64-bit mask of its keys' hash bits (a method's name absent from an instance dict without the index); object's method descriptors marked on the Builtin | attr.go, eval.go, aot.go, code.go, dict.go, func.go | 10-25% |
| calls: bound built-in methods copied their arguments twice; bindArgs' general path for every call; a class's `__init__` called through a nested loop | the arguments made once with self first; the all-positional call first; `__init__` pushed in the caller's loop (CPython's CALL_ALLOC_AND_ENTER_INIT) | eval.go, func.go, frame.go, type.go | 5-10% on calls, 10% on a class's call |
| every method call made a bound method for the call to unwrap | the compiled code's load-attr of a call with simple arguments as `AotLoadMethod`/`AotCallMethod` (CPython's LOAD_ATTR method variant): a Python method of an instance, or a built-in container's method descriptor, pushed with self, no bound object | aot.ae, aot.go | richards 16%, deltablue 14%, bpe_tokeniser 8% |
| to-bool pushed a bool the conditional jump popped | fused in the compiled code | aot.ae | richards 5% |
| bm_coroutines: newFrame 25% (a frame per coroutine, never reused) | a generator's or coroutine's frame reused once it returned (nothing escaped it) | frame.go, gen.go | coroutines 36% |
| bm_fannkuch: a Slice made at every slice assignment; a list's slice copied out and back | the slice used as it is when its bounds are ints; same-length assignment in place | slice.go, containers.go | fannkuch 28% |
| spectral_norm, nbody: mixed float and int operands through floatBinary's conversions | float +, -, *, / of a float and a float or an int64 directly | abstract.go | 4-6% |
| a short program's start: the IR of each imported module read, a fifth of a short benchmark | strings without escapes sliced, an instruction's pair in one allocation | load.go | start 8% CPU |

**The table** (the process's wall seconds, the least of 3 rounds; each round runs CPython, the base and this branch
back to back on the same benchmark; the VM's load 6-13, four other agents running: differences under 10% are noise;
CPython 3.14.10 `python3 -s -B`; base c3488342, after this branch's head; loops as test/python/bench/benchmarks.tsv):

| benchmark | loops | CPython | base | after | base / CPython | after / CPython |
|---|---:|---:|---:|---:|---:|---:|
| raytrace | 1 | 0.21 | 0.96 | 0.73 | 4.6x | 3.5x |
| deltablue | 50 | 0.13 | 0.74 | 0.49 | 5.7x | 3.8x |
| fannkuch | 1 | 0.29 | 1.21 | 0.82 | 4.2x | 2.8x |
| generators | 4 | 0.16 | 0.90 | 0.80 | 5.6x | 5.0x |
| nqueens | 4 | 0.27 | 0.86 | 0.76 | 3.2x | 2.8x |
| go | 1 | 0.10 | 0.45 | 0.33 | 4.5x | 3.3x |
| richards | 10 | 0.33 | 1.30 | 0.83 | 3.9x | 2.5x |
| chaos | 1 | 0.08 | 0.33 | 0.28 | 4.1x | 3.5x |
| json_dumps | 1 | 0.03 | 0.29 | 0.25 | 9.7x | 8.3x |
| spectral_norm | 4 | 0.36 | 1.10 | 1.02 | 3.1x | 2.8x |
| unpack_sequence | 20000 | 0.24 | 0.90 | 0.91 | 3.8x | 3.8x |
| coroutines | 20 | 0.38 | 1.40 | 0.84 | 3.7x | 2.2x |
| float | 4 | 0.23 | 0.68 | 0.62 | 3.0x | 2.7x |
| nbody | 3 | 0.32 | 0.60 | 0.62 | 1.9x | 1.9x |
| hexiom | 1 | 0.03 | 0.12 | 0.11 | 4.0x | 3.7x |
| base64 | 1 | 1.67 | 4.57 | 3.83 | 2.7x | 2.3x |
| bpe_tokeniser | 1 | 3.51 | 10.89 | 8.35 | 3.1x | 2.4x |
| async_tree | 1 | 0.24 | 1.61 | 1.26 | 6.7x | 5.2x |
| mdp | 1 | 0.85 | 7.63 | 3.63 | 9.0x | 4.3x |
| pprint | 1 | 1.40 | 34.06 | 5.24 | 24.3x | 3.7x |

Over the 20, CPython 10.8 s, the base 70.6 s (6.5x), this branch 31.7 s (2.9x); without pprint 3.9x -> 2.8x; the
median benchmark 4.0x -> 3.3x. The brief's aim, about 2x CPython or better, is met by nbody (1.9x) and nearly by
coroutines, base64, bpe_tokeniser and richards (2.2-2.5x), not by the rest: what is left is the compiled loop's design,
not one hot spot. Every instruction is a helper call over the frame's heap stack (a push and a pop through memory,
`f.Lasti` stored each time); a call of a Python function returns to runFrames and enters the callee through
guardedStep's deferred recover; a generator's or coroutine's resumption from Go (for-iter over a generator, await, a
genexp's consumer) is a nested run; every int and float is a heap object. The profiles are flat now (no function above
10% flat on the call-heavy benchmarks). The next steps, by their weight in the profiles:
- values kept in Go locals across a basic block (a register allocation of the stack in arbace.python.aot), with
  f.Lasti stored only at the instructions that may raise;
- a call of a compiled function from compiled code without the return to runFrames (the callee's native function
  entered directly; the C-level depth and the tracebacks as runFrames keeps them);
- for-iter over a generator and yield-from resuming the generator's frame in the caller's loop (CPython's
  FOR_ITER_GEN and SEND_GEN);
- small ints and floats unboxed or cached (an allocation per loop step today);
- dicts without the Go map index up to 8 entries (202b's open addressing: 3-11% compiled, but goi 15% slower, so
  decided for after the re-seed that compiles pyi into stage 0).

Not changed: Go's collector stays at GOGC 400 (GOGC 800 measured 3-7% faster for 1.6-1.9x the peak RSS).
## As built: 211, test_ast's last methods and test_dict's leftovers (2026-11-05, the Mac VM, queue 211)

- **Interned constants.** A str constant of `[a-zA-Z0-9_]*` is interned when its code is loaded (`load.go`'s
  `loadConst`, codeobject.c's `intern_constants` with `should_intern_string` of the default build; in tuples too, as
  `loadConst` recurses). `nameStr`'s own table (the names GetStr, SetStr and `co_names` use, 202b) is merged into
  `sys.intern`'s `internTable`, so a name, a constant, a parser's identifier (202c) and `sys.intern(s)` are one object, as
  in CPython's one table. `node.id is 'x'` inside a method holds: test_ast's three CopyTests replace tests.
- **`b''` is one object** (`emptyBytes`, the constant's load): test_singletons. Only the constant: `bytes()` and the
  empty results of operations stay new objects (no test needs them).
- **ast.parse's int digits limit**: `_PyPegen_number_token`'s SyntaxError ("Exceeds the limit (N digits) for integer
  string conversion: value has M digits; use sys.set_int_max_str_digits() to increase the limit - Consider hexadecimal
  for huge integer literals to avoid decimal conversion limits.", the literal's line, offsets 0) for a decimal literal
  past `sys.get_int_max_str_digits()`. The host's parser does not know the runtime's limit and answers the value in
  decimal, so `builtins_ast_parse.go` reads the literal's radix from the source at the Constant's position:
  test_literal_eval_str_int_limit. A huge literal in exec'd or imported source still compiles (a low of 211's report).
- **Pickling the dict iterators**: their `__reduce__` is `dictiter_reduce`'s `(iter, (list,))` of a copy, the
  iterator not advanced; list_iterator's and tuple_iterator's `__reduce__` and `__setstate__` are
  `listiter_reduce_general`'s and `tupleiter_reduce`'s (an unpickled dict iterator is a list_iterator, which the tests
  pickle again). test_dict's six `*iterator_pickling` tests. `:pickle-iterators` keeps the other built-in iterators.
- **Classes.** `:ast-objects` is narrowed to two causes: a `**{object(): v}` or `**{FakeStr('id'): v}` keyword refused
  by `CALL_FUNCTION_EX` ("keywords must be strings") before a node's `__init__` or `__replace__` sees it, because pyi's
  calls carry keyword names as strs (`kwnames []*Str`) where CPython hands a builtin the dict (test_non_str_kwarg,
  test_replace_non_str_kwarg; the call protocol's owner, PY-core); and test_pickling's 300 s limit (it passed on the VM).
  test_AST_garbage_collection moves to `:finalization-collect` (a cycle through a weak reference cleared at
  gc.collect(), PYS9 (a)). test_dict's three finalization methods stay classed: test_container_iterator and
  test_free_after_iterating call gc.collect() and need the finalizer queue (`:finalization-collect`), and
  test_oob_indexing_dictiter_iternextitem needs `X.__del__` run when `d[2] = None` drops the last reference
  (`:finalization`, by design).
- **The sample**: `bin/pyi lib-test-all --fresh test_ast test_dict`, 339 methods: 316 equal, 23 classed, 0 failing
  (the base: 305, 34, 0). test_ast 207 of 217 (the 10: 7 `:cpython-only`, 2 `:ast-objects`, 1
  `:finalization-collect`); test_dict 109 of 122 (10 `:cpython-only`, 3 finalization).

## As built: 212, Python's GT9 fragment checked and R1's controls (2026-11-05, the Mac VM, queue 212)

- **R1's negative controls** (GTS81 (a), PY11-PREP's R1). `test/python/controls-read.edn` declares a crossed pair per
  corpus (samples, stdlib, lib-test, site-packages, lex-errors, syntax-errors), two files that pass every judge. After its
  run each check judges the subject's output against the reference's expected result by its own judge, which must fail:
  the round trip by `judge-file` (the subject's printed dump against the reference's original; `--text`: `judge-text`,
  the bytes and marshal), lex-check and parse-check (and `--full`) by `judge-dump` (the subject's native dump, named as the
  reference, against the reference's helper dump). `arbace.repr.python.results/judge-controls`: "control passed:" or
  "control not judged:" fails the check (also with `--record`, before anything is written); a pair a selection did not run
  is left. They replace the inline controls, which compared on their own (a judge made to pass everything still passed
  them), ran on the last corpus only, compared a non-`--full` dump against `--full` references, and printed success with
  nothing judged. `REPR_PYTHON_CONTROLS` names another file.
- **The rotations' subsets**: `test/python/corpora.edn`'s `:subset-roundtrip` and `:subset-native` for stdlib, lib-test and
  site-packages, by hand (every known file, the controls' pair, every 24th file), for the gate's `python-roundtrip` and
  `python-native` (`test/gate/crops/python-records.edn`, the main host's, spliced after batch P's night of records).
- **The pinned files from the main checkout** (GTS212 (a)): `bin/gate` drops `REPR_PYTHON_SRC`, so `bin/repr-python`'s
  fetch and `bin/pyi wheels fetch` take, when it is unset, the main checkout's copy at the same path before the network
  (a linked worktree's and the clean clone's).
- **The fragment** (`test/gate/crops/python.edn`, the main host's): checked in a scratch merge of `gt9-verbs` with this
  branch; its corrections and the second fragment are queue 212's patch for batch P (report 212).

## As built: 213, Python's oracle records (2026-11-05, the Mac VM, queue 213; ORS12)

The sixth crop of doc/ORACLE-RECORDS-SURVEY.md (ORS1-ORS19 decided; the shared tooling on the main host's `oracle-records-c`,
the model `oracle-records-scheme`): no Python gate step outside the oracle tier needs python3. Light: recorded and verified on
samples on the VM; nothing recorded is committed (the main host's night records them).

- **What the steps asked of CPython, and what each became.**

  | Step | Live CPython | Class | Now |
  |---|---|---|---|
  | python-read-essential | spec_check.py; corpus.py samples and list; pydump.py tree (originals and prints), tree --full; tokdump.py | A, B, D (the prints) | outcomes; the essential corpora's dumps committed; the print read natively, levels 2, 3, 5 the oracle tier's |
  | python-front-essential, python-front | corpus.py samples, methods (gen --check), list; pydump.py tree, scopes, tree --full | A, B | outcomes; packs (essential committed) |
  | pyi-aot-essential | none, but bin/pyi aborted without python3, and bin/pyi-clib aot ran two python3 one-offs | | no python3: need_python per verb; aot_text.ae on stage 0 |
  | pyi-check-aot | runtime check's smoke runs and REPL session, the Unicode and CJK generators; record-ref --check | A, D | smoke and REPL outcomes; the generators and record-ref --check the oracle tier's |
  | pyi-wheels | CPython's runs of the samples and the console script, pip --target's layout, a python3 zip one-off; pip-probe | A | outcomes (the layout as lines); zip; pip-probe the oracle tier's |
  | pyi-bench | CPython per benchmark | A (speed) | the oracle tier's |
  | python-roundtrip, python-native (python-records.edn) | as python-read-essential over the corpora | B, D | packs; the print native |
  | pyi-c2j | none | | unchanged |

- **`bin/pyi run` and `bin/pyi python` read the program natively** (arbace.python's `run` without `--dump`: the native reader's
  dump in mode :full, as `arbace.python.run` reads what a program imports; `PYI_READER=helper` the helper's): checked equal on
  test/python/pyi samples and a SyntaxError file.
- **The records** (`tools/pydump/records.bash`, sourced by both scripts): `REPR_PYTHON_ORACLE` records (the default where
  `test/oracle-records/oracle.bash` is; live without it, so this branch alone behaves as before), live, record, verify.
  - Class B: one pack a corpus, `python-pydump-CORPUS` (`test/python/records/`), members by kind (`tree/`, `full/`, `scopes/`,
    `tok/`; each tree, full and tok dump with its TSV row, its run's seconds and RSS 0), keyed `INPUT16:HELPER16`;
    syntax-errors' generated inputs in its pack; the mutants of lex-check and parse-check their own corpora (`mutants-lex`,
    `mutants-parse`); the essential corpora (samples, synthetic, errors, lex-errors) committed raw under `essential/`
    (1,514 files, 1.87 MB; the gate's essential chain needs no store). A missing entry is UNRECORDED, a changed input or
    helper STALE, a committed file off its manifest REFUSED: each fails the check.
  - Class A, outcomes: `test/python/records/outcomes/NAME.{out,meta}` (stdout with `$root` as `@ROOT`, the status, the key,
    `host: :python ID`): spec-check, samples, gen --check's eighteen, `smoke/*.py`, `repl/*.in`, `wheels/*` (the samples'
    and the console script's runs, the site's directory as `@WHEELS`, and pip's layout).
  - The corpora listed and counted without python3 (`pyrec_list`, `count_files`: equal to corpus.py's on all nine corpora,
    and `pins --check` without python3 matched every corpus's recorded files, lines and digest).
  - The round trips in records mode: the print read by the native reader, `compare --deferred` (levels 1 and 4 judged; 2, 3
    and 5 the oracle tier's; a known `:code-lines` file not "gone"); `--record` refused (it needs every level: live).
  - verify mode: python3 runs, each answer compared with its record, ORS6 (a)'s verdict (`oracle_verdict python`), the crossed
    pair (one file's live dump against another's record) required to differ.
  - `bin/repr-python records record|verify [--sample N] [CORPUS...|outcomes]`: every record made by the checks in that mode,
    one pack a corpus made once at the end; recorded twice from nothing on the VM, the packs were byte for byte the same.
- **The fragment** (gt9's `test/gate/crops/python.edn` and `python-records.edn`, the main host's: queue 213's report holds
  them and the patch for `test/oracle-records/`): `:python` only on the oracle tier's `python-records-verify`,
  `python-roundtrip-oracle` and `pyi-oracle`. Checked in a scratch overlay (gt9-verbs, oracle-records-scheme, this branch,
  212's patch): `bin/gate --full --plan` and `--nightly --plan` show no step on `:python`, and the essential keys are the same
  with python3 off `PATH`; `bin/gate --oracle --plan` runs the 14 Python oracle rows with python3 and shows them absent
  without it.
- **Sizes**, extrapolated from 24 files a corpus: the packs about 410 MB raw, 50 MB xz (stdlib 18, site-packages 11,
  lib-test 21; scopes besides), within ORS11's 100 MB; committed: the essential dumps 1.9 MB and the manifests about 4 MB.
