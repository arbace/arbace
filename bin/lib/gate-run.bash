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
#   inputs_unchanged NAME  the plan's file, pin and logic lines hashed again before a pass is recorded
#   gate_prune [--dry-run] [--cheap] [--worktrees]  the store and the executables trimmed (GTS27 (a)); since GD4 every
#                        cache of test/gate/caches.edn by its rule (GDS17 (a)); --cheap its cheap rules alone (a gate's start,
#                        a step that finds the disk below the floor); --worktrees the merged and idle worktrees listed
#   disk_admit NAME      GD4 (GDS12 (a)): the disk floor before a step (deferred below it), enospc NAME after it
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
# The memory lanes (GTS227 (a), "As built: GTS227 (a)"): with GATE_LANES=1 (bin/gate --full [--clean] --concurrent)
# every step of the plan runs under bin/gate-lane in place of GATE_HEAVY_CMD, admitted by its declared :mem-gib
# (run_one's step_lane) and its index in the plan (ordix), ARBACE_HEAVY_HELD=1 inside (the run holds the big lock);
# the admission's state and events in LOGS/lanes/. With GATE_LANES=free (bin/gate without --concurrent: the essential
# memory lane, doc/GATE-SURVEY.md, "As built: the essential memory lane") every step of the plan is admitted by bin/gate-lane's
# free mode (its :mem-gib against MemAvailable) before GATE_HEAVY_CMD, which a :heavy true step still takes; report flags a
# peak over the declaration.

# Outputs (GT5): a step with :outputs writes into GATE_OUT (.tmp/gate/out/NAME), kept with its pass
# (STORE/NAME/KEY.out) and put back when it is skipped; a step whose :uses name them reads them under
# GATE_OUTS, their digests in its key: known at the plan when the producers are skipped, else computed
# here once they ran (the decision "after", an early cutoff).

gate_env() {
  local main
  . "$root/bin/lib/gate-cache.bash" # the main checkout and the executables' directory, shared with the tools run by hand (GTS120)
  main=$(arbace_main_checkout "$root")
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
  exes=$(arbace_gate_exes "$root")
  mkdir -p "$(dirname "$ARBACE_HEAVY_LOCK")" "$exes" || return 1
  # the executables and javac's tool by the key of what they are built from, one directory each,
  # shared by the worktrees (GTS24 (a)): a worktree whose check runs finds main's build at the same key
  gkey=$("$root/bin/arbace-go" --gate-key) || return 1
  . "$root/bin/lib/javac-key.bash"
  jkey=$(arbace_javac_key "$root") || return 1
  # GTS71 (c), GTS87 (a): javac's tool key handed to the steps, for bin/lib/javac-key.bash's arbace_javac_fresh
  # (repr_java_host, bin/javac-vendor check --from): the tool's .key compared with it, none of its sources hashed
  export GATE_JAVAC_KEY=$jkey GATE_JAVAC_KEY_ROOT=$(cd "$root" && pwd -P)
  # the gate computes the directory whatever ARBACE_GATE_CACHE the caller has (an override is for the tools run by hand)
  ARBACE_GATE_CACHE=$(unset ARBACE_GATE_CACHE; arbace_gate_cache "$root" "$gkey") || return 1
  GATE_CACHE=$ARBACE_GATE_CACHE GATE_JAVAC_DIR=$exes/javac-${jkey:0:20}
  export ARBACE_GATE_CACHE GATE_CACHE GATE_JAVAC_DIR
  export GATE_EXE=$GATE_CACHE/amd64/arbace GATE_TESTS=$GATE_CACHE/amd64/arbace-tests
  export GATE_JAVAC=$GATE_JAVAC_DIR/arbace-javac GATE_JOBS=${GATE_TEST_JOBS:-12}
  # Java's check's cache of read forms, shared by the worktrees (GT5: keyed by the sources, the options,
  # the reader's sources and the tool's key), so that a level that runs where the read was skipped finds it
  export GATE_JAVA_FORMS=${GATE_JAVA_FORMS:-$main/.tmp/java-check/forms}
  # GD4 (GDS12 (a), doc/GATE-DIET-SURVEY.md, "As built: GD4"): the disk is the gate's: every tool's own guard (cppi's, rsi's:
  # bin/gate-policy disk-ok) yields under ARBACE_DISK_POLICY=gate, the runner's floor before each step deciding instead
  export ARBACE_DISK_POLICY=gate
  # GD4 (GDS17 (a)): Go's build cache is held to its size by the caches' table (gate_cache_cap); bin/lib/gocache.bash's trim in the
  # builds follows the same cap (n-gocache-trim) and, with Go's own five days (7200 minutes) here, removes no entry used since
  export ARBACE_GOCACHE_KEEP_MIN=${ARBACE_GOCACHE_KEEP_MIN:-7200}
}

gate_gcc_id() {  # the :gcc host's identity: gcc's --version line, its driver's, cc1's and the C checks' headers' SHA-256; none without gcc
  local g c
  g=$(command -v gcc) || { echo none; return; }
  g=$(readlink -f "$g"); c=$(readlink -f "$(gcc -print-prog-name=cc1 2>"$TMPDIR/gate-gcc.err")" 2>"$TMPDIR/gate-gcc.err")
  { gcc --version 2>"$TMPDIR/gate-gcc.err" | head -1
    sha256sum < "$g" | cut -c1-64
    if [ -f "$c" ]; then sha256sum < "$c" | cut -c1-64; else echo "no cc1"; fi
    # the headers C's checks read through it (test/c/host/headers.sha256's paths: gcc's include directory, musl's /usr/include), their bytes now
    awk '{print $2}' "$root/test/c/host/headers.sha256" 2>"$TMPDIR/gate-gcc.err" | while read -r h; do
      if [ -f "$h" ]; then sha256sum "$h"; else echo "missing $h"; fi; done; } | sha256sum | cut -c1-64
}
plan_inputs() {  # plan_inputs DIR: DIR/files.sha256, DIR/hosts.edn and DIR/ancestors.txt
  local d=$1 jdk rel
  mkdir -p "$d"
  # review 181 (the user's (a)): this commit's ancestors, the commits whose runs count toward the rotation's record
  git rev-list HEAD > "$d/ancestors.txt" 2>"$d/ancestors.err" || : > "$d/ancestors.txt"
  find . \( -path ./.git -o -path ./.tmp -o -path ./target -o -path ./.claude \) -prune -o -type f -printf '%P\n' \
    | LC_ALL=C sort | xargs -d '\n' sha256sum > "$d/files.sha256" || return 1
  # queue 023, report 022 finding 9: a symbolic link is keyed by its target's text (the SHA-256 of
  # readlink's output: find -type f skipped it, so no key held it); a name sha256sum escapes (a backslash or
  # a newline in it: its line starts with \ and gate.plan's fixed columns would misread it) refuses the plan
  local l
  find . \( -path ./.git -o -path ./.tmp -o -path ./target -o -path ./.claude \) -prune -o -type l -printf '%P\n' \
    | LC_ALL=C sort | while IFS= read -r l; do
        printf '%s  %s\n' "$(readlink -- "$l" | tr -d '\n' | sha256sum | cut -c1-64)" "$l"
      done >> "$d/files.sha256" || return 1
  if grep -q '^\\' "$d/files.sha256"; then
    echo "gate: file names the plan cannot key (a backslash or a newline in them): $(grep '^\\' "$d/files.sha256" | head -3 | cut -c67- | tr '\n' ' ')" >&2
    return 1
  fi
  # :jdk (queue 023, report 022 finding 3; the user 2026-11-02): the release files of the JDK the steps run,
  # java's (the frozen jar's JVM, bin/lib/frozen.bash) and javac's (the reference javac): the explicit
  # Java 26, ARBACE_FROZEN_JAVA_HOME (bin/lib/java26.bash), never the system's default; none when it is not there
  local b r h= jh
  jh=${ARBACE_FROZEN_JAVA_HOME:-${JDK:-/root/jdk26u}/build/linux-x86_64-server-release/images/jdk}
  for b in java javac; do
    r=none
    jdk=$(readlink -f "$jh/bin/$b" 2>"$TMPDIR/jdk.err") && [ -f "${jdk%/bin/$b}/release" ] \
      && r=$(sha256sum < "${jdk%/bin/$b}/release" | cut -c1-64)
    h="$h$b $r
"
  done
  rel=none
  [ "$h" = "java none
javac none
" ] || rel=$(printf '%s' "$h" | sha256sum | cut -c1-64)
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
    printf ' :go "%s"\n' "$(sha256sum < "${ARBACE_GOROOT:-/root/tamago-go}/VERSION" | cut -c1-64)"
    # :gcc (the user, 2026-11-02; doc/C23-SURVEY.md, "As built: C's full checks in the gate"): the host's gcc, C's reference,
    # by its --version line, the digests of its driver and of cc1, resolved through their links, and of the headers C's checks read; none without gcc
    printf ' :gcc "%s"}\n' "$(gate_gcc_id)"; } > "$d/hosts.edn"
}
run_plan() {  # run_plan DIR [--rerun]: the plan into DIR (plan.tsv, plan/), printed; plan_rot (GT8.4): --night N or --rotation subset
  local d=$1; shift
  plan_inputs "$d" || { echo "gate: the plan's inputs failed" >&2; return 1; }
  arbace_tool -p "$root/test" -m gate.plan plan "$root" "$d" "$store" "$mode" "$@" ${plan_rot[@]+"${plan_rot[@]}"}
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
    # (through cat: busybox awk stops at a file it cannot open, and a process may end between the glob and its read,
    # which lost the whole sample)
    { cat /proc/meminfo; cat /proc/[0-9]*/stat; } 2>>"$logs/sampler.err" | awk -v t="$t" -v gate="$$" -v steps="$steps" '
      $1 ~ /:$/ { if ($1 == "MemAvailable:") print t "\tMemAvailable\t" $2; next }
      { s = $0; i = index(s, ")"); while ((j = index(substr(s, i + 1), ")")) > 0) i += j
                 pid = $1; n = split(substr(s, i + 2), f, " "); ppid[pid] = f[2]; rss[pid] = f[22] * 4
                 kids[f[2]] = kids[f[2]] " " pid }
      function tree(p,   k, a, n, sum) { sum = rss[p]; n = split(kids[p], a, " "); for (k = 1; k <= n; k++) sum += tree(a[k]); return sum }
      END { print t "\tgate\t" tree(gate)
            n = split(steps, st, " "); for (k = 1; k <= n; k++) { split(st[k], q, ":"); if (q[2] in rss) print t "\t" q[1] "\t" tree(q[2]) } }
    ' >> "$logs/mem.tsv" 2>>"$logs/sampler.err"
    # GD4 (GDS12 (a)): the free disk too (KiB, the checkout's file system), DiskAvail beside MemAvailable
    printf '%s\tDiskAvail\t%s\n' "$t" "$(disk_free_kib)" >> "$logs/mem.tsv" 2>>"$logs/sampler.err"
    sleep 5
  done
}

