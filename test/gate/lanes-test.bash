#!/bin/bash
# GTS227 (a) (doc/GATE-SURVEY.md, "As built: GTS227 (a)"): the memory lanes on made-up steps, and the table's declarations.
#  1. the rule: bin/gate-lane --decide and gate.lanes' decide (the view's simulation) admit the same waiters on the same
#     states (the plan's order, the budget, one big step at a time, a step over the budget alone, conservative backfill)
#  2. the admission: synthetic steps (sleeps) through bin/gate-lane: the order of their admission the rule's, two big ones
#     never at once, the declared sum never over the budget, a dead entry pruned
#  3. the runner: bin/lib/gate-run.bash's step under GATE_LANES admits through bin/gate-lane, ARBACE_HEAVY_HELD=1 inside
#  4. the declarations: gate.lanes mem-check on a made-up table and records (a peak over the margin fails, a step without
#     :mem-gib fails, a parted step's whole run held to its parts' largest, a declaration far above only listed), then on
#     test/gate/steps.edn and the committed clean records (test/gate/clean-*.edn): every step declares one and holds them
#  5. the planner: a step of 16 GiB or more without :heavy refused
#  6. the view: a made-up plan simulated (the lanes, the wall), and bin/gate's options (--concurrent with --only refused)
#  7. the essential memory lane (bin/gate without --concurrent, GATE_LANES=free; "As built: the essential memory lane"):
#     its rule in both implementations (free memory, the running steps' RSS, two big steps when they fit, the head big
#     one never starved, a small one beside a big one); four big steps whose sum exceeds the free memory admitted in turn,
#     at most two at once, a small one beside them; the runner's step through the lane then the heavy lock, the lane's
#     wait apart; report's flag of a peak over its declaration; the view against the big steps one at a time
# Seconds, no build, no gate run. Usage: test/gate/lanes-test.bash
# needs: exe (gate.lanes and the planner on stage 0)
set -u
# the gate's own run (bin/gate --clean's audit_step, GATE_CLEAN_INNER, audit_all) is not the fabricated runner's
unset audit_step audit_all GATE_CLEAN_INNER
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/gate-lanes-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"
lanes() { (cd "$here" && arbace_tool -p "$here/test" -m gate.lanes "$@"); }
starttime() { local s; s=$(cat "/proc/$1/stat") || return 1; s=${s##*) }; set -- $s; echo "${20}"; }

# ---- 1. the rule, both implementations on the same states: CASE BUDGET BIG "R:GIB..." "W:NAME:GIB..." EXPECTED
me=$$ mst=$(starttime $$)
rule() {
  local name=$1 b=$2 g=$3 rs=$4 ws=$5 want=$6 d=$t/rule-$1 i=0 x got1 got2
  mkdir -p "$d/run" "$d/wait"
  for x in $rs; do i=$((i + 1)); echo "${x#R:} $mst" > "$d/run/00000-r$i.$me"; done
  i=0
  for x in $ws; do i=$((i + 1)); x=${x#W:}; echo "${x#*:} $mst" > "$d/wait/$(printf '%05d' "$i")-${x%%:*}.$me"; done
  got1=$(GATE_MEM_BUDGET_GIB=$b GATE_LANE_BIG_GIB=$g "$here/bin/gate-lane" --decide "$d" | sed 's/^[0-9]*-//; s/\.[0-9]*$//' | tr '\n' ' ')
  # shellcheck disable=SC2086
  got2=$(lanes decide "$b" "$g" $rs $ws 2>"$d.err" | tr '\n' ' ')
  if [ "$got1" = "$want" ] && [ "$got2" = "$want" ]; then ok "rule $name: admitted [$want]"
  else no "rule $name: bin/gate-lane [$got1], gate.lanes [$got2], want [$want]"; fi
}
rule empty        58 16 ""          "W:a:50 W:b:6 W:c:2"  "a b c "
rule backfill     58 16 "R:6"       "W:a:50 W:b:6 W:c:2"  "a "
rule one-big      58 16 "R:20"      "W:a:30 W:b:2"        "b "
rule alone-over   58 16 ""          "W:a:70 W:b:1"        "a "
rule over-waits   58 16 "R:2"       "W:a:70 W:b:1"        ""
rule small-beside 58 16 "R:50"      "W:a:6 W:b:3 W:c:1"   "a "
rule order        58 16 "R:53"      "W:x:4 W:y:6 W:z:1"   "x "

# ---- 2. the admission on synthetic steps: a blocker of the whole budget first, then four waiting (b1, b2 big, s1, s2
# small, in that order); the rule's order: blocker, b1 (b2 big beside it no, s1 and s2 not past b2's place), then b2 with
# s1 (6 + 3 <= 10), then s2
L=$t/lanes; mkdir -p "$L"
run_step() {  # run_step NAME GIB ORDER SECS: through bin/gate-lane, its start and end in $t/steps.log
  GATE_LANE_DIR=$L GATE_LANE_STEP=$1 GATE_LANE_GIB=$2 GATE_LANE_ORDER=$3 GATE_MEM_BUDGET_GIB=10 GATE_LANE_BIG_GIB=6 GATE_LANE_POLL=0.1 \
    "$here/bin/gate-lane" bash -c 'echo "$(date +%s%N) start $0 $1" >> "$2"; sleep "$3"; echo "$(date +%s%N) end $0 $1" >> "$2"' \
    "$1" "$2" "$t/steps.log" "$4" 2>>"$t/waits.log"
}
run_step blocker 10 0 1.2 &
pids=($!)
for i in $(seq 1 50); do [ -s "$t/steps.log" ] && break; sleep 0.1; done
run_step s2 3 4 0.6 & pids+=($!)
run_step s1 3 3 0.6 & pids+=($!)
run_step b2 6 2 0.6 & pids+=($!)
run_step b1 6 1 0.6 & pids+=($!)
wait "${pids[@]}"
order=$(awk '$2 == "start" {print $3}' "$t/steps.log" | tr '\n' ' ')
admits=$(awk -F'\t' '$2 == "admit" {print $3}' "$L/events.tsv" | tr '\n' ' ')
case $admits in
  "blocker b1 b2 s1 s2 "|"blocker b1 s1 b2 s2 ") ok "admission in the rule's order: $admits" ;;
  *) no "admission order: $admits (starts: $order); want blocker b1 {b2 s1} s2" ;;
esac
sort -n "$t/steps.log" > "$t/steps.sorted"
# the overlaps: at every start, the declared GiB of the steps running (with it) and the big ones among them
awk '{ if ($2 == "start") { run[$3] = $4; s = 0; b = 0; for (k in run) { s += run[k]; if (run[k] >= 6) b++ }
                           if (s > maxs) maxs = s; if (b > maxb) maxb = b }
       else delete run[$3] } END { print maxs, maxb }' "$t/steps.sorted" > "$t/overlap.txt" 2>&1
