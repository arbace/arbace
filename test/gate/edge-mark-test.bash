#!/bin/bash
# GTS113 (b): the run's mark that bin/j2g-edge trace counts by (audit 041): the classifier counts an
# event on a future's thread (the marker frame) and arbace.j2g.model's *run* is conveyed to futures and
# wraps only where it is bound. Needs an Arbace executable: ARBACE_HOST_EXE=EXE test/gate/edge-mark-test.bash
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "edge-mark-test: no executable $exe" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$TMPDIR/edge-mark-test.ae
cat > "$t" <<'F'
(require '[j2g.edge-verdict :as v] '[arbace.j2g.model :as m])
(def results
  [["main frame counted" (v/in-run? ["java.lang.Class.forName" "arbace.j2g.main$run.invoke"] false) true]
   ["future marker counted" (v/in-run? ["java.lang.Class.forName" "arbace.j2g.model$on_run_thread.invoke" "java.util.concurrent.FutureTask.run"] false) true]
   ["executor thread alone not counted" (v/in-run? ["java.lang.Class.forName" "java.util.concurrent.FutureTask.run"] false) false]
   ["truncated counted" (v/in-run? ["x"] true) true]
   ["unbound: f itself" (let [f (fn [] 1)] (identical? f (m/run-fn f))) true]
   ["bound: wrapped" (let [f (fn [] 1)] (binding [m/*run* true] (not (identical? f (m/run-fn f))))) true]
   ["bound: wrapped value" (binding [m/*run* true] ((m/run-fn (fn [] 7)))) 7]
   ["conveyed to a future" (binding [m/*run* true] @(future ((m/run-fn (fn [] m/*run*))))) true]
   ["par-mapv under the binding" (binding [m/*run* true] (m/par-mapv 3 inc [1 2 3 4])) [2 3 4 5]]])
(def bad (remove (fn [[_ got want]] (= got want)) results))
(doseq [[what got want] results] (println (if (= got want) "ok:  " "FAIL:") what))
(System/exit (if (empty? bad) 0 1))
F
ARBACE_PATH=$src:$src/test "$exe" "$t"; st=$?
[ $st = 0 ] && echo "edge-mark-test: passed" || echo "edge-mark-test: failed"
exit $st
