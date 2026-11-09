#!/bin/bash
# The framework runner's self-test (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS331 (a), "As built: JCS330-JCS333"): the
# runner (test/java/runner/org/arbace/jtreg/Runner.java) is the judge of every framework test (its status the test's own
# verdict), so its negative controls are its own: test/java/runner-test/'s fixtures, TestNG, JUnit 5 and JUnit 4 classes
# that pass and that fail in each way the runner must fail (a test's failure, an expected exception not thrown, a
# configuration or container failure, no test at all), each run by the runner and its status held to
# test/java/runner-test/expected.txt (0 the run passes, 1 it fails). A runner that passes everything fails this test.
#
#   test/java/runner-test.bash [--jdk | --read]
#   test/java/runner-test.bash --forms verify|update
#
# Default (JCS338 (a): the essential gate's java-runner-test): on Arbace, as bin/java-check's interp level runs a test,
# from the committed forms of the fixtures and the runner (test/java/runner-test/forms/), each class run by
# test/java/run.ae on the executable (JAVA_CHECK_EXE, required); --read: the forms read afresh first by repr/java on
# javac's tool executable (bin/repr-java read; REPR_JAVA_EXE); --jdk: the JDK's javac and java (the oracle tier's, by
# hand: bin/lib/java26.bash). --forms update writes the committed forms (read as --read reads them, each file's first
# line naming its source in the checkout, provenance.edn the converter's as test/java/forms' is), --forms verify
# reads afresh and compares, byte for byte (the provenance by repr_java_provenance_same: its :host apart; the --full
# java-forms-verify's). Scratch in .tmp/runner-test. Exit 1 when a status is not the expected one, or the forms differ.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
t=$root/.tmp/runner-test fx=$root/test/java/runner-test rn=$root/test/java/runner
rm -rf "${t:?}"; mkdir -p "$t/src" "$t/out"
cp "$fx"/*.java "$t/src/"
cp -r "$rn"/org "$t/src/"
committed=$fx/forms
# read_forms OUT: the fixtures and the runner read by repr/java into OUT, each file's first line naming its source in
# the checkout (test/java/runner-test/F.java, test/java/runner/...), not the scratch copy
read_forms() {
  local f src
  (cd "$root" && bin/repr-java read "$1" "$t/src") > "$t/read.log" 2>&1 || { echo "runner-test: the read failed"; tail -5 "$t/read.log"; return 2; }
  find "$1" -name '*.ae' | LC_ALL=C sort > "$t/forms.list"
  while read -r f; do
    src=$(sed -n "1s|^;; Converted from $t/src/\(.*\) by arbace\.repr\.java\..*|\1|p" "$f")
    [ -n "$src" ] || continue
    case $src in org/*) src=test/java/runner/$src ;; *) src=test/java/runner-test/$src ;; esac
    sed -i "1s|^;; Converted from .* by arbace\.repr\.java\.|;; Converted from $src by arbace.repr.java.|" "$f"
  done < "$t/forms.list"
  if grep -rlF "$root" "$1" | head -3 | grep .; then echo "runner-test: the forms above name the checkout" >&2; return 2; fi
  . "$root/bin/lib/repr-java/host.bash"
  {
    echo ";; The provenance of test/java/runner-test/forms/ (test/java/runner-test.bash --forms): the framework runner's"
    echo ";; fixtures and the runner (test/java/runner/), read by repr/java (JCS338 (a))."
    echo "{:jdk \"$(repr_java_platform)\""
    echo " :converter {:repr-java \"$(repr_java_reader_hash)\""
    echo "             :host \"$(repr_java_host)\"}"
    echo " :files $(find "$1" -name '*.ae' | wc -l)}"
  } > "$1/provenance.edn"
}
mode=arbace
case ${1:-} in
  --jdk) mode=jdk ;;
  --read) mode=read ;;
  --forms)
    read_forms "$t/forms" || exit 2
    case ${2:-} in
      update) rm -rf "$committed"; cp -r "$t/forms" "$committed"
              echo "runner-test --forms update: $committed written ($(find "$committed" -name '*.ae' | wc -l) files): commit it"; exit 0 ;;
      verify) . "$root/bin/lib/repr-java/host.bash"
              if diff -r -x provenance.edn "$committed" "$t/forms" > "$t/forms.diff" 2>&1 &&
                 repr_java_provenance_same "$committed/provenance.edn" "$t/forms/provenance.edn"; then
                echo "runner-test --forms verify: test/java/runner-test/forms is the conversion, byte for byte"; exit 0
              fi
              diff -r "$committed" "$t/forms" > "$t/forms.diff" 2>&1
              echo "runner-test --forms verify: test/java/runner-test/forms differs from a fresh conversion ($t/forms.diff)"; head -20 "$t/forms.diff"; exit 1 ;;
      *) echo "usage: test/java/runner-test.bash --forms verify|update" >&2; exit 2 ;;
    esac ;;
esac
if [ $mode = jdk ]; then
  . "$root/bin/lib/java26.bash"; arbace_use_java26 || exit 2
  (cd "$t/src" && javac -J-Xmx512m -nowarn -d "$t/out" $(find . -name '*.java')) > "$t/javac.log" 2>&1 || { echo "runner-test: javac failed"; head -5 "$t/javac.log"; exit 2; }
else
  : "${JAVA_CHECK_EXE:?runner-test: JAVA_CHECK_EXE names the executable that interprets}"
  if [ $mode = read ]; then read_forms "$t/forms" || exit 2
  else [ -f "$committed/provenance.edn" ] || { echo "runner-test: no committed forms $committed (--forms update)"; exit 2; }
       rm -rf "$t/forms"; cp -r "$committed" "$t/forms"; fi
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
