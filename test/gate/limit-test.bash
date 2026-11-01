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
  alive=; [ -n "$p" ] && [ -d "/proc/$p" ] && alive=yes
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
exit $fail
