#!/bin/bash
# bin/goi check's retry of the failed (the user, 2026-11-14, round 33; night 13's typeparam/chans.go): a program the reference
# records passing that fails among the others runs again alone, once, and its second result stands. Two cases on helloworld.go,
# by the hooks GOI_CHECK_FAIL_ONCE and GOI_CHECK_FAIL (check_one), each a real check of one program (about a minute warm):
#   - its first run forced to fail: the retry runs it, it passes, the check passes, the forced row kept in first-fails.tsv;
#   - the control: every run forced to fail: the retry fails too and the check reports the regression and fails.
# By hand (needs goi's executable: GOI_RUN_EXE, or the build's): bash test/goi/retry-test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$root/.tmp/tmp}; mkdir -p "$TMPDIR"
bad=0
out=$(cd "$root" && GOI_CHECK_FAIL_ONCE=helloworld.go bin/goi check --only '^helloworld\.go$' 2>&1); st=$?
if [ "$st" = 0 ] && grep -q 'passing in check-linux-amd64.edn failed, run again one at a time' <<< "$out" \
   && grep -q '1 pass, 0 fail' <<< "$out" && grep -q 'forced once' "$root/.tmp/goi-check/first-fails.tsv"; then
  echo "ok   a recorded pass that fails once passes on its retry, its first row kept"
else echo "FAIL the retry of a program failing once: status $st"; tail -8 <<< "$out"; bad=1; fi
out=$(cd "$root" && GOI_CHECK_FAIL=helloworld.go bin/goi check --only '^helloworld\.go$' 2>&1); st=$?
if [ "$st" = 1 ] && grep -q 'regression: helloworld.go: fail forced (GOI_CHECK_FAIL)' <<< "$out"; then
  echo "ok   control: a recorded pass that fails every time is a regression after its retry"
else echo "FAIL the control (failing every time): status $st"; tail -8 <<< "$out"; bad=1; fi
exit $bad
