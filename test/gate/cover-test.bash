#!/bin/bash
# The cover build's hook and run directories (doc/GATE-SURVEY.md, GT8.3; review 182's lows), on toys: bin/lib/cover.bash's
# overlay (cover_go_flags: every file of cover_hook_table), and the hook's files (tools/covsubset/hook) built into a toy
# Go program with -cover by TamaGo's go (ARBACE_GOROOT, /root/tamago-go; skipped with a message when it is missing), as
# the cover builds add them: the program's own signal handling kept, no tmp.covcounters left, an ignored SIGHUP
# ignored (L3); a deadlock reported as Go reports it, at once, and none where Go reports none (L2); and a run directory
# (ARBACE_COVER_DIR) of one cover build's data alone (L5). The cover build is amd64-only; the hook's logic is not: the toy runs on the host's architecture.
# Seconds after the toy's first build (about 10 s). bin/cover subset and classes (review 195's mediums) on toys too: the latter on
# the tools' host (stage 0).
# Usage: test/gate/cover-test.bash
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$(mktemp -d "$TMPDIR/cover-test.XXXXXX") || exit 2
[ -n "${KEEP:-}" ] || trap 'rm -rf "$t"' EXIT
n=0 bad=0
ok() { n=$((n + 1)); echo "ok   $*"; }
no() { n=$((n + 1)); bad=$((bad + 1)); echo "FAIL $*"; }
. "$here/bin/lib/cover.bash"

# (1) cover_go_flags: the overlay gets every hook file of cover_hook_table, each into its package as zz_arbace_cover.go
mkdir -p "$t/prog/go/arbace/jrt"; printf '{"Replace":{\n "a.go": "b.go"}}\n' > "$t/overlay.json"
cover_go_flags "$here" "$t/prog" "$t/work" "$t/overlay.json" "$t/ov.json" > "$t/flags.txt" || no "cover_go_flags failed"
miss=0
while read -r p f _; do grep -qF "\"${ARBACE_GOROOT:-/root/tamago-go}/src/$p/zz_arbace_cover.go\": \"$here/$f\"" "$t/ov.json" || miss=$((miss + 1)); done <<< "$(cover_hook_table)"
if [ "$miss" = 0 ] && grep -q '"a.go": "b.go",' "$t/ov.json" && [ "$(tail -1 "$t/ov.json" | tail -c 3)" = '}}' ]; then ok "cover_go_flags: every hook file ($(cover_hook_table | wc -l)) in the overlay"; else no "the overlay: $(cat "$t/ov.json")"; fi

# the hook's files pinned (the cover key holds bin/lib/cover.bash and cover.go, not gate_key), a stale pin refused
if cover_hook_stale "$here" > "$t/stale"; then ok "the hook's files match their pins in cover_hook_table"; else no "stale pins: $(cat "$t/stale")"; fi
mkdir -p "$t/r/tools/covsubset/hook/signal" "$t/r/tools/covsubset/hook/runtime"
cp "$here/tools/covsubset/hook/runtime/timers.go" "$t/r/tools/covsubset/hook/runtime/"; echo '// changed' > "$t/r/tools/covsubset/hook/signal/refs.go"
if ! cover_go_flags "$t/r" "$t/prog" "$t/work" "$t/overlay.json" "$t/ov2.json" > "$t/flags2.txt" 2>"$t/flags2.err" && grep -q 'differ from their pins.*signal/refs.go' "$t/flags2.err"; then
  ok "cover_go_flags refuses a hook file that differs from its pin"; else no "a stale pin taken: $(cat "$t/flags2.err")"; fi

