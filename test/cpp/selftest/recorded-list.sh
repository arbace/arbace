# Review 018's R4: the recorded results guarded only the names a run listed, so a run of a recorded list that had lost files (another
# N, a filter change) passed with fewer, and the record's count was never compared. Now a run of a recorded list (named, or the list
# more than half of whose names it holds) fails on a recorded name missing and on another count than the summary's.
. "$(dirname "$0")/lib.bash"
rec=$root/test/cpp/lower-results.edn
mkwork() { # mkwork DIR NAME...: a judged work directory whose files all pass
  local w=$1 n d; shift; mkdir -p "$w"; : > "$w/list.tsv"
  for n in "$@"; do
    d=$w/files/$n; mkdir -p "$d"; printf '%s\tc++23\t\tP\n' "$n" >> "$w/list.tsv"
    for f in ir.status orig-link.status gcc.status; do echo 0 > "$d/$f"; done
    printf 'ok\t\n' > "$d/rt.status"; echo same > "$d/object"; echo 0 > "$d/orig.status"; echo 0 > "$d/low.status"
    : > "$d/orig.out"; : > "$d/low.out"; : > "$d/orig.err"; : > "$d/low.err"
  done
}
judge() { arbace_tool -m arbace.cpp.check judge "$1" "$root/test/cpp/lower-known.edn" "$rec" ${2:+"$2"} > "$1.log" 2>&1; }
all=$(grep -o '^  "\(samples\|runtime\)/[^"]*"' "$rec" | tr -d '" ' | sort -u)
set -- $all
[ $# = 14 ] || die "the record's samples: $#"
mkwork "$t/full" "$@"; judge "$t/full"; st=$?
[ $st = 0 ] && ok "the whole samples list passes" || bad "the whole list: exit $st, $(tail -2 "$t/full.log")"
# a file lost from the list (13 of 14)
shift; mkwork "$t/lost" "$@"; judge "$t/lost"; st=$?
[ $st = 1 ] && grep -q 'recorded names missing: runtime/ctor-throw.cpp' "$t/lost.log" && ok "a run of the samples that lost a file fails, naming it" || bad "13 of 14: exit $st, $(tail -2 "$t/lost.log")"
# the same, the list named
judge "$t/lost" samples; st=$?
[ $st = 1 ] && ok "named: fails too" || bad "named: exit $st"
# a single file, or a run of another list, is not held to the record
mkwork "$t/one" "$1"; judge "$t/one"; st=$?
[ $st = 0 ] && ok "one sample alone is not a run of the recorded list" || bad "one sample: exit $st, $(tail -2 "$t/one.log")"
judge "$t/lost" none; st=$?
[ $st = 0 ] && ok "none: not held" || bad "none: exit $st"
# an extra file: the count is not the record's
mkwork "$t/extra" $all "samples/new.cpp"; judge "$t/extra"; st=$?
[ $st = 1 ] && grep -q '15 files, the record 14' "$t/extra.log" && ok "a count other than the record's fails" || bad "extra: exit $st, $(tail -2 "$t/extra.log")"
finish