read -r maxs maxb < "$t/overlap.txt"
if [ "${maxs:-99}" -le 10 ] && [ "${maxb:-9}" -le 1 ]; then ok "never over the budget ($maxs of 10 GiB at most), one big step at a time"
else no "the overlaps: at most $maxs GiB, $maxb big steps at once"; fi
if grep -q 'gate-lane: s2 (3 GiB) waits' "$t/waits.log"; then ok "a waiter says so on stderr"; else no "no wait message: $(head -3 "$t/waits.log")"; fi
# a dead entry (a step killed) is pruned at the next look
echo "50 1" > "$L/run/00009-ghost.999999999"
GATE_MEM_BUDGET_GIB=10 "$here/bin/gate-lane" --state "$L" > "$t/state.txt" 2>&1
if [ ! -e "$L/run/00009-ghost.999999999" ] && ! grep -q ghost "$t/state.txt"; then ok "a dead entry pruned"; else no "the dead entry stays: $(cat "$t/state.txt")"; fi

# ---- 3. the runner's step under GATE_LANES: admitted by bin/gate-lane (events.tsv), the tools inside take no lock
(
  logs=$t/runner; mkdir -p "$logs"; : > "$logs/mem.tsv"
  GATE_HEAVY_CMD=env; . "$here/bin/lib/gate-run.bash"
  declare -A ordix=([toy]=7)
  export GATE_LANES=1 GATE_MEM_BUDGET_GIB=10 GATE_LANE_POLL=0.1
  step_lane=4 step toy - bash -c 'echo "held=${ARBACE_HEAVY_HELD:-} lane=${GATE_LANE_STEP:-}"' ; echo "status $?" > "$logs/toy.rc"
)
if grep -qx 'held=1 lane=toy' "$t/runner/toy.log" && grep -q "admit	toy	4" "$t/runner/lanes/events.tsv" && grep -qx 'status 0' "$t/runner/toy.rc"; then
  ok "the runner's step admitted through bin/gate-lane (4 GiB, ARBACE_HEAVY_HELD=1 inside)"
