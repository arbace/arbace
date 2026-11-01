#!/bin/bash
# Tests of the judges' view of jdk26u's checkout (GT7's audit 061, finding 5): JDK_SRC and JDK one tree, and the four
# tree-check steps of test/gate/steps.edn keying both. Run: bash test/repr/java/judge-env-test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/test/repr/java/judge-lib.bash"
t=${TMPDIR:-$root/.tmp/tmp}/judge-env-test.$$
rm -rf "$t"; mkdir -p "$t"; trap 'rm -rf "$t"' EXIT
. "$root/bin/lib/repr-java/judge.bash"
mkdir -p "$t/a/src" "$t/b/src"

# judge_jdk_paths: each defaulted from the other, a split refused
r=$(unset JDK; JDK_SRC=$t/a/src; judge_jdk_paths && echo "$jdk"); [ "$r" = "$t/a" ] && ok "JDK from JDK_SRC" || bad "JDK from JDK_SRC ($r)"
r=$(unset JDK_SRC; JDK=$t/b; judge_jdk_paths && echo "$jdk_src"); [ "$r" = "$t/b/src" ] && ok "JDK_SRC from JDK" || bad "JDK_SRC from JDK ($r)"
r=$(unset JDK JDK_SRC; judge_jdk_paths && echo "$jdk_src $jdk"); [ "$r" = "/root/jdk26u/src /root/jdk26u" ] && ok "the defaults" || bad "the defaults ($r)"
(JDK_SRC=$t/a/src JDK=$t/a judge_jdk_paths 2> /dev/null); expect_rc "both set, one tree" 0 $?
ln -s "$t/a" "$t/alink"
(JDK_SRC=$t/a/src/ JDK=$t/alink judge_jdk_paths 2> /dev/null); expect_rc "both set, one tree through a symlink and a slash" 0 $?
(JDK_SRC=$t/a/src JDK=$t/b judge_jdk_paths 2> "$t/err"); expect_rc "both set, two trees: refused" 1 $?
grep -q 'two trees' "$t/err" && ok "the refusal says two trees" || bad "the refusal says two trees"
r=$(JDK_SRC=$t/a/src; unset JDK; judge_jdk_paths; echo "$JDK"); [ "$r" = "$t/a" ] && ok "JDK exported for the originals" || bad "JDK exported for the originals ($r)"

# parse-check refuses a split (the stubbed checkout of judge-lib)
make_fake_parse_root "$t/r"
(cd "$t/r" && JDK_SRC=$t/r/jdk/src JDK=$t/b REPR_JAVA_PARSE_WORK="$t/work" bash bin/lib/repr-java/parse-check --corpus samples > "$t/out.log" 2>&1); expect_rc "parse-check with JDK_SRC and JDK on two trees fails" 1 $?
(cd "$t/r" && JDK_SRC=$t/r/jdk/src JDK=$t/r/jdk REPR_JAVA_PARSE_WORK="$t/work" bash bin/lib/repr-java/parse-check --corpus samples > "$t/out.log" 2>&1); expect_rc "parse-check with one tree" 0 $?

# the steps key the environment
for s in repr-java-parse repr-java-front repr-java-attr repr-java-nojavac; do
  awk -v s="$s" '$0 ~ "^  \\{:name " s " " {on = 1; next} on && /^  (\{:name|;;)/ {on = 0}
                 on && /:env \["JDK_SRC" "JDK"\]/ {f = 1} END {exit f ? 0 : 1}' "$root/test/gate/steps.edn"
  expect_rc "step $s keys JDK_SRC and JDK" 0 $?
done

# review 067 C: bin/gate --pins pins the commit of the tree the judges read
r=$(unset JDK; JDK_SRC=$t/a/src; judge_jdk_root); [ "$r" = "$t/a" ] && ok "judge_jdk_root from JDK_SRC alone" || bad "judge_jdk_root from JDK_SRC alone ($r)"
(JDK_SRC=$t/a/src JDK=$t/b judge_jdk_root > "$t/o" 2> /dev/null); expect_rc "judge_jdk_root refuses a split" 1 $?
grep -q 'judge_jdk_root' "$root/bin/gate" && ok "bin/gate --pins uses it" || bad "bin/gate --pins uses it"
# review 067 G: the gate's summary shows the new parse-check verdicts
awk '/^  \{:name repr-java-parse /{on=1} on && /:summary/ {f = ($0 ~ /javac diagnostics/ && $0 ~ /mutants: the set/); exit} END {exit f ? 0 : 1}' "$root/test/gate/steps.edn"
expect_rc "repr-java-parse's summary matches the diagnostics and mutants verdicts" 0 $?
finish
