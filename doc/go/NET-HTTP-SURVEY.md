# net/http for Arbace's Go: survey and plan

Status: survey and plan, 2026-10-29, at the user's request (complete Arbace's Go with `net/http`, client and
server, HTTP/1.1 and HTTP/2, and its subpackages `httptest`, `httputil`, `cookiejar` as this survey judges).
Nothing here is adopted: no code under `arbace/`, `go/`, `bin/` or `test/` changed. The decisions are the
user's, as proposals NH1-NH10 (§7); the milestones are NH0-NH6 (§6). The build comes after the tag
`arbace-reaps-3-crops-v2`: listing packages changes the shipped executable and needs a re-seed.

Base: branch `nethttp-survey`, from main `fdf939b5`. The measurements are light probes (§2, Appendix A), run
on the shared amd64 host with TamaGo's go1.27.1 (`/root/tamago-go`, `CGO_ENABLED=0`, a `GOCACHE` under the
worktree's `.tmp/`): `go list -deps`, and `go build` of tiny stand-alone probe programs (0.6 to 2.6 s each,
under 260 MB). No Arbace executable was built or run, no lock taken.

Contents: Executive summary · 1 Dependencies · 2 Cost · 3 What works · 4 Checks · 5 What is not in scope ·
6 Plan (NH0-NH6) · 7 Proposals (NH1-NH10) · 8 Risks · 9 Sources · Appendix A: the scratch.

## Executive summary

- **`net/http` is not new to goi: it is importable today, interpreted.** GR8a (doc/go/GR8-SURVEY.md, GOI-SPEC
  "As built: GR8a") made every std package that is not listed, `:out` or `cmd/` an interpretable package whose
  forms are embedded: `net/http` with its closure is 28 packages and 2.9 MB of forms, 8.48 s to import at the
  REPL, and interpreted std runs 36 to 430 times slower than gc (GR8 §1). Its `:drop` (`badRoundTrip`,
  `badServeHTTP`) and the `readMIMEHeader` linkname are already handled. What is missing is not the import but
  a server worth running (compiled, with real HTTP/2 and TLS speed), a check that runs HTTP at all (§4: the
  three programs of `$GOROOT/test` that name `net/http` are errorcheck programs; goi runs no `_test.go`), and
  the callbacks from compiled `net/http` into interpreted handlers measured. I did not run an interpreted
  server (no Arbace run); whether one works today is the first thing NH0 measures.
- **The dependencies (§1).** `net/http`'s closure for linux/amd64 is 189 packages; 207 are linked by the listed
  packages already. New to the executable: 19 packages: `net/http`, `net/http/httptrace`, `net/textproto`,
  `mime`, `mime/multipart`, `mime/quotedprintable`; `net/http/internal` and four more `net/http/internal/*`
  (**HTTP/2 is bundled inside std as `net/http/internal/http2`, not vendored from x/net, since go1.26**);
  and from `vendor/golang.org/x`: `net/http/httpguts`, `net/http/httpproxy`, `net/http2/hpack`, `net/idna`,
  `text/unicode/norm`, `text/unicode/bidi`, `text/transform`, `text/secure/bidirule`. `dns/dnsmessage`,
  x/crypto and x/sys/cpu are linked already (by `net`, `crypto/tls`). The sub-packages add nothing new but
  themselves (`httptest` also imports `flag`, which stays interpreted).
- **The size (§2).** Stand-alone probes, stripped, against a base that links the 96 listed std packages (3.51
  MB): `net/http` with `httptrace`, `mime*`, `textproto` and their whole exported API referenced as the table
  does: **+4.36 MB**; with `httputil` and `cookiejar` +4.51 MB; with `httptest` **+4.63 MB** (all nine
  packages). The table's invokers and stubs: about +0.2 to 0.4 MB. The forms of what becomes compiled leave
  the embedded interpretable set (about -0.2 to -0.4 MB). Estimate for the shipped executable
  (177,778,848 bytes today): **+4.6 to 5.4 MB, 2.6 to 3.0%**. GR8's own measurement of `net/http` alone, +1.87
  MB of the table, is lower than this probe's +3.66 MB for `net/http` alone; unexplained, so the number to
  trust is NH2's `bin/goi table --size`. No ReflectMethod trap: `net/http` and its closure never call
  `Method` or `MethodByName` (the templates' +18 MB trap does not apply).
- **Interpreted handlers from compiled `net/http` (§3.2): plausible, by the machinery GR9a-c built, unmeasured
  for HTTP.** An interpreted type's runtime type carries a method table of stubs by signature family;
  `ServeHTTP(ResponseWriter, *Request)` and `RoundTrip` become families when `net/http` is listed. A conn
  goroutine of the compiled server calls the stub, which enters goi's thread for that goroutine (a jrt `Thread`
  adopted on first use, GOI-SPEC §8.1). An Arbace fn as a handler is plausible through `http.HandlerFunc`
  (a named function type: `reflect.MakeFunc`, GOI-SPEC "As built: GR5" amendment 6) or through `reify`.
  The risks: per-goroutine thread cost with thousands of connections or HTTP/2 streams, the 16-slot default of
  a stub family, and assertions on compiled concrete `ResponseWriter` types (`Flusher`, `Hijacker`, `Pusher`).
- **The checks (§4).** `$GOROOT/test` cannot be the corpus. Recommended: a new corpus of Arbace's own
  programs (`test/nethttp/`, 40 to 60), each judged against gc's run of the same text (goi's reference), run at
  goi's eval level and at the AOT level (`bin/goi check --aot`), all offline over loopback; and, later,
  `net/http`'s own tests for the packages without `testing/synctest` (which is `:out`: 17 of the 39
  `net/http` test files and 228 uses) by a small `testing` driver, in AOT.
