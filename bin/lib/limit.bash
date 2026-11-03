# A program's limit in CPU time, with a wall cap (doc/GATE-SURVEY.md, GT6, GTS16 (a), "As built:
# GT4, GT6"). Sourced. Defines:
#
#   limited_cpu CPU WALL CMD...  runs CMD with a CPU-time limit of CPU seconds (ulimit -t: the kernel
#                        counts all of a process's threads; at the limit the process is killed, status
#                        137) and a wall cap of WALL seconds (timeout, status 124), for a program blocked
#                        without spending CPU.
#   limited TMO CMD...   limited_cpu 5xTMO 20xTMO: TMO is the limit the check had as a wall time on a
#                        quiet host (goi's 10 s, Java's 20 s); the CPU limit is five times it, the wall
#                        cap four times that. CPU time barely moves with the host's load for a program
#                        that computes (REAP9's slowdowns of 1.3 to 2.3 times were waiting for a CPU), so a
#                        correct program does not time out because the host is busy; it does move for a
#                        program that makes many traced syscalls under the audit (strace into a FIFO: an
#                        open-heavy probe went from 0.31 to 1.15 s of system time on a loaded host, review 075
#                        finding 10), and the wall cap, four times the CPU limit, can fire first on a host
#                        slowed past that factor (a program at 95 % of its CPU limit, 4.4 times slower); but a Go program's GC threads
#                        spend CPU in parallel: gcgort.go runs 2.5 s of wall time and 32 s of CPU on the
#                        64-CPU host, 3.2 times its 10 s, so a CPU limit of twice the wall limit was too
#                        tight (the first audit of goi-check) and five times holds the programs measured.
#
#   ARBACE_MEM_GIB       each limited program's memory cap in GiB (default 16; 0: none; 2026-11-03, after 25 OOM kills
#                        from an interpreter's leak, 5 of them other agents' processes): GOMEMLIMIT at three quarters of it
#                        when the caller set none (Go's collector works hard before it), and a hard address-space limit
#                        (ulimit -v) at the cap, so a leaking program fails alone (Go: "out of memory", status 2) instead of
#                        waking the host's OOM killer on its neighbours. A test that needs more names its own, as a check's
#                        heavy cases do
#   ARBACE_CPU_LOG       a file: each limited program's STATUS, WALL, USER, SYS seconds and command
#                        appended to it, tab-separated (GTS33 (a): bin/gate --clean measures the quiet
#                        CPU times its limits are to be recorded from)
#
# Callers read 124, 137 and 152 as "timeout" and, where the check has one, take the quiet retry (run alone,
# at the step's end) before they count it. Of the three, 152 (SIGXCPU) never comes from ulimit -t, which sets
# the soft and the hard limit equal, so the kernel sends SIGKILL (test/gate/limit-test.bash cpu-kill); it is
# read for a program's own exit 152 and for a cover run's CPU limit (limited_cpu's soft limit). 137 is ambiguous: the CPU limit, the wall cap's -k after a SIGTERM
# survivor, a self-kill, or the OOM killer (a memory kill is read as a timeout and run again alone). Where the
# distinction matters, ARBACE_CPU_LOG's user and system seconds tell a CPU kill (about the limit) from the rest
# (review 075, finding 6). Exported for the subshells and xargs that
# run actions: `export -f limited limited_cpu` where they are set up.

limited_cpu() {
  local cpu=$1 wall=$2 lim='ulimit -t "$1"; shift; exec "$@"'; shift 2
  local mem=${ARBACE_MEM_GIB:-16} memlim=
  # the memory cap (ARBACE_MEM_GIB above): before the CPU limit in the same inline shell, so xargs' exported copy keeps it
  [ "$mem" = 0 ] || memlim="ulimit -v $((mem * 1048576)); export GOMEMLIMIT=\${GOMEMLIMIT:-$((mem * 768))MiB}; "
  # under ARBACE_COVER_DIR (GT8.3, bin/lib/cover.bash) a soft limit at the limit and the hard one 5 s above, so the
  # kernel sends SIGXCPU first and the cover build's hook writes the counters before the program ends (status 152,
  # a timeout as ever; review 182, finding 4). Inline, not a function: limited_cpu is exported alone to xargs' shells
  [ -z "${ARBACE_COVER_DIR:-}" ] || lim='ulimit -S -t "$1"; ulimit -H -t "$(($1 + 5))"; shift; exec "$@"'
  lim=$memlim$lim
  if [ -n "${ARBACE_CPU_LOG:-}" ]; then
    # GTS33 (a)'s measurement (bin/gate --clean): each program's status, wall, user and system seconds
    # and command appended to ARBACE_CPU_LOG, one line each. time runs timeout, not the reverse (review 075,
    # finding 2): the program stays timeout's direct child, so its -k SIGKILL reaches a program that ignores
    # SIGTERM (137, as without the log; time inside timeout died at the SIGTERM, timeout returned 124 without
    # -k and left the program orphaned, a hang under strace -f) and a wall-cap row has its times
    local tf=$ARBACE_CPU_LOG.$BASHPID st
    /usr/bin/time -f '%e %U %S' -o "$tf" timeout -k 5 "$wall" bash -c "$lim" limited "$cpu" "$@"; st=$?
    # time exits with the signal's number when the program was killed: the status a shell gives
    local sig; sig=$(sed -n 's/^Command terminated by signal \([0-9]*\)$/\1/p' "$tf" 2>"$tf.err")
    [ -n "$sig" ] && [ "$st" = "$sig" ] && st=$((128 + sig))
    printf '%s\t%s\t%s\n' "$st" "$(tail -1 "$tf" 2>"$tf.err" | tr ' ' '\t')" "$*" >> "$ARBACE_CPU_LOG"
    rm -f "$tf" "$tf.err"
    return $st
  fi
  timeout -k 5 "$wall" bash -c "$lim" limited "$cpu" "$@"
}
limited() {
  local t=$1; shift
  limited_cpu $((t * 5)) $((t * 20)) "$@"
}

