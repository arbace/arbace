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
# review 018's R5: the key holds the musl headers, the pin that was used, and is taken at the build's start
grep -q 'musl-dev package' "$rt/key.txt" && grep -q '/usr/include/\*.h' "$rt/key.txt" && ok "the key covers the musl headers" || bad "no musl line in the key"
cp "$root/test/cpp/host/headers.sha256" "$t/pin2"; echo "0000000000000000000000000000000000000000000000000000000000000000  /nonexistent/header.h" >> "$t/pin2"
pk() { grep 'headers pin' <<< "$1" | cut -d' ' -f1; }
a=$("$t/tree/bin/cppi" runtime-key --print); b=$(CPPI_HEADERS_PIN=$t/pin2 "$t/tree/bin/cppi" runtime-key --print)
[ "$(pk "$a")" != "$(pk "$b")" ] && ok "the key records the pin CPPI_HEADERS_PIN names" || bad "the key ignores CPPI_HEADERS_PIN"
# runtime-build stops at the doctored pin; the key it took at its start stands in OUT/key.start, no key.txt
git_ok=1; rb=$t/rb
CPPI_HEADERS_PIN=$t/pin2 "$t/tree/bin/cppi" runtime-build "$rb" > "$t/rb.log" 2>&1; st=$?
[ $st != 0 ] && [ ! -f "$rb/key.txt" ] && [ -f "$rb/key.start" ] && ok "runtime-build took its key at the start (key.start), key.txt only at the end" || bad "runtime-build: exit $st, files: $(ls "$rb" 2>&1 | tr '\n' ' ')"
[ "$(pk "$(cat "$rb/key.start")")" = "$(pk "$b")" ] && ok "key.start is the key of the pin used" || bad "key.start differs from the key at the start"
rm -rf "$t/tree"
finish
