#!/bin/bash
# The cover (doc/GATE-DIET-SURVEY.md, "The cover and unit keys", GDS21-GDS24): a full-tier step's :subsumes [STEP ...] names essential checks whose
# every case it checks; in a --full, --nightly or --weekly run such a check is decided covered (no run, no failure, no pass) when every
# step that subsumes it runs whole in that run, while the essential gate still runs it. On a made-up table and store:
#  1. the essential gate runs the subsumed checks; --full covers them, a parted subsumer (every part run) too, and says by what
#  2. :subsumes is in no key (the subsumer's decl line leaves it out)
#  3. controls: --rerun, GATE_SUBSUME=0 and the clean run's inner gate (GATE_CLEAN_INNER) cover nothing; a subsumer that is skipped
#     (a pass at its key) covers nothing; --nightly with the subsumer rotated (its subset and a slice) covers nothing, --full (whole) does
#  4. the table's rules: :subsumes on an essential step, naming a full step or an essential one with :outputs, refused
#  5. the runner: a covered check's status covered, reported "covered", a need of a step that runs, no pass recorded, no failure
#  6. the checkout's table: its :subsumes valid (the planner's check), each naming a check of a lower tier
#  7. (the 11-20 audit) a :full check a :weekly step subsumes: covered in --weekly, run in --full (the subsumer not in its plan); a
#     parted essential check: each part covered
# Seconds, no build, no gate run. Usage: test/gate/subsume-test.bash
# needs: exe (the planner on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/subsume-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
bad=0 n=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"

