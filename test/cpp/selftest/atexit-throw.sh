# needs-host
# CP160 (b): an exception that leaves a function registered with atexit returned into musl's native exit with the pending flag set, so
# the program ended with status 0 where g++'s build terminates (134). test/cpp/selftest/atexit-throw.cpp lowered against the
# lowered runtime: atexit, at_quick_exit and __cxa_atexit are the pending-flag layer's cx$ ones, whose trampolines call cx$uncaught.
. "$(dirname "$0")/lib.bash"
printf 'selftest/atexit-throw.cpp\tc++23\t\t%s\n' "$root/test/cpp/selftest/atexit-throw.cpp" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" > "$t/w.log" 2>&1; st=$?
d=$t/w/files/selftest/atexit-throw.cpp
[ "$(cat "$d/orig.status" 2>&1)" = 134 ] && ok "the original terminates (134)" || bad "the original's status: $(cat "$d/orig.status" 2>&1)"
[ "$(cat "$d/gxx.status" 2>&1)" = 134 ] && ok "g++'s build terminates (134)" || bad "g++'s build's status: $(cat "$d/gxx.status" 2>&1)"
[ -f "$d/low.err" ] || bad "the lowered run left no stderr (low.err)"
[ "$(cat "$d/low.status" 2>&1)" = 134 ] && ok "the lowered program terminates (134)" || bad "the lowered program's status: $(cat "$d/low.status" 2>&1)"
grep -q "terminate called after throwing an instance of 'int'" "$d/low.err" && ok "the terminate message" || bad "no terminate message: $(head -3 "$d/low.err" 2>&1)"
cmp -s "$d/low.err" "$d/gxx.err" && ok "stderr is g++'s build's" || bad "stderr differs from g++'s build's"
[ $st = 0 ] && grep -q $'^selftest/atexit-throw.cpp\tpass\t' "$t/w/results.tsv" && ok "lower-check passes (stderr and status the original's)" || bad "lower-check exits $st: $(sed -n 2p "$t/w/results.tsv" 2>&1)"
# at_quick_exit: the last registered runs first, throws, and terminates before the first one runs
printf 'selftest/quick-exit-throw.cpp\tc++23\t\t%s\n' "$root/test/cpp/selftest/quick-exit-throw.cpp" > "$t/list2.tsv"
"$root/bin/cppi" lower-check "$t/list2.tsv" "$t/w2" > "$t/w2.log" 2>&1; st=$?
d=$t/w2/files/selftest/quick-exit-throw.cpp
[ "$(cat "$d/low.status" 2>&1)" = 134 ] && ok "at_quick_exit: the lowered program terminates (134)" || bad "at_quick_exit status: $(cat "$d/low.status" 2>&1)"
[ $st = 0 ] && cmp -s "$d/low.err" "$d/gxx.err" && ok "at_quick_exit: stderr is g++'s build's" || bad "at_quick_exit: lower-check exits $st: $(head -4 "$d/low.err" 2>&1)"
# report 066's R1 and R2: at_quick_exit's order (a handler registered by a handler runs next) and musl's 32 handlers, the lowered
# program's stderr and status the original's and g++'s build's
for n in quick-exit-order quick-exit-many; do
  printf 'selftest/%s.cpp\tc++23\t\t%s\n' "$n" "$root/test/cpp/selftest/$n.cpp" > "$t/l-$n.tsv"
  "$root/bin/cppi" lower-check "$t/l-$n.tsv" "$t/w-$n" > "$t/w-$n.log" 2>&1; st=$?
  d=$t/w-$n/files/selftest/$n.cpp
  [ $st = 0 ] && cmp -s "$d/low.err" "$d/orig.err" && cmp -s "$d/low.err" "$d/gxx.err" && ok "$n: lowered stderr is the original's and g++'s" || bad "$n: lower-check exits $st: $(tr '\n' ';' < "$d/low.err" 2>&1) wanted $(tr '\n' ';' < "$d/orig.err" 2>&1)"
done
[ "$(tr '\n' ' ' < "$t/w-quick-exit-order/files/selftest/quick-exit-order.cpp/orig.err" 2>&1)" = "q4 q2 q3 q1 " ] && ok "the original runs q4 q2 q3 q1" || bad "the original's order"
grep -q '^failed 8 ran 32$' "$t/w-quick-exit-many/files/selftest/quick-exit-many.cpp/orig.err" 2>&1 && ok "the original refuses 8 of 40" || bad "the original's count: $(cat "$t/w-quick-exit-many/files/selftest/quick-exit-many.cpp/orig.err" 2>&1)"
finish
