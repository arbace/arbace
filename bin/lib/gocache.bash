# gocache_trim: trim Go's build cache (GOCACHE, as `go env` resolves it) of entries unused for
# ARBACE_GOCACHE_KEEP_MIN minutes (default 90; three hours until 2026-10-18, six until 2026-10-17). Go keeps entries five days and
# refreshes an entry's time when it uses it (at most hourly), so 90 minutes is safe for builds in
# progress; j2g's programs, the round trips and parallel agents fill the cache by tens of GB in
# hours. Sourced by bin/gate and bin/arbace-go; prints one line.
gocache_trim() {
  local go=${1:-go} gocache before keep=${ARBACE_GOCACHE_KEEP_MIN:-90}
  gocache=$("$go" env GOCACHE 2>/dev/null || true)
  [ -n "$gocache" ] && [ -d "$gocache" ] || return 0
  before=$(du -sm "$gocache" | cut -f1)
  find "$gocache" -type f -mmin +"$keep" -delete 2>/dev/null || true
  find "$gocache" -mindepth 1 -type d -empty -delete 2>/dev/null || true
  echo "   Go build cache trimmed (entries unused for $keep min): $before MB -> $(du -sm "$gocache" | cut -f1) MB"
}

# build_go_print_cache_trim DIR: the build's cache of printed packages (bin/build-go --print-cache, standalone
# S6) trimmed alike, of entries unused for ARBACE_GOCACHE_KEEP_MIN minutes, and its collect
# indexes (DIR/collect/, files: arbace.build.go's root-packages)
build_go_print_cache_trim() {
  local keep=${ARBACE_GOCACHE_KEEP_MIN:-90}
  [ -d "$1" ] || return 0
  find "$1" -mindepth 2 -maxdepth 2 -type d -mmin +"$keep" -exec rm -rf {} + 2>/dev/null || true
  [ -d "$1/collect" ] && find "$1/collect" -type f -mmin +"$keep" -delete 2>/dev/null || true
}
