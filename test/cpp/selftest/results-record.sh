# needs-host
# CP124 (b): the round trip's results were held only to the known file, so a file that moved from pass to known (or was dropped
# from a list) went unnoticed. Now each corpus list's results are recorded (test/repr/cpp/roundtrip-results/KEY.edn, KEY the list's
# key; REPR_CPP_ROUNDTRIP_RESULTS names another directory): a recorded :pass that does not pass fails, and a run of the list is held
# to the record's names; --record-results writes the record from a run with no failure.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
v() { awk -F'\t' -v n="$2" '$1==n {print $2}' "$1/results.tsv"; }
res=$t/res; mkdir -p "$res"
echo '{:files {"tc/a.C" :forms-differ "tc/b.C" :forms-differ}}' > "$t/known.edn"
mk() { # mk WORK NAME:STATE...: a run of the list tc-every-10, each file passing (p) or with forms that differ (d)
  local w=$1 x n; shift; mkdir -p "$w"; echo tc-every-10 > "$w/list.key"
  for x; do n=${x%%:*}; rt_file "$w" "$n"; [ "${x##*:}" = d ] && echo "(other forms)" > "$w/forms2/$n.ae"; done; return 0
}
judge() { local w=$1; shift; arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/known.edn" no-objects - "$res" "$@" > "$w.log" 2>&1; }
mk "$t/w0" a.C:p b.C:d; judge "$t/w0" check; st=$?
[ $st = 0 ] && grep -q 'no recorded results for tc-every-10' "$t/w0.log" && ok "no record: not held, said" || bad "no record: exit $st, $(head -3 "$t/w0.log")"
mk "$t/w1" a.C:p b.C:d; judge "$t/w1" record; st=$?
f=$res/tc-every-10.edn
[ $st = 0 ] && grep -q '"tc/a.C" :pass' "$f" && grep -q '"tc/b.C" :known' "$f" && ok "--record-results writes tc/a.C :pass, tc/b.C :known" || bad "record: exit $st, $(cat "$f" 2>&1 | tail -4)"
mk "$t/w2" a.C:p b.C:d; judge "$t/w2" check; st=$?
[ $st = 0 ] && ok "the same run against the record passes" || bad "same: exit $st, $(tail -3 "$t/w2.log")"
mk "$t/w3" a.C:d b.C:d; judge "$t/w3" check; st=$?
[ $st = 1 ] && [ "$(v "$t/w3" tc/a.C)" = fail ] && grep -q $'\tregression$' "$t/w3/results.tsv" && grep -q 'tc/a.C (recorded :pass)' "$t/w3.log" \
  && ok "a recorded pass that is now known fails (a regression)" || bad "pass to known: exit $st, $(cut -f1,2,12 "$t/w3/results.tsv")"
mk "$t/w4" a.C:p; judge "$t/w4" check; st=$?
[ $st = 1 ] && grep -q '1 recorded names missing: tc/b.C' "$t/w4.log" && ok "a recorded name missing from the run fails" || bad "missing: exit $st, $(tail -2 "$t/w4.log")"
mk "$t/w5" a.C:p b.C:d c.C:p; judge "$t/w5" check; st=$?
[ $st = 1 ] && grep -q '1 names not recorded: tc/c.C' "$t/w5.log" && ok "a name not recorded fails" || bad "new: exit $st, $(tail -2 "$t/w5.log")"
mk "$t/w6" a.C:p b.C:p; judge "$t/w6" check; st=$?
[ $st = 0 ] && grep -q ':improvements 1' "$t/w6/summary.edn" && ok "a recorded known that passes now: an improvement, a count" || bad "improvement: exit $st, $(cat "$t/w6/summary.edn")"
cp "$f" "$t/before.edn"
echo '{:files {}}' > "$t/known.edn"; mk "$t/w7" a.C:p b.C:d; judge "$t/w7" record; st=$?
[ $st = 1 ] && cmp -s "$f" "$t/before.edn" && grep -q 'not recorded: 1 failing' "$t/w7.log" && ok "a run with a failure is not recorded" || bad "record a failure: exit $st"
# through bin/repr-cpp roundtrip (the script passes the record): min.cpp under --corpus tr, recorded, then held
printf 'min.cpp\tc++23\t-std=c++23\t%s\t-std=c++23\n' "$root/test/cpp/selftest/min.cpp" > "$t/list.tsv"
echo '{:files {}}' > "$t/known.edn"
rtrip() { REPR_CPP_ROUNDTRIP_KNOWN=$t/known.edn REPR_CPP_ROUNDTRIP_ORACLE=$t/oracle.edn REPR_CPP_ROUNDTRIP_RESULTS=$res \
  "$root/bin/repr-cpp" roundtrip "$t/list.tsv" "$t/$1" --no-objects --corpus tr "${@:2}" > "$t/$1.log" 2>&1; }
rtrip r1 --record-results; st=$?
[ $st = 0 ] && grep -q '"tr/min.cpp" :pass' "$res/tr.edn" && ok "bin/repr-cpp roundtrip --record-results writes tr.edn" || bad "script record: exit $st, $(tail -3 "$t/r1.log")"
sed -i 's|"tr/min.cpp" :pass|"tr/min.cpp" :pass\n  "tr/gone.cpp" :pass|' "$res/tr.edn"
rtrip r2; st=$?
[ $st = 1 ] && grep -q 'recorded names missing: tr/gone.cpp' "$t/r2.log" && ok "bin/repr-cpp roundtrip holds the run to the record" || bad "script check: exit $st, $(tail -3 "$t/r2.log")"
finish