else no "the runner's step: $(cat "$t/runner/toy.log" "$t/runner/lanes/events.tsv" "$t/runner/toy.rc" 2>&1 | tr '\n' ' ')"; fi
(
  logs=$t/runner2; mkdir -p "$logs"; : > "$logs/mem.tsv"
  GATE_HEAVY_CMD=env; . "$here/bin/lib/gate-run.bash"
  unset GATE_LANES
  step_lane=4 step toy - bash -c 'echo "lane=${GATE_LANE_STEP:-none}"'
)
if grep -qx 'lane=none' "$t/runner2/toy.log" && [ ! -e "$t/runner2/lanes" ]; then ok "without GATE_LANES no admission"; else no "without GATE_LANES: $(cat "$t/runner2/toy.log")"; fi

# ---- 4. the declarations' check
m=$t/mem; mkdir -p "$m/test/gate" "$m/store/a" "$m/store/p.x"
cat > "$m/steps.edn" <<'EOF'
{:groups {}
 :steps
 [{:name pre1 :phase :pre :kind :always :cmd "true"}
  {:name a :mem-gib 4 :phase :essential :cmd "true"}
  {:name p :mem-gib 10 :phase :full :cmd "true" :parts [{:part x :mem-gib 2} {:part y}]}
  {:name big :heavy true :mem-gib 30 :phase :full :cmd "true"}]}
EOF
pass() { printf '{:step "%s" :key "%s" :commit "c" :dirty false :worktree "/w" :date "2026-11-04T10:00:00Z"\n :secs 10 :cpu-s 1.0 :maxrss-kib %s :tree-peak-kib %s\n :log-sha256 "%064d"}\n' "$1" "$2" "$3" "$4" 0 > "$m/store/$1/$2.edn"; }
pass a k1 4194304 nil          # a: 4.0 GiB, its declaration
pass p.x k1 1048576 2097152    # p.x: 2.0 GiB (the tree's), its own declaration
mkdir -p "$m/store/big"; pass big k1 2097152 nil   # big: 2 GiB of 30 declared (far above: listed, not failed)
echo '{:tag "t" :date "2026-11-01T00:00:00Z" :steps [{:step "p" :maxrss-kib 9437184 :tree-peak-kib 10485760} {:step "gone" :maxrss-kib 99999999}]}' > "$m/rec-ok.edn"
mc() { GATE_STEPS=$m/steps.edn GATE_ALL_PARTS=1 lanes mem-check "$m" "$@"; }
mc "$m/store" "$m/rec-ok.edn" > "$t/mc1.txt" 2>&1; st=$?
if [ $st = 0 ] && grep -q 'every declaration holds' "$t/mc1.txt" && grep -q 'far above its peaks: big' "$t/mc1.txt" \
   && ! grep -q '^   UNDER' "$t/mc1.txt"; then
  ok "mem-check: the peaks within their declarations pass (p's whole run at 10 GiB held to its parts' largest, 10; an unknown step passed over)"
