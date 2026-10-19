# GR8: std at the REPL, and reflect's methods — survey and spec draft

Status: survey, 2026-10-19, step 2 of the plan to the milestone `arbace-interprets-go-v1`
(doc/AGENDA.md). Nothing here is adopted. The decisions are the user's, as proposals GC55-GC64
(§7). GR7b numbers its proposals GC45-GC54.

Base: branch `goi-gr8-survey`, from main `77370a70` (GR1 to GR7a). The executable is that
commit's `bin/arbace-go --build` (`target/arbace-go/amd64/arbace`, 135,557,280 bytes, the same as
GR7a's), and the toolchain is TamaGo's go1.27.1 (`/root/tamago-go`). linux/amd64's std as Go forms
is the table's cache (`.tmp/goi-table/597d808a01c97cfb/linux-amd64`, `bin/repr-go read --goos linux
std`: 382 packages, 1,834 files, 31.3 MB of forms). The machine was shared, with a load of 15 to 35.
Times are the best of five per run, and spreads are given where they matter. The scratch is
`.tmp/goi-gr8-survey/`, not tracked: the programs, a copy of std's forms with the experiments'
edits, the scanners (Python, run on the forms' text) and the logs. Appendix A lists them.
Nothing of goi, `bin/goi`, the checker or the table was changed. GR7b is changing them now.

The classification of all 382 packages is committed as data, `doc/go/GR8-STD.edn` (Clojure
reader). It gives each package's class, reason, `:replace` declarations, forms size and the
measured load time or load error.

## Summary

- **Missing today.** The check has 54 unsupported programs.
  - 29 lack a std package: `regexp` 5, `syscall` 5, `testing` 4, `math/big` 2, `text/tabwriter` 2,
    `runtime/trace` 2, and one each of `flag`, `container/heap`, `go/ast`, `go/build`,
    `html/template`, `runtime/pprof`, `encoding/json`, `encoding/xml` and `hash`.
  - 16 lack a reflect member: 13 need reflect's method lookups, 3 need `encoding/binary`'s byte
    orders.
  - 9 are assembly or `go:linkname`, out of scope by design (§1.1).
  - Beyond the check, real programs import more. Go's toolchain as forms (`tamago/cmd`, 105
    packages) imports 40 std packages that goi cannot import today: `bufio`, `flag`, `go/*`,
    `debug/*`, `encoding/json`, `regexp`, `text/scanner`, `container/heap`, `math/big`, ...
- **What goi can interpret.** Std has 180 packages a user can import (no `internal/` or
  `vendor/` element). They split as follows:
  - 44 compiled in the table, as now;
  - 4 interpreted, as now (`cmp`, `iter`, `maps`, `slices`);
  - **87 interpretable from their forms**: 85 as they are, and 2 with one `:replace` each, a
    `go:linkname` pull into `net/textproto`;
  - **34 that must stay compiled** and join the table: 7 bound to the runtime (`syscall`,
    `os/signal`, `runtime/pprof`, `runtime/trace`, ...), 27 whose types the table's API already
    names or that belong to the crypto module (`hash`, `math/big`, `net/url`, `crypto/*`, ...);
  - 11 out of scope (`unsafe`, `embed`, `plugin`, `unique`, `weak`, `testing/synctest`, ...).

  The 87 need 53 of std's internal and vendored packages interpreted with them. Go's internal
  rule forbids those in the table, a package of module `arbace`. Four of the 53 need `:replace`:
  10 declarations, the runtime's hooks of `internal/godebug`, `internal/cpu` and `internal/abi`,
  and `go/internal/srcimporter`'s cgo hook. Three "bad linkname" aliases are dropped.
- **What running them found.** Imported at the REPL from the forms, 52 of the 86 measurable
  packages load as they are. The other 34 fail on four gaps of goi or of the table, F2 to F5,
  and on `hash` not being listed (§2.5). A fifth gap, F1, is a silent wrong answer in goi today: a label on a short variable declaration is
  ignored (F1), so interpreted `regexp` matches nothing. With F1 worked around in the forms'
  copy, `regexp`, `math/big` (its 6 assembly functions replaced by their Go versions),
  `encoding/json/jsontext`, `bufio`, `container/*`, `text/tabwriter`, `encoding/base64`, `flag` and
  `text/template` (fields; methods need GR8b) all give gc's output.
- **The cost.**
  - Speed: interpreted std runs **36 to 430 times slower than gc**, against compiled. `regexp`
    matching is 150×, `math/big` 100-430×, `jsontext` 115-130×, `bufio` scanning 106×,
    `container/heap` 56×, `tabwriter` 51×, `base64` 47×, `slices.Sort` 44×.
  - Load: about 100 KB of forms per second. The median package takes 2.0 s to import at the
    REPL, `regexp` with `regexp/syntax` 3.1 s and +110-150 MB of memory, and the largest closure
    measured (`debug/buildinfo`) 10 s. The check is 80% of that time; reading the forms is most
    of the rest, and defining is negligible. The functions a call reaches are built on that
    first call (117 ms for `regexp`).
  - Size, compiled instead: the table grows 8 KB (`text/tabwriter`) to 1.9 MB (`net/http`) per
    package. The 34 must-be-compiled packages cost **+1.5 MB** together. Compiling every user
    package would cost +31.2 MB.
  - **`text/template` and `html/template` compiled cost +18.0 and +19.2 MB**: their
    `MethodByName` with a name known only at run time makes the linker keep every exported
    method. They must be interpreted.
  - Size, interpreted: their forms, embedded compressed, are 1.4-2.4 MB per configuration for all
    174 packages.
- **Reflect's methods.** Measured member by member: listing `Type.Method`, `Type.MethodByName` or
  `Value.Methods` costs +17.7 MB each, the linker's ReflectMethod. `Value.Method` and
  `Value.MethodByName` together cost **+4 KB**. The 13 programs all reflect over *interpreted*
  types, whose RT has no methods (§5.2), so listing reflect's members would not make them pass
  anyway.
  - The design: goi serves reflect's method lookups itself, through `:overrides` of those
    members. For compiled types it uses the table's method entries, where reflect's own
    `Value.Method` stays usable (4 KB). For interpreted types it uses goi's descriptors. Its cost
    is goi's code, no linker effect.
  - 5 of the 13 programs use faithful interpreted types (named structs with fields) and pass at
    GR8.
  - 8 use named non-struct types or empty structs (`type M int`, `type T struct{}`), whose
    reflect identity only GR9's runtime overlay (G8) gives. They join GR9's exit.
  - `encoding/binary`'s byte orders cost nothing: `binary.LittleEndian.Uint32` is called through
    the listed interface `ByteOrder`, whose method expressions the table already has.
- **The mixed boundary.**
  - A package's types must be compiled if a listed API names them (`hash` of `md5.New`,
    `*big.Int` of `crypto/rand.Int`), or if compiled code hands out their values behind
    interfaces (`syscall.Errno` in `os`'s errors). Otherwise one type exists twice: today
    `image/png` fails on `crc32.NewIEEE()`'s `hash.Hash32`, and `bug257.go` on `md5`'s
    `hash.Hash`.
  - Reflect-driven encoders interpreted (`encoding/json`, `xml`, `gob`, `database/sql`) need
    `reflect.TypeFor` of their own interfaces. An interpreted interface's RT is `any` (§5.2), so
    `t.Implements(marshalerType)` would hold for every type. Compiled, they cost +0.2-0.7 MB and
    see interpreted structs' fields as gc's.
  - Interpreted, `flag` handles an interpreted `flag.Value` correctly where compiled `flag`
    would `reflect.New` its wrapper (§5).
- **Recommended.**
  - **GR8a, std at the REPL**, 6-8 days:
    - fix F1-F5;
    - add the 34 packages to the table (+1.5 MB);
    - add the reflect-driven encoders (`encoding/json`, `encoding/xml`, `encoding/gob`) and a
      speed set to the table (`regexp`, `bufio`, `compress/gzip`, `encoding/base64`,
      `encoding/csv`), together about +1.6 MB;
    - interpret everything else, its forms embedded in the digest (+1.4-2.4 MB);
    - resolve `go:linkname` between interpreted packages;
    - call `encoding/binary`'s byte orders through `ByteOrder`.
  - **GR8b, reflect's methods**, 3-4 days: goi's overrides of the reflect members above.
  - Exit: no check program unsupported for a std package or a reflect member, but the 8 moved
    to GR9 (GC61); every user std package importable at the REPL, or refused with its reason
    (the 11 out of scope).

## 1. What is missing today

### 1.1 The check's unsupported programs

`test/goi/check-linux-amd64.edn` (GR7a's record) has 54 unsupported programs:

| cause | programs | GR8's answer |
|---|---:|---|
| `regexp` | 5: `issue26411`, `issue36437`, `issue7690`, `issue9355`, `nosplit` | interpreted (F1) or compiled (GC64) |
| `syscall` | 5: `issue15002`, `issue79874`, `issue8606b`, `recover4`, `sigchld` | compiled (§2.2) |
| `testing` | 4: `bug295`, `issue4618`, `issue4667`, `issue56990` | interpreted (F2; `runtime/trace` compiled) |
| `math/big` | 2: `abi/idata`, `issue62360` | compiled (GC57) |
| `text/tabwriter` | 2: `issue30116`, `issue30116u` | interpreted (loads today) |
| `runtime/trace` | 2: `issue73748a`, `issue73748b` | compiled |
| `runtime/pprof` | 1: `issue9321` | compiled |
| `flag`, `container/heap`, `go/ast`, `go/build`, `html/template` | 1 each: `chan/doubleselect`, `chan/sieve2`, `bug206`, `alias3` (directory test), `issue16037_run` | interpreted (`go/*` F2, `html/template` F5) |
| `encoding/json`, `encoding/xml` | 1 each: `typeparam/issue48317`, `issue48318` | compiled (GC62) |
| `hash` (a type of a package not in the table, `md5.New()`'s `hash.Hash`) | 1: `bug257` | compiled |
| `reflect.Type.Method`, `MethodByName`, `Value.Method`, `MethodByName` | 13: `reflectmethod1`, `4`-`7`, `recover`, `bug424`, `issue16331`, `issue19028`, `issue25897b`, `issue27695`, `issue27695b`, `issue27695c` | GR8b, 8 of them GR9 (§4) |
| `encoding/binary`'s byte orders (`littleEndian.Uint64` ...) | 3: `issue11656`, `issue19201`, `issue44823` | through `ByteOrder` (§4.4) |
| assembly, `go:linkname` | 9 | out of scope (§1.1) |

So GR8 can bring 45 of the 54 back: 29 std packages and 16 reflect members (8 of these after GR9).

### 1.2 Beyond the check

The std packages each corpus imports that goi cannot today (files importing them):

- `$GOROOT/test`'s run programs (linux-test): `regexp` 6, `syscall` 5, `testing` 5, `math/big` 4,
  `bufio` 3 (in programs unsupported for another reason), `text/tabwriter` 2, `runtime/trace` 2,
  `crypto/subtle` 2, `unique` 2, and one each of 10 more. The directory tests add `go/build`.
- Go's toolchain as forms (`tamago/cmd`, 105 packages): 40 packages, led by internal ones
  (`internal/buildcfg` 83 files, `internal/abi` 50). Of the importable ones: `go/constant` 53,
  `debug/elf` 21, `bufio` 14, `go/token` 13, `flag` 10, `debug/macho` 8, `text/scanner` 7,
  `syscall` 6, `encoding/base64` 6, `regexp` 6, `encoding/json` 6, `html` 4, `debug/pe` 4,
  `container/heap` 3, `math/big` 3, `runtime/pprof` 3, `net/url` 3 ...
- At the REPL a Go programmer expects to import:
  - text and data: `bufio`, `regexp`, `encoding/json`, `encoding/csv`, `encoding/base64`,
    `text/template`, `text/tabwriter`, `flag`, `html`;
  - containers and numbers: `container/*`, `math/big`, `math/cmplx`, `hash/*`;
  - systems: `compress/*`, `archive/zip`, `net/http`, `net/url`, `os/signal`, `log/slog`,
    `testing`;
  - tools and media: `go/ast`, `go/parser`, `go/token`, `go/format`, `image/*`.

  These are the plan's exit: every user std package importable (§6.3).

## 2. Which std packages goi can interpret

### 2.1 The method

Every package of the linux/amd64 conversion was scanned, its forms read as text
(`tools/scan.py`):

- its imports;
- its body-less declarations (`^:extern`): an assembly function when it has no `:go/linkname`,
  a linkname pull otherwise;
- its push linknames, harmless to interpretation;
- its uses of `unsafe` and its labels on declarations.

`tools/classify2.py` then put each package into one of the classes below, to a fixpoint over the
imports (a package is interpretable when all its imports are listed, compiled or interpretable).
The program's own compiled set came from `go list -deps arbace/cmd/...` on the work module: 182
std packages are linked into the executable today. The unlisted packages the table's API names
came from the digest's per-file imports, pruned to the ones the kept declarations use (GR1,
amendment 4).

### 2.2 What stops a package

- **Assembly and linkname pulls.** 74 packages have body-less declarations. Most are the
  runtime's internals (`internal/runtime/*`, `internal/sync`, `internal/poll`, `sync/atomic` 41,
  `reflect` 70). The packages a user would interpret have few:
  - `math/big` 6 (with Go versions beside them, `addVV_g` ...);
  - `internal/godebug` 4 (the runtime's `setUpdate` ...);
  - `internal/cpu` 3 (`cpuid`, `xgetbv`);
  - `internal/abi` 2;
  - `net/http` and `mime/multipart` 1 each (`net/textproto.readMIMEHeader`);
  - `go/internal/srcimporter` 1;
  - `go/types` and `net/http` 3 "bad linkname" aliases, never called, dropped.
- **The runtime's state.** One copy per program, which goi cannot hold twice:
  - `syscall` (its `Syscall6`, the runtime's fork and environment hooks);
  - `os/signal` (the runtime's signal queue);
  - `runtime/pprof`, `runtime/trace`, `runtime/metrics`;
  - `hash/maphash` (the runtime's `memhash` and seed);
  - `time/tzdata` (registers into compiled `time`);
  - `unique` and `weak` (generic over the runtime's weak handles, so neither compiled, since
    there are no instances, nor interpreted, since the bodies are the runtime's: out).
- **Types shared with compiled code.** An interpreted package's types are not its compiled twin's.
  When the table's API names a type of an unlisted package, that package must be compiled, else
  the member is unusable from interpreted code. The digest names 30 such packages. The
  importable ones are `hash`, `math/big` (`crypto/rand.Int`, `x509.Certificate.SerialNumber`),
  `net/url` (`x509`'s `URIs`), `compress/flate`, `crypto`, `crypto/cipher`, `crypto/ecdh`,
  `crypto/ecdsa`, `crypto/rsa`, `crypto/hpke`, `crypto/x509/pkix`, `encoding/asn1` and
  `encoding/pem`. The others are internal or vendored: `internal/poll`, `internal/abi`,
  `golang.org/x/net/dns/dnsmessage` ... The same holds for values compiled code hands out behind
  interfaces, mostly errors: `os`'s errors wrap `syscall.Errno`, so `errors.Is(err,
  syscall.ENOENT)` needs `syscall` compiled.
- **The crypto module.** `crypto/*` is assembly, FIPS self-checks through linkname
  (`crypto/internal/fips140/check`) and constant-time code. All of it stays compiled. Each
  package costs 0 to 86 KB, since most are linked already (`crypto/tls` is listed).
- **Go's internal rule.** The table is the package `arbace/gotable` of module `arbace`, so it
  cannot import `internal/...`, `.../internal/...` or `vendor/...` packages. Measured: the build
  fails with "use of internal package database/sql/internal not allowed". Std's internal
  packages that a user package needs are therefore either interpreted or the runtime's.
- **`unsafe`** blocks nothing. goi has all of `unsafe`'s builtins (`Add`, `Slice`, `String`,
  `SliceData`, `StringData`), and interpreted structs are real `StructOf` layouts (§5.2), so
  pointer conversions between them hold. It remains a risk where code assumes a compiled type's
  hidden layout. `strings.Builder`'s copy check and `reflect`-like header casts are compiled
  anyway.

### 2.3 The classes

| class | all 382 | user 180 | internal, vendor 202 |
|---|---:|---:|---:|
| compiled in the table (now) | 44 | 44 | 0 |
| interpreted (now: GR6's `:interpret`) | 5 | 4 | 1 (`internal/race`) |
| **interpretable as is** | 166 | 85 | 81 |
| **interpretable with `:replace`** | 8 | 2 | 6 |
| **to be compiled** (add to the table) | 34 | 34 | 0 |
| the runtime's or a compiled package's internals | 92 | 0 | 92 |
| out of scope | 28 | 11 | 17 |
| blocked by an out-of-scope import | 5 | 0 | 5 |

- **Interpretable as is (85 user):**
  - archives and compression: `archive/zip`, `compress/bzip2`, `compress/gzip`, `compress/lzw`;
  - text and data: `bufio`, `container/heap`, `container/list`, `container/ring`, `flag`,
    `html`, `html/template`, `regexp`, `regexp/syntax`, `text/scanner`, `text/tabwriter`,
    `text/template`, `text/template/parse`, `index/suffixarray`, `uuid`;
  - databases: `database/sql`, `database/sql/driver`;
  - debug formats: `debug/buildinfo`, `debug/dwarf`, `debug/elf`, `debug/gosym`, `debug/macho`,
    `debug/pe`, `debug/plan9obj`;
  - encodings: `encoding/ascii85`, `base32`, `base64`, `csv`, `gob`, `json`, `json/jsontext`,
    `json/v2`, `xml`;
  - logging and metrics: `expvar`, `log/slog`, `log/syslog`;
  - Go's own tools: `go/ast`, `go/build`, `go/build/constraint`, `go/constant`, `go/doc`,
    `go/doc/comment`, `go/format`, `go/importer`, `go/parser`, `go/printer`, `go/scanner`,
    `go/token`, `go/types`, `go/version`;
  - hashes and numbers: `hash/adler32`, `hash/crc64`, `hash/fnv`, `math/cmplx`;
  - images: `image`, `image/color`, `image/color/palette`, `image/draw`, `image/gif`,
    `image/jpeg`, `image/png`;
  - mail and the net: `mime`, `mime/quotedprintable`, `net/http/cgi`, `cookiejar`, `fcgi`,
    `httptest`, `httptrace`, `httputil`, `net/http/pprof`, `net/mail`, `net/rpc`,
    `net/rpc/jsonrpc`, `net/smtp`, `net/textproto`, `os/user`;
  - testing: `testing`, `testing/fstest`, `testing/iotest`, `testing/quick`, `testing/slogtest`.
- **With `:replace` (2 user):** `net/http` and `mime/multipart`, whose `readMIMEHeader` is a
  linkname pull of `net/textproto`'s unexported function: interpreted too (GC59).
- **To be compiled (34):** `syscall`, `os/signal`, `runtime/pprof`, `runtime/trace`,
  `runtime/metrics`, `hash/maphash`, `time/tzdata`, `hash`, `math/big`, `net/url`,
  `compress/flate`, `encoding/asn1`, `encoding/pem`, `crypto`, and 20 of `crypto/*`. Two of them,
  `crypto/hkdf` and `crypto/pbkdf2`, have a generic API only: their instances are the list's
  `:instances` (§9.3) when a program needs them.
- **Out of scope (11 user):**
  - `unsafe` (the checker's), `embed` (GS14), `plugin`, `runtime/race`, `runtime/coverage`;
  - `structs` (layout markers the compiler decides);
  - `unique`, `weak`, `testing/synctest` (the runtime's bubbles);
  - `runtime/goos` (TamaGo's board hooks, GR1);
  - `testing/cryptotest` (the crypto module's own tests, S45).
- **Internal packages interpreted with them:** 87 are interpretable, 53 of them needed by the user
  packages (`internal/bisect`, `internal/byteorder`,
  `internal/fmtsort`, `internal/godebugs`, `internal/goarch`, `internal/itoa`, `internal/zstd`,
  `vendor/golang.org/x/net/...`, `vendor/golang.org/x/text/...`, `encoding/json/internal/*`,
  `net/http/internal/*` ...). Their `:replace` declarations:

  | package | needed by | declarations | Go forms |
  |---|---:|---|---|
  | `internal/godebug` | 15 user packages (`archive/zip`, `html/template`, `go/build`, `net/http` and its 9 dependents, `expvar`, `mime/multipart`) | `setUpdate`, `registerMetric`, `setNewIncNonDefault`, `write` | `setUpdate` calls `update("", os.Getenv("GODEBUG"))` once (built in the experiment); the metrics hooks do nothing; `write` writes to `os.Stderr` |
  | `internal/cpu` | 3 (`testing` through `internal/sysinfo`, `testing/slogtest`, `net/http/httptest`) | `cpuid`, `xgetbv`, `getGOAMD64level` | zeros: no CPU feature, so the generic code paths |
  | `internal/abi` | 1 (`debug/gosym`) | `FuncPCABI0`, `FuncPCABIInternal` | a panic: never called by it |
  | `go/internal/srcimporter` | 1 (`go/importer`) | `setUsesCgo` | nothing (cgo is off) |
  | `net/textproto` (pulled by `net/http`, `mime/multipart`) | 2 | `readMIMEHeader` | goi's linkname resolution (GC59) |

  `internal/bytealg` (11) and `internal/reflectlite` (7) would need many more, but no user package
  needs them interpreted: their importers are compiled.

So 10 `:replace` declarations in 4 internal packages, 2 linkname pulls that goi resolves, and 3
dropped aliases make every interpretable user package's closure free of assembly and runtime
hooks.

### 2.4 How much is interpreted

An interpretable user package's closure (the forms goi checks to import it, its compiled imports
left out) has a median of 146 KB:

- the larger ones: `go/importer` 2.9 MB of forms, 39 packages; `net/http` 2.9 MB, 28;
  `net/rpc` 4.6 MB, 47; `html/template` 1.6 MB, 20; `go/types` alone 944 KB;
- the small ones: `container/*` 4-7 KB, `text/tabwriter` 18 KB, `flag` 50 KB, `bufio` 44 KB,
  `regexp` 241 KB with `regexp/syntax`, `encoding/json` 758 KB with v2 and `jsontext`.

The 174 interpretable packages together hold 10.0 MB of forms, 2.4 MB compressed. Without
comments and positions they are 8.3 MB, 1.9 MB compressed. With long doc strings dropped too,
6.4 MB and 1.3 MB.

### 2.5 What running them found

Each interpretable user package was imported at the REPL from the forms. `ARBACE_PATH` named the
forms' root: `(go/import [p "path"])`, then a declaration naming one of its functions, then its
call (`tools/loadone.sh`, every package, 6 at a time). 52 of 86 load and run as they are. The
other 34 fail on F2 to F5 and on `hash`. F1 shows only when the code runs:

- **F1, a label on a short variable declaration is ignored** (goi's builder). In
  `Skip: x := i`, converted as `(let [^{:go/label :Skip} x i] ...)`, a `goto :Skip` does not
  reach the label: the loop body runs on, and the result is wrong without an error. A
  three-function program shows it (`g3.go`: gc `103 3 3`, goi `0 3 0`). It is in `regexp`'s
  `tryBacktrack` (so interpreted `regexp` matches nothing: `MatchString("baaa")` false),
  `go/scanner`, `internal/zstd` and `internal/strconv` (21 such labels in std's forms, 4 in
  interpretable packages). No program of the check has one, so the check never saw it. It was
  worked around in the forms' copy by hand (the variable declared before the `goto`, the label on
  an assignment) for the measurements of §3.
- **F2, the table's members over `iter.Seq`** (10 packages: `go/ast` and 7 of its dependents,
  `go/build/constraint`, `testing`; `text/template` meets it once F5 is passed). The table's functions returning
  `iter.Seq[string]` (`strings.Lines`, `SplitSeq`, `SplitAfterSeq`, `FieldsSeq`, `FieldsFuncSeq`
  and `bytes`' five) and reflect's iterators (`Value.Seq`, `Seq2`, `Type.Fields`, `Ins`, `Outs`)
  have an invalid type for the checker, since `iter` is interpretable, not listed: "cannot range
  over strings.SplitSeq(...) (value with invalid type)". 18 members.
- **F3, `reflect.TypeFor`** (5: `encoding/xml`, `encoding/gob`, `database/sql`,
  `database/sql/driver`, `net/rpc`): GC32 left it unsupported ("a type of a package not in the
  table, in ((inst abi/TypeFor T))"). GC62 decides it.
- **F4, the builder's closure locations** (6: `encoding/json`, `encoding/json/v2`, `expvar`,
  `log/slog`, `net/rpc/jsonrpc`, `testing/slogtest`). In `json/v2`, a function literal's captured
  variable `src` has no location ("goi's builder: no location for src", `build.ae` `loc` from
  `build-fn-lit`, in an assignment's right side). In program mode it shows as `CallCompiled:
  interface conversion: interface {} is goi.R, not goi.S` in `json/v2`'s initialization.
- **F5, body-less declarations met at initialization** (12: 11 importers of `internal/godebug`,
  measured before its `:replace` was written, and `net/smtp` through `crypto/hmac`, then still
  interpreted). With `internal/godebug`'s four declarations replaced as in §2.3,
  `text/template` passes this point (F2 next).
- One more: `image/png` fails on `crc32.NewIEEE()`'s `hash.Hash32`, whose package the table does
  not list (§2.2: `hash` to be compiled).

Also seen: a std package loaded from `ARBACE_PATH` is an ordinary namespace that refers to
`arbace.core`, so Go names that shadow core's print warnings (`meta`, `compile`, `Inst`, and `Error`
over `java.lang.Error` in `regexp/syntax`). Digest-loaded packages (GR6's `load-digest-package!`)
do not go through `ns` and have none.

With F1 worked around and `internal/godebug` and `math/big` replaced, the programs of Appendix A
give gc's output byte for byte:

- `regexp`'s submatches;
- `math/big`'s 300! and fib(1000);
- `jsontext`'s token stream and re-encoding;
- `container/heap`, `container/list`, `bufio.Scanner`, `encoding/base64`, `text/tabwriter`;
- `text/template` over a map, over an interpreted struct's fields, and `{{.Hello}}` calling an
  interpreted method (gc: `hi cy`; goi stops at F2 before it, and after F2 it needs GR8b's
  `MethodByName`);
- `flag` with an interpreted `flag.Value` and `PrintDefaults`.

## 3. The cost

### 3.1 Speed

The same Go source, gc against goi with the package interpreted from its forms (Appendix A),
best of five, ms:

| work | gc | goi, package interpreted | ratio |
|---|---:|---:|---:|
| `regexp.MustCompile` of `(\w+)@(\w+)\.(com|org)` | 0.004 | 0.25-0.40 | ~60× |
| `regexp` 1,000 `FindAllStringSubmatch` over 85 bytes | 3.54 | 531-648 | 150-180× |
| `math/big` 300! by `Mul` | 0.009 | 3.90 | 430× |
| `math/big` fib(1000) by `Add` | 0.018 | 1.85 | 100× |
| `math/big` `String` of 1000! | 0.042 | 9.18 | 220× |
| `jsontext` 10 × `ReadToken` over 2,164 bytes | 0.326 | 43.1 | 132× |
| `jsontext` 10 × `ReadToken` + `WriteToken` | 1.19 | 135.6 | 114× |
| `bufio.Scanner` words over 115 KB | 0.48 | 50.6 | 106× |
| `container/heap` 10⁴ push and pop (methods of the program's type) | 1.94 | 107.7 | 56× |
| `text/tabwriter` 1,000 lines | 0.37 | 18.8 | 51× |
| `encoding/base64` 96 KB round trip | 0.42 | 19.9 | 47× |
| `slices.Sort` 10⁵ ints (GR6's interpreted `slices`) | 6.69 | 291 | 44× |
| `container/list` 10⁴ | 0.34 | 12.0 | 36× |

The ratios follow GR3's sketch of goi against gc: simple loops 30-60×, calls and interfaces more.
Byte-at-a-time code (`regexp`'s machine, `bufio`'s split functions) and `math/big`'s word
loops are 100-430×. These replace gc's assembly with interpreted Go. `math/big`'s, `regexp`'s
and `bufio`'s work is the package's own, so compiling the package brings them back near gc's
speed: a compiled package called from interpreted code costs one call through the invoker,
39 ns (GR1, X2). `container/heap` and `sort` call the program's interpreted methods for every
comparison, so compiling them gains less.

### 3.2 Load time at the REPL

`(go/import ...)` then a declaration naming a member (the import is completed at the first
declaration that needs it), on a shared machine (load 20-35):

| package | load (check, define) |
|---|---:|
| `container/heap`, `list`, `ring` | 0.17-0.19 s |
| `text/tabwriter`, `hash/*`, `encoding/ascii85`, `math/cmplx` | 0.3-0.5 s |
| `encoding/base64`, `bufio`, `html`, `encoding/csv` | 1.1-1.7 s |
| `flag`, `go/token`, `go/scanner`, `regexp/syntax`, `text/template/parse`, `net/textproto` | 2.0-2.9 s |
| `regexp` (with `regexp/syntax`), `math/big`, `encoding/json/jsontext`, `testing/quick` | 3.1-3.6 s |
| `compress/gzip`, `go/constant`, `image/gif`, `index/suffixarray` | 4.3-5.1 s |
| `debug/elf`, `debug/buildinfo` | 6.3, 10.0 s |

Across the 52 loaded packages the median is 2.0 s and the mean 2.1 s. For `regexp`, timed in
parts (`check-source` and `define!` wrapped): 3.1 s in all.

- The checks take 2.45 s: `slices` 0.50 s (GR6's interpretable package, checked anew in every
  session), `regexp/syntax` 1.27 s for 135 KB of forms, `regexp` 0.68 s for 106 KB.
- Defining takes 0.03 s.
- Reading the forms and making the namespaces take about 0.65 s.
- The first call builds the functions it reaches: 117 ms; the second call 0.36 ms.

So the checker runs at about 100 KB of forms per second, and it is most of a package's load. The
process with `regexp` loaded holds 475-490 MB at most, against 333-377 MB for a session with only
compiled `strings`.

### 3.3 Size, compiled instead

Each package added alone to `go/arbace/gotable.edn`. The table is generated for linux/amd64
(`arbace.go.table`), printed into a copy of the build's work module, and linked as
`bin/goi table --size` links it (stripped). Bytes over the base, 135,557,280, which is the
shipped executable:

| package | +bytes | its members | note |
|---|---:|---|---|
| `crypto/aes`, `crypto/hmac` | 0 | 1-2 | linked already |
| `text/tabwriter` | +8,192 | 1 func, 1 type, 3 methods | linked already |
| `container/heap`, `container/list`, `encoding/base64` | +20,480 each | | |
| `encoding/csv`, `os/signal` | +28,672 each | | |
| `compress/flate` | +32,768 | | |
| `net/url` | +45,056 | | |
| `runtime/trace` | +49,152 | | |
| `crypto/cipher` | +86,016 | | |
| `runtime/pprof` | +90,112 | | |
| `bufio`, `hash` | +94,208 each | | `hash`: interfaces only, 27 method entries |
| `flag` | +110,592 | | |
| `encoding/xml` | +212,992 | | |
| `math/big` | +233,472 | 142 methods | |
| `syscall` | +253,952 | 231 funcs, 71 types | |
| `regexp` | +258,048 | | |
| `go/ast` | +335,872 | 73 types, 139 methods | |
| `testing` | +389,120 | | |
| `encoding/json` | +688,128 | | |
| `log/slog` | +864,256 | | |
| `net/http` | +1,867,776 | | |
| **`text/template`** | **+18,042,880** | | `MethodByName` (exec.go) |
| **`html/template`** | **+19,185,664** | | through `text/template` |
| **the 36 to-be-compiled packages together** (§2.3, `unique` and `weak` included, their generics left out) | **+1,507,328** | 403 funcs, 185 types, 416 methods | |
| every user package compiled but the internal ones (125) | +31,150,080 | 1,077 funcs, 919 types, 2,538 methods | the templates' 17.7 MB included |

A package the program links already costs only the API it did not use. `text/tabwriter`,
`bufio`, `compress/flate`, `crypto/*` and `net/url` are among the 182 linked std packages. One
that it does not link costs its whole API (`net/http`, `log/slog`). The templates are the
exception that settles their case: compiled, their run-time `MethodByName` turns on the
linker's ReflectMethod, which keeps every exported method of every reachable type. That is the
same 17.7 MB as listing reflect's `Type.Method` (§4.2).

### 3.4 The trade

| | interpreted | compiled |
|---|---|---|
| speed | 36-430× gc's | gc's, plus 39 ns per crossing |
| executable | its forms, compressed: about 10-20% of their size (`regexp` 241 KB of forms, about 50 KB) | 8 KB-1.9 MB per package (`regexp` 258 KB) |
| REPL load | 0.2-10 s, median 2 s | none (vars on demand, S9) |
| reflect over interpreted values | goi can serve methods (GR8b) | sees RT and proxies (G8) |
| one type for both sides | own types | shared with the table's API |
| possible | 87 user packages | all but `internal/` ones (Go's rule) |

## 4. Reflect's methods

### 4.1 What the programs do

All 13 programs reflect over the *program's own* types, which goi interprets:

| program | call | the type | faithful (§5.2) |
|---|---|---|---|
| `reflectmethod1` | `TypeOf(v).Method(0).Func.Interface().(func(M))(v)` | `type M int` | no |
| `reflectmethod4` | `ValueOf(v).Method(0).Interface().(func())()` | `type M int` | no |
| `reflectmethod5`, `6` | `var h = reflect.Type.Method` (method expression), then `MethodByName` | `type foo struct{}` | no (empty) |
| `reflectmethod7` | `PointerTo(TypeOf(S(0))).MethodByName("M")`, `Func.Call` | `type S int` | no |
| `issue16331` | `ValueOf(T{}).Method(0)` | `type T struct{}` | no (empty) |
| `issue25897b` | `ValueOf(t).MethodByName("Foo")` | `*T`, `T struct{}` | no (empty) |
| `recover` | `Method(0)` on `T1 struct{}`, `T2 uintptr`, `T3`, `T4 [2]string`, `T5 [8192]byte` | several | no |
| `issue27695` | `ValueOf(&My{}).MethodByName("Run")` | `My struct{b byte}` | yes |
| `issue27695b`, `c` | `ValueOf(&T{}).Method(0)` | `T struct{...}` | yes |
| `bug424` (directory) | `t.NumMethod()`, `t.Method(j)` | `myT1`-`myT3` structs with fields | yes |
| `issue19028` (directory) | `typ.Method(i)` over `NumMethod` | `T struct{...}` | yes |

An interpreted named type's RT is a `StructOf` with goi's tag (faithful) for a struct with fields,
and its underlying type's otherwise (§5.2). In both cases the RT has no methods: reflect cannot
make a type with methods. A value of a type with methods is a proxy in an interface (§5.3). So
even reflect's real `Method` would find nothing on these values. Only goi knows their methods,
and only a faithful RT leads back to goi's descriptor (its registry). 5 programs (bug424,
issue19028, issue27695, b, c) are within GR8's reach. bug424 also has an empty `localT`, which
may push it to GR9. 8 need the type's identity from a non-faithful
RT (`M int` is `int`, `foo struct{}` is `struct {}`), which is GR9's runtime overlay (G8/GS8):
interpreted named types as real runtime types.

### 4.2 Their size, member by member

Each of reflect's six excluded members listed alone (or as a pair), linked as in §3.3:

| listed | +bytes |
|---|---:|
| `Value.Method` | 0 |
| `Value.MethodByName` | +4,096 |
| `Value.Method` and `Value.MethodByName` | +4,096 |
| `Type.Method` | +17,715,200 |
| `Type.MethodByName` | +17,715,200 |
| `Value.Methods` (go1.27's iterator) | +17,715,200 |
| all six (`Type.Methods` too) | +17,719,296 |

The linker's ReflectMethod (cmd/link's deadcode: a reachable call of `reflect.Type`'s `Method` or
`MethodByName` keeps every exported method of every reachable type) follows the interface
`reflect.Type`. The table's method expression of an interface method is a call through it.
`Value.Method` and `MethodByName` are concrete methods of `Value`: listing them marks nothing.
GR7b's +18.8 MB was measured on its branch; here all six cost 17.7 MB.

`Value.Method` without ReflectMethod is only safe for methods the program keeps for another
reason. A method the linker dropped is `runtime.unreachableMethod` in the method table, a fatal
error ("unreachable method called"), not a recoverable panic. The table's method expressions
keep every exported method of the listed types (1,035 entries), so `Value.Method(i)` is safe on
those values and only on those.

### 4.3 Options

- **(a) goi serves reflect's method lookups.** The list's `:overrides` (§6.7, as `os.Exit` today)
  point reflect's method members to goi's functions of the same signatures: `Value.Method`,
  `MethodByName`, `NumMethod`, `Type.Method`, `MethodByName`, `NumMethod`, and the iterators
  `Value.Methods`, `Type.Methods`. The overrides apply to interpreted code only: compiled code
  keeps reflect's own. Each looks at the receiver:
  - **an interpreted value** (a proxy, or a faithful RT in goi's registry): its descriptor's
    exported methods, sorted by name as reflect sorts them, the method set of the value's type
    or its pointer as the checker computes it. A method value is the interpreted method bound
    to the receiver, a Go func of the method's type made by the adapters or `reflect.MakeFunc`
    (§6.4). `Type.Method(i)` gives a `reflect.Method{Name, PkgPath, Type, Func, Index}`, its
    `Type` a `reflect.FuncOf` with the receiver first and its `Func` the method expression's
    func;
  - **a compiled value of a type with method entries in the table**: the table's entries for
    `Type.Method` and `MethodByName`, and reflect's own `Value.Method` (+4 KB, safe for these
    types: §4.2);
  - **anything else**: a Go panic naming the type ("goi: reflect.Value.Method of time.Location:
    its methods are not in the table").

  Cost: goi's code, no linker effect (nothing reaches `reflect.Type.Method`). Estimated at
  20-40 KB, about the size of GR1's `goi/call`.
- **(b) reflect's members listed.** +17.7 MB, and it serves compiled types only. The 13
  programs would still fail (§4.1).
- **(c) generated method tables for the listed types**, without goi's descriptors: the compiled
  half of (a) alone. The table already is that, 1,035 method expressions, so it costs nothing new
  but serves no interpreted value.
- **(d) the overlay first** (GR9's G8): interpreted named types as runtime types with method
  tables of trampolines into goi. reflect's own `Value.Method` would then work on them, but
  `Type.Method` still pulls 17.7 MB. So (a)'s overrides are needed after GR9 too. GR9 widens
  their reach to the non-faithful types.

Recommended: (a) at GR8b, its interpreted half on faithful types, widened to all named types by
GR9 (GC60, GC61).

### 4.4 `encoding/binary`'s byte orders

`binary.LittleEndian`, `BigEndian` and `NativeEndian` are variables of unexported types
(`littleEndian` ...), whose methods the table cannot name: a method expression of an
unexported type cannot be written outside its package. But the types implement the exported,
listed interfaces `ByteOrder` and `AppendByteOrder`, whose method expressions are in the table
(`ByteOrder.Uint32` ...). The design: a method call on a value of a compiled unexported type,
not found in the table, goes through a listed interface of its package that the type implements
and that has the method (the checker knows both method sets). A dynamic call on the interface
costs one indirect call. Size: none. Option (b), the variables' bound method values in the
table (22 entries), costs a few KB and only covers these three. The 3 programs pass with (a)
at GR8a.

## 5. The mixed boundary

1. **One type, two packages.** An interpreted package and its compiled twin (182 std packages
   are linked) have distinct types. Values cross only where a listed API names the type, or
   through interfaces. So:
   - The table's API: its 30 named unlisted packages (§2.2) are compiled, or the members that
     name them are unusable. Today: `md5.New()`'s `hash.Hash` (`bug257`), `crc32.NewIEEE()`'s
     `hash.Hash32` (`image/png`).
   - Interfaces: a compiled value whose dynamic type is an unlisted package's reaches
     interpreted code as an interface. A type assertion to the interpreted twin fails where gc's
     succeeds. The ones that matter are errors: `syscall.Errno` inside `os`'s `*PathError`
     (`errors.Is(err, syscall.ENOENT)`, `err.(syscall.Errno)`), `*url.Error`, and
     `*flate.CorruptInputError` from `compress/zlib`. Hence `syscall`, `net/url` and
     `compress/flate` compiled.
   - Package state exists twice, once compiled and once interpreted: `flag.CommandLine` (compiled
     `testing` would parse compiled flag's set), `http.DefaultServeMux`, `expvar`'s registry,
     `internal/godebug`'s settings. No listed package uses them, so the interpreted copy is the
     only one interpreted code sees. The rule "a package is compiled or interpreted, never both
     for interpreted code" keeps that true: an interpreted package never imports the compiled
     twin of another interpreted one, since the digest resolves its imports to the interpreted
     packages.
2. **Interpreted reflect-driven packages over interpreted values.**
   - Fields work: an interpreted struct's RT is a real struct with its fields' names and tags,
     goi's tag appended to the first field's (§5.2). `text/template`'s `{{.Name}} {{.Age}}` over
     an interpreted `User` gives gc's `bob 4`, and `jsontext` is exact.
   - Methods do not: `{{.Hello}}` needs `Value.MethodByName` (GR8b), and a proxied value
     (§5.3) walked by reflect shows `goi.Proxy`'s fields. (a)'s overrides of `reflect.ValueOf`
     and `TypeOf` in interpreted code would unwrap a proxy to its value, whose faithful RT leads
     back to the descriptor.
   - Interpreted *interfaces* are the hard case. `encoding/json/v2` asks
     `reflect.TypeFor[Marshaler]()` and `t.Implements(...)` for its own interfaces. An interpreted
     interface's RT is `any` (reflect cannot make interface types, golang/go#4146), so
     `Implements(any)` holds for every type and json would call `MarshalJSON` on everything.
     goi cannot give an interpreted interface a faithful RT. So `encoding/json`, `encoding/xml`,
     `encoding/gob`, `database/sql` and `net/rpc`, which reflect on their own interfaces, are
     better compiled. `json` +688 KB, `xml` +213 KB, `gob` and `database/sql` are not measured,
     likely 0.2-0.4 MB each (GC62). Compiled, they see interpreted structs' fields as gc's.
     Interpreted types' `MarshalJSON` methods stay invisible to them until G8 (GR9), the known
     difference of §5.3.
   - `reflect.TypeFor[T]` itself (F3): for a compiled T, the table's RT; for an interpreted
     faithful T, its descriptor's RT; for an interpreted interface, unsupported with that reason.
3. **Compiled reflect-driven packages over interpreted values.** Expected from `flag`'s source,
   not measured: compiled `flag`'s `PrintDefaults` would call `isZeroValue`, which does `reflect.New(reflect.TypeOf(value).Elem())`
   on the `flag.Value` it holds: for an interpreted `Value` that is goi's wrapper type, its zero a
   proxy with no descriptor, so its `String` panics. Go's `flag` then prints "panic calling String
   method on zero ..." where gc prints the default. Interpreted, `flag` gave gc's output (§2.5).
   This is why `flag` stays interpreted although compiled it costs only 111 KB.
4. **Generics across.** An interpreted package's generic code instantiated with compiled types
   works as GR6 built (`slices` over compiled values). A compiled package's generic API
   (`crypto/hkdf`, `sync.OnceValue`) is interpreted from the digest's bodies (§9.3), so its
   non-generic callees must be in the table (GC32).

## 6. Spec draft

### 6.1 The design

- **The list's classes** (`go/arbace/gotable.edn`):
  - `:packages` gains the 34 of §2.3, the reflect-driven encoders (GC62) and the speed set
    (GC64);
  - `:interpret` becomes "every interpretable package of the configuration", computed by the
    generator. The checker decides the closure per configuration, with `:out` naming the
    exceptions and their reasons, not a hand list.

  The generator classifies as §2.1 does, but from the checked packages, not from text:
  - a package is interpretable when it has no body-less declaration (but `:replace`d and dropped
    ones), all its imports are listed or interpretable, it is not `:out`, and no listed API
    names its types;
  - it reports the classes (`report.edn`) and the growth of the table.
- **`:replace`** keeps GR6's form (Go forms per package, by declaration name) for the 10
  declarations of §2.3: `internal/godebug`'s 4, `internal/cpu`'s 3, `internal/abi`'s 2,
  `go/internal/srcimporter`'s 1. **`:drop`** names the bad-linkname aliases to leave out
  (`go/types`' `badlinkname_Checker_infer`, `net/http`'s `badRoundTrip`, `badServeHTTP`). A
  body-less declaration left over is an error of the generator, not of the REPL.
- **Linkname between interpreted packages** (GC59): a body-less declaration with
  `:go/linkname "local target"`, whose target is a declaration of an interpreted package, is
  bound by goi to that declaration's cell when both are loaded: `mime/multipart`'s and
  `net/http`'s `readMIMEHeader` → `net/textproto.readMIMEHeader`. A target outside the
  interpreted packages is the generator's error, unless `:replace` gives it.
- **The forms at run time** (GC55): the interpretable packages' forms are in the configuration's
  digest, as GR6's are. Positions are kept at `:lines` (traces). Docs are kept as GC12 has them.
  Measured 1.4-2.4 MB compressed for all 174 per configuration, on top of 337 KB; GR8a records the
  real figure. A package is decompressed, read and checked on its first import (GR6's
  `load-digest-package!`). Its checked declarations could be cached per session, so `slices`'
  0.5 s is paid once, not per importer: it is checked again today when a second package imports
  it in a new session.
- **The box**: tamago's forms come from `tamago/goroot` (committed: 371 packages). The generator
  classifies them for `tamago/amd64` and `tamago/arm64` the same way: `syscall` is TamaGo's,
  `os/signal` and `runtime/pprof` differ. So the four configurations have their own interpretable
  sets, as their tables differ today.
- **Fixes in goi** (GR8a's):
  - F1: labels on short variable declarations, in the builder (`goto` into a `let`'s binding, as
    the converter writes it);
  - F2: the table's members whose signatures name instances of interpretable generic types
    (`iter.Seq[string]`). The digest names the instance, its RT the function type, so the
    compiled func value crosses unchanged (GC63);
  - F3: `reflect.TypeFor` (GC62);
  - F4: the builder's location of a function literal's captured variable in an assignment's
    right side (`json/v2`);
  - F5: covered by `:replace`.
- **Calls on compiled unexported types through a listed interface** (§4.4): the checker resolves
  `binary.LittleEndian.Uint32` to `ByteOrder.Uint32`'s method expression on the value.
- **Reflect's method overrides** (GR8b, §4.3 (a)): `:overrides {"reflect" {"Value.Method" ...}}`,
  goi's functions in `go/arbace/goi/reflect.ae` (new). They need the registry from a faithful RT
  to its descriptor (§5.2, built), the method sets of interpreted types (the checker's `lookup`,
  built), the table's method entries by receiver type (built: `goi.Lookup`), and the
  `ValueOf`/`TypeOf` overrides that unwrap proxies in interpreted code. `Value.Method` and
  `Value.MethodByName` leave `:exclude` (+4 KB); `Type.Method`, `MethodByName` and both `Methods`
  iterators stay excluded and become overrides.

### 6.2 Milestones

**GR8a, std at the REPL** (6-8 days):

1. F1, F2, F4 in goi; F3 as GC62 decides (1.5-2 days);
2. the generator's classification, `:interpret` computed, `:replace`, `:drop`, the digest with
   the interpretable forms, the size recorded (2 days);
3. linkname between interpreted packages (0.5 day);
4. the table's additions (§2.3, GC62, GC64), their sizes recorded (0.5 day);
5. calls through a listed interface (§4.4) (0.5 day);
6. `test/goi/std.ae`: every user std package of the configuration imported at the REPL and one
   member called, the out-of-scope ones refused with their reasons; the check recorded again
   (1-2 days);
7. the box: the tamago configurations' classes, `bin/goi check --box`'s subset (0.5 day).

Exit criteria:

- no program of `bin/goi check` unsupported for "an import neither in the table nor
  interpreted": the 29 of §1.1 pass or are known with their reason;
- the 3 `encoding/binary` programs pass;
- `test/goi/std.ae` passes on linux/amd64 and in the box: 169 of 180 user packages importable,
  11 refused with their reasons;
- the executable's growth recorded (expected +3-5 MB: the table's additions 1.5 MB, the encoders
  and speed set 1.6 MB, the forms 1.4-2.4 MB);
- no program passing at GR7b regresses;
- the essential gate, the fixpoint.

**GR8b, reflect's methods** (3-4 days):

1. goi's overrides (§4.3 (a)) for interpreted faithful types and compiled listed types (2 days);
2. `ValueOf`/`TypeOf` unwrapping proxies in interpreted code (0.5 day);
3. tests: the REPL (`strings.Builder`'s methods by name, an interpreted struct's,
   `text/template` calling an interpreted method), and the check (1 day).

Exit criteria:

- the 5 faithful programs of §4.1 pass;
- the other 8 are recorded as GR9's (unsupported, "reflect of a non-faithful interpreted type:
  GR9 (G8)");
- `text/template`'s `{{.Hello}}` gives gc's `hi cy`;
- the executable grows by goi's code only (no ReflectMethod: `Type.Method` stays out of the
  table).

Together 9-12 days. They can run in sequence on one branch (GR8b needs GR8a's F2 for
`text/template`), or GR8b beside GR8a's steps 2-7.

### 6.3 What "every std package importable" means

A user package (180 for linux/amd64) is importable when `(go/import [x "path"])` and a call of one
of its members work at the REPL, whether it is compiled or interpreted. The 11 out of scope are
refused at the import with their reason ("goi: unsafe is the checker's", "goi: embed:
go:embed is out of scope (GS14)", "goi: weak is generic over the runtime: out of scope").
Internal and vendored packages are not importable by Go's rule, and goi refuses them likewise.

## 7. Proposals for the user (GC55-GC64)

- **GC55 Where interpreted std's forms come from.**
  - (a) In the configuration's digest, all interpretable packages, positions at lines (1.4-2.4 MB
    compressed per configuration).
  - (b) A chosen REPL set in the digest (about 30 packages, 0.4 MB). The rest are loaded from a
    forms tree beside the executable (`ARBACE_PATH`, written by the build, and the box's store).
  - (c) All from the forms tree, no embedding: the executable unchanged, but it needs its tree,
    which the box must carry in its store.

  *Recommended: (a)*: one self-contained executable, as the box needs; about 1.5% of it.
- **GC56 The compiled core's growth.**
  - (a) The 34 of §2.3: the runtime-bound, those whose types the table's API names, and the
    crypto module (+1.5 MB measured).
  - (b) Only what the check needs (`syscall`, `runtime/trace`, `runtime/pprof`, `hash`,
    `math/big`; about +0.7 MB). The other runtime-bound packages unsupported, and crypto
    interpreted where possible: it is not, given assembly and FIPS.
  - (c) (a) and every interpretable package the program already links (`bufio`, `compress/*`,
    `encoding/base64`, `container/list`, `text/tabwriter`, `os/user`; about +0.3 MB).

  *Recommended: (a)*, with (c)'s speed-relevant ones under GC64.
- **GC57 `math/big`.**
  - (a) Compiled (+233 KB): `crypto/rand.Int` and `x509` name `*big.Int`, and it is 100-430×
    slower interpreted.
  - (b) Interpreted, its 6 assembly functions replaced by their Go versions (`addVV` →
    `addVV_g` ...; measured right): `crypto/rand.Int` and `x509`'s big fields unusable from
    interpreted code (two `big.Int` types).

  *Recommended: (a)*.
- **GC58 The internal packages interpreted std needs.**
  - (a) Interpreted, with `:replace` of their runtime hooks: `internal/godebug`'s 4 (GODEBUG read
    from the environment once; measured right), `internal/cpu`'s 3 (no features),
    `internal/abi`'s 2, `go/internal/srcimporter`'s 1.
  - (b) goi-written shim packages replacing them whole.
  - (c) Their importers unsupported: 15 user packages through `internal/godebug` alone
    (`net/http`, `html/template`, `archive/zip`, `go/build` ...).

  *Recommended: (a)*: 10 short declarations; the packages keep their own code.
- **GC59 `go:linkname` between interpreted packages.**
  - (a) goi binds a body-less declaration's linkname to the target's cell when the target is
    interpreted (`net/textproto.readMIMEHeader` for `net/http` and `mime/multipart`); bad-linkname
    aliases dropped by `:drop`.
  - (b) `:replace` copying the target's body.
  - (c) §1.1's letter: unsupported.

  *Recommended: (a)*: no copy to keep in step with std; the program's own linknames stay out of
  scope (§1.1), only std's interpreted packages use this.
- **GC60 Reflect's methods.**
  - (a) goi's overrides of reflect's method members, served from descriptors (interpreted
    faithful types) and the table's method entries (compiled listed types); `Value.Method` and
    `MethodByName` listed (+4 KB), `Type.Method`, `MethodByName`, `Methods` overridden, not
    listed.
  - (b) reflect's members listed (+17.7 MB): compiled types only, and the 13 programs still
    unsupported.
  - (c) Nothing until GR9's overlay.

  *Recommended: (a)*, GR8b.
- **GC61 Reflect of interpreted types that are not faithful** (named non-struct types, empty
  structs: 8 of the 13 programs).
  - (a) Moved to GR9: G8's overlay gives them runtime types, (a)'s overrides then reach them, and
    GR9's exit lists the 8.
  - (b) GR8 makes them faithful by a tagged wrapper struct: their `Kind` would change, so
    rejected.
  - (c) A proxy type per interpreted type (a `StructOf` over `goi.Proxy` tagged with the type), so
    `TypeOf` leads to the descriptor: `Kind` again wrong (`Struct` for `M int`).

  *Recommended: (a)*.
- **GC62 `reflect.TypeFor` and the reflect-driven encoders** (`encoding/json`, `xml`, `gob`,
  `database/sql`, `net/rpc`).
  - (a) `TypeFor` as GC32 (b) (the RT of T's descriptor, compiled or interpreted faithful;
    interpreted interfaces unsupported with the reason). `encoding/json`, `encoding/xml` and
    `encoding/gob` compiled in the table (+688 KB, +213 KB, gob to measure). `database/sql` and
    `net/rpc` interpreted with their interfaces' checks unsupported until G8.
  - (b) All interpreted, with goi faking interface RTs (a marker type per interpreted interface,
    `Implements` and `Kind` overridden): a large surface, every reflect call to vet.
  - (c) All interpreted with `TypeFor` alone: their `Implements(any)` true for every type, so
    wrong results. Rejected.

  *Recommended: (a)*.
- **GC63 The table's members over interpretable generic types** (F2: 18 members, `strings` and
  `bytes`' `Seq` functions, reflect's iterators).
  - (a) The digest names `iter.Seq[string]` and the like as instances of the interpreted `iter`,
    whose RT is the func type, so the compiled func values cross unchanged.
  - (b) `iter` listed compiled with `:instances` of the types the table names.
  - (c) Those members unsupported: `go/*`, `testing` and `text/template` stay unimportable.

  *Recommended: (a)*.
- **GC64 Interpreted or compiled when both are possible.**
  - (a) Interpreted by default; compiled by GC56 and GC62, plus a speed set whose work is the
    package's own and which is 100-180× slower interpreted: `regexp` (+258 KB), `bufio`
    (+94 KB), `compress/gzip` with `flate` (`flate` is in GC56), `encoding/base64` (+20 KB) and
    `encoding/csv` (+29 KB), together about +0.5 MB. Never the templates (+18-19 MB) or
    packages that `reflect.New` interpreted values (`flag`, §5).
  - (b) Everything interpretable interpreted, smallest executable, `regexp` at 150×.
  - (c) Everything compiled but the templates and `flag`: about +12-13 MB (the 125 cost 31.2 MB
    with the templates' 17.7 MB and their own), REPL loads of none.

  *Recommended: (a)*: the REPL's common text work at compiled speed for 0.5 MB, the long tail
  interpreted.

## 8. Sources

- go1.27.1's std (TamaGo's toolchain, `/root/tamago-go/src`), converted by repr/go for
  linux/amd64 (the table's cache key `597d808a01c97cfb`); `tamago/goroot` for tamago;
  `$GOROOT/test`'s programs named in §1.1 and §4.1.
- cmd/link's deadcode (`src/cmd/link/internal/ld/deadcode.go`: ReflectMethod, `reflectSeen`),
  `runtime.unreachableMethod` (`src/runtime/iface.go`), Go's internal-package rule (cmd/go,
  "use of internal package ... not allowed", reproduced by the table's build).
- doc/go/GOI-SPEC.md §1.1, §5.2, §5.3, §6, §9.3 and its "As built" sections GR1 (S39, the
  ReflectMethod finding), GR6 (`:interpret`, `:replace`, GC29, GC32), GR7a.

## Appendix A: the experiments

All in `.tmp/goi-gr8-survey/`, not tracked:

- `tools/scan.py`, `tools/classify2.py`, `tools/closure.py`: the scan and the classification of
  §2. Run with `python3 -I` on the forms' text; the output is `classes2.json`, committed as
  `doc/go/GR8-STD.edn`.
- `linked-std.txt` (`go list -deps -f '{{if .Standard}}...'` over the work module's
  `./cmd/...`), `refd.txt` (the unlisted packages the digest's files import),
  `digest-linux-amd64.edn` (the digest inflated).
- `stdroot/`: a copy of linux/amd64's std forms, edited for the experiments:
  - `regexp/backtrack.ae`: F1 worked around, `inst` declared before `goto :Skip`;
  - `math/big/arith_decl.ae`: the 6 assembly functions given bodies calling their `_g` versions;
  - `internal/godebug/godebug.ae`: the 4 runtime hooks replaced as §2.3.
- `src/*.go` and their forms (`conv.sh`: `godump -files -goos linux` then `arbace.repr.go read`):
  - `rx.go`, `rx2.go` (`regexp`), `bg.go` (`math/big`), `jt.go` (`jsontext`), `js.go`
    (`encoding/json`, stops at F4);
  - `misc.go` (`container/heap`, `container/list`, `encoding/base64`, `bufio`, `text/tabwriter`,
    `slices`);
  - `tt.go` (`text/template`, `flag`);
  - `z.go`, `g.go`, `g2.go`, `g3.go` (F1's reduction), `t1.go`, `t2.go` (`reflect.TypeFor`).

  Each was built by gc (`go build`, the toolchain above, `CGO_ENABLED=0`) and run by goi
  (`arbace -m arbace.go.run ROOT NS bench` with `ARBACE_PATH=stdroot`).
- `tools/loadone.sh`, `loads.tsv`: §3.2's REPL loads of every interpretable user package;
  `repl3.ae`: `regexp`'s load in parts.
- `tools/size.sh`, `lists/*.edn`, `sizes.tsv`, `sizes2.tsv`: §3.3 and §4.2. Each list variant
  went through `arbace.go.table --config linux/amd64=...`, then `bin/repr-go print` into a copy of
  `target/arbace-go/work-amd64/mod`, then `go build` with `bin/goi table --size`'s flags; the
  base list reproduces the shipped executable's 135,557,280 bytes.
