# Helpers of repr/java's judges (GT7's audit 061, doc/GATE-SURVEY.md, GTS130-): small functions the check
# scripts source (bin/lib/repr-java/{parse-check,front-check}, test/repr/java/jdk.bash) so that their verdicts
# can be tested over fabricated trees and a stubbed tool (test/repr/java/judge-*-test.bash). Sourced; defines:
#
#   judge_flat_get FILE KEY        the value after KEY in FILE, a flat Clojure map {KEY VALUE ...} (';' comments,
#                                  braces and whitespace only: read by awk, no tool needed); nothing when absent
#   judge_canon DIR                DIR's canonical path (pwd -P): what read-jdk writes into a converted file's
#                                  header (arbace/repr/java.ae, `.getCanonicalPath`); status 1 when no directory

judge_flat_get() {
  awk -v key="$2" '
    { sub(/;.*/, ""); gsub(/[{}]/, " "); n = split($0, t, /[ \t,]+/)
      for (i = 1; i <= n; i++) if (t[i] != "") w[++c] = t[i] }
    END { for (i = 1; i < c; i += 2) if (w[i] == key) { print w[i + 1]; exit } }' "$1"
}

judge_canon() {
  [ -d "$1" ] || { echo "judge_canon: no directory $1" >&2; return 1; }
  (cd "$1" && pwd -P)
}
