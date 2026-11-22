#!/bin/bash
# pyi's AOT module rebuilt one at a time (queue 481, 475-3 (a); bin/pyi's aot_module, its lock WORK/aot/mod.lock): two
# pyi runs of one checkout that both find the module's key stale. Rebuilds the checkout's AOT module once (a few
# minutes: the runtime printed, the C libraries compiled by cf2g) and compiles a two-line program twice.
#   1. the negative control: while the lock is held, a run told to take none (PYI_AOT_MODULE_LOCK=0) starts rebuilding;
#      the check "no run rebuilds while the lock is held" must fail on it (the run is then killed)
#   2. while the lock is held, two runs wait (neither rebuilds); released, they run at once: exactly one rebuilds, the
#      other keeps its build, both compile and their programs print 45
#   0. first, the pinned CPython tarball's extraction, which both runs need (bin/repr-python's extract_into): removed,
#      then extracted by two `bin/repr-python pins --fetch` at once, the tree whole (every file of the tarball and its
#      .extracted), and the site-packages apks' likewise. PYI_AOT_LOCK_TEST=extract runs this case alone (a minute, no rebuild).
# By hand or `make pyi-aot-lock-test`: wired to no gate step.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
AOT=$root/.tmp/repr-python/pyi/aot
w=$root/.tmp/pyi-aot-lock-test; rm -rf "$w"; mkdir -p "$w" "$AOT/mod"
printf 'n = sum(range(10))\nprint(n)\n' > "$w/prog.py"
fail() { echo "aot-lock-test: FAIL: $*" >&2; exit 1; }
# each run in a session of its own (its process group killed by its PID)
run() { local nm=$1; shift; setsid env "$@" "$root/bin/pyi" aot-compile "p_lock_$nm" "$w/prog.py" "$w/$nm.exe" > "$w/$nm.log" 2>&1 & }
rebuilding() { cat "$w/$1.log" 2>/dev/null | grep -c '^pyi: rebuilding the AOT module' || true; }
# the lock held by hand (flock's child holds it too: the holder's session killed as one)
holder() { setsid flock "$AOT/mod.lock" sh -c "echo held > '$w/held'; sleep $1" & }
wait_held() { local i; for i in $(seq 50); do [ -f "$w/held" ] && return 0; sleep 0.2; done; fail "the lock was not taken"; }

# 0. the tarball's extraction
top=$root/.tmp/repr-python/Python-3.14.10
tb=$root/.tmp/repr-python/src/Python-3.14.10.tar.xz
sp=$root/.tmp/repr-python/site-packages
rm -rf "$top" "$sp"
( env -u REPR_PYTHON_VERSION "$root/bin/repr-python" pins --fetch > "$w/x1.log" 2>&1 ) & x1=$!
( env -u REPR_PYTHON_VERSION "$root/bin/repr-python" pins --fetch > "$w/x2.log" 2>&1 ) & x2=$!
r1=0; wait $x1 || r1=$?; r2=0; wait $x2 || r2=$?
[ $r1 = 0 ] && [ $r2 = 0 ] || { tail -n 3 "$w/x1.log" "$w/x2.log" >&2; fail "an extraction failed ($r1, $r2)"; }
[ -f "$top/.extracted" ] || fail "no $top/.extracted"
for d in "$sp"/*/; do [ -f "$d.extracted" ] && [ -d "$d/usr" ] || fail "site-packages $d not whole"; done
if [ -f "$tb" ]; then
  want=$(tar -tJf "$tb" | grep -vc '/$'); have=$(cd "$top" && find . ! -type d ! -name .extracted | wc -l)
  [ "$have" = "$want" ] || fail "the tree holds $have files, the tarball $want"
  echo "aot-lock-test: two extractions at once: the tree whole ($have files)"
else
  echo "aot-lock-test: two extractions at once: the tree marked whole (no tarball at $tb to count)"
fi
[ "${PYI_AOT_LOCK_TEST:-}" != extract ] || exit 0

# 1. the negative control
rm -f "$AOT/mod/runtime.key" "$w/held"
holder 120; h=$!; wait_held
run ctl PYI_AOT_MODULE_LOCK=0; c=$!
for i in $(seq 120); do [ "$(rebuilding ctl)" = 0 ] || break; sleep 1; done
kill -- -"$c" 2>/dev/null || true; wait "$c" 2>/dev/null || true
kill -- -"$h" 2>/dev/null || true; wait "$h" 2>/dev/null || true
if [ "$(rebuilding ctl)" = 0 ]; then
  fail "control not judged: the run without the lock did not start rebuilding while the lock was held"
fi
echo "aot-lock-test: control failed as it must: a run taking no lock rebuilt while the lock was held"

# 2. two runs
rm -f "$AOT/mod/runtime.key" "$w/held"
holder 15; h=$!; wait_held
run a; a=$!
sleep 2
run b; b=$!
sleep 8
[ "$(rebuilding a)$(rebuilding b)" = 00 ] || fail "a run rebuilt while the lock was held"
echo "aot-lock-test: both runs waited for the held lock"
wait "$h" || true
ra=0; wait "$a" || ra=$?
rb=0; wait "$b" || rb=$?
[ $ra = 0 ] || { tail -5 "$w/a.log" >&2; fail "run a exited $ra"; }
[ $rb = 0 ] || { tail -5 "$w/b.log" >&2; fail "run b exited $rb"; }
n=$(( $(rebuilding a) + $(rebuilding b) ))
[ $n = 1 ] || fail "$n rebuilds, not 1"
echo "aot-lock-test: two concurrent runs: one rebuilt the module, the other kept it"
for nm in a b; do
  [ ! -f "$w/$nm.exe.refused" ] || fail "run $nm's program refused: $(cat "$w/$nm.exe.refused")"
  out=$(cd "$w" && PYI_AOT_COMPILER="$AOT/compiler.sh" "$w/$nm.exe" "$w/prog.py" 2>&1) || fail "run $nm's program: $out"
  [ "$out" = 45 ] || fail "run $nm's program printed '$out'"
done
echo "aot-lock-test: both programs printed 45: ok"
