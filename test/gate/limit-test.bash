#!/bin/bash
# bin/lib/limit.bash's tests (review 075, GTS180): each case is a finding's failure, failing before its fix and
# passing after. Run: test/gate/limit-test.bash [CASE...] (cases: wallcap-log, retry-log, ...; default all).
# Light: the longest case waits 6 s. Exits 1 on a failure.
root=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$root/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/limit-test.XXXXXX")
orphans=()
cleanup() { local p; for p in ${orphans[@]+"${orphans[@]}"}; do kill -9 "$p" 2> "$w/kill.err"; done; rm -rf "$w"; }
trap cleanup EXIT
. "$root/bin/lib/limit.bash"
fail=0; ck() { if [ "$2" = ok ]; then echo "ok   $1"; else echo "FAIL $1: $2"; fail=1; fi; }
want() { [ $# -eq 0 ] && return 0; local c; for c in "${WANT[@]}"; do [ "$c" = "$1" ] && return 0; done; return 1; }
# gone PID: the process has ended, polled up to about 1 s (-k's kill is asynchronous under load); a zombie (state Z in
# /proc/PID/stat, ended and not yet reaped) and a missing /proc entry count as gone
gone() {
  local i st
  for i in $(seq 20); do
    st=; [ -r "/proc/$1/stat" ] && st=$(sed 's/^.*) //' "/proc/$1/stat" 2> "$w/stat.err")
    case $st in ""|Z*) return 0 ;; esac
    sleep 0.05
  done
  return 1
}
WANT=("$@"); sel() { [ ${#WANT[@]} -eq 0 ] || want "$1"; }

if sel wallcap-log; then
  # finding 2: a program that ignores SIGTERM, with ARBACE_CPU_LOG set: the wall cap's -k must still kill it
  # (137, as without the log), no orphan, and the log's row has the five fields
  cat > "$w/ign.sh" <<'S'
echo $$ > "$1"; trap '' TERM
while :; do sleep 0.2; done
S
  export ARBACE_CPU_LOG=$w/cpu.tsv; : > "$ARBACE_CPU_LOG"
  s0=$(date +%s); limited_cpu 30 1 bash "$w/ign.sh" "$w/pid"; st=$?; s1=$(date +%s)
  p=$(cat "$w/pid" 2> "$w/pid.err"); [ -n "$p" ] && orphans+=("$p")
  alive=; [ -n "$p" ] && { gone "$p" || alive=yes; }
  [ "$st" = 137 ] && ck "wallcap-log: a SIGTERM-ignorer is killed by -k (137)" ok || ck "wallcap-log: status" "$st, expected 137"
  [ -z "$alive" ] && ck "wallcap-log: no orphan left" ok || ck "wallcap-log: no orphan left" "pid $p still runs"
  nf=$(awk -F'\t' 'END { print NF }' "$ARBACE_CPU_LOG")
  [ "$nf" = 5 ] && ck "wallcap-log: the row has status, wall, user, sys, command" ok || ck "wallcap-log: the row's fields" "$nf fields: $(tail -1 "$ARBACE_CPU_LOG")"
  unset ARBACE_CPU_LOG
fi

# java-check's run_actions and judge, extracted, with limited stubbed: LIMS gets each attempt's limit, the
# statuses of the attempts come from SEQ (a space-separated list, 0 after its end)
if sel retry-limits || sel attempt-log; then
  eval "$(sed -n '/^run_actions() {/,/^}/p;/^judge() {/,/^}/p' "$root/bin/java-check")"
  run_dirs() { echo "$work/w $work/tmp"; }
  limited() {
    local n; echo "$1" >> "$work/lims"; n=$(wc -l < "$work/lims"); local -a q=($SEQ); return "${q[n-1]:-0}"
  }
  runcase() {  # runcase TIMEOUT SEQ: run_actions on one test whose attempts end with the statuses SEQ
    work=$w/run; rm -rf "$work"; mkdir -p "$work/t"; : > "$work/empty"; : > "$work/lims"
    p=$work/t; id=t1; dir=$work; jdktest=$work; timeout=$1; SEQ=$2; level=interp
    printf 'run\t0\t0\t-\t-\t0\t-\t0\tMain\n' > "$p/plan.tsv"
    run_actions interp exe none "" 2> "$work/stderr"
  }
fi
if sel retry-limits; then
  # finding 4: the quiet retry (JAVA_CHECK_ONCE, at twice the first limit already) is one attempt, not two
  JAVA_CHECK_ONCE=1 runcase 40 "124 124" > "$w/res"; res=$(cat "$w/res"); lims=$(tr '\n' ' ' < "$work/lims")
  [ "$lims" = "40 " ] && ck "retry-limits: the retry is one attempt at 40" ok || ck "retry-limits: attempts" "limits '$lims', expected '40 '"
  runcase 20 "124 124" > "$w/res"; lims=$(tr '\n' ' ' < "$work/lims")
  [ "$lims" = "20 40 " ] && ck "retry-limits: a first run is 20, then 40" ok || ck "retry-limits: first run" "limits '$lims'"
fi
if sel attempt-log; then
  # finding 3: an attempt that timed out and the one after it are logged; a pass after a timeout is said on stderr
  export ARBACE_TIMEOUT_LOG=$w/attempts.tsv; : > "$ARBACE_TIMEOUT_LOG"
  runcase 20 "137 0" > "$w/res"; res=$(cat "$w/res")
  case $res in *pass*) ck "attempt-log: the second attempt's pass stands" ok ;; *) ck "attempt-log: result" "$res" ;; esac
  [ "$(awk -F'\t' '$3 == 1 && $5 == 137 && $6 == "cpu-or-kill" { n++ } END { print n+0 }' "$ARBACE_TIMEOUT_LOG")" = 1 ] \
    && ck "attempt-log: the first attempt's limit and status are kept" ok || ck "attempt-log: the log" "$(cat "$ARBACE_TIMEOUT_LOG" 2>&1)"
  grep -q 'first attempt timed out' "$work/stderr" && ck "attempt-log: a pass that needed the retry is said" ok || ck "attempt-log: stderr" "$(cat "$work/stderr")"
  unset ARBACE_TIMEOUT_LOG
fi
if sel first-timeouts; then
  # finding 3 (goi): the rows a retry replaces are kept, their ids printed
  printf 'a.go\terror\ttimeout (10 s)\nb.go\tpass\t\nc.go\terror\tgc reference: x\n' > "$w/results.tsv"
  keep_first_timeouts "$w/results.tsv" "$w/first.tsv" 2> "$w/first.err"
  [ "$(cut -f1 "$w/first.tsv")" = a.go ] && grep -q 'a.go' "$w/first.err" && ck "first-timeouts: the first timeout rows are kept and named" ok \
    || ck "first-timeouts" "$(cat "$w/first.tsv" "$w/first.err" 2>&1)"
fi
if sel retry-rows; then
  # finding 5: every timeout row goi's check prints is taken to the quiet retry
  printf '%s\n' $'a.go\terror\ttimeout (10 s)' $'b.go\tpass\t' $'c.go\terror\tgc reference: the original timed out' \
    $'d.go\terror\tgc reference: the compile timed out' $'e.go\tunsupported\terrors of gc\'s phases after type checking (go/types reports none; goi\'s check over 10 s)' \
    $'f.go\tunsupported\terrors of gc\'s phases after type checking (go/types reports none)' $'g.go\terror\tstatus 3: x' > "$w/rows.tsv"
  got=$(timeout_row_ids "$w/rows.tsv" 2> "$w/rows.err" | tr '\n' ' ')
  [ "$got" = "a.go c.go d.go e.go " ] && ck "retry-rows: the timeout rows of every path are retried" ok || ck "retry-rows" "got '$got', expected 'a.go c.go d.go e.go '"
fi
if sel gc-reason; then
  # finding 7: nethttp's gc level names a timeout
  [ "$(gc_status_reason 124 30)" = "gc timed out (limit 30 s)" ] && [ "$(gc_status_reason 137 30)" = "gc timed out (limit 30 s)" ] \
    && [ "$(gc_status_reason 3 30)" = "gc status 3" ] && ck "gc-reason: a gc timeout is named, another status is its number" ok \
    || ck "gc-reason" "$(gc_status_reason 124 30) / $(gc_status_reason 3 30)"
fi
if sel cpu-kill; then
  # findings 6 and 10: a CPU-limit kill is 137 (SIGKILL: ulimit -t sets the soft and the hard limit equal, so
  # the kernel sends no SIGXCPU and 152 never arises), with and without the log; a program's own exit 152 is read alike
  limited_cpu 1 20 bash -c 'while :; do :; done'; a=$?
  export ARBACE_CPU_LOG=$w/cpu2.tsv; limited_cpu 1 20 bash -c 'while :; do :; done'; b=$?; unset ARBACE_CPU_LOG
  [ "$a $b" = "137 137" ] && ck "cpu-kill: the CPU limit kills with 137, never 152" ok || ck "cpu-kill" "$a $b"
fi
if sel jdk-limit; then
  # finding 8: the JDK's reference run gets the test's own jtreg timeout (at least 60 s), and record_runs uses it
  [ "$(jdk_run_limit 400)" = 400 ] && [ "$(jdk_run_limit 120)" = 120 ] && [ "$(jdk_run_limit 30)" = 60 ] && [ "$(jdk_run_limit x)" = 60 ] \
    && ck "jdk-limit: max(60, the test's timeout)" ok || ck "jdk-limit" "$(jdk_run_limit 400) $(jdk_run_limit 30)"
  grep -q 'timeout -k 5 "\$(jdk_run_limit "\${x\[1\]}")" java' "$root/bin/java-check" && ck "jdk-limit: record_runs uses it" ok || ck "jdk-limit: record_runs" "still a flat limit"
fi
if sel program-tmo; then
  # finding 9: timeouts.txt applies to every kind of program of goi's check
  printf '# c\nnosplit.go 60\n' > "$w/timeouts.txt"
  [ "$(program_tmo nosplit.go 10 "$w/timeouts.txt")" = 60 ] && [ "$(program_tmo other.go 10 "$w/timeouts.txt")" = 10 ] \
    && ck "program-tmo: the file's line, else the default" ok || ck "program-tmo" "$(program_tmo nosplit.go 10 "$w/timeouts.txt")"
  n=$(grep -c 'tmo=$(program_tmo "$p" "$tmo")' "$root/bin/goi")
  [ "$n" = 4 ] && ck "program-tmo: action_parse, check_dir, check_err and check_err_aot use it" ok || ck "program-tmo: callers" "$n of 4"
fi
if sel log-reset; then
  # review 080 finding 3: the default log is emptied at a run's start, a caller's own is kept
  unset ARBACE_TIMEOUT_LOG; echo old > "$w/def.tsv"
  reset_timeout_log "$w/def.tsv"
  [ ! -s "$w/def.tsv" ] && [ "$ARBACE_TIMEOUT_LOG" = "$w/def.tsv" ] && ck "log-reset: the default log starts empty" ok || ck "log-reset: default" "$(cat "$w/def.tsv")"
  echo mine > "$w/mine.tsv"; ARBACE_TIMEOUT_LOG=$w/mine.tsv; reset_timeout_log "$w/def.tsv"
  [ "$(cat "$w/mine.tsv")" = mine ] && ck "log-reset: a caller's log is kept" ok || ck "log-reset: caller's" "$(cat "$w/mine.tsv")"
  unset ARBACE_TIMEOUT_LOG
  # the quiet retry's own pass (JAVA_CHECK_ONCE, attempt 1) is logged and said
  export ARBACE_TIMEOUT_LOG=$w/q.tsv; : > "$w/q.tsv"
  JAVA_CHECK_ONCE=1 note_attempt java-check t9 1 40 0 "pass" 2> "$w/q.err"
  [ "$(awk -F'\t' '$2 == "t9" && $7 == "pass" { n++ } END { print n+0 }' "$w/q.tsv")" = 1 ] && grep -q t9 "$w/q.err" && ck "log-reset: the quiet retry's pass is logged" ok || ck "log-reset: retry pass" "$(cat "$w/q.tsv" "$w/q.err")"
  : > "$w/q.tsv"; note_attempt java-check t8 1 20 0 pass 2> "$w/q.err"
  [ ! -s "$w/q.tsv" ] && ck "log-reset: a first-attempt pass is not logged" ok || ck "log-reset: first pass" "$(cat "$w/q.tsv")"
  unset ARBACE_TIMEOUT_LOG
  # goi empties first-timeouts.tsv before the retry decision
  l1=$(grep -n ': > "$cwork/first-timeouts.tsv"' "$root/bin/goi" | head -1 | cut -d: -f1); l2=$(grep -n 'if \[ -s "$cwork/timeouts.txt" \]' "$root/bin/goi" | head -1 | cut -d: -f1)
  [ -n "$l1" ] && [ -n "$l2" ] && [ "$l1" -lt "$l2" ] && ck "log-reset: goi empties first-timeouts.tsv at every run" ok || ck "log-reset: goi" "lines '$l1' '$l2'"
fi
exit $fail
