#!/bin/bash
# bin/java-check --judge's audited rule (GT7 rerun 8, review 070): bin/java-check --judge over small
# fabricated results files, the real ref, divergences and records, on stage 0 (ARBACE_HOST_EXE):
# nothing is run, seconds. The checkout's bin/ and test/java/ are copied into a scratch root first, so a
# case that records writes there. AUDITED_TEST_SRC names another checkout to test (to see a case fail
# before its fix). Usage: ARBACE_HOST_EXE=... test/java/audited-test.bash
set -u
src=${AUDITED_TEST_SRC:-$(cd "$(dirname "$0")/../.." && pwd)}
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
t=$(mktemp -d "${TMPDIR:-/root/arbace/.tmp/tmp}/audited-test.XXXXXX") || exit 2
trap 'rm -rf "$t"' EXIT
root=$t/root; mkdir -p "$root/test"
cp -r "$src/bin" "$root/bin"; cp -r "$src/test/java" "$root/test/java"
export JAVA_CHECK_WORK=$t/work
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }

# results DIR LEVEL ID RESULT [REASON] [audited]: a fabricated results file of one level
results() { local d=$t/$1; mkdir -p "$d"; printf '%s\t%s\t%s\t%s\n' "$3" "$2" "$4" "${5:-}" > "$d/results.tsv"; }
audited() { printf '%s\n' "$2" > "$t/$1/audited"; }
# judge ARGS...: bin/java-check --judge, its output in $t/out, its status in $st
judge() { "$root/bin/java-check" --judge "$@" > "$t/out" 2>&1; st=$?; }

TO="20 s, then 40 s"

# 1 (finding 1): the held ids are written to --held FILE, one a line
results i interp t03686 timeout "$TO"; audited i interp; results c compiled t03686 pass
rm -f "$t/held.txt"
judge --held "$t/held.txt" "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 0 ] && [ "$(cat "$t/held.txt" 2>/dev/null)" = t03686 ]; then ok "held ids written"; else no "held ids written (status $st, file: $(cat "$t/held.txt" 2>&1))"; fi

# 1b: nothing held: the file exists and is empty
results i interp t03686 pass; audited i interp
rm -f "$t/held.txt"
judge --held "$t/held.txt" "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 0 ] && [ -f "$t/held.txt" ] && [ ! -s "$t/held.txt" ]; then ok "empty held file"; else no "empty held file (status $st)"; fi

# 1c: unaudited, the same timeout is a regression whatever --held says, and nothing is held
results i interp t03686 timeout "$TO"; rm -f "$t/i/audited"
rm -f "$t/held.txt"
judge --held "$t/held.txt" "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 1 ] && [ ! -s "$t/held.txt" ]; then ok "unaudited timeout: regression, nothing held"; else no "unaudited timeout (status $st)"; fi

# 3 (finding 3): one of rerun 7's 13 is held only as the rule holds any test: audited, a timeout; a wrong
# stdout, or a timeout in an unaudited run, is a regression and a new levels disagreement
rm -f "$t/i/audited"; results c compiled t03235 pass
results i interp t03235 fail "differ: stdout"
judge "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 1 ] && grep -q 'regression: t03235' "$t/out" && grep -q 'levels: t03235' "$t/out"; then ok "unaudited wrong stdout of a former :unstable test: regression"; else no "unaudited wrong stdout of t03235 (status $st)"; fi
results i interp t03235 timeout "$TO"
judge "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 1 ] && grep -q 'regression: t03235' "$t/out"; then ok "unaudited timeout of a former :unstable test: regression"; else no "unaudited timeout of t03235 (status $st)"; fi
audited i interp
judge "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 0 ] && grep -q 'held (an :audited class' "$t/out"; then ok "audited timeout of it: held by the rule"; else no "audited timeout of t03235 (status $st)"; fi
results i interp t03235 fail "differ: stdout"
judge "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 1 ] && grep -q 'regression: t03235' "$t/out"; then ok "audited wrong stdout: regression (only timeouts are held)"; else no "audited wrong stdout of t03235 (status $st)"; fi

