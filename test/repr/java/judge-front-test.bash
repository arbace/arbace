#!/bin/bash
# Tests of front-check's counts and its known files' format (GT7's audit 061, findings 6 and 7). No javac tool.
# Run: bash test/repr/java/judge-front-test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/test/repr/java/judge-lib.bash"
t=${TMPDIR:-$root/.tmp/tmp}/judge-front-test.$$
rm -rf "$t"; mkdir -p "$t"; trap 'rm -rf "$t"' EXIT
. "$root/bin/lib/repr-java/judge.bash"

# finding 6: one forms file a side is one file from javac's trees, whatever the paths hold
w=$t/work
for side in javac native; do
  mkdir -p "$w/javac/jdk.compiler/$side/com/sun/tools/javac/code" "$w/jdk26u/jdk.compiler/$side/com/sun/tools/javac/code" "$w/samples/java/$side"
  touch "$w/javac/jdk.compiler/$side/com/sun/tools/javac/code/Symbol.ae" "$w/jdk26u/jdk.compiler/$side/com/sun/tools/javac/code/Symbol.ae" "$w/samples/java/$side/A.ae"
done
touch "$w/samples/java/javac/repr-java-split.edn"
for c in samples javac jdk26u; do
  n=$(judge_count_javac_forms "$w" "$c"); [ "$n" = 1 ] && ok "$c: 1 file a side counts 1" || bad "$c: counted $n, wanted 1"
done
n=$(judge_count_javac_forms "$w" jdk); [ "$n" = 0 ] && ok "a corpus not read counts 0" || bad "a corpus not read counts $n"
grep -q 'judge_count_javac_forms' "$root/bin/lib/repr-java/front-check" && ok "front-check counts with it" || bad "front-check counts with it"

# finding 7: attr-check's known file admits no bridged read (split.ae counts none)
printf ';; comment :bridges #{x}\n{:bridges #{}}\n' > "$t/k0.edn"
printf '{:bridges #{:model/type}}\n' > "$t/k1.edn"
printf '{:bridges #{ :a\n :b }\n :m {}}\n' > "$t/k2.edn"
printf '{}\n' > "$t/k3.edn"
judge_bridges_empty "$t/k0.edn" 2> "$t/stderr"; expect_rc "bridges empty (a comment names one)" 0 $?
judge_bridges_empty "$t/k1.edn" 2> "$t/stderr"; expect_rc "one bridged read listed: refused" 1 $?
judge_bridges_empty "$t/k2.edn" 2> "$t/stderr"; expect_rc "several, over lines: refused" 1 $?
judge_bridges_empty "$t/k3.edn" 2> "$t/stderr"; expect_rc "no :bridges key" 0 $?
judge_bridges_empty "$root/test/repr/java/attr-check-known.edn" 2> "$t/stderr"; expect_rc "the committed attr-check known file" 0 $?
grep -q 'judge_bridges_empty "\$known"' "$root/bin/lib/repr-java/front-check" && ok "front-check's attr mode calls it" || bad "front-check's attr mode calls it"
finish
