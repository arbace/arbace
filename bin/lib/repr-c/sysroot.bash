# C's target and sysroot (c/README.md; doc/C23-SURVEY.md, "As built: C's own target"): sourced by bin/repr-c, bin/ci and
# bin/lib/ci/musl after $root and bin/lib/tmpdir.bash. ARBACE_C_TARGET=arbace (unset) reads Arbace's own description c/target
# and headers c/include with musl 1.2.6's from its release tarball, no file of a toolchain; ARBACE_C_TARGET=gcc reads gcc's
# recorded answers test/c/host and gcc's installed headers as the pack c-headers-gcc15 (doc/ORACLE-RECORDS-SURVEY.md, ORS9 (a),
# "As built: the tooling and C"; the oracle tier's), ARBACE_C_SYSROOT=/ the host's own files in place, as before the pack.
#   c_target_host     exports ARBACE_C_HOST, the description every reader reads (arbace.repr.c.host/load-host)
#   c_sysroot         exports it and, under Arbace's target, ARBACE_C_SYSROOT: the sysroot c_arbace_sysroot assembles, which
#                     repr/c's preprocessor (arbace.repr.c.pp's sysroot-file) and tools/ccdump (sysroot.go) read a file under
#                     its roots.txt from, never from the host, each keeping its own name in every output
c_target_host() {
  [ -n "${ARBACE_C_HOST:-}" ] && return 0
  case ${ARBACE_C_TARGET:-arbace} in
    arbace) export ARBACE_C_HOST=$root/c/target ;;
    gcc) export ARBACE_C_HOST=$root/test/c/host ;;
    *) echo "c: ARBACE_C_TARGET is arbace or gcc, not $ARBACE_C_TARGET" >&2; return 1 ;;
  esac
}

