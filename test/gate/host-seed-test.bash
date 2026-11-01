#!/bin/bash
# bin/lib/host.bash reads the seed through ARBACE_SEED_GO, as bin/seed does (GTS120).
# Usage: test/gate/host-seed-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
mkdir -p "$TMPDIR"
T=$here/.tmp/host-seed-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
mkdir -p "$T/bin/lib"; cp "$here"/bin/lib/*.bash "$T/bin/lib/"
r=$T
# 10. the host reads bin/seed's variable, ARBACE_SEED_GO (relative to the checkout like bin/seed's)
mkdir -p "$r/target/seed/amd64"; printf '#!/bin/bash\n' > "$r/target/seed/amd64/arbace"; chmod +x "$r/target/seed/amd64/arbace"
echo abc > "$r/target/seed/seed.sha256"; : > "$r/alt.tar.xz"; echo "abc  alt.tar.xz" > "$r/alt.tar.xz.sha256"
got=$(root=$r ARBACE_SEED_GO=alt.tar.xz bash -c ". '$r/bin/lib/host.bash'; arbace_host_exe" 2>&1)
[ "$got" = "$r/target/seed/amd64/arbace" ] && ok "host.bash: ARBACE_SEED_GO names the seed" || bad "host.bash gave: $got"

exit $fail
