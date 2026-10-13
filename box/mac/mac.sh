#!/bin/bash
# Arbace in the box on the Mac: Hypervisor.framework on Apple silicon (doc/go/BOX-NOTES.md, "S4",
# BX66), modelled on go-whim's guest/mac/mac.sh. Run from the bundle's directory (bin/box-mac-bundle
# builds it on Linux: the launcher arbace-box, darwin/arm64 without cgo; the arm64 guest
# arbace-box.elf beside it; hv.test), on the Mac. Nothing is installed; every program that creates
# a VM is signed ad hoc with the hypervisor entitlement. Each step's output is saved under
# mac-results/ and packed into mac-results.tar.gz, to send back.
#
#   ./mac.sh check       what the Mac is: macOS, the chip, kern.hv_support, the bundle's VERSION
#   ./mac.sh sign        arbace-box and hv.test signed ad hoc with arbace-box.entitlements
#   ./mac.sh hv          hv's arm64 tests on the framework (the smallest guests)
#   ./mac.sh checklist   the box's checklist on 4 vCPUs and on 1: -e, the wall clock, the REPL on
#                        standard input, a script and -m from memory, an error's status, a nil
#                        dereference, a future during a long evaluation and while the REPL waits,
#                        the store (the launcher's tool, a script by ref), the network on loopback
#                        (a connection within the box, a socket REPL from the host by nc), the idle
#                        CPU at the prompt (20 s), and times (the start, three evaluations)
#   ./mac.sh all         check, sign, hv, checklist, then mac-results.tar.gz
#   ./mac.sh run [ARG...]  the box: the REPL (under rlwrap when it is installed), or arbace's ARGs
set -u
cd "$(dirname "$0")"
here=$(pwd)
res=$here/mac-results
mkdir -p "$res"
export TMPDIR="$here/.tmp"
mkdir -p "$TMPDIR"
B=$here/arbace-box
fails=0 oks=0

# t SECONDS CMD...: CMD, ended after SECONDS (status 124), as Linux's timeout (macOS has none)
t() {
  local s=$1; shift
  perl -e 'my $s = shift; my $p = fork; if (!$p) { exec @ARGV; exit 127 } local $SIG{ALRM} = sub { kill 9, $p; exit 124 }; alarm $s; waitpid $p, 0; exit($? & 127 ? 128 + ($? & 127) : $? >> 8)' "$s" "$@"
}
check() { # NAME EXPECTED ACTUAL
  if [ "$2" = "$3" ]; then echo "  ok   $1"; oks=$((oks+1)); else echo "  FAIL $1"; echo "    expected: $2"; echo "    got:      $3"; fails=$((fails+1)); fi
}
box() { ARBACE_BOX_CPUS=$cpus t 300 "$B" "$@"; }
ms() { perl -MTime::HiRes=time -e 'printf "%d\n", time * 1000'; }
# cpu_s PID: the process's CPU time in seconds (user + system)
cpu_s() { ps -o time= -p "$1" | awk -F'[:.]' '{ n = NF; s = $(n-1) + $(n-2) * 60; if (n > 3) s += $(n-3) * 3600; printf "%.2f\n", s + $n / 100 }'; }

do_check() {
  sw_vers
  uname -m
  sysctl -n machdep.cpu.brand_string
  sysctl kern.hv_support hw.ncpu hw.memsize
  cat VERSION
  ls -l arbace-box arbace-box.elf hv.test
  command -v rlwrap nc python3 || true
}

do_sign() {
  local f
  for f in arbace-box hv.test; do
    codesign --sign - --entitlements arbace-box.entitlements --force "$f" || return 1
    codesign --display --entitlements - "$f"
  done
}

do_hv() {
  ./hv.test -test.v -test.count=1
}

