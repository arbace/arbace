#!/usr/bin/env bash
# Arbace's own linux/arm64 executable on a64i (queue 302; doc/asm/ARM64-SURVEY.md "As built: AR25"), run by bin/a64i arbace-check:
#   check.bash ROOT A64I [--record] [--only NAME,...] [--jobs N]
# The executable: A64I_ARBACE_EXE, else ARBACE_HOST_EXE, else the checkout's stage 0 (target/seed/arm64/arbace); read, never
# written. Three runs, each natively on this arm64 CPU (the reference) and on a64i, in an empty directory with one environment:
#   e     -e '(+ 1 2)'
#   repl  the REPL fed from repl.ae on its stdin (a session: definitions, an exception and *e, atoms, futures, a promise, pmap,
#         Thread/sleep, a require)
#   ns    -m demo.core 1 two, the namespaces of src/ (ARBACE_PATH) loaded from their source with the runtime's (arbace.set,
#         arbace.walk, arbace.string, arbace.pprint): a protocol, records, a multimethod, a macro, ex-info
# a64i's run must equal the native's in its status, stdout and stderr byte for byte; the native's stdout is NAME.want's (repl's
# less its first line, the banner, which names the executable's version; --record writes them from the native runs); the
# lockstep's sys mode of each must be EQUAL TO THE FIRST THREAD (every system call before Go's runtime starts its first thread
# compared). The controls must fail: arbace-1 (e's a64i run against repl's native run), arbace-2 (ns's a64i run against a native
# run with another argument), arbace-3 (e's native stdout against repl.want), arbace-4 (e's lockstep judged as a program's whole
# run, plain EQUAL). Rows in .tmp/a64i/arbace/results.tsv: NAME RESULT INSTRUCTIONS SECONDS. Needs an arm64 host.
set -uo pipefail
root=$1 a64i=$2; shift 2
rec= only= jobs=3
while [ $# -gt 0 ]; do
  case $1 in
    --record) rec=1; shift ;;
    --only) only=$2; shift 2 ;;
    --jobs) jobs=$2; shift 2 ;;
    *) echo "a64i arbace-check: unknown argument $1" >&2; exit 2 ;;
  esac
done
case $(uname -m) in aarch64|arm64) ;; *) echo "a64i arbace-check: needs an arm64 host (this CPU's run is the reference)" >&2; exit 2 ;; esac
here=$root/test/asm/a64i-arbace
exe=${A64I_ARBACE_EXE:-${ARBACE_HOST_EXE:-$root/target/seed/arm64/arbace}}
[ -x "$exe" ] || { echo "a64i arbace-check: no executable $exe (bin/seed --build, or A64I_ARBACE_EXE)" >&2; exit 2; }
budget=${A64I_ARBACE_BUDGET:-600}
W=$root/.tmp/a64i/arbace; rm -rf "$W"; mkdir -p "$W"
names=(e repl ns); [ -z "$only" ] || IFS=, read -ra names <<< "$only"

# run NAME TAG CMD...: CMD in W/NAME.TAG/cwd with the one environment, stdin per NAME; W/NAME.TAG.{status,out,err}
envof() { printf '%s\n' env -i PATH=/usr/bin:/bin HOME="$1" TMPDIR="$1" LANG=C ARBACE_PATH="$here/src"; }
args_of() { case $1 in e) printf '%s\n' -e '(+ 1 2)' ;; repl) ;; ns) printf '%s\n' -m demo.core 1 "${2:-two}" ;; esac; }
stdin_of() { case $1 in repl) echo "$here/repl.ae" ;; *) echo /dev/null ;; esac; }
run() {
  local n=$1 tag=$2; shift 2
  local d=$W/$n.$tag; mkdir -p "$d/cwd"
  local -a E; mapfile -t E < <(envof "$d/cwd")
  local st=0
  (cd "$d/cwd" && "${E[@]}" "$@" < "$(stdin_of "$n")" > "$W/$n.$tag.out" 2> "$W/$n.$tag.err") || st=$?
  echo $st > "$W/$n.$tag.status"
}
one() {
  local n=$1; local -a A; mapfile -t A < <(args_of "$n")
  run "$n" native "$exe" "${A[@]}"
  local t0=$SECONDS
  run "$n" a64i timeout "$budget" "$a64i" run --stats "$exe" "${A[@]}"
  echo $((SECONDS - t0)) > "$W/$n.a64i.seconds"
  grep '^a64i: [0-9]* instructions' "$W/$n.a64i.err" | sed 's/^a64i: \([0-9]*\) instructions.*/\1/' > "$W/$n.a64i.count"
  grep -v '^a64i: [0-9]* instructions' "$W/$n.a64i.err" > "$W/$n.a64i.err2"; mv "$W/$n.a64i.err2" "$W/$n.a64i.err"
  local -a E; mapfile -t E < <(envof "$W/$n.sys")
  mkdir -p "$W/$n.sys"
  (cd "$W/$n.sys" && "${E[@]}" timeout 300 "$a64i" trace --mode sys "$exe" "${A[@]}" > "$W/$n.trace" 2>&1) || true
  [ "$n" != ns ] || run ns other "$exe" -m demo.core 1 three
}
t_start=$SECONDS
active=0
for n in "${names[@]}"; do
  case $n in e|repl|ns) ;; *) echo "a64i arbace-check: no run $n (e, repl, ns)" >&2; exit 2 ;; esac
  one "$n" & active=$((active + 1))
  if [ $active -ge "$jobs" ]; then wait -n; active=$((active - 1)); fi