step() {  # step NAME HEAVY COMMAND...: one step, logged, timed, measured (GT1); HEAVY yes: under the lock (GT6), inner: its tools lock (GT5)
  local name=$1 hv=$2 t0 t0w t1 pid st pre=() tr=() ae=() ap=; shift 2; t0=$(date +%s)
  [ "$hv" = yes ] || hv=
  rm -f "${logs:?}/${name:?}.start" "${logs:?}/${name:?}.waits"
  [ -n "$hv" ] && read -r -a pre <<< "$GATE_HEAVY_CMD"
  # GTS227 (a), the memory lanes (GATE_LANES=1: bin/gate --full --concurrent, --full --clean --concurrent): every step of
  # the plan (run_one's step_lane, its declared :mem-gib) admitted by bin/gate-lane, by its declared memory and its place
  # in the plan, in place of the heavy lock; the run holds the host's big lock as a whole, so its tools take none
  # (ARBACE_HEAVY_HELD=1: the admission is the step's, :inner ones too)
  if [ "${GATE_LANES:-}" = free ] && [ -n "${step_lane:-}" ]; then
    # the essential memory lane (GATE_LANES=free: bin/gate without --concurrent, GATE_MEM_LANE=0 not): admitted against
    # the host's free memory first (bin/gate-lane's free mode), then the heavy lock as before (a :heavy true step's
    # GATE_HEAVY_CMD, its FIFO; an :inner step's tools lock their heavy process), the lane's wait in NAME.lane-wait
    rm -f "${logs:?}/${name:?}.lane-wait"
    pre=(env GATE_LANE_MODE=free GATE_LANE_DIR="$logs/lanes" GATE_LANE_STEP="$name" GATE_LANE_GIB="$step_lane"
         GATE_LANE_ORDER="${ordix[$name]:-0}" GATE_LANE_WAITS="$logs/$name.lane-wait" "$root/bin/gate-lane" ${pre[@]+"${pre[@]}"})
  elif [ -n "${GATE_LANES:-}" ] && [ -n "${step_lane:-}" ]; then
    pre=(env GATE_LANE_DIR="$logs/lanes" GATE_LANE_STEP="$name" GATE_LANE_GIB="$step_lane" GATE_LANE_ORDER="${ordix[$name]:-0}"
         "$root/bin/gate-lane")
    hv=yes
  fi
  # GT7: the step's own run under the audit (audit_step, bin/gate --clean): strace into a FIFO its
  # reducer reads at once (NAME.paths), the namespaces loaded in NAME.loaded; the programs' CPU times
  # (GTS33 (a), bin/lib/limit.bash) in NAME.cpu.tsv
  if [ -n "${audit_step:-}" ]; then
    rm -f "${logs:?}/$name.fifo" "${logs:?}/$name.paths" "${logs:?}/$name.loaded"; mkfifo "$logs/$name.fifo"
    audit_reduce "$logs/$name.fifo" > "$logs/$name.paths" 2> "$logs/$name.reduce.err" &
    ap=$!
    tr=(strace -f -qq -y -s 4096 --seccomp-bpf -e "trace=$(audit_trace_set)" -o "$logs/$name.fifo")
    # GTS171: the step says so itself (bin/java-check --save marks its results from it, TracerPid as the cross-check);
    # bin/gate-tests accepts a test not run for being traced only here (GTS114 (a), audit 041)
    ae=(ARBACE_LOADED_LOG="$logs/$name.loaded" GATE_AUDITED=1)
  fi
  [ -n "${GATE_CPU_LOG:-}" ] && { rm -f "${logs:?}/$name.cpu.tsv"; ae+=(ARBACE_CPU_LOG="$logs/$name.cpu.tsv"); }
  # the lock first, then the clock: the start file is written once the step holds the lock
  "${pre[@]}" bash -c 'date +%s > "$1"; shift; exec "$@"' gate-step "$logs/$name.start" \
    env -u GATE_AUDITED ${GATE_STEP_MEM_GIB:+GATE_STEP_MEM_GIB=$GATE_STEP_MEM_GIB} ${hv:+ARBACE_HEAVY_HELD=1} HEAVY_LOCK_WAITS="$logs/$name.waits" ${ae[@]+"${ae[@]}"} ${tr[@]+"${tr[@]}"} \
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
  local iw=0 fso
  [ -f "$logs/$name.waits" ] && iw=$(awk '{s += $1} END {print s + 0}' "$logs/$name.waits")
  # GD4 (GDS16 rule 3): time's "File system outputs" (512-byte blocks), kept as :fs-out-kib, the material of a :disk-gib
  fso=$(awk -F': ' '/File system outputs/ {print int($2 / 2)}' "$logs/$name.time" 2>"$logs/$name.time.err")
  printf '{:step "%s" :wall-s %s :cpu-s %s :maxrss-kib %s :tree-peak-kib %s :wait-s %s :inner-wait-s %s :fs-out-kib %s}\n' \
    "$name" "$(cat "$logs/$name.secs")" "${cpu:-0}" "${rss:-0}" "${tree:-0}" "$(cat "$logs/$name.wait")" "$iw" "${fso:-0}" > "$logs/$name.mem"
  return $st
}
mem_of() {  # mem_of NAME FIELD: a number of NAME.mem
  sed -n "s/.*:$2 \([0-9.]*\).*/\1/p" "$logs/$1.mem" 2>"$logs/mem-of.err"
}
# pass_reads FILE [NAME]: a pass record that reads (GTS80 (a)): not empty, record_pass's shape, its step
# NAME, closed; an empty or cut one (a full disk's, GT7's rerun 4) is no pass. The planner reads it with
# the Clojure reader and skips one that does not read. Review 001, finding 5: a key with no value (a
# keyword followed by another or by the closing brace: an odd-length map, which the reader rejects) is
# no pass either (record_pass's values are never keywords). KEY (queue 023, report 022 finding 8): the record's
# :key is KEY, the name it is kept under (a record copied or written by hand under another key is not its pass).
# LOG (GTS95 (b)): the record's :log-sha256 is the digest of its log, when there is one: LOG, by default FILE's
# KEY.log beside it (a record and a log that do not belong together, e.g. a log replaced or cut, are no pass)
pass_reads() {  # pass_reads FILE [NAME [KEY [LOG]]]
  [ -s "$1" ] || return 1
  head -c 9 "$1" | grep -q '^{:step "' || return 1
  [ -z "${2:-}" ] || head -1 "$1" | grep -qF "{:step \"$2\" :key \"${3:-}" || return 1
  [ -z "${3:-}" ] || head -1 "$1" | grep -qF "{:step \"$2\" :key \"$3\"" || return 1
  grep -q ':log-sha256 "[0-9a-f]\{64\}"' "$1" && [ "$(tail -c 2 "$1")" = "}" ] || return 1
  ! tr '\n' ' ' < "$1" | grep -qE '(^|[ {]):[a-z0-9-]+[[:space:]]*(}|[[:space:]]:[a-z])' || return 1
  local log=${4:-}
  [ -n "$log" ] || case $1 in *.edn) log=${1%.edn}.log ;; esac
  [ -n "$log" ] && [ -e "$log" ] || return 0
  [ "$(sha256sum < "$log" | cut -c1-64)" = "$(sed -n 's/.*:log-sha256 "\([0-9a-f]\{64\}\)".*/\1/p' "$1" | head -1)" ]
}
# inputs_unchanged NAME (queue 023, report 022 finding 2): the plan's file, pin and logic lines of NAME
# (plan/NAME.inputs, hashed when the plan was made) hashed again: a file edited while the step ran (in this
# worktree, by another agent or step) means the run tested other content than its key's, NAME.recheck
# names the files. The hosts' identities and the :uses digests are not hashed again
inputs_unchanged() {
  awk '($1 == "file" || $1 == "pin" || $1 == "logic") && $2 != "missing" { p = $0; sub(/^[a-z]+ [0-9a-f]+ /, "", p); print $2 "  " p }' \
    "$logs/plan/$1.inputs" > "$logs/$1.recheck.sha256" || return 1
  [ -s "$logs/$1.recheck.sha256" ] || return 0
  (cd "$root" && sha256sum -c "$logs/$1.recheck.sha256") > "$logs/$1.recheck" 2>&1
}
record_pass() {  # record_pass NAME: its pass in the store at its key (GTS7 (a)), with its outputs (GT5)
  # queue 023, report 022 finding 2: not recorded when an input changed while the step ran (its key is the
  # plan's snapshot's)
  if ! inputs_unchanged "$1"; then
    echo "gate: the pass of $1 not recorded: its inputs changed while it ran: $(grep -v ': OK$' "$logs/$1.recheck" | head -5 | tr '\n' ' ')($logs/$1.recheck)" >&2
    return 1
  fi
  # GTS80 (a): every file through a temporary name renamed into place, the record last (a pass is its
  # .edn), each checked whole first: a full disk leaves temporary files (removed), never a cut pass. The
  # temporaries are dotfiles ending in .tmp, never .edn (review 001, finding 3: gate.plan's passes reads
  # *.edn, a killed gate's leftover would count as a pass). Review 001, finding 6: every file written and
  # checked under its temporary name before any is renamed, so that a failure to write (at a key that has a
  # pass already: --rerun, the canary) leaves the old record with its own log; then the old record removed,
  # the companions renamed, the record last (a failure there leaves no pass, never one with another's log)
  local name=$1 k=${key[$1]} d=$store/$1 t f secs cpu rss tree
  mkdir -p "$d" || return 1
  t=$d/.$k.$$
  rm -rf "${t:?}".*
  cp "$logs/$name.log" "$t.log.tmp" && cmp -s "$logs/$name.log" "$t.log.tmp" \
    && cp "$logs/plan/$name.inputs" "$t.inputs.tmp" && cmp -s "$logs/plan/$name.inputs" "$t.inputs.tmp" \
    || { rm -rf "${t:?}".*; return 1; }
  if [ "${outputs[$name]}" = yes ]; then
    cp -a "$logs/out/$name" "$t.out.tmp" && diff -rq "$logs/out/$name" "$t.out.tmp" > "$t.diff.tmp" \
      && rm -f "$t.diff.tmp" || { rm -rf "${t:?}".*; return 1; }
  fi
  # review 001, finding 5: a number not measured is nil, so that the record is a whole map (an empty value
  # made it odd-length, which the Clojure reader rejects)
  secs=$(cat "$logs/$name.secs" 2>"$logs/mem-of.err") cpu=$(mem_of "$name" cpu-s) rss=$(mem_of "$name" maxrss-kib) \
    tree=$(mem_of "$name" tree-peak-kib)
  printf '{:step "%s" :key "%s" :commit "%s" :dirty %s :worktree "%s" :date "%s"\n :secs %s :cpu-s %s :maxrss-kib %s :tree-peak-kib %s\n :log-sha256 "%s"%s}\n' \
    "$name" "$k" "$commit" "$dirty" "$root" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${secs:-nil}" \
    "${cpu:-nil}" "${rss:-nil}" "${tree:-nil}" \
    "$(sha256sum < "$logs/$name.log" | cut -c1-64)" \
    "$([ -n "${GATE_CLEAN_INNER:-}" ] && echo ' :clean true')" > "$t.edn.tmp" \
    && pass_reads "$t.edn.tmp" "$name" "$k" "$t.log.tmp" || { rm -rf "${t:?}".*; return 1; }
  rm -f "$d/$k.edn" || { rm -rf "${t:?}".*; return 1; }
  for f in log inputs out; do
    [ -e "$t.$f.tmp" ] || continue
    if [ "$f" = out ]; then rm -rf "${d:?}/$k.out" && mv -T "$t.out.tmp" "$d/$k.out"
    else mv -f "$t.$f.tmp" "$d/$k.$f"; fi || { rm -rf "${t:?}".*; return 1; }
  done
  mv -f "$t.edn.tmp" "$d/$k.edn" || { rm -rf "${t:?}".*; return 1; }
}

