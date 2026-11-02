#!/bin/bash
# The audit's trace set (bin/lib/gate-run.bash's audit_trace, review 075 finding 1) and its reducer
# (tools/gateaudit): (1) the set holds rt_sigprocmask, so a new thread leaves the fully traced state at
# once; (2) gateaudit reduces a trace holding rt_sigprocmask lines to the same OP<TAB>PATH lines as one
# without. Run: test/gate/audit-trace-test.bash (builds gateaudit with TamaGo's go, CGO_ENABLED=0, into
# .tmp/, skipped without it, a failure when it does not build; needs strace and gcc for (1)'s timing, which is
# skipped without them). Exits 1 on a failure.
root=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$root/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/audit-trace-test.XXXXXX"); trap 'rm -rf "$w"' EXIT
fail=0; ck() { if [ "$2" = ok ]; then echo "ok   $1"; else echo "FAIL $1: $2"; fail=1; fi; }

# (1) the trace set
src=$(sed -n 's/^audit_trace=//p' "$root/bin/lib/gate-run.bash")
case ",$src," in *,rt_sigprocmask,*) ck "audit_trace holds rt_sigprocmask" ok ;; *) ck "audit_trace holds rt_sigprocmask" "audit_trace=$src" ;; esac

# (1b) an override of the set (GATE_AUDIT_TRACE=%file) gets the reducer's bookkeeping calls and rt_sigprocmask
# appended when it lacks them, once each; a negated override is left alone (review 081, N5)
eval "$(sed -n '/^audit_trace_set() {/,/^}/p' "$root/bin/lib/gate-run.bash")"
audit_trace=$src
o=$(GATE_AUDIT_TRACE=%file,execve audit_trace_set); d=$(audit_trace_set)
k=$(GATE_AUDIT_TRACE=%file,clone,clone3,fork,vfork,chdir,fchdir,execve,rt_sigprocmask audit_trace_set)
x=$(GATE_AUDIT_TRACE='!write' audit_trace_set)
{ [ "$o" = "%file,execve,clone,clone3,fork,vfork,chdir,fchdir,rt_sigprocmask" ] && [ "$d" = "$src" ] \
  && [ "$k" = "%file,clone,clone3,fork,vfork,chdir,fchdir,execve,rt_sigprocmask" ] && [ "$x" = '!write' ]; } \
  && ck "an override gets the bookkeeping calls and rt_sigprocmask appended once, a negated one is left" ok \
  || ck "override" "'$o' '$d' '$k' '$x'"

# (2) the reducers on traces with the new lines: tools/gateaudit (built with TamaGo's go; skipped with a message
# when that is missing) and test/gate/audit.awk (GATE_AUDIT_AWK=1's reducer, run by awk -v root=...)
tg=${ARBACE_GOROOT:-/root/tamago-go}
reducers=(awk)
reduce() {  # reduce NAME TRACE: the OP<TAB>PATH lines of TRACE by reducer NAME
  case $1 in
    awk) awk -v root=/work -f "$root/test/gate/audit.awk" "$2" ;;
    gateaudit) "$w/gateaudit" -root /work "$2" ;;
  esac
}
# skipped only without TamaGo's go; a build that fails where go exists is a failure (review 081, N4), and (3) still runs
# (-buildvcs=false: go build's VCS stamp runs git status, which reads every file of the checkout, outside the step's inputs)
if [ ! -x "$tg/bin/go" ]; then echo "skip gateaudit: no $tg/bin/go here; audit.awk is tested"
elif (cd "$root/tools/gateaudit" && env -u GOOS -u GOARCH GOROOT="$tg" GOFLAGS= GOTOOLCHAIN=local CGO_ENABLED=0 \
     "$tg/bin/go" build -trimpath -buildvcs=false -o "$w/gateaudit" .) > "$w/build.log" 2>&1; then reducers+=(gateaudit)
