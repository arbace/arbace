# needs-host
# n-cpp-concepts: the concept rules' unit cases (test/cpp/selftest/concepts/, concepts-diag.ae) on Arbace's own front end, one
# process under 1 GiB (GOMEMLIMIT=700MiB: about 0.9 GB peak), no corpus: each case's error lines its dg-error lines, the
# control (a case's read judged against the lines of another rule's case) failing
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
. "$root/bin/lib/tools.bash"
cd "$root"
export GOMEMLIMIT=${GOMEMLIMIT:-700MiB} GOMAXPROCS=${GOMAXPROCS:-2}
arbace_tool -s arbace.c.dg -s arbace.repr.cpp.dg "$root/test/cpp/selftest/concepts-diag.ae" "$root"/test/cpp/selftest/concepts/*.C 2>&1 \
  | grep -v 'recur arg for primitive local\|Auto-boxing loop arg\|Reflection warning'
exit "${PIPESTATUS[0]}"