r=$t/root; mkdir -p "$r/test/gate" "$r/bin" "$r/core" "$r/rec"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
echo 1 > "$r/test/gate/epoch"; echo c > "$r/core/z"
# the rotated subsumer's record (its universe: part p's files a, b, c) and corpora (p's subset: a)
printf '[\n ["p/a" :pass]\n ["p/b" :pass]\n ["p/c" :pass]\n]\n' > "$r/rec/u.edn"
printf '[{:id :p :subset ["a"]}]\n' > "$r/rec/corpora.edn"
table() {
cat > "$r/test/gate/steps.edn" <<EOT
{:groups {:core ["core/" "bin/tool"]}
 :steps
 [{:name e1 :mem-gib 1 :phase :essential :cmd "bin/tool" :inputs [:core] :hosts [:stage0]}
  {:name e2 :mem-gib 1 :phase :essential :cmd "bin/tool" :inputs [:core] :hosts [:stage0]}
  {:name e3 :mem-gib 1 :phase :essential :cmd "bin/tool" :inputs [:core] :hosts [:stage0]}
  {:name e4 :mem-gib 1 :phase :essential :cmd "bin/tool" :inputs [:core] :hosts [:stage0] :outputs true}
  {:name f1 :mem-gib 1 :phase :full :cmd "bin/tool" :inputs [:core] :hosts [:stage0] :subsumes [e1]}
  {:name f2 :mem-gib 1 :phase :full :cmd "bin/tool {part}" :inputs [:core] :hosts [:stage0] :subsumes [e2] :parts [{:part x} {:part y}]}
  {:name f3 :mem-gib 1 :phase :full :cmd "bin/tool {part}" :inputs [:core] :hosts [:stage0] :subsumes [e3] :parts [{:part p}]
   :rotate {:only "--only {list}" :universe "rec/u.edn" :subset "rec/corpora.edn" :failed-re "^FAIL (.+)$"}}
  {:name f4 :mem-gib 1 :phase :full :cmd "bin/tool" :inputs [:core] :hosts [:stage0] :needs [e1]}
  {:name e5 :mem-gib 1 :phase :essential :cmd "bin/tool {part}" :inputs [:core] :hosts [:stage0] :parts [{:part m} {:part n}]}
  {:name f5 :mem-gib 1 :phase :full :cmd "bin/tool" :inputs [:core] :hosts [:stage0] :subsumes [e5]}
  {:name g1 :mem-gib 1 :phase :full :cmd "bin/tool" :inputs [:core] :hosts [:stage0]}
  {:name w1 :mem-gib 1 :phase :weekly :cmd "bin/tool" :inputs [:core] :hosts [:stage0] :subsumes [g1]}
  ${1:-}]}
EOT
}
table
export GATE_STEPS=$r/test/gate/steps.edn GATE_CANARY=0; unset GATE_LOADS GATE_SUBSUME GATE_CLEAN_INNER GATE_CROP_DEFER
store=$t/store; mkdir -p "$store"
plan() { local d=$1 mode=$2; shift 2; rm -rf "$d"; mkdir -p "$d"
  (cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$d/files.sha256"; : > "$d/ancestors.txt"
  printf '{:gate "g" :stage0 "s0"}\n' > "$d/hosts.edn"
  (cd "$here" && arbace_tool -p "$here/test" -m gate.plan plan "$r" "$d" "$store" "$mode" "$@") > "$d.out" 2>&1; }
col() { awk -F'\t' -v n="$2" -v c="$3" '$1 == n {print $c}' "$1/plan.tsv"; }
dec() { local d=$1; shift; local x o=; for x in "$@"; do o="$o$x=$(col "$d" "$x" 2) "; done; echo "${o% }"; }

# ---- 1. the essential gate and --full
plan "$t/p0" essential || no "the essential plan: $(head -5 "$t/p0.out")"
[ "$(dec "$t/p0" e1 e2 e3)" = "e1=run e2=run e3=run" ] && ok "the essential gate runs the subsumed checks" || no "essential: $(dec "$t/p0" e1 e2 e3)"
plan "$t/p1" full || no "the full plan: $(head -5 "$t/p1.out")"
[ "$(dec "$t/p1" e1 e2 e3 e4 f1 f2.x f2.y f3.p f4)" = "e1=covered e2=covered e3=covered e4=run f1=run f2.x=run f2.y=run f3.p=run f4=run" ] \
  && ok "--full: e1, e2 (a parted subsumer, both parts run) and e3 (its subsumer whole) covered, e4 (no subsumer) runs" \
  || no "--full: $(dec "$t/p1" e1 e2 e3 e4 f1 f2.x f2.y f3.p f4)"
grep -q "covered *e1 *covered by f1: its run in this gate checks every case of this one" "$t/p1.out" \
  && grep -q "covered *e2 *covered by f2 (2 checks)" "$t/p1.out" && ok "the plan says by what (a parted subsumer's checks counted)" \
  || no "the why: $(grep -E ' e[12] ' "$t/p1.out")"
grep -q '5 covered' "$t/p1.out" && ok "the summary counts the covered checks" || no "the summary: $(sed -n 1p "$t/p1.out")"

# ---- 2. :subsumes is in no key
grep '^decl ' "$t/p1/plan/f1.inputs" | grep -q subsumes && no "f1's decl line holds :subsumes" || ok ":subsumes is in no key (f1's decl line leaves it out)"

# ---- 3. the controls
plan "$t/p2" full --rerun
[ "$(dec "$t/p2" e1 e2 e3)" = "e1=run e2=run e3=run" ] && ok "--rerun: nothing covered" || no "--rerun: $(dec "$t/p2" e1 e2 e3)"
GATE_SUBSUME=0 plan "$t/p3" full
[ "$(dec "$t/p3" e1 e2 e3)" = "e1=run e2=run e3=run" ] && ok "GATE_SUBSUME=0: nothing covered" || no "GATE_SUBSUME=0: $(dec "$t/p3" e1 e2 e3)"
GATE_CLEAN_INNER=1 plan "$t/p4" full
[ "$(dec "$t/p4" e1 e2 e3)" = "e1=run e2=run e3=run" ] && ok "the clean run's inner gate: nothing covered" || no "GATE_CLEAN_INNER: $(dec "$t/p4" e1 e2 e3)"
# a pass of f1 and of f2.x at their keys: f1 skipped (e1 not covered), f2 half skipped (e2 not covered)
for x in f1 f2.x; do
  k=$(sha256sum < "$t/p1/plan/$x.inputs" | cut -c1-64); mkdir -p "$store/$x"; cp "$t/p1/plan/$x.inputs" "$store/$x/$k.inputs"
  printf '{:step "%s" :key "%s" :commit "c0ffee00" :worktree "t" :date "2026-11-17T00:00:00Z" :secs 10}\n' "$x" "$k" > "$store/$x/$k.edn"
done
plan "$t/p5" full
[ "$(dec "$t/p5" f1 e1 f2.x f2.y e2 e3)" = "f1=skip e1=run f2.x=skip f2.y=run e2=run e3=covered" ] \
  && ok "a subsumer skipped (or one of its parts): its essential check runs (a pass does not hold the essential step's own inputs)" \
  || no "a skipped subsumer: $(dec "$t/p5" f1 e1 f2.x f2.y e2 e3)"
rm -rf "$store/f1" "$store/f2.x"
plan "$t/p6" nightly --night 0 || no "the nightly plan: $(head -5 "$t/p6.out")"
[ "$(dec "$t/p6" e1 e3 f3.p.sub)" = "e1=covered e3=run f3.p.sub=run" ] \
  && ok "--nightly: e3 runs (its subsumer rotated: its subset and a slice may leave the essential cases out), e1 covered" \
  || no "--nightly: $(dec "$t/p6" e1 e3 f3.p.sub) ($(awk -F'\t' '{print $1}' "$t/p6/plan.tsv" | tr '\n' ' '))"

# ---- 4. the table's rules
cp "$r/test/gate/steps.edn" "$t/good.edn"
sed -e 's/{:name e2 :mem-gib 1 :phase :essential :cmd "bin\/tool" :inputs \[:core\] :hosts \[:stage0\]}/{:name e2 :mem-gib 1 :phase :essential :cmd "bin\/tool" :inputs [:core] :hosts [:stage0] :subsumes [e3]}/' \
    -e 's/:subsumes \[e1\]}/:subsumes [e1 f4 e4]}/' "$t/good.edn" > "$r/test/gate/steps.edn"
plan "$t/p7" full; st=$?
[ $st != 0 ] && grep -q 'e2: :subsumes on a step that is not a :full or :weekly check' "$t/p7.out" \
  && grep -q 'f1: :subsumes f4, which is not a check of a lower tier' "$t/p7.out" \
  && grep -q 'f1: :subsumes e4, which is not a check of a lower tier' "$t/p7.out" \
  && ok "the planner refuses :subsumes on an essential step, naming a step of its own tier (a full step under a full one), naming one with :outputs" || no "the rules (status $st): $(head -8 "$t/p7.out")"
cp "$t/good.edn" "$r/test/gate/steps.edn"

# ---- 5. the runner
(
  logs=$t/p1; status=0
  GATE_HEAVY_CMD=env; . "$here/bin/lib/gate-run.bash"
  record_pass() { echo "$1" >> "$t/recorded"; }
  load_plan
  run_one e1; echo "rc $?" > "$t/run.rc"
  ok_status e1 && echo "ok-status yes" >> "$t/run.rc"
  report e1 > "$t/report.txt"; echo "status $status" >> "$t/run.rc"
)
[ "$(cat "$t/p1/e1.status" 2>"$t/st.err")" = covered ] && grep -qx 'rc 0' "$t/run.rc" && grep -qx 'ok-status yes' "$t/run.rc" && grep -qx 'status 0' "$t/run.rc" \
  && ok "the runner: e1's status covered, a need that holds (f4's), the run not failed" || no "the runner: $(cat "$t/p1/e1.status" "$t/run.rc" 2>&1)"
grep -q "^   covered  e1 (covered by f1" "$t/report.txt" && ok "the report: covered with its why" || no "the report: $(cat "$t/report.txt")"
[ ! -e "$t/recorded" ] && ok "no pass recorded for a covered check" || no "a pass was recorded: $(cat "$t/recorded")"

# ---- 6. the checkout's table
unset GATE_STEPS
cat > "$t/table.ae" <<EOF
(let [t (read-string (slurp "$here/test/gate/steps.edn"))
      by (into {} (for [s (:steps t)] [(str (:name s)) s]))
      pairs (for [s (:steps t) x (:subsumes s)] [(str (:name s)) (str x)])
      rank {:essential 0 :full 1 :weekly 2}
      bad (for [[f e] pairs :let [d (by e)] :when (not (and d (#{:full :weekly} (:phase (by f))) (< (rank (:phase d) 9) (rank (:phase (by f)) 9))))] [f e])]
  (println "pairs" (count pairs) (vec (map (fn [[f e]] (str f "/" e)) pairs)))
  (println "bad" (vec bad)))
EOF
"$ARBACE_HOST_EXE" "$t/table.ae" > "$t/table.out" 2>&1
grep -qx 'bad \[\]' "$t/table.out" && grep -q '^pairs [1-9]' "$t/table.out" \
  && ok "the checkout's table: every :subsumes a full or weekly step's, naming a step of a lower tier ($(sed -n 1p "$t/table.out" | cut -d' ' -f2) pairs)" \
  || no "the checkout's table: $(cat "$t/table.out")"

# ---- 7. (the 11-20 audit) a :full check under a :weekly subsumer; a parted essential check
export GATE_STEPS=$r/test/gate/steps.edn
[ "$(dec "$t/p1" e5.m e5.n g1)" = "e5.m=covered e5.n=covered g1=run" ] \
  && ok "--full: a parted essential check's parts covered (by f5); g1 runs (its :weekly subsumer w1 not in --full's plan)" \
  || no "--full: $(dec "$t/p1" e5.m e5.n g1 w1)"
plan "$t/p8" full --weekly || no "the weekly plan: $(head -5 "$t/p8.out")"
[ "$(dec "$t/p8" g1 w1 e1)" = "g1=covered w1=run e1=covered" ] && grep -q "covered *g1 *covered by w1" "$t/p8.out" \
  && ok "--weekly: g1 (a :full check) covered by w1 (a :weekly one), which runs" || no "--weekly: $(dec "$t/p8" g1 w1 e1)"

echo "subsume-test: $((n - bad)) of $n ok"
exit $((bad > 0))
