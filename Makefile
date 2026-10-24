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
  gate gate-full suite oracle diffcheck generated-check verify jdk-verify edge-verify javac-verify javac-classes fixtures-verify tamago-verify \
  toolchain-verify box-vendor-verify x509roots-verify repr-go-roundtrip repr-go-roundtrip-text repr-go-native-check \
  repr-java-check repr-java-roundtrip repr-java-text repr-java-jdk repr-java-parse-check repr-java-front-check repr-java-attr-check repr-clojure-check repr-clojure-roundtrip repr-haskell-roundtrip repr-haskell-iface repr-haskell-text repr-haskell-layout repr-haskell-test repr-rust-roundtrip repr-rust-iface repr-rust-text repr-rust-pretty repr-rust-behaviour repr-rust-code repr-rust-all repr-rust-test goi-annotations goi-test goi-check goi-divergences alpine-check clean \
  check-makefile

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
goi-table: ## goi's table of compiled packages and its digests (bin/goi table; the build runs it)
	bin/goi table
goi-table-size: ## the table's size in the executable, the invokers' part (bin/goi table --size; after `make build`)
	bin/goi table --size

## The box: Arbace as a TamaGo guest (bin/arbace-go --box, bin/box-*)
box: ## the guest images and the launcher arbace-box (bin/arbace-go --build --box)
	bin/arbace-go --build --box
box-smoke: ## the box's smoke test on /dev/kvm (bin/arbace-go --smoke --box)
	bin/arbace-go --smoke --box
box-oracle: ## the oracle's subset in the box, against its known mismatches (as bin/gate --full)
	ARBACE_BOX_NET=loopback bin/oracle check 'target/arbace-go/box-amd64/arbace-box -' -j 8 \
	  --timeout 900 --exclude forms/files. --exclude forms/processes. \
	  --expected test/oracle/known-go-box-amd64.edn
box-suite: ## Clojure's suite's subset in the box (bin/box-suite; after `make suite`)
	bin/box-suite
box-goi: ## goi's check in the box: test/goi/box.txt's programs in the guest against gc (bin/goi check --box, as bin/gate --full)
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
gate: ## the essential gate, for every change to main (bin/gate)
	bin/gate
gate-full: ## the full gate, at milestones (bin/gate --full)
	bin/gate --full
suite: ## Clojure's suite on the test build (bin/clojure-tests, CLOJURE_TESTS_GO)
	CLOJURE_TESTS_GO=$(CLOJURE_TESTS_GO) bin/clojure-tests
oracle: ## the executable against the frozen JVM's results (bin/oracle check)
	bin/oracle check 'target/arbace-go/amd64/arbace -' --timeout 900 \
	  --expected test/oracle/known-go-amd64-frozen.edn

diffcheck: ## each language's two halves on the same forms: evaluated against compiled, interpreted against j2g (bin/diffcheck)
	bin/diffcheck
generated-check: ## the generated files as their generators write them: arbace/lang/go's variants (CompilerNodes.ae, CompilerAOTFn.ae) and repr/go's Go forms in go/arbace/repr/go (the dumper, gc's syntax renamed) (bin/generated-check; --update rewrites them)
	bin/generated-check

## Vendored trees and recorded data, converted or recorded afresh and compared byte for byte
VERIFY := jdk-verify edge-verify javac-verify javac-classes repr-java-check repr-java-parse-check repr-java-front-check repr-java-attr-check fixtures-verify tamago-verify toolchain-verify \
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
repr-haskell-test: ## repr/haskell's cases: the reader's samples, the spec's worked example, mode :full, the layout layout, R5's and R6's cases (bin/repr-haskell test)
	bin/repr-haskell test
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
goi-annotations: ## goi's checker over tamago/goroot, linux std, GOROOT/test: no error, every annotation verified (bin/goi annotations)
	bin/goi annotations
goi-test: ## goi's tests: the checker's (invalid forms with gc's messages, checked trees), the interpreter's programs against gc, Go forms at the REPL (bin/goi test --gc; after `make build`)
	bin/goi test --gc
goi-check: ## goi's check: GOROOT/test's run programs interpreted against gc, its errorcheck programs' errors judged as Go's test driver does (gc's syntax errors, the raw forms of invalid programs: GP6; gc's branch checks, noder's pragmas and declaration checks: GPS41 (c)), against test/goi/check-linux-amd64.edn (bin/goi check; after `make build`)
	bin/goi check
goi-divergences: ## v1's divergences: every program of goi's check not passing in a class of test/goi/divergences.edn, by design or deferred (bin/goi divergences; bin/goi check runs it)
	bin/goi divergences

## The host and housekeeping
alpine-check: ## one minimal apk add line reinstalls exactly the host's packages (bin/alpine-env check)
	bin/alpine-env check
clean: ## remove target/ (rebuilt by the next build; .tmp/ and the gate's cache kept)
	rm -rf target
check-makefile: ## every bin/ script this Makefile's recipes run exists and is executable
	@missing=0; for s in $$(grep -E '^[[:space:]]' $(firstword $(MAKEFILE_LIST)) | grep -oE '(^|[[:space:]=])bin/[a-z0-9-]+' \
	    | grep -oE 'bin/[a-z0-9-]+' | sort -u); do \
	  [ -x "$$s" ] || { echo "Makefile: $$s is not an executable script" >&2; missing=1; }; done; \
	exit $$missing
