# needs-host
# Review 007's finding 5: level files of an earlier run survived in a reused WORK, so a print that stopped compiling kept
# the earlier "same" object, and a refusal once made stayed. Each file's directory is emptied first now.
. "$(dirname "$0")/lib.bash"
fake_runtime "$t/rt" || die "fake runtime"
min_list "$t/list.tsv"
w=$t/w
"$root/bin/cppi" lower-check "$t/list.tsv" "$w" --runtime "$t/rt" > "$t/1.log" 2>&1 || die "the first run fails: $(tail -3 "$t/1.log")"
grep -q $'^min.cpp\tpass\t' "$w/results.tsv" && ok "the first run passes" || die "the first run: $(sed -n 2p "$w/results.tsv")"
# a refusal of an earlier run stands in the directory: the file lowers now
echo $'refused\tan earlier run' > "$w/files/min.cpp/lower.refused"
"$root/bin/cppi" lower-check "$t/list.tsv" "$w" --runtime "$t/rt" > "$t/2.log" 2>&1; st=$?
[ $st = 0 ] && grep -q $'^min.cpp\tpass\t' "$w/results.tsv" && ok "an earlier refusal does not stand" || bad "with an earlier refusal: exit $st, $(sed -n 2p "$w/results.tsv")"
# the print's object stops compiling (clang -c print.ll fails): the earlier run's "same" must not stand
w=$t/w2
"$root/bin/cppi" lower-check "$t/list.tsv" "$w" --runtime "$t/rt" > "$t/3a.log" 2>&1 || die "the first run in w2 fails"
mkdir -p "$t/bin"
printf '#!/bin/sh\nfor a in "$@"; do [ "$a" = print.ll ] && { echo "print.ll:1:1: error: a broken print" >&2; exit 1; }; done\nexec /usr/bin/clang "$@"\n' > "$t/bin/clang"; chmod +x "$t/bin/clang"
PATH=$t/bin:$PATH "$root/bin/cppi" lower-check "$t/list.tsv" "$w" --runtime "$t/rt" > "$t/3.log" 2>&1; st=$?
[ $st = 1 ] && grep -q $'^min.cpp\tfailing\tobject\t' "$w/results.tsv" && ok "a print that no longer compiles fails at the object" || bad "with a broken print: exit $st, $(sed -n 2p "$w/results.tsv")"
finish
