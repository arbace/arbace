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
#
# The heavy lock (GT6, GTS25 (a)): a step the table marks :heavy runs under GATE_HEAVY_CMD, by
# default `flock $ARBACE_HEAVY_LOCK` (ARBACE_HEAVY_LOCK, by default the main checkout's
# .tmp/big.lock: the host's big-job lock that agents take), the gate itself not. The step's wall
# time starts when it holds the lock; the wait is its .wait. A caller that holds the lock already
# (flock .tmp/big.lock bin/gate) sets GATE_HEAVY_CMD=env, else the gate waits for itself. The step's
# environment has ARBACE_HEAVY_HELD=1.

gate_env() {
  local common main
  common=$(git rev-parse --path-format=absolute --git-common-dir 2>"$TMPDIR/gate-git.err") && [ -d "$common" ] \
    && [ "$(basename "$common")" = .git ] && main=$(dirname "$common") || main=$root
  store=${ARBACE_GATE_STORE:-$main/.tmp/gate-pass}
  export ARBACE_HEAVY_LOCK=${ARBACE_HEAVY_LOCK:-$main/.tmp/big.lock}
  GATE_HEAVY_CMD=${GATE_HEAVY_CMD:-flock $ARBACE_HEAVY_LOCK}
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
}

plan_inputs() {  # plan_inputs DIR: DIR/files.sha256 and DIR/hosts.edn
  local d=$1 jdk rel
  mkdir -p "$d"
  find . \( -path ./.git -o -path ./.tmp -o -path ./target -o -path ./.claude \) -prune -o -type f -printf '%P\n' \
    | LC_ALL=C sort | xargs -d '\n' sha256sum > "$d/files.sha256" || return 1
  rel=none
  jdk=$(command -v javac) && jdk=$(readlink -f "$jdk") && [ -f "${jdk%/bin/javac}/release" ] \
    && rel=$(sha256sum < "${jdk%/bin/javac}/release" | cut -c1-64)
  { printf '{:gate "%s"\n' "$gkey"
    printf ' :javac "%s"\n' "$jkey"
    printf ' :stage0 "%s"\n' "$(cat seed/arbace-seed-go.tar.xz.sha256 seed/goroot.sha256 | sha256sum | cut -c1-64)"
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

step() {  # step NAME HEAVY COMMAND...: one step, logged, timed, measured (GT1); a heavy one under the lock (GT6)
  local name=$1 hv=$2 t0 t0w t1 pid st pre=(); shift 2; t0=$(date +%s)
  [ "$hv" = yes ] || hv=
  rm -f "${logs:?}/${name:?}.start"
  [ -n "$hv" ] && read -r -a pre <<< "$GATE_HEAVY_CMD"
  # the lock first, then the clock: the start file is written once the step holds the lock
  "${pre[@]}" bash -c 'date +%s > "$1"; shift; exec "$@"' gate-step "$logs/$name.start" \
    env ${hv:+ARBACE_HEAVY_HELD=1} /usr/bin/time -v -o "$logs/$name.time" "$@" > "$logs/$name.log" 2>&1 &
  pid=$!
  echo "$pid" > "$logs/$name.pid"
  wait "$pid"; st=$?
  rm -f "${logs:?}/${name:?}.pid"
  t1=$(date +%s); t0w=$t0; [ -f "$logs/$name.start" ] && t0w=$(cat "$logs/$name.start")
  echo $st > "$logs/$name.status"; echo $(( t1 - t0w )) > "$logs/$name.secs"; echo $(( t0w - t0 )) > "$logs/$name.wait"
  local cpu rss tree
  cpu=$(awk -F': ' '/User time|System time/ {s += $2} END {printf "%.1f", s}' "$logs/$name.time" 2>"$logs/$name.time.err")
  rss=$(awk -F': ' '/Maximum resident set size/ {print $2}' "$logs/$name.time" 2>"$logs/$name.time.err")
  tree=$(awk -F'\t' -v n="$name" '$2 == n && $3 > m {m = $3} END {print m + 0}' "$logs/mem.tsv" 2>"$logs/$name.time.err")
  printf '{:step "%s" :wall-s %s :cpu-s %s :maxrss-kib %s :tree-peak-kib %s :wait-s %s}\n' \
    "$name" "$(cat "$logs/$name.secs")" "${cpu:-0}" "${rss:-0}" "${tree:-0}" "$(cat "$logs/$name.wait")" > "$logs/$name.mem"
  return $st
}
mem_of() {  # mem_of NAME FIELD: a number of NAME.mem
  sed -n "s/.*:$2 \([0-9.]*\).*/\1/p" "$logs/$1.mem" 2>"$logs/mem-of.err"
}
record_pass() {  # record_pass NAME: its pass in the store at its key (GTS7 (a))
  local name=$1 k=${key[$1]} d=$store/$1 tmp
  mkdir -p "$d" || return 1
  tmp=$d/.$k.$$
  cp "$logs/$name.log" "$d/$k.log" && cp "$logs/plan/$name.inputs" "$d/$k.inputs" || return 1
  printf '{:step "%s" :key "%s" :commit "%s" :dirty %s :worktree "%s" :date "%s"\n :secs %s :cpu-s %s :maxrss-kib %s :tree-peak-kib %s\n :log-sha256 "%s"}\n' \
    "$name" "$k" "$commit" "$dirty" "$root" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$(cat "$logs/$name.secs")" \
    "$(mem_of "$name" cpu-s)" "$(mem_of "$name" maxrss-kib)" "$(mem_of "$name" tree-peak-kib)" \
    "$(sha256sum < "$logs/$name.log" | cut -c1-64)" > "$tmp" && mv "$tmp" "$d/$k.edn"
}

# the plan's steps, from plan.tsv: NAME DECISION KEY PHASE CHAIN NEEDS START-AFTER HEAVY MEM BUDGET
# SUMMARY-N SUMMARY-RE WHY AUDIT-CMD CMD
order=()
declare -A dec key phase chain needs after heavy memgib budget sum_n sum_re why acmd cmd
load_plan() {
  local n d k p c ne a hv mg bu sn sre w ac cm
  while IFS=$'\t' read -r n d k p c ne a hv mg bu sn sre w ac cm; do
    order+=("$n"); dec[$n]=$d key[$n]=$k phase[$n]=$p chain[$n]=$c needs[$n]=${ne//,/ } after[$n]=$a
    heavy[$n]=$hv memgib[$n]=$mg budget[$n]=$bu sum_n[$n]=$sn sum_re[$n]=$sre why[$n]=$w acmd[$n]=$ac cmd[$n]=$cm
  done < "$logs/plan.tsv"
}
ok_status() { [ -f "$logs/$1.status" ] && case $(cat "$logs/$1.status") in 0|skip) return 0 ;; esac; return 1; }
run_one() {  # run_one NAME: a step of the plan, as decided
  local n=$1 x st
  case ${dec[$n]} in
    skip|none) echo "${dec[$n]}" > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
  esac
  for x in ${needs[$n]}; do
    [ "$x" = - ] && continue
    ok_status "$x" || { echo "notrun $x" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
  done
  step "$n" "${heavy[$n]}" bash -c "${cmd[$n]}"; st=$?
  if [ $st = 0 ] && [ "${dec[$n]}" = run ]; then
    record_pass "$n" || echo "gate: warning: the pass of $n was not recorded in $store" >&2
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
  # GT6, GTS17 (a): a budget is a warning (three times the longest of its last passes), never a kill
  b=${budget[$n]:--}
  if [ "$b" != - ] && [ "$s" -gt "$b" ]; then w="$w; OVER ITS BUDGET of $(mmss "$b")"; fi
  if [ "$st" = 0 ]; then
    echo "   ok  $n ($w)"
  else
    echo "   FAILED  $n ($w; log $logs/$n.log)"; status=1
  fi
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
      sz=$(cat "$k".edn "$k".log "$k".inputs 2>/dev/null | wc -c); freed=$((freed + sz))
      echo "gate --prune: ${dry:+would remove }$n/$(basename "$k" | cut -c1-12) ($(stat -c %y "$f" | cut -c1-10), $sz bytes)"
      [ -n "$dry" ] || rm -f "${k:?}.edn" "${k:?}.log" "${k:?}.inputs"
    done < <(ls -t "$d"*.edn 2>/dev/null)
    [ -n "$dry" ] || find "$d" -maxdepth 1 -name '.*' -type f -mmin +1440 -delete 2>/dev/null
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
    done < <(ls -dt "$exes"/* 2>/dev/null)
  done
}

# GT4: the audit. A check's command run under strace (the opens, execve, readlink, clone, chdir;
# --seccomp-bpf stops only on those, so a Go program runs near full speed; the stat calls are left
# out, GATE_AUDIT_TRACE=%file puts them back: a Go build makes millions, and a stat reads no content),
# the paths reduced by test/gate/audit.awk to OP PATH lines and compared by gate.audit
# (test/gate/audit.ae) with the step's declared inputs
gate_audit() {  # gate_audit [STEP...]: the checks named, else every check
  local d=${GATE_AUDIT_DIR:-$root/.tmp/gate-audit} n x names=("$@") st=0 t0 fifo hv=() ap
  command -v strace > /dev/null || { echo "gate --audit: no strace" >&2; return 2; }
  rm -rf "${d:?}/plan"; mkdir -p "$d"
  mode=full; logs=$d/plan
  run_plan "$d/plan" > "$d/plan.txt" 2>&1 || { cat "$d/plan.txt"; return 1; }
  load_plan
  if [ ${#names[@]} -eq 0 ]; then
    for n in "${order[@]}"; do case ${dec[$n]} in skip|run) names+=("$n") ;; esac; done
  fi
  for n in "${names[@]}"; do
    [ -n "${cmd[$n]:-}" ] || { echo "gate --audit: no step $n" >&2; st=1; continue; }
    case ${dec[$n]} in skip|run) ;; *) echo "gate --audit: $n is not a check (${dec[$n]})"; continue ;; esac
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
    for attempt in 1 2 3; do
      fifo=$d/$n.fifo; rm -f "${fifo:?}" "${d:?}/$n.paths"; mkfifo "$fifo"
      awk -v root="$root" -v dbg="${GATE_AUDIT_SIGNALS:+$d/$n.signals}" -f "$root/test/gate/audit.awk" "$fifo" > "$d/$n.paths" 2> "$d/$n.awk.err" &
      ap=$!
      hv=(); [ "${heavy[$n]}" = yes ] && read -r -a hv <<< "$GATE_HEAVY_CMD"
      t0=$(date +%s); rm -f "${d:?}/$n.start"
      # the clock starts once the step holds the lock (the start file, as step() does)
      "${hv[@]}" strace -f $([ -n "${GATE_AUDIT_SIGNALS:-}" ] && echo -q || echo -qq) -y -s 4096 --seccomp-bpf -e "trace=${GATE_AUDIT_TRACE:-open,openat,openat2,execve,execveat,readlink,readlinkat,chdir,clone,clone3,fork,vfork}" -o "$fifo" \
        env ARBACE_HEAVY_HELD=1 bash -c 'date +%s > "$1"; exec bash -c "$2"' gate-audit "$d/$n.start" "$acmd_run" > "$d/$n.log" 2>&1
      echo $? > "$d/$n.status"; [ -f "$d/$n.start" ] && t0=$(cat "$d/$n.start"); echo $(( $(date +%s) - t0 )) > "$d/$n.secs"
      wait "$ap"; rm -f "${fifo:?}"
      grep -q '^strace: ptrace' "$d/$n.log" || break
      echo "== audit $n: strace aborted (attempt $attempt), $(mmss "$(cat "$d/$n.secs")") lost"
    done
    echo "== audit $n: exit $(cat "$d/$n.status") in $(mmss "$(cat "$d/$n.secs")"), $(wc -l < "$d/$n.paths") distinct (op, path) pairs"
    arbace_tool -p "$root/test" -m gate.audit "$root" "$d/plan" "$n" "$d/$n.paths" | tee "$d/$n.report" || st=1
    if [ "$(cat "$d/$n.status")" != 0 ]; then
      echo "audit $n: the step itself failed (exit $(cat "$d/$n.status"), $d/$n.log): what it read before failing is not an audit"; st=1
    fi
  done
  return $st
}
