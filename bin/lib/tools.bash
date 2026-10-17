# Shared by the scripts that run Arbace's tools and checks (bin/c2a, bin/clojure-tests, bin/repr-go's
# checks, bin/a2g-check, ...). Sourced, with $root set to the repository root. Defines:
#
#   arbace_tool [-p DIR]... [-s NS]... [-D NAME=VALUE]... ARG...
#                   runs Arbace's Go executable, the host of the tools (bin/lib/host.bash's
#                   arbace_host_exe: ARBACE_HOST_EXE, else stage 0 built from the Go seed), with
#                   ARG... (e.g. -m NS ARGS..., -e EXPR, a script), from the current directory.
#                   ARBACE_PATH is the checkout ($root), then each -p DIR (test/, a scratch tree),
#                   then the caller's ARBACE_PATH. The tools a2g, repr/go, c2a and arbace.aot are
#                   embedded in the executable and compiled since standalone S6 (SA133); a source
#                   of theirs on ARBACE_PATH that differs from the embedded copy is loaded from it
#                   instead, evaluated (RT.toolSource), so a change in the checkout runs until the
#                   next build. The rest (arbace.repr.java, test/'s) is not embedded and loads from the
#                   checkout's sources, as all the tools did on the JVM's target/stage2 until S4
#                   and on the executable until S6.
#                   -s NS loads an embedded namespace from the checkout instead (its file under
#                   $root, by load-file, then marked loaded, so that requires keep it): the
#                   executable's embedded sources come before ARBACE_PATH, and a tool embedded
#                   with them (c2a) must run as checked out, not as the executable was built; the
#                   namespaces in the order of their requires. -D sets a system property before
#                   ARG... (as the JVM's -D did; the executable takes no -D). The exit status is
#                   the executable's.
#   c2a_sources     the arguments -s NS... of arbace_tool for c2a (doc/C2A.md), embedded in the
#                   executable, from the checkout
#   frozen_clojure  prints the directory holding the frozen reference tree clojure/ (upstream
#                   Clojure and ASM sources with their javac classes), extracted once from the
#                   freeze tag arbace-for-java-26-v1 into .tmp/frozen/: repr/java's
#                   regression corpus, the class forms tests' javac classes, the reference runs
#                   of Clojure's suite. Re-extracted when the tag
#                   moves. Its classes are dated after its sources, so javac does not recompile
#                   them.

c2a_sources=(-s arbace.c2a.text -s arbace.c2a.rename -s arbace.c2a.rules -s arbace.c2a.main)

arbace_tool() {
  local exe path=$root init=() srcs="" kv f n
  while [ $# -gt 0 ]; do
    case $1 in
      -p) path=$path:$2; shift 2 ;;
      -s) f=$root/$(echo "$2" | tr .- /_).ae
          [ -f "$f" ] || { echo "arbace_tool: no $f for $2" >&2; return 2; }
          srcs="$srcs [$2 \"$f\"]"; shift 2 ;;
      -D) kv=$2; n=${kv%%=*}
          init+=(-e "(do (System/setProperty \"$n\" \"$(printf '%s' "${kv#*=}" | sed 's/\\/\\\\/g; s/"/\\"/g')\") nil)")
          shift 2 ;;
      *) break ;;
    esac
  done
  [ -n "$srcs" ] && init+=(-e "(doseq [[n f] '[$srcs]] (load-file f) (dosync (commute @#'arbace.core/*loaded-libs* conj n)))")
  . "$root/bin/lib/host.bash"
  exe=$(arbace_host_exe) || return 1
  ARBACE_PATH=$path${ARBACE_PATH:+:$ARBACE_PATH} "$exe" "${init[@]}" "$@"
}

frozen_clojure() {
  local tag=arbace-for-java-26-v1 dir=$root/.tmp/frozen rev tmp
  if ! rev=$(git -C "$root" rev-parse -q --verify "refs/tags/$tag^{commit}"); then
    echo "the frozen clojure/ comes from the tag $tag, which this repository lacks:" \
         "git fetch origin tag $tag" >&2
    return 1
  fi
  mkdir -p "$root/.tmp"
  # one extraction at a time: concurrent checks (bin/gate) share .tmp/frozen
  exec 8> "$root/.tmp/frozen.lock"
  flock 8
  if [ "$(cat "$dir/.rev" 2>/dev/null)" != "$rev" ]; then
    tmp=$(mktemp -d "$root/.tmp/frozen.XXXXXX") || { flock -u 8; return 1; }
    if ! git -C "$root" archive --format=tar "$rev" clojure | tar -x -C "$tmp"; then
      echo "could not extract clojure/ from $tag" >&2
      rm -rf "$tmp"
      flock -u 8
      return 1
    fi
    find "$tmp/clojure" -name '*.class' -exec touch {} +
    echo "$rev" > "$tmp/.rev"
    rm -rf "$dir"
    mv -T "$tmp" "$dir" || { rm -rf "$tmp"; flock -u 8; return 1; }
  fi
  flock -u 8
  echo "$dir"
}
