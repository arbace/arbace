# needs-host
# Report 043's F1: a call of libatomic's size-generic entry points must not keep the names gcc reads as its builtins
# (gcc: "incorrect number of arguments to function '__atomic_load'"). Lowered alone, then compiled by gcc.
. "$(dirname "$0")/lib.bash"
"$root/bin/cppi" lower "$root/test/cpp/selftest/atomic-libcalls.ll" -o "$t/a.c" > "$t/lower.log" 2>&1 || bad "the lowering failed: $(grep -v -i 'warning\|boxing\|recur arg' "$t/lower.log" | head -3)"
grep -q 'g\$__atomic_load' "$t/a.c" && ok "the call has a g\$ name" || bad "no g\$__atomic_load in the lowered C"
grep -q 'g\$__sync_val_compare_and_swap_1' "$t/a.c" && ok "a __sync_ name has a g\$ name" || bad "no g\$__sync_val_compare_and_swap_1 in the lowered C"
grep -q -F '__asm__("__atomic_load")' "$t/a.c" && ok "the linker's name is an asm label" || bad "no asm label for __atomic_load"
gcc -std=gnu23 -O0 -w -fno-builtin -fno-strict-aliasing -c "$t/a.c" -o "$t/a.o" > "$t/gcc.log" 2>&1 && ok "gcc compiles it" || bad "gcc: $(head -3 "$t/gcc.log")"
nm "$t/a.o" 2>&1 | grep -q ' U __atomic_load$' && ok "the object calls __atomic_load" || bad "the object's undefined symbols: $(nm "$t/a.o" 2>&1 | grep ' U ' | head -3)"
finish
