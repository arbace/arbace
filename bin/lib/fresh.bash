# The box's executables checked against the commit a script works on (bin/box-rebuild,
# bin/box-mac-bundle): a store, an expected image or a bundle made of one commit's sources must
# not take an executable built from another commit, or from uncommitted changes. Sourced, after
# setting root (the checkout). Defines:
#
#   arbace_exe_version FILE   prints the version map linked into the executable FILE
#                             (main.arbaceVersion, bin/lib/version.bash), found in its bytes, so
#                             that a guest (arbace.elf) or another architecture's executable is
#                             read without running it; the launcher arbace-box carries its
#                             appended guest's (the bare launcher arbace-box.bin and the tool
#                             guests of bin/box-tools carry none). Status 1 when FILE holds none,
#                             or more than one.
#   arbace_fresh WHO FILE COMMIT CMD
#                             stops (exit 1) with a message naming FILE and CMD, the command that
#                             rebuilds it, unless FILE exists and carries the version of COMMIT (40
#                             hex), clean unless the checkout is dirty; with ARBACE_BOX_STALE_OK=1
#                             (experiments) it warns, sets arbace_stale_used=1 and goes on. WHO
#                             prefixes the messages.
#   arbace_guest_key_fresh WHO ELF CMD
#                             the same for a guest's image.key (beside ELF) against the program
#                             bin/arbace-go --build translated last (target/arbace-go/j2g/prog,
#                             bin/arbace-go's box_key): bin/arbace-go --build --box links the
#                             guest with the checkout's version but from that program, which may
#                             be older.

arbace_stale_used=0

arbace_exe_version() {
  local v
  v=$(LC_ALL=C grep -aoE '\{:describe "[^"]*" :commit "[0-9a-f]{40}" :dirty (true|false)( :runtime "[0-9a-f]{64}")?\}' "$1" \
        | sort -u) || return 1
  [ -n "$v" ] && [ "$(printf '%s\n' "$v" | wc -l)" = 1 ] || return 1
  printf '%s\n' "$v"
}

# arbace_stale WHO FILE WHY CMD: the refusal (or, with ARBACE_BOX_STALE_OK=1, the warning)
arbace_stale() {
  if [ "${ARBACE_BOX_STALE_OK:-}" = 1 ]; then
    echo "$1: warning: $2 is stale ($3); used anyway (ARBACE_BOX_STALE_OK=1)" >&2
    arbace_stale_used=1
    return 0
  fi
  echo "$1: $2 is stale: $3" >&2
  echo "$1: rebuild it: $4 (or ARBACE_BOX_STALE_OK=1 to use it anyway)" >&2
  exit 1
}

arbace_fresh() {
  local who=$1 f=$2 want=$3 cmd=$4 v c d desc wd=false
  [ -e "$f" ] || { echo "$who: no $f; build it: $cmd" >&2; exit 1; }
  v=$(arbace_exe_version "$f") || { arbace_stale "$who" "$f" "it carries no version of Arbace" "$cmd"; return 0; }
  c=$(sed -E 's/.*:commit "([0-9a-f]{40})".*/\1/' <<< "$v")
  d=$(sed -E 's/.*:dirty (true|false).*/\1/' <<< "$v")
  desc=$(sed -E 's/^\{:describe "([^"]*)".*/\1/' <<< "$v")
  case $(git -C "$root" describe --tags --always --dirty 2>/dev/null) in *-dirty) wd=true ;; esac
  if [ "$c" != "$want" ]; then
    arbace_stale "$who" "$f" "built from $desc (${c:0:12}), not from the commit ${want:0:12}" "$cmd"
  elif [ "$d" = true ] && [ "$wd" = false ]; then
    arbace_stale "$who" "$f" "built from uncommitted changes ($desc), the checkout is clean" "$cmd"
  fi
}

arbace_guest_key_fresh() {
  local who=$1 elf=$2 cmd=$3 main=$root/target/arbace-go/j2g/prog/go/arbace/cmd/arbace k
  k=$(dirname "$elf")/image.key
  if [ ! -s "$k" ] || [ ! -s "$main/image.bin" ] \
     || [ "$(cat "$k")" != "$(cat "$main/image_types.ae" "$main/image.bin" | sha256sum | cut -c1-64)" ]; then
    arbace_stale "$who" "$elf" "not built from the program of the last bin/arbace-go --build (its image.key)" "$cmd"
  fi
}
