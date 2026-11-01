#!/bin/bash
# bin/j2g-edge's record on a fabricated tree and a fake frozen JVM (audit 041, finding 5): a j2g that
# exits 0 without writing must not have the previous run's files merged, and two records in one
# work directory exclude each other. No JVM. By hand: test/gate/j2g-edge-record-test.bash [SCRATCH]
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
scratch=${1:-$src/.tmp/j2g-edge-record-test}
rm -rf "$scratch"; mkdir -p "$scratch"
export TMPDIR=$scratch/tmp; mkdir -p "$TMPDIR"
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }

t=$scratch/root; mkdir -p "$t/bin/lib" "$t/jdk" "$scratch/fx"
cp "$src/bin/j2g-edge" "$t/bin/"; cp "$src/bin/lib/tmpdir.bash" "$src/bin/lib/renamed-env.bash" "$t/bin/lib/"
echo edge > "$t/jdk/edge.edn"
# the fake frozen JVM: j2g writes its --record-edge file unless FAKE_NOWRITE; the merge needs both inputs
cat > "$t/bin/lib/frozen.bash" <<'F'
frozen_jar() { echo fake-jar; }
frozen_java() {
  echo "frozen_java $*" >> "$FAKE_LOG"
  case " $* " in
    *" arbace.j2g.main "*)
      [ -n "${FAKE_NOWRITE:-}" ] && return 0
      while [ $# -gt 0 ]; do [ "$1" = --record-edge ] && echo edge > "$2"; shift; done ;;
    *" arbace.j2g.edge merge "*)
      while [ "$1" != merge ]; do shift; done; shift
      out=$1; shift
      for f in "$@"; do [ -s "$f" ] || { echo "merge: no $f" >&2; return 1; }; done
      cp "$1" "$out" ;;
  esac
}
F
w=$scratch/work; mkdir -p "$w"
run() { J2G_EDGE_WORK=$w J2G_FIXTURES=$scratch/fx FAKE_LOG=$scratch/fake.log "$@" "$t/bin/j2g-edge" verify > "$scratch/out" 2>&1; }

: > "$scratch/fake.log"
env J2G_EDGE_WORK=$w J2G_FIXTURES=$scratch/fx FAKE_LOG=$scratch/fake.log "$t/bin/j2g-edge" verify > "$scratch/out" 2>&1
[ $? = 0 ] && ok "a faithful record verifies" || { bad "faithful: $(cat "$scratch/out")"; }

# a previous run's files, then a j2g that writes nothing
echo edge > "$w/edge-program.edn"; echo edge > "$w/edge-tests.edn"
env FAKE_NOWRITE=1 J2G_EDGE_WORK=$w J2G_FIXTURES=$scratch/fx FAKE_LOG=$scratch/fake.log "$t/bin/j2g-edge" verify > "$scratch/out" 2>&1
st=$?
[ "$st" != 0 ] && ok "a j2g that writes nothing fails (status $st)" || bad "stale intermediates merged: $(cat "$scratch/out")"

# the lock: held by another process, a record gives up (here after 1 s) and runs no j2g
: > "$scratch/fake.log"
flock "$w/.lock" sleep 4 &
holder=$!
sleep 1
env J2G_EDGE_LOCK_WAIT=1 J2G_EDGE_WORK=$w J2G_FIXTURES=$scratch/fx FAKE_LOG=$scratch/fake.log "$t/bin/j2g-edge" verify > "$scratch/out" 2>&1
st=$?
wait $holder
if [ "$st" != 0 ] && [ ! -s "$scratch/fake.log" ] && grep -q locked "$scratch/out"; then ok "a locked work directory is waited for, not shared"
else bad "lock: status $st, log $(cat "$scratch/fake.log"), out $(cat "$scratch/out")"; fi

[ "$fails" = 0 ] && echo "j2g-edge-record-test: passed" || echo "j2g-edge-record-test: $fails failed"
exit $((fails > 0))