# review 195's HIGH: the cover builds' one overlay (cover_overlay), C's AOT shards' too: every hook file, with no base
# overlay as with one (bin/lib/ci/check-aot wrote its own, cover.go alone, and its shards failed on signal.ArbaceCoverRefs)
cover_overlay "$here" "" "$t/ov0.json" 2>"$t/ov0.err" || no "cover_overlay without a base: $(cat "$t/ov0.err")"
miss=0
while read -r p f _; do grep -qF "\"${ARBACE_GOROOT:-/root/tamago-go}/src/$p/zz_arbace_cover.go\": \"$here/$f\"" "$t/ov0.json" || miss=$((miss + 1)); done <<< "$(cover_hook_table)"
if [ "$miss" = 0 ] && [ "$(head -c 12 "$t/ov0.json")" = '{"Replace": ' ] && [ "$(tail -c 3 "$t/ov0.json")" = '}}' ]; then ok "cover_overlay without a base: every hook file ($(cover_hook_table | wc -l)) in a new overlay"; else no "cover_overlay without a base: $(cat "$t/ov0.json")"; fi
if grep -q 'cover_overlay "$root" "" "$work/cover-overlay.json"' "$here/bin/lib/ci/check-aot" && ! grep -q zz_arbace_cover "$here/bin/lib/ci/check-aot"; then
  ok "C's AOT shards take cover_overlay's overlay, no overlay of their own"; else no "bin/lib/ci/check-aot writes an overlay of its own: $(grep -n 'overlay' "$here/bin/lib/ci/check-aot" | head -3)"; fi

# (2) the hook in a toy
tg=${ARBACE_GOROOT:-/root/tamago-go}
if [ ! -x "$tg/bin/go" ]; then
  echo "skip the hook's toy: no $tg/bin/go here"
else
mkdir -p "$t/toy"
printf 'module toy\n\ngo 1.27\n' > "$t/toy/go.mod"
cat > "$t/toy/main.go" <<'GO'
package main

import (
	"fmt"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"
)

