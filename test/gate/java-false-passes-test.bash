#!/bin/bash
# test/gate/java-false-passes-test.bash [ROOT] (queue 485, high: a record never holds a pass that did not run). Light: no
# Arbace executable, no Java, no build. Copies of the scripts' functions (extracted by sed, not retyped) over fabricated
# plans, shards, stores and caches under TMPDIR; each case beside its control.
#   1. bin/java-check's plans (477-3): a test whose WORK/run/ID/plan.tsv is missing, has no end line, has no run line,
#      or is cut while the test runs is an error at every level it covers, never a pass (run_actions, check_one); the
#      control: a whole plan passes. And java.check writes each plan through write-plan, an end line last.
#   2. a compiled shard that failed (night 11-20's langtools-run part: 514 rows "error the shard failed" saved as a pass):
#      its rows are the run's own errors (run_errors), which fail the run (run_errors_ok, check's status with --save and
#      without); the control: a shard that ran fails nothing, and JAVA_CHECK_ERRORS_MAX raises the bound. --judge
#      --latest-saved passes over saved results that hold them, and saved-errors lists them, removing nothing.
#   3. the print cache (.tmp/repr-go-print-cache): the gate's prune (caches_prune, test/gate/caches.edn's :entries and
#      :lock) removes entries whole and leaves the cache alone while a build holds its lock; build_go_print_cache_trim
#      likewise; bin/build-go holds the lock shared while it builds. The controls: the files one by one (the old rule)
#      cut entries in half, and without the lock held the prunes remove.
# PASS/FAIL, exit 0/1. Run by bin/gate-tests.
set -u
R=${1:-$(cd "$(dirname "$0")/../.." && pwd)}
TMPDIR=${TMPDIR:-$R/.tmp/tmp}; mkdir -p "$TMPDIR"
T=$TMPDIR/java-false-passes.$$
mkdir -p "$T"; trap 'rm -rf "$T"' EXIT
fails=0 n=0
ok() { n=$((n + 1)); echo "ok   $1"; }
bad() { n=$((n + 1)); echo "FAIL $1"; fails=$((fails + 1)); }
check() { if [ "$2" = "$3" ]; then ok "$1: $2"; else bad "$1: got '$2', want '$3'"; fi; }

ext() { # FILE NAMES...: the functions as FILE defines them
  local f=$1 g; shift
  for g in "$@"; do sed -n "/^$g() {.*}\$/p; /^$g() {\$/,/^}\$/p" "$f"; done
}
{ ext "$R/bin/lib/limit.bash" attempt_limits timeout_kind note_attempt program_tmo
  ext "$R/bin/java-check" first_line root_cause mask_hashes run_dirs mask_dirs judge test_limit jtreg_props plan_bad run_actions \
    check_one run_shard run_errors run_errors_ok gate_store saved_bad plan_missing_ids saved_errors judge_saved; } > "$T/fns.bash"
. "$T/fns.bash"
cover_settle() { :; }
# limited's stub: status STUB_ST (default 0); with CUT set, the first call truncates the plan in place (another run's
# removal while this one reads it)
limited() { if [ -n "${CUT:-}" ] && [ ! -e "$T/cut.done" ]; then : > "$CUT"; : > "$T/cut.done"; fi; return "${STUB_ST:-0}"; }

