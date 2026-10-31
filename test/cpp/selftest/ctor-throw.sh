# Review 007's finding 9: a throw that leaves a dynamic initializer returned into the C library's constructor loop with the
# pending flag set, so later constructors ran (and printed) before the program terminated. test/cpp/runtime/samples/
# ctor-throw.cpp lowered against the lowered runtime (.tmp/cppi/runtime, rebuilt by lower-check when not at the key):
# terminate at once, the original's stderr and status 134.
. "$(dirname "$0")/lib.bash"
printf 'runtime/ctor-throw.cpp\tc++23\t\t%s\n' "$root/test/cpp/runtime/samples/ctor-throw.cpp" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" > "$t/w.log" 2>&1; st=$?
d=$t/w/files/runtime/ctor-throw.cpp
[ "$(cat "$d/orig.status" 2>&1)" = 134 ] && ok "the original terminates (134)" || bad "the original's status: $(cat "$d/orig.status" 2>&1)"
grep -q 'a later constructor' "$d/low.err" && bad "the lowered program ran a later constructor" || ok "no later constructor ran"
[ $st = 0 ] && grep -q $'^runtime/ctor-throw.cpp\tpass\t' "$t/w/results.tsv" && ok "lower-check passes (stderr and status the original's)" || bad "lower-check exits $st: $(sed -n 2p "$t/w/results.tsv" 2>&1)"
finish
