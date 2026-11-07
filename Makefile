# A familiar entry point for newcomers: every target runs a script of bin/, which holds the
# logic and caches by content (Make tracks no dependencies; all targets are phony). Keep it in
# step with bin/: a target's line names the script and options it runs, and `make check-makefile`
# (in bin/gate) checks that every script named here exists. The environment passes through
# (ARBACE_GO_ARCHES, ARBACE_BOX_ARCHES, ARBACE_GOROOT, ARBACE_BOX_STALE_OK, ...); see each script's
# header for its own.
# `make help` lists the targets from their `##` comments.

.DEFAULT_GOAL := help
SHELL := /bin/bash
CLOJURE_TESTS_GO ?= target/arbace-go/amd64/arbace-tests

.PHONY: help seed-check stage0 seed-verify reseed build build-tests build-java smoke repl fixpoint goi-table goi-table-size \
  box box-smoke box-oracle box-suite box-goi box-tools box-rebuild box-convert mac-bundle mac-bundle-convert arm64-vm-check arm64-vm-goi \
  program programs-check programs-essential gate gate-full gate-full-parallel gate-plan gate-costs gate-why gate-audit gate-prune gate-queue corpus-dedup cover-build gate-clean gate-full-clean gate-full-clean-only gate-clean-plan gate-pins gate-full-concurrent gate-full-clean-lanes gate-lanes-plan gate-mem-check gate-demos java-check-judge provenance-check reap crops-score suite oracle diffcheck generated-check gate-tests verify jdk-verify edge-verify javac-verify javac-classes fixtures-verify tamago-verify \
  toolchain-verify box-vendor-verify x509roots-verify repr-go-roundtrip repr-go-roundtrip-text repr-go-native-check \
  repr-java-check repr-java-roundtrip repr-java-text repr-java-jdk repr-java-parse-check repr-java-front-check repr-java-attr-check repr-java-nojavac-check repr-java-closure-check repr-java-platform-pack repr-java-platform-check repr-clojure-check repr-clojure-roundtrip repr-haskell-roundtrip repr-haskell-iface repr-haskell-text repr-haskell-layout repr-haskell-test repr-rust-roundtrip repr-rust-iface repr-rust-text repr-rust-pretty repr-rust-behaviour repr-rust-code repr-rust-all repr-rust-test repr-cpp-host-record repr-cpp-host-check repr-cpp-pins-check repr-cpp-cxxdump-build repr-cpp-cxxdump-test repr-cpp-spec-check repr-cpp-c2j-check repr-scheme-c2j-check repr-ocaml-c2j-check repr-cpp-pp-check repr-cpp-pp-record repr-cpp-cxxdump-check repr-cpp-roundtrip repr-cpp-roundtrip-corpus repr-cpp-pp-essential repr-cpp-roundtrip-essential repr-cpp-pp-record-results repr-llvm-roundtrip cppi-lower-check cppi-lower-corpus cppi-lower-essential cpp-essential cppi-runtime-build cppi-testsuite cppi-selftest cppi-run cppi-runtime-ci cppi-record-ref cppi-check cppi-check-aot cppi-divergences cppi-divergences-aot repr-c-host-record repr-c-host-check repr-c-ccdump repr-c-check repr-c-record repr-c-parse-check repr-c-read repr-c-print repr-c-roundtrip repr-c-corpus-dedup repr-c-record-gcc repr-c-c23 ci-diagnose ci-run ci-check ci-check-aot ci-dg ci-dg-essential ci-judge-test ci-c2j-check ci-cinterp-check ci-aot-translate ci-repl-session ci-divergences ci-musl ci-runtime ci-cirun-build ci-annot ci-annotations ci-annotations-record ci-record-ref ci-synth ci-synth-check goi-annotations goi-test goi-check goi-check-aot goi-check-aot-essential goi-check-nethttp goi-check-nethttp-arbace goi-divergences goi-divergences-aot java-check java-check-compiled java-check-compiled-essential java-check-print java-check-nread java-check-diagnostics java-check-essential java-check-ref alpine-check alpine-test clean \
  java-check-oracle java-check-rejudge java-verdicts \
  rsi-samples rsi-lower-check rsi-lower-check-all rsi-lower-check-native-all rsi-std-build rsi-runtime-build rsi-runtime-ci rsi-aot-lib ci-aot-lib rsi-record-ref rsi-check rsi-check-aot rsi-check-all rsi-divergences rsi-synthetic rsi-synthetic-check rsi-judge-test rsi-check-essential \
  repr-llvm-record-opt repr-llvm-record-rt llvmi-ir-make llvmi-run llvmi-check llvmi-check-aot llvmi-record-ref llvmi-divergences cppi-codegen-check \
  repr-llvm-check repr-llvm-check-essential llvm-controls-test llvmi-check-essential \
  repr-rust-native-check \
  repr-rust-native-errors \
  rust-front-corpus rust-front-mir-check rust-front-codegen-check rust-front-coverage rust-front-collect rust-front-run-native rust-front-oracles rust-front-expand-check rust-front-resolve-check rust-front-hir-check rust-front-collect-check rust-front-typeck-check \
  suite-aot suite-aot-essential suite-divergences java-forms-verify java-forms-update reap-record-native-check \
  repr-c-gcc-tests repr-scheme-fetch repr-scheme-check repr-scheme-check-quick repr-scheme-record repr-scheme-spec repr-scheme-roundtrip si-vendor-verify si-boot si-boot-check si-test si-test-record si-check-suite si-fixpoint si-unit si-light si-test-si si-check si-check-record-ref si-srfi si-srfi-record si-srfi-aot si-aot-check si-aot-check-record si-bench si-divergences si-r7rs si-r7rs-record si-r7rs-chez si-r7rs-chez-record si-r7rs-chibi si-r7rs-chibi-record si-r7rs-chibi-test si-r7rs-chibi-test-record si-chibi-tests si-chibi-tests-record si-ecraven si-ecraven-record si-ecraven-chez si-ecraven-chez-record si-ecraven-chibi si-ecraven-chibi-record si-mats si-mats-record si-corpora-files si-corpora-divergences repr-ocaml-ocdump repr-ocaml-spec-check repr-ocaml-samples repr-ocaml-corpus repr-ocaml-corpus-count repr-ocaml-roundtrip repr-ocaml-roundtrip-all repr-ocaml-check repr-ocaml-text repr-ocaml-cmi repr-ocaml-dumps oi-stdlib oi-build oi-check oi-divergences oi-record-ref oi-repl oi-aot-lib oi-check-aot oi-divergences-aot oi-suite-check oi-suite-check-aot oi-suite-divergences oi-suite-expect oi-suite-rollup oi-suite-record-ref repr-ocaml-opam repr-python-fetch repr-python-pins repr-python-count repr-python-spec-check repr-python-samples repr-python-roundtrip repr-python-record repr-python-check repr-python-text repr-python-text-record pyi-gen pyi-gen-check pyi-record-ref pyi-record-ref-check pyi-scope-check pyi-compile-check pyi-run pyi-repl pyi-repl-check pyi-lib-test pyi-lib-test-oracle pyi-runtime-print pyi-runtime-read pyi-runtime-vet pyi-runtime-check \
  repr-scheme repr-scheme-roundtrip-record si-essential si-aot-essential scheme-read-essential scheme-essential \
  repr-python-lex-check repr-python-lex-record repr-python-pegen repr-python-pegen-check repr-python-parse-check repr-python-parse-record repr-python-ast-check repr-python-records-record repr-python-records-verify pyi-lib-test-record pyi-runtime-tables pyi-aot pyi-aot-check pyi-aot-check-record pyi-bench pyi-bench-record pyi-check pyi-check-record pyi-divergences pyi-cover pyi-c2j-check pyi-table-check pyi-clib-build pyi-clib-verify python-essential \
  oi-check-compiled check-makefile \
  repr-ocaml-native-check repr-ocaml-menhir \
  oi-typer-check oi-stdlib-check oi-check-native oi-typer-probe oi-repl-check-native repr-ocaml-types oi-judges-fixtures \
  oi-check-ocdump oi-own-verdicts \
  repr-haskell-native-check repr-haskell-tables-check repr-haskell-rename-check repr-haskell-typecheck-check repr-haskell-stg-check repr-haskell-run-check repr-haskell-run-check-nofib hsi-pins hsi-spec-check hsi-tests hsi-dumps hsi-generate hsi-record-ref hsi-build hsi-stubs-build hsi-repl-session hsi-check hsi-check-core hsi-check-essential hsi-check-aot hsi-check-aot-o1 hsi-record-ref-o1 hsi-primops hsi-libm-check hsi-runtime-check repr-haskell-test-oracle repr-haskell-record-corpus hsi-record-libs hsi-record-progs oracle-records-list oracle-records-check oracle-records-verify oracle-records-record oracle-records-mirror

help: ## list the targets
	@awk 'BEGIN {FS = ":.*## "} /^## / {printf "\n%s\n", substr($$0, 4)} \
	  /^[a-z0-9-]+:.*## / {printf "  %-22s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

## The bootstrap: the seed (seed/, bin/seed)
seed-check: ## the seed has its pinned hash (bin/seed --check)
	bin/seed --check
stage0: ## stage 0 built from the seed by Go's toolchain alone (bin/seed --build)
	bin/seed --build
seed-verify: ## the seed printed again from its commit; stage 0 = G1 = G2 = G3 (bin/seed --verify)
	bin/seed --verify
reseed: ## the fixpoint, then the seed packed from its G3 (rare; commit the result)
	bin/arbace-go --fixpoint
	ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3 bin/seed --make

## Arbace: the Go build (bin/arbace-go)
build: ## the executables, amd64 and arm64 (bin/arbace-go --build)
	bin/arbace-go --build
build-tests: ## the test build with the suite's Java fixtures (bin/arbace-go --build-tests)
	bin/arbace-go --build-tests
build-java: ## javac's tool build: the program with javac's forms, jdk/javac (bin/arbace-go --build-java)
	bin/arbace-go --build-java
smoke: ## the smoke test (bin/arbace-go --smoke)
	bin/arbace-go --smoke
repl: ## Arbace's REPL (bin/arbace-go, built first when needed)
	bin/arbace-go
fixpoint: ## G1 by stage 0, G2 by G1, G3 by G2; G2 = G3 required (bin/arbace-go --fixpoint)
	bin/arbace-go --fixpoint
program: ## a user's program of DIR=...: its namespaces (.ae, and .clj, .cljc translated by repr/clojure), Java forms and Go forms (compiled through its own goi table) built into one executable (bin/arbace-go --build-program DIR)
	bin/arbace-go --build-program $(DIR)
programs-check: ## the samples of test/programs (Clojure, Java forms, Go forms, mixed) built and run against their runs, as bin/gate --full (bin/arbace-go --build-program --check)
	bin/arbace-go --build-program --check
programs-essential: ## the mixed sample and the one that must not build, as bin/gate runs them (bin/arbace-go --build-program --check mixed bad-go)
	bin/arbace-go --build-program --check mixed bad-go
goi-table: ## goi's table of compiled packages and its digests (bin/goi table; the build runs it)
	bin/goi table
goi-table-size: ## the table's size in the executable, the invokers' part (bin/goi table --size; after `make build`)
	bin/goi table --size

## The box: Arbace as a TamaGo guest (bin/arbace-go --box, bin/box-*)
box: ## the guest images and the launcher arbace-box (bin/arbace-go --build --box)
	bin/arbace-go --build --box
box-smoke: ## the box's smoke test on /dev/kvm (bin/arbace-go --smoke --box)
	bin/arbace-go --smoke --box
box-oracle: ## the oracle's subset in the box, against its known mismatches (parked: not in bin/gate --full since 2026-10-26)
	ARBACE_BOX_NET=loopback bin/oracle check 'target/arbace-go/box-amd64/arbace-box -' -j 8 \
	  --timeout 900 --exclude forms/files. --exclude forms/processes. \
	  --expected test/oracle/known-go-box-amd64.edn
box-suite: ## Clojure's suite's subset in the box (bin/box-suite; after `make suite`)
	bin/box-suite
box-goi: ## goi's check in the box: test/goi/box.txt's programs in the guest against gc (bin/goi check --box, parked: not in bin/gate --full since 2026-10-26)
	bin/goi check --box
box-tools: ## the tool guests (gc, the linker, the assembler) from tamago/cmd (bin/box-tools build)
	bin/box-tools build
box-rebuild: ## Arbace rebuilt inside the box, compared byte for byte; stops on stale executables (bin/box-rebuild)
	bin/box-rebuild
box-convert: ## tamago/goroot converted inside the box by repr/go's native reader, = the committed forms (bin/box-convert)
	bin/box-convert
