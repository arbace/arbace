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

# fake_runtime DIR [TREE]: a runtime at TREE's key (default the checkout) that holds only the pending flag (rt.o) and empty
# archives: enough for a program that uses no library (min.cpp), without runtime-build's minutes
fake_runtime() {
  local d=$1 tree=${2:-$root}
  mkdir -p "$d"
  printf '_Thread_local int cx$pending;\n' > "$d/rt.c"
  gcc -c "$d/rt.c" -o "$d/rt.o"
  ar rcs "$d/liblstdc++.a"; ar rcs "$d/liblstdc++exp.a"
  "$tree/bin/cppi" runtime-key --print > "$d/key.txt" || return 1
  sha256sum < "$d/key.txt" | cut -d' ' -f1 > "$d/key"
}

# min_list FILE [NAME]: a lower-check list of test/cpp/selftest/min.cpp
min_list() { printf '%s\tc++23\t\t%s\n' "${2:-min.cpp}" "$root/test/cpp/selftest/min.cpp" > "$1"; }
