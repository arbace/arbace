# Shared by the scripts that run Arbace's tools and checks (bin/j2c, bin/class-forms-tests,
# bin/clojure-tests, ...). Sourced, with $root set to the repository root. Defines:
#
#   arbace_runtime  sets ARBACE_CP, the class path of the runtime the tools run on, for
#                   `java -cp "$ARBACE_CP:..." arbace.lang.Main`: target/stageN (default stage 2,
#                   ARBACE_STAGE=N for another), built by bin/build-arbace, then the repository
#                   root. The stage holds the class forms (arbace.lang, arbace.asm, ...) and its
#                   namespaces AOT-compiled; the tools' own namespaces (arbace.j2c, test/) load
#                   from source. RT.load takes a namespace's __init class only when it is newer
#                   than its source, so a namespace edited since the build (arbace.classes when
#                   working on the compiler) loads from the checkout's source, the rest from the
#                   stage's classes. Not target/arbace.jar: its own copies of the sources would
#                   shadow the checkout's (edits silently ignored), and its AOT cache applies
#                   only to the jar alone anyway.
#   frozen_clojure  prints the directory holding the frozen reference tree clojure/ (upstream
#                   Clojure and ASM sources with their javac classes), extracted once from the
#                   freeze tag arbace-for-java-26-v1 into .tmp/frozen/ (doc/FREEZE.md): j2c's
#                   regression corpus, the class forms tests' javac classes, the reference runs
#                   of Clojure's suite and the benchmarks' baseline. Re-extracted when the tag
#                   moves. Its classes are dated after its sources, so javac does not recompile
#                   them.

arbace_runtime() {
  local stage=${ARBACE_STAGE:-2} dir newer
  dir=$root/target/stage$stage
  if [ ! -f "$dir/arbace/lang/Main.class" ] || [ ! -f "$dir/arbace/core__init.class" ]; then
    echo "no $dir: run bin/build-arbace first" >&2
    return 1
  fi
  # The class forms of the runtime itself never load from source: say when they are stale
  newer=$(cd "$root" && find arbace -name '*.clj' ! -path 'arbace/classes/*' ! -path 'arbace/j2c/*' \
            -newer "$dir/arbace/core__init.class" -print -quit)
  if [ -n "$newer" ]; then
    echo "warning: $newer is newer than target/stage$stage; run bin/build-arbace" >&2
  fi
  ARBACE_CP=$dir:$root
}

frozen_clojure() {
  local tag=arbace-for-java-26-v1 dir=$root/.tmp/frozen rev tmp
  if ! rev=$(git -C "$root" rev-parse -q --verify "refs/tags/$tag^{commit}"); then
    echo "the frozen clojure/ comes from the tag $tag, which this repository lacks:" \
         "git fetch origin tag $tag" >&2
    return 1
  fi
  if [ "$(cat "$dir/.rev" 2>/dev/null)" != "$rev" ]; then
    mkdir -p "$root/.tmp"
    tmp=$(mktemp -d "$root/.tmp/frozen.XXXXXX") || return 1
    if ! git -C "$root" archive --format=tar "$rev" clojure | tar -x -C "$tmp"; then
      echo "could not extract clojure/ from $tag" >&2
      rm -rf "$tmp"
      return 1
    fi
    find "$tmp/clojure" -name '*.class' -exec touch {} +
    echo "$rev" > "$tmp/.rev"
    rm -rf "$dir"
    mv -T "$tmp" "$dir" || { rm -rf "$tmp"; return 1; }
  fi
  echo "$dir"
}
