#!/bin/bash
# GTS114 (a) (audit 041): bin/gate-tests runs every *-test.bash of a directory and fails when one does; the
# step gate-tests is in test/gate/steps.edn, essential, and the planner accepts the table (read by gate.plan
# on stage 0 over git's file list and a fabricated hosts.edn: its checks of rules matching no file, hosts, steps and scripts).
# Needs an Arbace executable: ARBACE_HOST_EXE=EXE test/gate/gate-tests-test.bash
# needs: exe (an Arbace executable: bin/gate-tests passes it as ARBACE_HOST_EXE)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
export ARBACE_HOST_EXE=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$ARBACE_HOST_EXE" ] || { echo "gate-tests-test: no executable $ARBACE_HOST_EXE" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
scratch=$TMPDIR/gate-tests-test.$$; rm -rf "$scratch"; mkdir -p "$scratch/pass" "$scratch/fail" "$scratch/none" "$scratch/logs" "$scratch/store"
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }

# the runner
printf '#!/bin/bash\nexit 0\n' > "$scratch/pass/a-test.bash"; chmod +x "$scratch/pass/a-test.bash"
cp "$scratch/pass/a-test.bash" "$scratch/fail/a-test.bash"
printf '#!/bin/bash\necho broken\nexit 1\n' > "$scratch/fail/b-test.bash"; chmod +x "$scratch/fail/b-test.bash"
"$src/bin/gate-tests" "$scratch/pass" > "$scratch/o1" 2>&1 && ok "passing tests pass" || bad "pass dir: $(cat "$scratch/o1")"
"$src/bin/gate-tests" "$scratch/fail" > "$scratch/o2" 2>&1 && bad "a failing test passed" || { grep -q "b-test.bash FAILED" "$scratch/o2" && ok "a failing test fails the run, named" || bad "failing: $(cat "$scratch/o2")"; }
"$src/bin/gate-tests" "$scratch/none" > "$scratch/o3" 2>&1 && bad "no test passed" || ok "an empty directory fails"