func main() {
	switch os.Args[1] {
	case "loop":
		for {
			time.Sleep(100 * time.Millisecond)
		}
	case "handler": // its own SIGTERM handler, which exits 0 at once
		c := make(chan os.Signal, 1)
		signal.Notify(c, syscall.SIGTERM)
		fmt.Println("ready")
		<-c
		os.Exit(0)
	case "keepgoing": // its own SIGTERM handler, which goes on and exits 3 later
		c := make(chan os.Signal, 1)
		signal.Notify(c, syscall.SIGTERM)
		fmt.Println("ready")
		<-c
		time.Sleep(1500 * time.Millisecond)
		fmt.Println("still here")
		os.Exit(3)
	case "deadlock":
		fmt.Println("before")
		var c chan int
		<-c
	case "wg":
		var wg sync.WaitGroup
		var mu sync.Mutex
		mu.Lock()
		wg.Add(2)
		go func() { mu.Lock(); wg.Done() }()
		go func() { mu.Lock(); wg.Done() }()
		wg.Wait()
	case "timer": // every goroutine waits on a channel a timer sends to: no deadlock
		<-time.After(2 * time.Second)
		fmt.Println("after")
	case "stdin": // a read of the standard input: no deadlock
		b := make([]byte, 1)
		os.Stdin.Read(b)
		fmt.Println("read")
	case "ownsig": // waits on a signal of its own: Go's detector does not fire either
		c := make(chan os.Signal, 1)
		signal.Notify(c, syscall.SIGUSR1)
		<-c
	}
}
GO
# the toy's overlay is the cover builds' (cover_overlay without a base, as C's AOT shards take it)
ARBACE_GOROOT=$tg cover_overlay "$here" "" "$t/toy.json" || no "cover_overlay for the toy"
if (cd "$t/toy" && env -u GOOS -u GOARCH GOROOT="$tg" GOFLAGS= GOTOOLCHAIN=local CGO_ENABLED=0 \
      "$tg/bin/go" build -trimpath -buildvcs=false -overlay "$t/toy.json" -cover -covermode=set -tags=arbacecover -o "$t/toy.exe" .) > "$t/build.log" 2>&1; then
  ok "the toy builds with the hook ($(cover_hook_table | wc -l) files)"
  toy=$t/toy.exe
  fresh() { rm -rf "$t/cov"; mkdir -p "$t/cov"; }
  counters() { find "$t/cov" -name 'covcounters.*' | wc -l; }
  # term MODE [DELAY]: the toy's run, SIGTERM after DELAY (0.3 s); its status in st, its output in $t/out
  term() { GOCOVERDIR=$t/cov "$toy" "$1" > "$t/out" 2>&1 & local p=$!; sleep "${2:-0.3}"; kill -TERM "$p"; wait "$p"; st=$?; }

  # (L3) a program without a handler of its own: the counters written, the signal raised again (143)
  fresh; term loop
  if [ "$st" = 143 ] && [ "$(counters)" -ge 1 ]; then ok "SIGTERM, no handler of the program's: the counters written, the status the signal's (143)"; else no "SIGTERM, no handler: status $st, $(counters) counter files"; fi
  # (L3) the program's own handler is left to act: neither raised again nor reset
  fresh; term keepgoing
  if [ "$st" = 3 ] && grep -q 'still here' "$t/out" && [ "$(counters)" -ge 1 ]; then ok "the program's own SIGTERM handler kept (status 3, its output), the counters written"; else no "the program's own handler: status $st, $(tr '\n' ' ' < "$t/out"), $(counters) counter files"; fi
  # (L3) a program whose handler exits at once: no tmp.covcounters left by a write the exit cut short
  fresh; sts=
  for i in 1 2 3 4 5 6 7 8 9 10 11 12; do term handler 0.2; sts="$sts$st "; done
  tmp=$(find "$t/cov" -name 'tmp.*' | wc -l)
  if [ "$tmp" = 0 ] && [ "$sts" = "0 0 0 0 0 0 0 0 0 0 0 0 " ]; then ok "a handler that exits at once: status 0 in 12 runs, no tmp.covcounters left"; else no "a handler that exits at once: statuses $sts, $tmp tmp files"; fi
  # (L3) SIGHUP ignored at the start (nohup) stays ignored
  fresh; (trap '' HUP; GOCOVERDIR=$t/cov exec "$toy" loop) > "$t/out" 2>&1 & p=$!
  sleep 0.3; kill -HUP "$p"; sleep 1.5
  if kill -0 "$p" 2>"$t/kill.err"; then ok "an ignored SIGHUP (nohup) stays ignored"; { kill -KILL "$p"; wait "$p"; } 2>"$t/kill.err"; else wait "$p"; no "an ignored SIGHUP ended the program: status $?"; fi

  # (L2) a deadlock is one: Go's message and status 2 at once, the counters written (the hook's signal thread and flush
  # kept Go's own detector off: the check's wall cap, 124, ended it); none where Go reports none
  # run MODE SECS: the toy's run under timeout SECS (stdin empty; for stdin, a pipe that stays open 3 s); st, $t/out, $t/err
  : > "$t/empty"
  run() {
    if [ "$1" = stdin ]; then (sleep 3; echo x) | GOCOVERDIR=$t/cov timeout "$2" "$toy" "$1" > "$t/out" 2> "$t/err"; st=$?
    else GOCOVERDIR=$t/cov timeout "$2" "$toy" "$1" < "$t/empty" > "$t/out" 2> "$t/err"; st=$?; fi
  }
  fresh; s0=$(date +%s); run deadlock 8
  if [ "$st" = 2 ] && [ $(( $(date +%s) - s0 )) -le 3 ] && [ "$(sed -n 1p "$t/err")" = "fatal error: all goroutines are asleep - deadlock!" ] \
     && grep -q '^goroutine 1 \[chan receive (nil chan)\]:' "$t/err" && [ "$(counters)" -ge 1 ]; then ok "a deadlock: Go's message, the goroutines, status 2 at once, the counters written"
  else no "a deadlock: status $st in $(( $(date +%s) - s0 )) s, $(head -3 "$t/err" | tr '\n' ' '), $(counters) counter files"; fi
  fresh; run wg 8
  if [ "$st" = 2 ] && grep -q 'deadlock!' "$t/err"; then ok "a deadlock on a WaitGroup and a mutex: status 2"; else no "the WaitGroup's deadlock: status $st"; fi
  fresh; run timer 8
  if [ "$st" = 0 ] && grep -q after "$t/out"; then ok "a wait a timer ends: no deadlock"; else no "a timer's wait: status $st, $(cat "$t/err")"; fi
  fresh; run stdin 8
  if [ "$st" = 0 ] && grep -q read "$t/out"; then ok "a read of the standard input: no deadlock"; else no "a read: status $st, $(cat "$t/err")"; fi
  fresh; run ownsig 3
  if [ "$st" = 124 ]; then ok "a wait for a signal of the program's own: no deadlock (as Go)"; else no "a wait for its own signal: status $st, $(cat "$t/err")"; fi
  fresh; ARBACE_COVER_DEADLOCK=0 run deadlock 3
  if [ "$st" = 124 ]; then ok "ARBACE_COVER_DEADLOCK=0: not detected"; else no "ARBACE_COVER_DEADLOCK=0: status $st"; fi
