#!/bin/bash
# gate_key and gate_rest_key leave out the crops' files no build reads (queue 111, 054's finding 9): bin/arbace-go's
# gate_key and gate_rest_key and bin/lib/version.bash's arbace_embedded_sources, extracted and run alone on a
# fabricated checkout (one file at each path below), sha256sum, xargs and cut stubbed so the key's input is printed:
# the crops' check scripts (bin/lib/ci, oi, repr-cpp, crops), their Go forms (go/arbace/{oi,scm}; ci, cinterp and crt are read since C9: the
# build copies them) and OCaml's
# runtime forms (jdk/ocaml) are absent, and what the builds read is present (the embedded arbace.o2j, arbace/http.ae,
# arbace/tls, goi, jrt, repr/go's Go forms, jdk/forms, jdk/data, bin/lib/tls-checks.bash, ...). Then the audit's
# identity of the hosts :gate and :prog (test/gate/audit.ae) names the same exclusions. No executable.
# C9: ci's namespaces (arbace/c, arbace/repr/c) and arbace.lang.CInterp are embedded, so the key hashes them.
# The re-seed after the backlog (the user, 2026-11-03): bin/lib/version.bash's arbace_runtime_key and
# bin/lib/javac-key.bash's arbace_javac_key leave out the crops' Go forms too (go/arbace/{oi,scm,scminterp,py,libm}),
# run for real (sha256sum) on the fabricated checkout: a change under each moves neither key, a change to jrt's, ci's
# or cinterp's Go forms moves both; the audit's :javac host names the same exclusions.
# By hand: test/gate/gate-key-crops-test.bash [ARBACE_GO [VERSION_BASH [AUDIT_AE [JAVAC_KEY_BASH]]]]   (older copies to test)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
ago=${1:-$src/bin/arbace-go}
ver=${2:-$src/bin/lib/version.bash}
aud=${3:-$src/test/gate/audit.ae}
jkb=${4:-$src/bin/lib/javac-key.bash}
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
d=$(mktemp -d "$TMPDIR/gate-key-crops-test.XXXXXX"); trap 'rm -rf "$d"' EXIT
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }
for f in gate_key gate_rest_key; do sed -n "/^$f() {/,/^}/p" "$ago"; done > "$d/fns.bash"
sed -n '/^arbace_embedded_sources() {/,/^}/p' "$ver" >> "$d/fns.bash"
grep -q '^gate_rest_key() {' "$d/fns.bash" && grep -q '^arbace_embedded_sources() {' "$d/fns.bash" \
  || { echo "FAIL: gate_key, gate_rest_key or arbace_embedded_sources not found"; exit 1; }

excluded="bin/lib/ci/check bin/lib/oi/suite.bash bin/lib/repr-cpp/roundtrip bin/lib/crops/c2j-check
go/arbace/oi.ae go/arbace/oi/apply.ae go/arbace/scm.ae go/arbace/scm/eval.ae go/arbace/libm.ae go/arbace/libm/libm.ae jdk/ocaml/ocaml/rt.ae jdk/ocaml/ocaml/rt/NInt.ae
arbace/repr/cpp.ae arbace/repr/cpp/convert.ae arbace/repr/llvm.ae arbace/cpp/lower.ae
arbace/repr/ocaml.ae arbace/repr/scheme.ae arbace/scheme/aot.ae arbace/repr/python.ae arbace/repr/python/print.ae
arbace/python.ae arbace/python/scope.ae go/arbace/py.ae go/arbace/py/frame.ae"
kept="go/arbace/ci.ae go/arbace/ci/expr.ae go/arbace/cinterp.ae go/arbace/cinterp/natives.ae go/arbace/crt.ae go/arbace/crt/mem.ae
arbace/c.ae arbace/c/build.ae arbace/c/cinterp.ae arbace/repr/c.ae arbace/repr/c/lex.ae arbace/lang/CInterp.ae
arbace/core.ae arbace/o2j.ae arbace/http.ae arbace/tls/x.ae arbace/go/check.ae arbace/repr/go.ae
go/arbace.ae go/arbace/api.ae go/arbace/goi.ae go/arbace/goi/x.ae go/arbace/jrt/x.ae go/arbace/repr/go/godump.ae
go/arbace/gotable.edn go/arbace/cmd/arbace/image.ae go/arbace/cimore/x.ae arbace/lang/RT.ae
jdk/forms/java.base/x.ae jdk/data/x jdk/edge.edn jdk/sources.txt overlay/go/runtime.ae overlay/jdk/variants/x.ae
x509roots/x bin/lib/tls-checks.bash bin/lib/version.bash bin/lib/repr-go/x bin/lib/cimore/x test/java-fixtures/x.ae
tamago/goroot/x.ae tools/godump/main.go"
r=$d/root
for f in $excluded $kept; do mkdir -p "$r/$(dirname "$f")"; echo x > "$r/$f"; done
mkdir -p "$d/goroot"; echo go-fake > "$d/goroot/VERSION"

