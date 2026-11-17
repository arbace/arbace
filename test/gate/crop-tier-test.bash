#!/bin/bash
# The crops tier (the user, 2026-11-17; doc/GATE-DIET-SURVEY.md, "As built: the crops tier"): a crop's essential subset
# (:essential-when-changed CROP, the table's :crop-paths CROP) runs in the essential gate only when its crop's own paths changed
# since its latest pass, else the planner defers it to the night's gate (the decision night). On a made-up table and store:
#  1. the planner: stage 0 moved (a re-seed), the crops' paths not: the core step runs, the crop steps are night, the build only
#     they need none; their why names the move; the crop's own file outside :inputs is in the key (the rules join it)
#  2. a crop's file changed: that crop's steps run, the other's stay night
#  3. controls of the tier: --full, --rerun and GATE_CROP_DEFER=0 defer nothing; a step that never passed runs, and a deferred
#     step it needs is woken (runs as decided)
#  4. the table's rules: :essential-when-changed on a full step or naming no :crop-paths entry, a :crop-paths rule matching no file
#  5. the runner: a night step's status is night, reported "night", a need of a step that runs, no pass recorded, no failure
#  6. the checkout's table: every new crop's essential check in the tier, none of the core's
# Seconds, no build, no gate run. Usage: test/gate/crop-tier-test.bash
# needs: exe (the planner on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/crop-tier-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
bad=0 n=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"

