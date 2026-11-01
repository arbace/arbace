#!/bin/bash
# Tests of repr/clojure's judge (bin/repr-clojure check, GTS160, audit 076), over fabricated trees
# and stubbed tools; run: test/repr/clojure/judge-test.bash [frozen|counts|notread]...
# (default: all). Exit status 0 when every case holds. TOOLS_BASH names another bin/lib/tools.bash
# (the tests of a fix are run with the file from before it to show they fail), REPR_BIN another
# bin/repr-clojure, REPR_AE another arbace/repr/clojure.ae (stage 0 is ARBACE_HOST_EXE).
set -uo pipefail
here=$(cd "$(dirname "$0")/../../.." && pwd)
. "$here/bin/lib/tmpdir.bash"
tools=${TOOLS_BASH:-$here/bin/lib/tools.bash}
scratch=$(mktemp -d "$here/.tmp/judge-test.XXXXXX") || exit 1
trap 'rm -rf "$scratch"' EXIT
fails=0
ok() { echo "ok: $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }
expect() { # expect DESC STATUS-WANTED STATUS
  if [ "$2" = "$3" ]; then ok "$1"; else bad "$1 (status $3, wanted $2)"; fi
}

# a fabricated repository holding the tag arbace-for-java-26-v1 with clojure/a{1,2,3}.clj
frozen_repo() { # DIR
  local r=$1
  mkdir -p "$r/clojure" "$r/test" "$r/.tmp"
  git -C "$r" init -q
  for i in 1 2 3; do echo "(def a$i $i)" > "$r/clojure/a$i.clj"; done
  git -C "$r" add -A >/dev/null
  git -C "$r" -c user.name=t -c user.email=t@t commit -q -m c
  git -C "$r" tag arbace-for-java-26-v1
  (cd "$r/clojure" && find . -type f -print0 | LC_ALL=C sort -z | xargs -0 sha256sum | sha256sum | cut -c1-64) | sed 's/$/  clojure/' > "$r/test/frozen-clojure.sha256"
}
frozen_call() { # DIR: frozen_clojure in the fabricated repository DIR; prints its directory
  ( root=$1; . "$tools"; frozen_clojure )
}

t_frozen() {
  local r=$scratch/fz d n
  frozen_repo "$r"
  d=$(frozen_call "$r" 2>"$scratch/err"); expect "frozen: first extraction" 0 $?
  n=$(find "$d/clojure" -type f | wc -l); [ "$n" = 3 ] && ok "frozen: 3 files" || bad "frozen: $n files"
  rm -f "$d/clojure/a2.clj" "$d/clojure/a3.clj"
  d=$(frozen_call "$r" 2>"$scratch/err"); expect "frozen: after losing files" 0 $?
  n=$(find "$d/clojure" -type f | wc -l); [ "$n" = 3 ] && ok "frozen: re-extracted whole" || bad "frozen: still $n files with the .rev marker matching"
  echo "(def a1 99)" > "$d/clojure/a1.clj"
  d=$(frozen_call "$r" 2>"$scratch/err")
  [ "$(cat "$d/clojure/a1.clj")" = "(def a1 1)" ] && ok "frozen: edited file restored" || bad "frozen: edited file kept"
  rm -rf "$d/clojure"
  d=$(frozen_call "$r" 2>"$scratch/err"); expect "frozen: after losing clojure/" 0 $?
  [ -d "$d/clojure" ] && ok "frozen: clojure/ back" || bad "frozen: clojure/ missing, exit 0"
  # a tag re-pointed at another tree is refused
  echo "(def a1 2)" > "$r/clojure/a1.clj"
  git -C "$r" -c user.name=t -c user.email=t@t commit -q -a -m d
  git -C "$r" tag -f arbace-for-java-26-v1 >/dev/null
  frozen_call "$r" > "$scratch/out" 2>"$scratch/err"; local s=$?
  [ "$s" != 0 ] && ok "frozen: a re-pointed tag refused" || bad "frozen: a re-pointed tag accepted"
  rm -f "$r/test/frozen-clojure.sha256"
  frozen_call "$r" > "$scratch/out" 2>"$scratch/err"; s=$?
  [ "$s" != 0 ] && ok "frozen: no pin refused" || bad "frozen: no pin accepted"
}

tests=("$@"); [ ${#tests[@]} = 0 ] && tests=(frozen)
for t in "${tests[@]}"; do "t_$t"; done
[ $fails = 0 ] && echo "judge-test: all hold" || echo "judge-test: $fails FAILED"
[ $fails = 0 ]
