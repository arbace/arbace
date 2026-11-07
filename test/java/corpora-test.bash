#!/bin/bash
# GT8 (doc/GATE-SURVEY.md, "As built: GT8 (the tools' lists)"): bin/java-check's runs by corpus and by list, judged
# over fabricated results files with the real ref, divergences and records, on stage 0 (ARBACE_HOST_EXE): nothing is
# run, seconds. A --save of whole corpora (--corpus alone, the gate's parts) writes DIR/corpus, and --judge judges
# those corpora whole (a test of theirs without a row is missing; the other corpora's tests are not), --record keeping
# the other corpora's lines; a --list run is partial (DIR/partial); java.check/plan --list selects by id. The
# checkout's bin/ and test/java/ are copied into a scratch root first. Usage: ARBACE_HOST_EXE=... test/java/corpora-test.bash
set -u
src=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$src/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$src/.tmp/tmp}/java-corpora-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
root=$t/root; mkdir -p "$root/test"
# what --judge reads (the gate-tests step declares them): the script and its libraries, java.check, the ref, the records,
# the classes and the controls
mkdir -p "$root/bin" "$root/test/java"; cp "$src/bin/java-check" "$root/bin/"; cp -r "$src/bin/lib" "$root/bin/lib"
for f in check.ae ref-jdk26.edn corpora.edn controls.edn verdicts.edn divergences.edn levels-known.edn check-interp.edn; do cp "$src/test/java/$f" "$root/test/java/"; done
export JAVA_CHECK_WORK=$t/work
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }

# the jls corpus's tests the JDK passes, and their recorded interp results (pass at the record: a whole jls run)
grep -oE '^ \{:id "t[0-9]+" :test "[^"]*" :corpus :jls :category :[a-z-]+ :jdk :pass ' "$root/test/java/ref-jdk26.edn" \
  | sed -E 's/^ \{:id "(t[0-9]+)".*/\1/' > "$t/jls.ids"
nj=$(wc -l < "$t/jls.ids")
mkdir -p "$t/jls"
while read -r id; do
  l=$(grep -F "[\"$id\" " "$root/test/java/check-interp.edn") || continue
  r=$(echo "$l" | sed -E 's/^ \["[^"]+" "[^"]*" :([a-z-]+) "(.*)"\]$/\1\t\2/; s/\\"/"/g')
  printf '%s\tinterp\t%s\n' "$id" "$r"
done < "$t/jls.ids" | LC_ALL=C sort > "$t/jls/results.tsv"
# the negative control c00001 (its subject t03208 a jls test): its row as a real run has it (judged against t03209's reference)
printf 'c00001\tinterp\tdiffer\tstdout\n' >> "$t/jls/results.tsv"; LC_ALL=C sort -o "$t/jls/results.tsv" "$t/jls/results.tsv"
echo interp > "$t/jls/levels"; echo jls > "$t/jls/corpus"
[ "$nj" -gt 5 ] && [ "$(wc -l < "$t/jls/results.tsv")" = "$((nj + 1))" ] || { echo "corpora-test: no jls tests ($nj, $(wc -l < "$t/jls/results.tsv") rows)"; exit 2; }

judge() { "$root/bin/java-check" --judge "$@" > "$t/out" 2>&1; st=$?; }

# 1. a whole run of one corpus (DIR/corpus): judged whole within it, the other corpora's tests not missing
judge "$t/jls/results.tsv"
if [ $st = 0 ] && ! grep -q 'missing' "$t/out" && grep -q "java-check interp: $nj tests" "$t/out"; then ok "a corpus's whole run: judged within its corpus ($nj tests), nothing missing"
else no "a corpus's whole run (status $st): $(grep -E 'missing|interp:' "$t/out" | head -3)"; fi

# 2. a test of that corpus without a row: missing, the run fails
lost=$(grep -v '^c' "$t/jls/results.tsv" | head -1 | cut -f1)
mkdir -p "$t/jls2"; cp "$t/jls/levels" "$t/jls/corpus" "$t/jls2/"; grep -v "^$lost	" "$t/jls/results.tsv" > "$t/jls2/results.tsv"
judge "$t/jls2/results.tsv"
if [ $st = 1 ] && grep -q "missing: $lost " "$t/out" && [ "$(grep -c 'missing: ' "$t/out")" = 1 ]; then ok "a corpus's lost row: missing (that test alone)"
else no "a corpus's lost row (status $st): $(grep -c 'missing: ' "$t/out") missing lines"; fi

# 3. the same file without the corpus marker and without partial: a whole run, every other test missing
rm "$t/jls2/corpus"
judge "$t/jls2/results.tsv"
if [ $st = 1 ] && [ "$(grep -c 'missing: ' "$t/out")" -gt 1000 ]; then ok "no marker: a whole run of every corpus (the others missing)"; else no "no marker (status $st)"; fi

