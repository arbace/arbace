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
