# CP101 (c), review 007's finding 14: runtime-build checks the configured headers against their pinned digests before it
# compiles anything: a pin with one digest changed stops it (exit 1, the header named) before the IR.
. "$(dirname "$0")/lib.bash"
pin=$t/headers.sha256
# the first digest's first hex digit changed to another one (review 018's R9: not a sed that fails when the digit is 0)
d1=$(head -1 "$root/test/cpp/host/headers.sha256" | cut -c1); n1=$([ "$d1" = a ] && echo b || echo a)
sed "1s/^./$n1/" "$root/test/cpp/host/headers.sha256" > "$pin"
cmp -s "$pin" "$root/test/cpp/host/headers.sha256" && die "the doctored pin equals the real one"
CPPI_HEADERS_PIN=$pin timeout 60 "$root/bin/cppi" runtime-build "$t/rt" --jobs 1 > "$t/rb.log" 2>&1; st=$?
[ $st = 1 ] && ok "runtime-build stops (exit 1)" || bad "runtime-build with a changed header digest: exit $st"
h=$(head -1 "$root/test/cpp/host/headers.sha256" | sed 's/^[0-9a-f]*  //')
grep -q "$h" "$t/rb.log" && ok "the header is named ($h)" || bad "no header named"
[ ! -d "$t/rt/units" ] && ok "no IR made" || bad "the IR was being made"
rm -rf "$t/rt/gen"
finish
