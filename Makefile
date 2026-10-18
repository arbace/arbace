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

.PHONY: help seed-check stage0 seed-verify reseed build build-tests smoke repl fixpoint goi-table goi-table-size \
  box box-smoke box-oracle box-suite box-tools box-rebuild mac-bundle arm64-vm-check \
  gate gate-full suite oracle diffcheck verify jdk-verify edge-verify fixtures-verify tamago-verify \
  toolchain-verify box-vendor-verify x509roots-verify repr-go-roundtrip repr-go-roundtrip-text \
  repr-java-roundtrip repr-java-text repr-clojure-check repr-clojure-roundtrip repr-haskell-roundtrip repr-haskell-iface repr-haskell-text repr-haskell-layout repr-haskell-test repr-rust-roundtrip repr-rust-iface repr-rust-test goi-annotations goi-test alpine-check clean \
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
box-tools: ## the tool guests (gc, the linker, the assembler) from tamago/cmd (bin/box-tools build)
	bin/box-tools build
box-rebuild: ## Arbace rebuilt inside the box, compared byte for byte; stops on stale executables (bin/box-rebuild)
	bin/box-rebuild
mac-bundle: ## the Mac's launcher, guest and checklist, all HEAD's (bin/box-mac-bundle; after box-rebuild for arm64)
	bin/box-mac-bundle
arm64-vm-check: ## the arm64 box in an emulated aarch64 VM: smoke, hv (bin/box-arm64-vm check)
	bin/box-arm64-vm check

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

## Vendored trees and recorded data, converted or recorded afresh and compared byte for byte
VERIFY := jdk-verify edge-verify fixtures-verify tamago-verify toolchain-verify \
  box-vendor-verify x509roots-verify repr-go-roundtrip repr-go-roundtrip-text repr-java-roundtrip repr-java-text \
  repr-clojure-roundtrip
verify: $(VERIFY) ## all of the below, one after another
jdk-verify: ## jdk/'s forms by repr/java, on the frozen JVM (bin/jrt-convert verify)
	bin/jrt-convert verify
edge-verify: ## j2g's edge, on the frozen JVM (bin/j2g-edge verify)
	bin/j2g-edge verify
fixtures-verify: ## the suite's Java fixtures, on the frozen JVM (bin/clojure-tests --java-fixtures verify)
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
repr-java-roundtrip: ## Java -> forms -> Java -> forms over Clojure's Java, the samples, jdk/forms (bin/repr-java roundtrip)
	bin/repr-java roundtrip --corpus baseline,samples,jdk
repr-java-text: ## the same in mode :full: each Java file printed back byte for byte, javac's classes compared (bin/repr-java roundtrip --text-only)
	bin/repr-java roundtrip --corpus baseline,samples,jdk --text-only
repr-clojure-check: ## repr/clojure's unit tests; Clojure's suite translated against its digest and printed back (bin/repr-clojure check [--suite])
	bin/repr-clojure check
	bin/repr-clojure check --suite
repr-clojure-roundtrip: ## Clojure -> Arbace -> Clojure over the frozen clojure/, the suite, its libraries (.cljc back whole with --full); Arbace -> Clojure -> Arbace over arbace/ (bin/repr-clojure check --roundtrip)
	bin/repr-clojure check --roundtrip
repr-haskell-roundtrip: ## Haskell -> forms -> Haskell over GHC 9.14.1's boot libraries (41 packages): GHC's trees and lines (bin/repr-haskell roundtrip)
	bin/repr-haskell roundtrip
repr-haskell-iface: ## the same, then GHC's interfaces of the originals and the printed modules compiled, compared (bin/repr-haskell roundtrip --iface)
	bin/repr-haskell roundtrip --iface
repr-haskell-text: ## the same in mode :full: each module printed back byte for byte, GHC's trees of mode :full compared (bin/repr-haskell roundtrip --text-only)
	bin/repr-haskell roundtrip --text-only
repr-haskell-layout: ## the same forms printed by Haskell's layout rule, as a person writes them: GHC's trees and lines, then interfaces (bin/repr-haskell roundtrip --layout-only --iface)
	bin/repr-haskell roundtrip --layout-only --iface
repr-haskell-test: ## repr/haskell's cases: the reader's samples, the spec's worked example, mode :full, the layout layout (bin/repr-haskell test)
	bin/repr-haskell test
repr-rust-roundtrip: ## Rust -> forms -> Rust over std's five crates of rustc 1.99.1: rustc's trees and lines (bin/repr-rust roundtrip)
	bin/repr-rust roundtrip
repr-rust-iface: ## the same, then rustdoc's JSON of the five crates from the originals and from the printed files, compared without spans (bin/repr-rust roundtrip --iface)
	bin/repr-rust roundtrip --iface
repr-rust-test: ## repr/rust's cases: the reader's samples, the spec's worked example and its round trip, the interface level's cases (bin/repr-rust test)
	bin/repr-rust test
goi-annotations: ## goi's checker over tamago/goroot, linux std, GOROOT/test: no error, every annotation verified (bin/goi annotations)
	bin/goi annotations
goi-test: ## goi's checker's tests: invalid forms with gc's messages, checked trees (bin/goi test --gc)
	bin/goi test --gc

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
