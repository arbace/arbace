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

## Decided (the user, 2026-11-08)

1. **(a): build CPython 3.15.2 now** from the pinned tarball (SHA-256 8b5536d2...797de, 27,960,396 bytes) as the 3.15
   oracle in `.tmp/oracle315/`, reopening PYS26 (a)'s wait for Alpine's 3.15: the move's checks become run, not read.
   The build is the first task of the 3.15 series (proposal 1), not done by queue 233.
2. **(a): out by design**: the JIT, the sampling profiler (`_remote_debugging`), the C API, and `dis`'s specializing
   names (as 229's `:specializing`); about 2,000 test methods leave the judged set; `profiling.tracing` stays in, as
   `cProfile`.

## As built: the 3.15.2 oracle (proposal 1, 2026-11-08, the Mac VM)

CPython 3.15.2 built from the pinned tarball (SHA-256 checked) in `.tmp/oracle315/src/Python-3.15.2/python`
(`./configure --prefix=.tmp/oracle315/install --without-ensurepip`, `make -j6`, 33 s on the VM; GCC 15.2.0,
linux-aarch64 musl): 116 modules checked, 37 built in, 76 shared, 0 failed on import. Present: the test modules
(`_testcapi`, `_testinternalcapi`, `_testmultiphase`, `_testbuffer`, `_ctypes_test`), `_ssl` and `_hashlib` (OpenSSL
3.5.9), `_lzma`, `readline`, `_zstd`, `_bz2`, `_sqlite3`, `_ctypes`. Missing: `_tkinter` (not needed) and `_decimal`
(3.15 no longer bundles libmpdec: Alpine's `mpdecimal-dev`, not installed; test_decimal's C half has no 3.15 reference
until it is). The host packages added for it, by the user's leave: `openssl-dev`, `xz-dev`, `readline-dev`.

It is a VM-local build in `.tmp/`, not committed: references it gives are VM-recorded, so under the records rule they
serve the 3.15 series' checks on the VM; the record itself is the main host's, from its own 3.15.2 build of the same pin.

## As built: 236, the reader for 3.15 (proposal P2, 2026-11-08, the Mac VM)

The reader reads Python 3.15 behind one switch, `REPR_PYTHON_VERSION` (3.14 the default, 3.15), and 3.14 reads as
before: the 3.14 checks' samples print the same text before and after (run, below).

- **The switch**: `arbace.repr.python.lex/*minor*` (14, or 15 when `REPR_PYTHON_VERSION=3.15`) selects the generated
  parser (`native/grammar`: `arbace.repr.python.parser` or `parser315`, the second loaded only when asked, by
  `requiring-resolve`), the ASDL tables astjson writes by, `_feature_version`'s current minor, and the lexer's 3.15
  message. `bin/repr-python` under `REPR_PYTHON_VERSION=3.15` takes the pin `:cpython-3.15.2` (new in
  `test/python/corpora.edn`: no corpus of its own; a corpus of `:cpython-3.14.10` is read at the same paths of 3.15.2's
  tree), the 3.15.2 oracle live (`REPR_PYTHON_ORACLE_315`, default the main checkout's `.tmp/oracle315/...`, first in
  PATH as `python3`; the records stay 3.14's: any other `REPR_PYTHON_ORACLE` is refused), the helper
  `tools/pydump/pydump315.py`, the known and results files `*-315.edn`, the work dirs `.tmp/repr-python/*-315`; its verbs
  `pins --fetch|--check`, `pegen`, `spec-check`, `read`, `print`, `roundtrip` (no `--record`), `lex-check`,
  `parse-check`, `ast-check`; the rest refuse.
- **parser315.ae**: `bin/repr-python pegen` under 3.15 (pegen.ae takes the namespace from the output's name and the
  release from `Include/patchlevel.h`; parser.ae regenerates byte for byte). Nothing in pegen's translator needed
  changing: `lazy ? 1 : 0`, the `memo` on `import_stmt`, the new `invalid_*` rules all translate as 3.14's did.
- **The helpers** (peg.ae, ported from 3.15.2's `action_helpers.c`): `checked-from-import` (`lazy from __future__`
  refused, barry_as_FLUFL's flag, `is_lazy`), `warn-relative-import-of-lazy` (the SyntaxWarning of `from . lazy import`,
  its error under an "error" filter as `_PyErr_RaiseSyntaxError` makes it: UTF-8 columns plus one, no text),
  `raise-error-for-missing-comma` (a multi-line left side pointed at on its last line). The lexer (lex.ae): `f-string:
  expecting '}' to close '{' on line N`. The `module=` argument of 3.15's compile() (warning filters by module) is not
  the reader's: P3, with compile().
- **The forms**: `is_lazy` is `:lazy true` (left out at 0; 3.14's trees have no such field), `DictComp` without a value
  `(dict-comp KEY nil [GEN...])`, a starred element of a comprehension `(starred x)` as its element: native.ae's writer,
  `tools/pydump/pydump315.py` (a subclass of pydump.py's Writer: pydump.py is unchanged, the 3.14 records are keyed by
  its digest), print.ae (`lazy import`, `lazy from`, `{**d for ...}`), convert.ae unchanged (generic). doc/python/SPEC.md
  §5.2, §8, §9, §13 (the fields marked `(3.15)`), `forms-3.15` blocks; `tools/pydump/spec_check.py` requires a marked
  field of 3.15's Python.asdl and leaves it out of 3.14's, and parses the `forms-3.15` examples on a 3.15 oracle only.
  `tools/pyi/asdl_table.py` names the tree's release in its header (3.14.10's table regenerates byte for byte; 3.15's
  differs by the three fields; pyi's committed table stays 3.14's until P3).

Counts against the 3.15.2 oracle (run, the VM; before: the base 54e77ba65's reader on the same references):

| corpus (3.15.2's) | files | before: equal / differ | after: equal / differ / refused |
|---|---:|---|---|
| stdlib (`Lib` less `Lib/test`, 758) | 758 | 731 / 27 | 758 / 0 / 0 |
| syntax-errors (syntax_cases.py over 3.15.2's Lib/test) | 935 | 846 / 87 (2 refused) | 933 / 0 / 2 (`:charset`) |
| lex-errors | 62 | 62 / 0 | 62 / 0 / 0 |
| samples | 6 | 6 / 0 | 6 / 0 / 0 |
| lib-test, a sample (35 files with lazy imports, 1 in 25, the grammar's tests) | 99 | 66 / 33 | 98 / 1 (`test_fstring.py`, the helper's, also on 3.14) / 0 |

The same corpora in mode `--full`, lex-check over stdlib (758 equal) and the lib-test sample (99 equal), the mutants,
`roundtrip` and `roundtrip --text` over the 30 lazy files (levels 1-4 all 30; level 5 29, `webbrowser.py`'s marshal
depends on what the helper's process compiled before it) and ast-check (the same 1 and 3 warning-prefix differences as
3.14's) were run; every negative control failed as it must. The known and results files of 3.15 are the main host's
to record (`REPR_PYTHON_VERSION=3.15 bin/repr-python parse-check --record ...`). Remaining for P3 and P7: the `module=`
parameter, the whole Lib/test, `test_fstring.py`'s debug text (pydump.py keeps a comment inside the expression's span,
3.14 and 3.15 alike), the level 5 helper determinism, and the embedding of parser315.ae (embedded with repr/python since
W2; loaded only under 3.15).

## As built: 237, the runtime's additions (proposal P4a, 2026-11-08, the Mac VM)

pyi's runtime follows Python 3.15 behind the reader's switch, `REPR_PYTHON_VERSION=3.15` (236's: one switch for the
reader and the runtime, so a program is read and run by one version); without it 3.14 runs as before.

- **The switch** (`go/arbace/py/version.ae`): the package variable `pyMinor` (14, or 15 under the switch: a variable, so
  every file's `init` sees it) and `py315()`; `since315` names the builtins 3.15 adds (`sentinel`, `frozendict`), left
  out of the builtins module under 3.14. `sys.version_info`, `cache_tag` and the standard library stay 3.14.10's (P7
  moves them with the pin). Under the switch `bin/pyi` still fetches the 3.14 pins (`env -u REPR_PYTHON_VERSION
  bin/repr-python pins --fetch`: before, a run under the switch in a fresh worktree extracted 3.15.2's tree and put a
  3.14 Lib that was not there on sys.path). The IR cache is keyed by the reader's version under 3.15 (`arbace.python.run`'s
  and `bin/pyi`'s compiler digest begin with `REPR_PYTHON_VERSION=3.15\0`; 3.14's keys unchanged): **run**, the base
  served a 3.14 run 3.15's answer its 3.15 probes had cached ("Generator expression must be parenthesized" for
  test_exceptions' test_encodings, 3.14's "invalid syntax"); the head does not.
- **Ported from 3.15.2's own source**, each `py315()`-gated:
  - `sentinel` (`sentinel.ae`; `Objects/sentinelobject.c`): `sentinel(name, /, *, repr=None)`, the caller's module
    (the running function's `__module__`, a body's globals' `__name__`), `__name__` read-only and `__module__`
    writable members, `__copy__`/`__deepcopy__` the object itself, `__reduce__` the name (pickled as a global of its
    module), `|` a union (3.15's `is_unionable`), not subclassable, `__flags__` immutable and GC.
  - `frozendict` (`frozendict.ae`; `Objects/dictobject.c`'s frozendict): a Go type of its own over a `Dict` filled once
    (no `*Dict` path of the runtime takes it, so nothing writes into one; dict's readers take it where 3.15's
    `PyAnyDict_Check` does): `frozendict_hash` (frozenset's mixing over `tuple_hash` of each pair, cached,
    RecursionError "while hashing a frozendict"), `frozendict_or`/`_PyDict_Or` (an exact empty operand gives the other;
    `dict | fd` a dict), `frozendict(fd)` the same object, `copy`, `fromkeys` (`_PyDict_FromKeys`: `cls()` then
    `cls(copy)` for a subclass; dict's `fromkeys` likewise when the constructor returns a frozendict), `__getnewargs__`
    (protocol 2 and up; below it CPython's refusal), "as a frozendict key", a subclass's `__missing__`, the dict views,
    `__class_getitem__`, mapping and class patterns; `type(name, bases, fd)`, `str.maketrans(fd)`, and `exec`/`eval`
    globals (read through it, stores refused by it, "cannot assign __builtins__ to frozendict globals").
  - `math.integer` (`builtins_math.ae`; `Modules/mathintegermodule.c`, `math_exec`'s end): `_math_integer` named
    `math.integer`, in `sys.modules` under both names and bound as `math.integer`; math's six integer functions are its
    (`__module__` and messages `math.integer.NAME()`).
  - The small changes: `slice[...]`; `bytearray.take_bytes(n=None, /)` (`bytearray_take_bytes_impl`); ImportError's
    repr with `name=`/`path=` (`ImportError_repr`, a keyword None shown); any `__slots__` on a tuple subclass and
    `__weakref__` on a variable-size base (`type_new_slots`; "arbitrary __slots__ not supported" for int and bytes);
    one shared `__dict__` and `__weakref__` descriptor for every class (`<attribute '__dict__'>`, `__objclass__` object).
  - UTF-8 mode by default (`sys.ae`; `preconfig_init_utf8_mode`): `sys.flags.utf8_mode` 1 unless `-X utf8=0` or
    `PYTHONUTF8=0` (which `-E` and `-I` ignore); another value is the fatal start error. 3.14's stays pyi's 1.
  - Unicode 17 (`builtins_unicodedata_table315.ae`, `builtins_unicodedata.ae`): `tools/pyi/unicodedata_table.py
    --suffix 315` over 3.15.2's generated headers (UCD 17.0.0; 3.15's three new record fields, the grapheme
    functions', left out; 3.14.10's table regenerates byte for byte), the tables chosen by version at start (`ucdSel`),
    3.15's `_getucname` (every derived prefix), `unicodedata.isxidstart` and `isxidcontinue`. `bin/pyi runtime tables`
    (and `runtime check` where the oracle runs live) judges the 3.15 table against 3.15.2's headers, then its negative
    control (3.14.10's table against them must differ: run, it does).
- **For both versions**: `math.factorial` past int64 now raises CPython's OverflowError (big's `MulRange` took
  `Int64()`'s wrap).

**Counts** (probed: each 3.15.2 test module copied over 3.14.10's Lib/test, run by `bin/pyi aot` of
test/python/libtest.py under the switch, 3.15's `mapping_tests.py` and `support/warnings_helper.py` beside it; the
3.15.2 oracle runs the same module in its own tree; own verdict: unittest's ok; base 440e5a380 with the switch set):

| module (3.15.2's) | oracle: methods / ok | base: ok (methods) | head: ok (methods) | head agrees with the oracle |
|---|---:|---:|---:|---:|
| test_math_integer | 19 / 19 | import fails (`math.integer`) | 17 (19) | 17 |
| test_dict | 145 / 145 | import fails (`frozendict`) | 131 (145) | 131 |
| test_builtin | 157 / 145 | import fails (`sentinel`) | 89 (142) | 97 |
| test_unicodedata | 73 / 66 | 45 (73) | 55 (73) | 62 |
| test_exceptions | 115 / 112 | 97 (115) | 99 (115) | 102 |
| test_descr | 168 / 167 | 142 (168) | 146 (168) | 147 |
| test_utf8_mode | 12 / 11 | 6 (12) | 8 (12) | 9 |
| test_slice, test_copy | 11 / 11, 84 / 84 | 9, 82 | 9, 82 | 9, 82 |

Every frozendict test of test_dict passes (FrozenDictTests, FrozenDictMappingTests), every sentinel test of
test_builtin but `test_sentinel_str_subclass_name_cycle` (gc.is_tracked, pyi's GC class), every math.integer test but the
two `cpython_only`; the non-passes left are the existing classes (cpython_only skips, cycle collection) and the 3.15
parts not ported (below). **3.14 unchanged** (run, the same 3.14.10 modules on base and head, no switch): test_builtin
81/132, test_dict 110/122, test_unicodedata 53/61, test_descr 143/163, test_str 126/139, test_math 83/89, test_tuple
35/39, test_ucn 16/18, test_copy 81/81, test_slice 9/11, test_unicode_identifiers 3/3 the same on both, method for method;
test_exceptions 96 to 97, the one difference the base's poisoned IR cache above. `make python-essential`: the same on both
(check --aot 21 programs: 20 pass, 1 classed). The feature probe (sentinel, frozendict, math.integer, the small changes,
UTF-8 mode, UCD 17, exec/eval: 64 lines) prints 3.15.2's text under the switch and 3.14.10's without it on the head, and
fails on the base.

Not done (P4a's rest): unicodedata's other 3.15 functions (`block`, `iter_graphemes`, `grapheme_cluster_break`,
`indic_conjunct_break`, `extended_pictographic`: their tables need the generator to keep the three record fields and
the block table; 8 methods of test_unicodedata), frozendict's place in 3.14's Lib (`collections.abc.Mapping.register`,
`copy`, `json`, `pprint`, `marshal`: 3.15's Lib does them, with the move), `memoryview`'s `Zf`/`Zd`, `bytes.replace(count=)`
as a keyword, the removed names (P4b/P7 with the Lib), the UTF-8 mode's stdio error handlers (test_utf8_mode's
test_stdio and test_cmd_line, a pyi gap of 3.14 too).

## As built: 238, the bytecode view and its tables (proposal P5, 2026-11-09, the Mac VM)

`dis` of Python 3.15 behind the switch of 236 and 237 (`REPR_PYTHON_VERSION=3.15`); under 3.14 the view, its tables and
its answers are as before (run, below). The view still names an instruction by 3.14's constants (`bytecode_table.go`);
the version decides the numbers it writes, the caches, the names and the stack effects (`bytecode315.go`: `bcPop`,
`bcPush`, `bcCacheN`, `bcOpName`, `bcOut`, `bcIn`).

- **The tables**, by the existing generators with a 3.15 variant (as 237's Unicode table): `tools/pyi/bytecode_table.py
  TOP315 OUT --suffix 315 --base TOP314` (`bytecode_table315.go`: `bcNum315`, `bcCaches315`, `bcNames315`,
  `bcPopped315`/`bcPushed315` from 3.15.2's `opcode_ids.h` and `pycore_opcode_metadata.h`; an opcode 3.14 has not,
  `TRACE_RECORD`, gets 1000 plus its number; `GET_YIELD_FROM_ITER` has none) and `tools/pyi/opcode_table.py OUT
  --suffix 315` run by the 3.15.2 oracle (`builtins_opcode_table315.go`); both 3.14 files regenerate byte for byte.
  `_opcode` under 3.15 answers from them (no `ENABLE_SPECIALIZATION_FT`; `stack_effect` takes 3.15's numbers).
- **flowgraph.c's changed lines** (3.15.2's own): `loads_const` with `LOAD_COMMON_CONSTANT`,
  `maybe_instr_make_load_common_const` at the end of `basicblock_optimize_load_const` and in `instr_make_load_const` (the
  folds), the common constants as values (`get_const_value`), `remove_redundant_nops_and_pairs` over them,
  `propagate_line_numbers` through an empty target of one predecessor, `optimize_load_fast`'s `GET_ITER` among the
  opcodes that consume no inputs, `END_SEND` popping 3, the fixed `PUSH_REF(i, ...)`, the fall-through of an empty
  block, no generator prefix in `insert_prefix_instructions`; assemble.c's `END_SEND_OFFSET` 6. The rest of the 224
  lines (the `except` argument threaded through `basicblock_addop`, the dump's depth, memsets, a message) change no
  output of the view.
- **codegen.c's emission deltas, recovered in `irPlan`/`bcEmit`**: `RETURN_GENERATOR; POP_TOP` before `RESUME`, outside
  every handler (`codegen_enter_scope`); a generator expression's `RESUME 4` (`RESUME_AT_GEN_EXPR_START`), `LOAD_FAST 0`,
  `GET_ITER 0` before them and its call without `GET_ITER` (`ITERATOR_ON_STACK`), the wrapper's depth over them; an
  inlined comprehension's `GET_ITER` after its hidden locals and container (`ITERABLE_ON_STACK`, in the cleanup's
  region); the NULL index over each loop's iterator (an IR slot marked `iter`): `SWAP`, `COPY` and the appends' depths
  one more per iterator they reach across, a pop of an iterator two `POP_TOP`s (`COMPILE_FBLOCK_FOR_LOOP`'s unwinding:
  `SWAP 3`); `GET_ITER 0` with an oparg, `GET_ITER 3` (2 in a coroutine) for `yield from` as `_PyCode_Quicken`'s
  `fixup_getiter` leaves co_code; `PUSH_NULL` before the `None` of an awaitable's `SEND`; `RESUME`'s depth bit 8;
  `IMPORT_NAME`'s name shifted by 2 and the eager bit 2 outside the module's scope, in a try or handler region
  (`_PyCompile_InExceptionHandler`) and for `import *`; `async for`'s `NOT_TAKEN` after the `__anext__`'s `END_SEND` and
  `END_ASYNC_FOR`'s target, the `SEND` it ends (both also 3.14's, where the view had no `async for`: it failed to build,
  left for 3.14, a low).
- **The magic number**: under 3.15 the IR digest takes the version, so the `.pyc` magic (`'a'` and a digest byte) and
  the code blob's header differ from 3.14's (run: `importlib.util.MAGIC_NUMBER` 61fb0d0a under 3.14, 61610d0a under
  3.15; `TestPycMagic315`), and a 3.14 `.pyc` or marshalled code does not load under 3.15. 3.14's is unchanged.
- **Checks**: `bin/pyi bytecode-check` under the switch compares with the 3.15.2 oracle (`REPR_PYTHON_ORACLE_315`);
  `bin/pyi runtime tables` (and `runtime check`) judge `bytecode_table.go`, `bytecode_table315.go` and
  `builtins_opcode_table315.go` against their generators, each 3.15 table with its negative control (3.14.10's headers,
  python3's `_opcode`, made into it must differ: run, both fail as they must). `PYI_BC_DEBUG=1` prints a view's panic.

**Counts** (run on the VM; the specializing names out by design, `:specializing`):

| sample | base 4f3be635e | head |
|---|---:|---:|
| 3.15.2's test_dis (3.15's `dis.py`, `opcode.py`, `_opcode_metadata.py` and test file over 3.14.10's Lib; the oracle 145 of 145) | 34 ok | 111 ok |
| 3.15.2's test_peepholer (the oracle 142 of 143) | 12 ok | 48 ok |
| bytecode-check, 40 stdlib files, 2,935 code objects, 3.15 against the 3.15.2 oracle: co_code / all five | 0 / 0 | 2,534 (86.3%) / 1,840 (62.7%) |
| the same, 3.14 against 3.14.10 | 2,469 / 1,799 | 2,469 / 1,799 |

test_dis's 34 left: 26 skips of the existing classes (`cpython_only`, `_testinternalcapi`, the specializing tests) and
4 tests twice (the file and string variants) that fail under 3.14 too: `test_disassemble_recursive` (3.15 inlines
`list(<genexpr>)` and `set(<genexpr>)` as 3.14 inlines `tuple`, `all` and `any`, which the view does not: 229's tail),
`test_disassemble_str`, `test_disassemble_try_finally`, `test_intrinsic_2`. 3.14 unchanged (run, base and head): 3.14.10's
test_dis 107, test_peepholer 47, test_compile 20 of the 65 that run before `test_big_dict_literal`'s 1,800 s cap, equal
method for method; the bytecode-check counts above equal; `make python-essential` as on the base.

Not done: the lazy `IMPORT_NAME` bit (1) waits for P3's `lazy` in the compiler; `list()`/`set()` of a generator
expression inlined (with 3.14's `tuple`/`all`/`any`, 229's tail); the exit blocks CPython 3.15 no longer inlines
because the iterator's NULL makes them 5 instructions (pyi's compiler's flow! inlines at the IR's 4: compile.ae, the
`return` inside a `for` after a chained comparison); a non-inlined comprehension's own `GET_ITER` (none in the samples);
the annotations' order (P3).

## Design: 239, the language switch (P3 and P4b as one, the user's PYS26, 2026-11-08)

The user (PYS26, 2026-11-08 evening): build P3 and P4b now, against the 3.15.2 oracle, behind the switch of 236-238
(`REPR_PYTHON_VERSION=3.15`); 3.14 stays the default and its records unchanged; the move of the default is decided later.
One switch: what the reader reads under 3.15 the compiler compiles and the runtime runs; under 3.14 no form, IR or
answer changes (every 3.15 branch is behind `lex/*minor*` in the compiler, `py315()` in the runtime).

- **P3, the compiler** (3.15.2's `symtable.c` and `codegen.c`, `compile.c`, `ast_preprocess.c` deltas, ported where
  pyi's port of 3.14.10 has the same code): `scope.ae` keeps `ste_in_try_block` and `check_lazy_import_context`'s three
  errors, `lazy from ... import *`, "duplicate parameter", the `<lambda>`/`<listcomp>`... block names (`_Py_STR(anon_*)`)
  and `ste_function_name` (a function's `__annotate__` is `f.__annotate__`); `compile.ae` gives `IMPORT_NAME` its flag
  (IR ARG `[NAME-INDEX FLAG]` under 3.15 only, 1 lazy, 2 forced eager by `_PyCompile_InExceptionHandler`, a non-module
  scope or `import *`, 0 left to the mode; 3.14's ARG stays the index, so 3.14's IR is byte for byte the same), the
  module-scope and `*` errors of codegen, a starred element of a list, set or generator comprehension
  (`LIST_EXTEND`/`SET_UPDATE` at the element's depth, a generator's inner loop of yields) and `**` in a dict
  comprehension (`DICT_UPDATE`), the positional-only annotations first, `+NUMBER` keys of mapping patterns folded
  (`fold_const_match_patterns`). No new IR operation: `ir/verify`, the loader's `decodeArg`, the AOT's static imports and
  the bytecode view (its `IMPORT_NAME` oparg is now the compiler's flag, not 238's recomputation) read the pair.
- **P4b, lazy imports** (3.15.2's `Objects/lazyimportobject.c`, `Python/import.c`'s lazy half, ceval.c's
  `_PyEval_LazyImportName`, `_PyEval_LazyImportFrom`, `_PyEval_LoadGlobalStackRef`'s and `_PyEval_LoadName`'s
  reification, `moduleobject.c`'s getattr, `sysmodule.c`'s API, `initconfig.c`'s option): a Go type `lazy_import`
  (`types.LazyImportType`) with CPython's fields (root: builtins, absolute name, fromlist; projection: source and
  attribute; the declaring code and lasti; active), `IMPORT_NAME`'s flag read by the one import operation the eval
  loop and the AOT share (the AOT runs `import-name` by `AotExec`, so one implementation), reification at the one place
  each load of a global or a name ends (the eval loop's `loadGlobalAt`/`loadNameGlobal`/the dict-or-global and
  dict-or-deref loads, and the AOT's `AotLoadGlobal(V)`/`AotLoadName(V)` helpers, which call the same functions), the
  module's `__getattribute__` (`_Py_module_getattro_impl`: a lazy value reified, pending submodules loaded),
  `sys.lazy_modules`, `sys.set_lazy_imports`/`get_lazy_imports` (`"normal"`, `"all"`), `sys.flags.lazy_imports`, the filter
  (`set_lazy_imports_filter(f)`, `f(importer, name, fromlist)`), `builtins.__lazy_import__`, `__lazy_modules__`,
  `-X lazy_imports=` and `PYTHON_LAZY_IMPORTS`, `ImportCycleError`. A lazy object exists only under 3.15 (only a lazy
  import makes one), so 3.14's paths pay one type test where a value is read, nothing else.
- **Judged** against the 3.15.2 oracle on its own tests (each module before and after), 3.14 by the same samples on
  base and head. Out of this task: P6-P8.

## As built: 239, the language switch (P3 and P4b, 2026-11-09, the Mac VM)

As designed above (Design: 239), behind `REPR_PYTHON_VERSION=3.15`; under 3.14 the IR, the answers and the samples are
as before (run, below).

- **P3** (`arbace/python/scope.ae`, `compile.ae`, `ir.ae`, `aot.ae`; ported from 3.15.2's `symtable.c`, `codegen.c`,
  `compile.c`, `ast_preprocess.c`): the lazy-import context errors (try/except, function, class, `lazy from ... import *`)
  at the statement, "duplicate parameter", the `<lambda>`, `<listcomp>`, `<setcomp>`, `<dictcomp>`, `<genexpr>` block
  names, `ste_function_name` (`f.__annotate__`, `C.m.__annotate__`), `IMPORT_NAME`'s flag as the IR's `[NAME FLAG]`
  (the view's oparg is now the compiler's: 238's recomputation `irEagerImport` removed), "lazy imports only allowed in
  module scope" and "cannot lazy import *", starred comprehension elements and `**` in dict comprehensions, the
  positional-only annotations first, `+NUMBER` mapping-pattern keys. compile()'s `module=` (keyword-only, str or None)
  goes to the warnings the compile emits (`warn_explicit(..., module=)`).
- **P4b** (`go/arbace/py/lazyimport.ae` and hooks in `import.ae`, `eval.ae`, `aot.ae`, `func.ae`, `sys.ae`, `registry.ae`,
  `builtins.ae`, `builtins_exec.ae`, `builtins_imp.ae`): the design's list, plus: the mode `all` is set after the
  start's imports (`init_interp_main`) and never applies while importlib installs (pyi installs it at the first import,
  CPython before any mode); hasattr's lookup and the fromlist's are CPython's `suppress=1` (a pending child then
  imported by its name); `_imp._set_lazy_attributes`; `_types` under 3.15 (Lib/types.py's fallback derivations and
  `LazyImportType`), so 3.14's and 3.15's `types.py` both take `from _types import *`; `__cached__` no longer set.
  The AOT runs `import-name` and `import-from` by `AotExec`, and its load helpers call the eval loop's, so the compiled
  and the interpreted paths share one implementation (run: the probes below by `bin/pyi aot`, the children of
  test_lazy_import by `bin/pyi run`).
- **Hierarchical import locks** (3.15.2's `_bootstrap._resolve_parents`) are Lib: pyi runs importlib's
  `_bootstrap.py` from the pinned Lib, so they come with 3.15's Lib (P7); nothing in the runtime.
- **An inlined comprehension's saved names** (`codegen_push_inlined_comprehension_locals`, found by test_unpack_ex's and
  test_named_expressions' walrus cases): under 3.15 a `DEF_NONLOCAL` name is not saved and a saved name that is not the
  comprehension's local (an assignment expression's target at module level, `GLOBAL_EXPLICIT` there) resolves by its
  scope; 3.14 keeps its old (wrong) answer, `y` unbound after `[(y := i), ...]` at module level, until the user decides.

**Counts** (run on the VM; 3.15.2's test modules over 3.14.10's Lib/test, `bin/pyi aot` of libtest.py under the switch,
as 237's; own verdict; base 413d54d06 with the switch set):

| module (3.15.2's) | oracle: ok / methods | base: ok (methods) | head: ok (methods) | head agrees |
|---|---:|---:|---:|---:|
| test_lazy_import | 165 / 166 | 2 (166) | 153 (166), 155 with the two fixed after the run | 152 |
| test_syntax | 109 / 109 | 39 (48) | 93 (109): the 16 others `cpython_only` | 93 |
| test_named_expressions | 74 / 74 | 59 (74) | 74 (74) | 74 |
| test_patma | 343 / 343 | 323 (328) | 338 (343) | 338 |
| test_unpack_ex (doctests) | 6 / 6 | 0 (1) | 5 (6) | 5 |
| test_listcomps | 65 / 65 | 64 (66) | 64 (65) | 64 |
| test_grammar, test_setcomps, test_dictcomps | 75, 2, 10 | 75, 2, 10 | 75, 2, 10 | all |
| test_type_annotations, test_genexps | 49, 1 | 43, 0 | 43, 0 | 43, 0 |

test_lazy_import's run (59 min: its 130 subprocess tests start `bin/pyi python`, interpreted) put the package where the
children find it (copied into the worktree's 3.14.10 Lib/test for the run and removed after: a probe's need until 3.15's
Lib is pyi's); its oracle skips `test_deeply_dotted_name` (the VM build's C stack segfaults there; pyi passes it). Its
non-passes: `test_retry_after_recursive_access` and `test_concurrent_reification_same_module_high_contention`, fixed
after the run (`IMPORT_FROM`'s suppressed lookup; `import_ensure_initialized`), each run alone on the head by the
oracle's script (pass; the committed forms before the fixes fail); the four `_testcapi` skips; the two `LazyImportDisTests`
(3.14.10's `dis.py` under the switch; with 3.15.2's beside them both pass); the three `LazyImportFinalizationTests` (a
`__del__` at the interpreter's end: pyi's finalization class); `test_add_lazy_to_exec_builtins_after_specialization` and
`test_cached_import_releases_builtins` (3.14 gaps, run: a function made by exec does not read the globals'
`__builtins__`; the interpreted runtime keeps an exec namespace's builtins alive, the compiled one does not).
The other modules' non-passes are 3.14's (the same methods fail under 3.14) or need 3.15's Lib (test_patma's two
runtime-checkable protocols: 3.15's typing; its jump-threading test: 3.15's dis), and test_unpack_ex's 3.15 message
for a `keys()` returning a non-iterable (P6).

**3.14 unchanged** (run, base and head, no switch): `make python-essential` the same lines (check --aot 21 programs: 20
pass, 1 classed); 3.14.10's test_grammar, test_listcomps, test_patma, test_import, test_importlib, test_types,
test_type_annotations, test_syntax, test_dis equal method for method; `PYI_SMOKE_ONLY='^(f-flags|g-math-int|f-intbytes|e-gen)'
bin/pyi runtime check` every package printed and read again the same, every table and control as before, 4 programs and
1 session equal.

Not done: compile()'s `module=` in `ast.parse` and `symtable` (their 3.15 Lib passes it), unicodedata's and the other P4a
rests, the `{**x}` 3.15 messages, the lazy-import parts of `pickle`/`copy` of modules and `import *` of placeholders (no
3.15.2 test reaches them), the 3.15 Lib itself (P7: hierarchical locks, `_find_and_load_lazy_submodule`); P6-P8.

## Design: 252, the move's next parts (P6, P7, P8 behind the switch, 2026-11-10, the Mac VM)

The brief of 252: P6, P7 and P8 in order, behind `REPR_PYTHON_VERSION=3.15`; 3.14 the default, its behaviour and records
unchanged; when 3.15 becomes the default and which records move with it are the user's (the DECISIONs below), so 252 builds
only what every answer to them shares, and nothing that changes 3.14.

- **The standard library moves with the switch** (P7's first part, taken first because P6's measure needs it; 1.1: no
  option keeps 3.14's Lib under the 3.15 language). Under 3.15 pyi runs 3.15.2's `Lib` and lib-test reads 3.15.2's
  `Lib/test` (`bin/pyi`'s `pytop`, one place for every verb), `sys.version`, `version_info`, `hexversion` and
  `implementation` are 3.15.2's, `cache_tag` `arbace-315` (pyi's `.pyc` files of the two versions apart),
  `stdlib_module_names` 3.15.2's (`Python/stdlib_module_names.h`: `_math_integer` and `profiling` in, the `sre_*` out),
  sysconfig's `VERSION` 3.15. 237-239 probed 3.15.2's test files copied over 3.14.10's `Lib/test`; from 252 a 3.15 test
  runs in its own tree, as the main host's night will run it.
- **P6, first pass: what 3.15.2's Lib asks of the C modules** (1.5's list, measured, not guessed): every top-level module
  of 3.15.2's Lib imported under pyi (the oracle imports all 165), the names Lib imports from the C modules that 3.14's did
  not (a scan of both trees' `from _x import y` and `_x.y`), then the changed modules' own 3.15.2 tests as samples, each
  delta ported from 3.15.2's C source behind `py315()`. The rest of 1.5's list (the deltas only the tests see) is
  measured by the main host's 3.15 night (P7), not guessed here.
- **P7's tooling**: lib-test's discovery from the tree's own `SPLITTESTDIRS` (`libregrtest/findtests.py`'s set: 3.15.2
  splits `test_io` and `test_os`, 1.6's id moves; 3.14.10's list unchanged), the check's universe keyed by the tree, and
  every record's file chosen by version (`ref-cpython315*.edn`, `universe-cpython315.edn`; `-315` for the version-less
  names: `ref-costs*-315.edn`, `divergences*-315.edn`, `check-lib-test-linux-amd64-315.edn`), 236's naming for
  repr-python's; none is recorded on the VM (the records rule), so under 3.15 they are absent until the main host's
  night.
- **P8**: 239 needed no new IR operation (`IMPORT_NAME`'s flag rides its ARG; the AOT runs `import-name`/`import-from` by
  `AotExec`, its global and name loads call the eval loop's `loadGlobalAt`/`loadNameGlobal`, which reify). 252 checks that
  this still holds on 251's and 216's paths (the inline caches of `loadGlobalAt0` return the dict's value, reified after;
  251's attribute fast path takes instances only, never modules) and proves it on the compiled path by the lazy probes,
  now over 3.15.2's own Lib.

DECISION: When does 3.15 become pyi's and repr/python's default?
- (a) After the milestone's tag (`arbace-reaps-4-crops-v1`), on a records night of the main host: it builds 3.15.2 from the pin (its own oracle, amd64, the compile flags recorded in `tools/pydump/`), records the 3.15 references with `REPR_PYTHON_VERSION=3.15` into the `*315*` files 252 names, and one reviewed commit flips the default (`version.go`'s `versionMinor`, `lex/*minor*`, bin/pyi's and bin/repr-python's checks) (recommended): the milestone's records stay 3.14's; the move is one commit with its records; cost: one night of 3.15 runs, a review of the counts by class.
- (b) When Alpine edge ships python3 3.15 (PYS26 (a)'s first trigger): the oracle an apk again, no build to pin; the date is unknown (edge still 3.14.10-r1).
- (c) Before the tag: the milestone carries 3.15; night 11's 3.14 records are redone; the tag's date moves.

DECISION: Which records move at the flip, and does 3.14 stay checked after it?
- (a) All of Python's move: the `*315*` files become the records; the 3.14 files and the `py315()`/`*minor*` branches leave main once a night shows the 3.15 records reproduce (3.14 kept at the tag); `REPR_PYTHON_VERSION=3.14` works until then (recommended): one record set and one night; cost: a task removing the 3.14 branches.
- (b) Both stay: 3.14 a second configuration with its own records, judged every night: Python's night time doubles (lib-test-all twice), the branches stay.

DECISION: The 3.15 classes: a file of their own or one file?
- (a) `divergences-315.edn` (and `-aot-315`), started at the flip from a copy of 3.14's and edited by the 3.15 night's results; 252's paths already name it (recommended): the classes move with their records, 3.14's file untouched until (a) of the previous DECISION retires it.
- (b) One `divergences.edn`, each class marked `:python "3.14"`/`"3.15"` where it applies to one only: one place to edit; cost: the judge learns the key, and 3.14's file changes now.

## As built: 252, the move's next parts (P6, P7, P8 behind the switch, 2026-11-10, the Mac VM)

As designed above (Design: 252), behind `REPR_PYTHON_VERSION=3.15`; under 3.14 no answer, record or IR changes (run,
below). The three DECISIONs of the design stay open for the user; nothing built depends on their answers but the 3.15
files' names, which no 3.14 path reads.

- **P7's first part, the standard library under the switch** (`bin/pyi`'s `pytop`, `stdlib_lib`, `tarball_test`,
  `corpus_modules`): 3.15.2's `Lib` on sys.path and its `Lib/test` for lib-test; `sys.version` `3.15.2 (pyi, Arbace)
  [Go]`, `version_info` `(3, 15, 2, 'final', 0)`, `hexversion` `0x030f02f0`, `implementation.cache_tag` `arbace-315`,
  `stdlib_module_names` 3.15.2's 296 (`sys.ae`), sysconfig's `VERSION` `3.15` (`builtins_sysconfigdata.ae`). Under the
  switch a run in a fresh worktree extracts 3.15.2's tree (3.14's still `env -u`'s, as 237 made it).
- **P6, first pass: what 3.15.2's Lib asks of pyi's C modules**, found by importing every top-level module of 3.15.2's Lib
  (165; the oracle imports all) and by a scan of both trees' imports of C-module names, each ported from 3.15.2's C:
  - `binascii` (`builtins_binascii315.ae`, new; `Modules/binascii.c`): `b2a_base32`/`a2b_base32`, `b2a_ascii85`/
    `a2b_ascii85`, `b2a_base85`/`a2b_base85` (Base85 and Z85 by `alphabet=`), `b2a_base64`'s `padded=`, `wrapcol=`,
    `alphabet=`, `a2b_base64`'s `padded=`, `alphabet=`, `ignorechars=`, `canonical=` and the strict mode's default by
    `ignorechars`, `a2b_hex`/`unhexlify`'s `ignorechars=`, the ten alphabets as constants, the reverse tables cached per
    alphabet as the module state's `reverse_table_cache`, every message; `hexlify`'s `bytes_per_sep` a `Py_ssize_t`. 3.15's
    `base64` is these functions (`b64encode` calls `b2a_base64(padded=, wrapcol=)`), so base64, email and http need them.
  - `_sre`: `Pattern.prefixmatch` and `Scanner.prefixmatch`, `match` their alias with `prefixmatch()`'s messages (re,
    json, argparse, traceback, tokenize, unittest, urllib.parse, asyncio, decimal did not import without it).
  - `_codecs._normalize_encoding` (`encodings` imports it: subprocess did not import), `_Py_normalize_encoding` without
    the lower casing, the strict UTF-8 encoder's error for a surrogate.
  - `_ssl`: `get_sigalgs()` (Go's crypto/tls signature schemes by OpenSSL's names) and `HAS_PSK_TLS13` False (ssl did not
    import); `posix._clearenv` (`os.environ.clear`'s fast path).
  - `_ctypes`: an unknown `_type_` is 3.15's one AttributeError, which 3.15's ctypes catches around its complex types
    (`"Zd"`, `"Zf"`, `"Zg"`): pyi's ffi has no complex types (as a CPython built without them), so ctypes imports.
  - `{**x}`, `f(**x)`, `dict(x)` and `update(x)` with a `keys()` that returns a non-iterable: 3.15's `%T.keys() must return
    an iterable, not %T` (`abstract.c`'s `method_output_as_list`; 239's leftover of test_unpack_ex).
  - `codecs.lookup`'s normalization back to 3.15.2's `normalizestring` (ASCII letters lower case, spaces to hyphens: the
    search functions see `test.aaa-8`, not 3.14's `test.aaa_8`), and `encodings` imported before the registry's first use
    (CPython imports it at start; pyi imported it only while no search function was registered, so one registered before
    any lookup hid every built-in codec: a 3.14 gap too, left to the user's call in 252's report).
- **P7's tooling**: `tools/pyi/libtest_all.py find` reads the tree's own `SPLITTESTDIRS` (3.15.2 splits `test_io` and
  `test_os`: 560 modules against 3.14.10's 538, the list of 3.14.10 byte for byte as before), the check's universe keyed by
  the tree under 3.15 only, every record's path by version (`RECV`, `RECS`). The records themselves are the main host's.
- **P8**: no new IR operation, as 239 found; the compiled paths checked on this base (216's register loop, 251's fast
  paths): `t252-lazy-aot.sh` (a hot loop's load-global cache over a placeholder, a closure, a class body, another module's
  attribute, a deleted and rebound lazy name, `sys.lazy_modules`) prints the oracle's text by `aot` and by `run`.

**Counts** (run on the VM; 3.15.2's tests in their own tree, `bin/pyi aot test/python/libtest.py MODULE` under the switch,
own verdict; the base 7e3db54e9 by 237-239's overlay on 3.14.10's Lib, as those tasks measured):

| module (3.15.2's) | oracle ok / methods | base ok | head ok | head agrees |
|---|---:|---:|---:|---:|
| test_binascii | 220 / 237 | 48 | 220 | 237 |
| test_base64 | 67 / 67 | 34 | 63 | 63 |
| test_re | 164 / 168 | 145 | 151 | 155 |
| test_codecs | 275 / 288 | | 266 | 279 |
| test_unpack_ex (doctests) | 6 / 6 | 5 (239) | 6 | 6 |
| test_struct | 47 / 47 | | 32 | 32 |
| test_json | 230 / 233 | | import fails | 0 |

The non-passes left: the `cpython_only` and `_testinternalcapi` skips (PYS15, the existing classes); test_base64's
`test_encodebytes`/`test_decodebytes` (memoryview's shaped `cast` keeps one dimension, under 3.14 too: 252's report) and
its `test_b16encode_wrapcol` (hexlify's `Py_ssize_t`, fixed after the run); test_re's three 3.14 failures; test_struct's
new `Struct` construction (`__new__` does the work, `__init__` deprecated: 3.15.2's `_struct.c`, P6's second pass);
test_json's package does not import under 3.14 either (no `_json`, `cjson.JSONDecodeError = ...` on None).
Every top-level module of 3.15.2's Lib imports but `curses` and `symtable` (no `_curses`, `_symtable`: 3.14's gaps).

**3.14 unchanged** (run, no switch): `p/feat314.py` (no 3.15 name, the version, `stdlib_module_names`, sysconfig, the old
messages) the same on base and head; `make python-essential` the same lines (check --aot 21 programs: 20 pass, 1 classed);
`bin/pyi table-check --configs tamago/amd64,tamago/arm64` ok; `bin/pyi runtime vet` rc 0; `REPR_PYTHON_ORACLE=live
PYI_SMOKE_ONLY='^(f-flags|g-math-int|f-intbytes|e-gen|c-locale|f-binascii|g-re)' bin/pyi runtime check` every package
printed and read again the same, every table and control as they must, 7 programs and 1 session equal; 3.14.10's discovery
byte for byte. 239's `t239-p3.sh` and `t239-lazy.sh` pass on the head (with 3.15.2's Lib now under the switch).

Not done (P6's second pass, measured by the main host's first 3.15 night rather than by hand): `_struct`'s construction,
`_json` (absent by design), `resource`'s unsigned `RLIM_INFINITY` and `RLIM_SAVED_*` (read: `py2rlim`, `rlim2py`), `math`,
`_datetime`'s twin, `_pickle`'s, `array`, `zlib.crc32_combine`, `mmap`, `sqlite3`, `_ssl`'s groups and sigalgs setters,
`socket`, `select`, unicodedata's rest (237); the records themselves (the `*315*` files) with DECISION 1.
