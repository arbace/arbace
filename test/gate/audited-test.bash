#!/bin/bash
# The gate's side of the audited rule for Java timeouts (GT7 rerun 8, review 070): the steps table read by
# the planner (test/gate/plan.ae, which validates it) on a scratch plan, and the runner's step
# (bin/lib/gate-run.bash). Seconds, no build, no gate run. GATE_STEPS names another table to test (to
# see a case fail before its fix); ARBACE_HOST_EXE is stage 0. Usage: test/gate/audited-test.bash
# A process already traced cannot run the audited step's case (a tracer cannot be traced again, PTRACE_TRACEME):
# "not run", status 77 (bin/gate-tests lists it so under the audit; AUDITED_TEST_ASSUME_UNTRACED=1 only for
# audited-test-test, which reads its trace). It reads the checkout's file names (git ls-files, zero digests) and
# the scripts and namespaces the step gate-tests declares, not the tree's contents (review 077).
# needs: exe (the planner and stage 0 tools host)
set -u
root=$(cd "$(dirname "$0")/../.." && pwd)
tracer=$(sed -n 's/^TracerPid:[[:space:]]*//p' /proc/self/status)
if [ "${tracer:-0}" != 0 ] && [ -z "${AUDITED_TEST_ASSUME_UNTRACED:-}" ]; then
  echo "already traced by pid $tracer: a tracer cannot be traced again"; exit 77
fi
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$root/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$root/.tmp/tmp}/gate-audited-test.XXXXXX") || exit 2
trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }

# the plan of the table (as bin/lib/gate-run.bash's plan_inputs makes its inputs, the hosts' identities
# zeros), its material per step in $t/plan/NAME.inputs
git -C "$root" ls-files | LC_ALL=C sort | sed 's|^|0000000000000000000000000000000000000000000000000000000000000000  |' > "$t/files.sha256"
z=$(printf '0%.0s' $(seq 64))
printf '{:gate "%s" :javac "%s" :prog-units "none" :prog-rest "%s" :javac-prog-units "none" :javac-prog-rest "%s" :stage0 "%s" :frozen "%s" :jdk "%s" :go "%s" :gcc "%s"}\n' \
  $z $z $z $z $z $z $z $z $z > "$t/hosts.edn"
mkdir -p "$t/store"
. "$root/bin/lib/tools.bash"
# GT9 (GTS210 (a)): the table's :host-ids hosts beside, zeros too (bin/gate's gate_host_ids adds those a step names), so that a
# crop's steps on its own host plan here without this fixture naming it
for h in $(cd "$root" && arbace_tool -p "$root/test" -m gate.crops host-names "${GATE_STEPS:-$root/test/gate/steps.edn}"); do
  grep -q ":$h \"" "$t/hosts.edn" || sed -i "\$ s/}\$/ :$h \"$z\"}/" "$t/hosts.edn"
done
planned=0
if (cd "$root" && arbace_tool -p "$root/test" -m gate.plan plan "$root" "$t" "$t/store" full) > "$t/plan.out" 2>&1; then planned=1; fi
# decl NAME: the step's declaration line of its material
decl() { sed -n "/^step $1\$/{n;p;q}" "$t/plan/$1.inputs" 2>/dev/null; }

if [ $planned = 1 ]; then ok "the table validates and plans"; else no "the table validates and plans: $(head -5 "$t/plan.out")"; fi

# finding 1: java-judge keeps its output (held.txt) and java-held runs the held tests again, keyed by it
if decl java-judge | grep -q ':outputs true' && decl java-judge | grep -q 'held.txt'; then ok "java-judge writes held.txt as an output"; else no "java-judge writes held.txt as an output"; fi
if decl java-held | grep -q ':uses \["java-judge/held.txt"\]'; then ok "java-held uses java-judge's held.txt"; else no "java-held uses java-judge's held.txt"; fi
if decl java-held | grep -q -- '--levels interp' && ! decl java-held | grep -q -- '--save'; then ok "java-held runs the interp level, unsaved (its own judgment)"; else no "java-held runs interp, unsaved"; fi
# review 077: the clean run (GATE_CLEAN_INNER) keys java-held apart, so its traced pass is not the keyed gate's
if decl java-held | grep -q ':env \["GATE_CLEAN_INNER"\]'; then ok "java-held's key holds GATE_CLEAN_INNER"; else no "java-held's key holds GATE_CLEAN_INNER"; fi
# its command, as the planner lists it (plan.tsv's last column), run in a scratch tree with a stub bin/java-check
hcmd=$(awk -F'\t' '$1 == "java-held" { print $NF }' "$t/plan.tsv")
mkdir -p "$t/h/bin" "$t/h/outs/java-judge"
printf '#!/bin/bash\necho "STUB $*" >> "$STUBLOG"\n' > "$t/h/bin/java-check"; chmod +x "$t/h/bin/java-check"
printf 't1\nt2\n' > "$t/h/outs/java-judge/held.txt"
hrun() { : > "$t/h/stub.log"; (cd "$t/h" && env -u GATE_AUDITED "$@" STUBLOG="$t/h/stub.log" GATE_OUTS="$t/h/outs" GATE_EXE=x GATE_JAVAC=y GATE_JAVA_FORMS=z bash -c "$hcmd") > "$t/h/out" 2>&1; }
hrun env
if grep -q 'STUB --levels interp --only \^(t1|t2) ' "$t/h/stub.log" && ! grep -q -- '--timeout' "$t/h/stub.log"; then ok "java-held untraced: the held ids run at the default limit"; else no "java-held untraced: $(cat "$t/h/stub.log")"; fi
hrun env GATE_AUDITED=1
if [ ! -s "$t/h/stub.log" ] && grep -q 'DEFERRED' "$t/h/out" && grep -q 't1 t2' "$t/h/out"; then ok "java-held under the audit: defers loudly, runs nothing"; else no "java-held under the audit: $(cat "$t/h/stub.log" "$t/h/out")"; fi
: > "$t/h/outs/java-judge/held.txt"; hrun env
if [ ! -s "$t/h/stub.log" ] && grep -q 'no test held' "$t/h/out"; then ok "java-held with nothing held: runs nothing"; else no "java-held, nothing held"; fi