# listed FN: the files FN's key hashes (its input, stubbed)
listed() {
  env -i PATH="$PATH" ARBACE_GOROOT="$d/goroot" bash -c '
    root=$1; . "$2"
    xargs() { cat; }; sha256sum() { cat; }; cut() { cat; }
    "$3"' _ "$r" "$d/fns.bash" "$1"
}
gk=$(listed gate_key); rk=$(listed gate_rest_key)
echo "$gk" | grep -qx go-fake && echo "$gk" | grep -qx arbace/core.ae || { echo "FAIL: gate_key's stubbed run printed no list"; echo "$gk" | head; exit 1; }
for f in $excluded; do
  if echo "$gk" | grep -qxF "$f"; then bad "gate_key hashes $f"; else ok "gate_key leaves out $f"; fi
done
for f in $kept; do
  if echo "$gk" | grep -qxF "$f"; then ok "gate_key hashes $f"; else bad "gate_key leaves out $f"; fi
done
for f in bin/lib/ci/check bin/lib/oi/suite.bash bin/lib/repr-cpp/roundtrip bin/lib/crops/c2j-check; do
  if echo "$rk" | grep -qxF "$f"; then bad "gate_rest_key hashes $f"; else ok "gate_rest_key leaves out $f"; fi
done
for f in bin/lib/tls-checks.bash bin/lib/cimore/x overlay/go/runtime.ae x509roots/x; do
  if echo "$rk" | grep -qxF "$f"; then ok "gate_rest_key hashes $f"; else bad "gate_rest_key leaves out $f"; fi
done

# the audit's identities: :gate's and :prog's vectors (up to the next host) name the same exclusions
host_block() { awk -v h="$1" '$0 ~ "^ *(\\{)?:" h " " {f=1} f && $0 ~ /^ *:[a-z-]+ / && $0 !~ "^ *(\\{)?:" h " " {exit} f' "$aud"; }
gb=$(host_block gate); pb=$(host_block prog)
[ -n "$gb" ] && [ -n "$pb" ] || { echo "FAIL: no :gate or :prog in $aud"; exit 1; }
for p in '!bin/lib/ci/' '!bin/lib/oi/' '!bin/lib/repr-cpp/' '!bin/lib/crops/' '!jdk/ocaml/' '!go/arbace/oi.ae' \
         '!go/arbace/oi/' '!go/arbace/scm.ae' '!go/arbace/scm/' '!go/arbace/py.ae' '!go/arbace/py/' \
         '!go/arbace/libm.ae' '!go/arbace/libm/'; do
  if echo "$gb" | grep -qF "\"$p\""; then ok "audit :gate names $p"; else bad "audit :gate does not name $p"; fi
done
for p in '!bin/lib/ci/' '!bin/lib/oi/' '!bin/lib/repr-cpp/' '!bin/lib/crops/'; do
  if echo "$pb" | grep -qF "\"$p\""; then ok "audit :prog names $p"; else bad "audit :prog does not name $p"; fi