else
  no "the toy does not build with the hook: $(tail -5 "$t/build.log")"
fi
fi

# (L5) a run directory holds one cover build's data: RUN/key written by the first check's cover_exe, another build's
# run into it refused, and bin/cover blocks refusing a run of another build than the one whose packages it would read
for k in 1 2; do mkdir -p "$t/c$k/amd64"; printf '#!/bin/sh\n' > "$t/c$k/amd64/arbace"; chmod +x "$t/c$k/amd64/arbace"; echo "key$k" > "$t/c$k/key"; done
x1=$(ARBACE_COVER_CACHE=$t/c1 ARBACE_COVER_DIR=$t/run cover_exe "$here" 2>"$t/claim1.err"); s1=$?
x1b=$(ARBACE_COVER_CACHE=$t/c1 ARBACE_COVER_DIR=$t/run cover_exe "$here" 2>"$t/claim1b.err"); s1b=$?
if [ "$s1" = 0 ] && [ "$x1" = "$t/c1/amd64/arbace" ] && [ "$(cat "$t/run/key" 2>&1)" = key1 ] && [ "$s1b" = 0 ]; then ok "the first run into a directory names its cover build (RUN/key), a second of the same build is taken"
else no "the first run's claim: $s1 $s1b '$x1' $(cat "$t/run/key" "$t/claim1.err" 2>&1)"; fi
x2=$(ARBACE_COVER_CACHE=$t/c2 ARBACE_COVER_DIR=$t/run cover_exe "$here" 2>"$t/claim2.err"); s2=$?
if [ "$s2" = 1 ] && [ -z "$x2" ] && grep -q 'holds the data of another cover build' "$t/claim2.err" && [ "$(cat "$t/run/key")" = key1 ]; then ok "a run of another cover build into it refused"
else no "another build's run: status $s2, '$x2', $(cat "$t/claim2.err")"; fi
(unset ARBACE_COVER_DIR; ARBACE_COVER_CACHE=$t/c2 "$here/bin/cover" blocks "$t/run") > "$t/blocks.out" 2>&1; sb=$?
if [ "$sb" = 1 ] && grep -q 'holds the data of cover build key1' "$t/blocks.out"; then ok "bin/cover blocks refuses a run of another build than its cover cache's"
else no "bin/cover blocks on another build's run: status $sb, $(cat "$t/blocks.out")"; fi

# night 3's java-interp: bin/cover blocks over a run of more tests than one command line holds (xargs ran the tool twice,
# the second's tests.tsv, 131 tests, overwriting the first's 3,190): every directory in one tests.tsv and summary
if [ -x "$tg/bin/go" ]; then
  mkdir -p "$t/big/java-interp"; echo key1 > "$t/big/key"; echo arbace/jrt > "$t/c1/pkgs-arbace.txt"
  long=$(printf 'x%.0s' $(seq 1 100))
  for i in $(seq 1 3000); do d=$t/big/java-interp/t$i-$long; mkdir "$d"; echo "t$i" > "$d/id"; done
  (unset ARBACE_COVER_DIR; ARBACE_COVER_CACHE=$t/c1 "$here/bin/cover" blocks "$t/big") > "$t/big.out" 2>&1; sb=$?
  if [ "$sb" = 0 ] && [ "$(wc -c < "$t/big/blocks/java-interp.dirs")" -gt 262144 ] && [ "$(wc -l < "$t/big/blocks/java-interp/tests.tsv")" = 3000 ] \
     && [ "$(grep -c '^tests' "$t/big.out")" = 3 ] && grep -qx 'tests	3000' "$t/big.out"; then
    ok "bin/cover blocks: 3,000 tests' directories ($(wc -c < "$t/big/blocks/java-interp.dirs") bytes of paths) read by one run of the tool, all in tests.tsv"
  else no "bin/cover blocks over 3,000 tests: status $sb, $(wc -l < "$t/big/blocks/java-interp/tests.tsv" 2>&1) in tests.tsv, $(grep '^tests' "$t/big.out" | tr '\n\t' ' :')"; fi
