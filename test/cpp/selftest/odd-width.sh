# needs-host
# Review 007's finding 4: integers of odd widths were loaded and stored at their container's width, unmasked: a 24-bit
# bit-field's store clobbered the next field. test/cpp/samples/bitfields.cpp (i24, i40, i48, i56 and a masked i17) lowered,
# run as the original (it needs no library: a minimal runtime).
. "$(dirname "$0")/lib.bash"
# ORS27: the lowered run is gcc's (CPPI_GCC=1; in toolchain.txt: gcc, the oracle tier's); bitfields.cpp's run on ci is cpp-check-essential's
export CPPI_GCC=1
fake_runtime "$t/rt" || die "fake runtime"
printf 'samples/bitfields.cpp\tc++23\t\t%s\n' "$root/test/cpp/samples/bitfields.cpp" > "$t/list.tsv"
"$root/bin/cppi" lower-check "$t/list.tsv" "$t/w" --runtime "$t/rt" > "$t/w.log" 2>&1; st=$?
[ $st = 0 ] && grep -q $'^samples/bitfields.cpp\tpass\t' "$t/w/results.tsv" && ok "lower-check passes" || bad "lower-check exits $st: $(sed -n 2p "$t/w/results.tsv" 2>&1)"
d=$t/w/files/samples/bitfields.cpp
cmp -s "$d/orig.out" "$d/low.out" && ok "the lowered run prints the original's" || bad "the lowered run prints: $(tr '\n' '|' < "$d/low.out")"
grep -q 'load i24' "$d/x.ll" && grep -q 'store i40' "$d/x.ll" && ok "the IR has load i24 and store i40" || bad "the IR has no odd-width access"
finish
