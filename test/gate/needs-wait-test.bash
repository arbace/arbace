#!/bin/bash
# GTS-A (doc/GATE-SURVEY.md, "As built: GTS-A"): the runner's waits for a needed step of another chain, on made-up steps
# (sleeps and false) through bin/lib/gate-run.bash's run_one, run_chain and run_queue as bin/gate runs them concurrently.
#  1. night 12's failure (three haskell-lib-* steps "not run: haskell-lib-check did not pass" while it still ran): a step
#     needing a slower step of another chain, without the wait (GATE_NEED_WAIT=0, as before GTS-A) not run, with it run
#     after its need passed; a need that fails still fails its dependant
#  2. the serial runner beside the chains (GTS216 (a)'s --full): a serial step waits for a chain's step, and back
#  3. no deadlock: a cycle of needs across two chains, and a cycle through the chains' order (a1 needs b2, b1 needs a2),
#     both fail clearly ("a cycle of waits") and at once; a need later in the step's own chain fails at once; a need in no
#     runner's queue (a later phase) at once, as before; a need whose runner ended without it, or is gone (killed), fails
#     ("never run")
# Seconds, no build, no gate run. Usage: test/gate/needs-wait-test.bash
set -u
unset audit_step audit_all GATE_CLEAN_INNER GATE_NEED_WAIT
for v in $(compgen -e); do case $v in GATE_LANES|GATE_LANES_*|GATE_LANE_*|GATE_MEM_BUDGET_GIB|GATE_MEM_LANE) unset "$v" ;; esac; done
unset ARBACE_HEAVY_HELD ARBACE_HEAVY_CMD HEAVY_LOCK_WAITS
here=$(cd "$(dirname "$0")/../.." && pwd)
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/needs-wait-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }

# run CASE "NAME PHASE CHAIN NEEDS CMD" ...: the steps as a plan (plan.tsv's columns), each phase's chains at once as
# bin/gate runs them (queue_chains, then run_chain in the background), the essential phase before the full one;
# SERIAL=1: the full phase's chain "serial" as GTS216 (a)'s serial runner (run_queue full.serial) beside the other chains
run() {
  local c=$1; shift
  (
    logs=$t/$c root=$here store=$t/$c/store; mkdir -p "$logs/plan" "$store"; : > "$logs/mem.tsv"
    GATE_HEAVY_CMD=env; . "$here/bin/lib/gate-run.bash"
    GATE_NEED_POLL=0.2
    record_pass() { :; }
    local l nm ph ch ne cm
    for l in "$@"; do
      read -r nm ph ch ne cm <<< "$l"
      printf '%s\trun\t%s\t%s\t%s\t%s\t-\tno\t1\t-\t0\t-\t-\t-\tno\t-\tcheck\t%s\n' "$nm" "k$nm" "$ph" "$ch" "$ne" "$cm"
    done > "$logs/plan.tsv"
    load_plan
    local p cc pids s
    for p in essential full; do
      pids=()
      if [ "$p" = full ] && [ -n "${SERIAL:-}" ]; then
        s=$(chain_steps full serial)
        queue_write full.serial $s
        for cc in $(chains_of full); do [ "$cc" = serial ] || queue_write "full.$cc" $(chain_steps full "$cc"); done
        for cc in $(chains_of full); do [ "$cc" = serial ] || { run_chain full "$cc" & pids+=($!); }; done
        run_queue full.serial $s
      else
        queue_chains "$p"
        for cc in $(chains_of "$p"); do run_chain "$p" "$cc" & pids+=($!); done
      fi
      [ ${#pids[@]} -eq 0 ] || wait "${pids[@]}"
    done
    for nm in "${order[@]}"; do report "$nm"; done > "$logs/report.txt"
  )
}
st() { cat "$t/$1/$2.status" 2>"$t/st.err" || echo missing; }
later() { [ "$(stat -c %Y "$t/$1/$2.done")" -ge "$(stat -c %Y "$t/$1/$3.done")" ]; }

# ---- 1. night 12's case: haskell-lib-x (chain b) needs haskell-lib-check (chain a), still running when b starts
steps1=("lib-check full a - sleep 2" "lib-x full b lib-check true" "lib-y full b lib-check true")
GATE_NEED_WAIT=0 run old "${steps1[@]}"
if [ "$(st old lib-x)" = "notrun lib-check" ] && [ "$(st old lib-y)" = "notrun lib-check" ] && [ "$(st old lib-check)" = 0 ] \
   && grep -q 'FAILED  lib-x (not run: lib-check did not pass)' "$t/old/report.txt"; then
  ok "without the wait (as before GTS-A): night 12's failure, lib-x not run while lib-check still ran"
else no "the old failure not reproduced: lib-x $(st old lib-x), lib-check $(st old lib-check)"; fi
SECONDS=0; run new "${steps1[@]}"
if [ "$(st new lib-x)" = 0 ] && [ "$(st new lib-y)" = 0 ] && later new lib-x lib-check && grep -q 'lib-x waits for lib-check (runner full.a)' "$t/new/runners/waits.log"; then
  ok "with the wait: lib-x and lib-y run after lib-check passed (${SECONDS} s)"
else no "the wait: lib-x $(st new lib-x), lib-y $(st new lib-y); $(cat "$t/new/runners/waits.log" 2>&1)"; fi
run fails "lib-check full a - sleep 1; false" "lib-x full b lib-check true"
if [ "$(st fails lib-x)" = "notrun lib-check" ] && [ "$(st fails lib-check)" = 1 ]; then ok "a need that fails after the wait fails its dependant"
else no "a failing need: lib-x $(st fails lib-x)"; fi

# ---- 2. the serial runner beside the chains, both ways
SERIAL=1 run serial "s1 full serial c1 true" "s2 full serial - sleep 1" "c1 full crop - sleep 1" "c2 full crop s2 true"
if [ "$(st serial s1)" = 0 ] && [ "$(st serial c2)" = 0 ] && later serial s1 c1 && later serial c2 s2; then
  ok "the serial runner and a chain beside it wait for each other's needed steps"
else no "serial beside chains: s1 $(st serial s1), c2 $(st serial c2)"; fi

# ---- 3. no deadlock
SECONDS=0; timeout 60 bash -c "$(declare -f run st); t=$t here=$here; run cycle 'a1 full a b1 true' 'b1 full b a1 true'"; rc=$?
if [ $rc = 0 ] && case $(st cycle a1)$(st cycle b1) in *"a cycle of waits"*) true ;; *) false ;; esac \
   && case $(st cycle a1) in notrun*) true ;; *) false ;; esac && case $(st cycle b1) in notrun*) true ;; *) false ;; esac; then
  ok "a cycle of needs across two chains: both fail, one as a cycle of waits (${SECONDS} s): $(st cycle a1 | head -c 80)"