# report 055, finding 6: no test defaults its scratch directory to a fixed path (two runs at once would remove each other's)
fixed=$(grep -l '^scratch=.*-test[};]' "$src"/test/gate/*-test.bash)
[ -z "$fixed" ] && ok "the tests' default scratch directories are per process" || bad "fixed scratch default in: $fixed"
# review 419, I2: nor a scratch directory fixed in the checkout's .tmp (T=$here/.tmp/NAME-test), nor a TMPDIR forced over the one
# bin/gate-tests gives each test (two runs at once, or GATE_TESTS_JOBS, would share it): $TMPDIR/NAME-test, TMPDIR when unset
fixed=$(grep -lE '^(T|W|scratch)=\$(here|root|src)/\.tmp/[A-Za-z0-9_-]+($|;| )|^export TMPDIR=\$(here|root|src)/' "$src"/test/gate/*-test.bash)
[ -z "$fixed" ] && ok "no test's scratch is a fixed path of the checkout's .tmp, none forces TMPDIR" || bad "a fixed scratch path or a forced TMPDIR in: $fixed"
# report 055, finding 5: without an executable the tests that need none run, the others are named, status 1 (0: --no-exe)
r=$scratch/noexe-root; mkdir -p "$r/bin/lib" "$r/d"
cp "$src/bin/gate-tests" "$r/bin/"; cp "$src/bin/lib/tmpdir.bash" "$src/bin/lib/host.bash" "$r/bin/lib/"
printf '#!/bin/bash\nexit 0\n' > "$r/d/plain-test.bash"; printf '#!/bin/bash\n# needs: exe\nexit 1\n' > "$r/d/exe-test.bash"; chmod +x "$r/d/"*
out=$(cd "$r" && env -u ARBACE_HOST_EXE bin/gate-tests d 2>&1); st=$?
if [ "$st" = 1 ] && grep -q "exe-test.bash not run" <<< "$out" && grep -q "plain-test.bash passed" <<< "$out"; then ok "no stage 0: the tests that need it are named, status 1"
else bad "no exe: status $st: $out"; fi
out=$(cd "$r" && env -u ARBACE_HOST_EXE bin/gate-tests --no-exe d 2>&1); st=$?
[ "$st" = 0 ] && ok "--no-exe: the others alone, status 0" || bad "--no-exe: status $st: $out"

# report 059: a test that exits 77 is "not run", not passed, and does not fail the run
mkdir -p "$scratch/u77"; printf '#!/bin/bash\necho already traced\nexit 77\n' > "$scratch/u77/a-test.bash"; cp "$scratch/pass/a-test.bash" "$scratch/u77/b-test.bash"; chmod +x "$scratch/u77/"*
out=$(env -u GATE_AUDITED "$src/bin/gate-tests" "$scratch/u77" 2>&1); st=$?
if [ "$st" = 1 ] && grep -q "a-test.bash FAILED (not run, already traced, and no audit" <<< "$out"; then ok "exit 77 without the audit's marker fails"
else bad "exit 77 unaudited: status $st: $out"; fi
# the marker is set in both of the gate's audited runs (bin/lib/gate-run.bash)
[ "$(grep -c 'GATE_AUDITED=1' "$src/bin/lib/gate-run.bash")" -ge 2 ] && ok "gate-run.bash sets GATE_AUDITED in both audited runs" || bad "GATE_AUDITED missing in gate-run.bash"
# an inherited GATE_TESTS_IN_NESTED is dropped like GATE_TESTS_NESTED
mkdir -p "$scratch/inh"; printf '#!/bin/bash\n[ -z "${GATE_TESTS_IN_NESTED:-}" ]\n' > "$scratch/inh/a-test.bash"; chmod +x "$scratch/inh/a-test.bash"
out=$(GATE_TESTS_IN_NESTED=1 "$src/bin/gate-tests" "$scratch/inh" 2>&1) && ok "an inherited GATE_TESTS_IN_NESTED is dropped" || bad "inherited in-nested: $out"
out=$(GATE_AUDITED=1 "$src/bin/gate-tests" "$scratch/u77" 2>&1); st=$?
if [ "$st" = 0 ] && grep -q "a-test.bash not run (already traced)" <<< "$out" && ! grep -q "a-test.bash passed" <<< "$out" && grep -q "not run: a-test.bash" <<< "$out"; then ok "exit 77 is listed as not run, not passed"
else bad "exit 77: status $st: $out"; fi
# report 059: GATE_TESTS_NESTED in the caller's environment skips nothing; only --nested skips gate-reads-test
mkdir -p "$scratch/nest"; printf '#!/bin/bash\nexit 1\n' > "$scratch/nest/gate-reads-test.bash"; chmod +x "$scratch/nest/gate-reads-test.bash"
out=$(GATE_TESTS_NESTED=1 "$src/bin/gate-tests" "$scratch/nest" 2>&1); st=$?
[ "$st" = 1 ] && ok "an inherited GATE_TESTS_NESTED skips nothing" || bad "inherited nested: status $st: $out"
out=$("$src/bin/gate-tests" --nested "$scratch/nest" 2>&1); st=$?
[ "$st" = 0 ] && grep -q "gate-reads-test.bash not run (nested" <<< "$out" && ok "--nested skips gate-reads-test, loudly" || bad "--nested: status $st: $out"

# report 059: gate-reads-test must not pass without having looked. Cases with a fake strace (a mode: no trace
# file, an empty one, one without -f) and through a symlinked checkout; skipped inside gate-reads-test's own run
# (it would run itself) and, for the real strace, when this process is already traced
traced=$(sed -n 's/^TracerPid:[[:space:]]*//p' /proc/self/status)
if [ -n "${GATE_TESTS_IN_NESTED:-}" ]; then echo "skip: gate-reads-test cases (in gate-reads-test's own run)"
else
  real=$(command -v strace || true)
  mkdir -p "$scratch/fakebin"
  cat > "$scratch/fakebin/strace" <<'F'
#!/bin/bash
# a fake strace: FAKE_STRACE_MODE none (writes no trace), empty (an empty one), nof (the real one without -f)
out= args=()
while [ $# -gt 0 ]; do
  case $1 in
    -o) out=$2; shift 2 ;;
    -s|-e) args+=("$1" "$2"); shift 2 ;;
    -f) [ "$FAKE_STRACE_MODE" = nof ] || args+=(-f); shift ;;
    -*) args+=("$1"); shift ;;
    *) break ;;
  esac
