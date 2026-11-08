#!/bin/bash
# GTS112 (b): every step that uses the frozen JVM (the :frozen host, or a :cmd whose bin/ script sources
# bin/lib/frozen.bash at its top level: bin/oracle) declares the variables that choose
# its jar and class path in :env, so that a pass records them (audit 041). Needs an Arbace executable:
# ARBACE_HOST_EXE=EXE test/gate/frozen-env-test.bash
# needs: exe (an Arbace executable: bin/gate-tests passes it as ARBACE_HOST_EXE)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "frozen-env-test: no executable $exe" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$TMPDIR/frozen-env-test.ae
cat > "$t" <<'F'
(let [steps (read-string (slurp (first *command-line-args*)))
      need #{"ARBACE_FROZEN_JAVA_HOME" "FROZEN_CP" "FROZEN_TAG" "ARBACE_ORACLE_JAR" "ARBACE_ORACLE_TAG"}
      steps (filter map? (if (map? steps) (:steps steps) steps))
      ;; bin/oracle and bin/j2g-edge themselves, not bin/oracle-records (which sources no frozen.bash): \b matched before its -
      sources? (fn [s] (re-find #"bin/(oracle|j2g-edge)(?![\w-])" (str (:cmd s))))
      frozen (filter #(or (some #{:frozen} (:hosts %)) (sources? %)) steps)
      bad (remove #(every? (set (:env %)) need) frozen)]
  (println (count frozen) "steps use the frozen JVM:" (apply str (interpose " " (map :name frozen))))
  (doseq [s bad] (println "FAIL:" (:name s) "lacks" (remove (set (:env s)) need)))
  (System/exit (if (or (empty? frozen) (seq bad)) 1 0)))
F
"$exe" "$t" "$src/test/gate/steps.edn"; st=$?
[ $st = 0 ] && echo "frozen-env-test: passed" || echo "frozen-env-test: failed"
exit $st
