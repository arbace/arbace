# The gate's runner (doc/GATE-SURVEY.md, GTS22 (a), "As built: GT4, GT6"): what decides a step's
# outcome, sourced by bin/gate. With test/gate/plan.ae it is the gate's logic that every check's key
# holds; bin/gate's header, summary and option parsing are not. Sourced with $root, $logs, $store,
# $mode, $full, $start, $status set by bin/gate. Defines:
#
#   gate_env             the store of passes, the heavy lock, the executables' directory (shared by
#                        the worktrees, GTS24 (a)), the exported GATE_* variables
#   plan_inputs DIR      DIR/files.sha256 and DIR/hosts.edn, run_plan DIR [--rerun]: the plan
#   step NAME HEAVY CMD  one step, logged, timed, measured (GT1); a heavy one under GATE_HEAVY_CMD
#   record_pass, load_plan, run_one, report, pre_step, run_chain, chains_of, names_of
#   gate_prune [--dry-run]  the store and the executables trimmed (GTS27 (a))
#   gate_audit [STEP...] the strace audit of the declared inputs (GT4)
#   audit_reducer DIR, audit_reduce FIFO, audit_compare NAME STATUS
#                        the audit's reducer and comparison; with audit_all set (bin/gate --clean, GT7)
#                        every step's own run is traced (step's audit_step) and compared: an undeclared
#                        read fails it (NAME.audit, NAME.audit-status) and its pass is not recorded; a
#                        pass of the clean run (GATE_CLEAN_INNER) is marked :clean; GATE_CPU_LOG: each
#                        limited program's CPU time in NAME.cpu.tsv (ARBACE_CPU_LOG, bin/lib/limit.bash)
#
# The heavy lock (GT6, GTS25 (a); GT5, GTS31 (b), GTS32 (a)): a step the table marks :heavy true runs
# under GATE_HEAVY_CMD, by default bin/heavy-lock (the host's big-job lock ARBACE_HEAVY_LOCK, by default
# the main checkout's .tmp/big.lock, taken in turn: a FIFO of tickets in front of the flock), the gate
# itself not; its environment has ARBACE_HEAVY_HELD=1, so that its tools take no lock again. A step
# marked :heavy :inner runs without it: its tools take the lock around their heavy process alone
# (bin/lib/heavy.bash's heavy_run: bin/j2g, repr/go's reader, repr/java's corpus reads), under
# ARBACE_HEAVY_CMD, which the gate sets to GATE_HEAVY_CMD; a light step likewise. The step's wall time
# starts when it holds the lock; the wait is its .wait, the inner waits its .waits (bin/heavy-lock's,
# HEAVY_LOCK_WAITS). A caller that holds the lock already (flock .tmp/big.lock bin/gate) sets
# GATE_HEAVY_CMD=env: then ARBACE_HEAVY_HELD=1 for every step, nothing locks again.
#
# Outputs (GT5): a step with :outputs writes into GATE_OUT (.tmp/gate/out/NAME), kept with its pass
# (STORE/NAME/KEY.out) and put back when it is skipped; a step whose :uses name them reads them under
# GATE_OUTS, their digests in its key: known at the plan when the producers are skipped, else computed
# here once they ran (the decision "after", an early cutoff).

gate_env() {
  local common main
  common=$(git rev-parse --path-format=absolute --git-common-dir 2>"$TMPDIR/gate-git.err") && [ -d "$common" ] \
    && [ "$(basename "$common")" = .git ] && main=$(dirname "$common") || main=$root
  store=${ARBACE_GATE_STORE:-$main/.tmp/gate-pass}
  export ARBACE_HEAVY_LOCK=${ARBACE_HEAVY_LOCK:-$main/.tmp/big.lock}
  GATE_HEAVY_CMD=${GATE_HEAVY_CMD:-$root/bin/heavy-lock}
  # the tools' own lock around their heavy process (GTS31 (b), bin/lib/heavy.bash): the same command;
  # GATE_HEAVY_CMD=env (the caller holds the lock): nothing locks again
  export ARBACE_HEAVY_CMD=$GATE_HEAVY_CMD
  [ "$GATE_HEAVY_CMD" = env ] && export ARBACE_HEAVY_HELD=1
  # GTS52 (a): TamaGo's bin/ first on every step's PATH (and the tools'), so that a program or script
  # that runs `go` runs the pinned toolchain, never the host's (no build may use the host's go)
  PATH=${ARBACE_GOROOT:-/root/tamago-go}/bin:$PATH; export PATH
  exes=${ARBACE_GATE_EXES:-$main/.tmp/gate-exe}
  mkdir -p "$(dirname "$ARBACE_HEAVY_LOCK")" "$exes" || return 1
  # the executables and javac's tool by the key of what they are built from, one directory each,
  # shared by the worktrees (GTS24 (a)): a worktree whose check runs finds main's build at the same key
  gkey=$("$root/bin/arbace-go" --gate-key) || return 1
  . "$root/bin/lib/javac-key.bash"
  jkey=$(arbace_javac_key "$root") || return 1
  export ARBACE_GATE_CACHE=$exes/${gkey:0:20} GATE_CACHE=$exes/${gkey:0:20} GATE_JAVAC_DIR=$exes/javac-${jkey:0:20}
  export GATE_EXE=$GATE_CACHE/amd64/arbace GATE_TESTS=$GATE_CACHE/amd64/arbace-tests
  export GATE_JAVAC=$GATE_JAVAC_DIR/arbace-javac GATE_JOBS=${GATE_TEST_JOBS:-12}
  # Java's check's cache of read forms, shared by the worktrees (GT5: keyed by the sources, the options,
  # the reader's sources and the tool's key), so that a level that runs where the read was skipped finds it
  export GATE_JAVA_FORMS=${GATE_JAVA_FORMS:-$main/.tmp/java-check/forms}
}

