# needs-host
# queue 423: overload resolution's unit cases for class-type assignment and braced lists for class references
# (test/cpp/selftest/overload/), judged by concepts-diag.ae as the concept rules' cases are: Arbace's own front end at gnu++20,
# each case's error lines its dg-error lines, the control (*-control.C, a case judged against moved lines) failing; the valid
# lines accepted by g++ 15.2 and clang++ (-std=c++20 -pedantic-errors -fsyntax-only), the error lines rejected by both
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
. "$root/bin/lib/tools.bash"
cd "$root"
export GOMEMLIMIT=${GOMEMLIMIT:-700MiB} GOMAXPROCS=${GOMAXPROCS:-2}
arbace_tool -s arbace.c.dg -s arbace.repr.cpp.dg "$root/test/cpp/selftest/concepts-diag.ae" "$root"/test/cpp/selftest/overload/*.C 2>&1 \
  | grep -v 'recur arg for primitive local\|Auto-boxing loop arg\|Reflection warning'
exit "${PIPESTATUS[0]}"
