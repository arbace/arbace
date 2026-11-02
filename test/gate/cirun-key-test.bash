#!/bin/bash
# queue 138, finding 4: ci's light back end (.tmp/ci/cirun, bin/ci cirun-build) is keyed by its sources (.tmp/ci/key); its
# callers' `bin/ci cirun-build --ensure` keeps it while the key is the checkout's and builds it again when go/arbace/ci,
# go/arbace/crt, tools/cirun or repr/go's printer changed (before, a cirun once built was never rebuilt: a back end change
# passed on the old one). The script runs on a fabricated checkout (bin/repr-go and TamaGo's go stubbed; the stub go counts
# its builds). No executable, seconds.
# By hand: test/gate/cirun-key-test.bash [CHECKOUT]   (another checkout's bin/lib/ci/cirun-build to test)
set -uo pipefail
src=$(cd "${1:-$(dirname "$0")/../..}" && pwd)
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$(mktemp -d "$TMPDIR/cirun-key-test.XXXXXX"); trap 'rm -rf "$t"' EXIT
mkdir -p "$t/bin/lib/ci" "$t/go/arbace/ci" "$t/go/arbace/crt" "$t/tools/cirun" "$t/arbace/repr/go" "$t/goroot/bin"
cp "$src/bin/lib/ci/cirun-build" "$t/bin/lib/ci/"; cp "$src/bin/lib/tmpdir.bash" "$t/bin/lib/"
: > "$t/bin/lib/repr-go-host.bash"
printf '#!/bin/bash\n# print ... FILE DIR\nfor a; do :; done; echo "package x" > "$a/x.go"\n' > "$t/bin/repr-go"; chmod +x "$t/bin/repr-go"
printf '#!/bin/bash\necho build >> %s/builds\nwhile [ $# -gt 0 ]; do [ "$1" = -o ] && { printf "#!/bin/bash\\n" > "$2"; chmod +x "$2"; }; shift; done\n' "$t" > "$t/goroot/bin/go"
chmod +x "$t/goroot/bin/go"; echo go1.27.1 > "$t/goroot/VERSION"
for f in go/arbace/ci.ae go/arbace/ci/a.ae go/arbace/crt.ae go/arbace/crt/b.ae tools/cirun/main.go arbace/repr/go.ae arbace/repr/go/print.ae; do
  echo "; $f" > "$t/$f"
done
fails=0
ensure() { (cd "$t" && ARBACE_GOROOT=$t/goroot bash bin/lib/ci/cirun-build --ensure > "$t/out" 2>&1); }
builds() { wc -l < "$t/builds" 2>"$TMPDIR/cirun-key-test.err" | tr -d ' ' || echo 0; }
check() { # WHAT WANT-BUILDS
  if [ "$(builds)" = "$2" ]; then echo "ok:   $1 ($2 builds)"
  else echo "FAIL: $1: $(builds) builds, want $2"; cat "$t/out"; fails=$((fails + 1)); fi
}
ensure; check "the first --ensure builds" 1
[ -s "$t/.tmp/ci/key" ] || { echo "FAIL: no .tmp/ci/key written"; fails=$((fails + 1)); }
ensure; check "a second --ensure keeps it" 1
echo "; changed" >> "$t/go/arbace/ci/a.ae"; ensure; check "a change to go/arbace/ci/ rebuilds" 2
echo "; changed" >> "$t/go/arbace/crt.ae"; ensure; check "a change to go/arbace/crt.ae rebuilds" 3
echo "// changed" >> "$t/tools/cirun/main.go"; ensure; check "a change to tools/cirun rebuilds" 4
echo "; changed" >> "$t/arbace/repr/go/print.ae"; ensure; check "a change to repr/go's printer rebuilds" 5
ensure; check "unchanged again: kept" 5
rm -rf "$t/.tmp/ci/go"; ensure; check "a missing printed tree rebuilds" 6
[ "$fails" = 0 ] && { echo "PASS: cirun-key-test"; exit 0; }
echo "FAIL: cirun-key-test ($fails)"; exit 1
