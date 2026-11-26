#!/bin/bash
# The host's big lock's two slots (bin/heavy-lock, 2026-11-23, doc/GATE-SURVEY.md "As built: the big lock's two slots"), on a
# scratch lock under $TMPDIR/heavy-lock-test (ARBACE_HEAVY_LOCK), never the host's, with sleep jobs and MemAvailable's
# stand-in HEAVY_LOCK_MEMAVAIL_KIB:
#   1. two jobs run at once, a third waits for one of them; --queue names the slots and the waiter;
#   2. the memory rule: a head whose need does not fit beside the running job waits for it, and a later ticket that would
#      fit does not pass it; a job alone needs no memory;
#   3. FIFO: with no room for a second job, five waiters run in their order of arrival;
#   4. a dead ticket and a dead state file are removed by the next waiter, which is not held up;
#   5. plain `flock LOCK CMD`: it holds slot 1, a ticket runs beside it in slot 2 when it fits, and it waits for a ticket in slot 1;
#   6. the one-slot version's waiters (the file at 903b053e4, when git has it): a ticket runs beside an old holder in slot 2;
#      with an old ticket at the head waiting for slot 1, a later ticket takes slot 2, never slot 1, and the old one gets slot 1;
#      an old ticket after a new holder runs when it ends;
#   7. one slot (LOCK.slots 1, the user, 2026-11-25): a big ticket does not run beside a holder, a small one (at most
#      HEAVY_LOCK_SMALL_GIB) does, in slot 2, and a small one behind a big waiter backfills beside the holder.
# Usage: test/gate/heavy-lock-test.bash
# gate-tests: alone (its jobs hold on each other by time)
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}; mkdir -p "$TMPDIR"
T=$TMPDIR/heavy-lock-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
HL=$here/bin/heavy-lock
export HEAVY_LOCK_POLL=0.1
unset ARBACE_HEAVY_HELD ARBACE_HEAVY_CMD HEAVY_LOCK_WAITS HEAVY_MEM_GIB
G=1048576

# a job: NAME's start and end (nanoseconds) and its slot in $T/N/log
cat > "$T/job" <<'EOF'
#!/bin/bash
echo "$1 start $(date +%s%N) ${ARBACE_HEAVY_SLOT:-0} ${ARBACE_HEAVY_HELD:-}" >> "$LOG"
sleep "$2"
echo "$1 end $(date +%s%N)" >> "$LOG"
EOF
chmod +x "$T/job"
at() { awk -v n="$1" -v w="$2" '$1 == n && $2 == w {print $3}' "$LOG"; }
slot() { awk -v n="$1" '$1 == n && $2 == "start" {print $4}' "$LOG"; }
before() { local a b; a=$(at "$1" "$2"); b=$(at "$3" "$4"); [ -n "$a" ] && [ -n "$b" ] && [ "$a" -le "$b" ]; }
case_dir() { D=$T/$1; mkdir -p "$D"; export LOG=$D/log ARBACE_HEAVY_LOCK=$D/big.lock; : > "$LOG"; }
run() {  # run NAME SECONDS NEED [heavy-lock]: in the background
  local hl=${4:-$HL}
  HEAVY_MEM_GIB=$3 "$hl" "$T/job" "$1" "$2" 2>> "$D/stderr" &
}

# 1. two at once, the third waits
case_dir two; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G))
run A 2 1; sleep 0.3; run B 2 1; sleep 0.3; run C 0.5 1; sleep 0.5
qs=$("$HL" --queue)
wait
before B start A end && ok "1: B started while A ran" || bad "1: B did not run beside A"
{ before A end C start || before B end C start; } && ! before C start A end && ok "1: C waited for a slot (A's)" || bad "1: C did not wait: $(cat "$LOG")"
[ "$(slot A) $(slot B) $(slot C)" = "1 2 1" ] && ok "1: slots A 1, B 2, C 1" || bad "1: slots $(slot A) $(slot B) $(slot C)"
[ "$(awk '$2 == "start" {print $5}' "$LOG" | sort -u)" = 1 ] && ok "1: ARBACE_HEAVY_HELD=1 inside" || bad "1: ARBACE_HEAVY_HELD"
grep -q 'slot 1' <<< "$qs" && grep -q 'slot 2' <<< "$qs" && grep -q 'waiting' <<< "$qs" && ok "1: --queue names slot 1, slot 2 and the waiter" || bad "1: --queue: $qs"
ls -A "$D/big.lock.queue" | grep -v '^\.err$' | grep -q . && bad "1: tickets left: $(ls -A "$D/big.lock.queue")" || ok "1: no ticket or state file left"

