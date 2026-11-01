# needs-host
# CP161 (b): a 16-byte std::atomic (here a struct of two longs) is calls of libatomic's __atomic_*_16; -latomic is on the link lines of
# the original, g++'s build and the lowered program (bin/cppi lower-check), so the three run and print the same.
. "$(dirname "$0")/lib.bash"
printf 'selftest/atomic16.cpp\tc++23\t\t%s\n' "$root/test/cpp/selftest/atomic16.cpp" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" > "$t/w.log" 2>&1; st=$?
d=$t/w/files/selftest/atomic16.cpp
grep -q '^1 2 1 5 6$' "$d/orig.out" 2>&1 && ok "the original links and prints" || bad "the original: $(cat "$d/orig-link.log" 2>&1 | head -3) $(head -2 "$d/orig.out" 2>&1)"
cmp -s "$d/orig.out" "$d/gxx.out" && ok "g++'s build prints the same" || bad "g++'s build: $(head -3 "$d/gxx-link.log" 2>&1)"
cmp -s "$d/low.out" "$d/orig.out" && ok "the lowered program prints the original's" || bad "the lowered program: $(head -3 "$d/gcc.log" 2>&1) $(head -2 "$d/low.out" 2>&1)"
[ $st = 0 ] && grep -q $'^selftest/atomic16.cpp\tpass\t' "$t/w/results.tsv" && ok "lower-check passes" || bad "lower-check exits $st"
finish