- **The plan (§6): NH0-NH5 about 10 to 15 working days, with the optional NH6 (std's own tests) 15 to 23.**
  The shipped change is one listing commit (NH2) and fits the re-seed that carries JN10's, if NH2 is ready
  then; C23's C3 and later re-seeds are separate.
- **Recommendations (§7):** NH1 (a) client and server with the bundled HTTP/2, `httptrace`, `httputil`,
  `cookiejar` and `httptest`; NH2 (a) all of them listed compiled; NH3 (c) the new corpus first, std's tests
  later; NH5 (a) a budget of 6 MB; NH6 (b) the listing batched with JN10's re-seed, ahead of C23's.

## 1. Dependencies

### 1.1 What `net/http` needs beyond the listed packages

`go list -deps` of `net/http`, `httputil`, `httptest`, `cookiejar` for linux/amd64, compared with the closure
of the 96 listed packages less the program's own (`gotable.edn` `:packages` is 96 entries, 93 std ones; the
header's "93" counts those without `arbace`, `gcsyntax`, `godump`).

| package | non-test source | files | role |
|---|---:|---:|---|
| `net/http` | 558 KB | 29 | the package (client, server, mux, cookies, `fs`, `httptrace` hooks) |
| `net/http/internal/http2` | 365 KB | 18 | **HTTP/2, bundled in std** (go1.26+): server, transport, frames, flow control |
| `net/http/internal`, `.../ascii`, `.../httpcommon`, `.../httpsfv` | 17, 1, 18, 18 KB | 6 | chunked codec, helpers, header rules, structured fields |
| `net/textproto` | 34 KB | 5 | MIME headers, `readMIMEHeader` (linknamed by `net/http` and `mime/multipart`) |
| `mime`, `mime/multipart`, `mime/quotedprintable` | 33, 29, 7 KB | 11 | types, multipart forms, encodings |
| `net/http/httptrace` | 9 KB | 1 | client trace hooks (named by `Request.Context`) |
| `vendor/golang.org/x/net/http/httpguts`, `.../httpproxy` | 10, 10 KB | 3 | header validity, proxy environment |
| `vendor/golang.org/x/net/http2/hpack` | 45 KB | 5 | HPACK for the bundled HTTP/2 |
| `vendor/golang.org/x/net/idna` | 346 KB | 5 | IDNA for host names |
| `vendor/golang.org/x/text/{unicode/norm, unicode/bidi, transform, secure/bidirule}` | 462, 196, 22, 10 KB | 17 | what idna needs (large tables) |
| `net/http/httputil` | 52 KB | 4 | `ReverseProxy`, dumps |
| `net/http/cookiejar` | 20 KB | 2 | `Jar` |
| `net/http/httptest` | 30 KB | 3 | `Server`, `ResponseRecorder`; imports `flag` and `net/http/internal/testcert` |


New to the program, all else already linked: **19 packages, about 2.3 MB of source**, 1.1 MB of it the vendored
x/net and x/text parts (idna with the Unicode tables is the bulk). Already linked and not new: `dns/dnsmessage`
(by `net`), `x/crypto/*` and `x/sys/cpu` (by `crypto/tls`), and the crypto internals. Generic packages
(`cmp`, `maps`, `slices`, `iter`, `container/list`) are imported by the compiled `net/http` as std's own
compiled copy; none appears in `net/http`'s exported API (`grep` of the API found no `iter.Seq`), so the
table needs no `:api-interpreted` addition.

The user's list of possible dependencies, answered: `mime`, `mime/multipart`, `mime/quotedprintable`,
`net/textproto` (needed); http2 **bundled**, `hpack`, `idna`, `http/httpguts`, `httpproxy` (needed); anything
else: `net/http/httptrace`, the four `net/http/internal/*`, `x/text`'s four packages. `runtime/goos` is
reached only on tamago (`go list` for linux shows it under TamaGo's tree because of `internal/...`; it is
`:out` and not a dependency of a listed package on linux).

### 1.2 Vendored paths and the table, repr/go's reader

Nothing new is needed. `vendor/golang.org/x/...` and `internal/` paths are not importable by Go's rule, so the
table never names them (`importable?` in `arbace/go/table.ae`), and goi refuses them at an import. A listed
package's digest reaches the unexported and internal types its exported declarations name (GOI-SPEC §6.5),
and `:resolve` reads a package for the declaration layer only. The std's vendor directory is already
resolved by `table.ae` (`available`: `path`, else `vendor/path`) and by the loader for the interpretable
packages (GR8a: the 53 internal and vendored packages interpreted with the user packages, among them
`net/http/internal/*` and `vendor/golang.org/x/net/...`). For the compiled `net/http` they are
compiled from GOROOT as the std's other packages are (the program's printed module imports std from
TamaGo's tree; only the program's own packages are printed).

For tamago the forms are `tamago/goroot/go/...` (the vendored directory is spelled `vendor/golang_org/x/...`
there, with `http`, `http2`, `idna`, `nettest`, `text`, `crypto`, `sys` present, and `net/http` with its
`internal/*` and `testcert`). The linux forms are repr/go's native read of std at build time, cached by the
list's and the toolchain's hashes.

### 1.3 The tamago configurations

