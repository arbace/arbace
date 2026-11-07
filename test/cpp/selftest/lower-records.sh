# needs-host
# ORS27 (b): from the records the lowering's gcc and run levels are skipped and the lowered C's level is repr/c's read and ci's check
# (FILES/NAME/ci-check.status). The judge passes a program whose C ci checks, knows one refused in a class of test/cpp/divergences.edn
# (CS35's long double), and fails one refused for another reason and one whose C level did not run (the negative cases); no toolchain.
. "$(dirname "$0")/lib.bash"
w=$t/work; mkdir -p "$w"
cp "$root/test/cpp/divergences.edn" "$t/divergences.edn"; echo '{}' > "$t/known.edn"
mk() { # NAME CI-STATUS CI-MESSAGE: a program the records' lowering left at the C level
  local d=$w/files/$1; mkdir -p "$d"
  echo 0 > "$d/ir.status"; echo 0 > "$d/orig-link.status"; printf 'ok\n' > "$d/rt.status"; echo skipped > "$d/object"
  echo skipped > "$d/gcc.status"; echo 0 > "$d/orig.status"; : > "$d/orig.out"; : > "$d/orig.err"; : > "$d/lowered.c"
  [ "$2" = - ] || { echo "$2" > "$d/ci-check.status"; echo "$3" > "$d/ci-check.log"; }
  printf '%s\tc++23\t\t%s\n' "$1" "$d/x.cpp" >> "$w/list.tsv"
}
: > "$w/list.tsv"
mk a.cpp ok ""
mk b.cpp check-error "26: static assertion failed; 32: static assertion failed uses-long-double"
mk c.cpp check-error "5: use of undeclared identifier 'x'"
mk d.cpp - ""
arbace_tool -m arbace.cpp.check judge "$w" "$t/known.edn" "$t/none.edn" none > "$t/judge.log" 2>&1; st=$?
col() { awk -F'\t' -v n="$1" -v c="$2" '$1 == n {print $c}' "$w/results.tsv"; }
[ "$(col a.cpp 2)" = pass ] && [ "$(col a.cpp 7)" = skipped ] && [ "$(col a.cpp 8)" = skipped ] && ok "ci's check of the lowered C passes, gcc and the run skipped" || bad "a: $(grep '^a.cpp' "$w/results.tsv")"
[ "$(col b.cpp 2)" = known ] && [ "$(col b.cpp 12)" = long-double ] && ok "a refusal in a class of divergences.edn is known in that class" || bad "b: $(grep '^b.cpp' "$w/results.tsv")"
[ "$(col c.cpp 2)" = failing ] && [ "$(col c.cpp 3)" = c-check ] && ok "a refusal outside the classes fails (negative)" || bad "c: $(grep '^c.cpp' "$w/results.tsv")"
[ "$(col d.cpp 2)" = failing ] && ok "a C level that did not run fails (negative)" || bad "d: $(grep '^d.cpp' "$w/results.tsv")"
[ $st = 1 ] && grep -q 'failing: c.cpp c-check' "$t/judge.log" && ok "the judge exits 1 and names the refusal" || bad "judge: exit $st, $(tail -3 "$t/judge.log")"
finish
