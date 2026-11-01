# needs-host
# CP130 (c): the oracle's failures in the round trip. g++.dg's run had two files clang cannot dump (one over our 300 s, one a segmentation
# fault of clang's) that made the round trip exit 1 for good, and its 745 rejections were taken on trust. Now the oracle's rejections and
# failures are recorded per corpus by name and kind (test/repr/cpp/roundtrip-oracle.edn, REPR_CPP_ROUNDTRIP_ORACLE): recorded ones do
# not fail the round trip, a new rejection, a new failure or a failure of another kind does.
. "$(dirname "$0")/lib.bash"
"$root/bin/repr-cpp" cxxdump-build > "$t/build.log" 2>&1 || die "cxxdump-build"
real=$root/.tmp/repr-cpp/cxxdump
mkdir -p "$t/src"
for n in min rej slow crash; do cp "$root/test/cpp/selftest/min.cpp" "$t/src/$n.cpp"; done
# a cxxdump that rejects rej.cpp as clang does (status 1, a diagnostic), times out on slow.cpp (124) and crashes on crash.cpp (3)
cat > "$t/cx" <<X
#!/bin/sh
for a; do f=\$a; done
case "\$f" in
  rej.cpp) echo "rej.cpp:1:1: error: rejected" >&2; exit 1 ;;
  slow.cpp) exit 124 ;;
  crash.cpp) echo "clang failed (not a rejection)" >&2; exit 3 ;;
esac
exec $real "\$@"
X
chmod +x "$t/cx"
: > "$t/list.tsv"
for n in min rej slow crash; do printf '%s.cpp\tc++23\t-std=c++23\t%s\t-std=c++23\n' "$n" "$t/src/$n.cpp" >> "$t/list.tsv"; done
echo '{:files {}}' > "$t/known.edn"
rt() { # rt WORKNAME ORACLE-FILE [options]: the exit status; the verdicts in one line
  local w=$t/$1 o=$2; shift 2
  REPR_CPP_CXXDUMP_EXE=$t/cx REPR_CPP_CXXDUMP_CACHE=$t/cache REPR_CPP_ROUNDTRIP_KNOWN=$t/known.edn REPR_CPP_ROUNDTRIP_ORACLE=$o \
    "$root/bin/repr-cpp" roundtrip "$t/list.tsv" "$w" --no-objects "$@" > "$t/$1.log" 2>&1
}
vs() { awk -F'\t' '{printf "%s=%s ", $1, $2}' "$t/$1/results.tsv"; }
good='{:corpora {"tc" {:rejected ["rej.cpp"] :fails {"slow.cpp" :timeout "crash.cpp" :crash}}}}'
echo "$good" > "$t/good.edn"
rt w1 "$t/good.edn" --corpus tc; st=$?
[ $st = 0 ] && ok "the recorded rejection and failures do not fail: $(vs w1)" || bad "recorded: exit $st, $(vs w1)"
echo '{:corpora {"tc" {:rejected [] :fails {"slow.cpp" :timeout "crash.cpp" :crash}}}}' > "$t/newrej.edn"
rt w2 "$t/newrej.edn" --corpus tc; st=$?
[ $st = 1 ] && grep -q 'rej.cpp	fail' "$t/w2/results.tsv" && ok "a rejection not recorded fails" || bad "new rejection: exit $st, $(vs w2)"
echo '{:corpora {"tc" {:rejected ["rej.cpp"] :fails {"slow.cpp" :crash "crash.cpp" :crash}}}}' > "$t/kind.edn"
rt w3 "$t/kind.edn" --corpus tc; st=$?
[ $st = 1 ] && grep -q 'slow.cpp	fail' "$t/w3/results.tsv" && ok "a failure of another kind than recorded fails" || bad "kind: exit $st, $(vs w3)"
echo '{:corpora {"tc" {:rejected ["rej.cpp"] :fails {"slow.cpp" :timeout}}}}' > "$t/newfail.edn"
rt w4 "$t/newfail.edn" --corpus tc; st=$?
[ $st = 1 ] && grep -q 'crash.cpp	fail' "$t/w4/results.tsv" && ok "a new failure fails" || bad "new failure: exit $st, $(vs w4)"
# the header line names the corpus as --corpus does
{ echo "#list tc"; cat "$t/list.tsv"; } > "$t/hdr.tsv"
REPR_CPP_CXXDUMP_EXE=$t/cx REPR_CPP_CXXDUMP_CACHE=$t/cache REPR_CPP_ROUNDTRIP_KNOWN=$t/known.edn REPR_CPP_ROUNDTRIP_ORACLE=$t/good.edn \
  "$root/bin/repr-cpp" roundtrip "$t/hdr.tsv" "$t/w5" --no-objects > "$t/w5.log" 2>&1; st=$?
[ $st = 0 ] && ok "the #list header names the corpus" || bad "header: exit $st, $(vs w5)"
# another corpus (no entry): unrecorded, the failures fail as before
rt w6 "$t/good.edn" --corpus other; st=$?
[ $st = 1 ] && ok "a corpus with no record: the dump failures fail" || bad "other corpus: exit $st, $(vs w6)"
# --record-oracle writes the entry, and the run then passes against it
rt w7 "$t/rec.edn" --corpus tc --record-oracle; st=$?
[ $st = 0 ] && grep -q '"rej.cpp"' "$t/rec.edn" && grep -q ':timeout' "$t/rec.edn" && ok "--record-oracle records this run" || bad "record: exit $st, $(cat "$t/rec.edn" 2>&1 | head -5)"
rt w8 "$t/rec.edn" --corpus tc; st=$?
[ $st = 0 ] && ok "a run against its own record passes" || bad "against the record: exit $st, $(vs w8)"
finish