# ---- 1. the plans
root=$T/root work=$T/work jdktest=$T/jdk timeout=20 levels=read,interp
mkdir -p "$root/test/java" "$work"; : > "$work/empty"; : > "$root/test/java/timeouts.txt"
runl() { printf 'run\t%s\t0\t-\t0\t0\t-\t0\tMain\n' "$1"; }
mkplan() { # ID [nore] [noend]: a plan as java.check writes it (an end line last), or without its run lines or end line
  mkdir -p "$work/run/$1"
  { printf 'test\tjdk/java/lang/X%s.java\ndir\tx\nroot\tjdk26u\nclasses\t0\nverdict\tstdout\n' "$1"
    [ "${2:-}" = norun ] || { runl 0; runl 1; }
    [ "${3:-}" = noend ] || echo end; } > "$work/run/$1/plan.tsv"
}
run() { # ID: run_actions' result line
  local id=$1 p=$work/run/$1 dir=$T
  run_actions interp "$T/exe" files "$T/forms" 2>"$T/run.err"
}
res() { cut -f3 <<< "$1"; }
mkplan t1; r=$(run t1); check "control: a whole plan, its actions run and passing" "$(res "$r")" pass
mkplan t2; rm "$work/run/t2/plan.tsv"; r=$(run t2)
check "no plan.tsv (477-3: 104 rows passed unrun): an error" "$(res "$r")" error
case $r in *"no plan:"*) ok "  its reason names the missing plan" ;; *) bad "  its reason: $r" ;; esac
mkplan t3 "" noend; r=$(run t3); check "a plan without its end line (cut): an error" "$(res "$r")" error
mkplan t4 norun; r=$(run t4); check "a plan without a run line: an error" "$(res "$r")" error
mkplan t5; rm -f "$T/cut.done"; r=$(CUT=$work/run/t5/plan.tsv run t5)
check "a plan cut while the test runs (read to no end line): an error, and no oracle row passing it" "$(cut -f2,3 <<< "$r" | paste -sd' ')" "interp	error"
case $r in *"cut while the test ran"*) ok "  its reason: cut while it ran" ;; *) bad "  its reason: $r" ;; esac
r=$(STUB_ST=1 run t1); check "control: a whole plan whose run fails is no error, its own result" "$(res "$r")" fail
mkplan t6; rm "$work/run/t6/plan.tsv"
r=$(check_one t6 2>"$T/one.err" | cut -f2,3 | paste -sd' ')
check "check_one, no plan, at read,interp: an error at each level" "$r" "read	error interp	error"
r=$(levels=classes check_one t6 2>"$T/one.err" | cut -f2,3 | paste -sd' '); check "  at classes" "$r" "classes	error"
check "java.check writes both kinds of plan through write-plan" "$(grep -c '(write-plan (str work "/run/"' "$R/test/java/check.ae")" 2
check "write-plan writes an end line last, renamed into place" \
  "$(awk '/^\(defn- write-plan/ { f = 1 } f && /concat ls \["end"\]/ { a = 1 } f && /renameTo/ { b = 1 } f && /^$/ { exit } END { print a + b }' "$R/test/java/check.ae")" 2
check "plan_bad exported where run_actions is (the xargs subshells)" "$(grep -c '^  export -f .*run_actions.* plan_bad\|^  export -f .*run_actions plan_bad' "$R/bin/java-check")" 2

