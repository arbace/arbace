#!/bin/bash
# OC122 (b): the dumps' passes are deterministic (doc/ocaml/OCAML-SURVEY.md, "As built: OC122"). The stdlib's files that compile alone were 115 or
# 116 from run to run (set.ml racing set.mli for set.cmi in a parallel pass); now the same files every run. Runs `types stdlib` N times
# (default 6) and requires one count (at least the floor in test/ocaml/floors.tsv) and one list of files. Needs the corpus fetched
# (bin/repr-ocaml corpus-fetch) and ARBACE_HOST_EXE. REPR_OCAML_BIN names another bin/repr-ocaml (the test of the script before the fix).
# Run: bash test/ocaml/dumps_determinism_test.bash [N]
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
bin=${REPR_OCAML_BIN:-$root/bin/repr-ocaml}
n=${1:-6}
counts=""; lists=""
for i in $(seq "$n"); do
  out=$("$bin" types --jobs 4 stdlib 2>&1)
  c=$(echo "$out" | sed -n 's/^== stdlib: [0-9]* files, \([0-9]*\) compile alone.*/\1/p')
  counts="$counts $c"
  lists="$lists $(sort "$root/.tmp/repr-ocaml/ty/stdlib/compilable" | sha256sum | cut -c1-12)"
done
echo "counts:$counts"; echo "lists:$lists"
if [ "$(echo $counts | tr ' ' '\n' | sort -u | wc -l)" = 1 ] && [ "$(echo $lists | tr ' ' '\n' | sort -u | wc -l)" = 1 ]; then
  echo "ok   the same files compile alone in $n runs"
else
  echo "FAIL the count or the files changed from run to run"; exit 1
fi
