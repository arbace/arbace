# The gate's executables' directory (doc/GATE-SURVEY.md, GTS24 (a), GTS120). Sourced. Defines:
#
#   arbace_main_checkout ROOT   the main checkout's directory: the parent of the git common dir when
#                               ROOT is a worktree of a checkout whose common dir is named .git, else ROOT
#   arbace_gate_exes ROOT       the directory the gate keeps its builds in: ARBACE_GATE_EXES, else
#                               MAIN/.tmp/gate-exe (shared by the worktrees)
#   arbace_gate_cache ROOT [KEY]
#                               where the gate's builds of ROOT's gate_key are: ARBACE_GATE_CACHE when set
#                               (an override; the gate sets it for its own steps), else
#                               ARBACE_GATE_EXES/KEY20, KEY20 the first 20 characters of KEY (default
#                               `ROOT/bin/arbace-go --gate-key`), as bin/lib/gate-run.bash's gate_env
#                               computes it. Its amd64/arbace is a hit after a bin/gate run of that key
#
# Since GTS24 (a) nothing populates .tmp/arbace-go-gate: the scripts that look for the gate's build by
# hand (bin/diffcheck, bin/generated-check, bin/clojure-tests --aot, bin/java-check, bin/lib/program.bash)
# take their default from here.

arbace_main_checkout() {
  local root=$1 common
  # (git's error, if any, joins the output: then not a directory, the fallback; no file written, unlike
  # ${TMPDIR:-.}/gate-cache-git.err, which left an empty file in a checkout's root when TMPDIR was unset)
  common=$(cd "$root" && git rev-parse --path-format=absolute --git-common-dir 2>&1) \
    && [ -d "$common" ] && [ "$(basename "$common")" = .git ] && dirname "$common" || echo "$root"
}

arbace_gate_exes() {
  echo "${ARBACE_GATE_EXES:-$(arbace_main_checkout "$1")/.tmp/gate-exe}"
}

arbace_gate_cache() {
  local root=$1 key=${2:-}
  [ -z "${ARBACE_GATE_CACHE:-}" ] || { echo "$ARBACE_GATE_CACHE"; return 0; }
  [ -n "$key" ] || key=$("$root/bin/arbace-go" --gate-key) || return 1
  echo "$(arbace_gate_exes "$root")/${key:0:20}"
}