done
wait

same() { cmp -s "$W/$1.status" "$W/$2.status" && cmp -s "$W/$1.out" "$W/$2.out" && cmp -s "$W/$1.err" "$W/$2.err"; }
wantof() { case $1 in repl) tail -n +2 "$W/$1.native.out" ;; *) cat "$W/$1.native.out" ;; esac; }
fails=0; : > "$W/results.tsv"
for n in "${names[@]}"; do
  [ -z "$rec" ] || wantof "$n" > "$here/$n.want"
  r=pass d=
  if ! [ "$(cat "$W/$n.native.status")" = 0 ] || ! cmp -s <(wantof "$n") "$here/$n.want"; then
    r=native d="the native's run (status $(cat "$W/$n.native.status")) is not $n.want: $(diff <(wantof "$n") "$here/$n.want" | head -3 | tr '\n' ' ')"
  elif ! same "$n.native" "$n.a64i"; then
    r=a64i d="a64i status $(cat "$W/$n.a64i.status"), native $(cat "$W/$n.native.status"): $(diff "$W/$n.native.out" "$W/$n.a64i.out" | head -3 | tr '\n' ' ')$(diff "$W/$n.native.err" "$W/$n.a64i.err" | head -3 | tr '\n' ' ')"
  elif ! grep -q '^a64i trace sys: EQUAL TO THE FIRST THREAD:' "$W/$n.trace" || ! grep -qx 'loader: EQUAL' "$W/$n.trace"; then
    r=lockstep d=$(head -2 "$W/$n.trace" | tr '\n' ' ')
  fi
  ins=$(cat "$W/$n.a64i.count"); secs=$(cat "$W/$n.a64i.seconds")
  printf '%s\t%s\t%s\t%s\n' "$n" "$r" "$ins" "$secs" >> "$W/results.tsv"
  if [ $r = pass ]; then
    echo "a64i arbace-check: $n: equal to the native's ($(wc -c < "$W/$n.native.out") bytes of stdout, $(wc -c < "$W/$n.native.err") of stderr, status $(cat "$W/$n.native.status")), ${ins:-?} instructions in ${secs}s; lockstep: $(sed -n 's/^a64i trace sys: EQUAL TO THE FIRST THREAD: \([0-9]* system calls, [0-9]* instructions\).*/\1/p' "$W/$n.trace") to the first thread"
  else echo "FAIL: a64i arbace-check: $n: $r: $d"; fails=$((fails + 1)); fi
done

# the controls (each must fail)
ctl() { if [ "$2" = differs ]; then echo "control $1: fails as it must"; else echo "FAIL: control $1: passes"; fails=$((fails + 1)); fi; }
has() { for x in "${names[@]}"; do [ "$x" = "$1" ] && return 0; done; return 1; }
if has e && has repl; then same e.a64i repl.native && ctl arbace-1 same || ctl arbace-1 differs; fi
if has ns; then same ns.a64i ns.other && ctl arbace-2 same || ctl arbace-2 differs; fi
if has e; then cmp -s <(wantof e) "$here/repl.want" && ctl arbace-3 same || ctl arbace-3 differs; fi
if has e; then grep -q '^a64i trace sys: EQUAL:' "$W/e.trace" && ctl arbace-4 same || ctl arbace-4 differs; fi
echo "a64i arbace-check: ${#names[@]} runs in $((SECONDS - t_start))s (W: $W)"
[ $fails = 0 ] || exit 1
echo "a64i arbace-check: ok"
