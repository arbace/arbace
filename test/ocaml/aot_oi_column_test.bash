#!/bin/bash
# Review 008, finding 1 (doc/ocaml/OCAML-SURVEY.md, "As built: review 008's fixes"): the OI column of bin/oi check --aot
# is oi's result for the same programs from the same oi build, not whatever bin/oi check last left in .tmp/oi/check.tsv.
# Three programs, judged against the record (no --record), in three states of .tmp/oi/check.tsv: absent, left by another
# --only run, and stale (its key not the current oi's). The judge must not print CHANGED and must exit 0 in all three.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
OI=.tmp/oi; keep=$root/.tmp/w/aot-oi-column; rm -rf "$keep"; mkdir -p "$keep"
for f in check.tsv check.tsv.key; do [ -f "$OI/$f" ] && cp "$OI/$f" "$keep/$f"; done
restore() { rm -f "$OI/check.tsv" "$OI/check.tsv.key"; for f in check.tsv check.tsv.key; do [ -f "$keep/$f" ] && cp "$keep/$f" "$OI/$f"; done; }
trap restore EXIT
only='basic/(arrays|divint|min_int)'
bad=0
run() { # NAME
  local out=$keep/$1.log
  bin/oi check --aot --only "$only" > "$out" 2>&1; local st=$?
  if [ $st != 0 ] || grep -q '^CHANGED' "$out"; then echo "FAIL $1: exit $st"; grep '^CHANGED' "$out" | head -3; bad=1; else echo "ok   $1"; fi
}
rm -f "$OI/check.tsv" "$OI/check.tsv.key"; run absent
bin/oi check --only 'basic/arrays' > "$keep/partial.oi.log" 2>&1; run partial
printf 'basic/divint\t0\t0000\t0\t0\t\n' > "$OI/check.tsv"; echo stale > "$OI/check.tsv.key"; run stale
exit $bad
