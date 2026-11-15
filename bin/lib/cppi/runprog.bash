# cppi_run_prog CPU WALL STATUS-FILE PROGRAM ARG...: a lowered C++ program run by ci's back end (tools/cirun) with a memory cap of its
# own under a CPU-time limit (the user, 2026-11-15, round 43; gate 13e's runtime/atomics.cpp). bin/cppi exports its tools'
# GOMEMLIMIT=1500MiB, which a run used to inherit: under it a program whose heap is larger collects without end (atomics.cpp's 2.8 GB:
# 210 s of wall and 750 s of CPU, against 33 s of both without it). The run drops it and takes limit.bash's limited_cpu instead:
# CPPI_RUN_MEM_GIB (default 8) GiB of address space, three quarters of it as GOMEMLIMIT, CPU seconds of CPU time and a wall cap of
# WALL, at least four times CPU. STATUS-FILE gets the status, "timeout" for a limit's stop (124 at the wall cap, 137 with the
# program's CPU at four fifths of CPU or more). Sourced; needs $root. The caller redirects the program's standard streams.
cppi_run_prog() {
  local cpu=$1 wall=$2 sf=$3; shift 3
  [ "$wall" -ge $((cpu * 4)) ] || wall=$((cpu * 4))
  ( env -u GOMEMLIMIT ARBACE_MEM_GIB="${CPPI_RUN_MEM_GIB:-8}" bash -c '. "$0"; limited_cpu "$@"' "$root/bin/lib/limit.bash" "$cpu" "$wall" "$@"
    echo $? > "$sf"; times > "$sf.cpu" )
  local st; st=$(cat "$sf")
  # times' second line: the children's user and system time (the program's)
  if [ "$st" = 124 ]; then st=timeout
  elif [ "$st" = 137 ] && [ "$(awk -v c="$cpu" 'NR == 2 { split($1, u, "m"); split($2, s, "m"); sub(/s$/, "", u[2]); sub(/s$/, "", s[2])
                                       print (u[1] * 60 + u[2] + s[1] * 60 + s[2] >= c * 0.8) ? 1 : 0 }' "$sf.cpu")" = 1 ]; then st=timeout; fi
  echo "$st" > "$sf"; rm -f "$sf.cpu"
}
