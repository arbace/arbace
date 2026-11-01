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
run() { out=$(ALPINE_ENV_INDEX=${IDX:-$t/empty.idx} ALPINE_ENV_DB=$3 ALPINE_ENV_WORLD=$2 bash "$root/bin/alpine-env" "$1" 2>&1); rc=$?; }

# no repository index unless a case gives one (IDX)
: > "$t/empty.idx"

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
run check "$t/t2c.world" "$t/t2.db"; [ $rc != 0 ] && echo "$out" | grep -q "not reached: busybox" && ok "an installed provider nothing needs is not reached (apk would be content, the line must not hide it)" || bad "unneeded provider (rc $rc: $out)"
# the positive case: busybox needed by an exact-name dependency, so A gawk is complete
printf 'P:busybox\nV:1-r0\np:cmd:awk=1-r0\n\nP:gawk\nV:5-r0\np:cmd:awk=5-r0\n\nP:A\nV:1-r0\nD:cmd:awk busybox\n\n' > "$t/t2d.db"
run check "$t/t2c.world" "$t/t2d.db"; [ $rc = 0 ] && ok "A gawk is complete when busybox is needed by name" || bad "positive case (rc $rc: $out)"

# one installed provider: an ordinary dependency, still implied
printf 'P:gawk\nV:5-r0\np:cmd:awk=5-r0\n\nP:A\nV:1-r0\nD:cmd:awk\n\n' > "$t/s.db"
echo 'A gawk' > "$t/s.world"
run world "$t/s.world" "$t/s.db"; [ "$out" = A ] && ok "a single provider is implied" || bad "a single provider is implied (got: $out)"
run check "$t/s.world" "$t/s.db"; [ $rc = 0 ] && ok "check passes it" || bad "check passes it (rc $rc: $out)"

# T1: two names of one package
printf 'P:procps-ng\nV:1-r0\np:procps=1-r0\n\n' > "$t/t1.db"; echo 'procps procps-ng' > "$t/t1.world"
run check "$t/t1.world" "$t/t1.db"; [ $rc != 0 ] && ok "T1: two names for one package still fail check" || bad "T1 (rc $rc: $out)"

# java-jdk: one JDK installed, the repositories offer several (audit 065 review 068 A)
printf 'P:clojure\nV:1-r0\nD:java-jdk\n\nP:openjdk25-jdk\nV:25-r0\np:java-jdk=25\n\n' > "$t/j.db"
printf 'P:openjdk21-jdk\nV:21-r0\np:java-jdk=21\n\nP:openjdk25-jdk\nV:25-r0\np:java-jdk=25\n\nP:clojure\nV:1-r0\nD:java-jdk\n\n' > "$t/j.idx"
echo 'clojure openjdk25-jdk' > "$t/j.world"
IDX=$t/j.idx run world "$t/j.world" "$t/j.db"; [ "$out" = "clojure openjdk25-jdk" ] && ok "world keeps the one installed JDK when others are offered" || bad "world keeps the JDK (got: $out)"
IDX=$t/j.idx run check "$t/j.world" "$t/j.db"; [ $rc = 0 ] && ok "check passes the line naming the JDK" || bad "check, JDK named (rc $rc: $out)"
echo 'clojure' > "$t/j2.world"
IDX=$t/j.idx run check "$t/j2.world" "$t/j.db"; [ $rc != 0 ] && echo "$out" | grep -q 'java-jdk has several providers' && ok "check fails a line that leaves java-jdk to apk" || bad "check, JDK unnamed (rc $rc: $out)"
run check "$t/j2.world" "$t/j.db"; [ $rc = 0 ] && ok "without an index the single installed JDK is an edge (offline fallback)" || bad "no index (rc $rc: $out)"
# an exact-name package wins (coreutils, also offered by uutils-coreutils)
printf 'P:coreutils\nV:1-r0\n\nP:A\nV:1-r0\nD:coreutils\n\n' > "$t/c.db"
printf 'P:coreutils\nV:1-r0\n\nP:uutils-coreutils\nV:1-r0\np:coreutils=1\n\n' > "$t/c.idx"
echo 'A coreutils' > "$t/c.world"
IDX=$t/c.idx run world "$t/c.world" "$t/c.db"; [ "$out" = A ] && ok "an exact-name package is the edge" || bad "exact name (got: $out)"
# so: names count installed providers only
printf 'P:lib1\nV:1-r0\np:so:libx.so.1=1\n\nP:A\nV:1-r0\nD:so:libx.so.1\n\n' > "$t/l.db"
printf 'P:lib1\nV:1-r0\np:so:libx.so.1=1\n\nP:lib2\nV:2-r0\np:so:libx.so.1=2\n\n' > "$t/l.idx"
echo 'A lib1' > "$t/l.world"
IDX=$t/l.idx run world "$t/l.world" "$t/l.db"; [ "$out" = A ] && ok "a library offered in two packages is still an edge" || bad "so: name (got: $out)"

[ $fails = 0 ] && { echo "all passed"; exit 0; } || { echo "$fails failed"; exit 1; }
