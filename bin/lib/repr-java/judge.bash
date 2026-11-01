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
