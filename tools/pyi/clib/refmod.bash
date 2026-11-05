#!/bin/bash
# The reference's copy of a C library module (queue 215; doc/python/PYTHON-SURVEY.md, "As built: 215"): CPython
# 3.14.10's own Modules/ glue for LIB, built by the host's C compiler as an extension module of the oracle's python3
# (the same version: its pyconfig.h, the tarball's Include/) over the library's pinned tarball with lib.conf's sources
# and flags (no threads, no assembly, no intrinsics), into OUT. The oracle needs it where its python3 was built without
# the module (Alpine's python3 3.14.10 has no _zstd): bin/pyi record-ref lib-test-all puts OUT on its PYTHONPATH, so the
# reference runs the same C library and CPython's glue that pyi's port follows. Kept by a key over its inputs.
#
#   tools/pyi/clib/refmod.bash LIB OUT     prints OUT once OUT/MODULE.EXT_SUFFIX is built (LIB: zstd)
#
# Not in bin/pyi-clib nor tools/pyi/clib/LIB/: both are in the node program's key (bin/pyi-clib key), which this
# reference's builder must not move.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
lib=${1:?LIB}; out=${2:?OUT}
case $lib in
  zstd) mod=_zstd; mdir=Modules/_zstd; msrc="_zstdmodule.c compressor.c decompressor.c zstddict.c" ;;
  *) echo "refmod: no reference module for $lib" >&2; exit 2 ;;
esac
conf=$root/tools/pyi/clib/$lib/lib.conf
url= sha256= top= cflags= sources= glue= includes=
. "$conf"
cmain=${root%%/.tmp/gate-clean/*}; cmain=${cmain%%/.tmp/worktrees/*}
tb=${PYI_CLIB_CACHE:-$cmain/.tmp/cache/clib}/$(basename "$url")
[ -f "$tb" ] || "$root/bin/pyi-clib" fetch "$lib" >/dev/null
echo "$sha256  $tb" | sha256sum -c --quiet
py=$root/.tmp/repr-python/Python-3.14.10
[ -d "$py/$mdir" ] || "$root/bin/repr-python" pins --fetch >/dev/null
inc=$(python3 -I -c 'import sysconfig; print(sysconfig.get_config_var("INCLUDEPY"))')
ext=$(python3 -I -c 'import sysconfig; print(sysconfig.get_config_var("EXT_SUFFIX"))')
[ -f "$inc/pyconfig.h" ] || { echo "refmod: no $inc/pyconfig.h (the oracle's)" >&2; exit 1; }
cc=${CC:-gcc}
# lib.conf's flags less the -U of ci's host (they undefine the host's architecture for ci's builder alone)
flags=()
for f in $cflags; do case $f in -U*) ;; *) flags+=("$f") ;; esac; done
key=$( { echo "$sha256 $cc $ext"; "$cc" --version | head -1; sha256sum "$0" "$conf" "$inc/pyconfig.h"
         (cd "$py" && sha256sum $(for s in $msrc; do echo "$mdir/$s"; done) "$mdir"/*.h "$mdir"/clinic/*.h); } | sha256sum | cut -c1-16)
if [ -f "$out/$mod$ext" ] && [ "$(cat "$out/refmod-$lib.key" 2>/dev/null)" = "$key" ]; then echo "$out"; exit 0; fi
w=$out/.build-$lib; rm -rf "$w"; mkdir -p "$w/src" "$w/obj"
tar -xzf "$tb" -C "$w/src"
src=$w/src/$top
incs=(-I"$src"); for d in $includes; do incs+=(-I"$src/$d"); done
objs=()
for s in $sources; do
  o=$w/obj/$(echo "$s" | tr / _).o
  "$cc" -O1 -fPIC "${flags[@]}" "${incs[@]}" -c "$src/$s" -o "$o"; objs+=("$o")
done
for s in $msrc; do
  o=$w/obj/m_$s.o
  "$cc" -O1 -fPIC "${flags[@]}" "${incs[@]}" -DPy_BUILD_CORE_MODULE -I"$py/Include" -I"$py/Include/internal" -I"$inc" \
    -I"$py/$mdir" -c "$py/$mdir/$s" -o "$o"; objs+=("$o")
done
"$cc" -shared -o "$out/$mod$ext" "${objs[@]}"
rm -rf "$w"
echo "$key" > "$out/refmod-$lib.key"
echo "$out"
