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

echo "audited-test: $((n - bad)) of $n passed"
[ $bad = 0 ]
