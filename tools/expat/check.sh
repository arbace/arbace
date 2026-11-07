#!/bin/bash
# tools/expat/check.sh [--pyi] [--mutants N]: the differential of go/arbace/expat (queue 202e), a sample.
#   1. Go against C: the pinned tarball's Modules/expat compiled by gcc (cdrv.c) and go/arbace/expat printed by repr/go
#      and built by TamaGo's go (gdrv/main.go), each document's events with their line, column and byte index and the
#      error's code and position, under 4 configurations (namespaces or not; whole, in 7-byte chunks, byte by byte;
#      the default handler or not), over gencorpus.py's corpus (the tarball's XML files, hand-written documents, 300
#      mutants) and N more seeded mutants (default 3000): "cases C, differing 0".
#   2. --pyi: pyi's pyexpat against the host's python3 (xmldiff.py: every handler's event and position, the errors,
#      external entity parsers, 5 configurations) over the same corpus: "pyi: L lines, differing 0". The host's
#      python3 may link another expat than the tarball's 2.9.0 (Alpine's 2.10.0 is equal on this corpus).
# Work in .tmp/expat-check. Environment as bin/pyi's (REPR_PYTHON_SRC, ARBACE_HOST_EXE); exit 1 on a difference.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
pyi="" mut=3000
while [ $# -gt 0 ]; do case $1 in --pyi) pyi=1; shift ;; --mutants) mut=$2; shift 2 ;; *) echo "usage: $0 [--pyi] [--mutants N]" >&2; exit 2 ;; esac; done
W=$root/.tmp/expat-check; mkdir -p "$W"
top=$root/.tmp/repr-python/Python-3.14.10
[ -d "$top/Modules/expat" ] || "$root/bin/repr-python" pins --fetch >/dev/null
E=$top/Modules/expat

# the C driver over the tarball's expat (its expat_config.h includes pyconfig.h: an empty one)
mkdir -p "$W/c"; : > "$W/c/pyconfig.h"
gcc -O1 -w -fsigned-char -I"$W/c" -I"$E" -DHAVE_EXPAT_CONFIG_H -o "$W/c/drv" "$root/tools/expat/cdrv.c" \
  "$E/xmlparse.c" "$E/xmlrole.c" "$E/xmltok.c" "$E/xcs.c"
# the Go driver over go/arbace/expat as repr/go prints it
rm -rf "$W/mod"; mkdir -p "$W/mod/expat" "$W/mod/cmd/gdrv"
printf 'module arbace\n\ngo 1.27\n' > "$W/mod/go.mod"
ARBACE_PATH="$root" "$root/bin/repr-go" print "$root/go/arbace/expat.ae" "$W/mod/expat" >/dev/null
cp "$root/tools/expat/gdrv/main.go" "$W/mod/cmd/gdrv/"
(cd "$W/mod" && env GOROOT= PATH="${ARBACE_GOROOT:-/root/tamago-go}/bin:$PATH" GOCACHE="$W/gocache" GOFLAGS=-mod=mod \
  CGO_ENABLED=0 GOTOOLCHAIN=local go build -o "$W/gdrv" ./cmd/gdrv)

rm -rf "$W/corpus" "$W/mutants"
python3 -I "$root/tools/expat/gencorpus.py" "$top" "$W/corpus"
python3 -I "$root/tools/expat/gencorpus.py" "$top" "$W/mutants" --mutants-only "$mut" --seed 7

rc=0 n=0 bad=0
for f in "$W"/corpus/* "$W"/mutants/*; do
  for cfg in "0 0 0" "1 0 1" "0 7 0" "1 1 1"; do
    set -- $cfg
    [ "$2" = 1 ] && [ "$(stat -c %s "$f")" -gt 4096 ] && continue
    n=$((n + 1))
    a=$(timeout 10 "$W/c/drv" "$f" $cfg 2>&1 || true); b=$(timeout 20 "$W/gdrv" "$f" $cfg 2>&1 || true)
    [ "$a" = "$b" ] || { bad=$((bad + 1)); echo "DIFF $(basename "$f") [$cfg]"; }
  done
done
echo "cases $n, differing $bad"
[ $bad -eq 0 ] || rc=1

if [ -n "$pyi" ]; then
  python3 -I "$root/tools/expat/xmldiff.py" "$W/corpus" > "$W/ref.txt"
  "$root/bin/pyi" run "$root/tools/expat/xmldiff.py" "$W/corpus" > "$W/pyi.txt"
  d=$(diff "$W/ref.txt" "$W/pyi.txt" | grep -c '^>' || true)
  echo "pyi: $(wc -l < "$W/ref.txt") lines, differing $d"
  [ "$d" -eq 0 ] || rc=1
fi
exit $rc
