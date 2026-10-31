# Review 018's R1: the round trip's -norm dump (level 3) kept no status and a reused WORK kept the earlier run's level files, so a
# cxxdump that fails in norm mode left the earlier run's .norm files equal and level 3 passed; a print's dump failing (status 3)
# read only as level 1 or 3 false, so a file known as print-rejected stayed known. Now the statuses are kept, a dump's own failure
# fails the file (known or not), and a reused WORK is emptied first.
. "$(dirname "$0")/lib.bash"
"$root/bin/repr-cpp" cxxdump-build > "$t/build.log" 2>&1 || die "cxxdump-build"
real=$root/.tmp/repr-cpp/cxxdump
src=$root/test/cpp/selftest/min.cpp
printf 'min.cpp\tc++23\t-std=c++23\t%s\t-std=c++23\n' "$src" > "$t/list.tsv"
# a cxxdump that fails (exit 3, its own failure) in the mode MODE: norm (-norm), print (run in the print root), else not
mk() { printf '#!/bin/sh\ncase " $* " in *" -norm "*) [ "%s" = norm ] && exit 3 ;; esac\ncase "$(pwd)" in */print) [ "%s" = print ] && exit 3 ;; esac\nexec %s "$@"\n' "$1" "$1" "$real" > "$t/cx-$1"; chmod +x "$t/cx-$1"; }
mk none; mk norm; mk print
rt() { REPR_CPP_CXXDUMP_EXE=$t/cx-$1 REPR_CPP_CXXDUMP_CACHE=$t/cache REPR_CPP_ROUNDTRIP_KNOWN=${3:-$t/known.edn} "$root/bin/repr-cpp" roundtrip "$t/list.tsv" "$2" --no-objects > "$t/rt-$1.log" 2>&1; }
verdict() { awk -F'\t' '$1=="min.cpp" {print $2 " " $4}' "$1/results.tsv"; }
echo '{:files {}}' > "$t/known.edn"
w=$t/w
rt none "$w"; st=$?
[ $st = 0 ] && [ "$(verdict "$w")" = "pass ok" ] && ok "the first run passes" || die "the first run: exit $st, $(verdict "$w")"
# the same WORK, cxxdump failing in norm mode: the earlier .norm files must not stand
rt norm "$w"; st=$?
[ $st = 1 ] && ok "a failing -norm dump fails the round trip in a reused WORK" || bad "norm failure in a reused WORK: exit $st, $(verdict "$w")"
case "$(verdict "$w")" in "fail dump-error-norm-3") ok "verdict fail, dump-error-norm-3" ;; *) bad "verdict: $(verdict "$w")" ;; esac
# a fresh WORK, the file known as ast-differs: a dump's own failure is not that
echo '{:files {"min.cpp" :ast-differs}}' > "$t/known-ast.edn"
rt norm "$t/w2" "$t/known-ast.edn"; st=$?
[ $st = 1 ] && [ "$(verdict "$t/w2")" = "fail dump-error-norm-3" ] && ok "a known ast-differs file fails when the -norm dump failed" || bad "known ast-differs: exit $st, $(verdict "$t/w2")"
# the print's dump failing, the file known as print-rejected
echo '{:files {"min.cpp" :print-rejected}}' > "$t/known-pr.edn"
rt print "$t/w3" "$t/known-pr.edn"; st=$?
[ $st = 1 ] && [ "$(verdict "$t/w3")" = "fail dump-error-print-3" ] && ok "a known print-rejected file fails when the print's dump failed" || bad "known print-rejected: exit $st, $(verdict "$t/w3")"
finish
