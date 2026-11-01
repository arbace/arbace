# needs-host
# Report 043's F3: gcc's documentation says a compare-exchange's failure order cannot be stronger than its success order;
# LLVM allows it. The lowering passes the join of the two as the success order (monotonic+acquire -> acquire, release+acquire ->
# acq_rel, anything+seq_cst -> seq_cst) and the failure order as it is.
. "$(dirname "$0")/lib.bash"
"$root/bin/cppi" lower "$root/test/cpp/selftest/atomic-failure-order.ll" -o "$t/a.c" > "$t/lower.log" 2>&1 || bad "the lowering failed: $(grep -v -i 'warning\|boxing\|recur arg' "$t/lower.log" | head -3)"
# one line per cmpxchg, in order: the last two arguments are success and failure
grep -o '__atomic_compare_exchange_n([^;]*' "$t/a.c" | sed 's/.*, \([0-9]\), \([0-9]\)).*/\1 \2/;s/.*, \([0-9]\), \([0-9]\)$/\1 \2/' > "$t/pairs"
printf '0 0\n2 2\n5 5\n2 0\n2 2\n5 5\n3 0\n4 2\n5 5\n4 0\n4 2\n5 5\n5 0\n5 2\n5 5\n' > "$t/want"
cmp -s "$t/pairs" "$t/want" && ok "the success orders are the joins" || bad "the (success, failure) pairs: $(tr '\n' ';' < "$t/pairs") wanted $(tr '\n' ';' < "$t/want")"
gcc -std=gnu23 -O0 -Winvalid-memory-model -Werror -fno-builtin -fno-strict-aliasing -c "$t/a.c" -o "$t/a.o" > "$t/gcc.log" 2>&1 && ok "gcc compiles it without -Winvalid-memory-model" || bad "gcc: $(head -3 "$t/gcc.log")"
finish
