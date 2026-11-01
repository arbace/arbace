# needs-host
# CP131 (b): `new A` with `typedef int A[10]` has clang's implicit array size, an IntegerLiteral written nowhere (its range is the type
# name's token), which the printer has no spelling for: the read threw "a literal: (lit __size_t 10)" (g++.old-deja's g++.law/cvt22.C).
# Now the converter reads the size from the source text: not a number, so the allocated type is the text's and the form is `new A`.
. "$(dirname "$0")/lib.bash"
"$root/bin/repr-cpp" cxxdump-build > "$t/build.log" 2>&1 || die "cxxdump-build"
src=$root/test/cpp/selftest/new-implicit.cpp
printf 'new-implicit.cpp\tc++23\t-std=c++23\t%s\t-std=c++23\n' "$src" > "$t/list.tsv"
echo '{:files {}}' > "$t/known.edn"
REPR_CPP_CXXDUMP_CACHE=$t/cache REPR_CPP_ROUNDTRIP_KNOWN=$t/known.edn "$root/bin/repr-cpp" roundtrip "$t/list.tsv" "$t/w" --no-objects > "$t/rt.log" 2>&1; st=$?
v=$(awk -F'\t' '{print $2 " " $4}' "$t/w/results.tsv")
[ $st = 0 ] && [ "$v" = "pass ok" ] && ok "the round trip passes: $v" || bad "round trip: exit $st, $v ($(tail -2 "$t/rt.log"))"
grep -q 'new A;' "$t/w/print/new-implicit.cpp" && grep -q 'new C;' "$t/w/print/new-implicit.cpp" && ok "the print writes new A and new C" || bad "print: $(grep new "$t/w/print/new-implicit.cpp")"
# re-review 032 F1: the arrays of `T[]{...}` and of a macro size stay arrays
for w in 'new int[]{1, 2, 3}' 'new int[]{5}' 'new int[N]' 'new S[]{{1}, {2}}' 'new char[]{"abc"}' 'new int[][2]{{1, 2}, {3, 4}}'; do
  grep -qF "$w" "$t/w/print/new-implicit.cpp" && ok "the print writes $w" || bad "print lacks $w: $(grep -n 'new' "$t/w/print/new-implicit.cpp" | tr '\n' ' ')"
done
finish
