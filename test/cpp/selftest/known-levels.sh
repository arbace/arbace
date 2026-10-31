# Review 007's finding 8: both known files masked any failure of a listed file, whatever its level. Now a listed file is
# known only when it fails at the level its class names: the round trip's classes by the first level that fails, the
# lowering check's refusal classes at the lowering (that refusal) and its run classes at the run.
. "$(dirname "$0")/lib.bash"
. "$(dirname "$0")/lib-rt.bash"
# the round trip
known=$t/rt-known.edn
echo '{:files {"crash.C" :gxx-objects-location "loc.C" :gxx-objects-location "pr.C" :print-rejected "ast.C" :ast-differs}}' > "$known"
w=$t/rt; mkdir -p "$w"
for n in loc.C pr.C ast.C; do rt_file "$w" "$n"; done
mkdir -p "$w/orig"; echo 0 > "$w/orig/crash.C.status"; printf 'crash.C\tc++23\t\tP\t\n' >> "$w/list.tsv"
printf 'crash.C\tcrash\t0\tjava.lang.NullPointerException\n' >> "$w/read.tsv"
rt_obj "$w" loc.C gxx 0 0 differs; rt_obj "$w" loc.C clang 0 0
echo "(other forms)" > "$w/forms2/pr.C.ae"; echo 1 > "$w/dump2/pr.C.status"; rt_obj "$w" pr.C gxx 0 1; rt_obj "$w" pr.C clang 0 1
echo "(other norm)" > "$w/dump2/ast.C.norm"; rt_obj "$w" ast.C gxx 0 0; rt_obj "$w" ast.C clang 0 0 differs
arbace_tool -m arbace.repr.cpp.rtcheck judge "$w" "$known" > "$t/rt.log" 2>&1; st=$?
v() { awk -F'\t' -v n="$2" '$1==n {print $2}' "$1/results.tsv"; }
[ $st = 1 ] && ok "the round trip's judge fails (exit 1)" || bad "the round trip's judge: exit $st"
[ "$(v "$w" crash.C)" = fail ] && ok "a gxx-objects-location file that crashes in read fails" || bad "crash.C: $(v "$w" crash.C)"
[ "$(v "$w" loc.C)" = known ] && ok "g++'s objects alone differing: known as gxx-objects-location" || bad "loc.C: $(v "$w" loc.C)"
[ "$(v "$w" pr.C)" = known ] && ok "level 1 failing: known as print-rejected" || bad "pr.C: $(v "$w" pr.C)"
[ "$(v "$w" ast.C)" = known ] && ok "level 3 failing first: known as ast-differs" || bad "ast.C: $(v "$w" ast.C)"
# the lowering check
lk=$t/lower-known.edn
echo '{"x/asm.cpp" :inline-asm "x/run.cpp" :inline-asm "x/cl.cpp" :uncaught-cleanups}' > "$lk"
w=$t/lc; mkdir -p "$w"
printf 'x/asm.cpp\tc++23\t\tA\nx/run.cpp\tc++23\t\tB\nx/cl.cpp\tc++23\t\tC\n' > "$w/list.tsv"
for n in asm run cl; do
  d=$w/files/x/$n.cpp; mkdir -p "$d"
  for f in ir.status orig-link.status gcc.status; do echo 0 > "$d/$f"; done
  echo $'ok\t-' > "$d/rt.status"; echo same > "$d/object"; : > "$d/orig.err"; : > "$d/low.err"
  echo 0 > "$d/orig.status"; echo 0 > "$d/low.status"; echo a > "$d/orig.out"; echo b > "$d/low.out"
done
echo $'inline-asm\tinline assembly' > "$w/files/x/asm.cpp/lower.refused"; rm "$w/files/x/asm.cpp/gcc.status" "$w/files/x/asm.cpp/low.status"
arbace_tool -m arbace.cpp.check judge "$w" "$lk" "$root/test/cpp/lower-results.edn" > "$t/lc.log" 2>&1; st=$?
[ $st = 1 ] && ok "the lowering check's judge fails (exit 1)" || bad "the lowering check's judge: exit $st"
[ "$(v "$w" x/asm.cpp)" = known ] && ok "refused inline-asm: known" || bad "asm.cpp: $(v "$w" x/asm.cpp)"
[ "$(v "$w" x/run.cpp)" = failing ] && ok "known as inline-asm but failing the run: failing" || bad "run.cpp: $(v "$w" x/run.cpp)"
[ "$(v "$w" x/cl.cpp)" = known ] && ok "uncaught-cleanups at the run: known" || bad "cl.cpp: $(v "$w" x/cl.cpp)"
finish
