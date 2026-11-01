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

# finding 6: a program with an environment runs under OI_TIMEOUT (1 s: the stdlib's load alone takes longer), as a row "timeout"
out=$(cd "$root" && OI_TIMEOUT=1 O2J_LOAD_ALLOWANCE=0 bin/oi check --aot --only 'tool-ocamltest-var-expansion/subst1' 2>&1 | grep -a 'subst1'); st=$?
expect "aot: a program with an environment is under the time limit" 1 'timeout 1s' "$out" $st

# finding 7 (and 2): a known entry is kept only for the kind of failure it was made for; a timeout is a speed class only for the listed programs
k=$W/known; mk "$k"
printf '[{:id :gc :match [#"^fail known gc: "]} {:id :speed :match [#"^slow timeout "]}]\n' > "$k/classes.edn"
printf '[\n ["a" "fail" "known gc: a finaliser (output differs (1 bytes, ocamlc 1))"]\n ["b" "pass" ""]\n ["c" "pass" ""]\n]\n' > "$k/oi-results.edn"
printf '[\n ["a" "fail" "known gc: a finaliser (output differs (1 bytes, ocamlopt 1))" "differs"]\n ["b" "pass" "" "same"]\n ["c" "pass" "" "same"]\n]\n' > "$k/aot-results.edn"
printf '[["a" "gc: a finaliser" "output differs"]]\n' > "$k/oi-known.edn"; cp "$k/oi-known.edn" "$k/aot-known.edn"
printf 'a\t0\tx\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$k/check.tsv"
out=$(judge_oi judge "$k/check.tsv" "$k/corpus.tsv" "$k/refs.edn" "$k/oi-results.edn" "$k/classes.edn"); expect "oi: a known program failing as recorded stays known" 0 'oi: 2 pass, 0 failing' "$out" $?
printf 'a\t125\tx\t1\t1.0\tcrash 7\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$k/check.tsv"
out=$(judge_oi judge "$k/check.tsv" "$k/corpus.tsv" "$k/refs.edn" "$k/oi-results.edn" "$k/classes.edn"); expect "oi: a known program that now crashes is failing" 1 'FAILING a: fail crash 7' "$out" $?
out=$(judge_aot judge "$k/check.tsv" "$k/corpus.tsv" "$k/refs.edn" "$k/oi.tsv" "$k/aot-results.edn" "$k/classes.edn"); expect "aot: a known program that now crashes is failing" 1 'FAILING a: fail crash 7' "$out" $?
printf 'a\t0\tx\t1\t1.0\t\nb\t124\tx\t0\t120\ttimeout 120s\nc\t0\th\t1\t1.0\t\n' > "$k/check.tsv"
printf '[\n ["a" "fail" "known gc: a finaliser (output differs (1 bytes, ocamlopt 1))" "differs"]\n ["b" "fail" "timeout 120s" "differs"]\n ["c" "pass" "" "same"]\n]\n' > "$k/aot-results.edn"
out=$(judge_aot judge "$k/check.tsv" "$k/corpus.tsv" "$k/refs.edn" "$k/oi.tsv" "$k/aot-results.edn" "$k/classes.edn"); expect "aot: a timeout of a program not listed as slow is failing" 1 'FAILING b: fail timeout 120s' "$out" $?
printf '["b"]\n' > "$k/aot-slow.edn"
printf '[\n ["a" "fail" "known gc: a finaliser (output differs (1 bytes, ocamlopt 1))" "differs"]\n ["b" "slow" "timeout 120s" "differs"]\n ["c" "pass" "" "same"]\n]\n' > "$k/aot-results.edn"
out=$(judge_aot judge "$k/check.tsv" "$k/corpus.tsv" "$k/refs.edn" "$k/oi.tsv" "$k/aot-results.edn" "$k/classes.edn"); expect "aot: a timeout of a listed slow program is the speed class" 0 'classes: .*:speed 1' "$out" $?

