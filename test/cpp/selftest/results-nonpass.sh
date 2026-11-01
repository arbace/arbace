# needs-host
# Report 043's F5: the results record held only a recorded :pass; a recorded :known (or :oracle-rejects, :oracle-fails) that became
# another non-pass verdict went unseen. Now it fails (12th column changed-from-VERDICT); a move to :pass is still an improvement.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
res=$t/res; mkdir -p "$res"
cat > "$res/tc-every-10.edn" <<'EOR'
{:key "tc-every-10"
 :files {
  "tc/a.C" :pass
  "tc/b.C" :known
  "tc/c.C" :oracle-rejects
  "tc/d.C" :known
 }}
EOR
echo '{:files {"tc/b.C" :forms-differ "tc/c.C" :forms-differ "tc/d.C" :forms-differ}}' > "$t/known.edn"
mk() { local w=$1 x n; shift; mkdir -p "$w"; echo tc-every-10 > "$w/list.key"
  for x; do n=${x%%:*}; rt_file "$w" "$n"; [ "${x##*:}" = d ] && echo "(other forms)" > "$w/forms2/$n.ae"; done; return 0; }
judge() { local w=$1; arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/known.edn" no-objects - "$res" check > "$w.log" 2>&1; }
# the record: b known, c oracle-rejects (the oracle file is "-" here, so the run's verdict for c is known or pass), d known
mk "$t/w1" a.C:p b.C:d c.C:d d.C:d; judge "$t/w1"; st=$?
[ $st = 1 ] && grep -q 'tc/c.C (recorded :oracle-rejects, now known:forms-differ)' "$t/w1.log" && ok "a recorded :oracle-rejects that is now known fails" || bad "oracle-rejects to known: exit $st, $(tail -2 "$t/w1.log")"
[ "$(awk -F'\t' '$1=="tc/b.C"{print $2}' "$t/w1/results.tsv")" = known ] && ok "a recorded :known that is known stays known" || bad "b: $(cut -f1,2,12 "$t/w1/results.tsv")"
mk "$t/w2" a.C:p b.C:p c.C:p d.C:p; judge "$t/w2"; st=$?
[ $st = 0 ] && grep -q ':improvements 3' "$t/w2/summary.edn" && ok "recorded non-passes that pass: improvements, no failure" || bad "improvements: exit $st, $(cat "$t/w2/summary.edn")"
# CP165 (b), CP172 (b): a recorded known carries its class, [:known CLASS]; a run known in another class fails, a bare :known still holds the verdict
cat > "$res/tc-every-10.edn" <<'EOR'
{:key "tc-every-10"
 :files {
  "tc/a.C" :pass
  "tc/b.C" [:known :objects-differ]
  "tc/c.C" :known
  "tc/d.C" [:known :forms-differ]
 }}
EOR
mk "$t/w3" a.C:p b.C:d c.C:d d.C:d; judge "$t/w3"; st=$?
[ $st = 1 ] && grep -q 'tc/b.C (recorded :known:objects-differ, now known:forms-differ)' "$t/w3.log" && ok "a recorded known of another class fails" || bad "class change: exit $st, $(tail -2 "$t/w3.log")"
[ "$(awk -F'\t' '$1=="tc/c.C"{print $12}' "$t/w3/results.tsv")" = - ] && [ "$(awk -F'\t' '$1=="tc/d.C"{print $12}' "$t/w3/results.tsv")" = - ] && ok "a bare :known and a known of the same class hold" || bad "c, d: $(cut -f1,2,12 "$t/w3/results.tsv")"
mk "$t/w4" a.C:p b.C:d c.C:d d.C:d
rm "$res/tc-every-10.edn"; echo '{:files {"tc/b.C" :forms-differ "tc/c.C" :forms-differ "tc/d.C" :forms-differ}}' > "$t/known.edn"
arbace_tool -m arbace.repr.cpp.rtcheck judge "$t/w4" "$t/known.edn" no-objects - "$res" record > "$t/w4.log" 2>&1; st=$?
[ $st = 0 ] && grep -q '"tc/b.C" \[:known :forms-differ\]' "$res/tc-every-10.edn" && grep -q '"tc/a.C" :pass' "$res/tc-every-10.edn" && ok "the record writes [:known CLASS]" || bad "record: exit $st, $(cat "$res/tc-every-10.edn" 2>&1 | tail -5)"
# CP164 (c): an improvement fails in a whole-corpus run (the key ends in -all), and is a count in a sample run (w2 above)
cat > "$res/tc-all.edn" <<'EOR'
{:key "tc-all"
 :files {
  "tc/a.C" :pass
  "tc/b.C" [:known :forms-differ]
 }}
EOR
mk "$t/w5" a.C:p b.C:p; echo tc-all > "$t/w5/list.key"; judge "$t/w5"; st=$?
[ $st = 1 ] && grep -q 'now pass: write the record again' "$t/w5.log" && ok "an improvement fails in a whole-corpus run" || bad "whole-corpus improvement: exit $st, $(tail -2 "$t/w5.log")"
finish
