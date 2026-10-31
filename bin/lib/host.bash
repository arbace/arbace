# The host of Arbace's tools (standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM removed"):
# the Go executable that runs j2g, repr/go, repr/clojure and the other tools' namespaces, loaded from the
# checkout's sources (ARBACE_PATH), as the JVM's target/stage2 did until S4. Sourced, with $root
# set to the repository root. Defines:
#
#   arbace_host_exe   prints the path of the host executable: ARBACE_HOST_EXE when set, else
#                     stage 0, the executable built from the Go-source seed (bin/seed --build,
#                     target/seed/<host arch>/arbace), built first when missing or when the seed
#                     changed (target/seed/seed.sha256 names the seed it was built from); in a
#                     linked worktree (git's common dir not the checkout's own .git) with no
#                     current stage 0 of its own, the main checkout's stage 0 when its seed's
#                     and Go's pins (target/seed/pins) are the worktree's, else a refusal naming
#                     ARBACE_HOST_EXE and ARBACE_SEED_BUILD=1 (GTS82 (a), doc/GATE-SURVEY.md);
#                     ARBACE_SEED_BUILD=1 builds the worktree's own as in the main checkout
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
  want=$([ -f "$seed.sha256" ] && cut -d' ' -f1 "$seed.sha256")
  have=$([ -f "$root/target/seed/seed.sha256" ] && cat "$root/target/seed/seed.sha256")
  if [ ! -x "$exe" ] || [ -z "$want" ] || [ "$want" != "$have" ]; then
    # GTS82 (a): a linked worktree takes the main checkout's stage 0 rather than build its own (a
    # 10-minute, 8 GB build), when it is built from the same seed and Go
    local common main mpins gopin why
    mkdir -p "$root/.tmp"
    if [ "${ARBACE_SEED_BUILD:-}" != 1 ] \
       && common=$(git -C "$root" rev-parse --path-format=absolute --git-common-dir 2>"$root/.tmp/host-git.err") \
       && [ "$(basename "$common")" = .git ] && main=$(dirname "$common") \
       && [ "$(cd "$main" && pwd -P)" != "$(cd "$root" && pwd -P)" ]; then
      gopin=$(sha256sum < "$root/seed/goroot.sha256" | cut -c1-64)
      mpins=$([ -f "$main/target/seed/pins" ] && cut -d' ' -f1,2 "$main/target/seed/pins")
      if [ ! -x "$main/target/seed/$a/arbace" ]; then why="has no $a/arbace"
      elif [ -z "$want" ] || [ "$mpins" != "$want $gopin" ] \
           || [ "$(cat "$main/target/seed/seed.sha256" 2>"$root/.tmp/host-git.err")" != "$want" ]; then
        why="is built from other pins (${mpins:-none recorded}; this worktree's seed ${want:-none}, Go's pin $gopin)"
      else
        echo "$main/target/seed/$a/arbace"; return
      fi
      echo "arbace_host_exe: $root is a linked worktree with no stage 0 of its own built from its seed, and the main" >&2
      echo "arbace_host_exe: checkout's stage 0 ($main/target/seed) $why: not building one here." >&2
      echo "arbace_host_exe: set ARBACE_HOST_EXE=/path/to/arbace (a stage 0 of this seed), or ARBACE_SEED_BUILD=1 to build" >&2
      echo "arbace_host_exe: this worktree's own (bin/seed --build: about 10 minutes, 8 GB)" >&2
      return 1
    fi
    mkdir -p "$root/.tmp"
    (
      # one stage 0 build at a time (concurrent tools share target/seed)
      flock 9
      have=$([ -f "$root/target/seed/seed.sha256" ] && cat "$root/target/seed/seed.sha256")
      if [ ! -x "$exe" ] || [ "$want" != "$have" ]; then
        echo "== stage 0: bin/seed --build --arch $a (the host of Arbace's tools)" >&2
        SEED_BUILD_LOCKED=1 "$root/bin/seed" --build --arch "$a" >&2 || exit 1
      fi
    ) 9> "$root/.tmp/seed-build.lock" || { echo "no stage 0: bin/seed --build failed" >&2; return 1; }
  fi
  echo "$exe"
}
