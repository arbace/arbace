# Go's build cache by one rule, the gate's and a build's by hand alike (n-gocache-trim, 2026-11-19; test/gate/caches.edn's
# "{gocache}" row, whose numbers gocache_trim's defaults repeat and test/gate/gocache-trim-test.bash holds equal):
#   gocache_cap_kib SIZE_KIB FREE_KIB MAX_GIB [FREE_GIB [MIN_GIB]]: the cap in KiB, min(MAX, size + free - FREE), at least MIN
#                 (n-gate-gocache's rule: 40 GiB at most, less when keeping it would leave under 35 GiB free, never under 8);
#                 FREE_GIB "-" or empty: MAX alone
#   caches_go_busy: 0 when a process of Go's toolchain (ARBACE_GOROOT's go, compile, link, asm) runs on the host
#                 (GATE_CACHES_GO_BUSY=1|0 decides instead, the tests): never trim under a live build
#   gocache_trim [GO]: GOCACHE (as GO's go env resolves it; GATE_CACHES_GOCACHE names it instead, the tests) trimmed to its cap
#                 by the least recently used, only when no Go build runs and only when it holds more than its cap: no age-based
#                 wipe while there is disk to spare. Sourced by bin/arbace-go (every --build) and bin/lib/gate-run.bash; prints one line.
#                 ARBACE_GOCACHE_MAX_GIB (40), ARBACE_GOCACHE_FREE_GIB (35), ARBACE_GOCACHE_MIN_GIB (8): the rule's numbers;
#                 GATE_CACHES_FREE_KIB: the free disk instead of df's (the tests); ARBACE_GOCACHE_TRIM_DRY=1: lists, removes nothing.
#                 ARBACE_GOCACHE_KEEP_MIN (default 90; the gate's 7200, Go's own five days) keeps its meaning as a bound: no entry
#                 used within that many minutes is removed, even over the cap. Until 2026-11-19 every entry unused for that long
#                 was removed whatever the disk (it cut the cache from 10.5 to 6.4 GiB on 2026-11-19, the next build cold).
gocache_cap_kib() {
  awk -v s="${1:-0}" -v f="${2:-0}" -v c="$3" -v fr="${4:--}" -v mn="${5:-0}" 'BEGIN {
    k = c * 1048576; if (fr != "-" && fr != "") { v = s + f - fr * 1048576; if (v < k) k = v }
    if (mn == "-") mn = 0; if (k < mn * 1048576) k = mn * 1048576; printf "%d", k }'
}
caches_go_busy() {
  case ${GATE_CACHES_GO_BUSY:-} in 1) return 0 ;; 0) return 1 ;; esac
  local g=${ARBACE_GOROOT:-/root/tamago-go} p e err=${TMPDIR:-$root/.tmp/tmp}/caches-go.err
  mkdir -p "${err%/*}"
  g=$(readlink -f "$g" 2>"$err" || echo "$g")
  for p in /proc/[0-9]*; do
    e=$(readlink "$p/exe" 2>"$err") || continue
    case $e in "$g"/bin/*|"$g"/pkg/tool/*) return 0 ;; esac
  done
  return 1
}
gocache_trim() {
  local go=${1:-go} gocache keep=${ARBACE_GOCACHE_KEEP_MIN:-90} discard=${TMPDIR:-$root/.tmp/tmp}/gocache.discard
  local max=${ARBACE_GOCACHE_MAX_GIB:-40} fr=${ARBACE_GOCACHE_FREE_GIB:-35} mn=${ARBACE_GOCACHE_MIN_GIB:-8} sk fk capk lst n
  mkdir -p "${discard%/*}"
  if caches_go_busy; then echo "   Go build cache left alone: a Go build runs (never trimmed under a live build)"; return 0; fi
  if [ -n "${GATE_CACHES_GOCACHE:-}" ]; then gocache=$GATE_CACHES_GOCACHE; else gocache=$("$go" env GOCACHE 2>"$discard" || true); fi
  [ -n "$gocache" ] && [ -d "$gocache" ] || return 0
  sk=$(du -sk "$gocache" 2>"$discard" | cut -f1)
  if [ -n "${GATE_CACHES_FREE_KIB:-}" ]; then fk=$GATE_CACHES_FREE_KIB; else fk=$(df -Pk "$gocache" 2>"$discard" | awk 'NR == 2 {print $4 + 0}'); fi
  capk=$(gocache_cap_kib "${sk:-0}" "${fk:-0}" "$max" "$fr" "$mn")
  if [ "${sk:-0}" -le "$capk" ]; then
    echo "   Go build cache kept: $(( ${sk:-0} / 1024 )) MB, its cap $(( capk / 1024 )) MB (at most $max GiB, $fr GiB left free, at least $mn GiB)"
    return 0
  fi
  lst=$discard.list
  # the least recently used first, those used within ARBACE_GOCACHE_KEEP_MIN minutes never, until the cache holds its cap
  find "$gocache" -type f -mmin +"$keep" -printf '%T@\t%s\t%p\n' 2>"$discard" | sort -n \
    | awk -F'\t' -v t="$(( sk * 1024 ))" -v lim="$(( capk * 1024 ))" '{ if (t <= lim) exit; print $3; t -= $2 }' > "$lst"
  n=$(wc -l < "$lst")
  if [ "${ARBACE_GOCACHE_TRIM_DRY:-}" = 1 ]; then
    echo "   Go build cache: would remove $n entries ($(( sk / 1024 )) MB, its cap $(( capk / 1024 )) MB)"; rm -f "$lst"; return 0
  fi
  xargs -r -d '\n' rm -f -- < "$lst"
  find "$gocache" -mindepth 1 -type d -empty -delete 2>"$discard" || true
  rm -f "$lst"
  echo "   Go build cache trimmed to its cap $(( capk / 1024 )) MB by the least recently used ($n entries): $(( sk / 1024 )) MB -> $(du -sm "$gocache" 2>"$discard" | cut -f1) MB"
}

# build_go_print_cache_trim DIR: the build's cache of printed packages (bin/build-go --print-cache, standalone
# S6) trimmed alike, of entries unused for ARBACE_GOCACHE_KEEP_MIN minutes, and its collect
# indexes (DIR/collect/, files: arbace.build.go's root-packages); queue 485: under DIR/.lock taken exclusive, and
# not at all while a build holds it (bin/build-go holds it shared): no entry a running build has read is removed
build_go_print_cache_trim() {
  local keep=${ARBACE_GOCACHE_KEEP_MIN:-90} discard=${TMPDIR:-$root/.tmp/tmp}/gocache.discard
  [ -d "$1" ] || return 0
  mkdir -p "${discard%/*}"
  (
    flock -n 9 || { echo "   the print cache $1 left alone: a build uses it (its lock $1/.lock)" >&2; exit 0; }
    find "$1" -mindepth 2 -maxdepth 2 -type d -mmin +"$keep" -exec rm -rf {} + 2>"$discard" || true
    [ -d "$1/collect" ] && find "$1/collect" -type f -mmin +"$keep" -delete 2>"$discard" || true
  ) 9>> "$1/.lock"
}
