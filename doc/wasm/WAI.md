# wai: Arbace's WebAssembly engine

wai runs WebAssembly modules (doc/wasm/WASM-SURVEY.md §6, WAS6 (a), WAS7 (a), the user 2026-11-13; "As built: WA4"). It is Go forms,
`go/arbace/wai` (a crop path of `bin/lib/version.bash`'s `arbace_crop_paths`: out of every key), built with its light driver
`test/wasm/wai-driver` by TamaGo's go alone (`bin/wai light`: printed by repr/go with `bin/build-go --print-only`, then `go build`,
as x86i's and a64i's drivers are; no Arbace build). It was written after the specification's semantics (WebAssembly/spec at tag
wg-3.0, `interpreter/exec/eval.ml`, `interpreter/runtime/`, `interpreter/host/spectest.ml` and `interpreter/script/run.ml` read for
the order of effects and the messages); wasmi's register IR and wazero's interpreter (v1.12.0, `.tmp/wasm/tree`) were read as
references for the shape of an internal code, nothing of either converted. Its Go was written in a module copy and converted to forms
by repr/go's native reader (`bin/repr-go read --goos linux --goarch amd64`, one package at a time, AXS68 (a)'s way).

## 1. Breadth

Wasm 3.0 and the legacy exceptions (WA8), but threads: the MVP, sign extension, saturating conversions, multi-value, bulk memory and tables,
reference types, multiple tables and memories, memory64 and table64, SIMD128 and relaxed SIMD, tail calls, exceptions (`try_table`,
`throw`, `throw_ref`, `exnref`), typed function references, extended constant expressions, GC (structs, arrays, `i31`, casts,
`any.convert_extern`, the subtyping of iso-recursive canonical types), and since WA8 the legacy exceptions (`try`, `catch`,
`catch_all`, `delegate`, `rethrow`; §3), and since queue 326 the wide arithmetic proposal (`i64.add128`, `i64.sub128`, `i64.mul_wide_s`,
`i64.mul_wide_u`: 0xfc 19-22, two i64 results, the low half first; `math/bits`' `Add64`, `Sub64`, `Mul64`). A module using what is not built (a shared memory, an atomic) is refused at decoding as
`unimplemented: FEATURE (WHAT)`, not as malformed.

Since WA5 wai validates every module before it decodes it (`valid.go`, §7; WAS30 (a)): the driver runs no module its own validator
rejects, and the decoder still assumes a valid module (a fault it meets is a decode error, never a crash past the engine's boundary).

## 2. Values and the store

- A value is `V{L, H uint64}`: a number in L (an i32 zero-extended, a float as its bits), a v128 in L (bytes 0-7) and H (8-15), a
  reference as H its kind (0 null) and L its payload: `rFunc` (the store's function index), `rHost` (a host value: the scripts'
  `ref.host N`), `rI31` (the 31 bits), `rStruct`, `rArray`, `rExn` (the store's object index); the flag `rExt` marks an external
  reference (`ref.extern N` is `rHost|rExt`; `extern.convert_any` sets it, `any.convert_extern` clears it). Every value is one stack
  slot of 16 bytes, so the internal code needs no types (a `drop` or `select` of a v128 moves one slot).
- Types are canonical for the process: each rec group keyed by its shape (references inside the group relative, outside it by their
  canonical ids), so iso-recursive equivalence is id equality and subtyping walks the declared supertypes (`types.go`).
- Instances, functions (Wasm's and the host's), tables, memories (a byte slice, 64 KiB pages), globals, tags, element and data
  segments with their dropped state (nil), the exports; instantiation in eval.ml's order (imports matched by `match.ml`'s rules,
  then tags, functions, globals, tables with their initializers, memories, data, elements, exports; the active and declarative
  element segments, the active data segments, the start function), a trap leaving the earlier effects.
- The implementation's limits: a memory grows to at most 16,384 pages (1 GiB), a table to 10,000,000 elements, an array to 2^24
  elements; growth beyond fails as the spec allows (`memory.grow` and `table.grow` return -1).

## 3. The internal code

Each function body (and each constant expression) is translated at decoding into a flat array of `Ins{Op, A, B, D, E, C}`
(`compile.go`): the stack's height tracked statically (the locals first, then the operands, relative to the frame's base), so a branch
carries its arity, its target's height and its target's pc (`br`, `br_if`, `br_table`'s side table, `br_on_null`, `br_on_non_null`,
`br_on_cast`, `br_on_cast_fail`); `block` and `loop` leave no instruction, `if` is a conditional jump, `else` a jump; the code after an
unconditional transfer is not translated. `try_table`'s catch clauses become a handler (the range of pcs it covers, each clause's tag,
target pc, height and arity), inner handlers first. The legacy exceptions (WA8, WAS52 (a)) take the same handlers: a `try`'s body is
a handler's range, closed at its first `catch`, `catch_all` or `delegate`; a `catch TAG` or `catch_all` is a clause of kind
`catchLegacy` or `catchAllLegacy` whose code starts after a jump from the previous part to the end, the caught exception's reference in
the slot at the try's height (below the tag's values: the catch block's operands start one above), so `rethrow L` is `local.get` of
label L's slot and `throw_ref`; a `delegate L` is a clause of kind `catchDelegate` whose count is the active handlers of the blocks
between the try and label L (a `try_table`'s, a legacy try's before its first catch): `Machine.catch`, walking the handlers whose range
holds the raising pc innermost first, skips that many after it, so the exception goes on from label L. The op numbers are repr/wasm's table's (`optab.go`, generated by `bin/wai ops gen`
from `arbace/repr/wasm/ops.ae`, `test/wasm/gen_wai_ops.awk`; `bin/wai ops check` compares the driver's table with ops.ae row by row).

## 4. The interpreter

`exec.go`: one loop over the code with the frame's base and the stack's top in locals; a call is a Go call (`run`), counted against
`MaxDepth` (20,000: `call stack exhausted`, never Go's stack overflow); a tail call (`return_call*`) replaces the frame in place. A
trap is a Go panic of `*Trap` recovered at the engine's boundary (`Machine.Call`, `Instantiate`) into an error, as an exhaustion, an
uncaught exception (`*Uncaught`) and a link failure (`*LinkError`) are; any other panic is a crash, reported with its first frames.
An exception thrown is looked up in the frame's handlers at its pc, then returned to the caller, which looks it up at its call's pc.
Floating point: Go's float32 and float64 operations (each result stored, so nothing fuses); `min`, `max`, `nearest`, `ceil`, `floor`,
`trunc` and the conversions by the spec's rules, a NaN operand's result quieted; `relaxed_madd` fused and rounded once (an f32 one
through a sum rounded to odd, `fma32`). SIMD (`simd.go`): each lane operation written out over the 16 bytes; the relaxed operations
take the reference interpreter's choices (eval_vec.ml: swizzle, trunc_sat, fused madd, bitselect, min and max, q15mulr_sat_s, the
signed dot products). The trap messages are eval.ml's (`unreachable executed`, `integer divide by zero`, `integer overflow`,
`invalid conversion to integer`, `out of bounds memory access`, `out of bounds table access`, `undefined element N`,
`uninitialized element N`, `indirect call type mismatch`, `null function reference`, `null structure reference`, `null array
reference`, `null i31 reference`, `null reference`, `null exception reference`, `out of bounds array access`, `cast failure`).

