# The frozen JVM Arbace (standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM removed"): what
# stays on a JVM after S4 runs on the jar of the frozen branch's last release, built from its
# tag and pinned by hash, never on a JVM build of the checkout (main has none). It is the
# oracle's reference (bin/oracle) and the host of the JVM-side tools: j2a (javac is its front
# end), the edge's recording (bin/a2g-edge: the JVM's reflection), the data generators
# (bin/jrt testdata|manifest|standins, bin/jrt-coverage, bin/net-check). Sourced, with $root
# set to the repository root. Defines:
#
#   frozen_jar     prints the path of the frozen jar: FROZEN_JAR (or ARBACE_ORACLE_JAR) when
#                  set, unchecked; else the jar of the tag FROZEN_TAG (or ARBACE_ORACLE_TAG,
#                  default arbace-for-java-26-v4), built once from the tag (the tag's own
#                  bin/build-arbace in a temporary git worktree, a few minutes) into
#                  .tmp/oracle-frozen/TAG.jar and checked against its pinned SHA-256,
#                  test/oracle/frozen-jar.sha256 (sha256sum's format, one line per tag)
#   frozen_view K  prints a directory to put before the jar on the class path: the checkout's
#                  namespaces the frozen jar is to load from source, as .clj links (its loader
#                  reads .clj only, not .ae), .tmp/frozen-view/K:
#                    test   test/'s namespaces (the oracle's runner, jrt's generators, ...)
#                    tools  those, the tools' namespaces (arbace.j2a, arbace.a2g, arbace.c2a,
#                           arbace.g2a), the class forms' front end (arbace.classes.types, env,
#                           parse, lower, analyze) and every namespace of the checkout the jar
#                           lacks (arbace.box, a JVM-only part of env, the access flags ...)
#                  arbace.core, the runtime (the jar's class forms: arbace.lang, arbace.asm)
#                  and every other namespace stay the jar's. The jar's sources and __init
#                  classes are dated 2000-01-01, the links take their target's date: RT.load
#                  loads a namespace from its source when the source is newer than its __init
#                  class, so a linked namespace loads from the checkout. Not linked:
#                  arbace.classes.native (the jar's Compiler hands class forms in code to it,
#                  which needs the jar's own back end, emit and compiler: tools' code that
#                  uses class forms compiles with the jar's compiler, front end included,
#                  unchanged) and the back end (boot, compiler, emit, shape, verify, build)
#   frozen_host    the jar as provenance files name it: "NAME SHA-256" (arbace-for-java-26-v4 ...)
#   frozen_java    runs java on the frozen jar: frozen_java K [JVM-OPTION...] -- [ARG...] runs
#                  `java JVM-OPTION... -cp VIEW:JAR[:FROZEN_CP] arbace.lang.Main ARG...` with
#                  frozen_view K (K: jar for the jar alone, test, tools)

