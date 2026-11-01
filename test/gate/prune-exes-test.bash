#!/bin/bash
# bin/gate --prune's executables loop (the real gate_prune): the leftovers of a miss (KEY20.tmp.PID,
# KEY20.old.PID) are removed when an hour old and do not count among the newest 3 directories kept
# (review 048, N5). Usage: test/gate/prune-exes-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=$here/.tmp/tmp; mkdir -p "$TMPDIR"
T=$here/.tmp/prune-exes-test
rm -rf "$T"; mkdir -p "$T/store" "$T/exes"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
now=$(date +%s)
mk() { mkdir -p "$T/exes/$1"; touch -d "@$(( now - $2 ))" "$T/exes/$1"; }
# four directories two days old to five days old; two leftovers newer than all, two hours old
mk aaaaaaaaaaaaaaaaaaaa 172800; mk bbbbbbbbbbbbbbbbbbbb 200000; mk cccccccccccccccccccc 250000; mk dddddddddddddddddddd 300000
mk aaaaaaaaaaaaaaaaaaaa.tmp.111 7200; mk bbbbbbbbbbbbbbbbbbbb.old.222 7200
mk cccccccccccccccccccc.tmp.333 60   # a minute old: a build under way, kept
store=$T/store exes=$T/exes
( . "$here/bin/lib/gate-run.bash"; gate_prune ) > "$T/out" 2>&1
left=$(ls "$T/exes" | tr '\n' ' ')
for d in aaaaaaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbbbbbb cccccccccccccccccccc; do
  [ -d "$T/exes/$d" ] && ok "$d kept (among the newest 3 real directories)" || bad "$d removed; left: $left"
done
[ ! -d "$T/exes/dddddddddddddddddddd" ] && ok "the fourth real directory pruned" || bad "the fourth real directory kept; left: $left"
[ ! -e "$T/exes/aaaaaaaaaaaaaaaaaaaa.tmp.111" ] && [ ! -e "$T/exes/bbbbbbbbbbbbbbbbbbbb.old.222" ] && ok "two-hour-old leftovers removed" || bad "leftovers kept; left: $left"
[ -d "$T/exes/cccccccccccccccccccc.tmp.333" ] && ok "a minute-old copy kept (a build under way)" || bad "a build under way removed"
exit $fail