else no "mem-check on peaks within: status $st: $(cat "$t/mc1.txt")"; fi
pass a k2 5767168 nil          # a: 5.5 GiB > 4 x 1.1 + 0.5 = 4.9
mc "$m/store" > "$t/mc2.txt" 2>&1; st=$?
if [ $st = 1 ] && grep -q 'UNDER  a: declared 4 GiB, recorded 5.5 GiB' "$t/mc2.txt"; then ok "mem-check: a peak over the margin fails"
else no "mem-check on a peak over: status $st: $(cat "$t/mc2.txt")"; fi
rm "$m/store/a/k2.edn"
echo '{:tag "t2" :date "2026-11-02T00:00:00Z" :steps [{:step "p.y" :maxrss-kib 12582912 :tree-peak-kib nil}]}' > "$m/rec-over.edn"
mc - "$m/rec-over.edn" > "$t/mc3.txt" 2>&1; st=$?
if [ $st = 1 ] && grep -q 'UNDER  p.y: declared 10 GiB, recorded 12.0 GiB (rec-over.edn)' "$t/mc3.txt"; then ok "mem-check: a clean record's peak over a part's inherited declaration fails"
else no "mem-check on a record's peak: status $st: $(cat "$t/mc3.txt")"; fi
sed 's/{:name a :mem-gib 4 /{:name a /' "$m/steps.edn" > "$m/steps2.edn"
GATE_STEPS=$m/steps2.edn GATE_ALL_PARTS=1 lanes mem-check "$m" - > "$t/mc4.txt" 2>&1; st=$?
if [ $st = 1 ] && grep -q 'NO :mem-gib  a' "$t/mc4.txt" && ! grep -q 'NO :mem-gib  pre1' "$t/mc4.txt"; then ok "mem-check: a step without :mem-gib fails (a :pre step need not)"
else no "mem-check without a declaration: status $st: $(cat "$t/mc4.txt")"; fi
# the table itself against the committed clean records: every declaration true
(cd "$here" && GATE_ALL_PARTS=1 arbace_tool -p "$here/test" -m gate.lanes mem-check "$here" - "$here"/test/gate/clean-*.edn) > "$t/mc-real.txt" 2>&1; st=$?
if [ $st = 0 ]; then ok "test/gate/steps.edn: $(head -1 "$t/mc-real.txt" | cut -c12-)"
else no "test/gate/steps.edn against the committed clean records: $(cat "$t/mc-real.txt")"; fi

# ---- 5. the planner refuses a big step without :heavy
r=$t/root; mkdir -p "$r/test" "$r/bin"; printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
cat > "$r/test/steps.edn" <<'EOF'
{:groups {}
 :steps
 [{:name small :mem-gib 15 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name huge :mem-gib 20 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name huge-ok :heavy :inner :mem-gib 50 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}]}
