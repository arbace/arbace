#!/bin/bash
# GT8.4, GT8.7 (doc/GATE-SURVEY.md, "As built: GT8.4, GT8.7 (write-ahead)"): the rotation on a made-up tree, table
# and store. The planner (test/gate/plan.ae) slices a toy corpus (a record of 40 ids, a :subset of 4), names the
# subset and the night's slice as checks of their own, keys each over its list, picks the slice by the store's runs;
# the runner (bin/lib/gate-run.bash's rotation_note) notes runs and moves a failure's files into the subset;
# gate.plan's rotation-status judges the record complete or not. Seconds, no build, no gate run.
# Usage: test/gate/rotation-test.bash
# needs: exe (the planner on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
mkdir -p "${TMPDIR:-$here/.tmp/tmp}"
t=$(mktemp -d "${TMPDIR:-$here/.tmp/tmp}/gate-rotation-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
root=$here; . "$here/bin/lib/tools.bash"
# review 181, finding 2: every note in one second (date +%s frozen, as a fast run's notes are): the order of the
# runs is the store's sequence, never the clock's second
realdate=$(command -v date); mkdir -p "$t/shim"
printf '#!/bin/sh\nif [ "$1" = "+%%s" ]; then echo 1793000000; else exec %s "$@"; fi\n' "$realdate" > "$t/shim/date"; chmod +x "$t/shim/date"
PATH=$t/shim:$PATH
export GATE_NOW=1793000000 # the planner's clock (the night's choice: a slice covered within the period) the same

# the made-up tree: a record of 40 programs of corpus toy (and 3 of another), its corpora file with a :subset of 4
r=$t/root; mkdir -p "$r/test" "$r/bin"
printf '#!/bin/bash\nexit 0\n' > "$r/bin/tool"
{ echo ";; a record"; echo "["
  for i in $(seq 1 40); do echo "[\"toy/p$i.c\" :pass \"\"]"; done
  for i in 1 2 3; do echo "[\"other/q$i.c\" :pass \"\"]"; done
  echo "]"; } > "$r/test/record.edn"
cat > "$r/test/corpora.edn" <<'EOF'
{:corpora [{:id :toy :subset ["p1.c" "p2.c" "p3.c" "p4.c"]}
           {:id :whole}]}
EOF
cat > "$r/test/steps.edn" <<'EOF'
{:groups {}
 :steps
 [{:name toy :phase :full :cmd "bin/tool --corpus {part}" :inputs ["bin/tool"] :hosts []
   :parts [{:part toy} {:part whole}]
   :rotate {:only "--only {list}" :universe "test/record.edn" :subset "test/corpora.edn" :failed-re "^FAIL ([^ ]+).*$"}}
  {:name after-toy :phase :full :cmd "bin/tool" :inputs ["bin/tool"] :hosts [] :needs [toy]}]}
EOF
(cd "$r" && find . -type f -printf '%P\n' | LC_ALL=C sort | xargs -d '\n' sha256sum) > "$t/files.sha256"
cp "$t/files.sha256" "$t/files.base"
echo '{}' > "$t/hosts.edn"
# this commit's ancestors (bin/lib/gate-run.bash's plan_inputs: git rev-list HEAD): c0ffee and its parent
printf 'c0ffee\nbeef00\n' > "$t/ancestors.txt"
store=$t/store; mkdir -p "$store"
export GATE_STEPS=$r/test/steps.edn GATE_CANARY=0
plan() { # plan DIR MODE ARGS...: the planner into DIR
  local d=$1; shift; rm -rf "$d"; mkdir -p "$d"; cp "$t/files.sha256" "$t/hosts.edn" "$t/ancestors.txt" "$d/"
  (cd "$here" && arbace_tool -p "$here/test" -m gate.plan plan "$r" "$d" "$store" "$@") > "$d.out" 2>&1
}
names() { cut -f1 "$1/plan.tsv" | tr '\n' ' '; }
# failed_now: the files whose latest event in the failed file is a failure (the runner appends them in order)
failed_now() { awk -F'\t' '{ e[$3] = $4 } END { for (i in e) if (e[i] == "fail") print i }' "$store/_rotation/toy.toy.failed" 2>"$t/failed.err" | LC_ALL=C sort; }
col() { awk -F'\t' -v n="$2" -v c="$3" '$1 == n {print $c}' "$1/plan.tsv"; }

# 1. --full: whole parts, {only} gone, no list
plan "$t/full" full || no "the full plan: $(cat "$t/full.out")"
if [ "$(names "$t/full")" = "toy.toy toy.whole after-toy " ]; then ok "--full: the parts whole"; else no "--full: $(names "$t/full")"; fi
if [ "$(col "$t/full" toy.toy 18)" = "bin/tool --corpus toy" ]; then ok "--full: the command without --only"; else no "--full's command: $(col "$t/full" toy.toy 18)"; fi

# 2. the slices: a function of the name alone (SHA-256's first 8 hex digits mod 7), every file in one slice
plan "$t/n0" nightly --night 0 || no "the nightly plan: $(cat "$t/n0.out")"
s0=$(names "$t/n0")
case $s0 in "toy.toy.sub toy.toy.s0 toy.whole after-toy ") ok "--nightly, night 0, an empty store: the subset, slice s0, the part with no :subset whole" ;;
  *) no "--nightly night 0: $s0 ($(head -3 "$t/n0.out"))" ;; esac