# ---- 2. a failed shard
d=$T/shard; mkdir -p "$d"; printf 't%05d\n' $(seq 1 514) > "$d/tests.txt"
failing() { return 1; }
passing() { awk -F'\t' '{ print $1 "\tcompiled\tpass\t" }' "$2/tests.txt" > "$2/results.tsv"; }
shard_fn=failing run_shard 0 "$d" 2>"$T/shard.err"
check "a failed shard's rows" "$(cut -f3 "$d/results.tsv" | sort | uniq -c | awk '{ print $1, $2 }')" "514 error"
check "run_errors counts them" "$(run_errors "$d/results.tsv")" 514
run_errors_ok "$d/results.tsv" 2>"$T/ok.err"; check "run_errors_ok fails the run (night 11-20: saved as a pass)" "$?" 1
JAVA_CHECK_ERRORS_MAX=514 run_errors_ok "$d/results.tsv" 2>"$T/ok.err"; check "  JAVA_CHECK_ERRORS_MAX=514 lets it through" "$?" 0
shard_fn=passing run_shard 0 "$d" 2>"$T/shard.err"
check "control: a shard that ran, its rows" "$(cut -f3 "$d/results.tsv" | sort -u)" pass
run_errors_ok "$d/results.tsv" 2>"$T/ok.err"; check "control: run_errors_ok passes it" "$?" 0
printf 't1\tinterp\terror\tthe plan ./x/plan.tsv is partial (no end line)\nt2\tinterp\terror\tno plan: x is missing\nt3\tinterp\terror\tsome test error\n' > "$T/mixed.tsv"
check "run_errors: the plans' errors counted, a test's own error not" "$(run_errors "$T/mixed.tsv")" 2
check "check: the run's errors computed before --save and the report" "$(grep -c 'run_errors_ok "$work/results.tsv" || errs=1' "$R/bin/java-check")" 1
check "check: its status the run's errors with --save and after the report" "$(grep -c '^  *return "\$errs"$' "$R/bin/java-check")" 2
# --latest-saved and saved-errors over a fabricated store
S=$T/store; mkdir -p "$S/java-compiled-full.langtools-run" "$S/java-interp.jdk-lang"
mk_out() { # DIR KEY AGE-MIN ROWS-FILE [LOG]
  mkdir -p "$1/$2.out"; cp "$4" "$1/$2.out/results.tsv"; echo compiled > "$1/$2.out/levels"
  printf '{:step "x" :key "%s" :date "2026-11-20T13:23:00Z"}\n' "$2" > "$1/$2.edn"; printf '%s\n' "${5:-ran}" > "$1/$2.log"
  touch -d "@$(( $(date +%s) - $3 * 60 ))" "$1/$2.out"
}
printf 't1\tcompiled\tpass\t\n' > "$T/good.tsv"
mk_out "$S/java-compiled-full.langtools-run" kold 60 "$T/good.tsv"
mk_out "$S/java-compiled-full.langtools-run" knew 5 "$d/results.tsv"
shard_fn=failing run_shard 0 "$d" 2>"$T/shard.err"; cp "$d/results.tsv" "$S/java-compiled-full.langtools-run/knew.out/results.tsv"
mk_out "$S/java-interp.jdk-lang" kplan 5 "$T/good.tsv" "awk: fatal: cannot open file \`/x/.tmp/java-check/run/t03296/plan.tsv' for reading: No such file or directory
environment: /x/.tmp/java-check/run/t03287/plan.tsv: No such file or directory"
mk_out "$S/java-interp.jdk-lang" kok 60 "$T/good.tsv"
(cd "$S" && find . | LC_ALL=C sort; find . -type f -exec sha256sum {} + | LC_ALL=C sort) > "$T/store.before"
bash "$R/bin/java-check" saved-errors "$S" > "$T/se.out" 2>"$T/se.err"  # the command itself (set -euo pipefail)
check "saved-errors: its status and summary" "$?: $(tail -1 "$T/se.err")" "0: java-check saved-errors: 2 of 4 saved results in $S hold rows no test earned (nothing removed)"
check "saved-errors: the shard's failed pass listed, newest, 514 rows" "$(awk -F'\t' '$1 ~ /langtools-run\/knew/ { print $2, $4, $5, $7 }' "$T/se.out")" "pass 514 0 newest"
check "saved-errors: the pass whose log shows 2 plans missing" "$(awk -F'\t' '$1 ~ /jdk-lang\/kplan/ { print $4, $5 }' "$T/se.out")" "0 2"
check "control: saved-errors leaves the clean ones out" "$(grep -c 'kold\|kok' "$T/se.out")" 0
(cd "$S" && find . | LC_ALL=C sort; find . -type f -exec sha256sum {} + | LC_ALL=C sort) > "$T/store.after"
cmp -s "$T/store.before" "$T/store.after" && ok "saved-errors removes nothing" || bad "saved-errors changed the store"
jc() { shift; echo "judged: $*" >> "$T/judged"; }  # java.check's report, stubbed: the files judged
ref=$T/ref; : > "$T/judged"
ARBACE_GATE_STORE=$S judge_saved --latest-saved > "$T/js.out" 2>"$T/js.err"
grep -q 'langtools-run/kold.out/results.tsv' "$T/work/judge/results.tsv" "$T/js.out" 2>/dev/null; :
check "--latest-saved takes the older clean results of langtools-run" "$(grep -o 'langtools-run/k[a-z]*\.out' "$T/js.out" | sort -u)" "langtools-run/kold.out"
check "--latest-saved passes over the plans' missing pass" "$(grep -o 'jdk-lang/k[a-z]*\.out' "$T/js.out" | sort -u)" "jdk-lang/kok.out"
check "  and names what it passed over" "$(grep -c 'passed over' "$T/js.err")" 2

