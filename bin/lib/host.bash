# The host of Arbace's tools (standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM removed"):
# the Go executable that runs j2g, repr/go, repr/clojure and the other tools' namespaces, loaded from the
# checkout's sources (ARBACE_PATH), as the JVM's target/stage2 did until S4. Sourced, with $root
# set to the repository root. Defines:
#
#   arbace_host_exe   prints the path of the host executable: ARBACE_HOST_EXE when set, else
#                     stage 0, the executable built from the Go-source seed (bin/seed --build,
#                     target/seed/<host arch>/arbace), built first when missing or when the seed
#                     changed (target/seed/seed.sha256 names the seed it was built from); in a
#                     linked worktree (git's common dir not the checkout's own .git), its own
#                     target/seed when bin/seed --kept keeps it (the whole pins line, as
#                     bin/gate's stage0 step), else the main checkout's linked into it (bin/seed
#                     --link: hard links of its executable, seed.sha256 and pins, when --kept
#                     accepts it), else a refusal naming ARBACE_HOST_EXE and ARBACE_SEED_BUILD=1
#                     (GTS82 (a), review 001's fixes, doc/GATE-SURVEY.md); ARBACE_SEED_BUILD=1
#                     builds the worktree's own as in the main checkout
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
  seed=${ARBACE_SEED_GO:-seed/arbace-seed-go.tar.xz} # bin/seed's variable and default
  case $seed in /*) ;; *) seed=$root/$seed ;; esac
  want=$([ -f "$seed.sha256" ] && cut -d' ' -f1 "$seed.sha256")
  have=$([ -f "$root/target/seed/seed.sha256" ] && cat "$root/target/seed/seed.sha256")
  # GTS82 (a), review 001's fixes: in a linked worktree (git's common dir another checkout's .git) the
  # stage 0 is the one bin/seed --kept accepts (the gate's identity: the whole pins line), its own, else the
  # main checkout's linked into its own target/seed (bin/seed --link: hard links, checked again after
  # linking), rather than a build of its own (10 minutes, 8 GB); else a refusal
  local common main why sv log
  mkdir -p "$root/.tmp"
  if [ "${ARBACE_SEED_BUILD:-}" != 1 ] \
     && common=$(git -C "$root" rev-parse --path-format=absolute --git-common-dir 2>"$root/.tmp/host-git.err") \
     && [ "$(basename "$common")" = .git ] && main=$(dirname "$common") \
     && [ "$(cd "$main" && pwd -P)" != "$(cd "$root" && pwd -P)" ]; then
    sv=(env ARBACE_SEED_GO="$seed") log=$root/.tmp/host-seed.$BASHPID.log
    if "${sv[@]}" "$root/bin/seed" --kept "$root/target/seed" --arch "$a" > "$log" 2>&1; then
      rm -f "$log"; echo "$exe"; return
    fi
    # queue 063: --link waits for a stage 0 build in the main checkout (re-review 020, finding 2: up to
    # ARBACE_SEED_LINK_WAIT s, default 1200), its "waiting" line in the log read only after; said here as it
    # begins, as bin/gate's stage0_choose does (GTS101 (b)), so that a tool call does not sit silent
    if [ -f "$main/.tmp/seed-build.lock" ] && ! flock -n -s "$main/.tmp/seed-build.lock" true 2> "$root/.tmp/host-flock.err"; then
      echo "arbace_host_exe: waiting for $main's stage 0 build to link it (at most ${ARBACE_SEED_LINK_WAIT:-1200} s)" >&2
    fi
    if "${sv[@]}" "$root/bin/seed" --link "$main/target/seed" --arch "$a" > "$log" 2>&1; then
      rm -f "$log"; echo "$exe"; return
    fi
    why=$(sed 's/^bin\/seed: //; s/^not linked: //' "$log" | tail -1); rm -f "$log"
    echo "arbace_host_exe: $root is a linked worktree with no stage 0 of its own that bin/seed --kept keeps, and the" >&2
    echo "arbace_host_exe: main checkout's ($main/target/seed) is not linked: ${why:-bin/seed failed}; not building one here." >&2
    echo "arbace_host_exe: set ARBACE_HOST_EXE=/path/to/arbace (a stage 0 of this seed), or ARBACE_SEED_BUILD=1 to build" >&2
    echo "arbace_host_exe: this worktree's own (bin/seed --build: about 10 minutes, 8 GB)" >&2
    return 1
  fi
  if [ ! -x "$exe" ] || [ -z "$want" ] || [ "$want" != "$have" ]; then
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
