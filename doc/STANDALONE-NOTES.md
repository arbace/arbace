# The standalone Arbace — notes

Working notes of the standalone stages ([STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), §9.1; the
user's decisions D1-D13 of 2026-10-12). Amendments to the survey are numbered SA1, SA2, ... and
are the user's to accept.

## S4: the Go-source seed

Status: preparation, 2026-10-12, amendments SA1-SA9 accepted by the user the same day, branch `s4-seed` (beside S2 and S3, which it does not depend
on). The JVM bootstrap stays as it is; nothing here removes it. The tooling, a candidate seed
printed by today's JVM chain, and the proofs below.

### What the seed is (D3)

Stage 0 of the bootstrap becomes **Arbace's Go program as a2g and g2a print it**: the module
`arbace` that `bin/arbace-go --build` hands to `go build` (`target/arbace-go/work-<arch>/mod`),
with the Go runtime's files that `overlay/go` patches, and nothing else Arbace-made. Built by
Go's toolchain alone, it gives the executable `bin/arbace-go --build` gives, byte for byte. That
executable then runs the chain (S3: a2g, g2a, `go build`) that builds stage 1 from the
checkout's sources, and so on to the fixpoint.

Contents of the archive, under one top directory `arbace-seed-go/`:

| path | what | size |
|---|---|---:|
| `SEED.edn` | what the seed is: its format, the version map of the commit it was printed from (linked as `main.arbaceVersion`), Go's version, the build's settings, what printed it (read with `read`) | 1 KB |
| `mod/go.mod` | `module arbace`, `go 1.27`; no requirements (no `go.sum`, no module cache) | |
| `mod/cmd/arbace/` | the main package: `main.go`, the image's types and loader, `rlwrap_*.go`, the embedded namespaces' sources `res/` (47 namespaces, a2g's compressed `.ae.z`, SZ2); `image.bin` empty (SA3) | 0.6 MB |
| `mod/lang/` | a2g's translation of Clojure's runtime (`arbace.lang`) | |
| `mod/jrt/` | jrt's hand-written Go and the JDK classes a2g translated from `jdk/forms`, with their data files (`tzdb.dat`, `uniName.dat`, ICU's `nfc.nrm`, ...) | |
| `mod/x509roots/` | Mozilla's roots (`x509roots/`, BX72) | |
| `overlay/runtime/` | `arbace_local.go`, `proc.go`, `runtime2.go`: Go's runtime as `overlay/go` patches it (`bin/jrt overlay`) | 0.4 MB |
| `namespaces.txt` | the embedded namespaces the image prepares (a2g's `prog/namespaces.txt`) | |

1,148 files, 29.2 MB unpacked (1,070 Go files, 28.4 MB). It is the same for every architecture:
the work modules of amd64 and arm64 are identical (checked, `diff -r`), so one seed builds both.
It holds no tests, no box (the box's guest is built by a later stage from `box/`, `tamago/` and
the same a2g output), and no host tools (`tools/elfsyms`, `tools/tlsserver` are plain Go in the
checkout).

### Format and pin (SA1, SA2)

`seed/arbace-seed-go.tar.xz` beside `seed/arbace-seed-go.tar.xz.sha256` (sha256sum's format, as
the jar's). The tar is deterministic: POSIX format, members sorted by name, mtime 0, owner and
group 0 (numeric), modes 0644 and 0755, no atime or ctime (GNU tar 1.35: `--sort=name
--format=posix --pax-option=exthdr.name=%d/PaxHeaders/%f,delete=atime,delete=ctime --mtime=@0
--owner=0 --group=0 --numeric-owner`); compressed with `xz -9e -T1` (one thread, so one block:
the output depends only on xz's version, 5.8.4 here). The pin is the archive's hash, what
`--go-check` checks in a second; `--go-verify` compares the *tar* byte for byte (then reports
whether the archive is identical too), so a different xz release does not make a valid seed look
wrong.

Sizes of today's candidate (main `b0f5730` plus the tooling, `7498921`):

| form | bytes |
|---|---:|
| the tree | 29,155,346 (tar 30,074,880) |
| tar.xz, `-9e` (chosen) | 3,063,796 |
| tar.xz, `-9` | 3,143,856 |
| tar.zst, `-19` | 3,235,094 |
| tar.gz, `-9` | 4,461,467 |
| `image.bin`, if carried (SA3), xz alone | +1,320,412 (6,308,340 raw) |

The survey's 35 MB and 4.4 MB counted the work module with its image.

### Stage 0 from the seed (SA3, SA4, SA5)

`bin/seed --go-build [-o DIR] [--arch amd64,arm64]` checks the hash, unpacks into `DIR/src`
(default `target/seed-go`), writes the overlay file for the toolchain's GOROOT, and builds as
`bin/arbace-go --build` builds:

1. `go build -trimpath -buildvcs=false -overlay overlay.json arbace/cmd/arbace` for the host,
   with the empty image (the environment of `bin/arbace-go`'s `go_env`: `GOTOOLCHAIN=local`,
   `CGO_ENABLED=0`, `GOOS=linux`, `GOAMD64=v1` / `GOARM64=v8.0`, `GOPROXY=off`, no `GOFLAGS`,
   `GOWORK`, `GOEXPERIMENT`);
2. that executable prepares the image (`ARBACE_PREPARE`, requiring `namespaces.txt`'s
   namespaces), as `bin/arbace-go`'s `prepare` does;
3. each architecture linked again with the image and `-ldflags "-s -w -X
   'main.arbaceVersion=<SEED.edn's :version>'"`, into `DIR/<arch>/arbace`.

About 20 s with Go's build cache warm; minutes cold (gc needs 7-8 GB for `jrt`, survey §8.2).

- **SA3 The image is not in the seed.** The image of prepared namespaces (6.3 MB, 1.3 MB as xz)
  is derived data, a function of the seed's own sources and its executable; carrying it would
  put 6.3 MB of opaque bytes in a seed meant to be readable source. Stage 0 prepares it, as every
  `--build` does; the result is the same image, byte for byte (checked). Alternative: carry it
  (one `go build` fewer, about 11 s; the archive 4.4 MB).
- **SA4 The toolchain**, pinned by its version and its sources. The toolchain is TamaGo's Go
  distribution (`/root/tamago-go`, https://github.com/usbarmory/tamago-go, go1.27.1,
  2026-08-28), the one every Arbace build uses (`G2A_GOROOT`). `seed/goroot.sha256` pins it as
  `<SHA-256>  go1.27.1`, and `SEED.edn` records the same as `:go` and `:goroot-sha256`. The hash
  of GOROOT's sources is defined as: sha256sum's lines (`<SHA-256>  <path relative to GOROOT>`)
  of `VERSION` and every file under `src/`, sorted by path in the C locale's byte order, hashed
  again; `src/` must hold only regular files and directories (a symbolic link or other file is
  refused); `pkg/`, `bin/`, `lib/`, `misc/`, `test/`, caches and the rest of GOROOT do not count
  (they are built or not compiled into Arbace). TamaGo's go1.27.1: 12,175 files,
  `5745170fd3c83cb77ec88bc50aee8606f0436e718c222b48376b5943ac0cebe5`. `--go-make` refuses to
  pack with another toolchain; `--go-build` and `--go-verify` refuse a GOROOT whose version or
  hash differs from the pin or from `SEED.edn` (a seed without `:goroot-sha256`, as the first
  candidate, is refused). The check takes 0.65 s with the files in the page cache, 3.6 s cold.
  The toolchain's binaries are not hashed: they are built from these sources, and Go's builds are
  reproducible.
- **SA5 Stage 0's version** is the version map of the commit the seed was printed from
  (`SEED.edn`'s `:version`, `git describe` there), so stage 0 *is* that commit's Arbace: its
  banner and `*arbace-version*` name that commit, not the checkout building it. A seed made from
  a dirty checkout records `:dirty true` (and `--go-make` warns); a seed to commit is made from a
  clean one.

### `bin/seed`'s modes (SA6)

Beside the jar's `--check` and `--verify`, unchanged (the gate still runs `--check`):

| mode | what | time |
|---|---|---:|
| `--go-check` | the archive (`ARBACE_SEED_GO`, default `seed/arbace-seed-go.tar.xz`) has its pinned hash | <1 s |
| `--go-make [ARCHIVE]` | pack this checkout's last `bin/arbace-go --build` (the host's work module, the patched runtime, `namespaces.txt`, the version map) and pin its hash in `ARCHIVE.sha256` | 17 s (xz) |
| `--go-build [-o DIR] [--arch A,B]` | stage 0, as above | 20 s warm |
| `--go-verify [REV]` | in a temporary worktree at REV (default `SEED.edn`'s `:commit`): the commit's own build of its Go program (today `bin/build-arbace`, `bin/arbace-go --build`; from S3 the Go-hosted chain started from the previous seed), its `--go-make`; then compares the tar byte for byte and the archive, and builds stage 0 from the printed seed and compares it with the commit's own `--build` executable | ~10 min |

The code is `bin/lib/seed-go.bash` (sourced by `bin/seed`). At S4's end (SA6): the jar and its
modes leave main, the `--go-*` modes become `--check`, `--build`, `--verify` (and `--make`),
`bin/gate`'s first step checks the Go seed, and `bin/build-arbace` gives way to S3's chain started
from `bin/seed --build`.

### When the seed is committed (SA7)

The seed to commit is the **fixpoint's**: printed by the Go-hosted chain (S3, G2 = G3), at the
commit that is tagged `arbace-seed-is-go-v1`. Today's candidate is printed by the JVM chain; if
S3 holds its exit criterion (the Go-hosted chain prints what the JVM chain prints), the two are
the same tree, but that is S3's proof, not this one's. So the candidate stays in `.tmp/` and is
not committed: 3.1 MB per seed in git's history is cheap, but a seed that is replaced within days
is churn. Recommended: commit the seed once, at S4's end, with `--go-verify` run on it; re-seed
rarely (as the jar was pinned once, at a freeze), for instance when the toolchain moves.
Alternative (rejected): the tree committed as files (29 MB, readable and diffable on GitHub, but
every re-seed a diff of up to 29 MB, and 1,070 Go files of a module nobody edits inside the
repository; `jdk/` and `tamago/` are committed as forms because they are *sources*, which the
printed Go is not: its sources are the forms).

### Proofs (2026-10-12)

On the worktree `s4-seed` at `7498921` (main `b0f5730` plus the tooling), a clean checkout,
`bin/build-arbace` then `ARBACE_GO_ARCHES=amd64,arm64 bin/arbace-go --build`:

- **The module is architecture-neutral**: `work-amd64/mod` and `work-arm64/mod` identical
  (`diff -rq`).
- **Generation A**: `bin/seed --go-make` → 3,063,796 bytes, SHA-256
  `c4e00bf6c127568784aa307d41ddb2a565680f4fec5e1a02e474a6b8869b99a3`.
- **Stage 0 = `--build`, both architectures**: `bin/seed --go-build --arch amd64,arm64` from A
  (in a fresh directory) gives `amd64/arbace` (69,169,312 bytes, SHA-256
  `0793e34f…5c036e8a`) and `arm64/arbace` (66,781,344 bytes, `9e7a7cee…0fc837d`), each
  byte-identical to `bin/arbace-go --build`'s; the image it prepares is identical to the
  build's. `*arbace-version*`: `{:describe "arbace-box-arm64-v1-28-g7498921", :commit
  "7498921…", :dirty false}`.
- **Generation B**: `bin/seed --go-verify` (a fresh worktree at `7498921`: its own bootstrap,
  a2g, g2a, gc, `--go-make`; 4m50s): the same tar byte for byte (SHA-256
  `cc61aabbf4fcf5e050d58edb7f1ee735b2ff91eb0d7464dce720056ff294aca0`) and the same archive
  (`c4e00bf6…`); stage 0 built from B (amd64) is that worktree's own `--build` executable, byte
  for byte (`0793e34f…`). The seed is reproducible from its commit.
- An earlier generation from the same tree with a dirty checkout differed only in `SEED.edn`'s
  version map, and its stage 0 equalled that build's executables too (the version linked is the
  only input outside the tree).
- **Again with SA4's toolchain pin**, after main was merged (`3d3a13b`, clean; a fresh
  `ARBACE_GO_ARCHES=amd64,arm64 bin/arbace-go --build`): the candidate 3,063,232 bytes, SHA-256
  `311420be5f7fb60d195550d033f17b60b593dc94738caba8b389fd88b4aeb2a2` (its `SEED.edn` with
  `:goroot-sha256`), `--go-check` as pinned; stage 0 from it identical to the build's
  executables, amd64 `27b438c5…fe892ad`, arm64 `bfb2136b…c3f59a7`, built in 18 s; the oracle with
  main's reference on that executable: the 46 known mismatches, as recorded (with `files.edn`
  re-recorded). The first candidate, without `:goroot-sha256`, is now refused by `--go-build`;
  a GOROOT with one file of `src/runtime` changed is refused (checked).

### The amendments

Accepted by the user 2026-10-12: SA1-SA3 and SA5-SA9 as proposed, SA4 with the hash of GOROOT's
sources (as above, rather than the version alone).

- **SA1 The seed's form**: one deterministic `tar.xz` committed as `seed/arbace-seed-go.tar.xz`
  with `.sha256` beside it (3.1 MB), not the printed tree committed as files (29 MB) nor a gzip
  (4.5 MB).
- **SA2 Its determinism and pin**: the tar's settings above, `xz -9e -T1`; the pin is the
  archive's SHA-256; `--go-verify` compares the tar, so only the tree must reproduce.
- **SA3** The image of prepared namespaces not carried; stage 0 prepares it.
- **SA4** The toolchain pinned by version (TamaGo's go1.27.1) and by the SHA-256 of GOROOT's
  `VERSION` and `src/` (`seed/goroot.sha256`, `SEED.edn`'s `:goroot-sha256`), checked by
  `--go-make`, `--go-build` and `--go-verify`.
- **SA5** Stage 0 carries the version of the commit the seed was printed from.
- **SA6** `bin/seed`'s `--go-check`, `--go-make`, `--go-build`, `--go-verify` now, beside the
  jar's modes; renamed `--check`, `--make`, `--build`, `--verify` when the jar leaves (S4's end).
- **SA7** The seed committed once, at S4's end, printed by the Go-hosted fixpoint and verified;
  today's JVM-printed candidate stays in `.tmp/`.
- **SA8** The oracle's reference is the jar of `arbace-for-java-26-v4`, built from its tag and
  pinned by hash (`test/oracle/frozen-jar.sha256`), not `seed/arbace-seed.jar` (v1).
- **SA9** At S4 the expected files are recorded again from v4, and the Go build's known
  mismatches against them are `test/oracle/known-go-amd64-frozen.edn` (the 46 of
  `known-go-amd64.edn` plus main's two divergences from v4); `known-go-amd64.edn` stays the
  gate's until then, unchanged.

## S4: the oracle's reference (D4)

`bin/oracle` ran its runner, and `record` and the `jvm` implementation, on `bin/arbace-j`, that
is `target/arbace.jar` from the bootstrap, which leaves main at S4. It now takes
`ARBACE_ORACLE_REF`:

- `main` (the default until S4): as before.
- `frozen`: the frozen JVM Arbace, run by `java` alone (no `bin/build-arbace`, no
  `bin/arbace-j`): the runner (`java -cp JAR:.tmp/oracle-view:test arbace.lang.Main -m
  oracle.runner`; the frozen jar's loader reads `.clj` only, so `oracle/runner.clj` and
  `harvest.clj` are links to the `.ae` files), and the reference implementation for `record` and
  `check jvm` (`java -XX:+UseCompactObjectHeaders -XX:-OmitStackTraceInFastThrow -cp JAR
  arbace.lang.Main -`, through the runner's new `ARBACE_ORACLE_JVM`).
- `ARBACE_ORACLE_EXPECTED` names the expected files' directory (default
  `test/oracle/expected`), so a record from another reference can go elsewhere.

**SA8 The reference is the frozen branch's last release, v4, not the seed jar (v1).** The seed
jar is `arbace-for-java-26-v1`, which still compares NaN wrongly (`(< ##NaN 1)` true, `(long
##NaN)` throwing: the class forms compiler's `dcmpg`, fixed in v2, `398f01b`); recorded from it,
`forms/numbers` alone differs from the expected files in 8 cases. `bin/oracle` builds the jar of
`ARBACE_ORACLE_TAG` (default `arbace-for-java-26-v4`, `c4095d1`) once from the tag, with the
tag's own `bin/build-arbace` in a temporary worktree (about 2 minutes), into
`.tmp/oracle-frozen/arbace-for-java-26-v4.jar`, and checks it against
`test/oracle/frozen-jar.sha256` (pinned:
`4b6f031990ba61edc73030d17b827ad2e2fca7aa2c53c3ed3e178ae1c89d640a`; two builds identical).
`ARBACE_ORACLE_JAR` takes another jar unchecked (`seed/arbace-seed.jar` for v1). Alternatives:
commit the v4 jar (7.5 MB) as the reference, which spares the tag's build but is one more binary
in git; or keep `seed/arbace-seed.jar` and accept v1's bugs as recorded divergences (rejected:
they are bugs, not divergences).

### The expected files recorded from v4

`ARBACE_ORACLE_REF=frozen ARBACE_ORACLE_EXPECTED=.tmp/oracle-exp-frozen bin/oracle record`: every
file recorded (22,118 cases), 5 of 84 files differ from the committed expected files, 6 cases:

| case | committed (main's JVM) | v4 | what |
|---|---|---|---|
| `forms/state_errors.ae:542` | "Could not locate ...`__init.class`, `.ae`, `.clj` or `.cljc`" | without `.ae` | **divergence**: main's loaders take `.ae` (2026-10-11), the frozen jar does not |
| `forms/polymorphism.ae:176` | "... `java.io.Serializable` and `java.lang.Comparable` ..." | the two interfaces in the other order | **divergence**: the order of the ambiguous methods in the message (main's multimethod, or the JDK's interface order through the jar's build) |
| `forms/files.ae:123, 349-353` | the forms' text with `.ae` | with `.clj` | **stale expected file**: the `.ae` rename (`c76b468`) edited `expected/forms/files.edn` but not `forms/files.ae`; main's JVM records the same as v4 (checked). Not a divergence: re-recording on main fixes it |
| `forms/strings.ae:408` | `(hash (StringBuilder. "x"))` 1318227903 | 1293885429 | **noise**: an identity hash, different on every run (main's own record differs too) |
| `forms/concurrency.ae:201` | `CyclicBarrier`'s arrival indexes `[2 (0 1)]` | `[0 (1 2)]` | **noise**: thread timing |

### The native oracle with the v4 reference

The native executable (`target/arbace-go/amd64/arbace` at `7498921`) checked with the runner
and the expected files from v4 (`ARBACE_ORACLE_REF=frozen
ARBACE_ORACLE_EXPECTED=.tmp/oracle-exp-frozen bin/oracle check ... --expected
test/oracle/known-go-amd64.edn`): 22,229 of 22,278 cases, 49 mismatches: the 46 known and 3
new. The same executable with main's reference: 22,232, the 46 known, as recorded.

- `forms/concurrency.ae:201`: a noisy record (v4 recorded `[0 (1 2)]` once; three records
  again gave `[2 (0 1)]`, as the committed file and the Go build). Not a divergence; the expected
  file kept the stable value.
- `forms/polymorphism.ae:176`: main's MultiFn names two ambiguous classes in the order of their
  names (`ac8b971`, 2026-10-09, after the freeze); v4 in the JDK's order. A divergence of main.
- `forms/state_errors.ae:542`: main's loaders take `.ae` first (2026-10-11), so the not-found
  message names `.ae`. A divergence of main (D13).

The existing `test/oracle/known-go-amd64.edn` is unchanged. The two divergences go into a
separate reference, `test/oracle/known-go-amd64-frozen.edn` (48 cases: the 46 plus a group for
the two, with their reasons), for the check against v4's expected files (SA9); with it the check
passes (22,230 of 22,278 match, the 48 as recorded, the frozen jar matching its pin). The expected files from v4 are not committed (they are 5 of 84 files
different, of which `files.edn`'s was a stale file: re-recorded from main's JVM reference on
this branch, the diff only its six `.ae`-rename cases); recording them into
`test/oracle/expected` is S4's step, with `known-go-amd64-frozen.edn` becoming the gate's.

## S4: inventory for the JVM's removal

What S4 removes from main or moves off the JVM, with what replaces each (D4: the JVM stays only
as the oracle's reference, the frozen jar, and as j2a's front end). Nothing is removed yet.
Tags: **[P]** the JVM product, leaves main (kept on `arbace-for-java-26`); **[T]** a tool that
runs on the JVM today and is hosted by the Go executable after S3; **[R]** stays on a JVM (the
oracle's reference or j2a). Counts at `b0f5730`.

### Scripts

- [ ] [P] `bin/build-arbace` (229 lines: the seed view, stages 1-3, AOT, `arbace.classes.verify`,
  `bin/native-tests`, the jar, the AOT cache, `--suite`, `--image`) → S3's chain from `bin/seed
  --build` (stage 0 from the Go seed, then G1 → G2 → G3).
- [ ] [P] `bin/arbace-j` (105) → the Go executable (`bin/arbace-go`); the oracle runs the frozen
  jar with `java` (done: `ARBACE_ORACLE_REF=frozen`).
- [ ] [P] `bin/arbace-image` (70, jlink and its AOT cache) → removed.
- [ ] [P] `bin/seed`'s jar modes → its `--go-*` modes renamed (SA6).
- [ ] [P] `bin/native-tests` (24, `test/native` on stage 2) → removed; its semantic tests
  (`defclass`, `deftype`, `fn`, `overload`, `nan`, `bridge`, `reduce`, `imports`,
  `nested_deftype`) into the oracle's corpus first.
- [ ] [P] `bin/class-forms-tests` (24, `test/classes` against javac's bytecode) → removed, or
  reduced to the front end's tests on Go.
- [ ] [R/P] `bin/class-forms-check` (13: converted forms compiled to bytecode and compared with
  javac's) → used by `bin/j2a-check` (steps 2-3) and `bin/jrt-convert check`: see open point O2.
- [ ] [P] `bin/java-surface` (32, ASM over stage 2) → retired; its output
  `doc/go/java-surface.edn` stays as data.
- [ ] [T] `bin/lib/tools.bash`'s `arbace_runtime` (`target/stage2`) → the Go executable
  (`ARBACE_PATH` to the checkout); `frozen_clojure` stays (git only).
- [ ] [T] `bin/a2g`, `bin/a2g-check`, `bin/a2g-race`, `bin/a2g-evalproof`; `bin/g2a` (read,
  convert, build's driver), `bin/g2a-print`, `-print-tests`, `-convert-tests`, `-build-tests`;
  `bin/c2a`, `bin/c2a-check`; `bin/box-vendor`, `bin/tamago-vendor`, `bin/x509roots-vendor` (g2a);
  `bin/jrt` build/test/print (g2a) → on the Go executable (S3).
- [ ] [T/R] `bin/clojure-tests` (369): c2a's translation and the listing/reports on stage 2 → on
  Go; its stage modes [P] removed; `--java-fixtures` (j2a) [R] or the fixtures committed as forms
  (the `test-fixtures-forms` branch).
- [ ] [R] `bin/oracle` → the frozen jar (done, SA8); `ARBACE_ORACLE_REF=frozen` becomes the
  default and `main` goes.
- [ ] [R] `bin/jrt testdata`, `manifest`, `standins`; `bin/jrt-coverage`; `bin/net-check`'s JVM
  side → on the frozen jar (data generators: their outputs are committed or regenerated rarely).
- [ ] [R] `bin/j2a`, `bin/j2a-check`, `bin/jrt-convert` → stay on the JVM (javac), see O1, O2.
- [ ] `bin/arbace-go`: `build` (a2g, g2a on the JVM) [T] → S3's chain; `jvm_store_checks` and the
  JVM's `tls_checks` in `--smoke` [P] → removed; `gate_key` hashes `seed/arbace-seed.jar.sha256`,
  `bin/arbace-j`, `bin/build-arbace`, `java -version` → the Go seed's pin, the chain's scripts, Go
  only (plus `java` while a2g runs on it).

### Code

- [ ] [P] ASM: `arbace/asm.ae` and `arbace/asm/` (45 files, 14,747 lines) → removed, after
  `Opcodes`' access-flag constants (used by `classes/{parse,analyze,compiler}`,
  `lang/Intrinsics`, `a2g/{checks,decls,dyn,fromfn,model,out,reach}`) move into a constants
  namespace of Arbace's own (O3).
- [ ] [P] `arbace/lang/Compiler.ae`'s bytecode back end (8,717 lines; 105 `emit*` methods,
  `ObjExpr.compile`) → the analyzer stays; the emitters removed (the Go variant
  `lang/go/Compiler.ae` already erases them).
- [ ] [P] `arbace/lang/Compile.ae` (72, AOT's main) → removed.
- [ ] [P] `arbace/lang/DynamicClassLoader.ae` (78) and its uses (`RT.ae`, `main.ae`,
  `core/server.ae`, `repl/deps.ae`) → the Go variants (`lang/go/RT.ae`, `ns/main.subst.ae`,
  `ns/core/server.subst.ae`) become the sources.
- [ ] [P] `arbace/genclass.ae` (760, gen-class, gen-interface by ASM) → removed (D13); the Go
  variant `lang/go/ns/genclass.ae` (gen-interface at run time) becomes the one.
- [ ] [P] `arbace/core_proxy.ae` (403) → `lang/go/ns/core_proxy.ae`.
- [ ] [P] `arbace/core.ae`: `descriptor`, `add-annotation` (ASM `Type`), `ns`'s `:gen-class`,
  `compile`/`*compile-files*`, `(load "genclass")`; `core/specs/alpha.ae`'s `::ns-gen-class` →
  removed or no-ops, each a recorded divergence (D13).
- [ ] [P] `arbace/reflect/java.ae` (267, ClassReader) → `lang/go/ns/reflect/java.subst.ae`.
- [ ] [P] `arbace/core_classes.ae`'s `write-module!`, `write-package!` → removed.
- [ ] [P] the class forms compiler's back end: `classes/emit.ae` (2,187; first move a2g's
  `ctor-real-desc`, `this0-name`, used in 6 a2g files, O4), `compiler.ae` (169), `shape.ae` (161),
  `verify.ae` (102), `build.ae` (74), `boot.ae` (31) → removed; the front end (`parse`, `lower`,
  `analyze`, `env`, `types`, `native`) stays.
- [ ] [T] ASM inside the front end: `analyze.ae`'s accessor bytecode (`add-accessor!`,
  `accessorize`, ...), `env.ae`'s ClassReader (`read-fields`, annotations), a2g's `access.ae` → S2
  (data instead of bytecode).
- [ ] [P] the JVM namespaces a2g already leaves out (`repl/deps`, `java/basis`, `tools/deps`,
  `java/browse*`, `java/javadoc`, `inspector`, `xml`, `instant_timestamp`, `java/api`) → removed
  or ported, per D13.
- [ ] [P] `arbace/box/host.ae` (794, the store on the JVM, BX37), `arbace/tls/host.ae` (98,
  JSSE) → removed (the Go variants under `lang/go/ns/` stay).
- [ ] [R] `arbace/j2a/` → stays; `j2a/build.ae` (bytecode, j2a-check's step 4) see O2.

### Tests and data

- [ ] [P] `test/aot-training.ae`; `test/arbace-results.edn` (the suite on stages 1, 2) → removed;
  `test/arbace-go-results.edn` becomes the gate's.
- [ ] [R] `test/baseline-results.edn` → stays while `j2a-check --suite` runs on the JVM.
- [ ] [P] `test/native/` (19 files) → removed after the semantic ones move to the oracle.
- [ ] [P] `test/classes/` (17 files and fixtures) → removed, except `check_converted.ae` (O2).
- [ ] [P] `test/classforms/jvm_check.ae` → removed; `gen_corpus.ae`, `class_scripts.ae` [R] on
  the frozen jar.
- [ ] [R] `test/oracle/` (runner, harvest, expected) → on the frozen jar (done); the expected
  files re-recorded from v4 at S4.
- [ ] [R] `test/jrt/testdata*.ae`, `tables.ae`, `manifest.ae`, `standins*.ae`,
  `jrt_coverage.ae`, `jrt_sources.ae` → on the frozen jar; `java_surface.ae` [P] retired.
- [ ] [T] `test/a2g/eval_ops.ae` (generates `lang/go/CompilerOps.ae` on stage 2),
  `test/a2g/check.ae` → on Go.
- [ ] `test/c2a/c2a_test.ae` names `arbace.asm.commons.Method` as data → check after ASM goes.

### `bin/gate`

- [ ] `bin/seed --check` → the Go seed's hash.
- [ ] `bin/build-arbace` [P] → `bin/seed --build` and S3's chain (the fixpoint, printed Go
  compared).
- [ ] `suite-stage2` [P] → Clojure's suite on the Go test build (`bin/arbace-go --build-tests`,
  `CLOJURE_TESTS_GO`, `test/arbace-go-results.edn`): the gate's suite (survey S4's exit).
- [ ] `cft` (`bin/class-forms-tests`) [P] → removed or the front end's tests on Go.
- [ ] `go` (`bin/arbace-go --gate`) → keyed without the JVM; no JVM checks in the smoke test.
- [ ] `--full`: `suite-stage1` [P] removed; `j2a` (`j2a-check --suite`) and `jrt-convert verify`
  [R] stay on the JVM (O1, O2); `oracle-go` → the frozen reference; g2a, box, go-build,
  go-build-tests, suite-go, box-forms, box-smoke [T] → on the Go-hosted tools; the converters
  no longer wait for `suite-stage1`.

### Documents

- [ ] CLAUDE.md (Layout: the seed, `bin/build-arbace`, the stages, the jar, the AOT cache,
  `bin/arbace-j`, `bin/arbace-image`; the gate; "the tools run on `target/stage2`"), README.md
  (building and running), LICENSE.md (ASM's entries and section, the seed jar's entry: kept while
  any ASM-derived file or the frozen jar is in the tree).
- [ ] doc/VENDOR-NOTES.md (the bootstrap, the jar and AOT cache, the runtime image, the suite on
  stages 1 and 2, the binary seed: marked as the frozen branch's), doc/classes/{SPEC (§6, §8.3,
  §9.3, §9.4, §9.6), COMPILER-NOTES, CONVERTER-NOTES}.md, doc/ARBACE.md, doc/C2A.md,
  doc/go/{A2G-SPEC, EVAL-NOTES, CLASSFORMS-REPL, JAVA-SURFACE, BOX-NOTES (BX37, TLS on the JVM),
  EVAL-PLAN, EXEC-NOTES, ORACLE, A2G-NOTES, B1-PLAN, JRT-NOTES, JRT-SOURCES, SPEED-NOTES, BUILD,
  ...}.md; doc/AGENDA.md; a JOURNAL entry.

### Open points for S4

- **O1 Which JVM Arbace hosts j2a after S4.** j2a's Arbace side runs on `target/stage2` today.
  Options: (a) the frozen v4 jar with `arbace.j2a` loaded from the checkout's source (it reads
  `.clj` only: a view as the oracle's); (b) j2a's Arbace side on the Go executable, javac alone in
  a JVM (a process boundary inside j2a). Recommended: (a) at S4, (b) with D12's later stage.
- **O2 j2a's checks compile to bytecode.** `j2a-check` (steps 2-4) and `jrt-convert check`
  compile converted forms with the class forms back end (emit, ASM's ClassReader) and compare
  with javac. Options: (a) run them on the frozen jar's own compiler (at the freeze's state);
  (b) make them forms-level only (j2a's output compared with the committed forms, as `jrt-convert
  verify` does); (c) keep emit, shape and ASM on main as a j2a-check toolkit. Recommended: (b),
  with (a) for the suite step.
- **O3** `Opcodes`' constants need a home before ASM goes. **O4** a2g's two helpers from emit
  move first.
- **O5** The Go suite's Java fixtures come from j2a; committed as forms (the `test-fixtures-forms`
  branch), the gate's suite needs no JVM.
