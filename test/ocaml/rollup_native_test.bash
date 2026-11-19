#!/bin/bash
# The rollup reads the records the gate judges (n-oc-rollup, 2026-11-19): bin/oi suite rollup's test/ocaml/testsuite_check.ae
# takes each level's native record (BASE-native.edn) over the oracle path's (BASE.edn), the native row winning. Synthetic records,
# no run: a program recorded only natively must be classed (queue 355's link-test/test was "unclassed" so), a program in both
# takes the native row; the negative control, the oracle path alone (OI_CHECK_DEFAULT=ocdump), must leave the native-only
# program without a result and fail.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
. "$root/bin/lib/host.bash"
exe=$(arbace_host_exe) || { echo "FAIL rollup_native: no tools' host"; exit 1; }
d=$root/.tmp/w/rollup-native; rm -rf "$d"; mkdir -p "$d"
for f in roundtrip-known ref-expect divergences divergences-suite divergences-aot divergences-suite-aot divergences-testsuite \
         divergences-expect divergences-native check-oi check-aot; do echo '[]' > "$d/$f.edn"; done
printf '[\n ["t/both" "fail" "unclassed reason"]\n]\n' > "$d/check-suite-oi.edn"
printf '[\n ["t/both" "fail" "unclassed reason" "same"]\n]\n' > "$d/check-suite-aot.edn"
printf '[\n ["t/both" "pass" ""]\n ["t/native_only" "pass" ""]\n]\n' > "$d/check-suite-oi-native.edn"
printf '[\n ["t/both" "pass" "" "same"]\n ["t/native_only" "pass" "" "same"]\n]\n' > "$d/check-suite-aot-native.edn"
printf 'tests/t/both.ml\ttest\trun\t\t\t\t\t\ntests/t/native_only.ml\ttest\trun\t\t\t\t\t\n' > "$d/roles.tsv"
bad=0
OI_CHECK_DEFAULT=native "$exe" test/ocaml/testsuite_check.ae record "$d/roles.tsv" "$d/results.edn" > "$d/native.log" 2>&1; st=$?
want='["tests/t/both.ml" "test" "run" "pass" "-" "pass" "pass"]
["tests/t/native_only.ml" "test" "run" "pass" "-" "pass" "pass"]'
got=$(sed -n 's/^ \(\[.*\]\)$/\1/p' "$d/results.edn" 2>&1)
if [ $st = 0 ] && [ "$got" = "$want" ]; then echo "ok   native: the native-only program classed, the native row over the oracle's"
else echo "FAIL native: exit $st"; echo "$got"; tail -3 "$d/native.log"; bad=1; fi
OI_CHECK_DEFAULT=ocdump "$exe" test/ocaml/testsuite_check.ae record "$d/roles.tsv" "$d/results-ocdump.edn" > "$d/ocdump.log" 2>&1; st=$?
if [ $st != 0 ] && grep -q '^FAILING tests/t/native_only.ml' "$d/ocdump.log"; then echo "ok   control: the oracle path alone leaves it without a result"
else echo "FAIL control: exit $st (the oracle path alone must fail on tests/t/native_only.ml)"; tail -3 "$d/ocdump.log"; bad=1; fi
exit $bad
