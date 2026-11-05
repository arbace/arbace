# OCaml's deferred cases, triaged by root cause (2026-11-05)

The REAP matrix's OCaml cells at `ocaml-o8` (`test/reap/matrix.edn`): **eval 360 pass / 114 by design / 190 deferred**,
**AOT 337 / 108 / 219** (the light level, OC87). This triage asks what the 190 and 219 deferred programs really wait for,
groups them by root cause rather than by the class of `test/ocaml/divergences*.edn` that files them, counts them, estimates
days per fix, ranks the fixes and designs the first five far enough to start. It follows doc/classes/JAVA-DEFERRED-TRIAGE.md
(branch `java-deferred-triage`). Nothing was built and no corpus was run: the records (`test/ocaml/check-{oi,aot,suite-oi,suite-aot}.edn`
and the five classes files), the testsuite's sources (the `ocaml-5.5.0` tarball's `testsuite/tests` and `otherlibs/`, read in place), oi's
code (`go/arbace/oi`, 11.6k lines of Go forms) and the AOT runtime (`jdk/ocaml/ocaml/rt`, 4.9k lines of Java forms) were read; two
one-line probes of `bin/oi run` (light) confirm the first finding below.

## Summary

- The deferred classes are **header classes, not causes**. `:multi-file` (93) and `:libraries` (90) are 183 of the 190 eval
  deferred; the rest is `:bigarray` 3, `:header-flags` 2 and the suite's `Marshal` with closures 2. At AOT the 29 programs of the light
  level (`:light-speed` 26, `:light-depth` 3) come on top (219).
- **Nothing outside the stdlib is in oi at all.** `Thread`, `Unix`, `Str` are "Unbound module" (probe: `Thread.create` and `Unix.sleepf`
  programs); oi's registry holds 347 externals, none `unix_*`, `caml_thread_*` or `caml_re_*` (mutex, condition, domain and effects are there).
  So every library is a fix of the same shape (its units compiled into the library set, its externals registered, twice: oi's Go and the AOT
  runtime's Java forms, +50% to +100% for the mirror at AOT).
- **79 of the 190 can never pass in oi's design** and should move to by design (Proposed OT1-OT7): the compiler's output tests (19), ocamltest's
  tools (15), tests that run a program the test built (4), `OCAMLRUNPARAM` backtrace references (2), `Unix.fork` (4), runtime events (6),
  Dynlink (6), C stubs that use the runtime's internals (21), `Marshal`'s Closures flag (2). That leaves **111 deferred at eval, 140 at AOT**.
- **The ranking by tests per day** (days = eval; the AOT mirror in the table): the compiled level (29 AOT-only tests, 4.8 a day), the Unix core
  (9, 4.5), the harness for multi-module tests, data files and flags (13, 4.3), Unix descriptors and files (17, 4.3), threads (15, 3.75). These five,
  **19 days eval + 8 AOT**, unblock at most **54 programs at eval and 83 at AOT**; with the calibration of the Java triage (0.7 of
  "alone" regained) about **38 at eval (360 to about 400 pass)** and **58 at AOT (337 to about 395)**.
- **The typer** (branches `ocaml-typer`, `ocaml-typer2`: doc/ocaml/TYPER.md, T1-T12) changes none of the runtime fixes (externals are the same
  whoever produces Lambda) and four others (section 5); its biggest consequence is that it could reopen the by-design backtraces (28 tests).

## 1. Method

**The classification, reproduced.** A script over the records and classes (first class of the file whose `:match` finds "RESULT REASON";
`:kind-map {:corpus :design :light :deferred}`) gives the matrix's numbers: eval = `check-oi.edn` + `check-suite-oi.edn`: 273 + 87 pass; the
deferred 93 + 90 + 3 + 2 (check) + 2 (suite) = 190; AOT 251 + 86 pass, the same 188 + 2 plus 29 light = 219.

**The causes.** For each of the 183 header-class programs the test's source was read (and for a `modules` list the directory's `.ml` and `.c`
files): which library it includes, which `Unix.*`, `Thread.*`, `Str.*` functions it calls, which C stubs it builds. Every program gets a set of
causes; the columns of section 2 are as the Java triage's: **touches**, **alone** (the only cause: unblocked if it alone were fixed), **+deps**
(unblocked by it with its prerequisites), **design** (it also needs something by design: `Unix.fork`, `Printexc` backtraces, the GC's counters,
weak and ephemerons, plugins, the compiler's output).

**Calibration.** "Unblocked" means the library exists; a test can still fail on a behaviour (timing, `torture`, `Gc` counters). The Java triage
measured 0.7 of "alone" regained; this plan uses the same factor. oi's passes show the base rate: of the 556 programs of the run corpus 247 are classed by header and not run; of the other 309, 273 pass (88%).

## 2. The causes: touches, alone, +deps, design (eval; AOT the same plus the light level)

| cause | what it is | touches | alone | +deps | design |
|---|---|---:|---:|---:|---:|
| `c-stubs` | the test builds C stubs (`modules = x.c`) | 41 | 41 | 41 | 21 (runtime internals, OT6) |
| `compiler-output` | `compile_only`, `-dshape`, `setup-ocamlopt.byte-build-env`, dumpobj | 19 | 19 | 19 | 19 (OT2) |
| `tool` | ocamldoc 5, caml-tex 2, lexyacc, debugger, ocamldep, objinfo, scripts, toplevel | 15 | 15 | 15 | 15 (OT3) |
| `multi-ml` | `modules = a.ml b.ml ...`, `readonly_files` of modules (no C) | 11 | 11 | 11 | 0 |
| `compiler-libs` | `include ocamlcommon`/`config` (Misc, Parsetree, Location...), plus `-I $ocamlsrcdir/parsing` | 9 (+1) | 9 | 9 | 0 |
| `thread` | `Thread`, `Event` (systhreads) | 29 | 14 | 15 | 6 (4 fork, 2 Gc counters) |
| `unix-fd` | pipes, read/write, dup, openfile, stat, truncate, utimes, symlink, select, channels of descriptors | 29 | 10 | 12 | 3 |
| `unix-min` | `sleep`, `sleepf`, `gettimeofday`, `getpid`, `environment`, errors | 21 | 9 | 9 | 1 |
| `unix-proc` | `waitpid`, `create_process`, `open_process*`, `kill`, `alarm`, `sigprocmask`, signals | 13 | 2 | 10 | 0 |
| `unix-sock` | sockets (3 threaded servers, `recvfrom`, `gethostbyaddr`) | 7 | 3 | 4 | 0 |
| `dynlink` | `Dynlink.loadfile` of plugins the test builds | 6 | 6 | 6 | 6 (OT5) |
| `runtime-events` | `Runtime_events` (the GC's ring, a consumer reading the file) | 6 | 6 | 6 | 6 (OT4) |
| `fork` | `Unix.fork` | 4 | 0 | 0 | 4 (OT1) |
| `subprocess` | the test runs a program it built (`reflector.ml`, `myecho.ml`, `cmdline_prog.ml`) | 4 | 4 | 4 | 0 (OT3) |
| `bigarray` | `bigarray.c` beyond the stdlib's minimum (3 of the class; `map_file`, `read_bigarray`, 2 of unix's; 4 C stubs) | 3 (+2, +4) | 3 | 3 | 0 |
| `str` | `Str` (strstubs.c, str.ml) | 2 | 2 | 2 | 0 |
| `backtrace-ref` | `reference = $` of backtrace tests | 2 | 2 | 2 | 2 |
| `readonly-data` | `readonly_files` of data (`basic-io-2/io`) | 1 | 1 | 1 | 0 |
| `flags` | `-safer-matching` (the other, `-I`, is compiler-libs) | 1 | 1 | 1 | 0 |
| `marshal-closures` | `Marshal.Closures` (suite) | 2 | 2 | 2 | 2 (OT7) |
| `light` (AOT only) | the light level: 26 over the CPU limit, 3 over the 10,000-frame depth | 0 / 29 | 29 | 29 | 0 |

(+deps: `thread` after `unix-min`; `unix-fd` after `unix-min`; `unix-sock` after `unix-fd`; `unix-proc` after `unix-min` and `unix-fd`.)
The 67 programs of `include unix|systhreads` split by what they call: threads alone 14, descriptors alone 10, the core alone 9, and a long tail
of combinations (the columns above count the combinations under each cause). **The `:libraries` class hides four different jobs.**

What the largest causes contain:

- **Unix** (67 programs with systhreads). Of ~130 externals only a few are common: `close` 15, `pipe` 10, `read` 8, `sleep` 8, `getpid` 5, `kill` 5,
  `fork` 5, `write` 5, `out_channel_of_descr` 5, `Unix_error` 10 (its registered exception). The 9 "core" programs need only `sleep`/`sleepf`/`getpid`/
  `gettimeofday`/`environment`/`Unix_error`: `parallel/backup_thread`, `domain_*_spawn_burn` (3), `poll`, `recommended_domain_count_unix`, `lib-random/parallel`,
  `lib-systhreads/multicore_lifecycle`, `lib-unix/win-env/test_env`.
- **Threads** (29): `Thread.create` 28, `join` 22, `delay` 12, `yield` 6, `self`, `id`, `Thread.sigmask` 2; `Event` (`bank`, `sockets`).
  `thread.ml` has 97 lines and 8 externals; `st_stubs.c` 1,099 lines. oi's domains are goroutines already (`thread.ae`) under an interpreter lock; a systhread
  is a goroutine of the same domain under that lock (the master lock), released in blocking calls.
- **C stubs** (41): 21 touch the runtime's internals (global roots, the C thread registration, custom blocks' finalizers, `caml_startup` embedding, the
  runtime-events consumer, locale, Windows); 20 use the stable C API (callbacks 10, `external` with 15 arguments, `[@unboxed]` externals 2, exceptions from C,
  BLAKE2's self test, `Marshal`'s helper 2, bigarray stubs 3).

