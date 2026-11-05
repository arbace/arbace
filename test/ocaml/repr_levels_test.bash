#!/bin/bash
# bin/repr-ocaml's checks on synthetic inputs (review 008's finding 8, doc/ocaml/OCAML-SURVEY.md, "As built: review 008's fixes"): each
# level that could pass on nothing, or accept anything on a record, is shown failing. Scratch in .tmp/w/rl/. Run: bash test/ocaml/repr_levels_test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:?ARBACE_HOST_EXE}
OCD=$root/.tmp/ocdump/ocdump
W=$root/.tmp/w/rl; rm -rf "$W"; mkdir -p "$W/o" "$W/c" "$W/p" "$W/l" "$W/d1" "$W/d2"
bad=0
expect() { # NAME WANT-STATUS REGEX OUTPUT STATUS
  if [ "$5" = "$2" ] && echo "$4" | grep -Eq "$3"; then echo "ok   $1"; else echo "FAIL $1: exit $5 (want $2), want /$3/"; echo "$4" | head -4; bad=1; fi
}
tool() { ARBACE_PATH=$root GOMEMLIMIT=1500MiB GOMAXPROCS=4 "$exe" -m arbace.repr.ocaml "$@" 2>&1; }

printf 'let x = 1\n' > "$W/x.ml"
"$OCD" --file x.ml "$W/x.ml" > "$W/o/x.ml.ae"

# floors: the function of bin/repr-ocaml
FLOORS=$W/floors.tsv; eval "$(sed -n '/^floor() {/,/^}/p' "$root/bin/repr-ocaml")"
# with the default corpora's lists it reads (re-review 042's finding 3)
eval "$(grep -E '^DEFAULT_(DUMPS|RT)_CORPORA=' "$root/bin/repr-ocaml")"
RECORD=1 floor cmi-compiled demo 10 > /dev/null
out=$(floor cmi-compiled demo 10); expect "floor: the recorded count passes" 0 '^$' "$out" $?
out=$(floor cmi-compiled demo 9); expect "floor: a drop fails" 1 'below the recorded 10' "$out" $?
# a corpus of the default invocations with no row (re-review 030's finding 2: another corpus is not checked, floors_test's case)
out=$(floor cmi-compiled stdlib 9); expect "floor: no count recorded fails" 1 'no count recorded' "$out" $?

# compare --record: a file the printer did not print is not recorded
echo '{}' > "$W/known.edn"
out=$(tool compare --orig "$W/o" --cand "$W/c" --printed "$W/p" --known "$W/known.edn" --record); st=$?
expect "compare --record: a file not printed is not recorded" 1 'NOT RECORDED' "$out" $st
grep -q 'not-printed' "$W/known.edn" && { echo "FAIL compare --record wrote the not-printed row"; bad=1; }
# compare --record: an unstable print is not recorded
cp "$W/o/x.ml.ae" "$W/c/x.ml.ae"; printf 'let   x=1\n' > "$W/p/x.ml"
out=$(tool compare --orig "$W/o" --cand "$W/c" --printed "$W/p" --known "$W/known.edn" --record); st=$?
expect "compare --record: an unstable print fails" 1 'UNSTABLE' "$out" $st

# full: a layout dump the helper rejects, of a file the round trip lists as a :diff, is not hidden
mkdir -p "$W/src"; cp "$W/x.ml" "$W/src/"
printf '(ocaml/error :lexer "bad layout")\n' > "$W/l/x.ml.ae"
printf '{"x.ml" {:class :diff, :at [] :orig 1 :printed 2}}\n' > "$W/rt.edn"
out=$(tool full --src "$W/src" --parse "$W/o" --layout "$W/l" --out "$W/fo" --forms "$W/ff" --known "$W/fk.edn" --rt-known "$W/rt.edn"); st=$?
expect "full: a layout rejection of a :diff file is not hidden" 1 'UNEXPECTED REJECT' "$out" $st
printf '{"x.ml" {:class :syntax-error, :kind :syntax, :message "Syntax error"}}\n' > "$W/rt.edn"
out=$(tool full --src "$W/src" --parse "$W/o" --layout "$W/l" --out "$W/fo" --forms "$W/ff" --known "$W/fk.edn" --rt-known "$W/rt.edn"); st=$?
expect "full: a rejection the round trip lists as one stays uncounted" 0 'full: 1 files' "$out" $st

# spec-check: an extraction of nothing fails
mkdir -p "$W/empty"
out=$("$exe" "$root/test/ocaml/spec_check.ae" "$W/empty" 2>&1); st=$?
expect "spec-check: no entries is a failure" 1 'no entries extracted' "$out" $st

# dumps: a listed file whose dump is no longer an error is reported
(cd "$W" && "$OCD" --mode lambda --simplify --file x.ml x.ml > d1/x.ml.lambda.ae && cp d1/x.ml.lambda.ae d2/x.ml.lambda.ae)
printf '{:lambda {"x.ml" :shadowed}}\n' > "$W/dk.edn"
out=$(tool dumps --mode lambda --root "$W/d1" --again "$W/d2" --known "$W/dk.edn"); st=$?
expect "dumps: a listed file that no longer errs fails" 1 'no longer an error' "$out" $st
exit $bad