# re-review 030's finding 4: a known entry of kind "status " pins the output's digest: the same status with other output is not the known failure
p=$W/pin; mk "$p"
printf '[{:id :gc :match [#"^fail known gc: "]}]\n' > "$p/classes.edn"
printf '[\n ["a" "fail" "known gc: x (status 2, ocamlc 0)"]\n ["b" "pass" ""]\n ["c" "pass" ""]\n]\n' > "$p/oi-results.edn"
printf '[\n ["a" "fail" "known gc: x (status 2, ocamlopt 0)" "same"]\n ["b" "pass" "" "same"]\n ["c" "pass" "" "same"]\n]\n' > "$p/aot-results.edn"
printf '[["a" "gc: x" "status " "abc"]]\n' > "$p/oi-known.edn"; cp "$p/oi-known.edn" "$p/aot-known.edn"
printf 'a\t2\tabc123\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$p/check.tsv"; cp "$p/check.tsv" "$p/oi.tsv"
out=$(judge_oi judge "$p/check.tsv" "$p/corpus.tsv" "$p/refs.edn" "$p/oi-results.edn" "$p/classes.edn"); expect "oi: a known status with its digest stays known" 0 'oi: 2 pass, 0 failing' "$out" $?
out=$(judge_aot judge "$p/check.tsv" "$p/corpus.tsv" "$p/refs.edn" "$p/oi.tsv" "$p/aot-results.edn" "$p/classes.edn"); expect "aot: a known status with its digest stays known" 0 'o2j: 2 pass, 0 failing' "$out" $?
printf 'a\t2\tdef456\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$p/check.tsv"; cp "$p/check.tsv" "$p/oi.tsv"
out=$(judge_oi judge "$p/check.tsv" "$p/corpus.tsv" "$p/refs.edn" "$p/oi-results.edn" "$p/classes.edn"); expect "oi: a known status with other output is failing" 1 'FAILING a: fail status 2' "$out" $?
out=$(judge_aot judge "$p/check.tsv" "$p/corpus.tsv" "$p/refs.edn" "$p/oi.tsv" "$p/aot-results.edn" "$p/classes.edn"); expect "aot: a known status with other output is failing" 1 'FAILING a: fail status 2' "$out" $?

# re-review 030's finding 5: both levels timed out: the OI column is "-" (two partial outputs are not compared); the rerun's CPU limit has
# the library's load on top of the program's (bin/oi aot-cpu: the CPU seconds and the wall cap)
q=$W/dbl; mk "$q"
printf '[{:id :speed :match [#"^slow timeout "]}]\n' > "$q/classes.edn"; printf '[]\n' > "$q/aot-known.edn"; printf '["a"]\n' > "$q/aot-slow.edn"
printf '[\n ["a" "slow" "timeout 120s of CPU" "-"]\n ["b" "pass" "" "same"]\n ["c" "pass" "" "same"]\n]\n' > "$q/aot-results.edn"
printf 'a\t124\tx1\t5\t120\ttimeout 120s of CPU\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$q/check.tsv"
printf 'a\t124\ty2\t7\t120\ttimeout 120s\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$q/oi.tsv"
out=$(judge_aot judge "$q/check.tsv" "$q/corpus.tsv" "$q/refs.edn" "$q/oi.tsv" "$q/aot-results.edn" "$q/classes.edn"); expect "aot: a double timeout is not compared with oi's partial output" 0 'o2j: 2 pass' "$out" $?
out=$(cd "$root" && bin/oi aot-cpu); expect "aot: the rerun's CPU limit is the program's and the library's load" 0 '^140 560$' "$out" $?
out=$(cd "$root" && OI_TIMEOUT=60 O2J_LOAD_ALLOWANCE=5 bin/oi aot-cpu); expect "aot: the load allowance is a variable" 0 '^65 260$' "$out" $?

# re-review 030's finding 7 (a): oi's own level runs a program under the CPU limit (limited_cpu), as the AOT level's rerun: a program that needs
# more than OI_TIMEOUT seconds of CPU is a "timeout 1s" row, and the limit is the project's (ARBACE_CPU_LOG records the run)
rm -f "$W/cpu.log"
out=$(cd "$root" && OI_TIMEOUT=1 ARBACE_CPU_LOG=$W/cpu.log bin/oi check --only 'misc/taku' 2>&1; cut -f2,6 .tmp/oi/check.tsv; cut -f1 "$W/cpu.log")
expect "oi: a program over the CPU limit is a timeout row" 0 '124.timeout 1s' "$out" $?
echo "$out" | grep -Eq '^(137|152)$' || { echo "FAIL oi: the program did not run under limited_cpu (no line in ARBACE_CPU_LOG)"; bad=1; }

