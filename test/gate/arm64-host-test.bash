#!/bin/bash
# B1 (the user, 2026-11-15; doc/GATE-SURVEY.md, "As built: the CPU hosts"): a CPU's host, :arm64, its :host-ids recipe an
# [:arch NAME...] item. On a made-up table and store:
#  1. bin/gate's gate_host_id: none unless the CPU (uname -m, GATE_HOST_ARCH in its place) is one of the item's names; on one,
#     an identity, the CPU's name in its hash (control: a recipe without :arch is never none for the CPU)
#  2. the planner (gate.plan's cpu-hosts): a step on :arm64 whose identity is none is absent in the essential and the full phase,
#     no key, no .inputs; on an identity it is keyed and runs, the identity in its material; control: a step on a host none here
#     whose recipe has no :arch (:gcc) is not absent outside the oracle tier, as before
#  3. the runner: an absent step's status is absent, reported "absent", no pass recorded, the run not failed
#  4. the checkout's table: :arm64's recipe is an :arch one (so cpu-hosts holds it) and the fixture step arm64-host is on it
# Seconds, no build, no gate run. Usage: test/gate/arm64-host-test.bash
# needs: exe (the planner on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/arm64-host-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
bad=0 n=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"

# ---- 1. gate_host_id
eval "$(sed -n '/^# --- gt9-hosts begin/,/^# --- gt9-hosts end/p' "$here/bin/gate")"
type gate_host_id > "$t/type" 2>&1 || { no "bin/gate's gate_host_id: $(cat "$t/type")"; exit 1; }
printf 'arch\taarch64\tarm64\n' > "$t/arm64.tsv"
printf 'require\tsh\n' > "$t/plain.tsv"
i_amd=$(GATE_HOST_ARCH=x86_64 gate_host_id "$t/arm64.tsv"); i_arm=$(GATE_HOST_ARCH=aarch64 gate_host_id "$t/arm64.tsv")
i_mac=$(GATE_HOST_ARCH=arm64 gate_host_id "$t/arm64.tsv")
[ "$i_amd" = none ] && ok "gate_host_id: :arch on x86_64 is none" || no "gate_host_id on x86_64: $i_amd"
[[ $i_arm =~ ^[0-9a-f]{64}$ ]] && [[ $i_mac =~ ^[0-9a-f]{64}$ ]] && [ "$i_arm" != "$i_mac" ] \
  && ok "gate_host_id: on aarch64 and arm64 an identity each, the CPU's name in it" || no "gate_host_id on arm64: $i_arm $i_mac"
[ "$(GATE_HOST_ARCH=x86_64 gate_host_id "$t/plain.tsv")" != none ] && ok "control: a recipe without :arch is not none on x86_64" \
  || no "control: a recipe without :arch is none"

# ---- 2. the planner
r=$t/root; mkdir -p "$r/test/gate" "$r/bin"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
echo 1 > "$r/test/gate/epoch"
cat > "$r/test/gate/steps.edn" <<'EOT'
{:groups {}
 :steps
 [{:name arm-e :mem-gib 1 :phase :essential :cmd "bin/tool" :inputs ["bin/tool"] :hosts [:arm64]}
  {:name arm-f :mem-gib 1 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts [:go :arm64]}
  {:name gcc-f :mem-gib 1 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts [:gcc]}
  {:name plain :mem-gib 1 :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts []}]
 :host-ids {:arm64 [[:arch "aarch64" "arm64"]]
            :gcc [[:require "gcc"]]}}
