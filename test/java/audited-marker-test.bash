#!/bin/bash
# A light test of the audited rule's plumbing (GT7 rerun 8, 07194e71; queue 088): no Arbace executable,
# no stage 0, seconds. It runs copies of the code, extracted from the files at test time (so the test
# follows them):
#   A. bin/java-check --save's marker, mark_audited (audit-070: GATE_AUDITED=1 and TracerPid, both): A1-A4
#      the function as is, untraced and under strace -f (the gate's audit flags: --seccomp-bpf and its
#      trace set), with and without GATE_AUDITED, a stale marker removed; A5-A8 with /proc/self/status
#      replaced by a fabricated status file (TracerPid non-zero, zero, a missing file), under set -euo
#      pipefail as bin/java-check is.
#   B. judge_saved's union of the markers beside its results files into --audited (jc stubbed: its
#      arguments recorded).
#   C. test/java/check.ae's audit-held? (its three bindings, run by joker with joker.string for
#      clojure.string) on fabricated results and classes, and the PRE-TAG step's grep of the printed
#      "held (an :audited class, ...)" lines (both of check.ae's forms, the literal taken from check.ae).
#   D. audit-070's answers to 088's findings 1-3 as data: java-judge's :uses, :audit-timeout's keys,
#      levels-known.edn, check.ae's refusal of --record with --audited (queue 093).
#   E. check()'s head (its options) run alone: --record refused under a tracer unless --save (queue 093).
#   F. the rule's documentation: :audit-timeout's :rule and check.ae's comment, interp only (queue 093).
#   G. java-judge's :summary over a fabricated judgment's log: the held lines kept (queue 093).
# Usage: bash test/java/audited-marker-test.bash   (AUDITED_TEST_REV=REV: the files at a git revision of
# this checkout instead of its working tree, e.g. 07194e71 to see it fail before audit-070). Needs
# bash, awk, strace (A1-A3 skipped without it, or when this shell is traced already), joker (C skipped
# without it). PASS/FAIL, exit 0/1.
set -u
src=$(cd "$(dirname "$0")/../.." && pwd)
rev=${AUDITED_TEST_REV:-}
mkdir -p "${TMPDIR:-$src/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$src/.tmp/tmp}/audited-marker-test.XXXXXX") || exit 2
trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
skip() { echo "skip $*"; }
file() { if [ -n "$rev" ]; then git -C "$src" show "$rev:$1"; else cat "$src/$1"; fi; }
file bin/java-check > "$t/java-check" && file test/java/check.ae > "$t/check.ae" || { echo "FAIL: no sources"; exit 1; }

# ---- A: the marker (mark_audited, audit-070: GATE_AUDITED=1 and a tracer, both) ---------------------
awk '/^mark_audited\(\) \{/ { on = 1 } on { print } on && /^\}$/ { exit }' "$t/java-check" > "$t/marker.body"
if [ ! -s "$t/marker.body" ]; then
  no "A: no mark_audited() in bin/java-check"