# the plan's steps, from plan.tsv: NAME DECISION KEY PHASE CHAIN NEEDS START-AFTER HEAVY MEM BUDGET
# SUMMARY-N SUMMARY-RE WHY AUDIT-CMD OUTPUTS USES KIND DECLARED DISK CMD (GD4: MEM the admission, the declaration or 1.1 x the
# recorded peak when larger, GDS13 (a); DECLARED the table's :mem-gib; DISK its :disk-gib, GDS12 (a))
order=()
declare -A dec key phase chain needs after heavy memgib declgib diskgib budget sum_n sum_re why acmd outputs uses kind cmd ordix
load_plan() {  # load_plan: plan.tsv into the arrays (again after a second plan: the order anew; ordix, a step's index in it)
  local n d k p c ne a hv mg bu sn sre w ac ou us ki dm dg cm
  order=()
  while IFS=$'\t' read -r n d k p c ne a hv mg bu sn sre w ac ou us ki dm dg cm; do
    ordix[$n]=${#order[@]}
    order+=("$n"); dec[$n]=$d key[$n]=$k phase[$n]=$p chain[$n]=$c needs[$n]=${ne//,/ } after[$n]=$a
    heavy[$n]=$hv memgib[$n]=$mg budget[$n]=$bu sum_n[$n]=$sn sum_re[$n]=$sre why[$n]=$w acmd[$n]=$ac
    outputs[$n]=$ou uses[$n]=${us//,/ } kind[$n]=$ki declgib[$n]=$dm diskgib[$n]=$dg cmd[$n]=$cm
  done < "$logs/plan.tsv"
}
# (night: the crops tier's subset deferred to the night's gate, gate.plan's crop-why; never a need of a step that runs, which the
# planner wakes, but not a failure of one)
ok_status() { [ -f "$logs/$1.status" ] && case $(cat "$logs/$1.status") in 0|skip|absent|night) return 0 ;; esac; return 1; }
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
          rotation_note "$n" skip
          echo skip > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
    none) echo none > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
    # the oracle tier (doc/ORACLE-RECORDS-SURVEY.md, ORS1 (a)): a step whose toolchain this host lacks
    absent) echo absent > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
    # the crops tier (doc/GATE-DIET-SURVEY.md, "As built: the crops tier"): a crop's subset whose crop did not change, deferred to
    # the night's gate: no run, no failure, no pass
    night) echo night > "$logs/$n.status"; touch "$logs/$n.done"; return 0 ;;
  esac
  local nw
  for x in ${needs[$n]}; do
    [ "$x" = - ] && continue
    # GTS-A: a needed step of another runner (a chain beside this one) not done yet is waited for
    nw=$(need_wait "$n" "$x")
    [ -z "$nw" ] || { echo "notrun $x: $nw" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
    # GD4 (GDS12 (a)): a step whose need was deferred for the disk is deferred with it (retried with it), not failed (after
    # GTS-A's wait, so that a need of another chain is judged once it is done)
    if [ "$(cat "$logs/$x.status" 2>"$logs/$n.need.err")" = deferred ]; then
      echo "its need $x was deferred" > "$logs/$n.deferred"; echo deferred > "$logs/$n.status"; touch "$logs/$n.done"; return 1
    fi
    ok_status "$x" || { echo "notrun $x" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
  done
  if [ "${dec[$n]}" = after ]; then
    # GT5: the key once the producers ran, their outputs' digests appended to the plan's material
    for u in ${uses[$n]}; do echo "uses $u $(out_sha "$logs/out/$u")" >> "$logs/plan/$n.inputs"; done
    key[$n]=$(sha256sum < "$logs/plan/$n.inputs" | cut -c1-64)
    # (re-review 020, finding 4: a record that reads, pass_reads, as the planner's and publish_passes'
    # decisions: an empty, cut or odd-length one at the key is no pass, and the step runs)
    if [ -z "${rerun:-}" ] && pass_reads "$store/$n/${key[$n]}.edn" "$n" "${key[$n]}"; then
      why[$n]="cut off: its producers' outputs as at its pass; $(pass_desc "$store/$n/${key[$n]}.edn")"
      dec[$n]=skip; echo "${why[$n]}" > "$logs/$n.why"
      restore_out "$n" || { echo "notrun its outputs" > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
      echo skip > "$logs/$n.status"; touch "$logs/$n.done"; return 0
    fi
    dec[$n]=run
  fi
  local attempt aud= hv=${heavy[$n]}
  [ -n "${audit_all:-}" ] && aud=1
  # GD4 (GDS12 (a)): the disk floor first (a step below it waits, then is deferred: no run, no failure, retried at the end)
  disk_admit "$n" || { echo deferred > "$logs/$n.status"; touch "$logs/$n.done"; return 1; }
  gate_cache_cap
  # GTS57 (a): a build kept at its key (a few seconds) takes no lock and queues for none
  if [ "${kind[$n]}" = build ] && [ "$hv" = yes ] && build_cached "$n"; then hv=cached; fi
  for attempt in 1 2 3; do
    if [ "${outputs[$n]}" = yes ]; then rm -rf "${logs:?}/out/$n"; mkdir -p "$logs/out/$n"; fi
    # GATE_FAIL_LINES (review 181 L3): a file the step's judge writes every failure line into, in its log's format (the
    # log shows the first 40), which rotation_note and judge_note read with the log
    rm -f "${logs:?}/$n.fail-lines"
    # GD4 (GDS13 (a)): the admission (MEM, the plan's: the declaration or 1.1 x the recorded peak) handed to the tools as
    # GATE_STEP_MEM_GIB, from which their GOMEMLIMIT defaults are capped (bin/gate-policy mem); unset for an undeclared,
    # unrecorded step (the runner's 1 GiB is the lane's, no tool's limit)
    gsm=; [ "${memgib[$n]:--}" = - ] || gsm=${memgib[$n]}
    GATE_STEP_MEM_GIB=$gsm step_lane=$( [ "$hv" = cached ] || { m=${memgib[$n]:--}; [ "$m" = - ] && m=1; echo "$m"; } ) \
    audit_step=$aud GATE_OUT=$logs/out/$n GATE_OUTS=$logs/out GATE_LIST=$logs/rotation/$n.list GATE_USES="${uses[$n]}" GATE_FAIL_LINES=$logs/$n.fail-lines step "$n" "$hv" bash -c "${cmd[$n]}"; st=$?
    # GT4: strace -f aborts (ptrace(PTRACE_LISTEN): I/O error) when a traced process dies in a group
    # stop; an aborted trace is no audit and its run no run: again, twice at most
    [ -n "$aud" ] && grep -q '^strace: ptrace' "$logs/$n.log" || break
    echo "$attempt" > "$logs/$n.audit-aborts"
    cp "$logs/$n.log" "$logs/$n.log.aborted$attempt"
  done
  # GD4 (GDS12 (a)): a step whose log or outputs say "No space left on device" fails (a full disk's corrupt saves are no
  # pass), whatever its status; NAME.enospc names where
  if enospc "$n" && [ $st = 0 ]; then st=28; echo $st > "$logs/$n.status"; fi
  [ -n "$aud" ] && audit_compare "$n" "$st"
  if [ $st = 0 ]; then rotation_note "$n" pass; else rotation_note "$n" fail; judge_note "$n"; fi
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
# rotation_note NAME pass|fail|skip (GT8.4, doc/GATE-SURVEY.md, "As built: GT8.4, GT8.7 (write-ahead)"): a rotated
# check's run (the planner's LOGS/rotation/NAME.rot: its part, slice, list's digest, :failed-re) appended to the
# store's STORE/_rotation/NAME.runs (EPOCH DATE STATUS KEY LIST-SHA COMMIT WORKTREE), which the night's choice of
# a slice and bin/gate --rotation-status read. A failure's files (its log's lines :failed-re names, sed -E's
# group 1, that are in its list) join the part's subset (STORE/_rotation/PART.failed) until a run of the
# subset (or the whole part) passes them. A rotated part run whole (--full; the clean run with GATE_CLEAN_WHOLE=1)
# is noted as NAME.PART.whole.runs, its list the universe: a run of every slice. In the clean run the clone's store
# gets them, and bin/gate's clean_launch appends its runs to the shared store's. Never for an unrotated check
rotation_note() {
  local rd=$store/_rotation
  [ -f "$logs/rotation/$1.rot" ] || return 0
  mkdir -p "$rd" || return 0
  # review 181: under the store's lock (L1: two gates' notes lose no failed file), each run numbered by the store's
  # sequence (finding 2: the run noted last is the later one, whatever the clock's second)
  { flock 9 && rotation_note_locked "$@"; } 9>>"$rd/.lock"
  return 0
}
rotation_note_locked() {
  local n=$1 r=$logs/rotation/$1.rot rd=$store/_rotation part slice ls re as now rn sq lg=${3:-$logs/$1.log}
  part=$(awk -F'\t' '$1 == "part" {print $2}' "$r") slice=$(awk -F'\t' '$1 == "slice" {print $2}' "$r")
  ls=$(awk -F'\t' '$1 == "list-sha" {print $2}' "$r") re=$(awk -F'\t' '$1 == "failed-re" {print $2}' "$r")
  as=$(awk -F'\t' '$1 == "failed-as" {print $2}' "$r"); as=${as:-\\1}
  # a judge's failure (judge_note): its log and its own expressions
  [ -z "${4:-}" ] || { re=$4 as=$5; }
  [ -n "${3:-}" ] || lg=$(fail_source "$n")
  rn=$(awk -F'\t' '$1 == "runs" {print $2}' "$r"); rn=${rn:-$n}
  now=$(date +%s)
  sq=$(( $(cat "$rd/.seq" 2>"$logs/$n.rot.err" || echo 0) + 1 )); echo "$sq" > "$rd/.seq"
  # review 181 (the user's (a)): the commit and whether its tree was dirty, so that only a clean ancestor's runs count
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$now" "$(date -u -d "@$now" +%Y-%m-%dT%H:%M:%SZ)" "$2" "${key[$n]:--}" "$ls" \
    "${commit:-unknown}" "$root" "$sq" "${dirty:-unknown}" >> "$rd/$rn.runs"
  # the failed files: events appended (SEQ EPOCH ID fail|clear COMMIT DIRTY), never rewritten, so that each commit's
  # lineage sees its own (the planner's failed-of)
  local ids id
  if [ "$2" = fail ] && [ -n "$re" ] && [ -f "$lg" ]; then
    ids=$(sed -nE "s"$'\001'"$re"$'\001'"$as"$'\001''p' "$lg" | grep -Fxf "$logs/rotation/$n.list" | LC_ALL=C sort -u)
    # one id a line (a name may hold spaces: cxgo's "main with args.c")
    [ -z "$ids" ] || while IFS= read -r id; do printf '%s\t%s\t%s\tfail\t%s\t%s\n' "$sq" "$now" "$id" "${commit:-unknown}" "${dirty:-unknown}"; done <<< "$ids" >> "$rd/$part.failed"
    [ -n "$ids" ] && echo "   $n: $(echo "$ids" | wc -l) file(s) of $part noted as failed: in its subset until they pass ($rd/$part.failed)" >> "$logs/$n.log"
  elif [ "$2" = pass ] && { [ "$slice" = sub ] || [ "$slice" = whole ]; } && [ -s "$rd/$part.failed" ]; then
    ids=$(awk -F'\t' '$4 == "fail" {print $3}' "$rd/$part.failed" | LC_ALL=C sort -u | grep -Fxf "$logs/rotation/$n.list")
    [ -z "$ids" ] || while IFS= read -r id; do printf '%s\t%s\t%s\tclear\t%s\t%s\n' "$sq" "$now" "$id" "${commit:-unknown}" "${dirty:-unknown}"; done <<< "$ids" >> "$rd/$part.failed"
  fi
  return 0
}
# fail_source NAME: the file a step's failure lines are read from: its log, and when its judge wrote GATE_FAIL_LINES
# (every failure line, where the log prints the first 40: review 181 L3, a mass regression's files all join the subset
# at once) the two together, LOGS/NAME.fail-all
fail_source() {
  if [ -s "$logs/$1.fail-lines" ]; then
    cat "$logs/$1.log" "$logs/$1.fail-lines" > "$logs/$1.fail-all" 2>"$logs/$1.fail-all.err"; echo "$logs/$1.fail-all"
  else
    echo "$logs/$1.log"
  fi
}
# judge_note NAME (GT8, doc/GATE-SURVEY.md, "As built: GT8 (the tools' lists)"): a failed judge of rotated checks
# (a step whose :judges {:failed-re ERE :failed-as REPL} the planner wrote as LOGS/rotation/NAME.judges, its :uses'
# rotated producers): each producer whose list has an id its log's failure lines name is noted as failed with them
# (rotation_note's fail: a run of its slice that failed, its files into the subset), since the producer's own run,
# which judges nothing (Java's levels' --save), passed
judge_note() {
  local n=$1 j=$logs/rotation/$1.judges re as p
  [ -f "$j" ] && [ -f "$logs/$n.log" ] || return 0
  re=$(awk -F'\t' '$1 == "failed-re" {print $2}' "$j") as=$(awk -F'\t' '$1 == "failed-as" {print $2}' "$j"); as=${as:-\\1}
  [ -n "$re" ] || return 0
  local src; src=$(fail_source "$n")
  for p in $(awk -F'\t' '$1 == "producer" {print $2}' "$j"); do
    [ -f "$logs/rotation/$p.rot" ] || continue
    sed -nE "s"$'\001'"$re"$'\001'"$as"$'\001''p' "$src" | grep -qFxf "$logs/rotation/$p.list" || continue
    rotation_note "$p" fail "$src" "$re" "$as"
    echo "   $n: failed ids of $p's list noted as its failure (judge_note)" >> "$logs/$n.log"
  done
  return 0
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
    absent) echo "   absent  $n (${why[$n]})"; return ;;
    night) echo "   night  $n (${why[$n]})"; return ;;
    # GD4 (GDS12 (a)): deferred for the disk: not a failure; bin/gate retries it at the end, and a run left with one is incomplete
    deferred) echo "   deferred  $n ($(cat "$logs/$n.deferred" 2>"$logs/count.err"))"; return ;;
    notrun*:*) echo "   FAILED  $n (not run: needs ${st#notrun })"; status=1; return ;;
    notrun*) echo "   FAILED  $n (not run: ${st#notrun } did not pass)"; status=1; return ;;
  esac
  s=$(cat "$logs/$n.secs"); w=$(mmss "$s")
  local c; c=$(mem_of "$n" cpu-s); c=${c%.*}
  w="$w, cpu $(mmss "${c:-0}"), peak $(gib "$(mem_of "$n" maxrss-kib)") (tree $(gib "$(mem_of "$n" tree-peak-kib)"))"
  wt=$(cat "$logs/$n.wait" 2>"$logs/count.err"); wt=${wt:-0}
  # the essential memory lane: the wait for room in it (NAME.lane-wait, bin/gate-lane's), the rest the lock's
  local lw; lw=$(cat "$logs/$n.lane-wait" 2>"$logs/count.err"); lw=${lw:-0}
  [ "$lw" -ge 5 ] && w="$w, waited $(mmss "$lw") for room in the memory lane"
  wt=$(( wt - lw ))
  [ "$wt" -ge 5 ] && w="$w, waited $(mmss "$wt") for the lock"
  wt=$(mem_of "$n" inner-wait-s); wt=${wt:-0}
  [ "$wt" -ge 5 ] && w="$w, its tools waited $(mmss "$wt") for the lock"
  # the essential memory lane: a peak (the larger of the largest process's and the tree's) over the step's declared
  # :mem-gib by test/gate/lanes.ae's mem-check margin (10% and 0.5 GiB) flagged, never failed (bin/gate --mem-check
  # and gate-tests' lanes-test hold the declarations to the recorded peaks)
  local mg=${memgib[$n]:--} pk
  if [ "$mg" != - ]; then
    pk=$(mem_of "$n" maxrss-kib); local tp; tp=$(mem_of "$n" tree-peak-kib)
    awk -v a="${pk:-0}" -v b="${tp:-0}" -v d="$mg" 'BEGIN {p = (a > b ? a : b) / 1048576; exit !(p > d * 1.1 + 0.5)}' \
      && w="$w; OVER ITS DECLARED :mem-gib $mg"
  fi
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
  local ns=
  [ -f "$logs/$n.enospc" ] && ns="; NO SPACE LEFT ON DEVICE in $(cat "$logs/$n.enospc"): no pass recorded"
  if [ "$st" = 0 ]; then
    echo "   ok  $n ($w$au)"
  else
    echo "   FAILED  $n ($w$au$ns; log $logs/$n.log)"; status=1
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
chain_steps() {  # chain_steps PHASE CHAIN: its steps in the plan's order
  local n
  for n in "${order[@]}"; do [ "${phase[$n]}" = "$1" ] && [ "${chain[$n]}" = "$2" ] && echo "$n"; done
  return 0
}
queue_chains() {  # queue_chains PHASE: every chain's queue written (queue_write) before any of them starts (GTS-A)
  local c
  for c in $(chains_of "$1"); do queue_write "$1.$c" $(chain_steps "$1" "$c"); done
}
run_chain() {  # run_chain PHASE CHAIN: its steps one after the other (after its :start-after), a runner PHASE.CHAIN
  local q=()
  q=($(chain_steps "$1" "$2"))
  [ ${#q[@]} -gt 0 ] || return 0
  if [ "${after[${q[0]}]}" != - ]; then
    while [ ! -f "$logs/${after[${q[0]}]}.done" ]; do sleep 5; done
  fi
  run_queue "$1.$2" "${q[@]}"
}
# The runners (GTS-A, doc/GATE-SURVEY.md, "As built: GTS-A"): run_queue ID STEP... runs its steps one after the other
# as the runner ID, its queue in LOGS/runners/ID.queue (a step a line) and its end in ID.end. A step whose :needs names a
# step of another runner not done yet waits for it (need_wait: its .done), the wait in ID.waits ("STEP NEEDED"); it fails
# at once, as before, on a needed step in no runner's queue (a step of an earlier phase is done, of a later one never
# started) or later in its own runner's queue, and while it waits on a needed step whose runner ended without it (never
# started) or on a cycle of waits (its needed step's runner waits, through others, on this runner). Outside a runner
# (the builds, the oracle tier's loop) every need is decided at once, as before
# (queue_write ID STEP...: the queue alone, written before the runners start, so that no runner looks for a step of a
# runner not yet started; bin/gate writes them all, queue_chains, before it starts any)
queue_write() {
  local id=$1; shift
  mkdir -p "$logs/runners"
  rm -f "$logs/runners/$id.end" "$logs/runners/$id.waits" "$logs/runners/$id.pid"
  printf '%s\n' "$@" > "$logs/runners/$id.queue.tmp" && mv -f "$logs/runners/$id.queue.tmp" "$logs/runners/$id.queue"
}
run_queue() {  # run_queue ID STEP...
  local gate_runner=$1 n; shift
  queue_write "$gate_runner" "$@"
  echo "$BASHPID" > "$logs/runners/$gate_runner.pid"
  for n in "$@"; do run_one "$n"; done
  touch "$logs/runners/$gate_runner.end"
}
runner_of() {  # runner_of STEP: the runner whose queue holds it (none: nothing)
  local f
  for f in "$logs"/runners/*.queue; do
    [ -f "$f" ] && grep -qxF "$1" "$f" && { f=${f##*/}; echo "${f%.queue}"; return 0; }
  done
  return 0
}
# need_wait STEP NEEDED: waits for NEEDED when it is another runner's and not done; prints why it will not be (a cycle,
# its runner ended, later in this runner's own queue), else nothing
need_wait() {
  local n=$1 x=$2 r
  [ -f "$logs/$x.done" ] && return 0
  [ -n "${gate_runner:-}" ] || return 0
  # GATE_NEED_WAIT=0: no wait, every need decided at once (before GTS-A; its test's demonstration of the old failure)
  [ "${GATE_NEED_WAIT:-1}" = 0 ] && return 0
  r=$(runner_of "$x")
  [ -n "$r" ] || return 0
  [ "$r" = "$gate_runner" ] && { echo "later in its own chain ($r): never run before $n"; return 0; }
  echo "$n $x" > "$logs/runners/$gate_runner.waits"
  echo "   $n waits for $x (runner $r)" >> "$logs/runners/waits.log"
  local why=
  while [ ! -f "$logs/$x.done" ]; do
    if [ -f "$logs/runners/$r.end" ]; then
      [ -f "$logs/$x.done" ] || why="never run: its chain ($r) ended without it"; break
    fi
    # a runner that started and is gone without its end (killed): its step will never run
    local rp; rp=$(cat "$logs/runners/$r.pid" 2>"$logs/runners/pid.err")
    if [ -n "$rp" ] && ! kill -0 "$rp" 2>"$logs/runners/pid.err" && [ ! -f "$logs/runners/$r.end" ] && [ ! -f "$logs/$x.done" ]; then
      why="never run: its chain's runner ($r, PID $rp) is gone"; break
    fi
    why=$(need_cycle "$x" "$n -> $x")
    [ -n "$why" ] && break
    sleep "${GATE_NEED_POLL:-5}"
  done
  rm -f "$logs/runners/$gate_runner.waits"
  [ -z "$why" ] || echo "$why"
}
# need_cycle NEEDED PATH: a cycle of waits back to this runner (gate_runner): NEEDED's runner waits on a step whose
# runner waits ... on a step of this runner's queue not done (the steps of a runner run in order, so a step not done
# in a waiting runner's queue is after its waiting step). Prints "a cycle of waits: PATH", else nothing
need_cycle() {
  local cur=$1 path=$2 r m y i=0
  while [ $i -le 64 ]; do
    i=$((i + 1))
    [ -f "$logs/$cur.done" ] && return 0
    r=$(runner_of "$cur")
    [ -n "$r" ] || return 0
    [ "$r" = "$gate_runner" ] && { echo "a cycle of waits: $path"; return 0; }
    [ -f "$logs/runners/$r.waits" ] || return 0
    read -r m y < "$logs/runners/$r.waits" 2>"$logs/runners/cycle.err" || return 0
    [ -n "$y" ] || return 0
    path="$path (in $r, waiting at $m) -> $y" cur=$y
  done
  return 0
}
chains_of() {  # chains_of PHASE: its chains, in the order of their first step
  local n seen=" "
  for n in "${order[@]}"; do
    [ "${phase[$n]}" = "$1" ] || continue
    case $seen in *" ${chain[$n]} "*) ;; *) seen="$seen${chain[$n]} "; echo "${chain[$n]}" ;; esac
  done
}
names_of() { local n; for n in "${order[@]}"; do [ "${phase[$n]}" = "$1" ] && echo "$n"; done; }

