# repr/go's host: what runs repr/go's namespaces (arbace.repr.go, .print, .read) and the build's
# (arbace.build.go) for bin/repr-go and bin/build-go. Sourced, with $root set to the repository
# root. Defines:
#
#   repr_go_exec NS ARG...  replaces the shell by -m NS ARG... on Arbace's Go executable (standalone
#                   S3 and S4, doc/STANDALONE-NOTES.md), the checkout first on ARBACE_PATH:
#                   arbace.repr.go and arbace.build.go are embedded in the executable and
#                   compiled since S6 (SA133; until 2026-10-17 as arbace.g2a), and a
#                   source of it in the checkout that differs from the embedded copy is loaded
#                   from the checkout instead, evaluated (RT.toolSource; until S6 all of it loaded
#                   from the checkout). The executable is REPR_GO_EXE, else the tools' host
#                   (bin/lib/host.bash: ARBACE_HOST_EXE, else stage 0 built from the Go seed), run from a copy in
#                   .tmp/repr-go-host/ refreshed when it differs: a build of the executable
#                   (bin/arbace-go --build, REPR_GO_EXE=target/arbace-go/<arch>/arbace) may
#                   replace it while the copy runs. The collector's settings are the
#                   executable's (GOGC 200 unless set: GOGC=400 saves about 13% of the CPU
#                   time, not of the wall time, for a third more memory; doc/STANDALONE-NOTES.md).
#   repr_go_run NS ARG...   the same, as a command of the shell (not replacing it)

# repr_go_host_exe: prints the path of the copy of the Go executable to run (made or refreshed)
repr_go_host_exe() {
  local exe dir=$root/.tmp/repr-go-host
  . "$root/bin/lib/host.bash"
  exe=${REPR_GO_EXE:-$(arbace_host_exe)} || return 1
  if [ ! -x "$exe" ]; then
    echo "repr/go: no executable $exe (REPR_GO_EXE)" >&2
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

repr_go_run() {
  local ns=$1 exe a
  shift
  exe=$(repr_go_host_exe) || return 1
  # build --print-cache (standalone S6): the host's identity keys the cache of printed
  # packages, the executable's hash and that of repr/go's and the build's sources in the checkout (a tool source
  # there that differs from the executable's own is loaded from it)
  for a in "$@"; do
    if [ "$a" = --print-cache ]; then
      REPR_GO_HOST_ID=$( (sha256sum < "$exe"; cat "$root"/arbace/repr/go.ae "$root"/arbace/repr/go/*.ae "$root"/arbace/build/*.ae | sha256sum) | sha256sum | cut -c1-64)
      export REPR_GO_HOST_ID
      break
    fi
  done
  # repr_go_heavy set (the native reader's and the converter's runs, bin/repr-go): under the host's
  # big-job lock when the gate asks for it (GTS31 (b), bin/lib/heavy.bash)
  if [ -n "${repr_go_heavy:-}" ]; then
    . "$root/bin/lib/heavy.bash"
    ARBACE_PATH=$root${ARBACE_PATH:+:$ARBACE_PATH} heavy_run "$exe" -m "$ns" "$@"
    return
  fi
  ARBACE_PATH=$root${ARBACE_PATH:+:$ARBACE_PATH} "$exe" -m "$ns" "$@"
}

repr_go_exec() {
  local s=0
  repr_go_run "$@" || s=$?
  exit $s
}
