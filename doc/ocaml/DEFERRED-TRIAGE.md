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
measured 0.7 of "alone" regained; this plan uses the same factor. oi's passes show the base rate: 273 of 556 programs of the run corpus pass,
of the 309 not classed by header 273 (88%).

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

Total 1-11: eval **49 + 22 AOT mirror = 71 days**, **126 tests at eval, 155 at AOT**; every fix above 8 is under 2.5 tests a day. The first five (1-5):
**19 days eval (+ 8 AOT mirror)**, **54 tests at eval (13 + 9 + 17 + 15), 83 at AOT** (adding the 29 light). 0.7 × alone: about 38 / 58.
After every fix and every Proposed move, the deferred programs left are the 8-10 of tails (the `torture`-like timing tests, `bank`...).

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
`unix_sleep` the 5.x name `caml_unix_sleep`; get the list from the units: `bin/oi aot-lib` reports missing ones, as for the stdlib), the core ones real (time, `gettimeofday`, `getpid`,
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
- **OT8 The AOT cell's level.** (a) the compiled level when recorded (fix 1), the light level kept as the quick check and its classes `:light-*` by design; the matrix's AOT cell judged from the compiled record (29 deferred gone, the 100%-by-class light rows no longer counted); (b) both levels counted: the light level's 29 stay deferred. *Recommended: (a)*; the 3 depth programs are fixed by the compiled level's stack (OC92) and the 26 speed ones by its speed.
- **OT9 The order of the libraries.** OC63 (a) says unix and str, then bigarray, then systhreads; the counts say unix core, the harness, descriptors, threads, sockets, processes, then compiler-libs; Str (1.0 a day) and bigarray (1.0) after the C shim. (a) as ranked here; (b) OC63's order. *Recommended: (a)*; it supersedes OC63's order only.
- **OT10 Where the library units live.** (a) the units of `otherlibs` (`unix.ml`, `thread.ml`, `event.ml`, `str.ml`, `compiler-libs`) built into the library set like the stdlib, every program of every check loading them (a start-up cost: the stdlib's 10 CPU seconds at the light level, OC88, are shared per process; Unix is 4,000 lines of OCaml); (b) loaded only for a program that names them (`include unix`, found by the header or by the program's free modules), the way the stdlib's units are loaded on demand. *Recommended: (b)*: no cost for the 273 passing programs, and the AOT's `ocaml.u` package stays the stdlib's.

## 7. After all of it

If OT1-OT9 are accepted and items 1-11 built (71 days): eval 360 + about 100 = **460 pass**, 114 + 79 = **193 by design**, about 11 deferred; AOT 337 + about 125 =
**460 pass**, 108 + 79 = 187 by design, about 15 deferred. By the 0.7 factor, minus 30%: eval about **430**, AOT about **425**. The first five alone, 19 days: eval 360 to about 400, AOT 337 to about 395.