r=$t/root; mkdir -p "$r/test/gate" "$r/bin" "$r/a" "$r/b" "$r/core"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
echo 1 > "$r/test/gate/epoch"; echo a > "$r/a/x"; echo frag > "$r/a/fragment.edn"; echo b > "$r/b/y"; echo c > "$r/core/z"
table() {
cat > "$r/test/gate/steps.edn" <<EOT
{:groups {:core ["core/" "bin/tool"]}
 :steps
 [{:name build1 :phase :build :kind :build :cmd "bin/tool" :hosts [:stage0]}
  {:name core1 :mem-gib 1 :phase :essential :cmd "bin/tool" :inputs [:core] :hosts [:stage0]}
  {:name a1 :mem-gib 1 :phase :essential :chain a :cmd "bin/tool" :inputs [:core "a/x"] :hosts [:stage0] :essential-when-changed :a}
  {:name a2 :mem-gib 1 :phase :essential :chain a :needs [a1] :cmd "bin/tool" :inputs [:core "a/x"] :hosts [:stage0] :essential-when-changed :a}
  {:name b1 :mem-gib 1 :phase :essential :chain b :needs [build1] :cmd "bin/tool" :inputs [:core "b/"] :hosts [:stage0] :essential-when-changed :b}
  ${1:-}]
 :crop-paths {:a ["a/"] :b ["b/"]}}
EOT
}
table
export GATE_STEPS=$r/test/gate/steps.edn GATE_CANARY=0; unset GATE_LOADS GATE_CROP_DEFER GATE_CLEAN_INNER
store=$t/store; mkdir -p "$store"
plan() { local d=$1 s0=$2 mode=$3; shift 3; rm -rf "$d"; mkdir -p "$d"
  (cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$d/files.sha256"; : > "$d/ancestors.txt"
  printf '{:gate "g" :stage0 "%s"}\n' "$s0" > "$d/hosts.edn"
  (cd "$here" && arbace_tool -p "$here/test" -m gate.plan plan "$r" "$d" "$store" "$mode" "$@") > "$d.out" 2>&1; }
col() { awk -F'\t' -v n="$2" -v c="$3" '$1 == n {print $c}' "$1/plan.tsv"; }
dec() { local d=$1; shift; local x o=; for x in "$@"; do o="$o$x=$(col "$d" "$x" 2) "; done; echo "${o% }"; }
# the passes: each check of a plan recorded at its key, as the runner records it
passes() { local d=$1 x k
  for x in core1 a1 a2 b1 "$@"; do
    [ -f "$d/plan/$x.inputs" ] || continue
    k=$(sha256sum < "$d/plan/$x.inputs" | cut -c1-64); mkdir -p "$store/$x"; cp "$d/plan/$x.inputs" "$store/$x/$k.inputs"
    printf '{:step "%s" :key "%s" :commit "c0ffee00" :worktree "t" :date "2026-11-17T00:00:00Z" :secs 10}\n' "$x" "$k" > "$store/$x/$k.edn"
  done; }

# ---- 1. a re-seed: stage 0 moved, the crops' paths not
plan "$t/p0" old essential || no "the first plan: $(head -5 "$t/p0.out")"
[ "$(dec "$t/p0" core1 a1 a2 b1)" = "core1=run a1=run a2=run b1=run" ] && ok "no pass yet: every check runs (a crop step that never passed is not deferred)" \
  || no "no pass: $(dec "$t/p0" core1 a1 a2 b1)"
grep -qx "file $(sha256sum < "$r/a/fragment.edn" | cut -c1-64) a/fragment.edn" "$t/p0/plan/a1.inputs" \
  && ok "the crop's own file outside :inputs is in the key (its :crop-paths rules join it)" || no "a/fragment.edn not in a1's material"
! grep -q ' a/' "$t/p0/plan/b1.inputs" && ok "another crop's paths are not" || no "b1's material holds a/"
passes "$t/p0"
plan "$t/p1" new essential || no "the re-seed's plan: $(head -5 "$t/p1.out")"
[ "$(dec "$t/p1" core1 a1 a2 b1 build1)" = "core1=run a1=night a2=night b1=night build1=none" ] \
  && ok "stage 0 moved: core1 runs, a1, a2 and b1 night, build1 (only b1 needs it) not built" || no "the re-seed: $(dec "$t/p1" core1 a1 a2 b1 build1)"
grep -q "night  *a1 .*deferred to the night's gate: no change to the crop's paths (a) since its pass at .*its key moved by host stage0" "$t/p1.out" \
  && ok "the plan explains: deferred, no change to the crop's paths, its key moved by host stage0" || no "a1's why: $(grep ' a1 ' "$t/p1.out")"
grep -q '3 night' "$t/p1.out" && grep -q 'crops tier: 3 crop subsets, 3 deferred' "$t/p1.out" && ok "the summary counts the night steps" \
  || no "the summary: $(head -3 "$t/p1.out")"
[ -n "$(col "$t/p1" a1 3)" ] && [ "$(col "$t/p1" a1 3)" != - ] && ok "a night step keeps its key (bin/gate --why)" || no "a1's key: $(col "$t/p1" a1 3)"

# ---- 2. a crop's file changed
echo a2 > "$r/a/x"
plan "$t/p2" new essential
[ "$(dec "$t/p2" core1 a1 a2 b1)" = "core1=run a1=run a2=run b1=night" ] && ok "a/x changed: a1 and a2 run, b1 stays night" || no "a/x changed: $(dec "$t/p2" core1 a1 a2 b1)"
grep -q "run  *a1 *crops tier (a): its crop's paths changed since its pass at .*: a/x" "$t/p2.out" && ok "the plan names the crop's file that changed" \
  || no "a1's why: $(grep ' a1 ' "$t/p2.out")"
echo frag2 > "$r/a/fragment.edn"; echo a > "$r/a/x"
plan "$t/p2b" new essential
[ "$(dec "$t/p2b" a1 b1)" = "a1=run b1=night" ] && ok "a crop file outside :inputs (its fragment) changed: a1 runs" || no "the fragment changed: $(dec "$t/p2b" a1 b1)"
echo frag > "$r/a/fragment.edn"

# ---- 3. the tier's controls
plan "$t/p3" new full
[ "$(dec "$t/p3" core1 a1 a2 b1 build1)" = "core1=run a1=run a2=run b1=run build1=build" ] && ok "--full: nothing deferred (keyed as before)" || no "--full: $(dec "$t/p3" core1 a1 a2 b1 build1)"
plan "$t/p4" new essential --rerun
[ "$(dec "$t/p4" a1 a2 b1)" = "a1=run a2=run b1=run" ] && ok "--rerun: nothing deferred" || no "--rerun: $(dec "$t/p4" a1 a2 b1)"
GATE_CROP_DEFER=0 plan "$t/p5" new essential
[ "$(dec "$t/p5" a1 a2 b1)" = "a1=run a2=run b1=run" ] && ok "GATE_CROP_DEFER=0: nothing deferred" || no "GATE_CROP_DEFER=0: $(dec "$t/p5" a1 a2 b1)"
table '{:name b2 :mem-gib 1 :phase :essential :chain b :needs [b1] :cmd "bin/tool" :inputs [:core "b/"] :hosts [:stage0] :essential-when-changed :b}'
plan "$t/p6" new essential
[ "$(dec "$t/p6" b1 b2 build1 a1)" = "b1=run b2=run build1=build a1=night" ] && ok "b2 never passed: it runs, its need b1 woken (and b1's build), a1 still night" \
  || no "the wake: $(dec "$t/p6" b1 b2 build1 a1)"
grep -q "run  *b1 *crops tier: a step that runs needs it" "$t/p6.out" && ok "the woken step's why says so" || no "b1's why: $(grep ' b1 ' "$t/p6.out")"
table

# ---- 4. the table's rules
cp "$r/test/gate/steps.edn" "$t/good.edn"
sed -e 's/{:name core1 :mem-gib 1 :phase :essential/{:name core1 :mem-gib 1 :phase :full :essential-when-changed :a/' \
    -e 's/:essential-when-changed :b}/:essential-when-changed :zz}/' -e 's/:b \["b\/"\]/:b ["b\/" "nothing\/"]/' "$t/good.edn" > "$r/test/gate/steps.edn"
plan "$t/p7" new essential; st=$?
[ $st != 0 ] && grep -q 'core1: :essential-when-changed on a step that is not an essential check' "$t/p7.out" \
  && grep -q 'b1: :essential-when-changed :zz names no :crop-paths entry' "$t/p7.out" && grep -q ':crop-paths :b: input rule nothing/ matches no file' "$t/p7.out" \
  && ok "the planner refuses a full step in the tier, an unknown crop, a :crop-paths rule matching no file" || no "the rules (status $st): $(head -8 "$t/p7.out")"
cp "$t/good.edn" "$r/test/gate/steps.edn"

# ---- 5. the runner
(
  logs=$t/p1; status=0
  GATE_HEAVY_CMD=env; . "$here/bin/lib/gate-run.bash"
  record_pass() { echo "$1" >> "$t/recorded"; }
  load_plan
  run_one a1; echo "rc $?" > "$t/run.rc"
  ok_status a1 && echo "ok-status yes" >> "$t/run.rc"
  report a1 > "$t/report.txt"; echo "status $status" >> "$t/run.rc"
)
[ "$(cat "$t/p1/a1.status" 2>"$t/st.err")" = night ] && grep -qx 'rc 0' "$t/run.rc" && grep -qx 'ok-status yes' "$t/run.rc" && grep -qx 'status 0' "$t/run.rc" \
  && ok "the runner: a1's status night, a need that holds, the run not failed" || no "the runner: $(cat "$t/p1/a1.status" "$t/run.rc" 2>&1)"
grep -q "^   night  a1 (deferred to the night's gate" "$t/report.txt" && ok "the report: night with its why" || no "the report: $(cat "$t/report.txt")"
[ ! -e "$t/recorded" ] && ok "no pass recorded for a night step" || no "a pass was recorded: $(cat "$t/recorded")"

# ---- 6. the checkout's table
unset GATE_STEPS
cat > "$t/table.ae" <<EOF
(let [t (read-string (slurp "$here/test/gate/steps.edn"))
      nc (set (get-in t [:budgets :new-crops]))
      co (into {} (for [[c ns] (:crops t) n ns] [(str n) c]))
      ess (filter #(and (= :essential (:phase %)) (contains? #{nil :check} (:kind %))) (:steps t))
      tier (filter :essential-when-changed ess)]
  (println "untiered-new" (vec (for [s ess :when (and (nc (co (str (:name s)))) (not (:essential-when-changed s)))] (str (:name s)))))
  (println "tiered-core" (vec (for [s tier :when (#{:shared :clojure :java :go} (co (str (:name s))))] (str (:name s)))))
  (println "tier" (count tier) "core" (- (count ess) (count tier))))
EOF
"$ARBACE_HOST_EXE" "$t/table.ae" > "$t/table.out" 2>&1
grep -qx 'untiered-new \[\]' "$t/table.out" && grep -qx 'tiered-core \[\]' "$t/table.out" \
  && ok "the checkout's table: every new crop's essential check in the tier, no step of the four's core or :shared ($(sed -n 3p "$t/table.out"))" \
  || no "the checkout's table: $(cat "$t/table.out")"

echo "crop-tier-test: $((n - bad)) of $n ok"
exit $((bad > 0))
