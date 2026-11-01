# Helpers of repr/java's judges (GT7's audit 061, doc/GATE-SURVEY.md, GTS130-): small functions the check
# scripts source (bin/lib/repr-java/{parse-check,front-check}, test/repr/java/jdk.bash) so that their verdicts
# can be tested over fabricated trees and a stubbed tool (test/repr/java/judge-*-test.bash). Sourced; defines:
#
#   judge_flat_get FILE KEY        the value after KEY in FILE, a flat Clojure map {KEY VALUE ...} (';' comments,
#                                  braces and whitespace only: read by awk, no tool needed); nothing when absent
#   judge_flat_set FILE KEY VALUE  FILE's map with KEY set (comments kept, entries sorted by key, one per line)
#   judge_canon DIR                DIR's canonical path (pwd -P): what read-jdk writes into a converted file's
#                                  header (arbace/repr/java.ae, `.getCanonicalPath`); status 1 when no directory

judge_flat_get() {
  awk -v key="$2" '
    { sub(/;.*/, ""); gsub(/[{}]/, " "); n = split($0, t, /[ \t,]+/)
      for (i = 1; i <= n; i++) if (t[i] != "") w[++c] = t[i] }
    END { for (i = 1; i < c; i += 2) if (w[i] == key) { v = w[i + 1]; gsub(/^"|"$/, "", v); print v; exit } }' "$1"
}

judge_canon() {
  [ -d "$1" ] || { echo "judge_canon: no directory $1" >&2; return 1; }
  (cd "$1" && pwd -P)
}

# judge_flat_set FILE KEY VALUE: FILE's flat map with KEY set to VALUE (a string value written by the caller
# with its quotes), the file's leading ';;' comment lines kept, entries one per line sorted by key
judge_flat_set() {
  local f=$1 k=$2 v=$3 tmp
  tmp=$f.new.$$
  { [ -f "$f" ] && grep '^;;' "$f"
    echo "{"
    { [ -f "$f" ] && awk -v key="$k" '{ sub(/;.*/, ""); gsub(/[{}]/, " "); n = split($0, t, /[ \t,]+/)
        for (i = 1; i <= n; i++) if (t[i] != "") w[++c] = t[i] }
      END { for (i = 1; i < c; i += 2) if (w[i] != key) print w[i], w[i + 1] }' "$f"
      echo "$k $v"; } | LC_ALL=C sort | sed 's/^/ /'
    echo "}"; } > "$tmp" && mv "$tmp" "$f"
}

# judge_tree_digest DIR: the SHA-256 of DIR's sorted "sha256 path" listing (every file below it), and the count:
# "DIGEST COUNT". What identifies a generated corpus (the mutants) by content.
judge_tree_digest() {
  local l d n
  l=$(cd "$1" && find . -type f | LC_ALL=C sort | xargs -r sha256sum) || return 1
  d=$(printf '%s\n' "$l" | sha256sum | cut -c1-64)
  n=$(printf '%s\n' "$l" | grep -c .)
  echo "$d $n"
}

# judge_jdk_paths: jdk26u's checkout as the judges and the originals see it, one tree (GT7's audit 061 #5).
# JDK_SRC (the src directory) and JDK (the checkout: the originals, the build tree) are each defaulted from the
# other (JDK_SRC = JDK/src, JDK = dirname JDK_SRC; else /root/jdk26u), and refused when both are set and not one
# tree. Sets and exports jdk_src, jdk, JDK_SRC and JDK (the originals' functions read JDK); status 1 on a split.
judge_jdk_paths() {
  local s=${JDK_SRC:-} j=${JDK:-} cs cj c0
  [ -n "$s" ] || { if [ -n "$j" ]; then s=$j/src; else s=/root/jdk26u/src; fi; }
  c0=$(cd "$s" 2>&1 && pwd -P) || c0=$s
  [ -n "$j" ] || j=$(dirname "$c0")
  cs=$(dirname "$c0")
  cj=$(cd "$j" 2>&1 && pwd -P) || cj=$j
  if [ "$cs" != "$cj" ]; then
    echo "JDK_SRC ($s) is not JDK/src (JDK=$j): the corpora would come from two trees" >&2
    return 1
  fi
  jdk_src=$s jdk=$j
  export JDK_SRC=$s JDK=$j
}

# judge_count_javac_forms WORK CORPUS: the forms files of javac's side of a front-check corpus: the .ae files
# below WORK/CORPUS/*/javac (read_both and read_jdk write DIR/javac and DIR/native one level under the corpus), not
# every path with a javac component (the javac corpus and jdk.compiler's com/sun/tools/javac hold native files
# on such paths too, GT7's audit 061 #6)
judge_count_javac_forms() {
  local r
  for r in "$1/$2"/*/javac; do [ -d "$r" ] && find "$r" -name '*.ae'; done | grep -c .
}

# judge_bridges_empty FILE: attr-check's known file admits no bridged read. split.ae admits a read bridged to
# javac's symbol by name alone, in every corpus and any number of times (:bridges #{read ...}); until it counts them
# per corpus, as the :facts are, one listed read would hide any number of bridges, so the set must stay empty
# (GT7's audit 061 #7). Status 1 and a message when it is not.
judge_bridges_empty() {
  local b
  b=$(sed 's/;.*//' "$1" | tr '\n' ' ' | grep -o ':bridges *#{[^}]*}' | head -1)
  case $b in
    ''|':bridges #{}'|':bridges #{ }') return 0 ;;
    *) echo "$1: :bridges is not empty ($b): a listed read admits any number of bridges in every corpus (split.ae counts none); count them per corpus first" >&2; return 1 ;;
  esac
}

# judge_jdk_root: the jdk26u checkout the judges read (judge_jdk_paths' $jdk), for bin/gate --pins, so that the
# pinned commit is the one of the tree the checks read (review 067 C); status 1 and the refusal on a split
judge_jdk_root() { (judge_jdk_paths && echo "$jdk"); }