exp0=$(for i in $(seq 5 40); do id=toy/p$i.c; h=$(printf '%s' "$id" | sha256sum | cut -c1-8); [ $(( 16#$h % 7 )) = 0 ] && echo "$id"; done | LC_ALL=C sort)
if [ "$(cat "$t/n0/rotation/toy.toy.s0.list")" = "$exp0" ] && [ -n "$exp0" ]; then ok "slice s0's list: the ids whose SHA-256 is 0 mod 7 ($(echo "$exp0" | wc -l))"; else no "slice s0's list: $(cat "$t/n0/rotation/toy.toy.s0.list")"; fi
if [ "$(cat "$t/n0/rotation/toy.toy.sub.list")" = "$(printf 'toy/p1.c\ntoy/p2.c\ntoy/p3.c\ntoy/p4.c')" ]; then ok "the subset's list: the :subset, its corpus's ids"; else no "the subset's list: $(cat "$t/n0/rotation/toy.toy.sub.list")"; fi
total=0
for k in 0 1 2 3 4 5 6; do
  plan "$t/k$k" nightly --night "$k" || no "night $k's plan"
  f=$t/k$k/rotation/toy.toy.s$k.list
  [ -f "$f" ] && total=$((total + $(wc -l < "$f")))
done
if [ "$total" = 36 ]; then ok "the seven nights' slices cover the rest, 36 files, once each"; else no "the slices cover $total of 36"; fi
if grep -q 'bin/tool --corpus toy --only "\$GATE_LIST"' "$t/n0/plan.tsv"; then ok "a rotated check's command takes \"\$GATE_LIST\""; else no "the rotated command: $(col "$t/n0" toy.toy.s0 18)"; fi
if [ "$(col "$t/n0" after-toy 6)" = "toy.toy.sub,toy.toy.s0,toy.whole" ]; then ok ":needs naming the step names its rotated checks"; else no ":needs: $(col "$t/n0" after-toy 6)"; fi

# 3. keys: over the list (a line list SHA N); the same list, the same key; another list, another key
if grep -q "^list $(sha256sum < "$t/n0/rotation/toy.toy.s0.list" | cut -c1-64) " "$t/n0/plan/toy.toy.s0.inputs"; then ok "the key's material holds the list's digest"; else no "the material: $(grep ^list "$t/n0/plan/toy.toy.s0.inputs")"; fi
plan "$t/n7" nightly --night 7
k0=$(col "$t/n0" toy.toy.s0 3)
if [ ${#k0} = 64 ] && [ "$k0" = "$(col "$t/n7" toy.toy.s0 3)" ]; then ok "night 7 is night 0's slice, at the same key"; else no "night 7's key"; fi
if [ "$(col "$t/n0" toy.toy.sub 3)" != "$(col "$t/n0" toy.toy.s0 3)" ]; then ok "the subset and the slice keyed apart"; else no "the subset's and the slice's keys equal"; fi
kw=$(col "$t/full" toy.whole 3)
if [ ${#kw} = 64 ] && [ "$kw" = "$(col "$t/n0" toy.whole 3)" ]; then ok "a part with no :subset: --nightly's key is --full's (its passes serve both)"; else no "toy.whole's keys differ"; fi

# 4. the runner: rotation_note's runs, a failure's files into the subset, a subset's pass clearing them
root=$here logs=$t/n0 commit=c0ffee dirty=false
declare -A key
. "$here/bin/lib/gate-run.bash"
key[toy.toy.s0]=$(col "$t/n0" toy.toy.s0 3) key[toy.toy.sub]=$(col "$t/n0" toy.toy.sub 3)
victim=$(head -1 "$t/n0/rotation/toy.toy.s0.list")
printf 'ci judge: ...\nFAIL %s differs\nFAIL toy/p99.c not in the list\n' "$victim" > "$t/n0/toy.toy.s0.log"
rotation_note toy.toy.s0 fail
if [ "$(failed_now)" = "$victim" ]; then ok "a slice's failure: its named file (in its list) joins the subset"; else no "the failed file: $(cat "$store/_rotation/toy.toy.failed" 2>&1)"; fi
if [ "$(cut -f3 "$store/_rotation/toy.toy.s0.runs")" = fail ]; then ok "the run noted (fail)"; else no "the runs: $(cat "$store/_rotation/toy.toy.s0.runs")"; fi
plan "$t/n1" nightly --night 1
if grep -qx "$victim" "$t/n1/rotation/toy.toy.sub.list" && ! grep -qx "$victim" "$t/n1/rotation/toy.toy.s0.list"; then ok "the failed file in the subset's list, out of its slice's"; else no "the failed file's place"; fi
case $(names "$t/n1") in *toy.toy.s0*) ok "night 1 runs s0 again: its latest run failed" ;; *) no "night 1: $(names "$t/n1")" ;; esac
logs=$t/n1; key[toy.toy.sub]=$(col "$t/n1" toy.toy.sub 3)
rotation_note toy.toy.sub pass
if [ -z "$(failed_now)" ]; then ok "the subset's pass clears the failed file"; else no "failed after the subset passed: $(cat "$store/_rotation/toy.toy.failed")"; fi

# 5. completeness (rotation-status): incomplete, then every slice passed within the period, then a stale one
status() { (cd "$here" && GATE_NOW="$1" arbace_tool -p "$here/test" -m gate.plan rotation-status "$r" "$store" --ancestors "$t/ancestors.txt") > "$t/status.out" 2>&1; }
if ! status "$(date +%s)" && grep -q '^INCOMPLETE toy.toy' "$t/status.out"; then ok "rotation-status: incomplete with slices never run"; else no "rotation-status (incomplete): $(cat "$t/status.out")"; fi
logs=$t/n0b; plan "$logs" nightly --night 0
now=$(date +%s)
for k in 0 1 2 3 4 5 6; do
  logs=$t/k$k; [ -f "$logs/rotation/toy.toy.s$k.rot" ] || continue
  plan "$logs" nightly --night "$k"  # (the plan again: the slices' lists without the failed file)
  [ -f "$logs/rotation/toy.toy.s$k.rot" ] || continue
  key[toy.toy.s$k]=$(col "$logs" toy.toy.s$k 3)
  rotation_note "toy.toy.s$k" pass
done
if status "$now" && grep -q '^complete   toy.toy' "$t/status.out"; then ok "rotation-status: complete when every slice passed with its list"; else no "rotation-status (complete): $(cat "$t/status.out")"; fi
if ! status $((now + 8 * 86400)) && grep -q 'not passed within 7 days' "$t/status.out"; then ok "rotation-status: incomplete eight days later (at any key, within the period)"; else no "rotation-status (stale): $(cat "$t/status.out")"; fi
printf '999\t%s\t%s\tfail\tc0ffee\tfalse\n' "$(date +%s)" "$victim" >> "$store/_rotation/toy.toy.failed"
if ! status "$now" && grep -q 'failed since' "$t/status.out"; then ok "rotation-status: incomplete while a file failed since"; else no "rotation-status (failed): $(cat "$t/status.out")"; fi
printf '1000\t%s\t%s\tclear\tc0ffee\tfalse\n' "$(date +%s)" "$victim" >> "$store/_rotation/toy.toy.failed"
# the oldest slice first: every slice ran, s3's run the oldest
awk -F'\t' -v OFS='\t' '{ $8 = 0; print }' "$store/_rotation/toy.toy.s3.runs" > "$t/s3.runs" && cp "$t/s3.runs" "$store/_rotation/toy.toy.s3.runs"
if [ -s "$store/_rotation/toy.toy.s3.runs" ]; then
  plan "$t/n5" nightly --night 5
  case $(names "$t/n5") in *toy.toy.s3*) ok "every slice passed: the night runs the one that ran longest ago (s3)" ;; *) no "the oldest slice: $(names "$t/n5")" ;; esac
fi

# 7. a whole part's run (--full; the clean run with GATE_CLEAN_WHOLE=1) is a run of every slice: noted as
# NAME.PART.whole with the universe as its list, completing the rotation in one night
store=$t/store2; mkdir -p "$store"
plan "$t/w" full || no "the whole plan"
if [ "$(sed -n 's/^slice\t//p' "$t/w/rotation/toy.toy.rot")" = whole ] && [ "$(wc -l < "$t/w/rotation/toy.toy.list")" = 40 ]; then ok "--full: a rotated part's run noted as whole, its list the universe (40)"; else no "the whole part's .rot: $(cat "$t/w/rotation/toy.toy.rot" 2>&1)"; fi
sed 's/^   :rotate .*}}$/}/' "$r/test/steps.edn" > "$t/steps-norot.edn"
GATE_STEPS=$t/steps-norot.edn plan "$t/wn" full
kr=$(col "$t/w" toy.toy 3)
if [ ${#kr} = 64 ] && [ "$kr" = "$(col "$t/wn" toy.toy 3)" ]; then ok "--full: the whole part's key is the one without :rotate"; else no "the whole part's key changed with :rotate: $kr $(col "$t/wn" toy.toy 3)"; fi
logs=$t/w; key[toy.toy]=$kr
rotation_note toy.toy pass
if [ -s "$store/_rotation/toy.toy.whole.runs" ] && [ ! -e "$store/_rotation/toy.toy.s0.runs" ]; then ok "the whole run noted in toy.toy.whole.runs"; else no "the whole run's note: $(ls "$store/_rotation")"; fi
if status "$(date +%s)" && grep -q '^complete   toy.toy' "$t/status.out"; then ok "rotation-status: one whole pass completes every slice"; else no "rotation-status after a whole pass: $(cat "$t/status.out")"; fi
plan "$t/w2" nightly --night 2
case $(names "$t/w2") in *toy.toy.s2*) ok "after a whole pass every slice ran at once: the night's index decides (s2)" ;; *) no "after a whole pass: $(names "$t/w2")" ;; esac
# a slice run that fails after the whole pass: its latest run fails, the rotation is incomplete again
logs=$t/w2; key[toy.toy.s2]=$(col "$t/w2" toy.toy.s2 3)
: > "$t/w2/toy.toy.s2.log"; rotation_note toy.toy.s2 fail
if ! status "$(date +%s)" && grep -q 's2 (.*fail' "$t/status.out"; then ok "a slice's failure after the whole pass: incomplete"; else no "a later slice failure: $(cat "$t/status.out")"; fi
# the whole part again: a failure names its file (into the subset), every slice's latest run a failure; then a pass clears it
logs=$t/w; printf 'FAIL toy/p20.c differs\n' > "$t/w/toy.toy.log"; rotation_note toy.toy fail
if [ "$(failed_now)" = toy/p20.c ]; then ok "a whole part's failure: its named file joins the subset"; else no "the whole failure's file: $(cat "$store/_rotation/toy.toy.failed" 2>&1)"; fi
plan "$t/w3" nightly --night 4
case $(names "$t/w3") in *toy.toy.s4*) ok "after a whole failure every slice's latest run failed: the night's index decides (s4)" ;; *) no "after a whole failure: $(names "$t/w3")" ;; esac
rotation_note toy.toy pass
if [ -z "$(failed_now)" ] && status "$(date +%s)"; then ok "the whole part's pass clears the failed file and completes the rotation"; else no "after the whole pass: $(cat "$store/_rotation/toy.toy.failed" "$t/status.out")"; fi
# the universe changed since (a program added): the whole run covered the old one, so no slice counts as run
cp "$r/test/record.edn" "$t/record.bak"
sed -i 's|^\["toy/p40.c" :pass ""\]$|&\n["toy/p41.c" :pass ""]|' "$r/test/record.edn"
if ! status "$(date +%s)" && grep -q 'never run with this list, nor the part whole with this universe' "$t/status.out"; then ok "a whole run of another universe counts for no slice"; else no "the changed universe: $(cat "$t/status.out")"; fi
cp "$t/record.bak" "$r/test/record.edn"
store=$t/store

# 8. review 181 (the user's (a)): only runs of this commit's ancestors, from clean trees, count: completeness, the
# night's slice and the failed files
store=$t/store3; mkdir -p "$store"
logs=$t/w; key[toy.toy]=$kr
commit=feedface rotation_note toy.toy pass
if ! status "$(date +%s)" && grep -q '^INCOMPLETE toy.toy' "$t/status.out"; then ok "another branch's whole pass does not complete this commit's rotation"; else no "a foreign commit's pass counted: $(cat "$t/status.out")"; fi
dirty=true rotation_note toy.toy pass
if ! status "$(date +%s)"; then ok "a dirty tree's whole pass does not count"; else no "a dirty run counted: $(cat "$t/status.out")"; fi
commit=beef00 rotation_note toy.toy pass
if status "$(date +%s)"; then ok "an ancestor's clean whole pass counts"; else no "an ancestor's pass: $(cat "$t/status.out")"; fi
# a branch's failure is not this commit's failed file; its clearing does not clear this commit's
printf 'FAIL toy/p20.c differs\n' > "$t/w/toy.toy.log"
commit=feedface rotation_note toy.toy fail
plan "$t/b1" nightly --night 1
if ! grep -qx toy/p20.c "$t/b1/rotation/toy.toy.sub.list" && status "$(date +%s)"; then ok "another branch's failure joins neither this commit's subset nor its record"; else no "a foreign failure: $(cat "$t/status.out")"; fi
case $(names "$t/b1") in *toy.toy.s1*) ok "another branch's failed run does not steer this commit's slice (night 1: s1)" ;; *) no "the slice after a foreign failure: $(names "$t/b1")" ;; esac
rotation_note toy.toy fail
commit=feedface rotation_note toy.toy pass
plan "$t/b2" nightly --night 1
if grep -qx toy/p20.c "$t/b2/rotation/toy.toy.sub.list"; then ok "this commit's failure stays in its subset after another branch's pass"; else no "a foreign pass cleared this commit's failure"; fi
store=$t/store

