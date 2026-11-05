#!/bin/bash
# GT9's tooling (doc/GATE-SURVEY.md, "As built: GT9 (write-ahead)"): the hosts' identities from test/gate/steps.edn's
# :host-ids (bin/gate's gate_host_id; :gcc's recipe gives gate_gcc_id's identity, so moving :gcc into the table rekeys nothing),
# a fabricated host's identity following its executable, version, files and environment, none without a :require command,
# hosts.edn gaining only the table's hosts a step names (bin/gate's gate_host_ids); the gated crops' pins as bin/gate --pins's
# awk reads them and as test/gate/crops.ae does; GTS216 (a)'s concurrent chains; the GT9 rules (gate.crops check) on a made-up
# table and on steps.edn itself; a crop's fragment spliced into steps.edn (gate.crops splice). Seconds, no build, no gate run.
# needs: exe (an Arbace executable: bin/gate-tests passes it as ARBACE_HOST_EXE)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "crops-test: no executable $exe" >&2; exit 2; }
export ARBACE_HOST_EXE=$exe
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/crops-test.XXXXXX")
trap 'rm -rf "$w"' EXIT
fail=0
ok() { echo "ok   $1"; }
bad() { echo "FAIL $1"; fail=1; }
root=$src
. "$src/bin/lib/tools.bash"
crops() { ARBACE_PATH=$src/test "$exe" -m gate.crops "$@"; }
# bin/gate's GT9 functions (between its markers) and the runner's gate_gcc_id
eval "$(sed -n '/^# --- gt9-hosts begin/,/^# --- gt9-hosts end/p' "$src/bin/gate")"
eval "$(sed -n '/^gate_crop_pins_list() {/,/^}/p' "$src/bin/gate")"
eval "$(sed -n '/^gate_gcc_id() {/,/^}/p' "$src/bin/lib/gate-run.bash")"
type gate_host_id > "$w/type" 2>&1 && type gate_host_ids >> "$w/type" 2>&1 && type gate_crop_pins_list >> "$w/type" 2>&1 \
  && ok "bin/gate's GT9 functions extracted" || { bad "bin/gate's GT9 functions: $(cat "$w/type")"; exit 1; }

# ---- :gcc's recipe is gate_gcc_id
echo '{}' > "$w/none.edn"
cat > "$w/gcc-table.edn" <<'F'
{:steps [{:name x :phase :essential :cmd "true" :hosts [:gcc]}]}
F
crops host-recipes "$src/test/gate/steps.edn" "$w/none.edn" > "$w/all.tsv" 2>&1 || bad "host-recipes on steps.edn: $(cat "$w/all.tsv")"
# (steps.edn's own steps name :gcc: its recipe is printed when hosts.edn lacks it)
awk -F'\t' '$1 == "gcc" { sub(/^[^\t]*\t/, ""); print }' "$w/all.tsv" > "$w/gcc.tsv"
[ -s "$w/gcc.tsv" ] && ok ":gcc's recipe printed (a step names it, hosts.edn lacks it)" || bad "no :gcc recipe: $(cat "$w/all.tsv")"
a=$(gate_host_id "$w/gcc.tsv"); b=$(gate_gcc_id)
[ "$a" = "$b" ] && ok ":gcc from the table is gate_gcc_id's identity ($(echo "$a" | cut -c1-12))" || bad ":gcc from the table $a, gate_gcc_id $b"