The list is one for the four configurations (linux and tamago, amd64 and arm64). `GOOS=tamago go list -deps
net/http` succeeds with the same closure (net has TamaGo's hooks: `dnsconfig_tamago.go`, `error_tamago.go`),
and the forms of `net/http` are in `tamago/goroot/go`. Listing it therefore lists it in the box's table too
(the guest image grows by the same order, about 4.6 MB). The box is parked (the user, 2026-10-26): its build
and checks are not run for this work; NH2 only requires the table generator to succeed for all four
configurations (`bin/goi table --configs ...` runs on the tools' host). A caveat for the box when it is
revisited: TamaGo's runtime has no netpoll (doc/go/BOX-NOTES.md around line 1618, "a netpoll for TamaGo in
the runtime overlay"): `net/http` in the box needs the box's networking, which is not this survey's.
`:exclude-in` excludes members by configuration, not packages (NH9).

## 2. Cost

### 2.1 The executable

Probes (Appendix A), stand-alone, `-ldflags='-s -w'`, linux/amd64. The base program blank-imports the 96 listed
std packages (3,510,432 bytes). Each probe adds, with the exported API of the packages referenced the way the
table references it (every exported function, variable and method expression of every non-generic type: 301
to 384 references), so no dead-code elimination hides the API:

| probe | bytes | over the base |
|---|---:|---:|
| `mime`, `mime/multipart`, `mime/quotedprintable`, `net/textproto` | 4,206,752 | +696,320 |
| + `net/http`, `httptrace` (**core**) | 7,868,576 | **+4,358,144** |
| core + `httputil`, `cookiejar` | 8,024,224 | +4,513,792 |
| core + `httputil`, `cookiejar`, `httptest` (**all**) | 8,143,008 | **+4,632,576** |
| a usage program (httptest TLS server, client with a jar, reverse proxy, `ListenAndServe`) | 7,590,048 | +4,079,616 |

So `net/http` and its HTTP/2, hpack, idna and x/text is +3.66 MB; the three sub-packages together +0.27 MB
(httputil and cookiejar +0.16, httptest +0.12). Unstripped, the probe with the whole API is 11.7 MB over a 5.1 MB
base.

The shipped program adds to these:

- **The table's invokers and adapters** (GOI-SPEC §6.4, GS4): the new API is about 380 members; at the
  `:min-sharing 2` rule the invokers cost about 3.7 KB each (GR1: 231 invokers, 856,064 bytes). If
  120 to 160 of the new signatures are shared, +0.15 to 0.3 MB.
- **The stubs** (GOI-SPEC "As built: GR9a-c"; 225 bytes a stub, 16 slots a family): `net/http` adds the listed
  interfaces' method signatures as families (`Handler`, `ResponseWriter`, `Flusher`, `Hijacker`, `Pusher`,
  `RoundTripper`, `CookieJar`, `FileSystem`, `File`, `Dialer`s ...): about 15 families, 16 slots each: 54 KB;
  64 slots for `ServeHTTP` is +11 KB.
- **The digests** (declarations without bodies, zlib): not measured; +0.1 to 0.3 MB, set against the forms
  that stop being embedded: a listed package is no longer interpretable, so the forms of `net/http`, the
  bundled `http2`, `mime*` and `textproto` (about 1.5 MB uncompressed of the 2.9) leave the embedded set (GR8:
  the interpretable forms are 1.4 to 2.4 MB compressed in all). The x/text, idna and hpack forms stay (still
  interpretable, now unneeded).

**Estimate: +4.6 to 5.4 MB on 177,778,848 bytes (the build in `target/arbace-go/amd64`, 2026-10-29): 2.6 to
3.0%.** The lower bound is GR8's +1.87 MB for `net/http`'s table alone, which the probe does not support
(see Executive summary). The comparison with other growth: GP7's go/parser +1.06 MB; GR8a's 34 compiled
packages +1.5 MB; the templates +18 MB if listed (not applicable here, §1.1).

### 2.2 Build time

Go compiles the new packages in about 2 s on this host (the probes took 0.6 to 2.6 s wall cold for the whole
closure, 64 cores). The link of the 178 MB program grows by 3%. The new cost is the table generator: the
listed packages are read by repr/go natively for linux and cached by hash (`.tmp/goi-table/`), checked by the
declaration layer, and their digests and invokers written. By the order of GP7's and GR8a's builds (a few
packages of that size): a cold generator run of a few minutes, once; seconds when the key is unchanged. The
gate-build (about 8 minutes for the two Go-hosted builds when something changed) grows by that. Estimates, to
be measured at NH2.

## 3. What works

### 3.1 The runtime facts that bear on HTTP

- **Goroutines**: real Go goroutines (GOI-SPEC §8.1): compiled `net/http`'s conn goroutines, timers and
  `context` are the runtime's. The program keeps a ticker pending (`jrt.RunMain`), so Go's deadlock detector
  never fires (§8.5): a server that blocks forever blocks the REPL's call, as an Arbace `@(promise)` does.
- **net's polling on Linux**: Go's own `internal/poll` and netpoller over epoll; `overlay/go` patches the
  runtime (`runtime.ae`, `proc.ae`, `runtime2.ae`), not netpoll or the net package; nothing in the repository
  documents a loopback listener from the program, but `net` is listed (GR1) and the `crypto/tls` check
  (`test/goi/calls.ae`) exists. Unverified: a `Listen`/`Dial` pair in the executable; NH0 does it first.
  (TamaGo's runtime has no netpoll: the box is a different matter, §1.3.)
- **TLS**: `crypto/tls` and `crypto/x509` are listed and compiled, and so is the crypto module. Offline checks
  need no roots: `httptest.NewTLSServer`'s certificate is `net/http/internal/testcert` and its client trusts
  it. `x509roots/` is Mozilla's bundle for **jrt's** connections (`jrt.TLSRoots`); the fallback is not
  installed (`x509.SetFallbackRoots` is not called), so a Go-forms client reaching a public HTTPS server uses
  the host's `/etc/ssl` (Alpine's `ca-certificates`) as gc's would. Offline, not an issue (NH8).
- **Timers**: Go's. `time.Timer`, `context.WithTimeout`, server timeouts are compiled; interpreted code sees
  them through the table (GOI-SPEC §8.4).
- **Signals**: `os/signal` is listed. `http.Server.Shutdown` after `signal.Notify` is the usual pattern and is
  plain table calls; the host's SIGPIPE handling is Go's runtime's.

### 3.2 A server from Go forms at the REPL

The mechanism exists (GOI-SPEC "As built: GR9a-c", amendments of GR5). Walking one request:

1. Go forms define `type h struct{...}` with `func (h) ServeHTTP(w http.ResponseWriter, r *http.Request)`. The
   type has a runtime type of its own (`ArbaceNamed`), and its method table is filled once with **stubs**: one
   per method, of its signature's family (the families: every listed interface's method signatures and
   `:common`). `http.Handler`'s `ServeHTTP` is a family once `net/http` is listed (it is not today: with
   `net/http` interpreted, interpreted code calls interpreted code and no stub is involved).
