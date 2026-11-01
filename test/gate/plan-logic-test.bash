#!/bin/bash
# The gate's logic that every step's key holds (test/gate/plan.ae's `logic`, GTS22 (a)) names every library
# gate_env sources: bin/lib/gate-run.bash and bin/lib/gate-cache.bash (review 045, 11).
# Usage: test/gate/plan-logic-test.bash
here=$(cd "$(dirname "$0")/../.." && pwd)
if grep -q 'logic \["bin/lib/gate-run.bash" "bin/lib/gate-cache.bash"' "$here/test/gate/plan.ae"; then echo "ok   plan.ae's logic names bin/lib/gate-cache.bash"; else echo "FAIL plan.ae's logic lacks bin/lib/gate-cache.bash"; exit 1; fi
