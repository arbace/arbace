#!/bin/bash
# The step gate-tests reads only what it declares (audit 041, report 055 finding 1): bin/gate-tests under strace
# (the gate's audit, bin/gate --audit, does the same), and every file of the checkout it opens must lie in the
# step's :inputs of test/gate/steps.edn (the group :lib-tools expanded here) or in .tmp/, .git/, target/.
# Needs strace. By hand: test/gate/gate-reads-test.bash
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
[ -n "${GATE_TESTS_NESTED:-}" ] && { echo "gate-reads-test: nested, skipped"; exit 0; }
command -v strace > /dev/null || { echo "gate-reads-test: no strace" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
trace=$TMPDIR/gate-reads.strace
GATE_TESTS_NESTED=1 strace -f -qq -y -s 4096 -e trace=openat,execve -o "$trace" "$src/bin/gate-tests" > "$TMPDIR/gate-reads.out" 2>&1 \
  || { echo "FAIL: bin/gate-tests under strace failed"; tail -5 "$TMPDIR/gate-reads.out"; exit 1; }
# the step's inputs (the quoted strings of its :inputs vector, from steps.edn)
inputs=$(awk '/\{:name gate-tests /{f=1} f{print} f&&/:summary/{exit}' "$src/test/gate/steps.edn" | sed 's/.*:inputs//; s/:summary.*//' | grep -o '"[^"]*"' | tr -d '"')
inputs="$inputs
bin/lib/tmpdir.bash
bin/lib/renamed-env.bash
bin/lib/tools.bash
bin/lib/host.bash"
undeclared=$(grep -E 'openat' "$trace" | grep -o " = [0-9]*<$src/[^>]*>" | sed "s|^.*<$src/||; s|>\$||" \
  | grep -v '^\.tmp/\|^\.git\|^target/' | LC_ALL=C sort -u | while read -r f; do
    ok=
    for i in $inputs; do
      case $i in */) case $f in "$i"*) ok=1 ;; esac ;; *) [ "$f" = "$i" ] && ok=1 ;; esac
    done
    [ -n "$ok" ] || echo "$f"
  done)
if [ -z "$undeclared" ]; then echo "gate-reads-test: passed (every file opened is declared)"; exit 0; fi
echo "FAIL: files the step reads outside its :inputs: $(echo "$undeclared" | wc -l), e.g. $(echo "$undeclared" | head -5 | tr '\n' ' ')"
exit 1
