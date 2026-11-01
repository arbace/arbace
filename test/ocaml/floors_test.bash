#!/bin/bash
# bin/repr-ocaml's floor function (re-review 030's finding 2, doc/ocaml/OCAML-SURVEY.md, "As built: re-review 030's fixes"): a corpus with no row
# is not a failure (opam packages, the testsuite's and whiml's .cmi level), a default corpus with no row still is, RECORD=0 does not record,
# a count below its row fails. The function is taken out of bin/repr-ocaml (no tree, no stage 0 needed).
# Run: bash test/ocaml/floors_test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
grep -q REPR_OCAML_FLOORS "$root/bin/repr-ocaml" || { echo "FAIL bin/repr-ocaml takes no REPR_OCAML_FLOORS: this test would rewrite the real floors"; exit 1; }
W=$root/.tmp/w/floors; rm -rf "$W"; mkdir -p "$W"
sed -n '/^FLOORS=/,/^}/p' "$root/bin/repr-ocaml" > "$W/floor.bash"
printf 'dumps-compilable\tstdlib\t116\ncmi-compiled\tstdlib\t115\n' > "$W/floors.tsv"
export REPR_OCAML_FLOORS=$W/floors.tsv root W
bad=0
t() { # NAME WANT-STATUS ENV... -- KIND CORPUS N
  local name=$1 want=$2; shift 2
  local envs=(); while [ "$1" != -- ]; do envs+=("$1"); shift; done; shift
  env "${envs[@]}" bash -c '. "$W/${FSRC:-floor.bash}"; floor "$@"' _ "$@" > "$W/out" 2>&1; local st=$?
  if [ "$st" != "$want" ]; then echo "FAIL $name: exit $st, want $want: $(cat "$W/out")"; bad=1; else echo "ok   $name"; fi
}
t "an opam corpus without a row passes" 0 X=1 -- dumps-compilable opam-fmt 12
t "the testsuite's .cmi level without a row passes" 0 X=1 -- cmi-compiled testsuite 900
t "a default corpus without a row fails" 1 X=1 -- cmi-compiled samples 1
t "an opam corpus that compiles nothing fails" 1 X=1 -- dumps-compilable opam-fmt 0
t "the whiml .cmi level that compiles nothing fails" 1 X=1 -- cmi-compiled whiml 0
t "RECORD=1 does not record a count of 0" 1 RECORD=1 -- dumps-compilable opam-zero 0
# the must-have-a-row corpora are the default arrays: a corpus added to one becomes required, and a hand copy would not follow
sed 's/^DEFAULT_DUMPS_CORPORA=(/DEFAULT_DUMPS_CORPORA=(newdump /; s/^DEFAULT_RT_CORPORA=(/DEFAULT_RT_CORPORA=(newrt /' "$W/floor.bash" > "$W/floor-added.bash"
t "a corpus added to the dumps default list needs a row" 1 FSRC=floor-added.bash -- dumps-compilable newdump 5
t "a corpus not in the dumps default list does not" 0 FSRC=floor-added.bash -- dumps-compilable newrt 5
t "a corpus added to the roundtrip default list needs a row" 1 FSRC=floor-added.bash -- cmi-compiled newrt 5
t "a count at its row passes" 0 X=1 -- dumps-compilable stdlib 116
t "a count below its row fails" 1 X=1 -- dumps-compilable stdlib 115
t "RECORD=0 does not record" 1 RECORD=0 -- dumps-compilable stdlib 3
[ "$(cat "$W/floors.tsv")" = "$(printf 'dumps-compilable\tstdlib\t116\ncmi-compiled\tstdlib\t115')" ] || { echo "FAIL RECORD=0 rewrote the floors"; bad=1; }
t "RECORD=1 records" 0 RECORD=1 -- dumps-compilable opam-fmt 12
grep -q "^dumps-compilable.opam-fmt.12$" "$W/floors.tsv" || { echo "FAIL RECORD=1 wrote no row"; bad=1; }
exit $bad
