# Review 007's finding 2: any failure to make the IR counted as the oracle's rejection, which never fails the check, and the
# recorded results were not compared. Now only clang's own diagnostics are a rejection, a recorded pass that does not pass
# fails, and an empty list is refused.
. "$(dirname "$0")/lib.bash"
fake_runtime "$t/rt" || die "fake runtime"
min_list "$t/list.tsv"
# 1. clang++ shadowed by a tool that fails without a diagnostic of clang's (as a missing or broken tool, a kill)
mkdir -p "$t/bin"; printf '#!/bin/sh\necho "clang++: error: unable to execute command: Killed" >&2\nexit 1\n' > "$t/bin/clang++"; chmod +x "$t/bin/clang++"
PATH=$t/bin:$PATH "$root/bin/cppi" lower-check "$t/list.tsv" "$t/w1" --runtime "$t/rt" > "$t/w1.log" 2>&1; st=$?
[ $st = 1 ] && ok "a broken clang++ fails the check (exit 1)" || bad "a broken clang++: exit $st ($(tail -1 "$t/w1.log"))"
grep -q $'^min.cpp\tfailing\tir\t' "$t/w1/results.tsv" && ok "outcome ir" || bad "results: $(sed -n 2p "$t/w1/results.tsv")"
# 2. a timeout of ours (124) and clang's own rejection, judged from a work directory; one name is recorded as a pass
w=$t/w2; mkdir -p "$w/files/samples/auto.cpp" "$w/files/x/slow.cpp" "$w/files/x/bad.cpp"
printf 'samples/auto.cpp\tc++23\t\tA\nx/slow.cpp\tc++23\t\tB\nx/bad.cpp\tc++23\t\tC\n' > "$w/list.tsv"
echo 1 > "$w/files/samples/auto.cpp/ir.status"; echo "auto.cpp:3:1: error: unknown type name 'foo'" > "$w/files/samples/auto.cpp/ir.log"
echo 124 > "$w/files/x/slow.cpp/ir.status"; : > "$w/files/x/slow.cpp/ir.log"
echo 1 > "$w/files/x/bad.cpp/ir.status"; echo "bad.cpp:1:2: error: expected ';'" > "$w/files/x/bad.cpp/ir.log"
arbace_tool -m arbace.cpp.check judge "$w" "$root/test/cpp/lower-known.edn" "$root/test/cpp/lower-results.edn" > "$t/w2.log" 2>&1; st=$?
[ $st = 1 ] && ok "the judge fails (exit 1)" || bad "judge: exit $st"
grep -q $'^samples/auto.cpp\tfailing\t' "$w/results.tsv" && ok "a recorded pass rejected now is failing" || bad "auto.cpp: $(grep auto "$w/results.tsv")"
grep -q $'^x/slow.cpp\tfailing\tir\t' "$w/results.tsv" && ok "a timeout is failing (ir)" || bad "slow.cpp: $(grep slow "$w/results.tsv")"
grep -q $'^x/bad.cpp\toracle-rejected\t' "$w/results.tsv" && ok "clang's own diagnostic is the oracle's rejection" || bad "bad.cpp: $(grep bad.cpp "$w/results.tsv")"
# 3. an empty list
: > "$t/empty.tsv"
"$root/bin/cppi" lower-check "$t/empty.tsv" "$t/w3" --runtime "$t/rt" > "$t/w3.log" 2>&1; st=$?
[ $st != 0 ] && ok "an empty list is refused (exit $st)" || bad "an empty list passes"
finish