EOF
pd=$t/plan; mkdir -p "$pd" "$t/pstore"
(cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$pd/files.sha256"
echo '{}' > "$pd/hosts.edn"; : > "$pd/ancestors.txt"
(cd "$here" && GATE_STEPS=$r/test/steps.edn GATE_CANARY=0 arbace_tool -p "$here/test" -m gate.plan plan "$r" "$pd" "$t/pstore" full) > "$t/plan.out" 2>&1; st=$?
if [ $st != 0 ] && grep -q 'huge: :mem-gib 20 (16 or more: lane 1) without :heavy' "$t/plan.out" && ! grep -q 'small:\|huge-ok:' "$t/plan.out"; then
  ok "the planner refuses a step of 16 GiB or more without :heavy"
else no "the planner on a big light step: status $st: $(cat "$t/plan.out")"; fi

# ---- 6. the view on a made-up plan: pre 60 s, a build 100 s; essential: e1 (20 GiB, 300 s) and e2 (2 GiB, 100 s) at once;
# full: chain j (j1 50 GiB 1000 s, j2 50 GiB 1000 s), chain c (c1 6 GiB 600 s, c2 9 GiB 600 s), chain g (g1 20 GiB 500 s,
# then a skipped step). Budget 58, big 16: e1 and e2 at once (300 s, from 160); the full phase from 460: j1 with c1 (56 GiB),
# g1 waits (big); at 1060 c2 waits (50 + 9 = 59); at 1460 j2 first (the plan's order), c2 and g1 still wait; at 2460 c2
# and g1 together (29); the end at 3060 (0:51)
v=$t/view; mkdir -p "$v"
row() { printf '%s\t%s\t-\t%s\t%s\t-\t%s\t%s\t%s\t-\t-\t-\t-\t-\t-\t-\tcheck\ttrue\n' "$@"; }
{ row pre1 always pre pre1 - - -
  row b1 build build b1 - yes 20
  row e1 run essential e1 - yes 20
  row e2 run essential e2 - - 2
  row j1 run full j - inner 50
  row j2 run full j - inner 50
  row c1 run full c - yes 6
  row c2 run full c - yes 9
  row g1 run full g - yes 20
  row s1 skip full g - - 1; } > "$v/plan.tsv"
echo '{:tag "v" :date "2026-11-04T00:00:00Z" :steps [{:step "pre1" :wall-s 60} {:step "b1" :wall-s 100} {:step "e1" :wall-s 300} {:step "e2" :wall-s 100} {:step "j1" :wall-s 1000} {:step "j2" :wall-s 1000} {:step "c1" :wall-s 600} {:step "c2" :wall-s 600} {:step "g1" :wall-s 500}]}' > "$v/rec.edn"
lanes view "$v" - 58 16 "$v/rec.edn" > "$t/view.txt" 2>&1; st=$?
if [ $st = 0 ] && grep -q 'simulated wall 0:51 (the pre steps and builds 0:02)' "$t/view.txt" \
   && grep -qE '^ +0:24 +0:41 +1 +50 +j2 ' "$t/view.txt" && grep -qE '^ +0:41 +0:51 +2 +9 +c2 ' "$t/view.txt" \
   && grep -qE '^ +0:41 +0:49 +1 +20 +g1 ' "$t/view.txt" && grep -q 'at most 56 GiB at once' "$t/view.txt"; then
  ok "the view simulates the lanes (j2 then g1 beside c2; wall 0:51)"
else no "the view: status $st: $(cat "$t/view.txt")"; fi
# bin/gate: --concurrent with --only refused; GATE_LANES=0 with --clean refused
(cd "$here" && bin/gate --full --clean --concurrent --only x) > "$t/opt1.txt" 2>&1; st1=$?
(cd "$here" && GATE_LANES=0 bin/gate --full --clean --concurrent --plan) > "$t/opt2.txt" 2>&1; st2=$?
if [ $st1 = 2 ] && [ $st2 = 2 ]; then ok "bin/gate: --concurrent --only and --clean with GATE_LANES=0 refused"
else no "bin/gate's options: $st1 $st2: $(head -2 "$t/opt1.txt" "$t/opt2.txt")"; fi

# ---- 7. the essential memory lane
# 7a. the rule, both implementations: CASE AVAIL BIG "R:GIB[:USED]..." "W:NAME:GIB..." EXPECTED (AVAIL: MemAvailable
# less the margin, in GiB; USED: the running step's tree now, GiB)
rule_free() {
  local name=$1 a=$2 g=$3 rs=$4 ws=$5 want=$6 d=$t/free-$1 i=0 x gb u got1 got2
  mkdir -p "$d/run" "$d/wait"
  for x in $rs; do i=$((i + 1)); x=${x#R:}; gb=${x%%:*}; u=0; case $x in *:*) u=${x#*:} ;; esac
    echo "$gb $mst $(awk -v u="$u" 'BEGIN {printf "%d", u * 1048576}')" > "$d/run/00000-r$i.$me"; done
  i=0
  for x in $ws; do i=$((i + 1)); x=${x#W:}; echo "${x#*:} $mst" > "$d/wait/$(printf '%05d' "$i")-${x%%:*}.$me"; done
  got1=$(GATE_LANE_MODE=free GATE_LANE_MARGIN_GIB=0 GATE_LANE_MEMAVAIL_KIB=$(awk -v a="$a" 'BEGIN {printf "%d", a * 1048576}') \
         GATE_LANE_BIG_GIB=$g "$here/bin/gate-lane" --decide "$d" | sed 's/^[0-9]*-//; s/\.[0-9]*$//' | tr '\n' ' ')
  # shellcheck disable=SC2086
  got2=$(lanes decide-free "$a" "$g" $rs $ws 2>"$d.err" | tr '\n' ' ')
  if [ "$got1" = "$want" ] && [ "$got2" = "$want" ]; then ok "free rule $name: admitted [$want]"
  else no "free rule $name: bin/gate-lane [$got1], gate.lanes [$got2], want [$want]"; fi
}
rule_free four-bigs     56 16 ""          "W:dc:31 W:jc:22 W:sa:20 W:pg:21"  "dc jc "
rule_free two-fit       56 16 "R:31"      "W:jc:22 W:sa:20 W:s:2"            "jc s "
rule_free small-beside  56 16 "R:31"      "W:jc:30 W:s:2"                    "s "
rule_free rss-counts    30 16 "R:20:15"   "W:b:22"                           "b "
rule_free rss-unknown   30 16 "R:20"      "W:b:22"                           ""
rule_free head-big      30 16 "R:10 R:10" "W:b:25 W:s:1"                     "b "
rule_free head-reserved 30 16 "R:3"       "W:b:40 W:s:2"                     ""
rule_free alone-over    10 16 ""          "W:b:40 W:s:1"                     "b "
rule_free greedy-small  40 16 "R:31"      "W:b:22 W:s:3 W:t:9"               "s "

# 7b. the admission: a blocker of the whole free memory (13 GiB) first, then four big steps (6 GiB each, big from 5: 24
# GiB, over the 13 free) and a small one (1 GiB) waiting; then b1 and b2 at once (12), s1 beside them (13), b3 and b4 in
# turn as room comes back; never two big steps beyond what fits, never over the free memory
F=$t/free; mkdir -p "$F"
free_step() {  # free_step NAME GIB ORDER SECS
  GATE_LANE_MODE=free GATE_LANE_MARGIN_GIB=0 GATE_LANE_MEMAVAIL_KIB=$((13 * 1048576)) GATE_LANE_DIR=$F GATE_LANE_STEP=$1 \
    GATE_LANE_GIB=$2 GATE_LANE_ORDER=$3 GATE_LANE_BIG_GIB=5 GATE_LANE_POLL=0.1 GATE_LANE_WAITS=$t/free-$1.wait \
    "$here/bin/gate-lane" bash -c 'echo "$(date +%s%N) start $0 $1" >> "$2"; sleep "$3"; echo "$(date +%s%N) end $0 $1" >> "$2"' \
    "$1" "$2" "$t/free.log" "$4" 2>>"$t/free-waits.log"
}
free_step blocker 13 0 1.2 &
pids=($!)
for i in $(seq 1 50); do [ -s "$t/free.log" ] && break; sleep 0.1; done
free_step s1 1 5 2.0 & pids+=($!)
free_step b4 6 4 0.6 & pids+=($!)
free_step b3 6 3 0.6 & pids+=($!)
free_step b2 6 2 2.0 & pids+=($!)
free_step b1 6 1 2.0 & pids+=($!)
wait "${pids[@]}"
admits=$(awk -F'\t' '$2 == "admit" {print $3}' "$F/events.tsv" | tr '\n' ' ')
sort -n "$t/free.log" > "$t/free.sorted"
awk '{ if ($2 == "start") { run[$3] = $4; s = 0; b = 0; for (k in run) { s += run[k]; if (run[k] >= 5) b++ }
                           if (s > maxs) maxs = s; if (b > maxb) maxb = b
                           if ("s1" in run) for (k in run) if (run[k] >= 5 && k != "blocker") beside = 1 }
       else delete run[$3] } END { print maxs, maxb, beside + 0 }' "$t/free.sorted" > "$t/free-overlap.txt" 2>&1
read -r maxs maxb beside < "$t/free-overlap.txt"
# the admissions of one decision land in the order their waiters look (the same second): the rounds compared as sets
read -r -a ad <<< "$admits"
mid=$(printf '%s\n' "${ad[@]:1:3}" | LC_ALL=C sort | tr '\n' ' ')
admits="${ad[0]:-} {${mid% }} ${ad[*]:4}"
case "$admits " in
  "blocker {b1 b2 s1} b3 b4 "|"blocker {b1 b2 s1} b4 b3 ") ok "free lane: four big steps over the free memory admitted in turn ($admits)" ;;
  *) no "free lane: admission order $admits; want blocker {b1 b2 s1} b3 b4" ;;
