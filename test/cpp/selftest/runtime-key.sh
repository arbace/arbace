# Review 007's finding 1: lower-check used whatever runtime OUT held. In a copy of the tree: a runtime whose key is the tree's
# is current; a change of arbace/cpp/rt/eh.c.ae makes it stale (the input named); lower-check refuses a stale runtime
# (CPPI_RUNTIME_STALE=refuse) before any native step.
. "$(dirname "$0")/lib.bash"
mkdir -p "$t/tree"
cp -r "$root/bin" "$root/arbace" "$t/tree/"; mkdir -p "$t/tree/test"; cp -r "$root/test/cpp" "$t/tree/test/"
rt=$t/rt; mkdir -p "$rt"
ar rcs "$rt/liblstdc++.a"; : > "$rt/rt.o"
"$t/tree/bin/cppi" runtime-key --print > "$rt/key.txt" 2> "$t/print.err" || bad "runtime-key --print"
sha256sum < "$rt/key.txt" | cut -d' ' -f1 > "$rt/key"
s=$("$t/tree/bin/cppi" runtime-key "$rt" 2>&1); [ "$s" = current ] && ok "a runtime at the tree's key is current" || bad "at the tree's key: $s"
echo ';; a change' >> "$t/tree/arbace/cpp/rt/eh.c.ae"
s=$("$t/tree/bin/cppi" runtime-key "$rt" 2>&1); st=$?
[ $st = 1 ] && grep -q 'differs: arbace/cpp/rt/eh.c.ae' <<< "$s" && ok "eh.c.ae changed: stale, the input named" || bad "eh.c.ae changed: status $st: $s"
printf 'min.cpp\tc++23\t\t%s\n' "$root/test/cpp/selftest/min.cpp" > "$t/list.tsv"
CPPI_RUNTIME_STALE=refuse "$t/tree/bin/cppi" lower-check "$t/list.tsv" "$t/w" --runtime "$rt" > "$t/lc.log" 2>&1; st=$?
[ $st = 4 ] && ok "lower-check refuses the stale runtime (exit 4)" || bad "lower-check on a stale runtime: exit $st"
[ ! -d "$t/w/files" ] && ok "no native step ran" || bad "lower-check ran with the stale runtime"
rm -rf "$t/tree"
finish
