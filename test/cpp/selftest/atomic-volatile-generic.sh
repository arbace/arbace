# needs-host
# Report 043's F4: a volatile atomic float load, store or exchange, or a volatile atomicrmw lowered to a compare-exchange loop,
# keeps volatile in the pointer's type (LLVM.md section 7), as the _n builtins' places do.
. "$(dirname "$0")/lib.bash"
"$root/bin/cppi" lower "$root/test/cpp/selftest/atomic-volatile-generic.ll" -o "$t/a.c" > "$t/lower.log" 2>&1 || bad "the lowering failed: $(grep -v -i 'warning\|boxing\|recur arg' "$t/lower.log" | head -3)"
for want in '__atomic_load((volatile float *)l$p' '__atomic_store((volatile float *)l$p' '__atomic_exchange((volatile float *)l$p' \
    '__atomic_compare_exchange((volatile float *)l$p' '__atomic_compare_exchange((volatile unsigned *)l$p'; do
  grep -q -F "$want" "$t/a.c" && ok "lowered: $want" || bad "not lowered as $want"
done
gcc -std=gnu23 -O0 -w -fno-builtin -fno-strict-aliasing -c "$t/a.c" -o "$t/a.o" > "$t/gcc.log" 2>&1 && ok "gcc compiles it" || bad "gcc: $(head -3 "$t/gcc.log")"
finish
