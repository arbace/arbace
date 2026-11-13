#!/bin/bash
# GD4 (doc/GATE-DIET-SURVEY.md, GDS13 (a), "As built: GD4"): the planner's admission by the recorded peak on a made-up table and
# store: a step's MEM column (plan.tsv's 9th, what bin/gate-lane admits it by and GATE_STEP_MEM_GIB) is the larger of its
# :mem-gib and 1.1 x its passes' largest peak (the larger of :maxrss-kib and :tree-peak-kib, up to half a GiB); DECLARED and
# DISK-GIB (the 18th and 19th) are the table's :mem-gib and :disk-gib; a step over its declaration by the margin (10% and
# 0.5 GiB) in two passes is printed UNDER with the figure to declare, one in a single pass is not; :disk-gib moves no key.
# Seconds, no build, no gate run. Usage: test/gate/admit-test.bash
# needs: exe (the planner on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/gate-admit-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
bad=0
ok() { echo "ok   $*"; }
no() { bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"
r=$t/root; mkdir -p "$r/test/gate" "$r/bin"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
echo 1 > "$r/test/gate/epoch"
table() { cat > "$r/test/gate/steps.edn" <<EOT
{:groups {}
 :steps
 [{:name low :mem-gib 2 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name high :mem-gib 8 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name once :mem-gib 2 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name bare :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}
  {:name disk :mem-gib 1 $1 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}]}
EOT
}
table ":disk-gib 3"
(cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$t/files.sha256"
printf '{:gate "g"}\n' > "$t/hosts.edn"; : > "$t/ancestors.txt"
store=$t/store; mkdir -p "$store"
export GATE_STEPS=$r/test/gate/steps.edn GATE_CANARY=0; unset GATE_LOADS
plan() { local d=$1; rm -rf "$d"; mkdir -p "$d"; cp "$t/files.sha256" "$t/hosts.edn" "$t/ancestors.txt" "$d/"
  (cd "$here" && arbace_tool -p "$here/test" -m gate.plan plan "$r" "$d" "$store" full) > "$d.out" 2>&1; }
col() { awk -F'\t' -v n="$2" -v c="$3" '$1 == n {print $c}' "$1/plan.tsv"; }
# passes (at other keys: their peaks count, whatever the key): low 3.5 and 4 GiB (UNDER twice), once 5 GiB once, bare 3 GiB, high 1 GiB
p() { local d=$store/$1 k; mkdir -p "$d"; k=$(printf '%s%s' "$1" "$2" | sha256sum | cut -c1-64)
  printf '{:step "%s" :key "%s" :commit "c" :worktree "t" :date "2026-11-1%sT00:00:00Z" :secs 10 :maxrss-kib %s :tree-peak-kib %s}\n' "$1" "$k" "$2" "$3" "$4" > "$d/$k.edn"; }
p low 1 3670016 0; p low 2 1000 4194304; p once 1 5242880 0; p bare 1 3145728 3145728; p high 1 1048576 0
plan "$t/a" || no "the plan: $(cat "$t/a.out")"
[ "$(col "$t/a" low 9)" = 4.5 ] && [ "$(col "$t/a" low 18)" = 2 ] && ok "low: declared 2, peaks 3.5 and 4: admitted at 4.5 (1.1 x 4 up to half a GiB), DECLARED 2" || no "low: $(col "$t/a" low 9) $(col "$t/a" low 18)"
[ "$(col "$t/a" high 9)" = 8 ] && ok "high: declared 8, peak 1: admitted at its declaration" || no "high: $(col "$t/a" high 9)"
[ "$(col "$t/a" once 9)" = 5.5 ] && ok "once: a 5 GiB pass admits it at 5.5" || no "once: $(col "$t/a" once 9)"
[ "$(col "$t/a" bare 9)" = 3.5 ] && [ "$(col "$t/a" bare 18)" = - ] && ok "bare: undeclared, peak 3: admitted at 3.5 from the record alone" || no "bare: $(col "$t/a" bare 9) $(col "$t/a" bare 18)"
[ "$(col "$t/a" disk 19)" = 3 ] && [ "$(col "$t/a" low 19)" = - ] && ok "DISK-GIB: the table's :disk-gib, - without" || no "disk: $(col "$t/a" disk 19)"
grep -q 'UNDER  low: declared 2 GiB, 2 passes above it .* declare :mem-gib 5' "$t/a.out" && ok "UNDER printed for low (two passes), the figure to declare 5" || no "UNDER: $(grep UNDER "$t/a.out")"
! grep -q 'UNDER  once' "$t/a.out" && ok "not UNDER for once (one pass)" || no "once printed UNDER"
k1=$(col "$t/a" disk 3); table ":disk-gib 9"; plan "$t/b"; k2=$(col "$t/b" disk 3)
[ -n "$k1" ] && [ "$k1" = "$k2" ] && ok ":disk-gib in no key" || no ":disk-gib moved the key: $k1 $k2"
[ "$(awk -F'\t' '{print $NF}' "$t/a/plan.tsv" | sort -u)" = bin/tool ] && ok "the command still the last column" || no "the last column: $(awk -F'\t' '{print $NF}' "$t/a/plan.tsv" | sort -u)"
exit $bad
