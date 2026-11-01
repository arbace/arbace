#!/bin/bash
# Tests of bin/alpine-env's offline verbs over fabricated apk databases (ALPINE_ENV_DB, ALPINE_ENV_WORLD;
# GT7's audit 065, finding 3): a dependency with several installed providers. No apk, no network.
# Run: bash test/alpine-env/test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
t=${TMPDIR:-$root/.tmp/tmp}/alpine-env-test.$$
rm -rf "$t"; mkdir -p "$t"; trap 'rm -rf "$t"' EXIT
fails=0
ok()  { echo "ok   $1"; }
bad() { echo "FAIL $1"; fails=$((fails + 1)); }
# run VERB WORLD DB: prints the output, sets rc
run() { out=$(ALPINE_ENV_DB=$3 ALPINE_ENV_WORLD=$2 bash "$root/bin/alpine-env" "$1" 2>&1); rc=$?; }

# T2: A needs cmd:awk, which busybox and gawk both provide
printf 'P:busybox\nV:1-r0\np:cmd:awk=1-r0\n\nP:gawk\nV:5-r0\np:cmd:awk=5-r0\n\nP:A\nV:1-r0\nD:cmd:awk\n\n' > "$t/t2.db"
echo 'A busybox gawk' > "$t/t2a.world"
run world "$t/t2a.world" "$t/t2.db"
[ "$out" = "A busybox gawk" ] && ok "world keeps both providers the line names" || bad "world keeps both providers (got: $out)"
run check "$t/t2a.world" "$t/t2.db"; [ $rc = 0 ] && ok "check passes the line naming both providers" || bad "check passes the line naming both providers (rc $rc: $out)"
echo 'A' > "$t/t2b.world"
run check "$t/t2b.world" "$t/t2.db"
[ $rc != 0 ] && ok "check fails a line that misses the providers" || bad "check fails a line that misses the providers (rc $rc: $out)"
echo "$out" | grep -q 'cmd:awk has several providers' && ok "check names the dependency" || bad "check names the dependency (got: $out)"
echo 'A gawk' > "$t/t2c.world"
run check "$t/t2c.world" "$t/t2.db"; [ $rc != 0 ] && ok "check fails when only one of two installed providers is named" || bad "one of two named (rc $rc: $out)"

# one installed provider: an ordinary dependency, still implied
printf 'P:gawk\nV:5-r0\np:cmd:awk=5-r0\n\nP:A\nV:1-r0\nD:cmd:awk\n\n' > "$t/s.db"
echo 'A gawk' > "$t/s.world"
run world "$t/s.world" "$t/s.db"; [ "$out" = A ] && ok "a single provider is implied" || bad "a single provider is implied (got: $out)"
run check "$t/s.world" "$t/s.db"; [ $rc = 0 ] && ok "check passes it" || bad "check passes it (rc $rc: $out)"

# T1: two names of one package
printf 'P:procps-ng\nV:1-r0\np:procps=1-r0\n\n' > "$t/t1.db"; echo 'procps procps-ng' > "$t/t1.world"
run check "$t/t1.world" "$t/t1.db"; [ $rc != 0 ] && ok "T1: two names for one package still fail check" || bad "T1 (rc $rc: $out)"

[ $fails = 0 ] && { echo "all passed"; exit 0; } || { echo "$fails failed"; exit 1; }
