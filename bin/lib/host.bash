# The host of Arbace's tools (standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM removed"):
# the Go executable that runs j2g, repr/go, c2a and the other tools' namespaces, loaded from the
# checkout's sources (ARBACE_PATH), as the JVM's target/stage2 did until S4. Sourced, with $root
# set to the repository root. Defines:
#
#   arbace_host_exe   prints the path of the host executable: ARBACE_HOST_EXE when set, else
#                     stage 0, the executable built from the Go-source seed (bin/seed --build,
#                     target/seed/<host arch>/arbace), built first when missing or when the seed
#                     changed (target/seed/seed.sha256 names the seed it was built from)
#   arbace_host_arch  amd64 or arm64, the host's architecture

arbace_host_arch() { case $(uname -m) in aarch64) echo arm64 ;; *) echo amd64 ;; esac; }

arbace_host_exe() {
  if [ -n "${ARBACE_HOST_EXE:-}" ]; then
    [ -x "$ARBACE_HOST_EXE" ] || { echo "ARBACE_HOST_EXE: no executable $ARBACE_HOST_EXE" >&2; return 1; }
    echo "$ARBACE_HOST_EXE"; return
  fi
  local a seed want have exe
  a=$(arbace_host_arch)
  exe=$root/target/seed/$a/arbace
  seed=${ARBACE_SEED:-$root/seed/arbace-seed-go.tar.xz}
  want=$(cut -d' ' -f1 "$seed.sha256" 2>/dev/null)
  have=$(cat "$root/target/seed/seed.sha256" 2>/dev/null)
  if [ ! -x "$exe" ] || [ -z "$want" ] || [ "$want" != "$have" ]; then
    mkdir -p "$root/.tmp"
    (
      # one stage 0 build at a time (concurrent tools share target/seed)
      flock 9
      have=$(cat "$root/target/seed/seed.sha256" 2>/dev/null)
      if [ ! -x "$exe" ] || [ "$want" != "$have" ]; then
        echo "== stage 0: bin/seed --build --arch $a (the host of Arbace's tools)" >&2
        "$root/bin/seed" --build --arch "$a" >&2 || exit 1
      fi
    ) 9> "$root/.tmp/seed-build.lock" || { echo "no stage 0: bin/seed --build failed" >&2; return 1; }
  fi
  echo "$exe"
}