# ---- a fabricated host
mkdir -p "$w/bin" "$w/lib" "$w/root/test"
cat > "$w/bin/fakec" <<'F'
#!/bin/sh
case $1 in --version) echo "fakec 1.0"; echo "second line" ;; -print-prog-name=cc9) echo "$FAKE_PROG" ;; -where) echo "$FAKE_DIR" ;; -v) echo "to stderr" >&2 ;; esac
F
chmod +x "$w/bin/fakec"
echo lib1 > "$w/lib/a.lib"; echo prog > "$w/lib/cc9"
printf '%s  %s\n' x "$w/lib/a.lib" x "$w/lib/gone.h" > "$w/root/test/list.sha256"
export FAKE_PROG=$w/lib/cc9 FAKE_DIR=$w/lib
printf 'require\tfakec\nversion\tfakec\t--version\nversion-any\tfakec\t-v\nlines\tfakec\t--version\nexe\tfakec\nprog\tno cc9\tfakec\t-print-prog-name=cc9\ndir\tfakec\t-where\nfile\t{dir}/a.*\t{dir}/none.lib\ndigest-list\ttest/list.sha256\n' > "$w/fake.tsv"
id() { ( PATH=$w/bin:$PATH; root=$w/root; gate_host_id "$w/fake.tsv" ); }
i1=$(id); i2=$(id)
[ "$i1" = "$i2" ] && [ ${#i1} = 64 ] && ok "a fabricated host's identity, stable ($(echo "$i1" | cut -c1-12))" || bad "fabricated host: $i1 / $i2"
# what the identity is the hash of (the items' lines in order)
want=$( { echo "fakec 1.0"; echo "to stderr"; printf 'fakec 1.0\nsecond line\n'; sha256sum < "$w/bin/fakec" | cut -c1-64
          sha256sum < "$w/lib/cc9" | cut -c1-64; sha256sum < "$w/lib/a.lib" | cut -c1-64; echo "missing $w/lib/none.lib"
          sha256sum "$w/lib/a.lib"; echo "missing $w/lib/gone.h"; } | sha256sum | cut -c1-64)
[ "$i1" = "$want" ] && ok "the identity is the SHA-256 of the items' lines (version, version-any, lines, exe, prog, dir, file, digest-list)" || bad "identity $i1, the items' lines give $want"
echo lib2 > "$w/lib/a.lib"; i3=$(id)
[ "$i3" != "$i1" ] && ok "a :file's change changes it" || bad "a :file's change kept $i3"
FAKE_PROG=$w/lib/nothing; i4=$(id); FAKE_PROG=$w/lib/cc9
[ "$i4" != "$i3" ] && ok ":prog with no file gives its MISSING text (a new identity)" || bad ":prog missing kept the identity"
echo '#' >> "$w/bin/fakec"; i5=$(id)
[ "$i5" != "$i3" ] && ok "the executable's change changes it" || bad "exe change kept $i5"
printf 'require\tfakec\tno-such-command-gt9\nversion\tfakec\t--version\n' > "$w/fake2.tsv"
[ "$( PATH=$w/bin:$PATH; gate_host_id "$w/fake2.tsv")" = none ] && ok "a missing :require command: none" || bad "missing :require not none"
printf 'require\t${FAKE_CMD:-fakec}\nexe\t${FAKE_CMD:-fakec}\n' > "$w/fake3.tsv"
cp "$w/bin/fakec" "$w/bin/otherc"; echo '# other' >> "$w/bin/otherc"
e1=$( PATH=$w/bin:$PATH; unset FAKE_CMD; gate_host_id "$w/fake3.tsv"); e2=$( PATH=$w/bin:$PATH; FAKE_CMD=otherc gate_host_id "$w/fake3.tsv")
[ "$e1" != "$e2" ] && [ "$e1" != none ] && [ "$e2" != none ] && ok "\${NAME:-DEFAULT}: the environment's command, else the default" || bad "\${NAME:-DEFAULT}: $e1 $e2"

# ---- hosts.edn gains the named hosts only
cat > "$w/t1.edn" <<'F'
{:steps [{:name s1 :phase :essential :cmd "true" :hosts [:stage0 :fake]}
         {:name s2 :phase :full :cmd "true" :parts [{:part p :hosts [:fake2]}]}]
 :host-ids {:fake [[:require "fakec"] [:version "fakec" "--version"]]
            :fake2 [[:require "fakec"] [:exe "fakec"]]
            :unused [[:require "fakec"] [:exe "fakec"]]
            :stage0 [[:require "fakec"]]}}
F
mkdir -p "$w/plan"; printf '{:gate "a"\n :stage0 "b"}\n' > "$w/plan/hosts.edn"
( PATH=$w/bin:$PATH GATE_STEPS=$w/t1.edn; export GATE_STEPS; gate_host_ids "$w/plan" ) > "$w/hi.out" 2>&1 || bad "gate_host_ids: $(cat "$w/hi.out")"
ARBACE_PATH=$src/test "$exe" -e "(let [m (read-string (slurp \"$w/plan/hosts.edn\"))] (println (sort (map name (keys m)))) (println (get m :stage0)))" > "$w/hosts.out" 2>&1
[ "$(head -1 "$w/hosts.out")" = "(fake fake2 gate stage0)" ] && ok "hosts.edn gains :fake and :fake2 (a part's), not :unused, and keeps its own :stage0" \
  || bad "hosts.edn: $(cat "$w/hosts.out" "$w/plan/hosts.edn")"
[ "$(sed -n 2p "$w/hosts.out")" = b ] && ok "a host plan_inputs writes is not recomputed from the table" || bad ":stage0 replaced: $(cat "$w/hosts.out")"
printf '{:gate "a"}\n' > "$w/plan/hosts.edn"; cp "$w/plan/hosts.edn" "$w/hosts0.edn"
( GATE_STEPS=$w/gcc-table.edn; export GATE_STEPS; printf '{:steps [{:name x :phase :essential :cmd "true" :hosts [:gate]}] :host-ids {}}\n' > "$w/t0.edn"; GATE_STEPS=$w/t0.edn gate_host_ids "$w/plan" ) > "$w/hi0.out" 2>&1
cmp -s "$w/plan/hosts.edn" "$w/hosts0.edn" && ok "no table host named: hosts.edn untouched (byte for byte)" || bad "hosts.edn changed with no host to add"

# ---- the pins: awk (bin/gate --pins, before stage 0) and gate.crops agree
sed 's/^ :crops {:shared/ :crops {:ocaml [x] :python [y]\n         :cpp []\n         :shared/' "$src/test/gate/steps.edn" > "$w/pins.edn"
gate_crop_pins_list "$w/pins.edn" > "$w/pins-awk.tsv"; crops pins "$w/pins.edn" > "$w/pins-ae.tsv" 2>&1
cmp -s "$w/pins-awk.tsv" "$w/pins-ae.tsv" && [ "$(cut -f1 "$w/pins-awk.tsv" | tr '\n' ' ')" = "ocaml python " ] \
  && ok "the gated crops' pins (ocaml, python; not :cpp's empty entry), awk and gate.crops alike" \
  || { bad "the pins differ or are wrong"; diff "$w/pins-awk.tsv" "$w/pins-ae.tsv"; cat "$w/pins-awk.tsv"; }
gate_crop_pins_list "$src/test/gate/steps.edn" > "$w/pins0.tsv"; crops pins "$src/test/gate/steps.edn" > "$w/pins0-ae.tsv"
[ ! -s "$w/pins0.tsv" ] && [ ! -s "$w/pins0-ae.tsv" ] && ok "steps.edn gates no new crop yet: --pins checks no crop's pins" || bad "steps.edn's crop pins: $(cat "$w/pins0.tsv")"

# ---- GTS216 (a)'s chains
cat > "$w/cc.edn" <<'F'
{:steps [{:name j :phase :full :chain java :cmd "true" :mem-gib 2}
         {:name a1 :phase :full :chain a :cmd "true" :mem-gib 2}
         {:name a2 :phase :full :chain a :cmd "true" :mem-gib 3 :needs [a1 build1]}
         {:name h :phase :full :chain hv :cmd "true" :mem-gib 10 :heavy true}
         {:name big :phase :full :chain big :cmd "true" :mem-gib 2 :parts [{:part p :mem-gib 5}]}
         {:name x :phase :full :chain xc :cmd "true" :mem-gib 1 :needs [j]}
         {:name y :phase :full :chain yc :cmd "true" :mem-gib 1}
         {:name z :phase :full :chain java2 :cmd "true" :mem-gib 1 :needs [y]}
         {:name nm :phase :full :chain nomem :cmd "true"}
         {:name b1 :phase :full :chain b :cmd "true" :mem-gib 4}
         {:name c1 :phase :full :chain c :cmd "true" :mem-gib 3}]
 :crops {:java [j z] :ocaml [a1 a2 h big x y nm b1 c1]}
 :budgets {:crops 10 :new-crops [:ocaml]}
 :night-concurrent {:max-step-gib 4 :max-gib 7}}
F
crops concurrent "$w/cc.edn" > "$w/cc.out" 2>&1
[ "$(tr '\n' ' ' < "$w/cc.out")" = "a b " ] && ok "concurrent chains: a (its needs in the chain or outside :full) and b; not the four's, :heavy, a part above 4 GiB, a need on another chain, one another chain needs, no :mem-gib, nor c past :max-gib" \
  || bad "concurrent chains: $(cat "$w/cc.out")"
crops concurrent "$src/test/gate/steps.edn" > "$w/cc0.out" 2>&1
[ ! -s "$w/cc0.out" ] && ok "steps.edn: no concurrent chain yet (the full phase runs as before)" || bad "steps.edn's chains: $(cat "$w/cc0.out")"

# ---- the rules
mkdir -p "$w/r/test/ocaml"; echo '{}' > "$w/r/test/ocaml/controls.edn"; echo x > "$w/r/test/ocaml/other.edn"
cat > "$w/rules.edn" <<'F'
{:groups {:oc ["test/ocaml/" "!test/ocaml/other.edn"]}
 :steps [{:name e1 :phase :essential :cmd "true" :mem-gib 4 :inputs [:oc]}
         {:name e2 :phase :essential :cmd "true" :mem-gib 2 :heavy true :inputs [:oc]}
         {:name f1 :phase :full :cmd "true" :inputs [:oc]}
         {:name f2 :phase :full :cmd "true" :mem-gib 2 :inputs [:oc]}
         {:name f3 :phase :full :cmd "true" :mem-gib 2 :inputs [:oc]}
         {:name f4 :phase :full :cmd "true" :mem-gib 2 :inputs [:oc]}
         {:name b :phase :build :kind :build :cmd "true"}
         {:name four :phase :full :cmd "true"}]
 :crops {:ocaml [e1 e2 f1 f2 f3 f4 b] :c [four]}
 :budgets {:crops 10 :new-crops [:ocaml :rust] :shares {:rust 0.5 :zz 1}}
 :host-ids {:h1 [[:frob "x"]] :h2 [[:digest-list "test/none.sha256"]] :h3 [[:version]]}
 :crop-pins [[:ocaml "w" "c"] [:java "w" "c"] [:rust "a \"q\"" "c"]]
 :controls {e1 "test/ocaml/controls.edn" e2 {:waived "judges records"} f2 "test/ocaml/other.edn" f3 "test/ocaml/nothing.edn" f4 {:why "x"} gone "x"}}
F
crops check "$w/r" "$w/rules.edn" > "$w/rules.out" 2>&1; st=$?
[ $st = 1 ] && ok "gate.crops check fails on a bad table" || bad "check's status $st"
for p in 'ocaml e1: an essential check of 4 GiB' 'ocaml e2: an essential check :heavy' 'ocaml f1: no :mem-gib' 'ocaml f1: no negative control' \
         'f2: :controls: test/ocaml/other.edn is not among its :inputs' 'f3: :controls: test/ocaml/nothing.edn is no file' 'f4: :controls: a map without :waived' \
         ':controls: gone is no step' ':host-ids :h1: unknown op :frob' ':host-ids :h2: :digest-list test/none.sha256 is no file' ':host-ids :h3: :version without an argument' \
         ':crop-pins :java: crop :java is not in :budgets :new-crops' ':crop-pins :rust: a tab, newline or double quote' ':budgets :shares: :zz is not in :new-crops' \
         'weights sum to 5.5, not :crops 10'; do
  grep -qF -- "$p" "$w/rules.out" && ok "rule: $p" || { bad "rule not found: $p"; }
done
grep -q ' four:' "$w/rules.out" && bad "a step of the four judged by GT9's rules" || ok "the four crops' steps are not GT9's rules' (no control or :mem-gib asked)"
grep -q 'ocaml b: no negative' "$w/rules.out" && bad "a build asked for a control" || ok "a :build step needs no control"
[ $fail = 0 ] || sed 's/^/     /' "$w/rules.out"
crops check "$src" "$src/test/gate/steps.edn" > "$w/real.out" 2>&1 && ok "steps.edn passes GT9's rules" || { bad "steps.edn: GT9's rules"; cat "$w/real.out"; }

# ---- a fragment spliced into steps.edn
cat > "$w/frag.edn" <<'F'
;; notes, left out
;;@ groups
  :gt9-test ["test/gate/"]
;;@ steps
  {:name gt9-test-step :mem-gib 1 :phase :full :chain gt9-test :cmd "true" :hosts [:stage0] :inputs [:gt9-test]}
;;@ crops
         :ocaml [gt9-test-step]
;;@ controls
  gt9-test-step {:waived "a test"}
F
crops splice "$src/test/gate/steps.edn" "$w/frag.edn" > "$w/spliced.edn" 2> "$w/splice.err" && ok "a fragment spliced (the result reads)" || bad "splice: $(cat "$w/splice.err")"
"$exe" -e "(let [t (read-string (slurp \"$w/spliced.edn\"))] (println (:gt9-test (:groups t)) (:name (last (:steps t))) (:ocaml (:crops t)) (get (:controls t) 'gt9-test-step)))" > "$w/sp.out" 2>&1
[ "$(cat "$w/sp.out")" = '[test/gate/] gt9-test-step [gt9-test-step] {:waived a test}' ] && ok "its groups, step, crop and control in place" || bad "spliced: $(cat "$w/sp.out")"
grep -q 'notes, left out' "$w/spliced.edn" && bad "the fragment's notes spliced" || ok "the fragment's notes left out"
[ "$(grep -c ';;@ ' "$w/spliced.edn")" = 4 ] && ok "the markers kept (a second crop splices after)" || bad "markers: $(grep -c ';;@ ' "$w/spliced.edn")"
printf ';;@ nowhere\n x\n' > "$w/frag2.edn"
crops splice "$src/test/gate/steps.edn" "$w/frag2.edn" > "$w/sp2.out" 2>&1 && bad "a section with no marker spliced" || ok "a section with no marker refused"

[ $fail = 0 ] && echo "crops-test: ok" || echo "crops-test: FAILED"
exit $fail
