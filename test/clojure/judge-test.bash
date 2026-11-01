#!/bin/bash
# Tests of the suite runner's judging (test/run_clojure_tests.clj's report) on fabricated results (report 060):
# the runner is translated by repr/clojure and run by the tools' host (ARBACE_HOST_EXE) on made-up
# per-namespace results; no suite, no build.
#   test/clojure/judge-test.bash    prints "ok NAME" per test, exits 1 when one fails
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
runner_src=${RUNNER:-$here/test/run_clojure_tests.clj}
host=${ARBACE_HOST_EXE:-$here/target/seed/amd64/arbace}
export ARBACE_HOST_EXE=$host
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
tmp=$TMPDIR/clojure-judge-test.$$
mkdir -p "$tmp/out"; trap 'rm -rf "$tmp"' EXIT
failed=0
t() { local name=$1; shift; if "$@"; then echo "ok $name"; else echo "FAIL $name"; failed=1; fi; }

"$here/bin/repr-clojure" read --stdin < "$runner_src" > "$tmp/runner.clj" || { echo "FAIL translating the runner"; exit 1; }
cat > "$tmp/exp.edn" <<'EOT'
{:revision "x" :skipped {} :namespaces {a.one {:test 3 :pass 3 :fail 0 :error 0} a.vars {:test 9 :pass 9 :fail 0 :error 0}}}
EOT
echo '{:ns a.one :test 3 :pass 3 :fail 0 :error 0}' > "$tmp/out/a.one.edn"
echo '[{:id "1" :subject a.macros :reference a.vars}]' > "$tmp/controls.edn"

# report NSS [ENV=VALUE...]: the report on the fabricated results, output in $tmp/o.txt, its status
report() {
  local nss=$1; shift
  env "$@" CLOJURE_TESTS_CONTROLS="$tmp/controls.edn" ARBACE_PATH="$here/test/suite-go" \
    "$host" "$tmp/runner.clj" report "$tmp/out" "$tmp/exp.edn" "$tmp/res.edn" "$nss" > "$tmp/o.txt" 2>&1
}

# finding 5: a control whose subject did not run is no failure of a run of chosen namespaces, and is one
# of the essential subset (CLOJURE_TESTS_CONTROLS_STRICT)
test_control_lenient() { report a.one X=1 && grep -q 'not judged, a.macros not run' "$tmp/o.txt"; }
t control-subject-missing-lenient test_control_lenient
test_control_strict() {
  ! report a.one CLOJURE_TESTS_CONTROLS_STRICT=1 && grep -q 'control-1 - .*a.macros not run' "$tmp/o.txt"
}
t control-subject-missing-essential-fails test_control_strict

exit $failed
