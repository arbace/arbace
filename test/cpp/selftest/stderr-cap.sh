# needs-host
# Review 018's R7: a run's stderr was written without bound and cut to 1 MiB afterwards (a runaway program could fill the disk for
# its whole timeout), and two streams that agreed on their first MiB compared equal. Now the run is limited to a 1 MiB file
# (SIGXFSZ past it), and a stderr at the cap is not comparable.
. "$(dirname "$0")/lib.bash"
fake_runtime "$t/rt" || die "fake runtime"
src=$root/test/cpp/selftest/stderr-cap.cpp
printf 'stderr-cap.cpp\tc++23\t\t%s\n' "$src" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" --runtime "$t/rt" --recorded none > "$t/w.log" 2>&1; st=$?
d=$t/w/files/stderr-cap.cpp
sz=0; [ -f "$d/orig.err" ] && sz=$(stat -c %s "$d/orig.err")
[ "$sz" -le 1048576 ] && ok "the original's stderr stays within 1 MiB ($sz bytes)" || bad "orig.err is $sz bytes"
[ "$(cat "$d/orig.status" 2>&1)" != 0 ] && ok "the writer is stopped at the cap (status $(cat "$d/orig.status"))" || bad "the original ran to the end (status 0)"
[ $st = 1 ] && grep -q $'^stderr-cap.cpp\tfailing\trun\t' "$t/w/results.tsv" && ok "two cut stderr streams are not the same run" || bad "lower-check exit $st: $(sed -n 2p "$t/w/results.tsv" 2>&1)"
# the judge: stderr at the cap is not comparable even with a status of 0
w=$t/j; d=$w/files/x/a.cpp; mkdir -p "$d"; printf 'x/a.cpp\tc++23\t\tA\n' > "$w/list.tsv"
for f in ir.status orig-link.status gcc.status; do echo 0 > "$d/$f"; done
echo $'ok\t-' > "$d/rt.status"; echo same > "$d/object"; echo 0 > "$d/orig.status"; echo 0 > "$d/low.status"; : > "$d/orig.out"; : > "$d/low.out"
yes xxxxxxxxxxxxxxx | head -c 1048576 > "$d/orig.err"; cp "$d/orig.err" "$d/low.err"
arbace_tool -m arbace.cpp.check judge "$w" "$root/test/cpp/lower-known.edn" "$root/test/cpp/lower-results.edn" > "$t/j.log" 2>&1; st=$?
[ $st = 1 ] && grep -q $'^x/a.cpp\tfailing\trun\t' "$w/results.tsv" && ok "a stderr at the cap fails the judge" || bad "judge: exit $st, $(sed -n 2p "$w/results.tsv")"
finish