plan_inputs() {  # plan_inputs DIR: DIR/files.sha256 and DIR/hosts.edn
  local d=$1 jdk rel
  mkdir -p "$d"
  find . \( -path ./.git -o -path ./.tmp -o -path ./target -o -path ./.claude \) -prune -o -type f -printf '%P\n' \
    | LC_ALL=C sort | xargs -d '\n' sha256sum > "$d/files.sha256" || return 1
  rel=none
  jdk=$(command -v javac) && jdk=$(readlink -f "$jdk") && [ -f "${jdk%/bin/javac}/release" ] \
    && rel=$(sha256sum < "${jdk%/bin/javac}/release" | cut -c1-64)
  # the program hosts (GT5, GTS4 (b), (c)): the units of the build at this key (units.tsv), pending
  # when there is no build yet, none for a build made before the units were recorded
  local pu=pending ju=pending
  [ -x "$GATE_EXE" ] && { pu=none; [ -f "$GATE_CACHE/amd64/units.tsv" ] && pu=$GATE_CACHE/amd64/units.tsv; }
  [ -x "$GATE_JAVAC" ] && { ju=none; [ -f "$GATE_JAVAC_DIR/units.tsv" ] && ju=$GATE_JAVAC_DIR/units.tsv; }
  # the gate's own tests (a toy table, GATE_STEPS): units given
  pu=${GATE_PROG_UNITS:-$pu} ju=${GATE_JAVAC_PROG_UNITS:-$ju}
  { printf '{:gate "%s"\n' "$gkey"
    printf ' :javac "%s"\n' "$jkey"
    printf ' :prog-units "%s" :prog-rest "%s"\n' "$pu" "$("$root/bin/arbace-go" --gate-rest-key)"
    printf ' :javac-prog-units "%s" :javac-prog-rest "%s"\n' "$ju" "$(arbace_javac_rest_key "$root")"
    printf ' :stage0 "%s"\n' "$(cat seed/arbace-seed-go.tar.xz.sha256 seed/goroot.sha256 bin/seed bin/lib/seed-go.bash | sha256sum | cut -c1-64)"
    printf ' :frozen "%s"\n' "$(sha256sum < test/oracle/frozen-jar.sha256 | cut -c1-64)"
    printf ' :jdk "%s"\n' "$rel"
    printf ' :go "%s"}\n' "$(sha256sum < "${ARBACE_GOROOT:-/root/tamago-go}/VERSION" | cut -c1-64)"; } > "$d/hosts.edn"
}
run_plan() {  # run_plan DIR [--rerun]: the plan into DIR (plan.tsv, plan/), printed
  local d=$1; shift
  plan_inputs "$d" || { echo "gate: the plan's inputs failed" >&2; return 1; }
  arbace_tool -p "$root/test" -m gate.plan plan "$root" "$d" "$store" "$mode" "$@"
}

secs() { local s=$(( $(date +%s) - $1 )); printf '%dm%02ds' $((s / 60)) $((s % 60)); }
mmss() { printf '%dm%02ds' $(($1 / 60)) $(($1 % 60)); }
gib() { awk -v k="${1:-0}" 'BEGIN {printf "%.1f GiB", k / 1048576}'; }