else
  # mark DIR LEVELS [STATUS-FILE]: the function run alone, set -euo pipefail as bin/java-check; GATE_AUDITED
  # from the caller's environment (env GATE_AUDITED=1 or env -u GATE_AUDITED)
  { echo 'set -euo pipefail'; cat "$t/marker.body"; echo 'mark_audited "$1" "$2" "${3:-}"'; echo 'echo done'; } > "$t/mark.bash"
  sed 's|/proc/self/status|"$3"|' "$t/mark.bash" > "$t/mark-stub.bash"
  traced_now=$(awk '$1 == "TracerPid:" { print $2 }' /proc/self/status)
  strace_args=(-f -qq -y -s 4096 --seccomp-bpf -e trace=open,openat,openat2,execve,execveat,readlink,readlinkat,chdir,clone,clone3,fork,vfork -o /dev/null)
  ga=(env GATE_AUDITED=1) gu=(env -u GATE_AUDITED)
  if ! command -v strace > /dev/null; then skip "A1-A4: no strace"
  elif [ "${traced_now:-0}" != 0 ]; then skip "A1-A4: this shell is traced already (TracerPid $traced_now)"
  else
    d=$t/a1; mkdir -p "$d"; echo interp > "$d/audited"
    out=$("${gu[@]}" bash "$t/mark.bash" "$d" interp 2>&1)
    if [ "$out" = done ] && [ ! -e "$d/audited" ]; then ok "A1 untraced: no marker, a stale one removed"; else no "A1 untraced (out: $out; marker: $(cat "$d/audited" 2>&1))"; fi
    d=$t/a2; mkdir -p "$d"
    out=$("${ga[@]}" strace "${strace_args[@]}" bash "$t/mark.bash" "$d" read,interp 2>&1)
    if [ "$out" = done ] && [ "$(cat "$d/audited" 2>/dev/null)" = read,interp ]; then ok "A2 GATE_AUDITED=1 under strace -f --seccomp-bpf: marker = the levels"; else no "A2 traced (out: $out; marker: $(cat "$d/audited" 2>&1))"; fi
    d=$t/a3; mkdir -p "$d"; echo stale > "$d/audited"
    out=$("${ga[@]}" strace "${strace_args[@]}" bash -c 'bash "$0" "$1" interp' "$t/mark.bash" "$d" 2>&1)
    if [ "$out" = done ] && [ "$(cat "$d/audited" 2>/dev/null)" = interp ]; then ok "A3 traced grandchild: the stale marker replaced"; else no "A3 traced grandchild (out: $out; marker: $(cat "$d/audited" 2>&1))"; fi
    d=$t/a4; mkdir -p "$d"; echo stale > "$d/audited"
    out=$("${gu[@]}" strace "${strace_args[@]}" bash "$t/mark.bash" "$d" interp 2>&1)
    if [ "${out##*$'\n'}" = done ] && [ ! -e "$d/audited" ] && grep -q 'GATE_AUDITED is not 1' <<< "$out"; then ok "A4 strace without GATE_AUDITED (088 finding 4): not marked, said, stale removed"; else no "A4 traced, not the gate (out: $out; marker: $(cat "$d/audited" 2>&1))"; fi
  fi
  printf 'Name:\tbash\nTracerPid:\t4242\n' > "$t/st-traced"; printf 'Name:\tbash\nTracerPid:\t0\n' > "$t/st-plain"
  d=$t/a5; mkdir -p "$d"
  out=$("${ga[@]}" bash "$t/mark-stub.bash" "$d" interp "$t/st-traced" 2>&1)
  if [ "$out" = done ] && [ "$(cat "$d/audited" 2>/dev/null)" = interp ]; then ok "A5 stub TracerPid 4242, GATE_AUDITED=1: marked"; else no "A5 stub traced (out: $out)"; fi
  d=$t/a6; mkdir -p "$d"
  out=$("${gu[@]}" bash "$t/mark-stub.bash" "$d" interp "$t/st-traced" 2>/dev/null)
  if [ "$out" = done ] && [ ! -e "$d/audited" ]; then ok "A6 stub TracerPid 4242, no GATE_AUDITED (gdb, a person's strace): not marked"; else no "A6 stub traced, not the gate (out: $out)"; fi
  d=$t/a7; mkdir -p "$d"; echo interp > "$d/audited"
  out=$("${ga[@]}" bash "$t/mark-stub.bash" "$d" interp "$t/st-plain" 2>&1)
  if [ "${out##*$'\n'}" = done ] && [ ! -e "$d/audited" ] && grep -q 'no tracer is attached' <<< "$out"; then ok "A7 stub TracerPid 0, GATE_AUDITED=1: not marked, said, stale removed"; else no "A7 stub plain (out: $out)"; fi
  d=$t/a8; mkdir -p "$d"
  out=$("${ga[@]}" bash "$t/mark-stub.bash" "$d" interp "$t/no-such-status" 2>/dev/null)
  if [ "$out" = done ] && [ ! -e "$d/audited" ]; then ok "A8 no status file (no /proc): not marked, set -e not tripped"; else no "A8 missing status (out: $out)"; fi
fi

# ---- B: the union in judge_saved -----------------------------------------------------------------
awk '/^judge_saved\(\) \{/ { on = 1 } on { print } on && /^\}$/ { exit }' "$t/java-check" > "$t/judge.body"
if ! grep -q 'audited' "$t/judge.body"; then
  no "B: judge_saved reads no audited marker"
else
  # judge FILES...: judge_saved with jc stubbed; jc's arguments one a line in $t/jc.args, status in $st
  cat > "$t/judge.bash" <<EOF
