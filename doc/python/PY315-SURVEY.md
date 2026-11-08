# Python 3.15: the delta against 3.14.10, surveyed (queue 233, 2026-11-08, the Mac VM)

Continues `AFTER-PY11.md` §3 (which measured a first table and decided PYS26 (a), 2026-11-04: move after Python's tag, when
Alpine edge's python3 is 3.15). This survey diffs the two trees file by file and says what each change costs pyi. No code
changed. Tags: **run** (executed here), **probed** (a model or sample executed), **read** (from the sources alone).

## 0. The pin and the method

- **Fetched once** (the user's allowance, 2026-11-08): `https://www.python.org/ftp/python/3.15.2/Python-3.15.2.tar.xz` (the
  newest 3.15; python.org lists 3.15.0, 3.15.1, 3.15.2), into `.tmp/cache/python/Python-3.15.2.tar.xz`.
  - SHA-256 `8b5536d280142ecf6cdfe15d12a21ba1c65e300bbf7b28a8d0cdb849575797de`, 27,960,396 bytes.
  - Not yet in `test/python/corpora.edn` (its SHA-512 is to be computed on the move; run `sha512sum` on the cache file then).
  - 3.14.10 for the diff: SHA-256 `de414a57119abef52784d8993e341d53aed635fc2b05c2607d9f98a2c5b72032`.
- Both unpacked under `.tmp/q233/{a,b}`; `diff -ru`, Misc/NEWS and `Doc/whatsnew/3.15.rst` (2,936 lines) read. Line counts
  below are `diff | grep -c '^[<>]'` (they include whitespace and clinic noise: an order of magnitude, not a size).
- **Alpine edge** (run, 2026-11-08, APKINDEX of main, community, testing, aarch64): still `python3-3.14.10-r1`, no
  `python3.15`. PYS26 (a)'s trigger has not fired.
- Nothing of 3.15 was run (no build of it here); every claim about 3.15's behaviour is **read** unless tagged otherwise.

## 1. Findings

### 1.1 The stdlib itself needs the new language (probed, decisive)

The 3.15 `Lib/` is not 3.14 plus additions: it is written in the new syntax and with the new built-ins, so pyi cannot run
3.15's `Lib` (it takes its stdlib from the pinned tarball) before the reader, the compiler and the runtime all move.

- **run**: `grep` of 3.15.2's `Lib/` less `Lib/test`: 40 `lazy import`/`lazy from` statements in 28 files, among them
  `site.py`, `typing.py`, `collections/__init__.py`, `argparse.py`, `dataclasses.py`, `contextlib.py`, `inspect.py`,
  `traceback.py`, `tokenize.py`, `pathlib/__init__.py`; 35 files of `Lib/test` too.
- **probed**: pyi's native reader (`bin/repr-python read --reader native`, 3.14's grammar) on 528 distinct file names
  of 3.15.2's `Lib` (633 files less idlelib, lib2to3 and `test/`; the output names collapse same-named files, 2 min 11 s):
  507 read, **21 rejected**: `_colorize`, `_layout`, `argparse`, `collector`, `contextlib`, `cookies`, `dataclasses`, `difflib`, `doctest`, `inspect`,
  `pickletools`, `pkgutil`, `pstats_collector`, `runner`, `sample`, `site`, `tokenize`, `traceback`, `typing`, `utils`,
  `webbrowser` (all `lazy`). Log: `.tmp/q233/lib-read.log`.
- **read**: `frozendict` is used by 20+ stdlib files (`copy`, `json`, `_collections_abc`, `opcode`, `_opcode_metadata`,
  `functools`...), `sentinel(...)` by `functools`, `typing`, `dataclasses`, `urllib.parse`, `multiprocessing.process`: both built-ins, a
  NameError at import without them. `_opcode_metadata.py` builds `_specializations` as a `frozendict`.
