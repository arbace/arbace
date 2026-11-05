# C's installed headers as a pack (doc/ORACLE-RECORDS-SURVEY.md, ORS9 (a); "As built: the tooling and C"): sourced by
# bin/repr-c and bin/ci after $root, bin/lib/tmpdir.bash and test/oracle-records/oracle.bash.
#
# The headers C's checks read at the paths of test/c/host/search.txt (gcc's include directory, /usr/include/fortify,
# /usr/include: gcc's, musl-dev's, fortify-headers' and the few other packages' test/c/host/headers.sha256 names) are the
# pack c-headers-gcc15 (test/c/records/c-headers-gcc15.sha256, its manifest .tsv beside it; bin/repr-c host pack makes it on
# the host with gcc). ARBACE_C_SYSROOT names the directory that holds them under their absolute paths, with roots.txt
# (search.txt's directories): repr/c's preprocessor (arbace.repr.c.pp's resolve-file) and tools/ccdump read a file under one of
# those roots from it, never from the host, and report it under its own path (so every output is the host's, byte for byte).
#
#   c_sysroot   exports ARBACE_C_SYSROOT unless set ("/": the host's own files, as before the pack): the pack unpacked from
#               the store; when the store lacks it, the same members staged from the host's files when each has the
#               manifest's digest (the checkout's .tmp/c-sysroot/MANIFEST-SHA); else 1 with the reason
#   Until arbace.repr.c.pp reads ARBACE_C_SYSROOT (its change is a commit of its own, which changes gate_key and so lands with
#   the next change that rekeys every build: doc/ORACLE-RECORDS-SURVEY.md, "As built: the tooling and C"), repr/c reads the
#   host's headers: c_sysroot then requires each of the pack's members the host has to be the pack's, so that a step keyed by
#   the pack's pin reads what the pin names (tools/ccdump reads the pack already).
c_sysroot() {
  [ -n "${ARBACE_C_SYSROOT:-}" ] && return 0
  local pin=$root/test/c/records/c-headers-gcc15.sha256 man d rc st id
  man=$(oracle_manifest "$pin")
  [ -f "$pin" ] && [ -f "$man" ] || { echo "c: no headers pack pinned ($pin): ARBACE_C_SYSROOT=/ reads the host's" >&2; return 1; }
  if ! grep -q ARBACE_C_SYSROOT "$root/arbace/repr/c/pp.ae"; then
    awk -F'\t' '!/^#/ && NF && $1 != "roots.txt" {print $3 "  " $1}' "$man" | while read -r want id; do
      [ ! -f "$id" ] || [ "$(sha256sum < "$id" | cut -c1-64)" = "$want" ] || echo "$id"; done > "$TMPDIR/c-sysroot.host"
    [ ! -s "$TMPDIR/c-sysroot.host" ] || { echo "c: repr/c reads the host's headers until arbace.repr.c.pp takes ARBACE_C_SYSROOT, and $(wc -l < "$TMPDIR/c-sysroot.host") of them are not the pack's ($(head -1 "$TMPDIR/c-sysroot.host"))" >&2; return 1; }
  fi
  if d=$(oracle_unpack "$pin" 2>"$TMPDIR/c-sysroot.err"); then export ARBACE_C_SYSROOT=$d/c-headers-gcc15; return 0; else rc=$?; fi
  [ $rc = 2 ] || { cat "$TMPDIR/c-sysroot.err" >&2; return 1; }
  # the store lacks the pack: the host's files, when each is the manifest's, staged as the pack would unpack
  d=$root/.tmp/c-sysroot/$(sha256sum < "$man" | cut -c1-16)
  if [ ! -f "$d.ok" ]; then
    st=$d.$$; rm -rf "$st"; mkdir -p "$st"
    while IFS=$'\t' read -r id _; do
      case $id in '#'*|'') continue ;; roots.txt) awk 'NF' "$root/test/c/host/search.txt" > "$st/roots.txt"; continue ;; esac
      [ -f "$id" ] || { echo "c: no headers pack in $(oracle_store) and the host lacks $id (bin/oracle-records fetch c-headers-gcc15)" >&2; rm -rf "$st"; return 1; }
      mkdir -p "$st$(dirname "$id")"; cp "$id" "$st$id"
    done < "$man"
    oracle_check_members "$st" "$man" 2>"$TMPDIR/c-sysroot.err" \
      || { echo "c: no headers pack in $(oracle_store), and the host's headers are not the pack's ($(head -1 "$TMPDIR/c-sysroot.err")): bin/oracle-records fetch c-headers-gcc15" >&2; rm -rf "$st"; return 1; }
    rm -rf "$d"; mv "$st" "$d"; : > "$d.ok"
  fi
  export ARBACE_C_SYSROOT=$d
}
