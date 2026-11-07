# The crops' oracles as records: a gate with no host toolchain

Survey, 2026-11-05 (the day's second round, task 1; branch `oracle-records` from `next`). Reading and design only: nothing
here was built, and no gate or corpus run was made. The inventory comes from the scripts on `next`, the crops' branches
(`haskell-he5`, `haskell-native`, `rust-re5`, `rust-native`, `ocaml-o8`, `ocaml-native`, `ocaml-errors2`, `scheme-sm9`,
`cpp-cx8`, `cpp-cx9`, `cpp-cx10`, `cpp-native2`, `llvm-verify3`, the Mac's `mac/201-python-py7-py11`), GT9's step drafts
(`gt9-build`'s `test/gate/crops/*.edn` and `steps.edn`'s `:host-ids`) and `du`/`xz` over the caches the crops' worktrees left.
Proposed decisions ORS1-ORS14 are at the end; none is decided.

## Executive summary

- **The goal.** No step of the essential gate, the nightly `--full` or the clean run of any crop needs a toolchain from
  Alpine at run time: GHC, rustc (with rustc-dev and rustdoc), clang/LLVM, g++, ocamlc/ocamlopt, Chez, Chibi, python3 or
  gcc. The oracles stay the reference. What the gate reads becomes committed records or pinned packs, and the live oracle
  verifies them wherever it is installed. Java's `test/java/ref-jdk26.edn` and C's `test/c/ref-gcc15.edn` (CS14 (a)) are the
  model.
- **The toolchains are used in five ways (§2), and only one of them cannot be recorded.**
  - **A. Outcomes** of the reference: runs, acceptance, diagnostics, token digests. They are small, and most are already
    committed.
  - **B. Dumps of fixed inputs** that Arbace's readers are compared with: trees, tokens, types, symtables. They are
    medium-sized and can be recorded.
  - **C. Front-end products that Arbace's evaluators consume**: OCaml's Lambda, GHC's STG, rustc's and clang's LLVM IR, and
    the helper dumps the repr readers convert. They are the largest, can be recorded, and are required. Without them `oi`,
    `hsi`, `rsi`, `cppi` and the reads cannot run at all.
  - **D. The toolchain run on Arbace's own output**: the printed source compiled or accepted, objects or `.cmi` compared, the
    lowered C compiled, Arbace's IR assembled, std built from the printed library, Chez's expander rebuilding psyntax. This
    output changes with every change to Arbace, so it cannot be recorded in advance.
  - **E. Installed trees read as data**: gcc's include directory, musl's `/usr/include`, libstdc++'s headers. These can be
    packed.
- **The design (§3).**
  - Outcomes (A) go in committed records, one map a line.
  - Dumps and products (B, C) go in deterministic tar.xz packs, one per crop, kind and corpus. Each pack is pinned by a
    committed manifest (input digest → member digest) and a `.sha256` line. The packs live in a shared store outside the
    clone (`ARBACE_ORACLE_PACKS`), as the corpora already do. The essential subsets' dumps are committed, so a fresh clone runs
    the essential gate with nothing external but its pinned corpora.
  - Class D leaves the three gates in one of two ways. Where a native part is sound it replaces the oracle: the native
    readers read the print, and ci runs the lowered C. Otherwise the check moves to a new oracle tier (`:phase :oracle`),
    which runs where the toolchain is installed, verifies the records and is required once per tag.
  - Recording is by hand only, never by a gate. Each record names the toolchain's identity (GT9's `:host-ids` digest).
  - A judging step loses the toolchain host from its `:hosts`. Alpine edge's upgrades then stop rekeying and breaking the
    gate, and become a re-record decision.
- **Sizes (§4).**
  - Committed records today: test/c about 1 MB, test/cpp plus test/repr/cpp about 2.4 MB, test/rust 2.7 MB, test/haskell
    0.2 MB, test/ocaml 0.6 MB, test/scheme 1.2 MB.
  - Packs, estimated from measured xz ratios of 4-28x: about 180-200 MB xz for every crop and corpus.
  - The essential subsets' committed dumps: about 10-20 MB raw in all.
- **Cost (§6).** About 27 agent-days in total: about 3 for the shared tooling, then by crop, in the recommended order: C 3,
  Scheme 1, Haskell 4, Rust 4.5, OCaml 4, Python 2.5 (the Mac's), C++ and LLVM 5. ocaml-typer, cpp-codegen and HES21 would
  make class C unnecessary crop by crop. The packs bridge until they do.

## 1. Today

### 1.1 How the gate knows a toolchain

GT9 (`gt9-build`) names each crop's toolchain as a host in `steps.edn`'s `:host-ids`: `:gcc`, `:cxx` (g++ and clang++),
`:llvm` (llvm-as, opt, lli and FileCheck under `/usr/lib/llvm23/bin`), `:ocaml`, `:chez`, `:chibi`, `:ghc`, `:rustc` and
`:python`. A host's identity is the SHA-256 of a recipe over the tool: its version lines, its executables' bytes, compiler-libs,
boot files, `librustc_driver`, `libpython`, and the headers lists. `bin/gate`'s `gate_host_id` answers `none` when a `:require`
command is missing.

Each step on such a host has the identity in its key. Alpine edge's upgrade of gcc, LLVM or GHC therefore reruns every step on
that host, and a host without the package cannot run them. C's `:gcc` has been in the essential gate since C8: `c-dg-essential`,
`c-check-essential`, `c-aot-essential` and `c-c23`.

Java is the model, though it is not from Alpine: jdk26u is built from source.
- The conformance check reads `test/java/ref-jdk26.edn`, which holds per test the JDK's status, stdout digest and length,
  stderr's first line and whether two runs agreed. It needs no JVM.
- `bin/java-check --record-ref [--only REGEX]` rewrites the record, or only the matching lines. In that case it keeps every
  other line byte for byte and checks that they are unchanged.
- C did the same for gcc's runs, `test/c/ref-gcc15.edn` (2,528 rows, `bin/ci record-ref`), and C++ for g++'s
  (`test/cpp/ref-gxx15.edn` and `ref-dg-gxx15.edn`).

### 1.2 Inventory, step by step

Key to the columns:
- **Live**: the tool runs at check time.
- **Class**: as in the summary, A outcome, B dump of a fixed input, C front-end product consumed, D toolchain on Arbace's
  output, E installed tree read as data.
- **Recorded**: what is committed today.

Step names are GT9's (`steps.edn` on `next` for C; `test/gate/crops/*.edn` for the others).

**C** (`:gcc`; ccdump is go-whim's cc in Go, built by TamaGo's go, not Alpine's)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| c-dg-essential | none: `--essential` exits before `gcc1` (bin/lib/ci/dg:70-74) | E: the headers in test/c/host/search.txt | divergences-dg.edn | yes, but the headers |
| c-check-essential, c-aot-essential, c-check, c-aot | none; musl's one-time `CC=gcc ./configure` (bin/lib/ci/musl:45) | A; E | ref-gcc15.edn (392 KB) | yes, but the headers and musl's configure |
| c-c23 | `gcc -std=c23 -fsyntax-only` on each original and each print, `gcc -c` of control pairs, then `cmp` (bin/lib/repr-c/c23check:21-32) | A for the originals; D for the prints and objects | parse-gcc.tsv (originals' acceptance) | no: D |
| c-dg (full) | `gcc -S`/`-E` per file with its dg-options (dg:78-91) | A: gcc's diagnostics | Arbace's verdicts only (check-dg-linux-amd64.edn) | no: gcc's diagnostics not recorded |
| c-tokens | `gcc -E -P` only under `--record`/`--judge-all` | A: pp-gcc.sha256 (456 KB) | yes | yes |
| c-parse, c-annotations | ccdump (Go); gcc's acceptance from parse-gcc.tsv | B (cc's, not Alpine's) | parse-gcc.tsv (149 KB) | yes |
| c-roundtrip | `gcc -c` of the original and the print, `cmp`, link and run both (bin/lib/repr-c/roundtrip:130-140) | A for the originals; D for the prints | none (CS77 proposed it) | no: D |
| c-judge-test | none (synthetic records) | | | yes |

**C++ and LLVM IR** (`:cxx`, `:gcc`, `:llvm`)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| cpp-runtime (build) | `clang`/`clang++ -O0 -S -emit-llvm` over 52 libsupc++/libstdc++ units (bin/cppi:170-172), `clang -c`, `gcc -c` of the lowered C, `ar` | C: IR (digests pinned in test/cpp/runtime/ir.sha256, 17.6 KB); D: lowered C under gcc | IR digests | no: C and D |
| cpp-read-essential, cpp-tokens | g++ `-E` and clang `-dump-raw-tokens` only with `--record` | A | pp-gxx.sha256, lex-clang.sha256, test/cpp/host/ | yes, but the headers (E) |
| cpp-print-essential, cpp-roundtrip | cxxdump (clang `-ast-dump=json`) on the original and on the print; level 4: `g++ -c` and `clang++ -c` of both, `cmp` | B/C: the originals' AST; D: the print's AST and objects | roundtrip-oracle.edn, roundtrip-results/ | no: C and D |
| cpp-lower-essential, cpp-lower | `clang++ -emit-llvm` (bin/cppi:123), clang link and run, a g++ build and run (the reference), `gcc -O0` of the lowered C | C: IR; A: g++'s runs (not recorded); D: lowered C under gcc | lower-results.edn | no |
| cpp-check-essential, cpp-check, cpp-aot | `clang++ -emit-llvm` live; the run on ci/cf2g | C: IR; A: g++'s runs | ref-gxx15.edn (55 KB) | no: C |
| cpp-dg | cxxdump per file; g++ only with `--record-ref` | A: ref-dg-gxx15.edn (579 KB); B: clang's verdicts | g++'s side | no: B live |
| cpp-native(-essential) | cxxdump (cached in .tmp/repr-cpp/cxxdump-cache) | B | none | no: B live |
| cpp-selftest | clang, g++, gcc, cxxdump, opt and llvm-as; tests needing the tools skip with exit 3 under `/bin/true` | the checks' own tests | | partly |
| llvm-roundtrip | `clang -c` of the original and the print, `llvm-as` of both, `cmp` (llvm-verify3's bin/repr-llvm:86-92) | A for the originals; D for the print | none | no: D |

**Haskell** (`:ghc`; the helper tools/hsdump is built by the host's GHC)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| haskell-read-essential | hsdump dump/tree on test/haskell's cases; `ghc -c` and `--show-iface` for the iface cases; native-check's hsdump per module | C (the dumps repr/haskell converts); B (native-check's reference); D (the print's tree and interface) | nothing persists (native-check re-dumps 3.6 MB) | no |
| hsi-essential, hsi-check | `hsdump stg -O0` per program (haskell-he5's bin/lib/hsi/check:131); the libraries' STG (.tmp/hsi/dumps, 151 MB, 1,415 files) | C: STG; A: ref-ghc914.edn (267 KB, 907 ids) | the runs; STG cached per program by key | no: C (cacheable) |
| haskell-roundtrip | hsdump per module; `ghc --make` of each package from the originals and from the print; `--show-iface` | C for the originals; D for the print | known files only | no: C and D |

**Rust** (`:rustc`; tools/rsdump is a `rustc_private` driver against the rustc-dev apk, 589 MB unpacked)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| rust-read-essential (`test`) | rsdump dump/tree, rustdoc JSON, rustc builds (run-program) | C; A (the spec cases' runs); D | none | no |
| rsi-essential, rsi-check, rsi-aot | `rustc --emit=llvm-ir,link` per program (rust-re5's bin/lib/rsi/plan.bash:22); `cc` for rust_test_helpers.c; gcc in the runtime's build | C: IR (std's 51 MB pinned by test/rust/runtime/ir.sha256); A: ref-rustc-1.99.1.edn (40 KB) | the runs; std's IR digests | no: C (per-program IR is `rm -rf`'d each run) |
| rust-roundtrip, rust-iface | rsdump on the originals and on the print; rustdoc JSON of both | C for the originals; D for the print | known files | no |
| rust-behaviour (`--run`, `--std`, `--code`) | rustc builds and runs of the originals and the print; `cargo -Zbuild-std` of std from the printed library; rsdump `--hir --mir` and objects | A for the originals; D for the print | known files | no: D |
| rust-native(-essential) | rsdump per file of test/rust/native-check.txt | B | none (33 MB re-dumped each run) | no: B (dumps carry an absolute `:abs`) |

**OCaml** (`:ocaml`; tools/ocdump on compiler-libs, built by ocamlopt)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| oi-essential | `bin/oi stdlib`: `ocamlc -c` over the stdlib and ocdump `--mode lambda` per unit; ocdump per REPL phrase (repl-check); ocdump's Lambda per program (oi_one) | C: Lambda and the stdlib's .cmi; A: ref-ocaml55.edn (84 KB), session.ref | the runs | no: C |
| oi-divergences, ocaml-judges | none | | records | yes |
| ocaml-read-essential, ocaml-roundtrip | `ocamlc -c` of the original and of the `:full` print, `cmp` of the .cmi (bin/repr-ocaml:429-455); ocdump `--loc` | C for the originals; D for the print's .cmi | floors.tsv, roundtrip-known.edn | no |
| ocaml-dumps | `ocamlc -c` plus ocdump `--mode types|lambda` per corpus, each twice (the determinism test) | B (a test of the helper itself) | dumps-known.edn | no: it checks the dumper |
| oi-check, oi-aot, oi-suite-aot | as oi-essential over about 2,400 programs; the suite's units `ocamlc -c` (suite.bash's stages), the expect tool `ocamlc -linkall` | C; A: ref-suite.edn, ref-expect.edn | the runs | no: C |
| ocaml-native(-essential) | `menhir --check`: none (the tarball's parser.ml); native-check: ocdump `--loc`, `--mode tokens` | B | none (76 MB scratch) | no: B |

**Scheme** (`:chez`, `:chibi`)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| si-essential, si-aot-essential, si-aot, si-ecraven, si-mats, scheme-roundtrip | none | A: ref-chez.edn, ref-r7rs-*.edn, ref-ecraven-*.edn | yes | yes |
| scheme-read-essential, scheme-read | `chez --script tools/schemedump/dump.ss` over the corpora, the probes and 7 scans of every scalar value | B | verdicts only (2.8 MB of dumps in scratch) | no: B |
| si-check | `si boot --check`: Chez expands psyntax (`tools/psyntax/boot.ss`) and the result is `cmp`'d with scheme/psyntax/psyntax.pp.ae | D: a check of the vendored product's provenance | the product itself | no: D |
| si-corpora | `chibi-tests`: chibi-scheme per library (3 files) | A (not recorded) | ref-r7rs-chibi.edn for r7rs | no: chibi-tests |

**Python** (`:python`, CPython 3.14.10 pinned by tools/pydump/PYTHON_VERSION; the Mac's branch)

| Step | Live host calls | Class | Recorded today | Toolchain-free? |
|---|---|---|---|---|
| python-front-essential, python-front | `pydump.py tree`, `scopes` (CPython's `_symtable`), `tree --full`; `corpus.py` (gen --check) | B: trees, symtables, compile errors | known files (classes) only | no: B |
| pyi-aot-essential | none (`--no-pyi`); but bin/pyi aborts at :420 without python3 | A: ref-cpython314.edn (25 KB) | yes | yes once bin/pyi stops requiring python3 |
| pyi-check-aot | `runtime check`'s smoke runs against `python3 -I` and `python3 -q -i`; `record-ref --check` reruns CPython twice | A (smoke not recorded); `record-ref --check` is itself a verification | ref-cpython314*.edn (1.1 MB lib-test) | no: smoke and verify |
| pyi-bench | python3 per pyperformance benchmark | A: timings | bench-aot-linux-arm64.edn | no |

### 1.3 How often each changes

- The **inputs** of classes A, B and C are pinned corpora: tarballs, apks and commits. They change only with a pin change, a
  few times a month per crop. When a test program is added, it changes for that program alone.
- The **toolchains** move with Alpine edge, every few weeks for GHC, LLVM and gcc. Their identities are already in the keys.
- The **helpers** change with their crop's work: hsdump, rsdump, ocdump and cxxdump changed weekly during HR, RR, O and CX.
  Each change of a helper is a re-record of its kind.
- **Class D's inputs** change with every change to Arbace's printers, lowerings and evaluators, several times a day.

## 2. The five uses, and what each can become

1. **A, outcomes.** These are committed records, one map a line, sorted by id, with a header naming the command, the
   toolchain's identity and the date. Java, C, C++ (check and dg), Haskell's hsi, Rust's rsi, OCaml's oi and Scheme's si
   already have them. The gaps are:
   - gcc's dg diagnostics;
   - g++'s runs of cpp-lower's programs;
   - CPython's smoke runs;
   - Chibi's chibi-tests;
   - the originals' half of every round trip (objects, interfaces, runs), which CS77 (a) proposed for C.
2. **B, dumps of fixed inputs.** They are deterministic once their paths are normalized (Rust's `:abs`; Haskell's already
   uses `$libdir`). A pass needs only the digest. A diff needs the body, which the pack holds.
3. **C, front-end products.** Arbace evaluates the toolchain's output: oi runs Lambda, hsi STG, rsi and cppi LLVM IR, and the
   readers convert the helpers' dumps. The bodies are required. These are the packs that matter most.
   - They depend only on the pinned input, the helper and the toolchain, never on Arbace.
   - The native front ends remove them crop by crop: ocaml-typer (`oi` without ocamlc), cpp-codegen (C++ without clang),
     HES21 (Haskell to STG), and for the reads the native readers (haskell-native, rust-native, ocaml-native, cpp-native).
4. **D, the toolchain on Arbace's output.** This cannot be recorded in advance. Four ways out:
   - (i) **A native substitute, where it is sound.** Once native-check proves the native reader equal to the helper on the
     originals, the print's tree level can be read natively and compared with the original's recorded tree. ci can run the
     lowered C that gcc compiles today (`cppi check` already runs it on ci). si's fixpoint already rebuilds psyntax without
     Chez.
   - (ii) **A memo keyed by the output's digest**: the printed file's SHA-256 plus the toolchain's identity, mapped to the
     verdict and object digest. It is committed, and a miss needs the toolchain. It pays when a printer change touches few
     files, and fails the day the printer changes everything.
   - (iii) **The oracle tier (§3.6).**
   - (iv) Keep the step as an exception.
5. **E, installed trees.** C's preprocessor reads gcc's include directory and musl's headers at the paths in
   test/c/host/search.txt, whose digests are in test/c/host/headers.sha256 (24 KB). C++ reads libstdc++'s
   (test/cpp/host/headers.sha256, 99 KB). Two ways:
   - pack them, pinned by those very lists;
   - or keep them as the host's, which means Alpine's gcc-dev, musl-dev and libstdc++-dev at run time.

   The headers are data, not a toolchain, but the goal excludes the package all the same.

## 3. The design

### 3.1 Records (class A): committed

- **Place and format.** `test/CROP/ref-TOOLVERSION.edn` and the like, as today: one map a line, sorted by `:id`.
- **Header.** `;; Made by: COMMAND`, `;; host: :ghc IDENTITY` (the `:host-ids` digest, with the version line) and
  `;; date:`.
- **Re-recording a part.** `--record-ref --only REGEX` keeps every other line byte for byte and checks it, as JCS68 (a)
  does.
- **New records:**
  - c-dg's `test/c/ref-dg-gcc15.edn`: gcc's filtered diagnostic lines per file, as C++'s ref-dg-gxx15.edn;
  - cpp-lower's g++ runs, folded into ref-gxx15.edn;
  - test/python/ref-smoke-cpython314.edn;
  - test/scheme/si/ref-chibi-tests.edn;
  - the round trips' originals' sides: object digests, interface digests and runs per file, CS77 (a) generalized to every
    crop as `test/CROP/roundtrip-orig-TOOL.edn`.

### 3.2 Packs (classes B, C, E): pinned, outside the clone

- **Unit.** One pack per crop, kind and corpus: `haskell-stg-libs`, `haskell-dump-ghc-9.14.1`, `rust-ir-ui`,
  `ocaml-lambda-stdlib`, `cpp-ast-gxx-dg`, `c-headers`, and so on.
- **Format.** A deterministic tar.xz, made with the seed's recipe: sorted, mtime 0, owner 0, `xz -9`.
- **Manifest.** Each pack has a committed manifest, `test/CROP/records/KIND-CORPUS.tsv`. Each line holds:
  - the input's id;
  - the entry key: the SHA-256 of the input's bytes, the helper's source digest, the toolchain's identity and the flags;
  - the member's SHA-256 and size.

  The pack's own `.sha256` line sits beside it. A gate can therefore tell a stale entry (an input or helper changed since the
  record) without unpacking anything.
- **Store.** `ARBACE_ORACLE_PACKS`, by default the main checkout's `.tmp/oracle-packs/`, shared by the worktrees as
  `.tmp/gate-exe` is. The clean run's clone reads it in place, as it reads the corpora in place.
- **Unpacked bodies.** These go to `.tmp/oracle-packs/unpacked/PACKSHA/`, and the tools read them where they read their
  caches today: `.tmp/hsi/dumps`, `.tmp/rsi/std/ir`, `.tmp/oi/lib`, the cxxdump cache.
- **Distribution.** The mirror is ORS3.
- **Essential subsets.** Their dumps and products are committed in the tree, under `test/CROP/records/essential/`, as plain
  text, about 10-20 MB in all. A fresh clone with only the pinned corpora runs the essential gate. This follows the model of
  Java's committed `test/java/forms/`.
- **Tool behaviour.** Each helper call site (`hsdump`, `rsdump`, `ocdump`, `cxxdump`, `pydump`, `dump.ss`, `clang -emit-llvm`,
  `rustc --emit=llvm-ir`) goes through one function, `oracle_get KIND KEY CMD...`, in a new `bin/lib/oracle.bash`. It
  answers from the committed essential records or the unpacked pack.
  - On a miss with the toolchain present, it runs CMD and marks the entry *unrecorded*, which fails a gate step (§3.5).
  - On a miss without the toolchain, it fails with "no record for ID and no TOOL to make it".

  Outside the gate, by hand, a miss with the toolchain present is simply computed. Day work keeps its speed.

### 3.3 Who records, when

- Records and packs are written **by hand only**: `--record`, `record-ref` or `bin/oracle-records make CROP KIND CORPUS`.
  This is done on the amd64 host whose toolchain has the identity the records name, in a commit of its own whose message
  names that identity.
- A gate never records, as CS240 (a) rules for C's full runs. A gate writing its own references would judge Arbace against
  itself.
- A nightly run with the toolchain present verifies (§3.4). It never rewrites anything.
- The Mac's VM records Python's records for arm64, and amd64's on the host. Dumps and outcomes are architecture-independent
  where the oracle is; a record says which architecture made it.

### 3.4 Freshness

The oracle tier's verify steps, one per crop and kind (`CROP-records-verify`), run the live oracle over the entries and
compare byte for byte.
- **Same toolchain identity, an entry differs.** The step fails: the record is stale, or the helper is non-deterministic.
  The latter is a bug of its own; OCaml's `ocaml-dumps` already tests it with two runs.
- **Different identity** (Alpine upgraded the package). The step reports "toolchain moved: records of IDENTITY-A, host
  IDENTITY-B" with the list of differing entries, and does not fail. The user decides on a re-record day (ORS6).
- **Sampling.**
  - Nightly, when the toolchain is present: a rotating sample, as GT8's rotation does, over a universe that is the manifest.
  - At the oracle run for a tag: every entry.

### 3.5 Keys and pins

- A judging step's `:inputs` gain its records and manifests.
- Its `:pins` gain the packs' `.sha256` files. `bin/gate --pins`, the clean run's first step, checks that each pinned pack is
  in the store with its hash, as it checks the corpora's tarballs.
- Its `:hosts` lose `:ghc`, `:rustc`, `:cxx`, `:gcc`, `:llvm`, `:ocaml`, `:chez`, `:chibi` and `:python`. Those hosts stay
  only on the oracle tier's steps.
- **Statuses.**
  - An *unrecorded* entry (an input in the corpus whose key the manifest lacks) fails the step, as "no reference" fails C's
    and Java's checks today. The commit that adds a program carries its record.
  - A *stale* entry (the manifest's key differs from the input's) fails as well.

### 3.6 The oracle tier

- **What runs in it.** A new phase, `:phase :oracle`, run by `bin/gate --oracle`. It holds the verify steps (§3.4) and the
  class-D checks that have no native substitute (§5).
- **Where it runs.** Wherever the hosts' identities are not `none`. Elsewhere its steps are *absent*: the plan says so, and
  no step fails.
- **Passes and record.** Its passes go into the shared store as any pass does. Its record at a tag is
  `test/gate/oracle-TAG.edn`: each step with the host identities it ran on.
- **Relation to the three gates.**
  - The essential gate, the nightly `--full` and the clean run never include it.
  - A tag requires its oracle record (ORS1).
  - The nightly runs it beside `--full` when the host has the packages and the budget allows (ORS8).

### 3.7 Negative controls

Two controls, following GTS81 (a):
- **Verify steps.** The control is a crossed pair: one entry's live output judged against another entry's record. It must
  differ, or the step fails.
- **Records' readers.** A tampered pack member, one byte flipped, must fail `oracle_get`'s digest check. The control pack
  lives under `test/oracle-records/controls/`.

The judging steps keep their families' controls unchanged. Those controls already judge a case's run against another
case's reference, and the reference is now a record.

## 4. Sizes and budget

Measured: raw sizes by `du`, and xz ratios on sample files with `xz -c FILE | wc -c`. The pack column is an estimate, raw
size divided by the measured ratio.

| Crop | Pack contents | Raw | xz ratio measured | Pack (est.) |
|---|---|---|---|---|
| Haskell | corpus dumps (haskell-native's .tmp/repr-haskell/dumps); libraries' STG; programs' STG | 106 MB; 151 MB; 42 MB | 8x (134 KB → 17 KB); 4.6x (46 KB → 10 KB) | 13 + 33 + 9 = 55 MB |
| Rust | std's IR; programs' IR (165 + the UI sample); native-check dumps; round trip's originals; rustdoc JSON | 51 MB; about 300 MB; 33 MB; 42 MB; 4.5 MB | 28x (1.8 MB → 64 KB); 7.6x (60 KB → 7.9 KB) | 2 + 11 + 4.5 + 6 + 1 = 25 MB |
| OCaml | stdlib Lambda; programs' Lambda (about 2,400); types and Lambda dumps; parse and token dumps | 5.4 MB; not measured; 119 MB; 76 MB | 13x (Lambda); 4x (types); 6x (parse) | 1 + 15 (guess) + 15 + 13 = 45 MB |
| C++ and LLVM | cxxdump AST (cpp-cx9's cache, 55K entries); the runtime's IR; programs' IR; libstdc++'s headers | 199 MB; 52 MB; small; about 20 MB | 10x; 11x | 20 + 5 + 1 + 3 = 30 MB |
| C | gcc's include directory and musl's headers; musl's configured tree; dg diagnostics (627 files, a record) | about 10 MB; small | | 2 MB |
| Scheme | Chez's dumps and scans | 3.3 MB | 7.5x | under 1 MB, committed |
| Python | trees, symtables and compile errors of the stdlib, site-packages and lib-test | about 300 MB (guess) | 16x | 20 MB |

- **Total.** About 180-200 MB of packs. The committed growth is about 1-3 MB of records per crop, plus the essential
  subsets' dumps at 10-20 MB.
- **Proposed budget (ORS11).** At most 3 MB of committed records per crop, at most 100 MB of packs per crop and 500 MB in
  all. A pack over its budget is subsetted through the rotation's universe.
- **What it replaces.** Today's per-worktree caches: 0.5-0.7 GB per worktree for Rust's apks alone, 704 MB for hsi, 1.5 GB
  for C's token dumps (ccdump's, which stay live: Go, not Alpine). Each worktree re-dumps these. The packs are shared.

## 5. Each step's disposition

The code letters used below:
- **R**: judged from committed records.
- **P**: records plus packs.
- **N**: a native substitute replaces the oracle on Arbace's output.
- **O**: the step, or its part, moves to the oracle tier.

| Crop | Essential | Nightly and clean |
|---|---|---|
| C | c-dg-essential R+P(headers); c-check-, c-aot-essential R+P(headers, musl's configured tree); c-c23: originals R (parse-gcc.tsv), the print's syntax and the object pairs O; c-c2j R | c-check, c-aot R+P; c-dg R (ref-dg-gcc15.edn); c-tokens, c-parse, c-annotations R (ccdump is Go); c-roundtrip: originals R (CS77 (a)), the print's objects and runs O |
| C++ | cpp-read-essential R+P(headers); cpp-print-essential: originals P, the print's tree N (CX11's native reader, once native-check is clean), level 4 O; cpp-lower-essential: IR P, g++'s runs R, the lowered C on ci N (gcc O); cpp-check-essential P+R; cpp-native-essential P | cpp-runtime: IR P, gcc's objects of the lowered C O (`runtime-ci` is the N half); cpp-tokens R; cpp-dg R+P; cpp-roundtrip as print-essential; cpp-lower as lower-essential; cpp-check, cpp-aot P+R; cpp-native P; cpp-selftest: tool-free tests in, the tool tests O |
| LLVM | | llvm-roundtrip: originals' objects and bitcode R (digests), the print O; its input list from the C++ runtime's IR pack |
| Haskell | haskell-read-essential: `test`'s dumps P (essential, committed), its iface cases O, `tables --check` R, native-check P; hsi-essential P+R | haskell-roundtrip: originals P, the print's tree N (the native reader), `--iface` O, `--text` P; hsi-check P+R |
| Rust | rust-read-essential: originals' dumps and rustdoc P, spec runs R, the print's parse N, the print's builds O; rsi-essential P(IR)+R; rust-native-essential P | rust-roundtrip: originals P, the print N; rust-iface: originals P, the print O; rust-behaviour: originals R, the print O (`--std` O whole); rsi-check, rsi-aot P+R; rust-native P |
| OCaml | oi-essential: Lambda P (stdlib, programs, the REPL session's phrases keyed per phrase), runs R; oi-divergences R; ocaml-read-essential: originals' .cmi digests R and dumps P, the print's .cmi O, `--spec`/spec-check P; ocaml-native-essential P | ocaml-roundtrip as read-essential; ocaml-dumps O (it tests the dumper); oi-check, oi-aot, oi-suite-aot P+R (the suite's units' .cmi and Lambda in the pack); ocaml-judges R; ocaml-native P |
| Scheme | si-essential, si-aot-essential R; scheme-read-essential P (committed: under 1 MB) | scheme-read P; scheme-roundtrip R; si-check: all R but `boot --check`, which is O; si-corpora R (chibi-tests recorded); si-ecraven, si-mats R |
| Python | python-front-essential P (committed); pyi-aot-essential R (bin/pyi's python3 requirement made lazy) | python-front P; pyi-check-aot: smoke R, `record-ref --check` O; pyi-bench: CPython's timings R (ref-costs), compared with tolerance, or O |

### 5.1 What can never avoid a toolchain

These checks put the oracle's judgement on Arbace's own output, and no record made before the output exists can stand in:
- the round trips' compiled levels: C's and C++'s objects of the print; Haskell's `--iface` of the print; Rust's
  `--iface`, `--run`, `--code` of the print and `--std`; OCaml's `.cmi` of the print; LLVM's objects and bitcode of the
  print;
- c-c23's print syntax and object pairs;
- gcc compiling the C++ lowering's output in cpp-runtime;
- `si boot --check`, Chez's expander reproducing the vendored psyntax;
- `ocaml-dumps`' determinism test and the verify steps themselves;
- cpp-selftest's tool tests;
- CPython's speed in pyi-bench.

They go to the oracle tier. What the three gates lose is a per-night guarantee that the toolchain still accepts Arbace's
latest print with the same objects. The forms-level and native-level checks keep a per-merge guarantee that the print reads
back to the same forms. The tag requires the rest.

### 5.2 What is lost, in general

- **Adding a test program takes a recording step.** The commit that adds the program carries its record, so a host without
  the toolchain cannot add programs.
- **A toolchain upgrade is now explicit.** It is a re-record day. Today it is an implicit rekey, which reruns everything
  and may fail.
- **Helper changes are more visible.** A change to hsdump or rsdump means a re-record of its packs in the same commit, and a
  pack is a 25-55 MB artifact to rebuild.
- **The store is a new external.** Losing it, with no mirror, means a full re-record on a host with every toolchain: a few
  hours a crop.
- **Bisecting against a moved toolchain.** Old commits name old packs. The store keeps every pinned pack (ORS3); pruning
  keeps the packs a tag names.

## 6. Migration, crop by crop

Estimates are agent-days on the host, sample checks included. Each step is merged with its records in the same commit.

0. **Shared tooling: 3 days.**
   - `bin/lib/oracle.bash` (`oracle_get`, the manifest's keys).
   - `bin/oracle-records` with the verbs `make`, `verify [--sample N]`, `pins`, `fetch` and `push` (the mirror), and
     `arbace.records` for the manifests.
   - `bin/gate --oracle` and the planner's `:phase :oracle`, with absent steps where `gate_host_id` is `none`.
   - `--pins` over the packs' `.sha256`; the controls.

   This needs a planner change, which rekeys every step: put it with the runner's next change, as GTS243 does.
1. **C: 3 days.**
   - `ref-dg-gcc15.edn` and c-dg's judge reading it (0.5).
   - The headers pack and musl's configured tree from `bin/ci musl prepare`, read where search.txt says (1).
   - CS77 (a): the originals' objects and runs, with c-roundtrip's and c-c23's print halves moved to the oracle tier (1).
   - `:gcc` dropped from nine steps (0.5).
2. **Scheme: 1 day.** The Chez dumps committed with a verify verb (0.5); chibi-tests recorded and `boot --check` moved to
   the oracle tier (0.5).
3. **Haskell: 4 days.**
   - The STG packs: the libraries and the programs keyed as `.tmp/hsi/progs` already are (1).
   - The corpus dumps' pack, with native-check against it (1).
   - `test` split into its recordable cases and its iface cases (0.5).
   - The round trip's print read natively (1.5).
4. **Rust: 4.5 days.**
   - rsi's IR keyed and packed; `ir1`'s `rm -rf` replaced by `oracle_get` (1).
   - `:abs` normalized, then native-check against a pack (0.5).
   - The originals' dumps and rustdoc packed; the print read natively (1.5).
   - The behaviour levels' originals recorded, their print halves moved to the oracle tier (1).
   - `test` split (0.5).
5. **OCaml: 4 days.**
   - The Lambda packs: the stdlib, the programs, the suite's units, the REPL's phrases (1.5).
   - The parse and token dumps' pack for native-check and `--spec` (1).
   - The originals' .cmi digests recorded, the print side and `ocaml-dumps` moved to the oracle tier (1).
   - The expect tool's outputs recorded (0.5).

   ocaml-typer (T1-T8) makes the Lambda pack a check instead of a necessity.
6. **Python (the Mac's VM): 2.5 days.**
   - pydump's dumps: the essential ones committed, the corpora packed (1.5).
   - Smoke references recorded (0.5).
   - bin/pyi's python3 check made lazy, and `record-ref --check` and the bench moved to the oracle tier (0.5).
7. **C++ and LLVM: 5 days.**
   - The IR packs: the runtime's 52 units and the programs (1).
   - cxxdump's packs: the corpora's lists, with the essential ones committed (1).
   - g++'s runs for the lowering recorded (0.5).
   - The lowering's gcc compile replaced by the ci run, with gcc's compile moved to the oracle tier (1).
   - The libstdc++ headers pack (0.5).
   - The print's level 3 read by CX11's native reader, level 4 moved to the oracle tier, and llvm-roundtrip's originals'
     digests recorded (1).

   cpp-codegen makes the IR packs a check instead of a necessity.

**The order.** C and Scheme first: they are nearly done, and they take `:gcc` out of the essential gate. Then the eval crops'
class C packs, Haskell and Rust, the largest benefit since their worktrees each re-dump hundreds of MB. Then OCaml, beside
ocaml-typer's progress. Then Python, on the Mac's queue. C++ goes last, as cpp-codegen will shrink it. After each crop: its
steps' `:hosts` without the toolchain, one `bin/gate --plan` on a host without the package (an Alpine container or `PATH`
stripped) showing every essential and nightly step runnable, and the oracle tier's verify run once with every entry.

## Proposed (2026-11-05): ORS1-ORS14 (none decided)

- **ORS1 The scope.** Where the toolchain stays.
  - Options:
    - (a) None of the three gates needs a toolchain. A new oracle tier (§3.6) holds the verify steps and the class-D checks,
      runs where the toolchains are installed, and its record is required at each tag (`test/gate/oracle-TAG.edn`).
    - (b) The essential and nightly gates are toolchain-free, and the clean run requires the toolchains and runs the oracle
      tier. The tag keeps one record, but the clean run is no longer runnable on any host.
    - (c) Records only where they exist today, with live toolchains in the rest. This is the status quo.
  - *Recommended: (a)*: it is the brief's goal, and the tag still certifies freshness on the host that has the toolchains.
- **ORS2 Where the records live.**
  - Options:
    - (a) Outcomes committed. Dumps and products in pinned packs outside the clone, except the essential subsets', which are
      committed.
    - (b) Everything committed: about 180-200 MB xz of churn in git, and every helper change rewrites tens of MB.
    - (c) Everything in packs: the essential gate then needs the store too.
  - *Recommended: (a)*.
- **ORS3 The packs' store and its mirror.**
  - Options:
    - (a) `ARBACE_ORACLE_PACKS` (default the main checkout's `.tmp/oracle-packs/`), mirrored to the Mac's bare repository as
      an orphan branch `oracle-packs`, one commit per pack, pushed only with `git push mac`, never to origin.
    - (b) The host's store alone: losing it means re-recording.
    - (c) Packs fetched from upstream builds: none exist for these dumps.
  - *Recommended: (a)*, with pruning that keeps every pack a tag's manifests name.
- **ORS4 Keys.**
  - Options:
    - (a) Records and manifests in `:inputs`, packs' `.sha256` in `:pins`, toolchain hosts only on oracle steps, and each
      manifest entry keyed by the input's bytes, the helper's digest, the toolchain's identity and the flags.
    - (b) The packs' identities in `:hosts` as new hosts.
  - *Recommended: (a)*: it fits GT1's model, since a pin is an external checked by `--pins`.
- **ORS5 Who records.**
  - Options:
    - (a) By hand only, on the host with the named toolchain identity, in a commit of its own. Gates verify, never record.
    - (b) The night records missing entries automatically.
  - *Recommended: (a)* (CS240 (a)'s rule): a gate that writes its own references judges Arbace against itself.
- **ORS6 A moved toolchain.**
  - Options:
    - (a) The verify step reports "toolchain moved" with the differing entries and does not fail. A re-record day follows
      by the user's decision, and records name their identity.
    - (b) It fails until re-recorded.
    - (c) The records are re-recorded automatically on a move.
  - *Recommended: (a)*: Alpine edge's upgrades should not break nights. The tag's oracle record shows which identity
    verified what.
- **ORS7 Negative controls.**
  - Options:
    - (a) A crossed pair per verify family (GTS81 (a)'s rule), and a tampered pack member that `oracle_get` must refuse.
    - (b) The verify steps waived, as judgements of records.
  - *Recommended: (a)*.
- **ORS8 Class D.**
  - Options:
    - (a) A native substitute where it is sound: the native readers reading the print, once their native-check is clean on
      the corpus; ci running the lowered C. The rest moves to the oracle tier, run nightly beside `--full` when the host has
      the packages and the budget allows, and required at a tag.
    - (b) A committed memo keyed by the output's digest and the toolchain's identity.
    - (c) Every D check to the oracle tier, with no substitutes.
  - *Recommended: (a)*. (b) fails the day a printer change touches every file, which is most printer changes.
- **ORS9 Installed headers and trees** (gcc's include directory, musl's headers, libstdc++'s, musl's configured tree).
  - Options:
    - (a) Packed, pinned by the existing headers lists (test/c/host/headers.sha256, test/cpp/host/headers.sha256), and read
      from the unpacked pack.
    - (b) Kept as the host's (Alpine's gcc, musl-dev, libstdc++-dev at run time).
  - *Recommended: (a)*: C's essential steps then lose `:gcc` entirely.
- **ORS10 A program with no record.**
  - Options:
    - (a) It is *unrecorded*, and fails the step. The commit that adds it carries its record.
    - (b) A class *unrecorded*, counted and skipped.
  - *Recommended: (a)*, as Java's and C's "no reference" do today.
- **ORS11 The size budget.**
  - Options:
    - (a) At most 3 MB of committed records per crop, essential dumps included in a separate allowance of 20 MB in all; at
      most 100 MB of packs per crop and 500 MB in all; a pack over budget is subsetted through its rotation universe.
    - (b) No budget.
  - *Recommended: (a)*.
- **ORS12 The order.**
  - Options:
    - (a) Tooling, then C, Scheme, Haskell, Rust, OCaml, Python (the Mac's) and C++ with LLVM, each crop's packs bridging
      until its native front end (ocaml-typer, HES21, cpp-codegen) makes them checks.
    - (b) Wait for the native front ends and record only class A and B.
  - *Recommended: (a)*: the eval crops cannot wait months for their front ends, and the packs are what the front ends will
    be checked against anyway.
- **ORS13 Java's remaining JVM in the essential gate.** `oracle-go`'s runner (test/oracle/runner.ae) runs on the frozen JVM,
  which feeds the implementation. jdk26u is built from source, not from Alpine, so it is outside the brief, but it is the
  same pattern.
  - Options:
    - (a) Port the runner to stage 0, so the essential gate needs no JVM; the expected results are already recorded.
    - (b) Leave it.
  - *Recommended: (a)*, after the crops, as its own item.
- **ORS14 Tools fail on a missing toolchain only when a miss needs it.**
  - Options:
    - (a) Every crop's script checks for its toolchain lazily, at the first miss (bin/pyi:420 aborts without python3 even
      for `check --aot --no-pyi`; bin/repr-haskell and bin/repr-rust probe ghc and rustc at start).
    - (b) As built.
  - *Recommended: (a)*: it is a precondition of every crop's migration.

## Decided (2026-11-05, the user)

ORS1-ORS14, each (a) as recommended: no gate tier needs a host toolchain, the oracle tier required per tag; outcomes committed, packs outside the clone, the essential dumps committed; the store .tmp/oracle-packs mirrored to the Mac's bare repository as an orphan branch; records in :inputs, packs in the pins, toolchain hosts on oracle steps only; records by hand only; a moved toolchain reported, not failed; crossed-pair and tampered-pack controls; class D to native substitutes where sound, else the oracle tier; headers packed; a program without a record fails; 100 MB a crop, 500 MB in all; the migration after the tag in the order C, Scheme, Haskell, Rust, OCaml, Python, C++ and LLVM; oracle-go's runner off the frozen JVM later; tools look for a toolchain only when a missing record needs one.

## As built: the tooling and C (write-ahead, 2026-11-05)

The migration's first two parts in ORS12's order, on branch `oracle-records-c` (from `oracle-records`), light: every record and
verify below was made on a few small corpora's files; the whole recording is the night's (its commands below). The commits, in
the order they land:

| Commit | What | Rekeys |
|---|---|---|
| `c3892e875` | the shared tooling | nothing (no step reads it yet) |
| `c60cb208c` | C's installed headers and musl's configured tree as packs; tools/ccdump reads the headers' pack | C's steps |
| `9f53d55a7` | gcc's dg diagnostics as a record | C's steps |
| `81d57166c` | the round trip's and C23's originals as records (CS77 (a), CS79 (a)) | C's steps |
| `4aef699f9` | `bin/repr-c check` asks gcc's include directory only when gcc runs | C's steps |
| `aefac28af` | `steps.edn`: C's steps judge from the records, `:gcc` off their `:hosts`; the oracle tier's three steps | C's steps, `gate-tests`, `reap` |
| `3a711db67` | `bin/gate --pins`: no gcc is no failure | nothing (bin/gate's pins are in no key) |
| `fe436f366` | `--record-orig` records alone | C's steps |
| `d3737b220` | **separate:** the planner's `:phase :oracle`, `bin/gate --oracle`, absent steps | every step (the planner and the runner are in every key): lands with GTS240 |
| `dbc9a76c2` | **separate:** repr/c's preprocessor reads the headers' pack | gate_key (arbace.repr.c.pp is embedded): every build and the steps on them; lands with GTS240 |

### The shared tooling (ORS2-ORS7)

- **The library**, `test/oracle-records/oracle.bash`, not `bin/lib/oracle.bash` as §6 named it: `bin/arbace-go`'s gate_key hashes
  `bin/lib/` but the crops' and the repr checks' directories, so a new file there rekeys every build. Moving it there, with its
  exclusion from gate_key, belongs to the next change that rekeys every build (ORS15 below).
  - Packs: a deterministic tar.xz (the seed's recipe: sorted, POSIX, mtime 0, owner 0, modes 0644/0755, `xz -9e -T1`), its pin
    `test/CROP/records/NAME.sha256` and manifest `NAME.tsv` beside it (`# ` header lines: the pack, `host: :gcc IDENTITY
    (VERSION)`, the command, the date; then `ID<TAB>KEY<TAB>SHA256<TAB>SIZE`, the member `NAME/ID`). Re-packing the same tree gives
    the same bytes (checked: `c-headers-gcc15` and `c-musl-1.2.6-x86_64` packed twice, the same SHA-256).
  - The store: `ARBACE_ORACLE_PACKS`, by default the main checkout's `.tmp/oracle-packs/` (`NAME/SHA256.tar.xz`, every version
    kept; `unpacked/SHA256/NAME/`). `oracle_unpack` checks the pack's bytes against the pin and every member against the manifest
    (no member missing, none extra) at every unpack; `oracle_get PIN ID` checks the member it returns (exit 3: unrecorded, ORS10
    (a); 1: refused; 2: absent).
  - `oracle_host_id gcc`: the same recipe as `bin/lib/gate-run.bash`'s `gate_gcc_id` (the runner keeps its own copy: changing
    it rekeys every step). `oracle_verdict`: ORS6 (a)'s rule (entries differing under the identity the record names fail; under
    another identity "toolchain moved", reported with the count, exit 0).
- **`bin/oracle-records`**: `list`, `record CROP KIND [--corpus C]`, `verify CROP [KIND...] [--corpus C] [--sample N]` (absent
  where the kind's host is none), `pack`, `unpack`, `pins`, `controls`, `check` (pins and controls), `mirror [--push]`, `fetch
  NAME...`. The kinds are data, `test/oracle-records/kinds.edn` (C's nine: `headers`, `host`, `musl`, `dg`, `c23-syntax`,
  `roundtrip-orig`, `parse-gcc`, `runs`, `tokens`; the last two have no `:verify` yet, `parse-gcc` is verified by
  `roundtrip-orig`'s). Makefile: `oracle-records-list`, `-check`, `-verify`, `-record`, `-mirror`.
- **The mirror (ORS3 (a))**: `bin/oracle-records mirror` prints the commands (`git init --bare STORE/mirror.git`, one commit a
  pinned pack on the orphan branch `oracle-packs` through `git hash-object`/`update-index`/`commit-tree`, its remote `mac` the
  main checkout's, `git -C STORE/mirror.git push mac oracle-packs`); `--push` runs them. Not pushed today.
- **The controls (ORS7 (a))**: the tampered pack `test/oracle-records/controls/tampered.{tar.xz,sha256,tsv}` (two members, one a
  byte off its manifest's digest; its bytes as pinned): `bin/oracle-records controls` requires `oracle_unpack` and `oracle_get` to
  refuse it and its untampered member to be taken; it runs in `bin/oracle-records check`, the first command of `c-dg-essential`,
  and in `bin/gate --pins`. The crossed pairs are in each verify: dg's (one file's live output against another's entry), C23's
  (a file gcc rejects afresh against a line it accepts), the round trip's originals' (one object afresh against another's
  member); each must differ, else the verify fails.
- **`bin/gate --pins`** checks every pinned pack in the store with its members, and the control; no gcc on the host is no
  failure there (its `test/c/host/` check is the oracle tier's).

### C

- **The installed headers (ORS9 (a))**, the pack `c-headers-gcc15`: gcc's include directory whole, musl-dev's and
  fortify-headers' `/usr/include` files (apk's lists) and every header `test/c/host/headers.sha256` names (gmp-dev's and
  pcre2-dev's one each among them), 379 headers and `roots.txt` (search.txt's three directories), 3.5 MB raw, **291 KB xz**
  (`bin/repr-c host pack`; `host verify-pack`; the headers list's 198 digests are the pack's, checked). `ARBACE_C_SYSROOT` names
  its directory (`bin/lib/repr-c/sysroot.bash`'s `c_sysroot`, in `bin/ci` and `bin/repr-c`: the store's pack unpacked, else the
  same members staged from the host's files when each has the manifest's digest, else a refusal; `/` the host's own). A name
  under the roots is read from it alone and keeps its own name in every output: tools/ccdump through cc's `Config.FS` (a name the
  pack lacks opens as an unreadable directory, so cc's fallback to `os.Open` never reaches the host), repr/c's preprocessor in
  `dbc9a76c2` (until it lands, `c_sysroot` requires the host's headers to be the pack's).
- **musl's configured tree**, the pack `c-musl-1.2.6-x86_64`: configure's four products (`config.mak`, `obj/include/bits/
  {alltypes,syscall}.h`, `obj/src/internal/version.h`), **6.6 KB xz** (`bin/ci musl pack`, `verify-pack`); `bin/ci musl prepare`
  takes them from the pack, and configures with gcc only when the store lacks it (ORS14 (a)). The tree prepared from the pack is
  configure's file for file, and musl's forms cache key is the same as the main checkout's (checked).
- **gcc's dg diagnostics**, `test/c/ref-dg-gcc15.edn` (`test/c/dgref.ae`): one map a file, `:sha256` of its bytes, `:action`,
  `:opts`, `:status` and `:diag` (gcc-diag.bash's lines, each byte a character, `\uXXXX` outside 0x20-0x7e). `bin/ci dg` reads it
  in place of gcc (a file with no entry, or whose bytes or dg-options differ, fails: ORS10 (a)); `--record-ref`, `--verify-ref
  [--sample N]`, `--live`. Recorded here: gcc-va-opt and gcc-cpp-c23, 28 of 627 files (the materialized outputs byte for byte
  the live gcc's, the non-ASCII `c23-ucnid-1-utf8.c` among them).
- **C23's acceptance (CS79 (a))**, `test/c/c23/ref-syntax-gcc15.tsv`: all 62 files recorded. `bin/repr-c check --c23` by default:
  judge 1 from the record, levels 1-2 (`--no-gcc`), judges 3 and 5's objects and the control's objects deferred
  (`test/c/c23records.ae`, the records mode of the embedded `arbace.repr.c.c23check`, ORS16); `--oracle` every judge with gcc;
  `--record-ref`, `--verify-ref`.
- **The round trip's originals (CS77 (a) generalized)**: per corpus the pack `c-roundtrip-orig-CORPUS` (`test/repr/c/records/`):
  each original's `NAME.cc` (gcc's compile status, its run's: a status, `link` or `-`), `NAME.o`, `NAME.stdout`, keyed by the
  original's bytes, standard and options. `bin/repr-c roundtrip` reads them in place of compiling the originals and compiles,
  links and runs the print alone (the corpora not packed yet: the originals compiled as before); `--record-orig` (gcc alone,
  minutes), `--verify-orig [--sample N]` (with `parse-gcc.tsv`'s acceptance afresh), `--no-gcc` (levels 1-2 and the acceptance,
  no gcc at all; the known entries of `:objects`, `:objects-macro-body`, `:run-address` and `:print-grammar` that pass the rest
  deferred, not stale: the embedded judge's own `--no-gcc` is ORS16's). Recorded here: benchmarksgame, 52 originals, 121 members,
  **34 KB xz**; its `obj.tsv` and `run.tsv` from the pack are the main checkout's last live run's line for line.
- **The steps** (`aefac28af`): `:gcc` off twelve steps, the packs' pins in their `:pins`, `test/oracle-records/` and
  `bin/oracle-records` in the `:c` group; `c-dg-essential` runs `bin/oracle-records check` first; `c-roundtrip` runs `--no-gcc`.
  The oracle tier's `c-c23-oracle` (`bin/repr-c check --c23 --oracle`), `c-roundtrip-oracle` (the print's levels 3-4, a part a
  corpus) and `c-records-verify` (`bin/oracle-records verify c --sample 8`) keep `:gcc`: in `--full` at `aefac28af`, `:phase
  :oracle` with `d3737b220`.

  | Step | `:hosts` before | after |
  |---|---|---|
  | c-dg-essential, c-c23, c-dg, c-roundtrip | `[:stage0 :gcc]` | `[:stage0]` |
  | c-check-essential, c-aot-essential, c-judge-test, c-aot, c-annotations, c-tokens, c-parse | `[:stage0 :go :gcc]` | `[:stage0 :go]` |
  | c-check | `[:prog :stage0 :go :gcc]` | `[:prog :stage0 :go]` |
  | c-c2j | `[:stage0]` | unchanged |
  | c-c23-oracle, c-records-verify, c-roundtrip-oracle (20 parts) | | `[:stage0 :gcc]` |

### What was checked (light, in the worktree, main's stage 0)

- `bin/gate --full --plan` at `aefac28af`: 250 steps; intersecting each step's material (`.tmp/gate-plan/plan/NAME.inputs`) with
  the files changed since `3f800b868`, the steps that move are C's sixteen (with the three new), `gate-tests` (`bin/gate`,
  `steps.edn` and the C scripts are its inputs) and `reap` (it reads `test/c/` and `test/repr/c/`'s records): nothing else, and no
  build (no file of gate_key changed). 22 `host gcc` lines remain, the oracle tier's 22 steps.
- `bin/gate --full --plan` with gcc off `PATH` (a directory of links to every executable but gcc's): 228 steps, every C step
  `run`, none on `:gcc`. `bin/gate --oracle --plan` at `d3737b220`: with gcc the 22 oracle steps `run`; without it all 22
  `absent` ("host :gcc none here").
- With gcc off `PATH` and the packs from the store: `bin/ci musl prepare` (the pack's products), `bin/ci dg --essential` (92 of 92,
  no execve of gcc or cc1 under strace), `bin/repr-c check --c23` (59 pass, 3 expected, the control's four refused) and `bin/ci
  repl-session` pass; with `dbc9a76c2`, `bin/repr-c pp` opens no file under `/usr/include` or `/usr/lib/gcc` (strace), its stream
  the host-read one byte for byte, as tools/ccdump's pp and types dumps are.
- `bin/ci check --only test/c/essential-check.txt` (45 programs) and `bin/ci check --aot --only test/c/essential-aot.txt` (11) pass
  on the packs; `bin/ci dg --corpus gcc-va-opt,gcc-cpp-c23` from the record judges as the live run; with gcc-embed added, its 47
  files fail as unrecorded and the run fails; `bin/repr-c roundtrip --corpus benchmarksgame` (pack), `--no-gcc` (no gcc execve
  under strace) and `--verify-orig --sample 6` (20 entries equal, the crossed pair differs); `bin/oracle-records verify c headers
  musl c23-syntax host` (all equal, the C23 crossed pair differs); `bin/oracle-records check` (the two packs, the control refused).
- `bin/ci judge-test` (the judges' unit tests, `c-judge-test`'s command) with `dbc9a76c2`: 104 tests, 464 assertions, none failing.

### The night's recording (by hand, on the host with gcc; ORS5 (a): a commit of its own naming `:gcc b0a52aaa52e07744`)

1. The packs into the main store (deterministic, so either): `cp -a .tmp/worktrees/oracle-records-c/.tmp/oracle-packs/c-*
   /root/arbace/.tmp/oracle-packs/`, or in the merged checkout `bin/repr-c host pack` (expected `oracle: packed c-headers-gcc15:
   380 members, 291196 bytes xz, 385f97364c71…`) and `bin/ci musl pack` (`4 members, 6592 bytes xz, 60a22151b385…`); then
   `bin/oracle-records check` (both in the store, the control refused). **Before the first gate of the merge**: `c-dg-essential`
   fails without them.
2. `bin/oracle-records record c dg` (= `bin/ci dg --record-ref`, the seven groups): gcc on 627 files, about 8 minutes at 4 jobs;
   expected `recorded test/c/ref-dg-gcc15.edn: 627 files of gcc-c23,…, 627 in all`, about 460 KB (332 KB of diagnostics in
   the main checkout's last run); then its judgment, the same totals as `test/c/check-dg-linux-amd64.edn`'s.
3. The originals of the round trip, a corpus at a time: `bin/repr-c roundtrip --record-orig --corpus C` for the 19 corpora
   other than benchmarksgame (4,804 files; gcc alone, minutes each, torture-compile and torture-execute the largest); expected
   19 more pins in `test/repr/c/records/`, the objects about 15 MB raw, about 3-5 MB xz in all.
4. `bin/oracle-records verify c` (every entry, no `--sample`): every kind `all equal`, each crossed pair differing.
5. `bin/gate --oracle` (after `d3737b220`; before it, the three steps run in `--full`): `c-c23-oracle`, the 20
   `c-roundtrip-oracle` parts and `c-records-verify` pass. Then `bin/gate --full`: C's steps on the records.
6. `bin/oracle-records mirror --push`, when the user agrees: the packs to the Mac's bare repository's `oracle-packs`.

### Left

- The night's recording above; until it, `c-dg` fails on 599 unrecorded files and `c-records-verify` on the 19 corpora with no
  originals' pack (`c-roundtrip-oracle` compiles their originals as before).
- The oracle record at a tag (`test/gate/oracle-TAG.edn`, §3.6) is not written by `bin/gate --oracle` yet; the nightly rotation
  over a manifest (§3.4) is `--sample N`'s even spread, not GT8's rotation.
- Not migrated here: `runs` (`ref-gcc15.edn`) and `tokens` (`pp-gcc.sha256`) have no verify verb yet; `bin/lib/ci/record-ref`
  and `--record`'s gcc runs are by hand only, as before.

## Proposed (2026-11-05, the tooling and C): ORS15-ORS18

- **ORS15 The library's place.** `test/oracle-records/oracle.bash` today, because a new file in `bin/lib/` rekeys every build.
  - Options: (a) move it to `bin/lib/oracle.bash` with its exclusion from gate_key (`bin/arbace-go`'s two find lists) in the commit
    that lands with GTS240, `gate_gcc_id` then sourced from it (one recipe); (b) leave it in `test/oracle-records/`.
  - Costs: (a) one more rekey-free move at that change, and one place for the identity recipe; (b) nothing, but a library
    outside `bin/lib/`.
  - *Recommended: (a)*.
- **ORS16 The records modes of the embedded judges.** C23's records judge (`test/c/c23records.ae`) and the round trip's
  `--no-gcc` deferral (in `bin/lib/repr-c/roundtrip`, over the judge's output) stand in for `arbace.repr.c.c23check` and
  `arbace.repr.c.rtcheck`, which are embedded (gate_key).
  - Options: (a) fold them into the two namespaces (a `--records`/`--no-gcc` argument, the deferral by class in `judge-row`) in
    the gate_key commit that lands with GTS240, the stand-ins removed; (b) keep the stand-ins.
  - Costs: (a) half a day, one gate_key change shared with `dbc9a76c2`; (b) two judges of C23 to keep in step, and a judgment
    over the judge's printed lines.
  - *Recommended: (a)*.
- **ORS17 gcc's headers outside the clone.** ORS2 (a) commits the essential subsets' dumps so that a fresh clone runs the essential
  gate with nothing external but its corpora; the headers pack holds gcc's include directory (GPL-3.0 with the GCC Runtime
  Library Exception) and musl's headers (MIT).
  - Options: (a) both packs stay in the store, a pinned external of the essential gate as gcc's tarball already is; (b) commit
    musl's configured products (6.6 KB, MIT) and keep the headers pack in the store; (c) commit both.
  - Costs: (a) the essential gate needs the store (or the host's own files, staged when each is the pack's); (b) as (a) for the
    headers; (c) 3.5 MB of GPL-covered headers in the tree, which the vendoring rule keeps out.
  - *Recommended: (a)*.
- **ORS18 The dg record's size.** `test/c/ref-dg-gcc15.edn` will be about 460 KB committed (627 files' diagnostics).
  - Options: (a) committed, within ORS11 (a)'s 3 MB for C (C's records then about 1.5 MB); (b) a pack.
  - *Recommended: (a)*: it is an outcome (class A), read by the essential gate's family, and small.

## Decided (2026-11-05, the user: ORS15-ORS18)

ORS15-ORS18 (a): the library into bin/lib/ excluded from gate_key, and the record judges folded into the embedded c23check and rtcheck, both in the GTS240 change; both packs kept in the store outside the clone; the dg record committed.

## As built: Scheme (write-ahead, 2026-11-05)

ORS12's second crop, on branch `oracle-records-scheme` (from `oracle-records-c`, with `scheme-sm9` merged in), light: the whole
recording is small enough that it was made here (15 s of Chez); what is left to the night is the verification and the commits.
Scheme's gate steps were read from GT9's fragment (`git show gt9-verbs:test/gate/crops/scheme.edn`) and `scheme-sm9`'s scripts.

### What the steps asked of Chez and Chibi, and what each became

| Step | Live call | Class | Now |
|---|---|---|---|
| scheme-read-essential, scheme-read (7 parts) | `chez --script tools/schemedump/dump.ss` over the corpora, the probes and the 7 scans | B | the pack `scheme-chez-dumps`; the essential subset's dumps committed; `:chez` off |
| si-corpora | `bin/si chibi-tests`: chibi-scheme runs 3 libraries' own tests | A | committed records `test/scheme/records/chibi-tests/`; `:chibi` off |
| si-check | `bin/si boot --check`: Chez expands psyntax, `cmp` with `scheme/psyntax/psyntax.pp.ae` | D | the oracle tier's `si-boot-oracle`; out of `si-check`; `:chez` off |
| si-essential, si-aot-essential, si-aot, si-ecraven, si-mats, scheme-roundtrip | none (`ref-chez.edn`, `ref-r7rs-*.edn`, `ref-ecraven-*.edn` committed) | A | unchanged |

No other call: `bin/si test`, `check-suite`, `fixpoint`, `vendor verify`, `r7rs`, `r7rs --chibi-test` and `srfi` read the committed
references; `--chez`, `--chibi` and `--record-ref` are recording verbs no step runs.

### Chez's dumps (class B)

- **The pack `scheme-chez-dumps`** (`test/scheme/records/scheme-chez-dumps.{sha256,tsv}`): 23 units, a member each: `dump-ID.edn` for
  the 15 corpora and `dump-probes.edn`, `scan-KIND.edn` for the 7 scans of every scalar value. 6.2 MB raw (whimsical's 4.7 MB the
  most), **510,016 bytes xz** (`f61291c11fbc…`), made by `bin/repr-scheme check --record-dumps` in 15 s. The `:file` paths are
  normalized (`@corpora/`, `@whimsical/`, `@probes/`, the roots the reading puts back), so the pack is the same wherever the corpora
  are. The key of an entry: the digest of `tools/schemedump/dump.ss` and of each file the dump reads, in order (the probes' files
  are generated from `test/scheme/probes.edn`, so a changed probe changes the key); the identity is the manifest's `host:` line
  (`:chez 1abe097a713483b5`, the gate's `:host-ids` identity, `oracle_host_id chez`), as for C.
- **The essential subset committed** (ORS2 (a)): `test/scheme/records/essential/`, the 15 dumps of the probes, racket-r6rs,
  larceny-r6rs, chibi-r7rs and the srfi-* corpora, **1.4 MB** raw. The essential steps (`scheme-read-essential`) need no store.
  A committed file that differs from the manifest's digest is refused, never replaced by the pack's.
- **`bin/repr-scheme check`** reads the records by default: no Chez run. An entry whose inputs changed is `STALE`, an absent one
  `UNRECORDED`, a differing committed file `REFUSED`; each fails the run (ORS10 (a)). `--live` runs Chez as before (day work and
  the oracle tier), `--record-dumps [--corpus ID]...` records (the units not named are kept from the pack in place), `--verify-dumps
  [--corpus ID]... [--sample N]` runs Chez over the units and compares each with its entry byte for byte (ORS6 (a)'s verdict), with
  the crossed pair (the first unit's live dump against the last unit's entry, which must differ).
- Kind `scheme chez-dumps` in `test/oracle-records/kinds.edn` (`bin/oracle-records verify scheme chez-dumps --corpus ID --sample N`).

### Chibi's answers (class A)

`test/scheme/records/chibi-tests/NAME.chibi.{out,err,rc}` for diff, optional and term-ansi (9 small files, 7 KB) with their manifest
`chibi-tests.tsv`, committed (an outcome, not a pack). The key of a library: its driver text, the digest of its vendored test file
and the escapes flag. `bin/si chibi-tests` replays them (the comparison with si's run is unchanged: `2 of 3 results pass`, the class
`chibi-optional-fallback`); `--live`, `--record-chibi`, `--verify-chibi` as above. Chibi's `run-tests` prints its own elapsed time
("passed in 0.0017 seconds"), the one nondeterministic part: the verify masks it on both sides, the record keeps the bytes as made.
Kind `scheme chibi-tests`. Chibi's identity: `:chibi fa919cb939995a70` (`oracle_host_id chibi`, the same recipe as the gate's).

### Class D: `si boot --check`

Chez expanding psyntax on Arbace's own product has no record to keep. It is the oracle tier's step **`si-boot-oracle`** (`bin/si
boot --check`, 38 s, `:hosts [:stage0 :chez]`, `:phase :oracle`), and kind `scheme boot` (verify only). The expander's fixpoint
without Chez stays in `si-check` (`bin/si fixpoint`). The other oracle-tier Scheme step, **`scheme-records-verify`**
(`bin/oracle-records verify scheme chez-dumps chibi-tests --sample 8`, `:hosts [:stage0 :chez :chibi]`), is the verify of both kinds.

### The fragment and the plan

`test/gate/crops/scheme.edn` is GT9's fragment with these changes (on merge take this file's side): `scheme-read-essential`,
`scheme-read`, `si-check` and `si-corpora` lose `:chez` and `:chibi`; `scheme-read` pins the pack's `.sha256`; `si-check` loses
`bin/si boot --check`; the two oracle steps and their controls (waived: the verifies' crossed pairs) are new; `:scheme` gains
`bin/oracle-records` and `test/oracle-records/`. Not spliced into `test/gate/steps.edn`: GT9's splice tool (`gate.crops`) is on
`gt9-verbs`, so the merge order is gt9-verbs, then this branch.

Checked with the plan (`GATE_STEPS` a scratch table, `.tmp/overlay/`: this branch's `steps.edn` with each fragment spliced in by a
script, `ARBACE_GATE_STORE` a scratch store, `bin/gate --full --plan`):

| Step | hosts with GT9's fragment | with this one |
|---|---|---|
| scheme-read-essential, scheme-read (7 parts) | `chez stage0` | `stage0` |
| si-check | `chez go stage0` | `go stage0` |
| si-corpora | `chibi go stage0` | `go stage0` |
| the other 14 Scheme steps and parts | as before, none on `:chez` or `:chibi` | |

- No Scheme step outside the oracle tier names `:chez` or `:chibi` (the plan's `host` lines of every `si-*` and `scheme-*` input).
- The keys do not depend on the toolchain any more: with `chez` and `chibi-scheme` off `PATH`, this fragment's keys are the same
  (`scheme-read-essential` `30bed9c6c5c3` both ways); GT9's fragment's change (`eb6645ade959` with Chez, `0e32052605fa` without).
- `bin/gate --oracle --plan`: with both tools `si-boot-oracle` and `scheme-records-verify` `run`; without them both `absent`
  ("host :chez none here").

### What was checked (light, in the worktree, main's stage 0)

- `bin/repr-scheme check` with Chez off `PATH`: 1,364 files and probes equal to Chez, the 7 scans equal, 1 m 42 s. With an empty
  store and the essential corpora alone (probes, racket-r6rs, larceny-r6rs, chibi-r7rs, srfi-*): 1,358 equal; whimsical alone:
  `UNRECORDED` naming the missing pack; a changed manifest key: `STALE`; a committed dump with a byte appended: `REFUSED`.
- `bin/repr-scheme check --verify-dumps --corpus srfi-39 --corpus probes --corpus scans --sample 4`: 4 units equal, the crossed pair
  differs. `bin/si chibi-tests --verify-chibi`: 9 entries equal, the crossed pair differs. `bin/oracle-records list` and `check`
  list and check the pack; `bin/si boot --check`: "the bootstrap gives the committed file".
- `bin/si chibi-tests` with Chibi off `PATH`: the same three result lines as `--live`.
- Not run: any gate, any build, the whole `bin/oracle-records verify scheme`.

### The night's commands

1. The pack is in the main store already (this branch's store is the main checkout's, `.tmp/oracle-packs/scheme-chez-dumps/`; the
   command that made it: `bin/repr-scheme check --record-dumps`, deterministic: `oracle: packed scheme-chez-dumps: 23 members,
   510016 bytes xz, f61291c11fbcd93b…`). `bin/oracle-records check` then shows it. The record is a commit of its own naming
   `:chez 1abe097a713483b5` and `:chibi fa919cb939995a70` (ORS5 (a)): `test/scheme/records/`.
2. `bin/oracle-records verify scheme` (every unit and both Chibi kinds, about 3 minutes), then `bin/si boot --check` (38 s).
3. After the merge with gt9-verbs: `bin/gate --full --plan` shows no Scheme step on `:chez` or `:chibi`; `bin/gate --oracle` runs
   the two Scheme steps.
4. `bin/oracle-records mirror --push` once the user agrees (ORS3 (a)): `scheme-chez-dumps` is a pack to mirror.

### Left

- The merge with gt9-verbs (the fragment) and the planner's `:phase :oracle` (`d3737b220`, on `oracle-records-c`): both are the
  merge's; until then `si-boot-oracle` and `scheme-records-verify` exist only in the fragment.
- `bin/si ecraven --chez|--chibi` and `bin/si r7rs --chez|--chibi` (the references' own recording) have no verify verb: they are
  `--record` commands run by hand with their toolchain and judged against the committed `ref-*.edn`; the verify of those records
  is the kinds' next entries (`ref-r7rs-chez.edn`, `ref-ecraven-*.edn`: about 2,800 and 600 rows, minutes of Chez and Chibi).

## Proposed (2026-11-05, Scheme): ORS19

- **ORS19 The essential dumps' form.** The essential subset's 15 dumps are 1.4 MB raw in `test/scheme/records/essential/`, over the
  survey's "under 1 MB" for Scheme.
  - Options: (a) raw text, diffable, as ORS2 (a) says; (b) one committed xz of them (about 150 KB), which a helper change rewrites
    whole and git cannot diff.
  - Costs: (a) 1.4 MB in the tree, each helper or corpus change a visible diff; (b) smaller, opaque.
  - *Recommended: (a)*: a helper change is rare (`dump.ss` has not changed since SM2) and a diff of the dumps is the review.

## Decided (2026-11-05, the user: ORS19)

ORS19 (a): the essential subset's Chez dumps stay committed as raw text (1.4 MB), so a helper change shows as a diff.

## As built: Haskell (write-ahead, 2026-11-05)

ORS12's third crop, on branch `oracle-records-haskell` (from `oracle-records-scheme`, with `haskell-row2` merged: repr/haskell's tools
and records, hsi with HE5 and HE8). The merge's conflicts, resolved as both sides: `Makefile`'s `.PHONY` line (main's list with
`ci-runtime`, row2's `hsi-*` targets added), `doc/JOURNAL.md` (both sides' entries, this branch's first), `test/reap/cells.edn` and
`matrix.edn` (the Scheme row and the Haskell row, `:languages [... :scheme :haskell]`). Light: the helper (`tools/hsdump`) was built here
(1 min), the records were made on samples and on the libraries' dumps the `haskell-he8` worktree's scratch holds; the whole recording is
the night's. Read from GT9's fragment (`git show gt9-verbs:test/gate/crops/haskell.edn`), `bin/repr-haskell`, `bin/lib/repr-haskell/test`,
`bin/hsi` and `bin/lib/hsi/{check,aot}`.

### What the steps asked of GHC, and what each became

| Step | Live call | Class | Now |
|---|---|---|---|
| haskell-read-essential (`bin/repr-haskell test`) | the helper's `dump` and `tree` of the 9 cases' fixed inputs (Shape, Space, Layout, R5's, R6's, HR26's); `ghc -c` and `--show-iface` of the iface cases and the originals; `ghc --make` of R6's originals | B | 74 files committed, `test/haskell/records/test/` (1.06 MB) with the manifest `test.tsv`; no GHC; `:ghc` off |
| the same, the print's half | the helper's `tree` of the printed modules, `ghc -c`/`--make` and `--show-iface` of the printed modules | D | `bin/repr-haskell test --oracle` (45 s), the oracle tier's `haskell-test-oracle`; 22 checks listed as `skipped` in the records mode |
| haskell-roundtrip (4 parts) | the helper's dump (both modes) and tree (both modes) of the originals; the helper's tree and `ghc --make`/`--show-iface` of the print | B; D | the originals from four packs `haskell-{dumps,dumps-full,trees,trees-full}-ghc-9.14.1`; the whole step, GHC reading the print, is the oracle tier's `haskell-roundtrip-oracle` (until ORS20's native reader) |
| hsi-essential, hsi-check | `hsdump stg -O0` per program (`bin/lib/hsi/check`); the libraries' STG (`bin/hsi dumps`, hadrian's flags, 41 packages) | C | the packs `haskell-stg-progs` (keyed per program: sources, flags, helper) and `haskell-stg-libs` (keyed per package); `:ghc` off. GHC's runs are `ref-ghc914.edn` (A, 908 ids) already |
| haskell-native-essential (haskell-native's `native-check`, `tables --check`) | the helper's `dump` and `cpp` of the listed modules | B | not on this branch: the step stays on `:ghc` until that branch reads the packs (ORS20) |

Not steps: `bin/hsi pins`/`spec-check` (the source tarball, no GHC), `libm-check` (the host's cc), `primops` (`ghc :browse`),
`genprimopcode`'s build; `bin/hsi record-ref` (GHC's runs, the record already).

### What the tooling is

- **The identity** `:ghc` in `test/oracle-records/oracle.bash` (`oracle_ghc_id`): GT9's recipe for the `:ghc` host (the `--version` line, the SHA-256 of
  `{libdir}/../bin/ghc-[0-9]*` and of `{libdir}/package.conf.d/ghc-[0-9]*.conf`): here `:ghc b9908216a3a8aa45` (GHC 9.14.1).
- **`bin/lib/repr-haskell/records.bash`** (out of `gate_key`, as `bin/lib/repr-haskell/` is): committed records (`hsr_records_unpack`: `STALE` when
  the key moved, `UNRECORDED`, `REFUSED` when a file's digest is not the manifest's; `hsr_records_make`, `hsr_records_verify` with the crossed pair).
  `bin/lib/hsi/records.bash`: hsi's two packs.
- **`bin/repr-haskell test [--oracle | --record | --verify-records]`**: the originals' GHC calls are one function (`live_origs`) whose outputs
  are the records; the key is one digest of the helper's sources, every input of the cases and `live_origs`' text. `--record` 6 s, `--verify-records`
  74 files equal, the crossed pair differs. With a `ghc` that exits 99 first on `PATH`: 18 checks `ok`, 22 skipped, rc 0, the same 39 `ok`
  lines as the old run under `--oracle`.
- **`bin/repr-haskell record-corpus`, `verify-corpus [--sample N]`**: four packs (a pack a kind: unpacking `trees-full` alone is the
  big one), members `PKG/FILE`, keyed per package by the source tarball's SHA-256, the helper's identity, the kind, the package's plan line as
  the corpus writes it (`$libdir` and `$repo` unexpanded) and `test/haskell/generated`'s files; `REPR_HASKELL_ONLY_PACKAGES="P Q"` selects. `read --corpus`
  and the round trip's originals (`dump_corpus`, `tree_jobs`) read the packs unless `REPR_HASKELL_LIVE=1`; `dump --corpus` stays live (it records).
- **`bin/hsi dumps --record [--packages P,...]`, `record-libs DIR`, `verify-libs --packages P,...|--sample N`**: `haskell-stg-libs`, members
  `PKG/MODULE.stg.ae` (the layout of `.tmp/hsi/dumps/stg`, a `--lib` directory), keyed per package by the tarball, the helper, `HSI_BIGNUM`, the text of
  `bin/hsi`'s dump functions and the plan's line. `bin/hsi run`, `check` and `aot` read it unless `HSI_LIBS` names others or `HSI_LIVE=1`
  (`--live`).
- **`bin/hsi check [--live] [--dump-only] [--record-progs] [--verify-progs [--sample N]]`**: `haskell-stg-progs`, members `ID/stg/FILE` or
  `ID/dump-failed.txt` (the helper's first error: the class `:no-dump` is reproduced), keyed by `prog_key` (what `.tmp/hsi/progs/ID/KEY` already
  was). A program with no entry is `:unrecorded` (`UNRECORDED`, or `STALE` when its key moved), in no class: the check fails (ORS10 (a)).
- Kinds in `test/oracle-records/kinds.edn`: `haskell test-dumps`, `hsdump-corpus`, `stg-libs`, `stg-progs` (B, B, C, C) and `roundtrip` (D, verify only).
  `bin/oracle-records verify haskell ... --sample N` runs each kind's verify (the corpus' with `REPR_HASKELL_ONLY_PACKAGES` while only two packages are recorded).

### Sizes (ORS11)

| Pack | Measured here | Whole corpus |
|---|---|---|
| `test/haskell/records/test/` (committed) | 74 files, 1,057 KB raw | the same |
| `haskell-stg-libs` | 41 packages, 1,320 modules, 157 MB raw, **9,292,924 bytes xz** (17x; 2 min of one xz thread) | the same |
| `haskell-stg-progs` | 63 programs (samples, synthetic), 11.3 MB raw, 335,512 bytes xz (34x) | 908 ids in `ref-ghc914.edn`: about 5 MB xz by the same ratio (testsuite-core and nofib not here) |
| `haskell-dumps` (array, deepseq) | 1.56 MB raw, 122,184 bytes | the native worktree's 106 MB of lines dumps: 8,644,228 bytes xz measured by one tar and xz -9e over it (12x) |
| `haskell-dumps-full` | 4.2 MB raw, 235,572 bytes (18x) | about 289 MB raw by the 68x of dumps, about 16 MB |
| `haskell-trees`, `trees-full` | 5.8 MB, 81,704 bytes (71x); 12.1 MB, 241,648 bytes (50x) | about 396 and 825 MB raw, about 6 and 17 MB |

The whole Haskell packs: about 9.3 + 5 + 46 = **60 MB xz**, inside ORS11's 100 MB a crop (the corpus' share is an extrapolation from two packages
and the dumps' measure: confirm after the night's recording). The committed side: 1.06 MB of records and the manifests (the libraries' 0.23 MB,
the programs' about 0.25 MB, the corpus' four about 1.5 MB at 187 bytes a member and about 8,100 members): about **3.1 MB, a little over ORS11's 3 MB** (ORS21).
Unpacking checks every member (a sha256 of the pack's bytes): `haskell-stg-libs` 157 MB, `trees-full` the largest.

### Checked (light, in the worktree, main's stage 0)

- `bin/repr-haskell test` before: 39 `ok`, 43 s. After, with a `ghc` that exits 99 first on `PATH`: rc 0, 18 `ok`, 22 `skipped`, 32 s; `--oracle`: the same 39 `ok` lines;
  `--record` then `--verify-records`: 74 entries equal (`:ghc b9908216a3a8aa45`), the crossed pair differs.
- `REPR_HASKELL_ONLY_PACKAGES="array deepseq" bin/repr-haskell record-corpus`: 4 packs (100 members) in 49 s; `verify-corpus --sample 2`: 100 entries equal, crossed pair
  differs; `read --corpus` and `read --full --corpus` with no GHC: 16 dumps converted, 925 forms; `roundtrip` (tree level) on the packs: the originals' side read, 16 modules equal.
- `bin/hsi record-libs` of the `haskell-he8` worktree's 41 packages' dumps (helper `60fdf0c2f05c`, the same as this tree's), then `bin/hsi verify-libs --packages
  array,deepseq,pretty,stm`: dumped afresh by the helper, 32 entries byte for byte equal, crossed pair differs (the recording's source is therefore credible; its manifest says so).
- `bin/hsi check --live --dump-only --record-progs --corpus samples,synthetic`: 63 programs, 42 s; `--verify-progs --sample 6` and via `bin/oracle-records verify haskell test-dumps
  hsdump-corpus stg-libs stg-progs --sample 2`: all equal, crossed pairs differ.
- `bin/hsi check --only` four programs (samples, synthetic) with GHC replaced by a stub that exits 99: 4 pass (libraries from the pack, the 1,320 images made in 10 s,
  programs from the pack); with one sample's source changed: `:unrecorded`, `STALE`, "in no class", the run fails; `bin/oracle-records pins`/`controls`: the Haskell packs found in the
  store with their members, the tampered pack refused (the C packs are not in this store).
- Not run: any gate, any build of the executable, `bin/hsi check --aot` (it reads the libraries through the same function; its programs build with TamaGo's go), the
  testsuite-core and nofib corpora (no network), the whole `record-corpus`.

### The plan (scratch overlay)

`.tmp/overlay/` here: this branch's tree with `bin/lib/gate-run.bash` given the `:ghc` host (GT9's `:host-ids :ghc` is on `gt9-verbs`, not here; the recipe is `oracle_ghc_id`'s)
and `steps.edn` with GT9's fragment and, apart, this branch's spliced in; `bin/gate --full --plan`:

| Step | hosts with GT9's fragment | with this one |
|---|---|---|
| haskell-read-essential | `ghc stage0` | `stage0` |
| hsi-essential, hsi-check | `ghc go stage0` | `go stage0` |
| haskell-roundtrip (4 parts) | `ghc stage0` | gone from the gate: `haskell-roundtrip-oracle` (4 parts) in the oracle tier |
| haskell-native-essential | (in haskell-read-essential) | `ghc stage0`: the one gate step on `:ghc` left (ORS20) |

- With another `ghc` first on `PATH` (a stub printing 9.99.9) every key of GT9's fragment's seven steps and parts changes (`haskell-read-essential` `1df4724d021d` to `4ff9e34121dd`);
  with this fragment `haskell-read-essential` (`dbbb8368f0f2`), `hsi-essential` (`e021f77afd60`) and `hsi-check` (`78da2ff99729`) do not, `haskell-native-essential` does.
- `bin/gate --oracle --plan`: `haskell-test-oracle`, `haskell-roundtrip-oracle` (tree, layout, iface, text) and `haskell-records-verify` `run`.

### The night's commands

1. `bin/repr-haskell record-corpus` (the whole corpus into the four packs, the helper 32 at a time; the dump and the trees of 1,345 modules, both modes: estimate 20-30 min),
   then `bin/oracle-records verify haskell hsdump-corpus --sample 8` (the pack must be in the store first).
2. `bin/heavy-lock bin/hsi dumps --record` (the libraries compiled by the helper as hadrian does: heavy; it replaces the pack made here from the he8 worktree's dumps; the same bytes expected,
   which `bin/hsi verify-libs --sample 8` then shows) and `bin/hsi pins --fetch` (the testsuite tarball and nofib: network).
3. `bin/hsi check --live --dump-only --record-progs --corpus samples,synthetic,testsuite-core` (about 63 programs a 40 s: the 700 of testsuite-core about 10-15 min; add `nofib` to record it too).
4. `bin/repr-haskell test --record` is done here (the records are in this branch); redo it only when the helper or a case changes.
5. A commit of its own naming `:ghc b9908216a3a8aa45` for the records and pins (ORS5 (a)); `bin/oracle-records verify haskell`; `bin/oracle-records mirror --push` once the user agrees (five packs, about 60 MB).
6. After the merge with gt9-verbs: `bin/gate --full --plan` shows hsi-essential and hsi-check without `:ghc`, `bin/gate --oracle` the three steps.

### Left

- **haskell-native's verbs** (`native-check`: the helper's `dump` and `cpp` of its list; `tables --check`: GHC's version pin only): not on this branch; until `native_check` reads the packs
  its step keeps `:ghc` (ORS20, ORS21).
- **The committed pins are samples**: the corpus packs hold array and deepseq, `haskell-stg-progs` the 63 programs of samples and synthetic; the night's commands replace them. An unrecorded package or
  program fails (`UNRECORDED`), by design.
- The class D steps no longer run per merge: GHC's reading of the print (trees, interfaces) is once a night at most and at tags (ORS1 (a)); the forms level of the round trip is still in `haskell-read-essential`
  (the byte-for-byte prints of mode `:full`, the layout's expected files).
- `test --record`'s key covers `live_origs`' text: a new case rekeys the records (STALE until `--record` is run again, by hand).

## Proposed (2026-11-05, Haskell): ORS20-ORS21

- **ORS20 The print's reading, and haskell-native's steps.** GHC reads Arbace's print at every level of the round trip (the helper's tree and `ghc --make`), class D; the native reader
  (haskell-native, `arbace.repr.haskell.native`) is the survey's N substitute (§5) and is not merged.
  - Options: (a) as built: the whole round trip and the print halves of `test` in the oracle tier, `native-check` on a gate step with `:ghc` until its verb reads the packs; (b) keep the round
    trip's tree, layout and text parts on `--full` with `:ghc` until the native reader reads the print; (c) merge haskell-native first, then make `native_check` read `haskell-dumps` (and a pack of the
    `cpp` outputs) and the round trip's print side read natively (N), so that the gate keeps the tree level with no `:ghc`.
  - Costs: (a) the print's tree level is checked once a night, not per merge, and one gate step keeps `:ghc` for a while; (b) the gate keeps three Alpine-GHC-dependent parts (each GHC upgrade
    reruns them); (c) the most work (about a day), after the merge of a branch the user has not scheduled.
  - *Recommended: (a) now, then (c)*: (a) is the survey's ORS8 (a) for class D; (c) is the entry that gives the tree level back to the gate with no toolchain.
- **ORS21 The committed size.** The records and manifests come to about 3.1 MB (estimated from 187 bytes a manifest line and 8,100 corpus members), a little over ORS11's 3 MB for a crop.
  - Options: (a) accept 3.1 MB (the figure is confirmed after the night's recording); (b) one key a package in a separate small file and the manifests without the KEY column (about 1.3 MB less, a new
    manifest format for this crop); (c) raise Haskell's cap to 3.5 MB.
  - Costs: (a) none beyond the overrun; (b) a second format next to `oracle.bash`'s; (c) a precedent.
  - *Recommended: (a)*: the overrun is 3%, and the manifests are what makes a tampered member visible.

## Decided (2026-11-05, the user: ORS20, ORS21)

ORS20: (a) now, the round trip and test's print halves in the oracle tier and native-check on :ghc, then (c) haskell-native merged and the print's tree level native in the gate; ORS21 (a): the committed side's 3% overrun accepted.

## As built: Rust (write-ahead, 2026-11-05)

ORS12's fourth crop, on branch `oracle-records-rust` (from `oracle-records-haskell`, with `rust-row` merged: Rust's REAP row on rust-re5's rsi,
tools and records). The merge's conflicts, resolved as both sides: `Makefile`'s `.PHONY` line (the haskell branch's list with rust-row's `rsi-*`
targets added), `test/reap/cells.edn` (the comment lines, `:languages [... :scheme :haskell :rust]`, `:names`, the Rust cells after Haskell's;
one slip fixed after: Haskell's block closed the map, so the Rust cells had landed outside it), `test/reap/main.ae` (Scheme's and Haskell's
readers kept, Rust's `:rust-lower` added; the two `:corpus-known` readers merged into one whose `:count` is `:hs-modules`, `:files`, `:ifaces`
or `:programs`, the case name `ghc-VERSION` or the corpus's name; `classify-entry` kept Haskell's, which does what Rust's did too) and
`test/reap/matrix.edn` (both rows). `bin/reap` after the merge: 27 of 28 cells checked (Rust AOT "no check yet"), its only problems the Java
eval row's one-test drift (`pass 1847, recorded 1848`), which this branch did not touch. Light: the helper was built here (10 s, with the
apks from `crop-controls-rust`'s `.tmp` by `REPR_RUST_APKS`), std built here (45 s), the records made on the checkout's samples and on the
whole `rustc-1.99.1` corpus; the other corpora (library, compiler, the crates, the UI suite) need sources this host has no network for.
Read from GT9's fragment (`git show gt9-verbs:test/gate/crops/llvm-rust.edn`), `bin/repr-rust`, `bin/lib/repr-rust/{test,run-program,code-unit}`,
`bin/rsi` and `bin/lib/rsi/{check,wrap-ir,link-ir,plan.bash}`.

### What the steps asked of rustc, and what each became

| Step | Live call | Class | Now |
|---|---|---|---|
| rust-read-essential (`bin/repr-rust test`) | the helper's `dump` (both modes, the expression file) and `tree` of the 10 cases' fixed inputs; rustdoc's JSON of shapes, attrs and the mutated shapes; whether edition 2018 parses edition2015.rs | B | 43 files committed, `test/rust/records/test/` (1.22 MB, manifest `test.tsv`); no rustc; `:rustc` off |
| the same, the print's half | the helper's `tree` of the printed files (cases 3, 4, 7, 8, 9, 10), rustdoc of the printed shapes and attrs, `run-program` and `code-unit` of the originals and the print | D | `bin/repr-rust test --oracle` (62 s), the oracle tier's `rust-test-oracle`; 11 checks listed as `skipped` in the records mode |
| rsi-essential, rsi-check, rsi-aot | `rustc --emit=llvm-ir,link` per program (wrap-ir, link-ir: the program's codegen unit and the allocator shim by `clang -S -emit-llvm`); std's IR (`bin/rsi std-build`, cargo) for `runtime-build` | C | packs `rust-ir-samples` (5 programs), `rust-ir-synthetic` (67) and `rust-std-ir` (110 units); `:rustc` off. rustc's runs are `ref-rustc-1.99.1.edn` (A) already |
| rust-roundtrip, rust-iface (originals' half) | the helper's dump (both modes) and tree of every file of the corpus | B | packs `rust-{dumps,dumps-full,trees}-rustc-1.99.1` (1,074 files, 144 + 180 + 200 MB raw) |
| rust-roundtrip, rust-iface, rust-behaviour (the print's half) | the helper's tree of the printed files, rustdoc of the mirror, `run-program` and `code-unit` builds, `cargo -Zbuild-std` of std from the print | D | the oracle tier's `rust-roundtrip-oracle`, `rust-iface-oracle`, `rust-behaviour-oracle` (class D until a native reader reads the print) |
| rust-native(-essential) | rsdump per file of `native-check.txt` | B | not on this branch (rust-native is not merged here): the dumps carry the absolute `:abs` the pack format below normalizes; the step is the merge's (ORS22) |

Not steps: the UI suite's tarball and the crate set's `.crate` files (pinned sources), `bin/rsi sets`, `lower-unit` (lli, `:llvm`), `lower-check`
(rustc builds each program from the original and its lowered IR: both sides live, an oracle-tier candidate with `rsi-aot`'s native-std leg, left).

### What the tooling is

- **The identity** `:rustc` in `test/oracle-records/oracle.bash` (`oracle_rustc_id`): GT9's recipe for the `:rustc` host (`rustc --version -v`,
  the executable's SHA-256 through its links, the sysroot's `librustc_driver-*.so`): here `:rustc b789fd432132e377`, rustc 1.99.1 (Alpine r0).
  The packs of `rust-std-ir` and `rust-ir-*` are made by rustc and by `clang -S -emit-llvm` (the shim); the record names rustc's identity only (ORS23).
- **`bin/lib/repr-rust/records.bash`** (out of `gate_key`): the committed record, as Haskell's (`rsr_records_unpack`: `STALE`, `UNRECORDED`, `REFUSED`;
  `rsr_records_make`; `rsr_records_verify` with the crossed pair); the helper's identity `rsr_helper_hash` is `bin/repr-rust`'s `helper_hash`.
- **`bin/repr-rust test [--oracle | --record | --verify-records]`**: the originals' calls are one function (`live_origs`) whose outputs are the records, the
  key one digest of the helper, every file under `test/rust/sample/` and `live_origs`' text. The dumps' `:abs` is the one path in any output (the converter
  reads the file it names): written `@WORK@/...` and `@ROOT@/...` in the records and put back at the unpack. Trees, lines and rustdoc's JSON hold no path.
  `--record` 3 s, `--verify-records` 43 files equal, the crossed pair differs. With a `rustc` that exits 99 first on `PATH`: 15 checks `ok`, 11 skipped,
  rc 0, 54 s; `--oracle`: 28 `ok`, none failed. One case moves to the records side whole: the changed doc comment (rustdoc of two originals).
- **`bin/repr-rust record-corpus [CORPUS]`, `verify-corpus [--sample N]`**: three packs (a pack a kind: `dumps`, `dumps-full`, `trees`), members `FILE.edn` or
  `FILE.tree`, `FILE.lines` (FILE relative to the corpus's source root), the dumps' `:abs` written `@SRC@/FILE`, keyed per crate (the corpus's source pin, the helper's
  identity, the kind, the crate's plan line); `REPR_RUST_ONLY_CRATES="core alloc"` selects crates and keeps the others from the pack in place. `read --corpus` and the
  round trip's originals (`dump_corpus`, the three `orig` tree loops) read the packs unless `REPR_RUST_LIVE=1`; `dump --corpus` stays live (it is what records). A host with
  no rustc needs only rust-src's apk (`REPR_RUST_APK_ONLY=rust-src`, in `prepare`), the files the dumps name. Whole record: 8 min, one xz thread the larger part.
- **`bin/rsi check --record-ir | --verify-ir [--sample N] | --ir-only`**: a pack per corpus `rust-ir-samples`, `rust-ir-synthetic` (`-ui` the night's), members
  `KEY/FILE` for each program (KEY: its id as a directory name; `ir.status`, `x.ll`, `shim.ll`, and `x.refused` or `ir.log` of a failure), keyed by `ir_key`: the program's
  source (the UI corpus's tarball pin for the UI suite), its plan line, the digest of `test/rust/runtime/ir.sha256` (std's identity), and the wrap, link and plan scripts. The shim's
  `ModuleID` names the scratch directory: written `@WORK@`, put back by `ir1`. `check` reads the packs (`ir_records_get`: a program with no entry is `UNRECORDED`, one whose key moved
  `STALE`: the step fails, ORS10 (a)) unless `RSI_LIVE=1`; the heavy half (runtime-ci, cirun, musl) is skipped by the three flags. `--record-ir` records whole corpora.
- **`bin/rsi std-record | std-verify`**: `rust-std-ir`, members `CRATE/UNIT.ll`, keyed by the std pin (the apk's SHA-256, `std.edn`, `ir.sha256`); `runtime_build` takes
  std's IR from it when the build in place is not at the pin and `RSI_LIVE` is unset (`std_ir_get`: the unpacked units' digests must be `ir.sha256`'s); `std-verify` builds std afresh into
  `.tmp/rsi/std-verify` and compares.
- Kinds in `test/oracle-records/kinds.edn`: `rust test-dumps` (B), `corpus-dumps` (B; one line for the three packs, its verify runs all three), `std-ir` (C), `ir` (C).

### Sizes (ORS11)

| Pack | Members | Raw | xz | Ratio |
|---|---|---|---|---|
| `test/rust/records/test/` (committed) | 43 | 1.22 MB | not packed | |
| `rust-dumps-rustc-1.99.1` | 1,074 | 144.5 MB | 9,070,628 | 16x |
| `rust-dumps-full-rustc-1.99.1` | 1,074 | 180.0 MB | 13,495,720 | 13x |
| `rust-trees-rustc-1.99.1` | 2,148 | 199.5 MB | 4,151,080 | 48x |
| `rust-std-ir` | 110 | 50.9 MB | 2,376,000 | 21x |
| `rust-ir-samples` | 15 | 5.9 MB | 295,808 | 20x |
| `rust-ir-synthetic` | 201 | 23.7 MB | 984,424 | 24x |

The packs total 30.4 MB, inside ORS11's 100 MB a crop. The committed side: the 43 test files and the manifests (the corpus' three 0.73 MB, the IR's and std's 0.06 MB), **2.0 MB**, inside 3 MB.
The larger corpora by the same ratios would not fit (ORS22): library, compiler and the crates have 13,665 files against 1,117, about 110 MB of dumps alone. Unpacking checks every member: `rust-trees` 200 MB.

### Checked (light, in the worktree, main's stage 0)

- `bin/repr-rust test --record` then `--verify-records`: 43 entries equal (`:rustc b789fd432132e377`), the crossed pair differs. With a stub `rustc` and `rustdoc` first on `PATH` (exit 99):
  rc 0, 15 `ok` lines and 11 `skipped`. `--oracle`: 28 `ok`, none `FAIL`.
- `REPR_RUST_ONLY_CRATES="proc_macro test" bin/repr-rust record-corpus` (3 packs, 47 files, 18 s), `verify-corpus --sample 12`, `read --corpus` from the packs with the `.tmp/repr-rust/dumps` removed (47 dumps,
  47 converted, 1,779 forms), `roundtrip rustc-1.99.1` on the same crates (the originals' trees from the pack: 47 equal, the others `:no-original`, as a partial record must be); then the whole
  `record-corpus` of `rustc-1.99.1` (8 min) and `bin/oracle-records verify rust test-dumps corpus-dumps ir --sample 20`: 43, 60 and 15 entries equal, crossed pairs differ.
- `bin/rsi check --record-ir --corpus samples,synthetic` (27 s); `--verify-ir` from another work directory: 15 and 201 entries equal (the shim's `ModuleID` was the one difference, now normalized);
  `--ir-only` with a stub `rustc` first on `PATH`: the IR from the packs in 2 s; a changed synthetic program: `STALE`, the check fails. `bin/rsi std-record` (32 s) and `std_ir_get` into a clean directory: 110 units, `ir.sha256` equal to the pin.
- `bin/oracle-records pins`: the Rust packs found in the store with their members (the other crops' packs are not in this scratch store).
- Not run: any gate, any build of the executable, `bin/rsi check` past the IR (runtime-ci lowers std: 4 to 6 GiB), `std-verify` (cargo), the library, compiler, crates and UI corpora.

### The plan (scratch overlay)

`.tmp/overlay/` here: this branch's tree with `bin/lib/gate-run.bash` given the `:rustc` host (GT9's `:host-ids :rustc` is on `gt9-verbs`, not here; the recipe is `oracle_rustc_id`'s) and `:cxx`, `:llvm`
fixed, a stub `cpp-runtime`, and `steps.edn` with GT9's fragment (its LLVM step left out) and, apart, `test/gate/crops/rust.edn` spliced in; `bin/gate --full --plan`:

| Step | hosts with GT9's fragment | with this one |
|---|---|---|
| rust-read-essential | `stage0 rustc` | `stage0` |
| rsi-essential | `stage0 go rustc cxx gcc` | `stage0 go cxx gcc` |
| rsi-check | `.samples` and `.ui`: `stage0 go rustc cxx gcc` | `.samples`, `.synthetic`: `stage0 go cxx gcc` (the ui part waits for `rust-ir-ui`) |
| rust-roundtrip (2 parts planned), rust-iface, rust-behaviour (3 parts) | `stage0 rustc` (and `gcc`) | gone from the gate: `rust-roundtrip-oracle`, `rust-iface-oracle`, `rust-behaviour-oracle` in the oracle tier |

- No Rust step outside the oracle tier names `:rustc` (the `host` lines of every `rust-*` and `rsi-*` input); `:cxx` and `:gcc` stay on the rsi steps (the lowering's C++ part, gcc).
- With another `rustc` first on `PATH` (a stub printing 9.99.9) every key of GT9's fragment's steps changes (`rust-read-essential` `8b4bf4cabb52` to `4735e3a46d22`, `rsi-essential` `2298b3733aab` to
  `5b919c5157e0`); with this fragment `rust-read-essential` (`b628ea54d3ec`), `rsi-essential` (`998175104b00`) and `rsi-check.samples` (`cc80c744ceec`) do not.
- `bin/gate --oracle --plan`: `rust-test-oracle`, `rust-roundtrip-oracle` (rustc-1.99.1, -ui), `rust-iface-oracle`, `rust-behaviour-oracle` (run, std, code-std) and `rust-records-verify` `run`.
- The controls named by GT9's fragment (`test/rust/controls.edn`, `roundtrip-controls.edn`) are the ones the merge brings; the new oracle steps reuse them, the verify waives its own crossed pairs.

### The night's commands

1. `bin/rsi std-build` has made the std at the pin here; `bin/rsi std-verify` (cargo, 1 min) after the commit shows the pack is what a second build gives.
2. The UI suite: `bin/repr-rust corpus fetch rustc-1.99.1-ui` (the tarball: network), then `bin/rsi check --record-ir --corpus ui` (the whole list, the programs not built by the judge left out: ORS22's size to confirm)
   and `bin/repr-rust record-corpus rustc-1.99.1-ui` (dumps both modes and trees of 24,831 files: the pack's size decides ORS22).
3. `bin/oracle-records verify rust --sample 8` and, once, `bin/oracle-records verify rust test-dumps corpus-dumps std-ir ir` whole; a commit of its own naming `:rustc b789fd432132e377` for the records and pins (ORS5 (a));
   `bin/oracle-records mirror --push` once the user agrees (six packs, 30 MB).
4. After the merge with gt9-verbs: `bin/gate --full --plan` shows `rust-read-essential` and the rsi steps without `:rustc`, `bin/gate --oracle` the five.

### Left

- **rust-native's verbs** (`native-check`: rsdump per file of its list, the dumps' `:abs` the same normalization): the branch is not merged here; its step reads the corpus packs once it is (ORS22).
- **The UI suite**: no pack (`rust-ir-ui`, its dumps and trees), so `rsi-check`'s ui part is out of the fragment and the UI round trip's parts run live (`REPR_RUST_LIVE=1`) in the oracle tier; the same for library, compiler and the crates.
- **`rsi lower-check`** (both sides built by rustc from the original, one with the lowered IR as the linker's input) and `rsi record-ref` still need rustc and std's sysroot: not gate steps, left as they are.
- `bin/rsi check` past the IR on the packs was not run here (heavy); the IR step alone was (`--ir-only`).
- The class D steps no longer run per merge: rustc's reading of the print is once a night at most and at tags (ORS1 (a)); the forms level of the round trip stays in `rust-read-essential` (mode `:full`'s byte for byte).
- `test --record`'s key covers `live_origs`' text and `sample/`'s files: a new case rekeys the records (STALE until `--record`, by hand). A `rustc` that moved with Alpine edge: the verify reports "toolchain moved", the records are kept.

## Proposed (2026-11-05, Rust): ORS22-ORS23

- **ORS22 The larger corpora's records, and rust-native.** The packs cover `rustc-1.99.1` (std's five crates, 1,117 files), the checkout's rsi programs and std's IR. Library, compiler, the crate set and the UI
  suite (about 13,700 and 24,831 files) are not recorded: by the std ratios their dumps alone would be 110 MB, past ORS11's 100 MB a crop, and the rest of the packs 2 to 3 times that.
  - Options: (a) as built: those corpora run live in the oracle tier (`REPR_RUST_LIVE=1`), the UI IR pack (`rust-ir-ui`, programs only) recorded at the night and `rsi-check`'s ui part back; (b) record the
    dumps and trees of every corpus, raising Rust's cap to about 400 MB; (c) record a rotating subset per corpus (a list of files, GT8-style) and run the rest live.
  - Costs: (a) the compiler's and the crates' round trips need rustc as they do today, in the oracle tier only; (b) a precedent of four times the cap, the store and mirror 400 MB; (c) a subset list per corpus to keep.
  - *Recommended: (a)*: the larger corpora are read-and-print checks of Arbace's converter, run once a night; what the gate needed (the essential and the rsi steps) is recorded.
- **ORS23 The identity of the packs' producers.** `rust-std-ir` and `rust-ir-*` are made by cargo and rustc, and the shim and std's IR text by `clang -S -emit-llvm`; GT9's `:rustc` host covers rustc and its driver.
  - Options: (a) as built, the record names rustc's identity (the clang version is in `std.edn`'s pin and `std-build` refuses another); (b) the kinds name two hosts (`:rustc` and `:llvm`), the manifest's header both identities;
    (c) a combined host `:rustc` that adds `clang --version`.
  - Costs: (a) none, a moved clang is found by `std-build`'s check and by `std-verify`; (b) the oracle tier's verify needs both, `kinds.edn` a second `:host`; (c) a change of GT9's recipe, a rekey of every Rust step.
  - *Recommended: (a)*.

## Decided (2026-11-05, the user: ORS22, ORS23)

ORS22 (a): as built, the larger corpora live in the oracle tier, the UI suite's IR pack recorded at night, rust-native's native-check with its merge; ORS23 (a): the packs name rustc's identity only.

## As built: OCaml (write-ahead, 2026-11-05)

ORS12's fifth crop, on branch `oracle-records-ocaml` (from `oracle-records-rust`, with `ocaml-o8` merged: OCaml's REAP row, the gate steps, `bin/oi check --only-list`,
`judges-test`, the essential subset `test/ocaml/essential-check.txt`). `ocaml-libs2` is a descendant of `ocaml-o8` (22 commits more: libraries, not steps), so it was not needed; the typer
line (`ocaml-typer2`, `ocaml-t10`) is not merged. The merge's conflicts, resolved as both sides: `CLAUDE.md` (Scheme's paragraphs kept, OCaml's added beside each), `Makefile` (the oracle-records targets
and the `reap` line, "C, Scheme, OCaml"), `bin/lib/gate-run.bash` (`gate_scheme_id` and `gate_ocaml_id` both, `hosts.edn` printing `:chez`, `:chibi`, then `:ocaml`; the `ancestors.txt` line of the
branch kept), `test/gate/audited-test.bash` and `gate-tests-test.bash` (their fabricated `hosts.edn` with `:ocaml`), `test/gate/steps.edn` (o8's groups `:ocaml`, `:ocaml-run`, `:ocaml-aot`, its steps
after the C steps, the `reap` step's inputs and summary with both rows), `test/reap/cells.edn`, `main.ae`, `matrix.edn` (Scheme's, Haskell's and Rust's rows kept, OCaml's cells appended; the `:ocaml` reader
beside `:scheme` and `:rust-lower`). `bin/reap` after the merge: 31 of 32 cells checked (Rust AOT "no check yet"), problems only the Java eval and AOT rows' one-test drift (`pass 1847, recorded 1848`),
which this branch did not touch. `test/gate/costs-test.bash` fails on o8's steps (they are in no `:crops` crop: GT9's `:budgets :new-crops` is what admits `:ocaml`, and it is not on this branch);
the same on `ocaml-o8`. Light: the helper `tools/ocdump` built here (1 s), the pinned tarball copied from another worktree's `.tmp` (no network), every record made on this host's OCaml 5.5.0;
the opam set's rows need the packages' tarballs (present here) and were recorded too. Read: GT9's fragment (`git show gt9-verbs:test/gate/crops/ocaml.edn`), `bin/oi`, `bin/lib/oi/*`,
`bin/repr-ocaml`.

### What the steps asked of ocamlc and ocdump, and what each became

| Step | Live call | Class | Now |
|---|---|---|---|
| oi-essential (`bin/oi test`, `repl-check`, `check --only-list`) | `ocamlc -c` of the stdlib in dependency order and ocdump `--mode lambda --simplify` per unit (`bin/oi stdlib`); ocdump per run program (`oi_one`); `ocamlc -c` and ocdump `--mode types|lambda` per REPL phrase (`bin/lib/oi/phrase`) | C | packs `ocaml-lambda-stdlib` (73 members) and `ocaml-lambda-progs` (309), the REPL's phrases committed (`test/ocaml/records/repl/`, 72 files, 18 KB); `:ocaml` off |
| oi-check, oi-aot, oi-suite-aot (the run corpus and the suite) | the same per program; the suite's units `ocamlc -c` in link order (their `.cmi` for the next) and ocdump per unit (`suite_dump`) | C | the packs above and `ocaml-lambda-suite` (98 programs, 434 members); `:ocaml` off. ocamlc's and ocamlopt's runs are `ref-ocaml55.edn` and `ref-suite.edn` (A) already |
| ocaml-read-essential, ocaml-roundtrip (stdlib, samples), `--spec`, `spec-check` | ocdump's parse dump and `--mode tokens` of every original; the same of the spec's 218 examples; the print's parse dump (the tree level's compare); `ocamlc -c` of the original and of the print (the `.cmi` level) | B for the originals; D for the print | packs `ocaml-dumps-{samples,stdlib,spec}`; the text level (mode `:full` printed back, byte for byte) and the print run from them; `:ocaml` off. The print's redump and the `.cmi` level are the oracle tier's |
| ocaml-dumps | `ocamlc -c` plus ocdump `--mode types|lambda` per corpus, each twice | B (a test of the helper itself) | the oracle tier's `ocaml-dumps-oracle`, unchanged |
| ocaml-judges | the judges' synthetic cases (no toolchain), the floors, the key, the OI column; the expect action's cases (the expect tool built by `ocamlc -linkall`); `repr_levels_test` and the dumps' determinism (ocdump) | none; C and A; B | `bin/oi judges-test --records` (the first four, no toolchain); the rest `ocaml-judges-oracle` (`bin/oi judges-test`) |
| oi-divergences | none | | unchanged |
| ocaml-roundtrip (compiler-sources, compiler-backend, testsuite, whiml, opam) | as the stdlib's | B, D | no packs yet: the oracle tier's `ocaml-roundtrip-oracle` runs them live (the night's `record-dumps` brings them back, ORS24) |

Not steps and not migrated: `bin/oi record-ref`, `suite record-ref` (ocamlc's and ocamlopt's runs, A: the records themselves), `bin/oi suite expect` (class A against `ref-expect.edn`, live), `bin/repr-ocaml corpus check`
(already `bin/gate --pins`' in GT9), the typed and Lambda dumps of `dumps` (B, the dumper's own test), ocaml-native (not merged).

### What the tooling is

- **The identity `:ocaml`** in `test/oracle-records/oracle.bash` (`oracle_ocaml_id`): `gate_ocaml_id`'s recipe (O8; GT9's `:host-ids :ocaml` is the same): ocamlc's and ocamlopt's version lines, their executables' SHA-256
  through the links, compiler-libs' `ocamlcommon` (`.cmxa`, `.a`, `.cma`) and the stdlib's archives; here `:ocaml c7205b33cdf0c5f6`, OCaml 5.5.0 (Alpine). It equals `gate_ocaml_id`'s output.
- **`bin/lib/oi/records.bash`** (in `bin/lib/oi/`, out of `gate_key`): `oir_member PIN ID KEY` (UNRECORDED, STALE, REFUSED, absent pack), `lambda_prog`, `oir_stdlib_get`, `oir_pack_make` (a partial recording keeps the old pack's
  other members). `OI_LIVE=1` runs ocamlc and ocdump as before; without it `bin/oi` needs neither: `need_tree` fetches the tarball only, `stdlib_key` is the pack's identity.
- **`bin/oi stdlib --record | --verify`**: `ocaml-lambda-stdlib` (members `UNIT.ae`, `order.txt`, `Testing.ae`; one key: the helper's source, `OCAML_VERSION`, the tarball's pin); `cmd_stdlib` unpacks it into `.tmp/oi/lib` and `testlib`.
- **`bin/oi record-lambda [--only-list FILE | --only RE]`, `verify-lambda [--sample N]`**: `ocaml-lambda-progs`, a member `ID.ae` for every run program, keyed by the program's source, its flags and library and the helper
  (the first recording keyed 26 programs wrong: `IFS=$'\t' read` collapses empty fields, the flags and the library shifted; found by `judges_test`'s `subst1` case as STALE and fixed with the `\037` separator the file already used elsewhere;
  all 309 made again). `check`, `check --aot` and `suite check` unpack the pack once before their parallel children.
- **`bin/oi suite record-lambda`**: `ocaml-lambda-suite`, members `ID/units.list`, `ID/U.ae` or `ID/error`, a key per program (the units' sources); `suite_dump` reads it unless `OI_LIVE=1`. No `:verify` for this kind
  (the record command on a scratch store is the verify; listed as such in `kinds.edn`).
- **`bin/oi repl-check --record | --verify-records`**: `bin/lib/oi/phrase` answers from `test/ocaml/records/repl/KEY.{status,ae,typed}` (KEY: the helper, the opened units, the phrase), manifest `repl.tsv`; UNRECORDED or REFUSED print as the
  compiler's error and the session differs from `session.ref`, so the check fails.
- **`bin/repr-ocaml record-dumps [CORPUS...]`, `verify-dumps [--sample N]`**: packs `ocaml-dumps-CORPUS` (members `parse/REL.ae`, `tokens/REL.ae`; the spec's `parse/` and `check/ID.dump`), one key a file; `dump_one`, `layout_one`
  and `spec-check` read them unless `REPR_OCAML_LIVE=1`; an unusable record is named in a file and fails the step (never an `(ocaml/error` a known reject could hide behind). `roundtrip --oracle` is the live run, including the print's
  redump and the `.cmi` level; in the records mode those two are reported as the oracle tier's. The verifies run a crossed pair (the first live output against the last record: must differ).
- **`bin/oi judges-test --records`**: `judges_test` with `OI_JUDGES_NO_LIVE=1` (the expect action's 7 cases left out and counted as run), `floors_test`, `oi_key_test` (its live-key case, a missing `OI_OCDUMP_KEY`, under `OI_LIVE=1` and left out of `--records`; its scratch root takes
  `test/oracle-records/` too), `aot_oi_column_test`; without `--records` every test, the helper's and the expect tool's too.
- Kinds in `test/oracle-records/kinds.edn`: `ocaml lambda-stdlib` (C), `lambda-progs` (C), `lambda-suite` (C), `repl` (C, committed), `dumps` (B), `roundtrip` (D, nothing to record). Makefile targets `oi-*-record`, `oi-*-verify`,
  `repr-ocaml-record-dumps`, `repr-ocaml-verify-dumps`, `repr-ocaml-oracle`, `ocaml-judges-records`.

### Sizes (ORS11)

| Pack | Members | Raw | xz | Ratio |
|---|---|---|---|---|
| `ocaml-lambda-stdlib` | 73 | 5.56 MB | 242,796 | 23x |
| `ocaml-lambda-progs` | 309 | 13.1 MB | 403,140 | 33x |
| `ocaml-lambda-suite` | 434 | 23.6 MB | 359,028 | 66x |
| `ocaml-dumps-stdlib` | 300 | 8.47 MB | 506,864 | 17x |
| `ocaml-dumps-samples` | 16 | 66 KB | 8,996 | 7x |
| `ocaml-dumps-spec` | 436 | 66 KB | 7,880 | 8x |

The packs total 1.5 MB, far inside ORS11's 100 MB a crop. The committed side: the manifests (265 KB for six packs) and the REPL's record (72 files, 18.5 KB raw), under 0.4 MB, inside 3 MB. The survey's estimates were high
(the run corpus has 309 programs, not 2,400; the stdlib's Lambda 5.5 MB as estimated). The other corpora's dumps (compiler-sources, compiler-backend, testsuite, whiml, opam: the survey's 76 MB of parse and token dumps in all)
by the stdlib's ratio would be about 5 MB.

### The plan (scratch overlay)

`.tmp/ov2/` here: this branch's tree (`git archive`), `before` its `steps.edn` as merged (ocaml-o8's steps, with the two `:rotate` maps taken out: their subsets name `run/ID` where the record's ids are bare, a mismatch of o8 with this tree's
planner, GT9's `:id-re` answers it), `after` with o8's OCaml steps replaced by `test/gate/crops/ocaml.edn`'s steps and group (the fragment's own `controls` and `crops` sections are GT9's planner's); `bin/gate --full --oracle --plan`
on each, stage 0 linked from the main checkout, the hosts each step names read from the plan's `plan/STEP.inputs`:

| Step | hosts before (o8) | hosts after |
|---|---|---|
| oi-essential | `stage0 go ocaml` | `stage0 go` |
| oi-divergences | `stage0` | `stage0` |
| ocaml-read-essential | `stage0 ocaml` | `stage0` |
| ocaml-roundtrip | 7 parts, `stage0 ocaml` | `.stdlib`, `.samples`: `stage0` |
| oi-check, oi-aot, oi-suite-aot | `stage0 go ocaml` | `stage0 go` |
| ocaml-judges | `stage0 go ocaml` | `stage0 go` |
| ocaml-dumps | `stage0 ocaml` | gone from the gate: `ocaml-dumps-oracle` |
| (new, `:phase :oracle`) | | `ocaml-roundtrip-oracle` (parts samples, stdlib; the compiler, testsuite, whiml and opam parts `:clean-only`), `ocaml-dumps-oracle`, `ocaml-judges-oracle` (`stage0 go ocaml`), `ocaml-records-verify` (`stage0 ocaml`) |

- No OCaml step outside the oracle tier names `:ocaml` after the fragment (the `host` lines of every `oi-*`, `ocaml-*` input).
- With a stub `ocamlc` and `ocamlopt` first on `PATH` (printing 9.99.9) every key of the gate's OCaml steps is unchanged (`oi-essential` `a41420f20a55`, `ocaml-read-essential` `56b2d64db78d`, `oi-check.run` `f00fa2cd7663`,
  `oi-aot.run`, `oi-suite-aot.suite`, `ocaml-roundtrip.stdlib`, `ocaml-judges`, `oi-divergences`), the four oracle steps' keys change (`ocaml-records-verify` `26aabc633d2c` to `f37b2f001f64`). The `before` side was not re-run under the stub.
- The negative controls named by GT9's fragment (`test/ocaml/controls.txt`, `roundtrip-controls.txt`): the first is here; `roundtrip-controls.txt` is `crop-controls-ocaml`'s, which merges with the fragment.

### Checked (light, in the worktree, main's stage 0)

- With `ocamlc` and `ocamlopt` stubs (exit 99) first on `PATH` and `.tmp/ocdump` moved away (so no ocdump either): `bin/oi check --only-list test/ocaml/essential-check.txt` (58 programs: 58 pass, the control differs), `bin/oi repl-check`
  (24 phrases equal), `bin/oi check --aot --only-list` of four programs, `bin/oi suite check --only-list` of two suite programs, and the repr/ocaml step's commands (`roundtrip --text samples stdlib`, `--spec`, `spec-check`, `embed-check`: samples
  8 and stdlib 148 of 150 text equal, the 2 the parse rejects as classed, 218 of 218 spec examples equal), 1 min 8 s.
- `bin/oi judges-test --records` under the same stubs (4 min 44 s): `judges_test` 46 cases run (the expect's 7 counted), `floors_test`, `aot_oi_column_test` ok; `oi_key_test` failed on the first run (its two
  "missing part" cases and the scratch root, all fixed as above) and passes alone under the stubs (the live-key case skipped, said).
- `bin/oi stdlib --verify` (the stdlib built afresh by ocamlc: 72 entries equal), `bin/oi verify-lambda --sample 5` and `bin/repr-ocaml verify-dumps --sample 4 samples` (equal, crossed pairs differ), `bin/oi repl-check --verify-records`
  (72 equal), `bin/oracle-records verify ocaml lambda-stdlib lambda-progs repl dumps --sample 6` (all equal, `:ocaml c7205b33cdf0c5f6`), `bin/oracle-records pins` (the six packs found with their members, the tampered-pack control refused),
  `roundtrip --oracle --cmi samples` (the live levels still pass: 8 equal, 1 `.cmi` equal).
- A source of `test/ocaml/samples/` changed by one comment: `roundtrip samples` fails (`STALE parse/ops.ml.ae`). The whole run corpus (309 programs) and the whole suite corpus (98 programs) were recorded here; their whole checks were not run (heavy).
- Not run: any gate (the plans only), any build of the executable, `bin/oi check` past samples, the whole `suite check`, the opam corpora's dumps (no pack).

### The night's commands

1. `bin/oi stdlib --verify`, `bin/oi verify-lambda`, `bin/oi suite record-lambda` again (the suite has no verify), `bin/repr-ocaml verify-dumps` on a host whose OCaml moved (reported, not failed).
2. The other corpora's dumps: `bin/repr-ocaml record-dumps compiler-sources compiler-backend testsuite whiml opam-astring ...` (`roundtrip ... all` names the opam corpora; about 5 MB of packs by the ratios); then move their
   `ocaml-roundtrip` parts from the oracle tier to the gate in the fragment (pins named, as stdlib's).
3. `bin/oracle-records verify ocaml --sample 8` and, once, `bin/oracle-records verify ocaml lambda-stdlib lambda-progs repl dumps` whole; a commit of its own naming `:ocaml c7205b33cdf0c5f6` for the records and pins (ORS5 (a));
   `bin/oracle-records mirror --push` once the user agrees (six packs, 1.5 MB).
4. After the merge with gt9-verbs: `bin/gate --full --plan` shows the steps without `:ocaml`, `bin/gate --oracle` the four; run `bin/gate --audit oi-essential ocaml-read-essential oi-check` to see that no step reads ocamlc.

### Left

- The oi-native typer line (`ocaml-typer2` .. `ocaml-t10`) is not merged: when `bin/oi` types natively the stdlib and the programs, the Lambda packs become checks of it (class C to N), not necessities; recorded here because the Lambda
  is what the gate still reads (the typer yields typed trees, Lambda comes from ocdump's `Translcore`/`Simplif`).
- The tree level of the round trip (the print read by ocdump) and the `.cmi` level are not in the gate any more: the survey's class D, the oracle tier's. The native parser (ocaml-native) is the substitute that gives the tree level back (ORS25).
- `bin/oi suite expect` (class A against `ref-expect.edn`, the expect tool built by `ocamlc -linkall`) and `record-ref` need ocamlc: the oracle tier's `ocaml-judges-oracle`; `suite expect` is not a record of its own.
- The stdlib's and the REPL's verifies run no crossed pair (one pack, one session); the programs', the suite's by record and the dumps' do.
- `GT9`'s `:rotate` (subsets of the AOT levels and the round trip) is not in the fragment: the records make a whole check cost what the run costs, the rotation's budget is the planner's (gt9v-ocaml's `--only-list` verbs for
  `repr-ocaml` are not here).
- Records are keyed by the helper's source and the corpora's pins, not by Arbace's code: a change to `tools/ocdump/ocdump.ml` stales every Lambda and dump record (STALE until the night's re-record, by hand).

## Proposed (2026-11-05, OCaml): ORS24-ORS25

- **ORS24 The larger corpora's dumps.** compiler-sources, compiler-backend, testsuite, whiml and opam have no packs here; the compiler's two are not essential and their round trips are the oracle tier's today.
  - Options: (a) as built: record them at the night (`record-dumps`, about 5 MB), then the gate's `ocaml-roundtrip` takes all seven parts from the packs, the print's tree level and the `.cmi` level staying the oracle tier's; (b) leave them live in
    the oracle tier for good; (c) record the dumps and rotate (GT8.4-style) the parts.
  - Costs: (a) a night's run (minutes on a host with ocamlc), 5 MB of packs, the fragment's parts edited; (b) the compiler's text level checked only at night; (c) a subset list to keep.
  - *Recommended: (a)*: the packs are small (the ratios of the stdlib's), and the text level is the check that costs the gate nothing but CPU.
- **ORS25 The tree level without ocdump.** The print's parse by ocdump (class D) was the round trip's strongest per-merge check; it is now once a night. ocaml-native (`arbace.repr.ocaml.native`, ports of OCaml's lexer and parser) would read the print
  natively (survey class N, ORS8 (a)).
  - Options: (a) as built, the oracle tier only, then merge ocaml-native and let `ocaml-roundtrip` compare the print's native parse with the original's recorded dump; (b) keep `:ocaml` on `ocaml-read-essential` for the redump until then;
    (c) a second pack of the print's dumps (a record of Arbace's own output: stale at every printer change).
  - Costs: (a) the tree level not per merge for a while; (b) the essential step on a toolchain, the reason of this migration; (c) a record of a product, not an oracle, re-recorded often.
  - *Recommended: (a)*, as ORS20 for Haskell: the native reader is the substitute that makes the level free again.

## As built: C++ and LLVM IR (write-ahead, 2026-11-05)

ORS12's last crops, on branch `oracle-records-cpp` (from `oracle-records-ocaml` at `500b3a13f`, which already held C, Scheme, Haskell, Rust and OCaml's gate steps; `oracle-records-ocaml` was merged again at `056caff62`, its survey sections and ORS24-ORS25 beside this crop's ORS26-ORS27, conflicts in `test/oracle-records/oracle.bash` (the `case` and the identity functions: `ocaml`, `cxx`, `llvm`), `kinds.edn` (both kinds' lines) and the survey (both sections) resolved as both sides), with `cppi-check` and `llvm-row`
merged. **The merges**, resolved as both sides. `cppi-check` (C++'s four REAP cells, the run checks, the steps `cpp-check*` and `cpp-aot*`, the sampling's records): `CLAUDE.md` (the two C++
paragraphs beside the Scheme and OCaml ones), `Makefile` (the `.PHONY` list: the union of both, a backslash lost on the way and put back; the `reap` target's `##` line names Scheme, OCaml and C++),
`bin/gate` (the pins record: `:oracle-packs` and a new `:cpp {:pins-check :host-check}` in one `printf`), `bin/lib/gate-run.bash` (`gate_cxx_id` beside `gate_scheme_id` and `gate_ocaml_id`; the hosts file's
printf chain one line a host), `bin/reap` (the header), `test/gate/steps.edn` (the reap step's inputs and summary; the C oracle block closed before C++'s steps), `test/reap/cells.edn` (`:languages`,
`:names`, the comment lines, the cells: the earlier crops' map closed once, after C++'s), `test/reap/main.ae` (`:results-dir` in `missing-files` beside `:parts`), `arbace/cpp/lower.ae` (the other crops'
`packed-vector-c-size-align` beside cppi-check's `sha256-hex`); `test/reap/matrix.edn` recorded afresh. `llvm-row` conflicted in `test/reap/cells.edn` alone (the languages, names and the end of the cells).
`bin/reap` after both: 37 of 40 cells checked (the three "no check yet" are cells of other crops' checks not recorded yet), 0 problems. It was run twice, after each merge; the first time
it showed the Java eval and AOT rows one test off (`pass 1847, recorded 1848`), the drift the Rust section names, and the record made here holds the checks' present numbers.

Read from the scripts: `bin/cppi`, `bin/lib/cppi/{check,check-aot,lists.bash,record-ref}`, `bin/repr-cpp` and `bin/lib/repr-cpp/{pp-check,roundtrip,roundtrip-corpus,dg,cxxdump-check}`, `tools/cxxdump`, `bin/repr-llvm`,
`bin/lib/repr-llvm/results`, `test/cpp/selftest/`, GT9's fragments (`git show gt9-verbs:test/gate/crops/cpp.edn`, `llvm-rust.edn`).

### What the steps asked of the toolchain, and what each became

| Step | Live call | Class | Now |
|---|---|---|---|
| cpp-read-essential, cpp-tokens (`bin/repr-cpp pp-check`) | none: g++ `-E` and clang's raw tokens only with `--record`, their digests are `test/repr/cpp/pp-gxx.sha256`, `lex-clang.sha256`; the installed libstdc++ headers are read | A, E | `:cxx` dropped. With `g++`, `clang++` and `clang` first on `PATH` as stubs exiting 99, `pp-check --essential` runs: 17 s, the three corpora's lines agree. The headers (class E) are checked against `test/cpp/host/headers.sha256` by `bin/repr-cpp host check --headers` (`bin/gate --pins` runs it; the compilers' own answers are the oracle tier's `host check`) |
| cpp-print-essential, cpp-roundtrip (originals' half) | `tools/cxxdump` (clang's `-ast-dump=json`, filtered) on each file and its `-norm` form | B | packs `cxxdump-KEY` (test/repr/cpp/records/), one per list, **the cache's own layout**: each entry's dump, status, errors and dependencies' digests, which `cxxdump -records DIRS` reads with no clang. Recorded here: `samples` (9 files), `gxx-dg-essential` (33) |
| the same, the print's half | cxxdump of the print (level 1: forms again; level 3: the AST equal), `g++ -c` and `clang++ -c` of both (level 4) | D | the oracle tier's `cpp-roundtrip-oracle` (`REPR_CPP_LIVE=1`): all the levels, every list. In records mode the judge (`rtcheck judge ... records`) does not judge levels 1, 3, 4 and says so (a line on standard output); no native substitute yet: CX11's reader of the print is not merged here. The gate's level is the read, the print and the forms' round trip of the originals |
| cpp-lower-essential, cpp-lower | `clang++ -O0 -S -emit-llvm` per program; `clang++ x.ll` link and run of the original; `clang -c x.ll`; g++'s build and run (the reference's opinion); `clang -c print.ll` (print's object) | C, A, D | pack `cpp-ir-samples` (programs of samples and runtime): members `x.ll`, `ir.status`, `ir.log`, the original's link and run (`orig*`), g++'s (`gxx*`), the digest of `clang -c x.ll`. The print's object comparison: `object` = `skipped` from the records, judged in the oracle tier's `cpp-lower-oracle` (`CPPI_LIVE=1`); the lowered C under gcc stays on `:gcc` |
| cpp-runtime (`runtime-build`) | 52 libsupc++/libstdc++ units compiled to IR by clang; the headers' pin and the gcc tarball's extraction | C | pack `cpp-runtime-ir`: the 198 units' IR (the runtime has grown since the survey counted), keyed by the unit's line, its shim, the tarball's pin, the recorded clang line and the recipe; `test/cpp/runtime/ir.sha256` still holds (a build in records mode compares it). `runtime_key` takes the recorded clang line, not `clang --version` |
| cpp-check(-aot) (cppi-check) | `clang++ -emit-llvm` per program (`ir1`); the testsuite's util library IR | C | the same pack; `ir1` reads it; g++'s runs are `ref-gxx15.edn` (A, already). `libstdcxx` and `gxx-dg-run`: packs `cpp-ir-libstdcxx`, `cpp-ir-gxx-dg-run` (the night's) |
| cpp-selftest | clang, g++, gcc, llvm-as, opt, lli and cxxdump in 28 of its 33 tests (three of them by their live build) | A | `test/cpp/selftest/toolchain.txt` names them: the gate's step runs `--no-toolchain` (5 tests: `ctor-throw`, `odd-width`, `oracle-key`, `results-hold`, `results-nonpass`), the oracle tier's `cpp-selftest-oracle` `--toolchain` live. Three (`headers-pin`, `results-record`, `runtime-key`) exercise the live build and moved with them |
| llvm-roundtrip | `clang -c` of the original and the print, `llvm-as` of both, byte for byte | A, D | `test/llvm/records/roundtrip-orig.tsv` (680 modules: the originals' digests, `none` when the original does not build); the print's half is `llvm-roundtrip-oracle` (`REPR_LLVM_LIVE=1`). The substitute in records mode: **the native verifier** (`arbace.repr.llvm.verify`, llvm-ll4's) must judge each print as valid or invalid as opt did the original (the pack below), `print-verify.tsv` |
| llvm-verify (`verify-check`) | `opt -passes=verify -disable-output` on each module | B | pack `llvm-opt-verify`: status and whole stderr of 680 modules (paths as `@ROOT@`, addresses `<0xADDR>`); `verify-check` reads it; the live opt is `llvm-verify-oracle` |
| cpp-dg (CX9), cpp-native, cpp-codegen | | | not on this branch (cpp-dg joins later; cpp-native and cpp-codegen are other branches): their cxxdump calls would take `-records` the same way |

Not steps: `bin/repr-cpp host record|check` (class A, the compilers' answers: `test/cpp/host/`), `record-ref` (g++'s runs), `dg --record-ref`: they stay the by-hand recorders, listed as kinds below.

### What the tooling is

- **Identities** in `test/oracle-records/oracle.bash`: `:cxx` (`oracle_cxx_id`, the text of `gate_cxx_id`: g++'s and clang++'s version lines, their drivers, `cc1plus`, clang, the headers' digests: `:cxx 778e744abed1efac`, g++ 15.2.0 and clang 23.1.4)
  and `:llvm` (`oracle_llvm_id`, GT9's recipe: `llvm-as --version`, `llvm-as`, `opt`, `lli`, `FileCheck`, `libLLVM.so.*`: `:llvm b584dbaff8a679a5`, LLVM 23.1.4). `bin/lib/gate-run.bash` gains `gate_llvm_id` and the `:llvm` entry of `hosts.edn` (the oracle steps name it).
- **`test/oracle-records/cpp.bash`** (out of `gate_key`): `cpr_live` (`CPPI_LIVE`), `cpr_key` (the source's bytes, the standard, the arguments with the checkout's path as `@ROOT@`, the recorded clang line, the headers' pin, the recipe),
  `cpr_ir_get` (the members of an ID into a directory: exit 3 UNRECORDED, 4 STALE, 1 refused), `cpr_ir_or_fail` (a failure written as `ir.status` 99 and the message in `ir.log`: the program then fails the check), `cpr_prepare`
  (the packs unpacked once before a parallel run: `oracle_check_members` keeps scratch files that two `xargs` workers would share), `cpr_dump_dirs` (the unpacked cxxdump packs, colon-joined).
- **`bin/cppi record-ir [--corpus C,...] [--runtime] [--verify [--sample N]]`**: by hand, with clang++ and g++. `one` (the lowering's native steps) and the run checks' `ir1` read the pack unless `CPPI_LIVE=1`; `rt_ir` the runtime pack (`runtime_ir`, split out of
  `runtime_build`, needs neither tarball nor headers in records mode). `two` and `rt_two` write `object` = `skipped`; `arbace/cpp/check.ae`'s judge takes `skipped` as the print's object judged elsewhere.
- **`tools/cxxdump -records DIRS [-clang-id LINE | CXXDUMP_CLANG_ID]`**: the cache's lookup over recorded entries (a miss is an error, "unrecorded"; an entry whose dependency is on the host with another digest, "stale"; a dependency not on the host is not stale: the record stands),
  the key's clang part the recorded line. **`bin/repr-cpp record-dumps KEY`, `verify-dumps KEY [--sample N]`** (a list's dumps made into a fresh cache and packed, minus the stats; verified entry by entry); `roundtrip --dumps-only`; `REPR_CPP_LIVE=1` is the live mode.
- **`bin/repr-llvm list hw|probes|runtime|valid|all`** (the lists of the records: the runtime's IR is the pack's members), `record-orig`, `verify-orig`, `record-opt`, `verify-opt`; `roundtrip`, `roundtrip --text` and `verify-check` take a list name for LIST. The verifies print their crossed pair
  (one module's live answer against another's record: must differ).
- Kinds in `test/oracle-records/kinds.edn` (crop `cpp`: `ir`, `runtime-ir`, `cxxdump`, `host`, `runs`, `dg`, `tokens`; crop `llvm`: `roundtrip-orig`, `opt-verify`), `bin/oracle-records verify cpp ir runtime-ir`, `verify llvm`.
- Gate: the steps' `:hosts` below; `bin/gate`'s pins record gains `:cpp` (the headers' pin); the group `:cpp` of `steps.edn` gains the tooling's directories and `test/llvm/`.

### Sizes (ORS11)

| Pack or record | Members | Raw | xz | Ratio |
|---|---|---|---|---|
| `cpp-ir-samples` | 244 | 3.74 MB | 266,964 | 14x |
| `cpp-runtime-ir` | 990 | 38.2 MB | 1,270,392 | 30x |
| `cxxdump-samples` | 90 | 0.61 MB | 35,328 | 17x |
| `cxxdump-gxx-dg-essential` | 318 | 1.74 MB | 58,052 | 30x |
| `llvm-opt-verify` | 1,360 | 62 KB | 22,644 | 3x |
| `test/llvm/records/roundtrip-orig.tsv` (committed) | 680 rows | 106 KB | | |

Committed: manifests 200 KB (cpp) + 19 KB + 66 KB, the LLVM record 312 KB with the opt manifest: about 0.6 MB, inside ORS11's 3 MB. The packs recorded here total 1.65 MB. **The night's** (estimated from these ratios and the recorded lists' counts): `cpp-ir-libstdcxx` (the testsuite's
3,874 run tests: 234 are the every-15th list, 17 KB xz a program as the samples') about 65 MB, `cpp-ir-gxx-dg-run` (about 1,675: 67 every-25th) about 12 MB, the round trip's dump packs (`gxx-dg-every-10` 1,807 files, `gxx-old-deja-every-10` 323, `c-c++-common-every-10`, `libstdcxx-testsuite-every-40`;
the essential list's 33 files are 58 KB xz, 1.8 KB a file) about 8 to 12 MB the four, the modules lists (`gxx-modules`, 1,181 files) 2 MB. The crop's total about 100 MB: **at ORS11's cap** (ORS26).

### Checked (light, in the worktree, main's stage 0, `clang`, `clang++`, `g++`, `clang-23`, `opt` and `llvm-as` first on `PATH` as stubs exiting 99 where it says so)

- `bin/cppi record-ir` (25 s), `record-ir --runtime` (1 min 26 s, 198 units); `bin/oracle-records verify cpp ir --sample 4`: 4 entries equal. `cpr_ir_get`: the same source gives 16 members; another standard, an edited source: STALE (rc 4); a name not in the pack, a corpus with no pack: UNRECORDED (rc 3).
- `bin/cppi runtime-build` with the stubs: IR from the pack in 11 s, `ir.sha256` equal to the pin, 198 units lowered, built and judged, 198 pass (3 min 43 s). `bin/cppi lower-essential` with the stubs: 6 files, 6 pass, the negative control differs as it must (28 s).
- `bin/repr-cpp record-dumps samples` (21 s), `record-dumps gxx-dg-essential`; `roundtrip-corpus samples` with the stubs: 9 pass (3 s); `roundtrip-corpus gxx-dg-essential` with the stubs: 33 files, 31 pass and 2 rejected by the oracle as recorded, rc 0.
  (A first run read the print's dumps in records mode too and failed all nine: the print's dump, forms again and objects are skipped in records mode now.)
- `bin/repr-cpp pp-check --essential` with the stubs: ok, 17 s. `bin/cppi selftest --no-toolchain` with the stubs: the first run, 8 tests, 5 pass and 3 fail (`headers-pin`, `results-record`, `runtime-key`, which exercise the live build: moved to `toolchain.txt`, leaving the five); `selftest headers-pin results-record` live: pass. `runtime-key` live was not rerun here.
- `bin/repr-llvm record-orig all` (680 modules, 70 s), `record-opt all` (46 s; a first record held a heap address of opt's messages: normalized), `verify-orig`, `verify-opt` with `--sample 10` to `60`: all equal, the crossed pairs differ.
  `roundtrip` over the four hand-written modules with the stubs: 4 of 4 forms equal, 4 originals recorded, 0 prints judged otherwise by the native verifier than opt the original (3.5 s); `verify-check` over 60 probes with the stubs: 59 of 60 verdicts as opt's, 52 of 53 first messages, 59 of 60 whole stderr (the probes' known file holds the rest).
- `bin/oracle-records verify cpp ir --sample 4`, `verify llvm --sample 30` (23 entries a kind): equal.
- `bin/cppi lower-corpus samples` with the stubs: 16 files, 16 pass, the control differs (26 s). `bin/cppi check --only test/cpp/essential-check.txt` (eval on ci) with the stubs, the lowered runtime built from the pack's IR before:
  samples 3 pass, runtime 2 pass and `threads` in its recorded class, no REGRESSION, the run control differs as it must; the ci library of the runtime was read in about 25 minutes (2 threads, 2 GiB limit, the host loaded), the programs in a few.
  `--aot` was not run here.
- `bin/repr-llvm roundtrip valid` with the stubs (the 4 hand-written modules and the runtime's 198 units' IR from the pack): 202 of 202 forms equal, 202 originals recorded, 0 prints the native verifier judges otherwise than opt the original, rc 0 (3 min 24 s, 4 threads, 12.6 CPU minutes).

### The plan (scratch overlay)

`.tmp/overlay/` here: a copy of the tracked files at this branch's tree with `test/ocaml/subsets.edn` emptied (the OCaml branch's subsets name runs its records lack: `bin/gate --plan` refused the real tree on it, not part of this work), the stage 0 of main by symlinks;
`bin/gate --full --plan` and `--oracle --plan`.

- **Before** (the merged `steps.edn` of cppi-check): 20 C++ steps and parts name `:cxx`; with `g++` and `clang++` stubs printing 9.99.9 first on `PATH` **all 20 keys change**.
- **After**: the 15 steps and parts of C++ and LLVM in `--full` (`cpp-read-essential`, `cpp-print-essential`, `cpp-lower-essential`, `cpp-check-essential`, `cpp-aot-essential`, `cpp-selftest`, `cpp-tokens` (3 parts), `cpp-roundtrip.samples`, `cpp-lower.samples`,
  `cpp-check.samples-runtime`, `cpp-aot.samples-runtime`, `llvm-roundtrip`, `llvm-verify`) name neither `:cxx` nor `:llvm`: **all 15 keys are equal** with the stubs first on `PATH`. `:gcc` stays on `cpp-lower-essential` and `cpp-lower` (the lowered C under gcc, class D with no native substitute here).
- The only steps with `:cxx` or `:llvm` in `:hosts`: `cpp-roundtrip-oracle` (6 parts), `cpp-lower-oracle` (2), `cpp-check-oracle` (2), `cpp-selftest-oracle`, `llvm-roundtrip-oracle`, `llvm-verify-oracle`, `cpp-records-verify`, `llvm-records-verify`, all `:phase :oracle`; `bin/gate --oracle --plan` lists them with the C crop's.
- The parts that wait for records left the gate (ORS22's precedent): `cpp-roundtrip`'s four sampled lists, `cpp-lower`'s `libstdcxx-every-15th`, `cpp-check` and `cpp-aot`'s `gxx-dg-run-every-25` run live in the oracle tier until the night records them; a part's records that are missing fail as UNRECORDED or STALE, they are never taken on trust.

### The night's commands (a commit of its own naming `:cxx 778e744abed1efac` and `:llvm b584dbaff8a679a5`: ORS5 (a); record from the main checkout, its path is in the IR)

1. `bin/cppi record-ir` and `bin/cppi record-ir --runtime` again in the main checkout (the IR names the source by its path in `source_filename` and the program's own strings: the packs here were made in this worktree and read anywhere, but a fresh record from `/root/arbace` is the one whose digests do not depend on a worktree's name).
2. The run corpora's IR: `bin/cppi testsuite`, then `bin/cppi record-ir --corpus libstdcxx` and `--corpus gxx-dg-run` (the libstdcxx util library's four IR members as `libstdcxx/util/NAME.cc`: not made by `record-ir` yet, ORS26's second half).
3. The round trip's dumps: `bin/repr-cpp record-dumps KEY` for `gxx-dg-every-10`, `gxx-old-deja-every-10`, `c-c++-common-every-10`, `libstdcxx-testsuite-every-40`, `gxx-modules-every-10`: 8 min for the g++.dg tenth live (CP124), 4 threads; `verify-dumps KEY --sample 100` after.
4. `bin/repr-llvm record-opt` over the mutants and held-out sets (`bin/repr-llvm mutants LIST OUT N`; the names `NAME.KIND.INDEX`), and `record-orig` for the 278 modules of the round trip's lists that are not the runtime's.
5. `bin/oracle-records verify cpp ir runtime-ir cxxdump host` and `verify llvm` whole, then `bin/gate --oracle`; `bin/oracle-records mirror --push` once the user agrees (the packs here are 1.7 MB, the night's about 100 MB).

### Left

- **The print's levels** of the round trip (clang's dump of Arbace's print, g++ and clang objects) and the lowering's print object are the oracle tier's only: the gate no longer sees them. CX11's native reader of the print, or `cpp-codegen`'s IR, would bring level 1 back; the native substitutes noted: llvm-ll4's verifier on the print (wired in `roundtrip` of repr/llvm; not in cppi's `rt` level) and cpp-codegen's IR (not merged).
- **`:gcc`** on `cpp-lower-essential` and `cpp-lower` (gcc compiling the lowered C): the survey's "replace by the ci run" is `cpp-check`'s already; the lowering's own gcc stage stays until it is retired (ORS27).
- Recorded here: samples, runtime, `gxx-dg-essential`, hw, probes, the runtime's IR as modules. Not recorded: the other corpora and the mutants (above). `bin/repr-cpp dg` (CX9), `cpp-native` and `libstdcxx` util IR: not on this branch.
- `selftest runtime-key` in live mode, `bin/cppi check --aot` on the packs (heavy), `bin/repr-llvm verify-check valid` and `roundtrip --text` over the runtime's modules (the round trip and 60 probes' verify-check were run).
- `rsi` (the Rust branch's) still names `:cxx` and `:gcc` for the lowering and `lli`; on merge its steps take `bin/cppi`'s records the same way.

## Proposed (2026-11-05, C++ and LLVM IR): ORS26-ORS27

- **ORS26 The C++ crop's night size.** The IR of `libstdcxx` and `gxx-dg-run` and the round trip's dump packs come to about 100 MB, ORS11's cap.
  - Options: (a) record them all (about 100 MB, mirror and store the same); (b) record `gxx-dg-run`, the run lists' samplings (every 15th and 25th: 234 and 67 programs, about 5 MB) and the dumps of the essential lists and the every-40th/every-10th lists, the rest live in the oracle tier (about 25 MB); (c) record the samplings alone, the dumps live.
  - Costs: (a) the cap's boundary and a mirror of 100 MB; (b) the gate's parts are the samplings it runs already, every other list the oracle tier's, as ORS22 (a); (c) the round trip's gate level is the essential list only.
  - *Recommended: (b)*: the gate runs these samplings, nothing else reads the whole lists.
- **ORS27 The lowering's gcc stage.** `cpp-lower*` keep `:gcc` (class D with no native substitute).
  - Options: (a) as built: C's `:gcc` stays on them until the lowering is retired in favour of `cpp-check`'s ci run; (b) move the gcc half to the oracle tier now (the lowering step then checks forms and the lowered C's reading by repr/c and ci's checker); (c) record gcc's compile of the lowered C (it changes with every lowering change: not recordable).
  - Costs: (a) none, C's agent takes `:gcc` out of its own steps; (b) the lowering's gate step loses its gcc and run levels (ci's run in `cpp-check` covers the run); (c) none works.
  - *Recommended: (a)* now, (b) once `cpp-check-essential` covers every program of `essential-lower.txt`.

## Decided (2026-11-05, the user: ORS24-ORS27)

ORS24 (a), ORS25 (a) for OCaml; ORS26 (b): C++'s night records gxx-dg-run, the samplings and the essential and sampled dump lists (~25 MB), the rest live in the oracle tier; ORS27: :gcc kept on cpp-lower* now, its gcc half to the oracle tier once cpp-check-essential covers essential-lower.txt.