mac-bundle: ## the Mac's launcher, guest and checklist, all HEAD's (bin/box-mac-bundle; after box-rebuild for arm64)
	bin/box-mac-bundle
mac-bundle-convert: ## the Mac's bundle of the conversion alone, ./mac.sh convert (bin/box-mac-bundle --convert; after box-convert --arch arm64 prepare host)
	bin/box-mac-bundle --convert
arm64-vm-check: ## the arm64 box in an emulated aarch64 VM: smoke, hv (bin/box-arm64-vm check)
	bin/box-arm64-vm check
arm64-vm-goi: ## goi's check in the arm64 box in the emulated aarch64 VM: every second program of test/goi/box.txt, about 2 hours (bin/box-arm64-vm goi)
	bin/box-arm64-vm goi

## Checks
gate: ## the essential gate, for every change to main; a check that passed at its key is skipped (bin/gate)
	bin/gate
gate-full: ## the full gate, one step after another, at milestones (bin/gate --full)
	bin/gate --full
gate-full-parallel: ## the full gate's chains concurrently (GATE_FULL_PARALLEL=1 bin/gate --full)
	GATE_FULL_PARALLEL=1 bin/gate --full
gate-plan: ## the full gate's plan: each step run, skipped, built or not needed, and why (bin/gate --full --plan)
	bin/gate --full --plan
gate-costs: ## each step's cost from the store of passes (the last 5: wall, CPU, peak), per crop and tier against steps.edn's :budgets (bin/gate --full --costs)
	bin/gate --full --costs
gate-why: ## why STEP=... runs: its key's inputs, its passes, what changed since the latest (bin/gate --why STEP)
	bin/gate --why $(STEP)
gate-audit: ## audit STEP=... (every check without): the files a step reads under strace against its declared inputs in test/gate/steps.edn; an undeclared read fails (bin/gate --audit)
	bin/gate --audit $(STEP)
gate-prune: ## the gate's store of passes and the executables trimmed: the last 5 passes per step and the last 30 days kept, the newest 3 executables' directories (bin/gate --prune; DRY=--dry-run)
	bin/gate --prune $(DRY)
gate-clean: ## the essential gate clean: a fresh clone of HEAD's commit, empty caches, stage 0 and the executables built fresh, every step audited, its record test/gate/clean-TAG.edn (bin/gate --clean; TAG=...)
	bin/gate --clean $(if $(TAG),--tag $(TAG))
gate-full-clean: ## the clean run at a tag: bin/gate --full --clean (all parts, every step audited, Java's levels on the read's cache forced; hours; TAG=...)
	bin/gate --full --clean $(if $(TAG),--tag $(TAG))
gate-full-clean-only: ## the clean run's rerun of some steps, merged into its record: bin/gate --full --clean --only ONLY (default failed,changed; TAG=...)
	bin/gate --full --clean --only "$${ONLY:-failed,changed}" $(if $(TAG),--tag $(TAG))
gate-clean-plan: ## the clean --full run's plan: every step, every part, an empty store (bin/gate --full --clean --plan)
	bin/gate --full --clean --plan
gate-pins: ## what a clean run takes from outside its clone, checked by its pins: TamaGo's go, jdk26u and its platform, the system JDK, the frozen jar (bin/gate --pins)
	bin/gate --pins
gate-full-concurrent: ## the full gate's chains at once, the big lock held once for the run, each step admitted by its declared memory in two lanes (GTS227 (a); bin/gate --full --concurrent)
	bin/gate --full --concurrent
gate-full-clean-lanes: ## the clean run at a tag in the memory lanes: the chains at once, the big steps one at a time, the rest beside (GTS227 (a); bin/gate --full --clean --concurrent; TAG=...)
	bin/gate --full --clean --concurrent $(if $(TAG),--tag $(TAG))
gate-lanes-plan: ## the clean run's plan in the lanes, each step's lane and a simulated schedule (bin/gate --full --clean --concurrent --plan)
	bin/gate --full --clean --concurrent --plan
gate-mem-check: ## every step's declared :mem-gib against the peaks recorded in the store and the clean records (bin/gate --full --mem-check)
	bin/gate --full --mem-check
gate-demos: ## GTS42 (a): a classes file and a j2g comment changed in a scratch worktree, the full plan of each (after the builds it needs), reverted (bin/gate-demos; RECORD=test/gate/clean-TAG.edn)
	bin/gate-demos $(if $(RECORD),--record $(RECORD))
gate-queue: ## the host's big-job lock's queue: the tickets of bin/heavy-lock waiting in turn, oldest first (FIFO in front of the flock, GT5; bin/heavy-lock --queue)
	bin/heavy-lock --queue
cover-build: ## GT8.3: the cover build, the gate's executable and test build with Go's coverage of the crops' code under check, kept in ARBACE_GATE_EXES/cover-KEY20 (bin/cover build; heavy: run under the host's lock)
	bin/cover build
provenance-check: ## the provenance files' repr/java hash equals the reader's (bin/provenance-check; the gate's essential step, seconds)
	bin/provenance-check
oracle-records-list: ## the oracles' records and packs and their state here (bin/oracle-records list; doc/ORACLE-RECORDS-SURVEY.md)
	bin/oracle-records list
oracle-records-check: ## every pinned pack in the store, its members as its manifest says, and the tampered-pack control (bin/oracle-records check)
	bin/oracle-records check
oracle-records-verify: ## a crop's records against its live toolchain, CROP=c [KIND=...] [CORPUS=...] [SAMPLE=N] (bin/oracle-records verify; a moved toolchain reported, not failed)
	bin/oracle-records verify $(CROP) $(KIND) $(if $(CORPUS),--corpus $(CORPUS)) $(if $(SAMPLE),--sample $(SAMPLE))
oracle-records-record: ## a crop's kind recorded by hand on the host with its toolchain, CROP=c KIND=... [CORPUS=...] (bin/oracle-records record)
	bin/oracle-records record $(CROP) $(KIND) $(if $(CORPUS),--corpus $(CORPUS))
oracle-records-mirror: ## the packs' mirror commands, the orphan branch oracle-packs for the Mac's bare repository (bin/oracle-records mirror; PUSH=1 pushes)
	bin/oracle-records mirror $(if $(PUSH),--push)
reap: ## the REAP matrix: read, eval, AOT, print for Clojure, Java, Go, C, C++, Scheme, OCaml, Python, LLVM IR, Rust, Haskell from the checks' recorded results; every non-pass in a class, by design or deferred (bin/reap; no runs, seconds)
	bin/reap
crops-score: ## each crop's completeness, maturity and independence as percentages, from the recorded results (bin/crops-score [ARGS="--newest|--ref REF|--crop C|--tsv"]; doc/CROPS-SCORE.md; no runs, seconds)
	bin/crops-score $(ARGS)
reap-record-native-check: ## Go's read level's numbers, test/repr/go/native-check-results.edn, from bin/repr-go native-check's report (bin/reap record-native-check [REPORT=...])
	bin/reap record-native-check $(REPORT)
suite: ## Clojure's suite on the test build (bin/clojure-tests, CLOJURE_TESTS_GO)
	CLOJURE_TESTS_GO=$(CLOJURE_TESTS_GO) bin/clojure-tests
suite-aot: ## Clojure's suite compiled by c2j into its own program, against its reference and the eval level's (bin/clojure-tests --aot; heavy: j2g's whole-program run)
	bin/clojure-tests --aot
suite-aot-essential: ## its essential subset, as bin/gate runs it: test/clojure/essential-aot.txt's namespaces compiled alone into their own program (bin/clojure-tests --aot --essential -j 8)
	bin/clojure-tests --aot --essential -j 8
suite-divergences: ## the suite's results judged per test against the Clojure classes (bin/clojure-tests --divergences [RESULTS=...])
	bin/clojure-tests --divergences $(RESULTS)
oracle: ## the executable against the frozen JVM's results (bin/oracle check)
	bin/oracle check 'target/arbace-go/amd64/arbace -' --timeout 900 \
	  --expected test/oracle/known-go-amd64-frozen.edn

diffcheck: ## each language's two halves on the same forms: evaluated against compiled, interpreted against j2g (bin/diffcheck)
	bin/diffcheck
generated-check: ## the generated files as their generators write them: arbace/lang/go's variants (CompilerNodes.ae, CompilerAOTFn.ae) and repr/go's Go forms in go/arbace/repr/go (the dumper, gc's syntax renamed) and goi's node factories (go/arbace/goi/ops.ae, registry.ae) and jrt's module table (bin/generated-check; --update rewrites them)
	bin/generated-check

corpus-dedup: ## the corpora's duplicates (GT8): every corpus a gate step reads, byte-identical and normalized by each language's reader, per corpus, across corpora and per check, the first copy kept (bin/corpus-dedup --normalize; CROP=c,go,java,clojure)
	bin/corpus-dedup --normalize $(if $(CROP),--crop $(CROP))
gate-tests: ## the gate's own scripts' tests, test/gate/*-test.bash (bin/gate-tests; the essential gate's step gate-tests)
	bin/gate-tests

## Vendored trees and recorded data, converted or recorded afresh and compared byte for byte
VERIFY := jdk-verify edge-verify javac-verify javac-classes repr-java-check repr-java-parse-check repr-java-front-check repr-java-attr-check repr-java-nojavac-check fixtures-verify java-forms-verify tamago-verify toolchain-verify \
  box-vendor-verify x509roots-verify repr-go-roundtrip repr-go-roundtrip-text repr-java-roundtrip repr-java-text \
  repr-clojure-roundtrip
verify: $(VERIFY) ## all of the below, one after another
jdk-verify: ## jdk/'s forms by repr/java and the JDK build's generators, on javac's tool executable; the edge step on the tools' host (bin/jrt-convert verify)
	bin/jrt-convert verify
edge-verify: ## j2g's edge, on the frozen JVM (bin/j2g-edge verify)
	bin/j2g-edge verify
javac-verify: ## jdk/javac, javac's forms (and the JDK build's tools, java.xml's SAX parser) read by javac on Arbace (the fixpoint), all of jdk.compiler's generated sources generated on the tool by the build's langtools tools (FlagsEnum through javac on Arbace's javax.tools API), its edge on the frozen JVM; j2g translates javac's world (bin/javac-vendor verify, check)
	bin/javac-vendor verify
	bin/javac-vendor check
javac-classes: ## javac on Arbace (the tool build) compiles the baseline, samples and jdk corpora as the JVM's javac, byte for byte (bin/javac-vendor classes)
	bin/javac-vendor classes
repr-java-check: ## repr/java's converter checks on javac's tool executable: the baseline, the samples, the module sample (bin/repr-java check)
	bin/repr-java check
repr-java-jdk: ## jdk26u's 68 modules read by repr/java on javac's tool executable, no failures, the forms as test/repr/java/expected/jdk26u.sha256; long, not in verify (bin/repr-java check --jdk)
	bin/repr-java check --jdk
repr-java-parse-check: ## repr/java's native parser: every file of the samples, the baseline, jdk/forms' and javac's sources, jdk26u's 68 modules, mutants and jdk26u's tests parsed to javac on Arbace's trees, positions included (bin/repr-java parse-check)
	bin/repr-java parse-check
repr-java-front-check: ## repr/java's converter on the native parser's trees, javac's attribution by position: its forms equal to javac's trees' over the samples, the baseline, the jdk corpus and javac's sources, and the split measured (bin/repr-java front-check)
	bin/repr-java front-check
repr-java-attr-check: ## repr/java's native attribution provider: the scope and declarations tiers answered without javac (the platform's class files read natively), each fact equal to javac on Arbace's, the forms still javac's trees', over the samples, the baseline, the jdk corpus and javac's sources (bin/repr-java attr-check)
	bin/repr-java attr-check
repr-java-nojavac-check: ## repr/java's read without javac: every corpus read by the native parser and the native provider alone (a file that meets what only javac answers read again with javac, counted), its forms equal to javac's, file for file, with its fallbacks, time and memory (bin/repr-java nojavac-check)
	bin/repr-java nojavac-check
repr-java-closure-check: ## repr/java's read closure names no javac class, as a ratchet (test/repr/java/closure-known.edn), its clean namespaces loaded in one process; light, no javac (bin/repr-java closure-check)
	bin/repr-java closure-check
repr-java-platform-pack: ## the java.base pack for the shipped reader, jdk/platform/java.base.decls and its provenance, deterministic (JN10 part 2, JFS135 (a); bin/repr-java platform-pack)
	bin/repr-java platform-pack
repr-java-platform-check: ## the pack's proof: (parse (strip b)) = (parse b) over java.base, the pack a fresh one's, its hash the provenance's (bin/repr-java platform-check)
	bin/repr-java platform-check
fixtures-verify: ## the suite's Java fixtures, on javac's tool executable (bin/clojure-tests --java-fixtures verify)
	bin/clojure-tests --java-fixtures verify
