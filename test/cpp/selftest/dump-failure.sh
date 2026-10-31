# Review 007's finding 3: repr/cpp's round trip counted every cxxdump failure, its own included, as the oracle's rejection.
# Now cxxdump exits 1 only when clang rejects the unit (3 for its own failures and a clang that is killed or crashes, never
# cached), and the round trip fails on a dump that failed for another reason.
. "$(dirname "$0")/lib.bash"
"$root/bin/repr-cpp" cxxdump-build > "$t/build.log" 2>&1 || die "cxxdump-build"
exe=$root/.tmp/repr-cpp/cxxdump
src=$root/test/cpp/selftest/min.cpp
mk() { printf '#!/bin/sh\n%s\n' "$2" > "$t/$1"; chmod +x "$t/$1"; }
mk garbage 'echo "not json"; exit 0'
mk killed 'for a in "$@"; do case $prev in -MF) : > "$a" ;; esac; prev=$a; done; kill -9 $$'
mk rejects 'echo "min.cpp:1:1: error: unknown type name" >&2; exit 1'
"$exe" -clang "$t/garbage" -o "$t/o1" -- -std=c++23 "$src" 2> "$t/e1"; st=$?
[ $st = 3 ] && ok "a filter error exits 3" || bad "a filter error exits $st"
"$exe" -clang "$t/killed" -o "$t/o2" -- -std=c++23 "$src" 2> "$t/e2"; st=$?
[ $st = 3 ] && ok "a killed clang exits 3" || bad "a killed clang exits $st"
"$exe" -clang "$t/rejects" -o "$t/o3" -- -std=c++23 "$src" 2> "$t/e3"; st=$?
[ $st = 1 ] && ok "clang's rejection exits 1" || bad "clang's rejection exits $st"
"$exe" -clang "$t/killed" -cache "$t/cache" -o "$t/o4" -- -std=c++23 "$src" 2> "$t/e4"; st=$?
[ $st = 3 ] && ok "a killed clang exits 3 through the cache" || bad "a killed clang through the cache exits $st"
n=$(find "$t/cache" -name '*.status' | wc -l)
[ "$n" = 0 ] && ok "and is not cached" || bad "a killed clang's run is cached ($n entries)"
# the round trip on min.cpp with clang++ killed on every run (PATH): before, oracle-rejects and exit 0
mkdir -p "$t/bin"; cp "$t/killed" "$t/bin/clang++"
printf 'min.cpp\tc++23\t-std=c++23\t%s\t-std=c++23\n' "$src" > "$t/list.tsv"
PATH=$t/bin:$PATH REPR_CPP_CXXDUMP_CACHE=$t/rtcache "$root/bin/repr-cpp" roundtrip "$t/list.tsv" "$t/w" --no-objects > "$t/rt.log" 2>&1; st=$?
[ $st = 1 ] && ok "the round trip fails (exit 1)" || bad "the round trip exits $st: $(tail -1 "$t/rt.log")"
grep -q $'^min.cpp\tfail\t-\tdump-error-3' "$t/w/results.tsv" && ok "verdict fail, read dump-error-3" || bad "results: $(cat "$t/w/results.tsv")"
finish
