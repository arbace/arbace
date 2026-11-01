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
exit $fail