- **Consequence**: there is no half move, and no "keep 3.14's Lib with a 3.15 runtime" (the Lib/test suite and the oracle are
  3.15's too). The first milestone of a move is "3.15's `functools`, `typing`, `site`, `collections` import", which needs P2,
  P3 and the first of P4 below.

### 1.2 Grammar and tokenizer (read; the diff of `Grammar/python.gram` is 378 lines)

| Change | Where | Effect on repr/python |
|---|---|---|
| `lazy` soft keyword before `import` and `from` (PEP 810); `import_stmt` becomes memoized; `&('import'\|'from'\|"lazy")` in `simple_stmt` | gram | regenerate `parser.ae`; AST `Import(names, is_lazy)`, `ImportFrom(module, names, level, is_lazy)` (ASDL, run: the two `int? is_lazy`) |
| unpacking in comprehensions (PEP 798): `[*x for ...]`, `{*x for ...}`, `(*x for ...)`, `{**d for ...}`; `DictComp` value becomes optional (`expr? value`, NULL for `**`) | gram, ASDL | parser, `convert.ae`, `print.ae`, `astjson.ae`, `doc/python/SPEC.md`; old forms stay readable |
| `star_named_expressions_sequence` and `invalid_starred_expression_unpacking_sequence` in list/tuple/set displays; the display alternatives are reordered (`genexp` before `tuple`, `listcomp` before `list`, `dictcomp`/`setcomp` before `dict`/`set`) | gram | ordering matters for the generated parser's memo and error behaviour, so regenerate, never patch |
| unary `+` accepted in `match` literal patterns (`'+' NUMBER`, `'+' real_number`) | gram | a `UnaryOp(UAdd)` in a pattern: reader, compiler's `match` (constant-folded?), printer |
| `if_expression` rule extracted; `invalid_if_expression`, `invalid_raise_stmt`, `invalid_assert_stmt`, `invalid_import_from`, `invalid_mapping_pattern`, `invalid_kvpair_unpacking`, "with trailing comma", "case statement must be inside match statement" | gram | new SyntaxError messages: `test/python/errors/`, `syntax_cases.py`, the PEG error pass (`peg.ae`) |
| `raise a from b` split into two alternatives (`'raise' a=expression 'from' b=expression`, then `invalid_raise_stmt`, then the bare one) | gram | same AST |
| messages reworded: "argument" to "parameter" ("named parameters must follow bare *", "var-positional parameter cannot have default value", "* may appear only once", "at least one parameter must precede /", "parameters cannot follow var-keyword parameter"; symtable's "duplicate parameter"); `dict unpacking cannot be used in dict comprehension` and `iterable unpacking cannot be used in comprehension` removed | gram, symtable | the divergence records keyed on these messages (`divergences-read.edn`, `test/python/errors/`) |
| missing-comma error computed in a C function (`_PyPegen_raise_error_for_missing_comma`); `_PyPegen_checked_from_import` (rejects `lazy from __future__`, warns on `from . lazy import x`) | `action_helpers.c`, `pegen.h` | `peg.ae`'s helpers; a SyntaxWarning, which needs the new `module` parameter below |
| `compile()`/`ast.parse`/`symtable`/`source_to_code` take `module=` (warning filters by module name), threaded through `_PyParser_ASTFromString`, the tokenizer (`tok->module`) and string parsing | pegen, peg_api, string_parser, symtable | reader's entry points, `compile` builtin, `ast.parse`, SyntaxWarning plumbing |
| bytes concatenation (`b"a" b"b"`) rewritten with `memcpy` (no semantics change) | action_helpers | none |
| f-string error: `f-string: expecting '}' to close '{' on line N` when the brace opened on another line | `Parser/lexer/lexer.c` (10 lines) | `lex.ae`; one message |
| `Grammar/Tokens`: unchanged (78 lines); no new token | | none |

Scale (read): the grammar delta is 164 changed lines of 1,653; the lexer changes 10 lines. The reader is a small job; the large
part is the new error messages, which are judged by the syntax-error corpora.

### 1.3 Symbol table and compiler (read)

- **symtable.c** (112 lines): `ste_in_try_block` and `ste_function_name` (new fields); the lazy-import context checks (SyntaxError "lazy import
  not allowed inside try/except blocks", "...functions", "...classes", "lazy from ... import * is not allowed"); comprehension scope names now
  `_Py_STR(anon_*)` (`<listcomp>` etc. are unchanged text, an internal change); `_Py_IsPrivateName` exported. Port into `scope.ae`.
- **codegen.c** (476 lines), the changes that alter emitted code:
  1. **Lazy imports**: `IMPORT_NAME`'s oparg carries two flag bits (`(name<<2)|1` lazy, `|2` forced eager inside `try/except`, in a
     function/class, and for `import *`); "lazy imports only allowed in module scope", "cannot lazy import *".
  2. **Starred elements in comprehensions** (PEP 798): generator expression yields from an inner loop (`GET_ITER; FOR_ITER; YIELD; POP_TOP; JUMP; END_FOR; POP_ITER`),
     list: `LIST_EXTEND`, set: `SET_UPDATE`, dict: `DICT_UPDATE` (value NULL).
  3. **Generator prefix**: `RETURN_GENERATOR; POP_TOP` is emitted in the code object's start (3.14 added it later, in `flowgraph`'s prologue); the exception-table
     handler wraps after it. Magic 3660.
  4. **A NULL "index" slot under the iterator** in every `for` (`PUSH_NULL` before the loop; `LIST_APPEND`/`SET_ADD` depth 3 instead of 2; `GET_ITER` has an oparg;
     `COMPILE_FBLOCK_ASYNC_FOR_LOOP` is a new fblock kind with its own unwinding). `GET_YIELD_FROM_ITER` merged into `GET_ITER` (3663), `SEND` reshaped, `END_SEND` pops 3.
  5. **`list(<genexpr>)` and `set(<genexpr>)` inlined** as `tuple`, `all`, `any` already are (`CONSTANT_BUILTIN_LIST`, `CONSTANT_BUILTIN_SET`).
  6. **`LOAD_COMMON_CONSTANT`** also loads `None`, `True`, `False`, `""`, `-1` (made in flowgraph's constant folding; replaces `LOAD_CONST_IMMORTAL/MORTAL`).
  7. Annotations: positional-only annotations are now emitted before the ordinary arguments' (the `__annotate__` dict's key order changes: a bug fix of 3.14's order); `__annotate__`'s `__qualname__` fixed (3664).
  8. `ADDOP_LOAD_CONST_NEW` and a few reference leaks: no semantics.
- **flowgraph.c** (224 lines): `i_except` is passed and kept through block and instruction creation (exception-region propagation, in the stack-depth pass); `RESUME_AT_GEN_EXPR_START`;
  common-constant folding; `END_SEND`'s refs; the CFG dump shows depth. **assemble.c**: 3 lines. **compile.c**: 74 lines (the `module` parameter, `_PyCompile_InExceptionHandler` use).
- **Opcodes** (run, `Include/opcode_ids.h`): 236 to 243 names; every opcode number below 43 shifts or moves (BINARY_OP 44 to 42, CALL 52 to 50, ...). Added: `BINARY_OP_SUBSCR_USTR_INT`,
  `CALL_EX_NON_PY_GENERAL`, `CALL_EX_PY`, `FOR_ITER_VIRTUAL`, `GET_ITER_SELF`, `GET_ITER_VIRTUAL`, `RESUME_CHECK_JIT`, `SEND_ASYNC_GEN`, `SEND_VIRTUAL`, `TRACE_RECORD`
  (specialization and JIT names); removed: `GET_YIELD_FROM_ITER`, `LOAD_CONST_IMMORTAL`, `LOAD_CONST_MORTAL`. Intrinsics: unchanged (11 + 4). `PYC_MAGIC_NUMBER` 3627 to 3666 (a4-a8 and b1 bumps: 3659 CALL_FUNCTION_EX
  specialization, 3660 generator preamble, 3661 lazy IMPORT_NAME, 3662 RESUME counter, 3663 GET_ITER merge, 3664 `__qualname__`, 3665 FOR_ITER_VIRTUAL, 3666 SEND_VIRTUAL).
- What pyi's compiler needs: items 1, 2 (and the scope checks) are language changes (compile.ae must accept and run them); 3 to 7 only change what `dis` shows (pyi's IR is its own, PYS7), so they bind 229's view and not the compiler.
  Annotation order (7) is visible at run time (`__annotate__`'s dict).

### 1.4 Runtime: the language-visible changes (read; `Doc/whatsnew/3.15.rst`)

| Item | Where in CPython | What pyi needs |
|---|---|---|
| **Lazy imports** (PEP 810): `Objects/lazyimportobject.c`, `Python/import.c` (974 lines), `importlib/_bootstrap*.py`, `sys.set_lazy_imports`, `get_lazy_imports`, `set/get_lazy_imports_filter`, `-X lazy_imports`, `PYTHON_LAZY_IMPORTS`; module-globals lookup resolves a lazy object on first use (LOAD_NAME/LOAD_GLOBAL/`module.__dict__`/`getattr`, `globals()` keeps the lazy object) | a new object type (`LazyImport`), reification at every global read point, `import.ae`/`imp_bootstrap.ae`, the dict/module-attribute paths, `pickle`/`copy` of modules, the `__lazy_modules__` global | the largest single item of the runtime: touches the hot path of LOAD_GLOBAL in the eval loop and in the AOT (`aot.ae`) |
| **`frozendict`** (PEP 814): a built-in, hashable, `dict`-like; `Objects/dictobject.c` (1,690 lines), `copy`, `pickle`, `json`, `pprint`, `typing`, `types.MappingProxyType`... | a type beside `dict` in `go/arbace/py/dict.ae`, its repr (`frozendict({...})`), `|` and `==` with dict, hash, `copy`/`pickle`/`marshal` (a new marshal type?) |
| **`sentinel`** (PEP 661): `Objects/sentinelobject.c`, built-in `sentinel(name)`, repr `<name>`, never copied, `typing`/`functools` use it | a small type (`unicode`-free) and its pickle/copy rules |
| **UTF-8 by default** (PEP 686): `open()`, `locale.getencoding` untouched; `-X utf8=0` restores | pyi's `io.open` default, `sys.flags.utf8_mode`, `locale` and the c-locale probe (`c-locale.py`); the `sys.stdout` encoding on a C locale |
| `slice[...]` generic; `memoryview` `'Zf'`/`'Zd'`; `bytes.replace(count=)`; `bytearray.take_bytes`; `__dict__`/`__weakref__` slots for any class; `__slots__` for `tuple` subclasses; `ImportError.__repr__` with `name=`/`path=` | `Objects/*` | small each; `take_bytes` and slots on tuple need the type machinery (`class.ae`, `descr.ae`) |
| import locks acquired in hierarchical order (3.15.2 fixes a regression of it); `__cached__` no longer set or used; `-W` takes `/regex/` fields; timestamp/timeout args accept any real number; `Loader.load_module()` removed | `import.c`, `_bootstrap` | `import.ae` and the warning filters |
| `ast` node constructors raise `TypeError` instead of a DeprecationWarning for missing/unknown arguments | `Python/Python-ast.c` | `_ast` table (generated); ast-objects class |
| removed: `sre_compile`, `sre_constants`, `sre_parse`, `glob.glob0/1`, `http.server.CGIHTTPRequestHandler`, `platform.java_ver`, `pathlib.PurePath.is_reserved`, `types.CodeType.co_lnotab`, `ctypes.SetPointerType`, `datetime.strptime` `%d` without year, `typing.NamedTuple` keyword syntax | | tests of the removed names go; pyi's `co_lnotab` if exposed |
| new: `math.integer` (`Modules/mathintegermodule.c`, 1,297 lines: `gcd`, `lcm`, `isqrt`, `factorial`, `perm`, `comb`; `math` keeps its names), `profiling` (Tachyon sampler `profiling.sampling` needs `_remote_debugging`; `profiling.tracing` is `cProfile`), `_pyrepl` split in 4, `importlib.metadata` +2, `xml.utils`, `sqlite3._completer` | `math_extra.ae`/`math_dunders.ae` (the module's code is the integer half of `math`: a move plus `_math_integer`); `profiling.tracing` rests on `_lsprof` (220 owns it); the sampler is by design out (a debugger of another process) |
| PEP 829 startup configuration files (`site.py`, 983 lines of test_site), PEP 831 frame pointers (build flag), PEP 803 stable ABI for free-threaded builds, PEP 788 (C API) | | `site` runs as the tarball's, so pyi follows it; the others do not bind (no C API, no builds) |
| Unicode 16.0.0 to 17.0.0; `unicodedata` 641 lines | `unicodedata_table.py`; `unicodedata_db.h` | regenerated table, `test_unicodedata`'s 543 lines of delta, `ucn.ae` (`\N{}` names: new names) |

### 1.5 C modules that changed (read; `diff -rq Modules`: 221 files, most of them clinic or `_testcapi`)

- **Added**: `mathintegermodule.c`, `_remote_debugging/` (the sampler's reader of another process: out), `xxlimited_3_13.c`, `_ssl_data_40.h` (replacing `_ssl_data_35.h`).
- **Large deltas in glue modules pyi owns** (lines changed; whether it binds is an unread-in-depth call): `_decimal` 3,002, `binascii` 1,788 (new base64 variants; `padded=`), `_hashopenssl` 1,588, `math` 1,444
  (the integer half leaves), `posixmodule` 1,180, `_datetime` 1,162, `_ssl` 1,031 (a certificate denylist, 3.15.2), `_struct` 787, `unicodedata` 641, `_thread` 607, `_json` 560,
  `array` 539, `_pickle` 503, `faulthandler` 466 (220 owns), `socket` 440, `zlib` 390 (`ctypes` 267, `_functools` 233, `mmap` 216, `pyexpat` 184, `_sqlite` cursor 139 and connection 80, `lzma` 132, `select` 130,
  `_asyncio` 121, `bz2` 116, `_io` bufferedio 113 and fileio 86, `_sre` 105, `itertools` 95, `_lsprof` 57, `_io` textio 39, `stringio` 28, `cjkcodecs` 21).
  - The libraries these wrap are the system's (expat, libmpdec, sqlite, xz, zstd, openssl) and pyi's own pure-Go ones; a delta in CPython's glue binds pyi where it is **behaviour** the tests
    exercise: `sqlite3.connect` keyword-only parameters and positional-only `create_function`, `resource.RLIM_INFINITY` positive, `base64.urlsafe_b64decode` unpadded, `mmap.resize` absent,
    `ElementTree.iterparse` ResourceWarning, `argparse` `dest` inference.
- **Core objects** (Objects/, Python/): `bytecodes.c` 3,622, `unicodeobject.c` 2,795 (the formatter split into `unicode_format*.c`, `unicode_writer.c`: refactors), `typeobject.c` 2,057 (new `Objects/slots.toml` generating
  type slots), `dictobject.c` 1,690, `ceval.c` 1,572, `bytesobject.c` 1,225, `import.c` 974, `specialize.c` 963, `genobject.c` 754 (virtual iteration), `float` 658, `initconfig` 588, `sysmodule` 402.
  The JIT (`jit_*`, `ceval.h` 3.15's tracing recorder, `RESUME_CHECK_JIT`, `TRACE_RECORD`) is CPython-internal and by design not pyi's.

### 1.6 Lib/test's delta (run on the two trees)

| | 3.14.10 | 3.15.2 | |
|---|---:|---:|---|
| files under `Lib/test` | 1,666 | 1,819 | 169 added, 16 removed (8 modules moved into packages) |
| `test_*` top-level entries | 434 | 434 | new: `test_lazy_import` (dir, 167 `test` defs), `test_profiling` (dir), `test_math_integer`, `test_xml`, `test_gc_stats`, `test_pystats`, `test_c_stack_unwind`, `test_samply_profiler`; `audit_test_data`; `test_io` and `test_os` are now **packages** |
| `def test*` (a grep) | 31,909 | 34,392 | +2,483 (+7.8%) |
| test modules that changed | | 371 of the common | the largest: `test_capi/test_opt` 4,679 lines (JIT), `test_external_inspection` 4,657, `test_generated_cases` 1,261, `test_binascii` 1,181, `test_site` 983, `test_pprint` 951, `test_httpservers` 803, `test_traceback` 766, `test_threaded_import` 703, `test_typing` 654, `test_argparse` 636, `test_dis` 611, `test_syntax` 490, `test_math` 477, `test_descr` 468, `test_ssl` 445, `test_unpack_ex` 400 |

- **Moved, so the records' ids change** (run, on the references): `test_io` (675 rows of `ref-cpython314-lib-test-all.edn`), `test_os` (385 in `ref-cpython314-lib-test.edn`),
  `test_posix` (180 in both), `test_file` 36, `test_fileio` 100, `test_largefile` 16, `test_memoryio` 188, `test_univnewlines` 34, `test_bufio` 4, `test_cprofile` 27 (now `test_profiling`): about
  1,645 rows change id (`test_io` becomes `test_io/test.test_io.test_general....`) and `divergences.edn`'s `:tests` keys and `:match`es that name them (14 lines mention them). `libtest.py`'s
  module discovery and `lib-test-skip.tsv` / `lib-test-all-owners.tsv` follow `regrtest --list-tests`.
- `lib-test-all`'s 34,832 reference rows (439 of 538 modules) must be re-recorded in whole on the main host under CPython 3.15.2: every row's outcome and the 3.15 additions.

### 1.7 What each pin and record is bound to (read; AFTER-PY11 §3.3 holds, plus these)

- **229's bytecode view** (`go/arbace/py/bytecode*.ae`, 3,439 lines; ported `flowgraph.c`/`assemble.c` of 3.14.10; generated `bytecode_table.ae` from `pycore_opcode_metadata.h`): binds to 1.3's items 3-6 (not 1, 2 which are compile-side too)
  and to the renumbering. `tools/pyi/bytecode_table.py` and `opcode_table.py` regenerate the tables; the port's deltas are flowgraph.c's 224 lines and codegen.c's emission changes in `irPlan`
  (the abstract stack walk that recovers CPython's shape from pyi's IR: the generator prefix, the NULL slot of loops, `GET_ITER` oparg, `IMPORT_NAME` bits). At 3.14.10 the five-attribute
  equality was 59.9% (229's measure), so the route's tail is already open; a move adds to it. The specializing names (`adaptive=True`) stay `:specializing` by design.
- **The AOT** (`arbace/python/aot.ae`, 886 lines): binds only through the IR. New IR ops are needed for lazy imports (the lazy `IMPORT_NAME`), starred elements in comprehensions
  (the inner loop is expressible in existing ops, so none), and the global-read reification (the AOT's global fast path must check for a lazy object). 1 to 2 ops.
- **pyi's compiler** (`compile.ae`, 2,636 lines) and scope (`scope.ae`, 780): 1.3's items 1 and 2 and the checks; the rest is the bytecode view's.
- **Records**: `ref-cpython314.edn` (152 rows; the 49 programs), `ref-cpython314-lib-test.edn` (11,287), `-all` (34,832), `-asyncio`, `ref-costs*.edn`, `divergences*.edn` (198 classes in `divergences.edn`),
  `compile-known.edn`, `full-known.edn`, `parse-check-known.edn`, `lex-check-known.edn`, the syntax-error corpora, the essential lists and the AOT's results: all re-recorded or re-judged; the file names carry `314`.
- **The oracle**: the host's `python3` and 20 py3-* apks. When the 3.14 apk leaves edge's mirrors, PYS27 (a) (copies of the apks, decided) keeps 3.14.10's oracle; for 3.15 no apk exists yet (run: 2026-11-08).

## 2. Proposals, ranked

Costs in queue tasks (one task is about a working day of the agent, as in AFTER-PY11 §3.4); "unblocks" names the next step.

1. **P1 The 3.15.2 oracle built from the pinned tarball** (1 task; the VM, aarch64, `./configure && make` into `.tmp/oracle315/`, no install; the main host does x86_64). Unblocks: every record, the probes below being **run** instead of **read**
   (the grammar corpora, `syntax_cases.py`, `bytecode-check`, `_opcode` tables), without waiting for Alpine. Needs DECISION 1. Without it the work below is read-only against the sources.
2. **P2 The reader to 3.15** (2 tasks): regenerate `parser.ae` from the new grammar (`bin/repr-python pegen`); port the lexer's message, the action helpers (`checked_from_import`, missing comma, the `module` argument and the `SyntaxWarning` plumbing),
   the ASDL fields (`is_lazy`, optional `DictComp.value`) through `convert.ae`, `print.ae`, `astjson.ae`, `SPEC.md`, `asdl_table.py`; the syntax-error corpora and `lex-check`/`parse-check` over 3.15's Lib: **the 21 files above read, 507 already do**.
   Unblocks: every other part (3.15's `Lib` is reached by the tools); can run first, with the 3.14 runtime unchanged, because the old programs read the same (new fields default to 0, absent).
3. **P3 Scope and compiler for the new language** (2 tasks): `lazy` (the context errors; module-scope only; the eager forcing in try/except), starred comprehension elements and `**` in dict comprehensions, unary `+` patterns, annotation order, `module=` threading in `compile()`.
   Unblocks: `site`, `typing`, `collections`, `argparse`, `inspect` etc. executing. Needs P2.
4. **P4a Runtime: `sentinel`, `frozendict`, `math.integer`, the small type changes (`slice[]`, `take_bytes`, slots on tuple, `ImportError.__repr__`), the removed names, UTF-8 by default, Unicode 17 table** (3 tasks). Unblocks: 3.15's `functools`, `copy`, `json`, `opcode` importing; with P2, P3 and P4b the whole Lib.
5. **P4b Runtime: lazy imports** (2 tasks; the object, reification at global reads in the eval loop and the AOT, the `sys.*lazy*` API, `-X lazy_imports`, the import system's `module=`/filter protocol, hierarchical locks, `__cached__` removal). The risk is the AOT's global fast path and 219/220's trace hooks sharing those lines.
6. **P5 The bytecode view and tables to 3.15** (2 tasks): regenerate `bytecode_table`/`opcode_table`, port flowgraph.c's 224 lines, the emission deltas in `irPlan`; magic. Alternative: keep `dis` of 3.15 as `:dis` by design for a first stage (0 tasks) and do P5 after the Lib runs. Unblocks: test_dis/test_peepholer/test_compile.
7. **P6 The C-module behaviours the tests exercise** (3 tasks, measured by the tests: `binascii`/`base64`, `sqlite3`, `argparse`'s dest, `resource`, `mmap`, `_ssl`'s denylist, `_decimal` if it is pyi's own, `datetime`, `faulthandler`/`_lsprof` with 220): a first pass by the test deltas of the 371 changed modules; the main host's night run says which fail.
8. **P7 Records and tests** (2 tasks + the main host's nights): the id moves (`test_io`, `test_os` packages: libtest.py, lib-test-skip, owners, divergences keys), the 169 new files (`test_lazy_import` first: 167 tests of the feature), counts "before and after" per class, `ref-cpython315*.edn` recorded on the main host, `corpora.edn` (digests, measures), `PYTHON_VERSION`, `apks.sha256` (empty until Alpine moves, or the built oracle's id in `:host-ids`), `cache_tag` `arbace-315`, `sys.version_info`.
9. **P8 The AOT's new ops** (1 task, with P4b).

Totals: P1 1 + P2 2 + P3 2 + P4 5 + P5 2 + P6 3 + P7 2 + P8 1 = **18 tasks** (AFTER-PY11 §3.4 said 12 to 24 working days: this survey agrees, at the upper half, because the stdlib needs the language at once, 1.1).
The order that never leaves the tree broken: P2 (3.14 programs read the same), P4a's additions (new names only), P3 and P4b together as one switch (the Lib then imports), P5, P6, P7; P1 first if DECISION 1 says (a).

What is cheap and can be done earlier without moving the pin (candidate follow-ups, each under 1 task): P2 alone (reading 3.15 sources with a 3.14 oracle is wrong for the errors, so the comparison needs P1); the table generators parameterised by the tarball path.

## 3. DECISIONs

DECISION: Build CPython 3.15.2 from the pinned tarball as the oracle now (reopening PYS26 (a), 2026-11-04, whose trigger is Alpine edge's python3 = 3.15; edge still has 3.14.10-r1 on 2026-11-08)?
- (a) Yes, in `.tmp/oracle315/` (not installed, outside apk), used only for the 3.15 work; the 3.14.10 apk stays the oracle of the frozen records until the move lands (recommended): one task (about 20 to 40 minutes of `make` per architecture, the system's libraries linked, `sys.version` differs from Alpine's build line); the oracle's pin becomes the tarball's SHA-256 plus the compile flags in `tools/pydump/`; P2 onwards become **run**-checked; every reference stays "from the main host's build", as the records rule says.
- (b) No: wait for Alpine's python3 3.15 (PYS26 (a) as decided); the work meanwhile is the read-only part of P2 (regenerating the parser, which needs no oracle) and the design of P4b; unknown date, the records wait with it.
- (c) Both, the move being decided when Alpine moves, P1 only for P2's probes (a middle: one build kept for syntax-error comparison and `dis`).

DECISION: The scope of 3.15 that pyi leaves out by design (recorded as classes, each a `:design` entry with the test names)?
- (a) Out: the JIT and tracing recorder (`test_capi/test_opt`, `test_generated_cases`, `test_pystats`, `test_c_stack_unwind`, `test_samply_profiler`), `_remote_debugging` and `profiling.sampling` (`test_external_inspection`, `test_profiling/test_sampling_profiler`), PEP 803/788 and the C API, the specializing names of `dis` (`:specializing`, already decided) (recommended): about 2,000 test methods fewer to judge; `profiling.tracing` stays in (it is `cProfile`, 220's `_lsprof`).
- (b) The sampling profiler in, over pyi's own `sys._current_frames` rather than another process's memory: 3 or more tasks, a different design.

(The decision on the timing of the move itself, PYS26, stays the user's; this survey only adds that the move is one switch, 1.1, and costs about 18 tasks.)

## 4. The main host's commands (expected output)

None needed for this survey (no code, no record). When P1 lands: `bin/repr-python pins --check` with the 3.15.2 tarball's SHA-256 above should print the pins hold; the whole-Lib read probe of 1.1 is
`bin/repr-python read -o DIR --reader native $(find Python-3.15.2/Lib -name '*.py' -not -path '*/test/*' -not -path '*/idlelib/*' -not -path '*/lib2to3/*')` on the 3.14 reader: expected `528 dumps: 507 converted and printed, 21 rejected by CPython, 0 failed`.

## 5. Scratch and sources

`.tmp/q233/{a,b}` (the two unpacked trees), `.tmp/q233/d-gram.txt` (the grammar diff), `.tmp/q233/lib-read.log`, `.tmp/cache/python/Python-3.15.2.tar.xz`. Sources: `Grammar/python.gram`, `Parser/Python.asdl`, `Parser/action_helpers.c`, `Python/symtable.c`, `codegen.c`,
`flowgraph.c`, `Include/opcode_ids.h`, `Include/internal/pycore_magic_number.h`, `Doc/whatsnew/3.15.rst`, `Lib/test` of both tarballs (3.15.2 and 3.14.10, python.org).
