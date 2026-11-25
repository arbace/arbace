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
    `host: :python ID`): spec-check, samples, gen --check's seventeen, `smoke/*.py`, `repl/*.in`, `wheels/*` (the samples'
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

## As built: 214, pyi's embedding for the re-seed window W2 (2026-11-05, the Mac VM, queue 214; PYS28 (a))

The work doc/W-WINDOW.md §5 lists for pyi (W-P1-W-P10 decided), built on `mac/214-python-embed` for the main host to take into W2's
candidate `window-w2`. Nothing here is built into an executable: no seed, no `bin/arbace-go`, no gate run (light checks only).

- **goi's table lists pyi's runtime** (`go/arbace/gotable.edn`): `arbace/py` and the libm port it imports, `arbace/libm` (the Haskell
  line's), and ci's back end `arbace/ci` and `arbace/crt` (queue 208's DECISION 1 (a), decided for this re-seed: pyi's C library cores
  run ci's interpreter compiled under goi). `arbace.go.table/program-path?` takes the four as packages of the program's go/;
  j2g --program copies `go/arbace/py` and `go/arbace/libm` into the program beside ci's (`arbace/j2g/main.ae`).
- **Not listed** (W-P6 (a)): pyexpat and expat, and by the same rule the other lazy modules, the C library host `clib`, the glue
  `pybz2`, `pylzma`, `pyzstd` (queue 208z) and `_ssl`'s fallback store `pyssl`: each is interpreted from its forms on its module's
  first import, none is imported by `arbace/py`, so the table need not name them. They stay out of every key (the crop list).
- **The front end embedded**: `arbace/j2g/embed.ae` drops `arbace/python{.ae,/}` and `arbace/repr/python{.ae,/}` (PYS29 (a): no
  stdlib embedded). On this branch's keys (Python's, before gt9's keys commit): `arbace_embedded_sources`, `gate_key`, the audit's
  `:gate` and `steps.edn`'s `:tools` follow; `gate-key-crops-test.bash` moves them to the kept side. In W2's form (gt9's
  `arbace_crop_paths`) the same is a patch for the main host: the four paths leave the list and `crop-rules`, `go/arbace/{py,libm}`
  join `arbace_runtime_not` (W-P3 (a): out of the runtime key, in `gate_key` and the javac key), and `go/arbace/{clib,pybz2,pylzma,
  pyzstd,pyssl}` join the list (queue 212's finding 3, and pyzstd).
- **`RT.toolSource` takes `arbace/python`** (a REQUEST: arbace/lang is the runtime): `arbace/python.ae` and `arbace/python/*` are
  reloaded from the checkout when they differ from the embedded copy, as repr/python's already are (`arbace/repr/`).
- **pyi's runtime builds for tamago** (found by the table's check, run): goi's table checks a listed package in all four
  configurations, and `arbace/py` had 464 errors of goi's checker in tamago/amd64 and tamago/arm64 (TamaGo's syscall has no Linux
  sockets, mmap, fcntl, select, resource, spawn, umask, rusage). Twelve files are now `//go:build linux` (`builtins_{socket,
  socket_table,select,mmap,fcntl,fcntl_table,resource,posix_ext,posix_ids,posix_table,posix_spawn,posixsubprocess}.go`) and one new
  file, `builtins_os_other.go` (`//go:build !linux`), gives the rest of the runtime what it names of them on tamago: Linux's numbers,
  no process time, no umask, no `*at` path but the current directory's, a socket type that is never made. `posix.go`, `signal.go` and
  `time.go` name four helpers in place of Linux-only syscall names (`sysUmask`, `ignorePipeSignals`, `cpuTimeNs`, `oNofollow()`; the
  last a function, as O_NOFOLLOW differs between amd64 and arm64 and a constant's `:val` would be one architecture's). On linux
  nothing changes. `bin/pyi runtime read` reads the `!linux` files under tamago/arm64 and merges them into `go/arbace/py.ae`
  (`runtime_read_other`), so the round trip keeps them.
- **goi honours `//go:build` in a loaded package** (a REQUEST: arbace/go is goi's): the REPL's package state (`arbace.go.repl/
  source-of`) checked every file of a namespace it loaded, a `!linux` file included; it now selects them as `load/package-source`
  does. And the checker verifies a package's `:init-order` (written for the package's own `:config`) in a configuration that leaves
  files out as the order of what is left (`load/package-source`'s `:excluded`, `arbace.go.check`).
- **crt builds for tamago** (a REQUEST for C's owner): `arbace/crt` had 8 checker errors on tamago (`syscall.Access`, `Stat_t`'s
  `Atim`/`Mtim`/`Ctim`); `sys_linux.go` and `sys_other.go` hold them now.
- **`bin/pyi table-check [--configs C,...] [--tables]`** (`test/python/table_check.ae`, as `bin/oi table-check`): the four packages
  listed, taken by `program-path?`, checked by goi's checker in every configuration (linux's by `bin/goi table-configs`, embed-gaps'),
  `--tables` the generator over them alone. On the VM, tamago/amd64 and tamago/arm64: ok (py 138 files, about 20 s a configuration;
  with `--tables` 123 s, 3.7 GB). The generator over the four alone: 332 signatures, 82 invokers, 28 adapters, 19 stub families
  (784 stubs), 9 files; each tamago configuration 368 functions, 231 methods, 102 types, 144 variables, 250 members without a typed
  invoker (called by reflection); the digest 845,330 bytes (184,961 compressed) a configuration. The linux configurations need linux's
  std converted (`bin/goi table-configs`): the main host's.
- **The table's size** (W-P7 (a)): `bin/goi table --size` links the executable with and without the table, so it needs W2's build;
  its line is recorded here by day after W2 (the main host's command: `bin/goi table --size` after `ARBACE_GO_ARCHES=amd64
  bin/arbace-go --build`, W2's step 05).
## As built: 217, `_ssl` round two (2026-11-05, the Mac VM, queue 217)

209's `_ssl` passed test_ssl's sample (176 equal) and asyncio's TLS tests. 217 measured what was still short of
CPython: test_ssl's classed methods, a wider sample of modules that use `ssl`, and probes over MemoryBIOs that compare
pyi with `python3 -I -B` beyond test_ssl. It made OpenSSL's behaviour where Go's TLS allows it, and recorded the rest
as by design.

### What was built

- **The SNI callback's alert.** `_servername_callback` answers a refusal with an alert: the integer the callback
  returns, handshake_failure when it raises, internal_error for another type. Go's server always sends
  internal_error when `GetConfigForClient` fails. The transport sees the records the engine writes, and that alert is
  still in the clear (before the server's hello), so it puts the callback's description in Go's alert record. The
  unraisable is CPython's (`PyErr_FormatUnraisable`'s text, object None).
- **The message callback** (`_msg_callback`, kept and never called before). The transport carries the records, and
  the Python thread feeds and flushes them with the GIL held, so the callback runs there. It reports each record's
  header (SSL3_RT_HEADER, 256) and the messages still in the clear: the handshake up to the change of keys, change
  cipher spec (SSL3_MT_CHANGE_CIPHER_SPEC, 257) and alerts. The versions are OpenSSL 3.5's: the context's highest
  before the server hello, the negotiated one after (supported_versions read from the hello). A received change
  cipher spec is reported under TLS 1.3 only, as OpenSSL 3.5 does. A callback's exception is raised by the operation
  (PySSL_ChainExceptions).
- **Verification failures send OpenSSL's alerts** (ssl_x509err2alert): unknown_ca for 18, 19 and 20,
  certificate_expired for 10, bad_certificate for 62, 64 and 66-68, and so on. Go sends bad_certificate. Under
  TLS 1.2 that record is in the clear, and the transport rewrites its description.
- **The security level.** `set_ciphers`' `@SECLEVEL=N` now sets the level; before, it was read and dropped. The cipher
  string is cut as OpenSSL's rule parser cuts it: a word ends at `@`, a `@KEYWORD` ends its item, and `DEFAULT` counts
  only as the first word. `load_cert_chain` refuses a leaf key below the level (EE_KEY_TOO_SMALL) and a chain key
  below it (CA_KEY_TOO_SMALL). It refuses no signature digest: OpenSSL 3.5 loads a SHA-1 leaf at level 2.
  Verification adds X509_verify_cert's level checks: 66 for the leaf's key, 67 for an issuer's key, 68 for a
  signature digest. Go refuses SHA-1 signatures itself; from level 1 on, that refusal is code 68.
- **PEM.** A block OpenSSL's PEM reader cannot decode is "PEM lib" at the certificate step. Go's `pem.Decode`
  silently skips such a block.
- **The automatic chain** (ssl_add_cert_chain without SSL_MODE_NO_AUTO_CHAIN). A certificate loaded without its chain
  is sent with the chain that the context's store builds for it, root included.
- **Sessions.**
  - Before its first ticket, a TLS 1.3 client's session has no id and a ticket_lifetime_hint of 0.
  - A server's session is reported; it was None.
  - OP_NO_TICKET stops TLS 1.2's tickets only: OpenSSL's TLS 1.3 server then sends stateful tickets, which the client
    sees as tickets. Go's switch covers both versions, so the server's configuration follows the version the client
    hello will get.
- **memoryview** (`memoryview.go`, a fix outside `_ssl`). `memoryview(view)` keeps the view's start, stride and
  format. Before, a non-contiguous view passed through `memoryview()` became contiguous, so `MemoryBIO.write` (and
  every `y*` argument) accepted it instead of raising BufferError. This fixes test_ssl's
  `MemoryBIOTests.test_buffer_types`, which `:buffer-exports`' pattern had matched though it was not that cause.

### By design (what Go's crypto/tls does not give; `:ssl-go-tls` and the probes)

- **Kept from 209:**
  - TLS 1.3 post-handshake authentication (HAS_PHA False). `verify_client_post_handshake` raises CPython's
    NotImplementedError; with OpenSSL it raises EXTENSION_NOT_RECEIVED.
  - TLS-PSK.
  - finite-field DHE.
  - SSLv3.
  - the cipher each side prefers.
- **The TLS 1.3 exporter secret in the key log.** Go logs four secrets a connection, OpenSSL five
  (test_keylog_filename).
- **What the message callback cannot see.** Messages under the keys are not reported: TLS 1.2's Finished, TLS 1.3's
  encrypted handshake, and the inner content types. Go's `tls.Conn` keeps its keys.
- **TLS 1.3's verification alerts.** They go out under the handshake keys, so the peer sees Go's bad_certificate where
  OpenSSL sends unknown_ca or certificate_expired.
- **`get_channel_binding("tls-unique")` under TLS 1.3 is None.** Go gives no Finished for TLS 1.3, where RFC 9266
  leaves tls-unique undefined; OpenSSL returns its Finished.
- **SHA-1 signatures at security level 0.** OpenSSL accepts them; Go's x509 refuses them at any level and has no
  switch to allow them since Go 1.24.
- **TLS 1.2 with OP_NO_TICKET resumes nothing.** Go's server has no session-id cache; OpenSSL's has one.

### Samples (run on the VM)

| sample | result |
|---|---|
| `bin/pyi lib-test-all --fresh test_ssl` | 206 methods: 176 → **180 equal**, 30 → 26 classed (20 → 17 `:ssl-go-tls`, 4 `:ssl-finalization`, 5 cpython-only, `:buffer-exports` 1 → 0), 0 failing, on each of the three runtime commits |
| `test/python/pyi/d-ssl-callbacks.py` (new) | equal to `python3 -I -B`: the SNI alerts, the clear records and messages under TLS 1.2 and 1.3, a callback's exception, a non-contiguous view |
| `test/python/pyi/d-ssl-levels.py` (new, with `ssl/levels/`) | equal to `python3 -I -B`: set_ciphers' levels, load_cert_chain's checks, 8 verification failures with their TLS 1.2 alerts, a client certificate refused, the automatic chain, sessions under TLS 1.2 and 1.3 with and without OP_NO_TICKET. Before 217: the run stopped at `set_ciphers("DEFAULT@SECLEVEL=1")` |
| `test/python/pyi/d-ssl.py` (209's) | still equal |
| asyncio before the changes: `test_asyncio.test_events test_streams test_sendfile test_ssl test_sslproto` | 515 methods: 485 equal, 30 classed, 0 failing; the only TLS methods among the classed are `:ssl-finalization`'s 8 |
| asyncio at the head: `test_asyncio.test_ssl test_sslproto test_streams test_selector_events test_base_events` | 386 methods: 371 equal, 15 classed (8 `:ssl-finalization`, the rest not TLS's), 0 failing |
| `bin/pyi lib-test-all --fresh test_httpservers test_logging test_xmlrpc test_urllib` | their TLS tests all equal (test_httpservers' HTTPS server, test_logging's HTTPS handler, test_urllib's, test_xmlrpc's ssl presence); the 7 failures are not `_ssl`'s (logging's exception tracebacks and `__del__` at shutdown, two 300 s timeouts under the VM's load) |

### Narrowed and left

- **Narrowed.** `:ssl-go-tls` lost test_msg_callback_tls12, test_sni_callback_alert and test_sni_callback_raising.
  test_buffer_types passes.
- **`:ssl-finalization` stays PYS9's.** Its 4 test_ssl methods and 8 asyncio methods need an SSL object freed by
  `gc.collect()`.
- **The engine goroutine of an abandoned SSL object is not ended.** This is 209's Low; it needs the same finalizer.
## Design: 216, a register-allocating compiled loop (2026-11-05, the Mac VM, queue 216; the user's (a) on 210's DECISION)

210's profiles were flat: what is left of the compiled code's cost is its design, each instruction a helper call that
pops its operands off the frame's heap stack (`f.Stack`, a slice: a bounds check and a cleared slot per pop, an append
per push, a write barrier per pointer stored) and pushes its result there, with `f.Lasti` stored before every
instruction. The change: `arbace.python.aot` keeps the value stack in Go locals within a basic block and writes the
frame back only where something may read it. The runtime (eval.go's loop, gen.go, the exception unwinding) is
unchanged; the compiled code's new helpers take their operands as arguments and return their results (aot.go).

**What may observe a running frame's stack or `f.Lasti`**, read from go/arbace/py at 5b34c021:

| observer | reads | when |
|---|---|---|
| exception unwinding (eval.go's runFrames) | `f.Lasti` (the traceback entry `Traceback{Frame, PC: f.Lasti}`, the handler `handlerAt(f.Lasti)`, a handler's pushed lasti); `f.Stack` popped to the handler's depth, then the exception pushed | the compiled function returns an error |
| a call of a Python function (`*pushFrame`), a class's `__init__` inlined | the arguments on `f.Stack` (callStack, newFrame read them in place); on the callee's return its value pushed on `f.Stack` and the function re-entered at `f.PC` | the function returns |
| generators' and coroutines' suspension (yield-value, yield-from, the await family: operations run by AotExec) | `f.Stack` kept in the frame while suspended; resumed at `f.PC`, or thrown into at `f.Lasti` (gen.go sets `f.PC = f.Lasti` and re-runs the yield) | the function returns a value with `f.Suspended` |
| every operation run by AotExec (the rarer ones, the registered ones) | `f.Stack` (operands A items down, e.g. list-extend, dict-merge) | inside the call |
| a frame object's `f_lineno`, `f_lasti`, `tb_lineno` (tbobj.go, `sys._getframe`, `traceback.extract_stack`, warnings' stack levels) | `f.Lasti` | from code the instruction calls (a dunder method, a property, `__hash__`, a warning) |
| `f_locals`, `locals()`, `frame.clear` | `f.Fast`, `f.Cells`, `f.LocalsMap`: never the value stack; `frame.clear` of a running frame raises | anytime |
| the eval breaker (signals, threads' switch) | runs only in runFrames, at a frame's entry and at calls | at a call |
| per-instruction caches (load-global, load-attr, store-attr) | the instruction's index: load-global read it from `f.Lasti` | inside the call |
| `sys.settrace`, `sys.setprofile` | nothing: no-ops in pyi (sys.go), so no line event to keep | - |

Code that the instruction calls (a dunder method run by a nested loop, a builtin) runs on its own frame: it cannot
read this frame's value stack, only its `f_lineno` through `f_back` or `sys._getframe(1)`, and `f_locals`.

**The rules the generator follows:**

1. *The stack's shape is static.* Each instruction's depth is computed as ir/verify computes it (every path the same
   depth); a method call's load-attr (210's `AotLoadMethod`) pushes one item more up to its call. At any point of the
   generated code, slots `[0, m)` are in `f.Stack` (whose length is then `m`) and slots `[m, d)` in Go locals `s<slot>`;
   an instruction that pops a slot below `m` first takes those slots into locals (`f.Stack[k]` read by constant index,
   then the stack truncated).
2. *Block starts* are where the runtime may enter the function, or where control merges: instruction 0, the handlers'
   targets, the jumps' targets, the instruction after a call, every operation run by AotExec and the instruction after
   it (a yield is re-run at `f.Lasti` when thrown into). At a block start every slot is in `f.Stack`; the code falling
   into it, or jumping to it, writes its locals back first (one append). The entry switch on `f.PC` lists only the
   block starts; any other PC is AotBadPC's SystemError, never a silent resumption with a wrong stack.
3. *Calls and suspensions write everything back*: a call (`AotCall`, `AotCallMethod`), raise, reraise and every
   AotExec operation (yields among them) run with the whole stack in `f.Stack`, as today. A generator suspended in the
   middle of an expression (`f(a, (yield x))`) therefore keeps `f`, `a` in its frame across the suspension.
4. *Exceptions:* an instruction that may raise writes back, on its error path only, the slots its handler keeps (up to
   the handler's depth of the innermost handler covering it, `handlerAt`'s); the slots above are discarded by the
   unwinding anyway. No handler: nothing is written back (the frame is left). A handler deeper than the instruction's
   depth after its pops (never made by the compiler) makes the generator fall back to 210's stack code for that code
   object.
5. *Positions:* `f.Lasti` is stored before an instruction that may raise or run Python code (so a traceback, a nested
   `f_lineno`, a warning's stack level and the per-instruction caches see the instruction's index), and before every
   return, raise, reraise, call and AotExec operation, as today; an instruction whose only error is an unbound name
   (load-fast, load-deref, delete-fast, delete-deref) stores it on its error path; the rest (constants, stores to
   locals, stack shuffles, builds of tuples and lists, `is`, jumps) store nothing. load-global takes its cache's index
   as a constant. Tracebacks keep their lines: CPython's positions are by instruction, pyi's `co_positions` has lines
   only (no columns, so no carets: unchanged).
6. *Fallback:* a code object whose depths do not hold, or that the generator cannot place, is compiled by 210's stack
   generator (kept, unchanged), so the change is never a refusal.

The proof asked: the tracebacks' lines of sample programs that raise in mid-block, in calls, in nested calls'
dunders, in generators and coroutines (suspended mid-expression, thrown into, closed) and in handlers, compiled, on
the loop and on CPython (the caret lines, which pyi does not print, filtered); `make python-essential`,
`bin/pyi check --aot --no-pyi` on the synthetic sample, 210's check210.sh, `bin/pyi runtime check`; the bench sample's
table, base and branch back to back with CPython.

## As built: 216, the register-allocating compiled loop (2026-11-05, the Mac VM, queue 216)

Built as "Design: 216" above: `arbace.python.aot`'s register generator (`reg-func`) beside 210's stack generator
(`code-func`, kept as the fallback for a code object whose stack's shape is not static: none of the samples' code
objects needed it, the compile logs print "aot: the stack generator for ..." when one does), and aot.go's helpers that
take operands as arguments (`AotDrop`, `AotSpill`, `AotLoadGlobalV` with its cache's index a constant, `AotLoadMethodV`,
`AotNext`, `AotTruthy2`, ...; the old helpers stay for the fallback). In the generated function the slots are Go locals
`s<slot>` declared once at its top (gotos cross no declaration), the entry switch lists only the block starts, an
instruction's error path writes back the handler's slots and stores `f.Lasti` when the instruction stores it nowhere
else. `check-depths` (off) makes each block start check the frame's depth; the samples below ran once with it on.
Locals are never cached in Go variables (load-fast reads `f.Fast` every time, store-fast writes it at once), so
`f_locals` and the frame's other readers see what they saw before.

A function before and after (`x = a + b * 2` in a function): `AotLoadFast(f,0)`, `AotLoadFast(f,1)`, `AotConst`,
`AotBinary(f, BinMul)`, `AotBinary(f, BinAdd)`, `AotStoreFast` over `f.Stack`, `f.Lasti` stored six times; now
`s0 = f.Fast[0]`, `s1 = f.Fast[1]`, `s2 = consts[0]`, `f.Lasti = 3; s1, err = py.BinaryOp(py.BinMul, s1, s2)`,
`f.Lasti = 4; s0, err = py.BinaryOp(py.BinAdd, s0, s1)`, `py.AotStoreFastV(f, 4, s0)`, the frame's stack untouched.

**The table** (the process's wall seconds, the least of 3 rounds; each round runs CPython, the base and this branch back
to back on the same benchmark; the VM's load 5-9, two other agents running: differences under 10% are noise; CPython
3.14.10 `python3 -s -B`; base 5b34c021 (210's runtime and generator), after this branch's head; loops as
test/python/bench/benchmarks.tsv; `/root/arbace-mac/.tmp/q210r/table1.tsv`):

| benchmark | loops | CPython | base | after | base / CPython | after / CPython |
|---|---:|---:|---:|---:|---:|---:|
| raytrace | 1 | 0.20 | 0.70 | 0.61 | 3.5x | 3.0x |
| deltablue | 50 | 0.12 | 0.49 | 0.44 | 4.1x | 3.7x |
| fannkuch | 1 | 0.29 | 0.83 | 0.70 | 2.9x | 2.4x |
| generators | 4 | 0.16 | 0.79 | 0.77 | 4.9x | 4.8x |
| nqueens | 4 | 0.26 | 0.75 | 0.67 | 2.9x | 2.6x |
| go | 1 | 0.10 | 0.31 | 0.26 | 3.1x | 2.6x |
| richards | 10 | 0.34 | 0.78 | 0.71 | 2.3x | 2.1x |
| chaos | 1 | 0.08 | 0.25 | 0.23 | 3.1x | 2.9x |
| json_dumps | 1 | 0.03 | 0.22 | 0.21 | 7.3x | 7.0x |
| spectral_norm | 4 | 0.35 | 0.83 | 0.70 | 2.4x | 2.0x |
| unpack_sequence | 20000 | 0.23 | 0.85 | 0.63 | 3.7x | 2.7x |
| coroutines | 20 | 0.36 | 0.82 | 0.78 | 2.3x | 2.2x |
| float | 4 | 0.22 | 0.55 | 0.48 | 2.5x | 2.2x |
| nbody | 3 | 0.30 | 0.60 | 0.35 | 2.0x | 1.2x |
| hexiom | 1 | 0.03 | 0.09 | 0.09 | 3.0x | 3.0x |
| base64 | 1 | 1.58 | 3.78 | 3.01 | 2.4x | 1.9x |
| bpe_tokeniser | 1 | 3.26 | 7.07 | 6.48 | 2.2x | 2.0x |
| async_tree | 1 | 0.23 | 1.14 | 1.03 | 5.0x | 4.5x |
| mdp | 1 | 0.82 | 3.26 | 3.08 | 4.0x | 3.8x |
| pprint | 1 | 1.39 | 5.07 | 4.81 | 3.6x | 3.5x |

Sum over the 20: CPython 10.35 s, base 29.18 s (2.82x), after 26.04 s (2.52x); median 3.05x -> 2.67x. The register
generator pays most where the code is arithmetic and stack shuffling between few calls (nbody 2.0x -> 1.2x,
unpack_sequence 3.7x -> 2.7x, base64 2.4x -> 1.9x, spectral_norm, fannkuch); call-, attribute- and generator-heavy
benchmarks gain 5-15%: their profiles are now the runtime's (attribute caches, newFrame and freeFrame, runFrames'
return for every Python call, a nested run for every generator resumption from Go), which this change leaves alone.

**The proofs (run, the VM):**
- Tracebacks' positions: `t/check216.sh` (the report's appendix) compiles `tb.py` (exceptions mid-block in multi-line
  expressions, an unbound local, a dunder and a property called mid-expression reading `sys._getframe(1).f_lineno`, a
  recursion, a generator suspended mid-expression then raising, thrown into and closed, a coroutine raising after a
  suspension, nested handlers with `with` and `finally`, a warning's stack level, the uncaught exception's traceback, a
  callee writing its caller's local through `f_locals`) and runs it compiled and on the loop: the compiled output is the
  loop's byte for byte, and both differ from CPython's (the caret lines filtered: pyi has no columns) only in the 16
  known lines of one pre-existing class, the compiler's (the next paragraph) and `f_locals`' write-through.
- `make python-essential`: check --aot 21 programs, 20 pass, 1 classed (as-pyi), 0 failing, the control refused.
- `bin/pyi check --aot --no-pyi --only-list` of 210's 73-program sample, with `check-depths` on: 73 pass, 0 failing.
- 210's `check210.sh` (edge, meth, mixed, nums, frames): PASS, compiled and on the loop.
- `bin/pyi runtime check`: the seven packages the same forms, the tables and runtime-ops equal, 135 of 136 programs equal (e-executable.py timed out at load 15, its subprocesses on goi; run alone, `bin/pyi run` = CPython, 2 min 17 s), the REPL session equal (the check does not run compiled code: the runtime's change is aot.go's additions).

**Found on the way (pre-existing, not the compiled loop's):** pyi's compiler (arbace.python.compile) sets an
instruction's line per statement (`stmt*!`'s `:line`), so in a multi-line expression every instruction has the
statement's first line where CPython locates each sub-expression's instructions at the sub-expression (`b.missing` on
the next line of `y = (x +`, the third item of a list display, a call's argument); tracebacks, `f_lineno` and warnings
show the first line. And pyi's `frame.f_locals` is a snapshot, not PEP 667's write-through proxy. Both are the loop's too.

## After 216's decisions (2026-11-05 evening, the user: 1 (a), 2 (a))

- **2 (a), per-expression lines (8f23d75e):** arbace.python.compile's `expr!` sets the expression's line while it
  compiles and restores the statement's after (CPython's codegen locates each instruction at the node that emits it).
  Tracebacks, `f_lineno` and warnings' stack levels in multi-line expressions are now CPython's: of tb.py's 16 known
  diff lines 15 are gone (the one left is `frame.f_locals`' write-through, pre-existing). Every module's IR changes
  (its line table only). Checked (run, the VM): `t/check216.sh` PASS (1 known), `t/check210.sh` PASS,
  `make python-essential` 0 failing, `bin/pyi runtime check` 136 of 136 programs equal and the REPL equal,
  `bin/pyi lib-test --aot test_exceptions` 109 methods, 0 failing. The main host's night: `bin/pyi check --aot --record`
  and Lib/test's traceback-sensitive modules (test_traceback, test_exceptions, test_warnings, test_sys_settrace,
  test_dis, test_compile, test_code, test_frame, test_inspect, test_doctest, test_pdb).
- **1 (a), direct calls between compiled functions** (210's option (b)): the next step, a follow-up after 216's merge.
## As built: 215, `_zstd`'s glue, and `_sqlite3` measured (2026-11-05, the Mac VM, queue 215; 208's remainder, 208z)

- **`go/arbace/pyzstd`** (package `arbace/pyzstd`): CPython 3.14.10's `Modules/_zstd/` (`_zstdmodule.c`, `compressor.c`,
  `decompressor.c`, `zstddict.c`, `buffer.h`, the clinic files' argument parsing; 2,567 + 1,176 lines) ported statement for
  statement over zstd 1.5.7's committed node program (`go/arbace/clib/zstd`), through `arbace/clib` as `_bz2` and `_lzma`
  are. ZstdCompressor (levels, options, the three end directives, `set_pledged_input_size`, `last_mode`), ZstdDecompressor
  (`max_length`, `needs_input`, `eof`, `unused_data`, the unconsumed input kept in C memory with CPython's
  `in_begin`/`in_end`), ZstdDict (`dict_id`, `dict_content`, `as_digested_dict`, `as_undigested_dict`, `as_prefix`, its
  digested ZSTD_CDicts by level and its ZSTD_DDict), `train_dict`, `finalize_dict` (through `pyzstd_finalize`),
  `get_param_bounds` (through `pyzstd_bounds`), `get_frame_size`, `get_frame_info`, `set_parameter_types`, ZstdError and the
  constants (the version, `ZSTD_CLEVEL_DEFAULT` and `ZSTD_DStreamOutSize` asked of the library). ZSTD_inBuffer and
  ZSTD_outBuffer are C structs in the library's memory, laid out by `pyzstd_layout`. Lazy as `_lzma`
  (`builtins_zstd_api.go`); compiled, aot.ae's main links `arbace/pyzstd` and cf2g's `arbace/clib/zstd/aot`
  (`bin/pyi-clib aot zstd`: 8.8 MB of Go, 60 s, 1.8 GB). `compression.zstd` is the tarball's Python over it.
- **No threads.** zstd is built without `ZSTD_MULTITHREAD`: `nb_workers`' bounds are (0, 0), test_zstd's three
  multithreaded tests skip as on CPython over such a libzstd. The GIL is held around every library call; the objects'
  PyMutex locks are the GIL.
- **The reference.** Alpine's python3 3.14.10 has no `_zstd`, so `ref-cpython314-lib-test-all.edn` records test_zstd as a
  skipped module (no methods). `tools/pyi/clib/refmod.bash zstd OUT` builds CPython's own `Modules/_zstd` by the host's cc
  as an extension of the oracle's python3 (its pyconfig.h, the tarball's Include/) over the same pinned zstd with
  lib.conf's flags, kept by key (8.6 s); `bin/pyi record-ref lib-test-all` puts it on the reference's PYTHONPATH. Not in
  `bin/pyi-clib` nor `tools/pyi/clib/zstd/`, which are in the node program's key.
- **zstd's node program rebuilt** with `-DNDEBUG` (CPython's release flags): divsufsort.c's `assert` had put `__FILE__`,
  the building checkout's absolute path, into the node program, so `bin/pyi-clib verify zstd` could only pass in the
  checkout that built it (q208's worktree). The rebuild also refreshes its `Key`, which 213's change to `bin/pyi-clib`
  (6e159c3b) had left stale for all three libraries (bzip2's and xz's still are: `bin/pyi-clib build bzip2`, `build xz`).
- **`py`: "cannot set ... attribute of immutable type" names tp_name** (`compression.zstd.ZstdCompressor`,
  `collections.deque`; builtins bare), as CPython.
- **`:pyio-buffered`**: `_pyio`'s BufferedReader `_readinto` computes `avail` without what it wrote already (an upstream
  `_pyio` bug) and its `seek` does not check `raw.seekable()`; CPython with `io = _pyio` fails the same two test_zstd tests.
- **Samples (the VM, run).**
  - `z1.py` (every entry of `_zstd` and `compression.zstd`: levels, options and their errors, incremental modes,
    pledged sizes, `max_length`, chunked input, skippable and multiple frames, trained, finalized, raw, digested,
    undigested and prefix dictionaries, pickling, subclassing, immutability, ZstdFile in binary and text): 76 lines
    equal to CPython over the reference `_zstd`, compiled (`bin/pyi aot`).
  - `z0.py` (z1 without the dictionary training, smaller inputs): 67 lines equal on goi, 35 s with the start.
  - test_zstd compiled (`bin/pyi aot test/python/libtest_run.py test_zstd SKIP`): 121 methods, 119 as CPython's (116 ok,
    3 skipped: the multithreaded ones, as CPython's), 2 `:pyio-buffered`; 9 s with the module built.
  - test_zstd on goi (the driver under `bin/pyi run`, 40 minutes' cap): 19 methods ran in the cap, all as CPython's;
    setUpModule (130 KB compressed with a checksum, a dictionary trained on 300 samples) and the 3-byte chunked
    decompressions dominate. ci interpreted under goi (`:clib-on-goi`, 208's DECISION 1 (a)): about 10 KB/s for zstd's
    compressor (20 KB in 2.1 s), so the whole module takes hours on goi until the re-seed lists arbace/ci and arbace/crt.
- **`_sqlite3`: the amalgamation through ci, measured.** `tools/pyi/clib/sqlite/` (lib.conf: sqlite-3.50.4.0, the
  SPDX pin; `NDEBUG`, `SQLITE_THREADSAFE=0`, `SQLITE_OMIT_LOAD_EXTENSION`, `SQLITE_DEFAULT_MEMSTATUS=0`,
  `SQLITE_DISABLE_INTRINSIC`, and `__atomic_load_n`/`__atomic_store_n` as plain loads and stores; `pysqlite.c`, the
  entry). `bin/pyi-clib verify sqlite` (the build into `.tmp/pyi-clib/sqlite/verify`, nothing committed): 6 min 36 s,
  6.7 GB peak, a node program of 5,481,087 bytes and 4,012 functions (5.8 MB of forms): not 208's estimate of 25-30 MB.
  The amalgamation's 262,899 lines are 99,911 preprocessed (zstd's 30 files: 91,084), and gcc -O0's text is 1.13 MB
  (zstd's 1.31 MB): SQLite costs about what zstd costs. Through `arbace/clib` (ci's interpreter compiled, a scratch Go
  program) `sqlite3_open(":memory:")`, a table, 200 inserts in a transaction, an index, an error ("no such column"),
  and an aggregate query with a `filter` clause print what gcc's build of the same files prints (205 statements in
  90 ms). Two things ci lacks, met on the way: the `__atomic` builtins (the first build faulted in `sqlite3_open` on
  "undefined function `__atomic_store_n`"), and an answer of 0 for `__has_extension(c_atomic)` (its preprocessor
  answers as the reference compiler, so `SQLITE_DISABLE_INTRINSIC` alone kept SQLite's atomics).
- **`_sqlite3`'s plan** (not built; the glue is `Modules/_sqlite/`'s 6,563 lines plus 2,826 of clinic, about 3.5 times
  `_zstd`'s):
  1. Where the node program lives: committed as forms like zstd's (5.8 MB), or built into a keyed cache (208's
     DECISION 2 (a), decided on the 25-30 MB estimate). The DECISION in 215's report.
  2. Callbacks: `create_function`, aggregates, window functions, collations, the authorizer, progress and trace
     handlers are C calling Go. ci's natives are Go functions the node program calls by name (`ci.Natives`):
     `pysqlite.c` declares extern trampolines (`pysqlite_func`, `pysqlite_step`, `pysqlite_final`,
     `pysqlite_collation`, `pysqlite_authorizer`, ...) whose addresses the glue passes to SQLite, and the Go glue
     registers them before `clib.Open`; the first probe is that ci takes the address of an extern native.
  3. The port, file by file as CPython's: module.c (constants, adapters' registry, `connect`), connection.c,
     cursor.c, statement.c, row.c, blob.c, microprotocols.c, prepare_protocol.c, util.c; `Lib/sqlite3` is the
     tarball's.
  4. Threads: the first slice is `SQLITE_THREADSAFE=0` (`sqlite3.threadsafety` 0, which test_dbapi accepts) under
     the GIL, one C thread; a busy handler then sleeps holding the GIL, so a test where another Python thread holds
     the lock ends in "database is locked" after the timeout instead of waiting: a class until 208's design (a
     `ci.Thread` per Python thread, the GIL released around the library's sleeps; a ci change, the main host's).
  5. Proof: test_sqlite3's sample compiled against the CPython reference. Alpine's python3 has `_sqlite3`, but over
     the system's SQLite 3.54.1, not the pinned 3.50.4 (`sqlite3.sqlite_version`, and the tests that branch on it):
     the reference's `_sqlite3` then built by `tools/pyi/clib/refmod.bash` over the pinned amalgamation, as `_zstd`'s.

## Decided (2026-11-05 evening, the user): 215's DECISIONs

- **DECISION 1 (a), decided:** SQLite's node program is committed as forms, as zstd's is (checked by `bin/pyi-clib verify`,
  the same loading path: lazy on goi, cf2g's Go compiled), superseding 208's DECISION 2 (a) for SQLite.
- **DECISION 2 (a), decided:** the main host re-records `ref-cpython314-lib-test-all.edn` at night with the reference's
  `_zstd` (and `_sqlite3`) of `tools/pyi/clib/refmod.bash` on the oracle's PYTHONPATH.

## After the decisions: `_sqlite3` (2026-11-05, the Mac VM, queue 215)

- **bzip2's and xz's node programs rebuilt** (only their `Key` moved, 213's change to `bin/pyi-clib`): `bin/pyi-clib
  verify bzip2` and `verify xz` say byte for byte again (30 s and 3 min 22 s).
- **SQLite's node program, `go/arbace/clib/sqlite`**, built with the options CPython's own build of the amalgamation
  gives it (PCbuild/sqlite3.vcxproj: math functions, FTS4, FTS5, R*Tree, `SQLITE_OMIT_AUTOINIT`) beside the first slice's
  (`SQLITE_THREADSAFE=0`, `SQLITE_OMIT_LOAD_EXTENSION`, `SQLITE_DEFAULT_MEMSTATUS=0`, `SQLITE_DISABLE_INTRINSIC`, plain
  atomics, `NDEBUG`): 7,865,270 bytes of node program, 8.3 MB of forms, 7 min 45 s and 8.1 GB on the VM; no absolute path.
  `tools/pyi/clib/sqlite/pysqlite.c` holds the callbacks' extern natives and their registering wrappers
  (`pysqlite_create_function`, `_window_function`, `_collation`, `pysqlite_set_authorizer`, `_progress_handler`,
  `pysqlite_set_trace`), the doubles as their bits (`pysqlite_column_double`, `_value_double`, `pysqlite_bind_double`,
  `_result_double`) and `sqlite3_db_config`'s boolean form. Compiled, cf2g's `arbace/clib/sqlite/aot`.
- **`go/arbace/pysqlite3`** (package `arbace/pysqlite3`, 4,800 lines of Go): CPython 3.14.10's `Modules/_sqlite/`
  (module.c, connection.c, cursor.c, statement.c, row.c, blob.c, microprotocols.c, prepare_protocol.c, util.c, clinic's
  argument parsing and its deprecation warnings) ported statement for statement. Connection (its `__init__`, so subclasses
  and `factory=` work; the statement cache an `lru_cache` over the connection, as CPython's), Cursor, Row, Blob (the
  mapping protocol, slices), Statement, PrepareProtocol, the adapters (`__adapt__`, `__conform__`, the registry) and
  converters (declared types and column names), the legacy and PEP 249 transaction control, `executescript`, `backup`,
  `serialize`/`deserialize`, limits and config, `iterdump`, the audit events, the exceptions with `sqlite_errorcode` and
  `sqlite_errorname`. The callbacks (functions, aggregates, window functions, collations, the authorizer, progress and
  trace handlers) are ci's natives (`ci.Natives`, registered before the library opens): SQLite's context pointers are
  handles of the package's table standing for the Go values; a callback's exception that CPython's thread state keeps
  across `sqlite3_step` (a collation's) is held pending and raised where CPython tests `PyErr_Occurred`. `strerror` is a
  native too: musl's is not in the node program the builder linked (SQLite's unix VFS calls it only to log). py gains
  `ExtWarn`, `ExtWriteUnraisable`, `ExtCurrentIdent`, `ExtImportModule` (`builtins_sqlite3_api.go`).
- **Threads and memory.** One C thread under the GIL (`sqlite3.threadsafety` 0, accepted by test_dbapi). A connection and
  its statement cache form a cycle Go's collector does not finalize: a connection never closed keeps its database open
  to the process's end, and gives no ResourceWarning (`:finalization`). crt's fcntl takes no lock, so two processes are
  not excluded (`:sqlite-process-locks`; connections of one process are, by SQLite's own lock table).
- **The reference's `_sqlite3`.** Alpine's python3 links SQLite 3.54.1; `tools/pyi/clib/refmod.bash sqlite` builds
  CPython's `Modules/_sqlite` over the pinned 3.50.4 with lib.conf's flags (its pyconfig.h taking back
  `PY_SQLITE_ENABLE_LOAD_EXTENSION`), on record-ref's PYTHONPATH with `_zstd`'s. test_sqlite3 over it: 510 ok, 4 skipped,
  as the committed record (Alpine's).
- **Samples (the VM, run).** `s1.py` (the module's surface: rows, errors with their codes, text factories, adapters and
  converters, functions, aggregates, window functions, collations, trace, authorizer, progress, autocommit, blobs,
  iterdump, backup, serialize, limits, config, a file database through a Connection subclass): 59 lines equal to CPython
  over the reference `_sqlite3`, compiled and on goi (36 s). test_sqlite3: 514 methods, 506 equal, 8 classed
  (`:testcapi` 3, `:finalization-collect` 2, `:finalization` 1, `:surrogate-pairs` 1, `:sqlite-process-locks` 1), 0
  failing, on goi (`bin/pyi lib-test-all --fresh test_sqlite3`, 13 min, against the committed record) and compiled (about
  70 s). SQLite through ci under goi is quick enough for the whole module, unlike zstd's dictionary training.

## Design: 216d, direct calls between compiled functions (2026-11-05, the Mac VM, queue 216d; the user's 1 (a) on 216's DECISION)

The aim (216's DECISION 1 (a), 210's option (b)): a compiled function calling a compiled function without going back
through runFrames' generic dispatch, and generators resumed in the caller's loop, with frames, exception unwinding,
tracebacks, the C level's accounting and the recursion limit as before. No behaviour change.

**What a call costs, probed first** (fib(35) compiled, the VM, least of 3): a callee run by a loop of its own from the
caller's Go code (runFrames entered for the callee, the caller's Go frames kept under it) costs what the pushed frame
costs (2.4 s both): the trip back to runFrames and the caller's re-entry by its entry switch is not where a call's
time goes. It goes to the frame (newFrame, bindArgs, freeFrame and the allocations of a free list of 4 per code object:
a recursion allocates frames as it descends), the boxed ints, and the compiled code's own traffic around a call: every
local written back onto `f.Stack` (AotSpill), the arguments copied off it and the slots cleared (callStack), the value
pushed and the locals taken back after the call's block start. So the direct call is placed in the register
generator, where it removes that traffic, not only in the runtime.

**The design:**

1. *A call from the locals.* The register generator (216's `reg-func`) compiles call [N KWNAMES] and a method call
   (after `AotLoadMethodV`) as `tmp, err = py.AotCallV(t, f, kw, callable, args...)` (`py.AotCallMethodV(t, f, m,
   self, args...)`): the callable and its arguments are Go locals (Go's variadic array, kept on Go's stack: never
   retained by the helper, copied where a callee may keep it), the locals below them stay in Go locals across the
   call, the value comes back in a local. The helpers take callStack's cases in callStack's order (a Python function, a
   bound method with self in the callable's place, a class whose `__init__` callInit runs, a bound built-in, any other
   callable called): a compiled Python function's frame is made by newFrame as before and run directly (runDirect),
   a generator's or coroutine's object made, a built-in or any other callable called as callStack calls it.
2. *The stack's way, when the callee is not compiled* (code compiled at run time, on the loop) *or `directMax` (256)
   direct calls are nested*: the helper answers `py.AotStackCall` before doing anything; the generated code then writes
   everything back, sets `f.PC` and calls AotCall or AotCallMethod as before (the callee's frame pushed for runFrames),
   and the frame re-enters at the call's next instruction by a re-entry label of its own, `C<n>`, in the entry switch,
   which takes the slots the direct way keeps in locals back from the frame. The call's next instruction is no longer
   a block start (unless a jump or a handler makes it one: then both ways meet there with everything written back).
3. *runDirect* runs the callee by `runFrames(callee, direct)`: a loop with the callee as its entry frame, entered as
   runFrames enters a pushed frame: `f_back` the caller, `escape` passed on, `t.depth` counted against the recursion
   limit (exceeded: the RecursionError returned to the caller's code and raised at the call, as the pushed frame's is
   raised in the caller), the eval breaker checked at the callee's entry with its error raised in the callee
   (`f.Lasti = f.PC`, as for a pushed frame; a call from Go raises it in the caller instead), and no C level counted
   (`t.cdepth` is Go's entries into the machine: a Python call is none, so a pure Python recursion still goes past
   10,000 levels as before). The callee's own calls run in that loop (or directly again), its exceptions unwind
   through its handlers there; one that leaves it has the callee's traceback entry, the caller's code returns it
   through its error path (the handler's slots written back, 216's rule 4) and the caller's loop adds the caller's
   entry, the order and the context chaining as before. callInit's instance replaces the value (an `__init__`
   returning anything but None: the TypeError raised in the caller). A Go panic in the callee is caught by the
   callee's guardedStep and raised at the callee's instruction, as before; guardedStep now also restores the thread's
   frame and direct depth should a panic cross a direct call.
4. *What may observe the caller during the call* (216's table): its value stack is not observable from Python; the
   locals the direct way keeps in Go variables are the value stack's slots, kept across a call as 216 keeps them across
   an instruction that runs Python code by a nested run (a dunder, a property). `f.Lasti` is stored before the call
   (tracebacks, `f_lineno` from the callee, warnings' stack levels, the caches); `f.PC` only on the stack's way (only
   runFrames' re-entry reads it). `f_locals` and the frame's other readers see `f.Fast`, unchanged. A generator or
   coroutine calling is never suspended in the middle of a direct call (a callee's yield suspends its own frame).
5. *Go's stack*: each direct call holds its caller's Go frames (the compiled function, the helper, runDirect,
   runFrames, guardedStep), so at most `directMax` (256) are nested on a thread at once (`t.direct`); deeper, the calls
   go the stack's way in the innermost loop, with no Go frames per Python call, so a recursion limit of any size works
   as before.
6. *Frames*: a code object's free list keeps 16 frames instead of 4 (a recursion's descent no longer allocates every
   frame past the fourth; memory only: the frames are cleared as before), and guardedStep runs a compiled function
   itself rather than through step (step's large Go frame entered for one call per resumption).

**Generators resumed in the caller's loop.** A compiled for-iter already resumes its generator from the caller's Go
code with the caller's values in Go locals (216: `AotNext(it)`, the iterator a local), so the caller-side traffic the
direct call removes is not there. What is left is the resumption itself (resume, run's C level, runFrames' entry, the
generator's entry switch and its yield through AotExec). CPython's way (FOR_ITER_GEN, SEND_GEN: the generator's frame
pushed into the caller's loop, its yield returning to the caller in that loop) would replace one Go call by two trips
through runFrames per item and per level of a `yield from` chain (the caller left and re-entered by its entry switch
for every item), and run's C level would have to be emulated (a resumption from Go counts one today: removing it
changes where a deep generator nesting raises); probed by the calls above, a trip through runFrames is not cheaper than
a Go call, so it is not built. The resumption gains what the call path gains in the runtime (guardedStep running the
compiled body without step).

**The proofs asked:** a recursion and traceback program (recursion limits from 60 to 3,000 and 15,000 levels with the
limit raised; tracebacks through functions, methods, `__init__`, bound methods, keywords, closures, lambdas; exceptions
caught and raised again mid-chain with causes, contexts and `finally`; `f_back`'s chain and the callers' `f_lineno`
from a callee; a callback from `sorted` raising; a recursion through `map` running out of C levels at the same depth as
before; RecursionError caught in a handler past the direct bound; recursive generators, a generator and a coroutine
raising; callees compiled at run time called mid-expression from compiled code, the stack's way, and calling back) run
compiled and on the loop against CPython, its known lines recorded on the base (ce1b4e71); 216's check216.sh and
210's check210.sh; `make python-essential`; `bin/pyi check --aot --no-pyi` on 210's 73-program sample; `bin/pyi runtime
check`; the bench sample's table, base and branch back to back with CPython.

## As built: 216d, direct calls between compiled functions (2026-11-05, the Mac VM, queue 216d)

Built as "Design: 216d" above: `go/arbace/py` (eval.go: `runFrames(f, direct)`, `enter`, `callDirect`, `runDirect`,
`directMax` 256, guardedStep running `Code.Native` itself and restoring `t.Frame` and `t.direct` on a panic; aot.go:
`AotCallV`, `AotCallMethodV`, `AotStackCall`, `stackCall`, `callFunctionV`; frame.go: `Thread.direct`, `frameFreeMax`
16) and `arbace.python.aot`'s register generator (`call!`: calls and method calls from the locals, the stack's way
re-entered at `C<n+1>`; with `check-depths` on, the re-entry checks the frame's depth too). The stack generator
(`code-func`, the fallback) and AotExec's call-ex reach compiled callees through `enter`, directly as well. Generators'
resumption is unchanged but for step's frame (the design's paragraph: CPython's in-loop resumption not built).

Where the time went (fib(35) compiled, least of 3, the VM): base 2.4-2.6 s; the stack's direct call alone (a nested
loop per call, the locals still written back) the same; the free list of 16 alone 1.95 s; the calls from the locals
alone 1.95 s; both 1.42 s (CPython 0.11 s for fib(31), pyi 0.31 -> 0.2 s).

**The table** (wall seconds, the least of 3 rounds; each round runs CPython 3.14 (`python3 -s -B`), the base
(ce1b4e71, 216's head) and this branch back to back on the same benchmark; the VM's load 3.7-4.3, another agent
running: differences under 10% are noise; `/root/arbace-mac/.tmp/q216d/table1.tsv`; every output equal to CPython's):

| benchmark | loops | CPython | base | after | base / CPython | after / CPython |
|---|---:|---:|---:|---:|---:|---:|
| raytrace | 1 | 0.20 | 0.60 | 0.58 | 3.0x | 2.9x |
| deltablue | 50 | 0.12 | 0.43 | 0.38 | 3.6x | 3.2x |
| fannkuch | 1 | 0.29 | 0.61 | 0.61 | 2.1x | 2.1x |
| generators | 4 | 0.15 | 0.78 | 0.68 | 5.2x | 4.5x |
| nqueens | 4 | 0.25 | 0.66 | 0.64 | 2.6x | 2.6x |
| go | 1 | 0.10 | 0.25 | 0.23 | 2.5x | 2.3x |
| richards | 10 | 0.32 | 0.63 | 0.51 | 2.0x | 1.6x |
| chaos | 1 | 0.07 | 0.21 | 0.20 | 3.0x | 2.9x |
| json_dumps | 1 | 0.03 | 0.20 | 0.20 | 6.7x | 6.7x |
| spectral_norm | 4 | 0.34 | 0.68 | 0.61 | 2.0x | 1.8x |
| unpack_sequence | 20000 | 0.23 | 0.62 | 0.61 | 2.7x | 2.7x |
| coroutines | 20 | 0.36 | 0.71 | 0.62 | 2.0x | 1.7x |
| float | 4 | 0.22 | 0.48 | 0.46 | 2.2x | 2.1x |
| nbody | 3 | 0.30 | 0.35 | 0.35 | 1.2x | 1.2x |
| hexiom | 1 | 0.03 | 0.09 | 0.09 | 3.0x | 3.0x |
| base64 | 1 | 1.60 | 2.96 | 2.96 | 1.8x | 1.8x |
| bpe_tokeniser | 1 | 3.25 | 6.41 | 5.92 | 2.0x | 1.8x |
| async_tree | 1 | 0.22 | 1.01 | 0.98 | 4.6x | 4.5x |
| mdp | 1 | 0.81 | 2.76 | 2.69 | 3.4x | 3.3x |
| pprint | 1 | 1.40 | 4.70 | 4.24 | 3.4x | 3.0x |

Sum over the 20: CPython 10.29 s, base 25.14 s (2.44x), after 23.56 s (2.29x); median 2.67x -> 2.61x. The call-heavy
benchmarks gain 10-20% (richards, deltablue, coroutines, spectral_norm, pprint), generators 13% (step's frame), the
arithmetic and attribute-bound ones nothing; the base is faster here than in 216's table (load 4 against 5-15). What
is left of a call is its frame (newFrame, bindArgs, freeFrame), the defer of guardedStep, the boxed ints; of a
generator's item, one resumption per level (resume, run, runFrames' entry, the yield through AotExec).

**The proofs (run, the VM):**
- `t/check216d.sh` (rec.py, the design's program, 70 output lines): compiled = the loop byte for byte, both = CPython
  but for the one line recorded on the base (where a recursion through `map` runs out of C levels: pyi's count, 10,001
  as on the base; CPython's own stack); on the base the same. RecursionError's depth at limits 60-3,000, the
  tracebacks and the frames' chains equal CPython's.
- 216's `t/check216.sh` PASS (1 known line, `f_locals`), 210's `t/check210.sh` PASS (10 pass lines); both also with
  `check-depths` on, and the 73-program sample with it on: 73 pass, 0 failing, no code object on the stack generator.
- `make python-essential`: check --aot 21 programs, 20 pass, 1 classed, 0 failing, the control refused.
- `bin/pyi runtime check`: the seven packages the same forms, the unicodedata and CJK tables equal, runtime-ops equal, 136 programs of test/python/pyi run by pyi as by the oracle: 136 equal, the REPL session 1 equal (40 min at load 4; the check runs the loop, not compiled code: it covers runFrames' new parameter, guardedStep and the free list).

## Design: 219, `frame.f_locals` as PEP 667's write-through proxy, and the frame's other views (2026-11-05, the Mac VM, queue 219)

216 found `frame.f_locals` a snapshot (tbobj.go's `FrameLocals`: a new dict of the fast locals and cells): a callee's
`sys._getframe(1).f_locals["x"] = 2` did not reach its caller's `x` (tb.py: CPython `(1, None, 2, 2)`, pyi
`(1, None, 1, 1)`, loop and compiled alike). Read from go/arbace/py and arbace/python/aot.ae at 40ab1e13, and from
CPython 3.14.10's Objects/frameobject.c (`framelocalsproxy_*`, `frame_locals_get_impl`, `frame_clear_impl`,
`frame_lineno_set_impl`) and Python/bltinmodule.c's locals, exec and eval.

**What reads or writes a frame's locals from outside its code:**

| observer | CPython 3.14 | pyi at 40ab1e13 | 219 |
|---|---|---|---|
| `frame.f_locals`, a function's frame | `FrameLocalsProxy`: reads and writes the frame's localsplus (a cell's or free variable's contents through the cell), a name none of the code's in `f_extra_locals`; no delete of a local (ValueError) | a new dict each time | the proxy (below) |
| `frame.f_locals`, a module's or class's body, exec's frame | the namespace object itself (`__prepare__`'s or exec's mapping) | `f.Locals`, the dict (a dict subclass's dict, not the instance) | `f.LocalsMap` when set, else `f.Locals` |
| `locals()` / `vars()` / `dir()` in a function | a new dict each call: `dict(proxy)` (extra locals included), a snapshot | a new dict of fast locals and cells | the proxy's snapshot |
| `locals()` in a module or class body | the namespace (its mapping) | `f.Locals` | as f_locals |
| `exec`/`eval` without locals in a function | that snapshot: writes do not reach the frame | the same | unchanged |
| `exec(code, f.f_globals, f.f_locals)` (pdb, code.interact, the debuggers) | any mapping as locals: the name operations through the proxy, so writes reach the frame | a dict or dict subclass only ("locals must be a mapping"; `:function-constructor`'s rest) | any mapping (`PyMapping_Check`): `f.LocalsMap`, the name operations through its methods |
| `frame.clear()` | executing: RuntimeError; a suspended generator's: RuntimeError; a generator's not started: closed; else locals, stack, extra locals, f_trace dropped | no `clear` | as CPython (executing: on the current thread's chain of frames or a running generator's) |
| `f_trace`, `f_trace_lines`, `f_trace_opcodes` | stored; the trace function called per event | absent (AttributeError) | stored, no event (below) |
| `f_lineno = N` | a jump, allowed only in a trace function on a line event ("f_lineno can only be set in a trace function" outside) | AttributeError | always the outside answer: pyi runs no trace function |
| `sys.settrace`, `sys.setprofile`, `threading.settrace` | events | no-ops (sys.go) | unchanged: a DECISION (below) |
| `inspect` (`getargvalues`, `currentframe().f_back.f_locals`, `FrameInfo`), `traceback.StackSummary`'s `capture_locals`, `warnings` | read f_locals | read the snapshot | read the proxy: the same values |

**Where the locals live, and why no write-back or reload point is needed.** A function's locals are `f.Fast` (the
fast locals) and `f.Cells` (its cells, then the free variables' cells), heap memory the frame owns for its whole life,
also after it returned (an escaped frame is never reused: frame.go's `releaseFrame`). The loop reads and writes them
in place. The compiled code does too: 216's register generator (`reg-func`) emits `(aget (.-Fast f) i)` at every
`load-fast`, `AotStoreFastV` at every `store-fast`, `(.-Value (aget (.-Cells f) i))` at every `load-deref`; it keeps
only the *value stack* in Go locals within a block, never a local variable's value across an instruction, and 210's
stack generator (the fallback) keeps nothing. So the proxy writes the memory the code reads: a write made by any code
the frame calls (a callee through `f_back`/`sys._getframe(n)`, a dunder, a property, a warning's filter, a signal
handler, another thread between two instructions) is seen at the frame's next load, compiled or not, with no
write-back before and no reload after. What a Python-level view can never see is the value stack, the one thing 216
holds in Go locals: f_locals never shows it (CPython's neither). An operand already on the stack keeps its value:
`(x, poke(), x)` gives `(1, None, 2)` in CPython and in pyi alike. Hence the rule the compiled code keeps, written here
for later optimisations (unboxed locals, a local held in a Go variable across a loop): **a local may live in a Go
variable only between two instructions that cannot run Python code**; one that keeps it across a call, an AotExec
operation, a dunder dispatch or anything that may raise into a handler must store it to `f.Fast` before and load it
after (a callee's `f_locals` write is not statically visible: `sys._getframe` is dynamic). 219 adds no instruction
to the compiled code: 216's and 216d's speed is unchanged by construction (measured below).

**The proxy** (`FrameLocalsProxy`, tbobj.go): the names in `co_localsplusnames`' order (the fast locals, a parameter
that is a cell read and written through its cell as pyi's code reads it, `CellParams`; then the cells that are no
parameter; then the free variables), each with its value when bound; a key found by its text when it is an exact str,
else by hash and `==` (a str subclass, an impostor with str's hash: `framelocalsproxy_getkeyindex`); unhashable keys
TypeError. `__getitem__` (KeyError "local variable 'KEY' is not defined"), `__setitem__` (a local's slot or cell, a
free variable's cell: the defining frame's variable changes; otherwise the frame's extra locals), `__delitem__`
(ValueError for a local, the extra locals' delete), `__contains__`, `__len__`, `__iter__`, `keys`/`values`/`items`
(lists, as CPython's), `get`, `setdefault`, `pop` (ValueError for a local), `update` (a dict or a proxy only),
`copy` (a dict), `__reversed__` (a list), `__or__`/`__ror__`/`__ior__`, `__eq__` (proxies by frame, a dict by
content), `__repr__` (the dict's), unhashable, unpicklable ("cannot pickle 'FrameLocalsProxy' object"), the
constructor `FrameLocalsProxy(frame)` with frameobject.c's messages, a mapping for `match` (Py_TPFLAGS_MAPPING) and
`collections.abc.Mapping` (`_collections_abc` registers `type(sys._getframe().f_locals)`). The extra locals are a
dict on the frame, read by the proxy and the `locals()` snapshot, never by the code (CPython's: a name that is not a
local of the code object is no variable of the function).

**By design (pyi's IR, not the proxy):** PEP 709's hidden locals (`CO_FAST_HIDDEN`: an inlined comprehension's
variables while it runs) are not marked in pyi's IR, so a write to such a name while the comprehension runs goes to
the slot where CPython puts it in the extra locals; pyi's inlined comprehension swaps its cells and locals in the
function's slots (load-fast-and-clear, load-cell-and-clear), so reads see what CPython's do (test_frame's
`test_closure_with_inline_comprehension`, `test_write_with_hidden`). `frame.clear()` finds an executing frame on the
current thread only (the other threads' frames are not walked: pyi's threads hold the GIL in turn, a frame of a thread
blocked elsewhere is not detected).

**Tracing (`sys.settrace`, `sys.setprofile`, `f_trace`'s events, `f_lineno` jumps): not built here.** pyi's
`sys.settrace` is a no-op (the class `:tracing`); `f_trace` is stored and never called. Building it is a task of its
own, sketched for its decision: (1) the loop calls the trace function on call, line (a new line or a backward jump,
from the IR's line table), return, exception and opcode events while a thread's trace function is set (one branch per
instruction on the loop, taken only then); (2) the compiled code cannot report lines, so a frame that is traced runs
on the loop: runFrames enters `Code.Native` only when no trace function is set, and a compiled frame already running
when one is set (pdb's `set_trace` sets `f_trace` on its caller's frames) hands itself to the loop at its next block
start, where the frame holds its whole stack (216's rule 2: a check of a thread flag per block start, the cost to
measure); (3) `f_lineno` jumps from a line event need CPython's `mark_stacks` over pyi's IR (the kind of each stack slot
at each instruction: iterator, exception, object; the jump's errors are named by them) and the jump's legality rules;
(4) test_sys_settrace's expected event sequences follow CPython's line table instruction by instruction (a loop's
head re-lined at each iteration, `with`'s exit, `RESUME`'s call event), each difference a compiler line fix. See
DECISION in queue 219's report.

**The proof asked:** tb.py's f_locals line equal to CPython's (check216.sh's known line gone), compiled and on the
loop; test_frame's and test_scope's methods that need it (`bin/pyi lib-test-all test_frame test_scope`), test_inspect's,
test_sys_settrace's and test_pdb's that need f_locals rather than tracing; `bin/pyi runtime check`, `make
python-essential`, check216/check216d/check210 PASS; `bin/pyi bench`'s sample, CPython, base and branch back to back
(210's method), to show 216's speed kept.

## As built: 219, `frame.f_locals` as PEP 667's proxy, and the frame's other views (2026-11-05, the Mac VM, queue 219)

Built as "Design: 219" above, on 40ab1e13: no change to `arbace.python.aot` or to the compiled code's helpers (aot.go,
eval.go untouched); the runtime's frame views (tbobj.go, frame.go, code.go, funcattrs.go, match.go, one line in gen.go:
a generator's frame knows its generator, `f_generator`), locals()/exec (builtins.go, builtins_exec.go, import.go), and
one compiler change found on the way:

- **`FrameLocalsProxy`** (tbobj.go) as the design's table: the slots in `co_localsplusnames`' order computed once per
  code object (`Code.slots`), CPython's key lookup (exact str by text, else hash and `==`), the extra locals, every
  mapping method of frameobject.c with its message, the constructor, unhashable and unpicklable, a `match` mapping.
  A module's or class's frame answers its namespace (its mapping when `__prepare__`'s or exec's is no dict) unless an
  inlined comprehension of the body has its own names bound (PEP 709's hidden locals: then a proxy over them, CPython's
  `_PyFrame_HasHiddenLocals`). A name twice among the slots (an inlined comprehension's own name in a fast slot beside
  the function's cell of that name: GH-128396's case) reads its first bound slot, as CPython's one slot holding the plain
  value.
- **The frame's rare views in one word** (`Frame.views`, a `*frameViews`: the extra locals, `f_trace`, `f_trace_lines`,
  `f_trace_opcodes`), nil until set, so a call's frame grows by two words (`views`, `gen`), not six.
- **`frame.clear()`**: executing (the current thread's chain, a running generator) and suspended refused with CPython's
  messages; a generator's frame not started closes the generator; else the locals, cells, value stack, extra locals and
  `f_trace` dropped, the code, globals, builtins and `f_back` kept (`f_locals` then `{}`).
- **`f_trace`** stored, **`f_trace_lines`/`f_trace_opcodes`** booleans with the member's and getset's messages,
  **`f_lineno = N`** "f_lineno can only be set in a trace function" (or "lineno must be an integer"), the read-only
  attributes "is not writable"; the frame's attributes also getset descriptors of the type (`type(frame).f_locals`).
- **`locals()`, `vars()`, `dir()`**: a function's the proxy's snapshot (extra locals included), a namespace's itself;
  **`exec`/`eval`** take any mapping as the locals (`PyMapping_Check`), its name operations through it (`LocalsMap`), so
  `exec(code, f.f_globals, f.f_locals)` writes the frame's locals (pdb's `default`, `code.interact`); `import *` into a
  mapping namespace through its `__setitem__`; exec's "locals must be a mapping or None, not T".
- **`co_cellvars`** in `co_localsplusnames`' order (a parameter's cell at the parameter, then the others: CPython's
  from the kinds), pyi's `Code.Cells` order (the closure's index, sorted) unchanged.
- **The compiler's local order** (arbace.python.compile, 79acc2dd): a function's locals were the parameters then the
  symbol table's locals *sorted*; CPython's `co_varnames` are the parameters then each name as the compiler first emits a
  name operation on it (`u_varnames`). Now the same (the unit starts with the parameters, `add-local!` at each name
  operation, which it already called), so `f_locals`', `locals()`' and `co_varnames`' order are CPython's (fl.py's
  `varorder`: a `def`, a `for` target, a `with` target, an `except` name, an inlined comprehension's name). Every
  function's IR changes (its slot numbers only): the IR cache is keyed by the compiler's digest, so the night re-records.

**What stays by design:** tracing (`sys.settrace`, `sys.setprofile`, `threading.settrace`, `f_trace`'s events,
`f_lineno` jumps) is not built: pyi calls no trace function and `:tracing` stays deferred (DECISION in queue 219's report,
the design's sketch above); `f_lineno` writes therefore always get the answer CPython gives outside a trace function.
PEP 709's hidden names are not marked in pyi's IR (a write to one while its comprehension runs goes to the slot, CPython's
to the extra locals). `frame.clear()` does not find a frame executing on another thread. A weakref's referent dropped by
`frame.clear()` is not collected (test_frame's `test_clear_locals`, `test_clear_locals_after_f_locals_access`, and
`test_entry_frames_are_invisible_during_teardown`'s weakref callback): pyi's weak references hold their referent until
218's finalizer queue (`:finalization-collect`, 218's).


**The bench (run, the VM, 2026-11-06):** `bin/pyi bench`'s 20-benchmark sample, 210's method: each round CPython, base (40ab1e13, built in a
worktree of it) and branch back to back, 3 rounds, the least wall seconds; load 5-6 (218 and 220 alongside); every output
equal to CPython's (table1.err empty).

| benchmark | CPython | base | after | base / CPython | after / CPython |
|---|---:|---:|---:|---:|---:|
| raytrace | 0.20 | 0.58 | 0.62 | 2.9x | 3.1x |
| deltablue | 0.13 | 0.39 | 0.40 | 3.0x | 3.1x |
| fannkuch | 0.29 | 0.62 | 0.62 | 2.1x | 2.1x |
| generators | 0.15 | 0.67 | 0.67 | 4.5x | 4.5x |
| nqueens | 0.26 | 0.64 | 0.63 | 2.5x | 2.4x |
| go | 0.10 | 0.23 | 0.23 | 2.3x | 2.3x |
| richards | 0.32 | 0.50 | 0.51 | 1.6x | 1.6x |
| chaos | 0.08 | 0.20 | 0.20 | 2.5x | 2.5x |
| json_dumps | 0.03 | 0.20 | 0.19 | 6.7x | 6.3x |
| spectral_norm | 0.33 | 0.60 | 0.61 | 1.8x | 1.8x |
| unpack_sequence | 0.23 | 0.63 | 0.61 | 2.7x | 2.7x |
| coroutines | 0.36 | 0.61 | 0.60 | 1.7x | 1.7x |
| float | 0.22 | 0.45 | 0.45 | 2.0x | 2.0x |
| nbody | 0.30 | 0.34 | 0.34 | 1.1x | 1.1x |
| hexiom | 0.03 | 0.09 | 0.09 | 3.0x | 3.0x |
| base64 | 1.56 | 2.80 | 2.81 | 1.8x | 1.8x |
| bpe_tokeniser | 3.17 | 5.70 | 5.74 | 1.8x | 1.8x |
| async_tree | 0.23 | 0.99 | 1.00 | 4.3x | 4.3x |
| mdp | 0.80 | 2.98 | 2.85 | 3.7x | 3.6x |
| pprint | 1.44 | 4.38 | 4.37 | 3.0x | 3.0x |

Sum: CPython 10.23 s, base 23.60 s (2.31x), after 23.54 s (2.30x); median 2.5x both. Every difference is within 5% and
both ways (mdp 4% faster, raytrace 7% slower in its least of three, 0.04 s; raytrace and deltablue again, 5 rounds: base 0.59 and 0.39, after 0.58 and 0.39): noise at this load. 216's and 216d's speed is kept, as
the design expected (no instruction added to the compiled code; a frame two words larger).

**The proofs (run, the VM):** fl.py (`t/check219.sh`, 66 lines: proxy reads and writes through locals, cells and
free variables, the mapping methods and their errors, class and module scope, the locals() snapshot, exec/eval into
f_locals and into a user mapping, clear, f_trace, f_lineno, f_generator, the constructor, str-like keys, hidden locals,
co_varnames' order) equal to CPython's compiled and on the loop; 216's tb.py with no known line left (its f_locals line
`(1, None, 2, 2)` as CPython's); check216d.sh and check210.sh PASS; `make python-essential` 20 pass, 1 classed, 0
failing; the 73-program `check --aot` sample 73 pass; `bin/pyi lib-test-all test_frame test_scope` 23 failing -> 3
(the 3 the finalizer queue's, 218's), test_scope 0 failing; `bin/pyi lib-test test_generators` 43 -> 45 equal, 0
failing; `bin/pyi runtime check` the same forms, the tables and runtime-ops equal, 137 of 138 programs equal (e-executable.py
past the 120 s cap at load 5-8: 138 s run alone, its output equal to the base's), the REPL equal. test_inspect does not
import on pyi (`_pickle`, pre-existing). **Decided (the user, 2026-11-06): 219's DECISION (a)**, tracing a task of its own
after 219.
## Design: 218, the finalizer queue (2026-11-05, the Mac VM, queue 218; PYS9 (a), decided 2026-11-04)

pyi has no reference counts: `weakref.ref` holds its referent, `gc.collect()` returns 0, no `__del__` runs, and
`identityHash`'s map keeps every object ever hashed by identity (`id()`, the default `__hash__`) alive for good. This
section is what CPython guarantees, what Go's collector can give of it, and what stays by design.

**What CPython guarantees** (3.14's Objects/typeobject.c, weakrefobject.c, genobject.c, Python/gc.c):
- *Refcount death* (subtype_dealloc): `__del__` (tp_finalize, slot_tp_finalize) first; when it did not resurrect the object,
  its weak references are cleared and their callbacks called (PyObject_ClearWeakRefs). The list's order: the basic ref
  (no callback, reused by `ref(o)`), the basic proxy, then the refs with callbacks, the newest first (insert_after); the
  callbacks are called in that order with the weak reference, an exception going to `sys.unraisablehook` as "Exception
  ignored while calling weakref callback %R". A weak reference that died first is out of the list: no callback.
- *PEP 442*: `__del__` runs at most once per object; an object it resurrects is not finalized again. Cyclic trash: weak
  references to it are cleared (callbacks only for refs outside the trash), the finalizers called, the cycle broken.
  `gc.garbage` holds what cannot be freed (DEBUG_SAVEALL); `gc.callbacks` are called with ("start", info) and ("stop",
  info), info's `generation`, `collected`, `uncollectable`; an exception "Exception ignored while calling GC callback %R".
- *`__del__`'s exception*: "Exception ignored while calling deallocator %R" (the bound `__del__`).
- *Generators* (_PyGen_Finalize): a generator suspended at its death is closed (GeneratorExit thrown in; an exception
  "Exception ignored while closing generator %R"); a coroutine never started warns "coroutine '...' was never awaited"
  (RuntimeWarning through `warnings._warn_unawaited_coroutine`); an async generator with sys's finalizer hook (asyncio's
  `_asyncgen_finalizer_hook`) is handed to the hook instead ("Exception ignored while finalizing generator %R").
- *Sockets* (sock_finalize): an open socket warns ResourceWarning "unclosed %R" (source the socket) and is closed.
- *`weakref.finalize` at exit*: Lib/weakref.py's own atexit hook, over weak references with callbacks.

**How Go's collector serves it.**
- *Weak references.* Go's `weak` package is out of goi's scope (gotable.edn's `:out`), and pyi's runtime runs interpreted
  by goi until W2 lists it compiled (and whenever the checkout's runtime differs from the embedded one). So the weak
  reference keeps its referent's address hidden from the collector (a `uintptr` and the pointer's element type) and
  rebuilds the object through `reflect.NewAt`; the referent gets a Go finalizer at its first weak reference (probed,
  compiled and under goi: `.tmp/q218/probe/refl`). Go's heap does not move, and an object with a finalizer is kept
  until its finalizer ran: the finalizer clears the address under a mutex before anything can free it, so no weak
  reference ever yields freed memory. `runtime.AddCleanup` would not resurrect the object (no `__del__`), and is generic:
  goi cannot call it.
- *A tracked object's record* (finTrack) lives in a table keyed by its address, never holding it: its weak references in
  CPython's order, whether a `__del__`, a generator's close or a socket's warning is due, and its identity once hashed.
  `identityHash` moves an object's number there (an instance keeps it in its own field), so hashing no longer pins it.
- *The queue.* Go's finalizer goroutine only moves the dead object (resurrected by Go for the call) into pyi's queue and
  sets an eval-breaker bit; the thread holding the GIL drains it at the eval breaker (a frame's start, a backward jump)
  and in `gc.collect()`. Python code never runs on Go's finalizer goroutine.
- *The order, refcount's*: an object with a `__del__` due is finalized first and its Go finalizer set again without it
  (PEP 442's once-only); found unreachable again, its weak references are cleared and their callbacks called, newest
  first, the basic `ref(o)` reused as CPython does. A resurrecting `__del__` simply keeps the object reachable.
- *`gc.collect()`*: the callbacks' "start"; rounds of `runtime.GC()`, a wait until Go's finalizer goroutine has been quiet
  for a moment (there is no public barrier: `blockUntilEmptyFinalizerQueue` is the runtime's tests' own) and a drain,
  until a round drains nothing (at most 8); "stop" with the count; that count returned.
- *Generators and coroutines*: a generator gets the finalizer when it first suspends inside a try region (its code has
  handlers: only then can `close()` be observed); a coroutine at its creation (the unawaited warning), an async
  generator when sys's finalizer hook is set at its first iteration. *Sockets*: at creation, the warning and close.
- *`sys.unraisablehook`*: `writeUnraisable` with CPython's messages, the queue's exceptions never escaping into the code
  the drain interrupted.

**What cannot be served** (stays by design, with its class):
- *The moment* (`:finalization`, PYS9 (a)): nothing happens when the last reference goes, only at a collection.
- *Cycles through a finalized object*: Go never frees a cycle that reaches an object with a finalizer from itself
  (SetFinalizer's documented rule), so such an object's `__del__` never runs and its weak references are never cleared:
  CPython 2's uncollectable cycles, without `gc.garbage`. That includes every class (its MRO holds it) and module-level
  functions (their globals). A new class `:finalization-cycle` (`:kind :design`) takes the methods that need it.
- *goi's registers* (the runtime interpreted): an interpreted frame still running may keep the last value it handled in
  a slot, so an object can survive one more collection than compiled; the compiled runtime (AOT, W2's table) is exact.
- *The weak references themselves* are held by their referent's record: a dropped weak reference's callback still runs.
- `gc.garbage`, `get_referrers`, `get_objects`, `is_tracked`, thresholds and counts keep their shapes, not CPython's values.
## As built: 218, the finalizer queue (2026-11-06, the Mac VM, queue 218; PYS9 (a); the user's decisions 2026-11-06)

What "Design: 218" proposed, built in `go/arbace/py/finalize.go` and its call sites; light samples only (the main host's
whole runs below).

- **Weak references** (`builtins_weakref.go` rewritten): the referent's address hidden in a `uintptr`, rebuilt through
  `reflect.NewAt`, cleared by its Go finalizer under `finMu` before Go can free it; CPython's list order (the basic ref,
  the basic proxy, then the refs with callbacks, newest first), the basic ref and proxy reused, the callbacks called with
  the reference (a subclass's instance: `wrapInstance`), "Exception ignored while calling weakref callback %R";
  `__callback__` None once cleared; `hash` cached, "weak object has gone away"; equality of dead refs by identity;
  CPython's repr (`__name__` looked up on the type, gh-99184); `_remove_dead_weakref`; proxies forward every number
  operator (and pow with a modulus), item assignment and deletion, `__index__`, `__next__`, `__reversed__`, `__bytes__`,
  ReferenceError once dead. A referent of another package's type (it may carry its own Go finalizer: _bz2's, _sqlite3's,
  _zstd's) is held, as before.
- **The queue**: Go's finalizer only queues the object and sets the eval breaker's bit 8; the GIL's holder drains it at a
  frame's start and a backward jump and in `gc.collect()` (the user's decision 2026-11-06 (a): at safe points as built).
  Refcount death's order: the work (`__del__`, once, PEP 442; a generator's close; a socket's warning), the Go finalizer
  set again without it; found unreachable again, the weak references cleared and their callbacks called.
  `PYI_NO_FINALIZERS=1` turns tracking off (the weak references hold, as before 218).
- **`gc.collect()`**: `gc.callbacks` ("start", "stop", info's generation, collected, uncollectable), rounds of
  `runtime.GC()` with a wait until Go's finalizers are quiet (3 ms, at most 500), until a round drains nothing (at most 8),
  nothing at all when nothing is tracked; the classes' stale attribute caches dropped first. It returns 0, as CPython does
  for garbage outside cycles (its count is of cyclic garbage; returning the dead objects handled broke f-struct.py's
  smoke line, `bin/pyi runtime check`).
- **`__del__`**: an instance of a class whose MRO defines it is tracked at creation (`classInfo.hasDel`, set by
  `fixupSlots`), "Exception ignored while calling deallocator %R". **Generators and coroutines**: a coroutine made, a
  generator suspended in a try region, go to a pending list; those still unfinished when it is flushed (every 256, and in
  `gc.collect()`) are tracked (measured, `bin/pyi aot`: a coroutine 0.13 us untracked against 0.6 us tracked at its
  creation; a generator with a try 0.66 us against 1.0-1.2 us tracked at its first suspension); at death a coroutine
  never started warns through `warnings._warn_unawaited_coroutine`, a suspended one is closed, an async generator with
  sys's finalizer hook (kept at its first iteration) is handed to it. **Sockets**: `socket.socket`'s instance warns
  "unclosed %R" (ResourceWarning, its source the socket) and closes its descriptor.
- **Identity**: `identityHash` keeps an instance's number in its `ident` field and a tracked object's in its record (the
  same sequence as before): hashing or `id()` no longer keeps an object alive.
- **Retention found and fixed**: a frame's value stack grown from the array it shares with the fast locals kept its
  stale values (`growStack`, frame.go's push, `AotPush`, `AotSpill`); deque's pop, popleft and trims kept their items;
  goi's `Thread.restore` keeps a returned call's R slots set (a REQUEST commit for goi's owner, `go/arbace/goi/frame.ae`,
  unbuilt on the VM).
- **Not served (classes)**: `:finalization` (the moment, 47), `:finalization-cycle` (Go never frees a cycle through an
  object with a finalizer: 33), `:gc-collector` (the collector's own observables, modules cleared at exit: 20),
  `:weakref-list` (a weak reference lives as long as its referent's record: 3); `:finalization-goi` (41, the user's
  decision 2026-11-06 (a)): they pass compiled, and lag under goi; dropped once W2's stage 0 makes the record.
  `:finalization-collect` keeps the 26 methods of modules not sampled and WeakMethod's two (an attribute site's inline
  cache keeps a deleted method).

**The classes, before and after** (methods named): `:finalization-collect` 61 to 28; `:finalization` 41 to 47 (5 of
test_weakref's mapping tests and test_fatal_coro_warning, whose failure is an earlier test's coroutine finalized late);
new `:finalization-goi` 41, `:finalization-cycle` 33, `:gc-collector` 20, `:weakref-list` 3. test_weakref, test_gc and
test_finalization had no class before: their 96 failing methods are in these. `:ssl-finalization` (12) is unchanged: test_ssl and asyncio's TLS modules were not sampled (the main host's run below places them; sockets now warn and close at a collection).

**The samples** (the VM):
- `bin/pyi lib-test --aot test_weakref test_queue test_gc test_finalization test_dict test_ast test_coroutines`: 817
  methods, 702 equal, 115 classed, 0 failing (test_weakref 115 equal, test_gc 13, test_finalization 2, test_queue all 164).
- The same 12-module sample before the last fixes (test_array, test_list, test_tuple, test_dict, test_scope, test_struct,
  test_queue, test_mmap, test_socket, test_exceptions, test_ast, test_weakref): 2,266 equal; test_socket's 5 failing are
  not finalization's (`sys.getrefcount`'s constant 2, an abstract socket name another agent's run held, a signal's timing).
- `bin/pyi lib-test-all --fresh test_weakref test_gc test_finalization` (goi): 214 methods, 121 equal, 93 classed,
  0 failing (the base 40ab1e13: 94 equal, 24 classed, 96 failing).
- `bin/pyi table-check --configs tamago/arm64`: ok (py 141 files); `go vet` of the printed runtime for linux and tamago.
- `bin/pyi runtime check`: the packages printed and read again the same forms, the tables and runtime-ops equal, 136 of 138
  programs equal before the count fix: f-struct.py (gc.collect's count, fixed, rerun alone: pass; test_gc, test_weakref
  and test_finalization --aot again after it: 130 equal, 84 classed, 0 failing) and e-executable.py
  (status 124: its 2 min 15 s under the VM's load past the smoke limit, the same with `PYI_NO_FINALIZERS=1`: not 218's);
  the REPL equal. `make python-essential`: check --aot 21 programs, 20 pass, 1 classed, 0 failing, the control refused.
- **Decided (the user, 2026-11-06)**: 218's DECISION 1 (a), the compiled-only methods kept in `:finalization-goi` until
  W2's record; DECISION 2 (a), the queue drained at safe points as built.

**The main host's runs**: `bin/pyi lib-test-all --fresh test_weakref test_gc test_finalization` (expected: 0 failing, about
121 equal); `bin/pyi check --corpus lib-test-asyncio` and `bin/pyi lib-test-all --fresh test_sqlite3 test_importlib test_os
test_subprocess test_thread test_threading test_threading_local test_xml_etree test_ssl` to place `:finalization-collect`'s
and `:ssl-finalization`'s rest; `bin/pyi runtime check`; `make python-essential`. After W2: `bin/pyi check --record`, then
`:finalization-goi` dropped.
## Design: 220, the diagnostic C modules (2026-11-05, the Mac VM, queue 220; PYS22 (a))

Four of `:c-modules-deferred`'s modules, ported to Go as 204 ported `_csv` and `mmap`: runtime files of `go/arbace/py`,
function for function from the pinned tarball's C (`Modules/faulthandler.c`, `Python/traceback.c`'s dump functions,
`Modules/_lsprof.c`, `Modules/termios.c`, `Modules/_tracemalloc.c` and `Python/tracemalloc.c`, with their clinic files),
the Python halves (`Lib/cProfile.py`, `profile.py`, `pstats.py`, `tracemalloc.py`, `tty.py`) unchanged. In order of
value, as the brief takes them.

**faulthandler** (1,491 lines of C). Observable: what it writes to a descriptor and the process's exit by the signal.
- `dump_traceback(file, all_threads)`: `Current thread 0x...`/`Thread 0x...` headers (the thread's name in brackets),
  `  File "f", line N in name` per frame, most recent first, `_Py_DumpASCII`'s escapes and 500-character truncation,
  `MAX_FRAME_DEPTH` 100 (`  ...`), `MAX_NTHREADS` 100, `Stack (most recent call first):` for one thread. pyi needs every
  thread's frame chain: `Thread.Frame` per `*Thread` (threadInfos, the main thread first as CPython's list is newest
  first). The dump reads another goroutine's chain without its lock, as CPython reads another thread's frames: a frame
  pointer and an instruction index read whole.
- `enable(file, all_threads, c_stack)`, `disable`, `is_enabled`; the fatal signals SIGSEGV, SIGFPE, SIGABRT, SIGBUS,
  SIGILL: `Fatal Python error: Segmentation fault`, a blank line, the dump, `Current thread's C stack trace (most recent
  call first):` with musl's `  <cannot get C stack on this system>` (no backtrace on musl: CPython on Alpine prints the
  same), then the previous action restored and the signal raised again: the process dies by it (status -11). On pyi a
  fatal signal another process sends is caught through os/signal on a channel of faulthandler's own; one pyi raises
  itself (`_sigsegv`, `_read_null`, `_sigabrt`, `_sigfpe`, `_stack_overflow`) runs the handler in the raising thread
  first, as the kernel runs a synchronous signal's handler, then dies by the signal with its action set to SIG_DFL by
  rt_sigaction (Go's own handler would print a Go traceback instead: probed). A real fault in Go code stays Go's panic,
  pyi's SystemError (guardedStep): there is no C to fault.
- `dump_traceback_later(timeout, repeat, file, exit)` and `cancel_dump_traceback_later`: a watchdog goroutine, the
  `Timeout (0:00:00.500000)!` header, `_exit(1)` with exit. `register(signum, file, all_threads, chain)` and
  `unregister`: a user signal dumps the traceback; chain calls the previous handler (the Python handler of `signal`
  when one is set). `dump_c_stack`. `-X faulthandler` and `PYTHONFAULTHANDLER` enable it at start (bin/pyi python passes
  `-X` options to the runtime, `sys._xoptions`).
- What cannot match: thread ids are pyi's (goroutines have no pthread id; the dump prints the thread's ident as CPython
  prints `pthread_self`), `Extension modules:` never prints (pyi has no extension modules but its built-in ones, which
  CPython does not list), `_fatal_error_c_thread` and the `_testcapi` cases (`test_fatal_error`, `test_sigbus`,
  `test_sigill`) are `_testcapi`'s, and the GIL-disabled cases do not apply.

**_lsprof** (1,158 lines). Since 3.12 it is a `sys.monitoring` tool (PROFILER_ID 2, the name "cProfile"): enable
registers its callbacks for PY_START, PY_RESUME, PY_THROW, PY_RETURN, PY_YIELD, PY_UNWIND, CALL, C_RETURN and C_RAISE;
the tests read `sys.monitoring.get_tool(2)` and expect the second profiler's "tool 2 is already in use". So pyi gets
`sys.monitoring`'s call-family events for real, global events only (`set_events`, `register_callback`, `use_tool_id`,
`free_tool_id`, `clear_tool_id`, `get_events`; the callbacks run with the thread's tracing flag set, so they raise no
events themselves); `sys.setprofile` (and `threading.setprofile_all_threads`) is the same events' legacy tool
(`PY_MONITORING_SYS_PROFILE_ID` 6, as CPython's legacy_tracing.c), `call`/`return`/`c_call`/`c_return`/`c_exception`
with the frame. The local events, LINE, INSTRUCTION, JUMP, BRANCH and settrace are not this task's (219 owns
settrace's frame hooks). The hooks:
- a frame's start and end: runFrames' entry and its pushed frame (PY_START, or PY_RESUME for a resumed generator,
  PY_THROW for a throw into it), its return (PY_RETURN, PY_YIELD when suspended) and its unwinding (PY_UNWIND). Every
  Python frame passes there: the loop's, a compiled function's (216's register loop runs its callees by
  `runDirect` -> `runFrames`), a generator's resumption, exec's and a class body's.
- a call's CALL and, for a callable that is not a Python function, C_RETURN or C_RAISE after it: callStack, call-ex,
  `AotCallMethod`, `aotInvoke`, `AotCallV` and `AotCallMethodV`, the call sites of the loop and of compiled code.
- no cost when off: one load of a global word (the events of all tools, 0 when none) and a branch at each site; the
  monitored path is a separate function. Measured by `bin/pyi bench`'s sample against the base (210's method).
- What cannot match: CALL's `instruction_offset` is pyi's (2 x the IR's instruction, as `f_lasti`), timings are Go's
  monotonic clock, `DISABLE` returned by a callback disables nothing (CPython disables the event at that location: a
  local event, not built), and a C function is a `*Builtin` (builtin_function_or_method; a method descriptor called
  unbound is bound to its first argument as `_lsprof`'s `get_cfunc_from_callable`): pyi's built-in implementations of
  what CPython writes in Python would show as built-ins, and the reverse (none known in the profiled samples).

**termios** (1,399 lines). tcgetattr, tcsetattr, tcsendbreak, tcdrain, tcflush, tcflow, tcgetwinsize, tcsetwinsize
over the tty ioctls as musl's functions make them (TCGETS, TCSETS+when, TCSBRK, TCFLSH, TCXONC, TIOCGWINSZ/TIOCSWINSZ),
the struct's bytes by their address as mmap's msync passes one (reflect's Pointer: no unsafe; the `:pointer-syscall`
class's premise, "Go forms take no pointer", has this way round it); the 216 constants of musl's headers on Linux
(asm-generic's: amd64 and arm64 alike). test_termios and test_tty need `os.openpty` (absent: `:pointer-syscall`), so it
comes with termios (/dev/ptmx, TIOCSPTLCK, TIOCGPTN, as musl's openpty). Not on tamago (no tty).

**_tracemalloc** (1,717 lines with Python/tracemalloc.c). CPython hooks its three allocator domains: every
PyMem/PyObject malloc, realloc and free records (size, traceback of the Python frames) per pointer, and
`get_object_traceback` finds an object's trace by its address. pyi allocates through Go: no allocator to hook, no
frees (the collector reclaims without telling). An "allocation" on pyi is decided here as the creation of a Python object
that CPython's allocator would see, at the runtime's constructors, with CPython's sizes (`sys.getsizeof`'s, the object's
header and its variable part as 64-bit CPython lays them out); a free is the object's collection, seen by a cleanup
(`runtime.AddCleanup`) when the collector runs, and `get_traced_memory` runs the collector first so a dropped object's
size is gone as CPython's refcount frees it. Which numbers can match: the traces' tracebacks (the frames' filenames and
lines), the count of traces per line for objects made by Python code, `get_object_traceback`; the sizes of bytes, str,
tuple, list (CPython's over-allocation pattern), dict and int as `sys.getsizeof` gives them. What cannot: CPython's
internal allocations (frames, code, the interpreter's own buffers, free lists' reuse, the small-int cache), so a
snapshot's totals and `get_tracemalloc_memory` differ, and a trace never comes from inside a built-in's work on its own
buffers. Its port is last in the brief's order; how far it got is in "As built: 220".

## As built: 220, faulthandler, _lsprof, termios and _tracemalloc (2026-11-05/06, the Mac VM, queue 220)

Decided (2026-11-06, the user): 220's DECISION (a), _tracemalloc's second stage (allocations traced at the runtime's
constructors with CPython's sizes, frees seen by the collector, CPython's internal allocations out by design).

Built as "Design: 220" above, in `go/arbace/py` (`builtins_faulthandler.ae`, `builtins_lsprof.ae`, `builtins_termios.ae`,
`builtins_tracemalloc.ae`, `monitoring.ae`; the PSF License Agreement in LICENSE.md), with these hooks elsewhere:
- **the loops** (`eval.ae`, `aot.ae`, `thread_gil.ae`, their own commits): runFrames' start and pushed frame inside
  the eval breaker's check (`breakMonitor` set while a tool has events: no new load), its return and unwinding, and
  the call sites callStack, call-ex, `AotCallMethod`, `aotInvoke`, `AotCallV`, `AotCallMethodV`, each
  `if monEvents != 0`;
  `AotCallV`/`AotCallMethodV` leave a call that goes the stack's way (`aotStacked`) to callStack's event, so a call
  fires CALL once. Monitored, a call of anything but a Python function goes through `Call` (callInit's in-loop
  `__init__` is not taken then: its frame runs nested, PY_START inside).
- **signals** (`builtins_signal.ae`): `ownSignalHook` (os.kill of the own pid and signal.raise_signal: faulthandler's
  handlers run in the sending thread, and a signal whose Python disposition is SIG_DFL and whose default ends the process
  ends it, by SIG_DFL through rt_sigaction: Go's runtime ignored SIGUSR1 and printed its own traceback for SIGABRT), and
  `signalTakenHook` (faulthandler.register without chain keeps the Python handler from running, as its sigaction
  replaces it).
- **threads** (`builtins_thread.ae`): `allThreads`, the started threads with their idents under a lock of their own
  (faulthandler's dumps read it from goroutines that do not hold the GIL).
- **the start** (`interp.ae`, `sys.ae`, bin/pyi): `atStartHooks` (faulthandler's enable for `-X faulthandler` and
  `PYTHONFAULTHANDLER`, tracemalloc's start for `-X tracemalloc[=N]` and `PYTHONTRACEMALLOC`, as CPython's configuration
  checks them); bin/pyi python passes `-X` options in `PYI_XOPTIONS` (`sys._xoptions`) and `-E`/`-I` as
  `PYI_IGNORE_ENV`, both read once and removed from the environment; `-c`'s code runs as `"<string>"` (no prefix line:
  the runtime sets `sys.path[0]` to `""`, no `__file__`), so `-c`'s tracebacks, line numbers and faulthandler's dumps are
  CPython's.
- **the constructors** (`bytes.ae`, `containers.ae`, `dict.ae`, `frame.ae`): `if tmOn { tmAlloc(o) }` in NewBytes,
  NewList, NewDict and newFrame's cells (_tracemalloc's second stage).
- **posix**: `os.openpty` (termios' file, through `posixAddons`) and `isatty` as musl's (TIOCGWINSZ: a pty's master is a
  tty, the device numbers missed it).

What each module gives (run: the programs below against `python3 -I -B` and the lib-test sample):
- **faulthandler**: the four entry points, the fatal handler for SIGSEGV, SIGFPE, SIGABRT, SIGBUS and SIGILL (sent by
  another process: os/signal; raised by pyi: in the raising thread), dump_traceback_later's watchdog (repeat, exit,
  file, cancel), register/unregister with chain, the `_sigsegv`/`_read_null`/`_sigabrt`/`_sigfpe`/`_stack_overflow`/
  `_fatal_error_c_thread` helpers, `Extension modules:` (the built-in modules not in `sys.stdlib_module_names`), the
  message texts and argument errors. By design: a thread's ident stands for its pthread id and no thread name is
  printed (the tests' regexes allow both); the C stack is musl's "<cannot get C stack on this system>" (CPython on
  Alpine prints the same); a real fault in Go code is Go's panic (pyi's SystemError), not a signal; `  Garbage-collecting`
  never prints and test_gc's `__del__` does not run at gc.collect() (`:finalization-collect`, 218's).
- **_lsprof** and **sys.monitoring**: test_cprofile's and test_profile's profilee outputs (print_stats, print_callers,
  print_callees) equal CPython's to the digit under both profilers (the external timer's simulated ticks make every
  number exact), the calling conventions, the tool's registry, the second profiler's error, the external timer's
  unraisables, throw's entry, sys.setprofile's event sequence. By design: CALL's offset is pyi's, DISABLE disables
  nothing, no local events, the default timer is Go's monotonic clock.
- **termios**: every function, the 216 constants, musl 1.2.6's speeds in CIBAUD (`cfsetispeed`/`cfgetispeed`), the
  errors of clinic's converters. Not on tamago (no tty there).
- **_tracemalloc** (both stages; the second the user's (a) on this task's DECISION, 2026-11-06): start, stop,
  is_tracing, get_traceback_limit, clear_traces, reset_peak, get_traced_memory, get_tracemalloc_memory, `_get_traces`,
  `_get_object_traceback`, the start from `PYTHONTRACEMALLOC` and `-X tracemalloc[=N]` with CPython's errors. An
  allocation is the creation of a Python object at the runtime's constructors (`NewBytes`, `NewStr`, `NewTuple`,
  `NewList`, `NewDict`, a frame's cells: each `if tmOn { tmAlloc(o) }`), its size `sys.getsizeof`'s (a dict 64, a
  cell 40), its traceback the running thread's frames (a callee's cells are its caller's: the callee is not on the
  chain yet, CPython's "no incomplete frames"); tracebacks interned; a free is the object's collection, seen by a
  finalizer (`runtime.SetFinalizer`, which goi runs as Go's own; `runtime.AddCleanup`'s generic body goi does not
  interpret), and get_traced_memory, `_get_traces` and reset_peak run the collector and wait for the finalizers first
  (a sentinel's, queued last). By design: CPython's internal allocations (frames, code, the interpreter's buffers, free
  lists, the small-int cache, a list's or dict's growth by realloc) and a built-in's own Go buffers are none of pyi's
  traces, so totals and counts differ; str and tuple are not traced (the hook put `NewStr` and `NewTuple` past Go's
  inlining budget, measured by `-gcflags=-m`: their cost with tracing off decided it); an empty list made right after one was dropped has a trace (CPython's comes
  from its free list); instances are not traced (218's finalizer queue owns their finalization: one finalizer per Go
  object). TestTracemallocEnabled: 8 of the 10 methods `:tracemalloc-no-traces` held now pass; the other 2
  (test_set_traceback_limit, test_snapshot) fail under the lib-test driver in CPython itself (`bin/pyi lib-test-all
  --oracle test_tracemalloc`: 8 != 10, 6 not greater than 10, as pyi), `:tracemalloc-driver-depth`.

Samples (run, the VM):

| module | before (base) | after |
|---|---|---|
| test_termios | 2 equal, 18 classed (`:pointer-syscall`), 1 failing | 21 equal |
| test_tty | 4 classed | 4 equal |
| test_profile | 7 classed (`:c-modules-deferred`) | 7 equal |
| test_cprofile | 27 classed | 25 equal, 2 classed (`:multiprocessing`: spawn and forkserver children need `_multiprocessing`) |
| test_faulthandler | 48 classed | 43 equal, 5 classed (testcapi 4, `:finalization-collect` 1) |
| test_tracemalloc | 51 classed | 38 equal, 12 classed (testcapi 9, fork 1, `:tracemalloc-driver-depth` 2) |

The programs of `/root/arbace-mac/.tmp/q220/t` against `python3 -I -B`: `prof/prof1.py` (110 lines: the profilee's
three pstats tables under cProfile and profile, the calling conventions, the tool's registry, throw, the external
timer's unraisables, the documented cumulative example, sys.setprofile's events) equal under `bin/pyi run` and compiled
(`bin/pyi aot`); `fh.sh` (15 faulthandler cases through `bin/pyi python`) equal; `termios1.py` equal; `tm1.py` equal;
`tm2.py` (the traced-memory cases and the documented top-lines example) equal compiled, but for the empty list's trace;
under `bin/pyi run` its module-level drop of the 1 MiB bytes is not seen as freed (something of goi's interpretation
still holds it: test_get_traced_memory, the same drop inside a test method, passes under goi).

**Speed with profiling and tracing off** (run, the VM, load 4.4-4.8 with two other agents): `bin/pyi bench` of seven
benchmarks, 3 rounds, base (40ab1e13, `/root/arbace-mac/.tmp/worktrees/q220-base`) and branch back to back, the least
of 3 (milliseconds; `/root/arbace-mac/.tmp/q220/bench/all.tsv`, every output equal to CPython's):

| benchmark | CPython | base compiled | after compiled | base loop | after loop |
|---|---:|---:|---:|---:|---:|
| bm_coroutines | 360 | 642 | 617 (-3.9%) | 769 | 767 (-0.3%) |
| bm_deltablue | 130 | 386 | 389 (+0.8%) | 589 | 609 (+3.4%) |
| bm_float | 219 | 447 | 469 (+4.9%) | 639 | 647 (+1.3%) |
| bm_generators | 159 | 677 | 678 (+0.1%) | 681 | 681 (+0.0%) |
| bm_nbody | 303 | 353 | 356 (+0.8%) | 589 | 581 (-1.4%) |
| bm_richards | 321 | 504 | 507 (+0.6%) | 1108 | 1138 (+2.7%) |
| bm_spectral_norm | 335 | 614 | 662 (+7.8%) | 821 | 851 (+3.7%) |
| sum | | 3623 | 3678 (+1.5%) | 5196 | 5274 (+1.5%) |

The two outliers were taken apart on the compiled benchmarks themselves: the same program's binaries, built from the
base's runtime and from the branch's (and from variants), run interleaved 8-10 times each
(`/root/arbace-mac/.tmp/q220/var/run.sh`). The least times: bm_float base 438-440, after 441-444, after without
tracemalloc's hooks 445; bm_spectral_norm base 592-596, after 600-601, after with the frame start on the eval breaker
596, that without the call sites' hooks 592. The spread of one binary is 3-8%, wider than any difference between them:
the table's +4.9% and +7.8% were the load. Two changes came from these runs. First, a frame's PY_START check moved
under the eval breaker (`breakMonitor` is set while any tool has events), so the frame start costs nothing more than
the base's. Second, `NewStr` and `NewTuple` are not hooked, so both stay inlined. `NewList`, `NewDict` and `NewBytes` stay
inlined but grow, so a few of their callers no longer inline (`go build -gcflags=-m`: `AotBuildList`, `instanceDict`,
`currentContext`, `(*Exception).dict` and six more); their cost is within the noise above.

**The closing checks (run, the VM):**
- `make python-essential`: check --aot 21 programs, 20 pass, 1 classed, 0 failing; the controls fail as they must.
- `bin/pyi runtime check`: the packages print and read back to the same forms; the tables and runtime-ops are equal;
  the REPL session is equal; 136 of the 138 programs are equal.
  - e-executable.py: found `-c`'s `sys.path[0]` not `""` (fixed by 78c9ebd8). It also ran past the check's 120 s cap:
    on the loaded VM it takes 133 s, its six children starting in 14 s each, and the base's start is the same 14 s.
  - f-signal.py: the check ran as a background job, which inherits SIGINT ignored. CPython then reports
    `getsignal(SIGINT)` 1, pyi does not read the inherited disposition (as on the base).
  - Run alone in the foreground, both programs equal CPython's. With `PYI_SMOKE_ONLY='^(e-executable|f-signal)'`,
    f-signal passes and e-executable equals but for the cap.

## Design: 219t, tracing: `sys.settrace`, `f_trace`'s events, `f_lineno` jumps (2026-11-06, the Mac VM, queue 219t; the user's (a) on 219's DECISION)

Read from CPython 3.14.10's Python/legacy_tracing.c (`sys_trace_*`, `call_trace_func`, `trace_line`,
`sys_trace_jump_func`), Python/instrumentation.c (`initialize_lines`, `_Py_call_instrumentation_line`),
Python/sysmodule.c's `trace_trampoline` and Objects/frameobject.c (`mark_stacks`, `marklines`,
`frame_lineno_set_impl`), against go/arbace/py at 50c6882d with 220's `sys.monitoring` (075e56ae) merged in.

**One mechanism with 220's profiler.** Since 3.12 CPython's `sys.settrace` is a `sys.monitoring` tool of its own
(PY_MONITORING_SYS_TRACE_ID, 7), as `sys.setprofile` is tool 6. 220 built the registry, the call-family events at
runFrames' entry, return, pushed frame and unwinding, and the profiler as tool 6's native callback. 219t makes
`sys.settrace` tool 7 the same way (a native callback, `traceEvent`, in a new file `trace.go`): PY_START, PY_RESUME and
PY_THROW are `call`, PY_RETURN and PY_YIELD `return` with the value, PY_UNWIND `return` with None, RAISE `exception`
with `(type, value, traceback)`; and adds what neither had: the RAISE event (fired where runFrames adds a frame's
traceback entry, i.e. at the raise and in each frame the exception passes through, not at a re-raise: CPython's
`monitor_raise` at its `error:` label), and the local events LINE (and the legacy JUMP and INSTRUCTION, `opcode`) from
the dispatch loop. The trampoline is sysmodule.c's: `call` goes to the thread's trace function, the others to the
frame's `f_trace`; a result other than None becomes `f_trace`; an error removes the trace function and the frame's
`f_trace` and is raised where the event is. Callbacks run with 220's `monTracing` count up, so no event fires inside
one. A tool registered with `sys.monitoring.set_events(..., LINE)` gets LINE from the same loop hook (global events
only, as 220's registry; local events stay 220's not-built).

**Line events by CPython's rule over pyi's IR.** pyi's IR is CPython's codegen's shape (the same operations, jumps and
handlers, one instruction per operation, 216's per-expression lines). An instruction is *instrumented* when (a) its line
differs from the previous instruction's in code order and is known, (b) it is a jump's target (jump, jump-if-*,
for-iter's exit) with a line, or (c) a handler's target (not end-async-for) with a line; `initialize_lines`, computed
once per code object. Before an instrumented instruction runs, `line` fires unless the previous instruction this frame
ran (f.Lasti) has the same line, a previous RESUME forcing it: the frame's start, and the resumption after a
yield-value. A backward `jump` to an instruction of its own line fires `line` too (`sys_trace_jump_func`). f_trace_lines
false: no line events; f_trace_opcodes: `opcode` before each instruction.

**RESUME.** CPython's code objects start with RESUME at `co_firstlineno` (the `call` event's `f_lineno`, which
test_sys_settrace's `lineno_matches_lasti` checks against `co_lines` at `f_lasti`). pyi's had none: a function's first
instruction is its body's. So `arbace.python.compile` starts every code object but a module's with a `nop` at
`co_firstlineno` (its line only; the next instruction's line as before), never instrumented, the "previous RESUME" of
the first line event. The loop runs one more instruction per call; the compiled code none (a nop emits nothing). Every
function's IR changes (one instruction, the offsets one later: `f_lasti`, `tb_lasti` move as CPython's do, which have
RESUME too); the IR cache is keyed by the compiler's digest.

**Traced frames run on the loop.** The compiled code cannot report lines. While a trace function is set on any thread
or a tool has LINE events (`monEvents & monTraceMask`, one load of 220's word):
- runFrames' guardedStep (and step) enters a code object's `Native` only when tracing is off: a frame started or
  resumed while tracing runs on the loop;
- the loop checks the word once per instruction (`if monEvents&monTraceMask != 0 { traceInstr }`, taken only when on);
- a compiled frame already running hands itself to the loop at its next block start (216's rule 2: every slot is in
  `f.Stack` there): the register generator emits `if py.AotTracing() { f.PC = n; return nil, py.AotToLoop }` after each
  block start's label, and after a direct call's value (216d: the slots then in Go locals written back first, f.PC the
  call's next instruction), so `pdb.set_trace()` returning into compiled code reports the next line; guardedStep runs
  the frame on the loop from there. The stack generator (the fallback) checks at each instruction's label. Once on the
  loop a frame stays there until it returns; a later call of the code is compiled again. Every PC the loop leaves a
  frame at (a call's next instruction, a handler's target, a yield's resumption) is an entry of the compiled function
  (216's and 216d's re-entries), so a frame resumed compiled after tracing ends is entered correctly.
- Cost when off: one load and branch per block start and per direct call in compiled code, per instruction on the
  loop; measured with 210's method (CPython, base, branch back to back, 3 rounds, the least).

**`f_lineno` jumps** (frame_lineno_set_impl): allowed only during a trace function's event, by the event (line, PY_YIELD,
PY_RESUME and JUMP allow it; `call`'s "can't jump from the 'call' trace event of a new frame"; return, unwind, throw,
exception, opcode "can only jump from a 'line' trace event"; none "f_lineno can only be set in a trace function"); the
line's first instruction not before it (`marklines`, "line N comes before/after the current code block"); `mark_stacks`
over pyi's IR, each stack slot's kind (Iterator after get-iter and get-aiter, Except after push-exc-info and at a
handler's entry, Lasti for a handler that pushes it, Object otherwise), every operation's stack effect as
arbace.python.ir's table; the target the compatible one with the deepest stack, else CPython's messages ("can't jump
into the body of a for loop", "can't jump into an 'except' block as there's no exception", ...); unbound locals bound
to None after the RuntimeWarning "assigning None to N unbound local(s)"; the slots above popped, an Except slot
restoring the handled exception; f.PC set to the target, which then runs without a line event of its own.

**What cannot match by construction:** pyi's offsets are its instructions' (one code unit each), so `f_lasti` values and
jumps' "offsets" in opcode events differ from CPython's numbers; where pyi's compiler places a line differently from
CPython's flowgraph (NOPs kept for a line, the lines of a loop's exit and of handlers' preambles), the events differ,
each a compiler line fix found by test_sys_settrace's expected sequences. Opcode events are pyi's instructions, not
CPython's.

**The proof asked:** test_sys_settrace (the event sequences, the jump tests, raising trace functions), test_bdb, a
sample of test_pdb, test_trace, and probes of coverage-style tracing (line sets of a program's functions) against
CPython, on the loop and with the program compiled (the handoff); no regression on `bin/pyi runtime check`,
`make python-essential` and the check scripts of 210, 216, 216d and 219; the bench sample's table.

## As built: 219t, tracing: `sys.settrace`, `f_trace`'s events, `f_lineno` jumps (2026-11-06, the Mac VM, queue 219t)

Built as "Design: 219t" above, on 50c6882d with 220's `sys.monitoring` (075e56ae: its registry, call-family events and
loop hooks) merged in (bf8d0a0b), so that the tracer and the profiler are one mechanism:

- **`trace.go`** (go/arbace/py): `sys.settrace` is sys.monitoring's tool 7 with a native callback, as tool 6 is
  220's profiler: PY_START/PY_RESUME/PY_THROW `call`, PY_RETURN/PY_YIELD `return` with the value, PY_UNWIND `return`
  None, RAISE and STOP_ITERATION `exception` with `(type, value, traceback)`; sysmodule.c's trampoline (`call` to the
  thread's function, the rest to `f_trace`, a result not None the new `f_trace`, an error removing both); the thread's
  event while its trace function runs (`what_event`) for `f_lineno`'s setter; `sys.gettrace`, `threading.settrace`,
  `settrace_all_threads`. RAISE fires where runFrames adds a frame's traceback entry (each frame the exception passes
  through, not a re-raise); STOP_ITERATION where a yield-from's delegate ends, RAISE for the StopIteration a for-iter's
  Python `__next__` raises and for a generator that a for-iter runs to a value other than None (bytecodes.c's FOR_ITER,
  SEND, INSTRUMENTED_END_SEND, gen_iternext).
- **Line events from the loop** (`traceInstr`, before each instruction while `monEvents & monTraceMask`): CPython's
  `initialize_lines` over pyi's IR (a line start in code order, a jump's target, a handler's target; never RESUME or
  end-async-for), an instrumented instruction reporting `line` unless the frame's previous instruction had its line
  (RESUME's exception: the frame's start, a resumption after yield-value), a backward jump to its own line `line` too
  (`sys_trace_jump_func`), `opcode` per instruction with `f_trace_opcodes`; a tool's global LINE, INSTRUCTION and JUMP
  events from the same hook (coverage's sysmon core in its simplest form).
- **`f_lineno` jumps** (`frameSetLineno`): frame_lineno_set_impl's event rules and messages, `marklines`, `mark_stacks`
  over pyi's IR (`irEffect`: arbace.python.ir's stack effects in Go; the kinds Iterator, Except, Lasti, Object),
  `compatible_stack`, the deepest compatible target, the unbound locals' RuntimeWarning and None, the popped slots (an
  Except one restoring the handled exception).
- **Traced frames on the loop**: guardedStep and step enter `Code.Native` only while tracing is off; a compiled body
  returns `py.AotToLoop` at a block start (and after a direct call's value, its slots written back) when
  `py.AotTracing()`, guardedStep continuing the frame on the loop (arbace.python.aot: `to-loop` at each block start's label
  in the register generator, at each instruction in the stack generator).
- **One code object per code** (`Code.Object`): `frame.f_code is func.__code__` (pyi made a new wrapper per access; bdb
  and the tracers compare code objects by identity); 220's `monCode` the same (a REQUEST commit for 220).
- **The compiler's locations** (arbace.python.compile): a RESUME nop at `co_firstlineno` starting every code object but a
  module's; codegen.c's NO_LOCATION instructions and line-holding NOPs (pass, try, break, continue, return, a for
  target, a constant statement), and `flow!`, a port of flowgraph.c's line passes over a unit's instructions in basic
  blocks: small and line-less exit blocks inlined, unreachable code removed, line-less exits duplicated per jump and
  lines propagated (resolve_line_numbers, twice), constant tests folded, tuples of constants folded, jumps to jumps
  threaded, const-pop pairs and redundant NOPs and jumps removed, a backward conditional jump made a forward one over a
  backward jump (normalize_jumps_in_block); push_cold_blocks_to_end is not done (pyi's handlers stay inline). With it a
  target's store at the target's line, a decorated definition's name at its statement's line, a class body's
  `co_firstlineno` its first decorator's, docstrings at their line and a function's no statement of its body, the
  except clause's instructions at its line and a named clause's name stored as codegen_try_except stores it (the
  exception popped: the handler's stack is CPython's, which mark_stacks' jumps need), match cases at their pattern's
  line, async for's end at its iterator's line. Every function's IR changes (the IR cache is keyed by the compiler's
  digest: the night re-records).

**The proofs (run, the VM; CPython 3.14.10 `python3 -I -B` the reference; the test modules by `t/ut.py`, unittest's
loader, one line per test, run by a compiled runner on the loop, `t/pr.sh`):**
- test_sys_settrace: 441 of 448 equal (base: `sys.settrace` a no-op; with the tracer but the compiler's old lines, 254).
  The 7 left: `test_13_genexp` in its four classes, `test_jump_from_yield` and `test_reentrancy` need a generator's
  finalization or `__del__` (the trace events of a generator closed when collected: 218's finalizer queue), and
  `test_trace_lots_of_globals` (a display of more than 16 items: pyi builds it in one instruction, CPython item by item;
  the narrowed `:tracing` class).
- test_bdb: 36 of 37 (`test_skip`: pyi's `io` is `_pyio`, so the import's frames include `_pyio`'s, which the test's skip
  list does not name). test_trace: 18 of 27, the 9 left outside tracing (7 run `sys.executable`, empty under the runner;
  2 need `gc.get_referrers`, which pyi's collector does not answer). test_pdb's 72 doctests, each run alone as its
  load_tests runs them (`t/pdb3.py`): 54 equal (CPython 71); the rest pdb's other machinery on pyi: `dis` of pyi's code
  (`break FUNC`, find_first_executable_line), pdb's closures through `co_consts[0]`, a string statement in mode single
  taken for a docstring, the interact command. test_patma's TestTracing and test_threading's `gettrace_all_threads` and
  `getprofile_all_threads` equal (out of `:tracing`).
- `t/check219t.sh` (tr1: call, line, return and exception events through calls, loops, handlers and a generator; tr2:
  tracing switched on inside running frames as pdb's set_trace does, the compiled frames' handoff; cov1: line coverage
  by settrace of a module of branches, handlers, with, a class, a comprehension, a generator, a match, a lambda and
  multi-line displays, and by a sys.monitoring LINE tool; ts1: except* with else and finally; ci: `f_code is __code__`;
  th1: threading.settrace and settrace_all_threads): every output equal to CPython's, compiled and on the loop; on the
  base every one fails.
- 219's check219.sh and check219tb.sh, 216d's check216d.sh, 210's check210.sh: PASS. `make python-essential`: 20 pass, 1
  classed, 0 failing. `bin/pyi check --aot --no-pyi` on 210's 73-program sample: 73 pass. `bin/pyi runtime vet`: ok. `bin/pyi runtime check`: the same forms for all packages, runtime-ops equal, 137 of 138 programs equal, the REPL equal; the one FAIL, e-executable.py, is status 124, the 120 s cap that 216 and 219 also hit (its output is cut at the cap).

**The bench (run, the VM):** `bin/pyi bench`'s 20-benchmark sample, 210's method: each round CPython, base (bf8d0a0b:
219's head with 220's monitoring hooks merged, its own worktree) and branch back to back, 3 rounds, the least wall
seconds; load 1.5-3; every output equal to CPython's.

| benchmark | CPython | base | after | base / CPython | after / CPython |
|---|---:|---:|---:|---:|---:|
| raytrace | 0.20 | 0.55 | 0.55 | 2.8x | 2.8x |
| deltablue | 0.12 | 0.37 | 0.38 | 3.1x | 3.2x |
| fannkuch | 0.28 | 0.58 | 0.57 | 2.1x | 2.0x |
| generators | 0.15 | 0.65 | 0.65 | 4.3x | 4.3x |
| nqueens | 0.25 | 0.60 | 0.60 | 2.4x | 2.4x |
| go | 0.09 | 0.22 | 0.21 | 2.4x | 2.3x |
| richards | 0.30 | 0.48 | 0.48 | 1.6x | 1.6x |
| chaos | 0.07 | 0.19 | 0.19 | 2.7x | 2.7x |
| json_dumps | 0.03 | 0.19 | 0.19 | 6.3x | 6.3x |
| spectral_norm | 0.32 | 0.57 | 0.58 | 1.8x | 1.8x |
| unpack_sequence | 0.22 | 0.58 | 0.58 | 2.6x | 2.6x |
| coroutines | 0.35 | 0.59 | 0.59 | 1.7x | 1.7x |
| float | 0.21 | 0.43 | 0.43 | 2.0x | 2.0x |
| nbody | 0.29 | 0.33 | 0.33 | 1.1x | 1.1x |
| hexiom | 0.03 | 0.08 | 0.08 | 2.7x | 2.7x |
| base64 | 1.49 | 3.39 | 2.66 | 2.3x | 1.8x |
| bpe_tokeniser | 3.06 | 5.48 | 5.49 | 1.8x | 1.8x |
| async_tree | 0.22 | 0.96 | 0.96 | 4.4x | 4.4x |
| mdp | 0.75 | 2.54 | 2.51 | 3.4x | 3.3x |
| pprint | 1.35 | 4.16 | 4.26 | 3.1x | 3.2x |

Sum: CPython 9.78 s, base 22.94 s (2.35x), after 22.29 s (2.28x). Every difference is within 5% and goes both ways but
base64's, 19% faster (5 rounds again: base 3.30, after 2.67; its cause not established: the flow passes change its
loops' code, constant tuples folded, jumps threaded, NOPs gone); deltablue and pprint 2-5% slower in their least of 3
(5 rounds: deltablue 0.36 and 0.38, pprint 4.13 and 4.11). The handoff's check, one load and branch per block start and
per direct call, costs nothing measurable at this resolution.

**What stays by design:** pyi's offsets are its instructions' (`f_lasti`, an opcode event's instruction); the layout's
one difference left, handlers inline where CPython moves them to the end (push_cold_blocks_to_end), shows in no test
of the sample; sys.monitoring's local events (`set_local_events`) and DISABLE stay 220's not-built; the tests that need a
generator's finalization or `__del__` wait for 218.
## Design: 222, finalization at reference counting's moment, and cycles through finalizers (PEP 442) (2026-11-06, the Mac VM, queue 222)

Read from CPython 3.14.10's Python/gc.c (`gc_collect_main`: `deduce_unreachable`, `handle_weakrefs`, `finalize_garbage`,
`handle_resurrected_objects`, `delete_garbage`), Objects/typeobject.c (`subtype_dealloc`, `slot_tp_finalize`) and
Lib/test's test_finalization, test_gc, test_weakref and support's `check_free_after_iterating`, against
go/arbace/py at 160750c2 (218's finalizer queue, `finalize.go`, with 219, 219t and 220 merged).

**What the tests observe.** Two families, apart:
- *Cycles* (`:finalization-cycle`, 33): `del` the last outside reference to a cycle that holds an object with a `__del__`
  or a weak reference, `gc.collect()`, then look: `__del__` called once (PEP 442), the weak references cleared (also a
  survivor's: test_finalization's "XXX is this desirable?"), a resurrected object kept and never finalized again,
  `gc.garbage` empty (or every unreachable object in it under DEBUG_SAVEALL), and some tests `gc.collect()`'s count.
  Go never frees a cycle that reaches a finalized object from itself, so today such a cycle lives for good.
- *The moment* (`:finalization`, 47): the test looks right after the last reference went, with no collection between
  (`del o; self.assertIsNone(r())`, a key deleted in `__eq__`, an iterator rebound to None, a frame's return).

**The cycles: CPython's algorithm with Go's collector as the oracle of outside references.** CPython's
`deduce_unreachable` subtracts the references inside the generation from every object's count: what keeps a positive
count is held from outside. pyi has no counts, and the references from outside are Go's (stacks, registers of the
compiled loop, goi's frames, the runtime's own variables): only Go's collector sees them all. So `gc.collect()` asks it:
1. *The graph.* From the tracked objects (218's records) of the node types (an instance, its dictionary and `__slots__`
   folded into it while the dictionary has not been handed out as an object; a list; a tuple; a dictionary), the
   nodes reachable through their slots of interface type, and the edges between nodes. Only the strongly connected
   components that hold a tracked node and have a cycle matter (an acyclic dead object is Go's to find, 218), with
   what they reach.
2. *The probe.* Every edge between two probed nodes is hidden (its slot set to nil, its target kept as an address), the
   node's Go finalizer (218's) replaced by a probe's, `runtime.GC()` with the GIL held (no Python code may see a hidden
   slot), and a wait until every probe fired or Go's finalizers have been still for a moment. A node whose probe did
   not fire is held from outside the probed set (by any Go reference, or through an object the graph does not
   traverse); the slots are put back, and everything reachable from such a node through the edges is reachable:
   the rest is cyclic trash, CPython's `unreachable`. Sound by construction: a slot is hidden only between two
   probed nodes, so a node that fires has no reference from outside but the hidden ones, and the propagation puts
   those back; a type the graph does not traverse only keeps more alive (a class, a function, a cell, a frame:
   their references are typed fields, never hidden, so a cycle through them is not found: it stays classed).
3. *PEP 442's order* (gc_collect_main): the weak references to the trash cleared, and their callbacks called (a weak
   reference is not a node: its callback is always called, CPython's "outside the trash" case); `__del__` (218's work:
   a generator's close, a socket's warning too) on each object not finalized before, once (the record's work cleared);
   then, when any Python code ran, the probe again over the trash alone (`handle_resurrected_objects`): what a
   finalizer stored somewhere is held from outside now and survives, never finalized again; the weak references made
   meanwhile cleared without callbacks; DEBUG_SAVEALL puts the final trash in `gc.garbage`, else the records are dropped
   and the Go finalizers not set again, so Go frees the cycle (CPython's `tp_clear` breaks cycles for reference
   counting, which Go does not need: unobservable). `gc.collect()` returns the number of trash nodes (CPython counts
   its trash's objects; an instance's dictionary, folded, is not one, as 3.14's inline values).
4. *Cost.* Nothing when no tracked node is in a cycle; else the graph's walk over what the tracked objects reach, and one
   Go collection with the probes (two when a finalizer ran). Untracked cycles (no `__del__`, no weak reference) stay
   Go's: their count is not CPython's (test_gc's test_list and the like keep a class).
- *Go's limits respected*: one finalizer per object (an object that may carry another file's: _tracemalloc's lists and
  dicts once it traced, another package's types, is not a node); Go's heap does not move (the hidden addresses).

**The moment.** Exact reference counting would need a count on every holder of every tracked object: every store
into a list, a dict, an attribute, a cell, a register of the compiled loop, a goi register, and every Go variable of
the runtime that holds an object; one missed increment frees a live object (a weak reference cleared under a live
referent, `__del__` on a live object), which no test could excuse. The cheapest exact model instead asks Go at the
moments a test can observe, and keeps reference counting's order (218's queue):
- *`del NAME` of a tracked object* (`DELETE_FAST`, on the loop and compiled: `AotDeleteFast`): the deletion's site checks
  whether the value was tracked with work or weak references due; if so, one Go collection there and the queue drained
  before the next instruction (the statement leaves nothing on the value stack): exact when that was the last reference,
  a wasted collection when not. No cost on any other path; the collections are bounded (a budget of a tenth of the
  elapsed wall time; past it the site leaves the death to the next collection, 218's moment).
- *Rebinding and a frame's return* (`o = None`, a function's locals at its return, an iterator dropped) would need the
  same check on every `STORE_FAST` and every return: a load and a branch on the compiled loop's hottest paths. Measured
  with 210's method; past 3% it is the user's DECISION, not built.
## As built: 222, cycles through finalizers and the moment at del (2026-11-06, the Mac VM, queue 222)

What "Design: 222" proposed, built in `go/arbace/py/finalize.go` (with `dict.go`'s `escaped`, `builtins_gc.go`, and the two
DELETE_FAST sites, `eval.go` and `aot.go`'s `AotDeleteFast`); light samples only.

- **The cycle detector** (`cyCollect`, run by `gc.collect()` after 218's rounds): the graph from the tracked objects
  over instances (the dictionary folded in while `escaped` is false, `__slots__`), lists, tuples and dictionaries (lists
  and dictionaries not once tracemalloc traced: its finalizers), Tarjan's components with a cycle and a tracked node and
  what they reach (at most 2^18 nodes), the probe (`cyProbe`: edges between probed nodes hidden, a probe's Go finalizer on
  each, 218's own taken off and put back, `runtime.GC()` under the GIL, a wait until all ran or Go's finalizers are still
  for 3 ms), the reachable propagated from the unfired; then CPython 3.14.10's order: weak references to the trash
  cleared, their callbacks called; `__del__` (and 218's other work) once; when Python code ran, the probe again over the
  trash alone (resurrection); weak references made meanwhile cleared; DEBUG_SAVEALL's `gc.garbage`, else the records
  dropped and no Go finalizer set again (Go frees the cycle); 218's rounds again for what the trash held.
  `gc.collect()` returns the trash's node count; `gc.get_debug`/`set_debug` keep the flags.
- **Escapes** (the soundness assumption, `Dict.escaped`): an instance's dictionary handed out as an object (`__dict__`'s
  getter and setter, `object.__getstate__`, `_thread._local`'s dictionaries) is no longer folded into its instance:
  a cycle through it is not found (by design: the instance's typed field would keep it marked).
- **The moment at del** (`finDeleted`, `finVerify`): `DELETE_FAST` of a tracked object with work or weak references due
  collects at once (rounds while its record moves: `__del__`, then the weak references), the queue drained before the
  next instruction; the collections' time bounded to 50 ms plus a tenth of the elapsed wall time.
- **goi's registers** (found by the goi sample): the first build passed the deleted value to the check, and goi's frames
  (the eval loop's case-local, the callee's parameters: queue 218's REQUEST c9047d52, unbuilt) kept it past the next
  `gc_collect()`: test_weakref's test_equality and both test_hashing failed on goi. `finDeletedAt(f, i)` now reads and
  clears the slot itself, takes the type by reflection inline and drops the value before anything runs (7700bbf8).
  Under goi the cycle detector finds nothing yet (the walk's values stay in goi's registers, as 218's collections lag):
  the methods it serves are `:finalization-goi`'s, decided (a) by the user for 218.

**The classes, before and after** (methods named): `:finalization-cycle` 33 to 17 (test_finalization's 11 cycle tests,
test_gc's test_bug1055820b, test_resurrection_is_transitive and test_resurrection_only_happens_once_per_object,
test_weakref's test_weak_keyed_len_cycles and test_weak_valued_len_cycles found; left: classes, functions, a weak
reference in the trash, the count of untracked cyclic garbage, `gc.get_stats`' shape); `:finalization` 47 to 45
(test_weakref's two union_operators tests, served at `del`); `:finalization-goi` 41 to 59 (the 18, passing compiled).

**The samples** (the VM):
- `bin/pyi lib-test --aot test_finalization test_gc test_weakref`: 214 methods, 148 equal, 66 classed, 0 failing
  (218's head: 130 equal, 84 classed).
- `bin/pyi lib-test-all --fresh test_finalization test_gc test_weakref` (goi): 214 methods, 122 equal, 92 classed,
  0 failing (218's: 121, 93, 0); test_weakref took 1,134 s on the loaded VM (218's time was not recorded).
- check222.sh (`.tmp/q222/t/cyc.py` compiled against CPython: self-cycle, resurrection once, a chain through a list and a
  tuple with callbacks, a live cycle kept, DEBUG_SAVEALL, the moment at del, an escaped `__dict__`): equal but t7b (the
  escaped dictionary's cycle, by design); it fails at 160750c2 (t1 to t6 differ).
- `bin/pyi runtime check`: the packages printed and read again the same, tables and runtime-ops equal, the REPL equal,
  136 of 138 programs equal: e-executable.py (status 124, the 120 s cap under the VM's load, as at 218) and f-signal.py
  (run from a background job, whose SIGINT bash sets to SIG_IGN before CPython's reference ran: rerun in the foreground,
  CPython and compiled pyi equal). `make python-essential`: check --aot 21 programs, 20 pass, 1 classed, 0 failing, the
  control refused. 219t's check219t.sh, 219's check219.sh, 216d's check216d.sh and 210's check210.sh: PASS.

**The cost** (210's method: wall milliseconds, least of 3 rounds, each round base 160750c2 and the branch back to back, CPython's the least of all its runs; `bin/pyi bench` over ten benchmarks of 216d's table, every output equal to CPython's). The model as built adds nothing to the compiled loop (the check sits only in DELETE_FAST):

| benchmark | CPython | base | branch | branch / base |
|---|---:|---:|---:|---:|
| coroutines | 353 | 694 | 697 | 1.004 |
| generators | 152 | 657 | 653 | 0.994 |
| chaos | 77 | 199 | 198 | 0.995 |
| deltablue | 123 | 376 | 375 | 0.997 |
| float | 212 | 438 | 437 | 0.998 |
| nqueens | 250 | 596 | 594 | 0.997 |
| raytrace | 201 | 542 | 549 | 1.013 |
| richards | 309 | 493 | 488 | 0.990 |
| spectral_norm | 324 | 627 | 591 | 0.943 |
| unpack_sequence | 223 | 594 | 598 | 1.007 |
| sum | 2224 | 5216 | 5180 | 0.993 |

The wider model, measured as an experiment and not committed (`.tmp/q222/exp_patch.py`: an instance flagged at its tracking, checked on every `STORE_FAST`, `AotStoreFast`, `AotStoreFastV`, and over the fast locals at every frame's release, each found one collected as at `del`), against base again in 3 rounds:

| benchmark | CPython | base | exp | exp / base |
|---|---:|---:|---:|---:|
| coroutines | 353 | 688 | 892 | 1.297 |
| generators | 152 | 655 | 881 | 1.345 |
| chaos | 77 | 201 | 283 | 1.408 |
| deltablue | 123 | 377 | 485 | 1.286 |
| float | 212 | 439 | 528 | 1.203 |
| nqueens | 250 | 599 | 702 | 1.172 |
| raytrace | 201 | 543 | 652 | 1.201 |
| richards | 310 | 490 | 599 | 1.222 |
| spectral_norm | 324 | 630 | 711 | 1.129 |
| unpack_sequence | 223 | 597 | 495 | 0.829 |
| sum | 2225 | 5219 | 6228 | 1.193 |

+19.3% on the sum, past the brief's 3%: not built; DECISION 1 below. Its cost is the check itself (no benchmark has a
tracked instance to collect at a store), a type assertion on every store and a scan with a deferred call at every
frame's release; a variant gated by one global (no tracked instance alive: no check) is not measured.

**DECISION 1** (queue 222, the user's): the moment at rebinding and at a frame's return.
- (a) keep the moment at `del` alone, as built (recommended): 0% on the compiled loop; `:finalization` keeps the 45
  methods that rebind, return, delete an item or drop an iterator.
- (b) a follow-up task measures the gated variant (one global load per store and per frame's release while no tracked
  instance lives; the check when one does): adopt it if it stays under 3% on the sample, else (a).
- (c) adopt the wider model as measured: +19.3% on the bench sample, some of `:finalization`'s 45 methods.

**Not served, by design** (with their class): a cycle through a class, a function, a cell, a frame or a generator (typed
Go fields, never hidden), or through an instance whose `__dict__` was handed out; a weak reference inside the trash
(callbacks always called); the count of untracked cyclic garbage; `gc.get_stats`, `get_objects`, `is_tracked` and the
other collector observables (`:gc-collector`).

## Design: 223, the exception state, traceback objects and the error display (2026-11-06, the Mac VM, queue 223)

Read from CPython 3.14.10's Python/codegen.c (the locations each instruction is emitted at: `LOC(e)`,
`update_start_location_to_match_attr`, `maybe_optimize_method_call`, `codegen_augassign`), Python/flowgraph.c
(`propagate_line_numbers`, `duplicate_exits_without_lineno`: a location copied whole), Python/pythonrun.c (`_PyErr_PrintEx`,
`_PyErr_Display`), Python/traceback.c (`tb_new`, `tb_next`'s setter, `tb_lineno_get`, `tb_dir`), Python/ceval.c
(`do_raise`), Objects/exceptions.c (`AttributeError_getstate`, `AttributeError_reduce`) and Lib/traceback.py
(`_get_code_position`, `_should_show_carets`, `_print_exception_bltin`), against go/arbace/py at cd5fd6cf (222's head).

**The error display is traceback.py's.** CPython's `PyErr_Display` first calls `traceback._print_exception_bltin(exc)`
and only falls back to its C printer when that import or call fails. pyi's display (interp.go's `printChain`) is a Go
stand-in that prints no caret lines, no exception groups' boxes and no colour. So, as CPython: `PrintException` calls
`traceback._print_exception_bltin` first (not while it is already displaying: a second error takes the Go display),
and the Go display stays as the fallback. And `PyErr_Print`'s part, which pyi skipped: the uncaught exception of the
main module and of the REPL stored as `sys.last_exc` (`last_type`, `last_value`, `last_traceback`) and given to
`sys.excepthook`, a hook that raises reported as "Error in sys.excepthook:" then "Original exception was:".

**PEP 657's positions.** traceback.py draws the carets from `co_positions()` at `tb_lasti // 2`. pyi's compiler keeps
only a line per instruction. The unit gets, beside its `:line`, the `:loc` of the node being compiled (`[LINE COL
END-LINE END-COL]`, the forms' `:pos`, UTF-8 byte offsets as the ast's): `expr!` and `stmt!` set both and restore both;
an instruction emitted at a line without a node (`set-line!`, the many sites that set only a line) has no columns, as
`[l l None None]`. The places CPython moves a location are ported: an attribute's load, store and delete, and a method
call's CALL (maybe_optimize_method_call's conditions, `is_import_originated` from the module's symbol table), at the
attribute name's line and column (`update_start_location_to_match_attr`, which also changes the instruction's line),
and an augmented assignment's instructions at its target's location. `flow!` (219t's line passes) carries the location
with the line wherever it copies a line from one instruction to another (propagation, the duplicated exits, NOPs
replacing an instruction, threaded jumps). The IR gets `:positions`, `[END-LINE COL END-COL]` or nil per instruction
(absent in mode :lines); load.go reads it into `Code.Positions`; `co_positions()` gives `(line, end_line, col,
end_col)`, and `(line, line, None, None)` under `-X no_debug_ranges` or `PYTHONNODEBUGRANGES` (config.code_debug_ranges).
pyi has no CACHE entries, so its `co_positions()` lists one item per instruction, the same location as CPython's
instruction at the same position in the source.

**Traceback objects** (`:traceback-objects`): `types.TracebackType(tb_next, tb_frame, tb_lasti, tb_lineno)` (tb_new's
checks and messages), `tb_next` writable (None or a traceback, "traceback loop detected"), `tb_lasti` and `tb_lineno` as
given (tb_lineno -1: the frame's line at tb_lasti), `__dir__`.

**raise** (`do_raise`): the cause a class called (its result must be an instance: "calling X should have returned an
instance of BaseException, not Y"), an instance, or None, else "exception causes must derive from BaseException";
the exception class's call checked the same way.

**`:exc-pickle`**: AttributeError's `__reduce__` (`AttributeError_getstate`): its state a copy of the instance dict
with `name` (when set) and `args`.

**`:exc-state`**: the context and the generator's saved exception are measured on the sample first; each failing
method's cause is fixed in ceval's terms where it is a runtime path (the context chained in `_PyErr_SetObject`'s
places), else left classed with its cause.

**The cost.** The compiled loop is not touched: positions are data the loop never reads; the display imports traceback
only when an exception is displayed. The bench sample is still run (210's method) because the IR's code objects grow.

## As built: 223, the exception state, traceback objects and the error display (2026-11-06, the Mac VM, queue 223)

As designed ("Design: 223"), with what the samples found on the way. Base cd5fd6cf.

**The display.** `PrintException` is `_PyErr_Display`: `traceback._print_exception_bltin(exc)` first, the Go printer
only when that import or call fails (or while it runs). `PrintUncaught` is `PyErr_Print` for the main module and the
REPL: `sys.last_exc` and the three others, then `sys.excepthook`; a raising hook gets "Error in sys.excepthook:" and
"Original exception was:". The builtin `sys.excepthook` puts the given traceback on an exception that has none and
prints C's line for a non-exception value. The main module's SyntaxError is no longer the reader's error form on stderr:
`bin/pyi run` hands the compiler's answer to `RunModule`, which raises it as `compile()` does (the parser's `_metadata`,
a file's source None, a decoding error naming the file as the tokenizer does) and gives it to `sys.excepthook`, so the
keyword typos ("Did you mean 'and'?") come from traceback as in CPython. `-c`'s source is registered with
`linecache._register_code` (run_mod), and so are the REPL's statements (the Go display read them from RegisterSource). A SyntaxError's file line is decoded by its coding cookie, the BOM skipped
(err_programtext).

**PEP 657's positions**: the unit's `:loc` beside `:line`, `flow!` carrying it, `co_positions()`, `-X no_debug_ranges`,
as designed; but the IR's `:positions` is one string (`COL,END-COL[,END-LINE-LESS-LINE]` per instruction, empty for
none, `;` between), kept as text by load.go and decoded on the first `Code.Position` (a traceback, `co_positions`): the
vector of numbers per instruction first built lengthened every program's IR parse (the bench sample's process times). The locations codegen moves
are ported: attribute load, store and delete and a method call's CALL (`update_start_location_to_match_attr`,
`maybe_optimize_method_call` with `is_import_originated`), augmented assignment at its target, assert's raise at its
test, RESUME at `(line, line, 0, 0)` (a traceback at `tb_lasti` 0 prints its empty caret line), an except handler's
instructions at the handler's span (a bare `raise` in `except*` shows both lines), a comprehension's GET_ITER and
FOR_ITER at its iterable. The caret lines, chains, groups' boxes and notes then come from traceback.py unchanged.

**Objects.** `types.TracebackType(...)`, `tb_next`'s setter with the loop check, `tb_lasti`/`tb_lineno` as given,
`__dir__`; `sys.exc_info()`'s traceback; `do_raise`'s cause and class checks; `_PyEval_CheckExceptTypeValid` (a tuple's
items checked before matching); AttributeError's pickled state; exception groups: ExceptionGroup's two bases set at
start (a subclass made before the first group had no Exception in its MRO: `except Exception` missed it), `excs_str`,
read-only `message`/`exceptions`, split's recursion guard (test_deep_split ends with RecursionError instead of a 300 s
timeout and Go's stack overflow). `sys.stdlib_module_names` (stdlib_module_names.h), `module.__dir__`, the shadowing
hints of module_getattro and import_from, `sys.is_remote_debug_enabled()` False.

**The exception state.** `_PyErr_ChainStackItem`: an exception thrown into a generator's frame, directly or after its
delegate's throw raised, takes the generator's handled exception as its context. Found by test_asyncio's
`test_exception_chaining_after_await`: the task's exception is thrown into the awaiting coroutine by `Task.__wakeup`.

**The classes** (methods named in `:tests`; the patterns kept where noted):

| class | before | after |
|---|---:|---:|
| `:exc-state` | 1 (+4 patterns) | 0 (patterns kept: the 9 methods they found on the VM pass) |
| `:traceback-objects` | 3 (+2 patterns) | 0 (patterns kept) |
| `:error-display` | 11 | 6 (the -P and -m starts of test_import's five; GBK/cp437 sources) |
| `:exc-pickle` | 1 | removed |
| `:code-positions` | 4 | 2 |

Also test_sys's import opened (`sys.is_remote_debug_enabled`): its 102 methods were all `:remote-debugging`; now 38
equal, the rest `:cpython-only`, `:remote-debugging` (the skips) and the new `:sys-module` (23: sys's own surface,
outside this work, each for its owner).

**Samples** (goi; before at cd5fd6cf, after at the head):
- `bin/pyi lib-test-all --fresh test_traceback test_exception_group test_raise test_sys`: 569 methods, before 217 equal,
  318 classed, 34 failing; after 282 equal, 287 classed, 0 failing.
- `bin/pyi lib-test test_exceptions`: 109 methods, before 83 equal, 26 classed; after 90 equal, 19 classed, 0 failing.
- `bin/pyi check --fresh --only` over the methods the patterns found on the VM (test_asyncio's test_timeouts, test_waitfor,
  test_tasks: 9, all pass) and the named methods of other modules (test_threading's 3 pass, test_fstring's passes,
  test_setcomps' passes after the comprehension's locations; test_import's five as classed above). The display imports traceback when an exception is displayed (about 4.5 s on
goi per uncaught exception, CPython does the same), `-c` imports linecache (about 1.3 s on goi).

## Design: 224, generators' and coroutines' edge cases, and `_asyncio` (2026-11-07, the Mac VM, queue 224)

The classes `:generators`, `:coroutines` and `:asyncio-c`, base 7df8864b. Judged by test_generators, test_coroutines,
test_asyncgen and test_asyncio's packages, sampled; before any change (run, `bin/pyi lib-test --aot`): 244 methods, 208
equal, 28 classed, 8 failing (test_asyncgen's, in no class).

**`_asyncio` as a port, not a stand-in.** One file, `builtins_asyncio.go`, follows Modules/_asynciomodule.c function by
function: the same names (`future_schedule_callbacks` as `futureScheduleCallbacks`), the same order of checks, the same
messages. What C does by its object model is replaced as follows.

- **The object.** Every Future and Task, exact or a subclass's, is an `*Instance` with `value` a `*futObj` (FutureObj_HEAD
  and TaskObj's fields in one struct, no pointer back: Go never frees a cycle through a finalized object). `*Instance` gives
  `__dict__`, weak references, `__del__` and `__slots__` as a class's instances have them. `classInfos` declares the two
  types with a dict. The built-in slots must see the instance (a future yields itself, a task hands itself to callbacks),
  so `fixupSlots` leaves them unwrapped for these layouts (`selfSlots`, as `reprOfInstance` does for repr alone).
- **tp_finalize.** `__del__` is a built-in method (CPython's slot wrapper over tp_finalize), and tracking uses queue 218's
  `workDel`, so a subclass's `__del__` replaces it as slot_tp_finalize does. A Future is tracked when `fut_log_tb` is set,
  that is when an exception nobody asked for is stored; a Task when it is still pending at a flush of the pending list.
  That list is `pendingGens`' rule: a task finished before the flush pays no Go finalizer.
- **The task registry.** C's linked list holds borrowed references. Here a recent task is in a short strong list (removed by
  index when it finishes), and at a flush a still pending one is marked in its `finTrack` record, which holds no reference.
  `all_tasks` reads both under `finMu`. The per-thread lists exist in C for free threading; pyi has a GIL, so one registry.
- **The thread state.** `asyncio_running_loop` and `asyncio_running_task` are two fields of `Thread`. `current_task(loop)` for
  another thread's loop walks the thread list, as C's stop-the-world path does.
- **Contexts.** `PyContext_CopyCurrent` runs per task and per callback; C's HAMT makes it O(1). `pyContext` gets
  copy-on-write of its dictionary, and the current context moves from the interpreter to the `Thread` (context.c keeps it in
  the thread state; asyncio's executors make the difference visible).
- **PyIter_Send.** `genSendTo` learns FutureIter's am_send, so an `await` of a pending or finished future allocates no
  StopIteration; `GetAwaitable` takes exact futures without a method lookup.
- **Module state.** The module's exec imports what `module_init` imports, through `builtinModulesErr`, into one state.
  The types are made once, C's per module; a second import of `_asyncio` renews the state (a Low if a test notices).

**asyncio.graph** needs no port: Lib/asyncio/graph.py reads `_asyncio_awaited_by`, `cr_await`, `cr_frame`, `ag_await`,
`ag_frame`, `gi_yieldfrom`, `f_generator`. Those are the C getters and generators' attributes. The remote-debugging offsets
(`_AsyncioDebug`) have no meaning without a C layout and are left out, as `_remote_debugging` is absent.

**Generators and coroutines**, by the failing methods, porting Objects/genobject.c rather than patching messages:
`__name__` and `__qualname__` settable with their checks (`gen_set_name`), `throw`'s three-argument DeprecationWarning and
`_gen_throw`'s normalization (an exception instance's traceback, "throw() third argument must be a traceback", the check
before the frame state), `gi_suspended` and `cr_suspended` as FRAME_SUSPENDED states, `close()` clearing the frame and
`gen_close`'s cases, `async_gen_asend`/`athrow`'s close, throw and unawaited warnings (`async_gen_asend_close`,
`async_gen_athrow_close`, the "coroutine method ... was never awaited" RuntimeWarning), and the never-awaited coroutine's
warning through `warnings._warn_unawaited_coroutine`, as `_PyErr_WarnUnawaitedCoroutine` does.

What stays out, as the user decided: generators resumed by nested runs (216d) are unchanged.

## As built: 224, generators' and coroutines' edge cases, and `_asyncio` (2026-11-07, the Mac VM, queue 224)

As designed ("Design: 224"), with these changes the samples forced.

**`_asyncio`** (`go/arbace/py/builtins_asyncio.ae`) follows Modules/_asynciomodule.c function by function. The state is not
`Instance.value` as designed: `unwrap` hands that value for the object at every built-in method call, so a future's own
methods received the state. It is the instance's last slot, `value` stays nil, and no `selfSlots` was needed. C's oddity
that `_asyncio.Future` takes attributes but has no `__dict__` attribute is not copied (a Low). `asyncio.run`, TaskGroup,
timeouts, eager tasks and `asyncio.graph` run on it; the remote-debugging offsets stay out (`:remote-debugging`).

**Contexts.** Copy-on-write and the current context per `Thread`, as designed.

**Generators** (`gen.go`, `coro.go`). `throwArgs` is `_gen_throw`. The delegate is now served outside the frame's run, the
frame linked into the stack, so an error in throw's arguments leaves the generator suspended and a delegate's StopIteration
comes back through CLEANUP_THROW's rule in `opYieldFrom`. `resume`'s third argument is gen_send_ex2's `closing`. `gi_*`, `cr_*`,
`ag_*`, `__name__` and `__qualname__` are getsets (in `dir()`, read-only errors as C's). A throw into a generator not started
puts its frame in the traceback. The asend and athrow awaitables are ported whole, with `close` and "coroutine method ...
was never awaited" (`workAgen`, under `pendingGens`' rule). `async for` now calls the async generator's hooks
(`async_gen_anext`): before it did not, so `shutdown_asyncgens` never saw those generators. `anext(it, default)` keeps a
generator's return value, and PyIter_Send uses `send` on an object with no `__next__`.

**Found on the way: no suspended generator could be finalized.** `Frame.gen` pointed back at the generator, and Go never
finalizes an object that reaches itself, so queue 218's `workGen` ran only for generators whose frame had gone. Tracking
(`Generator.track`) now turns that pointer into an address in the frame's views, read through `tracked`. This is what made
the never-awaited warnings and the async generators' finalizer hooks fire after `gc.collect()`.

**The cycle graph** (`finalize.go`) gained suspended generators, FutureIter and `task_wakeup` as nodes, so gc.collect finds a
pending task's cycle (task, coroutine, frame, FutureIter, future, wakeup, task), and "Task was destroyed but it is pending!"
is logged. A generator with an escaped frame stays out: what else holds that frame is not in the graph. Debug mode
escapes it (`extract_stack` in the coroutine), so test_log_destroyed_pending_task stays classed; a frame's cells stay out for
the same reason. `cyNode.slots` holds addresses: a pointer into FutureIter kept FutureIter alive, so its probe never ran.

**The moment at `del`** (the user's (a) on 222) now covers DELETE_NAME and DELETE_GLOBAL too (`finPeek`, `finDeleted`), and a
generator still in the pending list is flushed first.

**Smaller.** `<genexpr>`, `<listcomp>`, `<setcomp>` and `<dictcomp>` as code names (`<generator-exp>` before); an exhausted
generator's frame has no `f_back`; an unraisable exception with no traceback gets the current frame's (format_unraisable_v).
A generator's rare fields moved to `genExt`: 32 more bytes had crossed a size class and cost bm_coroutines 7%.

**Classes** (`:tests` names): `:generators` 15 to gone, `:asyncio-c` 7 to gone, `:coroutines` 17 to 2 (the frame keeps the
coroutine; its one regex stays until a whole run shows it serves nothing). The rest went to the classes that say why:
`:finalization` 46 to 59 (10 `refcycles` methods read `gc.get_referrers`, which answers nothing here), `:finalization-goi` 59
to 68 (pass compiled), `:finalization-collect` 29 to 30, `:finalization-cycle` 17 to 18 (a cycle through a frame's globals),
`:docstrings` 1 to 2.

**Speed.**
Least compiled wall time in ms of 3 rounds, base and head back to back (210's method; q225's run shared the VM, so both sides carry its load):

| benchmark | CPython | base | head | change |
|---|---:|---:|---:|---:|
| bm_async_tree | 231 | 1303 | 746 | -42.7% |
| bm_async_tree_io | 546 | 2331 | 1380 | -40.8% |
| bm_async_tree_memoization | 256 | 1485 | 813 | -45.3% |
| bm_async_tree_eager | 111 | 782 | 405 | -48.2% |
| bm_async_tree_tg | 230 | 1191 | 549 | -53.9% |
| bm_async_generators | 316 | 923 | 1374 | +48.9% |
| bm_coroutines | 368 | 769 | 729 | -5.2% |
| bm_generators | 159 | 682 | 676 | -0.9% |

bm_async_generators pays for the firstiter hook, which the base skipped in `async for` (a bug): a WeakSet add per generator, as CPython does.
## Design: 225 (typing's runtime, PEP 695's and PEP 649's scopes)

The classes `:type-params`, `:module-annotations` and `:typing-runtime`, judged by test_type_params, test_annotationlib,
test_typing and test_type_aliases, CPython 3.14.10's own algorithms ported rather than stand-ins:

- The objects: `Objects/typevarobject.c`, `genericaliasobject.c` and `unionobject.c` ported whole into typing.go: TypeVar,
  ParamSpec, TypeVarTuple with their constructors' checks, the managed dict that holds `__module__` (a type parameter of
  PEP 695 has none: its `__module__` is the type's), `evaluate_*` (the function, else a `_ConstEvaluator`), `__reduce__` and
  `__mro_entries__`; ParamSpecArgs and ParamSpecKwargs as native types; TypeAliasType's constructor; `_Py_make_parameters`
  and `_Py_subs_parameters` for GenericAlias and Union, `ga_reduce`, `ga_getattro`'s lists, the union builder.
- The compiler: `codegen_type_param_bound_or_default` and `codegen_typealias_body` (the evaluators' `.format`, position-only,
  default VALUE, the prologue), `codegen_finish_annotations_scope` (an `__annotate__`'s parameter named `format`),
  `dictbytype(FREE, DEF_FREE_CLASS)` for every block, a class's type-parameter scope taking the class's private name,
  `compiler_set_qualname` whole (the name declared global, annotation scopes looked through), and the inlined comprehension
  in a class body under its own scopes (`_PyCompile_TweakInlinedComprehensionScopes`' `in_class_block`); a name free outside
  and a cell inside an inlined comprehension stays free.
- The surroundings the tests show: `__class_getitem__` of the built-in classes as real class methods, a built-in type's
  class method a `builtin_function_or_method`, `isinstance`'s and `issubclass`'s union, `__set_name__`'s note, the number
  slots in a built-in type's `__dict__` (typing's Protocols read them).

No trade-off needs the user so far: each item has CPython's answer.

## As built: 225, typing's runtime, PEP 695's and PEP 649's scopes (2026-11-07, the Mac VM, queue 225)

`lib-test-all --fresh` before (7df8864b) and after, per module; test_type_params (the language core's family) by `lib-test --aot`:

| module | before | after |
|---|---|---|
| test_type_params | 94 ok of 109 by the driver, 15 not | 109 equal |
| test_type_aliases | does not import, 31 failing | 31 equal |
| test_typing | does not import, 718 failing | 714 equal, 4 classed (cpython-only 3, finalization-goi 1) |
| test_annotationlib | 112 equal, 3 classed, 3 failing | 116 equal, 2 classed (cpython-only 1, c-accelerators 1) |

The classes: `:type-params` to none (10 tests named in it), `:typing-runtime` to none, `:module-annotations` to
none (test_pkg's 8 equal): all three retired. Two methods moved to standing classes: test_type_repr to `:c-accelerators`
(pyi's datetime is `_pydatetime`, its `isoformat` a bound method), test_args_kwargs_weakrefs to `:finalization-goi` (equal
compiled, `del`'s moment under goi).

As the design said, plus what the runs showed:

- Compiler: `compiler_set_qualname` whole gave `global`-declared classes their plain name (pickle of the tests' local classes);
  the comprehensions' code had the names `<generator-exp>`, `<list-comp>`: now `<genexpr>`, `<listcomp>`. Under the future
  import a string in an annotation is its repr (`pp/*string-repr*`, ast_unparse), the rest of arbace.repr.python.print's text
  unchanged. Function annotations evaluate in codegen_annotations_in_scope's order, args before posonly.
- A function's `__annotate__.__qualname__` is `f.__annotate__` while its code's co_qualname is `__annotate__`: as observed on
  CPython 3.14.10; the source of the rename was not found in funcobject.c or codegen.c, so `annotateOf` applies it on read.
- Runtime beyond typing: the number protocol and comparisons listed in `ownSlots` (`'__abs__' in float.__dict__`), `__abs__`'s
  slot wrapper, `__round__`, `__complex__`, `__bytes__`; a bound built-in's repr, equality and hash (meth_repr, meth_richcompare,
  meth_hash); mappingproxy's keys, values and items the mapping's views, no longer lists; `iter()` of a class or a union refused
  (a type's GetItem slot is the subscription, no sq_item); `type.__subclasscheck__`'s check_class; pickling a code object a
  TypeError.
- `__class_getitem__` of the rest of test_genericalias's generic types (mappingproxy, generator, coroutine, async_generator,
  ReferenceType, Template, Interpolation, Token, itertools.chain, DirEntry, whose old method returned the class).
- Known gap: `sorted(float.__dict__)` still lists only the dict's entries: `ownSlots` answers `in` and `[]`, not iteration.
  tp_name in "not callable" and setattr's messages stays the bare name (`'TypeVar' object is not callable`).


## Design: 227, pickling the core types, the iterators and the exceptions (2026-11-07, the Mac VM, queue 227)

The classes `:pickle-core`, `:pickle-iterators` and `:array-pickle` (`:exc-pickle` went with 223), judged by test_pickle (the
pure Pickler; the `_pickle` twin stays, PY6g's DECISION 1), test_itertools, test_array and test_exceptions, then test_iter,
test_enumerate, test_range, test_set, test_deque and test_bytes for the iterators. Base c44a0b5d. The rules are the pinned
3.14.10's C; a 1,088-line probe of `__reduce_ex__` at protocols 0, 1, 2 and 4, `pickle` and `copy` over about 150 objects, against
`python3 -I -B`, finds the gaps before the tests do.

**On 226.** 226 (running beside this task) ported `object_getstate_default` with its required check, `reduce_newobj`'s
conditions and the reduces of set, method, deque subclasses, bytearray and the str iterator. This task does not port them a
second time: 226's head is merged here first, by the printed-Go method (only `func.go` conflicts: two fields of `Builtin`).

**The iterators.** One new file, `iterreduce.go`, holds what `Objects/*.c` spreads out: `enum_reduce`, `map_reduce`,
`map_setstate`, `filter_reduce`, `zip_reduce`, `zip_setstate`, `iter_reduce`, `iter_setstate`, `calliter_reduce`, the
`striter`, `bytearrayiter`, `unicodeiter`, `rangeiter`, `longrangeiter` and `listreviter` pairs, `setiter_reduce`,
`dequeiter_reduce` and `dequeiter_new`. Each reads the builtin (`_PyEval_GetBuiltin`, by the dict's own lookup) before it reads the
iterator (issue #101765: a key's `__eq__` may exhaust it). What the iterators' structs must keep for that: bytes' and bytearray's
iterators their object, `filter` that its function was `bool`, a deque iterator its deque after the end and the C's counter,
and an exhausted str or bytes iterator its nil sequence.

**Types pyi lacks.** `list_reverseiterator` (`list.__reversed__`), `str_iterator` beside `str_ascii_iterator`,
`longrange_iterator` and `range.__reversed__` (`range_reverse`): the reduces name them, and an unpickled iterator's type shows
them. itertools' `__reduce__` went in 3.14: its types refuse (`cannot pickle 'itertools.count' object`), which the required
check gives without a line of its own, and test_itertools' pickling tests are the reference's as they stand.

**Out of the design.** `OrderedDict` is pyi's pure-Python twin, so `iter(od)` is a generator that does not pickle where
`odict_iterator` does: classed with the twin, not ported.

**The cost.** The compiled loop is not touched. `strIter` and `rangeIter` get a type by field or by test, and `Next` of a str
iterator one nil test; the bench sample checks the iteration programs.
## Design: 226, the core data model's gaps (2026-11-07, the Mac VM, queue 226)

The classes `:data-model`, `:module-subclasses`, `:function-constructor`, `:str-protocol` and `:surrogate-pairs`, judged by
test_descr, test_class, test_module, test_funcattrs, test_str and the surrogate cases of test_codecs and test_codeccallbacks.
Base 7df8864b. Objects/typeobject.c, descrobject.c, funcobject.c, classobject.c and moduleobject.c of the pinned 3.14.10 are
the rules; what follows is what changes in pyi's model.

**Instances of a built-in's subclass stay an `Instance` around the built-in's value.** classmethod, staticmethod and super
subclasses panic because their slots assert the Go type. Their slots take `unwrap`, and `unwrapSlots` covers `DescrGet`,
`DescrSet` and `Call`'s owners, rather than a `cls` field per Go type (the slots, `__dict__` and `__class__` of `Instance`
come for free).

**A class's layout is recorded where typeobject.c records it.** `classInfo` gets `hasWeakref`, and `type_new`'s slot rules are
ported whole (`type_new_slots`, `type_new_slots_bases`, `type_new_copy_slots`): `may_add_dict`, `may_add_weak` (a base with
tp_itemsize: int, bytes, tuple, type's own table, gives no `__weakref__`), `__slots__` names checked as identifiers, a mangled
name's conflict, `__qualname__` and `__classcell__` excepted. `__dict__` and `__weakref__` become getsets in the dict of the
class that adds them (`type_add_getset`), not one `object.__dict__`: `object` has none, and `hasattr(object(), '__dict__')` is
false. The built-in types that have a dict get their own. `weakref.ref` asks the same `hasWeakref` (218's Low).

**`__class__` assignment is object_set_class** with `compatible_for_assignment`, `compatible_with_tp_base` and
`same_slots_added`, over `Instance`, a module subclass's `Module` (its `Cls`) and an exception subclass; module subclasses get
`slots` as instances do. `module.__new__` makes an uninitialized module (no `__name__` in its dict yet), `module.__init__`
sets it, as moduleobject.c splits them.

**The MRO is mro_internal.** `type.mro()` computes C3 afresh; a metaclass's `mro` is called and checked (`mro_check`);
`__bases__` assignment is type_set_bases with `mro_hierarchy`'s undo list, each class's old MRO kept until all succeed. Every
MRO set moves `typeEpoch`, so report 210's caches stay correct by the rule they already keep; nothing on the hot paths changes.

**Special methods on built-ins.** `recordOwnSlots` covers every slot (number protocol, comparison, descriptors), so
`t.__dict__` lists what `add_operators` puts there, by the same lazy `ownSlot`. No wrapper is built at start.

**function and module attributes.** `function`'s `__dict__`, `__closure__`, `__globals__`, `__builtins__`, `__code__`
(free variable count), and `__qualname__` keep funcobject.c's rules: the fast path in `FunctionType.Slots.GetAttr` and
`SetAttr` stays for the common names and the read-only ones raise as `PyMemberDef`'s READONLY does.

**Surrogates.** A Java string cannot tell `"𐀀"` from `"\U00010000"`. `decode-str` splits its parts where an
escape makes a high surrogate meet a low one, through the `{:joined ...}` constant that `\N{...}` already uses; `join-strs`
does the same for adjacent literals; `portable-consts` carries a part with a lone surrogate as `{:codepoints ...}`, and the
loader's joined constant writes parts by WTF-8 bytes, so the runes stay two. `ast.parse`'s constant stays a Low (its JSON).

**str.** The str subclass results of `repr`, `ascii`, `str`, and `format`/`%`'s edge cases are unicodeobject.c's rules.

**test_class** imports `_testinternalcapi` for `has_inline_values` at module level. Providing the module makes 33 other test
files stop skipping, so the choice is made after measuring (a module with that one function; the tests that need more import
it and find AttributeError). Inline values themselves are CPython's layout and stay classed.

**Speed.** `lookupAttr`, the attribute sites and the compiled loop stay as they are; the class-creation path and `__class__`
assignment change. Instance creation keeps its fields. If `objectNew` or `genericGetAttrFound` change, 210's bench sample
is rerun.

## As built: 226, the core data model's gaps (2026-11-07, the Mac VM, queue 226)

As designed ("Design: 226"), with what the samples found. Base 7df8864b.

**Classes.** `type_new_slots` is `newSlots` (identifiers, `may_add_dict`, `may_add_weak`, a mangled name's conflict with
`__qualname__`, `__classcell__` and `__classdictcell__` excepted, the names sorted, a secondary base's `__dict__` and
`__weakref__`). `__dict__` and `__weakref__` are getsets in the class that adds them, by `PyDict_SetDefault`'s rule (a class's
own `__dict__ = property(...)` stays: test_builtin's test_vars caught the first version). `object` has no `__dict__`; `type`,
`module`, `function`, `BaseException` show their own. `subtype_dict` goes to a built-in base's dict (`class M(Base, type)`).
A dict subclass's instance assigned to `__dict__` keeps its identity through `Dict.self`. `__classcell__` is set before the MRO
(`type_new_set_attrs` comes before `PyType_Ready`), so a metaclass's `mro()` can call a method using `__class__`.

**`__class__` and `__bases__`** are `object_set_class`, `compatible_for_assignment` (`hasGC` for tp_free, `compatibleWithBase`,
`sameSlotsAdded`, `managedLayout` for the preheader and inline-values flags), `type_set_bases` with `mro_hierarchy`'s undo list.
`mroInternal` calls a metaclass's `mro()`, checks it, and lets a reentrant inner call win; `type.mro()` computes C3 afresh;
`__mro__` is None inside the first `mro()`; a class a custom `mro()` left out of its own MRO is refused at `super(type, type)`,
as CPython's is. Every MRO set moves `typeEpoch`, so 210's caches keep their rule.

**Descriptors.** A classmethod, staticmethod or super subclass's instance stays an `Instance`; their slots unwrap.
`functools_wraps` at `__init__`, `__annotations__` and `__annotate__` by `descriptor_get_wrapped_attribute`. `AddMember` makes
`member_descriptor`s (`types.MemberDescriptorType` is one again): function's `__globals__`, `__closure__`, `__builtins__`,
method's, property's, super's, complex's; read-only ones say "readonly attribute". A data descriptor without `__get__` no longer
hides the instance's dict. `hackcheck` is applied for type alone: of the built-in bases reachable here only type overrides
tp_setattro in CPython (module's Go `SetAttr` is pyi's own: importlib's `_LazyModule` caught a wider first version). A bound
method is not bound again. Wrapper descriptors check their owner at binding and through `callBound` (bpo-37619).

**Built-in methods** learn their type (`Builtin.Owner`): `__qualname__`, `__objclass__`, method_vectorcall's self check
(`str.split(fake)` was a Go panic), equality and hash of bound ones. `recordOwnSlots` covers every slot, `lookupAttrMRO` shows a
built-in type's wrapper only on the type that owns it (`bool.__add__ is int.__add__`), and `dir()` lists wrappers.

**Pickling.** `object_getstate_default` with `required`, `_PyType_GetSlotNames`, `__reduce__` of set, frozenset, method, deque
subclasses and the str iterator, bytearray's `__reduce_ex__`. **Functions**: `func_set_code`'s checks and warning,
`__dict__`'s setter, the read-only members; the compiler's `force_global` qualname. **Modules**: `module_new` and
`module_init`, `__class__`, `__slots__`, module's `__doc__`. **mappingproxy** over any mapping, its views, `|`.

**str.** `ReprObject` and `StrObject` keep a subclass's result; `format_complex_internal`; `%`'s limits; `format_map`'s
positional refusal; `sq_repeat`'s messages. **Surrogates**: as designed; `ast.parse`'s Constant stays a Low.

**test_class** runs: `test/python/libtest.py` and `libtest_run.py` give it alone a stand-in `_testinternalcapi` with
`has_inline_values` (raising), the new class `:inline-values`. Providing the module to all of Lib/test would have turned
33 files' skips into failures.

**Measured** (VM, the native driver on both sides, per method): see report 226. The classes: `:data-model` 90 listed tests to 13,
`:module-subclasses` 9 to 0, `:str-protocol` 19 to 5, `:surrogate-pairs` 14 to 0.
`:module-subclasses` 9 to 0, `:str-protocol` 19 to 6, `:surrogate-pairs` 14 to 0.

## As built: 227, pickling the core types, the iterators and the exceptions (2026-11-07, the Mac VM, queue 227)

As designed ("Design: 227"), on 226's head merged first (760899239). Base c44a0b5d. Counts are `bin/pyi lib-test --aot` (the compiled driver,
samples; the record is the main host's).

| class | before | after |
|---|---|---|
| `:pickle-core` | 41 listed | 2 listed, `:design` (`_pyio`) |
| `:pickle-iterators` | a pattern, 26 methods of the sample (test_iter 10, test_enumerate 8, test_set 4, test_range 3, test_deque 1) | removed |
| `:array-pickle` | 28 | removed |
| `:exc-pickle` | gone with 223 | test_exceptions' 11 pickling, copying and state methods pass (run alone: see the Lows) |

| module (sample) | not equal before | after |
|---|---|---|
| test_pickle | 56 of the 471 run | 26 of the 1,015 run (24 of the C classes, 2 by `_pyio`) |
| test_array | 56 | 28 (`:buffer-exports`) |
| test_iter | 13 | 0 |
| test_enumerate | 11 | 0 |
| test_range | 10 | 2 |
| test_set | 116 | 21 |
| test_deque | 14 | 8 |
| test_list | 4 | 0 |
| test_tuple | 2 | 0 |
| test_pickletools | 23 | 1 |

**The iterators** are as designed, in `iterreduce.go`, with `__length_hint__` beside them (test_enumerate's test_len and test_set
asked for it), `reversed` on its `__reversed__ = None` block, and calliter_iternext's clearing of the sentinel (gh-101892). An
enumerate, zip, map, filter or reversed subclass was a bare iterator of type `t` and had no `__dict__`: they now keep the built-in
type and `wrapNew` wraps them as any built-in's subclass; `iter(e) is e` needed `unwrapSlots`' `Iter` to give back the instance
when the value is its own iterator. `range_iter` and `range_reverse` choose `range_iterator` and `longrange_iterator` by the C's
overflow conditions, which test_range's pickling checks by type.

**The core types.** `set` follows `set_new` and `set_init`: `__new__` ignores its arguments and `__init__` fills, so a subclass's
own `__init__` signature works and `super().__init__(x)` no longer reaches `object.__init__` (the cause of test_pickle's
`test_complex_newobj` family and of most of test_set's). `frozenset_new` keeps its arguments, keywords refused for frozenset alone.
A subclass's repr names it for set, frozenset and bytearray. `deque.__reduce__` is `(type, ((), maxlen), state, iter(self))`, so
a deque holding itself pickles; `deque_copy` calls a subclass. `array.__reduce_ex__` takes the instance, so a subclass's `__dict__`
is its state. A method descriptor's and a slot wrapper's reduce is `getattr(owner, name)`; `bytearray.maketrans`; None passes
the required check of `object_getstate_default`, memoryview and code are var-sized for it.

**Identity for pickle's memo.** The memo goes by `id`, so byte-equal pickles need CPython's sharing: a static type's `__module__` is one
interned object, a built-in function's another (`funcModuleStr`, m_module's), and `pickle.dumps(map(int, x), 5)` is now byte
for byte CPython's. `type(...)` pickles still differ in one memo reference: `sys.modules`' key is not that interned string (Low).

**Found on the way.** `islice_next` overflowed `next` with a huge step and looped forever (test_islice had not returned after six minutes, when I stopped it): ported
whole. `tee` got `__copy__` and "cannot re-enter the tee iterator" (test_tee_concurrent waited forever). `set_lookkey`'s restart was
missing from `Set.lookup`, so an `__eq__` that clears the set was a Go panic (64 methods of test_set). `Contains` keeps a non-TypeError
of `__iter__`; `range()` keeps `__index__`'s exception; `_PyObject_FunctionStr` in "argument after **".

**`_pickle` (the user's decision on 227's DECISION (a), 2026-11-07).** pickle.py imports PickleBuffer from `_pickle` alone, test_inspect does
`import _pickle`, and test_pickle takes the module's presence for the C classes, so a module with PickleBuffer alone would break
test_pickle's import. `picklebuf.go` holds PickleBuffer (picklebufobject.c: `raw`, `release`, the object's own buffer through `memoryview`) and
the module `_pickle`, whose other names (Pickler, Unpickler, dump, dumps, load, loads, the three exceptions) are pickle's pure twin
through a module `__getattr__`: `from _pickle import (...)` at pickle.py's end then finds them already defined, and `pickle.Pickler` stays `_Pickler`.
`_pickle.c` is not ported, PY6g's DECISION 1 stands for it. Protocol 5's out-of-band buffers work, test_pickletools' 10 and test_pickle's
Py classes' buffer methods pass, and test_inspect imports. test_pickle's C classes now run on the pure Pickler: **a pass among them says
nothing about a C pickler.** The 24 that fail, what only `_pickle.c` does (a C function not binding in a class body, the memo proxies,
re-entrancy, its messages), stay in `:c-accelerators`, whose `:why` says this; the C classes' other entries stay listed for the same reason.
`:pickle-core` keeps the 2 `_pyio` methods, now `:design`.

**The cost.** The compiled loop is untouched. `str` iteration gains a nil test; `set(x)` runs New and Init as `deque` does. The bench sample the design named was not run.
## Design: 228, the compiler's errors and warnings, the C tokenizer, eval of byte sources (2026-11-07, the Mac VM, queue 228)

The classes `:compiler-errors`, `:compiler-warnings`, `:tokenizer`, `:eval-source`, `:ir-constants`. The parser's invalid_*
rules are already CPython's (PY9c's generated errors, 872 of 874 equal), so what is left is around them:

- **Locations.** symtable.c gives each `symtable_add_def` the LOCATION of its own node (arg, alias, keyword, pattern, handler,
  annotated target, type parameter), where `arbace.python.scope` kept the enclosing statement's; directives are recorded under
  the mangled name. `at` binds `*pos*` to the node's location; codegen's repeated keyword likewise.
- **The reader's warnings.** `lex/*warnings*` (an atom, nil: a second reading of the same text) collects, in the order made,
  `verify_end_of_number`'s and the tokenizer's backslash-brace warning and string_parser.c's `warn_invalid_escape_sequence`, the
  last in the first pass only (`p->call_invalid_rules`) and at the C's line and column, counted in the same transformed buffer.
  Each record holds the SyntaxError it becomes under an "error" filter, made by the same raising code. The host cannot run
  `warnings`, so a compiler's answer (IR, AST, tokens, or an error) carries them as a prefix (`warnings-prefix`), and the runtime
  emits them in order before reading the answer (`emitReaderWarnings`); the first that a filter raises is the compile's error.
  The alternative, a call back from the reader into the runtime, would make the ir-cache unusable for a source that warns.
- **PEP 765** (`ast_preprocess.c`, absent so far): `finally-warnings!` over the forms before the symbol table, its context the top
  of the C's stack only (a loop inside the finally ends the warning for `return` too, as the C does; a class pushes nothing).
- **The main module** goes through `compile-source` too, so it warns as `python3 FILE` does.
- **Mode eval** is read as a module; anything else than one expression statement is answered by the eval start rule.
- **eval and exec of bytes**: `_Py_SourceAsString` (`sourceAsString`), eval's leading blanks skipped.
- **`\N{}` in the compiler** by the reader's tables (`arbace.repr.python.ucn`), which ends the marker `{:named}`, the loader's
  unknown constant.
- **`_tokenize`** stays the native lexer's stream; the judge is `tokenize` over the corpus below against CPython.

Judged, beside the modules, by a corpus harvested from Lib/test: every string constant of 17 test modules that CPython's
`compile` refuses or warns about, 2,576 sources, compared with CPython on message, the four location fields, text and warnings.

## As built: 228, the compiler's errors and warnings, the C tokenizer, eval of byte sources (2026-11-07, the Mac VM, queue 228)

As designed ("Design: 228"), plus what the runs found:

- **Reader's warnings** (`lex/*warnings*`, `astjson/warnings-prefix`, go/arbace/py's `emitReaderWarnings`): tokenizer's
  `verify_end_of_number` and backslash-brace, string_parser.c's `warn_invalid_escape_sequence` (first pass, the C's line and
  column over the transformed buffer, an f-string's parts from its end token), each with the SyntaxError an error filter makes
  of it; the prefix's separators escaped (an invalid escape's character can be one). The compiler's warnings made before a
  SyntaxError are emitted ahead of it. The main module warns as `python3 FILE` does.
- **PEP 765** (`finally-warnings!`) and `c_disable_warning` (a finally body's second copy does not warn again).
- **Locations**: symtable's per node (`at`), codegen's per pattern store and alternative (`check-stores!`, codegen's order),
  type parameter, handler, starred target, repeated keyword; a dict comprehension's value before its key (symtable's order);
  mapping keys by repr.
- **compile()**: mode eval by its start rule (no fake newline, as the string tokenizer); codeop's flags to the parser (`+incomplete`,
  `+dedent`: `_is_end_of_source`'s IncompleteInputError in place of the approximate rule, the REPL's own rule unchanged);
  PyCF_ALLOW_TOP_LEVEL_AWAIT (`+await`: symtable's `allows_top_level_await`, the module's code CO_COROUTINE, eval gives the
  coroutine); CO_MAXBLOCKS; filename by PyUnicode_FSDecoder, mode's and dont_inherit's errors; `_Py_SourceAsString` (bytes,
  buffers, null bytes, eval's leading blanks, a str's U+FEFF); a byte source's decoding errors from the reader's
  `source-text` (hook mode `source-error`, bytes as Latin-1); a lone CR translated before positions.
- **Int literals over the digits limit**: the reader records a decimal literal of over 640 digits; the runtime raises
  `_PyPegen_number_token`'s SyntaxError when the run's `sys.set_int_max_str_digits` limit is lower.
- **Smaller**: `__debug__` a constant; debug f-strings' comments removed (set_ftstring_expr); a raw f-string's format spec;
  `\N{}` by the reader's tables (the `:ir-constants` marker gone); `__static_attributes__` by the name self in nested
  functions; dir() of a mapping by keys(); a main file's null bytes (file tokenizer's message) and its EOF after a
  continuation line (file tokenizer's column); a main module in a codec the host has no charset for, decoded and compiled
  by the runtime.
- **`_tokenize`**: readline's errors and wrongly typed lines after the tokens of the lines before them; lines decoded with
  errors replace. The lines are still all read first.

Results (run, the VM): 2,576 sources harvested from 17 Lib/test modules (`compile` against CPython: message, location, text,
warnings): 0 differ (72 at the first measure); 573 of them in modes eval and single: 0 (146); the same through `tokenize`: 0;
parse-check lex-errors 62 equal, syntax-errors 872 equal and 2 refused (as before). Classes: `:compiler-errors` narrowed to
test_codeop's test_future_imports (compile()'s future flags); `:compiler-warnings`, `:eval-source`, `:ir-constants` and
`:tokenizer` keep their patterns for unsampled methods, no method named. Per-module figures in the report (queue 228).
## As built: 229, dis of CPython's bytecode (2026-11-07, the Mac VM, queue 229)

### The design, first

**What tests and the library need of a code object** (read: `Lib/` and `Lib/test/` at the pinned tarball, 3.14.10):

| Attribute | Readers in `Lib/` | Tests that read it |
|---|---|---|
| `co_code`, `_co_code_adaptive` | dis, modulefinder (`_unpack_opargs`: the imports), inspect (a mention) | test_dis, test_peepholer, test_code, test_compile, test_marshal |
| `co_consts`, `co_names`, `_varname_from_oparg` | dis, pdb, trace, linecache, modulefinder | test_compile (30 uses), test_code, test_dis, test_peepholer, test_listcomps |
| `co_positions()` | dis, traceback and inspect (with `tb_lasti // 2`) | test_code, test_compile, test_dis, test_marshal |
| `co_lines()`, `co_linetable` | bdb, dis (`findlinestarts`), inspect, trace | test_compile, test_code, test_dis, test_sys_settrace, test_monitoring |
| `co_exceptiontable` | dis | test_code |
| `co_stacksize` | dis's `code_info`, inspect | test_code, test_compile |
| `f_lasti`, `tb_lasti` | dis (`distb`, the `-->` mark), traceback, inspect | test_dis, test_frame, test_traceback |
| the instructions' names | pdb (`find_first_executable_line`: what follows RESUME), dis | test_dis, test_peepholer (114 helpers), test_compile, test_patma, test_fstring |
| the specializing names (`adaptive=True`) | dis | test_opcache (38 `requires_specialization`), test_dis (6), test_type_cache (7), test_monitoring (2) |

Two facts decide the design. `traceback.py` and `inspect` index `co_positions()` by `tb_lasti // 2`, so `f_lasti`, `tb_lasti`,
`co_positions` and `co_code` must count the same units: a view cannot be a co_code alone, the machine's instruction and the
view's offset must answer for each other. And an exact dis is a matter of bytes: `flowgraph.c`'s passes (`LOAD_FAST_BORROW`, the
superinstructions, the cold blocks, `NOT_TAKEN`, the jump normalization, constant folding) decide nearly every line.

**The routes.**

- (a) A generator from pyi's IR. The IR is codegen.c's order of emission already (the compiler is written after it, its line
  passes after flowgraph.c, 219t), so the stream codegen.c would have emitted can be recovered: an abstract walk of the IR's stack finds
  where a call's callable was finished (`PUSH_NULL`, `LOAD_ATTR`'s method bit, `CALL 0` of decorators and comprehensions), what
  `with` keeps below (two slots for pyi's one) and the rest. From there flowgraph.c and assemble.c are ported as they are. It runs
  lazily, in the runtime, the first time an attribute is asked: nothing on any program's start, no IR text grows. Every instruction
  keeps the IR instruction it came from, so `f_lasti` and `tb_lasti` are exact. Its risk is the tail: wherever compile.ae is not
  codegen.c's shape. That tail is measurable, code object by code object, against CPython.
- (b) CPython's compiler ported beside pyi's own: codegen.c, compile.c, flowgraph.c, assemble.c, instruction_sequence.c (about
  13,700 lines of C; the symbol table is there). It is exact by construction, but it needs the tree: either every import compiles twice on
  the host (the compiler is evaluated there) and every IR text carries the bytecode, or the source is kept and compiled again when
  asked, which a `compile()`d string and a changed file do not allow. And its instructions have no link to the machine's: `tb_lasti` could only
  be paired by location, which is not one to one.
- (c) By design: `:dis` stays a class (with `_varname_from_oparg`, so that dis fails later). Nothing to build; test_dis, test_peepholer and the
  dis-based parts of test_compile, test_code and test_pdb stay out.

The route taken: (a) for codegen.c's half, (b)'s port for flowgraph.c's and assemble.c's. The specializing names are outside every route:
they report the adaptive interpreter's state, which pyi does not have (its inline caches are its own); `adaptive=True` shows the
unspecialized code, and those tests want a class of their own, `:specializing`.

### As built: the slice

`go/arbace/py/bytecode.go` (the IR as the stream codegen.c emits), `bytecode_flow.go` (flowgraph.c: `optimize_cfg`'s load-const,
jump-threading, tuple, list, unary and binary folds, `swaptimize`, `remove_unused_consts`, the uninitialized-locals check,
the superinstructions, `push_cold_blocks_to_end`, `calculate_stackdepth`, the prefix, `normalize_jumps`, `optimize_load_fast`),
`bytecode_asm.go` (assemble.c: jump offsets, the location and exception tables, and the view), `bytecode_table.go` (generated by
`tools/pyi/bytecode_table.py` from the tarball's `pycore_opcode_metadata.h`). About 2,900 lines of Go. `bcBuild` runs the first time an
attribute asks; a panic in it leaves the old answers from the IR. Asked of the view: `co_code`, `_co_code_adaptive`, `co_consts`, `co_stacksize`,
`co_linetable`, `co_exceptiontable`, `co_positions`, `co_lines`, `_varname_from_oparg`, `f_lasti`, `tb_lasti`, `sys.monitoring`'s offsets;
`code.replace` of them makes a new view (the machine keeps the IR; `co_consts` replaced is mapped back by identity). `marshal` keeps pyi's blob.
`_opcode.stack_effect` now answers.

What the IR's stack walk (`irPlan`) recovers: `PUSH_NULL` after the callable's last instruction (a merge of paths: at the label),
`LOAD_ATTR`'s method bit (not on a name bound by an import in the module's scope, `is_import_originated`), `LOAD_SUPER_ATTR`, `CALL 0` of decorators
and comprehensions, `f(*a)` without its list, `with`'s second slot, `STORE_FAST_MAYBE_NULL`, `CONVERT_VALUE` before a format spec, the lasti of pyi's
finally handlers (CPython's has none).

The compiler moved towards codegen.c where the shape was its own, each of these a change of behaviour or of the IR, not of the view:
`jump-if!` is `codegen_jump_if` (`if`, `while`, `assert`, conditional expressions, comprehension filters: no `COPY` of a boolean operator's value in a
test); inlined comprehensions save and restore as `codegen_push_inlined_comprehension_locals` (no tuple; a cleanup handler restores the names on
an exception, which pyi did not do); `return` of a constant loads it after the unwinding (`codegen_return`); `__classdict__`'s store after
`__firstlineno__`; locations of decorators' calls, stores of definitions, `with`, augmented assignment, lambdas and generator expressions.
And two real gaps it found: docstrings are cleaned now (`_PyCompile_CleanDoc`: `__doc__` kept its indentation since PY5), and CO_HAS_DOCSTRING is set.
The IR gained one key, `:threaded`.

**Measured** (`bin/pyi bytecode-check`, TestBytecodeDiff; VM, arm64, CPython 3.14.10 `python3 -I`):

| Sample | Code objects | `co_code` | `co_consts` | `co_stacksize` | `co_exceptiontable` | `co_linetable` | all five |
|---|---|---|---|---|---|---|---|
| 67 stdlib modules (before the last fixes) | 4,795 | 3,968 (82.8%) | 4,328 (90.3%) | 4,579 (95.5%) | 4,340 (90.5%) | 3,162 (65.9%) | 2,873 (59.9%) |
| the 9 files of the slice | 491 | 412 (83.9%) | 452 (92.1%) | 475 (96.7%) | 443 (90.2%) | 315 (64.2%) | 296 (60.3%) |

test_dis: before 0 of 145 equal (the module did not import: `code.replace(co_linetable=)`); after 107 equal, 26 classed (`@cpython_only`,
the specializing ones among them), 12 in the narrowed `:dis`. test__opcode: 7 of 7 (before: 5).

**The tail** (first differences over the 67 modules, largest first): a jump's offset after an earlier difference; `LOAD_FAST` left strong after some `try` statements (not established; read: `optimize_load_fast`'s walk
stops at an empty block, and the IR, after flow!, has not CPython's empty blocks); constant order; `all`, `any` and `tuple` of a generator expression (codegen.c inlines the
loop); keyword dicts in calls with `**`; `f"{x!r}"`'s pieces' locations. Handlers whose region lost every instruction in flow! (an `except E as e: return`,
`try: pass`) leave their blocks unreachable in the view: their depth is not in the IR. `async for`'s `END_ASYNC_FOR` jump. Each is a small
change in compile.ae or `irPlan`; none is structural.

### As built: the close (the user's decisions, 2026-11-07)

- Route: (a), the generator from the IR with flowgraph.c and assemble.c ported, decided by the user; the tail goes to later tasks.
- The specializing names: (a), `:specializing` by design. `_opcode.ENABLE_SPECIALIZATION` (and `_FT`) is 0: pyi has no adaptive interpreter, so
  `test.support.requires_specialization` skips, and the class matches the skip.
- Found on the way: sys.settrace's jump tests check `lineno_matches_lasti` (f_lasti inside the co_lines range of f_lineno) in every frame. Two
  places disagreed: an IR instruction the view folded or moved (`viewLasti` now picks a code unit of the IR instruction's line nearby), and a list
  or set display of constants, whose elements pyi loaded at their own lines (line events CPython never has: the loads are folded). 19 JumpTestCase
  methods failed on the sample until both were fixed.
- The sample against the base (6e85c5c7b), the same modules, `lib-test-all --fresh`: test_sys_settrace 365 equal, 6 failing (base the same); test_code
  8 equal, 20 failing (base 5 and 22); test_listcomps 64 equal, 2 failing (base the same: the walrus and name-reuse cases, the save and restore of
  hidden names, before and after); test__opcode 7 of 7 (base 5); test_dis 107 equal, 38 classed (12 `:dis`), 0 failing (base: none, it did not import).
  No method fails that passed on the base.

## Design: 230, PEP 734's interpreters on pyi (2026-11-07, the Mac VM, queue 230)

The classes `:subinterpreters` and `:interpreters`, judged by test__interpreters, test_interpreters and
test_concurrent_futures' interpreter pool, against `Modules/_interpretersmodule.c`, `Modules/_interpqueuesmodule.c`,
`Modules/_interpchannelsmodule.c`, `Python/crossinterp.c` and `Lib/concurrent/interpreters` of 3.14.10. Surveyed state: the
runtime already reaches most of an interpreter's state through `current.Interp` (the module table, builtins, importlib's
bootstrap, codecs, atexit, locale are `*Interp` or keyed by it, and `builtinModules[name](in)` makes a module per
interpreter). What is process-wide falls in three groups:

| state | an interpreter's? | how |
|---|---|---|
| `Interp`: Modules, Builtins, sys, importlib's bootstrap, codecs, atexit, locale | per interpreter | already; a new `Interp` fills them lazily at its first import, so `create()` costs a few dicts |
| id, whence, config, id refcount, the thread running `__main__`, the XI registry of heap types | per interpreter | new fields of `Interp` |
| the threads' count, `_shutdown`'s handles | per interpreter | filtered by `t.Interp` (cold paths) |
| monitoring's tools and `monEvents`, gc.garbage and the debug flags | per interpreter, as CPython's | the "active interpreter's registers": package variables swapped when the running thread's interpreter changes (`gil.take` and a session's enter and exit), so the hot path keeps its one load |
| finalizers | an object's `__del__` and weakref callback run in the interpreter that tracked it | `finTrack` records `current.Interp`; a drain that meets another interpreter's item enters it for the call; a destroyed interpreter's items are dropped |
| the GIL, `evalBreaker`, `current` | shared | see below |
| the intern table, `identities`, small ints, the singletons, code objects | shared | immutable or identity bookkeeping, under the GIL |
| built-in types, and the exception types a module makes once | shared | static types as in CPython (whose per-interpreter part, `tp_subclasses` and the like, is a leak recorded below) |

**One GIL.** PEP 684's per-interpreter GIL is not built: `typeEpoch`, the inline caches in shared `Code`s, the identity map, the
finalizer queue and `current` itself are unlocked package state, and locking them or passing a per-interpreter
context through every operation is exactly what the single-interpreter path must not pay for. An `isolated` interpreter
reports `gil='own'` as data (its config) and is isolated in everything but parallelism; a thread blocked in
`allowThreads` lets the others run, as threads do. This is a DECISION for the user.

**Switching.** CPython's session is `_PyXI_Enter`/`_PyXI_Exit`: on the calling thread a new `*Thread` bound to the target
interpreter takes over `current` (and the GIL's holder), with the caller's ident (one OS thread, two thread states), and
becomes `in.Main` for the session (`_PyInterpreterState_SetRunningMain`: a second session is "interpreter already
running"). `isMainThread` asks for the main interpreter too: signals stay there. Single-interpreter programs never run
this code: nothing is added to the eval loop, the compiled loop or the call path. The cost measured (210's method) is the
`gil.take` comparison and `finTrack`'s pointer.

**Cross-interpreter data.** `_PyXIData_t` as a Go value: the owner's id and a `new_object` closure, the builtin getters
(None, bool, int within ssize_t, float, str, bytes, tuple with its fallback) by exact type, `memoryview` over the shared
buffer, per-interpreter registered heap types (`Queue`, channel ends), the fallbacks `_PyFunction_GetXIData` (the stateless check
ported on the IR's names; the code shared, as marshal's copy would be equal) and `_PyPickle_GetXIData` with
`_PyPickle_Loads`' fake `__main__`. Exceptions cross as `_PyXI_excinfo` (type's names, message, errdisplay by
`traceback.TracebackException`), `types.SimpleNamespace` on the caller's side. Queues and channels are process-wide tables
under the GIL holding xidata, their items bound to the interpreter that put them, unbound at its end by the op the
queue was given.

**End.** `Py_EndInterpreter`: `threading._shutdown`, atexit's functions, the streams flushed, the interpreter marked dead and
out of the table, its items unbound. Objects still alive are left to Go's collector (a module's globals are not torn down,
as pyi's main interpreter does not either).

Not built, by design: extension-module isolation checks beyond `_testsinglephase`-less pyi (every built-in module of pyi
is per-interpreter safe or shares a read-only table), `_testinternalcapi`'s entry points (tests needing them skip).

## As built: 230, PEP 734's interpreters on pyi (2026-11-07, the Mac VM, queue 230)

As the design said: `_interpreters` (go/arbace/py/interpreters.ae), `_interpqueues` (interpqueues.ae) and `_interpchannels`
(interpchannels.ae), CPython 3.14.10's modules and Python/crossinterp.c ported over one GIL. Counts, on the VM's compiled
driver (the runtime compiled by pyi's AOT, the test modules on its loop; its subprocesses `tools/pyi/python3` on goi), judged
by `bin/pyi lib-test-all` against the recorded CPython references:

| module | before | after |
|---|---|---|
| test__interpreters | skips (no _interpreters, 34) | 34 equal |
| test__interpchannels | skips (no _interpreters, 73) | 73 equal |
| test_interpreters (api, channels, queues, lifecycle, stress) | skips (no _interpreters, 174) | 148 equal, 23 classed (testcapi 17, finalization 3, cpython-only 1, fork 1, subinterpreters 1), 3 failing |
| test_concurrent_futures.test_interpreter_pool | skips (no _interpreters, 45) | 45 equal |
| all four | 326 methods skipped | 300 equal, 23 classed, 3 failing (then test_globals_from_builtins fixed: 2) |

What the runs added to the design:

- A call with no Python frame (`interp.call(eval, ...)`, `globals`, `locals`, `vars`, `dir`, `exec`) uses the running `__main__`'s
  globals (`_PyEval_GetGlobalsFromRunningMain`); before, a nil frame panicked, and the panic left the session's thread current
  for the rest of the process. A session's run is now protected (a Go panic becomes a SystemError), so `exit` always runs.
- A new `__main__` has `__package__` None and, after the first session, `__loader__` BuiltinImporter (the sub's own
  `_frozen_importlib`).
- `_thread.set_name` and `_get_name` key on the OS thread's ident, not on the `*Thread`: one OS thread has one name in every
  interpreter (the pool's thread names seen from inside the interpreter).
- `method.__reduce__` (`getattr, (self, name)`): bound methods pickle, so `interp.call(Spam.from_values)` takes pickle's fallback.
- `Token.MISSING` is its own type, repr `<Token.MISSING>`.
- The module-builder audit: `struct.error`, `zlib.error` and `termios.error` are made once for the process (shared like static
  types); `_csv`'s state (its `_dialects`), `_asyncio`'s (`aio`) and `os.register_at_fork`'s hooks are interpreter registers now,
  swapped by `interpSwitch`, so a subinterpreter's import no longer replaces the main interpreter's.
- `bin/pyi python -c` dedents its code as CPython 3.14's `-c` does (the tests' `-c` scripts are indented).

The classes: `:interpreters` retired; `:subinterpreters` is now a design class for what stays different (`_testmultiphase` has
no `__file__`; no `-i` for the REPL test); `:testcapi` takes the `_testinternalcapi` subtests and `get_code_var_counts`;
`:finalization` takes the interpreter freed by the collector inside `list_all` and the channels whose ids CPython frees at
refcount's moment (pyi's ChannelID never reports its death, so a channel's row stays until destroyed).

Cost on the single-interpreter path (210's method, the least of 3 alternated rounds, the base 6e85c5c7b's runtime and this
branch's compiled into the same driver; a sample: fib(24), 20,000 objects with `__del__` and weak references, 4 threads
switching the GIL): CPU seconds fib 0.015 / 0.015, churn 0.391 / 0.223, threads 0.065 / 0.055, total 0.471 / 0.305 (base /
branch). The churn and threads figures vary twofold between rounds (Go's collector), so the sample shows no cost; it does not
show a gain.

Left: `test_lifecycle.StartupTests.test_sys_path_0` under `-c` (the sub's `sys.path[0]` is the `-c` file's directory, not ''),
`TestInterpreterClose.test_remaining_daemon_threads` (its subprocess exits 1), the shared GIL (the DECISION).

## Design: 231, `_multiprocessing`, posix's pointer calls, posix_spawn's signals, `_socket`'s rest and signal timing (2026-11-08, the Mac VM, queue 231)

The classes `:multiprocessing`, `:pointer-syscall`, `:spawn-signals`, `:socket-gaps` and `:signal-timing`, base d4446bf26.
Judged by test_posix, test_signal, test_socket (`bin/pyi lib-test --aot`) and test_multiprocessing_spawn (sampled). Before
any change (run): 974 methods, 771 equal, 196 classed, 7 failing; by class `:socket-gaps` 152, `:signal-timing` 15,
`:pointer-syscall` 3; failing test_posix's two test_setsigmask (the child pyi died of SIGUSR1: the mask was never applied)
and five of test_socket's (TestLinuxAbstractNamespace's three, testInterruptedTimeout, testMakefileCloseSocketDestroy).

**`_multiprocessing` as a port of semaphore.c over musl's semaphores.** SemLock's methods follow
Modules/_multiprocessing/semaphore.c (ISMINE, the recursive count, `release`'s maxvalue check by sem_getvalue,
`_rebuild`'s sem_open by name, the messages). Under it, sem_open, sem_post, sem_trywait, sem_timedwait, sem_getvalue,
sem_unlink and sem_close are musl's: the file `/dev/shm/NAME` (`__shm_mapname`'s rules), a 32-byte sem_t mapped shared,
created under a temporary name and linked (no opener sees it half written), one mapping per name and process (musl's
table: `_rebuild` in the same process gets the same handle, as CPython shows). Go forms have no atomic operation on a
mapping's memory, so what musl does by compare-and-swap is done under a lock: flock(2) on the file between processes and a
mutex between this process's threads; the waiters sleep in futex(2) on the value's address (reflect's Pointer of the
mapping, as mmap's msync takes it) and sem_post wakes one. Go restarts a futex wait after its own signal handlers, so a
waiter on the main thread sleeps in slices of 20 ms and runs the pending handlers between them, semaphore.c's own
`sem_timedwait_save` algorithm. Alternatives: System V semaphores (no pointer to shared memory) were rejected: their
IPC_RMID destroys at once where sem_unlink leaves open handles working, and a crashed process leaks them system-wide;
`_posixshmem`'s shm_open and shm_unlink come with it (musl's over /dev/shm).

**The pointer calls.** The class was by design because a raw system call took no pointer; queue 220 showed reflect's
Pointer of a live slice does (mmap, isatty, openpty). With it: timerfd_create, settime(_ns), gettime(_ns),
tcgetpgrp/tcsetpgrp, memfd_create, sched_getaffinity (the real mask, it was the CPU count) and sched_setaffinity, the
xattr functions and resource.prlimit, each as posixmodule.c's and resource.c's (the buffers grown on ERANGE/EINVAL as C
grows them). unshare and setns run on every thread (`syscall.AllThreadsSyscall`): Python's main thread is a goroutine on
any of Go's threads and /proc/self is the leader's, so only all threads give the calling thread's view C gives.

**posix_spawn's signals.** ForkExec takes no signal sets, but its child restores the forking thread's mask (Go's
BeforeFork saves it) and resets to SIG_DFL every signal Go handles while keeping an ignored one. So setsigmask is the mask
set on the forking thread (the goroutine locked to it) for the fork alone, and setsigdef of an ignored signal makes Go
handle it (notified to a channel nobody reads, which drops it as SIG_IGN would) for the fork alone.

**`_socket`.** recvmsg's ancillary data walked as C walks it (musl's CMSG_NXTHDR, get_cmsg_data_len, the truncation's
RuntimeWarning; Go's ParseSocketControlMessage refused a truncated buffer whole), recvmsg_into (one receive scattered
over the buffers, `__buffer__` taken), sendmsg_afalg, and AF_ALG's bind and accept by raw calls (Go's syscall has no
Sockaddr of the family).

**Signal timing.** Where CPython raises a pending signal's exception right after a call (pthread_sigmask's
PyErr_CheckSignals, the exit through SIGINT after an unhandled KeyboardInterrupt), the same here; the eval breaker's
places in the compiled loop are not touched (no bench needed), and the blocking calls Go restarts stay classed.

## As built: 231, `_multiprocessing`, posix's pointer calls, posix_spawn's signals, `_socket`'s rest and signal timing (2026-11-08, the Mac VM, queue 231)

As designed above; the files are `builtins_multiprocessing.go` (SemLock, sem_unlink, flags; `_posixshmem`),
`builtins_posix_ptr.go` (timerfd, tcgetpgrp/tcsetpgrp, unshare/setns, memfd_create, sched_get/setaffinity, the xattr
functions, prlimit), `builtins_socket_msg.go` (recvmsg's ancillary walk, recvmsg_into, sendmsg_afalg, AF_ALG's bind,
an empty sendmsg by a raw call) and `builtins_signal_wait.go` (the main thread's waits in ppoll with a wake pipe, the
inherited signal mask), with posix_spawn's sets in `builtins_posix_spawn.go` and exit_sigint in `builtins_signal.go`.
What the runs added to the design:

- **A blocked own signal is pending at once.** `os.kill(os.getpid(), n)` of a blocked, notified signal went through
  Go's signal thread, so `sigpending()` right after saw nothing (test_sigpending); it is now marked pending before kill
  returns, as the kernel does.
- **The inherited mask.** The child of `posix_spawn(setsigmask=...)` is pyi, whose pthread_sigmask's mask is kept by the
  module: it now starts from the thread's mask at start, so `raise_signal` of a blocked signal stays pending instead of
  faulthandler's default action (test_setsigmask, exit 138 after the mask was applied).
- **Go's SendmsgN of no data sends a byte** (and asks SO_TYPE, which AF_ALG refuses with ENOTSUP): sendmsg_afalg's
  `MSG_MORE` call before the data corrupted the cipher's input; such a call is a raw sendmsg(2) now.
- **A handler's exception through the waits.** `sockErr` turned a raised exception into an OSError of its text; it now
  passes a `*Raised` through (poll's and epoll's callers).
- Small: ancillary items of any sequence (lists), recvmsg_into into an array, `__buffer__` for sendmsg's data, assoclen
  None refused.

The eval breaker's places are unchanged, so the compiled loop is as fast as before (no bench). The new waits cost the
main thread one ppoll(2) before a select, poll, epoll_wait or socket wait with a timeout, and an fcntl(F_GETFL) plus a
ppoll before a blocking os.read; other threads are unchanged.

Counts (run, `bin/pyi lib-test --aot`; before at d4446bf26, after at the head):

| module | before | after |
|---|---|---|
| test_posix (176) | 165 equal, 9 classed, 2 failing | 170 equal, 6 classed, 0 failing |
| test_signal (53) | 38 equal, 15 classed | 42 equal, 11 classed |
| test_socket (745) | 568 equal, 172 classed, 5 failing | 731 equal, 10 classed, 4 failing |
| total (974) | 771 / 196 / 7 | 943 / 27 / 4 |

By class in these modules: `:socket-gaps` 152 to 1 (test_socket_type: gc.is_tracked), `:signal-timing` 15 to 2
(test__thread_interrupt_main, test_sendall_interrupted of the runner's skip list), `:pointer-syscall` 3 to 0. The four
failing are the base's (TestLinuxAbstractNamespace's three, testMakefileCloseSocketDestroy), not this task's.
test_multiprocessing_spawn, sampled (13 methods of WithProcessesTestLock, WithProcessesTestSemaphore and
WithThreadsTestSemaphore, spawn children on goi, 2 minutes): 8 equal to CPython, 3 skipped for sharedctypes (no ctypes:
`:ctypes`), 2 errors pickling a bound built-in method (`types.MethodDescriptorType` is builtin_function_or_method:
`:data-model`); before, the whole package skipped ("requires multiprocessing").
## As built: 232, pip on pyi and the pinned wheels' own suites: tomli and click (2026-11-08, the Mac VM, queue 232)

The user's decision on 232's DECISION (2026-11-08): **tomli** and **click** join the pins. What was built:

- **The pins** (`test/python/wheels.edn`): `:wheels` gains tomli 2.5.0 and click 8.5.1 (py3-none-any, PyPI's SHA-256 and
  size, pinned 2026-11-08; neither has a dependency on Linux), each with a sample (`test/python/wheels/use_tomli.py`,
  `use_click.py`). Two new vectors: `:test-wheels`, what the suites need beyond the pins (pytest 9.2.0 with iniconfig
  2.3.1, pluggy 1.7.0 and pygments 2.21.0, all py3-none-any; packaging is already a pin), installed by
  `arbace.python.wheels` into a site of their own (`site-test-KEY`); and `:suites`, each pin's own test suite at the
  pinned version: click's PyPI sdist (it carries `tests/`), tomli's repository tag archive (its sdist carries no tests;
  GitHub's archive bytes are pinned like the rest, a changed archive refused). `bin/pyi wheels fetch` fetches all three
  sets once (the sources into `${REPR_PYTHON_SRC:-.tmp/repr-python}/sdists/`).
- **`bin/pyi wheels suite NAME [TEST...]`**: the archive extracted afresh for each side, the suite's runner
  (`python -m unittest -v`, `python -m pytest -v`) run in its root against the installed wheels (the archive's `src/` is
  not on the path) by `python3 -S -B` (PYTHONPATH the two sites) and by pyi (PYI_SITE the same), TMPDIR and pytest's
  `--basetemp` under the run's own directory; each log read as outcomes (`arbace.python.wheels outcomes`), CPython's
  through the oracle's records (`pyrec_outcome`, live on the VM: nothing recorded is committed), judged test for test by
  `arbace.python.wheels judge` against `test/python/divergences.edn` (a class's `:tests` names `wheels-NAME/ID`, or its
  `:match` finds `pyi OUTCOME cpython OUTCOME`), and a negative control (CPython's outcomes with one passed test made
  failed, judged as pyi's: refused). TEST... runs a sample in place of the suite's `:args`.
- **pip on pyi** (`bin/pyi wheels pip-probe`) takes the two new pins with no change: 11 runs equal to CPython's pip, 8
  installed trees (331 files) equal, 2 controls refused; the no-ssl warning is gone since `_ssl` (217).

What the suites found in pyi (each fixed with a program of `test/python/pyi/` equal to CPython):

- `_operator`'s operator functions were operator.py's pure ones, so one stored in a class bound as a method: glob's
  `_StringGlobber.concat_path = operator.add` made pathlib's recursive glob fail (tomli's `tests/test_data.py` at import).
  `builtins_operator.go` now has Modules/_operator.c's operators, in-place forms, comparisons, truth, `is_`, `getitem`'s
  family and `concat` as built-in functions (`w232-operator-builtins.py`); `countOf`, `indexOf`, `length_hint`, `call`
  and the getter classes stay operator.py's.
- `BaseExceptionGroup[...]` (`__class_getitem__`, pytest's `RaisesGroup`).
- compile() of an AST unparses it (PY6h's design), so pytest's assertion rewriting, whose temporaries are named
  `@py_assert1`, `@py_builtins`, `@pytest_ar` and whose mark expressions name `$stress`, was a syntax error. Such names are
  respelled in a copy of the tree before the unparse and put back into the compiled code (names, locals, cells, frees,
  str constants, nested code), so they reach a mapping as given (`w232-ast-names.py`).
- `ast.parse` of a str constant holding a lone surrogate gave U+FFFD: the native reader's JSON answer spelled it
  `\udce2`, which Go's JSON decoder replaces. Such a str is now `{"$": "str", "u": [UTF-16 units]}`
  (`arbace.repr.python.astjson`, `tools/pyi/ast_json.py`, `tools/pydump/astdump.py` alike: ast-check stays byte for
  byte) (`w232-ast-surrogates.py`).
- print() to a file object writes each argument, separator and end apart, as `builtin_print_impl`.

Samples on the VM (aarch64), each against CPython's run of the same suite with the same wheels:

| suite | sample | result |
|---|---|---|
| tomli 2.5.0 | the whole suite (18 tests; test_data runs toml-test's valid and invalid files) | 18 equal (before: 16 equal, test_data failing at import) |
| click 8.5.1 | tests/test_basic.py | 109 equal (before: 0, conftest's rewritten import a syntax error) |
| click 8.5.1 | test_arguments, test_termui, test_testing, test_imports, test_utils/, test_compat | 1191: 1189 equal, 2 classed (`:pyio`), 0 failing |

The two classed tests are io being `_pyio` (`:pyio`, by design): `_pyio.StringIO.__init__` writes its initial value
through the subclass's `write` and seeks, and `_pyio`'s seek calls the subclass's `flush` before its `__init__` has set
its attributes; CPython running `_pyio` does the same (probed). The whole click suite (2,459 tests, the stress tests
deselected by its pyproject) is the main host's run.

## As built: 234, Lib/test judged by each test's own verdict (2026-11-08, the Mac VM, queue 234)

The user's rule of 2026-11-07 (doc/CROPS-SCORE.md on main, "Decided"): a test's own verdict is a judge no toolchain made
(1); a record a toolchain made is 0.5; a mixed stage the case-weighted mean. CPython's Lib/test is self-checking
(unittest's ok, skipped, failure, error per method), so its judge no longer needs CPython's runs.

- **The judge** (`test/python/libtest_judge.ae`): `--verdict own`, the default, passes a method when unittest's outcome
  under pyi is ok (an expected failure ok, an unexpected success a failure, as the drivers say). The methods are the
  inventory (every reference line's id, `:excluded` ones too, but the runner's lines that name no test) with every test
  pyi's run reported (pyi-only tests, e.g. the families' modules that have no reference, are judged now); a module with
  neither gives one row `MODULE/*` (its import line). No CPython outcome is read. `--verdict ref` is the old judge
  (pyi's outcome against CPython's recorded one), the oracle tier's. `--skips pass` makes a skip a pass (the DECISION's
  option (b)). `--agreement` prints both verdicts of every method and the disagreements by kind per corpus (`--dir`, or
  `--results` of a ref judge's `--save`). The record's header names its verdict (`;; (verdict own)`); a record of the
  other verdict is not judged against ("re-record it"), so a change of rule is not a storm of regressions.
- **Two classes** (`test/python/divergences.edn`, `:verdict :own`: the own verdict's alone, the ref judge leaves them
  out): `:fails-under-cpython-too` (the 11 methods CPython's recorded run fails on the recording host: the ref judge
  passed them as equal outcomes) and `:skipped-by-test`, last in the file (`^skipped`, `^import SkipTest: `, the wheels'
  `pyi skipped`): a skip is the test's "not run here", a non-pass by design; every earlier class (C modules, _testcapi,
  specialization ...) takes the skips that name pyi's gaps first, and the record still makes ok -> skipped a regression.
- **No python3 at check time**: `bin/pyi check` and `lib-test-all` ran `tools/pyi/libtest_all.py` (the universe's
  `owned`, the restartable runner `run`, `controls`, `table`) by python3. They run it compiled by pyi's AOT now
  (`pyrun`, `runner_exe`, `bin/pyi runner`, kept by the runtime's key, the generator's and the script's digests; about
  2.5 minutes the first time on the VM); `PYI_RUNNER=python3|pyi` the others; `--oracle` and the cover run keep python3.
  With python3, python and python3.14 stubs exiting 99 first on PATH, `bin/pyi check --fresh test_bool test_getopt`
  ran with the stub's log empty.
- **The negative controls hold**: the crossed pairs fail on the inventory (test_bool's run as test_int: 83 methods, 52
  failing; test_getopt as test_filecmp: 26, 16 failing); a new `:flip` pair (control 3) is the verdict's own control:
  test_bool's first ok test judged alone passes, the same run with that outcome made a failure fails; a judge blind to
  the verdict is "not sharp". All three fail as they must under both verdicts.
- **The wheels' suites** (`arbace.python.wheels judge --verdict own`, the default; `bin/pyi wheels suite`): pyi's run
  alone, passed and xfail pass, no CPython run unless `PYI_VERDICT=ref`; the control flips the judged side's first
  passed test. tomli: 18 tests, 17 pass, 1 `:skipped-by-test` (ref: 18 equal), with python3 stubbed.

**Agreement on saved results** (the newest complete run of each module the VM's task directories kept, 152 modules:
language-core 11, stdlib-tests 58, lib-test-asyncio 35, lib-test-all 48; 19,921 methods; many runs older than the
current runtime: this measures the judges, not pyi):

| corpus | methods | both verdicts | agree | disagree | own verdict alone |
|---|---:|---:|---:|---:|---:|
| language-core | 1,149 | 1,145 | 1,137 (99.30%) | 8 | 4 |
| stdlib-tests | 10,073 | 10,012 | 9,408 (93.97%) | 604 | 61 |
| lib-test-asyncio | 2,975 | 2,975 | 2,909 (97.78%) | 66 | 0 |
| lib-test-all (48 modules) | 5,724 | 5,712 | 5,646 (98.84%) | 66 | 12 |
| all | 19,921 | 19,844 | 19,100 (96.25%) | 744 | 77 |

The disagreements, by kind: 738 "ref pass, own non-pass: both skipped" (a skip CPython makes too: the ref judge's
pass, the own verdict's `:skipped-by-test`), 6 "own pass, ref non-pass: pyi ok, CPython skipped" (the ref judge's
`:runs-where-cpython-skips`). On every ok, failure and error the two verdicts agree: no method is a pass of one and a
failure of the other but by a skip. With `--skips pass`: 915 "pyi skipped, CPython ok" become passes (95.36% agree):
pyi's gaps would count as passes. The 77 judged by their own verdict alone: 59 skips, 10 ok, 4 failures or errors
(test_importlib's Frozen_/Source_FindSpecTests.test_module, test_module's finalization at shutdown: tests the ref judge
never saw), 2 imports, 2 not run. The failing sets with the classes: the ref judge fails 274 methods the own verdict
classes (pyi's skips that no class names any more: "No ssl module", "requires the C _functools module", pidfds ...,
old runs from before those were fixed; the own verdict hides such a skip of a test that never passed until the oracle
tier), the own verdict 3 the ref judge never judged.

**Fresh samples on this branch** (the VM, both verdicts of the same runs): `bin/pyi check --corpus language-core` (11
modules, 1,149 methods): own 1,065 pass, 82 classed, 2 failing; ref 1,073 equal, 70 classed, 2 failing: the same two
(test_exceptions' test_assertion_error_location and test_fstring's test_many_expressions past the 300 s test timeout on
the loaded VM); the 8 more classed are both-skipped tests, 4 more methods the excluded ones. `bin/pyi wheels suite click
tests/test_basic.py tests/test_utils`: 681 tests, 681 pass by their own verdict, the control refused, python3 stubbed.

**The independence facts** (a patch for main's `test/crops/independence.edn`, `:python :at "mac/mac/201-python-py7-py11"`,
in queue 234's report): read 1; eval 0.5 and AOT 0.5 (the runtime consumes five tables recorded from CPython's own
modules: errno, posix, fcntl, socket, _opcode); print 0.5 (the round trips print forms converted from CPython's recorded
dumps of the originals); judge 0.962, `:cases {:self 51346 :recorded 4189}` (Lib/test's 48,546 methods and print's 2,800
files by their own verdict; the 151 programs' 453 cases and read's 3,736 files against records): 69.2% (20.0% before;
89.2% if the tables count as platform data; 99.2% with the tables made from pinned sources and the originals read
natively). Scored by main's `bin/crops-score --crop python` in a scratch overlay: 69.2%, 0 refuted, 0 problems.

## As built: 235, Python's maturity: the controls, the gate's fragment, records without paths (2026-11-08, the Mac VM, queue 235)

Light: samples on the VM, nothing recorded committed as the record; the main host's night and patches in queue 235's report.

- **Every check family and its negative control** (GTS81 (a): a case's run judged against another case's reference by the
  family's own judge must fail; "control passed" or "control not judged" fails the check):

  | family | REAP source / gate step | control | |
  |---|---|---|---|
  | `bin/repr-python roundtrip`, `--text`, `lex-check`, `parse-check [--full]` | read, print; python-read-essential, python-roundtrip, python-native | `test/python/controls-read.edn`, a pair per corpus (212) | had |
  | `bin/pyi scope-check`, `compile-check` | eval (beside); python-front(-essential) | `cscontrol` (PY7) | had |
  | `bin/pyi gen --check` | python-front-essential | the generated `ops/` against the committed `datamodel/` by its `diff -r` | new |
  | `bin/pyi check`, `lib-test-all` | eval; pyi-check | `test/python/controls.edn` (crossed pairs, 234's flip) | had |
  | `bin/pyi check --aot` (compiled, the loop, pyi on goi) | AOT, eval; pyi-aot-essential, pyi-check-aot(-goi) | `arbace.python.aot/control` | fixed |
  | `bin/pyi runtime check`'s smoke programs, `repl --check` | pyi-runtime | `test/python/controls-runtime.edn` | new |
  | `bin/repr-python ast-check` | none | its first two equal files crossed | new |
  | `bin/pyi wheels check`, `pip-probe`, `suite`; `c2j-check`; `records verify` | pyi-wheels, pyi-c2j, the oracle tier | their own (205, 206, 213, 232) | had |
  | `record-ref --check`, `runtime tables`, `bench` | the oracle tier | waived (213: two runs, regenerations, speed) | |
  | `spec-check`, `samples`, `pins --check` | python-read-essential, `--pins` | none: a checklist and the pins, no case to cross | |
  | `pegen --check`, `bytecode-check`, `table-check`, `lib-test`, runtime check's forms round trip | none | missing (proposals in the report) | |

  check --aot's control was vacuous on a selection with fewer than two passing programs (it printed "do not pass" with
  nothing judged: now "left") and failed with nothing wrong when the first two passing programs share an output
  (synth/ops/cmp-eq.py and cmp-is.py: now the next program of another reference); pyi's runs on goi are crossed too.
- **bin/pyi runtime check** (230's low): a failure prints the diff's first lines and then, never cut off, each side's status
  (124 named a timeout) and stderr's last line.
- **The AOT's Go cache** trimmed by age past 2,000 MB (it was `go clean -cache`, which dropped the runtime's compiled
  packages: the next AOT step compiled `go/arbace/py` again at a 13.5 GB peak, the essential chain's `pyi-aot-essential` too).
- **Records without the machine's paths**: bin/pyi check's record, check --aot's record, repr/python's results files and
  CPython's references write the checkout's directory as `<checkout>/` and any other `/root/`, `/home/`, `/tmp/`, `/Users/`
  path's head as `<root>/` and the like (`host-free`); `ref-cpython314-lib-test-all.edn`'s two reasons naming Alpine's build
  directory rewritten in place by `fixrefs`. Found by the VM's saved runs: doctest failures' reasons name the tarball's
  files by absolute path (`File "/root/.../Lib/test/test_cmd.py"`).
- **The gate's fragment** (main's `test/gate/crops/python.edn`, a patch in the report; GT9's with 212's, 213's and
  python-records.edn in one, for the day after batch P's night of records): 4 essential steps, 8 full (pyi-runtime,
  pyi-check-aot-goi and pyi-check new), 3 oracle; pyi-check rotated a seventh of each corpus's modules a night (its
  `:id-re` reads the record's methods' MODULE, so no planner change), subsets `test/python/gate-subsets.edn`; every check
  names its control; `:mem-gib` from the VM's busybox `time -v` on samples (the essential chain 0.4 to 0.9 GB warm, pyi-runtime
  4.5 GB, pyi-check 5.6 GB at 2 jobs), pyi-aot-runtime 14 and `:heavy` (the cold compile of `go/arbace/py`).
  `gate.crops check`: 0 problems on main with this branch merged and the fragment spliced. crops-score's Python maturity:
  83.3% on main, 91.7% with this branch and the fragment spliced (the gate item 0.5 to 1), 100% once the night's records
  check the eval and AOT cells; the controls item counts 7 of 7 families with the rules matched in their files.

## As built: 240, the judge stage without python3 (2026-11-09, the Mac VM, queue 240)

Light: samples on the VM; the records it made were scratch (a detached worktree), none committed; the main host records them.

- **Records mode is the default on Python's line.** 213's `tools/pydump/records.bash` chose records only where main's shared
  tooling `test/oracle-records/oracle.bash` is; Python's branch lacked it, so every read check (lex-check, parse-check, the round
  trips, spec-check, samples, the front end) ran python3 live at check time, and 234's judge 0.962 rested on a mode the branch
  did not run. The file is vendored byte for byte from main (2a72c94aa): a missing record is UNRECORDED and fails; day work
  sets `REPR_PYTHON_ORACLE=live`; the main host records with `bin/repr-python records record` (which now makes
  `.tmp/repr-python` in a fresh clone).
- **The round trips' originals read natively** in records mode (`REPR_PYTHON_RT_ORIG=native`, the default there; 234's probe):
  level 1 the native reader's forms of the original against its forms of the print, level 4 (`--text`: `parsecheck dump --full`)
  the print's bytes against the original's: each file's own verdict, no record consumed; a file the reader refuses (a charset
  the runtime lacks) left out and printed. Levels 2, 3 and 5 and the helper's originals stay live's and the oracle tier's.
- **syntax-errors by each case's own verdict.** `tools/pydump/syntax_cases.py --expect` writes what each case's test in the pinned
  Lib/test states of its error: a test_syntax doctest's exception line (format_exception_only's, ELLIPSIS and
  IGNORE_EXCEPTION_DETAIL honoured), `_check_error`'s pattern searched in `str(err)` with its subclass and positions,
  test_exceptions' `check` positions, `assertAllRaise(SyntaxError, ...)`'s pattern: 746 of the 874 cases. `parsecheck check
  --expect` judges a case the native reader rejects by them alone (`:own-pass`, `:own-fail`; the record not read); a case it
  accepts (a compiler's error, which ast.parse accepts too) or with no expectation by its record. `--judge record` (the oracle
  tier's, verify mode's default) and `--agreement` (both, the disagreements printed). 621 cases judged so: 592 pass, 29 classed
  in parse-check-known.edn (`:own-traceback` 24: the doctest expects traceback's keyword suggestion, "invalid syntax. Did you mean
  'for'?", added by format_exception_only, not the parser; `:own-mode` 5: test_fstring evaluates its cases, mode eval, where the
  parser's error differs from exec's); the agreement with the record: every own pass is equal to the record, the 29 are the only
  disagreements. The control (controls-read.edn's syntax-errors pair) is now judged by the reference's own verdict, and fails as
  it must.
- **The cases made by pyi**: in records mode `syntax_cases.py` runs on pyi (`bin/pyi script SCRIPT ...`: a script of tools/pyi or
  tools/pydump compiled by pyi's AOT, kept by the runtime's key, the generator's and the script's digests; 2.5 minutes cold, 0.5 s
  warm), its cases and expectations byte for byte python3's (874 files, 746 lines); the pack's inputs are the verify's
  (`REPR_PYTHON_SYNTAX_CASES=pack` the old route).
- **bin/pyi check's universe by regrtest's findtests on pyi**: `tools/pyi/libtest_all.py find` (findtests and SPLITTESTDIRS copied
  from the pinned Lib/test/libregrtest/findtests.py) over the tarball's Lib/test on the compiled runner; the families' modules by
  corpora.edn's paths, lib-test-all's the others less the owners'; the references' `:module` lines (CPython's recorded
  `--list-tests`) no longer read: 529 modules, none more or less than they name. The inventory of methods stays the references'
  ids (a record of which tests exist: it can add a non-pass, "not run", never a pass).
- **Proved with python3 removed** (python3, python, python3.14 stubs exiting 99 first on PATH, their log empty), records made
  beforehand in a scratch checkout: `make python-essential` (every control fails as it must; check --aot 20 pass, 1 classed);
  lex-check and parse-check (both modes) on samples, lex-errors, syntax-errors; the round trips on samples and a 57-file sample;
  `bin/pyi check --fresh test_bool test_getopt` (41 pass, the three controls fail as they must).
- **The judge stage**: 0 before (main's fact at the ref, and true on the base: python3 live); after 0.968, `:cases {:self 51967
  :recorded 3568}` (+621 syntax-errors cases by their own verdict); print 0.5 to 1; Python's independence 79.4% by main's
  `bin/crops-score --crop python` in a scratch overlay (main's stale facts at the ref 80.0% with judge 0; 234's patch 69.2%).
  Left at 0.5: read's other 3,115 files against CPython's recorded tokens and trees (DECISION 3 (a)), the 151 programs' 453 cases,
  eval and AOT's five recorded tables (DECISION 2 (a)).

## As built: 239f, two fixes of 3.15's work for 3.14 too (2026-11-09, the Mac VM, queue 239f)

The user's decisions of 2026-11-09 on queue 238's and 239's DECISIONs (a): two fixes first made behind
`REPR_PYTHON_VERSION=3.15` apply to 3.14 as well. Light: samples on the VM, no record written.

- **`async for`'s bytecode view** (go/arbace/py/bytecode.ae, bcEmit): END_ASYNC_FOR's jump target (the SEND of the `__anext__`
  it ends) and codegen_async_for's NOT_TAKEN are no longer 3.15's alone; before, END_ASYNC_FOR had no target under 3.14,
  `removeUnreachable` dereferenced nil and the code object had no view (`bin/pyi bytecode-check` printed `NO VIEW`).
- **An inlined comprehension's saved names** (arbace/python/compile.ae, comprehension!): DEF_NONLOCAL names are not saved,
  and a saved name that is not the comprehension's own local (an assignment expression's target in a tuple element at module
  level, GLOBAL_EXPLICIT in the comprehension) is resolved by its scope as compiler_nameop does, not kept a hidden fast local
  (its value was lost). 3.14.10 has the same codegen rule (the starred element is 3.15 syntax).
- 3.14.10's own tests, base 97f2307e0 to head, method for method: test_dis 107 to 109 of 145 ok (the two
  `test_async_for_presentation`), test_named_expressions 59 to 74 of 74, test_listcomps 64 to 65 of 66
  (`test_assignment_expression`); no method that passed fails, in those and in test_compile's first 65 methods, test_grammar and
  test_patma. `test_disassemble_asyncwith` (twice) still fails under 3.14, for another cause: CPython's NOT_TAKEN after the
  with's exit POP_JUMP_IF_TRUE has no exception handler in an async with (a gap in the table), pyi's has one.
- test/python/divergences.edn: `:dis` no longer names the two `test_async_for_presentation` tests (the judge reports names of
  `:tests` that pass as stale).

## Design: 241, the cycle collector over pyi's objects (2026-11-09, the Mac VM, queue 241)

Read from CPython 3.14.10's Python/gc.c (`gc_collect_main`, `deduce_unreachable`, `move_legacy_finalizers`,
`handle_weakrefs`, `finalize_garbage`, `handle_resurrected_objects`, `delete_garbage`, `invoke_gc_callback`), Modules/gcmodule.c,
Objects/weakrefobject.c and Lib/test's test_gc, test_weakref, test_finalization and the modules `:finalization-collect` and
`:ssl-finalization` name, against go/arbace/py at 4c6ebe2c6 (218's queue and 222's detector in `finalize.go`).

**3.14.10's collector, as observable.** The pinned 3.14.10 is the generational collector again (gc.c: three generations,
`gc_select_generation` by thresholds 2000/10/10 and the long-lived ratio; no incremental young/old scheme: `gc_free_threading.c` is
the free-threaded build's, not this one's). `gc_collect_main`: callbacks' "start"; `deduce_unreachable` (`update_refs` copies each
object's count, `subtract_refs` takes away the references from inside the generation by `tp_traverse`, `move_unreachable` keeps
what a positive count reaches); legacy finalizers (`tp_del`, none in Python code since 3.4: only `gc.garbage` under DEBUG_SAVEALL
is observable); `handle_weakrefs` (every weak reference to the trash cleared; callbacks only of weak references outside the trash;
a weak reference inside it cleared without its callback); `finalize_garbage` (`tp_finalize`, `__del__`, once: PEP 442's flag);
`handle_resurrected_objects` (`deduce_unreachable` again over the trash); `handle_weakrefs` again without callbacks; `delete_garbage`
(`tp_clear`, or `gc.garbage` under DEBUG_SAVEALL); "stop" with collected and uncollectable; the count returned. Whatever is
outside a cycle dies by reference counting at once.

**How pyi knows its objects' references (`tp_traverse`'s role).** pyi has no counts, so `subtract_refs` has nothing to subtract
from: the references from outside are Go's (stacks, the compiled loop's registers, goi's frames, the runtime's globals), and only
Go's collector sees them all. 222's probe stays the oracle: every edge between two probed nodes hidden, a probe's Go finalizer on
each node, `runtime.GC()` under the GIL; a node whose probe did not run is held from outside. What 241 changes is the traverse,
`cyTraverse`, the table of each type's reference slots (CPython's `tp_traverse` per type), two kinds of slot: an `Object`
interface slot and a typed pointer slot (`*Dict`, `*Tuple`, `*Cell`, `*Frame` ...), both hidden by address. Soundness is 222's:
an edge is hidden only between two probed nodes, so a node whose probe runs has no reference but hidden ones; a slot the table does
not list (a map's value, a closure's capture, a field of a non-object struct) stays visible, which only keeps more alive. The one
new obligation: a slot listed must belong to its node alone (a shared backing array would hide an outside holder's reference); the
table lists only owned slots.

**What Go's collector already frees, and what must run at collect.** An unreachable object with no Go finalizer is Go's to free,
cycles included: nothing to do but count (which pyi cannot: below). A tracked object (218: `__del__`, weak references, a
generator's close, a socket's warning; 241: a built-in type's `tp_finalize`) has a Go finalizer, and Go never frees a cycle that
reaches a finalized object from itself: those are the collector's, as in 222. At collect, in CPython's order: the weak references
to the trash cleared and the outside ones' callbacks called, `__del__` once, resurrection rechecked, `gc.garbage` under
DEBUG_SAVEALL, `gc.callbacks`' start and stop with the counts.

**The runtime's own holders (CPython's caches hold no references).** A probe is defeated by any Go variable that holds a node: the
runtime's registries and caches are such holders, where CPython keeps a borrowed pointer, a version tag or a weak reference:
`identities` (id() of an untracked object), `funcAnnotations`, `localStates` (_thread._local's per-thread dicts), the attribute
sites' inline caches (`Code.attrICs`: a class and the function found), a type's `attrCache` and `initFn`, `classInfos`,
`subclasses`, `getsets`, `newWrappers`, abc's registries. 241 treats them as *weak registries*: before a probe each hook takes its
entries for the probed nodes out (kept by address), after it puts back the survivors' and drops the trash's; a cache entry is
simply dropped (it refills). A registry with no hook only keeps its objects alive (conservative). Classes are nodes only through
these hooks: a class the registries pin never fires.

**What the classes' tests need** (each test's own verdict at the base, its ids run alone by pyi's AOT, `.tmp/q241/t/runids.sh`):
`:finalization-collect` 31, of which 15 already pass (asyncio's free_threading, futures, base_events, streams, one test_tasks,
importlib's three locks, test_thread, test_subprocess, xml_etree's two: served by 218, 222 and 224 since the class was made); the
16 others: six PyTask tests (a task's cycle through bound methods, coroutine frames and futures), test_module's
test_clear_dict_in_ref_cycle (a cycle through a class, its functions and their globals), test_threading_local's two (`localStates`),
test_weakref's two WeakMethod tests (a stale inline cache), test_os's scandir iterator and test_sqlite3's two cursors (a built-in
type's `tp_finalize`), and two whose child process runs pyi on goi (faulthandler's "Garbage-collecting" frame, threading's
`__del__` at shutdown). `:ssl-finalization` 12: test_dealloc_warn passes; the rest are cycles through an SSL context's callbacks,
a socket's `owner` (a weak reference in CPython) and asyncio's transports, protocols and closures.
A probe over the base (`.tmp/q241/p/tl.py`): cycles through a list, a tuple, a dict or an instance are found; through a function's
defaults, a closure's cell, a bound method, an exception's traceback and its frames, an instance whose `__dict__` was handed out,
`_thread._local` and `_threading_local.local` are not.

**The plan.** (1) `cyTraverse` over instances (the dictionary its own node once handed out), lists, tuples, dicts, sets,
functions, cells, bound methods, built-in methods' `Self`, class and static methods, properties, exceptions, tracebacks, frames
(escaped and not running), generators (222's rule), modules not in `sys.modules`, the SSL context and socket; the dictionaries of
`sys.modules`' modules and builtins are leaves (they are held: walking them would walk the whole program). (2) the weak registries'
hooks: identities, funcAnnotations, localStates, the inline caches (a registry of the caches' arrays, swept of stale entries at each
collection: WeakMethod's), a class's own caches; classes as nodes with classInfos, subclasses, getsets and newWrappers hooked, a
class held by its module under its name a leaf. (3) `tp_finalize` for built-in types (`workFin`: a function in the record): the
scandir iterator's ResourceWarning, a sqlite3 cursor's statement reset (`pysqlite3` registers its own through `py.TrackFinalizer`;
a type of another package tracked only when it has no Go finalizer of its own). (4) faulthandler's "Garbage-collecting" frame line
while a collection runs `__del__`.

**DECISION 1** (queue 241, the user's): automatic collection.
- (a) none, as built: `gc.collect()`, DELETE_FAST's moment and Go's own collections (218's queue) only (recommended): no cost; a
  cycle through a finalizer lives until the program collects, as before 241 (GCTogglingTests and test_bug1055820c/d stay classed).
- (b) CPython's trigger approximated: a collection when the tracked objects grew by 2,000 since the last one (checked at the eval
  breaker's drain), `gc.disable()` honoured, `get_count` the tracked growth: `__del__` at CPython-like moments, a probe's
  `runtime.GC()` every 2,000 tracked allocations (to measure with 210's method before adopting).

**DECISION 2** (queue 241, the user's): finalization at exit.
- (a) a follow-up task ports `finalize_modules` (Python/pylifecycle.c: `__main__`'s dict cleared in `_PyModule_ClearDict`'s order,
  then the other modules', a collection between) (recommended): `__del__` and weak callbacks at exit as CPython runs them
  (test_threading's test_main_thread_during_shutdown, test_gc's two at_shutdown tests); every program's exit changes, so the smoke
  programs and the night's records judge it.
- (b) keep exit as it is: those tests stay classed (`:gc-collector`'s "modules cleared at exit").

**DECISION 3** (queue 241, the user's): `gc.get_objects`, `get_referrers` and the counts of untracked garbage.
- (a) keep them CPython's shapes (empty lists; the count of a cycle with no finalizer and no weak reference not counted)
  (recommended): exact answers need a registry of every container (a Go finalizer or an address list per list, dict, instance,
  tuple ...), a cost on every allocation, for test_gc's ten classed counts and test_get_objects*.
- (b) a registry of every heap container made since `gc.get_objects` was first called: exact from then on, 0 before.

## As built: 241, the cycle collector over pyi's objects (2026-11-09, the Mac VM, queue 241)

What "Design: 241" proposed, built in `go/arbace/py/finalize.go` and its call sites (attr.go, builtins_posix_ext.go,
builtins_ssl_conn.go, builtins_faulthandler.go, go/arbace/pysqlite3/cursor.go); light samples only.

- **`cyTraverse`** (tp_traverse's role) replaces 222's `cySlots`: instances (a handed-out `__dict__` a node of its own, the
  builtin base's value), lists, tuples, dicts, sets, functions (globals, defaults, kwdefaults, closure cells, annotate,
  type params, dict, doc, module), cells, bound methods, built-in methods' `Self`, class and static methods, properties,
  exceptions (args, context, cause, traceback, notes, dict), tracebacks, escaped frames not running and not a generator's,
  generators (222's rule), modules not in `sys.modules`, the SSL context's callbacks and its Python object, the SSL socket's
  context, socket and hostname. Typed pointer slots are hidden by address beside the Object slots. The leaves of a
  collection (`cyLeaves`): every interpreter's builtins, `sys.modules` and its modules' dicts, the frames on a thread's stack.
- **The weak registries** (`cyHideRegistries`): id()'s `identities` and `funcAnnotations` entries of the probed nodes taken out
  for the probe, the reachable ones put back, a tracked trash node's identity into its record. Classes are not nodes yet:
  their registries (classInfos, subclasses, getsets, newWrappers, the inline caches' types, abc's) hold them.
- **The inline caches** (`attrSites`, `sweepAttrSites`): every code's attribute caches registered at their allocation; each
  gc.collect() whose type epoch moved clears the stale entries (WeakMethod's method deleted from its class).
- **`tp_finalize` for built-in types** (`TrackFinalizer`, `workFin`, the record's `fin`): the scandir iterator (ResourceWarning
  "unclosed scandir iterator %R", then closed), a sqlite3 cursor (its statement reset, cursor_dealloc's), pysqlite3 a package
  of its own; DELETE_FAST's moment now checks a tracked object of any package.
- **_ssl's owner** a weak reference as CPython's (gh-146080: a dead owner fails the SNI callback with SSL_AD_INTERNAL_ERROR,
  the callback not called). **faulthandler** prints "  Garbage-collecting" for the current thread while gc.collect() runs.
- **`PYI_GC_DEBUG=1`**: each probe prints the probed nodes held from outside that no other probed node holds.
- The printed `.ae` diffs are large because mode `:full` keeps every form's line position: a line inserted shifts the
  `:go/pos` of every later form; the printed Go's diff is small (attr.go 26 lines, faulthandler 3, posix_ext 14,
  ssl_conn 50, finalize 422, pysqlite3/cursor 15).

**The classes** (each test's own verdict; the ids run alone by pyi's AOT, `.tmp/q241/t/runids.sh`): `:finalization-collect`
31 to 8 (15 passed at the base already, the class's names stale since 222 and 224; 6 served by 241: test_os's scandir,
test_sqlite3's two cursors, test_weakref's two WeakMethod, test_threading_local's pure-Python local, these six and test_ssl's three moved to `:finalization-goi`: they pass compiled; test_faulthandler's
test_gc to `:finalization-goi`, test_threading's test_main_thread_during_shutdown to `:gc-collector`); left: test_module's
class cycle, `_thread._local` (localStates), the six pure-Python Task and Future tests. `:ssl-finalization` 12 to 8 (test_ssl's
three SNI tests served, test_dealloc_warn passed at the base); left: asyncio's TLS tests.

**Found, not fixed**: in the six PyTask_PyFuture tests the future is held after the collection though no cycle holds it:
`PYI_GC_DEBUG=1` shows no probed root, so its holder is Go-side and acyclic (a shield of two `_PyFuture`s whose loop's
`create_future` makes `_PyFuture`s reproduces it, `.tmp/q241/p/m241.py` T.test_a; the same with the C Future passes).

**The samples** (the VM): `.tmp/q241/t/check241.sh ROOT` (10 cases against CPython, its control flipped): PASS on the head,
FAIL at 4c6ebe2c6 (9 cases differ). `bin/pyi lib-test --aot test_finalization test_gc test_weakref`: 216 methods, 146 pass,
70 classed, 0 failing. `make python-essential`: check --aot 21 programs, 20 pass, 1 classed, 0 failing, every control fails as
it must. `bin/pyi runtime vet`: ok. Compiled, the children of test_faulthandler's test_gc and test_threading's shutdown test
print CPython's lines (the Garbage-collecting frame; the first "GC: True True True").
On goi (`bin/pyi run cyc241.py`, the runtime interpreted): no crash; defaults, method, list and scandir as CPython, the rest
still held by goi's registers (218's REQUEST), as 222 found: hence `:finalization-goi`. `bin/pyi runtime check` with
`PYI_SMOKE_ONLY` (f-struct, g-weak, d-ssl, f-intbytes, c-locale, g-builtin-module, e-gen, g-gc, f-del): the packages printed
and read again the same, the tables and runtime-ops equal, 8 programs equal, the REPL session equal, the controls fail as they must.

## As built: 242, sys's attributes and sigwaitinfo (2026-11-09, the Mac VM, queue 242)

The two deferred classes `:sys-module` (23 tests) and `:siginfo` (named by its skip message), grouped by the missing piece and
ported from CPython 3.14.10's Python/sysmodule.c and Modules/signalmodule.c (the pinned tarball). Light: `bin/pyi lib-test-all
--fresh test_sys test_signal` on the VM before (base 2b5c0c075) and after, each test's own verdict; no record written.

- **The pieces of `:sys-module`** (23 tests): (1) struct sequences, sys.flags, float_info, int_info, hash_info, thread_info were
  namespaces (`len()` failed; flags' bool fields, no instantiation, the repr): test_attributes, test_thread_info, test_sys_flags,
  test_sys_flags_no_instantiation; (2) functions missing: call_tracing, _current_frames, _current_exceptions,
  _getframemodulename, setdlopenflags/getdlopenflags, getallocatedblocks; (3) argument and status rules: getdefaultencoding's
  arguments, setswitchinterval's type, displayhook without sys.stdout (RuntimeError), sys.exit's tuple status and
  handle_system_exit's exit status, implementation.supports_isolated_interpreters; (4) the process: PYTHONIOENCODING and the C
  locale's surrogateescape on stdin and stdout, the exit message through stderr's errors handler, sys.orig_argv, `exit()` in
  the REPL; (5) the end of life: is_finalizing and sys.flags read from a `__del__` at exit; (6) the C API's test modules
  (test_jit_is_active, test_getallocatedblocks' with_pymalloc).
- **Built**: go/arbace/py/builtins_sys_info.ae (new): the struct sequence types as `newStructSeqType("sys", ...)` with
  CPython's fields and counts (flags: 18 in the sequence, gil, thread_inherit_context and context_aware_warnings by name only;
  the 3.15 type adds lazy_imports; flags refuses instantiation), `sysStruct`/`set`, `call_tracing` (the thread's monitoring
  re-entry guard cleared around the call, as _PyEval_CallTracing clears tracing), `_current_frames`/`_current_exceptions` over
  the thread registry, `_getframemodulename` (the frame's globals' `__name__`), the dlopen flags (RTLD_NOW at start),
  getallocatedblocks (Go's live heap objects after a collection), the standard streams' encoding (`stdioConfig`: PYTHONIOENCODING
  `ENC[:ERRORS]` unless -E/-I, stderr's errors always backslashreplace; strict, or surrogateescape without a forced encoding
  when LC_ALL/LC_CTYPE/LANG says C or POSIX; `textFile.writeStr` encodes by the codec, `print` writes its pieces through it),
  `exit`/`quit` (site's Quitter) and `sys.orig_argv` (bin/pyi python passes the executable and its command line in
  PYI_ORIG_ARGV). sys.go: sys.exit(tuple) raises SystemExit with the tuple's items (PyErr_SetObject), is_finalizing reads
  `pyFinalizing`. interp.go: systemExitStatus is handle_system_exit's (several arguments a tuple, an int as C's int, beyond long
  -1 i.e. 255, no trimming of the message's newline, written through stderr's errors handler). Small hunks outside sys:
  builtins.go (print's write path), registry.go (exit/quit among the builtins), thread_gil.go (setswitchinterval's argument),
  bin/pyi (PYI_ORIG_ARGV).
- **`:siginfo`**: go/arbace/py/builtins_signal_info.ae (new): `signal.sigwaitinfo`, `sigtimedwait` (the timeout's rounding and
  "timeout must be non-negative" as signalmodule.c) and `signal.struct_siginfo` (seven fields, tp_name "signal.struct_siginfo"), over
  one wait that `sigwait` shares (`sigWaitSet`). Go's handlers give no siginfo_t, so the record is kept where pyi sends the signal:
  kill/os.kill/raise_signal of the own process (SI_USER, own pid and uid), pthread_kill (SI_TKILL), the interval timers
  (SI_KERNEL, 0x80); a signal from another process arrives as SI_USER from pid 0 uid 0 (CPython: the sender's pid and uid; no
  test checks it). The first arrival of a held signal keeps its record.
- **Counts** (own verdict, the tests of each class): `:sys-module` 23 not ok before, 4 after (19 ok): test_is_finalizing and
  test_issue20602 (a `__del__` at exit: queue 241's finalization at exit, which sets `pyFinalizing`), test_jit_is_active and
  test_getallocatedblocks (they import _testcapi, CPython's C API test module; getallocatedblocks, tried by hand, drifts by about
  130 blocks a call under goi where the test allows 50). `:siginfo`: the five named tests of test_signal (test_sigwaitinfo,
  test_sigtimedwait, _poll, _timeout, _negative_timeout) skipped before, ok after; the entry is removed; test_eintr's two (they
  need the walltime resource, skipped in both) were checked as scripts. test_sys alone: 31 ok / 21 not ok / 51 skipped to 50 ok /
  3 not ok / 50 skipped; test_signal: 38 to 43 ok, 18 to 13 skipped; the one failure (test__thread_interrupt_main) is
  `:signal-timing`'s.
- Not done, with the cause: audit hooks (`sys.audit`/`addaudithook` stay no-ops: pyi's runtime raises none of CPython's events;
  test_audit is in no class of this task), getrefcount (a constant; test_refcount is cpython_only), the JIT, `-X utf8=0`/-I's
  effect on the standard streams (the stream config reads the environment; -E and -I are honoured for PYTHONIOENCODING
  through PYI_IGNORE_ENV), stdin's decoding by PYTHONIOENCODING (only the encoding of writes).

## As built: 243, a module's doctests (2026-11-09, the Mac VM, queue 243)

`:doctests` (69 tests in the main host's records, one case each) was no single cause: doctest itself (CPython's pure-Python
Lib/doctest.py: parser, runner, finder, `__file__`/linecache, traceback matching, `__test__`, DocFileSuite) ran correctly
on pyi's modules; each failing case was one example, and the examples' causes were pyi's own.

- **How they were found.** A driver (`dt.py`, in the report's appendix) loads a Lib/test module through unittest's loader and
  runs every `DocTestCase` alone, printing doctest's own report of the failing examples. Run by pyi's AOT (`bin/pyi aot`,
  where finalization works; on goi, `bin/pyi run`, two more cases fail only for goi's registers). 249 doctest cases of
  test_extcall, test_unpack, test_unpack_ex, test_genexps, test_setcomps, test_listcomps, test_generators, test_weakref,
  test_syntax, test_descrtut, test_deque, test_heapq, test_metaclass, test_difflib, test_collections, test_enum, test_math,
  test_pep646_syntax, test_http_cookies, test_pickle, test_typing, test_decimal (67), test_statistics (29) and test_doctest
  (68): at the base 11 failed (test_extcall, test_unpack, test_unpack_ex, test_genexps, test_generators' email,
  test_weakref's libreftest, and test_doctest's test_CLI, test_DocFileSuite, test_look_in_unwrapped, test_testfile, wrapped);
  decimal's and statistics' doctests, in the main host's older record, already pass at the base.
- **Causes fixed** (each with its CPython source):
  1. `compile(src, name, 'single')` took a first string statement for a docstring and displayed nothing: `>>> "a"` printed
     no `'a'`. Interactive has no docstring (compiler_mod); `compile-module*` leaves the docstring to mode exec. The cause of
     six of the 11 (test_doctest's five and every file doctest whose example is a lone string).
  2. unpack: "too many values to unpack (expected 2, got 3)" for an exact tuple, list or dict (unpack_iterable); an iterator's
     message stays "(expected 2)".
  3. A call's `**` (DICT_MERGE): a keyword after a `**` is a one-item map merged (compiler_subkwargs), so a repeat is
     found; a mapping that is no dict is merged key by key through keys() and [], so its own repeated keys are found; the
     repeat is reported before a non-string key ("keywords must be strings" is the call's, later).
  4. A lone `*x` argument is the call's own tuple argument (starunpack_helper): CALL_FUNCTION_EX makes the tuple and says
     "f() argument after * must be an iterable, not X" (only for a type with no iteration; `f(1, *x)` keeps LIST_EXTEND's
     "Value after * ...").
  5. Slot wrappers' `__doc__` (typeobject.c's slotdefs: `go/arbace/py/slotdocs.ae`, generated from 3.14.10's own wrapper
     descriptors) and the doc of a built-in method named like a slot (`gen.__next__.__doc__` is "Implement next(self).").
  6. `weakref.ref` subclass with keywords (`ExtendedRef(ob, foo=1)`): __new__ counted the keyword values as positional.
- **Left: one case, by design.** test_genexps' "Verify re-use of tuples" needs a freed tuple's address handed to the next
  tuple (reference counting): class `:tuple-id-reuse`.
- **After.** The same 249 cases: 1 fails (the class above), measured with `bin/pyi aot` at the head. `bin/pyi lib-test --aot`
  on test_genexps, test_extcall, test_unpack, test_unpack_ex, test_doctest, test_weakref: see the report.
- **Not a cause:** `__file__`, `__loader__`, linecache, doctest's finder over pyi's modules, `sys.displayhook`, traceback
  matching, reporting flags: all as CPython. test_json's doctests do not load in the driver (a `import_fresh_module` artifact)
  and test_ctypes skips: unmeasured here.
- **Check.** The probe `probe243.py` run by pyi's AOT equals `python3 -I -B`'s output line by line; its control compares a
  changed reference and must differ (the report's appendix).
- **Found by the lib-test run, classed:** test_doctest's `test_DocTestFinder.non_Python_modules` is defined only `if
  int.__doc__:`; pyi's built-in types carry no docstrings, so the case is not run (before and after 243): class
  `:builtin-docstrings` (deferred). `bin/pyi lib-test --aot` of test_genexps, test_extcall, test_unpack, test_unpack_ex,
  test_doctest, test_weakref at the head: 217 methods, 192 pass, 24 classed, 0 failing once that class exists (the run
  before it: 1 failing, this one).

## As built: 247, the programs' own verdicts: golden outputs reviewed against the Reference (2026-11-09, the Mac VM, queue 247)

Light: the review is reading and awk, the checks run on stage 0 in seconds; pyi's own runs of the programs (`bin/pyi run`, goi)
were sampled beside (below). Nothing of the runtime (go/arbace/py) changed. Models: Java's JCV2 (the jls programs' `NAME.out`
and `golden.edn`, `main/java-jcv2` 79f4658d5: a `:golden` verdict, the review line by line, the judge counting it as own) and
Scheme's SCS6 (Larceny's 13 programs: golden outputs, reviewed line by line, the judge `golden-items`, the `_control` case judged
against another program's golden output).

**What is a program's own verdict.** `test/python/golden/ID.out` (ID the program's id without `.py`: `synth/datamodel/del.out`,
`samples/control.out`; a tree of its own, so the synthetic corpus's directories, `gen --check`'s `diff -r` and the records' source
keys are untouched) is the program's expected standard output, with `test/python/golden/review-*.edn`: per program `:sha` (the
SHA-256 of the reviewed text: editing a golden text needs a new review), `:status` (default 0) and `:stderr-last` (only where the
review states the program's last stderr line: `synth/paths/uncaught-exception.py`'s `ValueError: uncaught`; otherwise stderr is
not part of the verdict, as the DeprecationWarnings of `~True` in `builtins/bool.py` and `ops/unary.py` are not), and the entries
`[LINES SECTIONS BASIS WHY]`: each line of the text in exactly one entry, SECTIONS the Python 3.14 Language Reference's or the
library documentation's (`reference/FILE: heading`), BASIS `#{:ref :lib :impl}` (`:impl` where neither document fixes the line and
the reason says what CPython's behaviour is: 17 entries, 18 lines), WHY the reading (what the Reference says, the arithmetic
checked by hand: `12345 ** 3`, `divmod(12345, -7)`, `pow(12345, -1, 97)`, `2.5 % -0.7`, the precedence of `control.py`'s
`r`, the C3 order, the comprehension scopes). The golden texts were made once from `python3 -s -P -B` 3.14.10 (the interpreter
the references were recorded with) after the reading, and equal CPython's recorded runs (`ref-cpython314.edn`: status, stdout's
SHA-256, stderr's last line) in all 127 programs; `bin/pyi golden` checks this agreement on every run, so a golden text that
parts from the record is a failure to explain, not a class.

**The judge.** `arbace.python.aot` `golden-refs` turns a reviewed program into a reference of the same shape as a CPython record
(`:stderr-last :any` where the review states none); `check --aot --golden DIR` (`bin/pyi check --aot` passes it) judges a run
against it for the compiled run, the loop run and pyi's run alike (the three cases of a program: the 453 cases of the 151
programs, 381 of them now golden, 72 left on the record); a run that is not the golden text is classed `:as-pyi` (the rule: all
three executions agree, the divergence is the interpreter's) or `:differs`/`:failed` as before, its reason naming "the golden
text". The judge's negative control crosses golden references (the first two passing programs' runs, each judged against the
other's golden text: `samples/classes.py` and `samples/control.py`). It prints `verdicts: 127 programs by their own golden text
(381 cases: compiled, loop and pyi), 24 by CPython's recorded run`.

**`bin/pyi golden`** (`make pyi-golden`; in `make python-essential`): the reviews against the texts (a malformed entry, a line in
no entry or in two, an entry past the end, the digest), the agreement with the record, a text without a review or a review
without a text, the negative control (one program's review judged against another's text finds 8 problems), and then the awk
models.

**The models** (`test/python/golden/models/`, `models.tsv`: program, script, arguments): 13 of the programs are also *computed*
by an awk script written from the Reference alone, and `bin/pyi golden` requires its output to equal the golden text (and
the first model against another program's text to differ): `boolean.awk` (`and`/`or`: the deciding operand, the calls made;
49 rows), `identity.awk` (`is`, `is not`, `==`, `!=`: the 15 operands are 15 distinct objects of 15 distinct values, so the cell
(i, j) is true exactly on the diagonal; 4 programs), `matmul.awk` (`@` and `@=`: no built-in type implements them; 2),
`ordering.awk` (`<`, `<=`, `>`, `>=`: reals by value with bool, complex and dict and None and mixed kinds TypeError, a container
against itself; 4), `membership.awk` (`in`, `not in`: str substring, bytes with ints in range(256) and ValueError outside it, the
list's and tuple's elements by `==` so `True in [1, 2]`, a set searched by hash with a set operand taken as a frozenset,
unhashable list, set or dict operands TypeError for sets and dicts; 2). All 1,350 cells of the six comparison tables and 450 of
membership came out equal at the first run of the models, written before they were compared.

**The counts** (`bin/pyi golden`: 127 programs, 3,623 lines reviewed; the Reference 3,191, the library documentation 1,686,
implementation-defined 18, a line may rest on several): constructs 32 of 32, datamodel 45 of 45, builtins 17 of 17, paths 13 of
13, samples 6 of 6, ops 14 of 38 (boolean, cmp-is, cmp-is-not, cmp-eq, cmp-ne, cmp-lt, cmp-le, cmp-gt, cmp-ge, cmp-in,
cmp-not-in, matmul, aug-matmul, unary): 113 + 14 = 127 of 151, 381 of the 453 cases.

**Not reviewed: the other 24 ops tables** (add, sub, mul, truediv, floordiv, mod, pow, lshift, rshift, and, or, xor and the
twelve `aug-` forms except `aug-matmul`: 5,400 cells, 1,000 of them values). Their cells need exact arithmetic on 10**20 beside
doubles and complex numbers, Python's float `//` and `%`, two's-complement bit operations on negative and big ints, and the
printing of floats and complexes; awk's doubles are not enough (a model in Chez Scheme, whose exact integers and flonums would
do, makes the check depend on a Scheme host; gawk -M is not busybox awk). They stay on CPython's recorded runs at 0.5 (72
cases); DECISION 1 below.

**Implementation-defined lines**, none masked (the programs print `repr()`s and exception names only, GT8.6) and each named in
its review by `:impl`: set display order of small ints (`comprehensions.py`), `__del__` at once under reference counting
(`del.py`, two lines), `__builtins__`'s kind (`builtins-scope.py`), the side of the odd padding of `center` (`str.py`,
`bytes.py`, `bytearray-shared.py`), `bool.from_bytes`' construction through the subclass, `-0j`'s repr, `str.swapcase` of a
titlecase letter, the tie of `%05.1f` on 2.25, the limit of `1e400`'s literal, `RecursionError`'s depth, the code object's
argument counts, OrderedDict's and os.path's reprs in `samples/control.py`, and the spelling of an interpolation's expression
(`samples/literals.py`: `' n + 1 '` keeps its spaces).

**Samples of pyi** (the VM, `bin/pyi run` on goi, one process): 101 of the 127 golden programs (builtins, constructs, datamodel and 21 of
the ops tables; paths and samples not run) give the golden text and status: 100 equal, 1 not: `datamodel/del.py`, where goi calls
no `__del__` at all (`:finalization-goi`'s, 222/241). The compiled run of the same program (`bin/pyi check --aot --only
synth/datamodel/del.py`, the AOT cache warm) lacks `del 1`, the finalization of a temporary list's element: classed `:as-pyi`
against the golden text as against the record (queue 241's ground). Nothing else the golden texts catch that the record did not.

DECISION 1 (the other 24 ops tables): (a) leave them at 0.5, 72 cases (recommended: the gain is 0.2% of the judge); (b) a model
in Chez Scheme for exact integers, flonums and complexes, run at review time only (days).

## As built: the ops models, DECISION 1 decided (2026-11-21, the main host, py-ops-models-1121)

DECISION 1 above, decided by the user on 2026-11-21: the other 24 ops tables get a model of their own. `test/python/golden/models/ops.ae`
(Arbace on stage 0, not Chez: no Scheme host in the check) computes all 24 (5,400 cells) from the Reference and the library's numeric
rules, CPython 3.14.10's stated algorithms where neither fixes a digit or an object's identity (longobject.c's small ints,
floatobject.c's `_float_div_mod` and `float_pow`, complexobject.c's 3.14 mixed-mode operations and `_Py_c_pow`, pystrtod.c's `'r'`
repr, unicodeobject.c's `%` with a mapping argument): exact ints, IEEE doubles, int to float and int / int and each libm call rounded
once from exact values. Its 24 tables equalled CPython's recorded digests at the first run; they are the golden texts
(`test/python/golden/synth/ops/*.out`), reviewed per left operand in review-ops.edn; `bin/pyi golden` runs the model (models.tsv's
`ops.ae` rows) with a negative control per program (a changed rule). The judge: 151 of 151 programs by their own golden text (453
cases), none by the record; Python's independence 1 (doc/JOURNAL.md, 2026-11-21).

## As built: 248, the compiler's view: codegen.c's shapes and locations, the line table, line events (2026-11-09/10, the Mac VM, queue 248)

Route (a)'s tail (229's DECISION), ported from 3.14.10's Python/codegen.c, flowgraph.c and instrumentation.c where pyi's
compiler or the view were not CPython's shape; light samples only. 3.14 the default, 3.15 behind the switch as 238/239 left it.

- **compile.ae, the shapes** (the IR changes, the runtime runs them): starunpack_helper (a display or a call's positional
  part over `_PY_STACK_USE_GUIDELINE`, 30 values, is `BUILD_* 0` and an add per item at the display's location: the line
  events alternate between an item's line and the display's, `:tracing`); codegen_call_helper's ex_call with
  codegen_subkwargs (a `**` merged by DICT_MERGE: `f(**{'a': 1}, a=2)` now raises as CPython; it did not); codegen_dict's
  runs; a big constant tuple folded (fold_constant_seq_into_load_const, its loads without a line); comprehensions as
  codegen_sync_comprehension_generator (a filter jumps to if_cleanup, the loop's JUMP at the element's location, a later
  `for y in [f(x)]` bound without a loop); the parser's merged f-string constants (literals and debug texts one constant),
  ''.join over 30 values, a raw spec decoded per part of a concatenation; the module's conditional annotations
  (`__conditional_annotations__` a module cell, the set and `__annotate__` at start_location); a try body's POP_BLOCK on
  unwinding an uncovered NOP; the handler cleanup's SWAP under its handler; END_FOR's line in fl-nops (no line event
  after a loop CPython has not).
- **compile.ae, the locations**: a class docstring, the f-string constants' spans (from the replacement fields' and the
  quotes' positions), formatted values' CONVERT/FORMAT, a format spec, comprehension adds and yields, a generator
  expression's NO_LOCATION tail, `with`'s unwinding, DELETE_SUBSCR and DELETE_ATTR at the target.
- **go/arbace/py, the view**: fold_constant_seq_into_load_const (3.14.10), BINARY_SLICE at the subscript's location,
  CONVERT_VALUE at the field's, a method call's keyword names at the method's, `list(x for x in y)` not a decorator
  chain, a handler whose region lost every instruction reached and sized as SETUP_* does (`try: pass`), NOT_TAKEN's handler
  as 3.14's basicblock_addop leaves the slot (a removed instruction's handler, else none; 3.15 passes the jump's: gated),
  a threaded jump's handler none in 3.14 only. **code.go/tbobj.go**: a replaced `co_linetable` answers `f_lineno` and
  `tb_lineno` (None for no line), `co_lines`/`co_positions` end where the table does. **trace.go**: a FOR_ITER's target
  never starts a line (instrumentation.c: it is the POP_ITER after END_FOR).

**Measured** (bytecode-check, the 10-file sample of 238, 868 code objects; co_code / co_linetable / co_exceptiontable / all
five): 3.14 base 721 / 545 / 774 / 511, head 747 / 771 / 801 / 720; 3.15 base 736 / 545 / 798 / 519, head 767 / 771 / 813 / 737.

**The classes** (each test's own verdict, 3.14.10's tests by pyi's AOT, base 778a6333e against the head, method for method,
no method lost): `:dis` 10 to 0 (test_dis 109 to 119 of 145); `:tracing` 1 to 0 (test_sys_settrace 364 to 365);
`:code-positions` 1 to 0 (test_exceptions 97 to 98; test_code 7 to 8, test_empty_linetable too); `:fstring-text` 2 to 0, its
two tests passing at the base already (stale since 228). `:tracing` and `:fstring-text` dropped (no pattern, no test);
`:dis` and `:code-positions` keep their patterns for the methods the VM did not sample.

**Left**: f-string constants of an implicit concatenation whose middle parts carry no position keep the concatenation's
location (the tokens would tell); 3.14's NOT_TAKEN handler is a model of CPython's stale slot (exception tables differ in
67 of the 868); a coroutine's origin (`:code-positions`).

## As built: 249, buffer exports, sockets' and multiprocessing's rest (2026-11-09, the Mac VM, queue 249)

The deferred classes `:buffer-exports` (3), `:socket-gaps` (9) and `:multiprocessing` (2), ported from CPython 3.14.10's
Objects/memoryobject.c, Modules/arraymodule.c, Objects/bytearrayobject.c and Modules/socketmodule.c (the pinned
tarball); light samples only, each test by its own verdict.

- **The export count** (`go/arbace/py/buffer_export.go`): `BufferExports`, any exporter's `ob_exports` (`Exports()`,
  `Refuse`, `Acquire`/`Release` for a native holding a buffer over a call; reusable by another package's exporter, queue
  244's ctypes objects); memoryview's managed buffer (`_PyManagedBufferObject`): one export shared by a view's slices,
  casts, `memoryview(view)` and `toreadonly`, given back when the last of them is released, by `release()`, the
  context manager's exit or the view's death (241's `TrackFinalizer`, memory_dealloc's release). pyi has no reference
  counts, so a view dropped unreleased still counts until Go's collector finds it dead: a resize that finds a count
  first asks the collector (a collection, then the finalizers' drain as soon as one is queued; about 2 ms with pyi's
  heap at start) and refuses only for a view still held; a count found held is confirmed until it moves, so a view held
  over many refusals costs one collection. The collections have their own budget (a tenth of the elapsed wall time,
  and 200 ms, the moment at del's rule of 222); past it the resize is allowed (memoryview reads its exporter's bytes at
  each access, so a view never reads past them).
- **array** counts (arraymodule.c's refusals, issue #4509's early checks before anything moves, `frombytes` holding its
  argument's buffer, so `a.frombytes(a)` is refused as CPython's). **bytearray** has the same hooks (`_canresize` at
  every resize) but counts only under `PYI_BYTEARRAY_EXPORTS=1`: `_pyio`, pyi's io, makes temporary views of its own
  bytearrays and resizes them (`FileIO.readall`'s `memoryview(result)[n:]` then `result.resize`), so counting made every
  file read ask the collector, and under goi a view its registers still held failed an import's `get_data` with
  BufferError (found by test_cprofile's spawn child). bytearray's own fixes stay: `b *= n` in place (it rebound `b`),
  an extended slice assigned `b''` deleted. **PEP 688**: `__release_buffer__` when the last view of the export goes
  (also at the views' death, "Exception ignored in __release_buffer__ of %s"); **PickleBuffer** holds its export until
  `release()`. **memoryview**: its hash (memory_hash: the bytes' hash, the exporter hashed first with the view exported,
  gh-142664's re-entrant release refused with "memoryview has 1 exported buffer"; pyi hashed views by identity), an
  array's view writable (`readonly` False).
- **_socket**: `sendmsg` takes its data and ancillary data as tuples first (`PySequence_Tuple`: a part's `__buffer__`
  that clears the list changes nothing sent), with CPython's "must be an iterable" messages. **posix**: the functions of
  `int fd` pass a negative descriptor to the system call (OSError EBADF; `fsync`, `fdatasync`, `fchdir`, `fpathconf` keep
  fildes_converter's ValueError): asyncio's sendfile fallback reads `os.fstat(-1)`'s OSError as "not a regular file".
- **multiprocessing**: a script's `__main__` has `__spec__`, `__package__` and `__cached__` None and a `SourceFileLoader`
  (pymain_run_file's), so spawn's children find the parent's main script again (`init_main_from_path`; pyi's
  `__spec__` named `__main__` made spawn skip it: "module '__main__' has no attribute 'worker'"); `sys.stdin.close()`
  (the standard streams are closefd=False: flushed and marked closed). `test/python/libtest.py` runs its module only as
  `__main__`, as `libtest_run.py` already did. The start method forkserver forks its children in its server (`os.fork`:
  none in pyi, the user's decision of 2026-11-04); 3.14's default start method on Linux is forkserver (249's DECISION).

**The classes** (each test's own verdict; the classes' ids run alone by pyi's AOT, `.tmp/q249/t/runids.sh`, base 9aab26afd
against the head): `:buffer-exports` 3 to 0 (two already equal at the base, `test_sendmsg_reentrant_data_mutation`
served); the class keeps its `:match` for bytearray's uncounted exports (test_memoryio's nine, test_buffer's
`test_release_saves_reference_no_subclassing`). `:socket-gaps` 9 to 1 (test_asyncio.test_streams' seven already equal at
the base, `test_sock_sendfile_not_regular_file` served; left `test_socket_type`, `gc.is_tracked` of a type).
`:multiprocessing` 2 to 1 (spawn served; forkserver left).

**The samples** (`bin/pyi lib-test --aot`, base against head):

| module | base | head |
|---|---|---|
| test_array (893) | 819 equal, 74 classed (28 `:buffer-exports`) | 847 equal, 46 classed |
| test_memoryview (171) | 89 equal, 26 classed, 56 failing | 104 equal, 20 classed, 47 failing |
| test_buffer (95) | 5 equal, 81 classed, 9 failing | the same |
| test_memoryio (188) | 166 equal, 16 classed, 6 failing | the same |
| test_picklebuffer (9) | 5 equal, 3 classed, 1 failing | the same |
| test_socket (749) | 616 equal, 129 classed, 4 failing | 617 equal, 128 classed, 4 failing |

No method went from equal to not; the failing are the base's (test_memoryview's formats and shapes: memoryobject.c's
struct formats, multi-dimensional views and `cast` are not ported, PY6's minimum still; test_socket's four as 231 found).
`.tmp/q249/t/check249.sh ROOT` (67 lines against python3, `PYI_BYTEARRAY_EXPORTS=1`; its control a mutated reference):
PASS on the head, FAIL at the base. On goi (`bin/pyi lib-test test_array`, the runtime interpreted): 819 equal at the base,
847 at the head, as compiled. But a temporary view of an array that goi's registers keep is not found dead: `bytes(memoryview(a));
a.append(x)` in a loop is refused on goi (`.tmp/q249/t/tmp249.py`, a 380 ms collection that finds the view held), allowed
compiled (2 ms each). test_cprofile's spawn test passes, its child on goi. test_multiprocessing_spawn's 231 sample (13
methods): 8 equal, 3 skipped (sharedctypes), 2 errors (`__objclass__`), base and head alike. `bin/pyi runtime vet` ok;
`bin/pyi table-check --configs tamago/amd64,tamago/arm64` ok.

DECISION 1 (a bytearray's exports): (a) uncounted by default, `PYI_BYTEARRAY_EXPORTS=1` counts (as built; recommended: _pyio's
own temporary views of its bytearrays would cost a collection per file read and fail imports on goi); (b) counted by default
once pyi's io is a native `_io` (FileIO, BufferedReader and BytesIO in Go, taking no Python views), a task of its own.
DECISION 2 (3.14's default start method forkserver, which forks): (a) keep it, the tests classed (`:multiprocessing`), a program
calls `set_start_method('spawn')` (as built; recommended: the user's no-fork decision); (b) a pyi overlay of
`multiprocessing/context.py` choosing spawn when `os` has no `fork` (the first patch of the pinned Lib, an overlay
mechanism to build).
## As built: 244, ctypes over Arbace's own C: the design, and libffi's part on ci (2026-11-09, the Mac VM, queue 244)

The brief: `:ctypes` and `:testcapi` (CPython's `_test*` modules), after OCaml's C API on ci (the main host's `ocaml-compl`,
doc/ocaml/OCAML-SURVEY.md "As built: the C API on ci", read only: stubs run by ci against clean-room `caml/*.h`, values copied at
the boundary, `cnodes.ae --program`). This section is the design (asked first) with its DECISIONs, then the slice that holds under
every option: libffi's part over ci, CPython's own `_ctypes_test.c` built by ci against a clean-room `Python.h`, both run.

### What the two classes hold (sampled on the base 2b5c0c075, each test's own verdict)

`bin/pyi lib-test-all test_ctypes test_capi test_audit test_sysconfig` (the compiled runner, 2 jobs):

| module | methods | `:ctypes` | `:testcapi` | other |
|---|---:|---:|---:|---|
| test_ctypes | 620 | 620 (the package does not import: `No module named '_ctypes'`) | | |
| test_capi | 1,195 | 4 | 1,116 (each submodule skipped: `No module named '_testcapi'`, `_testlimitedcapi`) | extension-modules 71, cpython-only 3, skipped-by-test 1 |
| test_audit | 30 | 1 | 1 | 21 failing at the base (audit-tests.py's subprocess runs; not this task's), 1 pass |
| test_sysconfig | 39 | | | 39 failing at the base (`No module named '_sysconfig'`; not this task's) |

The brief's 13 and 10 are the main host's record's rows; the sample's methods are 625 and 1,117. The 10 named `:testcapi` tests
outside test_capi need `_testcapi.raise_SIGINT_then_send_None` (test_generators), `_testcapi`'s limits (test_pyexpat) and
`_testinternalcapi`'s interpreter functions (test_interpreters' 8).

### The pieces, and what exists

- **ci** (`go/arbace/ci`, `go/arbace/crt`, on this branch): a C program over one emulated address space. Its calls are libffi's
  missing half already: `Prog.Call(t, f, []Arg)` takes typed values (an integer, a floating value, a wide integer, an aggregate's
  address and size, copied into the callee's frame), a variadic call writes the rest into the va area, an aggregate result goes
  through a hidden first argument (`RetM`), and a `Func` may be a Go native, whose address C calls like any function pointer.
  So no register classification (libffi's x86-64 `examine_argument`) is needed: ci's convention is its own.
- **arbace/clib** (208): one ci program per library per process, musl initialized, faults ending the process as CPython's would.
- **bin/pyi-clib** (208): a C library built by ci's builder (repr/c, `arbace.c.check`, `arbace.c.build`, musl 1.2.6's forms) into
  one node program, committed as Go forms and verified byte for byte (208's DECISION 2 (a)).
- Not on this branch: the main host's C target `c/` (Arbace's own sysroot, `ocaml-compl`'s `cnodes.ae` reads with it) and
  `arbace.c.runtime` (musl prebuilt for linking). Nothing of them is needed: bin/pyi-clib links musl's forms as 208 does.
  Nothing of `ocaml-compl` is vendored; its model is followed in two points: clean-room headers from the documentation, and a
  C half of the runtime calling natives in Go.

### The design

1. **The C world.** One ci program per process, over one address space: musl's libc and libm, `_ctypes_test`, and (with
   DECISION 3) the C API's C half, linked as one node program, `go/arbace/clib/ctypestest` (bin/pyi-clib's). Every ctypes buffer
   lives in that address space, so `addressof`, `from_address`, `cast`, `string_at`, `memmove` are addresses C dereferences as
   is, and a pointer stored by C into a ctypes structure is a pointer ctypes reads back. A static link keeps only what is
   reached: the glue `tools/pyi/clib/ctypestest/pyctypes_test.c` names the libc functions a ctypes program may look up
   (`pyctypes_libc`; the ones Lib/test/test_ctypes and Lib/ctypes call). `CDLL(None)`, `CDLL("libc.so.6")`, `find_library("c")`
   and `find_library("m")` are this world, `CDLL(_ctypes_test.__file__)` too; dlsym is the program's names (DECISION 2).
2. **libffi's part** (`go/arbace/pyctypes/ffi.go`, built here): `ffi_type` (`Type`: the kinds, sizes and alignments of ci's
   target, x86-64 LP64; an aggregate laid out by libffi's `initialize_aggregate`, which is the System V rule ci's builder
   uses), `ffi_prep_cif`/`ffi_prep_cif_var` (FFI_BAD_TYPEDEF, FFI_BAD_ARGTYPE for a float or a narrow integer among the
   variable arguments), `ffi_call` (`World.Call`: the arguments' bytes as `avalue`, the result into `rvalue` with ffi_arg's
   widening; an aggregate copied below the C thread's stack pointer, as a caller's frame holds it) and closures
   (`ffi_prep_closure_loc`: a native `ci.Func` whose address C calls; freed ones reused). The interface is libffi's, bytes in
   and out, so a port of `callproc.c` and `callbacks.c` calls it as CPython calls libffi.
   `long double` is `double` in ci (doc/c/SPEC.md §12.5, CS35): `c_longdouble` is 8 bytes, as on a target whose long double
   is double; tests that assume x87's 80 bits go to a class citing CS35.
3. **`_ctypes`** (DECISION 1): a lazy package `arbace/pyctypes` (`lazyModules["_ctypes"]`, as `_bz2`), CPython 3.14.10's
   `Modules/_ctypes/` ported statement for statement over pyi's objects: `_ctypes.c` (the simple types, `Structure`, `Union`,
   arrays, pointers, function pointers, the metatypes `PyCSimpleType`, `PyCStructType`, `PyCPointerType`, `PyCArrayType`,
   `PyCFuncPtrType` and their `StgInfo`), `cfield.c` (the fields' getters and setters, bitfields, byte orders), `stgdict.c`,
   `callproc.c` (ConvParam, the call, `errno`'s swap, `addressof`, `byref`, `alignment`, `sizeof`, `resize`, `memmove`/`memset`'s
   addresses as closures), `callbacks.c` (CFUNCTYPE). Lib/ctypes is the tarball's (3.14 lays structures out in Python,
   `_layout.py`). Memory: ctypes's own allocator over a region of the world's address space (no C call per object, freed by the
   finalizer queue, 218's), small objects as CPython's inline `b_value`.
4. **The C API** (DECISION 3): extension modules built by ci against Arbace's clean-room `Python.h`
   (`tools/pyi/capi/include`, built here for `_ctypes_test`'s part: the stable ABI's shape, written from docs.python.org/3.14/c-api,
   no CPython header code). A `PyObject *` is the address of a proxy in the world's memory with the documented head (`ob_refcnt`,
   `ob_type`), one proxy per live object (identity kept), the functions natives in Go over pyi's objects. This gives
   `_ctypes_test`'s module functions, `ctypes.pythonapi`, `py_object` arguments (`_testfunc_pylist_append` appends to the
   caller's list), and the `_test*` modules (DECISION 4).
5. **The back ends.** On goi, ci is interpreted by goi (208's DECISION 1 (a): `arbace/ci` and `arbace/crt` in goi's table at the
   re-seed after Python's tag); compiled, pyi's AOT links cf2g's Go of the world (`bin/pyi-clib aot ctypestest`, as bzip2's).
   The world's node program is 1.44 MB: its cf2g Go about the size of liblzma's, which builds.

### DECISIONs

```
DECISION: 244-1, how _ctypes is built
- (a) Modules/_ctypes ported to Go over pyi's objects (arbace/pyctypes), libffi's part this slice's ffi.go over ci (recommended): as
  _bz2/_lzma/_sqlite3's glue; about 11,800 lines of C, 5-8 tasks (types and memory 2; calls and callbacks 1-2; pythonapi with 244-3;
  the rest); no dependence on the C API for ctypes itself
- (b) CPython's _ctypes C itself on ci over the C API (244-3 at the full API: 416 distinct Py*/_Py* names, its pycore_* headers,
  metatypes in C deriving from type with StgInfo in the type object, PyType_FromMetaclass): ffi.go as libffi's natives; the C API
  layer becomes a cpyext, weeks before the first test passes, every ctypes operation crossing the boundary
```

```
DECISION: 244-2, what CDLL loads
- (a) one world, a fixed link (recommended): musl's libc and libm, _ctypes_test and the C API's C half, one committed node program
  (bin/pyi-clib, verified); CDLL(None), libc, libm and _ctypes_test's file name it, any other name OSError (dlopen's message);
  data symbols (in_dll: an_integer, my_eggs, Py_OptimizeFlag) by the names the node program's static commands already carry
  (their fifth field, which ci's loader ignores: read by pyctypes from the text, the statics' addresses from the Prog): small,
  no ci change
- (b) a loader in ci for a second node program into a running Prog (functions and statics renumbered, undefined names resolved
  against the loaded ones): CDLL of any library ci builds (a user's C source); a ci
  change for the C crop (the main host's), days; can follow (a) without undoing it
```

```
DECISION: 244-3, PyObject * at the boundary
- (a) handles (recommended): the proxy's documented head in the world's memory, one per live object, a Go table both ways, the API's
  functions Go natives; Py_IncRef/Py_DecRef keep ob_refcnt, the table holds the object strongly while it is above 0; the limited API
  first (_ctypes_test, pythonapi, _testlimitedcapi), the full API's struct macros (PyTuple_GET_ITEM, PyBytes_AS_STRING, ...) by
  materialized bodies later; about 2 tasks for the limited core
- (b) values copied at the boundary (OCaml's oicapi): breaks identity and mutation (PyList_Append on the caller's list,
  _testfunc_pylist_append), fit only for immutable values
- (c) no C API: _ctypes_test's four module functions in Go, pythonapi absent (its tests a class by design), the _test* modules by design
```

```
DECISION: 244-4, the _test* modules
- (a) _testinternalcapi by design (a class :testinternalcapi, :design: CPython's internals, inline values, hamt, code var counts,
  interpreter ids, not an API); _testlimitedcapi (31 files written to the limited API), _testcapi, _testmultiphase, _testsinglephase,
  _testbuffer, _testimportmultiple built from their C sources by ci over 244-3 (a), file by file as the layer grows, a file whose
  functions the layer lacks left out and its tests kept in :testcapi (recommended)
- (b) all by design: the C API is CPython's interface, pyi is not CPython (one class :testcapi, :design)
- (c) Go stand-ins for the few functions the named tests use (raise_SIGINT_then_send_None, the limits): quick, not CPython's code
```

DECISION 1 is costly to reverse and changes everything after it, so the brief's rule held: the design, and the slice none of the
options undoes (libffi's part is (a)'s callee and (b)'s natives; `_ctypes_test` and the header are needed by every option).

### As built: the slice

- `go/arbace/pyctypes` (package `arbace/pyctypes`, not yet a module: `_ctypes` is not registered, so nothing a program imports
  changes): `ffi.go` as above; `ffi_test.go`, 21 calls of `_ctypes_test`'s functions and musl's through ci, each case's want
  the function body's arithmetic by hand: `_testfunc_{i,f,d}_bhilfd`, `_testfunc_q_bhilfdq`, narrow results widened by their
  signedness (`tf_b`, `tf_B`, `tf_h`, `tf_I`, `tf_Q`, `tf_bf`), aggregates in (`_testfunc_byval`), out through the hidden
  pointer (`ret_2h_func`, `ret_8i_func`) and by value unchanged for the caller (`_testfunc_large_struct_update_value`),
  musl's variadic `sprintf`, closures C calls back (`_testfunc_callback_i_if`, `_testfunc_callback_with_pointer`, an aggregate
  argument `_testfunc_cbk_large_struct`, qsort's comparator `my_qsort`, two through a struct of pointers
  `_testfunc_callfuncp`), `get_an_integer`; `TestPrepCIF` (the refusals, a layout); the negative control
  `TestFFINegativeControl` (each case judged against the next case's want must fail).
- `go/arbace/clib/ctypestest` (generated, committed: 1,440,296 bytes of node program, 1.5 MB of forms): `bin/pyi-clib build
  ctypes_test` from `tools/pyi/clib/ctypestest/` (lib.conf: the CPython tarball test/python/corpora.edn pins, read from
  `REPR_PYTHON_SRC`'s cache; `pyctypes_test.c`, the libc table and the main naming `__init_libc`). bin/pyi-clib: lib.conf's
  `cache_var` (a tarball another pin fetches), `members` (the members extracted; `tar -xf`, any compression) and
  `root_includes` (directories of the checkout on the include path and in the key); the other libraries' keys do not move.
- `tools/pyi/capi/include/Python.h`, `pyconfig.h`: the clean-room header's first slice (the object head, `PyMethodDef`, the
  module definition and slots, the reference counts as `Py_IncRef`/`Py_DecRef`, the twelve functions `_ctypes_test.c` calls).
  The functions are declared, not yet given: the world links (a call would fault: "undefined function"), the module's Python
  part waits on 244-3.
- bin/pyi's runtime verbs: `print` prints `clib/ctypestest` and `pyctypes`, `read` and `check` read `pyctypes` back, `vet` vets
  and tests it. LICENSE.md: the world's node program (the PSF's `_ctypes_test.c`, musl's MIT), the headers and `pyctypes`
  (Arbace's own).
- bin/pyi-clib hashes itself into every library's key, so its change moved the four committed keys: `Key` in
  `go/arbace/clib/{bzip2,xz,zstd,sqlite}/nodes.ae` set to `bin/pyi-clib key LIB` (the node programs unchanged; bzip2's and
  liblzma's `verify` byte for byte on the VM, zstd's and SQLite's the main host's).
- The classes: unchanged (`:ctypes` 625 and `:testcapi` 1,117 methods in the sample; the brief's 13 and 10 rows), as expected
  of a slice with no module.

## As built: 244, after the decisions: `_ctypes` (2026-11-09, the Mac VM, queue 244)

**Decided (the user, 2026-11-09, relayed by the coordinator):** 244-1 (a) Modules/_ctypes ported to Go over ffi.go;
244-2 (a) one fixed linked world (libc, libm, `_ctypes_test`, the C API's C half); 244-3 (a) handles, a proxy per
live object, the limited API first; 244-4 (a) `_testinternalcapi` by design, the other `_test*` modules from their C
sources on ci over the handles, file by file.

**Built:**

- `go/arbace/pyctypes`, the module `_ctypes` (lazy, `lazyModules["_ctypes"]`, as `_bz2`), CPython 3.14.10's
  `Modules/_ctypes` over pyi's objects:
  - the metatypes `PyCSimpleType` (with `__ctype_be__`/`__ctype_le__`, create_other_endian_type), `PyCStructType`
    and `UnionType` (their `_fields_` laid out by Lib/ctypes/_layout.py, the CField objects its; MakeAnonFields;
    `_fields_ is final`), `PyCArrayType` (and `TYPE * N`, cached), `PyCPointerType` (`set_type`,
    `__pointer_type__`), `PyCFuncPtrType` (`_argtypes_`, `_restype_`, `_flags_`): built-in subclasses of `type`
    whose `__new__` makes the class by `type.__new__` and then its StgInfo (a table by class);
  - `_SimpleCData`, `Structure`, `Union`, `Array`, `_Pointer`, `CFuncPtr` over `CData` (CDataObject: b_ptr an
    address of the world, b_objects/KeepRef/unique_key, b_base, b_index, b_length), cfield.c's field descriptors
    (the integers native and swapped with bit fields, `?`, `v`, `d`, `g`, `f`, `c`, `u`, `U`, `s`, `z`, `Z`, `P`,
    `O`), PyCData_get/PyCData_set, the from_param family as _ctypes.c has it, `from_address`, `from_buffer`,
    `from_buffer_copy`, `in_dll` (data symbols by the node program's static names), pickling (`__reduce__`,
    `__setstate__`, `_unpickle`);
  - calls (PyCFuncPtr_call, ConvParam, GetResult, `errcheck`, `use_errno`'s swap) through ffi.go's `World.Call`;
    callbacks (CFUNCTYPE of a callable) as closures, exceptions reported unraisable;
  - `dlopen`/`dlsym` of the world (244-2 (a): `None`, `libc.so.6`, `libm.so.6`, `_ctypes_test.__file__`; any other
    name OSError), `sizeof`, `alignment`, `byref`, `addressof`, `resize`, `get_errno`/`set_errno`, `buffer_info`,
    `PyObj_FromPtr`; memmove and memset as libc's own addresses, `string_at`, `wstring_at`, `cast`, `memoryview_at`
    as natives of the world taking and giving PyObject * handles;
  - `py_object` and PyObject * arguments as handles (244-3 (a)'s first part): a proxy (ob_refcnt, ob_type) per
    object in the world's memory, kept for the process (refcounts are the next part's).
- `_ctypes_test`, its module part: `__file__` (the world), `get_last_tf_arg_s/u` (the C statics), `func`, `func_si`;
  `get_generated_test_data` waits on the C API (244-3's next slice: it is C over Python.h).
- py: `builtins_ctypes_api.go` (the lazy modules, `ExtTypeNew`, `ExtWrapInstance`, `ExtRegisterMetaNew`); a REQUEST
  in class.go (`type(name, (Structure,), ns)` calls PyCStructType's new, as CPython's type_new does when a built-in
  metatype with its own tp_new wins).
- pyi's AOT links pyctypes and the world compiled by cf2g (`bin/pyi-clib aot ctypestest`); bin/pyi's runtime verbs
  and keys cover them. The world's library is `ctypestest` (goi maps a package path's `_` to a namespace's `-`).

### 244-3's first slice and the port's smaller causes (2026-11-10, the Mac VM, queue 244)

- **The C API over the handles** (`go/arbace/pyctypes/capi.go`):
  - The limited API's functions are Go natives of the world, a function the node program declares and does not define being the native of its name: references, ints, floats, str, bytes, lists, `PyArg_ParseTuple`'s format units, the error indicator (`PyErr_SetString`, `PyErr_Format`, `PyErr_Occurred`), `PyOS_snprintf`, `PySys_Write*`/`Format*`, `Py_GetVersion`.
  - A proxy carries `ob_type` (its type's handle), so `PyUnicode_Check` works as a macro over `PyType_GetFlags`.
  - The singletons and the `PyExc_*` pointers are defined in the world's C half, `tools/pyi/clib/ctypestest/pycapi.c`. An undefined extern datum links at address 0, and compiled code copies the static addresses at its setup, so they cannot be bound later.
  - `_ctypes_test` is made from its own `PyInit__ctypes_test` by multi-phase initialization, its `m_methods` called as METH_NOARGS, METH_O or METH_VARARGS.
  - A `pythonapi` call (FUNCFLAG_PYTHONAPI) raises the indicator.
- **The port's smaller causes**, as `_ctypes.c` has them:
  - function pointers made from memory get their state;
  - `(name, dll)` is accepted from a tuple subclass;
  - from_param in full; CArgObject's repr;
  - the musl library names are names of the world;
  - a c_char_p's kept object is the bytes itself;
  - callbacks' unraisable messages and the leak warning;
  - the 1024 argument limits;
  - `_abstract_`; a structure that contains itself is refused; a type is final once it is a field's type;
  - `__pointer_type__` as the metatypes' getset;
  - paramflags (`_validate_paramflags`, `_build_callargs`, `_build_result`);
  - pickling's instance `__dict__`;
  - the metatypes' tp_new is type.__new__ alone and their tp_init makes the StgInfo.
- **Found (a REQUEST for the C crop):** ci's builder ignores `#pragma pack(push, n)` inside a function body (16 bytes where gcc gives 9), and the `ms_struct` attribute.
  - `_ctypes_test_generated.c.h` declares its Packed structs inside a function, so test_generated_structs' Packed1-3 and test_bitfields' msvc mode differ there.
  - Probes: `.tmp/q244/cprobe/p1.c` and `p2.c`. At file scope ci gives 9 bytes, as gcc does.
- **By design** (no fix in this branch):
  - `from_address` of a Python buffer's address: array's memory is Go memory, not the world's.
  - The issue18060 tests rely on an ABI coincidence (a struct of two doubles where atan2 takes two doubles); ci's convention is its own.
- **Left:**
  - the buffer export of ctypes objects (PEP 3118; queue 249 owns memoryview's export protocol, so it comes last);
  - the C crop's layouts above;
  - the swapped and aligned structure cases;
  - a few messages.

## As built: 250, the object model's edges: captures, BLAKE2's parameters, str.format, t-strings, _ast's keywords, the child's start (2026-11-09/10, the Mac VM, queue 250)

The deferred classes `:pattern-captures`, `:hashlib-gaps`, `:str-protocol`, `:ast-objects` and `:error-display`, ported
from CPython 3.14.10's own code (the pinned tarball) onto pyi; light samples only.

- **compile.ae, pattern matching** (Python/codegen.c's pattern_context): a capture is no longer stored as its sub-pattern
  matches. codegen_pattern_helper_store_name keeps the value on the stack, rotated beneath what the pattern still needs (the
  depth above the case's subject, `*store-base*`; 3.10's ROT_N as SWAPs), codegen_pattern_or gives each alternative stores
  of its own above its copy of the subject and reorders a matching alternative's captures into the first's order (control)
  before rotating them beneath the outer pattern's items, and codegen_match_inner stores the names, in pc->stores' order,
  only once the case's pattern has matched (before its guard). A failed alternative or case binds nothing; the fail ladder
  pops the kept captures with the rest. A mapping's `**rest` is kept the same way.
- **BLAKE2** (Modules/blake2module.c, Argument Clinic): leaf_size and node_offset by `_PyLong_UnsignedLong_Converter` and
  `_PyLong_UnsignedLongLong_Converter` (longobject.c's UNSIGNED_INT_CONVERTER: `__index__`, a negative int a ValueError
  "Cannot convert negative int", beyond 64 bits an OverflowError naming the C type), before blake2's own range checks.
- **str.format** (Objects/stringlib/unicode_format.h, whole): MarkupIterator_next and parse_field (a `[` skips to its `]`, a
  spec to its matching `}`), get_integer (Py_UNICODE_TODECIMAL's digits, Py_ssize_t's overflow), the FieldNameIterator,
  field_name_split with AutoNumber, get_field_object, do_conversion's messages, build_string's recursion depth and
  do_markup's order: one field parsed and rendered before the next is read, so `"{0[}".format()` is the parser's ValueError,
  not an IndexError. `_string.formatter_parser` and `formatter_field_name_split` use the same iterators (read whole). A str's
  spec with a space sign: "Space not allowed in string format specifier" (format_string_internal).
- **t-strings** (Objects/templateobject.c, interpolationobject.c): `Template(*args)` (strs concatenated, an empty str
  between interpolations and at either end, no keywords), `Interpolation(value, expression='', conversion=None,
  format_spec='')` with _conversion_converter, and their `__reduce__` (`string.templatelib._template_unpickle`; the class),
  so both pickle at every protocol.
- **_ast's keywords** (Python/Python-ast.c): a `**` keyword that is no str. In CPython a C type's tp_call takes the kwargs
  dict itself; only vectorcall's _PyStack_UnpackDict refuses "keywords must be strings". pyi's calls carry str names, so
  CALL_FUNCTION_EX (eval loop and compiled loop) offers such a dict to `kwDictCall` before its TypeError: a node class whose
  `__init__` is AST's is built and ast_type_init runs over the dict's keys as they are (== against the fields: a FakeStr
  equal to a field is "got multiple values"; another key warned of, then setattr's TypeError), and a node's `__replace__`
  refuses the key by its repr (ast_type_replace). Any other callable keeps the TypeError.
- **The child's start** (Python/initconfig.c's sys_path_0, Objects/moduleobject.c, Python/ceval.c): `bin/pyi python`
  passes -P and -I as PYI_SAFE_PATH; the runtime's start puts no first entry under -P, "" for -c and stdin, the working
  directory for -m, else the script's directory, and sys.flags.safe_path is the command line's (bin/pyi run's programs and
  the check's driver keep True, the reference driver's -P; -c's own override of sys.path[0] applies only without -P).
  _PyModule_IsPossiblyShadowing returns 0 under -P;
  module_getattro's and import_from's stdlib test is PySet_Contains(sys.stdlib_module_names, __name__) on the attribute as
  it is now (None or deleted: no hint) with `__name__` itself, so a str subclass with `__hash__ = None` raises set's
  TypeError. set's `__contains__` names an unhashable key as set_contains_key does.

**The classes** (each test's own verdict, base b10b0b886 against the head, the 20 methods the five classes named run by
`bin/pyi check --only`): `:pattern-captures` 2 to 0, `:hashlib-gaps` 2 to 0, `:str-protocol` 5 to 0 (test_format's last
step, the space sign, passes run alone after the module run), all three dropped; `:ast-objects` 3 to 1 (the two non-str
keyword tests pass; test_exceptions' two offset tests passed at the base already); `:error-display` 5 to 2 (test_import's
three shadowing tests pass; test_encodings passed at the base already). Whole modules on the head, method for method
against the base where both ran (test_import, test_str, test_format): no method lost.

**Left**: test_ast's test_pickling (300 s), test_traceback's test_encoded_file (15 children) and test_import's
test_script_shadowing_stdlib (14 children): each child's start under goi is about 30 s, past the driver's limit.
## As built: 259, the classes ready for night 11's eval and AOT records (2026-11-09/10, the Mac VM, queue 259)

Light: samples and probes on the VM at 6ee13e5ae (244's head); nothing recorded committed. Night 11 (the main host, amd64)
records `bin/pyi check` (eval, Lib/test) and `bin/pyi check --aot` (AOT, the 151 programs); run.sh commits Python's
records only when `bin/reap` shows 0 problems, so every non-pass must be in a class, and the cells' structure must hold.

- **bin/reap and the own verdict** (a REQUEST commit for the main host, `test/reap/main.ae`): the eval cell held the record
  to the references' non-excluded `:outcome` ids, the ref verdict's methods. The record is the own verdict's since 234:
  the inventory (every reference id, `:excluded` ones too) with the tests pyi's run reported. So the night's record was a
  problem by construction ("N cases of check-lib-test-linux-amd64.edn not in [refs]": on the VM's harvested runs 94 such
  methods in 163 modules). A `:results` source with `:py-ref-modules` whose file says `(verdict own)` is now held to the
  inventory of its modules, its other ids not extra cases (t1: a fabricated own record with `:excluded` ids and a
  pyi-only test has 0 case problems, the control with an id dropped 1; the parent 5 extra).
- **The import probe**: every module of the universe (529) imported on pyi as the driver imports it (regrtest's setup
  first) and its tests loaded: 413 load, 107 do not import (skips at import: Windows, the GIL, test_gdb, absent C modules),
  7 have a submodule the loader skipped. Two causes were in no class: `test.support.requires` at module level raises
  `ResourceDenied` (SkipTest's subclass; the driver writes "import ResourceDenied: ...", eight modules) and `_sysconfig`
  (test_sysconfig): `:skipped-by-test` and `:c-modules-deferred` take them.
- **The id probe**: the loader's ids under pyi against the inventory, every module that imports (420 with an inventory):
  737 inventory methods are never loaded (the night's "not-run"), 703 already classed (test_decimal's C variants); the
  other 34 by cause: builtins' and itertools.batched's doctests (`:builtin-docstrings`), test_clinic's classes defined
  only with `_testclinic` (`:c-modules-deferred`), test_code's `CoExtra` under `check_impl_detail(cpython=True)`
  (`:cpython-only`), and 8 reference ids that name a subtest (`:subtest-ids`, new, by design).
- **New classes** (divergences.edn): `:memoryview-formats` (test_memoryview's 47: memoryobject.c's formats, shapes and
  cast), `:buffer-protocol-python` (test_buffer's 9: PEP 688's `__buffer__`/`__release_buffer__` from Python),
  `:io-memory-c` (test_memoryio's 6 C-class methods), all from 249's run at its head, unclassed there; `:subtest-ids`;
  `:runner-limits` (a test past the runner's 300 s, a module's rest past its 3600 s: the row's text is its cause, no
  verdict; DECISION 1, (a) provisional). Narrowed into existing classes: TestLinuxAbstractNamespace's 3 (`:socket-gaps`),
  testMakefileCloseSocketDestroy (`:finalization`, getrefcount), the ctypes users outside test_ctypes that fail
  (`:ctypes-port`: test_memoryview's test_ctypes_cast, test_threading's test_finalize_running_thread, test_audit's
  test_ctypes_call_function; a sample of the 53 such methods: 49 of 94 pass with the controls' subjects).
- **The families' sample** (`bin/pyi check --fresh`, 13 language-core and stdlib-tests modules whose saved runs held
  unclassed failures): 2,746 methods, 2,433 pass, 227 classed, 86 in no class, the same 86 as queue 234's runs of
  2026-11-07 (the families' rotation was never recorded, so nothing had classed them): `:builtin-functions-core`,
  `:set-core`, `:deque-core`, `:itertools-core`, `:functools-core` by module and cause, `:runtime-panics` (three Go
  panics, a bug each, by name, no pattern), seven cycles in `:finalization-cycle` (read: their probable cause). Queue 250's leftovers (its report's
  Lows): test_cmd_line_script's 28 in `:pyi-main-module` (bin/pyi python's prologue in place of runpy; -W ignored), 4 in
  `:runner-limits`, test_format's 6 in `:format-messages`.
- **Never-run modules** (about 366 of lib-test-all's 425 had no run under pyi on the VM): three random samples of 12, 10
  and 6 such modules, `bin/pyi check` (2,553 methods): 2,134 pass, 261 classed, 158 in no class (6%), now classed by cause:
  `:pyi-command-line` (bin/pyi python's options and environment variables taken and ignored: test_cmd_line, test_warnings'
  -W and PYTHONWARNINGS tests), `:warnings-c`, `:regrtest-children` (test_regrtest's regrtest in pyi children, not
  analysed past their cut tracebacks), `:inlined-comprehensions`, `:profile-events`, `:struct-sequences`, `:annotate-rest`,
  `:pprint-dispatch`, `:float-core`, `:sequence-index-ops`, `:compiler-shapes`, more `:runtime-panics`. At that rate the night's run holds some hundreds to a few thousand methods
  in no class in the modules not sampled: the morning script lists them with their likely class.
- **AOT** (divergences-aot.edn's first class, `:finalization-goi`, synth/datamodel/del.py): the night runs pyi on goi
  beside the compiled run; del.py's compiled run lacks 'del 1' and pyi on goi prints neither, so the rule `:as-pyi` does
  not take it and the judge's `:differs` is `:failed`, which the AOT cell refuses whatever its class (the essential
  `--no-pyi` run never saw it). The night's command on the VM, `bin/pyi check --aot` with pyi's runs: 151 programs, 150 pass,
  1 classed, 0 failing, no `:pyi-differs`; its record copied as the amd64 one, `bin/reap` gave AOT 150 / 0 / 1 and the
  eval cell's synthetic sources 150 / 1 / 0, the three stale `:until`s the only Python problems.
- **The morning script** (queue 259's report, for the main host's `.tmp/`): `py-unclassed.sh` lists every non-pass of
  the night's eval run (its `results.tsv`: the record leaves an unclassed method out) and bin/reap's Python problems,
  each with its likely class (the class most methods with the same reason are in, a rule by cause, the module's
  class), and `--control` appends a fabricated unclassed row that must be listed; then `bin/pyi check --judge RUN
  --record` writes the record from the night's run, no run again.

## As built: 258, the gate fragments `test/gate/crops/python.edn` and `python-oracle.edn` (2026-11-10, the Mac VM, queue 258)

GT9's write-ahead `python.edn` (main) with queues 212, 213, 235, 240 and 247's drafts, finished as one fragment and an
oracle-tier fragment, committed on this branch (crops-score reads them at the ref) and given to the main host as three patches
against main (queue 258's report): the fragments, the splice into `test/gate/steps.edn` with `test/gate/crops-test.bash`'s
expectations (Python gated, its chains beside the serial run), and `test/crops/independence.edn` (247's facts with every
stage's `:steps` and the stub runs of read and AOT).

- **Essential, chain `python-essential`** (each check at most 3 GiB, none on `:python`, no lock): `python-read-essential`
  (spec-check, samples, both round trips of samples, lex-check and parse-check --full of samples and lex-errors, from the
  committed records), `pyi-essential` (GT9's `python-front-essential` with 247's `pyi-golden`: gen --check, golden,
  scope-check, compile-check --full of samples, synthetic and errors), `pyi-aot-essential` (essential-aot.txt's 21 programs
  compiled and on the loop), `pyi-c2j`. pyi's AOT runtime is the `:build` step `pyi-aot-runtime`, run only when a check
  that runs needs it, so `pyi-aot-essential` runs on the cached runtime.
- **`--full`**: chain `python-read` (python-front whole; python-roundtrip and python-native rotated a seventh a night over
  the night's results files) and chain `pyi-aot` (check --aot --no-pyi whole, judged by the golden texts and CPython's
  recorded runs; pyi on goi rotated; the wheels), both light, so `gate.crops concurrent` runs them beside the serial run
  (GTS216 (a)); chain `pyi-full` (runtime check 5 GiB, Lib/test by its own verdict 6 GiB, rotated by module) serial.
- **Oracle tier** (`python-oracle.edn`, `:phase :oracle`, the only steps on `:python`): python-records-verify,
  python-roundtrip-oracle, pyi-oracle. `:host-ids :python` and `:crop-pins :python` (`bin/repr-python pins --check`: 15 s,
  ok on the VM) are steps.edn's already.
- **Measures** (arm64, the VM, busybox `time -v`, python3, python and python3.14 stubs exiting 99 first on PATH, the
  stubs' log empty): python-read-essential 16 s, 0.44 GiB; pyi-essential 10 s, 0.43 GiB; pyi-aot-essential 41 s warm
  (60 s the first), 0.93 GiB; pyi-c2j 25 s, 2.9 GiB; the chain about 92 s warm. pyi-aot-runtime cold (an empty Go cache)
  171 and 266 s, 4.0 GiB in one process, 4.7 GiB the tree (MemAvailable's drop): `:mem-gib 6`, not `:heavy` (queue 235's
  13.5 GB was a Go cache past its trim).
- **Proved** in a scratch merge of main (2a72c94aa) and this branch, stage 0 the VM's: `gate.crops check` 0 problems;
  `bin/gate --plan`, `--full --plan`, `--nightly --plan` (the rotations' slices) and `--oracle --plan` without a table
  error once the night's nine files exist (fabricated in the scratch); without them the planner names exactly those nine
  (five packs' pins, four universes); crops-test, costs-test, gate-key-crops-test and plan-logic-test pass on the spliced
  table (crops-test with its expectations updated); crops-score: gate item 100% (4 essential, 8 full, 3 oracle), every
  stage "steps agree", 0 facts refuted, independence 79.4% as before.

## As built: py-aot-classid (2026-11-10)
queue 259's Low: `aot.ae`'s `judge` took `(:class c)` of a class (key `:id`) at its two classed rows, recording `:differs`/`:refused`; now `(:id c)`. `test/python/aot_judge_test.ae` (`ARBACE_PATH=.:test $ARBACE_HOST_EXE -m python.aot-judge-test`) judges fabricated runs, with the unclassed control. `aot_gen` (aot.ae's digest) changes: the AOT's printed packages and the runner rerun (`pyi-aot-runtime`, then pyi-aot-essential, pyi-check-aot). The records are not touched: night 11's AOT record writes the ids.
## As built: 256, Lib/test's universe as data; the check without python3, proved again (2026-11-10, the Mac VM, queue 256)

The brief (from the main host's facts at main-ro, `bin/pyi:183 pyall() { python3`) asked for `bin/pyi check` with no python3 at
check time. That held at the base already: since queue 234 the check's runner is `tools/pyi/libtest_all.py` compiled by pyi's
AOT (`pyrun`, `runner_exe`), and since queue 240 the universe's modules are regrtest's findtests run on it. What still came from
CPython's runs at check time was the method inventory: the own verdict read every reference file (`ref-cpython314-lib-test*.edn`,
outcomes and all) for its ids. 256 makes that inventory a record of its own.

- **`test/python/universe-cpython314.edn`**: a line a module, `{:module M :tests [T ...] :cpython-skips {T REASON ...}}`, the
  modules (529) and test ids (47,836) CPython 3.14.10's recorded runs name and the tests they skipped on the recording platform
  (2,449, with the reason: information for the classing, never read by the judge). `test/python/universe.ae` writes it on stage 0
  (no python3, a second), keyed (`;; key`) by the SHA-256 of the generator and the references; `bin/pyi universe` writes it,
  `record-ref` writes it after Lib/test's references, `bin/pyi universe --check` writes it afresh and compares byte for byte, its
  negative control a universe from one reference less, which must differ (in `pyi-essential` and `make python-essential`).
- **The judge**: `libtest_judge.ae --universe FILE` takes the inventory from it (read-universe, the same rule as read-inventory)
  and, with the own verdict, reads no reference at all (only `--verdict ref` and `--agreement` need CPython's outcomes).
  `bin/pyi check` passes it to the judge and to the controls' judge and refuses a universe whose key is not its inputs' ("the
  universe ... is stale"); `universe_diff` compares findtests' modules with the record's.
- **Verdicts unchanged**: over all 529 modules (a fabricated run, every module empty) the judge's rows with `--universe` equal the
  rows from the references byte for byte (47,922 rows; t1); a real run over 11 modules of the four corpora judged by the head and by
  the base gives the same `results.tsv` (337 rows).
- **Proved with python3 removed**: python3, python and python3.14 first on PATH, each logging its call and exiting 99: `bin/pyi
  check --fresh` over test_bool, test_int, test_getopt, test_filecmp, test_augassign, test_colorsys, test_textwrap, test_winreg,
  test_shlex, test_asyncio.test_locks, test_fnmatch (the runner compiled cold): 337 methods, 313 pass, 23 classed, 1 failing, the
  three controls failing as they must, the stubs' log empty; the stub's own control, the same runner verb by python3
  (`PYI_RUNNER=python3 bin/pyi runner owned`), exits 99 and logs its call.
- **The one failing method**, on the base too: test_augassign's testCustomMethods1, `__iadd__ = None` in a class falls back to
  `__add__` under pyi where CPython raises TypeError ('NoneType' object is not callable): a runtime gap, in no class.
- **Independence** (main's `bin/crops-score --crop python --ref mac/mac/201-python-py7-py11` in a scratch clone of main-ro with
  the patch): main-ro's facts 80.0% (judge 0, stale); 258's patch 79.4%; 256's patch 79.4%, judge 0.971 with `:cases {:self 51638
  :recorded 3187}`: 240's 48,546 inventory methods counted 710 ids twice (12 modules both lib-test's and lib-test-all's references
  record), 47,836 distinct.

## As built: 257, the read cell judged from records: the round trip's levels 2 and 3 (2026-11-10, the Mac VM, queue 257)

The brief asked that every check of the read cell judge from data, not from CPython live. At the base most already did (queue 240,
records mode the default): lex-check and parse-check read CPython's tokens and trees from the python-pydump packs (240r's, the
essential corpora's members committed), syntax-errors' cases are judged by their own verdicts, and the round trips' levels 1 and 4
are each file's own. One part did not: in records mode the round trip's levels 2 (ast.dump) and 3 (the normalized code) were not
judged at all (`compare --deferred`, "the oracle tier's"), so the round trip's 30 `:code-lines` files passed there while the
record (`roundtrip --record`, live) classes them.

- **The print's rows, a pack per corpus** (`tools/pydump/records.bash`): `python-pydump-printed-CORPUS`, the rows alone
  (`tree/REL.row`: CPython's status, ast.dump's digest, the normalized code's, marshal's) of each file the round trip printed,
  keyed as every entry is, by the input's bytes (the printed file's) and pydump.py's. `pyrec_read` and `pyrec_stage` take an empty
  OUT for the rows alone; verify mode compares them row by row, its crossed pair on the rows. `printed-samples` is essential (its
  members committed under `test/python/records/essential/printed-samples/` by the main host's record).
- **`bin/repr-python roundtrip`, mode :lines:** mode record stages the print's rows, mode verify checks them; records mode, once
  `test/python/records/python-pydump-printed-CORPUS.sha256` is pinned, reads the originals' tree rows (python-pydump-CORPUS, as
  parse-check does) and the print's (`rt_recorded_levels`, `rt_levels`: each native row given CPython's columns; a row of no
  record or other bytes refused) and judges levels 2 and 3 as live does: "level 2 (ast) N, level 3 (code) M (CPython's,
  recorded)". A missing or stale entry fails the run (UNRECORDED, STALE; a member that differs from its manifest REFUSED), as every
  record. Without the pin the run says "levels 2 and 3 the oracle tier's: no record of the print" and judges as before, so the
  branch stays green until the main host records. Level 5 (`--text`, marshal) stays the oracle tier's: given level 4 (the same
  bytes under the same name) it checks CPython's determinism, not Arbace.
- **The levels' own negative control** (`arbace.repr.python`'s compare, live and records alike): controls-read.edn's pair, the
  reference's original against its own print's dump (level 1 equal) with the subject's print's digests, which must fail at level
  2 or 3 ("roundtrip (levels 2 and 3) control ...: failed as it must: level ast"). The existing control fails at level 1 first and
  never reached levels 2 and 3.
- **Verdicts unchanged:** live mode is as before (the new control aside). Records mode now gives live's verdicts: on a 14-file sample
  of lib-test, site-packages and stdlib (with test_patma.py, support/__init__.py and packaging/version.py, three `:code-lines`
  files, and bad_coding.py, `:rejected`) the lines equal live's but for their wording; on the base records mode passed the three.
- **Proof (the VM, run):** the print's rows recorded by hand with the VM's python3 3.14.10 in a scratch worktree, never committed;
  240r's samples tree rows equal byte for byte to the VM's (the record's manifest unchanged); then with python3, python and
  python3.14 stubs first on PATH: python-read-essential's command passes, the stubs' log empty; `records verify` 0 differ.
- **Independence:** no case moves (the round trip's files are read's 3,115, already judged by CPython's records at 0.5); the
  judge stays 0.971 (51,638 own, 3,187 recorded), independence 79.4%; the facts' evidence refreshed
  (`.tmp/q257/257-independence.patch`, superseding 256's).
- **The night's command (main host, python3):** `REPR_PYTHON_ORACLE=record bin/repr-python roundtrip samples stdlib site-packages
  lib-test`, then commit the four `python-pydump-printed-*` pins and manifests and `essential/printed-samples/` (report 257).

## As built: 251, pyi on goi's speed: `:slow-on-goi` and `:clib-on-goi` (2026-11-10, the Mac VM, queue 251)

The brief: profile pyi's eval loop under goi on the two classes' tests, port CPython 3.14's fast paths that apply, run the C
library cores natively where goi's table allows, each test's time before and after.

**Where the time goes (run, the VM).** Each of the eight tests was timed alone (`unittest` on the one test id, `bin/pyi run`, two
at a time, base 836dd541 and head in separate worktrees sharing one IR cache):

| test | class | base: load + run | head: load + run | what bounds it |
|---|---|---:|---:|---|
| test_traceback ...keyword_suggestions_from_command_string | slow | 34 + 499 s, ok | 34 + 487 s, ok | 22 children of pyi on goi, 18-22 s each to start |
| test_traceback ...keyword_suggestions_from_file | slow | 33 + 513 s, ok | 33 + 489 s, ok | the same |
| test_queue ...shutdown_immediate_all_methods_in_many_threads | slow | 27 + 49 s, ok | 31 + 38 s, ok | the eval loop (64K put/get under 12 threads) |
| test_xml_etree ...test_deeply_nested_deepcopy | slow | 42 + 190 s, ok | 45 + 161 s, ok | the eval loop (500,000 SubElement calls, then deepcopy) |
| test_subprocess ...test_communicate_timeout_large_input | slow | 75 + 13 s, error | 77 + 13 s, error | the child's start: `server.accept()` times out at the test's own 10 s |
| test_bz2 ...testDecompressLimited | clib | 405 + 700 s, ok | (not rerun) | ci interpreted by goi (the module's `BIG_DATA` compressed at import) |
| test_lzma ...test_decompress_limited | clib | killed at 1,200 s | (not rerun) | the same |
| test_bz2 ...testThreading | clib | killed at 1,200 s | (not rerun) | the same (50 MB) |

Three bounds, of which only one is the eval loop's:
- **pyi's start on goi** (traceback's two, subprocess's): `bin/pyi python -c pass` is 18.3 s at base and head; a Go profile of
  stage 0 (`ARBACE_CPUPROFILE`) puts it in reading the runtime's 23 MB of mode-`:full` forms (about 21% of the CPU), goi's checker
  over `arbace/py` (12%), goi's builder (5%), the evaluated front end (`arbace/python`, 25%) and Go's collector (29%). None of it is
  pyi's runtime running; all of it goes when W2 compiles `arbace/py` into the executable and embeds `arbace/python` (PYS28 (a),
  W-D1 (a)). test_subprocess's test is a failure, not a timeout: its child must connect within 10 s.
- **ci interpreted by goi** (the three `:clib-on-goi`): nothing in the eval loop; goi's table on this branch already lists
  `arbace/ci` and `arbace/crt` (queue 214, 208's DECISION 1 (a)), so W2's executable runs ci's interpreter compiled (cirun's speed,
  about 130 times, 208's table). No REQUEST is needed: the table allows it at W2, and before W2 nothing on the VM can.
- **The eval loop on goi** (queue, deepcopy): what this task changed.

**goi's cost model (probed, `bin/goi run` of a Go program on stage 0, ns per operation):** a push and a pop with `append` and
re-slicing 421, with an index and an int 204, inline 81; `f.Fast[i]` 33, a local slice's `fast[i]` 11; `in := c.Instrs[pc]` 136
(a struct copy), `in := &instrs[pc]` 34; a call 28, a call of a function with a `defer` 233; an interface method call 41. goi
loads a slice, struct or pointer from a field through `reflect.NewAt(...).Elem().Interface()` and stores one through `reflect`'s
`Set` (`go/arbace/goi/types.ae`'s `load` and `store`); only empty-interface, interpreted-interface and scalar fields are plain
loads. A pyi instruction was about 1 us on goi, two-fifths of it the value stack's slice header.

**The changes (run, `go/arbace/py`, `arbace/python/aot.ae`):**
- **The value stack as `Stack[:SP]`** (CPython's `stack_pointer`): `Frame.Stack` holds the room, `Frame.SP` the depth; a push or a
  pop moves an int, not the slice's header. Every `len(f.Stack)` is `f.SP`, every open slice of the stack bounded by it,
  `growStack` copies `Stack[:SP]`, the collector's walk (`finalize.go`) visits `Stack[:SP]`; arbace.python.aot's depth checks read
  `(.-SP f)`.
- **step's instructions by address**, `c.Instrs` and `f.Fast` held in locals (a frame's fast locals are made with it and never
  replaced).
- **CPython 3.14's FOR_ITER_RANGE, FOR_ITER_LIST, FOR_ITER_TUPLE**: the exact range, list and tuple iterators' next inline in step,
  their slots' code.
- **LOAD_ATTR and STORE_ATTR in step's switch** (through `opImpls` each copied its instruction; CPython's are the loop's own; the
  registered implementations stay for `AotExec`), LOAD_FAST, STORE_FAST and POP_TOP's push and pop inline, CALL's keyword names
  read only when given. The specializations 202b and 210 had already ported (LOAD_ATTR's and LOAD_GLOBAL's inline caches, the
  exact int and float BINARY_OPs, CALL_PY_EXACT_ARGS's binding, CALL_ALLOC_AND_ENTER_INIT, the method variant) were kept.

**Measured on goi** (`bin/pyi run`, 100,000 iterations, the base and the head alternated twice): an empty `for` 0.36 -> 0.23 s, a
function call 2.5 -> 2.1 s, a method call 3.3 -> 2.5 s, an attribute load 1.5 -> 1.0-1.2 s, a class's call 2.7 -> 2.1-2.3 s,
`list.append` 1.7 -> 1.2 s; 20,000 SubElement calls 3.9 -> 3.1 s; the tests above. Not taken: a call that stays in step (CPython's
`goto start_frame`; each Python call returns to runFrames and enters its callee through guardedStep's `defer`, about 0.5 us of a
call's 21 us on goi), and the collector's knobs (`PYI_GOMEMLIMIT=8GiB GOGC=400`: 6% for twice the memory).

**After W2 (probed, `bin/pyi aot`):** the runtime compiled, the tests on its loop as W2's executable runs them: deepcopy's test
0.8 s, the queue's 0.1 s.

**The classes:** `:slow-on-goi` 5 and `:clib-on-goi` 3 before and after: on the VM no member passes within the driver's 300 s that
did not already (deepcopy's and the queue's pass alone at base, in 190 s and 49 s; their rows are the night's runs, with other
modules beside them); the night's record decides. test_subprocess's member is a failure of its own 10 s timeout, kept in
`:slow-on-goi` (the child's start is the cause) until W2. Checks: `make python-essential`, `bin/pyi runtime vet`, `bin/pyi
table-check --configs tamago/amd64,tamago/arm64`, a sampled `bin/pyi runtime check`, `test-251-stack.bash` (report 251), and
`bin/pyi lib-test test_range test_iter test_with test_sys_settrace` in both worktrees: every method's outcome the same (588
methods), the four modules' wall 309 s at base, 195 s at the head.

## As built: 252f, the codec registry imports `encodings` before its first use, under 3.14 too (2026-11-10, the Mac VM, queue 252f)

252's finding 4, the user's DECISION 4 (a): `codecEnsureEncodings` (was `codecEnsureEncodings315`, `go/arbace/py/builtins_codecs.ae`)
runs under 3.14 as under 3.15, at the registry's first `register` or `lookup`. CPython imports `encodings` in
`_PyCodec_InitRegistry` (3.14.10's Python/codecs.c, line 1573 on; the interpreter's start), so its search function is first even
when a program registers one before any lookup: `codecs.register(lambda n: None); codecs.lookup("koi8_r")` gave `LookupError` in
pyi, `koi8-r` in CPython. The old lazy import (`if len(r.search) == 0`) went: with the registry initialized, an empty search list
raises "no codec search functions registered" as `_PyCodec_Lookup` does. The import stays lazy (first use, not interpreter start):
the one visible difference is `"encodings" in sys.modules` before any codec use (CPython True, pyi False; a Low). 3.15's
`normalizestring` stays gated under 3.15. Checks: `lib-test test_codecs test_codeccallbacks` the same at base and head (332
methods: 310 pass, 22 classed, 0 failing; test_codecs 266, test_codeccallbacks 44), `make python-essential`, `bin/pyi runtime
vet`, `table-check`, 252's `t252-binascii.sh`; no record or class in `divergences.edn` names a test this changes.

## As built: 265, the read and print cells' results files; the five runtime tables made by Arbace (2026-11-10, the Mac VM, queue 265)

**The results files (completeness).** The writers of `test/python/{roundtrip,full,lex-check,parse-check}-results.edn`
(`arbace.repr.python.results`, queue 201's R1) were in place: `--record` writes a row `["CORPUS FILE" :pass|:known|:fail REASON]` for
every file run, and `test/reap/cells.edn`'s Python read and print cells read them (`:c-known`, `:results`, the three read checks one
`:count-once` group). The cells counted no pass because no `--record` run's files were ever committed (the night's `records record`
runs the checks without `--record`). Two writer defects, fixed: a whole run kept its corpus's rows of files it did not run (a file
gone, a mutant of another pool stayed a pass), now replaced, a partial run (`--only-list`) keeps them; and the tree check's shared
row (`:lines` and `:full`) passed on one mode alone, now `:pending` ("full: not run") until both ran, which `bin/reap` holds a
problem (a row neither pass nor known). Proved on samples, lex-errors and syntax-errors (VM records, not committed): `bin/reap`
counts their passes, a `:pending` row is a problem. The night's commands, all live (mutants have no records):
`REPR_PYTHON_ORACLE=live bin/repr-python roundtrip --record`, `... roundtrip --text --record`, `... lex-check --record`, `...
parse-check --record`, `... parse-check --full --record`; expected cells: read 3,670 / 66 / 0 (3,736 files: samples 6, stdlib 720,
site-packages 921, lib-test 1,153, lex-errors 62, syntax-errors 874; the mutants not counted), print 2,795 / 5 / 0 (2,800 files).

**The five runtime tables (independence).** `builtins_{opcode,errno,fcntl,socket,posix}_table.go`, recorded by
`tools/pyi/*_table.py` from CPython's own modules (python3 at record time), are made by Arbace at check time:
`tools/pyi/runtime_tables.ae` on stage 0, `bin/pyi runtime gen-tables` (Python's essential chain, `pyi-runtime-tables`; `make
python-essential`). Each module's own C is preprocessed by repr/c's preprocessor (`arbace.repr.c.pp`) with its constant-adding calls
made markers (`PyModule_AddIntMacro`, `PyModule_AddIntConstant`, `add_errcode`, `INTRINSIC_FUNC_ENTRY`, `ADD_NB_OP`, the metadata
table's entries), the values evaluated as C's integer constant expressions (casts, `sizeof` on LP64, enumerators), for x86_64 and
aarch64. Sources: the CPython 3.14.10 tarball; musl 1.2.6's (the C crop's pin; its install's header layout composed: `alltypes.h`
by `mkalltypes.sed`'s rules, `bits/syscall.h`; `__strerror.h`, `sysconf.c`, `fpathconf.c`, `confstr.c` by their own rules); Go's
`zerrors_linux_{amd64,arm64}.go` (a name Go declares on both with musl's value is written `syscall.NAME`; one whose values differ
`archConst`: posix_table.py's hand-listed MUSL_ARCH, O_ACCMODE, O_LARGEFILE, O_TMPFILE, now derived); for fcntl, socket and posix
Alpine's `pyconfig.h` (python3 3.14.10-r1's), Linux's UAPI headers (linux-headers 7.2.1-r0) and BlueZ's (bluez-headers 5.87-r0),
the oracle's build inputs, pinned in `tools/pyi/header-apks.sha256` for both architectures. All five are byte for byte the committed
files, gofmt'd by TamaGo's gofmt; each table's control changes one source fact in a copy of the sources (BINARY_OP's HAS_ARG_FLAG,
EPERM's strerror text, F_ADD_SEALS, AI_NUMERICSERV, `_SC_CLK_TCK`) and must differ. `bin/pyi runtime tables` (the oracle tier)
checks the five python3 generators against the same files. `pyconfig.h` is CPython's configure output on Alpine's builder: whether
it counts as a pinned input (eval and AOT 1) or a toolchain's record (0.5, as C's configured musl) is the user's call (report 265's
DECISION); the patch scores 0.5 until then.

## As built: 253, the interpreter's end: Py_FinalizeEx and finalize_modules (2026-11-10, the Mac VM, queue 253; 241's DECISION 2 (a))

Ported from CPython 3.14.10's Python/pylifecycle.c (`_Py_Finalize`, `finalize_modules` and its helpers, `flush_std_files`,
`finalize_subinterpreters`), Objects/moduleobject.c (`_PyModule_ClearDict`), Python/gc.c (`PyGC_Collect`,
`_PyGC_CollectNoFail`, `_PyGC_DumpShutdownStats`), Python/import.c (`_PyImport_FiniExternal`) and Python/_warnings.c's
shutdown path, into `go/arbace/py/pylifecycle.go` (new) and its call sites. One function, `(*Interp).Finalize(status)`, ends
every run: `RunModule`'s exits (the main module's end, SystemExit, an uncaught exception, a SyntaxError of the main file) and
the REPL's; the interpreted loop (goi) and the compiled one (AOT) share it.

- **The order** (`_Py_Finalize`): `threading._shutdown()`; the finalizer queue drained (`_Py_FinishPendingCalls`'s place); the
  atexit functions; the subinterpreters left open ended with CPython's RuntimeWarning; then `sys.is_finalizing()` true and the
  other threads stopped: a thread that takes the GIL from then on hangs (`gilState.final`, PyThread_hang_thread's); sys.stdout
  and sys.stderr flushed (a failed flush of sys.stdout: "Exception ignored while flushing sys.stdout" and exit status 120;
  pyi's standard stream's `flush` now raises its write error as OSError); `gc.collect()`'s collection unless `gc.disable()`,
  `gc.callbacks` called; `sys.path_importer_cache` and `sys.path_hooks` set to None; `finalize_modules`; the streams flushed again.
- **`finalize_modules`**: `builtins._` and sys's `path argv ps1 ps2 last_exc last_type last_value last_traceback
  __interactivehook__ meta_path` set to None, `sys.stdin/stdout/stderr` restored from `__stdin__`...; every module of sys.modules
  set to None in its order (the weak list kept) and the dictionary cleared; the builtins restored to the interpreter's first
  copy (`Interp.builtinsCopy`, taken at `NewInterp` less site's `exit quit copyright credits license help`); the collection;
  `gc.garbage` reported (ResourceWarning "gc: N uncollectable objects at shutdown"); the modules still alive cleared, the latest
  imported first, by `_PyModule_ClearDict`'s two passes (one leading underscore, then all but `__builtins__`), a collection
  after each pass; sys's dictionary, then the builtins', cleared the same way; the last collection.
- **Which modules are dead at the collection.** CPython frees the dictionaries of the modules nothing reaches any more as cyclic
  garbage, so their objects' `__del__` run with every global in place (test_sys' `test_is_finalizing`, test_threading's
  `test_main_thread_during_shutdown`). pyi asks it in two parts. `exitMark` marks what CPython's interpreter state still reaches
  once sys.modules is empty: sys and its dictionary, the restored builtins, the codecs' registry, `gc.callbacks` and
  `gc.garbage`, the warnings module's filters, and what they reach by Python's references (`exitRefs`: 241's `cyTraverse` for
  every object, a heap class's namespace, bases and MRO, an instance's class, a module's dictionary, a weak reference's callback
  but not its referent, the Go fields of the runtime's other types by reflection). A module that held a module object is
  "alive" (cleared in the weak list's pass), a dictionary reached without its module is left as it is (CPython's dead module
  whose functions are still referenced), and every other module's dictionary (but the runtime's own modules') is given to the
  cycle collector as dead (`cyForced`): it is probed as garbage and an edge into it reaches nothing, so what those dictionaries
  alone hold, through nodes, is found and finalized in PEP 442's order, while Go's collector still vetoes anything held another
  way. That second part matters because pyi's classes are held by the runtime's registries (241's finding 2): Go alone would keep
  every module that defines a class. The dictionaries are not cleared afterwards (CPython's `tp_clear`): what they hold through no
  node (a class's attribute, a Go container's item) is left alone, silent rather than finalized with its globals gone.
- **At the shutdown's moments**: an import raises `ImportError("sys.meta_path is None, Python is likely shutting down")` once
  `meta_path` is None; a traceback printed by the Go display shows no source lines (CPython's import of io fails); the unraisable
  hook writes nothing once `sys.stderr` is None; a warning raised by the runtime (an unawaited coroutine, ResourceWarning, the
  shutdown's own) goes through `finalWarn`, _warnings.c's C path (the filters kept from the warnings module, else the
  interpreter's defaults; "<sys>:0: RuntimeWarning: coroutine 'f' was never awaited" when no frame runs); `sysAttr` and
  `sysModule` read the interpreter's own sys module (`Interp.sysMod`, interp->sysdict) rather than sys.modules' entry;
  `_thread.start_new_thread`/`start_joinable_thread` raise PythonFinalizationError "can't create new thread at interpreter
  shutdown" and a join of a running thread "cannot join thread at interpreter shutdown" (gh-123940), as _threadmodule.c.
- **The collections' order** (`cyOrder`): a collection's trash runs its finalizers and weak callbacks as CPython's lists hold
  it, containers before what they hold (its collector moves what a container reaches behind it): breadth first from what
  nothing else in the trash holds (the dead module dictionaries first, in sys.modules' order, then by the records' order,
  `finTrack.seq`), through the slots in their order; no longer Go's map order (`gc.collect()` too). An unclosed text file is
  finalized before its buffer and its raw file, so its data is flushed at exit, as CPython's io does. At the shutdown a weak
  reference that the interpreter state no longer reaches is in the trash itself: no callback (handle_weakrefs).
- **io and warnings, C in CPython**: pyi runs `_pyio` and `_py_warnings`, whose code imports at the moment CPython's C needs
  no import (`_pyio`'s `_dealloc_warn` imports warnings before it closes the file). At the shutdown an import made by their
  code (`cImplModules`) is answered from the modules finalize_modules took out, `warnings` by a stand-in whose `warn` is
  `finalWarn`, and their frames are not counted for a warning's stacklevel (C has none: "<sys>:0: ResourceWarning: unclosed
  file ..." under `-W always`); any other code's import fails as CPython's does.
- **Cost**: a shutdown collection runs only when a tracked object has work or weak callbacks due, and after a module
  dictionary's pass only when a value it dropped reaches such an object (`exitReachesWork`, not through modules); a program importing asyncio,
  unittest, json, re, threading, typing, dataclasses, logging, tempfile and subprocess (114 module dictionaries given dead, 22
  alive) ends in 0.62 s compiled on the VM, the same with or without an object to finalize; under goi a program printing a traceback ends in 24.5 s against the
  base's 23 s (36 s before the pass filter). `PYI_GC_DEBUG=1` lists each module's
  state at the shutdown and each collection.

**Not ported** (Lows in the report): Py_EndInterpreter's `finalize_modules` (a subinterpreter's end runs its atexit functions
only, as before), the daemon threads' frames (CPython clears them; their goroutines keep them), `-v`'s cleanup lines,
`_PySignal_Fini`, the order of several deaths in one collection beyond `cyOrder`'s approximation of CPython's gc lists, an
object reachable from a dead module only through a class or a Go value (not finalized), a pure-Python
`warnings.warn` from a `__del__` at shutdown (pyi's warnings is `_py_warnings`, whose `warn_explicit` imports linecache: the
ImportError CPython's own Python implementation would raise; CPython's C `_warnings` prints the warning). Under goi (the
runtime interpreted) an object the main module's last statements handled can stay in goi's registers and miss the shutdown's
collection (test_gc's ordinary-module test, an unclosed file written last: `:finalization-goi`, W2).

**The classes** (each test's own verdict, sampled on the VM): `:sys-module` 4 to 2 (test_is_finalizing, test_issue20602);
`:gc-collector` 23 to 19 (test_gc's test_gc_main_module_at_shutdown and test_global_del_SystemExit, test_threading's
test_main_thread_during_shutdown served; test_gc_ordinary_module_at_shutdown to `:finalization-goi`: it passes compiled, its
child runs on goi); `:finalization` 63 to 59 (test_struct's test_struct_cleans_up_at_runtime_shutdown, test_threading's
test_join_daemon_thread_in_finalization, test_join_finished_daemon_thread_in_finalization, test_start_new_thread_at_finalization);
test_module's test_module_finalization_at_shutdown, failing in no class at the base, passes; `:finalization-collect` stays 8 (its
tests are collections at run time, not at exit); divergences-aot.edn's `:finalization-goi` (synth/datamodel/del.py) stays, both
runs now print the rest at exit in different orders.

**The samples** (the VM): `.tmp/q253/t/check253.sh ROOT` (26 exit cases compiled against CPython 3.14's runs, stdout, stderr,
status and a file the case leaves unclosed; its control: e3's run judged by e1's reference): PASS on the head, FAIL at the base
c25cce1d1 (21 differ). `bin/pyi lib-test --aot` of test_gc, test_finalization, test_weakref (216 methods), test_module,
test_struct, test_linecache, test_tempfile, test_exceptions, test_traceback (735): every method's verdict the base's or better
(test_tempfile's TestMktemp.test_basic fails at both, a shared TMPDIR). `bin/pyi lib-test-all --fresh test_sys test_warnings
test_threading test_atexit`: 0 failing but test_atexit's test_general, which fails at the base too (its child's atexit tests).
`make python-essential` (golden 127 programs, check --aot 20 pass, 1 classed), `bin/pyi runtime vet`, `bin/pyi table-check
--configs tamago/amd64,tamago/arm64`: ok. `bin/pyi runtime check` with `PYI_SMOKE_ONLY` (20 programs: asyncio, coroutines,
generators, subprocess, threads, signals, io, struct, the REPL session): the forms printed and read again the same, 20 equal,
the REPL equal, the controls fail.

## As built: 260, ctypes objects export their buffer (PEP 3118) (2026-11-10, the Mac VM, queue 260)

244's follow-up 1 (the VM's proposal P1): CPython 3.14.10's `PyCData_NewGetBuffer` (Modules/_ctypes/_ctypes.c) on 249's
`BufferExports`, and what memoryview needs of it.

- **pyctypes** (`go/arbace/pyctypes/ctypes.go`): `CData` embeds a `BufferExports` and answers `Exports()`, so memoryview's
  managed buffer counts its views; nothing calls `Refuse`: CPython's `CDataObject` has no `ob_exports` and `ctypes.resize`
  refuses nothing (a view of a resized object reads the object's memory as it is at each access). `BytesLike` and
  `WritableBytes` are the object's memory in the C world in place (b_ptr, b_size: `bytes(c_int(1))`, `binascii.hexlify(obj)`,
  `bytearray(create_string_buffer(3))`, a write through `memoryview(d).cast("B")` changes `d`); `BufferShape` is the view's
  format (the StgInfo's, "B" when it has none: a union, an incomplete structure), its shape (ndim its length) and its itemsize
  (the innermost array item type's size, `PyCData_item_type`). `memoryview_at` is `PyMemoryView_FromMemory` over the world's
  memory (writes seen both ways, read-only on request, "memoryview_at: size is negative (or overflowed)"); it was a copy.
- **The format strings as _ctypes.c makes them**: `_ctypes_alloc_format_string_for_type`'s codes (ctypes' own but `l`/`L` as
  `q`/`Q` on LP64: `c_char_p` "<z", `c_wchar` "<u", `c_void_p` "<P"; pyi had "<P" and "<w"); a pointer's format with its item's
  shape and "B" for an incomplete item (`PyCPointerType_SetProto`: "&(2)<h", "&B"); the swapped simple types named `NAME_be`
  with the class's `__qualname__` (`create_other_endian_type`, `st->swapped_suffix`); `StructUnionType_init` sets `_fields_`
  through `PyObject_SetAttr`, so `_endian.py`'s `_swapped_meta.__setattr__` gives a `BigEndianStructure`'s fields their swapped
  types ("T{>q:x:>d:y:}"; pyi laid out the class's own fields, so a big-endian structure stored native-endian values: the
  aligned and byte-swapped structure tests, which had been put down to `_align_` and nesting, were this).
- **memoryview** (`go/arbace/py/memoryview.go`, a commit of its own: its core is PY6's 1-D bytes view): a view of a
  `BufferShaper` carries the exporter's format, itemsize and shape, C-contiguous (`len`: "0-dim memory has no length";
  `shape`, `strides`, `ndim`, `itemsize`; memory_subscript's 0-dim rules, items by index and tuple of indices, a contiguous slice
  of the first dimension; `adjust_fmt`'s NotImplementedError "memoryview: unsupported format <i" for a format that is not a
  native single character, as CPython's, which leaves ctypes' formats to the struct module; `tolist` over the shape;
  `cast_to_1D`'s checks of a shaped source; `memory_richcompare`'s shapes and a format the struct module cannot unpack making
  views unequal, a view itself included). Kept to what export needs: the shaped cast is queue 263's.

**Counts** (each test's own verdict; `bin/pyi lib-test-all --fresh test_ctypes`, goi): base b672a5810 621 methods, 491 pass, 130
classed (`:ctypes-port` 66); head 531 pass, 90 classed (`:ctypes-port` 26), 0 failing. The 40 that pass: test_pickling's 12,
test_pep3118's 2, test_byteswap's 12 (the five endian tests, the struct and union cases, the field alignment ones),
test_aligned_structures' 7, test_array_in_pointer's 2, test_memfunctions' 2 (`memoryview_at`), test_buffers'
`test_buffer_interface`, test_struct_fields' two `test_5`. `bin/pyi lib-test test_memoryview test_array`: 944 to 945 pass
(test_memoryview's `OtherTest.test_ctypes_cast`, out of `:ctypes-port`), no method lost, the 7 failing the base's. Left in
`:ctypes-port` (26 test_ctypes tests and 2 outside): the C crop's layouts (Packed1-3, msvc bit fields), bit fields over unions
and swapped uint32, the pointer caches, `test_bad_length`, `test_max_field_size_gh126937`, `from_address` of an array's memory and
`from_buffer` of one (Go memory, by design), the issue18060 ABI tests, `test_values`' tables, `test_typeerror`, `_objects`,
`test_open`'s errno, `test_c_char_p`'s keep object, `test_swapped_type_creation`, `test_p2a_objects`.

**The check** `.tmp/q260/t/check260.sh ROOT` (not committed; the report's appendix): `probe260.py`, 478 lines over 30 ctypes
types (format, shape, strides, len, bytes, items, slices, casts, hash, equality), the cast writes, `memoryview_at`, pickling,
a view across `resize`, `from_buffer` and `from_buffer_copy` of ctypes objects, against python3 3.14.10's run; its negative
control judges the run against a mutated reference, which must fail. PASS on the head, FAIL on the base (every `memoryview` of
a ctypes object a TypeError).

## As built: 264, a number dunder set to None is called, as CPython's slots call it (2026-11-10, the Mac VM, queue 264)

256's finding 4, 259f's class `:inplace-slot-none`. CPython 3.14.10's `update_one_slot` (Objects/typeobject.c) gives a class
whose `__iadd__` (or `__add__`, `__radd__`, any number dunder) is None the generic slot like any other value (only `__hash__`
= None is special), and the slot calls what it finds: `SLOT1` (`slot_nb_inplace_*`) by `vectorcall_method`, `SLOT1BINFULL`
(`slot_nb_add` ... `slot_nb_divmod`, `slot_nb_power`'s binary and ternary halves) by `vectorcall_maybe`, whose NotImplemented
answers a *missing* method only. So `x += 1` with `__iadd__ = None` raises TypeError ('NoneType' object is not callable): no
fallback to `__add__` (`binary_iop1` returns the slot's error), and `x + 1` with `__add__ = None`, `1 + x` with
`__radd__ = None`, `pow(x, 2, 3)` with `__pow__ = None` and `divmod` with `__divmod__`/`__rdivmod__` = None raise the same.
pyi's class slots dispatch through `callSpecialOr` (`go/arbace/py/class.ae`), which took a None for a missing method; it now
answers NotImplemented only when the type has no such attribute, and `builtinDivmod` (`builtins.ae`) goes through it instead of
`CallSpecial` (which keeps skipping None for its other callers: abs, round, `__fspath__` ...). The interpreted loop
(`eval.go`'s in-place op) and the compiled path (`AotInplace`, `AotBinary`) reach it through `InPlaceOp`/`BinaryOp`; their
int/float fast path (`numberFast`) is untouched and runs first, so the common case does no more work. test_augassign's
testCustomMethods1 passes and `:inplace-slot-none` leaves `test/python/divergences.edn`. Proof: a probe of every in-place dunder
(set to None with and without the binary one, inherited and blocked in a subclass, on a list subclass, set after the class
statement and deleted again), every binary and reflected one, the ternary pow, divmod and `operator.iadd`, 139 lines equal to
`python3 -I -B`'s interpreted (`bin/pyi run`) and compiled (`bin/pyi aot`), 109 of them different on the base; a negative
control (a reference with the first case changed to the base's fallback) differs. A second probe of the built-in subclasses
(int, float, str, tuple, set, dict) agrees too.

Found on the way (the brief's `lib-test test_binop`, on the base too, in no class): `doRichCompare` (`abstract.ae`) tried b's
swapped method first only when b's class is a proper subclass of a's that overrides it (`overridesReflected`, since PY5c), then
a's, then b's again. CPython's do_richcompare (Objects/object.c) tries b's first whenever b's class is a proper subclass of a's,
overriding or not, and keeps `checked_reverse_op` so it does not try it a second time. test_binop's test_comparison_orders logged
`['C.__eq__', 'B.__eq__', 'C.__eq__']` for `B() == C()`, and `B() <= D()` with D overriding nothing logged `['B.__le__',
'B.__ge__']` where CPython logs `['B.__ge__', 'B.__le__']`. Both rules are ported (their own commits); the method passes, as does test_compare's
test_ne_low_priority (`['Derived.__ne__', 'Base.__eq__', 'Derived.__ne__']`, the same double try, also in no class), and a
probe of `==`, `!=`, `<=` and `<` over unrelated classes, an overriding and a non-overriding subclass agrees with `python3 -I -B`
interpreted and compiled.
Checks: `lib-test test_augassign test_binop test_descr test_class test_compare test_richcmp`, 251 methods, the base 217 pass, 32
classed, 2 failing, the head 220 pass, 31 classed, 0 failing, method by method exactly the three above from failure to ok;
`make python-essential`, `bin/pyi runtime vet`, `table-check --configs tamago/amd64,tamago/arm64`, and a sample of `runtime
check` (live oracle: 9 programs and 1 session equal, controls failing as they must).

## As built: 262, the C API's next slices and `_testlimitedcapi` on ci (2026-11-10, the Mac VM, queue 262; 244-3 (a), 244-4 (a))

244's follow-up 3 (the VM's proposal P3): the limited C API beyond the first slice, with reference counts on the handles,
then the first part of 244-4, CPython's own `_testlimitedcapi` built by ci into pyi's C world.

- **The headers are CPython's own** (`tools/pyi/clib/ctypestest/lib.conf`): every source of the world defines `Py_LIMITED_API`
  (`_ctypes_test.c` 3.13, `_testlimitedcapi`'s parts 3.5 to 3.14), and ci's checker and builder take CPython 3.14.10's
  `Include/` in that mode as gcc does (a probe of `_testlimitedcapi/float.c` first). The clean-room `Python.h` of 244 is
  removed; `tools/pyi/capi/include/pyconfig.h` is Arbace's configuration of `Include/` for ci's target (LP64, musl, the GIL;
  `HAVE_SYS_TYPES_H`, `SIZEOF_PID_T` and the others `Include/` consults). `pycapi.c` defines the data `Include/` declares
  without including it (under the limited API the type objects' type is incomplete): the singletons' and the type objects'
  heads, and a `PyExc_NAME` pointer for every built-in exception, bound at the world's start to the objects' handles
  (`bindData`). The node program grew from 1.44 to 2.54 MB.
- **The world's modules**: `_testlimitedcapi.c` and its parts but `heaptype_relative.c` and `vectorcall_limited.c` (their Init
  functions make types from `PyType_Spec`s: natives that add nothing, the next slice's). The module is single-phase
  (`PyModule_Create2`): `extModuleObj` keeps the module PyInit made, its functions bound to it (`cMethod`: METH_VARARGS,
  KEYWORDS, NOARGS, O, FASTCALL; their messages name `module.func()` as `_PyObject_FunctionStr`; a result with an error
  set, or NULL without one, `_Py_CheckFunctionResult`'s SystemErrors).
- **Reference counts on the handles** (`go/arbace/pyctypes/world.go`): a proxy is a `PyVarObject` head (ob_refcnt, ob_type,
  ob_size: the limited API inlines `Py_SIZE`; a list's or bytearray's refreshed after every native, which may have run Python
  code) whose count is the references C holds. A proxy is made at 0 (a borrowed reference); a new reference C receives raises
  it; `Py_DecRef`, `_Py_DecRef`, and an inline `Py_DECREF` through `_Py_Dealloc`, lower it; at the end of the outermost C
  entry from Python (a ctypes foreign call, an extension module's function, its PyInit: `capiEnter`/`capiLeave`, after the
  result was taken) the proxies at 0 are released, their map entries dropped and their memory freed, so an object C held only
  during a call is Go's alone again (the probe's weak reference dies). A result C returns is stolen (the cMethod's, PyInit's,
  a `py_object` restype's: GetResult's `Py_XDECREF` after `O_get`). Types and the singletons are immortal with CPython's
  `_Py_IMMORTAL_INITIAL_REFCNT`, which the inline `Py_INCREF`/`Py_DECREF` of the old limited versions leave alone. A
  `py_object` stored in a ctypes object's memory pins its object (CPython keeps it in `_objects`, which the handles cannot
  follow); one a `from_param` CArg holds is given back when the CArg is collected.
- **The natives** (`capi_limited.go`, `capi_objects.go`, ports of `Objects/` and `Python/`): `PyModule_*`, `PyArg_Parse` and
  `PyArg_ParseTuple` (getargs.c's `vgetargs1_impl`, `convertsimple`'s units and `seterror`'s messages), `Py_BuildValue`,
  the errors (`PyErr_SetObject`, `ExceptionMatches`, `GetRaisedException` and the rest), capsules (`PyCapsule_New` over py's
  `PyCapsule`, its C destructor run when the capsule is collected; `PyCapsule_Import` as capsule.c), slices (sliceobject.c's
  `PySlice_Unpack`, `AdjustIndices`, `GetIndicesEx`, `GetIndices`), `PyEval_GetFrame*` and the recursion limit, `PyFrame_Get*`,
  `PyGILState_*`, `PySys_GetObject`/`SetObject`/`GetXOptions`, `Py_GetConstant[Borrowed]`, the object protocol (attributes,
  items, `HasAttr*` with the unraisable report of 3.13), sequences (abstract.c's `PySequence_GetItem` and its sq_item index
  rule), tuples, lists, dicts, numbers', strings' and types' cores, imports. 119 of the 320 C API functions the world
  references; the other 201 are natives that raise NotImplementedError "pyi's C API does not provide NAME (queue 262)"
  (`bindMissing`; the C stack unwound as by a longjmp), so the module loads whole and a function reaching one fails alone
  (`:capi-port`).
- **pythonapi**: `PyFrame_GetLocals` and the other `PyFrame_Get*`, `PyEval_GetFrame*`, `PyGILState_Ensure`/`Release`/`Check` (a
  thread that calls C holds the GIL already: `PyGILState_LOCKED`), `Py_PACK_VERSION`, and every native above, resolved by name.

**Counts** (each test's own verdict; `bin/pyi lib-test-all --fresh MODULES`, goi; base 9de32029b, head this branch):

| module | base | head |
|---|---|---|
| test_capi (1,195) | 4 pass; `:testcapi` 1,116, `:extension-modules` 71 (7 modules skip on `_testlimitedcapi`) | 37 pass; `:testcapi` 1,154, 0 failing |
| test_ctypes (621) | 531 pass, `:ctypes-port` 26 | the same, 0 failing |
| test_bytes (325) | does not import (SkipTest at `ByteArrayTest`'s class body: `:extension-modules` 325) | 253 pass, `:bytes-gaps` 54, 0 failing |
| test_class (42) | 31 pass | 32 pass (`testHasAttrString`) |
| test_frame (61) | 47 pass, `:ctypes-port` 1 | 48 pass (`TestFrameCApi.test_basic`); the base's 2 failing |
| test_codecs, test_support | 266, 64 pass | the same (test_support's 3 failing the base's) |

test_capi's 33 new passes are test_build's 6, test_capsule's 7, test_eval's 11 and test_slice's 9 (none lost); `:testcapi`
grows by 38 because test_bytearray, test_bytes and test_object of test_capi now import `_testlimitedcapi` and stop at
`_testcapi` instead (from `:extension-modules`). `:bytes-gaps` (new, deferred) holds test_bytes' 54 failures: pyi's bytes and
bytearray, two of them Go panics (`test_mutating_index`, `test_resize`), none the C API's. `:capi-port` (new) matches the
not-provided natives; no sampled row reaches one.

**The check** `.tmp/q262/t/check262.sh ROOT` (the report's appendix): `probe262.py`, 61 lines over every reachable part
(build, version, slices with their index rules, eval and frames, capsules and `PyCapsule_Import`'s errors, sys, the sequence
protocol through `bytearray`, `HasAttrString`, getargs' messages, a weak reference that dies once C has let go, pythonapi's
frames and GIL state), against python3 3.14.10's run of CPython's own `_testlimitedcapi`; its negative control judges the run
against a mutated reference, which must fail. PASS on the head, FAIL on the base (no `_testlimitedcapi`).

**What remains of 244-4**: the types a spec makes (`PyType_FromSpec`, `FromModuleAndSpec`, `FromMetaclass`: objects whose
memory is C's, their slots C functions; `heaptype_relative.c`, `vectorcall_limited.c`, `_testcapi`'s heap types), the 201
natives of `:capi-port`, and `_testcapi` itself: it uses the full API, whose macros read objects' layouts (`PyTuple_GET_ITEM`'s
`ob_item`, `PyBytes_AS_STRING`, `PyFloat_AS_DOUBLE`, the compact unicode's data), which proxies of a head alone cannot give
(the DECISION of the report); `_testinternalcapi` by design (244-4 (a)).

## As built: 263, P6's second pass and the shaped memoryview cast (2026-11-10, the Mac VM, queue 263)

The main host's proposal P6 (queue 260-264's P4): 252's first pass of 3.15.2's module changes left the modules below behind
the switch; this pass ports what their 3.15 tests ask, each change gated by `py315()` unless marked "both", from CPython
3.15.2's Modules/ (the VM's oracle build, `.tmp/oracle315`), and 3.14.10's memoryobject.c for the cast:

- **memoryview.cast with a shape** (both): `memoryview_cast_impl`, `cast_to_1D`, `cast_to_ND`, every format
  `get_native_fmtchar` accepts, assignment to a full multi-index and to a 0-dim view, count and index on N-D views; under
  3.15 the Zf/Zd formats and 3.15's error message.
- **struct**: 3.15.2's `s_new`/`s_init` split (the FutureWarning and DeprecationWarning paths, `set_format`'s non-ASCII
  ValueError, the uninitialised struct's AttributeError), Zf and Zd; pack_into's and unpack_from's Py_ssize_t offsets
  raise OverflowError (unpack_from under 3.14 too: 3.14.10's clinic converts it, pyi gave IndexError).
- **resource**: unsigned RLIM_INFINITY, RLIM_SAVED_CUR/MAX, a -1 limit's DeprecationWarning, prlimit through the same
  rlim2py; the limits by PyNumber_Index (both).
- **math**: fmax, fmin, signbit, isnormal, issubnormal; loghelper with a huge `__index__`.
- **zlib**: crc32_combine and adler32_combine (zlib 1.3's multmodp/x2nmodp, adler32_combine_), `__version__` deprecated.
- **mmap**: an anonymous map's size(), trackfd=False's ValueError, find's start unclamped, flush(offset, size=-1, *, flags)
  with MS_*.
- **ssl**: `_SSLSocket.group()`, `SSLContext.set_groups`/`get_groups` (Go's CurveIDs), load_cert_chain's OSError with its
  filename, and got_eof_error (gh-148292: after an SSLEOFError, read, write and do_handshake raise it again).
- **array**: typecodes 'e', 'Zf', 'Zd' (get, set, byteswap, repr, reduce, mformat codes 22-27 and the reconstructor).
- **_sqlite3**: connect, Connection and five methods with 3.15.2's positional-only and keyword-only parameters.

3.15's classes live in `test/python/divergences-315.edn` (252's DECISION 3 (a); the user's decision (a) on 263's DECISION,
2026-11-10): a copy of `divergences.edn` followed by a section marked `;; 3.15.2's own classes (queue 263)`, which the flip
keeps while the copy above it is refreshed. Its classes: `:unicodedata-315` (deferred: block, the grapheme, InCB and
Extended_Pictographic properties, iter_graphemes), `:ssl-sigalgs-315`, `:ssl-ciphersuites-315`, `:ssl-ktls-315` (design: Go's
crypto/tls neither sets nor reports signature algorithms, does not configure TLS 1.3's suites, has no kTLS),
`:memoryview-formats-315` (test_compare_equal, an array's view of format 'B'). `:ssl-go-tls` (both files) takes
test_wrong_cert_tls13: Go's server sends bad_certificate for any error its VerifyConnection callback returns where OpenSSL
sends unknown_ca; the client passes only when the race gives it EOF first (1 run of 6 on the head, 0 of 6 on the base), so the
3.14 count of the head's first measure (166 against the base's 167) was this flake.

Counts (the VM, `bin/pyi aot test/python/libtest.py MODULE`, each test's own verdict: pyi ok / methods, the oracle's ok;
3.14: python3 3.14.10, 3.15: the 3.15.2 build):

| module | 3.14 base | 3.14 head | 3.15 base | 3.15 head | oracle 3.14 / 3.15 |
|---|---|---|---|---|---|
| test_memoryview | 105 / 171 | 106 / 171 | 105 / 173 | 107 / 173 | 152 / 154 |
| test_base64 | 37 / 41 | 39 / 41 | 64 / 67 | 66 / 67 | 41 / 67 |
| test_buffer | 5 / 95 | 5 / 95 | 5 / 97 | 5 / 97 | 93 / 95 |
| test_struct | 39 / 44 | 39 / 44 | 33 / 47 | 42 / 47 | 44 / 47 |
| test_resource | 13 / 13 | 13 / 13 | 11 / 14 | 14 / 14 | 13 / 14 |
| test_math | 83 / 89 | 83 / 89 | 74 / 89 | 84 / 89 | 85 / 86 |
| test_array | 847 / 893 | 847 / 893 | 866 / 1098 | 1043 / 1098 | 893 / 1098 |
| test_zlib | 75 / 79 | 75 / 79 | 75 / 88 | 84 / 88 | 77 / 86 |
| test_mmap | 45 / 53 | 45 / 53 | 42 / 55 | 47 / 55 | 45 / 47 |
| test_sqlite3.test_dbapi | 201 / 205 | 201 / 205 | 201 / 206 | 202 / 206 | 205 / 206 |
| test_sqlite3.test_userfunctions | | | 66 / 74 | 72 / 74 | - / 72 |
| test_ssl | 167 / 206 | 166 / 206 (flake) | 153 / 222 | 158 / 222 | 189 / 204 |
| test_unicodedata | 53 / 61 | 53 / 61 | 55 / 73 | 55 / 73 | 56 / 66 |

test_sqlite3's regression (38 / 38) and types (46 / 48) and test_select (5 / 6) are equal on both. Judged against the classes
(`test/python/libtest_judge.ae --verdict own`), the 3.15 head's 16 modules: 2,068 pass, 325 classed, 2 failing, the two
sqlite3 tests the full run's module id `test_sqlite3/...` classes (this harness ran the submodule); the 3.14 head's 12: 1,672
pass, 275 classed, 3 failing, those two and test_base64's test_b64encode (on the base too). Proofs: `t/check263.sh mvcast`,
`p315`, `arr315` (references from 3.14.10 and 3.15.2, PASS on the head, FAIL on the base, negative controls failing), the
got_eof probe's five answers equal to 3.15.2's.

## As built: 266, bytes and bytearray: `:bytes-gaps` closed (2026-11-10, the Mac VM, queue 266)

262's finding 1: test_bytes ran on pyi for the first time with 54 failing methods, `:bytes-gaps`. Ported from CPython 3.14.10's
Objects/bytesobject.c, bytearrayobject.c, bytes_methods.c and stringlib (split.h, join.h) into `go/arbace/py/bytes.ae`,
`bytearray.ae`, `bytesmethods.ae` (and small hunks of `percent.ae`, `float.ae`, `sys.ae`, `builtins_codecs.ae`); by cause:
- **the two panics**: `bytearray_ass_subscript` reads the index, then the value (`_getbytevalue`, whose `__index__` may clear the
  bytearray, gh-91153), then checks the bounds; `resize` and the constructors past `maxBytesAlloc` (2^32) are MemoryError
  (`bytes(sys.maxsize)`: OverflowError "byte string is too large", as `_PyBytes_FromSize`).
- **constructors**: `_getbytevalue` (an `__index__`'s own error propagates, an int too large for a long is ValueError), the order
  of `bytes_new_impl` and `bytearray___init___impl` (encoding/errors messages, `__bytes__` looked up for every class and a None one
  called, `_PyObject_LookupSpecial` runs a property), `PyBytes_FromObject`'s buffer/iterable path (`__buffer__` classes too),
  an emptied bytearray first (a failing list or tuple leaves it empty).
- **find, rfind, index, rindex, count, startswith, endswith**: clinic's `slice_index` (clipped, `_PyEval_SliceIndex`'s TypeError),
  `ADJUST_INDICES` (a start past the end stays: nothing matches, not even an empty sub), `parse_args_finds_byte`, tailmatch,
  `bytes.__contains__` (`_Py_bytes_contains`) for both types.
- **split, rsplit, removeprefix, removesuffix, partition**: stringlib's split with the whitespace remainder kept as CPython keeps it,
  rsplit from the right, buffer arguments with "a bytes-like object is required, not 'X'".
- **fromhex** (`_PyBytes_FromHex`: bytes-like input, error positions, subclasses), **maketrans**, **join** (`PySequence_Fast`, size
  recheck after each `__buffer__`), **extend** (the size read before the buffer, clamped; `can't extend bytearray with X`),
  slice assignment (`can assign only bytes, buffers, or iterables of ints`), a shrink in place, `__alloc__`, `sys.getsizeof` of a
  bytearray, `__doc__`, bytearray's repr (an escaped quote), `%` (`format_obj`, `byte_converter`, `__rmod__`), `float(bytearray)`.
- **`-X dev`'s `unicode_check_encoding_errors`**: `checkEncodingErrors` in `codecEncodeText`/`codecDecodeText` (a LookupError for an
  unknown encoding or error handler even for an empty or ASCII text); `sys.flags.dev_mode` follows it.
Result (`bin/pyi lib-test test_bytes`, interpreted): 325 methods, 308 pass, 17 classed (buffer-exports 4, cpython-only 2,
skipped-by-test 8, testcapi 3), 0 failing; `:bytes-gaps` is removed from `divergences.edn` and `divergences-315.edn`. Proof: the probe
`t/probe.py` (1,950 lines of bytes and bytearray answers against python3 3.14.10): 438 lines differ on the base, 0 on the head, run
and aot.

## As built: 267, test_base64's test_b64encode (2026-11-11, the Mac VM, queue 267)

No code changed: 266 had already fixed it. `base64.b64encode(s, altchars=...)` calls `bytes.maketrans(b'+/', altchars)`
(Lib/base64.py:58, 3.14.10); before 266 pyi's `bytes.maketrans` compared lengths without reading both arguments through
the buffer protocol first, so a `str`, `int` or `list` `altchars` (test_b64encode's `altchars="*$"` TypeError case) gave
"maketrans arguments must have same length" (ValueError) where CPython 3.14.10's `bytes_maketrans`
(Objects/bytesobject.c, `_Py_bytes_maketrans` in Objects/bytes_methods.c) takes `Py_buffer` arguments and raises
"a bytes-like object is required, not 'str'" (TypeError) first. 266's rework (`bufArg` before the length check) matches
it. 263's Low 1 was recorded on the pre-266 base. Checked: test_base64 41 methods, 40 pass, 1 classed (cpython-only), 0
failing at the head; the probe `.tmp/q267/t/probe.py` (str, int, None, list, bytearray, memoryview and array altchars, and
`bytes.maketrans` with the same) equals python3 3.14.10's, and fails on 379efb8fe (the control).

## As built: 268, PyType_FromSpec and C-born objects; `_testlimitedcapi` whole (2026-11-11, the Mac VM, queue 268; 262's DECISION (a))

The first entry of 262's DECISION (a) (the user, 2026-11-10): types made from specs and objects whose memory is C's, then
`_testlimitedcapi`'s two parts 262 left out and the rest of the limited API's natives. Ported from CPython 3.14.10's
`Objects/typeobject.c` (`PyType_FromMetaclass` and its entries `PyType_FromSpec`, `FromSpecWithBases`,
`FromModuleAndSpec`; `get_bases_tuple`, `special_offset_from_member`, `check_basicsize_includes_size_and_offsets`,
`PyType_GetSlot`, `PyObject_GetTypeData`, `PyType_GetTypeDataSize`, `PyObject_GetItemData`, `PyType_GenericAlloc`,
`PyType_GenericNew`, `subtype_dealloc`'s part), `Python/structmember.c` (`PyMember_GetOne`, `PyMember_SetOne`),
`Objects/call.c` (`PyObject_Vectorcall`, `PyObject_VectorcallMethod`), `Python/getargs.c` (`vgetargskeywords`:
`PyArg_ParseTupleAndKeywords`, which clinic's limited-API parsers call) and the files of 262's remainder below.

- **A type made from a spec** (`go/arbace/pyctypes/capi_types.ae`) is a pyi type made by `py.NewExtType`: static to pyi (a
  class deriving from it keeps a C-born value, as a class deriving from a built-in type keeps its value: `WrapInstance`),
  its MRO C3's, its `cType` recording what `PyHeapTypeObject` holds (tp_name, basicsize and itemsize after PEP 697's
  relative arithmetic, `_align_up` by `ALIGNOF_MAX_ALIGN_T` = 16, the flags with `Py_TPFLAGS_HEAPTYPE`, the slots by number,
  inherited from a C base first, the members' copy with absolute offsets, the module, the token, the dict, weaklist and
  vectorcall offsets). Its pyi slots call the spec's C functions: tp_new, tp_init, tp_call and the vectorcall at
  `tp_vectorcall_offset` (Py_TPFLAGS_HAVE_VECTORCALL), repr, str, hash, richcompare, iter, iternext, the mapping and
  sequence slots, nb_bool, the number protocol's binary, in-place and unary slots, index, int, float, getattro, setattro,
  descr_get, descr_set. `tp_methods` become method descriptors (pyi `Builtin`s with the type as owner: self is the call's
  first argument, METH_METHOD's defining class the type, METH_CLASS and METH_STATIC as classmethod and staticmethod),
  `tp_members` member descriptors over the instance's memory, `tp_getset` getset descriptors that call C. Every check and
  message of `PyType_FromMetaclass` is kept (relative offsets with a positive basicsize, offsets out of range, the special
  members' type and flags, extending a variable-size class without `Py_TPFLAGS_ITEMS_AT_END`, immutable over mutable bases,
  the basicsize too small). Not provided (NotImplementedError "pyi's C API does not provide ..."): a metaclass other than
  `type`, and a base whose instances are not C's (a built-in type other than `object`, a Python class).
- **C-born objects**: their memory is the world's (`basicsize + (nitems+1) * itemsize`, zeroed, the head initialized: a new
  reference), their handle is that memory's address (no proxy), their ob_refcnt C's references as a proxy's. The Go value
  (`cObj`; for a class deriving from the type, its instance as `outer`, the object C's pointer names) is in the handles'
  maps while C holds a reference or a C entry is in progress, as 262's proxies are; once Python holds it alone and Go
  collects it (a finalizer on a small object of its own, outside the cycle the object and its instance make), its
  `tp_dealloc` runs on the world's next entry (`reapDead` in `capiEnter`): the type's, a C base's, else `subtype_dealloc`'s
  (the dict at tp_dictoffset released, the memory freed). A C-born object's `__dict__` (tp_dictoffset,
  `PyObject_GenericGetDict`) is a pyi dict held in its memory; pyi's generic attribute access finds it through
  `py.InstanceDictHook`.
- **The world** (`tools/pyi/clib/ctypestest/lib.conf`): `heaptype_relative.c` and `vectorcall_limited.c` are built, so
  `_testlimitedcapi` is CPython's whole; `go/arbace/clib/ctypestest` rebuilt by `bin/pyi-clib build ctypestest` on the VM
  (the main host rebuilds it on next, whose ci differs).
- **The remainder natives** (`capi_more.ae`): of the 201 natives 262 left raising, 171 are given (dicts, sets, lists,
  tuples, ints with longobject.c's messages, floats, complex numbers, bytes and bytearrays, the number, mapping and
  sequence protocols, str's core and its codecs' decoders, `PyUnicode_Translate` as `_PyUnicode_TranslateCharmap`, weak
  references, the module table, files, PyMem) and the parts' two Init functions are C's; 28 remain (codecs' incremental
  objects, charmap and locale codecs, wide strings, `PyUnicode_FromFormat`, `Resize`, `WriteChar`, exec'd code modules,
  frozen modules, `PyType_Freeze`, `Py_CompileString`, `PyByteArray_AsString`, `PyBytes_DecodeEscape`, `PyFile_FromFd`).

Counts (`bin/pyi lib-test-all --fresh`, each test's own verdict, the VM), base 2fc54227a and head the same: test_capi 1,195
methods, 37 pass, `:testcapi` 1,154; test_call 189, 7 pass, `:testcapi` 130; test_ctypes 531 pass, `:ctypes-port` 26; test_bytes
308 pass; test_frame 48 pass and its 2 failing of the base; `:capi-port` 0 rows on both sides. test_capi's and test_call's limited-API tests
(TestHeapTypeRelative, TestPEP590's vectorcall_limited tests, test_capi's dict, set, list, long, unicode modules) all
import or require `_testcapi` first, so they stay `:testcapi`: this task's proof is its two probes against python3
3.14.10's own `_testlimitedcapi` (49 and 84 lines equal on the head; on the base the first stops at the missing types and
the second differs in 133 lines).

## As built: 261, the rest of `:ctypes-port` by cause, and the packed structs (2026-11-11, the Mac VM, queue 261)

244's follow-ups 2 and 4 (the VM's proposal P2), on 260's buffer export and 268's C API; ported from CPython 3.14.10's
`Modules/_ctypes` (`_ctypes.c`, `cfield.c`, `stgdict.c`, `callproc.c`) and `Python/frozen.c`.

- **from_buffer shares memory** (`CDataType_from_buffer_impl`): a memoryview of the object (kept in `_objects`, so the
  exporter refuses a resize while the ctypes object lives; "underlying buffer is not writable", "not C contiguous", the
  memoryview's own TypeError for a non-buffer), and its bytes mapped into the C world in place (`world.ae`'s `mapGo`: a
  slab of the mappings' region whose bytes are the buffer's own Go slice, unmapped once the ctypes object is collected).
  C, the ctypes object and Python see each other's writes, as in CPython. `from_address` of a Python buffer's address
  stays by design: `array.buffer_info()` gives a Go address, no address of the world.
- **cast() shares b_objects** with its source (a dict made when it is None, the source kept in it by its id).
- **PyCStgInfo_clone does not share the pointer type cache** (a subclass's `POINTER()` is its own); the swapped simple
  types get a StgInfo of their own.
- **The metatypes' `__init__`** runs `type_init`'s argument checks before `PyStgInfo_Init` ("type.__init__() takes 1 or 3
  arguments"), and `PyCSimpleType_init` makes the swapped types when the class's metatype is `PyCSimpleType` itself
  (not when its base is `_SimpleCData`).
- `_length_`'s sign before its overflow; `d_set`, `f_set` and the C API's float conversions by `PyFloat_AsDouble` (a str is
  a TypeError, "must be real number, not str"); ctypes' private errno by thread (`get_errno`/`set_errno` and
  `use_errno`'s swap, CPython's thread state dict); `CField.bit_size` a Python int past Py_ssize_t (gh-126937).
- **pythonapi's data symbols**: `Py_OptimizeFlag` (sys.flags.optimize) and `_PyImport_FrozenBootstrap`, `_PyImport_FrozenStdlib`,
  `_PyImport_FrozenTest` (frozen.c's `const struct _frozen *`, made from pyi's frozen modules: name, marshalled code, size,
  is_package, a zero entry last), made in the world's memory on their first lookup (`pyDataSymbol`).
- Structure's, Union's, Array's and _Pointer's repr is object's, naming the instance's class (pyi's inherited slot was handed
  the built-in value).

**The packed structs.** ms_struct (c-props4) and `#pragma pack` in a function body of the main file (CS290 (a)) are
honoured by ci: test_bitfields' msvc mode passes. test_generated_structs' Packed1-3 and four Example_gh_* structs still
differ: `_ctypes_test_generated.c.h`'s function is in an *included* file, and ci ignores a `#pragma pack` inside a function
body there (gcc: 9 and 12 bytes; ci: 16 and 16; probe in queue 261's report). A REQUEST for the C crop; ctypestest is
rebuilt after it (no C of the world changed in 261).

**Left in `:ctypes-port`** (test_ctypes, sampled): test_generated_data (the C crop's); by design: test_numbers' three
`from_address` tests (Go memory), the issue18060 tests (an ABI coincidence ci's convention does not share),
test_c_char_p (sys.getrefcount); test_audit's test_ctypes_call_function (sys.addaudithook is a no-op in pyi).
test_pointer_types_factory moved to `:finalization-collect` (pyi collects no class, a plain one too).

## As built: 269, the full API's layouts and `_testcapi` on ci (2026-11-11, the Mac VM, queue 269; 262's DECISION (a), 268's DECISION (a))

262's DECISION (a) past `_testlimitedcapi`: the full C API's macros read objects in place, so the handles of the types
and of tuple, list, bytes and float carry CPython 3.14.10's layouts (`go/arbace/pyctypes/capi_mirror.ae`), and
CPython's `_testcapi` is built by ci into pyi's C world part by part.

- **Type objects**: a type's handle is a `PyTypeObject` (a `PyHeapTypeObject`'s 936 bytes, or the world's static struct of
  a type `Include/` names: `pycapi.c`'s are PyTypeObject-sized since this task, bound before any other handle is made)
  filled as CPython's: `tp_name`, `tp_basicsize`/`tp_itemsize`, `tp_flags` (CPython's own for the types `Include/`
  names, from python3 3.14.10's `__flags__`; a C type's own with the base's fast-subclass bits), `tp_base`,
  `tp_bases`, `tp_mro`, `tp_dict` (pinned), and the slots: a C type's C functions, a pyi type's the generic
  protocol's natives (`tp_repr` = `PyObject_Repr`, `tp_alloc` = `PyType_GenericAlloc`, `tp_new`/`tp_init`/`tp_dealloc`
  natives of their own), so `PyTuple_Check` (`Py_TYPE(o)->tp_flags`) and `PyList_Type.tp_new(type, ...)` work.
- **Mirrors** (tuple, list, bytes, float, a subclass's instance too): the object's CPython layout in the world's memory,
  made when C first sees it, synchronized at the C boundary both ways (as PyPy's cpyext): Go to C before the world is
  called and when a native or a closure returns (the mirrors it used), C to Go when the world returns and when a
  native receives a mirrored object (once a synchronization epoch, through the items' mirrors). A container's slots
  hold references as CPython's: a slot C writes steals C's reference, a slot Go changes holds a new one (the
  mirrors' own counted apart, so "C holds the only reference" stays a count of 1: `PySet_Add`'s brand-new frozenset).
  `PyTuple_New`/`PyList_New` give NULL items (a tuple's repr prints `<NULL>`, as 3.14's), `PyBytes_AsString` the
  mirror's `ob_sval` (C fills a new bytes in place).
- **PyType_Ready of C's type objects** (a static `PyTypeObject`, or one C filled after `PyType_Type.tp_alloc`): a pyi type
  made from its fields and slots (sub-structs by `Objects/typeslots.inc`'s offsets), its handle the C struct, the
  inherited slots, `tp_base`, `tp_bases`, `tp_mro`, `tp_dict` and `Py_TPFLAGS_READY` written back; a static type over
  object without `tp_new` disallows instantiation (`type_ready_set_new`).
- **C types over a built-in base** (268's DECISION (a), first part): a static type or a spec over list, tuple, bytes or
  float makes C-born instances: the memory `tp_basicsize` bytes, the built-in part a mirror of a pyi value (the Go value
  an instance of the type keeping it, `py.SetInstanceExt` the record), the tail C's; freed by its `tp_dealloc` once Go
  collects it (`PyList_Type.tp_dealloc` gives the items back). `_testcapi.MyList` works.
- **PyLong's layout** (reached: `_testcapi.call_long_compact_api`, `PyUnstable_Long_IsCompact` and `CompactValue` read it):
  an int's handle writes `lv_tag` (the 30-bit digits' count shifted by 3, the sign in the low bits) and the digits, once
  (ints are immutable); True's and False's structs are PyLongObject-sized; the small ints (-5 to 256, interned by pyi as
  by CPython) are immortal. The other type data the full API's macros name are bound to pyi's types of the same role
  (`PyFunction_Type`, `PyMethod_Type`, `PyCode_Type`, `PyFrame_Type`, `PyCell_Type`, `PyCFunction_Type`, the weak
  references'): an undefined data symbol is a zeroed static of ci's link, so a macro over an unbound one is silently false.
- **The world** (since this task): `Modules/_testcapimodule.c` and 16 parts: abstract, bytes, complex, dict, float, hash,
  immortal, list, long, numbers, object, pyatomic, set, structmember, tuple, weakref, in the full API's mode; the other 22
  parts' Init functions are stubs (`tools/pyi/clib/ctypestest/testcapi_parts.c`). `_testcapimodule.c`'s
  `_testcapi_feature_macros.inc`, included inside a function body, is inlined by `bin/pyi-clib`'s new `inline=` (ci's
  reader refuses a declaration split over files). `pyconfig.h` gained `HAVE_PTHREAD_H`, `HAVE_FORK` and gcc's `__atomic`
  builtins as plain accesses (ci's builder implements none and its checker takes the generic `__atomic_load`/`store` for
  prototyped functions; one thread: a REQUEST for the C crop). `pycapi.c` defines `Py_Version`, `PyInstanceMethod_Type`
  (a pyi `instancemethod`) and `PyGen_Type`. ci's `char` is signed (x86-64), so `_testcapi.CHAR_MAX` is 127 as on the main
  host's amd64 CPython (the VM's aarch64 python3 gives 255).
- **Natives** for the parts: `PyErr_NewException`, `PyModule_GetState` (`m_size`'s state), `PyState_AddModule`,
  `PyCallable_Check`, `PyObject_CallFunction`/`CallMethod`, `PyList_Clear`/`Extend`, `PyBytes_Join`, `_PyObject_New`,
  `_PyArg_CheckPositional`, `_PyArg_ParseStack`, `PyArg_UnpackTuple`, `PyWeakref_GetRef`/`IsDead`, `Py_HashPointer`,
  `PyOS_string_to_double`, `PyFloat_Pack2/4/8` and `Unpack2/4/8` (half precision rounded half to even, its OverflowError),
  `_PyTuple_Resize`, `_PyBytes_Resize`, `PyCFunction_Call`, `PyObject_GenericGetAttr`/`SetAttr`, `PyObject_GC_Del`, getargs'
  `c` and `C`; `PYI_CAPI_TRACE=1` names each native on stderr as C calls it.

Counts (`bin/pyi lib-test-all --fresh`, each test's own verdict, the VM; base 1ead003e3 as queue 268's integration measured it):

| module | base | head |
|---|---|---|
| test_capi (1,195) | 37 pass; `:testcapi` 1,154, cpython-only 3, skipped-by-test 1 | 258 pass; `:testcapi-parts` 548, `:testcapi` 262, `:capi-port` 112, `:capi-refcounts` 3, cpython-only 3, skipped-by-test 3, finalization 2, gc-collector 2, int-core 2; 0 failing |
| test_call (189) | 7 pass; `:testcapi` 130, cpython-only 52 | 125 pass; cpython-only 52, `:testcapi-parts` 11, `:capi-port` 1; 0 failing |
| test_bytes (325) | 308 pass; `:testcapi` 3 | 310 pass (its two gh-140939 tests run: `%`'s padding past the allocation limit is MemoryError now, `percent.ae`); 0 failing |
| test_ctypes (621), test_frame (61) | 531 pass; 48 pass, 2 failing | the same |

`:testcapi-parts` (new, deferred) takes a test reaching a part not built (`module '_testcapi' has no attribute`, `cannot import
name ... from '_testcapi'`: test_misc's 309 and getargs' importers' 106 among them); `:capi-refcounts` (new, design) the
checks of `Py_REFCNT` > 1 and of immortality, since a handle counts C's references alone; `:capi-port`'s `:tests` the
limited-API natives' gaps that test_capi's modules show now that they import (they stopped at `_testcapi` before).

**The check** `.tmp/q269/t/check269.sh ROOT` (the report's appendix): `probe269.py`, 34 lines (tuple, list, bytes and float
through their macros, NULL items, `_PyTuple_Resize`, `_PyBytes_Resize`, the pack/unpack natives, the module's constants,
`MyList`, the static types' `PyType_Ready`, the compact ints, weak references) against python3 3.14.10's own `_testcapi`;
its negative control judges the run against a mutated reference, which must fail. PASS on the head; on the base the module
does not exist.

**What remains**: heaptype.c (268's DECISION (a)'s second half: specs over `type` for C metaclasses, whose instances are pyi
types with a C tail, over int and over exceptions), the other 21 parts (vectorcall, unicode with Q3's compact layout, getargs,
...), the `:capi-port` gaps above; a mirror is synchronized when a native receives it or the world returns, so Python code a
native runs that changes a mirrored object the native was not given is not seen by C until the next synchronization (none of
the sampled tests does it), and each synchronization costs the mirror's size.

## As built: 270, the compact unicode layout, getargs and the buffer protocol (2026-11-12, the Mac VM, queue 270; 262's DECISION (a), the VM's proposal Q3)

The third entry of 262's DECISION (a): `_testcapi`'s unicode and getargs parts, over a str's CPython layout and a port of
the rest of `Python/getargs.c`. Ported from CPython 3.14.10's `Objects/unicodeobject.c` (`PyUnicode_New`, `PyUnicode_Fill`,
`PyUnicode_CopyCharacters` and `_copy_characters`, `PyUnicode_FromKindAndData`, `as_ucs4`, `unicode_kind_name`,
`unicode_check_modifiable`, the `PyUnicodeWriter` API), `Python/getargs.c` (`converttuple`, `convertitem`, `convertsimple`'s
D S Y U O& w* s* z* y* es et es# et#, `convertbuffer`, `getbuffer`, `convertcharerr`, `seterror`'s item levels, `skipitem`,
the freelist) and `Objects/abstract.c`'s buffer protocol.

- **A str's handle is a mirror** (`go/arbace/pyctypes/capi_unicode.ae`): the layout CPython would have made for its
  characters, in their canonical kind (`PyASCIIObject` and the ASCII characters of a compact ASCII str, a
  `PyCompactUnicodeObject` and 1, 2 or 4 bytes a character for another exact str, a `PyUnicodeObject` whose `data.any`
  points after it for a str subclass's instance: CPython's legacy form, `PyUnicode_IS_COMPACT` 0), so `PyUnicode_GET_LENGTH`,
  `PyUnicode_KIND`, `PyUnicode_DATA`, `PyUnicode_IS_ASCII`/`IS_COMPACT`, `PyUnicode_READ`/`READ_CHAR` and
  `PyUnicode_MAX_CHAR_VALUE` read it in place; `state.interned` is 0 (pyi's interning is not CPython's). A str is immutable,
  so Go writes its mirror once; the exception is a str `PyUnicode_New(size, maxchar)` made (the kind maxchar names, NUL
  characters): C fills it in place (`PyUnicode_WRITE`, `Fill`, `CopyCharacters`, memcpy) and its characters are copied
  back to the pyi str at each synchronization (`py.Str.SetRunesInPlace`, the one mutation of a str, before Python sees it).
  `Fill` and `CopyCharacters` refuse a str that is not such a new one held by C alone (`unicode_check_modifiable`'s
  SystemError).
- **getargs.c's rest** (`capi_getargs.ae`, over 262's `vgetargs` and 268's `vgetargskeywords`): nested tuples with the
  failing item's levels in the message (`argument 1, item 0`), their DeprecationWarning for a non-tuple sequence, the units
  D, S, Y, U, O& (the converter called), w*, s*, z*, y* (a `Py_buffer` filled, released on a later unit's failure), es,
  et, es#, et# (the encoded string in a `PyMem` block or the caller's buffer, its "encoded string too long"), c's and C's
  length messages, `convertbuffer`'s "read-only bytes-like object" for bytearray and memoryview.
- **The buffer protocol**: `PyObject_GetBuffer` (a bytes' mirror's `ob_sval` in place; another bytes-like object's
  contents copied into the world, written back by `PyBuffer_Release` when writable), `PyBuffer_FillInfo`,
  `PyObject_CheckBuffer`.
- **Fixes the tests showed**: `Python/pyctype.c`'s tables (`_Py_ctype_table`, `_Py_ctype_tolower`/`toupper`) and
  `_PyByteArray_empty_string` defined in `pycapi.c` (an undefined data symbol is a zeroed static of ci's link: `Py_ISALNUM`
  faulted at `0x13c`, 4 times 'O'); getbuffer's pending exception kept (CPython's seterror leaves it: a non-contiguous
  memoryview's BufferError, a non-buffer's TypeError); `PyFloat_FromString` refuses a number; `PyFloat_Pack2/4` and
  `Unpack2/4` keep a NaN's type and payload bit by bit (Go's float32 conversion quiets a signaling NaN on arm64); `float()`
  refuses a non-contiguous memoryview (`py/float.ae`); a keyword of a str subclass is a str; an invalid UTF-8 name of the
  keyword list is new_kwtuple's UnicodeDecodeError.
- **bytearray's and dict's layouts**: a bytearray's handle is a mirror too (`PyByteArrayObject`: `ob_alloc`, `ob_bytes`,
  `ob_start`; synchronized both ways as a bytes', so `PyByteArray_AS_STRING` is its storage), a dict's handle carries
  `ma_used` (`PyDict_GET_SIZE`).
- **The world**: `Modules/_testcapi/unicode.c` and `getargs.c` built (18 of the 38 parts).
- **`bin/pyi-clib`** (the coordinator's item): ci is run from the checkout with every source, glue file and `-I` relative
  to it, so `__FILE__` (assert's messages) and the statics' sizes no longer name the checkout's location: two checkouts at
  different paths build the same `nodes.ae`. The main host's next rebuild of ctypestest changes `nodes.ae` once for it.

## As built: 270b, heaptype.c, the other `_testcapi` parts and `:capi-port`'s named natives (2026-11-12, the Mac VM, queue 270b; 268's DECISION (a), second half; the VM's proposal Q3)

The second part of queue 270: `_testcapi`'s heaptype.c (the second half of 268's DECISION (a): C metaclasses, specs over
int and over exceptions), 17 more of its parts, test_getargs' DeprecationWarning rows and `:capi-port`'s named natives.
Ported from CPython 3.14.10's `Objects/typeobject.c` (`_PyType_CalculateMetaclass`, PyType_FromMetaclass's metaclass
checks, `inherit_special`'s itemsize, `PyType_GetBaseByToken`, `PyType_GetModuleByDef`, `PyType_Freeze`,
`_PyType_GetTextSignatureFromInternalDoc`, `solid_base`'s `extra_ivars`), `Objects/dictobject.c` (the managed dict,
`_PyObject_ComputedDictPointer`'s negative offset, the `Ref` and `Pop` functions), `Objects/abstract.c`,
`Objects/longobject.c` (`PyLong_AsNativeBytes`, `FromNativeBytes`, `GetNativeLayout`), `Objects/funcobject.c`,
`Python/codecs.c`, `Python/pytime.c`, `Python/errors.c`, `Python/import.c` (`PyImport_ExecCodeModule*`), `Python/ceval.c`
(`PyEval_EvalCodeEx`).

- **Metaclasses** (`go/arbace/pyctypes/capi_types.ae`): PyType_FromMetaclass takes a metaclass other than type, the
  winner of the given one and the bases' (`_PyType_CalculateMetaclass`: "metaclass conflict: ..."), refusing one with a
  tp_new of its own ("Metaclasses with custom tp_new are not supported."); the type's `Meta` is it. A spec over `type`
  (`HeapCTypeMetaclass`, sizeof(PyHeapTypeObject)) is a C metaclass: its instances are pyi types (type's own slots,
  `type.__new__` with the metaclass), their handle `PyHeapTypeObject` plus the metaclass's C tail when its basicsize is
  larger; Py_TPFLAGS_DISALLOW_INSTANTIATION refuses `HeapCTypeMetaclassNullNew(...)` as CPython's type_call.
- **Specs over int, exceptions and Python classes**: a spec over int (`HeapCTypeWithBasesSlot`) or an exception
  (`make_type_with_base(ValueError)`) makes C-born instances as 269's over list/tuple/bytes/float: an int's PyLongObject
  mirrored, an exception's fields left to the functions (no mirror; the record on `Exception.Ext`); the built-in value made
  by the base's `__new__` from the arguments (`HeapCTypeWithBasesSlot(5)` is 5; before, a spec over float without tp_new
  gave 0.0). A Python class over object as a base (`test_from_spec_metatype_inheritance`) gives the spec's instances a
  managed dict and weak references, as CPython's.
- **The managed dict and weak references** (Py_TPFLAGS_MANAGED_DICT, MANAGED_WEAKREF): a C-born object's dict is a Go
  dict beside its memory (`PyObject_GenericGetDict`/`SetDict`, `PyObject_ClearManagedDict`, `PyObject_VisitManagedDict`);
  a negative `tp_dictoffset` counts from the object's end; the head of an object's weak references is written into its
  `tp_weaklistoffset` slot (py's `ExtWeakrefHook`), so the C member `weakreflist` reads it; a C type whose basicsize is its
  base's adds no layout (`ExtAddsNoLayout`: `class C(HeapCTypeWithManagedWeakref, HeapCTypeWithManagedDict)` and
  `class C3(HeapCTypeWithManagedDict, list)` are layouts CPython accepts).
- **The rest of heaptype.c**: bf_getbuffer/bf_releasebuffer for pyi's buffer users (`ExtBufferHook`: `bytes(obj)`,
  `memoryview(obj)`) and for PyObject_GetBuffer; Py_tp_vectorcall (`ExtTypeCallHook`: the type called through it);
  `tp_doc` (the spec's copy), `ht_name`/`ht_qualname`, `__text_signature__` from tp_doc (`ExtTextSignatureHook`); tokens
  (`Py_TP_USE_SPEC` is NULL: the spec itself), `PyType_GetBaseByToken`, `PyModule_GetDef`, `PyType_GetModuleByDef`;
  `PyType_GetSlot` of `Py_tp_base` and of a static type's slots from the type object; the spec's itemsize and
  Py_TPFLAGS_ITEMS_AT_END inherited (`subclass_var_heaptype(HeapCCollection, ...)`); a spec without
  Py_TPFLAGS_IMMUTABLETYPE is mutable (`ExtMutableType`), `PyType_Freeze` sets py's new `FlagImmutable`.
- **DeprecationWarning** (py's `class.ae`): `__index__`, `__int__` and `__float__` returning an instance of a strict subclass
  of int or float warn as CPython's (`_PyNumber_Index`, `PyNumber_Long`, `PyNumber_Float`) and give the exact value
  (pyi gave none: test_getargs' 14 rows, which `:coroutines`' pattern took).
- **`:capi-port`'s named natives** (`capi_port.ae`, `capi_parts.ae`): the number protocol's rest (Index, Float, Long,
  the binary, in-place and unary operators, Power, ToBase, AsSsize_t), `PyLong_IsZero`/`IsPositive`/`IsNegative`/
  `GetSign`/`FromUnicodeObject`, `PyLong_AsNativeBytes`/`FromNativeBytes`/`GetNativeLayout` (CPython's compact path
  for one 30-bit digit kept: its byte counts), the optional lookups (`PyObject_GetOptionalAttr`,
  `PyMapping_GetOptionalItem`), the dict's `Ref`, `SetDefault` and `Pop` functions, `PyIter_NextItem`,
  `PyEval_EvalCodeEx` (an optimized code through `types.FunctionType`, a class body's in its locals),
  `PyComplex_FromCComplex` (ci passes the `Py_complex` aggregate's bytes), `PyRun_String`/`SimpleString`,
  `Py_CompileString` and their variants (`PyCF_IGNORE_COOKIE`), `PyImport_ExecCodeModule` and its variants
  (`_fix_up_module`), `PyType_Freeze`, the function accessors, the codec registry and error handlers, the clocks, the
  legacy error triple and the handled exception, the Unicode errors' start and end, `PyFrame_GetVar`, the charmap
  codec, `PyUnicode_FSConverter` (getargs' `O&` with `Py_CLEANUP_SUPPORTED` called again on failure: Issue 6012),
  `PyBytes_DecodeEscape`, `PyHash_GetFuncDef`, `PyCode_NewEmpty` (its `co_firstlineno` left: a Low),
  `PyFile_FromFd`, one thread state (`PyEval_SaveThread` a no-op).
- **The world**: heaptype.c and vectorcall, function, type, codec, exceptions, time, import, docstring, buffer, file,
  frame, code, config, datetime, run, gc and mem built (36 of the 38 parts); watchers and monitoring stay stubs (they need
  events from pyi's core: dict, type, function, code and context watchers; sys.monitoring's C API). `PyModule_AddType`
  readies a static type first (docstring.c's). `pycapi.c` defines `PyFrame_Type`, `PyCode_Type`, `PyFunction_Type`,
  `PyMethod_Type`, `PyCell_Type`, `PyCFunction_Type` and the weak references' type objects: an undefined data symbol is
  a zeroed static of each unit in ci's link, so frame.c's `PyFrame_Check` read another struct than the one pyctypes
  bound (269's Low on `PyWeakref_Check` through `_testlimitedcapi` is the same cause). Static types inherit
  `tp_vectorcall_offset`, Py_TPFLAGS_HAVE_VECTORCALL and METHOD_DESCRIPTOR from a C base (`inherit_slots`), a C type's
  `__flags__` is its type object's (`ExtTypeFlags`), type_new defers to a C metaclass (`ExtMetaclass`).

Counts (`bin/pyi lib-test-all --fresh`, each test's own verdict, the VM; base c3d19bdf5 as the coordinator's verify
measured it):

| module | base | head |
|---|---|---|
| test_capi (1,195) | 324 pass; `:testcapi-parts` 442, `:testcapi` 282, `:capi-port` 116, `:coroutines` 14, ... ; 0 failing | **445 pass**; `:testcapi` 592, `:capi-port` 77, `:testcapi-parts` 58, `:testcapi-behaviour` 4, `:capi-refcounts` 3, int-core 4, cpython-only 3, skipped-by-test 4, finalization 2, gc-collector 2, `:coroutines` 1; **0 failing** |
| test_call (189) | 125 pass; cpython-only 52, `:testcapi-parts` 11, `:capi-port` 1 | **129 pass**; cpython-only 52, `:testcapi-behaviour` 4, `:testcapi` 3, `:capi-port` 1; 0 failing |
| test_descr (163), test_exceptions (113) | (269's integration at a53fe65a9: one new failing each, `_testcapi`'s) | 144 pass, 0 failing; 90 pass, 0 failing (`:testcapi-behaviour` 1 each) |
| test_dataclasses, test_float, test_complex, test_codecs, test_bytes, test_str, test_ctypes, test_format | as the base | 277, 43, 34 (and the base's 3 failing), 266, 310, 128, 531, 10 pass: as the base |

`:testcapi` grows by test_capi's test_misc (309) and test_exceptions (32), which import past heaptype.c's
`HeapCTypeSubclass` now and stop at `import _testinternalcapi` (by design). **The checks** (`.tmp/q270b/t/`): `check270h.sh`
(heaptype.c, 44 lines), `check270p.sh` (`:capi-port`'s natives and the new parts, 77 lines), `checkdw.sh` (the
DeprecationWarning, 4 lines), each against python3 3.14.10's own `_testcapi` with a negative control.

## As built: 274, the C API's watchers: dict, type, function, code and context events as hooks in pyi's core; `_testcapi`'s watchers.c (2026-11-12, the Mac VM, queue 274; 270b's DECISION (a), the user's)

CPython 3.14.10's watchers (`Objects/dictobject.c` PyDict_AddWatcher/Watch/Unwatch/ClearWatcher and
`_PyDict_SendEvent`, `Objects/typeobject.c` PyType_AddWatcher/Watch and `type_modified_unlocked`,
`Objects/funcobject.c` `notify_func_watchers`, `Objects/codeobject.c` `notify_code_watchers`, `Python/context.c`
`notify_context_watchers`) ported over pyi's objects:

- **Hooks in the core** (`go/arbace/py/watchers.go`): `DictWatch`, `TypeWatch`, `FuncWatch`, `CodeWatch`,
  `ContextWatch`, package variables nil until the C API sets them (pyctypes, while a watcher of the kind is
  registered). A dict carries its watcher bits (`watched`, CPython's `_ma_watcher_tag`), checked where the dict already
  writes: Set (ADDED; MODIFIED when the value changes), the instance-attribute fast path (MODIFIED always, CPython's
  `store_instance_attr`), Delete and popitem (DELETED), Clear of a non-empty dict (CLEARED), Update of a compact dict into
  an empty watched one (one CLONED, the other dict as key). A type carries `watched` and `versioned` (CPython's
  `tp_version_tag`, kept only once a type was watched, `typeWatching`): PyType_Watch and a type's attribute lookup
  (`typeGetAttr`) set the tags of the type and its MRO; a modification (`type.__setattr__`, the C API's
  PyType_Modified) clears them, subclasses first, and reports a watched type once until its tag is set again (the
  events' aggregation). Functions: CREATE where a function is made (MAKE_FUNCTION in the evaluator and in AOT code,
  `types.FunctionType`), MODIFY_CODE, MODIFY_DEFAULTS, MODIFY_KWDEFAULTS on the attributes' sets and
  PyFunction_SetDefaults/SetKwDefaults. Code: CREATE where a code object is made (the IR's loader: compile and import,
  marshal, `code.replace`). Contexts: SWITCHED on Context.run's enter and exit.
- **The C API** (`go/arbace/pyctypes/capi_watchers.go`): eight ids per kind (dict from 2, type from 1, as CPython
  reserves the lower ones), CPython's messages for invalid and unassigned ids and for "no more ... watcher IDs
  available", each callback's error written as unraisable with CPython's message; `_PyOnceFlag_CallOnceSlow`,
  `PyErr_FormatUnraisable`.
- **Not given** (classed): DEALLOCATED and DESTROY events (pyi's objects are Go's: no deallocation when the last
  reference goes, `:finalization`), `clear_context_stack` (watchers.c reads PyThreadState's `context` and PyContext's
  `ctx_prev` fields; pyi's Context.run always has a base context to return to).
- **Cost** (the core's checks where no watcher is registered): a Go micro-bench of 1,024 dict mutations (Set new, Set
  replaced, Delete, Clear) on the printed tree, base 9880b6113 against head, interleaved three times three: 32.45 µs
  against 32.81 µs per iteration (+1.1%, inside the runs' spread of 31.4-33.6 µs); the dict's bits sit beside its two
  flags, so a dict is no larger.
- **Counts** (`bin/pyi lib-test-all --fresh test_capi`, the VM): 445 → 496 pass; `:testcapi-parts` 58 → 0 (51 of
  test_watchers' 58 rows pass; 6 deallocation rows `:finalization`'s; clear_context_stack's `:testcapi-behaviour`'s);
  `:testcapi` 592 and `:capi-port` 77 unchanged; 0 failing. monitoring.c (sys.monitoring's C API, test_monitoring's
  TestCApiEventGeneration, 5 rows) is the next entry.

## As built: 274b, sys.monitoring's C API: `_testcapi`'s monitoring.c, the last of its 38 parts; `_contextvars`' messages and Context's equality (2026-11-13, the Mac VM, queue 274b; 270b's DECISION (a), the user's)

CPython 3.14.10's `Python/instrumentation.c` 2598-2920 (`capi_call_instrumentation`, `PyMonitoring_EnterScope`,
`PyMonitoring_ExitScope`, the `_PyMonitoring_Fire*Event` functions; the PSF's license) ported over pyi's sys.monitoring:

- **py** (`go/arbace/py/monitoring.go`): the interpreter's instrumentation version (`monVersion`), raised when a tool's
  global events change (`monUpdate`, against the events last seen: `_PyMonitoring_SetEvents` leaves the version alone
  when the events are the same); `MonitoringTools(ev)`, `_Py_GlobalMonitors`' `tools[ev]` (an extension's tool's
  events as `monitoring_set_events_impl` stores them: BRANCH as BRANCH_LEFT and BRANCH_RIGHT, C_RETURN and C_RAISE left
  out); `MonitoringCall`, `call_one_instrument` (the thread's tracing count up, DISABLE told apart); the event names.
- **pyctypes** (`go/arbace/pyctypes/capi_monitoring.go`): `PyMonitoring_EnterScope` writes each `PyMonitoringState`'s
  `active` byte (the world's memory, which `monitoring.h`'s inline wrappers read) when the scope's version is not the
  interpreter's; the 17 `_PyMonitoring_Fire*Event` call the active tools' callbacks, the most significant first, with
  `(codelike, offset, ...)` (LINE: `(codelike, lineno)`); DISABLE clears the tool's bit for the local events and is
  refused for the others ("Cannot disable %s events. Callback removed.", the callback cleared); the exception events
  take the raised exception (a ValueError "Firing event %d with no exception set" without one) and raise it again when
  the callbacks succeed; STOP_ITERATION makes its StopIteration as PyErr_SetObject does.
- **pyi's own monitoring** (test_monitoring's cascade: two failing tests left a tool's events set, so every later
  class's setUp failed, TestCApiEventGeneration's included): C_RETURN and C_RAISE follow CALL (CPython's ancillary
  events; `set_events` stores the set as `monitoring_set_events_impl` does: C_RETURN and C_RAISE dropped, BRANCH as
  BRANCH_LEFT and BRANCH_RIGHT); a callback's DISABLE turns its local event off at the location (code object, offset)
  for the tool, the others raise "Cannot disable %s events. Callback removed." and lose the callback;
  `restart_events` turns them on again.
- **The world**: `Modules/_testcapi/monitoring.c` built (lib.conf; `_testcapi.CodeLike`, a static C type);
  `tools/pyi/clib/ctypestest/testcapi_parts.c`, the stubs of the parts not built, removed: all 38 parts are built.
- **`_contextvars`** (`builtins_contextvars.go`; `Python/context.c`): ContextVar's argument checks (`"O|$O:ContextVar"`:
  the name positional-only, `default` keyword-only), the name hashed at creation (gh-132002: an unhashable str
  subclass refused), `set`/`get`/`reset`'s arity and keyword messages, the reset's messages naming the token by its
  repr, Token's repr (`<Token used var=...>`) and `Token()`'s refusal, Context's key checks for `in` and `get`,
  `get`'s arity, Context's equality (`context_tp_richcompare`: the mappings compared).
- **PyCode_NewEmpty** reports one code CREATE event (274's Low): the compiled step is made with the code watchers off.
- **Classes**: `:monitoring-events` (new, deferred, queue 220's owner): test_monitoring's 45 rows pyi's monitoring does not
  fire (the local events, LINE, INSTRUCTION, JUMP, BRANCH*, RAISE, RERAISE, EXCEPTION_HANDLED, `_all_events`,
  `co_branches`), the base's own; `:testcapi-parts` retired from divergences.edn (no part left to build), kept in
  divergences-315.edn for 3.15's tests reaching attributes of 3.15's `_testcapi` that the world's lacks;
  test_context's `test_reset_with_reentrant_finalizer` (`__del__` when `reset` drops the last reference) `:finalization`.
- **Counts** (`bin/pyi lib-test-all --fresh`, each test's own verdict, the VM; base 28a89513d): test_monitoring 12 pass,
  75 failing → **40 pass**, 49 classed (`:monitoring-events` 45), 0 failing (TestCApiEventGeneration's 5 pass, also
  alone); test_context 25 pass, 9 failing → **33 pass**, 0 failing; test_capi 496 pass, 0 failing, `:testcapi` 592,
  `:capi-port` 77, `:testcapi-parts` 0: unchanged; test_sys_setprofile 26 pass (the same); test_cprofile 26 pass.

## As built: 278, night 12's partial Lib/test run classed (2026-11-13, the Mac VM, queue 278)

Night 12's amd64 eval run (`bin/pyi check --fresh` on a53fe65a9 + next, stopped by the user at 182 of 529 modules; the
queue's `data/night12-py-run.tar.xz`) judged again on mac/201 bdf8c85ea (`bin/pyi check --judge RUN`, no record: a
partial run is not recorded). Of the 181 modules the run finished, 549 methods were in no class; each has its cause now.

- **Fixed** (15 rows of the run, and 2 base rows):
  - the runner (`tools/pyi/libtest_all.py run`): each driver run in a working directory of its own, beside its TMPDIR
    (`TMPDIR + "c"`, made afresh each attempt, removed with the module), as regrtest's worker runs in its own; the run
    directory was every module's cwd, so `test.support.os_helper.temp_cwd()`'s `tempcwd` collided between the two jobs and
    after a killed driver ("[Errno 17] File exists: 'tempcwd'", 7 rows);
  - `from __future__ import annotations`' texts as `Python/ast_unparse.c` writes them (`arbace/python/compile.ae`'s
    annotation-text binding `arbace/repr/python/print.ae`'s new `*ast-unparse*`): a `u` prefix kept (the Constant's kind),
    a debug field as the parser's Constant and FormattedValue (`f'{x=!r}'` -> `f'x={x!r}'`, `!r` unless a conversion or a
    spec), an infinite float or imaginary literal as `1e309`, ast_unparse's precedences (a comprehension's tuple target bare,
    `1 .real`, a field's value at PR_TEST + 1, an if-expression's else at PR_TEST): test_future's 3 rows, every case of its
    `test_annotations` equal; the printer's round trip unchanged (`bin/repr-python roundtrip samples`: 6 of 6 at the three
    levels). Left: other numbers keep their spelling where ast_unparse writes the value's repr (`1_0.0e1_0`);
  - the runtime (`go/arbace/py`, through `bin/pyi runtime print`/`read`): `select.epoll.fromfd` a classmethod; a class's
    `__module__` set drops its `__firstlineno__` (`type_set_module`: inspect.getsource of `_py_abc`'s ABCMeta raises
    OSError); `types.MappingProxyType` of a dict subclass keeps the mapping (OrderedDict's repr and order-sensitive
    equality: inspect's `Signature.parameters`), an exact dict still by its storage; `__hello_only__` frozen
    (Tools/freeze/flag.py beside Lib/, no origname, `__origname__` only for a module that has one); an exact float's and
    complex's `from_number` is itself; a class's `__class_getitem__` of None makes it not subscriptable
    (`PyObject_GetItem`): 5 rows of the run, test_complex's test_from_number and test_genericclass' row.
- **Classed** (534 rows, `divergences.edn` and the copy part of `divergences-315.edn` alike): `:fork` +283 (os.fork's
  AttributeError by pattern; the not-run methods of classes whose setUpClass forked; the fork bomb's child); `:data-model`
  +185 (no method-wrapper, method_descriptor or classmethod_descriptor types, so inspect takes bound methods and
  classmethods for built-ins, "no signature found for builtin" by pattern; no `__text_signature__` on built-ins; int's
  getsets not on the type; multiprocessing.reduction's method-descriptor reducer registered for
  builtin_function_or_method, so the spawn and forkserver managers' setUpClass fails on `__objclass__`, 164 methods not
  run); `:memoryview-formats` +22 (assignment to a non-byte format, by pattern); `:finalization-collect` +18 (objects
  held after gc_collect); `:multiprocessing` +12 (the forkserver's server dies on os.fork: "unexpected EOF");
  `:finalization` +4; `:child-is-pyi`, `:slow-on-goi`, `:c-accelerators`, `:pyi-command-line` +1 each; new, deferred:
  `:stdio-text` (5: pyi's standard streams have no `reconfigure`, test_inspect's TestMain), `:host-memory` (1:
  test_many_expressions killed by the host's memory). The base's own rows: `:settrace-events` (new, deferred:
  test_sys_settrace's 7, test_20_async_for_loop's flake among them) and `:complex-core` (new, deferred: test_complex's
  test_mul and test_pow).
- **Samples** (`bin/pyi lib-test-all --fresh`, each test's own verdict, the VM): test_epoll 9 -> 10 pass, test_tstring 15
  -> 15 (its tempcwd row passes alone, the probe), test_future 20 -> 23, test_inspect 288 -> 291 (33 failing -> 0 with
  the classes), test_complex 34 -> 35, test_genericclass 20 -> 21, test_descr 144 pass, 0 failing; test_sys_settrace 0
  failing with `:settrace-events`. `:testcapi` 54 in the run's modules, unchanged; `:testcapi-parts` (3.14 retired) and
  `:capi-port` 0 in them.
- **A whole run still needs**: the 347 modules night 12 did not reach (test.test_multiprocessing_spawn.* onwards, through
  test_zstd), and 1,821 `:runner-limits` rows of the run (test_pickle 477, test_subprocess 297, test_socket 246, test_os
  202, test_asyncio.test_events 131, ...) judged once 275's per-method timeout and fixes let them run.

## As built: 279, the AOT record's dry run on arm64 and Lib/test compiled (2026-11-13, the Mac VM, queue 279)

The user (2026-11-12): the AOT cell (`bin/pyi check --aot`, `test/python/check-aot-linux-amd64.edn`, reap's `:pyi-aot`)
has never been recorded on amd64; run it on arm64 first as a dry run of the record's tooling. Base 201 bdf8c85ea.

- **Two things**: the AOT cell's record is the 151 programs (synthetic corpus and samples); Lib/test by AOT was only
  `lib-test --aot`, the driver compiled and every test module on its loop. **`bin/pyi check --aot --lib-test
  [check's options] [MODULE...]`** (new) runs bin/pyi check's run and judge with each module's driver compiled with the
  module ahead: `test/python/libtest_run.py` plus a never-run module-level `import MODULE` (`if sys.argv is None:`), so
  the module and its static imports are compiled ahead and the driver's own import at run time (after regrtest's setup)
  takes them compiled (AotRegister's key is the source's; probed: native 8 code objects against 2 without the import).
  Each driver's compile seconds and peak memory in `RUN/aot-compile.tsv`, each driver run's in `RUN/MODULE.mem`; a
  module not built is written as its IMPORT failure (not left to the loop); the run in `WORK/check/run-aot`, judged
  against `PYI_CHECK_AOT_LIB_RECORD` (none committed); the drivers (about 220 MB each) removed after the run.
  `bin/pyi aot-compile PKG FILE OUT`: one program compiled. The runner takes `PYI_LIB_TEST_DRIVER_DIR` (DIR/MODULE).
- **The record's tooling**: `PYI_CHECK_AOT_RECORD` names check --aot's record (a dry run's scratch file; the committed
  path was the only one).
- **Fixed on the way**: the register generator put the dispatch label in a function whose only AotExec operation is
  dead code ("label dispatch defined and not used": test_exception_variations did not build); a compiled driver ran
  without bin/pyi run's environment (sys.executable '', every script_helper child "[Errno 13] Permission denied: ''"); a
  compiled `reraise N` reported the reraise's lasti, f_lineno 0 after a handler's re-raise (AotReraise(f, i) as
  OpReraise: test_exceptions' PEP626Tests, 7 methods, compiled only).
- **The sample's runtime gaps** (compiled and on the loop alike, modules with no eval run on record): eight classes
  before `:skipped-by-test` (`:property-core`, `:with-exit-traceback`, `:yield-from-iterator-value`,
  `:dict-views-set-methods`, `:contains-none-blocks`, `:wrapper-attrs-shared`, `:setattr-str-subclass`,
  `:entry-frame-teardown`; 35 methods); test_sys_settrace's 6 left to 278.
- **Compile in parallel**: `PYI_AOT_LIB_JOBS` drivers at once (default `PYI_LIB_TEST_JOBS`), the first alone (a new
  generator's packages dropped and made once), a package copied into the module through a name of its own; the AOT Go
  cache trimmed after the run (`aot_cache_trim`). The judge of `check --aot` named a classed run `:differs` (it read
  the class map's `:class`, the name is `:id`): del.py is now recorded `:finalization-goi` (reap re-classes by id, so the
  matrix did not see it).
- **Measured on the VM** (arm64, load about 13 from three agents; run): `bin/pyi check --aot --record` into a scratch
  record, 151 programs: **150 pass, 1 classed (`:finalization-goi`), 0 failing**, the control failing as it must,
  1 h 10 min, peak 3.0 GB (pyi on goi 56 min of it, about 25 s a program; the compiles 13 min after a generator change,
  the compiled runs 9 s, the loop 5 s); copied in as the amd64 record, `bin/reap` reads it: AOT 150 / 0 / 1, the eval
  cell's synthetic sources 150 / 1 / 0; its only problems the three stale `:until "PY8 ..."` and test/reap/matrix.edn
  (`bin/reap --record` after the record). `check --aot --lib-test` over 47 modules (46 language-core and builtins
  modules and test_capi, no process or thread module): **4,731 methods, 3,769 pass, 954 classed, 8 failing** (6 of
  test_sys_settrace and test_fstring's test_many_expressions, 278's; test_exceptions' test_recursion_error_cleanup, a
  flaky weakref timing test), 29.5 min at 2 jobs: the drivers 379 s (about 8 s each, 1.1-2.3 GB), the runs 23 min,
  dominated by the modules whose children are pyi on goi (test_exceptions 21 min, two tests at the 300 s cap;
  test_fstring, test_coroutines, test_frame, test_capi, test_getopt, test_list); the rest 1-15 s each against
  3-12 min on goi (night 12: test_class 485 s, test_descr 687 s, test_patma 189 s; compiled 1-5 s). test_capi compiled:
  496 pass, `:testcapi` 592, `:capi-port` 77, `:testcapi-parts` 0, as 274b's eval.

## As built: 275, why the process and thread modules take an hour: the runner's hang timeout and the measures (2026-11-13, the Mac VM, queue 275)

Night 12's eval run (amd64, 2 jobs) stopped after 182 of 529 modules in 18.6 h; 13 modules hit the 3600 s cap. Each of
the task's seven was run whole on the VM (base bdf8c85ea, beside two other agents' pyi runs, load 5-14 on 8 cores),
each test's wall and the CPU of the driver's processes (pyi and its children) measured:

| module | night 12 | VM: tests' wall (CPU) | class |
|---|---|---|---|
| test_multiprocessing_fork.test_misc | 3601 s cap, 5 timeouts | 1146 s (1804 s), none | child starts: 8 tests 78-186 s take 1096 s |
| test_concurrent_futures.test_shutdown | 3392 s, 5 timeouts | 722 s + one hang | `test_del_shutdown` hangs (idle: 9 s CPU in 229 s); the rest child starts |
| test_signal | 3602 s cap, 3 timeouts | 1496 s (2610 s), none | child starts: 25 tests take 1479 s |
| test_os | 3603 s cap, 3 timeouts | 1239 s (2191 s), none | child starts: 25 tests take 1073 s |
| test_str | 1179 s | 942 s (1626 s) | the eval loop: find_periodic_pattern 275 s, index 158 s |
| test_unicodedata | 1500 s | 797 s (1534 s) | the eval loop: no_names_in_pua 155 s and 181 s (CPython 0.2 s) |
| test_pickle | 3601 s cap, 1 timeout | 2903 s (3661 s), none | the eval loop: 1014 tests, 7 past 60 s take 1106 s (multiple_unpicklings 168-240 s) |

- **No busy loop** was found: every test past 60 s burned one to two and a half cores of work, its own or its children's.
- **One hang**: `test_del_shutdown`'s `del executor` fires no weakref callback under goi (`:finalization-goi`: goi's
  registers keep the value), so the workers wait on their queue forever; compiled (`bin/pyi aot`) all five end at once.
- **The night's timeouts of tests with children do not reproduce here**: they pass in 31-186 s (a child is a pyi start:
  31 s wall and 60 s CPU alone, two to four times longer under load); also under the night's runner (the AOT
  `p_libtest_all`). The next main-host run's `MODULE.times` (below) tells a hang from a slow start there.
- **Where a start goes** (`ARBACE_CPUPROFILE` of `bin/pyi run` of one line): 58% Go's collector (GOGC=50 over a 2 GB heap
  of goi's built runtime), 10% the reader of the runtime's forms; GOGC=200 cuts the CPU by 35% (wall 13%) for 0.9 GB
  more. Each clib-backed import builds its interpreted package again per process: `_bz2` 9.7 s, `_lzma` 8.7 s, `_zstd`
  15.9 s, `_testcapi` 18.8 s, `pyexpat` 25.3 s, `_sqlite3` 34 s; `zipfile` (three of them) 42 s, so every driver start
  of a module that imports test.support.script_helper pays it. The eval loop under goi: test_no_names_in_pua's loop is
  ~70x the compiled loop's (8.9 s against 0.13 s for 20,000 code points; compiled 0.88 s for all 137,469, CPython 0.20 s).
  Remedies proposed, not built: W2 (pyi's runtime in goi's table: no forms read and built at a start, the loop
  compiled), goi's built packages kept across processes for the clib modules, GOGC raised for a child's start.

**The runner** (`tools/pyi/libtest_all.py run`): `--idle-timeout S` (bin/pyi passes `PYI_LIB_TEST_IDLE_TIMEOUT`, default
90; 0 off): every 5 s the CPU of the driver's session and its descendants (/proc); a test under 5% of one core for S
seconds is a hang, killed then and written `timeout the test idle past 90 s, a hang (killed; the driver started again)`
(`:runner-limits` matches it); a busy test keeps `--test-timeout` (300 s). `DIR/MODULE.times`: each test's ID, outcome,
wall and CPU seconds, and a `#start` line a driver start (its import). A/B under the same load: test_del_shutdown's
run 401 s on the base (killed at 240 s) and 270 s with it (killed idle at 94 s).

## As built: 281, sys.monitoring's local, line, jump, branch, instruction and exception events (2026-11-13, the Mac VM, queue 281)

CPython 3.14.10's `Python/instrumentation.c` (`monitoring_set_local_events_impl`, `_PyMonitoring_SetLocalEvents`,
`monitoring__all_events_impl`, `_PyMonitoring_RegisterCallback`'s branch handlers, `branchesiter_next`,
`call_instrumentation_vector`'s order) and `Python/bytecodes.c`'s instrumented instructions (INSTRUMENTED_JUMP_FORWARD and
_BACKWARD, _POP_JUMP_IF_*, _NOT_TAKEN, _FOR_ITER, _POP_ITER, _END_ASYNC_FOR, monitor_reraise, monitor_handled; the PSF's
license) ported over pyi's IR and its bytecode view:

- **Local events** (`go/arbace/py/monitoring.go`): a code object's events by tool (`Code.monLocal`, nil for none), each
  with the tool's version when set: `clear_tool_id` and `free_tool_id` raise the tool's version, which turns them off
  while `get_local_events` still reads them (CPython's `tool_versions`); `set_local_events`' checks and messages (a code
  object, the tool, C_RETURN/C_RAISE only with CALL, BRANCH as BRANCH_LEFT and BRANCH_RIGHT, "invalid local event set
  0x%x", the tool in use); the codes with local events are a registry whose union `monEvents` holds, so the hot path's
  checks are the same loads as before. A tool fires for an event when its global events or the frame's code's local
  events have it; the tools are called most significant first (the legacy tracer, then the profiler, then tools 5 to 0).
  `set_events` refuses C_RETURN or C_RAISE without both and CALL, as CPython does.
- **DISABLE** at the instruction (the code, the event, its location: the IR instruction for LINE, the view's offset for
  INSTRUCTION, JUMP and BRANCH); an event turned off and on again (set_events, set_local_events) is instrumented anew, its
  disabled locations dropped. `register_callback(BRANCH)` installs CPython's legacy branch handlers on BRANCH_LEFT and
  BRANCH_RIGHT: a DISABLE turns off the other side at the source too (a FOR_ITER's right not from its left), and a side
  whose source instruction is no longer instrumented answers DISABLE without calling ("Already disabled").
- **JUMP, BRANCH_LEFT, BRANCH_RIGHT, INSTRUCTION** from the loop (`trace.go`'s `traceInstr`, frames on the loop while a
  tool has LINE, INSTRUCTION, JUMP or BRANCH): the view (`bytecode_asm.go`) keeps its instructions (offset, opcode,
  size, jump target, the IR instruction each came from); the event of a jump is decided where the jump leads, before the
  destination's LINE: an IR jump the view has a JUMP_FORWARD or JUMP_BACKWARD for fires JUMP (its offset, its target), a
  conditional jump the view has a POP_JUMP_IF_* for fires BRANCH_RIGHT when the view's jump is taken (the sense compared:
  the view may invert the IR's) or BRANCH_LEFT past the NOT_TAKEN, a FOR_ITER BRANCH_LEFT on a value and BRANCH_RIGHT
  past END_FOR and POP_ITER at the end, an async for's END_SEND BRANCH_LEFT past its NOT_TAKEN and its END_ASYNC_FOR
  BRANCH_RIGHT from the END_SEND. INSTRUCTION fires for the view's instructions an IR instruction came from (NOT_TAKEN and
  POP_ITER on their path, RESUME never), and for the view's instructions of no IR instruction just before them on a
  straight path (PUSH_NULL). `code.co_branches()` is `branchesiter_next` over the view. A JUMP or BRANCH callback runs
  with f_lasti the jump and may set f_lineno (CPython's what_event, now set for every monitoring callback).
- **Exceptions** (`eval.go`'s unwinding): RERAISE where pyi re-raises (a bare raise, the IR's reraise, END_ASYNC_FOR's),
  EXCEPTION_HANDLED where a handler is entered, and for a generator's or coroutine's frame left by an exception the
  handled-and-reraised pair of the handler codegen wraps its body in (the view's trailing CALL_INTRINSIC_1, RERAISE).
- **CALL's callable and first argument** as the instrumented CALL has them: a bound method expanded into its function and
  self (`_MAYBE_EXPAND_METHOD`), a built-in method or slot wrapper bound at the attribute load (pyi's IR binds where
  CPython's method form keeps the descriptor and self) with its object.
- `_all_events()` is each ungrouped event's tools with it among their global events.
- **The view**: LOAD_SUPER_ATTR only when the module's scope has no symbol super (`can_optimize_super_call`; test_monitoring's
  TestLoadSuperAttr checks it).
- **Classes**: `:monitoring-events` retired (both files). Of its 45 rows 41 pass; test_generator_with_line `:finalization`
  (its last LINE is CPython's close of a yield-from generator at its last reference), test_match and test_except_star
  `:dis` (the view's offsets: a match's value patterns keep the subject's COPY and POP_TOPs, except* lacks a LIST_APPEND),
  test_vs_other_type_call `:data-model` (no method-wrapper type: super().__repr__'s bound slot wrapper is a method like
  int().__repr__'s, CALL's arg0 self where CPython's is MISSING).
- **settrace** (the line machinery's other user): a jump (f_lineno) from a yield's event goes on at the target, the value
  not yielded (INSTRUMENTED_YIELD_VALUE's instr_ptr check): test_jump_from_yield passes. `:settrace-events` narrowed to
  test_20_async_for_loop's flake: test_13_genexp (four rows) and test_reentrancy are `:finalization`'s (a generator any()
  leaves is closed at its last reference, its GeneratorExit and finally traced; `__del__` sets the trace function;
  `.tmp/q281/t/pf.py`: with a reference kept CPython gives pyi's events).
- **Cost** (`bin/pyi aot` of `.tmp/q281/t/bench281.py`: a loop with a call, a branch and a caught exception; base 08b32e300
  and head built alike and run interleaved, CPU seconds, the VM shared): no tool, compiled: median 0.378 s base, 0.388 s
  head (8 runs each, spread 0.336-0.518); on the loop (PYI_AOT_LOOP): 0.664 against 0.659 (spread 0.608-0.900): inside the
  noise, the checks with no tool being the same loads of `monEvents` (the local events in it). With a LINE tool (300,000
  iterations, every frame on the loop): 0.907, 1.110, 1.145 base against 0.714, 0.638, 0.652 head compiled-built, 0.992,
  0.943, 1.090 against 0.645, 0.665, 0.751 on the loop: head faster, the base making an INSTRUCTION and a JUMP call's
  arguments at every instruction for no tool.
- **Counts** (`bin/pyi lib-test-all --fresh`, each test's own verdict, the VM; base 08b32e300): test_monitoring 40 pass, 49
  classed (`:monitoring-events` 45) → **81 pass**, 8 classed (4 skipped by the test, 4 above), 0 failing; test_sys_settrace
  365 → **366** pass, 0 failing; test_bdb 36 → 36 (its one row, test_skip, the base's own, classed); test_sys_setprofile
  26 → 26; test_cprofile 26 → 26; test_pdb 86 → 87 equal, the same 11 rows in no class at the base and the head (the
  base's own: PdbTestReadline's nine not run, test_compile_error_message's Go panic, `-i`).
- **What the IR cannot give exactly**: INSTRUCTION inside an await or a yield from (the view's SEND loop is one IR
  instruction: every instruction of it fires at each step), a view instruction of no IR instruction reached by a jump;
  LINE stays 219t's rule over the IR (test_sys_settrace's), not the view's line table.

## As built: 284, the compiled launcher: Lib/test's children compiled in the AOT cell (2026-11-13, the Mac VM, queue 284; 279's DECISION (a))

The user chose 279's DECISION (a): Lib/test compiled (`bin/pyi check --aot --lib-test`) becomes a recorded source of the
AOT cell, after a compiled launcher (a child of a compiled driver ran as pyi on goi, 30 s and more a start) and 275's
per-method timeout (the runner's idle timeout, already in the compiled runner: `runner_exe` is keyed by
`tools/pyi/libtest_all.py`'s digest, and check passes `--idle-timeout`). Base 201 7ba1d69c2.

- **The launcher**: `tools/pyi/launcher.py` compiled once by pyi's AOT (`script_exe`, kept by the runtime's key, the
  generator's and the script's digests), its never-run imports (`if sys.argv is None:`) the modules a child commonly
  imports (test.support and its helpers, unittest, subprocess, json, asyncio, ...) compiled ahead with it. Its wrapper
  `WORK/aot/bin/python3` (`bin/pyi launcher` prints it; `launcher_py`) runs `bin/pyi python` with `PYI_LAUNCHER`:
  `cmd_python` parses CPython's command line as for goi (the -c file, -m's wrapper, stdin's, the options' variables),
  then execs the launcher with **`PYI_AOT_MAIN`** naming the file to run: AotMain (`go/arbace/py/aot.go`) runs it as
  `__main__` in place of the program's own main module (RunModule's charset path: decoded by its coding spec, compiled at
  run time by the host's compiler or its IR cache; a module compiled ahead with the launcher runs native),
  `sys.path[0]` its directory, the variable removed at start. `sys.executable` is the wrapper, whose paths are written in
  it, so a grandchild, even with `env={}`, starts compiled too. check --aot --lib-test exports it as `PYI_EXECUTABLE`
  (`PYI_AOT_LAUNCHER=0`: goi as before). A child's start: 4-10 s on the VM (about 3 s of it the host compiler's start for a
  source not in the IR cache), against 30-60 s on goi.
- **Fixed on the way** (runtime by print, read, gofmt, vet): the launcher's main file that does not decode names the
  file ("Non-UTF-8 code starting with '\x89' in file F on line 1", as the tokenizer and goi's error-answer report it;
  `mainSourceError`, asked only when the bytes are not UTF-8: test_exceptions' test_non_utf8); `socket.getaddrinfo`
  takes an int port outside 0-65535 as its text and a numeric service past 65535 is EAI_SERVICE (gh-74895, musl;
  test_socket's test_getaddrinfo_int_port_overflow; goi too).
- **The record**: `--record` writes `test/python/check-aot-lib-test-linux-ARCH.edn` (the eval check's format, a row a
  method; the main host's night), `PYI_CHECK_AOT_LIB_RECORD` another (a dry run's).
- **Classes** (both files; each probed the same on goi, so the runtime's): `:signal-wakeup-errors` (new, deferred, PY6f:
  set_wakeup_fd's write errors and full buffer not reported, test_signal's 3); `:pyi-command-line` +4 (test_subprocess's
  test_empty_env: bin/pyi's own variables in an env={} child's os.environ; test_communicate_errors' undecodable -c
  argument); `:multiprocessing` +2 (test_get_event_loop_new_process: the forkserver's EOF); `:capi-port` +1 (test_pickle's
  test_clear_unpickler_memo_memory_error: `_testcapi.set_nomemory`).
- **Measured on the VM** (arm64, 2 jobs, beside two other agents; run): 279's 47 modules: 4,731 methods, **3,771 pass,
  960 classed, 0 failing** (279's run judged with this base's classes: 3,769 / 962 / 0; test_exceptions' two 300 s
  timeouts now pass), wall 11.4 min against 29.5 min (5.5 min of it test_fstring's test_many_expressions at its 300 s
  cap), each driver's peak 0.1-0.4 GB against 2.0-2.1 GB for the child-heavy ones (test_fstring 714 s -> 1 s, test_frame
  130 -> 1, test_coroutines 197 -> 2, test_list 99 -> 1, test_getopt 65 -> 1, test_capi 105 -> 7). Then 101 more of night
  12's modules (all it finished but multiprocessing, concurrent_futures, free_threading, the thread modules and
  test_queue; test_subprocess, test_signal, test_os, test_socket, test_pickle among them): 13,140 methods, 11 failing
  before the fix and the classes above, 0 after; 44 min. The 148 together: **17,871 methods, 14,452 pass, 3,419 classed,
  0 failing**, the 3 controls failing as they must; the drivers' compile 14 s each (summed 35 min, peak 3.0 GB), the
  runs 2,585 s summed against night 12's 88,211 s on goi for the 109 it ran (2.8%; test_subprocess 3,602 s capped ->
  251 s, test_signal 3,602 -> 449, test_os 3,602 -> 83, test_pickle 3,601 -> 67).

## As built: 289, test_pdb's last rows: a source nested too deep, `python -i`, the readline module (2026-11-14, the Mac VM, queue 289)

test_pdb's 11 failing rows no class covered (281's verification): test_compile_error_message, TestREPLSession's
test_return_from_inline_mode_to_REPL and PdbTestReadline's nine.

- **A source nested past the reader's depth** (`arbace/python/run.ae`): `compile('-' * 100_000 + '1')` overflowed the host
  reader (the evaluator's StackOverflowError, from about 5,000 levels on the VM) outside compile-uncached's catch, and the
  hook's panic came back as "SystemError: pyi: Go panic: ...". compile-source now answers CPython's parser error,
  "MemoryError: Parser stack overflowed - Python source too complex to parse" (pegen's `_Pypegen_stack_overflow` past
  MAXSTACK; CPython gives it from about 6,000 levels), and an overflow in the compiler "RecursionError: maximum recursion
  depth exceeded during compilation"; compileSource and parseAST raise them (`compilerLimitError`, import.go). The level
  where each starts differs from CPython's (its C stack and MAXSTACK, the evaluator's depth here); the kind of error is
  CPython's.
- **`python -i`** (bin/pyi python, `go/arbace/py/interp.go`, `repl.go`): `-i` sets `PYI_INSPECT` (`1` after -c, -m, a file
  or `-`; `repl`, or `repl-quiet` under -q, with no code: the run is the REPL on stdin, an empty stdin module before it,
  `sys.argv` `['']`, the banner on stderr but under -q), read once at the runtime's start as config's inspect:
  `sys.flags.inspect` and `interactive` 1 (PYTHONINSPECT at start, but under -E or -I, sets inspect alone). RunModule's
  ends go through `endMain`: under pymain_repl's test (-i, or PYTHONINSPECT at start or left in the environment by the
  program with stdin a terminal) the REPL runs in `__main__`'s namespace after the module, the module's exception printed
  first (SystemExit's too, as handle_system_exit leaves it under -i), `sys.flags.inspect` 0 in it, and its status is the
  run's. The compiled launcher (AotMain) ends through RunModule too. The REPL's loop is `Interp.repl`; a statement after
  the first runs in `__main__`'s dict without making the module again.
- **The readline module** (`go/arbace/py/builtins_readline.go`, over a line editor of pyi's own in GNU readline's place):
  Modules/readline.c's functions over GNU's history list (history_get from history_base, 0-based remove and replace,
  stifling by `set history-size`, history files a line an entry, history_truncate_file's last lines, append_history's
  ENOENT), its hooks (startup before the prompt, pre-input after it, the completer, the matches' display), the init file
  (INPUTRC, else ~/.inputrc, else /etc/inputrc, at import, as setup_readline's rl_initialize reads it), parse_and_bind's
  `set`, `KEY: FUNCTION` and `KEY: "MACRO"` lines. input() with sys.stdin and sys.stdout the process's terminals reads by
  the editor (builtin_input's tty path, call_readline): the terminal as rl_prep_terminal sets it (no ICANON, ECHO,
  ICRNL, INLCR, ISTRIP, INPCK; VMIN 1; OPOST kept), the prompt on stdout after the program's output, the emacs keymap's
  common keys (C-a/b/d/e/f/h/k/u/w, DEL, arrows, Home/End, history), TAB self-insert as setup_readline binds it until
  bound to complete. Completion is rl_complete's: the word back to a delimiter of set_completer_delims, the completer's
  matches for state 0, 1, ... until a non-string (an exception ends them, as on_completion's PyErr_Clear), one match
  replaces the word, several their common prefix, a second TAB that changed nothing lists them (in columns, or by the
  display hook with the substitution, the sorted unique matches and the longest's length); no completer: file names;
  begidx and endidx in characters, as flex_complete gives them. The line goes to the history unless set_auto_history(False)
  or equal to the last. `backend` "readline", `_READLINE_VERSION` 0x803 (the library's interface it follows). On tamago
  there is no module (builtins_os_other.go's stubs). `:c-modules-deferred` (both files) no longer names readline.
- **Measured on the VM** (arm64, 4 cores, `bin/pyi lib-test-all --fresh` at 1 job, base 5c6371975 in a worktree of its
  own; run): test_pdb 87 pass, 55 classed, **11 failing -> 98 pass, 54 classed (doctests 3, skipped-by-test 51), 0
  failing**; test_readline 16 classed (did not import) -> 15 pass, 1 skipped by the test; test_cmd 4 pass, 4 classed -> 6
  pass, 1 classed (cpython-only); test_bdb 36 / 1 and test_sys_settrace 366 / 82, 0 failing, unchanged. Probes (each
  against python3 3.14.10's run, a mutated reference as the control; base FAIL, head PASS): the nested sources (compile,
  exec, eval, ast.parse, pdb's `_compile_error_message`), `python -i` (after -c, SystemExit and an error under -i, the
  no-code REPL's flags and argv, the banner, test_pdb's REPL session), PdbTestReadline and test_readline by unittest.
- **Other modules that start `python -i`** (sampled by unittest, head and base; run): test_repl's sessions now run, 7 of
  its methods and test_cmd_line's test_run_module_bug1764407, test_inspect's TestRepl pass (all failed on the base);
  the 11 still failing (test_pythonstartup_failure by its subtests), all failing on the base too, are `:repl-console` (new, deferred, both files: the asyncio REPL,
  PYTHONSTARTUP, a syntax error's source line, a last line without a newline). test_fstring's and test_tstring's
  test_expression_in_interactive_after_buffer_resize (`:child-is-pyi`), test_cmd_line's test_displayhook_unencodable
  (`:pyi-command-line`) and test_cmd_line_script's test_stdin_loader (`:pyi-main-module`) still fail, in their classes;
  `bin/pyi repl --check` 2 of 2.

## As built: 286, the largest deferred classes of the eval record: method_descriptor, cast memoryviews, reconfigure, int (2026-11-14, the Mac VM, queue 286)

The eval record of Lib/test on the VM is night 12's partial run (181 modules, 16,042 methods; 278's judged rows), its
deferred classes counted with the base's (5c6371975) divergences: `:runner-limits` 1,812 (1,753 of them whole modules
past night 12's module cap: 275's per-method timeout and 284's compiled cell judge them; no pyi fix), `:data-model` 448,
`:testcapi` 53, `:finalization-collect` 24, `:remote-debugging` and `:memoryview-formats` 22 each, `:multiprocessing` 14,
`:signal-timing` 12, `:finalization-goi` 10, ..., `:int-core` 6, `:stdio-text` 5. Closed by fixes in pyi, largest first:

- **method_descriptor** (`go/arbace/py/func.ae`, `descr.ae`, `registry.ae`): `:data-model`'s 420 rows of one cause,
  `'builtin_function_or_method' object has no attribute '__objclass__'`: `type(list.append)` was
  builtin_function_or_method, so `multiprocessing.reduction`'s `register(type(list.append), _reduce_method_descriptor)`
  took every built-in function ForkingPickler pickled (the spawn and forkserver managers' setUpClass, the process pools).
  A `*Builtin` with an Owner and no Self is now of type `method_descriptor` (`MethodDescrType`, hidden from builtins):
  BuiltinType's call (method_vectorcall's instance check), `<method 'join' of 'str' objects>`, `__get__` (descr_check),
  `__reduce__` (descr_reduce), `__name__`, `__qualname__`, `__objclass__` (member), `__doc__`; builtin_function_or_method
  keeps the functions and the bound methods and loses `__objclass__`. inspect's predicates, `classify_class_attrs`
  ('method' for `int.to_bytes`), pydoc's getset_descriptor and test_descr's test_qualname follow.
- **cast memoryviews by item** (`memoryview.ae`): `:memoryview-formats`' 22 rows (`assignment to format 'i'`:
  multiprocessing's Barrier over `BufferWrapper.create_memoryview().cast('i')`). An unshaped view of a format other than
  'B' indexed its bytes and refused assignment; now multiGet (unpack_single; a slice keeps the format, a step its stride,
  `data()` gathers a strided slice item by item), multiSet and mvPack (pack_single: the integer formats by `__index__`
  with type_error_int/value_error_int, f/d/e by PyFloat_AsDouble, '?' by truth, 'c' one byte; a slice from a view of the
  same structure), and shaped 1-D and 0-dim views assign wider formats too.
- **`TextIOWrapper.reconfigure`** for the standard streams (`sys.ae`, `builtins_sys_info.ae`): `:stdio-text`'s 5 rows
  (`python -m inspect`): encoding, errors (strict with an encoding alone), newline (checked), line_buffering, write_through;
  each stream's encoding and errors its own. The class is retired: its one row left (test_details_undecodable_path) is
  `:pyi-command-line`'s, pyi reads no PYTHONPATH.
- **int** (`format.ae`, `int.ae`, `numbers.ae`): `:int-core`'s rows: an int past 64 bits formatted (its big value's
  digits, the decimal ones under the int max str digits limit; the float types by PyLong_AsDouble; 'z' refused as
  format_long_internal does); int(str) maps Unicode decimal digits and spaces to ASCII first
  (_PyUnicode_TransformDecimalAndSpaceToASCII; an ASCII text told by `utf8.RuneCountInString`, as a loop under goi made
  test_int's denial-of-service timing fail); int() of a str subclass, a bytearray or a buffer, and with a base only of a
  str, bytes or bytearray (and their subclasses); a bytes text's error names it as bytes; int's real, imag, numerator,
  denominator and float's real, imag are getsets (they were a GetAttr slot that shadowed an int subclass's numerator).

Measured (`bin/pyi lib-test-all --fresh`, each test's own verdict, base 5c6371975 -> head, pass/classed/failing):
test_inspect 291/54/18 -> 297/54/12 (the 12 the base's own); test.test_concurrent_futures.test_as_completed 8/17/0 ->
12/14/0 (the spawn pool runs; the forkserver's rows `:multiprocessing`, its EOF now a pattern); test_memoryview 99/65/7
-> 105/59/7; test_fractions 45/5/0 -> 50/0/0; test_descr 144/19/0 -> 145/18/0; test_int 31/21/0 -> 35/17/0; test_long
35/9/4 -> 36/8/4; test_decimal 380/204/0 -> 381/203/0; test_pydoc 73/9/39 -> 74/9/38; test_buffer, test_types,
test_copy, test_float, test_numeric_tower, test_abstract_numbers unchanged. The forkserver test_manager's setUpClass now
dies on the forkserver's os.fork (`:multiprocessing`); the spawn test_manager's setUpClass passes and its tests run
(on the VM, pinned to 4 cores, the first two passed the 300 s test cap: the main host's run judges them). Class by
class on night 12's 181 modules: `:data-model` 448 -> 25, `:memoryview-formats` 22 -> 0, `:int-core` 6 -> 0,
`:stdio-text` 5 -> retired (its one row left, test_details_undecodable_path, `:pyi-command-line`: pyi reads no
PYTHONPATH), `:finalization-collect` 24 -> 25; `:runner-limits` (1,812), `:testcapi` (53), `:remote-debugging` (22)
and the rest unchanged. One regression found on the way and fixed: the Unicode-digit mapping's pass over the text,
interpreted under goi, made the digits limit's refusal slow (test_int's denial-of-service tests); an ASCII str (its
flag) is parsed as it is and the error's object made only on an error. Queue 286's report:
`.tmp/q286/286-python-deferred-classes.md` (the VM).

## As built: 290, the next deferred classes: the built-ins' signatures, method-wrapper, classmethod_descriptor (2026-11-14, the Mac VM, queue 290)

Night 12's partial eval record (181 modules, 16,042 methods), judged with 286's classes: the deferred classes are
`:runner-limits` 1,812 (whole modules past the night's cap: no pyi fix), `:multiprocessing` 96 (82 of them the
forkserver test_manager's not-run rows behind its setUpClass, the forkserver's own os.fork), `:testcapi` 53 (the
`_testinternalcapi` and `_datetime` C API skips, by design since 244-4 (a)), `:data-model` 25, `:finalization-collect` 25,
`:remote-debugging` 22, `:memoryview-formats` 22 (286's, the record not run since), `:signal-timing` 12,
`:finalization-goi` 10, `:ssl-finalization` 8, `:pyi-command-line` 6, `:socket-gaps` 4 and smaller. Closed:
`:data-model`'s 25 rows, and a regression of 286's found by the coordinator:

- **The built-ins' `__text_signature__`** (`go/arbace/py/textsig.ae`, `builtins_textsig_table.ae`): CPython keeps a C
  callable's signature at the head of its docstring, which pyi's built-ins do not carry; `tools/pyi/textsig_table.py`
  records the oracle's own (CPython 3.14.10: every type, dict callable, module function and slot wrapper of the built-in
  and lib-dynload modules but the test modules, 2,519 entries) into `builtins_textsig_table.go`, checked by `bin/pyi
  runtime tables`. A heap type's comes from its `__doc__` (_PyType_GetTextSignatureFromInternalDoc), a module's
  function's `__self__` is its module (inspect drops `$module`), a built-in type's `__new__` is bound to its type
  (`object.__new__.__self__`, `T.__new__` its qualified name). inspect.signature of object and of a class without its own
  `__init__` or `__new__`, of `str.join`, `len`, classmethod, staticmethod, getfullargspec of `os.stat`, rlcompleter's
  `f()` and pydoc's descriptions follow.
- **method-wrapper and classmethod_descriptor** (`descr.ae`): a slot wrapper bound to an instance was a Python bound
  method, which inspect's `_NonUserDefinedCallables` took for user code; it is a method-wrapper (a `*Method` over a
  `*slotWrapper`: its call, repr, comparison, `__self__`, `__objclass__`, `__name__`, `__qualname__`, `__reduce__`, no
  `__func__`). A built-in type's class method (`dict.fromkeys`) is a classmethod_descriptor, no longer an instance of
  classmethod (classmethod_get's and classmethoddescr_call's checks, repr, `__reduce__`). dict's and list's `__getitem__`,
  dict's, set's and frozenset's `__contains__` are method descriptors (METH_COEXIST; `keyword.iskeyword` a
  builtin_function_or_method), bound to the instance itself as the slot wrappers were (a dict subclass's `__missing__`).
- **The data model's rest**: a dict's or set's subclass keeps the keys of its stores and lookups as they are (Counter's
  float-subclass keys were stored as floats: test_statistics' TestMode.test_types_conserved), set_contains, discard and
  remove take a set subclass's instance as a frozenset of its items (PySet_Check), an exact exception's `__dict__`
  assignment goes through its getset (test_descr's test_set_dict), `__getnewargs_ex__`'s result is checked as
  _PyObject_GetNewArguments does and an empty keyword dict reduces through `__newobj__` (test_reduce; test_enum's
  test_subclasses_with_getnewargs_ex).
- **286's regression**: int's `numerator` and float's `real` became getsets in 286, which `int.__dict__['numerator']`
  found but `int.__dict__.get('numerator')` did not; enum's `__set_name__` looks for a base's descriptor that way to
  redirect a shadowing member, so an IntEnum member `numerator` took every member's attribute (test_enum's two
  test_shadowed_attr). mappingproxy.get answers as its subscript does.

Measured (`bin/pyi lib-test-all --fresh`, each test's own verdict, base 0eec2c528 in a base worktree -> head 3c6ab10ed,
pass/classed/failing): test.test_inspect.test_inspect 297/66/0 -> 315/48/0; test_pydoc 74/9/38 -> 92/9/20; test_set
609/21/0 -> 612/18/0; test_descr 145/18/0 -> 147/16/0; test_statistics 393/7/0 -> 394/6/0; test_rlcompleter 9/2/0 ->
11/0/0; test_enum (`bin/pyi lib-test`) 30 failing -> 27; test_exceptions 80/24/0 -> 89/24/0 (the base's run lost four
to the VM's load); test_dict, test_collections, test_copy, test_funcattrs, test_keyword, test_types unchanged; 286's
test_int 35/17/0, test_long 36/8/4, test_fractions 50/0/0 as 286 left them. No row got worse. Classes on night 12's
rows: `:data-model` 25 -> 0 (21 pass; test_excluding_predicates to `:c-accelerators`, _pydatetime's timedelta.days;
test_descrdoc to `:pyio`; test_cycle_through_dict and test_remove_subclass to `:finalization-collect`, a cycle through a
class); the class keeps its other named tests. `:capi-port` takes test_signature_parsing_with_defaults (a C type's
method keeps no ml_doc), `:set-core` loses three. The record's 21 rows read "in no class" until the main host's run
replaces night 12's. Queue 290's report: `.tmp/q290/290-python-deferred-classes-2.md` (the VM).

## As built: 287, the compiled cell over the whole Lib/test on arm64, and the amd64 night's recipe (2026-11-14, the Mac VM, queue 287)

284's launcher made Lib/test compiled (`bin/pyi check --aot --lib-test`) a candidate for one night; 287 ran it over every
module of the universe (`test/python/universe-cpython314.edn`, 529 modules) on arm64, judged and classed it as 279 did,
and writes down the main host's night. Base 5c6371975 (201 with 278, 279, 275, 281 and 284).

- **The run** (the VM, arm64, `PYI_AOT_LIB_JOBS=2 PYI_LIB_TEST_JOBS=2`, GOGC=50, the scratch record
  `PYI_CHECK_AOT_LIB_RECORD`; 48 pieces resumed into one run directory, then one invocation naming no module that ran
  nothing and judged all): **529 modules, 48,162 methods: 38,834 pass, 8,646 classed, 682 failing** on the base; the 3
  controls fail as they must. None of the 682 is in 284's 148 modules: night 12 never reached them on goi either.
- **Times and memory** (the VM; 104 modules at 8 shared cores, from 21:28 UTC every process pinned to 4 cores, about 2x
  slower): 5 h 40 min wall at 2 jobs; the drivers' compile summed 13,266 s (18.4 s a driver unpinned, 26.7 s pinned;
  peak median 1.17 GB, p90 1.32 GB, max 4.0 GB, test_typing); the modules' runs summed 19,348 s (the runner's
  "run (N s)"; 2,049 s for the 104 unpinned), each driver's peak median 0.15 GB, p90 0.35 GB, max 5.7 GB (test_zipfile),
  then 2.2 GB (test_lzma). The slowest modules (pinned): test_cmd_line_script 1,493 s (4 tests at the 300 s cap: the REPL
  flush tests), test_multiprocessing_main_handling 1,115 s (3 at the cap), test_email 789 s (2 at the cap), test_io 728 s
  (7 idle hangs), test_cmd_line 723 s, test_cext 537 s (pip into a venv), test_dbm_sqlite3 475 s, test_fstring 456 s.
  28 timeout rows (12 idle hangs at 90 s, 16 busy at 300 s), all in existing classes.
- **The goi probe**, 279's way: on goi (`bin/pyi check --only` of the failing methods) 6 modules took 45 min pinned
  (multiprocessing's 935-1,517 s), so the probe is 279's loop control instead: the same compiled drivers with
  `PYI_AOT_LOOP=1` (every code object on the dispatch loop, the runtime's own semantics), `--only` the 682: 606 the same
  outcome, 56 not run on either, 10 another failure (none passes), 10 pass on the loop only (the 2 PdbTestColorize and
  the 8 of `:compiled-flaky`). The goi rows that ran (test_pydoc,
  test___all__, test__interpreters, test_argparse, test_multiprocessing_fork's two) failed the same on goi.
- **Fixed** (runtime by print, edit, read, gofmt, vet clean; each a test on goi, base FAIL, head PASS):
  - a SimpleNamespace subclass's instance (an *Instance holding the *Namespace, wrapNew) panicked in GetAttr, SetAttr
    and `__reduce__` (`o.(*Namespace)`): now by `builtinValue`, `__reduce__` an objectDescr method so a subclass reduces
    to its class (test_zipfile's `class Invoked(types.SimpleNamespace)`, 45 methods);
  - posix had SEEK_SET/CUR/END, which posixmodule.c has not, so os.__all__ named them twice (test___all__); posix's
    `_is_inputhook_installed()` (False) and `_inputhook()` (0), which _pyrepl's unix console asks (test_pyrepl);
  - object's own slot wrappers (`object.__str__`, `__repr__`, `__format__`) took a built-in subclass's value, so
    `__str__ = object.__str__` in an int-mixin enum gave the int's text; an empty format spec of an int, float or complex
    subclass is its str() (long__format__'s rule; test_enum's 23 str-override and format tests);
  - defaultdict's `|` gave a plain dict (defdict_or: `type(dd)(dd.default_factory, left)` updated with right), and
    `__missing__` is setdefault (3.14.10's gh-145492: a value the factory stored stays; test_defaultdict).
  - bin/pyi: `check --aot --lib-test` compiled every selected module's driver before running any: the whole universe at
    once is 529 drivers of 170-260 MB, over 100 GB of disk. Now in batches of **`PYI_AOT_LIB_BATCH`** modules (default
    40; 0: all at once), each batch's drivers compiled, run and removed before the next's.
- **Not fixed, compiled only**: after `Pdb.set_trace(commands=[..., 'c'])` in a compiled test method, the method's
  frame is entered again at its resume point after a stack call with one value fewer than its spill left (PC 24, SP 3
  for 4), so AotCallMethodV takes a bound method as a function: a Go panic (test_pdb's PdbTestColorize, reproducer by
  `bin/pyi aot`); a guard keeping a handed-over frame on the loop changed nothing (no AotToLoop on that path).
  `:aot-trace-resume`.
- **Classes** (test/python/divergences.edn and divergences-315.edn, before `:skipped-by-test`; 21 new ones only, each
  naming its methods in `:tests`, 597 rows): `:aot-trace-resume` (compiled only, above), `:compiled-flaky` (9 rows that
  failed once compiled under the load and passed on the loop control and on the rerun), `:audit-hooks` (sys.audit calls
  no hook), `:multiprocessing-children`, `:pydoc-builtins` (no `__text_signature__`, no built-in subclasses,
  object.__dict__ without slot wrappers), `:c-bisect`, `:hash-algorithm`, `:ordered-dict-c`, `:structseq-protocol`,
  `:io-c-checks`, `:compileall-pyc`, `:dbm-sqlite3-locking`, `:interactive-mode` (no -i, no REPL), `:venv-and-build`
  (by design), `:run-dir-zip-main`, `:partial-python`, `:zoneinfo-c`, `:source-decoding`, `:core-objects-287`,
  `:os-and-files-misc`, `:tools-misc`. No existing class touched.
- **The head**: the fixes' modules and 12 neighbours compiled again at the head (`--only`, in batches of 6: test_zipfile,
  test_enum, test_defaultdict, test___all__, test_pyrepl, test_os, test_posix, test_descr, test_class, test_float,
  test_complex, test_str, test_long, test_types, test_fstring, test_collections and the loop-only rows), 86 of the 682
  pass, none of the rest newly failing but test_float's test_hash_nan (99 != 100, once: `:compiled-flaky`). The whole
  run's directory with those modules' head runs put in, judged with the head's classes: **529 modules, 48,162 methods:
  38,919 pass, 9,243 classed, 0 failing**; the 3 controls fail as they must.

### The amd64 night (the main host, one slot)

```
cd MAIN-CHECKOUT
GOGC=50 PYI_AOT_LIB_JOBS=4 PYI_LIB_TEST_JOBS=4 PYI_AOT_LIB_BATCH=40 \
  bin/pyi check --aot --lib-test --fresh --record
```

- It writes `test/python/check-aot-lib-test-linux-amd64.edn` (the cell's record; the first one: no regressions judged).
  Without `--fresh` it resumes (a module with a `.status` is kept), so a night cut short continues the next.
- Limits, the defaults: `PYI_LIB_TEST_TEST_TIMEOUT=300` (a busy test), `PYI_LIB_TEST_IDLE_TIMEOUT=90` (275's hang),
  `PYI_LIB_TEST_TIMEOUT=3600` (a module); `PYI_GOMEMLIMIT=3GiB` for the host compiler and pyi; GOGC=50 (the user's).
- **Expected time**: from the VM's unpinned rates (18.4 s a driver's compile, the runs at about half their pinned
  sum): about 9.7k s of compile and 11-12k s of runs, so **about 1.5-2 h at 4 jobs** on a quiet host, with the
  slowest modules (test_cmd_line_script, test_multiprocessing_main_handling) about 10-25 min each in the tail. Add
  about 12 min when the runtime changed since the last AOT build (the runtime printed, the five clib aots, the
  launcher and the runner compiled: `bin/pyi launcher` alone took 12 min 20 s here) and the drivers' first compile
  after a generator change (4.5 GB, alone).
- **Expected peak**: 4 compiles (median 1.2 GB, up to 4.0 GB) and 4 drivers (median 0.15 GB, up to 5.7 GB) at once:
  about 8 GB typical, under 22 GB at worst. Disk: a batch's drivers, about 8 GB at 40 (plus the launcher's 262 MB and
  the runner's 170 MB).
- **Expected outcome**: the VM's whole run had 682 failing on the base; with this branch's fixes and classes 0 failing
  is expected, apart from what amd64 adds (the timeouts the load moves, a first whole run's new rows).
- After it: name the record in `test/reap/cells.edn`'s `:pyi-aot` sources (284's REQUEST), then `bin/reap --record`.

## As built: 293, settrace and setprofile around the code that stands for C, the REPL's console (2026-11-14, the Mac VM, queue 293)

289 left test_bdb's and test_sys_settrace's classed rows and opened `:repl-console` (test_repl's 11 that `-i` reaches);
`:profile-events` (test_sys_setprofile's 3) is the same events' profiler side.

- **No event inside the code that stands for C** (`go/arbace/py/trace.go`'s `Frame.inCImpl`, monitoring.go's `monFireAt`,
  trace.go's `traceInstr`): the frames of `cImplModules` (pylifecycle.go: io's _pyio, warnings' _py_warnings) fire no
  sys.monitoring event, and so no trace or profile event, as none fires inside CPython's C; the answer is kept in the code
  object (`Code.cimpl`) from its first frame's globals. test_bdb's test_skip saw the source loader's open_code (_pyio's
  open, a Python function). Python code those frames call is traced as code C calls is (a callback, a codec: _pyio's
  StringIO runs codecs' incremental coder, CPython's C StringIO none: a Low).
- **getattr with a default** (builtins.go) looks up as hasattr does, module_getattro's suppress (`getAttrSuppressed`):
  a module's missing name gets a plain AttributeError (imp_bootstrap.go's `moduleNoAttr`), no Python code run for its
  message (_pyio's `getattr(os, 'O_BINARY', 0)` called the spec's has_location property under a tracer).
- **A replaced hook's `__del__`** (trace.go's `setTrace`, monitoring.go's `setProfile`, finalize.go's `finAddr`): the
  function sys.settrace or sys.setprofile replaces is checked at once by 222's `finDeleted` (Go's collector asked
  whether that was its last reference), its `__del__` then, as CPython's decref runs it (test_sys_settrace's and
  test_sys_setprofile's test_reentrancy). On goi a hook that was called (setprofile's own c_call) stays in goi's
  registers: setprofile's passes compiled (`bin/pyi aot`), `:finalization-goi`.
- **CALL's events expand a method over a C function** (monitoring.go's `monExpandMethod`, `monCallSite`'s `expand`):
  classmethod(repr)'s bound method gives CALL and C_RETURN the function and self, as `_MAYBE_EXPAND_METHOD` does before
  `_MONITOR_CALL`, so the profiler's c_call and c_return name it; CALL_FUNCTION_EX does not expand (no c_call:
  test_method_with_c_function's three cases).
- **The REPL** (repl.go, interp.go): a last line without its newline prints ps2 and the end of input's newline before
  the statement runs (the tokenizer's second read); a compiler's SyntaxError of the interactive source gets its line
  from the source (`replSyntaxText`; CPython compiles under a name linecache holds); with no code to run, PYTHONSTARTUP's
  file runs in `__main__` first (`runStartup`, pymain_run_startup: not under -E or -I, `__file__` for the run, an error
  printed, SystemExit the run's end, "Could not open PYTHONSTARTUP").
- **A module's code called as a function** (frame.go's `newFrame`): a code without CO_OPTIMIZED has the function's
  globals as its locals, as `_PyFunction_Vectorcall` gives them (`types.FunctionType(code, globals)()`, the asyncio
  REPL's runcode: every name panicked).
- **compile() of an AST object** (builtins_exec.go) ignores PyCF_ALLOW_INCOMPLETE_INPUT and PyCF_DONT_IMPLY_DEDENT,
  the tokenizer's flags (the asyncio console's per-statement `ast.Interactive` was "incomplete input" for any compound
  statement).
- **The asyncio REPL** (`python -m asyncio`) still ends at once: asyncio/__main__.py imports asyncio.tools, which imports
  the C module _remote_debugging (`:remote-debugging`, deferred, survey §6.6; its eight test_repl rows moved there). With
  a stand-in module in sys.modules (`.tmp/q293/t/pa.py`, probed) its console runs as CPython's: banner, contextvars at the
  top level and in an awaited coroutine, the traceback, "exiting asyncio REPL...". The task's DECISION 1.
- **Still classed**: test_sys_settrace's four test_13_genexp and test_sys_setprofile's test_unfinished_generator
  (`:finalization`): a generator a call leaves suspended (`any(gen())`, `next(f())`) is closed by CPython at its last
  reference, the call's return; test_20_async_for_loop's flake (`:settrace-events`), probably such a generator closed by a
  later collection inside its trace (read). The task's DECISION 2.
- **test_pyrepl's pty sessions** (the coordinator's note on 289's verification): TestMain's 19 and TestDumbTerminal's
  test_dumb_terminal_exits_cleanly start `python -i -u` on a pty and give up after 30 s of silence; pyi's start on the
  VM takes about 65 s before its banner (`.tmp/q293/t/ptyrun.py`, probed; the session then runs), so all fail "Timeout
  while waiting for output". Past the time, on a terminal CPython runs _pyrepl (pymain_run_stdin, unless
  PYTHON_BASIC_REPL or TERM=dumb) where pyi runs its basic REPL; _pyrepl's unix console needs fcntl.ioctl with a buffer
  and posix._is_inputhook_installed. `:repl-console` now holds these 20 (both files; the task's DECISION 3).
- `:profile-events` leaves both files, `:repl-console` keeps only test_pyrepl's pty sessions; `:pyio` and
  `:finalization` lose their tracing rows.
- **Measured on the VM** (arm64, 4 cores under load, `bin/pyi lib-test-all --fresh`, each test's own verdict; base
  679314248, or 289's run where the code is the same; run): test_bdb 36 -> **37 pass**; test_sys_settrace 366 -> **367**;
  test_sys_setprofile 26 -> **27** (and setprofile's test_reentrancy compiled); test_repl 4 -> **7** (8 in
  `:remote-debugging`); test_monitoring 81, test_pdb 98 and test_cprofile 26 unchanged; 0 failing in these. test_pyrepl:
  the 20 pty sessions classed, test_interact's test_runsource_show_syntax_error_location (the AST compile) and
  test_pep_765_warning now pass; its other 21 failing rows are the base's own (test_unix_console's 19 without
  `posix._is_inputhook_installed` and fcntl.ioctl with a buffer, two of test_interact's, TestPyReplCompleter's). Probes
  (`.tmp/q293/t/`, against python3 3.14.10, a mutated reference the control; base FAIL, head PASS): the events around
  _pyio (pb), compile and FunctionType as the asyncio console uses them (pc), test_skip, test_method_with_c_function and
  settrace's test_reentrancy (ptf), test_repl's three (prr); the replaced hooks' `__del__` compiled (pe).

## As built: 296, a _remote_debugging of pyi's own (2026-11-15, the Mac VM, queue 296; 293's DECISION 1 (a))

293 found `python -m asyncio` stopping at asyncio.tools's `from _remote_debugging import RemoteUnwinder, FrameInfo`
(test_repl's eight asyncio rows in `:remote-debugging`; test_asyncio.test_tools and test_external_inspection did not import).

- **The module** (`go/arbace/py/builtins_remote_debugging.go`; CPython 3.14.10's Modules/_remote_debugging_module.c and
  Python/remote_debug.h read for behaviour): the five struct sequences TaskInfo, FrameInfo, CoroInfo, ThreadInfo and
  AwaitedInfo with CPython's fields and docs, `PROCESS_VM_READV_SUPPORTED` 0, and `RemoteUnwinder(pid, *, all_threads=False,
  only_active_thread=False, debug=False)` with clinic's argument errors and `__init__`'s ValueError (all_threads with
  only_active_thread), then failing as CPython's Linux search fails on a process that holds no PyRuntime section: an
  unreadable `/proc/PID/maps` is its errno's OSError with the "Cannot open process memory map file" OSError raised from it,
  then RuntimeError "Failed to find the PyRuntime section in process PID on Linux platform" (context: that OSError), and
  under `debug=True` "Failed to get Python runtime address" raised from it. Its methods get_stack_trace,
  get_all_awaited_by and get_async_stack_trace exist; no instance reaches them attached.
- **Left out**: reading a process's frames. Another pyi's frames are Go's, with no PyRuntime section or debug offsets;
  a CPython's would need its `_Py_DebugOffsets` read through /proc/PID/mem (`PROCESS_VM_READV_SUPPORTED` 0 keeps
  test_external_inspection's Linux readers skipped). A larger unwinder over pyi's own frames is 293's option (c).
- **sys.flags from the command line**: `-q` sets `quiet`, `-E` and `-I` `ignore_environment`, `-I` `isolated` (bin/pyi
  python's `PYI_START_FLAGS`, read once by the runtime's `startFlag`): asyncio's REPL reads quiet for its banner and
  ignore_environment before running PYTHONSTARTUP (test_quiet_mode, test_asyncio_repl_respects_isolated_mode).
- Rows (the VM's runs, judged): test_repl 7 pass -> 14 (the asyncio REPL's seven; test_asyncio_repl_is_ok, a pty
  session, joins `:repl-console`: the start on a pty past its 30 s); test_asyncio.test_tools 22 in `:remote-debugging`
  (no import) -> 22 pass; test_external_inspection 25 skipped at import -> 23 in `:remote-debugging` (18 skips on
  PROCESS_VM_READV_SUPPORTED, 5 that unwind a pyi child) and 2 free-threading skips equal to CPython's; test_cmd_line's
  test_isolatedmode passes (out of `:pyi-command-line`); test_utf8_mode and test_faulthandler unchanged.

## As built: 297, a suspended generator closed at its call's return (2026-11-15, the Mac VM, queue 297)

293's DECISION 2 (a), the user's (round 37). CPython's reference counting closes a generator that a call leaves
suspended (`any(gen())`, `next(f())`) as the call drops its arguments: GeneratorExit is thrown in, and its finally
blocks and a monitor's events run there, with its frame's locals freed too. pyi gives it that moment without any
collection (in `go/arbace/py/finalize.go`: stampGen, freshGen, dropGen; and their call sites). The first build used
222's collection and was replaced after review 307: its candidates still held by a global or attribute each cost a
collection and a 20 ms wait, so the budget ran out and later deaths closed late.
- **The proof that the value stack was its only holder.**
  - At its making, a generator is stamped with the calling frame's address, its instruction (`Lasti`), and `callSeq`,
    which every call (callStack, AotCallV, AotCallMethodV) increments.
  - A generator counts as fresh if it has not run yet, was made by the instruction just before this call in the same
    frame, no call ran in between, and it was never handed out (`f_generator` marks it `exposed`).
  - No instruction ran between its making and this call, so nothing could have stored it. A walrus, a store or an
    attribute is an instruction of its own, so the pattern does not match.
- **The consumer.** The fresh generator must be the last argument of a built-in that keeps no reference to its
  arguments once it returns (`Builtin.dropsArg`: next, any, all, min, max, sum, sorted).
- **The close.** At the consumer's return (after C_RETURN, as in CPython's CALL), a generator that is still suspended
  and not exposed during the run is closed by `finalizeGen`. Then:
  - its frame's locals are cleared unless a frame object was handed out before (`escaped`);
  - the locals with a finalizer due get 222's del moment (`finDeleted`), preceded by `scrubSlots`, because stage 0's
    goi keeps a returned call's slots above its stack's top (c9047d52 is not in stage 0).
- **Cost.** One increment per call, and one flag test per built-in call (plus a type test of its last argument for the
  seven consumers). The close itself is the generator's resume with GeneratorExit, which CPython runs too.
- **What it does not cover.** These still close at a later collection, as at the base:
  - a generator made elsewhere, or passed through a name, then dropped;
  - a generator a frame holds when it returns;
  - a consumer outside the seven (list(), str.join, ...);
  - coroutines.

- **A hazard it exposed, fixed.** A throw into a generator is held in `genThrow` until the resumed frame takes it. A
  finalizer drained at that frame's start could close another generator and clear the slot, so the first generator
  resumed as if sent None ("generator ignored GeneratorExit": 6 of 12 runs of `.tmp/q297/t/mb.py` before the fix).
  resume now restores the throw it found when its own is done.

Measured on the VM (the probes of `.tmp/q297/t/check297.sh`, 10 runs each, compiled and on goi): review 307's g1
(3,000 `next(gen())`: late 0), its t1 E and F, the stored cases (s1: KEEP, an attribute, `make()`, a walrus,
f_generator: none closed), p13, pug and g13 (the five target methods). All 10 of 10, except t1 E's local
`__del__` in a generator with a handler on goi, 6 of 10: that check is 222's del moment, with its timing on the
loaded VM and stage 0's goi slots. `bin/pyi lib-test-all --fresh`: see the report of queue 297.

## As built: 298, _pyrepl on a terminal, and pyi's start on a pty (2026-11-15, the Mac VM, queue 298)

293's DECISION 3 (a): on a terminal CPython 3.14 runs _pyrepl, pyi ran its basic REPL; and pyi's start under a pty was
past test_pyrepl's run_repl wait (30 s of silence: `:repl-console`'s 20 pty sessions).

- **_pyrepl on a terminal** (interp.go's `endMain`, repl.go's `runPyrepl`): after the main module under -i, or as the run
  with no code, `_pymain_run_repl`'s terminal branch: stdin a terminal and no PYTHON_BASIC_REPL (not under -E or -I),
  `_pyrepl` imported (ModuleNotFoundError: the basic REPL) and `_pyrepl.main.interactive_console(mainmodule=__main__,
  pythonstartup=...)` called, pythonstartup after code (pymain_repl), not for stdin's run (its startup file ran before);
  an error printed as `pymain_exit_err_print`, SystemExit's status the run's. Its console is _pyrepl's own unix console
  (Lib/_pyrepl/unix_console.py, its terminfo reader, keymaps and history), on pyi's termios, select and os.
- **The interactive hook** (`runInteractiveHook`, pymain_run_interactive_hook): `sys.__interactivehook__` called before the
  REPL; pyi does not import site at start, so site is imported there (not under -I: site.main's enablerlcompleter),
  whose register_readline reads the history file (PYTHON_HISTORY) by _pyrepl's readline or the readline module and
  writes it at exit. `sys.flags.isolated` is -I's (296's PYI_START_FLAGS, `startFlag('I')`).
- **sys._baserepl** (repl.go's `baseRepl`): the basic REPL in __main__'s namespace, what interactive_console falls back
  on (TERM=dumb: "warning: can't use pyrepl"); `repl` and `baseRepl` share `replLoop`, which on a terminal with the readline
  module imported reads by its editor (`readlineInput`, PyOS_Readline's `call_readline`: the echo off, the prompt and line
  on stdout), as CPython's basic REPL does (test_no_newline, test_not_wiping_history_file's basic part).
- **The no-code __main__** (`mainModuleDefaults`): `__package__` and `__spec__` None, `__loader__` BuiltinImporter, as
  pylifecycle's add_main_module gives it (test_pyrepl's globals tests).
- **fcntl.ioctl with a buffer** (builtins_fcntl.go's `ioctlBuf`, fcntlmodule.c's cases): a writable buffer with
  mutate_flag copied into a 1024-byte buffer (larger: passed as it is) and back, the call's int returned; a bytes-like or
  str argument of at most 1024 bytes copied, the result's bytes returned; the guard bytes checked ("buffer overflow");
  the bytes passed by their address (reflect's Pointer, as termios's). unix_console's TIOCGWINSZ and FIONREAD.
- **The start** (measured on the VM under other agents' runs): goi's load of go.arbace.py was 19.7 s of a 27 s start at
  GOGC=50 (the runs' setting): its forms files read (6 s on one thread), the whole package checked (8.6 s), defined (2.4
  s). Now the start runs at GOGC 200 at least (arbace/python.ae's `with-start-gc`, the environment's setting restored
  before the program), and goi reads a package file's forms files ahead on GOMAXPROCS-1 threads while the package
  file's loads evaluate them (arbace/go/repl.ae's `start-read-ahead!`, `load-ahead`: arbace.core/load, :redef, takes
  the forms read ahead while a package file loads). `-c pass`: 34 s -> 25 s under the same load. The check itself is
  the rest; it is not cut here.

## As built: 313, review 307's findings 3-6: built-in types' __dict__, getattr's suppress, C events, the REPL's start (2026-11-15, the Mac VM, queue 313)

Review 307 (of 286-298) found four medium gaps; each is fixed as CPython 3.14.10 behaves.

- **A built-in type's `__dict__` is one mapping** (descr.go's `builtinTypeDict`, finding 3): 286 moved int's and float's
  `real`, `imag`, `numerator`, `denominator` out of the dict into getsets, and the slot wrappers were never in it; `in`,
  `[]` and `.get` found them (`ownSlot`), `keys()`, `values()`, `items()`, `copy()`, `len()`, iteration, `==`, `|` and the
  repr did not. The proxy of a static type's dict now reads a merged dict, in `type_ready_fill_dict`'s order: the own
  slots' wrappers in slotdefs' order (`__new__` first, `slotdefOrder`), the dict's own entries (pyi's methods), the
  getsets and members in their declaration order (attr.go's `getsetOrder`), `__doc__` last; kept per type while the
  dict's version and the types' epoch stand. A heap type's dict and `mappingproxy(d)` are as before.
- **getattr's default suppresses one lookup** (func.go's `moduleGetAttr`, lazyimport.go's `getAttrSuppressed`, finding
  4): 293's global `lazySuppress` counter made every module error under `getattr(o, n, d)` or `hasattr` the short one,
  in a property or a module `__getattr__` the lookup ran and in other threads. `suppress` is now module_getattro's
  argument (`_Py_module_getattro_impl(m, name, 1)` from `_PyObject_LookupAttr`): `getAttrSuppressed` passes it for a
  module whose class keeps module_getattro (no `__getattribute__` or `__getattr__` of a subclass's own), every other
  lookup is plain `GetAttr`; 3.15's lazy submodule load takes the same argument.
- **The code that stands for C fires C events at its caller** (monitoring.go, aot.go, with.go, finding 5): 293 silenced
  every event inside `_pyio`'s and `_py_warnings`' frames, so a profiler saw no `c_call`/`c_return` for io's methods
  (cProfile lost `{method 'read' of '_io.BufferedReader' objects}`). A call from a frame that does not stand for C of a
  function that does (`cImplCalled`; of `_py_warnings` only what CPython's warnings takes from `_warnings`: warn,
  warn_explicit, the lock functions) runs as a C callable's at the call sites with monitoring on (`monCallSite`,
  `callStackMon`, `AotCallMethod`, `AotCallMethodV`): CALL, the call, C_RETURN or C_RAISE, its events naming a stand-in
  `Builtin` (`cImplStandIn`: the method descriptor of the class in its first argument's MRO whose method it is, which
  the profiler binds to that argument, `BufferedReader.read`; or a module function of the C module, `_io`). cProfile's
  label names the C type (`_io.BufferedReader`, `_io._IOBase` for IOBase's methods); `open`'s module is `_io`
  (`<built-in method _io.open>`); a `with` statement's `__enter__` call fires CALL and its C events (LOAD_SPECIAL then
  CALL: a lock's `__enter__` had no `c_call`). Nothing fires inside those frames, as before (test_bdb's test_skip: its
  trace function's events are CPython's, now with the profiler's C events at the caller as CPython has them).
- **The REPL's start** (bin/pyi's cmd_python, interp.go, finding 6): with no code (or `-`) and stdin a terminal, without
  -i, CPython's pymain_run_stdin runs the REPL (stdin_is_interactive), PYTHONSTARTUP and `sys.__interactivehook__` first;
  bin/pyi read the terminal to its end as a script. It now starts the REPL (`PYI_INSPECT=tty`, `tty-quiet`: the run's
  REPL with sys.flags' inspect and interactive 0). A piped no-code run's `sys.argv` is `['']` (was `['-']`), `-`'s
  `['-']`. The hook in every REPL path and PYTHONSTARTUP after `-i FILE` on a terminal (_pyrepl's
  `interactive_console(pythonstartup=True)`) were 298's already; `.tmp/q313/t/check6.sh`'s eight start paths
  equal python3's.

## As built: 305, pyi's speed, round three (2026-11-16, the Mac VM, queue 305)

The brief: profile pyi compiled and interpreted on the bench and on slow Lib/test modules, find where the time goes,
and cut it without changing any result. Go's profile (`PYI_CPUPROFILE`) was taken on the compiled executables of 20
benchmarks (4 to 8 runs each, merged) and of an import probe (`import unittest, argparse, json, re, typing,
dataclasses, asyncio, ...`). A heap profile and `GODEBUG=gctrace=1` were taken at Py_FinalizeEx's start, on a build
instrumented for the purpose and not kept.

**Where the time goes (compiled; % of a benchmark's CPU, cumulative):**

| item | where it shows | weight |
|---|---|---|
| attribute load (loadAttrAt, typeGetAttr for a class's attributes) | deltablue 20% + 7.6%, richards 14%, float 14%, mdp 14%, go 13% | the largest call-heavy item |
| frames (newFrame, freeFrame) | richards 6% + 12%, spectral_norm 7% + 6%, coroutines 3% + 7%, raytrace 5% + 7% | `*f = Frame{...}` was 7% of richards |
| store-attr into a new instance's dict (insertNew: growslice, the index's map) | raytrace 18% | `__init__`'s stores grew the entries 1, 2, 4, 8 |
| int and float operations (numberFast, NewFloat) | nbody 47%, spectral_norm 35%, base64 14% | every number is a heap object |
| generator resumption (resume, run, guardedStep, AotExec's yield-from) | coroutines 72%, nqueens 76%, generators 59% | a nested run per level of `yield from` |
| Go's collector (gcBgMarkWorker) | async_tree 30%, hexiom 22%, go 17%, json_dumps 15%, 5-8% elsewhere | the live heap: 25 MB at a benchmark's end, a quarter of it attribute caches |
| reading the modules' IR text (ReadIR, the EDN reader) | hexiom 31%, chaos 12%, go 11%, nbody 10%, the import probe 21% | every import of a compiled module parses its IR |
| code run on the loop at start (step) | hexiom 60%, chaos 31%, go 27%, richards 12% | about 1,300 code objects of 41 sources compiled at run time (below) |
| the interpreter's end (Py_FinalizeEx's collections) | richards 25-73 ms, hexiom 18-83 ms, the import probe 200-240 ms of a process | 3 to 9 forced Go collections, each with a quiet wait of 3 ms or more |

An empty compiled program starts and ends in 5 ms. A benchmark's process is mostly its start and end when it is
short (hexiom: about 150 ms, CPython 63 ms).

**The changes (go/arbace/py; no IR, generator or bin/pyi change):**
- attr.go: an instruction's attribute cache (4 ways, 192 bytes) is made at its first lookup, and the code keeps an
  array of pointers. Before, it was made for every instruction of any code that ran one attribute instruction. That
  was a quarter of richards' heap. The live heap at the last collection went 29 -> 21 MB (raytrace), 25 -> 18
  (richards), 25 -> 20 (deltablue), 43 -> 35 (float), 48 -> 27 (the import probe), so every Go collection marks
  less, Py_FinalizeEx's forced ones too.
- attr.go: load-attr of a class whose metatype is type is answered from the instruction's cache entry for the class
  itself (CPython 3.14's LOAD_ATTR_CLASS). This applies when the attribute was found on a class statement's class
  of its MRO and type has no data descriptor of that name. The answer is d, or d's `__get__(None, cls)`, as
  typeGetAttr gives it. The entry is valid while typeEpoch holds, as every entry of the cache, and it is not used
  while a type watcher is set. The entry is marked only after typeGetAttr returned and only if the entry still holds
  the class's answer.
- frame.go: freeFrame zeroes the frame and then puts back Code, Fast and Stack. That takes half the time of
  assigning the literal (a micro-benchmark: 22 -> 11 ns). The value stack is cleared up to SP only, because its slots
  above SP are already nil: every pop, AotDrop and truncation clears what it leaves. This was checked with a panic on
  a stale slot, over the 20 benchmarks.
- type.go, class.go, dict.go: a class remembers the most entries store-attr gave one of its instances' dicts (at
  most 32). A new instance's dict is made with that room.
- registry.go, abstract.go: isNumberDunder compares `r`+name and `i`+name without building the strings (they were 5%
  of an import's start). binaryOp1's reflected method name is made once.

**The table** (the compiled process: the median wall and CPU milliseconds of 15 interleaved rounds, base 288fc7dc7
and head, B H then H B per benchmark; the VM's load 7.6-18.7, task 304 running beside it: single-run swings of 2x,
so the CPU columns are the readable ones; CPython 3.14.10's milliseconds from test/python/bench-aot-linux-arm64.edn):

| benchmark | CPython | base wall | head wall | base CPU | head CPU | CPU change | CPU / CPython |
|---|---:|---:|---:|---:|---:|---:|---:|
| raytrace | 306 | 960 | 720 | 787 | 700 | -11.1% | 2.6x -> 2.3x |
| deltablue | 180 | 719 | 599 | 564 | 466 | -17.4% | 3.1x -> 2.6x |
| fannkuch | 319 | 999 | 901 | 804 | 806 | +0.2% | 2.5x -> 2.5x |
| generators | 296 | 1007 | 970 | 857 | 837 | -2.3% | 2.9x -> 2.8x |
| nqueens | 357 | 838 | 804 | 732 | 735 | +0.4% | 2.1x -> 2.1x |
| go | 112 | 373 | 380 | 340 | 328 | -3.5% | 3.0x -> 2.9x |
| richards | 358 | 756 | 672 | 666 | 632 | -5.1% | 1.9x -> 1.8x |
| chaos | 139 | 359 | 325 | 310 | 290 | -6.5% | 2.2x -> 2.1x |
| json_dumps | 67 | 411 | 317 | 310 | 289 | -6.8% | 4.6x -> 4.3x |
| spectral_norm | 367 | 986 | 881 | 777 | 752 | -3.2% | 2.1x -> 2.0x |
| unpack_sequence | 263 | 948 | 896 | 767 | 724 | -5.6% | 2.9x -> 2.8x |
| coroutines | 517 | 1193 | 1234 | 1027 | 999 | -2.7% | 2.0x -> 1.9x |
| float | 378 | 748 | 683 | 591 | 593 | +0.3% | 1.6x -> 1.6x |
| nbody | 376 | 615 | 512 | 460 | 445 | -3.3% | 1.2x -> 1.2x |
| hexiom | 63 | 221 | 191 | 174 | 161 | -7.5% | 2.8x -> 2.6x |
| base64 | 1797 | 3766 | 3774 | 3317 | 3412 | +2.9% | 1.8x -> 1.9x |
| bpe_tokeniser | 3461 | 7871 | 7863 | 7063 | 7148 | +1.2% | 2.0x -> 2.1x |
| async_tree | 266 | 1217 | 1334 | 1290 | 1244 | -3.6% | 4.8x -> 4.7x |
| mdp | 948 | 4335 | 4028 | 3872 | 3569 | -7.8% | 4.1x -> 3.8x |
| pprint | 1536 | 5611 | 5620 | 5165 | 5191 | +0.5% | 3.4x -> 3.4x |
| the import probe | - | 461 | 412 | 428 | 416 | -2.8% | - |

Over the 20: CPU 29.9 s -> 29.3 s (-1.8%), wall 33.9 s -> 32.7 s (-3.6%), CPU against CPython's recorded run 2.47x ->
2.42x. A third run of 6 rounds (load 12-17) gave deltablue -12%, raytrace -8%, richards -6%, base64 -4% and
bpe_tokeniser +1.5%: the gains of deltablue, raytrace, richards, mdp and hexiom hold in all three runs, and the rest
is within the VM's noise. On goi, 8 interleaved rounds of a class's calls and attribute loads (30,000 iterations)
gave medians of 2.80 s at base and 2.62 s at head, and `python -c pass` took 31-35 s at both: no difference beyond
the noise.

**Lib/test** (`bin/pyi check --aot --lib-test --fresh` over 16 modules, every method's outcome the same at base and
head; the modules' run seconds as the check reports them): 6 runs, interleaved H B B H B H (load 11-15), all of
them with every one of the 4,195 methods' outcomes identical (3,621 pass, 572 classed, the same 2 failing at base
and head: test_functools' test_update_type_wrapper, a base failure from 313's type `__dict__`, below). The 16
modules' run seconds summed: base 584, 331 and 381, head 484, 403 and 419. That is no measurable difference: the
wall moves by up to 1.8x with the load between runs of the same tree. On goi, `bin/pyi lib-test-all --fresh
test_class test_scope test_list test_generators` took 772 s at head (load 17-18) and 657 s at base (load 15-16),
with the same outcomes (0 failing).

**Not changed, and why:** generator resumption, a design change (CPython's SEND_GEN in the caller's loop). The
numbers stay boxed: 202b and 210 already took the exact int and float paths. The dict's Go-map index stays: 202b's
open addressing was decided for after the re-seed. Py_FinalizeEx's collections stay: their rounds and quiet waits are
218's, 241's and 253's semantics (DECISION 1 of report 305).

**Found, not this task's:** test_functools' TestUpdateWrapper and TestWraps test_update_type_wrapper fail compiled at
base 288fc7dc7 too. `update_wrapper(wrapper, type)` copies `type.__dict__`, which since 313 holds type's getsets, into
the wrapper's `__dict__`, and `wrapper.__annotations__` then answers type's `__annotations__` getset. CPython answers
the function's own (a data descriptor ahead of the dict). The AOT lib-test record for arm64 does not exist yet, so the
check does not judge regressions.

**The next steps, by measured gain:**
1. The interpreter's end: Py_FinalizeEx runs finCollect and the two exitCollects. Each finRounds is one forced Go
   collection per wave plus one that finds nothing, and each round waits at least 3 ms for Go's finalizer goroutine
   to go quiet. That is 3-9 collections, 20-240 ms of every compiled process: hexiom 18-83 of about 200, the import
   probe about 200 of 450. Every Lib/test child pays it too.
2. The IR read at import: an AOT module's IR is EDN text, parsed into generic values and then into code objects at
   every import. That is 11-12% of go and chaos and 31% of hexiom. Emitting the code objects as Go data, or a binary
   form, would remove most of it.
3. The startup modules compiled ahead. 41 sources are compiled at run time and run on the loop in every benchmark:
   importlib's bootstrap, io, codecs, encodings, and argparse's lazy `_colorize`, which brings dataclasses, inspect,
   dis, ast and tokenize, plus 5 dataclass and 12 namedtuple execs. Probed by importing them at the top of hexiom's
   main module (1,296 -> 72 code objects interpreted), the CPU fell by only about 5%. Most of their cost is the IR
   read and the collections, not the loop.

## As built: 304, the next deferred classes (round 4): memoryview's formats, _bisect, property, struct_time, the seeds, the built-ins' checks (2026-11-16, the Mac VM, queue 304)

Night 12's partial eval record (181 modules) judged with 288fc7dc7's classes keeps only `:runner-limits` (252: whole modules
past the night's cap), `:testcapi` (53, by design since 244-4 (a)), `:finalization-collect` (27), `:memoryview-formats` (22,
286's Barrier rows: the record not run since) and single digits as deferred rows; the deferred classes with the most rows
are named by `:tests` from later runs of modules outside the record: `:multiprocessing-children` 95, `:finalization-goi`
82, `:memoryview-formats` 63 (41 named), `:capi-port` 63, `:regrtest-children` 58, `:io-c-checks` 48, `:pyi-command-line`
40, `:pydoc-builtins` 38, `:builtin-functions-core` 36, `:core-objects-287` 34, `:ordered-dict-c` 31 and below them
`:c-bisect` 23, `:property-core` 21, `:hash-algorithm` 17, `:structseq-protocol` 15, `:set-core` 14. The ones closed here by
fixes in pyi, the largest pyi-fixable first (the forkserver's and the C API's are not pyi code; goi's finalization is W2):

- **memoryview's formats** (`memoryview.ae`, `builtins_array.ae`, `bytes.ae`, `bytearray.ae`): an exporter's 1-D format
  (`BufferFormatter`: an array's typecode, as array_buffer_getbuf gives it; 'u' and 'w' stay byte views), so a view of an
  array('i') is a view of ints (len, items, slices by item, strides, tobytes, tolist, count, index); memory_richcompare's
  item comparison for native formats other than a byte format on both sides ('?' by truth, 'i' against 'B' by value, a NaN
  unequal to itself); BASE_INACCESSIBLE (a released view equals itself only); CHECK_RELEASED at `bytes(m)`,
  `bytearray(m)`, `with m:` and after an index's, a value's or a slice's conversion (gh-92888: a `__index__`, `__float__`
  or `__bool__` that releases the view); the tuple keys of a 1-D view (is_multiindex, is_multislice: an index's 1-tuple,
  NotImplementedError for slices, TypeError past one index); pack_single's TypeError for a value of no int ('B');
  `hex(sep, bytes_per_sep)` as bytes.hex's, the view exported meanwhile (gh-143195); `__buffer__` and
  `__release_buffer__` (wrap_releasebuffer's "memoryview's buffer is not this object"); the repr's address; a strided
  wider view's strides in bytes.
- **_bisect** (`builtins_bisect.ae`, new): Modules/_bisectmodule.c's four functions, so `bisect.py`'s `from _bisect import *`
  takes them: lo and hi as Py_ssize_t (hi None or -1 the length), the unsigned midpoint (issue 13496), get_sq_item's
  refusals, key, an exact list inserted into directly, anything else by its insert method.
- **property** (`descr.ae`, `attr.ae`, `class.ae`, `finalize.ae`): property_init_impl's subclass rule (the doc stored as the
  instance's own `__doc__`, gh-41287; a `__slots__` subclass without `__doc__` drops the getter's silently, raises for an
  explicit one), so a property subclass's instance has its `__dict__` and slots; getter_doc; property_copy as
  `type(old)(get, set, del, doc)` keeping the name; `__name__` with None a name (unset is NULL); the getter's
  `__isabstractmethod__` errors propagated; "property %R of %R object has no getter" with the owner's `__qualname__`;
  `__set_name__`'s argument count message.
- **struct_time and the struct sequences** (`builtins_time.ae`, `posix_structseq.ae`): `time.struct_time` is a struct
  sequence of 11 fields, 9 visible (n_fields, n_sequence_fields, n_unnamed_fields, `__match_args__`, `__replace__`,
  `__reduce__` with tm_zone and tm_gmtoff, copy and deepcopy); structseq_new_impl's "got duplicate or unexpected field
  name(s)" and `dict=None` refused; concatenation with a tuple and repetition by an int (a tuple's: its visible items);
  structseq_replace's messages. Not a tuple subclass still (`isinstance(t, tuple)`: test_tuple).
- **PYTHONHASHSEED** (`hash.ae`): `HashKey` is a package-level initializer, so every hash at the package's init is under
  it: an integer seed N keys siphash13 by lcg_urandom(N) (CPython's `_PyRandom_Init`), "random" by the system's random
  bytes; unset, 0, under -E or -I, or not a seed: the zero key, deterministic (CPython randomizes when unset: the
  DECISION of 304's report).
- **The built-ins' checks** (`sys.ae`, `builtins.ae`, `core.ae`, `class.ae`, `containers.ae`, `list.ae`, `dict.ae`,
  `set.ae`, `range.ae`, `slice.ae`, `descr.ae`): `sys.breakpointhook` and `sys.__breakpointhook__` (sys_breakpointhook:
  `$PYTHONBREAKPOINT`, "0", a builtin name, an unimportable one warned of), `breakpoint()`; NotImplemented has no truth
  (GH-79893); chr's overflow a ValueError; `__repr__ = None` blocks repr; a `__len__` past ssize_t an OverflowError;
  `None.__ne__(None)` is False (object's NE through EQ); PyObject_LengthHint's `__len__` errors propagate from list(),
  list.extend and bytearray.extend (issue 1242657); the dict iterators' `__length_hint__`; set_unhashable_type's message
  for any TypeError of a hash, set.discard's too; `set().difference(123)` iterates (issue 37219); the classes'
  `__dict__` and `__weakref__` descriptors' docs; a built-in exception's built-in subclasses in `__subclasses__()` (pydoc's
  "Built-in subclasses"); range's and slice's start, stop, step member descriptors.

Measured (`bin/pyi lib-test-all --fresh`, each test's own verdict, base 288fc7dc7 in a worktree of its own -> head, pass of
methods): test_memoryview 105 -> 146 of 171, test_bisect 23 -> 46 of 46, test_property 8 -> 29 of 31, test_builtin 83 -> 100
of 147, test_iterlen 14 -> 21 of 22, test_set 612 -> 620 of 630, test_structseq 10 -> 22 of 25 (rerun alone, `bin/pyi check
--only`), test_hash 13 -> 26 of 30, test.test_pydoc.test_pydoc 92 -> 99 of 121; test_array, test_buffer, test_bytes,
test_memoryio, test_fileio, test_struct, test_abc, test_enum, test_list, test_dict, test_inspect, test_pdb, test_time,
test_strptime unchanged method for method; test_io's two test_read_byteslike now refused as CPython's array.frombytes
refuses an 'i' view (_pyio's BytesIO.read1 calls the subclass's read: `:pyio`), its garbage-collection rows failing under
the VM's load pass alone (PyIOTest's fails alone at the base too). Classes (rows named), before -> after: `:memoryview-formats`
41 -> retired (40 pass, test_buffer_reference_loop to `:finalization-collect`), `:c-bisect` 23 -> retired,
`:property-core` 21 -> retired, `:builtin-functions-core` 36 -> 18, `:pydoc-builtins` 38 -> 11 (8 by these fixes, 19 that
passed since 290), `:hash-algorithm` 17 -> 4, `:structseq-protocol` 15 -> 3, `:set-core` 14 -> 5, `:core-objects-287` 34 ->
27. Judged with the head's classes, night 12's record has 22 more rows in no class (386 -> 408): its Barrier rows'
"memoryview: assignment to format 'i'", a message pyi has not raised since 286. Queue 304's report:
`.tmp/q304/304-python-deferred-classes-4.md` (the VM).

## As built: 316, the exit collections with less waiting (2026-11-17, the Mac VM, queue 316; 305's DECISION 1 (b), the user's)

Py_FinalizeEx's collections (finCollect, then exitCollect after the modules' removal and after their clearing) are 218's,
241's and 253's rounds unchanged: the same Go collections, the same drains, the same cycle probes in the same order. Two
things changed, both in `go/arbace/py/finalize.go` and `pylifecycle.go`.

- **A barrier in place of the quiet waits** (`finBarrier`). After `runtime.GC()` every round waited until Go's finalizer
  goroutine had run nothing for 3 ms (polled each millisecond, at most 500), there being no public barrier. There is one:
  `runtime.GC` returns with its sweep done, so every finalizer of its collection is queued by then, and runtime/metrics'
  `/gc/finalizers/queued:finalizers` and `/gc/finalizers/executed:finalizers` (the second counted once a block of
  finalizers has run) are equal once all of them ran. The wait now ends at that point, or when the caller's condition
  holds (finVerify's record moved, cyProbe's probes all fired), and is polled by Gosched and 20 us sleeps. A finalizer that
  does not return (no run counted for 3 ms) still ends it as 218's wait did. finRound, finVerify (222's moment at a
  deletion) and cyProbe use it. runtime/metrics is in goi's table, so the interpreted runtime reads the same counts.
- **The exit's last empty round skipped** (`finExitRounds`, `finDueLocked`). A round can only queue a dead object with
  work (`__del__`, a generator's close, a socket's warning, a tp_finalize) or with weak callbacks. When no tracked record
  has either and the queue is empty, the round's only effect is the Go finalizers of plain records: the record dropped and
  its plain weak references cleared. At the interpreter's end the next collection does that before any Python code of the
  end runs (that code runs in drains, each after a collection), so such a round is skipped there: in Py_FinalizeEx's
  finCollect when gc.callbacks is empty, and in exitCollect. Before a cycle collection the skipped round is owed
  (`finExitOwe`, `finRoundOwed`): a dead object still under its Go finalizer would hold what it reaches through the probe's
  collection, so cyCollect runs the owed round first when its graph has anything to probe (or passed `cyMaxNodes`,
  `cyGraphFull`), and nothing when it does not. exitCollect with dead module dictionaries always probes, so its first
  rounds keep every round. gc.collect() keeps every round: a program reads its weak references right after it (an object
  only a callback-only object held is freed in the round after the callback's).
- **What it leaves**: the residual skipped round is observable only to Python code of the end outside a drain that reads
  a plain weak reference to such an object: a user's `sys.__stdout__` whose `flush` does so. gc.callbacks at the end keep
  every round. The other empty rounds at the end (a record with work alive, `due` true) still run: the import probe's 9
  collections are 9.

**Measured** (the VM, base e892ff58b against head, compiled, interleaved B H / H B; Finalize timed by an instrument in
the AOT module's printed copy, not committed; `.tmp/q316/ab/`):
- Finalize, median ms (20 rounds, load 6.9-7.4): richards 39.3 -> 17.1 (Go collections 2-3 -> 2), hexiom 30.6 -> 11.1
  (3 -> 2), the import probe 118 -> 81 (9 -> 9); an earlier round with the barrier alone (15 rounds, load 6.6-7.4):
  richards 27.2 -> 12.4, hexiom 22.0 -> 12.0, the import probe 90.4 -> 44.5. Finalize's share of the process's wall:
  hexiom 21.9% -> 8.3%, the import probe 35.4% -> 25.7%, richards 6.6% -> 3.0%.
- The benchmarks (30 interleaved rounds, two runs of 15, load 6.9-10.0, warm): CPU median change richards -2.9%,
  deltablue -2.0%, raytrace -1.7%, mdp +0.2%, hexiom -5.5%, float -3.2%, base64 +1.0%, the import probe -1.1%; the wall's
  medians move by up to 15% either way between the two runs at this load (the gain is a few to 40 ms of wait per process).
  Every run's stdout the same at base and head.

**Order unchanged** (the VM): 218's, 241's and 253's own cases are gone with their scratch; six cases written anew
(`.tmp/q316/cases/`: rounds and waves of `__del__`, weak callbacks' list order, a plain reference held by a callback-only
object, resurrection, generators closed, the moment at `del`, gc.callbacks, WeakValueDictionary and WeakSet; cycles with
`__del__` and callbacks, PEP 442's order, a `__del__` that breaks its cycle; the end with module globals' `__del__`, a
wave at exit, cycles, callbacks, gc.callbacks, atexit, a suspended generator; the end without gc.callbacks, where the skip
applies; SystemExit; synth/datamodel/del.py): compiled, every output equal at base and head (the moment at `del` in the
rounds case moves at base too from run to run under load: 222's time budget; 6 of 6 pairs agree). On goi the rounds and
cycles cases are not stable at base itself (3 base runs, 3 outputs: goi's registers, `:finalization-goi`), and head's
outputs are among base's; the four end cases equal. Lib/test compiled (`bin/pyi check --aot --lib-test --fresh`, 12
modules: test_weakref, test_gc, test_finalization, test_module, test_atexit, test_threading_local, test_struct, test_sys,
test_coroutines, test_queue, test_generators, test_threading): 985 methods, every outcome the same (806 pass, 178 classed,
test_threading's test_can_interrupt_tight_loops failing at both; base's first run had 20 more child-process failures in
test_threading and test_gc, a rerun of the two modules alone at base gave head's outcomes).

## As built: 318, the read and print cells' passes (2026-11-17, the Mac VM, queue 318)

The pieces were all there already (PY10's R1, queues 212, 235, 265). `bin/repr-python roundtrip [--text]`, `lex-check` and
`parse-check [--full]` with `--record` write test/python/{roundtrip,full,lex-check,parse-check}-results.edn
(`arbace.repr.python.results/write`: rows ["CORPUS FILE" :pass|:known|:fail|:pending REASON]). test/reap/cells.edn's Python
read and print sources name them as `:results` of the `:c-known` reader. bin/records-check (GD4, on next) takes them as pairs
of that reader. What was missing is a record. No whole run had been recorded, so the cells counted no pass ("0 pass / 66
design", "0 pass / 5 design"). Queue 318 ran the chain on a recorded slice and committed nothing but this text. The results
files are the main host's, from whole runs.

**The slice** (`.tmp/q318/slice.list`, 458 lines; REPR_PYTHON_ORACLE=live, python3 3.14.10; stage 0 of next's seed
3c48859d): every other file of stdlib up to 300, plus the controls' pairs (303 files); samples (6); lex-errors (62); every
file of the four known files (lib-test 13, site-packages 23, syntax-errors 33, the mutant m01045-0.py); 16 more mutants.
Every run exited 0, and every negative control failed as it must:
- `roundtrip --record`: 345 files, 311 pass, 34 known (29 s, 355 MiB);
- `roundtrip --text --record`: 345 files, 341 pass, 4 known (82 s, 489 MiB);
- `lex-check --record`: 420 rows, every row a pass (48 s, 380 MiB);
- `parse-check --record` and then `--full --record`: 453 rows, 422 pass, 31 known (60 s and 73 s, 534 MiB). After the
  :lines run alone, every row is :pending (queue 265), as it should be.

**Read back**: `bin/reap` judges Python read at 376 / 64 / 0 (each file counted once over the three checks) and print at
341 / 4 / 0, with no problem besides the matrix's record. next's records-check gives each of the four pairs as ok, and its
four controls fail as they must. `bin/crops-score --crop python --input` the slice's cells (on next) moves Python's
completeness from 0 to 46: read 85, print 99.

**Found: four known entries are stale on next's runtime.** next's charsets (JC9b's F8) decode KOI8-R and cp1251. 201's
stage 0 still refuses them: `lex-check lib-test` on module_koi8_r.py gives "refused" on nf's stage 0 and "equal" on nx's.
So on next's stage 0, `Lib/test/encoded_modules/module_koi8_r.py` passes lex-check, parse-check (:lines and :full) and
roundtrip --text, and the syntax-errors case `test_exceptions-0012.py` (coding cp1251) passes parse-check in both modes. Only
`test_exceptions-0043.py` (iso8859-7) is still :charset. Without a fix, the non-record runs on 201 merged onto next fail
those files as GONE. The entries were dropped by hand on 318's merge of 317 (lex-check-known, parse-check-known's :lines
and :full, full-known), the same entries a slice record drops. The five judged runs of the slice then pass (no GONE), with
29 controls failing as they must. test/reap/matrix.edn and test/crops/scores.edn were recorded again: read design 66 -> 64,
print 5 -> 4, the passes still unrecorded. The :charset class texts still name KOI8-R; the whole record keeps them, and
only iso8859-7 is left.

**The main host's whole record** (amd64, a host with python3 3.14.10, the live oracle; the order matters: parse-check's rows
are :pending until both modes ran):

    export REPR_PYTHON_ORACLE=live
    bin/repr-python roundtrip --record --jobs 4
    bin/repr-python roundtrip --text --record --jobs 4
    bin/repr-python lex-check --record --jobs 4
    bin/repr-python parse-check --record --jobs 4
    bin/repr-python parse-check --full --record --jobs 4
    bin/reap --record && bin/records-check && bin/crops-score --record && bin/crops-score --judge --strict

Commit the four results files, the known files the record rewrote, the two costs files, test/reap/matrix.edn and
test/crops/scores.edn together.

## As built: 319, bin/pyi check with no CPython on the machine, both drivers compared (2026-11-17, the Mac VM, queue 319)

The brief (from next's facts, `test/crops/independence.edn`'s `:python :eval` 0 at `bin/pyi:183 pyall() { python3`) asked for
the check's driver and universe without python3. That held at the base already: since queue 234 the restartable runner
(`tools/pyi/libtest_all.py`: `owned`, `run`, `controls`, `table`) runs compiled by pyi's AOT (`pyrun`, `runner_exe`), since 240
the universe's modules are regrtest's findtests run on it (`find`), since 256 the methods' inventory is a record of its own
(`universe-cpython314.edn`, written on stage 0); each module's driver (`test/python/libtest_run.py`) runs by `bin/pyi run`, its
children (`sys.executable`, `tools/pyi/python3`) by `bin/pyi python`. python3 stays only where CPython is the subject: `--oracle`
(the driver's control: CPython's own runs), `record-ref` (`list`, `refs`: the references), and `bin/pyi cover run` (the coverage
subset's maintenance, outside the check, its runner python3 because the cover build's module is its own `WORK/aot-cover`).

- **Proved with no CPython on the machine** (stronger than 234's and 256's PATH stubs, which an absolute `/usr/bin/python3` or a
  test's own lookup would pass by): each run in a mount namespace of its own (`unshare -m`) with `/usr/bin/python3.14` (and so
  `python3` and `python`, its links) bind-mounted over by a stub that logs its call and exits 99. `bin/pyi check --fresh --only
  LIST` over 11 modules (test_bool, test_getopt, test_colorsys, test_augassign whole; test_with, test_complex, test_fstring,
  test_subprocess, test_cmd_line_script by test; two scratch modules, below), `PYI_LIB_TEST_TEST_TIMEOUT=60
  PYI_LIB_TEST_IDLE_TIMEOUT=20`: 92 methods, 75 pass, 10 classed, 7 failing (the scratch modules' own failures), the three
  controls failing as they must; the stub's log empty. The universe (`find`, then `owned`) was written in the same namespace. The
  stub's own control: `PYI_RUNNER=python3 bin/pyi runner owned ...` in the namespace exits 99 and logs its call.
- **Both drivers, the same rows**: the same selection run by the python3 runner (`PYI_RUNNER=python3`) and by the compiled one:
  `results.tsv` 92 rows, 91 equal byte for byte; the 92nd is test_cmd_line_script's test_basic_script, whose failure message
  names its `tempfile.mkdtemp` directory (`.tmp/t6i/jN/tmpXXXXXXXX/`): it differs between two runs of the same driver too, and
  with those names masked the rows and every module's `.out` are equal. The modules' driver-run counts are the same; `find`'s
  540 modules and `owned --invert`'s 427 equal byte for byte. CPU seconds of the whole check: 99 compiled, 78 by python3 (one
  run each; the compiled runner's own start and pyi's speed).
- **The scratch modules** (put into the worktree's extracted `Lib/test` for the runs and removed; never committed, in no
  universe: judged by their own verdict, as pyi-only tests): `test_q319synth`, a test for each outcome the runner writes (ok,
  failure, error, skipped, expected failure, unexpected success, a subtest's failure, output without a newline; children by
  `sys.executable` and the shell; a driver that exits (`os._exit(7)`) and one killed (SIGKILL) in a test, each followed by a test
  that runs on the restarted driver; an idle hang killed by the idle timeout, a busy test by the test timeout), and
  `test_q319import`, a module that does not import (the `IMPORT` row). 5 driver runs for the first, as the four deaths make.
- **A dead driver's cause without the run's PID** (`first_cause`): a driver killed by a signal had bin/pyi's shell's job line as
  its cause, `<checkout>/bin/pyi: line 520:  3473 Killed   ARBACE_PATH=...`: the process's PID (another each run, so a record's
  row changed every night), the checkout's path and the command's text. It is now `the driver died, Killed (status 137)` (the
  signal's description as bash gives it, `Segmentation fault (core dumped)`, `Aborted` and the rest alike); a Go panic's and
  Arbace's report keep their precedence. test_fstring's test_many_expressions (`:host-memory`, classed by name) is the one
  recorded method whose reason this changes.

## As built: 320, the read checks judged from the records (2026-11-17, the Mac VM, queue 320)

The task (Python judge independence 0 to 1) read independence.edn's fact of 2026-11-09: "the read cell's lex-check,
parse-check and the round trip compare with CPython live". The scripts were past it: since 213, 240 and 257 every gate step
of repr/python runs in records mode by default (`tools/pydump/records.bash`, `test/oracle-records/oracle.bash`), python3
only in the oracle tier (`test/gate/crops/python-oracle.edn`). A stub run proves it: python-read-essential's command
(spec-check, samples, both round trips, lex-check and parse-check --full over samples and lex-errors) with python3,
python and python3.14 first on PATH as stubs exiting 99: rc 0 in 11 s, the stubs' log empty. What 320 changed:

- **Level 5 implied by level 4** (`arbace/repr/python.ae` judge-text). `roundtrip --text` reaches level 5 (marshal of the
  code compiled from each file, positions included) only for a print whose bytes passed level 4, the original's bytes under
  the original's name, which compile to the same code: in records mode it is judged so ("level 5 (marshal) N (implied by
  level 4: the original's bytes)"), not left to the oracle tier. Live, it stays CPython's compile of both.
- **verify of an unpinned print** (`bin/repr-python` roundtrip_one). In verify mode the print's rows were checked against
  python-pydump-printed-CORPUS even when no such pack is pinned, every row then counted as differing: `records verify
  samples` said "6 of 12 entries differ", a failure on the host whose identity the records name (python-records-verify's
  essential part, the oracle tier). It verifies the print once its pack is pinned, and says so before.
- **A record keeps the pin's members** (`tools/pydump/records.bash` pyrec_flush). A record merged this run's members into
  the pinned pack's taken from the store; where the store lacked it (a scratch `ARBACE_ORACLE_PACKS`, a fresh clone) the
  pin was rewritten with this run's members alone. On the VM `lex-check --mutants 20 samples` in record mode dropped
  samples' tree and scopes members, 42 to 12. The earlier members now come from the essential corpus's committed copy
  (checked against its manifest), else the store, else the record is refused.
- **`records record --sample N`**: a slice's pack, as verify's `--sample`, to test the tooling (not for commit).

The proofs on the VM (CPython 3.14.10, apk python3-3.14.10-r1, aarch64, `:python` 252f2d87114360e8; the records' are
the main host's, amd64, c94aabf8e8b16934):
- `bin/repr-python records verify samples lex-errors`: every pack "all equal" (samples' 12, 6, 6, 6, 16, 9 entries,
  lex-errors' 62 three times), the crossed pairs differ; the VM's CPython answers as the main host's.
- samples re-recorded on the VM (in the mutants' run): the pack 2d289d7d68cc1d5d, the pinned one byte for byte; only the
  manifest's header (identity, verb, date) differs.
- a stdlib slice (40 files evenly spread, and the two of its control pair: 42) recorded into a scratch store
  (`python-pydump-stdlib`, 252 members, 1.47 MB xz; `python-pydump-printed-stdlib`, 42 rows, 7.5 KB): lex-check, parse-check,
  parse-check --full, roundtrip and roundtrip --text over it in records mode with the stubs (no call) and live: every rc 0,
  out/files.tsv and results.edn byte for byte equal, the verdicts the same (roundtrip: level 3 41 of 42, Lib/pdb.py known
  :code-lines; --text 42 of 42), every negative control failing as it must (the levels 2 and 3 control too); records 38.9 s
  user, live 30.5 s, 0.48 GB peak.
- the controls on that pack: its bytes one byte off (refused at unpack), an unpacked member one byte off (refused by the
  members' check), a manifest key changed (STALE), each failing lex-check; a member replaced by another file's dump with the
  manifest made to match (pins pass): lex-check "differ 1", rc 1.
- mutants: `lex-check --mutants 20 samples` and `parse-check --full --mutants 20 samples` recorded (mutants-lex,
  mutants-parse, 24 members each), then in records mode with the stubs: the same verdicts as live.

What is left is the main host's: the packs of the big corpora (stdlib, site-packages, lib-test: about 100 MB xz in all,
from the slice's 35 KB a file), the mutants' (2,000 files) and the prints' (python-pydump-printed-CORPUS, samples' members
committed under `essential/printed-samples`), recorded by `bin/repr-python records record samples stdlib site-packages
lib-test mutants` and pinned; until then the full gate's python-roundtrip and python-native parts of those corpora fail
UNRECORDED in records mode, and the essential round trip leaves levels 2 and 3 of samples to the oracle tier.

On the merge with 317 (next's shared tooling): Python's kinds in `test/oracle-records/kinds.edn` (pydump, outcomes, runs:
`bin/oracle-records list|record|verify python`), `oracle_python_id` in `test/oracle-records/oracle.bash` (the `:host-ids
:python` recipe records.bash patched in since 213; records.bash now asks whether the library has it by `declare -F`, since
computing it ran `python3 --version` at every start, 6 calls in the stub run), and the judge's fact in
`test/crops/independence.edn` as a case-weighted mean: 0.196 (66,657 cases; Lib/test's 47,621 methods live, 0, until
319's driver), Python's independence 60 to 64; with 319 and the methods by their own verdict (queue 234), 0.910.

## As built: the review of 317-320 (2026-11-17, the main host, h-review-mac-py)

320's premise for Lib/test was stale: `bin/pyi check` runs no python3 by default since queues 234, 256 and 279 (its runner
tools/pyi/libtest_all.py compiled by pyi's AOT, `pyrun`; the universe by findtests on pyi; each method by its own verdict;
python3, `pyall`, only for `--oracle` and `--cover`). Shown on amd64 by `bin/pyi check --only test_bool` (with the controls'
test_getopt) with python3, python and python3.14 stubbed to exit 99: rc 0, no stub called, 41 of 41 methods, the three negative
controls failing as they must. So the eval fact is 1 (`:steps [pyi-check]`) and the judge's `:lib-test` cases count 1 (the user's
DECISION 3 (a)): judge 0.196 to 0.910, Python's independence 64 to 98. Queue 319's port is moot (its remainder: a wider stub run).
The review (`.tmp/goal/review-mac-python.md` on the main host) also found that 317's splice of test/gate/crops/python.edn fails the
planner until the main host's records (the big corpora's packs, the four results files, the eval and AOT records) are committed.

## As built: 321, the eval and AOT cells' records night as one command, and its dry run (2026-11-17, the Mac VM, queue 321)

The eval and AOT cells wait for two amd64 records, `test/python/check-lib-test-linux-amd64.edn` (`bin/pyi check --record`,
reap's `:results`, `:py-ref-modules`) and `check-aot-linux-amd64.edn` (`bin/pyi check --aot --record`, reap's `:pyi-aot`, read
for the AOT cell and for the eval cell's loop and goi columns). 321 makes that night one command and ran it over a slice on
the VM. Base 201 cb8457fc0, then 317's merge (80a6f9eaa, 201 onto next): reap's readers and cells.edn are next's after it
(the Python cells and the readers `:results`, `:pyi-aot` unchanged by next).

- **`bin/pyi records`** (`make pyi-records`): the host first, then `check --aot --record` (the 151 programs with pyi's runs)
  and `check --fresh --record` (every module of the universe, afresh: a resumed run would record an older tree's modules),
  each under busybox time with a 5 s sampler of its processes' summed resident memory (`WORK/records/STEP.{time,mem,log}`),
  then the `:until "PY7 ..."`/`"PY8 ..."` of each source whose record is written dropped from `test/reap/cells.edn` (a
  recorded `:until` is a reap problem), `bin/reap --record` and `bin/crops-score --record`. linux/amd64 only.
  `--slice N` is its dry run: every Nth program of the AOT list and every Nth module of the check's universe (whole
  modules: reap reads Lib/test's record per module, `:py-ref-modules`, and the judge refuses `--record` of an `--only`
  selection), the records under `--dir` (default `WORK/records-slice`, named as the cells name them, never to commit), read
  through an overlay of the checkout (`DIR/overlay`: links, but `test/python`'s two records, a reference of the slice's
  programs alone, `ref-cpython314-slice.edn`, and cells.edn without the `:until` and on that reference) by `bin/reap
  --record` and, when reap writes its matrix, `bin/crops-score --crop python`. `--only aot|lib-test` one step.
- **The host** (found by the dry run: on next's stage 0 `import _bisect` fails, on 201's it works). Since W2 (seed 3c48859d)
  stage 0 lists `arbace/py`, `arbace/libm`, `arbace/ci` and `arbace/crt` in goi's table, and RT.load makes a listed package's
  namespace from the table before any source: `bin/pyi run` on that executable runs the runtime of its build's commit
  (`*arbace-version*`'s `:commit`, 53da41d93 for next's seed), never the checkout's `go/arbace/py`, silently. On 201's tree 46
  of the four packages' files differ. `runtime_state` asks the executable (`GoInterp/paths`, `*arbace-version*`, 0.1 s) and
  compares that commit's packages with the checkout's (`git diff`, untracked files too): `interpreted` (not listed: goi
  interprets the checkout's), `compiled COMMIT` (listed, the same files) or `stale COMMIT N`. `runtime_guard` refuses a
  `--record` of `check`, `check --aot` (its pyi runs), `lib-test` and `lib-test-all` on a stale host
  (`PYI_RUNTIME_STALE=1`: a dry run's override) and warns on every other run of them; check --aot's cached pyi runs
  (`pyi_key`) name the state, so a stale host's runs are not reused. `bin/pyi records` takes the gate's build at the
  checkout's key (`arbace_gate_cache`/amd64/arbace: built from the checkout, its runtime compiled in) for a stale stage 0,
  else stops with `bin/arbace-go --gate-build`. The compiled columns (`check --aot`'s compiled and loop runs, `check --aot
  --lib-test`) were never affected: their runtime is the checkout's forms printed and built by `aot_module`.
- **The runner's memory cap** (found by the dry run): test_capi's driver reached 29.6 GB resident on the VM (GOMEMLIMIT is
  soft; killed by hand, then by a watchdog at 6 GB: test_modulefinder's test_extended_opargs too; test_adjustindices, the
  test it was in, passes alone, so it is the module's growth). `tools/pyi/libtest_all.py run --mem-max MB` samples the
  summed resident memory of a driver's processes every 5 s, as `--idle-timeout` their CPU, and kills a test past it,
  written `crash the test's processes past MB MB resident (killed by the runner's memory cap; the driver started again)`;
  bin/pyi passes `PYI_LIB_TEST_MEM_MAX` (default 8192). `:host-memory` (divergences.edn and divergences-315.edn) matches the
  line. Proved by a fake driver (400 MB against a 200 MB cap: the test written so, the driver started again, the next test
  run, under python3 and under the compiled runner) and by `bin/pyi divergences --results` of such a row (`:host-memory`).
- **The dry run** (`bin/pyi records --slice 20`: 8 of the 151 programs, 27 of the 529 modules, among them test_subprocess,
  test_capi and test.test_multiprocessing_spawn.test_manager; 2 jobs, GOGC=50, the VM loaded by 7 other agents):
  - on 201 with next's stage 0 (`PYI_RUNTIME_STALE=1`: next's runtime compiled, so the night's speed): AOT 8 pass, 10 min 49 s
    (the runtime printed), 3.0 GB; Lib/test 3,713 methods: 2,532 pass, 1,163 classed, 18 in no class (left out of the record:
    reap's "18 cases ... not in" the record, its one problem), **31 min wall, 5,467 s CPU**, the modules' runs summed 3,708 s
    (test_multiprocessing_spawn.test_manager 1,792 s, test_capi 690 s, test_subprocess 660 s), peak 32.9 GB summed (the
    test_capi driver above). 8 of the 18 were next's runtime's (test_set's 37219 and unhashable message: 304's, passing on the
    checkout's runtime).
  - on 317's merge with 201's stage 0 (`interpreted`: the checkout's runtime, on goi): AOT 8 pass, 9 min 32 s, 617 s CPU,
    3.0 GB; Lib/test 3,713 methods: 2,100 pass, 1,518 classed, 95 in no class, **1 h 54 min wall, 14,422 s CPU**, peak 14.9
    GB summed (goi's drivers past 6 GB killed nine times by the VM's watchdog, written `crash Terminated`: with the runner's
    cap they are `:host-memory`'s). Of the 95: three on both runtimes, now classed (test_capi's test_long_fromstring in
    `:testcapi-behaviour`, test_functools' two test_update_type_wrapper in `:functools-core`); goi's alone (passing compiled):
    test_manager's 82 (the driver killed with no output, status 137, in 256 s; 1,792 s and results compiled), test_functools'
    two TestPartialPy test_weakref (a weak reference not cleared on goi), test_subprocess' test_leak_fast_process_del_killed;
    the watchdog's eight. reap through the overlay: every Python cell checked, eval 2,108 / 419 / 1,099, AOT 8 / 0 / 0; its one
    problem the 13 left-out methods (test_manager's 82 not among them: below). With the overlay's matrix written whatever the
    problem (a scratch script), `bin/crops-score --crop python`: completeness 40 (eval 58, AOT 100), maturity 92 (the four
    cells recorded), independence 60.
- **The night's time and memory** (from the compiled slice, the night's host): the modules' runs about 20 x 3,708 s =
  74,000 s, so about 5 h at 4 jobs on the VM's cores and about 3-4 h on the main host; check --aot about 20-30 min with
  pyi's runs compiled in (70 min on goi, 287). Peak: 4 drivers of 1-3 GB with their children, each capped at 8 GB by the
  runner: under 35 GB.
- **Left for the night** (findings): a method in no class is left out of the record (libtest_judge's rule: no placeholder
  class), so the first whole record will lack the methods the slice could not see (about 20 x 13 on its evidence); the night
  keeps its run (`WORK/check/run`), and after classing them `bin/pyi check --judge .tmp/repr-python/pyi/check/run --record`
  writes the record again with no run (the guard does not apply to a judge), then `bin/reap --record`. reap reads Lib/test's
  record per module (`:py-ref-modules`: the modules the record holds), so a module whose every method is left out (the slice's
  test_manager on goi) leaves the record without a problem: the judge's "N failing" line and exit 3 are the night's signal.
  Pre-existing: `bin/pyi divergences` reports two shadowed names (test_capi's test_no_FatalError_infinite_loop under
  `:testcapi`, test_pyexpat's test_error_path_no_crash under `:cpython-only`).
- **Round 2** (on python-1117-on-next merged with next d694a2f7e, which brings h-pyi-runtime's `runtime_from_source`). With it a
  stale stage 0 runs the checkout's go/arbace/py (and libm) interpreted from source (`ARBACE_GOI_FROM_SOURCE`), so
  `runtime_state` reads that decision: `interpreted`, `compiled COMMIT`, `checkout COMMIT` (the checkout's interpreted) or
  `stale COMMIT N`. Stale can now only come from `PYI_RUNTIME=compiled` on an older executable, or from ci and crt, which
  runtime_from_source leaves compiled. `runtime_guard` still refuses a `--record` in every state but compiled. The
  checkout's runtime interpreted is correct, but it runs at goi's speed and has goi's own divergences (this slice: 1 h 54
  min against 31 min, test_manager's 82 and three more failing on goi alone), so it is not the record the gate's compiled
  checks judge against. Off a record, a stale runtime fails unless `PYI_RUNTIME=compiled` asked for it.
  `bin/pyi records` takes the state its checks will see (`records_state`). DECISION 1 (a) (the user): pyi-check,
  pyi-check-aot-goi and pyi-runtime run with `ARBACE_HOST_EXE="$GATE_EXE" PYI_RUNTIME_REQUIRE=compiled` (`:prog`,
  `go-build`). The gate's build at the checkout's key compiles the checkout's runtime in, since go/arbace/py and libm are
  in gate_key, and any other state fails the step; `bin/pyi runtime check` passes through the guard. DECISION 2 (a): the
  record's header names its modules (`;; (modules M ...)`, kept across writes), and reap wants the methods of every one.
  Re-judged on this slice's run, reap reports 92 missing methods, the control without the line 10: test_manager's 82 had
  been silent.

## As built: pyi runs the checkout's runtime when stage 0's is older (2026-11-17, h-pyi-runtime; round 51, Mac task 317's DECISION 2)

Since W2's re-seed (seed 3c48859d) stage 0 lists pyi's runtime `arbace/py` and the libm port it imports in goi's table, so
`bin/pyi run` and every check on stage 0 ran the seed's compiled runtime: a change to go/arbace/py was not exercised by
pyi-runtime or pyi-check-aot-goi until the next re-seed (317's finding: queue 304's `_bisect` failed under stage 0). The
namespaces needed nothing: RT.toolSource already loads a changed arbace/python, arbace/repr/python or arbace/go namespace
from the checkout.

- **The decision** (bin/pyi's `runtime_from_source`, once by the outermost bin/pyi of the verbs that run pyi, exported to
  its children): the executable's version map (`*arbace-version*`, its commit; read once and kept in
  `.tmp/repr-python/pyi/exe-version/` by the executable's path, size, time and inode) against the checkout's
  `go/arbace/py{.ae,/}` and `go/arbace/libm{.ae,/}` (`git diff --quiet COMMIT --`, the working tree's changes included,
  about 10 ms). Equal: the compiled runtime (`ARBACE_GOI_FROM_SOURCE` empty). py differs: `arbace/py` from source; libm
  differs: both (compiled py would call compiled libm). An executable of no commit of this repository, or built dirty:
  both. `PYI_RUNTIME=compiled|checkout` overrides. One line on stderr says which runtime runs (`pyi: the compiled runtime
  of ...: go/arbace/py and libm are the executable's 53da41d93b5d`, or `pyi: the checkout's runtime (arbace/py)
  interpreted by goi: go/arbace/py differs from ...`), printed once (`PYI_RUNTIME_NOTED`), never by `bin/pyi python`
  (sys.executable), and by `run` and `repl` only when the checkout's runs (the program's own stderr otherwise).
- **goi's half** (arbace.go.repl, doc/go/GOI-SPEC.md §6.6): `ARBACE_GOI_FROM_SOURCE` names table packages that goi treats
  as unlisted (`from-source`, out of `listed?` and `interpretable?`) and loads from their forms on ARBACE_PATH
  (`require-source!`: RT.load would make the namespace from the table, so it loads the package file itself, completes the
  package as the after-load hook does and adds the namespace to the loaded libs; `load-import` takes it for such a path).
  `arbace.python.run/require-runtime!` (and arbace.python's runtime-fn) calls it before requiring `go.arbace.py`, so
  pyexpat, pybz2 and the other lazy packages, interpreted, import the same copy. No package goi's table lists imports
  `arbace/py`, and only `arbace/py` (and hsi's `arbace/hs`, outside pyi's runs) imports `arbace/libm`.
- **The cost**: the checkout's runtime starts in about 45-65 s against 2-3 s compiled, at a 2.3-3.1 GB peak (2.3 GB under
  `PYI_GOMEMLIMIT=2GiB`): the pre-W2 cost, only on a checkout whose runtime moved past stage 0's.
- **`bin/pyi runtime programs`**: runtime check's 145 programs and their controls, then repl --check, without its print,
  read and tables (`runtime_programs`, shared with `check`).
- **Proved** (stage 0 at 53da41d93b5d, next 8049a4359): a scratch change to `go/arbace/py/sys.ae`'s sysVersion (not
  committed) seen by `bin/pyi run` (`3.14.10 (pyi CHANGED, Arbace) [Go]`, the message naming go/arbace/py); with no change
  the compiled path (2.1 s, the message naming the executable's commit); `bin/pyi runtime programs` on the compiled path
  (live oracle, `REPR_PYTHON_ORACLE=live`: the smoke outcomes are not recorded yet) 143 of 145 equal, the two (d-mmap's
  module constants, f-struct's one number) this host's CPython against pyi on the unchanged runtime, the controls failing
  as they must, repl 2 of 2; the interpreted path with a neutral scratch change (a docstring of sys.ae) over a slice
  (arith, calls, d-generators, d-json, e-builtins, f-time) 6 of 6, both controls failing as they must, repl 1 of 1; a
  scratch change to libm.ae runs both from source (math.cbrt, erf, exp2 as CPython's); `PYI_RUNTIME=checkout` likewise.

## As built: 330, a yield-from chain resumed without each level's frame (2026-11-17, the Mac VM, queue 330)

305's profile put 59-76% of generators, nqueens and coroutines under the resume chain (resume, run, runFrames,
guardedStep, the compiled function's dispatch, AotExec, opYieldFrom, genSendTo, per level of `yield from`). Measured again
at the base (Go pprof, 4-6 runs merged): the chain's own frames are about a third of bm_generators' CPU (a tree's
`yield from` 17 deep per item); in bm_coroutines and bm_nqueens they are 8% or less, the rest Go's collector (35-45%:
the generators and frames made per call) and the work itself.

- **The pass** (`gen.go`'s `passDelegate`, `genDone`; `eval.go`'s `loop`, runFrames's loop split from its entry). A
  resumption with a value sent (`next`, `send`, a for loop's next item) of a generator suspended at its yield-from on a
  generator or coroutine hands the value to the delegate's resume itself: the frame is entered as run enters it (f_back,
  the depth, the C level, the thread's frame, gi_running, its exception state pushed, `f.PC` past its yield-from), and its
  loop, its instruction and its compiled function are not run. When the delegate yields, so does the frame from its
  yield-from (suspended there, f_back None), as opYieldFrom does. When the delegate returns or raises, or a tool's events
  came on while it ran, the frame goes on from its yield-from by its loop entered past its RESUME (it was resumed before its
  delegate ran), its yield-from finding the delegate's outcome in `genDone`: the value pushed, the error raised in the
  frame, the yield (the PY_YIELD event, the jump from a yield's event as before). Taken only when no tool has events, no
  eval breaker is pending, no throw is pending for the frame, and the depth and the C level are below their limits (resume's
  own path otherwise, with its errors). A chain of d levels runs one frame per item where it ran d. throw and close were
  already sent to the innermost delegate first (_gen_throw).
- **CPython's FOR_ITER_GEN and SEND_GEN themselves** (the generator's frame pushed in its caller's loop, its yield and
  return completing the caller's for-iter or yield-from in place) were built and measured, and left out: in pyi's loop a
  frame push returns from the caller's step and enters it again after the callee, which costs more than the Go-level
  resume it saves. On the loop (PYI_AOT_LOOP=1, 5 interleaved rounds, CPU medians): bm_generators 0.81 s with the pass alone
  and 0.87 s with both, bm_coroutines 1.89 and 1.95; the same push for a compiled caller's await (bm_coroutines compiled)
  1.63 against 1.75 (3 rounds). CPython's frames are pushed in one C loop; pyi's pushed frame leaves the caller's
  step and re-enters it, so the Go-level resume is the cheaper of the two here.
- **Two fixes on the way.** `yield from` over a class's iterator returned None for the iterator's `StopIteration(value)`:
  the class's tp_iternext slot ends the for loop's way, without the value; genSendTo now calls the class's `__next__` (as
  slot_tp_iternext does) and takes the value. PEP 479's RuntimeError carried an entry of the generator's own frame;
  CPython's (INTRINSIC_STOPITERATION_ERROR in the frame's handler, re-raised) carries none, its StopIteration cause does.

**Measured** (the VM, base 7baf829c0 against head, warm executables, interleaved B H / H B, 7 rounds, CPU (user+sys)
medians, load 30-37 with other tasks' runs beside; `.tmp/q330/log/ab-final.tsv`, `abloop-final.tsv`): compiled,
bm_generators 1.16 -> 0.83 s (-28%), bm_coroutines 1.89 -> 1.88, bm_nqueens 1.40 -> 1.39, bm_async_tree 1.90 -> 1.92; on
the loop (PYI_AOT_LOOP=1) bm_generators 1.26 -> 0.87 (-31%), bm_coroutines 1.99 -> 1.96, bm_nqueens 1.58 -> 1.62,
bm_async_tree 2.11 -> 2.06: the others' differences are within the load's noise. Every run's stdout the same at base and
head. bm_coroutines and bm_nqueens await or iterate fresh generators: their cost is the generator and frame made per call
and Go's collections of them (the pass does not apply to a first send), and 218's tracking of the coroutines a flush
finds running (`flushPendingGens`, 4% of bm_coroutines).

**Order unchanged** (the VM): two probes written for it (`.tmp/q330/t/p330.py`, `p330b.py`: sends and returns through a
4-deep chain, for loops and list() of chains, an exception caught at each level with the finally blocks' order, PEP 479
through a chain, close and throw into a suspended chain, gi_running, gi_yieldfrom and f_back seen from the leaf,
sys.exception() in nested generators resumed from handlers, a generator resumed through its own delegate, a reused
coroutine, await chains with sends and throws, the recursion limit in a chain, a class's iterator delegated to, f_lineno
of a suspended chain, a generator closed at its death, settrace switched on inside a delegate, break out of a for over
a generator, a generator iterating itself, tracebacks of generators' errors, async for) compiled and on the loop against
python3 -I: equal but at the base the class's iterator's value and PEP 479's tracebacks (3 lines), fixed, and one line
at both (the generator a `break` leaves is closed at its collection, not at the break: 297's moment does not cover a for
loop's iterator). Lib/test compiled (`bin/pyi check --aot --lib-test --fresh` test_generators test_coroutines
test_asyncgen test_contextlib test_sys_settrace test_sys_setprofile test_monitoring): 901 methods, 805 pass, 96 classed,
0 failing at base and head, every method's outcome and message the same. On goi (the eval level, `bin/pyi check --fresh`, the
same modules, stage 0 nf's, which interprets the checkout's runtime; nx's runs the runtime compiled into it): base 797
pass, 102 classed, 2 failing, head 798, 102, 1 failing; the one difference test_asyncgen's
test_async_gen_asyncio_shutdown_exception_02 (failing at base, passing at head: 297's base row, flaky), test_sys_setprofile's
ProfileHookTestCase.test_caught_exception failing on goi at both; every other method's outcome and message the same.


## As built: 329, the IR read at import: the binary IR (2026-11-17, the Mac VM, queue 329; 305's next step 2)

305 measured ReadIR, the printed IR's EDN reader and then its conversion, at 31% of hexiom's CPU, 11-12% of go and chaos
and 21% of an import probe: every import of a module compiled ahead parsed its package's IR text, and every source a
compiled program compiles at run time (the 41 of importlib's bootstrap, the codecs, argparse's lazy imports) parsed the
host's IR cache's text. Both now read a binary IR (`go/arbace/py/irbin.go`, PYI-SPEC.md §7.2): no EDN at import.

- **The layout**: `"\x02pyi-ir-1\n"`, a string table (each string once: names, operations, constants), then the code
  objects in the order `loadCode` reads them, counts and indices as varints, integers zigzag, a constant tagged as
  `loadConst`'s cases (a nested code object inline), the other IR values as generic tagged values; an instruction its
  operation's name (looked up once per string) and its ARG, decoded as `decodeArg` decodes it. The reader makes the same
  `*Code` as the text's reader, in the same order: the interned names, the parents, the creation events (queue 274).
- **Two writers, one layout**: `arbace.python.aot`'s `ir-binary` writes each compiled module's package `IR` as a
  `(byte-string ...)` (printable runs as strings); `irBinaryOf`, its port in Go, writes an IR cache answer's binary IR.
  Over the 1,751 modules of the shared package cache both writers give the same bytes, every one. A code object with a
  key `loadCode` does not read (`ir-code-keys`, `irCodeKeys`), a map constant of more than one key or any shape the
  writers do not mirror keeps the text; `PYI_AOT_BINARY_IR=0` keeps every text (packages and cache).
- **The IR cache's answers**: `irCacheRead` reads `KEY.b` beside the answer, the binary IR stamped with the runtime's key
  (`compiler.sh`'s new `# ir-bin:` line, the key `bin/pyi`'s aot_module builds the runtime by); the first compiled program
  of a runtime to read an answer's text writes it (a name of its own, then renamed). A runtime of another key rewrites it,
  so a change to the loader never reads another runtime's binary; a compiled program with no `# ir-bin:` line keeps none.
- **What a change to the loader must do**: a key the compiler adds keeps its modules' text until `loadCode`, `irbin.go`'s
  reader and both writers carry it (the guard); a change to how `loadCode` reads an existing key is a change to the three
  (load.go's comment). The package cache is keyed by `aot.ae`'s digest (the writer), the sidecars by the runtime's key.

**Proofs** (the VM):
- The 1,751 modules' IR texts of the shared package cache (`.tmp/repr-python/pyi/aot-shared/pkgs-e70dff6081aec2e1`, 140 MB):
  each read from its text, from `ir-binary`'s bytes and from `irBinaryOf`'s, the three trees of code objects equal field by
  field (names, instructions, handlers, lines, positions, constants by value, floats by their bits, interned names by
  identity, parents); a negative control (two modules' binaries swapped) differs. Reading: text 2.4 s, binary 0.26 s.
- `irbin_test.go` (`bin/pyi runtime vet`'s go test): a sample module's binary IR against its text, a control, another
  version refused, the key guard.
- The 145 programs of `test/python/pyi/` compiled (`bin/pyi aot`) and run as the runtime check runs a program (stdout,
  status, stderr's last line) at base and head: 145 of 145 the same bytes; 141 equal `python3 -I`'s, the other 4 fail at
  both by the direct run's harness (e-executable, e-subprocess, g-gil-blocking: no `sys.executable` outside `bin/pyi
  python`; f-thread: 120 s).
- On goi (`bin/pyi run`, nf's stage 0, the checkout's runtime interpreted: the text path, `isIR`, `loadIndexes`): 8 of 8
  sampled programs equal CPython's runs (arith, b-decorators, 6b-import, 6b-marshal, c-pickle0, c-datamodel, c-codecs,
  d-array).
- `bin/pyi runtime print` of the committed forms equal to the edited Go file for file; `runtime vet` ok; `table-check
  --configs tamago/amd64,tamago/arm64` ok (173 files); `make python-essential` rc 0 (21 programs: 20 pass, 1 classed).
- Every benchmark's stdout the same at base and head in every run, and each run's counts (code objects native and on the
  loop, the IR cache's answers) the same.

**Measured** (the VM, compiled, base 7baf829c0 against head, each executable's second run onward (warm: the IR cache and
its sidecars written by the compile's run), 10 interleaved rounds B H / H B, CPU (user + system) medians, load 15.6-34.3;
`.tmp/q329/ab/r2.tsv`):

| benchmark | base CPU ms | head CPU ms | change |
|---|---:|---:|---:|
| raytrace | 751 | 700 | -7% |
| deltablue | 503 | 460 | -9% |
| fannkuch | 893 | 841 | -6% |
| generators | 869 | 846 | -3% |
| nqueens | 789 | 742 | -6% |
| go | 348 | 288 | -17% |
| richards | 672 | 626 | -7% |
| chaos | 308 | 257 | -17% |
| json_dumps | 315 | 266 | -16% |
| spectral_norm | 843 | 757 | -10% |
| unpack_sequence | 786 | 753 | -4% |
| coroutines | 1073 | 1000 | -7% |
| float | 605 | 561 | -7% |
| nbody | 460 | 417 | -9% |
| hexiom | 179 | 114 | -36% |
| base64 | 3491 | 3493 | +0% |
| bpe_tokeniser | 7850 | 7844 | +0% |
| async_tree | 1405 | 1248 | -11% |
| mdp | 4330 | 4224 | -2% |
| pprint | 5673 | 5679 | +0% |
| the import probe | 381 | 292 | -23% |
| the 21 summed | 32526 | 31409 | -3% |

The packages' IR alone (head's first commit, 5 rounds, load 23-36): hexiom -15% of its -36%, go and chaos -8% of their
-17%, the import probe -22% of its -23% (its modules are compiled ahead); the rest is the 41 sources' answers read as
binary. Go's profile (12 merged runs each): ReadIR 23.9% of hexiom's CPU -> 5.7% (the EDN reader 21.9% -> not in the
profile), the import probe 21.3% -> 4.7%. What is left of a short program's start is 305's finding 2: the 41 sources'
code run on the loop, and the collections.

**The packages' size and build**: the printed `unit.go` of the 166 modules both caches hold 45.4 MB -> 44.1 MB (-3%);
the executables 0.8-2.9% smaller (the import probe's 237.7 MB -> 230.9 MB). Go's compile of hexiom's 28 module packages
(rebuilt alone, `-p 2`): 26.2 s -> 26.5 s of CPU, peak 440 -> 436 MiB, the same. The writer adds about 50 ms per module to
`arbace.python.aot program` (the 1,751 modules in 77-90 s on stage 0). The binary is 40-45% of the text's bytes (62 MB for
the 140 MB).

## As built: 328, the next deferred classes (round 5): itertools, float(), the sequences' repeat, the runtime's panics, the core objects' and built-ins' checks, -W (2026-11-17, the Mac VM, queue 328)

Base python-1117-on-next 7baf829c0 (317's merge onto next with 318-320). The deferred classes of divergences.edn with the most
rows that pyi code can close, after 304's round: `:pyi-command-line` 40, `:ordered-dict-c` 31 (odictobject.c's port: not cheap),
`:pyi-main-module` 28 (runpy's main: not cheap), `:core-objects-287` 27, `:builtin-functions-core` 18, `:itertools-core` 12,
`:float-core` 12, `:runtime-panics` 12, `:sequence-index-ops` 10. Taken here, by root cause in go/arbace/py (and one in
arbace/python/compile.ae, one in bin/pyi):

- **the runtime's panics** (`builtins_collections.ae`, `set.ae`, `eval.ae`, `aot.ae`, compile.ae): deque.count and deque.remove
  read the items by index and check the deque's state after each comparison (deque_count's RuntimeError, deque_remove's
  IndexError "deque mutated during iteration", its "deque.remove(x): x not in deque"; a comparison that cleared or popped was a
  nil and a slice bound); SET_ADD's target an exact set (gh-154902: a rebound `__conditional_annotations__` is a TypeError
  "'T' object is not a set", it was a type assertion's panic; the loop's and the compiled code's one-line change); a
  class-visible scope's `__classdict__` free variable even when the class binds that name (`__classdict__: int`: CPython's
  codegen adds it to u_freevars as it emits LOAD_DEREF; the annotate function's closure index was nil).
- **itertools** (`builtins_itertools.ae`, `builtins_iter.ae`): count's PyNumber_Check of start and step and count_repr (the
  type's name, the step left out when it is an int equal to 1); repeat_repr and `__length_hint__` (TypeError "len() of unsized
  object" when unbounded, so operator.length_hint's default); accumulate's `initial` and batched's `strict` keyword-only;
  pairwise_next's old item read before the next is asked (re-entrancy); groupby_next and _grouper_next as C's (the current value
  taken by the grouper that returns it, the target key compared first: gh-146613); tee_fromiterable (a tee of a tee is a copy,
  gh-123884's independent objects) and `_tee(iterable)`; the no-keyword types (chain, cycle, islice, pairwise, starmap,
  dropwhile, takewhile, filterfalse, and builtins' filter) take keywords in a subclass with its own `__init__`
  (`type->tp_init == X.tp_init`); an exhausted iterator drops its closure (its source).
- **float()** (`float.ae`, `floatextra.ae`, `builtins.ae`, `format.ae`, `percent.ae`): PyFloat_FromString (a str's Unicode decimal
  digits and spaces as ASCII, `_PyUnicode_TransformDecimalAndSpaceToASCII`; ASCII whitespace stripped; the error the object's
  repr); float_new_impl's PyNumber_Float (an exact str parsed, then `__float__` (a float subclass's too; a strict subclass
  returned warned of, DeprecationWarning), `__index__`, a float subclass's value); keywords for a subclass with its own
  `__init__`; float.fromhex's one sign and no underscore; round()'s ndigits clamped as PyNumber_AsSsize_t(NULL); format's 3.14
  fraction grouping (`'._f'`, `'.10_f'`, `'021_._e'`; "Cannot specify ... with 'n'.") and the omitted type with a precision
  (Py_DTSF_ADD_DOT_0's exponent a digit earlier: `format(1234.56, '.4')` is '1.235e+03'); `'%f' % str` a TypeError.
- **the sequences' repeat and strip** (`str.ae`, `bytes.ae`, `bytesmethods.ae`, `strmethods.ae`): str, bytes and bytearray
  times an object with `__index__` (and their `__mul__`/`__rmul__` wrappers); an exact str or bytes stripped of nothing is itself.
- **ints, slices, ranges** (`builtins.ae`, `intbytes.ae`, `slice.ae`, `range.ae`): int.__round__ at any size (long_round's
  _PyLong_DivmodNear); to_bytes' signed range [-2**(8n-1), 2**(8n-1)); from_bytes' PyObject_Bytes (an int or a str a TypeError,
  `__bytes__`); slice.indices and a range's slice by _PySlice_GetLongIndices (past sys.maxsize).
- **the core objects and the built-ins' checks** (`class.ae`, `containers.ae`, `descr.ae`, `builtins_exec.ae`, `memoryview.ae`,
  `func.ae`, `funcattrs.ae`): object_isinstance's `__class__` error propagated; list += a heap type's `__radd__` first
  (binary_iop1 before sq_inplace_concat: list += UserList is a UserList); type()'s name and a str `__doc__` as UTF-8 (a lone
  surrogate a UnicodeEncodeError) and the name without NUL; `__type_params__` not deletable; a dict subclass namespace with its
  own `__iter__` (OrderedDict) copied in its keys() order; object.__dir__ without `__class__` merges no class; format() of a str
  subclass's spec; compile()'s unrecognised flags and optimize value; exec()'s `closure=`; exec's frame builtins the globals'
  `__builtins__` (a module its dict); bytes' and bytearray's `__buffer__` (collections.abc.Buffer); a function's `__name__` and
  `__qualname__` objects kept (functools.wraps' identity).
- **-W, -b and PYTHONWARNINGS** (`sys.ae`, bin/pyi python): sys.warnoptions as config_init_warnoptions orders it ("default" in
  development mode, PYTHONWARNINGS's options unless -E/-I, -W's, -b's "default::BytesWarning" or -bb's "error::BytesWarning"),
  sys.flags.bytes_warning, and the warnings module imported at start when there is an option (its _processoptions; an invalid
  -W reported). bin/pyi python passes -W and -b (PYI_WARNOPTIONS, PYI_BYTES_WARNING) where it dropped them.

Proof (the VM, run): `bin/pyi check --only` and `bin/pyi check --aot --lib-test --only` over the 108 rows these classes named
(25 modules) and the controls' subjects. Compiled: base 7baf829c0 15 of the 108 pass (149 methods: 56 pass, 93 classed, 0
failing), head 80 (121 pass, 28 classed, 0 failing), no row passing at the base lost. Interpreted (goi, under 201's stage 0
`nf`, whose pyi runtime is the checkout's: next's stage 0 embeds next's), head 79 of the 108 (120 pass, 29 classed, 0 failing):
the levels agree but for ShutdownTest's test_cleanup (compiled only). The classes edited by both levels' passes (identical edits
in divergences-315.edn): `:sequence-index-ops` (10), `:float-core` (12), `:runtime-panics` (12) and `:itertools-core` (12;
test_islice to `:finalization-collect`) retired; `:core-objects-287` 27 to 13, `:builtin-functions-core` 18 to 9,
`:pyi-command-line` 40 to 26 (3 to `:warnings-c`). 15 of the rows served passed at the base already (test_clinic's 6, test_float's
4, test_pprint's, test_builtin's top-level await, test_enum's getnewargs_ex, test_reprlib's descriptors): fixed by earlier
queues, never re-recorded.

## As built: 339, a generator a for loop leaves closed at the loop's end (2026-11-17, the Mac VM, queue 339; 330's DECISION 1 (a), the user's)

297 closed a fresh generator at the return of a consumer that keeps nothing (`next(gen())`, `any(...)`); a generator a for
loop leaves by `break`, by `return` or by an exception was still closed at its Go collection, after the code that follows
the loop, where CPython closes it as the loop drops it (its POP_TOP at a break or a return, the exception's unwinding, the
frame's clear: reference counting).

- **Held** (`finalize.go`'s `holdLoopGen`, `loopNext`; the generator type's iter slot). The get-iter of a generator not yet
  run marks it held by its for loop when 297's stamp says the frame's previous instruction made it with no call since
  (stampGen: the making frame, its instruction, callSeq), it was never handed out (`exposed`), and the next instruction is
  the loop's for-iter, or an inlined comprehension's start that reaches its for-iter with only its own slots' moves between
  (load-fast-and-clear, load-cell-and-clear, swap, an empty build). Such a generator was held by the value stack alone and
  then by the loop's iterator slot, which no Python code reaches (not f_locals; a frame object's f_generator marks it
  exposed). `iter(gen())`, `g = gen(); for x in g`, a walrus and a generator expression's argument are not marked.
- **Closed** (`dropLoopGen`, `dropLoopGens`, `aotDropHeld`): when the loop's frame pops it (step's pop, the compiled code's
  AotDrop), when a handler's unwinding pops it (the top first), and when an exception leaves the frame (its stack from the
  bottom, as `_PyFrame_ClearLocals`, after PY_UNWIND): 297's dropGen (close, an error to sys.unraisablehook, the locals with
  a finalizer due at 222's moment). A loop that runs the generator to its end pops it finished (`unhold` at its end).
  Nothing is checked until a loop holds one (`loopHeldN`), then one test per pop.
- **The compiled code** (`arbace/python/aot.ae`): a for-iter starts a block, so a loop's iterator is in the frame's stack
  for the whole loop, also in a loop with no back edge (`for b in g(): return a, b`), where the register generator kept it
  in a Go local that a pop dropped unseen; the pops of it are then AotDrop's (a return's swap takes it into the locals
  only to pop it). The generator's digest changes: the AOT packages are compiled afresh once.
- **Not covered**: a generator a for loop iterates through `iter(...)` or a name; a generator expression's own iterator
  (its frame's `.0`) when the expression is closed; a loop's generator found by gc.get_referrers or gc.get_objects (marked
  all the same: closed at the loop's end, where CPython keeps it while the referrer holds it).

**Proved** (the VM; the base is 330's head 30efa1e7d, whose Python runtime is 593c97d03's: `git diff --stat 30efa1e7d
593c97d03 -- go/arbace/py arbace/python` names only run.ae's require-runtime!). Three probes (`.tmp/q330/t/p339.py`,
`p339c.py`, 330's `p330b.py`: break, return, a return from a with block, exceptions caught in the frame and leaving it,
nested loops left by return, named and walrus-held generators kept, exhaustion, an outer generator closed or dropped while
suspended in its loop, continue, a comprehension left by an exception, a close whose finally raises, settrace's events of
the close, a generator method, f_back exposed, 297's next()/any() and held cases beside, f_generator exposed and kept)
compiled and on the loop: equal to python3 -I, 21, 11 and 16 lines (the base: 32, 16 and 2 lines late). Lib/test compiled
(`check --aot --lib-test --fresh`, test_generators test_coroutines test_asyncgen test_contextlib test_sys_settrace
test_sys_setprofile test_monitoring): 901 methods, 805 pass, 96 classed, 0 failing, each method's outcome and message the
base's; on goi (nf's stage 0, the checkout's runtime interpreted) test_generators and test_sys_settrace: 507 methods, 423
pass, 84 classed, 0 failing, each the same as at 330's head. The generator programs of the synthetic corpus (13, `check
--aot --no-pyi`): 13 pass compiled and on the loop by their golden text; `make python-essential`: 20 pass, 1 classed, 0
failing.

## As built: 353, the AOT record judged again (2026-11-18, the Mac VM, queue 353; queue 347's DECISIONs 1 (a) and 2 (a), the user's)

- **`bin/pyi check --aot --judge [DIR] [--record]`** (347's DECISION 2 (a)): a kept run's `DIR/runs.tsv` (default the last
  `check --aot`'s, `.tmp/repr-python/pyi/aot/check`) judged again by `arbace.python.aot judge`, nothing compiled or run, as
  `bin/pyi check --judge RUN` does for Lib/test: the morning after a class of `divergences-aot.edn` changed. `check --aot`'s
  own last step is the same function (`aot_judge`). Refused: `--only`, `--only-list` and `--no-pyi` with `--judge` (the run's
  own programs are judged), a DIR without runs, and `--record` of a run that lacks programs of `aot_list` into the committed
  record (`PYI_CHECK_AOT_RECORD` names a scratch record).
- **The two records together** (347's DECISION 1 (a), procedure): `bin/pyi records`' header and PY11-PREP.md §6 say it: the
  AOT record is never committed without the Lib/test record (alone, it drops the eval cell's loop sources' `:until` and reap
  counts the eval cell checked by the loop's 151 programs, about 99 against Lib/test's about 68).
- **The unit check**: `test/python/aot_judge_test.ae` (21 checks) writes fabricated runs and references as files and runs the
  judge's command over them (`cmd-judge`, as bin/pyi calls it): its results and record hold the judge's rows, exit 1 while a
  row fails, a differing program classed by its id exits 0, a missing reference stays `:failed` whatever class names it.
- **Found**: `control`'s pair rule counts a golden reference's `:stderr-last :any` as another reference than a program's of the
  same stdout with stderr "", so such a pair's crossing passes and the judge exits 1 (the unit check leaves that program out of
  its re-judge). The corpus has no such pair: the duplicate stdouts are cmp-eq/cmp-is, cmp-ne/cmp-is-not and matmul/aug-matmul,
  and the first passing program (synth/builtins/bool.py) has a stdout of its own. Not changed: aot.ae is the AOT generator's
  digest (`aot_gen`), so any edit reprints every compiled package and the runner.

## As built: 350 and 366, pyi's slotdocs and ast tables made by Arbace (2026-11-18, the Mac VM, queues 350 and 366)

**350** (audit 344's DECISION 1 (a), the cheapest two of the tables CPython made): `tools/pyi/runtime_tables.ae slotdocs` (Objects/
typeobject.c's slotdefs through repr/c's preprocessor, add_operators' rule, the last entry per name: 79 entries; the recorded 80th,
`_value_repr_`, the python3 walk's artifact, dropped) and `runtime_tables.ae ast` (Parser/Python.asdl by ASDL's grammar:
builtins_ast_table.go byte for byte), both in `bin/pyi runtime gen-tables` with a negative control each. Python's eval and AOT facts
stay 0.5 for the rest (350's report, its plan and DECISION 1).

**366** (the host's review of 348-350):
- **The ast table's header** names its generator, `tools/pyi/runtime_tables.ae (ast)`, and its format's comment points there (the format
  stated in runtime_tables.ae). `go/arbace/py/builtins_ast_table.ae` moves once, its two comments alone (astTable's constant byte for
  byte), re-read from the generated Go by `bin/repr-go read --full` in a one-file module (control: the committed printed Go read the
  same way gives the committed forms byte for byte). `tools/pyi/asdl_table.py`, the oracle's, by hand: its output now differs from the
  committed file in the header line alone. Run: `bin/pyi runtime gen-tables ast slotdocs`, both byte for byte, both controls differ.
- **The keys these files move.** Since W2 (doc/W-WINDOW.md) `go/arbace/py` is copied into the program and listed in goi's table, out of
  `bin/lib/version.bash`'s `arbace_crop_paths`, so `go/arbace/py/slotdocs.ae` (350) and `go/arbace/py/builtins_ast_table.ae` (366)
  are in `gate_key` (bin/arbace-go) and the javac key: each moves the gate's builds, every step on `:gate` and javac's tool;
  `arbace_runtime_key` does not move (`arbace_runtime_not` keeps `go/arbace/py` out). Checked at 9cd2e4a96 and at 366's head with
  each tree's key functions (doc/scheme/SCHEME-SURVEY.md, "As built: 350 and 366", for the numbers: gate_key's per-file digests
  differ in exactly 350's four files and this table).
- **Round 2 (the user's choice of the lows, 2026-11-18): the five tables of queue 265** (opcode, errno, fcntl, socket, posix) name their
  generator the same way, `tools/pyi/runtime_tables.ae (TABLE)`; each `go/arbace/py/builtins_TABLE_table.ae` moves once, its header
  comment alone (the `;;` line and the `:go/comments` entry), re-read from the generated Go in a one-file module (fcntl, socket and posix
  for linux/amd64, their build line), each control (the committed printed Go read the same way) byte for byte the committed forms.
  `bin/pyi runtime tables`, the oracle tier's comparison with the python3 generators, respells the python3 header to the Arbace one
  before comparing (the rest byte for byte). Run: `bin/pyi runtime gen-tables`, the seven tables byte for byte and the seven controls
  differing (97 s); the five python3 generators run on the VM's python3 3.14.10 through that pipeline equal to the new files.

## As built: 375, one spelling for every generated table (2026-11-18, the Mac VM, queue 375; 357 merged with 350 and 366)

- **The merge**: `mac/350-indep-facts` (5caeb3bbd) into 357's head (1e1d87576). The one conflict was opcode's header line in
  `tools/pyi/runtime_tables.ae`; the resolution keeps both, 350's generator name and 357's `--suffix`. No file of `go/arbace/py` changed
  on both sides; `bin/pyi` merged on its own.
- **One spelling**: every table `go/arbace/py` holds from `runtime_tables.ae` says "Code generated by tools/pyi/runtime_tables.ae
  (TABLE[ --suffix S]) from ...". 357's bytecode, bytecode315, opcode315, cjk, unicodedata, unicodedata315 and textsig named their
  python3 generators, and stat's header had no parentheses. Seven committed forms move once, their header comment alone:
  `bytecode_table{,315}.ae`, `builtins_opcode_table315.ae`, `builtins_cjk_table.ae`, `builtins_unicodedata_table{,315}.ae` and
  `builtins_textsig_table.ae`. Each was re-read by `bin/repr-go read --full` from the generated Go in a one-file module (the Go-forms
  method, no hand edit of the forms). Control: the committed printed Go, read the same way, gives the committed forms byte for byte,
  all seven. stat's file is not committed (its constants are compared with builtins_stat.go's statConsts). The "see ... for the format"
  lines of cjk and unicodedata still point to the python3 scripts, which document the packed records' format.
- **The oracle tier**: `bin/pyi runtime tables` respells the printed tables' headers back to the python3 generators' (`py_spelling`:
  `TABLE_table.py[ --suffix S]`, cjk's `cjkcodecs_table.py`, ast's `asdl_table.py`, unicodedata_table.py without a suffix) before every
  comparison, the rest byte for byte. This replaces 366's respelling of the five tables' python3 output.
- **Run**: `bin/pyi runtime gen-tables --all`, the fifteen tables Arbace's byte for byte (stat by its constants) and the fifteen
  controls differing (one run, about 17 minutes on the VM's load); before the re-read, the seven DIFFERED in their header line alone.
  `bin/pyi runtime tables` (the python3 tier, the VM's python3 3.14.10) on the new spelling: every table equal to its python3
  generator's and both 3.15 controls failing as they must, rc 0; builtins_opcode_table315.go not judged (no 3.15.2 oracle on the VM).

## As built: 410, the records night prepared: the rows in no class predicted, written ahead (2026-11-19, the Mac VM, queue 410)

347 extrapolated about 190 methods in no class for the first whole Lib/test record from its re-judge of 321's slice (15 of
3,713 methods, 27 of 529 modules). Read again, 11 of the 15 are the slice's own and will not recur on the night's host:
- **test_set's 9** (test_issue_37219 x7, test_unhashable_element x2): the slice ran next's runtime compiled (stage 0 of seed
  3c48859d, `PYI_RUNTIME_STALE=1`); queue 304 fixed both in the checkout's runtime, which the gate's build compiles in. Probed:
  the nine methods under `bin/pyi run` of a unittest script on the checkout's runtime (interpreted by goi): 9 ok.
- **2 `crash Terminated`** (test_capi's test_adjustindices, test_modulefinder's test_extended_opargs): the VM's watchdog at
  6 GB; the night's runner caps each driver at 8,192 MB (`:host-memory`'s line).

The other 4 are test_multiprocessing_spawn.test_manager's: three Barrier tests broken by late spawned parties (pyi starts) and
test_release_task_refs. 287's whole compiled run (the compiled launcher's children) classed or passed every other module's
methods with the classes as they then were; the classes narrowed since (304 names taken out of :tests and 3 :match patterns dropped between 287's merge
514b2df88 and this base, by queues 289-328) took out only methods those queues measured to pass. So the night's rows in no class come from what neither run saw: pyi's own children (sys.executable
on the gate's executable, slower to start than the compiled launcher, faster than goi), amd64's load at 4 jobs, and any runtime
change since 287 not run whole. Estimated 10 to 50, most in the child-heavy modules no pyi-children run judged
(test_multiprocessing_spawn's processes, threads and misc, test_concurrent_futures' process pools).

**Written ahead** (test/python/divergences.edn and divergences-315.edn, the same edit, before `:skipped-by-test`; each
`:decided` says "write-ahead"):
- `:spawn-children-timing` (deferred, 6 names): the slice's three WithManagerTestBarrier rows (test_reset, test_timeout,
  test_wait_return) and, predicted, WithProcessesTestBarrier's same three.
- `:finalization-collect-spawn` (deferred, 5 names): the slice's WithManagerTestPool.test_release_task_refs and, predicted
  from the start-method grid of divergences.edn's names (night 12's fork and forkserver test_threads in `:finalization-collect`,
  286's spawn as_completed row), spawn's test_threads test_release_task_refs, test_resource_warning, test_lose_target_ref and
  test_process_pool's ProcessPoolSpawnProcessPoolExecutorTest.test_free_reference (the least sure: 287 passed it compiled).
- `:host-signal-kill` (deferred, `:match` `^crash (Killed|Terminated)(\s|$)`): a driver killed from outside the runner.

A write-ahead name the record passes is reported stale by `bin/pyi divergences` (no failure); the morning prunes those and
moves the confirmed ones into their lasting classes (PY11-PREP.md §6, "The records night, step by step").
- **Run**: `bin/pyi divergences --results` of 347's re-judged slice: 15 in no class -> 9 (the test_set rows above: the
  slice's stale runtime), the new classes 3, 1 and 2; night 12's partial run (amd64, goi, 181 modules) re-judged: 408 in no
  class -> 403, no new shadowed name. `bin/reap` (0 problems), `bin/reap --self-test`, `bin/records-check` (9 pairs, 0
  problems), `bin/crops-score --judge --strict` (rc 0), `python.aot-judge-test` (rc 0), `make check-makefile`.
- **The AOT record** needs nothing: del.py is classed either way (`:as-pyi`, divergences-synth.edn's `:finalization`, once pyi
  on goi runs the compiled runtime; else divergences-aot.edn's `:finalization-goi`), the other 150 passed in 279's whole run and
  347's sample.

## As built: 489, lex-check judged by CPython's C tokenizer on Arbace's C (2026-11-20, the Mac VM, queue 489; py-own-judge-1 (a), the user's)

py-own-judge-1120 found Lib/tokenize.py on pyi no judge of the native lexer: since 3.12 it tokenizes through
`_tokenize.TokenizerIter`, which on pyi is the native lexer itself. Its option (a), decided by the user: CPython's own C
tokenizer, built from the pinned tarball by Arbace's C, as lex-check's reference at check time.

- **The program.** `tools/pyi/clib/pytokenizer/` (lib.conf, `tokharness.c`): CPython 3.14.10's `Parser/lexer/` (lexer.c, state.c,
  buffer.c), `Parser/tokenizer/helpers.c` and `readline_tokenizer.c`, `Parser/token.c`, `Python/Python-tokenize.c` (the `_tokenize`
  module, the iterator's rules: extra tokens, positions in characters, the errors' arguments), `Objects/unicodectype.c` (the XID
  tables of the identifier scan) and `Python/pyctype.c`, unmodified, compiled in `Py_BUILD_CORE_MODULE`'s mode as a release build
  (`NDEBUG`) against the tarball's `Include/` with Arbace's `pyconfig.h`. `bin/pyi-clib build pytokenizer` builds them with the
  harness by ci's builder into one node program, committed as `go/arbace/clib/pytokenizer` (a crop path: no key moves).
- **The harness** (`tokharness.c`, Arbace's own): the C API the sources call, str and bytes in CPython's own layouts (the
  headers' inline accessors read them in place), UTF-8's decoder with "replace" as CPython's (one U+FFFD a maximal subpart),
  `PyUnicode_FromFormat`'s formats, the exception types and SyntaxError's arguments as `SyntaxError.__init__` takes them and
  `PyErr_SyntaxLocationObject` sets them, the module and type machinery `Python-tokenize.c`'s module needs (its exec slot makes
  the type; the iterator is reached through its slots), `Py_BuildValue`'s formats; and `tokenize.detect_encoding` ported (the BOM,
  PEP 263's cookie, `_get_normal_name`, `check`), the codecs looked up in the tarball's `Lib/encodings` as `encodings.search_function`
  does (aliases.py, the module's file), decoders for utf-8, latin-1, ascii and every charmap codec (its `decoding_table` read from the
  module's source); another codec is the harness's refusal, a `!HelperError` line. Copied from CPython (PSF): `Parser/pegen.c`'s
  three byte-offset conversions, `Objects/unicodeobject.c`'s `_PyUnicode_ScanIdentifier` and `_Py_ascii_whitespace`. It writes
  `tools/pydump/tokdump.py`'s format, the same lines.
- **The executable.** `bin/pyi-clib exe pytokenizer`: the committed node program translated whole by cf2g into a main package,
  built by TamaGo's go with ci's back end (`bin/ci cirun-build`'s printed `go/arbace/{ci,crt}`) into the main checkout's
  `.tmp/pyi-clib/pytokenizer/exe/`, kept by the Nodes' digest and the back end's key (VM: cf2g 100 s, go build 580 s at 2.4 GiB,
  once a key). Interpreted by cirun it is 12 times slower (82 s for 18 stdlib files, 6.9 s compiled).
- **The mode.** `bin/repr-python lex-check`'s reference: `REPR_PYTHON_LEX_REF=own`, records mode's default for 3.14 (the harness over
  the files, `own_tokdump`); `recorded`, and the live, record and verify modes, run or read tokdump.py's dumps, the oracle tier's;
  3.15 keeps its oracle. The gate fragment's `python-read-essential` and `python-native` declare the harness's inputs
  (`:pytokenizer`) and the `:go` host.

## As built: 494, parse-check judged by CPython's parser on Arbace's C (2026-11-20, the Mac VM, queue 494; py-own-judge-1 (a), the user's)

489-3 (a): parse-check's reference made as lex-check's (As built: 489), CPython 3.14.10's own parser built from the pinned
tarball by Arbace's C, at check time; CPython's recorded trees moved to the oracle tier.

- **The program.** `tools/pyi/clib/pyparser/` (lib.conf, `parseharness.c`): `Parser/parser.c`, `pegen.c`, `pegen_errors.c`,
  `action_helpers.c`, `string_parser.c` and `peg_api.c`, `Python/Python-ast.c` (the constructors; its object conversion and types
  never run), `asdl.c`, `pyarena.c`, `future.c` and `ast_preprocess.c` (what `compile()`'s `PyCF_ONLY_AST` path runs after the
  parser: `_PyFuture_FromAST`'s "not a chance" and undefined features, the preprocess without folding), `mystrtoul.c`, and 489's
  tokenizer with `Parser/tokenizer/string_tokenizer.c` and `utf8_tokenizer.c` (ast.parse reads from a string), unmodified, as 489's.
  `bin/pyi-clib build pyparser`: the node program `go/arbace/clib/pyparser` (key a94b0f88, 9.2 MB; VM 21 min at 2.8 GiB);
  `bin/pyi-clib exe pyparser` (cf2g and the go build, 14 min at 4.1 GiB on two CPUs).
- **The harness** (`parseharness.c`, Arbace's own): 489's object model widened: the exceptions as instances in
  `tstate->current_exception` (pegen.c reads it; CPython's `PySyntaxErrorObject` first in the layout, whose `metadata` pegen.c sets
  in place), SyntaxError's attributes from its arguments and the syntax-location setters (the program text of a file named by the
  corpus's relative path is none, as in CPython's working directory), UnicodeDecodeError's attributes and `str()` (the parser's
  "(unicode error) ..."), UTF-8's reasons, the numbers' boxes with longobject.c's 4,300-digit limit, `PyUnicodeWriter`, `%R`.
  Copied from CPython (PSF): `_PyUnicode_DecodeUnicodeEscapeInternal2`, `_PyBytes_DecodeEscape2`, `_PyLong_DigitValue`,
  `_PyAST_GetDocString`, `_Py_CArray_*`, `PyOS_mystrnicmp`, and `Modules/unicodedata.c`'s NFKC and name lookup (the identifiers'
  normalization, `\N{...}`), whose tables the harness reads at start from the tarball's `Modules/unicodedata_db.h` and
  `unicodename_db.h`: compiled in, their initializers made a 33 MB node program whose cf2g translation passed 4 GiB. Then
  pydump.py's `tree_one` (`ast.parse`, again with `type_comments=True`, `detect_encoding`, the decoded text) and its `Writer`
  ported to C over the parser's own tree (`mod_ty`), mode `:full`'s `:tokens` from `_tokenize.TokenizerIter` as tokenize.tokenize
  runs it: pydump.py tree's (and tree --full's) dumps, byte for byte.
- **The mode.** `bin/repr-python parse-check`'s reference: `REPR_PYTHON_PARSE_REF=own`, records mode's default for 3.14
  (`own_treedump`, `--full` with `--full`); `recorded`, live, record and verify read or run pydump.py, the oracle tier's. The gate
  fragment's `python-read-essential` and `python-native` add the inputs `:pyparser`.
- **Proved** (one whole run in each mode, own): the harness's dumps equal CPython's recorded ones (the pydump manifests' SHA-256) on
  3,736 of 3,736 files in `:lines` and in `:full` (samples, stdlib, site-packages, lib-test, lex-errors, syntax-errors), the 3,846
  mutants of each mode equal CPython 3.14.10 live; a planted native-parser bug (parser.ae's conjunction an Or) fails control.py
  and fnmatch.py in own mode. independence.edn: parse-check's 5,724 cases and the 256 syntax-errors cases without a stated error 1.

## As built: py-capi-1122, the C API natives test_capi's modules reach (2026-11-22, the burst's py-capi stream)

The deferred `:capi-port` (the C API's natives not provided yet) on P1's py-eval-1122 head (9a2b7842a, its executable
b2a5b50d4819; `go/arbace/pyctypes` is a lazy package, interpreted from the checkout, so no build). Before-run: `bin/pyi check
--only test_capi` (`PYI_RUNNER=python3`), test_capi 746 of 1,328 pass, `:capi-port` 169, failing 25 (the six "fatal error: out of
memory" crashes are the run's `ulimit -v 8 GiB`, passes in the record; test_number's three DeprecationWarnings fail only in the
module's whole run, as do test_run's ten, P1's natives: not this stream's). Proof: `bin/pyi check --only` of those 188 methods
(the crashes left out), the three controls failing as they must: 102 of the 169 pass, no new failure but test_misc's
Test_testcapi.test_weakref_capi (a segmentation fault where the native was missing: classed by name).

- **Mapping and dict** (capi_more): the string-keyed forms decode the key as PyUnicode_FromString (its UnicodeDecodeError
  propagates) and a NULL is null_error's SystemError; PyMapping_HasKey and the rest take only KeyError for absent, the unraisable
  forms by their own messages; the dict natives take a subclass's instance (PyDict_Check); PyDict_Update and Merge read a dict
  whose `__iter__` is dict's in place (dict_merge's fast path: a subclass's `__getitem__` not called); PyDict_GetItem(String)
  report a hash or decode error as unraisable, the hash's own message.
- **Objects and bytes**: PyObject_Repr and Str give `__repr__`/`__str__`'s result as it is (a str subclass's instance kept),
  ASCII the repr itself when it is ASCII; PyObject_Bytes takes `__bytes__` (a bytes subclass kept, anything else a TypeError),
  PyBytes_FromObject refuses an int and a str; PyBytes_Concat(AndDel) over two C-contiguous buffers (NULL w clears), PyBytes_Repr
  with smartquotes; PyByteArray_Concat over buffers, its FromStringAndSize and Resize past 2**40 a MemoryError.
- **Unicode**: ensure_unicode's TypeError for split, partition, replace, splitlines, count, find, findchar, tailmatch, contains
  and translate; count, find, findchar and tailmatch over the code points with ADJUST_INDICES (pyi's str.count counts a lone
  surrogate of a pair twice: the natives do not rely on it); GetLength and ReadChar's PyErr_BadArgument and ReadChar's 32-bit
  `(Py_UCS4)-1`; EqualToUTF8(AndSize) compares the strict UTF-8 only when the sizes agree; FromWideChar, AsWideChar,
  AsWideCharString; WriteChar on a str C holds alone; FromObject copies a subclass's instance; the AsXString encoders'
  PyErr_BadArgument, FromEncodedObject over a buffer.
- **Imports, longs, frames**: PyImport_GetMagicNumber is pyi's own importlib's MAGIC_NUMBER; AddModuleObject takes any hashable
  name (an unhashable one's TypeError); PyImport_Import is `__import__` then sys.modules; ImportModuleLevel(Object)'s level and
  globals checks; ImportFrozenModule(Object) over `_imp`; PyLong_Export, FreeExport and PyLongWriter (30-bit digits); PyLong_AsVoidPtr
  refuses `__index__` and gives NULL on an error, FromVoidPtr keeps the handle it names; PyFrame_New.
- **New natives** (capi_tail.go, registered last): complex (AsCComplex and Real/ImagAsDouble through `__complex__` with its
  DeprecationWarning, `_Py_c_sum/diff/neg/prod/quot/pow/abs` with C11 Annex G's recovery and the world's errno), PyObject_Print
  (fwrite into the FILE, musl's F_ERR flag read and cleared), PyStructSequence_NewType (a tuple subclass with its fields as
  properties), Py_Is, PyGen_GetCode, PyCode_GetCode/Varnames/Cellvars/Freevars, PyMem_Raw*, PyObject_Calloc/Realloc,
  PyBuffer_FromContiguous, PyThread_tss_* (one thread), PyThread_get_thread_native_id and _ident, PyUnstable_IsImmortal,
  Py_FrozenMain (exported, reports no frozen program), PyWeakref_GetObject; PyConfig_Set (capi_config) for the sys attributes,
  its type checks and read-only refusals.
- **Py_BuildValue** walks the whole format on an error (every argument consumed, each N's reference stolen, the first error
  given), O& calls the converter, D reads a Py_complex, C checks the range, a dict of an odd count is "Bad dict format".
- **Mirrors**: a list's sync from C keeps Go's value when C's slots are as last synchronized (Python code a native ran may have
  resized it: gh-72054's `__index__`), so the deprecated PySlice_GetIndicesEx sees the shortened list.

Left in `:capi-port` (67 of test_capi's, and the other modules' rows): PyConfig_Set of sys.flags' members and int_max_str_digits
(a new sys.flags object: pyi's flags type cannot be instantiated from a native) and of cpu_count (start-time); the weak reference
layout (PyWeakref_GET_OBJECT); the exception state natives (PyErr_SetExcInfo, SetHandledException) and PrepReraiseStar; the
thread natives (PyThread_allocate_lock and C threads); the bytes and str singletons a C result is compared with by `is`; the
invalid-string checks of release builds; test_getargs' str subclass keywords; the macro PySlice_GetIndicesEx's Py_SIZE read
after Python code ran in PySlice_Unpack.

### py-capi-1122, round 2: extension modules through the import system, and more of test_capi's natives

The user's decision of 2026-11-22: C extension modules are built by ci into pyi's C world and loaded through the import
system (`:extension-modules`, 75 methods in the records night's record, by design before).

- **The import system**: `_imp.extension_suffixes()` is `['.arbace-314.so']` (PEP 3149's `.CACHE_TAG.so` with pyi's cache tag
  `arbace-314`, so no other implementation's extension file is taken for one of the world's); `_imp.create_dynamic` and
  `exec_dynamic` are the C world's loader's, registered by go/arbace/pyctypes (`SetDynamicLoader`; core
  go/arbace/py/builtins_capi_api) and loaded with it on the first use. An extension file is an export table: its first line
  `pyi-c-world ctypestest`, then the world's `PyInit_` (`PyInitU_` and punycode for a non-ASCII name) functions it exports
  (go/arbace/pyctypes/capi_dynamic: "dynamic module does not define module export function (PyInit_X)" with name and path
  otherwise, as dlsym's miss). `tools/pyi/lib-dynload/` holds `_testsinglephase.arbace-314.so` and
  `_testmultiphase.arbace-314.so`; bin/pyi puts the directory on sys.path after the stdlib (CPython's lib-dynload). CPython
  3.14.10's `Modules/_testsinglephase.c` and `Modules/_testmultiphase.c` (with its clinic header) are built into the world
  (lib.conf; key ba0de8fa, 27 min 48 s). Single-phase: the init function's module, its `__file__`, m_size -1's dictionary
  copied for a later load (import_find_extension's m_copy); multi-phase: PyModule_FromDefAndSpec2's create part (the slots
  checked: unknown IDs, two create slots, two multiple-interpreters or gil slots; Py_mod_create's object, a non-module that
  asks for state or execution slots refused), PyModule_ExecDef by exec_dynamic once; an unreported exception is the
  SystemError's `__cause__` (_PyErr_FormatFromCause); an uninitialized definition (PyModuleDef_Init never ran:
  `ob_type` NULL, PyModuleDef_Init now sets it and m_index) refused; PyState_FindModule/AddModule/RemoveModule by m_index.
  The empty built-in `_testmultiphase` of PY6b is removed: the module is the world's.
- **test_capi's natives** (capi_tail and the files they supersede): PyErr_SetString/SetObject/Restore as errors.c's
  (`_PyErr_SetObject`'s issubclass check through `__subclasscheck__`, the "Normalization failed: type=T args=..." note,
  PyErr_Restore's traceback), PyErr_WriteUnraisable and FormatUnraisable with err_msg None and the default hook's header on
  sys.stderr (core: `ExtWriteUnraisableMsg`), PyUnicode_FromFormatV ported whole (flags, width, precision, `*`, the size
  modifiers, `%c %d %i %o %u %x %X %p %s %ls %U %V %S %R %A %T %#T %N`; PyErr_Format and FormatUnraisable use it),
  PyUnstable_Exc_PrepReraiseStar (`_PyExc_PrepReraiseStar` over BaseExceptionGroup.split by leaf identity), PyErr_SetExcInfo
  and SetHandledException (the thread's handled exception), PyConfig_Set of sys.flags' members (a new flags object, core
  `ExtSetSysFlag`), int_max_str_digits and cpu_count (core `ExtSetCPUCount`), the allocator domains with
  PyMem_GetAllocator/SetAllocator (a C allocator's functions called for its domain), PyInterpreterState_Get/GetDict,
  PyUnstable_AtExit (run by Py_EndInterpreter), PyGILState_GetThisThreadState, the code extra, `_PyObject_New(Var)` of a
  built-in type, a set mirror for PySet_GET_SIZE (fill and used), `__ipow__` with the optional modulo on a C type.

## As built: the code level without positions (2026-11-22, the main host, py-small-1122; the user's (b) of 2026-11-22)

The round trip's level 3 (§5.2, "As built: PY2") compared the code compiled from the original's text and from the print's,
normalized by `norm_code`; mode `:lines` records no positions and CPython 3.14's code generation follows them (E7 corrected:
NOPs kept by line, superinstructions fused within a line, exception ranges and jumps split by line), so 30 files were
`:code-lines`, by design (queue 175's DECISION (a)), and pip's `candidates.py` `:own-code-lines` under pyi's own levels (queue
431). The user chose (b) on 2026-11-22: level 3 a compile without positions, comparing what the forms record.

- **Live** (CPython; the read cell's records `test/python/roundtrip-{known,results}.edn`): `tools/pydump/codelevel.py` parses each
  file, sets every node's `lineno`, `col_offset`, `end_lineno` and `end_col_offset` to 1, 0, 1, 0, compiles the tree and digests
  `pydump.py`'s `norm_code` of it; `bin/repr-python`'s `code_levels` writes the TSVs with that CODE column as `level-*.tsv` in JOBS
  processes (the helper's TSVs stay as dumped, the costs' and the records' input). Not in `pydump.py`: its digest keys every member
  of the python-pydump packs, whose CODE column (positions included) stays the oracle's record. Recorded rows
  (`REPR_PYTHON_RT_LEVELS=recorded`): level 3 implied by level 2 (`compare --recorded`), a compile without positions being a
  function of the ast without attributes.
- **Records mode** (pyi's own levels): `own-code` compiles the forms read in mode `:lines` (no positions) instead of the text read
  in mode `:full`; `bin/pyi compile-check` compiles every corpus file's `:lines` forms already (`compile-known.edn` empty).
- **The control** (both modes): each control pair's reference print with `__code_control__ = 0` appended (`code_mutants`, rows by
  `codelevel.py` live; `own-mutant` in records mode): the print's row with the mutant's digests fails level 2, and with the mutant's
  code beside the print's own ast, level 3.
- **Proved** on the rows the change can move (a file whose ast is equal compiles to one code without positions): the 30 files and the
  three corpora's control pairs, `--record --only-list` live and in records mode: all levels pass, the controls fail as they must;
  roundtrip-known.edn's `:code-lines` and roundtrip-own-known.edn's `:own-code-lines` empty (kept, their texts saying so). An
  `--only-list` record no longer rewrites `roundtrip-costs.edn`. The read cell: 3,702 to 3,732 passes, by design 34 to 4.

## As built: the moment at a frame's return, gated (2026-11-22, the main host, py-small-1122; the user's (b) of 222's DECISION 1)

"As built: 222" left the moment at `del` alone (DECISION 1 (a) then); the user chose (b) on 2026-11-22: finalization at a frame's
return, gated so that it costs about nothing while no object with a finalizer waits, measured against the 3% budget.
`synth/datamodel/del.py` was its case: `[C(1) for _ in [0]] and None` drops a list whose element has a `__del__` inside a lambda,
and CPython prints `del 1` before the caller prints `0 -> None`; pyi printed it at exit (`:finalization`, `:finalization-goi`).

- **The notes** (`go/arbace/py/finalize.go`): an instance whose class has a `__del__` (`trackDel`, from `objectNew` and
  `wrapInstance`) is noted on its thread with the depth of the frame that made it (`finPendAdd`; `Thread.finPend`, at most 32, and
  `Thread.finPendDepth`, the deepest note's depth, 0 for none).
- **The gate** (`eval.go`'s loop, both return paths: every Python frame's return, compiled or on the loop, goes through it):
  `t.finPendDepth > t.depth` after the depth's decrement, one load and a compare. `__init__`'s own return does not open it (the
  instance was made at its caller's depth).
- **The check** (`finAtReturn`): not within a drain (a finalizer's own frames), the gate shut while it runs; goi's slots scrubbed
  (`scrubSlots`, dropGen's); `runtime.GC()` and `finBarrier` (done once every note of the returned depth moved), the drain, rounds while a
  finalized note's weak references are left (finVerify's); then `finPendSettle`: a dead note (its record gone or its `__del__` run)
  dropped, a survivor noted at the caller's depth, at most 3 checks; the time on finSpent's budget (50 ms and a tenth of the wall
  time, the moment at del's), past it the notes dropped.
- **Files keep the collection's moment** (`classInfo.delNote`): the first measure found every `open()` and every import's
  `FileIO` and `BufferedReader` noted (pyi's `_io` is `_pyio`, whose `IOBase.__del__` they inherit): 11 to 23 forced collections at
  a benchmark's start (GODEBUG=gctrace=1) and +6.9% compiled, +4.0% on the loop (wall, least of 5). A class whose `__del__` is
  `_pyio`'s is tracked as before and not noted: the benchmarks' runs then have base's collections exactly (bm_chaos 3 with 1
  forced, bm_float 3 with 1, bm_generators 3 with 0).
- **The cost** (the 10 benchmarks of 222's table, each compiled by `bin/pyi aot` at base and at the branch, run back to back,
  base first on odd rounds, 10 rounds, the least; the host loaded, 50-70 on 64 CPUs, so a benchmark's ratio moves by up to 10%):

| sum of the 10 | base ms | branch ms | branch / base |
|---|---:|---:|---:|
| compiled, wall | 12,580 | 12,950 | 1.029 |
| compiled, CPU | 13,120 | 13,469 | 1.027 |
| the loop, wall | 14,534 | 14,416 | 0.992 |
| the loop, CPU | 15,130 | 15,080 | 0.997 |

  Under the 3% budget; with no collection added, what is left is the compare (noise beside it).
- **Proved**: `bin/pyi check --aot --no-pyi --only-list` over del.py and the corpus's programs that make objects with a `__del__`
  (asyncio's loop: synth/constructs/async.py, synth/paths/async-gen-aclose.py, samples/functions.py; no other program of the corpus
  defines or imports one): 4 of 4 pass, del.py by its golden text compiled and on the loop; the negative control fails as it must.
  test_gc, test_weakref and test_finalization by `bin/pyi check --aot --lib-test --fresh --only` at base and at the branch: 194 pass,
  63 classed, 0 failing both, every row the same (test_trashcan_threads' counts aside, a failure both times). The AOT record's
  del.py row (and the eval cell's loop column) moves at the next whole `bin/pyi check --aot --record` (whole only, on the gate's
  build at the checkout's key); `:finalization-goi` and the synth `:finalization` test go then.

## As built: 521 and 525 merged, the C accelerators in the executable (2026-11-23, the main host, py-accel-1122)

Queues 521 (`_datetime` and `_decimal` from CPython 3.14.10's own C, built by ci into the world ctypestest, compiled into the executable
through goi's table: `bin/pyi-clib aot-forms`, `go/arbace/clib/ctypestest/aot`, `go/arbace/clib/worlds`; DECISION 521-2 (b)) and 525
(the gaps those modules exposed, DECISION 521-3 (a)) merged on next with two fixes: j2g's program copy now takes arbace/clib,
arbace/pyctypes, arbace/clib/worlds and the world's packages, and the world is built again on next's ci builder (queue 522's soft-float
long double). On `a179a8e5c`'s build (521's world, before the rebuild) the eval of test_datetime, test_types and test_decimal gives 1,905 of 1,969 methods passing, 0
failing (999 more than the record); the classes: `:c-accelerators` now covers _pickle and _json alone, and the 13 test_datetime methods
_datetime's C reaches beyond pyi's C API and C world are the deferred `:c-accelerator-gaps` (fromisoformat's NULL dereference, a lone
surrogate through PyUnicode_FromFormat's %c, the C world's local zone names without tzset, subclass constructors, one timedelta
computation, subinterpreters). PYI_C_ACCEL=0 runs the pure twins as before.
## As built: the user's n/a decisions of 2026-11-23 (na-apply-1123)

The user's decisions on the eval tail's proposal (A-F, H's test_gc counts, I, J1-J3 taken; G not now; doc/JOURNAL.md, "Python and
C++: the user's n/a decisions of 2026-11-23"), applied to `test/python/divergences.edn` from the record, no run:
`:cpython-marked` (CPython's own decorators, its list `test/python/cpython-marked.txt` made by `test/python/cpython_marked.py`
from the pinned tree and read through the class key `:tests-file`), `:bytecode` (absorbs `:dis`), `:object-layout` (absorbs
`:inline-values`, `:specializing`, `:tuple-id-reuse`, `:pyc-magic`), `:gc-cycle-count` (test_gc's twelve counts), and n/a for
`:remote-debugging`, `:gc-collector`, `:fails-under-cpython-too`, `:subtest-ids`, `:tracemalloc-driver-depth`; `:skipped-by-both`
keyed also on `:excluded` and on bin/reap's `:no-tests` (a module row whose CPython run recorded no method); `:fork`'s two
patterns more; test_capi's 51 `_testinternalcapi` rows out of `:capi-port`'s list. The eval cell: 40,552 / 1,604 / 1,714 (4,454
n/a) -> 40,552 / 1,398 / 1,585 (4,789 n/a). Refresh the list with the record: `python3 -I test/python/cpython_marked.py`
(`--check test/python/cpython-marked.txt` compares). Left as decided: `:refcount-moment` (G) after 222 (b), and the rest of
`:finalization-cycle` by design.

## As built: 536, the largest deferred classes of the eval level (2026-11-23, the Mac VM, queue 536)

The deferred classes of `test/python/divergences.edn` by rows on next fa635f636's record (`bin/pyi divergences --table`), less those
others own this round (537 the design classes; 535 D1-D6: multiprocessing, the native _io's checks, the spawn modules' limits,
sysconfig, fileio's strace; the main host :c-accelerators, pyctypes' `:capi-port`, the cap-crash modules):

- **`:goi-liveness-fallthrough` (255 rows): 254 were stale.** test_xml_etree's and test_xml_etree_c's rows were the records
  night's (2026-11-19), before goi's fallthrough fix (cfd69a6d2) reached an executable; the re-record of 2026-11-22 left both
  modules out. Run at this branch (goi interpreting py and pyexpat), all 254 pass; both modules are recorded again. The class
  keeps test_multiprocessing_spawn's test_rapid_restart.
- **`:audit-hooks` (23 rows): PEP 578 in pyi** (`go/arbace/py/audit.go`). `sys.addaudithook` keeps the hooks (one list for the
  process, as the interpreter's `audit_hooks`), `Audit(event, args...)` is PySys_Audit (each hook called with the event and the
  arguments' tuple; the first error stops it and fails the operation), `sys.audit` checks as `sys_audit` does (nothing without a
  hook, then "expected str for argument 'event'"), and `sys.addaudithook` first raises "sys.addaudithook" (an Exception refuses the
  hook silently, a BaseException propagates). The runtime raises the events test_audit checks, each at CPython's place and with
  its arguments: marshal.dumps (dumps and dump), marshal.loads, marshal.load; object.__setattr__ (check_set_special_type_attr:
  `__name__`, `__qualname__`, `__module__`, `__doc__`, `__bases__`, `__type_params__`; object_set_class's `__class__`);
  mmap.__new__; _posixsubprocess.fork_exec; sys.excepthook (PyErr_Print: a hook's RuntimeError ends the display); sys.unraisablehook
  (a user's hook); socket.gethostname, socket.__new__, socket.bind; gc.get_objects, get_referrers, get_referents; sys._getframe,
  sys._getframemodulename; _thread.start_new_thread, start_joinable_thread; syslog.openlog (the implicit one too, now with
  LOG_USER as CPython's), syslog.syslog, setlogmask, closelog; time.sleep (before the conversion); sys.monitoring.register_callback.
  pickle's find_class and sqlite3's connect are their modules' own `sys.audit` calls. Not given: CPython's C hooks
  (PySys_AddAuditHook), `__cantrace__` (pyi's tracer is not suspended while a hook runs: test_cantrace), and FileIO's "open":
  pyi's `_io` is `_pyio`, whose FileIO audits nothing (test_open; a wrapper of FileIO.__init__ was tried and withdrawn, `_io` being
  535's this round), nor the many events no test of the class needs (exec, compile, import, os.*, ...).
- **`:os-and-files-misc`: `os.fspath`** gives a str or bytes subclass's instance as it is (PyOS_FSPath's PyUnicode_Check):
  test_pathlib's six test_str_subclass_common rows pass; test_pathlib recorded again (971 pass, 398 skipped).
- **`:dbm-sqlite3-locking` (30 rows): not closed.** crt's fcntl F_GETLK answers 0 without writing l_type, so SQLite's unix VFS
  takes every probe for a conflict (the main host's lead). Answering F_UNLCK moves SQLite's WAL on to mmap its `-shm` file, which
  crt refuses (ENODEV): "disk I/O error", and test_dbm's test_clear fails. A file mmap in crt (a copy, written back at munmap, as
  emu's fileMap) makes WAL work, but the rows still fail on pyi's own cause: dbm.sqlite3 never closes the cursors of `PRAGMA
  journal_mode = wal` and of `__setitem__`, which CPython's reference count resets at once; under pyi the statements stay active
  until the collector runs, so a write is not in the file when the database is closed and reopened. Both crt changes were
  withdrawn (crt is the C crop's); the report's DECISION 536-1 carries the patch.

The eval cell: 40,552 / 1,604 / 1,714 before, 40,827 / 1,610 / 1,437 after (pass / design / deferred; the design count's +6 are
the six C14N subtests the xml modules now reach, skipped by the test); Python's completeness 0.9794 to 0.9810.

Proofs (the VM, arm64; stage 0 nx's with goi interpreting the checkout's py; `PYI_RUNNER=python3` drives the runner alone, so a py
or crt change does not compile the runner's AOT module again): `bin/pyi check --only` of test_audit, test_dbm, test_dbm_sqlite3,
test_xml_etree, test_xml_etree_c and test_pathlib's six rows, then test_audit and test_pathlib whole with the four timing rows and
xml_etree_c's eight IOTest and Iterparse rows again (`PYI_LIB_TEST_TEST_TIMEOUT=600`, `PYI_LIB_TEST_TIMEOUT=10800`); the three
negative controls fail as they must in each. The record's rows of those four modules were written by `bin/pyi check --judge DIR
--record` over copies of the run directories holding those modules alone; xml_etree_c's eight rows are the second run's (the first
run's errors came from a second `--only` run of the same worktree cleaning the shared `.tmp/t6i` directories). Two rows went the
other way on the VM: test_deeply_nested_deepcopy is past 600 s with py interpreted (`:runner-limits`, `:slow-on-goi`: it passes on
the main host's compiled runtime) and test_cyclic_gc fails whole and alone (`:finalization-collect`; the main host's whole-module
runs failed it too).

## As built: 535, forkserver without os.fork (D1) and the rest of D1-D6 (2026-11-23, the Mac VM, queue 535; the user's "Build them")

The user's word of 2026-11-23 on py-mods' six (D1-D6, doc/AGENDA.md's questions) supersedes the decision of 2026-11-04 (PY6e's
DECISION (a)) and of 2026-11-22 for the start method forkserver: build it. `os.fork` stays absent (`has_fork_support` False): the
start method fork and the tests of os.fork itself stay `:fork`, n/a.

**D1, the design.** A forked Go process keeps one thread of a runtime that needs its others (the scheduler's, the collector's
workers, the netpoller, sysmon): Python code cannot run in it, so a general `os.fork` is not built (the alternative, a fork-safe
child by a patched Go runtime in `overlay/go`, would only reach the executables built after a re-seed, and stage 0's goi would
still run the eval cell on the seed's runtime). But the fork server's child does nothing with the server's state that a fresh
process cannot do again: after `os.fork()` it closes the server's descriptors, clears atexit, restores the signal handlers and runs
`_serve_one`, which reads the preparation data and the process object from its pipe (`spawn._main`), as a spawned child does.
So pyi's fork server starts each child as a fresh process of `sys.executable` (`util.spawnv_passfds`, as the server itself was
started), with the request's descriptors at their numbers (child_r, the alive pipe, the resource tracker's, the client's own); the
child does the server's preload again (sys.argv, sys.path, `__main__` imported as `__mp_main__` with `_inheriting`, the preload
modules), clears atexit and registers `util._exit_function` as the forked child does, restores the server's original handlers
(passed by name: SIG_DFL, SIG_IGN, default_int_handler; the server's SIGINT is ignored and an ignored disposition survives exec)
and runs `_serve_one`. The server keeps forkserver's protocol unchanged: the authentication, the descriptors received by
SCM_RIGHTS, the child's pid written to the client, its exit status from the server's SIGCHLD wakeup and `waitpid(-1, WNOHANG)`, the
child the server's own (its parent pid the server's). Spawn as the default (249's option (b)) was not taken: forkserver works.

**Where.** CPython's `Lib/multiprocessing/forkserver.py` is pinned and unpatched on disk; `arbace/python/libpatch.ae` (new) holds
pyi's patches of the pinned Lib, applied by `arbace.python.run/compile-source`, the one place every pyi compile goes through (imports,
exec and compile on goi; the AOT's modules compiled ahead and its compile-server). A patch applies in mode exec to the exact text it
was written against (3.14.10's file by SHA-256 `12eb380e…`, 3.15.2's `811a5bfc…`, whose preload is its own `_handle_preload`), so
another text compiles as it is; it replaces one line (`pid = os.fork()` by `pid = _pyi_spawn_one(...)`) and appends
`_pyi_spawn_one`, `_pyi_serve_one` and `_pyi_preload` after the file's last line, so the module's line numbers are the file's
(tracebacks and linecache read the file). The patched answer is kept in the IR cache under the key of the source as given, as
go/arbace/py's aot.go looks the cache up itself by the text it has (the compiled launcher's server first died again on an unpatched
answer the control had kept under that key); libpatch.ae is in the compiler's digest (arbace.python.run's and bin/pyi's
`compiler_digest`). `PYI_LIB_PATCHES=0` compiles every source as it is (the control) and keeps no patched source's answer.

**What a fork alone gives, not given (n/a):** the preloaded modules' import runs again in each child, so their import-time side
effects repeat: test_multiprocessing_forkserver.test_misc's `test_preload_main`, `test_preload_main_sys_argv` and
`test_std_streams_flushed_after_preload` (mp_preload_main.py, mp_preload_sysargv.py and mp_preload_flush.py count the server's one
`__mp_main__` import in the output). They are the class `:forkserver-preload`, n/a (the server's memory copied into its child).

**The proofs** (the Mac VM, the checkout's runtime; stage 0 nx's has a stale runtime, so the eval runs are goi's interpretation of
it and the module runs are AOT's, `bin/pyi check --aot --lib-test --only`, the compiled launcher for the children). A script
(`.tmp/q535/p/fs1.py`: forkserver, a child's result through a Queue, exit statuses 0 and 7): `bin/pyi run` PASS in 5 min 17 s,
with `PYI_LIB_PATCHES=0` FAIL ("EOFError: unexpected EOF" at `read_signed`, the parent's behaviour); by the compiled launcher PASS
in 13 s. The modules against the committed eval record (`.tmp/q535/tools/moves.py`): of 1,651 rows run, 291 that the record does not
pass pass now (test_multiprocessing_forkserver's test_manager 93, test_processes 105, test_misc about 45; spawn's test_misc and
concurrent_futures' test_init, test_wait and test_shutdown, the ECHILD cascade gone; test_cprofile's and asyncio's forkserver
tests; test_sysconfig 2 and the 2 skipped before); none that it passes fails (round 3's two test_set rows, a dict `__ror__` that
took a set's operand, fixed before round 4). Left in the forkserver modules: the start method fork's (os.fork), the resource
tracker's signals and test_preload_resources and test_large_pool (`:multiprocessing-children`'s, as spawn's), test_heap, the four
named ahead in `:multiprocessing`, the three of `:forkserver-preload`. The negative controls of the check failed as they must in
every round.

**D4 (the runner's limits).** The fork server's children are now pyi processes as spawn's are, so test/python/lib-test-limits.tsv
gives test_multiprocessing_forkserver's test_manager, test_processes and test_misc spawn's rows (16384 MB; 3600, 7200 and 3600 s).

**D5 (the layout, in part).** `_imp.extension_suffixes()` is CPython's shape: the platform's suffix first, PEP 3149's with the
triplet (`.arbace-314-x86_64-linux-musl.so`, `aarch64` on arm64; musl's triplet, as Alpine's python3 has it), then py-capi's
`.arbace-314.so` the C world's export tables are named with (tools/pyi/lib-dynload unchanged). `_sysconfigdata__linux_` gives
`SOABI` `arbace-314-TRIPLET` and `EXT_SUFFIX` the first suffix (they were `''` and `.so`: since py-capi's suffix test_EXT_SUFFIX_in_vars
failed unrecorded), `LIBRARY` and `LDLIBRARY` `libpython3.14.a` (a static build's names). test_sysconfig's test_library,
test_linux_ext_suffix, test_EXT_SUFFIX_in_vars and test_soabi pass. Left in `:venv-and-build` (n/a): the six that ask files of an
installation or a build tree (DECISION in the report).

**Found on the way (the forkserver modules now run).** dict and set had no reflected wrappers: CPython's add_operators gives
`__ror__` (and set's `__rand__`, `__rsub__`, `__rxor__`) from the same nb slot, and a manager's DictProxy and SetProxy forward
`other | proxy` to them (TestSyncManagerTypes' test_dict and test_set under every start method); now `RBinary` of the same function
with the operands swapped. `_thread.get_native_id()` was the ident (1 in every process's main thread): the main thread's is now the
process id, as Linux's main thread's tid (test_process_mainthread_native_id).

**The classes.** The committed eval record is the main host's, recorded before 535: its forkserver rows still fail there, so they stay
classed as they are (`:fork`'s names and ECHILD match, `:multiprocessing`'s "unexpected EOF") until the next record, the classes'
`:why` saying so; `:multiprocessing` names ahead the four forkserver rows the AOT run fails as spawn's do (TestManagerExceptions' two,
test_poll_eintr, SharedMemoryServer_ignores_sigint); the three preload tests are the new `:forkserver-preload`, n/a.

**D2, D3, D6.** D3 is py-accel's. D2 (a native `_io`) and D6 (test_fileio's strace shapes) are not built here: the report's
DECISIONs.

## As built: 545, a native _io: CPython's Modules/_io ported to Go (2026-11-23, the Mac VM, queue 545; the user's 535-2 (a))

pyi's `io` was `_pyio` (§6.6, PY6a): `_io` re-exported `_pyio`'s classes. Since 545 `_io` is CPython's C module ported to Go
in go/arbace/py, and `_pyio` is the Python side only (test_io's Py* classes, as in CPython).
- **The port.** CPython 3.14.10's `Modules/_io` (tag v3.14.10, the pinned tarball, SHA-256 de414a57...b72032), with 3.15.2's
  differences under `py315()` (fileio's `truncate` mode string and readall's readable check, StringIO.truncate's `pos`,
  CHECK_CLOSED raising IS_CLOSED's error and close by IS_CLOSED, textio's write): `builtins_io.ae` (_iomodule.c: open, open_code,
  text_encoding, DEFAULT_BUFFER_SIZE 128 KiB, UnsupportedOperation a class of (OSError, ValueError) named `io`'s, the shared
  converters: clinic's Py_ssize_t, int, str, the r/w buffers, PyNumber_AsOff_t), `builtins_io_iobase.ae` (iobase.c's _IOBase and
  _RawIOBase, _BufferedIOBase, _TextIOBase), `builtins_io_bytesio.ae`, `builtins_io_stringio.ae` (with textio.c's
  IncrementalNewlineDecoder and `_PyIO_find_line_ending`), `builtins_io_fileio.ae`, `builtins_io_buffered.ae` (BufferedReader,
  Writer, Random, RWPair), `builtins_io_textio.ae`, `builtins_io_zguard.ae` (the getsets' type check, the types that inherit
  object's tp_new). Error messages, the C-only checks (uninitialized objects, detached buffers, gh-143008/142594's buffer
  checks, readinto's argument types, the misbehaving raw's OSError, reentrant calls), `_finalizing`, the pickling refusals
  and the 3.14 clinic messages are C's.
- **Objects.** The types are static (one set for every interpreter, where CPython's are each module's heap types; their
  `__flags__` say HEAPTYPE as CPython's do, so copyreg's `_reduce_ex` walks past them). Every instance holds iobase's
  `__dict__` (instanceDict, `ioDictOf`) and takes weak references. A subclass's instance keeps the Go value
  (`Instance.value`); the _io methods are `objectDescr` (they get the instance itself) and a subclass's _io slots (repr,
  iteration, `__init__`) keep the instance, so C's calls of `self`'s methods find a subclass's (class.go's fixupSlots).
  BytesIO's buffer is a slice (no copy-on-write: only CPython-only tests see it); getbuffer's view is a memoryview of a
  `_BytesIOBuffer` that counts its exports (buffer_export.go). StringIO's buffer is code points, accumulated as a string
  while every write appends. The buffered lock is ENTER_BUFFERED's: a reentrant call from the same thread (a signal
  handler's) is "reentrant call inside %R", another thread waits with the GIL released. FileIO's reads and writes are
  `_Py_read`/`_Py_write` over os.read's and os.write's interruptible waits (readInterruptible, writeInterruptible); a write
  to descriptor 1 or 2 flushes the runtime's buffered standard streams first, as os.write does.
- **Finalization.** iobase_finalize is the Go finalizer (TrackFinalizer) of an exact FileIO, buffered object, TextIOWrapper
  or base instance, and of every subclass's instance (wrapInstance), except a class with a Python `__del__` of its own
  (slot_tp_finalize; `_IOBase.__del__` is the base's). The cycle collector walks the _io objects (finalize.go's cyIsNode and
  cyTraverse), so `f.f = f` dies at `gc.collect()` with its ResourceWarning. The moment at the last reference stays Go's:
  test_io's eight test_error_through_destructor (C and Py) move to `:finalization`.
- **The standard streams** stay the runtime's own text streams (sys.go's textFile over the interpreter's bufio writers), now
  instances of types deriving from the native TextIOWrapper, BufferedWriter and BufferedReader (create_stdio's types):
  `isinstance(sys.stdout, io.TextIOWrapper)` holds without ABC registration; a native method or getset the stream does not
  define is the descriptor's TypeError on it (`sys.stdout.detach()`, CPython's detaches).
- **_pyio is no longer code that stands for C** (pylifecycle.go's cImplModules: `_py_warnings` alone), so its frames are traced
  and profiled as Python's, as in CPython.
- **The open audit event**: FileIO's `__init__` calls `sys.audit("open", name, mode, flags)` (PySys_Audit); queue 536's audit
  hooks answer it once merged (test_audit's test_open row).
- **Proved** on the VM (AOT, `bin/pyi check --aot --lib-test --fresh --only`): test_io 631 pass (590 in the record), test_fileio
  97 (92), test_file_eintr 18 (12), test_fileinput 57 (56), test_memoryio 177 (166); 0 failing, no row the record passes
  fails; the controls fail as they must (.tmp/q545/aot-final.log, from the committed forms after the merge with next). 64 rows of the record move to pass at the main host's re-record (`:io-c-checks` 38,
  `:runner-limits` 7 (CSignalsTest's and PySignalsTest's writes: interruptible now), `:io-memory-c` 6, `:buffer-exports` 5,
  `:data-model` 4, `:pyio` 4).
- **Classes**: `:strace-leader` (n/a, 535-3 (a): test_fileio's two test_syscalls_read); `:io-c-checks`, `:io-memory-c`, `:pyio`,
  `:pyio-size-type` and `:pyio-buffered` keep their rows until the re-record (their why says so).

## As built: 537, the largest by-design classes at eval: _hashlib over Go's crypto, the pseudo-terminal functions (2026-11-23, the Mac VM, queue 537)

The eval cell's `:kind :design` classes, largest first by the record's rows (test/python/check-lib-test-linux-amd64.edn at next
fa635f636, classified as `bin/reap` does, the references' outcomes keying `:ref-outcomes`): `:c-accelerators` 1,092 (the main
host's), `:skipped-by-test` 236, `:extension-modules` 66, `:finalization` 57, `:finalization-cycle` 25, `:coroutines` 20,
`:ssl-go-tls` 19, `:gc-collector` 14, `:child-is-pyi` 10, `:inline-values` 9, `:fails-under-cpython-too` 8, `:pyio` 7 (535's),
`:openssl` 6, `:subtest-ids` 6, `:pyio-size-type` 5 (535's), the rest 3 or fewer.

- **`_hashlib` built** (`go/arbace/py/builtins_hashopenssl.ae`): Modules/_hashopenssl.c of CPython 3.14.10 over Go's crypto in
  place of OpenSSL's EVP interface, as `_ssl` is over Go's crypto/tls (PYS22 (a)). `new`, the twelve `openssl_*` constructors,
  `HASH` and `HASHXOF` (a subclass), `hmac_new` and `HMAC` (builtins_hmac.go's hmacObject with this module's type; the methods
  shared by `hmacMethods`), `hmac_digest`, `pbkdf2_hmac` (RFC 8018 over the module's HMAC), `scrypt` (RFC 7914: Salsa20/8,
  BlockMix, ROMix, with OpenSSL 3's parameter checks and its 32 MiB default memory limit), `compare_digest` (`_operator`'s),
  `get_fips_mode` (0), `openssl_md_meth_names`, `_constructors`, `UnsupportedDigestmodError`, `_GIL_MINSIZE`. The digests are
  `py_hashes`' that Go has (md5, sha1, sha2 with the truncated sha512s, sha3, shake, blake2 by hashlib_blake2.go), found by the
  Python name or OpenSSL's names and aliases without case; OpenSSL's md5-sha1, ripemd160 and sm3 are not offered (absent from
  `openssl_md_meth_names`). OpenSSL's error-queue words are kept ("[digital envelope routines] unsupported", "no reason
  supplied" for an HMAC over an XOF or `HASH.digest` of one, scrypt's "memory limit exceeded"). hashlib.py and hmac.py now take
  it first, as CPython's do.
- **The pseudo-terminal functions** (`builtins_posix_ext.ae`): `os.posix_openpt`, `grantpt`, `unlockpt`, `ptsname` as
  posixmodule.c over musl 1.2.6 (grantpt does nothing; unlockpt TIOCSPTLCK 0; ptsname TIOCGPTN's /dev/pts/N; the fildes converter).
- **Pickling refusals** name the type's tp_name ("cannot pickle '_hashlib.HASH' object", '_md5.md5', 'zlib.Compress').
- **int() and complex() as 3.14's constructors**: `int(x)` of an int subclass calls its own `__int__` (PyNumber_Long's nb_int;
  a strict subclass of int returned is deprecated, as for `__index__`); `complex(x)` returns a single exact complex argument as
  it is, looks `__complex__` up on any type but that one (a complex subclass's too: test_complex's complex0, complex1), warns
  of a strict subclass of complex returned (Issue #29894), and in complex_new_impl's path (two arguments or keywords) warns of
  a complex 'real' even without 'imag' and refuses a string ("argument 'real' must be a real number, not str"); cmath's and the
  C API getargs' `D` take the same warning (PyComplex_AsCComplex).

Proofs (the VM, eval on goi with the checkout's runtime, `bin/pyi check --only`, the negative controls failing as they must in
each run): test_hmac 143 ok and 3 bigmem skips, test_hashlib 76 ok and 14 skips (CPython's resources), test_os' two
PseudoterminalTests, test_int, test_complex, test_cmath, test_index, test_capi's test_D, test_uuid and test_urllib2 (two
timeouts past 300 s at load 30-50, both children of pyi on goi; test_http_body_pipe passes alone); a differential probe of
`_hashlib` (83 lines) and of `complex()` (70 cases) against python3 3.14.10, equal but the pickling message fixed after.
The record's rows of the modules run whole were written by `bin/pyi check --judge DIR --record` over a directory of those
runs (test_hmac, test_hashlib, test_uuid, test_urllib2, test_int, test_index, test_complex, test_cmath; as queue 536 recorded):
test_uuid's test_unix_getnode_from_libuuid and test_urllib2's test_http_body_pipe recorded as the VM's load timeouts
(`:runner-limits`; the second passes alone), the two setUpClass skip rows of test_hmac gone. The rows of modules run by id
(test_os' PseudoterminalTests, test_capi's test_D) wait for the main host's whole record. Python eval at next 320e74469 (the
user's n/a decisions merged): 40,552 / 1,398 / 1,585 (4,789 n/a) -> 40,611 / 1,341 / 1,581; `:skipped-by-test` 87 -> 38,
`:openssl` 6 -> 0, `:coroutines` 20 -> 18.

What stays (queue 537's report, `.tmp/q537/537-py-design.md` on the VM, its DECISIONs): `:c-accelerators` (the main host's),
`:extension-modules` (66: built by py-capi-1122 after the record's commit, moved by the next whole record), `:finalization` (57),
`:finalization-cycle` (13), `:weakref-list` (3) and `:coroutines`' two test_bpo_45813 (reference counting's moment),
`:coroutines`' 15 test_warnings DeprecatedTests (`:warnings-c`'s cause: no C `_warnings`, whose `warn` reads
`sys.modules['warnings']` at each call, which BaseTest.setUp swaps; `_py_warnings`' `_wm` does not follow), `:ssl-go-tls` (19),
`:child-is-pyi` (10), the `_pyio` classes (535's), `:skipped-by-test`'s 36 (among them `_elementtree` 19 and `_zoneinfo` 2:
accelerators; `_testbuffer` 3; the UTF-8 mode 3; ctypes' complex and dllist 6, the main host's).

## As built: 545, a native _io: CPython's Modules/_io ported to Go (2026-11-23, the Mac VM, queue 545; the user's 535-2 (a))

pyi's `io` was `_pyio` (§6.6, PY6a): `_io` re-exported `_pyio`'s classes. Since 545 `_io` is CPython's C module ported to Go
in go/arbace/py, and `_pyio` is the Python side only (test_io's Py* classes, as in CPython).
- **The port.** CPython 3.14.10's `Modules/_io` (tag v3.14.10, the pinned tarball, SHA-256 de414a57...b72032), with 3.15.2's
  differences under `py315()` (fileio's `truncate` mode string and readall's readable check, StringIO.truncate's `pos`,
  CHECK_CLOSED raising IS_CLOSED's error and close by IS_CLOSED, textio's write): `builtins_io.ae` (_iomodule.c: open, open_code,
  text_encoding, DEFAULT_BUFFER_SIZE 128 KiB, UnsupportedOperation a class of (OSError, ValueError) named `io`'s, the shared
  converters: clinic's Py_ssize_t, int, str, the r/w buffers, PyNumber_AsOff_t), `builtins_io_iobase.ae` (iobase.c's _IOBase and
  _RawIOBase, _BufferedIOBase, _TextIOBase), `builtins_io_bytesio.ae`, `builtins_io_stringio.ae` (with textio.c's
  IncrementalNewlineDecoder and `_PyIO_find_line_ending`), `builtins_io_fileio.ae`, `builtins_io_buffered.ae` (BufferedReader,
  Writer, Random, RWPair), `builtins_io_textio.ae`, `builtins_io_zguard.ae` (the getsets' type check, the types that inherit
  object's tp_new). Error messages, the C-only checks (uninitialized objects, detached buffers, gh-143008/142594's buffer
  checks, readinto's argument types, the misbehaving raw's OSError, reentrant calls), `_finalizing`, the pickling refusals
  and the 3.14 clinic messages are C's.
- **Objects.** The types are static (one set for every interpreter, where CPython's are each module's heap types; their
  `__flags__` say HEAPTYPE as CPython's do, so copyreg's `_reduce_ex` walks past them). Every instance holds iobase's
  `__dict__` (instanceDict, `ioDictOf`) and takes weak references. A subclass's instance keeps the Go value
  (`Instance.value`); the _io methods are `objectDescr` (they get the instance itself) and a subclass's _io slots (repr,
  iteration, `__init__`) keep the instance, so C's calls of `self`'s methods find a subclass's (class.go's fixupSlots).
  BytesIO's buffer is a slice (no copy-on-write: only CPython-only tests see it); getbuffer's view is a memoryview of a
  `_BytesIOBuffer` that counts its exports (buffer_export.go). StringIO's buffer is code points, accumulated as a string
  while every write appends. The buffered lock is ENTER_BUFFERED's: a reentrant call from the same thread (a signal
  handler's) is "reentrant call inside %R", another thread waits with the GIL released. FileIO's reads and writes are
  `_Py_read`/`_Py_write` over os.read's and os.write's interruptible waits (readInterruptible, writeInterruptible); a write
  to descriptor 1 or 2 flushes the runtime's buffered standard streams first, as os.write does.
- **Finalization.** iobase_finalize is the Go finalizer (TrackFinalizer) of an exact FileIO, buffered object, TextIOWrapper
  or base instance, and of every subclass's instance (wrapInstance), except a class with a Python `__del__` of its own
  (slot_tp_finalize; `_IOBase.__del__` is the base's). The cycle collector walks the _io objects (finalize.go's cyIsNode and
  cyTraverse), so `f.f = f` dies at `gc.collect()` with its ResourceWarning. The moment at the last reference stays Go's:
  test_io's eight test_error_through_destructor (C and Py) move to `:finalization`.
- **The standard streams** stay the runtime's own text streams (sys.go's textFile over the interpreter's bufio writers), now
  instances of types deriving from the native TextIOWrapper, BufferedWriter and BufferedReader (create_stdio's types):
  `isinstance(sys.stdout, io.TextIOWrapper)` holds without ABC registration; a native method or getset the stream does not
  define is the descriptor's TypeError on it (`sys.stdout.detach()`, CPython's detaches).
- **_pyio is no longer code that stands for C** (pylifecycle.go's cImplModules: `_py_warnings` alone), so its frames are traced
  and profiled as Python's, as in CPython.
- **The open audit event**: FileIO's `__init__` calls `sys.audit("open", name, mode, flags)` (PySys_Audit); queue 536's audit
  hooks answer it once merged (test_audit's test_open row).
- **Proved** on the VM (AOT, `bin/pyi check --aot --lib-test --fresh --only`): test_io 631 pass (590 in the record), test_fileio
  97 (92), test_file_eintr 18 (12), test_fileinput 57 (56), test_memoryio 177 (166); 0 failing, no row the record passes
  fails; the controls fail as they must (.tmp/q545/aot-final.log, from the committed forms after the merge with next). 64 rows of the record move to pass at the main host's re-record (`:io-c-checks` 38,
  `:runner-limits` 7 (CSignalsTest's and PySignalsTest's writes: interruptible now), `:io-memory-c` 6, `:buffer-exports` 5,
  `:data-model` 4, `:pyio` 4).
- **Classes**: `:strace-leader` (n/a, 535-3 (a): test_fileio's two test_syscalls_read); `:io-c-checks`, `:io-memory-c`, `:pyio`,
  `:pyio-size-type` and `:pyio-buffered` keep their rows until the re-record (their why says so).


## As built: _elementtree and _zoneinfo in the C world, ctypes' complex types (2026-11-24, the main host, py-accel-1122)

The Mac's 537 routed the C-accelerator and ctypes rows here (DECISION 537-6 (a)). The world ctypestest also builds CPython's
`Modules/_zoneinfo.c` and `Modules/_elementtree.c` (glue as 521's) and the tarball's Expat C; `_elementtree` reaches Expat
through `pyexpat.expat_CAPI`, which the glue makes over the world's Expat since pyi's pyexpat is Go (`pyi_expat_install`, called
by pyctypes before the module's PyInit). pyctypes gained ctypes' complex types (`D`, `F`, `G`), `_ctypes.dllist`, and the C API
natives the new modules called (`PyType_GetModuleState`, `PyObject_SelfIter`, `_PyThreadState_GetCurrent`, `Py_ReprEnter`,
`Py_ReprLeave`, `_PyErr_ChainExceptions1`). On the targeted AOT runs: test_xml_etree_c 231 / 23 / 0, test_zoneinfo 244 / 33 / 0,
ctypes' 7 ids 5 / 2 / 0. Left in `:elementtree-c` (deferred, 6): XMLPullParser's flush and small-chunk events, a deep deepcopy's C
stack, an Element's weak reference, a Go panic in TreeBuilder's late tail. The world's aot forms (11.5 MB of node program) now
need more than 8 GiB to print.

## As built: py-core-1124, the core objects' and builtins' rows of the eval level (2026-11-24, the main host, the burst's py-core stream)

The burst's py-core stream (the coordinator's scope: py-na-proposal's "core objects and builtins" and "stdlib misc", with
:coroutines from the Mac's 551), on mac/py-mac-1123 (5d93b6732). The record's rows of those classes grouped by cause; built, the
largest causes first:

- **The built-ins' docstrings** (`:builtin-docstrings`, `:docstrings`, functools' test_builtin_update, pydoc's generic-alias and
  union-type rows). `tools/pyi/runtime_tables.ae`'s textsig reading (queue 357) also makes `builtins_docs_table.go`
  (`builtinDocs`, gen-tables `docs`, made in the same run as textsig: `bin/pyi runtime gen-tables textsig docs`, the --full step
  pyi-runtime-textsig): every built-in type's, method's and module function's `__doc__` as `_PyType_GetDocFromInternalDoc` gives it
  (the internal doc less Argument Clinic's "name(sig)\n--\n\n"), keyed as textSigs (`type:`, `meth:`, `func:`), and the getset and
  member descriptors' docs (`attr:`, PyGetSetDef's and PyMemberDef's doc, Py_tp_getset and Py_tp_members of the specs too); its
  control: bin()'s doc one word changed. The runtime reads it: a built-in function's or method descriptor's or classmethod
  descriptor's `__doc__` (builtinDoc, found as builtinTextSignature finds a signature; a method read from its type's `__dict__`
  by the type whose dict holds it, builtinDictOwner), a static type's dict's `__doc__` (type_dict_set_doc: its doc, else None as
  before), a getset or member descriptor's (getsetDoc). Doctest's finder over builtins now finds CPython's 14 objects with
  examples.
- **Struct sequences are tuples** (`:struct-sequences`, `:structseq-protocol`'s test_tuple, test_imaplib's Time2Internaldate):
  a struct sequence type's base is tuple and its value's visible items are its tuple (BaseTuple), so `isinstance(t, tuple)`
  holds and tuple's methods see them; the types CPython makes by PyStructSequence_NewType (the modules' own, not sys's static
  ones) take attributes (test_reference_cycle).
- **A non-dict `__builtins__`** (`:builtin-functions-core`): exec's, eval's and FunctionType's builtins from the globals'
  `__builtins__` kept as _PyEval_BuiltinsFromGlobals keeps it (a mappingproxy, a dict subclass, any object: Frame.BuiltinsMap,
  Function.BuiltinsMap), the builtins' loads through it (PyMapping_GetOptionalItem), IMPORT_NAME's `__import__` and
  LOAD_BUILD_CLASS's lookup too ("__import__ not found"), and _PyEval_GetBuiltin (an iterator's `__reduce__`) the running frame's.
- **compile()'s optimize** (test_builtin's test_compile): `MODE+O1`, `MODE+O2` to the host's compiler (arbace.python.run),
  `arbace.python.compile/*optimize*`: no assert statements and `__debug__` False from 1, no docstrings from 2 (the statement
  removed, as _PyAST_Preprocess does).
- **Inlined comprehensions' scopes** (`:inlined-comprehensions`): a name whose scope inside the comprehension differs from the
  unit's takes the inside one while the comprehension compiles (push_inlined_comprehension_state's temp_symbols; but a name free
  inside, and a cell inside free outside), so a name a first comprehension bound in a function and a second reads free is the
  global.
- **CO_FUTURE_ANNOTATIONS** (`:compiler-errors`, test_codeop's test_future_imports): the IR's flag `:future-annotations` on every
  code object of a unit under `from __future__ import annotations`, co_flags' 0x1000000.
- Smaller causes, each CPython's function ported: dict views' `isdisjoint` (`:dict-views-set-methods`); deque's `in` and `index`
  detecting a mutation (RuntimeError), `index`'s message, `n * deque` (`:deque-core`); int()'s base by `__index__` and the digits
  limit for every base not a power of two (`:int-core`); WITH_EXCEPT_START passing the exception's traceback to `__exit__` (it was
  None: `:with-exit-traceback`, contextlib's test_contextmanager_traceback); `_Py_c_prod`'s Annex G recovery of infinities
  (`:complex-core`); bytes' `%f` of a non-float "float argument required", a `*` precision PyLong_AsInt's, `{:zs}`'s message
  (`:format-messages`); except* calling a subclass's own `split` and checking its answer; a class body's LocalsMap not the
  globals' map (test_type_aliases' exotic globals); `__contains__ = None` blocking the fallback (`:contains-none-blocks`);
  BaseException.__setstate__ with a str subclass's key (`:setattr-str-subclass`); the exceptions' members (StopIteration.value
  ...) as member descriptors; OSError_init a no-op unless oserror_use_init (`:data-model`'s test_new_overridden); the method
  descriptors of a built-in type read from its `__dict__` (enum's shadowed attributes); object's slot wrappers in its `__dict__`
  (collections.abc.Hashable of object); dict.fromkeys of a set, frozenset or dict with the stored hashes (`:set-core`);
  frozenset's keyword refusal for a subclass without `__init__`; set.__repr__ of a subclass naming it (`:pprint-dispatch`); a
  function's and a type's `__annotate__` and `__annotations__` as typeobject.c and funcobject.c set them, a function's cache
  outside its `__dict__` (funcAnnotations: functools.update_wrapper's `__dict__` update put type's `__annotations__` descriptor
  there) (`:annotate-rest`, functools' test_update_type_wrapper); `'%'` after flags an unsupported format character, not a literal
  (unicode_format_arg); dir() of a dict subclass's locals by its keys(); input()'s flush of a stdout without one cleared; a match
  statement's patterns at their own locations (codegen_pattern's LOC(p): `:code-positions`' six test_matchcase rows); a static
  type's `__doc__` its tp_doc whatever its dict holds (property's); a class's `__name__` and `__qualname__` one str object while
  the text stands (ht_name: inspect.classify_class_attrs finds them by identity, test_enum's test_inspect_classify_class_attrs);
  type() of an OrderedDict namespace in its order (dict_merge's slow path: test_builtin's test_namespace_order, failing at the
  base).
- Tooling: bin/pyi's AOT module keeps the C libraries' cf2g translations across its rebuilds (bin/pyi-clib aot's key now the node
  program's and the translator's sources): a runtime change no longer translates the five libraries again (about 10 minutes).

- Also: OSError_init's use_init, a subclass's class attribute over an exception's field (OSError's errno: test_asyncio's three
  SelectorEventLoopSignalTests, py-os's hand-over), input()'s "lost sys.stdin" and "lost sys.stdout", `'%*r'`'s width
  PY_SSIZE_T_MIN "width too big", the owners of a built-in type's methods and class methods read from its `__dict__`
  (`__objclass__`; doctest's finder walks them).
- **Classes**: test_compile imports at this base (PyConfig_Get, 4c48d37c6), its rows run: the four compiler-shape rows named in
  `:compiler-shapes` (it named none), the two null-byte script rows (the file's message, "source code cannot contain null
  bytes", from a script run: pyi's command line) in `:source-decoding`. The classes whose rows pass now keep them until the
  train's re-record (the record is the main host's).

**Proofs** (the main host, pyi's AOT with the checkout's runtime, `bin/pyi check --aot --lib-test --fresh --only`; logs and probes in
.tmp/core/ of the worktree): probes against python3 3.14.10 (.tmp/core/p/probe1-7: every line equal but the doctest finder's
count, 758 of CPython's 783 in builtins, inside test_doctest's 750-800, the 14 examples' objects the same); the scope's 163 rows:
118 pass (run3), the rest listed below; 50 touched modules whole (6,907 methods) against the eval record: 3 rows the record passes
not passing, all three the same at the base (test_namespace_order, fixed since; test_recursion_error_cleanup, classed;
test_closerange, the host's descriptors); after the last changes test_builtin, test_enum, test_inspect, test_pydoc, test_format, test_doctest, test_exceptions and test_set whole again (2,589 methods: two rows the exceptions' field rule broke, a slotted exception's and getopt's msg, fixed by narrowing it to a built-in field left None), then test_exceptions, test_getopt and the three asyncio rows (157 methods, 0 failing; the one row differing from the record the same at the base). The negative controls failed as they must in
every run. `bin/pyi runtime gen-tables textsig docs`: textsig byte for byte, docs Arbace's, both controls differing.

**Left in the scope** (not built here): the finalization and collector rows (WeakSet's five, test_slice's cycle, test_sort's
mutating __del__, CodeWeakRefTest, test_no_exception_leak, test_structseq's gc tracking, test_coroutines' bpo_45813 pair, pdb's
frame refleak, test_trace's gc.get_referrers); C accelerators (functools' four reentrant reprs of the C partial, pydoc's _pickle and
_datetime signatures, plistlib's _datetime message); the subinterpreters' two (the Mac's 553); test_hash's three test_null_hash
(PYTHONHASHSEED unset: CPython randomizes, pyi keeps the zero key: a DECISION); the command line's (test_support's -O flags,
test_traceback's two relative file names, the two null-byte scripts); pydoc's help CLI, bad import and object addresses
(pyi's identity numbers are short: pydoc's stripid wants six hex digits), the namedtuple field descriptor; test_monitoring's
LoadSuperAttr call event; test_pyclbr's _testmultiphase.

## As built: py-os-1124, the command line, the environment and the process as CPython's (2026-11-24, the main host, the burst's py-os stream)

The eval rows of processes, the command line and the OS (`:pyi-command-line`, `:source-decoding`, `:os-and-files-misc`,
`:signal-timing`, `:run-dir-zip-main`, `:compileall-pyc`, `:pyi-main-module`, `:interactive-mode`, `:socket-gaps`,
`:hash-algorithm`; test_cmd_line and test_sys whole). The base's run (pyi's AOT, `bin/pyi check --aot --lib-test --only`, 39
modules, 595 methods) passed 40 of the 127 rows already (the record predates py-eval-1122, py-mods-1122, 537, 545 and 551).

- **The caller's environment** (`interp.go`'s entryEnviron): tools/pyi/python3 and the compiled launcher's wrapper keep the
  environment they were given in `PYI_ENTRY_ENV` (NAME=VALUE records split by \036, a newline as \035) before bin/pyi adds its
  own; the runtime reads it once, undoes its shell's PWD and SHLVL (busybox sh raises SHLVL, adds PWD when absent) and
  `posix.environ` is it; the names bin/pyi added stay in the process (the runtime and env=None children read them) but out of
  `os.environ` and `os.reload_environ()` until the program sets one. Without it (bin/pyi run, a compiled program): the process's.
- **The command line**: sys.argv's bytes from `PYI_ARGV`, decoded with surrogateescape (Py_DecodeLocale); -c's command not UTF-8
  is pymain_run_command's "Unable to decode the command from the command line:" and status 1; -c's coding spec ignored
  (PyCF_IGNORE_COOKIE: `arbace.repr.python.native/*ignore-cookie*` on goi's path, a UTF-8 decode on the compiled path); `-mMOD`
  and `-cCMD` joined; -O (each), -v, -d, -B and -R passed in `PYI_START_FLAGS`; a script whose path is not UTF-8 read by a
  prologue (the host reads files by its strings); a path inside a zip file a main importer; a script's CR LF and lone CR read as
  LF on goi's path (the reader's mode :full keeps a file's line ends for the round trip).
- **Configuration**: PYTHONPATH after sys.path[0] (getpath's `_Py_abspath`: "" and "." the working directory, a relative entry
  joined to it, undecodable bytes surrogate-escaped); sys.flags' optimize, verbose, debug and dont_write_bytecode from -O, -v,
  -d, -B and PYTHONOPTIMIZE, PYTHONVERBOSE, PYTHONDEBUG, PYTHONDONTWRITEBYTECODE (`_Py_get_env_flag`), hash_randomization as
  `use_hash_seed == 0 || hash_seed != 0`; sys.pycache_prefix from -X pycache_prefix and PYTHONPYCACHEPREFIX; -X importtime
  (and PYTHONPROFILEIMPORTTIME): import_find_and_load's lines (self and cumulative microseconds, two spaces a level, "cached"
  lines at 2, the name with backslashreplace, a failed import's line too), another value the start's error. pyi still writes no
  bytecode cache at import (sys.dont_write_bytecode stays True; the IR cache is the host's).
- **-O and -OO**: the compiler's `*optimize*` (`arbace.python.compile`): from 1 no assert statement (its "always true" warning
  kept) and `__debug__` False, from 2 no docstrings (module, class, function); every compile the runtime asks takes the level as
  the mode's `+opt1`/`+opt2` (the IR cache keys the whole mode), compile()'s `optimize` argument too (compileall's opt-1 and opt-2
  files and their hard-link deduplication), builtins.`__debug__`, the main module (arbace.python's start asks the runtime's
  `OptimizeLevel`).
- **Hashes**: randomized when PYTHONHASHSEED is unset, empty or not a seed, or under -E, -I or -R (CPython's default; queue
  304 kept them zero-keyed); bin/pyi's own verbs export `PYTHONHASHSEED=0` and `PYTHONDONTWRITEBYTECODE=1` when unset (the
  references' environment), so their drivers and their children stay deterministic and only a child whose environment drops
  the variable is randomized, as CPython's.
- **The future features compile() and the REPL inherit**: co_flags' CO_FUTURE_ANNOTATIONS and CO_FUTURE_BARRY_AS_BDFL (the IR's
  `:future-annotations`, `:future-barry` on every code of a module that imports them), compile()'s flags as the modes `+futA`
  and `+futB` (scope's `*inherited-features*`, the parser's and tokenizer's barry flag, `peg/set-barry!`), the basic REPL's
  accumulated features (PyCompilerFlags), ast.parse's CO_FUTURE_BARRY_AS_BDFL; a tree compiled from an AST object is made with
  `!=` and so compiles without barry's parser.
- **The process**: os.abort dies by SIGABRT (fhDie: SIG_DFL first); `restore_signals` makes SIGPIPE and SIGXFSZ Go's for the
  fork (Go's child sets the signals it handles back to SIG_DFL) and ignored again after, so the child starts with them default
  and a `restore_signals=False` child keeps them ignored; the start installs SIGINT's default_int_handler (not over an inherited
  SIG_IGN), so a program that never imports signal ends a SIGINT with KeyboardInterrupt; atexit as atexitmodule.c (a copy run,
  the list cleared after, an exception through sys.unraisablehook, unregister in the comparison's order).
- **Text**: str.format keeps surrogates (buildString as code points); sys.displayhook writes an unencodable repr with
  backslashreplace (sys_displayhook_unencodable); compile(), exec() and eval() of a str holding a lone surrogate raise the
  UTF-8 codec's UnicodeEncodeError; str.encode's and bytes.decode's encoding and errors arguments likewise (getargs' "s");
  exec()'s bytes take the tokenizer's get_normal_name and a decoded source's every coding spec of its first two lines is made
  inert; ast.parse of a tree past encoding/json's depth is the parser's MemoryError.
- **Sockets**: AF_UNIX getsockname and getpeername from the address and its length (Linux abstract names holding NULs);
  send and sendall interruptible (the user's decision 2 (a) of 2026-11-24): on a blocking socket in the main thread the wait
  for room is ppoll with the wake pipe and the send carries MSG_DONTWAIT, so a signal's handler runs while it waits (its
  exception ends the call, its return lets it go on), as send(2) under CPython's handlers without SA_RESTART
  (test_sendall_interrupted, no longer in lib-test-skip.tsv; test_wsgiref's test_interrupted_write).
- **n/a**: test_cmd_line's test_python_malloc_stats (`:pymalloc-stats`, the user's decision 1 (a) of 2026-11-24: pymalloc's
  statistics, an allocator detail).
- **bin/pyi's AOT module** keeps the libraries cf2g compiled (clib/LIB/aot, by their nodes' key) across a runtime change: a
  runtime rebuild is the print and copy, not 15 minutes of cf2g.
- **Proved** (the main host, pyi's AOT, `bin/pyi check --aot --lib-test --fresh --only` over the same 39 modules; the three
  negative controls failing as they must): 288 -> 332 pass, 0 failing, no row of the base's run lost; the launcher's direct
  probes (.tmp/os/t5.py, t8.py) and goi's path (`bin/pyi python` interpreting the checkout's runtime: -c's coding, a CR CR CR LF
  script, a double coding spec, a non-UTF-8 script path, -O); then 13 modules the later changes touch, whole (3,425 methods,
  3,212 pass), and test_source_encoding whole again (every row passes). On the compiled path alone, and not on goi's:
  test_subprocess's test_close_fds and test_pass_fds (the compiled launcher's compile server holds fds 6-13 in the child),
  test_threading's test_can_interrupt_tight_loops and test_signal's test_stress_modifying_handlers (a compiled tight loop never
  switches threads), test_cmd_line_script's two null-byte messages.
- **Left, and whose**: test_regrtest's test_add_python_opts (`_testcapi.config_get('buffered_stdio')` in pyctypes'
  capi_config, py-accel's world, should read the runtime's -u, -W, -b and -E); test_asyncio's three
  SelectorEventLoopSignalTests (an OSError subclass's class attribute `errno` over the instance's field: py-core's exception
  model); the wakeup-fd tests
  (_testcapi), test_jit_is_active (_testinternalcapi), test_c_locale_coercion's two (the UTF-8 mode's stdio errors, 553's),
  test_compileall's test_workers (ProcessPoolExecutor, 535's), the end's finalization rows (the Mac's).

## As built: 550, dbm.sqlite3's rows: crt's F_GETLK and file mmap as Linux's, statements as CPython's reference counts leave them (2026-11-24, the Mac VM, queue 550)

536's DECISION 536-1, the user's (a); the coordinator approved carrying crt on the branch. `:dbm-sqlite3-locking` (30 rows,
deferred) is closed: test_dbm 31 ok, test_dbm_sqlite3 25 ok (5 skips), and test_sqlite3's test_table_lock_cursor_non_readonly_select
passes too (it leaves `:finalization-goi`).

- **The causes, in their order.** With crt as it was, SQLite's WAL could not start: `F_GETLK` answered 0 and left the struct, so the
  -shm's DMS probe saw a conflict ("locking protocol" at the first write), and a file's mmap was ENODEV. Then two places where pyi's
  objects outlive CPython's reference counts: close left the connection open until Go finalized its statements (no checkpoint: the
  database 4096 bytes with -wal and -shm left, CPython's 12288 bytes alone), and dbm.sqlite3's never closed `PRAGMA journal_mode = wal`
  cursor, stepped to its row, held every later autocommit write back (python3 does the same while such a cursor lives:
  `.tmp/q550/p/rw4/py5.py`).
- **crt** (`go/arbace/crt`): F_GETLK asks the host (`hostGetLk`: syscall.FcntlFlock on the description's host file; another process's
  lock and its holder, else F_UNLCK; the program's own F_SETLK still takes none); mmap of a file is the host's mapping
  (`hostMapFile`: MAP_SHARED or MAP_PRIVATE, PROT_WRITE as asked) at the guest's address (`Mem.Place`, MAP_FIXED's) as a slab whose
  bytes are the host's (`Mem.MapFile`; `Mem.Zero` leaves a file's bytes); TamaGo copies the file's bytes in. crt moves gate_key.
- **pysqlite3** (`go/arbace/pysqlite3`, `py.ExtFinRounds`): close finalizes the statements no cursor is stepping (sqlite3_next_stmt,
  sqlite3_stmt_busy 0; the handles recorded in `stmts_final`, so a statement's later reset and Go finalizer skip them); a stepping
  statement stays with the database open, as CPython keeps a live cursor's (test_connection_reinit reads the old database through one).
  Before a statement that writes, and before COMMIT, when another statement of the connection is still stepping, the runtime's
  finalizer rounds run, each dead cursor resetting its statement (cursor_finalize); a live cursor's stays.

Proofs (the VM; `bin/pyi check --only` with `PYI_RUNNER=python3`, the three negative controls failing as they must): test_dbm,
test_dbm_sqlite3 and test_sqlite3 whole (test_sqlite3's module reached its 3,600 s on the six last WindowFunctionTests and
MultiprocessTests' memory-cap crash; those seven run again alone: six pass, test_ctx_mgr_rollback_if_commit_failed its recorded
failure, `:sqlite-process-locks`), recorded by `--judge DIR --record`; the probes `.tmp/q550/p/test-pysqlite.sh` (st1, rw4 and py5 equal to
python3's, all three failing on the parent's pyi; py5, a live cursor's write held back, the control against resetting too much) and `.tmp/q550/p/c/test-crt.sh` against gcc's builds (a lock another process holds, a shared
mapping's coherence before munmap: the controls an always-F_UNLCK fcntl and a copying mmap fail); C's eval and AOT essential subsets
and the four rows that mmap pass.

## As built: 551, _warnings in Go, the standard streams as TextIOWrappers, 537-2's n/a (2026-11-23, the Mac VM, queue 551)

The user's decisions of 2026-11-23: 537-2 (a), 537-3 (a), 545-1 (b); :builtin-modules, :runs-where-cpython-skips and
:host-clock n/a (:subinterpreters is queue 553's).

- **_warnings** (`go/arbace/py/builtins_warnings.ae`): Python/_warnings.c of CPython 3.14.10 in Go: warn (setup_context's
  frames, importlib's bootstrap frames and skip_file_prefixes skipped past the first), warn_explicit (with module_globals'
  source line through _PyImport_BlessMyLoader's checks), the registry's version, the once registry, the default action,
  filters searched in the context's list when one is set, else the global list; _showwarnmsg and WarningMessage, or
  show_warning's own line on sys.stderr when the warnings module is absent; the lock (an _thread.RLock), the
  _filters_mutated_lock_held version. warnings.py takes them as CPython's does, so each warning reads sys.modules['warnings']
  at the call (test_warnings' BaseTest.setUp swaps it). The shutdown's finalWarn, once the warnings module is gone, is
  unchanged.
- **The standard streams**: at the start (init_sys_streams, the first start hook: the encodings package is importable then;
  made while sys is created, before importlib, a TextIOWrapper's codec lookup broke `_bootstrap._setup`), sys.stdin,
  sys.stdout and sys.stderr and their `__std*__` become create_stdio's `TextIOWrapper(Buffered*(FileIO(fd, closefd=False)))`;
  input() flushes the real stdout, the REPL's prompt flushes both (flush_io), SystemExit's message goes through sys.stderr.
  The runtime's textFile stays for the REPL (`bin/pyi python` without a program), a closed descriptor and tamago.
- **n/a**: :finalization-cycle, :weakref-list and the rest of :finalization (reference counting's moment; its 30 rows that
  pass since queue 222 stay passes), :builtin-modules, :runs-where-cpython-skips, :host-clock (551b, 2026-11-24, for
  :finalization; :coroutines and its test_bpo_45813 rows are the main host's py-core).
- **Records** (551b): test_warnings whole, 186 pass, 15 classed, 0 failing; test_print 9 pass. :warnings-c keeps
  PyWarningsDisplayTests.test_tracemalloc alone (tracemalloc's object tracebacks).

## As built: 552, a vendored crypto/tls under _ssl: TLS 1.3 post-handshake authentication, PSKs, the exporter secret (2026-11-23, the Mac VM, queue 552; the user's 537-4 (a))

pyi's `_ssl` (queue 209, PYS22 (a)) ran over Go's own crypto/tls, which has no post-handshake authentication, no PSKs and
four key-log secrets where OpenSSL writes five (`:ssl-go-tls`, 19 rows). Since 552 it runs over a vendored copy with them.
- **The copy**: go/arbace/pytls (import path `arbace/pytls`, package `tls`) and its internal packages: TamaGo's go1.27.1
  `src/crypto/tls` (https://github.com/usbarmory/tamago-go, tag go1.27.1, the GOROOT `seed/goroot.sha256` pins) with what it
  imports from inside GOROOT, which a package outside it cannot: `vendor/golang.org/x/crypto`'s cryptobyte (and asn1),
  chacha20poly1305, chacha20, internal/poly1305, internal/alias (the generic code, no assembly), `crypto/internal/fips140/tls12`
  and `tls13` rewritten over the public crypto/hmac and crypto/hkdf, `crypto/tls/internal/fips140tls` (never required), and
  stubs of crypto/internal/boring (off), internal/godebug (Go 1.27's defaults; $GODEBUG not read) and internal/cpu. AES-GCM is
  crypto/cipher's (the FIPS module's increasing-nonce check of TLS 1.2 is the one thing lost). Every change is listed in
  doc/python/pytls-vendor.md and shown by doc/python/pytls-vendor.diff (against the files as copied); upstream lines edited
  are marked `// arbace`, the features' code is in `arbace_keylog_alerts.go`, `arbace_pha.go`, `arbace_psk.go`, their Go tests
  in `arbace_features_test.go` (17, each feature both ways). Upstream's own tests, run on a scratch copy: 686 pass, 3 fail by
  the intended changes (the key log's fifth line, the new Config fields, the PSK suites' order rule), 22 skipped; the 162
  recorded handshakes replay byte for byte.
- **The features**: `Config.PostHandshakeAuth` (client: extension 49, the answer to a post-handshake CertificateRequest),
  `Config.PostHandshakeClientAuth` (server: SSL_VERIFY_POST_HANDSHAKE, no request in a TLS 1.3 handshake whose client offered
  PHA), `Conn.RequestClientCertificate` (SSL_verify_client_post_handshake: the request now, the client's answer verified in
  the server's next Read; its errors OpenSSL's WRONG_SSL_VERSION, EXTENSION_NOT_RECEIVED, STILL_IN_INIT); TLS 1.3 external
  PSKs (psk_dhe_ke, SHA-256, `DidResume`) and TLS 1.2's plain PSK suites (RFC 4279: PSK-AES256-GCM-SHA384,
  PSK-CHACHA20-POLY1305, PSK-AES128-GCM-SHA256, PSK-AES256-CBC-SHA, PSK-AES128-CBC-SHA) through `PSKClientCallback`,
  `PSKServerCallback` and `PSKIdentityHint`; `EXPORTER_SECRET` in the key log; an `AlertError` from the verification
  callbacks sent as that alert (OpenSSL's unknown_ca and the others of ssl_x509err2alert, in TLS 1.3 too, where the
  transport could not rewrite the encrypted alert).
- **_ssl** (builtins_ssl*.ae): `HAS_PHA`, `HAS_PSK` (and 3.15's `HAS_PSK_TLS13`) true; `set_psk_client_callback` and
  `set_psk_server_callback` as _ssl.c's (the callbacks called on the Python thread, their errors unraisable); the PSK suites
  in the cipher table (kPSK, aPSK, PSK), offered only with this side's callback; `verify_client_post_handshake`; the peer's
  certificates taken again after a read that completed a post-handshake authentication.
- **Proved** on the VM (AOT, from the committed forms): `bin/pyi check --aot --lib-test --fresh test_ssl test_httplib`: test_ssl
  184 pass, 22 classed, test_httplib 143 pass, 5 classed, 0 failing; the 19 rows pass, none the record passes fails; the
  development run of test_asyncio.test_ssl and test_sslproto: no regression. Recorded by `bin/pyi check --judge DIR --record`
  (the 19 rows alone changed in the record); `:ssl-go-tls` removed (both divergences files). Python eval: pass 40,552 ->
  40,571, design 1,398 -> 1,379.
- **The keys and the table**: pyi's runtime imports arbace/pytls, so it is listed in goi's table (arbace/go/table.ae's
  `program-path?`) and copied into the program with go/arbace/py (arbace/j2g/main.ae); `gate_key` moves (those namespaces
  and go/arbace/pytls), `arbace_runtime_key` does not (`arbace_runtime_not`); the gate's `:pyi` host lists it; bin/pyi prints
  it beside the runtime, builds it into the AOT module and reads it back (`runtime read`, `runtime read-pytls`).

## As built: 553, subinterpreters' handles, `_testbuffer`, 3.14's UTF-8 mode and the children that are pyi (2026-11-24, the Mac VM, queue 553; the user's decisions of 2026-11-23 on 537-5 and 537-6)

**Subinterpreters (PEP 734, `concurrent.interpreters`).** The public package is CPython 3.14.10's own
`Lib/concurrent/interpreters` (pure Python) over pyi's `_interpreters`, `_interpqueues` and `_interpchannels`, ported in queue 230
("Design: 230", "As built: 230") with each interpreter's own module table, builtins, sys, importlib bootstrap, codecs, atexit,
locale and the interpreter registers (`_csv`'s, `_asyncio`'s, monitoring's, the at-fork hooks) swapped by `interpSwitch`: an
`isolated` interpreter's module state is its own, as `Py_mod_multiple_interpreters` modules have it in CPython; what stays process
wide is immutable or identity bookkeeping (interned strings, small ints, static types, code objects) and the one GIL (230's
DECISION: an `own` GIL is reported as configuration, isolation without parallelism). This task closes what `:subinterpreters` kept:

- **The moment of a handle's death.** A stdlib interpreter (`_interpreters.create(reqrefs=True)`) ends when the last
  `Interpreter` handle's `__del__` drops its reference (`decref`), and its items in queues and channels are unbound then (UNBOUND,
  UNBOUND_ERROR's `ItemInterpreterDestroyed`, UNBOUND_REMOVE). CPython does it at `del interp` by reference counting; pyi's moment at
  del (queue 222) asks Go's collector at a deletion within a budget of a tenth of the wall time and 50 ms, and past the budget the
  death waited for the next collection. The rows' failures fit that: in test_interpreters' module runs the budget is spent
  by the time TestQueueOps and TestSendRecv run, and `del interp` left the interpreter alive, while a probe alone passes (the
  budget fresh; read, not measured on the main host). Now a deletion past the
  budget while a subinterpreter is held by handles is owed (`xiDeathOwed`), and the operations whose answer depends on an
  interpreter's being alive settle it first with one collection round (`xiSettleDeaths`: the queues' get, get_count, is_full,
  list_all, the channels' recv, get_count, get_info, list_all, list_interpreters). A program with no subinterpreter never owes one;
  the cost is one collection per owed deletion, at the next such operation.
- **`_testbuffer`** is ci's since queue 456 (Modules/_testbuffer.c in the C world, its memoryview over Py_buffer since 473):
  the three `:skipped-by-test` rows ("needs _testbuffer", "No module named '_testbuffer'") and test_buffer's 73 "ndarray object
  required" rows in `:testcapi` are the 11-19 records night's, before 473; test_capi's two test_number rows and test_ctypes'
  test_fortran_contiguous pass by id on the VM (their record rows move at the host's whole record, as test_buffer's).
- **3.14's UTF-8 mode rule** (Python/preconfig.c's preconfig_init_utf8_mode): `sys.flags.utf8_mode` is `-X utf8[=0|1]`, else
  `PYTHONUTF8` (0 or 1; -E and -I ignore it; another value a fatal error at start, as 3.15's already was), else 1 only under the C
  or POSIX locale (LC_ALL, LC_CTYPE, LANG; musl's default C.UTF-8). pyi kept 1 before; under the runner's `LC_ALL=C.UTF-8`
  it is now 0, as CPython's reference runs had it, so open()'s and TextIOWrapper's default encoding is the locale's
  (`locale.getencoding()`, "UTF-8") and the three tests that skip under UTF-8 mode run and pass. The standard streams' errors
  are unchanged (stdioConfig: 551's area); test_utf8_mode's test_stdio still wants surrogateescape under `-X utf8`.
- **The children that are pyi (`:child-is-pyi`, 537-5), case by case:**
  - test_posix's two test_close_file (posix_spawn with POSIX_SPAWN_CLOSE 0): Go's runtime opens /dev/null on a closed 0, 1 or 2
    at its start (runtime.checkfds), so the child's `os.fstat(0)` succeeded. `bin/pyi python` sees descriptor 0 closed before
    Go's runtime starts and says so (`PYI_STDIN_CLOSED`); the runtime closes 0 again before anything runs and `sys.stdin` is
    None (create_stdio's is_valid_fd).
  - test_subprocess' test_preexec_at_exit: `_posixsubprocess.fork_exec` raises subprocess_fork_exec_impl's refusals first: a
    `preexec_fn` at the interpreter's shutdown is PythonFinalizationError "preexec_fn not supported at interpreter shutdown",
    within a subinterpreter RuntimeError (then pyi's own refusal: no fork).
  - test_unicodedata's test_unicodedata_unload_reload: the namereplace handler imports unicodedata once per interpreter
    (`_PyUnicode_GetNameCAPI`'s PyCapsule_Import) and keeps it, so `del sys.modules['unicodedata']` finds it there.
    test_failed_import_during_compiling: compile(), exec() and eval() of a source holding `\N{` ask the same capsule
    (compileSourceNamed), as CPython's tokenizer does when it decodes the escape, and when unicodedata does not import a copy
    of the source whose every `\N{` opens a name no character has is compiled: a decoded escape fails there ("unknown Unicode
    character name"), a raw string's, a bytes literal's or a comment's text does not, and the failure is the parser's SyntaxError
    "(unicode error) \N escapes not supported (can't load unicodedata module)" at the escape's place (the compiler's host
    decodes the names it knows itself, so the runtime sees no escape otherwise; the names it leaves, the IR's `{:named}` parts,
    ask the capsule as the code loads under compile(), exec() and eval()). A module an import loads is compiled code, as a .pyc
    is for CPython, and asks nothing (a first version asked at every load: with `sys.modules['unicodedata'] = None` every
    module holding an escape then failed to load and the child grew past the runner's 8 GB cap). A source with `\N{` only in
    a raw string or a comment imports unicodedata where CPython would not (sys.modules gains it; nothing else changes).
  - test_marshal's test_unorderable_set_items_in_code: the bytecode view's add_const keyed constants by their repr, so the two
    NaN complex numbers `{(nan)*1j, (nan)*2j, 1.5}` folds became one; `_PyCode_ConstantKey` keys a float or complex with a NaN
    part by the object itself, so a NaN constant now matches only itself (`bcConstNaN`).
  - test_os' URandomFDTests (test_urandom_failure here, fd_closed and fd_reopened of `:runs-where-cpython-skips`): sysconfig's
    `HAVE_GETRANDOM`, `HAVE_GETRANDOM_SYSCALL` and `HAVE_GETENTROPY` are 1, as CPython's build on Alpine has them (os.urandom is
    Go's crypto/rand, the getrandom system call), so they skip as CPython's do.
  - test_subprocess' test_encoding_warning passes as it is (its row the records night's).
  - Left: test_pass_fds_redirected (Go's poller holds two descriptors, 3 and 4, in every process: n/a, DECISION 553-1) and
    test_os' BytesFwalkTests.test_fd_finalization (reference counting's moment, not a child: closing the wrapper generator
    drops the inner os.fwalk generator, which CPython closes at once: to `:finalization`, under 537-2, DECISION 553-2).

The eval cell: 40,827 / 1,404 / 1,308 before, 40,829 / 1,404 / 1,306 after (pass / design / deferred): only test_utf8_mode was
recorded from the VM (`bin/pyi check --judge DIR --record` over check B's run of it: test_env_var and test_xoption pass, out of
`:source-decoding`). The other rows above pass on the VM by id or in whole-module runs that a VM-only result keeps out of the
record (test_builtin's TestType.test_namespace_order fails on goi alone; test_posix's test_posix_spawnp and test_unicodedata's
failed_import pass with the runner's memory cap at 16 GB, check C, and crashed at 8 GB in the whole runs); they move at the main
host's whole record, test_buffer with task 554's. Proofs (the VM, arm64; stage 0 nx's, goi interpreting the checkout's py,
`PYI_RUNNER=python3`): `bin/pyi check --only` A (ids and whole modules), B (test_unicodedata, test_utf8_mode, test_builtin and
test_posix whole), C (the VM-only rows alone), the three negative controls failing as they must in B and C; probes of the owed
death (`del interp` after the budget is spent: qsize 4) and of the `\N` SyntaxError against python3 3.14.10's line.

## As built: 554, the C API group: the stale rows recorded, PyLong_FromString (2026-11-24, the Mac VM, queue 554)

The group's classes (`:capi-port` 244, `:testcapi` 95, `:extension-modules` 66, `:testcapi-behaviour` 10, `:extension-subinterp-check`
5, `:capi-refcounts` 3, `:capi-singletons` 2, `:capi-gc-tracked` 2) held mostly rows recorded before PyConfig_Get (test_compile's 160
`:capi-port` rows stopped at its import), before py-capi-1122's natives and before `_testbuffer` was built (test_buffer's 73 `:testcapi`
skips, "ndarray object required").

- **Recorded** (whole-module `bin/pyi check` on the VM, the checkout's runtime on goi, `--judge DIR --record`, controls failing as
  they must): test_compile (154 pass of 185), test_buffer (61 more pass), test_import, test_code, test_tracemalloc, test_call,
  test_bool, test_getopt: 216 rows to pass. Python eval 40,886 / 1,332 / 1,292 -> 41,102 / 1,319 / 1,070.
- **PyLong_FromString** (capi_more): ASCII digits only, a non-ASCII byte is "invalid literal for int() with base N" with the base
  a base-0 prefix gives (test_capi's test_long_fromstring).
- **Classes**: `:boolop-truth-calls` (new, deferred, py-core): test_compound's `__bool__` called twice in `a and b or c`;
  test_compile_ast, test_consts_in_conditionals and test_folding_type_param into `:bytecode` (n/a).
- **Left for the host's record**: test_capi whole (about 90 of the group's rows; the VM's goi run needs many hours) and
  test_importlib (the 52 `:extension-modules` rows of `_testsinglephase`/`_testmultiphase`, built since py-capi-1122 round 2).
- **Left for owners**: PyFunction_SetClosure of a tuple of non-cells (pyi's Function keeps []*Cell: py-core), type `__flags__`'
  Py_TPFLAGS_HAVE_VECTORCALL inheritance and loss on `__call__` (test_call's TestPEP590: py-core), PyContext_Enter's prev of a
  thread with no context yet (test_watchers' test_exit_base_context: py-core's contextvars), `sys._is_immortal` (test_bytes'
  test_resize), `_testcapi.W_STOPCODE` (test_subprocess: py-os).

## As built: 557, the AOT's deferred row and the eval classes nobody owned (2026-11-24, the Mac VM, queue 557)

The classes of `test/python/divergences.edn` by rows on next a6da0aadf's record (`bin/pyi divergences --table`, `--methods`), each
with its owner this round (the main host's py-core, py-os, py-design, py-accel and py-goi; the VM's 545, 550, 551b, 552, 553, 554
and 557b): the table is the report's. What nobody owned and pyi could fix:

- **The AOT's one deferred row, synth/datamodel/del.py, is stale.** The amd64 record has it `:as-pyi` (compiled, loop and pyi on
  goi alike, none the golden text). Since py-small-1122 (222's moment at a frame's return) the compiled run and the loop print the
  golden text (`bin/pyi check --aot --no-pyi --only synth/datamodel/del.py` on the VM: 1 pass), and the judge passes a program whose
  compiled and loop runs are the reference even when pyi's differs (`:pyi-differs`). pyi on goi still prints `after del` before
  `del 2` (`bin/pyi run`, the VM). The row is the main host's next whole `bin/pyi check --aot --record`; `:finalization-goi` of
  divergences-aot.edn goes after it, as its :decided says.
- **`:source-decoding`: test_source_encoding's nine rows.** Four causes. The main file of `bin/pyi run` was read as bytes with no
  new-line translation (`arbace.python`'s run-dump), so a triple-quoted CR LF kept its CR and a lone CR broke the `:full` tokens'
  positions; the bytes are now translated first (CR LF and CR to LF), as compile()'s source already was (astjson/read-forms). A
  coding spec's charset that decodes leniently (Shift_JIS's illegal sequence) gave text: `native/source-text` now hands a source its
  charset does not decode strictly to Python's codec (the `:charset` path), and the runtime turns that codec's LookupError,
  ValueError or UnicodeDecodeError into the file tokenizer's SyntaxError (`fileInitError`, as `_PyTokenizer_raise_init_error`: the
  error's str, the file, line 0, offset -1; the lookup's codecs.decode() hint dropped, as fp_setreadl's). exec() of bytes: the
  cookie's name is normalized as get_normal_name (`iso-8859-1-xxx...` is iso-8859-1), and every cookie of the first two lines is
  made inert once the text is decoded (a second line's cookie, or a second cookie on the first line, was read again by the host's
  reader). The utf8_mode and c_locale rows are 553's; email's, quopri's and difflib's stay. The nine pass on the VM (a run of the
  whole module that reached the default 3600 s after 62 passes, the nine among them); the module's record is the main host's
  (its child processes take 2-3 min each on goi).
- **`:inlined-comprehensions` (2): `_PyCompile_TweakInlinedComprehensionScopes` outside class bodies.** A name whose scope inside an
  inlined comprehension differs from the enclosing block's (not FREE inside, not CELL inside and FREE outside) is now resolved the
  comprehension's way in any block, not only in a class body; a function's second comprehension read a global through the local
  slot its first comprehension had made (test_listcomps' test_multiple_comprehension_name_reuse, test_type_annotations'
  test_complex_comprehension_inlining_exec).
- **`:annotate-rest` (2).** A function's `__annotate__` and `__annotations__` went through funcattrs.go's fast path, which neither
  checked the value (`function___annotate___set_impl`: not deleted, callable or None, a callable dropping the cached annotations)
  nor the result (`__annotate__ returned non-dict`); a type's `__annotations__` set or deleted now pops `__annotate_func__` and the
  class body's own `__annotate__` (type_set_annotations, gh-132285).
- **`:compiler-errors` (1): co_flags' future bits.** The IR's code objects carry `:future-annotations` and `:future-barry-as-bdfl`
  when the module imports them (compute_code_flags: CO_FUTURE_ANNOTATIONS, CO_FUTURE_BARRY_AS_BDFL in every code object), so
  codeop's Compile learns a future import from co_flags (test_codeop's test_future_imports); code.replace(co_flags=...) keeps them.
- Records: test_listcomps, test_type_annotations and test_codeop written whole (5 methods now pass); the eval cell 40,827 / 1,404 /
  1,308 -> 40,832 / 1,405 / 1,302.
- **`:ref-empty-skip` (1) removed.** test_selectors' SelectSelectorTestCase.test_modify_unregister skips itself (`skipTest("")`) on
  every platform; judged by its own verdict (queue 234) the row is `:skipped-by-test`'s.
- Not taken: `:pickle-builtin-reference` (py-core's, train 5; pyi already pickles the builtins, `str.maketrans`,
  `bytearray.maketrans` and a class's `__new__` as CPython does, probed: those six rows look stale), `:compileall-pyc` and
  `:tools-misc` (557b), `:traceback-objects` (`:with-exit-traceback`'s cause, py-core's: `__exit__` gets no traceback),
  `:code-objects` (a code object's weakref callback at its deletion: finalization timing), `:docstrings` (py-core's built-in
  docstrings), `:compiled-flaky` (timing, the cell's record decides), `:audit-hooks`' two (test_open is 545's native _io,
  test_cantrace the tracer's suspension), `:buffer-exports`' bytearray rows (PYI_BYTEARRAY_EXPORTS's cost, py-core's).

## As built: py-limits-1125, the eval's `:runner-limits` and `:host-memory` rows by cause (2026-11-25, the main host)

The two classes are patterns over the record's rows (the runner's own words: a test past 300 s, idle past 90 s, a module past
its hour; the test's processes past 8192 MB), most of them written by the 11-19 records night, before py-eval-1122, py-mods-1122,
545's native _io and the compiler's speedups. Of the record's 85 `:runner-limits` rows and 79 `:host-memory` rows, 65 and 54
are multiprocessing's and concurrent.futures' process pools (spawn's test_processes past its module timeout, 61; its test_manager,
test_misc, test_multiprocessing_main_handling, test_concurrent_futures' process_pool, shutdown, deadlock: py-mp's lane), and 2 and
17 run ci's C world (test_buffer over `_testbuffer.c`, test_capi's two: the C API lane). The rest, measured alone (one test, `bin/pyi
python -m unittest`, the tree's resident set sampled every 2 s and capped at 2.6 GiB, on the gate build e0f40126 of main's runtime,
compiled; `.tmp/w/batch.sh` and `res1.tsv` in the worktree):

- **Pass alone now** (their rows move to a pass at the next record of their module): test_fstring's test_many_expressions (12-35 s,
  1.4 GB), test_longexp (38 s, 1.8 GB), test_modulefinder's test_extended_opargs (102 s, 1.9 GB), the three compile errors of
  `'-' * 100_000 + '1'` (test_code_module, test_pyrepl; test_pdb's moves with the Mac's 574): the parser's MAXSTACK MemoryError,
  each about 5 s; test_cmd_line's test_relativedir_bug46421 (30 s, 2.5 GB), test_email's test_get_param_linear_complexity
  (2 s: py-mods-1122's ASCII find), test_xml_etree(_c)'s test_deeply_nested_deepcopy (8 s, 3 s), test_uuid's
  test_unix_getnode_from_libuuid (12 s), test_urllib2's test_http_body_pipe (9 s), test_logging's
  test_should_not_rollover_named_pipe (0 s).
- **Slow, passes past the limit**: test_email's TestFeedParsers.test_long_lines, 674 s (1,311 s of CPU, 2.6 GB) on main's runtime,
  CPython 1.0 s: twenty thousand writes of 1,000 characters into feedparser's StringIO, then one 20 MB readlines; main's StringIO is
  _pyio's, train 5's is 545's native one (measured there by `res2.tsv`, below).
- **A hang, finalization's**: test_httpservers' RequestHandlerLoggingTestCase.test_get. The HTTP/1.1 response has no length, so
  http.client marks it will_close and closes the connection, but the response's makefile keeps the socket open until the response
  is deallocated: CPython's reference counting does that at the discarded getresponse(); under Go's collector the socket stays
  open, the single-threaded HTTPServer waits in the keep-alive's next readline, and tearDown's shutdown() never returns (killed at
  400 s, idle). test_err passes because send_error closes the connection. Proposed for `:finalization` (n/a, the user's 2026-11-23
  decision on that class); not moved here.
- **Need the train's runtime** (main's runtime is _pyio's and lacks py-os's interruptible send): test_io's seven
  C/PySignalsTest interrupted and reentrant writes, test_wsgiref's test_interrupted_write (hangs on main's runtime: py-os-1124's
  fix), test_compile's test_compiles_to_extended_op_arg (its import of _testcapi needs train 5's pyctypes), and the two that pass
  2.6 GiB: test_regrtest's ProgramsTestCase.test_module_test (`-j4`: six pyi processes at once; its four siblings pass) and
  test___all__'s test_all (900 s cap already in lib-test-limits.tsv; 2.95 GB at the kill). Queued under the big lock, 8 GiB, cap 7
  GiB: `.tmp/w/list2.txt`, results `.tmp/w/res2.tsv`.

Built:
- **str.find, rfind, index and rindex on a non-ASCII str** (strmethods.go's runeIndex): the range was encoded whole at every call,
  so `find(';', i)` in a loop was quadratic (40,000 calls on a 320 KB str: 25 s compiled, CPython 0.01 s). A one-code-point
  needle is scanned directly; a longer one in windows doubling from the end the search starts at (256 code points first), so a
  match costs its distance. Checked: 200,000 random cases against a naive search in Go, and 3,000 Python cases (find, rfind with
  bounds, count) equal to CPython's digest; 10,000 and 40,000 calls on goi's interpreted runtime 0.5 s and 2.4 s (linear).
- **A pyi child at GOGC 25** (tools/pyi/python3, `PYI_CHILD_GOGC`): every sys.executable child (multiprocessing's spawn, script_helper,
  subprocess) was at the parent's GOGC 100. Measured: an idle child's heap 611 MB at 100, 465 at 50, 397 at 25 (the binary's 390 MB
  of file pages beside it, shared between processes, half in each one's PSS when two run); importing test._test_multiprocessing
  peaks at 2.0 GB at 100, 1.75 at 50, 1.42-1.50 at 25, wall 33-40 s against 34 s, CPU about 30% more. The child's os.environ is the
  entry environment, captured before the assignment. The live heap after startup is about 330 MB: the rest of the footprint is
  Arbace's (a bare executable 177 MB of heap) and the host compiler's, not reducible by GOGC.

Caps proposed (the user's 2026-11-24 rule for OCaml, 1.5 x the CPU measured alone, as lib-test-limits.tsv's per-test rows): none yet
for the tests that pass alone within 300 s; test_email's test_long_lines once `res2.tsv` measures it on the train's runtime (on main's,
1.5 x 674 s wall would be 1,020 s; its row is better fixed than capped if 545's StringIO does not bring it near CPython).
