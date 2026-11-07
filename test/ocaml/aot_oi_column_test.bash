#!/bin/bash
# Review 008, finding 1 (doc/ocaml/OCAML-SURVEY.md, "As built: review 008's fixes"): the OI column of bin/oi check --aot
# is oi's result for the same programs from the same oi build, not whatever bin/oi check last left in .tmp/oi/check.tsv.
# Three programs, judged against the record (no --record), in three states of .tmp/oi/check.tsv: absent, left by another
# --only run, and stale (its key not the current oi's). The judge must not print CHANGED and must exit 0 in all three.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
OI=.tmp/oi; keep=$root/.tmp/w/aot-oi-column; rm -rf "$keep"; mkdir -p "$keep"
# the default path's rows file (TY24 (a): the native path's check-native.tsv; OI_CHECK_DEFAULT=ocdump, the oracle's check.tsv)
T=check-native.tsv; [ "${OI_CHECK_DEFAULT:-native}" = ocdump ] && T=check.tsv
for f in $T $T.key; do [ -f "$OI/$f" ] && cp "$OI/$f" "$keep/$f"; done
restore() { rm -f "$OI/$T" "$OI/$T.key"; for f in $T $T.key; do [ -f "$keep/$f" ] && cp "$keep/$f" "$OI/$f"; done; }
trap restore EXIT
only='basic/(arrays|divint|min_int)'
bad=0
run() { # NAME
  local out=$keep/$1.log
  bin/oi check --aot --only "$only" > "$out" 2>&1; local st=$?
  if [ $st != 0 ] || grep -q '^CHANGED' "$out"; then echo "FAIL $1: exit $st"; grep '^CHANGED' "$out" | head -3; bad=1; else echo "ok   $1"; fi
}
rm -f "$OI/$T" "$OI/$T.key"; run absent
bin/oi check --only 'basic/arrays' > "$keep/partial.oi.log" 2>&1; run partial
printf 'basic/divint\t0\t0000\t0\t0\t\n' > "$OI/$T"; echo stale > "$OI/$T.key"; run stale
exit $bad
