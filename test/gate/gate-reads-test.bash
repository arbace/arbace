#!/bin/bash
# The step gate-tests reads only what it declares (audit 041, reports 055 and 059): bin/gate-tests --nested under
# strace, reduced by the audit's own reducer (test/gate/audit.awk), and every file of the checkout it opens for
# reading or executes must lie in the step's :inputs of test/gate/steps.edn (the group :lib-tools expanded here).
# Guards against passing without having looked: the trace must be there and not empty, it must hold an execve of
# bin/gate-tests and of every other test, and a canary read of an undeclared file (LICENSE.md, read by the
# traced shell) must be reported, or the trace is blind (no -f, a symlinked path, a syscall not traced).
# A process that is already traced (bin/gate --audit runs the step under strace) cannot be traced again:
# "not run", status 77, which bin/gate-tests lists as not run; the audit does this job there.
# Needs strace. By hand: test/gate/gate-reads-test.bash
# needs: exe (bin/gate-tests runs bin/gate-tests under strace)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd -P)
tracer=$(sed -n 's/^TracerPid:[[:space:]]*//p' /proc/self/status)
if [ "${tracer:-0}" != 0 ] && [ -z "${GATE_READS_ASSUME_UNTRACED:-}" ]; then
  echo "already traced by pid $tracer: the audit holds the step to its inputs"; exit 77
fi
command -v strace > /dev/null || { echo "gate-reads-test: no strace" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
failed_calls_out() {
  awk 'NR == FNR { if (match($0, /^[0-9]+ /)) { p = substr($0, 1, RLENGTH - 1)
                     if ($0 ~ /<unfinished \.\.\.>$/) at[p] = FNR
                     else if ($0 ~ /^[0-9]+ +<\.\.\. [a-z_0-9]+ resumed>/ && (p in at)) { if ($0 ~ / = -1 /) drop[at[p]] = 1; delete at[p] } }
                   next }
       !(FNR in drop) && $0 !~ / = -1 /' "$1" "$1"
}
# its own check, on a made-up trace: a split failed open goes, a split successful one and a plain one stay
printf '%s\n' '7 openat(AT_FDCWD</r>, "/r/gate/x.ae", O_RDONLY|O_CLOEXEC <unfinished ...>' '8 openat(AT_FDCWD</r>, "/r/y", O_RDONLY) = 3</r/y>' \
  '7 <... openat resumed>) = -1 ENOENT (No such file or directory)' '9 openat(AT_FDCWD</r>, "/r/test/gate/x.ae", O_RDONLY <unfinished ...>' \
  '8 newfstatat(AT_FDCWD</r>, "/r/z", 0x1, 0) = -1 ENOENT (No such file or directory)' '9 <... openat resumed>) = 5</r/test/gate/x.ae>' > "$TMPDIR/gate-reads.$$.fx"
failed_calls_out "$TMPDIR/gate-reads.$$.fx" > "$TMPDIR/gate-reads.$$.fx.out"
[ "$(grep -c . "$TMPDIR/gate-reads.$$.fx.out")" = 3 ] && ! grep -q -e '"/r/gate/x.ae"' -e '/r/z' "$TMPDIR/gate-reads.$$.fx.out" \
  || { echo "FAIL: the failed calls' filter on its made-up trace: $(cat "$TMPDIR/gate-reads.$$.fx.out")"; exit 1; }
rm -f "$TMPDIR/gate-reads.$$.fx" "$TMPDIR/gate-reads.$$.fx.out"
trace=$TMPDIR/gate-reads.$$.strace
rm -f "$trace" "$trace.ok" "$trace.paths"
# --seccomp-bpf (n-gate-parts, 2026-11-19, as bin/gate --audit's): the tracees stop only on the traced calls, the same
# trace near full speed (without it the nested run took 110 of the step's 128 minutes on the night of 2026-11-17)
GATE_TESTS_NESTED= strace -f -qq -y -s 4096 --seccomp-bpf -e "trace=%file,execve,execveat,clone,clone3,fork,vfork,chdir,fchdir" -o "$trace" \
  bash -c 'read -r canary < "$1"; shift; exec "$@"' _ "$src/LICENSE.md" "$src/bin/gate-tests" --nested > "$TMPDIR/gate-reads.$$.out" 2>&1 \
  || { echo "FAIL: bin/gate-tests under strace failed (its output kept: $TMPDIR/gate-reads.$$.out; each test's log TMPDIR/gate-tests.nested.NAME.log)"
       grep -a -A40 ' FAILED' "$TMPDIR/gate-reads.$$.out" | head -80; tail -5 "$TMPDIR/gate-reads.$$.out"; exit 1; }
