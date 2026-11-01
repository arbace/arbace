#!/bin/bash
# The main checkout and the executables' directory are resolved in one place, bin/lib/gate-cache.bash
# (review 045, 6): bin/lib/gate-run.bash's gate_env and bin/gate keep no second copy of the git
# common-dir test, the .tmp/gate-exe default or the key's 20 characters. Usage: test/gate/one-copy-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
# (bin/gate's clean run reads the common dir for its clone, a different use)
for f in bin/lib/gate-run.bash bin/gate; do
  pats=('main/\.tmp/gate-exe' '\${gkey:0:20}'); [ "$f" = bin/gate ] || pats+=('git-common-dir')
  for pat in "${pats[@]}"; do
    if grep -n -- "$pat" "$here/$f" | grep -v '^[0-9]*:[[:space:]]*#' | grep -q .; then bad "$f still has $pat in code"
    else ok "$f: no $pat in code"; fi
  done
done
grep -q 'arbace_main_checkout "\$root"' "$here/bin/gate" && ok "bin/gate's main_checkout asks the library" || bad "bin/gate's main_checkout is its own"
exit $fail
