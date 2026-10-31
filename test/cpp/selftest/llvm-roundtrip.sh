# Review 007's finding 11: bin/repr-llvm roundtrip printed its counts and exited 0 whatever they were. It fails now unless
# every file's forms are equal and its objects the same.
. "$(dirname "$0")/lib.bash"
clang++ -std=c++23 -O0 -S -emit-llvm "$root/test/cpp/selftest/min.cpp" -o "$t/min.ll" || die "clang++"
printf 'min\t%s\n' "$t/min.ll" > "$t/good.tsv"
"$root/bin/repr-llvm" roundtrip "$t/good.tsv" "$t/w1" > "$t/1.log" 2>&1; st=$?
[ $st = 0 ] && ok "a file that round-trips passes" || bad "the good list: exit $st: $(tail -1 "$t/1.log")"
printf 'min\t%s\nbad\t%s\n' "$t/min.ll" "$t/bad.ll" > "$t/bad.tsv"
printf 'define i32 @f( {\n' > "$t/bad.ll"
"$root/bin/repr-llvm" roundtrip "$t/bad.tsv" "$t/w2" > "$t/2.log" 2>&1; st=$?
[ $st = 1 ] && ok "a file that does not round-trip fails the verb (exit 1)" || bad "the bad list: exit $st: $(tail -1 "$t/2.log")"
finish
