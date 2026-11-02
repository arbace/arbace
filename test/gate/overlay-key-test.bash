#!/bin/bash
# bin/jrt's patched-runtime overlay (.tmp/jrt-go/overlay) is keyed by its inputs (queue 162, audit 037 finding
# 3), not reused by mtime alone: a change of overlay/go's bytes, of the printer's sources, of the printing
# host's bytes or of the toolchain's VERSION prints again even when every file is older than the printed
# overlay (an older host's print was packed once); a touch with the same bytes does not. The functions
# overlay_key and make_overlay are extracted from bin/jrt (JRT=FILE: another version of the script, to see the
# test fail on it); the printer is a stub counting its runs, the host a fabricated file: no Go build, no Arbace.
# Fabricated tree under .tmp/overlay-key-test. Usage: test/gate/overlay-key-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
mkdir -p "$TMPDIR"
T=$here/.tmp/overlay-key-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
jrt=${JRT:-$here/bin/jrt}

# the extracted functions
sed -n '/^overlay_pkgs=/,/^# check_overlay/p' "$jrt" | sed '$d' > "$T/funcs.bash"
grep -q '^make_overlay()' "$T/funcs.bash" || { echo "FAIL make_overlay not found in $jrt"; exit 1; }

root=$T/root
mkdir -p "$root/overlay/go" "$root/arbace/repr/go" "$root/bin/lib" "$T/goroot" "$T/host"
for p in runtime reflect; do echo "(ns overlay.$p) ; v1" > "$root/overlay/go/$p.ae"; done
echo ';; printer' > "$root/arbace/repr/go.ae"
echo ';; print' > "$root/arbace/repr/go/print.ae"
echo 'host v1' > "$T/host/arbace"; chmod +x "$T/host/arbace"
echo go1.27.1 > "$T/goroot/VERSION"
echo '#!/bin/bash' > "$root/bin/repr-go"
# the stub printer: bin/repr-go print --layout gofmt SRC OUTDIR, counting runs
cat >> "$root/bin/repr-go" <<EOS
echo run >> "$T/prints"
mkdir -p "\$5" && echo "// \$(cat "\$4")" > "\$5/x.go"
EOS
echo '# host' > "$root/bin/lib/repr-go-host.bash"
chmod +x "$root/bin/repr-go"

run() {  # run: make_overlay in the fabricated tree; prints its stdout
  ( set -euo pipefail
    cd "$root"; goroot=$T/goroot; work=$T/work; root=$root
    export REPR_GO_EXE=$T/host/arbace
    . "$T/funcs.bash"
    make_overlay 2>/dev/null )
}
prints() { [ -f "$T/prints" ] && wc -l < "$T/prints" | tr -d ' ' || echo 0; }
# expect N: the cumulative number of printer runs (2 per printing: runtime and reflect)
expect() {  # expect N WHAT
  local got; got=$(prints)
  if [ "$got" = "$1" ]; then ok "$2 (printer runs: $got)"; else bad "$2: printer runs $got, expected $1"; fi
}
past() { touch -d '2001-01-01' "$@"; }  # a change older than the overlay printed

run > "$T/out" || bad "first make_overlay failed"
[ -f "$T/work/overlay/overlay.json" ] && grep -q '/reflect/\|/src/reflect/' "$T/work/overlay/overlay.json" \
  && ok "the overlay file names reflect" || bad "overlay.json missing or without reflect"
expect 2 "first call prints"
run > /dev/null; expect 2 "second call reuses"
touch "$root/overlay/go/runtime.ae"; run > /dev/null; expect 2 "a touch with the same bytes reuses"

echo "(ns overlay.runtime) ; v2" > "$root/overlay/go/runtime.ae"; past "$root/overlay/go/runtime.ae"
run > /dev/null; expect 4 "overlay/go changed, older than the print: printed again"
run > /dev/null; expect 4 "and then reused"

echo ';; printer v2' > "$root/arbace/repr/go/print.ae"; past "$root/arbace/repr/go/print.ae"
run > /dev/null; expect 6 "a printer source changed (older mtime): printed again"

echo 'host v2 (an older host)' > "$T/host/arbace"; past "$T/host/arbace"
run > /dev/null; expect 8 "the printing host's bytes changed: printed again"

echo go1.27.2 > "$T/goroot/VERSION"; past "$T/goroot/VERSION"
run > /dev/null; expect 10 "the toolchain's VERSION changed: printed again"

rm -f "$T/work/overlay/overlay.key"
run > /dev/null; expect 12 "no key beside the overlay (an interrupted print): printed again"
run > /dev/null; expect 12 "and then reused"
ls "$T/work/overlay" | grep -q '\.new$' && bad "a temporary file left" || ok "no temporary file left"

[ "$fail" = 0 ] && echo "PASS overlay-key-test" || echo "FAIL overlay-key-test"
exit "$fail"
