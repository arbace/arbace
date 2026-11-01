#!/bin/bash
# bin/j2g-edge trace's verdict (test/j2g/edge_verdict.ae) on an Arbace executable (audit 041, finding 4):
# a recording with no traced event fails. By hand: ARBACE_HOST_EXE=EXE test/gate/edge-verdict-test.bash
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "edge-verdict-test: no executable $exe" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$TMPDIR/edge-verdict-test.ae
cat > "$t" <<'F'
(require '[j2g.edge-verdict :as v])
(def cases [[0 0 true "no event"] [5 0 false "events, none by j2g"] [5 2 true "calls by j2g"] [0 3 true "both"]])
(def bad (remove (fn [[total n want]] (= want (v/failed? total n))) cases))
(doseq [[total n want what] cases]
  (println (if (= want (v/failed? total n)) "ok:  " "FAIL:") what))
(println (if (and (v/message 0) (nil? (v/message 4))) "ok:   message" "FAIL: message"))
(System/exit (if (and (empty? bad) (v/message 0) (nil? (v/message 4))) 0 1))
F
ARBACE_PATH=$src/test "$exe" "$t"; st=$?
[ $st = 0 ] && echo "edge-verdict-test: passed" || echo "edge-verdict-test: failed"
exit $st
