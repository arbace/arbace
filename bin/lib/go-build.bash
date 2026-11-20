# The compile's memory policy for go build (doc/go/BUILD.md, "The build in bounded steps", 2026-11-20).
# Sourced with $root set; used by bin/build-go (the program's build) and bin/arbace-go (the image's link,
# the profile-guided build). Defines:
#
#   go_build_policy   sets go_policy_env (VAR=VALUE words for env) and go_policy_flags (go build's
#                     flags, put after "build"): go's processes at most ARBACE_GO_BUILD_PROCS (default
#                     8: GOMAXPROCS of the go command, its -p and the compilers' -c), and each compile run
#                     by bin/lib/go-toolexec, which admits it by the Go bytes compiling beside it and runs a
#                     big package with less back-end concurrency (its header). Neither changes the
#                     executable's bytes (doc/go/BUILD.md). With ARBACE_BUILD_CHUNKS=0 none of it (go as
#                     before), but bin/lib/go-toolexec for its times when ARBACE_GO_BUILD_TIMES is set.
#                     Removes the admission's state of a previous call (go_build_done)
#   go_build_done     removes the admission's state
#   go_build_plan FILE
#                     runs the go build command arbace.build.go --go-cmd wrote into FILE (its lines dir,
#                     rm, env, arg) under the policy, in its directory; removes the temporary work
#                     directory it names; the status is go build's

go_build_policy() {
  go_policy_env=() go_policy_flags=()
  go_build_done
  if [ "${ARBACE_BUILD_CHUNKS:-1}" = 0 ]; then
    [ -z "${ARBACE_GO_BUILD_TIMES:-}" ] || go_policy_flags=(-toolexec "$root/bin/lib/go-toolexec")
    return 0
  fi
  mkdir -p "${TMPDIR:-$root/.tmp/tmp}"
  go_policy_state=$(mktemp "${TMPDIR:-$root/.tmp/tmp}/go-toolexec.XXXXXX") || return 1
  go_policy_env=(GOMAXPROCS="${ARBACE_GO_BUILD_PROCS:-8}" ARBACE_GO_TOOLEXEC_STATE="$go_policy_state")
  go_policy_flags=(-toolexec "$root/bin/lib/go-toolexec")
}

go_build_done() {
  [ -z "${go_policy_state:-}" ] || rm -f "$go_policy_state" "$go_policy_state.lock"
  go_policy_state=
}

go_build_plan() {
  local plan=$1 line d= rm= envs=() args=() st=0
  while IFS= read -r line; do
    case ${line%% *} in
      dir) d=${line#dir } ;;
      rm) rm=${line#rm } ;;
      env) envs+=("${line#env }") ;;
      arg) args+=("${line#arg }") ;;
      *) echo "build-go: a line of the go build plan $plan not understood: $line" >&2; return 1 ;;
    esac
  done < "$plan"
  [ -n "$d" ] && [ ${#args[@]} -ge 2 ] || { echo "build-go: no go build command in $plan" >&2; return 1; }
  go_build_policy || return 1
  # go build's flags after "build"
  args=("${args[0]}" "${args[1]}" ${go_policy_flags[@]+"${go_policy_flags[@]}"} "${args[@]:2}")
  (cd "$d" && env "${envs[@]}" ${go_policy_env[@]+"${go_policy_env[@]}"} "${args[@]}") || st=$?
  go_build_done
  [ -z "$rm" ] || rm -rf "$rm"
  return $st
}
