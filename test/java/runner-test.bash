#!/bin/bash
# The framework runner's self-test (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a), "As built: JCS330-JCS333"): the
# runner (test/java/runner/org/arbace/jtreg/Runner.java) is the judge of every framework test (its status the test's own
# verdict), so its negative controls are its own: test/java/runner-test/'s fixtures, TestNG, JUnit 5 and JUnit 4 classes
# that pass and that fail in each way the runner must fail (a test's failure, an expected exception not thrown, a
# configuration or container failure, no test at all), each run by the runner and its status held to
# test/java/runner-test/expected.txt (0 the run passes, 1 it fails). A runner that passes everything fails this test.
#
#   test/java/runner-test.bash [--jdk]
#
# Default: on Arbace, as bin/java-check's interp level runs a test: the fixtures and the runner read into forms by
# repr/java on javac's tool executable (bin/repr-java read; REPR_JAVA_EXE), each class run by test/java/run.ae on the
# executable (JAVA_CHECK_EXE, required); --jdk: the JDK's javac and java (the oracle tier's, by hand: bin/lib/java26.bash).
# Scratch in .tmp/runner-test. Exit 1 when a status is not the expected one.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
t=$root/.tmp/runner-test fx=$root/test/java/runner-test rn=$root/test/java/runner
rm -rf "${t:?}"; mkdir -p "$t/src" "$t/out"
cp "$fx"/*.java "$t/src/"
cp -r "$rn"/org "$t/src/"
mode=arbace; [ "${1:-}" = --jdk ] && mode=jdk
if [ $mode = jdk ]; then
  . "$root/bin/lib/java26.bash"; arbace_use_java26 || exit 2
  (cd "$t/src" && javac -J-Xmx512m -nowarn -d "$t/out" $(find . -name '*.java')) > "$t/javac.log" 2>&1 || { echo "runner-test: javac failed"; head -5 "$t/javac.log"; exit 2; }
else
  : "${JAVA_CHECK_EXE:?runner-test: JAVA_CHECK_EXE names the executable that interprets}"
  (cd "$root" && bin/repr-java read "$t/forms" "$t/src") > "$t/read.log" 2>&1 || { echo "runner-test: the read failed"; tail -5 "$t/read.log"; exit 2; }
fi
n=0 bad=0
while read -r cls fw want; do
  case $cls in ''|\#*) continue ;; esac
  n=$((n + 1)); st=0
  if [ $mode = jdk ]; then
    timeout 60 java -Xmx512m -cp "$t/out" org.arbace.jtreg.Runner "$fw" "$cls" > "$t/$cls.out" 2> "$t/$cls.err" || st=$?
  else
    (cd "$t" && ARBACE_PATH=$t/forms timeout 120 "$JAVA_CHECK_EXE" "$root/test/java/run.ae" --load files "$t/forms" org.arbace.jtreg.Runner -- "$fw" "$cls") \
      > "$t/$cls.out" 2> "$t/$cls.err" || st=$?
  fi
  got=$st; [ "$st" = 0 ] || got=1
  [ "$st" = 126 ] && got="126 (load-failed: $(grep -m1 'load-failed' "$t/$cls.err" | cut -c1-160))"
  if [ "$got" = "$want" ]; then echo "ok   $cls ($fw): status $st, $(grep -m1 '^Total tests run' "$t/$cls.out")"
  else bad=$((bad + 1)); echo "FAIL $cls ($fw): status $got, expected $want: $(grep -m1 -E 'Exception|Error|load-failed' "$t/$cls.err" | cut -c1-200)"; fi
done < "$fx/expected.txt"
echo "runner-test ($mode): $n fixtures, $((n - bad)) as expected, $bad not"
[ "$bad" = 0 ]