2. `http.ListenAndServe(addr, h)` or `srv := &http.Server{Handler: h}` is a table call; `h` converts to the
   interface (a wrapper of a listed interface, or the content itself since GR9b).
3. A conn goroutine of the compiled server calls `h.ServeHTTP(w, r)` through the itab: the stub, `EnterSlot`
   (`go/arbace/goi/stubs.ae`) takes `CurrentThread()` (the goroutine's goi thread; "Arbace code it calls adopts
   a jrt `Thread` on first use", GOI-SPEC §8.1), enters a frame of the method, and runs interpreted code.
4. Interpreted code calls `w.Header().Set(...)`, `w.WriteHeader`, `io.WriteString(w, ...)`: interface methods of
   a listed interface, by the table's method expressions (§6.1). `w` is a compiled `*http.response`
   (unexported, not in the table): its methods are reached through the interface's expressions, which is the
   case `sort.Sort` of a compiled `sort.Interface` already exercises, and `fmt`'s `Formatter`.
5. Panics propagate through compiled frames as Go panics (§7.5), so `http.Server`'s recover per request works
   and `http.ErrAbortHandler` is the table's variable.

`RoundTripper` (`Transport: myRT`) is the same in the other direction: the compiled `http.Client` calls the
stub. `CookieJar`, `FileSystem`, `File`, `Handler` wrappers: same.

**An Arbace fn as a handler** is plausible by two routes, neither built for HTTP: (i) `(http/HandlerFunc f)`: a
conversion of an Arbace `IFn` to a named function type, which GR5's amendment 6 gives `reflect.MakeFunc` (a
typed adapter if unnamed); `http.HandlerFunc` has a `ServeHTTP` method, so it is a `Handler` with no stub at
all. (ii) an Arbace object implementing `http.Handler` through G13's `reify` of Go interfaces (GR7b). Route (i)
is the idiom; cost: a `MakeFunc` call, about microseconds, against a request's tens of microseconds.

**Callbacks into interpreted code from compiled goroutines**: this is the design's normal case, not an
exception. What it costs and where it can break, to be measured at NH0/NH3:

| question | expectation | how it is checked |
|---|---|---|
| a conn goroutine calls a stub | adopts a `Thread` at the first call; one thread per goroutine | a server handling 1,000 sequential and 200 concurrent requests, thread count and memory |
| HTTP/2: a goroutine per stream | the handler runs on the stream's goroutine; many short-lived goroutines adopt a `Thread` each | 500 concurrent streams on one connection |
| the stub slots | 16 per family by default; an interpreted handler type uses one slot of `ServeHTTP`; a method with no slot left panics at its call | a program defining 40 handler types; raise `ServeHTTP` to 64 (NH7) |
| `w.(http.Flusher)`, `Hijacker`, `Pusher`, `io.ReaderFrom` | an assertion on a compiled concrete type to a listed interface: reflect's `Implements` on the content | server-sent events, `Hijack` (websocket-like), `http.NewResponseController` |
| interpreted `http.Request` use after the handler returns | Go's own rules | none |
| `context` cancellation, `r.Context().Done()` in a select | channels are real | a handler that waits for client disconnect |
| `Transport`'s `DialContext` as an interpreted closure | typed adapter `func(context.Context, string, string) (net.Conn, error)` | a client dialing through an in-process pipe |

The performance expectation is a request in the tens of microseconds of compiled work plus the interpreted
handler's own time (goi: 36 to 430 times gc on compute, but a handler is mostly calls into compiled code).

## 4. Checks

### 4.1 `$GOROOT/test`

Three programs name `net/http`, all errorcheck, none runs a server: `fixedbugs/bug358.go` (an unused import's
error order; passes in both goi's levels), `fixedbugs/issue25727.go` (an unexported field of `http.Server`:
`.doneChan undefined`; passes in goi's eval check, "no forms" in the AOT one: the converter refuses an invalid
program), `fixedbugs/issue20298.go` (errors order). `test/stress/runstress.go` and `parsego.go` are tools, not
in the check. The listing keeps all three passing; `issue25727`'s expectation depends on the digest keeping
`doneChan` unexported and absent from the exported API, which it is. The corpus of goi's check is thus no
witness: the HTTP check must be Arbace's own.

### 4.2 `net/http`'s own tests

Counts in Go 1.27.1 (non-`testdata` `_test.go`): `net/http` 39 files, 622 tests, 34,419 lines; the bundled
`internal/http2` 24 files, 396 tests, 16,920 lines; `httptest` 15, `httputil` 45, `cookiejar` 17, `httptrace`
2, `mime` 20, `mime/multipart` 33, `mime/quotedprintable` 5, `net/textproto` 26, `net/http/internal` 11.
Facts that bear:

