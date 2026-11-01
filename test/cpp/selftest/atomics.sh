# needs-host
# CP121 (c): the atomics were plain operations (CP1), so state the lowered program shares across threads raced: an atomic
# counter, shared_ptr's refcounts, a function-local static's guard. Now gcc's __atomic_* builtins with the IR's orderings.
# (1) test/cpp/selftest/atomics.ll, every form clang writes, lowered alone: each builtin with its ordering, compiled by gcc;
# (2) test/cpp/runtime/samples/atomics.cpp (4 threads) lowered against the lowered runtime (.tmp/cppi/runtime, rebuilt by
# lower-check when not at the key): the output and status of the original's and g++'s.
. "$(dirname "$0")/lib.bash"
"$root/bin/cppi" lower "$root/test/cpp/selftest/atomics.ll" -o "$t/atomics.c" > "$t/lower.log" 2>&1 || bad "the lowering failed: $(grep -v -i 'warning\|boxing\|recur arg' "$t/lower.log" | head -3)"
for want in '__atomic_fetch_add((volatile unsigned \*)l$p, l$v, 4)' '__atomic_exchange_n((unsigned \*)l$p, l$v, 0)' \
    '__atomic_compare_exchange_n((unsigned \*)l$p, &l$x.f0, l$v, 1, 5, 0)' '__atomic_compare_exchange_n((volatile unsigned \*)l$p, &l$y.f0, l$v, 0, 2, 2)' \
    '__atomic_load_n((unsigned \*)l$p, 2)' '__atomic_load_n((volatile unsigned \*)l$p, 5)' '__atomic_store_n((unsigned \*)l$p, l$l, 3)' \
    '__atomic_thread_fence(5)' '__atomic_signal_fence(2)' 'while (!__atomic_compare_exchange((float \*)l$p, &l$fa' \
    'while (!__atomic_compare_exchange((unsigned \*)l$p, &l$m' '__atomic_exchange_n((void \*\*)l$p, (void \*)0, 5)'; do
  grep -q -F "$(printf '%s' "$want" | sed 's/\\//g')" "$t/atomics.c" && ok "lowered: $want" || bad "not lowered as $want"
done
gcc -std=gnu23 -O0 -w -fno-builtin -fno-strict-aliasing -c "$t/atomics.c" -o "$t/atomics.o" > "$t/gcc.log" 2>&1 && ok "gcc compiles it" || bad "gcc: $(head -3 "$t/gcc.log")"
printf 'runtime/atomics.cpp\tc++23\t\t%s\n' "$root/test/cpp/runtime/samples/atomics.cpp" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" > "$t/w.log" 2>&1; st=$?
d=$t/w/files/runtime/atomics.cpp
grep -q '^counter 399994$' "$d/orig.out" 2>&1 && ok "the original counts 399994" || bad "the original's output: $(head -2 "$d/orig.out" 2>&1)"
cmp -s "$d/orig.out" "$d/gxx.out" && ok "g++'s build prints the same" || bad "g++'s build differs"
[ "$(cat "$d/low.status" 2>&1)" = 0 ] && ok "the lowered program exits 0" || bad "the lowered program's status: $(cat "$d/low.status" 2>&1)"
cmp -s "$d/low.out" "$d/orig.out" && ok "the lowered program prints the original's" || bad "the lowered output: $(head -3 "$d/low.out" 2>&1)"
grep -q '__atomic_fetch_add' "$d/lowered.c" 2>&1 && ok "the lowered sample uses the builtins" || bad "no __atomic_fetch_add in the lowered sample"
[ $st = 0 ] && grep -q $'^runtime/atomics.cpp\tpass\t' "$t/w/results.tsv" && ok "lower-check passes" || bad "lower-check exits $st: $(grep "^runtime/atomics.cpp" "$t/w/results.tsv" 2>&1)"
finish
