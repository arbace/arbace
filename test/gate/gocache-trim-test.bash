#!/bin/bash
# n-gocache-trim (2026-11-19): bin/lib/gocache.bash's gocache_trim, the trim of Go's build cache before every bin/arbace-go --build,
# follows the gate's rule (test/gate/caches.edn's "{gocache}" row, bin/lib/gate-run.bash's caches_prune), on a scratch cache under
# .tmp/gocache-trim-test (GATE_CACHES_GOCACHE, GATE_CACHES_FREE_KIB, GATE_CACHES_GO_BUSY), never Go's real cache:
#   1. gocache_trim's defaults (ARBACE_GOCACHE_MAX_GIB, _FREE_GIB, _MIN_GIB) are the table row's :rule [:size N], :free-gib, :min-gib,
#      and caches_prune computes its cap by the same gocache_cap_kib;
#   2. gocache_cap_kib: min(MAX, size + free - FREE), at least MIN;
#   3. under its cap nothing is removed, however old (no age-based wipe while there is disk to spare);
#   4. over its cap the least recently used are removed down to it, an entry used within ARBACE_GOCACHE_KEEP_MIN minutes never;
#   5. a Go build running: nothing removed; ARBACE_GOCACHE_TRIM_DRY=1: nothing removed.
# Usage: test/gate/gocache-trim-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=$here/.tmp/tmp; mkdir -p "$TMPDIR"
T=$here/.tmp/gocache-trim-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
now=$(date +%s)

# 1. the defaults are the table's
row=$(grep -F ':path "{gocache}"' "$here/test/gate/caches.edn")
t_max=$(sed -E 's/.*:rule \[:size ([0-9.]+) :lru\].*/\1/' <<< "$row")
t_free=$(sed -E 's/.*:free-gib ([0-9.]+).*/\1/' <<< "$row")
t_min=$(sed -E 's/.*:min-gib ([0-9.]+).*/\1/' <<< "$row")
grep -Eq ':idle-only true' <<< "$row" || bad "the table's {gocache} row is no longer :idle-only"
d=$(sed -nE 's/.*local max=\$\{ARBACE_GOCACHE_MAX_GIB:-([0-9.]+)\} fr=\$\{ARBACE_GOCACHE_FREE_GIB:-([0-9.]+)\} mn=\$\{ARBACE_GOCACHE_MIN_GIB:-([0-9.]+)\}.*/\1 \2 \3/p' "$here/bin/lib/gocache.bash")
[ "$d" = "$t_max $t_free $t_min" ] && ok "gocache_trim's defaults are the table's ($d)" || bad "gocache_trim's defaults '$d', the table's '$t_max $t_free $t_min'"
grep -q 'capk=$(gocache_cap_kib ' "$here/bin/lib/gate-run.bash" && ! grep -q '^caches_go_busy()' "$here/bin/lib/gate-run.bash" \
  && ok "caches_prune takes gocache.bash's cap and caches_go_busy" || bad "caches_prune has a cap or busy test of its own"

lib() { ( root=$here; . "$here/bin/lib/gocache.bash"; "$@" ); }
# 2. the cap (KiB): 40 GiB at most, size + free - 35 GiB, at least 8
g=1048576
[ "$(lib gocache_cap_kib $((10 * g)) $((100 * g)) 40 35 8)" = $((40 * g)) ] && ok "cap: a free disk, the maximum" || bad "cap with a free disk"
[ "$(lib gocache_cap_kib $((10 * g)) $((40 * g)) 40 35 8)" = $((15 * g)) ] && ok "cap: near the floor, size + free - 35" || bad "cap near the floor"
[ "$(lib gocache_cap_kib $((10 * g)) $((20 * g)) 40 35 8)" = $((8 * g)) ] && ok "cap: never under the minimum" || bad "cap's minimum"
[ "$(lib gocache_cap_kib $((10 * g)) 0 40 - 0)" = $((40 * g)) ] && ok "cap: no :free-gib, the maximum alone" || bad "cap without free"

# a scratch cache: 12 files of 1 MiB, used 1..12 hours ago (in Go's layout, a directory each)
mk() { rm -rf "$T/c"; local i; for i in $(seq 1 12); do mkdir -p "$T/c/$(printf %02x "$i")"; yes | head -c 1048576 > "$T/c/$(printf %02x "$i")/f$i"; touch -d "@$(( now - i * 3600 ))" "$T/c/$(printf %02x "$i")/f$i"; done; }
left() { find "$T/c" -type f | wc -l; }
# toy numbers in GiB: max 1 GiB, 4 MiB free, :free-gib 8 MiB: cap 12 + 4 - 8 = 8 MiB
trim() { GATE_CACHES_GOCACHE=$T/c GATE_CACHES_FREE_KIB=4096 ARBACE_GOCACHE_MAX_GIB=1 ARBACE_GOCACHE_FREE_GIB=0.0078125 ARBACE_GOCACHE_MIN_GIB=0 "$@" \
  bash -c 'root=$1; . "$1/bin/lib/gocache.bash"; gocache_trim /nonexistent/go' x "$here" > "$T/out" 2>&1; }

# 3. under the cap: nothing, though every entry is hours old (the 90-minute wipe removed 11 of 12)
mk; trim env GATE_CACHES_GO_BUSY=0 GATE_CACHES_FREE_KIB=$((100 * 1024))
[ "$(left)" = 12 ] && grep -q 'Go build cache kept' "$T/out" && ok "under its cap: nothing removed, though 11 entries are older than 90 minutes" || bad "under the cap: $(left) left, $(cat "$T/out")"
# 4. over the cap: the 4 oldest out, the newest 8 kept
mk; trim env GATE_CACHES_GO_BUSY=0
[ "$(left)" = 8 ] && [ ! -e "$T/c/0c/f12" ] && [ ! -e "$T/c/09/f9" ] && [ -e "$T/c/08/f8" ] && [ ! -e "$T/c/0c" ] \
  && ok "over its cap: the 4 least recently used removed, their empty directories too" || bad "over the cap: $(left) left, $(cat "$T/out")"
# ... but an entry used within ARBACE_GOCACHE_KEEP_MIN minutes never: with 630 minutes only f11 and f12 may go
mk; trim env GATE_CACHES_GO_BUSY=0 ARBACE_GOCACHE_KEEP_MIN=630
[ "$(left)" = 10 ] && [ -e "$T/c/0a/f10" ] && ok "ARBACE_GOCACHE_KEEP_MIN: entries used since stay, even over the cap" || bad "KEEP_MIN: $(left) left, $(cat "$T/out")"
# 5. a Go build running: nothing; the dry run: nothing
mk; trim env GATE_CACHES_GO_BUSY=1
[ "$(left)" = 12 ] && grep -q 'a Go build runs' "$T/out" && ok "a Go build running: nothing removed" || bad "busy: $(left) left, $(cat "$T/out")"
mk; trim env GATE_CACHES_GO_BUSY=0 ARBACE_GOCACHE_TRIM_DRY=1
[ "$(left)" = 12 ] && grep -q 'would remove 4 entries' "$T/out" && ok "ARBACE_GOCACHE_TRIM_DRY=1: 4 listed, nothing removed" || bad "dry: $(left) left, $(cat "$T/out")"

[ $fail = 0 ] && rm -rf "$T"
exit $fail