esac
if [ "${maxs:-99}" -le 13 ] && [ "${maxb:-9}" -eq 2 ] && [ "${beside:-0}" = 1 ]; then
  ok "free lane: never over the free memory ($maxs of 13 GiB), two big steps at once when they fit, the small one beside them"
else no "free lane: at most $maxs GiB, $maxb big steps at once, small beside a big one: ${beside:-?}"; fi
if [ "$(awk -F'\t' '$2 == "admit" && NF == 6 && $6 == 13631488' "$F/events.tsv" | wc -l)" = 6 ] && [ -s "$t/free-b3.wait" ] \
   && grep -q 'gate-lane: b3 (6 GiB) waits for room in the lanes (MemAvailable 13.0 GiB' "$t/free-waits.log"; then
  ok "free lane: the events with MemAvailable, the wait in GATE_LANE_WAITS, said on stderr"
else no "free lane's events and waits: $(cat "$F/events.tsv" "$t/free-waits.log" | head -12 | tr '\n' ' ')"; fi
# the real process trees: a running entry whose process holds memory holds back only the rest of its declaration
(
  d=$t/free-real; mkdir -p "$d/run" "$d/wait"
  # a process of up to about 200 MiB (awk's array), declared 1 GiB; once it holds U (at least 50 MiB; under strace it
  # grows slowly), MemAvailable set so that ROOM fits a waiter of U/2 only when the U it holds counts: A = 1 GiB - U/2 + U/8
  awk 'BEGIN { s = "x"; while (length(s) < 1048576) s = s s; for (i = 0; i < 200; i++) a[i] = s i; system("sleep 30") }' &
  p=$!
  for i in $(seq 1 300); do [ "$(awk '/^VmRSS:/ {print $2}' "/proc/$p/status" 2>"$d/rss.err" || echo 0)" -ge 51200 ] 2>"$d/rss.err" && break; sleep 0.1; done
  u=$(awk '/^VmRSS:/ {print $2}' "/proc/$p/status" 2>"$d/rss.err")
  w=$(awk -v u="$u" 'BEGIN {printf "%.6f", u / 2 / 1048576}')
  av=$(awk -v u="$u" 'BEGIN {printf "%d", 1048576 - u / 2 + u / 8}')
  echo "1 $(starttime $p)" > "$d/run/00000-hog.$p"; echo "$w $mst" > "$d/wait/00001-w.$me"
  r=$(GATE_LANE_MODE=free GATE_LANE_MARGIN_GIB=0 GATE_LANE_MEMAVAIL_KIB=$av "$here/bin/gate-lane" --decide "$d" | sed 's/^[0-9]*-//; s/\.[0-9]*$//')
  echo "1 $(starttime $p) 0" > "$d/run/00000-hog.$p"
  r0=$(GATE_LANE_MODE=free GATE_LANE_MARGIN_GIB=0 GATE_LANE_MEMAVAIL_KIB=$av "$here/bin/gate-lane" --decide "$d" | sed 's/^[0-9]*-//; s/\.[0-9]*$//')
  # stopped by PID: its sleep's shell and the sleep (its children), then awk
  kids=$(cat "/proc/$p/task/$p/children" 2>"$d/kids.err"); gk=
  for k in $kids; do gk="$gk $(cat "/proc/$k/task/$k/children" 2>"$d/kids.err")"; done
  # shellcheck disable=SC2086
  kill $gk $kids "$p" 2>"$d/kill.err"; wait "$p" 2>"$d/kill.err"; echo "$r|$r0" > "$t/free-real.txt"; echo "$u" > "$t/free-real.kib"
)
if [ "$(cat "$t/free-real.txt")" = "w|" ]; then ok "free lane: a running step's process tree RSS read from /proc ($(( $(cat "$t/free-real.kib") / 1024 )) MiB held: only its declaration's rest held back)"
else no "free lane on a real process: [$(cat "$t/free-real.txt")], want [w|]"; fi