# finding 9: bin/oi suite expect is a check: the run against the record, the committed record untouched without --record
only='basic/patmatch_for_multiple'
before=$(sha256sum "$root/test/ocaml/ref-expect.edn")
out=$(cd "$root" && bin/oi suite expect --only "$only" 2>&1); st=$?
expect "expect: a run equal to the record passes" 0 'equals the record' "$out" $st
[ "$before" = "$(sha256sum "$root/test/ocaml/ref-expect.edn")" ] || { echo "FAIL expect: the committed record was rewritten by a plain run"; bad=1; }
sed 's/\("basic\/patmatch_for_multiple.ml" \)"pass"/\1"diff"/; s/\("tests\/basic\/patmatch_for_multiple.ml" \)"pass"/\1"diff"/' "$root/test/ocaml/ref-expect.edn" > "$W/ref-expect.tampered"
cmp -s "$W/ref-expect.tampered" "$root/test/ocaml/ref-expect.edn" && { echo "FAIL expect: the test's tampering did not apply"; bad=1; }
out=$(cd "$root" && OI_EXPECT_RECORD=$W/ref-expect.tampered bin/oi suite expect --only "$only" 2>&1); st=$?
expect "expect: a run that differs from the record fails" 1 'not in the record' "$out" $st

# re-review 030's finding 1: a whole run equals the committed record (rows), and a header edit alone is not a difference
out=$(cd "$root" && bin/oi suite expect 2>&1); st=$?
expect "expect: a whole run passes against the committed record" 0 'equals the record' "$out" $st
{ echo ';; another header'; grep -v '^;;' "$root/test/ocaml/ref-expect.edn"; } > "$W/ref-expect.header"
out=$(cd "$root" && OI_EXPECT_RECORD=$W/ref-expect.header bin/oi suite expect 2>&1); st=$?
expect "expect: a header edit alone is not a difference" 0 'equals the record' "$out" $st

# re-review 030's finding 6: a missing record is an error, an --only that matches nothing says so
out=$(cd "$root" && OI_EXPECT_RECORD=$W/no-such-record bin/oi suite expect --only "$only" 2>&1); st=$?
expect "expect: a missing record is an error" 2 'cannot be read' "$out" $st
out=$(cd "$root" && bin/oi suite expect --only 'no-such-directory/zzz' 2>&1); st=$?
expect "expect: an --only that matches nothing says so" 2 'no test selected' "$out" $st

# finding 2: a listed slow program recorded as slow that passes this time (the CPU limit's boundary) is not a change
printf 'a\t0\th\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$k/check.tsv"
printf '[\n ["a" "fail" "known gc: a finaliser (output differs (1 bytes, ocamlopt 1))" "differs"]\n ["b" "slow" "timeout 120s" "same"]\n ["c" "pass" "" "same"]\n]\n' > "$k/aot-results.edn"
printf 'a\t0\tx\t1\t1.0\t\nb\t0\th\t1\t1.0\t\nc\t0\th\t1\t1.0\t\n' > "$k/check.tsv"
out=$(judge_aot judge "$k/check.tsv" "$k/corpus.tsv" "$k/refs.edn" "$k/oi.tsv" "$k/aot-results.edn" "$k/classes.edn"); expect "aot: a slow program that passes at the boundary is not a change" 0 'STALE b' "$out" $?
exit $bad

# re-review 042's finding 1: oi's wall cap is four times its CPU limit (as o2j_cpu), and 137 (SIGKILL) is a timeout only when the run lasted
# OI_TIMEOUT seconds: an OOM kill earlier stays a failure
out=$(cd "$root" && OI_TIMEOUT=120 bin/oi oi-limits 2>&1); expect "oi: the wall cap is four times the CPU limit" 0 '^120 480$' "$out" $?
out=$(cd "$root" && OI_TIMEOUT=120 bin/oi oi-status 137 5 2>&1); expect "oi: 137 after 5 s is an OOM kill, not a timeout" 0 '^137$' "$out" $?
out=$(cd "$root" && OI_TIMEOUT=120 bin/oi oi-status 137 121.5 2>&1); expect "oi: 137 after the CPU limit is a timeout" 0 '^124$' "$out" $?
out=$(cd "$root" && OI_TIMEOUT=120 bin/oi oi-status 152 3 2>&1); expect "oi: 152 (SIGXCPU) is a timeout" 0 '^124$' "$out" $?
out=$(cd "$root" && OI_TIMEOUT=120 bin/oi oi-status 2 3 2>&1); expect "oi: another status is kept" 0 '^2$' "$out" $?
