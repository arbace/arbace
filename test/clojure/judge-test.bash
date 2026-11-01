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

# finding 8: the essential subset's compile log may lack reference entries of sources it never loads, but not name
# one the reference does not have; the whole run's must equal
test_sources_check() {
  local ab=$tmp/ab fn
  mkdir -p "$ab"
  fn=$(awk '/^aot_sources_check\(\) \{/ { p = 1 } p { print } p && $0 == "}" { exit }' "${CT:-$here/bin/clojure-tests}")
  [ -n "$fn" ] || return 1
  eval "$fn"
  host_run() { # the reference's :not-compiled and :not-loaded keys (the call's -e argument names which)
    case $2 in *":not-compiled"*) printf 'a/one.clj\nb/two.clj\n' ;; *) printf 'ns.late\n' ;; esac
  }
  expected=$tmp/exp.edn
  # the subset compiled one of the two sources the reference names: good under --essential, bad whole
  printf 'aot: not compiled: a/one.clj\n' > "$ab/compile.log"
  ess_list=x
  aot_sources_check "$ab" > "$tmp/c1.out" 2>&1 || { cat "$tmp/c1.out" >&2; return 1; }
  ess_list=
  aot_sources_check "$ab" > "$tmp/c2.out" 2>&1 && return 1
  # a source the reference does not name fails both
  printf 'aot: not compiled: a/one.clj\naot: not compiled: c/three.clj\n' > "$ab/compile.log"
  ess_list=x
  aot_sources_check "$ab" > "$tmp/c3.out" 2>&1 && return 1
  grep -q 'c/three.clj' "$tmp/c3.out"
}
t sources-check-essential-subset test_sources_check

# finding 9: the compiled mode counts an embedded source without a compiled loader as evaluated (the hook's decision
# on stub lookups; the embedded lookup on the host's own runtime)
test_hook_embedded() {
  local out
  out=$(ARBACE_PATH="$here" "$host" -e '(load-file "'"${HOOK:-$here/test/suite_aot_hook.ae}"'")
    (let [f @#'"'"'suite-aot.hook/embedded-without-loader g @#'"'"'suite-aot.hook/embedded-source]
      (println (f (fn [b] (when (= b "x") "x.ae")) (constantly nil) "x")
               (f (fn [b] nil) (constantly nil) "y")
               (f (fn [b] "z.ae") (constantly :loader) "z")
               (g "arbace/core") (g "no/such/ns")))' 2>&1 | tail -1)
  [ "$out" = "x.ae nil nil arbace/core.ae nil" ] || { echo "$out" >&2; return 1; }
}
t hook-counts-embedded-without-loader test_hook_embedded

exit $failed
