# A crop's fetched inputs in a linked worktree or the gate's clean clone (batch 3's proposal 4 (a), the user 2026-11-07; as batch
# 2's proposal 8 (a) did for Haskell's, bin/lib/repr-haskell/inputs.bash). Sourced after $root is set.
#
#   crop_main            the main checkout: the parent of the git common dir of a linked worktree (bin/lib/gate-cache.bash's
#                        arbace_main_checkout), else the checkout a path .../.tmp/gate-clean/SHA or .../.tmp/worktrees/W is under,
#                        else $root itself
#   crop_take FILE HOW   FILE (under $root) missing: the main checkout's copy at the same path, hard-linked (copied across file
#                        systems) through a temporary name; status 0 when taken, 1 when $root is the main checkout (the caller
#                        fetches), and a refusal (exit 1) when a worktree's main checkout has no copy: a worktree never downloads
#                        (the user's rule), HOW the command that fetches it once in the main checkout
# The callers check every pin after, as for a download.

. "$root/bin/lib/gate-cache.bash"

crop_main() {
  local m
  m=$(arbace_main_checkout "$root")
  if [ "$m" = "$root" ]; then m=${root%%/.tmp/gate-clean/*}; m=${m%%/.tmp/worktrees/*}; fi
  echo "$m"
}

crop_take() {
  local f=$1 how=$2 m rel src
  m=$(crop_main); [ "$m" != "$root" ] || return 1
  rel=${f#"$root"/}; src=$m/$rel
  if [ ! -f "$src" ]; then
    echo "crop inputs: $f is missing and the main checkout ($m) has no copy at $src; a worktree never downloads:" \
         "fetch it once in the main checkout (cd $m && $how)" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$f")"
  echo "== crop inputs: $rel from the main checkout ($m)" >&2
  rm -f "$f.part.$$"
  ln "$src" "$f.part.$$" 2>"$TMPDIR/crop-take.err" || cp "$src" "$f.part.$$" || { echo "crop inputs: cannot take $src" >&2; exit 1; }
  mv "$f.part.$$" "$f"
}
