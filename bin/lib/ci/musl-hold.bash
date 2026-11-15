# ci_musl_hold: musl's forms cache (bin/ci musl's .tmp/ci-musl/forms, CI_MUSL_FORMS) fresh, and held under a shared flock on FORMS.lock
# for the rest of the calling shell and its children (review 300's finding 2; gate 13c's llvm-controls-test: a C step's `bin/ci musl
# read` removed and reread the cache while llvmi indexed it). Sourced; needs $root. The readers' writer, `bin/ci musl read`, takes the
# exclusive lock on the same file, so no read removes the cache while any holder runs; holders share it.
#
#   - the shared lock first, then the fresh check (no window between an ensure and the lock); when stale, the lock released, `bin/ci musl
#     ensure` (its read under the exclusive lock), the shared lock taken again and the check repeated (three times at most)
#   - the lock on fd 197 (no script of bin/ uses it), inherited by the children; CI_MUSL_HELD=1 exported: a child's ci_musl_hold checks
#     the cache fresh and takes no lock of its own (releasing an inherited fd would release the parent's lock), and a child's `bin/ci
#     musl read` or `ensure` finds it fresh and reads nothing, or fails at once when it is stale (an exclusive lock under its own
#     parent's shared one would never come)
#   - each wait at most CI_MUSL_LOCK_WAIT seconds (default 7200), then a failure naming the lock (busybox's flock has no -w: timeout(1)
#     runs flock on the inherited fd, and the lock, the open file's, stays the shell's when flock exits)
ci_musl_hold() {
  local forms=${CI_MUSL_FORMS:-$root/.tmp/ci-musl/forms} wait=${CI_MUSL_LOCK_WAIT:-7200} i
  if [ -n "${CI_MUSL_HELD:-}" ]; then
    "$root/bin/lib/ci/musl" fresh && return 0
    echo "ci: musl's forms cache is held by a parent and not fresh ($forms; its key moved during the run?)" >&2; return 1
  fi
  mkdir -p "$(dirname "$forms")"
  "$root/bin/lib/ci/musl" prepare > "$TMPDIR/ci-musl-hold.$$.log" 2>&1 || { cat "$TMPDIR/ci-musl-hold.$$.log" >&2; rm -f "$TMPDIR/ci-musl-hold.$$.log"; return 1; }
  rm -f "$TMPDIR/ci-musl-hold.$$.log"
  exec 197>>"$forms.lock"
  for i in 1 2 3; do
    timeout "$wait" flock -s 197 || { echo "ci: no shared lock on $forms.lock in $wait s (a reader holds it: CI_MUSL_LOCK_WAIT)" >&2; exec 197>&-; return 1; }
    if "$root/bin/lib/ci/musl" fresh; then export CI_MUSL_HELD=1; return 0; fi
    flock -u 197
    "$root/bin/ci" musl ensure || { exec 197>&-; return 1; }
  done
  echo "ci: musl's forms cache not fresh after three reads ($forms)" >&2; exec 197>&-; return 1
}
