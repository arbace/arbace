#!/bin/bash
# bin/lib/frozen.bash's frozen_jar and bin/j2g-edge's work directory on fabricated trees (audit 041,
# finding 3): no JVM, no jar of the build. By hand: test/gate/frozen-test.bash [SCRATCH]
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
scratch=${1:-$src/.tmp/frozen-test}
rm -rf "$scratch"; mkdir -p "$scratch"
export TMPDIR=$scratch/tmp; mkdir -p "$TMPDIR"
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }

root=$scratch/root
mkdir -p "$root/test/oracle" "$root/bin/lib" "$root/jdk" "$root/.tmp/oracle-frozen"
echo "good jar" > "$scratch/good.jar"
pin=$(sha256sum "$scratch/good.jar" | cut -d' ' -f1)
echo "$pin  good.jar" > "$root/test/oracle/frozen-jar.sha256"
echo "other jar" > "$scratch/bogus.jar"
cp "$scratch/good.jar" "$root/.tmp/oracle-frozen/my-tag.jar"   # a built jar of a tag with no pin line

run() { # run NAME WANT_STATUS VAR=VALUE...: frozen_jar with the variables, its status
  local name=$1 want=$2; shift 2
  local out st
  out=$(root=$root env "$@" bash -c '. "$0/bin/lib/frozen.bash"; frozen_jar' "$src" 2>&1); st=$?
  if [ "$st" = "$want" ]; then ok "$name (status $st)"; else bad "$name: status $st, wanted $want: $out"; fi
}

run pinned-jar 0 FROZEN_JAR="$scratch/good.jar"
run bogus-jar-fails 1 FROZEN_JAR="$scratch/bogus.jar"
run bogus-oracle-jar-fails 1 ARBACE_ORACLE_JAR="$scratch/bogus.jar"
run unpinned-tag-fails 1 FROZEN_TAG=my-tag
run unpinned-tag-allowed 0 FROZEN_TAG=my-tag FROZEN_ALLOW_UNPINNED=1
run unpinned-refused-in-gate 1 FROZEN_TAG=my-tag FROZEN_ALLOW_UNPINNED=1 GATE_EXE=/x
cp "$scratch/good.jar" "$scratch/unpinned.jar"
run unpinned-name-fails 1 FROZEN_JAR="$scratch/unpinned.jar"

# bin/j2g-edge refuses a work directory of tracked files, before any JVM
t=$scratch/edge; mkdir -p "$t/bin/lib" "$t/test/oracle" "$t/jdk"
cp "$src/bin/j2g-edge" "$t/bin/"; cp "$src/bin/lib/tmpdir.bash" "$src/bin/lib/renamed-env.bash" "$t/bin/lib/"
printf 'frozen_jar() { echo fake-jar; }\nfrozen_java() { echo "frozen_java $*" >> "$FAKE_LOG"; return 1; }\n' > "$t/bin/lib/frozen.bash"
echo edge > "$t/jdk/edge.edn"
(cd "$t" && git init -q . && git add -A && git -c user.name=t -c user.email=t@t commit -q -m t) || bad "git init"
for w in jdk "$t/jdk" "$t/jdk/sub/.." "$t" .; do
  out=$(cd "$t" && J2G_EDGE_WORK=$w FAKE_LOG=$scratch/fake.log bin/j2g-edge verify 2>&1); st=$?
  if [ "$st" = 2 ] && [ "$(cat "$t/jdk/edge.edn")" = edge ]; then ok "work $w refused"; else bad "work $w: status $st: $out"; fi
done
# report 046, finding 1: the checkout reached through a symlinked path, and an untracked directory in a tracked tree
ln -s "$t" "$scratch/link"
for w in jdk jdk/newdir; do
  out=$(cd "$scratch/link" && J2G_EDGE_WORK=$w FAKE_LOG=$scratch/fake.log "$scratch/link/bin/j2g-edge" verify 2>&1); st=$?
  if [ "$st" = 2 ] && [ "$(cat "$t/jdk/edge.edn" 2>&1)" = edge ] && [ ! -e "$t/jdk/newdir" ]; then ok "work $w refused through a symlinked checkout"
  else bad "symlinked checkout, work $w: status $st: $out"; fi
done
out=$(cd "$t" && J2G_EDGE_WORK=.tmp/w FAKE_LOG=$scratch/fake.log bin/j2g-edge verify 2>&1); st=$?
if [ "$st" = 1 ] && ! echo "$out" | grep -q 'tracked'; then ok "work under .tmp accepted"; else bad "work .tmp/w: status $st: $out"; fi

[ "$fails" = 0 ] && echo "frozen-test: passed" || echo "frozen-test: $fails failed"
exit $((fails > 0))
