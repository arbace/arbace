#!/bin/bash
# needs: exe
# cpp-key (2026-11-25): the gate's cpp-runtime (bin/cppi runtime-ci) builds the lowered runtime only when OUT is not at the
# checkout's key, and runtime_key is closed by OUT's load closure (OUT/loaded, made by rt_closure from RT.load's log). On a
# fabricated OUT (no build): the scriptbases of a log as files, a namespace the host embeds and does not reload (arbace.string)
# as `embedded  FILE`, a reloaded one (arbace/cpp/...) and a namespace of the checkout alone hashed; OUT at the key it was written
# at is current; a change to a hashed file of its closure makes it stale and names the file; a change to an `embedded` file and
# to a file outside the closure does not; a missing closure (a runtime built before cpp-key) is stale. The gate step's command is
# runtime-ci alone (no unconditional runtime-build). Files changed: under the test's own directory in .tmp only.
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
d=$(mktemp -d "$src/.tmp/cpp-runtime-key-test.XXXXXX"); trap 'rm -rf "$d"' EXIT
rel=${d#"$src"/}
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }

# a namespace of the checkout alone (no host embeds it): RT.load's scriptbase, relative to the checkout
mkdir -p "$d/ns"; echo '(ns demo)' > "$d/ns/demo.ae"
printf '%s\n' arbace/string arbace/cpp/abi "$rel/ns/demo" arbace/string > "$d/loaded.raw"
eval "$(sed -n '/^rt_closure() {/,/^}/p; /^rt_closure_lines() {/,/^}/p' "$src/bin/cppi")"
root=$src
rt_closure "$d/loaded.raw" "$d/loaded" 2>"$d/closure.err"
want=$(printf '%s\n' "$rel/ns/demo.ae" arbace/cpp/abi.ae "embedded  arbace/string.ae" | LC_ALL=C sort)
if [ "$(cat "$d/loaded")" = "$want" ]; then ok "rt_closure: files, the core embedded, a tool's namespace and the checkout's hashed"
else bad "rt_closure: $(tr '\n' ' ' < "$d/loaded") ($(cat "$d/closure.err"))"; fi

out=$d/out; mkdir -p "$out"; cp "$d/loaded" "$out/loaded"
"$src/bin/cppi" runtime-key --print "$out" > "$out/key.records.txt" 2>"$d/print.err" || bad "runtime-key --print: $(tail -1 "$d/print.err")"
sha256sum < "$out/key.records.txt" | cut -d' ' -f1 > "$out/key.records"
grep -q "  $rel/ns/demo.ae\$" "$out/key.records.txt" && grep -q '^embedded  arbace/string.ae$' "$out/key.records.txt" \
  && ok "the key's lines name the closure" || bad "the key's lines lack the closure"
st() { "$src/bin/cppi" runtime-key "$out" 2>"$d/st.err"; }
[ "$(st)" = current ] && ok "OUT at its key: current" || bad "OUT at its key: $(st) $(cat "$d/st.err")"
# the closure's checkout file changed: stale, named
echo ';; changed' >> "$d/ns/demo.ae"
if [ "$(st)" = stale ] && grep -q "differs: $rel/ns/demo.ae" "$d/st.err"; then ok "a closure file changed: stale, named"
else bad "a closure file changed: $(st) $(cat "$d/st.err")"; fi
echo '(ns demo)' > "$d/ns/demo.ae"
[ "$(st)" = current ] && ok "the change undone: current again" || bad "the change undone: $(st)"
# an embedded entry is the host's (its digest keys it): a checkout file of that name is not read
printf '%s\n' arbace/cpp/abi.ae "embedded  $rel/ns/demo.ae" > "$out/loaded"
"$src/bin/cppi" runtime-key --print "$out" > "$out/key.records.txt"; sha256sum < "$out/key.records.txt" | cut -d' ' -f1 > "$out/key.records"
echo ';; changed' >> "$d/ns/demo.ae"
[ "$(st)" = current ] && ok "an embedded entry's checkout file changed: current" || bad "an embedded entry: $(st) $(cat "$d/st.err")"
# no closure recorded: stale
rm -f "$out/loaded"
[ "$(st)" = stale ] && ok "no closure recorded: stale" || bad "no closure recorded: $(st)"
# the edited-during-the-build line: a closure file newer than the build's start key
printf '%s\n' "$rel/ns/demo.ae" > "$d/l2"; touch -d '2000-01-01' "$d/since"
rt_closure_lines "$d/l2" "$d/since" | grep -q '^edited during the build  ' && ok "a file edited during the build: a line no checkout matches" \
  || bad "a file edited during the build: $(rt_closure_lines "$d/l2" "$d/since")"
# the gate's step: runtime-ci alone
for f in test/gate/steps.edn test/gate/crops/cpp.edn; do
  c=$(awk '/:name cpp-runtime :mem-gib/ { on = 1 } on && /:cmd / { print; exit }' "$src/$f")
  case $c in *'"bin/cppi runtime-ci"'*) ok "$f: cpp-runtime runs runtime-ci alone" ;; *) bad "$f: cpp-runtime's command is $c" ;; esac
done
[ "$fails" = 0 ] && echo "cpp-runtime-key-test: ok" || { echo "cpp-runtime-key-test: $fails failures"; exit 1; }