done
case $FAKE_STRACE_MODE in
  none) exec "$@" ;;
  empty) : > "$out"; exec "$@" ;;
  nof) exec "$FAKE_REAL_STRACE" "${args[@]}" -o "$out" "$@" ;;
esac
F
  chmod +x "$scratch/fakebin/strace"
  for mode in none empty nof; do
    [ "$mode" = nof ] && { [ -n "$real" ] && [ "${traced:-0}" = 0 ] || continue; }
    out=$(PATH="$scratch/fakebin:$PATH" FAKE_STRACE_MODE=$mode FAKE_REAL_STRACE=$real GATE_READS_ASSUME_UNTRACED=1 "$src/test/gate/gate-reads-test.bash" 2>&1); st=$?
    [ "$st" != 0 ] && ok "gate-reads-test fails on a trace that is $mode" || bad "gate-reads-test passed on a trace that is $mode: $out"
  done
  if [ -n "$real" ] && [ "${traced:-0}" = 0 ]; then
    ln -s "$src" "$scratch/lnk"
    out=$("$scratch/lnk/test/gate/gate-reads-test.bash" 2>&1); st=$?
    [ "$st" = 0 ] && grep -q "canary seen" <<< "$out" && ok "gate-reads-test through a symlinked checkout sees its canary" || bad "symlinked checkout: status $st: $out"
    out=$("$real" -f -o "$scratch/outer.strace" "$src/test/gate/gate-reads-test.bash" 2>&1); st=$?
    [ "$st" = 77 ] && grep -q "already traced" <<< "$out" && ok "gate-reads-test under a tracer: not run (77)" || bad "traced: status $st: $out"
  else echo "skip: gate-reads-test real-strace cases (no strace, or this process is traced)"; fi
fi

# the step and the planner
cd "$src" || exit 1
# the file list from git's index (tracked files; fake hashes): nothing of the checkout is read, so the step's
# declared inputs stay true (bin/gate --audit); the planner only checks that its rules match names
git -C "$src" ls-files | LC_ALL=C sort | sed 's|^|0000000000000000000000000000000000000000000000000000000000000000  |' > "$scratch/logs/files.sha256"
echo '{:gate "a" :javac "b" :prog-units "pending" :prog-rest "c" :javac-prog-units "pending" :javac-prog-rest "d" :stage0 "e" :frozen "f" :jdk "g" :go "h" :gcc "i"}' > "$scratch/logs/hosts.edn"
. "$src/bin/lib/tools.bash"
root=$src
# GT9 (GTS210 (a)): the table's :host-ids hosts beside (bin/gate's gate_host_ids adds those a step names)
for h in $(arbace_tool -p "$src/test" -m gate.crops host-names "$src/test/gate/steps.edn"); do
  grep -q ":$h \"" "$scratch/logs/hosts.edn" || sed -i "\$ s/}\$/ :$h \"x\"}/" "$scratch/logs/hosts.edn"
done
# (GD1, GDS7 (a): gate-tests is a step of the full tier; the planner checks every step's rules in the essential mode too, and
# its full mode would read the rotated steps' universes and subsets, which this step does not declare)
# (GD3: no load closures, GATE_LOADS none: their text scan reads arbace/'s sources, which gate-tests does not declare)
GATE_LOADS=$scratch/no-loads.edn arbace_tool -p "$src/test" -m gate.plan plan "$src" "$scratch/logs" "$scratch/store" essential > "$scratch/plan.out" 2>&1
if grep -q "problem(s)" "$scratch/plan.out"; then bad "the planner rejects the table: $(grep -A5 'problem(s)' "$scratch/plan.out")"
elif grep -q "^ *run *claude-md " "$scratch/plan.out" && grep -q '^  {:name gate-tests :mem-gib [0-9]* :phase :full ' "$src/test/gate/steps.edn"; then
  ok "the planner accepts the table, plans claude-md, and gate-tests is a step of the full tier"
else bad "claude-md not planned or gate-tests not :phase :full: $(head -5 "$scratch/plan.out")"; fi

[ "$fails" = 0 ] && echo "gate-tests-test: passed" || echo "gate-tests-test: $fails failed"
exit $((fails > 0))