done
# the runtime's and javac's tool's keys (the re-seed after the backlog, the user 2026-11-03): computed for real on the
# fabricated checkout, before and after a change to one file; the crops' Go forms (scminterp: the embedding's gaps,
# not on main yet, fabricated here) move neither, jrt's, ci's and cinterp's move both
sed -n '/^arbace_runtime_key() {/,/^}/p' "$ver" > "$d/keys.bash"
sed -n '/^arbace_javac_key() {/,/^}/p' "$jkb" >> "$d/keys.bash"
grep -q '^arbace_runtime_key() {' "$d/keys.bash" && grep -q '^arbace_javac_key() {' "$d/keys.bash" \
  || { echo "FAIL: arbace_runtime_key or arbace_javac_key not found"; exit 1; }
crop_go="go/arbace/oi.ae go/arbace/oi/apply.ae go/arbace/scm.ae go/arbace/scm/eval.ae go/arbace/scminterp.ae
go/arbace/scminterp/natives.ae go/arbace/py.ae go/arbace/py/frame.ae go/arbace/libm.ae go/arbace/libm/libm.ae"
for f in go/arbace/scminterp.ae go/arbace/scminterp/natives.ae seed/arbace-seed-go.tar.xz.sha256 seed/goroot.sha256 \
         bin/arbace-go bin/j2g bin/repr-go bin/build-go bin/jrt bin/seed bin/goi bin/javac-vendor; do
  mkdir -p "$r/$(dirname "$f")"; echo x > "$r/$f"
done
keys() {
  env -i PATH="$PATH" ARBACE_GOROOT="$d/goroot" TMPDIR="$TMPDIR" bash -c '
    . "$2"; echo "runtime $(arbace_runtime_key "$1")"; echo "javac $(arbace_javac_key "$1")"' _ "$r" "$d/keys.bash"
}
k0=$(keys)
echo "$k0" | grep -qE '^runtime [0-9a-f]{64}$' && echo "$k0" | grep -qE '^javac [0-9a-f]{64}$' \
  || { echo "FAIL: the keys' run printed no keys"; echo "$k0"; exit 1; }
for f in $crop_go; do
  echo changed >> "$r/$f"
  k=$(keys)
  for n in runtime javac; do
    if [ "$(echo "$k" | grep "^$n ")" = "$(echo "$k0" | grep "^$n ")" ]; then ok "arbace_${n}_key unmoved by a change to $f"
    else bad "arbace_${n}_key moved by a change to $f"; fi
  done
done
for f in go/arbace/jrt/x.ae go/arbace/ci/expr.ae go/arbace/cinterp/natives.ae arbace/lang/RT.ae; do
  k1=$(keys); echo changed >> "$r/$f"; k=$(keys)
  for n in runtime javac; do
    if [ "$(echo "$k" | grep "^$n ")" != "$(echo "$k1" | grep "^$n ")" ]; then ok "arbace_${n}_key moved by a change to $f"
    else bad "arbace_${n}_key unmoved by a change to $f"; fi
  done
done
jb=$(host_block javac)
[ -n "$jb" ] || { echo "FAIL: no :javac in $aud"; exit 1; }
for p in '!go/arbace/oi.ae' '!go/arbace/oi/' '!go/arbace/scm.ae' '!go/arbace/scm/' '!go/arbace/scminterp.ae' \
         '!go/arbace/scminterp/' '!go/arbace/py.ae' '!go/arbace/py/' '!go/arbace/libm.ae' '!go/arbace/libm/'; do
  if echo "$jb" | grep -qF "\"$p\""; then ok "audit :javac names $p"; else bad "audit :javac does not name $p"; fi
done
[ $fails = 0 ] && { echo "PASS: gate-key-crops-test"; exit 0; }
echo "FAIL: gate-key-crops-test, $fails failures"; exit 1
