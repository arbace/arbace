#!/bin/bash
# GD3 (doc/GATE-DIET-SURVEY.md, GDS4 (b), GDS9 (a), GDS11 (a), "As built: GD3"): the planner's weekly phase, its epoch and
# its load closures on a made-up tree, table and store. A :phase :weekly step (and part) is planned with --weekly alone; a
# weekly step's :also-changed brings it into another night's plan when its files changed since its latest pass, and not when
# they did not; a :needs on a weekly step left out is dropped from the plan but kept in the key; the epoch file is the one
# logic line of a key (a change to it moves every key, a change to the planner's own file none); a load closure narrows a
# step's arbace/ files to those it read and the sources they name, and on :prog adds the families it never loaded to
# :never-loads. Seconds, no build, no gate run.
# Usage: test/gate/weekly-test.bash
# needs: exe (the planner on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/gate-weekly-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"

r=$t/root; mkdir -p "$r/test/gate" "$r/bin" "$r/arbace/a" "$r/arbace/go"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/build"
echo 1 > "$r/test/gate/epoch"
echo ';; the planner' > "$r/test/gate/plan.ae"
printf '(ns arbace.a.one (:require [arbace.a.two]))\n' > "$r/arbace/a/one.ae"
printf '(ns arbace.a.two)\n(defn f [] (require (quote arbace.a.three)))\n' > "$r/arbace/a/two.ae"
printf '(ns arbace.a.three)\n' > "$r/arbace/a/three.ae"
printf '(ns arbace.a.other)\n' > "$r/arbace/a/other.ae"
printf '(ns arbace.go.escape)\n' > "$r/arbace/go/escape.ae"
cat > "$r/test/gate/steps.edn" <<'EOF'
{:groups {:tools ["arbace/"]}
 :steps
 [{:name daily :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name wk :phase :weekly :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name wk-also :phase :weekly :cmd "bin/tool" :inputs ["bin/tool"] :hosts [] :also-changed ["bin/build"]}
  {:name parted :phase :full :cmd "bin/tool {part}" :inputs ["bin/tool"] :hosts []
   :parts [{:part every} {:part sat :phase :weekly}]}
  {:name after-wk :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts [] :needs [wk]}
  {:name tools :phase :full :cmd "bin/tool" :inputs ["bin/tool" :tools] :hosts []}
  {:name on-prog :phase :full :cmd "bin/tool" :inputs ["bin/tool" :tools] :hosts [:prog]}]}
EOF
cat > "$t/loads.edn" <<'EOF'
{"tools" {:from "test" :never [:go] :reads ["arbace/a/one.ae"]}
 "on-prog" {:from "test" :never [:go] :reads ["arbace/a/one.ae"]}}