# GT1: the memory sampler, every 5 s: the host's MemAvailable, the gate's processes' RSS and each
# running step's (its process tree's, from its NAME.pid), in KiB, to mem.tsv (TIME WHAT KIB)
sampler() {
  local t steps f
  while :; do
    t=$(date +%s) steps=
    for f in "$logs"/*.pid; do [ -f "$f" ] && steps="$steps $(basename "$f" .pid):$(cat "$f" 2>"$logs/sampler.err")"; done
    awk -v t="$t" -v gate="$$" -v steps="$steps" '
      FILENAME == "/proc/meminfo" { if ($1 == "MemAvailable:") print t "\tMemAvailable\t" $2; next }
      FNR == 1 { s = $0; i = index(s, ")"); while ((j = index(substr(s, i + 1), ")")) > 0) i += j
                 pid = $1; n = split(substr(s, i + 2), f, " "); ppid[pid] = f[2]; rss[pid] = f[22] * 4
                 kids[f[2]] = kids[f[2]] " " pid }
      function tree(p,   k, a, n, sum) { sum = rss[p]; n = split(kids[p], a, " "); for (k = 1; k <= n; k++) sum += tree(a[k]); return sum }
      END { print t "\tgate\t" tree(gate)
            n = split(steps, st, " "); for (k = 1; k <= n; k++) { split(st[k], q, ":"); if (q[2] in rss) print t "\t" q[1] "\t" tree(q[2]) } }
    ' /proc/meminfo /proc/[0-9]*/stat >> "$logs/mem.tsv" 2>>"$logs/sampler.err"
    sleep 5
  done
}

step() {  # step NAME HEAVY COMMAND...: one step, logged, timed, measured (GT1); HEAVY yes: under the lock (GT6), inner: its tools lock (GT5)
  local name=$1 hv=$2 t0 t0w t1 pid st pre=() tr=() ae=() ap=; shift 2; t0=$(date +%s)
  [ "$hv" = yes ] || hv=
  rm -f "${logs:?}/${name:?}.start" "${logs:?}/${name:?}.waits"
  [ -n "$hv" ] && read -r -a pre <<< "$GATE_HEAVY_CMD"
  # GT7: the step's own run under the audit (audit_step, bin/gate --clean): strace into a FIFO its
  # reducer reads at once (NAME.paths), the namespaces loaded in NAME.loaded; the programs' CPU times
  # (GTS33 (a), bin/lib/limit.bash) in NAME.cpu.tsv
  if [ -n "${audit_step:-}" ]; then
    rm -f "${logs:?}/$name.fifo" "${logs:?}/$name.paths" "${logs:?}/$name.loaded"; mkfifo "$logs/$name.fifo"
    audit_reduce "$logs/$name.fifo" > "$logs/$name.paths" 2> "$logs/$name.reduce.err" &
    ap=$!
    tr=(strace -f -qq -y -s 4096 --seccomp-bpf -e "trace=${GATE_AUDIT_TRACE:-$audit_trace}" -o "$logs/$name.fifo")
    ae=(ARBACE_LOADED_LOG="$logs/$name.loaded")
  fi
  [ -n "${GATE_CPU_LOG:-}" ] && { rm -f "${logs:?}/$name.cpu.tsv"; ae+=(ARBACE_CPU_LOG="$logs/$name.cpu.tsv"); }
  # the lock first, then the clock: the start file is written once the step holds the lock
  "${pre[@]}" bash -c 'date +%s > "$1"; shift; exec "$@"' gate-step "$logs/$name.start" \
    env ${hv:+ARBACE_HEAVY_HELD=1} HEAVY_LOCK_WAITS="$logs/$name.waits" ${ae[@]+"${ae[@]}"} ${tr[@]+"${tr[@]}"} \
    /usr/bin/time -v -o "$logs/$name.time" "$@" > "$logs/$name.log" 2>&1 &
  pid=$!
  echo "$pid" > "$logs/$name.pid"
  wait "$pid"; st=$?
  rm -f "${logs:?}/${name:?}.pid"
  if [ -n "$ap" ]; then
    # a reducer still waiting for strace to open the FIFO (the step failed before it ran) is released
    kill -0 "$ap" 2>"$logs/$name.reduce.err2" && timeout 10 bash -c ': > "$1"' release "$logs/$name.fifo" 2>>"$logs/$name.reduce.err2"
    wait "$ap"; rm -f "${logs:?}/$name.fifo"
  fi
  t1=$(date +%s); t0w=$t0; [ -f "$logs/$name.start" ] && t0w=$(cat "$logs/$name.start")
  echo $st > "$logs/$name.status"; echo $(( t1 - t0w )) > "$logs/$name.secs"; echo $(( t0w - t0 )) > "$logs/$name.wait"
  local cpu rss tree
  cpu=$(awk -F': ' '/User time|System time/ {s += $2} END {printf "%.1f", s}' "$logs/$name.time" 2>"$logs/$name.time.err")
  rss=$(awk -F': ' '/Maximum resident set size/ {print $2}' "$logs/$name.time" 2>"$logs/$name.time.err")
  tree=$(awk -F'\t' -v n="$name" '$2 == n && $3 > m {m = $3} END {print m + 0}' "$logs/mem.tsv" 2>"$logs/$name.time.err")
  local iw=0
  [ -f "$logs/$name.waits" ] && iw=$(awk '{s += $1} END {print s + 0}' "$logs/$name.waits")
  printf '{:step "%s" :wall-s %s :cpu-s %s :maxrss-kib %s :tree-peak-kib %s :wait-s %s :inner-wait-s %s}\n' \
    "$name" "$(cat "$logs/$name.secs")" "${cpu:-0}" "${rss:-0}" "${tree:-0}" "$(cat "$logs/$name.wait")" "$iw" > "$logs/$name.mem"
  return $st
}
mem_of() {  # mem_of NAME FIELD: a number of NAME.mem
  sed -n "s/.*:$2 \([0-9.]*\).*/\1/p" "$logs/$1.mem" 2>"$logs/mem-of.err"
}
record_pass() {  # record_pass NAME: its pass in the store at its key (GTS7 (a)), with its outputs (GT5)
  local name=$1 k=${key[$1]} d=$store/$1 tmp
  mkdir -p "$d" || return 1
  tmp=$d/.$k.$$
  cp "$logs/$name.log" "$d/$k.log" && cp "$logs/plan/$name.inputs" "$d/$k.inputs" || return 1
  if [ "${outputs[$name]}" = yes ]; then
    rm -rf "${d:?}/.$k.out.$$"
    cp -a "$logs/out/$name" "$d/.$k.out.$$" && rm -rf "${d:?}/$k.out" && mv -T "$d/.$k.out.$$" "$d/$k.out" || return 1
  fi
  printf '{:step "%s" :key "%s" :commit "%s" :dirty %s :worktree "%s" :date "%s"\n :secs %s :cpu-s %s :maxrss-kib %s :tree-peak-kib %s\n :log-sha256 "%s"}\n' \
    "$name" "$k" "$commit" "$dirty" "$root" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$(cat "$logs/$name.secs")" \
    "$(mem_of "$name" cpu-s)" "$(mem_of "$name" maxrss-kib)" "$(mem_of "$name" tree-peak-kib)" \
    "$(sha256sum < "$logs/$name.log" | cut -c1-64)" > "$tmp" || return 1
  # GT7: a pass of the clean run (bin/gate --clean) says so
  [ -n "${GATE_CLEAN_INNER:-}" ] && sed -i '$ s/}$/ :clean true}/' "$tmp"
  mv "$tmp" "$d/$k.edn"
}