set -euo pipefail
root=$t/root ref=REF work=$t/work
jc() { printf '%s\n' "\$@" > "$t/jc.args"; }
$(cat "$t/judge.body")
judge_saved "\$@"
EOF
  judge() { rm -f "$t/jc.args"; bash "$t/judge.bash" "$@" > "$t/judge.out" 2>&1; st=$?; }
  aud() { awk 'p { print; exit } $0 == "--audited" { p = 1 }' "$t/jc.args" 2>/dev/null; }
  res() { mkdir -p "$t/$1"; printf '%s\t%s\tpass\t\n' t00001 "$2" > "$t/$1/results.tsv"; }
  res i interp; res c compiled; res r read; res i2 interp
  judge "$t/i/results.tsv" "$t/c/results.tsv"
  if [ $st = 0 ] && [ -f "$t/jc.args" ] && [ -z "$(aud)" ] && ! grep -q -- --audited "$t/jc.args"; then ok "B1 no marker: no --audited"; else no "B1 no marker (status $st, --audited $(aud))"; fi
  echo interp > "$t/i/audited"
  judge "$t/i/results.tsv" "$t/c/results.tsv"
  if [ $st = 0 ] && [ "$(aud)" = interp ] && grep -q 'the levels interp ran under the audit' "$t/judge.out"; then ok "B2 one marked dir: --audited interp"; else no "B2 (status $st, --audited '$(aud)')"; fi
  echo read,interp > "$t/r/audited"
  judge "$t/r/results.tsv" "$t/i/results.tsv" "$t/c/results.tsv"
  if [ $st = 0 ] && [ "$(aud)" = interp,read ]; then ok "B3 union of two markers, sorted and unique: interp,read"; else no "B3 (status $st, --audited '$(aud)')"; fi
  cp "$t/c/results.tsv" "$t/i/results-c.tsv"
  judge "$t/i/results.tsv" "$t/i/results-c.tsv"
  if [ $st = 0 ] && [ "$(aud)" = interp ]; then ok "B4 two results files beside one marker: interp once"; else no "B4 (status $st, --audited '$(aud)')"; fi
  rm -f "$t/i/audited" "$t/r/audited"; mkdir "$t/c/audited"
  judge "$t/i/results.tsv" "$t/c/results.tsv"
  if [ $st = 0 ] && [ -z "$(aud)" ]; then ok "B5 a directory named audited: ignored, set -e not tripped"; else no "B5 (status $st, --audited '$(aud)')"; fi
  rmdir "$t/c/audited"
  # the union is by file (088 finding 5, queue 093): one unmarked interp file makes interp unaudited
  echo interp > "$t/i/audited"
  judge "$t/i/results.tsv" "$t/i2/results.tsv" "$t/c/results.tsv"
  if [ $st = 0 ] && [ -z "$(aud)" ] && grep -q 'the levels interp have marked and unmarked results files' "$t/judge.out"; then ok "B6 a marked and an unmarked interp file: interp not audited, said (finding 5)"; else no "B6 a marked and an unmarked interp file (status $st, --audited '$(aud)')"; fi
  echo interp > "$t/i2/audited"
  judge "$t/i/results.tsv" "$t/i2/results.tsv" "$t/c/results.tsv"
  if [ $st = 0 ] && [ "$(aud)" = interp ]; then ok "B8 two marked interp files: interp audited"; else no "B8 (status $st, --audited '$(aud)')"; fi
  rm -f "$t/i2/audited"
  # a marker naming a level its file has no rows at does not make another file's rows audited
  echo interp > "$t/c/audited"
  judge "$t/c/results.tsv" "$t/i2/results.tsv"
  if [ $st = 0 ] && [ -z "$(aud)" ]; then ok "B9 a marker for interp beside compiled rows only: the unmarked interp file not audited"; else no "B9 (status $st, --audited '$(aud)')"; fi
  rm -f "$t/c/audited"
  judge "$t/i/results.tsv" "$t/missing/results.tsv"
  if [ $st = 1 ] && [ ! -f "$t/jc.args" ]; then ok "B7 a missing results file: status 1 before the union"; else no "B7 (status $st)"; fi
fi