# ---- GD4 (doc/GATE-DIET-SURVEY.md, GDS12 (a), "As built: GD4"): the gate's disk policy
# disk_free_kib: the free KiB of the checkout's file system (GATE_DISK_FREE_FILE: a file holding the number, the gate's own
# tests, with a store of their own)
disk_free_kib() {
  if [ -n "${GATE_DISK_FREE_FILE:-}" ] && [ -n "${ARBACE_GATE_STORE:-}" ]; then cat "$GATE_DISK_FREE_FILE"; return; fi
  df -Pk "$root" 2>"$logs/df.err" | awk 'NR == 2 {print $4 + 0}'
}
# disk_admit NAME: 0 when the free disk holds the step's :disk-gib (DISK, the plan's) plus the floor GATE_DISK_FLOOR (GiB,
# default 25). Below it: the prune's cheap rules once a low spell (gate_prune --cheap, LOGS/disk.log), then a wait for the
# disk to come back, GATE_DISK_WAIT seconds (default 1800) shared by the run's steps from the spell's start (LOGS/disk.low-since:
# a later step waits only what is left, so a night of low disk defers its steps at once and does not wait 30 minutes each),
# polled every GATE_DISK_POLL (30); then 1, NAME.deferred saying why. GATE_DISK_FLOOR=0 and no :disk-gib: never deferred
disk_admit() {
  local n=$1 need free since now f=${GATE_DISK_FLOOR:-25} d=${diskgib[$1]:--} w=${GATE_DISK_WAIT:-1800}
  [ "$d" = - ] && d=0
  need=$(awk -v f="$f" -v d="$d" 'BEGIN {printf "%d", (f + d) * 1048576}')
  [ "$need" -gt 0 ] || return 0
  free=$(disk_free_kib); free=${free:-0}
  if [ "$free" -ge "$need" ]; then rm -f "${logs:?}/disk.low-since"; return 0; fi
  if [ ! -f "$logs/disk.low-since" ]; then
    date +%s > "$logs/disk.low-since"
    echo "$(date -u +%T) $n: $(gib "$free") free, below $(gib "$need") (the floor $f GiB + its :disk-gib $d): the cheap prune" >> "$logs/disk.log"
    gate_prune --cheap >> "$logs/disk.log" 2>&1
  fi
  since=$(cat "$logs/disk.low-since" 2>"$logs/df.err"); since=${since:-$(date +%s)}
  while :; do
    free=$(disk_free_kib); free=${free:-0}
    if [ "$free" -ge "$need" ]; then rm -f "${logs:?}/disk.low-since"; echo "$(date -u +%T) $n: the disk back ($(gib "$free") free)" >> "$logs/disk.log"; return 0; fi
    now=$(date +%s)
    [ $(( now - since )) -lt "$w" ] || break
    sleep "${GATE_DISK_POLL:-30}"
  done
  echo "the disk: $(gib "$free") free, below the floor $f GiB + its :disk-gib $d (waited from $(date -u -d "@$since" +%T), GATE_DISK_WAIT $w s)" > "$logs/$n.deferred"
  echo "$(date -u +%T) $n: deferred" >> "$logs/disk.log"
  return 1
}
# enospc NAME: 0 (and NAME.enospc naming where) when the step's log, its failure lines or its outputs (GATE_OUT: the saves) say
# "No space left on device", the message of ENOSPC (a full disk, or ext4's full directory index: night 12's 208,000 entries)
enospc() {
  local n=$1 f w=
  rm -f "${logs:?}/$n.enospc"
  for f in "$logs/$n.log" "$logs/$n.fail-lines"; do
    [ -f "$f" ] && grep -qsF 'No space left on device' "$f" && w="$w ${f##*/}"
  done
  [ -d "$logs/out/$n" ] && grep -rqsIF 'No space left on device' "$logs/out/$n" && w="$w its outputs"
  [ -n "$w" ] || return 1
  echo "${w# }" > "$logs/$n.enospc"
}
# disk_retry: the steps deferred for the disk run again, in the plan's order, once the run's chains are done (the night's
# slack), each admitted without a wait (the floor checked once); prints their reports. A step deferred again stays deferred
disk_retry() {
  local n any=
  for n in "${order[@]}"; do [ "$(cat "$logs/$n.status" 2>"$logs/count.err")" = deferred ] && any=1; done
  [ -n "$any" ] || return 0
  echo "   the steps deferred for the disk, again ($(gib "$(disk_free_kib)") free):"
  for n in "${order[@]}"; do
    [ "$(cat "$logs/$n.status" 2>"$logs/count.err")" = deferred ] || continue
    rm -f "${logs:?}/$n.done" "${logs:?}/$n.status" "${logs:?}/$n.deferred"
    rm -f "${logs:?}/disk.low-since"
    GATE_DISK_WAIT=0 run_one "$n"; report "$n"
  done
}
# deferred_count: the steps left deferred
deferred_count() {
  local n c=0
  for n in "${order[@]}"; do [ "$(cat "$logs/$n.status" 2>"$logs/count.err")" = deferred ] && c=$((c + 1)); done
  echo "$c"
}