else no "a cycle (rc $rc): a1 $(st cycle a1), b1 $(st cycle b1)"; fi
SECONDS=0; timeout 60 bash -c "$(declare -f run st); t=$t here=$here; run order 'a1 full a b2 true' 'a2 full a - true' 'b1 full b a2 true' 'b2 full b - true'"; rc=$?
if [ $rc = 0 ] && case $(st order a1)$(st order b1) in *"a cycle of waits"*) true ;; *) false ;; esac; then
  ok "a cycle through the chains' order (a1 needs b2, b1 needs a2) fails as a cycle (${SECONDS} s): a1 $(st order a1 | head -c 100); b1 $(st order b1 | head -c 60)"
else no "a cycle through the order (rc $rc): a1 $(st order a1), b1 $(st order b1)"; fi
run own "a1 full a a2 true" "a2 full a - true"
case $(st own a1) in "notrun a2: later in its own chain"*) ok "a need later in its own chain fails at once" ;; *) no "own chain: $(st own a1)" ;; esac
run phase "e1 essential a f1 true" "f1 full a - true"
if [ "$(st phase e1)" = "notrun f1" ] && [ "$(st phase f1)" = 0 ]; then ok "a need of a later phase (in no runner's queue) fails at once, as before"
else no "a later phase: e1 $(st phase e1)"; fi
# a runner that ended without its step, and one gone (its PID dead, no end): a ghost queue beside the plan's
mkdir -p "$t/ghost/runners" "$t/gone/runners"
echo g1 > "$t/ghost/runners/full.ghost.queue"; touch "$t/ghost/runners/full.ghost.end"
run ghost "a1 full a g1 true"
case $(st ghost a1) in "notrun g1: never run: its chain (full.ghost) ended without it") ok "a need whose runner ended without it: never run" ;; *) no "ghost: $(st ghost a1)" ;; esac
bash -c 'exit 0' & dead=$!; wait $dead
echo g1 > "$t/gone/runners/full.gone.queue"; echo "$dead" > "$t/gone/runners/full.gone.pid"
SECONDS=0; timeout 60 bash -c "$(declare -f run st); t=$t here=$here; run gone 'a1 full a g1 true'"; rc=$?
case $rc:$(st gone a1) in "0:notrun g1: never run: its chain's runner (full.gone, PID $dead) is gone") ok "a need whose runner is gone (killed): never run (${SECONDS} s)" ;; *) no "gone (rc $rc): $(st gone a1)" ;; esac
grep -q 'FAILED  a1 (not run: needs g1: never run' "$t/gone/report.txt" && ok "report: FAILED a1 (not run: needs g1: never run ...)" || no "report: $(cat "$t/gone/report.txt")"

echo "needs-wait-test: $((n - bad)) of $n ok"
[ "$bad" = 0 ]