frozen_jar() {
  if [ -n "${FROZEN_JAR:-${ARBACE_ORACLE_JAR:-}}" ]; then
    local j=${FROZEN_JAR:-$ARBACE_ORACLE_JAR}
    case $j in /*) ;; *) j=$root/$j ;; esac
    [ -f "$j" ] || { echo "no frozen jar $j (FROZEN_JAR)" >&2; return 1; }
    echo "$j"; return
  fi
  local tag=${FROZEN_TAG:-${ARBACE_ORACLE_TAG:-arbace-for-java-26-v4}} dir=$root/.tmp/oracle-frozen wt st want got
  local jar=$dir/$tag.jar
  mkdir -p "$dir"
  (
    flock 9
    if [ ! -f "$jar" ]; then
      wt=$dir/wt
      git -C "$root" worktree remove --force "$wt" 2> /dev/null; rm -rf "$wt"
      git -C "$root" worktree add --detach "$wt" "$tag" > /dev/null 2>&1 || { echo "frozen_jar: no worktree of $tag" >&2; exit 1; }
      find "$wt/clojure" -name '*.class' -exec touch {} +
      echo "frozen_jar: building the frozen jar of $tag (log $dir/$tag.log)" >&2
      "$wt/bin/build-arbace" > "$dir/$tag.log" 2>&1; st=$?
      [ $st = 0 ] && cp "$wt/target/arbace.jar" "$jar.new" && mv "$jar.new" "$jar"
      git -C "$root" worktree remove --force "$wt"
      [ $st = 0 ] || { echo "frozen_jar: the build of $tag failed, see $dir/$tag.log" >&2; exit 1; }
    fi
  ) 9> "$dir/.lock" || return 1
  want=$(sed -n "s/^\([0-9a-f]\{64\}\)  $tag.jar\$/\1/p" "$root/test/oracle/frozen-jar.sha256" 2>/dev/null)
  got=$(sha256sum "$jar" | cut -d' ' -f1)
  if [ -z "$want" ]; then
    echo "frozen_jar: warning: no pinned hash for $tag.jar in test/oracle/frozen-jar.sha256 (it is $got)" >&2
  elif [ "$want" != "$got" ]; then
    echo "frozen_jar: $jar: SHA-256 $got, pinned $want" >&2; return 1
  fi
  echo "$jar"
}

# frozen_link DIR FILE: DIR/FILE's .clj link to the checkout's FILE (.ae), unless there
frozen_link() {
  local l=$1/${2%.ae}.clj
  [ "$(readlink "$l" 2>/dev/null)" = "$3/$2" ] && return
  mkdir -p "$(dirname "$l")"
  ln -sfn "$3/$2" "$l"
}

frozen_view() {
  local kind=$1 jar view list f
  view=$root/.tmp/frozen-view/$kind
  mkdir -p "$view"
  [ "$kind" = jar ] && { echo "$view"; return; }
  jar=$(frozen_jar) || return 1
  (
    # one refresh at a time; links are added and stale ones removed, never the whole view, so
    # that a tool running on it meanwhile keeps its files
    flock 9
    list=$root/.tmp/frozen-view/jar.list
    if [ ! -s "$list" ] || [ "$jar" -nt "$list" ]; then
      unzip -Z1 "$jar" > "$list.new" && mv "$list.new" "$list" || exit 1
    fi
    find -L "$view" -type l -delete 2> /dev/null   # links whose target is gone
    (cd "$root/test" && find . -name '*.ae' -printf '%P\n') | while read -r f; do
      frozen_link "$view" "$f" "$root/test"
    done
    if [ "$kind" = tools ]; then
      (cd "$root" && find arbace -name '*.ae' ! -path 'arbace/lang/go/*' | LC_ALL=C sort) | awk -v list="$list" '
        BEGIN { while ((getline l < list) > 0) jar[l] = 1 }
        { b = substr($0, 1, length($0) - 3) }
        b ~ /^arbace\/(j2a|a2g|c2a|g2a)\// { print; next }
        b ~ /^arbace\/classes\/(types|env|parse|lower|analyze)$/ { print; next }
        b ~ /^arbace\/classes\/(native|boot|compiler|emit|shape|verify|build)$/ { next }
        !((b ".clj") in jar) && !((b "__init.class") in jar) { print }' |
        while read -r f; do frozen_link "$view" "$f" "$root"; done
    fi
  ) 9> "$root/.tmp/frozen-view/$kind.lock" || return 1
  echo "$view"
}

frozen_java() {
  local kind=$1 jar view opts=()
  shift
  jar=$(frozen_jar) || return 1
  view=$(frozen_view "$kind") || return 1
  while [ $# -gt 0 ] && [ "$1" != -- ]; do opts+=("$1"); shift; done
  [ $# -gt 0 ] && shift
  java "${opts[@]}" -cp "$view:$jar${FROZEN_CP:+:$FROZEN_CP}" arbace.lang.Main "$@"
}

# frozen_host: the frozen jar as provenance records name it: its file's name and its SHA-256
frozen_host() {
  local j
  j=$(frozen_jar) || return 1
  echo "$(basename "$j" .jar) $(sha256sum "$j" | cut -c1-64)"
}
