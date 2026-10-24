# The jdk corpus's originals: java.base's closure as bin/jrt-convert's sources and generate steps
# make it (jdk/sources.txt's files), the tree that bin/repr-java roundtrip, parse-check and
# front-check and bin/javac-vendor classes read. Kept between runs, made again when what it is made
# from changed, and checked against its sources whenever it is used (doc/classes/JAVA-FRONTEND-SURVEY.md,
# "Fixed (2026-10-23): mode :full's members": a tree made before a change to jrt's own Java was
# read for days). Sourced, after setting root (the checkout). Defines:
#
#   arbace_jdk_originals_key   prints the SHA-256 of what the two steps read: bin/jrt-convert, the
#                              closure (test/jrt, doc/go/java-surface.edn), jrt's own Java
#                              (overlay/jdk/java.base), jdk26u's checkout ($JDK: its commit and
#                              status) and JRT_LOCALES
#   arbace_jdk_originals_check ORIG
#                              each file jdk/sources.txt lists as copied (share, share:MODULE, unix,
#                              overlay; not the generated ones) the same in ORIG/src/java.base as its
#                              source, byte for byte; status 1 naming each stale file
#   arbace_jdk_originals ORIG LOG [OWNED]
#                              ORIG made (bin/jrt-convert into ORIG, its log LOG) when it has no
#                              src/java.base or its ORIG/originals.key is not the key, then
#                              checked, under a lock (ORIG.lock: two checks may share ORIG). OWNED
#                              non-empty: ORIG is the caller's (JRT_WORK_RT), made when missing,
#                              never removed nor keyed, but checked

arbace_jdk_originals_key() {
  local jdk=${JDK:-/root/jdk26u}
  (cd "$root" && {
    find bin/jrt-convert test/jrt doc/go/java-surface.edn overlay/jdk/java.base -type f | LC_ALL=C sort \
      | xargs -d '\n' sha256sum
    echo "jdk $jdk $(git -C "$jdk" rev-parse HEAD 2>&1) $(git -C "$jdk" status --porcelain 2>&1 | sha256sum)"
    echo "locales ${JRT_LOCALES:-}"; }) | sha256sum | cut -c1-64
}

arbace_jdk_originals_check() {
  local o=$1 jdk=${JDK:-/root/jdk26u} m kind rel s n=0
  while read -r m kind rel; do
    case $kind in
      share) s=$jdk/src/java.base/share/classes/$rel ;;
      share:*) s=$jdk/src/${kind#share:}/share/classes/$rel ;;
      unix) s=$jdk/src/java.base/unix/classes/$rel ;;
      overlay) s=$root/overlay/jdk/java.base/$rel ;;
      *) continue ;;
    esac
    cmp -s "$s" "$o/src/java.base/$rel" || { echo "the originals: $o/src/java.base/$rel is not $s"; n=$((n + 1)); }
  done < "$root/jdk/sources.txt"
  [ "$n" = 0 ] || { echo "the originals: $n files stale (remove $o)"; return 1; }
}

arbace_jdk_originals() {
  local o=$1 log=$2 owned=${3:-} key
  key=$(arbace_jdk_originals_key)
  # one maker at a time: parse-check and front-check share a tree (flock on ORIG.lock)
  mkdir -p "$(dirname "$o")"
  (flock 9
  if [ ! -d "$o/src/java.base" ] || { [ -z "$owned" ] && [ "$(cat "$o/originals.key" 2>&1)" != "$key" ]; }; then
    echo "the originals: bin/jrt-convert's sources and generate steps into $o"
    [ -n "$owned" ] || rm -rf "$o"
    JRT_WORK=$o JRT_STEPS=sources,generate "$root/bin/jrt-convert" > "$log" 2>&1 || { echo "bin/jrt-convert failed: $log"; exit 1; }
    [ -n "$owned" ] || echo "$key" > "$o/originals.key"
  fi
  arbace_jdk_originals_check "$o") 9> "$o.lock"
}
