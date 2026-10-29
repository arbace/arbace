# The heavy processes' lock inside the tools (doc/GATE-SURVEY.md, GTS31 (b), "As built: GT5"). Sourced
# with $root set. Defines:
#
#   heavy_run CMD [ARG...]
#                 runs CMD under ARBACE_HEAVY_CMD (bin/gate exports it: GATE_HEAVY_CMD, by default
#                 bin/heavy-lock, the host's big-job lock taken in turn) when ARBACE_HEAVY_CMD is set and
#                 ARBACE_HEAVY_HELD is not, else as it is; CMD's environment has ARBACE_HEAVY_HELD=1 either
#                 way, so that a tool it starts takes no lock again (a second flock of the lock by a child
#                 of its holder would wait for ever)
#   heavy_exec CMD [ARG...]
#                 heavy_run, exec'd
#
# The protocol (bin/lib/gate-run.bash's step): a step the table marks :heavy true runs under the lock
# as a whole, ARBACE_HEAVY_HELD=1 in its environment: its tools take none. A step marked :heavy :inner
# does not: its tools take the lock around their heavy process alone (bin/j2g, repr/go's native reader
# and converter, repr/java's corpus reads: read-jdk), and its light work runs beside the other steps.
# A light step runs without the lock, and a heavy tool it starts still takes it. Outside the gate
# ARBACE_HEAVY_CMD is unset and the tools take no lock (an agent wraps its heavy command in
# `flock big.lock` itself, as before).

heavy_run() {
  if [ -n "${ARBACE_HEAVY_CMD:-}" ] && [ -z "${ARBACE_HEAVY_HELD:-}" ]; then
    local pre=()
    read -r -a pre <<< "$ARBACE_HEAVY_CMD"
    "${pre[@]}" env ARBACE_HEAVY_HELD=1 "$@"
  else
    ARBACE_HEAVY_HELD=${ARBACE_HEAVY_HELD:-1} "$@"
  fi
}
heavy_exec() {
  if [ -n "${ARBACE_HEAVY_CMD:-}" ] && [ -z "${ARBACE_HEAVY_HELD:-}" ]; then
    local pre=()
    read -r -a pre <<< "$ARBACE_HEAVY_CMD"
    exec "${pre[@]}" env ARBACE_HEAVY_HELD=1 "$@"
  fi
  ARBACE_HEAVY_HELD=${ARBACE_HEAVY_HELD:-1} exec "$@"
}
