# After PY11: C extensions, packaging, Python 3.15 and the embedding (a design note)

Status: a design note, queue 202d, 2026-11-04, on the Mac VM. Nothing is adopted. The proposals are PYS21-PYS31, each
with its options and the recommended one first; they are also listed in doc/python/PYTHON-SURVEY.md §12 ("After PY11").
The calls are the user's. Base: `mac/201-python-py7-py11` 45ce1e92. No code changed.

Every claim is tagged with how it was established:
- **run**: executed on the real code or data (pyi on stage 0, the key functions of `bin/lib`, `go build` of the printed
  runtime with /root/tamago-go, `nm` on the pinned apks' shared objects, file counts on the pinned tarball);
- **probed**: a model or stand-in executed (CPython 3.14.10 of the host standing for what pyi would load, 3.15.2's
  tarball fetched into `.tmp/q202d/py315/` and read, never run);
- **read**: from the sources or records alone, or from memory of other projects (marked "to verify").

The VM is aarch64 with 8 CPUs, under the load of 202's other parts. Its measures are samples, and amd64's may differ.

Contents: 1 C extensions · 2 Packaging · 3 Python 3.15 · 4 Embedding pyi at the re-seed · 5 The proposals in brief ·
6 The main host's commands · 7 Scratch and sources.

## 1. C extensions

### 1.1 What is absent today, and why

pyi loads no extension module and has no C API. Four classes of `test/python/divergences.edn` hold what that costs (read):
- **`:c-accelerators`** (design; PY6g's DECISION 1, the user, 2026-11-04). `_pickle`, `_json`, `_decimal` and `_datetime`
  are absent. pickle, json, decimal and datetime run as their pure-Python twins.
- **`:c-modules-deferred`**. `_sqlite3`, `termios`, `_lzma`, `_csv`, `faulthandler`, `_lsprof`, `_tracemalloc`,
  `_interpreters` and others are missing.
- **`:extension-modules`** (design). The import system's extension loaders find nothing.
- **`:testcapi`**. `_testcapi` and `_testinternalcapi` are missing.

What a user meets today (**run**: `bin/pyi run` on stage 0, a program importing 19 modules, 5 min 39 s on goi). Twelve
import as on CPython, including zipfile, tarfile, email, sysconfig, importlib.metadata, hmac, decimal, datetime, json and
pickle. Seven do not:

| module | missing C module |
|---|---|
| csv | `_csv` |
| bz2 | `_bz2` |
| lzma | `_lzma` |
| ssl | `_ssl` |
| mmap | `mmap` |
| sqlite3 | `_sqlite3` |
| ctypes | `_ctypes` |

xml.parsers.expat also fails today (`pyexpat`, which 202e is porting).

### 1.2 What Arbace has for C (doc/C23-SURVEY.md; read)

- **repr/c.** It reads C into C forms (cc's lexer, preprocessor and parser, ported).
- **ci** (`arbace/c`, `go/arbace/{ci,crt,cinterp}`). It interprets those forms. Its memory model (CS9 (a)) is one flat
  virtual address space per program: 64-bit addresses, little-endian, Go slabs, the SysV x86-64 layout.
- **musl 1.2.6 as C forms**, read into a cache by `bin/lib/ci/musl` and not committed.
- **cf2g** (`arbace/c/cf2g.ae`). It translates ci's node program to Go functions over the same memory, with the same
  order of evaluation and the same arithmetic.

Speed, recorded on the C crop's host (C6.4, read):

| program | gcc -O0 | cf2g | ci |
|---|---:|---:|---:|
| nbody | 0.29 s | 4.9 s | 49 s |
| binarytrees | 0.07 s | 1.5 s | 13.6 s |

So cf2g is about 17-21 times gcc -O0, and ci about 170-190 times.

Three facts bound every path below:

1. **No binary loading.** Arbace builds use no cgo (`CGO_ENABLED=0` everywhere) and the executables are static. pyi
   cannot `dlopen` a `.so`, so every wheel that ships a compiled extension is out of reach as shipped. A C path starts
   from the extension's C source (an sdist), read by repr/c. (read)
2. **C only.** ci and cf2g take C, not C++, Rust, Fortran or assembly. (read)
3. **ci's memory is bytes, pyi's objects are Go values.** A `PyObject *` handed to C must be an address in ci's memory.
   So every pyi object that crosses into C needs a proxy there, and a table from proxy to object. PyPy's cpyext works
   the same way. (read)

### 1.3 The API's size (run, on the pinned tarball's headers and `Misc/stable_abi.toml`)

| part | 3.14.10 | 3.15.2 |
|---|---:|---:|
| stable ABI: functions (abi_only among them) | 782 (47) | 824 |
| stable ABI: data (types and exceptions) | 143 | 143 |
| stable ABI: structs | 20 | 30 |
| stable ABI: consts | 151 | 181 |
| stable ABI: macros | 11 | 26 |
| `PyAPI_FUNC` declarations in `Include/*.h` and `Include/cpython/*.h` | 1,215 | 1,259 |
| those headers' lines (`Include/internal/`'s 140 files not counted) | 19,359 | |

The **limited API** is the 735 functions of the stable ABI that are not abi_only, plus its 133 data. Under it, reference
counting is a function call: `Py_INCREF` calls `_Py_IncRef` when `Py_LIMITED_API >= 3.12` (`Include/refcount.h:254`,
read). That lets the proxies count references without C reading `ob_refcnt` itself.

The **full API's** inline functions and macros read the objects' memory directly: `PyTuple_GET_ITEM`, `PyList_GET_ITEM`,
`PyUnicode_DATA` and `PyUnicode_KIND` (the compact layout), `Py_TYPE(o)->tp_flags`, `Py_INCREF`'s `ob_refcnt`. So
under the full API each proxy must mirror its object's CPython layout and keep that mirror coherent both ways. That
coherence is cpyext's known cost.

How the three shared objects of the 20 pinned apks use the API (run: `nm -D --undefined-only`, aarch64 apks):

| extension | `Py*` symbols it imports | API | pure fallback |
|---|---:|---|---|
| pyyaml `_yaml` (Cython over libyaml) | 158, including `PyFrame_New`, `PyCode_NewEmpty` and `PyErr_Fetch` | full | yes: `yaml/__init__.py` leaves `__with_libyaml__ = False` |
| coverage `tracer` (C) | 49, including `PyEval_SetTrace`, `PyFrame_GetLasti` and `_Py_Dealloc` | full | yes: the Python tracer and sys.monitoring |
| markupsafe `_speedups` (C) | 2 (`PyModuleDef_Init`, `PyUnicode_New`), the rest the inline macros over `PyUnicode`'s layout | full | yes: `markupsafe/_native.py` |

None of the three is abi3. All three run today without their extension.

### 1.4 The paths

- **(A) The C API over pyi's objects, through ci and cf2g.** The pieces:
  - CPython's `Include/`, read by repr/c with a `pyconfig.h` of Arbace's, gives the declarations.
  - A layer in Go forms (`go/arbace/py/capi`) gives the natives for ci, as `crt` gives musl's system calls:
    - proxies and the handle table;
    - reference counts bridged to Go's GC (a proxy keeps its object alive while its count is above zero);
    - type objects from `PyType_Spec` and from static `PyTypeObject`s;
    - multi-phase module initialization;
    - the thread state's current exception;
    - `PyGILState` over pyi's GIL;
    - `PyArg_Parse*`'s format strings over ci's varargs (`Python/getargs.c`, 2,863 lines);
    - `Py_buffer`.
  - The extension's C, linked with musl, runs on ci (eval) or is translated by cf2g (AOT).
  - Most functions are thin: pyi already has the semantics (`PyObject_GetAttr` is `py.GetAttr`, `PyNumber_Add` is the
    binary operator).

  Estimates (read; from the counts above and goi's and ci's experience, to re-estimate before any start):
  - **(A1) the limited API**: about 25-45 working days.
  - **(A2) the full API**: another 30-60, never complete (cpyext took years, from memory, to verify).
- **(B) Single modules ported to Go**, as PY6 did:
  - each C module rewritten in Go forms over pyi's objects, exact by its tests;
  - sizes (run, the tarball): `_csv.c` 1,864 lines, `_heapqmodule.c` 811, `_json.c` 1,974, `_pickle.c` 8,304,
    `_datetimemodule.c` 7,799, `_decimal.c` 6,355 over libmpdec's 18,015.
- **(C) Self-contained C libraries through cf2g, with glue in Go forms.**
  - The library is read by repr/c and translated by cf2g. It needs no Python API, only musl.
  - The Python module's glue is written in Go forms over pyi's objects, replacing the C module's API glue.
  - Candidates:
    - libmpdec for `_decimal` (it ships in the tarball, `Modules/_decimal/libmpdec/`);
    - SQLite for `_sqlite3` (already a planned C program corpus, C9.3);
    - liblzma, libbz2 and zstd for `_lzma`, `_bz2` and `_zstd`;
    - HACL* for the hashes.
  - This is PYS11 (b) and survey §6.6's "C modules through the C crop", made concrete. It needs no general C API.
- **(D) None.** Extension modules stay a design class. A package runs where it has a pure-Python fallback.

### 1.5 Speed (estimate, read)

The ratios used:
- pyi's compiled loop is about 7 times CPython on pyperformance's pure benchmarks (PY8a: 21.9 s against 3.3 s);
- cf2g is about 17-21 times gcc -O0, and gcc -O2 is commonly 2-3 times -O0 (from memory, to verify);
- C `_decimal` is commonly 30-100 times `_pydecimal` on CPython (from memory, to verify).

What follows for decimal-heavy code:
- **`_decimal` through cf2g** (C's libmpdec, path (C)): about 40-60 times CPython's C `_decimal`. That is about as fast as
  `_pydecimal` on CPython, so several times faster than `_pydecimal` on pyi.
- **The same on ci** (interpreted): slower than the pure twin. An accelerator gains only when compiled.
- **Path (A)'s crossings** cost more again: a proxy lookup and a call through ci's natives per API call.

So the accelerators' value is speed on a few modules and the C-variant tests of `:c-accelerators`. Behaviour gains
nothing: `_pydecimal` and `_decimal` are held to the same tests.

### 1.6 What each path reaches (read; the packages' structure from memory of the projects, to verify)

| package (popular on PyPI) | its extension | (D) none | (B)/(C) | (A1) limited | (A2) full |
|---|---|---|---|---|---|
| markupsafe, pyyaml, msgpack, simplejson, wrapt, coverage, multidict, yarl, frozenlist, aiohttp, protobuf (python impl), charset-normalizer | C, Cython or mypyc with a pure fallback | **runs** (the fallback) | | faster, where the source builds limited | faster |
| psutil, regex, ujson | C, no fallback | no | (B) only by porting each | if it builds limited (most do not) | yes, from source |
| lxml | Cython over libxml2 and libxslt | no | (C): libxml2 through cf2g, large | no | yes, with libxml2 through cf2g |
| numpy, pandas | C, C++ and Cython; SIMD; their own C API | no | no | no | in principle; in practice no (C++, intrinsics, size) |
| cryptography, pydantic-core, orjson, polars, tokenizers | Rust (PyO3) | no | no | no | no: not C |
| grpcio, pyarrow; scipy | C++; Fortran | no | no | no | no |
| greenlet, gevent | stack switching in assembly | no | no | no | no |
| cffi, ctypes users (zstandard's cffi backend, ...) | calls into native libraries | no | no | no | no: no `dlopen` |
| the stdlib's `_sqlite3`, `_decimal`, `_lzma`, `_bz2`, `_zstd`, `_csv`, `mmap`, `_ssl` | C over a library or the OS | twins for decimal only | **(C) for the library ones, (B) for `_csv` and `mmap`, Go's TLS for `_ssl`** | | |

What the table shows:
- A general C API reaches the source-built C packages with no fallback: few of the popular ones, and none of the growing
  Rust share.
- The stdlib's C-only modules are reached by (B) and (C) at a fraction of (A)'s cost.

### 1.7 Proposals

**PYS21 Third-party C extensions (the general C API).**
- (a) **None for now** (recommended). Extension modules stay `:extension-modules` (design). Packages run on their pure
  fallbacks. The C API is revisited only on a named need: a package the user wants, with no fallback, C source, and
  limited-API buildable.
- (b) The limited API, (A1), through ci and cf2g, from sdists only: 25-45 days. It reaches the few limited-buildable C
  packages.
- (c) The full API, (A2), as cpyext: 55-105 days with (b), never complete.
- (d) HPy: the universal ABI's handles fit pyi's objects best, but few packages ship HPy builds (from memory, to verify).
- Why (a): no binary wheel can load (no cgo, no `dlopen`), so every path starts from sources and reaches little. The
  stdlib's own gaps have cheaper routes (PYS22). On ci, an accelerator is slower than its pure twin.

**PYS22 The stdlib's C modules after the tag.**
- (a) **By module, the cheapest exact route** (recommended):
  - **Go ports (B)** for the small glue-heavy modules: `_csv` first (pip imports csv: §2.1), then `mmap`.
  - **cf2g with Go glue (C)** for library cores: `_sqlite3` after C9.3's SQLite corpus, `_lzma`, `_bz2`, `_zstd`, and
    libmpdec for `_decimal` only if decimal's speed is measured to matter.
  - **`_ssl`** over Go's crypto/tls with `x509roots/`.
  - `_datetime`, `_pickle` and `_json` stay twins: PY6g's DECISION 1 stands, and the classes stay design.
- (b) Every module ported to Go by hand: libmpdec, SQLite and liblzma are large and exact C, and porting them is
  translation by hand.
- (c) Keep them all as classes.

## 2. Packaging

### 2.1 What runs today

- **The 20 apks** (PYS17 (a), queue 174's DECISION (a)): 921 files of site-packages, read and printed at every level.
  They are not run. Seventeen are pure Python, and three carry a `.so` with a pure fallback (§1.3, run).
- **pip** (26.2.1, among the 20) cannot start on pyi. `pip/_internal/metadata/base.py` and
  `operations/install/wheel.py` import csv at their top (read), and csv needs `_csv` (run, §1.1).
  - With `_csv` ported, an offline `pip install --no-index --no-deps FILE.whl` should need only zipfile, email,
    importlib.metadata, sysconfig and hashlib, which import today (run). This is a projection, not run.
  - Network installs need `_ssl`.
  - Building an sdist runs its PEP 517 backend in a subprocess of `sys.executable`. On the AOT executable that name is
    empty today (PY7's `Permission denied: ''`).
- **The host's CPython** loads, on importing pip's install path, 527 modules, 44 of them built in or shared objects
  (probed).

### 2.2 Pins and the records rule

A package is an external like the tarball. It is pinned by name, version, file name and SHA-256 (PyPI's published
digest), fetched once into the shared cache (`REPR_PYTHON_SRC`, GTS212 (a)) by `bin/repr-python pins --fetch`, and
checked by `pins --check`. Nothing of it is committed but the pin.

A pin file in PEP 751's `pylock.toml` shape is the standard lock format with hashes (accepted 2025, from memory, to
verify). The reader would be Arbace's; `test/python/corpora.edn` could equally hold the list, as it holds pyperformance.

References of programs that use packages are CPython's runs, recorded on the main host (amd64). There, CPython 3.14.10
runs with the same wheels installed into a virtual environment under `.tmp/` by the host's pip. The VM records nothing
(the records rule).

### 2.3 Proposals

**PYS23 Packages beyond the 20 apks.**
- (a) **Pinned pure-Python wheels from PyPI** (recommended). `py3-none-any` only. A first set is chosen by the user:
  jinja2's and requests' own dependencies are in the apks already; candidates are attrs-free pure ones such as
  `six`, `toml` or `tomli`, `click`, `rich`, `pygments`, `docutils`. The list is a pin file, the references CPython's runs.
- (b) sdists built by pyi through their PEP 517 backends. That needs setuptools on pyi and `sys.executable`, and
  reaches pure sdists only.
- (c) Alpine's apks only: their versions move with edge (§3).

**PYS24 The installer.**
- (a) **An installer of Arbace's own** (recommended). It would be a verb, `bin/pyi install`, in `arbace.python`, and
  implement only the wheel format: PEP 427 and 491, unzip, `RECORD` checked, `.dist-info`, `entry_points.txt`'s console
  scripts as launchers of pyi. It runs on the tools' host, reproducible and needing no Python. pip on pyi stays an eval
  target: `pip install --no-index` of a pinned wheel into a scratch site, compared with CPython's pip run on the same
  wheel.
- (b) pip on pyi as the installer: it needs `_csv`, and `_ssl` for anything not local; it depends on pip's own behaviour.
- (c) The host's CPython pip installing into a directory pyi reads: CPython becomes a build tool, against the
  standalone goal.

**PYS25 Where packages live.**
- (a) **A site directory built from the pins** (recommended):
  - for the checks, `.tmp/cache/python/site-KEY/` (KEY the pin set's digest), shared by the worktrees as the AOT's
    packages are (R4);
  - for a user's program, `program.edn` gains `:python {:wheels [...]}` and `--build-program` installs them into the
    program's work directory, then compiles the modules the program imports, as it compiles the stdlib's (PY8);
  - `sys.path` names the site after the stdlib.
- (b) Committed as Python forms (`python/site-packages/`, as `jdk/`): a large commit per version bump, for no gain over a
  pin.
- (c) A user site (`~/.local`, CPython's way): outside the checkout, not reproducible.

## 3. Python 3.15

### 3.1 Where things stand (probed, 2026-11-04)

- **python.org** lists 3.15.0 (`Python-3.15.0.tar.xz`, 2026-10-09), 3.15.1 (2026-10-21) and 3.15.2 (2026-10-25, the same
  day as 3.14.10).
- **Alpine edge** still has `python3-3.14.10-r1`. The VM's APKINDEX caches of 2026-11-04 list no other python3, and no
  `python3.15` in main, community or testing. aports' `main/python3/APKBUILD` on master reads `pkgver=3.14.10`.
- **When edge will move** is not knowable here. Historically Alpine moves edge's python3 to a new minor some months after
  CPython's release, before a stable branch (May or November). From memory, to verify.
- **When it moves:**
  - the 3.14.10 apk and the 20 py3-* apks leave edge's mirrors (edge keeps only the current build);
  - `bin/repr-python pins --fetch` then cannot refetch them;
  - the oracle's libraries move too. python3 links the system's expat, libmpdec, sqlite, xz, bz2, zlib, libffi and
    openssl (run: `apk info -R python3`), and PY11-PREP's `:host-ids` hashes only `python3` and `libpython`.

### 3.2 The delta, measured (run on the two tarballs; 3.15.2 read, never run)

| what | 3.14.10 | 3.15.2 | changed |
|---|---:|---:|---|
| `Grammar/python.gram` | 1,577 lines | 1,653 | 164 lines: `lazy` imports (PEP 810, a soft keyword), unpacking in comprehensions (PEP 798), `+` before numbers in patterns, new `invalid_` rules (raise, assert, import-from, if-expression, mapping pattern) |
| `Parser/Python.asdl` | 154 | 154 | `Import` and `ImportFrom` gain `int? is_lazy`; `DictComp`'s value becomes optional |
| `Grammar/Tokens` | 78 | 78 | unchanged |
| `Parser/lexer/lexer.c`, `pegen_errors.c`, `pegen.c` | | | 10, 3 and 38 lines |
| `Parser/action_helpers.c` | 2,008 | 2,134 | 146 lines |
| `Python/symtable.c` | 3,299 | 3,391 | 112 lines |
| `Python/codegen.c`, `flowgraph.c`, `compile.c` | | | 476, 224 and 74 lines |
| `Python/import.c`; `importlib/_bootstrap.py`, `_bootstrap_external.py` | | | 974; 233 and 145 lines (lazy imports) |
| `Objects/dictobject.c`, `typeobject.c`, `unicodeobject.c`, `setobject.c`, `longobject.c`, `floatobject.c` | | | 1,690, 2,057, 2,795, 570, 437 and 658 lines (frozendict among them) |
| `PYC_MAGIC_NUMBER` | 3627 | 3666 | |
| opcodes (`Include/opcode_ids.h` defines) | 236 | 243 | `_opcode_metadata.py` 744 lines |
| Unicode (`UNIDATA_VERSION`) | 16.0.0 | 17.0.0 | |
| the stdlib (`Lib/` less `Lib/test`) | 720 files, 335,637 lines | 758, 353,814 (+5.4%) | added: `profiling/` (Tachyon), `_pyrepl` 4, `importlib.metadata` 2; removed: `sre_compile`, `sre_constants`, `sre_parse` |
| `Lib/test` | 1,153 files, 667,670 lines | 1,302, 724,289 (+8.5%) | 161 files added, 12 removed, 397 of the common changed; 434 top-level `test_*` entries in both |
| `Modules/` C files | 234 | 254 | `mathintegermodule.c`, `_remote_debugging/` split |

`Doc/whatsnew/3.15.rst` names these as the language and runtime changes:
- PEP 661: the `sentinel` built-in;
- PEP 686: UTF-8 as the default encoding;
- PEP 798: unpacking in comprehensions;
- PEP 810: lazy imports;
- PEP 814: `frozendict`;
- PEP 829: startup configuration files.

It also names the profiling package (PEP 799), typing's PEPs 728, 747 and 800, and a stable ABI for free-threaded builds.

### 3.3 What in repr/python and pyi is version-bound (read)

- **Pins:**
  - `tools/pydump/PYTHON_VERSION`;
  - `test/python/corpora.edn` (the tarball's digests and every corpus's measures);
  - `tools/pydump/apks.sha256` (the 20 apks, rebuilt by Alpine for 3.15);
  - PY11-PREP's `:host-ids` (`/usr/lib/libpython3.14.so.1.0` by name).
- **Generated, mechanical:**
  - `arbace/repr/python/parser.ae`, from the grammar by `bin/repr-python pegen`;
  - pyi's tables, from `tools/pyi/*_table.py`: `asdl_table` (`_ast`), `opcode_table` (`_opcode` and `dis`'s tables),
    `unicodedata_table` (UCD 17.0.0), `cjkcodecs_table`; the errno, posix, socket and fcntl tables come from the host's
    headers, not the version;
  - `tools/pydump/syntax_cases.py`'s corpus.
- **Ported by hand (each a port of a file above):**
  - the lexer `lex.ae`;
  - `peg.ae` (action helpers, errors);
  - the scope analysis `scope.ae` (symtable.c);
  - the compiler `compile.ae` (codegen.c). Only the language's changes bind it: the IR is Arbace's (PYS7), so 3.15's new
    opcodes do not, except where `dis` and `_opcode` expose them;
  - the runtime's types and the import path, for lazy imports, frozendict, sentinel and UTF-8 by default.
- **The forms:** `doc/python/SPEC.md`, the converter and the printer, for `is_lazy` and the optional `DictComp` value.
  Old forms stay readable: the new field is optional.
- **pyi's own identity:**
  - `sys.version_info` and `hexversion` (`sys.ae`);
  - `cache_tag` `"arbace-314"`, which becomes `"arbace-315"` (PY6b's DECISION; pyi's magic is its IR's digest, not
    CPython's number).
- **Records:** `ref-cpython314*.edn` (renamed and re-recorded), every check's record, the classes re-judged, PY7's
  coverage subset made again, the essential lists, the costs.

### 3.4 Cost of a move (estimate, read; working days)

| part | days |
|---|---:|
| read: the parser regenerated; the lexer's, helpers' and errors' deltas ported; the spec, converter and printer | 2-4 |
| scope and compile: symtable's and codegen's deltas for the language changes | 2-4 |
| runtime: lazy imports, frozendict, sentinel, UTF-8 by default, Unicode 17, the types' visible changes, the tables | 5-10 |
| records: re-recorded on the main host (nights), Lib/test's +161 files and 397 changed, classes re-judged | 3-6 |
| **sum** | **12-24**, plus the main host's nights for the whole-corpus records |

### 3.5 Proposals

**PYS26 When to move to 3.15.**
- (a) **After Python's tag, when Alpine edge's python3 is 3.15** (recommended). The oracle is the host's apk again, as
  PYS1 (a) wants. The move is a round of its own (PY12: the delta above), and 3.14.10's records stay at the tag.
- (b) Now, the oracle CPython 3.15.x built from the tarball into `.tmp/` (PYS1 (b)). The site-packages apks would stay
  3.14's (their `.py` files are version-neutral) or the corpus would wait.
- (c) Support both versions: a version parameter through the reader, pyi and every record. CPython itself does not do
  this, and it doubles the records.

**PYS27 Keeping 3.14.10's oracle once edge moves.**
- (a) **Keep copies of the oracle's apks** (recommended):
  - `python3-3.14.10-r1`, its dependencies' apks (`apk info -R`: libexpat, libmpdec, sqlite-libs, xz-libs, libbz2,
    zlib, libffi, openssl, readline, ncurses) and the 20 py3-* apks;
  - in the shared cache, pinned by SHA-256 per architecture as `apks.sha256` is;
  - installable offline into a root of its own (`apk add --root .tmp/... --allow-untrusted --no-network`, to verify);
  - `:host-ids` hashes the libraries too.
- (b) Build CPython 3.14.10 from the pinned tarball when the apk is gone (PYS1 (b)). Alpine's patches and configure
  flags then differ (`sys.version`'s build line, the system libraries), so a reference or two may move.
- (c) Nothing: the move to 3.15 becomes forced, on Alpine's date.

## 4. Embedding pyi at the re-seed

PYS18 (a) and PY10's DECISION 1 (a) (the user, 2026-11-04) settle the order. pyi is embedded at the first re-seed after
Python's tag, batched with C++'s and Scheme's (CP180, SC230). Until then pyi and repr/python are evaluated from the
checkout, out of `gate_key`. This section lists what the embedding needs, by C9's window (doc/C23-SURVEY.md, C9.1-C9.2)
as the model.

### 4.1 What changes, file by file (read)

1. **goi's table.** `go/arbace/gotable.edn`'s `:packages` gains `"arbace/py"` and `"arbace/libm"` (pyi imports libm, the
   Haskell line's). That was PY5a's DECISION (a): "listing it is the re-seed's step". `bin/goi table --size` records what
   it costs. The surface goi must invoke is 173 exported functions, 183 exported methods, 63 types and 20 variables and
   constants (run, counted on the printed runtime). Members goi cannot invoke go to `:exclude` with their reason.
2. **The embedded tree.** `arbace/j2g/embed.ae` drops its exclusion of `arbace/python.ae`, `arbace/python/`,
   `arbace/repr/python.ae` and `arbace/repr/python/`:
   - these are 3,640 and 6,470 lines (191,626 and 647,768 bytes; run);
   - `parser.ae` alone is 348,917 bytes of generated parser;
   - `bin/lib/version.bash`'s `arbace_embedded_sources`, `test/gate/steps.edn`'s `:tools`, `test/gate/audit.ae`'s
     `:gate` and `test/gate/gate-key-crops-test.bash` change with it.
3. **`gate_key`.** `bin/arbace-go`'s `gate_key` drops `! -path go/arbace/py*` and `go/arbace/libm*`.
4. **The keys outside `gate_key` already hold Python today** (run: each key computed before and after a one-line change):
   - **`arbace_runtime_key`** hashes all of `go/`. A change to `go/arbace/py` alone changes it.
   - **`arbace_javac_key`** hashes all of `arbace/` and `go/`. A change to `go/arbace/py` or to `arbace/python/compile.ae`
     alone changes it.
   - `arbace_aot_key` follows the runtime key.

   Each commit to pyi's runtime therefore does three things (read, `bin/lib/program.bash`, `bin/arbace-go`,
   `bin/java-check`), though no build reads go/arbace/py:
   - it makes stage 0's runtime "not the checkout's": `host_compiles` fails, so `--build`, `--gate-build` misses and
     `--build-program` take the two-run path, about 5.5 minutes instead of 2.5-3;
   - it invalidates the shipped build's `aot.key` (the test build's reuse, `--essential-compiled`'s base) and
     `reader_key`;
   - it gives javac's tool a new key: the `javac-tool` step, a 26 GiB heavy build, reruns in the next `--full`.

   122 commits touched `go/arbace/py` on 2026-11-03 and 11-04 (run, `git log`). See PYS30.
5. **The stdlib at run time.** Today the stdlib is not in the checkout. `bin/pyi` reads it from the pinned tarball's
   `Lib/`, extracted into `.tmp/repr-python/Python-3.14.10/Lib` (`stdlib_lib`). The embedded executable needs it from
   somewhere: PYS29.
6. **The compiler in-process.** A program's `exec`, `eval`, `compile` and run-time imports go today to a compile server
   on stage 0 (`compiler.sh`, `arbace.python.aot compile-server`). Embedded, the runtime calls `arbace.python.compile`
   in-process, through Go forms' API to Arbace (package `arbace`, `go/arbace.ae`, GR5).
7. **`sys.executable`.** The embedded `arbace` can give pyi a real one, `arbace -m arbace.python` or a `python3` launcher.
   That closes PY7's AOT-only `Permission denied: ''` and lets subprocess tests and PEP 517 builds run.

### 4.2 Measured on a sample (run, the VM, /root/tamago-go, `go build -p 2 -trimpath`)

The runtime is `go/arbace/py` and `go/arbace/libm` printed by `bin/pyi`'s `runtime_print`, at 45ce1e92's key
`43e48d70b7932b54`:

| what | measure |
|---|---|
| `go/arbace/py` as forms | 140 files, 67,302 lines, 14.9 MB (the Unicode and CJK tables' long lines) |
| printed Go | 138 files, 67,321 lines, 2.7 MB; libm 3,945 lines |
| compile of `arbace/py` with libm, Go's cache cold (std included) | 6.0 s wall, 15.8 s user, 0.65 GB peak |
| compile of `arbace/py` after a one-line change, cache warm | 2.7 s wall, 6.5 s user, 0.63 GB peak |
| an executable of the runtime alone (`py.AotMain` referenced) | 12.5 MB (linux/arm64) |
| stage 0 (`target/seed/arm64/arbace`, seed 1fe1063f) | 194.9 MB |
| a compiled program with its stdlib closure (samples/functions.py, 146 module packages, 66 MB of generated Go) | 34 s wall, 127 s user, 4.3 GB peak; 83 MB |

So pyi's Go side adds about 12 MB, about 6%, and seconds of compile to the executable. That excludes goi's invokers for
the listed members, which `bin/goi table --size` measures on the main host. The cost that matters is not the build's
size or time. It is that any change to pyi then rebuilds the gate's executables (about 8 minutes, `go-build` 19 GiB)
and is a change of the shipped runtime.

The last row is the AOT's, not the embedding's. A compiled program stays a separate build: its stdlib modules are
compiled ahead into packages.

### 4.3 What the tiers change (read; estimates)

- **Essential.** `python-read-essential` and `pyi-aot-essential` (40 s warm) run on the gate's executable instead of
  stage 0 plus the checkout's sources. Their `:inputs` then overlap `gate_key`, and the planner reruns them after every
  build of a new key. `pyi-aot-runtime` (the AOT's printed runtime, 6.6 minutes cold) stays: compiled programs still
  build over the printed runtime.
- **Nightly.** `pyi-check-aot-goi` (pyi on goi, 35 minutes for the 151 programs, a seventh a night under PY10's
  DECISION 2 (a)) loses its 10 s startup a process and goi's interpretation of the runtime. Its runs approach the loop's,
  about 5 minutes for the whole set (estimate). The rotation can then end, and the eval cell can judge pyi itself in
  every tier (PYS31).
- **Lib/test.** `pyi-check`'s driver runs on the executable at the cover driver's speed (2-5 s a module against 20-160 s
  on goi, PY7). `lib-test-all`'s 15 job-hours on the VM would drop by a large factor (estimate; to measure on the first
  night after the re-seed).
- **The window.** Like C9.2: the light checks first, then the build, `--smoke`, `bin/pyi run` and `make python-essential`
  on the new executable, the fixpoint, `bin/seed --make` and `--verify`.

### 4.4 Proposals

**PYS28 How pyi's runtime enters the executable.**
- (a) **Listed in goi's table** (recommended; PY5a's DECISION (a)): `arbace/py` and `arbace/libm` in `gotable.edn`, the
  size recorded by `bin/goi table --size`. `bin/pyi run` keeps its path and its code becomes compiled. PYS20's REPL
  reaches pyi through goi.
- (b) Copied into the program as C's `ci`, `crt` and `cinterp` are (j2g's `main.ae`), with a runtime class like
  `CInterp`. There are no invokers, but goi cannot call it, and PYS20 would need the table anyway.
- (c) Stay as today: the compiled loop by TamaGo's go plus the compile server on stage 0. That means no re-seed for
  Python, and pyi absent from the shipped `arbace`.

**PYS29 The stdlib of the embedded pyi.**
- (a) **Not embedded at the first re-seed** (recommended). It is read at run time from a configured `Lib` directory: the
  pinned tarball's, fetched by `bin/repr-python pins --fetch`, or a `--python-lib DIR`/`ARBACE_PYTHON_LIB`.
  `sys.path` follows. The seed and the build stay free of a new external.
- (b) Embedded compressed. `Lib/` less tests is 16 MB, 4.0 MB as xz and 4.9 MB as gzip (run). It would be compiled
  lazily, with `.pyc` in the `arbace-314` tag. The build would need the tarball (a new input of `bin/arbace-go` and of
  the seed).
- (c) Committed as Python forms, `python/Lib/` (survey §6.6's plan, `bin/python-vendor`), and embedded. This is the
  standalone end (the box has no host filesystem). It is large, and its moment is the box's revival.

**PYS30 The crops' files out of `arbace_runtime_key` and `arbace_javac_key` (not a re-seed question).**
- (a) **One exclusion list in `bin/lib/version.bash`, used by `gate_key`, `arbace_runtime_key` and `arbace_javac_key`**
  (recommended), now:
  - `go/arbace/{py,libm,oi,scm}` and their `.ae` go out of the runtime key. They are never what c2j's compilation of the
    namespaces depends on, before or after the embedding: the compiled namespaces name the runtime's classes, not goi's
    packages.
  - The crops' Arbace and Go files that no build of javac's tool reads go out of the javac key. The tool embeds
    `embed.ae`'s tree, which excludes them.
  - The effect: a Python commit no longer forces the two-run builds or a `javac-tool` rebuild (§4.1 item 4).
  - The cost: one function, and `test/gate/gate-key-crops-test.bash` extended to the two keys.
- (b) Leave them. Each crop commit keeps costing a two-run build and a 26 GiB tool build in the next `--full`.

**PYS31 The eval cell after the embedding.**
- (a) **pyi on the executable judged in every tier** (recommended, once measured): `pyi-check-aot-goi` becomes a whole
  run each night, if the first night measures it under 10 minutes. PY10's DECISION 2 (a), the loop as the judged
  source, was made for goi's 35 minutes.
- (b) Keep DECISION 2 (a): the loop judged, pyi's runs rotated.

The window also needs **a light c2j check of the Python namespaces before it** (C9.1's `bin/ci c2j-check` found an
"unboxed emit of invoke" that would have stopped the re-seed after hours). That is `bin/pyi c2j-check`: `arbace.python.*`
and `arbace.repr.python.*` compiled by c2j on stage 0, as the build's first pass would. It needs no decision, only a
REQUEST to `bin/pyi`'s owner, and is listed in §6.

## 5. The proposals in brief

| | proposal | recommended |
|---|---|---|
| PYS21 | third-party C extensions | (a) none for now; the limited API through ci and cf2g only on a named need |
| PYS22 | the stdlib's C modules | (a) by module: Go ports (`_csv` first, `mmap`), cf2g with Go glue for library cores (`_sqlite3`, `_lzma`, `_bz2`, `_zstd`; libmpdec if decimal's speed matters), `_ssl` over Go's TLS; the twins stay |
| PYS23 | packages beyond the apks | (a) pinned pure-Python wheels from PyPI |
| PYS24 | the installer | (a) Arbace's own wheel installer; pip on pyi as an eval target |
| PYS25 | where packages live | (a) a site directory from the pins, under the shared cache or the program's work directory |
| PYS26 | moving to 3.15 | (a) after the tag, when Alpine edge's python3 is 3.15, as a round of its own (12-24 days) |
| PYS27 | 3.14.10's oracle after edge moves | (a) the oracle's apks and their libraries kept, pinned, installable offline |
| PYS28 | pyi in the executable | (a) listed in goi's table |
| PYS29 | the embedded pyi's stdlib | (a) not embedded at the first re-seed: a configured `Lib` directory |
| PYS30 | the crops out of the runtime and javac keys | (a) one exclusion list for the three keys, now |
| PYS31 | the eval cell after embedding | (a) pyi on the executable in every tier, once measured under 10 minutes |

## 6. The main host's commands (expected output)

None of these is a record. Each one measures something this note estimates.

- PYS30's effect, before and after the change. Run `bash test/gate/gate-key-crops-test.bash`; extended, it ends `PASS`.
  Then run `. bin/lib/version.bash; . bin/lib/javac-key.bash; arbace_runtime_key .; arbace_javac_key .` before and after
  `echo // >> go/arbace/py/frame.ae`. The two keys must be unchanged (today both differ: §4.1 item 4). Restore the file
  with `git checkout HEAD -- go/arbace/py/frame.ae`.
- PYS28's size, on a branch that lists `arbace/py` and `arbace/libm`: `bin/goi table --size`. It prints the executable
  with and without the table, and the difference goes into `gotable.edn`'s comment.
- The window's light check (to write, REQUEST to `bin/pyi`'s owner): `bin/pyi c2j-check` ends with
  `0 failed` for the namespaces of `arbace/python` and `arbace/repr/python`.
- After the re-seed, PYS31's measure: `time bin/pyi check --aot` (pyi on the executable). The expectation is about
  5 minutes for the 151 programs, against 35 on goi.

## 7. Scratch and sources

- Scratch, under `/root/arbace-mac/.tmp/q202d/` (gitignored, never committed):
  - `build/`: the printed runtime and one compiled program, copied from the 201 integration's AOT cache, built with a
    fresh `GOCACHE`;
  - `apks/`: three apks and pip, extracted for `nm` and reading;
  - `py315/`: `Python-3.15.2.tar.xz` (SHA-256 `8b5536d2...97de`, 27,960,396 bytes, fetched from python.org 2026-11-04)
    and its extraction, read only;
  - `probes/pkgmods.py`: the import probe, run by `bin/pyi run` and by `python3 -I`.
- Sources:
  - CPython `v3.14.10` (the pinned tarball, `test/python/corpora.edn`) and `v3.15.2` (python.org): `Misc/stable_abi.toml`,
    `Include/`, `Grammar/python.gram`, `Parser/Python.asdl`, `Doc/whatsnew/3.15.rst`, `Modules/`, `Lib/`;
  - Alpine `aports` master `main/python3/APKBUILD`, and the APKINDEX caches of 2026-11-04;
  - doc/C23-SURVEY.md (§6, §7, C6.4, C9.1-C9.4);
  - doc/python/PY11-PREP.md;
  - doc/python/PYTHON-SURVEY.md (§6.6, PYS1, PYS11, PYS17, PYS18, the As built sections);
  - `bin/lib/version.bash`, `bin/lib/javac-key.bash`, `bin/lib/program.bash`, `bin/arbace-go`, `bin/pyi`;
  - `test/python/divergences.edn`.
