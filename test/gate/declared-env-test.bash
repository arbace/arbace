#!/bin/bash
# bin/gate's environment scrub keeps the names the steps declare in :env (queue 085, report 084 finding 5;
# GTS112 (b)): the frozen JVM's FROZEN_CP, FROZEN_TAG, ARBACE_ORACLE_JAR, ARBACE_ORACLE_TAG, oracle-go's
# ARBACE_ORACLE_EXPECTED and j2g-edge's J2G_FIXTURES and J2G_EDGE_WORK reach the steps whose keys hold them, where the
# scrub (queue 023) used to drop them and the keys held "unset" whatever the caller set. bin/gate's gate_keep_env,
# gate_declared_env_of and gate_scrub_env, extracted and run alone: on test/gate/steps.edn (every declared name kept,
# the names cross-checked by a plain grep of the table's one-line :env vectors), and on a fabricated table (a vector
# over two lines kept; an :env in a comment or inside a string, and an undeclared FROZEN_JAR, dropped). No executable.
# By hand: test/gate/declared-env-test.bash [GATE_SCRIPT]   (another bin/gate to test, e.g. an older one's copy)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
gate=${1:-$src/bin/gate}
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
d=$(mktemp -d "$TMPDIR/declared-env-test.XXXXXX"); trap 'rm -rf "$d"' EXIT
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }
for f in gate_keep_env gate_declared_env_of gate_scrub_env; do
  sed -n "/^$f() {/,/^}/p" "$gate"
done > "$d/fns.bash"
grep -q '^gate_scrub_env() {' "$d/fns.bash" || { echo "FAIL: no gate_scrub_env in $gate"; exit 1; }

# scrubbed TABLE VAR...: the variables (each set to "v") still set after gate_scrub_env with GATE_STEPS=TABLE
scrubbed() {
  local table=$1; shift
  env -i PATH="$PATH" HOME="$HOME" ARBACE_GATE_STORE="$d/store" GATE_STEPS="$table" \
    $(for v in "$@"; do printf '%s=v ' "$v"; done) \
    bash -c 'root=$1; shift; . "$0"; gate_scrub_env 2>/dev/null; for v in "$@"; do [ -n "${!v+x}" ] && echo "$v"; done' \
    "$d/fns.bash" "$d/root" "$@"
}

# 1. the real table: every name its steps declare in :env
want=$(grep -o ':env \[[^]]*\]' "$src/test/gate/steps.edn" | grep -o '"[^"]*"' | tr -d '"' | sort -u)
[ -n "$want" ] || bad "no :env in test/gate/steps.edn"
for n in ARBACE_FROZEN_JAVA_HOME FROZEN_CP FROZEN_TAG ARBACE_ORACLE_JAR ARBACE_ORACLE_TAG ARBACE_ORACLE_EXPECTED J2G_FIXTURES J2G_EDGE_WORK; do
  printf '%s\n' "$want" | grep -qx "$n" || bad "test/gate/steps.edn no longer declares $n (this test's premise)"
done
got=$(scrubbed "$src/test/gate/steps.edn" $want | sort -u)
lost=$(comm -23 <(printf '%s\n' "$want") <(printf '%s\n' "$got") | tr '\n' ' ')
if [ -z "$lost" ]; then ok "steps.edn: the $(printf '%s\n' "$want" | wc -l) declared names kept"
else bad "steps.edn: declared in :env but dropped by the scrub (their keys hold \"unset\"): $lost"; fi
got=$(scrubbed "$src/test/gate/steps.edn" FROZEN_JAR ARBACE_PATH REPR_GO_READER)
[ -z "$got" ] && ok "steps.edn: undeclared FROZEN_JAR, ARBACE_PATH, REPR_GO_READER dropped" || bad "steps.edn: kept undeclared: $got"

# 2. a fabricated table
cat > "$d/steps.edn" <<'T'
;; :env ["ARBACE_IN_COMMENT"]
{:groups {}
 :steps
 [{:name a :cmd "echo ':env [\"ARBACE_IN_STRING\"]'; true" :env ["FROZEN_TAG"
                                                              "J2G_TWO_LINES"]}
  {:name b :cmd "true" :env-like ["ARBACE_NOT_ENV"] :summary ["\\[" 1]}]}
T
got=$(scrubbed "$d/steps.edn" FROZEN_TAG J2G_TWO_LINES | sort | tr '\n' ' ')
[ "$got" = "FROZEN_TAG J2G_TWO_LINES " ] && ok "fabricated: a vector over two lines kept" || bad "fabricated: kept '$got', wanted FROZEN_TAG J2G_TWO_LINES"
got=$(scrubbed "$d/steps.edn" ARBACE_IN_COMMENT ARBACE_IN_STRING ARBACE_NOT_ENV FROZEN_JAR FROZEN_CP | tr '\n' ' ')
[ -z "$got" ] && ok "fabricated: names in a comment, a string or another key, and undeclared ones, dropped" || bad "fabricated: kept '$got'"
got=$(scrubbed "$d/steps.edn" ARBACE_GOROOT GATE_TEST_JOBS | sort | tr '\n' ' ')
[ "$got" = "ARBACE_GOROOT GATE_TEST_JOBS " ] && ok "fabricated: gate_keep_env's own list kept" || bad "fabricated: own list: '$got'"

[ $fails = 0 ] && echo "declared-env-test: passed" || echo "declared-env-test: $fails failed"
[ $fails = 0 ]