# 4. two corpora's files, one marker each: the union judged whole
mkdir -p "$t/oth"; echo interp > "$t/oth/levels"; echo hotspot-runtime > "$t/oth/corpus"
grep -oE '^ \{:id "t[0-9]+" :test "[^"]*" :corpus :hotspot-runtime :category :[a-z-]+ :jdk :pass ' "$root/test/java/ref-jdk26.edn" \
  | sed -E 's/^ \{:id "(t[0-9]+)".*/\1/' > "$t/oth.ids"
while read -r id; do
  l=$(grep -F "[\"$id\" " "$root/test/java/check-interp.edn") || continue
  printf '%s\tinterp\t%s\n' "$id" "$(echo "$l" | sed -E 's/^ \["[^"]+" "[^"]*" :([a-z-]+) "(.*)"\]$/\1\t\2/; s/\\"/"/g')"
done < "$t/oth.ids" | LC_ALL=C sort > "$t/oth/results.tsv"
judge "$t/jls/results.tsv" "$t/oth/results.tsv"
if [ $st = 0 ] && ! grep -q 'missing' "$t/out"; then ok "two corpora's whole runs: judged within their union"; else no "two corpora (status $st): $(grep -E 'missing|regression' "$t/out" | head -3)"; fi

# 5. --record of a corpus's whole run keeps the other corpora's lines
before=$(grep -c '^ \["' "$root/test/java/check-interp.edn")
judge --record "$t/jls/results.tsv"
after=$(grep -c '^ \["' "$root/test/java/check-interp.edn")
if [ $st = 0 ] && [ "$before" = "$after" ] && cmp -s "$src/test/java/check-interp.edn" "$root/test/java/check-interp.edn"; then ok "--record of a corpus's run: the other corpora's lines kept ($after), the record unchanged"
else no "--record of a corpus's run (status $st): $before lines before, $after after"; fi

# 6. a --list run is partial (its DIR/partial): a slice's rows judged alone, nothing missing
mkdir -p "$t/sl"; echo interp > "$t/sl/levels"; : > "$t/sl/partial"; grep -v '^c' "$t/jls/results.tsv" | grep -v '^t03208	' | head -3 > "$t/sl/results.tsv"
judge "$t/sl/results.tsv"
if [ $st = 0 ] && ! grep -q 'missing' "$t/out" && grep -q 'java-check interp: 3 tests' "$t/out"; then ok "a list's run (partial): its 3 rows judged, nothing missing"; else no "a list's run (status $st)"; fi
# and a regression in it fails, named by the judge's line the gate's :judges reads
id1=$(head -1 "$t/sl/results.tsv" | cut -f1)
printf '%s\tinterp\tfail\tdiffer: stdout\n' "$id1" > "$t/sl/results.tsv"
judge "$t/sl/results.tsv"
if [ $st = 1 ] && [ "$(sed -nE 's/^   (regression|in no class|missing): (t[0-9]+) .*$/\2/p' "$t/out" | sort -u)" = "$id1" ]; then ok "a list's regression: failed, its id on the :judges line"
else no "a list's regression (status $st): $(grep -E 'regression|in no class' "$t/out" | head -2)"; fi

# 7. java.check/plan --list: the listed ids alone (blank and # lines left out); bin/java-check passes --list on
printf '# a slice\n%s\n\n%s\n' "$(sed -n 1p "$t/jls.ids")" "$(sed -n 2p "$t/jls.ids")" > "$t/list.txt"
mkdir -p "$t/pw"
(cd "$src" && root=$src && . "$src/bin/lib/tools.bash" && arbace_tool -p "$src/test" -m java.check plan "$root/test/java/ref-jdk26.edn" "$t/pw" --list "$t/list.txt") > "$t/plan.out" 2>&1
if [ "$(cat "$t/pw/run.list" 2>&1)" = "$(head -2 "$t/jls.ids")" ]; then ok "plan --list: the listed tests alone"; else no "plan --list: $(cat "$t/pw/run.list" "$t/plan.out" 2>&1 | head -3)"; fi
if grep -q -- '--list) list=$(realpath -m "$2")' "$root/bin/java-check" && grep -q 'pargs+=(--list "$list")' "$root/bin/java-check" \
   && grep -q 'if \[ -n "$only" \] || \[ -n "$list" \] || \[ -n "$essential" \]; then : > "$save/partial"' "$root/bin/java-check"; then ok "bin/java-check --list: passed to plan, its --save partial"
else no "bin/java-check --list's wiring"; fi
"$root/bin/java-check" --list "$t/list.txt" --essential > "$t/ess.out" 2>&1
if [ $? = 2 ] && grep -q 'with --essential' "$t/ess.out"; then ok "--list with --essential refused"; else no "--list with --essential: $(head -2 "$t/ess.out")"; fi

echo "$((n - bad)) of $n passed"
[ "$bad" = 0 ]