# the plan's steps, from plan.tsv: NAME DECISION KEY PHASE CHAIN NEEDS START-AFTER HEAVY MEM BUDGET
# SUMMARY-N SUMMARY-RE WHY AUDIT-CMD OUTPUTS USES KIND CMD
order=()
declare -A dec key phase chain needs after heavy memgib budget sum_n sum_re why acmd outputs uses kind cmd
load_plan() {  # load_plan: plan.tsv into the arrays (again after a second plan: the order anew)
  local n d k p c ne a hv mg bu sn sre w ac ou us ki cm
  order=()
  while IFS=$'\t' read -r n d k p c ne a hv mg bu sn sre w ac ou us ki cm; do
    order+=("$n"); dec[$n]=$d key[$n]=$k phase[$n]=$p chain[$n]=$c needs[$n]=${ne//,/ } after[$n]=$a
    heavy[$n]=$hv memgib[$n]=$mg budget[$n]=$bu sum_n[$n]=$sn sum_re[$n]=$sre why[$n]=$w acmd[$n]=$ac
    outputs[$n]=$ou uses[$n]=${us//,/ } kind[$n]=$ki cmd[$n]=$cm
  done < "$logs/plan.tsv"
}
ok_status() { [ -f "$logs/$1.status" ] && case $(cat "$logs/$1.status") in 0|skip) return 0 ;; esac; return 1; }
# out_sha PATH: an output's digest, as gate.plan's out-digest: a file's SHA-256; a directory's, the
# SHA-256 of sha256sum's lines of its files in order; missing
out_sha() {
  if [ -f "$1" ]; then sha256sum < "$1" | cut -c1-64
  elif [ -d "$1" ]; then (cd "$1" && find . -type f | sed 's|^\./||' | LC_ALL=C sort | xargs -r -d '\n' sha256sum) | sha256sum | cut -c1-64
  else echo missing; fi
}
# pass_desc FILE: a pass record's "passed at KEY, COMMIT, DATE, WORKTREE", as gate.plan's
pass_desc() {
  sed -n -e 's/.*:key "\([0-9a-f]\{12\}\)[0-9a-f]*".*:commit "\([0-9a-f]\{0,8\}\)[^"]*".*:worktree "\([^"]*\)" :date "\([^"]*\)".*/passed at \1, \2, \4, \3/p' "$1"
}
# restore_out NAME: a skipped step's outputs put back from its pass
restore_out() {
  [ "${outputs[$1]}" = yes ] || return 0
  rm -rf "${logs:?}/out/$1"; mkdir -p "$logs/out"
  [ -d "$store/$1/${key[$1]}.out" ] && cp -a "$store/$1/${key[$1]}.out" "$logs/out/$1"
}
# build_cached NAME: a build step's directory exists at its key (GTS57 (a)); the scripts' own checks
build_cached() {
  case $1 in
    go-build) "$root/bin/arbace-go" --gate-build --check-cached > "$logs/$1.cached" 2>&1 ;;
    javac-tool) "$root/bin/javac-vendor" tool "$GATE_JAVAC_DIR" --check > "$logs/$1.cached" 2>&1 ;;
    *) return 1 ;;
  esac
}
run_one() {  # run_one NAME: a step of the plan, as decided (once: a build of the build phase is done)
  local n=$1 x st u p
  [ -f "$logs/$n.done" ] && return 0
  case ${dec[$n]} in
    skip) restore_out "$n" || { echo "notrun its outputs" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
          echo skip > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
    none) echo none > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
  esac
  for x in ${needs[$n]}; do
    [ "$x" = - ] && continue
    ok_status "$x" || { echo "notrun $x" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
  done
  if [ "${dec[$n]}" = after ]; then
    # GT5: the key once the producers ran, their outputs' digests appended to the plan's material
    for u in ${uses[$n]}; do echo "uses $u $(out_sha "$logs/out/$u")" >> "$logs/plan/$n.inputs"; done
    key[$n]=$(sha256sum < "$logs/plan/$n.inputs" | cut -c1-64)
    if [ -f "$store/$n/${key[$n]}.edn" ] && [ -z "${rerun:-}" ]; then
      why[$n]="cut off: its producers' outputs as at its pass; $(pass_desc "$store/$n/${key[$n]}.edn")"
      dec[$n]=skip; echo "${why[$n]}" > "$logs/$n.why"
      restore_out "$n" || { echo "notrun its outputs" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
      echo skip > "$logs/$n.status"; touch "$logs/$n.done"; return 0
    fi
    dec[$n]=run
  fi
  local attempt aud= hv=${heavy[$n]}
  [ -n "${audit_all:-}" ] && aud=1
  # GTS57 (a): a build kept at its key (a few seconds) takes no lock and queues for none
  if [ "${kind[$n]}" = build ] && [ "$hv" = yes ] && build_cached "$n"; then hv=cached; fi
  for attempt in 1 2 3; do
    if [ "${outputs[$n]}" = yes ]; then rm -rf "${logs:?}/out/$n"; mkdir -p "$logs/out/$n"; fi
    audit_step=$aud GATE_OUT=$logs/out/$n GATE_OUTS=$logs/out step "$n" "$hv" bash -c "${cmd[$n]}"; st=$?
    # GT4: strace -f aborts (ptrace(PTRACE_LISTEN): I/O error) when a traced process dies in a group
    # stop; an aborted trace is no audit and its run no run: again, twice at most
    [ -n "$aud" ] && grep -q '^strace: ptrace' "$logs/$n.log" || break
    echo "$attempt" > "$logs/$n.audit-aborts"
    cp "$logs/$n.log" "$logs/$n.log.aborted$attempt"
  done
  [ -n "$aud" ] && audit_compare "$n" "$st"
  if [ $st = 0 ] && [ "${dec[$n]}" = run ]; then
    if [ -n "$aud" ] && [ "$(cat "$logs/$n.audit-status")" != ok ]; then
      echo "gate: the pass of $n not recorded: its audit is $(cat "$logs/$n.audit-status")" >&2
    else
      record_pass "$n" || echo "gate: warning: the pass of $n was not recorded in $store" >&2
    fi
  fi
  touch "$logs/$n.done"
  return $st
}
report() {  # report NAME: a step's outcome, times, memory and summary lines
  local n=$1 s w st b wt
  if [ ! -f "$logs/$n.status" ]; then
    echo "   FAILED  $n (not run: an earlier step failed)"; status=1; return
  fi
  st=$(cat "$logs/$n.status")
  [ -f "$logs/$n.why" ] && why[$n]=$(cat "$logs/$n.why")
  case $st in
    skip) echo "   skipped  $n (${why[$n]})"; return ;;
    none) echo "   -  $n (not needed: no step that runs needs it)"; return ;;
    notrun*) echo "   FAILED  $n (not run: ${st#notrun } did not pass)"; status=1; return ;;
  esac
  s=$(cat "$logs/$n.secs"); w=$(mmss "$s")
  local c; c=$(mem_of "$n" cpu-s); c=${c%.*}
  w="$w, cpu $(mmss "${c:-0}"), peak $(gib "$(mem_of "$n" maxrss-kib)") (tree $(gib "$(mem_of "$n" tree-peak-kib)"))"
  wt=$(cat "$logs/$n.wait" 2>"$logs/count.err"); wt=${wt:-0}
  [ "$wt" -ge 5 ] && w="$w, waited $(mmss "$wt") for the lock"
  wt=$(mem_of "$n" inner-wait-s); wt=${wt:-0}
  [ "$wt" -ge 5 ] && w="$w, its tools waited $(mmss "$wt") for the lock"
  # GT6, GTS17 (a): a budget is a warning (three times the longest of its last passes), never a kill
  b=${budget[$n]:--}
  if [ "$b" != - ] && [ "$s" -gt "$b" ]; then w="$w; OVER ITS BUDGET of $(mmss "$b")"; fi
  local au=
  if [ -f "$logs/$n.audit-status" ]; then
    case $(cat "$logs/$n.audit-status") in
      ok) au=", audit ok" ;;
      step-failed) au=", not audited (the step failed)" ;;
      *) au=", AUDIT $(cat "$logs/$n.audit-status") ($logs/$n.audit)"; status=1 ;;
    esac
  fi
  if [ "$st" = 0 ]; then
    echo "   ok  $n ($w$au)"
  else
    echo "   FAILED  $n ($w$au; log $logs/$n.log)"; status=1
  fi
  [ -f "$logs/$n.audit-status" ] && [ "$(cat "$logs/$n.audit-status")" != ok ] && [ "$st" = 0 ] \
    && grep -A8 '^UNDECLARED' "$logs/$n.audit" | sed 's/^/     /' 
  [ "${sum_re[$n]:--}" = - ] || grep -E "${sum_re[$n]}" "$logs/$n.log" | tail -"${sum_n[$n]}" | sed 's/^/     /'
}
pre_step() {  # pre_step NAME REGEX LINES CMD...: before the plan, one after the other
  local name=$1 re=$2 nl=$3; shift 3
  step "$name" "" "$@"; sum_re[$name]=$re sum_n[$name]=$nl; report "$name"
  if [ "$(cat "$logs/$name.status")" != 0 ]; then
    tail -20 "$logs/$name.log"; echo "== gate FAILED ($(secs "$start"))"; exit 1
  fi
}
run_chain() {  # run_chain PHASE CHAIN: its steps one after the other (after its :start-after)
  local n first=1
  for n in "${order[@]}"; do
    [ "${phase[$n]}" = "$1" ] && [ "${chain[$n]}" = "$2" ] || continue
    if [ -n "$first" ] && [ "${after[$n]}" != - ]; then
      while [ ! -f "$logs/${after[$n]}.done" ]; do sleep 5; done
    fi
    first=
    run_one "$n"
  done
}
chains_of() {  # chains_of PHASE: its chains, in the order of their first step
  local n seen=" "
  for n in "${order[@]}"; do
    [ "${phase[$n]}" = "$1" ] || continue
    case $seen in *" ${chain[$n]} "*) ;; *) seen="$seen${chain[$n]} "; echo "${chain[$n]}" ;; esac
  done
}
names_of() { local n; for n in "${order[@]}"; do [ "${phase[$n]}" = "$1" ] && echo "$n"; done; }