## 3. Fixes, days and ranking

Days: eval (oi) + AOT mirror (the Java runtime `jdk/ocaml/ocaml/rt`, checked at the light level; the compiled level shares it). Sources of the
estimates: the C line counts of the library (unix 142 non-Windows files, 12.9k lines but most are 20 to 100 line wrappers; `st_stubs.c` 1,099; `strstubs.c`
550; `bigarray.c` 1,395; `runtime_events.c` 872 + the consumer 1,344) at the pace of the existing externals (oi's 11.6k lines were O3-O5), and the
checks' time. **Rate** = tests unblocked (the `+deps`-closure's new tests, design ones excluded) per eval day.

| # | fix | tests (eval = AOT) | days eval + AOT | tests/day | needs |
|--:|---|---:|---:|---:|---|
| 1 | **compiled level** (AOT's reference check): o2j per-program package, depth counter, shards, record | 29 (AOT only) | 6 + 0 | 4.8 | j2g's world items (heavy list 2), a night window |
| 2 | **Unix core**: the `Unix` unit, `Unix_error`, time, pid, environment, unlisted externals failing at call | 9 | 2 + 1 | 4.5 | none |
| 3 | **harness**: multi-module programs, readonly data, flags | 13 (11 + 1 + 1) | 3 + 0.5 | 4.3 | none |
| 4 | **Unix descriptors and files** (the fd syscalls, `*_channel_of_descr`, stat family, select, `isatty`) | 17 (12 + 5 with threads) | 4 + 2 | 4.3 | 2 (and 5 for the 5) |
| 5 | **threads**: `Thread`, `Event`, the master lock, `delay`/`yield`/`join`, `Thread.sigmask` | 15 | 4 + 2 | 3.75 | 2 |
| 6 | Unix sockets (`socket` .. `recv`, `getsockname`, `gethostbyaddr`) | 7 | 2 + 1 | 3.5 | 4 |
| 7 | Unix processes and signals, no fork (`waitpid`, `create_process`, `open_process*`, `kill`, `alarm`, `sigprocmask`, signal handlers) | 13 | 4 + 2 | 3.25 | 4 |
| 8 | compiler-libs (`Misc`, `Config`, `Parsetree`, `Location`, `Lexing`) as library units | 10 | 4 + 1.5 | 2.5 | the compiled sources of the tarball's `utils/` and `parsing/` |
| 9 | C API shim on ci (callbacks, unboxed externals, exceptions, strings) | 20 | 10 + 4 | 2.0 | ci, a clean-room `caml/*.h` |
| 10 | `Str` | 2 | 2 + 1 | 1.0 | none |
| 11 | bigarray.c (kinds, layouts, overflow, marshalling) | 3 (+2 unix, +4 C) | 3 + 1.5 | 1.0 | 4 for `map_file` |

Total 1-11: eval **44 + 16.5 AOT mirror = about 61 days**, **109 tests at eval, 138 at AOT** (item 1 is AOT only); every fix after 7 is under 2.6 tests a day. The first five (1-5):
**19 days eval (+ 8 AOT mirror)**, **54 tests at eval (13 + 9 + 17 + 15), 83 at AOT** (adding the 29 light). 0.7 × alone: about 38 / 58.
After every fix and every Proposed move, the deferred programs left are the tails (the timing tests such as `torture`, programs that also touch the GC's counters).

## 4. The first five, designed

**1. The compiled level.** The AOT cell's 29 deferred (26 over the OC87 CPU limit, 3 over the 10,000-frame depth) are the light level's: Arbace's Java
forms interpreted on stage 0, about ten times slower than oi. The heavy list of the survey ("As built: O6", items 1-4) is the work: (a) the depth counter of
OC92 (a) in the translated code (a counter in each call, raising `Stack_overflow` at the interpreted limit, 200,000 as oi's OC69); (b) o2j's per-program
package `ocaml.p<N>.M_<Unit>` and the stdlib's units once in `ocaml.u` (OC93 (a)), the run corpus in shards of a few dozen programs, `bin/j2g --program`,
`bin/build-go`; (c) `bin/oi check --aot --level compiled` (to write) judged by `test/ocaml/o2j_check.ae` against ocamlopt's runs, recorded as
`test/ocaml/check-aot-compiled.edn`, its classes the light level's less `:light-*`; (d) j2g's edge for the classes the runtime asks of the JDK
(`SynchronousQueue`, `ThreadLocal`, `RandomAccessFile`, `Files`, `BigDecimal`, `ProcessBuilder`): `bin/j2g-edge update`, `bin/jrt-convert update`; (e) the
gate step `oi-aot` (heavy, OC95). First day: the depth counter and one shard; the rest follows the measure of the shard. Risk: the 26 are programs
of 10^7 steps and more; expect 20 to 24 to pass, the rest `tailcalls`/`chi2`-like over the limit even compiled. **Decision needed**: OT8 (the AOT cell judged at the compiled level).

**2. The Unix core.** (a) Load `otherlibs/unix`'s OCaml units (`unix.ml`, `unixLabels.ml`; `-I +unix`) into the library set as the stdlib's are (`bin/oi stdlib`:
oi's library is the units' Lambda dumped by ocdump; add the two units and the `Unix` alias module); today ocamlc prints the alert "unix subdirectory ... added to the search path" for a
program, the `-I +unix` flag silences it (the harness passes it for `include unix`). (b) Externals in a new `ext_unix.ae`: registered under their C names (`unix_sleep`,
the names as the units declare them (`caml_unix_*` in 5.x); get the list from the units: `bin/oi aot-lib` reports missing ones, as for the stdlib), the core ones real (time, `gettimeofday`, `getpid`,
`getppid`, `environment`, `getenv`/`putenv`/`unsetenv`, `sleep`/`sleepf`, `times`, `error_message`), every other `unix_*` registered as a stub raising
`Unix_error(ENOSYS, name, "")` so the unit links. (c) `Unix_error`: `Callback.register_exception "Unix.Unix_error"` and the errno table of
`unixsupport.c` (the 68 error constructors from Go's `syscall.Errno`; `unix_error_message` the C library's strerror text: Go's `syscall.Errno.Error()` differs in case for a few, a table). (d) AOT: the same in a new `ExtUnix.ae` and `Fd.ae`'s table. Probe `lib-unix/win-env/test_env`, `parallel/backup_thread`.
Tests: the 9 above. Checks: `bin/oi check --only` over them, then `--aot`.

**3. The harness.** Three header features of `test/ocaml/oi_check.ae` and the oracle's invocation (ocdump): (a) `modules = "a.ml b.ml"` (and the `readonly_files` that name
modules): each listed `.ml` compiled in order, the earlier ones' interfaces on the path, each unit's Lambda added to the library set before the main file's; the
reference run is ocamlc's on the same link (recorded by `bin/oi check --record-ref` on a host with ocamlc). The 11 programs: `badly-ordered-deps/coccinelle`, `basic-modules/main`,
`basic-multdef/usemultdef`, `basic-private/tlength`, `link-test/test`, `memory-model/{forbidden,publish}`, `misc-kb/kbmain`, `raise-counts/main`, `required-external/main`,
`typing-shadowing-of-pervasives-submodules/redefine_largefile` (`link-test` uses `external_for_pack`: packs are not units: leave it in the class if it does not link). (b) `readonly_files` of data: copy to the run's cwd (`basic-io-2/io`).
(c) `flags = -safer-matching`: ocdump's compiler flags passthrough (`basic-more/safer_matching`). AOT: o2j already makes a package a unit per `M_<Unit>`: the light level's check translates each unit, 0.5 day.
The `modules` programs of `basic-modules/main` and `basic-multdef/usemultdef` already pass in the suite check as other cases (`check-suite-oi.edn`): the harness unifies them.

**4. Unix descriptors and files.** OCaml's `Unix.file_descr` is an `int` on Unix, and oi's channels already wrap real descriptors with `syscall` (`ext_io.ae`: "a buffered file
descriptor with the C runtime's exact buffering"), so the table is the OS's: `unix_read`/`write` (with the Unix library's 64 KiB limit and `EINTR` loop), `pipe`, `dup`, `dup2`, `close`,
`openfile` (flags list to `O_*`), `lseek`, `fstat`/`stat`/`lstat` (the record of 12 fields, `st_kind` from the mode), `truncate`/`ftruncate`, `rename`, `unlink`, `mkdir`, `symlink`, `readlink`, `realpath`,
`utimes`, `chmod`, `isatty`, `select`, `set_nonblock`/`clear_nonblock`, `lockf`, `in_channel_of_descr`/`out_channel_of_descr` (a Channel on the descriptor: `Channel.fd` exists) and
`descr_of_*_channel`. Each is a few lines over `syscall` (the TamaGo build has `syscall` for linux; oi builds only on it). Blocking calls run with the interpreter lock released (the same hook as threads' item 5).
The 17 programs: 10 `lib-unix/common/*` and `isatty`, `realpath`, `lib-channels/close_during_flush`, `lib-threads/{close,fileio,torture}`, `parallel/{backup_thread_pipe,multicore_systhreads}`; 4 of them wait for threads.
Design question: `Unix.fork` (OT1) is kept out. AOT: `Fd.ae`'s table over `RandomAccessFile`/`FileChannel` (no raw descriptors on the JVM's API): the compiled level through Go's `syscall` would be direct; at the light level,
the Java forms' `jrt` has only `FileDescriptor`: expect 3 of the 5 mirrored days to be in this.

**5. Threads.** Compile `otherlibs/systhreads`'s `thread.ml`, `event.ml` (no `Mutex`/`Condition`/`Semaphore`: the stdlib's) into the library set; externals `caml_thread_initialize/cleanup/new/self/id/
join/yield/exit`, `thread_uncaught_exception`, `set_current_thread_name`; `Thread.delay` is `Unix.sleepf` (item 2). Semantics: a thread is a goroutine of its domain under the master lock; the lock is released at
blocking sections (the sleeps, channel reads, `select`, `Mutex.lock`'s wait, `Condition.wait`, `join`) and at an `yield` and, in OCaml, by the tick thread every 50 ms: oi's interpreter has a lock that serialises channels
today, so threads reuse it, with a preemption count at calls' polls (as signals' polling in `ext_sys.ae`). `Thread.id` is a counter; a thread's uncaught exception printed by `Thread.default_uncaught_exception_handler`. The 4 fork
programs and `callstack`/`boundscheck`/`moved_while_blocking` (backtraces and counters) stay out. AOT: `Gil.ae` and `TState.ae` exist in the Java runtime; Java forms threads map to `java.lang.Thread` (j2g's `ThreadLocal`).

## 5. What the typer work changes (ocaml-typer: T1-T6 done, 36 of 36 samples; ocaml-typer2: T7 Simplif, T8 the module system, 44 of 44; T9-T12 ahead)

| cause | effect of a native front end |
|---|---|
| Unix, threads, sockets, descriptors, Str, bigarray, C shim | **none**: the externals and the runtime are the same whoever makes Lambda. They are needed in oi whether `ocdump` or the typer compiles the library's units; once T8 compiles stdlib units natively, `otherlibs` (`unix.ml`, `thread.ml`, `str.ml`) follow the same route (T8 "the stdlib's own .ml typed and translated natively"; each unit's Lambda equal to the oracle's). Until then they keep ocdump, which needs a host `ocamlc` with `-I +unix`. |
| harness: multi-module | **helps**: the typer's `Env` reads interfaces natively (no `.cmi` files), so a `modules` list needs the typer to keep each unit's signature in the environment, not a file; T8 gives `Typemod` and `Includemod`. After T8 the harness has no ocamlc at all. |
| compiler-libs | **helps and hurts**: finding 4 of the O7 review (the host's compiler-libs were compiled against its own stdlib: 7 tests run on the installed stdlib instead of the tree's, with other digests) disappears when the units are compiled from the tarball's sources by the typer. But `utils/` and `parsing/` use GADTs, first-class modules and polymorphic variants: T10. The fix 8 waits for T10; the AOT mirror is the translation of the units. |
| flags | `-safer-matching` is Matching's own flag: free once `Matching` is the typer's (T1-T6 ported it). |
| compiler-output (19) | T12 (errors, shapes, `-dshape`, `-dsource`) makes these comparable to the compiler's reference files as a **typer check**, not a program run: they belong to the typer's classes (TYPER.md section 5), not to oi's run corpus (OT2). |
| backtraces (by design: 23 `ocamlrunparam`, 3 + 2 backtraces: 28) | **reopens** OC11 (a): `Translcore` generates `Levent` events when compiled with `-g` and the native Lambda can keep them, which ocdump's dump does not; a shadow stack of events would give Printexc's backtraces, byte for byte on the common programs. About 6 to 8 days after T11. Not a Proposed item now. |
| C shim | the typer reads `external` declarations' attributes (`[@unboxed]`, `[@untagged]`, `[@@noalloc]`) as Translprim does: the same primitives descriptions as ocdump's. |
| dynlink | the typer compiles a plugin and its `.cmi` natively: no `.cmo`, so Dynlink's CRC checks stay out (OT5). |

## 6. Proposed (OT1-OT10; options with costs, a recommendation each, none decided)

- **OT1 `Unix.fork`.** (a) by design: Go cannot fork a multithreaded process (the runtime's threads and the interpreter's goroutines do not survive in the child); oi's `Unix.fork` raises `Unix_error(ENOSYS)`; 4 tests (`lib-systhreads/testfork`, `testfork2`, `lib-unix/common/fork_cleanup`, `fork_cleanup_systhreads`) and the one in `lib-runtime-events`; (b) `fork` by re-exec of oi with the program's state replayed: no, nothing is
  deterministic. *Recommended: (a)*.
- **OT2 The compiler's output tests.** (a) by design for oi's run corpus (19: `shape-index/*` 12, `uid-deps`, `uids`, `no-alias-deps`, `syntactic-arity/*` 3, `tool-dumpobj`: they compare the compiler's `-dshape`, `-bin-annot`, `.cmt` or ocamlopt's allocation, no program runs), judged with the typer (T12) as its own check
  later; (b) deferred until T12. *Recommended: (a)*, the typer's classes of TYPER.md §5 own them.
- **OT3 ocamltest's tools and programs the test builds.** (a) by design (15 tools: ocamldoc 5, caml-tex 2, lexyacc, debugger, ocamldep shadowing, ocamlobjinfo, `self-contained-toplevel`, 2 `script`, `use_runtime = false`; and 4 `subprocess`: `reflector.ml`, `myecho.ml`, `cmdline_prog.ml`, which the test compiles and runs with `Sys.command`/`Unix.create_process`: oi would need itself as an executable of the program); (b) the subprocess 4 as work (a `bin/oi exec` the harness installs: 3 days). *Recommended: (a)* for all 19 and the `reference = $` 2 (backtraces, OC11 (a)).
- **OT4 Runtime events.** (a) by design (6): the ring of the runtime's own events (GC phases, `Runtime_events.User` events through a memory-mapped file read by a consumer: 2,200 lines of C) observes the GC, which oi's values do not have (OC64: the counters stay at zero); (b) a user-events-only subset (2 of 6 tests): 3 days. *Recommended: (a)*.
- **OT5 Dynlink.** (a) by design (6): `Dynlink.loadfile` of a `.cmo`/`.cmxs` the test builds, with the CRC checks of interfaces and the unit table the error messages print; oi has Lambda in memory, no bytecode files; (b) a `loadfile` of an oi unit (a Lambda file) with the unit names: 5 days, the 6 tests would still differ in the messages. *Recommended: (a)*.
- **OT6 C stubs.** (a) by design for the 21 that use the runtime's internals (`embedded/cmcaml`, `gc-roots` 3, `ephe-c-api`, `c-api` 3, the thread registrations 3, `statmemprof` 2, `regression/pr3612` and `pr11865`, `locale`, `win-stat`, `output-complete-obj/test2`, `lib-runtime-events/test`, `lib-dynlink-native`, `backtrace_c_exn`), (b) also for the 20 of the stable C API; (c) a C API shim on ci for the 20 (10 + 4 days): `value` as an int (tagged ints as OCaml's, blocks as handles of a table), a clean-room
  `caml/mlvalues.h`, `memory.h`, `callback.h`, `alloc.h`, `fail.h` over oi's Values; the stubs' C compiled by `arbace.c`. *Recommended: (a) for the 21 and (c) kept as item 9 of the ranking, last in the order*: it is 2.0 tests a day and the biggest risk.
- **OT7 `Marshal` with Closures.** (a) by design (2, suite: `lib-marshal/fuzzy`, `runtime-objects/Tests`): closures marshal as code pointers and program digests (O7, OC103); (b) a code-pointer scheme keyed by the program's Lambda digest: 3 days for 2 tests. *Recommended: (a)*.
- **OT8 The AOT cell's level.** (a) the compiled level when recorded (fix 1), the light level kept as the quick check and its classes `:light-*` by design; the matrix's AOT cell judged from the compiled record (the 29 light rows no longer counted); (b) both levels counted: the light level's 29 stay deferred. *Recommended: (a)*; the 3 depth programs are fixed by the compiled level's stack (OC92) and the 26 speed ones by its speed.
- **OT9 The order of the libraries.** OC63 (a) says unix and str, then bigarray, then systhreads; the counts say unix core, the harness, descriptors, threads, sockets, processes, then compiler-libs; Str (1.0 a day) and bigarray (1.0) after the C shim. (a) as ranked here; (b) OC63's order. *Recommended: (a)*; it supersedes OC63's order only.
- **OT10 Where the library units live.** (a) the units of `otherlibs` (`unix.ml`, `thread.ml`, `event.ml`, `str.ml`, `compiler-libs`) built into the library set like the stdlib, every program of every check loading them (a start-up cost: the stdlib's 10 CPU seconds at the light level, OC88, are shared per process; Unix is 4,000 lines of OCaml); (b) loaded only for a program that names them (`include unix`, found by the header or by the program's free modules), the way the stdlib's units are loaded on demand. *Recommended: (b)*: no cost for the 273 passing programs, and the AOT's `ocaml.u` package stays the stdlib's.

## 7. After all of it

If OT1-OT9 are accepted and items 1-11 built (about 61 days): eval 360 + at most 109 = **469 pass** at best, 114 + 79 = **193 by design**, 2 deferred; AOT 337 + at most 138
= **475**, 108 + 79 = **187 by design**, 5 deferred. With the 0.7 factor on the new passes: eval about **436**, AOT about **434**, the 30% that fail on a behaviour staying deferred (about 33 and 41).
The first five alone, 19 days: eval 360 to about 400, AOT 337 to about 395.

## Decided (2026-11-05, the user)

OT1-OT10 as recommended: Unix.fork, the compiler-output tests, ocamltest's tools and subprocess tests, runtime events, Dynlink, the 21 C-stub tests on the runtime's internals and Marshal's closures by design (the 20 on the stable API to a ci shim later); the AOT cell judged at the compiled level; the library order of §4 (superseding OC63); library units loaded on demand.

## As built: OT9's first two fixes (2026-11-05, branch `ocaml-libs1`)

Built on the light tier of the day (no gate, no build of Arbace's executables, no corpus run, stage 0 of main as the tools' host): each
check a list of programs (`--only-list`), the compiled level's builds one shard at a time.

### Fix 1: the compiled AOT level (§4 item 1; OT8 (a))

**What is in the tree**

- `bin/oi check --aot --level compiled [--record] [--only RE | --only-list FILE]` (and `bin/oi suite check --aot --level compiled`, the suite
  corpus's units): the programs dumped as the light level dumps them, translated by o2j in the compiled style (`o2j clib`, `cprogs`: each
  program's units in a package `ocaml.p<N>`, the library's in `ocaml.u`, OC93 (a); unit files that start `(in-ns ...)`, as j2g's trees take
  them), in shards of `O2J_SHARD` programs (default 8); a shard is one Go program: the runtime `jdk/ocaml/`, the library units its programs
  link and theirs, the dispatcher `ocaml.u.Shard` (`o2j shard`: the program `O2J_PROG=p<N>` names, argv.(0) `O2J_ARGV0`) and the main
  package `test/ocaml/o2j/main` (`jrt.RunMain`), translated by `bin/j2g --jdk jdk/forms --input FORMS --root ocaml.u.Shard#main --main ...`
  (**the JDK's forms alone: no Arbace runtime, no compiled namespaces, no base build**, so a shard's j2g is a light run) and built by
  `bin/build-go`; a shard that does not build is split in two until the program that fails is alone (its row `refused: j2g ...` or
  `refused: build ...`). Each program runs alone in its directory under `limited_cpu` (`OI_TIMEOUT`, one Go thread). Judged by
  `test/ocaml/o2j_check.ae` (`O2J_LEVEL`, `O2J_KNOWN`, `O2J_SLOW`) against ocamlopt's runs and oi's: `test/ocaml/check-aot-compiled.edn`,
  `check-suite-aot-compiled.edn`, classes `divergences-aot-compiled.edn`, `divergences-suite-aot-compiled.edn` (the light level's less its
  `:light` classes, plus `:compiled-depth`), known `aot-compiled-known.edn`, slow `aot-compiled-slow.edn`; `bin/oi divergences --compiled`;
  `O2J_KEEP=1` keeps the shards' executables; `make oi-check-compiled`.
- The depth counter (OC92 (a)): every function o2j writes counts its calls (`Rt/enter`, `Rt/leave`), Stack_overflow beyond `Rt/DEPTH_LIMIT`
  (200,000, oi's; `O2J_DEPTH_LIMIT` overrides it for measurements), a handler puts back the depth its `try` was entered at; the depth is the
  lock holder's (`Gil` keeps each thread's in a `ThreadLocal` while it waits).
- Mutual tail calls (OC84 (c), which the compiled level's corpus showed: `basic/tailcalls`' 10^7 mutual steps): the functions of a letrec
  group of several have their body as the method `tb`; a tail call of another function of the group returns a `TailCall` (`jdk/ocaml/ocaml/rt/
  TailCall.ae`) and the entry bounces (`Rt/bounce`), in one frame.
- Preemption: oi's tick (a call's or a loop's turn, every 128 while other domains run: `Rt/tick` in each function's entry, self-tail loop,
  `while` and `for`) and a fair `Gil/yield` (the lock given up until another thread took it): `parallel/pingpong`, `churn` (domains spinning on a
  reference) and `lazy/lazy3` no longer wait forever.
- The runtime as j2g's world needs it (no `arbace.lang`): `=` on objects (`.equals`), a double's `(long ...)` (`unchecked-long`), `/` and
  `unchecked-subtract` on primitives (`Rt`, `Compare`, `ExtSys`: the light level's interpreter took Clojure's meaning); MD5 as the runtime's own
  (`Md5.ae`, RFC 1321: `MessageDigest` asks java.security's file, which a compiled program does not embed) and the random seed from
  `/dev/urandom` (not `SecureRandom`).
- **Finding**: `-m arbace.o2j` ran stage 0's **embedded** copy of o2j, not the checkout's (`RT.toolSource` loads a tool's namespace from the
  checkout only under `arbace/j2g/`, `arbace/repr/`, `arbace/build/`, `arbace/c2j/`, `arbace/go/`). Until today the two were the same file (main's
  `9c68df54d`), so O6-O8's records are unaffected; `bin/oi`'s `o2j` now runs `test/ocaml/o2j/tool.ae`, which loads `arbace/o2j.ae` from the
  checkout (and is in `o2j_key`).
- **A bug of oi's and the runtime's, found by the compiled level** (`opam/uutf/test_uutf`, which neither the light level nor oi had reached
  within the limit): `caml_blit_bytes`/`caml_fill_bytes` of 0 bytes at an offset past the end raised "index out of bounds" (C's memmove of 0
  bytes reads nothing; uutf's encoder blits 0 bytes at an empty buffer's end): `ext_str.ae`, `ExtStr.ae` return at once for 0 bytes.
- The light level's batch ends after a program that leaves domains running (status 4; the driver runs the rest in a new process):
  `parallel/backup_thread`'s domain wrote into the next program's output.

**The numbers** (the 29 programs of the light level's classes, OT8: 24 of the run corpus, 5 of the suite's)

| | |
|---|---|
| run corpus, the light level's 24 (26 speed and 3 depth, less the suite's 5) | **23 pass**, 1 `:compiled-depth` (`parallel/test_issue_11094`: its 100,000 nested effect handlers overflow 200,000 calls; it passes at 1,000,000, OT11) |
| suite corpus, the light level's 5 | **3 pass** (`lib-dynarray/test`, `lib-floatarray/floatarray`, `lib-random/chi2`), `callback/test_gc_alarm` known gc (its alarm is a finaliser, never run: OC68 (a)), `opam/uutf/test_uutf` `:compiled-speed` (120 s of CPU; slow at oi too) after a bug fix (below) |
| **the 29 together** | **26 pass**, 3 classed (depth, gc, speed); the AOT cell's 29 deferred programs (OT8) become 26 passes and 3 non-passes in a class |
| oi's essential subset (`test/ocaml/essential-check.txt`, 58 programs, shards of 20) | **58 pass**, all 58 the same as oi (the light level: 57 and one `:light-speed`); a shard of 20: j2g 120 to 160 s, 2.5 to 2.7 GB; the build 120 to 180 s, 3.2 to 4.2 GB |
| the record, `check-aot-compiled.edn` | 91 programs (the 24, the 10 Unix ones, the 58 less one in both lists): **87 pass**, 4 classed, 0 failing |
| against oi | 22 of the 24 the same, 2 differ (the depth program, and `basic/tailcalls`' timing) |
| a shard of 8 programs | j2g 140 to 235 s, 2.5 to 3.6 GB peak (`GOMEMLIMIT=1700MiB`, 4 Go threads); `bin/build-go` 160 to 220 s, **3.2 to 4.2 GB** peak (gc compiling the shard's one large package `arbace/lang`: 1,640 classes, the JDK's translations with OCaml's); a program 0.1 to 97 s |
| the slowest | `lazy/lazy7` 97 s, `lazy/lazy5` 65 s, `basic/tailcalls` 54 s, `lazy/lazy3` 47 s of wall time (under the 120 s of CPU) |

### Fix 2: the Unix library's core (§4 item 2; OT10 (b))

- `bin/oi stdlib` compiles the pinned tarball's `otherlibs/unix/unix.mli`, `unix_unix.ml` (as `unix.ml`) and `unixLabels.ml{,i}` (`-nolabels`, its
  Makefile's `EXTRACAMLFLAGS`) against the installed stdlib, as `lib/testing`, and dumps their Lambda into `.tmp/oi/unixlib/` (`Unix.ae`,
  `UnixLabels.ae`); the header `include unix` (and `hasunix`) is now a program's include (`bin/lib/oi/header.awk`), whose dump is typed against
  that `unix.cmi` and which runs with the two units before its own (`bin/oi`'s `inc_I`, `inc_units`: loaded only for the programs that include
  unix, OT10 (b)); `bin/oi record-ref` records with `-I +unix unix.cma`/`unix.cmxa` (and `record-ref-only`: `--only` merges).
- oi: `go/arbace/oi/ext_unix.ae`: `Unix_error` (the 68 constant constructors in unix.mli's order, their Linux errnos, `EUNKNOWNERR` for the
  rest; raised through the exception unix.ml registers), `error_message` (oi's strerror texts, Go's), `environment`, `putenv`, `unsetenv`,
  `getpid`, `getppid`, `get[e]uid`, `get[e]gid`, `umask`, `time`, `gettimeofday`, `gmtime`, `localtime`, `mktime`, `times`, `sleep` (outside the
  interpreter's lock), `getcwd`, `chdir`, `stat`, `lstat`, and `inet_addr_of_string` (unix.ml's initialisation calls it). Any other
  `caml_unix_*` fails at its call, as every external oi lacks: the new class `:unix-unported` (divergences.edn, -aot, -aot-compiled).
- The AOT runtime: `jdk/ocaml/ocaml/rt/ExtUnix.ae`, the same less `gmtime`, `localtime`, `mktime`, `times` and the ids: Java's limits are met
  by an environment overlay (`ExtUnix/env`, which `ExtSys`'s getenv reads first), `ExtSys/cwd` for chdir, and a `stat` whose kind,
  permissions (as the running user may read, write, run), size and times are the file's and whose other ints are 0.
- **The count corrected**: of the triage's 9 "core alone" programs, `lib-unix/win-env/test_env` is Windows-only (`target-windows`, a
  `stubs.c`), `lib-systhreads/multicore_lifecycle` needs threads (fix 5) and `parallel/recommended_domain_count_unix` pipes and processes
  (`open_process_full`, fixes 4 and 7); 6 pass, and 2 the triage counted elsewhere pass too (`parallel/major_gc_wait_backup`,
  `lib-unix/common/test_unixlabels`).

| level | the 24 programs `include unix` now runs (systhreads' stay a class) |
|---|---|
| eval (oi) | **8 pass**: `parallel/backup_thread`, `domain_parallel_spawn_burn`, `domain_parallel_spawn_burn_gc_set`, `domain_serial_spawn_burn`, `poll`, `major_gc_wait_backup`, `lib-random/parallel`, `lib-unix/common/test_unixlabels`; 16 `:unix-unported` |
| AOT, light | **7 pass**, 2 `:light-speed` (`major_gc_wait_backup`, `mctest`: `aot-slow.edn`), 15 `:unix-unported` |
| AOT, compiled | of the 8 and 2 unported (`--only-list`, one shard): **7 pass**, `major_gc_wait_backup` known gc (`Gc.quick_stat`'s major collections: zeros in the runtime, OC64 (a); Go's `NumGC` in oi), 2 `:unix-unported` |

The run corpus: 309 to **333 run programs**; oi **273 to 281 pass** (`bin/oi divergences`: 0 failing); the light level **251 to 258**
(`bin/oi divergences --aot`: 0 failing).

### Checks run (all light, by list)

`bin/oi check --only-list` (the 24 Unix programs, `--record`; `test/ocaml/essential-check.txt`: 58 pass), `bin/oi record-ref --only-list`
(the 24), `bin/oi check --aot --only-list` (the 24, recorded), `bin/oi check --aot --level compiled --only-list` (the light level's 24,
`--record`; the 10 failing and the depth program again; the 10 Unix programs), `bin/oi suite check --aot --level compiled --only-list` (the
5, `--record`; 2 again after the blit fix), `bin/oi test` (go vet, go test: ok, after each change of oi), `bin/oi divergences --compiled` (30 pass of 34,
0 failing; the suite's 3 of 5, 0 failing), `bin/oi divergences`, `divergences --aot`,
`bin/oi check --aot --only-list test/ocaml/essential-check.txt` (the light level after the runtime's changes: no row changed),
`O2J_SHARD=20 bin/oi check --aot --level compiled --only-list test/ocaml/essential-check.txt --record` (58 pass), `make check-makefile`,
`bin/oi run`/`aot-run` probes (depth, mutual tail calls, a domain spinning, the Unix core against ocamlopt's output byte for byte), an MD5
check against `md5sum`. **Not run**: the gate, any corpus run, `bin/arbace-go`; the light records of the programs that do not include unix
were not rerun after the runtime's changes (the tick, the letrec groups' `tb`, `Md5`), nor oi's after `ext_unix` (the night's runs below).

### The night's record commands (in this order; heavy: the compiled level)

    bin/oi check --record                         # oi, the run corpus (333 programs; ext_unix, the header)
    bin/oi suite check --record                   # oi, the suite corpus
    bin/oi check --aot --record                   # the light level: the runtime changed (tick, tb, Md5)
    bin/oi suite check --aot --record
    O2J_SHARD=20 bin/oi check --aot --level compiled --record        # the compiled level, every run program: 17 shards, about 2 h (a shard of 20
                                                                     # costs as one of 8: j2g 2.7 GB, the build 4.2 GB at most)
    O2J_SHARD=20 bin/oi suite check --aot --level compiled --record  # and the suite's
    bin/oi divergences && bin/oi divergences --aot && bin/oi divergences --compiled && bin/oi pins

then OT8 (a)'s matrix change: `test/reap/cells.edn`'s OCaml AOT cell from `check-aot-compiled.edn` and `check-suite-aot-compiled.edn` (with
their classes), `bin/reap record`. Until the compiled record covers the corpus the cell stays the light level's.

### Proposed (OT11-OT15; none decided)

- **OT11 The compiled level's depth limit.** (a) 200,000 calls, oi's (OC69 (a)), as built: `parallel/test_issue_11094` in `:compiled-depth`;
  (b) 1,000,000 (Go's goroutine stacks grow to 1 GB: the program passes in 2.6 GB of memory; a deep plain recursion's cost per call to be
  measured); (c) count only the calls of non-tail positions. *Recommended: (a)*, (b) if the night's whole corpus shows more programs over
  200,000 that ocamlopt's stack holds.
- **OT12 The compiled level in the gate.** (a) a `--full` step `oi-aot-compiled`, heavy (its builds take 3 to 4 GB), GT8.4's `:rotate` over
  `test/ocaml/subsets.edn`, and `bin/oi divergences --compiled` in the essential `oi-divergences`; (b) by hand at night only. *Recommended: (a)*
  once the night's record exists (OC95 (a) named the compiled level's subset as the AOT essential step: later, with its time measured).
- **OT13 Unix's error texts.** oi's and the runtime's `error_message` are Go's texts (glibc's wording); the reference ocamlopt runs on musl,
  whose texts differ for some errnos. (a) Go's, a class when a test prints one; (b) musl's table (68 strings). *Recommended: (b)* with fix 4,
  whose tests print errors.
- **OT14 The JDK's resource data in compiled programs.** (a) the runtime avoids the classes that read them (as built: `Md5`, `/dev/urandom`);
  (b) embed `jdk/data` in the main package as j2g's `--program` does. *Recommended: (a)*: the runtime's world stays small.
- **OT15 The compiled level's builds.** A shard's build peaks at 3 to 4 GB (one package of 1,640 classes). (a) at night under the heavy lock;
  (b) the JDK's translated classes in a package of their own built once (the shards share jrt's and the JDK's compiled objects in Go's cache).
  *Recommended: (a) now, (b) when the gate step is added*: it would turn a shard into j2g plus a small link.

## As built: OT9's next three fixes (2026-11-05, branch `ocaml-libs2`)

Built on the light tier of the day, as the first two (no gate, no build of Arbace's executables, no corpus run, stage 0 of main as the
tools' host): each check a list of programs (`--only-list`), the records merged (`--record` with a list), the compiled level one shard at
a time. The AOT runtime's mirror of fixes 4 and 5 (`jdk/ocaml/ocaml/rt`) is the section "The AOT mirror" below.

### Fix 3: the harness (§4 item 3)

- **One row a program across the two corpora.** A program the run corpus's header reading (`bin/lib/oi/header.awk`) classes but the
  suite's reading (`actions.awk`: `modules`, `readonly_files`, explicit build steps) runs is now the suite corpus's alone: `bin/oi corpus`
  leaves it out, so `bin/oi suite corpus` takes it. Before, 6 programs (`basic-modules/main`, `basic-multdef/usemultdef`,
  `basic-private/tlength`, `misc-kb/kbmain`, `raise-counts/main`, `typing-shadowing-of-pervasives-submodules/redefine_largefile`) passed
  in the suite and were deferred in the run corpus at once: the matrix's eval and AOT cells counted them twice. Their stale run-corpus
  rows (and those of the 7 other moved programs) are dropped from `check-oi.edn` and `check-aot.edn`.
- `readonly_files` of data of any plain name (a C file stays a class): `basic-io-2/io`; `not-bsd` (a platform filter):
  `memory-model/{forbidden,publish}`; `flags = -safer-matching`, passed through by the oracle (`tools/ocdump`'s new `-safer-matching`,
  `Clflags.safer_matching`): `basic-more/safer_matching` (run corpus).
- **ocamltest's explicit build steps** (`actions.awk`'s build mode): from `setup-ocamlc.byte-build-env` to its `check-program-output`, the
  modules compiled one by one, a library made of some (`program = "lib.cma"`, `-a`), the program linked from `all_modules` and run: the
  run's units are the link's, a library expanded to its modules (`badly-ordered-deps/coccinelle`, `required-external/main`). A pack
  (`-pack`), a second executable (OT3's subprocess tests: `lib-filename/quotecommand`) and steps that end without a run stay classes;
  ocamltest's `${test_build_directory}` no longer splits the header (an argument naming it stays a class).
- One place for a program's libraries, both corpora: `inc_I`, `inc_units`, `inc_ref` (the reference compilers' flags) in `bin/oi`;
  `bin/oi suite record-ref --only-list` merges (`record-ref-only`), as the run corpus's did; the suite reads `include unix` and
  `include systhreads` too.
- `link-test/test` stays a class (packs: `-for-pack P`, `-pack`, a pack is not a unit of oi's): **12 of the 13** run.

| level | the 13 |
|---|---|
| eval (oi) | **12 pass**: 6 new (`basic-more/safer_matching` in the run corpus; `basic-io-2/io`, `memory-model/forbidden`, `memory-model/publish`, `badly-ordered-deps/coccinelle`, `required-external/main` in the suite), 6 already passing in the suite now counted once; `link-test/test` a class |
| AOT, light | the 6 new: **4 pass**, `memory-model/{forbidden,publish}` `:light-speed` (`aot-slow.edn`: 10^6 iterations of a litmus test each, over 120 s interpreted) |

### Fix 4: Unix's descriptors and files (§4 item 4)

- oi: `go/arbace/oi/ext_unix_fd.ae`, over Go's syscall, a `Unix.file_descr` the OS's descriptor: `open` (the `open_flag` list, `O_CLOEXEC`
  as asked), `close`, `fsync`, `read`, `write` (the whole buffer, retried; `EAGAIN` after a partial write returns the count), `single_write`,
  the bigarray read and write, `in_channel_of_descr`/`out_channel_of_descr` (oi's channels over the descriptor), `lseek` and its 64-bit form,
  `truncate`, `ftruncate`, `fstat` and the `LargeFile` stats, `isatty` (`TCGETS`), `unlink`, `rename`, `link`, `realpath`, `chmod`, `fchmod`,
  `access`, `dup`, `dup2` (`?cloexec`), `set_nonblock`/`clear_nonblock`, the close-on-exec flags, `mkdir`, `rmdir`, `mkfifo`, `readlink`,
  `symlink`, `has_symlink`, `utimes` (0.0 and 0.0: now), `pipe`, the directories (`opendir` reads getdents64's entries, `.` and `..` among
  them, `readdir`, `rewinddir`, `closedir`), `Unix._exit` (no `at_exit`, the channels not flushed). Each call that may block runs outside the
  interpreter's lock (the other threads and domains run meanwhile). `select`, `lockf`, the processes, signals and sockets stay
  `:unix-unported` (fixes 6 and 7).
- `Unix.fork` by design, OT1 (a): the class `:unix-fork` (`:kind :design`) in the six run-level classes files.
- `lib-channels/close_during_flush` needs a signal (`Unix.alarm`'s handler empties a full pipe a write blocks on): without it the exit's
  flush blocks until the wall cap (4 × `OI_TIMEOUT`); a known entry (`oi-known.edn`, "unix:"), `:unix-unported` until fix 7.

| level | |
|---|---|
| eval, run corpus (the 16 `:unix-unported` of fix 2) | **6 pass**: `lib-unix/common/{dup,bigarrays,rename,truncate}`, `lib-unix/realpath/test`, `parallel/backup_thread_pipe`; 10 classed: `fork_cleanup` (`:unix-fork`), `process_pid`, `pipe_eof`, `parallel/recommended_domain_count_unix` (`create_process`, fix 7), `channel_of`, `gethostbyaddr` (sockets, fix 6), `lib-sys/signal` (`kill`), `parallel/mctest` (`select`), `lib-bigarray-file/mapfile` (`map_file`, fix 11), `close_during_flush` (above) |
| eval, suite (newly read: `include unix`) | **3 pass**: `lib-unix/common/{append,utimes,uexit}`; 5 classed: `getaddrinfo`, `sigwait` (`sigprocmask`), `recvfrom_unix` (sockets), `multicore_fork_domain_alone{,2}` (`:unix-fork`) |

### Fix 5: threads (§4 item 5)

- `bin/oi stdlib` compiles `otherlibs/systhreads`' `thread.ml{,i}` and `event.ml{,i}` against unix.cmi into `.tmp/oi/threadlib/` (`Thread.ae`,
  `Event.ae`), loaded after Unix's units by the programs that `include systhreads` (OT10 (b)); `hassysthreads` is read, and a later
  alternative that skips (`{ include systhreads; hassysthreads; }{ reason = ...; skip; ... }`, lib-threads' headers) leaves the first.
- oi: `go/arbace/oi/ext_thread.ae`: a thread is a goroutine of its domain under the interpreter's lock, which plays OCaml's master lock (given
  up in the blocking calls and at the calls' polls while more than one goroutine runs, as the tick thread's forced yield); `Thread.t` the block
  `[| id |]`, `join` waits outside the lock; threads first take the lock in the order they were created (OCaml's master lock is a FIFO:
  `lib-threads/tls`); `caml_thread_uncaught_exception` prints st_stubs.c's message; `exit` in a thread ends the process; an internal error in a
  thread is reported as the main program's. Mutex and Condition are the stdlib's (ext_domain.go, already); `Thread.delay` is `Unix.sleepf`.

| level | the 28 run-corpus and 7 suite programs `include systhreads` now runs |
|---|---|
| eval, run corpus | **17 pass**: `lib-domain/DLS_thread_safety`, `lib-systhreads/{boundscheck,multicore_lifecycle,testpreempt}`, `lib-threads/{bank,beat,bufchan,close,fileio,mutex_errors,pr7638,prodcons,prodcons2,swapchan,tls,torture}`, `parallel/multicore_systhreads`; 11 classed: 3 `:unix-fork` (`testfork`, `testfork2`, `fork_cleanup_systhreads`), 5 `:unix-unported` (sockets: `pr4466`, `pr5325`, `sockets`; `sigprocmask`: `eintr`, `threadsigmask`), `backtrace/callstack` (backtraces), `statmemprof/moved_while_blocking` (gc), `parallel/fib_threads` (speed: fib 42 on four domains, `oi-slow.edn`) |
| eval, suite | **4 pass**: `lib-channels/input_all`, `lib-systhreads/testyield`, `lib-threads/backtrace_threads`, `lib-threads/pr8857`; 3 classed (`parallel/catch_break`'s `sigprocmask`, 2 statmemprof gc) |

### The numbers (eval)

The run corpus 333 to **362 run programs** (543 with a `.reference` in it: 13 moved to the suite), oi **281 to 305 pass**, 0 failing
(`bin/oi divergences`); the suite corpus 98 to **118 run programs**, **87 to 99 pass**, 0 failing (`bin/oi suite divergences`). The matrix's
eval cell (`check-oi.edn` + `check-suite-oi.edn`) **368 to 404 pass**, the 6 double-counted programs counted once. oi's essential subset: 58 of 58.
