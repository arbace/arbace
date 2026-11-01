#!/bin/bash
# No script, comment or doc names .tmp/arbace-go-gate as the gate's cache: nothing populates it since GTS24 (a) (GTS120).
# Usage: test/gate/stale-comments-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
mkdir -p "$TMPDIR"
T=$here/.tmp/stale-comments-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
# 8. the fixpoint's --g1 documented as the gate's EXES/KEY (no .tmp/arbace-go-gate)
if grep -n 'g1 .tmp/arbace-go-gate' "$here/bin/gate" > /dev/null; then bad "bin/gate still names --g1 .tmp/arbace-go-gate"; else ok "bin/gate: no --g1 .tmp/arbace-go-gate"; fi

# 9. the stale comments
stale=$(cd "$here" && grep -n 'arbace-go-gate' bin/arbace-go bin/gate bin/diffcheck bin/generated-check bin/java-check bin/clojure-tests bin/lib/program.bash test/gate/steps.edn doc/go/BUILD.md 2>&1)
if [ -z "$stale" ]; then ok "no .tmp/arbace-go-gate left"; else bad "stale .tmp/arbace-go-gate:"$'\n'"$stale"; fi

exit $fail
