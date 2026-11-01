#!/bin/bash
# test/java/timeouts-test.bash [ROOT]: queue 115's test of GTS102 redone after the audits (bin/java-check's per-test
# limits, test/java/timeouts.txt, on main-merge's limit.bash: attempt_limits, JAVA_CHECK_ONCE, note_attempt,
# program_tmo). ROOT holds bin/ and test/ (default the checkout this script is in); run by bin/gate-tests through test/gate/java-timeouts-test.bash.
# Needs python3 (the steps.edn keys). Runs copies of bin/java-check's and
# bin/lib/limit.bash's functions (extracted by sed, not retyped) with `limited` stubbed to log its TMO and time
# out, over fabricated plans; then checks the committed files as data (timeouts.txt, divergences.edn,
# steps.edn's keys with the planner's last-rule-wins matching, copied into python). PASS/FAIL, exit 0/1.
set -u
R=${1:-$(cd "$(dirname "$0")/../.." && pwd)}
TMPDIR=${TMPDIR:-$R/.tmp/tmp}; mkdir -p "$TMPDIR"
T=$TMPDIR/gts102.$$
mkdir -p "$T"; trap 'rm -rf "$T"' EXIT
fails=0 n=0
ok() { n=$((n + 1)); echo "ok   $1"; }
bad() { n=$((n + 1)); echo "FAIL $1"; fails=$((fails + 1)); }
check() { if [ "$2" = "$3" ]; then ok "$1: $2"; else bad "$1: got '$2', want '$3'"; fi; }

ext() { # FILE NAMES...: the functions as FILE defines them
  local f=$1 g; shift
  for g in "$@"; do sed -n "/^$g() {.*}\$/p; /^$g() {\$/,/^}\$/p" "$f"; done
}
{ ext "$R/bin/lib/limit.bash" attempt_limits timeout_kind note_attempt program_tmo
  ext "$R/bin/java-check" first_line root_cause mask_hashes run_dirs mask_dirs judge test_limit run_actions; } > "$T/fns.bash"
. "$T/fns.bash"
limited() { echo "$1" >> "$T/lim.log"; return "${STUB_ST:-124}"; }

root=$T/root work=$T/work jdktest=$T/jdk timeout=20
mkdir -p "$root/test/java" "$work"; : > "$work/empty"
cp "$R/test/java/timeouts.txt" "$root/test/java/" 2>/dev/null || : > "$root/test/java/timeouts.txt"
mkplan() { # ID NAME
  mkdir -p "$work/run/$1"
  printf 'test\t%s\ndir\tx\nroot\tjdk26u\nclasses\t0\nrun\t0\t0\t-\t0\t0\t-\t0\tMain\n' "$2" > "$work/run/$1/plan.tsv"
}
run() { # ID: run_actions' result line; the limits limited got in lim.log
  local id=$1 p=$work/run/$1 dir=$T; : > "$T/lim.log"
  run_actions interp "$T/exe" files "$T/forms" 2>/dev/null
}
lims() { paste -sd' ' "$T/lim.log"; }
mkplan t1 "hotspot/jtreg/compiler/c2/IVTest.java"
mkplan t2 "hotspot/jtreg/compiler/loopopts/superword/TestNoInputInLoop.java#WithFlags"
mkplan t3 "jdk/java/lang/Unlisted.java"
mkplan t4 "hotspot/jtreg/compiler/loopopts/TestCountedLoopPhiValue.java"

# 1. the limit and the second chance
r=$(run t1); check "a listed test's attempts" "$(lims)" "60 120"
check "its reason" "$(cut -f3,4 <<< "$r")" "timeout	60 s, then 120 s"
r=$(run t2); check "a listed name with #" "$(lims)" "60 120"
r=$(run t4); check "t03686, rerun 8's 14th, listed" "$(lims)" "60 120"
r=$(run t3); check "an unlisted test's attempts" "$(lims)" "20 40"
check "its reason, unchanged" "$(cut -f3,4 <<< "$r")" "timeout	20 s, then 40 s"
r=$(timeout=100 run t1); check "--timeout 100 above a listed 60 (a line never lowers)" "$(lims)" "100 200"
r=$(STUB_ST=0 run t1); check "a pass runs once, at the test's limit" "$(lims) $(cut -f3 <<< "$r")" "60 pass"
# 2. --essential-compiled's quiet retry: one attempt (JAVA_CHECK_ONCE) at twice the test's limit (tmo_scale=2)
r=$(JAVA_CHECK_ONCE=1 tmo_scale=2 run t1); check "the quiet retry, listed" "$(lims) / $(cut -f4 <<< "$r")" "120 / 120 s"
r=$(JAVA_CHECK_ONCE=1 tmo_scale=2 run t3); check "the quiet retry, unlisted" "$(lims) / $(cut -f4 <<< "$r")" "40 / 40 s"
check "the retry scales the test's limit, not the global" "$(grep -c 'JAVA_CHECK_ONCE=1 tmo_scale=2 xargs' "$R/bin/java-check")" "1"
check "no retry doubles the global timeout" "$(grep -c 'JAVA_CHECK_ONCE=1 timeout=' "$R/bin/java-check")" "0"
# 3. timeout-attempts.tsv (GTS180 (d)) logs the test's limits
r=$(ARBACE_TIMEOUT_LOG=$T/att.tsv run t1); check "note_attempt's limits" "$(cut -f4 "$T/att.tsv" 2>/dev/null | paste -sd' ')" "60 120"
# 4. the file's edges
printf '# hotspot/jtreg/compiler/c2/IVTest.java 90\n' >> "$root/test/java/timeouts.txt"
r=$(run t1); check "a commented line is no limit" "$(lims)" "60 120"
printf 'jdk/java/lang/Unlisted.java x\n' >> "$root/test/java/timeouts.txt"
r=$(run t3); check "a line that is no number leaves the default" "$(lims)" "20 40"
printf 'jdk/java/lang/Unlisted.java 10\n' >> "$root/test/java/timeouts.txt"
r=$(run t3); check "a line below the default leaves the default" "$(lims)" "20 40"
mv "$root/test/java/timeouts.txt" "$T/tt"; r=$(run t1); check "no file: the default" "$(lims)" "20 40"; mv "$T/tt" "$root/test/java/timeouts.txt"
# 5. exported where run_actions is (the xargs subshells)
check "test_limit and program_tmo in both export -f lines" "$(grep -c '^  export -f .*program_tmo test_limit .*run_actions' "$R/bin/java-check")" "2"
check "one limited call (run_actions')" "$(grep -c '&& limited ' "$R/bin/java-check")" "1"

