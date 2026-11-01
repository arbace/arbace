#!/bin/bash
# test/gate/audited-test.bash behaves where bin/gate-tests runs it (review 077): already traced, it says so with
# status 77 (a tracer cannot be traced again; the clean run and gate-reads-test trace it), and untraced it reads no
# file of the checkout outside what the step gate-tests declares: the trace of a run (strace -f, the successful
# opens) holds none of the tree's bulk (jdk/, go/, overlay/, tamago/, ...), and does hold the steps table (a canary
# against a blind trace). AUDITED_TEST_CHILD names another script (one copied into test/gate/, to see the cases
# fail before the fix). Needs strace. Traced itself: "not run", 77.
# needs: exe (the child's planner)
set -u
root=$(cd "$(dirname "$0")/../.." && pwd -P)
tracer=$(sed -n 's/^TracerPid:[[:space:]]*//p' /proc/self/status)
if [ "${tracer:-0}" != 0 ]; then echo "already traced by pid $tracer"; exit 77; fi
command -v strace > /dev/null || { echo "audited-test-test: no strace" >&2; exit 2; }
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0}"
export TMPDIR=${TMPDIR:-$root/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$(mktemp -d "$TMPDIR/audited-test-test.XXXXXX") || exit 2
trap 'rm -rf "$t"' EXIT
child=${AUDITED_TEST_CHILD:-$root/test/gate/audited-test.bash}
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }

# 1: traced, the child is "not run"
strace -f -qq -o "$t/tr1" -e trace=none "$child" > "$t/out1" 2>&1; st=$?
if [ $st = 77 ]; then ok "traced: status 77, not run"; else no "traced: status $st (want 77): $(tail -2 "$t/out1")"; fi

# 2: untraced (assumed, under the trace that reads it): the files it opens for reading
AUDITED_TEST_ASSUME_UNTRACED=1 strace -f -qq -y -s 4096 -o "$t/tr2" -e trace=openat,open "$child" > "$t/out2" 2>&1
[ -s "$t/tr2" ] || no "the trace is empty"
grep -v ' = -1 ' "$t/tr2" | grep -E 'open(at)?\(' | grep -E ', O_RDONLY' | sed -n 's/^[^"]*"\([^"]*\)".*/\1/p' \
  | awk -v r="$root" '{ print (substr($0, 1, 1) == "/" ? $0 : r "/" $0) }' | sort -u > "$t/opened"
if grep -qx "$root/test/gate/steps.edn" "$t/opened"; then ok "the trace is not blind (the steps table was opened)"; else no "the trace is blind: steps.edn not opened ($(wc -l < "$t/opened") files)"; fi
bulk=$(grep -E "^$root/(jdk|go|overlay|tamago|x509roots|doc|seed|tools|box|test/java|test/repr|test/goi)/" "$t/opened" | head -3)
if [ -z "$bulk" ]; then ok "no file of the tree's bulk read ($(grep -c "^$root/" "$t/opened") files of the checkout opened)"; else no "undeclared reads, the first: $(echo "$bulk" | tr '\n' ' ')"; fi

echo "audited-test-test: $((n - bad)) of $n passed"
[ $bad = 0 ]
