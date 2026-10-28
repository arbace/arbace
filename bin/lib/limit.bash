# A program's limit in CPU time, with a wall cap (doc/GATE-SURVEY.md, GT6, GTS16 (a), "As built:
# GT4, GT6"). Sourced. Defines:
#
#   limited_cpu CPU WALL CMD...  runs CMD with a CPU-time limit of CPU seconds (ulimit -t: the kernel
#                        counts all of a process's threads; at the limit the process is killed, status
#                        137) and a wall cap of WALL seconds (timeout, status 124), for a program blocked
#                        without spending CPU.
#   limited TMO CMD...   limited_cpu 5xTMO 20xTMO: TMO is the limit the check had as a wall time on a
#                        quiet host (goi's 10 s, Java's 20 s); the CPU limit is five times it, the wall
#                        cap four times that. CPU time barely moves with the host's load (REAP9's
#                        slowdowns of 1.3 to 2.3 times were waiting for a CPU), so a correct program
#                        does not time out because the host is busy; but a Go program's GC threads
#                        spend CPU in parallel: gcgort.go runs 2.5 s of wall time and 32 s of CPU on the
#                        64-CPU host, 3.2 times its 10 s, so a CPU limit of twice the wall limit was too
#                        tight (the first audit of goi-check) and five times holds the programs measured.
#
# Callers read 124, 137 and 152 (SIGXCPU) as "timeout" and, where the check has one, take the quiet
# retry (run alone, at the step's end) before they count it. Exported for the subshells and xargs that
# run actions: `export -f limited limited_cpu` where they are set up.

limited_cpu() {
  local cpu=$1 wall=$2; shift 2
  timeout -k 5 "$wall" bash -c 'ulimit -t "$1"; shift; exec "$@"' limited "$cpu" "$@"
}
limited() {
  local t=$1; shift
  limited_cpu $((t * 5)) $((t * 20)) "$@"
}
