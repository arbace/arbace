# Sourced by test/cpp/selftest/NAME.sh: root, the tools (arbace_tool), the scratch directory $t (emptied), and
#   ok MSG / bad MSG   a line of the test's log; bad makes the test fail at its end (or at once: die MSG)
#   finish            the test's exit status
set -uo pipefail
# a test runs Arbace's tools on a copy of the tree too, where no stage 0 can be built: the host is named
[ -n "${ARBACE_HOST_EXE:-}" ] || { echo "selftest: ARBACE_HOST_EXE is not set (the tools' host)"; exit 1; }
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
. "$root/bin/lib/tools.bash"
export GOMAXPROCS=${GOMAXPROCS:-4} GOMEMLIMIT=${GOMEMLIMIT:-1500MiB}
t=$root/.tmp/cpp-selftest/$(basename "$0" .sh)
rm -rf "$t"; mkdir -p "$t"
nbad=0
ok() { echo "ok:  $*"; }
bad() { echo "BAD: $*"; nbad=$((nbad+1)); }
die() { echo "BAD: $*"; exit 1; }
finish() { [ "$nbad" = 0 ] && echo "selftest $(basename "$0" .sh): ok" || echo "selftest $(basename "$0" .sh): $nbad bad"; [ "$nbad" = 0 ]; }
