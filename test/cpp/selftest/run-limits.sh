# The user's decision of 2026-11-15 (round 43; gate 13e's runtime/atomics.cpp): a lowered program runs with a memory cap of its own
# under a CPU-time limit (bin/lib/cppi/runprog.bash's cppi_run_prog, every cirun run of bin/cppi check and run), not under bin/cppi's
# tools' GOMEMLIMIT=1500MiB (lib.bash exports it, as bin/cppi does). A stand-in program prints the limits it sees: GOMEMLIMIT
# CPPI_RUN_MEM_GIB x 768 MiB and an address-space cap of CPPI_RUN_MEM_GIB GiB, and the control, the same stand-in run directly, sees
# the tool's 1500MiB; a program spinning past its CPU seconds is a "timeout" (137 at the CPU limit), a program's own exit 3 is 3.
. "$(dirname "$0")/lib.bash"
. "$root/bin/lib/cppi/runprog.bash"
p=$t/prog; printf '#!/bin/sh\necho "GOMEMLIMIT=${GOMEMLIMIT:-unset} v=$(ulimit -v)"\nexit ${PROG_EXIT:-0}\n' > "$p"; chmod +x "$p"
[ "$GOMEMLIMIT" = 1500MiB ] || bad "the tool's limit not set for the test ($GOMEMLIMIT)"
CPPI_RUN_MEM_GIB=8 cppi_run_prog 30 30 "$t/st" "$p" > "$t/out" 2> "$t/err"
[ "$(cat "$t/st")" = 0 ] && grep -qx 'GOMEMLIMIT=6144MiB v=8388608' "$t/out" && ok "the run's own cap: $(cat "$t/out")" || bad "the run saw $(cat "$t/out") (status $(cat "$t/st"))"
"$p" > "$t/out0"
grep -qx "GOMEMLIMIT=1500MiB v=$(ulimit -v)" "$t/out0" && ok "control: run directly it sees the tool's: $(cat "$t/out0")" || bad "control: $(cat "$t/out0")"
PROG_EXIT=3 cppi_run_prog 30 30 "$t/st" "$p" > "$t/out" 2> "$t/err"
[ "$(cat "$t/st")" = 3 ] && ok "a program's own exit 3 is 3" || bad "own exit: $(cat "$t/st")"
s=$t/spin; printf '#!/bin/sh\nwhile :; do :; done\n' > "$s"; chmod +x "$s"
cppi_run_prog 1 30 "$t/st" "$s" > "$t/out" 2> "$t/err"
[ "$(cat "$t/st")" = timeout ] && ok "a spin past 1 s of CPU is a timeout" || bad "the spin: $(cat "$t/st")"
finish