# 9. review 181, findings 3 and 4: the night's choice. Every slice passed first (one note a night)
store=$t/store4; mkdir -p "$store"
for k in 0 1 2 3 4 5 6; do
  plan "$t/c$k" nightly --night "$k"; logs=$t/c$k
  for x in $(names "$logs"); do case $x in toy.toy.s*) key[$x]=$(col "$logs" "$x" 3); rotation_note "$x" pass; last=$x ;; esac; done
done
if status "$(date +%s)"; then ok "seven nights, one slice each: complete"; else no "seven nights: $(cat "$t/status.out")"; fi
# finding 4: a slice that fails naming no file is run again, and the rotation's slice beside it
plan "$t/c7" nightly --night 7; logs=$t/c7
s7=$(names "$logs" | grep -o 'toy\.toy\.s[0-9]' | head -1); key[$s7]=$(col "$logs" "$s7" 3)
echo 'bin/ci check: cirun build failed' > "$logs/$s7.log"; rotation_note "$s7" fail
plan "$t/c8" nightly --night 8
n8=$(names "$t/c8" | grep -o 'toy\.toy\.s[0-9]' | tr '\n' ' ')
case $n8 in "$s7 "?*) ok "a slice failing with no file named runs again, and the rotation's slice beside it ($n8)" ;; *) no "after a nameless failure: $n8" ;; esac
# finding 3: a program added to the slice that ran last: not covered with its list, it runs next, not in a week
logs=$t/c8; for x in $n8; do key[$x]=$(col "$logs" "$x" 3); rotation_note "$x" pass; last=$x; done
i=41; while :; do id=toy/p$i.c; h=$(printf '%s' "$id" | sha256sum | cut -c1-8); [ "s$(( 16#$h % 7 ))" = "${last#toy.toy.}" ] && break; i=$((i + 1)); done
cp "$r/test/record.edn" "$t/record.bak"
sed -i "s|^\[\"toy/p40.c\" :pass \"\"\]\$|&\n[\"$id\" :pass \"\"]|" "$r/test/record.edn"
plan "$t/c9" nightly --night 9
case $(names "$t/c9") in *"$last "*) ok "a slice whose list changed ($id into ${last#toy.toy.}) runs the next night" ;; *) no "after a list change in $last: $(names "$t/c9")" ;; esac
cp "$t/record.bak" "$r/test/record.edn"
store=$t/store

# 6. the clean run: the subsets alone
plan "$t/clean" full --rotation subset
if [ "$(names "$t/clean")" = "toy.toy.sub toy.whole after-toy " ]; then ok "--rotation subset (the clean run): the subset alone"; else no "the clean plan: $(names "$t/clean")"; fi

echo "$((n - bad)) of $n passed"
[ "$bad" = 0 ]
