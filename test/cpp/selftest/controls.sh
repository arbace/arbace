# needs-host
# GTS81 (a), queue 064: lower-check's negative control (test/cpp/controls.edn). The judge on a made-up WORK of the control's two
# files, each passing (its lowered run its original's): the control (the subject's lowered run against the reference's
# original) differs and the judgment passes; the subject's lowered output made the reference's (the control's failure removed)
# fails the judgment (control passed); the reference without an original run, not judged, fails; the reference left out of the
# list leaves the control.
. "$(dirname "$0")/lib.bash"
w=$t/w
mkfile() { # NAME OUT: a passing file whose original and lowered runs print OUT
  local d=$w/files/$1; mkdir -p "$d"
  for f in ir.status orig-link.status gcc.status; do echo 0 > "$d/$f"; done
  echo $'ok\t-' > "$d/rt.status"; echo same > "$d/object"
  echo 0 > "$d/orig.status"; echo 0 > "$d/low.status"; printf '%s\n' "$2" > "$d/orig.out"; printf '%s\n' "$2" > "$d/low.out"; : > "$d/orig.err"; : > "$d/low.err"
}
judge() { arbace_tool -m arbace.cpp.check judge "$w" "$root/test/cpp/lower-known.edn" "$root/test/cpp/lower-results.edn" none > "$t/$1.log" 2>&1; }
mkfile samples/bitfields.cpp "bits 1 2 3"; mkfile samples/io.cpp "hello"
printf 'samples/bitfields.cpp\tc++23\t\tA\nsamples/io.cpp\tc++23\t\tB\n' > "$w/list.tsv"
judge j1; st=$?
[ $st = 0 ] && grep -q 'control _control/bitfields-vs-io .*differs, as a control must' "$t/j1.log" && ok "the control differs; the judgment passes" || bad "j1: exit $st, $(tail -2 "$t/j1.log")"
cp "$w/files/samples/io.cpp/orig.out" "$w/files/samples/bitfields.cpp/low.out"; cp "$w/files/samples/io.cpp/orig.out" "$w/files/samples/bitfields.cpp/orig.out"
judge j2; st=$?
[ $st = 1 ] && grep -q 'control passed: _control/bitfields-vs-io' "$t/j2.log" && ok "the control made to pass fails the judgment" || bad "j2: exit $st, $(tail -2 "$t/j2.log")"
printf 'bits 1 2 3\n' > "$w/files/samples/bitfields.cpp/low.out"; printf 'bits 1 2 3\n' > "$w/files/samples/bitfields.cpp/orig.out"
rm "$w/files/samples/io.cpp/orig.status"
judge j3; st=$?
[ $st = 1 ] && grep -q 'control not judged: _control/bitfields-vs-io' "$t/j3.log" && ok "the reference with no original run: not judged, failing" || bad "j3: exit $st, $(tail -2 "$t/j3.log")"
printf 'samples/bitfields.cpp\tc++23\t\tA\n' > "$w/list.tsv"
judge j4; st=$?
[ $st = 0 ] && grep -q 'control _control/bitfields-vs-io: left' "$t/j4.log" && ok "the reference not in the list: left" || bad "j4: exit $st, $(tail -2 "$t/j4.log")"
finish
