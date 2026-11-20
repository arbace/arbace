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

# GDS30 (a), the 11-20 audit: attr-check's pass implies front-check's while its known file admits no differing fact and no
# differing forms file front-check's does not (repr-java-front's samples, baseline and javac parts dropped on that condition)
. "$root/bin/lib/tools.bash"
printf '{}\n' > "$t/f0.edn"
printf '{:javac {:forms #{"a/B.ae"}}}\n' > "$t/f1.edn"
printf '{:bridges #{}\n :samples {:declined 3}}\n' > "$t/a0.edn"
printf '{:samples {:declined 3 :facts {:expr/type 2}}}\n' > "$t/a1.edn"
printf '{:samples {:facts {:expr/type 0}}}\n' > "$t/a2.edn"
printf '{:javac {:forms #{"a/B.ae"}}}\n' > "$t/a3.edn"
judge_front_implied "$t/a0.edn" "$t/f0.edn" 2> "$t/stderr"; expect_rc "declines alone: implied" 0 $?
judge_front_implied "$t/a1.edn" "$t/f0.edn" 2> "$t/stderr"; expect_rc "a differing fact admitted: refused" 1 $?
grep -q 'no longer implies' "$t/stderr" && ok "the refusal says why" || bad "the refusal: $(cat "$t/stderr")"
judge_front_implied "$t/a2.edn" "$t/f0.edn" 2> "$t/stderr"; expect_rc "a fact at count 0: implied" 0 $?
judge_front_implied "$t/a3.edn" "$t/f0.edn" 2> "$t/stderr"; expect_rc "a differing forms file front-check's known file lacks: refused" 1 $?
judge_front_implied "$t/a3.edn" "$t/f1.edn" 2> "$t/stderr"; expect_rc "a differing forms file both known files admit: implied" 0 $?
judge_front_implied "$root/test/repr/java/attr-check-known.edn" "$root/test/repr/java/front-check-known.edn" 2> "$t/stderr"; expect_rc "the committed known files" 0 $?
grep -q 'judge_front_implied "\$known"' "$root/bin/lib/repr-java/front-check" && ok "front-check's attr mode calls it" || bad "front-check's attr mode calls judge_front_implied"
finish
