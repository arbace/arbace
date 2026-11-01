#!/bin/bash
# GTS114 (a) (audit 041): bin/gate-tests runs every *-test.bash of a directory and fails when one does; the
# step gate-tests is in test/gate/steps.edn, essential, and the planner accepts the table (read by gate.plan
# on stage 0 over a fabricated hosts.edn: its checks of rules matching no file, hosts, steps and scripts).
# Needs an Arbace executable: ARBACE_HOST_EXE=EXE test/gate/gate-tests-test.bash
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
export ARBACE_HOST_EXE=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$ARBACE_HOST_EXE" ] || { echo "gate-tests-test: no executable $ARBACE_HOST_EXE" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
scratch=$TMPDIR/gate-tests-test; rm -rf "$scratch"; mkdir -p "$scratch/pass" "$scratch/fail" "$scratch/none" "$scratch/logs" "$scratch/store"
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }

# the runner
printf '#!/bin/bash\nexit 0\n' > "$scratch/pass/a-test.bash"; chmod +x "$scratch/pass/a-test.bash"
cp "$scratch/pass/a-test.bash" "$scratch/fail/a-test.bash"
printf '#!/bin/bash\necho broken\nexit 1\n' > "$scratch/fail/b-test.bash"; chmod +x "$scratch/fail/b-test.bash"
"$src/bin/gate-tests" "$scratch/pass" > "$scratch/o1" 2>&1 && ok "passing tests pass" || bad "pass dir: $(cat "$scratch/o1")"
"$src/bin/gate-tests" "$scratch/fail" > "$scratch/o2" 2>&1 && bad "a failing test passed" || { grep -q "b-test.bash FAILED" "$scratch/o2" && ok "a failing test fails the run, named" || bad "failing: $(cat "$scratch/o2")"; }
"$src/bin/gate-tests" "$scratch/none" > "$scratch/o3" 2>&1 && bad "no test passed" || ok "an empty directory fails"

# the step and the planner
cd "$src" || exit 1
find . \( -path ./.git -o -path ./.tmp -o -path ./target -o -path ./.claude \) -prune -o -type f -printf '%P\n' \
  | LC_ALL=C sort | xargs -d '\n' sha256sum > "$scratch/logs/files.sha256"
echo '{:gate "a" :javac "b" :prog-units "pending" :prog-rest "c" :javac-prog-units "pending" :javac-prog-rest "d" :stage0 "e" :frozen "f" :jdk "g" :go "h"}' > "$scratch/logs/hosts.edn"
. "$src/bin/lib/tools.bash"
root=$src
arbace_tool -p "$src/test" -m gate.plan plan "$src" "$scratch/logs" "$scratch/store" essential > "$scratch/plan.out" 2>&1
if grep -q "problem(s)" "$scratch/plan.out"; then bad "the planner rejects the table: $(grep -A5 'problem(s)' "$scratch/plan.out")"
elif grep -q "^ *run *gate-tests " "$scratch/plan.out"; then ok "the planner accepts the table and plans gate-tests"
else bad "gate-tests not planned: $(head -5 "$scratch/plan.out")"; fi

[ "$fails" = 0 ] && echo "gate-tests-test: passed" || echo "gate-tests-test: $fails failed"
exit $((fails > 0))
