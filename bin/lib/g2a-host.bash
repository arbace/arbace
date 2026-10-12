# g2a's host: what runs g2a's namespaces (arbace.g2a.build, .print, .main, .read) for bin/g2a and
# bin/g2a-print. Sourced, with $root set to the repository root. Defines:
#
#   g2a_exec HEAP NS ARG...  replaces the shell by -m NS ARG... on the host G2A_HOST names:
#                   jvm (the default): Arbace on the JVM, target/stage2 plus the checkout
#                   (bin/lib/tools.bash), with G2A_JVM_OPTS or else HEAP (e.g. -Xmx2g) and a 64 MB
#                   stack;
#                   go: the native Go executable (standalone S3, doc/STANDALONE-NOTES.md), the
#                   checkout first on ARBACE_PATH, so that arbace.g2a (not embedded in the
#                   executable) loads from the checkout's sources, as on the JVM. The executable
#                   is G2A_GO_EXE (default target/arbace-go/<host arch>/arbace, built by
#                   bin/arbace-go --build), run from a copy in .tmp/g2a-host/ refreshed when it
#                   differs: a build of the executable (bin/arbace-go --build on this host)
#                   replaces it while the copy runs. The collector's settings are the
#                   executable's (GOGC 200 unless set: GOGC=400 saves about 13% of the CPU
#                   time, not of the wall time, for a third more memory; doc/STANDALONE-NOTES.md).
#   g2a_run HEAP NS ARG...  the same, as a command of the shell (not replacing it)
#   g2a_host        prints the host: jvm or go (fails on another value of G2A_HOST)

g2a_host() {
  case ${G2A_HOST:-go} in
    jvm|go) echo "${G2A_HOST:-go}" ;;
    *) echo "G2A_HOST is jvm or go, not $G2A_HOST" >&2; return 2 ;;
  esac
}

# g2a_go_exe: prints the path of the copy of the Go executable to run (made or refreshed)
g2a_go_exe() {
  local exe dir=$root/.tmp/g2a-host
  . "$root/bin/lib/host.bash"
  exe=${G2A_GO_EXE:-$(arbace_host_exe)} || return 1
  if [ ! -x "$exe" ]; then
    echo "g2a: no executable $exe (G2A_GO_EXE)" >&2
    return 1
  fi
  mkdir -p "$dir"
  (
    # one refresh at a time (concurrent printings share the copy); a running copy is replaced
    # by a rename, never written in place
    flock 9
    if ! cmp -s "$exe" "$dir/arbace"; then
      cp "$exe" "$dir/arbace.new.$$" && mv -f "$dir/arbace.new.$$" "$dir/arbace"
    fi
  ) 9> "$dir/lock" || return 1
  echo "$dir/arbace"
}

g2a_run() {
  local heap=$1 ns=$2 host exe
  shift 2
  host=$(g2a_host) || return 2
  if [ "$host" = go ]; then
    exe=$(g2a_go_exe) || return 1
    ARBACE_PATH=$root${ARBACE_PATH:+:$ARBACE_PATH} "$exe" -m "$ns" "$@"
    return
  fi
  . "$root/bin/lib/tools.bash"
  arbace_runtime || return 1
  # shellcheck disable=SC2086
  java ${G2A_JVM_OPTS:-$heap} -Xss64m -cp "$ARBACE_CP" arbace.lang.Main -m "$ns" "$@"
}

g2a_exec() {
  local s=0
  g2a_run "$@" || s=$?
  exit $s
}