EOT
(cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$t/files.sha256"
: > "$t/ancestors.txt"
store=$t/store; mkdir -p "$store"
export GATE_STEPS=$r/test/gate/steps.edn GATE_CANARY=0; unset GATE_LOADS
plan() { local d=$1 arm=$2; rm -rf "$d"; mkdir -p "$d"; cp "$t/files.sha256" "$t/ancestors.txt" "$d/"
  printf '{:gate "g" :go "o" :gcc "none" :arm64 "%s"}\n' "$arm" > "$d/hosts.edn"
  (cd "$here" && arbace_tool -p "$here/test" -m gate.plan plan "$r" "$d" "$store" full) > "$d.out" 2>&1; }
col() { awk -F'\t' -v n="$2" -v c="$3" '$1 == n {print $c}' "$1/plan.tsv"; }
plan "$t/off" none || no "the plan off arm64: $(head -5 "$t/off.out")"
[ "$(col "$t/off" arm-e 2)" = absent ] && [ "$(col "$t/off" arm-f 2)" = absent ] \
  && ok "off arm64: arm-e (essential) and arm-f (full) absent" || no "off arm64: arm-e $(col "$t/off" arm-e 2), arm-f $(col "$t/off" arm-f 2)"
[ ! -e "$t/off/plan/arm-e.inputs" ] && [ ! -e "$t/off/plan/arm-f.inputs" ] && ok "off arm64: no material, no key" \
  || no "off arm64: an absent step was keyed"
grep -q 'arm-f.*host :arm64 none here' "$t/off.out" "$t/off/plan.tsv" && ok "off arm64: the why names the host" \
  || no "the why: $(col "$t/off" arm-f 13)"
[ "$(col "$t/off" gcc-f 2)" = run ] && [ -s "$t/off/plan/gcc-f.inputs" ] \
  && ok "control: gcc-f, on a host none here without :arch, is keyed and runs outside the oracle tier" || no "control gcc-f: $(col "$t/off" gcc-f 2)"
[ "$(col "$t/off" plain 2)" = run ] && ok "control: plain runs" || no "plain: $(col "$t/off" plain 2)"
plan "$t/on" abc123 || no "the plan on arm64: $(head -5 "$t/on.out")"
[ "$(col "$t/on" arm-e 2)" = run ] && [ "$(col "$t/on" arm-f 2)" = run ] && grep -qx 'host arm64 abc123' "$t/on/plan/arm-f.inputs" \
  && ok "on arm64: arm-e and arm-f keyed and run, the identity in their material" || no "on arm64: arm-e $(col "$t/on" arm-e 2), arm-f $(col "$t/on" arm-f 2)"

# ---- 3. the runner
(
  logs=$t/off; store=$t/store; status=0
  GATE_HEAVY_CMD=env; . "$here/bin/lib/gate-run.bash"
  record_pass() { echo "$1" >> "$t/recorded"; }
  load_plan
  run_one arm-f; echo "rc $?" > "$t/run.rc"
  report arm-f > "$t/report.txt"; echo "status $status" >> "$t/run.rc"
)
[ "$(cat "$t/off/arm-f.status" 2>"$t/st.err")" = absent ] && grep -qx 'rc 0' "$t/run.rc" && grep -qx 'status 0' "$t/run.rc" \
  && ok "the runner: arm-f's status absent, the run not failed" || no "the runner: $(cat "$t/off/arm-f.status" "$t/run.rc" 2>&1)"
grep -q '^   absent  arm-f (host :arm64 none here' "$t/report.txt" && ok "the report: absent with its why" || no "the report: $(cat "$t/report.txt")"
[ ! -e "$t/recorded" ] && ok "no pass recorded for an absent step" || no "a pass was recorded: $(cat "$t/recorded")"

# ---- 4. the checkout's table
unset GATE_STEPS
(cd "$here" && arbace_tool -p "$here/test" -e "(require 'gate.plan) (let [t (read-string (slurp \"test/gate/steps.edn\"))] (println (sort (gate.plan/cpu-hosts (:host-ids t)))) (println (some #(when (= 'arm64-host (:name %)) (:hosts %)) (:steps t))))") > "$t/table.out" 2>&1
[ "$(sed -n 1p "$t/table.out")" = "(:arm64)" ] && [ "$(sed -n 2p "$t/table.out")" = "[:arm64]" ] \
  && ok "the checkout's table: cpu-hosts (:arm64), the fixture arm64-host on [:arm64]" || no "the checkout's table: $(head -3 "$t/table.out")"

echo "arm64-host-test: $((n - bad)) of $n ok"
exit $((bad > 0))
