#!/bin/bash
# The night's switch of a library and framework corpus (doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS337 (a), JCS338 (a),
# "As built: JCS334-JCS339"): the night that records the corpora of night N (test/java/corpora.edn's :night N) runs
#
#   test/java/night-corpora.bash N [--dry]
#
# before its --record-ref: test/java/corpora.edn's :night raised to N (java.check/active-corpora: the scan then takes
# them), and test/gate/steps.edn given those corpora as parts of the Java level steps that run every corpus
# (java-read, java-interp, java-classes, java-compiled-full, java-print: {:part ID}, appended to each step's :parts);
# and, the first time (JCS338 (a)), the essential step java-runner-test (test/java/runner-test.bash from its committed
# forms on the gate's executable: the framework runner's negative controls) after java-check-judge, with
# java-forms-verify verifying those forms too. Idempotent: a part or step already there is left. --dry prints the diff
# and changes nothing. Then commit the two files with the night's records.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
n=${1:?usage: test/java/night-corpora.bash N [--dry]}
dry=${2:-}
[[ $n =~ ^[0-9]+$ ]] || { echo "night-corpora: N is a number" >&2; exit 2; }
corpora=$root/test/java/corpora.edn steps=$root/test/gate/steps.edn
t=$root/.tmp/night-corpora; rm -rf "$t"; mkdir -p "$t"
# the corpora of night N, in the file's order (each corpus map's :id line, then its :night line)
awk -v n="$n" '/^  \{:id :/ { id = $2; sub(/^:/, "", id) } $0 ~ "^   :night " n "}?$" && id != "" { print id; id = "" }' "$corpora" > "$t/ids"
[ -s "$t/ids" ] || { echo "night-corpora: no corpus of night $n in test/java/corpora.edn" >&2; exit 1; }
echo "night-corpora: night $n: $(paste -sd' ' "$t/ids")"
sed -E "s/^\{:night [0-9]+$/{:night $n/" "$corpora" > "$t/corpora.edn"
grep -q "^{:night $n\$" "$t/corpora.edn" || { echo "night-corpora: no top-level :night line in test/java/corpora.edn" >&2; exit 1; }
# the parts: in each level step, the :parts block's last line (ending "}]") gets a continuation line of the new parts
awk -v ids="$(paste -sd' ' "$t/ids")" '
  BEGIN { split("java-read java-interp java-classes java-compiled-full java-print", s, " "); for (i in s) want[s[i]] = 1
          ni = split(ids, id, " ") }
  function flush(   i, add, l) {
    if (nb == 0) return
    add = ""
    for (i = 1; i <= ni; i++) if (index(blk, "{:part " id[i] "}") == 0) add = add (add == "" ? "" : " ") "{:part " id[i] "}"
    for (i = 1; i < nb; i++) print b[i]
    l = b[nb]
    if (add != "") { sub(/\]$/, "", l); print l; print "           " add "]" } else print l
    nb = 0; blk = ""
  }
  /^  \{:name / { flush(); name = $2; inparts = 0 }
  {
    if (want[name] && ($0 ~ /^   :parts \[/ || (inparts && $0 ~ /^           \{:part /))) {
      inparts = 1; b[++nb] = $0; blk = blk $0
      if ($0 ~ /\}\]$/) { flush(); inparts = 0 }
      next
    }
    flush(); inparts = 0; print
  }
  END { flush() }' "$steps" > "$t/steps.1"
# JCS338 (a): the runner's essential step, once
if grep -q '^  {:name java-runner-test ' "$t/steps.1"; then cp "$t/steps.1" "$t/steps.edn"
else
  awk '
    /^  \{:name java-check-judge / { judge = 1 }
    { print }
    judge && /^   :summary .*\]\}$/ {
      print "  ;; JCS338 (a) (doc/classes/JAVA-CONFORMANCE-SURVEY.md, \"As built: JCS334-JCS339\"): the framework runner'"'"'s own negative"
      print "  ;; controls, ten fixtures run by the runner from their committed forms on the executable, each status held to"
      print "  ;; test/java/runner-test/expected.txt (about 20 s); java-forms-verify reads the forms afresh"
      print "  {:name java-runner-test :mem-gib 1 :phase :essential :chain java-essential :needs [go-build]"
      print "   :cmd \"env JAVA_CHECK_EXE=\\\"$GATE_EXE\\\" test/java/runner-test.bash\""
      print "   :hosts [:prog] :never-loads [arbace.j2g]"
      print "   :inputs [:java-run \"test/java/runner-test.bash\" \"test/java/runner-test/\" \"test/java/run.ae\"]"
      print "   :summary [\"^runner-test|^FAIL\" 3]}"
      judge = 0
    }' "$t/steps.1" > "$t/steps.2"
  # java-forms-verify: the runner's forms verified beside the essential tests'
  sed -E '/^  \{:name java-forms-verify /,/^   :summary /{
            s#^(   :cmd "env REPR_JAVA_EXE=\\"\$GATE_JAVAC\\" JAVA_CHECK_FORMS=\\"\$GATE_JAVA_FORMS\\" bin/java-check --forms verify)"$#\1 \&\& env REPR_JAVA_EXE=\\"$GATE_JAVAC\\" test/java/runner-test.bash --forms verify"#
            s#^(   :inputs \[:tools :java-run :repr-java )#\1"test/java/runner-test.bash" "test/java/runner-test/" #
          }' "$t/steps.2" > "$t/steps.edn"
  grep -q 'runner-test.bash --forms verify' "$t/steps.edn" || { echo "night-corpora: java-forms-verify's :cmd is not the one this script knows; add the runner's verify by hand" >&2; exit 1; }
fi
diff -u "$corpora" "$t/corpora.edn" | sed "s|$root/||g" || true
diff -u "$steps" "$t/steps.edn" | sed "s|$root/||g" || true
[ "$dry" = --dry ] && { echo "night-corpora: --dry, nothing changed"; exit 0; }
cp "$t/corpora.edn" "$corpora"; cp "$t/steps.edn" "$steps"
echo "night-corpora: test/java/corpora.edn's :night is $n, test/gate/steps.edn has its parts; check with bin/gate --full --plan"