fi

# review 195's medium 2: bin/cover subset --corpus C, a corpus's subset from a whole record's classes and keep: only C/'s ids
# (the reducer, tools/covsubset, built by TamaGo's go as bin/cover builds it)
if [ -x "$tg/bin/go" ]; then
  mkdir -p "$t/sub"; printf 'mode: set\na.go:1.1,2.2 1 1\n' > "$t/sub/p1"; printf 'mode: set\na.go:3.1,4.2 1 1\n' > "$t/sub/p2"
  printf 'bg/a.c\t%s\t1\nbg/b.c\t%s\t1\n' "$t/sub/p1" "$t/sub/p2" > "$t/sub/tests.tsv"
  printf 'bg/a.c\t-\nbg/b.c\tcls-x\ntx/c.c\tcls-y\ntx/d.c\tcls-z\n' > "$t/sub.classes"; printf 'tx/k.c\nbg/a.c\n' > "$t/sub.keep"
  cp -a "$t/sub" "$t/sub2"
  "$here/bin/cover" subset "$t/sub" --keep "$t/sub.keep" --classes "$t/sub.classes" --seeds 2 --corpus bg > "$t/sub.out" 2>&1 || no "bin/cover subset --corpus: $(tail -3 "$t/sub.out")"
  "$here/bin/cover" subset "$t/sub2" --keep "$t/sub.keep" --classes "$t/sub.classes" --seeds 2 > "$t/sub2.out" 2>&1
  if [ "$(cut -f1 "$t/sub/index.tsv" | tr '\n' ' ')" = "bg/a.c bg/b.c " ] && ! grep -q '^tx/' "$t/sub/subset.txt" && [ "$(grep -c . "$t/sub/subset.txt")" = 2 ] \
     && grep -q '^tx/' "$t/sub2/subset.txt"; then ok "bin/cover subset --corpus bg: the other corpora's classed and kept ids out of its index and subset (without it, in)"
  else no "bin/cover subset --corpus: index $(cut -f1 "$t/sub/index.tsv" | tr '\n' ' '), subset $(tr '\n' ' ' < "$t/sub/subset.txt" 2>&1), without $(tr '\n' ' ' < "$t/sub2/subset.txt" 2>&1)"; fi
fi

# review 195's medium 3: bin/cover classes reads every DIVERGENCES file the level's judge reads (comma-separated, in order),
# and warns on a non-pass in no class (on the tools' host, stage 0)
printf '[{:id :aot-only :kind :design :match [#"aot-thing"]}]\n' > "$t/d1.edn"
printf '[{:id :gap-b :kind :gap :tests ["bg/b.c"]} {:id :des-c :kind :design :match [#"differ"]}]\n' > "$t/d2.edn"
printf '[["bg/a.c" :pass ""] ["bg/b.c" :fail "x"] ["bg/c.c" :fail "differ stdout"] ["bg/e.c" :fail "aot-thing"] ["bg/f.c" :refused "odd"]]\n' > "$t/res.edn"
"$here/bin/cover" classes "$t/d1.edn,$t/d2.edn" "$t/res.edn" -o "$t/cl" > "$t/cl.out" 2>"$t/cl.err"; sc=$?
if [ "$sc" = 0 ] && [ "$(tr '\t\n' ': ' < "$t/cl.classes")" = "bg/a.c:- bg/b.c:gap-b bg/c.c:des-c bg/e.c:aot-only bg/f.c:unclassed-refused " ] \
   && [ "$(cat "$t/cl.keep")" = bg/b.c ] && grep -q 'warning: 1 non-pass row(s) in no class.*bg/f.c' "$t/cl.err"; then
  ok "bin/cover classes: two DIVERGENCES files read in order as one list, a non-pass in no class warned"
else no "bin/cover classes with two files: status $sc, $(tr '\t\n' ': ' < "$t/cl.classes" 2>&1) keep $(cat "$t/cl.keep" 2>&1) $(cat "$t/cl.err")"; fi

echo "$((n - bad)) of $n passed"
[ "$bad" = 0 ]
