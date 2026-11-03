#!/bin/bash
# The cover build's hook and run directories (doc/GATE-SURVEY.md, GT8.3; review 182's lows), on toys: bin/lib/cover.bash's
# overlay (cover_go_flags: every file of cover_hook_table), and the hook's files (tools/covsubset/hook) built into a toy
# Go program with -cover by TamaGo's go (ARBACE_GOROOT, /root/tamago-go; skipped with a message when it is missing), as
# the cover builds add them: the program's own signal handling kept, no tmp.covcounters left, an ignored SIGHUP
# ignored (L3). The cover build is amd64-only; the hook's logic is not: the toy runs on the host's architecture.
# Seconds after the toy's first build (about 10 s). No Arbace executable.
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
cover_go_flags /R "$t/prog" "$t/work" "$t/overlay.json" "$t/ov.json" > "$t/flags.txt" || no "cover_go_flags failed"
miss=0
while read -r p f; do grep -qF "\"${ARBACE_GOROOT:-/root/tamago-go}/src/$p/zz_arbace_cover.go\": \"/R/$f\"" "$t/ov.json" || miss=$((miss + 1)); done <<< "$(cover_hook_table)"
if [ "$miss" = 0 ] && grep -q '"a.go": "b.go",' "$t/ov.json" && [ "$(tail -1 "$t/ov.json" | tail -c 3)" = '}}' ]; then ok "cover_go_flags: every hook file ($(cover_hook_table | wc -l)) in the overlay"; else no "the overlay: $(cat "$t/ov.json")"; fi

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
{ printf '{"Replace":{'; sep=
  while read -r p f; do printf '%s"%s/src/%s/zz_arbace_cover.go": "%s/%s"' "$sep" "$tg" "$p" "$here" "$f"; sep=,; done <<< "$(cover_hook_table)"
  printf '}}\n'; } > "$t/toy.json"
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
else
  no "the toy does not build with the hook: $(tail -5 "$t/build.log")"
fi
fi

echo "$((n - bad)) of $n passed"
[ "$bad" = 0 ]