EOF
sums() { (cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$t/files.sha256"; }
sums
printf 'arbace.a.one\t%s\narbace.go.escape\t%s\n' 1111 2222 > "$t/units.tsv"
printf '{:prog-units "%s" :prog-rest "r" :gate "g"}\n' "$t/units.tsv" > "$t/hosts.edn"
: > "$t/ancestors.txt"
store=$t/store; mkdir -p "$store"
export GATE_STEPS=$r/test/gate/steps.edn GATE_LOADS=$t/loads.edn GATE_CANARY=0
plan() { # plan DIR MODE ARGS...
  local d=$1; shift; rm -rf "$d"; mkdir -p "$d"; cp "$t/files.sha256" "$t/hosts.edn" "$t/ancestors.txt" "$d/"
  (cd "$here" && arbace_tool -p "$here/test" -m gate.plan plan "$r" "$d" "$store" "$@") > "$d.out" 2>&1
}
names() { cut -f1 "$1/plan.tsv" | tr '\n' ' '; }
col() { awk -F'\t' -v n="$2" -v c="$3" '$1 == n {print $c}' "$1/plan.tsv"; }
key() { col "$1" "$2" 3; }
pass() { # pass DIR NAME: a pass of NAME at its key in DIR's plan, its material beside it (as bin/gate records them)
  local k; k=$(key "$1" "$2"); mkdir -p "$store/$2"
  printf '{:key "%s" :commit "c0ffee" :worktree "t" :date "2026-11-13T00:00:00Z" :secs 1}\n' "$k" > "$store/$2/$k.edn"
  cp "$1/plan/$2.inputs" "$store/$2/$k.inputs"
}

# 1. the weekly phase: --full and --nightly leave the weekly steps and parts out, --weekly plans them as :full steps
plan "$t/full" full || no "the full plan: $(cat "$t/full.out")"
if [ "$(names "$t/full")" = "daily wk-also parted.every after-wk tools on-prog " ]; then ok "--full: the weekly steps and part out, wk-also in (it never passed: its :also-changed's rule)"; else no "--full: $(names "$t/full")"; fi
plan "$t/week" full --weekly || no "the weekly plan: $(cat "$t/week.out")"
if [ "$(names "$t/week")" = "daily wk wk-also parted.every parted.sat after-wk tools on-prog " ]; then ok "--weekly: the weekly steps and the weekly part planned"; else no "--weekly: $(names "$t/week")"; fi
if [ "$(col "$t/week" wk 4)" = full ]; then ok "a weekly step is planned in the full phase"; else no "wk's phase: $(col "$t/week" wk 4)"; fi
# 2. a :needs on a weekly step: dropped where it is left out, its key the same in both plans
if [ "$(col "$t/full" after-wk 6)" = - ] && [ "$(col "$t/week" after-wk 6)" = wk ]; then ok "after-wk's :needs wk dropped where wk is left out"; else no "after-wk's needs: $(col "$t/full" after-wk 6) / $(col "$t/week" after-wk 6)"; fi
if [ "$(key "$t/full" after-wk)" = "$(key "$t/week" after-wk)" ]; then ok "after-wk's key the same on the weekly night and the others"; else no "after-wk's key differs by the tier"; fi
if [ "$(key "$t/full" wk-also)" = "$(key "$t/week" wk-also)" ]; then ok "a weekly step's key the same in the weekly tier and on another night"; else no "wk-also's key differs by the tier"; fi
# 3. :also-changed: passed, nothing changed: out; its file changed: in, saying why
pass "$t/week" wk-also
plan "$t/full2" full || no "the full plan after the pass"
case " $(names "$t/full2")" in *" wk-also "*) no "wk-also planned with its files unchanged since its pass" ;; *) ok "wk-also left out: its :also-changed files unchanged since its pass" ;; esac
plan "$t/night2" nightly --night 0 || no "the nightly plan: $(cat "$t/night2.out")"
case " $(names "$t/night2")" in *" wk-also "*) no "--nightly planned wk-also unchanged" ;; *) ok "--nightly: wk-also left out too" ;; esac
printf '#!/bin/bash\nexit 1\n' > "$r/bin/build"; sums
plan "$t/full3" full || no "the full plan after the change"
case " $(names "$t/full3")" in *" wk-also "*) ok "wk-also planned after a change to its :also-changed file" ;; *) no "wk-also not planned after bin/build changed" ;; esac
if grep -q 'wk-also .*weekly, run tonight: its :also-changed files changed since its pass.*bin/build' "$t/full3.out"; then ok "the plan says why (bin/build)"; else no "no why: $(grep wk-also "$t/full3.out")"; fi
# 4. the epoch: the planner's own text is in no key, the epoch file in every one
k1=$(key "$t/full3" daily)
echo ';; the planner, changed' > "$r/test/gate/plan.ae"; sums
plan "$t/e1" full || no "the plan after the planner's change"
if [ "$(key "$t/e1" daily)" = "$k1" ]; then ok "a change to test/gate/plan.ae moves no key"; else no "the planner's text moved daily's key"; fi
echo 2 > "$r/test/gate/epoch"; sums
plan "$t/e2" full || no "the plan after the epoch's bump"
if [ "$(key "$t/e2" daily)" != "$k1" ] && [ "$(key "$t/e2" tools)" != "$(key "$t/e1" tools)" ]; then ok "the epoch's bump moves every key"; else no "the epoch's bump moved no key"; fi
if grep -qx 'logic [0-9a-f]* test/gate/epoch' "$t/e2/plan/daily.inputs" && [ "$(grep -c '^logic ' "$t/e2/plan/daily.inputs")" = 1 ]; then ok "the material's one logic line is the epoch"; else no "the logic lines: $(grep '^logic ' "$t/e2/plan/daily.inputs")"; fi
# 5. the load closure: tools reads arbace/a/one.ae: its key holds one, two (its require) and three (a require in code), not
# other or escape; on :prog, arbace.go joins :never-loads and its unit leaves the host's identity
fl() { grep '^file ' "$1/plan/$2.inputs" | awk '{print $3}' | grep '^arbace/' | tr '\n' ' '; }
if [ "$(fl "$t/e2" tools)" = "arbace/a/one.ae arbace/a/three.ae arbace/a/two.ae " ]; then ok "the closure: the read file and the sources it names, transitively"; else no "tools' arbace/ files: $(fl "$t/e2" tools)"; fi
if grep -q '^decl .*:never-loads \[arbace.go\]' "$t/e2/plan/on-prog.inputs"; then ok "on :prog, :never [:go] adds arbace.go to :never-loads"; else no "on-prog's decl: $(grep '^decl' "$t/e2/plan/on-prog.inputs")"; fi
hp1=$(grep '^host prog ' "$t/e2/plan/on-prog.inputs")
printf 'arbace.a.one\t%s\narbace.go.escape\t%s\n' 1111 3333 > "$t/units.tsv"
plan "$t/e3" full || no "the plan after goi's unit changed"
if [ "$(grep '^host prog ' "$t/e3/plan/on-prog.inputs")" = "$hp1" ]; then ok "goi's unit changed: on-prog's host identity unmoved"; else no "on-prog's identity moved with arbace.go.escape's unit"; fi
printf '(ns arbace.a.other) ;; changed\n' > "$r/arbace/a/other.ae"; sums
plan "$t/e4" full || no "the plan after other.ae changed"
if [ "$(key "$t/e4" tools)" = "$(key "$t/e3" tools)" ]; then ok "a source outside the closure moves no key"; else no "other.ae moved tools' key"; fi
printf '(ns arbace.a.three) ;; changed\n' > "$r/arbace/a/three.ae"; sums
plan "$t/e5" full || no "the plan after three.ae changed"
if [ "$(key "$t/e5" tools)" != "$(key "$t/e4" tools)" ]; then ok "a source inside the closure moves its key"; else no "three.ae did not move tools' key"; fi
GATE_LOADS=$t/none.edn plan "$t/e6" full || no "the plan without closures"
if [ "$(fl "$t/e6" tools)" = "arbace/a/one.ae arbace/a/other.ae arbace/a/three.ae arbace/a/two.ae arbace/go/escape.ae " ]; then ok "without an entry: the whole declaration"; else no "no closures: $(fl "$t/e6" tools)"; fi

echo "weekly-test: $n checks, $bad failed"
[ "$bad" = 0 ] && echo "PASS: weekly-test" || { echo "FAIL: weekly-test"; exit 1; }
