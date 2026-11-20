#!/bin/bash
# The unit keys' measurement (bin/gate-unit-keys measure, test/gate/unitkeys.ae; doc/GATE-DIET-SURVEY.md, "Unit keys: the
# measurement"): on a made-up log, store, builds and loaded lists, each check's verdict under unit keys:
#  1. only the gate's identity moved, the units it loads the same: spared (host-only)
#  2. the gate's identity moved and a unit it loads moved: rerun (host-units); one it never loaded moved: spared
#  3. a file moved: rerun (file); a file of a namespace it never loaded: spared (unloaded-files)
#  4. a build whose units are not kept: rerun (host-unknown); only a build script moved: rerun, counted apart (build-scripts)
#  5. a check that never passed before, a FAILED one: rerun; the totals and the family table
# Seconds, no build, no gate run. Usage: test/gate/unitkeys-test.bash
# needs: exe (the measurement on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/unitkeys-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
bad=0 n=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"

r=$t/root; mkdir -p "$r/test/gate"
printf '{:groups {:build ["bin/arbace-go" "bin/lib/build-go/"]} :steps []}\n' > "$r/test/gate/steps.edn"
G1=aaaaaaaaaaaaaaaaaaaa1111 G2=bbbbbbbbbbbbbbbbbbbb2222 G3=cccccccccccccccccccc3333
exes=$t/exes; mkdir -p "$exes/${G1:0:20}/amd64" "$exes/${G2:0:20}/amd64"
printf -- '-core\tc1\narbace.a\ta1\narbace.b\tb1\n' > "$exes/${G1:0:20}/amd64/units.tsv"
printf -- '-core\tc1\narbace.a\ta1\narbace.b\tb2\n' > "$exes/${G2:0:20}/amd64/units.tsv"
loaded=$t/loaded; mkdir -p "$loaded"
for s in s1 s2 s3 s4 s5 s6; do printf 'arbace/a\n' > "$loaded/$s.loaded"; done
printf 'arbace/a\narbace/b\n' > "$loaded/s2.loaded"
store=$t/store
# pass STEP KEY COMMIT DATE LINES...: a pass and its material
pass() { local s=$1 k=$2 c=$3 d=$4; shift 4; mkdir -p "$store/$s"; printf '%s\n' "step $s" "$@" > "$store/$s/$k.inputs"
  printf '{:step "%s" :key "%s" :commit "%s" :worktree "t" :date "%s" :secs 60}\n' "$s" "$k" "$c" "$d" > "$store/$s/$k.edn"; }
old=(2026-11-16T00:00:00Z) new=(2026-11-17T00:00:00Z)
base=("decl {:name x}" "logic e1 test/gate/epoch")
# s1: only the gate moved, its units (arbace.a, -core) the same
pass s1 k1a 0ld00000 "$old" "${base[@]}" "host gate $G1" "file f1 x/y"
pass s1 k1b 0e700000 "$new" "${base[@]}" "host gate $G2" "file f1 x/y"
# s2: the gate moved and arbace.b, which it loads, moved
pass s2 k2a 0ld00000 "$old" "${base[@]}" "host gate $G1"
pass s2 k2b 0e700000 "$new" "${base[@]}" "host gate $G2"
# s3: a file moved
pass s3 k3a 0ld00000 "$old" "${base[@]}" "file f1 x/y"
pass s3 k3b 0e700000 "$new" "${base[@]}" "file f2 x/y"
# s4: a file of a namespace it never loaded (arbace.b)
pass s4 k4a 0ld00000 "$old" "${base[@]}" "file f1 arbace/b.ae"
pass s4 k4b 0e700000 "$new" "${base[@]}" "file f2 arbace/b.ae"
# s5: a build whose units are not kept
pass s5 k5a 0ld00000 "$old" "${base[@]}" "host gate $G1"
pass s5 k5b 0e700000 "$new" "${base[@]}" "host gate $G3"
# s6: only a build script moved, the gate's units the same
pass s6 k6a 0ld00000 "$old" "${base[@]}" "host gate $G1" "file f1 bin/arbace-go"
pass s6 k6b 0e700000 "$new" "${base[@]}" "host gate $G2" "file f2 bin/arbace-go"
# s7: no earlier pass; s8: failed
pass s7 k7b 0e700000 "$new" "${base[@]}"
cat > "$t/log" <<EOF
== gate --full: 0e70000 a test
   ok  makefile (0m00s, cpu 0m00s)
   ok  s1 (1m00s, cpu 0m00s)
   ok  s2 (1m00s, cpu 0m00s)
   ok  s3 (1m00s, cpu 0m00s)
   ok  s4 (1m00s, cpu 0m00s)
   ok  s5 (1m00s, cpu 0m00s)
   ok  s6 (1m00s, cpu 0m00s)
   ok  s7 (1m00s, cpu 0m00s)
   FAILED  s8 (2m00s, cpu 0m00s)
   skipped  s9 (passed at ...)
EOF
(cd "$here" && arbace_tool -p "$here/test" -m gate.unitkeys measure "$r" "$t/log" "$store" "$exes" --loaded "$loaded" --tsv "$t/out.tsv") > "$t/out" 2>&1 \
  || no "the measurement failed: $(tail -5 "$t/out")"
v() { awk -F'\t' -v s="$1" '$1 == s {print $2 "/" $4}' "$t/out.tsv"; }
[ "$(v s1)" = spared/host-only ] && ok "only the gate moved, its units the same: spared" || no "s1: $(v s1)"
[ "$(v s2)" = rerun/host-units ] && grep -q '^s2 .*1 of its 3 units moved: arbace.b' "$t/out" && ok "a unit it loads moved: rerun, named" || no "s2: $(v s2) $(grep '^s2 ' "$t/out")"
[ "$(v s1)" = spared/host-only ] && [ "$(v s3)" = rerun/file ] && ok "a file moved: rerun" || no "s3: $(v s3)"
[ "$(v s4)" = spared/unloaded-files ] && ok "a file of a namespace it never loaded: spared" || no "s4: $(v s4)"
[ "$(v s5)" = rerun/host-unknown ] && ok "a build whose units are not kept: rerun (unknown)" || no "s5: $(v s5)"
[ "$(v s6)" = rerun/build-scripts ] && ok "only a build script moved: rerun, counted apart" || no "s6: $(v s6)"
[ "$(v s7)" = rerun/never-passed ] && [ "$(v s8)" = rerun/failed ] && [ -z "$(v s9)" ] && [ -z "$(v makefile)" ] \
  && ok "never passed, failed: rerun; a skipped step and the pre steps are not counted" || no "s7 s8 s9 makefile: $(v s7) $(v s8) [$(v s9)] [$(v makefile)]"
grep -q '^unitkeys: 0e70000: 8 checks ran, 6 would rerun under unit keys, 2 spared; serial 0:09, spared 0:02 (and 1 more, 0:01, if the build' "$t/out" \
  && ok "the totals" || no "the totals: $(tail -1 "$t/out")"
grep -Eq '^s1 +1 +0 ' "$t/out" && ok "the family table" || no "the family table: $(grep -E '^(family|s1) ' "$t/out")"

echo "unitkeys-test: $((n - bad)) of $n ok"
exit $((bad > 0))
