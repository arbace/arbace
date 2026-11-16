#!/bin/bash
# GD4 (doc/GATE-DIET-SURVEY.md, GDS12 (a), GDS13 (a), "As built: GD4"): the runner's disk and memory policy (bin/lib/gate-run.bash's
# disk_admit, enospc, disk_retry, run_one's GATE_STEP_MEM_GIB) on a fabricated plan and store under .tmp/disk-policy-test, the free
# disk given by GATE_DISK_FREE_FILE (honoured with a store of the test's own):
#   1. above the floor a step runs; below it the cheap prune runs once, the step waits GATE_DISK_WAIT and is deferred (not failed,
#      no pass), a step that needs it is deferred with it, and a later step is deferred at once (the wait is the run's, not each
#      step's); :disk-gib adds to the floor;
#   2. the disk back, disk_retry runs the deferred steps in order: they pass and their passes are recorded;
#   3. "No space left on device" in a step's log or outputs fails it whatever its status, its pass not recorded, its report says so;
#   4. a step sees GATE_STEP_MEM_GIB, the plan's admission (none for an undeclared step); ARBACE_DISK_POLICY=gate; bin/gate-policy
#      caps a tool's GOMEMLIMIT default at it and its disk guard yields under the gate.
# Seconds, no build, no gate run. Usage: test/gate/disk-policy-test.bash
set -uo pipefail
unset audit_step audit_all GATE_CLEAN_INNER GATE_LANES GATE_STEP_MEM_GIB ARBACE_DISK_POLICY
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=$here/.tmp/tmp; mkdir -p "$TMPDIR"
T=$here/.tmp/disk-policy-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
root=$here logs=$T/logs store=$T/store
mkdir -p "$logs/plan" "$store"
export ARBACE_GATE_STORE=$store GATE_DISK_FREE_FILE=$T/free GATE_CACHES=none GATE_DISK_POLL=1
. "$here/bin/lib/gate-cache.bash"
. "$here/bin/lib/gate-run.bash"
export ARBACE_DISK_POLICY=gate
commit=c0ffee dirty=false start=$(date +%s) status=0
GATE_HEAVY_CMD=env
gib_kib() { awk -v g="$1" 'BEGIN {printf "%d", g * 1048576}'; }
# plan.tsv: NAME DECISION KEY PHASE CHAIN NEEDS START-AFTER HEAVY MEM BUDGET SUMMARY-N SUMMARY-RE WHY AUDIT-CMD OUTPUTS USES KIND DECLARED DISK CMD
row() { printf '%s\trun\t%s\tfull\tc\t%s\t-\t-\t%s\t-\t-\t-\twhy\t-\t%s\t-\tcheck\t%s\t%s\t%s\n' "$1" "$(printf '%s' "$1" | sha256sum | cut -c1-64)" "$2" "$3" "$4" "$3" "$5" "$6"; }
{ row a - 2 - - 'echo a; echo "$GATE_STEP_MEM_GIB" > '"$T"'/a.mem; echo "$ARBACE_DISK_POLICY" > '"$T"'/a.pol'
  row b - - - - 'echo b; echo "${GATE_STEP_MEM_GIB-unset}" > '"$T"'/b.mem'
  row c - 1 - - 'echo c'
  row d c 1 - - 'echo d'
  row e - 1 - 2 'echo e'
  row f - 1 - 0.5 'echo f'
  row g - 1 - - 'echo g; echo "No space left on device"'
  row h - 1 yes - 'mkdir -p "$GATE_OUT"; echo "write: No space left on device" > "$GATE_OUT/save.txt"'
} > "$logs/plan.tsv"
load_plan
for n in "${order[@]}"; do : > "$logs/plan/$n.inputs"; done
rerun_one() { rm -f "$logs/$1.done" "$logs/$1.status" "$logs/$1.deferred"; run_one "$1"; }
pass_of() { [ -f "$store/$1/${key[$1]}.edn" ]; }

# 1. above the floor
gib_kib 40 > "$T/free"
run_one a > "$T/a.out" 2>&1
[ "$(cat "$logs/a.status")" = 0 ] && pass_of a && ok "40 GiB free, the floor 25: a step runs and its pass is recorded" || bad "a: $(cat "$logs/a.status") $(cat "$T/a.out")"
[ "$(cat "$T/a.mem")" = 2 ] && ok "the step sees GATE_STEP_MEM_GIB=2, the plan's admission" || bad "a's GATE_STEP_MEM_GIB: $(cat "$T/a.mem")"
[ "$(cat "$T/a.pol")" = gate ] && ok "the step sees ARBACE_DISK_POLICY=gate" || bad "a's ARBACE_DISK_POLICY: $(cat "$T/a.pol")"
run_one b > "$T/b.out" 2>&1
[ -z "$(cat "$T/b.mem")" ] && ok "an undeclared, unrecorded step gets no GATE_STEP_MEM_GIB" || bad "b's GATE_STEP_MEM_GIB: $(cat "$T/b.mem")"
# :disk-gib on the floor: 26 GiB free, e needs 25 + 2, f 25 + 0.5
gib_kib 26 > "$T/free"
GATE_DISK_WAIT=0 run_one f > "$T/f.out" 2>&1
GATE_DISK_WAIT=0 run_one e > "$T/e.out" 2>&1
[ "$(cat "$logs/f.status")" = 0 ] && [ "$(cat "$logs/e.status")" = deferred ] && ok ":disk-gib on the floor: 26 GiB free runs a 0.5 GiB step, defers a 2 GiB one" \
  || bad ":disk-gib: f $(cat "$logs/f.status"), e $(cat "$logs/e.status")"
