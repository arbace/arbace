#!/bin/bash
# cpp-indep3 (2026-11-26; DECISION cpp-indep3-1 (a), the user): the oracle's measure for test/cpp/divergences-dg.edn's
# :ifndr-uninstantiated-template, by hand (g++ 15.2.0, the host's; the oracle tier's tool, never a gate step).
# usage: test/cpp/ifndr/template-body.sh LIST GXXDG OUT
#   LIST: g++.dg names (one a line, gxx-dg/ prefix optional); GXXDG: the g++.dg tree (the pinned gcc tarball's);
#   OUT: a TSV, a row a file: name, the lines of its errors g++ tags [-Wtemplate-body], the lines of its errors under
#   -Wno-template-body; each run with the -std and the options of test/cpp/ref-dg-gxx15.edn's row.
# A file's row qualifies (ifndr.txt) when each line its own record misses is in the second column and not in the third: g++
# diagnoses it in a template's body early and defers it to an instantiation that never comes ([temp.res.general]/6: ill-formed,
# no diagnostic required).
set -u
root=$(cd "$(dirname "$0")/../../.." && pwd)
list=$1 G=$(cd "$2" && pwd) out=$3
: > "$out"
sed 's#^gxx-dg/##' "$list" | while read -r f; do
  line=$(grep -F "\"gxx-dg/$f\"" "$root/test/cpp/ref-dg-gxx15.edn")
  std=$(echo "$line" | grep -o ':std "[^"]*"' | cut -d'"' -f2)
  args=$(echo "$line" | grep -o ':args \[[^]]*\]' | sed 's/:args \[//; s/\]$//' | tr -d '"')
  d=$(dirname "$G/$f") b=$(basename "$f")
  # shellcheck disable=SC2086
  a=$(cd "$d" && timeout 60 g++ -fsyntax-only -std=$std $args -fdiagnostics-plain-output -I"$G" "$b" 2>&1 \
      | grep -E "^[^:]*$b:[0-9]+:[0-9]+: error: .*\[-Wtemplate-body\]" | sed -E 's/^[^:]*:([0-9]+):.*/\1/' | sort -un | tr '\n' ' ')
  # shellcheck disable=SC2086
  n=$(cd "$d" && timeout 60 g++ -fsyntax-only -std=$std $args -Wno-template-body -fdiagnostics-plain-output -I"$G" "$b" 2>&1 \
      | grep -E "^[^:]*$b:[0-9]+:[0-9]+: error:" | sed -E 's/^[^:]*:([0-9]+):.*/\1/' | sort -un | tr '\n' ' ')
  printf '%s\t%s\t%s\n' "$f" "$a" "$n" >> "$out"
done
