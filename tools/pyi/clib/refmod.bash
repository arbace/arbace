#!/bin/bash
# The reference's copy of a C library module (queue 215; doc/python/PYTHON-SURVEY.md, "As built: 215"): CPython
# 3.14.10's own Modules/ glue for LIB, built by the host's C compiler as an extension module of the oracle's python3
# (the same version: its pyconfig.h, the tarball's Include/) over the library's pinned tarball with lib.conf's sources
# and flags (no threads, no assembly, no intrinsics), into OUT. The oracle needs it where its python3 was built without
# the module (Alpine's python3 3.14.10 has no _zstd): bin/pyi record-ref lib-test-all puts OUT on its PYTHONPATH, so the
# reference runs the same C library and CPython's glue that pyi's port follows. Kept by a key over its inputs.
#
#   tools/pyi/clib/refmod.bash LIB OUT     prints OUT once OUT/MODULE.EXT_SUFFIX is built (LIB: zstd, sqlite)
#
# Not in bin/pyi-clib nor tools/pyi/clib/LIB/: both are in the node program's key (bin/pyi-clib key), which this
# reference's builder must not move.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
lib=${1:?LIB}; out=${2:?OUT}
case $lib in
  zstd) mod=_zstd; mdir=Modules/_zstd; msrc="_zstdmodule.c compressor.c decompressor.c zstddict.c"; mdefs=""; noloadext= ;;
  # Alpine's python3 has _sqlite3, but over the system's SQLite (3.54.1), not the pinned 3.50.4
  sqlite) mod=_sqlite3; mdir=Modules/_sqlite
          msrc="blob.c connection.c cursor.c microprotocols.c module.c prepare_protocol.c row.c statement.c util.c"
          # the oracle's pyconfig.h enables loadable extensions; ci has no dlopen and pyi's build omits them
          # (SQLITE_OMIT_LOAD_EXTENSION): a pyconfig.h of the build's own includes it and takes the macro back
          mdefs="-DPY_SQLITE_HAVE_SERIALIZE=1"; noloadext=1 ;;
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
key=$( { echo "$sha256 $cc $ext $mdefs"; "$cc" --version | head -1; sha256sum "$0" "$conf" "$inc/pyconfig.h"
         (cd "$py" && sha256sum $(for s in $msrc; do echo "$mdir/$s"; done) "$mdir"/*.h "$mdir"/clinic/*.h); } | sha256sum | cut -c1-16)
if [ -f "$out/$mod$ext" ] && [ "$(cat "$out/refmod-$lib.key" 2>/dev/null)" = "$key" ]; then echo "$out"; exit 0; fi
w=$out/.build-$lib; rm -rf "$w"; mkdir -p "$w/src" "$w/obj"
tar -xzf "$tb" -C "$w/src"
src=$w/src/$top
incs=(-I"$src"); for d in $includes; do incs+=(-I"$src/$d"); done
pyinc=$inc
if [ -n "$noloadext" ]; then
  mkdir -p "$w/pyconfig"; pyinc=$w/pyconfig
  printf '#include "%s/pyconfig.h"\n#undef PY_SQLITE_ENABLE_LOAD_EXTENSION\n' "$inc" > "$w/pyconfig/pyconfig.h"
fi
objs=()
for s in $sources; do
  o=$w/obj/$(echo "$s" | tr / _).o
  "$cc" -O1 -fPIC "${flags[@]}" "${incs[@]}" -c "$src/$s" -o "$o"; objs+=("$o")
done
for s in $msrc; do
  o=$w/obj/m_$s.o
  # shellcheck disable=SC2086
  "$cc" -O1 -fPIC "${flags[@]}" "${incs[@]}" -DPy_BUILD_CORE_MODULE $mdefs -I"$pyinc" -I"$py/Include" -I"$py/Include/internal" \
    -I"$py/$mdir" -c "$py/$mdir/$s" -o "$o"; objs+=("$o")
done
"$cc" -shared -o "$out/$mod$ext" "${objs[@]}"
rm -rf "$w"
echo "$key" > "$out/refmod-$lib.key"
echo "$out"
