# needs-host
# Review 007's finding 7: level 4 of repr/cpp's round trip passed when a compiler failed on the original (any file) or when
# the object step did not run. Now a compiler may fail on the original only where the known file's :orig-fails records it,
# and missing objects fail unless the run had --no-objects.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
known=$t/known.edn
echo '{:files {} :orig-fails {"rec.C" #{:gxx}}}' > "$known"
w=$t/w; mkdir -p "$w"
for n in rec.C new.C none.C good.C; do rt_file "$w" "$n"; done
rt_obj "$w" rec.C gxx 1 0; rt_obj "$w" rec.C clang 0 0
rt_obj "$w" new.C gxx 1 0; rt_obj "$w" new.C clang 0 0
rt_obj "$w" good.C gxx 0 0; rt_obj "$w" good.C clang 0 0
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$known" > "$t/j.log" 2>&1; st=$?
v() { awk -F'\t' -v n="$1" '$1==n {print $2}' "$w/results.tsv"; }
[ $st = 1 ] && ok "the judge fails (exit 1)" || bad "judge: exit $st"
[ "$(v good.C)" = pass ] && ok "equal objects pass" || bad "good.C: $(v good.C)"
[ "$(v rec.C)" = pass ] && ok "a recorded original g++ rejects passes" || bad "rec.C: $(v rec.C)"
[ "$(v new.C)" = fail ] && ok "an unrecorded original g++ rejects fails" || bad "new.C: $(v new.C)"
[ "$(v none.C)" = fail ] && ok "no objects with objects asked for fails" || bad "none.C: $(v none.C)"
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$known" no-objects > "$t/j2.log" 2>&1
[ "$(v none.C)" = pass ] && ok "no objects under --no-objects passes" || bad "none.C under --no-objects: $(v none.C)"
finish