# c_arbace_sysroot: Arbace's sysroot in .tmp/c-target/KEY (KEY: c/include, c/target's search list and libraries, musl's
# prepared tree's headers): roots.txt (c/target/search.txt), /usr/lib/arbace/include (c/include), /usr/include as musl's
# `make install-headers` lays it out (include/*.h and include/*/*.h, then bits/ from arch/generic, arch/x86_64 over it and
# obj/include/bits' generated alltypes.h and syscall.h), and the libraries' headers of c/target/libraries.txt (the host's
# packages' files, each with the digest test/c/host/headers.sha256 records; left out, and said, when the host lacks them)
c_arbace_sysroot() {
  local m=$root/.tmp/ci-musl/musl-1.2.6 k d st f id want
  "$root/bin/ci" musl prepare > "$TMPDIR/c-target-musl.out" 2>&1 || { cat "$TMPDIR/c-target-musl.out" >&2; return 1; }
  k=$( { (cd "$root" && find c/include c/target/search.txt c/target/libraries.txt -type f | LC_ALL=C sort | xargs sha256sum)
         (cd "$m" && find include arch/generic/bits arch/x86_64/bits obj/include/bits -type f -name '*.h' | LC_ALL=C sort | xargs sha256sum); } \
       | sha256sum | cut -c1-16)
  d=$root/.tmp/c-target/$k
  if [ ! -f "$d.ok" ]; then
    st=$d.$$; rm -rf "$st"; mkdir -p "$st/usr/lib/arbace/include" "$st/usr/include/bits"
    awk 'NF' "$root/c/target/search.txt" > "$st/roots.txt"
    cp -R "$root/c/include/." "$st/usr/lib/arbace/include/"
    (cd "$m/include" && find . -name '*.h' -type f | while read -r f; do mkdir -p "$st/usr/include/$(dirname "$f")"; cp "$f" "$st/usr/include/$f"; done)
    cp "$m"/arch/generic/bits/*.h "$st/usr/include/bits/"
    cp "$m"/arch/x86_64/bits/*.h "$st/usr/include/bits/"
    cp "$m"/obj/include/bits/alltypes.h "$m"/obj/include/bits/syscall.h "$st/usr/include/bits/"
    grep -v '^#' "$root/c/target/libraries.txt" | awk 'NF' | while read -r id; do
      want=$(awk -v id="$id" '$2 == id {print $1}' "$root/test/c/host/headers.sha256")
      if [ -f "$id" ] && [ -n "$want" ] && [ "$(sha256sum < "$id" | cut -c1-64)" = "$want" ]; then mkdir -p "$st$(dirname "$id")"; cp "$id" "$st$id"
      else echo "c: $id is not the host's with test/c/host/headers.sha256's digest: left out of the sysroot" >&2; fi
    done
    rm -rf "$d"; mv "$st" "$d"; : > "$d.ok"
  fi
  export ARBACE_C_SYSROOT=$d
}

# c_gcc_sysroot (ORS9 (a), from mac/oracle-records-c): the headers C's checks read at the paths of test/c/host/search.txt under
# gcc's target (gcc's include directory, /usr/include/fortify, /usr/include: gcc's, musl-dev's, fortify-headers' and the few
# other packages' test/c/host/headers.sha256 names) are the pack c-headers-gcc15 (test/c/records/c-headers-gcc15.sha256, its
# manifest .tsv beside it; bin/repr-c host pack makes it on the host with gcc), unpacked from the store; when the store lacks
# it, the same members staged from the host's files when each has the manifest's digest (.tmp/c-sysroot/MANIFEST-SHA); else 1
# with the reason
c_gcc_sysroot() {
  declare -F oracle_unpack > "$TMPDIR/c-sysroot.fn" || . "$root/test/oracle-records/oracle.bash"
  local pin=$root/test/c/records/c-headers-gcc15.sha256 man d rc st id
  man=$(oracle_manifest "$pin")
  [ -f "$pin" ] && [ -f "$man" ] || { echo "c: no headers pack pinned ($pin): ARBACE_C_SYSROOT=/ reads the host's" >&2; return 1; }
  if d=$(oracle_unpack "$pin" 2>"$TMPDIR/c-sysroot.err"); then export ARBACE_C_SYSROOT=$d/c-headers-gcc15; return 0; else rc=$?; fi
  [ $rc = 2 ] || { cat "$TMPDIR/c-sysroot.err" >&2; return 1; }
  # the store lacks the pack: the host's files, when each is the manifest's, staged as the pack would unpack
  d=$root/.tmp/c-sysroot/$(sha256sum < "$man" | cut -c1-16)
  if [ ! -f "$d.ok" ]; then
    st=$d.$$; rm -rf "$st"; mkdir -p "$st"
    while IFS=$'\t' read -r id _; do
      case $id in '#'*|'') continue ;; roots.txt) awk 'NF' "$root/test/c/host/search.txt" > "$st/roots.txt"; continue ;; esac
      [ -f "$id" ] || { echo "c: no headers pack in $(oracle_store) and the host lacks $id (bin/oracle-records fetch c-headers-gcc15; ARBACE_C_SYSROOT=/ the host's)" >&2; rm -rf "$st"; return 1; }
      mkdir -p "$st$(dirname "$id")"; cp "$id" "$st$id"
    done < "$man"
    oracle_check_members "$st" "$man" 2>"$TMPDIR/c-sysroot.err" \
      || { echo "c: no headers pack in $(oracle_store), and the host's headers are not the pack's ($(head -1 "$TMPDIR/c-sysroot.err")): bin/oracle-records fetch c-headers-gcc15" >&2; rm -rf "$st"; return 1; }
    rm -rf "$d"; mv "$st" "$d"; : > "$d.ok"
  fi
  export ARBACE_C_SYSROOT=$d
}

c_sysroot() {
  c_target_host || return 1
  [ -n "${ARBACE_C_SYSROOT:-}" ] && return 0
  if [ "${ARBACE_C_TARGET:-arbace}" = arbace ]; then c_arbace_sysroot; else c_gcc_sysroot; fi
}
