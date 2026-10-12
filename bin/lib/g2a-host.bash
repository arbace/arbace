# g2a's host: what runs g2a's namespaces (arbace.g2a.build, .print, .main, .read) for bin/g2a and
# bin/g2a-print. Sourced, with $root set to the repository root. Defines:
#
#   g2a_exec NS ARG...  replaces the shell by -m NS ARG... on Arbace's Go executable (standalone
#                   S3 and S4, doc/STANDALONE-NOTES.md), the checkout first on ARBACE_PATH, so
#                   that arbace.g2a (not embedded in the executable) loads from the checkout's
#                   sources. The executable is G2A_GO_EXE, else the tools' host (bin/lib/host.bash:
#                   ARBACE_HOST_EXE, else stage 0 built from the Go seed), run from a copy in
#                   .tmp/g2a-host/ refreshed when it differs: a build of the executable
#                   (bin/arbace-go --build, G2A_GO_EXE=target/arbace-go/<arch>/arbace) may
#                   replace it while the copy runs. The collector's settings are the
#                   executable's (GOGC 200 unless set: GOGC=400 saves about 13% of the CPU
#                   time, not of the wall time, for a third more memory; doc/STANDALONE-NOTES.md).
#                   G2A_HOST, which chose the JVM until S4, may only be go (or unset).
#   g2a_run NS ARG...   the same, as a command of the shell (not replacing it)

# g2a_go_exe: prints the path of the copy of the Go executable to run (made or refreshed)
g2a_go_exe() {
  local exe dir=$root/.tmp/g2a-host
  case ${G2A_HOST:-go} in
    go) ;;
    *) echo "G2A_HOST: g2a runs on the Go executable only (the JVM host left with standalone S4), not $G2A_HOST" >&2; return 2 ;;
  esac
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
  local ns=$1 exe
  shift
  exe=$(g2a_go_exe) || return 1
  ARBACE_PATH=$root${ARBACE_PATH:+:$ARBACE_PATH} "$exe" -m "$ns" "$@"
}

g2a_exec() {
  local s=0
  g2a_run "$@" || s=$?
  exit $s
}
