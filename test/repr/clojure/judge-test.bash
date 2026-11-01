#!/bin/bash
# Tests of repr/clojure's judge (bin/repr-clojure check, GTS160, audit 076), over fabricated trees
# and stubbed tools; run: test/repr/clojure/judge-test.bash [frozen|notread]...
# (default: all). Exit status 0 when every case holds. TOOLS_BASH names another bin/lib/tools.bash
# (the tests of a fix are run with the file from before it to show they fail). The checks that
# need Arbace run on stage 0 (ARBACE_HOST_EXE) with arbace/repr/clojure.ae from the checkout.
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

# bin/repr-clojure roundtrip over a fabricated tree, a known file; prints its output, returns its status
rt() { "$here/bin/repr-clojure" roundtrip "$@" 2>&1; }

# Arbace -> Clojure -> Arbace with a stubbed print that loses "(LOST) " (a fault of print)
lost_rt() { # DIR [lose]
  ( cd "$here"; root=$here; . "$here/bin/lib/tools.bash"
    local fault='(fn [s p skip] (orig s p skip))'
    [ "${2:-}" = lose ] && fault='(fn [s p skip] (.replace ^String (orig s p skip) "(LOST) " ""))'
    arbace_tool "${repr_clojure_sources[@]}" -e "(require 'arbace.repr.clojure)
      (let [orig arbace.repr.clojure.rules/print-text]
        (with-redefs [arbace.repr.clojure.rules/print-text $fault]
          (arbace.repr.clojure/-main \"roundtrip\" \"--from-arbace\" \"-v\" \"$1\")))" 2>&1 )
}

t_notread() {
  local d=$scratch/n out
  mkdir -p "$d/named" "$d/plain"
  printf '(ns x)\n;; see clojure.core/map\n(defn f [] (LOST) 1)\n' > "$d/named/x.ae"
  printf '(ns y)\n(defn f [] (LOST) 1)\n' > "$d/plain/y.ae"
  out=$(lost_rt "$d/named"); expect "notread: no fault, a text naming clojure.* is outside read's image" 0 $?
  echo "$out" | grep -q "1 outside" && ok "notread: counted outside" || bad "notread: not counted outside: $out"
  out=$(lost_rt "$d/plain" lose); expect "notread: a print fault in a plain file fails" 1 $?
  out=$(lost_rt "$d/named" lose); expect "notread: the same fault in a file naming clojure.* fails" 1 $?
}

tests=("$@"); [ ${#tests[@]} = 0 ] && tests=(frozen notread)
for t in "${tests[@]}"; do "t_$t"; done
[ $fails = 0 ] && echo "judge-test: all hold" || echo "judge-test: $fails FAILED"
[ $fails = 0 ]
