# The platform's kind and the generated sources' root (doc/classes/JAVA-FRONTEND-SURVEY.md, "Own platform", JOP2 (a),
# JOP5 (a)): sourced, after setting root (the checkout); jdk (jdk26u, default $JDK or /root/jdk26u) is read when set.
# Out of every key (bin/lib/repr-java/), as the checks that source it.
#
#   repr_java_platform_kind      own when jdk/javac/platform.sha256 says "# kind: own" (the own platform, Arbace's: the switch
#                                of JOP4 (a) rewrites the pin), else build (the JDK build tree's exploded modules)
#   repr_java_gensrc_root        the generated sources' root, MODULE/... below it: for the build kind the JDK build tree's
#                                support/gensrc, for the own kind the committed pack jdk/javac/platform-gensrc.tar.xz unpacked
#                                once into .tmp/javac-platform-gensrc/PACK20 (seconds; no platform or javac needed); status 1
#                                with a message when there is none
#   repr_java_platform_record F  the own kind's recorded input F (jdk/javac/platform-records/F): a file jdk/'s conversion takes
#                                from the JDK build and Arbace does not make yet (JOP10, proposed), else the build tree's
repr_java_platform_kind() {
  if grep -qx '# kind: own' "$root/jdk/javac/platform.sha256" 2>"${TMPDIR:-$root/.tmp/tmp}/repr-java-platform.quiet"; then echo own; else echo build; fi
}

repr_java_gensrc_root() {
  local j=${jdk:-${JDK:-/root/jdk26u}} x p d h
  if [ "$(repr_java_platform_kind)" = build ]; then
    for x in "$j"/build/*/support/gensrc; do if [ -e "$x" ]; then echo "$x"; return 0; fi; done
    echo "repr_java_gensrc_root: no JDK build tree's support/gensrc under $j/build" >&2; return 1
  fi
  p=$root/jdk/javac/platform-gensrc.tar.xz
  [ -f "$p" ] || { echo "repr_java_gensrc_root: no $p (the own generated sources)" >&2; return 1; }
  h=$(sha256sum < "$p" | cut -c1-20)
  d=$root/.tmp/javac-platform-gensrc/$h
  if [ ! -f "$d/ok" ]; then
    mkdir -p "$root/.tmp/javac-platform-gensrc"
    (
      flock 9
      [ -f "$d/ok" ] && exit 0
      rm -rf "$d.new"; mkdir -p "$d.new" && xz -dc "$p" | tar -x -C "$d.new" && rm -rf "$d" && mv "$d.new" "$d" && touch "$d/ok"
    ) 9> "$root/.tmp/javac-platform-gensrc/lock" || { echo "repr_java_gensrc_root: $p not unpacked" >&2; return 1; }
  fi
  echo "$d"
}

repr_java_platform_record() {
  local j=${jdk:-${JDK:-/root/jdk26u}} x
  if [ "$(repr_java_platform_kind)" = own ]; then echo "$root/jdk/javac/platform-records/$1"; return 0; fi
  for x in "$j"/build/*/; do if [ -e "$x/spec.gmk" ]; then echo "${x%/}/$2"; return 0; fi; done
  return 1
}
