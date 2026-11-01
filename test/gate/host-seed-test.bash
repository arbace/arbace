#!/bin/bash
# bin/lib/host.bash reads the seed through ARBACE_SEED_GO, as bin/seed does (GTS120).
# Usage: test/gate/host-seed-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
mkdir -p "$TMPDIR"
T=$here/.tmp/host-seed-test
rm -rf "$T"; mkdir -p "$T"
# the fake root a repository of its own: under the checkout's .tmp/, git's common dir would be the checkout's,
# and GTS82 (a) (arbace_host_exe in a linked worktree) would take it for a linked worktree and refuse
git init -q "$T" || { echo "FAIL git init $T"; exit 1; }
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
mkdir -p "$T/bin/lib"; cp "$here"/bin/lib/*.bash "$T/bin/lib/"
r=$T
a=$(root=$T bash -c '. "$root/bin/lib/host.bash"; arbace_host_arch') # the host's architecture, as host.bash names it
# 10. the host reads bin/seed's variable, ARBACE_SEED_GO (relative to the checkout like bin/seed's)
mkdir -p "$r/target/seed/$a"; printf '#!/bin/bash\n' > "$r/target/seed/$a/arbace"; chmod +x "$r/target/seed/$a/arbace"
echo abc > "$r/target/seed/seed.sha256"; : > "$r/alt.tar.xz"; echo "abc  alt.tar.xz" > "$r/alt.tar.xz.sha256"
# without the caller's ARBACE_HOST_EXE (the gate's steps have one): the case is the seed's choice
got=$(env -u ARBACE_HOST_EXE root=$r ARBACE_SEED_GO=alt.tar.xz bash -c ". '$r/bin/lib/host.bash'; arbace_host_exe" 2>&1)
[ "$got" = "$r/target/seed/$a/arbace" ] && ok "host.bash: ARBACE_SEED_GO names the seed" || bad "host.bash gave: $got"

exit $fail
