# needs-host
# CP127 (b): the round trip's known file keyed bare names across its corpora, so a list naming the sample samples/coroutine.cpp
# (as lower-check's lists do) failed it, and two corpora could share a name. Now the known file's names (:files, :orig-fails)
# are qualified by the corpus: the list's key without its sampling (`tc-every-10` -> `tc/NAME`), a name already under its corpus
# kept, a list without a key named as written; results.tsv names the files so.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
v() { awk -F'\t' -v n="$2" '$1==n {print $2}' "$1/results.tsv"; }
judge() { arbace_tool -m arbace.repr.cpp.rtcheck judge "$1" "$2" no-objects > "$1.log" 2>&1; }
fd() { # fd WORK NAME [KEY]: NAME's forms differ (level 1), the list's key KEY
  rt_file "$1" "$2"; echo "(other forms)" > "$1/forms2/$2.ae"; [ -z "${3:-}" ] || echo "$3" > "$1/list.key"
}
echo '{:files {"tc/x.C" :forms-differ "samples/coroutine.cpp" :forms-differ}}' > "$t/q.edn"
echo '{:files {"x.C" :forms-differ}}' > "$t/bare.edn"
echo '{:files {"other/x.C" :forms-differ}}' > "$t/other.edn"
fd "$t/a" x.C tc-every-10; judge "$t/a" "$t/q.edn"; st=$?
[ $st = 0 ] && [ "$(v "$t/a" tc/x.C)" = known ] && ok "a keyed run's x.C is tc/x.C, known" || bad "tc/x.C: exit $st, $(cut -f1,2 "$t/a/results.tsv")"
fd "$t/b" x.C tc-every-10; judge "$t/b" "$t/bare.edn"; st=$?
[ $st = 1 ] && [ "$(v "$t/b" tc/x.C)" = fail ] && ok "a bare name in the known file does not cover tc's x.C" || bad "bare: exit $st, $(cut -f1,2 "$t/b/results.tsv")"
fd "$t/c" x.C tc-all; judge "$t/c" "$t/other.edn"; st=$?
[ $st = 1 ] && [ "$(v "$t/c" tc/x.C)" = fail ] && ok "another corpus' x.C does not cover tc's" || bad "other: exit $st, $(cut -f1,2 "$t/c/results.tsv")"
fd "$t/d" samples/coroutine.cpp; judge "$t/d" "$t/q.edn"; st=$?
[ $st = 0 ] && [ "$(v "$t/d" samples/coroutine.cpp)" = known ] && ok "a list naming samples/coroutine.cpp (no key) matches it" || bad "samples: exit $st, $(cut -f1,2 "$t/d/results.tsv")"
fd "$t/e" samples/coroutine.cpp samples; judge "$t/e" "$t/q.edn"; st=$?
[ $st = 0 ] && [ "$(v "$t/e" samples/coroutine.cpp)" = known ] && ok "under the key samples, samples/coroutine.cpp is not requalified" || bad "samples key: exit $st, $(cut -f1,2 "$t/e/results.tsv")"
# :orig-fails qualified too: clang fails on tc's y.C's original, recorded as tc/y.C
echo '{:files {} :orig-fails {"tc/y.C" #{:clang}}}' > "$t/of.edn"
w=$t/f; rt_file "$w" y.C; echo tc-every-10 > "$w/list.key"; rt_obj "$w" y.C gxx 0 0; rt_obj "$w" y.C clang 1 0
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/of.edn" objects > "$w.log" 2>&1; st=$?
[ $st = 0 ] && [ "$(v "$w" tc/y.C)" = pass ] && ok "a recorded tc/y.C original failure is allowed" || bad "orig-fails: exit $st, $(cut -f1,2,8 "$w/results.tsv")"
# the committed known file: every name is under one of the round trip's corpora
k=$root/test/repr/cpp/roundtrip-known.edn
n=$(grep -c '^ *"[^"]*" \(:\|#{\)' "$k"); q=$(grep -c '^ *"\(samples\|gxx-dg\|gxx-old-deja\|c-c++-common\|libstdcxx-testsuite\|hotspot-x86-linux\)/[^"]*" \(:\|#{\)' "$k")
[ "$n" -gt 0 ] && [ "$n" = "$q" ] && ok "the known file's $n names are qualified" || bad "the known file: $q of $n names qualified"
grep -q '"samples/coroutine.cpp" :gxx-objects-location' "$k" && ok "samples/coroutine.cpp is in it" || bad "no samples/coroutine.cpp"
finish
