# Review 018's R3 (a): review 007's fix changed the statuses cxxdump caches (a clang that is killed or crashes is no longer
# cached as "rejected"), but the cache key's version stayed, so an entry made before was still served as a rejection. The key
# now has a version newer than the fix's (cxxdump-8 or later), and a rejection's cache entry is still served as one.
. "$(dirname "$0")/lib.bash"
"$root/bin/repr-cpp" cxxdump-build > "$t/build.log" 2>&1 || die "cxxdump-build"
exe=$root/.tmp/repr-cpp/cxxdump
v=$(grep -a -o 'cxxdump-[0-9][0-9]*' "$exe" | sort -u | tr '\n' ' ')
n=$(grep -o 'version = "cxxdump-[0-9]*"' "$root/tools/cxxdump/run.go" | grep -o '[0-9]*')
[ "${n:-0}" -ge 8 ] && ok "run.go's cache version is cxxdump-$n" || bad "the cache version is cxxdump-$n (before the fix: 7)"
case " $v" in *" cxxdump-$n "*) ok "the built executable carries it ($v)" ;; *) bad "the executable's versions: $v" ;; esac
src=$root/test/cpp/selftest/min.cpp
printf '#!/bin/sh\necho "min.cpp:1:1: error: unknown type name" >&2\nexit 1\n' > "$t/rejects"; chmod +x "$t/rejects"
"$exe" -clang "$t/rejects" -cache "$t/cache" -o "$t/o1" -- -std=c++23 "$src" 2> "$t/e1"; a=$?
"$exe" -clang "$t/rejects" -cache "$t/cache" -o "$t/o2" -- -std=c++23 "$src" 2> "$t/e2"; b=$?
[ "$a $b" = "1 1" ] && grep -q 'clang rejected the unit' "$t/e2" && ok "a rejection is cached and served as one" || bad "rejection twice: $a $b, $(tail -1 "$t/e2")"
finish