tamago-verify: ## tamago/goroot and goos; the guest from them (bin/tamago-vendor verify, check)
	bin/tamago-vendor verify
	bin/tamago-vendor check
toolchain-verify: ## tamago/cmd, Go's toolchain; the tools from it (bin/toolchain-vendor verify, check)
	bin/toolchain-vendor verify
	bin/toolchain-vendor check
box-vendor-verify: ## box/'s forms and KVM tests (bin/box-vendor roundtrip, kvm-tests, purego-check)
	bin/box-vendor roundtrip
	bin/box-vendor kvm-tests
	bin/box-vendor purego-check
x509roots-verify: ## x509roots/, Mozilla's roots (bin/x509roots-vendor verify, check)
	bin/x509roots-vendor verify
	bin/x509roots-vendor check
repr-go-roundtrip: ## Go -> forms -> Go over std, GOROOT/test and its directory tests (bin/repr-go roundtrip [--dirs])
	bin/repr-go roundtrip amd64
	bin/repr-go roundtrip linux/amd64
	bin/repr-go roundtrip --dirs amd64
	bin/repr-go roundtrip --dirs linux/amd64
repr-go-roundtrip-text: ## mode :full prints the original text back, byte for byte (bin/repr-go roundtrip --text [--dirs])
	bin/repr-go roundtrip --text amd64
	bin/repr-go roundtrip --text linux/amd64
	bin/repr-go roundtrip --text --dirs amd64
	bin/repr-go roundtrip --text --dirs linux/amd64
repr-go-native-check: ## the native reader (bin/repr-go read's default) against the helper, its oracle, over every corpus: headers and trees, raw forms, the checker's annotations, the forms byte for byte (bin/repr-go native-check)
	bin/repr-go native-check
repr-java-roundtrip: ## Java -> forms -> Java -> forms over Clojure's Java, the samples, jdk/forms (bin/repr-java roundtrip)
	bin/repr-java roundtrip --corpus baseline,samples,jdk
repr-java-text: ## the same in mode :full: each Java file printed back byte for byte, javac's classes compared (bin/repr-java roundtrip --text-only)
	bin/repr-java roundtrip --corpus baseline,samples,jdk --text-only
repr-clojure-check: ## repr/clojure's unit tests; Clojure's suite translated against its digest and printed back (bin/repr-clojure check [--suite])
	bin/repr-clojure check
	bin/repr-clojure check --suite
repr-clojure-roundtrip: ## Clojure -> Arbace -> Clojure over the frozen clojure/, the suite, its libraries (.cljc back whole with --full); Arbace -> Clojure -> Arbace over arbace/ (bin/repr-clojure check --roundtrip)
	bin/repr-clojure check --roundtrip
repr-haskell-roundtrip: ## Haskell -> forms -> Haskell over GHC 9.14.1's boot libraries (41 packages, their .hsc, boot and Setup.lhs files): GHC's trees and lines (bin/repr-haskell roundtrip)
	bin/repr-haskell roundtrip
repr-haskell-iface: ## the same, then GHC's interfaces of the originals and the printed modules compiled, compared (bin/repr-haskell roundtrip --iface)
	bin/repr-haskell roundtrip --iface
repr-haskell-text: ## the same in mode :full: each module printed back byte for byte, GHC's trees of mode :full compared (bin/repr-haskell roundtrip --text-only)
	bin/repr-haskell roundtrip --text-only
repr-haskell-layout: ## the same forms printed by Haskell's layout rule, as a person writes them: GHC's trees and lines, then interfaces (bin/repr-haskell roundtrip --layout-only --iface)
	bin/repr-haskell roundtrip --layout-only --iface
repr-haskell-test: ## repr/haskell's cases on the records, no GHC: the reader's samples, the spec's worked example, mode :full, the layout layout, R5's and R6's cases (bin/repr-haskell test)
	bin/repr-haskell test
repr-haskell-native-check: ## the native reader (GHC 9.14.1's lexer, grammar and PostProcess in Arbace) against the helper over test/haskell/native-check.txt: trees, pragmas, forms (bin/repr-haskell native-check)
	bin/repr-haskell native-check
repr-haskell-tables-check: ## the native parser's LALR(1) tables generated from GHC's Parser.y against Happy's in the tarball's Parser.hs (bin/repr-haskell tables --check)
	bin/repr-haskell tables --check
repr-haskell-rename-check: ## the front end's renamer (HF2) against GHC's: the rn dump over test/haskell/rename-check.txt (samples, ghc-internal modules, nofib programs) (bin/repr-haskell rename-check)
	bin/repr-haskell rename-check
repr-haskell-typecheck-check: ## the front end's elaborator (HF3) against GHC's types and desugared Core's evidence over test/haskell/typecheck's samples, with its negative control (bin/repr-haskell typecheck-check)
	bin/repr-haskell typecheck-check
repr-haskell-stg-check: ## HF5-HF6: the match compiler, the -O0 Simplifier, CorePrep and CoreToStg; desugared and simplified Core and STG against GHC's -O0, test/haskell/stg's programs run on hsi from Arbace's STG against GHC's output (bin/repr-haskell stg-check)
	bin/repr-haskell stg-check
repr-haskell-run-check: ## HF7's first light: the run corpus (test/haskell/eval's samples and synthetic programs) compiled by Arbace's front end alone and run on hsi, against GHC's recorded runs and test/haskell/run-check.edn (bin/repr-haskell run-check)
	bin/repr-haskell run-check
repr-haskell-run-check-nofib: ## HF3's second slice measured: the single-module nofib programs of test/haskell/run-check-nofib.txt by Arbace's front end on hsi, against test/haskell/ref-nofib.edn and test/haskell/run-check-nofib.edn (bin/repr-haskell run-check --nofib)
	bin/repr-haskell run-check --nofib
repr-haskell-test-oracle: ## the same cases with GHC reading the printed modules (class D; the oracle tier's haskell-test-oracle; bin/repr-haskell test --oracle)
	bin/repr-haskell test --oracle
repr-haskell-record-corpus: ## the corpus's originals dumped and parsed by the helper into the four packs, by hand (bin/repr-haskell record-corpus; REPR_HASKELL_ONLY_PACKAGES="P Q" some packages)
	bin/repr-haskell record-corpus
hsi-pins: ## Haskell's eval corpora: the testsuite tarball and nofib fetched by their pins, every corpus's measures against test/haskell/eval-corpora.edn (bin/hsi pins --check)
	bin/hsi pins --check
hsi-spec-check: ## doc/haskell/STG-SPEC.md's checklist against GHC 9.14.1's STG constructors (bin/hsi spec-check)
	bin/hsi spec-check
hsi-tests: ## the testsuite's all.T files read, its run tests listed in test/haskell/eval/testsuite-run.edn (bin/hsi tests)
	bin/hsi tests
hsi-dumps: ## the boot libraries and the samples compiled to STG forms by the helper, ABI hashes against the installed, read back, twice (bin/hsi dumps --samples --again; heavy: run by night)
	bin/hsi dumps --samples --again
hsi-record-libs: ## the boot libraries' STG dumped and packed into haskell-stg-libs, by hand (bin/hsi dumps --record; heavy: run by night)
	bin/hsi dumps --record
hsi-record-progs: ## the run corpora's programs' STG dumped by the helper and packed into haskell-stg-progs, by hand (bin/hsi check --live --dump-only --record-progs)
	bin/hsi check --live --dump-only --record-progs --corpus samples,synthetic,testsuite-core
hsi-generate: ## the synthetic corpus test/haskell/eval/synthetic/ against its generator (bin/hsi generate --check)
	bin/hsi generate --check
hsi-record-ref: ## GHC 9.14.1's -O0 runs of the run corpora into test/haskell/ref-ghc914.edn (bin/hsi record-ref)
	bin/hsi record-ref
hsi-build: ## hsi's light driver: go/arbace/hs printed by repr/go and built by TamaGo's go into .tmp/hsi/bin/hsi (bin/hsi build)
	bin/hsi build
hsi-check: ## the samples and the synthetic corpus on hsi against test/haskell/ref-ghc914.edn, every non-pass in a class (bin/hsi check)
	bin/hsi check

hsi-check-core: ## the samples, the synthetic corpus and the testsuite's language core (701 run tests) on hsi, about 4 minutes (bin/hsi check --corpus samples,synthetic,testsuite-core)
	bin/hsi check --corpus samples,synthetic,testsuite-core

hsi-check-essential: ## GT8's list of Haskell's eval, test/haskell/hsi/essential.txt, on hsi (bin/hsi check --only @test/haskell/hsi/essential.txt)
	bin/hsi check --corpus samples,synthetic,testsuite-core --only @test/haskell/hsi/essential.txt

hsi-check-aot: ## HE8's AOT: the samples and the synthetic corpus compiled to Go over hsi's machine and run against test/haskell/ref-ghc914.edn and against hsi (bin/hsi check --aot)
	bin/hsi check --aot

hsi-check-aot-o1: ## HES74 (c): the essential list compiled from GHC's -O1 STG against GHC's -O1 runs, test/haskell/ref-ghc914-O1.edn (bin/hsi check --aot --opt O1)
	bin/hsi check --aot --opt O1 --corpus samples,synthetic,testsuite-core --only @test/haskell/hsi/essential.txt

hsi-record-ref-o1: ## GHC 9.14.1's -O1 runs of the run corpora into test/haskell/ref-ghc914-O1.edn, the compiled level's reference at -O1 (bin/hsi record-ref --opt O1)
	bin/hsi record-ref --opt O1

hsi-stubs-build: ## HES60's probe and stub writer (tools/hsdump/probe/StubProbe.hs) built by the host's ghc into .tmp/hsi/stubprobe (bin/hsi stubs-build)
	bin/hsi stubs-build

hsi-repl-session: ## HE7's REPL: test/haskell/repl/'s sessions through bin/hsi repl, stdout byte for byte against GHCi's recorded transcripts, and the negative control (bin/hsi repl-session)
	bin/hsi repl-session

hsi-primops: ## hsi's primops against GHC.Prim's 742 non-SIMD names (bin/hsi primops)
	bin/hsi primops

hsi-libm-check: ## hsi's port of musl's libm against the host's musl, bit for bit, function by function, and its negative control (bin/hsi libm-check)
	bin/hsi libm-check

hsi-runtime-check: ## hsi's runtime (go/arbace/hs, go/arbace/libm, the driver) printed from its :full Go forms and read again: the same forms (bin/hsi runtime check)
	bin/hsi runtime check
repr-rust-roundtrip: ## Rust -> forms -> Rust over std's five crates of rustc 1.99.1: rustc's trees and lines (bin/repr-rust roundtrip)
	bin/repr-rust roundtrip
repr-rust-iface: ## the same, then rustdoc's JSON of the five crates from the originals and from the printed files, compared without spans (bin/repr-rust roundtrip --iface)
	bin/repr-rust roundtrip --iface
repr-rust-text: ## std's five crates in mode :full, printed back to the original text byte for byte, and their trees (bin/repr-rust roundtrip --text-only)
	bin/repr-rust roundtrip --text-only
repr-rust-pretty: ## std's five crates' forms without positions printed in the pretty layout, their trees (bin/repr-rust roundtrip --pretty-only)
	bin/repr-rust roundtrip --pretty-only
repr-rust-behaviour: ## rustc's UI suite's run-pass programs run from their originals and their printed files in every layout, and against std built from its printed files (bin/repr-rust roundtrip --text --pretty --run --std rustc-1.99.1-ui)
	bin/repr-rust roundtrip --text --pretty --run --std rustc-1.99.1-ui
repr-rust-code: ## the lowered and code levels: std's five crates' HIR, MIR and objects built from the originals and from the printed files of every layout (bin/repr-rust roundtrip --text --pretty --code)
	bin/repr-rust roundtrip --text --pretty --code
repr-rust-all: ## every level over all five corpora: std, the rest of library/, the compiler, the pinned crate set, rustc's UI suite; then the behaviour level, the lowered and code levels (bin/repr-rust roundtrip --iface --text --pretty --run --code --std all)
	bin/repr-rust roundtrip --iface --text --pretty --run --code --std all
repr-rust-test: ## repr/rust's cases: the reader's samples, the spec's worked example and its round trip, the interface level's cases, mode :full's, the behaviour and code levels' (bin/repr-rust test)
	bin/repr-rust test
repr-rust-native-check: ## the native reader (rustc's lexer and parser ported to Arbace) against the helper: the samples' and a few dozen std files' dumps equal (bin/repr-rust native-check)
	bin/repr-rust native-check
repr-rust-native-errors: ## the native reader's first error of each file against rustc's (bin/repr-rust native-errors [--edition E] FILES=...)
	bin/repr-rust native-errors $(FILES)
