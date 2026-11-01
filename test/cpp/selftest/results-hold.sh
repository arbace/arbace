# needs-host
# Report 043's F6: the results record could be lost without a failure. (1) A list whose key has an oracle record but no
# RESULTS/KEY.edn fails, unless RESULTS/pending.edn lists the key; (2) --record-results from a run whose names are not the existing
# record's is refused unless --replace-results ("record-replace" for the judge). Report 052's N2: a pending.edn key whose KEY.edn
# exists, or that the oracle has no record of, fails; N3: a key the oracle record does not have fails.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
res=$t/res; mkdir -p "$res"
echo '{:files {}}' > "$t/known.edn"
echo '{:corpora {"tc-every-10" {:fails {} :rejected []}}}' > "$t/oracle.edn"
mk() { local w=$1 x; shift; mkdir -p "$w"; echo tc-every-10 > "$w/list.key"; for x; do rt_file "$w" "$x"; done; return 0; }
judge() { local w=$1; shift; arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/known.edn" no-objects "$t/oracle.edn" "$res" "$@" > "$w.log" 2>&1; }
mk "$t/w1" a.C b.C; judge "$t/w1" check; st=$?
[ $st = 1 ] && grep -q 'has an oracle record but no recorded results' "$t/w1.log" && ok "an oracle-recorded corpus with no results file fails" || bad "no results: exit $st, $(tail -2 "$t/w1.log")"
echo '#{"tc-every-10"}' > "$res/pending.edn"
mk "$t/w2" a.C b.C; judge "$t/w2" check; st=$?
[ $st = 0 ] && ok "listed in pending.edn: not held, no failure" || bad "pending: exit $st, $(tail -2 "$t/w2.log")"
mk "$t/w3" a.C b.C; judge "$t/w3" record; st=$?
[ $st = 0 ] && grep -q '"tc/b.C" :pass' "$res/tc-every-10.edn" && ok "recorded" || bad "record: exit $st, $(tail -2 "$t/w3.log")"
mk "$t/w6" a.C b.C; judge "$t/w6" check; st=$?
[ $st = 1 ] && grep -q 'stale in .*pending.edn: tc-every-10' "$t/w6.log" && ok "a pending key with a results file fails" || bad "stale pending: exit $st, $(tail -2 "$t/w6.log")"
echo '#{"ghost-every-10"}' > "$res/pending.edn"
mk "$t/w7" a.C b.C; judge "$t/w7" check; st=$?
[ $st = 1 ] && grep -q 'stale in .*pending.edn: ghost-every-10' "$t/w7.log" && ok "a pending key naming no list fails" || bad "ghost pending: exit $st, $(tail -2 "$t/w7.log")"
echo '#{}' > "$res/pending.edn"
mk "$t/w8" a.C b.C; echo tc-mistyped > "$t/w8/list.key"; judge "$t/w8" check; st=$?
[ $st = 1 ] && grep -q 'the corpus tc-mistyped has no record' "$t/w8.log" && ok "a key with no oracle record fails" || bad "unknown key: exit $st, $(tail -2 "$t/w8.log")"
cp "$res/tc-every-10.edn" "$t/before.edn"
mk "$t/w4" a.C; judge "$t/w4" record; st=$?
[ $st = 1 ] && cmp -s "$res/tc-every-10.edn" "$t/before.edn" && grep -q 'not recorded: the run.s 1 names are not the existing record.s 2' "$t/w4.log" && ok "recording a trimmed run is refused, the record kept" || bad "trimmed record: exit $st, $(tail -2 "$t/w4.log")"
mk "$t/w5" a.C; judge "$t/w5" record-replace; st=$?
[ $st = 0 ] && ! grep -q 'tc/b.C' "$res/tc-every-10.edn" && ok "--replace-results rewrites it" || bad "replace: exit $st, $(tail -2 "$t/w5.log")"
finish