# attempt_limits TMO ONCE: the limits (limited's TMO) a check's action is tried at, one a line: TMO, then twice
# TMO (JCS47 (a): a timeout is run again once before it counts), or TMO alone when ONCE is set, the quiet retry
# that already runs at twice the first limit (review 075, finding 4: it doubled again, four attempts at 20, 40,
# 40 and 80 s in all, an hour of the essential gate for a blocked hang)
attempt_limits() {
  echo "$1"
  [ -n "${2:-}" ] || echo "$(($1 * 2))"
}

# timeout_kind STATUS: what a timeout status says it was: 124 the wall cap, 137 and 152 the CPU limit or a kill
# (an OOM kill, a -k after the wall cap, a self-kill read alike: review 075, finding 6), else empty
timeout_kind() {
  case $1 in 124) echo wall ;; 137|152) echo cpu-or-kill ;; *) echo ;; esac
}

# note_attempt WHO ID ATTEMPT LIMIT STATUS RESULT: review 075, finding 3: a retry keeps no record of the attempt
# that timed out. Appends WHO, ID, ATTEMPT, LIMIT, STATUS, the kind and RESULT, tab-separated, to
# ARBACE_TIMEOUT_LOG (nothing when it is unset) for an attempt that timed out and for the attempt after one, and
# prints a line to stderr when an attempt after a timeout did not time out (a pass that needed the retry).
# STATUS "-": the attempt's status is not known to the caller
note_attempt() {
  local who=$1 id=$2 n=$3 lim=$4 st=$5 res=$6 kind
  kind=$(timeout_kind "$st")
  # (JAVA_CHECK_ONCE: the quiet retry's one attempt follows a timeout too, review 080, finding 3)
  case $res in timeout*) ;; *) [ "$n" -gt 1 ] || [ -n "${JAVA_CHECK_ONCE:-}" ] || return 0
    echo "$who: $id: the first attempt timed out; attempt $n (limit $lim) ended $res" >&2 ;; esac
  [ -z "${ARBACE_TIMEOUT_LOG:-}" ] || printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$who" "$id" "$n" "$lim" "$st" "${kind:--}" "${res%%$'\t'*}" >> "$ARBACE_TIMEOUT_LOG"
}

# keep_first_timeouts RESULTS OUT: the rows of RESULTS (ID, result, reason...) that are timeouts, kept in OUT
# before a retry replaces them, and their ids printed to stderr with the reason (review 075, finding 3: goi's
# retry printed only the count, and the row replaced was gone)
keep_first_timeouts() {
  awk -F'\t' "$timeout_row_awk" "$1" > "$2"
  awk -F'\t' '{ print "   first attempt: " $1 ": " $3 }' "$2" >&2
}

# timeout_row_ids RESULTS: the ids of the rows (ID, result, reason...) a goi check takes to the quiet retry
# (review 075, finding 5: the rows of a timeout that were not worded "timeout" never were): "error timeout (N s)",
# "error gc reference: the original timed out" and "...: the compile timed out" (gc's reference at three times the
# limit), and check_err's "unsupported ... goi's check over N s)", a timeout read as gc's later phases, which
# only the retry's second timeout may decide
timeout_row_awk='($2 == "error" && ($3 ~ /^timeout/ || $3 ~ /^gc reference: the (original|compile) timed out$/)) || ($2 == "unsupported" && $3 ~ /goi.s check over [0-9]+ s\)$/)'
timeout_row_ids() { awk -F'\t' "$timeout_row_awk { print \$1 }" "$1"; }

# gc_status_reason STATUS LIMIT: the reason of a gc-level run that did not end 0 (nethttp's gc level, limited
# LIMIT; review 075, finding 7: a timeout read "gc status 124", a fail like any other)
gc_status_reason() {
  case $1 in 124|137|152) echo "gc timed out (limit $2 s)" ;; *) echo "gc status $1" ;; esac
}

# jdk_run_limit TMO: the wall limit (seconds) of the JDK's reference run of a jtreg test whose own timeout is
# TMO (review 075, finding 8: a flat 60 s recorded the tests jtreg gives 120 or 400 s as JDK failures, status
# 124, which took them out of every level): the test's own, at least 60
jdk_run_limit() {
  local t=${1:-0}
  case $t in ''|*[!0-9]*) t=0 ;; esac
  [ "$t" -ge 60 ] && echo "$t" || echo 60
}

# program_tmo PROGRAM TMO [FILE]: PROGRAM's time limit in seconds: its line in test/goi/timeouts.txt (FILE), else
# TMO (the action's -t or the default). goi's check reads it for every kind of program (review 075, finding 9:
# only check_one did; a directory test or an errorcheck program could not be given more time)
program_tmo() {
  awk -v p="$1" -v t="$2" '$1 == p { t = $2 } END { print t }' "${3:-$root/test/goi/timeouts.txt}"
}

# reset_timeout_log DEFAULT: ARBACE_TIMEOUT_LOG for this run: the caller's when it is set (not touched), else DEFAULT,
# exported and emptied (review 080, finding 3: the default file grew with every run, rows of runs long past)
reset_timeout_log() {
  if [ -z "${ARBACE_TIMEOUT_LOG:-}" ]; then
    export ARBACE_TIMEOUT_LOG=$1
    : > "$ARBACE_TIMEOUT_LOG"
  fi
}
