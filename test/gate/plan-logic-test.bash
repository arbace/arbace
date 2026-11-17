#!/bin/bash
# The gate's logic that every step's key holds (test/gate/plan.ae's `logic`, GTS22 (a)): since GD3 (GDS11 (a); doc/GATE-DIET-SURVEY.md,
# "As built: GD3") the epoch file test/gate/epoch alone, in place of the libraries gate_env sources (bin/lib/gate-run.bash and
# bin/lib/gate-cache.bash, review 045, 11) and the planner, whose changes bump it by hand.
# Usage: test/gate/plan-logic-test.bash
here=$(cd "$(dirname "$0")/../.." && pwd)
if grep -q "logic \[epoch-file\]" "$here/test/gate/plan.ae" && grep -q "\"test/gate/epoch\"" "$here/test/gate/plan.ae" && [ -f "$here/test/gate/epoch" ]; then echo "ok   plan.ae's logic is the epoch, test/gate/epoch (GDS11 (a))"; else echo "FAIL plan.ae's logic is not the epoch test/gate/epoch"; exit 1; fi