- goi runs **no `_test.go`** (GOI-SPEC §1.1, S45): there is no `go test` driver; the two directory tests that
  hold one pass without it.
- **`testing/synctest` is `:out`** (the runtime's bubbles) and `net/http` uses it: 17 test files and 228
  calls of `synctest.Test`/`Run`, most of `http2`'s transport and server tests. Those tests cannot run in goi
  or its AOT level; they run only as gc's.
- Many tests use `internal/testenv`, `internal/nettest`, `httptest` with loopback, `os/exec` (re-executing the
  test binary), and `-short` or environment gates.
- External tests (`package http_test`) use the package's `export_test.go`, which exposes internals: an
  interpreted or AOT run needs the test files read with `--tests` (repr/go has the mode) and the package built
  together with them.

Realistic scope, if wanted: the small packages first (`mime`, `mime/multipart`, `quotedprintable`,
`net/textproto`, `cookiejar`, `httputil`, `httptest`: 163 tests, about 8,000 lines, no synctest), in AOT
(`arbace.build.go` builds the forms with the test files and a `testing.MainStart`-style main; the reference is
gc's `go test` of the original), the failures in recorded classes. `net/http` itself: the 600 tests less
synctest's and the exec-based, a selection of a few hundred; days of classing, not a check to put in the
essential gate. That is NH6, optional.

### 4.3 Options for the corpus

| option | what | for | against |
|---|---|---|---|
| **C1** a new corpus of Arbace's own programs | `test/nethttp/*.go`-like programs (Go text read by repr/go, as goi's corpora are, or `.ae` forms), each printing a deterministic transcript; the reference is gc's run of the original; levels: eval (goi) and AOT (`arbace.build.go`) | exactly the features asked (§4.4); offline; fits goi's check and REAP2's levels with no new machinery; cheap | ours, not Go's: it proves what we thought to test |
| **C2** std's own tests | a driver for `_test.go` (`testing.MainStart`) over the packages of §4.2 | Go's coverage, an oracle for free | synctest `:out`, a driver to write (goi runs none), `export_test.go`, scale, flakiness on a loaded host |
| **C3** both, phased | C1 first, C2 for the small packages in AOT | | the cost of both |

All offline: loopback only (`127.0.0.1:0`, `httptest`), no DNS (`localhost` resolves from `/etc/hosts`),
no external network, no `GODEBUG` that reaches out. A program that cannot listen (no network namespace) is
`unsupported` with its reason, as the box's checks do.

### 4.4 What the new corpus should hold (C1)

Programs (a transcript each), grouped, 40 to 60 in all:

- **client**: `Get`/`Post`/`PostForm`, redirects and `CheckRedirect`, timeouts and `context`, a body streamed, a
  `Transport` with `MaxIdleConns`, `httptrace`, cookies with a `cookiejar`, basic auth, `Range`, gzip
  transparency, `http.NewRequestWithContext`, errors (`*url.Error`, `Timeout()`).
- **server from Go forms (interpreted handlers)**: `ServeMux` with 1.22 patterns (`GET /items/{id}`), middleware
  chains, `Handler` types, `HandlerFunc`, `http.FileServer` over `fstest.MapFS`, `Flusher` (SSE), `Hijacker`,
  `ResponseController`, `Shutdown`, panics in handlers, `MaxBytesReader`, `ServeContent`, multipart upload,
  `Server.ConnState`.
- **HTTP/2**: `httptest.NewUnstartedServer` with `EnableHTTP2` and `StartTLS`, `ForceAttemptHTTP2`,
  `r.ProtoMajor == 2`, many concurrent streams, `Pusher`, trailers, `h2c` through `Protocols`.
- **a proxy**: `httputil.ReverseProxy` with a `Rewrite`, `ModifyResponse`, `ErrorHandler`; `DumpRequest`.
- **mime and textproto**: `mime.ParseMediaType`, `multipart.Reader` and `Writer`, `quotedprintable`,
  `textproto.Reader`.
- **crossings**: an Arbace fn as a handler, as `RoundTripper`, as `http.HandlerFunc`; a Clojure namespace
  serving; a Java forms program calling a Go forms server through a var.
- **errors**: refused imports of `net/http/internal/http2` (the table's refusal), `unsupported` reasons.

The programs should run the same text under gc (the reference) and under goi, with the transcript free of
timings, ports, addresses and header order (sorted), as goi's `run/` tests are.

### 4.5 Levels and REAP

`bin/goi check` and `--aot` take their corpora from repr/go's (`linux-test`, `linux-dirtest`); `test/programs/`
(REAP7) holds user programs built by `--build-program --check`. A REAP cell is the language's level judged from
recorded results (`test/reap/cells.edn`): Go's row has four cells (read, eval, AOT, print) over goi's corpora.
Options (NH4): the new corpus as a third corpus of goi's check, counted in Go's eval and AOT cells (no new
cell), or its own cell. The first needs no change to `bin/reap` but changes the Go row's counts; the
second adds a `:net` pseudo-row for a std area, which no area has had.

## 5. What is not in scope

- `net/http/cgi`, `fcgi`, `pprof`, `net/rpc`, `net/rpc/jsonrpc`, `expvar`, `net/mail`, `net/smtp`: they stay
  interpretable packages over the compiled `net/http`, importable as today (GR8), unchecked here.
- HTTP/3 (`x/net/http3` and QUIC are vendored but not imported by `net/http`), `net/http2`'s public x/net
  package (not in std).
- The box's networking (parked) and a TLS fallback bundle (NH8).
- Speed work: no profile-guided or pooled-thread work is proposed before NH3 measures.

## 6. Plan (NH0-NH6)

Estimates in working days, as the repository's surveys give them (they have run well under). Each milestone
ends with its check green and its records (the journal, the agenda, this survey's "As built").

| milestone | content | exit criteria | days | who |
|---|---|---|---:|---|
| **NH0** the spike on what exists | no program change: with the executable of the gate and `net/http` interpreted from the embedded forms, a loopback `Listen`/`Dial`, an `httptest` server, a client `Get`; then a server with a Go-forms handler; timings (import 8.5 s, requests per second, threads); the three callback questions of §3.2 answered on the interpreted package (where no stub is involved) | a recorded list of what works interpreted, what fails and why; the numbers the milestones compare with | 0.5-1 | Sonnet |
| **NH1** the corpus | `test/nethttp/` (C1, §4.4) with its manifest, the runner on `bin/goi check` (a corpus option) and the reference of gc's runs recorded in the check's format; classes file for the non-passes | 40-60 programs, each passing under gc in the reference run and run interpreted (NH0's package) : none failing, every non-pass in a class | 2-3 | Sonnet |
| **NH2** the listing | `go/arbace/gotable.edn`: `net/textproto`, `mime`, `mime/multipart`, `mime/quotedprintable`, `net/http`, `net/http/httptrace`, `httputil`, `cookiejar`, `httptest` (NH1/NH2 of §7); `:resolve` where a digest needs one; `:stubs` slots; `bin/goi table` for the four configurations; the size recorded (`bin/goi table --size`; the executable before and after); `test/goi/std.ae` imports; `bin/goi annotations`; the `:drop` entries for `net/http` removed if unused; **the shipped change: needs the re-seed** | the four configurations' tables generate; the executable's growth recorded and within NH5's budget; `bin/gate` (essential) passes on the new table; the gate-build key changes once | 1.5-2.5 | Sonnet (Opus if the linker or digest surprises) |
| **NH3** callbacks | NH1's corpus run on the listed (compiled) package: interpreted handlers, `RoundTripper`, `CookieJar`, `FileSystem`, `ResponseWriter` assertions, Arbace fns as handlers, thread adoption under concurrency and HTTP/2 streams; the slot counts (NH7); what the measurements of §3.2 show, fixes in goi's stubs/threads where needed | the corpus passes at the eval level with the compiled package; the table of §3.2 filled with numbers; the leaks (threads, memory) shown absent after 10,000 requests | 3-5 | Opus (goi internals) |
| **NH4** AOT and the levels | `bin/goi check --aot` over the corpus (programs built by `arbace.build.go`, run under the lock), the diffcheck between eval and AOT, the essential subset (`bin/gate`: seconds), REAP's counts (NH4 of §7) | eval and AOT agree or differ in recorded classes; the essential subset in the gate | 1.5-2 | Sonnet |
| **NH5** records and the milestone step | AGENDA, journal, GOI-SPEC "As built: NH2-NH4", the table (`doc/go/GOI-SPEC.md` §6.6 growth recorded), the Makefile and `make check-makefile` if a script is added, `bin/gate --full` plan; the re-seed in the batch (NH6 of §7) | `bin/gate` passes on main; the seed verified; the executable's size in the record | 1-1.5 | Sonnet |
| **NH6** (optional) std's own tests | a `testing` driver over `_test.go` (`testing.MainStart`; read with `--tests`), the small packages first in AOT, then a selection of `net/http`'s; gc's `go test` as reference; classes (synctest, exec, testenv) | the small packages' 163 tests classified, none failing; `net/http`'s selection with its classes | 5-8 | Opus (the driver), Sonnet (the running and classing) |

**Total: NH0-NH5 about 10 to 15 working days; with NH6, 15 to 23.** NH0 and NH1 change nothing shipped
and can start before the tag (evaluated from the checkout, light, with the gate's executable); NH2 changes
`go/arbace/gotable.edn` and so the shipped table, and needs the re-seed. NH3 is the uncertain one: if the
spike shows the thread-per-goroutine model holds, it is days; if not (thousands of adopted threads), a pool
or a lighter adoption is goi's work of its own (a proposal then).

**The re-seed batching.** Today's pending changes to the shipped program, per the agenda: C3 (cc and the
dumper listed in goi's table, compiled; merged after the tag, one re-seed batched), JN10 (JFS130-JFS132, the
reader's javac half split, about 4 to 6 days, a re-seed to carry the shipped reader), and the C5/C6 re-seeds
much later. NH2 is a one-file listing of the same kind as GP7's (a table and a compiled package set).
Recommended (NH6): after the tag, JN10, then NH0-NH2, **one re-seed carrying JN10 and NH2**; C3's listing goes
with the next, which C23 needs anyway; NH3-NH5 follow on the re-seeded executable. If C3 is ready first, its
listing joins that same re-seed (the table is regenerated once either way); the cost of waiting for C3 is
that C's long pole delays HTTP, so NH does not wait for it.

## 7. Proposals (NH1-NH10)

Each with its options, the recommended one marked.

- **NH1 The scope.**
  - Options: (a) client and server, HTTP/1.1 and HTTP/2 (bundled; the same listing), `net/http/httptrace`,
    and `httputil`, `cookiejar`, `httptest` with `mime`, `mime/multipart`, `mime/quotedprintable`,
    `net/textproto` (the dependencies the API names); (b) `net/http` and its required packages only, the
    sub-packages left interpreted over it (as `cgi` and `pprof`); (c) (a) plus `cgi`, `fcgi`, `pprof`, `rpc`.
  - *Recommended: (a)*: HTTP/2 is not separable (it is inside `net/http`); the three sub-packages cost 0.27 MB
    together and are what a program using HTTP imports (a proxy, a jar, a test server); (c)'s packages are rare
    and stay importable interpreted.
- **NH2 Listed compiled or interpreted.**
  - Options: (a) listed compiled: `net/http` and the packages of NH1 (a), in the table, all four configurations;
    (b) the status quo, interpreted from the embedded forms (8.5 s to import, 36-430 times slower, an
    interpreted TLS-free HTTP/1.1 server only plausible), no re-seed, no size; (c) hybrid: `net/http`, `net/textproto`, `mime*` compiled,
    `httputil`, `cookiejar`, `httptest` interpreted over them (0.27 MB less, a second or two of import each,
    `ReverseProxy` interpreted).
  - *Recommended: (a)*: (b) is the cheapest and may stay the answer if NH0 shows it is enough, but the request is
    HTTP/2 and a server at a real rate, which interpreted cannot give (hpack and the x/text tables alone
    are a large part of the 8.5 s); (c) saves 0.27 MB at the price of the three packages' import time for
    every program that uses them. Revisit with NH0's numbers before the listing commit.
- **NH3 The check corpus.**
  - Options: (a) a new corpus of Arbace's own programs (§4.3 C1) only; (b) std's own tests (C2) only; (c) both,
    phased: (a) at NH1-NH4, (b) the small packages in AOT at NH6, `net/http` proper only if wanted after.
  - *Recommended: (c)*, with (a) the exit and (b) optional: `$GOROOT/test` has no HTTP program that runs; (b)
    alone needs a test driver, loses synctest's share, and is a second project.
- **NH4 Levels and REAP.**
  - Options: (a) the corpus a third corpus of goi's check, eval and `--aot`, folded into Go's eval and AOT cells
    of `bin/reap` by the counts, its classes in `test/goi/divergences*.edn`; (b) its own cell (a std-area
    pseudo-row); (c) no REAP integration, a script and a gate step only.
  - *Recommended: (a)*: the corpus is Go programs judged against gc, the thing the cells count; (b) invents a
    row type for one std area. The Go row's numbers change at the merge; recorded.
- **NH5 The size budget.**
  - Options: (a) 6 MB (3.4% of 177.8 MB; the estimate is 4.6 to 5.4), recorded by `bin/goi table --size`
    in the listing commit and in GOI-SPEC §6.6; over it, the decision returns to the user; (b) no budget, the
    number recorded; (c) 3 MB, which forces NH2 (c) and still misses, since `net/http` alone is +3.7 MB.
  - *Recommended: (a)*: GS5 asks for the size to be recorded, and a bound makes a surprise (GR8's 1.87 MB
    against the probe's 3.7 MB) a decision, not an accident.
- **NH6 The order relative to C23 and JN10.**
  - Options: (a) NH2 before JN10, its own re-seed; (b) NH2 with JN10's re-seed, after the tag and after NH0-NH1
    (one re-seed, the second of the milestone's work); C3's listing and C5-C6 later; (c) after C23 whole,
    in the re-seed of C9.
  - *Recommended: (b)*: two shipped changes of days each share one re-seed (a re-seed takes `--make` and
    `--verify`, hours of the host); (a) costs a re-seed; (c) delays HTTP by C23's 32 to 54 days (doc/C23-SURVEY.md §11)
    for no gain, since nothing in HTTP depends on C.
- **NH7 Stub slots and handler types.**
  - Options: (a) raise `:slots-of` for `ServeHTTP`'s family to 64 and keep 16 for the others (+11 KB), measuring
    at NH3; (b) the default 16 (an interpreted program with more than 16 handler types panics at the call, with
    goi's one warning at the REPL); (c) 256, as `String` and `Error`.
  - *Recommended: (a)*: a program with a type per route is realistic; (c) costs 50 KB for a family few
    programs fill.
- **NH8 TLS roots for a Go-forms client.**
  - Options: (a) as is: the host's system roots, no fallback; offline checks use `httptest`'s certificate;
    (b) install `x509roots/`'s bundle through `x509.SetFallbackRoots` in the Go-forms mode (128 KB already in
    the program; jrt's connections use `jrt.TLSRoots`); (c) bundle and system roots both.
  - *Recommended: (a)*: no external network in the checks, and gc's behaviour is the reference; revisit
    for the standalone box, where there are no system roots.
- **NH9 The box and the tamago configurations.**
  - Options: (a) one list for the four configurations, the box parked: NH2 only checks that the tamago tables
    generate; (b) a per-package `:exclude-in` for the box (a new key), so the guest does not grow 4.6 MB
    until its networking exists; (c) list in linux only.
  - *Recommended: (a)*: the key of (b) is a table feature for a parked target; the box's guest is built from
    `tamago/goroot` and the growth is the same order as on linux; if the box's size matters when it is
    revisited, (b) is a small change then.
- **NH10 An Arbace-facing API.**
  - Options: (a) the idiom only: `(http/HandlerFunc f)` and `reify`, documented in GOI-SPEC's "As built: NH3" with
    the corpus's crossing programs as the examples; (b) a helper in the package `arbace` (`arbace.Handler`,
    `arbace.RoundTripper`), listed, wrapping an `IFn`; (c) Clojure-side functions (Ring-like) in a namespace.
  - *Recommended: (a)*: the crossing machinery is goi's and exists; (b) and (c) are API design to do on the
    evidence of the corpus, after NH3, not before.

## 8. Risks

- **Per-goroutine thread cost.** Each compiled goroutine that calls an interpreted handler adopts a jrt
  `Thread` with its own frames and stacks. A server with thousands of connections or HTTP/2 streams could
  hold hundreds of megabytes. NH0 measures the interpreted package (all goroutines are adopted there too);
  NH3 the compiled one. A pooled adoption would be new work in goi (days).
- **The table fails on a package of the closure.** `net/http` is already read natively and checked by goi for the
  interpretable path; the declaration layer on the listed path is new for it (unexported types reached from
  the exported API: `http2`'s). A `:resolve` entry or a `:replace` is the known remedy; the cost is days, not
  weeks (GR8a listed 34 packages with a handful of such fixes).
- **The size lands above the estimate.** The probe is stand-alone; the shipped build adds the invokers and
  digests, unmeasured. NH5's budget makes it a decision. If the real figure is far above 6 MB, NH2 (c) and a
  smaller list.
- **Loopback in the sandboxes.** The checks need a network namespace with `127.0.0.1`. The gate runs on the
  host; the box and any sandboxed runner may not offer it: `unsupported` with a reason, never a failure.
- **Flakiness.** Port reuse, timeouts under the gate's load (the repository's CPU-time limits with a wall
  cap and a quiet retry apply, `bin/lib/limit.bash`), goroutine scheduling order. The corpus transcripts must
  not depend on timing.
- **The reference.** gc's run of the originals needs the same loopback; a program using a fixed port is
  banned; `:0` and `httptest` only.
- **Interpretable packages that import `net/http` today** (`expvar`, `net/http/pprof`, `net/rpc`, `cgi`,
  `fcgi`, `httputil`, `cookiejar`, `httptest` if left interpreted, `go/doc` readers ...) change from importing
  an interpreted package to a compiled one: types identities move; `std.ae` (the 169 imports in one process)
  and the check's recorded results detect it.
- **The seed.** Listing changes the shipped table and so the program; `bin/seed --verify` requires the
  fixpoint; the re-seed is the user's step, batched (NH6).
- **Estimates are the repository's, not the host's.** NH3 and NH6 have wide ranges on purpose.

## 9. Sources

- Go 1.27.1, TamaGo's tree `/root/tamago-go` (VERSION `go1.27.1`, time 2026-08-28T16:20:06Z; pinned for the
  seed by `seed/goroot.sha256`): `src/net/http/` (29 files, with `internal/http2`, `internal/httpcommon`,
  `internal/httpsfv`, `internal/ascii`, `internal/testcert`), `src/net/http/{httptest,httputil,cookiejar,
  httptrace}/`, `src/mime/`, `src/net/textproto/`, `src/vendor/golang.org/x/{net,text,crypto,sys}/`;
  `test/fixedbugs/{bug358,issue20298,issue25727}.go`, `test/stress/`.
- This repository at `fdf939b5` (main, 2026-10-29): `go/arbace/gotable.edn` (`:packages` 96 entries, `:drop`
  of `net/http`, `:stubs`, `:min-sharing`), `arbace/go/table.ae` (`importable?`, `vendor/` resolution),
  `go/arbace/goi/stubs.ae` (`EnterSlot`), doc/go/GOI-SPEC.md (§6.1-§6.6, §7.4-§7.5, §8, §11, "As built: GR3",
  "GR5", "GR9a-c", "GS14" and the v1 divergences table), doc/go/GR8-SURVEY.md (§1, §2.3-§2.4, the size table
  of §3.3), doc/FULL-SUPPORT-SURVEY.md (REAP2, RPS10-RPS12), doc/go/BUILD.md, doc/go/BOX-NOTES.md
  (TamaGo's netpoll), doc/C23-SURVEY.md (§11 plan style, the re-seed batching, §12), doc/classes/
  JAVA-FRONTEND-SURVEY.md (JN9f.11, JFS130-JFS132), doc/AGENDA.md (the order of work), `x509roots/README.md`,
  `tamago/goroot/go/net/http` and `.../vendor/golang_org/x/`, `test/goi/check-linux-amd64.edn`,
  `test/goi/check-aot-linux-amd64.edn`, `test/goi/timeouts.txt`.
- Measured against: `target/arbace-go/amd64/arbace` of the main checkout, 177,778,848 bytes (2026-10-29),
  not run.

## Appendix A: the scratch

In this worktree's `.tmp/nethttp/` (gitignored): `listed.txt` (the 96 listed paths read from
`gotable.edn`), `deps-net_http.txt`, `deps-net_http_httptest.txt`, `deps-net_http_httputil.txt`,
`deps-net_http_cookiejar.txt` (`go list -deps` with a leading `X` on non-std), `deps-listed.txt` (the closure
of the listed packages), `gen/main.go` (the generator of the API-reference programs, `go/build` and `go/parser`
over each package's GoFiles), `probe-base/`, `probe-use/`, `probe-api/`, `probe-core/`, `probe-h2none/`,
`probe-sub/` (their programs) and `s-*.bin` (the stripped binaries of §2.1: base 3,510,432; h2none 4,206,752;
core 7,868,576; sub 8,024,224; api 8,143,008; use 7,590,048 bytes), `probe-*.log` (build times and memory).
Every build: `CGO_ENABLED=0 GOCACHE=$PWD/gocache /root/tamago-go/bin/go build -ldflags='-s -w'`.

## Decided (2026-10-29): NH1-NH10

The user decided all ten as recommended: NH1 (a), NH2 (a), NH3 (c), NH4 (a), NH5 (a), NH6 (b), NH7 (a), NH8 (a),
NH9 (a), NH10 (a). In short: client and server, the bundled HTTP/2, httptrace, httputil, cookiejar and httptest, listed
compiled in all four configurations, with a 6 MB budget recorded by `bin/goi table --size`. The new offline corpus
comes first and std's small packages' tests later. Both fold into Go's eval and AOT cells. One re-seed carries JN10
and the listing, ahead of C23.