rm -f "$logs/disk.low-since"
# below the floor
gib_kib 10 > "$T/free"; : > "$logs/disk.log"
t0=$(date +%s); GATE_DISK_WAIT=2 run_one c > "$T/c.out" 2>&1; t1=$(date +%s)
[ "$(cat "$logs/c.status")" = deferred ] && ! pass_of c && [ $((t1 - t0)) -ge 2 ] && ok "10 GiB free: the step waited $((t1 - t0)) s, then deferred, no pass" \
  || bad "c: $(cat "$logs/c.status") after $((t1 - t0)) s"
grep -q 'the cheap prune' "$logs/disk.log" && ok "the cheap prune ran once at the low spell's start" || bad "no cheap prune: $(cat "$logs/disk.log")"
grep -q 'below the floor 25 GiB' "$logs/c.deferred" && ok "the deferral says why: $(cut -c1-60 "$logs/c.deferred")..." || bad "c.deferred: $(cat "$logs/c.deferred")"
run_one d > "$T/d.out" 2>&1
[ "$(cat "$logs/d.status")" = deferred ] && grep -q 'its need c was deferred' "$logs/d.deferred" && ok "a step whose need was deferred is deferred with it" || bad "d: $(cat "$logs/d.status")"
t0=$(date +%s); GATE_DISK_WAIT=2 rerun_one e > "$T/e.out" 2>&1; t1=$(date +%s)
[ "$(cat "$logs/e.status")" = deferred ] && [ $((t1 - t0)) -lt 2 ] && ok "a later step of the same low spell is deferred at once ($((t1 - t0)) s): the wait is the run's" || bad "e waited $((t1 - t0)) s: $(cat "$logs/e.status")"
[ "$(grep -c 'the cheap prune' "$logs/disk.log")" = 1 ] && ok "the cheap prune not run again within the spell" || bad "the cheap prune ran $(grep -c 'the cheap prune' "$logs/disk.log") times"
report c > "$T/rep.out"; report d >> "$T/rep.out"
grep -q '^   deferred  c (the disk' "$T/rep.out" && [ "$status" = 0 ] && ok "the report says deferred, and the run's status is not failed" || bad "report: $(cat "$T/rep.out") status $status"
[ "$(deferred_count)" = 3 ] && ok "deferred_count: 3 (c, d, e)" || bad "deferred_count $(deferred_count)"

# 2. the disk back: the retry
gib_kib 40 > "$T/free"
disk_retry > "$T/retry.out" 2>&1
for n in c d e; do [ "$(cat "$logs/$n.status")" = 0 ] && pass_of "$n" || bad "retry: $n $(cat "$logs/$n.status")"; done
[ "$(deferred_count)" = 0 ] && grep -q '^   ok  d' "$T/retry.out" && ok "the disk back: disk_retry ran c, d (after c) and e, passed and recorded" || bad "retry: $(cat "$T/retry.out")"

# 3. ENOSPC
run_one g > "$T/g.out" 2>&1; report g > "$T/g.rep"
[ "$(cat "$logs/g.status")" = 28 ] && ! pass_of g && grep -q 'NO SPACE LEFT ON DEVICE in g.log' "$T/g.rep" && ok "ENOSPC in a log: the step fails (28) though it exited 0, no pass" \
  || bad "g: $(cat "$logs/g.status") $(cat "$T/g.rep")"
status=0
run_one h > "$T/h.out" 2>&1; report h > "$T/h.rep"
[ "$(cat "$logs/h.status")" = 28 ] && ! pass_of h && grep -q 'in its outputs' "$T/h.rep" && ok "ENOSPC in a step's outputs (its saves): failed, no pass" || bad "h: $(cat "$logs/h.status") $(cat "$T/h.rep")"

# 4. bin/gate-policy
[ "$(GATE_STEP_MEM_GIB=2 "$here/bin/gate-policy" mem 6GiB)" = 2048MiB ] && [ "$(GATE_STEP_MEM_GIB=8 "$here/bin/gate-policy" mem 6GiB)" = 6GiB ] \
  && [ "$(env -u GATE_STEP_MEM_GIB "$here/bin/gate-policy" mem 6GiB)" = 6GiB ] && [ "$(GATE_STEP_MEM_GIB=4 "$here/bin/gate-policy" mem 1500MiB)" = 1500MiB ] \
  && ok "gate-policy mem: a default capped at the admission (6GiB at 2 -> 2048MiB), kept below it and outside the gate" || bad "gate-policy mem"
ARBACE_DISK_POLICY=gate "$here/bin/gate-policy" disk-ok 0 && ! env -u ARBACE_DISK_POLICY "$here/bin/gate-policy" disk-ok 0 2>"$T/dk.err" \
  && ok "gate-policy disk-ok: yields under the gate, stops outside it past its percent" || bad "gate-policy disk-ok"
[ $fail = 0 ] && rm -rf "$T"
exit $fail
