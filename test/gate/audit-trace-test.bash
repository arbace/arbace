#!/bin/bash
# The audit's trace set (bin/lib/gate-run.bash's audit_trace, review 075 finding 1) and its reducer
# (tools/gateaudit): (1) the set holds rt_sigprocmask, so a new thread leaves the fully traced state at
# once; (2) gateaudit reduces a trace holding rt_sigprocmask lines to the same OP<TAB>PATH lines as one
# without. Run: test/gate/audit-trace-test.bash (builds gateaudit with TamaGo's go, CGO_ENABLED=0, into
# .tmp/; needs strace and gcc for (1)'s timing, which is skipped without them). Exits 1 on a failure.
root=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$root/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/audit-trace-test.XXXXXX"); trap 'rm -rf "$w"' EXIT
fail=0; ck() { if [ "$2" = ok ]; then echo "ok   $1"; else echo "FAIL $1: $2"; fail=1; fi; }

# (1) the trace set
src=$(sed -n 's/^audit_trace=//p' "$root/bin/lib/gate-run.bash")
case ",$src," in *,rt_sigprocmask,*) ck "audit_trace holds rt_sigprocmask" ok ;; *) ck "audit_trace holds rt_sigprocmask" "audit_trace=$src" ;; esac

# (2) the reducer on a trace with the new lines
tg=${ARBACE_GOROOT:-/root/tamago-go}
(cd "$root/tools/gateaudit" && env -u GOOS -u GOARCH GOROOT="$tg" GOFLAGS= GOTOOLCHAIN=local CGO_ENABLED=0 \
   "$tg/bin/go" build -trimpath -o "$w/gateaudit" .) > "$w/build.log" 2>&1 || { cat "$w/build.log"; ck "gateaudit builds" "build failed"; exit 1; }
cat > "$w/plain.trace" <<'T'
100 execve("/bin/sh", ["sh", "-c", "x"], 0x7ffd /* 3 vars */) = 0
100 openat(AT_FDCWD</etc>, "/etc/ld.so.cache", O_RDONLY|O_CLOEXEC) = 3</etc/ld.so.cache>
100 clone(child_stack=NULL, flags=CLONE_VM|SIGCHLD) = 101
101 openat(AT_FDCWD, "rel.txt", O_WRONLY|O_CREAT, 0644) = 4</work/rel.txt>
T
awk '{print} NR == 1 || NR == 3 { print "100 rt_sigprocmask(SIG_SETMASK, [], NULL, 8) = 0";
  print "102 rt_sigprocmask(SIG_BLOCK, ~[RTMIN RT_1], [], 8) = 0"; print "102 rt_sigprocmask(SIG_SETMASK, [], NULL, 8) = 0" }' "$w/plain.trace" > "$w/sig.trace"
grep -c rt_sigprocmask "$w/sig.trace" > "$w/n"
"$w/gateaudit" -root /work "$w/plain.trace" > "$w/plain.out" 2> "$w/plain.err"
"$w/gateaudit" -root /work "$w/sig.trace" > "$w/sig.out" 2> "$w/sig.err"
[ "$(cat "$w/n")" -ge 6 ] && [ -s "$w/plain.out" ] || ck "reducer fixture" "no paths in the plain trace"
if cmp -s "$w/plain.out" "$w/sig.out" && [ ! -s "$w/sig.err" ]; then ck "gateaudit ignores rt_sigprocmask lines" ok
else ck "gateaudit ignores rt_sigprocmask lines" "$(diff "$w/plain.out" "$w/sig.out"; cat "$w/sig.err")"; fi
grep -q 'W	/work/rel.txt' "$w/sig.out" && ck "paths still reduced (W /work/rel.txt)" ok || ck "paths still reduced" "$(cat "$w/sig.out")"

# (3) the system time of a thread that never opens or clones, under the audit's strace line
if command -v strace > "$w/which" 2>&1 && command -v gcc > "$w/which" 2>&1; then
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