# 7c. the runner's step under GATE_LANES=free: through bin/gate-lane's free mode first, then GATE_HEAVY_CMD for a heavy
# step (a fake lock that notes itself), not for a light one; the tools inside hold the lock only under it; the lane's
# wait in NAME.lane-wait
printf '#!/bin/bash\necho "$(date +%%s%%N) lock" >> "%s"\nexec "$@"\n' "$t/lock.log" > "$t/fakelock"; chmod +x "$t/fakelock"
(
  logs=$t/runner3; mkdir -p "$logs"; : > "$logs/mem.tsv"
  GATE_HEAVY_CMD=$t/fakelock; . "$here/bin/lib/gate-run.bash"
  declare -A ordix=([hv]=3 [lt]=4)
  export GATE_LANES=free GATE_LANE_POLL=0.1
  step_lane=4 step hv yes bash -c 'echo "held=${ARBACE_HEAVY_HELD:-} mode=${GATE_LANE_MODE:-}"'; echo "status $?" > "$logs/hv.rc"
  step_lane=2 step lt "" bash -c 'echo "held=${ARBACE_HEAVY_HELD:-}"'
)
if grep -qx 'held=1 mode=free' "$t/runner3/hv.log" && grep -qx 'held=' "$t/runner3/lt.log" && grep -qx 'status 0' "$t/runner3/hv.rc" \
   && [ "$(wc -l < "$t/lock.log")" = 1 ] && grep -q "admit	hv	4" "$t/runner3/lanes/events.tsv" && grep -q "admit	lt	2" "$t/runner3/lanes/events.tsv" \
   && [ -f "$t/runner3/hv.lane-wait" ]; then
  ok "the runner's step in the free lane: admitted, then the heavy lock for the heavy step alone (ARBACE_HEAVY_HELD=1 inside it only)"
