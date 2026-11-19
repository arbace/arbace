#!/bin/bash
# Review 419 L3, DECISION 4 (a) (2026-11-19): programs-full's :parts (n-gate-mem: a part a sample, a hand list) equal the
# samples bin/arbace-go --build-program --check takes with no names: test/programs/'s directories and test/programs-fail/'s.
# A new sample not listed would go unchecked by --full and --weekly. The rule on steps.edn, then its negative controls: a
# made-up samples tree with one more sample (missing from :parts) and a table with one more part (no such sample) must fail.
# Seconds, no build, no gate run.
# needs: exe (the table's reader)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "programs-parts-test: no executable $exe" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/programs-parts-test.XXXXXX")
trap 'rm -rf "$w"' EXIT
fail=0
ok() { echo "ok   $1"; }
bad() { echo "FAIL $1"; fail=1; }
cat > "$w/parts.ae" <<'F'
(let [steps (:steps (read-string (slurp (first *command-line-args*))))
      s (first (filter #(= 'programs-full (:name %)) steps))]
  (doseq [p (sort (map #(str (:part %)) (:parts s)))] (println p)))
F
# check TABLE ROOT: the parts against ROOT's test/programs and test/programs-fail, one line a difference
check() {
  "$exe" "$w/parts.ae" "$1" > "$w/parts.txt" || { echo "programs-parts-test: $1 not read" >&2; return 2; }
  { find "$2/test/programs" "$2/test/programs-fail" -mindepth 1 -maxdepth 1 -type d -printf '%f\n'; } | LC_ALL=C sort > "$w/dirs.txt"
  LC_ALL=C sort -o "$w/parts.txt" "$w/parts.txt"
  [ -s "$w/parts.txt" ] || { echo "no programs-full parts"; return 1; }
  diff "$w/parts.txt" "$w/dirs.txt" > "$w/diff.txt" && return 0
  sed -n 's/^< /in :parts, no sample: /p; s/^> /a sample, not in :parts: /p' "$w/diff.txt"; return 1
}
out=$(check "$src/test/gate/steps.edn" "$src"); st=$?
[ $st = 0 ] && ok "programs-full's :parts are test/programs/ and test/programs-fail/ ($(wc -l < "$w/parts.txt") samples)" \
  || bad "programs-full's :parts differ from the samples: $out"
# control 1: one more sample in a copy of the samples' tree
mkdir -p "$w/tree/test"; cp -r "$src/test/programs" "$src/test/programs-fail" "$w/tree/test/"
mkdir -p "$w/tree/test/programs/zz-new-sample"
out=$(check "$src/test/gate/steps.edn" "$w/tree"); st=$?
[ $st = 1 ] && grep -q 'not in :parts: zz-new-sample' <<< "$out" && ok "control: a sample missing from :parts fails" \
  || bad "control: a sample missing from :parts was not flagged ($st: $out)"
rmdir "$w/tree/test/programs/zz-new-sample"
# control 2: one more part than samples
sed 's/{:part bad-go /{:part zz-no-sample} {:part bad-go /' "$src/test/gate/steps.edn" > "$w/steps.edn"
grep -q zz-no-sample "$w/steps.edn" || bad "control 2's table not made (bad-go's part not found)"
out=$(check "$w/steps.edn" "$w/tree"); st=$?
[ $st = 1 ] && grep -q 'no sample: zz-no-sample' <<< "$out" && ok "control: a part with no sample fails" \
  || bad "control: a part with no sample was not flagged ($st: $out)"
exit $fail
