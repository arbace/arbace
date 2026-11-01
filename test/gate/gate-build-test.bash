#!/bin/bash
# bin/arbace-go --gate-build, the real script (not an extracted function) under its own set -uo pipefail,
# in a fabricated checkout under .tmp/gate-build-test with a fake TamaGo directory: a hit at the key
# (also --check-cached) succeeds; a miss never removes the directory of another key, javac's tool's or the
# executables' root (GTS121); a failed build leaves the directory it was to replace (GTS126). No build.
# Usage: test/gate/gate-build-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=$here/.tmp/tmp
mkdir -p "$TMPDIR"
T=$here/.tmp/gate-build-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }

r=$T/tree
mkdir -p "$r/bin" "$r/seed" "$r/arbace" "$r/go" "$r/overlay" "$r/jdk" "$r/x509roots" "$r/test/java-fixtures" \
  "$r/tamago/goroot" "$r/tools/godump" "$T/goroot/bin" "$r/.tmp"
cp -r "$here/bin/lib" "$r/bin/lib"
cp "$here/bin/arbace-go" "$r/bin/"
for f in j2g repr-go build-go jrt seed goi; do : > "$r/bin/$f"; done
: > "$r/seed/arbace-seed-go.tar.xz.sha256"; : > "$r/seed/goroot.sha256"
echo go1.27.1 > "$T/goroot/VERSION"
export ARBACE_GOROOT=$T/goroot ARBACE_GATE_EXES=$T/exes
unset ARBACE_GATE_CACHE ARBACE_HOST_EXE
mkdir -p "$ARBACE_GATE_EXES"
KEY=$("$r/bin/arbace-go" --gate-key 2>"$T/key.err")
[ ${#KEY} = 64 ] && ok "--gate-key prints a 64-character key" || { bad "no key: $KEY $(cat "$T/key.err")"; exit 1; }
K20=${KEY:0:20}
mkdir_exe() { mkdir -p "$1/amd64"; printf '#!/bin/bash\n' > "$1/amd64/arbace"; cp "$1/amd64/arbace" "$1/amd64/arbace-tests"; chmod +x "$1/amd64/arbace" "$1/amd64/arbace-tests"; echo "$2" > "$1/key"; }
run() { (cd "$r" && "$r/bin/arbace-go" --gate-build "$@" > "$T/out" 2>&1; echo $? > "$T/status"); }
status() { cat "$T/status"; }

# a hit at the checkout's key, as gate_env exports it and without
mkdir_exe "$ARBACE_GATE_EXES/$K20" "$KEY"
for env in "" "ARBACE_GATE_CACHE=$ARBACE_GATE_EXES/$K20"; do
  for opt in "" --check-cached; do
    ( [ -z "$env" ] || export $env; run $opt )
    if [ "$(status)" = 0 ] && grep -q "the cached executables" "$T/out"; then ok "a hit (${env:-default} ${opt:-})"
    else bad "a hit (${env:-default} ${opt:-}): status $(status): $(head -3 "$T/out")"; fi
  done
done

# a miss never removes another directory
OTHER=ffffffffffffffffffff
mkdir_exe "$ARBACE_GATE_EXES/$OTHER" "$OTHER"
mkdir_exe "$ARBACE_GATE_EXES/javac-$OTHER" "$OTHER"
for c in "$ARBACE_GATE_EXES/$OTHER" "$ARBACE_GATE_EXES/javac-$OTHER" "$ARBACE_GATE_EXES"; do
  ( export ARBACE_GATE_CACHE=$c; run )
  if [ "$(status)" != 0 ] && grep -q "not removed" "$T/out" && { [ -x "$c/amd64/arbace" ] || [ -x "$c/$K20/amd64/arbace" ]; }; then ok "a miss refused and kept $c"
  else bad "a miss removed or accepted $c (status $(status))"; fi
done

# a failed build leaves the directory it was to replace (the key file is stale), no half-made one beside it
echo stale > "$ARBACE_GATE_EXES/$K20/key"
run
if [ "$(status)" != 0 ] && grep -q "building the executables" "$T/out" && [ -x "$ARBACE_GATE_EXES/$K20/amd64/arbace" ] && ! ls "$ARBACE_GATE_EXES" | grep -q '\.tmp\.'; then ok "a failed build kept the old directory"
else bad "a failed build: status $(status), $(ls "$ARBACE_GATE_EXES" | tr '\n' ' ')"; fi

# a successful miss puts the new directory in place whole (GTS126): while it is made, the directory still
# holds the old one. The builds are stubbed in the tree's copy of the script (functions defined before its
# dispatch); gate_build is the real one. arbace_version is called after the copying and before the swap.
sed -i '/^case \${1:-} in$/i\
build() { [ -z "${BUILD_LOG:-}" ] || echo b >> "$BUILD_LOG"; sleep "${BUILD_SLEEP:-0}"; mkdir -p "$dir/amd64/x" "$dir/aot" "$dir/j2g" "$dir/j2g-tests"; [ -z "${STUB_NO_AOT:-}" ] || rm -rf "$dir/aot"; echo new > "$dir/amd64/arbace"; echo new > "$dir/amd64/arbace-tests"; chmod +x "$dir/amd64/arbace" "$dir/amd64/arbace-tests"; : > "$dir/aot.key"; : > "$dir/aot.runtime"; }\
build_tests() { :; }\
arbace_version() { ls "$cache/amd64" > "'"$T"'/during.ls" 2>&1; ls "$cache" >> "'"$T"'/during.ls" 2>&1; echo version; }' "$r/bin/arbace-go"
KEY=$("$r/bin/arbace-go" --gate-key 2>"$T/key.err"); K20=${KEY:0:20} # the script is another file now
mkdir_exe "$ARBACE_GATE_EXES/$K20" stale
echo old > "$ARBACE_GATE_EXES/$K20/amd64/old-marker"; echo stale > "$ARBACE_GATE_EXES/$K20/key"
run
if [ "$(status)" = 0 ] && grep -q old-marker "$T/during.ls" && ! grep -q version.edn "$T/during.ls" \
   && [ "$(cat "$ARBACE_GATE_EXES/$K20/key")" = "$KEY" ] && [ ! -e "$ARBACE_GATE_EXES/$K20/amd64/old-marker" ] \
   && ! ls "$ARBACE_GATE_EXES" | grep -q '\.\(tmp\|old\)\.'; then ok "a miss swapped the whole directory in"
else bad "a miss: status $(status); during: $(tr '\n' ' ' < "$T/during.ls"); $(tail -3 "$T/out")"; fi

# a fresh host: the executables' root does not exist yet (review 048, N1); the builds are the stubs above
( export ARBACE_GATE_EXES=$T/fresh-exes; run )
if [ "$(status)" = 0 ] && [ "$(cat "$T/fresh-exes/$K20/key" 2>&1)" = "$KEY" ]; then ok "a miss on a fresh host (no executables' root) built"
else bad "a miss on a fresh host: status $(status): $(head -2 "$T/out")"; fi

# an override naming a directory the gate did not make is refused and kept, however it is spelled (review 048, N3)
mkdir -p "$T/precious/sub" "$T/exes-parent/exes"; echo data > "$T/precious/file"; echo data > "$T/exes-parent/other"
for c in "$T/precious" "$T/exes-parent" "$T/precious/" "$T/exes-parent/exes/.."; do
  ( export ARBACE_GATE_CACHE=$c; run )
  if [ "$(status)" != 0 ] && grep -q "not removed" "$T/out" && [ -f "$T/precious/file" ] && [ -f "$T/exes-parent/other" ]; then ok "an override refused and kept: $c"
  else bad "an override accepted: $c (status $(status))"; fi
done

# two concurrent misses of one key: both succeed, one build, nothing left behind (review 048, N2)
mkdir_exe "$ARBACE_GATE_EXES/$K20" stale
rm -f "$T/builds"; leftovers() { ls -A "$ARBACE_GATE_EXES" | grep '\.\(tmp\|old\)\.' | tr '\n' ' '; }
for n in 1 2 3; do
  mkdir_exe "$ARBACE_GATE_EXES/$K20" stale; rm -f "$T/builds"
  ( cd "$r" && BUILD_LOG=$T/builds BUILD_SLEEP=0.4 "$r/bin/arbace-go" --gate-build > "$T/c1.out" 2>&1; echo $? > "$T/c1.status" ) &
  ( cd "$r" && BUILD_LOG=$T/builds BUILD_SLEEP=0.4 "$r/bin/arbace-go" --gate-build > "$T/c2.out" 2>&1; echo $? > "$T/c2.status" ) &
  wait
  if [ "$(cat "$T/c1.status") $(cat "$T/c2.status")" = "0 0" ] && [ "$(wc -l < "$T/builds")" = 1 ] && [ -z "$(leftovers)" ] \
     && [ "$(cat "$ARBACE_GATE_EXES/$K20/key")" = "$KEY" ]; then ok "two concurrent misses, round $n: both 0, one build, no leftovers"
  else bad "two concurrent misses, round $n: $(cat "$T/c1.status") $(cat "$T/c2.status"), builds $(wc -l < "$T/builds"), left: $(leftovers)"; fi
done

# a copy that fails swaps nothing in and leaves nothing (review 048, N4): the build made no aot/
mkdir_exe "$ARBACE_GATE_EXES/$K20" stale; echo old > "$ARBACE_GATE_EXES/$K20/amd64/old-marker"
( export STUB_NO_AOT=1; run )
if [ "$(status)" != 0 ] && [ -f "$ARBACE_GATE_EXES/$K20/amd64/old-marker" ] && [ "$(cat "$ARBACE_GATE_EXES/$K20/key")" = stale ] && [ -z "$(leftovers)" ]; then ok "a failed copy kept the old directory"
else bad "a failed copy: status $(status), key $(cat "$ARBACE_GATE_EXES/$K20/key"), left: $(leftovers)"; fi

# the old directory that cannot be removed does not fail a swap that was made (review 048, N6)
mkdir -p "$T/shim"; printf '#!/bin/bash\ncase "$*" in *.old.*) exit 1 ;; esac\nexec /bin/rm "$@"\n' > "$T/shim/rm"; chmod +x "$T/shim/rm"
mkdir_exe "$ARBACE_GATE_EXES/$K20" stale
( export PATH=$T/shim:$PATH; run )
if [ "$(status)" = 0 ] && [ "$(cat "$ARBACE_GATE_EXES/$K20/key")" = "$KEY" ] && grep -q "could not remove" "$T/out"; then ok "an old directory that stays does not fail the swap"
else bad "an unremovable old directory: status $(status): $(tail -3 "$T/out")"; fi
rm -rf "$ARBACE_GATE_EXES"/*.old.*

# a directory holding some other tool's file named key is not a gate build (review 051, A)
mkdir -p "$T/work-a" "$T/work-b/amd64"; echo not-a-gate-key > "$T/work-a/key"; echo data > "$T/work-a/disk"
echo "$KEY" > "$T/work-b/key"; echo data > "$T/work-b/disk"   # a key, but no version.edn nor amd64/ of a build
for c in "$T/work-a" "$T/work-b"; do
  ( export ARBACE_GATE_CACHE=$c; run )
  if [ "$(status)" != 0 ] && grep -q "not removed" "$T/out" && [ -f "$c/disk" ]; then ok "a directory with a stray key file refused and kept: $c"
  else bad "a directory with a stray key file accepted: $c (status $(status))"; fi
done
# ... while a directory of a build, under another name, is still replaced
mkdir_exe "$T/legacy" "$(printf f%.0s $(seq 64))"; echo v > "$T/legacy/version.edn"; touch "$T/legacy/amd64/old-marker"
( export ARBACE_GATE_CACHE=$T/legacy; run )
if [ "$(status)" = 0 ] && [ ! -e "$T/legacy/amd64/old-marker" ] && [ "$(cat "$T/legacy/key")" = "$KEY" ]; then ok "a build's directory under another name replaced"
else bad "a legacy build directory: status $(status): $(tail -2 "$T/out")"; fi

exit $fail