repr-cpp-host-record: ## record g++ 15.2's and clang 23.1.4's answers under test/cpp/host/: predefined macros per standard, search lists, __has_* answers, sizes, libstdc++'s header digests (bin/repr-cpp host record)
	bin/repr-cpp host record
repr-cpp-host-check: ## the host's compilers still give test/cpp/host/'s answers and libstdc++'s headers their digests (bin/repr-cpp host check)
	bin/repr-cpp host check
repr-cpp-pins-check: ## test/cpp/corpora.edn's pins against the tarballs and compilers on the host (bin/repr-cpp pins check)
	bin/repr-cpp pins check
repr-cpp-cxxdump-build: ## build the oracle's filter tools/cxxdump into .tmp/repr-cpp/cxxdump (bin/repr-cpp cxxdump-build)
	bin/repr-cpp cxxdump-build
repr-cpp-cxxdump-test: ## go test of tools/cxxdump (bin/repr-cpp cxxdump-test)
	bin/repr-cpp cxxdump-test
repr-cpp-spec-check: ## every forms block of doc/cpp/SPEC.md reads with Arbace's reader (bin/repr-cpp spec-check)
	bin/repr-cpp spec-check

repr-cpp-c2j-check: ## repr/cpp's, repr/llvm's and the lowering's namespaces compiled by c2j alone, as the re-seed's build will compile them (the essential step cpp-c2j; bin/repr-cpp c2j-check)
	bin/repr-cpp c2j-check
repr-cpp-pp-check: ## repr/cpp's token check: the corpora's files lexed and preprocessed in C++ mode, judged by g++ -E's and clang's recorded answers (bin/repr-cpp pp-check; CORPUS=a,b SAMPLE=N)
	bin/repr-cpp pp-check $(if $(CORPUS),--corpus $(CORPUS)) $(if $(SAMPLE),--sample $(SAMPLE))
repr-cpp-pp-record: ## g++ -E and clang's raw tokens run on the corpora's files, their digests and the host's option deltas rewritten (bin/repr-cpp pp-check --record --add-missing; CORPUS=a,b SAMPLE=N)
	bin/repr-cpp pp-check --record --add-missing $(if $(CORPUS),--corpus $(CORPUS)) $(if $(SAMPLE),--sample $(SAMPLE))
repr-cpp-cxxdump-check: ## the oracle's filter over a list of files, its numbers (bin/repr-cpp cxxdump-check LIST OUT; LIST=... OUT=...)
	bin/repr-cpp cxxdump-check $(LIST) $(OUT) --layouts
repr-cpp-roundtrip: ## repr/cpp's round trip in mode :lines over a list: forms, print, forms again, the print's AST, g++'s and clang's objects (bin/repr-cpp roundtrip LIST WORK; LIST=... WORK=...)
	bin/repr-cpp roundtrip $(LIST) $(WORK)
repr-cpp-roundtrip-corpus: ## repr/cpp's round trip over a recorded list made by its key: CORPUS-every-N, CORPUS-all, CORPUS-essential, samples (bin/repr-cpp roundtrip-corpus KEY; KEY=...)
	bin/repr-cpp roundtrip-corpus $(KEY)
repr-cpp-pp-essential: ## the essential subset of C++'s token check (test/cpp/essential-read.txt: the lexical corpus, one g++.dg file of each class, three headers) (bin/repr-cpp pp-check --essential)
	bin/repr-cpp pp-check --essential
repr-cpp-roundtrip-essential: ## the essential subset of C++'s round trip (test/cpp/essential-print.txt, held to roundtrip-results/gxx-dg-essential.edn) and the samples (bin/repr-cpp roundtrip-corpus gxx-dg-essential, samples)
	bin/repr-cpp roundtrip-corpus gxx-dg-essential && bin/repr-cpp roundtrip-corpus samples
repr-cpp-pp-record-results: ## the token check's results, test/repr/cpp/pp-check-results.edn, at the recorded samplings (bin/repr-cpp pp-check --record-results, three runs)
	bin/repr-cpp pp-check --corpus cpp-lexical --record-results && bin/repr-cpp pp-check --corpus gxx-dg --sample 10 --record-results && bin/repr-cpp pp-check --corpus libstdcxx-headers --sample 8 --record-results
repr-llvm-roundtrip: ## repr/llvm's round trip over a list of IR files: forms, .ae text, print, forms again, clang -c's objects equal (bin/repr-llvm roundtrip LIST WORK; LIST=... WORK=...)
	bin/repr-llvm roundtrip $(LIST) $(WORK)
rsi-samples: ## Rust's eval, RE3: rsi's samples (test/rust/eval/) lowered with std's lowered IR and run as the originals (bin/rsi lower-check on bin/rsi list --set samples)
	bin/rsi list --set samples -o .tmp/rsi/samples.tsv && bin/rsi lower-check .tmp/rsi/samples.tsv .tmp/rsi/samples
rsi-lower-check: ## Rust's eval, RE3: every 10th UI program built from its lowered IR with the lowered std (gcc -O0) and run as the original, against test/rust/divergences-lower.edn (bin/rsi lower-check)
	bin/rsi list --sample 10 -o .tmp/rsi/sample10.tsv && bin/rsi lower-check .tmp/rsi/sample10.tsv .tmp/rsi/sample10
rsi-lower-check-all: ## the same over all 3,718 UI programs, recorded into test/rust/lower-std-results.edn (a night's run; bin/rsi lower-check --record)
	bin/rsi list -o .tmp/rsi/all.tsv && bin/rsi lower-check .tmp/rsi/all.tsv .tmp/rsi/all --record
rsi-lower-check-native-all: ## RE2's native-std leg over all 3,718 UI programs, recorded into test/rust/lower-results.edn (bin/rsi lower-check --leg native --record)
	bin/rsi list -o .tmp/rsi/all.tsv && bin/rsi lower-check .tmp/rsi/all.tsv .tmp/rsi/all-native --leg native --record
rsi-std-build: ## std's sysroot built from the pinned rust-src (library/'s workspace) with its IR, against test/rust/runtime/ir.sha256 (bin/rsi std-build)
	bin/rsi std-build
rsi-runtime-build: ## RE3: std's IR lowered, compiled by gcc -O0 and archived by crate with the pending-flag layer (bin/rsi runtime-build)
	bin/rsi runtime-build
rsi-runtime-ci: ## RE4: std's lowered units and the pending-flag layer read by repr/c as ci's library, with ci's ports (bin/rsi runtime-ci)
	bin/rsi runtime-ci
rsi-aot-lib: ## RES34: every unit of std's, musl's and the ports' libraries translated once by cf2g into its cached Go package, .tmp/ci-aot-lib/ (bin/rsi aot-lib)
	bin/rsi aot-lib
rsi-record-ref: ## RE4: the judge's runs (rustc 1.99.1 -O0 against std's build) of the samples and every 10th UI program recorded into test/rust/ref-rustc-1.99.1.edn (bin/rsi record-ref)
	bin/rsi record-ref --sample 10
rsi-check: ## RE4: the samples and every 10th UI program run on ci against the judge's runs, every non-pass in a class of test/rust/divergences.edn (bin/rsi check)
	bin/rsi check --sample 10
rsi-check-aot: ## RE4: the same compiled by cf2g (bin/rsi check --aot)
	bin/rsi check --aot --sample 10
rsi-check-all: ## RE4: every UI program on ci, recorded into test/rust/check-linux-amd64.edn with its times (a night's run; bin/rsi record-ref then bin/rsi check --record)
	bin/rsi record-ref && bin/rsi check --record
rsi-synthetic: ## RE5: the synthetic corpus's programs (test/rust/synthetic/) generated from test/rust/synthetic.edn and its templates (bin/rsi synthetic generate)
	bin/rsi synthetic generate
rsi-synthetic-check: ## RE5: the committed synthetic programs equal what the table and templates give, and their expected statuses the judge's runs (bin/rsi synthetic check, ref)
	bin/rsi synthetic check && bin/rsi synthetic ref
rsi-judge-test: ## RE5: the check's judge without ci: the judge's own runs pass, a changed output fails, the controls differ (bin/rsi judge-test)
	bin/rsi judge-test
rust-front-corpus: ## RF1: the helper's mir, census and iface modes over the synthetic corpus and the samples, the new solver's MIR against the old's (bin/rust-front corpus)
	bin/rust-front corpus
rust-front-mir-check: ## RF2: each program's MIR forms read and printed back equal to rustc's -Zunpretty=mir text (bin/rust-front mir-check)
	bin/rust-front mir-check
rust-front-codegen-check: ## RF3: the no_std samples (test/rust/front/) through arbace.rust.back.codegen, each function against rustc's IR, then the run (bin/rust-front codegen-check)
	bin/rust-front codegen-check test/rust/front/a[1-57].rs
