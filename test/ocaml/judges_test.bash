#!/bin/bash
# The judges of bin/oi (test/ocaml/oi_check.ae, o2j_check.ae) on synthetic inputs (review 008's fixes, doc/ocaml/OCAML-SURVEY.md,
# "As built: review 008's fixes"): small corpora in .tmp/w/judges/, each case the judge's exit status and a line of its output.
#   bin/test-ocaml-judges is not a script of its own: run  bash test/ocaml/judges_test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:?ARBACE_HOST_EXE}
W=$root/.tmp/w/judges; rm -rf "$W"; mkdir -p "$W"
bad=0
t=$(printf '\t')

mk() { # DIR: a corpus of a, b, c; references, runs and records in which all three pass
  local d=$1; mkdir -p "$d"
  printf 'a\trun\t\t\t\t\nb\trun\t\t\t\t\nc\trun\t\t\t\t\n' > "$d/corpus.tsv"
  printf '[{:test "a" :ocamlc {:status "0" :sha256 "h" :bytes 1} :ocamlopt {:status "0" :sha256 "h" :bytes 1}}\n {:test "b" :ocamlc {:status "0" :sha256 "h" :bytes 1} :ocamlopt {:status "0" :sha256 "h" :bytes 1}}\n {:test "c" :ocamlc {:status "0" :sha256 "h" :bytes 1} :ocamlopt {:status "0" :sha256 "h" :bytes 1}}]\n' > "$d/refs.edn"
  printf 'a\t0\th\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$d/check.tsv"
  printf 'a\t0\th\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$d/oi.tsv"
  printf '[]\n' > "$d/classes.edn"
  printf '[\n ["a" "pass" ""]\n ["b" "pass" ""]\n ["c" "pass" ""]\n ["d" "pass" ""]\n]\n' > "$d/oi-results.edn"
  printf '[\n ["a" "pass" "" "same"]\n ["b" "pass" "" "same"]\n ["c" "pass" "" "same"]\n ["d" "pass" "" "same"]\n]\n' > "$d/aot-results.edn"
}
judge_oi() { "$exe" "$root/test/ocaml/oi_check.ae" "$@" 2>&1; }
judge_aot() { "$exe" "$root/test/ocaml/o2j_check.ae" "$@" 2>&1; }
expect() { # NAME WANT-STATUS WANT-LINE-REGEX OUTPUT STATUS
  local name=$1 ws=$2 re=$3 out=$4 st=$5
  if [ "$st" = "$ws" ] && echo "$out" | grep -Eq "$re"; then echo "ok   $name"; else echo "FAIL $name: exit $st (want $ws), want /$re/"; echo "$out" | head -5; bad=1; fi
}

# finding 4: "d" is recorded and no longer in the corpus
d=$W/gone; mk "$d"
out=$(judge_oi judge "$d/check.tsv" "$d/corpus.tsv" "$d/refs.edn" "$d/oi-results.edn" "$d/classes.edn"); expect "oi: a recorded program gone from the corpus is reported" 1 '^GONE d' "$out" $?
cp "$d/oi-results.edn" "$d/oi-rec.edn"
out=$(judge_oi record "$d/check.tsv" "$d/corpus.tsv" "$d/refs.edn" "$d/oi-rec.edn" "$d/classes.edn"); expect "oi: a full record says what it drops" 0 '^DROPPED d' "$out" $?
out=$(judge_oi judge-only "$d/check.tsv" "$d/corpus.tsv" "$d/refs.edn" "$d/oi-results.edn" "$d/classes.edn"); expect "oi: --only does not report the programs outside it" 0 'oi: 3 programs' "$out" $?
out=$(judge_aot judge "$d/check.tsv" "$d/corpus.tsv" "$d/refs.edn" "$d/oi.tsv" "$d/aot-results.edn" "$d/classes.edn"); expect "aot: a recorded program gone from the corpus is reported" 1 '^GONE d' "$out" $?
cp "$d/aot-results.edn" "$d/aot-rec.edn"
out=$(judge_aot record "$d/check.tsv" "$d/corpus.tsv" "$d/refs.edn" "$d/oi.tsv" "$d/aot-rec.edn" "$d/classes.edn"); expect "aot: a full record says what it drops" 0 '^DROPPED d' "$out" $?


# finding 5: the rollup does not judge a roles.tsv left by an earlier tree, header reading or actions.awk
S=$root/.tmp/o7; cp "$S/roles.tsv" "$W/roles.keep" 2>/dev/null
printf 'tests/nothing/stale.ml\ttest\trun\tx\n' > "$S/roles.tsv"
out=$(cd "$root" && bin/oi suite rollup 2>&1); st=$?
expect "rollup: a stale roles.tsv is regenerated, not judged" 0 'testsuite: 0 files without a result or a class' "$out" $st
exit $bad