# GTS27 (a): per step the last ARBACE_GATE_KEEP (5) passes and every pass younger than
# ARBACE_GATE_KEEP_DAYS (30) kept, the rest removed (a pass is KEY.edn, KEY.log, KEY.inputs); the
# executables' directories (the gate's, javac's) older than a day but the newest 3 of each removed
gate_prune() {  # gate_prune [--dry-run]
  local dry= keep=${ARBACE_GATE_KEEP:-5} days=${ARBACE_GATE_KEEP_DAYS:-30} d n f k i pat npass=0 nkeep=0 freed=0 sz b now
  now=$(date +%s)
  [ "${1:-}" = --dry-run ] && dry=1
  [ -d "$store" ] || { echo "gate --prune: no store $store"; return 0; }
  for d in "$store"/*/; do
    [ -d "$d" ] || continue
    n=$(basename "$d"); i=0
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      i=$((i + 1)); npass=$((npass + 1)); k=${f%.edn}
      if [ $i -le "$keep" ] || [ $(( now - $(stat -c %Y "$f") )) -lt $(( days * 86400 )) ]; then nkeep=$((nkeep + 1)); continue; fi
      sz=$(du -sb "$k".edn "$k".log "$k".inputs "$k".out 2>"$store/.prune.err" | awk '{s += $1} END {print s + 0}'); freed=$((freed + sz))
      echo "gate --prune: ${dry:+would remove }$n/$(basename "$k" | cut -c1-12) ($(stat -c %y "$f" | cut -c1-10), $sz bytes)"
      [ -n "$dry" ] || rm -rf "${k:?}.edn" "${k:?}.log" "${k:?}.inputs" "${k:?}.out"
    done <<< "$(ls -t "$d" | sed -n "s|^\([0-9a-f]*\.edn\)$|$d\1|p")"
    [ -n "$dry" ] || find "$d" -maxdepth 1 -name '.*' -mmin +1440 -exec rm -rf {} + 2>"$store/.prune.err"
  done
  echo "gate --prune: $npass passes, $nkeep kept, $((npass - nkeep)) ${dry:+would be }removed ($freed bytes) in $store"
  for pat in gate javac; do
    i=0
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      b=$(basename "$f")
      case $pat in javac) case $b in javac-*) ;; *) continue ;; esac ;; gate) case $b in javac-*) continue ;; esac ;; esac
      i=$((i + 1)); [ $i -gt 3 ] || continue
      [ $(( now - $(stat -c %Y "$f") )) -ge 86400 ] || continue
      echo "gate --prune: ${dry:+would remove }executables $f"
      [ -n "$dry" ] || rm -rf "${f:?}"
    done <<< "$(ls -dt "$exes"/* 2>"$store/.prune.err")"
  done
}

# the audit's reducer (GT5, GTS30 (a)): tools/gateaudit, the Go port of test/gate/audit.awk (23 times
# faster, the same lines), built by TamaGo's go into DIR when its sources changed; GATE_AUDIT_AWK=1:
# busybox awk. audit_reducer DIR sets red; audit_reduce FIFO prints the OP<TAB>PATH lines
red=()
audit_trace=open,openat,openat2,execve,execveat,readlink,readlinkat,chdir,clone,clone3,fork,vfork
audit_reducer() {
  local d=$1 tg=${ARBACE_GOROOT:-/root/tamago-go} rk
  red=(); mkdir -p "$d"
  [ -z "${GATE_AUDIT_AWK:-}" ] || return 0
  rk=$(cat "$root"/tools/gateaudit/*.go "$root/tools/gateaudit/go.mod" "$tg/VERSION" | sha256sum | cut -c1-64)
  if [ "$(cat "$d/gateaudit.key" 2>"$d/gateaudit.err")" != "$rk" ] || [ ! -x "$d/gateaudit" ]; then
    (cd "$root/tools/gateaudit" && env -u GOOS -u GOARCH GOROOT="$tg" GOFLAGS= GOTOOLCHAIN=local CGO_ENABLED=0 \
       "$tg/bin/go" build -trimpath -o "$d/gateaudit.new" .) > "$d/gateaudit.err" 2>&1 \
      && mv -f "$d/gateaudit.new" "$d/gateaudit" && echo "$rk" > "$d/gateaudit.key"
  fi
  if [ -x "$d/gateaudit" ] && [ "$(cat "$d/gateaudit.key" 2>"$d/gateaudit.err")" = "$rk" ]; then red=("$d/gateaudit" -root "$root")
  else echo "gate --audit: tools/gateaudit did not build ($d/gateaudit.err): busybox awk reduces" >&2; fi
}
audit_reduce() {  # audit_reduce FIFO
  if [ ${#red[@]} -gt 0 ]; then "${red[@]}" ${GATE_AUDIT_SIGNALS:+-signals "$1.signals"} "$1"
  else awk -v root="$root" -v dbg="${GATE_AUDIT_SIGNALS:+$1.signals}" -f "$root/test/gate/audit.awk" "$1"; fi
}
# audit_compare NAME STATUS (GT7: a step run under the audit, audit_all): its reads against its declared
# inputs (gate.audit, the plan's files.sha256 in $logs); NAME.audit the report, NAME.audit-status ok,
# "N undeclared", step-failed (what a failed step read is not an audit) or "error"
audit_compare() {
  local n=$1 st=$2 a
  if [ "$st" != 0 ]; then echo step-failed > "$logs/$n.audit-status"; return 0; fi
  [ -f "$logs/$n.loaded" ] && LC_ALL=C sort -u -o "$logs/$n.loaded" "$logs/$n.loaded"
  arbace_tool -p "$root/test" -m gate.audit "$root" "$logs" "$n" "$logs/$n.paths" "$logs/$n.loaded" > "$logs/$n.audit" 2>&1; a=$?
  case $a in
    0) echo ok ;;
    1) sed -n "s/^audit $n: \(.*\)$/\1/p" "$logs/$n.audit" | tail -1 | grep . || echo failed ;;
    *) echo "error (exit $a)" ;;
  esac > "$logs/$n.audit-status"
}

# GT4: the audit. A check's command run under strace (the opens, execve, readlink, clone, chdir;
# --seccomp-bpf stops only on those, so a Go program runs near full speed; the stat calls are left
# out, GATE_AUDIT_TRACE=%file puts them back: a Go build makes millions, and a stat reads no content),
# the paths reduced by test/gate/audit.awk to OP PATH lines and compared by gate.audit
# (test/gate/audit.ae) with the step's declared inputs
gate_audit() {  # gate_audit [STEP...]: the checks named, else every check
  local d=${GATE_AUDIT_DIR:-$root/.tmp/gate-audit} n x names=("$@") st=0 t0 fifo hv=() ap
  command -v strace > "$d.which" || { echo "gate --audit: no strace" >&2; return 2; }
  rm -rf "${d:?}/plan"; mkdir -p "$d"
  audit_reducer "$d"
  mode=full; logs=$d/plan
  run_plan "$d/plan" > "$d/plan.txt" 2>&1 || { cat "$d/plan.txt"; return 1; }
  load_plan
  if [ ${#names[@]} -eq 0 ]; then
    for n in "${order[@]}"; do case ${dec[$n]} in skip|run) names+=("$n") ;; esac; done
  fi
  for n in "${names[@]}"; do
    [ -n "${cmd[$n]:-}" ] || { echo "gate --audit: no step $n" >&2; st=1; continue; }
    case ${dec[$n]} in skip|run) ;; always) [ "$n" = stage0 ] || { echo "gate --audit: $n is not a check (${dec[$n]})"; continue; } ;; *) echo "gate --audit: $n is not a check (${dec[$n]})"; continue ;; esac
    for x in ${needs[$n]}; do
      [ "$x" = - ] && continue
      case $x in
        go-build) [ -x "$GATE_EXE" ] || { echo "gate --audit: $n needs the executables ($GATE_CACHE): run bin/gate first" >&2; st=1; continue 2; } ;;
        javac-tool) [ -x "$GATE_JAVAC" ] || { echo "gate --audit: $n needs javac's tool ($GATE_JAVAC_DIR): run bin/gate --full first" >&2; st=1; continue 2; } ;;
      esac
    done
    # strace -f can abort ("ptrace(PTRACE_LISTEN): I/O error") when a traced process is killed in a
    # group stop, as the programs of $GOROOT/test that signal themselves are: an aborted trace is
    # not an audit, the step runs again (twice at most)
    acmd_run=${cmd[$n]}; [ "${acmd[$n]}" = - ] || acmd_run=${acmd[$n]}
    # GT5: the outputs its :uses name, from their producers' passes
    for x in ${uses[$n]}; do
      [ "$x" = - ] && continue
      case ${dec[${x%%/*}]} in
        skip) restore_out "${x%%/*}" ;;
        *) [ -e "$logs/out/${x%%/*}" ] || { echo "gate --audit: $n uses ${x%%/*}'s outputs, which have no pass at its key: bin/gate --full first" >&2; st=1; continue 2; } ;;
      esac
    done
    if [ "${outputs[$n]}" = yes ]; then rm -rf "${logs:?}/out/$n"; mkdir -p "$logs/out/$n"; fi
    rm -f "${d:?}/$n.loaded"
    for attempt in 1 2 3; do
      fifo=$d/$n.fifo; rm -f "${fifo:?}" "${d:?}/$n.paths"; mkfifo "$fifo"
      audit_reduce "$fifo" > "$d/$n.paths" 2> "$d/$n.awk.err" &
      ap=$!
      hv=(); [ "${heavy[$n]}" = yes ] && read -r -a hv <<< "$GATE_HEAVY_CMD"
      t0=$(date +%s); rm -f "${d:?}/$n.start"
      # the clock starts once the step holds the lock (the start file, as step() does)
      "${hv[@]}" strace -f $([ -n "${GATE_AUDIT_SIGNALS:-}" ] && echo -q || echo -qq) -y -s 4096 --seccomp-bpf -e "trace=${GATE_AUDIT_TRACE:-$audit_trace}" -o "$fifo" \
        env $([ "${heavy[$n]}" = yes ] && echo ARBACE_HEAVY_HELD=1) ARBACE_LOADED_LOG="$d/$n.loaded" GATE_OUT="$logs/out/$n" GATE_OUTS="$logs/out" \
        bash -c 'date +%s > "$1"; exec bash -c "$2"' gate-audit "$d/$n.start" "$acmd_run" > "$d/$n.log" 2>&1
      echo $? > "$d/$n.status"; [ -f "$d/$n.start" ] && t0=$(cat "$d/$n.start"); echo $(( $(date +%s) - t0 )) > "$d/$n.secs"
      wait "$ap"; rm -f "${fifo:?}"
      grep -q '^strace: ptrace' "$d/$n.log" || break
      echo "== audit $n: strace aborted (attempt $attempt), $(mmss "$(cat "$d/$n.secs")") lost"
    done
    echo "== audit $n: exit $(cat "$d/$n.status") in $(mmss "$(cat "$d/$n.secs")"), $(wc -l < "$d/$n.paths") distinct (op, path) pairs"
    [ -f "$d/$n.loaded" ] && LC_ALL=C sort -u -o "$d/$n.loaded" "$d/$n.loaded"
    arbace_tool -p "$root/test" -m gate.audit "$root" "$d/plan" "$n" "$d/$n.paths" "$d/$n.loaded" | tee "$d/$n.report" || st=1
    if [ "$(cat "$d/$n.status")" != 0 ]; then
      echo "audit $n: the step itself failed (exit $(cat "$d/$n.status"), $d/$n.log): what it read before failing is not an audit"; st=1
    fi
  done
  return $st
}
