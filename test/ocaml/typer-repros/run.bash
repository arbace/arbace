#!/bin/bash
# queue 546 (test/ocaml/typer-repros/run.bash ROOT [STDLIB-BUILD]): the samples of the typer's fixes for ppxlib's ast/versions.ml and
# astlib's migrations, each ocamlc accepts, each typed by ROOT's typer (arbace.repr.ocaml.typer lambda) must be accepted: a record's
# first label qualified by find_record_qual (c0), constrained abbreviations expanded by instance (c1), a declaration's nodes all generic
# (c4), no abbreviation memo under GADT equations (c5, c12), a package type's nested constraints (c15, c19, c20, c21). STDLIB-BUILD: the
# native stdlib's build directory (bin/oi stdlib's .tmp/oi/stdlib-native-build by default).
ROOT=${1:?ROOT}; L=${2:-$ROOT/.tmp/oi/stdlib-native-build}; D=$(cd "$(dirname "$0")" && pwd); fail=0
W=$(mktemp -d "${TMPDIR:-$ROOT/.tmp/tmp}/t546.XXXX")
for f in "$D"/*.ml; do
  n=$(basename "$f"); cp "$f" "$W/"
  if (cd "$W" && ARBACE_PATH=$ROOT "$ARBACE_HOST_EXE" -m arbace.repr.ocaml.typer lambda --simplify --stdlib "$L" --file "$n" "$n" > /dev/null 2> "$n.err"); then echo "ok $n"
  else echo "FAIL $n: $(grep -v '^Execution\|^$\|Full report\|edn$' "$W/$n.err" | head -1 | cut -c1-160)"; fail=1; fi
done
rm -rf "$W"; [ $fail = 0 ] && echo PASS; exit $fail