# GTS27 (a): per step the last ARBACE_GATE_KEEP (5) passes and every pass younger than
# ARBACE_GATE_KEEP_DAYS (30) kept, the rest removed (a pass is KEY.edn, KEY.log, KEY.inputs); the
# executables' directories (the gate's, javac's, the cover builds', GT8.3) older than a day but the newest 3 of each removed; a
# KEY.log, KEY.inputs or KEY.out with no KEY.edn, and a temporary, removed once older than a day
gate_prune() {  # gate_prune [--dry-run] [--cheap] [--worktrees]
  local o dry= cheap= wts=
  for o in "$@"; do case $o in --dry-run) dry=--dry-run ;; --cheap) cheap=1 ;; --worktrees) wts=1 ;; esac; done
  # GD4 (GDS17 (a)): --cheap, the table's cheap rules alone (seconds: Go's caches by size, TMPDIR by age)
  if [ -n "$cheap" ]; then caches_prune "$dry" cheap; return 0; fi
  gate_prune_store $dry
  caches_prune "$dry" all
  [ -z "$wts" ] || worktrees_list
  return 0
}
gate_prune_store() {  # gate_prune_store [--dry-run]: GTS27 (a)'s store and executables, ci's runtime (the table's :builtin rows)
  local dry= keep=${ARBACE_GATE_KEEP:-5} days=${ARBACE_GATE_KEEP_DAYS:-30} d n f k i pat npass=0 nkeep=0 freed=0 sz b now
  local norphan=0 old
  now=$(date +%s)
  [ "${1:-}" = --dry-run ] && dry=1
  [ -d "$store" ] || { echo "gate --prune: no store $store"; return 0; }
  for d in "$store"/*/; do
    [ -d "$d" ] || continue
    n=$(basename "$d"); i=0
    # the rotation's record is no step's passes: its .seq and .lock are never temporaries (review 181 L7)
    [ "$n" = _rotation ] && continue
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      i=$((i + 1)); npass=$((npass + 1)); k=${f%.edn}
      if [ $i -le "$keep" ] || [ $(( now - $(stat -c %Y "$f") )) -lt $(( days * 86400 )) ]; then nkeep=$((nkeep + 1)); continue; fi
      sz=$(du -sb "$k".edn "$k".log "$k".inputs "$k".out 2>"$store/.prune.err" | awk '{s += $1} END {print s + 0}'); freed=$((freed + sz))
      echo "gate --prune: ${dry:+would remove }$n/$(basename "$k" | cut -c1-12) ($(stat -c %y "$f" | cut -c1-10), $sz bytes)"
      [ -n "$dry" ] || rm -rf "${k:?}.edn" "${k:?}.log" "${k:?}.inputs" "${k:?}.out"
    done <<< "$(ls -t "$d" | sed -n "s|^\([0-9a-f]*\.edn\)$|$d\1|p")"
    # re-review 020, finding 7: a pass's companions left with no record (a record_pass or publish_passes
    # interrupted after the old record's removal or the companions' rename) removed once older than a day,
    # as the temporaries below; a younger one may be a publication in progress
    while IFS= read -r k; do
      [ -n "$k" ] && [ ! -e "$d$k.edn" ] || continue
      old=()
      for f in "$d$k.log" "$d$k.inputs" "$d$k.out"; do
        [ -e "$f" ] && [ $(( now - $(stat -c %Y "$f") )) -ge 86400 ] && old+=("$f")
      done
      [ ${#old[@]} -gt 0 ] || continue
      norphan=$((norphan + 1))
      echo "gate --prune: ${dry:+would remove }$n/${k:0:12} (no record: its$(for f in "${old[@]}"; do printf ' .%s' "${f##*.}"; done))"
      [ -n "$dry" ] || rm -rf "${old[@]}"
    done <<< "$(ls "$d" | sed -nE 's/^([0-9a-f]+)\.(log|inputs|out)$/\1/p' | sort -u)"
    [ -n "$dry" ] || find "$d" -maxdepth 1 -name '.*' -mmin +1440 -exec rm -rf {} + 2>"$store/.prune.err"
  done
  rotation_prune "$now" "$days" "$dry"
  echo "gate --prune: $npass passes, $nkeep kept, $((npass - nkeep)) ${dry:+would be }removed ($freed bytes), $norphan left without a record ${dry:+would be }removed in $store"
  # platform: the own platform by its pin, ARBACE_GATE_EXES/platform-PIN20 (doc/classes/JAVA-FRONTEND-SURVEY.md, "Own
  # platform", JOP3 (a)), its own kind: the newest 3 kept, as javac's tools
  for pat in gate javac cover platform; do
    i=0
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      b=$(basename "$f")
      # a miss's copy (KEY20.tmp.PID) or old directory (KEY20.old.PID, bin/arbace-go's gate_miss): never one of the
      # newest 3, removed when an hour old (younger: a build under way). The hidden .KEY20.lock files beside the
      # directories stay: unlinking a lock file someone has open lets two runs lock different inodes
      case $b in *.tmp.*|*.old.*)
        [ "$pat" = gate ] || continue # once, not in the javac pass too
        [ $(( now - $(stat -c %Y "$f") )) -ge 3600 ] || continue
        echo "gate --prune: ${dry:+would remove }executables $f (a miss's leftover)"
        [ -n "$dry" ] || rm -rf "${f:?}"; continue ;; esac
      case $pat in javac|cover|platform) case $b in "$pat"-*) ;; *) continue ;; esac ;; gate) case $b in javac-*|cover-*|platform-*) continue ;; esac ;; esac
      i=$((i + 1)); [ $i -gt 3 ] || continue
      [ $(( now - $(stat -c %Y "$f") )) -ge 86400 ] || continue
      echo "gate --prune: ${dry:+would remove }executables $f"
      [ -n "$dry" ] || rm -rf "${f:?}"
    done <<< "$(ls -dt "$exes"/* 2>"$store/.prune.err")"
  done
  # ci's prebuilt runtime (doc/C23-SURVEY.md, "As built: ci's prebuilt runtime", CS272 (a)): the checkout's cache of
  # fragments, a directory per library and key (NAME-KEY); per library the newest 3 kept, an older one removed when a day old
  local rtc=${CI_RUNTIME:-$root/.tmp/ci-runtime} lib
  if [ -d "$rtc" ]; then
    for lib in $(ls "$rtc" | sed -n 's/^\(.*\)-[0-9a-f]\{64\}$/\1/p' | sort -u); do
      i=0
      while IFS= read -r f; do
        [ -n "$f" ] || continue
        i=$((i + 1)); [ $i -gt 3 ] || continue
        [ $(( now - $(stat -c %Y "$f") )) -ge 86400 ] || continue
        echo "gate --prune: ${dry:+would remove }ci's runtime $f"
        [ -n "$dry" ] || rm -rf "${f:?}"
      done <<< "$(ls -dt "$rtc/$lib"-* 2>"$store/.prune.err")"
    done
  fi
}

# ---- GD4 (doc/GATE-DIET-SURVEY.md, GDS17 (a), GDS18 (a), "As built: GD4"): the caches' table, test/gate/caches.edn
# (GATE_CACHES another, "none" no table: the gate's own tests), one entry a line (the format caches_rows reads, held by
# test/gate/caches-test.bash): :path, where it is (:where :checkout, this checkout's; :main, the main checkout's, shared by
# the worktrees; :home, under HOME; {gocache}: Go's build cache, go env GOCACHE), its :owner, its :rule:
#   [:size GIB :lru]   the files of the directory, the least recently used first (their mtime: Go refreshes an entry it
#                      uses, hourly), removed until it holds GIB at most
#   [:age DAYS]        the files not modified for DAYS days removed ([:age DAYS :atime]: not read: relatime's atime), then the
#                      empty directories
#   [:key-gen N]       the directory's children named by a key (:match, an ERE, default 16 to 64 hex digits): the newest N kept
#                      by mtime, an older one removed when a day old
#   [:count N]         the newest N children kept, an older one removed when a day old
#   [:age-self DAYS]   the path itself removed when its own mtime is DAYS days old (the gate's tests' scratch)
#   :builtin           gate_prune_store's own rule (the store, the executables, ci's runtime)
#   :keep              never removed (the clean runs' logs, the oracles' packs, locks), its tool's own eviction
#   :list              listed by --prune, never removed (the main session's: keep/, the worktrees)
# :cheap true: also by --cheap (a gate's start, a step below the disk floor); :max-entries N: a directory (any under :path)
# holding more entries is a warning of --prune (ext4's index filled at 208,000 in night 12); :shared true: content-keyed,
# one copy at the main checkout's .tmp/shared for every worktree at the same key (bin/lib/ci/musl's forms)
# A [:size GIB :lru] row may also take (n-gate-gocache, 2026-11-19: Go's build cache, whose warm set for go-build, javac's tool
# and the fixpoint is about 30 to 37 GiB; an 8 GiB cap started each gate's builds cold):
#   :free-gib F   the cap relative to the free disk: GIB at most, less when keeping it would leave under F GiB free on the
#                 cache's file system (the cap = min(GIB, its size + free - F)), never under :min-gib M (default 0): larger
#                 when the disk is free, smaller near GD4's floor (GATE_DISK_FLOOR, 25 GiB; F above it)
#   :idle-only true  nothing removed while a Go build runs on the host (a process whose executable is under ARBACE_GOROOT:
#                 go, compile, link, asm; GATE_CACHES_GO_BUSY=1|0 decides instead, the tests): the user's rule, never trim Go's
#                 cache under a live build (memory: a 2-hour trim still broke one); the row says it waited, and the next cap
#                 (before a step, every 5 minutes) or the floor's deferral of the steps takes it up
# caches_go_busy and gocache_cap_kib: bin/lib/gocache.bash's, the same rule a build's trim by hand follows (n-gocache-trim)
. "$root/bin/lib/gocache.bash"
caches_rows() {  # caches_rows TABLE: PATH WHERE RULE A1 A2 MATCH CHEAP MAXENT FREE MIN IDLE, a line each (TSV)
  awk '
    /^[ \t]*;/ { next }
    /:path "/ {
      l = $0; sub(/;.*$/, "", l)
      p = l; sub(/.*:path "/, "", p); sub(/".*/, "", p)
      w = "checkout"; if (match(l, /:where :[a-z]+/)) w = substr(l, RSTART + 8, RLENGTH - 8)
      r = "keep"; a1 = "-"; a2 = "-"
      if (match(l, /:rule \[[^]]*\]/)) {
        v = substr(l, RSTART + 7, RLENGTH - 8); n = split(v, f, /[ \t]+/)
        r = f[1]; sub(/^:/, "", r); if (n >= 2) a1 = f[2]; if (n >= 3) { a2 = f[3]; sub(/^:/, "", a2) }
      } else if (match(l, /:rule :[a-z-]+/)) r = substr(l, RSTART + 7, RLENGTH - 7)
      m = "-"; if (match(l, /:match "[^"]*"/)) m = substr(l, RSTART + 8, RLENGTH - 9)
      c = (l ~ /:cheap true/) ? "cheap" : "-"
      x = "-"; if (match(l, /:max-entries [0-9]+/)) x = substr(l, RSTART + 13, RLENGTH - 13)
      fr = "-"; if (match(l, /:free-gib [0-9.]+/)) fr = substr(l, RSTART + 10, RLENGTH - 10)
      mn = "-"; if (match(l, /:min-gib [0-9.]+/)) mn = substr(l, RSTART + 9, RLENGTH - 9)
      id = (l ~ /:idle-only true/) ? "idle" : "-"
      print p "\t" w "\t" r "\t" a1 "\t" a2 "\t" m "\t" c "\t" x "\t" fr "\t" mn "\t" id
    }' "$1"
}
caches_base() {  # caches_base WHERE PATH: the path on disk
  local w=$1 p=$2
  case $p in
    '{gocache}') if [ -n "${GATE_CACHES_GOCACHE:-}" ]; then echo "$GATE_CACHES_GOCACHE"; else "${ARBACE_GOROOT:-/root/tamago-go}/bin/go" env GOCACHE 2>"$TMPDIR/caches-go.err"; fi; return ;;
  esac
  case $w in
    main) echo "${GATE_CACHES_MAIN:-$(arbace_main_checkout "$root")}/$p" ;;
    home) echo "${GATE_CACHES_HOME:-$HOME}/${p#\~/}" ;;
    *) echo "${GATE_CACHES_ROOT:-$root}/$p" ;;
  esac
}
caches_prune() {  # caches_prune DRY(--dry-run or "") all|cheap
  local tab=${GATE_CACHES:-$root/test/gate/caches.edn} dry=$1 which=$2 p w r a1 a2 m c x base now n freed tot=0 rm=0 warn=0 g
  [ "$tab" = none ] && return 0
  [ -f "$tab" ] || { echo "gate --prune: no table $tab"; return 0; }
  now=$(date +%s)
  local lst=$TMPDIR/caches-prune.$$
  caches_rows "$tab" > "$lst.rows"
  local fr mn id capk
  while IFS=$'\t' read -r p w r a1 a2 m c x fr mn id; do
    [ "$which" = cheap ] && [ "$c" != cheap ] && continue
    for base in $(caches_base "$w" "$p"); do
      [ -e "$base" ] || continue
      : > "$lst"
      if [ "${id:--}" = idle ] && caches_go_busy; then
        echo "gate --prune: $base left alone: a Go build runs (:idle-only, never trimmed under a live build)"; continue
      fi
      case $r in
        size)
          # the least recently used files first, until the directory holds the cap: A1 GiB, or with :free-gib the smaller of
          # A1 and what leaves FREE GiB free (its size + the free disk - FREE), at least :min-gib
          capk=$(awk -v a="$a1" 'BEGIN {printf "%d", a * 1048576}')
          if [ "${fr:--}" != - ]; then
            local fk sk
            if [ -n "${GATE_CACHES_FREE_KIB:-}" ]; then fk=$GATE_CACHES_FREE_KIB; else fk=$(df -Pk "$base" 2>"$lst.err" | awk 'NR == 2 {print $4 + 0}'); fi
            sk=$(du -sk "$base" 2>"$lst.err" | cut -f1)
            capk=$(gocache_cap_kib "${sk:-0}" "${fk:-0}" "$a1" "$fr" "${mn/-/0}")
            echo "gate --prune: $base holds $(gib "${sk:-0}"), $(gib "${fk:-0}") free: its cap $(gib "$capk") (at most $a1 GiB, $fr GiB left free, at least ${mn/-/0} GiB)"
          fi
          find "$base" -type f -printf '%T@\t%s\t%p\n' 2>"$lst.err" | sort -n \
            | awk -F'\t' -v capk="$capk" '{ f[NR] = $3; s[NR] = $2; t += $2 } END { lim = capk * 1024; for (i = 1; i <= NR && t > lim; i++) { print s[i] "\t" f[i]; t -= s[i] } }' > "$lst" ;;
        age)
          if [ "$a2" = atime ]; then find "$base" -type f -atime +"$a1" -printf '%s\t%p\n' 2>"$lst.err" > "$lst"
          else find "$base" -type f -mtime +"$a1" -printf '%s\t%p\n' 2>"$lst.err" > "$lst"; fi ;;
        age-self)
          # the path itself (a test's scratch directory), when nothing in it changed for A1 days (its own mtime)
          [ $(( now - $(stat -c %Y "$base") )) -ge $(( a1 * 86400 )) ] && printf '%s\t%s\n' "$(du -sb "$base" 2>"$lst.err" | cut -f1)" "$base" > "$lst" ;;
        key-gen|count)
          [ -d "$base" ] || continue
          g=$m; [ "$g" = - ] && { [ "$r" = key-gen ] && g='^[0-9a-f]{16,64}$' || g='.'; }
          ls -t "$base" 2>"$lst.err" | grep -E "$g" | tail -n +$((a1 + 1)) | while IFS= read -r n; do
            [ $(( now - $(stat -c %Y "$base/$n") )) -ge 86400 ] || continue
            printf '%s\t%s\n' "$(du -sb "$base/$n" 2>"$lst.err" | cut -f1)" "$base/$n"
          done > "$lst" ;;
      esac
      if [ -s "$lst" ]; then
        freed=$(awk -F'\t' '{s += $1} END {print s + 0}' "$lst"); n=$(wc -l < "$lst")
        tot=$((tot + freed)); rm=$((rm + n))
        echo "gate --prune: ${dry:+would remove }$n entries of $base (its rule $r $a1$( [ "$a2" = - ] || echo " $a2"): $(gib $((freed / 1024))))"
        [ -n "$dry" ] || { cut -f2- "$lst" | xargs -r -d '\n' rm -rf --; [ "$r" = age ] && find "$base" -mindepth 1 -type d -empty -delete 2>"$lst.err"; }
      fi
      if [ "$x" != - ] && [ -d "$base" ]; then
        # GDS17 (a): no directory above :max-entries (night 12: ext4's directory index full at 208,000 entries)
        # (one pass: each entry's parent counted)
        find "$base" -mindepth 1 -printf '%h\n' 2>"$lst.err" | sort | uniq -c | awk -v x="$x" '$1 > x {n = $1; sub(/^ *[0-9]+ /, ""); print n "\t" $0}' > "$lst.big"
        while IFS=$'\t' read -r n g; do
          echo "gate --prune: WARNING: $g holds $n entries, more than its :max-entries $x (test/gate/caches.edn)"; warn=$((warn + 1))
        done < "$lst.big"
      fi
    done
  done < "$lst.rows"
  rm -f "$lst" "$lst.rows" "$lst.err" "$lst.big"
  echo "gate --prune: the caches' table ($which): $rm entries ${dry:+would be }removed ($(gib $((tot / 1024)))), $warn over their :max-entries"
  [ "$which" = all ] && caches_unlisted "$tab"
  return 0
}
caches_unlisted() {  # caches_unlisted TABLE: this checkout's .tmp entries no row of the table names (listed, never removed)
  local tab=$1 base=${GATE_CACHES_ROOT:-$root}/.tmp e un=() k
  [ -d "$base" ] || return 0
  caches_rows "$tab" | awk -F'\t' '$2 == "checkout" || $2 == "main" {p = $1; if (p ~ /^\.tmp\//) { sub(/^\.tmp\//, "", p); sub(/\/.*/, "", p); print p }}' | sort -u > "$TMPDIR/caches-names.$$"
  for e in "$base"/* "$base"/.[!.]*; do
    [ -e "$e" ] || continue
    k=${e##*/}
    awk -v k="$k" 'BEGIN { st = 1 } { g = $0; gsub(/\./, "\\.", g); gsub(/\*/, ".*", g); if (k ~ ("^" g "$")) st = 0 } END { exit st }' "$TMPDIR/caches-names.$$" || un+=("$k")
  done
  rm -f "$TMPDIR/caches-names.$$"
  [ ${#un[@]} -eq 0 ] && return 0
  echo "gate --prune: ${#un[@]} entries of $base the table does not name (scratch: listed, the main session removes them): $(printf '%s ' "${un[@]:0:30}")$( [ ${#un[@]} -gt 30 ] && echo "...")"
}
# worktrees_list (GDS18 (a)): the main checkout's worktrees whose branch is merged into GATE_PRUNE_MERGED (default mac/next,
# else next) and idle for a day (their index and HEAD untouched): listed with their size class, never removed
worktrees_list() {
  local m into w b idle now t n=0
  m=${GATE_CACHES_MAIN:-$(arbace_main_checkout "$root")}
  into=${GATE_PRUNE_MERGED:-mac/next}
  git -C "$m" rev-parse -q --verify "$into" > "$TMPDIR/wt.rev" 2>&1 || into=next
  now=$(date +%s)
  git -C "$m" for-each-ref --merged "$into" --format='%(refname:short)' refs/heads > "$TMPDIR/wt.merged" 2>"$TMPDIR/wt.err" || return 0
  for w in "$m"/.tmp/worktrees/*/; do
    w=${w%/}; [ -e "$w/.git" ] || continue
    b=$(git -C "$w" symbolic-ref -q --short HEAD 2>"$TMPDIR/wt.err") || continue
    grep -qxF "$b" "$TMPDIR/wt.merged" || continue
    t=$(stat -c %Y "$(git -C "$w" rev-parse --git-path index)" 2>"$TMPDIR/wt.err" || echo "$now")
    idle=$(( (now - t) / 3600 ))
    [ "$idle" -ge 24 ] || continue
    n=$((n + 1))
    echo "gate --prune --worktrees: $w ($b, merged into $into, idle ${idle} h): the main session removes it once its report is read"
  done
  echo "gate --prune --worktrees: $n worktrees merged into $into and idle for a day"
}
# gate_cache_cap: the table's cheap rules before a step, at most every GATE_CACHE_CAP_EVERY seconds (300) and by one step at a
# time (flock -n: a concurrent step skips it): Go's build cache held to its size between the night's builds, not a 90-minute wipe
gate_cache_cap() {
  local st=$logs/cache-cap.at now
  now=$(date +%s)
  [ $(( now - $(cat "$st" 2>"$logs/count.err" || echo 0) )) -ge "${GATE_CACHE_CAP_EVERY:-300}" ] || return 0
  { flock -n 9 || return 0; echo "$now" > "$st"; caches_prune "" cheap >> "$logs/cache-cap.log" 2>&1; } 9>>"$logs/cache-cap.lock"
}

# rotation_prune NOW DAYS [DRY] (review 181 L7): each rotated check's runs, STORE/_rotation/NAME.runs, trimmed to the last
# ARBACE_GATE_KEEP_RUNS (50) lines and every line younger than DAYS (its EPOCH), under the store's lock (the runner
# appends to them under it). The period the record is judged over (7 days) is shorter than DAYS, and a slice whose
# every run is older reads as never run, which the night's choice ranks with the oldest: no decision moves. The failed
# files' events (NAME.PART.failed) are kept: a failure stands until a later event clears it
rotation_prune() {
  local now=$1 days=$2 dry=$3 rd=$store/_rotation keep=${ARBACE_GATE_KEEP_RUNS:-50} f nl kept tot=0 cut=0
  [ -d "$rd" ] || return 0
  for f in "$rd"/*.runs; do
    [ -f "$f" ] || continue
    nl=$(wc -l < "$f")
    kept=$(awk -F'\t' -v n="$nl" -v k="$keep" -v since=$(( now - days * 86400 )) 'NR > n - k || $1 >= since' "$f" | wc -l)
    tot=$((tot + nl)); [ "$kept" -lt "$nl" ] || continue
    cut=$((cut + nl - kept))
    echo "gate --prune: ${dry:+would trim }_rotation/${f##*/} ($nl runs, $kept kept)"
    [ -n "$dry" ] && continue
    { flock 9 && nl=$(wc -l < "$f") \
        && awk -F'\t' -v n="$nl" -v k="$keep" -v since=$(( now - days * 86400 )) 'NR > n - k || $1 >= since' "$f" > "$f.tmp.$$" \
        && mv -f "$f.tmp.$$" "$f"; } 9>>"$rd/.lock"
  done
  echo "gate --prune: $tot rotation runs, $cut ${dry:+would be }trimmed in $rd"
}

