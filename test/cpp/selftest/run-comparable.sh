# Review 007's finding 12: two runs both killed by our timeout (124), or both cut at the 1 MB cap and dead of SIGPIPE
# (141), were judged "same". Neither is comparable now (run not-comparable, failing); the program's own abort (134) is.
. "$(dirname "$0")/lib.bash"
w=$t/w
printf 'x/hang.cpp\tc++23\t\tA\nx/big.cpp\tc++23\t\tB\nx/abort.cpp\tc++23\t\tC\n' > "$w.list"
mkdir -p "$w"; cp "$w.list" "$w/list.tsv"
for n in hang big abort; do
  d=$w/files/x/$n.cpp; mkdir -p "$d"
  for f in ir.status orig-link.status gcc.status; do echo 0 > "$d/$f"; done
  echo $'ok\t-' > "$d/rt.status"; echo same > "$d/object"; : > "$d/orig.err"; : > "$d/low.err"
done
d=$w/files/x/hang.cpp; echo 124 > "$d/orig.status"; echo 124 > "$d/low.status"; echo partial > "$d/orig.out"; echo partial > "$d/low.out"
d=$w/files/x/big.cpp; echo 141 > "$d/orig.status"; echo 141 > "$d/low.status"
yes 0123456789abcdef | head -c 1048576 > "$d/orig.out"; cp "$d/orig.out" "$d/low.out"
d=$w/files/x/abort.cpp; echo 134 > "$d/orig.status"; echo 134 > "$d/low.status"; echo x > "$d/orig.out"; echo x > "$d/low.out"
arbace_tool -m arbace.cpp.check judge "$w" "$root/test/cpp/lower-known.edn" "$root/test/cpp/lower-results.edn" > "$t/j.log" 2>&1; st=$?
[ $st = 1 ] && ok "the judge fails (exit 1)" || bad "judge: exit $st"
grep -q $'^x/hang.cpp\tfailing\trun\t' "$w/results.tsv" && ok "two timeouts are not the same run" || bad "hang: $(grep hang "$w/results.tsv")"
grep -q $'^x/big.cpp\tfailing\trun\t' "$w/results.tsv" && ok "two truncated outputs are not the same run" || bad "big: $(grep big "$w/results.tsv")"
grep -q $'^x/abort.cpp\tpass\t' "$w/results.tsv" && ok "the program's own abort (134) is comparable" || bad "abort: $(grep abort "$w/results.tsv")"
finish