# 2. the memory rule; a later ticket that fits backfills beside the holder ahead of a head that does not fit
case_dir mem; export HEAVY_LOCK_MEMAVAIL_KIB=$((10 * G))
run A 3 1; sleep 0.3; run B 0.5 20; sleep 0.3; run C 0.3 1
sleep 0.4; grep -q 'waiting for memory' "$D/stderr" && ok "2: the head says it waits for memory" || bad "2: no memory message"
wait
before A end B start && ok "2: B (20 GiB, 10 available) waited for A" || bad "2: B ran beside A: $(cat "$LOG")"
before C start B start && before C start A end && ok "2: C (1 GiB, fits beside A) backfilled ahead of B, beside A" || bad "2: C did not backfill: $(cat "$LOG")"
[ "$(slot B)" = 1 ] && ok "2: B ran alone in slot 1" || bad "2: B's slot $(slot B)"

# 3. FIFO with room for one job only
case_dir fifo; export HEAVY_LOCK_MEMAVAIL_KIB=0
for n in W1 W2 W3 W4 W5; do run $n 0.3 1; sleep 0.15; done
wait
order=$(awk '$2 == "start" {print $1}' "$LOG" | tr '\n' ' ')
[ "$order" = "W1 W2 W3 W4 W5 " ] && ok "3: FIFO: $order" || bad "3: order $order"

# 4. a dead ticket and a dead state file
case_dir dead; export HEAVY_LOCK_MEMAVAIL_KIB=0
mkdir -p "$D/big.lock.queue"; echo 12345 > "$D/big.lock.queue/1-999999"; echo "12345 1 0" > "$D/big.lock.queue/.s-1-999998"
t0=$(date +%s%N); run A 0.2 1; wait; t1=$(date +%s%N)
[ -n "$(at A end)" ] && [ $(( (t1 - t0) / 1000000 )) -lt 3000 ] && ok "4: the waiter was not held up by a dead ticket" || bad "4: A: $(cat "$LOG")"
[ ! -e "$D/big.lock.queue/1-999999" ] && [ ! -e "$D/big.lock.queue/.s-1-999998" ] && ok "4: the dead ticket and state file removed" || bad "4: left: $(ls -A "$D/big.lock.queue")"

# 5. plain flock
case_dir plain; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G))
flock "$D/big.lock" "$T/job" P 1.5 & sleep 0.3; run A 0.3 1; wait
before A end P end && [ "$(slot A)" = 2 ] && ok "5: a ticket ran beside plain flock, in slot 2" || bad "5: $(cat "$LOG")"
: > "$LOG"; export HEAVY_LOCK_MEMAVAIL_KIB=$((3 * G))
flock "$D/big.lock" "$T/job" P 1 & sleep 0.3; run A 0.3 2; wait
before P end A start && ok "5: a ticket that does not fit waited for plain flock" || bad "5: $(cat "$LOG")"
: > "$LOG"; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G))
run A 1.5 1; sleep 0.3; flock "$D/big.lock" "$T/job" P 0.2; wait
before A end P start && ok "5: plain flock waited for slot 1's ticket" || bad "5: $(cat "$LOG")"

