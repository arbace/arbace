# gocache_trim: trim Go's build cache (GOCACHE, as `go env` resolves it) of entries unused for
# ARBACE_GOCACHE_KEEP_MIN minutes (default 360, six hours). Go keeps entries five days and
# refreshes an entry's time when it uses it (at most hourly), so six hours is safe for builds in
# progress; c2g's programs, the round trips and parallel agents fill the cache by tens of GB in
# hours. Sourced by bin/gate and bin/arbace-go; prints one line.
gocache_trim() {
  local go=${1:-go} gocache before keep=${ARBACE_GOCACHE_KEEP_MIN:-360}
  gocache=$("$go" env GOCACHE 2>/dev/null || true)
  [ -n "$gocache" ] && [ -d "$gocache" ] || return 0
  before=$(du -sm "$gocache" | cut -f1)
  find "$gocache" -type f -mmin +"$keep" -delete 2>/dev/null || true
  find "$gocache" -mindepth 1 -type d -empty -delete 2>/dev/null || true
  echo "   Go build cache trimmed (entries unused for $keep min): $before MB -> $(du -sm "$gocache" | cut -f1) MB"
}