## 5. The testsuite's check (`bin/wai check`)

1. **The plan** (`arbace.wasm.check`, `arbace/wasm/check.ae`, on the tools' host): each script read by repr/wasm's lexer, its commands
   numbered as `bin/repr-wasm wast` numbers them (from 1; a script of a module's fields alone is that module, command 1); each module
   written as a binary into `WORK/mods/SCRIPT/K.wasm` (a `binary` module's own bytes, a text or `quote` one encoded canonically by
   arbace.repr.wasm.binary), each command a plan line.
2. **The run** (`waidriver wast PLANS.LIST ROWS`): a store, the registry with `spectest`, the modules and instances by name, each
   command judged by its own assertion; the rows `[ID RESULT REASON]` (ID `testsuite/SCRIPT#K`, and `msg/SCRIPT#K` for a message).
3. **The judgment** (`test/wasm/judge.ae`, kind `eval`): the rows against `test/wasm/eval-results.edn`, every non-pass in a class of
   `test/wasm/divergences-eval.edn`, the negative controls of `test/wasm/controls-eval.txt` failing (§5.4).

### 5.1 Plan lines

Tab-separated, `K KIND FIELDS...`: `module NAME FILE` (decoded, linked, instantiated: the row passes when it instantiates),
`definition NAME FILE` (decoded), `instance INST MOD`, `register NAMEHEX INST` (no row), `action ACT`, `assert_return ACT RESULTS`,
`assert_trap ACT MSGHEX`, `assert_trap_module FILE MSGHEX` (a module whose instantiation must trap), `assert_exhaustion ACT MSGHEX`,
`assert_exception ACT`, `assert_unlinkable FILE MSGHEX`, `assert_uninstantiable FILE MSGHEX`, `fail REASON` (the planner could not
carry the command). ACT is four fields, `invoke INST NAMEHEX ARGS` or `get INST NAMEHEX` (ARGS empty); NAME and INST `-` for none
(the latest); strings hex-encoded UTF-8. `assert_malformed` and `assert_invalid` are the read cell's (WAS9 (a)) and have no line.

### 5.2 Value tokens

Arguments: `i32:HHHHHHHH`, `i64:H{16}`, `f32:HHHHHHHH`, `f64:H{16}` (the bits), `v128:H{32}` (the 16 bytes in memory order),
`null:HEAP`, `extern:N`, `host:N`. Expected results also: `f32:canonical`, `f32:arithmetic` (and f64's), `v128:SHAPE:L,L,...` (lanes
as hex bits or a NaN pattern), `ref:KIND` (the reference interpreter's `RefTypePat`: `func`, `extern`, `any`, `eq`, `i31`, `struct`,
`array`, `exn`, with run.ml's quirks: `ref:any` is anything but a function, `ref:extern` anything), `either:A|B|...`.

### 5.3 The judges

As the reference runner's (`interpreter/script/run.ml` at wg-3.0, WAS8 (a)): `assert_return` the results by `assert_result` (numbers
bit for bit, the NaN patterns by their bits, references by kind, each result also of its type's kind); `assert_trap` trapped (a return,
an exhaustion or an exception fails it); `assert_exhaustion` exhausted; `assert_exception` an uncaught exception;
`assert_unlinkable` a link failure; `assert_uninstantiable` and `assert_trap` of a module a trap during instantiation. Each trap's
and link failure's message against the script's by prefix is a `msg/` row, the second level (`:beside` in the REAP cell).

### 5.4 The controls

`waidriver control PLAN MODDIR K MODE [PLAN2 K2]`: the plan run up to its command K, which is then judged in another role:
`wrong-value`, `as-trap`, `as-return`, `vs`, `msg-vs`, `as-module`, `as-uninstantiable` must fail (`differs`); `self` (the command
unchanged) must pass (`same`), a `_check/` line. Since WA5 the validator's rows too: `valid-flip` (a module's `valid/` row judged
against the opposite of arbace.wasm.valid's verdict) must fail, `valid-self` must pass.

## 6. WASI preview 1 (`wasi.go`)

The module `wasi_snapshot_preview1` over one guest's world (`WASI`: its arguments, environment, standard streams, preopened
directories, a seed), written after the snapshot's witx (WebAssembly/WASI, `legacy/preview1`) as Go's `syscall/fs_wasip1.go` and
`runtime/os_wasip1.go` read it; wazero's `imports/wasi_snapshot_preview1` (v1.12.0, `.tmp/wasm/tree`) read for the layouts, nothing
of it converted. The 36 names Go's wasip1 port imports (runtime, syscall, internal/syscall/unix; the survey's "38" counted two
names declared twice) and the rest of the snapshot's:

