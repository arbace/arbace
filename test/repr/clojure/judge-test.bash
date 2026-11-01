#!/bin/bash
# Tests of repr/clojure's judge (bin/repr-clojure check, GTS160, audit 076), over fabricated trees
# and stubbed tools; run: test/repr/clojure/judge-test.bash [frozen|counts|notread|x509|libs|known]...
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
  git -C "$r" add -A >"$scratch/out"
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
  git -C "$r" tag -f arbace-for-java-26-v1 >"$scratch/out"
  frozen_call "$r" > "$scratch/out" 2>"$scratch/err"; local s=$?
  [ "$s" != 0 ] && ok "frozen: a re-pointed tag refused" || bad "frozen: a re-pointed tag accepted"
  rm -f "$r/test/frozen-clojure.sha256"
  frozen_call "$r" > "$scratch/out" 2>"$scratch/err"; s=$?
  [ "$s" != 0 ] && ok "frozen: no pin refused" || bad "frozen: no pin accepted"
}

# bin/repr-clojure roundtrip over a fabricated tree, a known file; prints its output, returns its status
rt() { "$here/bin/repr-clojure" roundtrip "$@" 2>&1; }

t_counts() {
  local d=$scratch/c k=$scratch/c-known.edn s out
  mkdir -p "$d/two" "$d/empty"
  echo "(ns a.one) (def x 1)" > "$d/two/one.clj"; echo "(ns a.two) (def y 2)" > "$d/two/two.clj"
  echo "{:c {} :counts {:c {:files 2 :one-way 0 :outside 0}}}" > "$k"
  out=$(rt --known "$k" --corpus c "$d/two"); expect "counts: the recorded size holds" 0 $?
  echo "{:c {} :counts {:c {:files 3 :one-way 0 :outside 0}}}" > "$k"
  out=$(rt --known "$k" --corpus c "$d/two"); expect "counts: a shrunken corpus fails" 1 $?
  echo "{:c {} :counts {:c {:files 2 :one-way 0 :outside 0}}}" > "$k"
  out=$(rt --known "$k" --corpus c "$d/empty"); expect "counts: an empty corpus fails" 1 $?
  out=$(rt --known "$k" --corpus c "$d/missing"); expect "counts: a missing directory fails" 1 $?
  echo "{:c {} :counts {:other {:files 2 :one-way 0 :outside 0}}}" > "$k"
  out=$(rt --known "$k" --corpus c "$d/two"); expect "counts: no counts recorded fails" 1 $?
  echo "{:c {} :counts {:c {:files-min 2 :one-way 0 :outside-max 0}}}" > "$k"
  out=$(rt --known "$k" --corpus c "$d/two"); expect "counts: a floor held" 0 $?
  echo "{:c {} :counts {:c {:files-min 3 :one-way 0 :outside-max 0}}}" > "$k"
  out=$(rt --known "$k" --corpus c "$d/two"); expect "counts: a floor not held fails" 1 $?
}