# 4 (finding 4): --record of an audited judgment is refused, nothing written; unaudited it records
results i interp t03686 timeout "$TO"; audited i interp; results c compiled t03686 pass
cp "$root/test/java/check-interp.edn" "$t/interp.before"; cp "$root/test/java/levels-known.edn" "$t/known.before"
judge --record "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st != 0 ] && grep -q -- '--record with --audited' "$t/out" && cmp -s "$t/interp.before" "$root/test/java/check-interp.edn" \
   && cmp -s "$t/known.before" "$root/test/java/levels-known.edn"; then ok "--record with an audited judgment refused, nothing written"
else no "--record with an audited judgment (status $st; files changed: $(cmp -s "$t/interp.before" "$root/test/java/check-interp.edn" || echo interp) $(cmp -s "$t/known.before" "$root/test/java/levels-known.edn" || echo known))"; fi
cp "$t/interp.before" "$root/test/java/check-interp.edn"; cp "$t/known.before" "$root/test/java/levels-known.edn"
results i interp t03686 pass; rm -f "$t/i/audited"
judge --record --partial "$t/i/results.tsv" "$t/c/results.tsv"
if [ $st = 0 ] && grep -q 'recorded' "$t/out"; then ok "--record unaudited still records"; else no "--record unaudited (status $st)"; fi

# 5 (finding 5): --save's marker (mark_audited, extracted from bin/java-check) needs GATE_AUDITED=1 and a
# tracer; one without the other is said and not marked
if sed -n '/^mark_audited() {/,/^}/p' "$root/bin/java-check" | grep -q mark_audited; then
  sed -n '/^mark_audited() {/,/^}/p' "$root/bin/java-check" > "$t/mark.bash"
else
  # before the function existed: the marker's lines inline, as --save had them
  { echo 'mark_audited() { local save=$1 levels=$2; rm -f "$save/audited"'
    echo "  if awk '\$1 == \"TracerPid:\" && \$2 != 0 { t = 1 } END { exit !t }' /proc/self/status; then printf '%s\\n' \"\$levels\" > \"\$save/audited\"; fi; }"; } > "$t/mark.bash"
fi
# mark GATEVAR TRACE: the marker after mark_audited under (strace -f | nothing) with GATE_AUDITED GATEVAR
mark() {
  local d=$t/mk; rm -rf "$d"; mkdir -p "$d"
  local pre=(); [ "$2" = trace ] && pre=(strace -f -qq -o "$t/mk.strace" -e trace=none)
  env -u GATE_AUDITED ${1:+GATE_AUDITED=$1} "${pre[@]}" bash -c '. "$1"; mark_audited "$2" interp' mark "$t/mark.bash" "$d" 2> "$t/mark.err"
  if [ -f "$d/audited" ]; then echo "marked"; else echo "unmarked"; fi
}
r=$(mark 1 trace);   if [ "$r" = marked ]; then ok "GATE_AUDITED=1 under a tracer: marked"; else no "GATE_AUDITED=1 traced: $r"; fi
r=$(mark 1 plain);   if [ "$r" = unmarked ] && grep -q 'GATE_AUDITED=1 but no tracer' "$t/mark.err"; then ok "GATE_AUDITED=1 and no tracer: not marked, said"; else no "GATE_AUDITED=1 untraced: $r $(cat "$t/mark.err")"; fi
r=$(mark "" trace);  if [ "$r" = unmarked ] && grep -q 'a tracer is attached' "$t/mark.err"; then ok "a tracer without GATE_AUDITED: not marked, said"; else no "traced without GATE_AUDITED: $r $(cat "$t/mark.err")"; fi
r=$(mark "" plain);  if [ "$r" = unmarked ] && [ ! -s "$t/mark.err" ]; then ok "neither: not marked, silent"; else no "neither: $r $(cat "$t/mark.err")"; fi

echo "audited-test: $((n - bad)) of $n passed"
[ $bad = 0 ]