- **arguments and environment**: `args_sizes_get`, `args_get`, `environ_sizes_get`, `environ_get`;
- **clocks and randomness**: `clock_time_get` (0 the real clock, 1-3 the monotonic one from the world's start), `clock_res_get`,
  `random_get` (a generator seeded by the world, `--seed`, so a check's runs are reproducible);
- **the process**: `proc_exit` (an `*Exit` panic recovered at the engine's boundary: the guest's status), `proc_raise` (ENOSYS),
  `sched_yield`, `poll_oneoff` (clock subscriptions, relative or absolute, sleep until the earliest; an `fd_read` or `fd_write`
  subscription is ready at once: Go's netpoll on wasip1);
- **descriptors**: `fd_write`, `fd_read`, `fd_pwrite`, `fd_pread`, `fd_close`, `fd_seek`, `fd_tell`, `fd_sync`, `fd_datasync`,
  `fd_advise`, `fd_allocate` (ENOTSUP), `fd_renumber`, `fd_fdstat_get`, `fd_fdstat_set_flags` (append, nonblock),
  `fd_fdstat_set_rights`, `fd_filestat_get`, `fd_filestat_set_size`, `fd_filestat_set_times`, `fd_prestat_get`,
  `fd_prestat_dir_name`, `fd_readdir` (`.` and `..` first, then the entries sorted by name; the cookie an entry's ordinal);
- **paths**: `path_open` (the rights choose read, write or both; `O_CREAT`, `O_EXCL`, `O_TRUNC`, `O_DIRECTORY`, append, the lookup
  flag's symbolic-link following), `path_filestat_get`, `path_filestat_set_times`, `path_create_directory`, `path_remove_directory`,
  `path_unlink_file`, `path_readlink`, `path_symlink`, `path_link`, `path_rename`;
- **sockets**: `sock_accept` (ENOTSUP, EBADF for no descriptor), `sock_shutdown` (ENOTSOCK), `sock_recv`, `sock_send` (ENOTSUP).

**The sandbox.** Descriptors 0-2 are the standard streams, 3 on the preopened directories in their order (`--dir GUEST=HOST[:ro]`),
then the opened files. A path is resolved component by component inside the preopen of its directory descriptor: `.` and empty
components dropped, `..` never above the preopen (ENOTCAPABLE), a symbolic link (an intermediate component, or the last one when the
lookup follows) read and spliced in, an absolute link target refused (ENOTCAPABLE), at most 40 links (ELOOP); an absolute path is
refused. Under a read-only preopen every change (a write, a creation, a truncation, a removal, a rename, a link, the times) is EROFS.
Host errors map to the snapshot's errnos (`wasiErr`); a guest pointer outside its memory is EFAULT.

**The driver's run** (`waidriver run [--dir GUEST=HOST[:ro]]... [--env K=V]... [--seed N] [--no-validate] MODULE [ARG...]`): the
module validated (§7), decoded, linked against `wasi_snapshot_preview1` alone, its memory bound, its `_start` called; standard
output buffered and flushed before each write to standard error (the program's order kept); the status the guest's (`proc_exit`'s, 0
when `_start` returns), or wai's own: 125 a module wai does not run (unimplemented), 126 an invalid module, a link failure, a trap or
a crash, each with a line `wai: ...` on standard error.

## 7. The validator (`valid.go`)

`Validate(bytes)`: the module's sections read again on their own (not the decoder's pass) and checked by Wasm 3.0's typing rules, as
the specification's validation algorithm (its appendix) and valid.ml at wg-3.0 state them, which arbace.wasm.valid (WA3) follows too:
the type section's rec groups (references inside the group or before it, at most one supertype, declared before its subtype, not
final, its composite type matching), canonicalized as the decoder does (`canonGroup`, so subtyping is the decoder's); limits
(memories at most 2^16 pages for i32 and 2^48 for i64, tables 2^32-1 and 2^64-1, minimum not above maximum); imports, functions,
tables (a table without an initializer of a nullable element type), memories, tags (no results), globals (their initializers seeing
the globals before them), exports (indices known, names unique), the start function (`[] -> []`), element segments (their type
matching the table's), the data count, the bodies, the data segments. A constant expression admits the constants, `ref.null`,
`ref.func`, `ref.i31`, the extended constants' `add`, `sub`, `mul`, `global.get` of an immutable global, `struct.new[_default]`,
`array.new[_default|_fixed]` and the two conversions; a function body is checked over an operand stack of value types (an unknown
type, K 0, below an unreachable frame's height; an unknown heap type `hBot`) and a control stack of frames (block, loop, if, else,
try_table with its catch clauses checked against the enclosing labels), the non-defaultable locals' initialization undone at each
frame's end, `ref.func` of a function declared outside the bodies. The numeric and vector instructions' signatures come from their
names, as arbace.wasm.valid's `simple-sig` and `mem-size`. Results: nil, `*Invalid` (a message after valid.ml's), `*Malformed` (a
module it cannot read: not judged as invalid) or `*Unimplemented` (the atomics). The legacy exceptions since WA8: `try` a frame
(0x06), `catch TAG` and `catch_all` frames after it (0x07, 0x19: the tag's values in, the try's results out), `delegate L` its end with
L checked in the enclosing frames (`unknown label`), `rethrow L` requiring a catch frame (`invalid rethrow label`).

**Its judgment** (WAS30 (a), the `valid/` rows, `:beside` in the eval cell): the planner writes, for every module of the testsuite it
writes as a binary (a module command's, an `assert_unlinkable`'s, `assert_uninstantiable`'s and `assert_trap`'s module's) and for every
`assert_invalid`'s module, a line `K valid FILE VERDICT`, VERDICT arbace.wasm.valid's on the module's items (`ok`, `invalid:MSGHEX`,
or `none:REASONHEX` when they do not read); the driver's row `valid/SCRIPT#K` passes when wai's validator agrees (both accept, or
both reject). The messages are not compared (valid.ml's wording is the read cell's second level).

## 8. The Go run corpus (`bin/wai go`)

WAS11 (a): Go programs built for `GOOS=wasip1 GOARCH=wasm` by TamaGo's go1.27.1 at check time (not a host toolchain) and run by the
driver (`bin/lib/wai/go-run.bash`).

1. **The programs**: `$GOROOT/test`'s run programs (the ids of goi's record `test/goi/check-linux-amd64.edn` whose action is `run`, as
   Go's test driver reads the first line: 1,051 of its 1,837; the directory tests since WA5b, item 6; the errorcheck programs are not
   runs),
   and Go's own small programs of `test/wasm/go-run.txt` (`test/wasm/go/NAME/main.go`: hello, goroutines, maps, fmt, time, files).
2. **The build**: as goi's gc reference builds the original (GOPATH mode in its directory, the action's build flags and the files
   it names, `run cmplxdivide1.go`; an action setting `-goexperiment` or `-godebug` a skip, as goi's; a program Go's test driver would
   not run on wasip1/wasm by its build constraints, `waidriver buildok` after cmd/internal/testdir's `shouldTest`, a skip, class :na), cached by the source's, the flags' and the toolchain's hashes in the main
   checkout's `.tmp/wai-go/bin`; a build that build constraints refuse for wasip1/wasm is a skip.
3. **The run**: `waidriver run` in `$GOROOT/test` with `$GOROOT` preopened read-only at its own path (`$GOROOT/test` the guest's
   working directory, `PWD`), `/tmp` a fresh scratch directory (`TMPDIR=/tmp`), the environment Go's test driver gives a run test (goi's
   `run_env`), the action's arguments; under `limited` at three times the action's limit (`WAI_GO_SLOW`), the driver's
   `GOMEMLIMIT` 1200 MiB; three programs at a time (`WAI_GO_JOBS`).
4. **The judge**: goi's: against gc's linux/amd64 build and run of the original (its reference, goi's cache `.tmp/goi-check/gcref` read
   by goi's own key, a missing one made the same way into `.tmp/wai-go/gcref`): exit status, standard output byte for byte, standard
   error up to the goroutines' traces; a program gc's own run does not pass is a skip. Go's own programs by their `want.out` (status
   0, standard error empty). Rows `[go/ID RESULT REASON]` against `test/wasm/go-results.edn`, every non-pass in a class of
   `test/wasm/divergences-go.edn`.
5. **The controls** (`test/wasm/controls-go.txt`): a subject's run judged against another program's gc reference must fail; `self`
   must pass.
6. **The directory tests** (WA5b, WAS34 (a)): goi's record's rundir, buildrundir and runindir tests and `test/wasm/go-dirs.txt`'s
   (`wasmmemsize.go`'s `//go:build wasm` keeps it out of goi's linux record), built for wasip1 as cmd/internal/testdir builds them
   (`gorun_dir_build`: rundir's packages of the `.dir` compiled in the files' order by `go tool compile -e -D test -p test/NAME`
   against `go list -export std`'s export data for wasip1 and the last linked as main; buildrundir's `.go` files compiled as main
   with its `.s` files assembled, packed and linked; runindir's directory copied into a module named after it, `go build .`),
   cached by the directory's files' digest; run by wai in Go's driver's directory (rundir's and buildrundir's a fresh work
   directory, runindir's the module's copy, preopened beside `$GOROOT`), standard output and error combined, judged as the
   driver judges them: status 0 and the golden `.out` byte for byte (none: no output). Their controls, the mode `golden [OTHER]`:
   a run against another directory test's golden output must fail, against its own pass.

## 9. w2g, the AOT level (`w2g.go`; WA7)

WAS10 (a): Wasm translated to Go and built by TamaGo's go. The unit is wai's internal code (§3), not the binary: its static stack
heights (`Code.H`, each instruction's height before it runs, recorded by the compiler since WA7) make every operand a slot of the
frame known at translation, and its branches carry their arity, target height and target pc. A defined function becomes one Go
function `func(m *Machine, f *Func, fp int) *Exn` over the machine's stack (the interpreter's calling convention: the arguments at
the frame's base, the results left there), so translated functions, interpreted ones and the hosts call one another freely:

1. **the body**: a label `Lpc` before each instruction a branch targets, then each instruction as Go: `sp = fp + H` (a constant the
   compiler folds), then its statements;
2. **the transfers of control** written by w2g: `br` as `br()`'s move and a `goto`, a branch to the function's end as the results'
   move and `return nil`, `br_if` and `if` as Go's `if`, `br_table` as a `switch` (its entries grouped by target), `br_on_null`,
   `br_on_non_null`, `br_on_cast[_fail]`; `call`, `call_indirect` and `call_ref` as the interpreter's call sequence (a host's call,
   or `Machine.run` on the callee: its depth counted, a translated callee run as Go); `throw` and `throw_ref` returning the
   exception;
3. **every other instruction** the interpreter's own case of `Machine.exec`'s switch, verbatim: `w2gCases` (`w2gcases.go`) is
   generated from `exec.go` by `waidriver w2g-cases` (`bin/wai w2g gen|check`), each case's statements printed with the
   instruction's fields made placeholders, which w2g replaces by constants (`uint16(N)` for the op, so a case shared by several ops
   folds to its own branch) or, for the instructions whose fields hold the process's canonical type ids (struct, array, ref, the
   casts, `call_indirect`), by the function's code read at run time; the SIMD instructions through the switch's default (their
   lane functions). So the AOT level runs the interpreter's semantics instruction for instruction, and an interpreter fix is w2g's
   by regeneration.

A tail call (`return_call*`) moves the callee's arguments to the frame's base and leaves the call to `Machine.run`'s trampoline
(`X_tailTo`): the callee runs in the same frame once the caller has returned, so a chain of tail calls grows neither Go's stack nor
the depth (the interpreter, tail-calling a translated function, leaves it to the trampoline too). A function with `try_table`'s
handlers sends every exception raised inside it (a callee's, a `throw`'s, a `throw_ref`'s) to its label `Lthrow`, which asks
`Machine.catch` at the raising instruction's pc, as the interpreter's `throw:` does, and jumps to the clause's target (a `switch`
over the handlers' pcs) with the clause's values pushed, or returns the exception to the caller. A function with an op neither the
cases nor w2g know is left to the interpreter (none in the testsuite's or the Go programs' modules). The
translated functions of a module are registered under its bytes' SHA-256 (`RegisterAOT`, in the generated file's `init`) and
attached at instantiation (`Func.Native`, run by `Machine.run` in place of `exec`). `Machine.Interpreted` counts the Wasm calls the
interpreter ran in an AOT program; a command (or a Go program's run) that passes but counted any is not an AOT pass: its row is
`fail "w2g: N calls on the interpreter (REASON)"`.

**The programs.** `waidriver w2g OUT.go [--stats FILE] MODULE...` writes the modules' Go as one file of package wai; it is copied
beside wai's printed sources in a copy of the light driver's module and built by TamaGo's go alone (`bin/lib/wai/aot.bash`'s
`aot_exe`, kept by the driver's sources' digest and the file's): an AOT program is the light driver itself (its verbs `wast`,
`control`, `run`) with the modules' functions compiled.

### 9.1 The testsuite at AOT (`bin/wai check --aot`)

The plans of §5 (the same planner, the same modules, the same plan lines), the scripts in batches (`WAI_AOT_BATCH`, 24 a program):
each batch's modules translated, its AOT program built and run (`waiaot wast`), the rows judged as §5.3 judges them, then the AOT
rule; the validator's rows are eval's (the same code) and are not repeated. Rows against `test/wasm/aot-results.edn`, classes
`test/wasm/divergences-aot.edn`, the controls `test/wasm/controls-aot.txt` (§9.4) run by the subject's batch's program; the
essential subset `test/wasm/essential-aot.txt` (`bin/wai aot-essential`).

### 9.2 The Go corpus at AOT (`bin/wai go --aot`)

The programs (`test/wasm/go-aot.txt`, or `--list`, or the whole corpus with `--vs-eval`) built as §8 builds them, run and judged as
§8 judges them, under the same limit (three times the action's), then the AOT rule (`WAI_AOT_STATS`: the run's interpreted calls).
Rows against `test/wasm/go-aot-results.edn` (`--vs-eval`: against eval's record, program for program).

**Translated by shape** (WAS40 (c), WAS50 (a); the default since WA8's branch). Go-built modules share most of their code (the
runtime, the libraries) but not its constants: a function's data addresses, function and global indices and its own resume
points differ from program to program, so the per-module translation (one AOT program a module, `WAI_AOT_SHARED=0`) shares
nothing. `W2GShape` translates a function with every immediate that is not a transfer of control's read from the function's own
code at run time (through an array pointer `ins := (*[N]Ins)(code.Ins)`, N the instructions' count, so no bounds check), so its Go
is a function of its shape alone: the parameters' and locals' counts, each instruction's op and stack height, the branches'
arities, heights and targets, br_table's entries, the handlers' clause kinds and targets. `ShapeKey` hashes exactly these (the
first 16 bytes of a SHA-256, `w2gShapeVersion` first); `waidriver w2g --shared` writes each shape once across all the modules it
is given (`--verify`: every function's Go written and two of one key required equal; else a shape met again is known by its key
alone) and registers them by key (`RegisterShapes`); at instantiation a module the per-module registry does not name takes, function
by function, the shape registry's (`ShapeKey` of each code). A run is two passes: the first builds every program and collects its
module (`gorun_collect`: `aot_go_exe` notes the module and stops); then, a slice of `WAI_AOT_SLICE` programs at a time (default 400:
the disk's measure; the whole corpus is 8,869 shapes, 364 MB of Go), the slice's modules are translated by shape into one AOT program
(`aot_go_shared`, kept by key in the main checkout's `.tmp/wai-go/aot-shared`, its build's cache and the previous slice's program
removed) and the slice's programs run on it (`WAI_GO_JOBS`, default 2). A function whose shape the program lacks runs on the
interpreter and fails the AOT rule, as before. Since WAS51 (a) `bin/wai go --aot` is the whole corpus (`--list FILE` a part, judged on
its programs' rows: `test/wasm/go-aot.txt`, WA7's 18).

**Large functions** (WAS54 (a)): a function of more than `w2gSegMax` (2,000) instructions is a dispatcher over segments, each a Go
function `NAME_sK(m, f, fp, pc0) (int, *Exn)` entered by a switch over its labels (its branch targets and its first instruction),
returning the next pc (a branch to another segment, the fall to the next) or -1 with the function's result; the frame and the stack
heights are the function's, so nothing else changes.

### 9.3 Cost

A Go-built module (about 2 MB of Wasm, a few thousand functions) is some tens of MB of Go; translated a module (WA7), about 150 s a
program. By shape (WAS50 (a)) the whole Go corpus, 1,188 programs, runs at AOT in about 37 minutes (three slices' programs of 187 to
239 MB built in 9 to 11 minutes each, the runs about 6 minutes; 2.05 GB at most); see the survey's "As built: WAS40 (c), WA8".

### 9.4 The controls

`controls-eval.txt`'s modes run by the AOT program, and `as-interpreted`: the command run by the same AOT program with its
translated functions not attached (`AOTDisable`), which passes on the interpreter and so must be refused by the AOT judge.

## 10. gojs, the headless world (`gojs.go`, `js.go`, `jsrt.go`, `jseval.go`, `gojsnode.go`, `gojspage.go`; WA11)

WAS13 (a), WAS16 (a), WAS17 (a): Go's `js/wasm` port run with no browser and no JavaScript engine. A Go program built for `GOOS=js`
imports module `gojs` (and syscall/js's tests `_gotest` too) and exports `run`, `resume`, `getsp` and its memory `mem`; the world
implements the imports over a model of JavaScript's values, as the pinned TamaGo go1.27.1's `lib/wasm/wasm_exec.js` (BSD-3; LICENSE.md)
implements them in a browser and `wasm_exec_node.js` under Node.js.

### 10.1 The imports

The 24 names of `wasm_exec.js`'s gojs object (and its `debug`), each a function of Go's stack pointer reading its arguments from Go's
stack at wasm_exec.js's offsets: `runtime.wasmExit`, `wasmWrite`, `resetMemoryDataView`, `nanotime1`, `walltime`,
`scheduleTimeoutEvent`, `clearTimeoutEvent`, `getRandomData`; `syscall/js.finalizeRef`, `stringVal`, `valueGet`, `valueSet`,
`valueDelete`, `valueIndex`, `valueSetIndex`, `valueCall`, `valueInvoke`, `valueNew`, `valueLength`, `valuePrepareString`,
`valueLoadString`, `valueInstanceOf`, `copyBytesToGo`, `copyBytesToJS`. The reference table is wasm_exec.js's: a value slot is a
float64, 0 `undefined`, a number itself, else a NaN-boxed reference (head `0x7FF8xxxx`, the type flag 1 object, 2 string, 3 symbol, 4
function) whose low word indexes `_values`; the ids 0-6 predefined (NaN, 0, null, true, false, the global, the Go object), the rest
counted by `_goRefCounts`, interned by SameValueZero (`_ids`) and pooled after `finalizeRef` (`_idPool`). After a call that may run
Go (a getter, a call, a constructor) the stack pointer is read again through `getsp`, as wasm_exec.js does.

**Re-entry.** A JavaScript function made by `js.FuncOf` is the Go object's `_makeFuncWrapper(id)`: called (by the world, by code the
world evaluates, by another import), it sets `_pendingEvent` and calls the export `resume` inside the running import, so Go runs
inside Go's own call; `_gotest.callExport` calls the exports `testExport0` and `testExport` the same way. The machine supports it
since WA11: a host function runs with the machine's base moved above its caller's operands (`Machine.HostCall`, used by the
interpreter and by w2g's calls), so the inner run's frames sit above the outer's. At the world's top an export runs as
`Machine.Call` (its traps and crashes stop the world); inside an import it runs on the running machine, its failures unwinding to the
top. `wasmExit` marks the world exited (Go's `exit` then unwinds by itself); an import called after it unwinds the machine as an
`*Exit`.

### 10.2 The values and the runtime (`js.go`, `jsrt.go`)

A value is `JSUndefined`, `JSNull`, a `bool`, a `float64`, a Go `string` (its length counted in UTF-16 units), a `*JSSymbol` or a
`*JSObject` (its own properties in insertion order, a prototype, and the slots of its class: an Array's elements, a typed array's bytes,
a wrapper's or a Date's value, a function's call and construct, frozen, an exotic getter and setter). Thrown values are panics of
`*JSThrow`, caught where JavaScript catches: wasm_exec.js's `try` around `valueCall`, `valueInvoke` and `valueNew` (the exception
stored with ok 0, so Go panics with `js.Error`), a promise's reaction, the evaluator's `try`. The realm (`NewRealm`) holds what Go's
packages and the programs use, after ECMA-262's algorithms for what is built (their names kept: `ToNumber`, `ToString`,
`ToPrimitive`, `[[Get]]`, `[[Set]]`, `OrdinaryHasInstance`, `===`, `==`): `Object` (`freeze`, `keys`, `assign`,
`getPrototypeOf`, `hasOwnProperty`, `toString`), `Function.prototype` (`call`, `apply`, `bind`), `Array` (`push`, `pop`, `shift`,
`concat`, `slice`, `indexOf`, `join`, `forEach`, `map`, `isArray`, `from`), `Uint8Array`, `Uint8ClampedArray`, `Float32Array`
(`subarray`, `slice`, `set`, `fill`), `Number`, `Boolean`, `String` (`indexOf`, `substring`, `split`, `charCodeAt`), `Symbol`, the
errors, `Date` (the world's clock, UTC: `getTimezoneOffset` 0, which `time`'s js zone reads), `Promise` (`then`, `catch`,
`resolve`, `reject`; reactions as microtasks), `Math`, `isNaN`, `parseInt`, `eval`, `TextEncoder`, `TextDecoder`, `console`,
`performance.now`, `crypto.getRandomValues`, `setTimeout`, `clearTimeout`, `queueMicrotask`. Numbers print by Number::toString.

### 10.3 `eval` (`jseval.go`, WAS43 (a))

A small subset of JavaScript, lexed, parsed and evaluated over the realm: literals (numbers, strings with escapes, objects with
methods' shorthand, arrays), function expressions and declarations, arrow functions, `this`, `var`/`let`/`const`, `if`, `while`,
`for(;;)`, `return`, `throw`, `try`/`catch`/`finally`, `break`, `continue`, the assignments (`=`, `+=`, ...), `?:`, `||`, `&&`, `??`,
equality, relations, arithmetic, bitwise, `typeof`, `void`, `delete`, `instanceof`, `in`, `new`, calls and member access. syscall/js's
tests evaluate an object literal and an immediately invoked function; the own programs use it for their JavaScript halves. A program
outside the subset is a SyntaxError that says so. It is not a JavaScript engine (WAS17 (b) would be its own crop).

### 10.4 Node's part (`gojsnode.go`)

`globalThis.fs`: Node's callback API as `syscall/fs_js.go` calls it, each callback called before the call returns (as wasm_exec.js's
own `fs` does): `open`, `close`, `read`, `write`, `writeSync`, `fstat`, `stat`, `lstat`, `readdir`, `mkdir`, `unlink`, `rmdir`,
`rename`, `truncate`, `ftruncate`, `fsync`, `chmod`, `fchmod`, `chown`, `fchown`, `lchown`, `utimes`, `readlink`, `symlink`, `link`;
`fs.constants` Linux's values (so the flags Go hands `open` are the host's). Paths: resolved against `process.cwd()`, cleaned, mapped
by the longest mount (`--dir GUEST=HOST[:ro]`); nothing exists outside a mount; a write under a read-only one is EROFS; errors are
Node's (`code` the errno's name, which `fs_js.go` maps back). Descriptors 0-2 the world's standard streams. `process`: `cwd`,
`chdir`, the ids -1 and `pid`, `ppid` -1, `getgroups` and `umask` ENOSYS (wasm_exec.js's own); `path.resolve`.

### 10.5 The page's part (`gojspage.go`)

doc/wasm/WEB.md §4: `window` (the global), `document` with one canvas (id `screen`, 800 × 600, `devicePixelRatio` 1), `navigator.gpu`,
`requestAnimationFrame`, `addEventListener`, and the bridge `globalThis.arbace` (`gpu.handle`, `gpu.release`, `gpu.exec`,
`input.ring`). Every GPU object is a recording fake (`fakeSpec`: each kind's methods and what they return; a promise for
`requestAdapter` and `requestDevice`); each call, and each property a program sets on a fake, is a line of the recording. The command
buffer is decoded by `gpuBridge.exec`, the shim's `exec` statement for statement, calling the fakes.

### 10.6 The event loop and the clock (WAS44 (a))

`waidriver gojs` writes argv (the module's path and its arguments, as go_js_wasm_exec gives them) and the environment (`--env`, sorted)
at 4096 as wasm_exec.js's `run` does, calls `run`, then runs the loop: the microtasks after each task; the earliest timer (Go's
`scheduleTimeoutEvent`, whose callback resumes Go, with wasm_exec.js's missed-event retry; `setTimeout`, at least 1 ms as Node's), the
clock moved to its time; in the page's mode (`--frames N`) an animation frame whenever one is asked and due before the next timer
(the input script's events of that frame delivered first), until N frames ran. Idle with nothing pending: in Node's mode the program is
resumed with event 0, as `wasm_exec_node.js`'s exit handler does, and Go reports its deadlock; in the page's mode the world ends. The
clock is virtual: the monotonic time starts at 0 and advances 1 µs a reading and to each timer's or frame's time when the world
waits, the wall clock from 2026-01-01T00:00:00Z (`GoJS.Epoch`); a run past a virtual day fails. So a run is a function of the
module, its arguments and its input, and its outputs judge it. The status: the program's (`wasmExit`'s), 0 when the frames are done,
126 a trap or a crash (with `wai: ...`).

### 10.7 The corpus (`bin/wai js`)

`bin/lib/wai/js-run.bash`: syscall/js's tests (`go test -c syscall/js` for js/wasm, run with `-test.v`), Go's own programs of
`test/wasm/js-run.txt` (`test/wasm/js/NAME/main.go`: `want.out`, `want.status`, `want.err`, `args`), the demo scene's frames
(doc/wasm/WEB.md §5) and the shim's checks, built by TamaGo's go (cached by key in the main checkout's `.tmp/wai-go/js`) and run by
the light driver; rows `[js/ID RESULT REASON]` against `test/wasm/js-results.edn`, classes `test/wasm/divergences-js.edn`, the negative
controls `test/wasm/controls-js.txt` (a program against another's statement, the scene one frame off and without its key press, the
shim without an import and with an argument count changed: each must fail; `self` checks must pass). Not a REAP cell (WAS17 (a)).