# the audit's reducer (GT5, GTS30 (a)): tools/gateaudit, the Go port of test/gate/audit.awk (23 times
# faster, the same lines), built by TamaGo's go into DIR when its sources changed; GATE_AUDIT_AWK=1:
# busybox awk. audit_reducer DIR sets red; audit_reduce FIFO prints the OP<TAB>PATH lines
red=()
# rt_sigprocmask (GTS180, review 075 finding 1): strace resumes a new thread or child with PTRACE_SYSCALL, so it
# stops on every syscall until its first traced one; Go's runtime threads (sysmon, idle Ms) never open or clone,
# and paid that in system time against ulimit -t. Every new thread makes an rt_sigprocmask at once (Go's minit,
# musl's thread start and fork): traced, they leave the fully traced state. gateaudit ignores the lines (no
# case for the call).
audit_trace=open,openat,openat2,execve,execveat,readlink,readlinkat,chdir,fchdir,clone,clone3,fork,vfork,rt_sigprocmask
# audit_trace_set: the trace set strace gets: GATE_AUDIT_TRACE (e.g. %file) when set, else audit_trace; the reducer's
# bookkeeping calls and rt_sigprocmask are appended to an override that lacks them (review 080, point 4: without
# rt_sigprocmask new threads are slow again; review 081, N5: %file holds no clone, so no child would take its
# parent's directory); a negated override (!write) is left as it is: strace negates the whole list, so it already
# traces them, and an appended name would be excluded
audit_trace_set() {
  local t=${GATE_AUDIT_TRACE:-$audit_trace} c
  case $t in '!'*) echo "$t"; return ;; esac
  for c in clone clone3 fork vfork chdir fchdir execve rt_sigprocmask; do
    case ",$t," in *,"$c",*) ;; *) t=$t,$c ;; esac
  done
  echo "$t"
}
audit_reducer() {
  local d=$1 tg=${ARBACE_GOROOT:-/root/tamago-go} rk
  red=(); mkdir -p "$d"
  [ -z "${GATE_AUDIT_AWK:-}" ] || return 0
  rk=$(cat "$root"/tools/gateaudit/*.go "$root/tools/gateaudit/go.mod" "$tg/VERSION" | sha256sum | cut -c1-64)
  if [ "$(cat "$d/gateaudit.key" 2>"$d/gateaudit.err")" != "$rk" ] || [ ! -x "$d/gateaudit" ]; then
    (cd "$root/tools/gateaudit" && env -u GOOS -u GOARCH GOROOT="$tg" GOFLAGS= GOTOOLCHAIN=local CGO_ENABLED=0 \
       "$tg/bin/go" build -trimpath -buildvcs=false -o "$d/gateaudit.new" .) > "$d/gateaudit.err" 2>&1 \
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
      "${hv[@]}" strace -f $([ -n "${GATE_AUDIT_SIGNALS:-}" ] && echo -q || echo -qq) -y -s 4096 --seccomp-bpf -e "trace=$(audit_trace_set)" -o "$fifo" \
        env $([ "${heavy[$n]}" = yes ] && echo ARBACE_HEAVY_HELD=1) ARBACE_LOADED_LOG="$d/$n.loaded" GATE_AUDITED=1 GATE_OUT="$logs/out/$n" GATE_OUTS="$logs/out" GATE_LIST="$logs/rotation/$n.list" GATE_USES="${uses[$n]}" \
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