else ck "gateaudit builds" "$(tail -1 "$w/build.log")"; fi
cat > "$w/plain.trace" <<'T'
100 execve("/bin/sh", ["sh", "-c", "x"], 0x7ffd /* 3 vars */) = 0
100 openat(AT_FDCWD</etc>, "/etc/ld.so.cache", O_RDONLY|O_CLOEXEC) = 3</etc/ld.so.cache>
100 clone(child_stack=NULL, flags=CLONE_VM|SIGCHLD) = 101
101 openat(AT_FDCWD, "rel.txt", O_WRONLY|O_CREAT, 0644) = 4</work/rel.txt>
T
awk '{print} NR == 1 || NR == 3 { print "100 rt_sigprocmask(SIG_SETMASK, [], NULL, 8) = 0";
  print "102 rt_sigprocmask(SIG_BLOCK, ~[RTMIN RT_1], [], 8) = 0"; print "102 rt_sigprocmask(SIG_SETMASK, [], NULL, 8) = 0" }' "$w/plain.trace" > "$w/sig.trace"
# a new thread's first line (its rt_sigprocmask) printed before its parent's clone result (review 080, finding 1):
# its later relative execve must resolve against the parent's directory, and the parent's executable applies
cat > "$w/early.trace" <<'T'
100 chdir("/work/sub") = 0
100 clone(child_stack=0x7f00, flags=CLONE_VM|CLONE_THREAD|CLONE_SIGHAND <unfinished ...>
101 rt_sigprocmask(SIG_SETMASK, [] <unfinished ...>
100 <... clone resumed>, parent_tid=[101]) = 101
101 <... rt_sigprocmask resumed>NULL, 8) = 0
101 execve("./nx", ["nx"], 0x7ffd /* 3 vars */) = -1 ENOENT (No such file or directory)
T
# a reused PID (strace -qq hides exits, PIDs wrap): pid 300 chdir'd in an earlier life; the new process with that
# PID, cloned by a parent in /work/sub, must take the parent's directory (review 081, N1)
cat > "$w/reuse.trace" <<'T'
300 chdir("/work/old") = 0
100 chdir("/work/sub") = 0
100 clone(child_stack=NULL, flags=CLONE_VM|SIGCHLD) = 300
300 execve("./nx", ["nx"], 0x7ffd /* 3 vars */) = -1 ENOENT (No such file or directory)
T
# the exe half of the birth (review 081, N1): pid 300 ran git in an earlier life; the new process with that PID,
# cloned by a non-git parent, must not keep git's executable, which hides its reads
cat > "$w/reusex.trace" <<'T'
300 execve("/usr/bin/git", ["git", "status"], 0x7ffd /* 3 vars */) = 0
100 clone(child_stack=NULL, flags=CLONE_VM|SIGCHLD) = 300
300 openat(AT_FDCWD</work>, "f.txt", O_RDONLY) = 3</work/f.txt>
T
# a child's early line that is a path call, neither chdir nor execve (review 081, N3): it is reduced, and the
# child still takes its parent's directory at the clone result
cat > "$w/earlyopen.trace" <<'T'
100 chdir("/work/sub") = 0
100 clone(child_stack=0x7f00, flags=CLONE_VM|CLONE_THREAD|CLONE_SIGHAND <unfinished ...>
101 openat(AT_FDCWD</work/sub>, "x", O_RDONLY) = 3</work/sub/x>
100 <... clone resumed>) = 101
101 execve("./nx", ["nx"], 0x7ffd /* 3 vars */) = -1 ENOENT (No such file or directory)
T
printf 'R\t/work/sub/x\nX\t/work/sub/nx\n' > "$w/earlyopen.want"
# fchdir by descriptor (the JVM's perf data: fchdir into /tmp/hsperfdata_root, then a stale file opened by its bare PID
# name): the name resolves against the directory -y names, not the checkout's root (a failed fchdir changes nothing)
cat > "$w/fchdir.trace" <<'T'
100 fchdir(5</tmp/hsperfdata_root>) = 0
100 openat(AT_FDCWD</tmp/hsperfdata_root>, "42568", O_RDONLY) = 6</tmp/hsperfdata_root/42568>
100 fchdir(6</nowhere>) = -1 ENOTDIR (Not a directory)
100 open("42569", O_RDONLY) = 7</tmp/hsperfdata_root/42569>
T
printf 'R\t/tmp/hsperfdata_root/42568\nR\t/tmp/hsperfdata_root/42569\n' > "$w/fchdir.want"
for r in "${reducers[@]}"; do
  reduce "$r" "$w/reusex.trace" > "$w/reusex.out" 2> "$w/reusex.err"
  if [ "$(cat "$w/reusex.out")" = "R	/work/f.txt" ]; then ck "$r: a reused PID's new process does not keep the dead one's executable" ok
  else ck "$r: reused PID's executable" "got '$(cat "$w/reusex.out")', expected 'R /work/f.txt'"; fi
  reduce "$r" "$w/earlyopen.trace" > "$w/earlyopen.out" 2> "$w/earlyopen.err"
  if cmp -s "$w/earlyopen.out" "$w/earlyopen.want"; then ck "$r: a child's early path line does not stop it taking its parent's directory" ok
  else ck "$r: early path line" "got '$(tr '\n\t' '; ' < "$w/earlyopen.out")'"; fi
  reduce "$r" "$w/fchdir.trace" > "$w/fchdir.out" 2> "$w/fchdir.err"
  if cmp -s "$w/fchdir.out" "$w/fchdir.want"; then ck "$r: fchdir sets the process's directory" ok
  else ck "$r: fchdir" "got '$(tr '\n\t' '; ' < "$w/fchdir.out")'"; fi
  reduce "$r" "$w/plain.trace" > "$w/plain.out" 2> "$w/plain.err"
  reduce "$r" "$w/sig.trace" > "$w/sig.out" 2> "$w/sig.err"
  [ -s "$w/plain.out" ] || ck "$r: fixture" "no paths in the plain trace"
  if cmp -s "$w/plain.out" "$w/sig.out" && [ ! -s "$w/sig.err" ]; then ck "$r ignores rt_sigprocmask lines" ok
  else ck "$r ignores rt_sigprocmask lines" "$(diff "$w/plain.out" "$w/sig.out"; cat "$w/sig.err")"; fi
  grep -q 'W	/work/rel.txt' "$w/sig.out" && ck "$r: paths still reduced (W /work/rel.txt)" ok || ck "$r: paths still reduced" "$(cat "$w/sig.out")"
  reduce "$r" "$w/reuse.trace" > "$w/reuse.out" 2> "$w/reuse.err"
  if [ "$(cat "$w/reuse.out")" = "X	/work/sub/nx" ]; then ck "$r: a reused PID's new process takes its parent's directory" ok
  else ck "$r: reused PID" "got '$(cat "$w/reuse.out")', expected 'X /work/sub/nx'"; fi
  reduce "$r" "$w/early.trace" > "$w/early.out" 2> "$w/early.err"
  if [ "$(cat "$w/early.out")" = "X	/work/sub/nx" ]; then ck "$r: a thread's first line before its clone result keeps the parent's directory" ok
  else ck "$r: early thread line" "got '$(cat "$w/early.out")', expected 'X /work/sub/nx'"; fi
