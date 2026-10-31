# Review 007's finding 6: a run was judged by its status and stdout alone, though the samples claim stderr (uncaught.cpp's
# terminate message). A lowered run whose stderr differs from the original's fails now (run stderr-differs).
. "$(dirname "$0")/lib.bash"
w=$t/w; d=$w/files/x/a.cpp; mkdir -p "$d"
printf 'x/a.cpp\tc++23\t\tA\n' > "$w/list.tsv"
for f in ir.status orig-link.status gcc.status; do echo 0 > "$d/$f"; done
echo $'ok\t-' > "$d/rt.status"; echo same > "$d/object"
echo 134 > "$d/orig.status"; echo 134 > "$d/low.status"; echo before > "$d/orig.out"; echo before > "$d/low.out"
printf "terminate called after throwing an instance of 'std::runtime_error'\n  what():  boom\n" > "$d/orig.err"
printf "terminate called without an active exception\n" > "$d/low.err"
arbace_tool -m arbace.cpp.check judge "$w" "$root/test/cpp/lower-known.edn" "$root/test/cpp/lower-results.edn" > "$t/j.log" 2>&1; st=$?
[ $st = 1 ] && grep -q $'^x/a.cpp\tfailing\trun\t' "$w/results.tsv" && ok "a stderr that differs fails the run" || bad "exit $st: $(sed -n 2p "$w/results.tsv")"
cp "$d/orig.err" "$d/low.err"
arbace_tool -m arbace.cpp.check judge "$w" "$root/test/cpp/lower-known.edn" "$root/test/cpp/lower-results.edn" > "$t/j2.log" 2>&1; st=$?
[ $st = 0 ] && grep -q $'^x/a.cpp\tpass\t' "$w/results.tsv" && ok "the same stderr passes" || bad "exit $st: $(sed -n 2p "$w/results.tsv")"
finish
