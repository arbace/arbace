# The suite's test libraries for bin/repr-clojure check (sourced by bin/lib/repr-clojure/check): the
# jars are those of bin/clojure-tests' deps block (one "org/clojure/NAME/VERSION SHA1" per line),
# never a list kept here, and each is checked against its SHA-1 there before it is used.
#
#   lib_jars_of CLOJURE-TESTS      the jar names, NAME-VERSION, one per line (jar_of's)
#   lib_verify CLOJURE-TESTS DIR   0: every jar of the deps is in DIR with its pinned SHA-1;
#                                  2: none is there (bin/clojure-tests has not downloaded them);
#                                  1: some is missing or differs (said on standard error)
lib_deps() { sed -n '/^deps="$/,/^"$/p' "$1" | sed -n 's/^\([^ ]*\/[^ /]*\/[^ /]*\) \([0-9a-f]\{40\}\)$/\1 \2/p'; }

lib_jars_of() { lib_deps "$1" | while read -r coord sha; do
    v=${coord##*/}; a=${coord%/*}; echo "${a##*/}-$v"
  done; }

lib_verify() {
  local ct=$1 dir=$2 coord sha v a jar n=0 have=0 bad=0
  while read -r coord sha; do
    [ -n "$coord" ] || continue
    v=${coord##*/}; a=${coord%/*}; jar=$dir/${a##*/}-$v.jar; n=$((n + 1))
    if [ ! -f "$jar" ]; then echo "repr-clojure libs: no $jar" >&2; bad=1; continue; fi
    have=$((have + 1))
    if ! echo "$sha  $jar" | sha1sum -c --status; then
      echo "repr-clojure libs: $jar is not the SHA-1 $sha that bin/clojure-tests pins" >&2; bad=1
    fi
  done <<< "$(lib_deps "$ct")"
  [ "$n" -gt 0 ] || { echo "repr-clojure libs: no deps in $ct" >&2; return 1; }
  [ "$have" -gt 0 ] || return 2
  return $bad
}