# ---- C: audit-held? and the PRE-TAG grep ----------------------------------------------------------
lit='held (an :audited class, the run under the audit): '
nlit=$(grep -cF "$lit" "$t/check.ae")
if [ "$nlit" = 2 ]; then ok "C0 check.ae prints the held line in two places"; else no "C0 check.ae's held literal $nlit times, not 2"; fi
awk '/^ *audited \(set / { on = 1 } /^ *problems \(atom/ { exit } on { print }' "$t/check.ae" > "$t/held.body"
if ! command -v joker > /dev/null; then skip "C1-C11: no joker"
elif ! grep -q 'audit-held? (fn' "$t/held.body"; then no "C: no audit-held? binding in check.ae's report"
else
  cat > "$t/held.clj" <<EOF
(require '[joker.string :as str])
;; a copy of test/java/check.ae's report bindings audited, compiled-pass, audit-held? (extracted)
(defn held-fn [aud results classes]
  (let [o (if aud {"--audited" aud} {})
$(cat "$t/held.body")]
    audit-held?))
(def cls [{:id :speed :match [#"^timeout "]}
          {:id :audit-timeout :level :interp :unstable true :audited true :tests ["t03235"]}])
(def rs [["t03686" :interp :timeout "20 s, then 40 s"] ["t03686" :compiled :pass ""]
         ["t04819" :interp :timeout ""] ["t04819" :compiled :flaky ""]
         ["t09001" :interp :timeout ""] ["t09001" :compiled :fail "x"]
         ["t09002" :interp :timeout ""]
         ["t09003" :interp :differ "stdout"] ["t09003" :compiled :pass ""]])
(defn check [name want got] (println (str (if (= (boolean want) (boolean got)) "ok   " "FAIL ") name)))
(let [h (held-fn "interp" rs cls)]
  (check "C1 audited interp timeout, compiled pass: held" true (h :interp "t03686" :timeout))
  (check "C2 compiled flaky: held" true (h :interp "t04819" :timeout))
  (check "C3 compiled fail: not held" false (h :interp "t09001" :timeout))
  (check "C4 no compiled result: not held" false (h :interp "t09002" :timeout))
  (check "C5 a differ (not a timeout): not held" false (h :interp "t09003" :differ))
  (check "C6 another level (no :audited class there): not held" false (h :read "t03686" :timeout)))
(check "C7 no --audited: not held" false ((held-fn nil rs cls) :interp "t03686" :timeout))
(check "C8 --audited read only: interp not held" false ((held-fn "read" rs cls) :interp "t03686" :timeout))
(check "C9 no class with :audited: not held" false ((held-fn "interp" rs (map #(dissoc % :audited) cls)) :interp "t03686" :timeout))
(check "C10 --audited ',interp,' (blanks dropped): held" true ((held-fn ",interp," rs cls) :interp "t03686" :timeout))
(check "C11 a timeout of a test with no results at all: not held" false ((held-fn "interp" rs cls) :interp "t07777" :timeout))
EOF
  joker "$t/held.clj" > "$t/held.out" 2>&1 || echo "FAIL joker: $(head -3 "$t/held.out")" >> "$t/held.out"
  while IFS= read -r l; do case $l in "ok   "*) ok "${l#ok   }" ;; "FAIL "*) no "${l#FAIL }" ;; *) echo "     $l" ;; esac; done < "$t/held.out"
fi
# the PRE-TAG grep (.tmp/tag-v2's claude-md.txt.proposed, report 083) over both printed forms
{ echo "java-check interp: 3 tests: 2 timeout, 1 pass"
  echo "   ${lit}t03235 hotspot/jtreg/compiler/arraycopy/TestArrayCopyConjoint.java: timeout 20 s, then 40 s"
  echo "   ${lit}t03686 hotspot/jtreg/compiler/loopopts/TestCountedLoopPhiValue.java: timeout 20 s, then 40 s"
  echo "   ${lit}levels: t03686 hotspot/jtreg/compiler/loopopts/TestCountedLoopPhiValue.java: interp timeout 20 s, then 40 s, compiled pass "
  echo "   regression: t05555 jdk/java/lang/Foo.java: timeout 20 s, then 40 s"; } > "$t/java-judge.log"
got=$(grep -o 'held (an :audited class, the run under the audit): \(levels: \)\{0,1\}t[0-9]*' "$t/java-judge.log" | grep -o 't[0-9]*$' | sort -u | paste -sd' ' -)
if [ "$got" = "t03235 t03686" ]; then ok "C12 the PRE-TAG grep: both forms, t03235 t03686 once each, the regression not"; else no "C12 the PRE-TAG grep gave '$got'"; fi

# ---- D: audit-070's answers to 088's findings 1-3, as data -------------------------------------------
file test/gate/steps.edn > "$t/steps.edn" && file test/java/divergences.edn > "$t/div.edn" && file test/java/levels-known.edn > "$t/lk.edn" || { echo "FAIL: no data files"; exit 1; }
# step NAME: its entry in steps.edn (from {:name NAME to the next {:name)
step() { awk -v n="{:name $1 " 'index($0, n) { on = 1; print; next } on && /\{:name / { exit } on { print }' "$t/steps.edn"; }
step java-judge > "$t/jj.edn"
miss=""
for lv in $(grep -o 'java-{[a-z,-]*}' "$t/jj.edn" | head -1 | sed 's/^java-{//; s/}$//' | tr ',' ' '); do
  grep -qF "\"java-$lv/audited\"" "$t/jj.edn" || miss="$miss java-$lv/audited"
done
if [ -s "$t/jj.edn" ] && [ -z "$miss" ] && grep -q 'java-interp/audited' "$t/jj.edn"; then ok "D1 java-judge's :uses names every producer's audited marker (088 finding 1)"; else no "D1 java-judge's :uses misses:${miss:- the step}"; fi
if step java-check-judge | grep -qF ':uses ["java-check"]'; then ok "D1b java-check-judge's :uses is java-check's whole output, its marker included"; else no "D1b java-check-judge's :uses"; fi
awk '/\{:id :audit-timeout/ { on = 1; print; next } on && /\{:id / { exit } on { print }' "$t/div.edn" > "$t/at.edn"
if [ -s "$t/at.edn" ] && ! grep -qE '^ *:unstable ' "$t/at.edn" && ! grep -qE '^ *:tests ' "$t/at.edn" && grep -qE '^ *:audited true' "$t/at.edn"; then ok "D2 :audit-timeout: :audited, no :unstable, no :tests (088 finding 2)"; else no "D2 :audit-timeout still holds by list (:unstable or :tests)"; fi
inlk=""
for id in $(grep -oE '"t[0-9]+"' "$t/at.edn" | tr -d '"'); do grep -q "\[\"$id\"" "$t/lk.edn" && inlk="$inlk $id"; done
if [ -z "$inlk" ]; then ok "D3 levels-known.edn names none of the class's examples (088 finding 2)"; else no "D3 levels-known.edn still names:$inlk"; fi
if awk '/\(when \(and record\? \(seq audited\)\)/ { g = 1 } g && /System\/exit 2/ { e = 1 } /\(doseq \[lv \(map keyword/ { exit !(g && e) }' "$t/check.ae"; then ok "D4 the report refuses --record with --audited before the levels (088 finding 3)"; else no "D4 no refusal of --record with --audited in check.ae's report"; fi

# ---- E: the run's own --record under a tracer (088 finding 3, the part audit-070 left: no marker reaches it)
# check()'s head (its options, up to the executable's choice) run alone, then "proceed"
awk '/^check\(\) \{/ { on = 1 } on && /# the executable interprets/ { exit } on { print }' "$t/java-check" > "$t/check.body"
if [ ! -s "$t/check.body" ]; then no "E: no check() in bin/java-check"
else
  { echo 'set -euo pipefail'; cat "$t/check.body"; echo '  echo proceed'; echo '}'; echo 'check "$@"'; } > "$t/checkhead.bash"
  ehead() { "$@" > "$t/e.out" 2>&1; est=$?; eout=$(tail -1 "$t/e.out"); }
  ehead bash "$t/checkhead.bash" --levels interp --record
  if [ $est = 0 ] && [ "$eout" = proceed ]; then ok "E1 untraced --record: runs"; else no "E1 untraced --record (status $est: $eout)"; fi
  if ! command -v strace > /dev/null || [ "${traced_now:-0}" != 0 ]; then skip "E2-E4: no strace, or traced already"
  else
    ehead strace "${strace_args[@]}" bash "$t/checkhead.bash" --levels interp --record
    if [ $est = 2 ] && grep -q 'record under a tracer' "$t/e.out"; then ok "E2 traced --record (no --save): refused, status 2"; else no "E2 traced --record ran (status $est: $eout)"; fi
    ehead strace "${strace_args[@]}" bash "$t/checkhead.bash" --levels interp --record --save "$t/esave"
    if [ $est = 0 ] && [ "$eout" = proceed ]; then ok "E3 traced --record --save: runs (--save never records)"; else no "E3 traced --save (status $est: $eout)"; fi
    ehead strace "${strace_args[@]}" bash "$t/checkhead.bash" --levels interp
    if [ $est = 0 ] && [ "$eout" = proceed ]; then ok "E4 traced, no --record: runs"; else no "E4 traced (status $est: $eout)"; fi
  fi
fi

# ---- F: the rule documented interp-only and inert without compiled results (088 finding 6, queue 093)
rule=$(grep -E '^ *:rule ' "$t/at.edn")
if grep -q 'Interp only' <<< "$rule" && grep -q 'java-check-judge' <<< "$rule" && grep -q ':compiled would hold nothing' <<< "$rule"; then ok "F1 :audit-timeout's :rule: interp only, :compiled holds nothing, the essential pair never holds"; else no "F1 :audit-timeout's :rule does not document the rule as interp-only and inert without compiled results"; fi
awk '/^ *;;/ { b = b $0 "\n"; next } /^ *audited \(set / { printf "%s", b; exit } { b = "" }' "$t/check.ae" > "$t/held.doc"
if grep -q 'Interp only' "$t/held.doc" && grep -q 'java-check-judge' "$t/held.doc" && ! grep -q ':unstable holds' "$t/held.doc"; then ok "F2 check.ae's comment on audited: interp only, the essential pair, no stale ':unstable holds'"; else no "F2 check.ae's comment on audited: $(tr '\n' ' ' < "$t/held.doc")"; fi

# ---- G: java-judge's summary shows the held lines (088 finding 7, queue 093) ---------------------------
# the step's :summary [REGEX N] as gate-run.bash's report applies it (grep -E REGEX LOG | tail -N), over a
# judgment's log of six levels, rerun 8's 14 held tests, t03686's levels line and a regression
sre=$(grep -E '^ *:summary ' "$t/jj.edn" | sed -E 's/^ *:summary \["(.*)" ([0-9]+)\]\}?$/\1/')
sn=$(grep -E '^ *:summary ' "$t/jj.edn" | sed -E 's/^ *:summary \[".*" ([0-9]+)\]\}?$/\1/')
held14="t03235 t03314 t03513 t03627 t03628 t03686 t03720 t03744 t03756 t03796 t03797 t03798 t03836 t04819"
{ for lv in read interp classes diagnostics compiled print; do
    echo "java-check $lv: 100 tests: 99 pass, 1 timeout"
    echo "   against check-$lv.edn: 100 as recorded, 0 new or changed, 0 now passing, 0 regressions"
    if [ $lv = interp ]; then for id in $held14; do echo "   ${lit}$id x/Y.java: timeout 20 s, then 40 s"; done; fi
    echo "   now passing: t00001 x/Z.java"
  done
  echo "   ${lit}levels: t03686 hotspot/jtreg/compiler/loopopts/TestCountedLoopPhiValue.java: interp timeout 20 s, then 40 s, compiled pass "
  echo "   regression: t05555 jdk/java/lang/Foo.java: timeout 20 s, then 40 s"; } > "$t/judge-full.log"
grep -E "$sre" "$t/judge-full.log" | tail -"${sn:-0}" > "$t/summary.out"
got=$(grep -o 'held (an :audited class, the run under the audit): \(levels: \)\{0,1\}t[0-9]*' "$t/summary.out" | grep -o 't[0-9]*$' | sort -u | paste -sd' ' -)
if [ "$got" = "$held14" ] && grep -q '^java-check read: ' "$t/summary.out" && grep -q 'held (an :audited class, the run under the audit): levels: t03686' "$t/summary.out" && grep -q 'regression: t05555' "$t/summary.out"; then
  ok "G1 java-judge's :summary keeps the 14 held, the levels line, the first level's line and the regression"
else no "G1 java-judge's :summary (regex '$sre', $sn lines) gave the held '$got'$(grep -q '^java-check read: ' "$t/summary.out" || echo ', no read line')"; fi

echo "audited-marker-test: $((n - bad)) of $n"
if [ "$bad" = 0 ] && [ "$n" -gt 0 ]; then echo PASS; exit 0; else echo FAIL; exit 1; fi