# 6. the committed data
tt=$R/test/java/timeouts.txt
check "timeouts.txt's lines" "$(grep -vc '^#' "$tt" 2>/dev/null)" "14"
check "each at 60" "$(grep -v '^#' "$tt" 2>/dev/null | awk '$2 != 60' | wc -l)" "0"
want=$(for id in t03235 t03314 t03513 t03627 t03628 t03720 t03744 t03756 t03796 t03797 t03798 t03836 t04819 t03686; do
  grep -o "\[\"$id\" \"[^\"]*\"" "$R/test/java/check-interp.edn" | sed 's/.* "//; s/"$//'; done | LC_ALL=C sort)
check "the 14 held tests' names, as check-interp.edn" "$(grep -v '^#' "$tt" 2>/dev/null | cut -d' ' -f1 | LC_ALL=C sort | md5sum)" "$(md5sum <<< "$want")"
check ":audit-timeout kept, :audited (GTS180 (a))" "$(awk '/:id :audit-timeout/ { f = 1 } f && /:audited true/ { print "yes"; exit }' "$R/test/java/divergences.edn")" "yes"
check ":audit-timeout's :why names the file" "$(awk '/:id :audit-timeout/ { f = 1 } f && /:why / { print (index($0, "test/java/timeouts.txt") ? "yes" : "no"); exit }' "$R/test/java/divergences.edn")" "yes"
# the planner's rules (test/gate/plan.ae's expand and matching: groups in order, the last matching rule decides)
py=$(cat <<'EOF'
import re, sys
s = open(sys.argv[1]).read()
s = re.sub(r';[^\n]*', '', s)
def vec_at(i):  # the vector text starting at s[i] == '['
    d = 0
    for j in range(i, len(s)):
        if s[j] == '[': d += 1
        elif s[j] == ']':
            d -= 1
            if d == 0: return s[i + 1:j]
groups = {}
for m in re.finditer(r'\n\s*:([\w-]+) \[', s):
    groups.setdefault(m.group(1), vec_at(m.end() - 1))
def expand(v):
    out = []
    for t in re.findall(r'"[^"]*"|:[\w-]+', v):
        if t.startswith(':'): out += expand(groups[t[1:]])
        else:
            t = t[1:-1]
            out.append((False, t[1:]) if t.startswith('!') else (True, t))
    return out
def match(pat, p):
    if pat.endswith('/'): return p.startswith(pat)
    if '*' in pat: return re.search('^' + re.escape(pat).replace(r'\*', '[^/]*') + '$', p) is not None
    return p == pat
for m in re.finditer(r'\{:name ([\w.-]+)', s):
    name = m.group(1)
    if not name.startswith('java-'): continue
    end = s.find('{:name', m.end()); blk = s[m.end():end if end > 0 else len(s)]
    k = blk.find(':inputs [')
    if k < 0: continue
    inc = False
    for i, pat in expand(vec_at(m.end() + k + len(':inputs '))):
        if match(pat, 'test/java/timeouts.txt'): inc = i
    print(name, 'yes' if inc else 'no')
EOF
)
keys=$(python3 -c "$py" "$R/test/gate/steps.edn")
for st in java-check java-compiled java-interp java-compiled-full java-held; do
  check "steps.edn: $st keys timeouts.txt" "$(awk -v s=$st '$1 == s { print $2 }' <<< "$keys")" "yes"; done
for st in java-read java-classes java-diagnostics java-print java-forms-verify; do
  check "steps.edn: $st does not key timeouts.txt" "$(awk -v s=$st '$1 == s { print $2 }' <<< "$keys")" "no"; done
# this test is gate-tests' input alone: no Java step keys it (an edit of its pins reruns no Java level)
check "steps.edn: no Java step keys this test" "$(python3 -c "$(sed "s|'test/java/timeouts.txt'|'test/java/timeouts-test.bash'|" <<< "$py")" "$R/test/gate/steps.edn" | awk '$2 == "yes"' | wc -l)" "0"

echo "$((n - fails)) of $n"
if [ "$fails" = 0 ]; then echo PASS; exit 0; else echo FAIL; exit 1; fi
