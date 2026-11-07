# The Haskell crop's fetched inputs in a linked worktree or the gate's clean clone (batch 2's proposal 8 (a), 2026-11-07;
# as Scheme's corpora, SM9, and Python's tarball, GTS212 (a)). Sourced by bin/repr-haskell and bin/hsi after $root is set.
#
#   haskell_main               the main checkout: the parent of the git common dir of a linked worktree
#                              (bin/lib/gate-cache.bash's arbace_main_checkout), else the checkout a path
#                              .../.tmp/gate-clean/SHA or .../.tmp/worktrees/W is under, else $root itself
#   haskell_take FILE          FILE (under $root) missing: the main checkout's copy at the same path under it, hard-linked
#                              (copied across file systems) through a temporary name; status 0 when taken, 1 when $root
#                              is the main checkout (the caller downloads), and a refusal (exit 1) when a worktree's
#                              main checkout has no copy: a worktree never downloads (the user's rule: no network in the
#                              worktrees), the main checkout fetches once (bin/repr-haskell corpus fetch, bin/hsi pins --fetch)
#   haskell_take_git DIR COMMIT
#                              DIR (under $root) a git checkout of COMMIT made from the main checkout's at the same path
#                              (a local fetch of the one commit); status as haskell_take's
# The callers check every pin (SHA-256, SHA-512, the tree hash) after, as for a download.

. "$root/bin/lib/gate-cache.bash"

haskell_main() {
  local m
  m=$(arbace_main_checkout "$root")
  if [ "$m" = "$root" ]; then m=${root%%/.tmp/gate-clean/*}; m=${m%%/.tmp/worktrees/*}; fi
  echo "$m"
}

haskell_refuse() {
  echo "haskell inputs: $1 is missing and the main checkout ($2) has no copy at $3; a worktree never downloads:" \
       "fetch it once in the main checkout (cd $2 && bin/repr-haskell corpus fetch; bin/hsi pins --fetch)" >&2
  exit 1
}

haskell_take() {
  local f=$1 m rel src
  m=$(haskell_main); [ "$m" != "$root" ] || return 1
  rel=${f#"$root"/}; src=$m/$rel
  [ -f "$src" ] || haskell_refuse "$f" "$m" "$src"
  mkdir -p "$(dirname "$f")"
  echo "== haskell inputs: $rel from the main checkout ($m)" >&2
  rm -f "$f.part.$$"
  ln "$src" "$f.part.$$" 2>"$TMPDIR/haskell-take.err" || cp "$src" "$f.part.$$" || haskell_refuse "$f" "$m" "$src"
  mv "$f.part.$$" "$f"
}

haskell_take_git() {
  local d=$1 commit=$2 m rel src
  m=$(haskell_main); [ "$m" != "$root" ] || return 1
  rel=${d#"$root"/}; src=$m/$rel
  [ -d "$src/.git" ] && [ "$(git -C "$src" rev-parse HEAD 2>"$TMPDIR/haskell-take.err")" = "$commit" ] \
    || haskell_refuse "$d" "$m" "$src (at $commit)"
  echo "== haskell inputs: $rel at $commit from the main checkout ($m)" >&2
  rm -rf "$d"; mkdir -p "$d"
  git -C "$d" init -q
  git -C "$d" fetch -q --depth 1 "$src" "$commit"
  git -C "$d" -c advice.detachedHead=false checkout -q FETCH_HEAD
}
