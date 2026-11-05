# needs-host
# CPR1 (doc/cpp/PRINT-DESIGN-REVIEW.md): a file clang rejects that the native reader read is judged at level 1 and level R (the
# print fails like the original: clang's and g++'s errors by message, positions dropped), not left oracle-rejects; one the native
# reader did not read stays oracle-rejects; a level-R difference fails unless known as :rejected-differs (CPR2); known-rejected writes it.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
# rej WORK NAME CLANG-PRINT-ERR GXX-ORIG-ERR GXX-PRINT-ERR [unread]: NAME rejected by clang, read natively unless "unread"
rej() {
  local w=$1 n=$2 d
  for d in orig dump2 forms forms2 diag/orig diag/print; do mkdir -p "$w/$d/$(dirname "$n")"; done
  echo 1 > "$w/orig/$n.status"; printf '%s:3:5: error: unknown type name %s\n' "$n" "'char8_t'" > "$w/orig/$n.err"
  echo 1 > "$w/dump2/$n.status"; printf '%s:9:7: error: %s\n' "$n" "$3" > "$w/dump2/$n.err"
  for d in orig print; do echo 1 > "$w/diag/$d/$n.gxx.status"; done
  printf '%s:3:5: error: %s\n' "$n" "$4" > "$w/diag/orig/$n.gxx.log"; printf '%s:8:1: error: %s\n' "$n" "$5" > "$w/diag/print/$n.gxx.log"
  echo "(forms)" > "$w/forms/$n.ae"; echo "(forms)" > "$w/forms2/$n.ae"
  if [ "${6:-}" = unread ]; then printf '%s\trefused\t0\tx: y\n' "$n" >> "$w/read.tsv"
  else printf '%s\tok\t0\t{}\tsame\n' "$n" >> "$w/read.tsv"; printf '%s\tok\t0\t{}\t-\n' "$n" >> "$w/read2.tsv"; fi
  printf '%s\tc++23\t\tP\t\n' "$n" >> "$w/list.tsv"
}
w=$t/w1; mkdir -p "$w"
rt_file "$w" a.C
rej "$w" b.C "unknown type name 'char8_t'" "'x' was not declared in this scope (see x.C:4:2)" "'x' was not declared in this scope (see x.C:7:9)"
rej "$w" c.C "unknown type name 'char8_t'" "static assertion failed" "expected ';' before '}' token"
rej "$w" d.C "unknown type name 'char8_t'" "e" "e" unread
echo '{:files {}}' > "$t/known.edn"
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/known.edn" no-objects > "$t/w1.log" 2>&1; st=$?
v() { awk -F'\t' -v n="$2" '$1==n{print $2 " " $4}' "$1/results.tsv"; }
[ "$(v "$w" b.C)" = "pass rejected-native" ] && ok "a rejected file read natively, its errors the same without positions: pass" || bad "b: $(v "$w" b.C)"
[ "$(v "$w" c.C)" = "fail rejected-native:gxx" ] && [ $st = 1 ] && ok "g++'s errors differ: level R fails" || bad "c: $(v "$w" c.C), exit $st"
[ "$(v "$w" d.C)" = "oracle-rejects oracle-rejects" ] && ok "a rejected file the native reader refused stays oracle-rejects" || bad "d: $(v "$w" d.C)"
grep -q ':rejected-native 2, :rejected-native-pass 1' "$w/summary.edn" && ok "the summary counts them" || bad "summary: $(cat "$w/summary.edn")"
printf '{:classes {}\n :files {\n  "a.C" :forms-differ}}\n' > "$t/known2.edn"
arbace_tool -m arbace.repr.cpp.rtcheck known-rejected "$w" "$t/known2.edn" > "$t/kr.log" 2>&1
grep -q '"c.C" :rejected-differs' "$t/known2.edn" && ! grep -q '"b.C"' "$t/known2.edn" && ok "known-rejected classes the failing one alone" || bad "known-rejected: $(cat "$t/kr.log") $(cat "$t/known2.edn")"
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/known2.edn" no-objects > "$t/w2.log" 2>&1; st=$?
[ "$(v "$w" c.C)" = "known rejected-native:gxx" ] && [ $st = 0 ] && ok "known as :rejected-differs: known, no failure" || bad "c known: $(v "$w" c.C), exit $st, $(tail -1 "$t/w2.log")"
# clang accepting the print of a file it rejects is a level-R difference too
w=$t/w3; mkdir -p "$w"
rej "$w" e.C "x" "e" "e"; echo 0 > "$w/dump2/e.C.status"; : > "$w/dump2/e.C.err"
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$t/known.edn" no-objects > "$t/w3.log" 2>&1
[ "$(v "$w" e.C)" = "fail rejected-native:clang" ] && ok "clang accepting the print fails level R" || bad "e: $(v "$w" e.C)"
finish