[ -s "$trace" ] || { echo "FAIL: strace wrote no trace ($trace)"; exit 1; }
# (calls that failed - a lookup along the class path - read nothing). A call strace splits across two lines (another
# thread's call in between: "openat(...) <unfinished ...>", later "<... openat resumed>) = -1 ENOENT") failed too: its
# unfinished line is dropped with its result's (night 13: the loader's miss of $src/gate/crops.ae on ARBACE_PATH's
# first root, split so, was taken for a read)
failed_calls_out "$trace" > "$trace.ok" || { echo "FAIL: the failed calls' filter failed"; exit 1; }
awk -v root="$src" -f "$src/test/gate/audit.awk" "$trace.ok" > "$trace.paths" || { echo "FAIL: the reducer failed"; exit 1; }
[ -s "$trace.paths" ] || { echo "FAIL: the reduced trace is empty"; exit 1; }
# positive controls: bin/gate-tests and every other test were executed
grep -qxF "$(printf 'X\t%s' "$src/bin/gate-tests")" "$trace.paths" || { echo "FAIL: no execve of bin/gate-tests in the trace"; exit 1; }
for t in "$src"/test/gate/*-test.bash; do
  [ "$t" = "$src/test/gate/gate-reads-test.bash" ] && continue
  grep -qxF "$(printf 'X\t%s' "$t")" "$trace.paths" || { echo "FAIL: no execve of $(basename "$t") in the trace"; exit 1; }
done
# the step's inputs (the quoted strings of its :inputs vector, from steps.edn)
inputs=$(awk '/\{:name gate-tests /{f=1} f{print} f&&/:summary/{exit}' "$src/test/gate/steps.edn" | sed 's/.*:inputs//; s/:summary.*//' | grep -o '"[^"]*"' | tr -d '"')
inputs="$inputs
bin/lib/tmpdir.bash
bin/lib/renamed-env.bash
bin/lib/tools.bash
bin/lib/host.bash"
# the canary is declared for the audit (the fake-strace cases of gate-tests-test read it under bin/gate --audit's trace),
# never here: it must stay undeclared for this check
inputs=$(echo "$inputs" | grep -vx "LICENSE.md")
undeclared=$(awk -F'\t' '$1 == "R" || $1 == "X" || $1 == "H" { print $2 }' "$trace.paths" | grep "^$src/" | sed "s|^$src/||" \
  | grep -v '^\.tmp/\|^\.git\|^target/' | LC_ALL=C sort -u | while read -r f; do
    ok=
    for i in $inputs; do
      case $i in */) case $f in "$i"*) ok=1 ;; esac ;; *) [ "$f" = "$i" ] && ok=1 ;; esac
    done
    [ -n "$ok" ] || echo "$f"
  done)
# the canary must be among the undeclared, or the trace saw nothing
grep -qx "LICENSE.md" <<< "$undeclared" || { echo "FAIL: the canary read of LICENSE.md is not in the trace: the trace is blind"; exit 1; }
undeclared=$(echo "$undeclared" | grep -vx "LICENSE.md")
rm -f "$trace" "$trace.ok" "$trace.paths" "$TMPDIR/gate-reads.$$.out"
if [ -z "$undeclared" ]; then echo "gate-reads-test: passed (canary seen; every other file read or executed is declared)"; exit 0; fi
echo "FAIL: files the step reads outside its :inputs: $(echo "$undeclared" | wc -l), e.g. $(echo "$undeclared" | head -5 | tr '\n' ' ')"
exit 1
