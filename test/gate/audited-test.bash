#!/bin/bash
# The gate's side of the audited rule for Java timeouts (GT7 rerun 8, review 070): the steps table read by
# the planner (test/gate/plan.ae, which validates it) on a scratch plan, and the runner's step
# (bin/lib/gate-run.bash). Seconds, no build, no gate run. GATE_STEPS names another table to test (to
# see a case fail before its fix); ARBACE_HOST_EXE is stage 0. Usage: test/gate/audited-test.bash
set -u
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
t=$(mktemp -d "${TMPDIR:-$root/.tmp/tmp}/gate-audited-test.XXXXXX") || exit 2
trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }

# the plan of the table (as bin/lib/gate-run.bash's plan_inputs makes its inputs, the hosts' identities
# zeros), its material per step in $t/plan/NAME.inputs
(cd "$root" && find . \( -path ./.git -o -path ./.tmp -o -path ./target -o -path ./.claude \) -prune -o -type f -printf '%P\n' \
   | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$t/files.sha256"
z=$(printf '0%.0s' $(seq 64))
printf '{:gate "%s" :javac "%s" :prog-units "none" :prog-rest "%s" :javac-prog-units "none" :javac-prog-rest "%s" :stage0 "%s" :frozen "%s" :jdk "%s" :go "%s"}\n' \
  $z $z $z $z $z $z $z $z > "$t/hosts.edn"
mkdir -p "$t/store"
. "$root/bin/lib/tools.bash"
planned=0
if (cd "$root" && arbace_tool -p "$root/test" -m gate.plan plan "$root" "$t" "$t/store" full) > "$t/plan.out" 2>&1; then planned=1; fi
# decl NAME: the step's declaration line of its material
decl() { sed -n "/^step $1\$/{n;p;q}" "$t/plan/$1.inputs" 2>/dev/null; }

if [ $planned = 1 ]; then ok "the table validates and plans"; else no "the table validates and plans: $(head -5 "$t/plan.out")"; fi

# finding 1: java-judge keeps its output (held.txt) and java-held runs the held tests again, keyed by it
if decl java-judge | grep -q ':outputs true' && decl java-judge | grep -q 'held.txt'; then ok "java-judge writes held.txt as an output"; else no "java-judge writes held.txt as an output"; fi
if decl java-held | grep -q ':uses \["java-judge/held.txt"\]'; then ok "java-held uses java-judge's held.txt"; else no "java-held uses java-judge's held.txt"; fi
if decl java-held | grep -q -- '--levels interp' && ! decl java-held | grep -q -- '--save'; then ok "java-held runs the interp level and judges it itself, unaudited"; else no "java-held runs interp, unsaved"; fi

echo "gate audited-test: $((n - bad)) of $n passed"
[ $bad = 0 ]
