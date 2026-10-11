# Arbace's version (doc/VENDOR-NOTES.md, hand change 20), for bin/build-arbace (the resource
# arbace/version.edn of every stage and of the jar) and bin/arbace-go (the Go program's
# arbaceVersion, set with go build -ldflags -X). Sourced. Defines:
#
#   arbace_version ROOT  prints the version map, read by *arbace-version*:
#                          {:describe "arbace-for-go1.27.1-v1-6-ge2a9f6b" :commit "<40 hex>" :dirty false}
#                        In a git checkout whose top level is ROOT: git describe --tags --always
#                        --dirty, git rev-parse HEAD, and :dirty when describe ends -dirty
#                        (tracked files modified; the gitignored target/ and .tmp/ do not
#                        count). Otherwise (a tarball, possibly unpacked inside another git
#                        repository, as abuild's src/ in aports) ROOT/VERSION as git archive
#                        filled it (export-subst): :dirty false, :describe the short commit
#                        when no tag describes it. Neither: {:describe "unknown" :commit nil
#                        :dirty nil}.
#
# The describe takes every tag (no --match): main descends from the freezes, so it reads
# <last freeze tag>-<commits since>-g<hash> until main has a tag of its own, which then wins as
# the nearest; the frozen branches' later tags (v2, v3 on arbace-for-java-26) are not ancestors
# of main and never show.

arbace_version() {
  local r top d c dirty=false
  r=$(cd "$1" && pwd -P) || return 1
  top=$(git -C "$r" rev-parse --show-toplevel 2>/dev/null) && top=$(cd "$top" && pwd -P)
  if [ -n "$top" ] && [ "$top" = "$r" ] && c=$(git -C "$r" rev-parse HEAD 2>/dev/null); then
    d=$(git -C "$r" describe --tags --always --dirty) || return 1
    case $d in *-dirty) dirty=true ;; esac
  elif [ -f "$r/VERSION" ] && ! grep -q '\$Format:' "$r/VERSION"; then
    c=$(sed -n 's/^commit //p' "$r/VERSION")
    d=$(sed -n 's/^describe //p' "$r/VERSION")
    [ -n "$d" ] || d=${c:0:7}
  else
    printf '{:describe "unknown" :commit nil :dirty nil}\n'
    return 0
  fi
  printf '{:describe "%s" :commit "%s" :dirty %s}\n' "$d" "$c" "$dirty"
}