do_checklist() {
  local st d0 d h s p port out w i
  fails=0 oks=0
  for cpus in 4 1; do
    echo "== the box, $cpus vCPUs"
    check "-e (+ 1 2)" "3" "$(box -e '(+ 1 2)' 2>&1)"
    h=$(date +%s)000
    check "the wall clock (new Date(), currentTimeMillis) is the Mac's" "true true" \
      "$(box -e "(let [h $h d (.getTime (java.util.Date.)) m (System/currentTimeMillis)] (print (< (abs (- d h)) 10000) (< (abs (- m h)) 10000)))" 2>&1)"
    check "the REPL on standard input" "$(printf 'user=> 3\nuser=> #'"'"'user/sq\nuser=> 49\nuser=> ')" \
      "$(printf '(+ 1 2)\n(defn sq [x] (* x x))\n(sq 7)\n' | box 2>&1 | tail -n +2)"
    check "stdin as a script (-)" "6" "$(echo '(println (* 2 3))' | box - 2>&1)"
    check "a script from the in-memory file system" "[:script (1 2)]" \
      "$(box -e '(spit "/tmp/s.ae" "(prn [:script (map inc [0 1])])")' /tmp/s.ae 2>&1)"
    check "-m, its namespace from the in-memory file system (ARBACE_PATH)" '("a" "b")' \
      "$(ARBACE_PATH=/tmp box -e '(spit "/tmp/hello.ae" "(ns hello) (defn -main [& args] (prn args))")' -m hello a b 2>&1)"
    box -e '(/ 1 0)' > "$TMPDIR/err.txt" 2>&1; st=$?
    check "an error's status and message" "1 Divide by zero" "$st $(grep -o 'Divide by zero' "$TMPDIR/err.txt" | head -1)"
    box -e '(System/exit 7)' > /dev/null 2>&1; st=$?
    check "System/exit's status" "7" "$st"
    check "files in memory, none of the Mac's" "[in memory false]" \
      "$(box -e '(do (spit "/tmp/x.txt" "in memory") (prn [(symbol (slurp "/tmp/x.txt")) (.exists (java.io.File. "/etc/hosts"))]))' 2>&1)"
    check "nil dereferences are NullPointerExceptions (BX60: the vectors' return to sigpanic)" "[:npe :npe :npe]" \
      "$(box -e '(prn (vec (for [f [#(aget ^ints (identity nil) 0) #(alength ^longs (identity nil)) #(.length ^String (identity nil))]] (try (f) :no (catch NullPointerException e :npe)))))' 2>&1)"
    check "a future and send" "[42 2]" \
      "$(box -e '(let [a (agent 1)] (send a inc) (await a) (prn [@(future (+ 40 2)) @a]) (shutdown-agents))' 2>&1)"
    check "a future during a long evaluation (R2)" "true" \
      "$(box -e '(let [p (promise) f (future (deliver p (System/nanoTime)))] (loop [i 0] (when (< i 3000000) (recur (inc i)))) (print (< @p (System/nanoTime))))' 2>&1)"
    check "a future's output while the REPL waits for input (R3)" "$(printf 'late\n2')" \
      "$( { printf '(future (Thread/sleep 300) (println "late"))\n'; sleep 2; printf '(+ 1 1)\n'; } | box 2>&1 | sed -e 's/user=> //g' | grep -v '^#object\|^Arbace' | grep .)"
    check "no processes" "java.io.IOException" \
      "$(box -e '(try (.start (ProcessBuilder. ["ls"])) (catch Exception e (print (.getName (class e)))))' 2>&1)"
    echo "== the network on loopback, $cpus vCPUs"
    check "no network unless ARBACE_BOX_NET" "java.net.SocketException" \
      "$(box -e '(try (java.net.ServerSocket. 0) (catch Exception e (print (.getName (class e)))))' 2>&1)"
    check "ARBACE_BOX_NET=loopback: a connection within the box" "hi" \
      "$(ARBACE_BOX_NET=loopback box -e '(let [ss (java.net.ServerSocket. 0 50 (java.net.InetAddress/getLoopbackAddress)) c (java.net.Socket. (.getInetAddress ss) (.getLocalPort ss)) s (.accept ss)] (.write (.getOutputStream c) (.getBytes "hi\n")) (print (.readLine (java.io.BufferedReader. (java.io.InputStreamReader. (.getInputStream s))))))' 2>&1)"
    check "ARBACE_BOX_NET=loopback: socket options (TCP_NODELAY, SO_KEEPALIVE)" "[true true]" \
      "$(ARBACE_BOX_NET=loopback box -e '(let [ss (java.net.ServerSocket. 0 50 (java.net.InetAddress/getLoopbackAddress)) c (java.net.Socket. (.getInetAddress ss) (.getLocalPort ss))] (.setTcpNoDelay c true) (.setKeepAlive c true) (prn [(.getTcpNoDelay c) (.getKeepAlive c)]))' 2>&1)"
    port=$((20000 + RANDOM % 20000))
    ARBACE_BOX_CPUS=$cpus ARBACE_BOX_NET=listen JAVA_TOOL_OPTIONS="-Darbace.server.repl=\"{:port $port :accept arbace.core.server/repl}\"" \
      "$B" -e '(Thread/sleep 60000)' > /dev/null 2>&1 &
    p=$!
    out=""
    for i in $(seq 100); do
      out=$(printf '(+ 1 2)\n[:box (+ 3 4)]\n:repl/quit\n' | nc -w 5 127.0.0.1 "$port" 2>/dev/null) && [ -n "$out" ] && break
      sleep 0.2
    done
    kill $p 2>/dev/null; wait $p 2>/dev/null
    check "a socket REPL from the Mac (nc 127.0.0.1 $port)" "$(printf 'user=> 3\nuser=> [:box 7]\nuser=> ')" "$out"
    echo "== idle at the prompt, $cpus vCPUs: the launcher's CPU over 20 s (the wake on events: no polling)"
    mkfifo "$TMPDIR/in.$cpus" 2>/dev/null
    ARBACE_BOX_CPUS=$cpus ARBACE_BOX_STATS=$res/idle-stats-$cpus.txt "$B" < "$TMPDIR/in.$cpus" > "$TMPDIR/idle.out" 2>&1 &
    p=$!
    exec 3> "$TMPDIR/in.$cpus"
    sleep 5
    d0=$(cpu_s $p)
    sleep 20
    d=$(cpu_s $p)
    printf '(+ 2 2)\n' >&3
    exec 3>&-
    wait $p
    rm -f "$TMPDIR/in.$cpus"
    w=$(awk -v a="$d0" -v b="$d" 'BEGIN { printf "%.2f", b - a }')
    echo "  idle CPU over 20 s: $w s"
    echo "idle $cpus vCPUs: $w s of CPU over 20 s at the prompt" >> "$res/measures.txt"
    check "an idle REPL takes under 0.5 s of CPU in 20 s, and answers after" "yes 4" \
      "$(awk -v w="$w" 'BEGIN { print (w < 0.5) ? "yes" : "no: " w }') $(grep -o 'user=> 4' "$TMPDIR/idle.out" | cut -c8-)"
    echo "== times, $cpus vCPUs (best of 5)"
    for s in "nil" "(time (reduce + (map inc (range 1000000))))" \
             "(do (defn fib [n] (if (< n 2) n (+ (fib (- n 1)) (fib (- n 2))))) (time (fib 25)))" \
             "(time (count (sort (map str (range 200000)))))"; do
      best=""
      for i in 1 2 3 4 5; do
        d0=$(ms); out=$(box -e "$s" 2>&1); d=$(ms)
        e=$(echo "$out" | grep -o 'Elapsed time: [0-9.]*' | awk '{ printf "%d", $3 }')
        [ -n "$e" ] || e=$((d - d0))
        if [ -z "$best" ] || [ "$e" -lt "$best" ]; then best=$e; fi
      done
      echo "  $s: $best ms"
      echo "time $cpus vCPUs: $s: $best ms" >> "$res/measures.txt"
    done
  done
  cpus=4
  echo "== the store"
  s=$TMPDIR/store
  rm -rf "$s"
  printf 'from the Mac' > "$TMPDIR/tool.txt"
  h=$(shasum -a 256 < "$TMPDIR/tool.txt" | cut -c1-64)
  check "the launcher's tool: put, then the guest's arbace.box/load" "$h from the Mac" \
    "$("$B" --store "$s" put "$TMPDIR/tool.txt" tool) $(box --store "$s" -e '(require (quote arbace.box))' -e '(print (arbace.box/load "tool"))' 2>&1)"
  box --store "$s" -e '(require (quote arbace.box))' -e '(arbace.box/put! "back" "from the guest")' > /dev/null 2>&1
  check "the launcher's tool: get, ls" "from the guest yes" \
    "$("$B" --store "$s" get back) $("$B" --store "$s" ls | grep -q ' tool$' && echo yes)"
  box --store "$s" -e '(require (quote arbace.box))' -e '(arbace.box/put! "hello.ae" "(prn [:from-ref *command-line-args*])")' > /dev/null 2>&1
  check "a script run from a ref (ref:NAME)" '[:from-ref ("a" "b")]' "$(ARBACE_BOX_STORE=$s box ref:hello.ae a b 2>&1)"
  check "compare-and-set" "[true false]" \
    "$(box --store "$s" -e '(require (quote [arbace.box :as b]))' -e '(let [x (b/put "x") y (b/put "y")] (prn [(b/ref! "c" x nil) (b/ref! "c" y nil)]))' 2>&1)"
  echo "== RAM above 4 GiB (HV1 (b), doc/go/BOX-NOTES.md): the framework's default IPA, 36 bits"
  check "ARBACE_BOX_RAM=6GiB: the collector's limit 1 GiB under it, a 1 GiB array" "[5120 7]" \
    "$(ARBACE_BOX_RAM=6GiB box -e '(let [a (byte-array (* 1024 1024 1024))] (aset a (dec (alength a)) (byte 7)) (prn [(quot (.maxMemory (Runtime/getRuntime)) 1048576) (aget a (dec (alength a)))]))' 2>&1)"
  check "ARBACE_BOX_RAM=60GiB: the most under 64 GiB (4 GiB + 60 GiB), mapped, not touched" "61440" \
    "$(ARBACE_BOX_RAM=60GiB box -e '(print (quot (+ (.maxMemory (Runtime/getRuntime)) (* 1024 1024 1024)) 1048576))' 2>&1)"
  ARBACE_BOX_RAM=61GiB box -e nil > "$TMPDIR/ram.txt" 2>&1; st=$?
  check "ARBACE_BOX_RAM=61GiB: refused, beyond the 36-bit addresses" "1 beyond" "$st $(grep -o beyond "$TMPDIR/ram.txt" | head -1)"
  m=$(sysctl -n hw.memsize 2>/dev/null || echo 0)
  echo "hw.memsize $m" >> "$res/measures.txt"
  if [ "$m" -ge $((24 << 30)) ]; then
    d0=$(ms)
    out=$(ARBACE_BOX_RAM=16GiB box -e '(let [g (* 1024 1024 1024) as (vec (for [i (range 10)] (let [a (byte-array g)] (java.util.Arrays/fill a (byte (inc i))) a)))] (print (count as) (reduce + (for [a as k (range 0 g (* 64 1024 1024))] (aget ^bytes a k)))))' 2>&1)
    d=$(ms)
    check "ARBACE_BOX_RAM=16GiB: 10 GiB allocated and touched" "10 880" "$out"
    echo "ram 16 GiB, 10 GiB touched: $((d - d0)) ms" >> "$res/measures.txt"
  else
    echo "  (the 10 GiB probe skipped: hw.memsize $m, under 24 GiB)"
  fi
  echo "== the checklist: $oks ok, $fails failed"
  [ $fails -eq 0 ]
}

step() { # NAME: the step, its output saved and shown, its status recorded
  local n=$1 st
  echo "#### $n" >&2
  "do_$n" 2>&1 | tee "$res/$n.log"
  st=${PIPESTATUS[0]}
  echo "$n: status $st" >> "$res/status.txt"
  return $st
}

cmd=${1:-}
[ $# -gt 0 ] && shift
case $cmd in
  check|sign|hv|checklist) step "$cmd" ;;
  all)
    rm -rf "$res"; mkdir -p "$res"
    s=0
    step check || s=1
    step sign || s=1
    step hv || s=1
    step checklist || s=1
    tar -czf mac-results.tar.gz mac-results
    echo "mac.sh: $(cat "$res/status.txt" | tr '\n' ' ')" >&2
    echo "mac.sh: send back $here/mac-results.tar.gz" >&2
    exit $s ;;
  run) exec "$B" "$@" ;;
  *) sed -n '/^#   \.\/mac.sh/,/^set -u/p' "$0" | sed '$d; s/^# \{0,1\}//' >&2; exit 2 ;;
esac
