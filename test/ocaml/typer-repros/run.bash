#!/bin/bash
# queue 546 (test/ocaml/typer-repros/run.bash ROOT [STDLIB-BUILD]): the samples of the typer's fixes for ppxlib's ast/versions.ml and
# astlib's migrations, each ocamlc accepts, each typed by ROOT's typer (arbace.repr.ocaml.typer lambda) must be accepted: a record's
# first label qualified by find_record_qual (c0), constrained abbreviations expanded by instance (c1), a declaration's nodes all generic
# (c4), no abbreviation memo under GADT equations (c5, c12), a package type's nested constraints (c15, c19, c20, c21), a parameter that unpacks a module scoped so its types leaving
# the function expand, and an object not linked to a scoped module's abbreviation (c24, c25); the directories of units: m1 and m2 (below). STDLIB-BUILD: the
# native stdlib's build directory (bin/oi stdlib's .tmp/oi/stdlib-native-build by default).
ROOT=${1:?ROOT}; L=${2:-$ROOT/.tmp/oi/stdlib-native-build}; D=$(cd "$(dirname "$0")" && pwd); fail=0
W=$(mktemp -d "${TMPDIR:-$ROOT/.tmp/tmp}/t546.XXXX")
for f in "$D"/*.ml; do
  n=$(basename "$f"); cp "$f" "$W/"
  if (cd "$W" && ARBACE_PATH=$ROOT "$ARBACE_HOST_EXE" -m arbace.repr.ocaml.typer lambda --simplify --stdlib "$L" --file "$n" "$n" > /dev/null 2> "$n.err"); then echo "ok $n"
  else echo "FAIL $n: $(grep -v '^Execution\|^$\|Full report\|edn$' "$W/$n.err" | head -1 | cut -c1-160)"; fail=1; fi
done
# a directory of units (its file `units`: their order), each typed against the earlier ones' native .cmi (--cmi-out), as the suite's native
# path types a program's units: m1, an alias in a unit's interface to a module inside an earlier sibling (queue 546b, env's find-unit);
# m2, a class whose methods name a module of its unit, inherited from another unit (queue 546b, env's subst-items); m3, an interface
# (.mli, typed with --intf) whose class inherits another unit's class read inside it (queue 546b, nested with-unit-stamps); m4, a method!
# without a type overriding an inherited polymorphic method (queue 546b, typeclass's first pass); m5, a class type whose methods name
# a module of its unit, inherited by another unit's interface (queue 546b, subst-items)
for d in "$D"/*/; do
  [ -f "$d/units" ] || continue
  n=$(basename "$d"); w=$W/$n; mkdir -p "$w"; cp "$d"/*.ml* "$w/"; ok=1
  for u in $(cat "$d/units"); do
    x="--cmi-out $w/${u%.ml}.cmi.ae"; [ -f "$w/${u%.ml}.mli" ] && x=--intf
    (cd "$w" && ARBACE_PATH=$ROOT "$ARBACE_HOST_EXE" -m arbace.repr.ocaml.typer lambda --simplify --stdlib "$L" --include "$w" $x --file "$u" "$u" > /dev/null 2> "$w/$u.err") || { ok=0; echo "FAIL $n/$u: $(grep -v '^Execution\|^$\|Full report\|edn$' "$w/$u.err" | head -1 | cut -c1-160)$(grep -o 'StackOverflowError' "$w/$u.err" | head -1)"; fail=1; break; }
  done
  [ $ok = 1 ] && echo "ok $n"
done
rm -rf "$W"; [ $fail = 0 ] && echo PASS; exit $fail