else no "the runner in the free lane: $(cat "$t/runner3/hv.log" "$t/runner3/lt.log" "$t/lock.log" "$t/runner3/lanes/events.tsv" 2>&1 | tr '\n' ' ')"; fi
# report's flag: a peak over the declaration by the margin (3 GiB of 1 declared), not one within (1.5 of 1: 1.6)
(
  logs=$t/runner4; mkdir -p "$logs"
  . "$here/bin/lib/gate-run.bash"
  for x in "over 3145728" "within 1572864"; do
    set -- $x
    memgib[$1]=1 budget[$1]=- sum_re[$1]=- sum_n[$1]=0 why[$1]=
    echo 0 > "$logs/$1.status"; echo 10 > "$logs/$1.secs"; echo 0 > "$logs/$1.wait"
    printf '{:step "%s" :wall-s 10 :cpu-s 1.0 :maxrss-kib %s :tree-peak-kib 0 :wait-s 0 :inner-wait-s 0}\n' "$1" "$2" > "$logs/$1.mem"
    report "$1"
  done > "$logs/report.txt" 2>&1
)
if grep -q '^   ok  over (.*OVER ITS DECLARED :mem-gib 1' "$t/runner4/report.txt" && grep -q '^   ok  within' "$t/runner4/report.txt" \
   && ! grep -q 'within.*OVER' "$t/runner4/report.txt"; then ok "report flags a peak over its :mem-gib (a warning, the step still ok)"
else no "report's flag: $(cat "$t/runner4/report.txt")"; fi

# 7d. the view in the free mode, the made-up plan of 6 with 76 GiB free: j1, c1 and g1 at once (two big ones: 76), c2 at
# 1060, j2 at 1460 (room 67), the end at 2460 (0:41); the big steps one at a time 0:49, every step at once 0:41
GATE_LANE_MODE=free lanes view "$v" - 76 16 "$v/rec.edn" > "$t/view-free.txt" 2>&1; st=$?
if [ $st = 0 ] && grep -q 'the essential memory lane: 76 GiB free' "$t/view-free.txt" && grep -q 'simulated wall 0:41' "$t/view-free.txt" \
   && grep -qE '^ +0:07 +0:16 +1 +20 +g1 ' "$t/view-free.txt" && grep -qE '^ +0:24 +0:41 +1 +50 +j2 ' "$t/view-free.txt" \
   && grep -q 'against the big steps one at a time 0:49 (not longer), every step at once 0:41' "$t/view-free.txt" \
   && grep -q 'at most 76 GiB at once' "$t/view-free.txt"; then
  ok "the view in the free lane (g1 beside j1, wall 0:41; the big steps one at a time 0:49)"
else no "the free view: status $st: $(cat "$t/view-free.txt")"; fi
# bin/gate: the essential lane is the ordinary gate's (GATE_LANES=free), GATE_MEM_LANE=0 none, --concurrent GTS227 (a)'s
if grep -q 'mem_lane=1; export GATE_LANES=free' "$here/bin/gate" && grep -q '"${GATE_MEM_LANE:-1}" != 0' "$here/bin/gate"; then
  ok "bin/gate: the essential memory lane on by default without --concurrent (GATE_MEM_LANE=0 off)"
else no "bin/gate does not set the essential lane"; fi

echo "lanes-test: $n checks, $bad failed"
[ "$bad" = 0 ]
