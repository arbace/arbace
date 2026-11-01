# needs-host
# Re-review 032 F4: pp-check's lists name their corpus for the round trip's oracle record. `lists` writes WORK/ID/roundtrip.list, the
# list under a first line `#list KEY` (KEY: the id, `-every-N` when sampled, `-all` otherwise), the key of test/repr/cpp/roundtrip-oracle.edn.
. "$(dirname "$0")/lib.bash"
arbace_tool -m arbace.repr.cpp.ppcheck lists "$t/w1" "{:checkout \"$root\"}" 1 cpp-lexical > "$t/l1.log" 2>&1 || die "lists: $(tail -3 "$t/l1.log")"
arbace_tool -m arbace.repr.cpp.ppcheck lists "$t/w2" "{:checkout \"$root\"}" 3 cpp-lexical > "$t/l2.log" 2>&1 || die "lists sampled: $(tail -3 "$t/l2.log")"
[ "$(head -1 "$t/w1/cpp-lexical/roundtrip.list")" = "#list cpp-lexical-all" ] && ok "unsampled: #list cpp-lexical-all" || bad "unsampled header: $(head -1 "$t/w1/cpp-lexical/roundtrip.list")"
[ "$(head -1 "$t/w2/cpp-lexical/roundtrip.list")" = "#list cpp-lexical-every-3" ] && ok "sampled: #list cpp-lexical-every-3" || bad "sampled header: $(head -1 "$t/w2/cpp-lexical/roundtrip.list")"
[ "$(tail -n +2 "$t/w2/cpp-lexical/roundtrip.list" | cmp - "$t/w2/cpp-lexical/gxx.list"; echo $?)" = 0 ] && ok "the rest is gxx.list" || bad "roundtrip.list differs from gxx.list"
finish