done

# (3) the system time of a thread that never opens or clones, under the audit's strace line; skipped under a tracer
# (gate-reads-test's strace, bin/gate --audit's): a traced process cannot run strace
tracer=$(sed -n 's/^TracerPid:[[:space:]]*//p' /proc/self/status)
if [ "${tracer:-0}" != 0 ]; then echo "skip (3): already traced by pid $tracer"
elif command -v strace > "$w/which" 2>&1 && command -v gcc > "$w/which" 2>&1; then
  cat > "$w/thr.c" <<'C'
#include <pthread.h>
#include <sched.h>
static void *f(void *a) { for (int i = 0; i < 100000; i++) sched_yield(); return a; }
int main(void) { pthread_t t; pthread_create(&t, 0, f, 0); pthread_join(t, 0); return 0; }
C
  if gcc -O1 -o "$w/thr" "$w/thr.c" -lpthread 2> "$w/gcc.err"; then
    t() {  # t TRACE-SET: wall milliseconds of the probe under the audit's strace line
      local s e; s=$(date +%s%N)
      strace -f -qq -y -s 4096 --seccomp-bpf -e "trace=$1" -o "$w/out.trace" "$w/thr"; e=$(date +%s%N); echo $(((e - s) / 1000000))
    }
    base=${src%,rt_sigprocmask}
    a=$(t "$base"); b=$(t "$src")
    echo "     thread of 100k sched_yield: $a ms without rt_sigprocmask, $b ms with"
    [ "$b" -lt "$a" ] && [ "$b" -lt 1000 ] && ck "a new thread is not traced on every syscall" ok || ck "a new thread is not traced on every syscall" "$a ms vs $b ms"
  else echo "skip (3): gcc failed"; fi
else echo "skip (3): no strace or gcc"; fi
exit $fail
