#!/bin/bash
# bin/lib/hsi/check's wall_cap (smalls-1123, the VM's queue 541, finding 1): the wall cap a backstop, three times the CPU limit on an
# idle host, times the load per core on a loaded one; with its negative control, the old fixed rule (three times) on the VM's loaded
# host, which must fall short of the wall time a run under its CPU cap took there. Run by hand: bash test/haskell/hsi/wall-cap-test.bash
set -u
root=$(cd "$(dirname "$0")/../../.." && pwd)
eval "$(sed -n '/^wall_cap() {/,/^}/p' "$root/bin/lib/hsi/check")"
unset HSI_WALL_FACTOR
bad=0
t() { local got; got=$(wall_cap "$1" "$2" "$3"); if [ "$got" = "$4" ]; then echo "ok   wall_cap $1 at load $2 on $3 cores: $got"; else echo "FAIL wall_cap $1 at load $2 on $3 cores: $got, not $4"; bad=1; fi; }
t 60 0.5 64 180      # the main host idle: the old rule's 3 x
t 60 64 64 180       # a load of one a core: still 3 x
t 60 90 64 360       # the main host loaded (load 90 on 64 cores): 6 x
t 510 43 8 9180      # the VM (load 43 on 8 cores): 18 x
# the run the VM recorded: secretary at its CPU cap 510 s, 321 s of CPU by 1,530 s of wall (a fifth of a core): it needed 510 / 0.21 s
need=$(awk 'BEGIN {printf "%d", 510 / (321 / 1530)}')
[ "$(wall_cap 510 43 8)" -ge "$need" ] && echo "ok   the VM's secretary within the cap (needs $need s)" || { echo "FAIL the VM's secretary needs $need s"; bad=1; }
# the negative control: the old rule (3 x, whatever the load) falls short of it, so it must fail this
[ $((510 * 3)) -lt "$need" ] && echo "ok   control: the old rule's $((510 * 3)) s falls short, as it must" || { echo "FAIL control: the old rule would pass"; bad=1; }
HSI_WALL_FACTOR=5; [ "$(wall_cap 60 43 8)" = 300 ] && echo "ok   HSI_WALL_FACTOR=5: 300" || { echo "FAIL HSI_WALL_FACTOR"; bad=1; }
exit $bad
