# needs-host
# CP120 (b) (review 007's finding 9): an exception that leaves a std::thread's callable returned into musl's native thread start
# with the pending flag set, so the thread ended silently and main went on (status 0, "joined" on stderr), where g++'s build
# terminates. test/cpp/runtime/samples/thread-throw.cpp lowered against the lowered runtime (.tmp/cppi/runtime, rebuilt by
# lower-check when not at the key): the lowered code's pthread_create is the pending-flag layer's cx$pthread_create, whose
# trampoline calls cx$uncaught: the terminate message and status 134, as the original's and g++'s.
. "$(dirname "$0")/lib.bash"
printf 'runtime/thread-throw.cpp\tc++23\t\t%s\n' "$root/test/cpp/runtime/samples/thread-throw.cpp" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" > "$t/w.log" 2>&1; st=$?
d=$t/w/files/runtime/thread-throw.cpp
[ "$(cat "$d/orig.status" 2>&1)" = 134 ] && ok "the original terminates (134)" || bad "the original's status: $(cat "$d/orig.status" 2>&1)"
[ "$(cat "$d/gxx.status" 2>&1)" = 134 ] && ok "g++'s build terminates (134)" || bad "g++'s build's status: $(cat "$d/gxx.status" 2>&1)"
[ -f "$d/low.err" ] || bad "the lowered run left no stderr (low.err)"
[ "$(cat "$d/low.status" 2>&1)" = 134 ] && ok "the lowered program terminates (134)" || bad "the lowered program's status: $(cat "$d/low.status" 2>&1)"
grep -q 'joined' "$d/low.err" && bad "the lowered program's main went on after the thread's throw" || ok "main did not go on"
grep -q "terminate called after throwing an instance of 'std::runtime_error'" "$d/low.err" && ok "the terminate message" || bad "no terminate message: $(head -3 "$d/low.err" 2>&1)"
cmp -s "$d/low.err" "$d/gxx.err" && ok "stderr is g++'s build's" || bad "stderr differs from g++'s build's"
[ $st = 0 ] && grep -q $'^runtime/thread-throw.cpp\tpass\t' "$t/w/results.tsv" && ok "lower-check passes (stderr and status the original's)" || bad "lower-check exits $st: $(sed -n 2p "$t/w/results.tsv" 2>&1)"
finish