# ---- 3. the print cache
. "$R/bin/lib/gocache.bash"
root=$T/root2 logs=$T/logs; mkdir -p "$root" "$logs"
. "$R/bin/lib/gate-cache.bash"; . "$R/bin/lib/gate-run.bash"
now=$(date +%s)
mkcache() { # DIR: 6 entries XX/KEY of 3 files of 256 KiB (names, f-a.go, f-b.go), entry i used i hours ago (the entry's mtime,
  # as arbace.build.go's cached-texts sets it); the files' own mtimes the time they were written (all 9 hours ago, but
  # f-b.go of entries 1 to 3 rewritten later: a file's mtime is no use)
  local c=$1 i e; rm -rf "$c"; mkdir -p "$c"
  for i in 1 2 3 4 5 6; do
    e=$c/0$i/k$i; mkdir -p "$e"; printf 'a.go\nb.go\n' > "$e/names"
    head -c 262144 /dev/zero > "$e/f-a.go"; head -c 262144 /dev/zero > "$e/f-b.go"; truncate -s 262144 "$e/names"
    touch -d "@$(( now - 9 * 3600 ))" "$e/names" "$e/f-a.go"; touch -d "@$(( now - (i <= 3 ? 10 : 8) * 3600 ))" "$e/f-b.go"
    touch -d "@$(( now - i * 3600 ))" "$e"
  done
}
whole() { # DIR: the entries left, each whole (W) or cut (C)
  local e out=""; for e in "$1"/0*/k*; do
    if [ -f "$e/names" ] && [ -f "$e/f-a.go" ] && [ -f "$e/f-b.go" ]; then out="$out ${e##*/}:W"; else out="$out ${e##*/}:C"; fi
  done; echo "${out# }"
}
C=$root/.tmp/repr-go-print-cache
# a cap of 0.003 GiB (3.07 MiB) holds 4 entries of 0.75 MiB (and their directories' blocks): the 2 least recently used out
printf '[\n {:path ".tmp/repr-go-print-cache" :owner "b" :rule [:size 0.003 :lru] :entries 2 :lock ".lock" :cheap true}\n]\n' > "$T/new.edn"
printf '[\n {:path ".tmp/repr-go-print-cache" :owner "b" :rule [:size 0.003 :lru] :cheap true}\n]\n' > "$T/old.edn"
check "the table's print-cache row takes :entries 2 and :lock" \
  "$(caches_rows "$R/test/gate/caches.edn" | awk -F'\t' '$1 == ".tmp/repr-go-print-cache" { print $3, $12, $13 }')" "size 2 .lock"
mkcache "$C"; GATE_CACHES=$T/old.edn caches_prune "" cheap > "$T/p.out" 2>&1
case $(whole "$C") in *:C*) ok "control: the files one by one (the old rule) cut entries: $(whole "$C")" ;; *) bad "control: the old rule cut none: $(whole "$C")" ;; esac
mkcache "$C"; GATE_CACHES=$T/new.edn caches_prune "" cheap > "$T/p.out" 2>&1
check ":entries 2: the least recently used entries out whole, the rest whole" "$(whole "$C")" "k1:W k2:W k3:W k4:W"
mkcache "$C"; exec 7>> "$C/.lock"; flock -s 7
GATE_CACHES=$T/new.edn caches_prune "" cheap > "$T/p.out" 2>&1
check ":lock held shared (a build): nothing removed" "$(whole "$C")" "k1:W k2:W k3:W k4:W k5:W k6:W"
grep -q 'left alone: a tool holds its lock' "$T/p.out" && ok "  the prune says so" || bad "  the prune said: $(cat "$T/p.out")"
ARBACE_GOCACHE_KEEP_MIN=150 build_go_print_cache_trim "$C" 2>"$T/t.err"
check "build_go_print_cache_trim, the lock held: nothing removed" "$(whole "$C")" "k1:W k2:W k3:W k4:W k5:W k6:W"
exec 7>&-
ARBACE_GOCACHE_KEEP_MIN=150 build_go_print_cache_trim "$C" 2>"$T/t.err"
check "control: build_go_print_cache_trim, no lock held: the entries unused for 150 minutes out" "$(whole "$C")" "k1:W k2:W"
# bin/build-go holds the lock shared while it builds: a fake checkout whose repr_go_exec tries the lock exclusive
F=$T/fake; mkdir -p "$F/bin/lib"; cp "$R/bin/build-go" "$F/bin/build-go"
: > "$F/bin/lib/tmpdir.bash"; : > "$F/bin/lib/renamed-env.bash"
cat > "$F/bin/lib/repr-go-host.bash" <<'EOF'
repr_go_exec() { if flock -n -x "$PC/.lock" true; then echo free; else echo held; fi; exit 0; }
EOF
check "bin/build-go --print-cache DIR: DIR/.lock held while it builds" "$(PC=$T/pc bash "$F/bin/build-go" --print-cache "$T/pc" x 2>&1)" held
check "control: bin/build-go without --print-cache holds no lock" "$(mkdir -p "$T/pc2"; PC=$T/pc2 bash "$F/bin/build-go" x 2>&1)" free

echo "$((n - fails)) of $n"
if [ "$fails" = 0 ]; then echo PASS; exit 0; else echo FAIL; exit 1; fi