rust-front-coverage: ## RF3: the mono items rustc collects for the synthetic corpus and the samples, codegen'd and judged against rustc's IR, the refusals ranked (bin/rust-front coverage)
	bin/rust-front coverage test/rust/synthetic/*.rs test/rust/sample/space.rs test/rust/sample/shapes.rs test/rust/eval/*.rs test/rust/front/a*.rs
rust-front-collect: ## RF3: the collector slice's item set of each program against rustc's (bin/rust-front collect)
	bin/rust-front collect test/rust/synthetic/*.rs test/rust/sample/space.rs test/rust/sample/shapes.rs test/rust/eval/*.rs test/rust/front/a*.rs
rust-front-run-native: ## RF3: the synthetic programs the front codegens completely, run from its IR with the host's std, against rustc's runs (bin/rust-front run-native)
	for n in abort exit-code panic-assert panic-todo panic-unwrap-err trap; do bin/rust-front run-native test/rust/synthetic/$$n.rs || exit 1; done
rust-front-oracles: ## RF4-RF5: the helper's expanded crates, HIR paths' resolutions and std's names and macros for the corpus and samples (bin/rust-front front-oracles)
	bin/rust-front front-oracles
rust-front-expand-check: ## RF4: arbace.rust.front.expand's crates against rustc's expansion, per item (bin/rust-front expand-check)
	bin/rust-front expand-check
rust-front-resolve-check: ## RF5: arbace.rust.front.resolve over rustc's expanded crates and over ours, against rustc's resolutions per path (bin/rust-front resolve-check)
	bin/rust-front resolve-check && bin/rust-front resolve-check --ours
rust-front-hir-check: ## RF6: arbace.rust.front.lower and hir-pretty over rustc's expanded crates and over the pipeline, against rustc's -Zunpretty=hir text (bin/rust-front hir-check)
	bin/rust-front hir-check && bin/rust-front hir-check --ours
rust-front-collect-check: ## RF7: arbace.rust.front.collect (generics, types, signatures, clauses) over both inputs against the helper's iface (bin/rust-front collect-check)
	bin/rust-front collect-check && bin/rust-front collect-check --ours
rust-front-typeck-check: ## RF9: arbace.rust.front.typeck (expressions' types and adjustments, bindings) over both inputs against the helper's typeck (bin/rust-front typeck-check)
	bin/rust-front typeck-check && bin/rust-front typeck-check --ours
rsi-check-essential: ## RE5: the essential programs (test/rust/essential.txt) on ci against the judge's runs (bin/rsi check --essential)
	bin/rsi check --essential
rsi-divergences: ## RE4: the recorded check's non-passes by class (bin/rsi divergences [--aot])
	bin/rsi divergences
cppi-lower-check: ## C++'s lowering over a list: the IR, its LLVM forms' round trip, the lowered C compiled by gcc -O0, linked with the lowered runtime and run as the original, against test/cpp/lower-known.edn (bin/cppi lower-check LIST WORK; LIST=... WORK=...)
	bin/cppi lower-check $(LIST) $(WORK)
cppi-lower-corpus: ## C++'s lowering check over a recorded list of test/cpp/lower-results.edn by its key: samples, libstdcxx-every-15th (bin/cppi lower-corpus KEY; KEY=...)
	bin/cppi lower-corpus $(KEY)
cppi-lower-essential: ## the essential subset of C++'s lowering check (test/cpp/essential-lower.txt: 6 programs, the negative control's pair) (bin/cppi lower-essential)
	bin/cppi lower-essential
cpp-essential: ## C++'s essential subsets, as the gate's chain cpp-essential runs them: tokens, round trip, lowering, run on ci and compiled (bin/repr-cpp, bin/cppi)
	bin/repr-cpp pp-check --essential && bin/repr-cpp roundtrip-corpus gxx-dg-essential && bin/repr-cpp roundtrip-corpus samples && bin/cppi lower-essential && bin/cppi check --only test/cpp/essential-check.txt && bin/cppi check --aot --only test/cpp/essential-check.txt
cppi-run: ## a C++ file compiled to IR by the oracle, lowered, built by ci with the lowered runtime and musl, run on ci (bin/cppi run FILE; FILE=...)
	bin/cppi run $(FILE)
cppi-runtime-ci: ## the lowered runtime's units read by repr/c as ci's library, .tmp/cppi/runtime/ci (bin/cppi runtime-ci)
	bin/cppi runtime-ci
cppi-record-ref: ## g++ 15.2's -O0 runs of the C++ run corpora recorded, test/cpp/ref-gxx15.edn (bin/cppi record-ref; ARGS=--corpus ...)
	bin/cppi record-ref $(ARGS)
cppi-check: ## C++'s run check on ci: the run corpora lowered and run on ci against g++'s runs, every non-pass in a class of test/cpp/divergences.edn (bin/cppi check, default samples,runtime; ARGS=--corpus gxx-dg-run --sample 25 [--record]: the sampling's own record check-every-25, --only ..., --record)
	bin/cppi check $(ARGS)
cppi-check-aot: ## C++'s AOT check: the same programs translated by cf2g and built by TamaGo's go, against g++'s runs and test/cpp/divergences-aot.edn (bin/cppi check --aot; ARGS=...)
	bin/cppi check --aot $(ARGS)
cppi-divergences: ## the recorded C++ run check judged against its classes (bin/cppi divergences)
	bin/cppi divergences
cppi-divergences-aot: ## the recorded C++ AOT check judged against its classes (bin/cppi divergences --aot)
	bin/cppi divergences --aot
repr-llvm-check: ## repr/llvm's check families against their records, the tests' own verdicts where they have one, no LLVM tool (bin/repr-llvm check FAMILY; FAMILY=roundtrip-llvmtest|text-llvmtest|verify-llvmtest|verify-probes)
	bin/repr-llvm check $(FAMILY)
repr-llvm-check-essential: ## repr/llvm's essential subsets of the four families (the gate's llvm-read-essential, llvm-print-essential)
	bin/repr-llvm check roundtrip-llvmtest --essential && bin/repr-llvm check verify-llvmtest --essential && bin/repr-llvm check verify-probes --essential && bin/repr-llvm check text-llvmtest --essential
llvm-controls-test: ## the LLVM row's negative controls wired: a control crossing a module with itself fails each check (test/repr/llvm/controls-test.bash)
	test/repr/llvm/controls-test.bash
repr-llvm-record-opt: ## opt -passes=verify's verdicts of IR lists recorded into test/llvm/ref-opt23.edn, verify-check's reference without opt (bin/repr-llvm record-opt LIST...; LISTS=...)
	bin/repr-llvm record-opt $(LISTS)
repr-llvm-record-rt: ## the round trip's judges (clang -c's objects, llvm-as's bitcode) recorded into test/llvm/ref-rt23.edn, roundtrip's reference without clang (bin/repr-llvm record-rt LIST WORK; LIST=... WORK=...)
	bin/repr-llvm record-rt $(LIST) $(WORK)
llvmi-ir-make: ## llvmi's pinned IR made afresh by clang 23 (test/llvm/ir/{tx,c}.tar.xz, their pins, manifests, commands; bin/llvmi ir-make)
	bin/llvmi ir-make
llvmi-run: ## llvmi: an IR module lowered to C and run by ci (bin/llvmi run FILE; FILE=...)
	bin/llvmi run $(FILE)
llvmi-check: ## llvmi's eval check: llvm/test's ExecutionEngine modules, C's synth and run programs, gcc's torture sample and C++'s samples as clang 23's pinned IR (test/llvm/ir), lowered and run by ci judged by the tests' own verdicts (lit's, gcc's torture harness's), else against lli 23's recorded runs; no clang or lli (bin/llvmi check)
	bin/llvmi check
llvmi-check-essential: ## llvmi's essential subset, test/llvm/essential-check.txt's ten modules with the negative controls (bin/llvmi check --essential)
	bin/llvmi check --essential
llvmi-check-aot: ## llvmi's AOT check: the same lowered C through cf2g and TamaGo's go (bin/llvmi check --aot)
	bin/llvmi check --aot
llvmi-record-ref: ## lli 23's runs of llvmi's modules recorded into test/llvm/ref-lli23.edn (bin/llvmi record-ref)
	bin/llvmi record-ref
llvmi-divergences: ## llvmi's recorded eval check judged against test/llvm/divergences-eval.edn (bin/llvmi divergences)
	bin/llvmi divergences
cppi-codegen-check: ## C++ without clang (doc/cpp/CODEGEN.md): test/cpp/codegen's samples by the native parser, arbace.cpp.sema and arbace.cpp.codegen, lowered and run against clang's IR lowered and run (bin/cppi codegen-check WORK; WORK=..., default .tmp/cppi/codegen)
	bin/cppi codegen-check $(or $(WORK),.tmp/cppi/codegen)
cppi-runtime-build: ## C++'s lowered runtime: libsupc++ and libstdc++-v3/src compiled to IR by the oracle (pinned by test/cpp/runtime/ir.sha256), lowered and archived into .tmp/cppi/runtime (bin/cppi runtime-build)
	bin/cppi runtime-build
cppi-selftest: ## the C++ checks' own tests: each check put in a situation review 007 found mishandled (bin/cppi selftest [NAME...]; test/cpp/selftest/)
	bin/cppi selftest
cppi-testsuite: ## libstdc++'s testsuite extracted from the pinned gcc tarball into .tmp/cppi/gcc (bin/cppi testsuite)
	bin/cppi testsuite
repr-c-host-record: ## record the reference's answers repr/c's preprocessor reads with under test/c/host/: gcc 15.2's predefined macros per standard, search list, __has_* answers, the headers' digests (bin/repr-c host record)
	bin/repr-c host record
repr-c-host-check: ## the host's gcc still gives test/c/host/'s answers and its headers their digests (bin/repr-c host check)
	bin/repr-c host check
repr-c-ccdump: ## build the oracle tools/ccdump against go-whim's cc at its pin into .tmp/repr-c/ccdump/ (bin/repr-c ccdump-build)
	bin/repr-c ccdump-build
repr-c-check: ## repr/c's token check: every corpus file read in place lexed and preprocessed by the port and by cc, equal or known, and judged by gcc -E's recorded digest (bin/repr-c check --tokens)
	bin/repr-c check --tokens
repr-c-record: ## gcc -E on every corpus file, its digests (test/repr/c/pp-gcc.sha256) and the token check's known file rewritten (bin/repr-c check --tokens --record)
	bin/repr-c check --tokens --record
repr-c-gcc-tests: ## gcc 15.2's tarball checked against its pin and its testsuite extracted into .tmp/repr-c/ (bin/repr-c gcc-tests)
	bin/repr-c gcc-tests
repr-c-parse-check: ## repr/c's tree check: every corpus file and 7,700 mutants parsed by the port and by cc, the trees equal node for node or known (bin/repr-c parse-check)
	bin/repr-c parse-check
repr-c-read: ## a C file's C forms, FILE=... (its .ae on the standard output; bin/repr-c read FILE)
	bin/repr-c read $(FILE)
repr-c-print: ## a C forms unit's C text, FILE=....ae (mode :lines, or the original for a unit read --full; bin/repr-c print FILE)
	bin/repr-c print $(FILE)
repr-c-roundtrip: ## repr/c's round trip: every corpus file read into C forms, printed and read again, :full byte for byte, the print's objects the original's and its runs too (bin/repr-c roundtrip)
	bin/repr-c roundtrip
repr-c-corpus-dedup: ## the C corpora collapsed by :dedup (torture-execute-mirror: its files not byte-identical to torture-execute's) recomputed and checked against test/c/corpora.edn's :files (bin/repr-c corpus-dedup --check)
	bin/repr-c corpus-dedup --check
repr-c-record-gcc: ## gcc -fsyntax-only's acceptance of every corpus file recorded (test/repr/c/parse-gcc.tsv, CS61 (a)), then the round trip (bin/repr-c roundtrip --record-gcc)
	bin/repr-c roundtrip --record-gcc
ci-diagnose: ## a C file read by repr/c and checked by ci's checker: the errors it gives, those gcc 15.2 gives, FILE=... (bin/ci diagnose FILE)
	bin/ci diagnose $(FILE)
ci-run: ## a C program read, checked, linked with musl 1.2.6's forms and run by ci, FILE=... (bin/ci run FILE)
	bin/ci run $(FILE)
ci-check: ## ci's check: every program of the C run corpora run by ci against gcc 15.2's recorded run, every non-pass classed (bin/ci check)
	bin/ci check
ci-check-aot: ## ci's AOT check: every program of the C run corpora translated by cf2g into Go, built in shards and run against gcc 15.2's run and the interpreted run (bin/ci check --aot)
	bin/ci check --aot
ci-check-essential: ## the essential subset of ci's run-level check (test/c/essential-check.txt: two passes of each corpus and one of each class), judged against the record (bin/ci check --only)
	bin/ci check --only test/c/essential-check.txt
ci-aot-essential: ## the essential subset of ci's AOT check (test/c/essential-aot.txt: six passes, a refused program, the threads class), judged against the record (bin/ci check --aot --only)
	bin/ci check --aot --only test/c/essential-aot.txt
ci-dg: ## the C23 corpus by its dg directives: gcc 15.2's c23 tests, the checker's errors judged by line and kind (bin/ci dg)
	bin/ci dg
ci-dg-essential: ## the essential subset of the C23 dg check: one file of each diagnostic family, all must pass (bin/ci dg --essential)
	bin/ci dg --essential
ci-aot-translate: ## a C program's node program translated by cf2g into Go forms printed as OUT/prog.go, NODES=... OUT=... (bin/ci aot NODES OUT)
	bin/ci aot $(NODES) $(OUT)
ci-repl-session: ## ci's REPL session (test/c/repl/letters.ae: a unit typed at the REPL, run, redefined, run again) against its output (bin/ci repl-session)
	bin/ci repl-session
ci-judge-test: ## the judges' unit tests of ci: the dg verdict, the run level's result and record comparison (bin/ci judge-test)
	bin/ci judge-test
ci-c2j-check: ## ci's and repr/c's namespaces compiled by c2j alone, as the re-seed's build compiles them (the essential step c-c2j; bin/ci c2j-check)
	bin/ci c2j-check
ci-cinterp-check: ## CInterp's Go side (go/arbace/cinterp, ci, crt) built with a stand-in jrt and run on test/c/cinterp/'s fixtures against cirun, EXE=... an executable built with ci instead (bin/ci cinterp-check [--exe EXE])
	bin/ci cinterp-check $(if $(EXE),--exe $(EXE))
ci-divergences: ## ci's recorded check judged against its classes, test/c/divergences.edn (bin/ci divergences)
	bin/ci divergences
ci-musl: ## musl 1.2.6 prepared and read into C forms for ci's libc, .tmp/ci-musl/ (bin/ci musl prepare, bin/ci musl read)
	bin/ci musl prepare && bin/ci musl read
ci-runtime: ## ci's prebuilt runtime: every musl unit checked and built once into its cached fragment, .tmp/ci-runtime/, the other keys' removed (bin/ci runtime build)
	bin/ci runtime build
ci-aot-lib: ## RES34: every musl unit translated once by cf2g into its cached Go package, .tmp/ci-aot-lib/, which check --aot links (bin/ci aot-lib build)
	bin/ci aot-lib build
ci-cirun-build: ## ci's back end (go/arbace/crt, go/arbace/ci) printed and built with tools/cirun, the light path's runner (bin/ci cirun-build)
	bin/ci cirun-build
ci-annot: ## a C file's annotations by ci's checker (types, values, layouts, linkage; tools/ccdump's types format), FILE=... (bin/ci annot FILE)
	bin/ci annot $(FILE)
ci-annotations: ## the checker's check: every corpus file checked, its annotations equal to cc's checker's or classed, no error where gcc accepts (bin/ci annotations)
	bin/ci annotations
ci-annotations-record: ## the checker's check, test/repr/c/annot-known.edn rewritten from the files that do not pass (bin/ci annotations --record)
	bin/ci annotations --record
ci-record-ref: ## gcc 15.2's runs of the single-file C run corpora recorded as data, test/c/ref-gcc15.edn: -O0 the judge, -O2 the witness (bin/ci record-ref)
	bin/ci record-ref
ci-synth: ## synth-c23's C23 programs written into test/c/synth/ from its table, test/c/synth/table.edn (bin/ci synth)
	bin/ci synth
ci-synth-check: ## synth-c23's committed programs and its corpus count against its table (bin/ci synth --check)
	bin/ci synth --check
repr-c-c23: ## C23's conformance judges over test/c/c23/ and their control (bin/repr-c check --c23)
	bin/repr-c check --c23
goi-annotations: ## goi's checker over tamago/goroot, linux std, GOROOT/test: no error, every annotation verified (bin/goi annotations)
	bin/goi annotations
goi-test: ## goi's tests: the checker's (invalid forms with gc's messages, checked trees), the interpreter's programs against gc, Go forms at the REPL (bin/goi test --gc; after `make build`)
	bin/goi test --gc
goi-check: ## goi's check: GOROOT/test's run programs interpreted against gc, its errorcheck programs' errors judged as Go's test driver does (gc's syntax errors, the raw forms of invalid programs: GP6; gc's branch checks, noder's pragmas and declaration checks: GPS41 (c)), against test/goi/check-linux-amd64.edn (bin/goi check; after `make build`)
	bin/goi check
goi-check-aot: ## Go's AOT level: goi's corpora built by arbace.build.go and run, errorcheck programs printed from mode :full and compiled by gc, against gc on the originals; test/goi/check-aot-linux-amd64.edn (bin/goi check --aot --jobs 8)
	bin/goi check --aot --jobs 8
goi-check-aot-essential: ## its essential subset, as bin/gate runs it: test/goi/essential-aot.txt's programs (bin/goi check --aot --essential --jobs 8)
	bin/goi check --aot --essential --jobs 8
goi-check-nethttp: ## the net/http corpus (NH1): test/nethttp's 66 loopback programs at the gc level and interpreted by goi against gc's runs, test/nethttp/check-linux-amd64.edn, the non-passes in test/nethttp/divergences.edn (bin/goi check --corpus nethttp)
	bin/goi check --corpus nethttp
goi-check-nethttp-arbace: ## NH12 (a)'s set: test/nethttp/arbace's Arbace programs (fns as handlers and RoundTrippers, reify, a handler at the REPL, the refused import) against their hand-written outputs (bin/goi check --corpus nethttp --arbace)
	bin/goi check --corpus nethttp --arbace
goi-divergences: ## v1's divergences: every program of goi's check not passing in a class of test/goi/divergences.edn, by design or deferred (bin/goi divergences; bin/goi check runs it)
	bin/goi divergences
java-check: ## Java's conformance check: jdk26u's jtreg run tests read by repr/java and interpreted (file by file), against the JDK's recorded runs (test/java/ref-jdk26.edn), every test not passing in a class of test/java/divergences.edn (bin/java-check; after `make build-java`)
	bin/java-check
java-check-compiled: ## its compiled level: the tests translated by j2g in shards of programs and run (bin/java-check --levels compiled; after `make build`, which leaves the compiled namespaces)
	bin/java-check --levels compiled
java-check-compiled-essential: ## its essential subset, as bin/gate runs it: the essential tests that pass compiled, in one program cached by key (bin/java-check --essential-compiled)
	bin/java-check --essential-compiled
java-check-print: ## its print level: every read test's forms printed back to Java by repr/java's printer, javac on Arbace accepting it, read again to the same forms, its class files against the original's, mode :full printed back byte for byte (bin/java-check --levels print; after `make build-java`)
	bin/java-check --levels print
java-check-nread: ## its nread level: every run test read without javac (the native parser and provider, a file that meets what only javac answers read by javac, counted by reason), the forms against javac's read (bin/java-check --levels read,nread; after `make build-java`)
	bin/java-check --levels read,nread
java-check-judge: ## its judgment of saved runs alone (seconds): RESULTS=... (bin/java-check --save's results.tsv files) against the classes and records (bin/java-check --judge, as bin/gate's java-judge)
	bin/java-check --judge $(RESULTS)
java-check-rejudge: ## JCV1: the whole corpus judged again by the tests' own verdicts from the gate's newest saved results alone (bin/java-check --judge --latest-saved)
	bin/java-check --judge --latest-saved
java-check-oracle: ## JCV1: the oracle tier, by hand at a tag: the same saved results judged by the JDK's recorded runs against test/java/oracle/ (bin/java-check --judge --oracle --latest-saved)
	bin/java-check --judge --oracle --latest-saved
java-verdicts: ## JCV1: whose verdict judges each test (its own, jtreg's, or the JDK's recorded stdout), test/java/verdicts.edn checked against the sources (bin/java-check verdicts --verify)
	bin/java-check verdicts --verify
java-check-essential: ## its essential subset: test/java/essential.txt's tests interpreted from their committed forms (bin/java-check --essential, as bin/gate)
	bin/java-check --essential
java-forms-verify: ## the essential tests' committed forms (test/java/forms/) read afresh on javac's tool executable, byte for byte (bin/java-check --forms verify; --full's)
	bin/java-check --forms verify
java-forms-update: ## the essential tests' forms written again, with their provenance (bin/java-check --forms update; after a change to repr/java)
	bin/java-check --forms update
java-check-diagnostics: ## its diagnostics level: langtools' golden-diagnostics tests compiled by javac on Arbace in place, the output against the golden files as jtreg compares them (bin/java-check --levels diagnostics; after `make build-java`)
	bin/java-check --levels diagnostics
java-check-ref: ## Java's conformance check's reference recorded afresh with the JDK: jtreg's tags scanned, the source sets, the JDK's javac and two runs (bin/java-check --record-ref)
	bin/java-check --record-ref
goi-divergences-aot: ## the AOT level's divergences: every program not passing in a class of test/goi/divergences-aot.edn (bin/goi divergences --aot)
	bin/goi divergences --aot

## repr/ocaml, OCaml's bridge (doc/ocaml/SPEC.md; O1: the spec, the helper and the pins)
repr-ocaml-ocdump: ## build the helper tools/ocdump into .tmp/ocdump/ with the host's ocamlopt on compiler-libs (bin/repr-ocaml ocdump-build)
	bin/repr-ocaml ocdump-build
repr-ocaml-spec-check: ## doc/ocaml/SPEC.md: every forms block reads, every example equals ocdump's output, every Parsetree constructor is in the checklist (bin/repr-ocaml spec-check)
	bin/repr-ocaml spec-check
repr-ocaml-samples: ## test/ocaml/samples/ read by ocdump and by Arbace's reader; the typed ones typed and lowered to Lambda (bin/repr-ocaml samples)
	bin/repr-ocaml samples
repr-ocaml-corpus: ## the corpora's counts and pins of test/ocaml/corpora.edn hold: OCaml 5.5.0's tarball (fetched once), go-whim's whiml (bin/repr-ocaml corpus check; --host: the host's compiler too)
	bin/repr-ocaml corpus check
repr-ocaml-corpus-count: ## the files and lines of every OCaml corpus (bin/repr-ocaml corpus count)
	bin/repr-ocaml corpus count
repr-ocaml-roundtrip: ## O2: OCaml -> forms -> OCaml over the stdlib, the compiler's sources and the samples: the helper's trees equal after the print, the print stable (bin/repr-ocaml roundtrip)
	bin/repr-ocaml roundtrip
repr-ocaml-roundtrip-all: ## the same over every corpus, go-whim's whiml and the testsuite too (bin/repr-ocaml roundtrip stdlib compiler-sources compiler-backend whiml testsuite samples)
	bin/repr-ocaml roundtrip stdlib compiler-sources compiler-backend whiml testsuite samples
repr-ocaml-text: ## O3: mode :full over the default corpora: the text printed from the forms equals the original byte for byte (bin/repr-ocaml roundtrip --text)
	bin/repr-ocaml roundtrip --text
repr-ocaml-cmi: ## O3: and the .cmi level: ocamlc -c on the original and on the print, the .cmi bytes equal; the :lines print's with -no-keep-locs too (bin/repr-ocaml roundtrip --cmi-lines)
	bin/repr-ocaml roundtrip --cmi-lines
repr-ocaml-types: ## OC183 (a): the typed signature level, the typer's signatures of the original and of the print equal, no ocamlc (bin/repr-ocaml roundtrip --text --types samples stdlib)
	bin/repr-ocaml roundtrip --text --types samples stdlib
oi-judges-fixtures: ## OC181 (a): the judges' own tests on fixtures, no toolchain (bin/oi judges-test --fixtures; --oracle the toolchain's parts)
	bin/oi judges-test --fixtures
repr-ocaml-dumps: ## O4: the typed and Lambda dumps of every file ocamlc compiles alone, read by Arbace's reader and shaped by SPEC 11.2, stable across two runs; sizes and seconds (bin/repr-ocaml dumps)
	bin/repr-ocaml dumps
repr-ocaml-native-check: ## the native reader (lexer.mll and parser.mly in Arbace) against the helper over the samples and the stdlib: dumps with positions and :full layouts equal byte for byte (bin/repr-ocaml native-check --layout)
	bin/repr-ocaml native-check --layout
repr-ocaml-menhir: ## the native parser's LALR(1) tables built from parser.mly equal to Menhir's own (boot/menhir/parser.ml) and to the committed arbace/repr/ocaml/tables.ae (bin/repr-ocaml menhir --check, --verify)
	bin/repr-ocaml menhir --check
	bin/repr-ocaml menhir --verify
oi-stdlib: ## O5: the stdlib built by ocamlc in dependency order and its Lambda dumped for oi, into .tmp/oi/lib/ (bin/oi stdlib)
	bin/oi stdlib
oi-build: ## O5: oi's light driver, go/arbace/oi's forms printed by repr/go and built by TamaGo's go (bin/oi build)
	bin/oi build
oi-typer-check: ## oi's native front end (doc/ocaml/TYPER.md): test/ocaml/typer/'s samples typed and translated to Lambda by Arbace, against the committed oracle's signatures, raw and simplified Lambda, then run on oi against the toplevel's outputs (bin/oi typer-check)
	bin/oi typer-check

oi-stdlib-check: ## the stdlib's units typed natively (doc/ocaml/TYPER.md, T8) against bin/oi stdlib's Lambda, the typer's samples run on oi with the equal units native (bin/oi stdlib-check)
	bin/oi stdlib-check

oi-check: ## the testsuite's run programs typed by Arbace's typer (TY24 (a): the default) and run on oi, each judged by its own verdict (the .reference, the exit status) or ocamlc's recorded run, every non-pass classed (bin/oi check)
	bin/oi check
oi-check-ocdump: ## O5: the oracle's path, the programs' Lambda dumped by ocdump on the host's compiler-libs (bin/oi check --ocdump)
	bin/oi check --ocdump
oi-own-verdicts: ## the judged corpora's split, by the tests' own verdicts and by the recorded runs, the two against each other from the records (bin/oi own-verdicts --check)
	bin/oi own-verdicts --check
oi-check-native: ## T11 (doc/ocaml/TYPER.md): the run programs typed and translated by Arbace's typer and run on oi, against ocamlc's runs (test/ocaml/check-oi-native.edn) and compared with ocamlc's path (bin/oi check --native)
	bin/oi check --native
oi-typer-probe: ## T11: the run corpus's signatures and Lambda from Arbace's typer against the oracle's (bin/oi typer-probe)
	bin/oi typer-probe
oi-repl-check-native: ## T11: the recorded toplevel session with each phrase typed by Arbace's typer, against the ocaml toplevel's (bin/oi repl-check --native)
	bin/oi repl-check --native
oi-divergences: ## O5: oi's recorded results against test/ocaml/divergences.edn's classes (bin/oi divergences)
	bin/oi divergences
oi-record-ref: ## O5: ocamlc's and ocamlopt's runs of oi's run corpus recorded into test/ocaml/ref-ocaml55.edn (bin/oi record-ref)
	bin/oi record-ref
oi-repl: ## O5: the toplevel on oi: phrases typed and lowered by the oracle, values printed with their types (bin/oi repl)
	bin/oi repl
oi-aot-lib: ## O6: the stdlib's Lambda translated by o2j into Java forms over the runtime jdk/ocaml/, into .tmp/o6/lib/ (bin/oi aot-lib)
	bin/oi aot-lib
oi-check-aot: ## O6: the run programs translated by o2j and run at the light level (the Java forms interpreted), against ocamlopt's runs and oi's (bin/oi check --aot)
	bin/oi check --aot
oi-check-compiled: ## OT9's first fix: the run programs at the compiled level (o2j's packages, j2g over the JDK's forms, bin/build-go, in shards), against ocamlopt's runs and oi's: test/ocaml/check-aot-compiled.edn (bin/oi check --aot --level compiled); heavy: a night window
	bin/oi check --aot --level compiled
oi-divergences-aot: ## O6: the AOT level's recorded results against test/ocaml/divergences-aot.edn's classes (bin/oi divergences --aot)
	bin/oi divergences --aot
oi-suite-record-ref: ## O7: ocamlc's and ocamlopt's runs of the suite corpus (the testsuite's run tests O5 does not hold, the opam set's programs) into test/ocaml/ref-suite.edn (bin/oi suite record-ref)
	bin/oi suite record-ref
oi-suite-check: ## O7: the suite corpus on oi against ocamlc's runs, every non-pass classed (bin/oi suite check)
	bin/oi suite check
oi-suite-check-aot: ## O7: the suite corpus at the light AOT level against ocamlopt's runs and oi's (bin/oi suite check --aot)
	bin/oi suite check --aot
oi-suite-divergences: ## O7: the suite corpus' recorded results against test/ocaml/divergences-suite{,-aot}.edn (bin/oi suite divergences; --aot)
	bin/oi suite divergences
	bin/oi suite divergences --aot
oi-suite-expect: ## O7: the testsuite's expect tests on the host's expect tool (testsuite/tools/expect.ml on compiler-libs) into test/ocaml/ref-expect.edn (bin/oi suite expect)
	bin/oi suite expect
oi-suite-rollup: ## O7: every .ml and .mli of testsuite/ in a result or a class, from the levels' records, no runs (bin/oi suite rollup)
	bin/oi suite rollup
repr-ocaml-opam: ## O7: the opam set (fetched at pinned versions): read, round trip and :full text, dumps (bin/repr-ocaml roundtrip --text, dumps over the :opam-* corpora)
	bin/repr-ocaml roundtrip --text opam-sexplib0 opam-fmt opam-astring opam-uutf opam-re opam-yojson opam-containers opam-pprint opam-cmdliner opam-base64
	bin/repr-ocaml dumps opam-sexplib0 opam-fmt opam-astring opam-uutf opam-re opam-yojson opam-containers opam-pprint opam-cmdliner opam-base64
repr-python-fetch: ## PY1: CPython 3.14.10's tarball and the site-packages apks fetched once into .tmp/repr-python/, checked by their pins, extracted (bin/repr-python pins --fetch)
	bin/repr-python pins --fetch
repr-python-pins: ## PY1: the oracle (python3 3.14.10's apk), the tarball, the apks and every corpus's measures of test/python/corpora.edn hold (bin/repr-python pins --check)
	bin/repr-python pins --check
repr-python-count: ## PY1: every Python corpus's files, lines, digest, test methods and rejected files as measured now (bin/repr-python pins --count)
	bin/repr-python pins --count
repr-python-spec-check: ## PY1: doc/python/SPEC.md's checklist against Parser/Python.asdl and the oracle's ast, its examples, its forms blocks read (bin/repr-python spec-check)
	bin/repr-python spec-check
repr-python-samples: ## PY1: test/python/samples/ parsed and compiled by the oracle, every ast node class covered (bin/repr-python samples)
	bin/repr-python samples
repr-python-roundtrip: ## PY2: Python -> forms -> Python over samples, stdlib, site-packages and Lib/test, levels forms, ast and code, against test/python/roundtrip-known.edn (bin/repr-python roundtrip; CORPUS=a b)
	bin/repr-python roundtrip $(CORPUS)
repr-python-record: ## PY2: the round trip with its known file and costs rewritten; a file of no class exits 3 (bin/repr-python roundtrip --record; CORPUS=a b)
	REPR_PYTHON_ORACLE=live bin/repr-python roundtrip --record $(CORPUS)
repr-python-check: ## PY2, PY3: spec-check, the samples and both round trips (:lines, :full) over the default corpora (bin/repr-python check)
	bin/repr-python check
repr-python-text: ## PY3: mode :full woven and printed back byte for byte, levels text and marshal, against test/python/full-known.edn (bin/repr-python roundtrip --text; CORPUS=a b)
	bin/repr-python roundtrip --text $(CORPUS)
repr-python-text-record: ## PY3: the :full round trip with its known file and costs rewritten; a file of no class exits 3 (bin/repr-python roundtrip --text --record; CORPUS=a b)
	REPR_PYTHON_ORACLE=live bin/repr-python roundtrip --text --record $(CORPUS)
repr-python-lex-check: ## PY9a: the native lexer against CPython's tokenizer, both streams and their errors, over the corpora and 2,000 files' mutants, against test/python/lex-check-known.edn (bin/repr-python lex-check; CORPUS=a b)
	bin/repr-python lex-check $(CORPUS)
repr-python-lex-record: ## PY9a: the token check with its known file rewritten; a file of no class exits 3 (bin/repr-python lex-check --record; CORPUS=a b)
	bin/repr-python lex-check --record $(CORPUS)
repr-python-pegen: ## PY9b: arbace/repr/python/parser.ae generated from CPython 3.14.10's Grammar/python.gram by pegen ported to Arbace (bin/repr-python pegen)
	bin/repr-python pegen
repr-python-pegen-check: ## PY9b: the parser generated again equal to the committed arbace/repr/python/parser.ae (bin/repr-python pegen --check)
	bin/repr-python pegen --check
repr-python-parse-check: ## PY9b: the native reader against ast.parse (the helper's dumps byte for byte), the corpora and mutants, against test/python/parse-check-known.edn (bin/repr-python parse-check; CORPUS=a b)
	bin/repr-python parse-check $(CORPUS)
repr-python-parse-record: ## PY9b: the tree check with its known file rewritten; a file of no class exits 3 (bin/repr-python parse-check --record; CORPUS=a b)
	bin/repr-python parse-check --record $(CORPUS)
repr-python-ast-check: ## PY9: pyi's _ast on the native reader, its answers equal to CPython's compile() (bin/repr-python ast-check)
	bin/repr-python ast-check
repr-python-records-record: ## queue 213: the oracle's records (test/python/records/) made by the checks with python3, by hand on the main host (bin/repr-python records record; CORPUS=a b, outcomes)
	bin/repr-python records record $(CORPUS)
repr-python-records-verify: ## queue 213: the records against python3 afresh, the oracle tier's (bin/repr-python records verify; CORPUS=a b, SAMPLE=N)
	bin/repr-python records verify $(if $(SAMPLE),--sample $(SAMPLE)) $(CORPUS)
pyi-gen: ## PY4: the synthetic Python corpus written into test/python/synth/ by test/python/gen.ae on stage 0 (bin/pyi gen)
	bin/pyi gen
pyi-gen-check: ## PY4: the synthetic corpus generated again equal to the committed one, every ast node class and builtin method covered (bin/pyi gen --check)
	bin/pyi gen --check
pyi-record-ref: ## PY4: CPython 3.14.10's reference runs (PYTHONHASHSEED=0, twice) of the synthetic corpus, the samples and Lib/test's language core per test method (bin/pyi record-ref)
	bin/pyi record-ref
pyi-record-ref-check: ## PY4: two more runs reproducing every reference of test/python/ref-cpython314*.edn (bin/pyi record-ref --check)
	bin/pyi record-ref --check
pyi-scope-check: ## PY5a: every read file's symbol table by pyi's scope analysis equal to CPython's (bin/pyi scope-check; CORPUS=a b)
	bin/pyi scope-check $(CORPUS)
pyi-compile-check: ## PY5a: every read file compiled to pyi's IR, the IR verified, compile errors judged on CPython's (bin/pyi compile-check; CORPUS=a b; FULL=--full: the forms of mode :full)
	bin/pyi compile-check $(FULL) $(CORPUS)

repr-ocaml-c2j-check: ## repr/ocaml's namespaces and arbace.o2j compiled by c2j alone (no gate step: OC165 (a) embeds oi alone; bin/repr-ocaml c2j-check)
	bin/repr-ocaml c2j-check

pyi-run: ## PY5b0: a Python program run by pyi's runtime on goi (bin/pyi run; FILE=prog.py)
	bin/pyi run $(FILE)

pyi-repl: ## PY5e: pyi's REPL on stdin, python3's basic REPL (bin/pyi repl)
	bin/pyi repl

pyi-repl-check: ## PY5e: test/python/pyi/*.in fed to pyi's REPL and to python3 -q -i, the sessions equal (bin/pyi repl --check)
	bin/pyi repl --check

pyi-lib-test: ## PY5e: Lib/test's 11 language-core modules run by pyi, judged per method against 177's references, test/python/divergences.edn's classes and the check's record (bin/pyi lib-test)
	bin/pyi lib-test

pyi-lib-test-record: ## PY6a: the same run, the record's rows of its modules written again (exit 3 on a method of no class; bin/pyi lib-test --record)
	bin/pyi lib-test --record

pyi-lib-test-oracle: ## PY5e: the lib-test judge's control: the driver run by python3, every method equal (bin/pyi lib-test --oracle)
	bin/pyi lib-test --oracle

pyi-runtime-print: ## PY5b0: go/arbace/py printed as a Go module into .tmp/repr-python/pyi/runtime to edit (bin/pyi runtime print)
	bin/pyi runtime print

pyi-runtime-read: ## PY5b0: the edited runtime's Go read back into go/arbace/py (bin/pyi runtime read)
	bin/pyi runtime read

pyi-runtime-vet: ## PY5b0: the runtime's Go being edited gofmt clean, go vet and go test by TamaGo's go (bin/pyi runtime vet)
	bin/pyi runtime vet

pyi-runtime-check: ## PY5b0: the runtime's forms stable, its operations the IR's, test/python/pyi run equal to the oracle (bin/pyi runtime check)
	bin/pyi runtime check
pyi-runtime-tables: ## queue 213: the generated Unicode and CJK tables against tools/pyi's generators (python3: the oracle tier's) (bin/pyi runtime tables)
	bin/pyi runtime tables

pyi-aot: ## PY8: a Python program compiled by pyi's AOT (the IR to Go forms, TamaGo's go) and run (bin/pyi aot; FILE=prog.py)
	bin/pyi aot $(FILE)

pyi-aot-check: ## PY8: the synthetic corpus and the samples compiled, against CPython's references, the loop and pyi on goi (bin/pyi check --aot)
	bin/pyi check --aot

pyi-aot-check-record: ## PY8: the same, its results recorded into test/python/check-aot-linux-ARCH.edn (bin/pyi check --aot --record)
	bin/pyi check --aot --record

pyi-check: ## PY7: Lib/test under pyi, every method against CPython's references, every non-pass in a class of test/python/divergences.edn, against the record (bin/pyi check; CORPUS=a,b: --corpus; ONLY=FILE: a selection)
	bin/pyi check $(if $(CORPUS),--corpus $(CORPUS)) $(if $(ONLY),--only $(ONLY))

pyi-check-record: ## PY7: the same over the whole universe, test/python/check-lib-test-linux-amd64.edn written (the main host's; bin/pyi check --record)
	bin/pyi check --record

pyi-divergences: ## PY7: the check's record classified by test/python/divergences.edn, no run (bin/pyi divergences)
	bin/pyi divergences

pyi-cover: ## PY7: the coverage subset: the cover run, its blocks, the subset into corpora.edn (bin/pyi cover run, blocks, subset --write)
	bin/pyi cover run && bin/pyi cover blocks && bin/pyi cover subset --write

pyi-c2j-check: ## queue 206: arbace/python's and arbace/repr/python's namespaces compiled by c2j alone, as the re-seed's build would compile them, and its negative control (bin/pyi c2j-check; 20 s, 3 GB)
	bin/pyi c2j-check
pyi-table-check: ## queue 214: pyi's runtime (arbace/py, libm) and ci's back end listed in goi's table, checked by goi's checker in the four configurations (bin/pyi table-check; linux's need bin/goi table-configs)
	bin/pyi table-check
pyi-clib-build: ## queue 208: a C library core of pyi (tools/pyi/clib/LIB) built by ci's builder into its node program, go/arbace/clib/LIB (bin/pyi-clib build; LIB=bzip2)
	bin/pyi-clib build $(or $(LIB),bzip2)
pyi-clib-verify: ## queue 208: the C library's node program built afresh, equal to the committed go/arbace/clib/LIB byte for byte (bin/pyi-clib verify; LIB=bzip2)
	bin/pyi-clib verify $(or $(LIB),bzip2)

python-essential: ## PY10: Python's essential chain as the gate's proposed steps run it (doc/python/PY11-PREP.md): the spec, the samples, both round trips and the front end on samples, synthetic and errors, then test/python/essential-aot.txt compiled and on the loop (bin/repr-python, bin/pyi; about a minute warm)
	bin/repr-python spec-check
	bin/repr-python samples
	bin/repr-python roundtrip samples
	bin/repr-python roundtrip --text samples
	bin/pyi gen --check
	bin/pyi scope-check samples synthetic errors
	bin/pyi compile-check --full samples synthetic errors
	bin/pyi check --aot --no-pyi --only-list test/python/essential-aot.txt

pyi-bench: ## PY8: pyperformance's pure benchmarks run by CPython, compiled and on the loop, the seconds (bin/pyi bench)
	bin/pyi bench

pyi-bench-record: ## PY8: the same, recorded into test/python/bench-aot-linux-ARCH.edn (bin/pyi bench --record)
	bin/pyi bench --record

repr-ocaml-check: ## O2's check: the round trip over the default corpora and over the spec's worked examples (bin/repr-ocaml check)
	bin/repr-ocaml check

## OCaml's row of the REAP matrix and its gate steps (O8, doc/ocaml/OCAML-SURVEY.md, "As built: O8 (write-ahead)")
.PHONY: repr-ocaml-record-results ocaml-read-essential oi-essential oi-divergences-all ocaml-essential ocaml-judges
repr-ocaml-record-results: ## the round trip over every corpus at every level, recording the read and print levels' test/ocaml/{read,print}-results.edn (bin/repr-ocaml roundtrip --cmi --record-results all)
	bin/repr-ocaml roundtrip --cmi --record-results all
ocaml-read-essential: ## repr/ocaml's essential step: the pins, the samples' and the stdlib's round trip at every level, the spec's examples, the embedding check (bin/repr-ocaml)
	bin/repr-ocaml corpus check && bin/repr-ocaml roundtrip --cmi samples stdlib && bin/repr-ocaml roundtrip --spec && bin/repr-ocaml spec-check && bin/repr-ocaml embed-check
oi-essential: ## oi's essential step (OC67 (a)): the package's tests, the REPL's session, the check's basic and effects directories with the control (test/ocaml/essential-check.txt) (bin/oi)
	bin/oi test && bin/oi repl-check && bin/oi check --only-list test/ocaml/essential-check.txt
oi-divergences-all: ## OC160 (a): the four judgments of oi's committed records, no runs (bin/oi divergences [--aot], bin/oi suite divergences [--aot])
	bin/oi divergences && bin/oi divergences --aot && bin/oi suite divergences && bin/oi suite divergences --aot
ocaml-essential: oi-essential oi-divergences-all ocaml-read-essential ## OCaml's three essential steps, one after another (about 2.5 minutes)
ocaml-judges: ## OC120 (b), OC130 (b), OC112 (b): the judges' and levels' tests, the dumps' determinism, the expect check against test/ocaml/ref-expect.edn (bin/oi judges-test)
	bin/oi judges-test

## The host and housekeeping
alpine-check: ## one minimal apk add line reinstalls exactly the host's packages (bin/alpine-env check)
	bin/alpine-env check
alpine-test: ## bin/alpine-env's offline verbs over fabricated apk databases (test/alpine-env/test.bash)
	bash test/alpine-env/test.bash
clean: ## remove target/ (rebuilt by the next build; .tmp/ and the gate's cache kept)
	rm -rf target
repr-scheme-fetch: ## fetch and pin repr/scheme's corpora (Racket's and Larceny's R6RS tests, Chibi's R7RS tests; whimsical checked in place) into .tmp/scheme/corpora/ (bin/repr-scheme corpus fetch)
	bin/repr-scheme corpus fetch
repr-scheme-check: ## repr/scheme against Chez 10.3.0: every corpus file and probe read and written equal, the lexical classes scanned over all Unicode scalar values (bin/repr-scheme check)
	bin/repr-scheme check
repr-scheme-check-quick: ## the same without the scans (bin/repr-scheme check --quick)
	bin/repr-scheme check --quick
repr-scheme-record: ## the same, rewriting test/scheme/check-known.edn and the read level's record test/scheme/check-read-results.edn (bin/repr-scheme check --record)
	bin/repr-scheme check --record
repr-scheme-spec: ## doc/scheme/SPEC.md's examples: the Scheme text read, equal to the forms beside it (bin/repr-scheme spec-check)
	bin/repr-scheme spec-check

repr-scheme-c2j-check: ## repr/scheme's and si's tools' namespaces compiled by c2j alone, as the re-seed's build will compile them (the essential step scheme-c2j; bin/repr-scheme c2j-check)
	bin/repr-scheme c2j-check
repr-scheme-roundtrip: ## repr/scheme's :full and layout printer: every corpus file and probe printed back byte for byte, the layout read back equal (bin/repr-scheme roundtrip)
	bin/repr-scheme roundtrip
si-vendor-verify: ## Scheme's expander: the vendored psyntax's forms (scheme/psyntax/) printed back to their pinned originals, byte for byte (bin/si vendor verify)
	bin/si vendor verify
si-boot: ## psyntax's bootstrap on Chez 10.3.0: scheme/psyntax/psyntax.pp.ae made again from the forms and the patches (bin/si boot)
	bin/si boot
si-boot-check: ## the same into .tmp/si/boot, compared with the committed psyntax.pp.ae (bin/si boot --check)
	bin/si boot --check
si-test: ## the expander's own tests (test/scheme/expand/): expanded and run on the bootstrap host against Chez's recorded runs, and against Chez's expand (bin/si test)
	bin/si test
si-test-record: ## the same, recording Chez's runs first (bin/si test --record)
	bin/si test --record
si-fixpoint: ## psyntax on Arbace expands psyntax's own sources to the core forms of scheme/psyntax/psyntax.pp.ae, up to gensyms' names (bin/si fixpoint)
	bin/si fixpoint
si-unit: ## si's runtime's unit tests: go/arbace/scm printed by repr/go and tested by TamaGo's go (bin/si unit)
	bin/si unit
si-light: ## si's light driver (arbace/scm alone, no Arbace build) into .tmp/si/si-light (bin/si light)
	bin/si light
si-test-si: ## the expander's own tests run on si, judged against Chez's recorded runs (bin/si test --si)
	bin/si test --si
si-check: ## si's check: the R6RS suites' run programs and si's own on si against Chez's recorded runs, every non-pass in a class (bin/si check)
	bin/si check
si-check-record-ref: ## the same, recording Chez's runs first (bin/si check --record-ref)
	bin/si check --record-ref
si-srfi: ## the SRFIs 1, 13, 14, 26, 39, 69, 98, 125, 128, 133, 158 as libraries (scheme/lib/srfi/): their test programs run on si (bin/si srfi)
	bin/si srfi

si-srfi-record: ## the SRFIs' programs run and their counted checks recorded (bin/si srfi --record, test/scheme/check-srfi-linux-amd64.edn)
	bin/si srfi --record
si-aot-check: ## si's AOT check: the check's programs compiled (SM7) and run, judged as si's, compared with the interpreted runs (bin/si check --aot)
	bin/si check --aot
si-aot-check-record: ## the same, recording test/scheme/check-aot-linux-amd64.edn (bin/si check --aot --record)
	bin/si check --aot --record
si-srfi-aot: ## the SRFIs' test programs compiled and run, their output compared with si's interpreted run (bin/si srfi --aot)
	bin/si srfi --aot
si-bench: ## timings for the speed report: larceny's fib and the R6RS suite's programs on si, compiled, and on Chez (bin/si bench)
	bin/si bench
si-divergences: ## the recorded check's non-pass items by class (bin/si divergences)
	bin/si divergences
si-r7rs: ## Chibi's R7RS suite (r7rs-tests.scm) on si in R7RS mode with scheme/lib/scheme: one result a test, every failing test in a class (bin/si r7rs)
	bin/si r7rs

si-r7rs-record: ## the same, recording test/scheme/check-r7rs-linux-amd64.edn (bin/si r7rs --record)
	bin/si r7rs --record

si-ecraven: ## ecraven's r7rs-benchmarks on si, repetition count 1, each benchmark's own check (bin/si ecraven)
	bin/si ecraven

si-ecraven-record: ## the same, recording test/scheme/check-ecraven-linux-amd64.edn (bin/si ecraven --record)
	bin/si ecraven --record

si-r7rs-chez: ## Chibi's R7RS suite rewritten for Chez and run by Chez: its answer to each test (bin/si r7rs --chez)
	bin/si r7rs --chez

si-r7rs-chez-record: ## the same, recording test/scheme/ref-r7rs-chez.edn (bin/si r7rs --chez --record)
	bin/si r7rs --chez --record

si-r7rs-chibi: ## Chibi's R7RS suite run by Chibi-Scheme 0.12.0 (the host's chibi-scheme, test/scheme/chibi/driver.scm): its answer to each test (bin/si r7rs --chibi)
	bin/si r7rs --chibi

si-r7rs-chibi-record: ## the same, recording test/scheme/ref-r7rs-chibi.edn (bin/si r7rs --chibi --record)
	bin/si r7rs --chibi --record

si-r7rs-chibi-test: ## Chibi's R7RS suite with Chibi's own (chibi test) (scheme/vendor/chibi), its tests compared with SM8's library's (bin/si r7rs --chibi-test)
	bin/si r7rs --chibi-test

si-r7rs-chibi-test-record: ## the same, recording test/scheme/check-r7rs-chibi-test-linux-amd64.edn (bin/si r7rs --chibi-test --record)
	bin/si r7rs --chibi-test --record

si-chibi-tests: ## Chibi's own tests of the vendored libraries (diff, optional, term ansi) on si and on Chibi, test by test (bin/si chibi-tests)
	bin/si chibi-tests

si-chibi-tests-record: ## the same, recording test/scheme/check-chibi-tests-linux-amd64.edn (bin/si chibi-tests --record)
	bin/si chibi-tests --record

si-ecraven-chez: ## ecraven's r7rs-benchmarks on Chez through the R7RS libraries' Chez variants: the reference's answers and times (bin/si ecraven --chez)
	bin/si ecraven --chez

si-ecraven-chez-record: ## the same, recording test/scheme/ref-ecraven-chez.edn (bin/si ecraven --chez --record)
	bin/si ecraven --chez --record

si-ecraven-chibi: ## ecraven's r7rs-benchmarks on Chibi-Scheme 0.12.0 (the host's chibi-scheme): the second reference's answers and times (bin/si ecraven --chibi)
	bin/si ecraven --chibi

si-ecraven-chibi-record: ## the same, recording test/scheme/ref-ecraven-chibi.edn (bin/si ecraven --chibi --record)
	bin/si ecraven --chibi --record

si-mats: ## Chez's mats on si: each mat run in an environment of (chezscheme) (si's subset) and the harness, under the watchdog; a mat that does not expand has its clauses run alone (bin/si mats)
	bin/si mats

si-mats-record: ## the same, recording test/scheme/check-mats-linux-amd64.edn (bin/si mats --record)
	bin/si mats --record

si-corpora-files: ## every file of Chez's mats, ecraven's benchmarks and Chibi's suite in a result or a class (arbace.scheme.corpora files)
	bin/si corpora files

si-corpora-divergences: ## the SM8 corpora's recorded non-pass items by class (arbace.scheme.corpora divergences)
	bin/si corpora divergences

## Scheme's row of the REAP matrix and its gate steps (SM9, doc/scheme/SCHEME-SURVEY.md, "As built: SM9 (write-ahead)")
repr-scheme: ## repr/scheme's two levels against their records: the read check against Chez (bin/repr-scheme check) and the round trip (bin/repr-scheme roundtrip)
	bin/repr-scheme check && bin/repr-scheme roundtrip
repr-scheme-roundtrip-record: ## the round trip, recording the print level's test/scheme/roundtrip-results.edn (bin/repr-scheme roundtrip --record)
	bin/repr-scheme roundtrip --record
si-essential: ## si's essential step: the runtime's unit tests, the expander's tests on si, the check's essential subset (test/scheme/essential-check.txt), Chibi's R7RS suite, the SRFIs (bin/si)
	bin/si unit && bin/si test --si && bin/si check --only test/scheme/essential-check.txt && bin/si r7rs && bin/si srfi
si-aot-essential: ## si's AOT essential subset (test/scheme/essential-aot.txt) compiled, against the record and the interpreted runs of si-essential (bin/si check --aot --only)
	bin/si check --aot --only test/scheme/essential-aot.txt
scheme-read-essential: ## repr/scheme's essential step: the read check and the round trip over every corpus but whimsical, the spec's examples, the corpora's files classed (bin/repr-scheme, bin/si corpora files)
	set -f; c='--corpus probes --corpus racket-r6rs --corpus larceny-r6rs --corpus chibi-r7rs --corpus srfi-*'; bin/repr-scheme check $$c && bin/repr-scheme roundtrip $$c && bin/repr-scheme spec-check && bin/si corpora files
scheme-essential: si-essential si-aot-essential scheme-read-essential ## Scheme's three essential steps, one after another (about 2.5 minutes)

si-check-suite: ## Racket's R6RS suite: every library and run.sps expanded by psyntax on Arbace (bin/si check-suite)
	bin/si check-suite
check-makefile: ## every bin/ script this Makefile's recipes run exists and is executable
	@missing=0; for s in $$(grep -E '^[[:space:]]' $(firstword $(MAKEFILE_LIST)) | grep -oE '(^|[[:space:]=])bin/[a-z0-9-]+' \
	    | grep -oE 'bin/[a-z0-9-]+' | sort -u); do \
	  [ -x "$$s" ] || { echo "Makefile: $$s is not an executable script" >&2; missing=1; }; done; \
	exit $$missing