# 6. the one-slot version's waiters
OLD=$T/old/bin/heavy-lock; mkdir -p "$T/old/bin"
if git -C "$here" show 903b053e4:bin/heavy-lock > "$OLD" 2>"$T/git.err"; then
  chmod +x "$OLD"
  case_dir old-holder; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G))
  run O 1.5 1 "$OLD"; sleep 0.4; run N 0.3 1; sleep 0.3
  qs=$("$HL" --queue); wait
  before N end O end && [ "$(slot N)" = 2 ] && ok "6a: a ticket ran beside an old holder, in slot 2" || bad "6a: $(cat "$LOG")"
  grep -q 'slot 1 (one-slot version)' <<< "$qs" && ok "6a: --queue sees the old holder in slot 1" || bad "6a: --queue: $qs"
  case_dir old-head; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G))
  flock "$D/big.lock" "$T/job" P 1.5 & sleep 0.3; run O 0.5 1 "$OLD"; sleep 0.3; run N 0.3 1; wait
  before N end P end && [ "$(slot N)" = 2 ] && ok "6b: behind an old head waiting for slot 1, a ticket took slot 2" || bad "6b: $(cat "$LOG")"
  before P end O start && ok "6b: the old head got slot 1 after plain flock" || bad "6b: $(cat "$LOG")"
  case_dir old-head-mem; export HEAVY_LOCK_MEMAVAIL_KIB=0
  flock "$D/big.lock" "$T/job" P 1 & sleep 0.3; run O 0.5 1 "$OLD"; sleep 0.3; run N 0.3 1; wait
  before O start N start && ok "6c: with no room, a ticket after an old one did not take slot 1 before it" || bad "6c: $(cat "$LOG")"
  case_dir old-after; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G))
  run N1 1 1; sleep 0.3; run O 0.3 1 "$OLD"; sleep 0.3; run N2 0.3 1; wait
  before N1 end O start && ok "6d: an old ticket after a new holder ran when it ended" || bad "6d: $(cat "$LOG")"
  [ "$(slot N2)" = 2 ] && before N2 start N1 end && ok "6d: a later ticket ran in slot 2 meanwhile" || bad "6d: $(cat "$LOG")"
else
  echo "skip 6: the one-slot version (903b053e4) is not in this clone's history"
fi

# 7. one slot, and the small slot beside it
case_dir one-slot; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G)); echo 1 > "$D/big.lock.slots"
run A 2 20; sleep 0.3; run B 0.3 20; sleep 0.3; run S 0.3 4; wait
before A end B start && ok "7: a big ticket (20 GiB) waited for the holder with room for it" || bad "7: B ran beside A: $(cat "$LOG")"
before S start A end && [ "$(slot S)" = 2 ] && ok "7: a small ticket (4 GiB) ran beside the holder in slot 2, past the big waiter" || bad "7: S: $(cat "$LOG")"
case_dir one-slot-env; echo 2 > "$D/big.lock.slots"
HEAVY_LOCK_SLOTS=1 run A 1.5 20; sleep 0.3; HEAVY_LOCK_SLOTS=1 HEAVY_LOCK_SMALL_GIB=2 run S 0.3 4; wait
before A end S start && ok "7: HEAVY_LOCK_SLOTS=1 over the file, and HEAVY_LOCK_SMALL_GIB=2 keeps a 4 GiB ticket out of slot 2" || bad "7: env: $(cat "$LOG")"
case_dir small-file; export HEAVY_LOCK_MEMAVAIL_KIB=$((100 * G)); echo 1 > "$D/big.lock.slots"; echo 16 > "$D/big.lock.small_gib"
run A 1.5 20; sleep 0.3; run S 0.3 12; wait
before S start A end && [ "$(slot S)" = 2 ] && ok "7: LOCK.small_gib 16 lets a 12 GiB ticket run in slot 2" || bad "7: small_gib file: $(cat "$LOG")"
unset HEAVY_LOCK_MEMAVAIL_KIB

[ "$fail" = 0 ] && echo "heavy-lock-test: passed" || echo "heavy-lock-test: FAILED"
exit "$fail"