# Arbace -> Clojure -> Arbace with a stubbed print that loses "(LOST) " (a fault of print)
lost_rt() { # DIR [lose [ARG...]]
  ( cd "$here"; root=$here; . "$here/bin/lib/tools.bash"
    local fault='(fn [s p skip] (orig s p skip))' extra="" a
    [ "${2:-}" = lose ] && fault='(fn [s p skip] (.replace ^String (orig s p skip) "(LOST) " ""))'
    local dir=$1; if [ $# -ge 2 ]; then shift 2; else shift $#; fi
    for a in "$@"; do extra="$extra \"$a\""; done
    arbace_tool "${repr_clojure_sources[@]}" -e "(require 'arbace.repr.clojure)
      (let [orig arbace.repr.clojure.rules/print-text]
        (with-redefs [arbace.repr.clojure.rules/print-text $fault]
          (arbace.repr.clojure/-main \"roundtrip\" \"--from-arbace\" \"-v\" $extra \"$dir\")))" 2>&1 )
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

# bin/x509roots-vendor check against a stubbed go (go env GOMODCACHE only) and a fabricated module cache
t_x509() {
  local x=$scratch/x g=$scratch/x/goroot c=$scratch/x/cache w=$scratch/x/work ver zip m out
  ver=$(sed -n 's/^mod_ver=//p' "$here/bin/x509roots-vendor"); zip=$(sed -n 's/^mod_zip=//p' "$here/bin/x509roots-vendor")
  mkdir -p "$g/bin" "$c/cache/download/golang.org/x/crypto/x509roots/fallback/@v" "$w/renamed"
  printf '#!/bin/sh\n[ "$1 $2" = "env GOMODCACHE" ] && { echo %s; exit 0; }\nexit 1\n' "$c" > "$g/bin/go"; chmod +x "$g/bin/go"
  echo "$zip" > "$c/cache/download/golang.org/x/crypto/x509roots/fallback/@v/$ver.ziphash"
  m=$c/golang.org/x/crypto/x509roots/fallback@$ver/bundle; mkdir -p "$m"; echo "package bundle" > "$m/bundle.go"
  echo stale > "$w/renamed/stale.txt"
  out=$(ARBACE_GOROOT=$g X509ROOTS_TODAY=20261111 "$here/bin/x509roots-vendor" --work "$w" check 2>&1)
  [ ! -e "$w/renamed/stale.txt" ] && ok "x509: check remakes renamed (a stale tree is no original)" || bad "x509: a stale renamed tree was reused"
  [ -f "$w/renamed/x509roots/fallback/bundle/bundle.go" ] && ok "x509: renamed holds the module's package" || bad "x509: renamed lacks the package"
  echo "$out" | grep -q "snapshot of 20261011, 31 days old at 20261111" && ok "x509: the snapshot's age is noted" || bad "x509: no age note: $out"
}

t_libs() {
  local d=$scratch/l ct=$scratch/l/clojure-tests s1 s2
  mkdir -p "$d/lib"
  . "$here/bin/lib/repr-clojure/libs.bash" 2>"$scratch/err" || { bad "libs: no bin/lib/repr-clojure/libs.bash"; return; }
  printf 'jar one\n' > "$d/stage-one"; printf 'jar two\n' > "$d/stage-two"
  s1=$(sha1sum < "$d/stage-one" | cut -c1-40); s2=$(sha1sum < "$d/stage-two" | cut -c1-40)
  printf 'x=1\ndeps="\norg/clojure/alpha/1.0 %s\norg/clojure/beta/2.1 %s\n"\nmore=2\n' "$s1" "$s2" > "$ct"
  [ "$(lib_jars_of "$ct" | tr '\n' ' ')" = "alpha-1.0 beta-2.1 " ] && ok "libs: jars derived from the deps block" || bad "libs: jars $(lib_jars_of "$ct")"
  lib_verify "$ct" "$d/lib" 2>"$scratch/err"; expect "libs: none downloaded is a skip (2)" 2 $?
  cp "$d/stage-one" "$d/lib/alpha-1.0.jar"; cp "$d/stage-two" "$d/lib/beta-2.1.jar"
  lib_verify "$ct" "$d/lib" 2>"$scratch/err"; expect "libs: both verified" 0 $?
  rm "$d/lib/beta-2.1.jar"
  lib_verify "$ct" "$d/lib" 2>"$scratch/err"; expect "libs: one missing fails (1)" 1 $?
  # a bump in the deps with an old jar left in the directory: the old jar is not taken by presence
  cp "$d/stage-two" "$d/lib/beta-2.1.jar"
  sed 's|alpha/1.0|alpha/1.1|' "$ct" > "$ct.bump"; cp "$d/stage-one" "$d/lib/alpha-1.1.jar"
  lib_verify "$ct.bump" "$d/lib" 2>"$scratch/err"; expect "libs: a bumped version verified" 0 $?
  rm "$d/lib/alpha-1.1.jar"; sed 's|alpha/1.0|alpha/1.1|' "$ct" > "$ct.bump"
  lib_verify "$ct.bump" "$d/lib" 2>"$scratch/err"; expect "libs: the old jar does not stand for the bumped version" 1 $?
  echo tampered > "$d/lib/alpha-1.0.jar"
  lib_verify "$ct" "$d/lib" 2>"$scratch/err"; expect "libs: a jar with another SHA-1 fails" 1 $?
}

# a known entry excuses the difference it describes, not another of its file
t_known() {
  local d=$scratch/k/plain k=$scratch/k/known.edn out
  mkdir -p "$d"; printf '(ns y)\n(defn f [] (LOST) 1)\n' > "$d/y.ae"
  local at='line 2: \"(defn f [] (LOST) 1)\" / \"(defn f [] 1)\"'
  kn() { echo "{:c {\"y.ae\" $1} :counts {:c {:files 1 :one-way 0 :outside 0}}}" > "$k"; }
  kn "{:why :text :at \"$at\" :reason \"print loses it\"}"
  out=$(lost_rt "$d" lose --known "$k" --corpus c); expect "known: an entry describing the difference excuses it" 0 $?
  kn "{:why :text :at \"line 9: \\\"x\\\" / \\\"y\\\"\"}"
  out=$(lost_rt "$d" lose --known "$k" --corpus c); expect "known: another first differing line is new" 1 $?
  kn "{:why :name :at \"$at\"}"
  out=$(lost_rt "$d" lose --known "$k" --corpus c); expect "known: another kind of difference is new" 1 $?
  kn '"print loses it"'
  out=$(lost_rt "$d" lose --known "$k" --corpus c); expect "known: a bare reason excuses nothing" 1 $?
}

tests=("$@"); [ ${#tests[@]} = 0 ] && tests=(frozen counts notread x509 libs known)
for t in "${tests[@]}"; do "t_$t"; done
[ $fails = 0 ] && echo "judge-test: all hold" || echo "judge-test: $fails FAILED"
[ $fails = 0 ]
