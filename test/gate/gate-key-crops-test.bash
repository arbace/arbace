#!/bin/bash
# gate_key and gate_rest_key leave out the crops' files no build reads (queue 111, 054's finding 9): bin/arbace-go's
# gate_key and gate_rest_key and bin/lib/version.bash's arbace_embedded_sources, extracted and run alone on a
# fabricated checkout (one file at each path below), sha256sum, xargs and cut stubbed so the key's input is printed:
# the crops' check scripts (bin/lib/ci, oi, repr-cpp), their Go forms (go/arbace/{oi,scm}; ci, cinterp and crt are read since C9: the
# build copies them) and OCaml's
# runtime forms (jdk/ocaml) are absent, and what the builds read is present (the embedded arbace.o2j, arbace/http.ae,
# arbace/tls, goi, jrt, repr/go's Go forms, jdk/forms, jdk/data, bin/lib/tls-checks.bash, ...). Then the audit's
# identity of the hosts :gate and :prog (test/gate/audit.ae) names the same exclusions. No executable.
# C9: ci's namespaces (arbace/c, arbace/repr/c) and arbace.lang.CInterp are embedded, so the key hashes them.
# By hand: test/gate/gate-key-crops-test.bash [ARBACE_GO [VERSION_BASH [AUDIT_AE]]]   (older copies to test)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
ago=${1:-$src/bin/arbace-go}
ver=${2:-$src/bin/lib/version.bash}
aud=${3:-$src/test/gate/audit.ae}
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
d=$(mktemp -d "$TMPDIR/gate-key-crops-test.XXXXXX"); trap 'rm -rf "$d"' EXIT
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }
for f in gate_key gate_rest_key; do sed -n "/^$f() {/,/^}/p" "$ago"; done > "$d/fns.bash"
sed -n '/^arbace_embedded_sources() {/,/^}/p' "$ver" >> "$d/fns.bash"
grep -q '^gate_rest_key() {' "$d/fns.bash" && grep -q '^arbace_embedded_sources() {' "$d/fns.bash" \
  || { echo "FAIL: gate_key, gate_rest_key or arbace_embedded_sources not found"; exit 1; }

excluded="bin/lib/ci/check bin/lib/oi/suite.bash bin/lib/repr-cpp/roundtrip
go/arbace/oi.ae go/arbace/oi/apply.ae go/arbace/scm.ae go/arbace/scm/eval.ae go/arbace/libm.ae go/arbace/libm/libm.ae go/arbace/expat.ae go/arbace/expat/xmlparse_doc.ae go/arbace/pyexpat.ae go/arbace/pyexpat/pyexpat.ae jdk/ocaml/ocaml/rt.ae jdk/ocaml/ocaml/rt/NInt.ae
arbace/repr/cpp.ae arbace/repr/cpp/convert.ae arbace/repr/llvm.ae arbace/cpp/lower.ae
arbace/repr/ocaml.ae arbace/repr/scheme.ae arbace/scheme/aot.ae arbace/repr/python.ae arbace/repr/python/print.ae
arbace/python.ae arbace/python/scope.ae go/arbace/py.ae go/arbace/py/frame.ae"
kept="go/arbace/ci.ae go/arbace/ci/expr.ae go/arbace/cinterp.ae go/arbace/cinterp/natives.ae go/arbace/crt.ae go/arbace/crt/mem.ae
arbace/c.ae arbace/c/build.ae arbace/c/cinterp.ae arbace/repr/c.ae arbace/repr/c/lex.ae arbace/lang/CInterp.ae
arbace/core.ae arbace/o2j.ae arbace/http.ae arbace/tls/x.ae arbace/go/check.ae arbace/repr/go.ae
go/arbace.ae go/arbace/api.ae go/arbace/goi.ae go/arbace/goi/x.ae go/arbace/jrt/x.ae go/arbace/repr/go/godump.ae
go/arbace/gotable.edn go/arbace/cmd/arbace/image.ae go/arbace/cimore/x.ae
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
for f in bin/lib/ci/check bin/lib/oi/suite.bash bin/lib/repr-cpp/roundtrip; do
  if echo "$rk" | grep -qxF "$f"; then bad "gate_rest_key hashes $f"; else ok "gate_rest_key leaves out $f"; fi
done
for f in bin/lib/tls-checks.bash bin/lib/cimore/x overlay/go/runtime.ae x509roots/x; do
  if echo "$rk" | grep -qxF "$f"; then ok "gate_rest_key hashes $f"; else bad "gate_rest_key leaves out $f"; fi
done

# the audit's identities: :gate's and :prog's vectors (up to the next host) name the same exclusions
host_block() { awk -v h="$1" '$0 ~ "^ *(\\{)?:" h " " {f=1} f && $0 ~ /^ *:[a-z-]+ / && $0 !~ "^ *(\\{)?:" h " " {exit} f' "$aud"; }
gb=$(host_block gate); pb=$(host_block prog)
[ -n "$gb" ] && [ -n "$pb" ] || { echo "FAIL: no :gate or :prog in $aud"; exit 1; }
for p in '!bin/lib/ci/' '!bin/lib/oi/' '!bin/lib/repr-cpp/' '!jdk/ocaml/' '!go/arbace/oi.ae' \
         '!go/arbace/oi/' '!go/arbace/scm.ae' '!go/arbace/scm/' '!go/arbace/py.ae' '!go/arbace/py/' \
         '!go/arbace/libm.ae' '!go/arbace/libm/' '!go/arbace/expat.ae' '!go/arbace/expat/' '!go/arbace/pyexpat.ae' '!go/arbace/pyexpat/'; do
  if echo "$gb" | grep -qF "\"$p\""; then ok "audit :gate names $p"; else bad "audit :gate does not name $p"; fi
done
for p in '!bin/lib/ci/' '!bin/lib/oi/' '!bin/lib/repr-cpp/'; do
  if echo "$pb" | grep -qF "\"$p\""; then ok "audit :prog names $p"; else bad "audit :prog does not name $p"; fi
done
[ $fails = 0 ] && { echo "PASS: gate-key-crops-test"; exit 0; }
echo "FAIL: gate-key-crops-test, $fails failures"; exit 1