# GT8: java-judge judges the results files its :uses name (GATE_USES: the levels' parts, or their rotated checks)
jcmd=$(awk -F'\t' '$1 == "java-judge" { print $NF }' "$t/plan.tsv")
: > "$t/h/stub.log"
(cd "$t/h" && STUBLOG="$t/h/stub.log" GATE_OUTS=/o GATE_OUT=/j GATE_USES="java-read.jls/results.tsv java-read.jls/audited java-interp.jls.sub/results.tsv" bash -c "$jcmd") > "$t/h/out" 2>&1
if grep -qx 'STUB --judge --held /j/held.txt /o/java-read.jls/results.tsv /o/java-interp.jls.sub/results.tsv' "$t/h/stub.log"; then ok "java-judge: the results files of GATE_USES"; else no "java-judge's command: $(cat "$t/h/stub.log" "$t/h/out")"; fi

# finding 2: the audited marker of every producer is in java-judge's key (a results file alone is not: an
# unaudited rerun with the same rows would find the audited pass). java-check-judge uses the whole directory
# of java-check, the marker included
# (GT8: the levels parted by corpus, each part's marker: java-read.jdk-run/audited, ...)
for p in java-read java-interp java-classes java-diagnostics java-compiled-full java-print; do
  np=$(cut -f1 "$t/plan.tsv" | grep -c "^$p\\.")
  if [ "$np" -gt 0 ] && [ "$(decl java-judge | grep -oE "\"$p\\.[a-z-]+/audited\"" | sort -u | wc -l)" = "$np" ]; then ok "java-judge's key has $p's parts' audited ($np)"; else no "java-judge's key has $p's parts' audited ($np parts)"; fi
done
if decl java-check-judge | grep -q ':uses \["java-check"\]'; then ok "java-check-judge uses java-check's whole output (its marker too)"; else no "java-check-judge uses java-check's whole output"; fi

# finding 5: the runner says a step is audited (GATE_AUDITED=1 in the step's environment, in the traced
# run only; an inherited value is removed in an unaudited step)
logs=$t/logs; store=$t/store; mkdir -p "$logs"; : > "$logs/mem.tsv"
. "${GATE_RUN_LIB:-$root/bin/lib/gate-run.bash}"  # GATE_RUN_LIB: another runner, to see a case fail before its fix
red=(); audit_trace=${audit_trace:-execve,openat}
GATE_AUDITED=1 audit_step= step plain no bash -c 'echo "GATE_AUDITED=${GATE_AUDITED:-unset}"'
if grep -qx 'GATE_AUDITED=unset' "$logs/plain.log"; then ok "an unaudited step does not see GATE_AUDITED (even exported outside)"; else no "unaudited step's GATE_AUDITED: $(cat "$logs/plain.log")"; fi
unset GATE_AUDITED
audit_step=1 step traced no bash -c 'echo "GATE_AUDITED=${GATE_AUDITED:-unset}"; awk "\$1 == \"TracerPid:\" { print \"tracer \" \$2 }" /proc/self/status'
if grep -qx 'GATE_AUDITED=1' "$logs/traced.log" && grep -q '^tracer [1-9]' "$logs/traced.log"; then ok "an audited step sees GATE_AUDITED=1 under a tracer"; else no "audited step: $(cat "$logs/traced.log")"; fi

# review 077, finding 4: bin/gate --audit (gate_audit's strace line) says GATE_AUDITED=1 too, as step() does for the clean run
lib=${GATE_RUN_LIB:-$root/bin/lib/gate-run.bash}
if grep 'ARBACE_LOADED_LOG="$d/$n.loaded"' "$lib" | grep -q 'GATE_AUDITED=1'; then ok "gate_audit's traced run has GATE_AUDITED=1"; else no "gate_audit's traced run lacks GATE_AUDITED=1"; fi

echo "gate audited-test: $((n - bad)) of $n passed"
[ $bad = 0 ]
